(ns app.client.substrate.webgpu.chrome-gpu
  "WebGPU projection for neutral quad marks. One private interleaved vertex
   buffer is repacked only when the mark mesh set changes; camera and followed
   container motion remain shader values."
  (:require [clojure.string :as str]
            [app.client.substrate.chrome-material :as chrome-material]
            [app.client.substrate.frame-inputs :as frame-inputs]
            [app.client.substrate.scene-tape :as scene-tape]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]))

(def chrome-color-mode-declaration
  "const kChromeLinearPremultiplied: bool = false;")

(def chrome-vertex-shader
  "struct Camera {
     pan: vec2<f32>, zoom: f32, padding: f32,
     screen_dimensions: vec2<f32>,
   };
   @group(0) @binding(0) var<uniform> camera: Camera;
   struct ContainerTransform {
     axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
     flags: u32, padding: u32,
   };
   @group(0) @binding(1) var<storage, read> containers: array<ContainerTransform>;
   struct VertexInput {
     @location(0) anchor: vec2<f32>,
     @location(1) offset_px: vec2<f32>,
     @location(2) color: vec4<f32>,
     @location(3) container_idx: u32,
   };
   struct VertexOutput {
     @builtin(position) position: vec4<f32>,
     @location(0) color: vec4<f32>,
   };
   @vertex fn main(input: VertexInput) -> VertexOutput {
     let c = containers[input.container_idx];
     let is_screen = (c.flags & 1u) != 0u;
     let zm = select(camera.zoom, 1.0, is_screen);
     let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
     let world_anchor = c.translation + c.axis_x * input.anchor.x +
                        c.axis_y * input.anchor.y;
     let screen_pos = world_anchor * zm + pn + input.offset_px;
     let ndc = (screen_pos / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
     var output: VertexOutput;
     output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
     output.color = input.color;
     return output;
   }")

(def chrome-fragment-shader
  (str chrome-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
       "}\n"
       "@fragment fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> {\n"
       "  var prepared = color;\n"
       "  if (!kChromeLinearPremultiplied) { return prepared; }\n"
       "  let alpha = clamp(prepared.a, 0.0, 1.0);\n"
       "  let linear = vec3<f32>(srgb_channel_to_linear(color.r),\n"
       "                         srgb_channel_to_linear(color.g),\n"
       "                         srgb_channel_to_linear(color.b));\n"
       "  return vec4<f32>(linear * alpha, alpha);\n"
       "}\n"))

(defn- configure-chrome-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader chrome-color-mode-declaration
                 "const kChromeLinearPremultiplied: bool = true;")
    shader))

(defn- scene-color-blend [color]
  (let [{[color-src color-dst] :color
         [alpha-src alpha-dst] :alpha} (:blend color)]
    {:color {:srcFactor (name color-src) :dstFactor (name color-dst)}
     :alpha {:srcFactor (name alpha-src) :dstFactor (name alpha-dst)}}))

(defn- create-vertex-buffer [^js device capacity]
  (.createBuffer device
                 (clj->js {:size (* (max 1 capacity)
                                    chrome-material/vertex-stride)
                           :usage (bit-or js/GPUBufferUsage.VERTEX
                                          js/GPUBufferUsage.COPY_DST)})))

