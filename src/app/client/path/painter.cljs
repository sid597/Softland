(ns app.client.path.painter
  "The path painter: one repacked vertex lane and a mesh cache keyed by
   geometry, algorithm, and zoom regime.
   Takes: a device, a format, and the shared buffers to build the system; the
   frame's path ops and zoom to prepare; a render pass and a vertex range to
   draw.
   Gives: a path system; a written vertex buffer; draw calls.
   Holds: per-system atoms for the buffer, capacity, mesh cache, prepared
   state, and the last revision/container/regime frame key."
  (:require [app.client.engine.color :as scene-color]
            [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.path.frame :as frame]
            [app.client.path.material :as path-material]
            [app.client.path.tessellation :as tessellation]))

(def vertex-words 7)
(def vertex-stride 28)

(def path-vertex-shader
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
     @location(0) position: vec2<f32>,
     @location(1) color: vec4<f32>,
     @location(2) container_idx: u32,
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
     let world = c.translation + c.axis_x * input.position.x +
                 c.axis_y * input.position.y;
     let panned = world * zm + pn;
     let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
     var output: VertexOutput;
     output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
     output.color = input.color;
     return output;
   }")

(def path-fragment-main
  "@fragment fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> {
     return scene_color(color, 1.0);
   }")

(defn- create-vertex-buffer [^js device capacity]
  (.createBuffer device
                 (clj->js {:size (* (max 1 capacity)
                                    vertex-stride)
                           :usage (bit-or js/GPUBufferUsage.VERTEX
                                          js/GPUBufferUsage.COPY_DST)})))

(defn init-path-system
  [^js device fformat camera-buffer groups-buffer
   & {:keys [initial-capacity scene-color]
      :or {initial-capacity 2048
           scene-color scene-color/legacy-direct-color}}]
  (assert camera-buffer "init-path-system requires :camera-buffer")
  (assert groups-buffer "init-path-system requires :groups-buffer")
  (let [vertex-module (.createShaderModule
                       device (clj->js {:code path-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js
                          {:code
                           (device/configure-scene-color-shader
                            (str device/scene-color-wgsl path-fragment-main)
                            scene-color)}))
        bind-layout (.createBindGroupLayout
                     device
                     (clj->js
                      {:entries [{:binding 0
                                  :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "uniform"}}
                                 {:binding 1
                                  :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "read-only-storage"}}]}))
        pipeline-layout (.createPipelineLayout
                         device (clj->js {:bindGroupLayouts [bind-layout]}))
        pipeline (.createRenderPipeline
                  device
                  (clj->js
                   {:layout pipeline-layout
                    :vertex
                    {:module vertex-module :entryPoint "main"
                     :buffers [{:arrayStride vertex-stride
                                :stepMode "vertex"
                                :attributes
                                [{:shaderLocation 0 :offset 0
                                  :format "float32x2"}
                                 {:shaderLocation 1 :offset 8
                                  :format "float32x4"}
                                 {:shaderLocation 2 :offset 24
                                  :format "uint32"}]}]}
                    :fragment
                    {:module fragment-module :entryPoint "main"
                     :targets [{:format fformat
                                :blend (device/scene-color-blend scene-color)}]}
                    :primitive {:topology "triangle-list"
                                :frontFace "ccw"
                                :cullMode "none"}}))
        bind-group (.createBindGroup
                    device
                    (clj->js {:layout bind-layout
                              :entries [{:binding 0
                                         :resource {:buffer camera-buffer}}
                                        {:binding 1
                                         :resource {:buffer groups-buffer}}]}))
        buffer (create-vertex-buffer device initial-capacity)]
    {:device device :pipeline pipeline :bind-group bind-group
     :camera-buffer camera-buffer :groups-buffer groups-buffer
     :scene-color scene-color
     :!buffer (atom buffer) :!capacity (atom initial-capacity)
     :!mesh-cache (atom {}) :!prepared (atom [])
     :!last-frame-key (atom ::never)}))

