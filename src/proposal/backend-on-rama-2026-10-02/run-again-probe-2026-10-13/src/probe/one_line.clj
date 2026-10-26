(ns probe.one-line
  "A second question, run on Rama 1.6.0 in-process: when every act of one
  space is appended under one partition key, does a gate that is run again
  over the depot from its beginning decide every act the same? And when the
  same acts are spread over the partitions instead?

  The gate is a microbatch topology in both cases, and the same code. It
  stands in for any gate: it reads claims of a name off a depot, and the
  first claim to arrive owns the name. It reads no clock. Each claim goes to
  the task of its name, so the rows are on several tasks in both cases.

  The claims are the same 3,000 in both cases, of 200 names, in one fixed
  shuffled order. Only the key under which the depot places a claim differs:

    one line   every claim has the key \"base\", so the depot puts all of
               them on one partition
    spread     every claim has its own id as its key, so the depot spreads
               them over the partitions

  Two modules run the one gate (`add-gate`), as in `probe.run-again`. `First`
  owns the depot and decides each claim as it arrives. `Again` is launched
  when every claim is in the depot: it mirrors the depot of `First`, and its
  gate starts from the depot's beginning. No option of Rama is set.

  State. Each module has two tables of its own, written by its gate alone.
  Each case runs on a cluster of its own. The run keeps nothing else but its
  log file, `logs/one-line.log`.

  A probe, not a design: no table, name or form here is a proposal for the
  store. It requires nothing outside itself."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest])
  (:import [java.util Random]))

;; ---- the rule, pure ---------------------------------------------------------

(defn by-arrival
  "The rule of a gate that answers at once. Takes the id that the table holds
  for a name, or nil, and the id that claims it. Gives :owns or :refused."
  [held id]
  (if (or (nil? held) (= held id)) :owns :refused))

(defn with-line
  "Takes the lines that a name has, or nil, and what one decision was. Gives
  the lines with one more: who claimed, the verdict, the id of the microbatch
  that decided it, the depot partition that the claim came from, and the task
  that holds the name's row."
  [lines id verdict batch from at]
  (conj (or lines []) {:id id :verdict verdict :batch batch :from from :at at}))

;; ---- the gate, the same code in both modules and both cases ----------------

(defn add-gate
  "Adds the microbatch topology `gate` and its two tables to a module. Takes
  the module's `topologies`. What must hold: the module has a depot or a
  mirror of one under the name `*claims`. The gate starts from the depot's
  beginning. Each claim goes to the task of its name and is decided there,
  against what that task holds."
  [topologies]
  (let [mb (microbatch-topology topologies "gate")]
    (declare-pstate mb $$owner {String Object})
    (declare-pstate mb $$decided {String Object})
    (<<sources mb
      (source> *claims {:start-from :beginning} :> %batch)
      (%batch :> {:keys [*name *id]})
      (ops/current-microbatch-id :> *batch)
      (ops/current-task-id :> *from)
      (|hash *name)
      (ops/current-task-id :> *at)
      (local-select> (keypath *name) $$owner :> *held)
      (by-arrival *held *id :> *verdict)
      (<<if (nil? *held)
        (local-transform> [(keypath *name) (termval *id)] $$owner))
      (local-select> (keypath *name) $$decided :> *lines)
      (with-line *lines *id *verdict *batch *from *at :> *lines-after)
      (local-transform> [(keypath *name) (termval *lines-after)] $$decided))))

(defmodule First [setup topologies]
  (declare-depot setup *claims (hash-by :k))
  (add-gate topologies))

(defmodule Again [setup topologies]
  (mirror-depot setup *claims "probe.one-line/First" "*claims")
  (add-gate topologies))

;; ---- the hand --------------------------------------------------------------

