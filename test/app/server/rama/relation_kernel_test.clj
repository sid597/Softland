;; Phase 5 (/rama skill) test suite for relation-kernel-module.
;;
;; Structure: TWO deftests = TWO IPC launches (Phase 6 T1 dropped the 3rd):
;;   dt1  relation-kernel-write-and-read-test — every assert / retract / reject /
;;        read path, all on DISJOINT keys, one launch, one cumulative-count
;;        barrier. Merged the former retract/reject deftest in here (T1): the
;;        blocks are self-contained on disjoint keys and the shared harness/counter
;;        extends unchanged, so a second launch bought nothing.
;;   dt2  relation-kernel-idempotency-and-partition-test — idempotency, convergence,
;;        partition provenance, same-batch read-your-writes. KEPT separate: its G1
;;        block drives the singleton topology's pause!/resume!, and a failure
;;        between pause and resume would starve every other block's 30s drain! in
;;        the same launch. Isolating the pause-wielding scenario bounds that blast
;;        radius (Phase 6 accepted this).
;;
;; Coverage: CONTRACT.md §11 gates 1-11 + the baton's carried checks (G1 same-batch
;; read-your-writes, G2 partition provenance, F1a whole-map / F1b kind-filter read
;; paths) + the IMPLICIT_SPEC entity×write matrix rows that Phase 6 found untested:
;;   T2 multi-asserter mixed retract (also §9 refusal 5, no-cascade)
;;   T3 retract-affirm (2nd retract, new key, on a retracted relation)
;;   T4 duplicate retract same key (accepted replay) + rejected same-key replay
;;   T5 retract with an unregistered kind
;;   T6 shape-rejection sweep (missing request-id / missing actor id / malformed from)
;;   T7 nil-containing R1 input
;;   T8 kinds-filter × include-retracted? survivorship on a retracted relation
;;   T9 status-log PState has no entry for a rejected relation (the R2-vacuous hole)
;;   T10 evidence/note round-trip + full-row agreement across endpoints and detail
;;
;; Phase 5 rule: WRITE tests + compile-check by loading this ns. DO NOT run them
;; (that is Phase 7). Assertions go through the foreign-client wrappers; only the
;; documented V1 readers touch PStates directly, per the CONTRACT §11 style gate.
;; Reads that must observe *physical* copies/counts (dedup, no-double-count,
;; partition provenance, a leaked status-log write) use V1 deliberately — R1 dedups
;; by relation-id and R2 returns empty history for a nil row, so both would MASK
;; the very bugs those checks exist to catch.
;;
;; T9 note: the module exposes no V1 wrapper for $$relation-status-log-by-relation
;; (and this phase may not edit the module), so its test reads the PState directly
;; via `status-log-for`, mirroring the module's own [(keypath k) ALL] idiom for a
;; subindexed map (relation_kernel.clj:812). IMPLICIT_SPEC V1 blesses direct PState
;; inspection in tests.
;;
;; Barrier: wait-for-microbatch-processed-count (deterministic; counts consumed
;; depot records incl. replays/rejections, so negative invariants are provable).
;; Every submit! appends a request with a present routing-key, so submit! == +1
;; processed, always -> the cumulative count can never desync.

(ns app.server.rama.relation-kernel-test
  (:require [app.server.rama.relation-kernel :as rk]
            [com.rpl.rama :refer [foreign-select]]
            [com.rpl.rama.path :refer [keypath ALL]]
            [com.rpl.rama.test :as rtest]
            [clojure.test :refer [deftest is testing]]))

(def ^:private topo-name "relation-kernel-topology")

(defn with-relation-runtime
  "One InProcessCluster + module launch, randomized task count to exercise
   partition alignment (testing.md). try/finally cleanup, mirroring the compute
   and transcript kernel test harnesses."
  [f]
  (let [runtime (rk/start-relation-runtime! {:tasks (rand-nth [2 4 8]) :threads 2})]
    (try
      (f runtime)
      (finally
        (rk/close-relation-runtime! runtime)))))

(defn make-harness
  "submit!/drain!/pause!/resume! bound to one runtime + a private append counter.
   drain! waits until the topology has processed every request appended so far
   (no-op replays and rejections included, since the count tracks depot records
   consumed, not writes). This is what makes negative invariants provable: a
   replayed or same-batch second request is guaranteed fully processed before the
   read."
  [runtime]
  (let [ipc         (:ipc runtime)
        module-name (:module-name runtime)
        !appended   (atom 0)]
    {:submit! (fn [req]
                (rk/append-relation-request! runtime req)
                (swap! !appended inc)
                req)
     :drain!  (fn []
                (rtest/wait-for-microbatch-processed-count
                  ipc module-name topo-name @!appended 30000))
     :pause!  (fn [] (rtest/pause-microbatch-topology! ipc module-name topo-name))
     :resume! (fn [] (rtest/resume-microbatch-topology! ipc module-name topo-name))}))

;; ── tiny builders / extractors ───────────────────────────────────────────────
(defn tref     [kind id]           (rk/->target-ref kind id))
(defn rows-for [result target-key] (vec (get result target-key)))
(defn id-set   [rows]              (set (map :relation-id rows)))
(defn statuses [history]           (mapv :relation-status history))

