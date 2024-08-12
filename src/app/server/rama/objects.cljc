(ns app.server.rama.objects
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clj-http.client :as http]
            [app.server.env :refer [oai-key roam-api-key roam-graph-name]]
            [com.roamresearch.sdk.backend :as b]
            [cheshire.core :as json])
  (:import (clojure.lang Keyword)
           [hyperfiddle.electric Failure Pending]
           [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent CompletableFuture]
           [java.util.function Supplier]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))



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

(defn query-roam-req [client query &args]
  (CompletableFuture/supplyAsync
    (reify Supplier
      (get [this]
        (try
          (do
            (println "trying to query" client query)
            (b/q client query &args))
          (catch Exception e
            (str "ROAM QUERY POST ERROR: " (.getMessage e))))))))


(defprotocol fetch-roam-client
  (roam-client [this]))


;; Define a task global to manage the Roam client
(deftype roam-task-global [token graph]
  TaskGlobalObject
  (prepareForTask [this task-id task-global-context]
    (println "Preparing Roam client for task" task-id))
  (close [this]
    (println "Closing Roam client"))

  fetch-roam-client
  (roam-client [this] {:token token
                       :graph graph}))
