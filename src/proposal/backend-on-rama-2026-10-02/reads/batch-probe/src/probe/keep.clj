(ns probe.keep
  "The run for B3 (ii) and B4: a clock that an attempt keeps outside the
  microbatch topology, so that a second attempt of the same batch reads the
  first attempt's clock.

  The road that is tried. The microbatch topology appends {batch id, clock}
  to a second depot from inside the attempt (`depot-partition-append!`). A
  stream topology of the same module consumes that depot into a PState of
  its own, batch id → clock, and writes a batch id only when it holds none:
  the first write wins. The microbatch topology then reads that PState, on
  the same task, and uses what it reads.

  The module. Depot of things, as in `probe.fail`. Depot of clocks, which no
  client may append to. The stream topology `keeper` and its PState
  `$$kept`. The microbatch topology `mb` of three batch blocks:

    1. from a constant, on task 0: reads the wall clock, then takes the road
       that the run chose for now (see `road`), and writes the clock that
       came out of it, with the attempt's number, to every task.
    2. from the things: one row for each, carrying that clock.
    3. on every task: the forced failure, as in `probe.fail`.

  What the JVM keeps, beside Rama: the road chosen for now, the count of
  attempts, what each attempt read and how long the road took, and each
  record that the keeper got. Memory of this process, for the report only.

  Run:  clojure -M:keep   (from this folder; the log is logs/keep.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [probe.util :as u]))

;; ---- what the JVM keeps -------------------------------------------------

(defonce ^{:doc "The road that step 1 takes now: :none, :read-first, :append-always, :append-ack-only or :no-ack."}
  road (atom :none))

(defonce ^{:doc "Batch id → how many attempts of it began."}
  attempts (atom {}))

(defonce ^{:doc "[batch id, attempt] → {:t-start :road :wall :clock :detail :road-ms :things n :t-end}."}
  attempt-notes (atom {}))

(defonce ^{:doc "Each record that the keeper got: {:t :batch :clock :held}."}
  keeper-got (atom []))

(defonce ^{:doc "The run's wish to fail: {:fail-times n}."}
  plan (atom {:fail-times 0}))

;; ---- the functions that the topologies call -----------------------------

(defn road-now
  "Gives the road that the run chose for now."
  []
  @road)

(defn keeper-gets!
  "The keeper calls it for each record of the depot of clocks. Takes the
  batch id, the clock of the record, and what `$$kept` held for that batch
  id before."
  [batch clock held]
  (swap! keeper-got conj {:t (u/ms) :batch batch :clock clock :held held})
  nil)

(defn attempt-begins!
  "Step 1 calls it once for an attempt, after the road. Takes the batch id,
  the road, the wall clock that this attempt read, the clock that came out
  of the road, a word for what the road did, and the nanoseconds before and
  after the road. Counts the attempt and gives its number."
  [batch road-taken wall clock detail t0 t1]
  (let [n (get (swap! attempts update batch (fnil inc 0)) batch)]
    (swap! attempt-notes assoc [batch n]
           {:t-start (u/ms) :road road-taken :wall wall :clock clock :detail detail
            :road-ms (/ (double (- t1 t0)) 1e6) :things 0})
    n))

(defn thing-arrives!
  "Step 2 calls it for each thing. Takes the batch id, the task, the thing's
  key and value, and the note that step 1 wrote on this task. Gives the row
  to write."
  [batch task k v note]
  (swap! attempt-notes update-in [[batch (:attempt note)] :things] (fnil inc 0))
  {:value v :clock (:clock note) :attempt (:attempt note) :batch batch})

