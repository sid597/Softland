(ns probe.own-copy
  "Claim C3, run on Rama 1.6.0 in-process, 4 tasks.

  The gate of C1 and C2 WITHOUT the skip of step 4: the copies that `|all`
  brings back to the deciding task are written there too. A copy is the row
  of the record it was sent for. If one comes back after a later record of
  the same key was decided, it sets the key back to an older row, until the
  later record's own copy comes back. A record decided in that time is
  refused, because the row it replaces is not the row it finds.

  The question: does any record get refused, or any key end on an older row
  on the deciding task? Fourteen layers, one after the other, in one module
  (`probe.gate/Gate`). A layer's name with \"noskip\" in it turns the skip
  off:

    c3-noskip-live-r1..r3     3,000 records appended while the gate runs
    c3-noskip-paused-r1..r3   3,000 appended while the gate is paused
    c3-noskip-yield-r1..r3    as paused, and the walk calls `(yield-if-overtime)`
    c3-noskip-slow-yield-r1..r3  as yield, and the walk keeps the thread busy
                              for 200 microseconds on every record, so that it
                              is sure to be over the 5 ms after which that yields
    c3-noskip-long            20,000, appended while the gate is paused
    c3-noskip-long-yield      as long, and the walk calls `(yield-if-overtime)`

  What the journal adds: how often one of the deciding task's own copies set
  a key back to an older row for a time, and how many decisions were made
  while a key did not stand on the record before.

  State: the module's tables, and the log `logs/own-copy.log`. A probe, not a
  design."
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(def layers
  "The layers in the order in which they are run: name, records, keys, how."
  (vec (concat
        (for [r [1 2 3]] [(str "c3-noskip-live-r" r) 3000 50 :live])
        (for [r [1 2 3]] [(str "c3-noskip-paused-r" r) 3000 50 :paused])
        (for [r [1 2 3]] [(str "c3-noskip-yield-r" r) 3000 50 :paused])
        (for [r [1 2 3]] [(str "c3-noskip-slow-yield-r" r) 3000 50 :paused])
        [["c3-noskip-long" 20000 50 :paused]
         ["c3-noskip-long-yield" 20000 50 :paused]])))

(def trial-layers
  "Small layers, for a trial run that only shows that the probe runs."
  [["c3-noskip-live-r1" 300 50 :live]
   ["c3-noskip-slow-yield-r1" 300 50 :paused]])

(defn -main
  "Runs the layers and ends the JVM. With one argument, the path of a log
  file, it runs the small trial layers instead and writes there."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "own-copy")
   (fn []
     (say "Rama 1.6.0, in-process cluster. One module, 4 tasks, 2 task threads, 2 workers. No option of Rama is set.")
     (say "In every layer here the deciding task also writes the copies that `|all` brings back to it.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc g/Gate 4)
       (let [m (g/open-module ipc g/Gate true)
             seen (vec
                   (for [[layer n-records n-keys how] (if trial-log trial-layers layers)]
                     (let [facts (g/chain-scenario! m "gate" layer n-records n-keys how)
                           decider (first (:deciders facts))
                           not-last (get-in facts [:rows decider :not-last] 0)
                           other (reduce + (map #(get % :other 0) (vals (:rows facts))))]
                       (say (format "  C3 (%s): refused: %d of %d. Keys that do not end on the last row of their chain on the deciding task: %d. Keys on which a task ends on a row other than the deciding task's, all tasks together: %d. Decisions out of the depot's order: %d. Own copies that set a key back for a time: %d. Decisions made on an older row: %d."
                                    layer (:refused facts) n-records not-last other
                                    (get-in facts [:walk :out-of-depot-order])
                                    (get-in facts [:walk :set-back]) (get-in facts [:walk :decided-on-older])))
                       {:layer layer
                        :wrong? (or (pos? (:refused facts)) (pos? not-last) (pos? other)
                                    (not (g/all-accepted? facts n-records)))
                        :out-of-order? (pos? (get-in facts [:walk :out-of-depot-order]))
                        :set-back? (pos? (get-in facts [:walk :set-back]))
                        :between? (> (get-in facts [:walk :stretches]) (:microbatches facts))})))
             in-order (remove :out-of-order? seen)
             out-of-order (filter :out-of-order? seen)
             names #(if (seq %) (str ": " (pr-str (mapv :layer %)) ".") ".")]
         (say "")
         (say (format "C3 VERDICT: runs in which the deciding task decided every record in the depot's order: %d of %d. Among them, a refused record, or a key that does not end on the last row of its chain, was seen in %d%s"
                      (count in-order) (count seen) (count (filter :wrong? in-order)) (names (filter :wrong? in-order))))
         (say (format "C3 VERDICT: runs with decisions out of the depot's order (a yield that yielded): %d of %d%s Among them a refused record was seen in %d. `probe.yield` shows the same with the skip on."
                      (count out-of-order) (count seen) (names out-of-order) (count (filter :wrong? out-of-order))))
         (say (format "C3 VERDICT: an own copy that set a key back to an older row for a time was seen in %d of %d runs. An own copy that came back between two decisions of one microbatch was seen in %d of %d runs%s"
                      (count (filter :set-back? seen)) (count seen)
                      (count (filter :between? seen)) (count seen) (names (filter :between? seen)))))))))
