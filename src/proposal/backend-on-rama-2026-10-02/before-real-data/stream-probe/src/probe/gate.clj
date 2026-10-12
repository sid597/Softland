(ns probe.gate
  "The module of the stream probe, and what its events call.

  The module stands in for one shape and proposes nothing: one depot of
  things that a client appends to, partitioned by the thing's `:k`; one
  stream topology whose source is `{:start-from :beginning :retry-mode
  :all-after}`; an event that stays on its task, reads three rows, writes
  three rows of the topology's own PState, and appends one record to a
  second depot, `*log`, declared `:disallow`. No topology of the module
  consumes `*log`.

  Each thing carries its own plan, so that one module serves every run:

    :k             the key that the depot partitions by (one key for a task)
    :id            the thing's own name; its row is kept under it
    :place         :after   the rows are written, then the record is appended
                   :before  the record is appended, then the rows are written
                   :none    no append at all
    :boundary      true puts `(|direct (ops/current-task-id))` right before
                   the append
    :ack           the ack level of the append: :append-ack, :ack or nil
    :pause         {point ms}: the event sleeps on the task's thread there
    :pause-first   the pauses hold only for attempts up to this number
    :fail-at       a point; the event throws there ...
    :fail-first    ... on attempts up to this number
    :commit-after  true puts a commit boundary after the write and the
                   append, and the point :after-commit behind it
    :wait-first    on attempts up to this number the event waits, without
                   holding the thread, for a future that the run completes
    :loud          true makes the event say a line at each point, on its
                   first two attempts only (`:loud-first` changes the two)
    :label         a word under which the event's times are kept

  The points of an event, in order: :start, :mid (between the write and the
  append, whichever comes first), :end (after both), :after-commit,
  :after-wait.

  What the JVM keeps beside Rama, as the memory of this process: how often
  the event of each id began, when each point was passed, how often each
  task's global was made ready (a second time means that Rama started the
  worker again), the futures, and the times. The in-process cluster is one
  JVM, so this memory survives a worker's restart; on a real cluster it
  would not."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.ops :as ops]
            [probe.util :as u])
  (:import [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent CompletableFuture ConcurrentHashMap ConcurrentLinkedQueue]))

;; ---- what the JVM keeps ---------------------------------------------------

(def stream-timeout-seconds
  "What the module sets `topology.stream.timeout.seconds` to at launch, or nil
  for Rama's own value. A run changes it before it launches the module."
  nil)

(defonce ^{:doc "id → how many times the event of that thing began."}
  attempts (atom {}))

(defonce ^{:doc "[id attempt point] → the ms at which that attempt passed that point."}
  marks (ConcurrentHashMap.))

(defonce ^{:doc "task → how many times its task global was made ready."}
  prepared (atom {}))

(defonce ^{:doc "id → the CompletableFuture that an event of that thing waits for."}
  futures (ConcurrentHashMap.))

(defonce ^{:doc "[label event-nanos append-nanos], one for each event that carried a label and ended."}
  times (ConcurrentLinkedQueue.))

(defn mark
  "Takes an id, an attempt and a point. Gives the ms at which it was passed, or nil."
  [id attempt point]
  (.get ^ConcurrentHashMap marks [id attempt point]))

