(ns softland.runtime.attacks.harness
  "Shared helpers for the runtime's attack checks: free loopback ports,
  bounded waits, opening and closing a copy on Rama's in-process cluster,
  and opening lines. It holds no check of its own."
  (:require [softland.command.client :as client]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.main :as main]
            [softland.runtime.sign-in :as sign-in])
  (:import [java.io Closeable]
           [java.net ServerSocket]))

(def first-person
  "The account of the first person, whom `setup!` makes."
  {:sub "1000001" :email "first@example.com" :name "First Person"})

(defn free-port
  "Gives a loopback port that nothing listens on now."
  []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))

(defn wait-for
  "Calls `f` until it gives a truthy value or `ms` passes. Gives that value,
  or nil."
  [f ms]
  (let [ends (+ (System/currentTimeMillis) (long ms))]
    (loop []
      (or (f)
          (when (< (System/currentTimeMillis) ends)
            (Thread/sleep 40)
            (recur))))))

(defn start-copy!
  "Starts and serves one copy of the runtime on `cluster`. Takes a map:
  `:cluster`, `:stand-in` (optional), `:google` (optional), `:numbers`
  (optional). Gives the started map with `:port`, `:url` and `:served`."
  [{:keys [cluster stand-in google numbers]}]
  (let [port (free-port)
        url (str "http://localhost:" port)
        started (main/start {:cluster cluster
                             :origin url
                             :stand-in stand-in
                             :google google
                             :numbers numbers})]
    (if (:could-not started)
      started
      (assoc started
             :port port
             :url url
             :served (main/serve! started "127.0.0.1" port)))))

(defn stop-copy!
  "Stops a copy that `start-copy!` started. Gives nil."
  [copy]
  (when copy
    (try (when (:served copy) (jetty/stop! (:served copy))) (catch Throwable _ nil))
    (try (when (:stop copy) ((:stop copy))) (catch Throwable _ nil)))
  nil)

(defn with-cluster
  "Starts an in-process cluster and a stand-in for Google, runs `setup!`
  through a temporary or caller-started copy when asked, calls `(f {:cluster
  :stand-in})` and closes the cluster at the end."
  [f]
  (let [cluster (main/in-process-cluster)
        the-stand-in (stand-in/make)]
    (try
      (f {:cluster cluster :stand-in the-stand-in})
      (finally
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))))

(defn setup-first-person!
  "Runs `sign-in/setup!` on `copy` for `first-person`."
  [copy]
  (sign-in/setup! (:the-seam copy) {:email (:email first-person)}))

(defn line
  "Opens the line at `url` with `credential` and `session` (each optional).
  Gives `{:line connection :hello hello}` or the failure word."
  [url credential session]
  (let [connection (client/connect {:url url :credential credential :session session})]
    (if (:could-not connection)
      connection
      {:line connection :hello (client/hello connection)})))

(defn close-line!
  "Closes a line that `line` opened."
  [opened]
  (when-let [connection (:line opened)]
    (client/close! connection)))

(defn say
  "Says `word` on `opened` with a bounded wait (15 s by default)."
  ([opened word] (say opened word 15000))
  ([{:keys [line]} word wait-ms]
   (client/say line word wait-ms)))
