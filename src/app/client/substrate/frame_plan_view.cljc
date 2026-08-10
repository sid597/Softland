(ns app.client.substrate.frame-plan-view
  "Keyed, incrementally maintained frame-plan fragments.

   The durable order is a sorted subview of stable pass ids.  Dense numeric
   ranks exist only in the executor projection and are never plan identity."
  (:require [app.client.substrate.frame-graph :as frame-graph]))

(defn presentation-variant [color-mode format capabilities]
  (cond
    (= :scene-color/linear color-mode) :linear-present
    (contains? capabilities :copy-present) :legacy-copy-present
    :else :legacy-direct))

(defn- effect-rows [effects]
  (->> effects vals
       (sort-by (juxt (comp - :depth) (comp pr-str :container/id)))
       vec))

(defn- region-rows [regions]
  (->> regions vals (sort-by (comp pr-str :region/id)) vec))

(defn- road-values [effects regions globals]
  (let [rows (effect-rows effects)
        regions (region-rows regions)
        color-mode
        (frame-graph/select-color-mode
         {:effect-spans rows
          :regions regions
          :forced-color-mode (:forced-color-mode globals)})]
    {:color-mode color-mode
     :presentation-variant
     (presentation-variant color-mode (:viewport-format globals)
                           (:capabilities globals))}))

(defn- order-key [fragment pass index]
  (case (:fragment/kind fragment)
    :region [0 0 (pr-str (:fragment/key fragment)) index]
    :effect [2 (- (:depth fragment)) (pr-str (:fragment/key fragment)) index]
    :global (case (:pass/id pass)
              :flat/base [1 0 "base" 0]
              :direct/main [1 0 "direct" 0]
              :flat/composite [3 0 "composite" 0]
              :present [4 0 "present" 0]
              [4 0 (pr-str (:pass/id pass)) index])))

(defn- normalize-fragment [kind key depth fragment]
  (let [fragment (assoc fragment :fragment/kind kind :fragment/key key
                        :depth (or depth 0))]
    (assoc fragment
           :passes
           (mapv (fn [index pass]
                   (assoc pass :frame-plan/order-key
                          (order-key fragment pass index)))
                 (range) (:passes fragment)))))

(defn- region-fragment [row]
  (normalize-fragment :region (:region/id row) 0
                      (frame-graph/compile-region-fragment row)))

