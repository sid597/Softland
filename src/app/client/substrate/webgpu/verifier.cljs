(ns app.client.substrate.webgpu.verifier
  "Permanent W0-A browser half.

   This harness deliberately instantiates the production renderer's public
   pipeline/update functions and repository font assets. It owns only capture,
   comparison inputs, a candidate geometry-contract probe, and receipts. It is
   not a product renderer and it never substitutes lookalike WGSL.

   The CPU point-in-path probe is NOT today's product picking path. Product
   picking remains axis-aligned rect-tree bounds; the receipt carries an
  explicit rounded-corner divergence sentinel so those truths cannot collapse."
  (:require [clojure.string :as str]
            [app.client.substrate.webgpu.renderer :as renderer]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.runtime.fonts :as fonts]
            [app.client.workspace.text-layout :as tl]
            [app.client.workspace.text-shaper :as text-shaper]))

(def ^:private canvas-size 128)
(def ^:private color-format "rgba8unorm")
(def ^:private fixture-screen-x 24.0)
(def ^:private fixture-screen-y 24.0)
(def ^:private fixture-screen-size 80.0)
(def ^:private glyph-screen-x 34.0)
(def ^:private glyph-screen-baseline 96.0)
(def ^:private glyph-screen-size 80.0)
(def ^:private rounded-radii [24.0 14.0 30.0 6.0])
(def ^:private q5-angle 0.637)
(def ^:private q5-pinned-pixels [[61 43] [66 84]])

(def ^:private zoom-cases
  [{:case-id "legal-min-z0p01" :zoom 0.01 :regime "legal-envelope-sentinel"}
   {:case-id "default-min-z0p1" :zoom 0.1 :regime "floor-default-clamp"}
   {:case-id "default-unit-z1" :zoom 1.0 :regime "floor-default-clamp"}
   {:case-id "default-max-z8" :zoom 8.0 :regime "floor-default-clamp"}
   {:case-id "legal-log-z10" :zoom 10.0 :regime "legal-envelope-sentinel"}
   {:case-id "legal-log-z100" :zoom 100.0 :regime "legal-envelope-sentinel"}
   {:case-id "legal-max-z1000" :zoom 1000.0 :regime "legal-envelope-sentinel"}])

