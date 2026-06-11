(ns app.server.rama.dogfood-llm-test
  (:require [app.server.rama.dogfood.llm :as llm]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def claude-stream-sample-lines
  ["{\"type\":\"system\",\"session_id\":\"sesh_123\"}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"message_start\",\"message\":{\"id\":\"msg_013Zva2CMHLNnXjNJJKqJ2EF\",\"type\":\"message\",\"role\":\"assistant\",\"content\":[],\"model\":\"claude-3-5-sonnet-20241022\",\"stop_reason\":null,\"stop_sequence\":null,\"usage\":{\"input_tokens\":2095,\"output_tokens\":1}}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_start\",\"index\":0,\"content_block\":{\"type\":\"text\",\"text\":\"\"}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\"I will check the\"}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_delta\",\"index\":0,\"delta\":{\"type\":\"text_delta\",\"text\":\" current directory.\"}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_stop\",\"index\":0}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_start\",\"index\":1,\"content_block\":{\"type\":\"tool_use\",\"id\":\"toolu_01T1x1fJ34qAmk2tTg\",\"name\":\"ls\",\"input\":{}}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"{\\\"pa\"}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_delta\",\"index\":1,\"delta\":{\"type\":\"input_json_delta\",\"partial_json\":\"th\\\": \\\".\\\"}\"}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_stop\",\"index\":1}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"message_delta\",\"delta\":{\"stop_reason\":\"tool_use\",\"stop_sequence\":null},\"usage\":{\"output_tokens\":80}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"message_stop\"}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_start\",\"index\":2,\"content_block\":{\"type\":\"tool_result\",\"tool_use_id\":\"toolu_01T1x1fJ34qAmk2tTg\",\"content\":\"file1.txt\\nfile2.txt\"}}}"
   "{\"type\":\"stream_event\",\"event\":{\"type\":\"content_block_stop\",\"index\":2}}"
   "{\"type\":\"result\",\"session_id\":\"sesh_123\",\"total_cost_usd\":0.0001}"])

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
                  (str run-id "/turn")
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
        space-id "space-writer-asymmetry"
        turn-id "turn-writer-asymmetry"
        bundle-id "bundle-writer-asymmetry"
        executor-task-id llm/pending-task-id
        approval-id "approval-writer-asymmetry"
        request (llm/turn-run-request
                  space-id
                  turn-id
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
           :space-id space-id
           :turn-id turn-id
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
                   :executor/id "executor-writer-asymmetry"
                   :claim/token "claim-token-writer-asymmetry"
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
                   :executor/id "executor-writer-asymmetry"
                   :claim/token "claim-token-writer-asymmetry"
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
                   :executor/id "executor-writer-asymmetry"
                   :claim/token "claim-token-writer-asymmetry"
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
                   :executor/id "executor-writer-asymmetry"
                   :claim/token "claim-token-writer-asymmetry"
                   :received-at-ms 150
                   :approval/id approval-id
                   :approval/type :exec
                   :native/json-rpc-request-id 77
                   :codex/event-method "item/cmdExec/requestApproval"
                   :codex/event-params {:cmd "echo writer-asymmetry"}})]]}))

