(ns softland.runtime.two-lines-proof
  "The proof where the store's line (Pieces 1 and 2) and the spine (Piece 4)
  meet: a watch that is open on two copies of the runtime, when its
  permission in the store is ended — directly, via a permission above it in
  the chain, or via a run-scoped permission — is told anew an answer that
  leaves its lane out (`:may-not-see`) and holds no fact, and stays open:
  the store's read path ends no watch for that, and a later write in the
  lane reaches it not.

  Runs either on an in-process cluster (no arguments) or against a real Rama
  cluster with two runtime processes (`--conductor HOST --conductor-port PORT`,
  as `softland.runtime.cluster-proof` does).

  State. Writes persons, lanes and acts into the cluster's store and Auth."
  (:require [clojure.java.io :as io]
            [softland.command.client :as client]
            [softland.runtime.drive :as drive]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.main :as main]
            [softland.runtime.permissions :as perm]
            [softland.runtime.sign-in :as sign-in]
            [softland.wire.credential :as credential]
            [softland.wire.proof :refer [check finish]]
            [com.rpl.rama :as rama])
  (:import [java.io Closeable File]
           [java.lang ProcessBuilder$Redirect]
           [java.net ServerSocket]
           [java.util UUID]))

(defn- free-port []
  (with-open [s (ServerSocket. 0)] (.getLocalPort s)))

(defn- wait-for [f ms]
  (let [ends (+ (System/currentTimeMillis) (long ms))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/currentTimeMillis) ends)
            (Thread/sleep 50)
            (recur))))))

(defn- line [url cred session]
  (let [conn (client/connect {:url url :credential cred :session session})]
    (if (:could-not conn)
      conn
      {:line conn :hello (client/hello conn)})))

(defn- close-line! [opened]
  (when-let [c (:line opened)]
    (client/close! c)))

(defn- say [{:keys [line]} word]
  (client/say line word 30000))

(defn- watching [{:keys [line]} query]
  (let [seen (atom [])]
    {:seen seen
     :id (client/watch line query (fn [msg] (swap! seen conj msg)))}))

(defn- value-of [seen]
  (some (fn [m]
          (or (get-in m [:anew :place :value])
              (some (fn [[_ pa]] (:value pa)) (:changed m))))
        (reverse seen)))