(defn late-point!
  "Step 3 calls it on every task, after step 2 is done on all of them. On
  task 0 it keeps the time at which the attempt came this far. On task 3,
  when the run asked for a failure and the attempt got things, it throws."
  [batch task]
  (let [attempt (get @attempts batch)
        things (get-in @attempt-notes [[batch attempt] :things] 0)]
    (when (zero? task)
      (swap! attempt-notes assoc-in [[batch attempt] :t-end] (u/ms)))
    (when (and (= 3 task) (pos? things))
      (let [[old _] (swap-vals! plan update :fail-times #(max 0 (dec %)))]
        (when (pos? (:fail-times old))
          (u/say "INSIDE  batch" batch "attempt" attempt "task" task ": the last step THROWS on purpose")
          (throw (ex-info "probe: forced failure of this attempt" {:batch batch :attempt attempt})))))
    nil))

;; ---- the module ----------------------------------------------------------

(defmodule KeepProbe [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-depot setup *clocks :disallow)

  (let [keeper (stream-topology topologies "keeper")]
    (declare-pstate keeper $$kept {String Long})
    (<<sources keeper
      (source> *clocks :> {:keys [*kept-batch *kept-clock]})
      (local-select> (keypath *kept-batch) $$kept :> *held)
      (keeper-gets! *kept-batch *kept-clock *held)
      (<<if (nil? *held)
        (local-transform> [(keypath *kept-batch) (termval *kept-clock)] $$kept))))

  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$rows {String Object})
    (declare-pstate mb $$own {String Object})
    (<<sources mb
      (source> *things :> %things)

      ;; 1. from a constant, on task 0: the wall clock, then the road
      (<<batch
        (ops/current-microbatch-id :> *mbid)
        (str *mbid :> *batch)
        (road-now :> *road)
        (System/currentTimeMillis :> *wall)
        (hash-map :kept-batch *batch :kept-clock *wall :> *to-keep)
        (System/nanoTime :> *t0)
        (<<cond
          ;; read first; append, and wait for the keeper, only when nothing is kept
          (case> (= :read-first *road))
          (local-select> (keypath *batch) $$kept :> *kept-before)
          (<<if (nil? *kept-before)
            (depot-partition-append! *clocks *to-keep :ack)
            (local-select> (keypath *batch) $$kept :> *clock)
            (identity :appended-then-read :> *detail)
           (else>)
            (identity *kept-before :> *clock)
            (identity :found-kept :> *detail))

          ;; append on every attempt, wait for the keeper, then read
          (case> (= :append-always *road))
          (depot-partition-append! *clocks *to-keep :ack)
          (local-select> (keypath *batch) $$kept :> *clock)
          (identity :appended-then-read :> *detail)

          ;; append and wait for the depot only, then read
          (case> (= :append-ack-only *road))
          (depot-partition-append! *clocks *to-keep :append-ack)
          (local-select> (keypath *batch) $$kept :> *clock)
          (identity :appended-append-ack-then-read :> *detail)

          ;; append and wait for nothing, then read
          (case> (= :no-ack *road))
          (depot-partition-append! *clocks *to-keep nil)
          (local-select> (keypath *batch) $$kept :> *clock)
          (identity :appended-no-ack-then-read :> *detail)

          ;; no road: the attempt's own reading of the wall clock
          (default>)
          (identity *wall :> *clock)
          (identity :no-road :> *detail))
        (System/nanoTime :> *t1)
        (attempt-begins! *batch *road *wall *clock *detail *t0 *t1 :> *attempt)
        (hash-map :batch *batch :attempt *attempt :clock *clock :> *note)
        (|all)
        (local-transform> [(keypath "note") (termval *note)] $$own))

      ;; 2. from the things: one row for each, with the clock of step 1
      (<<batch
        (%things :> {:keys [*k *v]})
        (ops/current-microbatch-id :> *mbid2)
        (str *mbid2 :> *batch2)
        (ops/current-task-id :> *task2)
        (local-select> (keypath "note") $$own :> *note2)
        (thing-arrives! *batch2 *task2 *k *v *note2 :> *row)
        (local-transform> [(keypath *k) (termval *row)] $$rows))

      ;; 3. on every task: the end of the attempt, or the forced failure
      (<<batch
        (|all)
        (ops/current-microbatch-id :> *mbid3)
        (str *mbid3 :> *batch3)
        (ops/current-task-id :> *task3)
        (late-point! *batch3 *task3)))))

;; ---- the run ---------------------------------------------------------------

(defn- stats
  "Takes numbers. Gives them as text: how many, the least, the middle, the
  ninth of ten, and the most, each with one decimal."
  [xs]
  (if (empty? xs)
    "none"
    (let [s (vec (sort xs)) n (count s) at (fn [q] (s (min (dec n) (int (* q n)))))]
      (format "%d runs · least %.1f · middle %.1f · 9 of 10 under %.1f · most %.1f ms"
              n (double (first s)) (double (at 0.5)) (double (at 0.9)) (double (last s))))))

(defn- batch-with-value
  "Takes a foreign PState of rows and a key whose row holds the wanted
  value. Gives the row's batch id."
  [rows k]
  (:batch (foreign-select-one (keypath k) rows)))