(defn llm-spine-pstate-snapshot
  [runtime {:keys [run-id thread-id space-id turn-id
                   executor-task-id approval-id]}]
  {:decision (llm/read-decision runtime run-id)
   :run (llm/read-run runtime run-id)
   :view (llm/read-view runtime run-id)
   :thread (llm/read-thread runtime thread-id)
   :thread-binding (llm/read-thread-binding runtime space-id)
   :runs-by-thread (llm/read-runs-by-thread runtime thread-id)
   :run-by-turn (llm/read-run-for-turn runtime turn-id)
   :pending-by-task (llm/read-pending runtime executor-task-id)
   :items-by-run (llm/read-items-by-run runtime run-id)
   :items-by-thread (llm/read-items-by-thread runtime thread-id)
   :raw-response-items (llm/read-raw-response-items runtime run-id)
   :tool-calls-by-run (llm/read-tool-calls-by-run runtime run-id)
   :approvals-by-run (llm/read-approvals-by-run runtime run-id)
   :pending-approval (llm/read-pending-approval runtime approval-id)
   :token-usage (llm/read-token-usage runtime run-id)
   :cost-by-thread (llm/read-cost-by-thread runtime thread-id)
   :run-detail-projection (llm/read-run-detail-projection runtime run-id)})

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
            :payload/execution-options-not-bundle-owned))))

  (testing "missing backend stays Codex-compatible while Claude is scalar-discriminated"
    (let [legacy-request (dissoc (llm/turn-run-request
                                   "chat-legacy"
                                   "WT-legacy"
                                   "B-legacy"
                                   {:llm-turn-run-id "run-legacy"})
                                 :llm/backend)
          normalized (llm/normalize-turn-run-request legacy-request)
          claude-request (llm/turn-run-request
                           "chat-claude"
                           "WT-claude"
                           "B-claude"
                           {:llm-turn-run-id "run-claude"
                            :llm/backend :claude
                            :llm/auth-mode :subscription})]
      (is (= :codex (:llm/backend normalized)))
      (is (= :codex (get-in normalized [:executor :agent/kind])))
      (is (empty? (llm/request-validation-errors normalized)))
      (is (= :claude (:llm/backend claude-request)))
      (is (= :claude (get-in claude-request [:executor :agent/kind])))
      (is (= :subscription (:llm/auth-mode claude-request)))
      (is (= :local-claude (get-in claude-request [:payload :executor/pool])))
      (is (empty? (llm/request-validation-errors claude-request)))))

  (testing "passive observation is rejected on active LLM run requests"
    (let [request (llm/turn-run-request
                    "chat-passive"
                    "WT-passive"
                    "B-passive"
                    {:llm-turn-run-id "run-passive"
                     :llm/backend :claude
                     :llm/auth-mode :passive-observe})
          errors (llm/request-validation-errors request)]
      (is (contains? (set (map :type errors))
                     :llm/auth-mode-passive-observe-not-active)))))

