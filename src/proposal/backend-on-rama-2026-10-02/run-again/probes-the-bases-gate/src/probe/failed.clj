(ns probe.failed
  "Claim C5, run on Rama 1.6.0 in-process, 4 tasks: a failed attempt.

  Two modules run the one gate over the same 1,500 records of one layer (a
  chain on each of 50 keys), each on a depot of its own: `probe.gate/Gate`,
  whose gate throws once, and `probe.gate/Twin`, which never throws.

  The records go in three parts, to both:
    part A   records 0..499, appended while the gates run, and waited for.
    part B   records 500..1099, appended while both gates are paused, so that
             they are ONE microbatch of 600.
    part C   records 1100..1499, appended while the gates run.

  Where the throw stands. Always in `Gate`, at record 900, the 401st record
  of part B's batch. Four cases, each on a cluster of its own:

    :walk   on the deciding task, at the head of the walk of record 900,
            before step 1 (so before any table is read for that record). By
            then 400 records of the batch are decided on that task and the
            walk has passed their `|all`.
    :back   on the deciding task, when the copy of record 900 comes back to
            it through `|all`, at the place where the skip stands.
    :copy-near  on another task that runs on the deciding task's thread, in
            the same worker, when the copy of record 900 arrives there, before
            that task writes it.
    :copy-far   the same on a task of the other worker, so that the deciding
            task's worker is not the one that the throw hits.

  The throwing function first sleeps 300 ms on its task's thread, so that the
  copies already sent can be written on the other tasks, then throws an
  `ex-info`. It throws once.

  How the probe tells the attempts apart: the gate writes, wherever it writes
  a row, an entry [attempt id] in the probe's table `$$attempts`. The attempt
  is 1 until the throw and 2 after it; it is read on the deciding task and
  carried to the copies. An entry [1 id] of a record of part B that is still
  there at the end is a row of the failed attempt that was left.

  The questions: after the retry, is every table on every task equal to the
  module that never failed? Is any row of the failed attempt left on any
  task? What did Rama log, and did a worker restart? The alias `:failed`
  names `log4j2-info.xml`, so the run's stdout file holds what Rama logged at
  INFO and above, each line behind the word RAMA.

  State: the two modules' tables, the journal, and the log `logs/failed.log`.
  A probe, not a design."
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g :refer [say yes-no]]))

(def layer "The one layer." "c5")
(def n-keys "Its keys." 50)
(def part-a "Records of part A." 500)
(def part-b "Records of part B, the one batch." 600)
(def part-c "Records of part C." 400)
(def throw-index "The place in the layer of the record at which the gate throws." 900)

(def places
  "The cases, with the words for the log."
  [[:walk "on the deciding task, at the head of the walk of record 900, before step 1"]
   [:back "on the deciding task, when the copy of record 900 comes back to it through |all"]
   [:copy-near "on another task of the deciding task's thread and worker, when the copy of record 900 arrives there, before it is written"]
   [:copy-far "on a task of the other worker, when the copy of record 900 arrives there, before it is written"]])

(defn- in-b?
  "Takes a record's id. Says whether it is of part B."
  [id]
  (let [i (g/index-of id)]
    (and (>= i part-a) (< i (+ part-a part-b)))))

