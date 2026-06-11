(ns app.server.rama.dogfood-space-test
  (:require [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.dogfood.space :as space]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(defn with-space-runtime
  [f]
  (let [runtime (space/start-space-runtime!)]
    (try
      (f runtime)
      (finally
        (space/close-space-runtime! runtime)))))

(defn append-and-await-decision!
  [runtime request]
  (space/append-space-action! runtime request)
  (space/await-decision runtime (:request/id request)))

(defn append-send-and-await-run!
  [runtime {:keys [space-id turn-id bundle-id run-id thread-id request-id]}]
  (let [request (space/compose-and-send-request
                  space-id
                  (str "Prompt for " run-id)
                  {:request-id request-id
                   :time-ms 100
                   :payload {:turn/id turn-id
                             :context-bundle/id bundle-id
                             :llm-turn-run/id run-id
                             :llm-thread/id thread-id
                             :executor/task-id llm/pending-task-id}})]
    (append-and-await-decision! runtime request)
    (llm/await-run runtime run-id #(= :pending (:status %)))
    request))

;; The LLM kernel only folds observations that carry the granted claim's
;; proof (executor id + claim token). Tests that stream observations claim
;; the run first with a per-run deterministic token (fixed claimed-at so the
;; writer-asymmetry snapshots stay byte-identical across rebuilds).
(def test-executor-id "space-test-executor")
(defn test-claim-token [run-id] (str "test-claim-" run-id))

(defn claim-test-run!
  [runtime run-id thread-id]
  (llm/append-claim!
    runtime
    (llm/claim-record run-id thread-id test-executor-id
                      {:claim-token (test-claim-token run-id)
                       :claimed-at-ms 150
                       :executor-task-id llm/pending-task-id}))
  (llm/await-run runtime run-id #(= :claimed (:status %))))

(defn test-claim-proof [run-id]
  {:executor/id test-executor-id
   :claim/token (test-claim-token run-id)})

(defn append-approval-observation!
  [runtime {:keys [run-id thread-id approval-id native-id]}]
  (llm/append-observation!
    runtime
    (llm/observation
      run-id
      thread-id
      :codex/approval-request
      0
      (merge (test-claim-proof run-id)
             {:observation-id (str approval-id "/obs")
              :approval/id approval-id
              :approval/type :exec
              :native/json-rpc-request-id native-id
              :codex/event-method "item/cmdExec/requestApproval"
              :codex/event-params {:cmd "echo approval"}})))
  (llm/await-materialized
    #(llm/read-pending-approval runtime approval-id)
    some?))

(defn append-raw-item-observation!
  [runtime {:keys [run-id thread-id item-id text sequence]}]
  (llm/append-observation!
    runtime
    (llm/observation
      run-id
      thread-id
      :codex/item-completed
      sequence
      (merge (test-claim-proof run-id)
             {:observation-id (str item-id "/obs/" sequence)
              :llm-item/id item-id
              :content/text text})))
  (llm/await-run runtime run-id #(<= (long sequence) (long (:last-seq %))))
  (llm/await-materialized #(llm/read-item-by-id runtime item-id) some?))

(defn relation-targets
  [relations]
  (set (map :to/object-id (vals (:out relations)))))

(defn relation-sources
  [relations]
  (set (map :from/object-id (vals (:in relations)))))

(defn materialized-projection-snapshot
  []
  (with-space-runtime
    (fn [runtime]
      (let [request (space/compose-and-send-request
                      "chat-projection"
                      "Render the projections."
                      {:request-id "req-projection"
                       :time-ms 17
                       :title "Projection chat"
                       :payload {:turn/id "WT-projection"
                                 :context-bundle/id "B-projection"
                                 :llm-turn-run/id "run-projection"
                                 :llm-thread/id "llm-thread-projection"
                                 :executor/task-id llm/pending-task-id}})
            _ (append-and-await-decision! runtime request)
            _ (llm/await-run runtime "run-projection" #(= :pending (:status %)))
            thread-object-id (space/catalog-object-id :space "chat-projection")
            turn-object-id (space/catalog-object-id :turn "WT-projection")
            bundle-object-id (space/catalog-object-id :context-bundle "B-projection")
            run-object-id (space/catalog-object-id :llm-turn-run "run-projection")]
        {:chat-canvas (space/await-materialized
                        #(space/read-chat-canvas-projection runtime "chat-projection")
                        #(= ["WT-projection"] (:turn-order %)))
         :run-detail (llm/await-materialized
                       #(llm/read-run-detail-projection runtime "run-projection")
                       #(= :pending (:status %)))
         :object-detail {:thread (space/await-materialized
                                   #(space/read-object-detail-projection runtime thread-object-id)
                                   #(= :space (:object/type %)))
                         :turn (space/await-materialized
                                 #(space/read-object-detail-projection runtime turn-object-id)
                                 #(= :turn (:object/type %)))
                         :bundle (space/await-materialized
                                   #(space/read-object-detail-projection runtime bundle-object-id)
                                   #(= :context-bundle (:object/type %)))
                         :run (space/await-materialized
                                #(space/read-object-detail-projection runtime run-object-id)
                                #(= :llm-turn-run (:object/type %)))}
         :relations {:thread (space/await-materialized
                               #(space/read-object-relations-projection runtime thread-object-id)
                               #(= #{turn-object-id} (relation-targets %)))
                     :turn (space/await-materialized
                             #(space/read-object-relations-projection runtime turn-object-id)
                             #(and (= #{thread-object-id} (relation-sources %))
                                   (= #{bundle-object-id run-object-id}
                                      (relation-targets %))))
                     :bundle (space/await-materialized
                               #(space/read-object-relations-projection runtime bundle-object-id)
                               #(and (= #{turn-object-id} (relation-sources %))
                                     (= #{run-object-id} (relation-targets %))))
                     :run (space/await-materialized
                            #(space/read-object-relations-projection runtime run-object-id)
                            #(= #{turn-object-id bundle-object-id}
                                (relation-sources %)))}}))))

(def space-replay-ids
  {:space-id "chat-space-property"
   :turn-id "WT-space-property-send"
   :bundle-id "B-space-property"
   :run-id "run-space-property"
   :thread-id "llm-thread-space-property"
   :request-id "req-space-property-send"
   :item-id "item-space-property"
   :control-id "req-space-property-compact/llm-control"
   :slice-id "slice-space-property"})

(defn space-property-empty-snapshot
  [runtime]
  (let [{:keys [space-id run-id thread-id item-id control-id slice-id]} space-replay-ids]
    {:thread (space/read-space runtime space-id)
     :chat-canvas (space/read-chat-canvas-projection runtime space-id)
     :llm-run (llm/read-run runtime run-id)
     :llm-thread (llm/read-thread runtime thread-id)
     :llm-run-detail (llm/read-run-detail-projection runtime run-id)
     :llm-cost (llm/read-cost-by-thread runtime thread-id)
     :llm-control (llm/read-control runtime control-id)
     :raw-object (space/read-object runtime (space/catalog-object-id :llm-item item-id))
     :slice (space/read-slice runtime slice-id)}))

(defn materialized-space-replay-snapshot
  []
  (with-space-runtime
    (fn [runtime]
      (let [{:keys [space-id turn-id bundle-id run-id thread-id
                    request-id item-id control-id slice-id]} space-replay-ids
            send-request (space/compose-and-send-request
                           space-id
                           "Replay this space input."
                           {:request-id request-id
                            :time-ms 300
                            :title "Space property"
                            :payload {:turn/id turn-id
                                      :context-bundle/id bundle-id
                                      :llm-turn-run/id run-id
                                      :llm-thread/id thread-id
                                      :executor/task-id llm/pending-task-id}})
            item-observation (llm/observation
                               run-id
                               thread-id
                               :codex/item-completed
                               0
                               (merge (test-claim-proof run-id)
                                      {:observation-id "obs-space-property-item"
                                       :received-at-ms 310
                                       :llm-item/id item-id
                                       :content/text "Space property item."
                                       :raw/json {:event "item/completed"}}))
            usage-observation (llm/observation
                                run-id
                                thread-id
                                :codex/token-usage
                                1
                                (merge (test-claim-proof run-id)
                                       {:observation-id "obs-space-property-usage"
                                        :received-at-ms 311
                                        :tokens/input-total 21
                                        :tokens/cached-input 8
                                        :tokens/output 5
                                        :tokens/reasoning-output 3}))
            compact-request (space/space-turn-request
                              :turn/compact-request
                              space-id
                              {:request-id "req-space-property-compact"
                               :time-ms 320
                               :payload {:turn/id "WT-space-property-compact"
                                         :llm-turn-run/id run-id
                                         :strategy :summarize-prefix}})
            slice-request (space/space-turn-request
                            :turn/slice-create
                            space-id
                            {:request-id "req-space-property-slice"
                             :time-ms 330
                             :payload {:turn/id "WT-space-property-slice"
                                       :slice/id slice-id
                                       :snapshot/text "Space property item."
                                       :source {:llm-item/id item-id
                                                :llm-turn-run/id run-id
                                                :llm-thread/id thread-id
                                                :content/text "Space property item."
                                                :content/hash (space/content-hash
                                                                "Space property item.")}}})
            thread-object-id (space/catalog-object-id :space space-id)
            turn-object-id (space/catalog-object-id :turn turn-id)
            raw-object-id (space/catalog-object-id :llm-item item-id)
            slice-object-id (space/catalog-object-id :slice slice-id)]
        (append-and-await-decision! runtime send-request)
        (llm/await-run runtime run-id #(= :pending (:status %)))
        (claim-test-run! runtime run-id thread-id)
        (llm/append-observation! runtime item-observation)
        (llm/await-materialized #(llm/read-item-by-id runtime item-id) some?)
        (llm/append-observation! runtime usage-observation)
        (llm/await-materialized
          #(llm/read-cost-by-thread runtime thread-id)
          #(= 21 (get-in % [:tokens :tokens/input-total])))
        (append-and-await-decision! runtime compact-request)
        (llm/await-materialized #(llm/read-control runtime control-id) some?)
        ;; microbatch: the control-by-id write and the run-row write land on
        ;; different tasks with no cross-task visibility order — await the run
        ;; row itself so both rebuilds snapshot the same folded state
        (llm/await-run runtime run-id #(seq (:compactions %)))
        (append-and-await-decision! runtime slice-request)
        (space/await-materialized #(space/read-slice runtime slice-id) some?)
        ;; the slice-create flow also promotes the source llm-item into the
        ;; object catalog on its own partition — await it so both property
        ;; rebuilds snapshot the same state
        (space/await-materialized #(space/read-object runtime raw-object-id) some?)
        {:space {:thread (space/read-space runtime space-id)
                 :turn-order (space/read-turns-by-space runtime space-id)
                 :bundle (space/read-context-bundle runtime bundle-id)
                 :slice (space/read-slice runtime slice-id)
                 :control (space/read-llm-control runtime control-id)}
         :llm {:run (llm/read-run runtime run-id)
               :view (llm/read-view runtime run-id)
               :run-detail (llm/read-run-detail-projection runtime run-id)
               :cost (llm/read-cost-by-thread runtime thread-id)
               :control (llm/read-control runtime control-id)
               :items (llm/read-items-by-run runtime run-id)}
         :catalog {:thread (space/read-object runtime thread-object-id)
                   :turn (space/read-object runtime turn-object-id)
                   :raw (space/read-object runtime raw-object-id)
                   :slice (space/read-object runtime slice-object-id)}
         :projections {:chat-canvas (space/read-chat-canvas-projection runtime space-id)
                       :thread-detail (space/read-object-detail-projection runtime thread-object-id)
                       :raw-detail (space/read-object-detail-projection runtime raw-object-id)
                       :slice-detail (space/read-object-detail-projection runtime slice-object-id)
                       :thread-relations (space/read-object-relations-projection runtime thread-object-id)
                       :raw-relations (space/read-object-relations-projection runtime raw-object-id)
                       :slice-relations (space/read-object-relations-projection runtime slice-object-id)}}))))

(deftest turn-control-keyword-discriminator-test
  (testing "space requests discriminate overlapping turn keywords by request/type"
    (doseq [request-type [:turn/cancel :turn/steer]]
      (let [request (space/space-turn-request
                      request-type
                      (str "chat-" (name request-type))
                      {:request-id (str "req-" (name request-type))
                       :payload {:turn/id (str "WT-" (name request-type))
                                 :llm-turn-run/id (str "run-" (name request-type))}})]
        (is (empty? (space/request-validation-errors
                      (assoc request :control/type :not-the-discriminator))))
        (is (= request-type (:request/type request))))))

  (testing "LLM controls discriminate by control/type and run requests reject turn controls"
    (doseq [control-type [:turn/cancel :turn/steer]]
      (let [run-id (str "run-" (name control-type))
            control (llm/control-record run-id control-type)
            run-request (llm/turn-run-request
                          "chat-discriminator"
                          (str "WT-" (name control-type))
                          (str "B-" (name control-type))
                          {:llm-turn-run-id run-id})
            rejected-run-request (assoc run-request :request/type control-type)
            error-types (set (map :type
                                  (llm/request-validation-errors rejected-run-request)))]
        (is (llm/valid-control? control))
        (is (empty? (llm/request-validation-errors run-request)))
        (is (contains? error-types :request/type-invalid))))))

(deftest space-create-test
  (with-space-runtime
    (fn [runtime]
      (testing "space/create materializes a user-facing Space"
        (let [request (space/space-create-request
                        "chat-A"
                        {:request-id "req-create-chat-A"
                         :time-ms 1
                         :title "Chat A"
                         :actor {:actor/id "sid"
                                 :actor/type :human}})
              decision (append-and-await-decision! runtime request)
              thread (space/await-space runtime "chat-A" some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= [:space "chat-A"] (:routing/key decision)))
          (is (= ["req-create-chat-A/event/space"] (:event/ids decision)))
          (is (= "Chat A" (:title thread)))
          (is (= :active (:status thread)))
          (is (= 0 (:turn-count thread)))
          (is (= [] (space/read-turns-by-space runtime "chat-A")))
          (is (= :space/created
                 (:event/type (space/read-event runtime (:event/id decision))))))))))

(deftest compose-and-send-freezes-context-bundle-test
  (with-space-runtime
    (fn [runtime]
      (testing "compose-and-send creates thread, turn, order row, and immutable bundle"
        (let [request (space/compose-and-send-request
                        "chat-B"
                        "Explain the contract."
                        {:request-id "req-send-chat-B"
                         :time-ms 10
                         :title "Contract chat"
                         :payload {:turn/id "WT-1"
                                   :context-bundle/id "B-1"
                                   :refs [{:object/id "note-17"}
                                          {:slice/id "slice-3"}]
                                   :llm/options {:agent/kind :codex
                                                 :model "gpt-5.2-codex"
                                                 :approval-policy :on-request
                                                 :sandbox :workspace-write
                                                 :cwd "/mnt/data/projects/Softland"
                                                 :executor/pool :not-bundle-owned}}})
              decision (append-and-await-decision! runtime request)
              thread (space/await-space runtime "chat-B" #(= 1 (:turn-count %)))
              turn (space/await-turn runtime "WT-1" some?)
              bundle (space/await-materialized
                       #(space/read-context-bundle runtime "B-1")
                       some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= ["req-send-chat-B/event/space"
                  "req-send-chat-B/event/turn"
                  "req-send-chat-B/event/context-bundle"
                  "req-send-chat-B/event/llm-turn-run"]
                 (:event/ids decision)))
          (is (= "Contract chat" (:title thread)))
          (is (= ["WT-1"] (space/read-turns-by-space runtime "chat-B")))
          (is (= :compose-and-send (:turn/kind turn)))
          (is (= "B-1" (:context-bundle/id turn)))
          (is (= "B-1" (space/read-context-bundle-by-turn runtime "WT-1")))
          (is (= {:agent/kind :codex
                  :model "gpt-5.2-codex"
                  :approval-policy :on-request
                  :sandbox :workspace-write
                  :cwd "/mnt/data/projects/Softland"}
                 (:execution/options bundle)))
          (is (not (contains? (:execution/options bundle) :executor/pool)))
          (is (str/starts-with? (:context-bundle/hash bundle) "sha256:"))
          (is (str/includes? (:rendered/model-input bundle)
                             "Explain the contract."))
          (is (str/includes? (:rendered/model-input bundle) "object:note-17"))
          (is (str/includes? (:rendered/model-input bundle) "slice:slice-3")))))))

(deftest space-catalog-materializes-eager-objects-test
  (with-space-runtime
    (fn [runtime]
      (testing "accepted space facts become catalog objects and relation edges"
        (let [request (space/compose-and-send-request
                        "chat-catalog"
                        "Catalog this send."
                        {:request-id "req-catalog"
                         :time-ms 15
                         :title "Catalog chat"
                         :payload {:turn/id "WT-catalog"
                                   :context-bundle/id "B-catalog"
                                   :llm-turn-run/id "run-catalog"
                                   :llm-thread/id "llm-thread-catalog"}})
              _decision (append-and-await-decision! runtime request)
              thread-object-id (space/catalog-object-id :space "chat-catalog")
              turn-object-id (space/catalog-object-id :turn "WT-catalog")
              bundle-object-id (space/catalog-object-id :context-bundle "B-catalog")
              run-object-id (space/catalog-object-id :llm-turn-run "run-catalog")
              thread-object (space/await-materialized
                              #(space/read-object runtime thread-object-id)
                              some?)
              turn-object (space/await-materialized
                            #(space/read-object runtime turn-object-id)
                            some?)
              bundle-object (space/await-materialized
                              #(space/read-object runtime bundle-object-id)
                              some?)
              run-object (space/await-materialized
                           #(space/read-object runtime run-object-id)
                           some?)
              thread-out (space/await-materialized
                           #(space/read-artifact-graph runtime thread-object-id)
                           #(= #{turn-object-id}
                               (set (map :to/object-id (vals %)))))
              turn-out (space/await-materialized
                         #(space/read-artifact-graph runtime turn-object-id)
                         #(= #{bundle-object-id run-object-id}
                             (set (map :to/object-id (vals %)))))
              bundle-out (space/await-materialized
                           #(space/read-artifact-graph runtime bundle-object-id)
                           #(= #{run-object-id}
                               (set (map :to/object-id (vals %)))))
              run-in (space/await-materialized
                       #(space/read-artifact-graph-in runtime run-object-id)
                       #(= #{turn-object-id bundle-object-id}
                           (set (map :from/object-id (vals %)))))]
          (is (= :space (:object/type thread-object)))
          (is (= :turn (:object/type turn-object)))
          (is (= :context-bundle (:object/type bundle-object)))
          (is (= :llm-turn-run (:object/type run-object)))
          (is (= "Catalog chat" (:title thread-object)))
          (is (= "B-catalog" (:context-bundle/id turn-object)))
          (is (= "WT-catalog" (:turn/id bundle-object)))
          (is (= "llm-thread-catalog" (:llm-thread/id run-object)))
          (is (= #{turn-object-id}
                 (set (map :to/object-id (vals thread-out)))))
          (is (= #{bundle-object-id run-object-id}
                 (set (map :to/object-id (vals turn-out)))))
          (is (= #{run-object-id}
                 (set (map :to/object-id (vals bundle-out)))))
          (is (= #{turn-object-id bundle-object-id}
                 (set (map :from/object-id (vals run-in))))))))))

(deftest projection-rebuildability-test
  (testing "projection PStates are rebuildable from the same canonical Space and LLM inputs"
    (let [snapshot-a (materialized-projection-snapshot)
          snapshot-b (materialized-projection-snapshot)
          chat-canvas (:chat-canvas snapshot-a)
          run-detail (:run-detail snapshot-a)
          thread-detail (get-in snapshot-a [:object-detail :thread])
          turn-relations (get-in snapshot-a [:relations :turn])]
      (is (= snapshot-a snapshot-b))
      (is (= :chat-canvas (:projection/type chat-canvas)))
      (is (= :space-canonical-pstates (:projection/source chat-canvas)))
      (is (= "Projection chat" (:title chat-canvas)))
      (is (= "WT-projection" (get-in chat-canvas [:latest-turn :turn/id])))
      (is (= :llm-run-detail (:projection/type run-detail)))
      (is (= :object-detail (:projection/type thread-detail)))
      (is (= :object-relations (:projection/type turn-relations)))
      (is (= #{(space/catalog-object-id :space "chat-projection")}
             (relation-sources turn-relations)))
      (is (= #{(space/catalog-object-id :context-bundle "B-projection")
               (space/catalog-object-id :llm-turn-run "run-projection")}
             (relation-targets turn-relations))))))

(deftest space-writer-asymmetry-property-test
  (testing "space and LLM record builders do not mutate PStates before depot append"
    (with-space-runtime
      (fn [runtime]
        (let [before (space-property-empty-snapshot runtime)
              {:keys [space-id turn-id bundle-id run-id thread-id
                      item-id slice-id]} space-replay-ids]
          (space/compose-and-send-request
            space-id
            "Replay this space input."
            {:request-id "req-space-property-send"
             :time-ms 300
             :payload {:turn/id turn-id
                       :context-bundle/id bundle-id
                       :llm-turn-run/id run-id
                       :llm-thread/id thread-id}})
          (llm/observation
            run-id
            thread-id
            :codex/item-completed
            0
            {:observation-id "obs-space-property-item"
             :received-at-ms 310
             :llm-item/id item-id})
          (space/space-turn-request
            :turn/slice-create
            space-id
            {:request-id "req-space-property-slice"
             :time-ms 330
             :payload {:turn/id "WT-space-property-slice"
                       :slice/id slice-id}})
          (is (= before (space-property-empty-snapshot runtime)))))))

  (testing "the same depot inputs rebuild the same Space, LLM, control, catalog, and projection surfaces"
    (let [snapshot-a (materialized-space-replay-snapshot)
          snapshot-b (materialized-space-replay-snapshot)
          {:keys [space-id item-id slice-id]} space-replay-ids
          raw-object-id (space/catalog-object-id :llm-item item-id)
          slice-object-id (space/catalog-object-id :slice slice-id)
          raw-relations (get-in snapshot-a [:projections :raw-relations])
          slice-relations (get-in snapshot-a [:projections :slice-relations])]
      (is (= snapshot-a snapshot-b))
      (is (= ["WT-space-property-send"
              "WT-space-property-compact"
              "WT-space-property-slice"]
             (get-in snapshot-a [:space :turn-order])))
      (is (= :compact/request
             (get-in snapshot-a [:llm :control :control/type])))
      (is (= 21
             (get-in snapshot-a [:llm :cost :tokens :tokens/input-total])))
      (is (= :llm-thread-cost-rollup
             (get-in snapshot-a [:llm :cost :projection/type])))
      (is (= :llm-run-detail
             (get-in snapshot-a [:llm :run-detail :projection/type])))
      (is (= "WT-space-property-slice"
             (get-in snapshot-a [:projections :chat-canvas :latest-turn :turn/id])))
      (is (= :llm-item
             (get-in snapshot-a [:catalog :raw :object/type])))
      (is (= :slice
             (get-in snapshot-a [:catalog :slice :object/type])))
      (is (= :object-detail
             (get-in snapshot-a [:projections :raw-detail :projection/type])))
      (is (= #{slice-object-id} (relation-targets raw-relations)))
      (is (= #{raw-object-id} (relation-sources slice-relations)))
      (is (= space-id
             (get-in snapshot-a [:space :thread :space/id]))))))

(deftest slice-raw-immutability-test
  (with-space-runtime
    (fn [runtime]
      (testing "slice snapshots do not mutate raw LLM items or get rewritten by later observations"
        (let [ids {:space-id "chat-slice-immutability"
                   :turn-id "WT-slice-source-send"
                   :bundle-id "B-slice-source"
                   :run-id "run-slice-source"
                   :thread-id "llm-thread-slice-source"
                   :request-id "req-slice-source-send"}
              item-id "item-slice-source"
              original-text "Original raw answer."
              mutated-text "Mutated duplicate answer."
              _ (append-send-and-await-run! runtime ids)
              _ (claim-test-run! runtime (:run-id ids) (:thread-id ids))
              raw-item (append-raw-item-observation!
                         runtime
                         {:run-id (:run-id ids)
                          :thread-id (:thread-id ids)
                          :item-id item-id
                          :text original-text
                          :sequence 0})
              raw-object-id (space/catalog-object-id :llm-item item-id)
              slice-request (space/space-turn-request
                              :turn/slice-create
                              (:space-id ids)
                              {:request-id "req-slice-create"
                               :time-ms 210
                               :payload {:turn/id "WT-slice-create"
                                         :slice/id "slice-immutability"
                                         :prompt/text "Make this excerpt public."
                                         :source {:llm-item/id item-id
                                                  :llm-turn-run/id (:run-id ids)
                                                  :llm-thread/id (:thread-id ids)
                                                  :content/text original-text
                                                  :content/hash (:content/hash raw-item)}}})]
          (is (nil? (space/read-object runtime raw-object-id)))
          (append-and-await-decision! runtime slice-request)
          (let [slice (space/await-materialized
                        #(space/read-slice runtime "slice-immutability")
                        some?)]
            (append-raw-item-observation!
              runtime
              {:run-id (:run-id ids)
               :thread-id (:thread-id ids)
               :item-id item-id
               :text mutated-text
               :sequence 1})
            (is (= original-text (:snapshot/text slice)))
            (is (= (:content/hash raw-item) (:source/content-hash slice)))
            (is (= original-text
                   (:content/text (llm/read-item-by-id runtime item-id))))
            (is (= original-text
                   (get-in (llm/read-items-by-run runtime (:run-id ids))
                           [item-id :content/text])))
            (is (= :llm-item
                   (:object/type
                    (space/await-materialized
                      #(space/read-object runtime raw-object-id)
                      some?))))))))))

(deftest slice-source-hash-test
  (with-space-runtime
    (fn [runtime]
      (testing "a slice records both its snapshot hash and the source content hash"
        (let [create-request (space/space-create-request
                               "chat-slice-hash"
                               {:request-id "req-slice-hash-thread"
                                :time-ms 220})
              source-hash (space/content-hash "larger source material")
              slice-request (space/space-turn-request
                              :turn/slice-create
                              "chat-slice-hash"
                              {:request-id "req-slice-hash"
                               :time-ms 221
                               :payload {:turn/id "WT-slice-hash"
                                         :slice/id "slice-hash"
                                         :snapshot/text "selected source"
                                         :source {:llm-item/id "item-slice-hash"
                                                  :content/hash source-hash}}})]
          (append-and-await-decision! runtime create-request)
          (append-and-await-decision! runtime slice-request)
          (let [slice (space/await-materialized
                        #(space/read-slice runtime "slice-hash")
                        some?)]
            (is (= source-hash (:source/content-hash slice)))
            (is (= (space/content-hash "selected source") (:snapshot/hash slice)))
            (is (not= (:source/content-hash slice) (:snapshot/hash slice)))))))))

(deftest derivative-renders-as-user-authored-test
  (with-space-runtime
    (fn [runtime]
      (testing "derivatives are stored and rendered as user-authored material"
        (let [create-request (space/space-create-request
                               "chat-derivative"
                               {:request-id "req-derivative-thread"
                                :time-ms 230})
              derivative-request (space/space-turn-request
                                   :turn/derivative-create
                                   "chat-derivative"
                                   {:request-id "req-derivative"
                                    :time-ms 231
                                    :actor {:actor/id "sid"
                                            :actor/type :human}
                                    :payload {:turn/id "WT-derivative"
                                              :derivative/id "derivative-user"
                                              :content/text "My rewritten understanding."
                                              :source {:llm-item/id "item-derivative-source"
                                                       :content/text "model draft"}}})
              send-request (space/compose-and-send-request
                             "chat-derivative"
                             "Use this derivative."
                             {:request-id "req-derivative-send"
                              :time-ms 232
                              :payload {:turn/id "WT-derivative-send"
                                        :context-bundle/id "B-derivative-send"
                                        :llm-turn-run/id "run-derivative-send"
                                        :refs [{:derivative/id "derivative-user"}]}})]
          (append-and-await-decision! runtime create-request)
          (append-and-await-decision! runtime derivative-request)
          (let [derivative (space/await-materialized
                             #(space/read-derivative runtime "derivative-user")
                             some?)
                derivative-object (space/await-materialized
                                    #(space/read-object
                                       runtime
                                       (space/catalog-object-id :derivative "derivative-user"))
                                    some?)]
            (is (= :user (:authorship derivative)))
            (is (= :user-authored (:render/as derivative)))
            (is (= :user (:authorship derivative-object))))
          (append-and-await-decision! runtime send-request)
          (let [bundle (space/await-materialized
                         #(space/read-context-bundle runtime "B-derivative-send")
                         some?)]
            (is (str/includes? (:rendered/model-input bundle)
                               "user-authored-derivative:derivative-user"))))))))

(deftest space-only-turn-no-llm-side-effect-test
  (with-space-runtime
    (fn [runtime]
      (testing "slice, comment, and derivative turns stay space-only"
        (let [create-request (space/space-create-request
                               "chat-space-only-material"
                               {:request-id "req-space-only-material-thread"
                                :time-ms 240})
              slice-request (space/space-turn-request
                              :turn/slice-create
                              "chat-space-only-material"
                              {:request-id "req-space-only-slice"
                               :time-ms 241
                               :payload {:turn/id "WT-space-only-slice"
                                         :slice/id "slice-space-only"
                                         :snapshot/text "space only slice"
                                         :source {:llm-item/id "item-space-only"
                                                  :content/text "raw"}}})
              comment-request (space/space-turn-request
                                :turn/comment-create
                                "chat-space-only-material"
                                {:request-id "req-space-only-comment"
                                 :time-ms 242
                                 :payload {:turn/id "WT-space-only-comment"
                                           :overlay/id "overlay-space-only"
                                           :prompt/text "A note on the raw item."
                                           :source {:llm-item/id "item-space-only"
                                                    :content/text "raw"}}})
              derivative-request (space/space-turn-request
                                   :turn/derivative-create
                                   "chat-space-only-material"
                                   {:request-id "req-space-only-derivative"
                                    :time-ms 243
                                    :payload {:turn/id "WT-space-only-derivative"
                                              :derivative/id "derivative-space-only"
                                              :content/text "user rewrite"
                                              :source {:llm-item/id "item-space-only"
                                                       :content/text "raw"}}})]
          (append-and-await-decision! runtime create-request)
          (append-and-await-decision! runtime slice-request)
          (append-and-await-decision! runtime comment-request)
          (append-and-await-decision! runtime derivative-request)
          (is (some? (space/await-materialized
                       #(space/read-slice runtime "slice-space-only")
                       some?)))
          (is (some? (space/await-materialized
                       #(space/read-overlay runtime "overlay-space-only")
                       some?)))
          (is (some? (space/await-materialized
                       #(space/read-derivative runtime "derivative-space-only")
                       some?)))
          (is (nil? (space/read-context-bundle-by-turn runtime "WT-space-only-slice")))
          (is (nil? (space/read-llm-run-by-turn runtime "WT-space-only-slice")))
          (is (nil? (space/read-context-bundle-by-turn runtime "WT-space-only-comment")))
          (is (nil? (space/read-llm-run-by-turn runtime "WT-space-only-comment")))
          (is (nil? (space/read-context-bundle-by-turn runtime "WT-space-only-derivative")))
          (is (nil? (space/read-llm-run-by-turn runtime "WT-space-only-derivative")))
          (is (empty? (llm/read-pending runtime llm/pending-task-id))))))))

(deftest fork-span-softland-anchor-test
  (with-space-runtime
    (fn [runtime]
      (testing "fork-from-span anchors a child space on a reusable slice and Codex thread fork"
        (let [parent-thread-id "chat-fork-parent"
              child-thread-id "chat-fork-child"
              parent-native-thread-id "codex-native-parent"
              child-native-thread-id "codex-native-child"
              source-text "The span that becomes its own local space."
              source-hash (space/content-hash source-text)
              create-parent (space/space-create-request
                              parent-thread-id
                              {:request-id "req-fork-parent"
                               :time-ms 250
                               :title "Fork parent"})
              fork-request (space/space-action-request
                             :space/fork-from-span
                             child-thread-id
                             {:request-id "req-fork-child"
                              :time-ms 251
                              :title "Fork child"
                              :payload {:parent-space/id parent-thread-id
                                        :turn/id "WT-fork-child"
                                        :context-bundle/id "B-fork-child"
                                        :slice/id "slice-fork-anchor"
                                        :llm-turn-run/id "run-fork-child"
                                        :llm-thread/id "llm-thread-fork-child"
                                        :prompt/text "Explore the forked span."
                                        :refs [{:slice/id "slice-fork-anchor"}]
                                        :source {:llm-item/id "item-fork-source"
                                                 :content/text source-text
                                                 :content/hash source-hash}
                                        :fork/from-native-thread-id parent-native-thread-id
                                        :native/codex-thread-id child-native-thread-id
                                        :executor/task-id llm/pending-task-id}})]
          (append-and-await-decision! runtime create-parent)
          (let [decision (append-and-await-decision! runtime fork-request)
                child-thread (space/await-space runtime child-thread-id #(= 1 (:turn-count %)))
                slice (space/await-materialized
                        #(space/read-slice runtime "slice-fork-anchor")
                        some?)
                bundle (space/await-materialized
                         #(space/read-context-bundle runtime "B-fork-child")
                         some?)
                llm-request (space/await-materialized
                              #(space/read-llm-run-request runtime "run-fork-child")
                              some?)
                llm-run (llm/await-run runtime "run-fork-child" #(= :pending (:status %)))]
            (is (= :accepted (:decision/status decision)))
            (is (= parent-thread-id (:parent-space/id child-thread)))
            (is (contains? (space/read-space-graph runtime parent-thread-id)
                           child-thread-id))
            (is (= source-text (:snapshot/text slice)))
            (is (= source-hash (:source/content-hash slice)))
            (is (str/includes? (:rendered/model-input bundle)
                               "slice:slice-fork-anchor"))
            (is (= parent-native-thread-id
                   (get-in llm-request [:executor :fork/from-native-thread-id])))
            (is (= child-native-thread-id
                   (get-in llm-request [:executor :native/thread-id])))
            (is (= parent-native-thread-id (:fork/from-native-thread-id llm-run)))
            (is (= child-native-thread-id (:native/codex-thread-id llm-run)))))))))

(deftest fork-binding-before-turn-start-test
  (with-space-runtime
    (fn [runtime]
      (testing "the executor does not start a forked child turn until native binding is durable"
        (let [parent-thread-id "chat-fork-wait-parent"
              child-thread-id "chat-fork-wait-child"
              adapter-called? (atom false)
              create-parent (space/space-create-request
                              parent-thread-id
                              {:request-id "req-fork-wait-parent"
                               :time-ms 260})
              fork-request (space/space-action-request
                             :space/fork-from-span
                             child-thread-id
                             {:request-id "req-fork-wait-child"
                              :time-ms 261
                              :payload {:parent-space/id parent-thread-id
                                        :turn/id "WT-fork-wait-child"
                                        :context-bundle/id "B-fork-wait-child"
                                        :slice/id "slice-fork-wait"
                                        :llm-turn-run/id "run-fork-wait-child"
                                        :llm-thread/id "llm-thread-fork-wait-child"
                                        :prompt/text "Wait for fork binding."
                                        :source {:llm-item/id "item-fork-wait"
                                                 :content/text "fork wait source"}
                                        :fork/from-native-thread-id "codex-native-parent-wait"
                                        :executor/task-id llm/pending-task-id}})]
          (append-and-await-decision! runtime create-parent)
          (append-and-await-decision! runtime fork-request)
          (llm/await-run runtime "run-fork-wait-child" #(= :pending (:status %)))
          (let [result (llm/run-one-pending-with-adapter!
                         runtime
                         {:executor-id "executor-fork-wait"
                          :adapter (fn [_ctx]
                                     (reset! adapter-called? true)
                                     [])})
                run (llm/read-run runtime "run-fork-wait-child")]
            (is (false? (:spawned? result)))
            (is (= :fork-binding-not-durable (:reason result)))
            (is (false? @adapter-called?))
            (is (= :pending (:status run)))
            (is (nil? (:claimed-by run)))))))))

(deftest patch-proposal-creation-test
  (with-space-runtime
    (fn [runtime]
      (testing "patch-like LLM observations become pending space proposals"
        (let [ids {:space-id "chat-patch-proposal"
                   :turn-id "WT-patch-source"
                   :bundle-id "B-patch-source"
                   :run-id "run-patch-proposal"
                   :thread-id "llm-thread-patch-proposal"
                   :request-id "req-patch-source"}
              proposal-id "patch-proposal-1"
              _ (append-send-and-await-run! runtime ids)]
          (space/append-llm-observation!
            runtime
            (llm/observation
              (:run-id ids)
              (:thread-id ids)
              :codex/patch-proposal
              0
              {:observation-id "obs-patch-proposal-1"
               :space/id (:space-id ids)
               :turn/id (:turn-id ids)
               :patch-proposal/id proposal-id
               :turn-diff/id "turn-diff-1"
               :summary/text "Update the parser."
               :patch/files [{:path "src/app/parser.clj"
                              :hunks 2}]}))
          (let [proposal (space/await-materialized
                           #(space/read-patch-proposal runtime proposal-id)
                           some?)]
            (is (= :pending (:status proposal)))
            (is (= (:run-id ids) (:llm-turn-run/id proposal)))
            (is (= (:space-id ids) (:space/id proposal)))
            (is (= "Update the parser." (:summary/text proposal)))
            (is (= [{:path "src/app/parser.clj" :hunks 2}]
                   (:patch/files proposal)))))))))

(deftest patch-accept-reject-turns-test
  (with-space-runtime
    (fn [runtime]
      (testing "patch acceptance and rejection are space turns, not tool approvals"
        (let [ids {:space-id "chat-patch-resolution"
                   :turn-id "WT-patch-resolution-source"
                   :bundle-id "B-patch-resolution-source"
                   :run-id "run-patch-resolution"
                   :thread-id "llm-thread-patch-resolution"
                   :request-id "req-patch-resolution-source"}
              accept-id "patch-proposal-accept"
              reject-id "patch-proposal-reject"
              _ (append-send-and-await-run! runtime ids)]
          (doseq [[proposal-id sequence] [[accept-id 0] [reject-id 1]]]
            (space/append-llm-observation!
              runtime
              (llm/observation
                (:run-id ids)
                (:thread-id ids)
                :codex/patch-proposal
                sequence
                {:observation-id (str "obs-" proposal-id)
                 :space/id (:space-id ids)
                 :turn/id (:turn-id ids)
                 :patch-proposal/id proposal-id
                 :summary/text proposal-id})))
          (space/await-materialized
            #(space/read-patch-proposal runtime accept-id)
            #(= :pending (:status %)))
          (space/await-materialized
            #(space/read-patch-proposal runtime reject-id)
            #(= :pending (:status %)))
          (let [accept-request (space/space-turn-request
                                 :turn/patch-accept
                                 (:space-id ids)
                                 {:request-id "req-patch-accept"
                                  :time-ms 270
                                  :payload {:turn/id "WT-patch-accept"
                                            :patch-proposal/id accept-id
                                            :prompt/text "Accept this patch."}})
                reject-request (space/space-turn-request
                                 :turn/patch-reject
                                 (:space-id ids)
                                 {:request-id "req-patch-reject"
                                  :time-ms 271
                                  :payload {:turn/id "WT-patch-reject"
                                            :patch-proposal/id reject-id
                                            :prompt/text "Reject this patch."
                                            :reason :not-right-shape}})
                accept-decision (append-and-await-decision! runtime accept-request)
                reject-decision (append-and-await-decision! runtime reject-request)
                accepted (space/await-materialized
                           #(space/read-patch-proposal runtime accept-id)
                           #(= :accepted (:status %)))
                rejected (space/await-materialized
                           #(space/read-patch-proposal runtime reject-id)
                           #(= :rejected (:status %)))]
            (is (= :accepted (:decision/status accept-decision)))
            (is (= :accepted (:decision/status reject-decision)))
            (is (= "WT-patch-accept" (:resolution/turn-id accepted)))
            (is (= "WT-patch-reject" (:resolution/turn-id rejected)))
            (is (= :not-right-shape (:reason rejected)))
            (is (nil? (:llm-control/type accept-decision)))
            (is (nil? (:llm-control/type reject-decision)))
            (is (nil? (space/read-llm-control-by-turn runtime "WT-patch-accept")))
            (is (nil? (space/read-llm-control-by-turn runtime "WT-patch-reject")))))))))

(deftest space-only-turn-no-context-bundle-test
  (with-space-runtime
    (fn [runtime]
      (testing "space-only turns are ordered material but do not freeze a model input"
        (let [create-request (space/space-create-request
                               "chat-C"
                               {:request-id "req-create-chat-C"
                                :time-ms 20})
              comment-request (space/space-turn-request
                                :turn/comment-create
                                "chat-C"
                                {:request-id "req-comment-chat-C"
                                 :time-ms 21
                                 :payload {:turn/id "WT-comment"
                                           :prompt/text "Actually, pin this nuance."
                                           :refs [{:turn/id "WT-older"}]}})]
          (append-and-await-decision! runtime create-request)
          (let [decision (append-and-await-decision! runtime comment-request)
                turn (space/await-turn runtime "WT-comment" some?)
                thread (space/await-space runtime "chat-C" #(= 1 (:turn-count %)))]
            (is (= :accepted (:decision/status decision)))
            (is (= ["req-comment-chat-C/event/turn"] (:event/ids decision)))
            (is (= :turn/comment-create (:turn/kind turn)))
            (is (= "Actually, pin this nuance." (:prompt/text turn)))
            (is (= ["WT-comment"] (space/read-turns-by-space runtime "chat-C")))
            (is (= 1 (:turn-count thread)))
            (is (nil? (space/read-context-bundle-by-turn runtime "WT-comment")))))))))

(deftest space-only-turn-requires-thread-test
  (with-space-runtime
    (fn [runtime]
      (testing "space-only turns cannot invent their containing Space"
        (let [request (space/space-turn-request
                        :turn/comment-create
                        "missing-chat"
                        {:request-id "req-comment-missing"
                         :time-ms 30
                         :payload {:turn/id "WT-missing"
                                   :prompt/text "No container."}})
              decision (append-and-await-decision! runtime request)]
          (is (= :rejected (:decision/status decision)))
          (is (= :space/not-found (:decision/reason decision)))
          (is (nil? (space/read-turn runtime "WT-missing"))))))))

(deftest space-first-send-test
  (with-space-runtime
    (fn [runtime]
      (testing "the user send enters Space first and Space derives the LLM run request"
        (let [request (space/compose-and-send-request
                        "chat-space-first"
                        "Start from the space."
                        {:request-id "req-space-first"
                         :time-ms 40
                         :idempotency-key "idem-space-first"
                         :payload {:turn/id "WT-space-first"
                                   :context-bundle/id "B-space-first"
                                   :llm-turn-run/id "run-space-first"
                                   :llm-thread/id "llm-thread-space-first"
                                   :executor/task-id llm/pending-task-id}})
              decision (append-and-await-decision! runtime request)
              llm-request (space/await-materialized
                            #(space/read-llm-run-request runtime "run-space-first")
                            some?)
              llm-decision (llm/await-decision runtime "run-space-first")
              llm-run (llm/await-run runtime "run-space-first"
                                     #(= :pending (:status %)))]
          (is (= :accepted (:decision/status decision)))
          (is (= "WT-space-first" (:turn/id decision)))
          (is (= "B-space-first" (:context-bundle/id decision)))
          (is (= "run-space-first" (:llm-turn-run/id decision)))
          (is (= :llm/turn-run-request (:request/type llm-request)))
          (is (= "WT-space-first" (:turn/id llm-request)))
          (is (= "B-space-first" (:context-bundle/id llm-request)))
          (is (= "req-space-first/llm-request" (:request/id llm-request)))
          (is (= :accepted (:decision/status llm-decision)))
          (is (= "B-space-first" (:context-bundle/id llm-run)))
          (is (contains?
                (llm/await-materialized
                  #(llm/read-pending runtime llm/pending-task-id)
                  #(contains? % "run-space-first"))
                "run-space-first")))))))

(deftest context-bundle-before-run-test
  (with-space-runtime
    (fn [runtime]
      (testing "Space materializes the bundle that the bridged LLM run points at"
        (let [request (space/compose-and-send-request
                        "chat-bundle-before-run"
                        "Freeze this before execution."
                        {:request-id "req-bundle-before-run"
                         :time-ms 50
                         :payload {:turn/id "WT-bundle-before-run"
                                   :context-bundle/id "B-bundle-before-run"
                                   :llm-turn-run/id "run-bundle-before-run"}})
              decision (append-and-await-decision! runtime request)
              bundle (space/await-materialized
                       #(space/read-context-bundle runtime "B-bundle-before-run")
                       some?)
              llm-run (llm/await-run runtime "run-bundle-before-run"
                                     #(= :pending (:status %)))]
          (is (= "B-bundle-before-run" (:context-bundle/id bundle)))
          (is (= (:context-bundle/hash bundle) (:context-bundle/hash decision)))
          (is (= (:context-bundle/id bundle) (:context-bundle/id llm-run)))
          (is (= "run-bundle-before-run"
                 (space/read-llm-run-by-turn runtime "WT-bundle-before-run"))))))))

(deftest one-bundle-per-run-test
  (with-space-runtime
    (fn [runtime]
      (testing "one compose-and-send creates exactly one bundle and one run"
        (let [request (space/compose-and-send-request
                        "chat-one-bundle"
                        "One run, one bundle."
                        {:request-id "req-one-bundle"
                         :time-ms 60
                         :payload {:turn/id "WT-one-bundle"
                                   :context-bundle/id "B-one-bundle"
                                   :llm-turn-run/id "run-one-bundle"}})
              decision (append-and-await-decision! runtime request)
              bundle-id (space/await-materialized
                          #(space/read-context-bundle-by-turn runtime "WT-one-bundle")
                          some?)
              run-id (space/await-materialized
                       #(space/read-llm-run-by-turn runtime "WT-one-bundle")
                       some?)
              llm-run (llm/await-run runtime "run-one-bundle"
                                     #(= :pending (:status %)))]
          (is (= "B-one-bundle" bundle-id))
          (is (= "run-one-bundle" run-id))
          (is (= "B-one-bundle" (:context-bundle/id llm-run)))
          (is (= "run-one-bundle" (:llm-turn-run/id decision)))
          (is (= ["WT-one-bundle"] (space/read-turns-by-space runtime "chat-one-bundle"))))))))

(deftest accepted-decision-fields-test
  (with-space-runtime
    (fn [runtime]
      (testing "accepted compose decisions expose the sibling facts needed by projections"
        (let [request (space/compose-and-send-request
                        "chat-decision-fields"
                        "Expose the accepted fields."
                        {:request-id "req-decision-fields"
                         :time-ms 70
                         :payload {:turn/id "WT-decision-fields"
                                   :context-bundle/id "B-decision-fields"
                                   :llm-turn-run/id "run-decision-fields"
                                   :llm-thread/id "llm-thread-decision-fields"}})
              decision (append-and-await-decision! runtime request)]
          (is (= :accepted (:decision/status decision)))
          (is (= "req-decision-fields/event/turn" (:event/id decision)))
          (is (= ["req-decision-fields/event/space"
                  "req-decision-fields/event/turn"
                  "req-decision-fields/event/context-bundle"
                  "req-decision-fields/event/llm-turn-run"]
                 (:event/ids decision)))
          (is (= "chat-decision-fields" (:space/id decision)))
          (is (= "WT-decision-fields" (:turn/id decision)))
          (is (= "B-decision-fields" (:context-bundle/id decision)))
          (is (str/starts-with? (:context-bundle/hash decision) "sha256:"))
          (is (= "llm-thread-decision-fields" (:llm-thread/id decision)))
          (is (= "run-decision-fields" (:llm-turn-run/id decision)))
          (is (= "req-decision-fields/llm-request" (:llm/request-id decision))))))))

(deftest idempotency-test
  (with-space-runtime
    (fn [runtime]
      (testing "replaying the same send key does not mint duplicate turns, bundles, or runs"
        (let [request-a (space/compose-and-send-request
                          "chat-idempotent"
                          "Send once."
                          {:request-id "req-idem-A"
                           :time-ms 80
                           :idempotency-key "idem-same-send"
                           :payload {:turn/id "WT-idem-A"
                                     :context-bundle/id "B-idem-A"
                                     :llm-turn-run/id "run-idem-A"
                                     :llm-thread/id "llm-thread-idem"}})
              ;; same material (space + prompt + refs + options) under the same
              ;; key = idempotent replay, even with fresh entity ids; different
              ;; material would be an :idempotency/conflict rejection
              request-b (space/compose-and-send-request
                          "chat-idempotent"
                          "Send once."
                          {:request-id "req-idem-B"
                           :time-ms 81
                           :idempotency-key "idem-same-send"
                           :payload {:turn/id "WT-idem-B"
                                     :context-bundle/id "B-idem-B"
                                     :llm-turn-run/id "run-idem-B"
                                     :llm-thread/id "llm-thread-idem"}})
              decision-a (append-and-await-decision! runtime request-a)
              _ (llm/await-run runtime "run-idem-A" #(= :pending (:status %)))
              decision-b (append-and-await-decision! runtime request-b)]
          (is (= :accepted (:decision/status decision-a)))
          (is (= :accepted (:decision/status decision-b)))
          (is (:idempotency/replayed? decision-b))
          (is (= (:event/ids decision-a) (:event/ids decision-b)))
          (is (= "WT-idem-A" (:turn/id decision-b)))
          (is (= "B-idem-A" (:context-bundle/id decision-b)))
          (is (= "run-idem-A" (:llm-turn-run/id decision-b)))
          (is (= ["WT-idem-A"] (space/read-turns-by-space runtime "chat-idempotent")))
          (is (= "B-idem-A" (space/read-context-bundle-by-turn runtime "WT-idem-A")))
          (is (nil? (space/read-context-bundle-by-turn runtime "WT-idem-B")))
          (is (some? (space/read-context-bundle runtime "B-idem-A")))
          (is (nil? (space/read-context-bundle runtime "B-idem-B")))
          (is (some? (space/read-llm-run-request runtime "run-idem-A")))
          (is (nil? (space/read-llm-run-request runtime "run-idem-B")))
          (is (some? (llm/read-run runtime "run-idem-A")))
          (is (nil? (llm/read-run runtime "run-idem-B")))
          (is (= "run-idem-A"
                 (:llm-turn-run/id
                  (space/read-send-by-idempotency runtime "chat-idempotent" "idem-same-send")))))))))

(deftest approval-space-first-test
  (with-space-runtime
    (fn [runtime]
      (testing "approval resolution enters Space and derives an LLM control record"
        (let [ids {:space-id "chat-approval"
                   :turn-id "WT-approval-send"
                   :bundle-id "B-approval"
                   :run-id "run-approval-space-first"
                   :thread-id "llm-thread-approval"
                   :request-id "req-approval-send"}
              approval-id "approval-space-first"
              native-id 44
              _ (append-send-and-await-run! runtime ids)
              _ (claim-test-run! runtime (:run-id ids) (:thread-id ids))
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id native-id})
              control-request (space/space-turn-request
                                :turn/tool-approval-resolve
                                (:space-id ids)
                                {:request-id "req-approval-resolve"
                                 :time-ms 110
                                 :payload {:turn/id "WT-approval-resolve"
                                           :llm-turn-run/id (:run-id ids)
                                           :approval/id approval-id
                                           :native/json-rpc-request-id native-id
                                           :decision :approved}})
              decision (append-and-await-decision! runtime control-request)
              run (llm/await-run runtime (:run-id ids) #(= :running (:status %)))
              approval (get (llm/read-approvals-by-run runtime (:run-id ids)) approval-id)
              control (llm/await-materialized
                        #(llm/read-control runtime "req-approval-resolve/llm-control")
                        some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= :approval/resolve (:llm-control/type decision)))
          (is (= "WT-approval-resolve" (:turn/id decision)))
          (is (= "req-approval-resolve/llm-control"
                 (space/read-llm-control-by-turn runtime "WT-approval-resolve")))
          (is (= :approval/resolve (:control/type control)))
          (is (= native-id (:native/json-rpc-request-id control)))
          (is (= :running (:status run)))
          (is (= :approved (:status approval)))
          (is (nil? (llm/await-materialized
                      #(llm/read-pending-approval runtime approval-id)
                      nil?))))))))

(deftest approval-is-not-patch-acceptance-test
  (with-space-runtime
    (fn [runtime]
      (testing "tool approval resolution is separate from patch acceptance"
        (let [ids {:space-id "chat-approval-not-patch"
                   :turn-id "WT-approval-not-patch-send"
                   :bundle-id "B-approval-not-patch"
                   :run-id "run-approval-not-patch"
                   :thread-id "llm-thread-approval-not-patch"
                   :request-id "req-approval-not-patch-send"}
              approval-id "approval-not-patch"
              _ (append-send-and-await-run! runtime ids)
              _ (claim-test-run! runtime (:run-id ids) (:thread-id ids))
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id 55})
              approval-request (space/space-turn-request
                                 :turn/tool-approval-resolve
                                 (:space-id ids)
                                 {:request-id "req-approval-not-patch"
                                  :time-ms 120
                                  :payload {:turn/id "WT-approval-not-patch"
                                            :llm-turn-run/id (:run-id ids)
                                            :approval/id approval-id
                                            :native/json-rpc-request-id 55
                                            :decision :approved}})
              patch-request (space/space-turn-request
                              :turn/patch-accept
                              (:space-id ids)
                              {:request-id "req-patch-accept-not-approval"
                               :time-ms 121
                               :payload {:turn/id "WT-patch-accept"
                                         :prompt/text "accept patch"}})
              approval-decision (append-and-await-decision! runtime approval-request)
              patch-decision (append-and-await-decision! runtime patch-request)]
          (is (= :approval/resolve (:llm-control/type approval-decision)))
          (is (= :turn/tool-approval-resolve
                 (:turn/kind (space/read-turn runtime "WT-approval-not-patch"))))
          (is (= :accepted (:decision/status patch-decision)))
          (is (= :turn/patch-accept
                 (:turn/kind (space/read-turn runtime "WT-patch-accept"))))
          (is (nil? (:llm-control/type patch-decision)))
          (is (nil? (space/read-llm-control-by-turn runtime "WT-patch-accept"))))))))

(deftest cancel-space-first-test
  (with-space-runtime
    (fn [runtime]
      (testing "cancel enters Space first and derives an LLM cancel control"
        (let [ids {:space-id "chat-cancel"
                   :turn-id "WT-cancel-send"
                   :bundle-id "B-cancel"
                   :run-id "run-cancel-space-first"
                   :thread-id "llm-thread-cancel"
                   :request-id "req-cancel-send"}
              _ (append-send-and-await-run! runtime ids)
              request (space/space-turn-request
                        :turn/cancel
                        (:space-id ids)
                        {:request-id "req-cancel"
                         :time-ms 130
                         :payload {:turn/id "WT-cancel"
                                   :llm-turn-run/id (:run-id ids)
                                   :reason :user-request}})
              decision (append-and-await-decision! runtime request)
              run (llm/await-run runtime (:run-id ids) #(= :cancelled (:status %)))
              control (llm/await-materialized
                        #(llm/read-control runtime "req-cancel/llm-control")
                        some?)]
          (is (= :accepted (:decision/status decision)))
          (is (= :turn/cancel (:llm-control/type decision)))
          (is (= :turn/cancel (:control/type control)))
          (is (= :cancelled (:status run)))
          (is (nil? (llm/await-materialized
                      #(get (llm/read-pending runtime llm/pending-task-id)
                            (:run-id ids))
                      nil?))))))))

(deftest compaction-space-first-test
  (with-space-runtime
    (fn [runtime]
      (testing "compaction request enters Space first and records an LLM control"
        (let [ids {:space-id "chat-compact"
                   :turn-id "WT-compact-send"
                   :bundle-id "B-compact"
                   :run-id "run-compact-space-first"
                   :thread-id "llm-thread-compact"
                   :request-id "req-compact-send"}
              _ (append-send-and-await-run! runtime ids)
              request (space/space-turn-request
                        :turn/compact-request
                        (:space-id ids)
                        {:request-id "req-compact"
                         :time-ms 140
                         :payload {:turn/id "WT-compact"
                                   :llm-turn-run/id (:run-id ids)
                                   :strategy :summarize-prefix}})
              decision (append-and-await-decision! runtime request)
              control (llm/await-materialized
                        #(llm/read-control runtime "req-compact/llm-control")
                        some?)
              ;; microbatch: control-by-id and the run row commit on different
              ;; tasks; await the run row's own fold before asserting on it
              run (llm/await-run runtime (:run-id ids) #(seq (:compactions %)))]
          (is (= :accepted (:decision/status decision)))
          (is (= :compact/request (:llm-control/type decision)))
          (is (= :compact/request (:control/type control)))
          (is (= :pending (:status run)))
          (is (= [{:control/id "req-compact/llm-control"
                   :time-ms 140
                   :actor {:actor/id "system" :actor/type :system}
                   :payload {:space/id "chat-compact"
                             :turn/id "WT-compact"
                             :llm-turn-run/id "run-compact-space-first"
                             :strategy :summarize-prefix}}]
                 (:compactions run))))))))

(deftest approval-timeout-test
  (with-space-runtime
    (fn [runtime]
      (testing "approval timeout is durably recorded as an expired approval"
        (let [ids {:space-id "chat-timeout"
                   :turn-id "WT-timeout-send"
                   :bundle-id "B-timeout"
                   :run-id "run-approval-timeout"
                   :thread-id "llm-thread-timeout"
                   :request-id "req-timeout-send"}
              approval-id "approval-timeout"
              _ (append-send-and-await-run! runtime ids)
              _ (claim-test-run! runtime (:run-id ids) (:thread-id ids))
              _ (append-approval-observation!
                  runtime
                  {:run-id (:run-id ids)
                   :thread-id (:thread-id ids)
                   :approval-id approval-id
                   :native-id 66})
              request (space/space-turn-request
                        :turn/tool-approval-resolve
                        (:space-id ids)
                        {:request-id "req-approval-timeout"
                         :time-ms 150
                         :payload {:turn/id "WT-approval-timeout"
                                   :llm-turn-run/id (:run-id ids)
                                   :approval/id approval-id
                                   :native/json-rpc-request-id 66
                                   :decision :expired
                                   :reason :timeout}})
              decision (append-and-await-decision! runtime request)
              run (llm/await-run runtime (:run-id ids) #(= :failed (:status %)))
              approval (get (llm/read-approvals-by-run runtime (:run-id ids)) approval-id)]
          (is (= :accepted (:decision/status decision)))
          (is (= :approval/resolve (:llm-control/type decision)))
          (is (= :expired (:status approval)))
          (is (= :expired (:decision approval)))
          (is (nil? (llm/await-materialized
                      #(llm/read-pending-approval runtime approval-id)
                      nil?)))
          (is (= :approval/declined (get-in run [:error :reason])))
          (is (= :expired (get-in run [:error :decision]))))))))
