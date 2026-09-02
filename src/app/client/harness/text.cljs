(ns app.client.harness.text
     "Browser evidence for the text renderer, shaped layout, and flat route.
      Takes: WebGPU device state and loaded font assets.
      Gives: text harness result maps.
      Holds nothing."
     (:require [clojure.string :as str]
               [app.client.engine.device :as device]
               [app.client.engine.transform :as transform]
               [app.client.text.layout :as tl]
               [app.client.text.layout-oracle :as layout-oracle]
               [app.client.text.renderer :as text-renderer]
               [app.client.text.shaped-line :as sl]
               [app.client.harness.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-world-transforms run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte adapter-information
        shader-digests w4-read-texture!]]))

(defn- render-system-bytes!
  [^js device system zoom]
  (let [row-bytes (* canvas-size 4) ; 512, already WebGPU's required 256 alignment
        texture (.createTexture device
                               (clj->js {:size {:width canvas-size
                                               :height canvas-size
                                               :depthOrArrayLayers 1}
                                         :format color-format
                                         :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                                        js/GPUTextureUsage.COPY_SRC)}))
        read-buffer (.createBuffer device
                                   (clj->js {:size (* row-bytes canvas-size)
                                             :usage (bit-or js/GPUBufferUsage.COPY_DST
                                                            js/GPUBufferUsage.MAP_READ)}))
        camera-floats (js/Float32Array. 6)
        _ (device/update-camera device (:camera-uniform-buffer system)
                                  camera-floats 0.0 0.0 zoom
                                  canvas-size canvas-size)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass encoder
                               (clj->js {:colorAttachments
                                         [{:view (.createView texture)
                                           :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
                                           :loadOp "clear"
                                           :storeOp "store"}]}))]
    (.setPipeline pass (:pipeline system))
    (.setBindGroup pass 0 (:bind-group system))
    (.setVertexBuffer pass 0 (:instance-buffer system))
    (.draw pass 6 (:num-instances system) 0 0)
    (.end pass)
    (.copyTextureToBuffer encoder
                          (clj->js {:texture texture})
                          (clj->js {:buffer read-buffer
                                    :bytesPerRow row-bytes
                                    :rowsPerImage canvas-size})
                          (clj->js {:width canvas-size
                                    :height canvas-size
                                    :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then
         (fn [_]
           (let [copy (js/Uint8Array. (js/Uint8Array. (.getMappedRange read-buffer)))]
             (.unmap read-buffer)
             (.destroy read-buffer)
             (.destroy texture)
             copy))))))

(defn- render-pair! [device system zoom]
  (-> (render-system-bytes! device system zoom)
      (.then
       (fn [first-bytes]
         (-> (render-system-bytes! device system zoom)
             (.then
              (fn [second-bytes]
                (-> (js/Promise.all
                     #js [(sha256-bytes first-bytes)
                          (sha256-bytes second-bytes)])
                    (.then
                     (fn [hashes]
                       {:bytes first-bytes
                        :first-sha256 (aget hashes 0)
                        :second-sha256 (aget hashes 1)
                        :byte-identical? (= (aget hashes 0) (aget hashes 1))}))))))))))

(defn- pixel-red [^js rgba x y]
  (aget rgba (* 4 (+ x (* y canvas-size)))))

(defn- quadratic-point [[[x1 y1] [x2 y2] [x3 y3]] t]
  (let [u (- 1.0 t)]
    [(+ (* u u x1) (* 2.0 u t x2) (* t t x3))
     (+ (* u u y1) (* 2.0 u t y2) (* t t y3))]))

(defn- point-in-curves?
  "Even-odd point-in-path over the actual half-float Slug curve asset. Curves
   are flattened only for this independent CPU reader; GPU coverage continues
   to use the live quadratic evaluator."
  [curves px py]
  (odd?
   (reduce
    (fn [crossings curve]
      (loop [i 1
             [x1 y1] (first curve)
             crossings crossings]
        (if (> i 48)
          crossings
          (let [[x2 y2 :as p2] (quadratic-point curve (/ i 48.0))
                crosses-y? (not= (> y1 py) (> y2 py))
                intersection-x (when crosses-y?
                                 (+ x1 (* (/ (- py y1) (- y2 y1))
                                          (- x2 x1))))
                crossings (if (and crosses-y? (< px intersection-x))
                            (inc crossings)
                            crossings)]
            (recur (inc i) p2 crossings)))))
    0
    curves)))

