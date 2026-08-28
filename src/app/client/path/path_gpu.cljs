(ns app.client.path.path-gpu
  "WebGPU projection for the path family. The system owns one repacked vertex
   lane and a content/version/regime mesh cache. Uploads occur only when the
   mesh-set identity or zoom regime changes; camera/container motion remains a
   shader value."
  (:require [clojure.string :as str]
            [app.client.path.material :as path-material]
            [app.client.path.tessellation :as tessellation]
            [app.client.engine.color :as scene-color]
            [app.client.engine.budget :as gpu-budget]))

(def path-color-mode-declaration
  "const kPathLinearPremultiplied: bool = false;")

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

(def path-fragment-shader
  (str path-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
       "}\n"
       "@fragment fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> {\n"
       "  if (!kPathLinearPremultiplied) { return color; }\n"
       "  let alpha = clamp(color.a, 0.0, 1.0);\n"
       "  let linear = vec3<f32>(srgb_channel_to_linear(color.r),\n"
       "                         srgb_channel_to_linear(color.g),\n"
       "                         srgb_channel_to_linear(color.b));\n"
       "  return vec4<f32>(linear * alpha, alpha);\n"
       "}\n"))

(defn- configure-path-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader path-color-mode-declaration
                 "const kPathLinearPremultiplied: bool = true;")
    shader))

(defn- scene-color-blend [color]
  (let [{[color-src color-dst] :color
         [alpha-src alpha-dst] :alpha} (:blend color)]
    {:color {:srcFactor (name color-src) :dstFactor (name color-dst)}
     :alpha {:srcFactor (name alpha-src) :dstFactor (name alpha-dst)}}))

(defn- create-vertex-buffer [^js device capacity]
  (.createBuffer device
                 (clj->js {:size (* (max 1 capacity)
                                    path-material/vertex-stride)
                           :usage (bit-or js/GPUBufferUsage.VERTEX
                                          js/GPUBufferUsage.COPY_DST)})))

(defn init-path-system
  [^js device fformat camera-buffer containers-buffer
   & {:keys [initial-capacity tracker scene-color]
      :or {initial-capacity 2048
           scene-color scene-color/legacy-direct-color}}]
  (assert camera-buffer "init-path-system requires :camera-buffer")
  (assert containers-buffer "init-path-system requires :containers-buffer")
  (let [vertex-module (.createShaderModule
                       device (clj->js {:code path-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js {:code (configure-path-color-shader
                                         path-fragment-shader scene-color)}))
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
                     :buffers [{:arrayStride path-material/vertex-stride
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
                                :blend (scene-color-blend scene-color)}]}
                    :primitive {:topology "triangle-list"
                                :frontFace "ccw"
                                :cullMode "none"}}))
        bind-group (.createBindGroup
                    device
                    (clj->js {:layout bind-layout
                              :entries [{:binding 0
                                         :resource {:buffer camera-buffer}}
                                        {:binding 1
                                         :resource {:buffer containers-buffer}}]}))
        buffer (create-vertex-buffer device initial-capacity)]
    (gpu-budget/register-buffer! tracker buffer "path/vertices"
                                 (* initial-capacity path-material/vertex-stride)
                                 :active-bytes 0)
    {:device device :pipeline pipeline :bind-group bind-group
     :camera-buffer camera-buffer :containers-buffer containers-buffer
     :scene-color scene-color :gpu-tracker tracker
     :frame-input/identity (js-obj) :!shape-rev (atom 0)
     :!buffer (atom buffer) :!capacity (atom initial-capacity)
     :!mesh-cache (atom {}) :!prepared (atom [])
     :!last-paths (atom ::never) :!last-regime (atom nil)
     :!last-mesh-set-key (atom ::never)
     :!receipt (atom {:path-system/version 1 :uploads 0
                      :mesh-derivations 0 :vertices 0})}))

(defn- inside-edge? [edge value boundary]
  (case edge
    :left (>= value boundary)
    :top (>= value boundary)
    :right (<= value boundary)
    :bottom (<= value boundary)))

(defn- edge-intersection [edge boundary [ax ay] [bx by]]
  (case edge
    :left
    (let [t (if (= ax bx) 0.0 (/ (- boundary ax) (- bx ax)))]
      [boundary (+ ay (* t (- by ay)))])
    :right
    (let [t (if (= ax bx) 0.0 (/ (- boundary ax) (- bx ax)))]
      [boundary (+ ay (* t (- by ay)))])
    :top
    (let [t (if (= ay by) 0.0 (/ (- boundary ay) (- by ay)))]
      [(+ ax (* t (- bx ax))) boundary])
    :bottom
    (let [t (if (= ay by) 0.0 (/ (- boundary ay) (- by ay)))]
      [(+ ax (* t (- bx ax))) boundary])))

