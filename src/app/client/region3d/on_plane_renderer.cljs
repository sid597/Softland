(ns app.client.region3d.on-plane-renderer
  "The on-plane renderer: packs and draws ink placed inside a 3D region.
   Takes: the region system, a region's GPU state, placements, the maintained
   scene, a camera, and a path cache; a render pass and a region uniform to
   draw.
   Gives: the caller's path cache plus packed rows in a flat vertex buffer; ink
   draw calls.
   Holds: GPU pipelines and per-region packing caches."
  (:require [app.client.region3d.on-plane :as on-plane]
            [app.client.region3d.scene :as scene]))

(def placement-gpu-version 1)
(def flat-vertex-stride 88)
(def placement-depth-bias -1)
(def placement-depth-bias-slope-scale -1.0)

(def placed-flat-shader
  "struct Region {
     view_proj: mat4x4<f32>, eye: vec4<f32>, ambient: vec4<f32>,
     settings: vec3<f32>,
   };
   @group(0) @binding(0) var<uniform> region: Region;
   struct In {
     @location(0) point: vec2<f32>,
     @location(1) m0: vec4<f32>, @location(2) m1: vec4<f32>,
     @location(3) m2: vec4<f32>, @location(4) m3: vec4<f32>,
     @location(5) color: vec4<f32>,
   };
   struct Out { @builtin(position) position: vec4<f32>,
                @location(0) color: vec4<f32>, };
   @vertex fn vs(input: In) -> Out {
     let model = mat4x4<f32>(input.m0, input.m1, input.m2, input.m3);
     var out: Out;
     out.position = region.view_proj * model
                  * vec4<f32>(input.point.x, -input.point.y, 0.0, 1.0);
     out.color = input.color;
     return out;
   }
   @fragment fn fs(input: Out) -> @location(0) vec4<f32> {
     return input.color;
   }")

(defn- shader-module [device code]
  (.createShaderModule ^js device (clj->js {:code code})))

(defn- blend-state []
  {:color {:srcFactor "one" :dstFactor "one-minus-src-alpha"}
   :alpha {:srcFactor "one" :dstFactor "one-minus-src-alpha"}})

(defn- pipeline-layout [device layouts]
  (.createPipelineLayout ^js device (clj->js {:bindGroupLayouts layouts})))

(defn- create-pipelines! [device]
  (let [flat-module (shader-module device placed-flat-shader)
        flat-layout
        (.createBindGroupLayout
         ^js device
         (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.VERTEX
                              :buffer {:type "uniform"}}]}))
        depth {:format "depth24plus" :depthWriteEnabled false
               :depthCompare "less-equal" :depthBias placement-depth-bias
               :depthBiasSlopeScale placement-depth-bias-slope-scale}
        target {:format "rgba16float" :blend (blend-state)}
        flat
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (pipeline-layout device [flat-layout])
                   :vertex {:module flat-module :entryPoint "vs"
                            :buffers [{:arrayStride flat-vertex-stride
                                       :stepMode "vertex"
                                       :attributes
                                       [{:shaderLocation 0 :offset 0
                                         :format "float32x2"}
                                        {:shaderLocation 1 :offset 8
                                         :format "float32x4"}
                                        {:shaderLocation 2 :offset 24
                                         :format "float32x4"}
                                        {:shaderLocation 3 :offset 40
                                         :format "float32x4"}
                                        {:shaderLocation 4 :offset 56
                                         :format "float32x4"}
                                        {:shaderLocation 5 :offset 72
                                         :format "float32x4"}]}]}
                   :fragment {:module flat-module :entryPoint "fs"
                              :targets [target]}
                   :primitive {:topology "triangle-list" :cullMode "none"}
                   :depthStencil depth :multisample {:count 4}}))]
    {:flat-layout flat-layout :flat flat}))

(defn- create-buffer! [system label size usage]
  (let [size (max 4 (int size))
        buffer (.createBuffer ^js (:device system)
                              (clj->js {:label label :size size :usage usage}))]
    {:buffer buffer :capacity size :label label}))

(defn- ensure-buffer! [system current label required]
  (let [required (max 4 (int required))]
    (if (>= (:capacity current) required)
      current
      (let [capacity (loop [candidate (:capacity current)]
                       (if (>= candidate required) candidate
                           (recur (* 2 candidate))))
            next (.createBuffer
                  ^js (:device system)
                  (clj->js {:label label :size capacity
                            :usage (bit-or js/GPUBufferUsage.COPY_DST
                                           js/GPUBufferUsage.VERTEX)}))]
        (.destroy ^js (:buffer current))
        {:buffer next :capacity capacity :label label}))))

