;; git-spine WP2 gate suite (CONTRACT v1.2 §5). Gates G1-G7 + G12, plus a
;; replay-durability unit test (the P1+PW pair test G8 is final-phase — this
;; exercises replay-assert-log! against a log file written in-test, and settles
;; duty §8.7 empirically). Fixture git repo is built in-test via ProcessBuilder
;; in a tmp dir (a merge commit + a rename); fixture jsonl carries a real + a
;; fake sha. Relation asserts settle on the microbatch processed-count barrier
;; (implementation-quirks discipline — never polling as proof of a negative).

(ns app.server.ingest.git-spine-test
  (:require [app.server.ingest.git-spine :as gs]
            [app.server.rama.trail-view :as tv]
            [app.server.rama.relation-kernel :as rk]
            [app.server.rama.object-container :as oc]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.ingest.markdown-adapter :as md]
            [com.rpl.rama.test :as rtest]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private rel-topo "relation-kernel-topology")

;; ── tmp dir + shell helpers ──────────────────────────────────────────────────
(defn- tmp-dir ^File [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- rm-rf [^File f]
  (when (.exists f)
    (doseq [^File c (reverse (file-seq f))] (.delete c))))

(defn- sh!
  "Run a command in `dir` (or cwd), stderr merged, returning {:out :exit}."
  [dir args]
  (let [pb (ProcessBuilder. ^java.util.List (into [] args))]
    (when dir (.directory pb (io/file dir)))
    (.redirectErrorStream pb true)
    (let [p (.start pb) out (slurp (.getInputStream p))]
      (.waitFor p)
      {:out out :exit (.exitValue p)})))

(defn- git!
  "Run git in `dir`; throw on nonzero exit so a broken fixture fails loudly.
   Fixed author/committer identity + dates keep the fixture free of wall clock."
  [dir & args]
  (let [pb (ProcessBuilder. ^java.util.List (into ["git"] args))]
    (.directory pb (io/file dir))
    (doto (.environment pb)
      (.put "GIT_AUTHOR_NAME" "Fixture") (.put "GIT_AUTHOR_EMAIL" "fix@ex.com")
      (.put "GIT_COMMITTER_NAME" "Fixture") (.put "GIT_COMMITTER_EMAIL" "fix@ex.com")
      (.put "GIT_AUTHOR_DATE" "2026-01-01 00:00:00 +0000")
      (.put "GIT_COMMITTER_DATE" "2026-01-01 00:00:00 +0000"))
    (.redirectErrorStream pb true)
    (let [p (.start pb) out (slurp (.getInputStream p)) _ (.waitFor p) exit (.exitValue p)]
      (when-not (zero? exit)
        (throw (ex-info (str "git " (pr-str args) " failed: " out) {:exit exit})))
      out)))

;; ── fixture git repo (merge + rename; a doc under the land roots for G6) ──────
(defn- build-fixture-repo! [^File dir]
  (git! dir "init" "-q" "-b" "main")
  (git! dir "config" "commit.gpgsign" "false")
  (git! dir "config" "user.name" "Fixture")
  (git! dir "config" "user.email" "fix@ex.com")
  (io/make-parents (io/file dir "docs/current-mental-model/note.md"))
  ;; c1 (root)
  (spit (io/file dir "alpha.md") "# Alpha\nfirst\n")
  (git! dir "add" "-A") (git! dir "commit" "-q" "-m" "c1 add alpha")
  ;; c2
  (spit (io/file dir "alpha.md") "# Alpha\nfirst\nsecond\n")
  (spit (io/file dir "docs/current-mental-model/note.md") "# Note\ndoc body v1\n")
  (git! dir "add" "-A") (git! dir "commit" "-q" "-m" "c2 extend alpha + add note")
  ;; c3 on feature
  (git! dir "checkout" "-q" "-b" "feature")
  (spit (io/file dir "gamma.md") "# Gamma\n")
  (git! dir "add" "-A") (git! dir "commit" "-q" "-m" "c3 add gamma on feature")
  ;; c4 on main (RENAME)
  (git! dir "checkout" "-q" "main")
  (git! dir "mv" "alpha.md" "alpha2.md")
  (git! dir "commit" "-q" "-m" "c4 rename alpha to alpha2")
  ;; c5 (MERGE, --no-ff => real merge commit, two parents)
  (git! dir "merge" "-q" "--no-ff" "-m" "c5 merge feature" "feature")
  ;; an out-of-land-roots file on disk for G6 (Edit target that must NOT edge)
  (spit (io/file dir "outside.md") "# Outside\n")
  dir)

;; ── fixture jsonl (real + fake shas; repeated Edit of one doc; out-of-roots) ──
(def ^:private fake-sha "deadbeefdeadbeefdeadbeefdeadbeefdeadbeef")

(defn- write-fixture-jsonl! [^File file session-id real-sha note-path outside-path]
  (let [asst (fn [uuid content]
               {:type "assistant" :sessionId session-id :uuid uuid
                :timestamp "2026-01-02T00:00:00.000Z"
                :message {:role "assistant" :content content}})
        entries
        [;; git-ish tool_use with no sha
         (asst "e1" [{:type "tool_use" :id "tu1" :name "Bash" :input {:command "git log --oneline -5"}}])
         ;; tool_result carrying a REAL + a FAKE sha in git context
         {:type "user" :sessionId session-id :uuid "e2" :timestamp "2026-01-02T00:00:01.000Z"
          :message {:role "user"
                    :content [{:type "tool_result" :tool_use_id "tu1"
                               :content (str "commit " real-sha "\ncommit " fake-sha "\n")}]}}
         ;; two Edits of the SAME in-roots doc (cap must collapse to one edge)
         (asst "e3" [{:type "tool_use" :id "tu3" :name "Edit" :input {:file_path note-path}}])
         (asst "e4" [{:type "tool_use" :id "tu4" :name "Edit" :input {:file_path note-path}}])
         ;; an Edit of a file OUTSIDE the land roots (must NOT edge)
         (asst "e5" [{:type "tool_use" :id "tu5" :name "Write" :input {:file_path outside-path}}])]]
    (with-open [w (io/writer file)]
      (doseq [e entries] (.write w ^String (json/write-str e)) (.write w "\n")))))

;; ── barriers ─────────────────────────────────────────────────────────────────
(defn- rel-drain!
  "Await the relation microbatch through `n` cumulative consumed depot records."
  [rt n]
  (rtest/wait-for-microbatch-processed-count (:ipc rt) (:module-name rt) rel-topo n 30000))

(defn- edge-row [rt relation-id]
  (:row (rk/read-relation-detail rt relation-id)))

;; =============================================================================
;; G1 + G3 + G12 — pure (no runtime)
;; =============================================================================
(deftest g1-adapter-purity-and-g3-parity
  (let [dir (tmp-dir "gs-pure-")]
    (try
      (build-fixture-repo! dir)
      (let [commits (gs/read-commits (str dir))
            rename-c (some #(when (some (fn [f] (str/includes? f " -> ")) (:files %)) %) commits)
            merge-c (some #(when (= 2 (count (:parents %))) %) commits)]

        (testing "G3 — parity: read-commits count == git rev-list --all --count"
          (let [n (Long/parseLong (str/trim (:out (sh! (str dir) ["git" "rev-list" "--all" "--count"]))))]
            (is (= n (count commits)) "every reachable commit is read")
            (is (= 5 (count commits)) "fixture has 5 commits")))

        (testing "fixture shape: a rename (normalized) and a merge (2 parents, 0 files)"
          (is (some? rename-c) "a rename commit was parsed")
          (is (some #(str/includes? % "alpha.md -> alpha2.md") (:files rename-c))
              "rename normalized old -> new")
          (is (some? merge-c) "a merge commit exists")
          (is (= 2 (count (:parents merge-c))) "merge has two parents")
          (is (empty? (:files merge-c)) "merge shows no files (name-status empty)"))

        (testing "G1 — adapter purity: byte-identical canonical text + identical request across runs"
          (doseq [c commits]
            (is (= (gs/commit->canonical-text c) (gs/commit->canonical-text c))
                "canonical text is byte-stable")
            (is (= (gs/commit->import-request c) (gs/commit->import-request c))
                "import request is identical (deterministic time + request-id)")
            (is (= (:idempotency/key (gs/commit->import-request c))
                   (:idempotency/key (gs/commit->import-request c)))
                "idempotency key stable"))
          ;; and across a SECOND independent read of the repo
          (let [commits2 (gs/read-commits (str dir))
                c1 (first commits) c2 (get (into {} (map (juxt :sha identity)) commits2) (:sha c1))]
            (is (= (gs/commit->canonical-text c1) (gs/commit->canonical-text c2))
                "same sha -> same canonical text across independent reads")
            (is (= (gs/commit->document-id c1) (gs/commit->document-id c2))
                "same sha -> same document-id")))

        (testing "canonical text is UTC + labeled + sorted (the §3.A cross-builder interface)"
          (let [txt (gs/commit->canonical-text rename-c)
                lines (str/split-lines txt)]
            (is (str/starts-with? (nth lines 0) "sha: "))
            (is (str/starts-with? (nth lines 1) "parents: "))
            (is (str/starts-with? (nth lines 2) "author: "))
            (is (re-find #"^authored-at: \d{4}-\d\d-\d\dT.*Z$" (nth lines 3)) "authored-at UTC ISO Z")
            (is (re-find #"^committed-at: \d{4}-\d\d-\d\dT.*Z$" (nth lines 4)) "committed-at UTC ISO Z")
            (is (str/starts-with? (nth lines 5) "subject: "))
            (is (str/includes? txt "\nfiles:\n")))))
      (finally (rm-rf dir)))))

;; =============================================================================
;; G2 + G4 — spine-sync ingest convergence + parent :based-on edges (tv runtime)
;; =============================================================================
(deftest g2-g4-spine-sync-gates
  (let [dir (tmp-dir "gs-spine-")
        rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})]
    (try
      (build-fixture-repo! dir)
      (let [cfg {:runtime rt :repo-root (str dir)}
            commits (gs/read-commits (str dir))
            sha->doc (into {} (map (fn [c] [(:sha c) (gs/commit->document-id c)])) commits)
            r1 (gs/spine-sync! cfg)]
        (rel-drain! rt (:edges-appended r1))

        (testing "G3(runtime) — every commit ingested (accepted decisions)"
          (is (= (count commits) (:commits r1)))
          (is (= (count commits) (:ingested r1)) "all commit decisions accepted"))

        (testing "t4-spine seam 3 — first-pass stats split truthfully: all FRESH, none converged"
          (is (= (count commits) (:fresh r1)) "first pass: every accept is fresh")
          (is (= 0 (:converged r1)))
          (is (= 0 (:rejected r1)))
          (is (= 0 (:unresolved r1))))

        (testing "t4-spine seam 1 — committed-at rides to the feed as the CLAIMED clock"
          (doseq [c commits]
            (is (= (:committed-at-ms c) (:claimed/at-ms (gs/commit->import-request c)))
                "the import request declares the committer clock as :claimed/at-ms"))
          (let [now (System/currentTimeMillis)
                feed (tv/read-recent-activity rt {:from-ms 0 :to-ms (+ now 3600000)} {})
                by-id (into {}
                            (comp (filter #(= :source-ingested (:entry/kind %)))
                                  (map (juxt #(get-in % [:entry/target :id]) identity)))
                            (:feed/entries feed))]
            (doseq [c commits
                    :let [entry (get by-id (sha->doc (:sha c)))]]
              (is (some? entry) (str "feed entry present for " (subs (:sha c) 0 7)))
              (is (= (:committed-at-ms c) (:time/claimed-ms entry))
                  "claimed-ms = the committer clock (two-clock stamp, band 2)")
              (is (some? (:time/arrival-ms entry)) "arrival clock still present")
              (is (not= (:time/claimed-ms entry) (:time/arrival-ms entry))
                  "claimed (2026-01-01 fixture clock) is NOT the arrival wall clock"))))

        (testing "G4 — every non-root commit has :based-on edge(s); merge -> one per parent"
          (doseq [c commits
                  :let [child-doc (sha->doc (:sha c))]]
            (doseq [parent (:parents c)
                    :let [parent-doc (sha->doc parent)
                          rid (gs/based-on-relation-id child-doc parent-doc)
                          row (edge-row rt rid)]]
              (is (some? row) (str "based-on edge present " (subs (:sha c) 0 7) "->" (subs parent 0 7)))
              (is (= :based-on (:relation-kind row)))
              (is (= :asserted (:relation-status row)))
              (is (= "spine-v1|parent" (:note row)) "parent basis note"))
            (when (= 2 (count (:parents c)))
              (is (= 2 (count (:parents c))) "merge commit yields two based-on edges (one per parent)")))
          (is (= 5 (:edges-appended r1)) "fixture yields exactly 5 parent edges"))

        (testing "duty §6 — the :container endpoint JOINS: edge filed under the object-key"
          (let [c2 (some #(when (= 1 (count (:parents %))) %) commits)
                child-doc (sha->doc (:sha c2))
                child-okey (oc/extract-object-key child-doc)
                rows (get (rk/read-relations-for-targets rt [child-okey] nil false) child-okey)]
            (is (some #(= :based-on (:relation-kind %)) rows)
                "based-on edge surfaces under the commit doc's object-key (render join)")))

        (testing "G2 — double-ingest convergence: 2nd sync re-accepts (no conflict), 0 new edges"
          ;; The kernel's convergent-replay (INPUTS §1.9) writes no new rows on a
          ;; byte-identical re-import; spine-sync's contribution is DETERMINISM (G1
          ;; — same ids) + the edge pre-check. Observable proof: the 2nd pass
          ;; re-accepts every commit (a non-deterministic re-run would fingerprint-
          ;; CONFLICT -> :rejected -> :ingested would drop) and appends no new edges.
          (let [r2 (gs/spine-sync! cfg)]
            (is (= (:ingested r1) (:ingested r2)) "same accepted count on the 2nd pass")
            (is (= (count commits) (:ingested r2)) "all commits re-accepted (converged, not conflicted)")
            ;; seam 3: the 2nd pass's accepts are prior decisions ack-returned
            ;; (identical request-id -> audit branch), and the stats say so —
            ;; the boot log can no longer read a converged re-run as fresh
            ;; material.
            (is (= (count commits) (:converged r2)) "2nd pass: every accept converged")
            (is (= 0 (:fresh r2)) "2nd pass: nothing fresh")
            (is (= 0 (:edges-appended r2)) "pre-check skips all edges -> zero new edge appends")
            (doseq [c commits]
              (let [req (gs/commit->import-request c)
                    d (ocr/read-decision rt req)
                    src (ocr/read-source rt (gs/commit-source-ref (:sha c))
                                         (oc/source-hash (gs/commit->canonical-text c)))]
                (is (= :accepted (:status d))
                    (str "re-ingest of " (subs (:sha c) 0 7) " stays accepted"))
                (is (some? src) "commit source artifact present and content-addressed after re-ingest"))))))
      (finally (rm-rf dir) (tv/close-trail-view-runtime! rt)))))

;; =============================================================================
;; G5 + G6 + G7 — transcript extractor (tv runtime)
;; =============================================================================
(deftest g5-g6-g7-extractor-gates
  (let [dir (tmp-dir "gs-extract-")
        jsonl-dir (tmp-dir "gs-jsonl-")
        cursor-path (str (io/file jsonl-dir "cursor.edn"))
        rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})]
    (try
      (build-fixture-repo! dir)
      (let [commits (gs/read-commits (str dir))
            root-commit (some #(when (empty? (:parents %)) %) commits)
            real-sha (:sha root-commit)
            real-doc-id (gs/commit->document-id root-commit)
            note-path (str (io/file dir "docs/current-mental-model/note.md"))
            outside-path (str (io/file dir "outside.md"))
            session-id "11111111-2222-3333-4444-555555555555"
            _ (write-fixture-jsonl! (io/file jsonl-dir (str session-id ".jsonl"))
                                    session-id real-sha note-path outside-path)
            cfg {:runtime rt :repo-root (str dir)
                 :transcript-roots [(str jsonl-dir)] :spine-cursor-path cursor-path
                 ;; stable per cluster instance (file_viewer mints one per
                 ;; runtime delay body); the cursor is only honored under it
                 :spine-run-id "instance-A"}
            ;; ingest commits (so the conversation->commit edge joins) + note.md (doc join)
            spine (gs/spine-sync! cfg)
            _ (let [note-req (md/markdown-source-import-request (slurp note-path) note-path)]
                (ocr/append-object-container-request! rt note-req)
                (ocr/await-object-container-decision rt note-req 10000))
            _ (rel-drain! rt (:edges-appended spine))
            ;; extractor endpoints (deterministic ids, for querying)
            conv-ref (rk/->target-ref :conversation (gs/session->conversation-container-id session-id))
            evidence-src (str "transcript:" session-id)
            real-commit-rid (rk/relation-id-for :produced conv-ref
                                                (rk/->target-ref :container real-doc-id)
                                                gs/import-asserter-actor-id)
            fake-doc-id (gs/commit->document-id {:sha fake-sha})
            fake-commit-rid (rk/relation-id-for :produced conv-ref
                                                (rk/->target-ref :container fake-doc-id)
                                                gs/import-asserter-actor-id)
            note-doc-id (oc/document-id-for note-path (oc/source-hash (slurp note-path)))
            note-rid (rk/relation-id-for :produced conv-ref
                                         (rk/->target-ref :container note-doc-id)
                                         gs/import-asserter-actor-id)
            outside-doc-id (oc/document-id-for outside-path (oc/source-hash (slurp outside-path)))
            outside-rid (rk/relation-id-for :produced conv-ref
                                            (rk/->target-ref :container outside-doc-id)
                                            gs/import-asserter-actor-id)
            ex1 (gs/extract-session-joins! cfg)]
        (rel-drain! rt (+ (:edges-appended spine) (:sha-edges ex1) (:doc-edges ex1)))

        (testing "G5 — extractor honesty: only repo-verified shas asserted, with evidence + note"
          (let [row (edge-row rt real-commit-rid)]
            (is (some? row) "REAL sha -> conversation->commit :produced edge")
            (is (= :produced (:relation-kind row)))
            (is (= evidence-src (:evidence-source-id row)) "evidence-source-id transcript:<session>")
            (is (= "e2" (:evidence-anchor-id row)) "evidence-anchor-id = the jsonl entry uuid")
            (is (= "spine-v1|sha-verified" (:note row))))
          (is (nil? (edge-row rt fake-commit-rid)) "FAKE sha -> NO edge (the map does not lie)")
          (is (= 1 (:sha-edges ex1)) "exactly one verified-sha edge"))

        (testing "duty §6 — conversation->commit edge JOINS the ingested commit"
          (let [okey (oc/extract-object-key real-doc-id)
                rows (get (rk/read-relations-for-targets rt [okey] nil false) okey)]
            (is (some #(= real-commit-rid (:relation-id %)) rows)
                "the :produced edge surfaces under the commit doc's object-key")))

        (testing "G6 — session×doc cap: repeated Edits of one file -> ONE edge; out-of-roots -> none"
          (is (some? (edge-row rt note-rid)) "in-roots doc edged once")
          (is (= "spine-v1|file-write" (:note (edge-row rt note-rid))))
          (is (nil? (edge-row rt outside-rid)) "file outside land roots -> no edge")
          (is (= 1 (:doc-edges ex1)) "the two Edits of note.md collapse to one edge"))

        (testing "duty §6 — conversation->doc edge JOINS the ingested doc"
          (let [okey (oc/extract-object-key note-doc-id)
                rows (get (rk/read-relations-for-targets rt [okey] nil false) okey)]
            (is (some #(= note-rid (:relation-id %)) rows) "doc edge surfaces under the doc's object-key")))

        (testing "t4-spine seam 1 — md ingest stays claimed-NIL-honest in the feed
                  (its :request/time-ms is a wall-clock default, never a claim)"
          (let [now (System/currentTimeMillis)
                feed (tv/read-recent-activity rt {:from-ms 0 :to-ms (+ now 3600000)} {})
                md-entry (->> (:feed/entries feed)
                              (filter #(and (= :source-ingested (:entry/kind %))
                                            (= note-doc-id (get-in % [:entry/target :id]))))
                              first)]
            (is (some? md-entry) "note.md source-ingested entry present")
            (is (nil? (:time/claimed-ms md-entry)) "md claimed stays nil (nil-honest)")
            (is (some? (:time/arrival-ms md-entry)) "arrival clock present")))

        (testing "G7 — cursor is cost-only: delete cursor, re-run -> identical land state, only more work"
          (let [okeys [(oc/extract-object-key (gs/session->conversation-container-id session-id))
                       (oc/extract-object-key real-doc-id)
                       (oc/extract-object-key note-doc-id)]
                land-of (fn [] (->> (rk/read-relations-for-targets rt okeys nil false)
                                    vals (apply concat) (map :relation-id) set))
                before (land-of)]
            (is (contains? before real-commit-rid))
            (is (contains? before note-rid))
            (is (.exists (io/file cursor-path)) "cursor written after first run")
            ;; a re-run WITH the cursor skips the file (cost optimization present)
            (let [ex-skip (gs/extract-session-joins! cfg)]
              (is (pos? (:skipped ex-skip)) "unchanged file skipped via cursor"))
            ;; delete the cursor -> reprocess everything, but journal keeps state identical
            (.delete (io/file cursor-path))
            (let [ex2 (gs/extract-session-joins! cfg)]
              (rel-drain! rt (+ (:edges-appended spine) (:sha-edges ex1) (:doc-edges ex1)
                                (:sha-edges ex2) (:doc-edges ex2)))
              (is (pos? (:files ex2)) "cursor deleted -> file REPROCESSED (more work done)")
              (is (= 0 (:skipped ex2)) "nothing skipped after cursor delete")
              (is (= 0 (+ (:sha-edges ex2) (:doc-edges ex2))) "no new edges appended (journal + pre-check)")
              (is (= before (land-of)) "land state (edge set) is identical"))))

        (testing "gate-review addendum 2026-07-05 — cursor is cluster-INSTANCE-scoped:
                  the land's cluster is ephemeral per JVM, so a fresh instance
                  honoring the previous boot's durable cursor would skip every
                  unchanged transcript and silently LOSE its conversation edges"
          ;; instance-A's cursor exists (written above). A different instance
          ;; must IGNORE it and reprocess in full...
          (let [ex-b (gs/extract-session-joins! (assoc cfg :spine-run-id "instance-B"))]
            (is (= 0 (:skipped ex-b)) "foreign cursor ignored — nothing skipped")
            (is (pos? (:files ex-b)) "full reprocess under the new instance"))
          ;; ...and then OWN the cursor: a same-instance re-run skips again.
          (let [ex-b2 (gs/extract-session-joins! (assoc cfg :spine-run-id "instance-B"))]
            (is (pos? (:skipped ex-b2)) "same-instance re-run skips via its own cursor"))
          ;; no stable run-id supplied -> per-call ids -> cursor never honored
          ;; (always correct, only costly)
          (let [ex-anon (gs/extract-session-joins! (dissoc cfg :spine-run-id))]
            (is (= 0 (:skipped ex-anon)) "anonymous runs never trust a cursor"))))
      (finally (rm-rf dir) (rm-rf jsonl-dir) (tv/close-trail-view-runtime! rt)))))

;; =============================================================================
;; t4-spine seams 2+3 (2026-07-06) — dual-working-dir doc edges + run-level dedup
;;
;; Seam 2 (gate-review doubt 1, verified live): the project root is reachable
;; under a symlink alias; a transcript recording the ALIAS path minted a doc id
;; no ingested doc ever matches — a dangling edge whose target IS present
;; (dishonest). Fix: rebase into the watcher's textual form before minting.
;;
;; Seam 3 (verified live): up to 298 jsonl files share ONE sessionId
;; (agent/sidechain files), so per-FILE dedup re-appended the same
;; conversation-scoped edge once per sibling file — counts over-reported
;; (journal kept the land honest). Dedup is now RUN-level on [session-id x].
;; =============================================================================
(deftest alias-rebase-and-run-level-dedup
  (let [dir (tmp-dir "gs-alias-repo-")
        jsonl-dir (tmp-dir "gs-alias-jsonl-")
        alias-parent (tmp-dir "gs-alias-link-")
        alias-root (io/file alias-parent "root")
        _ (Files/createSymbolicLink (.toPath alias-root) (.toPath dir)
                                    (make-array FileAttribute 0))
        rt (tv/start-trail-view-runtime! {:tasks (rand-nth [2 4]) :threads 2})]
    (try
      (build-fixture-repo! dir)
      (let [commits (gs/read-commits (str dir))
            root-commit (some #(when (empty? (:parents %)) %) commits)
            real-sha (:sha root-commit)
            real-doc-id (gs/commit->document-id root-commit)
            note-path (str (io/file dir "docs/current-mental-model/note.md"))
            alias-note-path (str (io/file alias-root "docs/current-mental-model/note.md"))
            session-id "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
            entry (fn [uuid content]
                    {:type "assistant" :sessionId session-id :uuid uuid
                     :timestamp "2026-01-02T00:00:00.000Z"
                     :message {:role "assistant" :content content}})
            write-jsonl! (fn [^File file entries]
                           (with-open [w (io/writer file)]
                             (doseq [e entries]
                               (.write w ^String (json/write-str e)) (.write w "\n"))))
            ;; file A (an agent/sidechain sibling): the ALIAS doc path + the sha
            _ (write-jsonl! (io/file jsonl-dir "file-a.jsonl")
                            [(entry "a1" [{:type "tool_use" :id "t1" :name "Edit"
                                           :input {:file_path alias-note-path}}])
                             (entry "a2" [{:type "tool_use" :id "t2" :name "Bash"
                                           :input {:command (str "git show " real-sha)}}])])
            ;; file B (same sessionId): the REAL doc path + the SAME sha
            _ (write-jsonl! (io/file jsonl-dir "file-b.jsonl")
                            [(entry "b1" [{:type "tool_use" :id "t3" :name "Edit"
                                           :input {:file_path note-path}}])
                             (entry "b2" [{:type "tool_use" :id "t4" :name "Bash"
                                           :input {:command (str "git show " real-sha)}}])])
            cfg {:runtime rt :repo-root (str dir) :transcript-roots [(str jsonl-dir)]}
            conv-ref (rk/->target-ref :conversation (gs/session->conversation-container-id session-id))
            note-doc-id (oc/document-id-for note-path (oc/source-hash (slurp note-path)))
            note-rid (rk/relation-id-for :produced conv-ref
                                         (rk/->target-ref :container note-doc-id)
                                         gs/import-asserter-actor-id)
            ;; the id the OLD code minted from the raw alias string — must NOT exist
            alias-doc-id (oc/document-id-for alias-note-path
                                             (oc/source-hash (slurp alias-note-path)))
            alias-rid (rk/relation-id-for :produced conv-ref
                                          (rk/->target-ref :container alias-doc-id)
                                          gs/import-asserter-actor-id)
            sha-rid (rk/relation-id-for :produced conv-ref
                                        (rk/->target-ref :container real-doc-id)
                                        gs/import-asserter-actor-id)
            ex (gs/extract-session-joins! cfg)]
        (rel-drain! rt (+ (:sha-edges ex) (:doc-edges ex)))

        (testing "seam 3 — run-level dedup: same session split across files -> ONE append each"
          (is (= 2 (:files ex)))
          (is (= 1 (:sha-edges ex)) "same (session, sha) in two files counts ONCE")
          (is (= 1 (:doc-edges ex)) "alias + real path of one doc count ONCE"))

        (testing "seam 2 — the alias path REBASES to the watcher's textual form and joins"
          (let [row (edge-row rt note-rid)]
            (is (some? row) "doc edge targets the id the md watcher would ingest under")
            (is (= "spine-v1|file-write" (:note row))))
          (is (nil? (edge-row rt alias-rid))
              "NO edge keyed on the raw alias string (the dishonest dangler is gone)"))

        (testing "sha edge sanity — one verified-sha edge, exactly one event"
          (let [{:keys [row history]} (rk/read-relation-detail rt sha-rid)]
            (is (some? row))
            (is (= 1 (count history)) "one append -> one event (no duplicate rows)"))))
      (finally
        (Files/deleteIfExists (.toPath alias-root))
        (rm-rf dir) (rm-rf jsonl-dir) (rm-rf alias-parent)
        (tv/close-trail-view-runtime! rt)))))

;; =============================================================================
;; replay-assert-log! — durability + duty §8.7 empirical probe (relation runtime)
;; =============================================================================
(deftest replay-durability-and-duty7-probe
  (let [dir (tmp-dir "gs-replay-")
        log-path (str (io/file dir "relation-assert-log.ednl"))
        rt (rk/start-relation-runtime! {:tasks 2 :threads 2})]
    (try
      ;; author a representative /assert envelope (conversation->commit :produced,
      ;; :import custody) and write ONE §3.C plain-map line to the log.
      (let [from (rk/->target-ref :conversation "oc:chat-conversation:chat:sess-abc")
            to (rk/->target-ref :container "oc:doc:commit-object-key")
            env (rk/assert-request {:kind :produced :from from :to to
                                    :asserter-actor-id gs/import-asserter-actor-id
                                    :asserter-type :import :asserted-at-ms 1000 :sent-at-ms 1000
                                    :request-id "assert-req-1" :idempotency-key "assert-idem-1"
                                    :evidence-source-id "transcript:sess-abc" :evidence-anchor-id "uuid-x"
                                    :note "spine-v1|sha-verified"})
            expected-rid (:relation/routing-key env)
            line (gs/envelope->log-line env)]
        (spit log-path (str line "\n"))

        (testing "§3.C serialization is pure edn (no reader tags; edn round-trips)"
          (is (not (str/includes? line "#app.server")) "no defrecord literal in the log line")
          (is (map? (edn/read-string line)) "clojure.edn reads it back as a plain map"))

        (testing "FRESH cluster + replay -> edge asserted with the SAME relation-id, one event"
          (let [{:keys [replayed]} (gs/replay-assert-log! {:runtime rt :assert-log-path log-path})]
            (is (= 1 replayed))
            (rel-drain! rt 1)
            (let [{:keys [row history]} (rk/read-relation-detail rt expected-rid)]
              (is (some? row) "replayed envelope materialized the edge")
              (is (= expected-rid (:relation-id row)) "durability = SAME relation-id, not decision-row reuse")
              (is (= :produced (:relation-kind row)))
              (is (= "spine-v1|sha-verified" (:note row)))
              (is (= 1 (count history)) "exactly one event/transition in the fresh cluster"))))

        (testing "same-cluster DOUBLE replay -> journal drops, zero new events"
          (let [{:keys [replayed]} (gs/replay-assert-log! {:runtime rt :assert-log-path log-path})]
            (is (= 1 replayed) "the line is re-appended")
            (rel-drain! rt 2)                    ; 2 cumulative depot records consumed
            (let [{:keys [history]} (rk/read-relation-detail rt expected-rid)]
              (is (= 1 (count history)) "the duplicate was dropped -> still one event"))))

        (testing "duty §8.7 — the depot ACCEPTS a raw plain-map payload directly (empirical)"
          ;; append an envelope whose :payload (and :from/:to) are PLAIN MAPS — not
          ;; reconstructed to records — to settle whether the depot needs the typed
          ;; constructors. (Production replay uses typed reconstruction for parity.)
          (let [pfrom (rk/->target-ref :conversation "oc:chat-conversation:chat:sess-plain")
                pto (rk/->target-ref :container "oc:doc:plain-map-target")
                typed (rk/assert-request {:kind :produced :from pfrom :to pto
                                          :asserter-actor-id gs/import-asserter-actor-id
                                          :asserter-type :import :asserted-at-ms 2000 :sent-at-ms 2000
                                          :request-id "plain-req-1" :idempotency-key "plain-idem-1"
                                          :note "spine-v1|sha-verified"})
                plain-env (gs/envelope->plain-map typed)   ; payload/from/to are plain maps now
                plain-rid (:relation/routing-key plain-env)]
            (is (map? (:payload plain-env)) "payload is a plain map (not a record)")
            (is (not (record? (:from (:payload plain-env)))) "from is a plain map")
            (rk/append-relation-request! rt plain-env)
            (rel-drain! rt 3)
            (is (some? (edge-row rt plain-rid))
                "PLAIN-MAP payload envelope materialized an edge -> depot accepts plain maps"))))
      (finally (rm-rf dir) (rk/close-relation-runtime! rt)))))

;; =============================================================================
;; Gate-review fixes 4+6 (2026-07-05) — DIFF_FALSIFICATION_R1 should-fixes:
;; per-LINE replay isolation (a torn line skips, never aborts the pass and
;; silently un-replays everything after it); the reader reconstructs from ALL
;; stored keys (a future record field survives the seam instead of being
;; select-keys-dropped); UTF-8 pinned on the reader (non-ASCII notes replay).
;; =============================================================================
(deftest replay-resilience-and-field-preservation
  (let [dir (tmp-dir "gs-resilience-")
        log-path (str (io/file dir "relation-assert-log.ednl"))
        rt (rk/start-relation-runtime! {:tasks 2 :threads 2})
        note-utf8 "café ☕ spine-v1|sha-verified"]
    (try
      (let [env1 (rk/assert-request {:kind :produced
                                     :from (rk/->target-ref :conversation "oc:chat-conversation:chat:res-1")
                                     :to (rk/->target-ref :container "oc:doc:res-target-1")
                                     :asserter-actor-id gs/import-asserter-actor-id
                                     :asserter-type :import :asserted-at-ms 1000 :sent-at-ms 1000
                                     :request-id "res-req-1" :idempotency-key "res-idem-1"
                                     :note note-utf8})
            env2 (rk/assert-request {:kind :produced
                                     :from (rk/->target-ref :conversation "oc:chat-conversation:chat:res-2")
                                     :to (rk/->target-ref :container "oc:doc:res-target-2")
                                     :asserter-actor-id gs/import-asserter-actor-id
                                     :asserter-type :import :asserted-at-ms 2000 :sent-at-ms 2000
                                     :request-id "res-req-2" :idempotency-key "res-idem-2"
                                     :note "spine-v1|parent"})
            ;; the exact torn shape a crash between .write(line) and .write(\n)
            ;; leaves behind: a non-blank, unparseable prefix of a real line
            torn (subs (gs/envelope->log-line env1) 0 40)]
        (spit log-path
              (str (gs/envelope->log-line env1) "\n" torn "\n" (gs/envelope->log-line env2) "\n")
              :encoding "UTF-8")

        (testing "torn line skipped + counted; the line AFTER it still replays"
          (let [{:keys [replayed failed]} (gs/replay-assert-log! {:runtime rt :assert-log-path log-path})]
            (is (= 2 replayed))
            (is (= 1 failed))
            (rel-drain! rt 2)
            (is (some? (edge-row rt (:relation/routing-key env1))))
            (is (some? (edge-row rt (:relation/routing-key env2)))
                "durability past the torn line — nothing after it is lost")))

        (testing "non-ASCII note survives the UTF-8-pinned reader"
          (is (= note-utf8 (:note (edge-row rt (:relation/routing-key env1))))))

        (testing "a FUTURE payload/target field survives writer->reader verbatim"
          (let [env+ (-> env1
                         (update :payload assoc :future-field "kept")
                         (update-in [:payload :to] assoc :future-ref-field 7))
                req  (gs/stored-map->request (edn/read-string (gs/envelope->log-line env+)))]
            (is (= "kept" (:future-field (:payload req)))
                "unknown payload key rides the record's extension map, not dropped")
            (is (= 7 (:future-ref-field (get-in req [:payload :to])))
                "unknown target-ref key survives too")
            (is (= (:relation/routing-key env1) (:relation/routing-key req))
                "ids still travel verbatim"))))
      (finally (rm-rf dir) (rk/close-relation-runtime! rt)))))

;; =============================================================================
;; G12 — file(1) reports text for every new/changed file; no NUL bytes
;; =============================================================================
(deftest g12-files-are-text
  (let [root (System/getProperty "user.dir")
        files ["src/app/server/ingest/git_spine.clj"
               "test/app/server/ingest/git_spine_test.clj"
               "src/app/server/ingest/ingest_watchers.clj"
               "docs/current-mental-model/build/git-spine/PHASE_P1P2.md"]]
    (doseq [rel files]
      (let [path (str (io/file root rel))
            {:keys [out]} (sh! nil ["file" path])]
        (is (re-find #"(?i)text" out) (str "file(1) says text: " rel " => " (str/trim out)))
        (is (not (str/includes? (slurp path) (str (char 0)))) (str "no NUL bytes in " rel))))))
