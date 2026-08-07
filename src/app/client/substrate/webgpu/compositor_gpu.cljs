(ns app.client.substrate.webgpu.compositor-gpu
  "W4's generic WebGPU compositor capability.

   The frame graph names resources and producer edges; this namespace owns the
   recycling target pool, linear group/mask/blur composition, exactly-one
   presentation transfer, per-draw scissor state, and asynchronous raster
   readback. Family pipelines arrive through a lazy variant-layer builder so
   textures/registries/instance bytes remain owned by their existing systems."
  (:require [app.client.substrate.frame-effects :as frame-effects]
            [app.client.substrate.frame-graph :as frame-graph]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]))

(def target-pool-version 1)
(def compositor-version 1)
;; The named W4 backdrop road peaks at five live viewport-sized rgba16 targets
;; (scene, group, snapshot, and the separable blur pair). Keep that road inside
;; the owned budget at a 3840x2160 physical canvas; the pool still refuses
;; larger live sets by name and reclaims free cache before doing so.
(def default-pool-budget-bytes (* 384 1024 1024))

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

(defn- texture-bytes [format width height]
  (* width height (if (= format "rgba16float") 8 4)))

(defn create-target-pool
  [device tracker & {:keys [budget-cap-bytes]
                     :or {budget-cap-bytes default-pool-budget-bytes}}]
  {:device device :tracker tracker :budget-cap-bytes budget-cap-bytes
   :!state (atom {:free {} :leased {} :allocations 0 :reuses 0
                  :destroyed 0 :reserved-bytes 0 :high-water-leased 0
                  :refusals []})})

(defn- target-key [format width height]
  [format (int width) (int height)])

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

(defn- create-target! [pool format width height label]
  (let [bytes (texture-bytes format width height)
        _ (when (and (empty? (:leased @(:!state pool)))
                     (> (+ (:reserved-bytes @(:!state pool)) bytes)
                        (:budget-cap-bytes pool)))
            (reclaim-free-targets! pool))
        state @(:!state pool)]
    (when (> (+ (:reserved-bytes state) bytes) (:budget-cap-bytes pool))
      (swap! (:!state pool) update :refusals conj
             {:reason :frame-target-budget-exceeded :requested-bytes bytes
              :reserved-bytes (:reserved-bytes state)
              :budget-cap-bytes (:budget-cap-bytes pool)
              :requested-by label})
      (throw (ex-info "Frame target pool budget exceeded"
                      {:reason :frame-target-budget-exceeded
                       :requested-bytes bytes
                       :reserved-bytes (:reserved-bytes state)
                       :budget-cap-bytes (:budget-cap-bytes pool)
                       :requested-by label})))
    (let [texture (.createTexture
                   ^js (:device pool)
                   (clj->js {:label label
                             :size {:width width :height height
                                    :depthOrArrayLayers 1}
                             :format format
                             :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                            js/GPUTextureUsage.TEXTURE_BINDING
                                            js/GPUTextureUsage.COPY_SRC
                                            js/GPUTextureUsage.COPY_DST)}))
          target {:target/id (random-uuid) :texture texture
                  :view (.createView texture) :format format
                  :width width :height height :bytes bytes :label label}]
      (gpu-budget/register-texture! (:tracker pool) texture label
                                    :format format :width width :height height)
      target)))

(defn acquire-target!
  [pool format width height label]
  (let [width (max 1 (int width))
        height (max 1 (int height))
        key (target-key format width height)
        state @(:!state pool)
        ;; An unmatched first acquisition marks a frame/viewport boundary.
        ;; Reclaim there, while no unsubmitted encoder can still name a free
        ;; target. Same-frame releases remain reusable but never destroyable.
        _ (when (and (empty? (:leased state))
                     (nil? (first (get-in state [:free key])))
                     (seq (:free state)))
            (reclaim-free-targets! pool))
        target (first (get-in @(:!state pool) [:free key]))]
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
      (let [target (create-target! pool format width height label)]
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
                                                (:height target))]
                              (fnil conj []) target))))))
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
   :!variant-layer (atom nil)
   :!plan-state (atom (frame-graph/empty-maintained-state))
   :!effect-state (atom (frame-effects/empty-maintained-state))
   :!receipt (atom {:frames 0 :linear-frames 0 :copy-present-frames 0
                    :exports 0 :passes [] :color-mode :legacy})})

