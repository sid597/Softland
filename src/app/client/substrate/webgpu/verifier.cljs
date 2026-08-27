(ns app.client.substrate.webgpu.verifier
  "Permanent W0-A browser half.

   This harness deliberately instantiates the production renderer's public
   pipeline/update functions and repository font assets. It owns only capture,
   comparison inputs, a candidate geometry-contract probe, and receipts. It is
   not a product renderer and it never substitutes lookalike WGSL.

   CPU geometry probes are independent readers of the production path and
   glyph pipelines."
  (:require [clojure.string :as str]
            [app.client.substrate.chrome-material :as chrome-material]
            [app.client.substrate.image-material :as image-material]
            [app.client.substrate.path-material :as path-material]
            [app.client.substrate.path-tessellation :as path-tessellation]
            [app.client.substrate.region3d-material :as region3d-material]
            [app.client.substrate.region3d-oracle :as region3d-oracle]
            [app.client.substrate.region3d-placement :as region3d-placement]
            [app.client.substrate.region3d-scene :as region3d-scene]
            [app.client.substrate.scene-color :as scene-color]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.substrate.webgpu.chrome-gpu :as chrome-gpu]
            [app.client.substrate.webgpu.compositor-gpu :as compositor-gpu]
            [app.client.substrate.webgpu.path-gpu :as path-gpu]
            [app.client.substrate.webgpu.region-bindings :as region-bindings]
            [app.client.substrate.webgpu.region3d-gpu :as region3d-gpu]
            [app.client.substrate.webgpu.region3d-placement-gpu :as region3d-placement-gpu]
            [app.client.substrate.webgpu.renderer :as renderer]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.runtime.fonts :as fonts]
            [app.client.workspace.text-layout :as tl]
            [app.client.workspace.text-shaper :as text-shaper]))

(def ^:private canvas-size 128)
(def ^:private color-format "rgba8unorm")
(def ^:private glyph-screen-x 34.0)
(def ^:private glyph-screen-baseline 96.0)
(def ^:private glyph-screen-size 80.0)

(def ^:private zoom-cases
  [{:case-id "legal-min-z0p01" :zoom 0.01 :regime "legal-envelope-sentinel"}
   {:case-id "default-min-z0p1" :zoom 0.1 :regime "floor-default-clamp"}
   {:case-id "default-unit-z1" :zoom 1.0 :regime "floor-default-clamp"}
   {:case-id "default-max-z8" :zoom 8.0 :regime "floor-default-clamp"}
   {:case-id "legal-log-z10" :zoom 10.0 :regime "legal-envelope-sentinel"}
   {:case-id "legal-log-z100" :zoom 100.0 :regime "legal-envelope-sentinel"}
   {:case-id "legal-max-z1000" :zoom 1000.0 :regime "legal-envelope-sentinel"}])

(def ^:private image-fixtures
  [{:filename "atlas-opaque-srgb.png" :digest "6e744e448e1480b510b3cd8aef86b5e2c9b8367bbafdb3e1586946c779b265eb"
    :width 32 :height 24 :color-tag :srgb :alpha-association :opaque}
   {:filename "clip-stripes-srgb.png" :digest "8000e248f9550d364286a5b873a77813040762998f087f7521f0900532730c15"
    :width 40 :height 32 :color-tag :srgb :alpha-association :opaque}
   {:filename "coverage-white-srgb.png" :digest "71bafd65a2358f69ab1e3086058fa4180760a54f87b0b58cd75579b3ed74d81e"
    :width 4 :height 4 :color-tag :srgb :alpha-association :opaque}
   {:filename "alpha-reference-straight.png" :digest "b38bcacf6298ec057c4e1b03fefa2116440d8bdd2ea31bb79dcd94396856d608"
    :width 160 :height 160 :color-tag :srgb :alpha-association :straight}
   {:filename "dedicated-alpha-straight.png" :digest "c859086c6a2cd8171f4cd429fd9d62529dd97c53e5ae9f131ce216b90af7e402"
    :width 160 :height 96 :color-tag :srgb :alpha-association :straight}
   {:filename "dedicated-alpha-premultiplied.png" :digest "28840b6c70cb6dcd01f2f4ae0304e9c38070fcb2c1505f25d03111b158b6f30b"
    :width 160 :height 96 :color-tag :srgb :alpha-association :premultiplied}
   {:filename "profiled-linear-rgb.png" :digest "8e0358b9e3830abfdbff58faf3a19907d3fd2c2fd4a79d4cc6156acbb09fabea"
    :width 8 :height 8 :color-tag :embedded-profile :alpha-association :opaque}
   {:filename "seam-byte-srgb.png" :digest "836e03f287a153693eecd3b75e4c8ffefbc38407f1e7784a4c2a8f4471f6650a"
    :width 8 :height 8 :color-tag :srgb :alpha-association :opaque}])

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
  [{:keys [device slug-system slug-assets curves]}
   {:keys [case-id zoom regime]}]
  (let [lines (glyph-lines zoom)
        font-size (/ glyph-screen-size zoom)
        slug-glyph (first (filter #(= 111 (:unicode %))
                                  (get-in slug-assets [:slug :meta :glyphs])))
        slug-instance (first (renderer/shape-text (first lines) font-size slug-assets
                                                  :char-width 0.60))
        slug-probe (instance-path-probe curves slug-instance
                                        (or (:sampleBounds slug-glyph)
                                            (:planeBounds slug-glyph))
                                        zoom "production-slug-sampleBounds")
        slug-system (renderer/update-text-data device slug-system lines slug-assets font-size
                                               :char-width 0.60)]
    (js/console.log "[W0-A] case-start" case-id "zoom" zoom)
    (->
        (render-pair! device slug-system zoom)
        (.then
         (fn [slug-pair]
           (js/console.log "[W0-A] case-complete" case-id)
           {:case-id case-id
            :zoom zoom
            :regime regime
            :normalization "screen-constant"
            :shape-extent-world (/ glyph-screen-size zoom)
            :canvas {:width canvas-size
                     :height canvas-size
                     :format color-format
                     :device-pixel-ratio (.-devicePixelRatio js/window)}
            :images [(image-record "slug" case-id slug-pair)]
            :pick-parity
            [(parity-receipt "slug-dejavu-o-path"
                               (:bytes slug-pair)
                               (:inside? slug-probe)
                               (:receipt slug-probe))]})))))

(defn- run-ubuntu-mixed-case!
  [device ubuntu-system ubuntu-assets]
  (let [text "Aɐ"
        font-size 56.0
        line-height 68.0
        layout-result (tl/layout {:text text
                                  :provider (:layout-provider ubuntu-assets)
                                  :font-size font-size
                                  :line-height line-height
                                  :origin [24.0 18.0]
                                  :baseline-offset font-size
                                  :source-id :verifier/ubuntu-slug-mixed-face
                                  :source-revision 1})
        glyphs (:glyphs (tl/paint-result layout-result))
        face-ids (->> glyphs (map :font-id) distinct sort vec)
        face-revisions (->> glyphs (map :font-revision) distinct sort vec)
        expected-faces ["noto-sans-regular" "ubuntu-sans-variable"]
        _ (when-not (= expected-faces face-ids)
            (throw (ex-info "Ubuntu Slug golden did not shape through both faces."
                            {:text text :expected expected-faces :actual face-ids})))
        lines (->> (tl/line-paint-ops
                     layout-result
                     {:size font-size :r 1.0 :g 1.0 :b 1.0 :a 1.0})
                   (mapv vector))
        ubuntu-system (renderer/update-text-data
                        device ubuntu-system lines ubuntu-assets font-size
                        :line-height line-height)]
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
              :regime "default-font-mixed-face"
              :normalization "material-fixed"
              :shape-extent-world font-size
              :images [(image-record "slug" "ubuntu-mixed-face" pair)]}]})))))

;; --- IMAGE-ATOM Package 2 ---------------------------------------------------

(defn- fetch-image-corpus! []
  (-> (promise-mapv
       (fn [{:keys [filename] :as fixture}]
         (-> (js/fetch (str "/images/" filename))
             (.then (fn [response]
                      (when-not (.-ok response)
                        (throw (js/Error.
                                (str "Image fixture fetch failed: " filename))))
                      (.arrayBuffer response)))
             (.then (fn [bytes]
                      (assoc fixture :bytes bytes
                             :source
                             {:image/digest (:digest fixture)
                              :image/color-tag (:color-tag fixture)
                              :image/width (:width fixture)
                              :image/height (:height fixture)
                              :image/bytes-route
                              {:kind :fixture
                               :path (str "images/" filename)}
                              :image/ingress-receipt image-material/ingress-receipt
                              :image/alpha-association
                              (:alpha-association fixture)})))))
       image-fixtures)
      (.then (fn [rows] (into {} (map (juxt :filename identity)) rows)))))

(defn- ingress-corpus! [image-system corpus]
  (promise-mapv
   (fn [{:keys [source bytes]}]
     (renderer/register-image-source! image-system source bytes))
   (mapv corpus (map :filename image-fixtures))))

(defn- image-op
  [id digest x y width height & {:keys [uv opacity tint]
                                 :or {uv [0.0 0.0 1.0 1.0]
                                      opacity 1.0
                                      tint [1.0 1.0 1.0 1.0]}}]
  {:id id :x x :y y :w width :h height
   :image/digest digest :image/uv uv
   :image/opacity opacity :image/tint tint
   :container-idx 0})

(defn- render-image-bytes!
  [^js device image-system ops zoom
   & {:keys [clear-value intermediate-copy?]
      :or {clear-value {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
           intermediate-copy? false}}]
  (let [row-bytes (* canvas-size 4)
        candidate? (get-in image-system [:scene-color :enabled?])
        view-format (if candidate? "rgba8unorm-srgb" "rgba8unorm")
        texture-options {:size {:width canvas-size :height canvas-size
                                :depthOrArrayLayers 1}
                         :format "rgba8unorm"
                         :viewFormats ["rgba8unorm-srgb"]
                         :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                        js/GPUTextureUsage.COPY_SRC
                                        js/GPUTextureUsage.COPY_DST)}
        target (.createTexture device (clj->js texture-options))
        presentation (when intermediate-copy?
                       (.createTexture device (clj->js texture-options)))
        read-buffer (.createBuffer
                     device
                     (clj->js {:size (* row-bytes canvas-size)
                               :usage (bit-or js/GPUBufferUsage.COPY_DST
                                              js/GPUBufferUsage.MAP_READ)}))
        camera (js/Float32Array. 6)
        _ (renderer/update-camera device (:camera-buffer image-system)
                                  camera 0.0 0.0 zoom
                                  canvas-size canvas-size)
        _ (renderer/prepare-image-frame! image-system ops)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format view-format}))
                          :clearValue clear-value
                          :loadOp "clear" :storeOp "store"}]}))]
    (renderer/draw-image-runs! pass image-system 0 (count ops))
    (.end pass)
    (when intermediate-copy?
      (.copyTextureToTexture encoder
                             (clj->js {:texture target})
                             (clj->js {:texture presentation})
                             (clj->js {:width canvas-size
                                       :height canvas-size})))
    (.copyTextureToBuffer
     encoder
     (clj->js {:texture (or presentation target)})
     (clj->js {:buffer read-buffer :bytesPerRow row-bytes
               :rowsPerImage canvas-size})
     (clj->js {:width canvas-size :height canvas-size
               :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then
         (fn [_]
           (let [copy (js/Uint8Array.
                       (js/Uint8Array. (.getMappedRange read-buffer)))]
             (.unmap read-buffer)
             (.destroy read-buffer)
             (.destroy target)
             (when presentation (.destroy presentation))
             copy))))))

(defn- render-image-pair! [device image-system ops zoom clear-value]
  (-> (render-image-bytes! device image-system ops zoom
                           :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-image-bytes! device image-system ops zoom
                                  :clear-value clear-value)
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
                        :byte-identical? (= (aget hashes 0)
                                            (aget hashes 1))}))))))))))

