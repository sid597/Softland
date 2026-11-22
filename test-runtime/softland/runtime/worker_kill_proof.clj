(ns softland.runtime.worker-kill-proof
  "Proves the two Rama worker-kill scenarios from `softland.runtime.cluster-proof`
  against a real persistent Rama cluster running `Store` and `Auth`, without
  editing `cluster_proof.clj`:

    1. Kills Rama's `Store` worker process (SIGKILL) while a series of 40 valid
       acts is on its way, waits for the supervisor to restart it, retries all
       40 acts under their UUIDs, and verifies all 40 are accepted once with 40
       distinct stamps and identical answers on re-send.
    2. Kills Rama's `Auth` worker process (SIGKILL) while a runtime connection
       is held, waits for the supervisor to restart it, verifies the connection
       is not left unwatched, and verifies the same credential and session still
       work once Auth is back.

  Unlike `cluster_proof.clj`, this namespace imports `java.lang.ProcessHandle`,
  reads `/proc/<pid>/cmdline` (avoiding OpenJDK 21's 4 KiB `ProcessHandle.Info`
  truncation on Rama's ~15 KiB worker classpath), and uses valid keyword entity
  ids (`:tile-0`..`:tile-39`)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :as rama]
            [softland.auth.module :as auth-module]
            [softland.command.client :as client]
            [softland.runtime.drive :as drive]
            [softland.runtime.main :as main]
            [softland.runtime.sign-in :as sign-in]
            [softland.store.module :as store]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io Closeable File]
           [java.lang ProcessBuilder$Redirect ProcessHandle]
           [java.net ServerSocket]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files Path]
           [java.util UUID]))

(defn- free-port []
  (with-open [s (ServerSocket. 0)] (.getLocalPort s)))

(defn- wait-for [f ms]
  (let [ends (+ (System/currentTimeMillis) (long ms))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/currentTimeMillis) ends)
            (Thread/sleep 200)
            (recur))))))

(defn- cmdline-of
  "Reads the full `/proc/<pid>/cmdline` for `handle`, returning arguments joined
  with spaces, or nil if the process is gone or unreadable."
  [^ProcessHandle handle]
  (try
    (let [bytes (Files/readAllBytes (Path/of (str "/proc/" (.pid handle) "/cmdline") (make-array String 0)))]
      (str/replace (String. bytes StandardCharsets/UTF_8) "\u0000" " "))
    (catch Throwable _ nil)))

(defn- worker-of
  "Finds the live Rama worker `ProcessHandle` running `module-name` by reading
  `/proc/<pid>/cmdline`."
  [module-name]
  (let [named (str/replace module-name "/" "%2F")]
    (some (fn [^ProcessHandle handle]
            (when-let [line (cmdline-of handle)]
              (when (and (str/includes? line "rpl.rama.distributed.daemon.worker")
                         (str/includes? line named))
                handle)))
          (iterator-seq (.iterator (ProcessHandle/allProcesses))))))

(defn- line [url credential session]
  (let [conn (client/connect {:url url :credential credential :session session})]
    (if (:could-not conn)
      conn
      {:line conn :hello (client/hello conn)})))

(defn- act-of [hello e k v]
  (let [layer (:own-layer hello)]
    {:version 1 :who (:who hello) :session (:session hello) :layer layer
     :permission [(:who hello) layer layer] :facts [{:e e :k k :v v}]}))

