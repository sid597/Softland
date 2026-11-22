(ns softland.runtime.worker-kill-proof
  "The two kills of Rama's workers from `softland.runtime.cluster-proof`,
  run alone: one copy of the runtime as a process of its own on a cluster
  that stays up; Rama's worker of the store's module killed while a series
  of acts is on its way; and Rama's worker of Auth killed while a
  connection is held. The steps are the cluster proof's own
  (`store-worker-killed!`, `auth-worker-killed!`), so the two proofs say
  the same and are held to the same checks. One command, from the
  repository root, against a Rama cluster on which the modules `Store` and
  `Auth` are launched and that holds nothing that is kept:

    clojure -M:runtime -m softland.runtime.worker-kill-proof --conductor 127.0.0.1 --conductor-port 1983

  State. It writes into the cluster's store and Auth, and leaves the log of
  its copy in a folder that it names at the start."
  (:require [clojure.java.io :as io]
            [com.rpl.rama :as rama]
            [softland.runtime.cluster-proof :as cluster-proof]
            [softland.runtime.drive :as drive]
            [softland.runtime.main :as main]
            [softland.runtime.sign-in :as sign-in]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io Closeable]))

(defn- arguments
  "Takes the arguments of the command line. Gives a map of them."
  [args]
  (into {} (map (fn [[flag value]] [(keyword (subs flag 2)) value])) (partition 2 args)))

(defn -main
  "Runs the two kills, prints the counts, and ends the process with a
  status."
  [& args]
  (let [{:keys [conductor conductor-port]} (arguments args)
        conductor (or conductor "127.0.0.1")
        conductor-port (or (some-> conductor-port parse-long) 1983)
        tag (Long/toString (System/currentTimeMillis) 36)
        logs (doto (io/file (System/getProperty "java.io.tmpdir") (str "softland-worker-kill-" tag)) (.mkdirs))
        first-person {:sub (str "wk-" tag) :email (str "wk-" tag "@example.com") :name "Worker Kill"}
        cluster (rama/open-cluster-manager {"conductor.host" conductor "conductor.port" conductor-port})
        mine (main/start {:cluster cluster :origin "http://localhost:0"})
        copies (atom [])]
    (println "The log of the copy is in" (.getPath logs))
    (try
      (sign-in/setup! (:the-seam mine) {:email (:email first-person)})
      (let [b (cluster-proof/copy! {:conductor conductor :conductor-port conductor-port
                                    :port (cluster-proof/free-port) :logs logs :numbers nil})]
        (swap! copies conj b)
        (check "A copy of the runtime starts as a process of its own on the cluster and answers."
               (:ready? b) (dissoc b :process))
        (let [cookie (:cookie (drive/sign-in (:url b) first-person))
              program (fn [label]
                        (:credential (drive/program (:url b) label
                                                    (fn [code] (drive/yes (:url b) cookie code {})))))]
          (cluster-proof/store-worker-killed! (:url b) (program "worker-kill-store"))
          (cluster-proof/auth-worker-killed! (:url b) (program "worker-kill-auth"))))
      (catch Throwable t
        (check "The worker-kill proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (doseq [{:keys [^Process process]} @copies]
          (try (.destroyForcibly process) (catch Throwable _ nil)))
        (try ((:stop mine)) (catch Throwable _ nil))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