(defn- image-atom-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-image-atom-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- run-image-golden-case!
  [device candidate-system corpus {:keys [case-id zoom regime]}]
  (let [screen->world #(/ % zoom)
        opaque (:digest (get corpus "atlas-opaque-srgb.png"))
        alpha (:digest (get corpus "alpha-reference-straight.png"))
        clipped (:digest (get corpus "clip-stripes-srgb.png"))
        clear {:r 0.018 :g 0.055 :b 0.09 :a 1.0}
        opaque-op (image-op :golden/opaque opaque
                            (screen->world 24.0) (screen->world 24.0)
                            (screen->world 80.0) (screen->world 80.0))
        alpha-op (image-op :golden/alpha alpha
                           (screen->world 24.0) (screen->world 24.0)
                           (screen->world 80.0) (screen->world 80.0))
        ;; T15: this is the post-clamp placement/crop pair for an original
        ;; 80px quad clipped by 20px on each x edge.
        clipped-op (image-op :golden/clipped clipped
                             (screen->world 44.0) (screen->world 24.0)
                             (screen->world 40.0) (screen->world 80.0)
                             :uv [0.25 0.0 0.75 1.0])]
    (-> (render-image-pair! device candidate-system [opaque-op] zoom clear)
        (.then
         (fn [opaque-pair]
           (-> (render-image-pair! device candidate-system [alpha-op] zoom clear)
               (.then (fn [alpha-pair] [opaque-pair alpha-pair])))))
        (.then
         (fn [[opaque-pair alpha-pair]]
           (-> (render-image-pair! device candidate-system [clipped-op] zoom clear)
               (.then
                (fn [clipped-pair]
                  {:case-id case-id :zoom zoom :regime regime
                   :normalization "screen-constant"
                   :shape-extent-world (/ 80.0 zoom)
                   :op-counts {:opaque 1 :alpha 1 :partially-clipped 1}
                   :images [(image-atom-record "opaque-atlas" case-id
                                               opaque-pair)
                            (image-atom-record "alpha-dedicated" case-id
                                               alpha-pair)
                            (image-atom-record "partially-clipped" case-id
                                               clipped-pair)]}))))))))

(defn- image-product-inside? [op zoom screen-x screen-y]
  (image-material/half-open-hit?
   (select-keys op [:x :y :w :h])
   [(/ screen-x zoom) (/ screen-y zoom)]))

(defn- image-parity-receipt [extent-id op zoom rgba]
  (let [boundary (boundary-pixels rgba)
        rows (mapv
              (fn [[x y coverage]]
                (let [inside? (image-product-inside?
                               op zoom (+ x 0.5) (+ y 0.5))
                      gpu-class (cond (< coverage 128) :outside
                                      (> coverage 128) :inside
                                      :else :half)
                      decisive? (not= :half gpu-class)
                      match? (when decisive?
                               (= inside? (= :inside gpu-class)))]
                  {:pixel [x y] :gpu-coverage-byte coverage
                   :product-inside? inside? :gpu-class gpu-class
                   :decisive? decisive? :match? match?}))
              boundary)
        decisive (filterv :decisive? rows)
        mismatches (filterv #(and (:decisive? %) (not (:match? %))) rows)
        pass? (and (pos? (count rows)) (pos? (count decisive))
                   (empty? mismatches))]
    {:extent extent-id :zoom zoom
     :boundary-pixel-count (count rows)
     :decisive-count (count decisive)
     :half-count (- (count rows) (count decisive))
     :mismatch-count (count mismatches)
     :hit-slop 0.0 :slop-path-ran? false
     :product-route "image half-open quad"
     :candidate-route "production image ramped quad"
     :agreement? pass? :pass? pass?
     :first-mismatches (subvec mismatches 0 (min 16 (count mismatches)))}))

(defn- run-image-parity!
  [device seam-system corpus]
  (let [digest (:digest (get corpus "coverage-white-srgb.png"))]
    (promise-mapv
     (fn [{:keys [case-id zoom]}]
       (promise-mapv
        (fn [[extent-id width height]]
          (let [op (image-op [:parity extent-id case-id] digest
                             (/ 24.25 zoom) (/ 24.25 zoom) width height)]
            (-> (render-image-bytes! device seam-system [op] zoom)
                (.then #(image-parity-receipt extent-id op zoom %)))))
        [["screen-constant" (/ 80.0 zoom) (/ 80.0 zoom)]
         ["world-256" 256.0 256.0]]))
     zoom-cases)))

(defn- byte-delta [^js left ^js right]
  (loop [index 0 maximum 0]
    (if (= index (.-length left))
      maximum
      (recur (inc index)
             (max maximum
                  (js/Math.abs (- (aget left index) (aget right index))))))))

(defn- pixel-rgba [^js bytes x y]
  (let [offset (* 4 (+ x (* y canvas-size)))]
    [(aget bytes offset) (aget bytes (+ offset 1))
     (aget bytes (+ offset 2)) (aget bytes (+ offset 3))]))

(defn- bitmap-pixel [^js bitmap x y]
  (let [canvas (js/OffscreenCanvas. (.-width bitmap) (.-height bitmap))
        context (.getContext canvas "2d" #js {:willReadFrequently true})
        _ (.drawImage context bitmap 0 0)
        data (.-data (.getImageData context x y 1 1))]
    [(aget data 0) (aget data 1) (aget data 2) (aget data 3)]))

(defn- icc-decode-receipt! [corpus]
  (let [bytes (:bytes (get corpus "profiled-linear-rgb.png"))
        blob (js/Blob. #js [bytes] #js {:type "image/png"})]
    (-> (js/Promise.all
         #js [(js/createImageBitmap
               blob #js {:colorSpaceConversion "none"
                          :premultiplyAlpha "none"})
              (js/createImageBitmap
               blob #js {:colorSpaceConversion "default"
                          :premultiplyAlpha "none"})])
        (.then
         (fn [bitmaps]
           (let [none (aget bitmaps 0)
                 converted (aget bitmaps 1)
                 rows (mapv
                       (fn [[pixel expected-none expected-converted]]
                         (let [[x y] pixel
                               none-rgba (bitmap-pixel none x y)
                               converted-rgba (bitmap-pixel converted x y)
                               delta (apply max
                                            (map #(js/Math.abs (- %1 %2))
                                                 (take 3 converted-rgba)
                                                 expected-converted))]
                           {:pixel pixel :none none-rgba
                            :default converted-rgba
                            :expected-none expected-none
                            :expected-default expected-converted
                            :pass? (and (= expected-none (take 3 none-rgba))
                                        (<= delta 2))}))
                       [[[1 1] [40 43 46] [110 114 118]]
                        [[4 2] [144 147 150] [198 200 202]]])]
             (.close none)
             (.close converted)
             {:fixture "profiled-linear-rgb.png" :rows rows
              :pass? (every? :pass? rows)}))))))

(defn- srgb->linear [value]
  (let [v (/ value 255.0)]
    (if (<= v 0.04045) (/ v 12.92)
        (js/Math.pow (/ (+ v 0.055) 1.055) 2.4))))

(defn- linear->srgb-byte [value]
  (let [v (max 0.0 (min 1.0 value))
        encoded (if (<= v 0.0031308) (* v 12.92)
                    (- (* 1.055 (js/Math.pow v (/ 1.0 2.4))) 0.055))]
    (js/Math.round (* encoded 255.0))))

(defn- run-color-receipts!
  [device candidate-system seam-system corpus]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        alpha-digest (:digest (get corpus "alpha-reference-straight.png"))
        profile-digest (:digest (get corpus "profiled-linear-rgb.png"))
        straight-digest (:digest (get corpus "dedicated-alpha-straight.png"))
        premultiplied-digest
        (:digest (get corpus "dedicated-alpha-premultiplied.png"))
        seam-digest (:digest (get corpus "seam-byte-srgb.png"))
        quad (fn [id digest]
               (image-op id digest 24.0 24.0 80.0 80.0))
        alpha-op (quad :color/alpha alpha-digest)
        profile-op (quad :color/profile profile-digest)
        straight-op (quad :color/straight straight-digest)
        premultiplied-op (quad :color/premultiplied premultiplied-digest)
        seam-op (image-op :color/seam seam-digest 24.0 24.0 64.0 64.0)
        seam-profile-op (image-op :color/seam-profile profile-digest
                                  24.0 24.0 8.0 8.0)]
    (-> (render-image-bytes! device candidate-system [alpha-op] 1.0
                             :clear-value clear)
        (.then
         (fn [alpha-bytes]
           (let [alpha (/ 128.0 255.0)
                 expected (mapv
                           (fn [source background]
                             (linear->srgb-byte
                              (+ (* (srgb->linear source) alpha)
                                 (* background (- 1.0 alpha)))))
                           [200 80 40] [0.04 0.18 0.35])
                 actual (subvec (vec (pixel-rgba alpha-bytes 64 64)) 0 3)
                 reference-delta (apply max (map #(js/Math.abs (- %1 %2))
                                                 expected actual))
                 edge (pixel-rgba alpha-bytes 23 64)]
             {:source-over {:expected expected :actual actual
                            :max-byte-delta reference-delta
                            :pass? (<= reference-delta 3)}
              :non-black-fringe {:edge-pixel edge
                                 :pass? (every? pos? (take 3 edge))}})))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device candidate-system [profile-op] 1.0)
               (.then
                (fn [direct]
                  (-> (render-image-bytes!
                       device candidate-system [profile-op] 1.0
                       :intermediate-copy? true)
                      (.then
                       (fn [copied]
                         (assoc receipt :presentation
                                {:direct-vs-intermediate-max-byte-delta
                                 (byte-delta direct copied)
                                 :pass? (zero? (byte-delta direct copied))})))))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device candidate-system [straight-op] 1.0)
               (.then
                (fn [straight]
                  (-> (render-image-bytes!
                       device candidate-system [premultiplied-op] 1.0)
                      (.then
                       (fn [premultiplied]
                         (assoc receipt :alpha-association
                                {:max-byte-delta
                                 (byte-delta straight premultiplied)
                                 :pass? (<= (byte-delta straight premultiplied)
                                            4)}
                                :alpha-association-bytes premultiplied)))))))))
        (.then
         (fn [receipt]
           (let [tracker (gpu-budget/create-tracker nil)
                 camera (renderer/create-camera-buffer device tracker)
                 containers-buffer (renderer/create-containers-buffer
                                    device tracker)
                 system (renderer/init-image-system
                         device "rgba8unorm-srgb" camera containers-buffer
                         :tracker tracker
                         :scene-color (scene-color/scene-color true))
                 row (get corpus "dedicated-alpha-premultiplied.png")
                 mistagged-source (assoc (:source row)
                                         :image/alpha-association :straight)]
             (-> (renderer/register-image-source! system mistagged-source
                                                  (:bytes row))
                 (.then (fn [_]
                          (render-image-bytes! device system
                                               [premultiplied-op] 1.0)))
                 (.then
                  (fn [mistagged]
                    (let [delta (byte-delta
                                 (:alpha-association-bytes receipt)
                                 mistagged)]
                      (renderer/destroy-image-system! system)
                      (-> receipt
                          (dissoc :alpha-association-bytes)
                          (assoc :mistagged-alpha
                                 {:max-byte-delta delta
                                  :pass? (> delta 8)})))))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device seam-system [seam-op] 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 56 56))
                                       0 3)]
                    (assoc receipt :seam-off
                           {:expected [64 128 192] :actual actual
                            :transfer-count 0 :pass? (= [64 128 192] actual)})))))))
        (.then
         (fn [receipt]
           (-> (render-image-bytes! device seam-system [seam-profile-op] 1.0)
               (.then
                (fn [seam-bytes]
                  (let [actual (subvec (vec (pixel-rgba seam-bytes 25 25))
                                       0 3)
                        row (get-in (renderer/image-ingress-receipt seam-system)
                                    [:rows profile-digest])]
                    (assoc receipt :seam-off-profile
                           {:fixture "profiled-linear-rgb.png"
                            :pixel [1 1]
                            ;; The 0.5px ramped hull makes this a pinned
                            ;; encoded-space bilinear sample of the raw
                            ;; no-conversion texels, not the decode probe's
                            ;; exact texel-center value.
                            :expected [51 54 57]
                            :actual actual
                            :ingress-transfers (:ingress-transfers row)
                            :presentation-encodes (:presentation-encodes row)
                            :pass? (and (= [51 54 57] actual)
                                        (zero? (:ingress-transfers row))
                                        (zero? (:presentation-encodes row)))})))))))
        (.then
         (fn [receipt]
           (-> (icc-decode-receipt! corpus)
               (.then
                (fn [icc]
                  (let [rows (:rows (renderer/image-ingress-receipt
                                     candidate-system))
                        transfer-rows (vals rows)
                        counts-ok? (every?
                                    #(and (= 1 (:ingress-transfers %))
                                          (= 1 (:presentation-encodes %)))
                                    (filter #(= :ok (:status %)) transfer-rows))]
                    (assoc receipt
                           :icc-decode icc
                           :candidate-transfer-counts
                           {:ingress 1 :presentation 1
                            :rows-pass? counts-ok?})))))))
        (.then
         (fn [receipt]
           (assoc receipt :pass?
                  (every? :pass?
                          [(:source-over receipt)
                           (:non-black-fringe receipt)
                           (:presentation receipt)
                           (:alpha-association receipt)
                           (:mistagged-alpha receipt)
                           (:seam-off receipt)
                           (:seam-off-profile receipt)
                           (:icc-decode receipt)])))))))

