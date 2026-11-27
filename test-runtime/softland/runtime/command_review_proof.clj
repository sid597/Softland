(ns softland.runtime.command-review-proof
  "The proof of what the review of 26 November 2026 found in the command
  line: a command with an argument left out, a file that is edited and sent
  again, the exit status, and the command line's own word for a program
  that is not signed in. One command, from the repository root:

    clojure -M:runtime -m softland.runtime.command-review-proof

  The command line runs as `command_proof.clj` runs it, through that file's
  own helpers: a process of its own for each command, against one copy of
  the runtime on Rama's in-process cluster. The parts that need no runtime
  are `command_pure_proof.clj`.

  State. A cluster that is thrown away at the end; folders for the command
  line's files, under the JVM's temporary folder."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [softland.command.client :as client]
            [softland.command.main]
            [softland.runtime.attacks.harness :as h]
            [softland.runtime.command-proof]
            [softland.runtime.drive :as drive]
            [softland.wire.proof :refer [check finish]])
  (:import [java.io File]
           [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def ^:private started @#'softland.runtime.command-proof/started)
(def ^:private ended @#'softland.runtime.command-proof/ended)
(def ^:private soft @#'softland.runtime.command-proof/soft)
(def ^:private wait-for @#'softland.runtime.command-proof/wait-for)
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

(defn- a-file-edited-and-sent-again
  [{:keys [home who lane]}]
  (let [file (io/file home "review-acts.edn")
        act (fn [n v] (pr-str {:version 1 :lane lane :permission [who lane lane]
                               :facts [{:e :review-shelf :k (keyword (str "slot-" n)) :v v}]}))
        _ (spit file (str/join "\n" [(act 0 0) (act 1 1) (act 2 2)]))
        first-run (soft home "send" (.getPath file))
        _ (spit file (str/join "\n" [(act 0 0) (act 1 :edited) (act 2 2)]))
        second-run (soft home "send" (.getPath file))
        answers (fn [run] (mapv :answer (:results (:out run))))
        slot-1 (value-at home ":review-shelf" ":slot-1")]
    (check "A file of three acts is sent, one act of it is edited so that the count stays, and it is sent again: the edited act is written, and reads back."
           (and (= 0 (:exit first-run)) (= 0 (:exit second-run))
                (every? (fn [answer] (= :accepted (:answer answer))) (answers second-run))
                (= :edited slot-1))
           {:first (:out first-run) :second (:out second-run) :slot-1 slot-1})
    (check "The two acts that were not edited are sent under the UUIDs that they had: each gets in the second run the answer that it got in the first, so the store took neither twice."
           (and (= 3 (count (answers first-run)) (count (answers second-run)))
                (= (get (answers first-run) 0) (get (answers second-run) 0))
                (= (get (answers first-run) 2) (get (answers second-run) 2))
                (not= (get (answers first-run) 1) (get (answers second-run) 1)))
           {:first (answers first-run) :second (answers second-run)})))

(defn- a-file-sent-again-in-another-session
  [{:keys [home who lane]}]
  (let [file (io/file home "review-acts-two.edn")
        _ (spit file (str/join "\n" (for [n (range 2)]
                                      (pr-str {:version 1 :lane lane :permission [who lane lane]
                                               :facts [{:e :review-crate :k (keyword (str "slot-" n)) :v n}]}))))
        first-run (soft home "send" (.getPath file))
        fresh (soft home "new-session")
        second-run (soft home "send" (.getPath file))
        results (fn [run] (:results (:out run)))]
    (check "A file that is sent, and sent again after `new-session`, goes under the same UUIDs, and the store takes no act of it a second time: each result of the second run carries the act-id of the first run and the answer `:not-mine`, and the run ends with 0."
           (and (= 0 (:exit first-run)) (= 0 (:exit fresh)) (= 0 (:exit second-run))
                (= 2 (count (results first-run)) (count (results second-run)))
                (every? (fn [result] (= :accepted (get-in result [:answer :answer]))) (results first-run))
                (= (map :act-id (results first-run)) (map :act-id (results second-run)))
                (every? (fn [result] (= {:answer :not-mine} (:answer result))) (results second-run)))
           {:first (seen first-run) :new-session (seen fresh) :second (seen second-run)})))

(defn- the-exit-status
  [{:keys [home url cookie ^File file]}]
  (let [no-home (io/file home "she-says-no")
        process (started no-home ["sign-in" "--url" url])
        err (.getErrorStream process)
        shown (StringBuilder.)
        code (wait-for (fn []
                         (while (pos? (.available err)) (.append shown (char (.read err))))
                         (second (re-find #"code: ([A-Z0-9]{4}-[A-Z0-9]{4})" (str shown))))
                       60000)
        _ (when code (drive/yes url cookie code {:say "no"}))
        said-no (ended process)]
    (check "`sign-in` ends with 3 when she says no at the page, and prints the runtime's own form, `{:state :no}`."
           (and code (= 3 (:exit said-no)) (= {:state :no} (:out said-no)))
           (seen said-no)))
  (let [word {:could-not :reach-the-runtime :said "the proof's own word, for a watch that could not be sent"}
        got (with-redefs [client/watch (fn [& _] word)]
              (let [running (future (#'softland.command.main/run "watch" [":lamp-2" ":lit" "--as" (.getPath file)]))
                    got (deref running 20000 :still-waiting)]
                (future-cancel running)
                got))]
    (check "When the library gives a word that holds `:could-not` for a watch, `watch` gives that word as its result and does not wait for the watch's end. The command runs here inside the proof, signed in against the runtime, with the library's `watch` replaced by one that gives the word."
           (= word got)
           got)))

(defn- not-signed-in
  [{:keys [home]}]
  (let [empty-home (io/file home "no-sign-in")
        who (soft empty-home "who")
        fresh (soft empty-home "new-session")]
    (check "A command in a home that holds no sign-in ends with 1 and says the command line's own word, `:find-a-credential`; `new-session` there does the same."
           (and (= [1 1] [(:exit who) (:exit fresh)])
                (= [:find-a-credential :find-a-credential] [(:could-not (:out who)) (:could-not (:out fresh))]))
           [(seen who) (seen fresh)])))

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
                signed (sign-in! home url cookie nil)
                with {:home home :url url :cookie cookie :file (io/file home "sign-in.edn")
                      :who (:who (:out signed)) :lane (:own-lane (:out signed))}]
            (an-argument-left-out with)
            (a-file-edited-and-sent-again with)
            (a-file-sent-again-in-another-session with)
            (the-exit-status with)
            (not-signed-in with))
          (check "The proof ran to its end without a throw." true)
          (catch Throwable t
            (check "The proof ran to its end without a throw." false (str t))
            (.printStackTrace t))
          (finally
            (h/stop-copy! copy))))))
  (finish))
