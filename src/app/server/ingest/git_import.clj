(ns app.server.ingest.git-import
  "Git history imported as commit documents and typed relations.
   Takes: repository paths, commit metadata, transcript joins, assertion lines, and kernel runtimes.
   Gives: commit import results, :based-on and :produced relations, and replay results.
   Holds: data/relation-assert-log.ednl."
  (:require [app.server.rama.object-container :as oc]
            [app.server.ingest.markdown-adapter :as md]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.object-container.transcript-identity :as tid]
            [app.server.ingest.transcript :as transcript]
            [app.server.rama.relation-kernel :as rk]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.io File]
           [java.nio.charset StandardCharsets]
           [java.time Instant]))

;; ── Constants (§2.2 / §2.4 / §3.E shared literal) ────────────────────────────
(def import-asserter-actor-id
  "Version-FREE (§2.4): the extractor version lives in `note`, never here — the
   relation-id includes the asserter, so versioning it would fork every edge."
  "import:git-spine")
(def import-asserter-type :import)

(def assert-log-relative-path
  "The §3.E LITERAL shared path (SF-R2.1). The /assert writer (PW, server_jetty)
   computes the SAME literal independently with zero shared code; the two meet
   only at file format + path."
  "data/relation-assert-log.ednl")

(defn default-assert-log-path
  [{:keys [repo-root]}]
  (str (or repo-root (System/getProperty "user.dir")) "/" assert-log-relative-path))

(defn- spine-note [basis] (str "spine-v1|" basis))
(defn- spine-key
  "STABLE idempotency + request key (trap 4): re-runs converge at the journal even
   if the pre-check races the :append-ack microbatch lag."
  [relation-id basis]
  (str "spine:" relation-id ":" basis))

;; ============================================================================
;; A — adapter half (PURE)
;; ============================================================================

(defn commit-source-ref
  "The synthetic, path-free source-ref the md request builder ingests under
   (duty §8.3: the builder makes zero path assumptions). The sha stays
   human-legible here and in every edge's `note`."
  [sha]
  (str "git-commit:" sha))

(defn- git-log-bytes
  "Run `git log` under repo-root and return raw stdout bytes. stderr is discarded
   so it can never fill the pipe buffer and deadlock; stdout is NUL-delimited
   (`-z`), so it must be read as bytes and decoded once as UTF-8 (0x00/0x1e/0x1f
   are all <0x80 and can never appear inside a multi-byte UTF-8 sequence, so
   splitting the decoded string on them is safe — cf. the readLine footgun)."
  [repo-root args]
  (let [pb (ProcessBuilder. ^java.util.List (into [] (cons "git" args)))]
    (.directory pb (io/file repo-root))
    (.redirectError pb java.lang.ProcessBuilder$Redirect/DISCARD)
    (let [proc (.start pb)
          out (.readAllBytes (.getInputStream proc))]
      (.waitFor proc)
      out)))

;; Field framing (duty §8.5, verified via `od -c` on this repo):
;;   %x1e (RS) starts each commit record; %x1f (US) separates the fixed fields;
;;   git inserts a '\n' then the NUL-delimited --name-status blob after the last
;;   %x1f. %at/%ct are UNIX epoch seconds (locale/tz-free) -> Instant ISO UTC.
(def ^:private git-log-args
  ["log" "--all" "-z" "-M"
   "--pretty=format:%x1e%H%x1f%P%x1f%an%x1f%ae%x1f%at%x1f%ct%x1f%s%x1f%b%x1f"
   "--name-status"])

(defn- parse-long-safe [s]
  (try (Long/parseLong (str/trim (str s))) (catch Exception _ 0)))

(defn- epoch->iso
  "UNIX epoch seconds -> ISO-8601 UTC (`…Z`). Byte-stable by construction."
  [epoch-seconds]
  (.toString (Instant/ofEpochSecond (parse-long-safe epoch-seconds))))

