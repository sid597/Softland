(ns app.server.rama.core
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

(defn artifact-id-from-unit-id
  [unit-id]
  (let [s (str unit-id)]
    (if-let [idx (str/index-of s "/line/")]
      (subs s 0 idx)
      s)))

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

(defn compat-record-request
  [{:keys [event-type target-kind target-id action-type capability payload actor]}]
  (action-request
    {:request-type :compat/record
     :actor actor
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
