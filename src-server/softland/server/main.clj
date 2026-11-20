(ns softland.server.main
  "The program: it connects to Rama, opens the door, the read path, the
  sessions and the seam, makes the ground of sign-in, and serves the face
  over HTTP. One command, from the repository root:

    clojure -M:server -m softland.server.main --stand-in-google

  What it takes, each as an argument of the command line:

    --port n                 the port to listen on; 8400 when left out
    --origin text            this server's origin as a browser names it;
                             `http://localhost:<port>` when left out
    --conductor host         connect to a cluster that runs, whose
                             conductor is at that host, and on which the
                             modules `Store` and `Sessions` are deployed.
                             Left out, the program starts Rama's in-process
                             cluster and launches both modules in it: a
                             store that is thrown away when the program
                             ends
    --google-client-id text  the client id of this server at Google. Left
                             out, the environment's
                             SOFTLAND_GOOGLE_CLIENT_ID is read
    --stand-in-google        sign in with a stand-in for Google
                             (`softland.server.google-stand-in`): anybody
                             who reaches the server signs in as any
                             account. For one machine, with a store that
                             is thrown away

  It listens on the loopback address only. What ends TLS in front, and
  reaches it from outside, is another program's.

  The threads. Jetty hands each request to a virtual thread, so a request
  that waits for a person's yes, and a stream that stays open for hours,
  each hold a thread that costs little. Every call of the store runs on
  the seam's own virtual threads (`softland.server.seam`).

  State. `start` gives a map that holds everything opened, and its `:stop`
  closes it all. The program owns the in-process cluster when it started
  one, and the cluster's files are temporary and are Rama's.

  Not run by any check here: `--conductor`, against a cluster that keeps
  its data. The network rule that only programs on machines that Sid runs
  may reach Rama's ports (src-store/README.md, 11 and 12 October 2026,
  entry 4) is set by nothing in this program."
  (:require [clojure.string :as str]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [missionary.core :as m]
            [ring.adapter.jetty :as jetty]
            [softland.server.google :as google]
            [softland.server.google-stand-in :as stand-in]
            [softland.server.http :as http]
            [softland.server.module :as module]
            [softland.server.seam :as seam]
            [softland.server.sessions :as sessions]
            [softland.server.sign-in :as sign-in]
            [softland.store.door :as door]
            [softland.store.module :as store]
            [softland.store.read-path :as read-path])
  (:import [java.io Closeable]
           [java.util.concurrent Executors]
           [org.eclipse.jetty.server Server]
           [org.eclipse.jetty.util.thread QueuedThreadPool]))

(defn- arguments
  "Takes the arguments of the command line. Gives a map of them: a flag
  that takes a value holds the next argument, and one that takes none holds
  true."
  [args]
  (loop [args args got {}]
    (if-let [[flag & more] (seq args)]
      (case flag
        "--stand-in-google" (recur more (assoc got :stand-in-google true))
        ("--port" "--origin" "--conductor" "--google-client-id")
        (recur (rest more) (assoc got (keyword (subs flag 2)) (first more)))
        (recur more got))
      got)))

(defn start
  "Opens everything that the server stands on. Takes a map: `:cluster`, a
  cluster handle of Rama's on which the modules `Store` and `Sessions` run;
  `:origin`; `:google`, {:client-id} or nil; `:stand-in`, a stand-in for
  Google or nil; and `:app-path`, when a browser's program is served in
  front. Gives a map: `:the-seam`; `:handler`, the Ring handler of the
  face over HTTP; and `:stop`, a function of no argument that closes what
  this call opened. Gives a map that holds `:could-not` when a part could
  not be opened; nothing is then left open.

  It runs the setup of sign-in (`softland.server.sign-in/setup`), which
  writes the layer of the people when it is not made."
  [{:keys [cluster origin google stand-in app-path]}]
  (let [the-door (door/open cluster (rama/get-module-name store/Store))
        the-read-path (read-path/open cluster (rama/get-module-name store/Store))
        the-sessions (sessions/open cluster (rama/get-module-name module/Sessions))
        word (some (fn [x] (when (and (map? x) (:could-not x)) x)) [the-door the-read-path the-sessions])]
    (if word
      (do (when-not (:could-not the-sessions) (sessions/close! the-sessions))
          word)
      (let [the-seam (seam/open {:door the-door :read-path the-read-path :sessions the-sessions})
            stop (fn [] (seam/close! the-seam) (sessions/close! the-sessions) nil)
            ready (m/? (sign-in/setup the-seam))]
        (if (not= :ready ready)
          (do (stop) ready)
          {:the-seam the-seam
           :stop stop
           :handler (http/handler
                     {:the-seam the-seam
                      :origin origin
                      :app-path app-path
                      :stand-in stand-in
                      :google (when (and google (not stand-in))
                                {:client-id (:client-id google)
                                 :issuers google/issuers
                                 :keys-fn (fn [] (google/fetch-keys google/keys-url))})})})))))

(defn in-process-cluster
  "Starts Rama's in-process cluster and launches the store's module and the
  server's in it, each with four tasks on two threads. Gives the cluster,
  which is the cluster handle and is closed with `.close`. The store that
  it holds is thrown away when it is closed."
  []
  (let [cluster (rtest/create-ipc)]
    (rtest/launch-module! cluster store/Store {:tasks 4 :threads 2})
    (rtest/launch-module! cluster module/Sessions {:tasks 4 :threads 2})
    cluster))

(defn serve
  "Runs Jetty with a Ring handler on the loopback address. Takes the
  handler, the port, and when the caller wants, a function that is given
  Jetty's server before it starts. Gives the server, started and not
  joined. Each request runs on a virtual thread."
  ([handler port] (serve handler port nil))
  ([handler port configurator]
   (jetty/run-jetty handler
                    (cond-> {:host "127.0.0.1"
                             :port port
                             :join? false
                             :send-server-version? false
                             :thread-pool (doto (QueuedThreadPool.)
                                            (.setVirtualThreadsExecutor
                                             (Executors/newVirtualThreadPerTaskExecutor)))}
                      configurator (assoc :configurator configurator)))))

(defn -main
  "Starts the program as the namespace docstring says, prints where it
  listens, and stays until the process is ended."
  [& args]
  (let [{:keys [port origin conductor google-client-id stand-in-google]} (arguments args)
        port (or (some-> port parse-long) 8400)
        origin (or origin (str "http://localhost:" port))
        client-id (or google-client-id (System/getenv "SOFTLAND_GOOGLE_CLIENT_ID"))
        cluster (if conductor
                  (rama/open-cluster-manager {"conductor.host" conductor})
                  (in-process-cluster))
        started (start {:cluster cluster
                        :origin origin
                        :google (when (seq client-id) {:client-id client-id})
                        :stand-in (when stand-in-google (stand-in/make))})]
    (if (:could-not started)
      (do (println "The server could not start:" (pr-str started))
          (.close ^Closeable cluster)
          (System/exit 1))
      (let [^Server server (serve (:handler started) port)]
        (println (str "Softland's server listens on " origin
                      (if conductor
                        (str ", on the cluster whose conductor is at " conductor ".")
                        ", on an in-process cluster: its store is thrown away when this program ends.")))
        (when stand-in-google
          (println "It runs with a stand-in for Google: anybody who reaches it signs in as any account."))
        (when-not (or stand-in-google (seq client-id))
          (println "No way to sign in is set: give --google-client-id, or --stand-in-google."))
        (.join server)))))
