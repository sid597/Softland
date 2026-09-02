(ns app.client.engine.compositor
  "The compositor: offscreen render targets and how they reach the screen. Owns
   a recycling pool of GPU textures, region leases, the single present to the
   canvas, and raster readback.
   Takes: a device and output format; lease requests by region; a target pass
   to begin, draw into, and release.
   Gives: a compositor with its pool and presentation pipeline; leased targets; one
   presented frame; pixels read back as evidence.
   Holds: the pool state (free and leased targets, counters, rejections); the
   current region leases, the lease keys retiring this frame, and retired
   targets awaiting release; a stats atom."
  (:require [app.client.engine.rungs :as region-rungs]
            [app.client.engine.leases :as region-bindings]))

(def target-pool-version 2)
(def compositor-version 2)
(def region-lease-quant 256)
(def region-lease-max 4096)
(def default-pool-budget-bytes (* 512 1024 1024))

(def full-screen-vertex-shader
  "struct Output { @builtin(position) position: vec4<f32>,
                    @location(0) uv: vec2<f32>, };
   @vertex fn main(@builtin(vertex_index) index: u32) -> Output {
     var positions = array<vec2<f32>, 3>(vec2<f32>(-1.0, -1.0),
                                          vec2<f32>(3.0, -1.0),
                                          vec2<f32>(-1.0, 3.0));
     var out: Output;
     out.position = vec4<f32>(positions[index], 0.0, 1.0);
     out.uv = positions[index] * vec2<f32>(0.5, -0.5) + vec2<f32>(0.5, 0.5);
     return out;
   }")

(def present-fragment-shader
  "@group(0) @binding(0) var linear_sampler: sampler;
   @group(0) @binding(1) var scene_texture: texture_2d<f32>;
   fn linear_to_srgb(v: f32) -> f32 {
     if (v <= 0.0031308) { return 12.92 * v; }
     return 1.055 * pow(v, 1.0 / 2.4) - 0.055;
   }
   @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
     let premult = textureSample(scene_texture, linear_sampler, uv);
     let straight = select(vec3<f32>(0.0), premult.rgb / premult.a,
                           premult.a > 0.000001);
     let encoded = vec3<f32>(linear_to_srgb(straight.r),
                             linear_to_srgb(straight.g),
                             linear_to_srgb(straight.b));
     return vec4<f32>(encoded * premult.a, premult.a);
   }")

(defn texture-bytes
  ([format width height]
   (texture-bytes format width height 1))
  ([format width height sample-count]
   (* width height (max 1 (or sample-count 1))
      (case format
        "rgba16float" 8
        ("rgba8unorm" "bgra8unorm" "rgba8unorm-srgb" "bgra8unorm-srgb"
         "depth24plus" "depth32float") 4
        (throw (ex-info "Frame target format lacks byte pricing"
                        {:format format}))))))

(defn create-target-pool
  [device & {:keys [budget-cap-bytes]
                     :or {budget-cap-bytes default-pool-budget-bytes}}]
  {:device device :budget-cap-bytes budget-cap-bytes
   :!state (atom {:free {} :leased {} :allocations 0 :reuses 0
                  :destroyed 0 :high-water-leased 0 :epoch 0 :rejections []})})

(defn- reserved-bytes [state]
  (reduce + 0 (map :bytes (concat (vals (:leased state))
                                  (mapcat identity (vals (:free state)))))))

(defn- target-key [format width height sample-count]
  [format (int width) (int height) (max 1 (or sample-count 1))])

(defn- reclaim-free-targets!
  "Discard the recoverable cache before refusing a new target. A viewport
   resize otherwise strands the old dimensions inside the fixed pool budget."
  [pool]
  (let [state @(:!state pool)
        targets (vec (mapcat val (:free state)))]
    (when (seq targets)
      (swap! (:!state pool)
             (fn [current]
               (-> current
                   (assoc :free {})
                   (update :destroyed + (count targets)))))
      (doseq [target targets]
        (.destroy ^js (:texture target))))
    (count targets)))

