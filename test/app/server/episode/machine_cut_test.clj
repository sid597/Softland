(ns app.server.episode.machine-cut-test
  "Machine-cut driver suite (machine-cut CONTRACT §9). Gates in THIS lane:
     PURE (no cluster): G1 input determinism, G2 validation totality,
                        G3 pair-plan diff.
     IPC  (fake adapter, no live LLM — llm.clj:2179-2184 canned :lines):
                        G5 end-to-end assert, G6 idempotent re-run, G7 never-drop,
                        G8 reconcile, G9 epoch, G11 WAL replay.

   Platform discipline (CONTRACT §3, /rama testing): microbatch :append-ack does
   NOT imply PState visibility — every read is behind a materialized-read barrier
   (llm/await-run, rk/await-relation, both verified in-tree). Every 'X must NOT
   have happened' assertion names a physical reader (llm/read-dead-letters,
   rk/read-target-index — validation-only reads that exist in both kernels).

   IPC economy: ONE llm-module launch + ONE relation-kernel launch for
   G5-G9 (disjoint conversation addresses where they must not interfere), plus
   ONE fresh relation-kernel for G11 (fresh cluster + existing WAL). The fake
   adapter means no gate touches a live Claude."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.data.json :as json]
            [app.server.episode.machine-cut :as mc]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.episode.llm :as llm]
            [app.server.rama.ingest-epoch :as ingest-epoch]))

;; ════════════════════════════════════════════════════════════════════════════
;;  Fixtures — synthetic river-page blocks (the SAME shape river-page emits;
;;  block_distiller.clj:1293-1301) + canned stream-json output.
;; ════════════════════════════════════════════════════════════════════════════

(defn blk
  "One synthetic river-page block. Minimal fields the driver reads: :event-uuid
   :actor :text :unit-id :source-id (block_distiller river-page output shape)."
  [euid actor text unit src]
  {:order      [0 0 (str "sb:" unit)]
   :event-uuid euid
   :actor      actor
   :form       :prose
   :text       text
   :unit-id    unit
   :source-id  src
   :part-path  (str "p:" euid)
   :block-path (str "b:" unit)})

;; A 4-event conversation: two human prompts, each with one assistant response.
(def conv1-address "chat:mcconv1")
(def conv1-blocks
  [(blk "e1" "human:external" "What is 2+2?" "u1" "src:tr:chat:mcconv1:s1")
   (blk "e2" "assistant"      "It is 4."     "u2" "src:tr:chat:mcconv1:s2")
   (blk "e3" "human:external" "And 3+3?"     "u3" "src:tr:chat:mcconv1:s3")
   (blk "e4" "assistant"      "It is 6."     "u4" "src:tr:chat:mcconv1:s4")])

;; Two disjoint conversations for the epoch gate (G9) so success/failure runs
;; never touch conv1's edges.
(def conv2-address "chat:mcconv2")
(def conv2-blocks
  [(blk "f1" "human:external" "Ping?"  "v1" "src:tr:chat:mcconv2:t1")
   (blk "f2" "assistant"      "Pong."  "v2" "src:tr:chat:mcconv2:t2")])

(def read-plan-4 {:river-events-total 4 :blocks-returned 4 :truncated? false})
(def read-plan-2 {:river-events-total 2 :blocks-returned 2 :truncated? false})

(defn loader-fn
  "Injected :load-river-blocks — serves synthetic blocks per address (MC-T4: the
   driver reads via this shared river loader, never a second transcript path)."
  [address _limit]
  (case address
    "chat:mcconv1" {:blocks conv1-blocks :read-plan read-plan-4}
    "chat:mcconv2" {:blocks conv2-blocks :read-plan read-plan-2}
    {:blocks [] :read-plan {:river-events-total 0 :blocks-returned 0 :truncated? false}}))

(defn canned-lines
  "Stream-json lines the fake adapter replays (llm.clj:2179-2184): a system line
   then a `result` line whose `result` field is the LLM's JSON OUTPUT string."
  [output]
  ["{\"type\":\"system\",\"session_id\":\"mc-sess\"}"
   (json/write-str {:type "result" :is_error false :session_id "mc-sess"
                    :total_cost_usd 0.001
                    :result (json/write-str output)})])

