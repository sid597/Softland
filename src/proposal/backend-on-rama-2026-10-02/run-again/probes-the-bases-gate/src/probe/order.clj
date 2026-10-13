(ns probe.order
  "Claims C1 and C2, run on Rama 1.6.0 in-process, 4 tasks.

  C1. Does a later record of a microbatch read the rows that the earlier
  records of the same batch wrote on the deciding task? Every record of a
  layer says that it replaces the record before it on its key, so one record
  that does not see its predecessor's row is refused.

  C2. Do the rows that the deciding task sends to every task with `|all`
  arrive there in the order in which they were sent? Every task is read with
  one select, under a partitioning key found to land on it. A key that ends on
  an older row on any task is the failure. The probe's journal also counts, on
  every task, the copies that were written out of the sent order.

  Seven layers, one after the other, in one module (`probe.gate/Gate`):

    c1-live        3,000 records appended while the gate runs
    c1-paused      3,000 appended while the gate is paused, so the batches
                   are as long as Rama makes them
    c2-yield       as c1-paused, and the walk calls `(yield-if-overtime)`
    c2-slow        as c1-paused, and the walk keeps the thread busy for 200
                   microseconds on every record, with no yield
    c2-slow-yield  as c2-slow, and the walk calls `(yield-if-overtime)`, so
                   that it is sure to be over the 5 ms after which that yields
    c2-long        20,000 records, appended while the gate is paused
    c2-long-yield  as c2-long, and the walk calls `(yield-if-overtime)`

  State: the module's tables, and the log `logs/order.log`. A probe, not a
  design."
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(def layers
  "The layers in the order in which they are run: name, records, keys, how."
  [["c1-live" 3000 50 :live]
   ["c1-paused" 3000 50 :paused]
   ["c2-yield" 3000 50 :paused]
   ["c2-slow" 3000 50 :paused]
   ["c2-slow-yield" 3000 50 :paused]
   ["c2-long" 20000 50 :paused]
   ["c2-long-yield" 20000 50 :paused]])

(def trial-layers
  "Small layers, for a trial run that only shows that the probe runs."
  [["c1-live" 300 50 :live]
   ["c2-slow" 300 50 :paused]
   ["c2-slow-yield" 300 50 :paused]])

(defn -main
  "Runs the seven layers and ends the JVM. With one argument, the path of a
  log file, it runs the small trial layers instead and writes there."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "order")
   (fn []
     (say "Rama 1.6.0, in-process cluster. One module, 4 tasks, 2 task threads, 2 workers. No option of Rama is set.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc g/Gate 4)
       (let [m (g/open-module ipc g/Gate true)]
         (say "A partitioning key for every task:" (pr-str (:pkeys m)))
         (doseq [[layer n-records n-keys how] (if trial-log trial-layers layers)]
           (let [facts (g/chain-scenario! m "gate" layer n-records n-keys how)]
             (say (format "  C1 VERDICT (%s): every record accepted, so every record read the rows of the records before it: %s. %d of %d accepted, in %d microbatches, at most %d in one."
                          layer (yes-no (g/all-accepted? facts n-records))
                          (:accepted facts) n-records (:microbatches facts) (:most-in-one facts)))
             (say (format "  C2 VERDICT (%s): every task holds the last row of every key's chain: %s. Every task holds the deciding task's row for every key: %s. Every other task wrote every copy in the order sent: %s. Decisions out of the depot's order: %d."
                          layer (yes-no (g/every-task-last? facts n-keys))
                          (yes-no (g/every-task-as-decider? facts))
                          (yes-no (g/copies-in-order? facts))
                          (get-in facts [:walk :out-of-depot-order]))))))))))
