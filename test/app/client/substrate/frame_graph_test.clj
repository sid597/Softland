(ns app.client.substrate.frame-graph-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.frame-effects :as effects]
            [app.client.substrate.frame-graph :as graph]
            [app.client.substrate.region3d-scene :as region3d]
            [app.client.substrate.scene-tape :as scene-tape]
            [app.client.workspace.containers :as containers]))

(defn- entry [id stratum pass-class path]
  {:entry/id id :family/id :render.family/path
   :order {:stratum stratum :pass-class pass-class :stack-path path
           :part-rank 0 :stable-tie id}})

(defn- effect-fixture []
  (let [registry (-> (containers/empty-registry)
                     (containers/add-container
                      :outer {:layer 1 :sibling-rank 1
                              :effects {:opacity 0.5}})
                     (containers/add-container
                      :inner {:parent :outer :layer 2 :sibling-rank 2
                              :effects {:mask :mask-child :isolate? true}}))
        effective (containers/effective registry)
        arrangement [(entry :outer-a :world :direct
                            (:stack-path (get effective :outer)))
                     (entry :inner :world :direct
                            (:stack-path (get effective :inner)))
                     (entry :overlay :overlay :direct [[:overlay 0 0]])]
        spans (effects/derive-effect-spans registry arrangement)]
    {:registry registry :arrangement arrangement :effect-spans spans
     :viewport {:width 320 :height 200 :format "bgra8unorm"}}))

