(ns probe.gate
  "The stand-in gate of the probe of 13 October 2026, and what the JVM keeps
  about it while it runs.

  The gate is a stream topology with no partitioner, no append and no wait
  inside an event. One event reads rows on its task, decides by a pure
  function, writes rows on its task, and ends:

    (a) it reads `$$answers` at the act's id. If an answer is there it does
        nothing: a repeat, or Rama running the record again.
    (b) it reads the row at [layer key]. The act is accepted when its
        `:replaces` equals the id that the row holds (nil for no row), and
        refused otherwise.
    (c) its number is the larger of `:time` x 65536 and the layer's last
        number + 1. It writes `$$last`, the answer, and for an accept the row.

  Two modules run the one gate. `First` owns the depot `*acts`, declared
  `(hash-by :layer)`, and decides each act as it arrives. `Again` mirrors
  that depot and runs the same gate over it from its beginning.

  The failures are put in from outside the gate's rules: `plan` holds, for
  the id of an act, what the FIRST attempt of that act does in module First
  only (sleep on the task's thread, throw). Module Again never reads `plan`.

  The cluster is Rama's in-process one, so the code of an event and the code
  that watches it run in one JVM. Each attempt leaves notes in `events`, with
  one clock. A note is not a row: a note of a failed attempt stays, its rows
  do not.

  A probe, not a design: no table, name or form here is a proposal for the
  store."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.walk :as walk]
            [com.rpl.rama.ops :as ops])
  (:import [com.rpl.rama.integration TaskGlobalObject]
           [java.util.concurrent ConcurrentLinkedQueue]))

;; ---- what a run sets before it launches a module ---------------------------