(def names
  "The 200 names that are claimed."
  (mapv #(format "name-%03d" %) (range 200)))

(def claims
  "The 3,000 claims, in the one order in which both cases send them: each a
  name drawn with a fixed seed, and an id that says its place in the order."
  (let [r (Random. 20261013)]
    (vec (for [i (range 3000)]
           {:name (names (.nextInt r (count names))) :id (format "c-%04d" i)}))))

(defonce ^{:doc "The open writer of the run's log file, or nil."} log-writer (atom nil))

(defn say
  "Takes parts of a line. Writes them as one line to the run's log and to
  stdout behind the word PROBE."
  [& parts]
  (let [line (str/join " " parts)]
    (println (str "PROBE " line))
    (when-let [^java.io.Writer w @log-writer]
      (.write w line) (.write w "\n") (.flush w))))

(defn- read-all
  "Takes the cluster and a module's name. Gives, for each name, its owner and
  the lines of its decisions in the order in which the module made them."
  [ipc module]
  (let [owner (foreign-pstate ipc module "$$owner")
        decided (foreign-pstate ipc module "$$decided")]
    (into {} (for [n names]
               [n {:owner (foreign-select-one (keypath n) owner {:pkey n})
                   :lines (foreign-select-one (keypath n) decided {:pkey n})}]))))

(defn- microbatches
  "Takes what `read-all` gave. Gives in how many microbatches the module
  decided the claims."
  [read]
  (count (distinct (mapcat #(map :batch (:lines %)) (vals read)))))

(defn- run-case
  "One case, on a cluster of its own. Takes the case's label and the function
  that gives a claim its partition key. Runs the first run and the run again
  and writes what each gave and how they differ."
  [label key-of]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc First {:tasks 4 :threads 2 :workers 2})
    (let [first-name (get-module-name First)
          again-name (get-module-name Again)
          depot (foreign-depot ipc first-name "*claims")]
      (doseq [c claims]
        (foreign-append! depot (assoc c :k (key-of c)) :append-ack))
      (rtest/wait-for-microbatch-processed-count ipc first-name "gate" (count claims))
      (let [first-read (read-all ipc first-name)]
        (rtest/launch-module! ipc Again {:tasks 4 :threads 2 :workers 2})
        (rtest/wait-for-microbatch-processed-count ipc again-name "gate" (count claims))
        (let [again-read (read-all ipc again-name)
              differs (fn [f] (vec (filter #(not= (f (first-read %)) (f (again-read %))) names)))
              other-owner (differs :owner)
              other-order (differs #(mapv :id (:lines %)))]
          (say "")
          (say (str/upper-case label))
          (say "  Records in the depot, by partition:"
               (str/join ", " (for [p (range (:num-partitions (foreign-object-info depot)))]
                                (str p ": " (:end-offset (foreign-depot-partition-info depot p))))))
          (say "  Rows of the 200 names, by the task that holds them:"
               (str/join ", " (for [[t n] (sort (frequencies (map #(:at (first (:lines %))) (vals first-read))))]
                                (str t ": " n))))
          (say "  The first run decided the 3,000 claims in" (microbatches first-read)
               "microbatches. The run again decided them in" (microbatches again-read) ".")
          (say "  Names with the same owner in both runs:" (- (count names) (count other-owner))
               "of" (count names) ". With another owner:" (count other-owner) ".")
          (say "  Names whose claims were decided in the same order in both runs:"
               (- (count names) (count other-order)) "of" (count names)
               ". In another order:" (count other-order) ".")
          (doseq [n (take 3 other-owner)]
            (say (format "      %s: the first run gave it to %s, the run again to %s"
                         n (:owner (first-read n)) (:owner (again-read n))))))))))

(defn -main
  "Runs both cases, one after the other, and ends the JVM: status 0 when both
  reached their end, 1 when something threw."
  [& _]
  (io/make-parents "logs/one-line.log")
  (reset! log-writer (io/writer "logs/one-line.log"))
  (let [status (try
                 (say "Rama 1.6.0, in-process cluster, 4 tasks. The gate is a microbatch topology in both cases.")
                 (say "The same 3,000 claims of 200 names, in the same order, in both cases. No option of Rama is set.")
                 (run-case "one line: every claim under the one key \"base\"" (constantly "base"))
                 (run-case "spread: every claim under its own id as its key" :id)
                 0
                 (catch Throwable t
                   (say "THE RUN DID NOT REACH ITS END:" (str t))
                   (.printStackTrace t)
                   1))]
    (when-let [^java.io.Writer w @log-writer] (.close w))
    (shutdown-agents)
    (System/exit status)))
