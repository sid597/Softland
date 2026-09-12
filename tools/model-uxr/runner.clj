(ns model-uxr.runner
  "Model-UXR benchmark v0 — DRY-RUN skeleton. NO live API calls in v0.

  Spec: history/docs/model-uxr/{SPEC,QUESTIONS,ABLATIONS}.md

  What it does today:
    * loads the question bank by extracting the fenced `edn` blocks from
      QUESTIONS.md (single source of truth — the .md IS the machine-readable
      bank, no drift),
    * loads a plain-EDN subjects config (subjects.edn) if present, else falls
      back to the embedded `default-config` template,
    * assembles a per-ablation prompt bundle from a snapshot dir (include/exclude
      globs), with a contamination guard that REFUSES to emit any prompt
      containing an answer-key (build/model-uxr) path,
    * `--dry-run` prints the first assembled prompt + a mock score row.

  What it does NOT do in v0:
    * make any network call. `call-openai-compatible` and `call-anthropic` are
      STUBS that throw. When wired live, keys come from ENVIRONMENT VARIABLES
      ONLY (System/getenv), NEVER from src/app/server/env.clj or any file.

  Runtimes: `clojure -M tools/model-uxr/runner.clj --dry-run`
            `bb      tools/model-uxr/runner.clj --dry-run`
  (plain clojure.core + clojure.edn/string/java.io only — no added deps.)"
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data.json :as json]))

;; ---------------------------------------------------------------------------
;; Defaults / paths
;; ---------------------------------------------------------------------------

(def default-questions-path
  "history/docs/model-uxr/QUESTIONS.md")

(def default-config-path "tools/model-uxr/subjects.edn")

(def default-snapshot-dir "snapshots/current")

(def metric-keys
  "The five pre-registered metrics (SPEC §2). Filled by the grader in a live
  run; :NA in dry-run."
  [:tokens-to-orientation :wrong-authority :re-derivation-ratio
   :join-success :invented-structure])

;; Ablation file operations (ABLATIONS.md). Globs support * (one segment) and
;; ** (any depth). `**/build/model-uxr/**` is force-added to EVERY exclude by
;; code (SPEC §6) regardless of config.
(def default-ablations
  {:A0 {:include ["CLAUDE.md" "docs/current-mental-model/**"
                  "docs/sessions/next-prompt.md" "vision/LOG.md" "relations/**"]
        :exclude ["bundles/**"]}
   :A1 {:include ["CLAUDE.md" "docs/current-mental-model/**"
                  "docs/sessions/next-prompt.md" "vision/LOG.md" "relations/**"]
        :exclude ["bundles/**" "docs/current-mental-model/decisions.md"]}
   :A2 {:include ["CLAUDE.md" "docs/current-mental-model/**"
                  "vision/LOG.md" "relations/**"]
        :exclude ["bundles/**" "docs/sessions/next-prompt.md"]}
   :A3 {:include ["CLAUDE.md" "docs/current-mental-model/**"
                  "docs/sessions/next-prompt.md" "vision/LOG.md"]
        :exclude ["bundles/**" "relations/**"]}
   :A4 {:include ["bundles/**"]
        :exclude []}})

;; The embedded config template. Operators copy this into `subjects.edn` and
;; fill model ids. The runner loads subjects.edn if it exists, else uses this.
(def default-config
  {:snapshot-dir default-snapshot-dir
   :questions    default-questions-path
   :tokenizer    :ws-proxy            ; frozen with the bank; :ws-proxy = words * 1.3
   :grader   {:provider :anthropic :model "<set-me>" :key-env "ANTHROPIC_API_KEY"}
   :subjects [{:id "nemotron-local" :provider :openai-compatible
               :endpoint "http://localhost:8080/v1" :model "<set-me>"
               :key-env "LOCAL_LLM_API_KEY" :max-tokens 4096 :temperature 0}
              {:id "frontier" :provider :anthropic
               :endpoint "https://api.anthropic.com" :model "<set-me>"
               :key-env "ANTHROPIC_API_KEY" :max-tokens 4096 :temperature 0}]
   :ablations default-ablations
   :run {:seed 0 :repeats 1 :human-audit-frac 0.15 :output "runs/"}})

(def system-preamble
  (str "You are given read access to a frozen snapshot of the Softland "
       "knowledge land. Answer the question using ONLY the material provided "
       "below. Cite the file (and section) your answer derives from. If the "
       "answer is NOT derivable from the provided material, respond exactly: "
       "NOT DERIVABLE FROM PROVIDED MATERIAL, then explain what would be needed. "
       "Do not invent files, decisions, relations, commits, or countersigns."))