(defn- may-not-see?
  "Takes what a watch was told and a lane. Gives true when the last thing
  is an answer anew that leaves the lane out because the asker may not see
  it and holds no fact, and nothing told was an end: the watch is open."
  [seen lane]
  (let [last-msg (last seen)
        anew (:anew last-msg)]
    (and (some? anew)
         (not-any? #(contains? % :ended) seen)
         (= :no-fact-given (get-in anew [:place :holds]))
         (boolean (some #(and (= lane (:lane %)) (= :may-not-see (:why %)))
                        (:left-out anew))))))

(defn- send-facts! [opened lane perm facts]
  (let [hello (:hello opened)]
    (say opened {:word :send
                 :act {:version 1
                       :who (:who hello)
                       :session (:session hello)
                       :lane lane
                       :permission (perm/permission-id perm)
                       :facts facts}
                 :act-id (UUID/randomUUID)})))

(defn- run-checks! [url-a url-b first-person tag]
  (let [ada-in (drive/sign-in url-a (assoc first-person :call-her "Ada"))
        ada-cookie (:cookie ada-in)
        ada-prog (drive/program url-a (str "ada-" tag) (fn [c] (drive/yes url-a ada-cookie c {})))
        ada-a (line url-a (:credential ada-prog) nil)
        ada-b (line url-b (:credential ada-prog) nil)
        ada-who (:who (:hello ada-a))
        lane (:own-lane (:hello ada-a))
        ada-own (perm/first-permission ada-who lane)
        ;; Make two agents beneath Ada: Bea and Cal
        bea-secret (credential/make-secret)
        bea-made (say ada-a {:word :make-agent :uuid (UUID/randomUUID) :label "bea"
                             :hash (credential/hash-of bea-secret)})
        bea-who (perm/agent (:made bea-made))
        bea-cred (credential/text (:root bea-made) (:sign-in bea-made) bea-secret)
        cal-secret (credential/make-secret)
        cal-made (say ada-a {:word :make-agent :uuid (UUID/randomUUID) :label "cal"
                             :hash (credential/hash-of cal-secret)})
        cal-who (perm/agent (:made cal-made))
        cal-cred (credential/text (:root cal-made) (:sign-in cal-made) cal-secret)
        bea-a (line url-a bea-cred nil)
        bea-b (line url-b bea-cred nil)
        cal-a (line url-a cal-cred nil)
        cal-b (line url-b cal-cred nil)
        bea-perm (perm/given-permission bea-who lane ada-own :see-and-act)
        bea-perm-2 (perm/given-permission bea-who lane ada-own {:allows :see-and-act :id (UUID/randomUUID)})
        cal-perm (perm/given-permission cal-who lane bea-perm-2 :see)]
    (try
      ;; Seed fact and grant Bea :see-and-act on Ada's lane
      (send-facts! ada-a lane ada-own [{:e :secret :k :note :v "v0"}])
      (send-facts! ada-a lane ada-own [(perm/permission-fact bea-perm)])
      ;; 1. Direct permission ending across two copies
      (let [ada-watch (watching ada-a {:asks [:place :secret :note] :asker ada-who :lanes [lane]})
            bea-q {:asks [:place :secret :note] :asker bea-who :lanes [lane]}
            w-a (watching bea-a bea-q)
            w-b (watching bea-b bea-q)
            _ (wait-for #(= "v0" (value-of @(:seen w-a))) 10000)
            _ (wait-for #(= "v0" (value-of @(:seen w-b))) 10000)
            _ (send-facts! ada-a lane ada-own [{:e :secret :k :note :v "v1"}])
            got-v1-a (wait-for #(= "v1" (value-of @(:seen w-a))) 10000)
            got-v1-b (wait-for #(= "v1" (value-of @(:seen w-b))) 10000)
            ;; End Bea's permission through copy B
            ended (send-facts! ada-b lane ada-own [(perm/end-fact bea-perm)])
            revoked-a (wait-for #(may-not-see? @(:seen w-a) lane) 10000)
            revoked-b (wait-for #(may-not-see? @(:seen w-b) lane) 10000)
            cnt-a (count @(:seen w-a))
            cnt-b (count @(:seen w-b))
            ;; Write after permission ended: Ada's watch sees v2, Bea's two watches see nothing more
            _ (send-facts! ada-a lane ada-own [{:e :secret :k :note :v "v2"}])
            ada-saw-v2 (wait-for #(= "v2" (value-of @(:seen ada-watch))) 10000)
            _ (Thread/sleep 600)
            bea-act-after (send-facts! bea-a lane bea-perm [{:e :secret :k :note :v "intrude"}])]
        (check "A watch open on two copies of the runtime sees live changes while its permission stands; when its permission is ended in the store, it is told anew on both copies an answer that leaves the lane out (:may-not-see) and holds no fact, and stays open with no end; a later write in the lane reaches neither; and an act under the ended permission is refused."
               (and got-v1-a got-v1-b
                    (= :accepted (get-in ended [:answer :answer]))
                    revoked-a revoked-b
                    ada-saw-v2
                    (= cnt-a (count @(:seen w-a)))
                    (= cnt-b (count @(:seen w-b)))
                    (= :refused (get-in bea-act-after [:answer :answer]))
                    (= :permission-ended (get-in bea-act-after [:answer :reason])))
               {:got-v1 [got-v1-a got-v1-b]
                :ended (:answer ended)
                :revoked [revoked-a revoked-b]
                :ada-saw-v2 ada-saw-v2
                :counts-before [cnt-a cnt-b]
                :counts-after [(count @(:seen w-a)) (count @(:seen w-b))]
                :bea-act-after (:answer bea-act-after)})
        (client/leave (:line bea-a) (:id w-a))
        (client/leave (:line bea-b) (:id w-b))

        ;; 2. Chain revocation: Ada grants Bea again, Bea grants Cal, Ada ends Bea -> Cal's watches on both copies close off the lane
        (send-facts! ada-a lane ada-own [(perm/permission-fact bea-perm-2)])
        (send-facts! bea-a lane bea-perm-2 [(perm/permission-fact cal-perm)])
        (let [cal-q {:asks [:place :secret :note] :asker cal-who :lanes [lane]}
              cw-a (watching cal-a cal-q)
              cw-b (watching cal-b cal-q)
              got-c-a (wait-for #(= "v2" (value-of @(:seen cw-a))) 10000)
              got-c-b (wait-for #(= "v2" (value-of @(:seen cw-b))) 10000)
              end-above (send-facts! ada-b lane ada-own [(perm/end-fact bea-perm-2)])
              c-rev-a (wait-for #(may-not-see? @(:seen cw-a) lane) 10000)
              c-rev-b (wait-for #(may-not-see? @(:seen cw-b) lane) 10000)
              cc-a (count @(:seen cw-a))
              cc-b (count @(:seen cw-b))
              _ (send-facts! ada-a lane ada-own [{:e :secret :k :note :v "v3"}])
              ada-saw-v3 (wait-for #(= "v3" (value-of @(:seen ada-watch))) 10000)
              _ (Thread/sleep 600)]
          (check "When a permission above a watcher's own ends in the store, the watcher's watches on both copies of the runtime are told anew an answer that leaves the lane out (:may-not-see) and stay open with no end, and a later write in the lane reaches neither."
                 (and got-c-a got-c-b
                      (= :accepted (get-in end-above [:answer :answer]))
                      c-rev-a c-rev-b
                      ada-saw-v3
                      (= cc-a (count @(:seen cw-a)))
                      (= cc-b (count @(:seen cw-b))))
                 {:got-c [got-c-a got-c-b] :c-rev [c-rev-a c-rev-b]
                  :counts [[cc-a (count @(:seen cw-a))] [cc-b (count @(:seen cw-b))]]})
          (client/leave (:line cal-a) (:id cw-a))
          (client/leave (:line cal-b) (:id cw-b)))

        ;; 3. Run-scoped permission ending across two copies sharing one session
        (let [run-sess (:session (:hello cal-a))
              cal-b-same (line url-b cal-cred run-sess)
              run-perm (perm/run-permission cal-who lane ada-own run-sess :see)
              _ (send-facts! ada-a lane ada-own
                             [(perm/permission-fact run-perm)])
              rq {:asks [:place :secret :note] :asker cal-who :session run-sess :lanes [lane]}
              rw-a (watching cal-a rq)
              rw-b (watching cal-b-same rq)
              got-r-a (wait-for #(= "v3" (value-of @(:seen rw-a))) 10000)
              got-r-b (wait-for #(= "v3" (value-of @(:seen rw-b))) 10000)
              end-run (send-facts! ada-b lane ada-own
                                   [(perm/end-fact run-perm)])
              r-rev-a (wait-for #(may-not-see? @(:seen rw-a) lane) 10000)
              r-rev-b (wait-for #(may-not-see? @(:seen rw-b) lane) 10000)
              rc-a (count @(:seen rw-a))
              rc-b (count @(:seen rw-b))
              _ (send-facts! ada-a lane ada-own [{:e :secret :k :note :v "v4"}])
              ada-saw-v4 (wait-for #(= "v4" (value-of @(:seen ada-watch))) 10000)
              _ (Thread/sleep 600)]
          (check "A watch held under a run-scoped permission on two copies of the runtime, when that run permission ends in the store, is told anew on both copies an answer that leaves the lane out (:may-not-see) and stays open with no end, and a later write in the lane reaches neither."
                 (and got-r-a got-r-b
                      (= :accepted (get-in end-run [:answer :answer]))
                      r-rev-a r-rev-b
                      ada-saw-v4
                      (= rc-a (count @(:seen rw-a)))
                      (= rc-b (count @(:seen rw-b))))
                 {:got-r [got-r-a got-r-b] :r-rev [r-rev-a r-rev-b]
                  :counts [[rc-a (count @(:seen rw-a))] [rc-b (count @(:seen rw-b))]]})
          (client/leave (:line cal-a) (:id rw-a))
          (client/leave (:line cal-b-same) (:id rw-b))
          (close-line! cal-b-same))
        (client/leave (:line ada-a) (:id ada-watch)))
      (finally
        (doseq [c [cal-a cal-b bea-a bea-b ada-a ada-b]]
          (close-line! c))))))

(defn- start-process-copy! [conductor conductor-port logs]
  (let [port (free-port)
        url (str "http://localhost:" port)
        java (str (System/getProperty "java.home") File/separator "bin" File/separator "java")
        cmd [java "-Xss16m" "-Xmx2g" "-cp" (System/getProperty "java.class.path")
             "clojure.main" "-m" "softland.runtime.main" "serve"
             "--conductor" conductor "--conductor-port" (str conductor-port)
             "--port" (str port) "--stand-in-google" "--thrown-away"]
        log (io/file logs (str "two-lines-copy-" port ".log"))
        proc (-> (ProcessBuilder. ^java.util.List cmd)
                 (.redirectErrorStream true)
                 (.redirectOutput (ProcessBuilder$Redirect/appendTo log))
                 (.start))
        ready? (boolean (wait-for #(= 200 (:status (drive/request :get (str url "/health") {}))) 120000))]
    {:process proc :url url :port port :ready? ready?}))

(defn- arguments [args]
  (into {} (map (fn [[flag value]] [(keyword (subs flag 2)) value])) (partition 2 args)))

(defn -main [& args]
  (let [{:keys [conductor conductor-port]} (arguments args)
        tag (Long/toString (System/currentTimeMillis) 36)
        first-person {:sub (str "tl-" tag) :email (str "tl-" tag "@example.com") :name "Ada"}]
    (if conductor
      (let [conductor-port (or (some-> conductor-port parse-long) 1983)
            logs (doto (io/file (System/getProperty "java.io.tmpdir") (str "softland-two-lines-" tag)) (.mkdirs))
            cluster (rama/open-cluster-manager {"conductor.host" conductor "conductor.port" conductor-port})
            mine (main/start {:cluster cluster :origin "http://localhost:0"})
            copies (atom [])]
        (try
          (sign-in/setup! (:the-seam mine) {:email (:email first-person)})
          (let [a (start-process-copy! conductor conductor-port logs)
                b (start-process-copy! conductor conductor-port logs)]
            (swap! copies into [a b])
            (check "Two runtime processes start on the cluster for the two-lines proof."
                   (and (:ready? a) (:ready? b))
                   [(dissoc a :process) (dissoc b :process)])
            (run-checks! (:url a) (:url b) first-person tag))
          (catch Throwable t
            (check "The two-lines proof ran to its end without a throw." false (str t))
            (.printStackTrace t))
          (finally
            (doseq [{:keys [^Process process]} @copies]
              (try (.destroyForcibly process) (catch Throwable _ nil)))
            (try ((:stop mine)) (catch Throwable _ nil))
            (try (.close ^Closeable cluster) (catch Throwable _ nil)))))
      (let [cluster (main/in-process-cluster)
            stand-in (stand-in/make)
            mk (fn []
                 (let [port (free-port)
                       url (str "http://localhost:" port)
                       started (main/start {:cluster cluster :origin url :stand-in stand-in})]
                   (assoc started :url url :served (main/serve! started "127.0.0.1" port))))
            a (mk)
            b (mk)]
        (try
          (sign-in/setup! (:the-seam a) {:email (:email first-person)})
          (run-checks! (:url a) (:url b) first-person tag)
          (catch Throwable t
            (check "The two-lines proof ran to its end without a throw." false (str t))
            (.printStackTrace t))
          (finally
            (doseq [c [a b]]
              (try (jetty/stop! (:served c)) (catch Throwable _ nil))
              (try ((:stop c)) (catch Throwable _ nil)))
            (try (.close ^Closeable cluster) (catch Throwable _ nil))))))
    (finish)))
