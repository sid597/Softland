(ns probe.cost
  "Claim C9, run on Rama 1.6.0 in-process, 4 tasks: what a copy costs, as an
  order of size.

  One record at a time, 100 records of one layer on one key, each replacing
  the one before. For each record: the clock starts, the record is appended
  (the append waits only for the depot), and one thread for each of the four
  tasks reads that task's `$$rows` again and again, 0.2 ms apart, until the
  row holds the record's id. The time from the start to that read is taken,
  for the deciding task and for the three others.

  Two series of 100:
    at once     the next record is appended as soon as the last one was
                readable on every task
    after rest  the next record is appended 500 ms after that, so the gate
                has no record waiting when it comes

  What the numbers include: the append, the wait for the next microbatch, the
  walk, the copy, the commit on that task, and the reader's own 0.2 ms step.
  The gate here also writes the probe's own tables and journal.

  State: the tables of `probe.gate/Gate`, and the log `logs/cost.log`. A
  probe, not a design."
  (:require [com.rpl.rama :refer [foreign-append! foreign-pstate foreign-select-one]]
            [com.rpl.rama.path :refer [keypath]]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say]])
  (:import [java.util.concurrent.locks LockSupport]))

(defn- until-readable
  "Takes the table `$$rows`, a partitioning key of one task, the row's key,
  the id to wait for and the nanoTime at which the clock started. Reads until
  the row holds the id. Gives the ms since the start."
  [rows pkey rk id t0]
  (loop []
    (if (= id (:id (foreign-select-one (keypath rk) rows {:pkey pkey})))
      (/ (- (System/nanoTime) t0) 1e6)
      (do (LockSupport/parkNanos 200000) (recur)))))

(defn- series!
  "Takes the opened module, a layer, how many records, and the ms to rest
  before each append. Sends the records one at a time. Gives, for each
  record, the ms until the append came back and task -> the ms until the row
  was readable there."
  [m layer n rest-ms]
  (let [rows (foreign-pstate (:ipc m) (:name m) "$$rows")
        rk [layer "k-00"]]
    (vec (for [i (range n)]
           (let [id (g/id-of layer i)
                 record {:layer layer :id id :key "k-00" :val i :replaces (when (pos? i) (g/id-of layer (dec i)))}
                 _ (when (pos? rest-ms) (Thread/sleep (long rest-ms)))
                 t0 (System/nanoTime)
                 readers (into {} (for [[t pk] (:pkeys m)] [t (future (until-readable rows pk rk id t0))]))
                 _ (foreign-append! (:depot m) record :append-ack)
                 appended (/ (- (System/nanoTime) t0) 1e6)]
             (swap! (:sent m) inc)
             {:appended appended :readable (into (sorted-map) (for [[t f] readers] [t @f]))})))))

(defn- three
  "Takes numbers. Gives the least, the middle and the most, as text."
  [xs]
  (let [v (vec (sort xs))]
    (format "least %.1f, middle %.1f, most %.1f" (first v) (nth v (quot (count v) 2)) (peek v))))

(defn- report!
  "Takes a label, the opened module, the layer and what `series!` gave.
  Writes the times."
  [label m layer measured]
  (let [decider (first (for [t (g/tasks-of m) :when (get (g/table m "$$last" t) layer)] t))]
    (say "")
    (say label)
    (say "  The layer was decided on task" decider ". Records:" (count measured) ". `$$last`:" (get (g/table m "$$last" decider) layer) ".")
    (say "  ms from the start until the append came back:" (three (map :appended measured)))
    (doseq [t (g/tasks-of m)]
      (say (format "  ms from the start until the row was readable on task %d%s: %s"
                   t (if (= t decider) " (deciding)" "") (three (map #(get-in % [:readable t]) measured)))))))

(defn -main
  "Runs the two series and ends the JVM."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "cost")
   (fn []
     (say "Rama 1.6.0, in-process cluster. One module, 4 tasks, 2 task threads, 2 workers. No option of Rama is set.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc g/Gate 4)
       (let [m (g/open-module ipc g/Gate true)
             n (if trial-log 10 100)
             at-once (series! m "c9-at-once" n 0)
             _ (g/settle! m)
             after-rest (series! m "c9-after-rest" n 500)
             _ (g/settle! m)]
         (report! "C9, ONE RECORD AT A TIME, THE NEXT AT ONCE" m "c9-at-once" at-once)
         (report! "C9, ONE RECORD AT A TIME, THE NEXT AFTER 500 ms OF REST" m "c9-after-rest" after-rest)
         (say "C9 VERDICT: see the two series above. An order of size only."))))))
