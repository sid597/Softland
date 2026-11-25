(ns softland.runtime.cluster-proof
  "The proof of the runtime on a cluster that stays up: three copies of the
  runtime as three processes of their own, met over real requests, and
  then processes killed while acts are on their way. One command, from the
  repository root, against a Rama cluster on which the modules `Store` and
  `Auth` are launched:

    clojure -M:runtime -m softland.runtime.cluster-proof --conductor localhost --conductor-port 1983

  The cluster must hold nothing that is kept: each copy runs with the
  stand-in for Google, which the program starts on a conductor's cluster
  only when it is told so (`softland.runtime.main`, `--thrown-away`).

  What it does, in order:

    1. The setup, from this process: the base when it is not made, and a
       first person of this run.
    2. Three copies of the runtime, each a JVM of its own on a port of the
       loopback address: two with the seam's default numbers, one with
       short times.
    3. The checks that both proofs of the runtime share
       (`softland.runtime.checks`), across the three processes.
    4. A copy of the runtime is killed while an act is on its way.
    5. Rama's worker of the store's module is killed while a series of acts
       is on its way.
    6. Rama's worker of Auth is killed while a connection is held.
    7. Many roots are watched at once, to hold Rama's bound on the live
       reads of one table on one task against what Auth set.

  A process is killed with the signal that it cannot catch. The supervisor
  of the cluster starts a killed worker again. A check of what follows a
  kill passes only when the kill was made: a worker that is not found
  fails each of its checks. Rama's worker is found by the command line of
  its process, read from `/proc`. The steps of the two workers' kills are
  public, and `softland.runtime.worker-kill-proof` runs them alone, on one
  copy.

  The cluster keeps what earlier runs wrote. So each run makes its own
  accounts, with a tag of the moment in each, and its own first person.

  State. It writes into the cluster's store and Auth, and leaves the logs
  of the three copies in a folder that it names at the start."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :as rama]
            [softland.auth.client :as auth]
            [softland.auth.module :as auth-module]
            [softland.command.client :as client]
            [softland.runtime.checks :as checks]
            [softland.runtime.drive :as drive]
            [softland.runtime.main :as main]
            [softland.runtime.seam :as seam]
            [softland.runtime.sign-in :as sign-in]
            [softland.store.module :as store]
            [softland.wire.credential :as credential]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io Closeable File]
           [java.lang ProcessBuilder$Redirect ProcessHandle]
           [java.net ServerSocket]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files Path]
           [java.util UUID]
           [java.util.concurrent TimeUnit]))

;; ------------------------------------------------------------------ processes

(defn free-port
  "Gives a port of the loopback address that nothing listens on now."
  []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))

(defn- wait-for
  "Calls a function of no argument until it gives a thing that is truthy
  or the span passes. Gives the thing, or nil."
  [f ms]
  (let [ends (+ (System/currentTimeMillis) ms)]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/currentTimeMillis) ends)
            (Thread/sleep 200)
            (recur))))))

(defn- ready?
  "Takes a copy's address. Gives true when the process answers."
  [url]
  (= 200 (:status (drive/request :get (str url "/health") {}))))

(defn copy!
  "Starts one copy of the runtime as a process of its own, with this
  process's own classpath, and waits until it answers. Takes the
  conductor's host and port, the port to listen on, the folder for its
  log, and the seam's numbers or nil. Gives {:process :url :port}."
  [{:keys [conductor conductor-port port logs numbers]}]
  (let [url (str "http://localhost:" port)
        java (str (System/getProperty "java.home") File/separator "bin" File/separator "java")
        command (into [java "-Xss16m" "-Xmx2g" "-cp" (System/getProperty "java.class.path")
                       "clojure.main" "-m" "softland.runtime.main" "serve"
                       "--conductor" conductor "--conductor-port" (str conductor-port)
                       "--port" (str port) "--stand-in-google" "--thrown-away"]
                      (mapcat (fn [[k v]] [(str "--" (name k)) (str v)]) numbers))
        log (io/file logs (str "copy-" port ".log"))
        process (-> (ProcessBuilder. ^java.util.List command)
                    (.redirectErrorStream true)
                    (.redirectOutput (ProcessBuilder$Redirect/appendTo log))
                    (.start))]
    {:process process :url url :port port
     :ready? (boolean (wait-for (fn [] (ready? url)) 120000))}))

(defn- kill!
  "Kills a process with the signal that it cannot catch, and waits until
  it is gone."
  [^Process process]
  (.destroyForcibly process)
  (.waitFor process 30 TimeUnit/SECONDS))

