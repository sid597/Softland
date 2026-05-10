(ns app.server.rama.dogfood-llm-test
  (:require [app.server.rama.dogfood.llm :as llm]
            [clojure.test :refer [deftest is testing]]))

(defn with-llm-runtime
  [f]
  (let [runtime (llm/start-llm-runtime!)]
    (try
      (f runtime)
      (finally
        (llm/close-llm-runtime! runtime)))))

(defn append-run-and-await-pending!
  [runtime run-id]
  (let [request (llm/turn-run-request
                  "chat-A"
                  (str run-id "/world-turn")
                  (str run-id "/bundle")
                  {:llm-turn-run-id run-id
                   :llm-thread-id "llm-thread-A"
                   :request-id (str run-id "/request")
                   :time-ms 1
                   :executor-task-id llm/pending-task-id})]
    (llm/append-turn-run-request! runtime request)
    (llm/await-decision runtime run-id)
    (llm/await-run runtime run-id #(= :pending (:status %)))
    (llm/await-materialized
      #(llm/read-pending runtime llm/pending-task-id)
      #(contains? % run-id))
    request))

(defn llm-spine-property-script
  []
  (let [run-id "run_writer_asymmetry"
        thread-id "llm-thread-writer-asymmetry"
        world-thread-id "world-thread-writer-asymmetry"
        world-turn-id "world-turn-writer-asymmetry"
        bundle-id "bundle-writer-asymmetry"
        executor-task-id llm/pending-task-id
        approval-id "approval-writer-asymmetry"
        request (llm/turn-run-request
                  world-thread-id
                  world-turn-id
                  bundle-id
                  {:llm-turn-run-id run-id
                   :llm-thread-id thread-id
                   :request-id "request-writer-asymmetry"
                   :time-ms 100
                   :executor-task-id executor-task-id})
        claim (llm/claim-record
                run-id
                thread-id
                "executor-writer-asymmetry"
                {:claim-token "claim-token-writer-asymmetry"
                 :claimed-at-ms 110
                 :executor-task-id executor-task-id})]
    {:ids {:run-id run-id
           :thread-id thread-id
           :world-thread-id world-thread-id
           :world-turn-id world-turn-id
           :executor-task-id executor-task-id
           :approval-id approval-id}
     :records [[:turn-run-request request]
               [:claim claim]
               [:observation
                (llm/observation
                  run-id
                  thread-id
                  :codex/item-completed
                  0
                  {:observation-id "obs-writer-asymmetry-item"
                   :received-at-ms 120
                   :llm-item/id "item-writer-asymmetry"
                   :item/type :assistant-message
                   :content/text "writer asymmetry item"
                   :raw/json {:event "item/completed"}})]
               [:observation
                (llm/observation
                  run-id
                  thread-id
                  :codex/token-usage
                  1
                  {:observation-id "obs-writer-asymmetry-usage"
                   :received-at-ms 130
                   :tokens/input-total 10
                   :tokens/cached-input 4
                   :tokens/output 3
                   :tokens/reasoning-output 2
                   :model/context-window 1000
                   :subscription/messages-used 1
                   :billing/mode :chatgpt-subscription})]
               [:observation
                (llm/observation
                  run-id
                  thread-id
                  :codex/tool-call
                  2
                  {:observation-id "obs-writer-asymmetry-tool"
                   :received-at-ms 140
                   :tool-call/id "tool-writer-asymmetry"
                   :tool-call/type :exec
                   :tool-call/name "shell"
                   :tool-call/status :requested
                   :raw/json {:event "tool/call"}})]
               [:observation
                (llm/observation
                  run-id
                  thread-id
                  :codex/approval-request
                  3
                  {:observation-id "obs-writer-asymmetry-approval"
                   :received-at-ms 150
                   :approval/id approval-id
                   :approval/type :exec
                   :native/json-rpc-request-id 77
                   :codex/event-method "item/cmdExec/requestApproval"
                   :codex/event-params {:cmd "echo writer-asymmetry"}})]]}))

(defn llm-spine-pstate-snapshot
  [runtime {:keys [run-id thread-id world-thread-id world-turn-id
                   executor-task-id approval-id]}]
  {:decision (llm/read-decision runtime run-id)
   :run (llm/read-run runtime run-id)
   :view (llm/read-view runtime run-id)
   :thread (llm/read-thread runtime thread-id)
   :thread-binding (llm/read-thread-binding runtime world-thread-id)
   :runs-by-thread (llm/read-runs-by-thread runtime thread-id)
   :run-by-world-turn (llm/read-run-for-world-turn runtime world-turn-id)
   :pending-by-task (llm/read-pending runtime executor-task-id)
   :items-by-run (llm/read-items-by-run runtime run-id)
   :items-by-thread (llm/read-items-by-thread runtime thread-id)
   :raw-response-items (llm/read-raw-response-items runtime run-id)
   :tool-calls-by-run (llm/read-tool-calls-by-run runtime run-id)
   :approvals-by-run (llm/read-approvals-by-run runtime run-id)
   :pending-approval (llm/read-pending-approval runtime approval-id)
   :token-usage (llm/read-token-usage runtime run-id)})

(defn append-llm-spine-record!
  [runtime ids [record-kind record]]
  (case record-kind
    :turn-run-request
    (do
      (llm/append-turn-run-request! runtime record)
      (llm/await-decision runtime (:run-id ids))
      (llm/await-run runtime (:run-id ids) #(= :pending (:status %)))
      (llm/await-materialized
        #(llm/read-pending runtime (:executor-task-id ids))
        #(contains? % (:run-id ids))))

    :claim
    (do
      (llm/append-claim! runtime record)
      (llm/await-run runtime (:run-id ids) #(= :claimed (:status %))))

    :observation
    (do
      (llm/append-observation! runtime record)
      (llm/await-run
        runtime
        (:run-id ids)
        #(<= (long (:sequence record)) (long (:last-seq %))))
      (when (= :codex/approval-request (:observation/type record))
        (llm/await-materialized
          #(llm/read-pending-approval runtime (:approval-id ids))
          some?)))))

(defn materialized-llm-spine-snapshot
  [ids records]
  (let [!snapshot (atom nil)]
    (with-llm-runtime
      (fn [runtime]
        (doseq [record records]
          (append-llm-spine-record! runtime ids record))
        (llm/await-view runtime (:run-id ids)
                        #(= :blocked-awaiting-approval (:status %)))
        (reset! !snapshot (llm-spine-pstate-snapshot runtime ids))))
    @!snapshot))

(deftest llm-turn-run-request-contract-test
  (testing "LLM run requests carry only the bundle reference and scheduling hints"
    (let [request (llm/turn-run-request
                    "chat-A"
                    "WT-1"
                    "B-1"
                    {:llm-turn-run-id "run-1"
                     :llm-thread-id "llm-thread-A"
                     :request-id "llm-req-1"
                     :time-ms 1})]
      (is (= :llm/turn-run-request (:request/type request)))
      (is (= [:llm-run "run-1"] (:routing/key request)))
      (is (= "B-1" (:context-bundle/id request)))
      (is (= "B-1" (get-in request [:payload :context-bundle/id])))
      (is (empty? (llm/request-validation-errors request)))
      (is (not (contains? (:payload request) :model)))
      (is (not (contains? (:payload request) :approval-policy)))
      (is (contains?
            (set (map :type
                      (llm/request-validation-errors
                        (assoc-in request [:payload :model] "gpt-5.2-codex"))))
            :payload/execution-options-not-bundle-owned)))))

(deftest pstate-writer-asymmetry-property-test
  (testing "record builders do not mutate PStates; only depot appends do"
    (with-llm-runtime
      (fn [runtime]
        (let [{:keys [ids]} (llm-spine-property-script)
              before (llm-spine-pstate-snapshot runtime ids)]
          (llm-spine-property-script)
          (is (= before (llm-spine-pstate-snapshot runtime ids)))))))

  (testing "the same depot inputs rebuild the same LLM PState surface"
    (let [{:keys [ids records]} (llm-spine-property-script)
          snapshot-a (materialized-llm-spine-snapshot ids records)
          snapshot-b (materialized-llm-spine-snapshot ids records)]
      (is (= snapshot-a snapshot-b))
      (is (= :blocked-awaiting-approval (get-in snapshot-a [:run :status])))
      (is (= 100 (get-in snapshot-a [:decision :decided-at])))
      (is (contains? (get-in snapshot-a [:approvals-by-run]) (:approval-id ids))))))

(deftest llm-claim-and-observation-lifecycle-test
  (with-llm-runtime
    (fn [runtime]
      (testing "run request, claim, observations, item rows, token usage, and view all materialize through Rama"
        (let [run-id "run_lifecycle"
              request (append-run-and-await-pending! runtime run-id)
              decision (llm/read-decision runtime run-id)
              pending-before (llm/read-pending runtime)
              claim (llm/claim-record
                      run-id
                      "llm-thread-A"
                      "executor-a"
                      {:claim-token "claim-token-a"
                       :claimed-at-ms 2
                       :executor-task-id llm/pending-task-id})]
          (is (= :accepted (:decision/status decision)))
          (is (= (:routing/key request) (:routing/key decision)))
          (is (= [(str (:request/id request) "/event")] (:event/ids decision)))
          (is (= (str run-id "/bundle") (get-in request [:payload :context-bundle/id])))
          (is (= run-id (llm/read-run-for-world-turn runtime (str run-id "/world-turn"))))
          (is (= "llm-thread-A" (llm/read-thread-binding runtime "chat-A")))
          (is (contains? pending-before run-id))

          (llm/append-claim! runtime claim)
          (llm/await-run runtime run-id #(= :claimed (:status %)))
          (is (not (contains? (llm/read-pending runtime) run-id)))
          (is (= :granted-to-us (llm/claim-state (llm/read-run runtime run-id) claim)))

          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/item-completed
              0
              {:observation-id "obs-item-0"
               :llm-item/id "item-0"
               :item/type :assistant-message
               :content/text "hello from codex"
               :raw/json {:event "item/completed"}}))
          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/token-usage
              1
              {:observation-id "obs-usage-1"
               :tokens/input-total 50000
               :tokens/cached-input 45000
               :tokens/output 2000
               :tokens/reasoning-output 8000
               :model/context-window 1050000
               :subscription/messages-used 1
               :billing/mode :chatgpt-subscription}))
          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/run-finished
              2
              {:observation-id "obs-finish-2"}))

          (let [view (llm/await-view runtime run-id #(= :succeeded (:status %)))
                items (llm/read-items-by-run runtime run-id)
                usage (llm/read-token-usage runtime run-id)]
            (is (= :succeeded (:status view)))
            (is (= ["item-0"] (mapv :llm-item/id (:items view))))
            (is (= "hello from codex" (get-in items ["item-0" :content/text])))
            (is (= 50000 (:tokens/input-total usage)))
            (is (= 45000 (:tokens/cached-input usage)))
            (is (= 8000 (:tokens/reasoning-output usage)))))))))

(deftest obs-sequence-buffer-test
  (with-llm-runtime
    (fn [runtime]
      (testing "out-of-order observations are buffered and drained, not dropped"
        (let [run-id "run_buffer"
              _ (append-run-and-await-pending! runtime run-id)
              claim (llm/claim-record
                      run-id
                      "llm-thread-A"
                      "executor-buffer"
                      {:claim-token "claim-token-buffer"
                       :executor-task-id llm/pending-task-id})]
          (llm/append-claim! runtime claim)
          (llm/await-run runtime run-id #(= :claimed (:status %)))

          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/item-completed
              1
              {:observation-id "obs-item-1"
               :llm-item/id "item-1"
               :content/text "second"}))
          (let [buffered-row (llm/await-run runtime run-id #(contains? (:obs-buffer %) 1))]
            (is (= -1 (:last-seq buffered-row)))
            (is (empty? (:items-by-id buffered-row))))

          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/item-completed
              0
              {:observation-id "obs-item-0"
               :llm-item/id "item-0"
               :content/text "first"}))

          (let [view (llm/await-view runtime run-id #(= 1 (:last-seq %)))]
            (is (= ["item-0" "item-1"] (mapv :llm-item/id (:items view))))
            (is (empty? (:obs-buffer (llm/read-run runtime run-id))))))))))

(deftest approval-pending-native-id-test
  (with-llm-runtime
    (fn [runtime]
      (testing "approval observations retain the native JSON-RPC request id"
        (let [run-id "run_approval"
              _ (append-run-and-await-pending! runtime run-id)
              claim (llm/claim-record
                      run-id
                      "llm-thread-A"
                      "executor-approval"
                      {:claim-token "claim-token-approval"
                       :executor-task-id llm/pending-task-id})]
          (llm/append-claim! runtime claim)
          (llm/await-run runtime run-id #(= :claimed (:status %)))
          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/approval-request
              0
              {:observation-id "obs-approval-0"
               :approval/id "approval-7"
               :approval/type :exec
               :native/json-rpc-request-id 44
               :codex/event-method "item/cmdExec/requestApproval"
               :codex/event-params {:cmd "touch x"}}))
          (let [approval (llm/await-materialized
                           #(llm/read-pending-approval runtime "approval-7")
                           some?)]
            (is (= :pending (:status approval)))
            (is (= 44 (:native/json-rpc-request-id approval)))
            (is (= run-id (:llm-turn-run/id approval)))
            (is (= :blocked-awaiting-approval
                   (:status (llm/read-run runtime run-id))))))))))
