(ns app.client.region3d.on-plane-renderer
  "Pack and render placed ink on object planes.

   Input: resolved placements, maintained matrices, camera and the prior
   per-region GPU state. Output: updated packing state, status counts and
   one instanced coverage draw per region. Each region owns a curve/band
   atlas, an instance buffer and a storage buffer of placement matrices;
   the system owns the pipeline. Placed ink takes the same regions and
   packs the 2D path lane draws (region3d/on-plane) and differs only in
   its vertex stage: the cover rectangle goes through the placement's
   model matrix and the region's view projection, and the fragment reads
   its local position as a perspective-correct varying.

   Resolved ink is the supported GPU placement kind. Resolved text and other
   kinds receive :unsupported-kind.

   Folder map: README.md."
  (:require [app.client.engine.buffer-pool :as buffer-pool]
            [app.client.engine.coverage :as coverage]
            [app.client.region3d.on-plane :as on-plane]
            [app.client.region3d.scene :as scene]))

(def placement-gpu-version 2)
(def placement-depth-bias -1)
(def placement-depth-bias-slope-scale -1.0)
(def matrix-floats 16)

;; Vertex main: six vertices per instance span the cover rectangle in the
;; ink's local units, flipped to object-local Y up, placed by the
;; instance's model matrix (tags.y indexes the region's placement matrices)
;; and the region's view projection. The fragment receives the local
;; position interpolated with perspective correction, takes device pixels
;; per local unit from its screen derivative, and paints the linear
;; premultiplied colour times the coverage. Depth is tested, not written,
;; with the surface bias.
(def placed-region-shader
  (str "
  struct Region {
    view_proj: mat4x4<f32>, eye: vec4<f32>, ambient: vec4<f32>,
    settings: vec3<f32>,
  };
  @group(0) @binding(2) var<uniform> region: Region;
  @group(0) @binding(3) var<storage, read> placements: array<mat4x4<f32>>;
  " coverage/instance-input-wgsl "
  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) local_pos: vec2<f32>,
    @location(1) @interpolate(flat) color: vec4<f32>,
    @location(2) @interpolate(flat) band: vec4<u32>,
    @location(3) @interpolate(flat) band_xf: vec4<f32>,
    @location(4) @interpolate(flat) tags: vec4<u32>,
    @location(5) @interpolate(flat) clip_band: vec4<u32>,
    @location(6) @interpolate(flat) clip_xf: vec4<f32>,
  };

  @vertex
  fn vs(@builtin(vertex_index) v_index: u32, inst: RegionInstance) -> VertexOutput {
    var pos = vec2<f32>(0.0, 0.0);
    switch(v_index) {
      case 0u: { pos = vec2<f32>(0.0, 0.0); }
      case 1u: { pos = vec2<f32>(1.0, 0.0); }
      case 2u: { pos = vec2<f32>(0.0, 1.0); }
      case 3u: { pos = vec2<f32>(1.0, 0.0); }
      case 4u: { pos = vec2<f32>(1.0, 1.0); }
      default: { pos = vec2<f32>(0.0, 1.0); }
    }
    let local_pos = inst.rect.xy + pos * inst.rect.zw;
    let model = placements[inst.tags.y];
    var out: VertexOutput;
    out.position = region.view_proj * model * vec4<f32>(local_pos.x, -local_pos.y, 0.0, 1.0);
    out.local_pos = local_pos;
    out.color = inst.color;
    out.band = inst.band;
    out.band_xf = inst.band_xf;
    out.tags = inst.tags;
    out.clip_band = inst.clip_band;
    out.clip_xf = inst.clip_xf;
    return out;
  }
  " coverage/coverage-wgsl "
  @fragment
  fn fs(in: VertexOutput) -> @location(0) vec4<f32> {
    let ppu = 1.0 / max(fwidth(in.local_pos), vec2<f32>(kMinDerivative, kMinDerivative));
    let alpha = region_alpha(in.local_pos, ppu, in.band, in.band_xf, in.tags.x, in.clip_band, in.clip_xf, "
       coverage/log2-atlas-width "u);
    return in.color * alpha;
  }"))

(defn- blend-state
  "No input → premultiplied blend descriptor. Fixed lane contract."
  []
  {:color {:srcFactor "one" :dstFactor "one-minus-src-alpha"}
   :alpha {:srcFactor "one" :dstFactor "one-minus-src-alpha"}})

(defn- create-pipelines!
  "Device → the placed-region pipeline and its binding layout.

   Fixed RGBA16F, 4× MSAA, depth-tested transparent geometry, for the
   region interior target."
  [^js device]
  (let [module (.createShaderModule device (clj->js {:code placed-region-shader}))
        layout (.createBindGroupLayout
                device
                (clj->js {:entries (into (coverage/bind-group-layout-entries js/GPUShaderStage.FRAGMENT)
                                         [{:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                          {:binding 3 :visibility js/GPUShaderStage.VERTEX :buffer {:type "read-only-storage"}}])}))
        depth {:format "depth24plus" :depthWriteEnabled false
               :depthCompare "less-equal" :depthBias placement-depth-bias
               :depthBiasSlopeScale placement-depth-bias-slope-scale}
        pipeline (.createRenderPipeline
                  device
                  (clj->js {:layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [layout]}))
                            :vertex {:module module :entryPoint "vs"
                                     :buffers [{:arrayStride coverage/instance-stride
                                                :stepMode "instance"
                                                :attributes coverage/instance-attributes}]}
                            :fragment {:module module :entryPoint "fs"
                                       :targets [{:format "rgba16float" :blend (blend-state)}]}
                            :primitive {:topology "triangle-list" :cullMode "none"}
                            :depthStencil depth :multisample {:count 4}}))]
    {:layout layout :placed pipeline}))

