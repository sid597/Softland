(ns app.server.rama.object-container
  "Common stored material and its transcript operational tracking.
   object-container-module accepts prepared import bundles and object edits,
   journals decisions, and writes source artifacts, containers, revisions, units,
   anchors, composition, and source/projection indexes. Adapters prepare material
   outside this namespace; the topology validates and applies it.
   object-container-transcript-ops-module owns run/file observations and resume
   offsets. It reads common-module source-line completions through a mirror;
   observing a file is distinct from accepting its material.
   Both modules use streams. Object-family writes and later source/tool/audit/file
   index writes cross task boundaries; the whole import is not one transaction.
   object-container/runtime contains foreign handles, append/read wrappers, and
   IPC lifecycle helpers. door/cluster supplies durable-cluster handle maps."
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.envelope :as envelope]
            [app.server.rama.object-container.transcript-identity :as transcript-identity]
            [clojure.set :as set]
            [clojure.string :as str]))

;; Source artifacts and derived units preserve imported material. Authored
;; containers point at revisions, and graduation connects an edited unit to its
;; container. Anchors retain source provenance; composition and outline are
;; separate relationship/projection rows. See README.md for adjacent modules.

(def object-key-separator (str (char 0)))
(def default-outline-page-size 1000)
(def default-transcript-offset-advance-limit 1000)
(def transcript-control-request-types
  #{:transcript/harvest
    :transcript/watch
    :transcript/run-status})