(defn- run-case!
  "Takes where the throw stands and the words for it. Runs the two modules on
  a cluster of its own and writes what was seen. Gives whether the tables
  were equal at the end and no row of the failed attempt was left."
  [where words]
  (reset! g/journal [])
  (reset! g/throw-at {})
  (reset! g/claimed #{})
  (reset! g/thrown {})
  (say "")
  (say (str "CASE " where ": THE THROW STANDS " words))
  (with-open [ipc (rtest/create-ipc)]
    (g/launch! ipc g/Gate 4)
    (g/launch! ipc g/Twin 4)
    (let [gate (g/open-module ipc g/Gate true)
          twin (g/open-module ipc g/Twin true)
          records (g/chain layer (+ part-a part-b part-c) n-keys)
          a (subvec records 0 part-a)
          b (subvec records part-a (+ part-a part-b))
          c (subvec records (+ part-a part-b))
          both! (fn [f] (f gate) (f twin))
          _ (both! #(g/send! % a))
          _ (both! g/settle!)
          _ (both! g/pause!)
          _ (both! #(g/send! % b))
          threads (g/threads-by-task (filter #(= "gate" (first %)) @g/journal))
          decider (first (for [[_ kind task] @g/journal :when (= kind :accept)] task))
          near (first (for [[t names] threads :when (and (not= t decider) (= names (threads decider)))] t))
          far (first (for [[t names] threads :when (not= names (threads decider))] t))
          _ (say (format "  Before the throw: the deciding task is %s. On its thread: task %s. In the other worker: task %s." (pr-str decider) (pr-str near) (pr-str far)))
          _ (reset! g/throw-at {"gate" (case where
                                         :copy-near {:id (g/id-of layer throw-index) :where :copy :task near}
                                         :copy-far {:id (g/id-of layer throw-index) :where :copy :task far}
                                         {:id (g/id-of layer throw-index) :where where})})
          released (System/nanoTime)
          _ (g/resume! twin)
          _ (g/resume! gate)
          _ (g/settle! twin)
          twin-ms (/ (- (System/nanoTime) released) 1e6)
          _ (g/settle! gate)
          gate-ms (/ (- (System/nanoTime) released) 1e6)
          _ (both! #(g/send! % c))
          _ (both! g/settle!)
          names ["$$rows" "$$answers" "$$last" "$$trace" "$$attempts"]
          gate-tables (g/read-tables gate names)
          twin-tables (g/read-tables twin names)
          entries (filterv #(= "gate" (first %)) @g/journal)
          thrown-at (get @g/thrown "gate")
          throw-entry (first (filter #(= :throw (second %)) entries))
          b-entries (filter #(and (in-b? (nth % 3)) (not= :throw (second %))) entries)
          first-try (filter #(= 1 (nth % 4)) b-entries)
          retry (filter #(= 2 (nth % 4)) b-entries)
          decided? #(#{:accept :refuse} (second %))
          copy? #(= :copy (second %))
          by-task (fn [es] (into (sorted-map) (frequencies (map #(nth % 2) es))))
          late (filter #(and thrown-at (> (nth % 6) thrown-at)) first-try)
          gate-facts (g/layer-facts gate-tables layer (count records) n-keys)
          twin-facts (g/layer-facts twin-tables layer (count records) n-keys)
          b-batches (fn [tables] (vec (sort (distinct (for [[id w] (g/union tables "$$trace") :when (in-b? id)] (:batch w))))))
          diffs (for [t (keys gate-tables) n g/gate-tables]
                  [t n (count (get-in gate-tables [t n])) (count (get-in twin-tables [t n]))
                   (g/differences (get-in gate-tables [t n]) (get-in twin-tables [t n]))])
          marks (fn [attempt]
                  (into (sorted-map)
                        (for [[t tb] gate-tables]
                          [t (count (filter (fn [[[at id] _]] (and (= attempt at) (in-b? id))) (tb "$$attempts")))])))
          left (marks 1)
          threads-before (g/threads-by-task first-try)
          threads-after (select-keys (g/threads-by-task retry) (keys threads-before))
          equal? (every? (fn [[_ _ _ _ differ]] (zero? differ)) diffs)]
      (say (format "  The gate threw: %s. At record %s, on task %s (the layer's deciding task is %s), thread %s."
                   (yes-no (some? throw-entry)) (pr-str (nth throw-entry 3 nil)) (pr-str (nth throw-entry 2 nil))
                   (pr-str (:deciders gate-facts)) (pr-str (nth throw-entry 5 nil))))
      (say (format "  Part B's records are in microbatch %s of the module that threw, and in microbatch %s of the one that did not."
                   (pr-str (b-batches gate-tables)) (pr-str (b-batches twin-tables))))
      (say "  THE FAILED ATTEMPT, by the probe's journal (entries of part B's records that carry attempt 1):")
      (say "    Decisions, by task:" (pr-str (by-task (filter decided? first-try)))
           ". Copies written, by task:" (pr-str (by-task (filter copy? first-try))) ".")
      (say "    Of these, written after the throw:" (count late) (pr-str (by-task late)) ".")
      (say "  THE RETRY (entries of part B's records that carry attempt 2):")
      (say "    Decisions, by task:" (pr-str (by-task (filter decided? retry)))
           ". Copies written, by task:" (pr-str (by-task (filter copy? retry))) ".")
      (say "    Records that the gate found already answered (step 1) in the whole run:" (count (filter #(= :repeat (second %)) entries)) ".")
      (when (and thrown-at (seq retry))
        (say (format "    From the throw to the retry's first decision: %.0f ms." (/ (- (apply min (map #(nth % 6) retry)) thrown-at) 1e6))))
      (say (format "    From the release of the batch to its end: %.0f ms in the module that threw, %.0f ms in the one that did not." gate-ms twin-ms))
      (say "    Thread names by task in the failed attempt:" (pr-str threads-before))
      (say "    The same tasks ran under the same thread names in the retry:" (yes-no (= threads-before threads-after)))
      (say "  AT THE END:")
      (doseq [[label f] [["The module that threw" gate-facts] ["The module that did not" twin-facts]]]
        (say (format "    %s: decided on task %s. Accepted %d of %d, refused %d. Numbered 1 to %d in the depot's order: %s. Microbatches: %d. Every task holds the last row of every key: %s."
                     label (pr-str (:deciders f)) (:accepted f) (count records) (:refused f) (count records)
                     (yes-no (:in-order? f)) (:microbatches f) (yes-no (g/every-task-last? f n-keys)))))
      (doseq [[t n in-gate in-twin differ] diffs]
        (say (format "    Task %d, %s: %d entries in the module that threw, %d in the one that did not, %d that differ." t n in-gate in-twin differ)))
      (say "    Entries [1 id] of part B in `$$attempts` at the end (rows of the failed attempt), by task:" (pr-str left))
      (say "    Entries [2 id] of part B in `$$attempts` at the end (rows of the retry), by task:" (pr-str (marks 2)))
      (say (str "C5 VERDICT (" where "):") "the gate threw once:" (yes-no (some? throw-entry))
           ". Decisions and copies that the failed attempt had made before the throw, all tasks:" (count (remove (set late) first-try))
           ". After the retry every table on every task equal to the module that did not fail:" (yes-no equal?)
           ". Rows of the failed attempt left on any task:" (reduce + (vals left)) ".")
      (and equal? (zero? (reduce + (vals left)))))))

(defn -main
  "Runs the cases and ends the JVM."
  [& [trial-log]]
  (g/run-probe!
   (or trial-log "failed")
   (fn []
     (say "Rama 1.6.0, in-process clusters, one for each case. Two modules, 4 tasks each, 2 task threads, 2 workers. No option of Rama is set.")
     (let [results (vec (for [[where words] places] [where (run-case! where words)]))]
       (say "")
       (say "C5 VERDICT, ALL CASES: tables equal at the end and no row of the failed attempt left:" (pr-str results))))))
