(ns app.client.engine.device
  (:require [clojure.string :as str]
            [app.client.engine.budget :as gpu-budget]
            [app.client.engine.color :as scene-color]))

(def ^:private scene-color-mode-declaration
  "const kSceneColorLinearPremultiplied: bool = false;")

(def ^:private scene-color-wgsl
  (str scene-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
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

(defn- configure-scene-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader scene-color-mode-declaration
                 "const kSceneColorLinearPremultiplied: bool = true;")
    shader))

(defn- scene-color-blend [color]
  (let [{[color-src color-dst] :color
         [alpha-src alpha-dst] :alpha} (:blend color)]
    {:color {:srcFactor (name color-src) :dstFactor (name color-dst)}
     :alpha {:srcFactor (name alpha-src) :dstFactor (name alpha-dst)}}))

;; --- W2-A/Q8: shared compact affine transport ------------------------------
;; One 32-byte storage entry per LIVE transform slot:
;; [axis-x.xy, axis-y.xy, translation.xy, flags:u32, pad:u32]. Semantic cids do
;; not index this table; containers/effective assigns compact stable slots.
;; Q8 priced 1,024 / 4,096 / 16,384 entries, and the largest measured tier is
;; the production allocation. Growing later rebinds the same storage scheme; it
;; is not another representation migration after atom multiplication.
(def affine-entry-bytes 32)

(def max-transform-nodes 16384)

(defn create-containers-buffer
  "Create the shared Q8 affine storage buffer and write identity slot 0.
   Shared across all four transform-consuming pipelines like the camera."
  [^js/GPUDevice device tracker]
  (let [size (* max-transform-nodes affine-entry-bytes)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.STORAGE
                                                              js/GPUBufferUsage.COPY_DST)}))
        identity0 (js/Float32Array. #js [1.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0])]
    (gpu-budget/register-buffer! tracker buffer "containers/affine-storage" size
                                 :active-bytes affine-entry-bytes)
    (.writeBuffer (.-queue device) buffer 0 identity0)
    buffer))

(defn write-containers!
  "Upload containers/effective through compact :transport-slot values. Sparse
   semantic cids never allocate holes. Returns a machine receipt used by the
   1,024/4,096/16,384 Q8 verifier."
  [^js/GPUDevice device ^js containers-buffer effective]
  (let [entries (vals effective)
        slots (map :transport-slot entries)
        _ (when (some nil? slots)
            (throw (ex-info "Affine transport entry lacks :transport-slot"
                            {:missing (count (filter nil? slots))})))
        _ (when-not (= (count slots) (count (set slots)))
            (throw (ex-info "Affine transport slots must be unique"
                            {:slots slots})))
        max-slot (if (seq slots) (apply max slots) 0)
        entry-count (inc max-slot)
        _ (when (> entry-count max-transform-nodes)
            (throw (ex-info "Live affine transport exceeds the Q8 capacity"
                            {:entries entry-count :max max-transform-nodes})))
        raw (js/ArrayBuffer. (* entry-count affine-entry-bytes))
        floats (js/Float32Array. raw)
        uints (js/Uint32Array. raw)]
    ;; Holes can only occur in a hand-built effective map; make them identity,
    ;; never a singular zero matrix. Normal registries allocate densely.
    (dotimes [slot entry-count]
      (let [base (* slot 8)]
        (aset floats (+ base 0) 1.0)
        (aset floats (+ base 3) 1.0)))
    (doseq [{:keys [affine flags transport-slot]} entries]
      (let [[a b c d tx ty] affine
            base (* transport-slot 8)]
        (when-not (= 6 (count affine))
          (throw (ex-info "Affine transport requires [a b c d tx ty]"
                          {:affine affine :transport-slot transport-slot})))
        (aset floats (+ base 0) a)
        (aset floats (+ base 1) b)
        (aset floats (+ base 2) c)
        (aset floats (+ base 3) d)
        (aset floats (+ base 4) tx)
        (aset floats (+ base 5) ty)
        (aset uints (+ base 6) (or flags 0))
        (aset uints (+ base 7) 0)))
    (.writeBuffer (.-queue device) containers-buffer 0 (js/Uint8Array. raw))
    {:entries entry-count
     :bytes (* entry-count affine-entry-bytes)
     :max-slot max-slot
     :capacity max-transform-nodes}))

(defn create-camera-buffer
  [^js/GPUDevice device tracker]
  (let [camera-buffer (.createBuffer device (clj->js {:size 24
                                                      :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                     js/GPUBufferUsage.COPY_DST)}))]
    (gpu-budget/register-buffer! tracker camera-buffer "text/shared-camera" 24 :active-bytes 24)
    camera-buffer))

;; --- Clear-quad system (Phase 6E: dirty-present) ---
(def clear-quad-shader "
  @vertex
  fn vs_main(@builtin(vertex_index) v: u32) -> @builtin(position) vec4<f32> {
      // Fullscreen triangle from vertex index — no vertex buffer needed
      let x = f32(i32(v & 1u)) * 4.0 - 1.0;
      let y = f32(i32(v >> 1u)) * 4.0 - 1.0;
      return vec4<f32>(x, y, 0.0, 1.0);
  }
  @fragment
  fn fs_main() -> @location(0) vec4<f32> {
      return vec4<f32>(0.0, 0.0, 0.0, 1.0);
  }")

(defn- configure-clear-quad-shader [color]
  (let [[r g b a] (:clear color)]
    (str/replace clear-quad-shader
                 "return vec4<f32>(0.0, 0.0, 0.0, 1.0);"
                 (str "return vec4<f32>(" r ", " g ", " b ", " a ");"))))

(defn- clear-value [color]
  (let [[r g b a] (:clear color)]
    {:r r :g g :b b :a a}))

(defn init-clear-quad
  [^js/GPUDevice device fformat & {:keys [scene-color]
                                   :or {scene-color scene-color/legacy-direct-color}}]
  (let [module (.createShaderModule device
                                   (clj->js {:code (configure-clear-quad-shader
                                                    scene-color)}))
        layout (.createPipelineLayout device (clj->js {:bindGroupLayouts []}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout layout
                             :vertex {:module module :entryPoint "vs_main"}
                             :fragment {:module module :entryPoint "fs_main"
                                        :targets [{:format fformat
                                                   :writeMask 0xF}]}
                             :primitive {:topology "triangle-list"}}))]
    {:pipeline pipeline
     :scene-color scene-color}))