(defn- reclaim-stale-free-targets!
  "Held region leases keep :leased occupied across frames, so the empty-leased
   reclaim road never runs on the region3d lane. Free targets released before
   the current submit epoch can no longer be named by an unsubmitted encoder,
   so budget pressure may destroy them mid-frame."
  [pool]
  (let [state @(:!state pool)
        epoch (:epoch state 0)
        stale? #(< (:released-epoch % -1) epoch)
        targets (into [] (comp (mapcat val) (filter stale?)) (:free state))]
    (when (seq targets)
      (let [stale-ids (into #{} (map :target/id) targets)]
        (swap! (:!state pool)
               (fn [current]
                 (let [free (reduce-kv
                             (fn [acc key row]
                               (let [row (into [] (remove #(stale-ids
                                                            (:target/id %)))
                                               row)]
                                 (if (seq row) (assoc acc key row) acc)))
                             {} (:free current))]
                   (-> current
                       (assoc :free free)
                       (update :destroyed + (count targets))))))
        (doseq [target targets]
          (.destroy ^js (:texture target)))))
    (count targets)))

(defn- ensure-target-capacity!
  [pool bytes label]
  (let [state @(:!state pool)]
    (when (> (+ (reserved-bytes state) bytes) (:budget-cap-bytes pool))
      (let [rejection {:reason :frame-target-budget-exceeded
                     :requested-bytes bytes
                     :reserved-bytes (reserved-bytes state)
                     :budget-cap-bytes (:budget-cap-bytes pool)
                     :requested-by label}]
        (swap! (:!state pool) update :rejections conj rejection)
        (throw (ex-info "Frame target pool budget exceeded" rejection))))))

(defn- create-target!
  [pool format width height label sample-count usage reclaim-free?]
  (let [sample-count (max 1 (or sample-count 1))
        bytes (texture-bytes format width height sample-count)
        _ (when (and reclaim-free?
                     (> (+ (reserved-bytes @(:!state pool)) bytes)
                        (:budget-cap-bytes pool)))
            (if (empty? (:leased @(:!state pool)))
              (reclaim-free-targets! pool)
              (reclaim-stale-free-targets! pool)))
        _ (ensure-target-capacity! pool bytes label)]
    (let [texture (.createTexture
                   ^js (:device pool)
                   (clj->js {:label label
                             :size {:width width :height height
                                    :depthOrArrayLayers 1}
                             :format format
                             :sampleCount sample-count
                             :usage (or usage
                                        (bit-or
                                         js/GPUTextureUsage.RENDER_ATTACHMENT
                                         js/GPUTextureUsage.TEXTURE_BINDING
                                         js/GPUTextureUsage.COPY_SRC
                                         js/GPUTextureUsage.COPY_DST))}))
          target {:target/id (random-uuid) :texture texture
                  :view (.createView texture) :format format
                  :width width :height height :sample-count sample-count
                  :bytes bytes :label label}]
      target)))

(defn acquire-target!
  [pool format width height label
   & {:keys [sample-count usage preserve-free? reclaim-free?]
      :or {sample-count 1 preserve-free? false reclaim-free? true}}]
  (let [width (max 1 (int width))
        height (max 1 (int height))
        sample-count (max 1 sample-count)
        key (target-key format width height sample-count)
        state @(:!state pool)
        ;; An unmatched first acquisition marks a frame/viewport boundary.
        ;; Reclaim there, while no unsubmitted encoder can still name a free
        ;; target. Same-frame releases remain reusable but never destroyable.
        reclaim-free? (and reclaim-free? (not preserve-free?))
        _ (when (and reclaim-free?
                     (empty? (:leased state))
                     (nil? (first (get-in state [:free key])))
                     (seq (:free state)))
            (reclaim-free-targets! pool))
        target (when-not preserve-free?
                 (first (get-in @(:!state pool) [:free key])))]
    (if target
      (do
        (swap! (:!state pool)
               (fn [state]
                 (let [remaining (subvec (get-in state [:free key]) 1)
                       state (if (seq remaining)
                               (assoc-in state [:free key] remaining)
                               (update state :free dissoc key))
                       leased (assoc (:leased state) (:target/id target) target)]
                   (-> state (assoc :leased leased)
                       (update :reuses inc)
                       (update :high-water-leased max (count leased))))))
        target)
      (let [target (create-target! pool format width height label
                                   sample-count usage reclaim-free?)]
        (swap! (:!state pool)
               (fn [state]
                 (let [leased (assoc (:leased state) (:target/id target) target)]
                   (-> state
                       (assoc :leased leased)
                       (update :allocations inc)
                       (update :high-water-leased max (count leased))))))
        target))))

(defn release-target! [pool target]
  (when target
    (swap! (:!state pool)
           (fn [state]
             (if-not (contains? (:leased state) (:target/id target))
               state
               (-> state
                   (update :leased dissoc (:target/id target))
                   (update-in [:free (target-key (:format target)
                                                (:width target)
                                                (:height target)
                                                (:sample-count target))]
                              (fnil conj [])
                              (assoc target :released-epoch
                                     (:epoch state 0))))))))
  nil)

(defn bump-frame-epoch!
  "Mark a submit boundary: everything free before this point is safe for
   reclaim-stale-free-targets! to destroy under budget pressure."
  [pool]
  (swap! (:!state pool) update :epoch (fnil inc 0))
  nil)

(defn- destroy-target! [pool target]
  (when target
    (let [target-id (:target/id target)
          key (target-key (:format target) (:width target) (:height target)
                          (:sample-count target))
          ;; Idempotent like release-target!: three async release roads can
          ;; name the same lease target.
          removed? (volatile! false)]
      (swap! (:!state pool)
             (fn [state]
               (let [tracked? (or (contains? (:leased state) target-id)
                                  (some #(= target-id (:target/id %))
                                        (get-in state [:free key] [])))]
                 (vreset! removed? (boolean tracked?))
                 (if-not tracked?
                   state
                   (let [free-rows (vec (remove #(= target-id (:target/id %))
                                                (get-in state [:free key] [])))
                         state (if (seq free-rows)
                                 (assoc-in state [:free key] free-rows)
                                 (update state :free dissoc key))]
                     (-> state
                         (update :leased dissoc target-id)
                         (update :destroyed inc)))))))
      (when @removed?
        (.destroy ^js (:texture target)))))
  nil)

(defn destroy-target-pool! [pool]
  (let [state @(:!state pool)
        targets (concat (vals (:leased state)) (mapcat val (:free state)))]
    (doseq [target targets]
      (.destroy ^js (:texture target)))
    (reset! (:!state pool)
            {:free {} :leased {} :allocations (:allocations state)
             :reuses (:reuses state) :destroyed (+ (:destroyed state)
                                                   (count targets))
             :high-water-leased (:high-water-leased state)
             :rejections (:rejections state)})
    nil))

(defn target-pool-stats [pool]
  (let [state @(:!state pool)]
    {:target-pool/version target-pool-version
     :budget-owner :frame-runtime/target-pool
     :budget-cap-bytes (:budget-cap-bytes pool)
     :allocations (:allocations state) :reuses (:reuses state)
     :leased (count (:leased state))
     :free (reduce + 0 (map count (vals (:free state))))
     :reserved-bytes (reserved-bytes state)
     :high-water-leased (:high-water-leased state)
     :destroyed (:destroyed state) :rejections (:rejections state)}))

(defn- shader-module [device code]
  (.createShaderModule ^js device (clj->js {:code code})))

(defn- create-pipelines! [device output-format]
  (let [sampler (.createSampler ^js device
                                (clj->js {:minFilter "linear" :magFilter "linear"
                                          :addressModeU "clamp-to-edge"
                                          :addressModeV "clamp-to-edge"}))
        present-layout
        (.createBindGroupLayout
         ^js device
         (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                              :sampler {:type "filtering"}}
                             {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                              :texture {:sampleType "float"}}]}))
        present-pipeline
        (fn [format]
          (.createRenderPipeline
           ^js device
           (clj->js {:layout (.createPipelineLayout
                              ^js device (clj->js {:bindGroupLayouts [present-layout]}))
                     :vertex {:module (shader-module device full-screen-vertex-shader)
                              :entryPoint "main"}
                     :fragment {:module (shader-module device present-fragment-shader)
                                :entryPoint "main"
                                :targets [{:format format}]}
                     :primitive {:topology "triangle-list"}})))]
    {:sampler sampler
     :present-layout present-layout
     :present-pipelines (into {} (map (fn [format]
                                       [format (present-pipeline format)]))
                              (distinct [output-format "rgba8unorm"]))
     :output-format output-format}))