(defn- parse-name-status
  "Parse the NUL-delimited --name-status blob into a SORTED vec of file lines.
   Rename/copy (`R100`/`C075`) tokens consume TWO paths, normalized `old -> new`
   (trap 8); every other status consumes one path."
  [blob]
  (let [tokens (->> (str/split (str/triml (or blob "")) #"\x00")
                    (remove str/blank?)
                    vec)]
    (loop [i 0 acc []]
      (if (>= i (count tokens))
        (vec (sort acc))
        (let [status (nth tokens i)]
          (if (or (str/starts-with? status "R") (str/starts-with? status "C"))
            (recur (+ i 3) (conj acc (str (nth tokens (inc i) "") " -> " (nth tokens (+ i 2) ""))))
            (recur (+ i 2) (conj acc (nth tokens (inc i) "")))))))))

(defn- parse-commit-chunk
  [chunk]
  (let [parts (str/split chunk #"\x1f" 9)]
    (when (>= (count parts) 8)
      (let [[sha parents an ae at ct subject body] parts]
        {:sha (str/trim sha)
         :parents (->> (str/split (str parents) #"\s+") (remove str/blank?) vec)
         :author-name an
         :author-email ae
         :authored-at (epoch->iso at)
         :committed-at (epoch->iso ct)
         :authored-at-ms (* 1000 (parse-long-safe at))
         :committed-at-ms (* 1000 (parse-long-safe ct))
         :subject subject
         :body (or body "")
         :files (parse-name-status (nth parts 8 ""))}))))

(defn read-commits
  "Structured seq of every commit reachable from any ref (`git log --all`). Repo
   path comes from cfg (never hardcoded). Deterministic per commit; order is not
   relied on."
  [repo-root]
  (let [text (String. ^bytes (git-log-bytes repo-root git-log-args) StandardCharsets/UTF_8)]
    (->> (str/split text #"\x1e")
         (remove str/blank?)
         (keep parse-commit-chunk)
         vec)))

(defn commit->canonical-text
  "PURE, byte-stable, LABELED lines in FIXED order (§3.A — a CROSS-BUILDER
   interface P3's bundle-subject parse reads; neither side may deviate). No
   locale, no wall clock: dates are UTC ISO-8601, files sorted, renames
   normalized."
  [{:keys [sha parents author-name author-email authored-at committed-at subject body files]}]
  (str "sha: " sha "\n"
       "parents: " (str/join " " parents) "\n"
       "author: " author-name " " author-email "\n"
       "authored-at: " authored-at "\n"
       "committed-at: " committed-at "\n"
       "subject: " subject "\n"
       "\n"
       (str/trimr (or body "")) "\n"
       "\n"
       "files:\n"
       (str/join "\n" files)
       (when (seq files) "\n")))

(defn commit->document-id
  "The commit object's `oc:doc:<object-key>` id — minted WITHOUT ingest by the
   SAME pure OC id math the md request builder uses over the identical canonical
   text (§2.3 / A-R2.1: extractor and adapter can never disagree). No :source/hash
   override is passed anywhere, so the ids are single-source."
  [commit]
  (oc/document-id-for (commit-source-ref (:sha commit))
                      (oc/source-hash (commit->canonical-text commit))))

(defn commit->import-request
  "Wrap the canonical text via the EXISTING md request builder with
   source-ref = \"git-commit:<sha>\" (§3.A). A deterministic :time-ms (the
   committer clock, not the wall clock) keeps the request byte-identical across
   runs (G1) while the idempotency key stays content-derived.

   `:claimed/at-ms` (t4-spine seam 1, ADDITIVE): the committer clock declared
   EXPLICITLY as a claimed clock. `:request/time-ms` cannot serve — the md
   builder defaults it to the wall clock when absent, so reading it as claimed
   would stamp every watcher md ingest's ARRIVAL as a claimed time (the map
   would lie; md ingest has no claimed clock and must stay nil-honest). Only a
   request that genuinely carries material-claimed time sets this key; the
   kernel copies it onto the source-ingest completion row, and the feed's
   two-clock stamp reads it from there."
  [commit]
  (assoc (md/markdown-source-import-request
          (commit->canonical-text commit)
          (commit-source-ref (:sha commit))
          {:time-ms (:committed-at-ms commit)
           ;; deterministic request-id (the sha) -> byte-identical request across runs
           ;; (G1) and a stable audit-id, not a random `req_<uuid>`.
           :request/id (str "git-spine:" (:sha commit))})
         :claimed/at-ms (:committed-at-ms commit)))

;; ============================================================================
;; B — sync driver + edges
;; ============================================================================

(defn- edge-already-asserted?
  "Cost guard on top of the journal (trap 4 / A2): is this relation currently an
   asserted edge? Reads the public relation-detail query."
  [runtime relation-id]
  (boolean (some-> (rk/read-relation-detail runtime relation-id)
                   :row :relation-status (= :asserted))))

(defn- assert-edge!
  "Append ONE :produced/:based-on assert with a stable key, unless the pre-check
   already sees it. Returns [relation-id appended?]."
  [runtime {:keys [kind from to basis claimed-ms evidence-source-id evidence-anchor-id]}]
  (let [relation-id (rk/relation-id-for kind from to import-asserter-actor-id)
        k (spine-key relation-id basis)]
    (if (edge-already-asserted? runtime relation-id)
      [relation-id false]
      (do (rk/append-relation-request!
           runtime
           (rk/assert-request {:kind kind :from from :to to
                               :asserter-actor-id import-asserter-actor-id
                               :asserter-type import-asserter-type
                               :asserted-at-ms claimed-ms
                               :sent-at-ms claimed-ms
                               :request-id k
                               :idempotency-key k
                               :evidence-source-id evidence-source-id
                               :evidence-anchor-id evidence-anchor-id
                               :note (spine-note basis)}))
          [relation-id true]))))

(defn based-on-relation-id
  "Deterministic id of the commit->parent :based-on edge (test/query helper)."
  [child-doc-id parent-doc-id]
  (rk/relation-id-for :based-on
                      (rk/->target-ref :container child-doc-id)
                      (rk/->target-ref :container parent-doc-id)
                      import-asserter-actor-id))

(defn spine-sync!
  "Idempotent full pass (§3.A): (1) BATCH commit ingests — append ALL requests,
   then await ALL decisions (NEVER a serial 5s await per commit, trap 7: the
   topology processes them concurrently while we append, so only the first await
   actually waits); (2) parent `:based-on` edges (from = commit, to = parent),
   one per parent so a merge commit yields one edge per parent (G4), each with a
   pre-check + stable key so a second sync run appends nothing new."
  [{:keys [runtime repo-root]}]
  (let [pass-started-ms (System/currentTimeMillis)
        commits (read-commits repo-root)
        reqs (mapv commit->import-request commits)
        _ (doseq [req reqs] (ocr/append-object-container-request! runtime req))
        decisions (mapv (fn [req] (ocr/await-object-container-decision runtime req 20000)) reqs)
        sha->doc (into {} (map (fn [c] [(:sha c) (commit->document-id c)])) commits)
        edge-results (for [c commits
                           parent (:parents c)
                           :let [child-doc (sha->doc (:sha c))
                                 parent-doc (sha->doc parent)]
                           :when (and child-doc parent-doc)]
                       (assert-edge! runtime
                                     {:kind :based-on
                                      :from (rk/->target-ref :container child-doc)
                                      :to (rk/->target-ref :container parent-doc)
                                      :basis "parent"
                                      :claimed-ms (:committed-at-ms c)}))
        edge-results (vec edge-results)]
    ;; Count truthfulness (t4-spine seam 3): :ingested keeps its gate-tested
    ;; meaning — decisions ACCEPTED, INCLUDING convergent re-accepts (G2's
    ;; convergence proof reads it: a non-deterministic re-run would conflict ->
    ;; :rejected -> the count would drop). The additive keys split the truth so
    ;; the boot log can't read a converged re-run as fresh material. A re-run
    ;; converges by TWO kernel mechanisms (verified in the topology source):
    ;; identical request-id -> the prior AUDIT decision row is ack-returned
    ;; untouched (spine-sync's G1-deterministic requests always take this
    ;; branch — its decided-at-ms predates this pass); fresh request-id + same
    ;; idempotency key -> replay-decision-row stamps :replayed-from-decision-id.
    ;; :converged counts both; :fresh = accepted AND decided during THIS pass.
    ;; :unresolved = no decision within the await window (appended, outcome
    ;; unknown here — NOT a failure claim).
    (let [accepted (filter #(= :accepted (:status %)) decisions)
          converged? (fn [d] (or (some? (:replayed-from-decision-id d))
                                 (< (long (or (:decided-at-ms d) Long/MAX_VALUE))
                                    pass-started-ms)))]
      {:commits (count commits)
       :ingested (count accepted)
       :fresh (count (remove converged? accepted))
       :converged (count (filter converged? accepted))
       :rejected (count (filter #(= :rejected (:status %)) decisions))
       :unresolved (count (remove some? decisions))
       :edges-appended (count (filter second edge-results))
       :edge-relation-ids (mapv first edge-results)})))

;; ============================================================================
;; B — transcript extractor
;; ============================================================================

(def ^:private git-context-re
  #"(?i)(?:^|[^0-9a-z])(git|commit|sha|rev-parse|head~|cherry-pick|rebase|checkout|revert|reset|log\b)")
(def ^:private hex-token-re #"\b[0-9a-f]{7,40}\b")

(defn session->conversation-container-id
  "The `oc:chat-conversation:chat:<sha>` id the transcript adapter mints for a
   claude-code session (duty §8.2). Joins: extract-object-key of it == the
   ingested conversation container's object-key."
  [session-id]
  (tid/chat-conversation-id (tid/transcript-object-key :claude-code session-id)))

(defn- try-parse-json [line]
  (try (json/read-str line :key-fn keyword) (catch Exception _ nil)))

(defn- entry-blocks
  "The content-block vector of a transcript entry (message content or top-level)."
  [entry]
  (let [c (or (get-in entry [:message :content]) (:content entry))]
    (cond (vector? c) c (sequential? c) (vec c) :else [])))

(defn- tool-use? [b] (contains? #{"tool_use" "tool-use"} (:type b)))
(defn- tool-result? [b] (contains? #{"tool_result" "tool-result"} (:type b)))

(defn- block-scan-text
  "Text of a block to scan for shas: tool_use name+input, tool_result content
   (string OR a vector of {:type \"text\" :text …}), or plain text."
  [b]
  (cond
    (tool-use? b) (str (:name b) " " (pr-str (:input b)))
    (tool-result? b) (let [c (:content b)]
                       (cond (string? c) c
                             (sequential? c) (str/join " " (map #(or (:text %) (str %)) c))
                             :else (str c)))
    (= "text" (:type b)) (str (:text b))
    :else (pr-str b)))

(defn- candidate-shas
  "7–40 hex tokens in git-ish text ONLY (trap 3 is the verify step, this is the
   cheap pre-filter)."
  [text]
  (when (and text (re-find git-context-re text))
    (distinct (re-seq hex-token-re (str/lower-case text)))))

(defn- resolve-sha
  "Repo verification without a shell-out: `candidate` resolves iff it is a full
   sha in the commit index OR a UNIQUE prefix of exactly one index sha (the same
   guarantee `git rev-parse` gives). Non-repo (fake/example) shas -> nil (trap 3)."
  [index-shas candidate]
  (let [c (str/lower-case candidate)]
    (if (contains? index-shas c)
      c
      (let [matches (filter #(str/starts-with? % c) index-shas)]
        (when (= 1 (count matches)) (first matches))))))

(defn- edit-file-paths
  "`:file_path` values from Edit/Write-family tool_use blocks."
  [entry]
  (->> (entry-blocks entry)
       (filter (fn [b] (and (tool-use? b)
                            (contains? #{"Edit" "Write" "MultiEdit" "NotebookEdit"} (:name b)))))
       (keep (fn [b] (get-in b [:input :file_path])))
       distinct))

(defn- entry-anchor
  "evidence-anchor-id = the entry uuid, else `line:<n>` (duty §8.4)."
  [entry line-idx]
  (or (:uuid entry)
      (transcript/transcript-message-uuid entry)
      (str "line:" line-idx)))

(defn- entry-time-ms
  "Deterministic asserted-at from the entry's own ISO timestamp (never the wall
   clock); 0 if absent."
  [entry]
  (try (.toEpochMilli (Instant/parse (str (:timestamp entry)))) (catch Exception _ 0)))

(defn- doc-document-id
  "Version-addressed (§2.3 / duty §8.6): the ingested doc's `oc:doc:<object-key>`
   id from (source-ref, CURRENT content-hash). source-ref is the file path as the
   md watcher uses it (`.getPath`)."
  [file-path]
  (oc/document-id-for file-path (oc/source-hash (slurp (io/file file-path)))))

(defn- canonical-under-roots
  "Rebase file-path into the WATCHER's textual form (t4-spine seam 2, closes
   gate-review doubt 1): the file must exist and CANONICALIZE under a land root
   (G6: outside the roots -> nil -> no edge); the returned path is the matching
   cfg root's TEXTUAL prefix + the canonical remainder — the exact source-ref
   string the md watcher ingests under (`.getPath` beneath that same cfg root).

   Grounds: the project root is reachable under two textual aliases
   (/mnt/data/projects/Softland is canonical; /home/sid/projects/Softland is a
   symlink to it), and transcripts record whichever alias the session used
   while the watcher's source-refs ride the boot cwd. Keying the doc id on the
   RAW transcript string (the old behavior) minted an id no ingested doc ever
   matches — a dangling edge whose target IS present, i.e. a DISHONEST
   dangler. Rebasing joins both directions: alias-recorded transcript paths
   join a canonical-cwd boot, and canonical-recorded paths join an
   alias-cwd boot. (Verified live 2026-07-06: 1089 /mnt-rooted + 1
   /home-rooted doc-land file_paths in the corpus.) Residual: a symlinked
   SUBDIRECTORY inside a land root would still split textual forms — none
   exist today (checked), recorded, not defended."
  [file-path land-roots]
  (try
    (let [f (io/file file-path)]
      (when (.exists f)
        (let [canon (.getCanonicalPath f)]
          (some (fn [root]
                  (let [rc (.getCanonicalPath (io/file root))]
                    (cond
                      (= canon rc) (str root)
                      (str/starts-with? canon (str rc File/separator))
                      (str root (subs canon (count rc)))
                      :else nil)))
                land-roots))))
    (catch Exception _ nil)))

(defn- session-id-of
  "The session-id (== conversation-id) the adapter keys on: prefer the `sessionId`
   field via transcript-conversation-id (scanning a few head lines), fall back to
   the filename stem (verified equal for claude-code files)."
  [^File file]
  (let [n (.getName file)
        stem (if (str/ends-with? n ".jsonl") (subs n 0 (- (count n) 6)) n)]
    (or (try
          (with-open [rdr (io/reader file :encoding "UTF-8")]
            (some (fn [line]
                    (when-not (str/blank? line)
                      (when-let [e (try-parse-json line)]
                        (transcript/transcript-conversation-id :claude-code e nil))))
                  (take 50 (line-seq rdr))))
          (catch Exception _ nil))
        stem)))

(defn- process-jsonl-file!
  "Stream ONE jsonl file line-by-line (never slurp — the corpus is ~1.2 GB).
   Emits conversation->commit `:produced` (repo-verified shas) and
   conversation->doc `:produced` (Edit/Write file_paths under the land roots,
   rebased to the watcher's textual form; ONE per (session, file)).

   `seen-sha`/`seen-doc` are RUN-level volatiles keyed [session-id x]
   (t4-spine seam 3): live corpus fact (2026-07-06) — up to 298 jsonl files
   share ONE sessionId (agent/sidechain files carry the parent session's id),
   and the relation-id is conversation-scoped, so per-FILE dedup re-appended
   the SAME edge once per sibling file: the journal drops the duplicates
   (land state was never wrong) but the run's :sha-edges/:doc-edges counts
   over-reported by up to ~300x per popular edge, and every duplicate paid a
   pre-check query + a depot append. Dedup now mirrors edge identity."
  [runtime sha->doc index-shas land-roots seen-sha seen-doc ^File file]
  (let [session-id (session-id-of file)
        conv-ref (rk/->target-ref :conversation (session->conversation-container-id session-id))
        evidence-source-id (str "transcript:" session-id)
        counts (volatile! {:sha-edges 0 :doc-edges 0})]
    (with-open [rdr (io/reader file :encoding "UTF-8")]
      (doseq [[line-idx line] (map-indexed vector (line-seq rdr))]
        (when-not (str/blank? line)
          (when-let [entry (try-parse-json line)]
            (let [anchor (entry-anchor entry line-idx)
                  claimed (entry-time-ms entry)
                  ;; conversation -> commit
                  shas (->> (entry-blocks entry)
                            (mapcat (fn [b] (candidate-shas (block-scan-text b))))
                            (keep #(resolve-sha index-shas %))
                            distinct)]
              (doseq [sha shas]
                (when-not (contains? @seen-sha [session-id sha])
                  ;; mark-seen AFTER the append returns (gate fix, SEAMS F1):
                  ;; a transient assert throw must leave the key unmarked so a
                  ;; same-session sibling file retries THIS boot — marking
                  ;; first would skip the edge for the whole run. assert-edge!
                  ;; RETURNS (never throws) on the already-asserted pre-check,
                  ;; so same-run re-marks stay impossible.
                  (let [[_ appended?] (assert-edge! runtime
                                                    {:kind :produced :from conv-ref
                                                     :to (rk/->target-ref :container (sha->doc sha))
                                                     :basis "sha-verified" :claimed-ms claimed
                                                     :evidence-source-id evidence-source-id
                                                     :evidence-anchor-id anchor})]
                    (vswap! seen-sha conj [session-id sha])
                    (when appended? (vswap! counts update :sha-edges inc)))))
              ;; conversation -> doc (capped one per (session, file))
              (doseq [fp (edit-file-paths entry)]
                (when-let [watch-path (canonical-under-roots fp land-roots)]
                  (when-not (contains? @seen-doc [session-id watch-path])
                    ;; mark AFTER append returns — same F1 rationale as above
                    (let [[_ appended?] (assert-edge! runtime
                                                      {:kind :produced :from conv-ref
                                                       :to (rk/->target-ref :container (doc-document-id watch-path))
                                                       :basis "file-write" :claimed-ms claimed
                                                       :evidence-source-id evidence-source-id
                                                       :evidence-anchor-id anchor})]
                      (vswap! seen-doc conj [session-id watch-path])
                      (when appended? (vswap! counts update :doc-edges inc)))))))))))
    @counts))

;; Cursor: {:run-id <cluster-instance id> :files {path {:mtime :size}}}. COST
;; optimization ONLY — correctness is the idempotency journal; deleting the
;; cursor must never change land state (G7).
;;
;; INSTANCE-SCOPED (gate-review addendum 2026-07-05): the land's cluster is an
;; in-process IPC — EPHEMERAL per JVM; all edge state rebuilds from re-ingest
;; at boot. A durable cursor honored by a FRESH cluster would skip every
;; unchanged transcript and silently lose the conversation edges on every
;; boot after the first. So the cursor is only valid for the cluster instance
;; (:spine-run-id, minted alongside the runtime) that wrote it; a foreign or
;; legacy cursor reads as absent → full reprocess, which G7 already declares
;; correct.
(defn- read-cursor [path run-id]
  (try (when (and path (.exists (io/file path)))
         (let [c (edn/read-string (slurp path))]
           (when (and (map? c) (= run-id (:run-id c)))
             (:files c))))
       (catch Exception _ nil)))

(defn- write-cursor! [path run-id files]
  (when path
    (io/make-parents (io/file path))
    (spit path (pr-str {:run-id run-id :files files}))))

(defn- file-sig [^File f] {:mtime (.lastModified f) :size (.length f)})

(defn- jsonl-files [transcript-roots]
  (for [root transcript-roots
        ^File f (file-seq (io/file root))
        :when (and (.isFile f) (str/ends-with? (.getName f) ".jsonl"))]
    f))

(defn land-doc-roots
  "The md roots the trail watcher ingests under (file_viewer boot future) — the
   only files a doc `:produced` edge may target."
  [repo-root]
  [(str repo-root "/docs/current-mental-model") (str repo-root "/vision")])

(defn extract-session-joins!
  "Stream every jsonl under cfg :transcript-roots, asserting transcript->commit
   and transcript->doc `:produced` edges. Repo-verified shas only. The cursor
   skips unchanged files (cost only)."
  [{:keys [runtime repo-root transcript-roots spine-cursor-path spine-run-id]}]
  (let [commits (read-commits repo-root)
        sha->doc (into {} (map (fn [c] [(:sha c) (commit->document-id c)])) commits)
        index-shas (set (keys sha->doc))
        land-roots (land-doc-roots repo-root)
        ;; no stable instance id supplied -> a per-call id: the cursor never
        ;; matches, every file reprocesses — always correct, only costly
        run-id (or spine-run-id (str (java.util.UUID/randomUUID)))
        cursor0 (or (read-cursor spine-cursor-path run-id) {})
        ;; RUN-level dedup, keyed [session-id x] — mirrors the edge identity
        ;; (relation-ids are conversation-scoped); see process-jsonl-file!.
        seen-sha (volatile! #{})
        seen-doc (volatile! #{})]
    (loop [fs (seq (jsonl-files transcript-roots))
           cursor cursor0
           stats {:files 0 :skipped 0 :failed 0 :sha-edges 0 :doc-edges 0}]
      (if (empty? fs)
        (do (write-cursor! spine-cursor-path run-id cursor) stats)
        (let [^File f (first fs)
              path (.getPath f)
              sig (file-sig f)]
          (if (= sig (get cursor path))
            (recur (next fs) cursor (update stats :skipped inc))
            ;; per-FILE isolation (gate review 2026-07-05): one unreadable
            ;; transcript (permissions, mid-read delete) is counted and
            ;; skipped — it must not abort the pass, cost later files their
            ;; edges this run, and leave the cursor unwritten. The failed
            ;; file's cursor entry is NOT advanced, so it retries next boot.
            (let [result (try
                           (process-jsonl-file! runtime sha->doc index-shas land-roots
                                                seen-sha seen-doc f)
                           (catch Exception e
                             (println "[GIT-SPINE] extract:" path "failed, skipping:"
                                      (.getMessage e))
                             nil))]
              (if result
                (recur (next fs)
                       (assoc cursor path sig)
                       (-> stats
                           (update :files inc)
                           (update :sha-edges + (:sha-edges result))
                           (update :doc-edges + (:doc-edges result))))
                (recur (next fs) cursor (update stats :failed inc))))))))))

;; ============================================================================
;; C — durability: /assert write-ahead log serialization + boot replay
;; ============================================================================

(defn- record->plain [x] (if (record? x) (into {} x) x))

(defn envelope->plain-map
  "§3.C serialization: the envelope's :payload (and its :from/:to) become PLAIN
   MAPS so the line is pure edn — no reader tags, safe under clojure.edn and
   *read-eval* false (R2 proved record literals throw otherwise). Namespaced
   envelope keys round-trip through edn untouched."
  [env]
  (let [payload (:payload env)]
    (if payload
      (assoc env :payload
             (-> (record->plain payload)
                 (assoc :from (record->plain (:from payload))
                        :to (record->plain (:to payload)))))
      env)))

(defn envelope->log-line
  "One assert-log line (the §3.E write-ahead format PW mirrors independently)."
  [env]
  (pr-str (envelope->plain-map env)))

(defn- plain-map->target-ref [m]
  ;; ALL stored keys, never a hard-coded select-keys (gate review 2026-07-05):
  ;; map->Record keeps unknown keys in the record's extension map, so a field
  ;; added to RelationTargetRef later is written by the route's `into {}` AND
  ;; survives replay — the two sides of the seam can no longer drift silently.
  (when m (rk/map->RelationTargetRef m)))

(defn- plain-map->payload [m]
  (rk/map->RelationMutationPayload
   (assoc m
          :from (plain-map->target-ref (:from m))
          :to (plain-map->target-ref (:to m)))))

(defn stored-map->request
  "Rebuild a depot-acceptable envelope from a stored plain-map line. Ids
   (:request/id, :idempotency/key, :relation/routing-key) travel VERBATIM (trap 5
   — minting is forbidden); the payload TYPES are reconstructed via the public
   defrecord constructors (duty §8.7: the depot reads the payload purely by
   keyword lookup, so a plain map would ALSO be accepted — typed reconstruction
   is chosen for byte-parity with a fresh assert)."
  [m]
  (assoc m :payload (plain-map->payload (:payload m))))

(defn replay-assert-log!
  "Re-append every stored /assert envelope to the relation depot VERBATIM (§3.C).
   Within one cluster lifetime double replay adds nothing (the journal drops on
   the stable idempotency key); on a FRESH cluster the first replay reconstructs
   state with IDENTICAL relation-ids (durability = state reconstruction, not
   decision-row reuse). Missing log file -> no-op.

   Per-LINE isolation (gate review 2026-07-05): a malformed/torn line — e.g. a
   crash between the writer's line and newline — is counted and SKIPPED, never
   allowed to abort the reduce and silently un-replay every line after it on
   every future boot. Reader charset pinned UTF-8, matching the route writer."
  [{:keys [runtime assert-log-path] :as cfg}]
  (let [path (or assert-log-path (default-assert-log-path cfg))
        f (io/file path)]
    (if-not (.exists f)
      {:replayed 0 :failed 0}
      (with-open [rdr (io/reader f :encoding "UTF-8")]
        (reduce
         (fn [stats [line-idx line]]
           (if (str/blank? line)
             stats
             (try
               (rk/append-relation-request! runtime (stored-map->request (edn/read-string line)))
               (update stats :replayed inc)
               (catch Exception e
                 (println "[GIT-SPINE] replay: line" line-idx "failed, skipping:"
                          (.getMessage e))
                 (update stats :failed inc)))))
         {:replayed 0 :failed 0}
         (map-indexed vector (line-seq rdr)))))))
