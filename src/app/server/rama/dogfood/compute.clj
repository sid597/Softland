(ns app.server.rama.dogfood.compute
  (:use [com.rpl.rama]
        [com.rpl.rama.path]
        [com.rpl.rama.ops])
  (:require [app.server.rama.core :as core]
            [clojure.string :as str]
            [com.rpl.rama.test :refer [create-ipc launch-module!]])
  (:import (clojure.lang Keyword)
           (com.rpl.rama.integration TaskGlobalContext TaskGlobalObject)
           (java.io BufferedReader File InputStreamReader)
           (java.util UUID)
           (java.util.concurrent ConcurrentHashMap Executors ThreadFactory TimeUnit)
           (java.util.concurrent.atomic AtomicInteger)))

(def schema-version 1)
(def pending-task-id "local")
(def default-stdout-tail-limit 200)
(def default-stderr-tail-limit 200)
(def default-error-limit 50)
(def default-command-timeout-ms 10000)
(def default-executor-reconcile-delay-ms 50)
(def default-executor-command-timeout-ms default-command-timeout-ms)

(def compute-target-kinds
  #{:workspace})

(def terminal-statuses
  #{:succeeded :failed})

(def observation-types
  #{:started :stdout :stderr :exit})

(defn now-ms [] (core/now-ms))
(defn random-id [prefix] (core/random-id prefix))

(defn default-compute-actor
  []
  (update (core/default-actor)
          :actor/capabilities
          (fn [caps] (conj (set caps) :compute/run))))

(defn compute-routing-key
  [run-id]
  [:compute/run run-id])

(defn normalize-argv
  [argv]
  (vec (map str argv)))

(defn normalize-executor-task-id
  [task-id]
  (when (some? task-id)
    (str task-id)))

(defn opts-executor-task-id
  [opts]
  (normalize-executor-task-id
    (or (:executor-task-id opts)
        (:executor/task-id opts))))

(defn run-command-request
  "Build the ActionRequest entering the compute Rama module."
  [argv & [opts]]
  (let [run-id (or (:run-id opts) (:run/id opts) (random-id "run"))
        request-id (or (:request-id opts) (:request/id opts) (random-id "req"))
        time-ms (or (:time-ms opts) (now-ms))
        cwd (str (or (:cwd opts) (System/getProperty "user.dir")))
        argv (normalize-argv argv)
        actor (merge (default-compute-actor) (:actor opts))
        branch (merge {:branch/id core/default-branch-id} (:branch opts))
        context (merge (core/default-context) (:context opts))
        causal (merge (core/default-causal) (:causal opts))
        executor-task-id (opts-executor-task-id opts)
        provenance (or (:provenance opts)
                       {:source/type :manual
                        :source/ref nil})]
    (cond-> {:run/id run-id
             :request/id request-id
             :request/type :compute/run-command
             :request/time-ms time-ms
             :request/schema-version schema-version
             :routing/key (compute-routing-key run-id)
             :actor actor
             :branch branch
             :context context
             :target {:target/kind :workspace
                      :target/id (or (:workspace-id opts) "local")
                      :target/address {:cwd cwd}}
             :action {:action/type :compute/run-command
                      :action/capability :compute/run
                      :action/params {:argv argv
                                      :cwd cwd}}
             :payload (cond-> {:run/id run-id
                                :argv argv
                                :cwd cwd}
                        executor-task-id
                        (assoc :executor/task-id executor-task-id))
             :causal causal
             :provenance provenance}
      (:proposed-event-id opts)
      (assoc :proposed/event-id (:proposed-event-id opts)))))

(def required-request-keys
  [:run/id :request/id :request/type :request/time-ms :request/schema-version
   :routing/key :actor :branch :context :target :action :payload :causal
   :provenance])

(defn blank-string?
  [x]
  (or (not (string? x)) (str/blank? x)))

(defn request-validation-errors
  [request]
  (let [run-id (:run/id request)
        payload-run-id (get-in request [:payload :run/id])
        request-type (:request/type request)
        action-type (get-in request [:action :action/type])
        argv (get-in request [:payload :argv])
        cwd (get-in request [:payload :cwd])]
    (cond-> []
      (not (map? request))
      (conj {:type :request/not-map})

      (and (map? request) (not-every? #(contains? request %) required-request-keys))
      (conj {:type :request/missing-envelope-key
             :missing (vec (remove #(contains? request %) required-request-keys))})

      (and (map? request) (not= :compute/run-command request-type))
      (conj {:type :request/type-invalid
             :value request-type})

      (and (map? request) (not= request-type action-type))
      (conj {:type :request/action-type-drift
             :request/type request-type
             :action/type action-type})

      (and (map? request) (blank-string? run-id))
      (conj {:type :run/id-invalid
             :value run-id})

      (and (map? request) (not= run-id payload-run-id))
      (conj {:type :run/id-payload-drift
             :run/id run-id
             :payload/run-id payload-run-id})

      (and (map? request) (not= (compute-routing-key run-id) (:routing/key request)))
      (conj {:type :routing/key-invalid
             :value (:routing/key request)
             :expected (compute-routing-key run-id)})

      (and (map? request)
           (not (contains? core/actor-types (get-in request [:actor :actor/type]))))
      (conj {:type :actor/invalid-type
             :value (get-in request [:actor :actor/type])})

      (and (map? request)
           (not (contains? compute-target-kinds (get-in request [:target :target/kind]))))
      (conj {:type :target/invalid-kind
             :value (get-in request [:target :target/kind])})

      (and (map? request) (not= :compute/run (get-in request [:action :action/capability])))
      (conj {:type :action/capability-invalid
             :value (get-in request [:action :action/capability])})

      (and (map? request) (not (and (vector? argv) (seq argv) (every? string? argv))))
      (conj {:type :payload/argv-invalid
             :value argv})

      (and (map? request) (blank-string? cwd))
      (conj {:type :payload/cwd-invalid
             :value cwd})

      (and (map? request) (nil? (get-in request [:branch :branch/id])))
      (conj {:type :branch/missing-id}))))

(defn request-errors? [errors] (boolean (seq errors)))
(defn request-run-id [request] (:run/id request))
(defn decision-run-id [decision] (:run/id decision))
(defn decision-accepted? [decision] (= :accepted (:decision/status decision)))

(defn authorized-request?
  [request]
  (let [required (get-in request [:action :action/capability])
        actor-type (get-in request [:actor :actor/type])
        caps (set (get-in request [:actor :actor/capabilities]))]
    (or (= :system actor-type)
        (contains? caps required))))

(defn decision-id-for-run-id
  [run-id]
  (str run-id "/decision"))

(defn run-event
  [request]
  {:event/id (or (:proposed/event-id request)
                 (str (:request/id request) "/event"))
   :event/type :compute/run-requested
   :event/time-ms (:request/time-ms request)
   :event/schema-version schema-version
   :actor (:actor request)
   :branch (:branch request)
   :context (:context request)
   :target (:target request)
   :action (:action request)
   :payload (:payload request)
   :causal (merge {:parents []
                   :correlation/id (:request/id request)
                   :intent/id (get-in request [:causal :intent/id])}
                  (:causal request))
   :ordering {:key (compute-routing-key (:run/id request))}
   :policy {:required-capabilities #{:compute/run}
            :visibility :private}
   :provenance {:source/type :action-request
                :source/ref (:request/id request)}})

(defn accepted-decision
  [request event]
  {:decision/id (decision-id-for-run-id (:run/id request))
   :decision/status :accepted
   :run/id (:run/id request)
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id (:event/id event)
   :event event
   :decided-at (now-ms)})

(defn rejected-decision
  [request reason & [errors]]
  {:decision/id (decision-id-for-run-id (:run/id request))
   :decision/status :rejected
   :run/id (:run/id request)
   :request/id (:request/id request)
   :request/type (:request/type request)
   :routing/key (:routing/key request)
   :event/id nil
   :decision/reason reason
   :errors (vec errors)
   :decided-at (now-ms)})

(defn interpret-run-command-request
  [request]
  (let [errors (request-validation-errors request)]
    (cond
      (seq errors) (rejected-decision request :request-invalid errors)
      (not (authorized-request? request)) (rejected-decision request :actor-not-authorized)
      :else (accepted-decision request (run-event request)))))

(defn initial-run-row
  [decision]
  (let [event (:event decision)
        payload (:payload event)
        time-ms (:event/time-ms event)]
    {:run/id (:run/id decision)
     :request/id (:request/id decision)
     :decision/id (:decision/id decision)
     :status :pending
     :argv (:argv payload)
     :cwd (:cwd payload)
     :executor/task-id (normalize-executor-task-id (:executor/task-id payload))
     :created-at time-ms
     :updated-at time-ms
     :claimed-by nil
     :claim-token nil
     :claimed-at nil
     :pid nil
     :started-at nil
     :finished-at nil
     :exit-code nil
     :last-seq -1
     :obs-buffer {}
     :stdout-tail []
     :stderr-tail []
     :observation-errors []}))

(defn assign-executor-task
  [run-row current-task-id]
  (let [executor-task-id (or (:executor/task-id run-row)
                             (normalize-executor-task-id current-task-id))]
    (assoc run-row :executor/task-id executor-task-id)))

(defn run-executor-task-id
  [run-row]
  (:executor/task-id run-row))

(defn pending-entry
  [run-row]
  {:run/id (:run/id run-row)
   :request/id (:request/id run-row)
   :executor/task-id (:executor/task-id run-row)
   :created-at (:created-at run-row)
   :status (:status run-row)})

(defn run-view
  [run-row]
  (select-keys run-row
               [:run/id :request/id :status :argv :cwd :pid :started-at
                :finished-at :exit-code :created-at :updated-at
                :executor/task-id :claimed-by
                :stdout-tail :stderr-tail :observation-errors]))

(defn claim-record
  [run-id executor-id & [opts]]
  (cond-> {:run/id run-id
           :claim/id (or (:claim-id opts) (random-id "claim"))
           :executor/id executor-id
           :claim-token (or (:claim-token opts) (str (UUID/randomUUID)))
           :claimed-at (or (:claimed-at opts) (now-ms))}
    (opts-executor-task-id opts)
    (assoc :executor/task-id (opts-executor-task-id opts))))

(defn claim-run-id [claim] (:run/id claim))
(defn observation-run-id [obs] (:run/id obs))

(defn valid-claim?
  [claim]
  (and (map? claim)
       (not (blank-string? (:run/id claim)))
       (not (blank-string? (:executor/id claim)))
       (not (blank-string? (:claim-token claim)))))

(defn grantable-claim?
  [run-row claim]
  (and (valid-claim? claim)
       (= :pending (:status run-row))
       (= (:run/id run-row) (:run/id claim))
       (= (:executor/task-id run-row) (:executor/task-id claim))))

(defn grant-claim
  [run-row claim]
  (let [t (or (:claimed-at claim) (now-ms))]
    (assoc run-row
           :status :launching
           :claimed-by (:executor/id claim)
           :claim-token (:claim-token claim)
           :claimed-at t
           :updated-at t)))

(defn claim-state
  [run-row claim]
  (cond
    (nil? run-row) :not-yet-processed
    (and (= :launching (:status run-row))
         (= (:executor/id claim) (:claimed-by run-row))
         (= (:claim-token claim) (:claim-token run-row))) :granted-to-us
    (= :pending (:status run-row)) :not-yet-processed
    :else :conflict-or-past))

(defn append-bounded
  [xs x limit]
  (let [v (conj (vec xs) x)
        c (count v)]
    (if (> c limit)
      (subvec v (- c limit))
      v)))

(defn observation-error
  [reason run-row obs]
  {:reason reason
   :run/id (:run/id run-row)
   :observation/type (:observation/type obs)
   :sequence (:sequence obs)
   :observed-at (or (:observed-at obs) (now-ms))})

(defn add-observation-error
  [run-row reason obs]
  (let [t (or (:observed-at obs) (now-ms))]
    (-> run-row
        (update :observation-errors
                append-bounded
                (observation-error reason run-row obs)
                default-error-limit)
        (assoc :updated-at t))))

(defn authorized-observation?
  [run-row obs]
  (and (= (:run/id run-row) (:run/id obs))
       (= (:claim-token run-row) (:claim-token obs))
       (not (contains? terminal-statuses (:status run-row)))))

(defn valid-observation-sequence?
  [obs]
  (let [seq-id (:sequence obs)]
    (and (integer? seq-id) (not (neg? seq-id)))))

(defn apply-observation-effect
  [run-row obs]
  (let [t (or (:observed-at obs) (now-ms))]
    (case (:observation/type obs)
      :started
      (assoc run-row
             :status :running
             :pid (:pid obs)
             :started-at t
             :updated-at t)

      :stdout
      (-> run-row
          (update :stdout-tail append-bounded (str (:line obs)) default-stdout-tail-limit)
          (assoc :updated-at t))

      :stderr
      (-> run-row
          (update :stderr-tail append-bounded (str (:line obs)) default-stderr-tail-limit)
          (assoc :updated-at t))

      :exit
      (assoc run-row
             :status (if (zero? (long (:exit-code obs))) :succeeded :failed)
             :exit-code (:exit-code obs)
             :finished-at t
             :updated-at t)

      (add-observation-error run-row :observation/type-invalid obs))))

(declare drain-observation-buffer)

(defn apply-observation-in-order
  [run-row obs]
  (-> run-row
      (apply-observation-effect obs)
      (assoc :last-seq (:sequence obs))))

(defn drain-observation-buffer
  [run-row]
  (loop [row run-row]
    (let [next-seq (inc (long (:last-seq row)))
          obs (get-in row [:obs-buffer next-seq])]
      (if obs
        (recur (-> row
                   (update :obs-buffer dissoc next-seq)
                   (apply-observation-in-order obs)))
        row))))

(defn fold-observation
  [run-row obs]
  (let [seq-id (:sequence obs)
        expected (inc (long (:last-seq run-row)))]
    (cond
      (not (authorized-observation? run-row obs))
      (add-observation-error run-row :observation/not-authorized obs)

      (not (contains? observation-types (:observation/type obs)))
      (add-observation-error run-row :observation/type-invalid obs)

      (not (valid-observation-sequence? obs))
      (add-observation-error run-row :observation/sequence-invalid obs)

      (< seq-id expected)
      run-row

      (= seq-id expected)
      (drain-observation-buffer (apply-observation-in-order run-row obs))

      :else
      (assoc-in run-row [:obs-buffer seq-id] obs))))

(declare append-claim!
         read-pending
         read-run
         run-granted-command!)

(defn compute-executor-runtime
  [^TaskGlobalContext context]
  (let [cluster (.getClusterRetriever context)
        module-name (.getModuleName (.getModuleInstanceInfo context))]
    {:module-name module-name
     :compute-claim-depot (.clusterDepot cluster module-name "*compute-claim-depot")
     :compute-obs-depot (.clusterDepot cluster module-name "*compute-obs-depot")
     :compute-runs (.clusterPState cluster module-name "$$compute-runs")
     :compute-pending-by-task (.clusterPState cluster module-name "$$compute-pending-by-task")}))

(defn submit-granted-run!
  [runtime ^ConcurrentHashMap registry workers run-id claim run-row opts]
  (.put registry run-id {:state :running
                         :claim claim})
  (.submit
    workers
    ^Runnable
    (reify Runnable
      (run [_]
        (try
          (run-granted-command! runtime run-row claim opts)
          (finally
            (.remove registry run-id)))))))

(defn reconcile-awaiting-claims!
  [runtime ^ConcurrentHashMap registry workers opts]
  (doseq [entry (seq (.entrySet registry))]
    (let [run-id (.getKey entry)
          local-state (.getValue entry)]
      (when (= :awaiting-grant (:state local-state))
        (let [claim (:claim local-state)
              run-row (read-run runtime run-id)
              state (claim-state run-row claim)]
          (case state
            :granted-to-us
            (submit-granted-run! runtime registry workers run-id claim run-row opts)

            :not-yet-processed
            nil

            :conflict-or-past
            (.remove registry run-id)))))))

(defn claim-new-pending-runs!
  [runtime ^ConcurrentHashMap registry executor-id executor-task-id opts]
  (let [pending (read-pending runtime executor-task-id)]
    (doseq [run-id (sort (keys pending))]
      (when-not (.containsKey registry run-id)
        (let [claim (claim-record run-id executor-id (assoc opts :executor-task-id executor-task-id))]
          (append-claim! runtime claim)
          (.put registry run-id {:state :awaiting-grant
                                 :claim claim}))))))

(defn reconcile-executor-once!
  [runtime registry workers executor-id executor-task-id opts]
  (claim-new-pending-runs! runtime registry executor-id executor-task-id opts)
  (reconcile-awaiting-claims! runtime registry workers opts))

(defonce !compute-executor-task-global-states
  (atom {}))

(defn daemon-thread-factory
  [prefix]
  (let [counter (AtomicInteger.)]
    (reify ThreadFactory
      (newThread [_ runnable]
        (doto (Thread. runnable (str prefix "-" (.incrementAndGet counter)))
          (.setDaemon true))))))

(defn close-executor-state!
  [{:keys [scheduler workers]}]
  (when scheduler
    (.shutdownNow scheduler))
  (when workers
    (.shutdownNow workers)))

(defn close-all-compute-executor-states!
  []
  (let [states @!compute-executor-task-global-states]
    (doseq [state (vals states)]
      (close-executor-state! state))
    (reset! !compute-executor-task-global-states {})))

(defrecord ComputeExecutorTaskGlobal [config]
  TaskGlobalObject
  (prepareForTask [this task-id context]
    (let [runtime (compute-executor-runtime context)
          task-id* (normalize-executor-task-id task-id)
          executor-id* (str "compute-executor-" task-id*)
          registry* (ConcurrentHashMap.)
          scheduler* (Executors/newSingleThreadScheduledExecutor
                       (daemon-thread-factory (str "compute-executor-reconcile-" task-id*)))
          workers* (Executors/newCachedThreadPool
                     (daemon-thread-factory (str "compute-executor-worker-" task-id*)))
          opts (merge {:timeout-ms default-executor-command-timeout-ms}
                      config)
          delay-ms (long (or (:reconcile-delay-ms opts)
                             default-executor-reconcile-delay-ms))
          state-key (System/identityHashCode this)]
      (swap! !compute-executor-task-global-states
             assoc
             state-key
             {:task-id task-id*
              :executor-id executor-id*
              :registry registry*
              :scheduler scheduler*
              :workers workers*})
      (.scheduleWithFixedDelay
        scheduler*
        ^Runnable
        (reify Runnable
          (run [_]
            (try
              (reconcile-executor-once! runtime registry* workers* executor-id* task-id* opts)
              (catch Throwable _ nil))))
        0
        delay-ms
        TimeUnit/MILLISECONDS)))
  (close [this]
    (let [state-key (System/identityHashCode this)
          state (get @!compute-executor-task-global-states state-key)]
      (close-executor-state! state)
      (swap! !compute-executor-task-global-states dissoc state-key))))

(defn compute-executor-task-global
  [config]
  (->ComputeExecutorTaskGlobal config))

(defmodule compute-module [setup topologies]
  (declare-depot setup *compute-depot (hash-by :run/id))
  (declare-depot setup *compute-claim-depot (hash-by :run/id))
  (declare-depot setup *compute-obs-depot (hash-by :run/id))
  (declare-object setup *compute-executor
                  (compute-executor-task-global
                    {:reconcile-delay-ms default-executor-reconcile-delay-ms
                     :timeout-ms default-executor-command-timeout-ms}))
  (let [n (stream-topology topologies "compute-run-command-topology")]
    (declare-pstate n $$compute-runs {String (map-schema Keyword Object)})
    (declare-pstate n $$compute-decisions-by-run-id {String (map-schema Keyword Object)})
    (declare-pstate n $$compute-pending-by-task {String {String (map-schema Keyword Object)}})
    (declare-pstate n $$compute-views {String (map-schema Keyword Object)})

    (<<sources n
      (source> *compute-depot :> *request)
      (interpret-run-command-request *request :> *decision)
      (decision-run-id *decision :> *run-id)
      (|hash *run-id)
      (local-transform> [(keypath *run-id) (termval *decision)] $$compute-decisions-by-run-id)
      (<<if (decision-accepted? *decision)
        (initial-run-row *decision :> *run-row)
        (current-task-id :> *current-task-id)
        (assign-executor-task *run-row *current-task-id :> *assigned-run-row)
        (run-executor-task-id *assigned-run-row :> *executor-task-id)
        (run-view *assigned-run-row :> *view)
        (pending-entry *assigned-run-row :> *pending-entry)
        (local-transform> [(keypath *run-id) (termval *assigned-run-row)] $$compute-runs)
        (local-transform> [(keypath *run-id) (termval *view)] $$compute-views)
        (|hash *executor-task-id)
        (local-transform> [(keypath *executor-task-id) (keypath *run-id) (termval *pending-entry)] $$compute-pending-by-task))

      (source> *compute-claim-depot :> *claim)
      (claim-run-id *claim :> *run-id)
      (|hash *run-id)
      (local-select> [(keypath *run-id)] $$compute-runs :> *run-row)
      (<<if (grantable-claim? *run-row *claim)
        (grant-claim *run-row *claim :> *claimed-run-row)
        (run-executor-task-id *claimed-run-row :> *executor-task-id)
        (run-view *claimed-run-row :> *view)
        (local-transform> [(keypath *run-id) (termval *claimed-run-row)] $$compute-runs)
        (local-transform> [(keypath *run-id) (termval *view)] $$compute-views)
        (|hash *executor-task-id)
        (local-transform> [(keypath *executor-task-id) (keypath *run-id) NONE>] $$compute-pending-by-task))

      (source> *compute-obs-depot {:retry-mode :all-after} :> *obs)
      (observation-run-id *obs :> *run-id)
      (|hash *run-id)
      (local-select> [(keypath *run-id)] $$compute-runs :> *run-row)
      (fold-observation *run-row *obs :> *updated-run-row)
      (run-view *updated-run-row :> *view)
      (local-transform> [(keypath *run-id) (termval *updated-run-row)] $$compute-runs)
      (local-transform> [(keypath *run-id) (termval *view)] $$compute-views))))

(defn start-compute-runtime!
  []
  (let [ipc (create-ipc)
        module-name (get-module-name compute-module)
        launch-opts {:tasks 4 :threads 2}]
    (launch-module! ipc compute-module launch-opts)
    {:ipc ipc
     :module-name module-name
     :compute-depot (foreign-depot ipc module-name "*compute-depot")
     :compute-claim-depot (foreign-depot ipc module-name "*compute-claim-depot")
     :compute-obs-depot (foreign-depot ipc module-name "*compute-obs-depot")
     :compute-runs (foreign-pstate ipc module-name "$$compute-runs")
     :compute-decisions-by-run-id (foreign-pstate ipc module-name "$$compute-decisions-by-run-id")
     :compute-pending-by-task (foreign-pstate ipc module-name "$$compute-pending-by-task")
     :compute-views (foreign-pstate ipc module-name "$$compute-views")}))

(defn close-compute-runtime!
  [runtime]
  (close-all-compute-executor-states!)
  (when-let [ipc (:ipc runtime)]
    (try
      (.close ipc)
      (catch Exception _ nil))))

(defn append-run-command!
  ([runtime request]
   (append-run-command! runtime request :append-ack))
  ([runtime request ack-level]
   (foreign-append! (:compute-depot runtime) request ack-level)
   request))

(defn append-claim!
  ([runtime claim]
   (append-claim! runtime claim :append-ack))
  ([runtime claim ack-level]
   (foreign-append! (:compute-claim-depot runtime) claim ack-level)
   claim))

(defn append-observation!
  ([runtime obs]
   (append-observation! runtime obs :append-ack))
  ([runtime obs ack-level]
   (foreign-append! (:compute-obs-depot runtime) obs ack-level)
   obs))

(defn select-pstate-one
  [pstate path]
  (first (foreign-select path pstate)))

(defn read-run
  [runtime run-id]
  (select-pstate-one (:compute-runs runtime) [(keypath run-id)]))

(defn read-decision
  [runtime run-id]
  (select-pstate-one (:compute-decisions-by-run-id runtime) [(keypath run-id)]))

(defn read-view
  [runtime run-id]
  (select-pstate-one (:compute-views runtime) [(keypath run-id)]))

(defn read-pending
  ([runtime]
   (read-pending runtime pending-task-id))
  ([runtime task-id]
   (or (select-pstate-one (:compute-pending-by-task runtime) [(keypath task-id)])
       {})))

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
  ([runtime run-id]
   (await-decision runtime run-id 2000))
  ([runtime run-id timeout-ms]
   (await-materialized #(read-decision runtime run-id) some? timeout-ms)))

(defn await-run
  ([runtime run-id pred]
   (await-run runtime run-id pred 2000))
  ([runtime run-id pred timeout-ms]
   (await-materialized #(read-run runtime run-id) pred timeout-ms)))

(defn await-view
  ([runtime run-id pred]
   (await-view runtime run-id pred 2000))
  ([runtime run-id pred timeout-ms]
   (await-materialized #(read-view runtime run-id) pred timeout-ms)))

(defn await-claim-resolution
  ([runtime claim]
   (await-claim-resolution runtime claim 2000))
  ([runtime claim timeout-ms]
   (await-materialized
     (fn []
       (let [row (read-run runtime (:run/id claim))]
         {:claim claim
          :run row
          :claim-state (claim-state row claim)}))
     #(not= :not-yet-processed (:claim-state %))
     timeout-ms)))

(defn claim-run!
  ([runtime run-id executor-id]
   (claim-run! runtime run-id executor-id {}))
  ([runtime run-id executor-id opts]
   (let [opts (if (opts-executor-task-id opts)
                opts
                (if-let [executor-task-id (:executor/task-id (read-run runtime run-id))]
                  (assoc opts :executor-task-id executor-task-id)
                  opts))
         claim (claim-record run-id executor-id opts)]
     (append-claim! runtime claim)
     (await-claim-resolution runtime claim (or (:timeout-ms opts) 2000)))))

(defn observation
  [run-id claim-token observation-type sequence payload]
  (merge {:run/id run-id
          :claim-token claim-token
          :observation/type observation-type
          :sequence sequence
          :observed-at (now-ms)}
         payload))

(defn process-pid
  [process]
  (try
    (.pid process)
    (catch Throwable _ nil)))

(defn stream-lines
  [stream]
  (with-open [reader (BufferedReader. (InputStreamReader. stream))]
    (doall (line-seq reader))))

(defn start-daemon-thread!
  [name f]
  (doto (Thread.
          ^Runnable
          (reify Runnable
            (run [_] (f)))
          name)
    (.setDaemon true)
    (.start)))

(defn append-process-observation!
  [runtime run-id claim-token seq* observation-type payload]
  (let [sequence (swap! seq* inc)
        obs (observation run-id claim-token observation-type sequence payload)]
    (append-observation! runtime obs)
    obs))

(defn run-granted-command!
  [runtime run-row claim & [opts]]
  (let [run-id (:run/id run-row)
        token (:claim-token claim)
        argv (:argv run-row)
        cwd (:cwd run-row)
        timeout-ms (or (:timeout-ms opts) default-command-timeout-ms)
        seq* (atom -1)
        append! #(append-process-observation! runtime run-id token seq* %1 %2)]
    (try
      (let [pb (ProcessBuilder. ^java.util.List argv)]
        (when-not (str/blank? cwd)
          (.directory pb (File. cwd)))
        (let [process (.start pb)
              pid (process-pid process)
              _ (append! :started {:pid pid})
              stdout-t (start-daemon-thread!
                         (str "compute-stdout-" run-id)
                         #(doseq [line (stream-lines (.getInputStream process))]
                            (append! :stdout {:line line})))
              stderr-t (start-daemon-thread!
                         (str "compute-stderr-" run-id)
                         #(doseq [line (stream-lines (.getErrorStream process))]
                            (append! :stderr {:line line})))
              exited? (.waitFor process (long timeout-ms) TimeUnit/MILLISECONDS)
              exit-code (if exited?
                          (.exitValue process)
                          (do
                            (.destroyForcibly process)
                            124))]
          (.join stdout-t)
          (.join stderr-t)
          (append! :exit {:exit-code exit-code})
          {:spawned? true
           :spawned-after-grant? (= :granted-to-us (claim-state run-row claim))
           :run/id run-id
           :claim claim
           :pid pid
           :exit-code exit-code}))
      (catch Throwable t
        (append! :stderr {:line (.getMessage t)})
        (append! :exit {:exit-code 127})
        {:spawned? false
         :spawned-after-grant? (= :granted-to-us (claim-state run-row claim))
         :run/id run-id
         :claim claim
         :exit-code 127
         :error (.getMessage t)}))))

(defn run-one-pending-local!
  ([runtime]
   (run-one-pending-local! runtime {}))
  ([runtime opts]
   (let [executor-id (or (:executor-id opts) "local-executor")
         executor-task-id (or (opts-executor-task-id opts) pending-task-id)
         opts (assoc opts :executor-task-id executor-task-id)
         pending (read-pending runtime executor-task-id)
         run-id (first (sort (keys pending)))]
     (when run-id
       (let [claim-result (claim-run! runtime run-id executor-id opts)]
         (if (= :granted-to-us (:claim-state claim-result))
           (merge claim-result
                  (run-granted-command! runtime (:run claim-result) (:claim claim-result) opts))
           claim-result))))))

(defn run-all-pending-local!
  ([runtime]
   (run-all-pending-local! runtime {}))
  ([runtime opts]
   (loop [results []]
     (if-let [result (run-one-pending-local! runtime opts)]
       (recur (conj results result))
       results))))
