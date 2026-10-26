(ns probe.run-again
  "One question, run on Rama 1.6.0 in-process: when a gate that decides is
  run again over the same depot from its beginning, does it decide the same?

  The gate here stands in for any gate. It reads claims of a name off a
  depot and decides who owns the name. Two rules run side by side on every
  claim, each into a table of its own:

    by arrival   the first claim to arrive owns the name, and a later one is
                 refused. This is what a gate that answers a maker at once
                 does.
    by time      the claim with the earliest `:time` owns the name, whenever
                 it arrives. `:time` is written on the claim before the
                 depot, by the sender. Under this rule an owner can change
                 after it was given.

  Two modules run the one gate (`add-gate`). `First` owns the depot and
  decides each claim as it arrives. `Again` is launched when every claim is
  in the depot: it mirrors the depot of `First`, and its gate starts from the
  depot's beginning. That is the way Rama's docs give for making every PState
  again from the depots (reference/rama/docs/19-operating-rama.md, \"Task
  scaling\"). No option of Rama is set in either module.

  The claims. The depot partitions by `:k`. On one partition: V, then a few
  thousand fillers, then X, then W. Only when `First` has decided X is Y
  sent, to another partition, where it stands near the start. X and Y claim
  one name; V and W claim another. Y carries an earlier `:time` than X, and W
  an earlier one than V: a sender whose clock is behind, or whose claim was
  slow on its way.

  State. Each module has three tables of its own, written by its gate alone.
  The run keeps nothing else but its log file, `logs/run-again.log`.

  A probe, not a design: no table, name or form here is a proposal for the
  store."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

;; ---- the two rules, pure ---------------------------------------------------

(defn by-arrival
  "The rule of a gate that answers at once. Takes the id that the table holds
  for a name, or nil, and the id that claims it. Gives :owns or :refused."
  [held id]
  (if (or (nil? held) (= held id)) :owns :refused))

(defn by-time
  "The rule that reads only the time written on a claim. Takes what the table
  holds for a name, {:id :time} or nil, and a claim's id and time. Gives what
  the table holds after: the claim with the earliest time."
  [held id time]
  (if (or (nil? held) (< (long time) (long (:time held))))
    {:id id :time time}
    held))

(defn with-line
  "Takes the lines that a name has, or nil, and what one decision was. Gives
  the lines with one more: who claimed, the verdict by arrival, the owner by
  time after this claim, the id of the microbatch that decided it, and the
  depot partition that the claim came from."
  [lines id verdict kept batch from]
  (conj (or lines []) {:id id :verdict verdict :by-time (:id kept) :batch batch :from from}))

;; ---- the gate, the same code in both modules -------------------------------

(defn add-gate
  "Adds the microbatch topology `gate` and its three tables to a module.
  Takes the module's `topologies`. What must hold: the module has a depot or
  a mirror of one under the name `*claims`. The gate starts from the depot's
  beginning. Each claim goes to the task of its name, and both rules are
  applied there, against what that task holds."
  [topologies]
  (let [mb (microbatch-topology topologies "gate")]
    (declare-pstate mb $$owner {String Object})
    (declare-pstate mb $$owner-by-time {String Object})
    (declare-pstate mb $$decided {String Object})
    (<<sources mb
      (source> *claims {:start-from :beginning} :> %batch)
      (%batch :> {:keys [*name *id *time]})
      (ops/current-microbatch-id :> *batch)
      (ops/current-task-id :> *from)
      (|hash *name)
      (local-select> (keypath *name) $$owner :> *held)
      (by-arrival *held *id :> *verdict)
      (<<if (nil? *held)
        (local-transform> [(keypath *name) (termval *id)] $$owner))
      (local-select> (keypath *name) $$owner-by-time :> *held-by-time)
      (by-time *held-by-time *id *time :> *kept)
      (local-transform> [(keypath *name) (termval *kept)] $$owner-by-time)
      (local-select> (keypath *name) $$decided :> *lines)
      (with-line *lines *id *verdict *kept *batch *from :> *lines-after)
      (local-transform> [(keypath *name) (termval *lines-after)] $$decided))))

(defmodule First [setup topologies]
  (declare-depot setup *claims (hash-by :k))
  (add-gate topologies))

(defmodule Again [setup topologies]
  (mirror-depot setup *claims "probe.run-again/First" "*claims")
  (add-gate topologies))

;; ---- the hand --------------------------------------------------------------

(def fillers
  "How many claims stand between V and X on their partition."
  3000)

(defonce ^{:doc "The open writer of the run's log file, or nil."} log-writer (atom nil))

(defn say
  "Takes parts of a line. Writes them as one line to the run's log and to
  stdout behind the word PROBE."
  [& parts]
  (let [line (str/join " " parts)]
    (println (str "PROBE " line))
    (when-let [^java.io.Writer w @log-writer]
      (.write w line) (.write w "\n") (.flush w))))

(defn- claim [k name id time] {:k k :name name :id id :time time})

(defn- filler-name [i] (format "filler-%05d" i))

(defn- tables
  "Takes the cluster and a module's name. Gives clients of its three tables."
  [ipc module]
  {:owner (foreign-pstate ipc module "$$owner")
   :owner-by-time (foreign-pstate ipc module "$$owner-by-time")
   :decided (foreign-pstate ipc module "$$decided")})

(defn- row [table name] (foreign-select-one (keypath name) table {:pkey name}))

(defn- read-name
  "Takes a module's tables and a name. Gives what both rules hold for it and
  the lines of its decisions, in the order in which they were made."
  [tables name]
  {:by-arrival (row (:owner tables) name)
   :by-time (:id (row (:owner-by-time tables) name))
   :decided (row (:decided tables) name)})

(defn- show
  "Takes a label and what `read-name` gave. Writes it as lines."
  [label {:keys [by-arrival by-time decided]}]
  (say (format "  %-34s owner by arrival: %s · owner by time: %s" label by-arrival by-time))
  (doseq [{:keys [id verdict batch from] :as line} decided]
    (say (format "      %s  %-8s  owner by time after it: %s  (microbatch %s, from partition %s)"
                 id (name verdict) (:by-time line) batch from))))

(defn- batches-of-fillers
  "Takes a module's tables. Gives in how many microbatches its gate decided
  the fillers, and the most fillers in one."
  [tables]
  (let [sizes (frequencies (for [i (range fillers)]
                             (:batch (first (row (:decided tables) (filler-name i))))))]
    {:microbatches (count sizes) :most-in-one (apply max (vals sizes))}))

(defn- compare-runs
  "Takes a label and one value of each run. Writes whether they are the same."
  [label first-run run-again]
  (say (format "  %-52s %s" label
               (if (= first-run run-again)
                 (str "the same: " first-run)
                 (str "DIFFERENT: the first run " first-run ", the run again " run-again)))))

(defn- run
  "The whole run, on an open cluster."
  [ipc]
  (rtest/launch-module! ipc First {:tasks 4 :threads 2 :workers 2})
  (let [first-name (get-module-name First)
        again-name (get-module-name Again)
        depot (foreign-depot ipc first-name "*claims")
        first-tables (tables ipc first-name)
        sent (atom 0)
        send! (fn [c] (foreign-append! depot c :append-ack) (swap! sent inc))
        settle! (fn [] (rtest/wait-for-microbatch-processed-count ipc first-name "gate" @sent))
        end-of (fn [p] (:end-offset (foreign-depot-partition-info depot p)))]
    (say "Rama 1.6.0, in-process cluster. Two modules, 4 tasks each:" first-name "and" again-name)
    (say "No option of Rama is set. The gate is the same code in both.")

    ;; two keys that the depot puts on two partitions
    (doseq [i (range 8)]
      (send! (claim (str "k" i) (str "find-k" i) (str "find-k" i) 0)))
    (settle!)
    (let [partition-of (into {} (for [i (range 8)]
                                  [(str "k" i) (:from (first (row (:decided first-tables) (str "find-k" i))))]))
          key-a "k0"
          key-b (first (filter #(not= (partition-of %) (partition-of key-a))
                               (map #(str "k" %) (range 1 8))))
          _ (when-not key-b (throw (ex-info "no two keys on two partitions" {:partition-of partition-of})))
          part-a (partition-of key-a)
          part-b (partition-of key-b)]

      ;; the first run: V, the fillers, X and W on one partition
      (send! (claim key-a "claimed-on-one-partition" "V" 10))
      (doseq [i (range fillers)]
        (send! (claim key-a (filler-name i) (filler-name i) 20)))
      (send! (claim key-a "claimed-from-two-partitions" "X" 2000))
      (let [x-at (dec (end-of part-a))]
        (send! (claim key-a "claimed-on-one-partition" "W" 5))
        (settle!)
        (let [before-y (read-name first-tables "claimed-from-two-partitions")]
          ;; only now, with X decided, is Y sent, to the other partition
          (send! (claim key-b "claimed-from-two-partitions" "Y" 1000))
          (let [y-at (dec (end-of part-b))]
            (settle!)
            (say "")
            (say "WHERE THE CLAIMS STAND IN THE DEPOT")
            (say (format "  X is at position %d of partition %d. It was sent first, and decided before Y was sent." x-at part-a))
            (say (format "  Y is at position %d of partition %d. It was sent last." y-at part-b))
            (say (format "  V and W are both on partition %d, V before the %d fillers and W after X." part-a fillers))
            (say "  Records in the depot, by partition:"
                 (str/join ", " (for [p (range (:num-partitions (foreign-object-info depot)))]
                                  (str p ": " (end-of p)))))

            (let [first-two (read-name first-tables "claimed-from-two-partitions")
                  first-one (read-name first-tables "claimed-on-one-partition")
                  first-batches (batches-of-fillers first-tables)]
              (say "")
              (say "THE FIRST RUN (module First, deciding each claim as it arrived)")
              (say "  Before Y was sent, the name claimed from two partitions had: owner by arrival"
                   (:by-arrival before-y) "· owner by time" (:by-time before-y))
              (show "claimed from two partitions" first-two)
              (show "claimed on one partition" first-one)
              (say "  The fillers were decided in" (:microbatches first-batches)
                   "microbatches, at most" (:most-in-one first-batches) "in one.")

              ;; the run again: a new module over the same depot, from its beginning
              (rtest/launch-module! ipc Again {:tasks 4 :threads 2 :workers 2})
              (rtest/wait-for-microbatch-processed-count ipc again-name "gate" @sent)
              (let [again-tables (tables ipc again-name)
                    again-two (read-name again-tables "claimed-from-two-partitions")
                    again-one (read-name again-tables "claimed-on-one-partition")
                    again-batches (batches-of-fillers again-tables)]
                (say "")
                (say "THE RUN AGAIN (module Again, the same gate over the same depot, from its beginning)")
                (show "claimed from two partitions" again-two)
                (show "claimed on one partition" again-one)
                (say "  The fillers were decided in" (:microbatches again-batches)
                     "microbatches, at most" (:most-in-one again-batches) "in one.")

                (say "")
                (say "THE TWO RUNS, HELD AGAINST EACH OTHER")
                (compare-runs "two partitions, owner by arrival"
                              (:by-arrival first-two) (:by-arrival again-two))
                (compare-runs "two partitions, the order of the decisions"
                              (mapv :id (:decided first-two)) (mapv :id (:decided again-two)))
                (compare-runs "two partitions, owner by time at the end"
                              (:by-time first-two) (:by-time again-two))
                (say (format "  %-52s %s" "two partitions, owner by time while the first ran"
                             (str "it was " (:by-time before-y) " until Y arrived, then " (:by-time first-two))))
                (compare-runs "one partition, owner by arrival"
                              (:by-arrival first-one) (:by-arrival again-one))
                (compare-runs "one partition, the order of the decisions"
                              (mapv :id (:decided first-one)) (mapv :id (:decided again-one)))
                (compare-runs "one partition, owner by time at the end"
                              (:by-time first-one) (:by-time again-one))))))))))

(defn -main
  "Runs the probe once and ends the JVM: status 0 when the run reached its
  end, 1 when something threw."
  [& _]
  (io/make-parents "logs/run-again.log")
  (reset! log-writer (io/writer "logs/run-again.log"))
  (let [status (try
                 (with-open [ipc (rtest/create-ipc)]
                   (run ipc))
                 0
                 (catch Throwable t
                   (say "THE RUN DID NOT REACH ITS END:" (str t))
                   (.printStackTrace t)
                   1))]
    (when-let [^java.io.Writer w @log-writer] (.close w))
    (shutdown-agents)
    (System/exit status)))
