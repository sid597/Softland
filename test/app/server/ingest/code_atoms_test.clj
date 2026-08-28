;; code-atom WP Phase P2 gate suite (CONTRACT §8 G5/G6/G9, §12 P2). ONE deftest,
;; ONE IPC launch with BOTH source modules + the relation kernel (trail-view
;; runtime = OC + OC-ops + relation-kernel + trail-view — the git_spine_test
;; precedent). Drives code-sync! against THIS repo scoped by :commit-filter to the
;; pinned specimen commits. Relation edges settle on the microbatch
;; processed-count barrier (implementation-quirks discipline: deterministic
;; barrier, physical PState readers for negatives — never Thread/sleep as proof of
;; a negative). Ground truth was derived ONCE at phase time from the REAL adapter
;; cut + git diffs and is quoted verbatim below; it == the driver's pure
;; commit-lineage (cross-checked in PHASE_P2.md).
(ns app.server.ingest.code-atoms-test
  (:require [app.server.ingest.code-atoms :as ca]
            [app.server.rama.trail-view :as tv]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [com.rpl.rama.test :as rtest]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]))

(def ^:private rel-topo "relation-kernel-topology")
(def ^:private repo-root (System/getProperty "user.dir"))

;; ── Pinned specimen (full shas + committer epochs) ───────────────────────────
;;   git log -1 --format="%H %ct" <short>
(def ^:private c-2796044 "27960446538dca556565d35b1d052139190c8f50")
(def ^:private c-af0e0e2 "af0e0e23b538031ac1f73bcbad52bf48cc848f6c")
(def ^:private c-fd59b78 "fd59b788bc26c65bebf4906049f8121c3671af9f")
(def ^:private c-63202b0 "63202b038a0670e93f12f3757ffa426995e06f77")
(def ^:private c-119f3f8 "119f3f8586b717dd3ba6fbdfbb1989260bfb9bb9")
(def ^:private g5-filter #{c-2796044 c-af0e0e2 c-fd59b78 c-63202b0})
(def ^:private af0e0e2-committer-ms (* 1000 1783185197))   ; %ct 1783185197
(def ^:private c119f3f8-committer-ms (* 1000 1780942527))

;; relation_kernel.clj blob shas across the G5 chain (git raw --no-abbrev, pinned).
;; Each commit's OLD sha == the previous commit's NEW sha (self-contained chain):
;;   2796044: :000000..→1dfea68  A (file born)
;;   af0e0e2: 1dfea68→79b6ae3     M
;;   fd59b78: 79b6ae3→f102b76     M
;;   63202b0: f102b76→a002c89     M
(def ^:private rk-blob
  {c-2796044 "1dfea6898bd7c6f082fdcde06c864999c7ba8345"
   c-af0e0e2 "79b6ae36b02ff5788bf1469f9730f3a92552704f"
   c-fd59b78 "f102b764f5c5fdc38b9606451249393021d1b45c"
   c-63202b0 "a002c89649ad55906f9a179e7b03d33216b72ad2"})

;; ── af0e0e2 ground truth (DERIVED at phase time; VERIFIED == driver commit-lineage)
;; `git show af0e0e2 -- src/app/server/rama/relation_kernel.clj` touches exactly
;; these 6 top-level NAMED forms (custody fields on 3 rows + the 3 fns that write
;; them). Every OTHER named form is byte-identical 1dfea68→79b6ae3, so re-addressed
;; (hash-equal) → mint NOTHING. (The 7th af0e0e2 mech overall is the TEST file's
;; `relation-kernel-write-and-read-test` — outside relation_kernel.clj.)
(def ^:private af0e0e2-superseded-vars
  ["RelationDecisionRow" "RelationEdgeRow" "RelationEventRow"
   "rejected-decision-row" "relation-outcome" "transition-row"])
;; 3 sampled RE-ADDRESSED vars — present + byte-identical in both blobs → NO edge:
(def ^:private af0e0e2-readdressed-sample ["relation-id-for" "sha1-hex" "well-formed-target?"])

;; ── G6 silver ground truth (DERIVED: 45 hash-exact moves OC-old(4585a02) → the 3
;; new adapter files; 34 md / 5 ta / 6 ti). 3 sampled moves, one per adapter file:
(def ^:private oc-old-119 "4585a02e4bd60028b830181272524b56dd38c1d9")   ; object_container.clj OLD
(def ^:private oc-new-119 "2bf717a90a056e9a23ab93ac16e49a655869f70e")   ; object_container.clj NEW
(def ^:private silver-samples
  ;; [var vanished-from-blob appeared-in-blob]
  [["materialization-object-key" oc-old-119 "07d7fe75a900cb75a849372b6057dbf6b3e280e9"]  ; → markdown_adapter
   ["transcript-role"            oc-old-119 "0e44c32f52eabf461558255cbb7a8470478ae235"]  ; → transcript_adapter
   ["transcript-source-id"       oc-old-119 "e0b8a63d94080fb7a64f2369f03c422e8d68c610"]]) ; → transcript_identity

;; ── helpers ──────────────────────────────────────────────────────────────────
(defn- uid [sha block-path] (ca/code-unit-id repo-root sha block-path))

(defn- supersedes-rid [new-uid old-uid]
  (rk/relation-id-for :supersedes
                      (rk/->target-ref ca/code-form-target-kind new-uid)
                      (rk/->target-ref ca/code-form-target-kind old-uid)
                      ca/lineage-asserter-actor-id))

(defn- edge-row [rt rid] (rk/read-relation-row rt rid))          ; physical $$relations-by-id (V1)
(defn- rows-under [rt target-key] (get (rk/read-relations-for-targets rt [target-key] nil false) target-key))

;; =============================================================================
(deftest code-atoms-lineage-gates
  (let [rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})
        cumulative (atom 0)
        drain! (fn [] (rtest/wait-for-microbatch-processed-count (:ipc rt) (:module-name rt) rel-topo @cumulative 30000))]
    (try
      ;; ===================================================================
      ;; DENY (R6, T10) — runs FIRST so the denied blob is genuinely un-ingested.
      ;; Deny BOTH af0e0e2 code paths → 0 ingests, 0 edges (no interference with G5).
      ;; ===================================================================
      (testing "R6/T10 — deny-list-override: denied path never ingested, counted, no text read"
        (let [rd (ca/code-sync! {:runtime rt :repo-root repo-root :commit-filter #{c-af0e0e2}
                                 :deny-list-override #{"src/app/server/rama/relation_kernel.clj"
                                                       "test/app/server/rama/relation_kernel_test.clj"}})
              denied-okey (oc/object-key-for (str "git-blob:" (rk-blob c-af0e0e2))
                                             (oc/source-hash (ca/blob-text repo-root (rk-blob c-af0e0e2))))
              denied-src (oc/source-id-for-object-key denied-okey)]
          (is (= 2 (:blobs-seen rd)) "af0e0e2 has 2 code blobs")
          (is (= 2 (:blobs-denied rd)) "both denied by path")
          (is (= 0 (:blobs-ingested rd)) "denied → nothing ingested")
          (is (= 0 (:supersedes-mech rd)) "denied paths → lineage skipped, no edges")
          (is (nil? (ocr/read-source rt denied-src))
              "denied blob's source is ABSENT — text never left git (deny before cat-file)")
          ;; the would-be relation-outcome edge must not exist yet either
          (is (nil? (edge-row rt (supersedes-rid (uid (rk-blob c-af0e0e2) "relation-outcome")
                                                 (uid (rk-blob c-2796044) "relation-outcome"))))
              "no af0e0e2 supersedes edge while denied")))

      ;; ===================================================================
      ;; G5 — lineage counts on real history (the specimen's 4 commits)
      ;; ===================================================================
      (let [r5 (ca/code-sync! {:runtime rt :repo-root repo-root :commit-filter g5-filter})]
        (swap! cumulative + (:supersedes-mech r5) (:supersedes-silver r5))
        (drain!)

        (testing "G5 — stats exact (whole-classification pin; every bound named, no caps)"
          (is (= {:commits-seen 4 :blobs-seen 8 :blobs-ingested 8 :blobs-denied 0 :blobs-converged 0
                  :blobs-unresolved 0 :git-failures 0 :blobs-unparseable 0 :lineage-over-unresolved 0
                  :supersedes-mech 16 :supersedes-silver 0 :re-addressed 284
                  :unmatched-vanished 0 :unmatched-appeared 114}
                 r5)
              "G5 filter: 16 mech, 0 silver, 284 re-addressed, 8 blobs ingested; F4/F5/G-F1 honest zeros"))

        (testing "G5 — af0e0e2: :supersedes edge EXACTLY for the 6 changed vars (physical read)"
          (doseq [v af0e0e2-superseded-vars]
            (let [rid (supersedes-rid (uid (rk-blob c-af0e0e2) v) (uid (rk-blob c-2796044) v))
                  row (edge-row rt rid)]
              (is (some? row) (str "mech supersedes edge present for " v))
              (is (= :supersedes (:relation-kind row)) v)
              (is (= :asserted (:relation-status row)) v)
              (is (= (str "clj-atoms-v1|mech|" c-af0e0e2) (:note row)) (str v " carries the mech marker + child sha"))
              (is (= "import:code-lineage" (:asserter-actor-id row)) (str v " version-free asserter (R4)"))
              (is (= af0e0e2-committer-ms (:first-asserted-at-ms row)) (str v " asserted-at = committer clock (T4)")))))

        (testing "G5 — negative: hash-equal (re-addressed) vars mint ZERO edges (sample 3, physical)"
          (doseq [v af0e0e2-readdressed-sample]
            (let [rid (supersedes-rid (uid (rk-blob c-af0e0e2) v) (uid (rk-blob c-2796044) v))]
              (is (nil? (edge-row rt rid))
                  (str "re-addressed " v " has NO edge — so certainly no \"mech\"-noted edge (T2)")))))

        (testing "G5 — the ingested NEW blob JOINS: its source-id is the edge evidence-source-id"
          ;; relation-outcome's af0e0e2 edge evidence-source-id = the NEW blob's source-id,
          ;; and that source artifact is present (dogfood receipt: edge → blob → git).
          (let [new-okey (oc/object-key-for (str "git-blob:" (rk-blob c-af0e0e2))
                                            (oc/source-hash (ca/blob-text repo-root (rk-blob c-af0e0e2))))
                new-src  (oc/source-id-for-object-key new-okey)
                rid (supersedes-rid (uid (rk-blob c-af0e0e2) "relation-outcome") (uid (rk-blob c-2796044) "relation-outcome"))
                row (edge-row rt rid)]
            (is (= new-src (:evidence-source-id row)) "evidence-source-id = the NEW blob's source-id")
            (is (some? (ocr/read-source rt new-src)) "the NEW blob was ingested (source artifact present)"))))

      ;; ===================================================================
      ;; G6 — silver-only move proposals across 119f3f8 (the adapter split)
      ;; ===================================================================
      (let [r6 (ca/code-sync! {:runtime rt :repo-root repo-root :commit-filter #{c-119f3f8}})]
        (swap! cumulative + (:supersedes-mech r6) (:supersedes-silver r6))
        (drain!)

        (testing "G6 — stats exact: 45 silver moves, 18 within-file mech, 309 re-addressed"
          (is (= {:commits-seen 1 :blobs-seen 6 :blobs-ingested 6 :blobs-denied 0 :blobs-converged 0
                  :blobs-unresolved 0 :git-failures 0 :blobs-unparseable 0 :lineage-over-unresolved 0
                  :supersedes-mech 18 :supersedes-silver 45 :re-addressed 309
                  :unmatched-vanished 21 :unmatched-appeared 22}
                 r6)
              "119f3f8: 45 hash-exact silver, 18 within-file mech; F4/F5/G-F1 honest zeros"))

        (testing "G6 — silver-marker discipline: moved vars surface as SILVER, NEVER mech (physical)"
          (doseq [[v van-sha app-sha] silver-samples]
            (let [van-uid (uid van-sha v)
                  app-uid (uid app-sha v)
                  rid (supersedes-rid app-uid van-uid)          ; from = appeared(new form), to = vanished(old)
                  row (edge-row rt rid)]
              (is (some? row) (str "silver-move edge present for " v))
              (is (str/starts-with? (str (:note row)) (str "clj-atoms-v1|silver-move|hash-exact|" c-119f3f8))
                  (str v " carries the silver-move marker (SPEC §4.3: the map must not lie)"))
              (is (not (str/includes? (str (:note row)) "|mech|")) (str v " is NOT mech")))))

        (testing "G6 — no false cross-file mech: every edge touching a moved var is silver, not mech"
          (doseq [[v van-sha _] silver-samples]
            (let [van-uid (uid van-sha v)
                  rows (rows-under rt van-uid)]
              (is (seq rows) (str "moved var " v " has ≥1 edge under its unit-id"))
              (is (every? #(not (str/includes? (str (:note %)) "|mech|")) rows)
                  (str "NO mech edge spans files for " v))
              (is (some #(str/includes? (str (:note %)) "|silver-move|") rows)
                  (str v " has its silver-move edge")))))

        (testing "G6 — a genuine WITHIN-FILE change IS mech (object_container.clj ns form changed)"
          (let [rid (supersedes-rid (uid oc-new-119 "ns") (uid oc-old-119 "ns"))
                row (edge-row rt rid)]
            (is (some? row) "object_container.clj ns supersedes edge present")
            (is (= (str "clj-atoms-v1|mech|" c-119f3f8) (:note row)) "within-file change carries the mech marker"))))

      ;; ===================================================================
      ;; G9 — byte-identical re-run (no wall clock; convergence)
      ;; ===================================================================
      (let [r9 (ca/code-sync! {:runtime rt :repo-root repo-root :commit-filter g5-filter})]
        (swap! cumulative + (:supersedes-mech r9) (:supersedes-silver r9))   ; += 0
        (drain!)

        (testing "G9 — re-run appends NOTHING new; converged; classification deterministic"
          (is (= {:commits-seen 4 :blobs-seen 8 :blobs-ingested 0 :blobs-denied 0 :blobs-converged 8
                  :blobs-unresolved 0 :git-failures 0 :blobs-unparseable 0 :lineage-over-unresolved 0
                  :supersedes-mech 0 :supersedes-silver 0 :re-addressed 284
                  :unmatched-vanished 0 :unmatched-appeared 114}
                 r9)
              "2nd pass: 0 fresh blobs (all converged), 0 new edges; re-addressed/unmatched stable"))

        (testing "G9 — no wall clock: a re-derived edge's asserted-at == the pinned committer ms"
          (let [rid (supersedes-rid (uid (rk-blob c-af0e0e2) "relation-outcome") (uid (rk-blob c-2796044) "relation-outcome"))
                row (edge-row rt rid)]
            (is (= af0e0e2-committer-ms (:first-asserted-at-ms row))
                "committer clock, unchanged across the byte-identical re-run (T4)")
            (is (= (str "clj-atoms-v1|mech|" c-af0e0e2) (:note row)) "note unchanged"))))

      ;; ===================================================================
      ;; F4 (GATE_REVIEW 2026-07-09 doubt 1) — the accounting identity at a
      ;; NON-ZERO :blobs-unresolved. Fault-inject a DROPPED OC decision (an await
      ;; timeout returns nil, runtime.clj:326): code-sync! must COUNT it, never
      ;; silently drop it, and blobs-seen = denied + git-failures + ingested +
      ;; converged + unresolved must still balance. Prior gates only hit the =0 floor.
      ;; ===================================================================
      (testing "F4 — a dropped OC decision is COUNTED as :blobs-unresolved; the identity holds at non-zero"
        (let [dropped    (atom false)
              real-await ocr/await-object-container-decision]
          ;; Variadic: await-object-container-decision is re-entrant (its 3-arg entry
          ;; resolves keys then re-calls the VAR at 4-arg), so we delegate every arity
          ;; to the real fn and drop ONLY the first 3-arg call — code-sync!'s ingest
          ;; await (runtime.clj:314-320). That one blob's decision returns nil (timeout).
          (with-redefs [ocr/await-object-container-decision
                        (fn [& args]
                          (if (and (= 3 (count args)) (compare-and-set! dropped false true))
                            nil
                            (apply real-await args)))]
            (let [rd (ca/code-sync! {:runtime rt :repo-root repo-root :commit-filter #{c-119f3f8}})]
              (is (true? @dropped) "the fault fired — one decision was dropped")
              (is (= 1 (:blobs-unresolved rd)) "the dropped blob is COUNTED as unresolved, never silent (F4)")
              (is (= (:blobs-seen rd)
                     (+ (:blobs-denied rd) (:git-failures rd) (:blobs-ingested rd)
                        (:blobs-converged rd) (:blobs-unresolved rd)))
                  "F4 identity balances at non-zero: seen = denied + git-fail + ingested + converged + unresolved")))))

      (finally (tv/close-trail-view-runtime! rt)))))

;; =============================================================================
;; Pure lane checks (no runtime) — enumeration oracle + the silver LANE on a
;; synthetic verbatim move (proves the break→silver law independent of git/kernel).
;; =============================================================================
(deftest enumeration-matches-ls-tree-oracle
  (testing "B-full post-image enumeration == the ls-tree union oracle (P0 §c pinned 895)"
    (let [blocks (ca/read-code-log repo-root)
          enum (into #{} (comp (mapcat :changes)
                               (filter #(and (ca/code-path? (:path %))
                                             (not= (:new-sha %) "0000000000000000000000000000000000000000")))
                               (map :new-sha))
                     blocks)
          oracle (ca/ls-tree-blob-oracle repo-root)]
      (is (= enum oracle) "every reachable code blob enumerated; nothing spurious"))))

(deftest silver-lane-on-synthetic-move
  (testing "commit-lineage: a function moved VERBATIM across files → silver edge, never mech"
    (let [foo "(defn foo [x] (+ x 1))"
          ns-a (str "(ns a)\n" foo "\n")
          ns-b-old "(ns b)\n(def other 1)\n"
          ns-b-new (str "(ns b)\n(def other 1)\n" foo "\n")   ; foo arrives VERBATIM in b
          cut (fn [sha] (ca/cut-named-units sha
                                            (case sha
                                              "a-old" ns-a         ; a had foo
                                              "a-new" "(ns a)\n"   ; a lost foo
                                              "b-old" ns-b-old
                                              "b-new" ns-b-new)))
          block {:sha "child" :committer-ms 1000
                 :changes [{:old-sha "a-old" :new-sha "a-new" :status "M" :path "src/a.clj"}
                           {:old-sha "b-old" :new-sha "b-new" :status "M" :path "src/b.clj"}]}
          plan (ca/commit-lineage block cut (constantly false))
          uid-in (fn [sha] (get-in (cut sha) [:units "foo" :unit-id]))]   ; from synthetic text, not git
      (is (= 1 (count (:silver-edges plan))) "exactly one hash-exact move detected")
      (is (empty? (:mech-edges plan)) "a cross-file move is NEVER a mech edge")
      (let [e (first (:silver-edges plan))]
        (is (str/starts-with? (:note e) "clj-atoms-v1|silver-move|hash-exact|") "silver marker, not mech")
        (is (= (uid-in "b-new") (:from-uid e)) "from = the appeared (new) form in b")
        (is (= (uid-in "a-old") (:to-uid e)) "to = the vanished (old) form in a")))))

;; =============================================================================
;; G-F1 (GATE_REVIEW 2026-07-09) — a committed-BROKEN blob (history holds
;; unparseable states: 5/895 in this repo) degrades to a 0-unit cut with
;; :parse-error carried through, so a full-history sync never aborts and lineage
;; sees empty name maps (no fake edges). Pre-fix, clojure-form-v0 threw and the
;; whole code-sync! died on the first broken historical blob.
;; =============================================================================
(deftest gf1-unparseable-blob-degrades-loud
  (testing "cut-named-units on reader-hostile text → zero units + :parse-error, no throw"
    (let [cut (ca/cut-named-units "sha-broken" "(ns b)\n(defn f [x] (inc x)))\n")]
      (is (= {} (:units cut)) "no named units from a broken blob")
      (is (some? (:parse-error cut)) "the parse failure is CARRIED, never swallowed"))))

;; =============================================================================
;; F5 (DIFF_FALSIFICATION) — a git failure is LOUD, never a silent empty blob.
;; =============================================================================
(deftest f5-git-failure-is-loud
  (testing "cat-file on a non-existent sha THROWS (pre-fix: returned empty → silent 0-unit cut)"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"git cat-file exited"
                          (ca/blob-text repo-root "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef"))
        "blob-text on a bad sha surfaces the non-zero git exit as an ex-info")))

;; =============================================================================
;; F6 (DIFF_FALSIFICATION) — deny is unified PER-PATH across ingest + lineage:
;; a blob at a denied path AND an allowed path is ingestable (via the allowed
;; path), matching commit-lineage's per-change-path denial. Pure (no IPC).
;; =============================================================================
(def ^:private zero-sha "0000000000000000000000000000000000000000")
(defn- deny-env? [p] (str/ends-with? (str p) "env.clj"))

(deftest f6-deny-granularity-unified-per-path
  (testing "a blob at BOTH a denied and an allowed path is INGESTABLE via the allowed path"
    (let [S "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa"
          block {:sha "child" :committer-ms 5000
                 :changes [{:old-sha zero-sha :new-sha S :status "A" :path "src/app/server/env.clj"}   ; denied
                           {:old-sha zero-sha :new-sha S :status "A" :path "src/app/foo.clj"}]}         ; allowed, SAME blob
          blob-map (#'ca/enumerate-blobs [block] deny-env?)]
      (is (contains? blob-map S) "the blob is enumerated (counted in :blobs-seen)")
      (is (false? (:denied? (blob-map S))) "NOT denied — per-path, it has an allowed-path occurrence")
      (is (= 5000 (:min-ms (blob-map S))) "clock rides the allowed occurrence")))
  (testing "a blob ONLY at a denied path is denied (no allowed occurrence, nil clock)"
    (let [T "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"
          block {:sha "child" :committer-ms 5000
                 :changes [{:old-sha zero-sha :new-sha T :status "A" :path "src/app/server/env.clj"}]}
          blob-map (#'ca/enumerate-blobs [block] deny-env?)]
      (is (true? (:denied? (blob-map T))) "denied — no allowed path")
      (is (nil? (:min-ms (blob-map T))) "no allowed clock")))
  (testing "commit-lineage denies the SAME per-path: a change only at a denied path mints nothing"
    (let [cut (fn [sha] (ca/cut-named-units sha (case sha
                                                  "old" "(ns a)\n(def bar 1)\n"
                                                  "new" "(ns a)\n(def bar 2)\n")))    ; bar changed → would-be mech
          block {:sha "child" :committer-ms 1000
                 :changes [{:old-sha "old" :new-sha "new" :status "M" :path "src/app/server/env.clj"}]}
          plan (ca/commit-lineage block cut deny-env?)]
      (is (empty? (:mech-edges plan)) "denied-path change mints no mech edge — ingest+lineage now AGREE")
      (is (= 0 (:re-addressed plan)) "and re-addresses nothing (the change never enters the lineage lane)"))))

;; =============================================================================
;; Phase P3 — analyzer lane (CONTRACT §8 G7/G8/G10, §12 P3). G8 is a PURE registry
;; check (no launch). G7 (real kondo at HEAD) + G10 (version-free retraction, via
;; :desired-override) share ONE IPC launch on disjoint keys, with a shared
;; cumulative microbatch barrier — the P2 discipline (deterministic barrier;
;; physical PState readers for the negatives; committer clocks only).
;; =============================================================================

;; ── G8: the ONE authorized registry edit (relation_kernel.clj:58-65) ─────────
(deftest g8-registry-additive
  (testing "G8 — :requires/:calls registered; the closed set still rejects the unregistered"
    (is (rk/registered-kind? :requires) ":requires joined the registry (code-atom CONTRACT §2)")
    (is (rk/registered-kind? :calls)    ":calls joined the registry")
    (is (not (rk/registered-kind? :relates-to))
        "registry stays CLOSED — an unregistered kind is still rejected (guard against kind mush)")
    ;; the block-kernel package's three kinds are NOT this package's concern (T11);
    ;; this package added EXACTLY two. Prove the stance kinds survived (no clobber).
    (doseq [k [:confirms :refutes :supersedes :based-on]]
      (is (rk/registered-kind? k) (str "pre-existing kind " k " untouched by the additive edit")))))

;; ── P2 (DIFF_FALSIFICATION) — the skip counter is pinned at a NON-ZERO value, not
;;    only at the =0 floor G7 exercises. Pure over derive-desired-edges with a
;;    synthetic clj-kondo analysis (stable — no HEAD drift, no kondo/kernel). Proves
;;    that an unmappable var-usage is COUNTED in :usages-unmapped, never silently
;;    dropped (the whole-tree 279 lives only in P3 prose; this gates the CLASS). ────
(deftest p2-skip-count-pinned-nonzero
  (testing "unmappable var-usages are COUNTED in :usages-unmapped, not silently dropped"
    (let [analysis {:namespace-usages
                    [{:from 'app.a :to 'app.b :row 1 :filename "src/app/a.clj"}]     ; 1 requires (app→app)
                    :var-usages
                    ;; nil :from-var (top-level usage, no caller continuant) → unmapped
                    [{:from 'app.a :to 'app.b        :name 'foo :from-var nil       :row 3 :filename "src/app/a.clj"}
                     ;; maps to NO enclosing top-level unit (empty units ctx) → unmapped
                     {:from 'app.a :to 'app.b        :name 'bar :from-var 'caller-x :row 9 :filename "src/app/a.clj"}
                     ;; non-app :to → dropped by the T5 allowlist (a different counter)
                     {:from 'app.a :to 'clojure.core :name 'str :from-var 'caller-y :row 4 :filename "src/app/a.clj"}
                     ;; non-app :from → ignored entirely (defensive; not even 'seen')
                     {:from 'other.x :to 'app.b      :name 'zzz :from-var 'w        :row 2 :filename "x"}]}
          ;; ctx with NO units → every mappable usage resolves to no enclosing unit.
          ctx-fn (fn [_] {:object-key "ok" :source-id "src" :units [] :line-starts [0]})
          {:keys [stats]} (ca/derive-desired-edges analysis ctx-fn "/tmp/none" "note")]
      (is (= {:ns-usages-seen 1 :var-usages-seen 3 :var-usages-dropped-nonapp 1 :usages-unmapped 2}
             stats)
          "skip counters pinned NON-ZERO: 2 unmapped (nil from-var + no enclosing unit), 1 dropped")
      (is (pos? (:usages-unmapped stats)) "the non-zero skip path is exercised, not just the =0 floor (G7)"))))

;; ── ground truth (pinned once, DERIVED from relation_kernel.clj@HEAD via the REAL
;;    clj-kondo run — P0 §B.1/§B.2, reconfirmed in PHASE_P3.md) ─────────────────
(def ^:private c-f6257a9 "f6257a9c728e342e22cb8afbe11d0f4cdec70618")   ; last-touch object_container.clj
(def ^:private rk-path "src/app/server/rama/relation_kernel.clj")
(def ^:private rk-ns "app.server.rama.relation-kernel")
(def ^:private oc-ns "app.server.rama.object-container")
;; relation-kernel's 7 ns dependencies (P0 §B.1). NOTE: :requires targets include
;; NON-app namespaces — the app.* allowlist is :calls-only (SPEC §5.2 covers ALL deps).
(def ^:private rk-requires
  ["com.rpl.rama" "com.rpl.rama.path" "com.rpl.rama.ops" "com.rpl.rama.aggs"
   "com.rpl.rama.test" "app.server.rama.object-container" "clojure.string"])
;; pinned :calls ground truth [from-var to-name representative-call-row] (P0 §B.2).
(def ^:private rk-calls-ground-truth
  [["target-sort-key" "fixed-width-order-key" 167]
   ["relation-outcome" "fixed-width-order-key" 457]   ; 457 + 479 collapse to ONE edge (T6)
   ["->target-ref"     "extract-object-key"   843]])

(defn- statuses [history] (mapv :relation-status history))

;; analyzer-sync! stat map for a :desired-override reconcile (no kondo derivation):
;; the derivation stats are all zero; only the reconcile quad + basis-missing vary.
(defn- ana-stats [calls-asserted retracted converged basis-missing]
  {:ns-usages-seen 0 :var-usages-seen 0 :var-usages-dropped-nonapp 0 :usages-unmapped 0
   :git-failures 0 :requires-asserted 0 :calls-asserted calls-asserted :retracted retracted
   :converged converged :reconcile-basis-missing basis-missing :unresolved-residual 0})

(defn- ana-req-rid [dep]
  (rk/relation-id-for :requires (rk/->target-ref ca/ns-target-kind rk-ns)
                      (rk/->target-ref ca/ns-target-kind dep) ca/analyzer-asserter-actor-id))
(defn- ana-call-rid [from-q to-q]
  (rk/relation-id-for :calls (rk/->target-ref ca/var-target-kind from-q)
                      (rk/->target-ref ca/var-target-kind to-q) ca/analyzer-asserter-actor-id))

(deftest analyzer-gates
  (let [rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})
        cumulative (atom 0)
        drain! (fn [] (rtest/wait-for-microbatch-processed-count (:ipc rt) (:module-name rt) rel-topo @cumulative 60000))
        ;; PIN the analyzer head to the specimen commit (like G5/G6 pin fixed commits;
        ;; T3 "analyze the blob, not the checkout"). All ground truth below — call rows
        ;; 167/457/843, kondo stats, census — was derived from rk@a002c89, i.e.
        ;; relation_kernel.clj@c-63202b0. A dynamic resolve-head-sha silently went stale
        ;; when this package's OWN registry/kernel commit re-touched relation_kernel.clj +
        ;; object_container.clj, moving HEAD's blobs off the ingested/pinned specimen (a
        ;; green-pre-commit / red-post-commit trap caught by the close-session recheck,
        ;; 2026-07-09). :head-override on the G7 sync below makes this stable forever.
        head c-63202b0
        rk-head-sha (ca/head-blob-sha repo-root head rk-path)
        rk-text (ca/blob-text repo-root rk-head-sha)
        line-starts (ca/line-start-offsets rk-text)]
    (try
      ;; ═══════════════════════════════════════════════════════════════════
      ;; INGEST — code-sync! scoped to the two HEAD-blob last-touch commits so
      ;; relation_kernel.clj@HEAD + object_container.clj@HEAD are stored (their
      ;; source-ids/anchors must resolve for the evidence joins, CONTRACT §5 order).
      ;; ═══════════════════════════════════════════════════════════════════
      (let [cs (ca/code-sync! {:runtime rt :repo-root repo-root
                               :commit-filter #{c-63202b0 c-f6257a9}})]
        (swap! cumulative + (:supersedes-mech cs) (:supersedes-silver cs))
        (drain!)
        (is (some? (ocr/read-source rt (oc/source-id-for-object-key
                                        (oc/object-key-for (str "git-blob:" rk-head-sha)
                                                           (oc/source-hash rk-text)))))
            "relation_kernel.clj@HEAD ingested (evidence source present)"))

      ;; ═══════════════════════════════════════════════════════════════════
      ;; G7 — analyzer floor: REAL clj-kondo at HEAD, scoped to the specimen.
      ;; ═══════════════════════════════════════════════════════════════════
      (let [as (ca/analyzer-sync! {:runtime rt :repo-root repo-root :head-override head
                                   :path-filter #(= % rk-path)})]
        (swap! cumulative + (:requires-asserted as) (:calls-asserted as) (:retracted as))
        (drain!)

        (testing "G7 — stats exact (whole derivation pinned; no silent caps)"
          (is (= {:ns-usages-seen 7 :var-usages-seen 837 :var-usages-dropped-nonapp 695
                  :usages-unmapped 0 :git-failures 0 :requires-asserted 7 :calls-asserted 115
                  :retracted 0 :converged 0 :reconcile-basis-missing 1 :unresolved-residual 5}
                 as)
              "7 requires, 115 continuant calls, 0 unmapped, 5 residual; F5 no git failures, F1 no basis on a first sync"))

        (testing "G7 — :requires edge for each of the specimen's 7 ns dependencies (§5.2: non-app targets counted)"
          (doseq [dep rk-requires]
            (let [row (rk/read-relation-row rt (ana-req-rid dep))]
              (is (some? row) (str ":requires " rk-ns " -> " dep))
              (is (= :requires (:relation-kind row)) dep)
              (is (= :asserted (:relation-status row)) dep)
              (is (= "import:code-analyzer" (:asserter-actor-id row)) (str dep " version-free asserter (R4)"))
              (is (= (str "clj-atoms-v1|analyzer|" head) (:note row)) (str dep " carries the analyzer note")))))

        (testing "G7 — :calls var→var for the pinned ground truth; evidence anchor resolves + spans the call row"
          (doseq [[from-var to-name call-row] rk-calls-ground-truth]
            (let [from-q   (str rk-ns "/" from-var)
                  to-q     (str oc-ns "/" to-name)
                  row      (rk/read-relation-row rt (ana-call-rid from-q to-q))
                  unit-id  (ca/code-unit-id repo-root rk-head-sha from-var)
                  anchor-id (oc/source-anchor-id unit-id)
                  anchors  (ocr/read-source-anchors rt unit-id)
                  du       (first (filter #(= :derived-unit (:target-kind %)) anchors))
                  off      (nth line-starts (dec call-row))]
              (is (some? row) (str ":calls " from-var " -> " to-name))
              (is (= :calls (:relation-kind row)) from-var)
              (is (= :asserted (:relation-status row)) from-var)
              (is (= anchor-id (:evidence-anchor-id row))
                  (str from-var " evidence-anchor = the calling FORM's anchor (SPEC §5.3)"))
              (is (some? (ocr/read-source rt (:evidence-source-id row)))
                  (str from-var " evidence-source-id resolves to the ingested calling blob"))
              (is (some? du) (str from-var " anchor row resolves"))
              (is (and (<= (long (:start-offset du)) off) (< off (long (:end-offset du))))
                  (str from-var " anchor span contains the call site row " call-row)))))

        (testing "G7 — continuant grain: relation-outcome's 2 fixed-width-order-key call sites = ONE edge (T6)"
          (let [ro-q (str rk-ns "/relation-outcome")
                rows (get (rk/read-relations-for-targets rt [ro-q] [:calls] false) ro-q)
                to-fwok (filter #(= (str oc-ns "/fixed-width-order-key") (:target-id (:to %))) rows)]
            (is (= 1 (count to-fwok)) "the 2 call sites (rows 457,479) collapse to exactly one :calls edge")))

        (testing "G7 — physical negative: NO :calls edge lands on a com.rpl.rama / clojure.core target (T5 allowlist)"
          (doseq [dropped ["com.rpl.rama.ops/explode" "clojure.core/str" "com.rpl.rama/keypath"]]
            (is (empty? (filter #(= :calls (:relation-kind %))
                                (get (rk/read-relations-for-targets rt [dropped] nil true) dropped)))
                (str "dropped target " dropped " carries no :calls edge (never mapped var-usages blindly)")))))

      ;; ═══════════════════════════════════════════════════════════════════
      ;; G10 — retract-on-disappear (R5 current-status), deterministic via
      ;; :desired-override + a caller-owned cluster-scoped :analyzer-basis atom.
      ;; D1 asserts 4 edges; e-ad shares endpoint A with survivors (retracts via the
      ;; target read), e-pq's endpoints P,Q are UNIQUE (retracts ONLY via the basis —
      ;; F1). D4 drops e-pq (both endpoints leave the desired set → invisible to the
      ;; target read); D5 re-adds it at the SAME head (F3 transition-unique key). Real
      ;; HEAD sha as head-override so committer clock + keys are real. Proves R4
      ;; version-free retraction, F1 vanished-endpoint retraction, F3 re-assert.
      ;; ═══════════════════════════════════════════════════════════════════
      (let [basis (atom nil)    ; cluster-scoped, minted here (F1; NO durable file). N3: nil is
                                ; the honest "no prior pass ran" shape — D1 asserts
                                ; :reconcile-basis-missing 1; an empty {} would falsely read as a
                                ; pass that ran and desired zero edges (the phantom N3 fixes).
            ana! (fn [desired] (ca/analyzer-sync! {:runtime rt :repo-root repo-root :head-override head
                                                   :desired-override desired :analyzer-basis basis}))
            mk (fn [from to] {:kind :calls
                              :from-ref (rk/->target-ref ca/var-target-kind from)
                              :to-ref   (rk/->target-ref ca/var-target-kind to)
                              :note (str "clj-atoms-v1|analyzer|" head)
                              :evidence-source-id nil :evidence-anchor-id nil})
            a "app.g10/caller" b "app.g10/callee-b" c "app.g10/callee-c" d "app.g10/callee-d"
            p "app.g10/lonely-p" q "app.g10/lonely-q"      ; UNIQUE endpoints (F1: leave HEAD together)
            e-ab (mk a b) e-ac (mk a c) e-ad (mk a d) e-pq (mk p q)
            rid  (fn [e] (rk/relation-id-for (:kind e) (:from-ref e) (:to-ref e) ca/analyzer-asserter-actor-id))
            status  (fn [e] (:relation-status (rk/read-relation-row rt (rid e))))
            history (fn [e] (statuses (:history (rk/read-relation-detail rt (rid e)))))
            step! (fn [desired] (let [s (ana! desired)]
                                  (swap! cumulative + (:requires-asserted s) (:calls-asserted s) (:retracted s))
                                  (drain!) s))]

        (testing "G10 — D1: four edges asserted (basis empty → :reconcile-basis-missing 1)"
          (let [s1 (step! [e-ab e-ac e-ad e-pq])]
            (is (= (ana-stats 4 0 0 1) s1) "4 fresh asserts, nothing retracted/converged, basis absent")
            (doseq [e [e-ab e-ac e-ad e-pq]]
              (is (= :asserted (status e)) "asserted"))))

        (testing "G10 — D2 drops A→D (endpoint A survives): retracts via the target read"
          (let [s2 (step! [e-ab e-ac e-pq])]
            (is (= (ana-stats 0 1 3 0) s2) "e-ad retracted (shares A); the other three converge; basis present")
            (is (= :retracted (status e-ad)) "A→D flipped to :retracted")
            (is (= [:asserted :retracted] (history e-ad)) "history preserved")
            (doseq [e [e-ab e-ac e-pq]]
              (is (= :asserted (status e)) "surviving edge still asserted")
              (is (= [:asserted] (history e)) "converged edge untouched (single transition)"))))

        (testing "G10 — D3 re-run: journal converges, ZERO new events"
          (let [s3 (step! [e-ab e-ac e-pq])]
            (is (= (ana-stats 0 0 3 0) s3) "no re-assert, no re-retract; three converged")
            (is (= [:asserted :retracted] (history e-ad)) "A→D history unchanged")))

        (testing "G10 — D4 (F1) drops P→Q whose endpoints BOTH leave HEAD: retracts via the BASIS"
          ;; e-pq shares NO target-key with the desired [e-ab e-ac], so the target read
          ;; cannot see it — only the cluster-scoped basis remembers it. Pre-F1 it leaked.
          (let [s4 (step! [e-ab e-ac])]
            (is (= (ana-stats 0 1 2 0) s4) "e-pq retracted via the basis-diff (F1); e-ab/e-ac converge")
            (is (= :retracted (status e-pq)) "P→Q flipped to :retracted though both endpoints left the desired set")
            (is (= [:asserted :retracted] (history e-pq)) "history preserved")))

        (testing "G10 — D5 (F3) re-adds P→Q at the SAME head: re-asserts via a transition-unique key"
          ;; pre-F3 the re-assert key == the original assert key → journal converge →
          ;; e-pq would STAY :retracted while the stat claimed +1. The t<N> suffix fixes it.
          (let [s5 (step! [e-ab e-ac e-pq])]
            (is (= (ana-stats 1 0 2 0) s5) "e-pq re-asserted (fresh key at fixed head, F3); e-ab/e-ac converge")
            (is (= :asserted (status e-pq)) "P→Q flipped back to :asserted (the re-assert LANDED)")
            (is (= [:asserted :retracted :asserted] (history e-pq)) "three transitions — every one a fresh journal entry")))

        ;; ── N3 (GATE_REVIEW 2026-07-09) — basis PRESENT-but-empty ≠ basis MISSING ──
        ;; A pass that desires zero edges retracts everything (via the basis) and stores {}
        ;; as the next basis. The FOLLOWING pass must read {} as a real prior pass and report
        ;; :reconcile-basis-missing 0, NOT 1 — pre-fix (empty? prior) reported a phantom 1.
        (testing "G10 — N3/D6: a zero-edge desired pass retracts all via the basis (basis present, 0)"
          (let [s6 (step! [])]
            (is (= (ana-stats 0 3 0 0) s6)
                "e-ab/e-ac/e-pq all retracted via the basis-diff; the basis WAS present (0), not missing")
            (doseq [e [e-ab e-ac e-pq]]
              (is (= :retracted (status e)) "every edge retracted after the zero-edge pass"))))

        (testing "G10 — N3/D7: the pass AFTER a zero-edge pass reads the empty {} basis as PRESENT"
          (let [s7 (step! [])]
            (is (= (ana-stats 0 0 0 0) s7)
                "empty desired + empty ({}) prior basis: nothing to do AND basis NOT missing (nil? not empty?)")
            (is (= 0 (:reconcile-basis-missing s7))
                "a prior pass that legitimately desired zero edges is STILL a prior pass — phantom 1 gone"))))

      ;; ═══════════════════════════════════════════════════════════════════
      ;; (c) SETTLE (GATE_REVIEW 2026-07-09 doubt 2) + N5 numeric DISPLAY order.
      ;; Each runs on its OWN basis atom so its single edge stays isolated from the
      ;; G10 set above; both reuse this launch (the minimize-IPC discipline).
      ;; ═══════════════════════════════════════════════════════════════════
      (let [mk  (fn [from to] {:kind :calls
                               :from-ref (rk/->target-ref ca/var-target-kind from)
                               :to-ref   (rk/->target-ref ca/var-target-kind to)
                               :note (str "clj-atoms-v1|analyzer|" head)
                               :evidence-source-id nil :evidence-anchor-id nil})
            rid (fn [e] (rk/relation-id-for (:kind e) (:from-ref e) (:to-ref e) ca/analyzer-asserter-actor-id))]

        ;; (c) — the EXIT settle makes back-to-back syncs safe with NO external
        ;; barrier: sync-1's assert is materialized before it returns, so sync-2
        ;; reads a fresh count and its retract lands. Pre-fix, sync-2 could read the
        ;; not-yet-materialized assert (:row nil) → basis-retract skipped → flip dropped.
        (testing "settle — two immediate syncs flip one edge, NO drain between, final = last desired"
          (let [basis-s (atom nil)
                ana-s! (fn [d] (ca/analyzer-sync! {:runtime rt :repo-root repo-root :head-override head
                                                   :desired-override d :analyzer-basis basis-s}))
                e-st (mk "app.settle/from" "app.settle/to")
                r-st (rid e-st)]
            (ana-s! [e-st])    ; assert — self-settles internally
            (ana-s! [])        ; retract, IMMEDIATELY — no external barrier between the two calls
            (is (= :retracted (:relation-status (rk/read-relation-row rt r-st)))
                "the retract LANDED with no external drain — the exit settle materialized the assert")
            (is (= [:asserted :retracted]
                   (statuses (:history (rk/read-relation-detail rt r-st))))
                "exactly two transitions — the retract was NOT dropped by a stale-count race")
            (swap! cumulative + 2) (drain!)))   ; 2 transitions appended; keep the barrier honest

        ;; N5 — >=10 transitions at a FIXED head: the raw status-log order-key sorts
        ;; request-ids LEXICALLY (t10 before t2); relation-history-display re-sorts
        ;; numeric. Count + authoritative current-status row are untouched.
        (testing "N5 — >=10 transitions DISPLAY numeric (t2 before t10); count + current status unchanged"
          (let [basis-n (atom nil)
                step-n! (fn [d] (let [s (ca/analyzer-sync! {:runtime rt :repo-root repo-root :head-override head
                                                            :desired-override d :analyzer-basis basis-n})]
                                  (swap! cumulative + (:requires-asserted s) (:calls-asserted s) (:retracted s))
                                  (drain!) s))
                e-n5 (mk "app.n5/from" "app.n5/to")
                r-n5 (rid e-n5)
                idx  (fn [h] (vec (keep #(#'ca/reconcile-transition-index (:request-id %)) h)))]
            (dotimes [i 11] (step-n! (if (even? i) [e-n5] [])))   ; assert t0, retract t1, … assert t10
            (let [detail  (rk/read-relation-detail rt r-n5)
                  raw     (:history detail)
                  display (ca/relation-history-display rt r-n5)]
              (is (= 11 (count raw)) "11 transitions recorded (t0..t10)")
              (is (= 11 (count display)) "display preserves the count — DISPLAY-only reorder (N5)")
              (is (= (vec (range 11)) (idx display))
                  "display order is NUMERIC: t0,t1,...,t10 — t2 precedes t10")
              (is (not= (vec (range 11)) (idx raw))
                  "the RAW status-log order is lexical (t10 before t2) — the fix genuinely reorders")
              (is (= :asserted (:relation-status (:row detail)))
                  "authoritative current status unchanged (the last flip was an assert, t10)")
              (is (= (:relation-status (:row detail)) (:relation-status (last display)))
                  "current status == the last DISPLAY transition")))))

      (finally (tv/close-trail-view-runtime! rt)))))
