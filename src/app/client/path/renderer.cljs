(ns app.client.path.renderer
  "Draw named path placements through the shared coverage program.
   Takes an initial view, explicit push diffs, camera/view changes and GPU
   resources. Gives draws and counted work. Owns current placements, atlas
   and instance storage; placements.cljc owns pure preparation and push.cljs
   applies its affected sets. Evidence: frame_test.clj, harness/path_push.cljs
   and the repo verifier's path/plane pixels. Folder map: README.md."
  (:require [app.client.engine.buffer-pool :as buffer-pool]
            [app.client.engine.color :as scene-color]
            [app.client.engine.coverage :as coverage]
            [app.client.engine.device :as device]
            [app.client.path.placements :as placements]
            [app.client.path.push :as push]))

;; Vertex main: six vertices per instance span the cover rectangle in local
;; units, placed through the compact group affine and the camera. It also
;; hands the fragment the inverse of that placement, flat, so the fragment
;; recovers its local position from its own pixel centre rather than from
;; an interpolated corner that the rasterizer snapped (the bench's session
;; 11 finding, ~0.01 local at zoom 3).
(def region-vertex-shader
  (str "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct GroupTransform {
    axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
    flags: u32, padding: u32,
  };
  @group(0) @binding(3) var<storage, read> groups: array<GroupTransform>;
  " coverage/instance-input-wgsl "
  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) @interpolate(flat) inv_a: vec4<f32>,
    @location(1) @interpolate(flat) inv_t: vec2<f32>,
    @location(2) @interpolate(flat) color: vec4<f32>,
    @location(3) @interpolate(flat) band: vec4<u32>,
    @location(4) @interpolate(flat) band_xf: vec4<f32>,
    @location(5) @interpolate(flat) tags: vec4<u32>,
    @location(6) @interpolate(flat) clip_band: vec4<u32>,
    @location(7) @interpolate(flat) clip_xf: vec4<f32>,
  };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, inst: RegionInstance) -> VertexOutput {
    var pos = vec2<f32>(0.0, 0.0);
    switch(v_index) {
      case 0u: { pos = vec2<f32>(0.0, 0.0); }
      case 1u: { pos = vec2<f32>(1.0, 0.0); }
      case 2u: { pos = vec2<f32>(0.0, 1.0); }
      case 3u: { pos = vec2<f32>(1.0, 0.0); }
      case 4u: { pos = vec2<f32>(1.0, 1.0); }
      default: { pos = vec2<f32>(0.0, 1.0); }
    }
    let local = inst.rect.xy + pos * inst.rect.zw;
    let c = groups[inst.tags.y];
    let is_screen = (c.flags & 1u) != 0u;
    let zm = select(camera.zoom, 1.0, is_screen);
    let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
    let a = c.axis_x * zm;
    let b = c.axis_y * zm;
    let t = c.translation * zm + pn;
    let fb = t + a * local.x + b * local.y;
    let ndc = (fb / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
    let det = a.x * b.y - b.x * a.y;
    var out: VertexOutput;
    out.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
    out.inv_a = vec4<f32>(b.y, -b.x, -a.y, a.x) / det;
    out.inv_t = t;
    out.color = inst.color;
    out.band = inst.band;
    out.band_xf = inst.band_xf;
    out.tags = inst.tags;
    out.clip_band = inst.clip_band;
    out.clip_xf = inst.clip_xf;
    return out;
  }"))

;; Fragment main: the pixel centre through the inverse placement gives the
;; local position; its screen derivative gives device pixels per local
;; unit; the shared program gives the coverage, times the clip's.
(def region-fragment-shader
  (str device/scene-color-wgsl coverage/coverage-wgsl "
  struct FragmentInput {
    @builtin(position) position: vec4<f32>,
    @location(0) @interpolate(flat) inv_a: vec4<f32>,
    @location(1) @interpolate(flat) inv_t: vec2<f32>,
    @location(2) @interpolate(flat) color: vec4<f32>,
    @location(3) @interpolate(flat) band: vec4<u32>,
    @location(4) @interpolate(flat) band_xf: vec4<f32>,
    @location(5) @interpolate(flat) tags: vec4<u32>,
    @location(6) @interpolate(flat) clip_band: vec4<u32>,
    @location(7) @interpolate(flat) clip_xf: vec4<f32>,
  };

  @fragment
  fn main(in: FragmentInput) -> @location(0) vec4<f32> {
    let d = in.position.xy - in.inv_t;
    let p = vec2<f32>(in.inv_a.x * d.x + in.inv_a.y * d.y, in.inv_a.z * d.x + in.inv_a.w * d.y);
    let ppu = 1.0 / max(fwidth(p), vec2<f32>(kMinDerivative, kMinDerivative));
    let alpha = region_alpha(p, ppu, in.band, in.band_xf, in.tags.x, in.clip_band, in.clip_xf, "
       coverage/log2-atlas-width "u);
    return scene_color(in.color, alpha);
  }"))

(defn- create-bind-group
  [^js device layout atlas camera-buffer groups-buffer]
  (let [{:keys [curve-view band-view]} (coverage/views atlas)]
    (.createBindGroup device
                      (clj->js {:layout layout
                                :entries [{:binding 0 :resource curve-view}
                                          {:binding 1 :resource band-view}
                                          {:binding 2 :resource {:buffer camera-buffer}}
                                          {:binding 3 :resource {:buffer groups-buffer}}]}))))

(defn init-path-system
  "Device, format, camera/groups buffers, initial view and options → system;
   allocates the pipeline, the atlas and the instance pool.

   Options: :scene-color (the engine's colour contract), :initial-capacity
   (instance rows), :label."
  [^js device fformat camera-buffer groups-buffer initial-view
   & {:keys [initial-capacity scene-color label]
      :or {initial-capacity 256 scene-color scene-color/legacy-direct-color label "path"}}]
  (assert camera-buffer "init-path-system requires :camera-buffer")
  (assert groups-buffer "init-path-system requires :groups-buffer")
  (let [vertex-module (.createShaderModule device (clj->js {:code region-vertex-shader}))
        fragment-module (.createShaderModule
                         device (clj->js {:code (device/configure-scene-color-shader region-fragment-shader scene-color)}))
        bind-layout (.createBindGroupLayout
                     device
                     (clj->js {:entries (into (coverage/bind-group-layout-entries js/GPUShaderStage.FRAGMENT)
                                              [{:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                               {:binding 3 :visibility js/GPUShaderStage.VERTEX :buffer {:type "read-only-storage"}}])}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bind-layout]}))
        pipeline (.createRenderPipeline
                  device
                  (clj->js {:layout pipeline-layout
                            :vertex {:module vertex-module :entryPoint "main"
                                     :buffers [{:arrayStride coverage/instance-stride
                                                :stepMode "instance"
                                                :attributes coverage/instance-attributes}]}
                            :fragment {:module fragment-module :entryPoint "main"
                                       :targets [{:format fformat :blend (device/scene-color-blend scene-color)}]}
                            :primitive {:topology "triangle-list"}}))
        atlas (coverage/create-atlas device :label label)
        pool (buffer-pool/create-pool device initial-capacity
                                      :floats-per-item coverage/instance-words
                                      :pack-fn coverage/pack-instance)
        bind-group (atom (create-bind-group device bind-layout atlas camera-buffer groups-buffer))]
    {:device device :pipeline pipeline :bind-layout bind-layout
     :camera-buffer camera-buffer :groups-buffer groups-buffer
     :scene-color scene-color :label label
     :atlas atlas :pool pool
     :!bind-group bind-group
     :refresh-bind-group! #(reset! bind-group (create-bind-group device bind-layout atlas camera-buffer groups-buffer))
     :!placements (atom (placements/empty-state initial-view))}))

(defn push!
  "System and {:upsert {placement-id item} :remove #{id} :order [id …]
   :groups world-transforms} → affected sets and physical write counts.
   Omit order/groups when unchanged. No source recipe executes here."
  [system diff] (push/push! system diff))

(defn frame!
  "System and view → work for declared view dependents only. Camera-only
   draws need no placement list. Pan visits snapped world placements only."
  [system view] (push/frame! system view))

(defn draw-path-instances!
  "Open pass, system, first instance, count → one instanced draw of six
   vertices per row. Caller owns ordering, scissors and pass lifetime."
  [^js pass system first-instance instance-count]
  (when (pos? instance-count)
    (.setPipeline pass (:pipeline system))
    (.setBindGroup pass 0 @(:!bind-group system))
    (.setVertexBuffer pass 0 (:buffer @(:pool system)))
    (.draw pass 6 instance-count 0 first-instance)))

(defn draw-path-frame!
  "Open pass and system → draws every prepared row in painter's order."
  [^js pass system]
  (draw-path-instances! pass system 0 (:row-count @(:!placements system))))

(defn prepared-rows
  "System → the instance rows of the last prepared frame."
  [system]
  (let [state @(:!placements system)]
    (vec (mapcat #(get-in state [:entries % :rows]) (:order state)))))

(defn item-range
  "System and placement id → [first-instance count], for interleaved passes."
  [system id]
  (get-in @(:!placements system) [:ranges id] [0 0]))

(defn destroy-path-system!
  "System → nil; destroys the atlas and the instance buffer, clears the
   caches. A destroyed system is not reusable."
  [system]
  (coverage/destroy-atlas! (:atlas system))
  (when-let [buffer (:buffer @(:pool system))]
    (.destroy ^js buffer))
  (reset! (:!placements system) (placements/empty-state nil))
  nil)