(defn canned-malformed-lines
  "A run that SUCCEEDS terminally but returns non-JSON output (§5.3 malformed →
   recorded failed, zero writes)."
  []
  ["{\"type\":\"system\",\"session_id\":\"mc-sess\"}"
   (json/write-str {:type "result" :is_error false :session_id "mc-sess"
                    :total_cost_usd 0.001 :result "this is not json at all"})])

(defn temp-wal [] (str (System/getProperty "java.io.tmpdir")
                       "/mc-wal-" (System/nanoTime) ".ednl"))

;; ════════════════════════════════════════════════════════════════════════════
;;  G1 — input determinism (PURE)
;; ════════════════════════════════════════════════════════════════════════════

(deftest g1-input-determinism-test
  (testing "G1 — same synthetic data-context → identical input-hash + ids"
    (let [in1 (mc/build-input {:address conv1-address :blocks conv1-blocks
                               :read-plan read-plan-4 :limit 64})
          in2 (mc/build-input {:address conv1-address :blocks conv1-blocks
                               :read-plan read-plan-4 :limit 64})
          ids1 (mc/run-ids conv1-address (:input-hash in1) "")
          ids2 (mc/run-ids conv1-address (:input-hash in2) "")]
      (is (= (:input-hash in1) (:input-hash in2)) "input-hash is deterministic")
      (is (= ids1 ids2) "all synthetic ids deterministic")
      (is (= ["e1" "e2" "e3" "e4"] (:event-uuids in1)) "shown set in river order")
      (is (= 4 (get-in in1 [:window :river-events-total])))
      (is (false? (get-in in1 [:window :truncated?])))))

  (testing "G1 — one changed block text → input-hash AND every id change"
    (let [base (mc/build-input {:address conv1-address :blocks conv1-blocks
                                :read-plan read-plan-4 :limit 64})
          edited-blocks (assoc-in conv1-blocks [1 :text] "It is FOUR.")
          edited (mc/build-input {:address conv1-address :blocks edited-blocks
                                  :read-plan read-plan-4 :limit 64})
          ids-b (mc/run-ids conv1-address (:input-hash base) "")
          ids-e (mc/run-ids conv1-address (:input-hash edited) "")]
      (is (not= (:input-hash base) (:input-hash edited)) "changed text → changed hash")
      (is (not= (:run-hash ids-b) (:run-hash ids-e)))
      (is (not= (:turn-id ids-b) (:turn-id ids-e)))
      (is (not= (:llm-turn-run-id ids-b) (:llm-turn-run-id ids-e)))
      (is (not= (:context-bundle-id ids-b) (:context-bundle-id ids-e)))))

  (testing "G1 — salt separates a deliberate re-guess from an idempotent re-run"
    (let [ih (:input-hash (mc/build-input {:address conv1-address :blocks conv1-blocks
                                           :read-plan read-plan-4 :limit 64}))]
      (is (= (mc/run-ids conv1-address ih "") (mc/run-ids conv1-address ih ""))
          "same salt → same run")
      (is (not= (mc/run-ids conv1-address ih "") (mc/run-ids conv1-address ih "v2"))
          "different salt → different run")))

  (testing "G1 — MC-T9 bundle rebuild-verify: match on same input, fail on drift"
    (let [ih (:input-hash (mc/build-input {:address conv1-address :blocks conv1-blocks
                                           :read-plan read-plan-4 :limit 64}))
          bundle-id (:context-bundle-id (mc/run-ids conv1-address ih ""))
          drift-ih  (:input-hash (mc/build-input
                                   {:address conv1-address
                                    :blocks (assoc-in conv1-blocks [0 :text] "drift")
                                    :read-plan read-plan-4 :limit 64}))]
      (is (:ok? (mc/verify-bundle-hash bundle-id conv1-address ih "")))
      (is (not (:ok? (mc/verify-bundle-hash bundle-id conv1-address drift-ih "")))))))

;; ════════════════════════════════════════════════════════════════════════════
;;  G2 — validation totality (PURE)
;; ════════════════════════════════════════════════════════════════════════════

