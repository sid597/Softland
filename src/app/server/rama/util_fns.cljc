(ns app.server.rama.util-fns
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest :refer [create-ipc launch-module! gen-hashing-index-keys]]
            [app.server.file :refer [save-event dg-nodes-edn dg-edges-edn load-events dg-page-data-edn]]
            [missionary.core :as m]
            [app.server.rama.roam-ns :refer [roam-readers]]
            [app.server.rama.core :refer [node-events-module]])
  (:import (clojure.lang Keyword)
           [hyperfiddle.electric Failure Pending]
           [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent CompletableFuture]
           [java.util.function Supplier]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))



(defrecord node-events [action-type node-data event-data])
;; uuid is unique generate using (java.util.UUID/randomUUID)
(defrecord registration [uuid username])
(defrecord update-user-graph-settings [user-id graph-name settings-data event-data])



(defonce !rama-ipc (atom nil))


(def ipc
  (let [c (create-ipc)]
    (println "--R--: Start ipc, launch module")
    (reset! !rama-ipc c)
    (launch-module! c node-events-module {:tasks 4 :threads 2})))


;; Define clj defs

(def event-depot                  (foreign-depot  @!rama-ipc (get-module-name node-events-module) "*node-events-depot"))
(def nodes-pstate                 (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$nodes-pstate"))
(def node-ids-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$node-ids-pstate"))
(def event-id-pstate              (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$event-id-pstate"))
(def user-registration-pstate     (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$user-registration-pstate"))
(def user-registration-depot      (foreign-depot @!rama-ipc (get-module-name node-events-module) "*user-registration-depot"))
(def  user-graph-settings-pstate  (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$user-graph-settings-pstate"))
(def  user-graph-settings-depot   (foreign-depot @!rama-ipc (get-module-name node-events-module) "*user-graph-settings-depot"))


(defn update-event-id []
  (foreign-append! event-depot (->node-events
                                 :update-event-id
                                 {}
                                 {})
    :append-ack))

(defn get-event-id []
  (first (foreign-select [] event-id-pstate)))

(defn get-user-id [username]
  (first (foreign-select [username] user-registration-pstate)))


(defn get-user-graph-settings [user-id graph-name]
  (foreign-select [(keypath user-id) graph-name :ui-mode] user-graph-settings-pstate))


(defn update-user-setting [settings-data event-data save? update?]
  (let [user-id (get-user-id (:username event-data))
        graph-name (:graph-name event-data)]
    (do (foreign-append! user-graph-settings-depot (->update-user-graph-settings
                                                     user-id
                                                     graph-name
                                                     settings-data
                                                     event-data))
        (when save? (save-event "update-user-setting" [settings-data event-data]))
        (when (or update?
                (some? (:event-id event-data)))
          (update-event-id)))))

(defn register-user
  ([username event-data]
   (register-user username event-data false false))
  ([username event-data save? update?]
   (let [uuid (str (java.util.UUID/randomUUID))]
     (do
       (foreign-append! user-registration-depot (->registration
                                                   uuid
                                                   username)
         :append-ack)
       (when save? (save-event "register-user" [username]))
       (when (or update?
                (some? (:event-id event-data)))
          (update-event-id))))))


(defn proxy-callback [f]
  (fn [new-val diff old-val]
    (println "R: nodes-pstate callback" new-val diff old-val)
    (f new-val)))


(defn !subscribe [path pstate]
  (println "---R---: SUBSCRIBE")
  (->> (m/observe
         (fn [!]
           (println "SUBSCRIBE")
           ;; check https://clojurians.slack.com/archives/CL85MBPEF/p1698064128506939?thread_ts=1698062851.851949&cid=CL85MBPEF
           (! (Failure. (Pending.)))
           ;; using subselect because foreign-procxy takes exactly one path
           (let [proxy (foreign-proxy-async path pstate
                         {:callback-fn (proxy-callback !)})]
             #(.close @proxy))))
    ; discard stale values, DOM doesn't support backpressure
    (m/relieve {})))


(defn add-new-node
  ([node-map event-data]
   (add-new-node node-map event-data false false))
  ([node-map event-data save?]
   (add-new-node node-map event-data save? false))
  ([node-map event-data save? update?]
   (do
     (foreign-append! event-depot (->node-events
                                    :new-node
                                    node-map
                                    event-data)
       :append-ack)
     (when save?
       (save-event "add-new-node" [node-map event-data]))
     (when (or update?
             (some? (:event-id event-data)))
       (update-event-id)))))

(defn add-dg-page-data [data]
  (->node-events
    :add-dg-page-data
    data
    {:graph-name :main}))

(defn add-dg-nodes [data]
  (->node-events
    :add-dg-nodes
    data
    {:graph-name :main}))

(defn add-dg-edges [data]
  (->node-events
    :add-dg-edges
    data
    {:graph-name :main}))

(defn update-node
  ([node-map event-data]
   (update-node node-map event-data false false))
  ([node-map event-data save? update?]
   (do
     (foreign-append! event-depot (->node-events
                                    :update-node
                                    node-map
                                    event-data)
       :append-ack)
     (when save?
       (save-event "update-node" [node-map event-data]))
     (when (or update?
             (some? (:event-id event-data)))
       (update-event-id)))))

(defn send-llm-request
  [node-map event-data]
  (println "SEND LLM REQUEST: " event-data)
  (foreign-append! event-depot (->node-events
                                 :llm-request
                                 node-map
                                 event-data)
    :append-ack))


(defn roam-query-request
  [node-map event-data]
  (println "SEND roam query REQUEST: " event-data)
  (foreign-append! event-depot (->node-events
                                 :roam-query
                                 node-map
                                 event-data)
    :append-ack))

(defn get-path-data [path pstate]
  (println "FOREIGN SELECT")
  (foreign-select path pstate))

(clojure.pprint/pprint roam-readers)
;(load-events softland-edn deserialize-and-execute false) ;; THIS IS A HACK: Will not work when we move away from ipc.
(println "------ ADDING DG PAGES------")
(load-events dg-page-data-edn add-dg-page-data) ;; THIS IS A HACK: Will not work when we move away from ipc.
(println "------ ADDING DG NODES------")
(load-events dg-nodes-edn add-dg-nodes) ;; THIS IS A HACK: Will not work when we move away from ipc.
(println "------ ADDING DG EDGES------")
(load-events dg-edges-edn add-dg-edges) ;; THIS IS A HACK: Will not work when we move away from ipc.



