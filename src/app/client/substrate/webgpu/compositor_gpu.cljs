(ns app.client.substrate.webgpu.compositor-gpu
  "W4's generic WebGPU compositor capability.

   The frame graph names resources and producer edges; this namespace owns the
   recycling target pool, linear group/mask/blur composition, exactly-one
   presentation transfer, per-draw scissor state, and asynchronous raster
   readback. Family pipelines arrive through a lazy variant-layer builder so
   textures/registries/instance bytes remain owned by their existing systems."
  (:require [app.client.substrate.region-rungs :as region-rungs]
            [app.client.substrate.webgpu.region-bindings :as region-bindings]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]))

(def target-pool-version 2)
(def compositor-version 2)
(def region-lease-quant 256)
(def region-lease-max 4096)
;; The W4 backdrop road at zoom peaks at six live viewport-sized rgba16
;; targets — scene, group content, group output, snapshot, and the separable
;; blur pair — plus the downsample chain and any mask target. Keep that road
;; inside the owned budget at a 3840x2160 physical canvas (~66MB per target);
;; the pool still refuses larger live sets by name, reclaims stale free cache
;; under pressure, and the blur road degrades on refusal instead of dying.
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

(def composite-fragment-shader
  "@group(0) @binding(0) var linear_sampler: sampler;
   @group(0) @binding(1) var group_texture: texture_2d<f32>;
   @group(0) @binding(2) var mask_texture: texture_2d<f32>;
   @group(0) @binding(3) var original_texture: texture_2d<f32>;
   @group(0) @binding(4) var blurred_texture: texture_2d<f32>;
   struct Params { opacity: f32, mask_enabled: f32,
                   backdrop_enabled: f32, padding: f32, };
   @group(0) @binding(5) var<uniform> params: Params;
   @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
     var group = textureSample(group_texture, linear_sampler, uv) * params.opacity;
     if (params.mask_enabled > 0.5) {
       group = group * textureSample(mask_texture, linear_sampler, uv).a;
     }
     if (params.backdrop_enabled > 0.5) {
       let original = textureSample(original_texture, linear_sampler, uv);
       let blurred = textureSample(blurred_texture, linear_sampler, uv);
       let backdrop = mix(original, blurred, group.a);
       return group + backdrop * (1.0 - group.a);
     }
     return group;
   }")

