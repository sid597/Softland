(ns probe.yield
  "Claim C2, the part about a yield in the deciding walk, looked at closer.
  Run on Rama 1.6.0 in-process, 4 tasks.

  `probe.order` found that a layer whose walk calls `(yield-if-overtime)` and
  is slow enough to be over the 5 ms had records refused. This probe runs
  that again and writes the order in which the deciding task decided the
  records, as runs of records in the depot's order. In every layer here the
  deciding task skips its own copy, so a refused record is not the work of a
  copy that came back.

    y1-slow-yield       1,000 records, one microbatch, 200 microseconds burnt
                        on every record before `(yield-if-overtime)`
    y2-slow             the same with no yield
    y3-yield            the same with the yield and nothing burnt
    y4-slow-yield-live  3,000 records appended while the gate runs, so the
                        batches are short; burnt and yielding
    y5-slow-yield       3,000 records appended while the gate is paused;
                        burnt and yielding

  State: the tables of `probe.gate/Gate`, and the log `logs/yield.log`. A
  probe, not a design."
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(def layers
  "The layers in the order in which they are run: name, records, keys, how."
  [["y1-slow-yield" 1000 50 :paused]
   ["y2-slow" 1000 50 :paused]
   ["y3-yield" 1000 50 :paused]
   ["y4-slow-yield-live" 3000 50 :live]
   ["y5-slow-yield" 3000 50 :paused]])

(defn -main
  "Runs the layers and ends the JVM."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "yield")
   (fn []
     (say "Rama 1.6.0, in-process cluster. One module, 4 tasks, 2 task threads, 2 workers. No option of Rama is set.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc g/Gate 4)
       (let [m (g/open-module ipc g/Gate true)]
         (doseq [[layer n-records n-keys how] layers]
           (let [facts (g/chain-scenario! m "gate" layer n-records n-keys how)]
             (say (format "  C2 YIELD VERDICT (%s): accepted %d of %d. Numbered 1 to %d in the depot's order: %s. Decisions out of the depot's order: %d. Every task holds the deciding task's row for every key: %s. Every other task wrote every copy in the order sent: %s."
                          layer (:accepted facts) n-records n-records (yes-no (:in-order? facts))
                          (get-in facts [:walk :out-of-depot-order])
                          (yes-no (g/every-task-as-decider? facts))
                          (yes-no (g/copies-in-order? facts)))))))))))
