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
    :world-turn/patch-proposal-create
    :world-turn/patch-accept
    :world-turn/patch-reject
    :world-turn/tool-approval-resolve
    :world-turn/cancel
    :world-turn/compact-request
    :world-turn/steer
    :world-turn/abandon})

(def world-thread-request-types
  #{:world-thread/create
    :world-thread/fork-from-span
    :world-thread/reconcile})

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
    :world-turn/patch-proposal-create :world-patch/ingest
    :world-turn/patch-accept :world-turn/write
    :world-turn/patch-reject :world-turn/write
    :world-turn/tool-approval-resolve :llm/control
    :world-turn/cancel :world-turn/control
    :world-turn/compact-request :llm/control
    :world-turn/steer :llm/control
    :world-turn/abandon :world-turn/control
    :world-thread/fork-from-span :world-thread/fork
    :world-thread/reconcile :world-thread/reconcile
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
           (not (or (contains? world-thread-request-types request-type)
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
    (:derivative/id ref) (str "user-authored-derivative:" (:derivative/id ref))
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
               :llm/backend (or (get-in bundle [:execution/options :llm/backend])
                                (get-in bundle [:execution/options :agent/kind])
                                :codex)
               :llm/auth-mode (get-in bundle [:execution/options :llm/auth-mode])
               :native/thread-id (get-in request [:payload :native/codex-thread-id])
               :fork/from-native-thread-id (get-in request [:payload :fork/from-native-thread-id])
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

(defn interpret-fork-from-span
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

(defn keep-existing-slice-row
  [existing slice]
  (or existing slice))

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

(defn parent-thread-id-from-event
  [event]
  (:parent-thread/id event))

(defn has-parent-thread?
  [thread-event]
  (not (blank-string? (:parent-thread/id thread-event))))

(defn child-thread-edge
  [thread-event]
  {:edge/type :world-thread/child
   :parent-thread/id (:parent-thread/id thread-event)
   :child-thread/id (:world-thread/id thread-event)
   :created-at-ms (:event/time-ms thread-event)
   :request/id (:request/id thread-event)})

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

(defn catalog-object-id
  [object-type source-id]
  (str (name object-type) ":" source-id))

(defn object-row-id
  [object-row]
  (:object/id object-row))

(defn world-thread-object-row
  [thread-row]
  {:object/id (catalog-object-id :world-thread (:world-thread/id thread-row))
   :object/type :world-thread
   :source/type :world-thread
   :source/id (:world-thread/id thread-row)
   :world-thread/id (:world-thread/id thread-row)
   :title (:title thread-row)
   :status (:status thread-row)
   :turn-count (:turn-count thread-row)
   :created-at-ms (:created-at-ms thread-row)
   :updated-at-ms (:updated-at-ms thread-row)})

(defn world-turn-object-row
  [turn-row]
  {:object/id (catalog-object-id :world-turn (:world-turn/id turn-row))
   :object/type :world-turn
   :source/type :world-turn
   :source/id (:world-turn/id turn-row)
   :world-thread/id (:world-thread/id turn-row)
   :world-turn/id (:world-turn/id turn-row)
   :world-turn/kind (:world-turn/kind turn-row)
   :context-bundle/id (:context-bundle/id turn-row)
   :created-at-ms (:created-at-ms turn-row)
   :updated-at-ms (:updated-at-ms turn-row)})

(defn context-bundle-object-row
  [bundle]
  {:object/id (catalog-object-id :context-bundle (:context-bundle/id bundle))
   :object/type :context-bundle
   :source/type :context-bundle
   :source/id (:context-bundle/id bundle)
   :world-thread/id (:world-thread/id bundle)
   :world-turn/id (:world-turn/id bundle)
   :context-bundle/id (:context-bundle/id bundle)
   :context-bundle/hash (:context-bundle/hash bundle)
   :created-at-ms (:created-at-ms bundle)
   :updated-at-ms (:created-at-ms bundle)})

(defn llm-turn-run-object-row
  [llm-request]
  {:object/id (catalog-object-id :llm-turn-run (:llm-turn-run/id llm-request))
   :object/type :llm-turn-run
   :source/type :llm-turn-run
   :source/id (:llm-turn-run/id llm-request)
   :world-thread/id (:world-thread/id llm-request)
   :world-turn/id (:world-turn/id llm-request)
   :context-bundle/id (:context-bundle/id llm-request)
   :llm-thread/id (:llm-thread/id llm-request)
   :llm-turn-run/id (:llm-turn-run/id llm-request)
   :llm/request-id (:request/id llm-request)
   :status :requested
   :created-at-ms (:request/time-ms llm-request)
   :updated-at-ms (:request/time-ms llm-request)})

(defn artifact-edge
  [from-object-id relation to-object-id time-ms request-id]
  {:artifact-edge/id (str from-object-id "->" (name relation) "->" to-object-id)
   :artifact-edge/type relation
   :from/object-id from-object-id
   :to/object-id to-object-id
   :request/id request-id
   :created-at-ms time-ms})

(defn artifact-edge-id
  [edge]
  (:artifact-edge/id edge))

(defn artifact-edge-from
  [edge]
  (:from/object-id edge))

(defn artifact-edge-to
  [edge]
  (:to/object-id edge))

(defn thread-turn-edge
  [thread-row turn-row]
  (artifact-edge
    (catalog-object-id :world-thread (:world-thread/id thread-row))
    :contains
    (catalog-object-id :world-turn (:world-turn/id turn-row))
    (:created-at-ms turn-row)
    (:request/id turn-row)))

(defn turn-bundle-edge
  [turn-row bundle]
  (artifact-edge
    (catalog-object-id :world-turn (:world-turn/id turn-row))
    :freezes-context
    (catalog-object-id :context-bundle (:context-bundle/id bundle))
    (:created-at-ms bundle)
    (:request/id turn-row)))

(defn turn-llm-run-edge
  [turn-row llm-request]
  (artifact-edge
    (catalog-object-id :world-turn (:world-turn/id turn-row))
    :requests-run
    (catalog-object-id :llm-turn-run (:llm-turn-run/id llm-request))
    (:request/time-ms llm-request)
    (:request/id turn-row)))

(defn bundle-llm-run-edge
  [bundle llm-request]
  (artifact-edge
    (catalog-object-id :context-bundle (:context-bundle/id bundle))
    :feeds-run
    (catalog-object-id :llm-turn-run (:llm-turn-run/id llm-request))
    (:request/time-ms llm-request)
    (:request/id llm-request)))

(defn content-hash
  [text]
  (str "sha256:" (kernel/sha-256 (or text ""))))

(defn material-request?
  [request-type]
  (contains? #{:world-turn/slice-create
               :world-turn/comment-create
               :world-turn/derivative-create}
             request-type))

(defn request-source
  [request]
  (or (get-in request [:payload :source])
      (get-in request [:payload :target])
      (first (get-in request [:payload :refs]))))

(defn raw-llm-source?
  [request]
  (not (blank-string? (get (request-source request) :llm-item/id))))

(defn source-content-text
  [request]
  (or (get-in request [:payload :source :content/text])
      (get-in request [:payload :source :snapshot/text])
      (get-in request [:payload :source :text])
      (get-in request [:payload :content/text])
      (get-in request [:payload :prompt/text])
      ""))

(defn source-content-hash
  [request]
  (or (get-in request [:payload :source :content/hash])
      (get-in request [:payload :source :snapshot/hash])
      (content-hash (source-content-text request))))

(defn request-slice-id
  [request]
  (or (get-in request [:payload :slice/id])
      (:slice/id request)
      (str (:request/id request) "/slice")))

(defn request-overlay-id
  [request]
  (or (get-in request [:payload :overlay/id])
      (:overlay/id request)
      (str (:request/id request) "/overlay")))

(defn request-derivative-id
  [request]
  (or (get-in request [:payload :derivative/id])
      (:derivative/id request)
      (str (:request/id request) "/derivative")))

(defn slice-row
  [request turn-event]
  (let [snapshot-text (or (get-in request [:payload :slice/snapshot-text])
                          (get-in request [:payload :snapshot/text])
                          (get-in request [:payload :content/text])
                          (source-content-text request))]
    {:slice/id (request-slice-id request)
     :world-thread/id (request-thread-id request)
     :world-turn/id (:world-turn/id turn-event)
     :source (request-source request)
     :source/content-hash (source-content-hash request)
     :snapshot/text snapshot-text
     :snapshot/hash (content-hash snapshot-text)
     :created-at-ms (:event/time-ms turn-event)
     :created-by (:actor request)}))

(defn overlay-row
  [request turn-event]
  {:overlay/id (request-overlay-id request)
   :overlay/type :comment
   :world-thread/id (request-thread-id request)
   :world-turn/id (:world-turn/id turn-event)
   :target (request-source request)
   :body/text (get-in request [:payload :prompt/text])
   :created-at-ms (:event/time-ms turn-event)
   :created-by (:actor request)})

(defn derivative-row
  [request turn-event]
  (let [text (or (get-in request [:payload :content/text])
                 (get-in request [:payload :prompt/text])
                 "")]
    {:derivative/id (request-derivative-id request)
     :world-thread/id (request-thread-id request)
     :world-turn/id (:world-turn/id turn-event)
     :source (request-source request)
     :source/content-hash (source-content-hash request)
     :content/text text
     :content/hash (content-hash text)
     :authorship :user
     :render/as :user-authored
     :created-at-ms (:event/time-ms turn-event)
     :created-by (:actor request)}))

(defn slice-row-id [slice] (:slice/id slice))
(defn overlay-row-id [overlay] (:overlay/id overlay))
(defn derivative-row-id [derivative] (:derivative/id derivative))

(defn raw-llm-item-object-row
  [request]
  (let [source (request-source request)
        item-id (:llm-item/id source)]
    {:object/id (catalog-object-id :llm-item item-id)
     :object/type :llm-item
     :source/type :llm-item
     :source/id item-id
     :llm-item/id item-id
     :llm-turn-run/id (:llm-turn-run/id source)
     :llm-thread/id (:llm-thread/id source)
     :content/hash (source-content-hash request)
     :visibility :public-material
     :promoted-at-ms (:request/time-ms request)
     :promoted-by-request/id (:request/id request)}))

(defn slice-object-row
  [slice]
  {:object/id (catalog-object-id :slice (:slice/id slice))
   :object/type :slice
   :source/type :slice
   :source/id (:slice/id slice)
   :slice/id (:slice/id slice)
   :world-thread/id (:world-thread/id slice)
   :world-turn/id (:world-turn/id slice)
   :snapshot/hash (:snapshot/hash slice)
   :source/content-hash (:source/content-hash slice)
   :created-at-ms (:created-at-ms slice)})

(defn overlay-object-row
  [overlay]
  {:object/id (catalog-object-id :overlay (:overlay/id overlay))
   :object/type :overlay
   :source/type :overlay
   :source/id (:overlay/id overlay)
   :overlay/id (:overlay/id overlay)
   :overlay/type (:overlay/type overlay)
   :world-thread/id (:world-thread/id overlay)
   :world-turn/id (:world-turn/id overlay)
   :created-at-ms (:created-at-ms overlay)})

(defn derivative-object-row
  [derivative]
  {:object/id (catalog-object-id :derivative (:derivative/id derivative))
   :object/type :derivative
   :source/type :derivative
   :source/id (:derivative/id derivative)
   :derivative/id (:derivative/id derivative)
   :world-thread/id (:world-thread/id derivative)
   :world-turn/id (:world-turn/id derivative)
   :content/hash (:content/hash derivative)
   :authorship (:authorship derivative)
   :render/as (:render/as derivative)
   :created-at-ms (:created-at-ms derivative)})

(defn source-slice-edge
  [raw-object slice]
  (artifact-edge
    (:object/id raw-object)
    :sliced-into
    (catalog-object-id :slice (:slice/id slice))
    (:created-at-ms slice)
    (:world-turn/id slice)))

(defn source-overlay-edge
  [raw-object overlay]
  (artifact-edge
    (:object/id raw-object)
    :commented-by
    (catalog-object-id :overlay (:overlay/id overlay))
    (:created-at-ms overlay)
    (:world-turn/id overlay)))

(defn source-derivative-edge
  [raw-object derivative]
  (artifact-edge
    (:object/id raw-object)
    :derived-into
    (catalog-object-id :derivative (:derivative/id derivative))
    (:created-at-ms derivative)
    (:world-turn/id derivative)))

(defn patch-proposal-observation?
  [obs]
  (= :codex/patch-proposal (:observation/type obs)))

(defn request-observation
  [request]
  (get-in request [:payload :observation]))

(defn patch-proposal-id-from-observation
  [obs]
  (or (:patch-proposal/id obs)
      (:turn-diff/id obs)
      (:observation/id obs)))

(defn patch-proposal-row
  [obs]
  (let [proposal-id (patch-proposal-id-from-observation obs)]
    {:patch-proposal/id proposal-id
     :turn-diff/id (or (:turn-diff/id obs) proposal-id)
     :llm-turn-run/id (:llm-turn-run/id obs)
     :llm-thread/id (:llm-thread/id obs)
     :world-thread/id (:world-thread/id obs)
     :world-turn/id (:world-turn/id obs)
     :status :pending
     :patch/files (:patch/files obs)
     :summary/text (:summary/text obs)
     :created-at-ms (:received-at-ms obs)
     :observation/id (:observation/id obs)
     :raw/json (:raw/json obs)}))

(defn patch-proposal-row-from-request
  [request]
  {:patch-proposal/id (get-in request [:payload :patch-proposal/id])
   :turn-diff/id (get-in request [:payload :turn-diff/id])
   :llm-turn-run/id (get-in request [:payload :llm-turn-run/id])
   :llm-thread/id (get-in request [:payload :llm-thread/id])
   :world-thread/id (request-thread-id request)
   :world-turn/id (get-in request [:payload :source-world-turn/id])
   :status :pending
   :patch/files (get-in request [:payload :patch/files])
   :summary/text (get-in request [:payload :summary/text])
   :created-at-ms (:request/time-ms request)
   :observation/id (get-in request [:payload :observation/id])
   :raw/json (get-in request [:payload :raw/json])})

(defn patch-decision-request?
  [request-type]
  (contains? #{:world-turn/patch-accept :world-turn/patch-reject} request-type))

(defn request-patch-proposal-id
  [request]
  (or (get-in request [:payload :patch-proposal/id])
      (:patch-proposal/id request)))

(defn patch-decision-status
  [request-type]
  (case request-type
    :world-turn/patch-accept :accepted
    :world-turn/patch-reject :rejected))

(defn apply-patch-decision
  [existing request turn-event]
  (let [proposal-id (request-patch-proposal-id request)
        t (:event/time-ms turn-event)]
    (assoc (or existing {:patch-proposal/id proposal-id})
           :patch-proposal/id proposal-id
           :status (patch-decision-status (:request/type request))
           :resolved-at-ms t
           :resolved-by (:actor request)
           :resolution/world-turn-id (:world-turn/id turn-event)
           :resolution/request-id (:request/id request)
           :reason (get-in request [:payload :reason]))))

(defn turn-projection-row
  [turn-row]
  (select-keys turn-row
               [:world-turn/id :world-thread/id :world-turn/kind
                :context-bundle/id :request/id :prompt/text :refs
                :created-at-ms :updated-at-ms]))

(defn chat-canvas-projection
  [thread-row turn-order latest-turn]
  (cond-> {:projection/type :chat-canvas
           :projection/source :world-canonical-pstates
           :world-thread/id (:world-thread/id thread-row)
           :title (:title thread-row)
           :status (:status thread-row)
           :turn-count (:turn-count thread-row)
           :turn-order (vec (or turn-order []))
           :updated-at-ms (:updated-at-ms thread-row)}
    latest-turn (assoc :latest-turn (turn-projection-row latest-turn))))

(defn object-detail-projection
  [object-row]
  (assoc object-row
         :projection/type :object-detail
         :projection/source :objects))

(defn empty-object-relations-projection
  [object-id]
  {:projection/type :object-relations
   :projection/source :artifact-graph
   :object/id object-id
   :out {}
   :in {}})

(defn add-projection-out-edge
  [existing object-id edge]
  (assoc-in (or existing (empty-object-relations-projection object-id))
            [:out (:artifact-edge/id edge)]
            edge))

(defn add-projection-in-edge
  [existing object-id edge]
  (assoc-in (or existing (empty-object-relations-projection object-id))
            [:in (:artifact-edge/id edge)]
            edge))

(defn turn-object-relations-projection
  [object-id incoming-edge bundle-edge llm-run-edge]
  (-> (empty-object-relations-projection object-id)
      (add-projection-in-edge object-id incoming-edge)
      (add-projection-out-edge object-id bundle-edge)
      (add-projection-out-edge object-id llm-run-edge)))

(defn bundle-object-relations-projection
  [object-id incoming-edge llm-run-edge]
  (-> (empty-object-relations-projection object-id)
      (add-projection-in-edge object-id incoming-edge)
      (add-projection-out-edge object-id llm-run-edge)))

(defn llm-run-object-relations-projection
  [object-id turn-edge bundle-edge]
  (-> (empty-object-relations-projection object-id)
      (add-projection-in-edge object-id turn-edge)
      (add-projection-in-edge object-id bundle-edge)))

(defn one-incoming-relation-projection
  [object-id incoming-edge]
  (add-projection-in-edge nil object-id incoming-edge))

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
    (declare-pstate n $$objects {String Object})
    (declare-pstate n $$artifact-graph {String Object})
    (declare-pstate n $$artifact-graph-in {String Object})
    (declare-pstate n $$slices {String Object})
    (declare-pstate n $$overlays {String Object})
    (declare-pstate n $$derivatives {String Object})
    (declare-pstate n $$world-patch-proposals {String Object})
    (declare-pstate n $$projection-chat-canvas {String Object})
    (declare-pstate n $$projection-object-detail {String Object})
    (declare-pstate n $$projection-object-relations {String Object})

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
          (world-thread-object-row *thread-row :> *thread-object)
          (object-row-id *thread-object :> *thread-object-id)
          (chat-canvas-projection *thread-row nil nil :> *chat-canvas)
          (object-detail-projection *thread-object :> *thread-object-detail)
          (|hash *event-id)
          (local-transform> [(keypath *event-id) (termval *thread-event)] $$world-events-by-id)
          (|hash *event-thread-id)
          (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
          (local-transform> [(keypath *event-thread-id) (termval *chat-canvas)] $$projection-chat-canvas)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail))

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
            (world-thread-object-row *thread-row :> *thread-object)
            (world-turn-object-row *turn-row :> *turn-object)
            (context-bundle-object-row *bundle :> *bundle-object)
            (llm-turn-run-object-row *llm-request :> *llm-run-object)
            (thread-turn-edge *thread-row *turn-row :> *thread-turn-edge)
            (turn-bundle-edge *turn-row *bundle :> *turn-bundle-edge)
            (turn-llm-run-edge *turn-row *llm-request :> *turn-llm-run-edge)
            (bundle-llm-run-edge *bundle *llm-request :> *bundle-llm-run-edge)
            (object-row-id *thread-object :> *thread-object-id)
            (object-row-id *turn-object :> *turn-object-id)
            (object-row-id *bundle-object :> *bundle-object-id)
            (object-row-id *llm-run-object :> *llm-run-object-id)
            (artifact-edge-id *thread-turn-edge :> *thread-turn-edge-id)
            (artifact-edge-id *turn-bundle-edge :> *turn-bundle-edge-id)
            (artifact-edge-id *turn-llm-run-edge :> *turn-llm-run-edge-id)
            (artifact-edge-id *bundle-llm-run-edge :> *bundle-llm-run-edge-id)
            (artifact-edge-from *thread-turn-edge :> *thread-turn-from)
            (artifact-edge-to *thread-turn-edge :> *thread-turn-to)
            (artifact-edge-from *turn-bundle-edge :> *turn-bundle-from)
            (artifact-edge-to *turn-bundle-edge :> *turn-bundle-to)
            (artifact-edge-from *turn-llm-run-edge :> *turn-llm-run-from)
            (artifact-edge-to *turn-llm-run-edge :> *turn-llm-run-to)
            (artifact-edge-from *bundle-llm-run-edge :> *bundle-llm-run-from)
            (artifact-edge-to *bundle-llm-run-edge :> *bundle-llm-run-to)
            (chat-canvas-projection *thread-row *turn-order *turn-row :> *chat-canvas)
            (object-detail-projection *thread-object :> *thread-object-detail)
            (object-detail-projection *turn-object :> *turn-object-detail)
            (object-detail-projection *bundle-object :> *bundle-object-detail)
            (object-detail-projection *llm-run-object :> *llm-run-object-detail)
            (turn-object-relations-projection
              *turn-object-id *thread-turn-edge *turn-bundle-edge *turn-llm-run-edge
              :> *turn-relations)
            (bundle-object-relations-projection
              *bundle-object-id *turn-bundle-edge *bundle-llm-run-edge
              :> *bundle-relations)
            (llm-run-object-relations-projection
              *llm-run-object-id *turn-llm-run-edge *bundle-llm-run-edge
              :> *llm-run-relations)
            (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
            (local-transform> [(keypath *event-thread-id) (termval *turn-order)] $$world-turns-by-thread)
            (local-transform> [(keypath *event-thread-id) (termval *chat-canvas)] $$projection-chat-canvas)
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
            (|hash *thread-object-id)
            (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
            (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)
            (local-select> [(keypath *thread-object-id)] $$projection-object-relations :> *thread-relations-existing)
            (add-projection-out-edge *thread-relations-existing *thread-object-id *thread-turn-edge :> *thread-relations)
            (local-transform> [(keypath *thread-object-id) (termval *thread-relations)] $$projection-object-relations)
            (local-transform> [(keypath *thread-turn-from) (keypath *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph)
            (|hash *thread-turn-to)
            (local-transform> [(keypath *thread-turn-to) (keypath *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph-in)
            (|hash *turn-object-id)
            (local-transform> [(keypath *turn-object-id) (termval *turn-object)] $$objects)
            (local-transform> [(keypath *turn-object-id) (termval *turn-object-detail)] $$projection-object-detail)
            (local-transform> [(keypath *turn-object-id) (termval *turn-relations)] $$projection-object-relations)
            (local-transform> [(keypath *turn-bundle-from) (keypath *turn-bundle-edge-id) (termval *turn-bundle-edge)] $$artifact-graph)
            (local-transform> [(keypath *turn-llm-run-from) (keypath *turn-llm-run-edge-id) (termval *turn-llm-run-edge)] $$artifact-graph)
            (|hash *turn-bundle-to)
            (local-transform> [(keypath *turn-bundle-to) (keypath *turn-bundle-edge-id) (termval *turn-bundle-edge)] $$artifact-graph-in)
            (|hash *turn-llm-run-to)
            (local-transform> [(keypath *turn-llm-run-to) (keypath *turn-llm-run-edge-id) (termval *turn-llm-run-edge)] $$artifact-graph-in)
            (|hash *bundle-object-id)
            (local-transform> [(keypath *bundle-object-id) (termval *bundle-object)] $$objects)
            (local-transform> [(keypath *bundle-object-id) (termval *bundle-object-detail)] $$projection-object-detail)
            (local-transform> [(keypath *bundle-object-id) (termval *bundle-relations)] $$projection-object-relations)
            (local-transform> [(keypath *bundle-llm-run-from) (keypath *bundle-llm-run-edge-id) (termval *bundle-llm-run-edge)] $$artifact-graph)
            (|hash *bundle-llm-run-to)
            (local-transform> [(keypath *bundle-llm-run-to) (keypath *bundle-llm-run-edge-id) (termval *bundle-llm-run-edge)] $$artifact-graph-in)
            (|hash *llm-run-object-id)
            (local-transform> [(keypath *llm-run-object-id) (termval *llm-run-object)] $$objects)
            (local-transform> [(keypath *llm-run-object-id) (termval *llm-run-object-detail)] $$projection-object-detail)
            (local-transform> [(keypath *llm-run-object-id) (termval *llm-run-relations)] $$projection-object-relations)
            (|hash$$ *llm-depot *llm-run-id)
            (depot-partition-append! *llm-depot *llm-request :append-ack)))

        (case> (= :world-thread/fork-from-span *request-type))
        (interpret-fork-from-span *request *existing-thread :> *decision)
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
          (parent-thread-id-from-event *thread-event :> *parent-thread-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (context-bundle-id-from-event *bundle-event :> *bundle-id)
          (context-bundle-row *request *turn-event :> *bundle)
          (world->llm-turn-run-request *request *turn-event *bundle :> *llm-request)
          (llm-run-id-from-request *llm-request :> *llm-run-id)
          (thread-row *existing-thread *thread-event :> *base-thread-row)
          (turn-row *turn-event *bundle-id :> *turn-row)
          (slice-row *request *turn-event :> *slice-row)
          (slice-row-id *slice-row :> *slice-id)
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
          (<<if (has-parent-thread? *thread-event)
            (child-thread-edge *thread-event :> *child-edge)
            (|hash *parent-thread-id)
            (local-transform> [(keypath *parent-thread-id) (keypath *event-thread-id) (termval *child-edge)] $$world-thread-graph))
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$world-turns)
          (local-transform> [(keypath *turn-id) (termval *bundle-id)] $$context-bundles-by-turn)
          (local-transform> [(keypath *turn-id) (termval *llm-run-id)] $$world-llm-run-by-turn)
          (|hash *bundle-id)
          (local-transform> [(keypath *bundle-id) (termval *bundle)] $$context-bundles)
          (|hash *slice-id)
          (local-select> [(keypath *slice-id)] $$slices :> *existing-slice-row)
          (keep-existing-slice-row *existing-slice-row *slice-row :> *stored-slice-row)
          (local-transform> [(keypath *slice-id) (termval *stored-slice-row)] $$slices)
          (|hash *llm-run-id)
          (local-transform> [(keypath *llm-run-id) (termval *llm-request)] $$world-llm-run-requests)
          (|hash$$ *llm-depot *llm-run-id)
          (depot-partition-append! *llm-depot *llm-request :append-ack))

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
          (world-thread-object-row *thread-row :> *thread-object)
          (world-turn-object-row *turn-row :> *turn-object)
          (thread-turn-edge *thread-row *turn-row :> *thread-turn-edge)
          (object-row-id *thread-object :> *thread-object-id)
          (object-row-id *turn-object :> *turn-object-id)
          (artifact-edge-id *thread-turn-edge :> *thread-turn-edge-id)
          (artifact-edge-from *thread-turn-edge :> *thread-turn-from)
          (artifact-edge-to *thread-turn-edge :> *thread-turn-to)
          (chat-canvas-projection *thread-row *turn-order *turn-row :> *chat-canvas)
          (object-detail-projection *thread-object :> *thread-object-detail)
          (object-detail-projection *turn-object :> *turn-object-detail)
          (one-incoming-relation-projection *turn-object-id *thread-turn-edge :> *turn-relations)
          (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
          (local-transform> [(keypath *event-thread-id) (termval *turn-order)] $$world-turns-by-thread)
          (local-transform> [(keypath *event-thread-id) (termval *chat-canvas)] $$projection-chat-canvas)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$world-events-by-id)
          (|hash *control-event-id)
          (local-transform> [(keypath *control-event-id) (termval *control-event)] $$world-events-by-id)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$world-turns)
          (local-transform> [(keypath *turn-id) (termval *control-id)] $$world-llm-control-by-turn)
          (|hash *control-id)
          (local-transform> [(keypath *control-id) (termval *control)] $$world-llm-controls)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)
          (local-select> [(keypath *thread-object-id)] $$projection-object-relations :> *thread-relations-existing)
          (add-projection-out-edge *thread-relations-existing *thread-object-id *thread-turn-edge :> *thread-relations)
          (local-transform> [(keypath *thread-object-id) (termval *thread-relations)] $$projection-object-relations)
          (local-transform> [(keypath *thread-turn-from) (keypath *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph)
          (|hash *thread-turn-to)
          (local-transform> [(keypath *thread-turn-to) (keypath *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph-in)
          (|hash *turn-object-id)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object)] $$objects)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *turn-object-id) (termval *turn-relations)] $$projection-object-relations)
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
          (world-thread-object-row *thread-row :> *thread-object)
          (world-turn-object-row *turn-row :> *turn-object)
          (thread-turn-edge *thread-row *turn-row :> *thread-turn-edge)
          (object-row-id *thread-object :> *thread-object-id)
          (object-row-id *turn-object :> *turn-object-id)
          (artifact-edge-id *thread-turn-edge :> *thread-turn-edge-id)
          (artifact-edge-from *thread-turn-edge :> *thread-turn-from)
          (artifact-edge-to *thread-turn-edge :> *thread-turn-to)
          (chat-canvas-projection *thread-row *turn-order *turn-row :> *chat-canvas)
          (object-detail-projection *thread-object :> *thread-object-detail)
          (object-detail-projection *turn-object :> *turn-object-detail)
          (one-incoming-relation-projection *turn-object-id *thread-turn-edge :> *turn-relations)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$world-events-by-id)
          (|hash *event-thread-id)
          (local-transform> [(keypath *event-thread-id) (termval *thread-row)] $$world-threads)
          (local-transform> [(keypath *event-thread-id) (termval *turn-order)] $$world-turns-by-thread)
          (local-transform> [(keypath *event-thread-id) (termval *chat-canvas)] $$projection-chat-canvas)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$world-turns)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)
          (local-select> [(keypath *thread-object-id)] $$projection-object-relations :> *thread-relations-existing)
          (add-projection-out-edge *thread-relations-existing *thread-object-id *thread-turn-edge :> *thread-relations)
          (local-transform> [(keypath *thread-object-id) (termval *thread-relations)] $$projection-object-relations)
          (local-transform> [(keypath *thread-turn-from) (keypath *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph)
          (|hash *thread-turn-to)
          (local-transform> [(keypath *thread-turn-to) (keypath *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph-in)
          (|hash *turn-object-id)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object)] $$objects)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *turn-object-id) (termval *turn-relations)] $$projection-object-relations)
          (<<if (= :world-turn/slice-create *request-type)
            (slice-row *request *turn-event :> *slice-row)
            (slice-object-row *slice-row :> *slice-object)
            (slice-row-id *slice-row :> *slice-id)
            (object-row-id *slice-object :> *slice-object-id)
            (object-detail-projection *slice-object :> *slice-object-detail)
            (|hash *slice-id)
            (local-transform> [(keypath *slice-id) (termval *slice-row)] $$slices)
            (|hash *slice-object-id)
            (local-transform> [(keypath *slice-object-id) (termval *slice-object)] $$objects)
            (local-transform> [(keypath *slice-object-id) (termval *slice-object-detail)] $$projection-object-detail)
            (<<if (raw-llm-source? *request)
              (raw-llm-item-object-row *request :> *raw-object)
              (source-slice-edge *raw-object *slice-row :> *source-edge)
              (object-row-id *raw-object :> *raw-object-id)
              (artifact-edge-id *source-edge :> *source-edge-id)
              (artifact-edge-from *source-edge :> *source-edge-from)
              (artifact-edge-to *source-edge :> *source-edge-to)
              (object-detail-projection *raw-object :> *raw-object-detail)
              (one-incoming-relation-projection *source-edge-to *source-edge :> *source-target-relations)
              (|hash *raw-object-id)
              (local-transform> [(keypath *raw-object-id) (termval *raw-object)] $$objects)
              (local-transform> [(keypath *raw-object-id) (termval *raw-object-detail)] $$projection-object-detail)
              (local-select> [(keypath *raw-object-id)] $$projection-object-relations :> *raw-relations-existing)
              (add-projection-out-edge *raw-relations-existing *raw-object-id *source-edge :> *raw-relations)
              (local-transform> [(keypath *raw-object-id) (termval *raw-relations)] $$projection-object-relations)
              (local-transform> [(keypath *source-edge-from) (keypath *source-edge-id) (termval *source-edge)] $$artifact-graph)
              (|hash *source-edge-to)
              (local-transform> [(keypath *source-edge-to) (keypath *source-edge-id) (termval *source-edge)] $$artifact-graph-in)
              (local-transform> [(keypath *source-edge-to) (termval *source-target-relations)] $$projection-object-relations)))
          (<<if (= :world-turn/comment-create *request-type)
            (overlay-row *request *turn-event :> *overlay-row)
            (overlay-object-row *overlay-row :> *overlay-object)
            (overlay-row-id *overlay-row :> *overlay-id)
            (object-row-id *overlay-object :> *overlay-object-id)
            (object-detail-projection *overlay-object :> *overlay-object-detail)
            (|hash *overlay-id)
            (local-transform> [(keypath *overlay-id) (termval *overlay-row)] $$overlays)
            (|hash *overlay-object-id)
            (local-transform> [(keypath *overlay-object-id) (termval *overlay-object)] $$objects)
            (local-transform> [(keypath *overlay-object-id) (termval *overlay-object-detail)] $$projection-object-detail)
            (<<if (raw-llm-source? *request)
              (raw-llm-item-object-row *request :> *raw-object)
              (source-overlay-edge *raw-object *overlay-row :> *source-edge)
              (object-row-id *raw-object :> *raw-object-id)
              (artifact-edge-id *source-edge :> *source-edge-id)
              (artifact-edge-from *source-edge :> *source-edge-from)
              (artifact-edge-to *source-edge :> *source-edge-to)
              (object-detail-projection *raw-object :> *raw-object-detail)
              (one-incoming-relation-projection *source-edge-to *source-edge :> *source-target-relations)
              (|hash *raw-object-id)
              (local-transform> [(keypath *raw-object-id) (termval *raw-object)] $$objects)
              (local-transform> [(keypath *raw-object-id) (termval *raw-object-detail)] $$projection-object-detail)
              (local-select> [(keypath *raw-object-id)] $$projection-object-relations :> *raw-relations-existing)
              (add-projection-out-edge *raw-relations-existing *raw-object-id *source-edge :> *raw-relations)
              (local-transform> [(keypath *raw-object-id) (termval *raw-relations)] $$projection-object-relations)
              (local-transform> [(keypath *source-edge-from) (keypath *source-edge-id) (termval *source-edge)] $$artifact-graph)
              (|hash *source-edge-to)
              (local-transform> [(keypath *source-edge-to) (keypath *source-edge-id) (termval *source-edge)] $$artifact-graph-in)
              (local-transform> [(keypath *source-edge-to) (termval *source-target-relations)] $$projection-object-relations)))
          (<<if (= :world-turn/derivative-create *request-type)
            (derivative-row *request *turn-event :> *derivative-row)
            (derivative-object-row *derivative-row :> *derivative-object)
            (derivative-row-id *derivative-row :> *derivative-id)
            (object-row-id *derivative-object :> *derivative-object-id)
            (object-detail-projection *derivative-object :> *derivative-object-detail)
            (|hash *derivative-id)
            (local-transform> [(keypath *derivative-id) (termval *derivative-row)] $$derivatives)
            (|hash *derivative-object-id)
            (local-transform> [(keypath *derivative-object-id) (termval *derivative-object)] $$objects)
            (local-transform> [(keypath *derivative-object-id) (termval *derivative-object-detail)] $$projection-object-detail)
            (<<if (raw-llm-source? *request)
              (raw-llm-item-object-row *request :> *raw-object)
              (source-derivative-edge *raw-object *derivative-row :> *source-edge)
              (object-row-id *raw-object :> *raw-object-id)
              (artifact-edge-id *source-edge :> *source-edge-id)
              (artifact-edge-from *source-edge :> *source-edge-from)
              (artifact-edge-to *source-edge :> *source-edge-to)
              (object-detail-projection *raw-object :> *raw-object-detail)
              (one-incoming-relation-projection *source-edge-to *source-edge :> *source-target-relations)
              (|hash *raw-object-id)
              (local-transform> [(keypath *raw-object-id) (termval *raw-object)] $$objects)
              (local-transform> [(keypath *raw-object-id) (termval *raw-object-detail)] $$projection-object-detail)
              (local-select> [(keypath *raw-object-id)] $$projection-object-relations :> *raw-relations-existing)
              (add-projection-out-edge *raw-relations-existing *raw-object-id *source-edge :> *raw-relations)
              (local-transform> [(keypath *raw-object-id) (termval *raw-relations)] $$projection-object-relations)
              (local-transform> [(keypath *source-edge-from) (keypath *source-edge-id) (termval *source-edge)] $$artifact-graph)
              (|hash *source-edge-to)
              (local-transform> [(keypath *source-edge-to) (keypath *source-edge-id) (termval *source-edge)] $$artifact-graph-in)
              (local-transform> [(keypath *source-edge-to) (termval *source-target-relations)] $$projection-object-relations)))
          (<<if (= :world-turn/patch-proposal-create *request-type)
            (patch-proposal-row-from-request *request :> *patch-proposal)
            (request-patch-proposal-id *request :> *patch-proposal-id)
            (|hash *patch-proposal-id)
            (local-transform> [(keypath *patch-proposal-id) (termval *patch-proposal)] $$world-patch-proposals))
          (<<if (patch-decision-request? *request-type)
            (request-patch-proposal-id *request :> *patch-proposal-id)
            (|hash *patch-proposal-id)
            (local-select> [(keypath *patch-proposal-id)] $$world-patch-proposals :> *existing-patch-proposal)
            (apply-patch-decision *existing-patch-proposal *request *turn-event :> *patch-proposal)
            (local-transform> [(keypath *patch-proposal-id) (termval *patch-proposal)] $$world-patch-proposals)))

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
     :objects (foreign-pstate ipc module-name "$$objects")
     :artifact-graph (foreign-pstate ipc module-name "$$artifact-graph")
     :artifact-graph-in (foreign-pstate ipc module-name "$$artifact-graph-in")
     :slices (foreign-pstate ipc module-name "$$slices")
     :overlays (foreign-pstate ipc module-name "$$overlays")
     :derivatives (foreign-pstate ipc module-name "$$derivatives")
     :world-patch-proposals (foreign-pstate ipc module-name "$$world-patch-proposals")
     :projection-chat-canvas (foreign-pstate ipc module-name "$$projection-chat-canvas")
     :projection-object-detail (foreign-pstate ipc module-name "$$projection-object-detail")
     :projection-object-relations (foreign-pstate ipc module-name "$$projection-object-relations")
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
     :llm-cost-by-thread (foreign-pstate ipc llm-module-name "$$llm-cost-by-thread")
     :llm-controls-by-run-id (foreign-pstate ipc llm-module-name "$$llm-controls-by-run-id")
     :llm-control-by-id (foreign-pstate ipc llm-module-name "$$llm-control-by-id")
     :llm-views (foreign-pstate ipc llm-module-name "$$llm-views")
     :projection-run-detail (foreign-pstate ipc llm-module-name "$$projection-run-detail")}))

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