(def blur-fragment-shader
  "@group(0) @binding(0) var linear_sampler: sampler;
   @group(0) @binding(1) var source_texture: texture_2d<f32>;
   struct BlurParams { texel: vec2<f32>, direction: vec2<f32>, };
   @group(0) @binding(2) var<uniform> params: BlurParams;
   @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
     let delta = params.texel * params.direction;
     var color = textureSample(source_texture, linear_sampler, uv) * 0.227027;
     color += textureSample(source_texture, linear_sampler, uv + delta * 1.384615) * 0.316216;
     color += textureSample(source_texture, linear_sampler, uv - delta * 1.384615) * 0.316216;
     color += textureSample(source_texture, linear_sampler, uv + delta * 3.230769) * 0.070270;
     color += textureSample(source_texture, linear_sampler, uv - delta * 3.230769) * 0.070270;
     return color;
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

(def clip-mask-fragment-shader
  "struct ClipPoints { p0: vec2<f32>, p1: vec2<f32>,
                       p2: vec2<f32>, p3: vec2<f32>, };
   @group(0) @binding(0) var<uniform> clip: ClipPoints;
   fn edge(a: vec2<f32>, b: vec2<f32>, p: vec2<f32>) -> f32 {
     let ab = b - a;
     let ap = p - a;
     return ab.x * ap.y - ab.y * ap.x;
   }
   @fragment fn main(@builtin(position) position: vec4<f32>)
       -> @location(0) vec4<f32> {
     let p = position.xy;
     let e0 = edge(clip.p0, clip.p1, p);
     let e1 = edge(clip.p1, clip.p2, p);
     let e2 = edge(clip.p2, clip.p3, p);
     let e3 = edge(clip.p3, clip.p0, p);
     let nonnegative = e0 >= 0.0 && e1 >= 0.0 && e2 >= 0.0 && e3 >= 0.0;
     let nonpositive = e0 <= 0.0 && e1 <= 0.0 && e2 <= 0.0 && e3 <= 0.0;
     let coverage = select(0.0, 1.0, nonnegative || nonpositive);
     return vec4<f32>(coverage, coverage, coverage, coverage);
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
  [device tracker & {:keys [budget-cap-bytes]
                     :or {budget-cap-bytes default-pool-budget-bytes}}]
  {:device device :tracker tracker :budget-cap-bytes budget-cap-bytes
   :!state (atom {:free {} :leased {} :allocations 0 :reuses 0
                  :destroyed 0 :reserved-bytes 0 :high-water-leased 0
                  :epoch 0 :refusals []})})

(defn- target-key [format width height sample-count]
  [format (int width) (int height) (max 1 (or sample-count 1))])

(defn- reclaim-free-targets!
  "Discard the recoverable cache before refusing a new target. A viewport
   resize otherwise strands the old dimensions inside the fixed pool budget."
  [pool]
  (let [state @(:!state pool)
        targets (vec (mapcat val (:free state)))
        reclaimed-bytes (reduce + 0 (map :bytes targets))]
    (when (seq targets)
      (swap! (:!state pool)
             (fn [current]
               (-> current
                   (assoc :free {})
                   (update :reserved-bytes - reclaimed-bytes)
                   (update :destroyed + (count targets)))))
      (doseq [target targets]
        (gpu-budget/destroy-resource! (:tracker pool) (:texture target)
                                      :reason :frame-target-pool-reclaim)
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
      (let [stale-ids (into #{} (map :target/id) targets)
            reclaimed-bytes (reduce + 0 (map :bytes targets))]
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
                       (update :reserved-bytes - reclaimed-bytes)
                       (update :destroyed + (count targets))))))
        (doseq [target targets]
          (gpu-budget/destroy-resource! (:tracker pool) (:texture target)
                                        :reason :frame-target-pool-reclaim)
          (.destroy ^js (:texture target)))))
    (count targets)))

(defn- ensure-target-capacity!
  [pool bytes label]
  (let [state @(:!state pool)]
    (when (> (+ (:reserved-bytes state) bytes) (:budget-cap-bytes pool))
      (let [receipt {:reason :frame-target-budget-exceeded
                     :requested-bytes bytes
                     :reserved-bytes (:reserved-bytes state)
                     :budget-cap-bytes (:budget-cap-bytes pool)
                     :requested-by label}]
        (swap! (:!state pool) update :refusals conj receipt)
        (throw (ex-info "Frame target pool budget exceeded" receipt))))))