(def g2-input
  (mc/build-input {:address conv1-address :blocks conv1-blocks
                   :read-plan read-plan-4 :limit 64}))

(defn totality-ok?
  "Totality law (§5.3/§6): every shown event appears in exactly one of pairs
   (as prompt or response) or unpaired."
  [valid shown]
  (let [in-pairs (mapcat (fn [pr] (cons (:prompt pr) (:responses pr))) (:pairs valid))
        all      (concat in-pairs (:unpaired valid))]
    (= (set shown) (set all))))

(deftest g2-validation-totality-test
  (let [shown ["e1" "e2" "e3" "e4"]]
    (testing "G2 valid — two clean pairs, no rejections"
      (let [v (mc/validate-output {:pairs [{:prompt "e1" :responses ["e2"]}
                                           {:prompt "e3" :responses ["e4"]}]
                                   :unpaired []} g2-input)]
        (is (= [{:prompt "e1" :responses ["e2"]}
                {:prompt "e3" :responses ["e4"]}] (:pairs v)))
        (is (= [] (:unpaired v)))
        (is (= {:pairs 2 :unpaired 0 :unknown-ids 0 :non-human-prompts 0
                :duplicates 0 :malformed-pairs 0 :empty-pairs 0
                :unclassified 0} (:counts v)))
        (is (totality-ok? v shown))))

    (testing "G2 unknown-uuid — hallucinated id rejects the containing pair (MC-T3)"
      (let [v (mc/validate-output {:pairs [{:prompt "e1" :responses ["e2"]}
                                           {:prompt "e3" :responses ["e9"]}]
                                   :unpaired []} g2-input)]
        (is (= [{:prompt "e1" :responses ["e2"]}] (:pairs v)) "only the clean pair survives")
        (is (= 1 (:unknown-ids (:counts v))))
        (is (= ["e3" "e4"] (:unpaired v)) "e3/e4 fall to unpaired")
        (is (totality-ok? v shown))))

    (testing "G2 missing-uuid — totality: absent events → unpaired + :unclassified"
      (let [v (mc/validate-output {:pairs [{:prompt "e1" :responses ["e2"]}]
                                   :unpaired []} g2-input)]
        (is (= 1 (:pairs (:counts v))))
        (is (= ["e3" "e4"] (:unpaired v)))
        (is (= 2 (:unclassified (:counts v))) "e3,e4 never mentioned")
        (is (totality-ok? v shown))))

    (testing "G2 duplicate-uuid — an event claimed twice: first-in-river-order wins"
      (let [v (mc/validate-output {:pairs [{:prompt "e1" :responses ["e2"]}
                                           {:prompt "e3" :responses ["e2" "e4"]}]
                                   :unpaired []} g2-input)]
        (is (= [{:prompt "e1" :responses ["e2"]}
                {:prompt "e3" :responses ["e4"]}] (:pairs v))
            "e2 owned by the earlier prompt; dropped from e3's responses")
        (is (= 1 (:duplicates (:counts v))))
        (is (= [] (:unpaired v)))
        (is (totality-ok? v shown))))

    (testing "G2 non-human-prompt — assistant event cannot open a pair (§5.3 T1)"
      (let [v (mc/validate-output {:pairs [{:prompt "e2" :responses ["e1"]}]
                                   :unpaired []} g2-input)]
        (is (= [] (:pairs v)))
        (is (= 1 (:non-human-prompts (:counts v))))
        (is (= ["e1" "e2" "e3" "e4"] (:unpaired v)))
        (is (totality-ok? v shown))))

    (testing "G2 malformed JSON — parse never throws; failed shape (§5.3)"
      (let [p (mc/parse-output "this is not json {")]
        (is (false? (:ok? p)))
        (is (= :invalid-json (get-in p [:error :reason])))))))

;; ════════════════════════════════════════════════════════════════════════════
;;  G3 — pair-plan diff (PURE)
;; ════════════════════════════════════════════════════════════════════════════

(defn edge-spec
  "A minimal desired edge spec (only :relation-id is read by reconcile-plan)."
  [rid] {:relation-id rid})

(defn edge-row
  "A minimal existing RelationEdgeRow (only :relation-id + :relation-status read)."
  [rid status] {:relation-id rid :relation-status status})

