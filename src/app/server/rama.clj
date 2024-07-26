(ns app.server.rama
  (:use [com.rpl.rama]
       [com.rpl.rama.path])
  (:require [com.rpl.specter :as s]
            [app.server.file :refer [save-event load-events]]
            [com.rpl.rama.test :as rtest :refer [create-ipc launch-module! gen-hashing-index-keys]]
            [com.rpl.rama.aggs :as aggs :refer [+merge +map-agg]]
            [com.rpl.rama.ops :as ops]
            [clj-http.client :as http]
            [cheshire.core :as json]
            [app.server.env :refer [oai-key]]
            [missionary.core :as m])
  (:import (clojure.lang Keyword)
           [hyperfiddle.electric Failure Pending]
           [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent CompletableFuture]
           [java.util.function Supplier]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))

;; Each node-pstate is a map of graph-id and its nodes



(defrecord node-events [action-type node-data event-data])
;; uuid is unique generate using (java.util.UUID/randomUUID)
(defrecord registration [uuid username])
(defrecord update-user-graph-settings [user-id graph-name settings-data event-data])

(defprotocol FetchTaskGlobalClient
  (task-global-client [this]))

(deftype CljHttpTaskGlobal []
  TaskGlobalObject
  (prepareForTask [this task-id task-global-context])
  (close [this])

  FetchTaskGlobalClient
  (task-global-client [this]
    {:http-get http/get
     :http-post http/post}))

(defn http-get-future [client url]
  (future
    (try
      (:body ((:http-get client) url))
      (catch Exception e
        (str "GET Error: " (.getMessage e))))))

(declare update-node)

(defn http-post-future [client path event-data]
  (CompletableFuture/supplyAsync
    (reify Supplier
      (get [this]
        (try
          (let [{:keys [request-data graph-name event-id create-time]} event-data
                {:keys
                 [url
                  model
                  messages
                  temperature
                  max-tokens]} request-data
                body           (json/generate-string
                                 {:model      model
                                  :messages   messages
                                  :temperature temperature
                                  :max_tokens max-tokens})
                headers        {"Content-Type" "application/json"
                                "Authorization" (str "Bearer " oai-key)}
                _             (println "R: POST REQUEST DATA ")
                response      ((:http-post client) url {:headers headers
                                                                    :body body
                                                                    :content-type :json
                                                                    :as :json
                                                                    :throw-exceptions false})
                llm-reply     (-> response :body :choices first :message :content str)]
            (println "GOT RESPONSE" response)

            (update-node [path llm-reply] {:graph-name  graph-name
                                           :event-id    event-id
                                           :create-time create-time} true false))

          (catch Exception e
            (str "POST Error: " (.getMessage e))))))))

(defmodule node-events-module [setup topologies]
  (declare-depot setup *node-events-depot :random)
  (declare-depot setup *user-registration-depot (hash-by :username))
  (declare-depot setup *user-graph-settings-depot (hash-by :user-id))
  (declare-object setup *http-client (CljHttpTaskGlobal.))
  (let [n      (stream-topology topologies "events-topology")
        id-gen (ModuleUniqueIdPState. "$$id")]
    (declare-pstate n $$nodes-pstate {Keyword (map-schema
                                                Keyword (fixed-keys-schema
                                                           {:id                 Keyword
                                                            :x                  (fixed-keys-schema
                                                                                   {:pos Double
                                                                                    :time Long})
                                                            :y                  (fixed-keys-schema
                                                                                   {:pos Double
                                                                                    :time Long})
                                                            :type-specific-data (map-schema Keyword Object)
                                                            :type               String
                                                            :fill               String}))}
      #_{:global? true})
    (declare-pstate n $$components-pstate {Keyword (map-schema Keyword Object)})
    (declare-pstate n $$node-ids-pstate {Keyword (vector-schema Keyword)})
    (declare-pstate n $$event-id-pstate Long {:global? true
                                              :initial-value 0})
    (declare-pstate n $$user-registration-pstate {String ; username
                                                  (fixed-keys-schema {:user-id Long
                                                                      :uuid String})})
    (declare-pstate n $$user-graph-settings-pstate {Long ;user-id
                                                    (map-schema
                                                      Keyword
                                                       (fixed-keys-schema {:ui-mode Keyword
                                                                           :viewbox (vector-schema Long)}))})
    (.declarePState id-gen n)

    (<<sources n
      ;; Source from user-graph-settings-depot
      (source> *user-graph-settings-depot :> {:keys [*user-id *graph-name *settings-data *event-data]})
      (|hash *user-id)
      (local-transform> [(keypath *user-id)
                         *graph-name
                         (first *settings-data) (termval (second *settings-data))]
        $$user-graph-settings-pstate)


      ;; Source from user-registration-depot
      (source> *user-registration-depot :> {:keys [*username *uuid]})
      (local-select> (keypath *username) $$user-registration-pstate :> {*curr-uuid :uuid :as *curr-info})
      (<<if (or> (nil? *curr-info)
              (= *curr-uuid *uuid))
        (java-macro! (.genId id-gen "*user-id"))
        (println "R: -- user id--" *user-id)
        (local-transform> [(keypath *username)
                           (multi-path [:user-id (termval *user-id)]
                             [:uuid (termval *uuid)])]
          $$user-registration-pstate)
        (|hash *user-id)
        ;; by default new user gets access to :main graph
        (local-transform> [(keypath *user-id)
                           :main
                           (multi-path [:ui-mode (termval :dark)]
                                       [:viewbox (termval [0 0 2000 2000])])]
          $$user-graph-settings-pstate))


      ;; Source from node-events-depot
      (source> *node-events-depot :> {:keys [*action-type *node-data *event-data]})
      (local-select> (keypath :graph-name) *event-data :> *graph-name)
      (|hash *graph-name)
      (println "R: PROCESSING EVENT" *action-type)

      (<<cond
        ;; llm request
        (case> (= :llm-request *action-type))
        ;; request data attached to event data
        (local-select> (keypath :request-data) *event-data :> *request-data)
        (println "R: GOT LLM REQUEST: " *request-data)
        ;; Send the data to post function
        ;; which takes the data and posts it open ai
        ;; givens response all at once
        ;; have to figure out how to do streaming
        (completable-future>
          (http-post-future (task-global-client *http-client) (first *node-data) *event-data)
          :> *response-body)
        ;; find whats the current value at the given data path
        #_(println "R: RESPONSE-->" *response-body)
        #_(first *node-data :> *data-path)
        #_(local-select> [*graph-name *data-path] $$nodes-pstate :> *cur-val)
        #_(println "R: CURRENT VALUE LLM WILL REPLACE:  " *cur-val)
        ;; Update the response at the given path
        #_(local-transform>
            [*graph-name
             *data-path (termval *response-body)]
            $$nodes-pstate)
        #_(println "R: UPDATED VALUE" (local-select> *data-path $$nodes-pstate))


        ;; Add nodes
        (case> (= :new-node *action-type))
        (println "R: ADDING NODE" *node-data)
        (local-transform>
          [*graph-name
           (keypath (ffirst *node-data))
           (termval (val (first *node-data)))]
          $$nodes-pstate)
        (local-transform>
          [(keypath *graph-name)
           AFTER-ELEM
           (termval (ffirst *node-data))]
          $$node-ids-pstate)

        ;; Delete nodes
        #_#_#_(case> (= :delete-node *action-type))
        (local-transform>
          [*graph-name (keypath (first *node-data)) NONE>]
          $$nodes-pstate)
        (local-transform>
          [*graph-name
           [ALL (= % (first *node-data))] NONE>]
          $$node-ids-pstate)

        ;; Update nodes
        (case> (= :update-node *action-type))
        (println "----------------------------------------------------")
        (println "R: UPDATING NODE" *node-data)
        (local-transform>
          [*graph-name (first *node-data) (termval (second *node-data))]
          $$nodes-pstate)
        (println "R: NODE UPDATED")
        ;(clojure.pprint/pprint (local-select> ALL $$nodes-pstate))
        (println "----------------------------------------------------")

        ;; update event id
        (case> (= :update-event-id *action-type))
        (local-select> [] $$event-id-pstate :> *event-id)
        (local-transform> [(termval (inc *event-id))] $$event-id-pstate)


        (default>) (println "FALSE" *action-type)))))