(defn- create-target!
  [pool format width height label sample-count usage reclaim-free?]
  (let [sample-count (max 1 (or sample-count 1))
        bytes (texture-bytes format width height sample-count)
        _ (when (and reclaim-free?
                     (> (+ (:reserved-bytes @(:!state pool)) bytes)
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
      (gpu-budget/register-texture! (:tracker pool) texture label
                                    :format format :width width :height height
                                    :details {:sample-count sample-count
                                              :pool-bytes bytes})
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
                       (update :reserved-bytes + (:bytes target))
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

(defn- destroy-target! [pool target reason]
  (when target
    (let [target-id (:target/id target)
          key (target-key (:format target) (:width target) (:height target)
                          (:sample-count target))
          ;; Idempotent like release-target!: three async release roads can
          ;; name the same lease target, and a second decrement would corrupt
          ;; reserved-bytes downward — the budget then over-admits.
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
                         (update :reserved-bytes - (:bytes target))
                         (update :destroyed inc)))))))
      (when @removed?
        (gpu-budget/destroy-resource! (:tracker pool) (:texture target)
                                      :reason reason)
        (.destroy ^js (:texture target)))))
  nil)

(defn destroy-target-pool! [pool]
  (let [state @(:!state pool)
        targets (concat (vals (:leased state)) (mapcat val (:free state)))]
    (doseq [target targets]
      (gpu-budget/destroy-resource! (:tracker pool) (:texture target)
                                    :reason :frame-target-pool-destroy)
      (.destroy ^js (:texture target)))
    (reset! (:!state pool)
            {:free {} :leased {} :allocations (:allocations state)
             :reuses (:reuses state) :destroyed (+ (:destroyed state)
                                                   (count targets))
             :reserved-bytes 0 :high-water-leased (:high-water-leased state)
             :refusals (:refusals state)})
    nil))

(defn target-pool-receipt [pool]
  (let [state @(:!state pool)]
    {:target-pool/version target-pool-version
     :budget-owner :frame-runtime/target-pool
     :budget-cap-bytes (:budget-cap-bytes pool)
     :allocations (:allocations state) :reuses (:reuses state)
     :leased (count (:leased state))
     :free (reduce + 0 (map count (vals (:free state))))
     :reserved-bytes (:reserved-bytes state)
     :high-water-leased (:high-water-leased state)
     :destroyed (:destroyed state) :refusals (:refusals state)}))

(defn- blend-state []
  {:color {:srcFactor "one" :dstFactor "one-minus-src-alpha"}
   :alpha {:srcFactor "one" :dstFactor "one-minus-src-alpha"}})

(defn- shader-module [device code]
  (.createShaderModule ^js device (clj->js {:code code})))

(defn- create-composite-pipeline [device bind-layout blend?]
  (.createRenderPipeline
   ^js device
   (clj->js
    {:layout (.createPipelineLayout
              ^js device (clj->js {:bindGroupLayouts [bind-layout]}))
     :vertex {:module (shader-module device full-screen-vertex-shader)
              :entryPoint "main"}
     :fragment {:module (shader-module device composite-fragment-shader)
                :entryPoint "main"
                :targets [(cond-> {:format "rgba16float"}
                            blend? (assoc :blend (blend-state)))]}
     :primitive {:topology "triangle-list"}})))

(defn- create-pipelines! [device output-format]
  (let [sampler (.createSampler ^js device
                                (clj->js {:minFilter "linear" :magFilter "linear"
                                          :addressModeU "clamp-to-edge"
                                          :addressModeV "clamp-to-edge"}))
        composite-layout
        (.createBindGroupLayout
         ^js device
         (clj->js
          {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                      :sampler {:type "filtering"}}
                     {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                      :texture {:sampleType "float"}}
                     {:binding 2 :visibility js/GPUShaderStage.FRAGMENT
                      :texture {:sampleType "float"}}
                     {:binding 3 :visibility js/GPUShaderStage.FRAGMENT
                      :texture {:sampleType "float"}}
                     {:binding 4 :visibility js/GPUShaderStage.FRAGMENT
                      :texture {:sampleType "float"}}
                     {:binding 5 :visibility js/GPUShaderStage.FRAGMENT
                      :buffer {:type "uniform"}}]}))
        blur-layout
        (.createBindGroupLayout
         ^js device
         (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                              :sampler {:type "filtering"}}
                             {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                              :texture {:sampleType "float"}}
                             {:binding 2 :visibility js/GPUShaderStage.FRAGMENT
                              :buffer {:type "uniform"}}]}))
        present-layout
        (.createBindGroupLayout
         ^js device
         (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                              :sampler {:type "filtering"}}
                             {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                              :texture {:sampleType "float"}}]}))
        clip-mask-layout
        (.createBindGroupLayout
         ^js device
         (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                              :buffer {:type "uniform"}}]}))
        blur-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (.createPipelineLayout
                            ^js device (clj->js {:bindGroupLayouts [blur-layout]}))
                   :vertex {:module (shader-module device full-screen-vertex-shader)
                            :entryPoint "main"}
                   :fragment {:module (shader-module device blur-fragment-shader)
                              :entryPoint "main"
                              :targets [{:format "rgba16float"}]}
                   :primitive {:topology "triangle-list"}}))
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
                     :primitive {:topology "triangle-list"}})))
        clip-mask-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (.createPipelineLayout
                            ^js device
                            (clj->js {:bindGroupLayouts [clip-mask-layout]}))
                   :vertex {:module (shader-module device full-screen-vertex-shader)
                            :entryPoint "main"}
                   :fragment {:module (shader-module device clip-mask-fragment-shader)
                              :entryPoint "main"
                              :targets [{:format "rgba16float"}]}
                   :primitive {:topology "triangle-list"}}))]
    {:sampler sampler :composite-layout composite-layout
     :composite-pipeline (create-composite-pipeline device composite-layout true)
     :backdrop-pipeline (create-composite-pipeline device composite-layout false)
     :blur-layout blur-layout :blur-pipeline blur-pipeline
     :present-layout present-layout
     :clip-mask-layout clip-mask-layout :clip-mask-pipeline clip-mask-pipeline
     :present-pipelines (into {} (map (fn [format]
                                       [format (present-pipeline format)]))
                              (distinct [output-format "rgba8unorm"]))
     :output-format output-format}))