(defn init-chrome-system
  [^js device fformat camera-buffer containers-buffer
   & {:keys [initial-capacity tracker scene-color]
      :or {initial-capacity 2048
           scene-color scene-tape/legacy-direct-color}}]
  (assert camera-buffer "init-chrome-system requires :camera-buffer")
  (assert containers-buffer "init-chrome-system requires :containers-buffer")
  (let [vertex-module (.createShaderModule
                       device (clj->js {:code chrome-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js {:code (configure-chrome-color-shader
                                         chrome-fragment-shader scene-color)}))
        bind-layout (.createBindGroupLayout
                     device
                     (clj->js
                      {:entries [{:binding 0 :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "uniform"}}
                                 {:binding 1 :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "read-only-storage"}}]}))
        pipeline-layout (.createPipelineLayout
                         device (clj->js {:bindGroupLayouts [bind-layout]}))
        pipeline (.createRenderPipeline
                  device
                  (clj->js
                   {:layout pipeline-layout
                    :vertex
                    {:module vertex-module :entryPoint "main"
                     :buffers [{:arrayStride chrome-material/vertex-stride
                                :stepMode "vertex"
                                :attributes
                                 [{:shaderLocation 0 :offset 0 :format "float32x2"}
                                 {:shaderLocation 1 :offset 8 :format "float32x2"}
                                 {:shaderLocation 2 :offset 16 :format "float32x4"}
                                 {:shaderLocation 3 :offset 32 :format "uint32"}]}]}
                    :fragment {:module fragment-module :entryPoint "main"
                               :targets [{:format fformat
                                          :blend (scene-color-blend scene-color)}]}
                    :primitive {:topology "triangle-list"
                                :frontFace "ccw" :cullMode "none"}}))
        bind-group (.createBindGroup
                    device
                    (clj->js {:layout bind-layout
                              :entries [{:binding 0 :resource {:buffer camera-buffer}}
                                        {:binding 1 :resource {:buffer containers-buffer}}]}))
        buffer (create-vertex-buffer device initial-capacity)]
    (gpu-budget/register-buffer! tracker buffer "chrome/vertices"
                                 (* initial-capacity chrome-material/vertex-stride)
                                 :active-bytes 0)
    {:device device :pipeline pipeline :bind-group bind-group
     :camera-buffer camera-buffer :containers-buffer containers-buffer
     :scene-color scene-color :gpu-tracker tracker
     :frame-input/identity (js-obj) :!shape-rev (atom 0)
     :!buffer (atom buffer) :!capacity (atom initial-capacity)
     :!prepared (atom []) :!last-chromes (atom ::never)
     :!last-mesh-set-key (atom ::never)
     :!receipt (atom {:chrome-system/version 1 :uploads 0 :vertices 0})}))

(defn- prepared-op [op first-vertex]
  (let [vertices (chrome-material/material-vertices (:chrome/material op))]
    {:op op :vertices vertices :first-vertex first-vertex
     :vertex-count (count vertices) :owner-vi (:owner-vi op)}))

(defn- prepare-ops [chromes]
  (loop [ops chromes first-vertex 0 result []]
    (if-let [op (first ops)]
      (let [row (prepared-op op first-vertex)]
        (recur (next ops) (+ first-vertex (:vertex-count row))
               (conj result row)))
      result)))

(defn- pack-vertices [prepared]
  (let [vertex-count (reduce + (map :vertex-count prepared))
        floats (js/Float32Array. (* vertex-count chrome-material/vertex-words))
        uints (js/Uint32Array. (.-buffer floats))]
    (loop [rows prepared vertex-offset 0]
      (if-let [{:keys [op vertices]} (first rows)]
        (do
          (doseq [[index vertex] (map-indexed vector vertices)]
            (let [base (* (+ vertex-offset index) chrome-material/vertex-words)
                  [ax ay ox oy r g b a container-idx]
                  (chrome-material/vertex-values vertex (:container-idx op))]
              (aset floats (+ base 0) ax)
              (aset floats (+ base 1) ay)
              (aset floats (+ base 2) ox)
              (aset floats (+ base 3) oy)
              (aset floats (+ base 4) r)
              (aset floats (+ base 5) g)
              (aset floats (+ base 6) b)
              (aset floats (+ base 7) a)
              (aset uints (+ base 8) container-idx)))
          (recur (next rows) (+ vertex-offset (count vertices))))
        floats))))

(defn- ensure-capacity! [chrome-system required]
  (let [capacity @(:!capacity chrome-system)]
    (when (> required capacity)
      (let [next-capacity (loop [candidate (max 1 capacity)]
                            (if (>= candidate required) candidate
                                (recur (* 2 candidate))))
            old-buffer @(:!buffer chrome-system)
            new-buffer (create-vertex-buffer (:device chrome-system) next-capacity)]
        (gpu-budget/replace-buffer!
         (:gpu-tracker chrome-system) old-buffer new-buffer "chrome/vertices"
         (* next-capacity chrome-material/vertex-stride)
         :active-bytes (* required chrome-material/vertex-stride)
         :reason :chrome-capacity-growth)
        (.destroy old-buffer)
        (reset! (:!buffer chrome-system) new-buffer)
        (reset! (:!capacity chrome-system) next-capacity)))
    @(:!buffer chrome-system)))

