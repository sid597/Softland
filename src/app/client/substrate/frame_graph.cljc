(ns app.client.substrate.frame-graph
  "Pure W4 frame-plan compiler.

   Render-seam declarations: keyed inputs are effect-bearing container topology
   (ids, nesting, normalized values), enabled regions, capabilities, viewport
   resource shape, and color mode; doors are registry/effect/region/viewport/
   capability changes;
   this namespace owns pass structure and fresh per-frame range binding;
   projections are validated executor data, replay hash, and export plan; the
   independent oracle is oracle-compile-frame-plan, which never calls the
   maintained structure compiler. Structure reuse never retains entry indices."
  (:require [clojure.set :as set]
            [app.client.substrate.frame-effects :as frame-effects]
            [app.client.substrate.scene-tape :as scene-tape]))

(def graph-version 1)
(def legal-pass-kinds scene-tape/frame-pass-kinds)
(def legal-color-modes #{:legacy :scene-color/linear})
(def legal-causes
  #{:world :viewport :resource :interaction :clock :readback :device-recovery})

(def seam-declarations
  {:keyed-inputs [:effect-container-topology :regions :capabilities
                  :viewport-shape :color-mode]
   :doors [:registry-change :effect-change :region-change
           :capability-change :viewport-change]
   :ownership :frame-graph/compiler
   :projections [:pass-structure :entry-range-binding :plan-hash :export-plan]
   :oracle :oracle-compile-frame-plan})

(defn clip-execution-mode
  "A screen-axis-aligned effective affine uses a scissor. Rotation/shear is a
   declared mask-road binding; callers must never approximate it as a scissor."
  [[a b c d _tx _ty]]
  (if (or (and (< (abs (double b)) 1.0e-9)
               (< (abs (double c)) 1.0e-9))
          (and (< (abs (double a)) 1.0e-9)
               (< (abs (double d)) 1.0e-9)))
    :scissor
    :mask))

(defn- canonical-value [value]
  (cond
    (map? value) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                       (map (fn [[k v]] [k (canonical-value v)])) value)
    (set? value) (mapv canonical-value (sort-by pr-str value))
    (sequential? value) (mapv canonical-value value)
    :else value))

(defn stable-hash
  "Small deterministic cross-runtime hash for plan/replay identity. It is not a
   security digest; the verifier's source/input digests remain SHA-256."
  [value]
  (let [text (pr-str (canonical-value value))]
    (str "fg1-"
         (reduce (fn [acc ch]
                   (mod (+ (* acc 131)
                           #?(:clj (int ch)
                              :cljs (.charCodeAt ch 0)))
                        2147483647))
                 7 text))))

(defn- effect-topology [effect-spans]
  (->> effect-spans
       (map #(select-keys % [:container/id :parent/container-id
                             :stack-path :depth :effects]))
       distinct
       (sort-by (juxt (comp - :depth) (comp pr-str :container/id)))
       vec))

(defn select-color-mode [{:keys [effect-spans regions forced-color-mode]}]
  (cond
    (and (seq regions) (= :legacy forced-color-mode))
    (throw (ex-info "Region3D plans cannot force the legacy color road"
                    {:forced-color-mode forced-color-mode
                     :regions (mapv :region/id regions)}))

    (seq regions) :scene-color/linear

    forced-color-mode
    (do (when-not (contains? legal-color-modes forced-color-mode)
          (throw (ex-info "Unknown forced frame color mode"
                          {:forced-color-mode forced-color-mode})))
        forced-color-mode)

    (seq effect-spans) :scene-color/linear
    :else :legacy))

(defn- arrangement-regions [arrangement]
  (->> arrangement
       (keep (fn [entry]
               (when (= :render.family/region-3d (:family/id entry))
                 (let [region-id (or (get-in entry [:paint :region-id])
                                     (:region-router entry))
                       [width height] (or (get-in entry [:paint :lease-size])
                                          (some-> (get-in entry [:paint :rect])
                                                  (subvec 2 4))
                                          [1 1])]
                   {:region/id region-id
                    :size [width height]
                    :shadow? (boolean (get-in entry [:paint :shadow?]))}))))
       (sort-by (comp pr-str :region/id))
       vec))