(defn create-compositor!
  [device output-format & {:keys [budget-cap-bytes]}]
  {:compositor/version compositor-version :device device
   :format output-format
   :target-pool (create-target-pool device :budget-cap-bytes
                                   (or budget-cap-bytes
                                       default-pool-budget-bytes))
   :pipelines (create-pipelines! device output-format)
   :!region-leases (atom {})
   :!retired-region-targets (atom [])
   :!retiring-region-keys (atom #{})
   :!stats (atom {})})

(defn quantize-region-size [value]
  (-> (/ (max 1 (double value)) region-lease-quant)
      js/Math.ceil
      (* region-lease-quant)
      (min region-lease-max)
      int))

(defn region-lease
  ([compositor region-id]
   (some (fn [[key lease]]
           (when (= region-id (first key)) lease))
         @(:!region-leases compositor)))
  ([compositor region-id width height shadow?]
   (get @(:!region-leases compositor)
        [region-id (quantize-region-size width)
         (quantize-region-size height)])))

(defn- acquire-region-target!
  "Acquire persistent Region3D storage without reclaiming the free frame-road
   cache. Those targets are the observed transient reserve for a frame that
   already fit; consuming them here can admit the region and make the later
   mandatory group-output allocation kill the whole frame. The caller's
   existing lease-rejection path owns the combined-set failure instead."
  [compositor format width height label & {:keys [sample-count usage]
                                           :or {sample-count 1}}]
  (acquire-target! (:target-pool compositor) format width height label
                   :sample-count sample-count
                   :usage usage
                   :preserve-free? true))

(defn- region-lease-bytes [width height shadow?]
  (+ (texture-bytes "rgba16float" width height 4)
     (texture-bytes "depth24plus" width height 4)
     (texture-bytes "rgba16float" width height 1)
     (if shadow?
       (texture-bytes "depth32float" 2048 2048 1)
       0)))

(defn acquire-region-lease!
  "Acquire or reuse one compositor-owned held lease. Rejection is returned as
   data so the family can draw its declared fill; no target byte has another
   owner."
  [compositor region-id width height shadow?]
  (let [qw (quantize-region-size width)
        qh (quantize-region-size height)
        key [region-id qw qh]
        existing (get @(:!region-leases compositor) key)]
    (cond
      (and existing (= (boolean (:shadow existing)) (boolean shadow?)))
      existing

      (and existing shadow? (nil? (:shadow existing)))
      (try
        (let [label (str "region3d/" region-id "/shadow")
              _ (ensure-target-capacity!
                 (:target-pool compositor)
                 (texture-bytes "depth32float" 2048 2048 1)
                 label)
              shadow (acquire-region-target!
                      compositor "depth32float" 2048 2048
                      label
                      :sample-count 1
                      :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                     js/GPUTextureUsage.TEXTURE_BINDING))
              lease (-> existing
                        (assoc :shadow shadow)
                        (update :bytes + (:bytes shadow)))]
          (swap! (:!region-leases compositor) assoc key lease)
          lease)
        (catch :default error
          (let [rejection-data (merge
                         {:reason :region3d-lease-rejected
                          :region-id region-id :key key
                          :requested-size [qw qh] :requested-shadow? true}
                         (ex-data error))]
            (swap! (:!stats compositor) assoc :last-region-rejection rejection-data)
            {:lease/version 1 :key key :region-id region-id
             :size [qw qh] :bytes 0 :rejected? true :rejection rejection-data})))

      (and existing (not shadow?) (:shadow existing))
      (let [shadow (:shadow existing)
            lease (-> existing
                      (assoc :shadow nil)
                      (update :bytes - (:bytes shadow)))]
        (swap! (:!region-leases compositor) assoc key lease)
        (swap! (:!retired-region-targets compositor) conj shadow)
        lease)

      :else
        (let [pool (:target-pool compositor)
              acquired (atom [])]
          (try
            (let [_ (ensure-target-capacity!
                     pool (region-lease-bytes qw qh shadow?)
                     (str "region3d/" region-id "/lease"))
                  color-msaa
                  (acquire-region-target!
                   compositor "rgba16float" qw qh
                   (str "region3d/" region-id "/color-msaa")
                   :sample-count 4
                   :usage js/GPUTextureUsage.RENDER_ATTACHMENT)
                  _ (swap! acquired conj color-msaa)
                  depth
                  (acquire-region-target!
                   compositor "depth24plus" qw qh
                   (str "region3d/" region-id "/depth")
                   :sample-count 4
                   :usage js/GPUTextureUsage.RENDER_ATTACHMENT)
                  _ (swap! acquired conj depth)
                  resolve
                  (acquire-region-target!
                   compositor "rgba16float" qw qh
                   (str "region3d/" region-id "/resolve")
                   :sample-count 1
                   :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                  js/GPUTextureUsage.TEXTURE_BINDING))
                  _ (swap! acquired conj resolve)
                  shadow
                  (when shadow?
                    (acquire-region-target!
                     compositor "depth32float" 2048 2048
                     (str "region3d/" region-id "/shadow")
                     :sample-count 1
                     :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                    js/GPUTextureUsage.TEXTURE_BINDING)))
                  _ (when shadow (swap! acquired conj shadow))
                  lease {:lease/version 1 :key key :region-id region-id
                         :size [qw qh] :color-msaa color-msaa :depth depth
                         :resolve resolve :shadow shadow
                         :bytes (reduce + (map :bytes @acquired))
                         :rejected? false}]
              (swap! (:!region-leases compositor) assoc key lease)
              lease)
            (catch :default error
              (doseq [target @acquired]
                (destroy-target! pool target))
              (let [rejection-data (merge
                             {:reason :region3d-lease-rejected
                              :region-id region-id :key key
                              :requested-size [qw qh]}
                             (ex-data error))
                    rejected-lease {:lease/version 1 :key key :region-id region-id
                             :size [qw qh] :bytes 0 :rejected? true
                             :rejection rejection-data}]
                (swap! (:!stats compositor) assoc :last-region-rejection rejection-data)
                rejected-lease)))))))

