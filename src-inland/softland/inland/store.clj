(ns softland.inland.store
  "Rama/Electric boundary. Takes read paths and operations. Gives owned proxy
   flows and durable decisions. Holds connection handles and diagnostic counts."
  (:require [com.rpl.rama :as r]
            [com.rpl.rama.path :as p]
            [missionary.core :as m]
            [softland.inland.module :as module]
            [softland.inland.reactive :as reactive]))

(defonce connection (atom nil))
(defonce metrics (atom {}))
(defonce test-hold (atom nil))
(defonce workspaces (atom #{"workbench"}))

(defn count! [owner key]
  (swap! metrics update-in [owner key] (fnil inc 0)))

(defn connect! []
  (let [manager (r/open-cluster-manager
                 {"conductor.host" "localhost" "conductor.port" 1997
                  "zookeeper.port" 2217 "zookeeper.root" "inland-electric"})
        module-name (r/get-module-name module/material)
        handles (into {:manager manager :depot (r/foreign-depot manager module-name "*operations")}
                      (for [[k n] [[:rows "$$rows"] [:versions "$$versions"] [:index "$$index"] [:decisions "$$decisions"] [:workspaces "$$workspaces"]]]
                        [k (r/foreign-pstate manager module-name n)]))]
    (reset! connection handles)))

(defn read-one [kind path]
  (r/foreign-select-one [(apply p/keypath path)] (get @connection kind)))
(defn registered-workspaces []
  (r/foreign-select [p/MAP-KEYS] (:workspaces @connection)))

(defn submit! [op]
  (when-let [hold @test-hold] @hold)
  (let [op (assoc op :arrival-time (System/currentTimeMillis))
        result (r/foreign-append! (:depot @connection) op :ack)]
    (or (get result "accept")
        (:decision (read-one :decisions [(:workspace op) (:request-id op)]))
        (throw (ex-info "No readable admission decision after acknowledgement." {})))))

(defn submit-result! [op]
  (try (submit! (assoc op :actor "sid"))
       (catch Throwable _
         {:request-id (:request-id op) :name (:name op) :status :unconfirmed
          :reason "Acceptance is unconfirmed. Retrieve this request's decision before retrying."})))

(defn metrics-snapshot []
  (into {} (for [[owner rows] @metrics]
             [owner (reduce (fn [out [[kind path] count]]
                              (-> out (update kind (fnil + 0) count)
                                  (update :paths conj {:kind kind :path (pr-str path) :count count})))
                            {:opened 0 :closed 0 :changes 0 :paths []} rows)])))

(defn ensure-workspace! [workspace]
  (swap! workspaces conj workspace)
  (submit! {:workspace workspace :request-id "seed-v2" :name "world" :layer "base" :actor "sid" :kind :seed}))

(defn request-proxy
  "Ordinary foreign API boundary; returns Rama's pending ProxyState acquisition."
  [kind path callback]
  (try (r/foreign-proxy-async [(apply p/keypath path)] (get @connection kind)
                            {:callback-fn callback})
       (catch Throwable error (java.util.concurrent.CompletableFuture/failedFuture error))))

(defn watch-path
  "Each Electric demand owns one ProxyState. No process-wide accepted mirror.
   Initial delivery and callbacks are serialized; cancellation wins setup races.
   Acquisition errors fail the flow; they never appear as accepted values."
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