(defn- start-copy! [conductor conductor-port logs]
  (let [port (free-port)
        url (str "http://localhost:" port)
        java (str (System/getProperty "java.home") File/separator "bin" File/separator "java")
        cmd [java "-Xss16m" "-Xmx2g" "-cp" (System/getProperty "java.class.path")
             "clojure.main" "-m" "softland.runtime.main" "serve"
             "--conductor" conductor "--conductor-port" (str conductor-port)
             "--port" (str port) "--stand-in-google" "--thrown-away"]
        log (io/file logs (str "worker-kill-copy-" port ".log"))
        proc (-> (ProcessBuilder. ^java.util.List cmd)
                 (.redirectErrorStream true)
                 (.redirectOutput (ProcessBuilder$Redirect/appendTo log))
                 (.start))
        ready? (boolean (wait-for #(= 200 (:status (drive/request :get (str url "/health") {}))) 120000))]
    {:process proc :url url :port port :ready? ready?}))

(defn- store-worker-killed! [url credential]
  (let [me (line url credential nil)
        hello (:hello me)
        series (mapv (fn [n] {:act (act-of hello (keyword (str "tile-" n)) :laid n)
                              :act-id (UUID/randomUUID)})
                     (range 40))
        results (atom {})
        send-one (fn [n {:keys [act act-id]}]
                   (let [got (client/say (:line me) {:word :send :act act :act-id act-id :wait-ms 5000} 30000)]
                     (when (= :accepted (get-in got [:answer :answer]))
                       (swap! results assoc n got))
                     got))
        store-mod (rama/get-module-name store/Store)
        worker (worker-of store-mod)
        first-pass (future (doall (map-indexed send-one series)))]
    (wait-for (fn [] (<= 8 (count @results))) 30000)
    (when worker (.destroyForcibly ^ProcessHandle worker))
    (let [first-results @first-pass
          back (wait-for (fn []
                           (let [now (worker-of store-mod)]
                             (when (and now (not= (.pid ^ProcessHandle now) (.pid ^ProcessHandle worker)))
                               now)))
                         120000)
          _ (loop [tries 60]
              (doseq [[n entry] (map-indexed vector series)
                      :when (not (contains? @results n))]
                (send-one n entry))
              (when (and (< (count @results) (count series)) (pos? tries))
                (Thread/sleep 2000)
                (recur (dec tries))))
          answers (mapv (fn [n] (:answer (get @results n))) (range (count series)))
          second-pass (mapv (fn [entry] (:answer (client/say (:line me) (assoc entry :word :send :wait-ms 5000) 30000)))
                            series)]
      (check "Rama's worker of the store's module is killed while a series of acts is on its way, and the supervisor starts it again."
             (and (some? worker) (some? back))
             {:killed (some-> ^ProcessHandle worker .pid) :back (some-> ^ProcessHandle back .pid)})
      (check "After Rama's store worker is killed mid-series, retrying each unanswered act under its UUID accepts all forty acts."
             (= (count series) (count (filter #(= :accepted (:answer %)) answers)))
             {:not-answered-at-first (count (remove #(= :accepted (get-in % [:answer :answer])) first-results))
              :accepted (count (filter #(= :accepted (:answer %)) answers))})
      (check "Each of the forty acts was taken once across the store worker kill: forty distinct stamps, and a second sending reads the same forty answers."
             (and (= (count series) (count (distinct (map :stamp answers))))
                  (= answers second-pass))
             {:stamps (count (distinct (map :stamp answers)))})
      (client/close! (:line me)))))

(defn- auth-worker-killed! [url credential]
  (let [me (line url credential nil)
        hello (:hello me)
        auth-mod (rama/get-module-name auth-module/Auth)
        worker (worker-of auth-mod)]
    (when worker (.destroyForcibly ^ProcessHandle worker))
    (let [back (wait-for (fn []
                           (let [now (worker-of auth-mod)]
                             (when (and now (not= (.pid ^ProcessHandle now) (.pid ^ProcessHandle worker)))
                               now)))
                         120000)
          told (wait-for (fn [] (client/bye (:line me))) 30000)
          again (wait-for (fn []
                            (let [opened (line url credential (:session hello))]
                              (when (:hello opened) opened)))
                          120000)]
      (check "Rama's worker of Auth is killed while a connection is held, and the supervisor starts it again."
             (and (some? worker) (some? back))
             {:killed (some-> ^ProcessHandle worker .pid) :back (some-> ^ProcessHandle back .pid)})
      (check "When Auth's worker is killed, a held connection is not left unwatched: it is told :reach-auth and ends, or its watch recovered and answers words."
             (or (= :reach-auth (:could-not told))
                 (= (:who hello) (:who (client/say (:line me) {:word :who-am-i} 10000))))
             {:told told})
      (check "When Auth's worker is back, the same credential is let in, in the same session: nothing durable in Auth was lost with the killed worker."
             (= (:session hello) (:session (:hello again)))
             (:hello again))
      (client/close! (:line me))
      (when (:line again) (client/close! (:line again))))))

(defn- arguments [args]
  (into {} (map (fn [[flag value]] [(keyword (subs flag 2)) value])) (partition 2 args)))

(defn -main [& args]
  (let [{:keys [conductor conductor-port]} (arguments args)
        conductor (or conductor "127.0.0.1")
        conductor-port (or (some-> conductor-port parse-long) 1983)
        tag (Long/toString (System/currentTimeMillis) 36)
        logs (doto (io/file (System/getProperty "java.io.tmpdir") (str "softland-worker-kill-" tag)) (.mkdirs))
        first-person {:sub (str "wk-" tag) :email (str "wk-" tag "@example.com") :name "Worker Kill"}
        cluster (rama/open-cluster-manager {"conductor.host" conductor "conductor.port" conductor-port})
        mine (main/start {:cluster cluster :origin "http://localhost:0"})
        copies (atom [])]
    (try
      (sign-in/setup! (:the-seam mine) {:email (:email first-person)})
      (let [b (start-copy! conductor conductor-port logs)]
        (swap! copies conj b)
        (check "Runtime copy process starts on the proof cluster for worker-kill checks."
               (:ready? b) (dissoc b :process))
        (let [cookie (:cookie (drive/sign-in (:url b) first-person))
              prog (fn [label]
                     (:credential (drive/program (:url b) label
                                                 (fn [code] (drive/yes (:url b) cookie code {})))))]
          (store-worker-killed! (:url b) (prog "worker-kill-store"))
          (auth-worker-killed! (:url b) (prog "worker-kill-auth"))))
      (catch Throwable t
        (check "The worker-kill proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (doseq [{:keys [^Process process]} @copies]
          (try (.destroyForcibly process) (catch Throwable _ nil)))
        (try ((:stop mine)) (catch Throwable _ nil))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