;; --- Persistent render target (Phase 6E: survives swap chain double-buffering) ---

(defn create-render-target
  [^js device width height fformat & {:keys [tracker label previous scene-color]
                                      :or {label "render-target/persistent"
                                           scene-color scene-color/legacy-direct-color}}]
  (let [safe-width (max 1 width)
        safe-height (max 1 height)
        tex (.createTexture device
              (clj->js {:size {:width safe-width :height safe-height}
                        :format fformat
                        :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                       js/GPUTextureUsage.COPY_SRC)}))
        old-texture (:texture previous)]
    (js/console.log "[RENDERER] Create render target"
                    {:label label
                     :width safe-width
                     :height safe-height
                     :format fformat
                     :replacing? (boolean old-texture)})
    (if old-texture
      (gpu-budget/replace-texture! tracker old-texture tex label
                                   :format fformat
                                   :width safe-width
                                   :height safe-height
                                   :reason :render-target-resize)
      (gpu-budget/register-texture! tracker tex label
                                    :format fformat
                                    :width safe-width
                                    :height safe-height))
    (when old-texture
      (.destroy ^js old-texture))
    {:texture tex
     :view (.createView tex)
     :width safe-width
     :height safe-height
     :resource/id :scene-color/main
     :scene-color scene-color
     :gpu-tracker tracker
     :gpu-label label}))

(defn destroy-render-target! [{:keys [^js texture gpu-tracker]}]
  (when texture
    (gpu-budget/destroy-resource! gpu-tracker texture :reason :render-target-destroy)
    (.destroy texture)))

(defn- scene-color-resource
  "Resolve the one frame scene-color resource.  With no persistent target this
   is the direct-present swap view; when the existing default-off target is
   enabled it becomes the intermediate view.  Future group targets extend this
   resource shape instead of creating another frame path."
  [swap-view render-target color]
  {:resource/id :scene-color/main
   :resource/mode (if render-target :intermediate :direct-present)
   :view (if render-target (:view render-target) swap-view)
   :format (:format render-target)
   :color color})

(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))

(defn- clip-execution-mode
  "A screen-axis-aligned effective affine uses a scissor. Rotation/shear is a
   declared mask-road binding; callers must never approximate it as a scissor."
  [[a b c d _tx _ty]]
  (if (or (and (< (abs (double b)) 1.0e-9)
               (< (abs (double c)) 1.0e-9))
          (and (< (abs (double a)) 1.0e-9)
               (< (abs (double d)) 1.0e-9)))
    :scissor
    :mask))

(defn project-clip-rect
  "Project a container-local clip into WebGPU attachment pixels. Camera and
   container coordinates are CSS pixels; scissor rectangles are device pixels,
   so the viewport-to-attachment scale is part of the projection."
  [clip container effective-transforms pan-x pan-y zoom attachment-size
   viewport-size]
  (when clip
    (let [{:keys [affine flags]}
          (or (get effective-transforms container)
              {:affine [1.0 0.0 0.0 1.0 0.0 0.0] :flags 0})]
      (let [[a b c d tx ty] affine
            {:keys [x y w h]} clip
            points [[x y] [(+ x w) y] [x (+ y h)] [(+ x w) (+ y h)]]
            screen? (= 1 (bit-and (or flags 0) 1))
            zm (if screen? 1.0 zoom)
            px (if screen? 0.0 pan-x)
            py (if screen? 0.0 pan-y)
            [aw ah] attachment-size
            [vw vh] (or viewport-size attachment-size)
            device-x (/ aw (max 1.0 vw))
            device-y (/ ah (max 1.0 vh))
            projected (map (fn [[lx ly]]
                             [(* device-x
                                 (+ (* (+ (* a lx) (* c ly) tx) zm) px))
                              (* device-y
                                 (+ (* (+ (* b lx) (* d ly) ty) zm) py))])
                           points)
            xs (map first projected)
            ys (map second projected)
            x0 (int (js/Math.floor (apply min xs)))
            y0 (int (js/Math.floor (apply min ys)))
            x1 (int (js/Math.ceil (apply max xs)))
            y1 (int (js/Math.ceil (apply max ys)))
            cx (max 0 (min aw x0))
            cy (max 0 (min ah y0))]
        (if (= :scissor (clip-execution-mode affine))
          {:mode :scissor
           :x cx :y cy :w (max 0 (- (max cx (min aw x1)) cx))
           :h (max 0 (- (max cy (min ah y1)) cy))}
          ;; Perimeter order (tl, tr, br, bl) feeds the generic convex mask
          ;; pass. A rotated/sheared clip is never approximated by its AABB.
          {:mode :mask :points [(nth projected 0) (nth projected 1)
                                (nth projected 3) (nth projected 2)]
           :container container :local-clip clip})))))