(defn- request-replacement-device! []
  ;; Dawn consumes an adapter after its first device.  A fresh adapter request
  ;; is therefore part of the real replacement-device lifecycle receipt.
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js replacement-adapter]
         (when-not replacement-adapter
           (throw (js/Error. "Image lifecycle could not acquire replacement adapter")))
         (.requestDevice replacement-adapter)))))

(defn- run-lifecycle-receipt!
  [device candidate-system corpus]
  (let [tracker (gpu-budget/create-tracker nil)
        first-texture (.createTexture
                       device
                       (clj->js {:size {:width 8 :height 4}
                                 :mipLevelCount 4 :format "rgba8unorm"
                                 :usage js/GPUTextureUsage.TEXTURE_BINDING}))
        second-texture (.createTexture
                        device
                        (clj->js {:size {:width 8 :height 4}
                                  :mipLevelCount 4 :format "rgba8unorm"
                                  :usage js/GPUTextureUsage.TEXTURE_BINDING}))
        registered (gpu-budget/register-texture!
                    tracker first-texture "image/budget-probe"
                    :width 8 :height 4 :format "rgba8unorm"
                    :mip-level-count 4)
        replaced (gpu-budget/replace-texture!
                  tracker first-texture second-texture "image/budget-probe"
                  :width 8 :height 4 :format "rgba8unorm"
                  :mip-level-count 4 :reason :image-budget-probe)
        expected-bytes (image-material/texture-bytes 8 4)
        tiny-tracker (gpu-budget/create-tracker nil)
        tiny-camera (renderer/create-camera-buffer device tiny-tracker)
        tiny-containers (renderer/create-containers-buffer device tiny-tracker)
        tiny-system (renderer/init-image-system
                     device "rgba8unorm-srgb" tiny-camera tiny-containers
                     :tracker tiny-tracker :budget-cap-bytes 1
                     :scene-color (scene-color/scene-color true))
        atlas-row (get corpus "atlas-opaque-srgb.png")
        atlas-digest (:digest atlas-row)
        registered-digests
        (mapv :digest (mapv corpus (map :filename image-fixtures)))
        unknown-digest (apply str (repeat 64 "f"))
        unavailable-op (image-op :lifecycle/unavailable unknown-digest
                                 0 0 8 8)
        over-budget-op (image-op :lifecycle/over-budget atlas-digest
                                0 0 8 8)]
    ;; IMAGE-ATOM T10/T11: both placeholder reasons are driven through the
    ;; actual executor and byte-compared; painting cannot erase refusal cause.
    (-> (render-image-bytes! device candidate-system [unavailable-op] 1.0)
        (.then
         (fn [unavailable-bytes]
           (-> (renderer/register-image-source! tiny-system
                                                (:source atlas-row)
                                                (:bytes atlas-row))
               (.then
                (fn [_]
                  (render-image-bytes! device tiny-system
                                       [over-budget-op] 1.0)))
               (.then
                (fn [over-budget-bytes]
                  (-> (js/Promise.all
                       #js [(sha256-bytes unavailable-bytes)
                            (sha256-bytes over-budget-bytes)])
                      (.then
                       (fn [hashes]
                         {:placeholder
                          {:unavailable-sha256 (aget hashes 0)
                           :over-budget-sha256 (aget hashes 1)
                           :byte-identical?
                           (= (aget hashes 0) (aget hashes 1))}}))))))))
        (.then
         (fn [receipt]
           (let [tiny-before (gpu-budget/snapshot tiny-tracker)
                 tiny-receipt (renderer/image-ingress-receipt tiny-system)]
             (renderer/destroy-image-system! tiny-system)
             (doseq [buffer [tiny-camera tiny-containers]]
               (gpu-budget/destroy-resource! tiny-tracker buffer
                                             :reason :image-verifier-destroy)
               (.destroy buffer))
             (assoc receipt
                    :tiny-before tiny-before
                    :tiny-after (gpu-budget/snapshot tiny-tracker)
                    :tiny-receipt tiny-receipt))))
        (.then
         (fn [receipt]
           (-> (request-replacement-device!)
               (.then
                (fn [replacement-device]
                  (let [replacement-tracker
                        (gpu-budget/create-tracker
                         nil)
                        replacement-camera
                        (renderer/create-camera-buffer replacement-device
                                                       replacement-tracker)
                        replacement-containers
                        (renderer/create-containers-buffer replacement-device
                                                           replacement-tracker)
                        replacement-system
                        (renderer/init-image-system
                         replacement-device "rgba8unorm-srgb"
                         replacement-camera replacement-containers
                         :tracker replacement-tracker
                         :scene-color (scene-color/scene-color true))]
                    (-> (renderer/rebuild-image-resources! candidate-system
                                                          replacement-system)
                        (.then
                         (fn [rebuild]
                           (let [replacement-receipt (:receipt rebuild)
                                 replacement-rows (:rows replacement-receipt)
                                 history-pass?
                                 (every?
                                  (fn [digest]
                                    (let [statuses
                                          (mapv :status
                                                (:history
                                                 (get replacement-rows digest)))]
                                      (and (some #{:device-lost} statuses)
                                           (= :ok (last statuses)))))
                                  registered-digests)
                                 lost-receipt
                                 (renderer/image-ingress-receipt
                                  candidate-system)
                                 rebuild-receipt
                                 {:replacement-receipt replacement-receipt
                                  :lost-receipt lost-receipt
                                  :history-pass? history-pass?}]
                             (renderer/destroy-image-system!
                              replacement-system)
                             (doseq [buffer [replacement-camera
                                             replacement-containers]]
                               (gpu-budget/destroy-resource!
                                replacement-tracker buffer
                                :reason :image-verifier-destroy)
                               (.destroy buffer))
                             (.destroy replacement-device)
                             (assoc receipt :rebuild rebuild-receipt)))))))))))
        (.then
         (fn [receipt]
           (gpu-budget/destroy-resource! tracker second-texture
                                         :reason :image-budget-probe)
           (.destroy first-texture)
           (.destroy second-texture)
           (let [unavailable (get-in
                              (renderer/image-ingress-receipt candidate-system)
                              [:rows unknown-digest])
                 over-budget (get-in receipt [:tiny-receipt :rows atlas-digest])
                 replacement-receipt
                 (get-in receipt [:rebuild :replacement-receipt])
                 lost-receipt (get-in receipt [:rebuild :lost-receipt])
                 device-loss (:device-loss replacement-receipt)
                 destroyed? (zero? (:total-reserved-bytes
                                     (:tiny-after receipt)))
                 budget-pass? (and (= expected-bytes
                                      (:reserved-bytes registered))
                                   (= expected-bytes
                                      (:reserved-bytes replaced)))
                 placeholder-sha256
                 "aab20b3aa071f60cd29cbcbc44271364792aa800eb6186042eb0d0ab7e4915e8"
                 placeholder-pass?
                 (and (get-in receipt [:placeholder :byte-identical?])
                      (= placeholder-sha256
                         (get-in receipt
                                 [:placeholder :unavailable-sha256]))
                      (= placeholder-sha256
                         (get-in receipt
                                 [:placeholder :over-budget-sha256])))
                 device-loss-pass?
                 (and (= (count registered-digests)
                         (:rebuilt-count device-loss))
                      (:replacement-device? device-loss)
                      (:resource-identities-fresh? device-loss)
                      (= (count registered-digests)
                         (get-in lost-receipt [:counts :device-lost]))
                      (= (count registered-digests)
                         (get-in replacement-receipt [:counts :ok]))
                      (get-in receipt [:rebuild :history-pass?]))]
             {:budget {:expected-bytes expected-bytes
                       :registered-bytes (:reserved-bytes registered)
                       :replacement-bytes (:reserved-bytes replaced)
                       :pass? budget-pass?}
              :placeholder (assoc (:placeholder receipt)
                                  :expected-sha256 placeholder-sha256
                                  :pass? placeholder-pass?)
              :unavailable unavailable
              :over-budget over-budget
              :device-loss device-loss
              :replacement-history-pass?
              (get-in receipt [:rebuild :history-pass?])
              :lost-device-counts (:counts lost-receipt)
              :replacement-counts (:counts replacement-receipt)
              :destroy {:reserved-before
                        (:total-reserved-bytes (:tiny-before receipt))
                        :reserved-after
                        (:total-reserved-bytes (:tiny-after receipt))
                        :pass? destroyed?}
              :pass? (and budget-pass? placeholder-pass?
                          (= :unavailable (:status unavailable))
                          (:placeholder-rendered unavailable)
                          (= :refused (:status over-budget))
                          (= :over-budget (:reason over-budget))
                          (:placeholder-rendered over-budget)
                          device-loss-pass? destroyed?)}))))))

(defn- run-image-atom! [device adapter]
  (let [candidate-tracker
        (gpu-budget/create-tracker (gpu-budget/snapshot-adapter-limits adapter))
        candidate-camera (renderer/create-camera-buffer device candidate-tracker)
        candidate-containers
        (renderer/create-containers-buffer device candidate-tracker)
        candidate-system
        (renderer/init-image-system
         device "rgba8unorm-srgb" candidate-camera candidate-containers
         :tracker candidate-tracker
         :scene-color (scene-color/scene-color true))
        seam-tracker
        (gpu-budget/create-tracker (gpu-budget/snapshot-adapter-limits adapter))
        seam-camera (renderer/create-camera-buffer device seam-tracker)
        seam-containers (renderer/create-containers-buffer device seam-tracker)
        seam-system
        (renderer/init-image-system
         device "rgba8unorm" seam-camera seam-containers
         :tracker seam-tracker
         :scene-color (scene-color/scene-color false))]
    (-> (fetch-image-corpus!)
        (.then
         (fn [corpus]
           (-> (ingress-corpus! candidate-system corpus)
               (.then (fn [_] (ingress-corpus! seam-system corpus)))
               (.then (fn [_] corpus)))))
        (.then
         (fn [corpus]
           (-> (promise-mapv
                (partial run-image-golden-case!
                         device candidate-system corpus)
                zoom-cases)
               (.then (fn [cases] {:corpus corpus :cases cases})))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-image-parity! device seam-system corpus)
               (.then #(assoc state :parity (vec (mapcat identity %)))))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-color-receipts! device candidate-system seam-system corpus)
               (.then #(assoc state :color %)))))
        (.then
         (fn [{:keys [corpus] :as state}]
           (-> (run-lifecycle-receipt! device candidate-system corpus)
               (.then #(assoc state :lifecycle %)))))
        (.then
         (fn [{:keys [corpus cases parity color lifecycle]}]
           (let [fixture-digests
                 (into {} (map (fn [[filename row]]
                                 [filename (:digest row)])) corpus)
                 pass? (and (= 21 (reduce + (map #(count (:images %)) cases)))
                            (= 14 (count parity))
                            (every? :pass? parity)
                            (:pass? color)
                            (:pass? lifecycle))
                 result {:cases cases :parity parity :color color
                         :lifecycle lifecycle
                         :fixture-digests fixture-digests
                         :candidate-ingress
                         (renderer/image-ingress-receipt candidate-system)
                         :seam-off-ingress
                         (renderer/image-ingress-receipt seam-system)
                         :product-loop-claim :parked-verifier-only
                         :product-loop-join :none
                         :image-above-text-kind-layer true
                         :default-dark true :felt-gate :sid-live
                         :pass? pass?}]
             (renderer/destroy-image-system! candidate-system)
             (renderer/destroy-image-system! seam-system)
             result))))))

;; CHROME ATOM ---------------------------------------------------------------

(def ^:private chrome-owner-vi [:chrome-neutral :fixture])

(defn- neutral-quad [anchors offsets-px color]
  {:anchors anchors :offsets-px offsets-px :color color})

(defn- chrome-neutral-ops [zoom]
  (let [point [(/ 32.0 zoom) (/ 36.0 zoom)]
        start [(/ 48.0 zoom) (/ 76.0 zoom)]
        end [(/ 92.0 zoom) (/ 76.0 zoom)]]
    [{:id :chrome/neutral :address :chrome/neutral
      :container 0 :container-idx 0 :owner-vi chrome-owner-vi
      :chrome/material
      [(neutral-quad (vec (repeat 4 point))
                     [[-5.0 -5.0] [5.0 -5.0] [5.0 5.0] [-5.0 5.0]]
                     [1.0 1.0 1.0 1.0])
       (neutral-quad [start end end start]
                     [[0.0 -1.5] [0.0 -1.5] [0.0 1.5] [0.0 1.5]]
                     [0.2 0.7 1.0 0.8])]}]))

(defn- render-chrome-bytes!
  [^js device chrome-system ops zoom]
  (let [row-bytes (* canvas-size 4)
        target (.createTexture
                device
                (clj->js {:size {:width canvas-size :height canvas-size
                                 :depthOrArrayLayers 1}
                          :format "rgba8unorm" :viewFormats ["rgba8unorm-srgb"]
                          :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                         js/GPUTextureUsage.COPY_SRC)}))
        read-buffer (.createBuffer
                     device
                     (clj->js {:size (* row-bytes canvas-size)
                               :usage (bit-or js/GPUBufferUsage.COPY_DST
                                              js/GPUBufferUsage.MAP_READ)}))
        camera (js/Float32Array. 6)
        _ (renderer/update-camera device (:camera-buffer chrome-system)
                                  camera 0.0 0.0 zoom
                                  canvas-size canvas-size)
        {:keys [vertices]} (chrome-gpu/prepare-chrome-frame! chrome-system ops)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format "rgba8unorm-srgb"}))
                          :clearValue {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
                          :loadOp "clear" :storeOp "store"}]}))]
    (chrome-gpu/draw-chrome-range! pass chrome-system 0 vertices)
    (.end pass)
    (.copyTextureToBuffer
     encoder (clj->js {:texture target})
     (clj->js {:buffer read-buffer :bytesPerRow row-bytes
               :rowsPerImage canvas-size})
     (clj->js {:width canvas-size :height canvas-size
               :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then
         (fn [_]
           (let [copy (js/Uint8Array.
                       (js/Uint8Array. (.getMappedRange read-buffer)))]
             (.unmap read-buffer)
             (.destroy read-buffer)
             (.destroy target)
             copy))))))

(defn- render-chrome-pair! [device chrome-system ops zoom]
  (-> (render-chrome-bytes! device chrome-system ops zoom)
      (.then
       (fn [first-bytes]
         (-> (render-chrome-bytes! device chrome-system ops zoom)
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
                        :byte-identical? (= (aget hashes 0)
                                             (aget hashes 1))}))))))))))

