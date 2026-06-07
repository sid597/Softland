(ns app.server.rama.dogfood.transcript-ingest
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as core]
            [app.server.rama.dogfood.transcript :as t]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (java.io File)))

;; ────────────────────────────────────────────────────────────────────────────────
;;   TRANSCRIPT INGEST MODULE
;;
;;   Ingests Claude Code and Codex JSONL transcript files (via harvest/watch)
;;   and creates native Softland object containers. Extends the existing
;;   transcript.clj parser/acquisition layer with object-container
;;   materialization, conversation projection, composition edges, source
;;   anchors, tool-call indexes, and audit entries.
;;
;;   All PState writes are keypath + termval (complete row overwrites).
;;   All container IDs are deterministic (sha256-based, no random UUIDs in topology).
;;   Stream retry safe: every write is naturally idempotent.
;; ────────────────────────────────────────────────────────────────────────────────

;; ── ID computation ──────────────────────────────────────────────────────────────

(defn conv-key
  "Deterministic conversation key: sha256(source-family + \":\" + conversation-id)"
  [source conversation-id]
  (core/sha-256 (str (name source) ":" conversation-id)))

(defn conversation-container-id
  [ck]
  (str "tc:conv:" ck))

(defn message-container-id
  [ck message-hash]
  (str "tc:msg:" ck ":" message-hash))

(defn tool-call-container-id
  [ck tool-use-id-hash]
  (str "tc:tc:" ck ":" tool-use-id-hash))

(defn tool-result-container-id
  [ck tool-use-id-hash]
  (str "tc:tr:" ck ":" tool-use-id-hash))

(defn artifact-container-id
  [ck artifact-id-hash]
  (str "tc:art:" ck ":" artifact-id-hash))

(defn message-uuid-or-fallback
  "Returns sha256 of message-uuid if present, otherwise sha256(file-id + \":\" + byte-offset)."
  [obs]
  (let [uuid (:transcript/message-uuid obs)]
    (if (and uuid (not (str/blank? (str uuid))))
      (core/sha-256 (str uuid))
      (core/sha-256 (str (pr-str (:source/file-id obs)) ":" (:source/byte-offset obs))))))

(defn message-order-key
  "Zero-padded byte offset for sort ordering."
  [byte-offset]
  (format "%010d" (long byte-offset)))

(defn edge-key
  [edge-type child-id]
  (str (name edge-type) ":" child-id))

(defn content-text-from-payload
  "Extract redacted text content from observation payload."
  [obs]
  (or (:transcript/redacted-preview obs)
      (when-let [payload (:transcript/redacted-payload obs)]
        (let [content (or (get-in payload [:message :content])
                          (:content payload))]
          (cond
            (string? content) content
            (sequential? content)
            (let [text-blocks (filter #(and (map? %)
                                            (= "text" (:type %)))
                                      content)]
              (str/join "\n" (map :text text-blocks)))
            :else nil)))))

(defn role-from-payload
  [obs]
  (let [payload (:transcript/redacted-payload obs)]
    (when payload
      (keyword (or (get-in payload [:message :role])
                   (:role payload)
                   (name (:transcript/event-type obs)))))))

(defn redacted-input-for-tool
  "First 200 chars of redacted tool input."
  [block]
  (let [input (or (:input block) {})]
    (subs (pr-str input) 0 (min 200 (count (pr-str input))))))