(defn prepare-chrome-frame! [chrome-system chromes]
  (let [chromes (or chromes [])]
    (if (frame-inputs/input-value-same? chromes
                                        @(:!last-chromes chrome-system))
      {:mesh-set-changed? false :writes 0
       :vertices (reduce + (map :vertex-count @(:!prepared chrome-system)))}
      (let [mesh-set-key (mapv (fn [op]
                                 [(:chrome/material op) (:container-idx op)
                                  (:owner-vi op)]) chromes)]
        (reset! (:!last-chromes chrome-system) chromes)
        (if (= mesh-set-key @(:!last-mesh-set-key chrome-system))
          {:mesh-set-changed? false :writes 0
           :vertices (reduce + (map :vertex-count @(:!prepared chrome-system)))}
          (let [prepared (prepare-ops chromes)
                packed (pack-vertices prepared)
                vertices (quot (.-length packed) chrome-material/vertex-words)
                buffer (ensure-capacity! chrome-system vertices)
                ^js device (:device chrome-system)]
            (when (pos? vertices)
              (.writeBuffer (.-queue device) buffer 0 packed))
            (gpu-budget/set-active-bytes!
             (:gpu-tracker chrome-system) buffer
             (* vertices chrome-material/vertex-stride))
            (reset! (:!prepared chrome-system) prepared)
            (reset! (:!last-mesh-set-key chrome-system) mesh-set-key)
            (frame-inputs/bump-shape-rev! chrome-system)
            (swap! (:!receipt chrome-system)
                   #(-> % (update :uploads inc)
                        (assoc :vertices vertices
                               :last-write-bytes
                               (* vertices chrome-material/vertex-stride))))
            {:mesh-set-changed? true :writes (if (pos? vertices) 1 0)
             :vertices vertices}))))))

(defn- frame-order [source-order entry-id]
  {:stratum :overlay
   :pass-class :direct
   ;; -1 is above every :world entry by stratum, but below the product chrome
   ;; ladder whose first overlay token is [:frame/root 0 0].
   :stack-path (into [[:frame/root -1 -1]]
                     (or (:stack-path source-order) []))
   :part-rank 0
   :stable-tie entry-id})

(defn chrome-entries [{:keys [store-frame chrome-system]}]
  (if-not (and store-frame chrome-system)
    []
    (loop [vis (:ordered-vis store-frame) op-offset 0 entries []]
      (if-let [vi (first vis)]
        (let [op-count (get-in store-frame [:ops-count-by-vi vi :chromes] 0)
              next-op-offset (+ op-offset op-count)
              rows (subvec @(:!prepared chrome-system) op-offset next-op-offset)
              vertex-count (reduce + (map :vertex-count rows))
              first-vertex (or (:first-vertex (first rows)) 0)
              entry-id [:frame/store vi :chromes]
              source-order (get-in store-frame [:order-by-vi vi])
              entries (cond-> entries
                        (pos? vertex-count)
                        (conj {:entry/id entry-id
                               :material/id entry-id
                               :material/revision 0
                               :instance/id entry-id
                               :family/id :render.family/chrome
                               :order (frame-order source-order entry-id)
                               :paint {:paint/source chrome-system
                                       :paint/source-type :chrome-system
                                       :vertex-count vertex-count
                                       :first-vertex first-vertex}
                               :visibility {:visible? true :clip :none}}))]
          (recur (next vis) next-op-offset entries))
        entries))))

(defn execute-chrome-batch! [^js pass entry]
  (let [paint (:paint entry)
        chrome-system (:paint/source paint)
        {:keys [vertex-count first-vertex]} paint
        ;; Explicit paint wins (same law as resolve-gpu-paint): the linear
        ;; variant's linearize-entry overrides pipeline/bind-group for the
        ;; rgba16float pass; only the buffer resolves through the source.
        pipeline (or (:pipeline paint) (:pipeline chrome-system))
        bind-group (or (:bind-group paint) (:bind-group chrome-system))
        buffer @(:!buffer chrome-system)]
    (.setPipeline pass pipeline)
    (.setBindGroup pass 0 bind-group)
    (.setVertexBuffer pass 0 buffer)
    (.draw pass vertex-count 1 first-vertex 0)
    (:entry/id entry)))

(defn chrome-receipt [chrome-system]
  @(:!receipt chrome-system))

(defn destroy-chrome-system! [chrome-system]
  (when-let [buffer @(:!buffer chrome-system)]
    (gpu-budget/destroy-resource! (:gpu-tracker chrome-system) buffer
                                  :reason :chrome-system-destroy)
    (.destroy buffer))
  (reset! (:!prepared chrome-system) [])
  (reset! (:!last-chromes chrome-system) ::destroyed)
  (reset! (:!last-mesh-set-key chrome-system) ::destroyed)
  nil)
