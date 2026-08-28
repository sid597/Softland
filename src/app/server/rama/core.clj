(ns app.server.rama.core
  "Request envelopes, event envelopes, validation, and replay guards.
   Takes: request, event, actor, target, payload, and error maps.
   Gives: normalized envelopes, stable ids, validation results, and replay decisions.
   Holds nothing."
  (:require [clojure.string :as str])
  (:import (java.security MessageDigest)))

;; Shared Rama contracts.
;;
;; Instance modules own their depots, topologies, PStates, materializations, and
;; runtime helpers. This namespace keeps the common request/event envelope and
;; small pure utilities used by those modules.

(def schema-version 1)
(def default-branch-id "main")

(def kernel-contract-table
  [{:contract :action-request
    :source-of-truth true
    :responsibility "Durable request for a kernel instance to interpret."
    :minimum-keys [:request/id :request/type :request/time-ms :routing/key
                   :actor :branch :context :target :action :payload :causal
                   :provenance]}
   {:contract :event
    :source-of-truth true
    :responsibility "Durable accepted fact derived from an action request."
    :minimum-keys [:event/id :event/type :event/time-ms :event/schema-version
                   :actor :branch :context :target :action :payload
                   :causal :ordering :policy :provenance]}
   {:contract :target
    :responsibility "Stable reference to the thing an action/request/event is about."
    :minimum-keys [:target/kind :target/id :target/address]}
   {:contract :action
    :responsibility "Interpreted operation over a target, expressed as a capability."
    :minimum-keys [:action/type :action/capability :action/params]}
   {:contract :materialization
    :responsibility "Reader-friendly PStates derived from requests/events; never source truth."}
   {:contract :projection
    :responsibility "View over materialized state; every meaningful item carries a target ref."}
   {:contract :policy
    :responsibility "Gate observations and actions, including derived views."}
   {:contract :distillation
    :responsibility "Versioned derivation of units, relations, summaries, or branches from raw artifacts."}])

(def target-kinds
  #{:artifact :revision :address :unit :branch :projection :policy :relation
    :space :turn :context-bundle :llm-turn-run :slice :overlay :derivative
    :source-artifact :object-container :object-container-import :derived-unit
    :composition-edge :source-anchor})

