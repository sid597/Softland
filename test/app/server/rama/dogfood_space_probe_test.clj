(ns app.server.rama.dogfood-space-probe-test
  "Depot-adversary probes for the Space kernel (fix session 3).

   One IPC launch, one deftest. Every probe asserts the semantic payload the
   spec promises (decision stability, fact-family presence, proposal
   lifecycle, turn ordering), not just row existence.

   Probes pinned here (pre-fix failures recorded in
   docs/retros/rama/03-space/FINDINGS.md and the prior retro's RAMA_REVIEW):
   compose replay decision stability + fact-family survival (SP-01/SP-02),
   idempotency replay with fresh ids, idempotency conflict instead of
   aliasing (prior F3), cross-space same-key independence, typed observation
   bridge (prior F1 + SP-05), phantom-free patch resolution and
   first-resolution-wins (SP-06), proposal redelivery after resolution
   (SP-02), never-requested approval guard (prior F2, llm-side), concurrent
   space-only turns (SP-04), duplicate request id with conflicting payload,
   duplicate create canvas survival (S8)."
  (:require [app.server.rama.dogfood.llm :as llm]
            [app.server.rama.dogfood.space :as space]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer [foreign-select-one]]
            [com.rpl.rama.path :refer [keypath]]))

(def fence-counter (atom 0))

