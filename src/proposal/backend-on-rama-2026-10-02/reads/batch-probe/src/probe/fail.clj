(ns probe.fail
  "The run for B2, B3 (i) and B3 (iii): the first attempt of one batch is made
  to fail late, and the rows are watched from outside across the failure and
  the second attempt.

  The module. One depot of things, each {:k key :v value}, partitioned by the
  key. One microbatch topology of three steps, each a batch block:

    1. starts from a constant, on task 0, as the store's step 2 does. It reads
       the clock, reads what this topology's own PState and a task global
       held before, writes both, and writes the note of the attempt to every
       task.
    2. starts from the things. On the task of each thing's key it reads the
       row that the key held, and writes the new row with the clock and the
       attempt of step 1.
    3. goes to every task. On task 3 it throws when the run asked for a
       failure. Step 2 is done on every task by then, so the rows of all four
       tasks were written before the throw.

  What the JVM keeps, beside Rama: how often each batch id began, which
  things each attempt got, and whether to fail. That is the memory of this
  process; it is how the run counts attempts, and it is one of the roads of
  B3 (iii).

  The watchers, per key (one key on each task): a foreign select read again
  and again, a query topology read again and again, and one open push.

  Run:  clojure -M:fail   (from this folder; the log is logs/fail.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [probe.util :as u])
  (:import [com.rpl.rama.integration TaskGlobalObject]))

;; ---- what the JVM keeps -------------------------------------------------

(defonce ^{:doc "Batch id → how many attempts of it began. Memory of this process."}
  attempts (atom {}))

(defonce ^{:doc "[batch id, attempt] → what step 1 found: {:t :own-before :memory-before :clock}."}
  begun (atom {}))

(defonce ^{:doc "[batch id, attempt] → the things that the attempt got, as [task key value], in the order of arrival."}
  things-got (atom {}))