;; ====== RAMA ======


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

(defn get-path-data [path pstate]
  (println "FOREIGN SELECT")
  (foreign-select path pstate))

(load-events) ;; THIS IS A HACK: Will not work when we move away from ipc.


(comment
  (do
    (def ipc
      (let [c (create-ipc)]
        (println "--R--: Start ipc, launch module")
        (reset! !rama-ipc c)
        (launch-module! c node-events-module {:tasks 4 :threads 2})))

    ;; Define clj defs

    (def event-depot (foreign-depot @!rama-ipc (get-module-name node-events-module) "*node-events-depot"))
    (def nodes-pstate (foreign-pstate @!rama-ipc (get-module-name node-events-module) "$$nodes-pstate")))

  (close! @!rama-ipc)

  (foreign-select [] event-id-pstate)

  (foreign-append! event-depot (->node-events
                                 :update-event-id
                                 {}
                                 {}))


  (foreign-append! event-depot (->node-events
                                 :new-node
                                 {:rect11 {:id :rect11
                                           :x 50.99
                                           :y 60.0
                                           :type-specific-data {:text "GM Hello"
                                                                :width 400
                                                                :height 800}
                                           :type "rect"
                                           :fill  "lightblue"}}
                                 {:graph-name :main})
    :append-ack)

  (foreign-select [(keypath :main) ALL FIRST ] nodes-pstate)
  (foreign-select [(keypath :main) ] nodes-pstate)
  (foreign-select-one [(keypath :main) ALL FIRST ] nodes-pstate)

  (foreign-select  ALL  nodes-pstate {:pkey 4})
  (foreign-select  [(keypath :main) ALL]  nodes-pstate {:pkey :rect})
  (foreign-select [:main :6f818232-b41d-405f-9e2f-df2c66c9181f :x] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main ] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main FIRST ] nodes-pstate {:pkey :rect})
  ;(foreign-select-one [:main :cc61a1a9-663e-4d70-9347-bb9571588eb6 FIRST ] nodes-pstate {:pkey :rect})
  (foreign-select-one [:main ALL] nodes-pstate {:pkey :rect})


  (do
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect3 {:id :rect3
                                            :x 500.032452345
                                            :y 600.989070
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :main})
      :append-ack)
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect4 {:id :rect4
                                            :x 500.2
                                            :y 600.3
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack)
    (foreign-append! event-depot (->node-events
                                   :new-node
                                   {:rect5 {:id :rect5
                                            :x 500.44
                                            :y 600.223
                                            :type-specific-data {:text "GM Hello"
                                                                 :width 400
                                                                 :height 800}
                                            :type "rect"
                                            :fill  "lightblue"}}
                                   {:graph-name :level-1})
      :append-ack))
 foreign-select (keypath ["main" ALL]) nodes-pstate {:pkey :rect})

