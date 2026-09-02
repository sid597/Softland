(ns app.client.harness.shared
  "Shared browser-harness constants, transport, hashing, pixels, and evidence.
   Takes: browser/WebGPU values and kind shader sources.
   Gives: deterministic helpers consumed by the harness kind drivers.
   Holds nothing."
  (:require [app.client.engine.color :as color]
            [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.region3d.on-plane-renderer :as on-plane-renderer]
            [app.client.text.renderer :as text-renderer]))

(def canvas-size 128)
(def color-format "rgba8unorm")
(def glyph-screen-x 34.0)
(def glyph-screen-baseline 96.0)
(def glyph-screen-size 80.0)

(def zoom-cases
  [{:case-id "legal-min-z0p01" :zoom 0.01 :lod "legal-zoom-range-sentinel"}
   {:case-id "default-min-z0p1" :zoom 0.1 :lod "engine-default-clamp"}
   {:case-id "default-unit-z1" :zoom 1.0 :lod "engine-default-clamp"}
   {:case-id "default-max-z8" :zoom 8.0 :lod "engine-default-clamp"}
   {:case-id "legal-log-z10" :zoom 10.0 :lod "legal-zoom-range-sentinel"}
   {:case-id "legal-log-z100" :zoom 100.0 :lod "legal-zoom-range-sentinel"}
   {:case-id "legal-max-z1000" :zoom 1000.0 :lod "legal-zoom-range-sentinel"}])

(def image-fixtures
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

(defn promise-mapv [f xs]
  (reduce (fn [p x]
            (.then p
                   (fn [acc]
                     (.then (f x) #(conj acc %)))))
          (js/Promise.resolve [])
          xs))

(defn bytes->hex [^js bytes]
  (apply str
         (map (fn [b]
                (let [h (.toString b 16)]
                  (if (= 1 (count h)) (str "0" h) h)))
              (array-seq bytes))))

(defn sha256-bytes [^js bytes]
  (-> (.digest (.-subtle js/crypto) "SHA-256" bytes)
      (.then #(bytes->hex (js/Uint8Array. %)))))

(defn sha256-string [s]
  (sha256-bytes (.encode (js/TextEncoder.) s)))

(defn opaque-png-data-url [^js rgba]
  ;; The golden is the visible result over the harness's black clear color.
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

(defn q8-world-transforms [entry-count]
  ;; Semantic ids deliberately stride by 17, reproducing the Q8 sparse-id
  ;; pressure while transport buffer indexes remain dense 0..N-1.
  (into {}
        (map (fn [buffer-index]
               [(* buffer-index 17)
                {:affine transform/identity-affine
                 :flags 0
                 :layer 0
                 :stack-path [[(* buffer-index 17) 0]]
                 :buffer-index buffer-index}]))
        (range entry-count)))

(defn run-q8-transport! [device groups-buffer]
  (let [rows
        (mapv (fn [entry-count]
                (let [world-transforms (q8-world-transforms entry-count)
                      stats (device/write-groups! device groups-buffer world-transforms)
                      expected-bytes (* entry-count device/affine-entry-bytes)]
                  (assoc stats
                         :semantic-max-id (* 17 (dec entry-count))
                         :expected-bytes expected-bytes
                         :pass? (and (= entry-count (:entries stats))
                                     (= expected-bytes (:bytes stats))
                                     (= (dec entry-count) (:max-buffer-index stats))))))
              [1024 4096 16384])]
    {:entry-bytes device/affine-entry-bytes
     :transport "compact-read-only-storage"
     :rows rows
     :pass? (every? :pass? rows)}))

(defn boundary-pixels [^js rgba]
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
                  (let [coverage (aget rgba (* 4 (+ x (* y canvas-size))))]
                    (recur (inc x)
                           (if (and (> coverage 8) (< coverage 247))
                             (conj! acc [x y coverage])
                             acc))))))))))

(defn byte-delta [^js left ^js right]
  (loop [index 0 maximum 0]
    (if (= index (.-length left))
      maximum
      (recur (inc index)
             (max maximum
                  (js/Math.abs (- (aget left index) (aget right index))))))))

(defn pixel-rgba [^js bytes x y]
  (let [offset (* 4 (+ x (* y canvas-size)))]
    [(aget bytes offset) (aget bytes (+ offset 1))
     (aget bytes (+ offset 2)) (aget bytes (+ offset 3))]))

(defn srgb->linear [value]
  (color/srgb-channel->linear (/ value 255.0)))

(defn linear->srgb-byte [value]
  (js/Math.round
   (* 255.0 (color/linear->srgb-channel (max 0.0 (min 1.0 value))))))
(defn selected-limits [^js limits]
  {:max-buffer-size (.-maxBufferSize limits)
   :max-uniform-buffer-binding-size (.-maxUniformBufferBindingSize limits)
   :max-storage-buffer-binding-size (.-maxStorageBufferBindingSize limits)
   :max-texture-dimension-2d (.-maxTextureDimension2D limits)
   :max-bind-groups (.-maxBindGroups limits)
   :max-vertex-buffers (.-maxVertexBuffers limits)})

(defn adapter-information [^js adapter]
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

(defn shader-digests []
  (let [entries [["slug-vertex" text-renderer/slug-vertex-shader]
                 ["slug-fragment" text-renderer/slug-fragment-shader]
                 ["region3d-placed-flat"
                  on-plane-renderer/placed-flat-shader]]]
    (-> (promise-mapv (fn [[label source]]
                        (.then (sha256-string source)
                               (fn [digest] [label digest])))
                      entries)
        (.then #(into {} %)))))

(defn w4-read-texture! [^js device ^js texture width height]
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
