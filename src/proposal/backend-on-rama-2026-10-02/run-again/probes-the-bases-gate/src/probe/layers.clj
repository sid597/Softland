(ns probe.layers
  "Claim C4, run on Rama 1.6.0 in-process, 4 tasks.

  Six layers, 500 records each, a chain on each of 10 keys as in C1. They are
  sent from 3 threads at once, two layers to a thread, one record of each in
  turn. The six names are found so that two layers share depot partition 0,
  two share partition 3, and partitions 1 and 2 have one each. The two layers
  of one partition come from two threads.

  The questions: is every record accepted? Does every task hold the last row
  of every key of every layer? Does each layer's number run 1 to 500 with no
  gap, whatever other layer shares its task?

  State: the tables of `probe.gate/Gate`, and the log `logs/layers.log`. A
  probe, not a design."
  (:require [clojure.string :as str]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(def n-records "Records of each layer." 500)
(def n-keys "Keys of each layer." 10)

(defn report-layers!
  "Takes a label for the log, what `read-tables` gave (with `$$trace`) and the
  layers. Writes one line for each layer and one for each task. Gives whether
  every record was accepted in order and every task holds every last row."
  [label tables layers]
  (let [facts (into {} (for [l layers] [l (g/layer-facts tables l n-records n-keys)]))
        batches (count (distinct (for [[_ tb] tables [_ w] (tb "$$trace")] (:batch w))))]
    (doseq [l layers
            :let [f (facts l)
                  numbers (sort (map :n (vals (:answers f))))]]
      (say (format "  %s %s: depot partition %s, decided on task %s. Accepted %d of %d, refused %d. Its numbers are 1 to %d with no gap: %s, and in the depot's order: %s. `$$last`: %s."
                   label l (pr-str (:from f)) (pr-str (:deciders f)) (:accepted f) n-records (:refused f)
                   n-records (yes-no (= numbers (range 1 (inc n-records)))) (yes-no (:in-order? f))
                   (pr-str (:last f)))))
    (doseq [t (keys tables)
            :let [counts (apply merge-with + (for [l layers] (get-in facts [l :rows t])))]]
      (say (format "  %s task %d: keys of the six layers that hold the last row of their chain, with the deciding task's number: %d of %d. Another row: %d. No row: %d."
                   label t (get counts :last 0) (* n-keys (count layers))
                   (+ (get counts :not-last 0) (get counts :other 0)) (get counts :none 0))))
    (say (format "  %s microbatches: %d." label batches))
    (and (every? #(g/all-accepted? (facts %) n-records) layers)
         (every? #(g/every-task-last? (facts %) n-keys) layers))))

(defn -main
  "Runs the six layers and ends the JVM."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "layers")
   (fn []
     (say "Rama 1.6.0, in-process cluster. One module, 4 tasks, 2 task threads, 2 workers. No option of Rama is set.")
     (with-open [ipc (rtest/create-ipc)]
       (g/launch! ipc g/Gate 4)
       (let [m (g/open-module ipc g/Gate true)
             layers (g/six-layers m)]
         (say "The six layers, found by where their names land:" (str/join ", " layers))
         (g/send-six! m layers n-records n-keys)
         (g/settle! m)
         (let [ok? (report-layers! "C4" (g/read-tables m ["$$rows" "$$answers" "$$last" "$$trace"]) layers)]
           (say "C4 VERDICT: every record of every layer accepted, each layer numbered 1 to" n-records
                "in its depot order, and every task holds the last row of every key of every layer:" (yes-no ok?))))))))
