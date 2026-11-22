(ns softland.runtime.main
  "The program: the runtime as one command. It opens a Rama cluster, the
  store's two clients and Auth's, the seam, and the faces, and serves them.
  It requires Rama, and everything of the runtime.

  Two commands:

    serve   start the runtime and stay
    setup   a deployment's birth: make the base and the first person, and
            end (`softland.runtime.sign-in/setup!`)

  The arguments, each with its value after it but the flags:

    --conductor host       the cluster whose conductor is at that host.
                           Left out: Rama's in-process cluster, whose store
                           is thrown away when the program ends
    --conductor-port n     the conductor's port, when it is not Rama's own
    --port n               the port to listen on; 8500 when left out
    --host address         the address to listen on; 127.0.0.1 when left out
    --origin text          this runtime's origin as a browser names it;
                           http://localhost:<port> when left out
    --google-client-id id  Google's client id, for the sign-in button; or
                           the environment's SOFTLAND_GOOGLE_CLIENT_ID
    --stand-in-google      a flag: sign-in through a stand-in for Google,
                           with which anybody signs in as any account
    --thrown-away          a flag: says that the cluster at --conductor
                           holds nothing that is kept. The stand-in starts
                           on a conductor's cluster only with it
    --first-person-email a the email of the first person's account, for
                           `setup`, and for `serve` on the in-process
                           cluster, which is born at each start
    --person uuid          for `setup`: the id that an earlier run printed
    --trusted-proxy a,b    the addresses of the proxies in front of the
                           runtime whose `X-Forwarded-For` is believed for
                           the pace of sign-ins
                           (`softland.runtime.http/options-of`)
    --<number> n           any of the seam's numbers by its name, such as
                           --secret-ms or --max-watches-per-connection
                           (`softland.runtime.seam/default-numbers`), for a
                           proof that cannot wait a week

  A runtime does not serve a store that is not born. On a conductor's
  cluster `serve` ends with a line that says to run `setup`: making the
  base by itself would turn a store that lost its data into a new, empty
  one without a word. On the in-process cluster, which is new at each
  start, `serve` runs the setup itself.

  The stand-in and a kept store. With the stand-in anybody who reaches the
  runtime signs in as any account that is invited. So it does not start on
  a conductor's cluster unless the opener says, with `--thrown-away`, that
  nothing there is kept: the proofs of the runtime run on such a cluster.

  State. What `start` opens, in the memory of the process, until `:stop`."
  (:require [clojure.string :as str]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.auth.client :as auth]
            [softland.auth.module :as auth-module]
            [softland.runtime.google :as google]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.http :as http]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.seam :as seam]
            [softland.runtime.sign-in :as sign-in]
            [softland.runtime.standing :as standing]
            [softland.store.door :as door]
            [softland.store.module :as store]
            [softland.store.read-path :as read-path])
  (:import [java.io Closeable]
           [java.util UUID]
           [org.eclipse.jetty.server Server]))

(def ^:private takes-a-value
  "The arguments that take a value: the program's own, and each of the
  seam's numbers by its name."
  (into #{"--port" "--host" "--origin" "--conductor" "--conductor-port" "--google-client-id"
          "--first-person-email" "--person" "--trusted-proxy"}
        (map (fn [k] (str "--" (name k))))
        (keys seam/default-numbers)))

