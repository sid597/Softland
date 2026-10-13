(ns probe.other-tasks
  "Claims C7 and C8, run on Rama 1.6.0 in-process: another number of tasks.

  `probe.gate/Gate` owns the depot and has 4 tasks. It decides C1's records
  (one layer, 3,000) and C4's (six layers of 500, over all four depot
  partitions). Then, for one other number of tasks N, in a cluster of its own:

    C7. `probe.gate/Again` (N tasks) mirrors the depot and runs the same gate
        from the depot's beginning, the walk as it is. `probe.gate/AgainHop`
        (N tasks) does the same with `(|hash *layer)` at the head of the walk.
        Does Rama allow it? On which task of the second module is a record of
        depot partition p emitted? Are the tables' rows the same as the first
        module's, as sets of rows? Does every task hold every row? Is each
        layer still decided in its depot order?

    C8. `probe.gate/Fresh` (N tasks) owns a depot of its own. A client reads
        every partition of the first module's depot in its order
        (`foreign-depot-partition-info`, `foreign-depot-read`) and appends the
        records to it, one thread for each source partition, so each layer's
        records keep their order. Are the tables the same as sets of rows?

  N is asked for as 6 and then 3. Rama's docs say that a module's number of
  tasks must be a power of two; the probe asks Rama itself and writes the
  error whole. It then runs 8 and 2 as well, which are other numbers that the
  docs allow. Before any record is sent, `Fresh` is launched with N tasks: if
  Rama refuses that, the two mirroring modules are tried too, their errors
  written, and the cluster is closed.

  State: the modules' tables, and the log `logs/other-tasks.log`. A probe,
  not a design."
  (:require [clojure.string :as str]
            [com.rpl.rama :refer [foreign-append! foreign-depot-partition-info foreign-depot-read foreign-object-info]]
            [com.rpl.rama.test :as rtest]
            [probe.again :as again]
            [probe.gate :as g :refer [say yes-no]]))

(defn- try-launch!
  "Takes the cluster, a module, a label for it and a number of tasks. Gives
  true when Rama launched it. Else writes the error, whole, and gives false."
  [ipc module label n]
  (try
    (g/launch! ipc module n)
    (say (format "  Rama launched %s with %d tasks." label n))
    true
    (catch Throwable t
      (say (format "  Rama REFUSED to launch %s with %d tasks. The error: %s" label n (g/error-text t)))
      false)))

(defn- held-against-first!
  "Takes a label, the first module's tables, another module's tables and the
  layers with their numbers. Writes how the other's tables stand against the
  first's as sets of rows. Gives whether all of it is the same."
  [label first-tables other-tables layers]
  (let [ok? (again/report-module! label other-tables layers)
        first-rows (g/union first-tables "$$rows")
        rows-differ (for [[t tb] other-tables] [t (count (tb "$$rows")) (g/differences first-rows (tb "$$rows"))])
        answers-differ (g/differences (g/union first-tables "$$answers") (g/union other-tables "$$answers"))
        last-differ (g/differences (g/union first-tables "$$last") (g/union other-tables "$$last"))
        twice (- (g/entries other-tables "$$answers") (count (g/union other-tables "$$answers")))]
    (say (format "  %s: a record of depot partition p is emitted on task: %s" label (pr-str (g/source-tasks first-tables other-tables))))
    (say (format "  %s, `$$rows` on each task against the first module's %d rows: %s" label (count first-rows)
                 (str/join "; " (for [[t n d] rows-differ] (format "task %d holds %d, %d differ" t n d)))))
    (say (format "  %s, `$$answers` of all tasks as one set against the first module's: %d differ of %d. Answers held on two tasks: %d. `$$last` as one set: %d differ of %d."
                 label answers-differ (count (g/union first-tables "$$answers")) twice
                 last-differ (count (g/union first-tables "$$last"))))
    (and ok?
         (every? (fn [[_ _ d]] (zero? d)) rows-differ)
         (zero? answers-differ) (zero? last-differ) (zero? twice))))

