(ns app.server.rama.dogfood.space
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as core]
            [app.server.rama.dogfood.llm :as llm]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]]))

;; ────────────────────────────────────────────────────────────────────────────────
;;   SPACE KERNEL
;;
;;   The Space Kernel is the local-world kernel: it manages a graph of
;;   inhabitable spaces — currently most visible as chat-thread work areas,
;;   but meant to hold more shapes of work over time. Each space holds its
;;   own turns, objects, slices, overlays, derivatives, patch proposals, and
;;   projections. The kernel creates spaces and the turns that happen inside
;;   them, freezes a context bundle per turn, accumulates the content
;;   attached to those turns, and — on compose/fork flows that need external
;;   model work — reaches across into the LLM Kernel through mirrored depots.
;;   Many space requests are purely local; only some dispatch LLM runs or
;;   controls.
;;
;;   Compressed:  conversation and intent become inhabitable local worlds.
;;
;;   Boundary:  Space manages chat as place; LLM executes model runs inside
;;   that place. Space owns the turns and freezes the bundles; it never runs
;;   the model itself — that crosses into llm-kernel via `mirror-depot`.
;;
;;   Scope note:  a single "space" is itself a small world (turns + objects +
;;   slices + overlays + derivatives + patches + projections); the kernel
;;   maintains many of them, related to each other through `$$space-graph`
;;   and `:space/fork-from-span`. Chat-thread is the current concrete form;
;;   the kernel's shape leaves room for other work-area shapes later.
;;
;;   For the kernel taxonomy and KERNEL-SHAPE spec see app.server.rama.kernel.
;; ────────────────────────────────────────────────────────────────────────────────

(def schema-version 1)
(def default-space-branch-id "space/main")

(def turn-request-types
  #{:turn/compose-and-send
    :turn/draft-save
    :turn/comment-create
    :turn/slice-create
    :turn/derivative-create
    :turn/patch-proposal-create
    :turn/patch-accept
    :turn/patch-reject
    :turn/tool-approval-resolve
    :turn/cancel
    :turn/compact-request
    :turn/steer
    :turn/abandon})

(def space-request-types
  #{:space/create
    :space/fork-from-span
    :space/reconcile})

(def space-control-request-types
  #{:turn/tool-approval-resolve
    :turn/cancel
    :turn/compact-request
    :turn/steer})

(def space-turn-request-types
  (disj turn-request-types :turn/compose-and-send))

(defn now-ms [] (core/now-ms))
(defn random-id [prefix] (core/random-id prefix))

(defn blank-string?
  [x]
  (or (not (string? x)) (str/blank? x)))

(defn space-routing-key
  [space-id]
  [:space space-id])

(defn normalize-actor
  [actor]
  (merge {:actor/id "system"
          :actor/type :system}
         actor))

(defn action-capability
  [request-type]
  (case request-type
    :space/create :space/create
    :turn/compose-and-send :llm/send-turn
    :turn/draft-save :turn/write
    :turn/comment-create :turn/write
    :turn/slice-create :turn/write
    :turn/derivative-create :turn/write
    :turn/patch-proposal-create :space-patch/ingest
    :turn/patch-accept :turn/write
    :turn/patch-reject :turn/write
    :turn/tool-approval-resolve :llm/control
    :turn/cancel :turn/control
    :turn/compact-request :llm/control
    :turn/steer :llm/control
    :turn/abandon :turn/control
    :space/fork-from-span :space/fork
    :space/reconcile :space/reconcile
    :space/write))

