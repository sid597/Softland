(ns app.server.ingest.transcript
  "Reads Claude Code and Codex session files: walks *.jsonl under ~/.claude/projects and ~/.codex/sessions,
   parses and redacts each line into observations, and imports them into the object container.
   Takes: a transcript request {:request/type :transcript/harvest | :transcript/watch, :transcript/source, :transcript/paths}.
   Gives: observation maps; import results {:status :counts :source-lines}; a daemon thread per watch.
   Holds: depots *transcript-depot *transcript-claim-depot *transcript-obs-depot; PStates $$transcript-runs $$transcript-source-ledger $$transcript-observed-conversations $$transcript-tool-call-index $$transcript-run-seen-lines (in-process module)."
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.envelope :as envelope]
            [app.server.rama.object-container :as oc]
            [app.server.ingest.transcript-adapter :as transcript-adapter]
            [app.server.rama.object-container.transcript-identity :as transcript-identity]
            [app.server.rama.object-container.runtime :as oc-runtime]
            [app.server.episode.llm :as llm]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (java.io BufferedInputStream BufferedReader ByteArrayOutputStream
                    File FileInputStream)
           (java.nio.charset StandardCharsets)
           (java.nio.file Files LinkOption)
           (java.util UUID)))

;; ────────────────────────────────────────────────────────────────────────────────
;;   LIVE TRANSCRIPT HELPERS + PARKED DRAFT MODULE
;;
;;   Episode and ingest-watcher call sites use the source parsing/identity
;;   helpers in this namespace today. `transcript-module` and its IPC runtime
;;   remain a parked draft and are not one of the five deployed land modules;
;;   deployed file-offset truth lives in ObjectContainer transcript-ops.
;;
;;   The parked module is a passive observer of external chat logs: it
;;   watches transcript files produced by CLI tools such as Claude and Codex,
;;   harvests past content or tails live appends, parses and redacts each
;;   line, deduplicates source material against a line ledger, and indexes
;;   the resulting conversations and tool calls. It never participates in the
;;   conversation, and that non-participation is the boundary that keeps it
;;   orthogonal to Space.
;;
;;   Compressed:  external chat history becomes indexed, deduplicated source
;;                material.
;;
;;   For the kernel taxonomy and KERNEL-SHAPE spec see app.server.rama.kernel.
;; ────────────────────────────────────────────────────────────────────────────────

(def schema-version 1)
(def default-redaction-policy :standard)
(def default-source-version :unknown)
(def default-host-id nil)

(def transcript-request-types
  #{:transcript/harvest
    :transcript/watch})

(def transcript-sources
  #{:claude-code
    :codex
    :future/source})