(defn- clip-polygon-edge [polygon edge boundary]
  (if (empty? polygon)
    []
    (loop [prior (peek polygon)
           points polygon
           result []]
      (if-let [current (first points)]
        (let [prior-value (if (#{:left :right} edge) (first prior) (second prior))
              current-value (if (#{:left :right} edge) (first current) (second current))
              prior-inside? (inside-edge? edge prior-value boundary)
              current-inside? (inside-edge? edge current-value boundary)
              result (cond
                       (and prior-inside? current-inside?)
                       (conj result current)

                       (and prior-inside? (not current-inside?))
                       (conj result (edge-intersection edge boundary prior current))

                       (and (not prior-inside?) current-inside?)
                       (conj result
                             (edge-intersection edge boundary prior current)
                             current)

                       :else result)]
          (recur current (next points) result))
        result))))

(defn- clip-triangle [triangle clip]
  (if-not clip
    [triangle]
    (let [{:keys [x y w h]} clip
          polygon (-> triangle
                      (clip-polygon-edge :left x)
                      (clip-polygon-edge :right (+ x w))
                      (clip-polygon-edge :top y)
                      (clip-polygon-edge :bottom (+ y h)))]
      (if (< (count polygon) 3)
        []
        (mapv (fn [index]
                [(first polygon) (nth polygon index) (nth polygon (inc index))])
              (range 1 (dec (count polygon))))))))

(defn- prepared-op [op mesh first-vertex]
  (let [origin [(:x op) (:y op)]
        absolute-triangles
        (mapv (fn [triangle]
                (mapv (fn [[x y]]
                        [(+ x (first origin)) (+ y (second origin))])
                      triangle))
              (:triangles mesh))
        triangles (into []
                        (mapcat #(clip-triangle % (:path/clip op)))
                        absolute-triangles)
        vertices (into [] cat triangles)]
    {:op op :mesh mesh :vertices vertices
     :first-vertex first-vertex :vertex-count (count vertices)}))

(defn- pack-vertices [prepared]
  (let [vertex-count (reduce + (map :vertex-count prepared))
        floats (js/Float32Array. (* vertex-count path-material/vertex-words))
        uints (js/Uint32Array. (.-buffer floats))]
    (loop [ops prepared vertex-offset 0]
      (if-let [{:keys [op vertices]} (first ops)]
        (let [material (:path/material op)
              [r g b a] (path-material/paint-color material)
              container-idx (or (:container-idx op) 0)]
          (doseq [[index [x y]] (map-indexed vector vertices)]
            (let [base (* (+ vertex-offset index) path-material/vertex-words)]
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
        (gpu-budget/replace-buffer!
         (:gpu-tracker path-system) old-buffer new-buffer "path/vertices"
         (* next-capacity path-material/vertex-stride)
         :active-bytes (* required path-material/vertex-stride)
         :reason :path-capacity-growth)
        (.destroy old-buffer)
        (reset! (:!buffer path-system) new-buffer)
        (reset! (:!capacity path-system) next-capacity)))
    @(:!buffer path-system)))

(defn prepare-path-frame!
  "Derive/cache/repack only when the path op vector identity or zoom regime
   changes. Pan/continuous zoom inside a regime never reaches this write."
  [path-system paths zoom]
  (let [paths (or paths [])
        regime (:regime/id (path-material/zoom-regime zoom))]
    (if (and (= paths @(:!last-paths path-system))
             (= regime @(:!last-regime path-system)))
      {:mesh-set-changed? false :writes 0
       :vertices (reduce + (map :vertex-count @(:!prepared path-system)))}
      (let [derivation (tessellation/derive-mesh-set
                        @(:!mesh-cache path-system)
                        (mapv :path/material paths) zoom)
            mesh-set-key
            (mapv (fn [op mesh]
                    [(:cache-key mesh)
                     (:x op) (:y op) (:path/clip op) (:container-idx op)])
                  paths (:meshes derivation))
            prepared
            (loop [ops paths meshes (:meshes derivation)
                   first-vertex 0 result []]
              (if-let [op (first ops)]
                (let [row (prepared-op op (first meshes) first-vertex)]
                  (recur (next ops) (next meshes)
                         (+ first-vertex (:vertex-count row))
                         (conj result row)))
                result))]
        (reset! (:!mesh-cache path-system) (:cache derivation))
        (reset! (:!last-paths path-system) paths)
        (reset! (:!last-regime path-system) regime)
        (if (= mesh-set-key @(:!last-mesh-set-key path-system))
          {:mesh-set-changed? false :writes 0
           :vertices (reduce + (map :vertex-count @(:!prepared path-system)))
           :derived (count (:derived-keys derivation))}
          (let [packed (pack-vertices prepared)
                vertices (quot (.-length packed) path-material/vertex-words)
                buffer (ensure-capacity! path-system vertices)
                ^js device (:device path-system)]
            (when (pos? vertices)
              (.writeBuffer (.-queue device) buffer 0 packed))
            (gpu-budget/set-active-bytes!
             (:gpu-tracker path-system) buffer
             (* vertices path-material/vertex-stride))
            (reset! (:!prepared path-system) prepared)
            (reset! (:!last-mesh-set-key path-system) mesh-set-key)
            (swap! (:!shape-rev path-system) inc)
            (swap! (:!receipt path-system)
                   (fn [receipt]
                     (-> receipt
                         (update :uploads inc)
                         (update :mesh-derivations +
                                 (count (:derived-keys derivation)))
                         (assoc :vertices vertices :regime regime
                                :last-write-bytes
                                (* vertices path-material/vertex-stride)))))
            {:mesh-set-changed? true :writes (if (pos? vertices) 1 0)
             :vertices vertices
             :derived (count (:derived-keys derivation))}))))))

(defn draw-path-range!
  "Paint one contiguous run of the prepared path vertices on an open pass."
  [^js pass path-system first-vertex vertex-count]
  (.setPipeline pass (:pipeline path-system))
  (.setBindGroup pass 0 (:bind-group path-system))
  (.setVertexBuffer pass 0 @(:!buffer path-system))
  (.draw pass vertex-count 1 first-vertex 0))

(defn path-receipt [path-system]
  @(:!receipt path-system))

(defn destroy-path-system! [path-system]
  (when-let [buffer @(:!buffer path-system)]
    (gpu-budget/destroy-resource! (:gpu-tracker path-system) buffer
                                  :reason :path-system-destroy)
    (.destroy buffer))
  (reset! (:!prepared path-system) [])
  (reset! (:!mesh-cache path-system) {})
  (reset! (:!last-paths path-system) ::destroyed)
  (reset! (:!last-mesh-set-key path-system) ::destroyed)
  nil)
