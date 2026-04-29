(ns app.server.rama.core
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (clojure.lang Keyword)
           (java.security MessageDigest)))

;; Canonical Rama center, 2026-04-28:
;;
;; Projection shows materialized state
;;   -> user/agent acts on a projected target
;;   -> action request enters Rama through a depot
;;   -> Rama interpreter/policy derives an accepted KernelEvent or rejected decision
;;   -> Rama ETL updates materialized state
;;   -> projection changes
;;
;; Text is the first carrier. It is not the ontology.
;;
;; PR trace map:
;;   docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md
;;
;; Correct V0 implemented loop:
;;
;;   helper builds ActionRequest
;;        |
;;        v
;;   *world-requests-depot     <-- Rama boundary: raw request enters the module
;;        |
;;        v
;;   Rama interpreter/policy
;;        |
;;        +--> rejected ActionDecision
;;        |
;;        v
;;   accepted KernelEvent
;;        |
;;        v
;;   Rama ETL materializations
;;        |
;;        v
;;   PState readers -> projection items with target refs
;;
;; V0 still uses one depot. That depot is not a "test" event stream anymore: it
;; receives ActionRequests, and Rama decides what becomes world truth. V1 starts
;; moving the physical shape toward Rama locality by routing requests by the
;; world entity they affect instead of by request id.

(def schema-version 1)
(def default-branch-id "main")
(def line-distiller-id "text-line-v0")

(def kernel-contract-table
  [{:contract :action-request
    :source-of-truth true
    :responsibility "Durable request for the world to interpret."
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
  #{:artifact :revision :address :unit :branch :projection :policy :relation})

(def actor-types
  #{:human :agent :system :bot})

(def discarded-statuses
  #{:rejected :hidden})

(def unit-statuses
  #{:accepted :rejected :hidden :promoted :superseded})

(defn now-ms
  "Wrapper for System/currentTimeMillis because Rama dataflow calls plain fns."
  ^long []
  (System/currentTimeMillis))

(defn random-id
  [prefix]
  (str prefix "_" (java.util.UUID/randomUUID)))

(defn sha-256
  [s]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (.getBytes (str s) "UTF-8"))]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn default-actor
  []
  {:actor/id "system"
   :actor/type :system
   :actor/capabilities #{:world/append :artifact/create :unit/create :unit/judge}})

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
      :else [:world "unkeyed"])))

(defn default-policy
  [action]
  (let [capability (:action/capability action)]
    {:required-capabilities (cond-> #{:world/append}
                              capability (conj capability))
     :visibility :private}))

(declare artifact-id-from-unit-id)

(def routing-key-contract
  {:kind :semantic-vector
   :transitional? true
   :note "V1 routes the request depot by the semantic vector. Current PStates remain keyed by artifact id, so the topology still re-hashes to artifact id for local reads/writes."})

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
   validated/authorized world fact. Rama ETL records the request and decides."
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
  "Build the stable world-kernel event envelope. Carrier-specific facts belong
   in :payload or :target/:target/address, not in the envelope itself."
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

      (and (map? request) (nil? (get-in request [:branch :branch/id])))
      (conj {:type :branch/missing-id}))))

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

(defn accepted-decision
  [request event]
  {:decision/id (decision-id-for-request-id (:request/id request))
   :decision/status :accepted
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id (:event/id event)
   :event event
   :decided-at (now-ms)})

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
   :decided-at (now-ms)})

(defn accepted-decision?
  [decision]
  (= :accepted (:decision/status decision)))

(defn decision-id
  [decision]
  (:decision/id decision))

(defn decision-event
  [decision]
  (:event decision))

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
       :ordering {:key [:artifact artifact-id]}
       :provenance (or provenance {:source/type (or source-type :paste)
                                   :source/ref nil})})))

(defn artifact-id-from-unit-id
  [unit-id]
  (let [s (str unit-id)]
    (if-let [idx (str/index-of s "/line/")]
      (subs s 0 idx)
      s)))

(defn unit-status-event
  [unit-id status & [{:keys [branch-id artifact-id reason actor context causal
                             provenance event-id time-ms]}]]
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
     :payload (cond-> {:artifact/id (or artifact-id (artifact-id-from-unit-id unit-id))
                       :unit/id unit-id
                       :status status}
                reason (assoc :reason reason))
     :causal causal
     :ordering {:key [:unit unit-id]}
     :provenance (or provenance {:source/type :manual
                                 :source/ref nil})}))

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
       :routing/key [:artifact artifact-id]
       :proposed-event-id proposed-event-id
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
     :routing/key [:artifact (or artifact-id (artifact-id-from-unit-id unit-id))]
     :proposed-event-id proposed-event-id
     :payload (cond-> {:artifact/id (or artifact-id (artifact-id-from-unit-id unit-id))
                       :unit/id unit-id
                       :status status}
                reason (assoc :reason reason))
     :causal causal
     :provenance (or provenance {:source/type :manual
                                 :source/ref nil})}))