(defn- arguments
  "Takes the arguments of the command line after the command. Gives a map
  of them: an argument that takes a value holds the next one, and a flag
  holds true."
  [args]
  (loop [args args got {}]
    (if-let [[flag & more] (seq args)]
      (cond
        (contains? #{"--stand-in-google" "--thrown-away"} flag)
        (recur more (assoc got (keyword (subs flag 2)) true))

        (contains? takes-a-value flag)
        (recur (rest more) (assoc got (keyword (subs flag 2)) (first more)))

        :else (recur more got))
      got)))

(defn start
  "Opens everything that the runtime stands on. Takes a map: `:cluster`, a
  cluster handle of Rama's on which the modules `Store` and `Auth` run;
  `:origin`; `:google`, {:client-id} or nil; `:stand-in`, a stand-in for
  Google or nil; `:trusted-proxies`, a set of addresses, or nil; and, when
  the opener wants, `:numbers`, the seam's. Gives
  a map: `:the-seam`; `:read-path` and `:standing`, for a proof that counts
  what is left open; `:options`, what the faces share; `:handler`, the
  Ring handler of the pages; `:accept`, who a connection to the line is;
  and `:stop`, a function of no argument that closes what this call
  opened. Gives a map that holds `:could-not` when a part could not be
  opened; nothing is then left open. It writes nothing."
  [{:keys [cluster origin google stand-in trusted-proxies numbers]}]
  (let [the-door (door/open cluster (rama/get-module-name store/Store))
        the-read-path (read-path/open cluster (rama/get-module-name store/Store))
        the-auth (auth/open cluster (rama/get-module-name auth-module/Auth))
        word (some (fn [x] (when (and (map? x) (:could-not x)) x)) [the-door the-read-path the-auth])]
    (if word
      (do (when-not (and (map? the-auth) (:could-not the-auth)) (auth/close! the-auth))
          word)
      (let [the-standing (standing/open the-auth)
            the-seam (seam/open {:door the-door :read-path the-read-path :auth the-auth
                                 :standing the-standing :numbers numbers})
            options (http/options-of
                     {:the-seam the-seam
                      :origin origin
                      :stand-in stand-in
                      :trusted-proxies trusted-proxies
                      :google (when (and google (not stand-in))
                                {:client-id (:client-id google)
                                 :issuers google/issuers
                                 :keys-fn (fn [] (google/fetch-keys google/keys-url))})})]
        {:the-seam the-seam
         :read-path the-read-path
         :standing the-standing
         :options options
         :handler (http/handler options)
         :accept (fn [request] (http/accept options request))
         :stop (fn []
                 (seam/close! the-seam)
                 (standing/close! the-standing)
                 (auth/close! the-auth)
                 nil)}))))

(defn in-process-cluster
  "Starts Rama's in-process cluster and launches the store's module and
  Auth in it, each with four tasks on two threads. Gives the cluster, which
  is the cluster handle and is closed with `.close`. What it holds is
  thrown away when it is closed."
  []
  (let [cluster (rtest/create-ipc)]
    (rtest/launch-module! cluster store/Store {:tasks 4 :threads 2})
    (rtest/launch-module! cluster auth-module/Auth {:tasks 4 :threads 2})
    cluster))

(defn serve!
  "Serves what `start` gave. Takes that map, the address and the port.
  Gives what `softland.runtime.jetty/serve` gives."
  [{:keys [the-seam handler accept]} host port]
  (jetty/serve {:the-seam the-seam :handler handler :line-path "/line" :accept accept
                :host host :port port}))

(defn- end!
  "Prints a line and ends the process with a status."
  [status line]
  (println line)
  (System/exit status))

(defn- uuid-of
  "Takes a text. Gives the UUID that it is, or nil."
  [text]
  (try (some-> text UUID/fromString) (catch Throwable _ nil)))

(defn -main
  "Runs a command as the namespace docstring says."
  [& args]
  (let [[command & more] args
        {:keys [port host origin conductor conductor-port google-client-id stand-in-google thrown-away
                first-person-email person trusted-proxy] :as given} (arguments more)
        numbers (into {} (keep (fn [k] (when-let [n (some-> (get given k) parse-long)] [k n])))
                      (keys seam/default-numbers))
        trusted-proxies (when (seq trusted-proxy)
                          (set (remove str/blank? (map str/trim (str/split trusted-proxy #",")))))
        port (or (some-> port parse-long) 8500)
        origin (or origin (str "http://localhost:" port))
        client-id (or google-client-id (System/getenv "SOFTLAND_GOOGLE_CLIENT_ID"))]
    (when-not (contains? #{"serve" "setup"} command)
      (end! 2 "Say what to do: serve, or setup."))
    (when (and stand-in-google conductor (not thrown-away))
      (end! 2 (str "The stand-in for Google signs anybody in as any account that is invited. It starts on a "
                   "conductor's cluster only when you say that nothing there is kept: add --thrown-away, "
                   "or leave out --stand-in-google.")))
    (let [cluster (if conductor
                    (rama/open-cluster-manager (cond-> {"conductor.host" conductor}
                                                 conductor-port (assoc "conductor.port" (parse-long conductor-port))))
                    (in-process-cluster))
          started (start {:cluster cluster
                          :origin origin
                          :google (when (seq client-id) {:client-id client-id})
                          :stand-in (when stand-in-google (stand-in/make))
                          :trusted-proxies trusted-proxies
                          :numbers numbers})
          close! (fn [] (when-not (:could-not started) ((:stop started))) (.close ^Closeable cluster))]
      (when (:could-not started)
        (.close ^Closeable cluster)
        (end! 1 (str "The runtime could not start: " (pr-str started))))
      (let [the-seam (:the-seam started)
            setup! (fn []
                     (when-not (and first-person-email (str/includes? first-person-email "@"))
                       (close!)
                       (end! 2 "Setup needs the first person's account: --first-person-email a@b."))
                     (when (and person (nil? (uuid-of person)))
                       (close!)
                       (end! 2 "Setup's --person must be a UUID."))
                     (let [done (sign-in/setup! the-seam {:email first-person-email :person (uuid-of person)})]
                       (if (:could-not done)
                         (do (close!)
                             (end! 1 (str "The setup did not end: " (pr-str done)
                                          (when (= :claim-in-use (:why done))
                                            " This email was set up before. If that run was cut off, run setup again with --person and the id that it printed."))))
                         (println (str "Setup: the first person is " (:person done)
                                       ". If this command was cut off, run it again with --person " (:person done)
                                       ". The base: " (name (:base done)) ". Her permission in it: "
                                       (pr-str (:gave done)) ".")))))]
        (case command
          "setup"
          (do (setup!) (close!) (System/exit 0))

          "serve"
          (let [born (sign-in/born? the-seam)]
            (cond
              (and (map? born) (:could-not born))
              (do (close!) (end! 1 (str "The store could not be read: " (pr-str born))))

              (and (false? born) conductor)
              (do (close!)
                  (end! 1 (str "This store is not born: it has no base. Run `setup` once for a new deployment. "
                               "If this deployment held data before, it has lost it: restore it, and do not set it up anew.")))

              (false? born) (setup!))
            (let [{:keys [^Server server]} (serve! started host port)]
              (println (str "Softland's runtime listens on " origin
                            (if conductor
                              (str ", on the cluster whose conductor is at " conductor ".")
                              ", on an in-process cluster: its store is thrown away when this program ends.")))
              (when stand-in-google
                (println "It runs with a stand-in for Google: anybody who reaches it signs in as any account that is invited."))
              (when-not (or stand-in-google (seq client-id))
                (println "No way to sign in is set: give --google-client-id, or --stand-in-google."))
              (.join server))))))))