;; ---------------------------------------------------------------------------
;; Question bank — parse fenced edn blocks from QUESTIONS.md
;; ---------------------------------------------------------------------------

(def ^:private edn-fence-re #"(?s)```edn\s*(.*?)```")

(defn parse-question-blocks
  "Extract every fenced ```edn ... ``` block from markdown, read each as EDN."
  [md-text]
  (->> (re-seq edn-fence-re md-text)
       (map (comp edn/read-string str/trim second))
       vec))

(defn load-questions
  [md-path]
  (let [qs (parse-question-blocks (slurp md-path))]
    (when (empty? qs)
      (throw (ex-info "no question edn blocks parsed" {:path md-path})))
    qs))

(defn bank-summary
  [qs]
  {:total       (count qs)
   :by-category (into (sorted-map) (frequencies (map :category qs)))
   :spine-gated (count (filter :spine-gated? qs))})

;; ---------------------------------------------------------------------------
;; Config
;; ---------------------------------------------------------------------------

(defn load-config
  [edn-path]
  (if (and edn-path (.exists (io/file edn-path)))
    (edn/read-string (slurp edn-path))
    default-config))

;; ---------------------------------------------------------------------------
;; Snapshot assembly (glob include/exclude)
;; ---------------------------------------------------------------------------

(defn- glob->re
  "Tiny glob -> regex. * matches one path segment, ** matches any depth."
  [g]
  (re-pattern
    (-> g
        (str/replace "." "\\.")
        (str/replace "**" "__GLOBSTAR__")
        (str/replace "*" "[^/]*")
        (str/replace "__GLOBSTAR__" ".*"))))

(def ^:private answer-key-glob "**/build/model-uxr/**")

(defn- match-any?
  [regexes ^String p]
  (boolean (some #(re-matches % p) regexes)))

(defn- selected?
  "Path p is selected iff it matches an include and no exclude. The answer-key
  glob is force-added to excludes (SPEC §6)."
  [ablation-spec p]
  (let [incl (map glob->re (:include ablation-spec))
        excl (map glob->re (conj (vec (:exclude ablation-spec)) answer-key-glob))]
    (and (match-any? incl p) (not (match-any? excl p)))))

(defn- rel-path
  [root ^java.io.File f]
  (str (.relativize (.toPath (io/file root)) (.toPath f))))

;; A tiny in-memory fixture so `--dry-run` always produces output even with no
;; snapshot dir on disk. Includes a build/model-uxr canary that MUST be excluded
;; by the contamination guard — if it ever leaks, dry-run throws (self-test).
;; The retired BETS.md entry represents historical snapshot material. It is
;; not a current repo pointer; vision orientation starts at docs/carry-on.md.
(def ^:private fixture-files
  {"CLAUDE.md"
   (str "## Decision Log — BINDING\n"
        "docs/current-mental-model/decisions.md is the binding decision log.\n"
        "The active work handoff is docs/sessions/next-prompt.md.")
   "docs/current-mental-model/decisions.md"
   "## D-001 CLOSED — Arbiter rule ... (fixture excerpt)"
   "docs/current-mental-model/BETS.md"
   "### H1 — ACTIVE · The trail view beats the wall ... (fixture excerpt)"
   "docs/sessions/next-prompt.md"
   "# Active work package: trail-view WP1 ... (baton fixture excerpt)"
   "vision/LOG.md"
   "# Vision Log — Sid, verbatim ... (fixture excerpt)"
   "relations/relation-edges.edn"
   "[{:relation-id \"rel:demo\" :kind :produced :from {:kind :conversation}}]"
   "bundles/oc_doc_demo.txt"
   ";; trail-text v0 ... (View-3 projection fixture excerpt)"
   "docs/current-mental-model/build/model-uxr/SPEC.md"
   "ANSWER KEY — must never appear in a subject prompt (contamination canary)"})

(defn resolve-material
  "Return a sorted seq of [relpath content] selected for the ablation. Uses the
  real snapshot dir when it exists, else the in-memory fixture."
  [snapshot-dir ablation-spec]
  (let [real? (and snapshot-dir (.isDirectory (io/file snapshot-dir)))
        pairs (if real?
                (->> (file-seq (io/file snapshot-dir))
                     (filter #(.isFile ^java.io.File %))
                     (map (fn [f] [(rel-path snapshot-dir f) (delay (slurp f))])))
                (map (fn [[p c]] [p (delay c)]) fixture-files))]
    (->> pairs
         (filter (fn [[p _]] (selected? ablation-spec p)))
         (sort-by first))))

(defn- guard-no-answer-key!
  [material]
  (when-let [leak (seq (filter (fn [[p _]] (re-find #"build/model-uxr" p)) material))]
    (throw (ex-info "CONTAMINATION: answer-key path leaked into snapshot bundle"
                    {:leaked (map first leak)}))))

(defn assemble-prompt
  "Assemble the cold-use prompt for one (ablation, question) over a snapshot."
  [snapshot-dir ablation-spec question]
  (let [material (resolve-material snapshot-dir ablation-spec)]
    (guard-no-answer-key! material)                 ; SPEC §6 — a guard, not a hope
    (str system-preamble "\n\n"
         (->> material
              (map (fn [[p c]] (str "===== FILE: " p " =====\n" @c)))
              (str/join "\n\n"))
         "\n\n===== QUESTION =====\n" (:question question) "\n")))

;; ---------------------------------------------------------------------------
;; Scoring (mock in dry-run; grader fills metrics in a live run — SPEC §4)
;; ---------------------------------------------------------------------------

(defn mock-score-row
  [subject-id ablation question]
  (merge {:subject      subject-id
          :ablation     ablation
          :qid          (:id question)
          :category     (:category question)
          :spine-gated? (boolean (:spine-gated? question))
          :verdict      :mock
          :note         "dry-run: no live call, no grade; metrics are grader-filled in a live run"}
         (zipmap metric-keys (repeat :NA))))

;; ---------------------------------------------------------------------------
;; Live calls — STUBBED in v0. Keys from ENV ONLY, never env.clj.
;; ---------------------------------------------------------------------------

(defn- api-key
  "Read the subject/grader API key from an environment variable ONLY.
  NEVER read src/app/server/env.clj (or any file) for keys."
  [key-env]
  (or (System/getenv key-env)
      (throw (ex-info (str "missing env var " key-env
                           " — keys come from the environment, never env.clj")
                      {:env key-env}))))

(defn call-openai-compatible
  "LIVE (2026-07-06, freeze-gate session). POST {endpoint}/chat/completions
  via JDK java.net.http — no added deps. The prompt already carries the
  system preamble (assemble-prompt), so it goes as ONE user message; sending
  the preamble again as a system message would double it.
  Returns {:completion .. :reasoning .. :model .. :usage .. :timings ..}.
  llama.cpp separates the reasoning channel as message.reasoning_content —
  captured, but metrics run on :completion only (MANIFEST deviation D3)."
  [subject prompt]
  (let [body (json/write-str
              (merge
               {:model       (:model subject)
                :messages    [{:role "user" :content prompt}]
                :temperature (:temperature subject 0)
                :max_tokens  (:max-tokens subject 4096)}
               ;; D9 (t4-bench 2026-07-06): extra sampling params passthrough
               ;; (e.g. llama.cpp dry_multiplier for the LM-1b rerun arm).
               ;; Never touches prompt assembly or guards.
               (:extra-params subject)))
        req  (-> (java.net.http.HttpRequest/newBuilder)
                 (.uri (java.net.URI/create (str (:endpoint subject) "/chat/completions")))
                 (.header "Content-Type" "application/json")
                 (.header "Authorization" (str "Bearer " (api-key (:key-env subject))))
                 (.timeout (java.time.Duration/ofSeconds 600))
                 (.POST (java.net.http.HttpRequest$BodyPublishers/ofString body))
                 (.build))
        client (-> (java.net.http.HttpClient/newBuilder)
                   (.connectTimeout (java.time.Duration/ofSeconds 10))
                   (.build))
        resp (.send client req (java.net.http.HttpResponse$BodyHandlers/ofString))
        code (.statusCode resp)]
    (when-not (= 200 code)
      (throw (ex-info "subject call failed" {:status code :body (subs (.body resp) 0 (min 500 (count (.body resp))))})))
    (let [parsed (json/read-str (.body resp) :key-fn keyword)
          msg    (get-in parsed [:choices 0 :message])]
      {:completion    (:content msg)
       :reasoning     (:reasoning_content msg)
       :finish-reason (get-in parsed [:choices 0 :finish_reason])
       :model         (:model parsed)
       :usage         (:usage parsed)
       :timings       (:timings parsed)})))

(defn call-anthropic
  "STUB — intentionally NOT wired this run (2026-07-06): no direct-API budget
  (Sid's ruling). The frontier subject runs as fresh Claude Code subagents on
  the subscription, orchestrated OUTSIDE this runner; its rows land in the
  same runs/ format with subject id \"opus-4.8-cc-harness\" (MANIFEST
  deviation D2). If a direct-API budget ever exists, implement per the
  claude-api skill: POST {endpoint}/v1/messages, headers x-api-key +
  anthropic-version, NO temperature (removed on Opus 4.8+), adaptive
  thinking semantics per model."
  [subject _prompt]
  (throw (ex-info "anthropic direct API disabled — no budget; frontier runs via CC-harness subagents"
                  {:subject (:id subject) :provider :anthropic})))

(defn call-subject
  "Dispatch to the right provider stub. No-op path in v0 (both stubs throw)."
  [subject prompt]
  (case (:provider subject)
    :openai-compatible (call-openai-compatible subject prompt)
    :anthropic         (call-anthropic subject prompt)
    (throw (ex-info "unknown provider" {:provider (:provider subject)}))))

;; ---------------------------------------------------------------------------
;; Manifest hash (SPEC §1 — pin the bank before any subject runs)
;; ---------------------------------------------------------------------------

(defn content-hash
  "SHA-256 hex of a string (bank-hash for the run manifest)."
  [s]
  (let [d  (java.security.MessageDigest/getInstance "SHA-256")
        bs (.digest d (.getBytes (str s) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) bs))))

;; ---------------------------------------------------------------------------
;; Dry run
;; ---------------------------------------------------------------------------

(defn- arg-val
  [args flag]
  (second (drop-while #(not= % flag) args)))

(defn dry-run
  [{:keys [questions-path config-path snapshot-dir]}]
  (let [qpath  (or questions-path default-questions-path)
        cfg    (load-config (or config-path default-config-path))
        snap   (or snapshot-dir (:snapshot-dir cfg) default-snapshot-dir)
        qs     (load-questions qpath)
        q1     (first qs)
        subj   (first (:subjects cfg))
        ablsp  (get-in cfg [:ablations :A0])
        prompt (assemble-prompt snap ablsp q1)
        real?  (.isDirectory (io/file snap))]
    (println "== Model-UXR runner v0 — DRY RUN ==")
    (println "questions :" qpath)
    (println "bank-hash :" (content-hash (slurp qpath)) "(pin this in the run MANIFEST — SPEC §1)")
    (println "bank      :" (pr-str (bank-summary qs)))
    (println "config    :" (if (.exists (io/file (or config-path default-config-path)))
                             (or config-path default-config-path)
                             "(embedded default-config)"))
    (println "snapshot  :" snap (if real? "(real dir)" "(absent -> in-memory fixture)"))
    (println "subject   :" (:id subj) "provider" (:provider subj) "(NOT called — stub)")
    (println)
    (println "-- first assembled prompt  (ablation A0 x question" (:id q1) ") --")
    (println "   included files:"
             (pr-str (map first (resolve-material snap ablsp))))
    (println "   [contamination guard passed: no build/model-uxr path in bundle]")
    (println (str "   prompt length: " (count prompt) " chars; head:"))
    (println (str "   " (str/replace (subs prompt 0 (min 900 (count prompt))) "\n" "\n   ")))
    (println "   ...<truncated>...")
    (println)
    (println "-- mock score row  (metrics graded live; :NA in dry-run) --")
    (println "  " (pr-str (mock-score-row (:id subj) :A0 q1)))
    (println)
    (println "OK: dry run complete. No network calls made. Live calls are stubbed;")
    (println "    keys come from environment variables only, never env.clj.")))

;; ---------------------------------------------------------------------------
;; Live run — one subject x ablation over the whole bank (crash-safe .ednl)
;; ---------------------------------------------------------------------------

(defn- find-subject
  [cfg id]
  (or (first (filter #(= id (:id %)) (:subjects cfg)))
      (throw (ex-info "unknown subject id" {:id id :known (map :id (:subjects cfg))}))))

(defn run-subject!
  "Run every bank question against ONE subject at ONE ablation, appending one
  EDN row per line to runs/<subject>-<ablation>.ednl (append = crash-safe;
  already-answered qids are skipped on re-run, so the command is resumable)."
  [{:keys [questions-path config-path snapshot-dir subject-id ablation out-dir]}]
  (let [cfg    (load-config (or config-path default-config-path))
        snap   (or snapshot-dir (:snapshot-dir cfg) default-snapshot-dir)
        qs     (load-questions (or questions-path default-questions-path))
        subj   (find-subject cfg subject-id)
        abl    (keyword ablation)
        ablsp  (or (get-in cfg [:ablations abl])
                   (throw (ex-info "unknown ablation" {:ablation abl})))
        outd   (io/file (or out-dir (get-in cfg [:run :output] "runs/")))
        _      (.mkdirs outd)
        outf   (io/file outd (str subject-id "-" (name abl) ".ednl"))
        done   (if (.exists outf)
                 (set (map :qid (map edn/read-string (line-seq (io/reader outf)))))
                 #{})]
    (println "== run-subject!" subject-id (name abl) "-> " (str outf))
    (println "   bank:" (count qs) "questions;" (count done) "already done (resume)")
    (doseq [q qs :when (not (done (:id q)))]
      (let [prompt (assemble-prompt snap ablsp q)
            t0     (System/currentTimeMillis)
            result (try (call-subject subj prompt)
                        (catch Exception e
                          {:error (.getMessage e) :data (ex-data e)}))
            row    (merge {:qid          (:id q)
                           :category     (:category q)
                           :spine-gated? (boolean (:spine-gated? q))
                           :subject      subject-id
                           :ablation     abl
                           :prompt-chars (count prompt)
                           :wall-ms      (- (System/currentTimeMillis) t0)
                           :ts-ms        t0}
                          result)]
        (spit outf (str (pr-str row) "\n") :append true)
        (println (format "   %-4s %s %dms %s" (:id q)
                         (if (:error row) "ERROR" "ok")
                         (:wall-ms row)
                         (if (:error row) (:error row)
                             (str (count (str (:completion row))) " chars"))))))
    (println "== done:" (str outf))))

(defn dump-prompts!
  "Materialize every (ablation x question) prompt to a file — the input for
  frontier-via-CC-harness subagents (each subagent Reads exactly ONE of these;
  reading anything else voids the row — MANIFEST deviation D2 audit rule)."
  [{:keys [questions-path config-path snapshot-dir ablation out-dir]}]
  (let [cfg    (load-config (or config-path default-config-path))
        snap   (or snapshot-dir (:snapshot-dir cfg) default-snapshot-dir)
        qs     (load-questions (or questions-path default-questions-path))
        abl    (keyword ablation)
        ablsp  (get-in cfg [:ablations abl])
        outd   (io/file (or out-dir "runs/prompts") (name abl))]
    (.mkdirs outd)
    (doseq [q qs]
      (spit (io/file outd (str (:id q) ".txt")) (assemble-prompt snap ablsp q)))
    (println "== dumped" (count qs) "prompts to" (str outd))))

(defn -main
  [& args]
  (cond
    (some #{"--dry-run"} args)
    (dry-run {:questions-path (arg-val args "--questions")
              :config-path    (arg-val args "--config")
              :snapshot-dir   (arg-val args "--snapshot")})

    (some #{"--run"} args)
    (run-subject! {:questions-path (arg-val args "--questions")
                   :config-path    (arg-val args "--config")
                   :snapshot-dir   (arg-val args "--snapshot")
                   :subject-id     (or (arg-val args "--subject")
                                       (throw (ex-info "--subject required" {})))
                   :ablation       (or (arg-val args "--ablation") "A0")
                   :out-dir        (arg-val args "--out")})

    (some #{"--dump-prompts"} args)
    (dump-prompts! {:questions-path (arg-val args "--questions")
                    :config-path    (arg-val args "--config")
                    :snapshot-dir   (arg-val args "--snapshot")
                    :ablation       (or (arg-val args "--ablation") "A0")
                    :out-dir        (arg-val args "--out")})

    :else
    (do
      (println "Model-UXR runner.")
      (println)
      (println "Usage:")
      (println "  clojure -M tools/model-uxr/runner.clj --dry-run       [--questions PATH] [--config PATH] [--snapshot DIR]")
      (println "  clojure -M tools/model-uxr/runner.clj --run           --subject ID [--ablation A0] [--out DIR]")
      (println "  clojure -M tools/model-uxr/runner.clj --dump-prompts  [--ablation A0] [--out DIR]")
      (println)
      (println "Keys come from environment variables only — never env.clj.")
      (println "anthropic provider is stubbed (no direct-API budget); frontier runs via CC-harness."))))

;; Script entry (no-op when this ns is required as a library with no CLI args).
(when (seq *command-line-args*)
  (apply -main *command-line-args*))