(def redaction-policies
  #{:standard})

(def terminal-statuses
  #{:complete :failed :cancelled})

(defn now-ms [] (envelope/now-ms))
(defn random-id [prefix] (envelope/random-id prefix))

(defn transcript-routing-key
  [request-id]
  [:transcript/request request-id])

(defn blank-string?
  [x]
  (or (not (string? x)) (str/blank? x)))

(defn expand-home
  [path]
  (let [s (str path)]
    (if (str/starts-with? s "~/")
      (str (System/getProperty "user.home") (subs s 1))
      s)))

(defn default-source-paths
  [source]
  (case source
    :claude-code ["~/.claude/projects/**/*.jsonl"]
    :codex ["~/.codex/sessions/**/*.jsonl"]
    []))

(defn transcript-request
  [request-type & [opts]]
  (let [request-id (or (:transcript/request-id opts)
                       (:request-id opts)
                       (str (UUID/randomUUID)))
        source (or (:transcript/source opts) (:source opts) :claude-code)]
    {:request/type request-type
     :request/schema-version schema-version
     :request/time-ms (or (:time-ms opts) (now-ms))
     :transcript/request-id request-id
     :transcript/source source
     :transcript/paths (vec (or (:transcript/paths opts)
                                (:paths opts)
                                (default-source-paths source)))
     :transcript/redaction-policy (or (:transcript/redaction-policy opts)
                                      (:redaction-policy opts)
                                      default-redaction-policy)
     :transcript/triggered-by (or (:transcript/triggered-by opts)
                                  (:triggered-by opts)
                                  {:agent :sid})
     :routing/key (transcript-routing-key request-id)}))

(defn request-validation-errors
  [request]
  (cond-> []
    (not (map? request))
    (conj {:type :request/not-map})

    (and (map? request) (not (contains? transcript-request-types (:request/type request))))
    (conj {:type :request/type-invalid
           :value (:request/type request)})

    (and (map? request) (blank-string? (:transcript/request-id request)))
    (conj {:type :transcript/request-id-invalid
           :value (:transcript/request-id request)})

    (and (map? request) (not= (transcript-routing-key (:transcript/request-id request))
                              (:routing/key request)))
    (conj {:type :routing/key-invalid
           :value (:routing/key request)})

    (and (map? request) (not (contains? transcript-sources (:transcript/source request))))
    (conj {:type :transcript/source-invalid
           :value (:transcript/source request)})

    (and (map? request) (not (contains? redaction-policies
                                        (:transcript/redaction-policy request))))
    (conj {:type :transcript/redaction-policy-invalid
           :value (:transcript/redaction-policy request)})

    (and (map? request) (not (sequential? (:transcript/paths request))))
    (conj {:type :transcript/paths-invalid
           :value (:transcript/paths request)})))

(defn initial-run-row
  [request]
  {:transcript/request-id (:transcript/request-id request)
   :request/type (:request/type request)
   :status :pending
   :transcript/source (:transcript/source request)
   :transcript/paths (:transcript/paths request)
   :transcript/redaction-policy (:transcript/redaction-policy request)
   :transcript/triggered-by (:transcript/triggered-by request)
   :created-at-ms (:request/time-ms request)
   :updated-at-ms (:request/time-ms request)
   :observed-line-count 0
   :parse-error-count 0
   :progress []})

(defn rejected-run-row
  [request errors]
  {:transcript/request-id (:transcript/request-id request)
   :request/type (:request/type request)
   :status :failed
   :errors (vec errors)
   :created-at-ms (:request/time-ms request)
   :updated-at-ms (:request/time-ms request)})

(defn transcript-run-status-record
  [request-id status & [opts]]
  {:transcript/request-id request-id
   :claim/id (or (:claim-id opts) (random-id "transcript-claim"))
   :claim/type :transcript/run-status
   :status status
   :routing/key (transcript-routing-key request-id)
   :time-ms (or (:time-ms opts) (now-ms))
   :executor/id (:executor/id opts)
   :progress (:progress opts)
   :counts (:counts opts)
   :error (:error opts)})

(defn claim-request-id [claim] (:transcript/request-id claim))
(defn request-id [request] (:transcript/request-id request))
(defn observation-request-id [obs] (:transcript/ingest-request-id obs))

(defn append-bounded
  [xs x limit]
  (let [v (conj (vec xs) x)
        c (count v)]
    (if (> c limit)
      (subvec v (- c limit))
      v)))

(defn fold-run-status
  "Apply one authorized claim to a run row. Status moves through
   envelope/sticky-status so a terminal status can never regress even if a claim
   slips past the authorization fence. The line counters are topology-owned
   (incremented per novel line on the run task) — claim :counts may add other
   fields but can never clobber them."
  [run-row claim]
  (let [t (:time-ms claim)
        counts (dissoc (or (:counts claim) {})
                       :observed-line-count :parse-error-count)]
    (-> run-row
        (assoc :status (envelope/sticky-status terminal-statuses
                                           (:status run-row)
                                           (:status claim))
               :updated-at-ms t)
        (cond-> (seq counts) (merge counts)
                (:error claim) (assoc :error (:error claim))
                (:progress claim) (update :progress append-bounded (:progress claim) 50)))))

(def applied-claim-ids-limit 50)
(def claim-errors-limit 50)
(def request-conflicts-limit 10)

(defn transcript-request-fingerprint
  "Content fingerprint for duplicate-submit detection. :request/time-ms is
   re-minted by transcript-request on every build, so it is excluded — a client
   resubmitting the same intent must fingerprint identically."
  [request]
  (envelope/request-fingerprint (dissoc request :request/time-ms)))

(defn fold-request
  "Guarded request fold: first write wins (a duplicate submit can never reset a
   run that has since progressed — no :pending regression, no counter zeroing).
   Same id + same fingerprint replays as a no-op; same id + different
   fingerprint records a bounded conflict note on the row and leaves every
   other field untouched. No-ops return the IDENTICAL existing row so the
   topology can skip the write."
  [existing-run proposed-row request]
  (if (nil? existing-run)
    (assoc proposed-row :request/fingerprint (transcript-request-fingerprint request))
    (let [stored-fp (:request/fingerprint existing-run)
          incoming-fp (transcript-request-fingerprint request)]
      (if (or (nil? stored-fp) (= stored-fp incoming-fp))
        existing-run
        (let [conflict {:type :request/id-conflict
                        :existing/fingerprint stored-fp
                        :incoming/fingerprint incoming-fp}]
          (if (some #(= conflict %) (:request-conflicts existing-run))
            existing-run
            (update existing-run :request-conflicts
                    append-bounded conflict request-conflicts-limit)))))))

(defn fold-claim
  "Guarded claim fold behind the session-0 authorization chain:

     1. replay   → a :claim/id already folded (accepted OR rejected) is a
                   total no-op, so redelivered claims never duplicate progress
                   entries or audit errors.
     2. authorize → envelope/authorize-mutation: terminal statuses are sticky
                   (late/duplicate terminals and stale heartbeats are rejected,
                   never folded); only :pending/:running rows accept claims;
                   once an owner exists, every claim must present the matching
                   :executor/id — the losing executor in a claim race is
                   rejected here.
     3. accepted → the first :running claim carrying an :executor/id is granted
                   ownership; the status folds through fold-run-status.
     4. rejected → a bounded, token-free audit entry folds into :claim-errors;
                   status, counters and ownership stay untouched.

   Every no-op returns the IDENTICAL row so the topology skips the write."
  [run-row claim]
  (let [claim-id (:claim/id claim)]
    (if (and claim-id (some #(= claim-id %) (:applied-claim-ids run-row)))
      run-row
      (let [auth (envelope/authorize-mutation
                   run-row claim
                   {:status-key :status
                    :accepting-statuses #{:pending :running}
                    :terminal-statuses terminal-statuses
                    :claim-token-key (when (:claim/owner run-row) :claim/owner)
                    :record-token-key :executor/id
                    :context {:transcript/request-id (claim-request-id claim)}})
            remember-claim (fn [row]
                             (if claim-id
                               (update row :applied-claim-ids
                                       append-bounded claim-id applied-claim-ids-limit)
                               row))]
        (case (:auth/status auth)
          :accepted
          (-> run-row
              (cond-> (and (nil? (:claim/owner run-row)) (:executor/id claim))
                (assoc :claim/owner (:executor/id claim)))
              (fold-run-status claim)
              (remember-claim))

          :replay
          run-row

          :rejected
          (-> run-row
              (update :claim-errors append-bounded
                      (:auth/dead-letter auth) claim-errors-limit)
              (remember-claim)))))))

(defn line-hash
  [line]
  (str "sha256:" (envelope/sha-256 line)))

(defn line-hash-bytes
  "Byte-correct source-identity hash of a line's raw content bytes (terminator
   excluded). Hashes the exact on-disk bytes so the hash is stable regardless of
   how the line decodes — the basis for dedup key (source, file-id, byte-offset,
   line-hash)."
  [^bytes content-bytes]
  (str "sha256:" (envelope/sha-256-bytes content-bytes)))

(defn file-id
  [^File file]
  (try
    (let [attrs (Files/readAttributes (.toPath file)
                                      "unix:dev,ino"
                                      (make-array LinkOption 0))]
      {:device (str (.get attrs "dev"))
       :inode (str (.get attrs "ino"))})
    (catch Throwable _
      {:canonical-path (.getCanonicalPath file)})))

(defn source-file-key
  [source file-id]
  (str (name source) ":" (pr-str file-id)))

(defn source-line-key
  [obs]
  (str (source-file-key (:transcript/source obs) (:source/file-id obs))
       ":" (:source/byte-offset obs)
       ":" (:source/line-hash obs)))

(defn source-file-state-key
  [obs]
  (str "file:" (source-file-key (:transcript/source obs) (:source/file-id obs))))

(defn transcript-conversation-id
  [source parsed fallback]
  (or (:conversation_id parsed)
      (:conversation-id parsed)
      (:session_id parsed)
      (:sessionId parsed)
      (:cwdSessionId parsed)
      (:thread_id parsed)
      (:thread-id parsed)
      (:parentUuid parsed)
      (:uuid parsed)
      fallback))

(defn transcript-message-uuid
  [parsed]
  (or (:message_uuid parsed)
      (:message-uuid parsed)
      (:uuid parsed)
      (:id parsed)
      (get-in parsed [:message :id])
      (get-in parsed [:message :uuid])))

(defn transcript-event-type
  [parsed]
  (or (:type parsed)
      (:event parsed)
      (get-in parsed [:message :role])
      :unknown))

(defn transcript-source-timestamp
  [parsed]
  (or (:timestamp parsed)
      (:created_at parsed)
      (:created-at parsed)
      (get-in parsed [:message :created_at])))

(defn parsed-tool-use-blocks
  [payload]
  (let [content (or (get-in payload [:message :content])
                    (:content payload)
                    [])]
    (filter #(and (map? %) (#{"tool_use" "tool-use"} (:type %))) content)))

(defn tool-call-index-rows
  [obs]
  (for [block (parsed-tool-use-blocks (:transcript/redacted-payload obs))
        :let [tool-id (or (:id block) (:tool_use_id block) (:tool-use-id block))]
        :when (seq (str tool-id))]
    {:tool-call/id tool-id
     :tool-call/name (:name block)
     ;; the block comes out of :transcript/redacted-payload, so the input is
     ;; already post-redaction (spec op 8: name + redacted inputs)
     :tool-call/input (:input block)
     :transcript/source (:transcript/source obs)
     :transcript/conversation-id (:transcript/conversation-id obs)
     :transcript/message-uuid (:transcript/message-uuid obs)
     :source/line-key (source-line-key obs)
     :source/file-path (:source/file-path obs)
     :source/byte-offset (:source/byte-offset obs)}))

(defn parse-json-line
  [line]
  (json/read-str line :key-fn keyword))

(def text-redaction-patterns
  "Conservative raw-text redaction patterns for content that cannot be parsed
   (malformed/truncated lines). Each entry is [name regex] where the regex has
   exactly three capture groups: (prefix)(secret value)(suffix); missing groups
   default to \"\". The value group is replaced by [REDACTED]. The JSON-pair
   pattern deliberately also matches an UNTERMINATED value (writer killed
   mid-secret) — the exact shape of the proven parse-error redaction breach."
  [[:sensitive-json-pair
    #"(?i)(\"[^\"]{0,80}?(?:key|token|secret|password|credential|authorization)[^\"]{0,80}?\"\s*:\s*\")([^\"]*)(\"|$)"]
   [:sensitive-assignment
    #"(?i)\b((?:api[_-]?key|access[_-]?key|auth[_-]?token|token|secret|password|authorization)\s*[=:]\s*)([^\s\"',;}{]+)()"]
   [:bearer-token
    #"(?i)\b(Bearer\s+)([A-Za-z0-9._~+/=-]{8,})()"]
   [:provider-key
    #"\b(sk-|sk_live_|sk_test_|ghp_|gho_|ghs_|xox[baprs]-)([A-Za-z0-9_-]{8,})()"]
   [:aws-access-key
    #"\b(AKIA)([0-9A-Z]{16})\b()"]
   [:jwt
    #"\b()(eyJ[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,}\.[A-Za-z0-9_-]{4,})()"]])

(defn redact-text
  "Pattern-redact a raw string (best effort for unparseable content). Returns
   {:text <masked> :redactions [{:redaction/kind :text-pattern
                                 :redaction/pattern <name>
                                 :redaction/length <chars masked>} ...]}
   The metadata answers \"what was masked?\" without revealing what was masked."
  [s]
  (reduce
    (fn [{:keys [text redactions]} [pattern-name re]]
      (let [hits (volatile! [])
            masked (str/replace
                     text re
                     (fn [m]
                       (let [groups (if (string? m) [m m] m)
                             prefix (or (nth groups 1 nil) "")
                             value (or (nth groups 2 nil) "")
                             suffix (or (nth groups 3 nil) "")]
                         (vswap! hits conj
                                 {:redaction/kind :text-pattern
                                  :redaction/pattern pattern-name
                                  :redaction/length (count value)})
                         (str prefix "[REDACTED]" suffix))))]
        {:text masked
         :redactions (into redactions @hits)}))
    {:text (str s) :redactions []}
    text-redaction-patterns))

(defn redacted-preview-with-redactions
  "Redacted, truncated preview of a raw line plus the redaction metadata.
   Redaction runs BEFORE truncation: truncating first could cut a secret's
   closing quote and hide it from the pattern while leaving a recognizable
   prefix in the stored preview."
  [line]
  (let [{:keys [text redactions]} (redact-text line)]
    {:preview (subs text 0 (min 200 (count text)))
     :redactions redactions}))

(defn redacted-preview
  [line]
  (:preview (redacted-preview-with-redactions (str line))))

(defn redact-payload-with-redactions
  "Structural redaction with metadata: masks exactly what
   llm/redact-provider-payload masks (same llm/sensitive-key? predicate, same
   replacement value, so the stored payload is identical to the previous
   redactor's output) while collecting one metadata entry per masked key. The
   entry records key name, path and stringified-value length — never the value."
  ([payload] (redact-payload-with-redactions payload []))
  ([x path]
   (cond
     (map? x)
     (reduce-kv
       (fn [{:keys [payload redactions]} k v]
         (if (llm/sensitive-key? k)
           {:payload (assoc payload k "[REDACTED]")
            :redactions (conj redactions
                              {:redaction/kind :key-name
                               :redaction/key (name k)
                               :redaction/path (conj path k)
                               :redaction/length (count (str v))})}
           (let [child (redact-payload-with-redactions v (conj path k))]
             {:payload (assoc payload k (:payload child))
              :redactions (into redactions (:redactions child))})))
       {:payload (empty x) :redactions []}
       x)

     (sequential? x)
     (reduce
       (fn [{:keys [payload redactions]} [i v]]
         (let [child (redact-payload-with-redactions v (conj path i))]
           {:payload (conj payload (:payload child))
            :redactions (into redactions (:redactions child))}))
       {:payload [] :redactions []}
       (map-indexed vector x))

     :else
     {:payload x :redactions []})))

(defn transcript-observation
  [request source source-version host-id file file-id byte-offset byte-length line line-hash-val]
  (let [ingest-ts (str (java.time.Instant/ofEpochMilli (now-ms)))
        base {:transcript/source source
              :transcript/source-version (or source-version default-source-version)
              :source/file-id file-id
              :source/file-path (.getPath ^File file)
              :source/byte-offset byte-offset
              :source/line-hash line-hash-val
              :source/byte-length byte-length
              :transcript/ingest-timestamp ingest-ts
              :transcript/redactions []
              :transcript/host-id host-id
              :transcript/ingest-request-id (:transcript/request-id request)
              :routing/key (transcript-routing-key (:transcript/request-id request))}]
    (try
      (let [parsed (parse-json-line line)
            {redacted :payload redactions :redactions}
            (redact-payload-with-redactions parsed)
            conversation-id (transcript-conversation-id source redacted (.getPath ^File file))]
        (assoc base
               :transcript/conversation-id conversation-id
               :transcript/message-uuid (transcript-message-uuid redacted)
               :transcript/source-timestamp (transcript-source-timestamp redacted)
               :transcript/event-type (transcript-event-type redacted)
               :transcript/redacted-payload redacted
               :transcript/redactions (vec redactions)
               :transcript/parse-error-kind nil))
      (catch Throwable _
        (let [{:keys [preview redactions]} (redacted-preview-with-redactions line)]
          (assoc base
                 :transcript/conversation-id (.getPath ^File file)
                 :transcript/message-uuid nil
                 :transcript/source-timestamp nil
                 :transcript/event-type :parse-error
                 :transcript/redacted-payload nil
                 :transcript/redacted-preview preview
                 :transcript/redactions (vec redactions)
                 :transcript/parse-error-kind :invalid-json))))))

(defn walk-jsonl-files
  [paths]
  (let [expand (fn [path]
                 (let [path (expand-home path)]
                   (cond
                     (str/includes? path "**")
                     (let [base-path (subs path 0 (str/index-of path "**"))
                           base (io/file base-path)]
                       (when (.exists base)
                         (filter #(and (.isFile ^File %)
                                       (str/ends-with? (.getName ^File %) ".jsonl"))
                                 (file-seq base))))

                     (.isDirectory (io/file path))
                     (filter #(and (.isFile ^File %)
                                   (str/ends-with? (.getName ^File %) ".jsonl"))
                             (file-seq (io/file path)))

                     (.exists (io/file path))
                     [(io/file path)]

                     :else [])))]
    (->> paths
         (mapcat expand)
         (remove nil?)
         (distinct)
         (sort-by #(.getPath ^File %)))))

(defn read-jsonl-observations
  "Read JSONL lines from `file` starting at byte `start-offset`, producing one
   observation per line.

   Reads raw bytes and decodes UTF-8 explicitly. `RandomAccessFile.readLine`
   decodes with modified UTF-8 (zero-extends each byte into a char), corrupting
   multi-byte characters AND inflating byte-length — which cascades into every
   downstream byte offset and line hash. This reader computes source identity
   from the actual byte slice instead:

     byte-offset = offset of the line's first byte
     byte-length = content bytes + 1 for the trailing \\n (0 if EOF is reached
                   with no terminator — a partial trailing line)
     line-hash   = sha256 of the exact content bytes (terminator excluded)

   The line terminator is LF (\\n); a preceding CR, if present, stays in the
   content byte slice (JSON tolerates trailing whitespace) so the hash remains a
   faithful image of the on-disk bytes."
  [request file start-offset]
  (let [fid (file-id file)
        source (:transcript/source request)
        source-version (or (:transcript/source-version request) default-source-version)
        host-id (or (:transcript/host-id request) default-host-id)]
    (with-open [fis (FileInputStream. ^File file)]
      (.position (.getChannel fis) (long start-offset))
      (let [in (BufferedInputStream. fis)]
        (loop [offset (long start-offset)
               observations []]
          (let [baos (ByteArrayOutputStream.)
                terminator (loop []
                             (let [b (.read in)]
                               (cond
                                 (= b -1) :eof
                                 (= b 10) :newline
                                 :else (do (.write baos b) (recur)))))
                content-bytes (.toByteArray baos)
                n (alength content-bytes)]
            (if (and (= terminator :eof) (zero? n))
              ;; Clean EOF with nothing buffered (or file ended exactly on a
              ;; newline) — no more lines.
              observations
              (let [byte-length (if (= terminator :newline) (inc n) n)
                    line (String. content-bytes StandardCharsets/UTF_8)
                    lh (line-hash-bytes content-bytes)
                    obs (transcript-observation
                          request source source-version host-id file fid
                          offset byte-length line lh)]
                (if (= terminator :eof)
                  ;; Partial trailing line (no terminator) — last record.
                  (conj observations obs)
                  (recur (+ offset byte-length)
                         (conj observations obs)))))))))))

(defn reduce-jsonl-observations
  "Streaming, BOUNDED JSONL reader: reads raw bytes from `start-offset` up to
   `end-offset` (a length snapshot taken by the caller), decodes UTF-8 once,
   and calls (f acc obs) for each newline-TERMINATED line. Returns
   {:acc <final> :next-offset <offset just past the last consumed newline>}.

   A line whose terminator does not sit inside the bound is NOT emitted, is not
   a parse error, and does not advance :next-offset (spec I5). Because the
   bound IS the newline gate, there is no separate check-then-read step to race
   against a live writer — unlike the old snapshot-length-then-read-to-live-EOF
   shape this replaces. Byte identity (offset/length/hash) comes from the raw
   byte slice, same as read-jsonl-observations."
  [request file start-offset end-offset f init]
  (let [fid (file-id file)
        source (:transcript/source request)
        source-version (or (:transcript/source-version request) default-source-version)
        host-id (or (:transcript/host-id request) default-host-id)
        start (long start-offset)
        bound (long end-offset)]
    (if (<= bound start)
      {:acc init :next-offset start}
      (with-open [fis (FileInputStream. ^File file)]
        (.position (.getChannel fis) start)
        (let [in (BufferedInputStream. fis)]
          (loop [offset start
                 acc init]
            (let [baos (ByteArrayOutputStream.)
                  terminator (loop [budget (- bound offset)]
                               (if (zero? budget)
                                 :bound
                                 (let [b (.read in)]
                                   (cond
                                     (= b -1) :eof
                                     (= b 10) :newline
                                     :else (do (.write baos b)
                                               (recur (dec budget)))))))
                  content-bytes (.toByteArray baos)
                  n (alength content-bytes)]
              (if (not= terminator :newline)
                ;; Bound or EOF reached before a terminator: the (possibly
                ;; empty) tail is withheld and the cursor stays at line start.
                {:acc acc :next-offset offset}
                (let [byte-length (inc n)
                      line (String. content-bytes StandardCharsets/UTF_8)
                      obs (transcript-observation
                            request source source-version host-id file fid
                            offset byte-length line (line-hash-bytes content-bytes))]
                  (recur (+ offset byte-length) (f acc obs)))))))))))

(defn source-file-state-entry
  [obs]
  {:source/file-id (:source/file-id obs)
   :source/file-path (:source/file-path obs)
   :transcript/source (:transcript/source obs)
   :source/last-byte-offset (+ (long (:source/byte-offset obs))
                               (long (:source/byte-length obs)))
   :source/last-line-hash (:source/line-hash obs)
   :transcript/ingest-timestamp (:transcript/ingest-timestamp obs)
   :transcript/ingest-request-id (:transcript/ingest-request-id obs)})

(defn fold-source-file-state
  "File cursor rows only advance: keep whichever row has the higher
   :source/last-byte-offset (a row-level monotonic watermark, see
   envelope/monotonic-watermark). Replays and cross-task reorders of a file's
   observations can never rewind the durable cursor below a consumed newline."
  [existing entry]
  (if (or (nil? existing)
          (>= (long (or (:source/last-byte-offset entry) 0))
              (long (or (:source/last-byte-offset existing) 0))))
    entry
    existing))

(defn conversation-entry
  [obs]
  {:source/line-key (source-line-key obs)
   :source/file-path (:source/file-path obs)
   :source/byte-offset (:source/byte-offset obs)
   :source/byte-length (:source/byte-length obs)
   :transcript/source (:transcript/source obs)
   :transcript/event-type (:transcript/event-type obs)
   :transcript/message-uuid (:transcript/message-uuid obs)
   :transcript/source-timestamp (:transcript/source-timestamp obs)
   :transcript/parse-error-kind (:transcript/parse-error-kind obs)})

(defn increment-run-counts
  [run-row obs]
  (-> run-row
      (update :observed-line-count (fnil inc 0))
      (cond-> (:transcript/parse-error-kind obs)
        (update :parse-error-count (fnil inc 0)))
      (assoc :updated-at-ms (now-ms))))

(defn obs-conversation-id
  [obs]
  (:transcript/conversation-id obs))

(defn tool-call-id
  [row]
  (:tool-call/id row))

;; Partitioning note (recorded deviation, retro F4/TR-09): *transcript-obs-depot
;; hashes by :transcript/ingest-request-id, so a harvest burst funnels through
;; one task before fanning out per line key. The capture contract wants
;; source-file partitioning; realigning requires a depot migration and is out of
;; this fix session's scope. The conversation PState's inner map is likewise
;; still unbounded/non-subindexed (TR-08, deferred — see FIX_PLAN for the
;; target shape).
(defmodule transcript-module [setup topologies]
  (declare-depot setup *transcript-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-claim-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-obs-depot (hash-by :transcript/ingest-request-id))
  (let [n (stream-topology topologies "transcript-capture-topology")]
    (declare-pstate n $$transcript-runs {String Object})
    (declare-pstate n $$transcript-source-ledger {String Object})
    (declare-pstate n $$transcript-observed-conversations {String Object})
    (declare-pstate n $$transcript-tool-call-index {String Object})
    ;; per-run dedup marks for the topology-owned line counters: keyed by
    ;; request-id (colocated with the run row), one inner key per line identity
    ;; this run has counted. Subindexed: a run can process 10^5-10^6 lines.
    (declare-pstate n $$transcript-run-seen-lines
                    {String (map-schema String Boolean {:subindex? true})})

    (<<sources n
      ;; ── request ── first write wins; duplicates replay/conflict via
      ;; fold-request, so a re-submitted request can never reset a live or
      ;; terminal run (no :pending regression, no counter zeroing).
      ;; records arrive on hash(:transcript/request-id) — already this run's
      ;; task, no |hash needed (same for the claim source below)
      (source> *transcript-depot :> *request)
      (request-id *request :> *request-id)
      (filter> (not (blank-string? *request-id)))
      (request-validation-errors *request :> *errors)
      (<<if (empty? *errors)
        (initial-run-row *request :> *proposed-row)
        (else>)
        (rejected-run-row *request *errors :> *proposed-row))
      (local-select> [(keypath *request-id)] $$transcript-runs :> *existing-run)
      (fold-request *existing-run *proposed-row *request :> *folded-run)
      (filter> (not (identical? *folded-run *existing-run)))
      (local-transform> [(keypath *request-id) (termval *folded-run)] $$transcript-runs)

      ;; ── claim ── guarded fold: replay no-op, terminal fence, ownership
      ;; arbitration; rejections fold a bounded audit entry. Identical-row
      ;; no-ops skip the write.
      (source> *transcript-claim-depot :> *claim)
      (claim-request-id *claim :> *request-id)
      (filter> (not (blank-string? *request-id)))
      (local-select> [(keypath *request-id)] $$transcript-runs :> *run-row)
      (filter> (some? *run-row))
      (fold-claim *run-row *claim :> *updated-run-row)
      (filter> (not (identical? *updated-run-row *run-row)))
      (local-transform> [(keypath *request-id) (termval *updated-run-row)] $$transcript-runs)

      ;; ── observation ── no front gate: the event crosses four partitions and
      ;; a stream retry replays it from source> WITHOUT rolling back writes
      ;; committed at earlier partitioner boundaries, so dedup is enforced
      ;; per write site instead — ledger rows are write-if-absent, file cursors
      ;; fold monotonically, counters are per-run seen-marked (marker and
      ;; increment sit on one task between partitioners, so they commit
      ;; atomically), and conversation/tool rows are deterministic termvals per
      ;; line identity. Any replay re-executes every site harmlessly; no
      ;; derived view can be skipped forever by a half-committed first attempt.
      (source> *transcript-obs-depot {:retry-mode :all-after} :> *obs)
      (observation-request-id *obs :> *request-id)
      (source-line-key *obs :> *line-key)
      (source-file-state-key *obs :> *file-state-key)
      (obs-conversation-id *obs :> *conversation-id)
      (conversation-entry *obs :> *conversation-entry)
      (source-file-state-entry *obs :> *file-state-entry)
      (|hash *line-key)
      (local-select> [(keypath *line-key)] $$transcript-source-ledger :> *existing-line)
      (envelope/write-if-absent *existing-line *obs :> *ledger-row)
      ;; <<if, NOT filter>: a duplicate must skip THIS write yet still flow to
      ;; every later write site (the old front gate dropped it here, which is
      ;; what made a half-committed first attempt unrepairable).
      (<<if (not (identical? *ledger-row *existing-line))
        (local-transform> [(keypath *line-key) (termval *ledger-row)]
                          $$transcript-source-ledger))
      (|hash *file-state-key)
      (local-select> [(keypath *file-state-key)] $$transcript-source-ledger :> *existing-file-state)
      (fold-source-file-state *existing-file-state *file-state-entry :> *folded-file-state)
      (<<if (not (identical? *folded-file-state *existing-file-state))
        (local-transform> [(keypath *file-state-key) (termval *folded-file-state)]
                          $$transcript-source-ledger))
      (|hash *request-id)
      (local-select> [(keypath *request-id)] $$transcript-runs :> *run-row)
      (<<if (some? *run-row)
        (local-select> [(keypath *request-id *line-key)] $$transcript-run-seen-lines :> *seen)
        (<<if (nil? *seen)
          (increment-run-counts *run-row *obs :> *counted-run-row)
          (local-transform> [(keypath *request-id *line-key) (termval true)]
                            $$transcript-run-seen-lines)
          (local-transform> [(keypath *request-id) (termval *counted-run-row)] $$transcript-runs)))
      (|hash *conversation-id)
      (local-transform> [(keypath *conversation-id *line-key) (termval *conversation-entry)]
                        $$transcript-observed-conversations)
      (tool-call-index-rows *obs :> *tool-call-rows)
      (explode *tool-call-rows :> *tool-call-row)
      (tool-call-id *tool-call-row :> *tool-call-id)
      (|hash *tool-call-id)
      (local-transform> [(keypath *tool-call-id) (termval *tool-call-row)]
                        $$transcript-tool-call-index))))

(defn start-transcript-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name transcript-module)]
    (launch-module! ipc transcript-module {:tasks 4 :threads 2})
    {:ipc ipc
     :module-name module-name
     :transcript-depot (foreign-depot ipc module-name "*transcript-depot")
     :transcript-claim-depot (foreign-depot ipc module-name "*transcript-claim-depot")
     :transcript-obs-depot (foreign-depot ipc module-name "*transcript-obs-depot")
     :transcript-runs (foreign-pstate ipc module-name "$$transcript-runs")
     :transcript-source-ledger (foreign-pstate ipc module-name "$$transcript-source-ledger")
     :transcript-observed-conversations (foreign-pstate ipc module-name "$$transcript-observed-conversations")
     :transcript-tool-call-index (foreign-pstate ipc module-name "$$transcript-tool-call-index")}))

(defn close-transcript-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-transcript-request!
  ([runtime request]
   (append-transcript-request! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:transcript-depot runtime) request ack-level)
   request))

(defn append-transcript-status!
  ([runtime claim]
   (append-transcript-status! runtime claim :append-ack))
  ([runtime claim ack-level]
   (foreign-append! (:transcript-claim-depot runtime) claim ack-level)
   claim))

(defn append-transcript-observation!
  ([runtime obs]
   (append-transcript-observation! runtime obs :append-ack))
  ([runtime obs ack-level]
   (foreign-append! (:transcript-obs-depot runtime) obs ack-level)
   obs))

(defn select-pstate-one
  [pstate path]
  (foreign-select-one path pstate))

(defn read-run
  [runtime request-id]
  (select-pstate-one (:transcript-runs runtime) [(keypath request-id)]))

(defn read-ledger-line
  [runtime line-key]
  (select-pstate-one (:transcript-source-ledger runtime) [(keypath line-key)]))

(defn read-source-file-state
  [runtime source fid]
  (select-pstate-one (:transcript-source-ledger runtime)
                     [(keypath (str "file:" (source-file-key source fid)))]))

(defn read-conversation
  [runtime conversation-id]
  (or (select-pstate-one (:transcript-observed-conversations runtime)
                         [(keypath conversation-id)])
      {}))

(defn read-tool-call
  [runtime tool-call-id]
  (select-pstate-one (:transcript-tool-call-index runtime) [(keypath tool-call-id)]))

(defn await-materialized
  ([read-f pred]
   (await-materialized read-f pred 2000))
  ([read-f pred timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop [value (read-f)]
       (cond
         (pred value) value
         (>= (System/currentTimeMillis) deadline) value
         :else (do
	                 (Thread/sleep 25)
	                 (recur (read-f))))))))

(defn object-container-runtime?
  [runtime]
  (contains? runtime :object-container-requests-depot))

(defn transcript-import-message-container-id
  [request]
  (some #(when (= :chat-message (:container-kind %)) (:container-id %))
        (get-in request [:payload :object-containers])))

(defn transcript-observation-file-key
  [obs]
  (source-file-key (:transcript/source obs) (:source/file-id obs)))

(defn transcript-conversation-container-id
  [obs]
  (transcript-identity/chat-conversation-id
   (transcript-identity/transcript-object-key (:transcript/source obs)
                                              (:transcript/conversation-id obs))))

(defn previous-common-message-container-id
  [runtime last-message-by-conversation obs]
  (or (get last-message-by-conversation (:transcript/conversation-id obs))
      (some->> (transcript-conversation-container-id obs)
               (oc-runtime/read-transcript-last-message runtime)
               :message-container-id)))

(defn common-transcript-source-line-order-key
  [obs]
  (format "%020d:%s"
          (long (or (:source/byte-offset obs) 0))
          (envelope/sha-256 (transcript-identity/transcript-source-line-key obs))))

(defn common-transcript-source-line
  [obs import-request]
  (assoc obs
         :source/file-key (transcript-observation-file-key obs)
         :source/line-key (transcript-identity/transcript-source-line-key obs)
         :source-line/order-key (common-transcript-source-line-order-key obs)
         :import/key (:import/key import-request)
         :material/fingerprint (:material/fingerprint import-request)))

(defn common-source-line-completion-matches?
  [source-line completion-row]
  (and (some? completion-row)
       (contains? transcript-identity/transcript-source-line-complete-statuses
                  (:status completion-row))
       (= (:source/file-key source-line) (:file-key completion-row))
       (= (:source-line/order-key source-line) (:order-key completion-row))
       (= (:source/line-key source-line) (:source-line-key completion-row))
       (= (:import/key source-line) (:import-key completion-row))
       (= (:material/fingerprint source-line) (:material-fingerprint completion-row))
       (= (long (or (:source/byte-offset source-line) 0))
          (long (or (:byte-offset completion-row) 0)))
       (= (long (or (:source/byte-length source-line) 0))
          (long (or (:byte-length completion-row) 0)))
       (= (:source/line-hash source-line) (:line-hash completion-row))))

(defn await-common-source-line-completion
  [runtime source-line timeout-ms]
  (await-materialized
   #(oc-runtime/read-transcript-source-line
     runtime
     (:source/file-key source-line)
     (:source-line/order-key source-line))
   #(common-source-line-completion-matches? source-line %)
   timeout-ms))

(defn transcript-import-error
  [import-request decision]
  {:type :object-container/import-failed
   :import-request-id (:request/id import-request)
   :import-key (:import/key import-request)
   :status (:status decision)
   :reason (:reason decision)
   :errors (vec (:errors decision))})

(defn transcript-source-line-completion-error
  [source-line completion-row]
  {:type :object-container/source-line-completion-missing
   :file-key (:source/file-key source-line)
   :order-key (:source-line/order-key source-line)
   :source-line-key (:source/line-key source-line)
   :import-key (:import/key source-line)
   :material-fingerprint (:material/fingerprint source-line)
   :observed-completion (select-keys completion-row
                                     [:file-key
                                      :order-key
                                      :status
                                      :import-key
                                      :material-fingerprint
                                      :source-line-key])})

(defn transcript-counts
  [observed-line-count parse-error-count containers-created-count]
  {:observed-line-count observed-line-count
   :parse-error-count parse-error-count
   :containers-created-count containers-created-count})

(defn append-object-container-file-state!
  [runtime request file source-lines next-offset]
  (when (seq source-lines)
    (let [first-line (first source-lines)
          last-line (last source-lines)
          file-key (:source/file-key first-line)]
      (oc-runtime/append-transcript-file-state!
       runtime
       {:request/type :transcript/file-state
        :source/file-key file-key
        :source/file-id (:source/file-id first-line)
        :source/file-path (.getPath ^File file)
        :source/current-byte-length (.length ^File file)
        :source/last-byte-offset next-offset
        :source/observed-byte-offset next-offset
        :source/line-count (count source-lines)
        :source/lines (vec source-lines)
        :transcript/source (:transcript/source request)
        :transcript/conversation-id (:transcript/conversation-id last-line)
        :transcript/ingest-request-id (:transcript/request-id request)
        :time-ms (now-ms)}))))

(defn expected-common-file-offset?
  [next-offset file-offset-row]
  (and (some? file-offset-row)
       (= (long next-offset) (long (or (:last-byte-offset file-offset-row) -1)))
       (= :safe (:resume-status file-offset-row))
       (false? (:repair-needed file-offset-row))))

(defn transcript-file-offset-advance-error
  [file-key next-offset file-offset-row]
  {:type :object-container/file-offset-not-advanced
   :file-key file-key
   :expected-last-byte-offset next-offset
   :observed-file-offset-row (select-keys file-offset-row
                                          [:file-key
                                           :last-byte-offset
                                           :observed-byte-offset
                                           :resume-status
                                           :repair-needed
                                           :error])})

(defn append-and-await-object-container-file-state!
  [runtime request file source-lines next-offset]
  (if (empty? source-lines)
    {:status :accepted}
    (let [file-key (:source/file-key (first source-lines))]
      (loop [attempt 1
             last-file-offset-row nil]
        (append-object-container-file-state! runtime request file source-lines next-offset)
        (let [file-offset-row (await-materialized
                               #(oc-runtime/read-transcript-file-offset runtime file-key)
                               #(expected-common-file-offset? next-offset %)
                               2000)]
          (cond
            (expected-common-file-offset? next-offset file-offset-row)
            {:status :accepted
             :file-offset-row file-offset-row}

            (< attempt 3)
            (do
              (Thread/sleep 50)
              (recur (inc attempt) (or file-offset-row last-file-offset-row)))

            :else
            (let [error (transcript-file-offset-advance-error
                         file-key
                         next-offset
                         (or file-offset-row last-file-offset-row))]
              (oc-runtime/append-transcript-control!
               runtime
               (transcript-run-status-record (:transcript/request-id request)
                                             :failed
                                             {:error error}))
              {:status :failed
               :error error
               :file-offset-row (or file-offset-row last-file-offset-row)})))))))

(defn import-observations-into-object-container!
  [runtime request observations last-message-by-conversation]
  (loop [remaining (vec observations)
         last-message-by-conversation last-message-by-conversation
	         observed-line-count 0
	         parse-error-count 0
	         containers-created-count 0
	         source-lines []]
    (if (empty? remaining)
      {:status :accepted
       :last-message-by-conversation last-message-by-conversation
       :source-lines source-lines
       :counts (transcript-counts observed-line-count
                                  parse-error-count
                                  containers-created-count)}
      (let [obs (first remaining)
            previous-message-id (previous-common-message-container-id
                                 runtime
                                 last-message-by-conversation
                                 obs)
            obs' (cond-> obs
                   previous-message-id
                   (assoc :transcript/previous-message-container-id previous-message-id))
            import-request (transcript-adapter/transcript-observation-import-request obs')
            source-line (common-transcript-source-line obs' import-request)
            container-count (count (get-in import-request [:payload :object-containers]))
            message-container-id (transcript-import-message-container-id import-request)
            observed-line-count' (inc observed-line-count)
            parse-error-count' (cond-> parse-error-count
                                 (:transcript/parse-error-kind obs) inc)
            containers-created-count' (+ containers-created-count container-count)
            _ (oc-runtime/append-object-container-request! runtime import-request)
            decision (oc-runtime/await-object-container-decision runtime import-request 5000)]
        (if (= :accepted (:status decision))
          (let [completion-row (await-common-source-line-completion runtime source-line 5000)]
            (if (common-source-line-completion-matches? source-line completion-row)
              (recur (rest remaining)
                     (cond-> last-message-by-conversation
                       (and message-container-id
                            (nil? (:transcript/parse-error-kind obs)))
                       (assoc (:transcript/conversation-id obs) message-container-id))
                     observed-line-count'
                     parse-error-count'
                     containers-created-count'
                     (conj source-lines source-line))
              (let [counts (transcript-counts observed-line-count'
                                              parse-error-count'
                                              containers-created-count')
                    error (transcript-source-line-completion-error source-line completion-row)]
                (oc-runtime/append-transcript-control!
                 runtime
                 (transcript-run-status-record (:transcript/request-id request)
                                               :failed
                                               {:counts counts
                                                :error error}))
                {:status :failed
                 :last-message-by-conversation last-message-by-conversation
                 :source-lines source-lines
                 :counts counts
                 :error error
                 :decision decision})))
          (let [counts (transcript-counts observed-line-count'
                                          parse-error-count'
                                          containers-created-count')
                error (transcript-import-error import-request decision)]
            (oc-runtime/append-transcript-control!
             runtime
             (transcript-run-status-record (:transcript/request-id request)
                                           :failed
                                           {:counts counts
                                            :error error}))
            {:status :failed
             :last-message-by-conversation last-message-by-conversation
             :source-lines source-lines
             :counts counts
             :error error
             :decision decision}))))))

(defn harvest-transcripts-into-object-container!
  [runtime request]
  (oc-runtime/append-transcript-control! runtime request)
  (oc-runtime/append-transcript-control!
   runtime
   (transcript-run-status-record (:transcript/request-id request) :running))
  (let [files (walk-jsonl-files (:transcript/paths request))]
    (loop [remaining-files files
           last-message-by-conversation {}
           observed-line-count 0
           parse-error-count 0
           containers-created-count 0]
      (if (empty? remaining-files)
        (let [counts (transcript-counts observed-line-count
                                        parse-error-count
                                        containers-created-count)]
          (oc-runtime/append-transcript-control!
           runtime
           (transcript-run-status-record (:transcript/request-id request)
                                         :complete
                                         {:counts counts}))
          {:transcript/request-id (:transcript/request-id request)
           :status :complete
           :files (count files)
           :observations-appended observed-line-count
           :counts counts})
        (let [file (first remaining-files)
              observations (vec (read-jsonl-observations request file 0))
              next-offset (.length ^File file)
              import-result (import-observations-into-object-container!
                             runtime
                             request
                             observations
                             last-message-by-conversation)
	      counts (:counts import-result)]
	  (if (= :accepted (:status import-result))
	    (let [file-state-result (append-and-await-object-container-file-state!
	                             runtime
	                             request
	                             file
	                             (:source-lines import-result)
	                             next-offset)]
	      (if (= :accepted (:status file-state-result))
	        (recur (rest remaining-files)
	               (:last-message-by-conversation import-result)
	               (+ observed-line-count (:observed-line-count counts))
	               (+ parse-error-count (:parse-error-count counts))
	               (+ containers-created-count (:containers-created-count counts)))
	        (assoc file-state-result
	               :transcript/request-id (:transcript/request-id request)
	               :files (count files)
	               :counts counts)))
	    (assoc import-result
	           :transcript/request-id (:transcript/request-id request)
	           :files (count files))))))))

(defn await-claim-grant
  "Append a :running claim for executor-id and await the arbitration outcome.
   Returns the run row once an owner is visible (or the last row read on
   timeout). The caller must verify ownership before doing any work — the
   losing executor of a claim race reads another owner here and skips."
  [runtime request-id executor-id]
  (append-transcript-status!
   runtime
   (transcript-run-status-record request-id :running {:executor/id executor-id}))
  (await-materialized #(read-run runtime request-id)
                      #(some? (:claim/owner %))
                      5000))

(defn harvest-transcripts!
  [runtime request]
  (if (object-container-runtime? runtime)
    (harvest-transcripts-into-object-container! runtime request)
    (let [request-id (:transcript/request-id request)
          executor-id (random-id "transcript-executor")]
      (append-transcript-request! runtime request)
      (let [run (await-materialized #(read-run runtime request-id) some?)]
        (cond
          ;; rejected request (validation errors) — nothing to execute
          (= :failed (:status run))
          {:transcript/request-id request-id
           :status :rejected
           :errors (:errors run)}

          :else
          (let [claimed (await-claim-grant runtime request-id executor-id)]
            (if (not= executor-id (:claim/owner claimed))
              ;; lost the claim race (or the run is already owned/terminal):
              ;; at-most-once execution — do not touch any file.
              {:transcript/request-id request-id
               :status :claim-lost
               :claim/owner (:claim/owner claimed)}
              (let [files (walk-jsonl-files (:transcript/paths request))
                    ;; stream: append each newline-terminated observation as it
                    ;; is read — no corpus-sized accumulation, and the trailing
                    ;; partial line of a live file is withheld (I5). A bad file
                    ;; is recorded and skipped; the run continues (I8).
                    totals
                    (reduce
                      (fn [acc ^File file]
                        (try
                          (:acc (reduce-jsonl-observations
                                  request file 0 (.length file)
                                  (fn [acc obs]
                                    (append-transcript-observation! runtime obs)
                                    (-> acc
                                        (update :observed-line-count inc)
                                        (cond-> (:transcript/parse-error-kind obs)
                                          (update :parse-error-count inc))))
                                  acc))
                          (catch Throwable t
                            (let [file-error {:file (.getPath file)
                                              :message (.getMessage t)}]
                              (append-transcript-status!
                               runtime
                               (transcript-run-status-record
                                 request-id :running
                                 {:executor/id executor-id
                                  :progress {:file-error file-error}}))
                              (update acc :file-errors conj file-error)))))
                      {:observed-line-count 0 :parse-error-count 0 :file-errors []}
                      files)]
                ;; materialization barrier: the run's own counters must reach
                ;; what this run appended BEFORE :complete lands, so a reader
                ;; can never observe :complete with counters missing the tally.
                (await-materialized #(read-run runtime request-id)
                                    #(>= (long (or (:observed-line-count %) 0))
                                         (long (:observed-line-count totals)))
                                    10000)
                (append-transcript-status!
                 runtime
                 (transcript-run-status-record
                   request-id :complete
                   {:executor/id executor-id
                    :error (when (seq (:file-errors totals))
                             {:type :transcript/file-errors
                              :file-errors (:file-errors totals)})}))
                (await-materialized #(read-run runtime request-id)
                                    #(= :complete (:status %)))
                {:transcript/request-id request-id
                 :status :complete
                 :files (count files)
                 :observations-appended (:observed-line-count totals)
                 :counts (select-keys totals [:observed-line-count :parse-error-count])
                 :file-errors (:file-errors totals)}))))))))

(defn read-complete-appended-lines
  "Newline-gated read of a file's appended lines from `start-offset`. The
   length snapshot taken here is the read BOUND, so a writer appending between
   the snapshot and the read can never leak a partial line into the result —
   the old shape (read to live EOF, then inspect the byte at the snapshot's
   end) raced exactly that interleaving. Returns {:observations :next-offset}
   where :next-offset never passes an unterminated tail (I5)."
  [request file start-offset]
  (let [len (.length ^File file)]
    (if (<= len (long start-offset))
      {:observations []
       :next-offset start-offset}
      (let [{:keys [acc next-offset]}
            (reduce-jsonl-observations request file start-offset len conj [])]
        {:observations acc
         :next-offset next-offset}))))

(defn start-transcript-watch-into-object-container!
  [runtime request & [opts]]
  (oc-runtime/append-transcript-control! runtime request)
  (oc-runtime/append-transcript-control!
   runtime
   (transcript-run-status-record (:transcript/request-id request) :running))
  (let [stop? (atom false)
        known-at-start (set (walk-jsonl-files (:transcript/paths request)))
        poll-ms (long (or (:poll-ms opts) 100))
        initialize-offset
        (fn [file created-after-start?]
          (let [fid (file-id file)
                file-key (source-file-key (:transcript/source request) fid)
                state (oc-runtime/read-transcript-file-offset runtime file-key)]
            (cond
              (:last-byte-offset state)
              (:last-byte-offset state)

              created-after-start?
	                  0

	                  (:backfill? opts)
	                  0
	                  :else
	                  (.length ^File file))))
        offsets (atom (into {}
                            (map (fn [file]
                                   [(source-file-key (:transcript/source request)
                                                     (file-id file))
                                    (initialize-offset file false)]))
                            known-at-start))
        last-message-by-conversation (atom {})
        poll-once!
        (fn []
          (let [files (set (walk-jsonl-files (:transcript/paths request)))]
            (doseq [file files]
              (let [fid (file-id file)
                    file-key (source-file-key (:transcript/source request) fid)
                    new-file? (not (contains? known-at-start file))
                    start-offset (if (contains? @offsets file-key)
                                   (get @offsets file-key)
                                   (initialize-offset file new-file?))
                    {:keys [observations next-offset]}
                    (read-complete-appended-lines request file start-offset)
                    observations (vec observations)]
                (when (seq observations)
                  (let [import-result (import-observations-into-object-container!
                                       runtime
                                       request
                                       observations
                                       @last-message-by-conversation)]
	                    (if (= :accepted (:status import-result))
	                      (let [file-state-result
	                            (append-and-await-object-container-file-state!
	                             runtime
	                             request
	                             file
	                             (:source-lines import-result)
	                             next-offset)]
	                        (if (= :accepted (:status file-state-result))
	                          (do
	                            (reset! last-message-by-conversation
	                                    (:last-message-by-conversation import-result))
	                            (swap! offsets assoc file-key next-offset))
	                          (do
	                            (reset! stop? true)
	                            (throw (ex-info
	                                    "Object-container transcript file offset failed"
	                                    {:result file-state-result})))))
	                      (do
	                        (reset! stop? true)
	                        (throw (ex-info "Object-container transcript import failed"
                                        {:result import-result}))))))))))]
    (let [thread (doto (Thread.
                         ^Runnable
                         (reify Runnable
                           (run [_]
                             (while (not @stop?)
                               (try
                                 (poll-once!)
                                 (catch Throwable t
                                   (oc-runtime/append-transcript-control!
                                    runtime
                                    (transcript-run-status-record
                                     (:transcript/request-id request)
                                     :failed
                                     {:error {:message (.getMessage t)
                                               :data (ex-data t)}}))))
                               (Thread/sleep poll-ms))))
                         (str "transcript-watch-common-" (:transcript/request-id request)))
                   (.setDaemon true)
                   (.start))]
      {:transcript/request-id (:transcript/request-id request)
       :thread thread
       :poll-once! poll-once!
       :stop! (fn []
                (reset! stop? true)
                (.join thread 1000)
                (oc-runtime/append-transcript-control!
                 runtime
                 (transcript-run-status-record (:transcript/request-id request)
                                               :cancelled)))})))

(defn start-transcript-watch!
  [runtime request & [opts]]
  (if (object-container-runtime? runtime)
    (start-transcript-watch-into-object-container! runtime request opts)
    (let [request-id (:transcript/request-id request)
          executor-id (random-id "transcript-executor")
          source (:transcript/source request)]
      (append-transcript-request! runtime request)
      (await-materialized #(read-run runtime request-id) some?)
      (let [claimed (await-claim-grant runtime request-id executor-id)]
        (if (not= executor-id (:claim/owner claimed))
          {:transcript/request-id request-id
           :status :claim-lost
           :claim/owner (:claim/owner claimed)}
          (let [stop? (atom false)
                ;; identity is the inode-based file-key, not the path: a file
                ;; deleted and re-created at the same path is a NEW file (read
                ;; from byte 0), and cursors survive renames.
                file-key-of (fn [file] (source-file-key source (file-id file)))
                known-at-start (into #{}
                                     (map file-key-of)
                                     (walk-jsonl-files (:transcript/paths request)))
                poll-ms (long (or (:poll-ms opts) 100))
                initialize-offset
                (fn [file created-after-start?]
                  (let [state (read-source-file-state runtime source (file-id file))]
                    (cond
                      (:source/last-byte-offset state)
                      (:source/last-byte-offset state)
                      created-after-start?
                      0
                      (:backfill? opts)
                      0
                      :else
                      (.length ^File file))))
                offsets (atom (into {}
                                    (map (fn [file]
                                           [(file-key-of file)
                                            (initialize-offset file false)]))
                                    (walk-jsonl-files (:transcript/paths request))))
                poll-once!
                (fn []
                  (doseq [file (walk-jsonl-files (:transcript/paths request))]
                    ;; per-file isolation (I8): one unreadable file is recorded
                    ;; as a progress note and skipped; its cursor does not move
                    ;; and the rest of the poll continues.
                    (try
                      (let [fk (file-key-of file)
                            new-file? (not (contains? known-at-start fk))
                            start-offset (if (contains? @offsets fk)
                                           (get @offsets fk)
                                           (initialize-offset file new-file?))
                            {:keys [observations next-offset]}
                            (read-complete-appended-lines request file start-offset)]
                        (doseq [obs observations]
                          (append-transcript-observation! runtime obs))
                        ;; advance only after every append returned (durable at
                        ;; :append-ack) — a failed append leaves the cursor at
                        ;; the line start so the next poll re-reads it, and the
                        ;; ledger folds away any duplicate.
                        (swap! offsets assoc fk next-offset))
                      (catch Throwable t
                        (append-transcript-status!
                         runtime
                         (transcript-run-status-record
                           request-id :running
                           {:executor/id executor-id
                            :progress {:file-error {:file (.getPath ^File file)
                                                    :message (.getMessage t)}}}))))))]
            (let [thread (doto (Thread.
                                 ^Runnable
                                 (reify Runnable
                                   (run [_]
                                     (while (not @stop?)
                                       (try
                                         (poll-once!)
                                         (catch Throwable t
                                           ;; loop-level failure is fatal: land the
                                           ;; terminal :failed AND stop polling —
                                           ;; terminal states are final (I7), so a
                                           ;; watch must never keep emitting after
                                           ;; reporting one.
                                           (reset! stop? true)
                                           (append-transcript-status!
                                            runtime
                                            (transcript-run-status-record
                                              request-id :failed
                                              {:executor/id executor-id
                                               :error {:message (.getMessage t)}}))))
                                       (when-not @stop?
                                         (Thread/sleep poll-ms)))))
                                 (str "transcript-watch-" request-id))
                           (.setDaemon true)
                           (.start))]
              {:transcript/request-id request-id
               :thread thread
               :poll-once! poll-once!
               :stop! (fn []
                        (reset! stop? true)
                        (.join thread 1000)
                        ;; rejected by the terminal fence if the run already
                        ;; reached :failed — stop! never flips a terminal.
                        (append-transcript-status!
                         runtime
                         (transcript-run-status-record
                           request-id :cancelled
                           {:executor/id executor-id})))})))))))