(defn release-region-lease!
  ([compositor region-id]
   (doseq [[key lease] @(:!region-leases compositor)
           :when (= region-id (first key))]
     (release-region-lease! compositor key lease))
   nil)
  ([compositor key lease]
   (swap! (:!region-leases compositor) dissoc key)
   (doseq [target (keep lease [:color-msaa :depth :resolve :shadow])]
     (destroy-target! (:target-pool compositor) target))
   nil))

(defn release-all-region-leases! [compositor]
  (doseq [[key lease] @(:!region-leases compositor)]
    (release-region-lease! compositor key lease))
  (doseq [target @(:!retired-region-targets compositor)]
    (destroy-target! (:target-pool compositor) target))
  (reset! (:!retired-region-targets compositor) [])
  (reset! (:!retiring-region-keys compositor) #{})
  nil)

(defn retire-absent-region-leases!
  "Retire held targets after the last submitted command that could name them.
   Used by the legacy road when closing the final region also removes the
   linear-mode trigger."
  [compositor active-region-ids]
  (let [pending @(:!retiring-region-keys compositor)
        retiring (into []
                       (filter (fn [[key _lease]]
                                 (and (not (contains? active-region-ids
                                                      (first key)))
                                      (not (contains? pending key)))))
                       @(:!region-leases compositor))
        keys (set (map first retiring))]
    (when (seq retiring)
      (swap! (:!retiring-region-keys compositor) into keys)
      (-> (.onSubmittedWorkDone (.-queue ^js (:device compositor)))
          (.then (fn []
                   (doseq [[key lease] retiring]
                     (when (identical? lease
                                       (get @(:!region-leases compositor) key))
                       (release-region-lease! compositor key lease)))
                   (swap! (:!retiring-region-keys compositor)
                          #(apply disj % keys))))
          (.catch (fn [_]
                    (swap! (:!retiring-region-keys compositor)
                           #(apply disj % keys)))))))
  nil)

(defn region-leases-stats [compositor]
  {:owner :compositor/region-leases
   :leases (into {}
                 (map (fn [[key lease]]
                        [key (select-keys lease
                                          [:region-id :size :bytes :rejected?
                                           :desired-key :rung-divisor])]))
                 @(:!region-leases compositor))
   :bytes (reduce + 0 (map :bytes (vals @(:!region-leases compositor))))})

(defn destroy-compositor! [compositor]
  (release-all-region-leases! compositor)
  (destroy-target-pool! (:target-pool compositor))
  nil)

(defn apply-scissor!
  "Set explicit per-draw state. nil means the full attachment; no draw inherits
   a prior neighbor's scissor. Returns false for an empty projected clip so the
   family walker can suppress the draw instead of issuing an invalid zero-area
   WebGPU scissor."
  [pass clip [width height]]
  (let [{:keys [x y w h]} clip
        x (int (max 0 (or x 0)))
        y (int (max 0 (or y 0)))
        w (int (max 0 (min (- width x) (or w width))))
        h (int (max 0 (min (- height y) (or h height))))]
    (if (and (pos? w) (pos? h))
      (do (.setScissorRect ^js pass x y w h)
          true)
      false)))

(defn begin-target-pass! [encoder target load-op]
  (.beginRenderPass
   ^js encoder
   (clj->js {:colorAttachments
             [{:view (:view target)
               :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
               :loadOp load-op :storeOp "store"}]})))

(defn world-transform-scale [world-transforms group-id]
  (let [[a b c d] (or (get-in world-transforms [group-id :affine])
                      [1.0 0.0 0.0 1.0])
        sx (js/Math.sqrt (+ (* a a) (* b b)))
        sy (js/Math.sqrt (+ (* c c) (* d d)))]
    (max sx sy)))

(defn draw-present!
  [compositor encoder scene output-view output-format]
  (let [{:keys [sampler present-layout present-pipelines]} (:pipelines compositor)
        present-pipeline (get present-pipelines output-format)]
    (when-not present-pipeline
      (throw (ex-info "No presentation pipeline for output format"
                      {:actual output-format :available (keys present-pipelines)})))
    (let [bind-group (.createBindGroup
                      ^js (:device compositor)
                      (clj->js {:layout present-layout
                                :entries [{:binding 0 :resource sampler}
                                          {:binding 1 :resource (:view scene)}]}))
          pass (.beginRenderPass
                ^js encoder
                (clj->js {:colorAttachments
                          [{:view output-view
                            :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
                            :loadOp "clear" :storeOp "store"}]}))]
      (apply-scissor! pass nil [(:width scene) (:height scene)])
      (.setPipeline pass present-pipeline)
      (.setBindGroup pass 0 bind-group)
      (.draw pass 3 1 0 0)
      (.end pass))))

(defn release-after-submit!
  [compositor acquired transient-buffers stale-region-leases]
  (bump-frame-epoch! (:target-pool compositor))
  (doseq [target acquired]
    (release-target! (:target-pool compositor) target))
  (let [retired @(:!retired-region-targets compositor)]
    (reset! (:!retired-region-targets compositor) [])
    (-> (.onSubmittedWorkDone (.-queue ^js (:device compositor)))
      (.then (fn []
               (doseq [buffer transient-buffers] (.destroy ^js buffer))
               ;; Same identity guard as retire-absent-region-leases!: a later
               ;; frame may have eagerly released this lease already.
               (doseq [[key lease] stale-region-leases]
                 (when (identical? lease
                                   (get @(:!region-leases compositor) key))
                   (release-region-lease! compositor key lease)))
               (doseq [target retired]
                 (destroy-target! (:target-pool compositor) target))))
      (.catch (fn [_] nil)))))

(defn active-region-leases!
  "Acquire this frame's region leases from the binding owner's desired rows,
   retiring every lease those rows no longer name. Records the rung stats
   and the frame's lease activity on the compositor stats."
  [compositor binding-owner]
  (let [regions (if binding-owner
                  (region-bindings/desired-rows binding-owner)
                  [])
        desired-region-ids (into #{} (map :region/id) regions)
        activity (atom {:leases-retired 0 :region-binding-updates 0
                        :leases-acquired 0 :region-rungs-worn 0
                        :region-rung-recoveries 0})
        note! (fn [key] (swap! activity update key inc))
        _ (swap! (:!stats compositor) dissoc :last-region-rejection)
        ;; Old-generation leases die BEFORE the new generation is acquired:
        ;; only submitted (or abandoned, never-submitted) encoders can still
        ;; name them, and destroy after submit defers deallocation, so the two
        ;; generations never bill the pool budget at once — a zoom gesture
        ;; that crosses lease quanta otherwise holds both until a frame
        ;; succeeds, which budget rejection can make unreachable.
        _ (doseq [[key lease] @(:!region-leases compositor)
                  :when (not (contains? desired-region-ids (first key)))]
            (note! :leases-retired)
            (release-region-lease! compositor key lease))
        {:keys [active rung-stats]}
        (reduce
         (fn [{:keys [active rung-stats]}
              {region-id :region/id [width height] :lease-size
               shadow? :shadow?}]
           (let [prior-physical (region-lease compositor region-id)
                 pool (:target-pool compositor)
                 pool-state @(:!state pool)
                 grant (region-rungs/grant
                        {:region/id region-id
                         :desired-size [width height]
                         :shadow? shadow?
                         :held-lease prior-physical
                         :reserved-bytes (reserved-bytes pool-state)
                         :budget-cap-bytes (:budget-cap-bytes pool)
                         :quantize quantize-region-size
                         :lease-bytes region-lease-bytes})
                 selected-key (or (:granted-key grant) (:desired-key grant))
                 [_ granted-width granted-height] selected-key
                 retiring (into []
                                (filter (fn [[key _lease]]
                                          (and (= region-id (first key))
                                               (not= selected-key key))))
                                @(:!region-leases compositor))
                 _ (doseq [[key lease] retiring]
                     (note! :leases-retired)
                     (release-region-lease! compositor key lease))
                 prior (when binding-owner
                         (region-bindings/lease binding-owner region-id))
                 update? (or (nil? prior)
                             (:rejected? prior)
                             (not= selected-key (:key prior))
                             (not= (boolean shadow?)
                                   (boolean (:shadow prior))))
                 acquired (acquire-region-lease!
                           compositor region-id granted-width granted-height shadow?)
                 lease (assoc acquired
                              :desired-key (:desired-key grant)
                              :rung-divisor (:rung-divisor grant)
                              :rung-stats (:rung-stats grant))
                 _ (when-not (:rejected? lease)
                     (swap! (:!region-leases compositor)
                            assoc selected-key lease))
                 prior-divisor (:rung-divisor prior)
                 granted-divisor (:rung-divisor grant)]
             (when update?
               (note! :region-binding-updates)
               (when-not (:rejected? lease)
                 (note! :leases-acquired)
                 (when (> granted-divisor 1)
                   (note! :region-rungs-worn))
                 (when (and prior-divisor
                            (< granted-divisor prior-divisor))
                   (note! :region-rung-recoveries))))
             (when binding-owner
               (region-bindings/record-lease! binding-owner region-id lease))
             {:active (assoc active region-id lease)
              :rung-stats (cond-> rung-stats
                               (:rung-stats grant)
                               (conj (:rung-stats grant)))}))
         {:active {} :rung-stats []}
         regions)
        active-keys (into #{} (keep (fn [[_ lease]]
                                      (when-not (:rejected? lease)
                                        (:key lease)))) active)
        stale (into []
                    (remove (fn [[key _lease]] (contains? active-keys key)))
                    @(:!region-leases compositor))]
    (swap! (:!stats compositor) assoc
           :region-rung-stats rung-stats
           :lease-activity @activity)
    {:active active :active-keys active-keys :stale stale
     :rung-stats rung-stats :lease-activity @activity}))

(defn copy-present!
  "Legacy COPY-PRESENT executor primitive used by the effectless verifier row."
  [device encoder source-texture swap-texture width height]
  (.copyTextureToTexture ^js encoder
                         (clj->js {:texture source-texture})
                         (clj->js {:texture swap-texture})
                         (clj->js {:width width :height height}))
  true)

(defn- strip-padded-rows [mapped width height padded-bytes-per-row]
  (let [row-bytes (* width 4)
        output (js/Uint8Array. (* row-bytes height))
        source (js/Uint8Array. mapped)]
    (dotimes [row height]
      (.set output
            (.subarray source (* row padded-bytes-per-row)
                       (+ (* row padded-bytes-per-row) row-bytes))
            (* row row-bytes)))
    output))

(defn- unpremultiply! [rgba]
  (loop [index 0]
    (when (< index (.-length rgba))
      (let [alpha (aget rgba (+ index 3))]
        (when (pos? alpha)
          (let [scale (/ 255.0 alpha)]
            (aset rgba index (min 255 (js/Math.round (* (aget rgba index) scale))))
            (aset rgba (+ index 1)
                  (min 255 (js/Math.round (* (aget rgba (+ index 1)) scale))))
            (aset rgba (+ index 2)
                  (min 255 (js/Math.round (* (aget rgba (+ index 2)) scale)))))))
      (recur (+ index 4))))
  rgba)

(defn- png-bytes! [rgba width height]
  (let [canvas (js/OffscreenCanvas. width height)
        context (.getContext canvas "2d")
        image-data (js/ImageData. (js/Uint8ClampedArray. (.-buffer rgba))
                                  width height)]
    (.putImageData context image-data 0 0)
    (-> (.convertToBlob canvas #js {:type "image/png"})
        (.then #(.arrayBuffer %))
        (.then #(js/Uint8Array. %)))))

(defn compositor-stats [compositor]
  (assoc @(:!stats compositor)
         :pool (target-pool-stats (:target-pool compositor))
         :region-leases (region-leases-stats compositor)))
