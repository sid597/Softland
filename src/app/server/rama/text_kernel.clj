(ns app.server.rama.text-kernel
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as core
             :refer [accepted-decision? action-request artifact-id-from-unit-id
                     authorized-request? decide-event decision-event decision-id
                     decision-id-for-request-id default-branch-id kernel-event
                     random-id rejected-decision request-validation-errors
                     sha-256 unit-id-parts]]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (clojure.lang Keyword)))

;; ────────────────────────────────────────────────────────────────────────────────
;;   TEXT KERNEL  (V0 / V1)
;;
;;   The Text Kernel is the text-artifact model itself: artifacts, revisions,
;;   branches, units, and declared policies live here. Its actions are
;;   synchronous and intent-only — ingest text, set unit status per branch,
;;   record compatibility — so the kernel needs no claim, observation, or
;;   control depot; the whole decision and materialization happen inside the
;;   request fold.
;;
;;   Compressed:  text becomes addressable, judgeable artifact units.
;;
;;   Commit shape (post fix session 4): one MICROBATCH topology with ZERO
;;   partitioner hops. Every record lands on hash(:routing/key) at the depot
;;   and every PState write for that record happens on that same task in the
;;   same microbatch — per-key serialization is depot order and a request's
;;   full effect set commits atomically (exactly-once). The cost: every PState
;;   is partitioned by the ROUTING KEY, so foreign reads route with
;;   {:pkey routing-key} instead of hashing the row's own id.
;;
;;   This namespace owns the text artifact depot, topology, text materializations,
;;   runtime lifecycle, append helpers, and projection readers. Shared
;;   ActionRequest / KernelEvent envelope contracts live in app.server.rama.core.
;;   For the kernel taxonomy and KERNEL-SHAPE spec see app.server.rama.kernel.
;; ────────────────────────────────────────────────────────────────────────────────

(def line-distiller-id "text-line-v0")

