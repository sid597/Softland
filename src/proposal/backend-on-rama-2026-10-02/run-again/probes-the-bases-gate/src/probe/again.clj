(ns probe.again
  "Claim C6, run on Rama 1.6.0 in-process: the run again, with the same number
  of tasks.

  `probe.gate/Gate` owns the depot, has 4 tasks, and decides the records as
  they arrive: C1's (one layer, 3,000 records, a chain on each of 50 keys) and
  then C4's (six layers of 500, sent from 3 threads). When all of it is
  decided, `probe.gate/Again` is launched with 4 tasks: it mirrors the depot
  and its gate, the same code, starts from the depot's beginning.

  The question: is every table on every task of the second module equal to
  the first module's? And in how many microbatches did each decide?

  State: the two modules' tables, and the log `logs/again.log`. A probe, not
  a design."
  (:require [clojure.string :as str]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(def read-names "The tables that are read." ["$$rows" "$$answers" "$$last" "$$trace"])

(defn feed!
  "Takes the opened module that owns the depot. Sends C1's records and then
  C4's, waits for the gate. Gives the layers with their numbers of records
  and keys."
  [m]
  (let [six (g/six-layers m)]
    (g/send! m (g/chain "c1" 3000 50))
    (g/send-six! m six 500 10)
    (g/settle! m)
    (into [["c1" 3000 50]] (for [l six] [l 500 10]))))

(defn report-module!
  "Takes a label, what `read-tables` gave, and the layers with their numbers.
  Writes one line for each layer and one for the microbatches. Gives whether
  every record of every layer was accepted in its depot order."
  [label tables layers]
  (let [facts (for [[l n k] layers] [l n (g/layer-facts tables l n k)])
        {:keys [microbatches most-in-one]} (g/microbatches tables)]
    (doseq [[l n f] facts]
      (say (format "  %s, layer %s: emitted on task %s, decided on task %s. Accepted %d of %d, refused %d. Numbered 1 to %d in the depot's order: %s."
                   label l (pr-str (:from f)) (pr-str (:deciders f)) (:accepted f) n (:refused f) n (yes-no (:in-order? f)))))
    (say (format "  %s: %d microbatches, at most %d records in one." label microbatches most-in-one))
    (every? (fn [[_ n f]] (g/all-accepted? f n)) facts)))

(defn -main
  "Runs the first module and the run again, and ends the JVM."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "again")
   (fn []
     (say "Rama 1.6.0, in-process cluster. Two modules, 4 tasks each, 2 task threads, 2 workers. No option of Rama is set.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc g/Gate 4)
       (let [first-module (g/open-module ipc g/Gate true)
             layers (feed! first-module)
             first-tables (g/read-tables first-module read-names)]
         (say "The layers:" (str/join ", " (map first layers)) ". Records in the depot:" @(:sent first-module))
         (say "")
         (say "THE FIRST RUN (the module that owns the depot)")
         (let [first-ok? (report-module! "first" first-tables layers)]
           (g/launch! ipc g/Again 4)
           (let [again (g/open-module ipc g/Again false)
                 _ (g/settle! again @(:sent first-module))
                 again-tables (g/read-tables again read-names)]
             (say "")
             (say "THE RUN AGAIN (a second module, the same gate over a mirror of the depot, from its beginning)")
             (let [again-ok? (report-module! "again" again-tables layers)
                   diffs (for [t (keys first-tables) n g/gate-tables]
                           [t n (count (get-in first-tables [t n])) (count (get-in again-tables [t n]))
                            (g/differences (get-in first-tables [t n]) (get-in again-tables [t n]))])]
               (say "  A record of depot partition p is emitted in the second module on task:" (pr-str (g/source-tasks first-tables again-tables)))
               (say "")
               (say "THE TWO, HELD AGAINST EACH OTHER, TABLE BY TABLE ON EVERY TASK")
               (doseq [[t n in-first in-again differ] diffs]
                 (say (format "  Task %d, %s: %d entries in the first, %d in the second, %d that differ." t n in-first in-again differ)))
               (say "C6 VERDICT: every table on every task of the second module equal to the first module's:"
                    (yes-no (every? (fn [[_ _ _ _ differ]] (zero? differ)) diffs))
                    ". Every record accepted in its depot order in the first:" (yes-no first-ok?)
                    ", in the second:" (yes-no again-ok?) ".")))))))))