(def transcript-file-state-request-type :transcript/file-state)
(def transcript-sources #{:claude-code :codex :future/source})
(def transcript-redaction-policies #{:standard})
(def terminal-transcript-run-statuses #{:complete :failed :cancelled})

(defprotocol IObjectContainerRequestPayload)
(defprotocol IObjectContainerEventPayload)

(defrecord ActorRow [actor-id actor-type actor-capabilities])
(defrecord TargetRefRow [target-kind target-id target-address])

(defrecord SourceIngestPayload
  [source-ref source-hash source-raw-text source-format distiller-id distiller-version]
  IObjectContainerRequestPayload)

(defrecord ObjectEditPayload
  [document-container-id object-key content-text content-hash edit-client-id edit-seq
   edit-lineage-key revision-id]
  IObjectContainerRequestPayload)

(defrecord SourceIngestedPayload
  [source-id source-ref source-hash document-container-id distiller-id distiller-version
   derived-unit-count composition-edge-count]
  IObjectContainerEventPayload)

(defrecord ObjectGraduatedPayload
  [unit-id container-id document-container-id revision-id content-hash edit-client-id
   edit-seq]
  IObjectContainerEventPayload)

(defrecord ObjectRevisedPayload
  [container-id source-unit-id document-container-id parent-revision-id revision-id
   content-hash edit-client-id edit-seq]
  IObjectContainerEventPayload)

(defrecord UnitReadResult [unit graduation target-kind target-id content-text content-hash])

(defrecord CommonMaterialBundle [containers derived-units anchors edges])

(defrecord ObjectContainerRequestRow
  [audit-id partition-key request-id request-type routing-key idempotency-key actor target
   payload requested-at-ms raw-request])

(defrecord ObjectContainerDecisionRow
  [decision-id audit-id partition-key request-id request-type idempotency-key status reason
   errors event-id event-row material-fingerprint import-completion-key
   conflict-with-decision-id decided-at-ms replayed-from-decision-id])

(defrecord ObjectContainerEventRow
  [event-id event-type object-key target-kind target-id actor payload event-time-ms
   request-id audit-id])

(defrecord SourceArtifactRow
  [source-id source-ref source-hash source-format source-raw-text document-container-id
   content-byte-count created-at-ms created-by event-id])

(defrecord SourceVersionRow
  [source-ref-key source-ref source-hash source-id document-container-id object-key
   order-key created-at-ms event-id])

(defrecord SourceIngestCompletionRow
  ;; claimed-at-ms (t4-spine seam 1, ADDITIVE 2026-07-06): the request's
  ;; :claimed/at-ms — a clock the MATERIAL claims (git-spine: the committer
  ;; clock), nil when the request declares none (watcher md ingests). NEVER
  ;; read :request/time-ms for this: it wall-clock-defaults, which would stamp
  ;; arrival as a claim. completed-at-ms stays the arrival clock.
  [source-id source-ref-key source-ref source-hash document-container-id derived-unit-count
   composition-edge-count completed-at-ms completed-by-request-id event-id claimed-at-ms])

(defrecord ImportCompletionRow
  [import-key object-key source-id material-fingerprint event-id decision-id containers-count
   native-claims-count derived-units-count anchors-count edges-count projections-count
   completed-at-ms completed-by-request-id])

(defrecord ObjectContainerRow
  [container-id container-kind object-key visibility source-id source-anchor-id
   source-unit-id document-container-id current-revision-id current-content-text
   current-content-hash created-at-ms created-by event-id])

(defrecord RevisionRow
  [revision-id container-id parent-revision-id content-text content-hash order-key
   created-at-ms created-by event-id])

(defrecord DerivedUnitRow
  [unit-id document-container-id source-id unit-kind block-path parent-slot-id
   source-anchor-id derived-content-text derived-content-hash distiller-id
   distiller-version event-id])

(defrecord UnitGraduationRow
  [unit-id container-id current-revision-id current-content-text current-content-hash
   graduated-at-ms last-revised-at-ms event-id])

(defrecord SourceAnchorRow
  [source-anchor-id target-kind target-id source-id source-ref source-hash start-offset
   end-offset block-path event-id])

(defrecord CompositionEdgeRow
  [edge-id object-key document-container-id parent-slot-id child-slot-id child-order-key
   parent-target-kind parent-target-id child-target-kind child-target-id source-id
   source-anchor-id event-id])

(defrecord SourceMaterialRefRow
  [source-id object-key target-kind target-id order-key event-id])

(defrecord NativeIdentityClaimRow
  [claim-key container-id container-kind object-key source-native-id source-id source-line-key
   source-anchor-id content-hash anchor-hash material-fingerprint import-key claim-status
   event-id accepted-at-ms])

(defrecord TranscriptConversationProjectionRow
  [projection-kind conversation-container-id order-key entry-kind container-id revision-id
   source-anchor-id source-id source-ref source-line-key event-id request-id import-key
   message-uuid role content-preview parse-error-kind])

(defrecord TranscriptToolCallIndexRow
  [projection-kind tool-name order-key tool-call-container-id conversation-container-id
   message-container-id source-id source-ref source-line-key event-id request-id import-key])

(defrecord TranscriptAuditEntryRow
  [projection-kind request-id order-key entry-kind conversation-container-id container-id
   source-id source-ref source-line-key parse-error-kind event-id import-key message])

(defrecord TranscriptLastMessageRow
  [projection-kind conversation-container-id message-container-id source-line-key order-key
   event-id request-id import-key updated-at-ms])

(defrecord TranscriptSourceLineStatusRow
  [file-key order-key status import-key import-completion-key material-fingerprint
   source-file-generation-key source-line-key source-id source-ref byte-offset byte-length
   line-hash request-id parse-error-kind observed-at-ms completed-at-ms message])

(defrecord TranscriptRunRow
  [request-id request-type status source paths redaction-policy triggered-by created-at-ms
   updated-at-ms observed-line-count parse-error-count containers-created-count error progress])

(defrecord TranscriptFileOffsetRow
  [file-key file-id file-path source file-generation-key file-stat-fingerprint
   policy-version last-byte-offset observed-byte-offset line-count request-id resume-status
   repair-needed previous-file-generation-key updated-at-ms error])

(defrecord OutlineNodeRow
  [document-container-id node-slot-id block-path parent-slot-id target-kind target-id
   source-anchor-id content-text content-hash graduated container-id event-id])

(defrecord EditOrderRow
  [lineage-key edit-client-id edit-seq idempotency-key request-id revision-id event-id
   accepted-at-ms])

(defn string-present?
  "True only for a nonblank string."
  [x]
  (and (string? x) (not (str/blank? x))))

(defn append-bounded
  "Append x to an optional sequence and keep at most the newest limit values.
   Returns a vector; limit must be a nonnegative count."
  [xs x limit]
  (let [v (conj (vec (or xs [])) x)
        n (count v)]
    (if (> n limit)
      (subvec v (- n limit))
      v)))

(defn actor-row
  "Convert envelope actor keys into an ActorRow, normalizing capabilities to a set."
  [actor]
  (->ActorRow (get actor :actor/id)
              (get actor :actor/type)
              (set (get actor :actor/capabilities))))

(defn target-row
  "Convert envelope target kind, id, and address into a TargetRefRow."
  [target]
  (->TargetRefRow (get target :target/kind)
                  (get target :target/id)
                  (get target :target/address)))

(defn source-ref-key
  "Hash the source reference for the source-version index partition."
  [source-ref]
  (envelope/sha-256 source-ref))

(defn object-key-for
  "Hash source-ref, a NUL separator, and source-hash into a version-specific object key."
  [source-ref source-hash]
  (envelope/sha-256 (str source-ref object-key-separator source-hash)))

(defn source-id-for-object-key
  "Prefix an object key with src: for a source-artifact ID."
  [object-key]
  (str "src:" object-key))

(defn document-id-for-object-key
  "Prefix an object key with oc:doc: for a document container ID."
  [object-key]
  (str "oc:doc:" object-key))

(defn source-anchor-id
  "Prefix target-id with sa: for its source-anchor ID."
  [target-id]
  (str "sa:" target-id))

(defn block-container-id
  "Build an authored block container ID from object-key and unit-local-id."
  [object-key unit-local-id]
  (str "oc:block:" object-key ":" unit-local-id))

(defn composition-edge-id
  "Build a composition edge ID from object-key, parent slot, and child order key."
  [object-key parent-slot-id child-order-key]
  (str "ce:" object-key ":" parent-slot-id ":" child-order-key))

(defn revision-id-for-request
  "Use the payload revision-id when supplied, otherwise derive one from
   object-key and request-id. Does not check uniqueness against stored revisions."
  [object-key request]
  (or (get-in request [:payload :revision-id])
      (str "rev:" object-key ":" (:request/id request))))

(defn import-revision-id
  "Derive a revision ID from object-key and hashes of target-id and import-key."
  [object-key target-id import-key]
  (str "rev:" object-key ":" (envelope/sha-256 target-id) ":" (envelope/sha-256 import-key)))

(defn event-id-for-request
  "Derive the common event ID from object-key and request-id."
  [object-key request]
  (str "evt:" object-key ":" (:request/id request)))

(defn fixed-width-order-key
  "Format a zero-padded numeric time (nil becomes zero) followed by request-id
   as an ordered string key; equal times are disambiguated by the suffix."
  [time-ms request-id]
  (format "%020d:%s" (long (or time-ms 0)) request-id))

(defn audit-id
  "Combine partition-key and request-id into the request audit key."
  [partition-key request-id]
  (str partition-key "/request/" request-id))

(defn decision-id-for-audit-id
  "Append /decision to an audit-id."
  [audit-id]
  (str audit-id "/decision"))

(defn source-id-for
  "Derive the version-specific source ID from source reference and content hash."
  [source-ref source-hash]
  (source-id-for-object-key (object-key-for source-ref source-hash)))

(defn document-id-for
  "Derive the version-specific document ID from source reference and content hash."
  [source-ref source-hash]
  (document-id-for-object-key (object-key-for source-ref source-hash)))

(defn source-hash
  "Hash the UTF-8 encoding of raw-text coerced to a string; this is not a raw-byte hash."
  [raw-text]
  (envelope/sha-256 (str raw-text)))

(defn leading-object-key
  "Extract the leading object identity from an ID suffix, keeping two segments
   for chat: and fm: keys and the first segment for other prefixes."
  [s]
  (let [s (str s)]
    (if (or (str/starts-with? s "chat:")
            ;; P1 editable-material: facet-master identities are two segments,
            ;; exactly like chat identities for routing purposes.
            (str/starts-with? s "fm:"))
      (let [parts (str/split s #":" 3)]
        (if (>= (count parts) 2)
          (str (first parts) ":" (second parts))
          s))
      (let [idx (str/index-of s ":")]
        (if idx (subs s 0 idx) s)))))

(defn positive-partition
  "Map k by Clojure hash into num-partitions; return zero when the count is nonpositive."
  [num-partitions k]
  (if (pos? num-partitions)
    (mod (hash k) num-partitions)
    0))

(defn extract-object-key
  "Recover a family routing key from the recognized source/container/import/row
   ID prefixes. Unknown strings pass through unchanged. Exact prefix handling
   matters because foreign PState reads must route to the task that wrote the row;
   this parser is not general ID validation."
  [id-or-key]
  (let [s (str id-or-key)]
    (cond
      (str/starts-with? s "src:tr:")
      (leading-object-key (subs s 7))

      (str/starts-with? s "imp:tr:")
      (leading-object-key (subs s 7))

      (str/starts-with? s "imp:md:")
      (leading-object-key (subs s 7))

      ;; G-F2 (code-atom GATE_REVIEW 2026-07-09; Sid-authorized this package's 2nd and
      ;; last kernel edit): route clojure import-keys "imp:clj:<object-key>:<sha>" to the
      ;; object-key partition, exactly like imp:md:. Pre-fix they fell to :else (the whole
      ;; string) → a foreign read-import-completion mis-routed to nil on a multi-task
      ;; cluster (the topology writes the completion by (|hash *object-key)). "imp:clj:"
      ;; is 8 chars (imp:md: is 7). Block-kernel's sibling key routes via a restructured
      ;; imp:tr: prefix instead, so no imp:sense-block: branch joins this cond.
      (str/starts-with? s "imp:clj:")
      (leading-object-key (subs s 8))

      ;; W2 framework (CONTRACT §16 — the wave's ONE pre-authorized kernel
      ;; edit; ships WITH gate G18, the twice-fired foreign-read latent class:
      ;; code-atom G-F2 `imp:clj:`, block-kernel F2 `imp:sense-block:`). Route
      ;; assembly import-keys "imp:asm:<object-key>:<sha>" to the object-key
      ;; partition, like imp:md:/imp:clj: — EXCEPT the assembly object-key is
      ;; itself two segments ("asm:<name>", CONTRACT §17: identity is the worn
      ;; NAME), so this branch takes the first TWO colon segments of the
      ;; remainder instead of riding leading-object-key, which would truncate
      ;; at "asm" and mis-route the foreign read this gate exists to kill.
      ;; "imp:asm:" is 8 chars; the full key reads imp:asm:asm:<name>:<sha>.
      (str/starts-with? s "imp:asm:")
      (let [remainder (subs s 8)
            idx1 (str/index-of remainder ":")
            idx2 (when idx1 (str/index-of remainder ":" (inc idx1)))]
        (if idx2 (subs remainder 0 idx2) remainder))

      ;; P1 editable-material: imp:fm:fm:<facet>:<content-hash>. The object-key
      ;; is two segments, so take both back before hashing the foreign read.
      (str/starts-with? s "imp:fm:")
      (let [remainder (subs s 7)
            idx1 (str/index-of remainder ":")
            idx2 (when idx1 (str/index-of remainder ":" (inc idx1)))]
        (if idx2 (subs remainder 0 idx2) remainder))

      ;; first-light A (CONTRACT §5.1 — the package's ONE sanctioned routing
      ;; edit; ships with gate G18's family, the THRICE-fired foreign-read
      ;; latent class: imp:clj: G-F2, imp:sense-block: block-kernel F2,
      ;; imp:asm: framework G18). Route native-episode import-keys
      ;; "imp:ep:<object-key>:<sha>" to the object-key partition — the
      ;; episode object-key is transcript-shaped ("chat:<hex>", two colon
      ;; segments), which leading-object-key already handles. Without this
      ;; branch the :else whole-string fall-through mis-routes a foreign
      ;; read-import-completion to nil on the multi-task cluster. "imp:ep:"
      ;; is 7 chars, like imp:md:/imp:tr:.
      (str/starts-with? s "imp:ep:")
      (leading-object-key (subs s 7))

      (str/starts-with? s "src:")
      (leading-object-key (subs s 4))

      (str/starts-with? s "oc:chat-conversation:")
      (leading-object-key (subs s 21))

      (str/starts-with? s "oc:chat-message:")
      (leading-object-key (subs s 16))

      (str/starts-with? s "oc:tool-call:")
      (leading-object-key (subs s 13))

      (str/starts-with? s "oc:tool-result:")
      (leading-object-key (subs s 15))

      (str/starts-with? s "oc:chat-artifact:")
      (leading-object-key (subs s 17))

      (str/starts-with? s "oc:run:")
      (leading-object-key (subs s 7))

      (str/starts-with? s "oc:doc:")
      (subs s 7)

      (str/starts-with? s "du:")
      (leading-object-key (subs s 3))

      (str/starts-with? s "sa:")
      (leading-object-key (subs s 3))

      (str/starts-with? s "ce:")
      (leading-object-key (subs s 3))

      (str/starts-with? s "oc:block:")
      (leading-object-key (subs s 9))

      (str/starts-with? s "rev:")
      (leading-object-key (subs s 4))

      (str/starts-with? s "evt:")
      (leading-object-key (subs s 4))

      :else s)))

(defn partition-by-object-key
  "Route a stored row ID or object key through extract-object-key and the task count."
  [num-partitions id-or-key]
  (positive-partition num-partitions (extract-object-key id-or-key)))

(defn audit-partition-key
  "Extract the prefix before /request/ from an audit/decision key; otherwise keep it."
  [audit-id]
  (let [s (str audit-id)
        marker "/request/"
        idx (str/index-of s marker)]
    (if idx (subs s 0 idx) s)))

(defn partition-by-audit-id
  "Route request/decision audit IDs by their original request partition key."
  [num-partitions audit-id]
  (positive-partition num-partitions (audit-partition-key audit-id)))

(defn payload-source-ref
  "Return :source-ref from payload, or nil when absent."
  [payload] (:source-ref payload))
(defn payload-source-hash
  "Return :source-hash from payload, or nil when absent."
  [payload] (:source-hash payload))
(defn payload-source-raw-text
  "Return :source-raw-text from payload, or nil when absent."
  [payload] (:source-raw-text payload))
(defn payload-source-format
  "Return :source-format from payload, or nil when absent."
  [payload] (:source-format payload))
(defn payload-distiller-id
  "Return :distiller-id from payload, or nil when absent."
  [payload] (:distiller-id payload))
(defn payload-distiller-version
  "Return :distiller-version from payload, or nil when absent."
  [payload] (:distiller-version payload))
(defn payload-document-container-id
  "Return :document-container-id from payload, or nil when absent."
  [payload] (:document-container-id payload))
(defn payload-object-key
  "Return :object-key from payload, or nil when absent."
  [payload] (:object-key payload))
(defn payload-content-text
  "Return :content-text from payload, or nil when absent."
  [payload] (:content-text payload))
(defn payload-content-hash
  "Return :content-hash from payload, or nil when absent."
  [payload] (:content-hash payload))
(defn payload-edit-client-id
  "Return :edit-client-id from payload, or nil when absent."
  [payload] (:edit-client-id payload))
(defn payload-edit-seq
  "Return :edit-seq from payload, or nil when absent."
  [payload] (:edit-seq payload))
(defn payload-edit-lineage-key
  "Return :edit-lineage-key from payload, or nil when absent."
  [payload] (:edit-lineage-key payload))

(defn request-partition-key
  "Return :partition/key from request, or nil when absent."
  [request] (:partition/key request))
(defn request-id
  "Return :request/id from request, or nil when absent."
  [request] (:request/id request))
(defn request-type
  "Return :request/type from request, or nil when absent."
  [request] (:request/type request))
(defn request-payload
  "Return :payload from request, or nil when absent."
  [request] (:payload request))
(defn request-idempotency-key
  "Return :idempotency/key from request, or nil when absent."
  [request] (:idempotency/key request))
(defn request-audit-id
  "Derive the request audit key from its :partition/key and :request/id."
  [request] (audit-id (request-partition-key request) (request-id request)))
(defn request-object-key
  "Read :object-key from the request payload."
  [request] (payload-object-key (request-payload request)))
(defn request-source-ref-key
  "Return the request partition key used by the source-ref request path."
  [request] (request-partition-key request))

(declare request-import-key
         request-material-fingerprint
         payload-source-artifacts
         payload-object-containers
         payload-derived-units
         payload-source-anchors
         payload-composition-edges
         payload-source-line-statuses)

(defn request-row
  "Construct an audit row preserving the original request plus normalized actor and target."
  [request]
  (let [audit (request-audit-id request)]
    (->ObjectContainerRequestRow audit
                                 (request-partition-key request)
                                 (request-id request)
                                 (request-type request)
                                 (:routing/key request)
                                 (request-idempotency-key request)
                                 (actor-row (:actor request))
                                 (target-row (:target request))
                                 (request-payload request)
                                 (:request/time-ms request)
                                 request)))

(defn event-row
  "Construct a common event row with the supplied identity, payload, time, and request custody."
  [event-id event-type object-key target-kind target-id actor payload time-ms request]
  (->ObjectContainerEventRow event-id
                             event-type
                             object-key
                             target-kind
                             target-id
                             (actor-row actor)
                             payload
                             time-ms
                             (request-id request)
                             (request-audit-id request)))

(defn accepted-decision-row
  "Build an accepted audit decision referencing event and import/fingerprint keys.
   Stamps decided-at-ms with the current wall clock; does not itself write state."
  [request event]
  (let [audit (request-audit-id request)]
    (->ObjectContainerDecisionRow (decision-id-for-audit-id audit)
                                  audit
                                  (request-partition-key request)
                                  (request-id request)
                                  (request-type request)
                                  (request-idempotency-key request)
                                  :accepted
                                  nil
                                  []
                                  (:event-id event)
                                  event
                                  (request-material-fingerprint request)
                                  (request-import-key request)
                                  nil
                                  (envelope/now-ms)
                                  nil)))

(defn rejected-decision-row
  "Build a rejected audit decision with reason/errors, no event, and a current
   wall-clock decision timestamp. Does not itself write state."
  [request reason errors]
  (let [audit (request-audit-id request)]
    (->ObjectContainerDecisionRow (decision-id-for-audit-id audit)
                                  audit
                                  (request-partition-key request)
                                  (request-id request)
                                  (request-type request)
                                  (request-idempotency-key request)
                                  :rejected
                                  reason
                                  (vec errors)
                                  nil
                                  nil
                                  (request-material-fingerprint request)
                                  (request-import-key request)
                                  nil
                                  (envelope/now-ms)
                                  nil)))

(defn conflict-decision-row
  "Build a rejection that points to the prior decision with which the request conflicts."
  [request reason errors prior-decision]
  (assoc (rejected-decision-row request reason errors)
         :conflict-with-decision-id (:decision-id prior-decision)))

(defn replay-decision-row
  "Copy a prior decision to the incoming audit/request identity and stamp the replay
   origin and current decision time. The original event remains referenced."
  [request prior-decision]
  (let [audit (request-audit-id request)]
    (assoc prior-decision
           :decision-id (decision-id-for-audit-id audit)
           :audit-id audit
           :partition-key (request-partition-key request)
           :request-id (request-id request)
           :request-type (request-type request)
           :idempotency-key (request-idempotency-key request)
           :material-fingerprint (request-material-fingerprint request)
           :decided-at-ms (envelope/now-ms)
           :replayed-from-decision-id (:decision-id prior-decision))))

(defn decision-accepted?
  "True when a common decision row has :status :accepted."
  [decision]
  (= :accepted (:status decision)))

(defn decision-event-id
  "Return :event-id from decision, or nil when absent."
  [decision]
  (:event-id decision))

(defn decision-material-fingerprint
  "Return :material-fingerprint from decision, or nil when absent."
  [decision]
  (:material-fingerprint decision))

(defn material-fingerprint-conflict?
  "True when a prior decision exists and its material fingerprint differs from the request."
  [request prior-decision]
  (and (some? prior-decision)
       (not= (request-material-fingerprint request)
             (decision-material-fingerprint prior-decision))))

(defn material-fingerprint-conflict-error
  "Describe an idempotency-key fingerprint conflict, including prior decision identity."
  [request prior-decision]
  {:type :idempotency/material-fingerprint-conflict
   :idempotency-key (request-idempotency-key request)
   :expected (decision-material-fingerprint prior-decision)
   :actual (request-material-fingerprint request)
   :conflict-with-decision-id (:decision-id prior-decision)})

(defn completion-material-fingerprint
  "Return :material-fingerprint from completion, or nil when absent."
  [completion]
  (:material-fingerprint completion))

(defn completion-event-id
  "Return :event-id from completion, or nil when absent."
  [completion]
  (:event-id completion))

(defn import-material-fingerprint-conflict-error
  "Describe reuse of an import-key with a different fingerprint from its completion."
  [request completion]
  {:type :import/material-fingerprint-conflict
   :import-key (request-import-key request)
   :expected (completion-material-fingerprint completion)
   :actual (request-material-fingerprint request)
   :conflict-with-event-id (completion-event-id completion)})

(defn source-request-valid?
  "True when the validation error collection is empty."
  [errors]
  (empty? errors))

(def edit-target-kinds
  #{:derived-unit :object-container})

(defn edit-request-validation-errors
  "Return envelope, edit-target, partition/object-key, content-hash, client/sequence,
   and capability errors. Stored-target existence and stale ordering are checked
   separately by edit-effects; this validator does not read PStates."
  [request]
  (let [payload (request-payload request)
        content (str (payload-content-text payload))
        expected-hash (source-hash content)
        target-kind (get-in request [:target :target/kind])
        target-id (get-in request [:target :target/id])]
    (cond-> (vec (envelope/request-validation-errors request))
      (not= :object/edit (request-type request))
      (conj {:type :request/type-invalid :value (request-type request)})

      (not (contains? edit-target-kinds target-kind))
      (conj {:type :target/kind-invalid :value target-kind})

      (not (string-present? target-id))
      (conj {:type :target/id-invalid :value target-id})

      (not (string-present? (request-partition-key request)))
      (conj {:type :partition/key-invalid :value (request-partition-key request)})

      (not (string-present? (request-idempotency-key request)))
      (conj {:type :idempotency/key-invalid :value (request-idempotency-key request)})

      (not (string-present? (payload-document-container-id payload)))
      (conj {:type :document/container-id-invalid
             :value (payload-document-container-id payload)})

      (not (string-present? (payload-object-key payload)))
      (conj {:type :object/key-invalid :value (payload-object-key payload)})

      (not= (payload-object-key payload) (request-partition-key request))
      (conj {:type :partition/object-key-mismatch})

      (not (string-present? (payload-content-hash payload)))
      (conj {:type :content/hash-invalid :value (payload-content-hash payload)})

      (not= expected-hash (payload-content-hash payload))
      (conj {:type :content/hash-mismatch
             :expected expected-hash
             :actual (payload-content-hash payload)})

      (not (string-present? (payload-edit-client-id payload)))
      (conj {:type :edit/client-id-invalid :value (payload-edit-client-id payload)})

      (not (integer? (payload-edit-seq payload)))
      (conj {:type :edit/seq-invalid :value (payload-edit-seq payload)})

      (not (envelope/authorized-request? request))
      (conj {:type :actor-not-authorized}))))

(defn request-import-key
  "Return :import/key from request, or nil when absent."
  [request] (:import/key request))
(defn request-material-fingerprint
  "Return :material/fingerprint from request, or nil when absent."
  [request] (:material/fingerprint request))

(defn import-completion-row
  "Build completion metadata/counts from the request bundle and accepted event/decision.
   Uses the first source row for source-id and the container count for native claims;
   stamps completion time now. The topology is responsible for writing the rows."
  [request event decision]
  (let [payload (request-payload request)
        source-rows (payload-source-artifacts payload)
        container-rows (payload-object-containers payload)
        derived-unit-rows (payload-derived-units payload)
        anchor-rows (payload-source-anchors payload)
        edge-rows (payload-composition-edges payload)
        projection-hints (vec (:projection-hints payload))]
    (->ImportCompletionRow (request-import-key request)
                           (request-object-key request)
                           (:source-id (first source-rows))
                           (request-material-fingerprint request)
                           (:event-id event)
                           (:decision-id decision)
                           (count container-rows)
                           (count container-rows)
                           (count derived-unit-rows)
                           (count anchor-rows)
                           (count edge-rows)
                           (count projection-hints)
	                           (envelope/now-ms)
	                           (request-id request))))

(defn source-ingest-completion-row
  "Build a source-version completion row with current arrival time and the optional
   :claimed/at-ms copied from the request; arrival is never substituted for a claim."
  [request source-version-row event]
  (let [payload (request-payload request)]
    (->SourceIngestCompletionRow (:source-id source-version-row)
                                (:source-ref-key source-version-row)
                                (:source-ref source-version-row)
                                (:source-hash source-version-row)
                                (:document-container-id source-version-row)
                                (count (payload-derived-units payload))
                                (count (payload-composition-edges payload))
                                (envelope/now-ms)
                                (request-id request)
                                (:event-id event)
                                ;; material-claimed clock, nil-honest (see record)
                                (:claimed/at-ms request))))

(defn payload-source-artifacts
  "Return :source-artifacts from payload as a vector; missing values become []."
  [payload] (vec (:source-artifacts payload)))
(defn payload-object-containers
  "Return :object-containers from payload as a vector; missing values become []."
  [payload] (vec (:object-containers payload)))
(defn payload-revisions
  "Return :revisions from payload as a vector; missing values become []."
  [payload] (vec (:revisions payload)))
(defn payload-derived-units
  "Return :derived-units from payload as a vector; missing values become []."
  [payload] (vec (:derived-units payload)))
(defn payload-source-anchors
  "Return :source-anchors from payload as a vector; missing values become []."
  [payload] (vec (:source-anchors payload)))
(defn payload-composition-edges
  "Return :composition-edges from payload as a vector; missing values become []."
  [payload] (vec (:composition-edges payload)))
(defn payload-source-versions
  "Return :source-versions from payload as a vector; missing values become []."
  [payload] (vec (:source-versions payload)))
(defn payload-projection-hints
  "Return :projection-hints from payload as a vector; missing values become []."
  [payload] (vec (:projection-hints payload)))
(defn payload-source-line-statuses
  "Return :source-line-statuses from payload as a vector; missing values become []."
  [payload] (vec (:source-line-statuses payload)))

(defn projection-hint-fingerprint
  "Select the projection fields included in import fingerprinting, omitting volatile metadata."
  [hint]
  (select-keys hint
               [:projection-kind
                :document-container-id
                :node-slot-id
                :block-path
                :parent-slot-id
                :target-kind
                :target-id
                :source-anchor-id
                :content-hash
                :graduated
                :container-id
                :conversation-container-id
                :order-key
                :entry-kind
                :revision-id
                :source-id
	                :source-ref
	                :source-line-key
	                :import-key
	                :message-uuid
                :role
                :content-preview
                :parse-error-kind
                :tool-name
                :tool-call-container-id
                :message-container-id
                :message]))

(defn source-line-status-fingerprint
  "Select source-line identity, generation, material, and byte fields for fingerprinting."
  [row]
  (select-keys row
               [:file-key
                :order-key
                :status
                :import-key
                :material-fingerprint
                :source-file-generation-key
                :source-line-key
                :source-id
                :source-ref
                :byte-offset
                :byte-length
	                :line-hash
	                :parse-error-kind]))

(defn native-claim-signature
  "Describe a candidate container identity claim from its row and import request.
   The source-native-id is currently the container ID, and source-line-key is import-key."
  [request container-row]
  {:claim-key (:container-id container-row)
   :container-id (:container-id container-row)
   :container-kind (:container-kind container-row)
   :object-key (:object-key container-row)
   :source-native-id (:container-id container-row)
   :source-id (:source-id container-row)
   :source-line-key (request-import-key request)
   :source-anchor-id (:source-anchor-id container-row)
   :content-hash (:current-content-hash container-row)
   :anchor-hash (:source-anchor-id container-row)
   :material-fingerprint (request-material-fingerprint request)
   :import-key (request-import-key request)})

(defn duplicate-native-claim-conflicts
  "Return conflicts for repeated container IDs with differing candidate signatures
   within one bundle; identical repeated signatures produce no conflict."
  [request container-rows]
  (->> container-rows
       (group-by :container-id)
       vals
       (keep (fn [rows]
               (when (> (count rows) 1)
                 (let [signatures (mapv #(native-claim-signature request %) rows)
                       distinct-signatures (vec (distinct signatures))]
                   (when (> (count distinct-signatures) 1)
                     {:container-id (:container-id (first rows))
                      :claim-count (count rows)
                      :distinct-claim-count (count distinct-signatures)})))))
       vec))

(defn import-request-validation-errors
  "Return structural/envelope, capability, source-hash, revision-reference, native
   duplicate, and anchor-reference errors for a prepared import bundle. Projection-only
   bundles are allowed. Does not compare against stored native claims or recompute
   the supplied material fingerprint; those limits matter for foreign appenders."
  [request]
  (let [payload (request-payload request)
        source-rows (payload-source-artifacts payload)
        container-rows (payload-object-containers payload)
        revision-rows (payload-revisions payload)
        derived-unit-rows (payload-derived-units payload)
        anchor-rows (payload-source-anchors payload)
        projection-hint-rows (payload-projection-hints payload)
        source-ids (set (map :source-id source-rows))
        container-ids (set (map :container-id container-rows))
        derived-unit-ids (set (map :unit-id derived-unit-rows))
        target-ids (set/union container-ids derived-unit-ids)
        anchor-target-ids (set (map :target-id anchor-rows))
	        anchor-source-ids (set (map :source-id anchor-rows))
	        revision-ids (set (map :revision-id revision-rows))
	        primary-source-row (first source-rows)
	        missing-anchors (seq (set/difference target-ids anchor-target-ids))
	        missing-anchor-sources (seq (set/difference anchor-source-ids source-ids))
	        missing-anchor-targets (seq (set/difference anchor-target-ids target-ids))
	        payload-source-hash-mismatch?
	        (and (= :markdown (:source-format primary-source-row))
	             (contains? payload :source-hash)
	             (not= (:source-hash payload) (:source-hash primary-source-row)))
	        source-hash-mismatches
	        (seq
	         (keep (fn [source-row]
	                 (when (and (= :markdown (:source-format source-row))
	                            (not= (:source-hash source-row)
	                                  (source-hash (:source-raw-text source-row))))
	                   {:type :source/hash-mismatch
	                    :source-id (:source-id source-row)
	                    :expected (source-hash (:source-raw-text source-row))
	                    :actual (:source-hash source-row)}))
	               source-rows))
	        duplicate-native-conflicts (duplicate-native-claim-conflicts request container-rows)
	        missing-current-revisions (seq (remove #(contains? revision-ids (:current-revision-id %))
	                                               container-rows))]
    (cond-> (vec (envelope/request-validation-errors request))
      (not= :object-container/import-material (request-type request))
      (conj {:type :request/type-invalid :value (request-type request)})

      (not (string-present? (request-partition-key request)))
      (conj {:type :partition/key-invalid :value (request-partition-key request)})

      (not (string-present? (request-import-key request)))
      (conj {:type :import/key-invalid :value (request-import-key request)})

      (not (string-present? (request-object-key request)))
      (conj {:type :object/key-invalid :value (request-object-key request)})

      (not (string-present? (request-idempotency-key request)))
      (conj {:type :idempotency/key-invalid :value (request-idempotency-key request)})

      (not (string-present? (request-material-fingerprint request)))
      (conj {:type :material/fingerprint-invalid
             :value (request-material-fingerprint request)})

      (not (envelope/authorized-request? request))
      (conj {:type :actor-not-authorized})

	      ;; F3 (Sid ruled A, 2026-07-10): accept a projection-hint-ONLY import
	      ;; (durable debris class row, no surface) — reject only a truly empty
	      ;; payload. Purely additive: any import WITH sources is unaffected.
	      (and (empty? source-rows) (empty? projection-hint-rows))
	      (conj {:type :source-artifacts/missing})

	      source-hash-mismatches
	      (conj {:type :source/hash-mismatch
	             :mismatches (vec source-hash-mismatches)})

	      payload-source-hash-mismatch?
	      (conj {:type :source/hash-mismatch
	             :expected (:source-hash primary-source-row)
	             :actual (:source-hash payload)})

	      missing-current-revisions
      (conj {:type :containers/current-revision-missing
             :container-ids (mapv :container-id missing-current-revisions)})

      (seq duplicate-native-conflicts)
      (conj {:type :native-identity/duplicate-conflicting-candidates
             :conflicts duplicate-native-conflicts})

      missing-anchors
      (conj {:type :source-anchors/missing-for-targets
             :target-ids (vec missing-anchors)})

      missing-anchor-sources
      (conj {:type :source-anchors/source-missing
             :source-ids (vec missing-anchor-sources)})

      missing-anchor-targets
      (conj {:type :source-anchors/target-missing
             :target-ids (vec missing-anchor-targets)}))))

(defn import-event-row
  "Build the imported-source-record event from request identity, payload, actor, and request time."
  [request]
  (let [object-key (request-object-key request)
        event-id (event-id-for-request object-key request)]
    (event-row event-id
               :object-container/imported-source-record
               object-key
               :object-container-import
               (request-import-key request)
               (:actor request)
               (request-payload request)
               (:request/time-ms request)
               request)))

(defn complete-transcript-source-line-status-row
  "Mark a line import-complete or parse-error-complete using an import completion
   fingerprint/key/time. Returns a row for the separate completion-depot write."
  [row completion]
  (assoc row
         :status (if (:parse-error-kind row) :parse-error-complete :import-complete)
         :import-completion-key (:import-key completion)
         :material-fingerprint (:material-fingerprint completion)
         :completed-at-ms (:completed-at-ms completion)))

(defn transcript-control-request-type
  "Prefer :request/type, falling back to :claim/type for control records."
  [request]
  (or (:request/type request) (:claim/type request)))

(defn transcript-control-request-id
  "Return :transcript/request-id from request, or nil when absent."
  [request]
  (:transcript/request-id request))

(defn transcript-control-validation-errors
  "Validate control type and request ID plus harvest/watch source, policy, and paths.
   Run-status requests require a nonnil status; this does not execute acquisition."
  [request]
  (let [request-type (transcript-control-request-type request)]
    (cond-> []
      (not (map? request))
      (conj {:type :request/not-map})

      (and (map? request)
           (not (contains? transcript-control-request-types request-type)))
      (conj {:type :request/type-invalid :value request-type})

      (and (map? request)
           (not (string-present? (transcript-control-request-id request))))
      (conj {:type :transcript/request-id-invalid
             :value (transcript-control-request-id request)})

      (and (map? request)
           (#{:transcript/harvest :transcript/watch} request-type)
	           (not (contains? transcript-sources (:transcript/source request))))
      (conj {:type :transcript/source-invalid :value (:transcript/source request)})

      (and (map? request)
           (#{:transcript/harvest :transcript/watch} request-type)
	           (not (contains? transcript-redaction-policies
                           (:transcript/redaction-policy request))))
      (conj {:type :transcript/redaction-policy-invalid
             :value (:transcript/redaction-policy request)})

      (and (map? request)
           (#{:transcript/harvest :transcript/watch} request-type)
           (not (sequential? (:transcript/paths request))))
      (conj {:type :transcript/paths-invalid :value (:transcript/paths request)})

      (and (map? request)
           (= :transcript/run-status request-type)
           (nil? (:status request)))
      (conj {:type :transcript/status-invalid :value (:status request)}))))

(defn transcript-run-progress
  "Append an optional progress item, keeping the newest 50; otherwise retain prior progress."
  [existing progress]
  (if progress
    (append-bounded (:progress existing) progress 50)
    (:progress existing)))

(defn terminal-transcript-run?
  "True for complete, failed, or cancelled operational run rows."
  [run-row]
  (contains? terminal-transcript-run-statuses (:status run-row)))

(defn transcript-initial-run-row
  "Build an accepted-running row with zero counts and request time, falling back
   to the current clock. This constructor does not guard an existing run."
  [request]
  (let [now (long (or (:request/time-ms request) (:time-ms request) (envelope/now-ms)))]
    (->TranscriptRunRow (transcript-control-request-id request)
                        (transcript-control-request-type request)
                        :accepted-running
                        (:transcript/source request)
                        (vec (:transcript/paths request))
                        (:transcript/redaction-policy request)
                        (:transcript/triggered-by request)
                        now
                        now
                        0
                        0
                        0
                        nil
                        [])))

(defn transcript-rejected-run-row
  "Build a failed operational run row carrying validation errors and request metadata."
  [request errors]
  (let [now (long (or (:request/time-ms request) (:time-ms request) (envelope/now-ms)))]
    (->TranscriptRunRow (transcript-control-request-id request)
                        (transcript-control-request-type request)
                        :failed
                        (:transcript/source request)
                        (vec (:transcript/paths request))
                        (:transcript/redaction-policy request)
                        (:transcript/triggered-by request)
                        now
                        now
                        0
                        0
                        0
                        {:errors (vec errors)}
                        [])))

(defn transcript-run-status-row
  "Keep a terminal existing run unchanged; otherwise apply supplied status, counts,
   progress, and error. Creates a status-only base row if the run is absent.
   This terminal guard applies to status updates, not repeated harvest/watch controls."
  [existing request]
  (if (terminal-transcript-run? existing)
    existing
    (let [now (long (or (:time-ms request) (:request/time-ms request) (envelope/now-ms)))
          counts (:counts request)
          base (or existing
                   (->TranscriptRunRow (transcript-control-request-id request)
                                       :transcript/run-status
                                       nil
                                       nil
                                       []
                                       nil
                                       nil
                                       now
                                       now
                                       0
                                       0
                                       0
                                       nil
                                       []))]
      (-> base
          (assoc :status (:status request)
                 :updated-at-ms now
                 :progress (transcript-run-progress base (:progress request)))
          (cond-> (:error request) (assoc :error (:error request))
                  (:observed-line-count counts)
                  (assoc :observed-line-count (:observed-line-count counts))
                  (:parse-error-count counts)
                  (assoc :parse-error-count (:parse-error-count counts))
                  (:containers-created-count counts)
                  (assoc :containers-created-count (:containers-created-count counts)))))))

(defn transcript-file-state-file-key
  "Return :source/file-key from file-state, or nil when absent."
  [file-state]
  (:source/file-key file-state))

(defn transcript-file-state-source-lines
  "Return the first supplied source-line collection under the supported aliases as a vector."
  [file-state]
  (vec (or (:source/lines file-state)
           (:source/source-lines file-state)
           (:transcript/source-lines file-state)
           [])))

(defn transcript-file-state-validation-errors
  "Validate file-state map shape, optional request type, and nonblank file key."
  [file-state]
  (cond-> []
    (not (map? file-state))
    (conj {:type :file-state/not-map})

    (and (map? file-state)
         (:request/type file-state)
         (not= transcript-file-state-request-type (:request/type file-state)))
    (conj {:type :request/type-invalid :value (:request/type file-state)})

    (and (map? file-state)
         (not (string-present? (transcript-file-state-file-key file-state))))
    (conj {:type :source/file-key-invalid
           :value (transcript-file-state-file-key file-state)})))

(defn transcript-file-state-stale?
  "Detect explicit repair flags, generation/file/stat/policy changes, or shrinkage
   below the saved offset. Compares supplied metadata; never stats the file itself."
  [existing file-state]
  (let [new-generation (transcript-identity/transcript-file-generation-key file-state)
        old-generation (:file-generation-key existing)
        current-length (:source/current-byte-length file-state)
        saved-offset (:last-byte-offset existing)]
    (boolean
     (or (:repair-needed file-state)
         (:source/repair-needed file-state)
         (and existing
              new-generation
              old-generation
              (not= new-generation old-generation))
         (and existing
              (:source/file-id file-state)
              (not= (:source/file-id file-state) (:file-id existing)))
         (and existing
              (:source/file-stat-fingerprint file-state)
              (not= (:source/file-stat-fingerprint file-state)
                    (:file-stat-fingerprint existing)))
         (and existing
              (:source/policy-version file-state)
              (not= (:source/policy-version file-state) (:policy-version existing)))
         (and current-length
              saved-offset
              (< (long current-length) (long saved-offset)))))))

(defn transcript-source-line-range-cursor
  "Format an offset as a zero-padded lower-bound cursor for source-line order keys."
  [offset]
  (format "%020d" (long (or offset 0))))

(defn transcript-file-state-advance-limit
  "Read the explicit source/transcript advance limit or the default as a long."
  [file-state]
  (long (or (:source/advance-limit file-state)
            (:transcript/advance-limit file-state)
            default-transcript-offset-advance-limit)))

(defn transcript-file-offset-last-byte-offset
  "Return :last-byte-offset from row, or nil when absent."
  [row]
  (:last-byte-offset row))

(defn transcript-file-offset-row
  "Build observed/resume state from a file observation and prior offset. Stale files
   reset the safe offset; pending observations retain prior progress. With no pending
   work it uses the requested offset. Completion matching happens in the next fold."
  [file-state existing errors]
  (let [now (long (or (:time-ms file-state) (:request/time-ms file-state) (envelope/now-ms)))
        rejected? (seq errors)
        stale? (and (not rejected?) (transcript-file-state-stale? existing file-state))
        requested-last-offset (long (or (:source/last-byte-offset file-state) 0))
        observed-offset (long (or (:source/observed-byte-offset file-state)
                                  requested-last-offset))
        prior-safe-offset (long (or (:last-byte-offset existing) 0))
        pending-observed-lines? (or (seq (transcript-file-state-source-lines file-state))
                                    (> observed-offset prior-safe-offset))
        safe-last-offset (cond
                           stale? 0
                           pending-observed-lines? prior-safe-offset
                           :else requested-last-offset)
        resume-status (cond
                        rejected? :rejected
                        stale? (or (:resume/status file-state)
                                   :stale-after-file-change)
                        pending-observed-lines? :observed-pending
                        :else (or (:resume/status file-state) :safe))]
    (->TranscriptFileOffsetRow (transcript-file-state-file-key file-state)
	                               (:source/file-id file-state)
	                               (:source/file-path file-state)
	                               (:transcript/source file-state)
	                               (transcript-identity/transcript-file-generation-key file-state)
	                               (:source/file-stat-fingerprint file-state)
                               (:source/policy-version file-state)
                               safe-last-offset
                               observed-offset
                               (long (or (:source/line-count file-state) 0))
                               (or (:transcript/ingest-request-id file-state)
                                   (:transcript/request-id file-state)
                                   (:request/id file-state))
                               resume-status
                               (boolean (or rejected? stale? pending-observed-lines?))
                               (or (:previous/file-generation-key file-state)
                                   (:file-generation-key existing))
                               now
                               (when rejected? {:errors (vec errors)}))))

(defn transcript-source-line-page-values
  "Normalize nil, an indexed page map, a sequence, or one line row into a vector."
  [page]
  (cond
    (nil? page) []
    (and (map? page) (not (contains? page :file-key))) (vec (vals page))
    (sequential? page) (vec page)
    :else [page]))

(defn transcript-source-line-end-offset
  "Add byte-offset and byte-length, treating missing values as zero."
  [row]
  (+ (long (or (:byte-offset row) 0))
     (long (or (:byte-length row) 0))))

(defn transcript-source-line-completion-by-order
  "Index normalized completion rows by order-key; later duplicate keys replace earlier ones."
  [completed-rows]
  (into {}
        (map (fn [row] [(:order-key row) row]))
        (transcript-source-line-page-values completed-rows)))

(defn transcript-source-line-completion-match?
  "Require a terminal completion with matching file/order/import/fingerprint,
   generation, line identity, byte span, and line hash for the observed row."
  [observed completed]
  (and (some? observed)
       (some? completed)
       (contains? transcript-identity/transcript-source-line-complete-statuses
                  (:status completed))
       (string-present? (:import-key observed))
       (string-present? (:material-fingerprint observed))
       (= (:file-key observed) (:file-key completed))
       (= (:order-key observed) (:order-key completed))
       (= (:import-key observed) (:import-key completed))
       (= (:material-fingerprint observed) (:material-fingerprint completed))
       (= (:source-file-generation-key observed)
          (:source-file-generation-key completed))
       (= (:source-line-key observed) (:source-line-key completed))
       (= (long (or (:byte-offset observed) 0))
          (long (or (:byte-offset completed) 0)))
       (= (long (or (:byte-length observed) 0))
          (long (or (:byte-length completed) 0)))
       (= (:line-hash observed) (:line-hash completed))))

(defn transcript-advance-file-offset-row
  "Advance through a contiguous prefix of observed lines whose common-module
   completions match. Stop at gaps or pending lines; preserve rejected/stale rows.
   Returns safe or observed-pending resume state without reading or writing PStates."
  [file-offset-row observed-rows completed-rows]
  (if (or (:error file-offset-row)
          (= :rejected (:resume-status file-offset-row))
          (= :stale-after-file-change (:resume-status file-offset-row)))
    file-offset-row
    (let [observed-target (long (or (:observed-byte-offset file-offset-row) 0))
          initial-safe-offset (long (or (:last-byte-offset file-offset-row) 0))
          completed-by-order (transcript-source-line-completion-by-order completed-rows)
          relevant-observed-rows (->> (transcript-source-line-page-values observed-rows)
                                      (filter #(and (:order-key %)
                                                    (< (long (or (:byte-offset %) 0))
                                                       observed-target)))
                                      (sort-by :order-key))
          [safe-offset stopped-pending?]
          (loop [current-safe-offset initial-safe-offset
                 remaining relevant-observed-rows]
            (if (empty? remaining)
              [current-safe-offset false]
              (let [observed-row (first remaining)
                    start-offset (long (or (:byte-offset observed-row) 0))
                    end-offset (transcript-source-line-end-offset observed-row)
                    completed-row (get completed-by-order (:order-key observed-row))]
                (cond
                  (<= end-offset current-safe-offset)
                  (recur current-safe-offset (rest remaining))

                  (> start-offset current-safe-offset)
                  [current-safe-offset true]

                  (transcript-source-line-completion-match? observed-row completed-row)
                  (recur (max current-safe-offset end-offset) (rest remaining))

                  :else
                  [current-safe-offset true]))))
          pending? (or stopped-pending? (< safe-offset observed-target))]
      (assoc file-offset-row
             :last-byte-offset safe-offset
             :resume-status (if pending? :observed-pending :safe)
             :repair-needed (boolean pending?)))))

(defn transcript-observed-source-line-status-row
  "Build an observed source-line row from file/line metadata, deriving missing
   order and line keys and retaining import identity for later completion matching."
  [file-state line]
  (let [file-key (transcript-file-state-file-key file-state)
        offset (long (or (:source/byte-offset line) 0))
        byte-length (long (or (:source/byte-length line) 0))
        line-hash (:source/line-hash line)
        order-key (or (:source-line/order-key line)
                      (:order-key line)
                      (format "%020d:%s" offset (envelope/sha-256 (str line-hash))))
        source-line-key (or (:source-line/key line)
                            (:source-line-key line)
                            (:source/line-key line)
                            (str file-key ":" offset ":" line-hash))
        now (long (or (:time-ms line)
                      (:time-ms file-state)
                      (:request/time-ms file-state)
                      (envelope/now-ms)))]
    (->TranscriptSourceLineStatusRow file-key
                                     order-key
                                     (or (:status line) :observed)
                                     (:import/key line)
	                                     (:import/completion-key line)
	                                     (:material/fingerprint line)
	                                     (or (:source/file-generation-key line)
	                                         (transcript-identity/transcript-file-generation-key
	                                          file-state))
	                                     source-line-key
                                     (:source-id line)
                                     (:source-ref line)
                                     offset
                                     byte-length
                                     line-hash
                                     (or (:transcript/ingest-request-id line)
                                         (:transcript/ingest-request-id file-state)
                                         (:transcript/request-id file-state))
                                     (:transcript/parse-error-kind line)
                                     now
                                     (:completed-at-ms line)
                                     (:message line))))

(defn row-source-id
  "Return :source-id from row, or nil when absent."
  [row] (:source-id row))
(defn row-source-ref-key
  "Return :source-ref-key from row, or nil when absent."
  [row] (:source-ref-key row))
(defn row-source-hash
  "Return :source-hash from row, or nil when absent."
  [row] (:source-hash row))
(defn row-document-container-id
  "Return :document-container-id from row, or nil when absent."
  [row] (:document-container-id row))
(defn row-container-id
  "Return :container-id from row, or nil when absent."
  [row] (:container-id row))
(defn row-container-kind
  "Return :container-kind from row, or nil when absent."
  [row] (:container-kind row))
(defn row-current-revision-id
  "Return :current-revision-id from row, or nil when absent."
  [row] (:current-revision-id row))
(defn row-current-content-text
  "Return :current-content-text from row, or nil when absent."
  [row] (:current-content-text row))
(defn row-current-content-hash
  "Return :current-content-hash from row, or nil when absent."
  [row] (:current-content-hash row))
(defn row-source-unit-id
  "Return :source-unit-id from row, or nil when absent."
  [row] (:source-unit-id row))
(defn row-revision-id
  "Return :revision-id from row, or nil when absent."
  [row] (:revision-id row))
(defn row-order-key
  "Return :order-key from row, or nil when absent."
  [row] (:order-key row))
(defn row-unit-id
  "Return :unit-id from row, or nil when absent."
  [row] (:unit-id row))
(defn row-source-anchor-id
  "Return :source-anchor-id from row, or nil when absent."
  [row] (:source-anchor-id row))
(defn row-block-path
  "Return :block-path from row, or nil when absent."
  [row] (:block-path row))
(defn row-parent-slot-id
  "Return :parent-slot-id from row, or nil when absent."
  [row] (:parent-slot-id row))
(defn row-target-id
  "Return :target-id from row, or nil when absent."
  [row] (:target-id row))
(defn row-edge-id
  "Return :edge-id from row, or nil when absent."
  [row] (:edge-id row))
(defn row-child-slot-id
  "Return :child-slot-id from row, or nil when absent."
  [row] (:child-slot-id row))
(defn row-child-order-key
  "Return :child-order-key from row, or nil when absent."
  [row] (:child-order-key row))
(defn row-parent-slot-id*
  "Return :parent-slot-id from row, or nil when absent."
  [row] (:parent-slot-id row))
(defn row-file-key
  "Return :file-key from row, or nil when absent."
  [row] (:file-key row))
(defn projection-kind
  "Return :projection-kind from row, or nil when absent."
  [row] (:projection-kind row))
(defn projection-conversation-container-id
  "Return :conversation-container-id from row, or nil when absent."
  [row] (:conversation-container-id row))
(defn projection-tool-name
  "Return :tool-name from row, or nil when absent."
  [row] (:tool-name row))
(defn projection-request-id
  "Return :request-id from row, or nil when absent."
  [row] (:request-id row))

(defn outline-projection-row
  "Convert a projection hint into the stored outline row shape."
  [row]
  (->OutlineNodeRow (:document-container-id row)
                    (:node-slot-id row)
                    (:block-path row)
                    (:parent-slot-id row)
                    (:target-kind row)
                    (:target-id row)
                    (:source-anchor-id row)
                    (:content-text row)
                    (:content-hash row)
                    (:graduated row)
                    (:container-id row)
                    (:event-id row)))
(defn row-source-anchor-id*
  "Return :source-anchor-id from row, or nil when absent."
  [row] (:source-anchor-id row))
(defn row-lineage-key
  "Return :lineage-key from row, or nil when absent."
  [row] (:lineage-key row))
(defn row-edit-client-id
  "Return :edit-client-id from row, or nil when absent."
  [row] (:edit-client-id row))
(defn row-edit-seq
  "Return :edit-seq from row, or nil when absent."
  [row] (:edit-seq row))
(defn row-event-id
  "Return :event-id from row, or nil when absent."
  [row] (:event-id row))
(defn edge-parent-slot-id
  "Return :parent-slot-id from row, or nil when absent."
  [row] (:parent-slot-id row))
(defn edge-child-order-key
  "Return :child-order-key from row, or nil when absent."
  [row] (:child-order-key row))

(defn source-material-ref-row
  "Construct a source-index reference to material, retaining ordering and event custody."
  [source-id object-key target-kind target-id order-key event-id]
  (->SourceMaterialRefRow source-id object-key target-kind target-id order-key event-id))

(defn source-material-ref-key
  "Combine an optional order-key and target-id for a source material index entry."
  [order-key target-id]
  (str (or order-key "") ":" target-id))

(defn composition-parent-ref-key
  "Combine parent-slot-id and edge-id for the child-to-parent reverse index."
  [edge-row]
  (str (:parent-slot-id edge-row) ":" (:edge-id edge-row)))

(defn native-identity-claim-row
  "Construct an accepted native claim for a candidate container, with import identity
   and current acceptance time. The caller must first check compatibility."
  [request container-row]
  (->NativeIdentityClaimRow (:container-id container-row)
                            (:container-id container-row)
                            (:container-kind container-row)
                            (:object-key container-row)
                            (:container-id container-row)
                            (:source-id container-row)
                            (request-import-key request)
                            (:source-anchor-id container-row)
                            (:current-content-hash container-row)
                            (:source-anchor-id container-row)
                            (request-material-fingerprint request)
                            (request-import-key request)
                            :accepted
                            (:event-id container-row)
                            (envelope/now-ms)))

(defn native-claim-compatible?
  "Allow no prior claim, an exact provenance/content match, or a same-container,
   same-kind/object/native-ID/content-hash match across provenance changes."
  [incoming existing]
  (or (nil? existing)
      (and (= (:claim-key incoming) (:claim-key existing))
           (= (:container-id incoming) (:container-id existing))
           (= (:source-native-id incoming) (:source-native-id existing))
           (= (:source-id incoming) (:source-id existing))
           (= (:source-line-key incoming) (:source-line-key existing))
           (= (:source-anchor-id incoming) (:source-anchor-id existing))
           (= (:content-hash incoming) (:content-hash existing))
           (= (:anchor-hash incoming) (:anchor-hash existing))
           (= (:material-fingerprint incoming) (:material-fingerprint existing))
           (= (:import-key incoming) (:import-key existing)))
      (and (= (:claim-key incoming) (:claim-key existing))
           (= (:container-id incoming) (:container-id existing))
           (= (:container-kind incoming) (:container-kind existing))
           (= (:object-key incoming) (:object-key existing))
           (= (:source-native-id incoming) (:source-native-id existing))
           (= (:content-hash incoming) (:content-hash existing)))))

(defn native-claim-conflict-error
  "Return conflict details when incoming and existing native claims are incompatible; else nil."
  [incoming existing]
  (when-not (native-claim-compatible? incoming existing)
    {:type :native-identity/conflict
     :container-id (:container-id incoming)
     :existing-material-fingerprint (:material-fingerprint existing)
     :incoming-material-fingerprint (:material-fingerprint incoming)
     :existing-import-key (:import-key existing)
     :incoming-import-key (:import-key incoming)}))

(defn event-id-from-row
  "Return :event-id from row, or nil when absent."
  [row] (:event-id row))
(defn decision-row-id
  "Return :decision-id from row, or nil when absent."
  [row] (:decision-id row))

(defn latest-source-id
  "Return :source-id from version-row, or nil when absent."
  [version-row]
  (:source-id version-row))

(defn latest-source-present?
  "True when a source-version row is present."
  [version-row]
  (some? version-row))

(defn unit-read-result
  "Return a unit with its current graduated target/content when present, otherwise
   its original derived content; return nil for an absent unit."
  [unit graduation]
  (if unit
    (if graduation
      (->UnitReadResult unit
                        graduation
                        :object-container
                        (:container-id graduation)
                        (:current-content-text graduation)
                        (:current-content-hash graduation))
      (->UnitReadResult unit
                        nil
                        :derived-unit
                        (:unit-id unit)
                        (:derived-content-text unit)
                        (:derived-content-hash unit)))
    nil))

(def common-material-categories
  [:containers :derived-units :anchors :edges])

(defn common-material-category-requested?
  "Test category membership in the requested category collection."
  [categories category]
  (contains? (set categories) category))

(defn common-material-cursor
  "Get the category cursor as a string, defaulting to the start of its index."
  [cursor-map category]
  (str (or (get cursor-map category) "")))

(defn common-material-limit
  "Coerce an explicit limit or the default outline page size to a long."
  [limit]
  (long (or limit default-outline-page-size)))

(defn material-ref-page-values
  "Convert an index page map to its vector of reference values."
  [page]
  (vec (vals page)))

(defn common-material-bundle
  "Collect four material categories as vectors in a CommonMaterialBundle."
  [containers derived-units anchors edges]
  (->CommonMaterialBundle (vec containers)
                          (vec derived-units)
                          (vec anchors)
                          (vec edges)))

(defn edit-lineage-key
  "Prefer explicit edit lineage, then source unit identities, then the target ID.
   This keeps ordering attached to a unit across graduation into a container."
  [request derived-unit graduation container]
  (or (payload-edit-lineage-key (request-payload request))
      (some-> derived-unit :unit-id)
      (some-> container :source-unit-id)
      (some-> graduation :unit-id)
      (get-in request [:target :target/id])))

(defn stale-edit?
  "True when a different idempotency key carries an edit sequence no greater than
   the stored client/lineage order. Assumes numeric sequences when prior order exists."
  [request last-edit-order]
  (let [seq (payload-edit-seq (request-payload request))
        last-seq (:edit-seq last-edit-order)]
    (and (some? last-edit-order)
         (not= (request-idempotency-key request) (:idempotency-key last-edit-order))
         (<= (long seq) (long last-seq)))))

(defn target-not-found?
  "Check the selected target kind against its loaded derived-unit or container row."
  [request derived-unit container]
  (let [target-kind (get-in request [:target :target/kind])]
    (case target-kind
      :derived-unit (nil? derived-unit)
      :object-container (nil? container)
      true)))

(defn edit-effects
  "Compute a rejection or the event/material rows for an object edit from the
   loaded target, graduation, anchor, outline, edge, and prior client order. The first
   edit of an ungraduated unit creates a container; later edits create revisions and
   move current pointers. Returns an effects map for topology writes, not a commit.
   Decision construction uses the wall clock; request time supplies revision order."
  [request derived-unit graduation container source-anchor outline-node child-edge last-edit-order]
  (let [validation-errors (edit-request-validation-errors request)
        target-kind (get-in request [:target :target/kind])
        payload (request-payload request)
        object-key (payload-object-key payload)
        target-id (get-in request [:target :target/id])
        now (:request/time-ms request)
        created-by (get-in request [:actor :actor/id])
        lineage-key (edit-lineage-key request derived-unit graduation container)
        stale? (stale-edit? request last-edit-order)
        not-found? (target-not-found? request derived-unit container)]
    (cond
      (seq validation-errors)
      {:decision (rejected-decision-row request :request-invalid validation-errors)}

      not-found?
      {:decision (rejected-decision-row request :target/not-found
                                        [{:type :target/not-found
                                          :target-kind target-kind
                                          :target-id target-id}])}

      stale?
      {:decision (rejected-decision-row request :edit/stale
                                        [{:type :edit/stale
                                          :edit-client-id (payload-edit-client-id payload)
                                          :edit-seq (payload-edit-seq payload)
                                          :last-seq (:edit-seq last-edit-order)}])}

      :else
      (let [graduating? (and (= :derived-unit target-kind) (nil? graduation))
            existing-container (if (= :object-container target-kind)
                                 container
                                 (when graduation container))
            source-unit-id (or (:unit-id derived-unit)
                               (:source-unit-id existing-container)
                               (:unit-id graduation))
            document-id (payload-document-container-id payload)
            unit-local-id (or (some-> source-unit-id (str/split #":") last)
                              (str "standalone-" target-id))
            container-id (if graduating?
                           (block-container-id object-key unit-local-id)
                           (:container-id existing-container))
            revision-id (revision-id-for-request object-key request)
            parent-revision-id (:current-revision-id existing-container)
            event-id (event-id-for-request object-key request)
            content-text (str (payload-content-text payload))
            content-hash (payload-content-hash payload)
            container-anchor-id (source-anchor-id container-id)
            event-type (if graduating? :object/graduated :object/revised)
            event-payload (if graduating?
                            (->ObjectGraduatedPayload source-unit-id
                                                      container-id
                                                      document-id
                                                      revision-id
                                                      content-hash
                                                      (payload-edit-client-id payload)
                                                      (payload-edit-seq payload))
                            (->ObjectRevisedPayload container-id
                                                    source-unit-id
                                                    document-id
                                                    parent-revision-id
                                                    revision-id
                                                    content-hash
                                                    (payload-edit-client-id payload)
                                                    (payload-edit-seq payload)))
            event (event-row event-id
                             event-type
                             object-key
                             :object-container
                             container-id
                             (:actor request)
                             event-payload
                             now
                             request)
            container-row (->ObjectContainerRow container-id
                                                (if (= document-id container-id)
                                                  :document
                                                  :text-block)
                                                object-key
                                                :private
                                                (or (:source-id existing-container)
                                                    (:source-id derived-unit))
                                                container-anchor-id
                                                source-unit-id
                                                document-id
                                                revision-id
                                                content-text
                                                content-hash
                                                (or (:created-at-ms existing-container) now)
                                                (or (:created-by existing-container) created-by)
                                                event-id)
            revision-row (->RevisionRow revision-id
                                        container-id
                                        parent-revision-id
                                        content-text
                                        content-hash
                                        (fixed-width-order-key now (request-id request))
                                        now
                                        created-by
                                        event-id)
            graduation-row (when source-unit-id
                             (->UnitGraduationRow source-unit-id
                                                  container-id
                                                  revision-id
                                                  content-text
                                                  content-hash
                                                  (or (:graduated-at-ms graduation) now)
                                                  now
                                                  event-id))
            copied-anchor-row (when source-anchor
                                (assoc source-anchor
                                       :source-anchor-id container-anchor-id
                                       :target-kind :object-container
                                       :target-id container-id
                                       :event-id event-id))
            outline-row (when outline-node
                          (assoc outline-node
                                 :target-kind :object-container
                                 :target-id container-id
                                 :content-text content-text
                                 :content-hash content-hash
                                 :graduated true
                                 :container-id container-id
                                 :event-id event-id))
            edge-row (when child-edge
                       (assoc child-edge
                              :child-target-kind :object-container
                              :child-target-id container-id
                              :event-id event-id))
            edit-order-row (->EditOrderRow lineage-key
                                           (payload-edit-client-id payload)
                                           (payload-edit-seq payload)
                                           (request-idempotency-key request)
                                           (request-id request)
                                           revision-id
                                           event-id
                                           now)
            decision (accepted-decision-row request event)]
        {:decision decision
         :event event
         :container-row container-row
         :revision-row revision-row
         :graduation-row graduation-row
         :copied-anchor-row copied-anchor-row
         :outline-row outline-row
         :edge-row edge-row
         :edit-order-row edit-order-row}))))

(defn effect-decision
  "Return :decision from effects, or nil when absent."
  [effects] (:decision effects))
(defn effect-event
  "Return :event from effects, or nil when absent."
  [effects] (:event effects))
(defn effect-container-row
  "Return :container-row from effects, or nil when absent."
  [effects] (:container-row effects))
(defn effect-revision-row
  "Return :revision-row from effects, or nil when absent."
  [effects] (:revision-row effects))
(defn effect-graduation-row
  "Return :graduation-row from effects, or nil when absent."
  [effects] (:graduation-row effects))
(defn effect-copied-anchor-row
  "Return :copied-anchor-row from effects, or nil when absent."
  [effects] (:copied-anchor-row effects))
(defn effect-outline-row
  "Return :outline-row from effects, or nil when absent."
  [effects] (:outline-row effects))
(defn effect-edge-row
  "Return :edge-row from effects, or nil when absent."
  [effects] (:edge-row effects))
(defn effect-edit-order-row
  "Return :edit-order-row from effects, or nil when absent."
  [effects] (:edit-order-row effects))
(defn effect-has-event?
  "True when the effects map contains a nonnil event."
  [effects] (some? (:event effects)))
(defn effect-has-graduation?
  "True when the effects map contains a nonnil graduation row."
  [effects] (some? (:graduation-row effects)))
(defn effect-has-anchor?
  "True when the effects map contains a nonnil copied anchor row."
  [effects] (some? (:copied-anchor-row effects)))
(defn effect-has-outline?
  "True when the effects map contains a nonnil outline row."
  [effects] (some? (:outline-row effects)))
(defn effect-has-edge?
  "True when the effects map contains a nonnil composition edge row."
  [effects] (some? (:edge-row effects)))

(defn import-material-fingerprint
  "Hash the selected identity/content fields of an import bundle and its object/import
   keys. Vector order is significant; uses pr-str over the selected structure and
   does not hash every field of the original payload."
  [object-key import-key payload]
  (envelope/sha-256
   (pr-str
	    {:request/type :object-container/import-material
	     :object-key object-key
	     :import-key import-key
	     :source-ref (:source-ref payload)
	     :source-hash (:source-hash payload)
	     :source-format (:source-format payload)
	     :source-artifacts
     (mapv #(select-keys %
                         [:source-id
                          :source-ref
                          :source-hash
                          :source-format
                          :source-raw-text
                          :document-container-id
                          :content-byte-count])
           (payload-source-artifacts payload))
     :object-containers
     (mapv #(select-keys %
                         [:container-id
                          :container-kind
                          :object-key
                          :visibility
                          :source-id
                          :source-anchor-id
                          :source-unit-id
                          :document-container-id
                          :current-revision-id
                          :current-content-hash])
           (payload-object-containers payload))
     :revisions
     (mapv #(select-keys %
                         [:revision-id
                          :container-id
                          :parent-revision-id
                          :content-hash])
           (payload-revisions payload))
     :derived-units
     (mapv #(select-keys %
                         [:unit-id
                          :document-container-id
                          :source-id
                          :unit-kind
                          :block-path
                          :parent-slot-id
                          :source-anchor-id
                          :derived-content-hash
                          :distiller-id
                          :distiller-version])
           (payload-derived-units payload))
     :source-anchors
     (mapv #(select-keys %
                         [:source-anchor-id
                          :target-kind
                          :target-id
                          :source-id
                          :source-ref
                          :source-hash
                          :start-offset
                          :end-offset
                          :block-path])
           (payload-source-anchors payload))
     :composition-edges
     (mapv #(select-keys %
                         [:edge-id
                          :object-key
                          :document-container-id
                          :parent-slot-id
                          :child-slot-id
                          :child-order-key
                          :parent-target-kind
                          :parent-target-id
                          :child-target-kind
                          :child-target-id
                          :source-id
                          :source-anchor-id])
           (payload-composition-edges payload))
     :source-versions
     (mapv #(select-keys %
                         [:source-ref-key
                          :source-ref
                          :source-hash
                          :source-id
                          :document-container-id
                          :object-key])
           (payload-source-versions payload))
	     :projection-hints
	     (mapv projection-hint-fingerprint
	           (payload-projection-hints payload))
	     :source-line-statuses
	     (mapv source-line-status-fingerprint
	           (payload-source-line-statuses payload))})))

(defn object-edit-request
  "Build an object/edit envelope and typed payload for a unit or container. Options
   can supply stable request/idempotency/revision IDs, actor, document, lineage,
   client ID, sequence, and content hash; defaults include a fresh request ID and
   sequence zero. Construction neither validates against stored state nor appends."
  ([target-kind target-id content-text]
   (object-edit-request target-kind target-id content-text {}))
  ([target-kind target-id content-text opts]
   (let [content-text (str content-text)
         object-key (or (:object/key opts)
                        (:object-key opts)
                        (extract-object-key target-id))
         document-id (or (:document/container-id opts)
                         (:document-container-id opts)
                         (document-id-for-object-key object-key))
         request-id (or (:request/id opts)
                        (:request-id opts)
                        (envelope/random-id "req"))
         payload (->ObjectEditPayload document-id
                                      object-key
                                      content-text
                                      (or (:content/hash opts)
                                          (:content-hash opts)
                                          (source-hash content-text))
                                      (or (:edit/client-id opts)
                                          (:edit-client-id opts)
                                          "default")
                                      (long (or (:edit/seq opts)
                                                (:edit-seq opts)
                                                0))
                                      (or (:edit/lineage-key opts)
                                          (:edit-lineage-key opts))
                                      (or (:revision/id opts)
                                          (:revision-id opts)))
         idempotency-key (or (:idempotency/key opts)
                             (:idempotency-key opts)
                             (str "object/edit:" object-key ":" target-id ":" request-id))
         material-fingerprint (envelope/sha-256
                               (pr-str {:request/type :object/edit
                                        :partition/key object-key
                                        :object/key object-key
                                        :target/kind target-kind
                                        :target/id target-id
                                        :document/container-id document-id
                                        :content/hash (:content-hash payload)
                                        :edit/client-id (:edit-client-id payload)
                                        :edit/seq (:edit-seq payload)
                                        :edit/lineage-key (:edit-lineage-key payload)}))]
     (assoc (envelope/action-request
              {:request-id request-id
               :request-type :object/edit
               :time-ms (:time-ms opts)
               :actor (or (:actor opts)
                          {:actor/id "system"
                           :actor/type :system
                           :actor/capabilities #{:source/ingest :object/edit}})
               :branch (:branch opts)
               :context (:context opts)
               :target {:target/kind target-kind
                        :target/id target-id
                        :target/address {:object/key object-key
                                         :document/container-id document-id}}
               :action {:action/type :object/edit
                        :action/capability :object/edit
                        :action/params {:edit/client-id (:edit-client-id payload)
                                        :edit/seq (:edit-seq payload)}}
               :routing/key [:object-container object-key]
               :payload payload
               :causal (:causal opts)
               :provenance (or (:provenance opts)
                               {:source/type :manual
                                :source/ref nil})})
	            :partition/key object-key
	            :idempotency/key idempotency-key
	            :material/fingerprint material-fingerprint))))

(defmodule object-container-module [setup topologies]
  (declare-depot setup *object-container-requests-depot (hash-by :partition/key))
  (declare-depot setup *transcript-source-line-completions-depot :disallow)
  (let [s (stream-topology topologies "object-container-topology")]
    (declare-pstate s $$requests-by-audit-id {String ObjectContainerRequestRow}
                    {:key-partitioner partition-by-audit-id})
    (declare-pstate s $$decisions-by-audit-id {String ObjectContainerDecisionRow}
                    {:key-partitioner partition-by-audit-id})
    (declare-pstate s $$decisions-by-idempotency
                    {String (map-schema String ObjectContainerDecisionRow {:subindex? true})})
    (declare-pstate s $$events-by-id {String ObjectContainerEventRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$import-completions-by-key {String ImportCompletionRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-artifacts-by-id {String SourceArtifactRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-versions-by-ref
                    {String (map-schema String SourceVersionRow {:subindex? true})})
    (declare-pstate s $$source-latest-by-ref {String SourceVersionRow})
    (declare-pstate s $$source-ingest-completions-by-ref
                    {String (map-schema String SourceIngestCompletionRow {:subindex? true})})
    (declare-pstate s $$containers-by-id {String ObjectContainerRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$revisions-by-id {String RevisionRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$revision-history-by-container
                    {String (map-schema String RevisionRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$derived-units-by-id {String DerivedUnitRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$unit-graduations-by-id {String UnitGraduationRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-anchors-by-target
                    {String (map-schema String SourceAnchorRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$composition-children-by-parent
                    {String (map-schema String CompositionEdgeRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$composition-parent-by-child
                    {String (map-schema String CompositionEdgeRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-containers-by-source
                    {String (map-schema String SourceMaterialRefRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-derived-units-by-source
                    {String (map-schema String SourceMaterialRefRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-anchors-by-source
                    {String (map-schema String SourceMaterialRefRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$source-edges-by-source
                    {String (map-schema String SourceMaterialRefRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$native-identity-claims-by-container {String NativeIdentityClaimRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$outline-by-document
                    {String (map-schema String OutlineNodeRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$transcript-conversation-projection
                    {String (map-schema String TranscriptConversationProjectionRow
                                        {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$transcript-tool-calls-by-name
                    {String (map-schema String TranscriptToolCallIndexRow
                                        {:subindex? true})})
    (declare-pstate s $$transcript-audit-by-request
                    {String (map-schema String TranscriptAuditEntryRow
                                        {:subindex? true})})
    (declare-pstate s $$transcript-last-message-by-conversation
                    {String TranscriptLastMessageRow}
                    {:key-partitioner partition-by-object-key})
    (declare-pstate s $$transcript-source-lines-by-file
                    {String (map-schema String TranscriptSourceLineStatusRow
                                        {:subindex? true})})
    (declare-pstate s $$edit-order-by-target
                    {String (map-schema String EditOrderRow {:subindex? true})}
                    {:key-partitioner partition-by-object-key})
    (<<sources s
      (source> *transcript-source-line-completions-depot {:retry-mode :all-after}
               :> *completed-source-line-status-row)
      (row-file-key *completed-source-line-status-row :> *source-line-file-key)
      (row-order-key *completed-source-line-status-row :> *source-line-order-key)
      (local-transform> [(keypath *source-line-file-key *source-line-order-key)
                          (termval *completed-source-line-status-row)]
                        $$transcript-source-lines-by-file)
      (ack-return> *completed-source-line-status-row))
    (<<sources s
      (source> *object-container-requests-depot {:retry-mode :all-after} :> *request)
      (request-type *request :> *request-type)
      (request-partition-key *request :> *partition-key)
      (request-idempotency-key *request :> *idempotency-key)
      (request-audit-id *request :> *audit-id)
      (decision-id-for-audit-id *audit-id :> *audit-decision-id)
      (local-select> [(keypath *audit-decision-id)] $$decisions-by-audit-id
                     :> *prior-audit-decision)

      (<<if (some? *prior-audit-decision)
        (ack-return> *prior-audit-decision)
        (else>)
        (request-row *request :> *request-row)
        (local-transform> [(keypath *audit-id) (termval *request-row)] $$requests-by-audit-id)
        (local-select> [(keypath *partition-key *idempotency-key)]
                       $$decisions-by-idempotency :> *prior-decision)

        (<<if (some? *prior-decision)
          (material-fingerprint-conflict? *request *prior-decision :> *fingerprint-conflict?)
          (<<if *fingerprint-conflict?
            (material-fingerprint-conflict-error *request *prior-decision
                                                 :> *fingerprint-conflict-error)
            (conflict-decision-row *request
                                   :idempotency/material-fingerprint-conflict
                                   [*fingerprint-conflict-error]
                                   *prior-decision
                                   :> *conflict-decision)
            (decision-row-id *conflict-decision :> *conflict-decision-id)
            (local-transform> [(keypath *conflict-decision-id)
                                (termval *conflict-decision)]
                              $$decisions-by-audit-id)
            (ack-return> *conflict-decision)
            (else>)
            (replay-decision-row *request *prior-decision :> *replay-decision)
            (decision-row-id *replay-decision :> *replay-decision-id)
            (local-transform> [(keypath *replay-decision-id) (termval *replay-decision)]
                              $$decisions-by-audit-id)
            (ack-return> *replay-decision))
          (else>)
          (<<cond
	            (case> (= :object-container/import-material *request-type))
	            (import-request-validation-errors *request :> *import-errors)
	            (<<if (source-request-valid? *import-errors)
	              (request-payload *request :> *import-payload)
	              (payload-object-key *import-payload :> *object-key)
	              (|hash *object-key)
	              (request-import-key *request :> *import-key)
	              (import-event-row *request :> *event-row)
	              (event-id-from-row *event-row :> *event-id)
	              (payload-object-containers *import-payload :> *import-container-rows)
	              (local-select> [(keypath *import-key)]
	                             $$import-completions-by-key :> *existing-import-completion)
	              (<<if (some? *existing-import-completion)
	                (request-material-fingerprint *request :> *import-request-fingerprint)
	                (completion-material-fingerprint *existing-import-completion
	                                                 :> *existing-import-fingerprint)
	                (<<if (= *import-request-fingerprint *existing-import-fingerprint)
	                  (completion-event-id *existing-import-completion
	                                       :> *existing-import-event-id)
	                  (local-select> [(keypath *existing-import-event-id)]
	                                 $$events-by-id :> *existing-import-event)
	                  (<<if (some? *existing-import-event)
	                    (accepted-decision-row *request *existing-import-event
	                                           :> *completed-import-decision)
	                    (decision-row-id *completed-import-decision
	                                     :> *completed-import-decision-id)
		                    (local-transform> [(keypath *completed-import-decision-id)
		                                        (termval *completed-import-decision)]
		                                      $$decisions-by-audit-id)
		                    (payload-source-line-statuses *import-payload
		                                                  :> *existing-source-line-status-rows)
		                    (loop<- [*remaining-existing-source-line-status-rows
		                             *existing-source-line-status-rows
		                             :> *existing-source-line-status-write-done]
		                      (yield-if-overtime)
		                      (<<if (empty? *remaining-existing-source-line-status-rows)
		                        (:> true)
		                        (else>)
		                        (first *remaining-existing-source-line-status-rows
		                               :> *existing-source-line-status-row)
		                        (complete-transcript-source-line-status-row
		                         *existing-source-line-status-row
		                         *existing-import-completion
		                         :> *completed-existing-source-line-status-row)
		                        (row-file-key *completed-existing-source-line-status-row
		                                      :> *existing-source-line-file-key)
		                        (|hash *existing-source-line-file-key)
		                        (depot-partition-append!
		                         *transcript-source-line-completions-depot
		                         *completed-existing-source-line-status-row
		                         :append-ack)
		                        (continue> (rest
		                                    *remaining-existing-source-line-status-rows))))
		                    (|hash *partition-key)
		                    (local-transform> [(keypath *partition-key *idempotency-key)
		                                        (termval *completed-import-decision)]
		                                      $$decisions-by-idempotency)
	                    (ack-return> *completed-import-decision)
	                    (else>)
	                    (rejected-decision-row *request
	                                           :import/completion-event-missing
	                                           [{:type :import/completion-event-missing
	                                             :import-key *import-key
	                                             :event-id *existing-import-event-id}]
	                                           :> *missing-import-event-decision)
	                    (decision-row-id *missing-import-event-decision
	                                     :> *missing-import-event-decision-id)
	                    (local-transform> [(keypath *missing-import-event-decision-id)
	                                        (termval *missing-import-event-decision)]
	                                      $$decisions-by-audit-id)
	                    (local-transform> [(keypath *partition-key *idempotency-key)
	                                        (termval *missing-import-event-decision)]
	                                      $$decisions-by-idempotency)
	                    (ack-return> *missing-import-event-decision))
	                  (else>)
	                  (import-material-fingerprint-conflict-error
	                   *request *existing-import-completion :> *import-conflict-error)
	                  (rejected-decision-row *request
	                                         :import/material-fingerprint-conflict
	                                         [*import-conflict-error]
	                                         :> *import-conflict-decision)
	                  (decision-row-id *import-conflict-decision
	                                   :> *import-conflict-decision-id)
	                  (local-transform> [(keypath *import-conflict-decision-id)
	                                      (termval *import-conflict-decision)]
	                                    $$decisions-by-audit-id)
	                  (local-transform> [(keypath *partition-key *idempotency-key)
	                                      (termval *import-conflict-decision)]
	                                    $$decisions-by-idempotency)
	                  (ack-return> *import-conflict-decision))
	                (else>)
	                (loop<- [*remaining-native-rows *import-container-rows
	                         *native-errors [] :> *native-validation-errors]
	                  (yield-if-overtime)
	                  (<<if (empty? *remaining-native-rows)
	                    (:> *native-errors)
	                    (else>)
	                    (first *remaining-native-rows :> *native-container-row)
	                    (native-identity-claim-row *request *native-container-row
	                                               :> *incoming-claim)
	                    (row-container-id *native-container-row :> *native-container-id)
	                    (local-select> [(keypath *native-container-id)]
	                                   $$native-identity-claims-by-container
	                                   :> *existing-claim)
	                    (native-claim-conflict-error *incoming-claim *existing-claim
	                                                 :> *claim-error)
	                    (<<if (some? *claim-error)
	                      (conj *native-errors *claim-error :> *next-native-errors)
	                      (continue> (rest *remaining-native-rows) *next-native-errors)
	                      (else>)
	                      (continue> (rest *remaining-native-rows) *native-errors))))
	                (<<if (empty? *native-validation-errors)
	                  (payload-source-artifacts *import-payload :> *import-source-rows)
	                  (loop<- [*remaining-import-source-rows *import-source-rows
	                           :> *import-source-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-source-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-source-rows :> *import-source-row)
	                      (row-source-id *import-source-row :> *import-source-id)
	                      (local-transform> [(keypath *import-source-id)
	                                          (termval *import-source-row)]
	                                        $$source-artifacts-by-id)
	                      (continue> (rest *remaining-import-source-rows))))
	                  (loop<- [*remaining-import-container-rows *import-container-rows
	                           :> *import-container-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-container-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-container-rows :> *import-container-row)
	                      (row-container-id *import-container-row :> *import-container-id)
	                      (row-source-id *import-container-row :> *import-container-source-id)
	                      (row-container-kind *import-container-row :> *import-container-kind)
	                      (local-transform> [(keypath *import-container-id)
	                                          (termval *import-container-row)]
	                                        $$containers-by-id)
	                      (source-material-ref-row *import-container-source-id
	                                               *object-key
	                                               :object-container
	                                               *import-container-id
	                                               *import-container-kind
	                                               *event-id
	                                               :> *import-container-source-ref-row)
	                      (source-material-ref-key *import-container-kind
	                                               *import-container-id
	                                               :> *import-container-source-ref-key)
	                      (local-transform> [(keypath *import-container-source-id
	                                                  *import-container-source-ref-key)
	                                          (termval *import-container-source-ref-row)]
	                                        $$source-containers-by-source)
	                      (native-identity-claim-row *request *import-container-row
	                                                 :> *native-claim-row)
	                      (local-transform> [(keypath *import-container-id)
	                                          (termval *native-claim-row)]
	                                        $$native-identity-claims-by-container)
	                      (continue> (rest *remaining-import-container-rows))))
	                  (payload-revisions *import-payload :> *import-revision-rows)
	                  (loop<- [*remaining-import-revision-rows *import-revision-rows
	                           :> *import-revision-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-revision-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-revision-rows :> *import-revision-row)
	                      (row-revision-id *import-revision-row :> *import-revision-id)
	                      (row-container-id *import-revision-row
	                                        :> *import-revision-container-id)
	                      (row-order-key *import-revision-row
	                                     :> *import-revision-order-key)
	                      (local-transform> [(keypath *import-revision-id)
	                                          (termval *import-revision-row)]
	                                        $$revisions-by-id)
	                      (local-transform> [(keypath *import-revision-container-id
	                                                  *import-revision-order-key)
	                                          (termval *import-revision-row)]
	                                        $$revision-history-by-container)
	                      (continue> (rest *remaining-import-revision-rows))))
	                  (payload-derived-units *import-payload :> *import-derived-unit-rows)
	                  (loop<- [*remaining-import-derived-unit-rows *import-derived-unit-rows
	                           :> *import-derived-unit-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-derived-unit-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-derived-unit-rows
	                             :> *import-derived-unit-row)
	                      (row-unit-id *import-derived-unit-row
	                                   :> *import-derived-unit-id)
	                      (row-source-id *import-derived-unit-row
	                                     :> *import-derived-unit-source-id)
	                      (row-block-path *import-derived-unit-row
	                                      :> *import-derived-unit-order-key)
	                      (local-transform> [(keypath *import-derived-unit-id)
	                                          (termval *import-derived-unit-row)]
	                                        $$derived-units-by-id)
	                      (source-material-ref-row *import-derived-unit-source-id
	                                               *object-key
	                                               :derived-unit
	                                               *import-derived-unit-id
	                                               *import-derived-unit-order-key
	                                               *event-id
	                                               :> *import-derived-unit-source-ref-row)
	                      (source-material-ref-key *import-derived-unit-order-key
	                                               *import-derived-unit-id
	                                               :> *import-derived-unit-source-ref-key)
	                      (local-transform> [(keypath *import-derived-unit-source-id
	                                                  *import-derived-unit-source-ref-key)
	                                          (termval *import-derived-unit-source-ref-row)]
	                                        $$source-derived-units-by-source)
	                      (continue> (rest *remaining-import-derived-unit-rows))))
	                  (payload-source-anchors *import-payload :> *import-anchor-rows)
	                  (loop<- [*remaining-import-anchor-rows *import-anchor-rows
	                           :> *import-anchor-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-anchor-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-anchor-rows :> *import-anchor-row)
	                      (row-target-id *import-anchor-row :> *import-anchor-target-id)
	                      (row-source-anchor-id *import-anchor-row
	                                            :> *import-anchor-id)
	                      (row-source-id *import-anchor-row :> *import-anchor-source-id)
	                      (local-transform> [(keypath *import-anchor-target-id
	                                                  *import-anchor-id)
	                                          (termval *import-anchor-row)]
	                                        $$source-anchors-by-target)
	                      (source-material-ref-row *import-anchor-source-id
	                                               *object-key
	                                               :source-anchor
	                                               *import-anchor-id
	                                               *import-anchor-id
	                                               *event-id
	                                               :> *import-anchor-source-ref-row)
	                      (local-transform> [(keypath *import-anchor-source-id
	                                                  *import-anchor-id)
	                                          (termval *import-anchor-source-ref-row)]
	                                        $$source-anchors-by-source)
	                      (continue> (rest *remaining-import-anchor-rows))))
	                  (payload-composition-edges *import-payload :> *import-edge-rows)
	                  (loop<- [*remaining-import-edge-rows *import-edge-rows
	                           :> *import-edge-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-edge-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-edge-rows :> *import-edge-row)
	                      (row-edge-id *import-edge-row :> *import-edge-id)
	                      (row-source-id *import-edge-row :> *import-edge-source-id)
	                      (edge-parent-slot-id *import-edge-row
	                                           :> *import-edge-parent-id)
	                      (edge-child-order-key *import-edge-row
	                                            :> *import-edge-order-key)
	                      (row-child-slot-id *import-edge-row
	                                         :> *import-edge-child-id)
	                      (composition-parent-ref-key *import-edge-row
	                                                  :> *import-edge-parent-ref-key)
	                      (local-transform> [(keypath *import-edge-parent-id
	                                                  *import-edge-order-key)
	                                          (termval *import-edge-row)]
	                                        $$composition-children-by-parent)
	                      (local-transform> [(keypath *import-edge-child-id
	                                                  *import-edge-parent-ref-key)
	                                          (termval *import-edge-row)]
	                                        $$composition-parent-by-child)
	                      (source-material-ref-row *import-edge-source-id
	                                               *object-key
	                                               :composition-edge
	                                               *import-edge-id
	                                               *import-edge-order-key
	                                               *event-id
	                                               :> *import-edge-source-ref-row)
	                      (source-material-ref-key *import-edge-order-key
	                                               *import-edge-id
	                                               :> *import-edge-source-ref-key)
	                      (local-transform> [(keypath *import-edge-source-id
	                                                  *import-edge-source-ref-key)
	                                          (termval *import-edge-source-ref-row)]
	                                        $$source-edges-by-source)
	                      (continue> (rest *remaining-import-edge-rows))))
	                  (payload-source-versions *import-payload
	                                           :> *import-source-version-rows)
	                  (loop<- [*remaining-import-source-version-rows *import-source-version-rows
	                           :> *import-source-version-write-done]
	                    (yield-if-overtime)
	                    (<<if (empty? *remaining-import-source-version-rows)
	                      (:> true)
	                      (else>)
	                      (first *remaining-import-source-version-rows
	                             :> *import-source-version-row)
	                      (row-source-ref-key *import-source-version-row
	                                          :> *import-source-ref-key)
	                      (row-source-hash *import-source-version-row
	                                       :> *import-source-version-key)
	                      (|hash *import-source-ref-key)
	                      (local-transform> [(keypath *import-source-ref-key
	                                                  *import-source-version-key)
	                                          (termval *import-source-version-row)]
	                                        $$source-versions-by-ref)
		                      (local-transform> [(keypath *import-source-ref-key)
			                                          (termval *import-source-version-row)]
			                                        $$source-latest-by-ref)
		                      (source-ingest-completion-row
		                       *request
		                       *import-source-version-row
		                       *event-row
		                       :> *source-ingest-completion-row)
		                      (local-transform> [(keypath *import-source-ref-key
		                                                  *import-source-version-key)
		                                          (termval *source-ingest-completion-row)]
		                                        $$source-ingest-completions-by-ref)
			                      (continue> (rest *remaining-import-source-version-rows))))
		                  (|hash *object-key)
		                  (payload-projection-hints *import-payload
		                                            :> *import-projection-hints)
		                  (loop<- [*remaining-import-projection-hints *import-projection-hints
		                           :> *import-projection-write-done]
		                    (yield-if-overtime)
		                    (<<if (empty? *remaining-import-projection-hints)
		                      (:> true)
		                      (else>)
		                      (first *remaining-import-projection-hints
		                             :> *import-projection-row)
		                      (projection-kind *import-projection-row
		                                       :> *import-projection-kind)
		                      (<<cond
		                        (case> (= :markdown-outline *import-projection-kind))
		                        (row-document-container-id *import-projection-row
		                                                 :> *import-outline-document-id)
		                        (row-block-path *import-projection-row
		                                      :> *import-outline-block-path)
		                        (outline-projection-row *import-projection-row
		                                                :> *import-outline-row)
		                        (|hash *object-key)
		                        (local-transform> [(keypath *import-outline-document-id
		                                                    *import-outline-block-path)
		                                            (termval *import-outline-row)]
		                                          $$outline-by-document)
		                        (continue> (rest *remaining-import-projection-hints))

		                        (case> (= :transcript-conversation-projection
		                                  *import-projection-kind))
		                        (projection-conversation-container-id
		                         *import-projection-row :> *conversation-container-id)
		                        (row-order-key *import-projection-row
		                                       :> *projection-order-key)
		                        (|hash *object-key)
		                        (local-transform> [(keypath *conversation-container-id
		                                                    *projection-order-key)
		                                            (termval *import-projection-row)]
		                                          $$transcript-conversation-projection)
		                        (continue> (rest *remaining-import-projection-hints))

		                        (case> (= :transcript-tool-call-index
		                                  *import-projection-kind))
		                        (projection-tool-name *import-projection-row :> *tool-name)
		                        (row-order-key *import-projection-row :> *tool-order-key)
		                        (|hash *tool-name)
		                        (local-transform> [(keypath *tool-name *tool-order-key)
		                                            (termval *import-projection-row)]
		                                          $$transcript-tool-calls-by-name)
		                        (continue> (rest *remaining-import-projection-hints))

		                        (case> (= :transcript-audit-entry *import-projection-kind))
		                        (projection-request-id *import-projection-row
		                                               :> *audit-request-id)
		                        (row-order-key *import-projection-row :> *audit-order-key)
		                        (|hash *audit-request-id)
		                        (local-transform> [(keypath *audit-request-id *audit-order-key)
		                                            (termval *import-projection-row)]
		                                          $$transcript-audit-by-request)
		                        (continue> (rest *remaining-import-projection-hints))

		                        (case> (= :transcript-last-message *import-projection-kind))
		                        (projection-conversation-container-id
		                         *import-projection-row :> *last-message-conversation-id)
		                        (|hash *object-key)
		                        (local-transform> [(keypath *last-message-conversation-id)
		                                            (termval *import-projection-row)]
		                                          $$transcript-last-message-by-conversation)
		                        (continue> (rest *remaining-import-projection-hints))

		                        (default>)
		                        (continue> (rest *remaining-import-projection-hints)))))
		                  (|hash *object-key)
		                  (local-transform> [(keypath *event-id) (termval *event-row)]
		                                    $$events-by-id)
		                  (accepted-decision-row *request *event-row :> *decision)
		                  (decision-row-id *decision :> *decision-id)
		                  (import-completion-row *request *event-row *decision
		                                         :> *completion-row)
		                  (local-transform> [(keypath *import-key)
		                                      (termval *completion-row)]
		                                    $$import-completions-by-key)
		                  (payload-source-line-statuses *import-payload
		                                                :> *source-line-status-rows)
		                  (loop<- [*remaining-source-line-status-rows *source-line-status-rows
		                           :> *source-line-status-write-done]
		                    (yield-if-overtime)
		                    (<<if (empty? *remaining-source-line-status-rows)
		                      (:> true)
		                      (else>)
		                      (first *remaining-source-line-status-rows
		                             :> *source-line-status-row)
		                      (complete-transcript-source-line-status-row
		                       *source-line-status-row
		                       *completion-row
		                       :> *completed-source-line-status-row)
		                      (row-file-key *completed-source-line-status-row
		                                    :> *source-line-file-key)
			                      (|hash *source-line-file-key)
			                      (depot-partition-append!
			                       *transcript-source-line-completions-depot
			                       *completed-source-line-status-row
		                       :append-ack)
		                      (continue> (rest *remaining-source-line-status-rows))))
		                  (|hash *object-key)
		                  (local-transform> [(keypath *decision-id) (termval *decision)]
		                                    $$decisions-by-audit-id)
		                  (local-transform> [(keypath *partition-key *idempotency-key)
		                                      (termval *decision)]
		                                    $$decisions-by-idempotency)
	                  (ack-return> *decision)
	                  (else>)
	                  (rejected-decision-row *request
	                                         :native-identity/conflict
	                                         *native-validation-errors
	                                         :> *native-conflict-decision)
	                  (decision-row-id *native-conflict-decision
	                                   :> *native-conflict-decision-id)
	                  (local-transform> [(keypath *native-conflict-decision-id)
	                                      (termval *native-conflict-decision)]
	                                    $$decisions-by-audit-id)
	                  (local-transform> [(keypath *partition-key *idempotency-key)
	                                      (termval *native-conflict-decision)]
	                                    $$decisions-by-idempotency)
	                  (ack-return> *native-conflict-decision)))
	              (else>)
	              (rejected-decision-row *request :request-invalid *import-errors :> *decision)
	              (decision-row-id *decision :> *decision-id)
              (local-transform> [(keypath *decision-id) (termval *decision)]
                                $$decisions-by-audit-id)
              (local-transform> [(keypath *partition-key *idempotency-key)
                                  (termval *decision)]
                                $$decisions-by-idempotency)
              (ack-return> *decision))

            (case> (= :object/edit *request-type))
            (request-payload *request :> *edit-payload)
            (payload-object-key *edit-payload :> *object-key)
            (get-in *request [:target :target/kind] :> *target-kind)
            (get-in *request [:target :target/id] :> *target-id)
            (|hash *object-key)
            (<<cond
            (case> (= :derived-unit *target-kind))
            (local-select> [(keypath *target-id)] $$derived-units-by-id :> *derived-unit)
            (local-select> [(keypath *target-id)] $$unit-graduations-by-id :> *graduation)
            (<<if (some? *graduation)
              (row-container-id *graduation :> *container-id)
              (local-select> [(keypath *container-id)] $$containers-by-id :> *container)
              (else>)
              (identity nil :> *container))
	            (local-select> [(keypath *target-id) (subselect MAP-VALS)]
	                           $$source-anchors-by-target :> *source-anchors)
	            (first *source-anchors :> *source-anchor)
	            (row-block-path *derived-unit :> *block-path)
	            (payload-document-container-id *edit-payload :> *document-id)
	            (local-select> [(keypath *document-id *block-path)]
	                           $$outline-by-document :> *outline-node)
	            (local-select> [(keypath *target-id) (subselect MAP-VALS)]
	                           $$composition-parent-by-child :> *child-edges)
	            (first *child-edges :> *child-edge)
	            (edit-lineage-key *request *derived-unit *graduation *container :> *lineage-key)

            (case> (= :object-container *target-kind))
            (identity nil :> *derived-unit)
            (identity nil :> *graduation)
            (local-select> [(keypath *target-id)] $$containers-by-id :> *container)
            (row-source-unit-id *container :> *source-unit-id)
	            (local-select> [(keypath *target-id) (subselect MAP-VALS)]
	                           $$source-anchors-by-target :> *source-anchors)
	            (first *source-anchors :> *source-anchor)
	            (payload-document-container-id *edit-payload :> *document-id)
	            (<<if (some? *source-unit-id)
	              (local-select> [(keypath *source-unit-id)] $$derived-units-by-id :> *source-unit)
	              (row-block-path *source-unit :> *block-path)
	              (local-select> [(keypath *document-id *block-path)]
	                             $$outline-by-document :> *outline-node)
	              (local-select> [(keypath *source-unit-id) (subselect MAP-VALS)]
	                             $$composition-parent-by-child :> *child-edges)
	              (first *child-edges :> *child-edge)
	              (else>)
	              (identity nil :> *outline-node)
	              (identity nil :> *child-edge))
            (edit-lineage-key *request *derived-unit *graduation *container :> *lineage-key)

            (default>)
            (identity nil :> *derived-unit)
            (identity nil :> *graduation)
            (identity nil :> *container)
            (identity nil :> *source-anchor)
            (identity nil :> *outline-node)
            (identity nil :> *child-edge)
            (identity *target-id :> *lineage-key))
          (local-select> [(keypath *lineage-key (payload-edit-client-id *edit-payload))]
                         $$edit-order-by-target :> *last-edit-order)
          (edit-effects *request
                        *derived-unit
                        *graduation
                        *container
                        *source-anchor
                        *outline-node
                        *child-edge
	                        *last-edit-order
	                        :> *effects)
	          (effect-decision *effects :> *decision)
	          (<<if (effect-has-event? *effects)
	            (effect-event *effects :> *event)
	            (event-id-from-row *event :> *event-id)
	            (effect-container-row *effects :> *container-row)
            (row-container-id *container-row :> *container-row-id)
            (effect-revision-row *effects :> *revision-row)
            (row-order-key *revision-row :> *revision-order-key)
            (row-revision-id *revision-row :> *revision-id)
            (effect-edit-order-row *effects :> *edit-order-row)
            (row-lineage-key *edit-order-row :> *edit-lineage-key)
            (row-edit-client-id *edit-order-row :> *edit-client-id)
            (local-transform> [(keypath *event-id) (termval *event)] $$events-by-id)
            (local-transform> [(keypath *container-row-id) (termval *container-row)]
                              $$containers-by-id)
            (local-transform> [(keypath *revision-id) (termval *revision-row)]
                              $$revisions-by-id)
            (local-transform> [(keypath *container-row-id *revision-order-key)
                                (termval *revision-row)]
                              $$revision-history-by-container)
            (local-transform> [(keypath *edit-lineage-key *edit-client-id)
                                (termval *edit-order-row)]
                              $$edit-order-by-target)
            (<<if (effect-has-graduation? *effects)
              (effect-graduation-row *effects :> *graduation-row)
              (row-unit-id *graduation-row :> *graduated-unit-id)
              (local-transform> [(keypath *graduated-unit-id) (termval *graduation-row)]
                                $$unit-graduations-by-id))
	            (<<if (effect-has-anchor? *effects)
	              (effect-copied-anchor-row *effects :> *copied-anchor-row)
	              (row-target-id *copied-anchor-row :> *copied-anchor-target-id)
	              (row-source-anchor-id *copied-anchor-row :> *copied-anchor-id)
	              (row-source-id *copied-anchor-row :> *copied-anchor-source-id)
	              (local-transform> [(keypath *copied-anchor-target-id *copied-anchor-id)
	                                  (termval *copied-anchor-row)]
	                                $$source-anchors-by-target)
	              (source-material-ref-row *copied-anchor-source-id
	                                       *object-key
	                                       :source-anchor
	                                       *copied-anchor-id
	                                       *copied-anchor-id
	                                       *event-id
	                                       :> *copied-anchor-source-ref-row)
	              (local-transform> [(keypath *copied-anchor-source-id *copied-anchor-id)
	                                  (termval *copied-anchor-source-ref-row)]
	                                $$source-anchors-by-source))
            (<<if (effect-has-outline? *effects)
              (effect-outline-row *effects :> *outline-row)
              (row-document-container-id *outline-row :> *outline-document-id)
              (row-block-path *outline-row :> *outline-block-path)
              (local-transform> [(keypath *outline-document-id *outline-block-path)
                                  (termval *outline-row)]
                                $$outline-by-document))
            (<<if (effect-has-edge? *effects)
	              (effect-edge-row *effects :> *edge-row)
	              (edge-parent-slot-id *edge-row :> *edge-parent-slot-id)
	              (edge-child-order-key *edge-row :> *edge-child-order-key)
	              (row-child-slot-id *edge-row :> *edge-child-slot-id)
	              (row-edge-id *edge-row :> *edge-id)
	              (row-source-id *edge-row :> *edge-source-id)
	              (composition-parent-ref-key *edge-row :> *edge-parent-ref-key)
	              (local-transform> [(keypath *edge-parent-slot-id *edge-child-order-key)
	                                  (termval *edge-row)]
	                                $$composition-children-by-parent)
	              (local-transform> [(keypath *edge-child-slot-id *edge-parent-ref-key)
	                                  (termval *edge-row)]
	                                $$composition-parent-by-child)
	              (source-material-ref-row *edge-source-id
	                                       *object-key
	                                       :composition-edge
	                                       *edge-id
	                                       *edge-child-order-key
	                                       *event-id
	                                       :> *edit-edge-source-ref-row)
	              (source-material-ref-key *edge-child-order-key
	                                       *edge-id
	                                       :> *edit-edge-source-ref-key)
		              (local-transform> [(keypath *edge-source-id *edit-edge-source-ref-key)
		                                  (termval *edit-edge-source-ref-row)]
		                                $$source-edges-by-source))
	            (decision-row-id *decision :> *accepted-decision-id)
	            (local-transform> [(keypath *accepted-decision-id) (termval *decision)]
	                              $$decisions-by-audit-id)
	            (local-transform> [(keypath *object-key *idempotency-key)
	                                (termval *decision)]
	                              $$decisions-by-idempotency)
	            (ack-return> *decision)
	            (else>)
	            (decision-row-id *decision :> *decision-id)
	            (local-transform> [(keypath *decision-id) (termval *decision)]
	                              $$decisions-by-audit-id)
	            (local-transform> [(keypath *object-key *idempotency-key)
	                                (termval *decision)]
	                              $$decisions-by-idempotency)
	            (ack-return> *decision))

	          (default>)
          (rejected-decision-row *request :unknown-action-type
                                 [{:type :unknown-action-type :value *request-type}]
                                 :> *decision)
          (decision-row-id *decision :> *decision-id)
          (local-transform> [(keypath *decision-id) (termval *decision)]
                            $$decisions-by-audit-id)
          (local-transform> [(keypath *partition-key *idempotency-key)
                              (termval *decision)]
                            $$decisions-by-idempotency)
	          (ack-return> *decision)))))

	  (<<query-topology topologies "read-latest-source-by-ref" [*source-ref :> *source-row]
	    (source-ref-key *source-ref :> *source-ref-key)
	    (|hash *source-ref-key)
	    (local-select> [(keypath *source-ref-key)] $$source-latest-by-ref :> *latest)
	    (<<if (latest-source-present? *latest)
	      (latest-source-id *latest :> *source-id)
	      (extract-object-key *source-id :> *object-key)
	      (|hash *object-key)
	      (local-select> [(keypath *source-id)] $$source-artifacts-by-id :> *source-row)
	      (|origin)
	      (else>)
	      (identity nil :> *source-row)
	      (|origin)))

	  (<<query-topology topologies "read-source-by-ref-version" [*source-ref *source-version-key
	                                                            :> *source-row]
	    (source-ref-key *source-ref :> *source-ref-key)
	    (|hash *source-ref-key)
	    (local-select> [(keypath *source-ref-key *source-version-key)]
	                   $$source-versions-by-ref :> *version)
	    (<<if (some? *version)
	      (latest-source-id *version :> *source-id)
	      (extract-object-key *source-id :> *object-key)
	      (|hash *object-key)
	      (local-select> [(keypath *source-id)] $$source-artifacts-by-id :> *source-row)
	      (|origin)
	      (else>)
	      (identity nil :> *source-row)
	      (|origin)))

	  (<<query-topology topologies "read-unit" [*unit-id :> *result]
	    (extract-object-key *unit-id :> *object-key)
	    (|hash *object-key)
	    (local-select> [(keypath *unit-id)] $$derived-units-by-id :> *unit)
	    (<<if (some? *unit)
	      (local-select> [(keypath *unit-id)] $$unit-graduations-by-id :> *graduation)
	      (unit-read-result *unit *graduation :> *result)
	      (|origin)
	      (else>)
	      (identity nil :> *result)
	      (|origin)))

	  (<<query-topology topologies "read-current-revision" [*container-id :> *revision]
	    (extract-object-key *container-id :> *object-key)
	    (|hash *object-key)
	    (local-select> [(keypath *container-id)] $$containers-by-id :> *container)
	    (<<if (some? *container)
	      (row-current-revision-id *container :> *revision-id)
	      (<<if (string-present? *revision-id)
	        (local-select> [(keypath *revision-id)] $$revisions-by-id :> *revision)
	        (|origin)
	        (else>)
	        (identity nil :> *revision)
	        (|origin))
	      (else>)
	      (identity nil :> *revision)
	      (|origin)))

	  (<<query-topology topologies "read-common-material-for-source"
	    [*source-id *categories *cursor-map *limit :> *result]
	    (extract-object-key *source-id :> *object-key)
	    (|hash *object-key)
	    (common-material-limit *limit :> *category-limit)
	    (common-material-category-requested? *categories :containers :> *read-containers?)
	    (common-material-category-requested? *categories :derived-units :> *read-derived-units?)
	    (common-material-category-requested? *categories :anchors :> *read-anchors?)
	    (common-material-category-requested? *categories :edges :> *read-edges?)
	    (common-material-cursor *cursor-map :containers :> *containers-cursor)
	    (common-material-cursor *cursor-map :derived-units :> *derived-units-cursor)
	    (common-material-cursor *cursor-map :anchors :> *anchors-cursor)
	    (common-material-cursor *cursor-map :edges :> *edges-cursor)
	    (<<if *read-containers?
	      (local-select> [(keypath *source-id)
	                      (sorted-map-range-from *containers-cursor *category-limit)]
	                     $$source-containers-by-source
	                     {:allow-yield? true}
	                     :> *container-page)
	      (material-ref-page-values *container-page :> *containers)
	      (else>)
	      (identity [] :> *containers))
	    (<<if *read-derived-units?
	      (local-select> [(keypath *source-id)
	                      (sorted-map-range-from *derived-units-cursor *category-limit)]
	                     $$source-derived-units-by-source
	                     {:allow-yield? true}
	                     :> *derived-units-page)
	      (material-ref-page-values *derived-units-page :> *derived-units)
	      (else>)
	      (identity [] :> *derived-units))
	    (<<if *read-anchors?
	      (local-select> [(keypath *source-id)
	                      (sorted-map-range-from *anchors-cursor *category-limit)]
	                     $$source-anchors-by-source
	                     {:allow-yield? true}
	                     :> *anchors-page)
	      (material-ref-page-values *anchors-page :> *anchors)
	      (else>)
	      (identity [] :> *anchors))
	    (<<if *read-edges?
	      (local-select> [(keypath *source-id)
	                      (sorted-map-range-from *edges-cursor *category-limit)]
	                     $$source-edges-by-source
	                     {:allow-yield? true}
	                     :> *edges-page)
	      (material-ref-page-values *edges-page :> *edges)
	      (else>)
	      (identity [] :> *edges))
	    (common-material-bundle *containers *derived-units *anchors *edges :> *result)
	    (|origin)))
    )

(defmodule object-container-transcript-ops-module [setup topologies]
  (declare-depot setup *transcript-control-depot (hash-by :transcript/request-id))
  (declare-depot setup *transcript-file-state-depot (hash-by :source/file-key))
  (mirror-pstate setup
                 $$object-transcript-source-lines-by-file
                 "app.server.rama.object-container/object-container-module"
                 "$$transcript-source-lines-by-file")
  (let [s (stream-topology topologies "transcript-operational-control-topology")]
    (declare-pstate s $$transcript-runs {String TranscriptRunRow})
    (declare-pstate s $$transcript-file-offsets {String TranscriptFileOffsetRow})
    (declare-pstate s $$transcript-file-source-lines-by-file
                    {String (map-schema String TranscriptSourceLineStatusRow
                                        {:subindex? true})})

    (<<sources s
      (source> *transcript-control-depot {:retry-mode :all-after} :> *control-request)
      (transcript-control-request-id *control-request :> *control-request-id)
      (transcript-control-request-type *control-request :> *control-request-type)
      (transcript-control-validation-errors *control-request :> *control-errors)
      (<<if (empty? *control-errors)
        (<<cond
          (case> (= :transcript/run-status *control-request-type))
          (local-select> [(keypath *control-request-id)]
                         $$transcript-runs :> *existing-run-row)
          (transcript-run-status-row *existing-run-row *control-request :> *run-row)
          (local-transform> [(keypath *control-request-id) (termval *run-row)]
                            $$transcript-runs)
          (ack-return> *run-row)

          (default>)
          (transcript-initial-run-row *control-request :> *run-row)
          (local-transform> [(keypath *control-request-id) (termval *run-row)]
                            $$transcript-runs)
          (ack-return> *run-row))
        (else>)
        (transcript-rejected-run-row *control-request *control-errors :> *run-row)
        (<<if (string-present? *control-request-id)
          (local-transform> [(keypath *control-request-id) (termval *run-row)]
                            $$transcript-runs))
        (ack-return> *run-row))

      (source> *transcript-file-state-depot {:retry-mode :all-after} :> *file-state)
      (transcript-file-state-file-key *file-state :> *file-key)
      (transcript-file-state-validation-errors *file-state :> *file-state-errors)
      (<<if (empty? *file-state-errors)
        (local-select> [(keypath *file-key)] $$transcript-file-offsets
                       :> *existing-file-offset-row)
        (transcript-file-offset-row *file-state
                                    *existing-file-offset-row
                                    *file-state-errors
                                    :> *file-offset-row)
        (transcript-file-state-source-lines *file-state :> *file-source-lines)
        (loop<- [*remaining-file-source-lines *file-source-lines
                 :> *file-source-lines-write-done]
          (yield-if-overtime)
          (<<if (empty? *remaining-file-source-lines)
            (:> true)
            (else>)
            (first *remaining-file-source-lines :> *file-source-line)
            (transcript-observed-source-line-status-row
             *file-state
             *file-source-line
             :> *observed-source-line-status-row)
            (row-order-key *observed-source-line-status-row
                           :> *observed-source-line-order-key)
            (local-transform> [(keypath *file-key *observed-source-line-order-key)
                                (termval *observed-source-line-status-row)]
                              $$transcript-file-source-lines-by-file)
            (continue> (rest *remaining-file-source-lines))))
        (transcript-file-offset-last-byte-offset *file-offset-row
                                                 :> *source-line-advance-offset)
        (transcript-source-line-range-cursor *source-line-advance-offset
                                             :> *source-line-advance-cursor)
        (transcript-file-state-advance-limit *file-state :> *source-line-advance-limit)
        (local-select> [(keypath *file-key)
                        (subselect
                         (sorted-map-range-from *source-line-advance-cursor
                                                *source-line-advance-limit)
                         MAP-VALS)]
                       $$transcript-file-source-lines-by-file
                       {:allow-yield? true}
                       :> *observed-source-line-status-rows)
	        (select> [(keypath *file-key)
	                  (subselect
	                   (sorted-map-range-from *source-line-advance-cursor
	                                          *source-line-advance-limit)
	                   MAP-VALS)]
	                 $$object-transcript-source-lines-by-file
	                 :> *completed-source-line-status-rows)
        (|hash *file-key)
        (transcript-advance-file-offset-row
         *file-offset-row
         *observed-source-line-status-rows
         *completed-source-line-status-rows
         :> *advanced-file-offset-row)
        (local-transform> [(keypath *file-key) (termval *advanced-file-offset-row)]
                          $$transcript-file-offsets)
        (ack-return> *advanced-file-offset-row)
        (else>)
        (transcript-file-offset-row *file-state nil *file-state-errors
                                    :> *file-offset-row)
        (ack-return> *file-offset-row)))))
