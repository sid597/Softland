(ns softland.runtime.command-review-proof
  "The proof of what the review of 26 November 2026 found in the command
  line: a command with an argument left out. One command, from the
  repository root:

    clojure -M:runtime -m softland.runtime.command-review-proof

  The command line runs as `command_proof.clj` runs it, through that file's
  own helpers: a process of its own for each command, against one copy of
  the runtime on Rama's in-process cluster. The parts that need no runtime
  are `command_pure_proof.clj`.

  State. A cluster that is thrown away at the end; folders for the command
  line's files, under the JVM's temporary folder."
  (:require [clojure.java.io :as io]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.command-proof]
            [softland.runtime.drive :as drive]
            [softland.wire.proof :refer [check finish]])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private soft @#'softland.runtime.command-proof/soft)
(def ^:private sign-in! @#'softland.runtime.command-proof/sign-in!)

(defn- value-at
  "Reads one place with the command line. Gives the value there."
  [home e k]
  (get-in (soft home "read" e k) [:out :place :value]))

(defn- seen
  "Takes what a command ended with. Gives its status and what it printed."
  [run]
  (select-keys run [:exit :out]))

(defn- an-argument-left-out
  [{:keys [home]}]
  (let [wrote (soft home "write" ":chair-7" ":colour" ":green")
        before (value-at home ":chair-7" ":colour")
        slip (soft home "write" ":chair-7" ":colour")
        after (value-at home ":chair-7" ":colour")
        nowhere (soft (io/file home "no-sign-in") "write" ":chair-7" ":colour")]
    (check "`write e k` with the value left out ends with 2 and says that the command could not be read; the place still reads as it did before."
           (and (= 0 (:exit wrote)) (= :green before)
                (= 2 (:exit slip)) (= :read-the-command (:could-not (:out slip)))
                (= :green after))
           {:before before :slip (seen slip) :after after})
    (check "A command with an argument left out is turned away before the sign-in is looked for: in a home that holds no sign-in it ends with 2 too."
           (and (= 2 (:exit nowhere)) (= :read-the-command (:could-not (:out nowhere))))
           (seen nowhere))))

(defn -main
  "Runs the proof, prints the counts, and ends the process with a status."
  [& _]
  (h/with-cluster
    (fn [{:keys [cluster stand-in]}]
      (let [copy (h/start-copy! {:cluster cluster :stand-in stand-in})
            url (:url copy)
            home (.toFile (Files/createTempDirectory "softland-command-review-proof" (make-array FileAttribute 0)))]
        (try
          (h/setup-first-person! copy)
          (let [cookie (:cookie (drive/sign-in url h/first-person))
                _ (sign-in! home url cookie nil)
                with {:home home}]
            (an-argument-left-out with))
          (check "The proof ran to its end without a throw." true)
          (catch Throwable t
            (check "The proof ran to its end without a throw." false (str t))
            (.printStackTrace t))
          (finally
            (h/stop-copy! copy))))))
  (finish))