(defn compat-record-request
  [{:keys [event-type target-kind target-id action-type capability payload actor]}]
  (action-request
    {:request-type :compat/record
     :actor actor
     :target {:target/kind target-kind
              :target/id target-id
              :target/address nil}
     :action {:action/type :compat/record
              :action/capability (or capability :world/append)
              :action/params {}}
     :payload {:compat/event-type event-type
               :compat/action-type action-type
               :compat/payload payload}
     :provenance {:source/type :legacy-route
                  :source/ref nil}}))

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

(defn compat-request->event
  [request]
  (let [event-type (get-in request [:payload :compat/event-type])
        action-type (get-in request [:payload :compat/action-type])]
    (kernel-event
      {:event-id (str (:request/id request) "/event")
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

(defn interpret-ingest-request
  [request]
  (let [errors (request-validation-errors request)]
    (cond
      (seq errors) (rejected-decision request :request-invalid errors)
      (not (authorized-request? request)) (rejected-decision request :actor-not-authorized)
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

(defn interpret-compat-request
  [request]
  (let [errors (request-validation-errors request)]
    (cond
      (seq errors) (rejected-decision request :request-invalid errors)
      (not (authorized-request? request)) (rejected-decision request :actor-not-authorized)
      :else (decide-event request (compat-request->event request)))))

(defn unknown-action-decision
  [request]
  (rejected-decision request :unknown-action-type))

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
                  unit-id (str artifact-id "/line/" n)
                  anchor-id (str artifact-id "/anchor/line/" n)]
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

(defmodule world-kernel-module [setup topologies]
  (declare-depot setup *world-requests-depot (hash-by :routing/key))
  (let [n (stream-topology topologies "world-kernel-topology")]
    (declare-pstate n $$requests-by-id {String (map-schema Keyword Object)})
    (declare-pstate n $$decisions-by-id {String (map-schema Keyword Object)})
    (declare-pstate n $$events-by-id {String (map-schema Keyword Object)})
    (declare-pstate n $$artifacts {String (map-schema Keyword Object)})
    (declare-pstate n $$artifact-heads {String String})
    (declare-pstate n $$branches {String (map-schema Keyword Object)})
    (declare-pstate n $$policies {String (map-schema Keyword Object)})
    (declare-pstate n $$text-revisions {String {String (map-schema Keyword Object)}})
    (declare-pstate n $$units-by-artifact {String {String (map-schema Keyword Object)}})
    (declare-pstate n $$unit-status-by-branch {String {String (map-schema Keyword Object)}})
    (declare-pstate n $$projection-cache {String (map-schema Keyword Object)})

    (<<sources n
      (source> *world-requests-depot :> *request)
      (request-id *request :> *request-id)
      (request-action-type *request :> *action-type)
      (request-validation-errors *request :> *request-errors)

      (<<cond
        (case> (request-errors? *request-errors))
        (rejected-decision *request :request-invalid *request-errors :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *request-id)
        (local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)

        (case> (= :artifact/ingest *action-type))
        (interpret-ingest-request *request :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *request-id)
        (local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)
        (<<if (accepted-decision? *decision)
          (decision-event *decision :> *event)
          (event-id *event :> *event-id)
          (event-branch-id *event :> *branch-id)
          (event-artifact-id *event :> *artifact-id)
          (event-revision-id *event :> *revision-id)
          (branch-materialization *event :> *branch)
          (artifact-materialization *event :> *artifact)
          (revision-materialization *event :> *revision)
          (units-by-id-materialization *event :> *units)
          (|hash *event-id)
          (local-transform> [(keypath *event-id) (termval *event)] $$events-by-id)
          (|hash *branch-id)
          (local-transform> [(keypath *branch-id) (termval *branch)] $$branches)
          (|hash *artifact-id)
          (local-transform> [(keypath *artifact-id) (termval *artifact)] $$artifacts)
          (local-transform> [(keypath *artifact-id) (termval *revision-id)] $$artifact-heads)
          (local-transform> [(keypath *artifact-id) (keypath *revision-id) (termval *revision)] $$text-revisions)
          (local-transform> [(keypath *artifact-id) (termval *units)] $$units-by-artifact))

        (case> (= :unit/status-set *action-type))
        (request-artifact-id *request :> *artifact-id)
        (request-unit-id *request :> *unit-id)
        (|hash *artifact-id)
        (local-select> [(keypath *artifact-id) (keypath *unit-id)] $$units-by-artifact :> *unit)
        (interpret-status-request *request *unit :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *request-id)
        (local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)
        (<<if (accepted-decision? *decision)
          (decision-event *decision :> *event)
          (event-id *event :> *event-id)
          (event-branch-id *event :> *branch-id)
          (event-unit-id *event :> *unit-id)
          (branch-materialization *event :> *branch)
          (status-materialization *event :> *status)
          (|hash *event-id)
          (local-transform> [(keypath *event-id) (termval *event)] $$events-by-id)
          (|hash *branch-id)
          (local-transform> [(keypath *branch-id) (termval *branch)] $$branches)
          (local-transform> [(keypath *branch-id) (keypath *unit-id) (termval *status)] $$unit-status-by-branch))

        (case> (= :compat/record *action-type))
        (interpret-compat-request *request :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *request-id)
        (local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)
        (<<if (accepted-decision? *decision)
          (decision-event *decision :> *event)
          (event-id *event :> *event-id)
          (event-branch-id *event :> *branch-id)
          (branch-materialization *event :> *branch)
          (|hash *event-id)
          (local-transform> [(keypath *event-id) (termval *event)] $$events-by-id)
          (|hash *branch-id)
          (local-transform> [(keypath *branch-id) (termval *branch)] $$branches))

        (default>)
        (unknown-action-decision *request :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *request-id)
        (local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$decisions-by-id)))))

(defn start-kernel-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name world-kernel-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc world-kernel-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :world-requests-depot (foreign-depot ipc module-name "*world-requests-depot")
     :requests-by-id (foreign-pstate ipc module-name "$$requests-by-id")
     :decisions-by-id (foreign-pstate ipc module-name "$$decisions-by-id")
     :events-by-id (foreign-pstate ipc module-name "$$events-by-id")
     :artifacts (foreign-pstate ipc module-name "$$artifacts")
     :artifact-heads (foreign-pstate ipc module-name "$$artifact-heads")
     :branches (foreign-pstate ipc module-name "$$branches")
     :policies (foreign-pstate ipc module-name "$$policies")
     :text-revisions (foreign-pstate ipc module-name "$$text-revisions")
     :units-by-artifact (foreign-pstate ipc module-name "$$units-by-artifact")
     :unit-status-by-branch (foreign-pstate ipc module-name "$$unit-status-by-branch")
     :projection-cache (foreign-pstate ipc module-name "$$projection-cache")}))

(defn close-kernel-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-action-request!
  [runtime request]
  (foreign-append! (:world-requests-depot runtime) request :append-ack)
  request)

(defn select-pstate-one
  [pstate path]
  (first (foreign-select path pstate)))

(defn read-request
  [runtime request-id]
  (select-pstate-one (:requests-by-id runtime) [(keypath request-id)]))

(defn read-decision
  [runtime request-id]
  (select-pstate-one (:decisions-by-id runtime)
                     [(keypath (decision-id-for-request-id request-id))]))

(defn read-event
  [runtime event-id]
  (select-pstate-one (:events-by-id runtime) [(keypath event-id)]))

(defn read-branch
  [runtime branch-id]
  (select-pstate-one (:branches runtime) [(keypath branch-id)]))

(defn read-artifact
  [runtime artifact-id]
  (select-pstate-one (:artifacts runtime) [(keypath artifact-id)]))

(defn read-text-head
  [runtime artifact-id]
  (when-let [revision-id (select-pstate-one (:artifact-heads runtime) [(keypath artifact-id)])]
    (select-pstate-one (:text-revisions runtime) [(keypath artifact-id) (keypath revision-id)])))

(defn read-units
  [runtime artifact-id]
  (or (select-pstate-one (:units-by-artifact runtime) [(keypath artifact-id)]) {}))

(defn read-unit-statuses
  [runtime branch-id]
  (or (select-pstate-one (:unit-status-by-branch runtime) [(keypath branch-id)]) {}))

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
  ([runtime request-id]
   (await-decision runtime request-id 2000))
  ([runtime request-id timeout-ms]
   (await-materialized #(read-decision runtime request-id) some? timeout-ms)))

(defn accepted-event-or-throw
  [decision]
  (if (accepted-decision? decision)
    (:event decision)
    (throw (ex-info "Action request rejected" decision))))

(defn ingest-text!
  [runtime content & [opts]]
  (let [request (ingest-text-request content opts)]
    (append-action-request! runtime request)
    (accepted-event-or-throw (await-decision runtime (:request/id request)))))

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
    (accepted-event-or-throw (await-decision runtime (:request/id request)))))

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
        statuses (read-unit-statuses runtime branch-id)
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
    (await-materialized #(read-unit-statuses runtime default-branch-id)
                        #(contains? % rejected-unit-id))
    {:artifact-event artifact-event
     :unit-count (count units)
     :status-event status-event
     :artifact (read-artifact runtime artifact-id)
     :text-head (read-text-head runtime artifact-id)
     :canonical (read-canonical-view runtime default-branch-id artifact-id)
     :discarded (read-discarded-view runtime default-branch-id artifact-id)}))