(defn- copy-depot!
  "Takes the opened module that owns the full depot and the opened fresh
  module. One thread for each partition of the first depot reads its records
  in their order, 500 at a time, and appends them to the fresh module's depot.
  Gives how many records each partition gave."
  [first-module fresh]
  (let [depot (:depot first-module)
        readers (doall
                 (for [p (range (:num-partitions (foreign-object-info depot)))]
                   (future
                     (let [{:keys [start-offset end-offset]} (foreign-depot-partition-info depot p)]
                       (loop [from start-offset n 0]
                         (if (< from end-offset)
                           (let [to (min end-offset (+ from 500))
                                 records (foreign-depot-read depot p from to)]
                             (doseq [r records]
                               (foreign-append! (:depot fresh) r :append-ack))
                             (recur to (+ n (count records))))
                           n))))))
        counts (mapv deref readers)]
    (swap! (:sent fresh) + (reduce + counts))
    counts))

(defn- one-number!
  "Takes another number of tasks. Runs C7 and C8 for it on a cluster of its own."
  [n]
  (say "")
  (say (format "ANOTHER NUMBER OF TASKS: %d (the first module has 4)" n))
  (with-open [ipc (rtest/create-ipc)]
    (g/launch! ipc g/Gate 4)
    (let [first-module (g/open-module ipc g/Gate true)]
      (if-not (try-launch! ipc g/Fresh "a module that owns its depot" n)
        (do (try-launch! ipc g/Again "a module that mirrors the first module's depot" n)
            (try-launch! ipc g/AgainHop "a module that mirrors the depot, with the hop" n)
            (say (format "C7 VERDICT (%d tasks): NOT RUN. Rama refused to launch a module with %d tasks." n n))
            (say (format "C8 VERDICT (%d tasks): NOT RUN. Rama refused to launch a module with %d tasks." n n)))
        (let [fresh (g/open-module ipc g/Fresh true)
              layers (again/feed! first-module)
              first-tables (g/read-tables first-module again/read-names)
              total @(:sent first-module)]
          (say "  THE FIRST MODULE, 4 tasks")
          (again/report-module! "first" first-tables layers)

          (say (format "  C7, THE WALK AS IT IS, %d tasks" n))
          (if-not (try-launch! ipc g/Again "the module that mirrors the first module's depot" n)
            (say (format "C7 VERDICT (%d tasks, no hop): NOT RUN." n))
            (let [m (g/open-module ipc g/Again false)
                  _ (g/settle! m total)
                  same? (held-against-first! "no hop" first-tables (g/read-tables m again/read-names) layers)]
              (say (format "C7 VERDICT (%d tasks, no hop): Rama let a module with %d tasks mirror and consume the depot of a module with 4. Same rows as the first module's, every task holds every row, each layer in its depot order: %s."
                           n n (yes-no same?)))))

          (say (format "  C7, WITH (|hash *layer) AT THE HEAD OF THE WALK, %d tasks" n))
          (if-not (try-launch! ipc g/AgainHop "the module that mirrors the depot, with the hop" n)
            (say (format "C7 VERDICT (%d tasks, hop): NOT RUN." n))
            (let [m (g/open-module ipc g/AgainHop false)
                  _ (g/settle! m total)
                  same? (held-against-first! "hop" first-tables (g/read-tables m again/read-names) layers)]
              (say (format "C7 VERDICT (%d tasks, hop): same rows as the first module's, every task holds every row, each layer in its depot order: %s."
                           n (yes-no same?)))))

          (say (format "  C8, A FRESH MODULE WITH %d TASKS, FED THE FIRST DEPOT'S RECORDS BY A CLIENT" n))
          (let [counts (copy-depot! first-module fresh)
                _ (g/settle! fresh)
                same? (held-against-first! "fresh" first-tables (g/read-tables fresh again/read-names) layers)]
            (say "  Records read from the first depot, by partition:" (pr-str counts) ". Appended to the fresh module:" @(:sent fresh))
            (say (format "C8 VERDICT (%d tasks): same rows as the first module's, every task holds every row, each layer in its order: %s."
                         n (yes-no same?)))))))))

(defn -main
  "Runs every number of tasks given (6, 3, 8 and 2 when none is given), each
  on a cluster of its own, and ends the JVM. A first argument that starts with
  a slash is the path of a trial run's log."
  [& args]
  (let [trial-log (first (filter #(str/starts-with? % "/") args))
        numbers (or (seq (map parse-long (remove #(str/starts-with? % "/") args))) [6 3 8 2])]
    (g/run-probe!
     (or trial-log "other-tasks")
     (fn []
       (say "Rama 1.6.0, in-process clusters, one for each other number of tasks. 2 task threads, 2 workers for every module. No option of Rama is set.")
       (run! one-number! numbers)))))
