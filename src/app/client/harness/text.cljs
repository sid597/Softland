(ns app.client.harness.text
     "Compare text packing and GPU coverage with controlled references.

      Input: device, shared buffers and loaded Slug/font resources. Output:
      zoom goldens, decoded-curve parity, fallback-face evidence and
      group/layout checks. Temporary render targets/readback buffers belong
      to individual captures. Renderer systems and their font bindings are
      retained through the run.

      Folder map: README.md."
     (:require [clojure.string :as str]
               [app.client.engine.device :as device]
               [app.client.engine.transform :as transform]
               [app.client.text.layout :as tl]
               [app.client.text.renderer :as text-renderer]
               [app.client.harness.shared
:refer [canvas-size color-format glyph-screen-x glyph-screen-baseline
        glyph-screen-size zoom-cases image-fixtures promise-mapv
        bytes->hex sha256-bytes sha256-string opaque-png-data-url
        q8-world-transforms run-q8-transport! boundary-pixels byte-delta pixel-rgba
        srgb->linear linear->srgb-byte adapter-information
        shader-digests w4-read-texture!]]))

(defn- render-system-bytes!
  "Device, text system and zoom → promise of 128×128 RGBA; updates camera
   and submits draw/copy.

   Offscreen render/readback with normal-path temporary cleanup. Intended
   for a controlled text capture."
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

(defn- render-pair!
  "Device, text system and zoom → promise of first bytes, two hashes and
   equality.

   Two sequential renders. Intended for within-run repeatability, not
   cross-machine stability."
  [device system zoom]
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

(defn- pixel-red
  "RGBA bytes and pixel position → red channel.

   Fixed-canvas indexing. Intended for the local coverage convention."
  [^js rgba x y]
  (aget rgba (* 4 (+ x (* y canvas-size)))))

(defn- quadratic-point
  "Three control points and t → Bézier point.

   Bernstein interpolation."
  [[[x1 y1] [x2 y2] [x3 y3]] t]
  (let [u (- 1.0 t)]
    [(+ (* u u x1) (* 2.0 u t x2) (* t t x3))
     (+ (* u u y1) (* 2.0 u t y2) (* t t y3))]))

(defn- point-in-curves?
  "Curves and point → even/odd inclusion.

   Flattens each quadratic into 48 segments and ray-crosses. Bounded
   approximation may differ near curved boundaries."
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
  "Packed instance, zoom and decoded curves → screen-to-curve mapping and
   inside predicate.

   Derives the inverse from the actual packed quad. Compares against
   uploaded placement rather than a separate guessed layout."
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

(defn- parity-evidence
  "Mode, pixels, inside predicate and inverse description → boundary
   decisions/mismatch report.

   Red-channel boundary samples and a half-coverage decision. Current
   limitation: a zero-boundary result can satisfy zero mismatches, unlike
   the image check's explicit nonempty guard."
  [mode rgba inside? cpu-inverse]
  (let [boundary (boundary-pixels rgba)
        rows (mapv (fn [[x y coverage]]
                     (let [sx (+ x 0.5)
                           sy (+ y 0.5)
                           cpu-inside? (boolean (inside? sx sy))
                           ;; rgba8unorm encodes exact half coverage as byte
                           ;; 128 (128/255). That is a declared boundary tie,
                           ;; not a Boolean inside vote. This ambiguity is
                           ;; declared explicitly; do not threshold it away.
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

(defn- read-u16
  "Byte array and offset → little-endian unsigned word.

   Direct two-byte decode. Assumes valid bounds."
  [^js view byte-offset]
  (.getUint16 view byte-offset true))

(defn- band-entry
  "Band texture data and x/y → two unsigned words.

   Converts texel address into byte offsets. Intended for this asset format."
  [^js view width x y]
  (let [offset (* 4 (+ x (* y width)))]
    [(read-u16 view offset) (read-u16 view (+ offset 2))]))

(defn- band-entry-at-offset
  "Band texture coordinates plus linear offset → entry.

   Wraps into texture rows before decoding."
  [view width origin-x origin-y offset]
  (let [linear (+ origin-x offset)
        x (mod linear width)
        y (+ origin-y (js/Math.floor (/ linear width)))]
    (band-entry view width x y)))

(defn- half->float
  "Half-float bits → numeric value, including subnormal/infinity/NaN cases.

   Explicit IEEE-754 decode. Intended for CPU curve inspection."
  [bits]
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

(defn- curve-texel
  "Curve texture and coordinates → four decoded components.

   Four half-float reads."
  [^js view width x y]
  (let [offset (* 8 (+ x (* y width)))]
    [(half->float (read-u16 view offset))
     (half->float (read-u16 view (+ offset 2)))
     (half->float (read-u16 view (+ offset 4)))
     (half->float (read-u16 view (+ offset 6)))]))

(defn- decode-glyph-curves
  "Slug assets and Unicode codepoint → unique, ordered quadratic
   control-point vectors.

   Follows glyph bands to curve references, deduplicates then decodes.
   References real asset geometry; tightly coupled to its packing format."
  [slug-assets unicode]
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

(defn- glyph-lines
  "Zoom → fixture lines for two “o” glyphs at constant screen size.

   Inverse-scales world size/position. Intended for the chosen
   normalization."
  [zoom]
  [[{:text "oo"
     :x (/ glyph-screen-x zoom)
     :y (/ glyph-screen-baseline zoom)
     :size (/ glyph-screen-size zoom)
     :r 1.0 :g 1.0 :b 1.0 :a 1.0
     :container 0}]])

(defn- text-world-transforms
  "No arguments → root/child-17 transform rows.

   Shared transform registry derivation. Intended for compact-index
   coverage."
  []
  (-> (transform/empty-registry)
      (transform/add-group 17 {:parent 0
                                   :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
      (transform/world-transforms)))

(defn- image-record
  "Mode, zoom and render pair → preview/hash/determinism record."
  [mode case-id pair]
  {:mode mode
   :file (str "gpu-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- run-case!
  "Text system/assets/provider, decoded curves and zoom → promise of glyph
   golden and parity evidence.

   Packs “oo”, checks DejaVu/no unresolved glyphs, updates renderer, renders
   twice and compares coverage. Intended for this glyph/provider/zoom slice;
   not broad language coverage."
  [{:keys [device slug-system slug-assets curves world-transforms]}
   {:keys [case-id zoom lod]}]
  (let [lines (glyph-lines zoom)
        font-size (/ glyph-screen-size zoom)
        slug-glyph (first (filter #(= 111 (:unicode %))
                                  (get-in slug-assets [:slug :meta :glyphs])))
        packed (text-renderer/pack-instances-flat
                [(first lines)] slug-assets font-size
                text-renderer/slug-text-instance-stride
                :world-transforms world-transforms)
        provider (select-keys (:layout-provider slug-assets)
                              [:face-id :face-revision :shaper-id :shaper-version])
        unresolved-glyphs (:unresolved-glyphs packed)
        _ (when-not (= "dejavu-sans-mono" (:face-id provider))
            (throw (ex-info "DejaVu case did not use its font-file provider."
                            {:case-id case-id :provider provider})))
        _ (when-not (zero? unresolved-glyphs)
            (throw (ex-info "DejaVu case has unresolved Slug glyphs."
                            {:case-id case-id
                             :provider provider
                             :unresolved-glyphs unresolved-glyphs})))
        packed-words (js/Float32Array. (:raw-buffer packed))
        slug-instance {:rect (mapv #(aget packed-words %) (range 4))}
        slug-probe (instance-path-probe curves slug-instance
                                        (or (:sampleBounds slug-glyph)
                                            (:planeBounds slug-glyph))
                                        zoom "production-slug-sampleBounds")
        slug-system (text-renderer/update-text-data
                     device slug-system lines slug-assets font-size
                     :world-transforms world-transforms)]
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
            :provider provider
            :unresolved-glyphs unresolved-glyphs
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
  "System and Ubuntu provider → promise of mixed-face “Aɐ” evidence.

   Requires Ubuntu plus Noto fallback, lays out/packs/renders twice. Its
   literal :pass true follows face validation; byte equality is recorded
   separately rather than included in that field."
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
(defn t1-layout-evidence
  "Font provider → shared-layout/RTL/fallback/variable-axis evidence or
   exception.

   Drives measure, wrap, paint, caret, selection, clip and hit paths and
   compares layout IDs. Current limitation: stale top-level cluster/run
   readers do not match the current layout shape."
  [provider]
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
(defn- packed-buffer-indexes
  "Packed instances → unsigned group index from each 25-word row.

   Reads slot 24 through uint view. Intended for verifying the actual
   transport representation."
  [packed]
  (let [words (js/Uint32Array. (:raw-buffer packed))]
    (mapv #(aget words (+ (* % 25) 24))
          (range (:num-instances packed)))))

(defn- group-rejected?
  "Draw inputs/provider → whether packing rejects group resolution.

   Executes the pack path and catches its error. A boolean loses exact
   failure provenance."
  [ubuntu-assets world-transforms text-draw-item]
  (try
    (text-renderer/pack-instances-flat
     [[text-draw-item]]
     ubuntu-assets 24 text-renderer/slug-text-instance-stride
     :world-transforms world-transforms)
    false
    (catch :default error
      (= :transform/unknown-group (:error-type (ex-data error))))))

(defn- run-group-tree-case!
  "System/provider/transforms → promise of carried-layout and
   fallback-layout group evidence.

   Checks child index 1, missing/unknown rejection, zero carried-layout
   fallbacks and two uncarried fallbacks, plus repeated render. Intended for
   the intended transport boundary."
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
        uncarried (text-renderer/pack-instances-flat
                   [[{:text "A" :x 0 :y 32 :size 24
                      :r 1 :g 1 :b 1 :a 1 :container 17}
                     {:text "ɐ" :x 24 :y 32 :size 24
                      :r 1 :g 1 :b 1 :a 1 :container 17}]]
                   ubuntu-assets 24 text-renderer/slug-text-instance-stride
                   :world-transforms world-transforms)
        buffer-indexes (packed-buffer-indexes packed)
        buffer-indexes-pass (and (seq buffer-indexes) (every? #{1} buffer-indexes))
        base-draw-item {:text "A" :x 0 :y 32 :size 24 :r 1 :g 1 :b 1 :a 1}
        missing-rejected (group-rejected? ubuntu-assets world-transforms base-draw-item)
        unknown-rejected (group-rejected? ubuntu-assets world-transforms
                                            (assoc base-draw-item :container 999))
        fallbacks-pass (and (zero? (:fallbacks packed))
                            (= 2 (:fallbacks uncarried)))
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
            :uncarried-fallbacks (:fallbacks uncarried)
            :case {:case-id "text-group-tree-mixed-face"
                   :zoom 1.0
                   :lod "group-tree-cid17-buffer-index1"
                   :normalization "component-fixed"
                   :shape-extent-world font-size
                   :images [(image-record
                             "slug" "container-tree-mixed-face-cid17-slot1"
                             pair)]}})))))



(defn run-text-slug!
  "Device/shared buffers and font assets/providers → promise of all text
   cases.

   Creates three systems; sequences zoom cases and joins independent Ubuntu
   checks. System teardown is absent at the end of this driver."
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