(deftest plan-structure-is-deterministic-colored-by-declarations-and-range-fresh
  (let [fixture (effect-fixture)
        plan-a (graph/compile-frame-plan fixture)
        plan-b (graph/compile-frame-plan fixture)
        effectless (graph/compile-frame-plan
                    {:arrangement (:arrangement fixture) :effect-spans []
                     :capabilities #{:live-atoms}
                     :viewport (:viewport fixture)})
        copy-plan (graph/compile-frame-plan
                   {:arrangement (:arrangement fixture) :effect-spans []
                    :capabilities #{:copy-present}
                    :viewport (:viewport fixture)})
        inserted (update fixture :arrangement
                         #(vec (concat [(entry :before :world :direct
                                              [[:before -1 -1]])] %)))
        inserted (assoc inserted :effect-spans
                        (effects/derive-effect-spans (:registry fixture)
                                                     (:arrangement inserted)))
        inserted-plan (graph/compile-frame-plan inserted)
        oracle (graph/oracle-compile-frame-plan inserted)
        removed (-> inserted
                    (update :arrangement #(vec (rest %)))
                    (assoc :effect-spans (:effect-spans fixture)))
        removed-plan (graph/compile-frame-plan removed)
        removed-oracle (graph/oracle-compile-frame-plan removed)]
    (is (= plan-a plan-b))
    (is (= (:passes plan-a)
           (:passes (graph/compile-frame-plan
                     (update fixture :arrangement
                             #(vec (sort scene-tape/compare-order %)))))))
    (is (= (:plan/hash plan-a) (:plan/hash plan-b)))
    (is (= :scene-color/linear (:color-mode plan-a)))
    (is (= "rgba16float" (get-in plan-a [:resources :scene-color/main :format])))
    (is (= :legacy (:color-mode effectless)))
    (is (= [:direct/main] (mapv :pass/id (:passes effectless))))
    (is (= [:direct/main :present] (mapv :pass/id (:passes copy-plan))))
    (is (= (:structure/hash plan-a) (:structure/hash inserted-plan)))
    (is (not= (:binding plan-a) (:binding inserted-plan)))
    (is (graph/plan-equivalent? inserted-plan oracle))
    (is (graph/plan-equivalent? removed-plan removed-oracle))
    (is (= [:inner :outer]
           (->> (:passes plan-a)
                (filter #(and (:group/container-id %)
                              (= :render (:pass/kind %))
                              (re-find #"render" (name (:pass/id %)))))
                (map :group/container-id)
                distinct vec)))))

(deftest retired-maintained-frame-plan-stays-absent
  (is (nil? (ns-resolve 'app.client.substrate.frame-graph
                        'maintain-frame-plan)))
  (is (nil? (ns-resolve 'app.client.substrate.frame-graph
                        'empty-maintained-state))))

(deftest clip-mode-fails-closed-to-the-mask-road
  (is (= :scissor (graph/clip-execution-mode [2.0 0.0 0.0 2.0 4.0 5.0])))
  (is (= :mask (graph/clip-execution-mode [0.9 0.2 -0.2 0.9 4.0 5.0])))
  (is (= scene-tape/frame-pass-kinds graph/legal-pass-kinds)))

(deftest registration-order-does-not-change-plan-identity
  (let [spec-a {:layer 1 :sibling-rank 1 :effects {:opacity 0.8}}
        spec-b {:layer 2 :sibling-rank 2 :effects {:isolate? true}}
        reg-ab (-> (containers/empty-registry)
                   (containers/add-container :a spec-a)
                   (containers/add-container :b spec-b))
        reg-ba (-> (containers/empty-registry)
                   (containers/add-container :b spec-b)
                   (containers/add-container :a spec-a))
        paths (containers/effective reg-ab)
        arrangement [(entry :a :world :direct (:stack-path (get paths :a)))
                     (entry :b :world :direct (:stack-path (get paths :b)))]
        inputs (fn [registry]
                 {:arrangement arrangement
                  :effect-spans (effects/derive-effect-spans registry arrangement)
                  :viewport {:width 64 :height 64 :format "bgra8unorm"}})]
    (is (= (graph/compile-frame-plan (inputs reg-ab))
           (graph/compile-frame-plan (inputs reg-ba))))))

(deftest remaining-frame-plan-validation-laws-reject-their-wrong-builds
  (let [valid (graph/compile-frame-plan
               {:arrangement [] :effect-spans []
                :viewport {:width 10 :height 10 :format "bgra8unorm"}})
        valid-family {:family/id :family/ok :drawable? true
                      :geometry :owned}
        cycle-resource {:kind :color :format "bgra8unorm"
                        :usage #{:render-attachment :texture-binding}
                        :lifetime :frame :budget-owner :test}
        cycle-plan
        {:resources {:cycle/a cycle-resource :cycle/b cycle-resource}
         :passes [{:pass/id :cycle/a :pass/kind :render :topology-rank 0
                   :reads [{:resource :cycle/b :producer :cycle/b
                            :mode :sampled}]
                   :attachments {:color {:resource :cycle/a
                                         :format "bgra8unorm"
                                         :load :clear :store :store}}}
                  {:pass/id :cycle/b :pass/kind :render :topology-rank 1
                   :reads [{:resource :cycle/a :producer :cycle/a
                            :mode :sampled}]
                   :attachments {:color {:resource :cycle/b
                                         :format "bgra8unorm"
                                         :load :clear :store :store}}
                   :presentation-terminal? true}]}
        invalids
        [;; 1 duplicate IDs
         (update valid :passes conj (first (:passes valid)))
         ;; 2a graph cycle
         cycle-plan
         ;; 2b unstable topological tie
         (assoc valid :passes
                [(first (:passes valid))
                 (-> (first (:passes valid))
                     (assoc :pass/id :direct/tied)
                     (dissoc :presentation-terminal?))])
         ;; 3 sampled read/write alias + illegal edge/usage
         (-> valid
             (assoc-in [:passes 0 :reads]
                       [{:resource :present :producer :none :mode :sampled}]))
         ;; 4 zero presentation terminals
         (update valid :passes #(mapv (fn [p] (dissoc p :presentation-terminal?)) %))
         ;; 5 drawable ownership
         (assoc valid :families [(dissoc valid-family :geometry)])
         ;; 6 overlay policy
         (assoc valid :families [(assoc valid-family :pass-class :overlay)])
         ;; 7 clock injection/stop
         (assoc valid :families [(assoc valid-family :clocked? true)])
         ;; 8 undeclared hand readback
         (assoc valid :families [(assoc valid-family :hand-path? true
                                        :readback? true)])
         ;; 9 lifetime/budget ownership
         (assoc-in valid [:resources :present :budget-owner] nil)]]
    (is (= 10 (count invalids)))
    (doseq [invalid invalids]
      (is (thrown? clojure.lang.ExceptionInfo (graph/validate-plan! invalid))))))

(deftest export-is-world-only-linear-and-region-door-is-now-executable
  (let [{:keys [arrangement viewport]} (effect-fixture)
        export (graph/compile-export-plan {:arrangement arrangement
                                           :viewport viewport})
        region-entry
        (region3d/tape-entry
         {:region-id :region/test :revision 1
          :source-order {:stack-path [[:root 1 1]]}
          :rect [0 0 320 200]})
        region-plan (graph/compile-frame-plan
                     {:arrangement [region-entry]
                      :effect-spans []
                      :regions [{:region/id :region/test :shadow? true}]
                      :viewport viewport})
        region-passes (filterv #(= :region (:pass/kind %))
                               (:passes region-plan))
        legacy-region (assoc region-plan :color-mode :legacy)]
    (is (= :scene-color/linear (:color-mode export)))
    (is (= [[0 2]] (get-in export [:passes 0 :entry-ranges])))
    (is (true? (get-in export [:passes 2 :async?])))
    (is (= #{:readback} (get-in export [:schedule :causes])))
    (is (= :scene-color/linear (:color-mode region-plan)))
    (is (not-any? #(contains? (:paint region-entry) %)
                  [:lease-size :shadow? :composite-index :background]))
    (is (= [:shadow :interior] (mapv :region/role region-passes)))
    (is (= 2 (count (filter #(= :depth (:kind (val %)))
                            (:resources region-plan)))))
    (is (every? #(= :held (:lifetime (val %)))
                (filter #(re-find #"region3d" (name (key %)))
                        (:resources region-plan))))
    (is (true? (graph/region-executable? region-plan)))
    (is (false? (graph/region-executable? legacy-region)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"whole-frame linear"
                          (graph/validate-plan! legacy-region)))))
