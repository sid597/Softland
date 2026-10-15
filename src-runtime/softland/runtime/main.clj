(ns softland.runtime.main
  "The runtime, run: Rama's in-process cluster with the store's module, the
  runtime's service on it, and the HTTP way in on localhost. Not pure: it
  starts a cluster, opens a port, writes the admin token's file, reads the
  clock and takes random bytes, and fetches Google's keys at each sign-in.

  From the repository root:

    clojure -M:runtime-module -m softland.runtime.main --google-client-id <id>

  or with the client id in the environment variable
  `SOFTLAND_GOOGLE_CLIENT_ID`. `--port <n>` serves another port than 8400,
  and `--admin-token-file <path>` writes the admin token elsewhere than
  `softland.runtime.admin/default-file`. The client id is not secret
  (BUILD.md, \"The Google client, for Sid\").

  What a start does, in order (`start!`):

    1. starts Rama's in-process cluster (`com.rpl.rama.test/create-ipc`)
       and launches the store's module `Store` in it, with four tasks, as
       the store's door proof does (`softland.store.module`);
    2. opens the runtime's service on the module
       (`softland.runtime.service/open`), with the client id and a random
       source; Google's keys are the service's default, fetched from
       https://www.googleapis.com/oauth2/v3/certs at each sign-in and kept
       nowhere (`softland.runtime.service/fetch-google-keys`), and its clock
       is the JVM's;
    3. runs the service's start (`start!`): the base and the system's
       layers, made when missing (`softland.runtime.start`);
    4. makes a new admin token and writes it, 0600
       (`softland.runtime.admin`);
    5. serves HTTP on 127.0.0.1 (`softland.runtime.http/serve!`).

  The store is thrown away: the in-process cluster keeps its files in a
  temporary folder of Rama's and removes them when it closes, and nothing
  else is kept (BUILD.md, default 19). So each start is a first start with
  no person, and `-main` prints at each start how to make the first
  invitation. It says so when it starts.

  Inputs and results, by call:

    start!  {:client-id :port :admin-token-file :google-keys} ->
            {:ok {:ipc :service :http :port :origin :admin-token-file
                  :stop}} or {:refuse word …}
    -main   the words of the command line -> runs until the process ends

  `start!` is what the HTTP proof starts too, with a stand-in for Google's
  keys, a free port and an admin token file in a temporary folder.

  State and effects. The cluster, the service and the server live until
  `:stop` is called, or the process ends. `:stop` stops the server, removes
  the admin token's file when it still holds this start's token
  (`softland.runtime.admin/remove!`), and closes the cluster; `-main` calls
  it from a shutdown hook. The admin token is never printed.

  Callers. Sid, at his shell; the HTTP proof (`start!`)."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.runtime.admin :as admin]
            [softland.runtime.http :as http]
            [softland.runtime.service :as service]
            [softland.store.module :as module])
  (:import [java.security SecureRandom]))

(def ^:private random (SecureRandom.))

(defn random-bytes
  "Takes a count. Gives that many bytes from one `SecureRandom` of the
  process: the service's random source, and the admin token's. Not pure."
  [n]
  (let [b (byte-array n)] (.nextBytes ^SecureRandom random b) b))

(defn start!
  "Takes {:client-id :port :admin-token-file :google-keys}: the Google
  client id; the port (8400 when left out; 0 takes a free one); the admin
  token's file (`softland.runtime.admin/default-file` when left out); and
  Google's keys as the service takes them, a function of no argument that
  gives a JWKSet (the service's own fetch when left out). Starts the cluster, launches `Store`, opens and starts the service,
  writes a new admin token, and serves HTTP (the namespace docstring).
  Gives {:ok {:ipc :service :http :port :origin :admin-token-file :stop}},
  or the first refusal with what was started closed again."
  [{:keys [client-id port admin-token-file google-keys]
    :or {port 8400 admin-token-file admin/default-file}}]
  (let [ipc (rtest/create-ipc)
        close-ipc (fn [] (try (.close ^java.io.Closeable ipc) (catch Throwable _ nil)))]
    (try
      (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 2})
      (let [opened (service/open ipc (rama/get-module-name module/Store)
                                 (cond-> {:google-client-id client-id :random-bytes random-bytes}
                                   google-keys (assoc :google-keys google-keys)))]
        (if (and (map? opened) (contains? opened :refuse))
          (do (close-ipc) opened)
          (let [svc opened
                started (service/start! svc)]
            (if-not (:ok started)
              (do (close-ipc) started)
              (let [token (admin/make-token (random-bytes 32))
                    written (admin/write! admin-token-file token)]
                (if-not (:ok written)
                  (do (close-ipc) written)
                  (let [h (http/serve! svc {:port port :client-id client-id :admin-token token})]
                    {:ok {:ipc ipc :service svc :http h :port (:port h) :origin (:origin h)
                          :admin-token-file (:ok written)
                          :stop (fn [] ((:stop h)) (admin/remove! (:ok written) token) (close-ipc))}})))))))
      (catch Throwable t
        (close-ipc)
        {:refuse :runtime-not-started :why (str (.getName (class t)) " " (.getMessage t))}))))

(defn- args-of
  "Takes the words of the command line. Gives {:client-id :port
  :admin-token-file}, the client id from `--google-client-id` or else the
  environment variable `SOFTLAND_GOOGLE_CLIENT_ID`."
  [words]
  (let [m (into {} (map vec (partition 2 words)))]
    {:client-id (or (get m "--google-client-id") (System/getenv "SOFTLAND_GOOGLE_CLIENT_ID"))
     :port (some-> (get m "--port") parse-long)
     :admin-token-file (get m "--admin-token-file")}))

(defn -main
  "Starts the runtime (`start!`), prints its address and how to make the
  first invitation, and runs until the process ends. Exits 1 with the
  refusal when no client id is given or the start is refused."
  [& words]
  (let [{:keys [client-id port admin-token-file]} (args-of words)]
    (when (or (nil? client-id) (= "" client-id))
      (binding [*out* *err*]
        (println "Give the Google client id: --google-client-id <id>, or SOFTLAND_GOOGLE_CLIENT_ID."))
      (System/exit 1))
    (println "Starting the Softland runtime. Its store is Rama's in-process cluster:"
             "everything in it is thrown away when this process ends.")
    (flush)
    (let [r (start! (cond-> {:client-id client-id}
                      port (assoc :port port)
                      admin-token-file (assoc :admin-token-file admin-token-file)))]
      (if-not (:ok r)
        (do (binding [*out* *err*] (println "refused:" (pr-str r)))
            (shutdown-agents)
            (System/exit 1))
        (let [{:keys [origin admin-token-file stop]} (:ok r)]
          (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable (fn [] (stop))))
          (println "The runtime is at" origin "(bound to 127.0.0.1 only).")
          (println "The admin token for the command line is in" admin-token-file "(0600).")
          (println "The store is new, so no person is in it yet. Make the first invitation,"
                   "yours, from another shell:")
          (println (str "  clojure -M:runtime-module -m softland.runtime.command invite --base see-and-act"
                        (when (not= origin "http://localhost:8400") (str " --at " origin))
                        (when (not= admin-token-file admin/default-file) (str " --admin-token-file " admin-token-file))))
          (println "then open the link it prints at" origin "and sign in with Google.")
          (println "Stop with Ctrl-C; the store goes with the process.")
          (flush)
          @(promise))))))
