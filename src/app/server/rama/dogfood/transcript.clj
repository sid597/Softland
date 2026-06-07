(ns app.server.rama.dogfood.transcript
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as core]
            [app.server.rama.dogfood.llm :as llm]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (java.io BufferedInputStream BufferedReader ByteArrayOutputStream
                    File FileInputStream RandomAccessFile)
           (java.nio.charset StandardCharsets)
           (java.nio.file Files LinkOption)
           (java.util UUID)))

;; ────────────────────────────────────────────────────────────────────────────────
;;   TRANSCRIPT KERNEL
;;
;;   The Transcript Kernel is a passive observer of external chat logs: it
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

(defn now-ms [] (core/now-ms))
(defn random-id [prefix] (core/random-id prefix))

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
  [run-row claim]
  (let [t (:time-ms claim)]
    (-> run-row
        (assoc :status (:status claim)
               :updated-at-ms t)
        (cond-> (:counts claim) (merge (:counts claim))
                (:error claim) (assoc :error (:error claim))
                (:progress claim) (update :progress append-bounded (:progress claim) 50)))))

(defn line-hash
  [line]
  (str "sha256:" (core/sha-256 line)))

(defn line-hash-bytes
  "Byte-correct source-identity hash of a line's raw content bytes (terminator
   excluded). Hashes the exact on-disk bytes so the hash is stable regardless of
   how the line decodes — the basis for dedup key (source, file-id, byte-offset,
   line-hash)."
  [^bytes content-bytes]
  (str "sha256:" (core/sha-256-bytes content-bytes)))

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
     :transcript/source (:transcript/source obs)
     :transcript/conversation-id (:transcript/conversation-id obs)
     :transcript/message-uuid (:transcript/message-uuid obs)
     :source/line-key (source-line-key obs)
     :source/file-path (:source/file-path obs)
     :source/byte-offset (:source/byte-offset obs)}))

(defn parse-json-line
  [line]
  (json/read-str line :key-fn keyword))

(defn redacted-preview
  [line]
  (let [s (str line)]
    (subs s 0 (min 200 (count s)))))

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
            redacted (llm/redact-provider-payload parsed)
            conversation-id (transcript-conversation-id source redacted (.getPath ^File file))]
        (assoc base
               :transcript/conversation-id conversation-id
               :transcript/message-uuid (transcript-message-uuid redacted)
               :transcript/source-timestamp (transcript-source-timestamp redacted)
               :transcript/event-type (transcript-event-type redacted)
               :transcript/redacted-payload redacted
               :transcript/parse-error-kind nil))
      (catch Throwable _
        (assoc base
               :transcript/conversation-id (.getPath ^File file)
               :transcript/message-uuid nil
               :transcript/source-timestamp nil
               :transcript/event-type :parse-error
               :transcript/redacted-payload nil
               :transcript/redacted-preview (redacted-preview line)
               :transcript/parse-error-kind :invalid-json)))))

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

(defn obs-tool-call-first
  [obs]
  (first (tool-call-index-rows obs)))

(defn obs-conversation-id
  [obs]
  (:transcript/conversation-id obs))

(defn tool-call-id
  [row]
  (:tool-call/id row))

(defmodule transcript-module [setup topologies]
  (declare-depot setup *transcript-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-claim-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-obs-depot (hash-by :transcript/ingest-request-id))
  (let [n (stream-topology topologies "transcript-capture-topology")]
    (declare-pstate n $$transcript-runs {String Object})
    (declare-pstate n $$transcript-source-ledger {String Object})
    (declare-pstate n $$transcript-observed-conversations {String Object})
    (declare-pstate n $$transcript-tool-call-index {String Object})

    (<<sources n
      (source> *transcript-depot :> *request)
      (request-id *request :> *request-id)
      (|hash *request-id)
      (request-validation-errors *request :> *errors)
      (<<if (empty? *errors)
        (initial-run-row *request :> *run-row)
        (local-transform> [(keypath *request-id) (termval *run-row)] $$transcript-runs))
      (<<if (not (empty? *errors))
        (rejected-run-row *request *errors :> *run-row)
        (local-transform> [(keypath *request-id) (termval *run-row)] $$transcript-runs))

      (source> *transcript-claim-depot :> *claim)
      (claim-request-id *claim :> *request-id)
      (|hash *request-id)
      (local-select> [(keypath *request-id)] $$transcript-runs :> *run-row)
      (<<if (some? *run-row)
        (fold-run-status *run-row *claim :> *updated-run-row)
        (local-transform> [(keypath *request-id) (termval *updated-run-row)] $$transcript-runs))

      (source> *transcript-obs-depot {:retry-mode :all-after} :> *obs)
      (observation-request-id *obs :> *request-id)
      (source-line-key *obs :> *line-key)
      (source-file-state-key *obs :> *file-state-key)
      (|hash *line-key)
      (local-select> [(keypath *line-key)] $$transcript-source-ledger :> *existing-line)
      (<<if (nil? *existing-line)
        (conversation-entry *obs :> *conversation-entry)
        (source-file-state-entry *obs :> *file-state-entry)
        (local-transform> [(keypath *line-key) (termval *obs)] $$transcript-source-ledger)
        (|hash *file-state-key)
        (local-transform> [(keypath *file-state-key) (termval *file-state-entry)] $$transcript-source-ledger)
        (|hash *request-id)
        (local-select> [(keypath *request-id)] $$transcript-runs :> *run-row)
        (<<if (some? *run-row)
          (increment-run-counts *run-row *obs :> *updated-run-row)
          (local-transform> [(keypath *request-id) (termval *updated-run-row)] $$transcript-runs))
        (obs-conversation-id *obs :> *conversation-id)
        (|hash *conversation-id)
        (local-transform> [(keypath *conversation-id) (keypath *line-key) (termval *conversation-entry)] $$transcript-observed-conversations)
        (obs-tool-call-first *obs :> *tool-call-row)
        (<<if (some? *tool-call-row)
          (tool-call-id *tool-call-row :> *tool-call-id)
          (|hash *tool-call-id)
          (local-transform> [(keypath *tool-call-id) (termval *tool-call-row)] $$transcript-tool-call-index))))))

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
  (first (foreign-select path pstate)))

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