(defn- effect-fragment [effects row]
  (let [children (mapv effects (get (:children-by-parent effects)
                                    (:container/id row) #{}))
        ;; The map carries declarations and its child index under metadata-like
        ;; keys; only declaration rows reach the compiler.
        topology (into [row] (remove nil?) children)]
    (normalize-fragment :effect (:container/id row) (:depth row)
                        (frame-graph/compile-effect-fragment topology row))))

(defn- effects-with-child-index [effects children-by-parent]
  (assoc effects :children-by-parent children-by-parent))

(defn- global-fragment [effects regions globals road]
  (normalize-fragment
   :global :global 0
   (frame-graph/compile-global-fragment
    {:effect-topology (effect-rows effects)
     :regions (region-rows regions)
     :capabilities (:capabilities globals)
     :viewport {:format (:viewport-format globals)}
     :color-mode (:color-mode road)})))

(defn- children-index [effects]
  (reduce (fn [index [cid row]]
            (update index (:parent/container-id row) (fnil conj #{}) cid))
          {} effects))

(defn- remove-fragment [state fragment-id]
  (if-let [old (get-in state [:fragments fragment-id])]
    (let [pass-ids (map :pass/id (:passes old))
          resource-ids (keys (:resources old))]
      (-> state
          (update :fragments dissoc fragment-id)
          (update :passes #(apply dissoc % pass-ids))
          (update :order #(apply dissoc % (map :frame-plan/order-key
                                               (:passes old))))
          (update :resources #(apply dissoc % resource-ids))))
    state))

(defn- put-fragment [state fragment]
  (let [fragment-id (:fragment/id fragment)
        without-old (remove-fragment state fragment-id)]
    (-> without-old
        (assoc-in [:fragments fragment-id] fragment)
        (update :passes into (map (juxt :pass/id identity) (:passes fragment)))
        (update :order into
                (map (fn [pass]
                       [(:frame-plan/order-key pass) (:pass/id pass)]))
                (:passes fragment))
        (update :resources merge (:resources fragment)))))

(defn- materialize-plan [state]
  (let [passes (into []
                     (map-indexed
                      (fn [index [_ pass-id]]
                        (-> (get (:passes state) pass-id)
                            (dissoc :frame-plan/order-key)
                            (assoc :topology-rank index))))
                     (:order state))
        structure-key
        {:effect-topology (effect-rows (:effects state))
         :regions (region-rows (:regions state))
         :capabilities (into (sorted-set) (get-in state [:globals :capabilities]))
         :viewport {:format (get-in state [:globals :viewport-format])}
         :color-mode (:color-mode state)}
        generation (:generation state)
        plan {:graph/version frame-graph/graph-version
              :structure/key structure-key
              :structure/hash (str "fpv1-" generation)
              :color-mode (:color-mode state)
              :resources (:resources state)
              :passes passes
              :schedule {:policy :on-demand
                         :clock-source :injected/monotonic
                         :causes frame-graph/legal-causes}
              :plan/generation generation
              :plan/hash (str "fpv1-" generation)}]
    (assoc state :plan plan)))

(defn bootstrap [{:keys [effects regions globals]}]
  (let [effects (into {} (map (juxt :container/id identity)) effects)
        regions (into {} (map (juxt :region/id identity)) regions)
        children (children-index effects)
        road (road-values effects regions globals)
        initial {:effects effects :regions regions :globals globals
                 :children-by-parent children
                 :color-mode (:color-mode road)
                 :presentation-variant (:presentation-variant road)
                 :generation 1
                 :fragments {} :passes {} :resources {}
                 :order (sorted-map)}
        with-regions (reduce #(put-fragment %1 (region-fragment %2))
                             initial (region-rows regions))
        effects* (effects-with-child-index effects children)
        with-effects (reduce #(put-fragment %1 (effect-fragment effects* %2))
                             with-regions (effect-rows effects))
        complete (put-fragment with-effects
                               (global-fragment effects regions globals road))]
    (materialize-plan complete)))

(defn- patch-effect-parameters [state cid decl]
  (if-let [fragment (get-in state [:fragments [:fragment/effect cid]])]
    (let [opacity (get-in decl [:effects :opacity])
          passes (mapv (fn [pass]
                         (if (:composite pass)
                           (assoc-in pass [:composite :opacity] opacity)
                           pass))
                       (:passes fragment))]
      (put-fragment state (assoc fragment :passes passes)))
    state))

(defn- update-global-edge-fragment [state]
  ;; Rebuilding this three-pass fragment only projects the maintained root and
  ;; region edge sets; it never compiles or sorts another fragment.
  (let [road {:color-mode (:color-mode state)
              :presentation-variant (:presentation-variant state)}]
    (put-fragment state
                  (global-fragment (:effects state) (:regions state)
                                   (:globals state) road))))

(defn- local-topology-update
  [state container-deltas region-deltas]
  (let [old-effects (:effects state)
        old-regions (:regions state)
        next-effects
        (reduce (fn [rows delta]
                  (if-let [decl (:new-decl delta)]
                    (assoc rows (:container/id delta) decl)
                    (dissoc rows (:container/id delta))))
                old-effects (filter #(= :topology (:class %)) container-deltas))
        next-regions
        (reduce (fn [rows delta]
                  (if-let [row (:new delta)]
                    (assoc rows (:region/id delta) row)
                    (dissoc rows (:region/id delta))))
                old-regions region-deltas)
        children (children-index next-effects)
        state (assoc state :effects next-effects :regions next-regions
                     :children-by-parent children)
        effect-ids (into #{}
                         (mapcat (fn [delta]
                                   [(:container/id delta)
                                    (get-in delta [:old-decl :parent/container-id])
                                    (get-in delta [:new-decl :parent/container-id])]))
                         (filter #(= :topology (:class %)) container-deltas))
        effect-ids (disj effect-ids nil)
        region-ids (into #{} (map :region/id) region-deltas)
        effects* (effects-with-child-index next-effects children)
        state
        (reduce (fn [next-state cid]
                  (if-let [row (get next-effects cid)]
                    (put-fragment next-state (effect-fragment effects* row))
                    (remove-fragment next-state [:fragment/effect cid])))
                state effect-ids)
        state
        (reduce (fn [next-state region-id]
                  (if-let [row (get next-regions region-id)]
                    (put-fragment next-state (region-fragment row))
                    (remove-fragment next-state [:fragment/region region-id])))
                state region-ids)
        state (update-global-edge-fragment state)
        compiled (+ (count effect-ids) (count region-ids))
        visited (+ (reduce + 0
                           (map #(count (get-in state [:fragments % :passes]))
                                (concat (map (fn [cid] [:fragment/effect cid]) effect-ids)
                                        (map (fn [id] [:fragment/region id]) region-ids))))
                   2)]
    {:state state
     :compiled compiled
     :nodes visited
     :edges (+ (count effect-ids) (count region-ids))}))

(defn apply-deltas
  "Maintain only fragments named by explicit topology/global deltas.  Entry
   moves and payload/binding deltas have no plan consumer."
  [state {:keys [effects regions globals container-deltas
                 region-topology-deltas global-deltas]}]
  (if-not state
    (let [state (bootstrap {:effects effects :regions regions :globals globals})]
      {:state state
       :work {:plan-fragments-touched (count (:fragments state))
              :plan-order-nodes-visited (count (:passes state))
              :plan-order-edges-visited 0
              :plan-full-validations 0
              :global-transition true
              :bootstrap? true}})
    (let [container-deltas (vec (or container-deltas []))
          region-deltas (vec (or region-topology-deltas []))
          global-deltas (vec (or global-deltas []))
          parameter-deltas (filter #(= :parameter (:class %)) container-deltas)
          topology-deltas (filter #(= :topology (:class %)) container-deltas)
          next-effects
          (reduce (fn [rows delta]
                    (if-let [decl (:new-decl delta)]
                      (assoc rows (:container/id delta) decl)
                      (dissoc rows (:container/id delta))))
                  (:effects state) container-deltas)
          next-regions
          (reduce (fn [rows delta]
                    (if-let [row (:new delta)]
                      (assoc rows (:region/id delta) row)
                      (dissoc rows (:region/id delta))))
                  (:regions state) region-deltas)
          next-globals (if (seq global-deltas) globals (:globals state))
          next-road (road-values next-effects next-regions next-globals)
          mode-changed? (not= (:color-mode state) (:color-mode next-road))
          variant-changed? (not= (:presentation-variant state)
                                 (:presentation-variant next-road))
          format-changed? (not= (get-in state [:globals :viewport-format])
                                (:viewport-format next-globals))
          global-transition? (or mode-changed? variant-changed? format-changed?)
          plan-relevant? (or (seq container-deltas) (seq region-deltas)
                             (seq global-deltas))]
      (if-not plan-relevant?
        {:state state
         :work {:plan-fragments-touched 0
                :plan-order-nodes-visited 0
                :plan-order-edges-visited 0
                :plan-full-validations 0
                :global-transition false
                :bootstrap? false}}
        (let [base (assoc state :effects next-effects :regions next-regions
                          :globals next-globals
                          :color-mode (:color-mode next-road)
                          :presentation-variant (:presentation-variant next-road))
              result
              (if global-transition?
                (let [rebuilt (bootstrap {:effects (vals next-effects)
                                          :regions (vals next-regions)
                                          :globals next-globals})]
                  {:state (assoc rebuilt :generation (inc (:generation state)))
                   :compiled (count (:fragments rebuilt))
                   :nodes (count (:passes rebuilt))
                   :edges (reduce + 0 (map (comp count :reads val)
                                           (:passes rebuilt)))})
                (if (or (seq topology-deltas) (seq region-deltas))
                  (local-topology-update base topology-deltas region-deltas)
                  {:state base :compiled 0 :nodes 0 :edges 0}))
              state (reduce (fn [next-state delta]
                              (patch-effect-parameters
                               next-state (:container/id delta) (:new-decl delta)))
                            (:state result) parameter-deltas)
              state (-> state
                        (assoc :generation (if global-transition?
                                             (:generation state)
                                             (inc (:generation state))))
                        materialize-plan)]
          {:state state
           :work {:plan-fragments-touched (:compiled result 0)
                  :plan-order-nodes-visited (:nodes result 0)
                  :plan-order-edges-visited (:edges result 0)
                  :plan-full-validations 0
                  :global-transition global-transition?
                  :bootstrap? false}})))))

(defn plan [state] (:plan state))

(defn oracle-equal?
  [state {:keys [arrangement effect-spans regions globals]}]
  (let [oracle (frame-graph/oracle-compile-frame-plan
                {:arrangement arrangement
                 :effect-spans effect-spans
                 :regions regions
                 :capabilities (:capabilities globals)
                 :viewport {:format (:viewport-format globals)}
                 :forced-color-mode (:forced-color-mode globals)})
        normalize (fn [plan]
                    {:color-mode (:color-mode plan)
                     :resources (:resources plan)
                     :passes (mapv #(dissoc % :topology-rank :entry-ranges)
                                   (:passes plan))})]
    (= (normalize (:plan state)) (normalize oracle))))
