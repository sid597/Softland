(ns app.client.path.renderer
  "Own what a path frame retains, and draw it.

   Input: shared GPU resources, ordered draw items ({:path/material record
   :container group-id}), the view ({:zoom :pan}), world transforms and an
   open pass. Output: prepared instance rows, uploads, one instanced
   coverage draw, and per-frame statistics naming what was rebuilt at each
   level. State per system: the run cache (a record's construction result
   with what it read), the pack cache per region and scale bucket, the
   curve/band atlas, the instance pool, the pipeline and bind group. It
   does not own the camera or group buffers.

   A frame runs a record's construction only when a value it read has
   changed; packs a region only when its content or the scale bucket
   changed; writes an instance row only when the row changed. A pan, or a
   zoom inside the bucket, touches nothing here: the camera buffer moves the
   picture. Painter's order is the item order, a fill before its stroke,
   dabs in drawing order, all in one instanced draw.

   Folder map: README.md."
  (:require [app.client.engine.buffer-pool :as buffer-pool]
            [app.client.engine.color :as scene-color]
            [app.client.engine.coverage :as coverage]
            [app.client.engine.device :as device]
            [app.client.engine.transform :as transform]
            [app.client.path.component :as component]
            [app.client.path.frame :as frame]
            [app.client.path.pack :as pack]))

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
  "Device, target format, camera and groups buffers, options → system;
   allocates the pipeline, the atlas and the instance pool.

   Options: :scene-color (the engine's colour contract), :initial-capacity
   (instance rows), :label."
  [^js device fformat camera-buffer groups-buffer
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
                                      :pack-fn coverage/pack-instance)]
    {:device device :pipeline pipeline :bind-layout bind-layout
     :camera-buffer camera-buffer :groups-buffer groups-buffer
     :scene-color scene-color :label label
     :atlas atlas :pool pool
     :!bind-group (atom (create-bind-group device bind-layout atlas camera-buffer groups-buffer))
     :!runs (atom {}) :!packs (atom {}) :!prepared (atom {:rows [] :items [] :item-ranges []})
     :!last-frame-key (atom ::never)}))

(def cell-cover-area-px (* 256 256))
(def cell-cover-px-per-curve 2048)

(defn cover-options
  "Pack, region rule, bucket and device scale → the cover the renderer
   asks for: the box, or cells when the box is large in device pixels and
   sparse in curves (a long thin stroke), with cells about a twelfth of the
   box's larger side."
  [pack rule bucket scale]
  (let [[x0 y0 x1 y1] (:bbox pack)
        w (- x1 x0) h (- y1 y0)
        area-px (* w h scale scale)
        margin (pack/bucket-margin bucket)]
    (if (and (> area-px cell-cover-area-px) (> (/ area-px (max 1 (:count pack))) cell-cover-px-per-curve))
      {:mode :cells :margin margin :cell (/ (max w h) 12.0) :rule rule}
      {:mode :box :margin margin})))

(defn- item-view
  "View, world transform → the view the item's construction sees: device
   pixels per local unit and the device offset of the group's origin."
  [view {:keys [affine flags]}]
  (let [[a b c d tx ty] (or affine transform/identity-affine)
        screen? (= 1 flags)
        zoom (if screen? 1.0 (or (:zoom view) 1.0))
        [px py] (if screen? [0.0 0.0] (or (:pan view) [0.0 0.0]))
        group-scale (max (Math/hypot a b) (Math/hypot c d))]
    {:scale (* zoom group-scale)
     :pan [(+ (* tx zoom) px) (+ (* ty zoom) py)]}))

(defn- run-for
  "Runs cache, record, item view → [runs result rebuilt?]."
  [runs record view]
  (let [id (:path/material-id record)
        cached (get runs id)]
    (if (and cached (not (component/rerun? record view (:reads cached))))
      [runs (:result cached) false]
      (let [result (component/run record view)]
        [(assoc runs id {:reads (:reads result) :result result}) result true]))))

(defn- pack-for
  "Packs cache, region, bucket, scale → [packs entry rebuilt?]; an entry is
   {:key :pack :cover} or nil for an empty region."
  [packs region bucket scale]
  (let [key (frame/pack-key region bucket)]
    (if-let [entry (get packs key)]
      [packs entry false]
      (let [{:keys [pack]} (pack/pack-region (:path region) (pack/bucket-tolerance bucket) {})
            entry (when pack
                    {:key key :pack pack
                     :cover (pack/cover pack (cover-options pack (:rule region) bucket scale))})]
        [(assoc packs key entry) entry (some? entry)]))))