(defn init-placement-system!
  "Device → pipeline owner."
  [device]
  {:placement-gpu/version placement-gpu-version
   :device device
   :pipelines (create-pipelines! device)})

(defn- create-matrix-buffer
  [^js device label matrices]
  (.createBuffer device (clj->js {:label label
                                  :size (* 4 matrix-floats (max 1 matrices))
                                  :usage (bit-or js/GPUBufferUsage.STORAGE js/GPUBufferUsage.COPY_DST)})))

(defn create-region-gpu!
  "System and region ID → the region's atlas, instance pool, matrix buffer
   and empty packing state."
  [system region-id]
  (let [device (:device system)
        label (str "region3d/" region-id "/placed")]
    {:atlas (coverage/create-atlas device :label label :curve-rows 4 :band-rows 4)
     :pool (buffer-pool/create-pool device 16 :floats-per-item coverage/instance-words
                                    :pack-fn coverage/pack-instance)
     :matrices {:buffer (create-matrix-buffer device (str label "/matrices") 4) :capacity 4 :label (str label "/matrices")}
     :pack-cache {}
     :pack-key ::never
     :instances 0
     :placements []
     :coverage-check {}}))

(defn- column-major
  "Row-major matrix → GPU column order."
  [matrix]
  (mapv #(nth matrix %) [0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15]))

(defn- placement-key
  "Placement and matrix → identity/kind/status/content-revision/matrix
   tuple, including the complete component value for content and paint changes."
  [placed matrix]
  [(:object-id placed) (:kind placed) (:status placed) (:content-revision placed) (:component placed) matrix])

(defn- pack-one
  "Old row, placement, maintained scene → {:row :packed?}: reuses an equal
   key, carries unresolved statuses, runs ink through on-plane's regions.
   Unsupported kinds are explicit data."
  [old placed maintained]
  (let [matrix (get-in maintained [:world-transforms (:object-id placed)])
        key (placement-key placed matrix)]
    (cond
      (= key (:key old)) {:row old :packed? false}
      (not= :resolved (:status placed))
      {:packed? true
       :row {:key key :placed (assoc placed :matrix matrix)
             :packed {:status (:status placed) :kind (:kind placed) :matrix matrix}}}
      (= :ink (:kind placed))
      (let [ink (on-plane/placed-ink-regions placed)]
        {:packed? true
         :row {:key key :placed (assoc placed :matrix matrix)
               :packed {:status (if (:ok? ink) :resolved :construction-failed)
                        :kind :ink :matrix matrix
                        :regions (:regions ink) :missing (:missing ink)
                        :curves (reduce + 0 (map (comp :count :pack) (:regions ink)))}}})
      :else
      {:packed? true
       :row {:key key :placed (assoc placed :matrix matrix)
             :packed {:status :unsupported-kind :kind (:kind placed) :matrix matrix}}})))

(defn- depth-of
  [matrix camera]
  (scene/length (scene/v- (scene/transform-point matrix [0.0 0.0 0.0]) (:eye camera))))

(defn- ensure-matrix-buffer!
  [system {:keys [capacity label] :as row} needed]
  (if (>= capacity needed)
    row
    (let [capacity (loop [c (max 1 capacity)] (if (>= c needed) c (recur (* 2 c))))]
      (.destroy ^js (:buffer row))
      {:buffer (create-matrix-buffer (:device system) label capacity) :capacity capacity :label label})))

(defn- bind-group
  [system region-gpu region-uniform]
  (let [{:keys [curve-view band-view]} (coverage/views (:atlas region-gpu))]
    (.createBindGroup ^js (:device system)
                      (clj->js {:layout (get-in system [:pipelines :layout])
                                :entries [{:binding 0 :resource curve-view}
                                          {:binding 1 :resource band-view}
                                          {:binding 2 :resource {:buffer (:buffer region-uniform)}}
                                          {:binding 3 :resource {:buffer (:buffer (:matrices region-gpu))}}]}))))

(defn prepare-placements!
  "System, previous region GPU row, placements, maintained scene, camera →
   {:gpu :changed? :packs :placements :coverage-check :uploads :instances
    :ink-curves}.

   Per-placement reuse by key; on a pack change the resolved ink rows are
   ordered back to front by object-origin distance, their matrices written
   to the storage buffer, their packs kept in the region's atlas and their
   cover rectangles written as instance rows in that order. A camera-only
   change keeps the old order."
  [system region-gpu placements maintained camera]
  (let [old-cache (:pack-cache region-gpu)
        {:keys [rows packs]} (reduce (fn [{:keys [rows packs]} placed]
                                       (let [{:keys [row packed?]} (pack-one (get old-cache (:object-id placed)) placed maintained)]
                                         {:rows (conj rows row) :packs (+ packs (if packed? 1 0))}))
                                     {:rows [] :packs 0} placements)
        next-cache (into {} (map (fn [row] [(get-in row [:placed :object-id]) row])) rows)
        pack-key (mapv (fn [{:keys [key packed]}] [key (:status packed) (:curves packed)]) rows)
        changed? (not= pack-key (:pack-key region-gpu))
        statuses (frequencies (map (comp :status :packed) rows))
        placed-out (mapv (fn [{:keys [placed packed]}] (assoc placed :status (:status packed))) rows)]
    (if-not changed?
      {:gpu (assoc region-gpu :pack-cache next-cache :coverage-check statuses)
       :changed? false :packs packs :placements (:placements region-gpu)
       :coverage-check statuses :uploads 0 :instances (:instances region-gpu)
       :ink-curves (reduce + 0 (map (comp #(or % 0) :curves :packed) rows))}
      (let [ink-rows (->> rows
                          (filter (fn [{:keys [packed]}] (and (= :resolved (:status packed)) (= :ink (:kind packed)))))
                          (map (fn [row] (assoc row :depth (depth-of (get-in row [:packed :matrix]) camera))))
                          (sort-by (juxt (comp - :depth) (comp pr-str :object-id :placed)))
                          vec)
            atlas (:atlas region-gpu)
            used-keys (into #{} (mapcat (fn [row]
                                           (mapcat (fn [entry]
                                                     (cond-> [[(:object-id (:placed row)) (:pack entry)]]
                                                       (:clip entry) (conj [(:object-id (:placed row)) (get-in entry [:clip :pack])])))
                                                   (get-in row [:packed :regions]))) ink-rows))
            _ (coverage/retain! atlas used-keys)
            instances (vec (for [[index row] (map-indexed vector ink-rows)
                                 entry (get-in row [:packed :regions])
                                 :let [slot (coverage/insert! atlas [(:object-id (:placed row)) (:pack entry)] (:pack entry))
                                       clip (when-let [clip (:clip entry)]
                                              {:rule (:rule clip)
                                               :slot (coverage/insert! atlas [(:object-id (:placed row)) (:pack clip)] (:pack clip))})
                                       color (on-plane/linear-premultiplied (:color entry) 1.0 1.0)]
                                 [x0 y0 x1 y1] (:rects (:cover entry))]
                             {:rect [x0 y0 (- x1 x0) (- y1 y0)] :slot slot :color color
                              :rule (:rule (:region entry)) :index index :clip clip}))
            flushed (coverage/flush! atlas)
            matrix-row (ensure-matrix-buffer! system (:matrices region-gpu) (max 1 (count ink-rows)))
            matrix-data (js/Float32Array. (clj->js (vec (mapcat (fn [row] (column-major (get-in row [:packed :matrix]))) ink-rows))))
            _ (when (pos? (.-length matrix-data))
                (.writeBuffer (.-queue ^js (:device system)) (:buffer matrix-row) 0 matrix-data))
            writes (buffer-pool/batch-update-pool! (:pool region-gpu) instances)
            gpu (assoc region-gpu
                       :pack-cache next-cache :pack-key pack-key
                       :coverage-check statuses :instances (count instances)
                       :placements placed-out :matrices matrix-row)]
        {:gpu gpu :changed? true :packs packs :placements placed-out
         :coverage-check statuses
         :uploads (+ (if (pos? writes) 1 0) (if (pos? (.-length matrix-data)) 1 0)
                     (if (pos? (+ (:curve-rows flushed) (:band-rows flushed))) 1 0))
         :instances (count instances)
         :ink-curves (reduce + 0 (map (comp #(or % 0) :curves :packed) rows))
         :atlas (merge (coverage/stats atlas) flushed)}))))

(defn draw-placements!
  "Open pass, system, region GPU row, uniform → draw count; encodes the
   region's placed ink as one instanced draw in the prepared order. The
   bind group is made per call: the atlas views and the uniform buffer are
   both allowed to change between frames."
  [pass system region-gpu region-uniform]
  (let [instances (:instances region-gpu 0)]
    (if (pos? instances)
      (let [bind (bind-group system region-gpu region-uniform)]
        (.setPipeline ^js pass (get-in system [:pipelines :placed]))
        (.setBindGroup ^js pass 0 bind)
        (.setVertexBuffer ^js pass 0 (:buffer @(:pool region-gpu)))
        (.draw ^js pass 6 instances 0 0)
        1)
      0)))

(defn destroy-region-gpu!
  "Region GPU row → destroys its atlas, instance buffer and matrix buffer."
  [region-gpu]
  (when-let [atlas (:atlas region-gpu)]
    (coverage/destroy-atlas! atlas))
  (when-let [buffer (:buffer @(:pool region-gpu))]
    (.destroy ^js buffer))
  (when-let [buffer (:buffer (:matrices region-gpu))]
    (.destroy ^js buffer)))

(defn destroy-placement-system!
  "System → true, without resource operations: the system holds pipeline
   and layout handles only."
  [_system] true)
