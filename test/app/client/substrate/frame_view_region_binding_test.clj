(ns app.client.substrate.frame-view-region-binding-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.frame-delta :as frame-delta]
            [app.client.substrate.frame-effect-view :as effect-view]
            [app.client.substrate.frame-effects :as frame-effects]
            [app.client.substrate.frame-inputs :as frame-inputs]
            [app.client.substrate.frame-plan-view :as plan-view]
            [app.client.substrate.frame-semantic-state :as semantic-state]
            [app.client.substrate.scene-tape :as scene-tape]
            [app.client.workspace.containers :as containers]))

(defn- entry [id path pass-class]
  {:entry/id id :family/id :render.family/path
   :order {:stratum :world :pass-class pass-class :stack-path path
           :part-rank 0 :stable-tie id}})

(defn- arrangement [entries]
  {:ordered
   (into (sorted-map-by scene-tape/entry-key-compare)
         (map (juxt scene-tape/entry-key identity)) entries)
   :keys-by-family {}})

(def globals {:viewport-format "bgra8unorm"
              :capabilities #{} :forced-color-mode nil})

(deftest s1-binding-only-crossing-preserves-the-whole-semantic-generation
  (let [entries (mapv (fn [index]
                        (entry (keyword (str "entry-" index))
                               [[:root index index]] :direct))
                      (range 256))
        stable-arrangement (arrangement entries)
        registry (containers/empty-registry)
        boot (:state
              (semantic-state/apply-deltas
               nil {:old-arrangement stable-arrangement
                    :new-arrangement stable-arrangement
                    :registry registry :regions [{:region/id :r1 :shadow? false}]
                    :globals globals}))
        _ (frame-inputs/begin-ledger!)
        crossing
        (semantic-state/apply-deltas
         boot {:old-arrangement stable-arrangement
               :new-arrangement stable-arrangement
               ;; A lease/payload delta is deliberately absent: this reducer
               ;; has no binding-lane input by construction.
               :registry registry :regions [{:region/id :r1 :shadow? false}]
               :globals globals})
        ledger (frame-inputs/ledger-receipt)]
    (is (= 256 (count entries)))
    (is (identical? boot (:state crossing)))
    (is (identical? (semantic-state/effect-state boot)
                    (semantic-state/effect-state (:state crossing))))
    (is (identical? (semantic-state/plan-state boot)
                    (semantic-state/plan-state (:state crossing))))
    (is (= 0 (get-in crossing [:work :plan-fragments-touched])))
    (is (= 0 (get-in crossing [:work :effect-containers-touched])))
    (is (= #{} (:changed-families ledger)))
    (is (= 0 (:produced ledger)))
    (is (= 0 (:arrangement-upserts ledger)))
    (is (= 0 (:arrangement-removes ledger)))
    (is (= 0 (:comparator-calls ledger)))
    (is (= 0 (:plan-full-validations ledger)))
    (is (= {:path-zoom-regime
            {:version :frame-input/path-zoom-regime-v1
             :input-key :path-zoom-regime}
            :connector-zoom-regime
            {:version :frame-input/connector-zoom-regime-v1
             :input-key :connector-zoom-regime}
            :region-interior-encode
            {:version :frame-input/region-interior-encode-v1
             :input-key :region-encode-scale
             :step 1.12}}
           frame-inputs/quantization-doors))
    (is (not (contains? frame-inputs/quantization-doors
                        :region-admission-rung)))))

(deftest s2-foreign-interleaving-splits-and-merges-the-neighbor-container
  (let [registry (-> (containers/empty-registry)
                     (containers/add-container
                      :a {:layer 1 :sibling-rank 1
                          :effects {:opacity 0.5}})
                     (containers/add-container
                      :c {:layer 3 :sibling-rank 3
                          :effects {:isolate? true}}))
        paths (containers/effective registry)
        a-path (:stack-path (get paths :a))
        c-path (:stack-path (get paths :c))
        a1 (entry :a1 a-path :direct)
        foreign (entry :foreign [[:zz 9 9]] :direct)
        a2 (entry :a2 a-path :intermediate)
        c1 (entry :c1 c-path :overlay)
        before (arrangement [a1 a2 c1])
        after (arrangement [a1 foreign a2 c1])
        boot (effect-view/bootstrap registry before)
        untouched-c (get-in boot [:containers :c])
        insert-delta (frame-delta/entry-delta
                      :insert nil foreign nil (scene-tape/entry-key foreign))
        inserted
        (effect-view/apply-deltas
         boot {:old-arrangement before :new-arrangement after
               :entry-deltas [insert-delta] :container-deltas []})
        inserted-state (:state inserted)
        spans (effect-view/project-spans inserted-state after)
        a-span (first (filter #(= :a (:container/id %)) spans))
        remove-delta (frame-delta/entry-delta
                      :remove foreign nil (scene-tape/entry-key foreign) nil)
        removed
        (effect-view/apply-deltas
         inserted-state {:old-arrangement after :new-arrangement before
                         :entry-deltas [remove-delta] :container-deltas []})
        merged-a (->> (effect-view/project-spans (:state removed) before)
                      (filter #(= :a (:container/id %))) first)]
    (is (contains? (get-in inserted [:work :touched-container-ids]) :a))
    (is (= [[0 1] [2 3]] (:entry-ranges a-span)))
    (is (= [[0 2]] (:entry-ranges merged-a)))
    (is (identical? untouched-c (get-in inserted-state [:containers :c])))
    (is (effect-view/oracle-equal? inserted-state registry after))
    (is (effect-view/oracle-equal? (:state removed) registry before))))

(deftest s3-parameter-topology-and-road-values-take-different-roads
  (let [empty (plan-view/bootstrap {:effects [] :regions [] :globals globals})
        registry-1 (containers/add-container
                    (containers/empty-registry) :a
                    {:layer 1 :sibling-rank 1 :effects {:opacity 0.5}})
        decl-1 (frame-delta/container-declaration registry-1 :a)
        topology (frame-delta/container-delta :a nil decl-1)
        first-effect
        (plan-view/apply-deltas
         empty {:effects [decl-1] :regions [] :globals globals
                :container-deltas [topology]})
        registry-2 (containers/set-effects registry-1 :a {:opacity 0.7})
        decl-2 (frame-delta/container-declaration registry-2 :a)
        parameter (frame-delta/container-delta :a decl-1 decl-2)
        parameter-result
        (plan-view/apply-deltas
         (:state first-effect)
         {:effects [decl-2] :regions [] :globals globals
          :container-deltas [parameter]})
        region-delta (frame-delta/region-topology-delta
                      :r1 :open nil {:region/id :r1 :shadow? false})
        region-result
        (plan-view/apply-deltas
         (:state parameter-result)
         {:effects [decl-2] :regions [{:region/id :r1 :shadow? false}]
          :globals globals :region-topology-deltas [region-delta]})
        a-entry (entry :a1 (:stack-path decl-2) :direct)
        effect-span (assoc (dissoc decl-2 :topology-signature)
                           :entry-ranges [[0 1]] :entry-count 1
                           :derived-effects
                           (frame-effects/derived-effect-declarations
                            (:effects decl-2)))]
    (is (= :topology (:class topology)))
    (is (= :parameter (:class parameter)))
    (is (= :scene-color/linear
           (get-in first-effect [:state :color-mode])))
    (is (true? (get-in first-effect [:work :global-transition])))
    (is (= 0 (get-in parameter-result [:work :plan-fragments-touched])))
    (is (= 0 (get-in parameter-result [:work :plan-order-nodes-visited])))
    (is (= 1 (get-in region-result [:work :plan-fragments-touched])))
    (is (false? (get-in region-result [:work :global-transition])))
    (is (= :scene-color/linear (get-in region-result [:state :color-mode])))
    (is (some #(= :interior (:region/role %))
              (get-in region-result [:state :plan :passes])))
    (is (plan-view/oracle-equal?
         (:state region-result)
         {:arrangement [a-entry] :effect-spans [effect-span]
          :regions [{:region/id :r1 :shadow? false}]
          :globals globals}))))

(deftest s4-region-fragments-are-keyed-and-entry-reorder-does-not-compile-plan
  (let [r1 {:region/id :r1 :shadow? false}
        r2 {:region/id :r2 :shadow? false}
        state (plan-view/bootstrap {:effects [] :regions [r1] :globals globals})
        open-r2 (plan-view/apply-deltas
                 state {:effects [] :regions [r1 r2] :globals globals
                        :region-topology-deltas
                        [(frame-delta/region-topology-delta :r2 :open nil r2)]})
        reorder (plan-view/apply-deltas
                 (:state open-r2)
                 {:effects [] :regions [r2 r1] :globals globals})
        shadow-r1 (assoc r1 :shadow? true)
        shadow (plan-view/apply-deltas
                (:state reorder)
                {:effects [] :regions [shadow-r1 r2] :globals globals
                 :region-topology-deltas
                 [(frame-delta/region-topology-delta
                   :r1 :shadow-flip r1 shadow-r1)]})]
    (is (= 1 (get-in open-r2 [:work :plan-fragments-touched])))
    (is (= 0 (get-in reorder [:work :plan-fragments-touched])))
    (is (identical? (:state open-r2) (:state reorder)))
    (is (= 1 (get-in shadow [:work :plan-fragments-touched])))
    (is (= [:shadow :interior]
           (->> (get-in shadow [:state :plan :passes])
                (filter #(= :r1 (:region/id %)))
                (mapv :region/role))))))

(deftest s5-capability-recovery-transitions-variant-without-changing-mode
  (let [state (plan-view/bootstrap {:effects [] :regions [] :globals globals})
        recovered-globals (assoc globals :capabilities #{:copy-present})
        result (plan-view/apply-deltas
                state {:effects [] :regions [] :globals recovered-globals
                       :global-deltas
                       [{:delta/kind :global :field :capabilities
                         :old #{} :new #{:copy-present}}]})]
    (is (= :legacy (get-in result [:state :color-mode])))
    (is (= :legacy-copy-present
           (get-in result [:state :presentation-variant])))
    (is (true? (get-in result [:work :global-transition])))
    (is (= [:direct/main :present]
           (mapv :pass/id (get-in result [:state :plan :passes]))))))
