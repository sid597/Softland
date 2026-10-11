(ns probe.commit
  "The run for B7: the window that the store's page names, in which one task
  has shown its part of a batch and another has not, and what Rama does when
  the batch is tried again from there.

  No code of a topology runs while Rama commits, so a throw in the
  microbatch topology cannot fail a commit. This run comes at it from the
  side: it HOLDS the thread of one task (task 3) while the batch is still
  being processed elsewhere, and holds it past the end of the processing.
  The other tasks can then commit and task 3 cannot. From there the attempt
  is made to fail in two ways:

    by the phase timeout   the topology's phase timeout is set low, and the
                           hold is longer than it.
    by a throw             a stream topology of the module, `crasher`, holds
                           task 3's thread and then throws on it. Rama
                           treats a throw on a task's thread as fatal for
                           the worker and starts the worker again
                           (`probe.fail` showed it).

  The module. Each task has a thread of its own (4 tasks, 4 threads). The
  depot of things, and the depot of clocks with its keeper, as in
  `probe.keep`. The depot `*crash` and the stream topology `crasher`. The
  microbatch topology `mb` of three batch blocks:

    1. from a constant, on task 0: the wall clock, or the kept clock when the
       run chose that road; the note of the attempt to every task.
    2. from the things: one row for each, on the task of its key.
    3. from a constant, to task 1 ONLY: when the run asked for a hold, it
       asks for the hold of task 3's thread, waits until the hold has begun,
       stays 150 ms more, and ends. Task 3 has no part in this block.

  The query topology `hold` goes to the task it is given and sleeps there.

  The watchers, per key (one key on each task): a foreign select read again
  and again, and one open push.

  Run:  clojure -M:commit            (the log is logs/commit.log)
        clojure -M:commit control    (the same hold with no phase timeout
                                      set; the log is logs/commit-control.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [probe.util :as u]))

(def phase-timeout-seconds
  "What the module sets `topology.microbatch.phase.timeout.seconds` to when
  it is launched, or nil to set nothing."
  2)

;; ---- what the JVM keeps -------------------------------------------------

(defonce ^{:doc "The road of step 1: :none, or :read-first (the kept clock, as in probe.keep)."}
  road (atom :none))

(defonce ^{:doc "True when a row is written without the attempt's number, so that two attempts with one clock write the same row."}
  plain-rows (atom false))

(defonce ^{:doc "Batch id → how many attempts of it began."}
  attempts (atom {}))

(defonce ^{:doc "True while each attempt's beginning is said, the ones that get nothing too."}
  loud (atom false))

(defonce ^{:doc "The run's wish for the next batch with things: {:hold-ms n :crash? bool :hold-began promise}."}
  plan (atom {}))

(defonce ^{:doc "[batch id, attempt] → how many things it got."}
  things-count (atom {}))

(defonce ^{:doc "What step 3 needs to ask for a hold: {:hold foreign query, :crash foreign depot, :crash-key key of task 3}. Set by the run once the module is up."}
  handles (atom {}))

(defonce ^{:doc "True from the moment a throw was asked for until `crasher` took the wish. A record of `*crash` that Rama gives to `crasher` again finds it false."}
  crash-armed (atom false))

;; ---- the functions that the topologies call -----------------------------

(defn road-now "Gives the road that the run chose for now." [] @road)

(defn attempt-begins!
  "Step 1 calls it once for an attempt. Takes the batch id, the wall clock
  that this attempt read, and the clock that it will write. Counts the
  attempt, says it when the run is loud, and gives its number."
  [batch wall clock]
  (let [n (get (swap! attempts update batch (fnil inc 0)) batch)]
    (when @loud
      (u/say "INSIDE  batch" batch "attempt" n "begins · it read the wall clock" wall "· it will write the clock" clock))
    n))

(defn thing-arrives!
  "Step 2 calls it for each thing. Takes the batch id, the task, the thing's
  key and value, and the note of step 1. Says it. Gives the row to write:
  value, clock and batch id, and the attempt's number unless the run asked
  for plain rows."
  [batch task k v note]
  (swap! things-count update [batch (:attempt note)] (fnil inc 0))
  (when @loud
    (u/say "INSIDE  batch" batch "attempt" (:attempt note) "task" task "writes the row of" (pr-str [k v])))
  (cond-> {:value v :clock (:clock note) :batch batch}
    (not @plain-rows) (assoc :attempt (:attempt note))))