(defn- command-line-of
  "Takes a process's handle. Gives its whole command line, the arguments
  joined with spaces, read from `/proc/<pid>/cmdline`; or nil when the
  process is gone. It is read from the file and not from
  `ProcessHandle.Info`, which on Linux gives no arguments for a command
  line longer than about 4 KiB, and a worker of Rama's has one of about
  15 KiB."
  [^ProcessHandle handle]
  (try
    (let [bytes (Files/readAllBytes (Path/of (str "/proc/" (.pid handle) "/cmdline") (make-array String 0)))]
      (str/replace (String. bytes StandardCharsets/UTF_8) "\u0000" " "))
    (catch Throwable _ nil)))

(defn worker-of
  "Takes a module's name as Rama has it. Gives the handle of the process of
  Rama's worker that runs it now, or nil: the process whose command line
  names the worker's class and the module."
  [module-name]
  (let [named (str/replace module-name "/" "%2F")]
    (some (fn [^ProcessHandle handle]
            (when-let [line (command-line-of handle)]
              (when (and (str/includes? line "rpl.rama.distributed.daemon.worker")
                         (str/includes? line named))
                handle)))
          (iterator-seq (.iterator (ProcessHandle/allProcesses))))))

;; -------------------------------------------------------------------- helpers

(defn- line
  "Opens the line at an address. Gives {:line :hello}, or the word with
  which the opening failed."
  [url credential session]
  (let [connection (client/connect {:url url :credential credential :session session})]
    (if (:could-not connection)
      connection
      {:line connection :hello (client/hello connection)})))

(defn- act-of
  "Builds one act of one fact in the hello's own lane."
  [hello e k v]
  (let [lane (:own-lane hello)]
    {:version 1 :who (:who hello) :session (:session hello) :lane lane
     :permission [(:who hello) lane lane] :facts [{:e e :k k :v v}]}))

(defn- arguments
  "Takes the arguments of the command line. Gives a map of them."
  [args]
  (into {} (map (fn [[flag value]] [(keyword (subs flag 2)) value])) (partition 2 args)))

;; ---------------------------------------------------------------- the kills

(defn- runtime-killed!
  "Step 4: a copy of the runtime is killed while an act is on its way."
  [{:keys [a b restart!]} credential]
  (let [me (line (:url a) credential nil)
        hello (:hello me)
        before-uuid (UUID/randomUUID)
        before-act (act-of hello :kiln-1 :fired 1)
        before (client/say (:line me) {:word :send :act before-act :act-id before-uuid})
        on-its-way-uuid (UUID/randomUUID)
        on-its-way (act-of hello :kiln-1 :fired 2)
        unheard (future (client/say (:line me) {:word :send :act on-its-way :act-id on-its-way-uuid} 15000))
        _ (kill! (:process a))
        cut-off @unheard
        other (line (:url b) credential (:session hello))
        once (client/say (:line other) {:word :send :act on-its-way :act-id on-its-way-uuid})
        twice (client/say (:line other) {:word :send :act on-its-way :act-id on-its-way-uuid})
        earlier (client/say (:line other) {:word :send :act before-act :act-id before-uuid})
        read (client/say (:line other) {:word :read :query {:asks [:place :kiln-1 :fired] :asker (:who hello)
                                                            :lanes [(:own-lane hello)]}})]
    (check "A copy of the runtime that is killed takes its connections with it and nothing else: the word that was on its way is not answered, and the system connects to another copy with the same credential, in the same session."
           (and (:could-not cut-off) (= (:session hello) (:session (:hello other))))
           {:cut-off cut-off :other (:hello other)})
    (check "An act that was on its way when the copy was killed is sent again under the same UUID through another copy, and is taken once: both sends after the break read one answer with one stamp, and the place holds what the act wrote."
           (and (= :accepted (get-in once [:answer :answer]))
                (= (:digest once) (:digest twice)) (= (:answer once) (:answer twice))
                (= 2 (:value (:place read))))
           [once twice read])
    (check "An act from before the break, sent again after it, reads the answer that it had."
           (and (= :accepted (get-in before [:answer :answer])) (= (:answer before) (:answer earlier)))
           [before earlier])
    (let [again (restart!)]
      (check "A copy that is started in the killed one's place holds nothing of it and serves at once: the same credential is let in there, in the same session."
             (and (:ready? again)
                  (= (:session hello) (:session (:hello (line (:url again) credential (:session hello))))))
             (dissoc again :process))
      again)))