(defn -main [& _]
  (u/open-log! "logs/keep.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc KeepProbe {:tasks 4 :threads 4})
      (let [m      (get-module-name KeepProbe)
            things (foreign-depot ipc m "*things")
            clocks (foreign-depot ipc m "*clocks")
            rows   (foreign-pstate ipc m "$$rows")
            kept   (foreign-pstate ipc m "$$kept")
            ks     (vec (rtest/gen-hashing-index-keys "key-" 4))
            row-of (fn [k] (foreign-select-one (keypath k) rows))
            ;; one thing, then wait for its row: gives the ms from the append call to the row
            one!   (fn [k v]
                     (let [t (u/ms)]
                       (foreign-append! things {:k k :v v} :append-ack)
                       (when (u/wait-until #(= v (:value (row-of k))) 15000)
                         (- (u/ms) t))))
            ;; four things in one batch, one for each task
            four!  (fn [v]
                     (rtest/pause-microbatch-topology! ipc m "mb")
                     (doseq [k ks] (foreign-append! things {:k k :v v} :append-ack))
                     (rtest/resume-microbatch-topology! ipc m "mb")
                     (boolean (u/wait-until #(every? (fn [k] (= v (:value (row-of k)))) ks) 30000)))
            measure (fn [the-road label n]
                      (reset! road the-road)
                      (Thread/sleep 600)
                      (let [before (set (keys @attempt-notes))
                            waits (doall (for [i (range n)]
                                           (let [w (one! (ks (mod i 4)) (str label "-" i))]
                                             (Thread/sleep (+ 20 (rand-int 120)))
                                             w)))
                            _ (Thread/sleep 600)
                            mine (for [[key note] @attempt-notes
                                       :when (and (not (before key)) (= the-road (:road note)))]
                                   note)
                            with-things (filter #(pos? (:things %)) mine)
                            empty-runs (filter #(zero? (:things %)) mine)]
                        (u/say "COST   " label ": the road, inside the attempt, in batches that got a thing:"
                               (stats (map :road-ms with-things)))
                        (u/say "COST   " label ": the road, inside the attempt, in runs that got nothing:"
                               (stats (map :road-ms empty-runs)))
                        (u/say "COST   " label ": from the start of step 1 to step 3 on task 0, batches that got a thing:"
                               (stats (keep #(when (:t-end %) (- (:t-end %) (:t-start %) (- (:road-ms %)))) with-things))
                               "(the road's own time is added back, so this is the whole attempt up to step 3)")
                        (u/say "COST   " label ": from the append call to the row read from outside:"
                               (stats (remove nil? waits)) "·" (count (filter nil? waits)) "never came")
                        (u/say "COST   " label ": what the road did:" (pr-str (frequencies (map :detail mine)))
                               "· clocks that came out nil:" (count (filter #(nil? (:clock %)) mine)))))]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads. Keys, one for each task:" (pr-str ks))
        (u/say "RUN     warm-up:" (four! "warm-1") (four! "warm-2"))
        (dotimes [i 20] (one! (ks (mod i 4)) (str "warm-" i)))

        ;; B3 (ii): the cost of the road
        (u/say "RUN     ---- the cost: 40 batches of one thing on each road ----")
        (measure :none "no road" 40)
        (measure :read-first "read first, append with :ack when nothing is kept" 40)
        (measure :append-always "append with :ack on every attempt" 40)
        (measure :none "no road, again" 40)
        (measure :append-ack-only "append with :append-ack" 20)
        (measure :no-ack "append with no ack" 20)
        (reset! road :none)
        (Thread/sleep 600)
        (u/say "RUN     records on the depot of clocks, task 0's partition:"
               (pr-str (foreign-depot-partition-info clocks 0)))

        ;; B4: the whole pattern, on each of the two roads that wait for the keeper
        (doseq [[the-road value] [[:read-first "X"] [:append-always "Y"]]]
          (u/say "RUN     ---- the whole pattern on the road" the-road ": the first attempt of batch" value "is made to fail ----")
          (reset! road the-road)
          (Thread/sleep 600)
          (rtest/pause-microbatch-topology! ipc m "mb")
          (doseq [k ks] (foreign-append! things {:k k :v value} :append-ack))
          (reset! plan {:fail-times 1})
          (rtest/resume-microbatch-topology! ipc m "mb")
          (u/say "OUTSIDE all four rows give" value ":"
                 (boolean (u/wait-until #(every? (fn [k] (= value (:value (row-of k)))) ks) 30000)))
          (let [batch (batch-with-value rows (ks 0))
                n (get @attempts batch)]
            (doseq [a (range 1 (inc n))]
              (let [{:keys [t-start wall clock detail road-ms things]} (get @attempt-notes [batch a])]
                (u/say "INSIDE  batch" batch "attempt" a "began at" (format "%.1f" t-start)
                       "· read the wall clock" wall "· the road" (pr-str detail) "gave" clock
                       "in" (format "%.2f" road-ms) "ms · things" things)))
            (doseq [g (filter #(= batch (:batch %)) @keeper-got)]
              (u/say "INSIDE  the keeper got, at" (format "%.1f" (:t g)) ": batch" (:batch g) "clock" (:clock g)
                     "· it held before:" (pr-str (:held g)) (if (nil? (:held g)) "→ written" "→ not written")))
            (u/say "OUTSIDE $$kept gives for batch" batch ":" (foreign-select-one (keypath batch) kept {:pkey (ks 0)}))
            (doseq [[i k] (map-indexed vector ks)]
              (u/say "OUTSIDE the row of task" i ":" (pr-str (row-of k))))
            (let [c1 (:wall (get @attempt-notes [batch 1]))
                  c2 (:wall (get @attempt-notes [batch 2]))
                  in-rows (set (map #(:clock (row-of %)) ks))]
              (u/say "RESULT  batch" batch ": attempt 1 read" c1 "· attempt 2 read" c2
                     "· the rows that attempt 2 wrote carry" (pr-str in-rows)
                     "→ the rows carry the FIRST attempt's clock:" (= #{c1} in-rows)))))
        (reset! road :none)
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