(deftest claude-spawn-env-contract-test
  (testing "subscription mode uses stream-json flags, omits --bare, and strips auth env"
    (let [run-row {:llm-turn-run/id "run-claude-sub"
                   :llm/backend :claude
                   :llm/auth-mode :subscription}
          argv (llm/claude-stream-argv run-row)
          env (llm/claude-child-env
                :subscription
                {:env {"CLAUDE_CODE_OAUTH_TOKEN" "oauth-secret"
                       "ANTHROPIC_API_KEY" "api-secret"
                       "KEEP_ME" "ok"}})]
      (is (= ["claude" "-p"] (subvec argv 0 2)))
      (is (some #{"--input-format"} argv))
      (is (some #{"stream-json"} argv))
      (is (some #{"--output-format"} argv))
      (is (some #{"--include-partial-messages"} argv))
      (is (some #{"--include-hook-events"} argv))
      (is (some #{"--replay-user-messages"} argv))
      (is (not (some #{"--bare"} argv)))
      (is (= {"KEEP_ME" "ok"} env))))

  (testing "api-key mode uses --bare and injects only the resolved Anthropic key"
    (let [run-row {:llm-turn-run/id "run-claude-api"
                   :llm/backend :claude
                   :llm/auth-mode :api-key}
          argv (llm/claude-stream-argv run-row)
          env (llm/claude-child-env
                :api-key
                {:env {"CLAUDE_CODE_OAUTH_TOKEN" "oauth-secret"
                       "ANTHROPIC_AUTH_TOKEN" "auth-secret"
                       "KEEP_ME" "ok"}
                 :anthropic-api-key-secret :sid-secret
                 :secret-resolver {:sid-secret "api-secret"}})]
      (is (some #{"--bare"} argv))
      (is (= "api-secret" (get env "ANTHROPIC_API_KEY")))
      (is (= "ok" (get env "KEEP_ME")))
      (is (not (contains? env "CLAUDE_CODE_OAUTH_TOKEN")))
      (is (not (contains? env "ANTHROPIC_AUTH_TOKEN"))))))

(deftest claude-stream-json-adapter-fold-test
  (with-llm-runtime
    (fn [runtime]
      (testing "Claude stream-json lines fold into the generic LLM run surface"
        (let [run-id "run_claude_stream"
              request (llm/turn-run-request
                        "chat-claude-stream"
                        "WT-claude-stream"
                        "B-claude-stream"
                        {:llm-turn-run-id run-id
                         :llm-thread-id "llm-thread-claude-stream"
                         :request-id "req-claude-stream"
                         :time-ms 1
                         :llm/backend :claude
                         :llm/auth-mode :subscription
                         :executor-task-id llm/pending-task-id})
              redacted-events (llm/claude-stream-json-lines->events
                                ["{\"type\":\"system\",\"session_id\":\"sesh_secret\",\"api_key\":\"never-store-me\"}"
                                 "{\"type\":\"result\",\"session_id\":\"sesh_secret\",\"total_cost_usd\":0.1}"])]
          (llm/append-turn-run-request! runtime request)
          (llm/await-run runtime run-id #(= :pending (:status %)))
          (let [result (llm/run-one-pending-with-claude!
                         runtime
                         {:executor-id "executor-claude-stream"
                          :load-context-bundle (fn [bundle-id]
                                                  {:context-bundle/id bundle-id
                                                   :rendered/model-input "hello claude"})
                          :lines claude-stream-sample-lines})
                view (llm/await-view runtime run-id #(= :succeeded (:status %)))
                run (llm/read-run runtime run-id)
                tool-calls (llm/read-tool-calls-by-run runtime run-id)
                raw-items (llm/read-raw-response-items runtime run-id)]
            (is (= :granted-to-us (:claim-state result)))
            (is (= :claude (:llm/backend run)))
            (is (= :subscription (:llm/auth-mode run)))
            (is (= "sesh_123" (:native/claude-session-id run)))
            (is (= :succeeded (:status view)))
            (is (some #(= :claude (:source %)) (:items view)))
            (is (seq tool-calls))
            (is (= 2095 (get-in view [:token-usage :tokens/input-total])))
            (is (= 80 (get-in view [:token-usage :tokens/output])))
            (is (every? #(not (str/includes? (pr-str %) "never-store-me"))
                        redacted-events))
            (is (every? #(not (str/includes? (pr-str %) "never-store-me"))
                        (vals raw-items)))))))))

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
      (is (= 10 (get-in snapshot-a [:cost-by-thread :tokens :tokens/input-total])))
      (is (= 2 (get-in snapshot-a [:cost-by-thread :tokens :tokens/reasoning-output])))
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
          (is (= run-id (llm/read-run-for-turn runtime (str run-id "/turn"))))
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
               :executor/id "executor-a"
               :claim/token "claim-token-a"
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
               :executor/id "executor-a"
               :claim/token "claim-token-a"
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
              {:observation-id "obs-finish-2"
               :executor/id "executor-a"
               :claim/token "claim-token-a"}))

          (let [view (llm/await-view runtime run-id #(= :succeeded (:status %)))
                run-detail (llm/await-materialized
                             #(llm/read-run-detail-projection runtime run-id)
                             #(= :succeeded (:status %)))
                items (llm/read-items-by-run runtime run-id)
                indexed-item (llm/read-item-by-id runtime "item-0")
                usage (llm/read-token-usage runtime run-id)]
            (is (= :succeeded (:status view)))
            (is (= :llm-run-detail (:projection/type run-detail)))
            (is (= :llm-turn-runs (:projection/source run-detail)))
            (is (= ["item-0"] (mapv :llm-item/id (:items run-detail))))
            (is (= 50000 (get-in run-detail [:token-usage :tokens/input-total])))
            (is (= ["item-0"] (mapv :llm-item/id (:items view))))
            (is (= "hello from codex" (get-in items ["item-0" :content/text])))
            (is (= "hello from codex" (:content/text indexed-item)))
            (is (= run-id (:llm-turn-run/id indexed-item)))
            (is (= 50000 (:tokens/input-total usage)))
            (is (= 45000 (:tokens/cached-input usage)))
            (is (= 8000 (:tokens/reasoning-output usage)))))))))

(deftest cost-rollup-by-thread-test
  (with-llm-runtime
    (fn [runtime]
      (testing "token usage observations roll up per LLM thread without double-counting runs"
        (let [first-run-id "run_cost_first"
              second-run-id "run_cost_second"
              thread-id "llm-thread-A"]
          (append-run-and-await-pending! runtime first-run-id)
          (llm/append-claim!
            runtime
            (llm/claim-record first-run-id thread-id "executor-cost"
                              {:claim-token "claim-token-cost-first"
                               :executor-task-id llm/pending-task-id}))
          (llm/await-run runtime first-run-id #(= :claimed (:status %)))
          (llm/append-observation!
            runtime
            (llm/observation
              first-run-id
              thread-id
              :codex/token-usage
              0
              {:observation-id "obs-cost-first"
               :executor/id "executor-cost"
               :claim/token "claim-token-cost-first"
               :tokens/input-total 10
               :tokens/cached-input 4
               :tokens/output 3
               :tokens/reasoning-output 2}))
          (llm/await-materialized
            #(llm/read-cost-by-thread runtime thread-id)
            #(= 1 (:run-count %)))

          (append-run-and-await-pending! runtime second-run-id)
          (llm/append-claim!
            runtime
            (llm/claim-record second-run-id thread-id "executor-cost"
                              {:claim-token "claim-token-cost-second"
                               :executor-task-id llm/pending-task-id}))
          (llm/await-run runtime second-run-id #(= :claimed (:status %)))
          (llm/append-observation!
            runtime
            (llm/observation
              second-run-id
              thread-id
              :codex/token-usage
              0
              {:observation-id "obs-cost-second"
               :executor/id "executor-cost"
               :claim/token "claim-token-cost-second"
               :tokens/input-total 7
               :tokens/cached-input 1
               :tokens/output 5
               :tokens/reasoning-output 0}))

          (let [rollup (llm/await-materialized
                         #(llm/read-cost-by-thread runtime thread-id)
                         #(= 2 (:run-count %)))]
            (is (= :llm-thread-cost-rollup (:projection/type rollup)))
            (is (= :llm-token-usage-by-run-id (:projection/source rollup)))
            (is (= 17 (get-in rollup [:tokens :tokens/input-total])))
            (is (= 5 (get-in rollup [:tokens :tokens/cached-input])))
            (is (= 8 (get-in rollup [:tokens :tokens/output])))
            (is (= 2 (get-in rollup [:tokens :tokens/reasoning-output])))
            (is (= #{first-run-id second-run-id} (set (keys (:runs rollup)))))
            (is (= 10 (get-in rollup [:runs first-run-id :token-usage :tokens/input-total])))
            (is (= 7 (get-in rollup [:runs second-run-id :token-usage :tokens/input-total])))))))))

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
               :executor/id "executor-buffer"
               :claim/token "claim-token-buffer"
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
               :executor/id "executor-buffer"
               :claim/token "claim-token-buffer"
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
               :executor/id "executor-approval"
               :claim/token "claim-token-approval"
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

(deftest fake-executor-claim-and-stream-test
  (with-llm-runtime
    (fn [runtime]
      (testing "executor boundary claims from Rama, loads bundle, then streams adapter observations"
        (let [run-id "run_fake_executor"
              _ (append-run-and-await-pending! runtime run-id)
              loaded-bundles (atom [])
              result (llm/run-one-pending-with-adapter!
                       runtime
                       {:executor-id "executor-fake"
                        :load-context-bundle (fn [bundle-id]
                                               (swap! loaded-bundles conj bundle-id)
                                               {:context-bundle/id bundle-id
                                                :rendered/model-input "hello model"})
                        :adapter (llm/fake-codex-adapter
                                   [{:observation/type :codex/item-completed
                                     :observation-id "obs-fake-item"
                                     :llm-item/id "item-fake"
                                     :content/text "fake adapter reply"
                                     :raw/json {:event "item/completed"}}
                                    {:observation/type :codex/token-usage
                                     :observation-id "obs-fake-usage"
                                     :tokens/input-total 12
                                     :tokens/cached-input 4
                                     :tokens/output 3}
                                    {:observation/type :codex/run-finished
                                     :observation-id "obs-fake-finish"}])})
              view (llm/await-view runtime run-id #(= :succeeded (:status %)))]
          (is (= :granted-to-us (:claim-state result)))
          (is (:spawned? result))
          (is (:spawned-after-grant? result))
          (is (= 3 (:observations-appended result)))
          (is (= [(str run-id "/bundle")] @loaded-bundles))
          (is (= "fake adapter reply"
                 (get-in (llm/read-items-by-run runtime run-id)
                         ["item-fake" :content/text])))
          (is (= 12 (get-in view [:token-usage :tokens/input-total]))))))))

(deftest stale-approval-on-executor-death-test
  (with-llm-runtime
    (fn [runtime]
      (testing "executor death expires unresolved approvals and fails the run"
        (let [run-id "run_stale_approval"
              _ (append-run-and-await-pending! runtime run-id)
              claim (llm/claim-record
                      run-id
                      "llm-thread-A"
                      "executor-stale"
                      {:claim-token "claim-token-stale"
                       :executor-task-id llm/pending-task-id})
              approval-id "approval-stale"]
          (llm/append-claim! runtime claim)
          (llm/await-run runtime run-id #(= :claimed (:status %)))
          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-A"
              :codex/approval-request
              0
              {:observation-id "obs-stale-approval"
               :executor/id "executor-stale"
               :claim/token "claim-token-stale"
               :approval/id approval-id
               :approval/type :exec
               :native/json-rpc-request-id 88
               :codex/event-method "item/cmdExec/requestApproval"}))
          (llm/await-materialized #(llm/read-pending-approval runtime approval-id) some?)
          (let [result (llm/mark-stale-approvals!
                         runtime
                         run-id
                         {:executor-id "executor-stale"
                          :time-ms 200})
                run (llm/await-run runtime run-id #(= :failed (:status %)))
                approval (get (llm/read-approvals-by-run runtime run-id) approval-id)]
            (is (= [approval-id] (:stale-approval-ids result)))
            (is (= :failed (:action result)))
            (is (= :expired (:status approval)))
            (is (= :executor-stale (:reason (llm/read-control runtime
                                                              (str run-id "/stale-approval/" approval-id)))))
            (is (nil? (llm/await-materialized
                        #(llm/read-pending-approval runtime approval-id)
                        nil?)))
            (is (= :approval/declined (get-in run [:error :reason])))))))))

(deftest run-failed-policy-on-stale-approval-test
  (with-llm-runtime
    (fn [runtime]
      (testing "stale approval follows the recorded fail policy for this MVP slice"
        (let [run-id "run_stale_policy"
              request (llm/turn-run-request
                        "chat-policy"
                        "WT-policy"
                        "B-policy"
                        {:llm-turn-run-id run-id
                         :llm-thread-id "llm-thread-policy"
                         :request-id "req-policy"
                         :time-ms 1
                         :executor-task-id llm/pending-task-id
                         :run-restart-policy :fail-on-stale-approval})
              approval-id "approval-policy"]
          (llm/append-turn-run-request! runtime request)
          (llm/await-run runtime run-id #(= :pending (:status %)))
          (llm/append-claim!
            runtime
            (llm/claim-record run-id "llm-thread-policy" "executor-policy"
                              {:claim-token "claim-token-policy"
                               :executor-task-id llm/pending-task-id}))
          (llm/await-run runtime run-id #(= :claimed (:status %)))
          (llm/append-observation!
            runtime
            (llm/observation
              run-id
              "llm-thread-policy"
              :codex/approval-request
              0
              {:observation-id "obs-policy-approval"
               :executor/id "executor-policy"
               :claim/token "claim-token-policy"
               :approval/id approval-id
               :native/json-rpc-request-id 99}))
          (llm/await-materialized #(llm/read-pending-approval runtime approval-id) some?)
          (let [result (llm/mark-stale-approvals!
                         runtime
                         run-id
                         {:executor-id "executor-policy"
                          :time-ms 250})
                run (llm/await-run runtime run-id #(= :failed (:status %)))]
            (is (= :fail-on-stale-approval (:run/restart-policy result)))
            (is (= :fail-on-stale-approval (:run/restart-policy run)))
            (is (= :failed (:action result)))
            (is (= :expired (get-in run [:error :decision])))))))))

(deftest follow-up-new-run-same-thread-test
  (with-llm-runtime
    (fn [runtime]
      (testing "a follow-up creates a new LLMTurnRun on the bound LLMThread"
        (let [thread-id "llm-thread-follow-up"
              space-id "chat-follow-up"
              native-thread-id "codex-native-thread-follow-up"
              first-run-id "run-follow-up-first"
              second-run-id "run-follow-up-second"
              first-request (llm/turn-run-request
                              space-id
                              "WT-follow-up-1"
                              "B-follow-up-1"
                              {:llm-turn-run-id first-run-id
                               :llm-thread-id thread-id
                               :request-id "req-follow-up-1"
                               :time-ms 1
                               :executor-task-id llm/pending-task-id})
              second-request (llm/turn-run-request
                               space-id
                               "WT-follow-up-2"
                               "B-follow-up-2"
                               {:llm-turn-run-id second-run-id
                                :llm-thread-id thread-id
                                :request-id "req-follow-up-2"
                                :time-ms 10
                                :executor-task-id llm/pending-task-id})
              executor-ctx (atom nil)]
          (llm/append-turn-run-request! runtime first-request)
          (llm/await-run runtime first-run-id #(= :pending (:status %)))
          (llm/run-one-pending-with-adapter!
            runtime
            {:executor-id "executor-follow-up-first"
             :adapter (llm/fake-codex-adapter
                        [{:observation/type :codex/item-completed
                          :observation-id "obs-follow-up-first-item"
                          :native/codex-thread-id native-thread-id
                          :llm-item/id "item-follow-up-first"
                          :content/text "first turn reply"}
                         {:observation/type :codex/run-finished
                          :observation-id "obs-follow-up-first-finish"
                          :native/codex-thread-id native-thread-id}])})
          (llm/await-view runtime first-run-id #(= :succeeded (:status %)))
          (llm/await-materialized
            #(llm/read-thread runtime thread-id)
            #(= native-thread-id (:native/codex-thread-id %)))

          (llm/append-turn-run-request! runtime second-request)
          (let [second-run (llm/await-run
                             runtime
                             second-run-id
                             #(= native-thread-id (:native/codex-thread-id %)))
                thread (llm/read-thread runtime thread-id)]
            (is (= :pending (:status second-run)))
            (is (= thread-id (:llm-thread/id second-run)))
            (is (= native-thread-id (:native/codex-thread-id second-run)))
            (is (= [first-run-id second-run-id] (:turn-run/ids thread)))
            (is (= first-run-id
                   (llm/read-run-for-turn runtime "WT-follow-up-1")))
            (is (= second-run-id
                   (llm/read-run-for-turn runtime "WT-follow-up-2"))))

          (llm/run-one-pending-with-adapter!
            runtime
            {:executor-id "executor-follow-up-second"
             :adapter (fn [ctx]
                        (reset! executor-ctx ctx)
                        [{:observation/type :codex/item-completed
                          :observation-id "obs-follow-up-second-item"
                          :native/codex-thread-id native-thread-id
                          :llm-item/id "item-follow-up-second"
                          :content/text "second turn reply"}
                         {:observation/type :codex/run-finished
                          :observation-id "obs-follow-up-second-finish"
                          :native/codex-thread-id native-thread-id}])})
          (llm/await-view runtime second-run-id #(= :succeeded (:status %)))

          (is (= native-thread-id
                 (get-in @executor-ctx [:run :native/codex-thread-id])))
          (is (= #{"item-follow-up-first"}
                 (set (keys (llm/read-items-by-run runtime first-run-id)))))
          (is (= #{"item-follow-up-second"}
                 (set (keys (llm/read-items-by-run runtime second-run-id)))))
          (is (= #{first-run-id second-run-id}
                 (set (keys (llm/read-runs-by-thread runtime thread-id))))))))))