(defn- instance-path-probe
  "Build the CPU inverse from the exact shaped quad and glyph bounds used by
   the selected production backend. This deliberately records, rather than
   assumes, the bearing/plane transform at the comparison boundary."
  [curves instance bounds zoom source]
  (let [[rx ry rw rh] (:rect instance)
        left (:left bounds)
        right (:right bounds)
        top (:top bounds)
        bottom (:bottom bounds)]
    {:inside?
     (fn [screen-x screen-y]
       (let [world-x (/ screen-x zoom)
             world-y (/ screen-y zoom)
             u (/ (- world-x rx) rw)
             v (/ (- world-y ry) rh)
             path-x (+ left (* u (- right left)))
             path-y (+ top (* v (- bottom top)))]
         (point-in-curves? curves path-x path-y)))
     :evidence {:source source
               :shaped-rect-world [rx ry rw rh]
               :glyph-bounds bounds
               :zoom zoom
               :mapping "screen->world->shaped-quad->glyph-path"}}))

(defn- parity-evidence [mode rgba inside? cpu-inverse]
  (let [boundary (boundary-pixels rgba)
        rows (mapv (fn [[x y coverage]]
                     (let [sx (+ x 0.5)
                           sy (+ y 0.5)
                           cpu-inside? (boolean (inside? sx sy))
                           ;; rgba8unorm encodes exact half coverage as byte
                           ;; 128 (128/255). That is a declared boundary tie,
                           ;; not a Boolean inside vote. W0-B Q6 found this
                           ;; ambiguity independently; do not threshold it away.
                           gpu-class (cond
                                       (< coverage 128) "outside"
                                       (> coverage 128) "inside"
                                       :else "boundary-tie")
                           decisive? (not= gpu-class "boundary-tie")
                           match? (when decisive?
                                    (= cpu-inside? (= gpu-class "inside")))]
                       {:pixel [x y]
                        :gpu-coverage-byte coverage
                        :cpu-inside? cpu-inside?
                        :gpu-class gpu-class
                        :decisive? decisive?
                        :match? match?}))
                   boundary)
        ties (filterv #(= "boundary-tie" (:gpu-class %)) rows)
        mismatches (filterv #(and (:decisive? %) (not (:match? %))) rows)]
    {:mode mode
     :contract "candidate-cpu-point-in-path-vs-production-gpu-coverage; byte-128-is-boundary-tie"
     :current-product-pick? false
     :cpu-inverse cpu-inverse
     :boundary-pixel-count (count rows)
     :decisive-count (- (count rows) (count ties))
     :match-count (- (count rows) (count ties) (count mismatches))
     :boundary-tie-count (count ties)
     :mismatch-count (count mismatches)
     :pass? (zero? (count mismatches))
     :verdict (cond
                (seq mismatches) "decisive-mismatch"
                (seq ties) "decisive-parity-with-declared-boundary-ties"
                :else "decisive-parity")
     :first-boundary-ties (subvec ties 0 (min 24 (count ties)))
     :first-mismatches (subvec mismatches 0 (min 24 (count mismatches)))}))

(defn- read-u16 [^js view byte-offset]
  (.getUint16 view byte-offset true))

(defn- band-entry [^js view width x y]
  (let [offset (* 4 (+ x (* y width)))]
    [(read-u16 view offset) (read-u16 view (+ offset 2))]))

(defn- band-entry-at-offset [view width origin-x origin-y offset]
  (let [linear (+ origin-x offset)
        x (mod linear width)
        y (+ origin-y (js/Math.floor (/ linear width)))]
    (band-entry view width x y)))

(defn- half->float [bits]
  (let [sign (if (zero? (bit-and bits 0x8000)) 1.0 -1.0)
        exponent (bit-and (unsigned-bit-shift-right bits 10) 0x1f)
        fraction (bit-and bits 0x03ff)]
    (cond
      (zero? exponent)
      (* sign (js/Math.pow 2.0 -14.0) (/ fraction 1024.0))

      (= exponent 31)
      (if (zero? fraction) (* sign js/Infinity) js/NaN)

      :else
      (* sign (js/Math.pow 2.0 (- exponent 15.0))
         (+ 1.0 (/ fraction 1024.0))))))

(defn- curve-texel [^js view width x y]
  (let [offset (* 8 (+ x (* y width)))]
    [(half->float (read-u16 view offset))
     (half->float (read-u16 view (+ offset 2)))
     (half->float (read-u16 view (+ offset 4)))
     (half->float (read-u16 view (+ offset 6)))]))

(defn- decode-glyph-curves [slug-assets unicode]
  (let [meta (get-in slug-assets [:slug :meta])
        glyph (first (filter #(= unicode (:unicode %)) (:glyphs meta)))
        band-width (get-in meta [:bandTexture :width])
        curve-width (get-in meta [:curveTexture :width])
        band-view (js/DataView. (get-in slug-assets [:slug :band-bytes]))
        curve-view (js/DataView. (get-in slug-assets [:slug :curve-bytes]))
        gx (get-in glyph [:slug :glyphLoc :x])
        gy (get-in glyph [:slug :glyphLoc :y])
        horizontal-count (inc (get-in glyph [:slug :bandMax :y]))
        vertical-count (inc (get-in glyph [:slug :bandMax :x]))
        !locations (atom #{})]
    (dotimes [header-index (+ horizontal-count vertical-count)]
      (let [[curve-count offset] (band-entry band-view band-width
                                             (+ gx header-index) gy)]
        (dotimes [curve-index curve-count]
          (swap! !locations conj
                 (band-entry-at-offset band-view band-width gx gy
                                       (+ offset curve-index))))))
    (mapv (fn [[x y]]
            (let [[p1x p1y p2x p2y] (curve-texel curve-view curve-width x y)
                  [p3x p3y _ _] (curve-texel curve-view curve-width (inc x) y)]
              [[p1x p1y] [p2x p2y] [p3x p3y]]))
          (sort-by (juxt second first) @!locations))))

(defn- glyph-lines [zoom]
  [[{:text "o"
     :x (/ glyph-screen-x zoom)
     :y (/ glyph-screen-baseline zoom)
     :size (/ glyph-screen-size zoom)
     :r 1.0 :g 1.0 :b 1.0 :a 1.0
     :container 0}]])

(defn- text-world-transforms []
  (-> (transform/empty-registry)
      (transform/add-group 17 {:parent 0
                                   :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
      (transform/world-transforms)))

(defn- image-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- run-case!
  [{:keys [device slug-system slug-assets curves world-transforms]}
   {:keys [case-id zoom lod]}]
  (let [lines (glyph-lines zoom)
        font-size (/ glyph-screen-size zoom)
        slug-glyph (first (filter #(= 111 (:unicode %))
                                  (get-in slug-assets [:slug :meta :glyphs])))
        slug-instance (first (:instances
                              (text-renderer/shape-text
                               (first lines) font-size slug-assets
                               :char-width 0.60)))
        slug-probe (instance-path-probe curves slug-instance
                                        (or (:sampleBounds slug-glyph)
                                            (:planeBounds slug-glyph))
                                        zoom "production-slug-sampleBounds")
        slug-system (text-renderer/update-text-data
                     device slug-system lines slug-assets font-size
                     :char-width 0.60 :world-transforms world-transforms)]
    (js/console.log "[W0-A] case-start" case-id "zoom" zoom)
    (->
        (render-pair! device slug-system zoom)
        (.then
         (fn [slug-pair]
           (js/console.log "[W0-A] case-complete" case-id)
           {:case-id case-id
            :zoom zoom
            :lod lod
            :normalization "screen-constant"
            :shape-extent-world (/ glyph-screen-size zoom)
            :canvas {:width canvas-size
                     :height canvas-size
                     :format color-format
                     :device-pixel-ratio (.-devicePixelRatio js/window)}
            :images [(image-record "slug" case-id slug-pair)]
            :pick-parity
            [(parity-evidence "slug-dejavu-o-path"
                               (:bytes slug-pair)
                               (:inside? slug-probe)
                               (:evidence slug-probe))]})))))

(defn- run-ubuntu-mixed-case!
  [device ubuntu-system ubuntu-assets world-transforms]
  (let [text "Aɐ"
        font-size 56.0
        line-height 68.0
        layout-result (tl/layout {:text text
                                  :provider (:layout-provider ubuntu-assets)
                                  :font-size font-size
                                  :line-height line-height
                                  :origin [24.0 18.0]
                                  :baseline-offset font-size
                                  :source-id :harness/ubuntu-slug-mixed-face
                                  :source-revision 1})
        glyphs (:glyphs (tl/paint-result layout-result))
        face-ids (->> glyphs (map :font-id) distinct sort vec)
        face-revisions (->> glyphs (map :font-revision) distinct sort vec)
        expected-faces ["noto-sans-regular" "ubuntu-sans-variable"]
        _ (when-not (= expected-faces face-ids)
            (throw (ex-info "Ubuntu Slug golden did not shape through both faces."
                            {:text text :expected expected-faces :actual face-ids})))
        lines (->> (tl/line-paint-draw-items
                     layout-result
                     {:size font-size :r 1.0 :g 1.0 :b 1.0 :a 1.0
                      :container 0})
                   (mapv vector))
        ubuntu-system (text-renderer/update-text-data
                        device ubuntu-system lines ubuntu-assets font-size
                        :line-height line-height :world-transforms world-transforms)]
    (-> (render-pair! device ubuntu-system 1.0)
        (.then
         (fn [pair]
           {:pass true
            :text text
            :face-ids face-ids
            :face-revisions face-revisions
            :cases
            [{:case-id "ubuntu-mixed-face"
              :zoom 1.0
              :lod "default-font-mixed-face"
              :normalization "component-fixed"
              :shape-extent-world font-size
              :images [(image-record "slug" "ubuntu-mixed-face" pair)]}]})))))
(defn t1-layout-evidence [provider]
  (let [text "AV office e\u0301\tسلام\nɐ"
        result (tl/layout {:text text :provider provider
                           :font-size 19 :line-height 24
                           :origin [10 20] :baseline-offset 19
                           :clip {:left 12 :right 180 :top 20 :bottom 68}
                           :source-id :harness/t1 :source-revision 1
                           :zoom 1})
        readers [(tl/measure-result result)
                 (tl/wrap-result result)
                 (tl/paint-result result)
                 (tl/caret-result result 0 5)
                 (tl/selection-result result 0 3 10)
                 (tl/clip-result result {:text (first (str/split-lines text))
                                         :from 0 :to 19 :x 10 :y 39 :size 19})
                 (tl/hit-test-result result [48 24])]
        variable-advance
        (fn [width]
          (first
            (get-in
              (tl/measure-result
                (tl/layout {:text "variable" :provider provider
                            :font-size 19 :line-height 24
                            :variations {:wght 400 :wdth width}
                            :source-id :harness/t1-variable-axis}))
              [:metrics :advance])))
        narrow-advance (variable-advance 75)
        wide-advance (variable-advance 125)
        evidence {:layout-id (:layout/id result)
                 :reader-layout-ids (mapv :layout/id readers)
                 :glyph-count (count (get-in readers [2 :glyphs]))
                 :cluster-count (count (:clusters result))
                 :lines (count (:lines result))
                 :rtl? (boolean (some #(= :rtl (:direction %)) (:runs result)))
                 :fallback? (boolean (some #(= "noto-sans-regular-2.011"
                                                (:font-revision %))
                                           (:runs result)))
                 :tab? (boolean (some #(= :virtual/tab (:glyph-id-kind %))
                                      (get-in readers [2 :glyphs])))
                 :variations (get-in result [:font :variations])
                 :variable-axis-delta (- wide-advance narrow-advance)
                 :lod (:lod result)}
        pass? (and (every? #{(:layout/id result)} (:reader-layout-ids evidence))
                   (= 2 (:lines evidence))
                   (:rtl? evidence) (:fallback? evidence) (:tab? evidence)
                   (not (zero? (:variable-axis-delta evidence)))
                   (= {:wght 400 :wdth 100} (:variations evidence)))]
    (when-not pass?
      (throw (ex-info "T1 browser layout evidence failed." evidence)))
    (assoc evidence :pass true)))
;; ---------------------------------------------------------------------------
;; The flat text route's consistency checks (docs/shaping-correction/SHAPER-BORDER.md §8):
;; F1 flat shaper ≡ oracle shaper · F2 flat layout ≡ oracle layout on real
;; HarfBuzz · F3 flat pack bytes ≡ oracle pack bytes; plus the µs/glyph
;; bracket, recorded and never gated.

(def ^:private flat-route-lines
  ["AV office é\tسلام ɐ"
   "漢字"
   ""
   "a\tb\tc"
   "مرحبا hello"
   "é"
   "ffi ffl — “quotes” 12:30"])

(defn- bytes= [^js a ^js b]
  (and (= (.-byteLength a) (.-byteLength b))
       (let [ua (js/Uint8Array. a) ub (js/Uint8Array. b)]
         (loop [i 0]
           (cond (>= i (.-length ua)) true
                 (= (aget ua i) (aget ub i)) (recur (inc i))
                 :else false)))))

(defn- flat-route-f1 [provider]
  (let [opts {}
        rows (mapv (fn [text]
                     (let [flat ((:shape-line provider) text opts)
                           back (sl/->maps flat)
                           oracle ((:shape-line-oracle provider) text opts)
                           pass (= back oracle)]
                       {:text text
                        :glyph-count (:glyph-count flat)
                        :run-count (:run-count flat)
                        :pass pass
                        :first-difference
                        (when-not pass
                          (pr-str (first (remove (fn [[a b]] (= a b))
                                                 (map vector (:glyphs back)
                                                      (:glyphs oracle))))))}))
                   flat-route-lines)
        rtl (first (filter #(= "AV office é\tسلام ɐ" (:text %)) rows))
        flat ((:shape-line provider) "AV office é\tسلام ɐ" opts)
        ;; S1's strengthening: the RTL run's clusters descend in visual order,
        ;; the tab lands on a stop, and ɐ shaped through the fallback face.
        rtl-descending?
        (let [runs (:run-count flat)]
          (boolean
           (some (fn [r]
                   (when (sl/run-rtl? flat r)
                     (let [starts (keep (fn [i]
                                          (when (= r (sl/u32-get (:run-index flat) i))
                                            (sl/u32-get (:cluster-start flat) i)))
                                        (range (:glyph-count flat)))]
                       (and (seq starts) (= (vec starts) (vec (reverse (sort starts))))))))
                 (range runs))))
        tab-on-stop?
        (boolean
         (some (fn [i]
                 (when (sl/glyph-tab? flat i)
                   (let [x (sl/i32-get (:glyph-x flat) i)
                         adv (sl/i32-get (:advance-x flat) i)]
                     (zero? (mod (+ x adv) 2000)))))
               (range (:glyph-count flat))))
        fallback-face?
        (boolean
         (some (fn [r] (pos? (sl/u32-get (:run-face flat) r)))
               (range (:run-count flat))))]
    {:name "F1 flat shaper = oracle shaper"
     :pass (and (every? :pass rows) rtl-descending? tab-on-stop? fallback-face?)
     :lines rows
     :rtl-descending rtl-descending?
     :tab-on-stop tab-on-stop?
     :fallback-face fallback-face?
     :rtl-line (:text rtl)}))

(defn- flat-route-f2 [provider]
  (let [inputs [{:text "AV office é\tسلام\nɐ" :provider provider
                 :font-size 19 :line-height 24
                 :origin [10 20] :baseline-offset 19
                 :clip {:left 12 :right 180 :top 20 :bottom 68}
                 :source-id :harness/flat-f2 :source-revision 1 :zoom 1}
                {:text "office ffi mixed سلام ɐ words wrap here" :provider provider
                 :font-size 19 :line-height 24 :origin [0 0] :baseline-offset 19
                 :inline-size 120 :wrap-policy :word
                 :source-id :harness/flat-f2-wrap :source-revision 1 :zoom 1}
                {:text "abc def ghi" :headers ["Head"] :provider provider
                 :font-size 12 :line-height 14 :origin [3 4] :baseline-offset 12
                 :wrap-policy :block-greedy :wrap-col 6
                 :source-id :harness/flat-f2-greedy :source-revision 1 :zoom 1}]
        rows (mapv (fn [input]
                     (let [flat (tl/layout input)
                           mapped (layout-oracle/layout input)]
                       {:source-id (str (:source-id input))
                        :lines (count (:lines flat))
                        :glyphs (:glyph-count (tl/plane-coverage-check flat))
                        :pass (and (tl/result= flat mapped)
                                   (= (tl/plane-coverage-check flat) (tl/plane-coverage-check mapped))
                                   (= (:glyphs (tl/paint-result flat))
                                      (:glyphs (tl/paint-result mapped))))}))
                   inputs)]
    {:name "F2 flat layout = oracle layout (real HarfBuzz)"
     :pass (every? :pass rows)
     :inputs rows}))

(defn- flat-route-f3 [slug-assets t1-assets world-transforms]
  (let [stride text-renderer/slug-text-instance-stride
        carried (let [layout (tl/layout {:text "Aɐ b\tc x\noffice ffi"
                                         :provider (:layout-provider t1-assets)
                                         :font-size 32 :line-height 40
                                         :origin [8 10] :baseline-offset 32
                                         :source-id :harness/flat-f3
                                         :source-revision 1})]
                  (mapv vector (tl/line-paint-draw-items
                                layout {:size 32 :r 0.9 :g 0.5 :b 0.2 :a 1.0
                                        :container 0})))
        cases [{:name "slug-case-lines" :texts (glyph-lines 1.0) :assets slug-assets
                :font-size glyph-screen-size :opts [:char-width 0.60]}
               {:name "ubuntu-carried-two-lines" :texts carried :assets t1-assets
                :font-size 32 :opts []}
               {:name "ubuntu-uncarried-lines"
                :texts [[{:text "Aɐ b\tc" :x 4 :y 30 :size 24
                          :r 1 :g 1 :b 1 :a 1 :container 0}]
                        [{:text "office" :x 4 :y 60 :size 24
                          :r 1 :g 0 :b 0 :a 1 :container 0}]]
                :assets t1-assets :font-size 24 :opts []}]
        rows (mapv (fn [{:keys [name texts assets font-size opts]}]
                     (let [opts (into opts [:world-transforms world-transforms])
                           flat (apply text-renderer/pack-instances-flat
                                       texts assets font-size stride opts)
                           oracle (apply text-renderer/pack-instances-oracle
                                         texts assets font-size stride opts)]
                       {:name name
                        :num-instances (:num-instances flat)
                        :line-offsets (:line-offsets flat)
                        :fallbacks (:fallbacks flat)
                        :pass (and (bytes= (:raw-buffer flat) (:raw-buffer oracle))
                                   (= (:line-offsets flat) (:line-offsets oracle))
                                   (= (:num-instances flat) (:num-instances oracle))
                                   (= (:fallbacks flat) (:fallbacks oracle))
                                   (pos? (:num-instances flat)))}))
                   cases)
        carried-row (first (filter #(= "ubuntu-carried-two-lines" (:name %)) rows))
        ;; S4: carried draw-items never fall back; uncarried ones count once per draw-item
        entry-points-pass (and (zero? (get-in carried-row [:fallbacks :combined-text-draw-items] 0))
                        (= 2 (get-in (first (filter #(= "ubuntu-uncarried-lines" (:name %)) rows))
                                     [:fallbacks :combined-text-draw-items] 0)))]
    {:name "F3 flat pack bytes = oracle pack bytes; I1 entry points"
     :pass (and (every? :pass rows) entry-points-pass)
     :cases rows
     :entry-points entry-points-pass}))

(defn- flat-route-bracket
  "µs per glyph for shape · layout · pack over a fixed fixture; a number in
   the evidence, never a gate."
  [provider t1-assets world-transforms]
  (let [lines (vec (remove empty? flat-route-lines))
        reps 40
        now #(js/performance.now)
        shape-glyphs (reduce + 0 (map #(:glyph-count ((:shape-line provider) % {})) lines))
        t0 (now)
        _ (dotimes [_ reps] (doseq [l lines] ((:shape-line provider) l {})))
        t1 (now)
        block (clojure.string/join "\n" lines)
        layout-input {:text block :provider provider :font-size 19 :line-height 24
                      :origin [0 0] :baseline-offset 19
                      :source-id :harness/flat-bracket :source-revision 1}
        layout-glyphs (:glyph-count (tl/plane-coverage-check (tl/layout layout-input)))
        t2 (now)
        _ (dotimes [_ reps] (tl/layout layout-input))
        t3 (now)
        draw-items (mapv vector (tl/line-paint-draw-items (tl/layout layout-input)
                                            {:size 19 :r 1 :g 1 :b 1 :a 1
                                             :container 0}))
        stride text-renderer/slug-text-instance-stride
        packed (text-renderer/pack-instances-flat
                draw-items t1-assets 19 stride :world-transforms world-transforms)
        t4 (now)
        _ (dotimes [_ reps]
            (text-renderer/pack-instances-flat
             draw-items t1-assets 19 stride :world-transforms world-transforms))
        t5 (now)
        per (fn [ms n] (when (pos? n) (/ (* 1000 ms) (* reps n))))]
    {:reps reps
     :shape-glyphs shape-glyphs
     :layout-glyphs layout-glyphs
     :pack-instances (:num-instances packed)
     :shape-us-per-glyph (per (- t1 t0) shape-glyphs)
     :layout-us-per-glyph (per (- t3 t2) layout-glyphs)
     :pack-us-per-instance (per (- t5 t4) (:num-instances packed))
     :clock-note "performance.now, one bracket per route, no gate"}))

(defn run-text-flat-route! [slug-assets t1-assets]
  (try
    (let [provider (:layout-provider t1-assets)
          world-transforms (text-world-transforms)
          rows [(flat-route-f1 provider)
                (flat-route-f2 provider)
                (flat-route-f3 slug-assets t1-assets world-transforms)]]
      {:pass (every? :pass rows)
       :rows rows
       :bracket (flat-route-bracket provider t1-assets world-transforms)})
    (catch :default error
      {:pass false
       :rows []
       :error (str error)
       :data (pr-str (ex-data error))})))

(defn- packed-buffer-indexes [packed]
  (let [words (js/Uint32Array. (:raw-buffer packed))]
    (mapv #(aget words (+ (* % 25) 24))
          (range (:num-instances packed)))))

(defn- group-rejected? [ubuntu-assets world-transforms text-draw-item]
  (try
    (text-renderer/pack-instances-flat
     [[text-draw-item]]
     ubuntu-assets 24 text-renderer/slug-text-instance-stride
     :world-transforms world-transforms)
    false
    (catch :default error
      (= :transform/unknown-group (:error-type (ex-data error))))))

(defn- run-group-tree-case!
  [device ubuntu-system ubuntu-assets world-transforms]
  (let [text "Aɐ"
        font-size 56.0
        line-height 68.0
        layout-result (tl/layout {:text text
                                  :provider (:layout-provider ubuntu-assets)
                                  :font-size font-size
                                  :line-height line-height
                                  :origin [24.0 18.0]
                                  :baseline-offset font-size
                                  :source-id :harness/text-group-tree
                                  :source-revision 1})
        lines (->> (tl/line-paint-draw-items
                    layout-result
                    {:size font-size :r 1.0 :g 1.0 :b 1.0 :a 1.0
                     :container 17})
                   (mapv vector))
        packed (text-renderer/pack-instances-flat
                lines ubuntu-assets font-size
                text-renderer/slug-text-instance-stride
                :world-transforms world-transforms)
        buffer-indexes (packed-buffer-indexes packed)
        buffer-indexes-pass (and (seq buffer-indexes) (every? #{1} buffer-indexes))
        base-draw-item {:text "A" :x 0 :y 32 :size 24 :r 1 :g 1 :b 1 :a 1}
        missing-rejected (group-rejected? ubuntu-assets world-transforms base-draw-item)
        unknown-rejected (group-rejected? ubuntu-assets world-transforms
                                            (assoc base-draw-item :container 999))
        fallbacks-pass (every? zero? (vals (:fallbacks packed)))
        ubuntu-system (text-renderer/update-text-data
                       device ubuntu-system lines ubuntu-assets font-size
                       :line-height line-height :world-transforms world-transforms)]
    (-> (render-pair! device ubuntu-system 1.0)
        (.then
         (fn [pair]
           {:pass (and (:byte-identical? pair) buffer-indexes-pass missing-rejected
                       unknown-rejected fallbacks-pass)
            :buffer-indexes buffer-indexes
            :missing-group-rejected missing-rejected
            :unknown-group-rejected unknown-rejected
            :fallbacks (:fallbacks packed)
            :case {:case-id "text-group-tree-mixed-face"
                   :zoom 1.0
                   :lod "group-tree-cid17-buffer-index1"
                   :normalization "component-fixed"
                   :shape-extent-world font-size
                   :images [(image-record
                             "slug" "container-tree-mixed-face-cid17-slot1"
                             pair)]}})))))



(defn run-text-slug!
  [device camera-buffer groups-buffer slug-assets t1-assets]
  (js/console.log "[W0-A] init-font-assets")
  (let [world-transforms (text-world-transforms)
        _ (device/write-groups! device groups-buffer world-transforms)
        slug-system (do
                      (js/console.log "[W0-A] init-slug-pipeline-start")
                      (let [system
                            (text-renderer/init-text-system
                             device color-format camera-buffer slug-assets
                             :initial-capacity 1
                             :groups-buffer groups-buffer)]
                        (js/console.log "[W0-A] init-slug-pipeline-complete")
                        system))
        ubuntu-system
        (text-renderer/init-text-system
         device color-format camera-buffer t1-assets
         :initial-capacity 2
         :groups-buffer groups-buffer)
        ubuntu-tree-system
        (text-renderer/init-text-system
         device color-format camera-buffer t1-assets
         :initial-capacity 2
         :groups-buffer groups-buffer)
        curves (do
                 (js/console.log "[W0-A] init-curve-decode-start")
                 (let [decoded (decode-glyph-curves slug-assets 111)]
                   (js/console.log "[W0-A] init-curve-decode-complete"
                                   (count decoded))
                   decoded))
        harness {:device device
                 :slug-system slug-system
                 :slug-assets slug-assets
                 :curves curves
                 :world-transforms world-transforms}]
    (-> (js/Promise.all
         #js [(promise-mapv (partial run-case! harness) zoom-cases)
              (run-ubuntu-mixed-case! device ubuntu-system t1-assets world-transforms)
              (run-group-tree-case! device ubuntu-tree-system t1-assets
                                        world-transforms)])
        (.then
         (fn [values]
           (let [ubuntu (aget values 1)
                 tree (aget values 2)]
             {:decoded-slug-curve-count (count curves)
              :ubuntu-slug (assoc ubuntu
                                  :pass (and (:pass ubuntu) (:pass tree))
                                  :group-tree tree)
              :cases (aget values 0)}))))))