(defn restarts
  "Gives how many times a task global was made ready beyond the first, summed over the tasks."
  []
  (reduce + (map #(max 0 (dec %)) (vals @prepared))))

;; ---- the task global: tells when a worker was started again ---------------

(deftype Memory [^java.util.HashMap held]
  TaskGlobalObject
  (prepareForTask [this task-id context]
    (let [n (get (swap! prepared update task-id (fnil inc 0)) task-id)]
      (when (and (> n 1) (zero? task-id))
        (u/say "INSIDE  the task global of task 0 was made ready again, time" n
               ": Rama started the worker again"))))
  (close [this])
  java.io.Serializable)

;; ---- what the event calls -------------------------------------------------

(defn- where
  "Takes a point and a thing. Gives the point in words."
  [point thing]
  (case point
    :start "at its start"
    :mid (case (:place thing)
           :before "after the append, before the write"
           :after "after the write, before the append"
           "after the write")
    :end (if (= :none (:place thing)) "after the write" "after the write and the append")
    :after-commit "in the tree's next event, behind a commit boundary"
    :after-wait "after its wait"
    (name point)))

(defn loud?
  "Takes a thing and the number of an attempt. Gives true when that attempt says its lines."
  [thing n]
  (and (:loud thing) (<= n (:loud-first thing 2))))

(defn worker-word
  "Gives the worker's own word out of the name of the thread that calls it, as \"worker 1a2b3c4d\"."
  []
  (if-let [[_ w] (re-find #"Worker (\S+)" (.getName (Thread/currentThread)))]
    (str "worker " w)
    (.getName (Thread/currentThread))))

(defn begin!
  "The event calls it first. Takes the task global, the thing and the task.
  Counts the attempt. Gives the note of the attempt: its number, the
  nanosecond at which it began, and the task."
  [memory thing task]
  (let [id (:id thing)
        n (get (swap! attempts update id (fnil inc 0)) id)]
    (.put ^ConcurrentHashMap marks [id n :begin] (u/ms))
    (when (loud? thing n)
      (u/say "INSIDE " id "attempt" n "begins on task" task (str "(" (worker-word) ")")))
    {:attempt n :t0 (System/nanoTime) :task task}))

(defn point!
  "The event calls it at each point. Takes the point, the thing and the note
  of the attempt. Keeps the time. Sleeps when the thing asks for a pause
  there. Throws when the thing asks for a failure there on this attempt."
  [point thing note]
  (let [id (:id thing) n (:attempt note)]
    (.put ^ConcurrentHashMap marks [id n point] (u/ms))
    (when (and (loud? thing n) (not= :start point))
      (u/say "INSIDE " id "attempt" n "is" (where point thing)))
    (when-let [pause-ms (get (:pause thing) point)]
      (when (<= n (:pause-first thing Long/MAX_VALUE))
        (Thread/sleep (long pause-ms))))
    (when (and (= point (:fail-at thing)) (<= n (:fail-first thing 0)))
      (.put ^ConcurrentHashMap marks [id n :threw] (u/ms))
      (u/say "INSIDE " id "attempt" n "THROWS on purpose" (where point thing))
      (throw (ex-info "probe: forced failure" {:id id :attempt n :point point})))
    nil))

(defn next-count
  "Takes what the row \"count\" held. Gives the next number."
  [held]
  (inc (or held 0)))

(defn make-row
  "Takes the thing, the note of the attempt and the count. Gives the row to write under the thing's id."
  [thing note n]
  {:id (:id thing) :attempt (:attempt note) :count n})

(defn make-record
  "Takes the thing, the note of the attempt, the count, and the row that the
  id held before. Gives the record to append to the second depot."
  [thing note n row-before]
  {:id (:id thing) :attempt (:attempt note) :count n
   :row-before (when row-before (:attempt row-before))})

(defn wait?
  "Takes the thing and the note of the attempt. Gives true when this attempt must wait for its future."
  [thing note]
  (<= (:attempt note) (:wait-first thing 0)))

(defn the-future
  "Takes an id. Gives the future of that id, made on first asking."
  ^CompletableFuture [id]
  (.computeIfAbsent ^ConcurrentHashMap futures id
                    (reify java.util.function.Function
                      (apply [_ _] (CompletableFuture.)))))

(defn waiting!
  "The event calls it right before it waits. Takes the thing and the note. Keeps the time."
  [thing note]
  (.put ^ConcurrentHashMap marks [(:id thing) (:attempt note) :waiting] (u/ms))
  (when (loud? thing (:attempt note))
    (u/say "INSIDE " (:id thing) "attempt" (:attempt note) "waits for its future; the task's thread is free"))
  nil)

(defn end!
  "The event calls it last. Takes the thing, the note of the attempt and the
  nanoseconds that the append took inside the event (0 with no append). Keeps
  the times under the thing's label."
  [thing note append-nanos]
  (let [t1 (System/nanoTime) id (:id thing) n (:attempt note)]
    (.put ^ConcurrentHashMap marks [id n :done] (u/ms))
    (when-let [label (:label thing)]
      (.add ^ConcurrentLinkedQueue times [label (- t1 (long (:t0 note))) append-nanos]))
    (when (loud? thing n)
      (u/say "INSIDE " id "attempt" n ": the event's code is done"))
    nil))

;; ---- the module -----------------------------------------------------------

(defmodule GateProbe [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-depot setup *log :disallow)
  (declare-object setup *memory (Memory. (java.util.HashMap.)))
  (when stream-timeout-seconds
    (set-launch-topology-dynamic-option! setup "gate" "topology.stream.timeout.seconds" stream-timeout-seconds))
  (let [s (stream-topology topologies "gate")]
    (declare-pstate s $$rows {String Object})
    (<<sources s
      (source> *things {:start-from :beginning :retry-mode :all-after} :> *thing)
      (ops/current-task-id :> *task)
      (begin! *memory *thing *task :> *note)
      (identity *thing :> {:keys [*id *place *boundary *ack *commit-after]})
      (point! :start *thing *note)

      ;; the reads: three rows of this task
      (local-select> (keypath *id) $$rows :> *row-before)
      (local-select> (keypath "count") $$rows :> *count-before)
      (local-select> (keypath "last") $$rows :> *last-before)
      (next-count *count-before :> *n)
      (make-row *thing *note *n :> *row)
      (make-record *thing *note *n *row-before :> *record)

      (<<cond
        ;; the record is appended, then the rows are written
        (case> (= :before *place))
        (<<if *boundary
          (|direct (ops/current-task-id)))
        (System/nanoTime :> *a0)
        (depot-partition-append! *log *record *ack)
        (System/nanoTime :> *a1)
        (- *a1 *a0 :> *append-nanos)
        (point! :mid *thing *note)
        (local-transform> [(keypath *id) (termval *row)] $$rows)
        (local-transform> [(keypath "count") (termval *n)] $$rows)
        (local-transform> [(keypath "last") (termval *id)] $$rows)

        ;; the rows are written, then the record is appended
        (case> (= :after *place))
        (local-transform> [(keypath *id) (termval *row)] $$rows)
        (local-transform> [(keypath "count") (termval *n)] $$rows)
        (local-transform> [(keypath "last") (termval *id)] $$rows)
        (<<if *boundary
          (|direct (ops/current-task-id)))
        (point! :mid *thing *note)
        (System/nanoTime :> *a0)
        (depot-partition-append! *log *record *ack)
        (System/nanoTime :> *a1)
        (- *a1 *a0 :> *append-nanos)

        ;; no append
        (default>)
        (local-transform> [(keypath *id) (termval *row)] $$rows)
        (local-transform> [(keypath "count") (termval *n)] $$rows)
        (local-transform> [(keypath "last") (termval *id)] $$rows)
        (identity 0 :> *append-nanos))

      (point! :end *thing *note)

      (<<if *commit-after
        (|direct (ops/current-task-id))
        (point! :after-commit *thing *note))

      (wait? *thing *note :> *wait?)
      (<<if *wait?
        (waiting! *thing *note)
        (the-future *id :> *future)
        (completable-future> *future :> *waited)
        (point! :after-wait *thing *note))

      (end! *thing *note *append-nanos))))

;; ---- what the runs share --------------------------------------------------

(def variants
  "The twelve ways of the append: its place, the commit boundary before it, and its ack level."
  (vec (for [place [:after :before] boundary [false true] ack [:append-ack :ack nil]]
         {:place place :boundary boundary :ack ack})))

(defn variant-name
  "Takes a way of the append. Gives it in words."
  [{:keys [place boundary ack]}]
  (if (= :none place)
    "no append"
    (str (if (= :after place) "write, then append" "append, then write")
         " · " (if boundary "boundary before the append" "no boundary")
         " · " (if ack (str ack) "no ack"))))

(defn variant-tag
  "Takes a way of the append. Gives a short word for an id."
  [{:keys [place boundary ack]}]
  (if (= :none place)
    "none"
    (str (if (= :after place) "wa" "aw") (if boundary "B" "-")
         (case ack :append-ack "p" :ack "k" "0"))))

(defn log-end
  "Takes the client of the second depot and a partition. Gives the partition's end offset."
  [log p]
  (:end-offset (foreign-depot-partition-info log p)))

(defn read-range
  "Takes a depot client, a partition, a start offset and an end offset. Gives
  the records in between as a vector, read 500 at a time."
  [depot p from to]
  (loop [at from out []]
    (if (>= at to)
      out
      (let [upto (min to (+ at 500))]
        (recur upto (into out (foreign-depot-read depot p at upto)))))))
