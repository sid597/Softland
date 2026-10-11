(ns probe.idle
  "The run for B6: how often a microbatch topology runs when its depot holds
  nothing new, and whether a batch block that does not start from the
  depot's things runs then.

  Three modules in one in-process cluster. Each has one depot and one
  microbatch topology of three batch blocks: the first starts from a
  constant on task 0 (as the store's step 2 does), the second starts from
  the things, the third starts from a constant and goes to every task. The
  three modules differ in one setting only:

    IdleDefault   sets nothing: what the in-process cluster gives
    IdleTwoSec    topology.microbatch.empty.sleep.time.millis = 2000
    IdleMinute    topology.microbatch.empty.sleep.time.millis = 60000

  The JVM keeps the time at which each run began. Nothing is written to a
  PState: the run asks only when Rama starts a run.

  Run:  clojure -M:idle   (from this folder; the log is logs/idle.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [probe.util :as u]))

(defonce ^{:doc "Module's label → the runs that began, each {:t ms :batch id :things [..] :tasks #{..}}. Memory of this process."}
  runs (atom {}))

(defn- on-last-run
  "Takes a label and a function. Applies the function to the newest run of
  that label."
  [label f]
  (swap! runs update label (fn [v] (if (seq v) (update v (dec (count v)) f) v))))

(defn run-begins!
  "The first block calls it, on task 0. Takes the module's label and the
  batch id. Keeps the time."
  [label batch]
  (swap! runs update label (fnil conj []) {:t (u/ms) :batch batch :things [] :tasks #{}})
  nil)

(defn thing-arrives!
  "The second block calls it for each thing. Takes the label and the thing's
  number. Adds the number, with the time, to the newest run."
  [label n]
  (on-last-run label #(update % :things conj [n (u/ms)]))
  nil)

(defn task-reached!
  "The third block calls it on every task. Takes the label and the task."
  [label task]
  (on-last-run label #(update % :tasks conj task))
  nil)

(defmodule IdleDefault [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (let [mb (microbatch-topology topologies "mb")]
    (<<sources mb
      (source> *things :> %things)
      (<<batch
        (ops/current-microbatch-id :> *mbid)
        (run-begins! "default" *mbid))
      (<<batch
        (%things :> {:keys [*n]})
        (thing-arrives! "default" *n))
      (<<batch
        (|all)
        (ops/current-task-id :> *task)
        (task-reached! "default" *task)))))

(defmodule IdleTwoSec [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (set-launch-topology-dynamic-option! setup "mb" "topology.microbatch.empty.sleep.time.millis" 2000)
  (let [mb (microbatch-topology topologies "mb")]
    (<<sources mb
      (source> *things :> %things)
      (<<batch
        (ops/current-microbatch-id :> *mbid)
        (run-begins! "two-sec" *mbid))
      (<<batch
        (%things :> {:keys [*n]})
        (thing-arrives! "two-sec" *n))
      (<<batch
        (|all)
        (ops/current-task-id :> *task)
        (task-reached! "two-sec" *task)))))

(defmodule IdleMinute [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (set-launch-topology-dynamic-option! setup "mb" "topology.microbatch.empty.sleep.time.millis" 60000)
  (let [mb (microbatch-topology topologies "mb")]
    (<<sources mb
      (source> *things :> %things)
      (<<batch
        (ops/current-microbatch-id :> *mbid)
        (run-begins! "minute" *mbid))
      (<<batch
        (%things :> {:keys [*n]})
        (thing-arrives! "minute" *n))
      (<<batch
        (|all)
        (ops/current-task-id :> *task)
        (task-reached! "minute" *task)))))

(defn- median [xs] (let [s (vec (sort xs))] (when (seq s) (s (quot (count s) 2)))))

(defn- report
  "Says what the runs of one label were: every run near a thing, and the
  gaps between two runs that both got nothing. Takes the label and the map
  from a thing's number to the time of its append call."
  [label appended]
  (let [rs (get @runs label)
        gaps (map (fn [a b] {:gap (- (:t b) (:t a)) :from a :to b}) rs (rest rs))
        idle-gaps (keep #(when (and (empty? (:things (:from %))) (empty? (:things (:to %)))) (:gap %)) gaps)]
    (u/say "REPORT " label ":" (count rs) "runs began · batch ids" (:batch (first rs)) "to" (:batch (last rs))
           "· the block that goes to every task reached, in each run, the tasks" (pr-str (frequencies (map :tasks rs))))
    (when (seq idle-gaps)
      (u/say "REPORT " label ": between two runs that both got nothing —" (count idle-gaps) "gaps, least"
             (format "%.0f" (apply min idle-gaps)) "ms, middle" (format "%.0f" (median idle-gaps))
             "ms, most" (format "%.0f" (apply max idle-gaps)) "ms"))
    (doseq [[i r] (map-indexed vector rs)
            :when (or (< i 6) (seq (:things r)) (seq (:things (get rs (dec i)))) (>= i (- (count rs) 3)))]
      (let [prev (get rs (dec i))]
        (u/say "REPORT " label ": run" i "batch" (:batch r) "began at" (format "%.0f" (:t r))
               (if prev (str "· " (format "%.0f" (- (:t r) (:t prev))) " ms after the run before") "")
               (if (seq (:things r))
                 (str "· got things " (pr-str (mapv first (:things r)))
                      ", the first " (format "%.0f" (- (:t r) (get appended (ffirst (:things r))))) " ms after its append call")
                 "· got nothing"))))))

(defn -main [& _]
  (u/open-log! "logs/idle.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc IdleDefault {:tasks 4 :threads 2})
      (rtest/launch-module! ipc IdleTwoSec {:tasks 4 :threads 2})
      (rtest/launch-module! ipc IdleMinute {:tasks 4 :threads 2})
      (let [depots {"default" (foreign-depot ipc (get-module-name IdleDefault) "*things")
                    "two-sec" (foreign-depot ipc (get-module-name IdleTwoSec) "*things")
                    "minute"  (foreign-depot ipc (get-module-name IdleMinute) "*things")}
            appended (atom {})
            number (atom 0)
            append! (fn []
                      (doseq [[label depot] depots]
                        (let [n (swap! number inc)]
                          (swap! appended assoc n (u/ms))
                          (foreign-append! depot {:k "k" :n n} :append-ack)
                          (u/say "OUTSIDE thing" n "appended to" label))))]
        (u/say "RUN     Rama 1.6.0, in-process, three modules, each 4 tasks and 2 threads. Nothing is appended for 5 s.")
        (Thread/sleep 5000)
        (append!)
        (Thread/sleep 3130)
        (append!)
        (Thread/sleep 3180)
        (append!)
        (Thread/sleep 3160)
        (append!) (Thread/sleep 50) (append!)
        (u/say "RUN     nothing more is appended. The run watches for 66 s more, for the module set to one minute.")
        (Thread/sleep 66000)
        (doseq [label ["default" "two-sec" "minute"]] (report label @appended))
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