(defn harvest-transcripts!
  [runtime request]
  (append-transcript-request! runtime request)
  (await-materialized #(read-run runtime (:transcript/request-id request)) some?)
  (append-transcript-status!
    runtime
    (transcript-run-status-record (:transcript/request-id request) :running))
  (let [files (walk-jsonl-files (:transcript/paths request))
        observations (mapcat #(read-jsonl-observations request % 0) files)
        counts (reduce (fn [acc obs]
                         (-> acc
                             (update :observed-line-count (fnil inc 0))
                             (cond-> (:transcript/parse-error-kind obs)
                               (update :parse-error-count (fnil inc 0)))))
                       {}
                       observations)]
    (doseq [obs observations]
      (append-transcript-observation! runtime obs))
    (append-transcript-status!
      runtime
      (transcript-run-status-record (:transcript/request-id request)
                                    :complete))
    (await-materialized #(read-run runtime (:transcript/request-id request))
                        #(= :complete (:status %)))
    {:transcript/request-id (:transcript/request-id request)
     :files (count files)
     :observations-appended (count observations)
     :counts counts}))

(defn read-complete-appended-lines
  [request file start-offset]
  (let [len (.length ^File file)]
    (if (<= len start-offset)
      {:observations []
       :next-offset start-offset}
      (let [observations (read-jsonl-observations request file start-offset)
            ends-with-newline?
            (with-open [raf (RandomAccessFile. file "r")]
              (when (pos? len)
                (.seek raf (dec len))
                (= (int \newline) (.read raf))))]
        (if ends-with-newline?
          {:observations observations
           :next-offset len}
          (let [complete (vec (butlast observations))
                last-offset (if (seq complete)
                              (+ (long (:source/byte-offset (last complete)))
                                 (long (:source/byte-length (last complete))))
                              start-offset)]
            {:observations complete
             :next-offset last-offset}))))))

(defn start-transcript-watch!
  [runtime request & [opts]]
  (append-transcript-request! runtime request)
  (await-materialized #(read-run runtime (:transcript/request-id request)) some?)
  (append-transcript-status!
    runtime
    (transcript-run-status-record (:transcript/request-id request) :running))
  (let [stop? (atom false)
        known-at-start (set (walk-jsonl-files (:transcript/paths request)))
        offsets (atom {})
        poll-ms (long (or (:poll-ms opts) 100))
        initialize-offset
        (fn [file created-after-start?]
          (let [fid (file-id file)
                state (read-source-file-state runtime (:transcript/source request) fid)]
            (cond
              (:source/last-byte-offset state)
              (:source/last-byte-offset state)

              created-after-start?
              0

              (:backfill? opts)
              0

              :else
              (.length ^File file))))
        poll-once!
        (fn []
          (let [files (set (walk-jsonl-files (:transcript/paths request)))]
            (doseq [file files]
              (let [new-file? (not (contains? known-at-start file))
                    start-offset (if (contains? @offsets file)
                                   (get @offsets file)
                                   (initialize-offset file new-file?))
                    {:keys [observations next-offset]}
                    (read-complete-appended-lines request file start-offset)]
                (swap! offsets assoc file next-offset)
                (doseq [obs observations]
                  (append-transcript-observation! runtime obs))))))]
    (let [thread (doto (Thread.
                         ^Runnable
                         (reify Runnable
                           (run [_]
                             (while (not @stop?)
                               (try
                                 (poll-once!)
                                 (catch Throwable t
                                   (append-transcript-status!
                                     runtime
                                     (transcript-run-status-record
                                       (:transcript/request-id request)
                                       :failed
                                       {:error {:message (.getMessage t)}}))))
                               (Thread/sleep poll-ms))))
                         (str "transcript-watch-" (:transcript/request-id request)))
                   (.setDaemon true)
                   (.start))]
      {:transcript/request-id (:transcript/request-id request)
       :thread thread
       :poll-once! poll-once!
       :stop! (fn []
                (reset! stop? true)
                (.join thread 1000)
                (append-transcript-status!
                  runtime
                  (transcript-run-status-record (:transcript/request-id request)
                                                :cancelled)))})))