(deftest g3-pair-plan-diff-test
  (testing "G3 — asserts / retracts / unchanged exact (§5.4)"
    (let [plan (mc/reconcile-plan
                 [(edge-spec "rel:A") (edge-spec "rel:B")]          ; desired
                 [(edge-row "rel:A" :asserted)                       ; unchanged
                  (edge-row "rel:X" :asserted)])]                    ; stale → retract
      (is (= #{"rel:B"} (set (map :relation-id (:asserts plan)))) "B new")
      (is (= #{"rel:X"} (set (map :relation-id (:retracts plan)))) "X stale")
      (is (= #{"rel:A"} (set (map :relation-id (:unchanged plan)))) "A unchanged")))

  (testing "G3 — a retracted existing row is NOT asserted → desired re-asserts it"
    (let [plan (mc/reconcile-plan
                 [(edge-spec "rel:A")]
                 [(edge-row "rel:A" :retracted)])]
      (is (= ["rel:A"] (map :relation-id (:asserts plan))) "retracted → re-assert")
      (is (empty? (:retracts plan)))
      (is (empty? (:unchanged plan)))))

  (testing "G3 — empty desired retracts every asserted existing edge"
    (let [plan (mc/reconcile-plan
                 []
                 [(edge-row "rel:A" :asserted) (edge-row "rel:B" :asserted)])]
      (is (empty? (:asserts plan)))
      (is (= #{"rel:A" "rel:B"} (set (map :relation-id (:retracts plan))))))))

;; ════════════════════════════════════════════════════════════════════════════
;;  IPC harness
;; ════════════════════════════════════════════════════════════════════════════

(defn with-runtimes
  "One llm-module + one relation-kernel launch (two IPCs — a foreign client spans
   IPCs; the git_spine/code_atoms precedent). try/finally cleanup."
  [f]
  (let [llm-rt (llm/start-llm-runtime!)
        rk-rt  (rk/start-relation-runtime! {:tasks 4 :threads 2})]
    (try
      (f llm-rt rk-rt)
      (finally
        (llm/close-llm-runtime! llm-rt)
        (rk/close-relation-runtime! rk-rt)))))

(defn ctx-for [llm-rt rk-rt wal]
  {:llm-rt llm-rt :rk-rt rk-rt :load-river-blocks loader-fn :wal-path wal})

(defn expected-rel-id
  "The deterministic relation-id of the response→prompt edge, via the driver's
   own edge builder (no hardcoded hashes)."
  [address input response-uuid prompt-uuid]
  (-> (mc/pairs->edge-specs {:address address
                             :pairs [{:prompt prompt-uuid :responses [response-uuid]}]
                             :events (:events input) :run-id "x" :observed-model nil
                             :window (:window input) :asserted-at-ms 0})
      first :relation-id))

(deftest machine-cut-ipc-test
  (with-runtimes
    (fn [llm-rt rk-rt]
      (let [wal   (temp-wal)
            ctx   (ctx-for llm-rt rk-rt wal)
            input1 (mc/build-input {:address conv1-address :blocks conv1-blocks
                                    :read-plan read-plan-4 :limit 64})
            rid-e2-e1 (expected-rel-id conv1-address input1 "e2" "e1")
            rid-e4-e3 (expected-rel-id conv1-address input1 "e4" "e3")
            rid-e4-e1 (expected-rel-id conv1-address input1 "e4" "e1")]

        ;; ── G5 end-to-end assert ────────────────────────────────────────────
        (testing "G5 — corpus → run row + rk edges land with §4 identity/provenance"
          (let [res (mc/annotate-conversation!
                      ctx conv1-address
                      {:lines (canned-lines {:pairs [{:prompt "e1" :responses ["e2"]}
                                                     {:prompt "e3" :responses ["e4"]}]
                                             :unpaired []})})
                run-id (:run-id res)
                run    (llm/read-run llm-rt run-id)
                edges  (get (rk/read-relations-for-targets rk-rt [conv1-address] [:pairs-with] false)
                            conv1-address)
                by-rid (into {} (map (juxt :relation-id identity)) edges)
                e21    (get by-rid rid-e2-e1)]
            (is (= :completed (:status res)))
            (is (= 2 (:edges-asserted res)))
            (is (= 0 (:edges-retracted res)))
            ;; llm-module run row exists under the synthetic id
            (is (some? run) "run row materialized under synthetic run-id")
            (is (= :succeeded (:status run)))
            (is (= "llm-run-mc:" (subs run-id 0 11)) "synthetic run-id prefix")
            ;; rk edges: exactly 2, correct identity/provenance/evidence/note
            (is (= 2 (count edges)))
            (is (= #{rid-e2-e1 rid-e4-e3} (set (keys by-rid))))
            (is (= :pairs-with (:relation-kind e21)))
            (is (= mc/machine-cut-actor-id (:asserter-actor-id e21)))
            (is (= :llm (:asserter-type e21)))
            (is (= :asserted (:relation-status e21)))
            (is (= (tid/chat-message-id conv1-address "e2") (:target-id (:from e21)))
                "from = the response event")
            (is (= (tid/chat-message-id conv1-address "e1") (:target-id (:to e21)))
                "to = the prompt event")
            (is (= "src:tr:chat:mcconv1:s2" (:evidence-source-id e21))
                "evidence = response event's first block source-id (§4.3)")
            (is (re-find #"^machine-cut v1 run=llm-run-mc:" (:note e21)) "note grammar v1")
            ;; §4.2 / MC-T14: both endpoint copies (o:/i:) live under the ONE
            ;; conversation key; the query dedups by relation-id.
            (let [raw (rk/read-target-index rk-rt conv1-address)]
              (is (= 4 (count raw)) "2 edges × o/i copies under one key (MC-T14)"))
            ;; dual-readable: the authoritative row agrees with the target read.
            (let [detail (rk/read-relation-detail rk-rt rid-e2-e1)]
              (is (= :asserted (:relation-status (:row detail)))))

            ;; ── G7 never-drop ─────────────────────────────────────────────
            (testing "G7 — dead-letters empty; every observation folded"
              (is (empty? (llm/read-dead-letters llm-rt run-id))))))

        ;; ── G6 idempotent re-run ────────────────────────────────────────────
        (testing "G6 — same input+version → ZERO adapter calls, PStates identical"
          (let [before-run  (llm/read-run llm-rt (:llm-turn-run-id (mc/run-ids
                                                    conv1-address (:input-hash input1) "")))
                before-edges (get (rk/read-relations-for-targets rk-rt [conv1-address] [:pairs-with] false)
                                  conv1-address)
                res2 (mc/annotate-conversation!
                       ctx conv1-address
                       {:lines (canned-lines {:pairs [{:prompt "e1" :responses ["e2"]}
                                                      {:prompt "e3" :responses ["e4"]}]
                                              :unpaired []})})
                after-run  (llm/read-run llm-rt (:run-id res2))
                after-edges (get (rk/read-relations-for-targets rk-rt [conv1-address] [:pairs-with] false)
                                 conv1-address)]
            (is (= :noop-complete (:status res2)) "re-run is a total no-op")
            (is (false? (:epoch-bumped? res2)))
            (is (= before-run after-run) "run row value-identical (no re-fold)")
            (is (= (set (map :relation-id before-edges))
                   (set (map :relation-id after-edges))) "edge set value-identical")))

        ;; ── G9 epoch ────────────────────────────────────────────────────────
        (testing "G9 — epoch bumps by 1 after edge acks; no bump on a failed run"
          (let [before @ingest-epoch/!ingest-epoch-atom
                ok  (mc/annotate-conversation!
                      ctx conv2-address
                      {:lines (canned-lines {:pairs [{:prompt "f1" :responses ["f2"]}]
                                             :unpaired []})})
                after-ok @ingest-epoch/!ingest-epoch-atom
                fail (mc/annotate-conversation!
                       ctx conv2-address
                       {:salt "malformed" :lines (canned-malformed-lines)})
                after-fail @ingest-epoch/!ingest-epoch-atom]
            (is (= :completed (:status ok)))
            (is (true? (:epoch-bumped? ok)))
            (is (= (inc before) after-ok) "exactly +1 after a successful write")
            (is (= :failed (:status fail)))
            (is (= :malformed-json (:reason fail)))
            (is (= 0 (:edges-asserted fail)) "malformed output writes zero edges")
            (is (= after-ok after-fail) "no epoch bump on a failed/zero-write run")))

        ;; ── G8 reconcile ────────────────────────────────────────────────────
        (testing "G8 — a salted re-guess moves a pairing: stale retracted, new asserted"
          (let [res (mc/annotate-conversation!
                      ctx conv1-address
                      {:salt "v2"
                       :lines (canned-lines {:pairs [{:prompt "e1" :responses ["e2" "e4"]}
                                                     {:prompt "e3" :responses []}]
                                             :unpaired []})})
                asserted (get (rk/read-relations-for-targets rk-rt [conv1-address] [:pairs-with] false)
                              conv1-address)
                asserted-ids (set (map :relation-id asserted))
                stale-detail (rk/read-relation-detail rk-rt rid-e4-e3)]
            (is (= :completed (:status res)))
            (is (= 1 (:edges-asserted res)) "e4→e1 new")
            (is (= 1 (:edges-retracted res)) "e4→e3 stale")
            (is (contains? asserted-ids rid-e4-e1) "moved edge asserted")
            (is (contains? asserted-ids rid-e2-e1) "unchanged edge untouched")
            (is (not (contains? asserted-ids rid-e4-e3)) "stale edge no longer asserted")
            (is (= :retracted (:relation-status (:row stale-detail))) "stale edge retracted")
            (is (= [:asserted :retracted] (mapv :relation-status (:history stale-detail)))
                "history shows both transitions")))

        ;; ── G11 WAL replay ──────────────────────────────────────────────────
        (testing "G11 — fresh cluster + existing WAL → edges re-asserted, ZERO adapter"
          ;; Append a torn trailing line to the WAL: replay must drop it and
          ;; replay everything before it (face-arsenal precedent).
          (spit wal "{:mc/status :completed :mc/address \"chat:tornnnn\" :mc/edges [" :append true)
          (let [fresh (rk/start-relation-runtime! {:tasks 4 :threads 2})]
            (try
              (let [stats (mc/replay-wal! {:rk-rt fresh :wal-path wal})
                    edges (get (rk/read-relations-for-targets fresh [conv1-address] [:pairs-with] false)
                               conv1-address)
                    ids   (set (map :relation-id edges))]
                (is (pos? (:failed stats)) "torn trailing line counted + dropped")
                (is (pos? (:lines stats)) "completed lines replayed")
                ;; Replay re-applies reconcile in file order → the v2 move is
                ;; reproduced: e4→e1 + e2→e1 asserted, e4→e3 retracted.
                (is (contains? ids rid-e4-e1) "re-asserted with identical id")
                (is (contains? ids rid-e2-e1))
                (is (not (contains? ids rid-e4-e3)) "stale edge retracted on replay")
                (is (= :asserted (:relation-status
                                   (:row (rk/read-relation-detail fresh rid-e4-e1))))
                    "ids identical across the fresh cluster"))
              (finally
                (rk/close-relation-runtime! fresh)))))))))

;; ════════════════════════════════════════════════════════════════════════════
;;  Gate-fix regressions (2026-07-12 falsification pass — FALSIFY_A/B/C).
;;  Each test is the biting form of a CONFIRMED finding; see
;;  build/machine-cut/INT.md §falsification for the dispositions.
;; ════════════════════════════════════════════════════════════════════════════

(deftest gate-fix-transition-keys-test
  (testing "F2 — first assert keeps the plain key; re-assert keys on the incumbent"
    (let [plan-new (mc/reconcile-plan [(edge-spec "rel:A")] [])
          plan-re  (mc/reconcile-plan
                     [(edge-spec "rel:A")]
                     [{:relation-id "rel:A" :relation-status :retracted
                       :status-changed-at-ms 777}])]
      (is (= "mc:rel:A" (:request-id (first (:asserts plan-new))))
          "never-seen relation → the plain stable key")
      (is (= "mc:rel:A:asserted:777" (:request-id (first (:asserts plan-re))))
          "re-assert after retract mints a TRANSITION key — the relation
           journal would replay the swallowed original (F2)")))
  (testing "F2 — retract keys on the asserted incumbent's transition time"
    (let [plan (mc/reconcile-plan
                 [] [{:relation-id "rel:X" :relation-status :asserted
                      :status-changed-at-ms 123}])]
      (is (= "mc:rel:X:retracted:123" (:mc/request-id (first (:retracts plan))))))))

(deftest gate-fix-validation-shapes-test
  (testing "F5 — an empty-responses pair is DROPPED (its prompt falls to unpaired), counted"
    (let [valid (mc/validate-output
                  {:pairs [{:prompt "e1" :responses []}
                           {:prompt "e3" :responses ["e4"]}]
                   :unpaired ["e2"]}
                  g2-input)]
      (is (= 1 (get-in valid [:counts :empty-pairs])))
      (is (= 1 (get-in valid [:counts :pairs])) "only the real pair survives")
      (is (some #{"e1"} (:unpaired valid)) "the empty pair's prompt is unpaired")
      (is (totality-ok? valid (:event-uuids g2-input)))))
  (testing "F11 — a non-sequential :responses is shape-rejected whole, never char-seq'd"
    (let [valid (mc/validate-output
                  {:pairs [{:prompt "e1" :responses "e2"}]
                   :unpaired ["e3" "e4"]}
                  g2-input)]
      (is (= 1 (get-in valid [:counts :malformed-pairs])))
      (is (= 0 (get-in valid [:counts :pairs])))
      (is (zero? (get-in valid [:counts :unknown-ids]))
          "the bare string must NOT count as N unknown char-ids")
      (is (totality-ok? valid (:event-uuids g2-input))))))

(deftest gate-fix-ipc-regressions-test
  (with-runtimes
    (fn [llm-rt rk-rt]
      (let [wal    (temp-wal)
            ctx    (ctx-for llm-rt rk-rt wal)
            input1 (mc/build-input {:address conv1-address :blocks conv1-blocks
                                    :read-plan read-plan-4 :limit 64})
            rid-e2-e1 (expected-rel-id conv1-address input1 "e2" "e1")
            rid-e4-e3 (expected-rel-id conv1-address input1 "e4" "e3")
            rid-e4-e1 (expected-rel-id conv1-address input1 "e4" "e1")
            pairing-a {:pairs [{:prompt "e1" :responses ["e2"]}
                               {:prompt "e3" :responses ["e4"]}] :unpaired []}
            pairing-b {:pairs [{:prompt "e1" :responses ["e2" "e4"]}]
                       :unpaired ["e3"]}
            status-of (fn [rid] (:relation-status (:row (rk/read-relation-detail rk-rt rid))))]

        (testing "F2/F3 — A→B→A pairing flip converges on A (re-assert not swallowed)"
          (let [r1 (mc/annotate-conversation! ctx conv1-address
                     {:lines (canned-lines pairing-a)})
                r2 (mc/annotate-conversation! ctx conv1-address
                     {:salt "flip" :lines (canned-lines pairing-b)})
                r3 (mc/annotate-conversation! ctx conv1-address
                     {:salt "back" :lines (canned-lines pairing-a)})]
            (is (= :completed (:status r1)))
            (is (= :completed (:status r2)))
            (is (= :completed (:status r3)) "the flip-back run completes")
            (is (= 1 (:edges-asserted r3)) "e4→e3 re-asserted")
            (is (= 1 (:edges-retracted r3)) "e4→e1 retracted")
            (is (= :asserted (status-of rid-e4-e3))
                "THE F2 KILL: pre-fix the journal replayed the original assert
                 decision and the edge stayed :retracted")
            (is (= :retracted (status-of rid-e4-e1)))
            (is (= :asserted (status-of rid-e2-e1)) "untouched edge stays")
            (is (= [:asserted :retracted :asserted]
                   (mapv :relation-status
                         (:history (rk/read-relation-detail rk-rt rid-e4-e3))))
                "full transition history preserved")))

        (testing "F1 — noop path VERIFIES + CONVERGES from the run's WAL line"
          ;; Simulate run-state/edge-state divergence: retract one edge behind
          ;; the driver's back (same actor — the retraction-rights law holds).
          (let [row (:row (rk/read-relation-detail rk-rt rid-e2-e1))
                _   (rk/append-relation-request!
                      rk-rt (rk/retract-request
                              {:kind :pairs-with :from (:from row) :to (:to row)
                               :asserter-actor-id mc/machine-cut-actor-id
                               :asserter-type :llm
                               :asserted-at-ms 1 :sent-at-ms 1
                               :request-id "manual-divergence-1"
                               :idempotency-key "manual-divergence-1"}))
                _   (rk/await-relation
                      #(rk/read-relation-detail rk-rt rid-e2-e1)
                      #(= :retracted (:relation-status (:row %))))
                epoch-before @ingest-epoch/!ingest-epoch-atom
                res (mc/annotate-conversation! ctx conv1-address
                      {:salt "back" :lines (canned-lines pairing-a)})]
            (is (= :noop-complete (:status res)) "prior succeeded run → noop path")
            (is (= 1 (:edges-asserted res)) "the diverged edge was re-asserted")
            (is (= :asserted (status-of rid-e2-e1))
                "THE F1 KILL: pre-fix noop-complete never looked at the edges")
            (is (true? (:epoch-bumped? res)))
            (is (= (inc epoch-before) @ingest-epoch/!ingest-epoch-atom))))

        (testing "F1 — a succeeded run with NO WAL line is honest stale, never silent"
          (let [res (mc/annotate-conversation!
                      (assoc ctx :wal-path (temp-wal))     ; a WAL that lacks the line
                      conv1-address
                      {:salt "back" :lines (canned-lines pairing-a)})]
            (is (= :stale-no-wal (:status res)))
            (is (true? (:retry-with-salt res)))))

        (testing "F7 — a bundle-hash mismatch is an honest :failed, never a throw"
          (let [calls (atom 0)
                shifting-loader (fn [address limit]
                                  (let [n (swap! calls inc)]
                                    (if (= 1 n)
                                      (loader-fn address limit)
                                      ;; the rebuild sees CHANGED material
                                      {:blocks (assoc-in conv2-blocks [0 :text] "CHANGED")
                                       :read-plan read-plan-2})))
                epoch-before @ingest-epoch/!ingest-epoch-atom
                res (mc/annotate-conversation!
                      (assoc ctx :load-river-blocks shifting-loader)
                      conv2-address {:lines (canned-lines {:pairs [] :unpaired ["f1" "f2"]})})]
            (is (= :failed (:status res)) "returned, not thrown")
            (is (= :bundle-hash-mismatch (:reason res)))
            (is (= 0 (:edges-asserted res)))
            (is (= epoch-before @ingest-epoch/!ingest-epoch-atom) "no epoch bump")))

        (testing "F6 — WAL replay is WINDOW-scoped: out-of-window edges survive"
          ;; Seed a machine-cut edge whose events are NOT in any WAL line's
          ;; window (a hypothetical page-2 annotation's edge under the same
          ;; conversation key).
          (let [from-ref (rk/->target-ref :container (tid/chat-message-id conv1-address "g9"))
                to-ref   (rk/->target-ref :container (tid/chat-message-id conv1-address "g8"))
                rid-g    (rk/relation-id-for :pairs-with from-ref to-ref mc/machine-cut-actor-id)
                _ (rk/append-relation-request!
                    rk-rt (rk/assert-request
                            {:kind :pairs-with :from from-ref :to to-ref
                             :asserter-actor-id mc/machine-cut-actor-id
                             :asserter-type :llm :asserted-at-ms 2 :sent-at-ms 2
                             :request-id (str "mc:" rid-g)
                             :idempotency-key (str "mc:" rid-g)}))
                _ (rk/await-relation
                    #(rk/read-relation-detail rk-rt rid-g)
                    #(= :asserted (:relation-status (:row %))))
                stats (mc/replay-wal! {:rk-rt rk-rt :wal-path wal})]
            (is (pos? (:lines stats)) "the run WAL replayed")
            (is (= :asserted (status-of rid-g))
                "THE F6 KILL: pre-fix replay reconciled address-wide and
                 retracted the out-of-window edge")))))))