(defn- write-buffer! [system row values]
  (let [data (js/Float32Array. (clj->js (vec values)))
        bytes (.-byteLength data)]
    (when (pos? bytes)
      (.writeBuffer (.-queue ^js (:device system)) (:buffer row) 0 data))
    bytes))

(defn init-placement-system! [device]
  {:placement-gpu/version placement-gpu-version
   :device device
   :pipelines (create-pipelines! device)})

(defn create-region-gpu! [system region-id]
  (let [usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.VERTEX)]
    {:flat (create-buffer! system (str "region3d/" region-id "/placed-flat")
                           256 usage)
     :pack-cache {}
     :pack-key ::never
     :draw-order []
     :placements []
     :coverage-check {}}))

(defn- column-major [matrix]
  (mapv #(nth matrix %) [0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15]))

(defn- ink-pack [cache placed matrix]
  (let [{next-cache :cache pack :pack} (on-plane/pack-placed-ink cache placed)]
    {:cache next-cache
     :packed {:status :resolved :kind :ink :matrix matrix
              :vertices (:vertices pack) :color (:color pack)
              :vertex-count (count (:vertices pack))}}))

(defn- placement-key [placed matrix]
  [(:object-id placed) (:kind placed) (:status placed)
   (:content-revision placed) matrix])

(defn- pack-one [cache old placed maintained]
  (let [matrix (get-in maintained [:world-transforms (:object-id placed)])
        key (placement-key placed matrix)]
    (cond
      (= key (:key old)) {:cache cache :row old :packed? false}
      (not= :resolved (:status placed))
      {:cache cache :packed? true
       :row {:key key :placed (assoc placed :matrix matrix)
             :packed {:status (:status placed) :kind (:kind placed)
                      :matrix matrix}}}
      (= :ink (:kind placed))
      (let [{next-cache :cache packed :packed} (ink-pack cache placed matrix)]
        {:cache next-cache :packed? true
         :row {:key key :placed (assoc placed :matrix matrix)
               :packed packed}})
      :else
      {:cache cache :packed? true
       :row {:key key :placed (assoc placed :matrix matrix)
             :packed {:status :unsupported-kind :kind (:kind placed)
                      :matrix matrix}}})))

(defn- device-ink-vertex-limit [system]
  (let [^js device (:device system)
        max-bytes (or (some-> device .-limits .-maxBufferSize) 268435456)]
    (long (/ max-bytes flat-vertex-stride))))

(defn- enforce-region-limits [system rows]
  (loop [remaining (sort-by (comp pr-str :object-id :placed) rows)
         ink-vertices 0 result []]
    (if-let [row (first remaining)]
      (let [packed (:packed row)
            next-ink (+ ink-vertices (or (:vertex-count packed) 0))
            over? (> next-ink (device-ink-vertex-limit system))
            row (if (and (= :resolved (:status packed)) over?)
                  (-> row
                      (assoc-in [:packed :status] :over-limit)
                      (assoc-in [:packed :vertices] []))
                  row)]
        (recur (next remaining)
               (if over? ink-vertices next-ink)
               (conj result row)))
      result)))

(defn- build-uploads [rows maintained camera]
  (reduce
   (fn [{:keys [flat] :as result} row]
     (let [{:keys [placed packed]} row
           kind (:kind packed)
           status (:status packed)
           matrix (:matrix packed)
           color (when (= :resolved status)
                   (on-plane/linear-premultiplied
                    (:color packed) 1.0 1.0))
           depth (when matrix
                   (scene/length
                    (scene/v- (scene/transform-point matrix [0.0 0.0 0.0])
                              (:eye camera))))
           placed (assoc placed :status status)]
       (cond
         (and (= :resolved status) (= :ink kind))
         (let [first-vertex (quot (count flat) 22)
               values (vec (mapcat (fn [[x y]]
                                     (concat [x y] (column-major matrix) color))
                                   (:vertices packed)))]
           (-> result
               (update :flat into values)
               (update :draws conj {:kind :ink :object-id (:object-id placed)
                                    :first first-vertex
                                    :count (:vertex-count packed) :depth depth})
               (update :placements conj placed)))

         :else (update result :placements conj placed))))
   {:flat [] :draws [] :placements []}
   rows))

(defn- sort-draws [draws maintained camera]
  (vec
   (sort-by
    (juxt (comp - :depth) (comp pr-str :object-id))
    (map (fn [draw]
           (let [matrix (get-in maintained
                                [:world-transforms (:object-id draw)])]
             (assoc draw :depth
                    (scene/length
                     (scene/v- (scene/transform-point matrix [0.0 0.0 0.0])
                               (:eye camera))))))
         draws))))

(defn prepare-placements!
  "Pack changed placements and upload only when the aggregate packing key
   changes. Returns the caller-owned path cache value after ink derivation."
  [system region-gpu placements maintained camera path-cache _options]
  (let [old-cache (:pack-cache region-gpu)
        packed
        (reduce
         (fn [{:keys [path-cache rows packs]} placed]
           (let [result (pack-one path-cache (get old-cache (:object-id placed))
                                  placed maintained)]
             {:path-cache (:cache result)
              :rows (conj rows (:row result))
              :packs (+ packs (if (:packed? result) 1 0))}))
         {:path-cache (or path-cache {}) :rows [] :packs 0}
         placements)
        rows (enforce-region-limits system (:rows packed))
        ;; The cache value is the whole row: `pack-one` compares its :key and
        ;; reuses its packed payload.  Storing only :placed here erases both,
        ;; turning every otherwise-idle prepare into a layout + pack.
        next-cache (into {}
                         (map (fn [row]
                                [(get-in row [:placed :object-id]) row]))
                         rows)
        pack-key (mapv (fn [{:keys [key packed]}]
                         [key (:status packed) (:vertex-count packed)]) rows)
        changed? (not= pack-key (:pack-key region-gpu))
        upload (if changed? (build-uploads rows maintained camera)
                   {:placements (:placements region-gpu)})
        flat-data (when changed? (:flat upload))
        flat-buffer (if changed?
                      (ensure-buffer! system (:flat region-gpu)
                                      (:label (:flat region-gpu))
                                      (* 4 (count flat-data)))
                      (:flat region-gpu))
        uploads (if changed?
                  (if (pos? (write-buffer! system flat-buffer flat-data)) 1 0)
                  0)
        statuses (frequencies (map (comp :status :packed) rows))
        next-gpu (cond-> (assoc region-gpu :pack-cache next-cache
                                :pack-key pack-key :flat flat-buffer
                                :coverage-check statuses
                                :draw-order
                                (if changed?
                                  (sort-draws (:draws upload) maintained camera)
                                  (:draw-order region-gpu)))
                   changed? (assoc :placements (:placements upload)))
        ink-count (reduce + 0 (map #(or (get-in % [:packed :vertex-count]) 0)
                                       rows))]
    {:gpu next-gpu :path-cache (:path-cache packed)
     :changed? changed? :packs (:packs packed)
     :placements (:placements next-gpu) :coverage-check statuses
     :uploads uploads :ink-vertices ink-count
     :over-limit (get statuses :over-limit 0)}))

(defn- flat-bind-group [system region-uniform]
  (.createBindGroup
   ^js (:device system)
   (clj->js {:layout (get-in system [:pipelines :flat-layout])
             :entries [{:binding 0
                        :resource {:buffer (:buffer region-uniform)}}]})))

(defn draw-placements! [pass system region-gpu region-uniform]
  (let [flat-bind (delay (flat-bind-group system region-uniform))
        draws (atom 0)]
    (doseq [{:keys [kind first count]} (:draw-order region-gpu)
            :when (pos? count)]
      (case kind
        :ink
        (do (.setPipeline ^js pass (get-in system [:pipelines :flat]))
            (.setBindGroup ^js pass 0 @flat-bind)
            (.setVertexBuffer ^js pass 0 (:buffer (:flat region-gpu)))
            (.draw ^js pass count 1 first 0)
            (swap! draws inc))
        nil))
    @draws))

(defn- destroy-buffer! [row]
  (when-let [buffer (:buffer row)]
    (.destroy ^js buffer)))

(defn destroy-region-gpu! [region-gpu]
  (destroy-buffer! (:flat region-gpu)))

(defn destroy-placement-system! [_system] true)