(defonce ^{:doc "What the next launch of module First uses.
  :timeout     seconds for `topology.stream.timeout.seconds`, or nil for Rama's own value
  :retry-mode  :all-after, :individual or :none, the retry mode of the gate's source in First
  :launch      what a run launches both modules with"}
  settings
  (atom {:timeout nil :retry-mode :all-after :launch {:tasks 4 :threads 4}}))

;; ---- what the JVM keeps ----------------------------------------------------

(defonce ^{:doc "The nanosecond from which every note's time is counted. A run sets it anew for each cluster."}
  clock-start (atom (System/nanoTime)))

(defn ms
  "Gives the milliseconds since `clock-start`, as a double."
  []
  (/ (double (- (System/nanoTime) (long @clock-start))) 1e6))

(defonce ^{:doc "id → what the first attempt of that act does in module First:
  {:sleep-ms n :sleep-at :start|:end} sleeps on the task's thread there;
  {:throw-at :start|:end} throws there, after the sleep if both stand at one point;
  {:bad-write true} makes the answer that the attempt writes one that Rama cannot write;
  {:end-fn f} calls f, a function of no arguments, at the end of the event's code."}
  plan (atom {}))

(defonce ^{:doc "[module-word id] → how many attempts of that act began in that module."}
  attempts (atom {}))

(defonce ^{:doc "The notes of the attempts, in the order in which they were made: maps with :t :word :id :attempt :what and more."}
  events (ConcurrentLinkedQueue.))

(defonce ^{:doc "When false, no note is kept. The cost runs set it false."}
  noting? (atom true))

(defonce ^{:doc "[module-word task] → how many times the task global of that module was made ready on that task."}
  prepared (atom {}))

(defonce ^{:doc "[module-word layer] → the task on which an act of that layer began."}
  task-of (atom {}))

(defn reset-state!
  "Empties everything the JVM keeps and starts the clock anew. A run calls it before each cluster."
  []
  (reset! clock-start (System/nanoTime))
  (reset! plan {})
  (reset! attempts {})
  (.clear ^ConcurrentLinkedQueue events)
  (reset! prepared {})
  (reset! task-of {})
  (reset! noting? true))

(defn note!
  "Takes a module word, an act, the number of the attempt, a word for what happened, and a map of more. Keeps one note."
  [word act attempt what more]
  (when @noting?
    (.add ^ConcurrentLinkedQueue events
          (merge {:t (ms) :word word :id (:id act) :layer (:layer act) :attempt attempt :what what
                  :thread (.getName (Thread/currentThread))}
                 more)))
  nil)

(defn notes
  "Gives the notes so far as a vector, in the order in which they were made."
  []
  (vec (.toArray ^ConcurrentLinkedQueue events)))

(defn restarts
  "Takes a module word. Gives how many times its task global was made ready beyond the first, summed over the tasks."
  [word]
  (reduce + (for [[[w _] n] @prepared :when (= w word)] (max 0 (dec n)))))

;; ---- the task global: tells when a worker was started again ----------------

(deftype Memory [word]
  TaskGlobalObject
  (prepareForTask [this task-id context]
    (let [n (get (swap! prepared update [word task-id] (fnil inc 0)) [word task-id])]
      (when (and @noting? (> n 1))
        (.add ^ConcurrentLinkedQueue events
              {:t (ms) :word word :id nil :attempt nil :what :task-global-ready-again :task task-id :time-n n}))))
  (close [this])
  java.io.Serializable)

;; ---- what the event calls --------------------------------------------------

(defn- act-out!
  "Takes a module word, an act, the number of the attempt and a point. In
  module First, on the first attempt, does what `plan` holds for the act at
  that point: the sleep on the calling thread, then the throw."
  [word act attempt point]
  (when (and (= "first" word) (= 1 attempt))
    (when-let [p (get @plan (:id act))]
      (when (and (:sleep-ms p) (= point (:sleep-at p)))
        (note! word act attempt :sleeps {:at point :for-ms (:sleep-ms p)})
        (Thread/sleep (long (:sleep-ms p)))
        (note! word act attempt :wakes {:at point}))
      (when (and (= point :end) (:end-fn p))
        ((:end-fn p)))
      (when (= point (:throw-at p))
        (note! word act attempt :throws {:at point})
        (throw (ex-info "probe: forced failure" {:id (:id act) :attempt attempt :at point}))))))

(defonce ^{:doc "The dynamic vars bound on the thread of an event whose act carried `:look true`: [name, the start of its value as text], sorted."}
  looked (atom nil))

(defn- look!
  "Keeps in `looked` the name of each dynamic var bound on the calling thread, with the first 80 characters of its value."
  []
  (reset! looked
          (vec (sort (for [[v x] (get-thread-bindings)]
                       [(str (.-ns ^clojure.lang.Var v) "/" (.-sym ^clojure.lang.Var v))
                        (let [s (try (pr-str x) (catch Throwable _ "?"))] (subs s 0 (min 80 (count s))))])))))

(defn begin!
  "The event calls it first. Takes the module word, the act and the task.
  Counts the attempt and gives its number."
  [word act task]
  (let [k [word (:id act)]
        n (get (swap! attempts update k (fnil inc 0)) k)]
    (when @noting?
      (swap! task-of assoc [word (:layer act)] task))
    (when (:look act) (look!))
    (note! word act n :begins {:task task})
    (act-out! word act n :start)
    n))

(defn decide
  "The gate's rule, pure. Takes the row that [layer key] holds or nil, the
  layer's last number or nil, and the act. Gives the verdict, the act's
  number, and the row to write for an accept."
  [row last act]
  (let [verdict (if (= (:replaces act) (:id row)) :accepted :refused)
        n (max (* (long (:time act)) 65536) (inc (long (or last 0))))]
    {:verdict verdict :n n :new-row {:val (:val act) :id (:id act) :n n}}))

(defn answer-of
  "Takes the module word, the act, a verdict, a number and the number of the
  attempt. Gives the answer to write under the act's id. In module First, on
  the first attempt of an act for which `plan` holds `:bad-write true`, the
  answer also carries a value that Rama cannot write: a way to make the write
  itself fail, with no throw in the probe's own code."
  [word act verdict n attempt]
  (cond-> {:verdict verdict :n n :attempt attempt}
    (and (= "first" word) (= 1 attempt) (:bad-write (get @plan (:id act))))
    (assoc :junk (Object.))))

(defn decided!
  "The event calls it when it has decided, before it writes. Keeps a note."
  [word act attempt verdict n]
  (note! word act attempt :decides {:verdict verdict :n n}))

(defn found-answer!
  "The event calls it when the act's answer was there already. Keeps a note."
  [word act attempt answer]
  (note! word act attempt :finds-its-answer {:answer answer}))

(defn end!
  "The event calls it last, after its writes."
  [word act attempt]
  (act-out! word act attempt :end)
  (note! word act attempt :ends {}))

;; ---- the gate --------------------------------------------------------------

(def ^:private gate-form
  "The form of a function that adds the gate to a module. FN-NAME, WORD and
  OPTS are replaced by `defgate`: the function's name, the module word that
  the event passes to its notes, and the options of the source."
  '(defn FN-NAME
     [topologies]
     (let [s (stream-topology topologies "gate")]
       (declare-pstate s $$rows {Object Object})
       (declare-pstate s $$answers {String Object})
       (declare-pstate s $$last {Object Long})
       (<<sources s
         (source> *acts OPTS :> *act)
         (ops/current-task-id :> *task)
         (begin! WORD *act *task :> *attempt)
         (identity *act :> {:keys [*layer *id *key]})
         (local-select> (keypath *id) $$answers :> *answer)
         (<<if (some? *answer)
           (found-answer! WORD *act *attempt *answer)
           (else>)
           (vector *layer *key :> *row-key)
           (local-select> (keypath *row-key) $$rows :> *row)
           (local-select> (keypath *layer) $$last :> *last)
           (decide *row *last *act :> {:keys [*verdict *n *new-row]})
           (decided! WORD *act *attempt *verdict *n)
           (local-transform> [(keypath *layer) (termval *n)] $$last)
           (answer-of WORD *act *verdict *n *attempt :> *new-answer)
           (local-transform> [(keypath *id) (termval *new-answer)] $$answers)
           (<<if (= :accepted *verdict)
             (local-transform> [(keypath *row-key) (termval *new-row)] $$rows)))
         (end! WORD *act *attempt)))))

(defmacro defgate
  "Defines a function of the module's `topologies` that adds the gate. Takes
  the function's name, the module word, and the options of the source as a
  literal map."
  [fn-name word opts]
  (walk/postwalk-replace {'FN-NAME fn-name 'WORD word 'OPTS opts} gate-form))

(defgate gate-first-all-after "first" {:start-from :beginning :retry-mode :all-after})
(defgate gate-first-individual "first" {:start-from :beginning :retry-mode :individual})
;; Rama refuses `:start-from` with the retry mode :none ("start-from option
;; disallowed for streaming with retry-mode = none", run 14 October), so this
;; one starts from the depot's end, Rama's own value. First is launched on an
;; empty depot, so the end is the beginning.
(defgate gate-first-none "first" {:retry-mode :none})
(defgate gate-again "again" {:start-from :beginning :retry-mode :all-after})

;; ---- the two modules -------------------------------------------------------

(defmodule First [setup topologies]
  (declare-depot setup *acts (hash-by :layer))
  (declare-object setup *memory (Memory. "first"))
  (when-let [t (:timeout @settings)]
    (set-launch-topology-dynamic-option! setup "gate" "topology.stream.timeout.seconds" t))
  (case (:retry-mode @settings)
    :all-after (gate-first-all-after topologies)
    :individual (gate-first-individual topologies)
    :none (gate-first-none topologies)))

(defmodule Again [setup topologies]
  (mirror-depot setup *acts "probe.gate/First" "*acts")
  (declare-object setup *memory-again (Memory. "again"))
  (gate-again topologies))