(defn ensure-variant-layer!
  "Lazy constructor. The callback must mint only mode-specific pipelines,
   views, and bind groups over the supplied existing systems."
  [compositor build-variant-layer! systems]
  (or @(:!variant-layer compositor)
      (let [variant (build-variant-layer! (:device compositor) systems)]
        (when-not (and (:families variant) (:linearize-entry variant))
          (throw (ex-info "Linear variant layer is incomplete"
                          {:keys (keys variant)})))
        (reset! (:!variant-layer compositor) variant)
        variant)))

(defn destroy-compositor! [compositor]
  (when-let [destroy! (:destroy! @(:!variant-layer compositor))]
    (destroy!))
  (destroy-target-pool! (:target-pool compositor))
  (reset! (:!variant-layer compositor) nil)
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

(defn- begin-target-pass! [encoder target load-op]
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
            horizontal (acquire-target! (:target-pool compositor)
                                        "rgba16float" width height
                                        (str label "/h" level))]
        (swap! acquired conj horizontal)
        (blur-pass! compositor encoder source horizontal [sample-scale 0.0]
                    transient-buffers)
        (when source-owned?
          (release-target! (:target-pool compositor) source))
        (let [vertical (acquire-target! (:target-pool compositor)
                                        "rgba16float" width height
                                        (str label "/v" level))]
          (swap! acquired conj vertical)
          (blur-pass! compositor encoder horizontal vertical [0.0 sample-scale]
                      transient-buffers)
          (release-target! (:target-pool compositor) horizontal)
          (recur vertical (inc level) true))))))

(defn- index-in-ranges? [ranges index]
  (some (fn [[start end]] (<= start index (dec end))) ranges))