(defn prepare-path-frame!
  "System, draw items, view, world transforms → statistics; updates every
   cache, the atlas and the instance pool.

   {:changed? :runs :packs :instances :instance-writes :atlas {...}
    :cell-covers :items}. :runs counts constructions that ran, :packs the
   regions lowered, :instance-writes the rows uploaded. An unchanged frame
   key returns early with :changed? false and the previous counts."
  [system draw-items view world-transforms]
  (let [draw-items (or draw-items [])
        key (frame/frame-key draw-items view)
        prepared @(:!prepared system)]
    (if (= key @(:!last-frame-key system))
      {:changed? false :runs 0 :packs 0 :instances (count (:rows prepared)) :instance-writes 0
       :atlas (coverage/stats (:atlas system)) :cell-covers 0 :items (:items prepared)}
      (let [atlas (:atlas system)
            ;; pass 1: runs and packs
            [runs packs items run-count pack-count]
            (reduce
             (fn [[runs packs items run-count pack-count] item]
               (let [record (:path/material item)
                     group-index (transform/buffer-index world-transforms (:container item))
                     wt (get world-transforms (:container item))
                     iv (item-view view wt)
                     bucket (pack/scale-bucket (:scale iv))
                     [runs result ran?] (run-for runs record iv)
                     [packs clip-entry clip-packed?] (if (:clip result)
                                                       (pack-for packs (:clip result) bucket (:scale iv))
                                                       [packs nil false])
                     [packs regions region-packs]
                     (reduce (fn [[packs regions n] region]
                               (let [[packs entry packed?] (pack-for packs region bucket (:scale iv))]
                                 [packs (conj regions [region entry]) (if packed? (inc n) n)]))
                             [packs [] 0] (:regions result))]
                 [runs packs
                  (conj items {:record record :group-index group-index :regions regions
                               :clip (when clip-entry [(:clip result) clip-entry])
                               :ok? (:ok? result) :missing (:missing result) :log (:log result)})
                  (if ran? (inc run-count) run-count)
                  (+ pack-count region-packs (if clip-packed? 1 0))]))
             [@(:!runs system) @(:!packs system) [] 0 0]
             draw-items)
            used-keys (into #{} (for [item items
                                      [_ entry] (concat (:regions item) (when (:clip item) [(:clip item)]))
                                      :when entry]
                                  (:key entry)))]
        ;; the atlas keeps only this frame's packs, then takes the new ones
        (coverage/retain! atlas used-keys)
        (let [item-rows (mapv (fn [item]
                                (let [clip (when-let [[clip-region entry] (:clip item)]
                                             {:slot (coverage/insert! atlas (:key entry) (:pack entry))
                                              :rule (:rule clip-region)})]
                                  (vec (for [[region entry] (:regions item)
                                             :when entry
                                             :let [slot (coverage/insert! atlas (:key entry) (:pack entry))
                                                   color (component/region-color (:record item) region)]
                                             [x0 y0 x1 y1] (:rects (:cover entry))]
                                         {:rect [x0 y0 (- x1 x0) (- y1 y0)] :slot slot :color color
                                          :rule (:rule region) :index (:group-index item) :clip clip}))))
                              items)
              rows (vec (apply concat item-rows))
              item-ranges (second (reduce (fn [[offset ranges] item-rows]
                                            [(+ offset (count item-rows)) (conj ranges [offset (count item-rows)])])
                                          [0 []] item-rows))
              flushed (coverage/flush! atlas)
              writes (buffer-pool/batch-update-pool! (:pool system) rows)
              cell-covers (count (filter (fn [item] (some (fn [[_ e]] (and e (> (count (:rects (:cover e))) 1))) (:regions item))) items))]
          (when (:regrown? flushed)
            (reset! (:!bind-group system)
                    (create-bind-group (:device system) (:bind-layout system) atlas (:camera-buffer system) (:groups-buffer system))))
          (reset! (:!runs system) (select-keys runs (map (comp :path/material-id :path/material) draw-items)))
          (reset! (:!packs system) (select-keys packs used-keys))
          (reset! (:!prepared system) {:rows rows :item-ranges item-ranges
                                       :items (mapv #(select-keys % [:ok? :missing :log]) items)})
          (reset! (:!last-frame-key system) key)
          {:changed? true :runs run-count :packs pack-count :instances (count rows) :instance-writes writes
           :atlas (merge (coverage/stats atlas) flushed) :cell-covers cell-covers
           :items (mapv #(select-keys % [:ok? :missing :log]) items)})))))

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
  (draw-path-instances! pass system 0 (count (:rows @(:!prepared system)))))

(defn prepared-rows
  "System → the instance rows of the last prepared frame."
  [system]
  (:rows @(:!prepared system)))

(defn item-range
  "System and draw-item index → [first-instance count] of that item's rows
   in the last prepared frame, so a caller can draw one item between other
   passes."
  [system index]
  (nth (:item-ranges @(:!prepared system)) index [0 0]))

(defn destroy-path-system!
  "System → nil; destroys the atlas and the instance buffer, clears the
   caches. A destroyed system is not reusable."
  [system]
  (coverage/destroy-atlas! (:atlas system))
  (when-let [buffer (:buffer @(:pool system))]
    (.destroy ^js buffer))
  (reset! (:!runs system) {})
  (reset! (:!packs system) {})
  (reset! (:!prepared system) {:rows [] :items [] :item-ranges []})
  (reset! (:!last-frame-key system) ::destroyed)
  nil)