(def discarded-statuses
  #{:rejected :hidden})

(def unit-statuses
  #{:accepted :rejected :hidden :promoted :superseded})

(def routing-key-contract
  {:kind :semantic-vector
   :transitional? true
   :note "V1 routes the request depot by the semantic vector and the topology
          stays on that partition for every read/write (zero partitioner hops),
          so all PStates are partitioned by routing key and foreign reads pass
          {:pkey routing-key}. The vector itself remains a transitional
          placeholder for a domain-specific key."})

(defn artifact-routing-key
  [artifact-id]
  [:artifact artifact-id])

(defn text-artifact-event
  [content & [{:keys [artifact-id revision-id source-type actor branch context
                      causal provenance event-id time-ms]}]]
  (let [artifact-id (or artifact-id (random-id "art"))
        revision-id (or revision-id (random-id "rev"))
        content (str content)]
    (kernel-event
      {:event-id event-id
       :event-type :artifact/ingested
       :time-ms time-ms
       :actor actor
       :branch branch
       :context context
       :target {:target/kind :artifact
                :target/id artifact-id
                :target/address nil}
       :action {:action/type :artifact/ingest
                :action/capability :artifact/create
                :action/params {:artifact/type :text}}
       :payload {:artifact/id artifact-id
                 :artifact/type :text
                 :source/type (or source-type :paste)
                 :text/content content
                 :content/hash (sha-256 content)
                 :revision/id revision-id}
       :causal causal
       :ordering {:key (artifact-routing-key artifact-id)}
       :provenance (or provenance {:source/type (or source-type :paste)
                                   :source/ref nil})})))

(defn unit-status-event
  [unit-id status & [{:keys [branch-id artifact-id reason actor context causal
                             provenance event-id time-ms]}]]
  (let [artifact-id (or artifact-id (artifact-id-from-unit-id unit-id))]
    (kernel-event
      {:event-id event-id
       :event-type :unit/status-set
       :time-ms time-ms
       :actor actor
       :branch {:branch/id (or branch-id default-branch-id)}
       :context context
       :target {:target/kind :unit
                :target/id unit-id
                :target/address nil}
       :action {:action/type :unit/status-set
                :action/capability :unit/judge
                :action/params {:status status}}
       :payload (cond-> {:artifact/id artifact-id
                         :unit/id unit-id
                         :status status}
                  reason (assoc :reason reason))
       :causal causal
       ;; The ordering key IS the routing key (C4): status events serialize on
       ;; their artifact, not on a per-unit key that nothing routes by.
       :ordering {:key (artifact-routing-key artifact-id)}
       :provenance (or provenance {:source/type :manual
                                   :source/ref nil})})))

(defn ingest-text-request
  [content & [{:keys [request-id proposed-event-id artifact-id revision-id source-type
                      actor branch context causal provenance time-ms]}]]
  (let [artifact-id (or artifact-id (random-id "art"))
        revision-id (or revision-id (random-id "rev"))
        content (str content)]
    (action-request
      {:request-id request-id
       :request-type :artifact/ingest
       :time-ms time-ms
       :actor actor
       :branch branch
       :context context
       :target {:target/kind :artifact
                :target/id artifact-id
                :target/address nil}
       :action {:action/type :artifact/ingest
                :action/capability :artifact/create
                :action/params {:artifact/type :text}}
       :routing/key (artifact-routing-key artifact-id)
       ;; Event identity is proposed in the header, minted client-side before
       ;; the append (C5/C7) — never invented inside interpretation.
       :proposed-event-id (or proposed-event-id (random-id "evt"))
       :payload {:artifact/id artifact-id
                 :artifact/type :text
                 :source/type (or source-type :paste)
                 :text/content content
                 :revision/id revision-id}
       :causal causal
       :provenance (or provenance {:source/type (or source-type :paste)
                                   :source/ref nil})})))

(defn unit-status-request
  [unit-id status & [{:keys [request-id proposed-event-id branch-id artifact-id reason
                             actor context causal provenance time-ms]}]]
  (let [artifact-id (or artifact-id (artifact-id-from-unit-id unit-id))]
    (action-request
      {:request-id request-id
       :request-type :unit/status-set
       :time-ms time-ms
       :actor actor
       :branch {:branch/id (or branch-id default-branch-id)}
       :context context
       :target {:target/kind :unit
                :target/id unit-id
                :target/address nil}
       :action {:action/type :unit/status-set
                :action/capability :unit/judge
                :action/params {:status status}}
       :routing/key (artifact-routing-key artifact-id)
       :proposed-event-id (or proposed-event-id (random-id "evt"))
       :payload (cond-> {:artifact/id artifact-id
                         :unit/id unit-id
                         :status status}
                  reason (assoc :reason reason))
       :causal causal
       :provenance (or provenance {:source/type :manual
                                   :source/ref nil})})))

(defn ingest-request->event
  [request]
  (text-artifact-event
    (get-in request [:payload :text/content])
    {:artifact-id (get-in request [:payload :artifact/id])
     :revision-id (get-in request [:payload :revision/id])
     :source-type (get-in request [:payload :source/type])
     :event-id (or (:proposed/event-id request)
                   (str (:request/id request) "/event"))
     :time-ms (:request/time-ms request)
     :actor (:actor request)
     :branch (:branch request)
     :context (:context request)
     :causal (merge {:parents []
                     :correlation/id (:request/id request)
                     :intent/id (get-in request [:causal :intent/id])}
                    (:causal request))
     :provenance {:source/type :action-request
                  :source/ref (:request/id request)}}))

(defn status-request->event
  [request]
  (unit-status-event
    (get-in request [:payload :unit/id])
    (get-in request [:payload :status])
    {:artifact-id (get-in request [:payload :artifact/id])
     :reason (get-in request [:payload :reason])
     :event-id (or (:proposed/event-id request)
                   (str (:request/id request) "/event"))
     :time-ms (:request/time-ms request)
     :actor (:actor request)
     :branch-id (get-in request [:branch :branch/id])
     :context (:context request)
     :causal (merge {:parents []
                     :correlation/id (:request/id request)
                     :intent/id (get-in request [:causal :intent/id])}
                    (:causal request))
     :provenance {:source/type :action-request
                  :source/ref (:request/id request)}}))

(defn interpret-ingest-request
  "existing-revision is the durable revision row already stored under this
   request's (artifact-id, revision-id), or nil. Revisions are immutable once
   accepted: re-ingesting an existing revision id rejects instead of
   overwriting committed content."
  [request existing-revision]
  (let [errors (request-validation-errors request)
        artifact-id (get-in request [:payload :artifact/id])
        revision-id (get-in request [:payload :revision/id])
        content (get-in request [:payload :text/content])]
    (cond
      (seq errors) (rejected-decision request :request-invalid errors)
      (not (authorized-request? request)) (rejected-decision request :actor-not-authorized)

      ;; These values key String-schema PStates on the accept path; reject
      ;; anything that could not be written.
      (not (and (string? artifact-id) (seq artifact-id)
                (string? revision-id) (seq revision-id)
                (string? content)))
      (rejected-decision request :artifact-payload-invalid
                         [{:type :artifact/payload-invalid
                           :artifact/id artifact-id
                           :revision/id revision-id}])

      (some? existing-revision)
      (rejected-decision request :revision-exists
                         [{:type :revision/already-ingested
                           :artifact/id artifact-id
                           :revision/id revision-id}])

      :else (decide-event request (ingest-request->event request)))))

(defn interpret-status-request
  [request unit]
  (let [errors (request-validation-errors request)
        status (get-in request [:payload :status])]
    (cond
      (seq errors) (rejected-decision request :request-invalid errors)
      (not (authorized-request? request)) (rejected-decision request :actor-not-authorized)
      (nil? unit) (rejected-decision request :target-unit-not-found)
      (not (contains? unit-statuses status)) (rejected-decision request :unit-status-invalid)
      :else (decide-event request (status-request->event request)))))

(defn- line-ranges
  [content]
  (let [lines (str/split (str content) #"\n" -1)]
    (loop [idx 0
           offset 0
           remaining lines
           result []]
      (if-let [line (first remaining)]
        (let [end (+ offset (count line))
              next-offset (inc end)]
          (recur (inc idx)
                 next-offset
                 (next remaining)
                 (conj result {:line-index idx
                               :start offset
                               :end end
                               :text line})))
        result))))

(defn line-units
  [artifact-event]
  (let [artifact-id (get-in artifact-event [:payload :artifact/id])
        revision-id (get-in artifact-event [:payload :revision/id])
        content (get-in artifact-event [:payload :text/content])
        root-event-id (:event/id artifact-event)]
    (mapv (fn [{:keys [line-index start end text]}]
            (let [n (inc line-index)
                  ;; Unit identity is revision-scoped: a judgment attached to
                  ;; <artifact>/<revision>/line/<n> can never silently
                  ;; re-attach to a different revision's text (Entity 3).
                  unit-id (str artifact-id "/" revision-id "/line/" n)
                  anchor-id (str artifact-id "/" revision-id "/anchor/line/" n)]
              {:unit/id unit-id
               :unit/type :text/line
               :artifact/id artifact-id
               :revision/id revision-id
               :anchor {:anchor/id anchor-id
                        :anchor/type :text/range
                        :revision/id revision-id
                        :range {:start start
                                :end end
                                :line-index line-index}}
               :unit/preview text
               :unit/order line-index
               :derived-by {:distiller/id line-distiller-id
                            :distiller/version 1}
               :provenance {:root-event-id root-event-id
                            :artifact/id artifact-id
                            :revision/id revision-id}}))
          (line-ranges content))))

(defn units-by-id-materialization
  [event]
  (into {}
        (map (fn [unit]
               [(:unit/id unit)
                (assoc unit
                       :created-by-event/id (:event/id event)
                       :root-event-id (get-in unit [:provenance :root-event-id]))]))
        (line-units event)))

(defn event-type [event] (:event/type event))
(defn event-id [event] (:event/id event))
(defn event-artifact-id [event] (or (get-in event [:payload :artifact/id])
                                    (get-in event [:payload :unit :artifact/id])))
(defn event-revision-id [event] (or (get-in event [:payload :revision/id])
                                    (get-in event [:payload :unit :revision/id])))
(defn event-unit-id [event] (or (get-in event [:payload :unit/id])
                                (get-in event [:payload :unit :unit/id])
                                (get-in event [:target :target/id])))
(defn event-branch-id [event] (get-in event [:branch :branch/id]))

(defn request-id [request] (:request/id request))
(defn request-action-type [request] (get-in request [:action :action/type]))
(defn request-errors? [errors] (boolean (seq errors)))
(defn request-artifact-id [request] (or (get-in request [:payload :artifact/id])
                                        (artifact-id-from-unit-id (get-in request [:payload :unit/id]))))
(defn request-unit-id [request] (or (get-in request [:payload :unit/id])
                                    (get-in request [:target :target/id])))
(defn request-revision-id [request] (get-in request [:payload :revision/id]))

(defn pstate-key
  "Coerce a candidate PState key to something a String-schema read can take:
   the value itself when it is a usable key, otherwise \"\" (a String that is
   never a real key, so the lookup misses instead of poisoning the event)."
  [x]
  (if (and (string? x) (seq x)) x ""))

(defn dedup-request-view
  "The request as fingerprinted for duplicate-id classification. Volatile
   client-minted fields are dropped so a client retry that re-mints
   :request/time-ms still classifies as a replay of the same intent, not a
   conflict (per request-fingerprint's contract). Total: non-maps pass
   through untouched."
  [record]
  (if (map? record)
    (dissoc record :request/time-ms)
    record))

(defn accepted-event-id-of
  "The event id an accepted decision proposes to write, nil for rejections —
   feeds the collision guard's existence probe."
  [decision]
  (when (accepted-decision? decision)
    (get-in decision [:event :event/id])))

(defn guard-event-collision
  "Event immutability (Entity 2): a request whose accepted event id already
   names a committed event must reject instead of overwriting it. The dedup
   gate has already absorbed replays of the SAME request, so any existing
   event seen here belongs to a different request."
  [decision request existing-event]
  (if (and (accepted-decision? decision) (some? existing-event))
    (rejected-decision request :event-id-conflict
                       [{:type :event/id-conflict
                         :event/id (:event/id existing-event)}])
    decision))

(defn branch-materialization
  [event]
  {:branch/id (event-branch-id event)
   :seen-at (:event/time-ms event)
   :last-event/id (:event/id event)})

(defn artifact-materialization
  [event]
  {:artifact/id (get-in event [:payload :artifact/id])
   :artifact/type (get-in event [:payload :artifact/type])
   :source/type (get-in event [:payload :source/type])
   :content/hash (get-in event [:payload :content/hash])
   :created-by (get-in event [:actor :actor/id])
   :created-at (:event/time-ms event)
   :root-event-id (:event/id event)})

(defn revision-materialization
  [event]
  {:revision/id (get-in event [:payload :revision/id])
   :artifact/id (get-in event [:payload :artifact/id])
   :text/content (get-in event [:payload :text/content])
   :content/hash (get-in event [:payload :content/hash])
   :parent-revision/id nil
   :root-event-id (:event/id event)})

(defn status-materialization
  [event]
  {:unit/id (event-unit-id event)
   :branch/id (event-branch-id event)
   :status (get-in event [:payload :status])
   :reason (get-in event [:payload :reason])
   :set-by (get-in event [:actor :actor/id])
   :set-at (:event/time-ms event)
   :event/id (:event/id event)})

(defmodule text-kernel-module [setup topologies]
  (declare-depot setup *text-requests-depot (hash-by :routing/key))
  ;; Microbatch, not stream: a request's full effect set (request row, decision
  ;; row, event row, materializations) must commit atomically and exactly once.
  ;; The topology body contains ZERO partitioners — every record is processed
  ;; entirely on its ingress task hash(:routing/key), so same-key requests are
  ;; serialized in depot order and a status-set appended after its own ingest
  ;; deterministically observes the ingest's writes.
  (let [mb (microbatch-topology topologies "text-kernel-topology")]
    (declare-pstate mb $$requests-by-id {String (map-schema Keyword Object)})
    (declare-pstate mb $$decisions-by-id {String (map-schema Keyword Object)})
    (declare-pstate mb $$events-by-id {String (map-schema Keyword Object)})
    (declare-pstate mb $$artifacts {String (map-schema Keyword Object)})
    (declare-pstate mb $$artifact-heads {String String})
    (declare-pstate mb $$branches {String (map-schema Keyword Object)})
    (declare-pstate mb $$policies {String (map-schema Keyword Object)})
    (declare-pstate mb $$text-revisions {String {String (map-schema Keyword Object)}})
    (declare-pstate mb $$units-by-artifact {String {String (map-schema Keyword Object)}})
    ;; artifact-id → branch-id → unit-id → status row. The artifact (routing
    ;; key) leads so the status write stays on the ingress task; the old
    ;; branch-led keying funneled every judgment in the world onto one task.
    (declare-pstate mb $$unit-statuses {String {String {String (map-schema Keyword Object)}}})
    (declare-pstate mb $$projection-cache {String (map-schema Keyword Object)})

    (<<sources mb
      (source> *text-requests-depot :> %requests)
      (%requests :> *raw)
      ;; Ingress guards (no keyed write may see an unusable key): the audit id
      ;; is the request's own id or a deterministic content surrogate; the
      ;; stored form is the verbatim request or a bounded preview wrapper.
      (core/audit-request-id *raw :> *audit-id)
      (decision-id-for-request-id *audit-id :> *audit-decision-id)
      ;; Dedup gate: the decision row is the durable dedup anchor. A replayed
      ;; identical request and a conflicting id reuse are both total no-ops —
      ;; the committed decision, its event, and all materializations stay
      ;; exactly as committed.
      (local-select> [(keypath *audit-decision-id)] $$decisions-by-id :> *stored-decision)
      (dedup-request-view *raw :> *dedup-view)
      (core/decision-dedup-gate *stored-decision *dedup-view :> *gate)
      (get *gate :gate/status :> *gate-status)
      (filter> (= :proceed *gate-status))
      (core/storable-request *raw :> *storable)
      (local-transform> [(keypath *audit-id) (termval *storable)] $$requests-by-id)
      (request-validation-errors *raw :> *request-errors)
      (request-action-type *raw :> *action-type)

      ;; Interpret: every branch yields exactly one *decision0. Envelope
      ;; validation precedes type dispatch (a malformed unknown type rejects
      ;; :request-invalid, not :unknown-action-type). Branches that consult
      ;; durable state read it locally — the record is already on its key's
      ;; task.
      (<<cond
        (case> (request-errors? *request-errors))
        (core/invalid-request-decision *audit-id *raw *request-errors :> *decision0)

        (case> (= :artifact/ingest *action-type))
        (request-artifact-id *raw :> *raw-artifact-id)
        (request-revision-id *raw :> *raw-revision-id)
        (pstate-key *raw-artifact-id :> *lookup-artifact-id)
        (pstate-key *raw-revision-id :> *lookup-revision-id)
        (local-select> [(keypath *lookup-artifact-id *lookup-revision-id)]
                       $$text-revisions :> *existing-revision)
        (interpret-ingest-request *raw *existing-revision :> *decision0)

        (case> (= :unit/status-set *action-type))
        (request-artifact-id *raw :> *raw-artifact-id)
        (request-unit-id *raw :> *raw-unit-id)
        (pstate-key *raw-artifact-id :> *lookup-artifact-id)
        (pstate-key *raw-unit-id :> *lookup-unit-id)
        (local-select> [(keypath *lookup-artifact-id *lookup-unit-id)]
                       $$units-by-artifact :> *unit)
        (interpret-status-request *raw *unit :> *decision0)

        (case> (= :compat/record *action-type))
        (core/interpret-compat-request *raw :> *decision0)

        (default>)
        (core/unknown-action-decision *raw :> *decision0))

      ;; Event-id collision guard (Entity 2): an accepted decision may not
      ;; overwrite a committed event under the same id. Rejections probe ""
      ;; (never a real key) and read nil.
      (accepted-event-id-of *decision0 :> *proposed-event-id)
      (pstate-key *proposed-event-id :> *lookup-event-id)
      (local-select> [(keypath *lookup-event-id)] $$events-by-id :> *existing-event)
      (guard-event-collision *decision0 *raw *existing-event :> *guarded-decision)
      ;; Stamp the fingerprint from the same normalized view the gate reads,
      ;; or replay classification would never match.
      (core/with-request-fingerprint *guarded-decision *dedup-view :> *decision)
      (decision-id *decision :> *decision-id)
      (local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)

      (<<if (accepted-decision? *decision)
        (decision-event *decision :> *event)
        (event-id *event :> *event-id)
        (event-branch-id *event :> *branch-id)
        (branch-materialization *event :> *branch)
        (event-type *event :> *event-type)
        (local-transform> [(keypath *event-id) (termval *event)] $$events-by-id)
        (local-transform> [(keypath *branch-id) (termval *branch)] $$branches)
        (<<if (= :artifact/ingested *event-type)
          (event-artifact-id *event :> *artifact-id)
          (event-revision-id *event :> *revision-id)
          (artifact-materialization *event :> *artifact)
          (revision-materialization *event :> *revision)
          (units-by-id-materialization *event :> *units)
          (local-transform> [(keypath *artifact-id) (termval *artifact)] $$artifacts)
          (local-transform> [(keypath *artifact-id) (termval *revision-id)] $$artifact-heads)
          (local-transform> [(keypath *artifact-id *revision-id) (termval *revision)] $$text-revisions)
          (local-transform> [(keypath *artifact-id) (termval *units)] $$units-by-artifact))
        (<<if (= :unit/status-set *event-type)
          (event-artifact-id *event :> *artifact-id)
          (event-unit-id *event :> *unit-id)
          (status-materialization *event :> *status)
          (local-transform> [(keypath *artifact-id *branch-id *unit-id) (termval *status)]
                            $$unit-statuses))))))

(defn start-text-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name text-kernel-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc text-kernel-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :text-requests-depot (foreign-depot ipc module-name "*text-requests-depot")
     :requests-by-id (foreign-pstate ipc module-name "$$requests-by-id")
     :decisions-by-id (foreign-pstate ipc module-name "$$decisions-by-id")
     :events-by-id (foreign-pstate ipc module-name "$$events-by-id")
     :artifacts (foreign-pstate ipc module-name "$$artifacts")
     :artifact-heads (foreign-pstate ipc module-name "$$artifact-heads")
     :branches (foreign-pstate ipc module-name "$$branches")
     :policies (foreign-pstate ipc module-name "$$policies")
     :text-revisions (foreign-pstate ipc module-name "$$text-revisions")
     :units-by-artifact (foreign-pstate ipc module-name "$$units-by-artifact")
     :unit-statuses (foreign-pstate ipc module-name "$$unit-statuses")
     :projection-cache (foreign-pstate ipc module-name "$$projection-cache")}))

(defn close-text-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-action-request!
  [runtime request]
  (foreign-append! (:text-requests-depot runtime) request :append-ack)
  request)

;; ── Reads ───────────────────────────────────────────────────────────────────
;;
;; Every PState lives on hash(:routing/key) (zero-hop colocation), so reads
;; route with {:pkey routing-key}. Audit reads take the routing key explicitly
;; (the caller constructed the request, so it has the key); artifact-scoped
;; truth reads derive it from the artifact id.

(defn read-request
  [runtime routing-key request-id]
  (foreign-select-one (keypath request-id) (:requests-by-id runtime)
                      {:pkey routing-key}))

(defn read-decision
  [runtime routing-key request-id]
  (foreign-select-one (keypath (decision-id-for-request-id request-id))
                      (:decisions-by-id runtime)
                      {:pkey routing-key}))

(defn read-event
  [runtime routing-key event-id]
  (foreign-select-one (keypath event-id) (:events-by-id runtime)
                      {:pkey routing-key}))

(defn read-branch
  [runtime routing-key branch-id]
  (foreign-select-one (keypath branch-id) (:branches runtime)
                      {:pkey routing-key}))

(defn read-artifact
  [runtime artifact-id]
  (foreign-select-one (keypath artifact-id) (:artifacts runtime)
                      {:pkey (artifact-routing-key artifact-id)}))

(defn read-text-head
  [runtime artifact-id]
  (let [pkey (artifact-routing-key artifact-id)]
    (when-let [revision-id (foreign-select-one (keypath artifact-id)
                                               (:artifact-heads runtime)
                                               {:pkey pkey})]
      (foreign-select-one (keypath artifact-id revision-id)
                          (:text-revisions runtime)
                          {:pkey pkey}))))

(defn read-units
  [runtime artifact-id]
  (or (foreign-select-one (keypath artifact-id) (:units-by-artifact runtime)
                          {:pkey (artifact-routing-key artifact-id)})
      {}))

(defn read-unit-statuses
  "Status rows for one (artifact, branch): unit-id → status row. Statuses are
   stored under the artifact (the routing key) so this read is one colocated
   seek; a branch-wide read across artifacts is a cross-partition gather and
   deliberately not offered here."
  [runtime artifact-id branch-id]
  (or (foreign-select-one (keypath artifact-id branch-id) (:unit-statuses runtime)
                          {:pkey (artifact-routing-key artifact-id)})
      {}))

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

(defn await-decision
  ([runtime routing-key request-id]
   (await-decision runtime routing-key request-id 2000))
  ([runtime routing-key request-id timeout-ms]
   (await-materialized #(read-decision runtime routing-key request-id) some? timeout-ms)))

(defn accepted-event-or-throw
  [decision]
  (if (accepted-decision? decision)
    (:event decision)
    (throw (ex-info "Action request rejected" decision))))

(defn ingest-text!
  [runtime content & [opts]]
  (let [request (ingest-text-request content opts)]
    (append-action-request! runtime request)
    (accepted-event-or-throw
      (await-decision runtime (:routing/key request) (:request/id request)))))

(defn unitize-lines!
  [runtime artifact-event & [_opts]]
  (let [artifact-id (get-in artifact-event [:payload :artifact/id])]
    (->> (await-materialized #(read-units runtime artifact-id) seq)
         vals
         (sort-by :unit/order)
         vec)))

(defn set-unit-status!
  [runtime unit-id status & [opts]]
  (let [request (unit-status-request unit-id status opts)]
    (append-action-request! runtime request)
    (accepted-event-or-throw
      (await-decision runtime (:routing/key request) (:request/id request)))))

(defn projection-item
  [projection-id branch-id unit status-entry]
  {:projection/id projection-id
   :branch/id branch-id
   :target {:target/kind :unit
            :target/id (:unit/id unit)
            :target/address (:anchor unit)}
   :unit/id (:unit/id unit)
   :unit/type (:unit/type unit)
   :status (or (:status status-entry) :unjudged)
   :preview (:unit/preview unit)
   :order (:unit/order unit)
   :provenance {:artifact/id (:artifact/id unit)
                :revision/id (:revision/id unit)
                :root-event-id (:root-event-id unit)
                :created-by-event/id (:created-by-event/id unit)}})

(defn read-unit-projection
  [runtime branch-id artifact-id view]
  (let [projection-id (str "text/" (name view))
        units (sort-by :unit/order (vals (read-units runtime artifact-id)))
        statuses (read-unit-statuses runtime artifact-id branch-id)
        visible? (case view
                   :canonical #(not (contains? discarded-statuses (:status %)))
                   :discarded #(contains? discarded-statuses (:status %)))]
    (->> units
         (keep (fn [unit]
                 (let [status-entry (get statuses (:unit/id unit))]
                   (when (visible? status-entry)
                     (projection-item projection-id branch-id unit status-entry)))))
         vec)))

(defn read-canonical-view
  [runtime branch-id artifact-id]
  (read-unit-projection runtime branch-id artifact-id :canonical))

(defn read-discarded-view
  [runtime branch-id artifact-id]
  (read-unit-projection runtime branch-id artifact-id :discarded))

(defn run-v0-text-proof!
  [runtime content]
  (let [artifact-event (ingest-text! runtime content {:request-id "req_v0_ingest"
                                                      :proposed-event-id "evt_v0_ingest"
                                                      :artifact-id "art_v0"
                                                      :revision-id "rev_v0"})
        artifact-id (get-in artifact-event [:payload :artifact/id])
        units (unitize-lines! runtime artifact-event)
        rejected-unit-id (:unit/id (second units))
        status-event (set-unit-status! runtime rejected-unit-id :rejected
                                       {:request-id "req_v0_reject"
                                        :proposed-event-id "evt_v0_reject"
                                        :artifact-id artifact-id
                                        :reason "V0 proof rejection"})]
    (await-materialized #(read-unit-statuses runtime artifact-id default-branch-id)
                        #(contains? % rejected-unit-id))
    {:artifact-event artifact-event
     :unit-count (count units)
     :status-event status-event
     :artifact (read-artifact runtime artifact-id)
     :text-head (read-text-head runtime artifact-id)
     :canonical (read-canonical-view runtime default-branch-id artifact-id)
     :discarded (read-discarded-view runtime default-branch-id artifact-id)}))
