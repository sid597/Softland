(ns probe.long-batch
  "Claims C2 and C3 with a batch longer than 1,000 records. Run on Rama 1.6.0
  in-process, 4 tasks.

  Rama reads at most `depot.microbatch.max.records` records of a depot
  partition into one microbatch; `probe.order` saw 1,000 at most. The module
  here, `LongBatch`, is the gate of `probe.gate` with that option of its depot set
  to 20,000 at launch (`set-launch-depot-dynamic-option!`). Nothing else is
  set. Each layer is 20,000 records, a chain on each of 50 keys, appended
  while the gate is paused, so the whole layer is one microbatch.

    l1-long               the deciding task skips its own copy
    l2-long-yield         the same, and the walk calls `(yield-if-overtime)`,
                          with nothing burnt
    l3-noskip-long        the deciding task writes its own copies too
    l4-noskip-long-r2     the same again

  State: the module's tables, and the log `logs/long-batch.log`. A probe, not
  a design."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(defmodule LongBatch [setup topologies]
  (declare-depot setup *acts (hash-by :layer))
  (set-launch-depot-dynamic-option! setup *acts "depot.microbatch.max.records" 20000)
  (g/add-ping setup topologies)
  (g/add-gate topologies "long" false))

(def layers
  "The layers in the order in which they are run: name, records, keys, how."
  [["l1-long" 20000 50 :paused]
   ["l2-long-yield" 20000 50 :paused]
   ["l3-noskip-long" 20000 50 :paused]
   ["l4-noskip-long-r2" 20000 50 :paused]])

(defn -main
  "Runs the layers and ends the JVM."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "long-batch")
   (fn []
     (say "Rama 1.6.0, in-process cluster. One module, 4 tasks, 2 task threads, 2 workers. One option of Rama is set: depot.microbatch.max.records = 20000 on the depot.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc LongBatch 4)
       (let [m (g/open-module ipc LongBatch true)]
         (doseq [[layer n-records n-keys how] layers]
           (let [facts (g/chain-scenario! m "long" layer n-records n-keys how)
                 decider (first (:deciders facts))]
             (say (format "  LONG BATCH VERDICT (%s): %d of %d accepted, in %d microbatches, at most %d in one. Decisions out of the depot's order: %d. Every task holds the last row of every key's chain: %s. Every other task wrote every copy in the order sent: %s. Own copies that set a key back for a time: %d. Decisions made on an older row: %d. Stretches: %d."
                          layer (:accepted facts) n-records (:microbatches facts) (:most-in-one facts)
                          (get-in facts [:walk :out-of-depot-order])
                          (yes-no (g/every-task-last? facts n-keys))
                          (yes-no (g/copies-in-order? facts))
                          (get-in facts [:walk :set-back]) (get-in facts [:walk :decided-on-older])
                          (get-in facts [:walk :stretches]))))))))))