(defn create-compositor!
  [device output-format tracker & {:keys [budget-cap-bytes]}]
  {:compositor/version compositor-version :device device
   :format output-format :tracker tracker
   :target-pool (create-target-pool device tracker
                                   :budget-cap-bytes
                                   (or budget-cap-bytes
                                       default-pool-budget-bytes))
   :pipelines (create-pipelines! device output-format)
   :!region-leases (atom {})
   :!retired-region-targets (atom [])
   :!retiring-region-keys (atom #{})
   :!receipt (atom {})})

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
   existing lease-refusal path owns the combined-set failure instead."
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
  "Acquire or reuse one compositor-owned held lease. Refusal is returned as
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
          (let [receipt (merge
                         {:reason :region3d-lease-refused
                          :region-id region-id :key key
                          :requested-size [qw qh] :requested-shadow? true}
                         (ex-data error))]
            (swap! (:!receipt compositor) assoc :last-region-refusal receipt)
            {:lease/version 1 :key key :region-id region-id
             :size [qw qh] :bytes 0 :refused? true :refusal receipt})))

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
                         :refused? false}]
              (swap! (:!region-leases compositor) assoc key lease)
              lease)
            (catch :default error
              (doseq [target @acquired]
                (destroy-target! pool target :region3d-partial-refusal))
              (let [receipt (merge
                             {:reason :region3d-lease-refused
                              :region-id region-id :key key
                              :requested-size [qw qh]}
                             (ex-data error))
                    refusal {:lease/version 1 :key key :region-id region-id
                             :size [qw qh] :bytes 0 :refused? true
                             :refusal receipt}]
                (swap! (:!receipt compositor) assoc :last-region-refusal receipt)
                refusal)))))))

(defn release-region-lease!
  ([compositor region-id]
   (doseq [[key lease] @(:!region-leases compositor)
           :when (= region-id (first key))]
     (release-region-lease! compositor key lease))
   nil)
  ([compositor key lease]
   (swap! (:!region-leases compositor) dissoc key)
   (doseq [target (keep lease [:color-msaa :depth :resolve :shadow])]
     (destroy-target! (:target-pool compositor) target
                      :region3d-lease-release))
   nil))