(defn- pack-vertices [prepared effective]
  (let [vertex-count (reduce + (map :vertex-count prepared))
        floats (js/Float32Array. (* vertex-count vertex-words))
        uints (js/Uint32Array. (.-buffer floats))]
    (loop [ops prepared vertex-offset 0]
      (if-let [{:keys [op mesh]} (first ops)]
        (let [material (:path/material op)
              vertices (:vertices mesh)
              [r g b a] (path-material/paint-color material)
              container-idx (transform/buffer-index effective (:container op))]
          (doseq [[index [x y]] (map-indexed vector vertices)]
            (let [base (* (+ vertex-offset index) vertex-words)]
              (aset floats (+ base 0) x)
              (aset floats (+ base 1) y)
              (aset floats (+ base 2) r)
              (aset floats (+ base 3) g)
              (aset floats (+ base 4) b)
              (aset floats (+ base 5) a)
              (aset uints (+ base 6) container-idx)))
          (recur (next ops) (+ vertex-offset (count vertices))))
        floats))))

(defn- ensure-capacity! [path-system required]
  (let [capacity @(:!capacity path-system)]
    (when (> required capacity)
      (let [next-capacity (loop [candidate (max 1 capacity)]
                            (if (>= candidate required)
                              candidate
                              (recur (* 2 candidate))))
            old-buffer @(:!buffer path-system)
            new-buffer (create-vertex-buffer (:device path-system)
                                             next-capacity)]
        (.destroy old-buffer)
        (reset! (:!buffer path-system) new-buffer)
        (reset! (:!capacity path-system) next-capacity)))
    @(:!buffer path-system)))

(defn prepare-path-frame!
  "Derive/cache/repack when a material revision, container, or zoom regime
   changes. Pan and continuous zoom within one regime never reach this write."
  [path-system paths zoom effective]
  (let [paths (or paths [])
        regime (:regime/id (tessellation/zoom-regime zoom))
        key (frame/frame-key paths regime)]
    (if (= key @(:!last-frame-key path-system))
      {:changed? false
       :writes 0
       :vertices (reduce + (map :vertex-count @(:!prepared path-system)))
       :derived 0}
      (let [derivation (tessellation/derive-mesh-set
                        @(:!mesh-cache path-system)
                        (mapv :path/material paths) zoom)
            prepared
            (loop [ops paths
                   meshes (:meshes derivation)
                   first-vertex 0
                   result []]
              (if-let [op (first ops)]
                (let [mesh (first meshes)
                      row {:op op
                           :mesh mesh
                           :first-vertex first-vertex
                           :vertex-count (:vertex-count mesh)}]
                  (recur (next ops)
                         (next meshes)
                         (+ first-vertex (:vertex-count row))
                         (conj result row)))
                result))
            packed (pack-vertices prepared effective)
            vertices (quot (.-length packed) vertex-words)
            buffer (ensure-capacity! path-system vertices)
            ^js device (:device path-system)]
        (when (pos? vertices)
          (.writeBuffer (.-queue device) buffer 0 packed))
        (reset! (:!mesh-cache path-system) (:cache derivation))
        (reset! (:!prepared path-system) prepared)
        (reset! (:!last-frame-key path-system) key)
        {:changed? true
         :writes (if (pos? vertices) 1 0)
         :vertices vertices
         :derived (count (:derived-keys derivation))}))))

(defn draw-path-range!
  "Paint one contiguous run of the prepared path vertices on an open pass."
  [^js pass path-system first-vertex vertex-count]
  (.setPipeline pass (:pipeline path-system))
  (.setBindGroup pass 0 (:bind-group path-system))
  (.setVertexBuffer pass 0 @(:!buffer path-system))
  (.draw pass vertex-count 1 first-vertex 0))

(defn destroy-path-system! [path-system]
  (when-let [buffer @(:!buffer path-system)]
    (.destroy buffer))
  (reset! (:!prepared path-system) [])
  (reset! (:!mesh-cache path-system) {})
  (reset! (:!last-frame-key path-system) ::destroyed)
  nil)