(defn space-action-request
  [request-type space-id & [opts]]
  (let [request-id (or (:request-id opts) (:request/id opts) (random-id "space-req"))
        time-ms (or (:time-ms opts) (:request/time-ms opts) (now-ms))
        space-id (or space-id
                            (:space-id opts)
                            (:space/id opts)
                            (get-in opts [:payload :space/id]))
        payload (merge (:payload opts)
                       (select-keys opts
                                    [:prompt/text :refs :llm/options
                                     :execution/options :turn/kind
                                     :draft/id :title])
                       {:space/id space-id})
        routing-key (or (:routing/key opts)
                        (:routing-key opts)
                        (space-routing-key space-id))]
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
     :branch (merge {:branch/id default-space-branch-id} (:branch opts))
     :context (merge {:projection/id nil
                      :selection/id nil}
                     (:context opts))
     :target {:target/kind :space
              :target/id space-id}
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

(defn space-create-request
  [space-id & [opts]]
  (space-action-request :space/create space-id opts))

(defn compose-and-send-request
  [space-id prompt-text & [opts]]
  (space-action-request
    :turn/compose-and-send
    space-id
    (assoc opts :prompt/text prompt-text)))

(defn space-turn-request
  [request-type space-id & [opts]]
  (space-action-request request-type space-id opts))

(def required-request-keys
  [:request/id :request/type :request/schema-version :request/time-ms
   :idempotency/key :routing/key :actor :target :action :payload])

(defn request-thread-id
  [request]
  (or (:space/id request)
      (get-in request [:payload :space/id])
      (get-in request [:target :target/id])))

(defn request-turn-id
  [request]
  (or (get-in request [:payload :turn/id])
      (:turn/id request)
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
(defn space-turn-request? [request-type] (contains? space-turn-request-types request-type))
(defn space-control-request? [request-type] (contains? space-control-request-types request-type))

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
           (not (or (contains? space-request-types request-type)
                    (contains? turn-request-types request-type))))
      (conj {:type :request/type-invalid
             :value request-type})

      (and (map? request) (blank-string? (:request/id request)))
      (conj {:type :request/id-invalid
             :value (:request/id request)})

      (and (map? request) (blank-string? (:idempotency/key request)))
      (conj {:type :idempotency/key-invalid
             :value (:idempotency/key request)})

      (and (map? request) (blank-string? thread-id))
      (conj {:type :space/id-invalid
             :value thread-id})

      (and (map? request) (not= (space-routing-key thread-id) (:routing/key request)))
      (conj {:type :routing/key-invalid
             :value (:routing/key request)
             :expected (space-routing-key thread-id)})

      (and (map? request)
           (not= request-type (get-in request [:action :action/type])))
      (conj {:type :request/action-type-drift
             :request/type request-type
             :action/type (get-in request [:action :action/type])})

      (and (map? request) (= :turn/compose-and-send request-type)
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
     :space/id (request-thread-id request)
     :turn/id (:turn/id turn-event)
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
   :space/id (request-thread-id request)
   :actor (:actor request)
   :branch (:branch request)
   :causal (merge {:parents []
                   :correlation/id (:request/id request)}
                  (:causal request))
   :provenance {:source/type :action-request
                :source/ref (:request/id request)}})

(defn space-event
  [request existing-thread]
  (let [parent-id (or (get-in request [:payload :parent-space/id])
                      (:parent-space/id existing-thread))
        parent-ids (or (get-in request [:payload :parent-space/ids])
                       (:parent-space/ids existing-thread)
                       (when parent-id [parent-id])
                       [])]
    (merge (base-event request
                       (if existing-thread
                         :space/touched
                         :space/created)
                       "space")
           {:space/id (request-thread-id request)
            :title (or (get-in request [:payload :title])
                       (:title existing-thread)
                       (request-thread-id request))
            :parent-space/id parent-id
            :parent-space/ids (vec parent-ids)})))

(defn turn-event
  [request]
  (let [request-type (:request/type request)
        turn-kind (if (= :turn/compose-and-send request-type)
                    :compose-and-send
                    (or (get-in request [:payload :turn/kind])
                        request-type))]
    (merge (base-event request :turn/created "turn")
           {:turn/id (request-turn-id request)
            :turn/kind turn-kind
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
    (:turn/id ref) (str "turn:" (:turn/id ref))
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
        stable-material {:space/id (request-thread-id request)
                         :turn/id (:turn/id turn-event)
                         :prompt/text (get-in request [:payload :prompt/text])
                         :refs (vec (or (get-in request [:payload :refs]) []))
                         :execution/options execution-options
                         :rendered/model-input rendered}]
    (assoc stable-material
           :context-bundle/id (request-bundle-id request)
           :request/id (:request/id request)
           :bundle/schema-version schema-version
           :context-bundle/hash (str "sha256:" (core/sha-256 (pr-str stable-material)))
           :created-at-ms (:request/time-ms request))))

(defn context-bundle-event
  [request turn-event bundle]
  (merge (base-event request :context-bundle/frozen "context-bundle")
         {:turn/id (:turn/id turn-event)
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

(defn space->llm-turn-run-request
  [request turn-event bundle]
  (let [run-id (request-llm-run-id request)
        llm-thread-id (request-llm-thread-id request)
        executor-task-id (or (get-in request [:payload :executor/task-id])
                             (get-in request [:payload :llm/options :executor/task-id])
                             (get-in request [:payload :execution/options :executor/task-id]))]
    (llm/turn-run-request
      (request-thread-id request)
      (:turn/id turn-event)
      (:context-bundle/id bundle)
      (cond-> {:llm-turn-run-id run-id
               :llm-thread-id llm-thread-id
               :request-id (request-llm-request-id request)
               :time-ms (:request/time-ms request)
               :idempotency-key (str "space-event:" (:request/id request) ":"
                                     (:turn/id turn-event) ":"
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
         {:turn/id (:turn/id turn-event)
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
  ;; NOTE: Some request/type values intentionally share the same keyword as the
  ;; LLM control/type they derive. This dispatch is authoritative on :request/type.
  (case (:request/type request)
    :turn/tool-approval-resolve :approval/resolve
    :turn/cancel :turn/cancel
    :turn/compact-request :compact/request
    :turn/steer :turn/steer))

(defn request-control-id
  [request]
  (or (get-in request [:payload :control/id])
      (:control/id request)
      (str (:request/id request) "/llm-control")))

(defn request-control-run-id
  [request]
  (or (get-in request [:payload :llm-turn-run/id])
      (:llm-turn-run/id request)))

(defn space->llm-control-record
  [request turn-event]
  (llm/control-record
    (request-control-run-id request)
    (request-control-type request)
    {:control-id (request-control-id request)
     :llm-thread/id (get-in request [:payload :llm-thread/id])
     :space/id (request-thread-id request)
     :turn/id (:turn/id turn-event)
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
         {:turn/id (:turn/id turn-event)
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
      (let [thread-event (space-event request existing-thread)]
        (accepted-decision request thread-event {:thread thread-event}))))

(defn interpret-compose-and-send
  [request existing-thread]
  (or (validate-or-reject request)
      (let [thread-event (space-event request existing-thread)
            turn-event (turn-event request)
            bundle (context-bundle-row request turn-event)
            bundle-event (context-bundle-event request turn-event bundle)
            llm-request (space->llm-turn-run-request request turn-event bundle)
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
      (let [thread-event (space-event request existing-thread)
            turn-event (turn-event request)
            bundle (context-bundle-row request turn-event)
            bundle-event (context-bundle-event request turn-event bundle)
            llm-request (space->llm-turn-run-request request turn-event bundle)
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
   :space/id (request-thread-id request)
   :turn/id (get-in decision [:events :turn :turn/id])
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
   :space/id (:space/id existing-row)
   :turn/id (:turn/id existing-row)
   :context-bundle/id (:context-bundle/id existing-row)
   :context-bundle/hash (:context-bundle/hash existing-row)
   :llm-thread/id (:llm-thread/id existing-row)
   :llm-turn-run/id (:llm-turn-run/id existing-row)
   :llm/request-id (:llm/request-id existing-row)
   :idempotency/key (:idempotency/key request)
   :idempotency/replayed? true
   :idempotency/original-request-id (:request/id existing-row)
   :decided-at-ms (:request/time-ms request)})

(defn interpret-space-turn
  [request existing-thread]
  (if-let [rejection (validate-or-reject request)]
    rejection
    (if (nil? existing-thread)
      (rejected-decision request :space/not-found)
      (let [turn-event (turn-event request)]
        (accepted-decision request turn-event {:turn turn-event})))))

(defn interpret-control-turn
  [request existing-thread]
  (cond
    (validate-or-reject request)
    (validate-or-reject request)

    (nil? existing-thread)
    (rejected-decision request :space/not-found)

    (blank-string? (request-control-run-id request))
    (rejected-decision request :llm-turn-run/id-invalid)

    (and (= :turn/tool-approval-resolve (:request/type request))
         (blank-string? (get-in request [:payload :approval/id])))
    (rejected-decision request :approval/id-invalid)

    :else
    (let [turn-event (turn-event request)
          control (space->llm-control-record request turn-event)
          control-event (llm-control-event request turn-event control)]
      (accepted-decision request turn-event {:turn turn-event
                                             :llm-control control-event}))))

(defn thread-row
  [existing-thread thread-event]
  (let [time-ms (:event/time-ms thread-event)]
    (-> (or existing-thread
            {:space/id (:space/id thread-event)
             :created-at-ms time-ms
             :turn-count 0})
        (assoc :space/id (:space/id thread-event)
               :title (:title thread-event)
               :status :active
               :updated-at-ms time-ms
               :parent-space/id (:parent-space/id thread-event)
               :parent-space/ids (vec (:parent-space/ids thread-event))))))

(defn turn-row
  [turn-event context-bundle-id]
  (cond-> {:turn/id (:turn/id turn-event)
           :space/id (:space/id turn-event)
           :turn/kind (:turn/kind turn-event)
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
  (:turn/id turn-event))

(defn space-id-from-event
  [event]
  (:space/id event))

(defn parent-thread-id-from-event
  [event]
  (:parent-space/id event))

(defn has-parent-thread?
  [thread-event]
  (not (blank-string? (:parent-space/id thread-event))))

(defn child-thread-edge
  [thread-event]
  {:edge/type :space/child
   :parent-space/id (:parent-space/id thread-event)
   :child-space/id (:space/id thread-event)
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

(defn space-object-row
  [thread-row]
  {:object/id (catalog-object-id :space (:space/id thread-row))
   :object/type :space
   :source/type :space
   :source/id (:space/id thread-row)
   :space/id (:space/id thread-row)
   :title (:title thread-row)
   :status (:status thread-row)
   :turn-count (:turn-count thread-row)
   :created-at-ms (:created-at-ms thread-row)
   :updated-at-ms (:updated-at-ms thread-row)})

(defn turn-object-row
  [turn-row]
  {:object/id (catalog-object-id :turn (:turn/id turn-row))
   :object/type :turn
   :source/type :turn
   :source/id (:turn/id turn-row)
   :space/id (:space/id turn-row)
   :turn/id (:turn/id turn-row)
   :turn/kind (:turn/kind turn-row)
   :context-bundle/id (:context-bundle/id turn-row)
   :created-at-ms (:created-at-ms turn-row)
   :updated-at-ms (:updated-at-ms turn-row)})

(defn context-bundle-object-row
  [bundle]
  {:object/id (catalog-object-id :context-bundle (:context-bundle/id bundle))
   :object/type :context-bundle
   :source/type :context-bundle
   :source/id (:context-bundle/id bundle)
   :space/id (:space/id bundle)
   :turn/id (:turn/id bundle)
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
   :space/id (:space/id llm-request)
   :turn/id (:turn/id llm-request)
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
    (catalog-object-id :space (:space/id thread-row))
    :contains
    (catalog-object-id :turn (:turn/id turn-row))
    (:created-at-ms turn-row)
    (:request/id turn-row)))

(defn turn-bundle-edge
  [turn-row bundle]
  (artifact-edge
    (catalog-object-id :turn (:turn/id turn-row))
    :freezes-context
    (catalog-object-id :context-bundle (:context-bundle/id bundle))
    (:created-at-ms bundle)
    (:request/id turn-row)))

(defn turn-llm-run-edge
  [turn-row llm-request]
  (artifact-edge
    (catalog-object-id :turn (:turn/id turn-row))
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
  (str "sha256:" (core/sha-256 (or text ""))))

(defn material-request?
  [request-type]
  (contains? #{:turn/slice-create
               :turn/comment-create
               :turn/derivative-create}
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
     :space/id (request-thread-id request)
     :turn/id (:turn/id turn-event)
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
   :space/id (request-thread-id request)
   :turn/id (:turn/id turn-event)
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
     :space/id (request-thread-id request)
     :turn/id (:turn/id turn-event)
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
   :space/id (:space/id slice)
   :turn/id (:turn/id slice)
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
   :space/id (:space/id overlay)
   :turn/id (:turn/id overlay)
   :created-at-ms (:created-at-ms overlay)})

(defn derivative-object-row
  [derivative]
  {:object/id (catalog-object-id :derivative (:derivative/id derivative))
   :object/type :derivative
   :source/type :derivative
   :source/id (:derivative/id derivative)
   :derivative/id (:derivative/id derivative)
   :space/id (:space/id derivative)
   :turn/id (:turn/id derivative)
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
    (:turn/id slice)))

(defn source-overlay-edge
  [raw-object overlay]
  (artifact-edge
    (:object/id raw-object)
    :commented-by
    (catalog-object-id :overlay (:overlay/id overlay))
    (:created-at-ms overlay)
    (:turn/id overlay)))

(defn source-derivative-edge
  [raw-object derivative]
  (artifact-edge
    (:object/id raw-object)
    :derived-into
    (catalog-object-id :derivative (:derivative/id derivative))
    (:created-at-ms derivative)
    (:turn/id derivative)))

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
     :space/id (:space/id obs)
     :turn/id (:turn/id obs)
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
   :space/id (request-thread-id request)
   :turn/id (get-in request [:payload :source-turn/id])
   :status :pending
   :patch/files (get-in request [:payload :patch/files])
   :summary/text (get-in request [:payload :summary/text])
   :created-at-ms (:request/time-ms request)
   :observation/id (get-in request [:payload :observation/id])
   :raw/json (get-in request [:payload :raw/json])})

(defn patch-decision-request?
  [request-type]
  (contains? #{:turn/patch-accept :turn/patch-reject} request-type))

(defn request-patch-proposal-id
  [request]
  (or (get-in request [:payload :patch-proposal/id])
      (:patch-proposal/id request)))

(defn patch-decision-status
  [request-type]
  (case request-type
    :turn/patch-accept :accepted
    :turn/patch-reject :rejected))

(defn apply-patch-decision
  [existing request turn-event]
  (let [proposal-id (request-patch-proposal-id request)
        t (:event/time-ms turn-event)]
    (assoc (or existing {:patch-proposal/id proposal-id})
           :patch-proposal/id proposal-id
           :status (patch-decision-status (:request/type request))
           :resolved-at-ms t
           :resolved-by (:actor request)
           :resolution/turn-id (:turn/id turn-event)
           :resolution/request-id (:request/id request)
           :reason (get-in request [:payload :reason]))))

(defn turn-projection-row
  [turn-row]
  (select-keys turn-row
               [:turn/id :space/id :turn/kind
                :context-bundle/id :request/id :prompt/text :refs
                :created-at-ms :updated-at-ms]))

(defn chat-canvas-projection
  [thread-row turn-order latest-turn]
  (cond-> {:projection/type :chat-canvas
           :projection/source :space-canonical-pstates
           :space/id (:space/id thread-row)
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

;; ────────────────────────────────────────────────────────────────────────────
;; Journal-entry-first fold (fix session 3).
;;
;; Transaction scope in a stream topology is the code between two partitioners,
;; so every dedup-relevant read and every non-idempotent write must happen in
;; ONE segment on the space task, atomically with the request journal entry.
;; Each request type computes a pure write PLAN:
;;   {:decision         decision to write downstream (always present)
;;    :journal-entry    journal row, nil = skip (replay/conflict)
;;    :spaces-row       $$spaces write, nil = skip
;;    :turn-order       $$turns-by-space write, nil = skip
;;    :canvas           $$projection-chat-canvas write, nil = skip
;;    :idem-key/:idem-row  $$send-by-idempotency write, nil = skip
;;    :thread-row       input for downstream object rows (current row on replay)
;;    :emit             :full | :decision-only
;;    :record-request?  false only for impostor conflicts}
;; A journal hit means EVERY Segment-1 write committed (same atomic group), so
;; replay skips Segment 1 and re-emits the full downstream fan-out — required,
;; because the first attempt may have died mid-tree. All downstream writes are
;; idempotent termvals of replay-stable values or consumer-deduplicated
;; appends (LLM intake gates run ids; fold-control dedups control ids).
;; ────────────────────────────────────────────────────────────────────────────

(defn journal-key
  "Partition/journal key for a request. Requests with a blank space id cannot
   touch real space state (validation rejects them) but still need a stable,
   isolated journal slot so one-request-one-decision holds for junk input."
  [thread-id request-id]
  (if (blank-string? thread-id)
    (str "invalid:" request-id)
    thread-id))

(defn idempotency-lookup-key
  [request]
  (let [k (request-idempotency-key request)]
    (if (blank-string? k) "__no-key__" k)))

(defn send-material-hash
  "Hash of WHAT a send asks for — space, prompt, refs, bundle-owned execution
   options — excluding request id, caller-supplied fresh entity ids, and time,
   so a spec-sanctioned replay with fresh ids still matches while a different
   payload reusing the key is a conflict."
  [request]
  (core/sha-256
    (core/canonical-str
      {:space/id (request-thread-id request)
       :prompt/text (get-in request [:payload :prompt/text])
       :refs (vec (or (get-in request [:payload :refs]) []))
       :execution/options (bundle-execution-options request)})))

(defn idempotency-conflict-decision
  "Same idempotency key, different material: explicit conflict, never aliasing
   the caller to the original send's facts (prior-retro F3)."
  [request existing-row material-hash]
  (assoc (rejected-decision request :idempotency/conflict
                            [{:type :idempotency/conflict
                              :idempotency/key (:idempotency/key request)
                              :original/request-id (:request/id existing-row)
                              :existing/material-hash (:material/hash existing-row)
                              :incoming/material-hash material-hash}])
         :idempotency/key (:idempotency/key request)))

(defn fingerprinted
  [decision request]
  (core/with-request-fingerprint decision request))

(def empty-plan
  {:journal-entry nil :spaces-row nil :turn-order nil :canvas nil
   :idem-key nil :idem-row nil :thread-row nil
   :emit :decision-only :record-request? true})

(defn plan-replay
  "Same request id + same payload: Segment 1 already committed (it is atomic
   with the journal entry), so write nothing here and re-emit downstream."
  [journal-decision existing-thread]
  (assoc empty-plan
         :decision journal-decision
         :thread-row existing-thread
         :emit :full))

(defn plan-conflict
  "Same request id, different payload: the committed decision and facts stay
   untouched; the impostor gets its own '/conflict' decision row and must not
   overwrite the original request row."
  [gate]
  (assoc empty-plan
         :decision (:gate/decision gate)
         :record-request? false))

(defn plan-decision-only
  [decision]
  (assoc empty-plan
         :decision decision
         :journal-entry decision))

(defn plan-ordered-turn
  "Segment-1 plan for an accepted turn that appends to the space's order:
   order, count, and canvas are computed and written on the same task that
   read them — the read→hop→write race (SP-04) is structurally impossible."
  [decision request existing-thread existing-turn-order]
  (let [turn-event (decision-turn-event decision)
        turn-id (turn-id-from-event turn-event)
        order (add-turn-id existing-turn-order turn-id)
        thread-event (space-event request existing-thread)
        row (bump-thread-turn-count (thread-row existing-thread thread-event) order)]
    (assoc empty-plan
           :decision decision
           :journal-entry decision
           :spaces-row row
           :turn-order order
           :canvas (chat-canvas-projection row order (turn-row turn-event nil))
           :thread-row row
           :emit :full)))

(defn refreshed-create-canvas
  "A (duplicate) create must refresh title/status, never wipe the turn order
   or the latest turn the canvas already holds."
  [thread-row existing-canvas]
  (if existing-canvas
    (assoc existing-canvas
           :title (:title thread-row)
           :status (:status thread-row)
           :turn-count (:turn-count thread-row)
           :updated-at-ms (:updated-at-ms thread-row))
    (chat-canvas-projection thread-row nil nil)))

(defn plan-thread-create
  [gate journal-decision request existing-thread existing-turn-order existing-canvas]
  (case (:gate/status gate)
    :conflict (plan-conflict gate)
    :replay (plan-replay journal-decision existing-thread)
    (let [decision (fingerprinted (interpret-thread-create request existing-thread) request)]
      (if (decision-accepted? decision)
        (let [thread-event (decision-thread-event decision)
              row (thread-row existing-thread thread-event)]
          (assoc empty-plan
                 :decision decision
                 :journal-entry decision
                 :spaces-row row
                 :canvas (refreshed-create-canvas row existing-canvas)
                 :thread-row row
                 :emit :full))
        (plan-decision-only decision)))
    ))

(defn plan-compose-fresh
  [request existing-thread existing-turn-order material-hash]
  (let [decision (fingerprinted (interpret-compose-and-send request existing-thread) request)]
    (if (decision-accepted? decision)
      (let [thread-event (decision-thread-event decision)
            turn-event (decision-turn-event decision)
            bundle (context-bundle-row request turn-event)
            llm-request (space->llm-turn-run-request request turn-event bundle)
            turn-id (turn-id-from-event turn-event)
            order (add-turn-id existing-turn-order turn-id)
            row (bump-thread-turn-count (thread-row existing-thread thread-event) order)]
        (assoc empty-plan
               :decision decision
               :journal-entry decision
               :spaces-row row
               :turn-order order
               :canvas (chat-canvas-projection row order (turn-row turn-event (:context-bundle/id bundle)))
               :idem-key (idempotency-lookup-key request)
               :idem-row (assoc (dedupe-row request decision bundle llm-request)
                                :material/hash material-hash)
               :thread-row row
               :emit :full))
      (plan-decision-only decision))))

(defn plan-compose-and-send
  [gate journal-decision request existing-thread existing-turn-order idem-row]
  (case (:gate/status gate)
    :conflict (plan-conflict gate)
    :replay (plan-replay journal-decision existing-thread)
    (let [material-hash (send-material-hash request)]
      (cond
        (and (idempotency-hit? idem-row)
             (= material-hash (:material/hash idem-row)))
        (plan-decision-only (fingerprinted (idempotent-decision request idem-row) request))

        (idempotency-hit? idem-row)
        (plan-decision-only
          (fingerprinted (idempotency-conflict-decision request idem-row material-hash) request))

        :else (plan-compose-fresh request existing-thread existing-turn-order material-hash)))))

(defn plan-fork-from-span
  [gate journal-decision request existing-thread existing-turn-order]
  (case (:gate/status gate)
    :conflict (plan-conflict gate)
    :replay (plan-replay journal-decision existing-thread)
    (let [decision (fingerprinted (interpret-fork-from-span request existing-thread) request)]
      (if (decision-accepted? decision)
        (let [thread-event (decision-thread-event decision)
              turn-event (decision-turn-event decision)
              turn-id (turn-id-from-event turn-event)
              order (add-turn-id existing-turn-order turn-id)
              row (bump-thread-turn-count (thread-row existing-thread thread-event) order)]
          ;; as-built: fork materializes no chat canvas for the child (residue)
          (assoc empty-plan
                 :decision decision
                 :journal-entry decision
                 :spaces-row row
                 :turn-order order
                 :thread-row row
                 :emit :full))
        (plan-decision-only decision)))))

(defn plan-control-turn
  [gate journal-decision request existing-thread existing-turn-order]
  (case (:gate/status gate)
    :conflict (plan-conflict gate)
    :replay (plan-replay journal-decision existing-thread)
    (let [decision (fingerprinted (interpret-control-turn request existing-thread) request)]
      (if (decision-accepted? decision)
        (plan-ordered-turn decision request existing-thread existing-turn-order)
        (plan-decision-only decision)))))

(defn plan-space-turn
  [gate journal-decision request existing-thread existing-turn-order]
  (case (:gate/status gate)
    :conflict (plan-conflict gate)
    :replay (plan-replay journal-decision existing-thread)
    (let [decision (fingerprinted (interpret-space-turn request existing-thread) request)]
      (if (decision-accepted? decision)
        (plan-ordered-turn decision request existing-thread existing-turn-order)
        (plan-decision-only decision)))))

(defn plan-invalid-type
  [gate journal-decision request]
  (case (:gate/status gate)
    :conflict (plan-conflict gate)
    :replay (plan-replay journal-decision nil)
    (plan-decision-only
      (fingerprinted (rejected-decision request :request/type-invalid) request))))

(defn emit-fan-out?
  "Full fact fan-out only for accepted decisions that minted facts: an
   idempotency replay's decision points at the ORIGINAL send's facts and must
   never (re-)emit writes for them under the replay request."
  [plan]
  (let [decision (:decision plan)]
    (and (= :full (:emit plan))
         (decision-accepted? decision)
         (not (:idempotency/replayed? decision)))))

(defn resolve-patch-proposal
  "Resolution row to write, or nil when nothing may be mutated: resolutions
   never invent proposals (no phantom rows under nil/unknown ids), the first
   resolution wins, and a replayed resolution is a no-op."
  [existing request turn-event]
  (let [proposal-id (request-patch-proposal-id request)]
    (cond
      (blank-string? proposal-id) nil
      (nil? existing) nil
      (= (:resolution/request-id existing) (:request/id request)) nil
      (not= :pending (:status existing)) nil
      :else (apply-patch-decision existing request turn-event))))

;; ── Typed server-side observation bridge (prior-retro F1, SP-05, SP-07) ──

(defn observation-space-id
  [obs]
  (or (:space/id obs) (:llm-thread/id obs)))

(defn bridgeable-patch-observation?
  [obs]
  (and (patch-proposal-observation? obs)
       (not (blank-string? (observation-space-id obs)))
       (not (blank-string? (patch-proposal-id-from-observation obs)))))

(defn dropped-patch-observation?
  "Patch-like but unroutable (no space, no proposal id): never-drop demands a
   dead-letter instead of silence."
  [obs]
  (and (patch-proposal-observation? obs)
       (not (bridgeable-patch-observation? obs))))

(defn observation-dead-letter-key
  [obs]
  (let [run-id (:llm-turn-run/id obs)]
    (if (blank-string? run-id) "unknown-run" run-id)))

(def obs-dead-letter-limit 100)

(defn append-obs-dead-letter
  [existing obs]
  (let [entry (core/bounded-dead-letter
                :patch-proposal/unroutable obs
                {:context {:observation/id (:observation/id obs)
                           :llm-turn-run/id (:llm-turn-run/id obs)}})]
    (vec (take-last obs-dead-letter-limit (conj (vec existing) entry)))))

(defmodule space-kernel-module [setup topologies]
  (mirror-depot setup *llm-depot (get-module-name llm/llm-module) "*llm-depot")
  (mirror-depot setup *llm-control-depot (get-module-name llm/llm-module) "*llm-control-depot")
  (mirror-depot setup *llm-obs-depot (get-module-name llm/llm-module) "*llm-obs-depot")
  (declare-depot setup *space-action-depot (hash-by :routing/key))
  (let [n (stream-topology topologies "space-topology")]
    (declare-pstate n $$space-requests-by-id {String Object})
    (declare-pstate n $$space-decisions-by-id {String Object})
    (declare-pstate n $$space-events-by-id {String Object})
    ;; dedup spine: journal-key -> request-id -> fingerprinted decision,
    ;; colocated with $$spaces so the gate is atomic with Segment-1 writes
    (declare-pstate n $$space-request-journal
                    {String (map-schema String Object {:subindex? true})})
    (declare-pstate n $$spaces {String Object})
    (declare-pstate n $$space-graph {String Object})
    (declare-pstate n $$turns {String Object})
    (declare-pstate n $$turns-by-space {String Object})
    (declare-pstate n $$context-bundles {String Object})
    (declare-pstate n $$context-bundles-by-turn {String String})
    ;; space-scoped: space-id -> idempotency-key -> dedupe row (+ material
    ;; hash); colocation with the deciding task makes check-and-claim atomic
    (declare-pstate n $$send-by-idempotency
                    {String (map-schema String Object {:subindex? true})})
    (declare-pstate n $$llm-run-requests {String Object})
    (declare-pstate n $$llm-run-by-turn {String String})
    (declare-pstate n $$llm-controls {String Object})
    (declare-pstate n $$llm-control-by-turn {String String})
    (declare-pstate n $$objects {String Object})
    (declare-pstate n $$artifact-graph {String Object})
    (declare-pstate n $$artifact-graph-in {String Object})
    (declare-pstate n $$slices {String Object})
    (declare-pstate n $$overlays {String Object})
    (declare-pstate n $$derivatives {String Object})
    (declare-pstate n $$space-patch-proposals {String Object})
    ;; never-drop: unroutable patch observations land here, bounded per run
    (declare-pstate n $$space-obs-dead-letters {String Object})
    (declare-pstate n $$projection-chat-canvas {String Object})
    (declare-pstate n $$projection-object-detail {String Object})
    (declare-pstate n $$projection-object-relations {String Object})

    (<<sources n
      (source> *space-action-depot :> *request)
      (request-id *request :> *request-id)
      (request-type *request :> *request-type)
      (request-thread-id *request :> *raw-thread-id)
      (journal-key *raw-thread-id *request-id :> *thread-id)
      ;; ── Segment 1: one task, one transaction scope. The journal gate, the
      ;; idempotency check-and-claim, and every non-idempotent write (space
      ;; row, turn order, canvas) commit atomically at the next partitioner.
      (|hash *thread-id)
      (local-select> [(keypath *thread-id *request-id)] $$space-request-journal :> *journal-decision)
      (core/decision-dedup-gate *journal-decision *request :> *gate)
      (local-select> [(keypath *thread-id)] $$spaces :> *existing-thread)
      (local-select> [(keypath *thread-id)] $$turns-by-space :> *existing-turn-order)
      (<<cond
        (case> (= :space/create *request-type))
        (local-select> [(keypath *thread-id)] $$projection-chat-canvas :> *existing-canvas)
        (plan-thread-create *gate *journal-decision *request *existing-thread *existing-turn-order *existing-canvas :> *plan)

        (case> (= :turn/compose-and-send *request-type))
        (idempotency-lookup-key *request :> *idempotency-key)
        (local-select> [(keypath *thread-id *idempotency-key)] $$send-by-idempotency :> *idem-row)
        (plan-compose-and-send *gate *journal-decision *request *existing-thread *existing-turn-order *idem-row :> *plan)

        (case> (= :space/fork-from-span *request-type))
        (plan-fork-from-span *gate *journal-decision *request *existing-thread *existing-turn-order :> *plan)

        (case> (space-control-request? *request-type))
        (plan-control-turn *gate *journal-decision *request *existing-thread *existing-turn-order :> *plan)

        (case> (space-turn-request? *request-type))
        (plan-space-turn *gate *journal-decision *request *existing-thread *existing-turn-order :> *plan)

        (default>)
        (plan-invalid-type *gate *journal-decision *request :> *plan))

      ;; Segment-1 writes — atomic with each other and with the journal entry
      (get *plan :journal-entry :> *journal-entry)
      (<<if (some? *journal-entry)
        (local-transform> [(keypath *thread-id *request-id) (termval *journal-entry)] $$space-request-journal))
      (get *plan :spaces-row :> *spaces-row)
      (<<if (some? *spaces-row)
        (local-transform> [(keypath *thread-id) (termval *spaces-row)] $$spaces))
      (get *plan :turn-order :> *new-turn-order)
      (<<if (some? *new-turn-order)
        (local-transform> [(keypath *thread-id) (termval *new-turn-order)] $$turns-by-space))
      (get *plan :canvas :> *canvas)
      (<<if (some? *canvas)
        (local-transform> [(keypath *thread-id) (termval *canvas)] $$projection-chat-canvas))
      (get *plan :idem-row :> *plan-idem-row)
      (<<if (some? *plan-idem-row)
        (get *plan :idem-key :> *plan-idem-key)
        (local-transform> [(keypath *thread-id *plan-idem-key) (termval *plan-idem-row)] $$send-by-idempotency))

      ;; ── downstream: audit rows (idempotent termvals; impostor conflicts
      ;; never overwrite the committed request row) ──
      (get *plan :decision :> *decision)
      (get *plan :record-request? :> *record-request?)
      (<<if *record-request?
        (|hash *request-id)
        (local-transform> [(keypath *request-id) (termval *request)] $$space-requests-by-id))
      (decision-id *decision :> *decision-id)
      (|hash *decision-id)
      (local-transform> [(keypath *decision-id) (termval *decision)] $$space-decisions-by-id)

      ;; ── downstream: fact fan-out. A journal replay re-emits everything
      ;; (the first attempt may have died mid-tree); every write below is an
      ;; idempotent termval of replay-stable values or consumer-deduplicated
      ;; (the LLM intake gates run ids; fold-control dedups control ids). ──
      (emit-fan-out? *plan :> *fan-out?)
      (<<if *fan-out?
        (get *plan :thread-row :> *thread-row)
        (<<cond
          (case> (= :space/create *request-type))
          (decision-thread-event *decision :> *thread-event)
          (event-id *thread-event :> *event-id)
          (space-object-row *thread-row :> *thread-object)
          (object-row-id *thread-object :> *thread-object-id)
          (object-detail-projection *thread-object :> *thread-object-detail)
          (|hash *event-id)
          (local-transform> [(keypath *event-id) (termval *thread-event)] $$space-events-by-id)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)

          (case> (= :turn/compose-and-send *request-type))
          (decision-thread-event *decision :> *thread-event)
          (decision-turn-event *decision :> *turn-event)
          (decision-bundle-event *decision :> *bundle-event)
          (decision-llm-run-event *decision :> *llm-run-event)
          (event-id *thread-event :> *thread-event-id)
          (event-id *turn-event :> *turn-event-id)
          (event-id *bundle-event :> *bundle-event-id)
          (event-id *llm-run-event :> *llm-run-event-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (context-bundle-id-from-event *bundle-event :> *bundle-id)
          (context-bundle-row *request *turn-event :> *bundle)
          (space->llm-turn-run-request *request *turn-event *bundle :> *llm-request)
          (llm-run-id-from-request *llm-request :> *llm-run-id)
          (turn-row *turn-event *bundle-id :> *turn-row)
          (space-object-row *thread-row :> *thread-object)
          (turn-object-row *turn-row :> *turn-object)
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
          (|hash *thread-event-id)
          (local-transform> [(keypath *thread-event-id) (termval *thread-event)] $$space-events-by-id)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$space-events-by-id)
          (|hash *bundle-event-id)
          (local-transform> [(keypath *bundle-event-id) (termval *bundle-event)] $$space-events-by-id)
          (|hash *llm-run-event-id)
          (local-transform> [(keypath *llm-run-event-id) (termval *llm-run-event)] $$space-events-by-id)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$turns)
          (local-transform> [(keypath *turn-id) (termval *bundle-id)] $$context-bundles-by-turn)
          (local-transform> [(keypath *turn-id) (termval *llm-run-id)] $$llm-run-by-turn)
          (|hash *bundle-id)
          (local-transform> [(keypath *bundle-id) (termval *bundle)] $$context-bundles)
          (|hash *llm-run-id)
          (local-transform> [(keypath *llm-run-id) (termval *llm-request)] $$llm-run-requests)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)
          (local-select> [(keypath *thread-object-id)] $$projection-object-relations :> *thread-relations-existing)
          (add-projection-out-edge *thread-relations-existing *thread-object-id *thread-turn-edge :> *thread-relations)
          (local-transform> [(keypath *thread-object-id) (termval *thread-relations)] $$projection-object-relations)
          (local-transform> [(keypath *thread-turn-from *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph)
          (|hash *thread-turn-to)
          (local-transform> [(keypath *thread-turn-to *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph-in)
          (|hash *turn-object-id)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object)] $$objects)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *turn-object-id) (termval *turn-relations)] $$projection-object-relations)
          (local-transform> [(keypath *turn-bundle-from *turn-bundle-edge-id) (termval *turn-bundle-edge)] $$artifact-graph)
          (local-transform> [(keypath *turn-llm-run-from *turn-llm-run-edge-id) (termval *turn-llm-run-edge)] $$artifact-graph)
          (|hash *turn-bundle-to)
          (local-transform> [(keypath *turn-bundle-to *turn-bundle-edge-id) (termval *turn-bundle-edge)] $$artifact-graph-in)
          (|hash *turn-llm-run-to)
          (local-transform> [(keypath *turn-llm-run-to *turn-llm-run-edge-id) (termval *turn-llm-run-edge)] $$artifact-graph-in)
          (|hash *bundle-object-id)
          (local-transform> [(keypath *bundle-object-id) (termval *bundle-object)] $$objects)
          (local-transform> [(keypath *bundle-object-id) (termval *bundle-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *bundle-object-id) (termval *bundle-relations)] $$projection-object-relations)
          (local-transform> [(keypath *bundle-llm-run-from *bundle-llm-run-edge-id) (termval *bundle-llm-run-edge)] $$artifact-graph)
          (|hash *bundle-llm-run-to)
          (local-transform> [(keypath *bundle-llm-run-to *bundle-llm-run-edge-id) (termval *bundle-llm-run-edge)] $$artifact-graph-in)
          (|hash *llm-run-object-id)
          (local-transform> [(keypath *llm-run-object-id) (termval *llm-run-object)] $$objects)
          (local-transform> [(keypath *llm-run-object-id) (termval *llm-run-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *llm-run-object-id) (termval *llm-run-relations)] $$projection-object-relations)
          (|hash$$ *llm-depot *llm-run-id)
          (depot-partition-append! *llm-depot *llm-request :append-ack)

          (case> (= :space/fork-from-span *request-type))
          (decision-thread-event *decision :> *thread-event)
          (decision-turn-event *decision :> *turn-event)
          (decision-bundle-event *decision :> *bundle-event)
          (decision-llm-run-event *decision :> *llm-run-event)
          (event-id *thread-event :> *thread-event-id)
          (event-id *turn-event :> *turn-event-id)
          (event-id *bundle-event :> *bundle-event-id)
          (event-id *llm-run-event :> *llm-run-event-id)
          (space-id-from-event *thread-event :> *event-thread-id)
          (parent-thread-id-from-event *thread-event :> *parent-thread-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (context-bundle-id-from-event *bundle-event :> *bundle-id)
          (context-bundle-row *request *turn-event :> *bundle)
          (space->llm-turn-run-request *request *turn-event *bundle :> *llm-request)
          (llm-run-id-from-request *llm-request :> *llm-run-id)
          (turn-row *turn-event *bundle-id :> *turn-row)
          (slice-row *request *turn-event :> *slice-row)
          (slice-row-id *slice-row :> *slice-id)
          (|hash *thread-event-id)
          (local-transform> [(keypath *thread-event-id) (termval *thread-event)] $$space-events-by-id)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$space-events-by-id)
          (|hash *bundle-event-id)
          (local-transform> [(keypath *bundle-event-id) (termval *bundle-event)] $$space-events-by-id)
          (|hash *llm-run-event-id)
          (local-transform> [(keypath *llm-run-event-id) (termval *llm-run-event)] $$space-events-by-id)
          (<<if (has-parent-thread? *thread-event)
            (child-thread-edge *thread-event :> *child-edge)
            (|hash *parent-thread-id)
            (local-transform> [(keypath *parent-thread-id *event-thread-id) (termval *child-edge)] $$space-graph))
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$turns)
          (local-transform> [(keypath *turn-id) (termval *bundle-id)] $$context-bundles-by-turn)
          (local-transform> [(keypath *turn-id) (termval *llm-run-id)] $$llm-run-by-turn)
          (|hash *bundle-id)
          (local-transform> [(keypath *bundle-id) (termval *bundle)] $$context-bundles)
          (|hash *slice-id)
          (local-select> [(keypath *slice-id)] $$slices :> *existing-slice-row)
          (core/write-if-absent *existing-slice-row *slice-row :> *stored-slice-row)
          (local-transform> [(keypath *slice-id) (termval *stored-slice-row)] $$slices)
          (|hash *llm-run-id)
          (local-transform> [(keypath *llm-run-id) (termval *llm-request)] $$llm-run-requests)
          (|hash$$ *llm-depot *llm-run-id)
          (depot-partition-append! *llm-depot *llm-request :append-ack)

          (case> (space-control-request? *request-type))
          (decision-turn-event *decision :> *turn-event)
          (decision-llm-control-event *decision :> *control-event)
          (event-id *turn-event :> *turn-event-id)
          (event-id *control-event :> *control-event-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (space->llm-control-record *request *turn-event :> *control)
          (llm-control-run-id *control :> *control-run-id)
          (llm-control-id *control :> *control-id)
          (turn-row *turn-event nil :> *turn-row)
          (space-object-row *thread-row :> *thread-object)
          (turn-object-row *turn-row :> *turn-object)
          (thread-turn-edge *thread-row *turn-row :> *thread-turn-edge)
          (object-row-id *thread-object :> *thread-object-id)
          (object-row-id *turn-object :> *turn-object-id)
          (artifact-edge-id *thread-turn-edge :> *thread-turn-edge-id)
          (artifact-edge-from *thread-turn-edge :> *thread-turn-from)
          (artifact-edge-to *thread-turn-edge :> *thread-turn-to)
          (object-detail-projection *thread-object :> *thread-object-detail)
          (object-detail-projection *turn-object :> *turn-object-detail)
          (one-incoming-relation-projection *turn-object-id *thread-turn-edge :> *turn-relations)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$space-events-by-id)
          (|hash *control-event-id)
          (local-transform> [(keypath *control-event-id) (termval *control-event)] $$space-events-by-id)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$turns)
          (local-transform> [(keypath *turn-id) (termval *control-id)] $$llm-control-by-turn)
          (|hash *control-id)
          (local-transform> [(keypath *control-id) (termval *control)] $$llm-controls)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)
          (local-select> [(keypath *thread-object-id)] $$projection-object-relations :> *thread-relations-existing)
          (add-projection-out-edge *thread-relations-existing *thread-object-id *thread-turn-edge :> *thread-relations)
          (local-transform> [(keypath *thread-object-id) (termval *thread-relations)] $$projection-object-relations)
          (local-transform> [(keypath *thread-turn-from *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph)
          (|hash *thread-turn-to)
          (local-transform> [(keypath *thread-turn-to *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph-in)
          (|hash *turn-object-id)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object)] $$objects)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *turn-object-id) (termval *turn-relations)] $$projection-object-relations)
          (|hash$$ *llm-control-depot *control-run-id)
          (depot-partition-append! *llm-control-depot *control :append-ack)

          (case> (space-turn-request? *request-type))
          (decision-turn-event *decision :> *turn-event)
          (event-id *turn-event :> *turn-event-id)
          (turn-id-from-event *turn-event :> *turn-id)
          (turn-row *turn-event nil :> *turn-row)
          (space-object-row *thread-row :> *thread-object)
          (turn-object-row *turn-row :> *turn-object)
          (thread-turn-edge *thread-row *turn-row :> *thread-turn-edge)
          (object-row-id *thread-object :> *thread-object-id)
          (object-row-id *turn-object :> *turn-object-id)
          (artifact-edge-id *thread-turn-edge :> *thread-turn-edge-id)
          (artifact-edge-from *thread-turn-edge :> *thread-turn-from)
          (artifact-edge-to *thread-turn-edge :> *thread-turn-to)
          (object-detail-projection *thread-object :> *thread-object-detail)
          (object-detail-projection *turn-object :> *turn-object-detail)
          (one-incoming-relation-projection *turn-object-id *thread-turn-edge :> *turn-relations)
          (|hash *turn-event-id)
          (local-transform> [(keypath *turn-event-id) (termval *turn-event)] $$space-events-by-id)
          (|hash *turn-id)
          (local-transform> [(keypath *turn-id) (termval *turn-row)] $$turns)
          (|hash *thread-object-id)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object)] $$objects)
          (local-transform> [(keypath *thread-object-id) (termval *thread-object-detail)] $$projection-object-detail)
          (local-select> [(keypath *thread-object-id)] $$projection-object-relations :> *thread-relations-existing)
          (add-projection-out-edge *thread-relations-existing *thread-object-id *thread-turn-edge :> *thread-relations)
          (local-transform> [(keypath *thread-object-id) (termval *thread-relations)] $$projection-object-relations)
          (local-transform> [(keypath *thread-turn-from *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph)
          (|hash *thread-turn-to)
          (local-transform> [(keypath *thread-turn-to *thread-turn-edge-id) (termval *thread-turn-edge)] $$artifact-graph-in)
          (|hash *turn-object-id)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object)] $$objects)
          (local-transform> [(keypath *turn-object-id) (termval *turn-object-detail)] $$projection-object-detail)
          (local-transform> [(keypath *turn-object-id) (termval *turn-relations)] $$projection-object-relations)
          (<<if (= :turn/slice-create *request-type)
            (slice-row *request *turn-event :> *slice-row)
            (slice-object-row *slice-row :> *slice-object)
            (slice-row-id *slice-row :> *slice-id)
            (object-row-id *slice-object :> *slice-object-id)
            (object-detail-projection *slice-object :> *slice-object-detail)
            (|hash *slice-id)
            (local-select> [(keypath *slice-id)] $$slices :> *existing-slice-row)
            (core/write-if-absent *existing-slice-row *slice-row :> *stored-slice-row)
            (local-transform> [(keypath *slice-id) (termval *stored-slice-row)] $$slices)
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
              (local-transform> [(keypath *source-edge-from *source-edge-id) (termval *source-edge)] $$artifact-graph)
              (|hash *source-edge-to)
              (local-transform> [(keypath *source-edge-to *source-edge-id) (termval *source-edge)] $$artifact-graph-in)
              (local-transform> [(keypath *source-edge-to) (termval *source-target-relations)] $$projection-object-relations)))
          (<<if (= :turn/comment-create *request-type)
            (overlay-row *request *turn-event :> *overlay-row)
            (overlay-object-row *overlay-row :> *overlay-object)
            (overlay-row-id *overlay-row :> *overlay-id)
            (object-row-id *overlay-object :> *overlay-object-id)
            (object-detail-projection *overlay-object :> *overlay-object-detail)
            (|hash *overlay-id)
            (local-select> [(keypath *overlay-id)] $$overlays :> *existing-overlay-row)
            (core/write-if-absent *existing-overlay-row *overlay-row :> *stored-overlay-row)
            (local-transform> [(keypath *overlay-id) (termval *stored-overlay-row)] $$overlays)
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
              (local-transform> [(keypath *source-edge-from *source-edge-id) (termval *source-edge)] $$artifact-graph)
              (|hash *source-edge-to)
              (local-transform> [(keypath *source-edge-to *source-edge-id) (termval *source-edge)] $$artifact-graph-in)
              (local-transform> [(keypath *source-edge-to) (termval *source-target-relations)] $$projection-object-relations)))
          (<<if (= :turn/derivative-create *request-type)
            (derivative-row *request *turn-event :> *derivative-row)
            (derivative-object-row *derivative-row :> *derivative-object)
            (derivative-row-id *derivative-row :> *derivative-id)
            (object-row-id *derivative-object :> *derivative-object-id)
            (object-detail-projection *derivative-object :> *derivative-object-detail)
            (|hash *derivative-id)
            (local-select> [(keypath *derivative-id)] $$derivatives :> *existing-derivative-row)
            (core/write-if-absent *existing-derivative-row *derivative-row :> *stored-derivative-row)
            (local-transform> [(keypath *derivative-id) (termval *stored-derivative-row)] $$derivatives)
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
              (local-transform> [(keypath *source-edge-from *source-edge-id) (termval *source-edge)] $$artifact-graph)
              (|hash *source-edge-to)
              (local-transform> [(keypath *source-edge-to *source-edge-id) (termval *source-edge)] $$artifact-graph-in)
              (local-transform> [(keypath *source-edge-to) (termval *source-target-relations)] $$projection-object-relations)))
          (<<if (= :turn/patch-proposal-create *request-type)
            (request-patch-proposal-id *request :> *patch-proposal-id)
            (<<if (not (blank-string? *patch-proposal-id))
              (patch-proposal-row-from-request *request :> *patch-proposal)
              (|hash *patch-proposal-id)
              (local-select> [(keypath *patch-proposal-id)] $$space-patch-proposals :> *existing-patch-proposal)
              (core/write-if-absent *existing-patch-proposal *patch-proposal :> *stored-patch-proposal)
              (local-transform> [(keypath *patch-proposal-id) (termval *stored-patch-proposal)] $$space-patch-proposals)))
          (<<if (patch-decision-request? *request-type)
            (request-patch-proposal-id *request :> *resolve-proposal-id)
            (<<if (not (blank-string? *resolve-proposal-id))
              (|hash *resolve-proposal-id)
              (local-select> [(keypath *resolve-proposal-id)] $$space-patch-proposals :> *existing-resolution-target)
              (resolve-patch-proposal *existing-resolution-target *request *turn-event :> *resolved-proposal)
              (<<if (some? *resolved-proposal)
                (local-transform> [(keypath *resolve-proposal-id) (termval *resolved-proposal)] $$space-patch-proposals))))))

      ;; ── typed server-side observation bridge: ONLY patch-like observations
      ;; become pending proposals (no decision, no turn — spec op 4); a
      ;; redelivered observation can never duplicate a proposal or reset a
      ;; resolved one; unroutable patch observations dead-letter (never-drop).
      (source> *llm-obs-depot :> *obs)
      (<<if (dropped-patch-observation? *obs)
        (observation-dead-letter-key *obs :> *dead-key)
        (|hash *dead-key)
        (local-select> [(keypath *dead-key)] $$space-obs-dead-letters :> *existing-dead-letters)
        (append-obs-dead-letter *existing-dead-letters *obs :> *dead-letters)
        (local-transform> [(keypath *dead-key) (termval *dead-letters)] $$space-obs-dead-letters))
      (<<if (bridgeable-patch-observation? *obs)
        (patch-proposal-id-from-observation *obs :> *obs-proposal-id)
        (patch-proposal-row *obs :> *obs-proposal-row)
        (|hash *obs-proposal-id)
        (local-select> [(keypath *obs-proposal-id)] $$space-patch-proposals :> *existing-obs-proposal)
        (core/write-if-absent *existing-obs-proposal *obs-proposal-row :> *stored-obs-proposal)
        (local-transform> [(keypath *obs-proposal-id) (termval *stored-obs-proposal)] $$space-patch-proposals)))))

(defn start-space-runtime!
  []
  (let [ipc (create-ipc)
        llm-module-name (get-module-name llm/llm-module)
        module-name (get-module-name space-kernel-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc llm/llm-module launch-opts)
    (launch-module! ipc space-kernel-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :llm-module-name llm-module-name
     :space-action-depot (foreign-depot ipc module-name "*space-action-depot")
     :space-requests-by-id (foreign-pstate ipc module-name "$$space-requests-by-id")
     :space-decisions-by-id (foreign-pstate ipc module-name "$$space-decisions-by-id")
     :space-events-by-id (foreign-pstate ipc module-name "$$space-events-by-id")
     :space-request-journal (foreign-pstate ipc module-name "$$space-request-journal")
     :space-obs-dead-letters (foreign-pstate ipc module-name "$$space-obs-dead-letters")
     :spaces (foreign-pstate ipc module-name "$$spaces")
     :space-graph (foreign-pstate ipc module-name "$$space-graph")
     :turns (foreign-pstate ipc module-name "$$turns")
     :turns-by-space (foreign-pstate ipc module-name "$$turns-by-space")
     :context-bundles (foreign-pstate ipc module-name "$$context-bundles")
     :context-bundles-by-turn (foreign-pstate ipc module-name "$$context-bundles-by-turn")
     :send-by-idempotency (foreign-pstate ipc module-name "$$send-by-idempotency")
     :llm-run-requests (foreign-pstate ipc module-name "$$llm-run-requests")
     :llm-run-by-turn (foreign-pstate ipc module-name "$$llm-run-by-turn")
     :llm-controls (foreign-pstate ipc module-name "$$llm-controls")
     :llm-control-by-turn (foreign-pstate ipc module-name "$$llm-control-by-turn")
     :objects (foreign-pstate ipc module-name "$$objects")
     :artifact-graph (foreign-pstate ipc module-name "$$artifact-graph")
     :artifact-graph-in (foreign-pstate ipc module-name "$$artifact-graph-in")
     :slices (foreign-pstate ipc module-name "$$slices")
     :overlays (foreign-pstate ipc module-name "$$overlays")
     :derivatives (foreign-pstate ipc module-name "$$derivatives")
     :space-patch-proposals (foreign-pstate ipc module-name "$$space-patch-proposals")
     :projection-chat-canvas (foreign-pstate ipc module-name "$$projection-chat-canvas")
     :projection-object-detail (foreign-pstate ipc module-name "$$projection-object-detail")
     :projection-object-relations (foreign-pstate ipc module-name "$$projection-object-relations")
     :llm-depot (foreign-depot ipc llm-module-name "*llm-depot")
     :llm-claim-depot (foreign-depot ipc llm-module-name "*llm-claim-depot")
     :llm-obs-depot (foreign-depot ipc llm-module-name "*llm-obs-depot")
     :llm-control-depot (foreign-depot ipc llm-module-name "*llm-control-depot")
     :llm-threads (foreign-pstate ipc llm-module-name "$$llm-threads")
     :llm-thread-by-space (foreign-pstate ipc llm-module-name "$$llm-thread-by-space")
     :llm-turn-runs (foreign-pstate ipc llm-module-name "$$llm-turn-runs")
     :llm-turn-runs-by-thread (foreign-pstate ipc llm-module-name "$$llm-turn-runs-by-thread")
     :llm-turn-run-by-turn (foreign-pstate ipc llm-module-name "$$llm-turn-run-by-turn")
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

(defn close-space-runtime!
  [runtime]
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-space-action!
  ([runtime request]
   (append-space-action! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:space-action-depot runtime) request ack-level)
   request))

(defn append-llm-observation!
  "Single append per logical operation: the observation goes to the LLM
   observation depot only. Patch-like observations become pending space patch
   proposals SERVER-SIDE — the space topology sources the observation depot —
   so a client crash between appends can no longer strand the proposal, and
   non-patch observations can never mint space meaning (typed bridge)."
  ([runtime obs]
   (append-llm-observation! runtime obs :append-ack))
  ([runtime obs ack-level]
   (llm/append-observation! runtime obs ack-level)
   obs))

(defn select-pstate-one
  [pstate path]
  (first (foreign-select path pstate)))

(defn read-request
  [runtime request-id]
  (select-pstate-one (:space-requests-by-id runtime) [(keypath request-id)]))

(defn read-decision
  [runtime request-id]
  (select-pstate-one (:space-decisions-by-id runtime)
                     [(keypath (decision-id-for-request-id request-id))]))

(defn read-event
  [runtime event-id]
  (select-pstate-one (:space-events-by-id runtime) [(keypath event-id)]))

(defn read-space
  [runtime space-id]
  (select-pstate-one (:spaces runtime) [(keypath space-id)]))

(defn read-turn
  [runtime turn-id]
  (select-pstate-one (:turns runtime) [(keypath turn-id)]))

(defn read-turns-by-space
  [runtime space-id]
  (or (select-pstate-one (:turns-by-space runtime) [(keypath space-id)])
      []))

(defn read-space-graph
  [runtime space-id]
  (or (select-pstate-one (:space-graph runtime) [(keypath space-id)])
      {}))

(defn read-context-bundle
  [runtime context-bundle-id]
  (select-pstate-one (:context-bundles runtime) [(keypath context-bundle-id)]))

(defn read-context-bundle-by-turn
  [runtime turn-id]
  (select-pstate-one (:context-bundles-by-turn runtime) [(keypath turn-id)]))

(defn read-send-by-idempotency
  "Idempotency keys are space-scoped (atomic check-and-claim requires
   colocation with the deciding task), so reads take the space id too."
  [runtime space-id idempotency-key]
  (select-pstate-one (:send-by-idempotency runtime)
                     [(keypath space-id idempotency-key)]))

(defn read-request-journal
  [runtime space-id request-id]
  (select-pstate-one (:space-request-journal runtime)
                     [(keypath space-id request-id)]))

(defn read-conflict-decision
  "The conflict decision a request-id impostor receives; lives under its own
   '/conflict' id so it can never alias the committed decision."
  [runtime request-id]
  (select-pstate-one (:space-decisions-by-id runtime)
                     [(keypath (str (decision-id-for-request-id request-id) "/conflict"))]))

(defn read-space-obs-dead-letters
  [runtime dead-letter-key]
  (or (select-pstate-one (:space-obs-dead-letters runtime) [(keypath dead-letter-key)])
      []))

(defn read-llm-run-request
  [runtime llm-turn-run-id]
  (select-pstate-one (:llm-run-requests runtime) [(keypath llm-turn-run-id)]))

(defn read-llm-run-by-turn
  [runtime turn-id]
  (select-pstate-one (:llm-run-by-turn runtime) [(keypath turn-id)]))

(defn read-llm-control
  [runtime control-id]
  (select-pstate-one (:llm-controls runtime) [(keypath control-id)]))

(defn read-llm-control-by-turn
  [runtime turn-id]
  (select-pstate-one (:llm-control-by-turn runtime) [(keypath turn-id)]))

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
  [runtime space-id]
  (select-pstate-one (:projection-chat-canvas runtime) [(keypath space-id)]))

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
  (select-pstate-one (:space-patch-proposals runtime) [(keypath patch-proposal-id)]))

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

(defn await-space
  [runtime space-id pred]
  (await-materialized #(read-space runtime space-id) pred))

(defn await-turn
  [runtime turn-id pred]
  (await-materialized #(read-turn runtime turn-id) pred))