(defn hold!
  "The query topology `hold` calls it on the task that it was sent to. Takes
  the task and the milliseconds. Sleeps on the task's thread: nothing else
  of that task runs meanwhile. Gives the milliseconds."
  [task hold-ms]
  (u/say "INSIDE  task" task ": its thread is HELD for" hold-ms "ms from now")
  (when-let [p (:hold-began @plan)] (deliver p true))
  (Thread/sleep (long hold-ms))
  (u/say "INSIDE  task" task ": its thread is free again")
  hold-ms)

(defn crash!
  "The stream topology `crasher` calls it for each record of `*crash`, on
  task 3. Takes the milliseconds. When a throw was asked for, it sleeps on
  the task's thread for that long and then throws. A record that comes a
  second time does nothing."
  [hold-ms]
  (let [[armed _] (swap-vals! crash-armed (constantly false))]
    (when armed
      (u/say "INSIDE  task 3 : its thread is HELD for" hold-ms "ms from now, and then `crasher` throws on it")
      (when-let [p (:hold-began @plan)] (deliver p true))
      (Thread/sleep (long hold-ms))
      (u/say "INSIDE  task 3 : `crasher` THROWS now")
      (throw (ex-info "probe: a throw on task 3 while the batch commits" {})))
    nil))

(defn tail!
  "Step 3 calls it, on task 1. Takes the batch id. When the run asked for a
  hold and this attempt got things, it asks for the hold of task 3 without
  waiting for its end, waits up to 2 s until the hold has begun, and stays
  150 ms more. The wish is used up by the first attempt that takes it."
  [batch]
  (let [attempt (get @attempts batch)
        n (get @things-count [batch attempt] 0)]
    (when (pos? n)
      (let [[old _] (swap-vals! plan dissoc :hold-ms)]
        (when-let [hold-ms (:hold-ms old)]
          (u/say "INSIDE  batch" batch "attempt" attempt ": step 3, on task 1, asks for the hold of task 3;"
                 "steps 1 and 2 are done on every task")
          (if (:crash? old)
            (do (reset! crash-armed true)
                (foreign-append-async! (:crash @handles) {:k (:crash-key @handles) :crash-hold-ms hold-ms} nil))
            (foreign-invoke-query-async (:hold @handles) 3 hold-ms))
          (deref (:hold-began old) 2000 nil)
          (Thread/sleep 150)
          (u/say "INSIDE  batch" batch "attempt" attempt ": step 3 ends; the attempt has no more code to run"))))
    nil))

;; ---- the module ----------------------------------------------------------

(defmodule CommitProbe [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-depot setup *clocks :disallow)
  (declare-depot setup *crash (hash-by :k))
  (when phase-timeout-seconds
    (set-launch-topology-dynamic-option! setup "mb" "topology.microbatch.phase.timeout.seconds" phase-timeout-seconds))

  (let [keeper (stream-topology topologies "keeper")]
    (declare-pstate keeper $$kept {String Long})
    (<<sources keeper
      (source> *clocks :> {:keys [*kept-batch *kept-clock]})
      (local-select> (keypath *kept-batch) $$kept :> *held)
      (<<if (nil? *held)
        (local-transform> [(keypath *kept-batch) (termval *kept-clock)] $$kept))))

  (let [crasher (stream-topology topologies "crasher")]
    (<<sources crasher
      (source> *crash :> {:keys [*crash-hold-ms]})
      (crash! *crash-hold-ms)))

  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$rows {String Object})
    (declare-pstate mb $$own {String Object})
    (<<sources mb
      (source> *things :> %things)

      ;; 1. from a constant, on task 0
      (<<batch
        (ops/current-microbatch-id :> *mbid)
        (str *mbid :> *batch)
        (road-now :> *road)
        (System/currentTimeMillis :> *wall)
        (<<if (= :read-first *road)
          (local-select> (keypath *batch) $$kept :> *kept-before)
          (<<if (nil? *kept-before)
            (hash-map :kept-batch *batch :kept-clock *wall :> *to-keep)
            (depot-partition-append! *clocks *to-keep :ack)
            (local-select> (keypath *batch) $$kept :> *clock)
           (else>)
            (identity *kept-before :> *clock))
         (else>)
          (identity *wall :> *clock))
        (attempt-begins! *batch *wall *clock :> *attempt)
        (hash-map :batch *batch :attempt *attempt :clock *clock :> *note)
        (|all)
        (local-transform> [(keypath "note") (termval *note)] $$own))

      ;; 2. from the things: one row for each, on the task of its key
      (<<batch
        (%things :> {:keys [*k *v]})
        (ops/current-microbatch-id :> *mbid2)
        (str *mbid2 :> *batch2)
        (ops/current-task-id :> *task2)
        (local-select> (keypath "note") $$own :> *note2)
        (thing-arrives! *batch2 *task2 *k *v *note2 :> *row)
        (local-transform> [(keypath *k) (termval *row)] $$rows))

      ;; 3. from a constant, to task 1 only: the hold of task 3, when asked
      (<<batch
        (ops/current-microbatch-id :> *mbid3)
        (str *mbid3 :> *batch3)
        (|direct 1)
        (tail! *batch3))))

  (<<query-topology topologies "hold" [*task *hold-ms :> *held-ms]
    (|direct *task)
    (hold! *task *hold-ms :> *held-ms)
    (|origin)))