(defn- group-for-index [spans index]
  (->> spans
       (filter #(index-in-ranges? (:entry-ranges %) index))
       (sort-by :depth >) first :container/id))

(defn- mask-source-entry? [selector entry]
  (or (= selector (:instance/id entry))
      (= selector (:entry/id entry))
      (= selector (:material/id entry))
      (and (sequential? (:entry/id entry))
           (some #(= selector %) (:entry/id entry)))))

(defn- expand-execution-spans
  "A split span is executed as one compositor action per contiguous range.
   This preserves forward tape order if a future pass-class interleaving makes
   the normally-contiguous stack path split. The full range set remains on the
   row so a designated mask source is shared by every action."
  [spans]
  (into []
        (mapcat (fn [span]
                  (map (fn [entry-range]
                         (assoc span
                                :all-entry-ranges (:entry-ranges span)
                                :entry-ranges [entry-range]
                                :execution/key
                                [(:container/id span) entry-range]))
                       (:entry-ranges span))))
        spans))

(defn- group-min-index [span]
  (ffirst (:entry-ranges span)))

(defn- child-groups [spans parent-id]
  (filter #(= parent-id (:parent/container-id %)) spans))

(defn- sequence-actions [arrangement spans owner]
  (let [owner-id (:container/id owner)
        owner-ranges (:entry-ranges owner)
        children (->> (child-groups spans owner-id)
                      (filter (fn [child]
                                (or (nil? owner)
                                    (some (fn [[start _end]]
                                            (index-in-ranges? owner-ranges start))
                                          (:entry-ranges child)))))
                      vec)
        child-at (into {} (keep (fn [child]
                                  (when-let [index (group-min-index child)]
                                    [index child]))) children)
        child-indices (set (mapcat (fn [child]
                                    (mapcat (fn [[start end]] (range start end))
                                            (:entry-ranges child)))
                                  children))
        mask-selector (get-in owner [:effects :mask])]
    (->> arrangement
         (map-indexed vector)
         (keep (fn [[index entry]]
                 (cond
                   (contains? child-at index)
                   {:action :group :index index :group (get child-at index)}

                   (contains? child-indices index) nil

                   (and (index-in-ranges? owner-ranges index)
                        (= owner-id (group-for-index spans index))
                        (not (mask-source-entry? mask-selector entry)))
                   {:action :entry :index index :entry entry}

                   (and (nil? owner) (nil? (group-for-index spans index)))
                   {:action :entry :index index :entry entry}

                   :else nil)))
         vec)))

(defn- render-direct-entry!
  [encoder target entry execute-entry! linearize-entry]
  (let [pass (begin-target-pass! encoder target "load")]
    (execute-entry! pass (linearize-entry entry)
                    [(:width target) (:height target)])
    (.end pass)))

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

(defn- mask-clip? [clip]
  (= :mask (:mode clip)))

(defn- render-masked-sub-draw!
  [compositor encoder target entry sub-draw execute-entry! linearize-entry
   acquired transient-buffers]
  (let [cid (or (:entry/id entry) (:instance/id entry))
        content (acquire-target! (:target-pool compositor) "rgba16float"
                                 (:width target) (:height target)
                                 (str "frame/clip-content/" cid))
        mask (acquire-target! (:target-pool compositor) "rgba16float"
                              (:width target) (:height target)
                              (str "frame/clip-mask/" cid))
        unclipped (assoc-in entry [:paint :sub-draws]
                            [(assoc sub-draw :clip nil)])]
    (swap! acquired into [content mask])
    (clear-target! encoder content)
    (render-direct-entry! encoder content unclipped execute-entry! linearize-entry)
    (draw-clip-mask! compositor encoder mask (get-in sub-draw [:clip :points])
                     transient-buffers)
    (draw-composite! compositor encoder target content mask nil nil
                     1.0 false transient-buffers "load")
    (release-target! (:target-pool compositor) content)
    (release-target! (:target-pool compositor) mask)))

(defn- render-entry-action!
  [compositor encoder target entry execute-entry! linearize-entry
   acquired transient-buffers]
  (let [sub-draws (get-in entry [:paint :sub-draws])]
    (if (some (comp mask-clip? :clip) sub-draws)
      ;; Preserve per-operation order whenever one sub-draw takes the general
      ;; mask road; neighboring scissor and unclipped operations stay distinct.
      (doseq [sub-draw sub-draws]
        (if (mask-clip? (:clip sub-draw))
          (render-masked-sub-draw! compositor encoder target entry sub-draw
                                   execute-entry! linearize-entry acquired
                                   transient-buffers)
          (render-direct-entry!
           encoder target (assoc-in entry [:paint :sub-draws] [sub-draw])
           execute-entry! linearize-entry)))
      (render-direct-entry! encoder target entry execute-entry! linearize-entry))))

(defn- render-actions!
  [compositor encoder target actions render-group! execute-entry!
   linearize-entry acquired transient-buffers project-blur-fn]
  (clear-target! encoder target)
  (doseq [{:keys [action entry group]} actions]
    (case action
      :entry
      (render-entry-action! compositor encoder target entry execute-entry!
                            linearize-entry acquired transient-buffers)

      :group
      (let [cid (:container/id group)
            {:keys [output mask]} (render-group! group)
            {:keys [opacity backdrop-blur]} (:effects group)]
        (if backdrop-blur
          (let [snapshot (acquire-target! (:target-pool compositor)
                                          "rgba16float" (:width target)
                                          (:height target)
                                          (str "frame/backdrop-snapshot/" cid))
                projection (project-blur-fn group :backdrop-blur backdrop-blur)
                _ (swap! acquired conj snapshot)
                _ (.copyTextureToTexture
                   ^js encoder
                   (clj->js {:texture (:texture target)})
                   (clj->js {:texture (:texture snapshot)})
                   (clj->js {:width (:width target) :height (:height target)}))
                blurred (blur-target! compositor encoder snapshot
                                      projection
                                      acquired transient-buffers
                                      (str "frame/backdrop/" cid) false)]
            (draw-composite! compositor encoder target output mask snapshot
                             blurred opacity true transient-buffers "load")
            (release-target! (:target-pool compositor) snapshot)
            (release-target! (:target-pool compositor) blurred))
          (draw-composite! compositor encoder target output mask nil nil
                           opacity false transient-buffers "load"))
        (release-target! (:target-pool compositor) output)
        (release-target! (:target-pool compositor) mask))
      nil)))

(defn- render-mask!
  [compositor encoder arrangement span variant execute-entry! acquired
   transient-buffers]
  (when-let [selector (get-in span [:effects :mask])]
    (let [target (acquire-target! (:target-pool compositor) "rgba16float"
                                  (:width variant) (:height variant)
                                  (str "frame/mask/" (:container/id span)))
          mask-ranges (or (:all-entry-ranges span) (:entry-ranges span))
          entries (->> arrangement
                       (map-indexed vector)
                       (keep (fn [[index entry]]
                               (when (and (index-in-ranges? mask-ranges
                                                            index)
                                          (mask-source-entry? selector entry))
                                 entry))))]
      (swap! acquired conj target)
      (clear-target! encoder target)
      (doseq [entry entries]
        (render-entry-action! compositor encoder target entry execute-entry!
                              (:linearize-entry variant) acquired
                              transient-buffers))
      target)))

(defn- effective-scale [effective-transforms container-id]
  (let [[a b c d] (or (get-in effective-transforms [container-id :affine])
                      [1.0 0.0 0.0 1.0])
        sx (js/Math.sqrt (+ (* a a) (* b b)))
        sy (js/Math.sqrt (+ (* c c) (* d d)))]
    (max sx sy)))

(defn- project-blur!
  [!projections effective-transforms zoom span kind blur]
  (let [projection
        (assoc (frame-effects/projected-blur
                blur (effective-scale effective-transforms
                                      (:container/id span)) zoom)
               :container/id (:container/id span)
               :effect kind)]
    (swap! !projections conj projection)
    projection))

(defn- encode-linear-scene!
  [compositor encoder arrangement effect-spans variant execute-entry! width height
   zoom effective-transforms]
  (let [acquired (atom [])
        transient-buffers (atom [])
        blur-projections (atom [])
        variant (assoc variant :width width :height height)
        linearize-entry (:linearize-entry variant)
        execution-spans (expand-execution-spans effect-spans)
        project-blur-fn #(project-blur! blur-projections effective-transforms
                                        zoom %1 %2 %3)]
    (try
      (letfn [(render-group! [span]
                (let [cid (:container/id span)
                      content
                      (acquire-target! (:target-pool compositor) "rgba16float"
                                       width height
                                       (str "frame/group-content/" cid))
                      _ (swap! acquired conj content)
                      actions (sequence-actions arrangement execution-spans span)
                      _ (render-actions! compositor encoder content actions
                                         render-group! execute-entry!
                                         linearize-entry acquired
                                         transient-buffers project-blur-fn)
                      layer-blur (get-in span [:effects :layer-blur])
                      source
                      (if layer-blur
                        (blur-target! compositor encoder content
                                      (project-blur-fn span :layer-blur
                                                       layer-blur)
                                      acquired transient-buffers
                                      (str "frame/layer/" cid) true)
                        content)
                      mask (render-mask! compositor encoder arrangement span
                                         variant execute-entry! acquired
                                         transient-buffers)
                      output
                      (acquire-target! (:target-pool compositor) "rgba16float"
                                       width height
                                       (str "frame/group-output/" cid))]
                  (swap! acquired conj output)
                  (clear-target! encoder output)
                  (draw-composite! compositor encoder output source mask nil nil
                                   1.0 false transient-buffers "load")
                  (release-target! (:target-pool compositor) content)
                  (when-not (identical? source content)
                    (release-target! (:target-pool compositor) source))
                  (release-target! (:target-pool compositor) mask)
                  ;; Mask coverage is resolved into the output exactly once.
                  {:output output :mask nil}))]
        (let [scene (acquire-target! (:target-pool compositor) "rgba16float"
                                     width height "frame/scene-color")
              _ (swap! acquired conj scene)
              root-actions (sequence-actions arrangement execution-spans nil)]
          (render-actions! compositor encoder scene root-actions render-group!
                           execute-entry! linearize-entry acquired
                           transient-buffers project-blur-fn)
          {:scene scene :acquired @acquired
           :transient-buffers @transient-buffers
           :blur-projections @blur-projections
           :group-results {}}))
      (catch :default error
        ;; Nothing from this encoder was submitted, so every lease and mapped
        ;; uniform can be retired immediately on the failed encode road.
        (doseq [target @acquired]
          (release-target! (:target-pool compositor) target))
        (doseq [buffer @transient-buffers]
          (.destroy ^js buffer))
        (throw error)))))

(defn- draw-present!
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

(defn- release-after-submit! [compositor acquired transient-buffers]
  (doseq [target acquired]
    (release-target! (:target-pool compositor) target))
  (-> (.onSubmittedWorkDone (.-queue ^js (:device compositor)))
      (.then (fn [] (doseq [buffer transient-buffers] (.destroy ^js buffer))))
      (.catch (fn [_] nil))))

(defn draw-multipass!
  "Encode one linear effect frame, submit once, and present once."
  [compositor {:keys [context arrangement effect-spans variant execute-entry!
                      width height plan zoom effective-transforms]
               :or {zoom 1.0 effective-transforms {}}}]
  (let [device (:device compositor)
        encoder (.createCommandEncoder ^js device)
        swap-texture (.getCurrentTexture ^js context)
        swap-view (.createView swap-texture)
        {:keys [scene acquired transient-buffers blur-projections]}
        (encode-linear-scene! compositor encoder arrangement effect-spans
                              variant execute-entry! width height zoom
                              effective-transforms)]
    (draw-present! compositor encoder scene swap-view (:format compositor))
    (.submit (.-queue ^js device) #js [(.finish encoder)])
    (release-after-submit! compositor acquired transient-buffers)
    (swap! (:!receipt compositor)
           (fn [receipt]
             (-> receipt (update :frames inc) (update :linear-frames inc)
                 (assoc :color-mode :scene-color/linear
                        :passes (mapv :pass/id (:passes plan))
                        :plan-hash (:plan/hash plan)
                        :blur-projections blur-projections
                        :pool (target-pool-receipt (:target-pool compositor))))))
    {:submitted? true :color-mode :scene-color/linear
     :plan-hash (:plan/hash plan)}))

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

(defn- world-export-projection [arrangement effect-spans]
  (let [indexed (->> arrangement
                     (map-indexed vector)
                     (filter (fn [[_ entry]]
                               (= :world (get-in entry [:order :stratum]))))
                     vec)
        arrangement (mapv second indexed)
        old->new (into {} (map-indexed (fn [new-index [old-index _]]
                                        [old-index new-index]) indexed))
        spans
        (->> effect-spans
             (keep (fn [span]
                     (let [new-indices
                           (keep old->new
                                 (mapcat (fn [[start end]] (range start end))
                                         (:entry-ranges span)))]
                       (when (seq new-indices)
                         (assoc span
                                :entry-ranges
                                (vec (frame-effects/contiguous-ranges new-indices))
                                :entry-count (count new-indices))))))
             vec)]
    {:arrangement arrangement :effect-spans spans}))

(defn export-viewport!
  "Render the declared world-only export plan and asynchronously return PNG
   bytes plus the C7/M11 loss/profile receipt. No mapping occurs in encode."
  [compositor {:keys [arrangement effect-spans variant execute-entry!
                      width height zoom effective-transforms]
               :or {zoom 1.0 effective-transforms {}}}]
  (let [{world-arrangement :arrangement world-spans :effect-spans}
        (world-export-projection arrangement effect-spans)
        device (:device compositor)
        encoder (.createCommandEncoder ^js device)
        export-plan (frame-graph/compile-export-plan
                     {:arrangement world-arrangement
                      :viewport {:width width :height height
                                 :format "rgba8unorm"}})
        {:keys [scene acquired transient-buffers blur-projections]}
        (encode-linear-scene! compositor encoder world-arrangement world-spans
                              variant execute-entry! width height zoom
                              effective-transforms)
        output (acquire-target! (:target-pool compositor) "rgba8unorm"
                                width height "frame/export-output")
        padded-bytes-per-row (* 256 (js/Math.ceil (/ (* width 4) 256)))
        buffer-size (* padded-bytes-per-row height)
        read-buffer (.createBuffer ^js device
                                   (clj->js {:size buffer-size
                                             :usage (bit-or js/GPUBufferUsage.COPY_DST
                                                            js/GPUBufferUsage.MAP_READ)}))
        _ (draw-present! compositor encoder scene (:view output) "rgba8unorm")
        _ (.copyTextureToBuffer
           encoder (clj->js {:texture (:texture output)})
           (clj->js {:buffer read-buffer
                     :bytesPerRow padded-bytes-per-row :rowsPerImage height})
           (clj->js {:width width :height height}))
        _ (.submit (.-queue ^js device) #js [(.finish encoder)])
        all-targets (conj (vec acquired) output)
        metadata {:output-profile :srgb :alpha-association :straight
                  :strata #{:world}
                  :intentional-losses
                  [:structured-identity :material-provenance
                   :family-export-projection :authoring-overlay]
                  :bytes-per-row padded-bytes-per-row
                  :async-only true :plan-hash (:plan/hash export-plan)
                  :entry-count (count world-arrangement)
                  :blur-projections blur-projections}]
    (-> (.mapAsync read-buffer js/GPUMapMode.READ)
        (.then
         (fn []
           (let [copy (js/Uint8Array. buffer-size)]
             (.set copy (js/Uint8Array. (.getMappedRange read-buffer)))
             (.unmap read-buffer)
             (.destroy read-buffer)
             (unpremultiply!
              (strip-padded-rows (.-buffer copy) width height
                                 padded-bytes-per-row)))))
        (.then (fn [rgba]
                 (-> (png-bytes! rgba width height)
                     (.then (fn [bytes]
                              (doseq [target all-targets]
                                (release-target! (:target-pool compositor) target))
                              (doseq [buffer transient-buffers]
                                (.destroy ^js buffer))
                              (swap! (:!receipt compositor)
                                     #(-> % (update :exports inc)
                                          (assoc :last-export metadata)))
                              {:bytes bytes :rgba rgba :metadata metadata})))))
        (.catch (fn [error]
                  (.destroy read-buffer)
                  (doseq [target all-targets]
                    (release-target! (:target-pool compositor) target))
                  (throw error))))))

(defn compositor-receipt [compositor]
  (assoc @(:!receipt compositor)
         :pool (target-pool-receipt (:target-pool compositor))))
