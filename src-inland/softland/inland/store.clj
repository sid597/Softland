(ns softland.inland.store
  "Foreign Rama access and the source-flow boundary used by Electric.
   Takes operations or narrow PState paths; gives accepted decisions or tagged flows.
   Owns process-wide connection handles, diagnostic counters and a workspace set.
   Each watch invocation owns its ProxyState; no shared accepted world is mirrored.
   Connection setup is process-scoped. Diagnostic counts and workspace names survive
   view disposal and are not a cache of application results."
  (:require [com.rpl.rama :as r]
            [com.rpl.rama.path :as p]
            [missionary.core :as m]
            [softland.inland.module :as module]
            [softland.inland.reactive :as reactive]))

(defonce connection (atom nil))
(defonce metrics (atom {}))
(defonce test-hold (atom nil))
(defonce workspaces (atom #{"workbench"}))

(defn count!
  "Owner and metric key → updated diagnostic map, incrementing that counter.
   Counts persist for the server process; callers own subscription lifetime."
  [owner key]
  (swap! metrics update-in [owner key] (fnil inc 0)))

(defn connect!
  "Local isolated cluster → process connection and five foreign PState handles.
   Called during process startup; replacing existing handles does not close them."
  []
  (let [manager (r/open-cluster-manager
                 {"conductor.host" "localhost" "conductor.port" 1997
                  "zookeeper.port" 2217 "zookeeper.root" "inland-electric"})
        module-name (r/get-module-name module/material)
        handles (into {:manager manager :depot (r/foreign-depot manager module-name "*operations")}
                      (for [[k n] [[:rows "$$rows"] [:versions "$$versions"] [:index "$$index"] [:decisions "$$decisions"] [:workspaces "$$workspaces"]]]
                        [k (r/foreign-pstate manager module-name n)]))]
    (reset! connection handles)))

(defn read-one
  "PState kind and key path → synchronous accepted value or nil.
   Requires connect!; foreign read failures propagate to the caller."
  [kind path]
  (r/foreign-select-one [(apply p/keypath path)] (get @connection kind)))
(defn registered-workspaces
  "Connected global workspace PState → all registered workspace names.
   Used by resident recovery; not a traversal of accepted world contents."
  []
  (r/foreign-select [p/MAP-KEYS] (:workspaces @connection)))

(defn submit!
  "Proposal → acknowledged admission decision, or an exception if unreadable.
   Stamps arrival time, appends to the depot, then uses the stream ack or retained
   decision. Test hold may block before append. A thrown error does not prove that
   the operation was rejected or never reached durable storage."
  [op]
  (when-let [hold @test-hold] @hold)
  (let [op (assoc op :arrival-time (System/currentTimeMillis))
        result (r/foreign-append! (:depot @connection) op :ack)]
    (or (get result "accept")
        (:decision (read-one :decisions [(:workspace op) (:request-id op)]))
        (throw (ex-info "No readable admission decision after acknowledgement." {})))))

(defn submit-result!
  "Browser proposal → decision under local actor sid, or unconfirmed on exception.
   The latter requires reading the retained request decision before any retry."
  [op]
  (try (submit! (assoc op :actor "sid"))
       (catch Throwable _
         {:request-id (:request-id op) :name (:name op) :status :unconfirmed
          :reason "Acceptance is unconfirmed. Retrieve this request's decision before retrying."})))

(defn metrics-snapshot
  "Retained counters → per-owner totals and printable path details.
   Read-only diagnostics; opened minus closed describes owned subscriptions."
  []
  (into {} (for [[owner rows] @metrics]
             [owner (reduce (fn [out [[kind path] count]]
                              (-> out (update kind (fnil + 0) count)
                                  (update :paths conj {:kind kind :path (pr-str path) :count count})))
                            {:opened 0 :closed 0 :changes 0 :paths []} rows)])))

(defn ensure-workspace!
  "Workspace name → idempotent seed-v2 admission and process discovery entry.
   Durable seeding preserves an existing world; the local set is not authority."
  [workspace]
  (swap! workspaces conj workspace)
  (submit! {:workspace workspace :request-id "seed-v2" :name "world" :layer "base" :actor "sid" :kind :seed}))

(defn request-proxy
  "PState kind/path and callback → asynchronous Rama ProxyState acquisition.
   Synchronous acquisition errors become failed futures for one cancellation path."
  [kind path callback]
  (try (r/foreign-proxy-async [(apply p/keypath path)] (get @connection kind)
                            {:callback-fn callback})
       (catch Throwable error (java.util.concurrent.CompletableFuture/failedFuture error))))

(defn watch-path
  "Owner and PState path → continuous tagged value, complete absence or failure.
   Each Electric demand owns one ProxyState. A lock serializes setup, callbacks and
   cancellation; a proxy arriving after cancellation is immediately closed. Errors
   are emitted as failed status values, not accepted data. This flow retains only
   the latest delivery; counters remain process-owned after subscription disposal."
  [owner kind path]
  (m/eduction
    (map (fn [{:keys [value error]}]
           (cond error {:status :failed :reason "The accepted source could not be read."}
                 (nil? value) {:status :absent :complete? true}
                 :else {:status :value :value value :complete? true})))
    (m/relieve
      (fn [_ next] next)
      (m/observe
        (fn [emit]
          (let [lock (Object.) !proxy (atom nil) !closed (atom false)
                key [kind path]
                pending (request-proxy kind path
                           (fn [_ _ _]
                             (locking lock
                               (when (and (not @!closed) @!proxy)
                                 (count! owner [:changes key])
                                 (emit {:value @ @!proxy})))
                             nil))]
            (count! owner [:opened key])
            (.whenComplete pending
              (reify java.util.function.BiConsumer
                (accept [_ proxy error]
                  (locking lock
                    (if @!closed
                      (when proxy (r/close! proxy))
                      (if error
                        (emit {:error error})
                        (do (reset! !proxy proxy) (emit {:value @proxy}))))))))
            (fn []
              (locking lock
                (reset! !closed true)
                (when-let [proxy @!proxy] (r/close! proxy))
                (count! owner [:closed key])))))))))
