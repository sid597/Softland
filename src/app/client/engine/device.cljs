(ns app.client.engine.device
  "The GPU pieces every entity kind shares: the camera and group buffers,
   render targets, the clear quad, the shared color-mode shader text, and clip-
   rect projection.
   Takes: a WebGPU device; camera pan, zoom, and viewport size; the group-id
   table; a group-local clip rect.
   Gives: GPU buffers, textures, and render targets the renderers draw into; a
   scissor or mask for a clip.
  Holds nothing."
  (:require [clojure.string :as str]
            [app.client.engine.color :as scene-color]))

(def ^:private scene-color-mode-declaration
  "const kSceneColorLinearPremultiplied: bool = false;")

(def scene-color-wgsl
  (str scene-color-mode-declaration "\n"
       "const kSrgbEncodedCutoff: f32 = " scene-color/srgb-encoded-cutoff ";\n"
       "const kSrgbLinearScale: f32 = " scene-color/srgb-linear-scale ";\n"
       "const kSrgbTransferScale: f32 = " scene-color/srgb-transfer-scale ";\n"
       "const kSrgbTransferOffset: f32 = " scene-color/srgb-transfer-offset ";\n"
       "const kSrgbTransferExponent: f32 = " scene-color/srgb-transfer-exponent ";\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= kSrgbEncodedCutoff) { return v / kSrgbLinearScale; }\n"
       "  return pow((v + kSrgbTransferOffset) / kSrgbTransferScale, kSrgbTransferExponent);\n"
       "}\n"
       "fn scene_color(straight: vec4<f32>, coverage: f32) -> vec4<f32> {\n"
       "  if (!kSceneColorLinearPremultiplied) {\n"
       "    return vec4<f32>(straight.rgb, straight.a * coverage);\n"
       "  }\n"
       "  let alpha = clamp(straight.a * coverage, 0.0, 1.0);\n"
       "  let linear = vec3<f32>(srgb_channel_to_linear(straight.r),\n"
       "                         srgb_channel_to_linear(straight.g),\n"
       "                         srgb_channel_to_linear(straight.b));\n"
       "  return vec4<f32>(linear * alpha, alpha);\n"
       "}\n"))

(defn configure-scene-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader scene-color-mode-declaration
                 "const kSceneColorLinearPremultiplied: bool = true;")
    shader))

(defn scene-color-blend [color]
  (let [{[color-src color-dst] :color
         [alpha-src alpha-dst] :alpha} (:blend color)]
    {:color {:srcFactor (name color-src) :dstFactor (name color-dst)}
     :alpha {:srcFactor (name alpha-src) :dstFactor (name alpha-dst)}}))

;; --- shared compact affine transport ---------------------------------------
;; One 32-byte storage entry per LIVE transform buffer index:
;; [axis-x.xy, axis-y.xy, translation.xy, flags:u32, pad:u32]. Semantic group ids do
;; not index this table; transform/world-transforms assigns compact stable buffer indexes.
;; The transport was priced at 1,024 / 4,096 / 16,384 entries, and the largest measured tier is
;; the production allocation. Growing later rebinds the same storage scheme; it
;; is not another representation migration after step multiplication.
(def affine-entry-bytes 32)

(def max-transform-nodes 16384)

(defn create-groups-buffer
  "Create the shared affine storage buffer and write identity buffer index 0.
   Shared across all four transform-consuming pipelines like the camera."
  [^js/GPUDevice device]
  (let [size (* max-transform-nodes affine-entry-bytes)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.STORAGE
                                                              js/GPUBufferUsage.COPY_DST)}))
        identity0 (js/Float32Array. #js [1.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0])]
    (.writeBuffer (.-queue device) buffer 0 identity0)
    buffer))

(defn write-groups!
  "Upload transform/world-transforms through compact :buffer-index values. Sparse
   semantic group ids never allocate holes. Returns machine stats used by the
   1,024/4,096/16,384 capacity harness."
  [^js/GPUDevice device ^js groups-buffer world-transforms]
  (let [entries (vals world-transforms)
        buffer-indexes (map :buffer-index entries)
        _ (when (some nil? buffer-indexes)
            (throw (ex-info "Affine transport entry lacks :buffer-index"
                            {:missing (count (filter nil? buffer-indexes))})))
        _ (when-not (= (count buffer-indexes) (count (set buffer-indexes)))
            (throw (ex-info "Affine buffer indexes must be unique"
                            {:buffer-indexes buffer-indexes})))
        max-buffer-index (if (seq buffer-indexes) (apply max buffer-indexes) 0)
        entry-count (inc max-buffer-index)
        _ (when (> entry-count max-transform-nodes)
            (throw (ex-info "Live affine transport exceeds its configured capacity"
                            {:entries entry-count :max max-transform-nodes})))
        raw (js/ArrayBuffer. (* entry-count affine-entry-bytes))
        floats (js/Float32Array. raw)
        uints (js/Uint32Array. raw)]
    ;; Holes can only occur in a hand-built world-transforms map; make them identity,
    ;; never a singular zero matrix. Normal registries allocate densely.
    (dotimes [buffer-index entry-count]
      (let [base (* buffer-index 8)]
        (aset floats (+ base 0) 1.0)
        (aset floats (+ base 3) 1.0)))
    (doseq [{:keys [affine flags buffer-index]} entries]
      (let [[a b c d tx ty] affine
            base (* buffer-index 8)]
        (when-not (= 6 (count affine))
          (throw (ex-info "Affine transport requires [a b c d tx ty]"
                          {:affine affine :buffer-index buffer-index})))
        (aset floats (+ base 0) a)
        (aset floats (+ base 1) b)
        (aset floats (+ base 2) c)
        (aset floats (+ base 3) d)
        (aset floats (+ base 4) tx)
        (aset floats (+ base 5) ty)
        (aset uints (+ base 6) (or flags 0))
        (aset uints (+ base 7) 0)))
    (.writeBuffer (.-queue device) groups-buffer 0 (js/Uint8Array. raw))
    {:entries entry-count
     :bytes (* entry-count affine-entry-bytes)
     :max-buffer-index max-buffer-index
     :capacity max-transform-nodes}))

(defn create-camera-buffer
  [^js/GPUDevice device]
  (let [camera-buffer (.createBuffer device (clj->js {:size 24
                                                      :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                     js/GPUBufferUsage.COPY_DST)}))]
    camera-buffer))

(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))
