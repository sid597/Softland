(ns app.client.path.renderer
  "Own the mesh cache and packed vertex lane.

   Input: shared GPU resources, ordered path items, zoom, world transforms
   and an open pass. Output: uploaded vertices, per-item vertex ranges, and
   draw commands. State is per system: buffer/capacity, mesh cache, prepared
   rows and last frame key. It does not own the camera or group buffers.

   Folder map: README.md."
  (:require [app.client.engine.color :as scene-color]
            [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.path.frame :as frame]
            [app.client.path.component :as path-component]
            [app.client.path.tessellation :as tessellation]))

(def vertex-words 7)
(def vertex-stride 28)

;; Vertex main(input) applies compact group affine, world/screen camera
;; behavior and NDC projection. Fragment main(color) uses shared
;; transfer/alpha handling. A vertex row carries local position, RGBA and
;; unsigned group index.
(def path-vertex-shader
  "struct Camera {
     pan: vec2<f32>, zoom: f32, padding: f32,
     screen_dimensions: vec2<f32>,
   };
   @group(0) @binding(0) var<uniform> camera: Camera;
   struct GroupTransform {
     axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
     flags: u32, padding: u32,
   };
   @group(0) @binding(1) var<storage, read> groups: array<GroupTransform>;
   struct VertexInput {
     @location(0) position: vec2<f32>,
     @location(1) color: vec4<f32>,
     @location(2) group_buffer_index: u32,
   };
   struct VertexOutput {
     @builtin(position) position: vec4<f32>,
     @location(0) color: vec4<f32>,
   };
   @vertex fn main(input: VertexInput) -> VertexOutput {
     let c = groups[input.group_buffer_index];
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

(defn- create-vertex-buffer
  "Device and capacity → vertex/copy-destination buffer.

   Allocates at least one vertex."
  [^js device capacity]
  (.createBuffer device
                 (clj->js {:size (* (max 1 capacity)
                                    vertex-stride)
                           :usage (bit-or js/GPUBufferUsage.VERTEX
                                          js/GPUBufferUsage.COPY_DST)})))

(defn init-path-system
  "Device, format, camera/groups buffers, options → system; allocates
   pipeline/buffer.

   One pipeline and binding set plus cache atoms. Rendering inputs and owned
   state are explicit."
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

(defn- pack-vertices
  "Prepared rows and world transforms → packed typed array.

   Replicates paint/index per local vertex; overlays uint view for indexes.
   Intended for a single simple vertex lane; repacks all prepared rows."
  [prepared world-transforms]
  (let [vertex-count (reduce + (map :vertex-count prepared))
        floats (js/Float32Array. (* vertex-count vertex-words))
        uints (js/Uint32Array. (.-buffer floats))]
    (loop [draw-items prepared vertex-offset 0]
      (if-let [{:keys [draw-item mesh]} (first draw-items)]
        (let [component (:path/material draw-item)
              vertices (:vertices mesh)
              [r g b a] (path-component/paint-color component)
              group-buffer-index (transform/buffer-index world-transforms (:container draw-item))]
          (doseq [[index [x y]] (map-indexed vector vertices)]
            (let [base (* (+ vertex-offset index) vertex-words)]
              (aset floats (+ base 0) x)
              (aset floats (+ base 1) y)
              (aset floats (+ base 2) r)
              (aset floats (+ base 3) g)
              (aset floats (+ base 4) b)
              (aset floats (+ base 5) a)
              (aset uints (+ base 6) group-buffer-index)))
          (recur (next draw-items) (+ vertex-offset (count vertices))))
        floats))))

(defn- ensure-capacity!
  "System and required vertices → current/enlarged buffer; may destroy old
   buffer.

   Doubling from at least one. Content need not be copied because
   preparation rewrites it."
  [path-system required]
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
  "System, items, zoom, transforms → change/write/vertex/derive statistics;
   updates caches and buffer.

   Frame-key early return, content-key mesh reuse, whole-frame packing and
   one upload. Transform coefficients are intentionally excluded, but a
   reassigned compact index with unchanged container ID is also excluded;
   callers must preserve that index or invalidate the frame."
  [path-system draw-items zoom world-transforms]
  (let [draw-items (or draw-items [])
        lod (:lod/id (tessellation/zoom-lod zoom))
        key (frame/frame-key draw-items lod)]
    (if (= key @(:!last-frame-key path-system))
      {:changed? false
       :writes 0
       :vertices (reduce + (map :vertex-count @(:!prepared path-system)))
       :derived 0}
      (let [derivation (tessellation/derive-mesh-set
                        @(:!mesh-cache path-system)
                        (mapv :path/material draw-items) zoom)
            prepared
            (loop [draw-items draw-items
                   meshes (:meshes derivation)
                   first-vertex 0
                   result []]
              (if-let [draw-item (first draw-items)]
                (let [mesh (first meshes)
                      row {:draw-item draw-item
                           :mesh mesh
                           :first-vertex first-vertex
                           :vertex-count (:vertex-count mesh)}]
                  (recur (next draw-items)
                         (next meshes)
                         (+ first-vertex (:vertex-count row))
                         (conj result row)))
                result))
            packed (pack-vertices prepared world-transforms)
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
  "Open pass, system, first vertex, count → encoded draw.

   Binds lane and emits one contiguous draw. Caller owns ordering, clipping
   and pass lifetime."
  [^js pass path-system first-vertex vertex-count]
  (.setPipeline pass (:pipeline path-system))
  (.setBindGroup pass 0 (:bind-group path-system))
  (.setVertexBuffer pass 0 @(:!buffer path-system))
  (.draw pass vertex-count 1 first-vertex 0))

(defn destroy-path-system!
  "System → nil; destroys buffer and clears caches/key.

   Explicit teardown. Destroyed system is not an initialized reusable
   system."
  [path-system]
  (when-let [buffer @(:!buffer path-system)]
    (.destroy buffer))
  (reset! (:!prepared path-system) [])
  (reset! (:!mesh-cache path-system) {})
  (reset! (:!last-frame-key path-system) ::destroyed)
  nil)
