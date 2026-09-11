(ns softland.inland.resident
  "Process-owned executor for accepted external activities.
   Takes pending Rama activity addresses; gives claim and outcome admissions around
   one bounded Claude CLI attempt. Owns a file lock, polling task and child processes;
   borrows accepted state from store. Closing a browser never cancels this owner.
   Startup marks possibly performed running calls unconfirmed, without retrying.
   The single polling task serializes calls and has no automatic failure supervisor."
  (:require [softland.inland.store :as store]
            [softland.inland.total :as total]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util.concurrent TimeUnit]
           [java.nio.channels FileChannel]
           [java.nio.file StandardOpenOption]))

(defonce running (atom nil))
(defonce fault (atom nil))
(defonce processes (atom #{}))

(defn stop-process!
  "Live process → best-effort destroy of current descendants, then parent.
   Does not wait or guarantee descendants exited; invoke! separately bounds its wait."
  [^Process process]
  (when (.isAlive process)
    (with-open [children (.descendants process)]
      (.forEach children (reify java.util.function.Consumer
                           (accept [_ child] (.destroy ^java.lang.ProcessHandle child)))))
    (.destroy process)))

(defn row
  "Workspace and activity name → current base-layer accepted row.
   Synchronous foreign read; errors propagate."
  [workspace name]
  (store/read-one :rows [workspace (total/row-key "base" name)]))

(defn observe!
  "Activity address, owner token, status and trusted outcome fields → admission.
   Uses a deterministic request id per token/status; Rama checks execution ownership."
  [workspace name token status value]
  (store/submit! (merge {:workspace workspace :name name :layer "base" :actor "executor"
                         :kind :observe :request-id (str name "/outcome/" token "/" (clojure.core/name status))
                         :execution-owner token :status status} value)))

(defn decode-result
  "CLI stdout and exit code → complete, failed or unconfirmed observation.
   Accepts object, array or newline JSON and selects the last terminal result.
   Success requires zero exit, success subtype and nonempty reply; retains at most
   8000 reply characters plus selected usage/cost metadata. Explicit provider error
   is failed; missing confirmation is unconfirmed. Raw logs are not returned."
  [text exit-code]
  (let [read-json #(try (json/read-str % :key-fn keyword) (catch Throwable _ nil))
        parsed (read-json text)
        lines (keep read-json (str/split-lines text))
        ;; read-str accepts the first object without consuming trailing objects.
        ;; Detect newline framing before treating that first object as the result.
        stream? (> (count lines) 1)
        messages (cond (vector? parsed) parsed stream? lines
                       (map? parsed) [parsed] :else lines)
        result (last (filter #(= "result" (:type %)) messages))
        details {:format (cond (vector? parsed) :array stream? :stream (map? parsed) :object :else :stream)
                 :exit-code exit-code :subtype (:subtype result)
                 :retry-events (count (filter #(= "api_retry" (:subtype %)) messages))
                 :assistant-messages (count (filter #(= "assistant" (:type %)) messages))
                 :usage (select-keys (:usage result) [:input_tokens :output_tokens :cache_read_input_tokens :cache_creation_input_tokens])
                 :cost-usd (:total_cost_usd result)}]
    (cond
      (and result (zero? exit-code) (= "success" (:subtype result)) (not (:is_error result)) (seq (:result result)))
      {:status :complete :reply (subs (:result result) 0 (min 8000 (count (:result result)))) :provider-result details}
      (and result (:is_error result))
      {:status :failed :reason "Claude reported an error outcome. No reply was accepted." :provider-result details}
      :else {:status :unconfirmed :reason "The CLI supplied no confirmable terminal result. This intent will not retry." :provider-result details})))

(defn invoke!
  "Accepted intent → one provider observation; owns process and stream readers.
   Runs Claude with no tools, one turn, bounded output configuration, cost ceiling,
   10–90 second process timeout and retries disabled. Uses the user's existing CLI
   auth without reading credentials. Controlled fault modes make no external call.
   Timeout is unconfirmed. Stream slurps have no independent byte cap; CLI flags
   and elapsed time are limits, not proof of a general memory/resource sandbox."
  [intent]
  (let [controlled @fault]
    (cond
      (= controlled :failure) {:status :failed :reason "Controlled provider refusal (fault test; no provider call)."}
      (= controlled :uncertain) {:status :unconfirmed :reason "Controlled interrupted call; whether the external effect happened is unknown. No retry."}
      :else
      (let [timeout (min 90 (max 10 (:timeout-seconds intent 60)))
            process (.start (doto (ProcessBuilder.
                                   ^java.util.List ["claude" "-p" "--safe-mode" "--model" (:model intent "haiku")
                                                    "--system-prompt" "You are Softland's resident. Answer the request directly and concisely. No tools."
                                                    "--output-format" "stream-json" "--verbose" "--max-turns" "1" "--max-budget-usd" "0.03"
                                                    "--no-session-persistence"
                                                    "--tools" "" "--strict-mcp-config"
                                                    "--setting-sources" "user"])
                             (.directory (io/file ".inland-runtime"))
                             (-> .environment (.put "CLAUDE_CODE_MAX_OUTPUT_TOKENS" (str (:max-output intent))))
                             (-> .environment (.put "MAX_THINKING_TOKENS" "0"))
                             (-> .environment (.put "CLAUDE_CODE_MAX_RETRIES" "0"))))
            output (future (slurp (.getInputStream process)))
            errors (future (slurp (.getErrorStream process)))]
        (swap! processes conj process)
        (try
        (with-open [writer (io/writer (.getOutputStream process))]
          (.write writer (str "Answer in no more than " (:max-output intent) " tokens. No tools.\n\n" (:input intent))))
        (if (.waitFor process timeout TimeUnit/SECONDS)
          (let [text @output _ @errors] (decode-result text (.exitValue process)))
          (do (stop-process! process) (.waitFor process 2 TimeUnit/SECONDS)
              (when (.isAlive process) (.destroyForcibly process))
              {:status :unconfirmed :reason "The call exceeded its time limit. The provider may have performed it; this intent will not retry."}))
          (finally (stop-process! process) (swap! processes disj process)
                   (future-cancel output) (future-cancel errors)))))))

(defn execute!
  "Accepted pending address → claim, one CLI attempt, then owned outcome admission.
   Only an accepted claim invokes the provider. Exceptions during invocation become
   unconfirmed; no automatic retry is scheduled. Admission errors still propagate."
  [workspace name]
  (let [token (str (random-uuid))
        claim (store/submit! {:workspace workspace :name name :layer "base" :actor "executor"
                              :kind :claim :request-id (str name "/claim/" token) :execution-owner token})]
    (when (= :accepted (:status claim))
      (let [current (row workspace name)
            result (try (invoke! (:activity current))
                        (catch Throwable _ {:status :unconfirmed :reason "The execution owner lost confirmation of the external call. No retry."}))]
        (observe! workspace name token (:status result) (dissoc result :status))))))

(defn recover!
  "Workspace → unconfirmed admissions for activities retained as running.
   Preserves their owner token and possible-effect uncertainty; never reissues calls."
  [workspace]
  (doseq [name (store/read-one :index [workspace "base/activities/running"])
          :let [current (row workspace name)]
          :when (= :running (:status current))]
    (observe! workspace name (:execution-owner current) :unconfirmed
      {:reason "The previous execution owner stopped during a possible external call. No automatic retry."})))

(defn start!
  "Isolated runtime directory and connected store → process execution owner.
   Acquires resident.lock, discovers durable workspaces, recovers running records,
   then polls pending indexes every 250 ms between serial calls. Registers shutdown
   cleanup for task, processes and lock. Requires one start per server process;
   an uncaught polling/read error ends the future without automatic restart."
  []
  (let [channel (FileChannel/open (.toPath (io/file ".inland-runtime/resident.lock"))
                  (into-array StandardOpenOption [StandardOpenOption/CREATE StandardOpenOption/WRITE]))
        lock (.tryLock channel)]
    (when-not lock (.close channel) (throw (ex-info "Another execution owner holds the runtime lock." {})))
    (let [!stop (atom false)
          thread (future
                   (swap! store/workspaces into (store/registered-workspaces))
                   (doseq [workspace @store/workspaces] (recover! workspace))
                   (while (not @!stop)
                     (doseq [workspace @store/workspaces
                             name (store/read-one :index [workspace "base/activities/pending"])
                             :when (= :pending (:status (row workspace name)))]
                       (execute! workspace name))
                     (Thread/sleep 250)))]
      (reset! running {:channel channel :lock lock :stop !stop :thread thread})
      (.addShutdownHook (Runtime/getRuntime)
        (Thread. (fn [] (reset! !stop true) (doseq [process @processes] (stop-process! process))
                   (future-cancel thread) (.release lock) (.close channel)))))))
