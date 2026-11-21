(ns softland.runtime.proof
  "The proof of the runtime on Rama's in-process cluster: three copies of
  the runtime in one JVM, over one store and one Auth, each served on a
  port of the loopback address, and met over real requests. One command,
  from the repository root:

    clojure -M:runtime -m softland.runtime.proof

  It starts the cluster, launches the store's module and Auth, opens three
  copies of what a runtime holds (`softland.runtime.main/start`), two with
  the seam's default numbers and one with short times, and serves each. It
  holds what a store that is not born is told, runs the setup, and then
  runs the checks that both proofs of the runtime share
  (`softland.runtime.checks`).

  What a pass shows, and what it does not. The copies share nothing but
  Rama, so a sentence that holds across them holds across copies. They are
  three clients in one JVM all the same: two processes, a cluster that
  keeps its data, and a process that is killed are the cluster proof's
  (`softland.runtime.cluster-proof`). Google is stood in for by a key pair
  of the proof's own.

  State. It changes nothing outside its process: the cluster is thrown
  away at the end."
  (:require [softland.runtime.checks :as checks]
            [softland.runtime.google-stand-in :as stand-in]
            [softland.runtime.jetty :as jetty]
            [softland.runtime.main :as main]
            [softland.runtime.sign-in :as sign-in]
            [softland.runtime.standing :as standing]
            [softland.store.read-path :as read-path]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io Closeable]
           [java.net ServerSocket]))

(defn- free-port
  "Gives a port of the loopback address that nothing listens on now."
  []
  (with-open [socket (ServerSocket. 0)]
    (.getLocalPort socket)))

(def ^:private first-person
  "The account of the first person, whom the setup makes."
  {:sub "1000001" :email "first@example.com" :name "First Person"})

(defn -main
  "Runs the proof, prints the counts, and ends the process with a status."
  [& _]
  (let [cluster (main/in-process-cluster)
        the-stand-in (stand-in/make)
        copy (fn [numbers]
               (let [port (free-port)
                     url (str "http://localhost:" port)
                     started (main/start {:cluster cluster :origin url :stand-in the-stand-in :numbers numbers})]
                 (assoc started :url url :served (main/serve! started "127.0.0.1" port))))
        a (copy nil)
        b (copy nil)
        c (copy checks/short-numbers)]
    (try
      (check "Three copies of the runtime open over one store and one Auth."
             (not-any? :could-not [a b c]) (keep :could-not [a b c]))
      (check "A store that is not born is told so: it has no base."
             (false? (sign-in/born? (:the-seam a))))
      (let [done (sign-in/setup! (:the-seam a) {:email (:email first-person)})
            again (sign-in/setup! (:the-seam b) {:email (:email first-person) :person (:person done)})
            other (sign-in/setup! (:the-seam b) {:email (:email first-person)})]
        (check "The setup makes the base and the first person, and gives her a permission in the base that a gate accepts; the store is then born, for every copy."
               (and (uuid? (:person done)) (= :made (:base done)) (= :accepted (:answer (:gave done)))
                    (true? (sign-in/born? (:the-seam b))))
               done)
        (check "The setup run again with the same person, through another copy, finds the base made and ends; run with a new person for the same email, it is refused, since the email names the first one for good."
               (and (= (:person done) (:person again)) (= :was-made (:base again))
                    (= :take-that (:could-not other)) (= :claim-in-use (:why other)))
               [again other]))
      (checks/run! {:a (:url a) :b (:url b) :c (:url c)
                    :tag "one"
                    :first first-person
                    :left (fn []
                            {:live-reads (reduce + (map (fn [copy] (:live-reads (read-path/open-count (:read-path copy)))) [a b c]))
                             :watched (reduce + (map (fn [copy] (standing/watched (:standing copy))) [a b c]))})})
      (check "The proof ran to its end without a throw." true)
      (catch Throwable t
        (check "The proof ran to its end without a throw." false (str t))
        (.printStackTrace t))
      (finally
        (doseq [copy [a b c]]
          (try (jetty/stop! (:served copy)) (catch Throwable _ nil))
          (try ((:stop copy)) (catch Throwable _ nil)))
        (try (.close ^Closeable cluster) (catch Throwable _ nil))))
    (finish)))
