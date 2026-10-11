(ns probe.walk
  "Run W: the changed step 2 of a batch, as the plan of `base-for-a-reader`
  writes it, on a stand-in module. It is a probe for the plan and no code of
  the store: it requires nothing of the store's tree.

  What the batch probe left open and this run asks:

    W1  does a depot append with :ack work at the place where the plan puts
        it: in a batch block, after a hash partitioner, inside two
        conditionals, before the block goes to every task and gathers into a
        temporary PState?
    W2  of which class is the microbatch id, and can a row hold it as a Long?
    W3  one row for the newest batch, under one key: is it read in one select
        as a map, and does a second attempt read the first attempt's clock?
    W4  a run that got nothing: does it read no clock, append nothing and
        read nothing of the kept row?
    W5  the stream topology fails the record on its first try (the batch
        probe's \"Not settled\"): what does the attempt that appended see, and
        what clock do the rows end with?
    W6  across a module update: does the microbatch id go on, and is the kept
        row still there?
    W7  the same with two workers and two threads for four tasks, as the
        store's proofs launch their cluster.

  The module. A depot of things. A depot of batch clocks that no client may
  append to. A stream topology that keeps one row, the clock of the newest
  batch: a record of the same batch as the row, or of an older one, writes
  nothing. A microbatch topology of four batch blocks, shaped as the store's
  walk: (1) each thing sends a number to the gate's route and itself to its
  own task; (2) on the gate's task the clock is taken, then every task
  replies; (3) each task writes a row for each thing, with the clock; (4)
  every task reaches a late point, where the run can ask for a throw.

  What the JVM keeps, for the report only: what each attempt did, each
  record that the stream topology got, and the run's wishes.

  Run:  clojure -M:walk              (from this folder; the log is logs/walk.log)
        clojure -M:walk other-task   (the gate's task is task 2, not the task
                                      where a batch block starts; the log is
                                      logs/walk-other-task.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [com.rpl.rama.test :as rtest]
            [probe.say :as s])
  (:import [clojure.lang Keyword]))

;; ---- what the JVM keeps ---------------------------------------------------

(defonce ^{:doc "Each thing that step 2 said on the gate's task, in order."}
  step-2-notes (atom []))

(defonce ^{:doc "Each record that the stream topology got, in order."}
  records-got (atom []))

(defonce ^{:doc "The run's wishes: {:throw-late? bool :throw-in-keeper? bool}."}
  wishes (atom {}))

;; ---- pure functions that the topologies call ------------------------------

(defn gather
  "Puts two gathered vectors together. nil is the empty value."
  [a b]
  (cond (nil? a) b (nil? b) a :else (into (vec a) b)))

(def +gathered
  "The aggregator of the two temporary PStates."
  (combiner gather :init-fn (fn [] nil)))

(def route
  "The route of the gate's task. :microbatch, as in the store, gives task 0
  in a cluster of 4 tasks, which is also the task where a batch block starts.
  The run `other-task` puts a key of task 2 here before the launch, so that
  the hash partitioner of step 2 does change the task."
  :microbatch)

(defn gates-route
  "Gives the route of the gate's task."
  []
  route)

(defn messages-of
  "Step 1 for one thing. Gives [route kind payload] twice: a number for the
  gate's task, and the thing for the task of its key."
  [thing]
  [[route :numbers [1]]
   [(:k thing) :entry [thing]]])

(defn kept-clock
  "Takes the kept row, {:batch :clock} or nil, and a microbatch id. Gives
  the row's clock when the row is of that batch, else nil."
  [row batch-id]
  (when (and row (= (long batch-id) (long (:batch row))))
    (:clock row)))

(defn record-of
  "Takes a microbatch id and a clock. Gives the record to append."
  [batch-id clock]
  {:batch (long batch-id) :clock (long clock)})

(defn row-to-keep
  "The stream topology's rule. Takes the kept row or nil, and a record.
  Gives the row to write, or nil when nothing is written: the record is of
  the batch that the row is of, or of an older one."
  [held record]
  (when (or (nil? held) (< (long (:batch held)) (long (:batch record))))
    {:batch (long (:batch record)) :clock (long (:clock record))}))

(defn kept-or-fail
  "Takes what `kept-clock` gave after the append, and the microbatch id.
  Gives the clock. Throws when there is none: no stamp may be made from a
  clock that is not kept."
  [clock batch-id]
  (when (nil? clock)
    (throw (ex-info "probe: the clock of this batch is not kept" {:batch batch-id})))
  clock)

(defn replies-of
  "Step 2 on one task. Takes the things that the task gathered, or nil, and
  the batch's clock. Gives [route [reply]] for each thing."
  [entries clock]
  (mapv (fn [thing] [(:k thing) [{:k (:k thing) :v (:v thing) :clock clock}]]) entries))

(defn row-of
  "Takes a reply. Gives the row to write: the value and the clock, and
  nothing of the attempt."
  [reply]
  {:value (:v reply) :clock (long (:clock reply))})

;; ---- functions that say what happened --------------------------------------

(defn note!
  "Takes a map. Keeps it with the time, as a note of step 2."
  [m]
  (swap! step-2-notes conj (assoc m :t (s/ms)))
  nil)

(defn found-kept! [batch-id clock]
  (note! {:what :found-kept :batch batch-id :clock clock}))

(defn read-the-clock! [batch-id wall]
  (note! {:what :read-the-clock :batch batch-id :wall wall :class (.getName (class batch-id))}))

(defn appended! [batch-id kept-after nanos-before nanos-after]
  (note! {:what :appended :batch batch-id :kept-after kept-after
          :ms (/ (double (- nanos-after nanos-before)) 1e6)}))

(defonce ^{:doc "The microbatch ids of the batches that had things."}
  batches-with-things (atom #{}))

(defn step-2-on-the-gates-task! [batch-id numbers clock]
  (when (seq numbers) (swap! batches-with-things conj batch-id))
  (note! {:what :step-2 :batch batch-id :things (count numbers) :clock clock}))

(defn stream-got!
  "The stream topology calls it for each record, with the row that it held.
  When the run asked for it, it throws, this once."
  [held record]
  (swap! records-got conj {:t (s/ms) :record record :held held})
  (let [[old _] (swap-vals! wishes assoc :throw-in-keeper? false)]
    (when (:throw-in-keeper? old)
      (s/say "INSIDE  the stream topology THROWS on the record" (pr-str record) "· it held" (pr-str held))
      (throw (ex-info "probe: the stream topology fails this record once" {}))))
  nil)

(defn late-point!
  "Step 4 calls it on every task. When the run asked for it and the batch
  had things, it throws, this once, on the first task that comes here. The
  first run of this probe threw in a run that got nothing, which shows
  nothing about a kept clock."
  [batch-id task]
  (when (contains? @batches-with-things batch-id)
    (let [[old _] (swap-vals! wishes assoc :throw-late? false)]
      (when (:throw-late? old)
        (s/say "INSIDE  batch" batch-id "task" task ": the late point THROWS on purpose, after the rows of step 3 were written")
        (throw (ex-info "probe: forced failure of this attempt" {:batch batch-id})))))
  nil)

;; ---- the module -------------------------------------------------------------

(defmodule WalkProbe [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-depot setup *batch-clock-depot :disallow)

  (let [clocks (stream-topology topologies "batch-clock")]
    (declare-pstate clocks $$batch-clock {Keyword (fixed-keys-schema {:batch Long :clock Long})})
    (<<sources clocks
      (source> *batch-clock-depot {:start-from :beginning} :> *record)
      (local-select> (keypath :microbatch) $$batch-clock :> *held)
      (stream-got! *held *record)
      (row-to-keep *held *record :> *row)
      (<<if *row
        (local-transform> [(keypath :microbatch) (termval *row)] $$batch-clock))))

  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$rows {String (fixed-keys-schema {:value String :clock Long})})
    (<<sources mb
      (source> *things {:start-from :beginning} :> %things)

      ;; 1. each thing: a number to the gate's route, itself to its own task
      (<<batch
        (%things :> *thing)
        (messages-of *thing :> *messages)
        (ops/explode *messages :> [*route *kind *payload])
        (|hash *route)
        (+compound {*kind (+gathered *payload)} :> $$questions))

      ;; 2. the gate's task takes the clock; then every task replies
      (<<batch
        (gates-route :> *gates-route)
        (|hash *gates-route)
        (local-select> (keypath :numbers) $$questions :> *numbers)
        (ops/current-microbatch-id :> *batch-id)
        (<<if *numbers
          (local-select> (keypath :microbatch) $$batch-clock :> *kept-before)
          (kept-clock *kept-before *batch-id :> *clock-before)
          (<<if *clock-before
            (found-kept! *batch-id *clock-before)
            (identity *clock-before :> *now)
           (else>)
            (System/currentTimeMillis :> *wall)
            (read-the-clock! *batch-id *wall)
            (record-of *batch-id *wall :> *to-keep)
            (System/nanoTime :> *nanos-before)
            (depot-partition-append! *batch-clock-depot *to-keep :ack)
            (local-select> (keypath :microbatch) $$batch-clock :> *kept-after)
            (System/nanoTime :> *nanos-after)
            (appended! *batch-id *kept-after *nanos-before *nanos-after)
            (kept-clock *kept-after *batch-id :> *clock-after)
            (kept-or-fail *clock-after *batch-id :> *now))
         (else>)
          (identity nil :> *now))
        (step-2-on-the-gates-task! *batch-id *numbers *now)
        (|all)
        (local-select> (keypath :entry) $$questions :> *entries)
        (replies-of *entries *now :> *replies)
        (ops/explode *replies :> [*reply-route *reply])
        (|hash *reply-route)
        (+compound {:reply (+gathered *reply)} :> $$replies))

      ;; 3. each task writes a row for each thing, with the clock
      (<<batch
        (|all)
        (local-select> (keypath :reply) $$replies :> *mine)
        (ops/explode *mine :> *one)
        (get *one :k :> *k)
        (row-of *one :> *new-row)
        (local-transform> [(keypath *k) (termval *new-row)] $$rows))

      ;; 4. every task: the late point
      (<<batch
        (|all)
        (ops/current-microbatch-id :> *batch-id-4)
        (ops/current-task-id :> *task-4)
        (late-point! *batch-id-4 *task-4)))))

;; ---- the run ------------------------------------------------------------------

(defn- notes-of
  "The notes of step 2 for one microbatch id, in order."
  [batch-id]
  (filter #(= batch-id (:batch %)) @step-2-notes))

(defn- say-batch
  "Says what step 2 did in each attempt of one batch, and what the stream
  topology got for it."
  [batch-id]
  (doseq [n (notes-of batch-id)]
    (s/say "INSIDE  batch" batch-id (format "at %.1f" (:t n)) (pr-str (dissoc n :t :batch))))
  (doseq [g (filter #(= batch-id (:batch (:record %))) @records-got)]
    (s/say "INSIDE  the stream topology got" (format "at %.1f" (:t g)) (pr-str (:record g))
           "· it held" (pr-str (:held g)))))

(defn -main [& args]
  (let [other-task? (= "other-task" (first args))]
    (when other-task? (alter-var-root #'route (constantly "key-j")))
    (s/open-log! (if other-task? "logs/walk-other-task.log" "logs/walk.log")))
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc WalkProbe {:tasks 4 :threads 2 :workers 2})
      (let [m      (get-module-name WalkProbe)
            things (foreign-depot ipc m "*things")
            clocks (foreign-depot ipc m "*batch-clock-depot")
            rows   (foreign-pstate ipc m "$$rows")
            kept   (foreign-pstate ipc m "$$batch-clock")
            ks     (vec (rtest/gen-hashing-index-keys "key-" 4))
            row-of* (fn [k] (foreign-select-one (keypath k) rows))
            kept-row (fn [] (s/within 5000 #(foreign-select-one (keypath :microbatch) kept {:pkey route})))
            clock-records (fn []
                            (vec (for [p (range (:num-partitions (foreign-object-info clocks)))
                                       :let [{:keys [start-offset end-offset]} (foreign-depot-partition-info clocks p)]
                                       r (foreign-depot-read clocks p start-offset end-offset)]
                                   [p r])))
            four!  (fn [v]
                     (rtest/pause-microbatch-topology! ipc m "mb")
                     (doseq [k ks] (foreign-append! things {:k k :v v} :append-ack))
                     (rtest/resume-microbatch-topology! ipc m "mb")
                     (boolean (s/wait-until #(every? (fn [k] (= v (:value (row-of* k)))) ks) 40000)))
            batch-of (fn [v]
                       ;; the batch whose step 2 counted four things, the last such
                       (:batch (last (filter #(and (= :step-2 (:what %)) (= 4 (:things %))) @step-2-notes))))
            report (fn [title v]
                     (let [b (batch-of v)]
                       (s/say "RUN     ----" title "---- the batch is" b)
                       (say-batch b)
                       (s/say "OUTSIDE the kept row:" (pr-str (kept-row)))
                       (s/say "OUTSIDE the rows:" (pr-str (mapv #(s/within 5000 (fn [] (row-of* %))) ks)))
                       (s/say "RESULT  the four rows carry one clock:" (= 1 (count (set (map #(:clock (row-of* %)) ks))))
                              "· it is the kept row's clock:" (= #{(:clock (kept-row))} (set (map #(:clock (row-of* %)) ks)))
                              "· clocks read in this batch:" (pr-str (mapv :wall (filter #(= :read-the-clock (:what %)) (notes-of b)))))))]
        (s/say "RUN     Rama 1.6.0, in-process, 4 tasks, 2 threads, 2 workers. Keys:" (pr-str ks)
               "· the route of the gate's task:" (pr-str route))

        ;; W1, W2, W3: batches with things
        (s/say "OUTSIDE warm:" (four! "warm-1") (four! "warm-2"))
        (report "W1 W2 W3: a batch with things, nothing fails" "warm-2")

        ;; W4: runs that got nothing
        (let [records-before (count (clock-records))
              notes-before (count @step-2-notes)]
          (Thread/sleep 3000)
          (let [idle (subvec @step-2-notes notes-before)]
            (s/say "RUN     ---- W4: 3 s with nothing appended ----")
            (s/say "RESULT  runs of step 2 in that time:" (count (filter #(= :step-2 (:what %)) idle))
                   "· of them with a clock:" (count (filter #(and (= :step-2 (:what %)) (:clock %)) idle))
                   "· clocks read:" (count (filter #(= :read-the-clock (:what %)) idle))
                   "· records on the depot of clocks before and after:" records-before (count (clock-records)))))

        ;; the cost of the keep, 20 batches of one thing
        (let [notes-before (count @step-2-notes)]
          (dotimes [i 20]
            (foreign-append! things {:k (ks (mod i 4)) :v (str "cost-" i)} :append-ack)
            (s/wait-until #(= (str "cost-" i) (:value (row-of* (ks (mod i 4))))) 20000))
          (let [took (sort (keep :ms (filter #(= :appended (:what %)) (subvec @step-2-notes notes-before))))]
            (s/say "COST    the append with :ack and the read back, inside the attempt:" (count took) "batches · least"
                   (format "%.1f" (first took)) "· middle" (format "%.1f" (nth took (quot (count took) 2)))
                   "· most" (format "%.1f" (last took)) "ms")))

        ;; W3: a second attempt
        (swap! wishes assoc :throw-late? true)
        (s/say "OUTSIDE four rows give second:" (four! "second"))
        (report "W3: the first attempt fails at the late point, after the clock is kept" "second")

        ;; W5: the stream topology fails the record on its first try
        (swap! wishes assoc :throw-in-keeper? true)
        (s/say "OUTSIDE four rows give keeper-fails:" (four! "keeper-fails"))
        (report "W5: the stream topology throws on the record's first try" "keeper-fails")
        (s/say "OUTSIDE the last records on the depot of clocks:" (pr-str (take-last 3 (clock-records))))

        ;; W6: a module update
        (let [before (kept-row)]
          (s/say "RUN     ---- W6: the module is updated with itself ----")
          (s/say "OUTSIDE update-module! gave" (pr-str (s/within 120000 #(do (rtest/update-module! ipc WalkProbe) :updated))))
          (s/say "OUTSIDE the kept row before the update:" (pr-str before) "· after:" (pr-str (kept-row)))
          (s/say "OUTSIDE four rows give after-update:" (four! "after-update"))
          (report "W6: the first batch with things after the update" "after-update"))

        (s/say "RESULT  records on the depot of clocks, by partition:"
               (pr-str (frequencies (map first (clock-records))))
               "· batches with things that step 2 saw:"
               (count (distinct (map :batch (filter #(and (= :step-2 (:what %)) (pos? (:things %))) @step-2-notes)))))
        (s/say "RUN     END")))
    (catch Throwable t
      (s/say "RUN     THREW" (s/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