(defn- promise-mapv [f xs]
  (reduce (fn [p x]
            (.then p
                   (fn [acc]
                     (.then (f x) #(conj acc %)))))
          (js/Promise.resolve [])
          xs))

(defn- bytes->hex [^js bytes]
  (apply str
         (map (fn [b]
                (let [h (.toString b 16)]
                  (if (= 1 (count h)) (str "0" h) h)))
              (array-seq bytes))))

(defn- sha256-bytes [^js bytes]
  (-> (.digest (.-subtle js/crypto) "SHA-256" bytes)
      (.then #(bytes->hex (js/Uint8Array. %)))))

(defn- sha256-string [s]
  (sha256-bytes (.encode (js/TextEncoder.) s)))

(defn- opaque-png-data-url [^js rgba]
  ;; The golden is the visible result over the verifier's black clear color.
  ;; Raw GPU bytes are hashed separately and remain the comparison authority.
  (let [canvas (.createElement js/document "canvas")
        _ (set! (.-width canvas) canvas-size)
        _ (set! (.-height canvas) canvas-size)
        context (.getContext canvas "2d")
        image (.createImageData context canvas-size canvas-size)
        opaque (js/Uint8ClampedArray. (.-length rgba))]
    (loop [i 0]
      (when (< i (.-length rgba))
        (aset opaque i (aget rgba i))
        (aset opaque (+ i 1) (aget rgba (+ i 1)))
        (aset opaque (+ i 2) (aget rgba (+ i 2)))
        (aset opaque (+ i 3) 255)
        (recur (+ i 4))))
    (.set (.-data image) opaque)
    (.putImageData context image 0 0)
    (.toDataURL canvas "image/png")))

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
        _ (renderer/update-camera device (:camera-uniform-buffer system)
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

(defn- q8-effective [entry-count]
  ;; Semantic ids deliberately stride by 17, reproducing the Q8 sparse-id
  ;; pressure while transport slots remain dense 0..N-1.
  (into {}
        (map (fn [slot]
               [(* slot 17)
                {:affine containers/identity-affine
                 :flags 0
                 :layer 0
                 :stack-path [[(* slot 17) 0]]
                 :transport-slot slot}]))
        (range entry-count)))

(defn- run-q8-transport! [device containers-buffer]
  (let [rows
        (mapv (fn [entry-count]
                (let [effective (q8-effective entry-count)
                      receipt (renderer/write-containers! device containers-buffer effective)
                      expected-bytes (* entry-count renderer/affine-entry-bytes)]
                  (assoc receipt
                         :semantic-max-id (* 17 (dec entry-count))
                         :expected-bytes expected-bytes
                         :pass? (and (= entry-count (:entries receipt))
                                     (= expected-bytes (:bytes receipt))
                                     (= (dec entry-count) (:max-slot receipt))))))
              [1024 4096 16384])]
    {:entry-bytes renderer/affine-entry-bytes
     :transport "compact-read-only-storage"
     :rows rows
     :pass? (every? :pass? rows)}))

(defn- q5-transform []
  (let [ct (js/Math.cos q5-angle)
        st (js/Math.sin q5-angle)]
    {:affine [ct st (- st) ct 64.0 64.0]
     :flags 0
     :layer 1
     :stack-path [[17 1]]
     :transport-slot 1}))

(defn- run-q5-affine-boundary! [device q5-system q5-effective]
  (-> (render-system-bytes! device q5-system 1.0)
      (.then
       (fn [bytes]
         (let [covered
               (for [y (range canvas-size)
                     x (range canvas-size)
                     :let [coverage (pixel-red bytes x y)]
                     :when (pos? coverage)]
                 [x y coverage])
               xs (map first covered)
               ys (map second covered)
               rows
               (mapv (fn [[x y :as pixel]]
                       (let [sample [(+ x 0.5) (+ y 0.5)]
                             [lx ly] (containers/inverse-point q5-effective sample)
                             cpu-class (if (and (< (js/Math.abs lx) 27.0)
                                                (< (js/Math.abs ly) 15.0))
                                         "inside"
                                         "outside")
                             coverage (pixel-red bytes x y)]
                         {:pixel pixel
                          :sample-center sample
                          :local-point [lx ly]
                          :cpu-class cpu-class
                          :gpu-coverage-byte coverage
                          :pass? (and (= "inside" cpu-class) (pos? coverage))}))
                     q5-pinned-pixels)]
           {:extent "128x128-target/54x30-local-quad"
            :normalization "centered-local-coordinates"
            :zoom 1.0
            :backend "production-rich-rect-affine-storage"
            :regime "hand"
            :rotation-radians q5-angle
            :boundary-rule "canonical-inside-must-not-emit-zero"
            :coverage-bounds (when (seq covered)
                               [(apply min xs) (apply min ys)
                                (apply max xs) (apply max ys)])
            :covered-pixel-count (count covered)
            :rows rows
            :pass? (every? :pass? rows)})))))

(defn- boundary-pixels [^js rgba]
  (persistent!
   (loop [y 0
          acc (transient [])]
     (if (= y canvas-size)
       acc
       (recur (inc y)
              (loop [x 0
                     acc acc]
                (if (= x canvas-size)
                  acc
                  (let [coverage (pixel-red rgba x y)]
                    (recur (inc x)
                           (if (and (> coverage 8) (< coverage 247))
                             (conj! acc [x y coverage])
                             acc))))))))))

(defn- rounded-path-inside? [screen-x screen-y]
  ;; Exact CPU transcription of the production rich-rect SDF's mathematical
  ;; boundary, evaluated at a screen pixel center. Paint/border are irrelevant.
  (let [half (/ fixture-screen-size 2.0)
        px (- screen-x (+ fixture-screen-x half))
        py (- screen-y (+ fixture-screen-y half))
        [tl tr br bl] rounded-radii
        r (cond
            (and (> px 0.0) (> py 0.0)) br
            (> px 0.0) tr
            (> py 0.0) bl
            :else tl)
        r (min r half)
        qx (+ (- (js/Math.abs px) half) r)
        qy (+ (- (js/Math.abs py) half) r)
        outside (js/Math.sqrt (+ (* (max qx 0.0) (max qx 0.0))
                                 (* (max qy 0.0) (max qy 0.0))))
        distance (- (+ (min (max qx qy) 0.0) outside) r)]
    (<= distance 0.0)))

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
   assumes, the bearing/plane transform at the comparison seam."
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
     :receipt {:source source
               :shaped-rect-world [rx ry rw rh]
               :glyph-bounds bounds
               :zoom zoom
               :mapping "screen->world->shaped-quad->glyph-path"}}))

(defn- parity-receipt [mode rgba inside? cpu-inverse]
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

(defn- rich-rect [zoom]
  {:x (/ fixture-screen-x zoom)
   :y (/ fixture-screen-y zoom)
   :w (/ fixture-screen-size zoom)
   :h (/ fixture-screen-size zoom)
   :r 0.12 :g 0.52 :b 0.92 :a 1.0
   :corner-radii rounded-radii
   :border-widths [2.0 5.0 8.0 3.0]
   :border-color [0.96 0.32 0.18 1.0]
   :gradient [0.55 0.78 0.0 0.0]
   :gradient-color2 [0.62 0.18 0.86 1.0]})

(defn- parity-rect [zoom]
  {:x (/ fixture-screen-x zoom)
   :y (/ fixture-screen-y zoom)
   :w (/ fixture-screen-size zoom)
   :h (/ fixture-screen-size zoom)
   :r 1.0 :g 1.0 :b 1.0 :a 1.0
   :corner-radii rounded-radii})

(defn- glyph-lines [zoom]
  [[{:text "o"
     :x (/ glyph-screen-x zoom)
     :y (/ glyph-screen-baseline zoom)
     :size (/ glyph-screen-size zoom)
     :r 1.0 :g 1.0 :b 1.0 :a 1.0}]])

(defn- image-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- run-case!
  [{:keys [device rect-system msdf-system slug-system msdf-assets slug-assets curves]}
   {:keys [case-id zoom regime]}]
  (let [world-extent (/ fixture-screen-size zoom)
        rect-rich-system (renderer/update-rects device rect-system [(rich-rect zoom)])
        lines (glyph-lines zoom)
        font-size (/ glyph-screen-size zoom)
        msdf-glyph (first (filter #(= 111 (:unicode %))
                                  (get-in msdf-assets [:atlas :glyphs])))
        slug-glyph (first (filter #(= 111 (:unicode %))
                                  (get-in slug-assets [:slug :meta :glyphs])))
        msdf-instance (first (renderer/shape-text (first lines) font-size msdf-assets
                                                  :char-width 0.60))
        slug-instance (first (renderer/shape-text (first lines) font-size slug-assets
                                                  :char-width 0.60))
        msdf-probe (instance-path-probe curves msdf-instance (:planeBounds msdf-glyph)
                                        zoom "production-msdf-planeBounds")
        slug-probe (instance-path-probe curves slug-instance
                                        (or (:sampleBounds slug-glyph)
                                            (:planeBounds slug-glyph))
                                        zoom "production-slug-sampleBounds")
        msdf-system (renderer/update-text-data device msdf-system lines msdf-assets font-size
                                               :px-range 16.0 :sharpness 0.0
                                               :char-width 0.60)
        slug-system (renderer/update-text-data device slug-system lines slug-assets font-size
                                               :char-width 0.60)]
    (js/console.log "[W0-A] case-start" case-id "zoom" zoom)
    (-> (render-pair! device rect-rich-system zoom)
        (.then (fn [rich-pair]
                 (js/console.log "[W0-A] case-stage" case-id "sdf-rich")
                 {:rich-pair rich-pair}))
        (.then
         (fn [capture]
           (let [rect-parity-system (renderer/update-rects device rect-rich-system
                                                           [(parity-rect zoom)])]
             (-> (render-system-bytes! device rect-parity-system zoom)
                 (.then (fn [bytes]
                          (js/console.log "[W0-A] case-stage" case-id "sdf-parity")
                          (assoc capture :rect-parity-bytes bytes)))))))
        (.then
         (fn [capture]
           (-> (render-pair! device msdf-system zoom)
               (.then (fn [pair]
                        (js/console.log "[W0-A] case-stage" case-id "msdf")
                        (assoc capture :msdf-pair pair))))))
        (.then
         (fn [capture]
           (-> (render-pair! device slug-system zoom)
               (.then (fn [pair]
                        (js/console.log "[W0-A] case-stage" case-id "slug")
                        (assoc capture :slug-pair pair))))))
        (.then
         (fn [{:keys [rich-pair rect-parity-bytes msdf-pair slug-pair]}]
           (let [corner-x 24
                 corner-y 24
                 corner-gpu (pixel-red rect-parity-bytes corner-x corner-y)
                 corner-cpu (rounded-path-inside? (+ corner-x 0.5)
                                                  (+ corner-y 0.5))]
             (js/console.log "[W0-A] case-complete" case-id)
             {:case-id case-id
              :zoom zoom
              :regime regime
              :normalization "screen-constant"
              :shape-extent-world world-extent
              :canvas {:width canvas-size
                       :height canvas-size
                       :format color-format
                       :device-pixel-ratio (.-devicePixelRatio js/window)}
              :images [(image-record "sdf-rich-rect" case-id rich-pair)
                       (image-record "msdf" case-id msdf-pair)
                       (image-record "slug" case-id slug-pair)]
              :pick-parity
              [(parity-receipt "sdf-rounded-path"
                               rect-parity-bytes rounded-path-inside?
                               {:source "production-rich-rect-analytic-boundary"
                                :mapping "screen-pixel-center->rounded-rect-SDF"})
               (parity-receipt "msdf-dejavu-o-path"
                               (:bytes msdf-pair)
                               (:inside? msdf-probe)
                               (:receipt msdf-probe))
               (parity-receipt "slug-dejavu-o-path"
                               (:bytes slug-pair)
                               (:inside? slug-probe)
                               (:receipt slug-probe))]
              :current-product-pick-sentinel
              {:kind "axis-aligned-bounds-vs-rounded-visible-coverage"
               :pixel [corner-x corner-y]
               :product-bounds-pick? true
               :candidate-cpu-point-in-path? corner-cpu
               :gpu-coverage-byte corner-gpu
               :expected-divergence?
               (and (not corner-cpu) (< corner-gpu 128))}}))))))

(defn- selected-limits [^js limits]
  {:max-buffer-size (.-maxBufferSize limits)
   :max-uniform-buffer-binding-size (.-maxUniformBufferBindingSize limits)
   :max-storage-buffer-binding-size (.-maxStorageBufferBindingSize limits)
   :max-texture-dimension-2d (.-maxTextureDimension2D limits)
   :max-bind-groups (.-maxBindGroups limits)
   :max-vertex-buffers (.-maxVertexBuffers limits)})

(defn- adapter-information [^js adapter]
  (let [info (.-info adapter)]
    {:vendor (some-> info .-vendor)
     :architecture (some-> info .-architecture)
     :device (some-> info .-device)
     :description (some-> info .-description)
     :features (vec (array-seq (js/Array.from (.-features adapter))))
     :limits (selected-limits (.-limits adapter))}))

(defn- shader-digests []
  (let [entries [["rect-vertex" renderer/rect-vertex-shader]
                 ["rect-fragment" renderer/rect-fragment-shader]
                 ["msdf-vertex" renderer/text-vertex-shader]
                 ["msdf-fragment" renderer/text-fragment-shader]
                 ["slug-vertex" renderer/slug-vertex-shader]
                 ["slug-fragment" renderer/slug-fragment-shader]]]
    (-> (promise-mapv (fn [[label source]]
                        (.then (sha256-string source)
                               (fn [digest] [label digest])))
                      entries)
        (.then #(into {} %)))))

(defn- load-t1-provider [font-config]
  (let [source (fn [config]
                 {:id (:id config)
                  :revision (or (:faceRevision config) (:font config))
                  :url (str "/fonts/" (:font config))
                  :variations (or (:variations config) {})})]
    (text-shaper/load-provider!
      (into [(source font-config)] (map source) (:fallbacks font-config))
      {:features (:features font-config)
       :language "und"
       :tab-columns (:tabColumns font-config)})))

(defn- t1-layout-receipt [provider]
  (let [text "AV office e\u0301\tسلام\nɐ"
        result (tl/layout {:text text :provider provider
                           :font-size 19 :line-height 24
                           :origin [10 20] :baseline-offset 19
                           :clip {:left 12 :right 180 :top 20 :bottom 68}
                           :source-id :verifier/t1 :source-revision 1
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
                            :source-id :verifier/t1-variable-axis}))
              [:metrics :advance])))
        narrow-advance (variable-advance 75)
        wide-advance (variable-advance 125)
        receipt {:layout-id (:layout/id result)
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
                 :regime (:regime result)}
        pass? (and (every? #{(:layout/id result)} (:reader-layout-ids receipt))
                   (= 2 (:lines receipt))
                   (:rtl? receipt) (:fallback? receipt) (:tab? receipt)
                   (not (zero? (:variable-axis-delta receipt)))
                   (= {:wght 400 :wdth 100} (:variations receipt)))]
    (when-not pass?
      (throw (ex-info "T1 browser layout receipt failed." receipt)))
    (assoc receipt :pass true)))

(defn ^:export run-verifier! []
  (js/console.log "[W0-A] init-start")
  (when-not (and (.-isSecureContext js/window)
                 (exists? js/navigator.gpu))
    (throw (js/Error. "W0-A requires a secure origin with WebGPU")))
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js adapter]
         (when-not adapter
           (throw (js/Error. "W0-A could not acquire a WebGPU adapter")))
         (js/console.log "[W0-A] init-adapter")
         (-> (.requestDevice adapter)
             (.then
              (fn [^js device]
                (js/console.log "[W0-A] init-device")
                (-> (fonts/load-font-manifest-async)
                    (.then
                     (fn [manifest]
                       (js/console.log "[W0-A] init-font-manifest")
                       (let [font-config (first (filter #(= "dejavu-sans-mono" (:id %))
                                                       (:fonts manifest)))
                             t1-font-config (first (filter #(= "ubuntu-sans-variable" (:id %))
                                                          (:fonts manifest)))]
                         (when-not (and font-config t1-font-config)
                           (throw (js/Error. "A verifier font is absent from manifest")))
                         (-> (js/Promise.all
                               #js [(fonts/load-font-assets font-config)
                                    (load-t1-provider t1-font-config)])
                             (.then
                              (fn [font-values]
                                (let [slug-assets (aget font-values 0)
                                      t1-receipt (t1-layout-receipt (aget font-values 1))]
                                (js/console.log "[W0-A] init-font-assets")
                                (let [msdf-assets (assoc slug-assets :backend :msdf)
                                      camera-buffer (renderer/create-camera-buffer device nil)
                                      containers-buffer (renderer/create-containers-buffer device nil)
                                      q8-transport (run-q8-transport! device containers-buffer)
                                      _ (js/console.log "[W0-A] init-shared-buffers")
                                      rect-system (do
                                                    (js/console.log "[W0-A] init-rect-pipeline-start")
                                                    (let [system
                                                          (renderer/init-rect-system
                                                           device color-format camera-buffer
                                                           :initial-capacity 1
                                                           :containers-buffer containers-buffer)]
                                                      (js/console.log "[W0-A] init-rect-pipeline-complete")
                                                      ;; Text systems expose the shared camera on their
                                                      ;; state map; the rect system only captures it in
                                                      ;; its bind group. Carry the same production buffer
                                                      ;; as verifier metadata so capture can update it.
                                                      (assoc system :camera-uniform-buffer camera-buffer)))
                                      q5-camera-buffer (renderer/create-camera-buffer device nil)
                                      q5-buffer (renderer/create-containers-buffer device nil)
                                      q5-effective (q5-transform)
                                      _ (renderer/write-containers!
                                         device q5-buffer
                                         {0 {:affine containers/identity-affine
                                             :flags 0 :layer 0 :stack-path [[0 0]]
                                             :transport-slot 0}
                                          17 q5-effective})
                                      q5-base (-> (renderer/init-rect-system
                                                   device color-format q5-camera-buffer
                                                   :initial-capacity 1
                                                   :containers-buffer q5-buffer)
                                                  (assoc :camera-uniform-buffer q5-camera-buffer))
                                      q5-system (renderer/update-rects
                                                 device q5-base
                                                 [{:x -27.0 :y -15.0 :w 54.0 :h 30.0
                                                   :r 1.0 :g 1.0 :b 1.0 :a 1.0
                                                   :corner-radii [0.0 0.0 0.0 0.0]
                                                   :container-idx 1}])
                                      msdf-system (do
                                                    (js/console.log "[W0-A] init-msdf-pipeline-start")
                                                    (let [system
                                                          (renderer/init-text-system
                                                           device color-format camera-buffer msdf-assets
                                                           :initial-capacity 1
                                                           :containers-buffer containers-buffer)]
                                                      (js/console.log "[W0-A] init-msdf-pipeline-complete")
                                                      system))
                                      slug-system (do
                                                    (js/console.log "[W0-A] init-slug-pipeline-start")
                                                    (let [system
                                                          (renderer/init-text-system
                                                           device color-format camera-buffer slug-assets
                                                           :initial-capacity 1
                                                           :containers-buffer containers-buffer)]
                                                      (js/console.log "[W0-A] init-slug-pipeline-complete")
                                                      system))
                                      curves (do
                                               (js/console.log "[W0-A] init-curve-decode-start")
                                               (let [decoded (decode-glyph-curves slug-assets 111)]
                                                 (js/console.log "[W0-A] init-curve-decode-complete"
                                                                 (count decoded))
                                                 decoded))
                                      harness {:device device
                                               :rect-system rect-system
                                               :msdf-system msdf-system
                                               :slug-system slug-system
                                               :msdf-assets msdf-assets
                                               :slug-assets slug-assets
                                               :curves curves}]
                                  (-> (js/Promise.all
                                       #js [(promise-mapv (partial run-case! harness) zoom-cases)
                                            (shader-digests)
                                            (run-q5-affine-boundary! device q5-system q5-effective)])
                                      (.then
                                       (fn [values]
                                         {:schema-version 2
                                          :verifier "softland-render-engine-w0-a"
                                          :production-renderer? true
                                          :product-server-used? false
                                          :secure-context? (.-isSecureContext js/window)
                                          :user-agent (.-userAgent js/navigator)
                                          :adapter (adapter-information adapter)
                                          :device-limits (selected-limits (.-limits device))
                                          :canvas {:width canvas-size
                                                   :height canvas-size
                                                   :device-pixel-ratio (.-devicePixelRatio js/window)
                                                   :color-format color-format}
                                          :font {:id (:id font-config)
                                                 :msdf-atlas (:atlas font-config)
                                                 :msdf-metrics (:metrics font-config)
                                                 :slug (:slug font-config)}
                                          :decoded-slug-curve-count (count curves)
                                          :shader-digests (aget values 1)
                                          :q8-transport q8-transport
                                          :t1-layout t1-receipt
                                          :q5-affine-boundary (aget values 2)
                                          :cases (aget values 0)})))))))))))))))))))

(defn ^:export start! []
  (js/console.log "[W0-A] start")
  (set! (.-__renderVerifierDone js/window) false)
  ;; Yield once so CDP can publish the boot marker before any browser/driver
  ;; implementation performs synchronous pipeline compilation.
  (js/setTimeout
   (fn []
     (js/console.log "[W0-A] scheduled-callback")
     (try
       (-> (run-verifier!)
           (.then
            (fn [result]
              (set! (.-__renderVerifierResult js/window) (clj->js result))
              (set! (.-__renderVerifierDone js/window) true)))
           (.catch
            (fn [error]
              (set! (.-__renderVerifierResult js/window)
                    #js {:fatal (str error)
                         :stack (.-stack error)})
              (set! (.-__renderVerifierDone js/window) true))))
       (catch :default error
         (set! (.-__renderVerifierResult js/window)
               #js {:fatal (str error)
                    :stack (.-stack error)})
         (set! (.-__renderVerifierDone js/window) true))))
   0))
