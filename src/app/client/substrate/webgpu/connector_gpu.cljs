(ns app.client.substrate.webgpu.connector-gpu
  "WebGPU upload and text-geo custody for connector route projections.

   Route resolution and mesh caching remain pure in connector-route. This
   namespace owns only the packed vertex buffer, upload identity gate, and one
   label geo cloned from the live content text system."
  (:require [clojure.string :as str]
            [app.client.substrate.connector-material :as connector-material]
            [app.client.substrate.connector-route :as connector-route]
            [app.client.substrate.frame-inputs :as frame-inputs]
            [app.client.substrate.path-material :as path-material]
            [app.client.substrate.scene-tape :as scene-tape]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.substrate.webgpu.path-gpu :as path-gpu]))

(def connector-color-mode-declaration
  "const kConnectorLinearPremultiplied: bool = false;")

(def connector-fragment-shader
  (str connector-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
       "}\n"
       "@fragment fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> {\n"
       "  if (!kConnectorLinearPremultiplied) { return color; }\n"
       "  let alpha = clamp(color.a, 0.0, 1.0);\n"
       "  let linear = vec3<f32>(srgb_channel_to_linear(color.r),\n"
       "                         srgb_channel_to_linear(color.g),\n"
       "                         srgb_channel_to_linear(color.b));\n"
       "  return vec4<f32>(linear * alpha, alpha);\n"
       "}\n"))

(defn- configure-connector-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader connector-color-mode-declaration
                 "const kConnectorLinearPremultiplied: bool = true;")
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