;; ---- the run ---------------------------------------------------------------

(defn- first-seen
  "Takes the watchers' changes, the start of a label, and a value. Gives,
  for each task, the time at which a watcher whose label starts so first
  gave a row with that value."
  [seen label-start v]
  (into (sorted-map)
        (for [[label changes] (group-by :label seen)
              :when (.startsWith ^String label label-start)
              :let [hit (first (filter #(and (map? (:v %)) (= v (:value (:v %)))) changes))]
              :when hit]
          [(subs label (count label-start) (+ 6 (count label-start))) (:t hit)])))

(defn- say-what-was-given
  "Says, for one value, how often its batch began, and each different row
  with that value that a watcher was given, in the order of time."
  [seen v]
  (let [given (filter #(and (map? (:v %)) (= v (:value (:v %)))) (sort-by :t seen))
        batch (:batch (:v (first given)))]
    (u/say "SUMMARY" (pr-str v) ": batch" batch "began" (get @attempts batch) "time(s)")
    (doseq [[label changes] (sort-by key (group-by :label given))]
      (u/say "SUMMARY  " label "was given, in order:"
             (pr-str (mapv (fn [c] [(format "%.0f" (:t c)) (select-keys (:v c) [:clock :attempt])]) changes))))))

(defn -main [& args]
  (let [control? (= "control" (first args))]
    (when control? (alter-var-root #'phase-timeout-seconds (constantly nil)))
    (u/open-log! (if control? "logs/commit-control.log" "logs/commit.log"))
    (try
      (with-open [ipc (rtest/create-ipc)]
        (rtest/launch-module! ipc CommitProbe {:tasks 4 :threads 4})
        (let [m      (get-module-name CommitProbe)
              things (foreign-depot ipc m "*things")
              rows   (foreign-pstate ipc m "$$rows")
              ks     (vec (rtest/gen-hashing-index-keys "key-" 4))
              stop   (atom false)
              seen   (atom [])
              row-of (fn [k] (foreign-select-one (keypath k) rows))
              four!  (fn [v before-resume]
                       (rtest/pause-microbatch-topology! ipc m "mb")
                       (doseq [k ks] (foreign-append! things {:k k :v v} :append-ack))
                       (before-resume)
                       (rtest/resume-microbatch-topology! ipc m "mb")
                       (u/say "OUTSIDE four things with the value" (pr-str v) "are on the depot; the topology goes on"))
              all?   (fn [v limit] (boolean (u/wait-until #(every? (fn [k] (= v (:value (row-of k)))) ks) limit)))
              scenario! (fn [title v the-road plain? the-plan]
                          (u/say "RUN     ----" title "----")
                          (reset! road the-road)
                          (reset! plain-rows plain?)
                          (Thread/sleep 800)
                          (reset! loud true)
                          (four! v #(reset! plan (assoc the-plan :hold-began (promise))))
                          (u/say "OUTSIDE all four rows give" (pr-str v) ":" (all? v 40000))
                          (Thread/sleep 1500)
                          (reset! loud false)
                          (reset! road :none)
                          (reset! plain-rows false)
                          (doseq [[i k] (map-indexed vector ks)]
                            (u/say "OUTSIDE the row of task" i "at the end:" (pr-str (u/within 5000 #(row-of k)))))
                          (say-what-was-given @seen v))]
          (reset! handles {:hold (foreign-query ipc m "hold")
                           :crash (foreign-depot ipc m "*crash")
                           :crash-key (ks 3)})
          (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads, the topology's phase timeout"
                 (if phase-timeout-seconds (str "set to " phase-timeout-seconds " s") "NOT set (Rama's own value)")
                 ". Keys, one for each task:" (pr-str ks))
          (four! "warm" (fn []))
          (u/say "OUTSIDE all four rows give warm:" (all? "warm" 20000))

          (doseq [[i k] (map-indexed vector ks)]
            (u/watch (str "SELECT  task " i " " k) #(row-of k) stop seen 100000))
          (let [pushes (doall
                        (for [[i k] (map-indexed vector ks)]
                          (let [label (str "PUSH    task " i " " k)]
                            (u/within 20000
                                      #(foreign-proxy (keypath k) rows
                                                      {:callback-fn
                                                       (fn [new-value diff old-value]
                                                         (swap! seen conj {:t (u/ms) :label label :v new-value})
                                                         (u/say label "got" (.getSimpleName (class diff))
                                                                "· new" (pr-str new-value)
                                                                (if (= new-value old-value) "· THE SAME as the value before" "")))})))))]
            (Thread/sleep 800)
            (if control?
              (scenario! "CONTROL: task 3's thread is held for 3500 ms, and no phase timeout is set"
                         "control" :none false {:hold-ms 3500})
              (do
                ;; S1: batches with no hold: how far apart do the four tasks show one batch?
                (u/say "RUN     ---- S1: ten batches with no hold. When does each task first show the batch? ----")
                (dotimes [i 10]
                  (let [v (str "plain-" i)]
                    (four! v (fn []))
                    (all? v 20000)
                    (Thread/sleep 150)
                    (let [at (first-seen @seen "SELECT  " v)
                          ts (vals at)]
                      (u/say "SPREAD " v ": first seen by the selects at" (pr-str (update-vals at #(format "%.1f" %)))
                             "→ from the first task to the last:" (format "%.1f" (- (apply max ts) (apply min ts))) "ms"))))

                ;; S2: a hold that is shorter than the phase timeout
                (u/say "RUN     ---- S2: task 3's thread is held for 1200 ms, less than the phase timeout ----")
                (reset! loud true)
                (four! "held-short" #(reset! plan {:hold-ms 1200 :hold-began (promise)}))
                (Thread/sleep 400)
                (foreign-append! things {:k (ks 0) :v "after-held-short"} :append-ack)
                (u/say "OUTSIDE one more thing, for task 0's key, was appended while task 3 is held")
                (u/say "OUTSIDE task 0's key gives after-held-short:"
                       (boolean (u/wait-until #(= "after-held-short" (:value (row-of (ks 0)))) 20000)))
                (Thread/sleep 800)
                (reset! loud false)
                (say-what-was-given @seen "held-short")

                (scenario! "S3: held for 3500 ms, more than the phase timeout. Each attempt writes its own reading of the clock."
                           "held-long" :none false {:hold-ms 3500})
                (scenario! "S4: the same hold, with the clock kept through the keeper. A row still carries its attempt's number."
                           "held-kept" :read-first false {:hold-ms 3500})
                (scenario! "S5: the same hold, the clock kept, and a row carries no attempt's number: two attempts write the same row."
                           "held-same" :read-first true {:hold-ms 3500})
                (scenario! "S6: held for 700 ms and then a THROW on task 3's thread. Each attempt writes its own reading of the clock."
                           "thrown" :none false {:hold-ms 700 :crash? true})
                (scenario! "S7: the same hold and throw, the clock kept, and two attempts write the same row."
                           "thrown-same" :read-first true {:hold-ms 700 :crash? true})))
            (reset! stop true)
            (doseq [p pushes] (when (instance? java.io.Closeable p) (try (.close ^java.io.Closeable p) (catch Throwable _ nil))))
            (u/say "RUN     END"))))
      (catch Throwable t
        (u/say "RUN     THREW" (u/root-message t))
        (.printStackTrace t))))
  (shutdown-agents)
  (System/exit 0))