(def secret-patterns
  [#"(?i)(sk-[a-zA-Z0-9]{20,})"
   #"(?i)(AKIA[A-Z0-9]{16})"
   #"(?i)(ghp_[a-zA-Z0-9]{36})"
   #"(?i)(gho_[a-zA-Z0-9]{36})"
   #"(?i)(xox[bpas]-[a-zA-Z0-9\-]+)"
   #"(?i)(eyJ[a-zA-Z0-9_-]{20,}\.[a-zA-Z0-9_-]+)"
   #"(?i)[\"']?(api[_-]?key|secret|token|password|authorization)[\"']?\s*[:=]\s*[\"']?([^\s\"',}{]{8,})[\"']?"
   #"(?i)[\"']?(api[_-]?key|secret|token|password|authorization)[\"']?\s*:\s*[\"']([^\"]{8,})[\"']"])

(defn redact-raw-string
  "Apply pattern-based secret redaction to a raw string. Used for malformed
   JSON lines that cannot be structurally redacted."
  [s]
  (reduce (fn [text pattern]
            (str/replace text pattern "<REDACTED>"))
          (str s)
          secret-patterns))

;; ── Custom partitioner ──────────────────────────────────────────────────────────

(defn- extract-after-prefix
  "Extract conv-key from the string after a known prefix. Conv-key ends at the next colon."
  [s prefix-len]
  (let [rest-str (subs s prefix-len)
        idx (str/index-of rest-str ":")]
    (if idx (subs rest-str 0 idx) rest-str)))

(defn extract-conv-key
  "Extracts conv-key from tc:conv:, tc:msg:, tc:tc:, tc:tr:, tc:art: prefixed IDs."
  [id-or-key]
  (let [s (str id-or-key)]
    (cond
      (str/starts-with? s "tc:conv:") (subs s 8)
      (str/starts-with? s "tc:msg:")  (extract-after-prefix s 7)
      (str/starts-with? s "tc:art:")  (extract-after-prefix s 7)
      (str/starts-with? s "tc:tc:")   (extract-after-prefix s 6)
      (str/starts-with? s "tc:tr:")   (extract-after-prefix s 6)
      :else s)))

(defn positive-partition
  [num-partitions k]
  (if (pos? num-partitions)
    (mod (hash k) num-partitions)
    0))

(defn partition-by-conv-key
  [num-partitions id-or-key]
  (positive-partition num-partitions (extract-conv-key id-or-key)))

;; ── Row types ───────────────────────────────────────────────────────────────────

(defrecord IngestRunRow
  [request-id request-type status source paths redaction-policy triggered-by
   created-at-ms updated-at-ms observed-line-count parse-error-count
   containers-created-count error])

(definterface IConversationEntry)

(defrecord ConversationMetaEntry
  [entry-type conversation-id conv-key source source-version session-metadata
   message-count first-timestamp last-timestamp source-file-path source-file-id
   created-at-ms]
  IConversationEntry)

(defrecord ConversationMessageEntry
  [entry-type container-id order-key role content-text content-hash timestamp
   source-anchor-summary tool-calls tool-results event-type parse-error-kind
   redacted-preview]
  IConversationEntry)

(defrecord ToolCallSummary
  [container-id tool-use-id tool-name redacted-input])

(defrecord ToolResultSummary
  [container-id tool-use-id redacted-content])

(defrecord TranscriptContainerRow
  [container-id container-kind conv-key conversation-id source visibility
   revision-content revision-metadata source-anchor created-at-ms
   created-by-request-id])

(defrecord CompositionEdgeRow
  [edge-key edge-type parent-id child-id child-kind order-key created-at-ms])

(defrecord SourceAnchorRow
  [container-id source file-id file-path byte-offset byte-length line-hash])

(defrecord SourceArtifactRow
  [conversation-id conv-key source file-id file-path source-version
   first-ingested-at-ms last-ingested-at-ms last-ingested-by-request])

(defrecord FileOffsetRow
  [source-file-key file-id file-path source conv-key conversation-id
   last-byte-offset line-count last-request-id updated-at-ms])

(defrecord ToolCallIndexRow
  [tool-name tool-use-id container-id conversation-id message-uuid
   source-line-key timestamp redacted-input-preview])

(defrecord AuditEntryRow
  [source-line-key event-type parse-error-kind redactions-count
   containers-created container-ids file-path byte-offset timestamp
   processed-at-ms])

;; ── Helper fns for topology (named, not keywords-as-functions) ──────────────────

(defn obs-conv-key [obs] (:transcript/conv-key obs))
(defn obs-source [obs] (:transcript/source obs))
(defn obs-source-version [obs] (:transcript/source-version obs))
(defn obs-conversation-id [obs] (:transcript/conversation-id obs))
(defn obs-message-uuid [obs] (:transcript/message-uuid obs))
(defn obs-event-type [obs] (:transcript/event-type obs))
(defn obs-source-timestamp [obs] (:transcript/source-timestamp obs))
(defn obs-redacted-payload [obs] (:transcript/redacted-payload obs))
(defn obs-redacted-preview [obs] (:transcript/redacted-preview obs))
(defn obs-parse-error-kind [obs] (:transcript/parse-error-kind obs))
(defn obs-ingest-request-id [obs] (:transcript/ingest-request-id obs))
(defn obs-file-id [obs] (:source/file-id obs))
(defn obs-file-path [obs] (:source/file-path obs))
(defn obs-byte-offset [obs] (:source/byte-offset obs))
(defn obs-byte-length [obs] (:source/byte-length obs))
(defn obs-line-hash [obs] (:source/line-hash obs))
(defn obs-redactions [obs] (:transcript/redactions obs))

(defn file-state-file-key [fs] (:source/file-key fs))
(defn file-state-conv-key [fs] (:transcript/conv-key fs))
(defn file-state-conversation-id [fs] (:transcript/conversation-id fs))
(defn file-state-request-id [fs] (:transcript/ingest-request-id fs))
(defn file-state-is-empty [fs] (:source/is-empty fs))
(defn file-state-file-id [fs] (:source/file-id fs))
(defn file-state-file-path [fs] (:source/file-path fs))
(defn file-state-source [fs] (:transcript/source fs))
(defn file-state-last-byte-offset [fs] (:source/last-byte-offset fs))
(defn file-state-line-count [fs] (:source/line-count fs))
(defn file-state-time-ms [fs] (:time-ms fs))

(defn request-id-from [request] (:transcript/request-id request))
(defn request-type-from [request] (:request/type request))
(defn request-source [request] (:transcript/source request))
(defn request-paths [request] (:transcript/paths request))
(defn request-redaction-policy [request] (:transcript/redaction-policy request))
(defn request-triggered-by [request] (:transcript/triggered-by request))
(defn request-time-ms [request] (:request/time-ms request))

(defn claim-request-id [claim] (:transcript/request-id claim))
(defn claim-status [claim] (:status claim))
(defn claim-time-ms [claim] (:time-ms claim))
(defn claim-counts [claim] (:counts claim))
(defn claim-error [claim] (:error claim))

(defn is-parse-error [obs]
  (some? (obs-parse-error-kind obs)))

(defn run-accepted?
  "True if the run exists — meaning a request was submitted and acknowledged.
   Does not gate on run status: observations may arrive after the executor
   sends :complete (different depots, no cross-depot ordering guarantee)."
  [run-row]
  (some? run-row))

;; ── Row construction helpers ────────────────────────────────────────────────────

(defn make-initial-run-row
  [request]
  (->IngestRunRow (request-id-from request)
                  (request-type-from request)
                  :pending
                  (request-source request)
                  (request-paths request)
                  (request-redaction-policy request)
                  (request-triggered-by request)
                  (request-time-ms request)
                  (request-time-ms request)
                  0 0 0 nil))

(defn make-rejected-run-row
  [request errors]
  (->IngestRunRow (request-id-from request)
                  (request-type-from request)
                  :failed
                  (request-source request)
                  (request-paths request)
                  (request-redaction-policy request)
                  (request-triggered-by request)
                  (request-time-ms request)
                  (request-time-ms request)
                  0 0 0 {:errors (vec errors)}))

(def terminal-run-statuses #{:complete :failed :cancelled})

(defn fold-claim-into-run
  "Fold a claim (status update) into an existing IngestRunRow.
   Terminal states are monotonic — late claims cannot overwrite them."
  [run-row claim]
  (if (contains? terminal-run-statuses (:status run-row))
    run-row
    (let [t (claim-time-ms claim)
          counts (claim-counts claim)
          err (claim-error claim)]
      (-> run-row
          (assoc :status (claim-status claim)
                 :updated-at-ms t)
          (cond-> counts (-> (assoc :observed-line-count (or (:observed-line-count counts)
                                                             (:observed-line-count run-row)))
                             (assoc :parse-error-count (or (:parse-error-count counts)
                                                           (:parse-error-count run-row))))
                  err (assoc :error err))))))

(defn increment-run-progress
  "Increment observation counts on the run row. Approximate under retry."
  [run-row obs]
  (-> run-row
      (update :observed-line-count inc)
      (cond-> (is-parse-error obs)
        (update :parse-error-count inc))
      (assoc :updated-at-ms (core/now-ms))))

(defn increment-run-containers
  "Increment container-created count on the run row."
  [run-row n]
  (-> run-row
      (update :containers-created-count + n)
      (assoc :updated-at-ms (core/now-ms))))

(defn make-source-anchor-row
  [container-id obs]
  (->SourceAnchorRow container-id
                     (obs-source obs)
                     (obs-file-id obs)
                     (obs-file-path obs)
                     (obs-byte-offset obs)
                     (obs-byte-length obs)
                     (obs-line-hash obs)))

(defn make-source-artifact-row
  [conversation-id ck source file-id file-path source-version request-id now]
  (->SourceArtifactRow conversation-id ck source file-id file-path source-version
                       now now request-id))

(defn make-audit-entry-row
  [obs container-ids now]
  (->AuditEntryRow (t/source-line-key obs)
                   (obs-event-type obs)
                   (obs-parse-error-kind obs)
                   (count (or (obs-redactions obs) []))
                   (count container-ids)
                   (vec container-ids)
                   (obs-file-path obs)
                   (obs-byte-offset obs)
                   (obs-source-timestamp obs)
                   now))

;; ── Materialization helpers (pure fns called from topology) ─────────────────────

(defn build-message-containers
  "Build all container rows, edges, anchors, projection entry, and tool-call index
   rows for a single observation. Returns a map of materialization data."
  [obs ck now]
  (if (is-parse-error obs)
    ;; Parse errors: no containers, but create a projection entry for visibility
    {:containers []
     :projection-entry (->ConversationMessageEntry
                         :message nil (message-order-key (obs-byte-offset obs))
                         nil nil nil (obs-source-timestamp obs)
                         {:file-path (obs-file-path obs)
                          :byte-offset (obs-byte-offset obs)
                          :line-hash (obs-line-hash obs)}
                         [] [] (obs-event-type obs)
                         (obs-parse-error-kind obs) (obs-redacted-preview obs))
     :edges []
     :tool-call-indexes []
     :container-ids []}
    (let [msg-hash (message-uuid-or-fallback obs)
          msg-id (message-container-id ck msg-hash)
          conv-id (conversation-container-id ck)
          order-k (message-order-key (obs-byte-offset obs))
          request-id (obs-ingest-request-id obs)
          source (obs-source obs)
          content (content-text-from-payload obs)
          content-hash (when content (core/sha-256 content))
          role (role-from-payload obs)
          anchor (make-source-anchor-row msg-id obs)
          tool-blocks (t/parsed-tool-use-blocks (or (obs-redacted-payload obs) {}))

          ;; Build tool-call and tool-result containers
          tool-data
          (vec
            (for [block tool-blocks
                  :let [tool-use-id (or (:id block) (:tool_use_id block) (:tool-use-id block))
                        tool-name (:name block)]
                  :when (and tool-use-id (seq (str tool-use-id)))]
              (let [tuid-hash (core/sha-256 (str tool-use-id))
                    tc-id (tool-call-container-id ck tuid-hash)
                    tr-id (tool-result-container-id ck tuid-hash)
                    tc-container (->TranscriptContainerRow
                                   tc-id :tool-call ck (obs-conversation-id obs)
                                   source :private nil
                                   {:tool-name tool-name
                                    :tool-use-id tool-use-id
                                    :timestamp (obs-source-timestamp obs)}
                                   anchor now request-id)
                    tr-container (->TranscriptContainerRow
                                   tr-id :tool-result ck (obs-conversation-id obs)
                                   source :private nil
                                   {:tool-use-id tool-use-id
                                    :timestamp (obs-source-timestamp obs)}
                                   anchor now request-id)
                    tc-edge (->CompositionEdgeRow
                              (edge-key :produced tc-id) :produced
                              msg-id tc-id :tool-call order-k now)
                    tr-edge (->CompositionEdgeRow
                              (edge-key :produced tr-id) :produced
                              msg-id tr-id :tool-result order-k now)
                    tc-summary (->ToolCallSummary tc-id tool-use-id tool-name
                                                  (redacted-input-for-tool block))
                    tr-summary (->ToolResultSummary tr-id tool-use-id nil)
                    tc-index (->ToolCallIndexRow
                               tool-name tool-use-id tc-id
                               (obs-conversation-id obs)
                               (obs-message-uuid obs)
                               (t/source-line-key obs)
                               (obs-source-timestamp obs)
                               (redacted-input-for-tool block))]
                {:tc-container tc-container
                 :tr-container tr-container
                 :tc-edge tc-edge
                 :tr-edge tr-edge
                 :tc-summary tc-summary
                 :tr-summary tr-summary
                 :tc-index tc-index
                 :tc-id tc-id
                 :tr-id tr-id
                 :tool-name tool-name})))

          ;; Message container
          msg-container (->TranscriptContainerRow
                          msg-id :chat-message ck (obs-conversation-id obs)
                          source :private content
                          {:role role
                           :timestamp (obs-source-timestamp obs)
                           :event-type (obs-event-type obs)}
                          anchor now request-id)

          ;; Composition edges: conv contains msg, msg follows prev (not tracked here)
          contains-edge (->CompositionEdgeRow
                          (edge-key :contains msg-id) :contains
                          conv-id msg-id :chat-message order-k now)

          ;; Conversation projection entry
          projection-entry (->ConversationMessageEntry
                             :message msg-id order-k role content content-hash
                             (obs-source-timestamp obs)
                             {:file-path (obs-file-path obs)
                              :byte-offset (obs-byte-offset obs)
                              :line-hash (obs-line-hash obs)}
                             (vec (map :tc-summary tool-data))
                             (vec (map :tr-summary tool-data))
                             (obs-event-type obs)
                             (obs-parse-error-kind obs)
                             (obs-redacted-preview obs))

          all-containers (into [msg-container]
                               (mapcat (fn [td] [(:tc-container td) (:tr-container td)])
                                       tool-data))
          all-edges (into [contains-edge]
                          (mapcat (fn [td] [(:tc-edge td) (:tr-edge td)])
                                  tool-data))
          all-container-ids (into [msg-id]
                                  (mapcat (fn [td] [(:tc-id td) (:tr-id td)])
                                          tool-data))]
      {:containers all-containers
       :projection-entry projection-entry
       :edges all-edges
       :tool-call-indexes (mapv :tc-index tool-data)
       :container-ids all-container-ids
       :msg-id msg-id
       :anchor anchor})))

;; ── Topology-callable wrappers (named fns, no keywords/interop in op position) ─

(defn build-obs-materials
  "Topology-callable: builds materialization data from an observation."
  [obs ck now]
  (build-message-containers obs ck now))

(defn materials-containers [m] (:containers m))
(defn materials-projection-entry [m] (:projection-entry m))
(defn materials-edges [m] (:edges m))
(defn materials-tool-call-indexes [m] (:tool-call-indexes m))
(defn materials-container-ids [m] (:container-ids m))
(defn materials-msg-id [m] (:msg-id m))
(defn materials-anchor [m] (:anchor m))

(defn container-id-from [c] (:container-id c))
(defn edge-key-from [e] (:edge-key e))
(defn edge-parent-id [e] (:parent-id e))
(defn anchor-container-id [a] (:container-id a))

(defn tool-index-name [ti] (:tool-name ti))
(defn tool-index-line-key [ti] (:source-line-key ti))

(defn conv-entry-order-key [pe] (:order-key pe))

(defn run-row-some? [r] (some? r))

(defn extract-session-metadata
  "Extract session metadata from observation payload for the meta entry."
  [obs]
  (when-let [p (obs-redacted-payload obs)]
    (select-keys p [:model :tools :plugins :mcpClients :session_id])))

(defn make-conversation-meta-entry
  "Build a ConversationMetaEntry from an observation."
  [obs ck conv-id now]
  (->ConversationMetaEntry
    :meta conv-id ck (obs-source obs) (obs-source-version obs)
    (extract-session-metadata obs)
    0 (obs-source-timestamp obs) (obs-source-timestamp obs)
    (obs-file-path obs) (obs-file-id obs) now))
(defn materials-has-projection [m] (some? (:projection-entry m)))
(defn materials-has-containers [m] (seq (materials-containers m)))
(defn tool-indexes-seq [m] (seq (materials-tool-call-indexes m)))

(defn container-count [m] (count (or (materials-container-ids m) [])))

;; ── Module definition ───────────────────────────────────────────────────────────

(defmodule TranscriptIngestModule [setup topologies]
  (declare-depot setup *transcript-ingest-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-claim-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-obs-depot (hash-by :transcript/conv-key))
  (declare-depot setup *transcript-file-state-depot (hash-by :source/file-key))

  (let [s (stream-topology topologies "transcript-ingest-topology")]

    ;; ── PStates ─────────────────────────────────────────────────────────────
    (declare-pstate s $$ingest-runs {String IngestRunRow})
    (declare-pstate s $$source-ledger {String Boolean} {:private? true})
    (declare-pstate s $$files-handled {String Boolean} {:private? true})
    (declare-pstate s $$file-offsets {String FileOffsetRow})
    (declare-pstate s $$last-msg-per-conv {String String} {:private? true})
    (declare-pstate s $$containers-by-id
                    {String TranscriptContainerRow}
                    {:key-partitioner partition-by-conv-key})
    (declare-pstate s $$conversation-projection
                    {String (map-schema String IConversationEntry {:subindex? true})}
                    {:key-partitioner partition-by-conv-key})
    (declare-pstate s $$composition-edges-by-parent
                    {String (map-schema String CompositionEdgeRow {:subindex? true})}
                    {:key-partitioner partition-by-conv-key})
    (declare-pstate s $$source-anchors-by-container
                    {String SourceAnchorRow}
                    {:key-partitioner partition-by-conv-key})
    (declare-pstate s $$source-artifacts
                    {String SourceArtifactRow}
                    {:key-partitioner partition-by-conv-key})
    (declare-pstate s $$tool-calls-by-name
                    {String (map-schema String ToolCallIndexRow {:subindex? true})})
    (declare-pstate s $$audit-entries
                    {String (map-schema String AuditEntryRow {:subindex? true})})

    ;; ── Stream topology body ────────────────────────────────────────────────
    (<<sources s

      ;; ── Request source ──────────────────────────────────────────────────
      (source> *transcript-ingest-depot :> *request)
      (request-id-from *request :> *request-id)
      (t/request-validation-errors *request :> *errors)
      (<<if (empty? *errors)
        (make-initial-run-row *request :> *run-row)
        (local-transform> [(keypath *request-id) (termval *run-row)] $$ingest-runs)
        (else>)
        (make-rejected-run-row *request *errors :> *run-row)
        (local-transform> [(keypath *request-id) (termval *run-row)] $$ingest-runs))

      ;; ── Claim source ────────────────────────────────────────────────────
      (source> *transcript-claim-depot :> *claim)
      (claim-request-id *claim :> *request-id)
      (local-select> [(keypath *request-id)] $$ingest-runs :> *run-row)
      (<<if (run-row-some? *run-row)
        (fold-claim-into-run *run-row *claim :> *updated-run)
        (local-transform> [(keypath *request-id) (termval *updated-run)] $$ingest-runs))

      ;; ── Observation source ──────────────────────────────────────────────
      (source> *transcript-obs-depot {:retry-mode :all-after} :> *obs)
      ;; Gate: verify the ingest request exists and is in an accepted state
      ;; before materializing any durable facts
      (obs-ingest-request-id *obs :> *request-id)
      (|hash *request-id)
      (local-select> [(keypath *request-id)] $$ingest-runs :> *run-check)
      (<<if (run-accepted? *run-check)
        ;; Repartition to conv-key for PState writes
        (obs-conv-key *obs :> *ck)
        (|hash *ck)
        (t/source-line-key *obs :> *line-key)
        (local-select> [(keypath *line-key)] $$source-ledger :> *existing)
        (<<if (nil? *existing)
          ;; New source record with accepted request — process it
          (local-transform> [(keypath *line-key) (termval true)] $$source-ledger)
          (core/now-ms :> *now)
          (build-obs-materials *obs *ck *now :> *materials)
          (obs-conversation-id *obs :> *conversation-id)
          (conversation-container-id *ck :> *conv-container-id)

        ;; Write conversation container (idempotent — same conv-key always
        ;; produces same conv container ID)
        (local-transform>
          [(keypath *conv-container-id)
           (termval (->TranscriptContainerRow
                      *conv-container-id :chat-conversation *ck *conversation-id
                      (obs-source *obs) :private nil
                      {:source-version (obs-source-version *obs)}
                      nil *now *request-id))]
          $$containers-by-id)

        ;; Write conversation meta entry to projection (idempotent overwrite)
        (make-conversation-meta-entry *obs *ck *conversation-id *now :> *meta-entry)
        (local-transform>
          [(keypath *conv-container-id "") (termval *meta-entry)]
          $$conversation-projection)

        ;; Write per-message containers
        (<<if (materials-has-containers *materials)
          (explode (materials-containers *materials) :> *container)
          (container-id-from *container :> *cid)
          (local-transform> [(keypath *cid) (termval *container)] $$containers-by-id))

        ;; Write conversation projection entry (message or parse-error)
        (materials-projection-entry *materials :> *proj-entry)
        (<<if (some? *proj-entry)
          (conv-entry-order-key *proj-entry :> *order-k)
          (local-transform>
            [(keypath *conv-container-id *order-k) (termval *proj-entry)]
            $$conversation-projection))

        ;; Write :follows edge using tracked previous message per conversation
        (<<if (materials-has-containers *materials)
          (materials-msg-id *materials :> *cur-msg-id)
          (message-order-key (obs-byte-offset *obs) :> *cur-order-k)
          (local-select> [(keypath *conv-container-id)] $$last-msg-per-conv :> *prev-msg-id)
          (<<if (some? *prev-msg-id)
            (local-transform>
              [(keypath *conv-container-id (edge-key :follows *cur-msg-id))
               (termval (->CompositionEdgeRow
                          (edge-key :follows *cur-msg-id) :follows
                          *prev-msg-id *cur-msg-id :chat-message *cur-order-k *now))]
              $$composition-edges-by-parent))
          (local-transform>
            [(keypath *conv-container-id) (termval *cur-msg-id)]
            $$last-msg-per-conv))

        ;; Write composition edges
        (<<if (materials-has-containers *materials)
          (explode (materials-edges *materials) :> *edge)
          (edge-parent-id *edge :> *parent-id)
          (edge-key-from *edge :> *ek)
          (local-transform>
            [(keypath *parent-id *ek) (termval *edge)]
            $$composition-edges-by-parent))

        ;; Write source anchors for each container
        (<<if (materials-has-containers *materials)
          (explode (materials-containers *materials) :> *container)
          (container-id-from *container :> *cid)
          (make-source-anchor-row *cid *obs :> *anchor)
          (local-transform>
            [(keypath *cid) (termval *anchor)]
            $$source-anchors-by-container))

        ;; Write source artifact (per-file, idempotent)
        (t/source-file-key (obs-source *obs) (obs-file-id *obs) :> *sfk)
        (local-select> [(keypath *sfk)] $$files-handled :> *file-seen)
        (<<if (nil? *file-seen)
          (local-transform> [(keypath *sfk) (termval true)] $$files-handled)
          (make-source-artifact-row
            *conversation-id *ck (obs-source *obs) (obs-file-id *obs)
            (obs-file-path *obs) (obs-source-version *obs) *request-id *now :> *sa-row)
          (local-transform>
            [(keypath *conv-container-id) (termval *sa-row)]
            $$source-artifacts))

        ;; Hop to request-id partition for audit + run progress
        (materials-container-ids *materials :> *cids)
        (make-audit-entry-row *obs *cids *now :> *audit-row)
        (container-count *materials :> *n-containers)
        (|hash *request-id)
        (local-transform>
          [(keypath *request-id *line-key) (termval *audit-row)]
          $$audit-entries)
        (local-select> [(keypath *request-id)] $$ingest-runs :> *run-row)
        (<<if (run-row-some? *run-row)
          (increment-run-progress *run-row *obs :> *run-row2)
          (increment-run-containers *run-row2 *n-containers :> *run-row3)
          (local-transform> [(keypath *request-id) (termval *run-row3)] $$ingest-runs))

        ;; Tool-call secondary index: hop to tool-name partition (terminal)
        (<<if (tool-indexes-seq *materials)
          (explode (materials-tool-call-indexes *materials) :> *ti)
          (tool-index-name *ti :> *tool-name)
          (tool-index-line-key *ti :> *ti-key)
          (|hash *tool-name)
          (local-transform>
            [(keypath *tool-name *ti-key) (termval *ti)]
            $$tool-calls-by-name))))

      ;; ── File-state source ───────────────────────────────────────────────
      (source> *transcript-file-state-depot :> *fs)
      ;; Already on file-key partition (depot hash-by source/file-key)
      (file-state-file-key *fs :> *fk)
      (file-state-conv-key *fs :> *fs-ck)
      (file-state-conversation-id *fs :> *fs-conv-id)
      (file-state-request-id *fs :> *fs-request-id)
      (conversation-container-id *fs-ck :> *fs-conv-container-id)

      ;; Write file offset (termval overwrite with latest)
      (file-state-last-byte-offset *fs :> *fs-last-offset)
      (file-state-line-count *fs :> *fs-line-count)
      (file-state-time-ms *fs :> *fs-time-ms)
      (local-transform>
        [(keypath *fk)
         (termval (->FileOffsetRow
                    *fk
                    (file-state-file-id *fs) (file-state-file-path *fs)
                    (file-state-source *fs) *fs-ck *fs-conv-id
                    *fs-last-offset
                    *fs-line-count
                    *fs-request-id
                    *fs-time-ms))]
        $$file-offsets)

      ;; For empty files: create source artifact and audit entry on conv partition
      (<<if (file-state-is-empty *fs)
        (|hash *fs-ck)
        (core/now-ms :> *fs-now)
        (make-source-artifact-row
          *fs-conv-id *fs-ck (file-state-source *fs) (file-state-file-id *fs)
          (file-state-file-path *fs) :unknown *fs-request-id *fs-now :> *fs-sa-row)
        (local-transform>
          [(keypath *fs-conv-container-id) (termval *fs-sa-row)]
          $$source-artifacts)
        ;; Audit entry for empty file
        (identity (str "empty-file:" *fk) :> *sentinel-key)
        (|hash *fs-request-id)
        (local-transform>
          [(keypath *fs-request-id *sentinel-key)
           (termval (->AuditEntryRow *sentinel-key :empty-file nil 0 0 []
                                     (file-state-file-path *fs) 0 nil *fs-now))]
          $$audit-entries)))))

;; ── Foreign client helpers ──────────────────────────────────────────────────────

(defn start-transcript-ingest-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name TranscriptIngestModule)]
    (launch-module! ipc TranscriptIngestModule {:tasks 4 :threads 2})
    {:ipc ipc
     :module-name module-name
     :ingest-depot (foreign-depot ipc module-name "*transcript-ingest-depot")
     :claim-depot (foreign-depot ipc module-name "*transcript-claim-depot")
     :obs-depot (foreign-depot ipc module-name "*transcript-obs-depot")
     :file-state-depot (foreign-depot ipc module-name "*transcript-file-state-depot")
     :ingest-runs (foreign-pstate ipc module-name "$$ingest-runs")
     :containers-by-id (foreign-pstate ipc module-name "$$containers-by-id")
     :conversation-projection (foreign-pstate ipc module-name "$$conversation-projection")
     :composition-edges (foreign-pstate ipc module-name "$$composition-edges-by-parent")
     :source-anchors (foreign-pstate ipc module-name "$$source-anchors-by-container")
     :source-artifacts (foreign-pstate ipc module-name "$$source-artifacts")
     :tool-calls-by-name (foreign-pstate ipc module-name "$$tool-calls-by-name")
     :audit-entries (foreign-pstate ipc module-name "$$audit-entries")
     :file-offsets (foreign-pstate ipc module-name "$$file-offsets")}))