(defn append-llm-observation!
  ([runtime obs]
   (append-llm-observation! runtime obs :append-ack))
  ([runtime obs ack-level]
   (llm/append-observation! runtime obs ack-level)
   (append-world-action!
     runtime
     (world-action-request
       :world-turn/patch-proposal-create
       (or (:world-thread/id obs) (:llm-thread/id obs))
        {:request-id (str (:observation/id obs) "/world-patch-proposal")
        :time-ms (:received-at-ms obs)
        :payload {:world-turn/id (str (:observation/id obs) "/world-patch-proposal-turn")
                  :patch-proposal/id (patch-proposal-id-from-observation obs)
                  :turn-diff/id (or (:turn-diff/id obs)
                                    (patch-proposal-id-from-observation obs))
                  :llm-turn-run/id (:llm-turn-run/id obs)
                  :llm-thread/id (:llm-thread/id obs)
                  :source-world-turn/id (:world-turn/id obs)
                  :prompt/text (:summary/text obs)
                  :summary/text (:summary/text obs)
                  :patch/files (:patch/files obs)
                  :observation/id (:observation/id obs)
                  :raw/json (:raw/json obs)}})
     ack-level)
   obs))

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

(defn read-thread-graph
  [runtime world-thread-id]
  (or (select-pstate-one (:world-thread-graph runtime) [(keypath world-thread-id)])
      {}))

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