;; T9: direct V1 read of the subindexed status-log PState. Returns the [order-key
;; row] entries under a relation-id (empty seq when the relation-id never got a
;; status event). Absent-key ALL-navigation returns empty, exactly as
;; read-target-index relies on for $$relations-by-target (same schema shape).
(defn status-log-for [runtime relation-id]
  (foreign-select [(keypath relation-id) ALL] (:status-log-by-relation runtime)))

;; ═════════════════════════════════════════════════════════════════════════════
;;  1) Write + read: asserts, retracts, rejections, and all query edges.
;;     Gates 1,4,5,6,7,8,9,10 / F1a / F1b + T2,T3,T4,T5,T6,T7,T8,T9,T10 + edges.
;;     One launch; every block is self-contained on disjoint keys.
;; ═════════════════════════════════════════════════════════════════════════════
(deftest relation-kernel-write-and-read-test
  (with-relation-runtime
    (fn [runtime]
      (let [{:keys [submit! drain!]} (make-harness runtime)]

        (testing "Gate 1 — assert A based-on B, dual-read from both (different partition keys)"
          (let [a      (tref :doc-file "oc:doc:g1-a.md")   ; key "g1-a.md"
                b      (tref :git-commit "g1-sha-b")        ; key "g1-sha-b"
                a-key  (:target-key a)
                b-key  (:target-key b)
                rel-id (rk/relation-id-for :based-on a b "sid")
                req    (rk/assert-request
                         {:kind :based-on :from a :to b
                          :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000
                          :request-id "g1-req" :idempotency-key "g1-idem"})]
            (is (not= a-key b-key) "endpoints occupy different partition keys")
            (submit! req) (drain!)
            (let [from-side (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)
                  to-side   (rows-for (rk/read-relations-for-targets runtime [b-key]) b-key)
                  detail    (rk/read-relation-detail runtime rel-id)]
              (is (= 1 (count from-side)))
              (is (= 1 (count to-side)))
              (is (= rel-id (:relation-id (first from-side))))
              (is (= rel-id (:relation-id (first to-side))) "same edge readable from both endpoints")
              (is (= :based-on (:relation-kind (first from-side))))
              (is (= "sid" (:asserter-actor-id (first from-side))))
              (is (= :asserted (:relation-status (first from-side))))
              (is (= :asserted (:relation-status (:row detail))))
              (is (= [:asserted] (statuses (:history detail)))))))

        (testing "Gate 7 / T10 — dangling accepted; evidence/note round-trip; endpoints and detail agree on the full row"
          ;; No object-container material is imported in this module test, so both
          ;; endpoints address nothing. CONTRACT trap 3 / §8: accept anyway.
          ;; T10: carry evidence + note through and assert the from-copy, to-copy,
          ;; and authoritative row are byte-identical (W1 row-agreement).
          (let [a      (tref :doc-file "oc:doc:g7-ghost.md")
                b      (tref :git-commit "g7-no-such-sha")
                a-key  (:target-key a) b-key (:target-key b)
                rel-id (rk/relation-id-for :references a b "sid")
                req    (rk/assert-request
                         {:kind :references :from a :to b
                          :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "g7-req" :idempotency-key "g7-idem"
                          :evidence-source-id "oc:doc:g7-evidence.md"
                          :evidence-anchor-id "g7-anchor-3"
                          :note "asserted because it cites §3"})]
            (submit! req) (drain!)
            (let [from-side (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)
                  to-side   (rows-for (rk/read-relations-for-targets runtime [b-key]) b-key)
                  row       (:row (rk/read-relation-detail runtime rel-id))]
              (is (= :asserted (:relation-status row)) "dangling target accepted")
              (is (= 1 (count from-side)))
              (is (= 1 (count to-side)))
              ;; evidence/note round-trip — the "why does this edge exist?" surface.
              (is (= "oc:doc:g7-evidence.md" (:evidence-source-id row)))
              (is (= "g7-anchor-3" (:evidence-anchor-id row)))
              (is (= "asserted because it cites §3" (:note row)))
              ;; W1 row-agreement: the same *row record is stored at by-id and at
              ;; both target copies, so all three must be equal on every field
              ;; (status, asserter, evidence, note, timestamps, event id, request id).
              (is (= (first from-side) (first to-side) row)
                  "endpoints and by-id agree on the full row"))))

        (testing "Gate 8 — unary :dead-end inherits from-key; one target-key, readable from-side"
          (let [a      (tref :doc-file "oc:doc:g8-a.md")
                a-key  (:target-key a)
                to     (rk/unary-to-ref a)               ; :none, target-key = a-key
                rel-id (rk/relation-id-for :dead-end a to "sid")
                req    (rk/assert-request
                         {:kind :dead-end :from a :to to
                          :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "g8-req" :idempotency-key "g8-idem"})]
            (is (= a-key (:target-key to)) "unary to inherits from target-key (no nil-key hotspot)")
            (submit! req) (drain!)
            (let [result-rows (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)
                  row         (first result-rows)]
              ;; Public R1 dedups by relation-id: the o:/i: copies collapse to one row.
              (is (= 1 (count result-rows)))
              (is (= rel-id (:relation-id row)))
              (is (= :none (:target-kind (:to row))))
              (is (nil? (:target-id (:to row))))
              (is (= a-key (:target-key (:to row))))
              ;; Physically both o: and i: copies land under the single inherited key;
              ;; there is no separate to-key to spray onto.
              (is (= 2 (count (rk/read-target-index runtime a-key)))
                  "both endpoint copies colocate on the from-key"))))

        (testing "Gate 9 — same (kind,from,to) by sid vs llm: two ids, both grouped by the query"
          (let [a       (tref :doc-file "oc:doc:g9-a.md")
                b       (tref :git-commit "g9-sha")
                a-key   (:target-key a) b-key (:target-key b)
                id-sid  (rk/relation-id-for :based-on a b "sid")
                id-llm  (rk/relation-id-for :based-on a b "llm:opus-4-8/run-x")
                req-sid (rk/assert-request
                          {:kind :based-on :from a :to b
                           :asserter-actor-id "sid" :asserter-type :human
                           :asserted-at-ms 1000 :request-id "g9-sid-req" :idempotency-key "g9-sid"})
                req-llm (rk/assert-request
                          {:kind :based-on :from a :to b
                           :asserter-actor-id "llm:opus-4-8/run-x" :asserter-type :llm
                           :asserted-at-ms 1100 :request-id "g9-llm-req" :idempotency-key "g9-llm"})]
            (is (not= id-sid id-llm) "asserter is part of identity")
            (submit! req-sid) (submit! req-llm) (drain!)
            (let [from-side (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)
                  to-side   (rows-for (rk/read-relations-for-targets runtime [b-key]) b-key)]
              (is (= 2 (count from-side)))
              (is (= #{id-sid id-llm} (id-set from-side)) "both asserters grouped under the target")
              (is (= #{id-sid id-llm} (id-set to-side)))
              (is (= "sid" (:asserter-actor-id (:row (rk/read-relation-detail runtime id-sid)))))
              (is (= "llm:opus-4-8/run-x" (:asserter-actor-id (:row (rk/read-relation-detail runtime id-llm))))))))

        (testing "T2 — retract one of two co-resident asserters (mixed state; §9 refusal 5 no-cascade)"
          ;; Builds on Gate 9's sid + llm rows. Retract only the llm identity.
          (let [a       (tref :doc-file "oc:doc:g9-a.md")
                b       (tref :git-commit "g9-sha")
                a-key   (:target-key a)
                id-sid  (rk/relation-id-for :based-on a b "sid")
                id-llm  (rk/relation-id-for :based-on a b "llm:opus-4-8/run-x")
                llm-retract (rk/retract-request
                              {:kind :based-on :from a :to b
                               :asserter-actor-id "llm:opus-4-8/run-x" :asserter-type :llm
                               :asserted-at-ms 1200 :request-id "g9-llm-retract" :idempotency-key "g9-llm-r"
                               :actor {:actor/id "llm:opus-4-8/run-x" :actor/type :llm}})]
            (submit! llm-retract) (drain!)
            ;; default R1: only the still-asserted sid identity survives.
            (let [from-side (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)]
              (is (= #{id-sid} (id-set from-side)) "retracting llm leaves only sid by default")
              (is (= [:asserted] (mapv :relation-status from-side))))
            ;; include-retracted?: both identities, each with its own status.
            (let [both  (rows-for (rk/read-relations-for-targets runtime [a-key] nil true) a-key)
                  by-id (into {} (map (juxt :relation-id :relation-status)) both)]
              (is (= #{id-sid id-llm} (set (keys by-id))))
              (is (= :asserted  (get by-id id-sid)))
              (is (= :retracted (get by-id id-llm))))
            ;; §9 refusal 5: sid's co-resident row is untouched by llm's retraction.
            (let [sid-detail (rk/read-relation-detail runtime id-sid)]
              (is (= :asserted (:relation-status (:row sid-detail))))
              (is (= [:asserted] (statuses (:history sid-detail))) "sid history unchanged (no cascade)"))
            (is (= [:asserted :retracted] (statuses (:history (rk/read-relation-detail runtime id-llm)))))))

        (testing "Gate 10 / F1a / F1b — multi-kind target: no filter returns all kinds, kind filter isolates"
          (let [x      (tref :doc-file "oc:doc:g10-x.md")
                y      (tref :git-commit "g10-y")
                z      (tref :git-commit "g10-z")
                x-key  (:target-key x)
                id-bo  (rk/relation-id-for :based-on x y "sid")
                id-rf  (rk/relation-id-for :references x z "sid")
                req-bo (rk/assert-request
                         {:kind :based-on :from x :to y
                          :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "g10-bo-req" :idempotency-key "g10-bo"})
                req-rf (rk/assert-request
                         {:kind :references :from x :to z
                          :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1100 :request-id "g10-rf-req" :idempotency-key "g10-rf"})]
            (submit! req-bo) (submit! req-rf) (drain!)
            ;; F1a: no kind filter -> whole-map read path returns BOTH kinds.
            (let [all-rows (rows-for (rk/read-relations-for-targets runtime [x-key]) x-key)]
              (is (= 2 (count all-rows)))
              (is (= #{id-bo id-rf} (id-set all-rows)))
              (is (= #{:based-on :references} (set (map :relation-kind all-rows)))))
            ;; F1b / Gate 10: kind filter -> per-prefix seek returns only that kind, no leak.
            (let [bo-only (rows-for (rk/read-relations-for-targets runtime [x-key] [:based-on] false) x-key)
                  rf-only (rows-for (rk/read-relations-for-targets runtime [x-key] [:references] false) x-key)]
              (is (= [id-bo] (mapv :relation-id bo-only)))
              (is (= [:based-on] (mapv :relation-kind bo-only)) "kind filter does not leak other kinds")
              (is (= [id-rf] (mapv :relation-id rf-only)))
              (is (= [:references] (mapv :relation-kind rf-only))))))

        (testing "R1/R2 edges — empty / unknown / duplicate / nil input, missing/malformed id"
          ;; Empty target list short-circuits to {} (no query roundtrip).
          (is (= {} (rk/read-relations-for-targets runtime [])))
          ;; Unknown target key returns an empty vector for that key, not an error.
          (is (= [] (rows-for (rk/read-relations-for-targets runtime ["no-such-target-key"]) "no-such-target-key")))
          ;; Duplicate target keys must not duplicate rows (distinct-present-target-keys).
          ;; g1-a.md carries exactly one (still-asserted) relation from Gate 1.
          (let [a-key (:target-key (tref :doc-file "oc:doc:g1-a.md"))]
            (is (= 1 (count (rows-for (rk/read-relations-for-targets runtime [a-key a-key]) a-key))))
            ;; T7: nil target keys are dropped like blank/absent keys — never a global scan.
            (is (= {} (rk/read-relations-for-targets runtime [nil])) "all-nil input collapses to {}")
            (is (= 1 (count (rows-for (rk/read-relations-for-targets runtime [nil a-key]) a-key)))
                "a nil beside a real key behaves as the real key alone"))
          ;; Missing / malformed relation id -> empty detail, no scan.
          (is (= {:row nil :history []} (rk/read-relation-detail runtime "rel:does-not-exist")))
          (is (= {:row nil :history []} (rk/read-relation-detail runtime "not-a-relation-id"))))

        (testing "Gate 4 — retract: :retracted visible from A, B, and detail; ordered history"
          (let [a           (tref :doc-file "oc:doc:g4-a.md")
                b           (tref :git-commit "g4-sha")
                a-key       (:target-key a) b-key (:target-key b)
                rel-id      (rk/relation-id-for :based-on a b "sid")
                base        {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human}
                assert-req  (rk/assert-request (merge base {:asserted-at-ms 1000 :request-id "g4-a" :idempotency-key "g4-ka"}))
                retract-req (rk/retract-request
                              (merge base {:asserted-at-ms 2000 :request-id "g4-r" :idempotency-key "g4-kr"
                                           :actor {:actor/id "sid" :actor/type :human}}))]
            (submit! assert-req) (drain!)
            (is (= 1 (count (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key))) "asserted -> visible by default")
            (submit! retract-req) (drain!)
            ;; Hidden by default from both endpoints.
            (is (= [] (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)))
            (is (= [] (rows-for (rk/read-relations-for-targets runtime [b-key]) b-key)))
            ;; Visible as :retracted from both endpoints with include-retracted?.
            (let [a-inc (rows-for (rk/read-relations-for-targets runtime [a-key] nil true) a-key)
                  b-inc (rows-for (rk/read-relations-for-targets runtime [b-key] nil true) b-key)]
              (is (= [:retracted] (mapv :relation-status a-inc)))
              (is (= [:retracted] (mapv :relation-status b-inc)) "endpoints never disagree on status"))
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :retracted (:relation-status (:row detail))))
              (is (= [:asserted :retracted] (statuses (:history detail))) "history ordered assert then retract"))))

        (testing "T8 — kinds-filter × survivorship on the retracted Gate-4 relation"
          ;; Exercises relation-read-ranges' kinds-branch with both survivorship
          ;; outcomes: asserted-count 0 ⇒ no seek ⇒ [] ; total-count 1 ⇒ the row.
          (let [a-key (:target-key (tref :doc-file "oc:doc:g4-a.md"))]
            (is (= [] (rows-for (rk/read-relations-for-targets runtime [a-key] [:based-on] false) a-key))
                "kind filter, exclude-retracted: asserted-count 0 ⇒ nothing")
            (is (= [:retracted]
                   (mapv :relation-status
                         (rows-for (rk/read-relations-for-targets runtime [a-key] [:based-on] true) a-key)))
                "kind filter, include-retracted: total-count 1 ⇒ the retracted row")))

        (testing "T3 — retract-affirm: 2nd retract (new key) on a retracted relation, no duplication"
          (let [a      (tref :doc-file "oc:doc:g4-a.md")
                b      (tref :git-commit "g4-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :based-on a b "sid")
                base   {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human
                        :actor {:actor/id "sid" :actor/type :human}}
                raffirm (rk/retract-request (merge base {:asserted-at-ms 3000 :request-id "g4-raffirm" :idempotency-key "g4-kr2"}))]
            (submit! raffirm) (drain!)
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :retracted (:relation-status (:row detail))) "still retracted")
              (is (= [:asserted :retracted :retracted] (statuses (:history detail)))
                  "no-op retraction recorded as an ordered status event (IMPLICIT_SPEC W2)"))
            (is (= [] (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)) "still excluded by default")
            (is (= 1 (count (rk/read-target-index runtime a-key))) "still one physical copy")
            (let [o-desc (get (rk/read-target-descriptors runtime a-key)
                              (rk/target-descriptor-key :outgoing :based-on))]
              (is (= 0 (:asserted-count o-desc)) "descriptor counts unchanged by retract-affirm")
              (is (= 1 (:total-count o-desc))))))

        (testing "Gate 5 — retraction rights: wrong actor rejected, status unchanged"
          (let [a           (tref :doc-file "oc:doc:g5-a.md")
                b           (tref :git-commit "g5-sha")
                a-key       (:target-key a)
                rel-id      (rk/relation-id-for :based-on a b "sid")
                base        {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human}
                assert-req  (rk/assert-request (merge base {:asserted-at-ms 1000 :request-id "g5-a" :idempotency-key "g5-ka"}))
                bad-retract (rk/retract-request
                              (merge base {:asserted-at-ms 2000 :request-id "g5-bad" :idempotency-key "g5-kbad"
                                           :actor {:actor/id "mallory" :actor/type :human}}))]
            (submit! assert-req) (drain!)
            (submit! bad-retract) (drain!)
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :asserted (:relation-status (:row detail))) "status unchanged by forbidden retract")
              (is (= 1 (count (:history detail))) "no status event added"))
            (is (= 1 (count (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key))) "still visible by default")
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id "g5-bad"))]
              (is (= :rejected (:status decision)))
              (is (= :relation/retraction-forbidden (:reason decision))))))

        (testing "T4a — duplicate retract (same idem key) replays: no new status event"
          (let [a      (tref :doc-file "oc:doc:t4-a.md")
                b      (tref :git-commit "t4-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :based-on a b "sid")
                base   {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human
                        :actor {:actor/id "sid" :actor/type :human}}
                assert-req  (rk/assert-request  (merge base {:asserted-at-ms 1000 :request-id "t4-a" :idempotency-key "t4-ka"}))
                retract-req (rk/retract-request (merge base {:asserted-at-ms 2000 :request-id "t4-r" :idempotency-key "t4-kr"}))]
            (submit! assert-req) (drain!)
            ;; identical retract appended twice; the barrier counts BOTH consumed,
            ;; so history staying at 2 proves the replay wrote nothing.
            (submit! retract-req) (submit! retract-req) (drain!)
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :retracted (:relation-status (:row detail))))
              (is (= [:asserted :retracted] (statuses (:history detail))) "duplicate retract writes no second event"))
            (is (= 1 (count (rk/read-target-index runtime a-key))) "no duplicate copy")
            (is (= :accepted (:status (rk/read-decision-by-idempotency runtime rel-id "t4-kr")))
                "the first retract decision is replayed")))

        (testing "T4b — resubmit Gate 5's rejected retract (same key): rejected decision replayed, nothing changes"
          ;; Depends on Gate 5 having run earlier in THIS launch (id-g5 asserted,
          ;; a rejected decision journaled under (id-g5, g5-kbad)).
          (let [a      (tref :doc-file "oc:doc:g5-a.md")
                b      (tref :git-commit "g5-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :based-on a b "sid")
                base   {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human}
                bad-retract (rk/retract-request
                              (merge base {:asserted-at-ms 2000 :request-id "g5-bad" :idempotency-key "g5-kbad"
                                           :actor {:actor/id "mallory" :actor/type :human}}))]
            (submit! bad-retract) (drain!)
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :asserted (:relation-status (:row detail))) "replay does not change truth")
              (is (= 1 (count (:history detail))) "still one status event"))
            (is (= 1 (count (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key))) "still visible")
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id "g5-bad"))]
              (is (= :rejected (:status decision)) "the same rejected decision is replayed")
              (is (= :relation/retraction-forbidden (:reason decision))))))

        (testing "T5 — retract with an unregistered kind rejected; truth unchanged"
          (let [a      (tref :doc-file "oc:doc:t5-a.md")
                b      (tref :git-commit "t5-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :bogus-kind a b "sid")
                req    (rk/retract-request
                         {:kind :bogus-kind :from a :to b :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "t5-req" :idempotency-key "t5-idem"
                          :actor {:actor/id "sid" :actor/type :human}})]
            (submit! req) (drain!)
            (is (= {:row nil :history []} (rk/read-relation-detail runtime rel-id)) "no relation truth")
            (is (nil? (rk/read-relation-row runtime rel-id)) "no authoritative row")
            (is (empty? (rk/read-target-index runtime a-key)) "no target copy")
            (is (empty? (status-log-for runtime rel-id)) "no status-log entry")
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id "t5-req"))]
              (is (= :rejected (:status decision)))
              (is (= :relation/kind-unregistered (:reason decision))))))

        (testing "Gate 6 / T9 — unregistered kind (assert) rejected; no non-audit materialization incl. status log"
          (let [a      (tref :doc-file "oc:doc:g6-a.md")
                b      (tref :git-commit "g6-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :bogus-kind a b "sid")
                req    (rk/assert-request
                         {:kind :bogus-kind :from a :to b :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "g6-req" :idempotency-key "g6-idem"})]
            (submit! req) (drain!)
            (is (= {:row nil :history []} (rk/read-relation-detail runtime rel-id)) "no relation truth")
            (is (= [] (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)))
            (is (nil? (rk/read-relation-row runtime rel-id)) "no authoritative row")
            (is (empty? (rk/read-target-index runtime a-key)) "no target copy")
            (is (nil? (rk/read-target-descriptors runtime a-key)) "no descriptor")
            ;; T9: R2's empty history is vacuous for a nil row; assert the status-log
            ;; PState itself has no stray entry under the rejected relation-id.
            (is (empty? (status-log-for runtime rel-id)) "no status-log entry for a rejected relation")
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id "g6-req"))]
              (is (= :rejected (:status decision)) "but the rejected decision IS durable")
              (is (= :relation/kind-unregistered (:reason decision))))))

        (testing "Edge / T9 — missing idempotency key rejected under the sentinel journal; no status log"
          (let [a      (tref :doc-file "oc:doc:e1-a.md")
                b      (tref :git-commit "e1-sha")
                rel-id (rk/relation-id-for :based-on a b "sid")
                req    (rk/assert-request
                         {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "e1-req" :idempotency-key ""})]
            (submit! req) (drain!)
            (is (= {:row nil :history []} (rk/read-relation-detail runtime rel-id)))
            (is (empty? (status-log-for runtime rel-id)) "no status-log entry")
            (let [decision (rk/read-decision-by-idempotency runtime rel-id "")]
              (is (= :rejected (:status decision)) "rejected decision journaled under the sentinel key")
              (is (= :idempotency/key-missing (:reason decision))))))

        (testing "Edge / T9 — retract of a non-existent relation rejected as :relation/absent; no status log"
          (let [a      (tref :doc-file "oc:doc:e2-a.md")
                b      (tref :git-commit "e2-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :based-on a b "sid")
                req    (rk/retract-request
                         {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "e2-req" :idempotency-key "e2-idem"
                          :actor {:actor/id "sid" :actor/type :human}})]
            (submit! req) (drain!)
            (is (= {:row nil :history []} (rk/read-relation-detail runtime rel-id)))
            (is (= [] (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key)))
            (is (empty? (status-log-for runtime rel-id)) "no status-log entry")
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id "e2-req"))]
              (is (= :rejected (:status decision)))
              (is (= :relation/absent (:reason decision))))))

        (testing "Edge / T9 — non-unary :none target rejected as malformed; no status log"
          (let [a      (tref :doc-file "oc:doc:e3-a.md")
                bad-to (rk/->target-ref :none nil)       ; :none WITHOUT inheriting a from-key -> blank key
                rel-id (rk/relation-id-for :based-on a bad-to "sid")
                req    (rk/assert-request
                         {:kind :based-on :from a :to bad-to :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "e3-req" :idempotency-key "e3-idem"})]
            (submit! req) (drain!)
            (is (= {:row nil :history []} (rk/read-relation-detail runtime rel-id)))
            (is (empty? (status-log-for runtime rel-id)) "no status-log entry")
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id "e3-req"))]
              (is (= :rejected (:status decision)))
              (is (= :relation/to-malformed (:reason decision))))))

        (testing "T6 — shape-rejection sweep: missing request-id / missing actor id / malformed from"
          ;; The reason is (:type (first errors)), decided by request-shape-errors'
          ;; cond-> order, so each case isolates exactly one malformation and keeps
          ;; everything earlier in that order valid.
          (doseq [{:keys [label req rel-id a-key request-id reason]}
                  [(let [a (tref :doc-file "oc:doc:t6-rid-a.md")
                         b (tref :git-commit "t6-rid-sha")]
                     {:label "missing request-id"
                      :req (rk/assert-request {:kind :based-on :from a :to b
                                               :asserter-actor-id "sid" :asserter-type :human
                                               :asserted-at-ms 1000 :request-id "" :idempotency-key "t6-rid-idem"})
                      :rel-id (rk/relation-id-for :based-on a b "sid")
                      :a-key (:target-key a) :request-id "" :reason :request/id-missing})
                   (let [a (tref :doc-file "oc:doc:t6-act-a.md")
                         b (tref :git-commit "t6-act-sha")]
                     {:label "missing actor id"
                      ;; real asserter (so relation-id is present) but a blank envelope actor id.
                      :req (rk/assert-request {:kind :based-on :from a :to b
                                               :asserter-actor-id "sid" :asserter-type :human
                                               :actor {:actor/id "" :actor/type :human}
                                               :asserted-at-ms 1000 :request-id "t6-act-req" :idempotency-key "t6-act-idem"})
                      :rel-id (rk/relation-id-for :based-on a b "sid")
                      :a-key (:target-key a) :request-id "t6-act-req" :reason :actor/id-missing})
                   (let [a (rk/->target-ref :none nil)     ; :none on the FROM side = malformed (IMPLICIT_SPEC W1)
                         b (tref :git-commit "t6-from-sha")]
                     {:label "malformed from"
                      :req (rk/assert-request {:kind :based-on :from a :to b
                                               :asserter-actor-id "sid" :asserter-type :human
                                               :asserted-at-ms 1000 :request-id "t6-from-req" :idempotency-key "t6-from-idem"})
                      :rel-id (rk/relation-id-for :based-on a b "sid")
                      :a-key (:target-key b) :request-id "t6-from-req" :reason :relation/from-malformed})]]
            (submit! req) (drain!)
            (is (= {:row nil :history []} (rk/read-relation-detail runtime rel-id)) label)
            (is (nil? (rk/read-relation-row runtime rel-id)) label)
            (is (empty? (rk/read-target-index runtime a-key)) label)
            (is (nil? (rk/read-target-descriptors runtime a-key)) label)
            (is (empty? (status-log-for runtime rel-id)) label)
            (let [decision (rk/read-decision-by-id runtime (rk/decision-id-for rel-id request-id))]
              (is (= :rejected (:status decision)) label)
              (is (= reason (:reason decision)) label))))

        (testing "Lifecycle — retracted -> reasserted returns to :asserted on one identity"
          (let [a      (tref :doc-file "oc:doc:e5-a.md")
                b      (tref :git-commit "e5-sha")
                a-key  (:target-key a)
                rel-id (rk/relation-id-for :based-on a b "sid")
                base   {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human
                        :actor {:actor/id "sid" :actor/type :human}}
                a1     (rk/assert-request  (merge base {:asserted-at-ms 1000 :request-id "e5-a1" :idempotency-key "e5-k1"}))
                r1     (rk/retract-request (merge base {:asserted-at-ms 2000 :request-id "e5-r1" :idempotency-key "e5-k2"}))
                a2     (rk/assert-request  (merge base {:asserted-at-ms 3000 :request-id "e5-a2" :idempotency-key "e5-k3"}))]
            (submit! a1) (drain!)
            (submit! r1) (drain!)
            (submit! a2) (drain!)
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :asserted (:relation-status (:row detail))))
              (is (= [:asserted :retracted :asserted] (statuses (:history detail)))))
            (is (= 1 (count (rows-for (rk/read-relations-for-targets runtime [a-key]) a-key))) "visible again by default")
            (is (= 1 (count (rk/read-target-index runtime a-key))) "single copy across the whole lifecycle")
            (let [o-desc (get (rk/read-target-descriptors runtime a-key)
                              (rk/target-descriptor-key :outgoing :based-on))]
              (is (= 1 (:asserted-count o-desc)) "asserted-count returns to 1 after retract→assert")
              (is (= 1 (:total-count o-desc))))))))))

