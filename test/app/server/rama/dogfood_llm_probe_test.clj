(ns app.server.rama.dogfood-llm-probe-test
  "Depot-adversary probes for the LLM kernel (fix session 2).

   One IPC launch, one deftest, testing blocks ordered so benign probes run
   before the ones that target the unknown-run / blank-id paths. Every probe
   asserts the semantic payload the spec promises (status, claim fields,
   approval rows, native ids, audit reasons), not just row existence.

   Probes pinned here (pre-fix all of these failed — see
   docs/retros/rama/02-llm/FINDINGS.md and the prior retro's RAMA_REVIEW):
   observation without claim, wrong-token observation, duplicate run request
   after :succeeded (same + conflicting payload), approval-resolve for a
   never-requested approval, buffered approval reaching the pending index,
   post-terminal stickiness in both directions, first-resolution-wins,
   run-close approval expiry, executor recovery index, unknown-run
   dead-letters, blank-id drops, terminal buffer fence."
  (:require [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.probe-harness :as probe]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer [foreign-append!]]))

(defn fence!
  "Settle fence: append a throwaway valid request and await its decision. Once
   the fence's decision is visible the microbatch has consumed past every
   record appended before it on the same partition-set. The fence run targets
   a dangling inbox so it is never executed."
  [runtime]
  (let [fence-id (str "llm-run-fence-" (System/nanoTime))
        request (llm/turn-run-request
                  "space-fence" (str fence-id "/turn") (str fence-id "/bundle")
                  {:llm-turn-run-id fence-id
                   :llm-thread-id (str fence-id "/thread")
                   :request-id (str fence-id "/request")
                   :time-ms 1
                   :executor-task-id "fence-inbox"})]
    (llm/append-turn-run-request! runtime request)
    (llm/await-materialized #(llm/read-decision runtime fence-id) some? 5000)))

(defn request-and-await-pending!
  [runtime run-id thread-id]
  (let [request (llm/turn-run-request
                  (str "space-" run-id) (str run-id "/turn") (str run-id "/bundle")
                  {:llm-turn-run-id run-id
                   :llm-thread-id thread-id
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

(defn claim-and-await-grant!
  [runtime run-id thread-id executor-id token]
  (let [claim (llm/claim-record run-id thread-id executor-id
                                {:claim-token token
                                 :claimed-at-ms 2
                                 :executor-task-id llm/pending-task-id})]
    (llm/append-claim! runtime claim)
    (llm/await-run runtime run-id #(= :claimed (:status %)))
    claim))

(defn obs
  "Observation stamped with a claim proof."
  [run-id thread-id claim obs-type sequence opts]
  (llm/observation run-id thread-id obs-type sequence
                   (merge {:executor/id (:executor/id claim)
                           :claim/token (:claim/token claim)}
                          opts)))

(defn run-truth
  [runtime run-id]
  (let [row (llm/read-run runtime run-id)]
    {:status (:status row)
     :claimed-by (:claimed-by row)
     :claim-token (:claim/token row)
     :last-seq (:last-seq row)
     :request-id (:request/id row)
     :items (set (keys (:items-by-id row)))
     :pending? (contains? (llm/read-pending runtime llm/pending-task-id) run-id)}))

(defn error-reasons
  [runtime run-id]
  (mapv :reason (:observation-errors (llm/read-run runtime run-id))))

(deftest llm-adversary-probe-matrix-test
  (let [runtime (llm/start-llm-runtime!)]
    (try
      (testing "P1 observation without any claim on a :pending run: not authorized, run stays claimable"
        (let [run-id "run_p1"
              thread-id "th-p1"
              _ (request-and-await-pending! runtime run-id thread-id)
              forged (llm/observation run-id thread-id :codex/item-completed 0
                                      {:observation-id "obs-p1-forged"
                                       :llm-item/id "item-p1-forged"
                                       :content/text "forged"
                                       :raw/json {}})
              result (probe/probe-unauthorized-observation!
                       {:read-state #(run-truth runtime run-id)
                        :append-obs! #(llm/append-observation! runtime forged)
                        :settle! #(fence! runtime)
                        :read-signal #(error-reasons runtime run-id)})]
          (is (= :pending (get-in result [:after :status]))
              "a claimless observation must never advance a :pending run")
          (is (nil? (get-in result [:after :claimed-by])))
          (is (= -1 (get-in result [:after :last-seq])))
          (is (empty? (get-in result [:after :items])) "no item may materialize")
          (is (true? (get-in result [:after :pending?])) "run must remain discoverable")
          (is (some #{:observation/not-authorized} (:signal-after result))
              "the forgery must be auditable on the run row")
          ;; the run must still be claimable and complete normally afterwards
          (let [claim (claim-and-await-grant! runtime run-id thread-id "exec-p1" "tok-p1")]
            (llm/append-observation!
              runtime (obs run-id thread-id claim :codex/run-finished 0
                           {:observation-id "obs-p1-finish" :raw/json {}}))
            (is (= :succeeded
                   (:status (llm/await-run runtime run-id
                                           #(= :succeeded (:status %)))))))))

      (testing "P2 wrong-token observation against a claimed run: truth unchanged, auditable"
        (let [run-id "run_p2"
              thread-id "th-p2"
              _ (request-and-await-pending! runtime run-id thread-id)
              _ (claim-and-await-grant! runtime run-id thread-id "exec-p2" "tok-p2-good")
              forged (llm/observation run-id thread-id :codex/item-completed 0
                                      {:observation-id "obs-p2-forged"
                                       :executor/id "exec-evil"
                                       :claim/token "tok-p2-wrong"
                                       :llm-item/id "item-p2-forged"
                                       :content/text "forged"
                                       :raw/json {}})
              result (probe/probe-unauthorized-observation!
                       {:read-state #(dissoc (run-truth runtime run-id) :pending?)
                        :append-obs! #(llm/append-observation! runtime forged)
                        :settle! #(fence! runtime)
                        :read-signal #(error-reasons runtime run-id)})]
          (is (= :claimed (get-in result [:after :status])))
          (is (= "exec-p2" (get-in result [:after :claimed-by])))
          (is (= "tok-p2-good" (get-in result [:after :claim-token]))
              "the stored grant must survive a wrong-token write")
          (is (= -1 (get-in result [:after :last-seq])))
          (is (empty? (get-in result [:after :items])))
          (is (some #{:observation/not-authorized} (:signal-after result)))))

      (testing "P3 identical request replay after :succeeded: total no-op, no respawn surface"
        (let [run-id "run_p3"
              thread-id "th-p3"
              request (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p3" "tok-p3")]
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 0
                         {:observation-id "obs-p3-finish" :raw/json {}}))
          (llm/await-run runtime run-id #(= :succeeded (:status %)))
          (let [result (probe/probe-duplicate-id-same-payload!
                         {:read-state #(run-truth runtime run-id)
                          :append! #(llm/append-turn-run-request! runtime request)
                          :settle! #(fence! runtime)})]
            (is (:pass? result) (pr-str (dissoc result :before :after)))
            (is (= :succeeded (get-in result [:after :status]))
                "terminal status must survive an identical redelivery")
            (is (= 0 (get-in result [:after :last-seq]))
                "folded progress must not reset")
            (is (= "tok-p3" (get-in result [:after :claim-token]))
                "the claim token must not be wiped")
            (is (false? (get-in result [:after :pending?]))
                "the inbox must not re-arm the spawn path"))))

      (testing "P4 conflicting request reusing a committed run id: decision never flips, nothing re-pends"
        (let [run-id "run_p4"
              thread-id "th-p4"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p4" "tok-p4")]
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 0
                         {:observation-id "obs-p4-finish" :raw/json {}}))
          (llm/await-run runtime run-id #(= :succeeded (:status %)))
          (let [conflicting (llm/turn-run-request
                              "space-p4-evil" "WT-p4-evil" "B-p4-evil"
                              {:llm-turn-run-id run-id
                               :llm-thread-id "th-p4-evil"
                               :request-id "req-p4-evil"
                               :time-ms 9
                               :executor-task-id "inbox-p4-evil"})
                result (probe/probe-duplicate-id-different-payload!
                         {:read-state #(assoc (run-truth runtime run-id)
                                              :pending-evil?
                                              (contains? (llm/read-pending runtime "inbox-p4-evil")
                                                         run-id))
                          :append! #(llm/append-turn-run-request! runtime conflicting)
                          :settle! #(fence! runtime)
                          :read-signal #(select-keys (llm/read-decision runtime run-id)
                                                     [:request/id :decision/status])})]
            (is (:pass? result) (pr-str (dissoc result :before :after)))
            (is (= :succeeded (get-in result [:after :status])))
            (is (= (str run-id "/request") (get-in result [:after :request-id]))
                "the run row must still record the FIRST request")
            (is (= {:request/id (str run-id "/request") :decision/status :accepted}
                   (:signal-after result))
                "the committed decision must stand")
            (is (false? (get-in result [:after :pending-evil?]))
                "the conflicting request must not land in a second inbox"))))

      (testing "P5 approval-resolve for a never-requested approval: nothing invented"
        (let [run-id "run_p5"
              thread-id "th-p5"
              approval-id "approval-p5-never-requested"
              _ (request-and-await-pending! runtime run-id thread-id)
              _ (claim-and-await-grant! runtime run-id thread-id "exec-p5" "tok-p5")
              control (llm/control-record run-id :approval/resolve
                                          {:control-id "ctl-p5-invent"
                                           :llm-thread-id thread-id
                                           :approval/id approval-id
                                           :native/json-rpc-request-id 999
                                           :decision :approved
                                           :time-ms 5})]
          (llm/append-control! runtime control)
          (fence! runtime)
          (let [row (llm/read-run runtime run-id)]
            (is (nil? (get (llm/read-approvals-by-run runtime run-id) approval-id))
                "no approval row may be invented from a control")
            (is (nil? (llm/read-pending-approval runtime approval-id)))
            (is (= :claimed (:status row)) "run truth unchanged")
            (is (some #(= :approval/unknown (:reason %)) (:observation-errors row))
                "the invention attempt must be auditable")
            (is (= :approval/resolve
                   (:control/type (llm/read-control runtime "ctl-p5-invent")))
                "the control itself stays on the durable trail"))))

      (testing "P8 buffered approval reaches the pending index after the gap fills"
        (let [run-id "run_p8"
              thread-id "th-p8"
              approval-id "approval-p8"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p8" "tok-p8")]
          ;; approval at seq 1 arrives ahead of the seq-0 gap: it must buffer
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/approval-request 1
                         {:observation-id "obs-p8-approval"
                          :approval/id approval-id
                          :approval/type :exec
                          :native/json-rpc-request-id 77
                          :raw/json {}}))
          (llm/await-run runtime run-id #(contains? (:obs-buffer %) 1))
          (fence! runtime)
          (is (nil? (llm/read-pending-approval runtime approval-id))
              "a buffered approval must not be indexed before it folds")
          ;; gap filler drains the buffer; the approval materializes via drain
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/item-completed 0
                         {:observation-id "obs-p8-item"
                          :llm-item/id "item-p8"
                          :content/text "gap filler"
                          :raw/json {}}))
          (let [pending (llm/await-materialized
                          #(llm/read-pending-approval runtime approval-id) some?)]
            (is (= :pending (:status pending))
                "a drain-materialized approval MUST reach the pending index")
            (is (= 77 (:native/json-rpc-request-id pending)))
            (is (= :blocked-awaiting-approval
                   (:status (llm/read-run runtime run-id)))))))

      (testing "P10 first approval resolution wins; the second is a no-op"
        (let [run-id "run_p10"
              thread-id "th-p10"
              approval-id "approval-p10"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p10" "tok-p10")]
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/approval-request 0
                         {:observation-id "obs-p10-approval"
                          :approval/id approval-id
                          :native/json-rpc-request-id 88
                          :raw/json {}}))
          (llm/await-run runtime run-id #(= :blocked-awaiting-approval (:status %)))
          (llm/append-control!
            runtime (llm/control-record run-id :approval/resolve
                                        {:control-id "ctl-p10-approve"
                                         :llm-thread-id thread-id
                                         :approval/id approval-id
                                         :decision :approved
                                         :time-ms 10}))
          (llm/await-run runtime run-id #(= :running (:status %)))
          ;; second resolution (denied) must not flip the approval or fail the run
          (llm/append-control!
            runtime (llm/control-record run-id :approval/resolve
                                        {:control-id "ctl-p10-deny-late"
                                         :llm-thread-id thread-id
                                         :approval/id approval-id
                                         :decision :denied
                                         :time-ms 11}))
          (fence! runtime)
          (let [row (llm/read-run runtime run-id)
                approval (get (llm/read-approvals-by-run runtime run-id) approval-id)]
            (is (= :running (:status row)) "a late deny must not fail the run")
            (is (= :approved (:status approval)) "first resolution wins")
            (is (= :approved (:decision approval)))
            (is (nil? (llm/read-pending-approval runtime approval-id))))))

      (testing "P11 run-finished with an unresolved approval expires it durably"
        (let [run-id "run_p11"
              thread-id "th-p11"
              approval-id "approval-p11"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p11" "tok-p11")]
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/approval-request 0
                         {:observation-id "obs-p11-approval"
                          :approval/id approval-id
                          :native/json-rpc-request-id 66
                          :raw/json {}}))
          (llm/await-run runtime run-id #(= :blocked-awaiting-approval (:status %)))
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 1
                         {:observation-id "obs-p11-finish" :raw/json {}}))
          (llm/await-run runtime run-id #(= :succeeded (:status %)))
          (fence! runtime)
          (let [approval (get (llm/read-approvals-by-run runtime run-id) approval-id)]
            (is (= :expired (:status approval))
                "a run close must expire its unresolved approvals")
            (is (= :run-closed (:reason approval)))
            (is (nil? (llm/read-pending-approval runtime approval-id))
                "the pending index row must be cleared at close"))))

      (testing "P9 terminal statuses are sticky in both directions"
        ;; succeeded ← late cancel
        (let [run-id "run_p9a"
              thread-id "th-p9a"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p9a" "tok-p9a")]
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 0
                         {:observation-id "obs-p9a-finish" :raw/json {}}))
          (llm/await-run runtime run-id #(= :succeeded (:status %)))
          (let [result (probe/probe-post-terminal-write!
                         {:read-state #(select-keys (llm/read-run runtime run-id)
                                                    [:status :finished-at :cancelled-by])
                          :append-late! #(llm/append-control!
                                           runtime
                                           (llm/control-record run-id :turn/cancel
                                                               {:control-id "ctl-p9a-cancel"
                                                                :llm-thread-id thread-id
                                                                :time-ms 20}))
                          :settle! #(fence! runtime)})]
            (is (:pass? result) (pr-str (dissoc result :before :after)))
            (is (= :succeeded (get-in result [:after :status]))
                "a late cancel must not regress :succeeded")
            (is (= "ctl-p9a-cancel"
                   (get-in (llm/read-run runtime run-id)
                           [:controls-by-id "ctl-p9a-cancel" :control/id]))
                "the late cancel stays recorded on the trail")))
        ;; cancelled ← late authorized run-finished
        (let [run-id "run_p9b"
              thread-id "th-p9b"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p9b" "tok-p9b")]
          (llm/append-control!
            runtime (llm/control-record run-id :turn/cancel
                                        {:control-id "ctl-p9b-cancel"
                                         :llm-thread-id thread-id
                                         :time-ms 21}))
          (llm/await-run runtime run-id #(= :cancelled (:status %)))
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 0
                         {:observation-id "obs-p9b-finish" :raw/json {}}))
          (fence! runtime)
          (is (= :cancelled (:status (llm/read-run runtime run-id)))
              "an authorized late run-finished must not flip :cancelled")))

      (testing "P12 executor recovery index: grant adds, terminal removes, token survives :running"
        (let [run-id "run_p12"
              thread-id "th-p12"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p12" "tok-p12")]
          (is (contains? (llm/read-executor-active-runs runtime "exec-p12") run-id)
              "the grant must register the run in the recovery index")
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/item-completed 0
                         {:observation-id "obs-p12-item"
                          :llm-item/id "item-p12"
                          :content/text "progress"
                          :raw/json {}}))
          (llm/await-run runtime run-id #(= :running (:status %)))
          (is (= :granted-to-us (llm/claim-state (llm/read-run runtime run-id) claim))
              "the rightful owner must verify its grant after :running (restart recovery)")
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 1
                         {:observation-id "obs-p12-finish" :raw/json {}}))
          (llm/await-run runtime run-id #(= :succeeded (:status %)))
          (llm/await-materialized
            #(llm/read-executor-active-runs runtime "exec-p12")
            #(not (contains? % run-id)))
          (is (not (contains? (llm/read-executor-active-runs runtime "exec-p12") run-id))
              "a terminal run must leave the recovery index")
          (is (= :conflict-or-past
                 (llm/claim-state (llm/read-run runtime run-id) claim))
              "recovery must never respawn finished work")))

      (testing "P13 winner-claim redelivery: grant fields identical, no double effects"
        (let [run-id "run_p13"
              thread-id "th-p13"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p13" "tok-p13")
              result (probe/probe-duplicate-id-same-payload!
                       {:read-state #(select-keys (llm/read-run runtime run-id)
                                                  [:status :claimed-by :claim/token
                                                   :claimed-at :updated-at])
                        :append! #(llm/append-claim! runtime claim)
                        :settle! #(fence! runtime)})]
          (is (:pass? result) (pr-str result))
          (is (= "exec-p13" (get-in result [:after :claimed-by])))
          (is (false? (contains? (llm/read-pending runtime llm/pending-task-id) run-id)))
          (is (contains? (llm/read-executor-active-runs runtime "exec-p13") run-id))))

      (testing "P15 buffered later-seq observation cannot reopen a terminal run"
        (let [run-id "run_p15"
              thread-id "th-p15"
              _ (request-and-await-pending! runtime run-id thread-id)
              claim (claim-and-await-grant! runtime run-id thread-id "exec-p15" "tok-p15")]
          ;; buffer order: item seq 2 and finish seq 1 buffer behind the seq-0 gap;
          ;; the drain applies seq 0, closes at seq 1, and MUST discard seq 2
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/item-completed 2
                         {:observation-id "obs-p15-late-item"
                          :llm-item/id "item-p15-late"
                          :content/text "after the end"
                          :raw/json {}}))
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/run-finished 1
                         {:observation-id "obs-p15-finish" :raw/json {}}))
          (llm/append-observation!
            runtime (obs run-id thread-id claim :codex/item-completed 0
                         {:observation-id "obs-p15-item"
                          :llm-item/id "item-p15"
                          :content/text "early"
                          :raw/json {}}))
          (llm/await-run runtime run-id #(= :succeeded (:status %)))
          (fence! runtime)
          (let [row (llm/read-run runtime run-id)]
            (is (= :succeeded (:status row)))
            (is (= {} (:obs-buffer row)) "buffer must be cleared at terminal")
            (is (= #{"item-p15"} (set (keys (:items-by-id row))))
                "the buffered post-close item must be discarded, not applied"))))

      (testing "P6 unknown-run observation: dead-lettered, no phantom state, topology alive"
        (let [orphan (llm/observation "run_p6_never_requested" "th-p6"
                                      :codex/item-completed 0
                                      {:observation-id "obs-p6-orphan"
                                       :executor/id "exec-ghost"
                                       :claim/token "tok-ghost"
                                       :llm-item/id "item-p6"
                                       :content/text "boo"
                                       :raw/json {}})
              result (probe/probe-append-before-request!
                       {:read-state #(llm/read-run runtime "run_p6_never_requested")
                        :append-obs! #(llm/append-observation! runtime orphan)
                        :settle! #(fence! runtime)
                        :read-signal #(llm/read-dead-letters runtime "run_p6_never_requested")})]
          (is (:pass? result) (pr-str (dissoc result :signal-after)))
          (is (nil? (llm/read-view runtime "run_p6_never_requested")))
          (let [letters (:signal-after result)]
            (is (= 1 (count letters)) "never-drop: the orphan must be ledgered")
            (is (= :observation/unknown-run (:dead-letter/reason (first letters)))))
          ;; topology must keep processing afterwards
          (request-and-await-pending! runtime "run_p6_alive" "th-p6-alive")
          (is (= :accepted (:decision/status (llm/read-decision runtime "run_p6_alive"))))))

      (testing "P7 unknown-run control: dead-lettered, nothing invented"
        (let [control (llm/control-record "run_p7_never_requested" :approval/resolve
                                          {:control-id "ctl-p7-orphan"
                                           :llm-thread-id "th-p7"
                                           :approval/id "approval-p7"
                                           :decision :approved
                                           :time-ms 7})]
          (llm/append-control! runtime control)
          (fence! runtime)
          (is (nil? (llm/read-run runtime "run_p7_never_requested")))
          (is (nil? (llm/read-pending-approval runtime "approval-p7")))
          (let [letters (llm/read-dead-letters runtime "run_p7_never_requested")]
            (is (= 1 (count letters)))
            (is (= :control/unknown-run (:dead-letter/reason (first letters)))))))

      (testing "P14 blank run-id request: refused client-side, dropped topology-side"
        (let [base (llm/turn-run-request
                     "space-p14" "WT-p14" "B-p14"
                     {:llm-turn-run-id "run_p14_tmp"
                      :llm-thread-id "th-p14"
                      :request-id "req-p14"
                      :time-ms 1})
              blank (assoc base
                           :llm-turn-run/id ""
                           :routing/key (llm/llm-routing-key ""))]
          (is (thrown? IllegalArgumentException
                       (llm/append-turn-run-request! runtime blank))
              "the client helper must refuse an identity-less request")
          ;; raw append bypasses the client refusal on purpose
          (foreign-append! (:llm-depot runtime) blank :append-ack)
          (fence! runtime)
          (is (nil? (llm/read-decision runtime "")) "no decision row under \"\"")
          (is (nil? (llm/read-run runtime "")) "no run row under \"\"")
          (request-and-await-pending! runtime "run_p14_alive" "th-p14-alive")
          (is (= :accepted (:decision/status (llm/read-decision runtime "run_p14_alive")))
              "topology must keep processing after a blank-keyed record")))

      (finally
        (llm/close-llm-runtime! runtime)))))
