(ns app.server.rama.objects
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clj-http.client :as http]
            [clojure.java.io :as io]
            [app.server.env :refer [oai-key roam-api-key roam-graph-name]]
            [com.roamresearch.sdk.backend :as b]
            [cheshire.core :as json])
  (:import (clojure.lang Keyword)
           [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent CompletableFuture]
           [java.util.function Supplier]
           [com.rpl.rama.helpers ModuleUniqueIdPState]))



(defprotocol FetchTaskGlobalClient
  (task-global-client [this]))

(defprotocol FetchCliTaskGlobalClient
  (cli-client [this]))

(deftype CljHttpTaskGlobal []
  TaskGlobalObject
  (prepareForTask [this task-id task-global-context])
  (close [this])

  FetchTaskGlobalClient
  (task-global-client [this]
    {:http-get http/get
     :http-post http/post}))

(deftype CliProcessTaskGlobal []
  TaskGlobalObject
  (prepareForTask [_this _task-id _task-global-context])
  (close [_this])

  FetchCliTaskGlobalClient
  (cli-client [_this]
    {:run-cli-process true}))

(defn http-get-future [client url]
  (future
    (try
      (:body ((:http-get client) url))
      (catch Exception e
        (str "GET Error: " (.getMessage e))))))

(declare update-node)

(defn provider-default-argv
  "Build argv when client does not send raw argv.
   Session support is best-effort per provider."
  [provider prompt session-id]
  (case provider
    :claude (vec (concat ["claude"]
                         (when (seq session-id) ["--resume" session-id])
                         ["-p" (or prompt "") "--output-format" "json"]))
    :codex (vec ["codex" "exec" (or prompt "")])
    :gemini (vec (concat ["gemini"]
                         (when (seq session-id) ["--resume" session-id])
                         ["-p" (or prompt "") "--output-format" "text"]))
    (vec ["echo" (str "Unknown provider: " provider)])))

(defn run-cli-process
  [argv cwd]
  (let [cmd (vec (map str argv))
        pb  (ProcessBuilder. (into-array String cmd))]
    (when (seq cwd)
      (.directory pb (io/file cwd)))
    (.redirectErrorStream pb true)
    (let [proc (.start pb)
          output (slurp (.getInputStream proc))
          exit-code (.waitFor proc)]
      {:argv cmd
       :cwd cwd
       :output output
       :exit-code exit-code})))

(defn parse-claude-json-output
  "Parse Claude's JSON output to extract session-id and text content.
   Claude with --output-format json returns {\"type\":\"result\", \"session_id\":\"...\", \"result\":\"...\"}."
  [raw-output]
  (try
    (let [parsed (json/parse-string raw-output true)]
      {:session-id (:session_id parsed)
       :content (or (:result parsed) raw-output)})
    (catch Exception _
      {:session-id nil :content raw-output})))

(defn cli-exec-future
  "Executes provider CLI asynchronously. Supports either raw argv or provider+prompt."
  [_client request-data]
  (CompletableFuture/supplyAsync
    (reify Supplier
      (get [_this]
        (try
          (let [{:keys [provider prompt session-id argv cwd]} request-data
                cmd (if (seq argv)
                      (vec argv)
                      (provider-default-argv provider prompt session-id))
                start-ms (System/currentTimeMillis)
                result (run-cli-process cmd cwd)
                end-ms (System/currentTimeMillis)
                parsed (when (= provider :claude)
                         (parse-claude-json-output (:output result)))]
            (assoc result
                   :output (if parsed (:content parsed) (:output result))
                   :session-id (when parsed (:session-id parsed))
                   :provider provider
                   :prompt prompt
                   :duration-ms (- end-ms start-ms)))
          (catch Exception e
            {:argv (vec (or (:argv request-data) []))
             :cwd (:cwd request-data)
             :output (str "CLI Error: " (.getMessage e))
             :exit-code 1
             :provider (:provider request-data)
             :prompt (:prompt request-data)
             :duration-ms 0}))))))

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