(defn fence!
  "Settle fence for the space stream: append a request that FAILS validation
   (action-type drift) but routes to the probed space's partition. It mints a
   rejected decision and zero facts; once its decision is visible, every
   record appended before it for this space has been consumed."
  [runtime space-id]
  (let [n (swap! fence-counter inc)
        fence-request-id (str "space-fence-" space-id "-" n)
        request (-> (space/space-turn-request
                      :turn/comment-create
                      space-id
                      {:request-id fence-request-id
                       :time-ms 1
                       :payload {:turn/id (str fence-request-id "/turn")}})
                    (assoc-in [:action :action/type] :fence/drift))]
    (space/append-space-action! runtime request)
    (space/await-materialized
      #(space/read-decision runtime fence-request-id) some? 5000)))

(defn compose-request
  [{:keys [space-id turn-id bundle-id run-id thread-id request-id prompt
           idempotency-key time-ms]}]
  (space/compose-and-send-request
    space-id
    (or prompt (str "Prompt for " run-id))
    (cond-> {:request-id request-id
             :time-ms (or time-ms 100)
             :payload {:turn/id turn-id
                       :context-bundle/id bundle-id
                       :llm-turn-run/id run-id
                       :llm-thread/id thread-id
                       :executor/task-id llm/pending-task-id}}
      idempotency-key (assoc :idempotency-key idempotency-key))))

(defn append-and-await-decision!
  [runtime request]
  (space/append-space-action! runtime request)
  (space/await-decision runtime (:request/id request)))

(defn append-send-and-await-run!
  [runtime ids]
  (let [request (compose-request ids)
        decision (append-and-await-decision! runtime request)]
    (llm/await-run runtime (:run-id ids) #(= :pending (:status %)))
    {:request request :decision decision}))

(defn read-decision-row
  "Read a decision row by EXPLICIT decision id (for conflict rows that live
   under `<request-id>/decision/conflict`, which read-decision cannot reach)."
  [runtime decision-id]
  (foreign-select-one (keypath decision-id) (:space-decisions-by-id runtime)))

(defn claim!
  [runtime run-id thread-id executor-id token]
  (llm/append-claim!
    runtime
    (llm/claim-record run-id thread-id executor-id
                      {:claim-token token
                       :claimed-at-ms 150
                       :executor-task-id llm/pending-task-id}))
  (llm/await-run runtime run-id #(= :claimed (:status %)))
  {:executor/id executor-id :claim/token token})

(defn finish-run!
  [runtime run-id thread-id claim]
  (llm/append-observation!
    runtime
    (llm/observation run-id thread-id :codex/run-finished 0
                     (merge claim {:observation-id (str run-id "/finish")
                                   :raw/json {}})))
  (llm/await-run runtime run-id #(= :succeeded (:status %))))

(defn patch-observation
  [{:keys [run-id thread-id space-id turn-id proposal-id obs-id sequence claim]}]
  (llm/observation
    run-id thread-id :codex/patch-proposal (or sequence 0)
    (merge (or claim {})
           {:observation-id (or obs-id (str "obs-" proposal-id))
            :space/id space-id
            :turn/id turn-id
            :patch-proposal/id proposal-id
            :summary/text (str "Patch " proposal-id)
            :patch/files [{:path "src/x.clj" :hunks 1}]})))

(deftest space-adversary-probe-matrix-test
  (let [runtime (space/start-space-runtime!)]
    (try
      (testing "P1 identical compose replay after the run finished: decision stable, fact family intact, run untouched"
        (let [ids {:space-id "chat-p1" :turn-id "WT-p1" :bundle-id "B-p1"
                   :run-id "run-p1" :thread-id "th-p1" :request-id "req-p1"}
              {:keys [request]} (append-send-and-await-run! runtime ids)
              claim (claim! runtime "run-p1" "th-p1" "exec-p1" "tok-p1")
              _ (finish-run! runtime "run-p1" "th-p1" claim)
              decision-before (space/read-decision runtime "req-p1")
              run-before (llm/read-run runtime "run-p1")]
          (space/append-space-action! runtime request)
          (fence! runtime "chat-p1")
          (let [decision-after (space/read-decision runtime "req-p1")
                run-after (llm/read-run runtime "run-p1")]
            (is (= decision-before decision-after)
                "a same-request replay must not rewrite the decision (no replayed? flip, no content change)")
            (is (not (:idempotency/replayed? decision-after))
                "a same-request retry is not an idempotency replay")
            (is (= ["WT-p1"] (space/read-turns-by-space runtime "chat-p1"))
                "turn order must hold exactly one turn")
            (is (some? (space/read-context-bundle runtime "B-p1")))
            (is (some? (space/read-llm-run-request runtime "run-p1"))
                "the dispatch row must exist after replay (fact family complete)")
            (is (some? (space/read-object runtime "turn:WT-p1"))
                "catalog must hold the turn object after replay")
            (is (some? (space/read-object runtime "llm-turn-run:run-p1"))
                "catalog must hold the run object after replay")
            (is (= :succeeded (:status run-after))
                "the redelivered dispatch must not reset the terminal LLM run")
            (is (= (:claim/token run-before) (:claim/token run-after))))))

      (testing "P2 same idempotency key + same material with fresh ids: replayed decision, fresh ids never materialize"
        (let [ids-a {:space-id "chat-p2" :turn-id "WT-p2-A" :bundle-id "B-p2-A"
                     :run-id "run-p2-A" :thread-id "th-p2" :request-id "req-p2-A"
                     :prompt "Send once." :idempotency-key "idem-p2" :time-ms 80}
              _ (append-send-and-await-run! runtime ids-a)
              request-b (compose-request
                          {:space-id "chat-p2" :turn-id "WT-p2-B" :bundle-id "B-p2-B"
                           :run-id "run-p2-B" :thread-id "th-p2" :request-id "req-p2-B"
                           :prompt "Send once." :idempotency-key "idem-p2" :time-ms 81})
              decision-b (append-and-await-decision! runtime request-b)]
          (is (= :accepted (:decision/status decision-b)))
          (is (true? (:idempotency/replayed? decision-b)))
          (is (= "WT-p2-A" (:turn/id decision-b)) "replay must return the original facts")
          (is (= "run-p2-A" (:llm-turn-run/id decision-b)))
          (is (= ["WT-p2-A"] (space/read-turns-by-space runtime "chat-p2")))
          (is (nil? (space/read-turn runtime "WT-p2-B")) "the replay's fresh turn id must never exist")
          (is (nil? (space/read-context-bundle runtime "B-p2-B")))
          (is (nil? (space/read-llm-run-request runtime "run-p2-B")))
          (is (nil? (llm/read-run runtime "run-p2-B")))))

      (testing "P3 same idempotency key + DIFFERENT material in the same space: explicit conflict, never aliasing"
        (let [ids-a {:space-id "chat-p3" :turn-id "WT-p3-A" :bundle-id "B-p3-A"
                     :run-id "run-p3-A" :thread-id "th-p3" :request-id "req-p3-A"
                     :prompt "Original material." :idempotency-key "idem-p3" :time-ms 80}
              _ (append-send-and-await-run! runtime ids-a)
              request-b (compose-request
                          {:space-id "chat-p3" :turn-id "WT-p3-B" :bundle-id "B-p3-B"
                           :run-id "run-p3-B" :thread-id "th-p3" :request-id "req-p3-B"
                           :prompt "Different material." :idempotency-key "idem-p3" :time-ms 81})
              decision-b (append-and-await-decision! runtime request-b)]
          (is (= :rejected (:decision/status decision-b))
              "conflicting reuse of an idempotency key must be rejected, not aliased")
          (is (= :idempotency/conflict (:decision/reason decision-b)))
          (is (not (:idempotency/replayed? decision-b))
              "a conflict must not masquerade as a replay of the original facts")
          (is (= ["WT-p3-A"] (space/read-turns-by-space runtime "chat-p3"))
              "the original send's facts must be untouched")
          (is (nil? (space/read-turn runtime "WT-p3-B")))
          (is (nil? (llm/read-run runtime "run-p3-B")))))

      (testing "P4 same idempotency key in a DIFFERENT space: independent facts, never aliased to the first space"
        (let [ids-a {:space-id "chat-p4-a" :turn-id "WT-p4-A" :bundle-id "B-p4-A"
                     :run-id "run-p4-A" :thread-id "th-p4-a" :request-id "req-p4-A"
                     :prompt "Space A send." :idempotency-key "idem-p4" :time-ms 80}
              _ (append-send-and-await-run! runtime ids-a)
              request-b (compose-request
                          {:space-id "chat-p4-b" :turn-id "WT-p4-B" :bundle-id "B-p4-B"
                           :run-id "run-p4-B" :thread-id "th-p4-b" :request-id "req-p4-B"
                           :prompt "Space B send." :idempotency-key "idem-p4" :time-ms 81})
              decision-b (append-and-await-decision! runtime request-b)]
          (is (= :accepted (:decision/status decision-b)))
          (is (not (:idempotency/replayed? decision-b))
              "a different space must not be aliased to the first space's facts")
          (is (= "chat-p4-b" (:space/id decision-b))
              "the second decision must apply to ITS OWN space")
          (is (= "WT-p4-B" (:turn/id decision-b)))
          (is (= ["WT-p4-B"] (space/read-turns-by-space runtime "chat-p4-b"))
              "space B must hold its own turn")
          (is (= :pending
                 (:status (llm/await-run runtime "run-p4-B" #(= :pending (:status %)))))
              "space B's send must mint its own run")))

      (testing "P5 typed observation bridge: non-patch observations never become proposals; patch observations become proposals without a space decision or turn"
        (let [ids {:space-id "chat-p5" :turn-id "WT-p5" :bundle-id "B-p5"
                   :run-id "run-p5" :thread-id "th-p5" :request-id "req-p5"}
              _ (append-send-and-await-run! runtime ids)
              claim (claim! runtime "run-p5" "th-p5" "exec-p5" "tok-p5")
              turns-before (space/read-turns-by-space runtime "chat-p5")
              non-patch (llm/observation
                          "run-p5" "th-p5" :codex/item-completed 0
                          (merge claim
                                 {:observation-id "obs-p5-nonpatch"
                                  :space/id "chat-p5"
                                  :turn/id "WT-p5"
                                  :llm-item/id "item-p5"
                                  :content/text "ordinary token stream"
                                  :raw/json {}}))]
          (space/append-llm-observation! runtime non-patch)
          ;; settle the obs path with a LATER patch obs on the same run
          (space/append-llm-observation!
            runtime
            (patch-observation {:run-id "run-p5" :thread-id "th-p5"
                                :space-id "chat-p5" :turn-id "WT-p5"
                                :proposal-id "proposal-p5" :sequence 1
                                :claim claim}))
          (let [proposal (space/await-materialized
                           #(space/read-patch-proposal runtime "proposal-p5") some?)]
            (is (= :pending (:status proposal)) "the patch observation must mint a pending proposal")
            (is (= "run-p5" (:llm-turn-run/id proposal))))
          (fence! runtime "chat-p5")
          (is (nil? (space/read-patch-proposal runtime "obs-p5-nonpatch"))
              "a non-patch observation must NOT become a patch proposal")
          (is (nil? (space/read-decision runtime "obs-p5-nonpatch/space-patch-proposal"))
              "the bridge must not mint a space decision for a non-patch observation")
          (is (nil? (space/read-decision runtime "obs-proposal-p5/space-patch-proposal"))
              "observation ingest must not mint a space decision at all (spec op 4)")
          (is (= turns-before (space/read-turns-by-space runtime "chat-p5"))
              "observation ingest must not append turns to the space")))

      (testing "P6 patch resolution against nil/unknown proposal ids: accepted turn, zero phantom rows"
        (let [ids {:space-id "chat-p6" :turn-id "WT-p6" :bundle-id "B-p6"
                   :run-id "run-p6" :thread-id "th-p6" :request-id "req-p6"}
              _ (append-send-and-await-run! runtime ids)
              no-id-request (space/space-turn-request
                              :turn/patch-accept "chat-p6"
                              {:request-id "req-p6-no-id"
                               :time-ms 200
                               :payload {:turn/id "WT-p6-no-id"
                                         :prompt/text "Accept nothing."}})
              unknown-request (space/space-turn-request
                                :turn/patch-accept "chat-p6"
                                {:request-id "req-p6-unknown"
                                 :time-ms 201
                                 :payload {:turn/id "WT-p6-unknown"
                                           :patch-proposal/id "proposal-p6-never-created"
                                           :prompt/text "Accept a ghost."}})
              decision-no-id (append-and-await-decision! runtime no-id-request)
              decision-unknown (append-and-await-decision! runtime unknown-request)]
          (is (= :accepted (:decision/status decision-no-id))
              "a patch-accept without a proposal id stays an accepted plain turn")
          (is (= :accepted (:decision/status decision-unknown)))
          (fence! runtime "chat-p6")
          (is (nil? (space/read-patch-proposal runtime nil))
              "no phantom proposal row under the nil key")
          (is (nil? (space/read-patch-proposal runtime "proposal-p6-never-created"))
              "resolving an unknown proposal must not invent it")
          (is (some? (space/read-turn runtime "WT-p6-no-id")) "the turn itself is recorded")
          (is (some? (space/read-turn runtime "WT-p6-unknown")))))

      (testing "P7 first resolution wins: a later reject cannot overwrite an accept"
        (let [ids {:space-id "chat-p7" :turn-id "WT-p7" :bundle-id "B-p7"
                   :run-id "run-p7" :thread-id "th-p7" :request-id "req-p7"}
              _ (append-send-and-await-run! runtime ids)
              claim (claim! runtime "run-p7" "th-p7" "exec-p7" "tok-p7")]
          (space/append-llm-observation!
            runtime
            (patch-observation {:run-id "run-p7" :thread-id "th-p7"
                                :space-id "chat-p7" :turn-id "WT-p7"
                                :proposal-id "proposal-p7" :claim claim}))
          (space/await-materialized
            #(space/read-patch-proposal runtime "proposal-p7")
            #(= :pending (:status %)))
          (append-and-await-decision!
            runtime
            (space/space-turn-request
              :turn/patch-accept "chat-p7"
              {:request-id "req-p7-accept"
               :time-ms 210
               :payload {:turn/id "WT-p7-accept"
                         :patch-proposal/id "proposal-p7"
                         :prompt/text "Accept."}}))
          (space/await-materialized
            #(space/read-patch-proposal runtime "proposal-p7")
            #(= :accepted (:status %)))
          (append-and-await-decision!
            runtime
            (space/space-turn-request
              :turn/patch-reject "chat-p7"
              {:request-id "req-p7-reject"
               :time-ms 211
               :payload {:turn/id "WT-p7-reject"
                         :patch-proposal/id "proposal-p7"
                         :reason :changed-my-mind
                         :prompt/text "Reject."}}))
          (fence! runtime "chat-p7")
          (let [proposal (space/read-patch-proposal runtime "proposal-p7")]
            (is (= :accepted (:status proposal))
                "the first resolution must win; the later reject is a no-op on the proposal")
            (is (= "WT-p7-accept" (:resolution/turn-id proposal))
                "the resolution turn must remain the accepting turn")
            (is (nil? (:reason proposal))
                "the reject's reason must not leak onto the accepted proposal"))))

      (testing "P8 proposal redelivery after resolution: the resolution survives, never resets to :pending"
        (let [ids {:space-id "chat-p8" :turn-id "WT-p8" :bundle-id "B-p8"
                   :run-id "run-p8" :thread-id "th-p8" :request-id "req-p8"}
              _ (append-send-and-await-run! runtime ids)
              claim (claim! runtime "run-p8" "th-p8" "exec-p8" "tok-p8")
              obs (patch-observation {:run-id "run-p8" :thread-id "th-p8"
                                      :space-id "chat-p8" :turn-id "WT-p8"
                                      :proposal-id "proposal-p8" :claim claim})]
          (space/append-llm-observation! runtime obs)
          (space/await-materialized
            #(space/read-patch-proposal runtime "proposal-p8")
            #(= :pending (:status %)))
          (append-and-await-decision!
            runtime
            (space/space-turn-request
              :turn/patch-accept "chat-p8"
              {:request-id "req-p8-accept"
               :time-ms 220
               :payload {:turn/id "WT-p8-accept"
                         :patch-proposal/id "proposal-p8"
                         :prompt/text "Accept."}}))
          (space/await-materialized
            #(space/read-patch-proposal runtime "proposal-p8")
            #(= :accepted (:status %)))
          ;; redeliver the SAME observation (client retry / at-least-once)
          (space/append-llm-observation! runtime obs)
          (fence! runtime "chat-p8")
          (let [proposal (space/read-patch-proposal runtime "proposal-p8")]
            (is (= :accepted (:status proposal))
                "a redelivered proposal observation must not resurrect :pending")
            (is (= "WT-p8-accept" (:resolution/turn-id proposal))))))

      (testing "P9 approval-resolve for a never-requested approval: nothing invented, run unchanged"
        (let [ids {:space-id "chat-p9" :turn-id "WT-p9" :bundle-id "B-p9"
                   :run-id "run-p9" :thread-id "th-p9" :request-id "req-p9"}
              _ (append-send-and-await-run! runtime ids)
              control-request (space/space-turn-request
                                :turn/tool-approval-resolve "chat-p9"
                                {:request-id "req-p9-resolve"
                                 :time-ms 230
                                 :payload {:turn/id "WT-p9-resolve"
                                           :llm-turn-run/id "run-p9"
                                           :llm-thread/id "th-p9"
                                           :approval/id "approval-p9-never-requested"
                                           :native/json-rpc-request-id 999
                                           :decision :approved
                                           :prompt/text "Approve a ghost."}})
              decision (append-and-await-decision! runtime control-request)]
          (is (= :accepted (:decision/status decision))
              "the control turn is a recorded epistemic act")
          (llm/await-materialized
            #(some #{:approval/unknown}
                   (mapv :reason (:observation-errors (llm/read-run runtime "run-p9"))))
            some?)
          (let [run (llm/read-run runtime "run-p9")]
            (is (nil? (get-in run [:approvals-by-id "approval-p9-never-requested"]))
                "the LLM kernel must never invent an approval row")
            (is (nil? (llm/read-pending-approval runtime "approval-p9-never-requested")))
            (is (= :pending (:status run))
                "a ghost approval must not move the run"))))

      (testing "P10 concurrent space-only turns: every turn lands in the order, none dropped"
        (let [create-request (space/space-create-request
                               "chat-p10" {:request-id "req-p10-create" :time-ms 50})
              _ (append-and-await-decision! runtime create-request)
              turn-ids (mapv #(str "WT-p10-" %) (range 5))
              requests (map-indexed
                         (fn [i turn-id]
                           (space/space-turn-request
                             :turn/comment-create "chat-p10"
                             {:request-id (str "req-p10-" i)
                              :time-ms (+ 240 i)
                              :payload {:turn/id turn-id
                                        :overlay/id (str "overlay-p10-" i)
                                        :prompt/text (str "Comment " i)}}))
                         turn-ids)]
          ;; rapid appends, no awaits between — exercises pipelined records
          (doseq [request requests]
            (space/append-space-action! runtime request))
          (doseq [i (range 5)]
            (space/await-decision runtime (str "req-p10-" i)))
          (fence! runtime "chat-p10")
          (let [order (space/read-turns-by-space runtime "chat-p10")]
            (is (= (set turn-ids) (set order))
                "every concurrent turn must appear in the turn order (none dropped)")
            (is (= 5 (count order)) "no duplicates either")
            (is (= 5 (:turn-count (space/read-space runtime "chat-p10")))))))

      (testing "P11 duplicate request id with conflicting payload: committed decision untouched, conflict recorded, no second fact family"
        (let [ids {:space-id "chat-p11" :turn-id "WT-p11-A" :bundle-id "B-p11-A"
                   :run-id "run-p11-A" :thread-id "th-p11" :request-id "req-p11"
                   :prompt "Original payload." :idempotency-key "idem-p11-a" :time-ms 80}
              _ (append-send-and-await-run! runtime ids)
              decision-before (space/read-decision runtime "req-p11")
              impostor (compose-request
                         {:space-id "chat-p11" :turn-id "WT-p11-B" :bundle-id "B-p11-B"
                          :run-id "run-p11-B" :thread-id "th-p11" :request-id "req-p11"
                          :prompt "Impostor payload." :idempotency-key "idem-p11-b" :time-ms 81})]
          (space/append-space-action! runtime impostor)
          (fence! runtime "chat-p11")
          (is (= decision-before (space/read-decision runtime "req-p11"))
              "the committed decision must never be rewritten by an impostor request")
          (is (= ["WT-p11-A"] (space/read-turns-by-space runtime "chat-p11"))
              "the impostor must not mint a second turn")
          (is (nil? (space/read-turn runtime "WT-p11-B")))
          (is (nil? (llm/read-run runtime "run-p11-B")))
          (let [conflict (read-decision-row
                           runtime (str (space/decision-id-for-request-id "req-p11") "/conflict"))]
            (is (some? conflict) "the conflict must be auditable as its own decision row")
            (is (= :rejected (:decision/status conflict))))))

      (testing "P12 duplicate create on a space with turns: the canvas turn order survives"
        (let [ids {:space-id "chat-p12" :turn-id "WT-p12" :bundle-id "B-p12"
                   :run-id "run-p12" :thread-id "th-p12" :request-id "req-p12-send"}
              _ (append-send-and-await-run! runtime ids)
              _ (space/await-materialized
                  #(space/read-chat-canvas-projection runtime "chat-p12")
                  #(= ["WT-p12"] (:turn-order %)))
              duplicate-create (space/space-create-request
                                 "chat-p12" {:request-id "req-p12-create-again"
                                             :time-ms 300
                                             :title "Renamed"})
              decision (append-and-await-decision! runtime duplicate-create)]
          (is (= :accepted (:decision/status decision)))
          (fence! runtime "chat-p12")
          (let [canvas (space/read-chat-canvas-projection runtime "chat-p12")]
            (is (= ["WT-p12"] (:turn-order canvas))
                "a duplicate create must not wipe the canvas turn order")
            (is (= 1 (:turn-count canvas))))))

      (finally
        (space/close-space-runtime! runtime)))))