(defn store-worker-killed!
  "Step 5: Rama's worker of the store's module is killed while a series of
  acts is on its way. Takes the address of a copy of the runtime and a
  program's credential. Each act writes one fact of its own entity, a
  keyword, as an act's entity is."
  [url credential]
  (let [me (line url credential nil)
        hello (:hello me)
        series (mapv (fn [n] {:act (act-of hello (keyword (str "tile-" n)) :laid n) :act-id (UUID/randomUUID)})
                     (range 40))
        results (atom {})
        send-one (fn [n {:keys [act act-id]}]
                   (let [got (client/say (:line me) {:word :send :act act :act-id act-id :wait-ms 5000} 30000)]
                     (when (= :accepted (get-in got [:answer :answer]))
                       (swap! results assoc n got))
                     got))
        worker (worker-of (rama/get-module-name store/Store))
        first-pass (future (doall (map-indexed send-one series)))]
    (wait-for (fn [] (<= 8 (count @results))) 30000)
    (when worker (.destroyForcibly ^ProcessHandle worker))
    (let [first-results @first-pass
          back (wait-for (fn [] (let [now (worker-of (rama/get-module-name store/Store))]
                                  (when (and now (not= (.pid ^ProcessHandle now) (.pid ^ProcessHandle worker))) now)))
                         120000)
          ;; Every act is sent again under its UUID, until each has its
          ;; answer or the tries are spent.
          _ (loop [tries 60]
              (doseq [[n entry] (map-indexed vector series)
                      :when (not (contains? @results n))]
                (send-one n entry))
              (when (and (< (count @results) (count series)) (pos? tries))
                (Thread/sleep 2000)
                (recur (dec tries))))
          answers (mapv (fn [n] (:answer (get @results n))) (range (count series)))
          second-pass (mapv (fn [entry] (:answer (client/say (:line me) (assoc entry :word :send :wait-ms 5000) 30000)))
                            series)
          not-answered-at-first (count (remove (fn [got] (= :accepted (get-in got [:answer :answer]))) first-results))]
      (check "Rama's worker of the store's module is killed while a series of acts is on its way, and the supervisor starts it again."
             (and worker back) {:killed (some-> ^ProcessHandle worker .pid) :back (some-> ^ProcessHandle back .pid)})
      ;; How many acts the kill cut off is printed, not held: Rama's client
      ;; sends an append again while the worker comes back, so a run can
      ;; have none cut off.
      (check "After the worker is killed mid-series, each act that had no answer then is sent again under its UUID until it has its answer, and in the end every act of the series is accepted."
             (and (some? worker)
                  (= (count series) (count (filter (fn [answer] (= :accepted (:answer answer))) answers))))
             {:killed? (some? worker)
              :not-answered-at-first not-answered-at-first
              :accepted (count (filter (fn [answer] (= :accepted (:answer answer))) answers))})
      (check "Each act of the series was taken once across the kill: forty acts have forty stamps, and a second sending of all of them reads the same forty answers."
             (and (some? worker)
                  (= (count series) (count (distinct (map :stamp answers))))
                  (= answers second-pass))
             {:killed? (some? worker) :stamps (count (distinct (map :stamp answers)))})
      (client/close! (:line me)))))

(defn auth-worker-killed!
  "Step 6: Rama's worker of Auth is killed while a connection is held.
  Takes the address of a copy of the runtime and a program's credential,
  which it ends.

  Whether the held connection is still watched is proven by what only a
  watch carries: when it was not told that Auth cannot be reached, the
  program's sign-in is ended through a second connection, and the held
  one must be told that its sign-in ended. A word that the runtime answers
  from its own memory, such as `:who-am-i`, proves nothing of the watch."
  [url credential]
  (let [me (line url credential nil)
        hello (:hello me)
        worker (worker-of (rama/get-module-name auth-module/Auth))]
    (when worker (.destroyForcibly ^ProcessHandle worker))
    (let [back (wait-for (fn [] (let [now (worker-of (rama/get-module-name auth-module/Auth))]
                                  (when (and now (not= (.pid ^ProcessHandle now) (.pid ^ProcessHandle worker))) now)))
                         120000)
          told (wait-for (fn [] (client/bye (:line me))) 30000)
          again (wait-for (fn [] (let [opened (line url credential (:session hello))]
                                   (when (:hello opened) opened)))
                          120000)
          ended (when (and (:line again) (nil? told))
                  (client/say (:line again) {:word :sign-out} 10000))
          told-after (when ended (wait-for (fn [] (client/bye (:line me))) 30000))]
      (check "Rama's worker of Auth is killed while a connection is held, and the supervisor starts it again."
             (and worker back) {:killed (some-> ^ProcessHandle worker .pid) :back (some-> ^ProcessHandle back .pid)})
      (check "When the worker is back, the same credential is let in, in the same session: nothing that Auth held was lost with the process."
             (and (some? worker) (= (:session hello) (:session (:hello again))))
             {:killed? (some? worker) :again (:hello again)})
      (check "A connection whose watch of Auth was lost with the worker is not left unwatched: it is told that Auth cannot be reached and ends; or its watch came back, and when its sign-in is then ended through another connection, it is told that its sign-in ended."
             (and (some? worker)
                  (or (= :reach-auth (:could-not told))
                      (= :find-her-sign-in (:could-not told-after))))
             {:killed? (some? worker) :told told :ended ended :told-after told-after})
      (client/close! (:line me))
      (when (:line again) (client/close! (:line again))))))

