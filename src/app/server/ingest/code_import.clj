(ns app.server.ingest.code-import
  "Code importer that calls a cutter, then writes rows and typed relations into the store.
   Takes: git commits, blob ids, changed paths, analyzer output, and kernel runtimes.
   Gives: import requests and :supersedes, :requires, and :calls relations.
   Holds: kondo-config-dir."
  (:require [app.server.rama.object-container :as oc]
            [app.server.ingest.clojure-adapter :as adapter]
            [app.server.rama.object-container.runtime :as ocr]
            [app.server.rama.relation-kernel :as rk]
            [clj-kondo.core :as kondo]           ; P3 analyzer lane (P0_PARSE_SPIKE.md §b)
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str])
  (:import [java.nio.charset StandardCharsets]))

;; ── Deriver constants (R4, T1: VERSION-FREE actor; version rides `note`) ─────
(def lineage-asserter-actor-id
  "Version-FREE (R4/T1): the deriver version lives in `note`, never here — the
   relation-id includes the asserter, so versioning it would fork every edge AND
   lose retraction rights (relation_kernel.clj:441-448)."
  "import:code-lineage")
(def lineage-asserter-type :import)
(def deriver-version "clj-atoms-v1")
(def code-form-target-kind
  "Open target-kind for a form-instance endpoint. rk/->target-ref falls through to
   verbatim target-key for unknown kinds (relation_kernel.clj:840-846), so the
   target-key IS the unit-id — no schema change, no object-key extraction."
  :code-form)

(def ^:private null-sha "0000000000000000000000000000000000000000")

;; ════════════════════════════════════════════════════════════════════════════
;; git process (own byte-safe idiom; MIRRORS git_spine/git-log-bytes, edits it not)
;; ════════════════════════════════════════════════════════════════════════════
(defn- git-bytes
  "Run git under repo-root, return raw stdout bytes. stderr is DISCARDed so it can
   never fill the pipe and deadlock; stdout is drained fully BEFORE waitFor. UTF-8
   decoded once by callers (git_spine.clj:75-88 idiom, reimplemented — not an edit).

   F5 (DIFF_FALSIFICATION): a NON-ZERO exit THROWS an ex-info tagged `:git/exit`.
   The pre-fix code returned stdout regardless of `.waitFor`, so a failed cat-file
   (bad sha, corrupt repo) yielded empty bytes → `blob-text \"\"` → a silent 0-unit
   cut with NO exception — the exact silent-empty-blob the finding names. Callers
   that must isolate one bad blob from a whole sync catch `:git/exit` and count it
   in `:git-failures` (spine per-file-isolation precedent); everyone else fails loud."
  [repo-root args]
  (let [pb (ProcessBuilder. ^java.util.List (into ["git"] args))]
    (.directory pb (io/file repo-root))
    (.redirectError pb java.lang.ProcessBuilder$Redirect/DISCARD)
    (let [proc (.start pb)
          out  (.readAllBytes (.getInputStream proc))
          exit (.waitFor proc)]
      (when-not (zero? exit)
        (throw (ex-info (str "git " (first args) " exited " exit)
                        {:git/exit exit :git/args (vec args) :repo-root (str repo-root)})))
      out)))

(defn- git-failure?
  "True iff `e` is a git-exit ex-info (F5 isolation predicate). A non-git exception
   re-throws so real bugs are never swallowed as a 'git failure'."
  [e]
  (and (instance? clojure.lang.ExceptionInfo e) (some? (:git/exit (ex-data e)))))

(defn- git-text [repo-root args]
  (String. ^bytes (git-bytes repo-root args) StandardCharsets/UTF_8))

(defn blob-text
  "UTF-8 text of git blob <sha>. cat-file reads the OBJECT STORE, never the working
   tree (T3: history text must come from the blob, not the one checked-out version;
   contrast git_spine's deliberate `slurp` of live doc files at :376-381)."
  [repo-root sha]
  (git-text repo-root ["cat-file" "blob" sha]))

;; ════════════════════════════════════════════════════════════════════════════
;; Enumeration + lineage source (P0 §c: ONE B-full pass feeds BOTH lanes)
;; ════════════════════════════════════════════════════════════════════════════
(def code-log-args
  "P0_PARSE_SPIKE.md §c B-full (895/895 == ls-tree oracle; --full-history + -m are
   load-bearing, not decoration). Machine format: RS(0x1e) starts each record,
   US(0x1f) separates <sha> <committer-epoch> <parents>; then the --raw lines."
  ["log" "--all" "--full-history" "-m" "--raw" "--no-abbrev" "--no-renames"
   "--format=%x1e%H%x1f%ct%x1f%P" "--" "src" "test"])

(defn code-path?
  "SPEC scope: `.clj/.cljc/.cljs` under src/ or test/."
  [path]
  (boolean
   (and path
        (or (str/starts-with? path "src/") (str/starts-with? path "test/"))
        (or (str/ends-with? path ".clj")
            (str/ends-with? path ".cljc")
            (str/ends-with? path ".cljs")))))

