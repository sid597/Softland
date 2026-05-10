(ns app.server.rama.dogfood.world
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as kernel]
            [app.server.rama.dogfood.llm :as llm]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]))

(def schema-version 1)
(def default-world-branch-id "world/main")

(def world-turn-request-types
  #{:world-turn/compose-and-send
    :world-turn/draft-save
    :world-turn/comment-create
    :world-turn/slice-create
    :world-turn/derivative-create
    :world-turn/patch-accept
    :world-turn/patch-reject
    :world-turn/tool-approval-resolve
    :world-turn/cancel
    :world-turn/compact-request
    :world-turn/steer
    :world-turn/abandon})

(def world-control-request-types
  #{:world-turn/tool-approval-resolve
    :world-turn/cancel
    :world-turn/compact-request
    :world-turn/steer})

(def world-only-turn-request-types
  (disj world-turn-request-types :world-turn/compose-and-send))

(defn now-ms [] (kernel/now-ms))
(defn random-id [prefix] (kernel/random-id prefix))

(defn blank-string?
  [x]
  (or (not (string? x)) (str/blank? x)))

(defn world-routing-key
  [world-thread-id]
  [:world-thread world-thread-id])

(defn normalize-actor
  [actor]
  (merge {:actor/id "system"
          :actor/type :system}
         actor))

(defn action-capability
  [request-type]
  (case request-type
    :world-thread/create :world-thread/create
    :world-turn/compose-and-send :llm/send-turn
    :world-turn/draft-save :world-turn/write
    :world-turn/comment-create :world-turn/write
    :world-turn/slice-create :world-turn/write
    :world-turn/derivative-create :world-turn/write
    :world-turn/patch-accept :world-turn/write
    :world-turn/patch-reject :world-turn/write
    :world-turn/tool-approval-resolve :llm/control
    :world-turn/cancel :world-turn/control
    :world-turn/compact-request :llm/control
    :world-turn/steer :llm/control
    :world-turn/abandon :world-turn/control
    :world/write))

(defn world-action-request
  [request-type world-thread-id & [opts]]
  (let [request-id (or (:request-id opts) (:request/id opts) (random-id "world-req"))
        time-ms (or (:time-ms opts) (:request/time-ms opts) (now-ms))
        world-thread-id (or world-thread-id
                            (:world-thread-id opts)
                            (:world-thread/id opts)
                            (get-in opts [:payload :world-thread/id]))
        payload (merge (:payload opts)
                       (select-keys opts
                                    [:prompt/text :refs :llm/options
                                     :execution/options :world-turn/kind
                                     :draft/id :title])
                       {:world-thread/id world-thread-id})
        routing-key (or (:routing/key opts)
                        (:routing-key opts)
                        (world-routing-key world-thread-id))]
    {:request/id request-id
     :request/type request-type
     :request/schema-version schema-version
     :request/time-ms time-ms
     :idempotency/key (or (:idempotency-key opts)
                          (:idempotency/key opts)
                          request-id)
     :client/op-id (:client/op-id opts)
     :routing/key routing-key
     :actor (normalize-actor (:actor opts))
     :branch (merge {:branch/id default-world-branch-id} (:branch opts))
     :context (merge {:projection/id nil
                      :selection/id nil}
                     (:context opts))
     :target {:target/kind :world-thread
              :target/id world-thread-id}
     :action {:action/type request-type
              :action/capability (action-capability request-type)
              :action/params (or (:action/params opts) {})}
     :payload payload
     :causal (merge {:parents []
                     :correlation/id request-id
                     :intent/id nil}
                    (:causal opts))
     :provenance (or (:provenance opts)
                     {:source/type :projection
                      :source/ref (:projection/id (:context opts))})}))

(defn world-thread-create-request
  [world-thread-id & [opts]]
  (world-action-request :world-thread/create world-thread-id opts))

(defn compose-and-send-request
  [world-thread-id prompt-text & [opts]]
  (world-action-request
    :world-turn/compose-and-send
    world-thread-id
    (assoc opts :prompt/text prompt-text)))

(defn world-only-turn-request
  [request-type world-thread-id & [opts]]
  (world-action-request request-type world-thread-id opts))

(def required-request-keys
  [:request/id :request/type :request/schema-version :request/time-ms
   :idempotency/key :routing/key :actor :target :action :payload])

(defn request-thread-id
  [request]
  (or (:world-thread/id request)
      (get-in request [:payload :world-thread/id])
      (get-in request [:target :target/id])))

(defn request-turn-id
  [request]
  (or (get-in request [:payload :world-turn/id])
      (:world-turn/id request)
      (str (:request/id request) "/turn")))

(defn request-bundle-id
  [request]
  (or (get-in request [:payload :context-bundle/id])
      (:context-bundle/id request)
      (str (:request/id request) "/bundle")))