(def actor-types
  #{:human :agent :system :bot})

(defn now-ms
  "Wrapper for System/currentTimeMillis because Rama dataflow calls plain fns."
  ^long []
  (System/currentTimeMillis))

(defn random-id
  [prefix]
  (str prefix "_" (java.util.UUID/randomUUID)))

(defn sha-256-bytes
  "SHA-256 hex digest of a raw byte array. Use for byte-correct source identity
   where the hash must reflect the exact on-disk bytes, not a re-encoded string
   (decode->re-encode is lossy for invalid UTF-8)."
  [^bytes ba]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") ba)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn sha-256
  [s]
  (sha-256-bytes (.getBytes (str s) "UTF-8")))

(defn default-actor
  []
  {:actor/id "system"
   :actor/type :system
   :actor/capabilities #{:action/append :artifact/create :unit/create :unit/judge}})

(defn default-context
  []
  {:context/id nil
   :projection/id nil
   :selection/id nil
   :question/id nil})

(defn default-causal
  []
  {:parents []
   :correlation/id nil
   :intent/id nil})

(defn ordering-key-for
  [event-type target]
  (let [target-kind (:target/kind target)
        target-id (:target/id target)]
    (cond
      target-id [(or target-kind :event) target-id]
      event-type [:event/type event-type]
      :else [:event "unkeyed"])))

(defn default-policy
  [action]
  (let [capability (:action/capability action)]
    {:required-capabilities (cond-> #{:action/append}
                              capability (conj capability))
     :visibility :private}))

(defn unit-id-parts
  "Parse a revision-scoped unit id `<artifact>/<revision>/line/<n>`. Returns
   {:artifact/id .. :revision/id .. :line/number n} or nil when the id does
   not match. Parses from the right so artifact ids may contain '/'; revision
   ids must not (kernel-minted ids never do)."
  [unit-id]
  (let [s (str unit-id)]
    (when-let [line-idx (str/last-index-of s "/line/")]
      (let [prefix (subs s 0 line-idx)
            n-str (subs s (+ line-idx 6))]
        (when-let [rev-idx (str/last-index-of prefix "/")]
          (let [artifact-id (subs prefix 0 rev-idx)
                revision-id (subs prefix (inc rev-idx))]
            (when (and (seq artifact-id) (seq revision-id)
                       (re-matches #"\d+" n-str))
              {:artifact/id artifact-id
               :revision/id revision-id
               :line/number (parse-long n-str)})))))))

(defn artifact-id-from-unit-id
  [unit-id]
  (or (:artifact/id (unit-id-parts unit-id))
      ;; legacy pre-revision-scoped shape <artifact>/line/<n>
      (let [s (str unit-id)]
        (if-let [idx (str/index-of s "/line/")]
          (subs s 0 idx)
          s))))

(defn routing-key-for
  [{:keys [request-id target payload]}]
  (let [artifact-id (or (:artifact/id payload)
                        (some-> (:unit/id payload) artifact-id-from-unit-id)
                        (get-in target [:target/address :artifact/id])
                        (when (= :artifact (:target/kind target))
                          (:target/id target)))
        target-kind (:target/kind target)
        target-id (:target/id target)]
    (cond
      artifact-id [:artifact artifact-id]
      target-id [(or target-kind :target) target-id]
      request-id [:request request-id]
      :else [:request "unkeyed"])))

(defn action-request
  "Build the request envelope that enters Rama. This is intentionally not a
   validated/authorized fact. The receiving topology records and decides."
  [opts]
  (let [{:keys [request-id request-type time-ms actor branch context target action
                payload causal provenance proposed-event-id]} opts
        request-id (or request-id (random-id "req"))
        payload (or payload {})
        action (merge {:action/type request-type
                       :action/capability nil
                       :action/params {}}
                      action)
        target (merge {:target/kind :artifact
                       :target/id nil
                       :target/address nil}
                      target)
        routing-key (or (:routing/key opts)
                        (:routing-key opts)
                        (routing-key-for {:request-id request-id
                                          :target target
                                          :payload payload}))
        proposed-event-id (or proposed-event-id (:proposed/event-id opts))]
    (cond-> {:request/id request-id
             :request/type (or request-type (:action/type action))
             :request/time-ms (or time-ms (now-ms))
             :request/schema-version schema-version
             :routing/key routing-key
             :actor (merge (default-actor) actor)
             :branch (merge {:branch/id default-branch-id} branch)
             :context (merge (default-context) context)
             :target target
             :action action
             :payload payload
             :causal (merge (default-causal) causal)
             :provenance (or provenance {:source/type :manual
                                         :source/ref nil})}
      proposed-event-id (assoc :proposed/event-id proposed-event-id))))

(defn kernel-event
  "Build the stable kernel event envelope. Instance-specific facts belong in
   :payload or :target/:target/address, not in the envelope itself."
  [{:keys [event-id event-type time-ms actor branch context target action payload
           causal ordering policy provenance]}]
  (let [action (merge {:action/type event-type
                       :action/capability nil
                       :action/params {}}
                      action)
        target (merge {:target/kind :artifact
                       :target/id nil
                       :target/address nil}
                      target)]
    {:event/id (or event-id (random-id "evt"))
     :event/type event-type
     :event/time-ms (or time-ms (now-ms))
     :event/schema-version schema-version
     :actor (merge (default-actor) actor)
     :branch (merge {:branch/id default-branch-id} branch)
     :context (merge (default-context) context)
     :target target
     :action action
     :payload (or payload {})
     :causal (merge (default-causal) causal)
     :ordering (or ordering {:key (ordering-key-for event-type target)})
     :policy (or policy (default-policy action))
     :provenance (or provenance {:source/type :manual
                                 :source/ref nil})}))

(def required-request-keys
  [:request/id :request/type :request/time-ms :request/schema-version :actor
   :routing/key :branch :context :target :action :payload :causal :provenance])

(def required-event-keys
  [:event/id :event/type :event/time-ms :event/schema-version :actor :branch
   :context :target :action :payload :causal :ordering :policy :provenance])

(defn request-validation-errors
  [request]
  (let [request-type (:request/type request)
        action-type (get-in request [:action :action/type])
        routing-key (:routing/key request)]
    (cond-> []
      (not (map? request))
      (conj {:type :request/not-map})

      (and (map? request) (not-every? #(contains? request %) required-request-keys))
      (conj {:type :request/missing-envelope-key
             :missing (vec (remove #(contains? request %) required-request-keys))})

      ;; Keys can be present with garbage values; the id keys PStates and the
      ;; time stamps decisions/events, so both must be well-typed before any
      ;; keyed write (a non-String id is a write-schema violation = poison
      ;; record; a non-number time makes :decided-at non-deterministic).
      (and (map? request)
           (not (and (string? (:request/id request))
                     (seq (:request/id request)))))
      (conj {:type :request/id-invalid
             :value (:request/id request)})

      (and (map? request) (not (number? (:request/time-ms request))))
      (conj {:type :request/time-ms-invalid
             :value (:request/time-ms request)})

      (and (map? request) (contains? request :event/id))
      (conj {:type :request/top-level-event-id
             :value (:event/id request)})

      (and (map? request) (contains? (:payload request) :event/id))
      (conj {:type :request/payload-event-id
             :value (get-in request [:payload :event/id])})

      (and (map? request) (not (qualified-keyword? (:request/type request))))
      (conj {:type :request/type-not-qualified-keyword
             :value (:request/type request)})

      (and (map? request) (not (qualified-keyword? action-type)))
      (conj {:type :action/type-not-qualified-keyword
             :value action-type})

      ;; Entity 6: the capability slot is mandatory NOW (even before policy
      ;; tables enforce it) so enabling enforcement later changes no envelope.
      (and (map? request)
           (not (keyword? (get-in request [:action :action/capability]))))
      (conj {:type :action/capability-missing
             :value (get-in request [:action :action/capability])})

      (and (map? request) (not= request-type action-type))
      (conj {:type :request/action-type-drift
             :request/type request-type
             :action/type action-type})

      (and (map? request)
           (or (not (vector? routing-key))
               (empty? routing-key)
               (some nil? routing-key)))
      (conj {:type :routing/key-invalid
             :value routing-key})

      (and (map? request)
           (not (contains? actor-types (get-in request [:actor :actor/type]))))
      (conj {:type :actor/invalid-type
             :value (get-in request [:actor :actor/type])})

      (and (map? request)
           (not (contains? target-kinds (get-in request [:target :target/kind]))))
      (conj {:type :target/invalid-kind
             :value (get-in request [:target :target/kind])})

      ;; The branch id keys String-schema PStates; a present-but-non-String
      ;; value would poison the write, so require a real String here.
      (and (map? request)
           (not (and (string? (get-in request [:branch :branch/id]))
                     (seq (get-in request [:branch :branch/id])))))
      (conj {:type :branch/missing-id
             :value (get-in request [:branch :branch/id])}))))

(defn event-validation-errors
  [event]
  (cond-> []
    (not (map? event))
    (conj {:type :event/not-map})

    (and (map? event) (not-every? #(contains? event %) required-event-keys))
    (conj {:type :event/missing-envelope-key
           :missing (vec (remove #(contains? event %) required-event-keys))})

    (and (map? event) (not (qualified-keyword? (:event/type event))))
    (conj {:type :event/type-not-qualified-keyword
           :value (:event/type event)})

    (and (map? event)
         (not (contains? actor-types (get-in event [:actor :actor/type]))))
    (conj {:type :actor/invalid-type
           :value (get-in event [:actor :actor/type])})

    (and (map? event)
         (not (contains? target-kinds (get-in event [:target :target/kind]))))
    (conj {:type :target/invalid-kind
           :value (get-in event [:target :target/kind])})

    (and (map? event) (nil? (get-in event [:branch :branch/id])))
    (conj {:type :branch/missing-id})

    (and (map? event) (nil? (get-in event [:ordering :key])))
    (conj {:type :ordering/missing-key})))

(defn valid-event?
  [event]
  (empty? (event-validation-errors event)))

(defn valid-request?
  [request]
  (empty? (request-validation-errors request)))

(defn authorized-request?
  [request]
  (let [required (cond-> #{}
                   (get-in request [:action :action/capability])
                   (conj (get-in request [:action :action/capability])))
        actor-caps (set (get-in request [:actor :actor/capabilities]))]
    (or (= :system (get-in request [:actor :actor/type]))
        (every? actor-caps required))))

(defn decision-id-for-request-id
  [request-id]
  (str request-id "/decision"))

;; :decided-at is copied from the request's own clock, never the kernel's wall
;; clock: interpretation must be a pure function of the request (+ durable
;; state) so a replayed delivery re-derives a byte-identical decision instead
;; of rewriting committed audit rows.
(defn accepted-decision
  [request event]
  {:decision/id (decision-id-for-request-id (:request/id request))
   :decision/status :accepted
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id (:event/id event)
   :event event
   :decided-at (:request/time-ms request)})

(defn rejected-decision
  [request reason & [errors]]
  {:decision/id (decision-id-for-request-id (:request/id request))
   :decision/status :rejected
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id nil
   :decision/reason reason
   :errors (vec errors)
   :decided-at (:request/time-ms request)})

(defn accepted-decision?
  [decision]
  (= :accepted (:decision/status decision)))

(defn decision-id
  [decision]
  (:decision/id decision))

(defn decision-event
  [decision]
  (:event decision))

;; ── :compat/record — a NARROW, transitional adapter, not a generic event mint.
;;
;; Every compatibility event type must be allow-listed here with a payload
;; validator. Anything else rejects :compat-type-not-allowed — otherwise the
;; legacy route quietly becomes the real public contract for arbitrary
;; KernelEvents (retro finding 06/F3). The set below is exactly what the
;; util-fns transitional helpers emit today; removing a helper should remove
;; its entry.
(def compat-allowed-event-types
  #{:compat/event-id-tick
    :identity/user-registered
    :settings/user-setting-updated
    :cli/session-updated
    :agent/run-submitted
    :sidebar/dir-toggle
    :sidebar/file-select
    :sidebar/project-select
    :sidebar/project-back
    :settings/update
    :agent-trail/saved
    :workspace/save-truth
    :editor/save-doc
    :flow/save-state})

(def ^:private compat-payload-required-keys
  {:cli/session-updated [:file-path :provider :session-id]
   :agent/run-submitted [:run-id]
   :agent-trail/saved [:run-id]
   :identity/user-registered [:username]
   :editor/save-doc [:file-path]})

(defn compat-payload-validation-error
  "nil when the payload is valid for this allow-listed compat event type,
   otherwise a typed error map."
  [event-type payload]
  (cond
    (not (map? payload))
    {:type :compat/payload-not-map :value payload}

    (not-every? #(some? (get payload %))
                (get compat-payload-required-keys event-type []))
    {:type :compat/payload-missing-key
     :missing (vec (remove #(some? (get payload %))
                           (get compat-payload-required-keys event-type [])))}))

(defn compat-record-request
  [{:keys [event-type target-kind target-id action-type capability payload actor]}]
  (action-request
    {:request-type :compat/record
     :actor actor
     ;; Event identity is proposed in the header, minted client-side before
     ;; the append (C5/C7) — never invented inside interpretation.
     :proposed-event-id (random-id "evt")
     :target {:target/kind target-kind
              :target/id target-id
              :target/address nil}
     :action {:action/type :compat/record
              :action/capability (or capability :action/append)
              :action/params {}}
     :payload {:compat/event-type event-type
               :compat/action-type action-type
               :compat/payload payload}
     :provenance {:source/type :legacy-route
                  :source/ref nil}}))

(defn compat-request->event
  [request]
  (let [event-type (get-in request [:payload :compat/event-type])
        action-type (get-in request [:payload :compat/action-type])]
    (kernel-event
      {:event-id (or (:proposed/event-id request)
                     (str (:request/id request) "/event"))
       :event-type event-type
       :time-ms (:request/time-ms request)
       :actor (:actor request)
       :branch (:branch request)
       :context (:context request)
       :target (:target request)
       :action {:action/type (or action-type event-type)
                :action/capability (get-in request [:action :action/capability])
                :action/params {}}
       :payload (get-in request [:payload :compat/payload])
       :causal (merge {:parents []
                       :correlation/id (:request/id request)
                       :intent/id (get-in request [:causal :intent/id])}
                      (:causal request))
       :ordering {:key [(get-in request [:target :target/kind])
                        (get-in request [:target :target/id])]}
       :provenance {:source/type :action-request
                    :source/ref (:request/id request)}})))

(defn decide-event
  [request event]
  (let [errors (event-validation-errors event)]
    (if (seq errors)
      (rejected-decision request :derived-event-invalid errors)
      (accepted-decision request event))))

(defn interpret-compat-request
  [request]
  (let [errors (request-validation-errors request)
        event-type (get-in request [:payload :compat/event-type])
        payload (get-in request [:payload :compat/payload])]
    (cond
      (seq errors) (rejected-decision request :request-invalid errors)
      (not (authorized-request? request)) (rejected-decision request :actor-not-authorized)

      (not (contains? compat-allowed-event-types event-type))
      (rejected-decision request :compat-type-not-allowed
                         [{:type :compat/event-type-not-allowed :value event-type}])

      (compat-payload-validation-error event-type payload)
      (rejected-decision request :compat-payload-invalid
                         [(compat-payload-validation-error event-type payload)])

      :else (decide-event request (compat-request->event request)))))

(defn unknown-action-decision
  [request]
  (rejected-decision request :unknown-action-type))

;; ────────────────────────────────────────────────────────────────────────────
;; Guarded-fold helpers.
;;
;; Pure fns for kernel topologies running under at-least-once delivery. A
;; replayed or duplicated depot record must never reset committed truth, flip a
;; committed decision, regress a terminal status, or rewind a watermark. These
;; helpers make that a one-call guard instead of per-module hand-rolling.
;; ────────────────────────────────────────────────────────────────────────────

(defn canonical-str
  "Deterministic string rendering of nested Clojure data: map entries sorted by
   the canonical rendering of their key, set elements sorted by canonical
   rendering, sequential order preserved. Equal values render identically
   regardless of map/set construction order, so renderings are safe to hash."
  [x]
  (cond
    (map? x)
    (str "{"
         (str/join " " (->> x
                            (map (fn [[k v]] [(canonical-str k) (canonical-str v)]))
                            (sort-by first)
                            (map (fn [[k v]] (str k " " v)))))
         "}")

    (set? x)
    (str "#{" (str/join " " (sort (map canonical-str x))) "}")

    (sequential? x)
    (str "[" (str/join " " (map canonical-str x)) "]")

    :else (pr-str x)))

(defn request-fingerprint
  "Content fingerprint of a request, for duplicate-id detection. Two requests
   with the same id and the same fingerprint are the same intent (safe to
   replay); same id with a different fingerprint is a conflict. Callers whose
   clients re-mint volatile fields (e.g. :request/time-ms) on retry should
   dissoc those fields before fingerprinting."
  [request]
  (sha-256 (canonical-str request)))

(defn with-request-fingerprint
  "Stamp a decision with the fingerprint of the request it decided. Decisions
   written with this stamp let decision-dedup-gate distinguish replay from
   conflict on the next delivery of the same request id."
  [decision request]
  (assoc decision :request/fingerprint (request-fingerprint request)))

(defn replay-decision
  "Mark a stored decision as the response to a replayed delivery. The returned
   value is for the caller/ack path only — the stored decision row and any
   events derived from it must not be rewritten."
  [decision]
  (assoc decision :decision/replay? true))

(defn conflict-rejected-decision
  "Rejected decision for a request id that already has a committed decision but
   arrived again with different content. Carries a distinct :decision/id so it
   can never alias or overwrite the committed decision row."
  [request existing-fingerprint incoming-fingerprint]
  (-> (rejected-decision request :request-id-conflict
                         [{:type :request/id-conflict
                           :existing/fingerprint existing-fingerprint
                           :incoming/fingerprint incoming-fingerprint}])
      (assoc :decision/id (str (decision-id-for-request-id (:request/id request)) "/conflict")
             :decision/conflict? true)))

(defn decision-dedup-gate
  "Classify an incoming request against the stored decision for its request id.
   The topology reads the stored decision FIRST, calls this, and only
   interprets + writes state when the gate says :proceed.

   Returns one of:
     {:gate/status :proceed}
       no decision exists for this id — interpret and write normally.
     {:gate/status :replay  :gate/decision <stored decision, replay-marked>}
       same id + same fingerprint (or a legacy stored decision with no
       fingerprint, where conflict cannot be proven) — return the committed
       decision unchanged; NO state writes, no re-interpretation.
     {:gate/status :conflict :gate/decision <conflict-rejected decision>}
       same id + different fingerprint — reject the impostor; NO state writes.
       The committed decision and its events stay exactly as committed."
  [stored-decision incoming-request]
  (if (nil? stored-decision)
    {:gate/status :proceed}
    (let [stored-fp (:request/fingerprint stored-decision)
          incoming-fp (request-fingerprint incoming-request)]
      (if (or (nil? stored-fp) (= stored-fp incoming-fp))
        {:gate/status :replay
         :gate/decision (replay-decision stored-decision)}
        {:gate/status :conflict
         :gate/decision (conflict-rejected-decision incoming-request stored-fp incoming-fp)}))))

(defn write-if-absent
  "Row guard for initial inserts under at-least-once delivery: keep the existing
   row when one is present (a replay must not reset a row that has since
   progressed), otherwise take the proposed row. Designed for use inside a
   (term ...) navigation:
     (local-transform> [(keypath *id) (term #(write-if-absent % *row))] $$rows)"
  [existing-row proposed-row]
  (if (some? existing-row) existing-row proposed-row))

(defn sticky-status
  "Status-level terminal fence: once a status is terminal it never regresses.
   Returns the status that must be stored."
  [terminal-statuses current-status proposed-status]
  (if (contains? terminal-statuses current-status)
    current-status
    proposed-status))

(defn sticky-terminal-fence
  "Row-level terminal fence: if the current row's status (under status-key) is
   terminal, keep the current row untouched; otherwise take the proposed row.
   Designed for use inside a (term ...) navigation, like write-if-absent."
  [terminal-statuses status-key current-row proposed-row]
  (if (contains? terminal-statuses (get current-row status-key))
    current-row
    proposed-row))

(defn monotonic-watermark
  "Watermarks only advance. nil-safe on both sides: a nil proposal keeps the
   current watermark; a nil current watermark takes the proposal; otherwise the
   max of the two. A replayed record carrying an older watermark can never
   rewind progress."
  [current proposed]
  (cond
    (nil? proposed) current
    (nil? current) proposed
    :else (max current proposed)))

;; ────────────────────────────────────────────────────────────────────────────
;; Observation/control authorization.
;;
;; Observations and controls arrive from outside the topology's truth (workers,
;; executors, users) and must prove their authority before they fold into a
;; truth row. Invalid input of ANY shape produces a bounded dead-letter value —
;; never a throw (a throw inside a topology is a poison record that retries
;; forever) and never a silent truth mutation.
;; ────────────────────────────────────────────────────────────────────────────

(def default-dead-letter-preview-chars 512)

(defn- safe-pr-str
  [x]
  (try
    (pr-str x)
    (catch Throwable _
      (str "<unprintable " (or (some-> (class x) .getName) "nil") ">"))))

(defn bounded-dead-letter
  "Bounded error value describing a rejected observation/control record. Total:
   never throws, regardless of record shape. The preview is capped so unbounded
   payloads cannot be copied wholesale into a dead-letter PState. The optional
   :context map is for small caller-supplied identifiers (run id, depot name) —
   it is included as-is, so keep it small. Never put expected secrets (claim
   tokens) in the context."
  ([reason record] (bounded-dead-letter reason record {}))
  ([reason record {:keys [max-preview-chars context]
                   :or {max-preview-chars default-dead-letter-preview-chars}}]
   (let [preview (safe-pr-str record)
         truncated? (> (count preview) max-preview-chars)]
     (cond-> {:dead-letter/reason reason
              :dead-letter/record-preview (if truncated?
                                            (subs preview 0 max-preview-chars)
                                            preview)
              :dead-letter/record-truncated? truncated?}
       context (assoc :dead-letter/context context)))))

(defn authorize-mutation
  "Authorization gate for observation/control records before they fold into a
   truth row. Total fn: never throws; garbage input of any shape returns a
   :rejected outcome with a bounded dead-letter value.

   target-row  the existing truth row the record claims to mutate (nil = absent)
   record      the incoming observation/control record (any shape)
   opts:
     :status-key          key in target-row holding the lifecycle status
     :accepting-statuses  set of statuses in which this mutation is allowed
     :terminal-statuses   set of sticky terminal statuses; anything arriving
                          while the row is terminal is rejected as late
     :claim-token-key     key in target-row holding the granted claim token;
                          when provided, the record must present the identical
                          token under :record-token-key
     :record-token-key    where the token lives on the record (default :claim/token)
     :seq-key             key in record carrying its sequence number; when
                          provided the record must carry a number, and a value
                          at or below :watermark classifies as :replay
     :watermark           current sequence watermark for the target row
     :context             small identifier map merged into any dead-letter

   Check order: record-shape → target-exists → terminal-rejects-late →
   state-accepts → claim-token-proof → valid-sequence. Terminal is checked
   before the accepting set so a late write gets the precise :target-terminal
   reason instead of a generic state rejection.

   Returns one of:
     {:auth/status :accepted}
     {:auth/status :replay  :auth/reason :sequence-replayed}   ; duplicate delivery — ignore, no dead-letter
     {:auth/status :rejected :auth/reason <kw> :auth/dead-letter <bounded map>}

   Dead-letters never contain the expected claim token, only the fact that the
   offered token was missing or mismatched."
  [target-row record {:keys [status-key accepting-statuses terminal-statuses
                             claim-token-key record-token-key seq-key watermark
                             context]
                      :or {record-token-key :claim/token}}]
  (try
    (let [reject (fn [reason extra-context]
                   {:auth/status :rejected
                    :auth/reason reason
                    :auth/dead-letter (bounded-dead-letter
                                        reason record
                                        {:context (merge context extra-context)})})
          current-status (when (and status-key (map? target-row))
                           (get target-row status-key))]
      (cond
        (not (map? record))
        (reject :record-not-map nil)

        (nil? target-row)
        (reject :target-not-found nil)

        (and terminal-statuses (contains? terminal-statuses current-status))
        (reject :target-terminal {:target/status current-status})

        (and accepting-statuses (not (contains? accepting-statuses current-status)))
        (reject :state-rejects-mutation {:target/status current-status})

        (and claim-token-key (nil? (get target-row claim-token-key)))
        (reject :no-claim-granted nil)

        (and claim-token-key (nil? (get record record-token-key)))
        (reject :token-missing nil)

        (and claim-token-key (not= (get target-row claim-token-key)
                                   (get record record-token-key)))
        (reject :token-mismatch nil)

        (and seq-key (not (number? (get record seq-key))))
        (reject :sequence-invalid {:sequence/value (get record seq-key)})

        (and seq-key (number? watermark) (<= (get record seq-key) watermark))
        {:auth/status :replay
         :auth/reason :sequence-replayed}

        :else
        {:auth/status :accepted}))
    (catch Throwable t
      ;; The fallback fence must itself be throw-proof: context may be the
      ;; very non-map that broke the main path, so sanitize before merging.
      {:auth/status :rejected
       :auth/reason :authorization-error
       :auth/dead-letter (bounded-dead-letter
                           :authorization-error record
                           {:context (merge (when (map? context) context)
                                            {:error/class (.getName (class t))})})})))

;; ────────────────────────────────────────────────────────────────────────────
;; Ingress guards for raw depot records.
;;
;; A client-appendable depot will eventually carry garbage: non-maps, maps
;; with non-keyword keys, missing/non-String request ids. None of that may
;; reach a keyed PState write (a write-schema violation is a poison record
;; that retries forever — and under microbatch it stalls the whole partition).
;; Every appended record still gets exactly one durable decision (no silent
;; drops), keyed by a deterministic surrogate when the record cannot provide
;; its own identity.
;; ────────────────────────────────────────────────────────────────────────────

(defn audit-request-id
  "The durable audit identity for a raw depot record: its :request/id when that
   is a usable String key, otherwise a deterministic content-derived surrogate
   (replay-stable, so a retried garbage record dedups against itself)."
  [record]
  (let [id (when (map? record) (:request/id record))]
    (if (and (string? id) (seq id))
      id
      (str "invalid/" (sha-256 (canonical-str record))))))

(defn storable-request
  "The value safe to store in a {String (map-schema Keyword Object)} request
   PState: the record itself when its shape fits, otherwise a bounded preview
   wrapper (never the raw value — that is the write-schema poison)."
  [record]
  (if (and (map? record) (every? keyword? (keys record)))
    record
    (bounded-dead-letter :request-unstorable record)))

(defn invalid-request-decision
  "Rejected decision for a record that failed envelope validation, keyed by the
   audit id (which may be a surrogate — (:request/id record) is unusable for
   exactly the records this fn exists for)."
  [audit-id record errors]
  (-> (rejected-decision (if (map? record) record {}) :request-invalid errors)
      (assoc :decision/id (decision-id-for-request-id audit-id)
             :request/id audit-id)))
