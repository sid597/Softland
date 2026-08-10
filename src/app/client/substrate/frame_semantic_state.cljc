(ns app.client.substrate.frame-semantic-state
  "One generation authority for arrangement, effect view, and plan view."
  (:require [app.client.substrate.frame-effect-view :as effect-view]
            [app.client.substrate.frame-plan-view :as plan-view]))

(defn apply-deltas
  "Apply one frame's already-minted semantic deltas.  Binding/payload deltas
   never call this reducer.  The returned `:state` is published with one swap."
  [state {:keys [old-arrangement new-arrangement entry-deltas
                 container-deltas region-topology-deltas global-deltas
                 registry regions globals]}]
  (let [entry-deltas (vec (or entry-deltas []))
        container-deltas (vec (or container-deltas []))
        region-topology-deltas (vec (or region-topology-deltas []))
        global-deltas (vec (or global-deltas []))
        semantic-deltas (into [] (concat entry-deltas container-deltas
                                         region-topology-deltas global-deltas))
        bootstrap? (nil? state)]
    (if (and state (empty? semantic-deltas))
      {:state state
       :work {:effect-containers-touched 0
              :container-declarations-inspected 0
              :plan-fragments-touched 0
              :plan-order-nodes-visited 0
              :plan-order-edges-visited 0
              :plan-full-validations 0
              :global-transition false}}
      (let [effect-result
            (effect-view/apply-deltas
             (:effect-view state)
             {:old-arrangement old-arrangement
              :new-arrangement new-arrangement
              :entry-deltas entry-deltas
              :container-deltas container-deltas
              :registry registry})
            effect-state (:state effect-result)
            effect-rows (effect-view/topology-rows effect-state)
            plan-result
            (plan-view/apply-deltas
             (:plan-view state)
             {:effects effect-rows
              :regions regions
              :globals globals
              :container-deltas container-deltas
              :region-topology-deltas region-topology-deltas
              :global-deltas global-deltas})
            generation (if bootstrap? 1 (inc (:generation state)))
            next-state
            {:arrangement new-arrangement
             :effect-view effect-state
             :plan-view (:state plan-result)
             :generation generation
             :globals globals
             :last-delta-receipt
             {:entry-deltas (count entry-deltas)
              :container-deltas (count container-deltas)
              :region-topology-deltas (count region-topology-deltas)
              :global-deltas (count global-deltas)}}]
        {:state next-state
         :work (merge (:work effect-result) (:work plan-result))}))))

(defn arrangement [state] (:arrangement state))
(defn effect-state [state] (:effect-view state))
(defn plan-state [state] (:plan-view state))
(defn plan [state] (some-> state :plan-view plan-view/plan))