(defn release-all-region-leases! [compositor]
  (doseq [[key lease] @(:!region-leases compositor)]
    (release-region-lease! compositor key lease))
  (doseq [target @(:!retired-region-targets compositor)]
    (destroy-target! (:target-pool compositor) target
                     :region3d-retired-release))
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

(defn region-leases-receipt [compositor]
  {:owner :compositor/region-leases
   :leases (into {}
                 (map (fn [[key lease]]
                        [key (select-keys lease
                                          [:region-id :size :bytes :refused?
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

(defn- mapped-uniform! [device floats]
  (let [size (* 4 (max 4 (count floats)))
        buffer (.createBuffer ^js device
                              (clj->js {:size size
                                        :usage js/GPUBufferUsage.UNIFORM
                                        :mappedAtCreation true}))
        data (js/Float32Array. (.getMappedRange buffer))]
    (doseq [[index value] (map-indexed vector floats)]
      (aset data index (double value)))
    (.unmap buffer)
    buffer))

(defn begin-target-pass! [encoder target load-op]
  (.beginRenderPass
   ^js encoder
   (clj->js {:colorAttachments
             [{:view (:view target)
               :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
               :loadOp load-op :storeOp "store"}]})))

(defn- draw-composite!
  [compositor encoder target group-target mask-target original-target
   blurred-target opacity backdrop? transient-buffers load-op]
  (let [{:keys [sampler composite-layout composite-pipeline backdrop-pipeline]}
        (:pipelines compositor)
        mask-enabled? (boolean mask-target)
        mask-target (or mask-target group-target)
        original-target (or original-target group-target)
        blurred-target (or blurred-target group-target)
        params (mapped-uniform! (:device compositor)
                                [opacity (if mask-enabled? 1.0 0.0)
                                 (if backdrop? 1.0 0.0) 0.0])
        _ (swap! transient-buffers conj params)
        bind-group
        (.createBindGroup
         ^js (:device compositor)
         (clj->js {:layout composite-layout
                   :entries [{:binding 0 :resource sampler}
                             {:binding 1 :resource (:view group-target)}
                             {:binding 2 :resource (:view mask-target)}
                             {:binding 3 :resource (:view original-target)}
                             {:binding 4 :resource (:view blurred-target)}
                             {:binding 5 :resource {:buffer params}}]}))
        pass (begin-target-pass! encoder target load-op)]
    (apply-scissor! pass nil [(:width target) (:height target)])
    (.setPipeline pass (if backdrop? backdrop-pipeline composite-pipeline))
    (.setBindGroup pass 0 bind-group)
    (.draw pass 3 1 0 0)
    (.end pass)))

(defn- blur-pass!
  [compositor encoder source target direction transient-buffers]
  (let [{:keys [sampler blur-layout blur-pipeline]} (:pipelines compositor)
        params (mapped-uniform! (:device compositor)
                                [(/ 1.0 (:width source))
                                 (/ 1.0 (:height source))
                                 (first direction) (second direction)])
        _ (swap! transient-buffers conj params)
        bind-group (.createBindGroup
                    ^js (:device compositor)
                    (clj->js {:layout blur-layout
                              :entries [{:binding 0 :resource sampler}
                                        {:binding 1 :resource (:view source)}
                                        {:binding 2 :resource {:buffer params}}]}))
        pass (begin-target-pass! encoder target "clear")]
    (apply-scissor! pass nil [(:width target) (:height target)])
    (.setPipeline pass blur-pipeline)
    (.setBindGroup pass 0 bind-group)
    (.draw pass 3 1 0 0)
    (.end pass)))

(defn- try-acquire-target!
  "Acquire on the effect road, degrading on budget refusal instead of killing
   the frame. The pool has already written the refusal receipt; nil lets the
   caller keep the sharpest source it still holds."
  [compositor width height label]
  (try
    (acquire-target! (:target-pool compositor) "rgba16float" width height
                     label :reclaim-free? false)
    (catch :default error
      (when-not (= :frame-target-budget-exceeded (:reason (ex-data error)))
        (throw error)))))

(defn- blur-target!
  [compositor encoder source projection acquired transient-buffers label
   release-source?]
  (loop [source source level 0 source-owned? release-source?]
    (if (> level (:downsample-levels projection))
      source
      (let [width (max 1 (quot (:width source) (if (zero? level) 1 2)))
            height (max 1 (quot (:height source) (if (zero? level) 1 2)))
            ;; The settled 9-tap weights have an approximately four-pixel
            ;; support at scale 1. Carry the projected radius through each
            ;; downsample level instead of quantizing magnitude to level count.
            sample-scale (/ (:radius-px projection)
                            (* 4.0 (js/Math.pow 2.0 level)))
            horizontal (try-acquire-target! compositor width height
                                            (str label "/h" level))]
        (if (nil? horizontal)
          source
          (do
            (swap! acquired conj horizontal)
            (blur-pass! compositor encoder source horizontal
                        [sample-scale 0.0] transient-buffers)
            (when source-owned?
              (release-target! (:target-pool compositor) source))
            (let [vertical (try-acquire-target! compositor width height
                                                (str label "/v" level))]
              (if (nil? vertical)
                horizontal
                (do
                  (swap! acquired conj vertical)
                  (blur-pass! compositor encoder horizontal vertical
                              [0.0 sample-scale] transient-buffers)
                  (release-target! (:target-pool compositor) horizontal)
                  (recur vertical (inc level) true))))))))))

(defn- clear-target! [encoder target]
  (let [pass (begin-target-pass! encoder target "clear")]
    (.end pass)))

(defn- draw-clip-mask!
  [compositor encoder target points transient-buffers]
  (let [{:keys [clip-mask-layout clip-mask-pipeline]} (:pipelines compositor)
        points (vec points)]
    (when-not (= 4 (count points))
      (throw (ex-info "General clip mask requires four perimeter points"
                      {:points points})))
    (let [params (mapped-uniform! (:device compositor)
                                  (vec (mapcat identity points)))
          _ (swap! transient-buffers conj params)
          bind-group (.createBindGroup
                      ^js (:device compositor)
                      (clj->js {:layout clip-mask-layout
                                :entries [{:binding 0
                                           :resource {:buffer params}}]}))
          pass (begin-target-pass! encoder target "clear")]
      (apply-scissor! pass nil [(:width target) (:height target)])
      (.setPipeline pass clip-mask-pipeline)
      (.setBindGroup pass 0 bind-group)
      (.draw pass 3 1 0 0)
      (.end pass))))

(defn effective-scale [effective-transforms container-id]
  (let [[a b c d] (or (get-in effective-transforms [container-id :affine])
                      [1.0 0.0 0.0 1.0])
        sx (js/Math.sqrt (+ (* a a) (* b b)))
        sy (js/Math.sqrt (+ (* c c) (* d d)))]
    (max sx sy)))

(def blur-algorithm-version :gaussian-9tap-downsample-v1)
(def default-max-blur-px 64.0)

(defn- finite-number? [x]
  (and (number? x) (js/Number.isFinite x)))

(defn- validate-blur! [kind blur]
  (when-not (map? blur)
    (throw (ex-info "Blur declaration must be a map"
                    {:effect kind :value blur})))
  (let [unknown (seq (remove #{:radius-world :max-px :algorithm-version}
                             (keys blur)))
        radius (:radius-world blur)
        max-px (get blur :max-px default-max-blur-px)
        algorithm (:algorithm-version blur)]
    (when unknown
      (throw (ex-info "Blur declaration has unknown fields"
                      {:effect kind :unknown (vec unknown)})))
    (when-not (and (finite-number? radius) (not (neg? radius)))
      (throw (ex-info "Blur radius must be a finite non-negative world value"
                      {:effect kind :radius-world radius})))
    (when-not (and (finite-number? max-px) (pos? max-px))
      (throw (ex-info "Blur max-px must be finite and positive"
                      {:effect kind :max-px max-px})))
    (when-not (= blur-algorithm-version algorithm)
      (throw (ex-info "Blur algorithm-version is absent or unsupported"
                      {:effect kind :algorithm-version algorithm
                       :required blur-algorithm-version})))
    (assoc blur :max-px (double max-px)
                :radius-world (double radius))))

(defn projected-blur
  "Project a world-space blur declaration to pixels and name its clamp regime."
  [blur effective-scale camera-zoom]
  (let [{:keys [radius-world max-px algorithm-version]}
        (validate-blur! :blur blur)
        raw (* radius-world (double effective-scale) (double camera-zoom))
        px (min max-px raw)]
    {:radius-px px
     :raw-radius-px raw
     :clamped? (> raw max-px)
     :regime (if (> raw max-px) :max-px-clamped :projected-world-radius)
     :algorithm-version algorithm-version
     :downsample-levels
     (loop [radius px levels 0]
       (if (and (> radius 8.0) (< levels 3))
         (recur (/ radius 2.0) (inc levels))
         levels))}))

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
                 (destroy-target! (:target-pool compositor) target
                                  :region3d-shadow-mode-change))))
      (.catch (fn [_] nil)))))

(defn active-region-leases!
  "Acquire this frame's region leases from the binding owner's desired rows,
   retiring every lease those rows no longer name. Records the rung receipts
   and the frame's lease activity on the compositor receipt."
  [compositor binding-owner]
  (let [regions (if binding-owner
                  (region-bindings/desired-rows binding-owner)
                  [])
        desired-region-ids (into #{} (map :region/id) regions)
        activity (atom {:leases-retired 0 :region-binding-updates 0
                        :leases-acquired 0 :region-rungs-worn 0
                        :region-rung-recoveries 0})
        note! (fn [key] (swap! activity update key inc))
        _ (swap! (:!receipt compositor) dissoc :last-region-refusal)
        ;; Old-generation leases die BEFORE the new generation is acquired:
        ;; only submitted (or abandoned, never-submitted) encoders can still
        ;; name them, and destroy after submit defers deallocation, so the two
        ;; generations never bill the pool budget at once — a zoom gesture
        ;; that crosses lease quanta otherwise holds both until a frame
        ;; succeeds, which budget refusal can make unreachable.
        _ (doseq [[key lease] @(:!region-leases compositor)
                  :when (not (contains? desired-region-ids (first key)))]
            (note! :leases-retired)
            (release-region-lease! compositor key lease))
        {:keys [active rung-receipts]}
        (reduce
         (fn [{:keys [active rung-receipts]}
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
                         :reserved-bytes (:reserved-bytes pool-state)
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
                             (:refused? prior)
                             (not= selected-key (:key prior))
                             (not= (boolean shadow?)
                                   (boolean (:shadow prior))))
                 acquired (acquire-region-lease!
                           compositor region-id granted-width granted-height shadow?)
                 lease (assoc acquired
                              :desired-key (:desired-key grant)
                              :rung-divisor (:rung-divisor grant)
                              :rung-receipt (:rung-receipt grant))
                 _ (when-not (:refused? lease)
                     (swap! (:!region-leases compositor)
                            assoc selected-key lease))
                 prior-divisor (:rung-divisor prior)
                 granted-divisor (:rung-divisor grant)]
             (when update?
               (note! :region-binding-updates)
               (when-not (:refused? lease)
                 (note! :leases-acquired)
                 (when (> granted-divisor 1)
                   (note! :region-rungs-worn))
                 (when (and prior-divisor
                            (< granted-divisor prior-divisor))
                   (note! :region-rung-recoveries))))
             (when binding-owner
               (region-bindings/record-lease! binding-owner region-id lease))
             {:active (assoc active region-id lease)
              :rung-receipts (cond-> rung-receipts
                               (:rung-receipt grant)
                               (conj (:rung-receipt grant)))}))
         {:active {} :rung-receipts []}
         regions)
        active-keys (into #{} (keep (fn [[_ lease]]
                                      (when-not (:refused? lease)
                                        (:key lease)))) active)
        stale (into []
                    (remove (fn [[key _lease]] (contains? active-keys key)))
                    @(:!region-leases compositor))]
    (swap! (:!receipt compositor) assoc
           :region-rung-receipts rung-receipts
           :lease-activity @activity)
    {:active active :active-keys active-keys :stale stale
     :rung-receipts rung-receipts :lease-activity @activity}))

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

(defn compositor-receipt [compositor]
  (assoc @(:!receipt compositor)
         :pool (target-pool-receipt (:target-pool compositor))
         :region-leases (region-leases-receipt compositor)))