(defn- run-chrome-golden! [device chrome-system zoom]
  (let [ops (chrome-neutral-ops zoom)
        case-id (str "neutral-point-line-z" (int zoom))]
    (-> (render-chrome-pair! device chrome-system ops zoom)
        (.then
         (fn [pair]
           {:case-id case-id
            :zoom zoom
            :regime "neutral-legal-zoom"
            :normalization "opaque-anchors+screen-px-offsets"
            :shape-extent-world (/ 60.0 zoom)
            :quad-count 2
            :images [{:mode "neutral-point-line"
                      :file (str "gpu-chrome-" case-id ".png")
                      :raw-sha256 (:first-sha256 pair)
                      :png-data-url (opaque-png-data-url (:bytes pair))
                      :determinism
                      {:first-raw-sha256 (:first-sha256 pair)
                       :second-raw-sha256 (:second-sha256 pair)
                       :byte-identical? (:byte-identical? pair)}}]})))))

(defn- run-chrome-upload-gate! [chrome-system]
  (let [ops (chrome-neutral-ops 1.0)
        first-write (chrome-gpu/prepare-chrome-frame! chrome-system ops)
        equal-vector (chrome-gpu/prepare-chrome-frame! chrome-system
                                                       (mapv identity ops))]
    {:first-write first-write :equal-new-vector equal-vector
     :pass? (and (:mesh-set-changed? first-write)
                 (= 1 (:writes first-write))
                 (not (:mesh-set-changed? equal-vector))
                 (zero? (:writes equal-vector)))}))

(defn- run-chrome-atom! [device adapter]
  (let [tracker (gpu-budget/create-tracker
                 (gpu-budget/snapshot-adapter-limits adapter))
        camera (renderer/create-camera-buffer device tracker)
        containers-buffer (renderer/create-containers-buffer device tracker)
        system (chrome-gpu/init-chrome-system
                device "rgba8unorm-srgb" camera containers-buffer
                :tracker tracker :scene-color (scene-color/scene-color true))]
    (-> (promise-mapv (partial run-chrome-golden! device system) [1.0 4.0])
        (.then
         (fn [cases]
           (let [determinism (mapcat #(map :determinism (:images %)) cases)
                 upload-gate (run-chrome-upload-gate! system)
                 before (gpu-budget/snapshot tracker)
                 registered
                 (first (filter #(= "chrome/vertices" (:label %))
                                (:by-label before)))
                 system-receipt (chrome-gpu/chrome-receipt system)
                 _ (chrome-gpu/destroy-chrome-system! system)
                 after (gpu-budget/snapshot tracker)
                 released?
                 (not-any? #(= "chrome/vertices" (:label %))
                           (:by-label after))
                 resources
                 {:registered registered
                  :reserved-before (:reserved-bytes registered)
                  :active-before (:active-bytes registered)
                  :released-on-destroy? released?
                  :pass? (and (some? registered) released?)}
                 pass? (and (= 2 (count cases))
                            (every? :byte-identical? determinism)
                            (:pass? upload-gate)
                            (:pass? resources))]
             {:cases cases
              :upload-gate upload-gate
              :resources resources
              :system system-receipt
              :pass? pass?}))))))

;; --- PATH ATOM --------------------------------------------------------------

(defn- path-paint [color opacity]
  {:color color :opacity opacity
   :color-space :srgb :alpha-association :straight})

(defn- screen-point [zoom [x y]] [(/ x zoom) (/ y zoom)])

(defn- path-ink-material [id zoom samples color opacity]
  {:path/material-id id
   :path/revision 1
   :path/kind :ink
   :path/geometry
   {:knots (mapv (fn [index [x y pressure]]
                   {:knot/id [id index]
                    :position (screen-point zoom [x y])
                    :pressure pressure})
                 (range) samples)
    :base-width (/ 16.0 zoom)
    :cap :round :join :round}
   :path/paint (path-paint color opacity)
   :path/provenance {:actor :render-verifier :act :fixture}})

(defn- path-shape-material [id zoom color opacity]
  {:path/material-id id
   :path/revision 1
   :path/kind :shape
   :path/geometry
   {:open-width (/ 5.0 zoom)
    :contours
    [{:contour/id [id :outer] :role :outer
      :points (mapv (partial screen-point zoom)
                    [[24.0 24.0] [104.0 24.0] [104.0 104.0]
                     [72.0 104.0] [72.0 64.0] [56.0 64.0]
                     [56.0 104.0] [24.0 104.0]])}
     {:contour/id [id :hole] :role :hole
      :points (mapv (partial screen-point zoom)
                    [[34.0 34.0] [50.0 34.0]
                     [50.0 50.0] [34.0 50.0]])}]}
   :path/paint (path-paint color opacity)
   :path/provenance {:actor :render-verifier :act :fixture}})

(defn- path-polygon-material [id points color opacity]
  {:path/material-id id
   :path/revision 1
   :path/kind :shape
   :path/geometry
   {:open-width 2.0
    :contours [{:contour/id [id :outer] :role :outer
                :points points}]}
   :path/paint (path-paint color opacity)
   :path/provenance {:actor :render-verifier :act :fixture}})

(defn- path-quad-material [id zoom color opacity]
  (path-polygon-material
   id
   (mapv (partial screen-point zoom)
         [[24.0 24.0] [104.0 24.0] [104.0 104.0] [24.0 104.0]])
   color opacity))

(defn- path-op [id material]
  {:id id :x 0.0 :y 0.0
   :path/material material :path/clip nil :container-idx 0})

(defn- render-path-bytes!
  [^js device path-system ops zoom
   & {:keys [clear-value]
      :or {clear-value {:r 0.0 :g 0.0 :b 0.0 :a 0.0}}}]
  (let [row-bytes (* canvas-size 4)
        candidate? (get-in path-system [:scene-color :enabled?])
        view-format (if candidate? "rgba8unorm-srgb" "rgba8unorm")
        target (.createTexture
                device
                (clj->js {:size {:width canvas-size :height canvas-size
                                 :depthOrArrayLayers 1}
                          :format "rgba8unorm"
                          :viewFormats ["rgba8unorm-srgb"]
                          :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                         js/GPUTextureUsage.COPY_SRC)}))
        read-buffer (.createBuffer
                     device
                     (clj->js {:size (* row-bytes canvas-size)
                               :usage (bit-or js/GPUBufferUsage.COPY_DST
                                              js/GPUBufferUsage.MAP_READ)}))
        camera (js/Float32Array. 6)
        _ (renderer/update-camera device (:camera-buffer path-system)
                                  camera 0.0 0.0 zoom
                                  canvas-size canvas-size)
        {:keys [vertices]} (path-gpu/prepare-path-frame! path-system ops zoom)
        encoder (.createCommandEncoder device)
        pass (.beginRenderPass
              encoder
              (clj->js {:colorAttachments
                        [{:view (.createView target
                                            (clj->js {:format view-format}))
                          :clearValue clear-value
                          :loadOp "clear" :storeOp "store"}]}))]
    (path-gpu/draw-path-range! pass path-system 0 vertices)
    (.end pass)
    (.copyTextureToBuffer
     encoder
     (clj->js {:texture target})
     (clj->js {:buffer read-buffer :bytesPerRow row-bytes
               :rowsPerImage canvas-size})
     (clj->js {:width canvas-size :height canvas-size
               :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then
         (fn [_]
           (let [copy (js/Uint8Array.
                       (js/Uint8Array. (.getMappedRange read-buffer)))]
             (.unmap read-buffer)
             (.destroy read-buffer)
             (.destroy target)
             copy))))))

(defn- render-path-pair! [device path-system ops zoom clear-value]
  (-> (render-path-bytes! device path-system ops zoom
                          :clear-value clear-value)
      (.then
       (fn [first-bytes]
         (-> (render-path-bytes! device path-system ops zoom
                                 :clear-value clear-value)
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
                        :byte-identical? (= (aget hashes 0)
                                             (aget hashes 1))}))))))))))