(defn read-object
  [runtime object-id]
  (select-pstate-one (:objects runtime) [(keypath object-id)]))

(defn read-artifact-graph
  [runtime object-id]
  (or (select-pstate-one (:artifact-graph runtime) [(keypath object-id)])
      {}))

(defn read-artifact-graph-in
  [runtime object-id]
  (or (select-pstate-one (:artifact-graph-in runtime) [(keypath object-id)])
      {}))

(defn read-chat-canvas-projection
  [runtime world-thread-id]
  (select-pstate-one (:projection-chat-canvas runtime) [(keypath world-thread-id)]))

(defn read-object-detail-projection
  [runtime object-id]
  (select-pstate-one (:projection-object-detail runtime) [(keypath object-id)]))

(defn read-object-relations-projection
  [runtime object-id]
  (or (select-pstate-one (:projection-object-relations runtime) [(keypath object-id)])
      (empty-object-relations-projection object-id)))

(defn read-slice
  [runtime slice-id]
  (select-pstate-one (:slices runtime) [(keypath slice-id)]))

(defn read-overlay
  [runtime overlay-id]
  (select-pstate-one (:overlays runtime) [(keypath overlay-id)]))

(defn read-derivative
  [runtime derivative-id]
  (select-pstate-one (:derivatives runtime) [(keypath derivative-id)]))

(defn read-patch-proposal
  [runtime patch-proposal-id]
  (select-pstate-one (:world-patch-proposals runtime) [(keypath patch-proposal-id)]))

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