(defn structure-input
  [{:keys [arrangement effect-spans capabilities viewport forced-color-mode]}]
  (let [regions (arrangement-regions arrangement)]
    {:effect-topology (effect-topology effect-spans)
   :regions regions
   :capabilities (into (sorted-set) (or capabilities #{}))
   :viewport (select-keys (or viewport {}) [:width :height :format :color-mode])
   :color-mode (select-color-mode {:effect-spans effect-spans
                                   :regions regions
                                   :forced-color-mode forced-color-mode})}))

(defn- resource
  [kind format usage lifetime budget-owner & {:as opts}]
  (merge {:kind kind :format format :usage (set usage)
          :lifetime lifetime :budget-owner budget-owner}
         opts))

(defn- read-edge [resource-id producer mode]
  {:resource resource-id :producer producer :mode mode})

(defn- attachment [resource-id format load store]
  {:resource resource-id :format format :load load :store store})

(defn- region-resource-id [kind region-id]
  (keyword "frame.region3d" (str (name kind) "-" (stable-hash region-id))))

(defn- region-resources [{id :region/id shadow? :shadow?}]
  (let [color-msaa (region-resource-id :color-msaa id)
        depth (region-resource-id :depth id)
        resolve (region-resource-id :resolve id)
        shadow (region-resource-id :shadow id)]
    (cond->
     {color-msaa
      (resource :color "rgba16float"
                #{:render-attachment} :held :compositor/region-leases
                :sample-count 4 :alpha-association :premultiplied
                :working-space :linear-srgb)
      depth
      (resource :depth "depth24plus"
                #{:render-attachment} :held :compositor/region-leases
                :sample-count 4 :channel-meaning :depth)
      resolve
      (resource :color "rgba16float"
                #{:render-attachment :texture-binding}
                :held :compositor/region-leases
                :sample-count 1 :alpha-association :premultiplied
                :working-space :linear-srgb)}
      shadow?
      (assoc shadow
             (resource :depth "depth32float"
                       #{:render-attachment :texture-binding}
                       :held :compositor/region-leases
                       :sample-count 1 :channel-meaning :depth)))))

(defn- region-passes [region rank]
  (let [{id :region/id shadow? :shadow?} region
        color-msaa (region-resource-id :color-msaa id)
        depth (region-resource-id :depth id)
        resolve (region-resource-id :resolve id)
        shadow (region-resource-id :shadow id)
        shadow-pass-id (region-resource-id :shadow-pass id)
        interior-pass-id (region-resource-id :interior-pass id)
        shadow-pass
        (when shadow?
          {:pass/id shadow-pass-id :pass/kind :region
           :topology-rank rank :region/id id :region/role :shadow
           :reads []
           :attachments {:depth (attachment shadow "depth32float"
                                            :clear :store)}})
        interior
        {:pass/id interior-pass-id :pass/kind :region
         :topology-rank (+ rank (if shadow? 1 0))
         :region/id id :region/role :interior
         :reads (cond-> []
                  shadow?
                  (conj (read-edge shadow shadow-pass-id :sampled)))
         :attachments
         {:color (assoc (attachment color-msaa "rgba16float" :clear :store)
                        :resolve-target resolve)
          :depth (attachment depth "depth24plus" :clear :store)}
         :produces resolve}]
    (cond-> [] shadow-pass (conj shadow-pass) true (conj interior))))

(defn- region-scene-reads [regions]
  (mapv (fn [{:region/keys [id]}]
          (read-edge (region-resource-id :resolve id)
                     (region-resource-id :interior-pass id)
                     :sampled))
        regions))

(defn- group-id [prefix cid]
  (keyword "frame.group" (str prefix "-" (stable-hash cid))))

(defn- group-resources [row]
  (let [cid (:container/id row)
        {:keys [mask layer-blur backdrop-blur]} (:effects row)
        content (group-id "content" cid)
        output (group-id "output" cid)]
    (cond-> {content (resource :color "rgba16float"
                               #{:render-attachment :texture-binding :copy-src}
                               :frame :frame-runtime/target-pool
                               :alpha-association :premultiplied
                               :working-space :linear-srgb
                               :clear [0.0 0.0 0.0 0.0])
             output (resource :color "rgba16float"
                              #{:render-attachment :texture-binding :copy-src}
                              :frame :frame-runtime/target-pool
                              :alpha-association :premultiplied
                              :working-space :linear-srgb
                              :clear [0.0 0.0 0.0 0.0])}
      mask
      (assoc (group-id "mask" cid)
             (resource :coverage "rgba16float"
                       #{:render-attachment :texture-binding}
                       :frame :frame-runtime/target-pool
                       :alpha-association :data :clear [0.0 0.0 0.0 0.0]))
      layer-blur
      (assoc (group-id "blur-h" cid)
             (resource :color "rgba16float"
                       #{:render-attachment :texture-binding}
                       :frame :frame-runtime/target-pool
                       :alpha-association :premultiplied
                       :working-space :linear-srgb)
             (group-id "blur-v" cid)
             (resource :color "rgba16float"
                       #{:render-attachment :texture-binding}
                       :frame :frame-runtime/target-pool
                       :alpha-association :premultiplied
                       :working-space :linear-srgb))
      backdrop-blur
      (assoc (group-id "backdrop-snapshot" cid)
             (resource :color "rgba16float"
                       #{:copy-dst :texture-binding :copy-src}
                       :frame :frame-runtime/target-pool
                       :alpha-association :premultiplied
                       :working-space :linear-srgb)
             (group-id "backdrop-blur" cid)
             (resource :color "rgba16float"
                       #{:render-attachment :texture-binding}
                       :frame :frame-runtime/target-pool
                       :alpha-association :premultiplied
                       :working-space :linear-srgb)))))

(defn- child-rows [topology cid]
  (filter #(= cid (:parent/container-id %)) topology))

(defn- group-passes [topology row rank]
  (let [cid (:container/id row)
        {:keys [mask layer-blur backdrop-blur opacity]} (:effects row)
        content (group-id "content" cid)
        output (group-id "output" cid)
        mask-id (group-id "mask" cid)
        blur-h (group-id "blur-h" cid)
        blur-v (group-id "blur-v" cid)
        snapshot (group-id "backdrop-snapshot" cid)
        backdrop (group-id "backdrop-blur" cid)
        render-id (group-id "render" cid)
        child-reads (mapv (fn [child]
                            (read-edge (group-id "output" (:container/id child))
                                       (group-id "composite" (:container/id child))
                                       :sampled))
                          (child-rows topology cid))
        mask-pass (when mask
                    {:pass/id (group-id "mask-render" cid)
                     :pass/kind :render :topology-rank (+ rank 0)
                     :group/container-id cid :mask/source mask
                     :reads []
                     :attachments {:color (attachment mask-id "rgba16float"
                                                      :clear :store)}})
        render-pass {:pass/id render-id :pass/kind :render
                     :topology-rank (+ rank 1)
                     :group/container-id cid :reads child-reads
                     :attachments {:color (attachment content "rgba16float"
                                                      :clear :store)}}
        blur-passes (when layer-blur
                      [{:pass/id (group-id "layer-blur-h" cid)
                        :pass/kind :render :topology-rank (+ rank 2)
                        :group/container-id cid
                        :reads [(read-edge content render-id :sampled)]
                        :attachments {:color (attachment blur-h "rgba16float"
                                                         :clear :store)}}
                       {:pass/id (group-id "layer-blur-v" cid)
                        :pass/kind :render :topology-rank (+ rank 3)
                        :group/container-id cid
                        :reads [(read-edge blur-h (group-id "layer-blur-h" cid)
                                           :sampled)]
                        :attachments {:color (attachment blur-v "rgba16float"
                                                         :clear :store)}}])
        snapshot-passes
        (when backdrop-blur
          [{:pass/id (group-id "backdrop-copy" cid)
            :pass/kind :copy :topology-rank (+ rank 4)
            :group/container-id cid
            :reads [(read-edge :scene-color/main :flat/base :copy)]
            :copy-writes #{snapshot}}
           {:pass/id (group-id "backdrop-blur" cid)
            :pass/kind :render :topology-rank (+ rank 5)
            :group/container-id cid
            :reads [(read-edge snapshot (group-id "backdrop-copy" cid)
                               :sampled)]
            :attachments {:color (attachment backdrop "rgba16float"
                                                     :clear :store)}}])
        source (if layer-blur blur-v content)
        source-producer (if layer-blur (group-id "layer-blur-v" cid) render-id)
        composite-reads
        (cond-> [(read-edge source source-producer :sampled)]
          mask (conj (read-edge mask-id (group-id "mask-render" cid) :sampled))
          backdrop-blur
          (conj (read-edge backdrop (group-id "backdrop-blur" cid) :sampled)))
        composite {:pass/id (group-id "composite" cid)
                   :pass/kind :render :topology-rank (+ rank 6)
                   :group/container-id cid
                   :composite {:opacity opacity
                               :mask? (boolean mask)
                               :layer-blur? (boolean layer-blur)
                               :backdrop-blur? (boolean backdrop-blur)}
                   :reads composite-reads
                   :attachments {:color (attachment output "rgba16float"
                                                    :clear :store)}}]
    (vec (concat (when mask-pass [mask-pass])
                 [render-pass] blur-passes snapshot-passes [composite]))))

(defn- base-resources [format linear? copy-present?]
  (cond
    linear?
    {:scene-color/main
     (resource :color "rgba16float"
               #{:render-attachment :texture-binding :copy-src :copy-dst}
               :frame :frame-runtime/target-pool
               :alpha-association :premultiplied
               :working-space :linear-srgb :clear [0.0 0.0 0.0 0.0])
     :present
     (resource :external-swap format #{:render-attachment :copy-dst}
               :external :canvas-context
               :alpha-association :premultiplied :working-space :srgb)}

    copy-present?
    {:scene-color/main
     (resource :color format #{:render-attachment :copy-src}
               :frame :frame-runtime/target-pool
               :alpha-association :straight :working-space :presentation-encoded)
     :present
     (resource :external-swap format #{:render-attachment :copy-dst}
               :external :canvas-context
               :alpha-association :premultiplied :working-space :srgb)}

    :else
    {:present
     (resource :external-swap format #{:render-attachment :copy-dst}
               :external :canvas-context
               :alpha-association :premultiplied :working-space :srgb)}))

(defn compile-plan-structure
  "Compile reusable pass/resource structure. No entry index is accepted or
   retained by this function."
  [structure-key]
  (let [{:keys [effect-topology regions capabilities viewport color-mode]}
        structure-key
        format (or (:format viewport) "bgra8unorm")
        linear? (= :scene-color/linear color-mode)
        copy-present? (and (not linear?) (contains? capabilities :copy-present))
        resources (merge (base-resources format linear? copy-present?)
                         (apply merge (map region-resources regions))
                         (when linear?
                           (apply merge (map group-resources effect-topology))))
        passes
        (cond
          linear?
          (let [region-passes (mapcat (fn [index region]
                                       (region-passes region (* index 2)))
                                     (range) regions)
                base-rank (inc (* 2 (count regions)))
                region-reads (region-scene-reads regions)
                base {:pass/id :flat/base :pass/kind :render
                      :topology-rank base-rank
                      :reads region-reads
                      :attachments {:color (attachment :scene-color/main
                                                       "rgba16float" :clear :store)}}
                group-passes (mapcat (fn [index row]
                                       (mapv #(update % :reads into region-reads)
                                             (group-passes effect-topology row
                                                           (+ base-rank 10
                                                              (* index 10)))))
                                     (range) effect-topology)
                top-groups (filter #(nil? (:parent/container-id %))
                                   effect-topology)
                composite {:pass/id :flat/composite :pass/kind :render
                           :topology-rank (+ base-rank 10
                                             (* 10 (count effect-topology)))
                           :reads (mapv #(read-edge
                                         (group-id "output" (:container/id %))
                                         (group-id "composite" (:container/id %))
                                         :sampled)
                                        top-groups)
                           :attachments {:color (attachment :scene-color/main
                                                            "rgba16float" :load :store)}}
                present {:pass/id :present :pass/kind :present
                         :topology-rank (inc (:topology-rank composite))
                         :reads [(read-edge :scene-color/main :flat/composite
                                            :sampled)]
                         :attachments {:color (attachment :present format
                                                          :clear :store)}
                         :presentation-terminal? true}]
            (vec (concat region-passes [base] group-passes
                         [composite present])))

          copy-present?
          [{:pass/id :direct/main :pass/kind :render :topology-rank 0 :reads []
            :attachments {:color (attachment :scene-color/main format
                                             :clear :store)}}
           {:pass/id :present :pass/kind :copy :topology-rank 1
            :reads [(read-edge :scene-color/main :direct/main :copy)]
            :copy-writes #{:present} :presentation-terminal? true}]

          :else
          [{:pass/id :direct/main :pass/kind :render :topology-rank 0 :reads []
            :attachments {:color (attachment :present format :clear :store)}
            :presentation-terminal? true}])
        structure {:graph/version graph-version
                   :structure/key structure-key
                   :color-mode color-mode
                   :resources resources
                   :passes passes
                   :schedule {:policy :on-demand :clock-source :injected/monotonic
                              :causes legal-causes}}]
    (assoc structure :structure/hash (stable-hash structure))))

(defn- ranges-for [effect-spans cid]
  (->> effect-spans
       (filter #(= cid (:container/id %)))
       (mapcat :entry-ranges)
       distinct sort vec))

(defn bind-entry-ranges
  "Resolve the current arrangement against a reusable structure. This is run
   every frame; the returned plan is the only value that carries indices."
  [structure arrangement effect-spans]
  (let [entry-count (count arrangement)
        passes (mapv (fn [pass]
                       (cond-> pass
                         (:group/container-id pass)
                         (assoc :entry-ranges
                                (ranges-for effect-spans
                                            (:group/container-id pass)))
                         (= :flat/base (:pass/id pass))
                         (assoc :entry-ranges [[0 entry-count]])
                         (= :direct/main (:pass/id pass))
                         (assoc :entry-ranges [[0 entry-count]])))
                     (:passes structure))
        binding {:entry-count entry-count
                 :entry-ids (mapv :entry/id arrangement)
                 :passes passes}
        plan (assoc structure :passes passes :binding binding)]
    (assoc plan :plan/hash (stable-hash (dissoc plan :structure/hash)))))

(defn- duplicate-values [values]
  (->> values frequencies (keep (fn [[value n]] (when (> n 1) value))) vec))

(defn- produced-before [passes pass-index]
  (reduce (fn [produced pass]
            (let [attachments (keep :resource (vals (:attachments pass)))
                  resolves (keep :resolve-target (vals (:attachments pass)))
                  writes (:copy-writes pass)]
              (into produced (concat attachments resolves writes))))
          #{} (take pass-index passes)))

(defn- required-usage [mode]
  (case mode
    :sampled :texture-binding
    :copy :copy-src
    :readback :copy-src
    nil))

(defn- validate-pass! [resources passes pass-index pass]
  (when-not (contains? legal-pass-kinds (:pass/kind pass))
    (throw (ex-info "Unknown frame pass kind" {:pass pass})))
  (let [attachment-resources (set (keep :resource (vals (:attachments pass))))
        read-resources (set (map :resource (:reads pass)))
        aliases (set/intersection attachment-resources read-resources)
        prior (produced-before passes pass-index)]
    (when (seq aliases)
      (throw (ex-info "Pass samples and writes the same resource"
                      {:pass/id (:pass/id pass) :resources aliases})))
    (doseq [{:keys [resource producer mode] :as read} (:reads pass)]
      (when-not (contains? resources resource)
        (throw (ex-info "Pass reads an undeclared resource" {:read read})))
      (when-not (contains? prior resource)
        (throw (ex-info "Pass reads a resource before it is produced"
                        {:pass/id (:pass/id pass) :read read})))
      (when (and producer
                 (not (some #(= producer (:pass/id %)) (take pass-index passes))))
        (throw (ex-info "Pass producer edge is absent or not topologically prior"
                        {:pass/id (:pass/id pass) :read read})))
      (when-let [usage (required-usage mode)]
        (when-not (contains? (get-in resources [resource :usage]) usage)
          (throw (ex-info "Resource lacks usage required by read"
                          {:pass/id (:pass/id pass) :resource resource
                           :required usage})))))
    (doseq [[_ attachment] (:attachments pass)]
      (let [resource-id (:resource attachment)
            row (get resources resource-id)]
        (when-not row
          (throw (ex-info "Pass attachment is undeclared"
                          {:pass/id (:pass/id pass) :resource resource-id})))
        (when-not (contains? (:usage row) :render-attachment)
          (throw (ex-info "Attachment resource lacks render usage"
                          {:pass/id (:pass/id pass) :resource resource-id})))
        (when-not (= (:format row) (:format attachment))
          (throw (ex-info "Attachment format is incompatible with resource"
                          {:pass/id (:pass/id pass) :resource resource-id
                           :attachment-format (:format attachment)
                           :resource-format (:format row)})))
        (when-let [resolve-target (:resolve-target attachment)]
          (let [resolve-row (get resources resolve-target)]
            (when-not resolve-row
              (throw (ex-info "Region resolve target is undeclared"
                              {:pass/id (:pass/id pass)
                               :resource resolve-target})))
            (when-not (and (= :color (:kind resolve-row))
                           (= 1 (:sample-count resolve-row))
                           (contains? (:usage resolve-row) :texture-binding))
              (throw (ex-info "Region resolve target is not sampleable color"
                              {:pass/id (:pass/id pass)
                               :resource resolve-target
                               :row resolve-row})))))))
    (doseq [resource-id (:copy-writes pass)]
      (when-not (contains? (get-in resources [resource-id :usage]) :copy-dst)
        (throw (ex-info "Copy destination lacks copy-dst usage"
                        {:pass/id (:pass/id pass) :resource resource-id}))))))

(defn- validate-families! [families]
  (when-let [duplicates (seq (duplicate-values (map :family/id families)))]
    (throw (ex-info "Duplicate frame family ids" {:duplicates duplicates})))
  (doseq [family families]
    (when (and (:drawable? family true)
               (or (nil? (:geometry family)) (nil? (:pick family))))
      (throw (ex-info "Drawable family lacks geometry/pick ownership"
                      {:family/id (:family/id family)})))
    (when (and (= :overlay (:pass-class family))
               (not-every? #(contains? family %)
                           [:anchor-space :metric-space :hit-policy :depth-policy]))
      (throw (ex-info "Overlay family lacks anchor/metric/hit/depth policy"
                      {:family/id (:family/id family)})))
    (when (and (:clocked? family)
               (or (nil? (:clock family)) (nil? (:stop-predicate family))))
      (throw (ex-info "Clocked family lacks injected clock or stop condition"
                      {:family/id (:family/id family)})))
    (when (false? (:pick-order-derived? family true))
      (throw (ex-info "Family pick order is not derived from scene order"
                      {:family/id (:family/id family)})))
    (when (and (:hand-path? family) (:readback? family)
               (not (:declared-readback? family)))
      (throw (ex-info "Hand-path family performs undeclared readback"
                      {:family/id (:family/id family)})))))

(defn validate-plan!
  "Fail closed on W0-C section 5.5's ten laws. Returns the plan unchanged."
  [plan]
  (let [{:keys [resources passes families]} plan
        pass-ids (map :pass/id passes)
        ranks (map :topology-rank passes)]
    (when-let [duplicates (seq (duplicate-values pass-ids))]
      (throw (ex-info "Duplicate frame pass ids" {:duplicates duplicates})))
    (when-let [duplicates (seq (duplicate-values ranks))]
      (throw (ex-info "Unstable topological pass ties" {:duplicates duplicates})))
    (when-not (= (vec passes) (vec (sort-by :topology-rank passes)))
      (throw (ex-info "Pass list is not in stable topological order" {})))
    (doseq [[index pass] (map-indexed vector passes)]
      (validate-pass! resources passes index pass))
    (let [terminals (filter :presentation-terminal? passes)]
      (when-not (= 1 (count terminals))
        (throw (ex-info "Frame plan must have exactly one presentation terminal"
                        {:terminals (mapv :pass/id terminals)}))))
    (doseq [[resource-id row] resources]
      (when (or (nil? (:lifetime row)) (nil? (:budget-owner row)))
        (throw (ex-info "Enabled resource lacks lifetime/budget owner"
                        {:resource resource-id :row row})))
      (when-not (contains? scene-tape/frame-resource-kinds (:kind row))
        (throw (ex-info "Unknown frame resource kind"
                        {:resource resource-id :kind (:kind row)})))
      (when-not (contains? scene-tape/frame-resource-lifetimes (:lifetime row))
        (throw (ex-info "Unknown frame resource lifetime"
                        {:resource resource-id :lifetime (:lifetime row)}))))
    (validate-families! (or families []))
    (let [region-passes (filter #(= :region (:pass/kind %)) passes)
          region-ids (set (map :region/id region-passes))]
      (when (and (seq region-passes)
                 (not= :scene-color/linear (:color-mode plan)))
        (throw (ex-info "Region passes require whole-frame linear color"
                        {:color-mode (:color-mode plan)})))
      (doseq [region-id region-ids]
        (let [rows (filter #(= region-id (:region/id %)) region-passes)
              interiors (filter #(= :interior (:region/role %)) rows)
              resolves (set (keep :produces interiors))]
          (when-not (= 1 (count interiors))
            (throw (ex-info "Region requires exactly one interior producer"
                            {:region/id region-id
                             :interiors (mapv :pass/id interiors)})))
          (doseq [resolve-id resolves]
            (when-not (some (fn [pass]
                              (and (not= :region (:pass/kind pass))
                                   (some #(= resolve-id (:resource %))
                                         (:reads pass))))
                            passes)
              (throw (ex-info "Region resolve lacks a scene producer edge"
                              {:region/id region-id :resolve resolve-id}))))))
    plan)))

(defn compile-frame-plan [inputs]
  (let [structure (compile-plan-structure (structure-input inputs))
        plan (bind-entry-ranges structure (:arrangement inputs)
                                (:effect-spans inputs))]
    (validate-plan! plan)))

(defn empty-maintained-state []
  {:structure-key nil :structure nil :structure-compiles 0 :structure-reuses 0})

(defn maintain-frame-plan [state inputs]
  (let [state (or state (empty-maintained-state))
        key (structure-input inputs)
        reuse? (= key (:structure-key state))
        structure (if reuse? (:structure state) (compile-plan-structure key))
        plan (-> (bind-entry-ranges structure (:arrangement inputs)
                                    (:effect-spans inputs))
                 validate-plan!)]
    {:structure-key key
     :structure structure
     :plan plan
     :structure-compiles (+ (:structure-compiles state 0) (if reuse? 0 1))
     :structure-reuses (+ (:structure-reuses state 0) (if reuse? 1 0))
     :reused? reuse?}))

(defn- oracle-plan-structure
  "Fresh batch expansion kept separate from compile-plan-structure. Shared leaf
   constructors define vocabulary, while this independent orchestration is the
   executable fence against stale/reused structure."
  [key]
  (let [{:keys [effect-topology regions capabilities viewport color-mode]} key
        format (or (:format viewport) "bgra8unorm")
        linear? (= :scene-color/linear color-mode)
        copy? (and (not linear?) (contains? capabilities :copy-present))
        resources (merge (base-resources format linear? copy?)
                         (reduce merge {} (map region-resources regions))
                         (if linear?
                           (reduce merge {} (map group-resources effect-topology))
                           {}))
        passes
        (if linear?
          (let [region-passes (reduce-kv
                               (fn [result index region]
                                 (into result (region-passes region (* index 2))))
                               [] regions)
                base-rank (inc (* 2 (count regions)))
                region-reads (region-scene-reads regions)
                base {:pass/id :flat/base :pass/kind :render
                      :topology-rank base-rank
                      :reads region-reads
                      :attachments {:color (attachment :scene-color/main
                                                       "rgba16float" :clear :store)}}
                expanded (reduce-kv
                          (fn [result index row]
                            (into result
                                  (mapv #(update % :reads into region-reads)
                                        (group-passes
                                         effect-topology row
                                         (+ base-rank 10 (* index 10))))))
                          [] effect-topology)
                roots (filterv #(nil? (:parent/container-id %)) effect-topology)
                composite-rank (+ base-rank 10
                                  (* 10 (count effect-topology)))
                composite {:pass/id :flat/composite :pass/kind :render
                           :topology-rank composite-rank
                           :reads (mapv (fn [row]
                                          (read-edge
                                           (group-id "output" (:container/id row))
                                           (group-id "composite" (:container/id row))
                                           :sampled)) roots)
                           :attachments {:color (attachment :scene-color/main
                                                            "rgba16float" :load :store)}}
                present {:pass/id :present :pass/kind :present
                         :topology-rank (inc composite-rank)
                         :reads [(read-edge :scene-color/main :flat/composite
                                            :sampled)]
                         :attachments {:color (attachment :present format
                                                          :clear :store)}
                         :presentation-terminal? true}]
            (vec (concat region-passes [base] expanded [composite present])))
          (if copy?
            [{:pass/id :direct/main :pass/kind :render :topology-rank 0
              :reads []
              :attachments {:color (attachment :scene-color/main format
                                               :clear :store)}}
             {:pass/id :present :pass/kind :copy :topology-rank 1
              :reads [(read-edge :scene-color/main :direct/main :copy)]
              :copy-writes #{:present} :presentation-terminal? true}]
            [{:pass/id :direct/main :pass/kind :render :topology-rank 0
              :reads []
              :attachments {:color (attachment :present format :clear :store)}
              :presentation-terminal? true}]))
        structure {:graph/version graph-version :structure/key key
                   :color-mode color-mode :resources resources :passes passes
                   :schedule {:policy :on-demand
                              :clock-source :injected/monotonic
                              :causes legal-causes}}]
    (assoc structure :structure/hash (stable-hash structure))))

(defn oracle-compile-frame-plan
  "Independent batch compiler used only as the maintained road's executable
   oracle. It constructs a fresh structure from raw inputs and binds ranges;
   it does not call compile-plan-structure or maintain-frame-plan."
  [inputs]
  (let [key (structure-input inputs)
        fresh (oracle-plan-structure key)
        plan (-> (bind-entry-ranges fresh (:arrangement inputs)
                                    (:effect-spans inputs))
                 validate-plan!)]
    plan))

(defn plan-equivalent? [left right]
  (= (dissoc left :plan/hash) (dissoc right :plan/hash)))

(defn region-executable? [plan]
  (or (not-any? #(= :region (:pass/kind %)) (:passes plan))
      (= :scene-color/linear (:color-mode plan))))

(defn- stratum-ranges [arrangement stratum]
  (frame-effects/contiguous-ranges
   (keep-indexed (fn [index entry]
                   (when (= stratum (get-in entry [:order :stratum])) index))
                 arrangement)))

(defn compile-export-plan
  "Compile the v1 raster export variant. Only world-stratum entry ranges bind;
   the plan is always linear and its sole GPU-to-CPU edge is declared async."
  [{:keys [arrangement viewport]}]
  (let [format "rgba8unorm"
        resources {:export/scene
                   (resource :color "rgba16float"
                             #{:render-attachment :texture-binding}
                             :export :frame-runtime/export
                             :alpha-association :premultiplied
                             :working-space :linear-srgb)
                   :export/output
                   (resource :color format
                             #{:render-attachment :copy-src}
                             :export :frame-runtime/export
                             :alpha-association :premultiplied
                             :working-space :srgb)
                   :export/readback
                   (resource :buffer :rgba8-bytes #{:copy-dst :map-read}
                             :readback :frame-runtime/export
                             :async-only? true)}
        world-ranges (vec (or (stratum-ranges arrangement :world) []))
        passes [{:pass/id :export/world :pass/kind :render :topology-rank 0
                 :entry-ranges world-ranges :reads []
                 :attachments {:color (attachment :export/scene "rgba16float"
                                                  :clear :store)}}
                {:pass/id :export/transfer :pass/kind :render :topology-rank 1
                 :reads [(read-edge :export/scene :export/world :sampled)]
                 :attachments {:color (attachment :export/output format
                                                  :clear :store)}
                 :presentation-terminal? true}
                {:pass/id :export/readback :pass/kind :readback :topology-rank 2
                 :reads [(read-edge :export/output :export/transfer :readback)]
                 :copy-writes #{:export/readback} :async? true}]
        plan {:graph/version graph-version
              :variant :export :color-mode :scene-color/linear
              :viewport viewport :resources resources :passes passes
              :schedule {:policy :on-demand :causes #{:readback}}}]
    (assoc (validate-plan! plan) :plan/hash (stable-hash plan))))