(defn- path-image-record [mode case-id pair]
  {:mode mode
   :file (str "gpu-path-" mode "-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
   :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- path-golden-spec [mode]
  (case mode
    :pressure-ink
    {:case-id "pressure-ink-default-min-z0p1"
     :zoom 0.1 :mode "pressure-ink"
     :material (path-ink-material
                :path-golden/pressure 0.1
                [[26.0 72.0 0.2] [48.0 36.0 0.45]
                 [78.0 84.0 0.72] [102.0 42.0 1.0]]
                [0.16 0.68 0.96 0.94] 1.0)}

    :holed-concave
    {:case-id "holed-concave-default-unit-z1"
     :zoom 1.0 :mode "holed-concave"
     :material (path-shape-material :path-golden/shape 1.0
                                    [0.94 0.32 0.18 0.96] 1.0)}

    :translucent-self-crossing
    {:case-id "translucent-self-crossing-legal-z10"
     :zoom 10.0 :mode "translucent-self-crossing"
     :material (path-ink-material
                :path-golden/self-cross 10.0
                [[26.0 28.0 0.65] [102.0 100.0 0.9]
                 [28.0 100.0 1.0] [102.0 28.0 0.7]]
                [0.84 0.36 0.94 0.62] 1.0)}))

(defn- run-path-golden! [device path-system mode]
  (let [{:keys [case-id zoom material] :as spec} (path-golden-spec mode)
        clear {:r 0.025 :g 0.06 :b 0.11 :a 1.0}
        op (path-op [:golden mode] material)]
    (-> (render-path-pair! device path-system [op] zoom clear)
        (.then
         (fn [pair]
           (let [mesh (path-tessellation/tessellate material zoom)]
             {:case-id case-id
              :zoom zoom
              :regime (name (:regime/id (path-material/zoom-regime zoom)))
              :normalization "screen-constant-shape-local"
              :shape-extent-world (/ 80.0 zoom)
              :mesh {:triangles (:triangle-count mesh)
                     :coverage (:coverage mesh)
                     :quantization
                     (path-tessellation/quantization-receipt mesh zoom)}
              :images [(path-image-record (:mode spec) case-id pair)]}))))))

(defn- path-parity-row! [device path-system {:keys [case-id zoom regime]}]
  (let [material (path-shape-material [:path-parity case-id] zoom
                                      [1.0 1.0 1.0 1.0] 1.0)
        mesh (path-tessellation/tessellate material zoom)
        op (path-op [:parity case-id] material)]
    (-> (render-path-bytes! device path-system [op] zoom)
        (.then
         (fn [bytes]
           (let [rows
                 (for [y (range canvas-size)
                       x (range canvas-size)
                       :let [point [(/ (+ x 0.5) zoom)
                                    (/ (+ y 0.5) zoom)]
                             distance-px (* zoom
                                            (path-material/boundary-distance
                                             material point))]
                       :when (> distance-px 1.25)]
                   (let [cpu (path-material/classify material point zoom)
                         alpha (nth (pixel-rgba bytes x y) 3)
                         gpu (cond (> alpha 128) :inside
                                   (< alpha 128) :outside
                                   :else :half)
                         decisive? (and (not= :boundary cpu)
                                        (not= :half gpu))]
                     {:pixel [x y] :cpu cpu :gpu gpu
                      :alpha alpha :decisive? decisive?
                      :match? (when decisive? (= cpu gpu))}))
                 decisive (filter :decisive? rows)
                 mismatches (filter #(false? (:match? %)) decisive)
                 boundary-count
                 (count (for [y (range canvas-size)
                              x (range canvas-size)
                              :let [point [(/ (+ x 0.5) zoom)
                                           (/ (+ y 0.5) zoom)]]
                              :when (<= (* zoom
                                           (path-material/boundary-distance
                                            material point))
                                        1.25)]
                          [x y]))]
             {:case-id case-id :zoom zoom :regime regime
              :quantization
              (path-tessellation/quantization-receipt mesh zoom)
              :boundary-band-screen-px 1.25
              :boundary-pixel-count boundary-count
              :decisive-count (count decisive)
              :inside-count (count (filter #(= :inside (:cpu %)) decisive))
              :outside-count (count (filter #(= :outside (:cpu %)) decisive))
              :mismatch-count (count mismatches)
              :mismatch-sample (vec (take 12 mismatches))
              :pass? (and (pos? boundary-count)
                          (pos? (count decisive))
                          (some #(= :inside (:cpu %)) decisive)
                          (some #(= :outside (:cpu %)) decisive)
                          (empty? mismatches))}))))))

(defn- run-path-color! [device path-system]
  (let [clear {:r 0.04 :g 0.18 :b 0.35 :a 1.0}
        color [(/ 200.0 255.0) (/ 80.0 255.0) (/ 40.0 255.0) 0.5]
        material (path-quad-material :path-color/source-over 1.0 color 1.0)]
    (-> (render-path-bytes! device path-system
                            [(path-op :path-color material)] 1.0
                            :clear-value clear)
        (.then
         (fn [bytes]
           (let [expected (mapv
                           (fn [source background]
                             (linear->srgb-byte
                              (+ (* (srgb->linear source) 0.5)
                                 (* background 0.5))))
                           [200 80 40] [0.04 0.18 0.35])
                 actual (subvec (vec (pixel-rgba bytes 64 64)) 0 3)
                 delta (apply max (map #(js/Math.abs (- %1 %2))
                                       expected actual))]
             {:expected expected :actual actual
              :max-byte-delta delta
              :non-black-background true
              :pass? (<= delta 3)}))))))

(defn- run-path-upload-gate! [path-system]
  (let [material (path-quad-material :path-upload-gate 1.0
                                     [0.3 0.7 0.4 1.0] 1.0)
        first-ops [(path-op :path-upload-gate material)]
        first-write (path-gpu/prepare-path-frame! path-system first-ops 1.0)
        equal-new-vector (mapv identity first-ops)
        same-mesh-set (path-gpu/prepare-path-frame!
                       path-system equal-new-vector 1.0)]
    {:first-write first-write
     :equal-new-vector same-mesh-set
     :pass? (and (:mesh-set-changed? first-write)
                 (= 1 (:writes first-write))
                 (not (:mesh-set-changed? same-mesh-set))
                 (zero? (:writes same-mesh-set)))}))

(defn- run-path-atom! [device adapter]
  (let [tracker (gpu-budget/create-tracker
                 (gpu-budget/snapshot-adapter-limits adapter))
        camera (renderer/create-camera-buffer device tracker)
        containers-buffer (renderer/create-containers-buffer device tracker)
        system (path-gpu/init-path-system
                device "rgba8unorm-srgb" camera containers-buffer
                :tracker tracker :scene-color (scene-color/scene-color true))]
    (-> (promise-mapv (partial run-path-golden! device system)
                      [:pressure-ink :holed-concave
                       :translucent-self-crossing])
        (.then (fn [cases] {:cases cases}))
        (.then
         (fn [state]
           (-> (promise-mapv (partial path-parity-row! device system)
                             zoom-cases)
               (.then #(assoc state :parity %)))))
        (.then
         (fn [state]
           (-> (run-path-color! device system)
               (.then #(assoc state :color %)))))
        (.then
         (fn [state]
           (assoc state :upload-gate (run-path-upload-gate! system))))
        (.then
         (fn [{:keys [cases parity color upload-gate] :as state}]
           (let [determinism
                 (mapcat (fn [case]
                           (map :determinism (:images case)))
                         cases)
                 pass? (and (= 3 (count cases))
                            (every? :byte-identical? determinism)
                            (= 7 (count parity))
                            (every? :pass? parity)
                            (:pass? color)
                            (:pass? upload-gate))
                 result (assoc state
                               :system (path-gpu/path-receipt system)
                               :coverage :aliased-v1
                               :product-pick :cpu-path-authority
                               :self-overlap-alpha
                               :direct-triangle-double-blend-declared
                               :pass? pass?)]
             (path-gpu/destroy-path-system! system)
             result))))))

(defn- selected-limits [^js limits]
  {:max-buffer-size (.-maxBufferSize limits)
   :max-uniform-buffer-binding-size (.-maxUniformBufferBindingSize limits)
   :max-storage-buffer-binding-size (.-maxStorageBufferBindingSize limits)
   :max-texture-dimension-2d (.-maxTextureDimension2D limits)
   :max-bind-groups (.-maxBindGroups limits)
   :max-vertex-buffers (.-maxVertexBuffers limits)})

(defn- adapter-information [^js adapter]
  (let [info (.-info adapter)
        architecture (some-> info .-architecture)
        description (some-> info .-description)
        native-fallback (some-> info .-isFallbackAdapter)]
    {:vendor (some-> info .-vendor)
     :architecture architecture
     :device (some-> info .-device)
     :description description
     :is-fallback-adapter (if (some? native-fallback)
                            (boolean native-fallback)
                            (= "swiftshader" architecture))
     :fallback-attestation-source (if (some? native-fallback)
                                    "GPUAdapterInfo.isFallbackAdapter"
                                    "GPUAdapterInfo.architecture=swiftshader")
     :renderer (or (not-empty description)
                   (not-empty architecture)
                   (not-empty (some-> info .-vendor)))
     :features (vec (array-seq (js/Array.from (.-features adapter))))
     :limits (selected-limits (.-limits adapter))}))

(defn- shader-digests []
  (let [entries [["slug-vertex" renderer/slug-vertex-shader]
                 ["slug-fragment" renderer/slug-fragment-shader]
                 ["region3d-placed-flat"
                  region3d-placement-gpu/placed-flat-shader]]]
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

;; ---------------------------------------------------------------------------
;; W4 frame runtime — compositor, clip, export, and scheduler machine receipts
;; ---------------------------------------------------------------------------

;; Rect-backed W4 fixture entries were removed with the rect render family.

(defn- w4-read-texture! [^js device ^js texture width height]
  (let [row-bytes (* width 4)
        padded (* 256 (js/Math.ceil (/ row-bytes 256)))
        ^js buffer (.createBuffer device
                              (clj->js {:size (* padded height)
                                        :usage (bit-or js/GPUBufferUsage.COPY_DST
                                                       js/GPUBufferUsage.MAP_READ)}))
        ^js encoder (.createCommandEncoder device)]
    (.copyTextureToBuffer encoder
                          (clj->js {:texture texture})
                          (clj->js {:buffer buffer :bytesPerRow padded
                                    :rowsPerImage height})
                          (clj->js {:width width :height height
                                    :depthOrArrayLayers 1}))
    (.submit (.-queue device) #js [(.finish encoder)])
    (-> (.mapAsync buffer js/GPUMapMode.READ)
        (.then
         (fn []
           (let [mapped (js/Uint8Array. (.getMappedRange buffer))
                 tight (js/Uint8Array. (* row-bytes height))]
             (dotimes [row height]
               (.set tight
                     (.subarray mapped (* row padded)
                                (+ (* row padded) row-bytes))
                     (* row row-bytes)))
             (.unmap buffer)
             (.destroy buffer)
             tight))))))

;; The rect-backed W4 runtime receipt was removed with the rect render family.

;; ---------------------------------------------------------------------------
;; REGION3D FLOOR — held interior/depth/shadow targets inside the 2D tape
;; ---------------------------------------------------------------------------

(def ^:private region3d-owner-vi [:region3d-floor :verifier])
(def ^:private region3d-id :region3d/verifier)

(defn- region3d-tagged
  ([r g b] (region3d-tagged r g b 1.0))
  ([r g b a]
   {:rgba [r g b a] :color-space :srgb :alpha-association :straight}))

(defn- region3d-transform
  ([translation scale]
   (region3d-transform translation scale [0.0 0.0 0.0 1.0]))
  ([translation scale rotation]
   {:translation translation :rotation rotation :scale scale}))

(defn- region3d-mesh
  [id primitive translation scale color metallic roughness]
  {:object/id id :object/kind :mesh :parent nil
   :transform (region3d-transform translation scale)
   :provenance {:asserted-by :sid :act :render-verifier}
   :mesh {:kind primitive
          :params (get region3d-material/primitive-defaults primitive)}
   :material {:base-color color :metallic metallic :roughness roughness
              :emissive (region3d-tagged 0.0 0.0 0.0)}})

(defn- region3d-light [id kind translation color intensity more]
  {:object/id id :object/kind :light :parent nil
   :transform (region3d-transform translation [1.0 1.0 1.0])
   :provenance {:asserted-by :sid :act :render-verifier}
   :light (merge {:kind kind :color color :intensity intensity
                  :cast-shadow false}
                 more)})

(defn- region3d-fixture-region
  ([] (region3d-fixture-region :transparent))
  ([background-kind]
   (region3d-material/validate-region!
    {:region3d/version 1
     :extent {:width 640.0 :height 360.0 :depth 100.0}
     :background {:kind background-kind
                  :color (region3d-tagged 0.055 0.07 0.10
                                         (if (= :transparent background-kind)
                                           0.0 1.0))}
     :ambient {:color (region3d-tagged 0.72 0.80 1.0) :intensity 0.11}
     :view-default {:pivot [0.0 0.0 0.0] :distance 8.0
                    :yaw 0.0 :pitch 0.0
                    :lens region3d-material/default-perspective-lens}
     :scene
     {:near (region3d-mesh :near :box [0.0 0.0 0.0] [2.0 2.0 2.0]
                           (region3d-tagged 0.58 0.64 0.73) 0.18 0.42)
      :far (region3d-mesh :far :box [0.35 0.05 -0.75] [2.5 1.35 2.0]
                          (region3d-tagged 0.24 0.48 0.82) 0.42 0.30)
      :sphere (region3d-mesh :sphere :sphere [-2.0 -0.05 0.0]
                             [1.55 1.55 1.55]
                             (region3d-tagged 0.82 0.34 0.16) 0.68 0.23)
      :glass (region3d-mesh :glass :box [2.25 0.65 0.15] [1.35 1.35 1.35]
                            (region3d-tagged 0.14 0.82 0.70 0.5) 0.08 0.24)
      :floor (region3d-mesh :floor :plane [0.0 -1.20 0.0] [8.0 1.0 7.0]
                            (region3d-tagged 0.30 0.33 0.38) 0.0 0.86)
      :sun (region3d-light :sun :directional [4.0 8.0 6.0]
                           (region3d-tagged 1.0 0.93 0.82) 2.4
                           {:cast-shadow true})
      :point (region3d-light :point :point [-3.0 2.5 4.0]
                             (region3d-tagged 0.38 0.58 1.0) 34.0
                             {:range 18.0})
      :spot (region3d-light :spot :spot [3.0 4.0 4.0]
                            (region3d-tagged 1.0 0.34 0.20) 24.0
                            {:range 20.0
                             :cone {:inner-deg 18.0 :outer-deg 32.0}})}})))

(defn- region3d-op [region & {:keys [width height]
                              :or {width 80.0 height 88.0}}]
  {:id :region3d/verifier-node :address :region3d/verifier-address
   :region-id region3d-id :owner-vi region3d-owner-vi
   :container 0 :container-idx 0
   :x 24.0 :y 20.0 :w width :h height
   :region3d/scene region})

(defn- region3d-seam-fixture []
  (let [ink-object
        {:object/id :seam/ink :object/kind :ink :parent nil
         :transform (region3d-transform
                     [-2.7 -0.95 0.4] [0.018 0.018 0.018]
                     [0.0 -0.21644 0.0 0.976296])
         :provenance {:asserted-by :sid :act :render-verifier}
         :ink {:ref {:address :seam/ink-material}}}
        region (-> (region3d-fixture-region :opaque)
                   (assoc :region3d/version 2)
                   (assoc-in [:scene :seam/ink] ink-object)
                   region3d-material/validate-region!)
        ink-material (path-ink-material
                      :seam/ink-material 1.0
                      [[0.0 8.0 0.45] [54.0 2.0 0.9]
                       [108.0 26.0 0.62] [164.0 8.0 1.0]
                       [222.0 34.0 0.55]]
                      [0.16 0.82 1.0 0.92] 1.0)
        ink-placement
        {:object-id :seam/ink :object ink-object :kind :ink
         :address :seam/ink-material :status :resolved
         :content-revision (path-material/material-content-key ink-material)
         :cache-key (path-material/material-cache-key ink-material 1.0)
         :material ink-material
         :owner {:vi :seam/ink-owner :op-id :seam/ink-material}}
        op (assoc (region3d-op region)
                  :region3d/resolved-placements
                  [ink-placement])]
    {:region region :op op
     :placements [ink-placement]}))

(defn- region3d-regions [op]
  {:regions [op]})

(defn- region3d-painters
  "The painters of one floor frame, back to front: the prepared surround
   paths (op 0 below, op 1 above) bracket the region composite."
  [{:keys [region-system surround-path-system]} sides]
  (let [surround (fn [op-index]
                   (let [{:keys [first-vertex vertex-count]}
                         (nth @(:!prepared surround-path-system) op-index)]
                     (fn [pass]
                       (path-gpu/draw-path-range! pass surround-path-system
                                                  first-vertex vertex-count))))
        region (fn [pass]
                 (region3d-gpu/composite-region! pass region-system
                                                 region3d-id))]
    (case sides
      :sandwich [(surround 0) region (surround 1)]
      :region [region]
      :empty [(surround 0)])))

(defn- region3d-direct-frame!
  "Direct driver, no order model: lease every desired region, encode the
   shadow and interior of each one the region painter has prepared, paint the
   painters back to front into one linear scene target, present it, and read
   the pixels back."
  [{:keys [device compositor region-system]} painters]
  (let [^js device device
        ^js texture (.createTexture
                     device
                     (clj->js {:size {:width canvas-size :height canvas-size
                                      :depthOrArrayLayers 1}
                               :format color-format
                               :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                              js/GPUTextureUsage.COPY_SRC)}))
        owner (region3d-gpu/binding-owner region-system)
        {:keys [active stale]}
        (compositor-gpu/active-region-leases! compositor owner)
        encoder (.createCommandEncoder device)
        prepared (:prepared (region3d-gpu/region3d-receipt region-system))
        passes (vec (for [{region-id :region/id shadow? :shadow?}
                          (region-bindings/desired-rows owner)
                          :when (contains? prepared region-id)
                          role (if shadow? [:shadow :interior] [:interior])]
                      (region3d-gpu/encode-region-pass!
                       region-system encoder region-id role
                       (get active region-id))))
        scene (compositor-gpu/acquire-target!
               (:target-pool compositor) "rgba16float"
               canvas-size canvas-size "frame/scene-color")
        pass (compositor-gpu/begin-target-pass! encoder scene "clear")]
    (doseq [paint! painters] (paint! pass))
    (.end pass)
    (compositor-gpu/draw-present! compositor encoder scene
                                  (.createView texture) color-format)
    (.submit (.-queue device) #js [(.finish encoder)])
    (compositor-gpu/release-after-submit! compositor [scene] [] stale)
    (-> (w4-read-texture! device texture canvas-size canvas-size)
        (.then (fn [bytes]
                 (.destroy texture)
                 {:bytes bytes :passes passes})))))

(defn- region3d-prepare-options [harness]
  {:zoom 1.0 :dpr 1.0
   :font-assets (:font-assets harness)
   :path-system (:path-system harness)})

(defn- region3d-capture!
  ([harness op session sides]
   (region3d-capture! harness op session sides {}))
  ([{:keys [compositor region-system] :as harness}
    op session sides prepare-overrides]
   (region3d-gpu/attach-compositor! region-system compositor)
   (region3d-gpu/prepare-region3d-frame!
    region-system (region3d-regions op) session
    (merge (region3d-prepare-options harness) prepare-overrides))
   (region3d-direct-frame! harness (region3d-painters harness sides))))

(defn- region3d-capture-pair!
  ([harness op session sides]
   (region3d-capture-pair! harness op session sides {}))
  ([{:keys [compositor region-system] :as harness}
    op session sides prepare-overrides]
   (region3d-gpu/attach-compositor! region-system compositor)
   (region3d-gpu/prepare-region3d-frame!
    region-system (region3d-regions op) session
    (merge (region3d-prepare-options harness) prepare-overrides))
   (let [painters (region3d-painters harness sides)]
     (-> (region3d-direct-frame! harness painters)
         (.then
          (fn [{first-bytes :bytes}]
            (-> (region3d-direct-frame! harness painters)
                (.then
                 (fn [{second-bytes :bytes}]
                   (-> (js/Promise.all
                        #js [(sha256-bytes first-bytes)
                             (sha256-bytes second-bytes)])
                       (.then
                        (fn [hashes]
                          {:bytes first-bytes
                           :first-sha256 (aget hashes 0)
                           :second-sha256 (aget hashes 1)
                           :byte-identical? (= (aget hashes 0)
                                               (aget hashes 1))}))))))))))))

(defn- region3d-image-record [case-id pair]
  {:mode case-id :file (str "gpu-region3d-floor-" case-id ".png")
   :raw-sha256 (:first-sha256 pair)
   :png-data-url (opaque-png-data-url (:bytes pair))
                 :determinism {:first-raw-sha256 (:first-sha256 pair)
                 :second-raw-sha256 (:second-sha256 pair)
                 :byte-identical? (:byte-identical? pair)}})

(defn- region3d-lights [maintained]
  (->> (get-in maintained [:region :scene])
       (keep (fn [[id object]]
               (when (= :light (:object/kind object))
                 (let [matrix (get-in maintained [:effective-transforms id])]
                   {:light (:light object)
                    :position (region3d-scene/transform-point matrix
                                                              [0.0 0.0 0.0])
                    :direction (region3d-scene/transform-direction
                                matrix [0.0 0.0 -1.0])}))))
       vec))

(defn- region3d-lit-oracle [region bytes]
  (let [maintained (assoc (region3d-scene/derive-scene region)
                          :region-id region3d-id)
        camera (region3d-scene/camera-matrices (:view-default region)
                                                [80.0 88.0])
        hit (region3d-scene/pick-region
             {:maintained maintained :camera camera :region-point [40.0 44.0]})
        object (get-in region [:scene (:object-id hit)])
        linear (region3d-oracle/shade-reference
                {:material (:material object) :normal (:normal hit)
                 :point (:point3 hit) :eye (:eye camera)
                 :lights (region3d-lights maintained)
                 :ambient (:ambient region) :bvh (:bvh maintained)
                 :object-id (:object-id hit)})
        expected (conj (mapv #(linear->srgb-byte %) (take 3 linear))
                       (js/Math.round (* 255.0 (last linear))))
        actual (pixel-rgba bytes 64 64)
        delta (apply max (map #(js/Math.abs (- %1 %2)) expected actual))
        glass-screen (:screen (region3d-scene/project-point camera
                                                             [2.25 0.65 0.825]))
        [glass-x glass-y] (mapv js/Math.floor glass-screen)
        glass-sample (pixel-rgba bytes (+ 24 glass-x) (+ 20 glass-y))
        outside-point [1.0 1.0]
        outside-pick (region3d-scene/pick-region
                      {:maintained maintained :camera camera
                       :region-point outside-point})
        outside-sample (pixel-rgba bytes 25 21)
        boundary-screen (:screen (region3d-scene/project-point
                                  camera [1.0 1.0 1.0]))
        boundary-pick (region3d-scene/pick-region
                       {:maintained maintained :camera camera
                        :region-point boundary-screen})
        [boundary-x boundary-y] (mapv js/Math.floor boundary-screen)
        boundary-sample (pixel-rgba bytes (+ 24 boundary-x)
                                    (+ 20 boundary-y))
        depth-classes
        {:interior {:cpu-route (:route hit) :cpu-object (:object-id hit)
                    :gpu-rgba actual}
         :outside {:cpu-route (:route outside-pick) :gpu-rgba outside-sample}
         :boundary {:cpu-route (:route boundary-pick)
                    :cpu-object (:object-id boundary-pick)
                    :cpu-boundary? (:boundary? boundary-pick)
                    :gpu-rgba boundary-sample
                    :screen boundary-screen}}
        depth-classes-pass?
        (and (= :object (get-in depth-classes [:interior :cpu-route]))
             (= :near (get-in depth-classes [:interior :cpu-object]))
             (pos? (last (get-in depth-classes [:interior :gpu-rgba])))
             (= :region-background
                (get-in depth-classes [:outside :cpu-route]))
             (zero? (last (get-in depth-classes [:outside :gpu-rgba])))
             (= :object (get-in depth-classes [:boundary :cpu-route]))
             (= :near (get-in depth-classes [:boundary :cpu-object]))
             (true? (get-in depth-classes [:boundary :cpu-boundary?]))
             (pos? (last (get-in depth-classes [:boundary :gpu-rgba]))))]
    {:sample [64 64] :expected-object :near
     :actual-object (:object-id hit) :expected-t 6.9 :actual-t (:t hit)
     :expected-rgba expected :actual-rgba actual :max-byte-delta delta
     :epsilon-bytes 2
     :transparency-sample [(+ 24 glass-x) (+ 20 glass-y)]
     :transparency-rgba glass-sample
     :depth-classes depth-classes
     :depth-classes-pass? depth-classes-pass?
     :pass? (and (= :near (:object-id hit))
                 (<= (js/Math.abs (- 6.9 (:t hit))) 1.0e-6)
                 (<= delta 2)
                 (< 0 (last glass-sample) 255)
                 depth-classes-pass?)}))

(defn- region3d-s5-lifecycle!
  [{:keys [device compositor region-system tracker] :as harness} region op]
  (let [base-view (:view-default region)
        changed-view (assoc base-view :yaw 0.045 :pitch -0.02)
        before-view (region3d-gpu/region3d-receipt region-system)
        wait-for-queue
        (fn [value-fn]
          (.then (.onSubmittedWorkDone (.-queue ^js device))
                 value-fn))
        steps
        [(fn [_]
           (.then
            (region3d-capture!
             harness op {:regions {region3d-id {:view changed-view}}} :region)
            (fn [{view-passes :passes}]
              (let [after-view (region3d-gpu/region3d-receipt region-system)]
                (.then
                 (region3d-capture!
                  harness op {:regions {region3d-id {:view changed-view}}}
                  :region)
                 (fn [{clean-passes :passes}]
                   {:before-view before-view :after-view after-view
                    :view-passes view-passes
                    :clean-passes clean-passes}))))))
         (fn [state]
           (let [resized (assoc op :w 300.0)]
             (.then
              (region3d-capture!
               harness resized {:regions {region3d-id {:view changed-view}}}
               :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :resize
                          (compositor-gpu/region-leases-receipt compositor)
                          :resized-op resized)))))))
         (fn [{:keys [resized-op] :as state}]
           (let [shadow-off (assoc-in region
                                      [:scene :sun :light :cast-shadow] false)
                 shadow-off-op (assoc resized-op :region3d/scene shadow-off)]
             (.then
              (region3d-capture!
               harness shadow-off-op
               {:regions {region3d-id {:view changed-view}}} :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :shadow-off
                          (compositor-gpu/region-leases-receipt compositor))))))))
         (fn [state]
           (region3d-gpu/prepare-region3d-frame!
            region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
           (let [frame (region3d-direct-frame!
                        harness (region3d-painters harness :empty))]
             (.then
              frame
              (fn [_]
                (wait-for-queue
                 (fn []
                   (assoc state :after-close
                          (compositor-gpu/region-leases-receipt compositor))))))))
         (fn [state]
           (let [refusal-compositor
                 (compositor-gpu/create-compositor!
                  device color-format tracker
                  :budget-cap-bytes (* 5 1024 1024))
                 refusal-harness (assoc harness :compositor refusal-compositor)]
             (.then
              (region3d-capture! refusal-harness op {} :region)
              (fn [{:keys [bytes]}]
                (wait-for-queue
                 (fn []
                   (let [receipt
                         (compositor-gpu/compositor-receipt refusal-compositor)
                         sample (pixel-rgba bytes 64 64)
                         next-state
                         (assoc state :refusal
                                {:receipt receipt :sample sample
                                 :pass? (and (some? (:last-region-refusal receipt))
                                             (pos? (apply max sample)))})]
                     (compositor-gpu/destroy-compositor! refusal-compositor)
                     (region3d-gpu/attach-compositor! region-system compositor)
                     (region3d-gpu/prepare-region3d-frame!
                      region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
                     next-state)))))))
         (fn [state]
           (let [first-compositor
                 (compositor-gpu/create-compositor!
                  device color-format tracker)
                 first-harness (assoc harness :compositor first-compositor)]
             (.then
              (region3d-capture! first-harness op {} :region)
              (fn [_]
                (wait-for-queue
                 (fn []
                   (let [before-destroy
                         (compositor-gpu/region-leases-receipt
                          first-compositor)]
                     (compositor-gpu/destroy-compositor! first-compositor)
                     (let [after-destroy
                           (compositor-gpu/region-leases-receipt
                            first-compositor)
                           recreated
                           (compositor-gpu/create-compositor!
                            device color-format tracker)
                           recreated-harness (assoc harness
                                                      :compositor recreated)]
                       (.then
                        (region3d-capture! recreated-harness op {} :region)
                        (fn [{:keys [passes]}]
                          (wait-for-queue
                           (fn []
                             (let [after-recreate
                                   (compositor-gpu/region-leases-receipt
                                    recreated)
                                   receipt
                                   {:before-destroy before-destroy
                                    :after-destroy after-destroy
                                    :after-recreate after-recreate
                                    :passes passes
                                    :pass?
                                    (and (pos? (:bytes before-destroy))
                                         (zero? (:bytes after-destroy))
                                         (pos? (:bytes after-recreate))
                                         (every? :encoded? passes))}]
                               (compositor-gpu/destroy-compositor! recreated)
                               (region3d-gpu/attach-compositor!
                                region-system compositor)
                               (region3d-gpu/prepare-region3d-frame!
                                region-system {:regions []} {}
                                {:zoom 1.0 :dpr 1.0})
                               (assoc state :destroy-recreate receipt))))))))))))))
         (fn [{:keys [before-view after-view view-passes clean-passes
                      resize shadow-off after-close refusal destroy-recreate]
               :as receipt}]
           (let [object-upload-delta
                 (- (:object-instance-uploads after-view)
                    (:object-instance-uploads before-view))
                 mesh-upload-delta
                 (- (:mesh-vertex-uploads after-view)
                    (:mesh-vertex-uploads before-view))
                 uniform-upload-delta
                 (- (:uniform-uploads after-view)
                    (:uniform-uploads before-view))
                 view-by-role (into {} (map (juxt :role identity)) view-passes)
                 clean? (every? #(and (:held? %) (not (:encoded? %)))
                                clean-passes)
                 resize-leases (vals (:leases resize))
                 shadow-off-leases (vals (:leases shadow-off))
                 pass? (and (zero? object-upload-delta)
                            (zero? mesh-upload-delta)
                            (= 1 uniform-upload-delta)
                            (get-in view-by-role [:interior :encoded?])
                            (get-in view-by-role [:shadow :held?])
                            clean?
                            (= 1 (count resize-leases))
                            (= [512 256] (:size (first resize-leases)))
                            (= 1 (count shadow-off-leases))
                            (< (:bytes shadow-off) (:bytes resize))
                            (zero? (:bytes after-close))
                            (:pass? refusal)
                            (:pass? destroy-recreate))]
             (-> receipt
                 (dissoc :resized-op)
                 (assoc :camera-wake
                        {:object-instance-upload-delta object-upload-delta
                         :mesh-vertex-upload-delta mesh-upload-delta
                         :uniform-upload-delta uniform-upload-delta
                         :passes view-passes}
                        :clean-held? clean?
                        :pass? pass?))))]]
    (reduce (fn [promise step] (.then promise step))
            (js/Promise.resolve nil)
            steps)))

(def ^:private lower-resolution-pressure-id :region3d/lower-resolution-pressure)

(defn- pool-holds-free-target?
  [pool target]
  (let [target-id (:target/id target)]
    (boolean
     (some #(= target-id (:target/id %))
           (mapcat val (:free @(:!state pool)))))))

(defn- region3d-lower-step!
  [harness painters]
  (-> (region3d-direct-frame! harness painters)
      (.then (fn [{:keys [bytes passes]}]
               {:bytes bytes :passes passes
                :receipt (compositor-gpu/compositor-receipt
                          (:compositor harness))}))))

(defn- region3d-refusal-leg!
  [{:keys [device tracker region-system] :as harness}
   region op budget-cap-bytes]
  (let [compositor (compositor-gpu/create-compositor!
                    device color-format tracker
                    :budget-cap-bytes budget-cap-bytes)
        refusal-harness (assoc harness :compositor compositor)]
    (-> (region3d-capture! refusal-harness op {} :region {:zoom 8.0})
        (.then
         (fn [{:keys [bytes]}]
           (let [receipt (compositor-gpu/compositor-receipt compositor)
                 result {:bytes bytes
                         :receipt receipt
                         :sample (pixel-rgba bytes 64 64)
                         :pass? (and (some? (:last-region-refusal receipt))
                                     (pos? (apply max (pixel-rgba bytes 64 64))))}]
             (compositor-gpu/destroy-compositor! compositor)
             (region3d-gpu/prepare-region3d-frame!
              region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
             result))))))

(defn- region3d-lower-resolution!
  [{:keys [device tracker region-system compositor] :as harness} region op]
  (let [lower-compositor
        (compositor-gpu/create-compositor!
         device color-format tracker :budget-cap-bytes (* 64 1024 1024))
        lower-harness (assoc harness :compositor lower-compositor)
        pool (:target-pool lower-compositor)
        reserve-target
        (compositor-gpu/acquire-target!
         pool "rgba16float" 512 256 "frame/group-output/lower-resolution"
         :usage js/GPUTextureUsage.RENDER_ATTACHMENT)
        _ (compositor-gpu/release-target! pool reserve-target)
        pressure-lease
        (compositor-gpu/acquire-region-lease!
         lower-compositor lower-resolution-pressure-id 768 512 false)
        pressure-row {:region/id lower-resolution-pressure-id
                      :lease-size [768 512] :shadow? false
                      :background nil :encode-rung 1
                      :composite {:x 0.0 :y 0.0 :w 1.0 :h 1.0
                                  :container-idx 0}}
        prepare-frame
        (fn [current-op zoom]
          (region3d-gpu/attach-compositor! region-system lower-compositor)
          (region3d-gpu/prepare-region3d-frame!
           region-system (region3d-regions current-op) {}
           (merge (region3d-prepare-options lower-harness) {:zoom zoom}))
          (region3d-painters lower-harness :region))
        install-pressure!
        (fn []
          (let [owner (region3d-gpu/binding-owner region-system)
                primary (first (filter #(= region3d-id (:region/id %))
                                       (region-bindings/desired-rows owner)))
                physical (compositor-gpu/region-lease
                          lower-compositor lower-resolution-pressure-id)]
            (region-bindings/reconcile-desired!
             owner [primary pressure-row])
            (region-bindings/record-lease!
             owner lower-resolution-pressure-id (or physical pressure-lease))))
        mutated-region
        (-> region
            (assoc-in [:scene :near :material :base-color]
                      (region3d-tagged 0.12 0.92 0.28))
            region3d-material/validate-region!)
        mutated-op (assoc op :region3d/scene mutated-region)
        steps
        [(fn [_]
           (let [frame (prepare-frame op 2.0)]
             (install-pressure!)
             (region3d-lower-step! lower-harness frame)))
         (fn [state]
           (let [frame (prepare-frame op 8.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :worn %))))
         (fn [state]
           (let [frame (prepare-frame mutated-op 8.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :mutated % :mutated-frame frame))))
         (fn [{:keys [mutated-frame] :as state}]
           (.then (region3d-lower-step! lower-harness mutated-frame)
                  #(assoc state :held %)))
         (fn [state]
           (let [frame (prepare-frame mutated-op 10.0)]
             (install-pressure!)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :honest-counter %))))
         (fn [state]
           (let [frame (prepare-frame mutated-op 8.0)
                 owner (region3d-gpu/binding-owner region-system)
                 primary (first (region-bindings/desired-rows owner))]
             (region-bindings/reconcile-desired! owner [primary])
             (compositor-gpu/release-region-lease!
              lower-compositor lower-resolution-pressure-id)
             (.then (region3d-lower-step! lower-harness frame)
                    #(assoc state :recovered %))))
         (fn [state]
           (let [no-shadow-region
                 (-> region
                     (assoc-in [:scene :sun :light :cast-shadow] false)
                     region3d-material/validate-region!)
                 no-shadow-op (assoc op :region3d/scene no-shadow-region)]
             (.then
              (region3d-refusal-leg! harness no-shadow-region no-shadow-op
                                     (* 3 1024 1024))
              #(assoc state :no-shadow-floor %))))
         (fn [state]
           (.then
            (region3d-refusal-leg! harness region op (* 5 1024 1024))
            #(assoc state :shadowed-floor %)))
         (fn [{:keys [bytes worn mutated held honest-counter recovered
                      no-shadow-floor shadowed-floor]
               :as state}]
           (-> (js/Promise.all
                #js [(sha256-bytes (get-in state [:mutated :bytes]))
                     (sha256-bytes (get-in state [:held :bytes]))
                     (sha256-bytes (:bytes no-shadow-floor))
                     (sha256-bytes (:bytes shadowed-floor))])
               (.then
                (fn [hashes]
                  (let [sharp state
                        primary-lease
                        (fn [step]
                          (first
                           (filter #(= region3d-id (:region-id %))
                                   (vals (get-in step
                                                 [:receipt :region-leases
                                                  :leases])))))
                        worn-lease (primary-lease worn)
                        mutated-passes (:passes mutated)
                        held-passes (:passes held)
                        recovered-lease (primary-lease recovered)
                        wear-sample (pixel-rgba (:bytes mutated) 100 24)
                        maintained (assoc (region3d-scene/derive-scene
                                           mutated-region)
                                          :region-id region3d-id)
                        pick-camera (region3d-scene/camera-matrices
                                     (:view-default mutated-region)
                                     [640.0 704.0])
                        object-pick (region3d-scene/pick-region
                                     {:maintained maintained
                                      :camera pick-camera
                                      :region-point [320.0 352.0]})
                        background-pick (region3d-scene/pick-region
                                         {:maintained maintained
                                          :camera pick-camera
                                          :region-point [5.0 5.0]})
                        reserve-preserved?
                        (pool-holds-free-target? pool reserve-target)
                        physical-crossing?
                        (and (= 1 (get-in worn [:receipt :lease-activity
                                               :region-binding-updates]))
                             (= 1 (get-in worn [:receipt :lease-activity :leases-acquired]))
                             (= 1 (get-in worn [:receipt :lease-activity :leases-retired]))
                             (= 1 (get-in worn [:receipt :lease-activity :region-rungs-worn])))
                        current-content?
                        (and (> (byte-delta bytes (:bytes mutated)) 2)
                             (every? :encoded? mutated-passes))
                        held-stable?
                        (and (zero? (get-in held [:receipt :lease-activity
                                                 :region-binding-updates]))
                             (zero? (get-in held [:receipt :lease-activity :leases-acquired]))
                             (zero? (get-in held [:receipt :lease-activity :leases-retired]))
                             (every? #(and (:held? %) (not (:encoded? %)))
                                     held-passes))
                        honest-counter?
                        (and (= [512 512]
                                (get-in honest-counter
                                        [:receipt :region-leases :leases
                                         [:region3d/verifier 512 512] :size]))
                             (zero? (get-in honest-counter
                                            [:receipt :lease-activity :region-binding-updates]))
                             (zero? (get-in honest-counter
                                            [:receipt :lease-activity :leases-acquired]))
                             (zero? (get-in honest-counter
                                            [:receipt :lease-activity :leases-retired])))
                        recovery?
                        (and (= 1 (:rung-divisor recovered-lease))
                             (= [768 768] (:size recovered-lease))
                             (= 1 (get-in recovered
                                          [:receipt :lease-activity :region-binding-updates]))
                             (= 1 (get-in recovered [:receipt :lease-activity :leases-acquired]))
                             (= 1 (get-in recovered [:receipt :lease-activity :leases-retired]))
                             (= 1 (get-in recovered
                                          [:receipt :lease-activity :region-rung-recoveries])))
                        floor-identical? (= (aget hashes 2) (aget hashes 3))
                        pick? (and (= :near (:object-id object-pick))
                                   (= :region-background
                                      (:route background-pick)))
                        deterministic? (= (aget hashes 0) (aget hashes 1))
                        worn? (and (= 2 (:rung-divisor worn-lease))
                                   (= [512 512] (:size worn-lease))
                                   (empty? (get-in worn [:receipt :pool
                                                         :refusals]))
                                   (nil? (get-in worn [:receipt
                                                       :last-region-refusal])))
                        glyph? (and (> (nth wear-sample 1) (nth wear-sample 0))
                                    (> (nth wear-sample 2) (nth wear-sample 0)))
                        pass? (and (not (:refused? pressure-lease))
                                   worn? physical-crossing? current-content?
                                   held-stable? honest-counter? recovery?
                                   reserve-preserved? deterministic? floor-identical?
                                   (:pass? no-shadow-floor)
                                   (:pass? shadowed-floor) pick? glyph?)
                        pair {:bytes (:bytes mutated)
                              :first-sha256 (aget hashes 0)
                              :second-sha256 (aget hashes 1)
                              :byte-identical? deterministic?}
                        result {:image (region3d-image-record "worn" pair)
                                :sharp {:lease (primary-lease sharp)
                                        :lease-activity (get-in sharp [:receipt :lease-activity])}
                                :worn {:lease worn-lease :lease-activity (get-in worn [:receipt :lease-activity])
                                       :rung-receipts
                                       (get-in worn [:receipt
                                                     :region-rung-receipts])}
                                :current-content? current-content?
                                :wear-sample wear-sample :glyph? glyph?
                                :held-stable? held-stable?
                                :honest-counter? honest-counter?
                                :recovery {:lease recovered-lease
                                           :lease-activity (get-in recovered [:receipt :lease-activity])
                                           :pass? recovery?}
                                :reserve-preserved? reserve-preserved?
                                :pick {:object (:object-id object-pick)
                                       :background (:route background-pick)
                                       :pass? pick?}
                                :floor {:no-shadow
                                        (dissoc no-shadow-floor :bytes)
                                        :shadowed
                                        (dissoc shadowed-floor :bytes)
                                        :byte-identical? floor-identical?}
                                :deterministic? deterministic?
                                :pass? pass?}]
                    (compositor-gpu/destroy-compositor! lower-compositor)
                    (region3d-gpu/attach-compositor! region-system compositor)
                    (region3d-gpu/prepare-region3d-frame!
                     region-system {:regions []} {} {:zoom 1.0 :dpr 1.0})
                    result)))))]]
    (reduce (fn [promise step] (.then promise step))
            (js/Promise.resolve nil)
            steps)))

(defn- run-region3d-floor! [device adapter font-assets]
  (let [tracker (gpu-budget/create-tracker
                 (gpu-budget/snapshot-adapter-limits adapter))
        camera (renderer/create-camera-buffer device tracker)
        containers-buffer (renderer/create-containers-buffer device tracker)
        _ (renderer/update-camera device camera (js/Float32Array. 6)
                                  0.0 0.0 1.0 canvas-size canvas-size)
        _ (renderer/write-containers!
           device containers-buffer
           {0 {:affine containers/identity-affine :flags 0 :layer 0
               :stack-path [[0 0]] :transport-slot 0}})
        surround-path-system
        (path-gpu/init-path-system
         device "rgba16float" camera containers-buffer
         :initial-capacity 16 :tracker tracker
         :scene-color (scene-color/scene-color true))
        surround-ops
        [(path-op
          :region3d/below
          (path-polygon-material
           :region3d/below
           [[8.0 8.0] [120.0 8.0] [120.0 120.0] [8.0 120.0]]
           [0.04 0.07 0.15 1.0] 1.0))
         (path-op
          :region3d/above
          (path-polygon-material
           :region3d/above
           [[10.0 58.0] [118.0 58.0] [118.0 70.0] [10.0 70.0]]
           [0.98 0.72 0.12 0.88] 1.0))]
        _ (path-gpu/prepare-path-frame! surround-path-system surround-ops 1.0)
        region-system (region3d-gpu/ensure-region3d-system!
                       device tracker camera containers-buffer)
        path-system
        (path-gpu/init-path-system
         device "rgba16float" camera containers-buffer
         :tracker tracker :scene-color (scene-color/scene-color true))
        compositor (compositor-gpu/create-compositor!
                    device color-format tracker)
        harness {:device device :tracker tracker :camera camera
                 :containers-buffer containers-buffer
                 :surround-path-system surround-path-system
                 :region-system region-system
                 :path-system path-system
                 :font-assets font-assets :compositor compositor}
        opaque-region (region3d-fixture-region :opaque)
        transparent-region (region3d-fixture-region :transparent)
        opaque-op (region3d-op opaque-region)
        transparent-op (region3d-op transparent-region)
        seam (region3d-seam-fixture)
        seam-region (:region seam)
        seam-op (:op seam)
        specs [{:case-id "sandwich" :region opaque-region :op opaque-op
                :session {} :sides :sandwich}
               {:case-id "lit-depth-shadow" :region transparent-region
                :op transparent-op :session {} :sides :region}
               {:case-id "placed-depth-interleave" :region seam-region
                :op seam-op :session {} :sides :sandwich
                :seam? true}]]
    (-> (promise-mapv
         (fn [{:keys [case-id region op session sides seam?]}]
           (-> (region3d-capture-pair! harness op session sides)
               (.then
                (fn [pair]
                  (let [placement-receipt
                        (:placements
                         (region3d-gpu/region3d-receipt region-system))]
                    {:case-id case-id :zoom 1.0
                     :regime :region3d-floor-default
                     :normalization :region-local-3d-inside-world-2d
                     :shape-extent-world [(:w op) (:h op)]
                     :oracle (when (= case-id "lit-depth-shadow")
                               (region3d-lit-oracle region (:bytes pair)))
                     :seam-receipt
                     (when seam?
                       {:resolved (count (filter #(= :resolved (:status %))
                                                 (:region3d/resolved-placements op)))
                        :ink-vertices (:ink-vertices placement-receipt)})
                     :images [(region3d-image-record case-id pair)]})))))
         specs)
        (.then
         (fn [cases]
           (-> (region3d-capture! harness transparent-op {} :region)
               (.then
                (fn [_]
                  (-> (region3d-s5-lifecycle!
                       harness transparent-region transparent-op)
                      (.then
                       (fn [s5]
                         (-> (region3d-lower-resolution!
                              harness transparent-region transparent-op)
                             (.then (fn [lower]
                                      {:cases cases :s5 s5 :lower lower})))))))))))
        (.then
         (fn [{:keys [cases s5 lower]}]
           (let [base-cases cases
                 s2 (get-in base-cases [1 :oracle])
                 s4 s2
                 cases (conj base-cases
                             {:case-id "worn" :zoom 8.0
                              :regime :region3d-floor-worn
                              :normalization :region-local-3d-inside-world-2d
                              :shape-extent-world [(:w transparent-op)
                                                   (:h transparent-op)]
                              :images [(:image lower)]})
                 determinism (mapcat #(map :determinism (:images %)) cases)
                 system-receipt (region3d-gpu/region3d-receipt region-system)
                 seam-receipt (:seam-receipt (last base-cases))
                 compositor-receipt
                 (compositor-gpu/compositor-receipt compositor)
                 seam-pass? (and (= 1 (:resolved seam-receipt))
                                 (pos? (or (:ink-vertices seam-receipt) 0)))
                 pass? (and (= 4 (count cases))
                            (every? :byte-identical? determinism)
                            (:pass? s2) (:pass? s4) (:pass? s5) seam-pass?
                            (:pass? lower))
                 result {:cases cases
                         :s2 s2 :s4 s4 :s5 s5
                         :lower-resolution (dissoc lower :image)
                         :seam (assoc seam-receipt :pass? seam-pass?)
                         :system system-receipt
                         :compositor compositor-receipt
                         :fixture-query "?region3d=1"
                         :pass? pass?}]
             (compositor-gpu/destroy-compositor! compositor)
             (path-gpu/destroy-path-system! path-system)
             (path-gpu/destroy-path-system! surround-path-system)
             (region3d-gpu/destroy-region3d-system! region-system)
             result))))))

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
                                    (fonts/load-font-assets t1-font-config)])
                             (.then
                              (fn [font-values]
                                (let [slug-assets (aget font-values 0)
                                      t1-assets (aget font-values 1)
                                      t1-receipt
                                      (try
                                        (t1-layout-receipt
                                         (:layout-provider t1-assets))
                                        (catch :default error
                                          (assoc (or (ex-data error) {})
                                                 :pass false
                                                 :foreign-failure
                                                 "T1 browser layout receipt failed.")))]
                                (js/console.log "[W0-A] init-font-assets")
                                (let [camera-buffer (renderer/create-camera-buffer device nil)
                                      containers-buffer (renderer/create-containers-buffer device nil)
                                      q8-transport (run-q8-transport! device containers-buffer)
                                      _ (js/console.log "[W0-A] init-shared-buffers")
                                      slug-system (do
                                                    (js/console.log "[W0-A] init-slug-pipeline-start")
                                                    (let [system
                                                          (renderer/init-text-system
                                                           device color-format camera-buffer slug-assets
                                                           :initial-capacity 1
                                                           :containers-buffer containers-buffer)]
                                                      (js/console.log "[W0-A] init-slug-pipeline-complete")
                                                      system))
                                      ubuntu-system
                                      (renderer/init-text-system
                                       device color-format camera-buffer t1-assets
                                       :initial-capacity 2
                                       :containers-buffer containers-buffer)
                                      curves (do
                                               (js/console.log "[W0-A] init-curve-decode-start")
                                               (let [decoded (decode-glyph-curves slug-assets 111)]
                                                 (js/console.log "[W0-A] init-curve-decode-complete"
                                                                 (count decoded))
                                                 decoded))
                                      harness {:device device
                                               :slug-system slug-system
                                               :slug-assets slug-assets
                                               :curves curves}]
                                  (-> (js/Promise.all
                                       #js [(promise-mapv (partial run-case! harness) zoom-cases)
                                            (run-ubuntu-mixed-case! device ubuntu-system t1-assets)
                                            (shader-digests)
                                            (run-image-atom! device adapter)
                                            (run-path-atom! device adapter)
                                            (run-chrome-atom! device adapter)
                                            (run-region3d-floor! device adapter
                                                                 t1-assets)])
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
                                                 :slug (:slug font-config)}
                                          :decoded-slug-curve-count (count curves)
                                          :shader-digests (aget values 2)
                                          :q8-transport q8-transport
                                          :t1-layout t1-receipt
                                          :ubuntu-slug (aget values 1)
                                          :image-atom (aget values 3)
                                          :path-atom (aget values 4)
                                          :chrome-atom (aget values 5)
                                          :region3d-floor (aget values 6)
                                          :cases (aget values 0)})))))))))))))))))))

(defn ^:export run-region3d-floor-verifier! []
  (when-not (and (.-isSecureContext js/window)
                 (exists? js/navigator.gpu))
    (throw (js/Error. "Region3D floor verifier requires WebGPU")))
  (-> (.requestAdapter js/navigator.gpu)
      (.then
       (fn [^js adapter]
         (when-not adapter
           (throw (js/Error. "Region3D floor verifier has no adapter")))
         (-> (.requestDevice adapter)
             (.then
              (fn [^js device]
                (-> (fonts/load-font-manifest-async)
                    (.then
                     (fn [manifest]
                       (let [font-config
                             (first (filter #(= "ubuntu-sans-variable" (:id %))
                                            (:fonts manifest)))]
                         (when-not font-config
                           (throw (js/Error. "Region3D verifier font is absent")))
                         (-> (fonts/load-font-assets font-config)
                             (.then
                              (fn [font-assets]
                                (-> (js/Promise.all
                                     #js [(shader-digests)
                                          (run-region3d-floor!
                                           device adapter font-assets)])
                                    (.then
                                     (fn [values]
                                       {:schema-version 2
                                        :verifier "softland-region3d-floor"
                                        :secure-context? (.-isSecureContext js/window)
                                        :user-agent (.-userAgent js/navigator)
                                        :adapter (adapter-information adapter)
                                        :device-limits
                                        (selected-limits (.-limits device))
                                        :canvas {:width canvas-size
                                                 :height canvas-size
                                                 :device-pixel-ratio
                                                 (.-devicePixelRatio js/window)
                                                 :color-format color-format}
                                        :shader-digests (aget values 0)
                                        :region3d-floor (aget values 1)})))))))))))))))))

(defn ^:export start! []
  (js/console.log "[W0-A] start")
  (set! (.-__renderVerifierDone js/window) false)
  ;; Yield once so CDP can publish the boot marker before any browser/driver
  ;; implementation performs synchronous pipeline compilation.
  (js/setTimeout
   (fn []
     (js/console.log "[W0-A] scheduled-callback")
     (try
       (let [params (js/URLSearchParams. (.-search js/location))
             runner (if (.has params "region3d-floor-only")
                      run-region3d-floor-verifier!
                      run-verifier!)]
       (-> (runner)
           (.then
            (fn [result]
              (set! (.-__renderVerifierResult js/window) (clj->js result))
              (set! (.-__renderVerifierDone js/window) true)))
           (.catch
            (fn [error]
              (set! (.-__renderVerifierResult js/window)
                    #js {:fatal (str error)
                         :stack (.-stack error)})
              (set! (.-__renderVerifierDone js/window) true)))))
       (catch :default error
         (set! (.-__renderVerifierResult js/window)
               #js {:fatal (str error)
                    :stack (.-stack error)})
         (set! (.-__renderVerifierDone js/window) true))))
   0))