(defn- many-roots-watched!
  "Step 7: many roots are watched at once through one client of Auth."
  [the-auth]
  (let [lost (atom 0)
        n 6000
        began (System/currentTimeMillis)
        stops (doall (repeatedly n (fn [] (auth/watch-root the-auth (UUID/randomUUID)
                                                           (fn [value] (when (= ::auth/not-reached value)
                                                                         (swap! lost inc)))))))
        opened-in (- (System/currentTimeMillis) began)]
    (Thread/sleep 5000)
    (check "Six thousand roots are watched at once through one copy, on a module of four tasks, and none is refused: Rama's own bound of a thousand live reads of one table on one task, which would have turned away a third of them, is not what binds Auth's watch."
           (and (zero? @lost) (every? fn? stops))
           {:lost @lost :opened-in-ms opened-in :no-function (count (remove fn? stops))})
    (doseq [stop stops :when (fn? stop)] (stop))))

;; ----------------------------------------------------------------------- main

(defn -main
  "Runs the proof, prints the counts, and ends the process with a status."
  [& args]
  (let [{:keys [conductor conductor-port]} (arguments args)
        conductor (or conductor "localhost")
        conductor-port (or (some-> conductor-port parse-long) 1973)
        tag (Long/toString (System/currentTimeMillis) 36)
        logs (doto (io/file (System/getProperty "java.io.tmpdir") (str "softland-cluster-proof-" tag)) (.mkdirs))
        first-person {:sub (str tag "-1") :email (str "first-" tag "@example.com") :name "First Person"}
        cluster (rama/open-cluster-manager {"conductor.host" conductor "conductor.port" conductor-port})
        mine (main/start {:cluster cluster :origin "http://localhost:0"})
        copies (atom [])]
    (println "The logs of the copies are in" (.getPath logs))
    (try
      (check "This process opens the store's door and read path and Auth on the cluster."
             (not (:could-not mine)) mine)
      (let [done (sign-in/setup! (:the-seam mine) {:email (:email first-person)})]
        (check "The setup makes the base when it is not made, and a first person of this run with a permission in the base that a gate accepts."
               (and (uuid? (:person done)) (= :accepted (:answer (:gave done))))
               done))
      (let [start! (fn [numbers] (let [started (copy! {:conductor conductor :conductor-port conductor-port
                                                       :port (free-port) :logs logs :numbers numbers})]
                                   (swap! copies conj started)
                                   started))
            a (start! nil)
            b (start! nil)
            c (start! checks/short-numbers)]
        (check "Three copies of the runtime start as three processes on the cluster and answer."
               (every? :ready? [a b c]) (mapv #(dissoc % :process) [a b c]))
        (checks/run! {:a (:url a) :b (:url b) :c (:url c) :tag tag :first first-person})
        (let [cookie (:cookie (drive/sign-in (:url b) first-person))
              program (fn [] (:credential (drive/program (:url b) "the cluster proof's program"
                                                         (fn [code] (drive/yes (:url b) cookie code {})))))
              restart! (fn [] (let [started (copy! {:conductor conductor :conductor-port conductor-port
                                                    :port (:port a) :logs logs :numbers nil})]
                                (swap! copies conj started)
                                started))]
          (runtime-killed! {:a a :b b :restart! restart!} (program))
          (store-worker-killed! (:url b) (program))
          (auth-worker-killed! (:url b) (program)))
        (many-roots-watched! (seam/auth-of (:the-seam mine))))
      (check "The proof ran to its end without a throw." true)
      (catch Throwable t
        (check "The proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (doseq [{:keys [^Process process]} @copies]
          (try (.destroyForcibly process) (catch Throwable _ nil)))
        (try ((:stop mine)) (catch Throwable _ nil))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
