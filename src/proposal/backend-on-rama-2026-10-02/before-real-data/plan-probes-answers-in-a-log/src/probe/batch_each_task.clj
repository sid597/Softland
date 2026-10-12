(ns probe.batch-each-task
  "The earlier form of run M of the probe of the plan `answers-in-a-log`:
  each task keeps and appends its own. The plan takes the later form,
  `probe.batch`, one keeping task for a batch; this one is kept because its
  log is cited.

  The plan has the microbatch gate keep what a batch decided in a table of
  its own, in the same attempt as the answers' rows, and has the next run of
  the gate, which begins only when that batch is committed on every task,
  append it to a depot and take it out of the table. The batch probe of 12
  October (`../../reads/batch-probe/`) ran the two facts that this rests on:
  Rama sets a topology's own table back when an attempt fails, and an append
  from inside an attempt stays. It did not run the two together in this
  shape. M runs it on a stand-in walk, with a failure forced in the attempt
  that writes and in the attempt that appends.

  The walk, three batch blocks:

    1. arrive   each thing is sent to the task of its id, and a count to one
                task, the \"gate's task\". A thing marked as a stray is kept
                at once, on the task where it arrived, under a key of this
                batch.
    2. close    the gate's task reads whether the batch before left
                something to keep, and writes the same for this batch. Then
                every task: when the batch before left something, it reads
                what the table holds under keys of earlier batches, appends
                each as one record with :append-ack, and takes the keys out.
    3. write    each task writes a row for each thing sent to it, and keeps
                them, at most `chunk-size` in one key of this batch.

  A row holds the number of the attempt that wrote it, so that a record of
  an attempt that failed could be told from one that counted. The store's
  rows hold no such number. A probe, not a design: it requires nothing of
  the store's tree, and no table, name or form here is a proposal for the
  store. The walk's functions read atoms of this JVM, which works only
  because the in-process cluster shares the JVM with the run."
  (:require [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [probe.util :as u]))

(def slots
  "A key of the table of what is to keep is batch x slots + slot, so the keys of one batch are neighbours and below every key of a later batch."
  (bit-shift-left 1 21))

(def chunk-size "How many rows one key of the table, and so one record of the depot, holds at most." 2)

;; ---- what the walk notes, in the memory of this JVM -----------------------

(defonce ^{:doc "batch id → how many attempts of it began."} attempts (atom {}))
(defonce ^{:doc "Each forced throw: {:batch :attempt :where :task}."} throws (atom []))
(defonce ^{:doc "True when the next closing that appended something must throw after it, once."} throw-after-keep (atom false))
(defonce ^{:doc "The milliseconds that the appends of one task took in one closing, each [records ms]."} keep-times (atom []))

(defn attempt!
  "The gate's task calls it once in each attempt. Takes the batch id. Counts the attempt and gives its number."
  [batch]
  (get (swap! attempts update batch (fnil inc 0)) batch))

(defn attempt-of
  "Takes a batch id. Gives the number of its newest attempt."
  [batch]
  (get @attempts batch 0))

(defn gather
  "Puts two gathered values together: two vectors as one; nil is the empty value."
  [a b]
  (cond (nil? a) b (nil? b) a :else (into (vec a) b)))

(def +gathered (combiner gather :init-fn (fn [] nil)))

(defn messages
  "Takes a thing. Gives what block 1 sends for it, each [route kind payload]: the thing to the task of its id, unless it is a stray, and a count to the gate's task."
  [thing]
  (if (:stray thing)
    [["gate" :count [1]]]
    [[(:id thing) :todo [thing]] ["gate" :count [1]]]))

(defn stray-key
  "Takes the batch id and a stray. Gives the key under which it is kept: a slot of this batch that no chunk of rows takes."
  [batch thing]
  (+ (* (long batch) slots) (bit-shift-left 1 20) (long (:n thing))))

(defn stray-chunk
  "Takes the batch id and a stray. Gives what is kept of it."
  [batch thing]
  {:batch batch :strays [(:id thing)]})

(defn to-close-op
  "Takes the count that the gate's task gathered in this batch, or nil, and what it holds of the batch before, or nil. Gives :set when this batch has something to keep, :clear when it has none and the mark is there, else :leave."
  [counted to-close]
  (cond counted :set to-close :clear :else :leave))

(defn bound-of
  "Takes the batch id. Gives the least key that a keeping of this batch can have: each key below it is of an earlier batch."
  [batch]
  (* (long batch) slots))

(defn as-map
  "Takes the part of the table that a range gave. Gives it as a sorted map from key to what is kept."
  [m]
  (into (sorted-map) m))

(defn records-of "Takes what a closing read. Gives the records to append, in the order of the keys." [held] (vec (vals held)))
(defn keys-of "Takes what a closing read. Gives the keys to take out." [held] (vec (keys held)))

(defn kept!
  "A task calls it at the end of its closing. Takes the batch id of the run,
  the task, what it read and the nanosecond before its appends. Notes the
  time. Throws, once, when a throw after a keeping was asked for and this
  task appended something."
  [batch task held k0]
  (let [ms (/ (- (System/nanoTime) (long k0)) 1e6)]
    (when (seq held)
      (swap! keep-times conj [(count held) ms])
      (u/say "INSIDE  run" batch "attempt" (attempt-of batch) "task" task ": appended" (count held)
             "record(s), of the batches" (pr-str (distinct (map :batch (vals held))))
             "and took their keys out ·" (format "%.2f ms for the appends" ms)))
    (when (and (seq held) (compare-and-set! throw-after-keep true false))
      (swap! throws conj {:batch batch :attempt (attempt-of batch) :where :after-the-keeping :task task})
      (u/say "INSIDE  run" batch "attempt" (attempt-of batch) "task" task "THROWS on purpose, after its appends")
      (throw (ex-info "probe: forced failure after the keeping" {:batch batch}))))
  nil)

(defn rows-of
  "Takes the batch id and the things that were sent to this task, or nil. Gives [id row] for each, in the order of the ids. A row holds a stamp made from the batch id and the place in that order, the batch, and the number of the attempt that made it."
  [batch todo]
  (let [n (attempt-of batch)]
    (vec (map-indexed (fn [i t] [(:id t) {:stamp (+ (* (long batch) 1000) i) :batch batch :attempt n}])
                      (sort-by :id todo)))))

(defn chunks-of
  "Takes the batch id and the rows that a task wrote. Gives [key chunk] for each `chunk-size` of them: what the task keeps of them, under keys of this batch."
  [batch rows]
  (vec (map-indexed (fn [i part]
                      [(+ (* (long batch) slots) 1 i)
                       {:batch batch :answers (mapv (fn [[id row]] {:id id :row row}) part)}])
                    (partition-all chunk-size rows))))

(defn wrote!
  "A task calls it at the end of block 3. Takes the batch id, the task and
  the rows. Throws on the first attempt of a batch in which this task wrote
  a row whose id begins with FAIL."
  [batch task rows]
  (when (and (= 1 (attempt-of batch)) (some (fn [[id _]] (str/starts-with? id "FAIL")) rows))
    (swap! throws conj {:batch batch :attempt 1 :where :after-the-writes :task task})
    (u/say "INSIDE  batch" batch "attempt 1 task" task "THROWS on purpose, after it wrote" (count rows) "row(s) and what to keep of them")
    (throw (ex-info "probe: forced failure after the writes" {:batch batch})))
  nil)

;; ---- the module -----------------------------------------------------------

(defmodule KeepBatch [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-depot setup *kept :disallow)
  ;; two things of one partition in a batch at most, so that the run can
  ;; make the batch that closes another hold things of its own
  (set-launch-depot-dynamic-option! setup "*things" "depot.microbatch.max.records" 2)
  (let [mb (microbatch-topology topologies "gate")]
    (declare-pstate mb $$rows {String Object})
    (declare-pstate mb $$task (fixed-keys-schema {:to-keep  (map-schema Long Object {:subindex? true})
                                                   :to-close Long}))
    (<<sources mb
      (source> *things {:start-from :beginning} :> %things)

      ;; 1. arrive
      (<<batch
        (%things :> *thing)
        (ops/current-microbatch-id :> *b1)
        (<<if (get *thing :stray)
          (stray-key *b1 *thing :> *sk)
          (stray-chunk *b1 *thing :> *sc)
          (local-transform> [(keypath :to-keep *sk) (termval *sc)] $$task))
        (messages *thing :> *messages)
        (ops/explode *messages :> [*route *kind *payload])
        (|hash *route)
        (+compound {*kind (+gathered *payload)} :> $$sent))

      ;; 2. close: what the batch before left to keep goes to the depot
      (<<batch
        (identity "gate" :> *gate)
        (|hash *gate)
        (ops/current-microbatch-id :> *b2)
        (attempt! *b2 :> *n2)
        (local-select> (keypath :to-close) $$task :> *to-close)
        (local-select> (keypath :count) $$sent :> *counted)
        (to-close-op *counted *to-close :> *op)
        (<<cond
          (case> (= :set *op))
          (local-transform> [(keypath :to-close) (termval *b2)] $$task)

          (case> (= :clear *op))
          (local-transform> [(keypath :to-close) NONE>] $$task)

          (default>)
          (identity nil :> *left-as-it-is))
        (|all)
        (<<if *to-close
          (ops/current-task-id :> *t2)
          (bound-of *b2 :> *bound)
          (local-select> [(keypath :to-keep) (sorted-map-range 0 *bound) (view as-map)] $$task :> *held)
          (records-of *held :> *records)
          (System/nanoTime :> *k0)
          (loop<- [*left *records :> *appended]
            (<<if (empty? *left)
              (:> true)
             (else>)
              (first *left :> *record)
              (depot-partition-append! *kept *record :append-ack)
              (continue> (rest *left))))
          (keys-of *held :> *held-keys)
          (loop<- [*keys-left *held-keys :> *cleared]
            (<<if (empty? *keys-left)
              (:> true)
             (else>)
              (first *keys-left :> *held-key)
              (local-transform> [(keypath :to-keep *held-key) NONE>] $$task)
              (continue> (rest *keys-left))))
          (kept! *b2 *t2 *held *k0)))

      ;; 3. write: the rows, and what to keep of them
      (<<batch
        (|all)
        (ops/current-microbatch-id :> *b3)
        (ops/current-task-id :> *t3)
        (local-select> (keypath :todo) $$sent :> *todo)
        (rows-of *b3 *todo :> *rows)
        (loop<- [*rows-left *rows :> *written]
          (<<if (empty? *rows-left)
            (:> true)
           (else>)
            (first *rows-left :> [*row-id *row])
            (local-transform> [(keypath *row-id) (termval *row)] $$rows)
            (continue> (rest *rows-left))))
        (chunks-of *b3 *rows :> *chunks)
        (loop<- [*chunks-left *chunks :> *chunks-kept]
          (<<if (empty? *chunks-left)
            (:> true)
           (else>)
            (first *chunks-left :> [*chunk-key *chunk])
            (local-transform> [(keypath :to-keep *chunk-key) (termval *chunk)] $$task)
            (continue> (rest *chunks-left))))
        (wrote! *b3 *t3 *rows)))))

;; ---- the hand -------------------------------------------------------------

(def ^:dynamic *round* nil)

(defn- steady
  "Takes a function of no arguments that reads from outside. Calls it until it does not throw, for 15 seconds at most. Gives what it gave, or [:read-threw message]."
  [f]
  (loop [n 0]
    (let [r (try [(f)] (catch Throwable t (when (>= n 300) [[:read-threw (u/root-message t)]])))]
      (if r (first r) (do (Thread/sleep 50) (recur (inc n)))))))

(defn- row-of "Takes an id. Gives its row, read from outside, or nil." [id]
  (foreign-select-one (keypath id) (:rows *round*) {:pkey id}))

(defn- all-records
  "Gives every record of the second depot, each with :partition and :offset."
  []
  (let [kept (:kept *round*)]
    (vec (for [p (range (:num-partitions (foreign-object-info kept)))
               :let [{:keys [start-offset end-offset]} (foreign-depot-partition-info kept p)]
               [i r] (map-indexed vector (foreign-depot-read kept p start-offset end-offset))]
           (assoc r :partition p :offset (+ start-offset i))))))

(defn- answers-of
  "Takes the records. Gives each kept answer as {:id :row :partition :offset}."
  [records]
  (vec (for [r records a (:answers r)] (assoc a :partition (:partition r) :offset (:offset r)))))

(defn- strays-of "Takes the records. Gives each kept stray as [id partition offset]." [records]
  (vec (for [r records s (:strays r)] [s (:partition r) (:offset r)])))

(defn- in-one-go!
  "Stops the topology, appends the things, and lets it go on."
  [things]
  (rtest/pause-microbatch-topology! (:ipc *round*) (:module *round*) "gate")
  (doseq [t things] (foreign-append! (:things *round*) t :append-ack))
  (rtest/resume-microbatch-topology! (:ipc *round*) (:module *round*) "gate"))

(defn- scenario
  "Sends the things in one go, waits until each that is no stray has a row
  and `done?` holds of the records, and says what the table and the depot
  hold of each."
  [label things done?]
  (u/say "RUN     ----" label "----")
  (in-one-go! things)
  (let [ids (map :id (remove :stray things))
        strays (set (map :id (filter :stray things)))
        ready (u/wait-until (fn [] (and (every? row-of ids) (done? (all-records)))) 90000)]
    (Thread/sleep 400)
    (let [records (steady all-records)
          answers (group-by :id (answers-of records))]
      (doseq [id ids]
        (let [row (steady #(row-of id)) as (get answers id)]
          (u/say "RESULT " id "· ready:" (boolean ready) "· the table holds:" (pr-str row)
                 "· kept answers:" (count as)
                 (pr-str (mapv (fn [a] [(str "attempt " (:attempt (:row a))) (str "p" (:partition a) "@" (:offset a))]) as))
                 "· each holds the table's row:" (every? #(= row (:row %)) as))))
      (doseq [s (filter (fn [[id _ _]] (contains? strays id)) (strays-of records))]
        (u/say "RESULT  stray" (pr-str s))))))

(defn- kept-count
  "Gives a function of the records: true when the ids have at least n kept answers and the strays at least m kept strays between them."
  [ids n stray-ids m]
  (fn [records]
    (and (<= n (count (filter #(contains? (set ids) (:id %)) (answers-of records))))
         (<= m (count (filter (fn [[id _ _]] (contains? (set stray-ids) id)) (strays-of records)))))))

(defn- watch-one
  "Starts a thread that reads, every millisecond, the row of an id and whether the depot holds a kept answer of it. Gives a promise of [ms when the row was first given, ms when the record was first there]."
  [id]
  (let [out (promise)]
    (doto (Thread. (bound-fn []
                     (loop [row-at nil]
                       (let [row-at (or row-at (when (try (row-of id) (catch Throwable _ nil)) (u/ms)))
                             rec? (try (some #(= id (:id %)) (answers-of (all-records))) (catch Throwable _ nil))]
                         (if rec?
                           (deliver out [row-at (u/ms)])
                           (do (Thread/sleep 1) (recur row-at)))))))
      (.setDaemon true) (.start))
    out))

(defn -main
  "Run M."
  [& _]
  (try
    (u/open-log! "logs/batch-each-task.log")
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc KeepBatch {:tasks 4 :threads 2 :workers 2})
      (binding [*round* (let [m (get-module-name KeepBatch)]
                          {:ipc ipc :module m
                           :things (foreign-depot ipc m "*things")
                           :kept (foreign-depot ipc m "*kept")
                           :rows (foreign-pstate ipc m "$$rows")})]
        (let [m0 (conj (vec (for [i (range 7)] {:k (str "k" (mod i 4)) :id (str "A" i)}))
                       {:k "k3" :id "stray-0" :stray true :n 0})
              seen (watch-one "A0")
              m1 [{:k "k0" :id "B0"} {:k "k1" :id "FAIL-1"} {:k "k2" :id "B2"} {:k "k3" :id "stray-1" :stray true :n 1}]
              m2 [{:k "k0" :id "C0"} {:k "k1" :id "C1"} {:k "k2" :id "C2"}]
              m3 [{:k "same" :id "H1a"} {:k "same" :id "H1b"}
                  {:k "same" :id "stray-3" :stray true :n 3} {:k "same" :id "FAIL-2"}]]
          (scenario "M0 · 7 things and a stray in one batch, no failure" m0 (kept-count (map :id m0) 7 ["stray-0"] 1))
          (let [[row-at record-at] (deref seen 20000 [nil nil])]
            (u/say "SEEN    A0: its row was first given at" (pr-str row-at) "ms and its record was first in the depot at" (pr-str record-at)
                   "ms · the record came" (when (and row-at record-at) (format "%.1f" (- record-at row-at))) "ms after the row"))
          (scenario "M1 · the attempt that writes fails after its writes, and runs again" m1 (kept-count ["B0" "FAIL-1" "B2"] 3 ["stray-1"] 1))
          (reset! throw-after-keep true)
          (scenario "M2 · the run that closes a batch fails after its appends, and runs again" m2 (kept-count ["C0" "C1" "C2"] 4 [] 0))
          (scenario "M3 · four things of one partition, two to a batch: the run that closes the first two holds a stray and a thing that fails after its writes"
                    m3 (kept-count ["H1a" "H1b" "FAIL-2"] 5 ["stray-3"] 1))
          (let [end (fn [] (mapv #(:end-offset (foreign-depot-partition-info (:kept *round*) %)) (range 4)))
                before (steady end)]
            (Thread/sleep 2500)
            (u/say "QUIET   the end of each partition of the depot:" (pr-str before) "· 2.5 s later, with nothing sent:" (pr-str (steady end))))
          (let [records (steady all-records)
                answers (answers-of records)
                ids (distinct (map :id answers))
                differ (remove (fn [a] (= (:row a) (steady #(row-of (:id a))))) answers)
                sent (map :id (remove :stray (concat m0 m1 m2 m3)))]
            (u/say "WHOLE   records in the depot:" (count records) "· kept answers:" (count answers) "of" (count ids) "ids"
                   "· kept answers that hold another row than the table's:" (count differ) (pr-str (take 4 differ))
                   "· rows with no kept answer:" (pr-str (remove (set ids) sent))
                   "· kept strays:" (pr-str (frequencies (map first (strays-of records)))))
            (u/say "WHOLE   how many ids have 1, 2, 3 ... kept answers:" (pr-str (into (sorted-map) (frequencies (vals (frequencies (map :id answers)))))))
            (u/say "WHOLE   attempts of the batches that had more than one:" (pr-str (into (sorted-map) (filter #(< 1 (val %)) @attempts)))
                   "· forced throws:" (pr-str @throws))
            (u/say "COST    the appends of one task in one closing, [records ms]:" (pr-str (mapv (fn [[n ms]] [n (/ (Math/round (* 100.0 ms)) 100.0)]) @keep-times)))))))
    (catch Throwable t
      (u/say "STOPPED" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