(defn- parse-change-line
  "A --raw line `:<om> <nm> <old-sha> <new-sha> <STATUS>\\t<path>` -> map, or nil.
   (--no-renames keeps a move as delete+add, so break→silver sees it; R3.)"
  [line]
  (when (str/starts-with? line ":")
    (let [[meta path] (str/split line #"\t" 2)
          parts (str/split (subs meta 1) #"\s+")]
      (when (and path (>= (count parts) 5))
        {:old-sha (nth parts 2) :new-sha (nth parts 3) :status (nth parts 4) :path path}))))

(defn parse-code-log
  "B-full output -> vec of BLOCKS {:sha :committer-ms :parents :changes}. -m repeats
   the header once per NON-EMPTY parent-diff, so a merge yields one block per
   parent — exactly R3's (parent-commit, commit) grain; the change line's own
   old-sha/new-sha carry the against-that-parent lineage, so no parent sha needs
   resolving (P0 §c). committer-epoch (%ct) → the ONLY clock (T4)."
  [text]
  (->> (str/split text #"\x1e")
       (remove str/blank?)
       (keep (fn [record]
               (let [lines (str/split-lines record)
                     [sha ct parents] (str/split (first lines) #"\x1f" 3)]
                 (when (and sha (not (str/blank? sha)))
                   {:sha (str/trim sha)
                    :committer-ms (* 1000 (try (Long/parseLong (str/trim (str ct)))
                                               (catch Exception _ 0)))
                    :parents (->> (str/split (str parents) #"\s+") (remove str/blank?) vec)
                    :changes (vec (keep parse-change-line (rest lines)))}))))
       vec))

(defn read-code-log
  "Every commit block reachable from any ref, code paths only downstream."
  [repo-root]
  (parse-code-log (git-text repo-root code-log-args)))

(defn ls-tree-blob-oracle
  "Correctness oracle (P0 §c pinned 895): the ls-tree UNION of code blobs across
   `--all`. A test compares the driver's enumerated blob set against this."
  [repo-root]
  (let [shas (->> (str/split (git-text repo-root ["rev-list" "--all"]) #"\n")
                  (remove str/blank?))]
    (into #{}
          (comp (mapcat (fn [c]
                          (->> (str/split-lines (git-text repo-root ["ls-tree" "-r" c "--" "src" "test"]))
                               (keep (fn [l]
                                       (let [[meta path] (str/split l #"\t" 2)
                                             parts (str/split (str meta) #"\s+")]
                                         (when (and path (code-path? path) (>= (count parts) 3))
                                           (nth parts 2))))))))
                (remove #(= % null-sha)))
          shas)))

;; ════════════════════════════════════════════════════════════════════════════
;; Pure cut over a blob (T2 datum: name -> {unit-id, text-hash})
;; ════════════════════════════════════════════════════════════════════════════
(defn cut-named-units
  "PURE. (sha, blob-text) -> {:object-key :source-id :units {block-path {:unit-id
   :hash :kind}}}. NAMED units only (`:name` non-nil): unnamed positional forms
   (`:clj/other`, reader-conds) drift across edits and are EXCLUDED from lineage,
   so unnamed-id drift can never fake a supersession (T2; PHASE_P1 doubt 3).

   object-key uses the SAME id math the adapter mints on ingest (source-ref
   \"git-blob:<sha>\" + content hash, R1), so a unit-id computed here JOINS the
   ingested DerivedUnitRow. `:hash` = oc/source-hash of the form text = the
   DerivedUnitRow derived-content-hash (SPEC §4.2) the lineage law reads."
  [sha text]
  (let [object-key (oc/object-key-for (str "git-blob:" sha) (oc/source-hash text))
        cut        (adapter/clojure-form-v0 text)]
    (cond->
     {:object-key object-key
      :source-id  (oc/source-id-for-object-key object-key)
      :units (into {}
                   (for [{:keys [block-path unit-kind text name]} (:units cut)
                         :when (some? name)]
                     [block-path {:unit-id (adapter/derived-unit-id object-key block-path)
                                  :hash    (oc/source-hash text)
                                  :kind    unit-kind}]))}
      ;; G-F1 (gate 2026-07-09): a committed-broken blob cuts to ZERO units with
      ;; :parse-error carried through, so lineage sees empty name maps (no fake
      ;; edges) and the sync driver COUNTS it — never aborts, never silent.
      (:parse-error cut) (assoc :parse-error (:parse-error cut)))))

;; ════════════════════════════════════════════════════════════════════════════
;; Sync driver internals
;; ════════════════════════════════════════════════════════════════════════════
(defn- convergent?
  "git_spine convergence test (git_spine.clj:283-286): a prior decision was
   ack-returned (identical request-id → audit branch, decided before this pass) or
   stamped :replayed-from-decision-id. Splits :blobs-ingested (fresh) from
   :blobs-converged so a re-run reads honestly (G9), never fresh material."
  [pass-started-ms d]
  (or (some? (:replayed-from-decision-id d))
      (< (long (or (:decided-at-ms d) Long/MAX_VALUE)) pass-started-ms)))

(defn- edge-already-asserted?
  "Cost guard on top of the journal (git_spine.clj:207-212): is this relation
   currently an asserted edge? Reads the public relation-detail query."
  [runtime relation-id]
  (boolean (some-> (rk/read-relation-detail runtime relation-id)
                   :row :relation-status (= :asserted))))

(defn- deny-fn
  "R6/T10: a PATH predicate. `deny-list-override` (tests only) replaces the
   adapter's real deny-list with a suffix-matched set; else the adapter's
   `denied-path?` (env.clj fail-closed) governs."
  [deny-list-override]
  (if deny-list-override
    (fn [path]
      (boolean (some (fn [d] (or (= path d) (str/ends-with? (str path) (str "/" d))))
                     deny-list-override)))
    adapter/denied-path?))

(defn- enumerate-blobs
  "Distinct NEW code blobs across the filtered blocks: {new-sha {:min-ms :path
   :denied?}}. Deny is fail-closed PER PATH (R6/F6): a blob occurrence at a denied
   path contributes NOTHING, and the blob is `:denied?` only when it has NO
   allowed-path occurrence at all. This MATCHES `commit-lineage`'s per-change-path
   denial below — pre-fix the ingest lane fail-closed a blob seen at ANY denied
   path while lineage cut it via an allowed path, so a blob denied at path X but
   changed at allowed path Y was EXCLUDED from ingest yet minted lineage edges
   (DIFF_FALSIFICATION F6; moot for env.clj, which is gitignored). `:min-ms`/`:path`
   ride the ALLOWED occurrences only (committer clock, T4; min = order-independent).
   Every reachable blob still APPEARS in the map (counted in :blobs-seen); a
   blob with no allowed occurrence carries :denied? true + nil clock and is never
   cat-filed. Skips 0000000 post-shas (deletions)."
  [blocks deny?]
  (let [raw (reduce
             (fn [acc {:keys [committer-ms changes]}]
               (reduce
                (fn [acc {:keys [new-sha path]}]
                  (if (and (code-path? path) (not= new-sha null-sha))
                    (update acc new-sha
                            (fn [e]
                              (let [e (or e {:min-ms nil :path nil :allowed? false})]
                                (if (deny? path)
                                  e   ; denied-path occurrence: record nothing but keep the blob present
                                  (-> e
                                      (assoc :allowed? true)
                                      (update :min-ms (fn [m] (if m (min (long m) (long committer-ms)) committer-ms)))
                                      (update :path #(or % path)))))))
                    acc))
                acc changes))
             {} blocks)]
    (into {} (map (fn [[sha {:keys [min-ms path allowed?]}]]
                    [sha {:min-ms min-ms :path path :denied? (not allowed?)}]))
          raw)))

(defn commit-lineage
  "PURE (given `cut-fn`: sha -> cut-named-units map, and `deny?`: path predicate).
   One commit BLOCK (per parent,commit — R3 grain) -> a lineage plan:
     {:mech-edges [edge-spec ...]   ; superseded (name equal, hash differs) new→old
      :silver-edges [edge-spec ...] ; break matched hash-exact across the block
      :re-addressed N :unmatched-vanished N :unmatched-appeared N}
   edge-spec = {:from-uid :to-uid :note :evidence-source-id :evidence-anchor-id
                :child-sha :child-ms}. Denied paths (R6) contribute nothing. This is
   the whole SPEC §4.3 three-outcome law with NO I/O — the silver lane is provable
   on synthetic in-memory blobs."
  [{child-sha :sha child-ms :committer-ms :keys [changes]} cut-fn deny?]
  (let [code-changes (filter #(and (code-path? (:path %)) (not (deny? (:path %)))) changes)
        st (reduce
            (fn [st {:keys [old-sha new-sha]}]
              (let [old-cut   (when (not= old-sha null-sha) (cut-fn old-sha))
                    new-cut   (when (not= new-sha null-sha) (cut-fn new-sha))
                    old-units (:units old-cut)
                    old-src   (:source-id old-cut)
                    new-units (:units new-cut)
                    new-src   (:source-id new-cut)
                    old-names (set (keys old-units))
                    new-names (set (keys new-units))]
                (as-> st st
                  ;; within-file: name in both versions of THIS path
                  (reduce (fn [st bp]
                            (if (= (:hash (old-units bp)) (:hash (new-units bp)))
                              (update st :re-addressed inc)        ; T2: mint NOTHING
                              (let [nu (:unit-id (new-units bp))]
                                (update st :mech-edges conj
                                        {:from-uid nu :to-uid (:unit-id (old-units bp))
                                         :note (str deriver-version "|mech|" child-sha)
                                         :evidence-source-id new-src
                                         :evidence-anchor-id (oc/source-anchor-id nu)
                                         :child-sha child-sha :child-ms child-ms}))))
                          st (set/intersection old-names new-names))
                  (reduce (fn [st bp]
                            (update st :vanished conj
                                    {:hash (:hash (old-units bp)) :unit-id (:unit-id (old-units bp))
                                     :source-id old-src}))
                          st (set/difference old-names new-names))
                  (reduce (fn [st bp]
                            (update st :appeared conj
                                    {:hash (:hash (new-units bp)) :unit-id (:unit-id (new-units bp))
                                     :source-id new-src}))
                          st (set/difference new-names old-names)))))
            {:mech-edges [] :re-addressed 0 :vanished [] :appeared []}
            code-changes)
        ;; block-level break detection: vanished text-hash matches appeared EXACTLY →
        ;; SILVER (never the "mech" marker — SPEC §4.3: the map must not lie).
        {:keys [vanished appeared]} st
        app-by-hash (reduce (fn [m a] (update m (:hash a) (fnil conj []) a)) {} appeared)
        acc (reduce
             (fn [acc v]
               (if-let [a (first (sort-by :unit-id (get app-by-hash (:hash v))))]
                 (-> acc
                     (update :matched-hashes conj (:hash v))
                     (update :silver-edges conj
                             {:from-uid (:unit-id a)    ; the APPEARED (new) form
                              :to-uid   (:unit-id v)    ; the VANISHED (old) form
                              :note (str deriver-version "|silver-move|hash-exact|" child-sha)
                              :evidence-source-id (:source-id a)
                              :evidence-anchor-id (oc/source-anchor-id (:unit-id a))
                              :child-sha child-sha :child-ms child-ms}))
                 (update acc :unmatched-vanished inc)))
             {:silver-edges [] :matched-hashes #{} :unmatched-vanished 0}
             (sort-by :unit-id vanished))]
    {:mech-edges (:mech-edges st)
     :silver-edges (:silver-edges acc)
     :re-addressed (:re-addressed st)
     :unmatched-vanished (:unmatched-vanished acc)
     :unmatched-appeared (count (remove #(contains? (:matched-hashes acc) (:hash %)) appeared))}))

;; ════════════════════════════════════════════════════════════════════════════
;; code-sync!  — the P2 entry (git_spine/spine-sync! shape, stats out, no caps)
;; ════════════════════════════════════════════════════════════════════════════
(defn code-sync!
  "Idempotent sync over EXISTING public APIs. cfg:
     :runtime            OC + relation-kernel handles (trail-view runtime map)
     :repo-root          repo path (never hardcoded)
     :commit-filter      optional predicate or set of full shas — nil = all commits
     :deny-list-override optional set of denied path suffixes (TESTS ONLY)

   Returns a stats map — every bound named, NO silent caps (F4/F5, G-F1):
     {:commits-seen :blobs-seen :blobs-ingested :blobs-denied :blobs-converged
      :blobs-unresolved :git-failures :blobs-unparseable :lineage-over-unresolved
      :supersedes-mech :supersedes-silver :re-addressed
      :unmatched-vanished :unmatched-appeared}
   :blobs-seen = denied + git-failures + ingested + converged + unresolved (identity).
   :blobs-unparseable (G-F1) is an OVERLAY count, not part of the identity: blobs
   whose committed text rewrite-clj cannot parse (history holds broken states —
   5/895 in this repo). They still ingest (raw surface stored, R1) with ZERO
   units, mint no lineage, and are counted here so the boundary is declared.
   (:supersedes-* are APPENDED counts — a byte-identical re-run appends 0, G9.)"
  [{:keys [runtime repo-root commit-filter deny-list-override]}]
  (let [pass-started-ms (System/currentTimeMillis)
        deny?  (deny-fn deny-list-override)
        ;; :commit-filter is a set OR a predicate over the full sha (a set IS a
        ;; predicate in Clojure) — nil = all commits.
        blocks (cond->> (read-code-log repo-root)
                 commit-filter (filter (fn [b] (boolean (commit-filter (:sha b))))))
        ;; per-sync caches: cat-file / cut each distinct sha at most once
        text-cache (atom {})
        text-of (fn [sha] (or (@text-cache sha)
                              (let [t (blob-text repo-root sha)]
                                (swap! text-cache assoc sha t) t)))
        cut-cache (atom {})
        cut-of (fn [sha] (or (@cut-cache sha)
                             (let [c (cut-named-units sha (text-of sha))]
                               (swap! cut-cache assoc sha c) c)))

        ;; ═══════════ LANE A — enumeration + batch ingest (T9) ═══════════
        git-failures (atom 0)     ; F5: blobs git could not read (isolated per blob)
        blob-map    (enumerate-blobs blocks deny?)
        denied-shas (into #{} (keep (fn [[sha e]] (when (:denied? e) sha)) blob-map))
        ingest-shas (sort (remove denied-shas (keys blob-map)))   ; sorted → deterministic
        ingest-entries (into []
                             (keep (fn [sha]
                                     ;; deny already excluded above → text-of NEVER runs
                                     ;; for a denied blob (deny by PATH before cat-file, T10).
                                     (try
                                       (let [{:keys [min-ms]} (blob-map sha)
                                             req (adapter/clojure-source-import-request
                                                  (text-of sha) (str "git-blob:" sha)
                                                  {:request/id (str "code-atoms:" sha)
                                                   :time-ms      min-ms   ; committer clock, T4
                                                   :claimed/at-ms min-ms})]
                                         {:sha sha :req req})
                                       ;; F5: a git failure isolates ONE blob (counted), never aborts
                                       ;; the sync and never appends a silent empty blob.
                                       (catch Exception e
                                         (if (git-failure? e) (do (swap! git-failures inc) nil) (throw e))))))
                             ingest-shas)
        _ (doseq [{:keys [req]} ingest-entries]
            (ocr/append-object-container-request! runtime req))     ; append ALL...
        decisions (mapv (fn [{:keys [sha req]}]
                          {:sha sha :decision (ocr/await-object-container-decision runtime req 20000)})
                        ingest-entries)                             ; ...then await ALL (T9)
        accepted   (filter #(= :accepted (:status (:decision %))) decisions)
        ;; F4: a nil decision (await timeout, runtime.clj:326) or a rejected status is
        ;; COUNTED as :blobs-unresolved, never silently dropped. The accounting identity
        ;; :blobs-seen = denied + git-failures + ingested + converged + unresolved holds.
        unresolved (remove #(= :accepted (:status (:decision %))) decisions)
        unresolved-source-ids (into #{} (map (fn [{:keys [sha]}] (:source-id (cut-of sha)))) unresolved)
        blobs-ingested  (count (remove #(convergent? pass-started-ms (:decision %)) accepted))
        blobs-converged (count (filter #(convergent? pass-started-ms (:decision %)) accepted))

        ;; ═══════════ LANE B — lineage (R3, SPEC §4.3) ═══════════
        seen-edges (atom #{})   ; run-level dedup by relation-id (honest counts)
        append-edge!
        (fn [{:keys [from-uid to-uid child-sha child-ms note evidence-source-id evidence-anchor-id]}]
          ;; returns true iff a fresh append happened (git_spine assert-edge! shape)
          (let [from-ref    (rk/->target-ref code-form-target-kind from-uid)
                to-ref      (rk/->target-ref code-form-target-kind to-uid)
                relation-id (rk/relation-id-for :supersedes from-ref to-ref lineage-asserter-actor-id)
                k (str "code:" relation-id ":" child-sha)]     ; RELATION-scoped, git_spine pattern
            (cond
              (contains? @seen-edges relation-id) false          ; appended already THIS run
              (edge-already-asserted? runtime relation-id)       ; converged (re-run) — pre-check
              (do (swap! seen-edges conj relation-id) false)
              :else
              (do (rk/append-relation-request!
                   runtime
                   (rk/assert-request {:kind :supersedes :from from-ref :to to-ref
                                       :asserter-actor-id lineage-asserter-actor-id
                                       :asserter-type lineage-asserter-type
                                       :asserted-at-ms child-ms :sent-at-ms child-ms
                                       :request-id k :idempotency-key k
                                       :evidence-source-id evidence-source-id
                                       :evidence-anchor-id evidence-anchor-id
                                       :note note}))
                  (swap! seen-edges conj relation-id)
                  true))))
        cut-fn (fn [sha] (cut-of sha))
        append-count (fn [edges] (reduce (fn [n e] (if (append-edge! e) (inc n) n)) 0 edges))
        ;; F4: lineage cuts a blob independent of ingest (pure over git), so a blob that
        ;; TIMED OUT at ingest can still mint :supersedes edges at its unit-ids (dangling
        ;; endpoints — kernel-legal, trap 3). Count edges whose new-side blob is unresolved.
        over-unresolved (fn [edges] (count (filter #(contains? unresolved-source-ids (:evidence-source-id %)) edges)))
        lineage (reduce
                 (fn [acc block]
                   (try
                     (let [{:keys [mech-edges silver-edges re-addressed
                                   unmatched-vanished unmatched-appeared]} (commit-lineage block cut-fn deny?)]
                       (-> acc
                           (update :supersedes-mech   + (append-count mech-edges))
                           (update :supersedes-silver + (append-count silver-edges))
                           (update :re-addressed       + re-addressed)
                           (update :unmatched-vanished + unmatched-vanished)
                           (update :unmatched-appeared + unmatched-appeared)
                           (update :lineage-over-unresolved + (over-unresolved mech-edges) (over-unresolved silver-edges))))
                     ;; F5: a git failure cutting a historical blob isolates THIS block's
                     ;; lineage (counted), never aborts the sync.
                     (catch Exception e
                       (if (git-failure? e) (do (swap! git-failures inc) acc) (throw e)))))
                 {:supersedes-mech 0 :supersedes-silver 0 :re-addressed 0
                  :unmatched-vanished 0 :unmatched-appeared 0 :lineage-over-unresolved 0}
                 blocks)]
    (assoc lineage
           :commits-seen (count (distinct (map :sha blocks)))
           :blobs-seen   (count blob-map)
           :blobs-ingested  blobs-ingested
           :blobs-denied    (count denied-shas)
           :blobs-converged blobs-converged
           :blobs-unresolved (count unresolved)   ; F4: nil-decision/rejected, never a silent drop
           :git-failures    @git-failures          ; F5: blobs git could not read (isolated)
           ;; G-F1: committed-broken blobs (cut degraded to 0 units; text is cached
           ;; from the request build, so cut-of never re-hits git here).
           :blobs-unparseable (count (filter #(:parse-error (cut-of (:sha %))) ingest-entries)))))

;; ── Query helper for consumers / the dogfood receipt (read the supersedes chain
;;    of a var at a given blob) ──────────────────────────────────────────────
(defn code-unit-id
  "The deterministic unit-id of `block-path` in blob `sha` (JOINS the ingested
   DerivedUnitRow). Pure over git + the adapter id math."
  [repo-root sha block-path]
  (get-in (cut-named-units sha (blob-text repo-root sha)) [:units block-path :unit-id]))

;; ════════════════════════════════════════════════════════════════════════════
;; ANALYZER LANE (CONTRACT §5 step 4, §12 P3; SPEC §5.2/§5.3/§5.4)  — ADDITIVE.
;; Everything above (P2's ingest + lineage lanes) is UNTOUCHED. clj-kondo analysis
;; over the HEAD tree → the desired :requires/:calls edge set → assert missing,
;; RETRACT stale (R5 current-status). The deriver actor is VERSION-FREE (R4/T1):
;; a versioned asserter forks every edge id AND loses retraction rights
;; (relation_kernel.clj:398-448), so the version rides `note`
;; "clj-atoms-v1|analyzer|<head-sha>". ALL times come from the HEAD commit's
;; committer clock (T4). Because retracts are asserted by import:code-analyzer —
;; the SAME actor that stored the edge — the retraction-rights check
;; (relation_kernel.clj:441-448: actor-id == stored asserter) passes, which is the
;; whole point of R4 (a better/newer analyzer run retracts its own stale edges).
;; ════════════════════════════════════════════════════════════════════════════

(def analyzer-asserter-actor-id
  "Version-FREE (R4/T1); the deriver version lives in `note`."
  "import:code-analyzer")
(def analyzer-asserter-type :import)
(def ns-target-kind
  "Both ends of a :requires edge. rk/->target-ref falls through to the verbatim
   target-key for unknown kinds (relation_kernel.clj:840-846), so target-key == the
   ns name string — no schema change."
  :ns)
(def var-target-kind
  "Both ends of a :calls edge — the ns-qualified var CONTINUANT (R5/T6), not a
   form-instance. Verbatim target-key == \"ns/name\"."
  :var)

(defn- app-ns? [x] (str/starts-with? (str x) "app."))

;; ── HEAD resolution (T3: analyze HEAD, never the dirty checkout; T4: commit clock)
(defn resolve-head-sha [repo-root] (str/trim (git-text repo-root ["rev-parse" "HEAD"])))

(defn head-committer-ms
  "HEAD commit's committer clock in ms (T4). 0 if `head` is not a resolvable ref."
  [repo-root head]
  (* 1000 (try (Long/parseLong (str/trim (git-text repo-root ["log" "-1" "--format=%ct" head])))
               (catch Exception _ 0))))

(defn head-blob-sha
  "Blob sha of `path` at `head` (tests + evidence joins). nil if absent."
  [repo-root head path]
  (let [s (str/trim (git-text repo-root ["rev-parse" (str head ":" path)]))]
    (when-not (str/blank? s) s)))

(defn head-code-blobs
  "{repo-rel-path -> blob-sha} for HEAD's `.clj/.cljc/.cljs` under src/+test/,
   deny-filtered by PATH (R6/T10) and optionally `path-filter` (a repo-rel-path
   predicate; nil = whole HEAD tree = the R5 analyzer scope; tests scope it like
   code-sync!'s :commit-filter)."
  [repo-root head deny? path-filter]
  (into {}
        (keep (fn [l]
                (let [[meta path] (str/split l #"\t" 2)
                      parts (str/split (str meta) #"\s+")]
                  (when (and path (code-path? path) (>= (count parts) 3)
                             (not (deny? path))
                             (or (nil? path-filter) (path-filter path)))
                    [path (nth parts 2)]))))
        (str/split-lines (git-text repo-root ["ls-tree" "-r" head "--" "src" "test"]))))

(defn- fresh-temp-dir [prefix]
  (str (java.nio.file.Files/createTempDirectory
        prefix (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- delete-recursively!
  "P1 (DIFF_FALSIFICATION): remove a materialized temp tree (post-order: file-seq is
   pre-order, so reverse deletes children before parents) so analyzer runs don't
   accumulate /tmp/code-atoms-head* dirs. Best-effort; a leftover file never fails a
   sync. (The kondo config dir is a `defonce` delay — created once, intentionally kept.)"
  [dir]
  (when dir
    (let [root (io/file dir)]
      (when (.exists root)
        (doseq [f (reverse (file-seq root))] (.delete f))))))

(defn materialize-head-tree!
  "Write each HEAD code blob's RAW bytes under `dir` at its repo path so clj-kondo lints
   HEAD, never the (uncommitted-file-carrying) checkout (T3). cat-file reads the OBJECT
   STORE, so a denied path's bytes never leave git (deny already excluded them from
   `path->sha`). F5: a per-file git failure isolates ONE blob (counted + skipped — never
   a silent empty file kondo would mis-analyze). Returns {:path->sha <WRITTEN subset>
   :git-failures N}.

   N4 (GATE_REVIEW 2026-07-09): `dir` is minted by the CALLER (analyzer-sync!) BEFORE its
   try/finally and passed in, so a throw MID-materialization (a non-git io/copy or mkdirs
   failure) still reaches the caller's finally-delete. Pre-fix this fn minted its own dir
   OUTSIDE that try, so such a throw leaked the temp tree (§6 corner)."
  [repo-root dir path->sha]
  (let [failures (atom 0)
        written (reduce-kv
                 (fn [m path sha]
                   (try
                     (let [f (io/file dir path)]
                       (.mkdirs (.getParentFile f))
                       (io/copy (git-bytes repo-root ["cat-file" "blob" sha]) f)
                       (assoc m path sha))
                     (catch Exception e
                       (if (git-failure? e) (do (swap! failures inc) m) (throw e)))))
                 {} path->sha)]
    {:path->sha written :git-failures @failures}))

;; ── clj-kondo config: Rama 1.6.0 ships its own hooks (P0 §B.4). Read them from the
;;    CLASSPATH (io/resource — no jar-path assumption) into a config dir with
;;    {:config-paths ["com.rpl/rama"]}. This collapses fabricated dataflow var-usages
;;    193→6 and findings 125→5 while PRESERVING the G7 floor (the 5 residual
;;    $$-PState unresolved symbols are ignorable). Without it, blindly mapping the
;;    193 fabricated bindings to edges is trap T5 made concrete.
(def ^:private rama-kondo-export-files
  ["config.edn" "com/rpl/utils.clj" "com/rpl/errors.clj" "com/rpl/rama_hooks.clj"])

(defn- build-kondo-config-dir! []
  (let [cfg (fresh-temp-dir "code-atoms-kondo-cfg")
        base "clj-kondo.exports/com.rpl/rama/"]
    (doseq [rel rama-kondo-export-files]
      (when-let [res (io/resource (str base rel))]
        (let [dst (io/file cfg "com.rpl" "rama" rel)]
          (.mkdirs (.getParentFile dst))
          (with-open [in (io/input-stream res)] (io/copy in dst)))))
    (spit (io/file cfg "config.edn") (pr-str {:config-paths ["com.rpl/rama"]}))
    cfg))

(defonce ^:private kondo-config-dir (delay (build-kondo-config-dir!)))

(defn run-analysis
  "clj-kondo analysis over `tree-dir` with Rama's shipped hooks active (P0 §b recipe:
   run! + {:output {:analysis true}} + :config-dir). Quiet (run! returns, never prints)."
  [tree-dir]
  (kondo/run! {:lint [tree-dir]
               :config {:output {:analysis true}}
               :config-dir @kondo-config-dir}))

;; ── row → enclosing top-level unit (evidence anchors, SPEC §5.3) ──────────────
(defn line-start-offsets
  "Vector of each line's start as a UTF-16 offset (index = row-1). A usage row's
   line-start lies inside its enclosing top-level form's [start,end) span (forms
   begin at col 0 and span whole lines), so mapping by line-start is robust to
   astral-char column drift (the one astral char in the repo is in server_jetty.clj)."
  [text]
  (let [t (str text)]
    (loop [i 0 starts (transient [0])]
      (if-let [nl (str/index-of t "\n" i)]
        (recur (inc (long nl)) (conj! starts (inc (long nl))))
        (persistent! starts)))))

(defn- head-file-context
  "Per calling FILE: {:object-key :source-id :units :line-starts}. text is the HEAD
   blob text (UTF-8 decode of the SAME bytes code-sync! ingested), so object-key /
   source-id JOIN the ingested rows and derived-unit anchor ids resolve. `:units`
   are ALL top-level forms (named + unnamed) for containment; anchors exist for all
   (clojure_adapter/source-materialization mints a SourceAnchorRow per unit)."
  [repo-root path sha]
  (let [text       (blob-text repo-root sha)
        object-key (oc/object-key-for (str "git-blob:" sha) (oc/source-hash text))]
    {:object-key  object-key
     :source-id   (oc/source-id-for-object-key object-key)
     :units       (:units (adapter/clojure-form-v0 text))
     :line-starts (line-start-offsets text)}))

(defn- row->offset [ctx row]
  (let [ls (:line-starts ctx)]
    (if (seq ls) (nth ls (dec (long row)) (long (peek ls))) 0)))

(defn enclosing-unit-block-path
  "block-path of the top-level unit whose UTF-16 span contains `offset`, or nil
   (comment/gap/unmappable → the usage is SKIPPED + counted, never guessed). Units
   are non-overlapping top-level forms, so at most one matches."
  [units offset]
  (let [o (long offset)]
    (some (fn [{:keys [block-path start-offset end-offset]}]
            (when (and (<= (long start-offset) o) (< o (long end-offset))) block-path))
          units)))

(defn- rel-path-of
  "Strip the materialization tempdir prefix off a clj-kondo :filename → repo-rel path."
  [tree-dir filename]
  (let [s (str filename) prefix (str tree-dir "/")]
    (if (str/starts-with? s prefix)
      (subs s (count prefix))
      (or (some (fn [seg] (when-let [i (str/index-of s seg)] (subs s i))) ["src/" "test/"]) s))))

(defn derive-desired-edges
  "PURE over (analysis, ctx-fn, tree-dir, note): the desired :requires + :calls edge
   set at HEAD + derivation stats. `ctx-fn`: repo-rel-path -> head-file-context.
     :requires (SPEC §5.2) — one edge per distinct (app.* from-ns → to-ns) from
       :namespace-usages; self-ns skipped; target-kind :ns both ends; ALL deps count
       — the app.* allowlist is FROM-side only (§5.2 covers non-app targets like
       com.rpl.rama / clojure.string). Evidence = the ns form's anchor (best-effort).
     :calls (SPEC §5.3, T5/T6) — from :var-usages, KEEP ONLY app.* :to (the P0 §B.5
       allowlist — never map var-usages → edges blindly; baseline garbage = 193 on the
       specimen alone). CONTINUANT grain: one edge per distinct (from-var → to-var),
       from = ns-qualified :from-var, to = ns-qualified :to/:name. Self-var (recursion)
       skipped; within-file cross-var KEPT. Evidence = the calling form's anchor +
       the calling file's HEAD source-id (representative = the lowest call-site row).
   nil :from-var, or a row mapping to no enclosing unit, is SKIPPED + counted in
   :usages-unmapped — never guessed."
  [analysis ctx-fn tree-dir note]
  (let [nus (:namespace-usages analysis)
        vus (:var-usages analysis)
        ;; ── :requires ──
        req-map
        (reduce
         (fn [m nu]
           (let [from-ns (str (:from nu)) to-ns (str (:to nu))]
             (if (and (app-ns? from-ns) (not= from-ns to-ns) (not (contains? m [from-ns to-ns])))
               (let [ctx    (ctx-fn (rel-path-of tree-dir (:filename nu)))
                     bp     (when ctx (enclosing-unit-block-path (:units ctx) (row->offset ctx (:row nu))))
                     anchor (when (and ctx bp) (oc/source-anchor-id (adapter/derived-unit-id (:object-key ctx) bp)))]
                 (assoc m [from-ns to-ns]
                        {:kind :requires
                         :from-ref (rk/->target-ref ns-target-kind from-ns)
                         :to-ref   (rk/->target-ref ns-target-kind to-ns)
                         :note note
                         :evidence-source-id (when ctx (:source-id ctx))
                         :evidence-anchor-id anchor}))
               m)))
         {} nus)
        ;; ── :calls ──
        call-acc
        (reduce
         (fn [acc vu]
           (let [from-ns (str (:from vu)) to-ns (str (:to vu)) nm (str (:name vu))
                 from-var (:from-var vu)]
             (cond
               (not (app-ns? from-ns)) acc                          ; not our code (defensive)
               (not (app-ns? to-ns))    (update acc :dropped inc)    ; T5 allowlist: drop non-app :to
               (nil? from-var)          (update acc :unmapped inc)   ; no caller continuant → skip
               :else
               (let [from-q (str from-ns "/" from-var) to-q (str to-ns "/" nm)]
                 (if (= from-q to-q)
                   acc                                               ; self-var (recursion) skipped
                   (let [ctx (ctx-fn (rel-path-of tree-dir (:filename vu)))
                         bp  (when ctx (enclosing-unit-block-path (:units ctx) (row->offset ctx (:row vu))))]
                     (if (nil? bp)
                       (update acc :unmapped inc)                    ; comment/gap/unmappable → skip
                       (let [row  (long (:row vu))
                             prev (get-in acc [:pairs [from-q to-q]])]
                         (if (and prev (<= (long (:row prev)) row))
                           acc                                       ; keep lowest-row representative
                           (assoc-in acc [:pairs [from-q to-q]]
                                     {:row row
                                      :anchor (oc/source-anchor-id (adapter/derived-unit-id (:object-key ctx) bp))
                                      :src    (:source-id ctx)}))))))))))
         {:pairs {} :dropped 0 :unmapped 0}
         vus)
        call-edges (mapv (fn [[[from-q to-q] {:keys [anchor src]}]]
                           {:kind :calls
                            :from-ref (rk/->target-ref var-target-kind from-q)
                            :to-ref   (rk/->target-ref var-target-kind to-q)
                            :note note
                            :evidence-source-id src
                            :evidence-anchor-id anchor})
                         (:pairs call-acc))]
    {:edges (into (vec (vals req-map)) call-edges)
     :stats {:ns-usages-seen            (count (filter #(app-ns? (:from %)) nus))
             :var-usages-seen           (count (filter #(app-ns? (:from %)) vus))
             :var-usages-dropped-nonapp (:dropped call-acc)
             :usages-unmapped           (:unmapped call-acc)}}))

(defn- reconcile-edges!
  "Current-status reconciliation (R5). Asserts desired-not-present, RETRACTS
   present-not-desired; every retract's envelope actor == the stored asserter
   (import:code-analyzer), so the retraction-rights guard passes (R4).

   F3 (transition-unique keys): the assert key is `code:<rid>:<head>:t<N>` and the
   retract key `…:t<N>:retract`, where N = the count of prior status transitions
   (`read-relation-detail` history, ground truth, replay-stable). The pre-fix key
   `code:<rid>:<head>` was HEAD-scoped only, so a retract-then-re-desire at a FIXED
   head reused the ORIGINAL assert's journal entry → the re-assert was dropped, the
   edge stayed :retracted, while the stat claimed +1. A transition-unique key makes
   each genuine transition a fresh journal entry (so the stat is honest by
   construction), and a true byte-identical re-run reads the SAME count → converges,
   0 events (G9/T4). N is read ONLY for rids that already have a row (re-asserts +
   retracts) — a fresh first sync issues zero detail reads.

   F1 (vanished-endpoint retract): the target read alone can only see edges sharing a
   target-key with a DESIRED edge, so an analyzer edge whose BOTH endpoints left HEAD
   (delete a self-contained file) is invisible → never retracted → a persistent lie
   (violates R5, 'the map must not lie'). `prior-basis` = the LAST pass's desired
   set (rid→refs), an in-memory CALLER-OWNED cluster-scoped record (NOT a durable
   file — durable side-state × the ephemeral create-ipc cluster poisons the next boot,
   implementation-quirks). A basis edge no longer desired and not currently asserted
   over any reachable tk is a vanished-endpoint edge → confirmed :asserted via
   read-relation-detail, then retracted with refs from the basis. A NIL basis (no prior
   pass ran) → :reconcile-basis-missing 1 (degrades to the target-key-only reconcile;
   cross-boot retraction rides Sid's durability fork, deferred). N3 (GATE_REVIEW
   2026-07-09): an EMPTY {} basis is NOT missing — a prior pass that legitimately desired
   zero edges stored {}, which is a real prior pass; missing is keyed on nil?, never
   empty? (the pre-fix (empty? prior) reported a phantom 1 after any zero-edge pass).
   Returns the stat quad + :reconcile-basis-missing + :desired-basis (caller stores it)."
  [runtime edges head head-ms prior-basis]
  (let [analyzer analyzer-asserter-actor-id
        desired-by-rid (into {} (map (fn [e]
                                       (let [rid (rk/relation-id-for (:kind e) (:from-ref e) (:to-ref e) analyzer)]
                                         [rid (assoc e :relation-id rid)])))
                             edges)
        involved-tks (into #{} (mapcat (fn [e] [(:target-key (:from-ref e)) (:target-key (:to-ref e))]) edges))
        ;; F3: include retracted rows so a re-asserted edge's prior (retracted) row is
        ;; visible → it is a re-assert (transition count read), not a mistaken new assert.
        by-target (rk/read-relations-for-targets runtime (vec involved-tks) [:requires :calls] true)
        analyzer-rows (into {} (comp (mapcat val)
                                     (filter #(= analyzer (:asserter-actor-id %)))
                                     (map (fn [row] [(:relation-id row) row])))
                            by-target)
        current-asserted      (into {} (filter #(= :asserted (:relation-status (val %))) analyzer-rows))
        current-retracted-ids (into #{} (keep (fn [[rid row]] (when (= :retracted (:relation-status row)) rid)) analyzer-rows))
        prior         (or prior-basis {})
        desired-rids  (set (keys desired-by-rid))
        asserted-rids (set (keys current-asserted))
        transitions   (fn [rid] (count (:history (rk/read-relation-detail runtime rid))))
        akey (fn [rid n] (str "code:" rid ":" head ":t" n))
        rkey (fn [rid n] (str "code:" rid ":" head ":t" n ":retract"))
        to-assert          (vec (remove asserted-rids desired-rids))          ; brand-new OR re-asserted
        to-retract-current (vec (remove desired-rids asserted-rids))          ; today's set (rows in hand)
        ;; F1: prior-basis edges no longer desired whose endpoints both left HEAD →
        ;; unreachable by the target read. Confirm :asserted (honest retract count) then
        ;; retract from basis refs.
        basis-candidates (remove (some-fn desired-rids asserted-rids) (keys prior))
        basis-retracts (into [] (keep (fn [rid]
                                        (let [d (rk/read-relation-detail runtime rid)]
                                          (when (= :asserted (:relation-status (:row d)))
                                            {:rid rid :n (count (:history d)) :b (prior rid)})))
                                      basis-candidates))]
    (doseq [rid to-assert]
      (let [e (desired-by-rid rid)
            n (if (contains? current-retracted-ids rid) (transitions rid) 0)   ; re-assert → real count; new → 0
            k (akey rid n)]
        (rk/append-relation-request!
         runtime
         (rk/assert-request {:kind (:kind e) :from (:from-ref e) :to (:to-ref e)
                             :asserter-actor-id analyzer :asserter-type analyzer-asserter-type
                             :asserted-at-ms head-ms :sent-at-ms head-ms
                             :request-id k :idempotency-key k
                             :evidence-source-id (:evidence-source-id e)
                             :evidence-anchor-id (:evidence-anchor-id e)
                             :note (:note e)}))))
    (doseq [rid to-retract-current]
      (let [row (current-asserted rid) k (rkey rid (transitions rid))]
        (rk/append-relation-request!
         runtime
         (rk/retract-request {:kind (:relation-kind row) :from (:from row) :to (:to row)
                              :asserter-actor-id analyzer :asserter-type analyzer-asserter-type
                              :actor {:actor/id analyzer :actor/type analyzer-asserter-type}
                              :asserted-at-ms head-ms :sent-at-ms head-ms
                              :request-id k :idempotency-key k}))))
    (doseq [{:keys [rid n b]} basis-retracts]
      (let [k (rkey rid n)]
        (rk/append-relation-request!
         runtime
         (rk/retract-request {:kind (:kind b) :from (:from-ref b) :to (:to-ref b)
                              :asserter-actor-id analyzer :asserter-type analyzer-asserter-type
                              :actor {:actor/id analyzer :actor/type analyzer-asserter-type}
                              :asserted-at-ms head-ms :sent-at-ms head-ms
                              :request-id k :idempotency-key k}))))
    ;; (c) SETTLE descriptor (GATE_REVIEW 2026-07-09 doubt 2): the appends above use
    ;; :append-ack (durable, NOT materialized), so a back-to-back analyzer-sync!
    ;; could read a stale transition count and mint an already-journaled key (the
    ;; flip dropped). analyzer-sync! awaits THIS pass's own appends materialized at
    ;; EXIT; here we hand it exactly what to await — the touched target-keys + each
    ;; appended rid's terminal status (a fully-converged pass appended nothing → {}).
    (let [settle-retracted (into (set to-retract-current) (map :rid basis-retracts))
          settle-expected  (merge (zipmap to-assert (repeat :asserted))
                                  (zipmap settle-retracted (repeat :retracted)))
          settle-tks (vec (into #{}
                                (concat
                                 (mapcat (fn [rid] (let [e (desired-by-rid rid)]
                                                     [(:target-key (:from-ref e)) (:target-key (:to-ref e))]))
                                         to-assert)
                                 (mapcat (fn [rid] (let [row (current-asserted rid)]
                                                     [(:target-key (:from row)) (:target-key (:to row))]))
                                         to-retract-current)
                                 (mapcat (fn [{:keys [b]}]
                                           [(:target-key (:from-ref b)) (:target-key (:to-ref b))])
                                         basis-retracts))))]
      {:requires-asserted (count (filter #(= :requires (:kind (desired-by-rid %))) to-assert))
       :calls-asserted    (count (filter #(= :calls (:kind (desired-by-rid %))) to-assert))
       :retracted         (+ (count to-retract-current) (count basis-retracts))
       :converged         (count (filter asserted-rids desired-rids))
       ;; N3: nil? not empty? — an EMPTY {} basis is a prior pass that desired zero edges
       ;; (present, → 0); only a NIL basis (never run) is missing (→ 1). `prior` is
       ;; (or prior-basis {}) for basis-candidates; the missing signal reads the raw arg.
       :reconcile-basis-missing (if (nil? prior-basis) 1 0)
       ;; the new basis to thread forward: THIS pass's desired set (rid → refs). The
       ;; caller stores it in its cluster-scoped atom (analyzer-sync! :analyzer-basis).
       :desired-basis (into {} (map (fn [[rid e]] [rid (select-keys e [:kind :from-ref :to-ref])])) desired-by-rid)
       :settle {:target-keys settle-tks :expected settle-expected}})))

(defn analyzer-sync!
  "Analyzer lane entry (CONTRACT §5 step 4). cfg:
     :runtime            OC + relation-kernel handles (trail-view runtime map)
     :repo-root          repo path (never hardcoded)
     :head-override      full sha to treat as HEAD (tests; default = git HEAD)
     :desired-override   seq of edge-specs to reconcile DIRECTLY, skipping kondo
                         (tests only, G10 — proves version-free retraction end-to-end)
     :path-filter        repo-rel-path predicate scoping the HEAD tree analyzed
                         (tests; nil = whole HEAD tree = R5 scope; like code-sync!'s
                         :commit-filter)
     :deny-list-override optional denied-path suffix set (TESTS ONLY, R6)
     :analyzer-basis     optional CALLER-OWNED atom, cluster-scoped (minted alongside
                         the runtime/cluster), holding {rid → edge-refs} of the last
                         pass's desired set. Threaded so a full current-status reconcile
                         (R5) can retract an edge whose BOTH endpoints left HEAD (F1) —
                         such an edge shares no target-key with any desired edge, so the
                         target read can't see it; the basis remembers it. Deliberately
                         NOT a durable file (implementation-quirks: durable side-state ×
                         the ephemeral create-ipc cluster poisons the next boot); the
                         atom lives + dies with the cluster, the correct retraction scope.
                         N6 (SCOPE, GATE_REVIEW §4): use ONE basis atom per (repo,
                         path-filter) SCOPE — the basis records HEAD's desired set for the
                         analyzed scope, so reusing one atom across DIFFERENT path-filter
                         scopes over-retracts edges that merely LEFT the analyzed scope
                         (not HEAD). The production default (whole tree, one atom minted
                         with the runtime) is consistent by construction.
                         NIL (never run) → :reconcile-basis-missing 1; an EMPTY {} basis
                         (a prior pass desired zero edges) is PRESENT → 0 (N3).
   Returns a stats map — every bound named, NO silent caps (F4/F5):
     {:ns-usages-seen :var-usages-seen :var-usages-dropped-nonapp :usages-unmapped
      :git-failures :requires-asserted :calls-asserted :retracted :converged
      :reconcile-basis-missing :unresolved-residual}"
  [{:keys [runtime repo-root head-override desired-override path-filter deny-list-override analyzer-basis]}]
  (let [deny?   (deny-fn deny-list-override)
        head    (or head-override (resolve-head-sha repo-root))
        head-ms (head-committer-ms repo-root head)
        note    (str deriver-version "|analyzer|" head)
        prior-basis (when analyzer-basis @analyzer-basis)
        derived (if desired-override
                  {:edges (vec desired-override)
                   :stats {:ns-usages-seen 0 :var-usages-seen 0
                           :var-usages-dropped-nonapp 0 :usages-unmapped 0 :git-failures 0}
                   :unresolved-residual 0}
                  ;; N4 (GATE_REVIEW §6): mint the temp dir BEFORE the try so
                  ;; materialize-head-tree!'s own writes are inside the finally-delete's
                  ;; guard — a non-git throw mid-materialization no longer leaks the tree.
                  ;; (fresh-temp-dir either creates+returns or throws having created
                  ;; nothing, so minting before the try never leaks either.)
                  (let [head-paths (head-code-blobs repo-root head deny? path-filter)
                        dir (fresh-temp-dir "code-atoms-head")]
                    (try
                      (let [{:keys [path->sha git-failures]} (materialize-head-tree! repo-root dir head-paths)
                            {:keys [analysis findings]} (run-analysis dir)
                            ctx-cache (atom {})
                            ctx-fn (fn [path]
                                     (or (@ctx-cache path)
                                         (when-let [sha (path->sha path)]
                                           (let [c (head-file-context repo-root path sha)]
                                             (swap! ctx-cache assoc path c) c))))
                            {:keys [edges stats]} (derive-desired-edges analysis ctx-fn dir note)]
                        {:edges edges
                         :stats (assoc stats :git-failures git-failures)
                         :unresolved-residual (count (filter #(= :unresolved-symbol (:type %)) findings))})
                      ;; P1+N4: delete the materialized HEAD tree on success OR ANY throw
                      ;; (materialize-head-tree!'s own throw is now inside this try too).
                      (finally (delete-recursively! dir)))))
        reconcile (reconcile-edges! runtime (:edges derived) head head-ms prior-basis)
        ;; (c) SETTLE AT EXIT (GATE_REVIEW 2026-07-09 doubt 2): reconcile appended with
        ;; :append-ack (durable, NOT materialized). Await THIS pass's own appends
        ;; materialized to their terminal status before returning, so a back-to-back
        ;; analyzer-sync! never reads a stale transition count and drops a flip. ONE
        ;; batched poll over the touched target-keys — O(polls), not O(edges); a
        ;; fully-converged pass appended nothing (:expected {}) and skips the wait.
        {:keys [target-keys expected]} (:settle reconcile)
        _ (when (seq expected)
            (rk/await-relation
             (fn [] (->> (rk/read-relations-for-targets runtime target-keys [:requires :calls] true)
                         (mapcat val)
                         (filter #(= analyzer-asserter-actor-id (:asserter-actor-id %)))
                         (map (juxt :relation-id :relation-status))
                         (into {})))
             (fn [rid->status] (every? (fn [[rid st]] (= st (rid->status rid))) expected))
             15000))]
    (when analyzer-basis (reset! analyzer-basis (:desired-basis reconcile)))
    (merge (:stats derived)
           (dissoc reconcile :desired-basis :settle)   ; :settle is internal wiring, not a stat
           {:unresolved-residual (:unresolved-residual derived)})))

;; ── N5 (GATE_REVIEW 2026-07-09) — numeric-aware history DISPLAY ───────────────
(defn- reconcile-transition-index
  "The analyzer transition number N parsed from a reconcile request-id
   `code:<rid>:<head>:t<N>` (assert, akey) or `code:<rid>:<head>:t<N>:retract`
   (retract, rkey). The regex anchors at end-of-string, so the trailing t<N> is the
   real transition even if <rid> happens to contain a `:t…` fragment. nil for any id
   without a t<N> tail (e.g. a lineage :supersedes edge `code:<rid>:<child-sha>`)."
  [request-id]
  (when-let [m (re-find #":t(\d+)(?::retract)?$" (str request-id))]
    (Long/parseLong (second m))))

(defn relation-history-display
  "A relation's status history in numeric-aware DISPLAY order (consumers/receipts).
   read-relation-detail returns :history in the status-log's order-key order —
   fixed-width-order-key(changed-at-ms, request-id) — so at a FIXED head (constant
   ts) it sorts by request-id LEXICALLY, and an analyzer relation with >=10
   transitions shows t10 before t2 (N5). This re-sorts by (changed-at-ms, the t<N>
   transition index), so t2 precedes t10. DISPLAY-ONLY: it reorders the SAME rows —
   count is preserved, and the authoritative current status is read from
   read-relation-detail's :row (unchanged), never from this ordering."
  [runtime relation-id]
  (->> (:history (rk/read-relation-detail runtime relation-id))
       (sort-by (juxt (fn [r] (long (or (:changed-at-ms r) 0)))
                      (fn [r] (or (reconcile-transition-index (:request-id r)) 0))))
       vec))