(defn request-type [request] (:request/type request))
(defn request-id [request] (:request/id request))
(defn request-idempotency-key [request] (:idempotency/key request))
(defn request-errors? [errors] (boolean (seq errors)))
(defn world-only-turn-request? [request-type] (contains? world-only-turn-request-types request-type))
(defn world-control-request? [request-type] (contains? world-control-request-types request-type))

(defn request-validation-errors
  [request]
  (let [request-type (:request/type request)
        thread-id (request-thread-id request)]
    (cond-> []
      (not (map? request))
      (conj {:type :request/not-map})

      (and (map? request) (not-every? #(contains? request %) required-request-keys))
      (conj {:type :request/missing-envelope-key
             :missing (vec (remove #(contains? request %) required-request-keys))})

      (and (map? request)
           (not (or (= :world-thread/create request-type)
                    (contains? world-turn-request-types request-type))))
      (conj {:type :request/type-invalid
             :value request-type})

      (and (map? request) (blank-string? (:request/id request)))
      (conj {:type :request/id-invalid
             :value (:request/id request)})

      (and (map? request) (blank-string? (:idempotency/key request)))
      (conj {:type :idempotency/key-invalid
             :value (:idempotency/key request)})

      (and (map? request) (blank-string? thread-id))
      (conj {:type :world-thread/id-invalid
             :value thread-id})

      (and (map? request) (not= (world-routing-key thread-id) (:routing/key request)))
      (conj {:type :routing/key-invalid
             :value (:routing/key request)
             :expected (world-routing-key thread-id)})

      (and (map? request)
           (not= request-type (get-in request [:action :action/type])))
      (conj {:type :request/action-type-drift
             :request/type request-type
             :action/type (get-in request [:action :action/type])})

      (and (map? request) (= :world-turn/compose-and-send request-type)
           (blank-string? (get-in request [:payload :prompt/text])))
      (conj {:type :prompt/text-invalid
             :value (get-in request [:payload :prompt/text])}))))

(defn decision-id-for-request-id
  [request-id]
  (str request-id "/decision"))

(defn event-id-for
  [request suffix]
  (str (:request/id request) "/event/" suffix))

(defn accepted-decision
  [request primary-event events-by-role]
  (let [events (vec (keep events-by-role [:thread :turn :context-bundle :llm-turn-run :llm-control]))
        turn-event (:turn events-by-role)
        bundle-event (:context-bundle events-by-role)
        llm-run-event (:llm-turn-run events-by-role)
        llm-control-event (:llm-control events-by-role)]
    {:decision/id (decision-id-for-request-id (:request/id request))
     :decision/status :accepted
     :request/id (:request/id request)
     :request/type (:request/type request)
     :routing/key (:routing/key request)
     :event/id (:event/id primary-event)
     :event/ids (mapv :event/id events)
     :events events-by-role
     :world-thread/id (request-thread-id request)
     :world-turn/id (:world-turn/id turn-event)
     :context-bundle/id (:context-bundle/id bundle-event)
     :context-bundle/hash (:context-bundle/hash bundle-event)
     :llm-thread/id (:llm-thread/id llm-run-event)
     :llm-turn-run/id (:llm-turn-run/id llm-run-event)
     :llm/request-id (:llm/request-id llm-run-event)
     :llm-control/id (:llm-control/id llm-control-event)
     :llm-control/type (:llm-control/type llm-control-event)
     :approval/id (:approval/id llm-control-event)
     :decided-at-ms (:request/time-ms request)}))

(defn rejected-decision
  [request reason & [errors]]
  {:decision/id (decision-id-for-request-id (:request/id request))
   :decision/status :rejected
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id nil
   :event/ids []
   :decision/reason reason
   :errors (vec errors)
   :decided-at-ms (:request/time-ms request)})

(defn decision-accepted? [decision] (= :accepted (:decision/status decision)))
(defn decision-id [decision] (:decision/id decision))
(defn decision-thread-event [decision] (get-in decision [:events :thread]))
(defn decision-turn-event [decision] (get-in decision [:events :turn]))
(defn decision-bundle-event [decision] (get-in decision [:events :context-bundle]))
(defn decision-llm-run-event [decision] (get-in decision [:events :llm-turn-run]))
(defn decision-llm-control-event [decision] (get-in decision [:events :llm-control]))
(defn decision-event-id [decision] (:event/id decision))
(defn event-id [event] (:event/id event))

(defn base-event
  [request event-type suffix]
  {:event/id (event-id-for request suffix)
   :event/type event-type
   :event/schema-version schema-version
   :event/time-ms (:request/time-ms request)
   :request/id (:request/id request)
   :routing/key (:routing/key request)
   :world-thread/id (request-thread-id request)
   :actor (:actor request)
   :branch (:branch request)
   :causal (merge {:parents []
                   :correlation/id (:request/id request)}
                  (:causal request))
   :provenance {:source/type :action-request
                :source/ref (:request/id request)}})

(defn world-thread-event
  [request existing-thread]
  (let [parent-id (or (get-in request [:payload :parent-thread/id])
                      (:parent-thread/id existing-thread))
        parent-ids (or (get-in request [:payload :parent-thread/ids])
                       (:parent-thread/ids existing-thread)
                       (when parent-id [parent-id])
                       [])]
    (merge (base-event request
                       (if existing-thread
                         :world-thread/touched
                         :world-thread/created)
                       "world-thread")
           {:world-thread/id (request-thread-id request)
            :title (or (get-in request [:payload :title])
                       (:title existing-thread)
                       (request-thread-id request))
            :parent-thread/id parent-id
            :parent-thread/ids (vec parent-ids)})))

(defn world-turn-event
  [request]
  (let [request-type (:request/type request)
        turn-kind (if (= :world-turn/compose-and-send request-type)
                    :compose-and-send
                    (or (get-in request [:payload :world-turn/kind])
                        request-type))]
    (merge (base-event request :world-turn/created "world-turn")
           {:world-turn/id (request-turn-id request)
            :world-turn/kind turn-kind
            :prompt/text (get-in request [:payload :prompt/text])
            :refs (vec (or (get-in request [:payload :refs]) []))
            :draft/id (get-in request [:payload :draft/id])})))

(def bundle-option-keys
  [:agent/kind :model :approval-policy :sandbox :cwd
   :model/reasoning-effort :model/verbosity :system/prompt])

(defn bundle-execution-options
  [request]
  (let [opts (merge (get-in request [:payload :llm/options])
                    (get-in request [:payload :execution/options]))]
    (select-keys opts bundle-option-keys)))

(defn render-ref
  [ref]
  (cond
    (:object/id ref) (str "object:" (:object/id ref))
    (:slice/id ref) (str "slice:" (:slice/id ref))
    (:world-turn/id ref) (str "world-turn:" (:world-turn/id ref))
    :else (pr-str ref)))

(defn render-model-input
  [request]
  (let [system-prompt (or (get-in request [:payload :system/prompt])
                          (get-in request [:payload :llm/options :system/prompt])
                          (get-in request [:payload :execution/options :system/prompt]))
        prompt-text (get-in request [:payload :prompt/text])
        refs (vec (or (get-in request [:payload :refs]) []))]
    (str (when-not (blank-string? system-prompt)
           (str system-prompt "\n\n"))
         prompt-text
         (when (seq refs)
           (str "\n\nRefs:\n"
                (str/join "\n" (map #(str "- " (render-ref %)) refs)))))))

(defn context-bundle-row
  [request turn-event]
  (let [rendered (render-model-input request)
        execution-options (bundle-execution-options request)
        stable-material {:world-thread/id (request-thread-id request)
                         :world-turn/id (:world-turn/id turn-event)
                         :prompt/text (get-in request [:payload :prompt/text])
                         :refs (vec (or (get-in request [:payload :refs]) []))
                         :execution/options execution-options
                         :rendered/model-input rendered}]
    (assoc stable-material
           :context-bundle/id (request-bundle-id request)
           :request/id (:request/id request)
           :bundle/schema-version schema-version
           :context-bundle/hash (str "sha256:" (kernel/sha-256 (pr-str stable-material)))
           :created-at-ms (:request/time-ms request))))

(defn context-bundle-event
  [request turn-event bundle]
  (merge (base-event request :context-bundle/frozen "context-bundle")
         {:world-turn/id (:world-turn/id turn-event)
          :context-bundle/id (:context-bundle/id bundle)
          :context-bundle/hash (:context-bundle/hash bundle)}))

(defn request-llm-thread-id
  [request]
  (or (get-in request [:payload :llm-thread/id])
      (:llm-thread/id request)
      (str "llm-thread:" (request-thread-id request))))

(defn request-llm-run-id
  [request]
  (or (get-in request [:payload :llm-turn-run/id])
      (:llm-turn-run/id request)
      (str (:request/id request) "/llm-run")))

(defn request-llm-request-id
  [request]
  (or (get-in request [:payload :llm-request/id])
      (:llm-request/id request)
      (str (:request/id request) "/llm-request")))

(defn world->llm-turn-run-request
  [request turn-event bundle]
  (let [run-id (request-llm-run-id request)
        llm-thread-id (request-llm-thread-id request)
        executor-task-id (or (get-in request [:payload :executor/task-id])
                             (get-in request [:payload :llm/options :executor/task-id])
                             (get-in request [:payload :execution/options :executor/task-id]))]
    (llm/turn-run-request
      (request-thread-id request)
      (:world-turn/id turn-event)
      (:context-bundle/id bundle)
      (cond-> {:llm-turn-run-id run-id
               :llm-thread-id llm-thread-id
               :request-id (request-llm-request-id request)
               :time-ms (:request/time-ms request)
               :idempotency-key (str "world-event:" (:request/id request) ":"
                                     (:world-turn/id turn-event) ":"
                                     (:context-bundle/id bundle))
               :agent-kind (or (get-in bundle [:execution/options :agent/kind])
                               :codex)
               :native/thread-id (get-in request [:payload :native/codex-thread-id])
               :executor-pool (or (get-in request [:payload :executor/pool])
                                  :local-codex)
               :executor-hints (or (get-in request [:payload :executor/hints])
                                   {:interactive? true})}
        executor-task-id (assoc :executor-task-id executor-task-id)))))

(defn llm-run-requested-event
  [request turn-event bundle llm-request]
  (merge (base-event request :llm-turn-run/requested "llm-turn-run")
         {:world-turn/id (:world-turn/id turn-event)
          :context-bundle/id (:context-bundle/id bundle)
          :context-bundle/hash (:context-bundle/hash bundle)
          :llm-thread/id (:llm-thread/id llm-request)
          :llm-turn-run/id (:llm-turn-run/id llm-request)
          :llm/request-id (:request/id llm-request)
          :llm/request llm-request}))

(defn llm-run-id-from-request
  [llm-request]
  (:llm-turn-run/id llm-request))

(defn request-control-type
  [request]
  (case (:request/type request)
    :world-turn/tool-approval-resolve :approval/resolve
    :world-turn/cancel :turn/cancel
    :world-turn/compact-request :compact/request
    :world-turn/steer :turn/steer))

(defn request-control-id
  [request]
  (or (get-in request [:payload :control/id])
      (:control/id request)
      (str (:request/id request) "/llm-control")))

(defn request-control-run-id
  [request]
  (or (get-in request [:payload :llm-turn-run/id])
      (:llm-turn-run/id request)))

(defn world->llm-control-record
  [request turn-event]
  (llm/control-record
    (request-control-run-id request)
    (request-control-type request)
    {:control-id (request-control-id request)
     :llm-thread/id (get-in request [:payload :llm-thread/id])
     :world-thread/id (request-thread-id request)
     :world-turn/id (:world-turn/id turn-event)
     :approval/id (get-in request [:payload :approval/id])
     :native/json-rpc-request-id (get-in request [:payload :native/json-rpc-request-id])
     :decision (get-in request [:payload :decision])
     :actor (:actor request)
     :time-ms (:request/time-ms request)
     :reason (get-in request [:payload :reason])
     :payload (:payload request)}))

(defn llm-control-event
  [request turn-event control]
  (merge (base-event request :llm-control/requested "llm-control")
         {:world-turn/id (:world-turn/id turn-event)
          :llm-control/id (:control/id control)
          :llm-control/type (:control/type control)
          :llm-turn-run/id (:llm-turn-run/id control)
          :approval/id (:approval/id control)
          :control control}))

(defn llm-control-run-id
  [control]
  (:llm-turn-run/id control))

(defn llm-control-id
  [control]
  (:control/id control))

(defn validate-or-reject
  [request]
  (let [errors (request-validation-errors request)]
    (when (seq errors)
      (rejected-decision request :request-invalid errors))))

(defn interpret-thread-create
  [request existing-thread]
  (or (validate-or-reject request)
      (let [thread-event (world-thread-event request existing-thread)]
        (accepted-decision request thread-event {:thread thread-event}))))

(defn interpret-compose-and-send
  [request existing-thread]
  (or (validate-or-reject request)
      (let [thread-event (world-thread-event request existing-thread)
            turn-event (world-turn-event request)
            bundle (context-bundle-row request turn-event)
            bundle-event (context-bundle-event request turn-event bundle)
            llm-request (world->llm-turn-run-request request turn-event bundle)
            llm-run-event (llm-run-requested-event request turn-event bundle llm-request)]
        (accepted-decision request
                           turn-event
                           {:thread thread-event
                            :turn turn-event
                            :context-bundle bundle-event
                            :llm-turn-run llm-run-event}))))

(defn dedupe-row
  [request decision bundle llm-request]
  {:idempotency/key (:idempotency/key request)
   :request/id (:request/id request)
   :decision/status (:decision/status decision)
   :routing/key (:routing/key request)
   :event/id (:event/id decision)
   :event/ids (:event/ids decision)
   :events (:events decision)
   :world-thread/id (request-thread-id request)
   :world-turn/id (get-in decision [:events :turn :world-turn/id])
   :context-bundle/id (:context-bundle/id bundle)
   :context-bundle/hash (:context-bundle/hash bundle)
   :llm-thread/id (:llm-thread/id llm-request)
   :llm-turn-run/id (:llm-turn-run/id llm-request)
   :llm/request-id (:request/id llm-request)
   :llm/request llm-request})

(defn idempotency-hit? [row] (some? row))

(defn idempotent-decision
  [request existing-row]
  {:decision/id (decision-id-for-request-id (:request/id request))
   :decision/status (:decision/status existing-row)
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id (:event/id existing-row)
   :event/ids (:event/ids existing-row)
   :events (:events existing-row)
   :world-thread/id (:world-thread/id existing-row)
   :world-turn/id (:world-turn/id existing-row)
   :context-bundle/id (:context-bundle/id existing-row)
   :context-bundle/hash (:context-bundle/hash existing-row)
   :llm-thread/id (:llm-thread/id existing-row)
   :llm-turn-run/id (:llm-turn-run/id existing-row)
   :llm/request-id (:llm/request-id existing-row)
   :idempotency/key (:idempotency/key request)
   :idempotency/replayed? true
   :idempotency/original-request-id (:request/id existing-row)
   :decided-at-ms (:request/time-ms request)})

(defn interpret-world-only-turn
  [request existing-thread]
  (if-let [rejection (validate-or-reject request)]
    rejection
    (if (nil? existing-thread)
      (rejected-decision request :world-thread/not-found)
      (let [turn-event (world-turn-event request)]
        (accepted-decision request turn-event {:turn turn-event})))))

(defn interpret-control-turn
  [request existing-thread]
  (cond
    (validate-or-reject request)
    (validate-or-reject request)

    (nil? existing-thread)
    (rejected-decision request :world-thread/not-found)

    (blank-string? (request-control-run-id request))
    (rejected-decision request :llm-turn-run/id-invalid)

    (and (= :world-turn/tool-approval-resolve (:request/type request))
         (blank-string? (get-in request [:payload :approval/id])))
    (rejected-decision request :approval/id-invalid)

    :else
    (let [turn-event (world-turn-event request)
          control (world->llm-control-record request turn-event)
          control-event (llm-control-event request turn-event control)]
      (accepted-decision request turn-event {:turn turn-event
                                             :llm-control control-event}))))

(defn thread-row
  [existing-thread thread-event]
  (let [time-ms (:event/time-ms thread-event)]
    (-> (or existing-thread
            {:world-thread/id (:world-thread/id thread-event)
             :created-at-ms time-ms
             :turn-count 0})
        (assoc :world-thread/id (:world-thread/id thread-event)
               :title (:title thread-event)
               :status :active
               :updated-at-ms time-ms
               :parent-thread/id (:parent-thread/id thread-event)
               :parent-thread/ids (vec (:parent-thread/ids thread-event))))))

(defn turn-row
  [turn-event context-bundle-id]
  (cond-> {:world-turn/id (:world-turn/id turn-event)
           :world-thread/id (:world-thread/id turn-event)
           :world-turn/kind (:world-turn/kind turn-event)
           :request/id (:request/id turn-event)
           :status :accepted
           :prompt/text (:prompt/text turn-event)
           :refs (vec (or (:refs turn-event) []))
           :draft/id (:draft/id turn-event)
           :created-at-ms (:event/time-ms turn-event)
           :updated-at-ms (:event/time-ms turn-event)}
    context-bundle-id (assoc :context-bundle/id context-bundle-id)))

(defn context-bundle-id-from-event
  [bundle-event]
  (:context-bundle/id bundle-event))

(defn turn-id-from-event
  [turn-event]
  (:world-turn/id turn-event))

(defn world-thread-id-from-event
  [event]
  (:world-thread/id event))

(defn conj-distinct
  [xs x]
  (let [v (vec xs)]
    (if (some #{x} v)
      v
      (conj v x))))

(defn add-turn-id
  [turn-order turn-id]
  (conj-distinct turn-order turn-id))

(defn bump-thread-turn-count
  [thread-row turn-order]
  (assoc thread-row :turn-count (count turn-order)))

(defmodule world-module [setup topologies]
  (mirror-depot setup *llm-depot (get-module-name llm/llm-module) "*llm-depot")
  (mirror-depot setup *llm-control-depot (get-module-name llm/llm-module) "*llm-control-depot")
  (declare-depot setup *world-action-depot (hash-by :routing/key))
  (let [n (stream-topology topologies "world-chat-topology")]
    (declare-pstate n $$world-requests-by-id {String Object})
    (declare-pstate n $$world-decisions-by-id {String Object})
    (declare-pstate n $$world-events-by-id {String Object})
    (declare-pstate n $$world-threads {String Object})
    (declare-pstate n $$world-thread-graph {String Object})
    (declare-pstate n $$world-turns {String Object})
    (declare-pstate n $$world-turns-by-thread {String Object})
    (declare-pstate n $$context-bundles {String Object})
    (declare-pstate n $$context-bundles-by-turn {String String})
    (declare-pstate n $$world-send-by-idempotency {String Object})
    (declare-pstate n $$world-llm-run-requests {String Object})
    (declare-pstate n $$world-llm-run-by-turn {String String})
    (declare-pstate n $$world-llm-controls {String Object})
    (declare-pstate n $$world-llm-control-by-turn {String String})

    (<<sources n
      (source> *world-action-depot :> *request)
      (request-id *request :> *request-id)
      (request-type *request :> *request-type)
      (request-thread-id *request :> *thread-id)
      (|hash *request-id)
      (local-transform> [(keypath *request-id) (termval *request)] $$world-requests-by-id)
      (|hash *thread-id)
      (local-select> [(keypath *thread-id)] $$world-threads :> *existing-thread)

      (<<cond
        (case> (= :world-thread/create *request-type))
        (interpret-thread-create *request *existing-thread :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$world-decisions-by-id)
        (<<if (decision-accepted? *decision)
          (decision-thread-event *decision :> *thread-event)
          (event-id *thread-event :> *event-id)
          (thread-row *existing-thread *thread-event :> *thread-row)
          (world-thread-id-from-event *thread-event :> *event-thread-id)
          (|hash *event-id)
          (local-transform> [(keypath *event-id) (termval *thread-event)] $$world-events-by-id)
          (|hash *event-thread-id)
          (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads))

        (case> (= :world-turn/compose-and-send *request-type))
        (request-idempotency-key *request :> *idempotency-key)
        (|hash *idempotency-key)
        (local-select> [(keypath *idempotency-key)] $$world-send-by-idempotency :> *idempotency-row)
        (<<if (idempotency-hit? *idempotency-row)
          (idempotent-decision *request *idempotency-row :> *decision)
          (decision-id *decision :> *decision-id)
          (|hash *decision-id)
          (local-transform> [(keypath *decision-id) (termval *decision)] $$world-decisions-by-id))
        (<<if (not (idempotency-hit? *idempotency-row))
          (interpret-compose-and-send *request *existing-thread :> *decision)
          (decision-id *decision :> *decision-id)
          (|hash *decision-id)
          (local-transform> [(keypath *decision-id) (termval *decision)] $$world-decisions-by-id)
          (<<if (decision-accepted? *decision)
            (decision-thread-event *decision :> *thread-event)
            (decision-turn-event *decision :> *turn-event)
            (decision-bundle-event *decision :> *bundle-event)
            (decision-llm-run-event *decision :> *llm-run-event)
            (event-id *thread-event :> *thread-event-id)
            (event-id *turn-event :> *turn-event-id)
            (event-id *bundle-event :> *bundle-event-id)
            (event-id *llm-run-event :> *llm-run-event-id)
            (world-thread-id-from-event *thread-event :> *event-thread-id)
            (turn-id-from-event *turn-event :> *turn-id)
            (context-bundle-id-from-event *bundle-event :> *bundle-id)
            (context-bundle-row *request *turn-event :> *bundle)
            (world->llm-turn-run-request *request *turn-event *bundle :> *llm-request)
            (llm-run-id-from-request *llm-request :> *llm-run-id)
            (thread-row *existing-thread *thread-event :> *base-thread-row)
            (turn-row *turn-event *bundle-id :> *turn-row)
            (dedupe-row *request *decision *bundle *llm-request :> *dedupe-row)
            (|hash *thread-event-id)
            (local-transform> [(keypath *thread-event-id) (termval *thread-event)] $$world-events-by-id)
            (|hash *turn-event-id)
            (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$world-events-by-id)
            (|hash *bundle-event-id)
            (local-transform> [(keypath *bundle-event-id) (termval *bundle-event)] $$world-events-by-id)
            (|hash *llm-run-event-id)
            (local-transform> [(keypath *llm-run-event-id) (termval *llm-run-event)] $$world-events-by-id)
            (|hash *event-thread-id)
            (local-select> [(keypath *event-thread-id)] $$world-turns-by-thread :> *existing-turn-order)
            (add-turn-id *existing-turn-order *turn-id :> *turn-order)
            (bump-thread-turn-count *base-thread-row *turn-order :> *thread-row)
            (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
            (local-transform> [(keypath *event-thread-id) (termval *turn-order)] $$world-turns-by-thread)
            (|hash *turn-id)
            (local-transform> [(keypath *turn-id) (termval *turn-row)] $$world-turns)
            (local-transform> [(keypath *turn-id) (termval *bundle-id)] $$context-bundles-by-turn)
            (local-transform> [(keypath *turn-id) (termval *llm-run-id)] $$world-llm-run-by-turn)
            (|hash *bundle-id)
            (local-transform> [(keypath *bundle-id) (termval *bundle)] $$context-bundles)
            (|hash *llm-run-id)
            (local-transform> [(keypath *llm-run-id) (termval *llm-request)] $$world-llm-run-requests)
            (|hash *idempotency-key)
            (local-transform> [(keypath *idempotency-key) (termval *dedupe-row)] $$world-send-by-idempotency)
            (|hash$$ *llm-depot *llm-run-id)
            (depot-partition-append! *llm-depot *llm-request :append-ack)))

        (case> (world-control-request? *request-type))
        (interpret-control-turn *request *existing-thread :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$world-decisions-by-id)
        (<<if (decision-accepted? *decision)
          (decision-turn-event *decision :> *turn-event)
          (decision-llm-control-event *decision :> *control-event)
          (event-id *turn-event :> *turn-event-id)
          (event-id *control-event :> *control-event-id)
          (world-thread-id-from-event *turn-event :> *event-thread-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (world->llm-control-record *request *turn-event :> *control)
          (llm-control-run-id *control :> *control-run-id)
          (llm-control-id *control :> *control-id)
          (turn-row *turn-event nil :> *turn-row)
          (|hash *event-thread-id)
          (local-select> [(keypath *event-thread-id)] $$world-turns-by-thread :> *existing-turn-order)
          (add-turn-id *existing-turn-order *turn-id :> *turn-order)
          (world-thread-event *request *existing-thread :> *thread-event)
          (thread-row *existing-thread *thread-event :> *base-thread-row)
          (bump-thread-turn-count *base-thread-row *turn-order :> *thread-row)
          (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
          (local-transform> [(keypath *event-thread-id) (termval *turn-order)] $$world-turns-by-thread)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$world-events-by-id)
          (|hash *control-event-id)
          (local-transform> [(keypath *control-event-id) (termval *control-event)] $$world-events-by-id)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$world-turns)
          (local-transform> [(keypath *turn-id) (termval *control-id)] $$world-llm-control-by-turn)
          (|hash *control-id)
          (local-transform> [(keypath *control-id) (termval *control)] $$world-llm-controls)
          (|hash$$ *llm-control-depot *control-run-id)
          (depot-partition-append! *llm-control-depot *control :append-ack))

        (case> (world-only-turn-request? *request-type))
        (interpret-world-only-turn *request *existing-thread :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$world-decisions-by-id)
        (<<if (decision-accepted? *decision)
          (decision-turn-event *decision :> *turn-event)
          (event-id *turn-event :> *turn-event-id)
          (world-thread-id-from-event *turn-event :> *event-thread-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (turn-row *turn-event nil :> *turn-row)
          (|hash *event-thread-id)
          (local-select> [(keypath *event-thread-id)] $$world-turns-by-thread :> *existing-turn-order)
          (add-turn-id *existing-turn-order *turn-id :> *turn-order)
          (world-thread-event *request *existing-thread :> *thread-event)
          (thread-row *existing-thread *thread-event :> *base-thread-row)
          (bump-thread-turn-count *base-thread-row *turn-order :> *thread-row)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$world-events-by-id)
          (|hash *event-thread-id)
          (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
          (local-transform> [(keypath *event-thread-id) (termval *turn-order)] $$world-turns-by-thread)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$world-turns))

        (default>)
        (rejected-decision *request :request/type-invalid :> *decision)
        (decision-id *decision :> *decision-id)
        (|hash *decision-id)
        (local-transform> [(keypath *decision-id) (termval *decision)] $$world-decisions-by-id)))))

(defn start-world-runtime!
  []
  (let [ipc (create-ipc)
        llm-module-name (get-module-name llm/llm-module)
        module-name (get-module-name world-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc llm/llm-module launch-opts)
    (launch-module! ipc world-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :llm-module-name llm-module-name
     :world-action-depot (foreign-depot ipc module-name "*world-action-depot")
     :world-requests-by-id (foreign-pstate ipc module-name "$$world-requests-by-id")
     :world-decisions-by-id (foreign-pstate ipc module-name "$$world-decisions-by-id")
     :world-events-by-id (foreign-pstate ipc module-name "$$world-events-by-id")
     :world-threads (foreign-pstate ipc module-name "$$world-threads")
     :world-thread-graph (foreign-pstate ipc module-name "$$world-thread-graph")
     :world-turns (foreign-pstate ipc module-name "$$world-turns")
     :world-turns-by-thread (foreign-pstate ipc module-name "$$world-turns-by-thread")
     :context-bundles (foreign-pstate ipc module-name "$$context-bundles")
     :context-bundles-by-turn (foreign-pstate ipc module-name "$$context-bundles-by-turn")
     :world-send-by-idempotency (foreign-pstate ipc module-name "$$world-send-by-idempotency")
     :world-llm-run-requests (foreign-pstate ipc module-name "$$world-llm-run-requests")
     :world-llm-run-by-turn (foreign-pstate ipc module-name "$$world-llm-run-by-turn")
     :world-llm-controls (foreign-pstate ipc module-name "$$world-llm-controls")
     :world-llm-control-by-turn (foreign-pstate ipc module-name "$$world-llm-control-by-turn")
     :llm-depot (foreign-depot ipc llm-module-name "*llm-depot")
     :llm-claim-depot (foreign-depot ipc llm-module-name "*llm-claim-depot")
     :llm-obs-depot (foreign-depot ipc llm-module-name "*llm-obs-depot")
     :llm-control-depot (foreign-depot ipc llm-module-name "*llm-control-depot")
     :llm-threads (foreign-pstate ipc llm-module-name "$$llm-threads")
     :llm-thread-by-world-thread (foreign-pstate ipc llm-module-name "$$llm-thread-by-world-thread")
     :llm-turn-runs (foreign-pstate ipc llm-module-name "$$llm-turn-runs")
     :llm-turn-runs-by-thread (foreign-pstate ipc llm-module-name "$$llm-turn-runs-by-thread")
     :llm-turn-run-by-world-turn (foreign-pstate ipc llm-module-name "$$llm-turn-run-by-world-turn")
     :llm-decisions-by-run-id (foreign-pstate ipc llm-module-name "$$llm-decisions-by-run-id")
     :llm-pending-by-task (foreign-pstate ipc llm-module-name "$$llm-pending-by-task")
     :llm-items-by-turn-run (foreign-pstate ipc llm-module-name "$$llm-items-by-turn-run")
     :llm-items-by-thread (foreign-pstate ipc llm-module-name "$$llm-items-by-thread")
     :llm-item-by-id (foreign-pstate ipc llm-module-name "$$llm-item-by-id")
     :llm-raw-response-items (foreign-pstate ipc llm-module-name "$$llm-raw-response-items")
     :llm-tool-calls-by-run-id (foreign-pstate ipc llm-module-name "$$llm-tool-calls-by-run-id")
     :llm-approvals-pending (foreign-pstate ipc llm-module-name "$$llm-approvals-pending")
     :llm-approvals-by-run-id (foreign-pstate ipc llm-module-name "$$llm-approvals-by-run-id")
     :llm-token-usage-by-run-id (foreign-pstate ipc llm-module-name "$$llm-token-usage-by-run-id")
     :llm-controls-by-run-id (foreign-pstate ipc llm-module-name "$$llm-controls-by-run-id")
     :llm-control-by-id (foreign-pstate ipc llm-module-name "$$llm-control-by-id")
     :llm-views (foreign-pstate ipc llm-module-name "$$llm-views")}))

(defn close-world-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-world-action!
  ([runtime request]
   (append-world-action! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:world-action-depot runtime) request ack-level)
   request))

(defn select-pstate-one
  [pstate path]
  (first (foreign-select path pstate)))

(defn read-request
  [runtime request-id]
  (select-pstate-one (:world-requests-by-id runtime) [(keypath request-id)]))

(defn read-decision
  [runtime request-id]
  (select-pstate-one (:world-decisions-by-id runtime)
                     [(keypath (decision-id-for-request-id request-id))]))

(defn read-event
  [runtime event-id]
  (select-pstate-one (:world-events-by-id runtime) [(keypath event-id)]))

(defn read-thread
  [runtime world-thread-id]
  (select-pstate-one (:world-threads runtime) [(keypath world-thread-id)]))

(defn read-turn
  [runtime world-turn-id]
  (select-pstate-one (:world-turns runtime) [(keypath world-turn-id)]))

(defn read-turns-by-thread
  [runtime world-thread-id]
  (or (select-pstate-one (:world-turns-by-thread runtime) [(keypath world-thread-id)])
      []))

(defn read-context-bundle
  [runtime context-bundle-id]
  (select-pstate-one (:context-bundles runtime) [(keypath context-bundle-id)]))

(defn read-context-bundle-by-turn
  [runtime world-turn-id]
  (select-pstate-one (:context-bundles-by-turn runtime) [(keypath world-turn-id)]))

(defn read-send-by-idempotency
  [runtime idempotency-key]
  (select-pstate-one (:world-send-by-idempotency runtime) [(keypath idempotency-key)]))

(defn read-llm-run-request
  [runtime llm-turn-run-id]
  (select-pstate-one (:world-llm-run-requests runtime) [(keypath llm-turn-run-id)]))

(defn read-llm-run-by-turn
  [runtime world-turn-id]
  (select-pstate-one (:world-llm-run-by-turn runtime) [(keypath world-turn-id)]))

(defn read-llm-control
  [runtime control-id]
  (select-pstate-one (:world-llm-controls runtime) [(keypath control-id)]))

(defn read-llm-control-by-turn
  [runtime world-turn-id]
  (select-pstate-one (:world-llm-control-by-turn runtime) [(keypath world-turn-id)]))

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
  [runtime request-id]
  (await-materialized #(read-decision runtime request-id) some?))

(defn await-thread
  [runtime world-thread-id pred]
  (await-materialized #(read-thread runtime world-thread-id) pred))

(defn await-turn
  [runtime world-turn-id pred]
  (await-materialized #(read-turn runtime world-turn-id) pred))
