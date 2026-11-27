(ns softland.runtime.attacks.lost-replacement
  "Attack 5: when `:replace-secret` is taken by Auth and its answer over the
  line is lost before reaching the command line, the command line must not
  be locked out — neither on an immediate retry while `:previous-ms` still
  stands, nor after `:previous-ms` has passed."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.auth.client :as auth]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.drive :as drive]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.main :as main]
            [softland.wire.frame :as frame]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io File InputStream OutputStream]
           [java.net InetAddress ServerSocket Socket]
           [java.nio.file Files]
           [java.util.concurrent TimeUnit]))

(defn- command-sdeps
  "Preserves `-Dsoftland.store.path` isolation when launching the command
  line subprocess, matching `softland.runtime.command-proof`."
  []
  (if-let [store-path (not-empty (System/getProperty "softland.store.path"))]
    ["-Sdeps" (pr-str {:aliases {:command {:replace-paths [store-path "src-wire" "src-command"]
                                           :replace-deps  {'org.clojure/clojure {:mvn/version "1.12.4"}}
                                           :jvm-opts      ["-XX:TieredStopAtLevel=1"]}}})]
    []))

(defn- start-cli
  ^Process [^File home args]
  (let [cmd (into (into ["clojure"] (command-sdeps))
                  (into ["-M:command" "-m" "softland.command.main"] args))
        pb (ProcessBuilder. ^java.util.List cmd)]
    (.put (.environment pb) "SOFTLAND_HOME" (.getPath home))
    (.start pb)))

(defn- wait-cli
  [^Process proc]
  (let [out (future (slurp (.getInputStream proc)))
        err (future (slurp (.getErrorStream proc)))]
    (.waitFor proc 90 TimeUnit/SECONDS)
    (let [lines (remove str/blank? (str/split-lines @out))
          [outcome form] (frame/read-one (or (last lines) ""))]
      {:exit (.exitValue proc) :out (when (= :ok outcome) form) :err @err})))

(defn- soft
  [home & args]
  (wait-cli (start-cli home args)))

(defn- cli-sign-in!
  [home url cookie]
  (let [proc (start-cli home ["sign-in" "--url" url])
        err (.getErrorStream proc)
        shown (StringBuilder.)
        code (h/wait-for (fn []
                           (while (pos? (.available err))
                             (.append shown (char (.read err))))
                           (second (re-find #"code: ([A-Z0-9]{4}-[A-Z0-9]{4})" (str shown))))
                         60000)]
    (when code (drive/yes url cookie code {}))
    (wait-cli proc)))

(defn- start-drop-relay!
  "Starts a local TCP relay in front of `target-port`. While `:drop?` in the
  returned control map is true and `:cut!` is called, the relay aborts the
  active client TCP socket and stops forwarding server->client bytes so the
  in-flight WebSocket reply is lost."
  [target-port]
  (let [server (ServerSocket. 0 50 (InetAddress/getByName "127.0.0.1"))
        port (.getLocalPort server)
        active-clients (atom #{})
        block-s2c? (atom false)
        running? (atom true)]
    (.start (Thread/ofPlatform)
            (fn []
              (while @running?
                (try
                  (let [client-sock (.accept server)
                        upstream-sock (Socket. "127.0.0.1" (int target-port))]
                    (swap! active-clients conj client-sock)
                    ;; client -> upstream
                    (.start (Thread/ofVirtual)
                            (fn []
                              (try
                                (let [^InputStream in (.getInputStream client-sock)
                                      ^OutputStream out (.getOutputStream upstream-sock)
                                      buf (byte-array 8192)]
                                  (loop []
                                    (let [n (.read in buf)]
                                      (when (pos? n)
                                        (.write out buf 0 n)
                                        (.flush out)
                                        (recur)))))
                                (catch Throwable _ nil)
                                (finally
                                  (try (.close upstream-sock) (catch Throwable _ nil))
                                  (try (.close client-sock) (catch Throwable _ nil))
                                  (swap! active-clients disj client-sock)))))
                    ;; upstream -> client
                    (.start (Thread/ofVirtual)
                            (fn []
                              (try
                                (let [^InputStream in (.getInputStream upstream-sock)
                                      ^OutputStream out (.getOutputStream client-sock)
                                      buf (byte-array 8192)]
                                  (loop []
                                    (let [n (.read in buf)]
                                      (when (pos? n)
                                        (if @block-s2c?
                                          (do (try (.setSoLinger client-sock true 0) (catch Throwable _ nil))
                                              (try (.close client-sock) (catch Throwable _ nil)))
                                          (do (.write out buf 0 n)
                                              (.flush out)
                                              (recur)))))))
                                (catch Throwable _ nil)
                                (finally
                                  (try (.close client-sock) (catch Throwable _ nil))
                                  (try (.close upstream-sock) (catch Throwable _ nil))
                                  (swap! active-clients disj client-sock))))))
                  (catch Throwable _ nil)))))
    {:port port
     :url (str "http://localhost:" port)
     :block-s2c? block-s2c?
     :abort-active! (fn []
                      (reset! block-s2c? true)
                      (doseq [^Socket s @active-clients]
                        (try (.setSoLinger s true 0) (catch Throwable _ nil))
                        (try (.close s) (catch Throwable _ nil))))
     :resume! (fn [] (reset! block-s2c? false))
     :stop! (fn []
              (reset! running? false)
              (try (.close server) (catch Throwable _ nil)))}))

(defn -main [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      ;; Bind the relay first so its address is the runtime's own `:origin`
      ;; for both browser `POST /yes` and command-line connections.
      (let [copy-port (h/free-port)
            relay (start-drop-relay! copy-port)
            started (main/start {:cluster cluster
                                 :origin (:url relay)
                                 :stand-in stand-in
                                 :numbers {:previous-ms 1000}})
            copy (assoc started
                        :port copy-port
                        :url (:url relay)
                        :served (main/serve! started "127.0.0.1" copy-port))
            root-dir (.toFile (Files/createTempDirectory "softland-lost-replace"
                                                         (make-array java.nio.file.attribute.FileAttribute 0)))
            home-a (io/file root-dir "a")
            home-b (io/file root-dir "b")]
        (try
          (h/setup-first-person! copy)
          (let [signed (drive/sign-in (:url relay) (assoc h/first-person :call-her "Sid"))
                cookie (:cookie signed)
                _ (cli-sign-in! home-a (:url relay) cookie)
                _ (cli-sign-in! home-b (:url relay) cookie)
                drop-next-replace? (atom false)
                orig-replace auth/replace-secret]
            (with-redefs [auth/replace-secret
                          (fn [c m]
                            (let [res (orig-replace c m)]
                              (when (and (:replaced res)
                                         (compare-and-set! drop-next-replace? true false))
                                ((:abort-active! relay)))
                              res))]
              ;; Case A: replacement answer is lost; retry `replace-secret`
              ;; immediately (while `:previous-ms` stands), then wait past
              ;; `:previous-ms` and run `who`.
              (reset! drop-next-replace? true)
              (let [lost-a (soft home-a "replace-secret")
                    _ ((:resume! relay))
                    retry-a (soft home-a "replace-secret")
                    _ (Thread/sleep 1500)
                    who-a (soft home-a "who")
                    ;; Case B: replacement answer is lost, and `:previous-ms`
                    ;; expires before the next command (`who`) runs.
                    _ (reset! drop-next-replace? true)
                    lost-b (soft home-b "replace-secret")
                    _ ((:resume! relay))
                    _ (Thread/sleep 1500)
                    who-b (soft home-b "who")]
                (check "A replacement whose answer is lost does not lock the command line out."
                       (and (not= 0 (:exit lost-a))
                            (= 0 (:exit retry-a))
                            (= 0 (:exit who-a))
                            (keyword? (:who (:out who-a)))
                            (not= 0 (:exit lost-b))
                            (= 0 (:exit who-b))
                            (keyword? (:who (:out who-b))))
                       {:case-a {:lost lost-a :retry retry-a :who-after-grace who-a}
                        :case-b {:lost lost-b :who-after-grace who-b}}))))
          (finally
            ((:stop! relay))
            (h/stop-copy! copy))))))
  (finish))
