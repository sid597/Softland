(ns softland.inland.resident
  "Process-owned executor for accepted external activities.
   Takes an accepted activity's intent, handed over by the store when a gesture
   starts it (nothing polls); gives its running and outcome observations to the
   writer the store supplies. Owns a file lock and child processes. Closing a
   browser never cancels this owner.

   Asking the resident spends money, so until Sid says yes the reply is a stand-in:
   no provider is called, nothing is spent, and the reply says so. Setting
   INLAND_RESIDENT_LIVE=1 makes one bounded Claude CLI attempt instead, as before.
   An attempt interrupted mid-call is unconfirmed and never retried."
  (:require [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util.concurrent TimeUnit]
           [java.nio.channels FileChannel]
           [java.nio.file StandardOpenOption]))

(defonce running (atom nil))
(defonce fault (atom nil))
(defonce processes (atom #{}))

(defn live?
  "Whether the resident is really asked (Sid's yes, as INLAND_RESIDENT_LIVE=1)."
  []
  (= "1" (System/getenv "INLAND_RESIDENT_LIVE")))

(defn stop-process!
  "Live process → best-effort destroy of current descendants, then parent.
   Does not wait or guarantee descendants exited; invoke! separately bounds its wait."
  [^Process process]
  (when (.isAlive process)
    (with-open [children (.descendants process)]
      (.forEach children (reify java.util.function.Consumer
                           (accept [_ child] (.destroy ^java.lang.ProcessHandle child)))))
    (.destroy process)))

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
   Timeout is unconfirmed. Only reached when `live?`."
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

(defn stand-in
  "The reply while the resident is not asked: it says so, and nothing is spent."
  [intent]
  (let [input (str (:input intent))]
    {:status :complete
     :reply (str "Stand-in reply: the resident was not asked, so no provider was called and nothing was spent. "
                 "Your request was \"" (subs input 0 (min 160 (count input))) (when (< 160 (count input)) "…") "\".")
     :provider-result {:stand-in true}}))

(defn execute!
  "An accepted activity's one attempt, handed over by the store when a gesture
   starts it: `observe!` is called with `:running` and no fields, then with the
   outcome's status and fields. Runs on its own thread; an exception during the
   attempt is unconfirmed, never retried."
  [intent observe!]
  (future
    (observe! :running {})
    (let [result (try (if (live?) (invoke! intent) (stand-in intent))
                      (catch Throwable _ {:status :unconfirmed :reason "The execution owner lost confirmation of the external call. No retry."}))]
      (observe! (:status result) (dissoc result :status)))))

(defn start!
  "Isolated runtime directory → this process as the one execution owner: takes
   resident.lock and registers shutdown cleanup of child processes and the lock."
  []
  (let [channel (FileChannel/open (.toPath (io/file ".inland-runtime/resident.lock"))
                  (into-array StandardOpenOption [StandardOpenOption/CREATE StandardOpenOption/WRITE]))
        lock (.tryLock channel)]
    (when-not lock (.close channel) (throw (ex-info "Another execution owner holds the runtime lock." {})))
    (reset! running {:channel channel :lock lock})
    (.addShutdownHook (Runtime/getRuntime)
      (Thread. (fn [] (doseq [process @processes] (stop-process! process))
                 (.release lock) (.close channel))))))