(defn init-connector-system
  [^js device fformat camera-buffer containers-buffer
   & {:keys [initial-capacity tracker scene-color text-api]
      :or {initial-capacity 2048
           scene-color scene-tape/legacy-direct-color}}]
  (assert camera-buffer "init-connector-system requires :camera-buffer")
  (assert containers-buffer "init-connector-system requires :containers-buffer")
  (assert (every? fn? ((juxt :clone :update :destroy) text-api))
          "init-connector-system requires clone/update/destroy text API")
  (let [vertex-module (.createShaderModule
                       device (clj->js {:code path-gpu/path-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js {:code (configure-connector-color-shader
                                         connector-fragment-shader scene-color)}))
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
    (gpu-budget/register-buffer! tracker buffer "connector/vertices"
                                 (* initial-capacity path-material/vertex-stride)
                                 :active-bytes 0)
    {:device device :pipeline pipeline :bind-group bind-group
     :camera-buffer camera-buffer :containers-buffer containers-buffer
     :scene-color scene-color :gpu-tracker tracker :text-api text-api
     :frame-input/identity (js-obj) :!shape-rev (atom 0)
     :!buffer (atom buffer) :!capacity (atom initial-capacity)
     :!route-cache (atom (connector-route/empty-cache))
     :!prepared (atom []) :!labels (atom [])
     :!last-mesh-set-key (atom ::never)
     :!last-prepare-key (atom ::never)
     :!label-geo (atom nil) :!label-token (atom ::never)
     :!label-parent (atom nil)
     :!receipt (atom {:connector-system/version 1 :uploads 0
                      :label-writes 0 :vertices 0
                      :route-resolutions 0 :mesh-derivations 0})}))

(defn- ensure-capacity! [connector-system required]
  (let [capacity @(:!capacity connector-system)]
    (when (> required capacity)
      (let [next-capacity (loop [candidate (max 1 capacity)]
                            (if (>= candidate required)
                              candidate
                              (recur (* 2 candidate))))
            old-buffer @(:!buffer connector-system)
            new-buffer (create-vertex-buffer (:device connector-system)
                                             next-capacity)]
        (gpu-budget/replace-buffer!
         (:gpu-tracker connector-system) old-buffer new-buffer
         "connector/vertices"
         (* next-capacity path-material/vertex-stride)
         :active-bytes (* required path-material/vertex-stride)
         :reason :connector-capacity-growth)
        (.destroy old-buffer)
        (reset! (:!buffer connector-system) new-buffer)
        (reset! (:!capacity connector-system) next-capacity)))
    @(:!buffer connector-system)))

(defn- prepared-route [route first-vertex]
  (let [vertices (:vertices route)]
    {:route route
     :edge (:edge route)
     :owner-vi (get-in route [:edge :owner-vi])
     :edge-instance-id (:edge-instance-id route)
     :vertices vertices
     :first-vertex first-vertex
     :vertex-count (count vertices)}))

(defn- pack-vertices [prepared]
  (let [vertex-count (reduce + (map :vertex-count prepared))
        floats (js/Float32Array. (* vertex-count path-material/vertex-words))
        uints (js/Uint32Array. (.-buffer floats))]
    (loop [rows prepared vertex-offset 0]
      (if-let [{:keys [route edge vertices]} (first rows)]
        (let [[r g b a] (connector-material/paint-color (:material route))
              container-idx (or (:container-idx edge) 0)]
          (doseq [[index [x y]] (map-indexed vector vertices)]
            (let [base (* (+ vertex-offset index) path-material/vertex-words)]
              (aset floats (+ base 0) x)
              (aset floats (+ base 1) y)
              (aset floats (+ base 2) r)
              (aset floats (+ base 3) g)
              (aset floats (+ base 4) b)
              (aset floats (+ base 5) a)
              (aset uints (+ base 6) container-idx)))
          (recur (next rows) (+ vertex-offset (count vertices))))
        floats))))

(defn- same-label-parent? [prior current]
  (and prior current
       (= (:backend prior) (:backend current))
       (= (:family/id prior) (:family/id current))
       (identical? (:pipeline prior) (:pipeline current))
       (identical? (:bind-group prior) (:bind-group current))))

(defn- reconcile-label-geo!
  [connector-system labels font-assets content-text-system]
  (let [{:keys [clone update destroy]} (:text-api connector-system)
        prior-parent @(:!label-parent connector-system)
        prior-geo @(:!label-geo connector-system)
        reclone? (and content-text-system
                      (not (same-label-parent? prior-parent
                                               content-text-system)))
        token (mapv (fn [label]
                      [(:edge-instance-id label)
                       (get-in label [:layout-result :layout/id])
                       (select-keys (:paint-op label)
                                    [:text :x :y :size :r :g :b :a
                                     :container-idx])])
                    labels)]
    (cond
      (nil? content-text-system)
      {:writes 0 :geo prior-geo}

      (and prior-geo (not reclone?) (= token @(:!label-token connector-system)))
      {:writes 0 :geo prior-geo}

      :else
      (let [_ (when (and prior-geo reclone?) (destroy prior-geo))
            base (if (and prior-geo (not reclone?))
                   prior-geo
                   (clone (:device connector-system) content-text-system
                          (max 64 (* 16 (max 1 (count labels))))))
            texts (mapv (comp vector :paint-op) labels)
            geo (update (:device connector-system) base texts font-assets
                        connector-route/label-font-size
                        :px-range 8.0
                        :line-height connector-route/label-line-height
                        :char-width 0.56
                        :snap-step nil
                        :sharpness 0.0
                        :surface :connector-label)]
        (reset! (:!label-geo connector-system) geo)
        (reset! (:!label-parent connector-system) content-text-system)
        (reset! (:!label-token connector-system) token)
        {:writes 1 :geo geo}))))

(defn prepare-connector-frame!
  ([connector-system connector-ops targets-by-address effective zoom
    font-assets content-text-system]
   (prepare-connector-frame! connector-system connector-ops targets-by-address
                             effective zoom font-assets content-text-system {}))
  ([connector-system connector-ops targets-by-address effective zoom
    font-assets content-text-system
    {:keys [region-anchor-resolver region-anchor-resolver-token region-doors]}]
  (let [connector-ops (or connector-ops [])
        prepare-key {:connector-ops connector-ops
                     :targets-by-address targets-by-address
                     :effective-transforms effective
                     :connector-zoom-regime
                     (:regime/id (path-material/zoom-regime zoom))
                     :font-provider
                     (connector-route/provider-identity font-assets)
                     :content-text-system
                     (frame-inputs/system-token content-text-system)
                     :region-anchor-resolver
                     (or region-anchor-resolver-token region-anchor-resolver)
                     :region-doors region-doors}]
    (if (and (map? @(:!last-prepare-key connector-system))
             (frame-inputs/inputs-same?
              (keys prepare-key) @(:!last-prepare-key connector-system)
              prepare-key))
      (let [receipt @(:!receipt connector-system)]
        {:mesh-set-changed? false :writes 0 :label-writes 0
         :vertices (:vertices receipt 0)
         :frame-receipt (:last-frame receipt)
         :census (:census receipt)})
      (let [derivation (connector-route/derive-route-set
                    @(:!route-cache connector-system)
                    connector-ops targets-by-address effective zoom font-assets
                    {:region-anchor-resolver region-anchor-resolver
                     :region-doors region-doors})
        routes (:routes derivation)
        prepared
        (loop [remaining routes first-vertex 0 rows []]
          (if-let [route (first remaining)]
            (let [row (prepared-route route first-vertex)]
              (recur (next remaining)
                     (+ first-vertex (:vertex-count row))
                     (conj rows row)))
            rows))
        labels (mapv (fn [index label] (assoc label :line-index index))
                     (range) (:labels derivation))
        label-result (reconcile-label-geo! connector-system labels
                                           font-assets content-text-system)
        mesh-set-key (:mesh-set-key derivation)
        mesh-set-changed? (not= mesh-set-key
                                @(:!last-mesh-set-key connector-system))
        vertices (reduce + (map :vertex-count prepared))
        writes
        (if mesh-set-changed?
          (let [packed (pack-vertices prepared)
                buffer (ensure-capacity! connector-system vertices)]
            (when (pos? vertices)
              (let [^js device (:device connector-system)]
                (.writeBuffer (.-queue device) buffer 0 packed)))
            (gpu-budget/set-active-bytes!
             (:gpu-tracker connector-system) buffer
             (* vertices path-material/vertex-stride))
            (reset! (:!prepared connector-system) prepared)
            (reset! (:!last-mesh-set-key connector-system) mesh-set-key)
            (if (pos? vertices) 1 0))
          0)
        route-state (:state derivation)
        frame-receipt (:frame-receipt derivation)
        shape-changed? (or mesh-set-changed?
                           (pos? (:writes label-result)))]
    (reset! (:!route-cache connector-system) route-state)
    (reset! (:!labels connector-system) labels)
    (reset! (:!last-prepare-key connector-system) prepare-key)
    (when shape-changed?
      (frame-inputs/bump-shape-rev! connector-system))
    (swap! (:!receipt connector-system)
           (fn [receipt]
             (-> receipt
                 (update :uploads + writes)
                 (update :label-writes + (:writes label-result))
                 (assoc :vertices vertices
                        :cache-size (:cache-size frame-receipt)
                        :route-resolutions (:route-resolutions route-state)
                        :anchor-projections (:anchor-projections route-state)
                        :mesh-derivations (:mesh-derivations route-state)
                        :routes
                        (mapv #(select-keys %
                                            [:edge-instance-id :status
                                             :anchor-points :stroke-points
                                             :label-position :anchor-clamped])
                              (:routes derivation))
                        :last-frame frame-receipt
                        :census (:census derivation)))))
    {:mesh-set-changed? mesh-set-changed?
     :writes writes :label-writes (:writes label-result)
     :vertices vertices
     :frame-receipt frame-receipt
     :census (:census derivation)})))))

(defn- frame-order [source-order entry-id part-rank]
  {:stratum (or (:stratum source-order) :world)
   :pass-class :direct
   :stack-path (into [[:frame/root 25 25]]
                     (or (:stack-path source-order) []))
   :part-rank part-rank
   :stable-tie entry-id})

(defn- contiguous-owner-runs [rows owner-key]
  (loop [remaining rows offset 0 runs []]
    (if-let [row (first remaining)]
      (let [owner (owner-key row)
            same (vec (take-while #(= owner (owner-key %)) remaining))
            count* (count same)]
        (recur (drop count* remaining) (+ offset count*)
               (conj runs {:owner owner :offset offset :rows same})))
      runs)))

(defn connector-entries
  "Mint one label entry and one mesh entry per connector-owning store slot.
   The label helper is supplied by renderer to avoid a namespace cycle."
  [{:keys [store-frame connector-system connector-label-entry]}]
  (if-not (and store-frame connector-system)
    []
    (let [prepared @(:!prepared connector-system)
          labels @(:!labels connector-system)
          label-geo @(:!label-geo connector-system)
          mesh-runs (contiguous-owner-runs prepared :owner-vi)
          label-runs (into {} (map (juxt :owner identity))
                           (contiguous-owner-runs labels :owner-vi))]
      (into []
            (mapcat
             (fn [{:keys [owner rows]}]
               (let [source-order (get-in store-frame [:order-by-vi owner])
                     mesh-first (or (:first-vertex (first rows)) 0)
                     mesh-count (reduce + (map :vertex-count rows))
                     mesh-id [:frame/store owner :connectors]
                     label-run (get label-runs owner)
                     label-rows (:rows label-run)
                     first-line (:line-index (first label-rows))
                     last-line (:line-index (peek label-rows))
                     offsets (:line-offsets label-geo)
                     label-first (when (and first-line offsets)
                                   (nth offsets first-line))
                     label-end (when (and last-line offsets)
                                 (if (< (inc last-line) (count offsets))
                                   (nth offsets (inc last-line))
                                   (:num-instances label-geo)))
                     label-id [:frame/store owner :connector-labels]
                     label-entry
                     (when (and connector-label-entry label-geo
                                label-first label-end (< label-first label-end))
                       (connector-label-entry
                        {:entry-id label-id
                         :order (frame-order source-order label-id 4)
                         :system label-geo
                         :instance-count (- label-end label-first)
                         :first-instance label-first
                         :pick {:geometry :connector-label
                                :owner (mapv (fn [label]
                                               {:edge-instance-id
                                                (:edge-instance-id label)
                                                :part :label})
                                             label-rows)}}))
                     mesh-entry
                     (when (pos? mesh-count)
                       {:entry/id mesh-id
                        :material/id mesh-id
                        :material/revision 0
                        :instance/id mesh-id
                        :family/id :render.family/connector
                        :order (frame-order source-order mesh-id 5)
                        :paint {:paint/source connector-system
                                :paint/source-type :connector-system
                                :vertex-count mesh-count
                                :first-vertex mesh-first}
                        :pick {:geometry :reference-edge-route
                               :owner (mapv (fn [row]
                                              {:edge-instance-id
                                               (:edge-instance-id row)
                                               :part :route})
                                            rows)
                               :boundary :hit
                               :hit-slop connector-material/hit-slop-screen-px}
                        :visibility {:visible? true :clip :shared-tree-clip}})]
                 (keep identity [label-entry mesh-entry])))
             mesh-runs)))))

(defn execute-connector-batch! [^js pass entry]
  (let [paint (:paint entry)
        connector-system (:paint/source paint)
        {:keys [vertex-count first-vertex]} paint
        ;; Explicit paint wins (same law as resolve-gpu-paint): the linear
        ;; variant's linearize-entry overrides pipeline/bind-group for the
        ;; rgba16float pass; only the buffer resolves through the source.
        pipeline (or (:pipeline paint) (:pipeline connector-system))
        bind-group (or (:bind-group paint) (:bind-group connector-system))
        buffer @(:!buffer connector-system)]
    (.setPipeline pass pipeline)
    (.setBindGroup pass 0 bind-group)
    (.setVertexBuffer pass 0 buffer)
    (.draw pass vertex-count 1 first-vertex 0)
    (:entry/id entry)))

(defn connector-receipt [connector-system]
  @(:!receipt connector-system))

(defn destroy-connector-system! [connector-system]
  (when-let [label-geo @(:!label-geo connector-system)]
    ((get-in connector-system [:text-api :destroy]) label-geo))
  (when-let [buffer @(:!buffer connector-system)]
    (gpu-budget/destroy-resource! (:gpu-tracker connector-system) buffer
                                  :reason :connector-system-destroy)
    (.destroy buffer))
  (reset! (:!prepared connector-system) [])
  (reset! (:!labels connector-system) [])
  (reset! (:!route-cache connector-system) (connector-route/empty-cache))
  (connector-route/reset-live-route-cache!)
  (reset! (:!last-mesh-set-key connector-system) ::destroyed)
  (reset! (:!label-geo connector-system) nil)
  nil)
