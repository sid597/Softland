(ns softland.browser.main
  "The program of the server with the browser's face in front: the same
  server as `softland.server.main`, whose handler is wrapped by
  `softland.browser.host`, so that one process serves the face over HTTP,
  the page `/app`, its files, and the Electric socket. One command, from
  the repository root, after the browser's program is built:

    clojure -M:server-browser -m softland.browser.main --stand-in-google

  It takes the arguments of `softland.server.main`, with another default
  port:

    --port n                 the port to listen on; 8401 when left out
    --origin text            this server's origin as a browser names it;
                             `http://localhost:<port>` when left out. It is
                             the one origin whose posts and sockets are taken
    --conductor host         connect to a cluster that runs, as
                             `softland.server.main` does; left out, Rama's
                             in-process cluster is started, with a store
                             that is thrown away when the program ends
    --google-client-id text  the client id of this server at Google; left
                             out, the environment's
                             SOFTLAND_GOOGLE_CLIENT_ID is read
    --stand-in-google        sign in with a stand-in for Google: anybody who
                             reaches the server signs in as any account

  It listens on the loopback address only.

  State. The cluster, when it started one, and what `softland.server.main/
  start` opened; both live as long as the process."
  (:require [com.rpl.rama :as rama]
            [softland.browser.host :as host]
            [softland.server.google-stand-in :as stand-in]
            [softland.server.main :as main])
  (:import [java.io Closeable]
           [org.eclipse.jetty.server Server]))

(def default-port
  "The port of this program when none is given: one beside the port of the
  server without the browser's face."
  8401)

(defn- arguments
  "Takes the arguments of the command line. Gives a map of them: a flag
  that takes a value holds the next argument, and one that takes none holds
  true. An argument that is no flag of this program is passed over."
  [args]
  (loop [args args got {}]
    (if-let [[flag & more] (seq args)]
      (case flag
        "--stand-in-google" (recur more (assoc got :stand-in-google true))
        ("--port" "--origin" "--conductor" "--google-client-id")
        (recur (rest more) (assoc got (keyword (subs flag 2)) (first more)))
        (recur more got))
      got)))

(defn -main
  "Starts the program as the namespace docstring says, prints where it
  listens, and stays until the process is ended."
  [& args]
  (let [{:keys [port origin conductor google-client-id stand-in-google]} (arguments args)
        port (or (some-> port parse-long) default-port)
        origin (or origin (str "http://localhost:" port))
        client-id (or google-client-id (System/getenv "SOFTLAND_GOOGLE_CLIENT_ID"))
        cluster (if conductor
                  (rama/open-cluster-manager {"conductor.host" conductor})
                  (main/in-process-cluster))
        started (main/start {:cluster cluster
                             :origin origin
                             :app-path "/app"
                             :google (when (seq client-id) {:client-id client-id})
                             :stand-in (when stand-in-google (stand-in/make))})]
    (if (:could-not started)
      (do (println "The server could not start:" (pr-str started))
          (.close ^Closeable cluster)
          (System/exit 1))
      (let [handler (host/wrap (:handler started) {:the-seam (:the-seam started) :origins #{origin}})
            ^Server server (main/serve handler port host/configure-websocket!)]
        (println (str "Softland's server, with the browser's face, listens on " origin
                      "; the browser's program is at " origin "/app"
                      (if conductor
                        (str ", on the cluster whose conductor is at " conductor ".")
                        ", on an in-process cluster: its store is thrown away when this program ends.")))
        (when stand-in-google
          (println "It runs with a stand-in for Google: anybody who reaches it signs in as any account."))
        (when-not (or stand-in-google (seq client-id))
          (println "No way to sign in is set: give --google-client-id, or --stand-in-google."))
        (.join server)))))