(defonce ^{:doc "[batch id, attempt] pairs whose first line was said."}
  said (atom #{}))

(defonce ^{:doc "The run's wish: {:fail-times n :about-to-fail promise :go-on promise}."}
  plan (atom {:fail-times 0}))

;; ---- the task global: memory that a task holds --------------------------

(deftype Memory [^java.util.HashMap held]
  TaskGlobalObject
  (prepareForTask [this task-id context]
    (.clear held)
    (when (zero? task-id)
      (u/say "INSIDE  the task global of task 0 was made ready, and it is empty")))
  (close [this])
  java.io.Serializable)

(defn memory-read
  "Takes the task global. Gives what it holds, as a map."
  [^Memory memory]
  (into {} (.-held memory)))

(defn memory-write!
  "Takes the task global and a note. Puts the note in its one slot."
  [^Memory memory note]
  (.put ^java.util.HashMap (.-held memory) "note" note)
  nil)

;; ---- the functions that the topology calls ------------------------------

(defn attempt-begins!
  "Step 1 calls it once for an attempt, on task 0. Takes the batch id, what
  the topology's own PState and the task global held before this attempt
  wrote, and the clock that the attempt read. Counts the attempt. Gives its
  number, counted from 1."
  [batch own-before memory-before clock]
  (let [n (get (swap! attempts update batch (fnil inc 0)) batch)]
    (swap! begun assoc [batch n] {:t (u/ms) :own-before own-before
                                  :memory-before memory-before :clock clock})
    n))

(defn thing-arrives!
  "Step 2 calls it for each thing, on the task of its key. Takes the batch
  id, the task, the thing's key and value, the note that step 1 wrote on this
  task in this attempt, and the row that the key held in the topology's view
  before this write. Says what it saw. Gives the row to write."
  [batch task k v note row-before]
  (let [attempt (:attempt note)
        [before _] (swap-vals! said conj [batch attempt])]
    (when-not (contains? before [batch attempt])
      (let [{:keys [t own-before memory-before clock]} (get @begun [batch attempt])]
        (u/say "INSIDE  batch" batch "attempt" attempt "began at" (format "%.1f" t)
               "and read the clock" clock)
        (u/say "INSIDE    its own PState held before:" (pr-str own-before))
        (u/say "INSIDE    the task global held before:" (pr-str memory-before))))
    (swap! things-got update [batch attempt] (fnil conj []) [task k v])
    (u/say "INSIDE  batch" batch "attempt" attempt "task" task
           (str "(" (.getName (Thread/currentThread)) ")")
           "got" (pr-str [k v]) "· the row before, as the topology sees it:" (pr-str row-before))
    {:value v :clock (:clock note) :attempt attempt :batch batch}))

(defn late-point!
  "Step 3 calls it on every task, after step 2 is done on all of them. Takes
  the batch id and the task. On task 3, when the run asked for a failure and
  the attempt got things, it tells the run, waits up to 1.5 seconds for the
  run's word to go on, and throws."
  [batch task]
  (let [attempt (get @attempts batch)
        n (count (get @things-got [batch attempt]))]
    (when (and (= 3 task) (pos? n))
      (let [[old _] (swap-vals! plan update :fail-times #(max 0 (dec %)))]
        (when (pos? (:fail-times old))
          (u/say "INSIDE  batch" batch "attempt" attempt "task" task
                 ": the last step THROWS on purpose, after" n "rows were written")
          (when-let [p (:about-to-fail old)] (deliver p true))
          (when-let [p (:go-on old)] (deref p 1500 nil))
          (throw (ex-info "probe: forced failure of this attempt" {:batch batch :attempt attempt})))))
    nil))

;; ---- the module ----------------------------------------------------------

(defmodule FailProbe [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-object setup *memory (Memory. (java.util.HashMap.)))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$rows {String Object})
    (declare-pstate mb $$own {String Object})
    (<<sources mb
      (source> *things :> %things)

      ;; 1. from a constant, on task 0: the clock, and what was held before
      (<<batch
        (ops/current-microbatch-id :> *mbid)
        (str *mbid :> *batch)
        (local-select> (keypath "note") $$own :> *own-before)
        (memory-read *memory :> *memory-before)
        (System/currentTimeMillis :> *clock)
        (attempt-begins! *batch *own-before *memory-before *clock :> *attempt)
        (hash-map :batch *batch :attempt *attempt :clock *clock :> *note)
        (memory-write! *memory *note)
        (|all)
        (local-transform> [(keypath "note") (termval *note)] $$own))

      ;; 2. from the things: one row for each, on the task of its key
      (<<batch
        (%things :> {:keys [*k *v]})
        (ops/current-microbatch-id :> *mbid2)
        (str *mbid2 :> *batch2)
        (ops/current-task-id :> *task2)
        (local-select> (keypath "note") $$own :> *note2)
        (local-select> (keypath *k) $$rows :> *row-before)
        (thing-arrives! *batch2 *task2 *k *v *note2 *row-before :> *row)
        (local-transform> [(keypath *k) (termval *row)] $$rows))

      ;; 3. on every task, after step 2 is done everywhere: the forced failure
      (<<batch
        (|all)
        (ops/current-microbatch-id :> *mbid3)
        (str *mbid3 :> *batch3)
        (ops/current-task-id :> *task3)
        (late-point! *batch3 *task3))))

  (<<query-topology topologies "row" [*k :> *row]
    (|hash *k)
    (local-select> (keypath *k) $$rows :> *row)
    (|origin)))

;; ---- the run ---------------------------------------------------------------

(defn- batch-of-value
  "Takes a value. Gives the id of the batch whose attempts got a thing with
  that value, or nil."
  [v]
  (some (fn [[[batch _] got]] (when (some #(= v (nth % 2)) got) batch)) @things-got))

(defn- say-batch
  "Says, for the batch that carried a value, how many attempts began and
  whether each attempt got the same things on each task in the same order."
  [v]
  (let [batch (batch-of-value v)
        n (get @attempts batch)
        per-attempt (for [a (range 1 (inc n))]
                      (into (sorted-map) (group-by first (get @things-got [batch a]))))]
    (u/say "SUMMARY the batch that carried" (pr-str v) "has id" batch "and began" n "time(s)")
    (doseq [[a got] (map vector (range 1 (inc n)) per-attempt)]
      (u/say "SUMMARY   attempt" a "got, by task:" (pr-str (update-vals got #(mapv (fn [[_ k v]] [k v]) %)))))
    (u/say "SUMMARY   every attempt got the same things in the same order on each task:"
           (apply = per-attempt))))

(defn -main [& _]
  (u/open-log! "logs/fail.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc FailProbe {:tasks 4 :threads 4})
      (let [m      (get-module-name FailProbe)
            things (foreign-depot ipc m "*things")
            rows   (foreign-pstate ipc m "$$rows")
            row-q  (foreign-query ipc m "row")
            ks     (vec (rtest/gen-hashing-index-keys "key-" 4))
            stop   (atom false)
            seen   (atom [])
            value-of (fn [k] (:value (foreign-select-one (keypath k) rows)))
            all-hold? (fn [v] (every? #(= v (value-of %)) ks))
            batch! (fn [v before-resume]
                     (rtest/pause-microbatch-topology! ipc m "mb")
                     (doseq [k ks] (foreign-append! things {:k k :v v} :append-ack))
                     (before-resume)
                     (u/say "OUTSIDE four things with the value" (pr-str v) "are on the depot; the topology goes on")
                     (rtest/resume-microbatch-topology! ipc m "mb"))]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads. Keys, one for each task:" (pr-str ks))

        ;; batch A: no failure, so that every key holds a row
        (batch! "A" (fn []))
        (u/say "OUTSIDE all four rows give A:" (boolean (u/wait-until #(all-hold? "A") 20000)))
        (say-batch "A")

        ;; the watchers
        (doseq [[i k] (map-indexed vector ks)]
          (u/watch (str "SELECT  task " i " " k) #(foreign-select-one (keypath k) rows) stop seen 200000)
          (u/watch (str "QUERY   task " i " " k) #(foreign-invoke-query row-q k) stop seen 200000))
        (let [pushes (doall
                      (for [[i k] (map-indexed vector ks)]
                        (let [label (str "PUSH    task " i " " k)
                              p (u/within 20000
                                          #(foreign-proxy (keypath k) rows
                                                          {:callback-fn
                                                           (fn [new-value diff old-value]
                                                             (swap! seen conj {:t (u/ms) :label label :v new-value})
                                                             (u/say label "got" (.getSimpleName (class diff))
                                                                    "· new" (pr-str new-value)
                                                                    (if (= new-value old-value)
                                                                      "· THE SAME as the value before"
                                                                      (str "· before " (pr-str old-value)))))}))]
                          (u/say label "opened:" (if (or (= :timed-out p) (vector? p)) (pr-str p) (pr-str @p)))
                          p)))]
          (Thread/sleep 500)

          ;; batch B: the first attempt fails late; one more thing is appended while it runs
          (u/say "RUN     ---- batch B: the first attempt is made to fail ----")
          (let [about (promise) go-on (promise)]
            (batch! "B" #(reset! plan {:fail-times 1 :about-to-fail about :go-on go-on}))
            (if (deref about 20000 nil)
              (do (foreign-append! things {:k (ks 0) :v "extra"} :append-ack)
                  (u/say "OUTSIDE one more thing, value \"extra\" for task 0's key, was appended while attempt 1 still ran")
                  (deliver go-on true))
              (u/say "OUTSIDE the failure was never reached within 20 s")))
          (u/say "OUTSIDE task 0's key gives \"extra\":" (boolean (u/wait-until #(= "extra" (value-of (ks 0))) 30000)))
          (u/say "OUTSIDE the other three give B:" (boolean (u/wait-until #(every? (fn [k] (= "B" (value-of k))) (rest ks)) 30000)))
          (Thread/sleep 1500)
          (say-batch "B")
          (say-batch "extra")

          ;; batch C: two attempts fail, one after the other
          (u/say "RUN     ---- batch C: the first two attempts are made to fail ----")
          (batch! "C" #(reset! plan {:fail-times 2}))
          (u/say "OUTSIDE all four rows give C:" (boolean (u/wait-until #(all-hold? "C") 30000)))
          (Thread/sleep 1500)
          (say-batch "C")

          ;; a restart of the module's workers, as the in-process cluster does it
          (u/say "RUN     ---- the module is updated with itself: Rama's way to restart its workers in a test ----")
          (let [r (u/within 90000 #(do (rtest/update-module! ipc FailProbe) :updated))]
            (u/say "OUTSIDE update-module! gave" (pr-str r)))
          (batch! "D" (fn []))
          (u/say "OUTSIDE all four rows give D:" (boolean (u/wait-until #(all-hold? "D") 30000)))
          (Thread/sleep 1500)
          (say-batch "D")

          (reset! stop true)
          (doseq [p pushes] (when (instance? java.io.Closeable p) (try (.close ^java.io.Closeable p) (catch Throwable _ nil))))
          (u/say "RUN     batch ids that began:" (count @attempts) "· of them with things:"
                 (count (distinct (map first (keys @things-got)))))
          (u/say "RUN     END"))))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