;; ═════════════════════════════════════════════════════════════════════════════
;;  2) Idempotency, convergence, partition provenance, same-batch read-your-writes
;;     (gates 2, 3, 11, G2, G1). SEPARATE launch: G1 drives pause!/resume!, whose
;;     failure would starve every other block's drain! in the same launch.
;; ═════════════════════════════════════════════════════════════════════════════
(deftest relation-kernel-idempotency-and-partition-test
  (with-relation-runtime
    (fn [runtime]
      (let [{:keys [submit! drain! pause! resume!]} (make-harness runtime)]

        (testing "Gate 2 — same idempotency key: one relation, one event, replayed decision"
          (let [a      (tref :doc-file "oc:doc:g2-a.md")
                b      (tref :git-commit "g2-sha")
                a-key  (:target-key a) b-key (:target-key b)
                rel-id (rk/relation-id-for :based-on a b "sid")
                req    (rk/assert-request
                         {:kind :based-on :from a :to b
                          :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "g2-req" :idempotency-key "g2-idem"})]
            (submit! req) (submit! req) (drain!)  ; identical request appended twice
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (= :asserted (:relation-status (:row detail))))
              (is (= 1 (count (:history detail))) "replay writes no second event"))
            (is (= 1 (count (rk/read-target-index runtime a-key))) "no duplicate from-side copy")
            (is (= 1 (count (rk/read-target-index runtime b-key))) "no duplicate to-side copy")
            (is (= :accepted (:status (rk/read-decision-by-idempotency runtime rel-id "g2-idem"))))))

        (testing "Gate 3 — convergent re-import: same logical assert, different idem keys, one row"
          (let [a        (tref :doc-file "oc:doc:g3-a.md")
                b        (tref :git-commit "g3-sha")
                a-key    (:target-key a) b-key (:target-key b)
                rel-id   (rk/relation-id-for :based-on a b "sid")
                base     {:kind :based-on :from a :to b
                          :asserter-actor-id "sid" :asserter-type :human}
                req-1    (rk/assert-request (merge base {:asserted-at-ms 1000 :request-id "g3-req-1" :idempotency-key "g3-k1"}))
                req-2    (rk/assert-request (merge base {:asserted-at-ms 2000 :request-id "g3-req-2" :idempotency-key "g3-k2"}))]
            (is (= rel-id (rk/relation-id-of-request req-2)) "different idem keys converge on one relation-id")
            (submit! req-1) (drain!)
            (submit! req-2) (drain!)
            (is (= 1 (count (rk/read-target-index runtime a-key))) "no duplicate copy across re-import")
            (is (= 1 (count (rk/read-target-index runtime b-key))))
            (let [o-desc (get (rk/read-target-descriptors runtime a-key)
                              (rk/target-descriptor-key :outgoing :based-on))]
              (is (= 1 (:asserted-count o-desc)) "re-import does not double-count")
              (is (= 1 (:total-count o-desc))))
            (is (= 2 (count (:history (rk/read-relation-detail runtime rel-id))))
                "reassertion recorded as status history, but on one identity")))

        (testing "Gate 11 — same idempotency key across two DIFFERENT relations: both succeed"
          (let [a1    (tref :doc-file "oc:doc:g11-a.md")  b1 (tref :git-commit "g11-b")
                a2    (tref :doc-file "oc:doc:g11-c.md")  b2 (tref :git-commit "g11-d")
                id-1  (rk/relation-id-for :based-on a1 b1 "sid")
                id-2  (rk/relation-id-for :based-on a2 b2 "sid")
                req-1 (rk/assert-request
                        {:kind :based-on :from a1 :to b1 :asserter-actor-id "sid" :asserter-type :human
                         :asserted-at-ms 1000 :request-id "g11-req-1" :idempotency-key "SHARED-KEY"})
                req-2 (rk/assert-request
                        {:kind :based-on :from a2 :to b2 :asserter-actor-id "sid" :asserter-type :human
                         :asserted-at-ms 1000 :request-id "g11-req-2" :idempotency-key "SHARED-KEY"})]
            (is (not= id-1 id-2))
            (submit! req-1) (submit! req-2) (drain!)
            ;; Relation-scoped journal (F2 ruling): the shared key does NOT collapse the two.
            (is (= :asserted (:relation-status (:row (rk/read-relation-detail runtime id-1)))))
            (is (= :asserted (:relation-status (:row (rk/read-relation-detail runtime id-2)))))
            (let [d-1 (rk/read-decision-by-idempotency runtime id-1 "SHARED-KEY")
                  d-2 (rk/read-decision-by-idempotency runtime id-2 "SHARED-KEY")]
              (is (= :accepted (:status d-1)))
              (is (= :accepted (:status d-2)))
              (is (not= (:decision-id d-1) (:decision-id d-2))
                  "two distinct journal entries, colocated on their own relation tasks"))))

        (testing "G2 — partition provenance: decision/event by-id readers (custom key-partitioner) hit"
          (let [a      (tref :doc-file "oc:doc:g2p-a.md")
                b      (tref :git-commit "g2p-sha")
                rel-id (rk/relation-id-for :produced a b "sid")
                req    (rk/assert-request
                         {:kind :produced :from a :to b :asserter-actor-id "sid" :asserter-type :human
                          :asserted-at-ms 1000 :request-id "g2p-req" :idempotency-key "g2p-idem"})]
            (submit! req) (drain!)
            ;; foreign-select routed by partition-by-decision-relation (clojure hash of
            ;; rel-id) MUST hit the row the topology wrote on the depot's rama-hash task.
            (let [decision-id (rk/decision-id-for rel-id "g2p-req")
                  decision    (rk/read-decision-by-id runtime decision-id)]
              (is (some? decision) "clojure hash == rama hash-by (mod N) for $$relation-decisions-by-id")
              (is (= :accepted (:status decision)))
              (is (= rel-id (:relation-id decision)))
              (let [event (rk/read-event-by-id runtime (:event-id decision))]
                (is (some? event) "custom key-partitioner also colocates $$relation-events-by-id")
                (is (= rel-id (:relation-id event)))))))

        (testing "G1 — same-batch multi-key convergence (intra-batch read-your-writes)"
          ;; Two same-relation requests, different idem keys AND different timestamps,
          ;; forced into ONE microbatch. If intra-batch read-your-writes were broken,
          ;; the 2nd request would see no current row, mint a 2nd copy (different
          ;; first-asserted-at-ms -> different sort-key) and double the descriptor.
          (let [a      (tref :doc-file "oc:doc:g1b-a.md")
                b      (tref :git-commit "g1b-sha")
                a-key  (:target-key a) b-key (:target-key b)
                rel-id (rk/relation-id-for :based-on a b "sid")
                base   {:kind :based-on :from a :to b :asserter-actor-id "sid" :asserter-type :human}
                req-1  (rk/assert-request (merge base {:asserted-at-ms 1000 :request-id "g1b-req-1" :idempotency-key "g1b-k1"}))
                req-2  (rk/assert-request (merge base {:asserted-at-ms 2000 :request-id "g1b-req-2" :idempotency-key "g1b-k2"}))]
            (pause!)
            (submit! req-1)
            (submit! req-2)
            (resume!)
            (drain!)
            (is (= 1 (count (rk/read-target-index runtime a-key))) "one from-side copy despite two requests in one batch")
            (is (= 1 (count (rk/read-target-index runtime b-key))) "one to-side copy")
            (let [o-desc (get (rk/read-target-descriptors runtime a-key)
                              (rk/target-descriptor-key :outgoing :based-on))
                  i-desc (get (rk/read-target-descriptors runtime b-key)
                              (rk/target-descriptor-key :incoming :based-on))]
              (is (= 1 (:asserted-count o-desc)) "descriptor counted once (read-your-writes held)")
              (is (= 1 (:asserted-count i-desc))))
            (let [detail (rk/read-relation-detail runtime rel-id)]
              (is (some? (:row detail)))
              (is (= rel-id (:relation-id (:row detail))) "one identity")
              (is (= :asserted (:relation-status (:row detail))))
              (is (= 2 (count (:history detail))) "both requests recorded as ordered status events"))))))))