(defn close-transcript-ingest-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

;; ── Depot appends ───────────────────────────────────────────────────────────────

(defn append-ingest-request!
  "Submit a harvest or watch request. Uses :ack so caller can read-after-write."
  [runtime request]
  (foreign-append! (:ingest-depot runtime) request :ack)
  request)

(defn append-ingest-claim!
  "Submit an executor lifecycle claim (running/complete/failed/cancelled)."
  [runtime claim]
  (foreign-append! (:claim-depot runtime) claim :append-ack)
  claim)

(defn append-ingest-observation!
  "Submit a per-record source observation."
  [runtime obs]
  (foreign-append! (:obs-depot runtime) obs :append-ack)
  obs)

(defn append-file-state!
  "Submit a per-file offset/state record."
  [runtime fs]
  (foreign-append! (:file-state-depot runtime) fs :append-ack)
  fs)

;; ── Reads ───────────────────────────────────────────────────────────────────────

(defn read-ingest-run
  "R5 — run status by request-id."
  [runtime request-id]
  (foreign-select-one [(keypath request-id)] (:ingest-runs runtime)))

(defn read-conversation-projection
  "R1 — full conversation: metadata + ordered messages with inline tool calls."
  [runtime conversation-id]
  (foreign-select [(keypath conversation-id) ALL] (:conversation-projection runtime)))

(defn read-container
  "R4 — single container by ID."
  [runtime container-id]
  (foreign-select-one [(keypath container-id)] (:containers-by-id runtime)))

(defn read-tool-calls-by-name
  "R3 — tool-call index entries for a given tool name."
  [runtime tool-name]
  (foreign-select [(keypath tool-name) ALL] (:tool-calls-by-name runtime)))

(defn read-source-artifact
  "R6 — file-level source metadata by conversation container ID."
  [runtime conversation-id]
  (foreign-select-one [(keypath conversation-id)] (:source-artifacts runtime)))

(defn read-audit-entries
  "R2 — audit entries for a harvest/watch request."
  [runtime request-id]
  (foreign-select [(keypath request-id) ALL] (:audit-entries runtime)))

(defn read-file-offset
  "File byte-offset for watch resume."
  [runtime file-key]
  (foreign-select-one [(keypath file-key)] (:file-offsets runtime)))

;; ── Request construction ────────────────────────────────────────────────────────

(defn redact-observation-preview
  "Ensure parse-error previews are redacted before depot append."
  [obs]
  (if-let [preview (:transcript/redacted-preview obs)]
    (assoc obs :transcript/redacted-preview (redact-raw-string preview))
    obs))

(defn prepare-observation
  "Add conv-key and redact preview before depot append."
  [obs source]
  (let [conversation-id (obs-conversation-id obs)
        ck (conv-key source conversation-id)]
    (-> obs
        (assoc :transcript/conv-key ck)
        redact-observation-preview)))

(defn transcript-ingest-request
  "Build a harvest or watch request map."
  [request-type & [opts]]
  (t/transcript-request request-type opts))

;; ── Executor orchestration ──────────────────────────────────────────────────────

(defn harvest-ingest!
  "Full harvest pipeline: request -> walk -> parse -> append observations."
  [runtime request]
  (append-ingest-request! runtime request)
  (append-ingest-claim!
    runtime
    (t/transcript-run-status-record (request-id-from request) :running))
  (let [files (t/walk-jsonl-files (request-paths request))
        request-id (request-id-from request)
        source (request-source request)]
    (doseq [^File file files]
      (let [fid (t/file-id file)
            observations (t/read-jsonl-observations request file 0)
            sfk (t/source-file-key source fid)]
        ;; Append per-line observations with conv-key and redacted previews
        (doseq [obs observations]
          (append-ingest-observation! runtime (prepare-observation obs source)))
        ;; Append file-state observation
        (let [last-obs (last observations)
              conversation-id (if last-obs
                                (obs-conversation-id last-obs)
                                (.getPath file))
              ck (conv-key source conversation-id)]
          (append-file-state!
            runtime
            {:source/file-key sfk
             :source/file-id fid
             :source/file-path (.getPath file)
             :transcript/source source
             :transcript/conv-key ck
             :transcript/conversation-id conversation-id
             :transcript/ingest-request-id request-id
             :source/last-byte-offset (if last-obs
                                        (+ (long (obs-byte-offset last-obs))
                                           (long (obs-byte-length last-obs)))
                                        0)
             :source/line-count (count observations)
             :source/is-empty (empty? observations)
             :time-ms (core/now-ms)}))))
    (append-ingest-claim!
      runtime
      (t/transcript-run-status-record request-id :complete))
    (t/await-materialized
      #(read-ingest-run runtime request-id)
      #(= :complete (:status %)))
    {:transcript/request-id request-id
     :files (count files)}))

(defn start-watch-ingest!
  "Watch pipeline: request -> poll/watch -> parse -> append. Returns stop handle."
  [runtime request & [opts]]
  (append-ingest-request! runtime request)
  (append-ingest-claim!
    runtime
    (t/transcript-run-status-record (request-id-from request) :running))
  (let [stop? (atom false)
        request-id (request-id-from request)
        source (request-source request)
        poll-ms (long (or (:poll-ms opts) 100))
        offsets (atom {})
        poll-once!
        (fn []
          (let [files (t/walk-jsonl-files (request-paths request))]
            (doseq [^File file files]
              (let [fid (t/file-id file)
                    sfk (t/source-file-key source fid)
                    start-offset (if (contains? @offsets sfk)
                                   (get @offsets sfk)
                                   (let [saved (read-file-offset runtime sfk)]
                                     (if saved
                                       (:last-byte-offset saved)
                                       (.length ^File file))))
                    {:keys [observations next-offset]}
                    (t/read-complete-appended-lines request file start-offset)]
                (swap! offsets assoc sfk next-offset)
                (doseq [obs observations]
                  (append-ingest-observation! runtime (prepare-observation obs source)))
                (when (seq observations)
                  (let [last-obs (last observations)
                        conversation-id (obs-conversation-id last-obs)
                        ck (conv-key source conversation-id)]
                    (append-file-state!
                      runtime
                      {:source/file-key sfk
                       :source/file-id fid
                       :source/file-path (.getPath file)
                       :transcript/source source
                       :transcript/conv-key ck
                       :transcript/conversation-id conversation-id
                       :transcript/ingest-request-id request-id
                       :source/last-byte-offset next-offset
                       :source/line-count (count observations)
                       :source/is-empty false
                       :time-ms (core/now-ms)})))))))]
    (let [thread (doto (Thread.
                         ^Runnable
                         (reify Runnable
                           (run [_]
                             (while (not @stop?)
                               (try
                                 (poll-once!)
                                 (catch Throwable t
                                   (append-ingest-claim!
                                     runtime
                                     (t/transcript-run-status-record
                                       request-id :failed
                                       {:error {:message (.getMessage t)}}))))
                               (Thread/sleep poll-ms))))
                         (str "transcript-watch-ingest-" request-id))
                   (.setDaemon true)
                   (.start))]
      {:transcript/request-id request-id
       :thread thread
       :poll-once! poll-once!
       :stop! (fn []
                (reset! stop? true)
                (.join thread 1000)
                (append-ingest-claim!
                  runtime
                  (t/transcript-run-status-record request-id :cancelled)))})))
