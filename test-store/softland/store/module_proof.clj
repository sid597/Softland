(ns softland.store.module-proof
  "The proof of the store's module on Rama's in-process cluster: the two
  gates, their records, and the two reads. One command, from the
  repository root:

    clojure -M:store-module -m softland.store.module-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The proof plays the door by hand and does not use `softland.store.door`:
  the store stays proven without the door, and the hand stays the proof that
  anything can append to a depot without it. The hand is a few functions of
  this namespace, in the order of one act: it reads the depot of the layer;
  makes the act-id, with a random UUID; writes a time on the thing, as the
  door does; appends the thing, each value plain as a maker gives it; reads
  the answer by act-id, with the act-id's layer as the routing id, for both
  depots; and compares the answer's digest with the digest of what it sent.
  The time is a reading of the proof's own clock, or a time that a check
  chose: a check of the time chooses it, in both gates, so that the time
  that an answer and a row hold is a number that the proof wrote out. A
  stamp holds no time: the proof works out each stamp that it holds from
  the order in which it sent the things of a layer. The hand
  also plays a reader of the tables, and does not use the read path
  (`softland.store.read-path`), which has a proof of its own: it selects a
  fact's row, the row of a layer's marked keys, and a run of the facts by
  value, with the keys that `softland.store.record.rows` gives, and reads a
  row back with that namespace's functions. An error from the append is not
  an answer. It keeps each thing that it sent, so that a repeat is the same
  thing byte for byte.

  The two depots. For a streaming layer the hand appends with the ack level
  :ack and reads the answer once: the ack waits for the gate. For the base,
  the one microbatch layer, no ack carries an answer: the hand appends with
  :append-ack, waits with Rama's test tool until the gate has processed
  each thing that it appended, and reads the answer once. It sends things of
  the base in three ways: one alone; several in a row, with no wait for the
  gate between two of them, as a maker sends her acts; and several while
  the microbatch topology is paused, so that one batch takes them all.

  Where the hand reads the base. The row of a fact and a row of the facts
  by value are on every task. The hand reads one on the base's task; on
  each of the four tasks, with a key that hashes to the task; and, at
  chosen moments of the run, in the whole table on every task, which it
  holds against itself task by task. An answer, a layer's last stamp and
  the digest of a stray are on one task. Which task is a layer's is told
  from the depot's log: the partition that holds the layer's things.

  The checks of the base. The streaming gate's groups are their measure:
  what the proof shows of a streaming layer it shows of the base, group for
  group. Two things are held once, for the streaming depot: the kinds of
  value that Rama carries on a depot, and what the two reads give for a
  thing that is no id. Beside them stand the checks of what only the base
  has: the line
  (acts that need each other's rows, appended with no wait for the gate,
  and a chain of 100 acts in one batch); the copies on every task; the
  stamp by the proof's own count for each thing that the gate decided, in
  the depot's order; a mark and the facts after it in one batch; and an
  attempt of a batch that fails and runs again.

  State. The proof starts two in-process clusters, one after the other in
  one JVM, each with four tasks and two workers, launches the module `Store`
  in each, and closes each; a cluster's files are temporary and are Rama's.
  Every check but two groups runs on the first cluster, on the functions
  as the store defines them: nothing is replaced in the JVM while it runs.
  The two groups that make a gate fail run on the second cluster: an
  attempt of a batch of the microbatch gate that fails in the middle of the
  batch and runs again, and an event of the streaming gate that fails
  before its first write and runs again. For that cluster alone `-main`
  puts two functions of this namespace into the vars of two pure functions
  of the store that the walk of one thing calls, before the launch, and
  puts the store's own back when the cluster is closed, in a `finally`:
  `writes` of `softland.store.gate.decision`, and `stray-reason` of
  `softland.store.door.declaration`. Each gives what the store's function
  gives, but for what a group asked: `writes` throws once for one act of
  each of the two groups; to each act of the failing batch before the
  throw it adds one row that no act writes; and to one act more, in an
  attempt that commits, such a row too. The module's source holds no
  hook for any of this. The walk reads each var at each call, because this
  namespace loads the store's module, and with it the walk, while the vars
  hold the store's own functions.

  The proof owns seven atoms: the lines that it showed; the things that it
  sent to each depot; the first answers of the streaming gate, in the order
  sent; what later checks come back to; and, for the second cluster, the
  batch that is to fail, with each call that was noted for it, and the
  events of the streaming gate that it watches, with the one that is to
  fail and each call that was noted for them. `-main` empties all but the
  shown lines between the two clusters: Rama counts the things that a gate
  processed from the launch of a cluster. The notes are the memory of this
  process. They hold a first attempt's calls through the restart of a
  worker only because the in-process cluster is one JVM. The proof reads
  the clock for the time that the hand writes on a thing when no check
  chose one, and to end a loop that watches a table.

  Approach. The checks run in one order on one cluster, and later checks
  build on the layers, the facts and the permissions that earlier ones
  made. The checks of the streaming gate run first. The base is made after
  the check that sends its making act to the streaming gate, because that
  check holds the word that the streaming gate gives an act for the base
  while the base is not made. Each check says what must be read back, and
  reads it back through the module's own queries. A check that reads a fact
  back holds the row's text against a text that the proof writes out, and
  the log's value against the value that the hand sent: never against a
  text that the store's own function makes from the same value. Where it
  shows a row, it selects the row from a gate's PState, with a partitioning
  key. Such a select goes round the two reads. Near the end the proof
  starts the module again in the cluster, by Rama's module update with the
  unchanged module, and reads both records once more; Rama then prints some
  lines of its own log, marked ERROR, that are no failed checks. So it does
  on the second cluster for each of the two throws that the proof makes:
  Rama says that it treats the throw as fatal for the worker, and starts
  the worker again.

  What rests on a time or a wait. The hand's wait for the microbatch gate
  is Rama's test tool, which counts the records that the gate processed
  and throws at its own time limit. On the first cluster two loops watch a
  table until a row shows, each for 30 seconds at most: for the first
  sight of an answer's row, and for the new row of a place that is
  replaced, on every task. A call that asks the cluster for a name is
  given ten seconds. On the second cluster the answers of the batch that
  fails, of the act behind it and of the event that fails are read in a
  loop for 60 seconds at most, and the stand-in sleeps 300 ms before the
  throw in the batch. No check holds a number of milliseconds.

  What the proof does not reach, for the base.

    - A killed process. The throw that the proof makes restarts a worker
      inside one JVM. No process is killed, and no row has to come back
      from a disk.
    - Several machines. Each cluster is one JVM on one machine.
    - A failure while a batch commits. The forced failure is in the walk,
      before any task commits. Of the time in which one task has committed
      a batch and another has not, the proof holds nothing: the rounds of
      selects of a replaced place count how often they saw it, and no check
      rests on that number.
    - A module with another number of tasks. Both clusters have four.
    - A yield in the walk. Nothing looks for one at run time. The proof
      holds it only by the chain of 100 acts in one batch, each of which
      names the fact of the act before it: an act decided out of the
      depot's order would be refused as stale.
    - A batch from inside. That the things appended while the topology is
      paused fall into one batch is Rama's test docs'. The proof sees that
      none of them had an answer while it was paused; and on the second
      cluster the notes show that the failed attempt decided 21 acts of 40
      and the retry all 40.
    - A second module over the same two depots. That is the proof of the
      run again (`softland.store.run-again-proof`).
    - A microbatch layer beside the base. No layer but the base starts
      with the microbatch depot. So the second layer in the check of two
      layers of one task is a layer of the other gate, whose acts this gate
      refuses: it has stamps and answers there, and no fact.

  What else it does not reach is in the folder's README."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- check
  "Checks that what was observed is what must be, and prints the check's
  sentence after PASSED or FAILED, so that a run lists what it checked. Gives
  what was observed."
  [what expected observed]
  (println (if (is (= expected observed) what) "PASSED" "FAILED") what)
  observed)

(defn- content
  "Takes a value. Gives it with each run of bytes as the vector of its
  bytes, so that two rows are compared by their content."
  [x]
  (cond (bytes? x) (vec x)
        (map? x) (into {} (map (fn [[k v]] [k (content v)])) x)
        (sequential? x) (mapv content x)
        :else x))

;; ------------------------------------------------------------------ the hand

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(def ^:dynamic ^:private *hand*
  "What the proof's hand holds of a cluster: the cluster itself, the name
  of the module, the two depots of acts, the two queries and the four
  PStates of each gate; and `:checks`, which says which of the proof's two
  clusters it is. `-main` binds it for the run on each cluster."
  nil)

(defonce ^{:private true
           :doc "Each thing that the hand appended to the streaming depot, in the
  order sent: what the depot's log is held against."}
  things-sent (atom []))

(defonce ^{:private true
           :doc "The first answer that the hand read for each act-id of a streaming
  layer, in the order sent: {:act-id :layer :stamp}. It holds each thing
  that the streaming gate decided for the hand, in the order decided: what
  the check of the stamps goes through."}
  first-answers (atom []))

(defonce ^{:private true
           :doc "What one group of checks hands to a later one, by name: results,
  the entities and the task of the base, and rows and answers as they were
  before the module is started again."}
  kept (atom {}))

(defonce ^{:private true
           :doc "Each thing that the hand appended to the microbatch depot, in the
  order sent: its count is what the wait for the gate waits for, and the
  depot's log is held against it."}
  base-things-sent (atom []))

(defn- open-hand
  "Takes the cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc        ipc
     :streaming-depot (rama/foreign-depot ipc m "*streaming-depot")
     :answer     (rama/foreign-query ipc m "answer-by-act-id")
     :depot-of-layer (rama/foreign-query ipc m "depot-of-layer")
     :answers-ps (rama/foreign-pstate ipc m "$$streaming-answers")
     :facts-ps   (rama/foreign-pstate ipc m "$$streaming-facts")
     :by-value-ps (rama/foreign-pstate ipc m "$$streaming-facts-by-value")
     :task-ps    (rama/foreign-pstate ipc m "$$streaming-task")
     :module     m
     :mb-depot      (rama/foreign-depot ipc m "*microbatch-depot")
     :mb-answers-ps (rama/foreign-pstate ipc m "$$microbatch-answers")
     :mb-facts-ps   (rama/foreign-pstate ipc m "$$microbatch-facts")
     :mb-by-value-ps (rama/foreign-pstate ipc m "$$microbatch-facts-by-value")
     :mb-task-ps    (rama/foreign-pstate ipc m "$$microbatch-task")}))

(defn- depot-of-layer
  "Reads the depot of a layer, or nil for a layer that is not made. The hand
  tests what it passes before it calls, and gives nil itself for a thing
  that is no id."
  [layer]
  (when (record/depot-read layer)
    (rama/foreign-invoke-query (:depot-of-layer *hand*) layer)))

(defn- answer-of
  "Reads the answer under an act-id, or nil for nothing yet: one call of
  the query. The hand passes the act-id's routing id as the first argument,
  which is the act-id's layer for both depots, and tests the act-id before
  it calls."
  [act-id]
  (let [routing-id (record/routing-id act-id)]
    (when (record/answer-read-by-routing-id routing-id act-id)
      (rama/foreign-invoke-query (:answer *hand*) routing-id act-id))))

(defn- new-act-id
  "Makes an act-id for a layer: the layer, its depot as the module gives it,
  or the depot that the layer starts with when it is not made, and a random
  UUID."
  [layer]
  [layer (or (depot-of-layer layer) (first-act/depot-at-making layer)) (UUID/randomUUID)])

(defn- thing
  "An act as the door sends it: the maker's parts, and the door's three: the
  act-id, the depot, and the time, under the declaration's name for it
  (`softland.store.door.declaration/time-part`). The time is a reading of
  the proof's clock, taken here, as the door takes its server's. `more` is
  merged in last, for the optional parts, for faults, and for a time that a
  check chose."
  ([layer who permission facts] (thing layer who permission facts {}))
  ([layer who permission facts more]
   (let [act-id (new-act-id layer)]
     (merge {:act-id act-id :depot (nth act-id 1) declaration/time-part (System/currentTimeMillis)
             :version 1 :who who :layer layer :permission permission :session :proof-session
             :facts (vec facts)}
            more))))

(defn- at-time
  "Takes a thing and a time in milliseconds since 1970. Gives the thing with
  that time on it, in place of the time that the hand wrote."
  [x millis]
  (assoc x declaration/time-part millis))

(defn- time-on
  "The time that a thing holds, as the hand wrote it, or nil for a thing
  that is no map or holds none."
  [x]
  (when (map? x) (get x declaration/time-part)))

(def ^:private chosen-time
  "The time that the hand writes on the things of the checks of the time,
  in milliseconds since 1970: 1,700,000,000,000, a day in November 2023. The
  proof chose it and reads it from no clock, so each time that a row or an
  answer of those checks holds is a number that the proof wrote out."
  1700000000000)

(def ^:private year-3084
  "A time far ahead of every clock, which the hand writes on an act: 2^45
  milliseconds since 1970, 35,184,372,088,832, a day in the year 3084."
  35184372088832)

(defn- send!
  "Appends a thing to the streaming depot with the ack level :ack, and keeps
  it. Gives :acked, or what the append threw, which is not an answer."
  [x]
  (swap! things-sent conj x)
  (try (rama/foreign-append! (:streaming-depot *hand*) x :ack)
       :acked
       (catch Throwable t [:append-failed (.getSimpleName (class t))])))

(defn- decide!
  "Sends a thing and reads its answer by act-id. Gives the thing, the ack,
  the answer, whether the answer's digest is the digest of what was sent,
  and one word for the outcome: :accepted, the reason of a refusal, or
  :nothing-yet. The first answer of each act-id is kept, in the order sent,
  for the check of the stamps."
  [x]
  (let [ack (send! x)
        answer (answer-of (:act-id x))
        mine? (digest/is-digest-of? (:digest answer) x)
        word (cond (nil? answer) :nothing-yet
                   (= :accepted (:verdict answer)) :accepted
                   :else (:reason answer))]
    (when (and answer mine? (not-any? #(= (:act-id x) (:act-id %)) @first-answers))
      (swap! first-answers conj {:act-id (:act-id x) :layer (:layer x) :stamp (:stamp answer)}))
    {:thing x :ack ack :answer answer :mine? mine? :word word}))

(defn- value-fact
  "A fact with a value as a maker gives it, and as the hand sends it:
  {:e :k :v}, the value plain."
  [e k v]
  {:e e :k k :v v})

(defn- utf8-of
  "The UTF-8 bytes of a text that the proof wrote out, as a vector: Java's
  own encoding of the String, and no function of the store."
  [^String s]
  (vec (.getBytes s "UTF-8")))

(defn- nested
  "A value inside n vectors, one inside the other."
  [n v]
  (nth (iterate vector v) n))

(defn- chain-of-permissions
  "Permissions of a layer, one beneath the other, down to a depth: a vector
  whose first is `top`, the layer's first permission, of depth 1, and whose
  entry at index d - 1 is the permission of depth d, held by the keyword
  `prefix`-d and given in the layer beneath the one before it."
  [layer top prefix depth]
  (vec (reductions (fn [above d] [(keyword (str prefix "-" d)) layer layer above]) top (range 2 (inc depth)))))

(defn- asked-of-the-cluster
  "Runs a call to the cluster on a thread of its own. Gives :there when the
  call came back, :not-there when it threw, and :no-word-in-time when it did
  neither within ten seconds: a call that does not come back cannot hold
  the proof, and fails its check. It does not say why a call threw; the
  check that uses it holds the same call for a name that the module has and
  for one that it never had."
  [call]
  (deref (future (try (call) :there (catch Throwable _ :not-there))) 10000 :no-word-in-time))

(defn- table-of-the-module
  "Asks the cluster for a PState of the module by its name and selects one
  key from it, on one task. Gives what `asked-of-the-cluster` gives."
  [pstate-name]
  (let [ipc (:ipc *hand*)
        m (:module *hand*)]
    (asked-of-the-cluster #(rama/foreign-select-one (path/keypath "the module's proof: a key that no row has")
                                                    (rama/foreign-pstate ipc m pstate-name) {:pkey :alice}))))

(defn- query-of-the-module
  "Asks the cluster for a query of the module by its name and calls it with
  these arguments. Gives what `asked-of-the-cluster` gives."
  [query-name & args]
  (let [ipc (:ipc *hand*)
        m (:module *hand*)]
    (asked-of-the-cluster #(apply rama/foreign-invoke-query (rama/foreign-query ipc m query-name) args))))

(defn- depot-of-the-module
  "Asks the cluster for a depot of the module by its name, and for the
  number of its partitions. Gives what `asked-of-the-cluster` gives."
  [depot-name]
  (let [ipc (:ipc *hand*)
        m (:module *hand*)]
    (asked-of-the-cluster #(:num-partitions (rama/foreign-object-info (rama/foreign-depot ipc m depot-name))))))

(defn- times-in
  "Takes things, as the hand sent them or as a depot's log gave them back.
  Gives how often each time is among them: a map from [act-id time] to a
  count, with one entry for each thing that is a map with an act-id. A
  thing with no time on it has :no-time there, so that it is told from a
  thing whose time is nil."
  [things]
  (frequencies (for [x things
                     :when (and (map? x) (contains? x :act-id))]
                 [(:act-id x) (get x declaration/time-part :no-time)])))

(defn- values-in
  "Takes things, as the hand sent them or as a depot's log gave them back.
  Gives how often each value is among them: a map from
  [act-id entity key value] to a count, with one entry for each fact that
  holds a value, of each thing that is a map whose facts are a sequence. A
  run of bytes is taken by its content."
  [things]
  (frequencies (for [x things
                     :when (and (map? x) (sequential? (:facts x)))
                     fact (:facts x)
                     :when (and (map? fact) (contains? fact :v))]
                 [(:act-id x) (:e fact) (:k fact) (content (:v fact))])))

(defn- counts-apart
  "Takes two maps from a key to a count, and a function that cuts a key to
  what a line can show. Gives the keys, cut, that the two maps do not hold
  with one count: an empty vector when the two maps are equal."
  [a b cut]
  (vec (keep (fn [k] (when-not (= (get a k) (get b k)) (cut k))) (distinct (concat (keys a) (keys b))))))

(defn- row
  "Selects one row of a PState of the gate, by its key, on the task of the
  layer."
  [pstate layer k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey layer}))

(defn- fact-row
  "The row of a place of a layer, or nil for an empty place."
  [layer e k]
  (row :facts-ps layer (record/place-key layer e k)))

(defn- value-bytes
  "The plain bytes of the value that a fact's row holds, as a vector: what
  `softland.store.record.rows/plain-bytes-of` gives for the row's String.
  nil for no row and for a row with no value."
  [row]
  (some-> (record/plain-bytes-of row) vec))

(defn- marked-keys
  "The keys that a streaming layer marks, each with the stamp since which its
  order by value is level: read from the row at the layer's marked-keys
  place."
  [layer]
  (record/marked-keys-of (fact-row layer layer :softland.store/marked-keys)))

(defn- entries-of
  "The rows of the run of one value under a marked key of a streaming layer,
  since a stamp of that key: [key row] of each, in the order of the keys.
  One select of a range of the facts by value, on the layer's task."
  [layer k since v]
  (let [[from to] (record/by-value-run layer k since v)]
    (rama/foreign-select [(path/sorted-map-range from to) path/ALL] (:by-value-ps *hand*) {:pkey layer})))

(defn- run-of
  "The entities of the places in the run of one value: what points at it, in
  a streaming layer, under a marked key, since a stamp of that key."
  [layer k since v]
  (mapv (fn [[k _]] (record/by-value-entity k)) (entries-of layer k since v)))

(defn- entry
  "The row at one key of the facts by value of a streaming layer, or nil."
  [layer k since v e]
  (row :by-value-ps layer (record/by-value-key layer k since (record/value-digest-of v) e)))

(defn- record-of-act
  "The record that a depot's log holds under an act-id, among records read
  back as [partition record]."
  [records act-id]
  (some (fn [[_ r]] (when (and (map? r) (= act-id (:act-id r))) r)) records))

(defn- last-stamp
  "The last stamp that the streaming gate gave in a layer: the number under
  the text of the layer id in `:last-stamp` of the gate's task table, read
  on the task of `on-the-task-of`, the layer's own when none is given. nil
  for a layer of which the gate decided nothing on that task."
  ([layer] (last-stamp layer layer))
  ([layer on-the-task-of]
   (rama/foreign-select-one (path/keypath :last-stamp (declaration/id-text layer)) (:task-ps *hand*)
                            {:pkey on-the-task-of})))

(defn- answer-row
  "The row of the answer under the act-id of a thing that the hand sent, as
  the answers table of the act-id's gate holds it on the layer's task, or
  nil: one select, of the streaming gate's table or, for an act-id of the
  microbatch depot, of the microbatch gate's. The read by act-id gives
  `:time` nil both for a row whose time is nil and for one with no time;
  the row tells the two apart."
  [x]
  (let [[layer depot uuid] (:act-id x)]
    (rama/foreign-select-one (path/keypath (record/answer-key layer uuid))
                             (get *hand* (if (= :microbatch depot) :mb-answers-ps :answers-ps))
                             {:pkey layer})))

(defn- strays
  "The number of different strays under a word, over every task: one select
  for each task, with a key that hashes to it."
  [word]
  (reduce + (map (fn [k] (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                                  (:task-ps *hand*) {:pkey k}))
                 (rtest/gen-hashing-index-keys tasks))))

(defn- stray-digests
  "The digests of the strays under a word, over every task."
  [word]
  (into #{} (mapcat (fn [k] (rama/foreign-select [(path/keypath :strays word) path/ALL]
                                                 (:task-ps *hand*) {:pkey k}))
                    (rtest/gen-hashing-index-keys tasks))))

(defn- stored
  "Each record of the streaming depot's log, read back: [partition record]."
  []
  (let [depot (:streaming-depot *hand*)]
    (for [p (range (:num-partitions (rama/foreign-object-info depot)))
          :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
          r (rama/foreign-depot-read depot p start-offset end-offset)]
      [p r])))

(defn- short-id
  "The first eight characters of the uuid of an act-id, for a line."
  [act-id]
  (subs (str (nth act-id 2)) 0 8))

(defn- told
  "A result as text for a line: the short act-id, the outcome, the stamp."
  [{:keys [thing answer word]}]
  (str (short-id (:act-id thing)) " " word " stamp " (:stamp answer)))

(defn- stamps-apart-from-the-rule
  "Holds each stamp of a gate's answers against the gates' one rule, by the
  proof's own count. Takes answers as `first-answers` keeps the streaming
  gate's, or as `decided-on-the-microbatch-depot` gives the microbatch
  gate's, {:act-id :layer :stamp} each, in the order decided. For each it
  makes the stamp that the rule gives, for an accept and a refusal alike:
  the stamp of the answer before it in its layer plus one, or 0 for the
  first answer of its layer. Neither the time on the thing nor a stamp that
  it carries counts. The stamp before is the one that the answer before
  holds, so each answer is held alone. Gives [short act-id, the stamp of
  the answer, the stamp by the rule] for each answer whose stamp is
  another: an empty vector when each is the rule's. What must hold: the
  answers hold each decision of each of their layers, or a layer's count
  shows a gap where the hand missed one."
  [answers]
  (:apart
   (reduce (fn [{:keys [last apart]} {:keys [act-id layer stamp]}]
             (let [by-the-rule (if-some [before (get last layer)] (inc before) 0)]
               {:last (assoc last layer stamp)
                :apart (if (= by-the-rule stamp) apart (conj apart [(short-id act-id) stamp by-the-rule]))}))
           {:last {} :apart []}
           answers)))

;; ------------------------------------------------------ the hand for the base

(def ^:private time-limit-ms
  "How long the hand watches a table for a row that a batch is to show, in
  milliseconds, before it gives up."
  30000)

(def ^:private index-keys
  "For each task of the proof's cluster, a key that Rama's hash partitioner
  sends to it: the i-th to task i. A select with one of them as the
  partitioning key reads that task's part of a table."
  (vec (rtest/gen-hashing-index-keys tasks)))

(defn- append-to-base!
  "Appends a thing to the microbatch depot with the ack level :append-ack,
  and keeps it. Gives :appended, or what the append threw. The ack says that
  the depot holds the thing, and nothing about the decision. Each append
  waits for that ack, so the depot holds the things of one thread in the
  order in which the thread appended them."
  [x]
  (swap! base-things-sent conj x)
  (try (rama/foreign-append! (:mb-depot *hand*) x :append-ack)
       :appended
       (catch Throwable t [:append-failed (.getSimpleName (class t))])))

(defn- wait-until-processed!
  "Waits until the microbatch gate has processed each thing that the hand
  appended to its depot. Rama's test tool; a door has none, and reads."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module *hand*) "microbatch-gate" (count @base-things-sent)))

(defn- base-result
  "A result as `decide!` gives one, for a thing that was appended to the
  microbatch depot and that the gate has processed: the thing, the ack, the
  answer, whether the answer's digest is the digest of what was sent, and
  one word. The answer is read once, by one call of the query with the
  act-id's layer as the routing id: the hand waits for nothing behind the
  gate's processing. What must hold: the gate has processed the thing."
  [x ack]
  (let [answer (answer-of (when (map? x) (get x :act-id)))]
    {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

(defn- decide-in-base!
  "Sends one thing to the microbatch depot, alone, waits until the gate has
  processed it, and reads its answer by act-id once. Gives the result, as
  `decide!` does."
  [x]
  (let [ack (append-to-base! x)]
    (wait-until-processed!)
    (base-result x ack)))

(defn- decide-in-a-row!
  "Sends things to the microbatch depot one after the other, with no wait
  for the gate between two of them, as one maker does who sends her acts in
  a row. The gate runs meanwhile, so the things fall into as many batches as
  it happens to cut. Then it waits until the gate has processed them and
  reads each answer once. Gives the result of each, in the order given."
  [xs]
  (let [acks (mapv append-to-base! xs)]
    (wait-until-processed!)
    (mapv base-result xs acks)))

(defn- decide-in-one-batch!
  "Sends things to the microbatch depot so that one batch of the gate takes
  them all: it pauses the microbatch topology, appends, and lets it go on.
  Rama's test docs say that the next batch then holds each record that was
  appended meanwhile, up to 1,000 for a partition; no check of the proof
  sees a batch from inside. `while-paused`, when given, is a function of no
  argument that the hand calls behind the appends and before it lets the
  topology go on. Gives the result of each thing, in the order given."
  ([xs] (decide-in-one-batch! xs nil))
  ([xs while-paused]
   (rtest/pause-microbatch-topology! (:ipc *hand*) (:module *hand*) "microbatch-gate")
   (let [acks (try
                (let [acks (mapv append-to-base! xs)]
                  (when while-paused (while-paused))
                  acks)
                (finally
                  (rtest/resume-microbatch-topology! (:ipc *hand*) (:module *hand*) "microbatch-gate")))]
     (wait-until-processed!)
     (mapv base-result xs acks))))

(defn- on-each-task
  "Selects one path of a PState on each task of the cluster, with a key that
  hashes to the task. Gives what each task gave, in the order of the tasks:
  nil for a task that holds nothing there."
  [pstate p]
  (mapv (fn [pkey] (rama/foreign-select-one p (get *hand* pstate) {:pkey pkey})) index-keys))

(defn- rows-by-task
  "Selects the row under a key on each task of the cluster. Gives a sorted
  map from the number of each task that holds a row to that row."
  [pstate k]
  (into (sorted-map)
        (keep-indexed (fn [task row] (when row [task row])))
        (on-each-task pstate (path/keypath k))))

(defn- base-row
  "Selects one row of a PState of the microbatch gate, by its key, on the
  base's task: with the base's id as the partitioning key."
  [pstate k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey :base}))

(defn- base-fact
  "The row of a place of the base, as the base's task holds it, or nil."
  [e k]
  (base-row :mb-facts-ps (record/place-key :base e k)))

(defn- facts-on-every-task
  "The row of a place of the base on each task, cut to what a check holds
  it against: a map from each task that holds a row to
  [the uuid of the act that wrote it, the index of the fact, the stamp, the
  text of the value]."
  [e k]
  (into (sorted-map)
        (map (fn [[task row]] [task [(:by-act row) (:by-index row) (:stamp row) (:value row)]]))
        (rows-by-task :mb-facts-ps (record/place-key :base e k))))

(defn- on-every-task
  "What `facts-on-every-task` gives when each task of the cluster holds the
  same row: the row's four parts, on each task."
  [by-act by-index stamp value-text]
  (into (sorted-map) (map (fn [task] [task [by-act by-index stamp value-text]])) (range tasks)))

(defn- base-marks-rows
  "The row at the base's marked-keys place, on each task that holds one."
  []
  (rows-by-task :mb-facts-ps (record/place-key :base :base :softland.store/marked-keys)))

(defn- base-marked-keys
  "The keys that the base marks, each with its stamp, as the base's task
  holds them."
  []
  (record/marked-keys-of (base-fact :base :softland.store/marked-keys)))

(defn- base-entries-of
  "The rows of the run of one value under a marked key of the base, since a
  stamp of that key: [key row] of each, in the order of the keys. One select
  of a range of the facts by value, on the base's task."
  [k since v]
  (let [[from to] (record/by-value-run :base k since v)]
    (rama/foreign-select [(path/sorted-map-range from to) path/ALL] (:mb-by-value-ps *hand*) {:pkey :base})))

(defn- base-run-of
  "The entities of the places in the run of one value of the base, as the
  base's task holds it."
  [k since v]
  (mapv (fn [[k _]] (record/by-value-entity k)) (base-entries-of k since v)))

(defn- base-runs-by-task
  "The entities of the places in the run of one value of the base, on each
  task, in the order of the tasks."
  [k since v]
  (let [[from to] (record/by-value-run :base k since v)]
    (mapv (fn [pkey]
            (mapv (fn [[k _]] (record/by-value-entity k))
                  (rama/foreign-select [(path/sorted-map-range from to) path/ALL] (:mb-by-value-ps *hand*) {:pkey pkey})))
          index-keys)))

(defn- base-entry-key
  "The key in the facts by value of a place of the base, for a value."
  [k since v e]
  (record/by-value-key :base k since (record/value-digest-of v) e))

(defn- base-entries-under
  "Each row of the base's facts by value under one marked key since one
  stamp of that key, whatever its value, on each task: for each task, in
  the order of the tasks, a map from the row's key to
  [the uuid of the act that wrote it, the index of the fact]."
  [k since]
  (let [run (str ":base " k " " since)]
    (mapv (fn [pkey]
            (into {}
                  (map (fn [[key row]] [key [(:by-act row) (:by-index row)]]))
                  (rama/foreign-select [(path/sorted-map-range (str run " ") (str run "!")) path/ALL]
                                       (:mb-by-value-ps *hand*) {:pkey pkey})))
          index-keys)))

(defn- last-stamps-by-task
  "The last stamp that the microbatch gate keeps for a layer, on each task
  that keeps one: a sorted map from the task's number to the stamp."
  [layer]
  (into (sorted-map)
        (keep-indexed (fn [task n] (when n [task n])))
        (on-each-task :mb-task-ps (path/keypath :last-stamp (str layer)))))

(defn- base-last-stamp
  "The last stamp that the microbatch gate gave in a layer, the base when
  none is given: the number under the text of the layer id in `:last-stamp`
  of the gate's task table, read on the layer's task. nil for a layer of
  which the gate decided nothing."
  ([] (base-last-stamp :base))
  ([layer]
   (rama/foreign-select-one (path/keypath :last-stamp (str layer)) (:mb-task-ps *hand*) {:pkey layer})))

(defn- times-on-every-task
  "The time that the row of a place of the base holds, the time on the act
  that wrote it: a sorted map from each task that holds a row to that time."
  [e k]
  (into (sorted-map)
        (map (fn [[task row]] [task (:time row)]))
        (rows-by-task :mb-facts-ps (record/place-key :base e k))))

(defn- the-time-on-every-task
  "What `times-on-every-task` gives when each task of the cluster holds a
  row with that time."
  [millis]
  (into (sorted-map) (map (fn [task] [task millis])) (range tasks)))

(defn- base-strays
  "The number of different strays of the microbatch gate under each word,
  over every task."
  []
  (into {}
        (map (fn [word]
               [word (reduce + (on-each-task :mb-task-ps [(path/keypath :strays word) (path/view count)]))]))
        declaration/stray-reasons))

(defn- stored-in-base
  "Each record of the microbatch depot's log, read back: [partition record],
  in the order of the partitions and, within one, of the offsets."
  []
  (let [depot (:mb-depot *hand*)]
    (for [p (range (:num-partitions (rama/foreign-object-info depot)))
          :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
          r (rama/foreign-depot-read depot p start-offset end-offset)]
      [p r])))

(defn- partitions-of
  "Takes the records of a depot's log, each [partition record], and a
  thing. Gives the set of the partitions that hold a record equal to the
  thing."
  [records x]
  (into #{} (keep (fn [[p r]] (when (= x r) p))) records))

(defn- partitions-of-layers
  "Takes the records of a depot's log, each [partition record]. Gives, for
  each layer that a record names under `:layer`, by the printed text of the
  layer id, the set of the partitions that hold a record of it: what a
  client can see of where the depot's partitioner put a layer."
  [records]
  (reduce (fn [by-layer [p r]]
            (let [layer (when (map? r) (get r :layer))]
              (if (or (keyword? layer) (uuid? layer))
                (update by-layer (str layer) (fnil conj #{}) p)
                by-layer)))
          {}
          records))

(defn- on-more-than-one-partition
  "Takes the records of a depot's log. Gives the text of each layer whose
  records are on more than one partition, sorted: an empty vector when each
  layer is one partition."
  [records]
  (vec (sort (keep (fn [[layer partitions]] (when (< 1 (count partitions)) layer)) (partitions-of-layers records)))))

(defn- table-on-each-task
  "Selects a whole table of rows of the microbatch gate on each task. Gives,
  in the order of the tasks, a map from each key that the task holds to its
  row, a run of bytes as the vector of its bytes. One select of a task's
  whole part of the table; the tables of this proof fit in memory."
  [pstate]
  (mapv (fn [pkey]
          (into {}
                (map (fn [[k row]] [k (content row)]))
                (rama/foreign-select [path/ALL] (get *hand* pstate) {:pkey pkey})))
        index-keys))

(defn- task-table-on-each-task
  "What the microbatch gate keeps for each task beside its three tables. In
  the order of the tasks: {:last-stamp, a map from the text of a layer id to
  that layer's last stamp; :strays, a map from each word that has a stray on
  the task to the set of the digests under it}."
  []
  (mapv (fn [pkey]
          (let [ps (:mb-task-ps *hand*)]
            {:last-stamp (into {} (map vec) (rama/foreign-select [(path/keypath :last-stamp) path/ALL] ps {:pkey pkey}))
             :strays (into {}
                           (keep (fn [word]
                                   (let [digests (set (rama/foreign-select [(path/keypath :strays word) path/ALL] ps {:pkey pkey}))]
                                     (when (seq digests) [word digests]))))
                           declaration/stray-reasons)}))
        index-keys))

(defn- base-tables
  "The four tables of the microbatch gate, whole, on every task:
  {:facts :by-value :answers :task}, each a vector in the order of the
  tasks (`table-on-each-task`, `task-table-on-each-task`)."
  []
  {:facts (table-on-each-task :mb-facts-ps)
   :by-value (table-on-each-task :mb-by-value-ps)
   :answers (table-on-each-task :mb-answers-ps)
   :task (task-table-on-each-task)})

(defn- keys-apart
  "Takes one table on each task, a vector of maps from key to row. Gives the
  keys whose row is not the same on every task, a task that lacks the key
  among them, sorted: an empty vector when every task holds the same map."
  [by-task]
  (vec (sort (remove (fn [k] (apply = (map #(get % k ::no-row) by-task)))
                     (distinct (mapcat keys by-task))))))

(defn- tables-apart
  "Takes what `base-tables` gave at two moments. Gives [table task] for each
  task's part of a table that is not the same at both, in every key and
  every field: an empty vector when nothing changed."
  [before after]
  (vec (for [table [:facts :by-value :answers :task]
             task (range tasks)
             :when (not= (get-in before [table task]) (get-in after [table task]))]
         [table task])))

(defn- the-copies
  "Reads the four tables of the microbatch gate whole, on every task, and
  holds them against the depot's log. Takes the records of the microbatch
  depot's log, each [partition record]. Gives:

    :facts            how many rows of the facts each task holds
    :facts-apart      the keys of the facts whose row is not the same on
                      every task
    :by-value         how many rows of the facts by value each task holds
    :by-value-apart   the same for the facts by value
    :answers          how many answers each task holds
    :answers-off      the key of each answer that is on a task which is not
                      the one partition of its layer in the depot's log
    :last-stamps      for each task, the texts of the layers of which it
                      keeps a last stamp
    :last-stamps-off  [task layer] of each last stamp on a task which is
                      not the one partition of its layer
    :strays           how many digests of strays each task holds
    :strays-off       [task word] of each digest of a stray on a task whose
                      partition holds no stray of that digest

  The layer of an answer is the text before the space in its key."
  [records]
  (let [{:keys [facts by-value answers task]} (base-tables)
        partitions (partitions-of-layers records)
        on-its-task? (fn [task-number layer-text] (= #{task-number} (get partitions layer-text)))
        strays-arrived (into #{}
                             (keep (fn [[p r]] (when (declaration/stray-reason :microbatch r) [p (digest/hex-of r)])))
                             records)]
    {:facts (mapv count facts)
     :facts-apart (keys-apart facts)
     :by-value (mapv count by-value)
     :by-value-apart (keys-apart by-value)
     :answers (mapv count answers)
     :answers-off (vec (for [[task-number rows] (map-indexed vector answers)
                             k (sort (keys rows))
                             :when (not (on-its-task? task-number (first (str/split k #" " 2))))]
                         k))
     :last-stamps (mapv (comp vec sort keys :last-stamp) task)
     :last-stamps-off (vec (for [[task-number kept-there] (map-indexed vector task)
                                 layer-text (sort (keys (:last-stamp kept-there)))
                                 :when (not (on-its-task? task-number layer-text))]
                             [task-number layer-text]))
     :strays (mapv (fn [kept-there] (reduce + (map count (vals (:strays kept-there))))) task)
     :strays-off (vec (for [[task-number kept-there] (map-indexed vector task)
                            [word digests] (sort-by key (:strays kept-there))
                            d (sort digests)
                            :when (not (contains? strays-arrived [task-number d]))]
                        [task-number word]))}))

(defn- check-the-copies
  "Holds where the rows of the microbatch gate are, at one moment of the
  proof. Takes a word for the moment, which opens each check's text, and the
  records of the microbatch depot's log as read then. Makes five checks and
  one SHOWN line. Gives what `the-copies` gave."
  [at records]
  (let [copies (the-copies records)
        base-partition (first (get (partitions-of-layers records) ":base"))]
    (check (str at ": `$$microbatch-facts` is the same map on every task: each task holds rows, as many as each other task, and no key has a row that is not the same on all of them, in every field")
           [true 1 []] [(pos? (first (:facts copies))) (count (set (:facts copies))) (:facts-apart copies)])
    (check (str at ": `$$microbatch-facts-by-value` is the same map on every task")
           [1 []] [(count (set (:by-value copies))) (:by-value-apart copies)])
    (check (str at ": `$$microbatch-answers` holds a row only on the task of its layer: no answer is on a task that is not the partition of its layer in the depot's log, and the base's task holds answers")
           [[] true] [(:answers-off copies) (pos? (nth (:answers copies) base-partition 0))])
    (check (str at ": `$$microbatch-task` keeps a layer's last stamp only on the task of that layer, and the digest of a stray only on the task of the partition where the stray arrived")
           [[] []] [(:last-stamps-off copies) (:strays-off copies)])
    (check (str at ": each layer that a thing of the microbatch depot names is on one partition of the depot")
           [] (on-more-than-one-partition records))
    (shown (str at ":") "the rows of the microbatch gate by task | facts" (pr-str (:facts copies)) "| facts by value" (pr-str (:by-value copies))
           "| answers" (pr-str (:answers copies)) "| the layers of the last stamps" (pr-str (:last-stamps copies))
           "| digests of strays" (pr-str (:strays copies)) "| the base's task is" base-partition)
    copies))

(defn- decided-on-the-microbatch-depot
  "Takes the records of the microbatch depot's log, each [partition record],
  in the order of the partitions and of each partition's offsets. Gives one
  entry for each act-id under which the microbatch gate decided a thing, in
  that order, as `first-answers` holds the streaming gate's, with one word
  more: {:act-id :layer :stamp :word}. The first record under an act-id is
  the one that the gate decided. A stray has no entry. The answer is read
  by act-id, once; an act-id with no answer has the word :nothing-yet and
  no stamp. A layer is one partition, so the entries of one layer are in
  the order in which the gate decided them."
  [records]
  (:out
   (reduce (fn [{:keys [seen out] :as so-far} [_ r]]
             (let [act-id (when (map? r) (get r :act-id))]
               (if (or (declaration/stray-reason :microbatch r) (contains? seen act-id))
                 so-far
                 (let [answer (answer-of act-id)]
                   {:seen (conj seen act-id)
                    :out (conj out {:act-id act-id :layer (nth act-id 0) :stamp (:stamp answer)
                                    :word (cond (nil? answer) :nothing-yet
                                                (= :accepted (:verdict answer)) :accepted
                                                :else (:reason answer))})}))))
           {:seen #{} :out []}
           records)))

;; ------------------------------- the hand for either depot and for the source

(defn- answered-of
  "Takes things that the hand sent to a gate's depot, and that gate's depot
  word. Gives [act-id answer] for each act-id of that depot among them that
  has an answer, once, as the read by act-id gives it now. An act-id of the
  other depot is left out: a gate keeps such a thing as a stray, and the
  answer under it, if any, is the other gate's."
  [things depot-word]
  (into []
        (comp (filter map?)
              (map :act-id)
              (filter (fn [act-id] (and (vector? act-id) (= depot-word (nth act-id 1 nil)))))
              (distinct)
              (keep (fn [act-id] (when-let [answer (answer-of act-id)] [act-id answer]))))
        things))

(defn- client-append
  "Appends a thing to a depot as a client of the cluster does, on a thread
  of its own. Gives :appended, :refused when the append threw, and
  :no-word-in-time when it did neither within ten seconds."
  [depot x]
  (deref (future (try (rama/foreign-append! depot x) :appended (catch Throwable _ :refused))) 10000 :no-word-in-time))

(defn- task-of-stray
  "The number of the task whose set of a stray's word holds a digest, in a
  gate's task table, or nil when no task's does: one select of the set on
  each task."
  [pstate word digest]
  (first (keep-indexed (fn [task digests] (when (some #{digest} digests) task))
                       (mapv (fn [pkey] (rama/foreign-select [(path/keypath :strays word) path/ALL] (get *hand* pstate) {:pkey pkey}))
                             index-keys))))

(defn- code-of
  "Takes the text of a Clojure source file. Gives it with each string and
  each comment taken out: what is left is code, so a name that a docstring
  holds is not in it. It knows a string with its escapes, a comment from a
  semicolon to the end of its line, and a character literal, so that a quote
  written as a character opens no string."
  [^String text]
  (let [sb (StringBuilder.)
        n (.length text)]
    (loop [i 0
           state :code]
      (if (< i n)
        (let [c (.charAt text i)]
          (case state
            :code (cond (= c \\) (do (.append sb c)
                                     (when (< (inc i) n) (.append sb (.charAt text (inc i))))
                                     (recur (+ i 2) :code))
                        (= c \") (do (.append sb \space) (recur (inc i) :string))
                        (= c \;) (recur (inc i) :comment)
                        :else (do (.append sb c) (recur (inc i) :code)))
            :string (cond (= c \\) (recur (+ i 2) :string)
                          (= c \") (recur (inc i) :code)
                          :else (recur (inc i) :string))
            :comment (if (= c \newline)
                       (do (.append sb c) (recur (inc i) :code))
                       (recur (inc i) :comment))))
        (.toString sb)))))

(defn- source-of-the-store
  "The code of each Clojure source file of the store, read from `src-store`
  under the directory that the proof runs in: a map from the file's path to
  its text with strings and comments taken out (`code-of`)."
  []
  (into {}
        (comp (filter (fn [^java.io.File f] (str/ends-with? (.getName f) ".clj")))
              (map (fn [^java.io.File f] [(.getPath f) (code-of (slurp f))])))
        (file-seq (java.io.File. "src-store"))))

(defn- named-in
  "Takes the code of the store's files and a pattern. Gives how often the
  pattern is found in each file that holds it: a map from the file's name
  to the count."
  [code pattern]
  (into {}
        (keep (fn [[path text]]
                (let [found (count (re-seq pattern text))]
                  (when (pos? found) [(.getName (java.io.File. ^String path)) found]))))
        code))

(defn- found-in-a-text
  "Takes a text of code that the proof wrote and a pattern. Gives how often
  the pattern is found in it, by the search that `named-in` makes of a file
  of the store: the text goes through `code-of` first. A check of a word
  that no file of the store holds uses it to show that the search finds the
  word where it is."
  [text pattern]
  (get (named-in {"a-text-of-the-proof.clj" (code-of text)} pattern) "a-text-of-the-proof.clj" 0))

;; ------------------------------------------------------------- the people

(def ^:private alice-own "Alice's permission in her personal layer." [:alice :alice :alice])
(def ^:private alice-in-team "Alice's permission in the group's layer that she makes." [:alice :team :team])
(def ^:private bobs "Bob's permission in Alice's layer, beneath hers." [:bob :alice :alice alice-own])
(def ^:private carols "Carol's permission in Alice's layer, beneath Bob's." [:carol :alice :alice bobs])
(def ^:private daves "Dave's permission in Alice's layer, beneath Carol's: four deep." [:dave :alice :alice carols])

(def ^:private system-own "The system's permission in the base, the base's first." [:system :base :base])
(def ^:private alice-in-base "Alice's permission in the base, beneath the system's." [:alice :base :base system-own])
(def ^:private carol-in-base "Carol's permission in the base, beneath the system's." [:carol :base :base system-own])
(def ^:private dave-in-base "Dave's permission in the base, beneath the system's." [:dave :base :base system-own])
(def ^:private bob-in-base "Bob's permission in the base, beneath Alice's." [:bob :base :base alice-in-base])

(def ^:private alice-in-canvas
  "Alice's permission in the layer :canvas, which she makes for the checks of
  what the gates keep for a read."
  [:alice :canvas :canvas])

(def ^:private who-may-see "The store key of who may see a layer." :softland.store/who-may-see)
(def ^:private builds-on "The store key of what a layer builds on." :softland.store/builds-on)
(def ^:private marks "The store key of the keys that a layer marks." :softland.store/marked-keys)

(defn- making
  "The making act of a layer: by `who`, naming the first permission of
  `holder`, with the depot that the layer starts with."
  [layer who holder]
  (thing layer who [holder layer layer]
         [{:e layer :k :softland.store/depot :v (first-act/depot-at-making layer)}
          {:e holder :k :softland.store/permission :v [holder layer layer]}]))

(defn- alice
  "Alice's act into her personal layer, with these facts."
  ([facts] (alice facts {}))
  ([facts more] (thing :alice :alice alice-own facts more)))

(defn- system-act
  "The system's act into the base, with these facts."
  ([facts] (system-act facts {}))
  ([facts more] (thing :base :system system-own facts more)))

(defn- canvas-act
  "Alice's act into the layer :canvas, with these facts."
  [facts]
  (thing :canvas :alice alice-in-canvas facts))

(def ^:private kinds-of-value
  "One value of each kind that the checks of what a fact keeps write, by a
  name that is the fact's key: an id that is a keyword, an id that is a
  UUID, a string with spaces, a number, a map, and a map that is not in its
  normal form, with a decimal that has a trailing zero, a big integer that a
  fixed-width integer holds, and its keys out of order."
  {:kw :canvas-1 :uuid #uuid "0b0f1c6e-5a52-4c8e-9a43-6f1d2e7c8a90" :string "a string with spaces" :number 42
   :map {:a [1 #{2}] "b c" 1.5} :as-given {:b 1.50M :a [1 2N]}})

(def ^:private texts-of-kinds
  "The text that the row of each of `kinds-of-value` must hold, written out
  by hand: the one text of the value in its normal form. No function of the
  store made any of them."
  {:kw ":canvas-1" :uuid "#uuid \"0b0f1c6e-5a52-4c8e-9a43-6f1d2e7c8a90\"" :string "\"a string with spaces\"" :number "42"
   :map "{\"b c\" 1.5 :a [1 #{2}]}" :as-given "{:a [1 2] :b 1.5M}"})

(def ^:private kinds-read-back
  "What a reader gets back from the row of each of `kinds-of-value`: the
  value in its normal form. It is the value that was given for each but the
  last, whose decimal has lost its trailing zero and whose big integer is a
  fixed-width one."
  (assoc kinds-of-value :as-given {:a [1 2] :b 1.5M}))

(def ^:private edges-of-a-value
  "Values at the edges of what a value is, by a name that is the fact's key,
  each with the text that its row must hold, written out by hand: `false`,
  an empty string and an empty map, each a value and no retract; and a
  number inside 32 vectors, the most collections that a value can be
  inside."
  {:false [false "false"]
   :empty-string ["" "\"\""]
   :empty-map [{} "{}"]
   :inside-32 [(nested 32 1) (str (apply str (repeat 32 "[")) 1 (apply str (repeat 32 "]")))]})

(def ^:private at-the-bound-of-a-value
  "A string whose one text is 65,536 bytes of UTF-8, the most that a gate
  takes for one value under a plain key: two characters of one byte, 21,844
  characters of three bytes, and the two quotes. It has 21,846 characters,
  so a bound that counted characters would not find its edge here."
  (apply str "ab" (repeat 21844 "€")))

(def ^:private over-the-bound-of-a-value
  "A string whose one text is 65,537 bytes of UTF-8: one byte over the bound
  of one value."
  (str at-the-bound-of-a-value "c"))

(defn- act-over-its-bound
  "An act into a layer whose accept would write more text into the rows of
  its facts than a gate takes for one act, 18,874,368 bytes: 256 facts, each
  with a value at the bound of one value, 65,536 bytes, under a who of 256
  characters and a permission of depth 32 whose holders have 256 characters
  each, which are more than 8,192 bytes for each row. So it is at least
  256 x (65,536 + 8,193) = 18,874,624 bytes. The permission stands nowhere:
  the bound is held before any row of the act's standing is read."
  [layer]
  (let [long-id (fn [i] (keyword (apply str "holder-" i "-" (repeat (- 256 (count (str ":holder-" i "-"))) "h"))))
        top [(long-id 1) layer layer]
        permission (reduce (fn [above d] [(long-id d) layer layer above]) top (range 2 33))]
    (thing layer (long-id 32) permission
           (mapv (fn [i] (value-fact (keyword (str "over-" i)) :text at-the-bound-of-a-value)) (range 256)))))

;; ---------------------------------------------------------------- the checks

(deftest what-rama-carries
  (let [kinds {:vector [1 2 3] :list (list 1 2 3) :map {:a {:b 1}} :keyword :tool/class
               :uuid (UUID/randomUUID) :big-integer 100000000000000000000N :decimal 1.50M
               :bytes (byte-array [1 2 -3]) :set #{1 2} :narrow-integer (int 5) :minus-zero -0.0
               :string "a \"text\"" :nothing nil :subvector (subvec [0 1 2] 1)}
        x {:act-id (new-act-id :carrier) :layer :carrier :kinds kinds}
        sent-digest (digest/hex-of x)
        result (decide! x)
        [partition back] (first (filter (fn [[_ r]] (and (map? r) (= (:act-id x) (:act-id r)))) (stored)))
        back-kinds (:kinds back)]
    (check "the thing was acked" :acked (:ack result))
    (check "the gate's digest of the thing as it arrived is the digest that the door took before the append"
           sent-digest (:digest (:answer result)))
    (check "a thing that is no act, with a usable act-id, gets a recorded refusal" :unknown-part (:word result))
    (check "the thing holds no time, and it is the first thing that the gate decides in its layer: the stamp of its refusal is 0, and that is the layer's last stamp"
           [nil 0 0] [(time-on x) (:stamp (:answer result)) (last-stamp :carrier)])
    (check "the record read back from the depot's log is another object than the one sent: it was serialised"
           false (identical? x back))
    (check "the digest of what came back from the depot's log is the digest of what was sent"
           sent-digest (digest/hex-of back))
    (check "a vector comes back a vector, a list a list in its order, a plain map a plain map, bytes the same bytes"
           [true true [1 2 3] true [1 2 -3]]
           [(vector? (:vector back-kinds))
            (and (sequential? (:list back-kinds)) (not (vector? (:list back-kinds))))
            (vec (:list back-kinds))
            (and (map? (:map back-kinds)) (not (record? (:map back-kinds))))
            (vec (:bytes back-kinds))])
    (check "a keyword with its namespace, a UUID, a big integer, a decimal and a set come back equal"
           (select-keys kinds [:keyword :uuid :big-integer :decimal :set])
           (select-keys back-kinds [:keyword :uuid :big-integer :decimal :set]))
    (shown "each kind of value comes back from the depot's log with the digest it was sent with:" sent-digest
           "| partition" partition "| the classes that came back:"
           (pr-str (into (sorted-map) (map (fn [[k v]] [k (some-> v class .getSimpleName)])) back-kinds)))
    (shown "the gate took the same digest of the thing as it arrived, and recorded it with its refusal:" (told result))))

(deftest layers-are-made
  (let [before (depot-of-layer :alice)
        not-made (decide! (alice [{:e :note-1 :k :text}]))
        value-early (decide! (alice [(value-fact :note-0 :text "a value for a layer that is not made")]))
        row-early (fact-row :alice :note-0 :text)
        by-stranger (decide! (assoc (making :alice :system :alice) :who :mallory))
        base-here (decide! (thing :base :system [:system :base :base]
                                  [{:e :base :k :softland.store/depot :v :microbatch}
                                   {:e :system :k :softland.store/permission :v [:system :base :base]}]
                                  {:act-id [:base :streaming (UUID/randomUUID)] :depot :streaming}))
        base-then (depot-of-layer :base)
        personal (decide! (making :alice :system :alice))
        group (decide! (making :team :alice :alice))
        own-uuid (UUID/randomUUID)
        own-layer (UUID/randomUUID)
        by-uuid (decide! (making own-layer own-uuid own-uuid))
        again-by-system (decide! (making :alice :system :alice))
        again-by-holder (decide! (making :team :alice :alice))
        system-again (decide! (thing :alice :system alice-own [{:e :note-1 :k :text}]))]
    (check "before its making, the depot of a layer is nothing" nil before)
    (check "an act into a layer that is not made is refused" :layer-not-made (:word not-made))
    (check "an act with a value into a layer that is not made is refused with the same word, under its own act-id and with the digest of what was sent, and writes no row at its place"
           [:acked :layer-not-made true nil] [(:ack value-early) (:word value-early) (:mine? value-early) row-early])
    (check "a making act by a who that is neither the holder nor the system is refused" :layer-not-made (:word by-stranger))
    (check "a making act of the base on the streaming depot, while the base is not made, is refused as an act for a layer of the other gate, under its streaming act-id and with the digest of what was sent: the gate tells it from the layer id, and the base has no depot yet"
           [:layer-belongs-to-the-other-gate true nil] [(:word base-here) (:mine? base-here) base-then])
    (check "a person's layer made by the system for that person: accepted, and its depot is streaming"
           [:accepted true :streaming] [(:word personal) (:mine? personal) (depot-of-layer :alice)])
    (check "the record holds the depot as a fact on the layer's own entity, and the person's permission on its holder"
           [:streaming alice-own]
           [(record/value-of (fact-row :alice :alice :softland.store/depot)) (record/value-of (fact-row :alice :alice :softland.store/permission))])
    (check "the depot, the permission and the answer hold one stamp"
           [(:stamp (:answer personal)) (:stamp (:answer personal))]
           [(:stamp (fact-row :alice :alice :softland.store/depot)) (:stamp (fact-row :alice :alice :softland.store/permission))])
    (check "a group's layer made by the person who creates it: accepted, and its depot is streaming"
           [:accepted :streaming] [(:word group) (depot-of-layer :team)])
    (check "a layer whose id is a UUID, made by a person whose id is a UUID" [:accepted :streaming]
           [(:word by-uuid) (depot-of-layer own-layer)])
    (check "a making act for a layer that is made, by the system under the person's permission, is refused"
           :who-not-the-permissions-holder (:word again-by-system))
    (check "a making act for a layer that is made, by its holder, is refused: its permission is not beneath its own"
           :permission-fact-not-beneath-the-acts-own (:word again-by-holder))
    (check "the system holds no permission in the personal layer that it made: its second act there is refused"
           :who-not-the-permissions-holder (:word system-again))
    (check "the first permission of the group's layer is the one that the first making act wrote"
           alice-in-team (record/value-of (fact-row :team :alice :softland.store/permission)))
    (shown "an act into a layer that is not made refused:" (told not-made) "| one with a value, the same:" (told value-early)
           "| the row at its place:" (pr-str row-early)
           "| a making act of the base on the streaming depot, while the base is not made:" (told base-here))
    (shown "a person's layer made by the system for that person, with the depot streaming:" (told personal)
           "| depot-of-layer :alice =" (depot-of-layer :alice))
    (shown "a group's layer made by the person who creates it:" (told group) "| depot-of-layer :team =" (depot-of-layer :team))
    (shown "a making act for a layer that is made is refused: by the system" (:word again-by-system)
           "| by its holder" (:word again-by-holder) "| a second act of the system in a personal layer" (:word system-again))))

(deftest an-act-lands
  (let [fact (value-fact :note-1 :text "the first text")
        landed (decide! (alice [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (fact-row :alice :note-1 :text)
        logged (record-of-act (stored) (:act-id (:thing landed)))
        listed (decide! (alice [] {:facts (list (value-fact :note-2 :text {:n 1}) (value-fact :note-3 :text #{:a}))}))
        list-uuid (nth (:act-id (:thing listed)) 2)
        logged-list (record-of-act (stored) (:act-id (:thing listed)))
        never-sent (answer-of (new-act-id :alice))]
    (swap! kept assoc :landed landed)
    (check "one act with a plain value lands through the streaming gate and is read back by act-id: accepted, with the digest of what was sent"
           [:acked :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the value as a String, the one text of the value in its normal form, and gives the UTF-8 of that text back; the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and the time on the act"
           ["\"the first text\"" (utf8-of "\"the first text\"") act-uuid 0 ":alice" "[:alice :alice :alice]" (:stamp (:answer landed))
            (time-on (:thing landed))]
           [(:value the-row) (value-bytes the-row) (:by-act the-row) (:by-index the-row) (:who the-row) (:permission the-row) (:stamp the-row)
            (:time the-row)])
    (check "the row holds those seven fields and no other"
           #{:value :by-act :by-index :who :permission :stamp :time} (set (keys the-row)))
    (check "the depot's log holds the act's fact as the hand sent it, the value plain, with no part added"
           [{:e :note-1 :k :text :v "the first text"}] (vec (:facts logged)))
    (check "the depot's log holds the time that the hand wrote on the act, a reading of its clock, and the answer holds that time beside its stamp"
           [true (time-on (:thing landed)) (time-on (:thing landed))]
           [(int? (time-on (:thing landed))) (time-on logged) (:time (:answer landed))])
    (check "an act whose facts came as a list is accepted, each fact has its index in the order sent, and each row holds the text of its value"
           [:accepted [list-uuid 0 "{:n 1}"] [list-uuid 1 "#{:a}"]]
           [(:word listed)
            ((juxt :by-act :by-index :value) (fact-row :alice :note-2 :text))
            ((juxt :by-act :by-index :value) (fact-row :alice :note-3 :text))])
    (check "the log holds that act's facts as a sequence that is no vector, the two facts in the order sent, each value plain"
           [true false [{:e :note-2 :k :text :v {:n 1}} {:e :note-3 :k :text :v #{:a}}]]
           [(sequential? (:facts logged-list)) (vector? (:facts logged-list)) (vec (:facts logged-list))])
    (check "an act-id that was never sent has no answer: nothing yet" nil never-sent)
    (shown "one act with a plain value landing through the streaming gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "its place in the record: written by" (str (:by-act the-row)) "index" (:by-index the-row)
           "stamp" (:stamp the-row) "| the text that the row holds:" (pr-str (:value the-row)) "| read back:" (pr-str (record/read-value the-row))
           "| who wrote it:" (pr-str (record/who-of the-row)) "under" (pr-str (record/permission-of the-row))
           "| the fields of the row:" (pr-str (vec (sort (keys the-row)))))
    (shown "the depot's log holds the value plain, as the hand sent it:" (pr-str (vec (:facts logged)))
           "| facts sent as a list are in the log as a" (.getSimpleName (class (:facts logged-list)))
           "| the time on the act, in the log:" (time-on logged))))

(deftest refusals-and-repeats
  (let [accepted-thing (:thing (:landed @kept))
        first-fact-id [(:act-id accepted-thing) 0]
        refused-one (decide! (alice [(assoc (value-fact :note-9 :text "a value that names a fact which is not there")
                                            :replaces [(new-act-id :alice) 0])]))
        answer-before (answer-of (:act-id accepted-thing))
        stamp-before (last-stamp :alice)
        row-before (fact-row :alice :note-1 :text)
        repeat-ack (send! accepted-thing)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (do (send! (:thing refused-one)) (answer-of (:act-id (:thing refused-one))))
        stamp-after (last-stamp :alice)
        at-another-time (at-time accepted-thing (+ 5 (time-on accepted-thing)))
        other-time (decide! at-another-time)
        after-other-time [(last-stamp :alice) (fact-row :alice :note-1 :text)]
        other-under-used (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id accepted-thing)))
        junk (decide! {:act-id (new-act-id :alice) :layer :alice :junk 1})
        good-under-junk (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id (:thing junk))))
        stale (decide! (alice [(assoc (value-fact :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        empty-place (decide! (alice [(assoc (value-fact :note-7 :text "x") :replaces first-fact-id)]))
        replaced (decide! (alice [(assoc (value-fact :note-1 :text "the second text") :replaces first-fact-id)]))
        second-fact-id [(:act-id (:thing replaced)) 0]
        replaced-row (fact-row :alice :note-1 :text)
        stale-now (decide! (alice [(assoc (value-fact :note-1 :text "late") :replaces first-fact-id)]))
        repeat-of-replace (do (send! (:thing replaced)) (answer-of (:act-id (:thing replaced))))
        over (decide! (alice [(value-fact :note-1 :text "the third text")]))
        over-row (fact-row :alice :note-1 :text)
        retract (decide! (alice [{:e :note-1 :k :text}]))
        retract-row (fact-row :alice :note-1 :text)
        over-retract (decide! (alice [(assoc (value-fact :note-1 :text "after the retract")
                                             :replaces [(:act-id (:thing retract)) 0])]))
        retract-of-retract (decide! (alice [{:e :note-6 :k :text}]))
        retract-again (decide! (alice [{:e :note-6 :k :text}]))
        onto-retract (decide! (alice [(value-fact :note-6 :text "onto a retract, naming nothing")]))
        on-the-layer (decide! (alice [(value-fact :alice :title "a fact of the layer's own entity, under a plain key")]))
        twice (decide! (alice [{:e :note-10 :k :text} {:e :note-10 :k :text}]))
        own-base (alice [])
        own-fact (decide! (assoc own-base :facts [{:e :note-10 :k :text}
                                                  {:e :note-11 :k :text :replaces [(:act-id own-base) 0]}]))]
    (check "an act that is refused for a place gets a recorded refusal: its reason, a stamp and the digest of what was sent"
           [:refused :replaced-fact-not-current true true]
           [(:verdict (:answer refused-one)) (:reason (:answer refused-one)) (int? (:stamp (:answer refused-one))) (:mine? refused-one)])
    (check "a refusal writes nothing at the place" nil (fact-row :alice :note-9 :text))
    (check "a repeat is acked and answered from the record: the same answer, with the stamp of the first decision"
           [:acked answer-before] [repeat-ack answer-after])
    (check "a repeat of a refused act reads the recorded refusal" (:answer refused-one) refused-again)
    (check "not decided twice: the layer's last stamp is the same after two repeats" stamp-before stamp-after)
    (check "two things under one act-id that differ only in the time on them, the second 5 ms later: they are not equal, and they have one digest; the second is acked and reads the one answer, that of the first, which is of its own digest too"
           [false true :acked answer-before true]
           [(= accepted-thing at-another-time) (= (digest/hex-of accepted-thing) (digest/hex-of at-another-time))
            (:ack other-time) (:answer other-time) (:mine? other-time)])
    (check "and it is a repeat: nothing is decided a second time, so the layer's last stamp and the row of the act's fact are as they were; the answer that the second reads holds the time on the first, and not its own"
           [stamp-before row-before (time-on accepted-thing)] (conj after-other-time (:time (:answer other-time))))
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false]
           [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (fact-row :alice :note-8 :text))
    (check "a thing that is no act uses its act-id: a good act sent under it reads the first thing's refusal"
           [:unknown-part true :unknown-part false]
           [(:word junk) (:mine? junk) (:word good-under-junk) (:mine? good-under-junk)])
    (check "a stale replaces is refused: a fact that is not the current fact of the place" :replaced-fact-not-current (:word stale))
    (check "a fact that names something to replace on an empty place is stale" :replaced-fact-not-current (:word empty-place))
    (check "a fact that names the current fact of its place is accepted, and the place then holds the text of its value"
           [:accepted "\"the second text\""] [(:word replaced) (:value replaced-row)])
    (check "a second act that names the fact that was replaced is stale" :replaced-fact-not-current (:word stale-now))
    (check "a repeat of the accepted replace is still accepted, although its replaces would now be stale"
           (:answer replaced) repeat-of-replace)
    (check "a fact that names nothing to replace is never stale: it becomes the current fact of a place that holds one"
           [:accepted (nth (:act-id (:thing over)) 2) "\"the third text\""] [(:word over) (:by-act over-row) (:value over-row)])
    (check "a retract is recorded as the current fact of its place: a row with no value, which holds who retracted and under which permission"
           [:accepted (nth (:act-id (:thing retract)) 2) false [:nothing-here] :alice alice-own]
           [(:word retract) (:by-act retract-row) (contains? retract-row :value) (record/read-value retract-row)
            (record/who-of retract-row) (record/permission-of retract-row)])
    (check "a later fact can replace the retract" :accepted (:word over-retract))
    (check "a retract on an empty place, and a retract of a retract, are accepted" [:accepted :accepted]
           [(:word retract-of-retract) (:word retract-again)])
    (check "a value that names nothing, on a place that holds a retract, is accepted" :accepted (:word onto-retract))
    (check "a value fact on the layer's own entity, under a plain key, is a fact like any other" :accepted (:word on-the-layer))
    (check "an act names a place once: two facts on one place, and a fact that replaces a fact of its own act, are refused"
           [:place-named-twice :replaces-own-fact nil] [(:word twice) (:word own-fact) (fact-row :alice :note-11 :text)])
    (shown "a refusal recorded with its reason:" (pr-str (:answer refused-one)))
    (shown "a repeat of that refused act reads the recorded refusal:" (pr-str refused-again))
    (shown "a repeat (same act-id sent twice) answered from the record, not decided twice: stamp before" (:stamp answer-before)
           "after" (:stamp answer-after) "| the layer's last stamp before" stamp-before "after" stamp-after)
    (shown "the same act under its act-id with another time on it, 5 ms later, is a repeat: one digest" (subs (digest/hex-of at-another-time) 0 16)
           "| it reads the first's answer, stamp" (:stamp (:answer other-time)) "| the layer's last stamp after it" (first after-other-time))
    (shown "a second, different act under a used act-id told so: the digest of the answer" (subs (:digest (:answer other-under-used)) 0 16)
           "is not the digest of what was sent" (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "a stale replaces refused:" (told stale) "| the current fact named:" (told replaced) "| the replaced fact named again:" (told stale-now))
    (shown "a place named twice:" (:word twice) "| a fact that replaces a fact of its own act:" (:word own-fact))
    (shown "a fact that names nothing is never stale:" (told over) "| a retract is the current fact of its place:" (told retract)
           "| a later fact replaces the retract:" (told over-retract) "(second fact id" (short-id (first second-fact-id)) ")")))

(deftest settings-and-permissions
  (let [setting (decide! (alice [{:e :team :k :softland.store/depot :v :streaming}]))
        other-layer (decide! (thing :alice :alice alice-in-team [{:e :note-5 :k :text}]))
        not-yet (decide! (thing :alice :bob bobs [{:e :note-5 :k :text}]))
        give (decide! (alice [{:e :bob :k :softland.store/permission :v bobs}]))
        not-yet-again (do (send! (:thing not-yet)) (answer-of (:act-id (:thing not-yet))))
        bob-writes (decide! (thing :alice :bob bobs [(value-fact :bobs-note :text "bob's text")]))
        bobs-row (fact-row :alice :bobs-note :text)
        beside (decide! (thing :alice :bob bobs [{:e :carol :k :softland.store/permission :v [:carol :alice :alice alice-own]}]))
        by-no-holder (decide! (thing :alice :carol bobs [{:e :dave :k :softland.store/permission :v [:dave :alice :alice bobs]}]))
        carol-not-given (decide! (thing :alice :carol [:carol :alice :alice alice-own] [{:e :note-5 :k :text}]))
        give-carol (decide! (thing :alice :bob bobs [{:e :carol :k :softland.store/permission :v carols}]))
        give-dave (decide! (thing :alice :carol carols [{:e :dave :k :softland.store/permission :v daves}]))
        dave-acts (decide! (thing :alice :dave daves [{:e :daves-note :k :text}]))
        eves [:eve :alice :alice daves]
        give-five (decide! (thing :alice :dave daves [{:e :eve :k :softland.store/permission :v eves}]))
        eve-acts (decide! (thing :alice :eve eves [(value-fact :eves-note :text "eve's text")]))
        eves-row (fact-row :alice :eves-note :text)
        never-given (decide! (thing :alice :zed [:zed :alice :alice eves] [{:e :zeds-note :k :text}]))
        granted-elsewhere (decide! (alice [{:e :erin :k :softland.store/permission :v [:erin :alice :team alice-own]}]))
        for-elsewhere (decide! (alice [{:e :erin :k :softland.store/permission :v [:erin :team :alice alice-own]}]))
        at-the-top (decide! (alice [{:e :erin :k :softland.store/permission :v [:erin :alice :alice]}]))
        give-again (decide! (alice [{:e :bob :k :softland.store/permission :v bobs}]))
        end-it (decide! (alice [{:e :bob :k :softland.store/permission}]))
        depot-again (decide! (alice [{:e :alice :k :softland.store/depot :v :streaming}]))
        depot-other (decide! (alice [{:e :alice :k :softland.store/depot :v :microbatch}]))
        depot-retract (decide! (alice [{:e :alice :k :softland.store/depot}]))
        bob-still (decide! (thing :alice :bob bobs [{:e :bobs-note-2 :k :text}]))
        own-give (decide! (thing :alice :frank [:frank :alice :alice alice-own]
                                 [{:e :frank :k :softland.store/permission :v [:frank :alice :alice alice-own]}]))
        with-stale (decide! (alice [{:e :erin :k :softland.store/permission :v [:erin :alice :alice alice-own]}
                                    (assoc (value-fact :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        erin-acts (decide! (thing :alice :erin [:erin :alice :alice alice-own] [{:e :note-5 :k :text}]))
        tools-own (decide! (alice [(value-fact :alice :class "a tool's own class")
                                   (value-fact :bob :permission "a tool's own permission")]))
        bob-after (decide! (thing :alice :bob bobs [{:e :bobs-note-5 :k :text}]))
        unknown-key (decide! (alice [{:e :alice :k :softland.store/owner :v :alice}]))]
    (check "a setting key on another layer is refused, and that layer's depot is as it was"
           [:setting-not-on-the-layer :streaming] [(:word setting) (depot-of-layer :team)])
    (check "an act with a permission for another layer is refused by the form" :permission-not-in-layer (:word other-layer))
    (check "an act under a permission that does not stand is refused" :permission-does-not-stand (:word not-yet))
    (check "a permission given beneath another by its holder is accepted, and sits on its holder as a fact"
           [:accepted bobs] [(:word give) (record/value-of (fact-row :alice :bob :softland.store/permission))])
    (check "a repeat of the refused act reads its recorded refusal, although the act would now pass"
           (:answer not-yet) not-yet-again)
    (check "an act under it, with a value, is accepted: the row holds the text of the value, who wrote it and the permission that was given"
           [:accepted "\"bob's text\"" :bob bobs]
           [(:word bob-writes) (:value bobs-row) (record/who-of bobs-row) (record/permission-of bobs-row)])
    (check "a permission given by one who does not hold the one above is refused: it is not beneath the giver's own"
           :permission-fact-not-beneath-the-acts-own (:word beside))
    (check "an act under a permission by a who that is not its holder is refused" :who-not-the-permissions-holder (:word by-no-holder))
    (check "the refused give gave nothing: an act under that permission is refused" :permission-does-not-stand (:word carol-not-given))
    (check "a chain of four: each holder gives one beneath its own, and the last acts under it"
           [:accepted :accepted :accepted] [(:word give-carol) (:word give-dave) (:word dave-acts)])
    (check "a permission five deep is a permission: the holder of one four deep gives it, it sits on its holder as given, and its holder acts under it with a value, whose row holds the whole permission"
           [:accepted eves :accepted "\"eve's text\"" eves
            "[:eve :alice :alice [:dave :alice :alice [:carol :alice :alice [:bob :alice :alice [:alice :alice :alice]]]]]"]
           [(:word give-five) (record/value-of (fact-row :alice :eve :softland.store/permission)) (:word eve-acts)
            (:value eves-row) (record/permission-of eves-row) (:permission eves-row)])
    (check "an act that names as its own a permission six deep that nobody gave is refused for its standing, with the word of a permission that does not stand: its depth is no fault"
           [:permission-does-not-stand nil] [(:word never-given) (fact-row :alice :zeds-note :text)])
    (check "a granted-in that is not the act's layer is refused" :permission-fact-granted-in-another-layer (:word granted-elsewhere))
    (check "a permission fact for another layer is refused" :permission-fact-for-another-layer (:word for-elsewhere))
    (check "a permission with nothing above it, in a layer that is made, is refused" :permission-fact-not-beneath-the-acts-own (:word at-the-top))
    (check "a permission that stands is not given again: a permission's place takes one write" :store-key-takes-one-write (:word give-again))
    (check "a retract of a permission is refused with the word that says it is not decided" :ending-a-permission-not-decided (:word end-it))
    (check "a depot fact after the making: the same depot, the other depot, a retract"
           [:store-key-takes-one-write :changing-a-depot-not-decided :store-key-retract-not-built]
           [(:word depot-again) (:word depot-other) (:word depot-retract)])
    (check "after each of those refusals the permission stands and the depot is as it was"
           [:accepted :streaming] [(:word bob-still) (depot-of-layer :alice)])
    (check "an act that gives a permission and names that permission as its own is refused: it does not stand before the act"
           :permission-does-not-stand (:word own-give))
    (check "whole or nothing: an act that gives a permission and carries a stale fact is refused, and the permission is not given"
           [:replaced-fact-not-current :permission-does-not-stand nil]
           [(:word with-stale) (:word erin-acts) (fact-row :alice :erin :softland.store/permission)])
    (check "a tool's own facts under the bare words :class and :permission, on the layer's entity and on a holder's, are accepted as facts under plain keys"
           [:accepted true] [(:word tools-own) (:mine? tools-own)])
    (check "each sits at its own place, and its row's value reads back as the tool's value; the store's facts are as they were: the layer's depot, Bob's permission, and Bob still acts"
           [[:value "a tool's own class"] [:value "a tool's own permission"] :streaming bobs :accepted]
           [(record/read-value (fact-row :alice :alice :class)) (record/read-value (fact-row :alice :bob :permission))
            (depot-of-layer :alice) (record/value-of (fact-row :alice :bob :softland.store/permission)) (:word bob-after)])
    (check "a fact under a key in the store's namespace that the store does not know is refused, the refusal is recorded, and nothing is written"
           [:store-key-not-known true nil]
           [(:word unknown-key) (:mine? unknown-key) (fact-row :alice :alice :softland.store/owner)])
    (shown "a setting key on another layer refused:" (told setting))
    (shown "an act with a permission for another layer refused:" (told other-layer))
    (shown "an act under a permission that does not stand refused:" (told not-yet))
    (shown "a permission given beneath another by its holder:" (told give) "| and an act under it accepted:" (told bob-writes))
    (shown "a permission given by one who does not hold the one above refused:" (told beside)
           "| by a who that is not the holder of the permission it names:" (told by-no-holder))
    (shown "a chain four deep stands:" (:word give-carol) (:word give-dave) (:word dave-acts)
           "| a fifth is given and acted under:" (:word give-five) (:word eve-acts) "| the permission in the row of that act:" (:permission eves-row)
           "| an own permission six deep that nobody gave:" (:word never-given))
    (shown "granted-in must be the act's layer:" (:word granted-elsewhere) "| a permission's place takes one write:" (:word give-again)
           "| ending a permission:" (:word end-it) "| a depot after the making:" (:word depot-again) (:word depot-other) (:word depot-retract))
    (shown "whole or nothing: a give with a stale fact" (:word with-stale) "| an act under the permission that was not given" (:word erin-acts))
    (shown "a tool's own facts under the bare words :class and :permission, accepted as facts under plain keys:" (told tools-own)
           "| depot-of-layer :alice =" (depot-of-layer :alice) "| an act under Bob's permission after:" (:word bob-after)
           "| a key in softland.store that the store does not know refused:" (told unknown-key))))

(deftest a-permission-deeper-than-four
  (let [made (decide! (making :deep :alice :alice))
        chain (chain-of-permissions :deep [:alice :deep :deep] "holder" 33)
        at (fn [depth] (nth chain (dec depth)))
        holder (fn [depth] (first (at depth)))
        on-its-holder (fn [depth] (record/value-of (fact-row :deep (holder depth) :softland.store/permission)))
        gives (mapv (fn [depth] (decide! (thing :deep (holder (dec depth)) (at (dec depth))
                                                [{:e (holder depth) :k :softland.store/permission :v (at depth)}])))
                    (range 2 34))
        give-33 (peek gives)
        under-5 (decide! (thing :deep (holder 5) (at 5) [(value-fact :deep-note-5 :text "under depth 5")]))
        under-32 (decide! (thing :deep (holder 32) (at 32) [(value-fact :deep-note-32 :text "under depth 32")]))
        row-5 (fact-row :deep :deep-note-5 :text)
        row-32 (fact-row :deep :deep-note-32 :text)
        own-33 (decide! (thing :deep (holder 33) (at 33) [{:e :deep-note-33 :k :text}]))
        acts-too [:seer-32 :deep :deep (at 31) :see-and-act]
        sees [:looker-32 :deep :deep (at 31) :see]
        give-acts-too (decide! (thing :deep (holder 31) (at 31) [{:e :seer-32 :k :softland.store/permission :v acts-too}]))
        give-sees (decide! (thing :deep (holder 31) (at 31) [{:e :looker-32 :k :softland.store/permission :v sees}]))
        seer-acts (decide! (thing :deep :seer-32 acts-too [(value-fact :seers-note :text "under depth 32, with the slot")]))
        looker-acts (decide! (thing :deep :looker-32 sees [{:e :lookers-note :k :text}]))
        slot-at-33 (decide! (thing :deep (holder 32) (at 32)
                                   [{:e :seer-33 :k :softland.store/permission :v [:seer-33 :deep :deep (at 32) :see-and-act]}]))]
    (check "a layer for these checks is made, and its first permission, the first of the proof's chain and of depth 1, sits on its holder"
           [:accepted [:alice :deep :deep] [:alice :deep :deep]] [(:word made) (at 1) (on-its-holder 1)])
    (check "each holder gives a permission beneath her own, one at each depth from 2 to 32: the 31 gives are each accepted"
           (vec (repeat 31 :accepted)) (mapv :word (pop gives)))
    (check "the permissions of depth 4, 5, 31 and 32 each sit on their holder as they were given"
           (mapv at [4 5 31 32]) (mapv on-its-holder [4 5 31 32]))
    (check "the give at depth 33 is refused as malformed: the refusal is recorded under its own act-id with the digest of what was sent, and nothing sits on that holder"
           [:malformed true nil] [(:word give-33) (:mine? give-33) (fact-row :deep (holder 33) :softland.store/permission)])
    (check "an act under the permission of depth 5, and one under the permission of depth 32, each with a value, are accepted"
           [:accepted :accepted "\"under depth 5\"" "\"under depth 32\""]
           [(:word under-5) (:word under-32) (:value row-5) (:value row-32)])
    (check "the row of each holds the whole permission that the act named: it reads back as that permission, its text opens 5 and 32 vectors, and no part of it is cut for its depth"
           [(at 5) (at 32) 5 32 false]
           [(record/permission-of row-5) (record/permission-of row-32)
            (count (filter #{\[} (:permission row-5))) (count (filter #{\[} (:permission row-32)))
            (str/includes? (:permission row-32) "#too-deep")])
    (check "an act that names as its own a permission of depth 33, deeper than any that was given, is refused as malformed, and writes nothing"
           [:malformed true nil] [(:word own-33) (:mine? own-33) (fact-row :deep :deep-note-33 :text)])
    (check "a permission of depth 32 with the fifth slot is given, with each of the slot's two words: the row holds the slot; the holder of see and act acts with a value, and the holder of see cannot act"
           [:accepted :accepted :see-and-act :see :accepted "\"under depth 32, with the slot\"" :permission-does-not-allow-acting]
           [(:word give-acts-too) (:word give-sees)
            (act/allows (record/value-of (fact-row :deep :seer-32 :softland.store/permission)))
            (act/allows (record/value-of (fact-row :deep :looker-32 :softland.store/permission)))
            (:word seer-acts) (:value (fact-row :deep :seers-note :text)) (:word looker-acts)])
    (check "a give of depth 33 with the fifth slot is refused as malformed" :malformed (:word slot-at-33))
    (shown "a permission deeper than four, through the streaming gate: the gives at depth 2 to 32:" (pr-str (frequencies (map :word (pop gives))))
           "| the give at depth 33:" (told give-33) "| an act under depth 5:" (told under-5) "| under depth 32:" (told under-32))
    (shown "the row of the act under depth 32 holds its permission whole:" (count (:permission row-32)) "characters of text that open"
           (count (filter #{\[} (:permission row-32))) "vectors | an own permission of depth 33:" (told own-33)
           "| the fifth slot at depth 32:" (:word give-acts-too) (:word give-sees) "| its holders act:" (:word seer-acts) (:word looker-acts)
           "| the fifth slot at depth 33:" (:word slot-at-33))))

(deftest the-time-on-the-act
  (let [t chosen-time
        hour 3600000
        layers (mapv #(keyword (str "timed-" %)) (range 5))
        own (fn [layer] [:alice layer layer])
        act-in (fn [layer e] (thing layer :alice (own layer) [(value-fact e :text (str "the text of " (name e)))]))
        makings (mapv (fn [layer] (decide! (at-time (making layer :alice :alice) t))) layers)
        ;; Five layers on four tasks: two of them are on one task. The pair
        ;; is found in the gate's own table: the task of the one holds a
        ;; last stamp of the other.
        on-one-task (first (for [a layers
                                 b layers
                                 :when (and (not= a b) (some? (last-stamp b a)))]
                             [a b]))
        [l1 l2] on-one-task
        same-1 (decide! (at-time (act-in l1 :same-1) t))
        same-2 (decide! (at-time (act-in l1 :same-2) t))
        same-2-row (fact-row l1 :same-2 :text)
        other-before [(last-stamp l2) (last-stamp l2 l1)]
        other-1 (decide! (at-time (act-in l2 :other-1) t))
        lasts-then [(last-stamp l1) (last-stamp l2) (last-stamp l1 l2) (last-stamp l2 l1)]
        ;; Acts whose times go forward, back, and to the earliest there is.
        moved-times [[:later (inc t)] [:earlier (- t 5000)] [:at-zero 0]]
        moved (mapv (fn [[e millis]] (decide! (at-time (act-in l1 e) millis))) moved-times)
        no-times [(dissoc (act-in l1 :no-time) declaration/time-part)
                  (at-time (act-in l1 :nil-time) nil)
                  (at-time (act-in l1 :time-below-zero) -1)
                  (at-time (act-in l1 :time-no-integer) 1.5)]
        without (mapv decide! no-times)
        last-after-without (last-stamp l1)
        unknown-part (decide! (at-time (assoc (act-in l1 :unknown-part) :a-part-that-no-form-names 1) (+ t hour)))
        other-layers-permission (decide! (at-time (thing l1 :alice (own l2) [(value-fact :another-fault :text "x")]) (+ t hour)))
        stranger (decide! (at-time (thing l1 :mallory [:mallory l1 l1] [(value-fact :a-strangers :text "x")]) (+ t hour)))
        ;; Acts with times far ahead; behind them, two acts with the chosen
        ;; time, the second of which carries the stamp of the first one's
        ;; fact and replaces that fact.
        far-times [[:an-hour-ahead (+ t hour)] [:in-3084 year-3084] [:at-the-greatest Long/MAX_VALUE]]
        far (mapv (fn [[e millis]] (decide! (at-time (act-in l1 e) millis))) far-times)
        written-after (decide! (at-time (act-in l1 :written-after) t))
        fact-after [(:act-id (:thing written-after)) 0]
        names-it (decide! (at-time (thing l1 :alice (own l1)
                                          [(assoc (value-fact :written-after :text "the text that replaces it") :replaces fact-after)]
                                          {:based-on {fact-after (:stamp (:answer written-after))}})
                                   t))
        names-it-row (fact-row l1 :written-after :text)
        in-l1 (vec (concat [(get (zipmap layers makings) l1) same-1 same-2] moved without
                           [unknown-part other-layers-permission stranger] far [written-after names-it]))
        lasts-at-the-end [(last-stamp l1) (last-stamp l2)]
        as-told (fn [results] (pr-str (mapv (juxt :word (comp :stamp :answer) (comp :time :answer)) results)))]
    (swap! kept assoc :timed {:layers [l1 l2] :last-stamps lasts-at-the-end})
    (check "a. five layers are made by hand, each with the one chosen time on its making act: each is accepted, and the stamp of each is 0, the first stamp of its layer; so five layers hold one stamp. The answer of each holds the chosen time"
           [(vec (repeat 5 :accepted)) #{0} #{t}]
           [(mapv :word makings) (set (map (comp :stamp :answer) makings)) (set (map (comp :time :answer) makings))])
    (check "a. a second act into one of them, with the same time on it, gets the stamp 1, and a third the stamp 2: one more for each decision of the layer. The row of the third's fact holds its stamp and the time on its act"
           [[:accepted :accepted] [1 2] [2 t]]
           [(mapv :word [same-1 same-2]) (mapv (comp :stamp :answer) [same-1 same-2]) [(:stamp same-2-row) (:time same-2-row)]])
    (check "b. two of the five layers are on one task, and the three decisions in the one did not raise the other: its last stamp is still its making act's, 0, read on its own task and on the task of the first, which holds it too"
           [true [0 0]] [(some? on-one-task) other-before])
    (check "b. an act into the other layer gets the stamp 1, that layer's own count, while the first layer of the task stands at 2: two layers of one task each hold an answer with the stamp 1"
           [:accepted 1 1]
           [(:word other-1) (:stamp (:answer other-1)) (:stamp (:answer same-1))])
    (check "b. the task keeps a last stamp for each of the two layers, each its own, whichever of the two layers the read is routed by"
           [2 1 2 1] lasts-then)
    (check "c. the time on an act moves no stamp. An act with a time one millisecond after the chosen one, an act with a time 5 seconds before it, and an act with the time 0, the earliest there is, are each accepted with the layer's last stamp plus one: 3, 4 and 5. The clock went back, and the stamps of the layer did not. The answer of each, and the row of its fact, hold the time on its own act"
           [[:accepted :accepted :accepted] [3 4 5] (mapv second moved-times) (mapv second moved-times)]
           [(mapv :word moved) (mapv (comp :stamp :answer) moved) (mapv (comp :time :answer) moved)
            (mapv (fn [[e _]] (:time (fact-row l1 e :text))) moved-times)])
    (check "d. a thing with a usable act-id and no time, one with a nil time, one with a time below zero and one with a time that is no integer: each is refused with the word of the time, under its own act-id with the digest of what was sent, with the layer's last stamp plus one, 6 to 9, which the layer then keeps; none writes a row; and the row of each answer holds no time, so the read gives none"
           [(vec (repeat 4 :no-door-time)) (vec (repeat 4 true)) [6 7 8 9] 9 (vec (repeat 4 nil)) (vec (repeat 4 false)) (vec (repeat 4 nil))]
           [(mapv :word without) (mapv :mine? without) (mapv (comp :stamp :answer) without) last-after-without
            (mapv (fn [x] (fact-row l1 (:e (first (:facts x))) :text)) no-times)
            (mapv #(some-> (answer-row (:thing %)) (contains? :time)) without) (mapv (comp :time :answer) without)])
    (check "d. a refused thing keeps the time that it carried, as a label, and its stamp holds nothing of it. Three things, each with a usable time one hour after the chosen one: one with a part that no form names, whose word comes before the time's in the declaration's order; one whose permission is for another layer, whose word comes after it; and an act of a stranger under a permission that no act gave, which the declaration takes and the gate refuses for its standing. Each gets the layer's last stamp plus one, 10, 11 and 12, and its answer holds the time one hour ahead"
           [[:unknown-part 10 (+ t hour)] [:permission-not-in-layer 11 (+ t hour)] [:permission-does-not-stand 12 (+ t hour)]]
           (mapv (fn [result] [(:word result) (:stamp (:answer result)) (:time (:answer result))])
                 [unknown-part other-layers-permission stranger]))
    (check "e. no time is too far ahead: an act with a time one hour after the chosen one, one with a time in the year 3084, 2^45 milliseconds, and one with the greatest fixed-width integer, 2^63 less one, are each accepted with the layer's last stamp plus one, 13, 14 and 15; the answer of each, and the row of its fact, hold its own time"
           [[:accepted :accepted :accepted] [13 14 15] (mapv second far-times) (mapv second far-times)]
           [(mapv :word far) (mapv (comp :stamp :answer) far) (mapv (comp :time :answer) far)
            (mapv (fn [[e _]] (:time (fact-row l1 e :text))) far-times)])
    (check "e. a time far ahead on an accepted act holds back no later act. Behind those three, an act with the chosen time on it writes a fact, with the stamp 16; and an act with the chosen time on it that carries the stamp of that fact in `:based-on`, and names the fact as the one that it replaces, is accepted with the stamp 17. Its fact is then the place's, with that stamp and the chosen time"
           [[:accepted 16] [:accepted 17] [(nth (:act-id (:thing names-it)) 2) 17 t]]
           [[(:word written-after) (:stamp (:answer written-after))] [(:word names-it) (:stamp (:answer names-it))]
            [(:by-act names-it-row) (:stamp names-it-row) (:time names-it-row)]])
    (check "the stamps of the eighteen decisions of the first layer, in the order sent, accepts and refusals alike, are 0 to 17 with no gap; at the end each of the two layers of one task keeps the stamp of its own last decision, 17 and 1"
           [(vec (range 18)) [17 1]]
           [(mapv (comp :stamp :answer) in-l1) lasts-at-the-end])
    (shown "the time on the act is a label beside the stamp, in the streaming gate. Five layers made with the chosen time" t
           "on their making acts, each with the stamp 0 | in one of them the same time again:" (told same-1) "|" (told same-2)
           "| a later time, a time 5 s earlier, and the time 0, as [word stamp time]:" (as-told moved))
    (shown "two layers of one task count apart," l1 "and" l2 "| after three decisions in the first the other stood at" (pr-str other-before)
           "| its next act:" (told other-1) "| the last stamps that the task keeps, of the first and of the other, read by each layer:" (pr-str lasts-then))
    (shown "a thing with no usable time is refused, with the layer's last stamp plus one and no time in its answer: no time, nil, below zero, no integer:"
           (as-told without) "| refused for another fault, with a time one hour ahead, which its answer keeps:"
           (as-told [unknown-part other-layers-permission stranger]))
    (shown "times far ahead, accepted: an hour, the year 3084, 2^63 less one:" (as-told far)
           "| behind them, an act at the chosen time:" (told written-after) "| one that carries its stamp and replaces its fact:" (told names-it)
           "| the stamps of the first layer, in the order sent:" (pr-str (mapv (comp :stamp :answer) in-l1)))))

(deftest strays-and-limits
  (let [counts (fn [] (into {} (map (fn [w] [w (strays w)])) declaration/stray-reasons))
        before (counts)
        long (keyword (apply str (repeat 300 "a")))
        no-act-id 5
        malformed {:act-id [:alice :streaming] :layer :alice :marker (UUID/randomUUID)}
        other-gate (assoc (alice [{:e :note-30 :k :text}]) :act-id [:alice :microbatch (UUID/randomUUID)] :depot :microbatch)
        other-layer (assoc (alice [{:e :note-30 :k :text}]) :layer :team)
        too-long {:act-id [long :streaming (UUID/randomUUID)] :layer long}
        stamp-before-strays (last-stamp :alice)
        acks (mapv send! [no-act-id malformed other-gate other-layer too-long other-gate other-gate])
        after (counts)
        second-of-a-word (do (send! (assoc malformed :marker (UUID/randomUUID))) (counts))
        stamp-after-strays (last-stamp :alice)
        many (fn [n] (alice (map (fn [i] {:e (keyword (str "many-" i)) :k :text}) (range n))))
        facts-256 (decide! (many 256))
        facts-257 (decide! (many 257))
        based-on (fn [n] (into {} (map (fn [i] [[[:alice :streaming (UUID. 0 1)] i] 1])) (range n)))
        based-1024 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1024)}))
        based-1025 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1025)}))
        value-over (decide! (alice [(value-fact :note-32 :text over-the-bound-of-a-value)]))
        over-row (fact-row :alice :note-32 :text)
        value-at (decide! (alice [(value-fact :note-32 :text at-the-bound-of-a-value)]))
        at-row (fact-row :alice :note-32 :text)
        act-over (decide! (act-over-its-bound :alice))
        act-over-bytes (* 256 (+ 65536 (count (utf8-of (pr-str (:who (:thing act-over)))))
                                 (count (utf8-of (pr-str (:permission (:thing act-over)))))))
        records-then (vec (stored))
        value-over-logged (record-of-act records-then (:act-id (:thing value-over)))
        act-over-logged (record-of-act records-then (:act-id (:thing act-over)))
        long-entity (decide! (alice [{:e long :k :text}]))
        ;; What an act carries is no measure of its stamp: a claim far
        ;; ahead, and the greatest stamp there is in `:based-on`.
        last-before-the-carried (last-stamp :alice)
        fact-id [[:alice :streaming (UUID. 0 1)] 0]
        claim (decide! (alice [{:e :note-33 :k :text}] {:claimed-when Long/MAX_VALUE}))
        carries-the-greatest (decide! (alice [{:e :note-34 :k :text}]
                                             {:based-on {fact-id Long/MAX_VALUE} :because-of (new-act-id :alice)}))
        next-one (decide! (alice [{:e :note-35 :k :text}]))]
    (check "each stray was acked" (vec (repeat 7 :acked)) acks)
    (check "a stray is kept under its word, once however often it comes: five different strays, one of them sent three times"
           {:no-act-id 1 :act-id-malformed 1 :id-too-long 1 :act-id-of-the-other-gate 1 :act-id-of-another-layer 1}
           (merge-with - after before))
    (check "what is kept is the digest of the stray thing"
           [true true] [(contains? (stray-digests :act-id-of-the-other-gate) (digest/hex-of other-gate))
                        (contains? (stray-digests :no-act-id) (digest/hex-of no-act-id))])
    (check "the count of strays under a word is the number of different ones: a second, different one makes it two more than before"
           2 (- (:act-id-malformed second-of-a-word) (:act-id-malformed before)))
    (check "a stray gets no answer: an act-id of the other gate, and an act-id whose layer is not the thing's layer"
           [nil nil] [(answer-of (:act-id other-gate)) (answer-of (:act-id other-layer))])
    (check "a stray writes no fact" nil (fact-row :alice :note-30 :text))
    (check "a stray gets no stamp: after the eight strays the last stamp of the layer :alice is as it was before them"
           stamp-before-strays stamp-after-strays)
    (check "an act of 256 facts is accepted, and each fact is a row; one of 257 is refused"
           [:accepted 255 :too-many-facts] [(:word facts-256) (:by-index (fact-row :alice :many-255 :text)) (:word facts-257)])
    (check "1,024 entries of based-on are taken; 1,025 are refused" [:accepted :too-many-based-on] [(:word based-1024) (:word based-1025)])
    (check "the proof's two strings at the bound of one value, each in its quotes, are 65,536 and 65,537 bytes of UTF-8 by Java's own count, and 21,848 and 21,849 characters"
           [65536 65537 21848 21849]
           [(count (utf8-of (str "\"" at-the-bound-of-a-value "\""))) (count (utf8-of (str "\"" over-the-bound-of-a-value "\"")))
            (+ 2 (count at-the-bound-of-a-value)) (+ 2 (count over-the-bound-of-a-value))])
    (check "a value whose one text is 65,537 bytes is refused by the gate with the word of the bound on one value: the refusal is recorded under its own act-id with the digest of what was sent, no row is written, and the depot's log holds the act whole, the value plain"
           [:acked :value-too-large true nil true]
           [(:ack value-over) (:word value-over) (:mine? value-over) over-row (= (:thing value-over) value-over-logged)])
    (check "a value whose one text is 65,536 bytes is accepted at the same place, and its row holds those bytes: the string in its quotes, which reads back as the value"
           [:accepted 65536 21848 true true]
           [(:word value-at) (count (value-bytes at-row)) (count (:value at-row))
            (= (str "\"" at-the-bound-of-a-value "\"") (:value at-row))
            (= [:value at-the-bound-of-a-value] (record/read-value at-row))])
    (check "an act of 256 values at that bound, under a who and a permission of more than 8,192 bytes of text, would write more than 18,874,368 bytes into the rows of its facts, counted by the proof with Clojure's printer: it is refused with the word of the bound on an act, the refusal recorded under its own act-id; the depot's log holds it whole, its 256 values plain; and no row of it is written"
           [true :acked :act-too-large true true 256 nil nil]
           [(< 18874368 act-over-bytes) (:ack act-over) (:word act-over) (:mine? act-over) (= (:thing act-over) act-over-logged)
            (count (:facts act-over-logged)) (fact-row :alice :over-0 :text) (fact-row :alice :over-255 :text)])
    (check "an id that prints to more than 256 characters is refused, and the refusal is recorded under the act-id" :id-too-long (:word long-entity))
    (check "claimed-when far ahead is a claim: the act is accepted, and its stamp does not follow it: it is the layer's last stamp plus one"
           [:accepted (inc last-before-the-carried)] [(:word claim) (:stamp (:answer claim))])
    (check "an act that carries the greatest stamp there is, 2^63 less one, in `:based-on`, based on a fact and because of an act that the record does not hold, is accepted with the layer's last stamp plus one: a gate raises no act's stamp above the stamps that it carries"
           [:accepted (+ 2 last-before-the-carried)] [(:word carries-the-greatest) (:stamp (:answer carries-the-greatest))])
    (check "and the next stamp of the layer is one more; it is the layer's last stamp"
           [(+ 3 last-before-the-carried) (+ 3 last-before-the-carried)] [(:stamp (:answer next-one)) (last-stamp :alice)])
    (shown "a stray is kept under its word and gets no answer; strays seen, by word:" (pr-str (merge-with - after before))
           "| the same stray sent three times counts once | answer of a stray:" (pr-str (answer-of (:act-id other-gate))))
    (shown "a thing over each limit is refused:" (:word facts-257) (:word based-1025) (:word value-over) (:word act-over) (:word long-entity)
           "| at each limit accepted:" (:word facts-256) (:word based-1024) (:word value-at))
    (shown "the bound on one value, through the streaming gate: a text of 65,537 bytes" (told value-over) "| of 65,536 bytes" (told value-at)
           "| its row holds" (count (value-bytes at-row)) "bytes in" (count (:value at-row)) "characters")
    (shown "the bound on an act, through the streaming gate: an act whose rows would hold" act-over-bytes "bytes of text, over 18874368:"
           (told act-over) "| in the log whole, with" (count (:facts act-over-logged)) "values plain")
    (shown "what an act carries is no measure of its stamp: the layer's last stamp before" last-before-the-carried
           "| claimed-when far ahead:" (told claim) "| carried the greatest stamp, 2^63 less one:" (told carries-the-greatest)
           "| the next stamp of the layer:" (:stamp (:answer next-one)))))

(deftest the-reads-take-anything
  (let [u (UUID/randomUUID)
        long (keyword (apply str (repeat 300 "a")))
        accepted (:act-id (:thing (:landed @kept)))
        ask (fn [query & args] (apply rama/foreign-invoke-query (get *hand* query) args))]
    (check "the hand gives nothing itself for what is no id or no act-id, and calls no query"
           [nil nil nil nil]
           [(depot-of-layer nil) (depot-of-layer "alice") (answer-of nil) (answer-of [:alice :streaming])])
    (check "the read by act-id, called with what is no act-id of the layer given, gives nothing"
           [nil nil nil nil nil true]
           [(ask :answer :alice [:team :streaming u]) (ask :answer :alice 5) (ask :answer :alice [:alice :streaming])
            (ask :answer "alice" ["alice" :streaming u]) (ask :answer long [long :streaming u])
            (some? (ask :answer :alice accepted))])
    (check "an act-id of the depot microbatch under a streaming layer, asked for with its layer as the routing id, gets nothing: the microbatch gate's answers hold no row under it" nil (ask :answer :alice [:alice :microbatch (nth accepted 2)]))
    (check "the read of a depot, called with what is no id, or with a layer that is not made, gives nothing"
           [nil nil nil] [(ask :depot-of-layer "alice") (ask :depot-of-layer long) (ask :depot-of-layer :nobody)])
    (check "the same two reads give an answer and a depot for what is one: the act-id of an accepted act with its layer, and a layer that is made"
           [:accepted :streaming] [(:verdict (ask :answer :alice accepted)) (ask :depot-of-layer :alice)])
    (check "the same uuid in two layers is two answers: an answer is found by its layer and its uuid"
           [nil nil] [(ask :answer :team [:team :streaming (nth accepted 2)]) (answer-of [:team :streaming (nth accepted 2)])])
    (shown "the two reads, of an answer and of a depot, give nothing for what is no id, no act-id, another layer's act-id or the other depot, and do not fail"
           "| for an act-id that was accepted and a layer that is made they give:" (:verdict (ask :answer :alice accepted)) (ask :depot-of-layer :alice))))

;; ------------------------------------ what the streaming gate keeps for a read

(deftest what-a-fact-keeps
  (let [made (decide! (making :canvas :alice :alice))
        facts (mapv (fn [[k v]] (value-fact :kept k v)) kinds-of-value)
        landed (decide! (canvas-act facts))
        rows (into {} (map (fn [k] [k (fact-row :canvas :kept k)])) (keys kinds-of-value))
        logged (record-of-act (stored) (:act-id (:thing landed)))
        logged-as-given (:v (some (fn [fact] (when (= :as-given (:k fact)) fact)) (:facts logged)))
        edges (decide! (canvas-act (mapv (fn [[k [v _]]] (value-fact :edges k v)) edges-of-a-value)))
        edge-rows (into {} (map (fn [k] [k (fact-row :canvas :edges k)])) (keys edges-of-a-value))
        inside-33 (decide! (canvas-act [(value-fact :edges :inside-33 (nested 33 1))]))
        refused (decide! (canvas-act [(value-fact :not-kept :text "a good value, and then dropped")
                                      (assoc (value-fact :not-kept-2 :text "x") :replaces [(new-act-id :canvas) 0])]))
        graces [:grace :canvas :canvas alice-in-canvas]
        give (decide! (canvas-act [{:e :grace :k :softland.store/permission :v graces}]))
        grace-writes (decide! (thing :canvas :grace graces [(value-fact :graces-note :text "grace's text")]))
        retract (decide! (thing :canvas :grace graces [{:e :graces-note :k :gone}]))
        graces-row (fact-row :canvas :graces-note :text)
        retract-row (fact-row :canvas :graces-note :gone)
        give-row (fact-row :canvas :grace :softland.store/permission)
        personal-depot (fact-row :alice :alice :softland.store/depot)
        personal-permission (fact-row :alice :alice :softland.store/permission)]
    (swap! kept assoc-in [:canvas-before :kept-value] (record/read-value (get rows :map)))
    (check "a layer for these checks is made, and an act with a plain value of each kind is accepted"
           [:accepted :accepted true] [(:word made) (:word landed) (:mine? landed)])
    (check "each row holds the one text of its value in its normal form, as the proof wrote it out by hand: a keyword, a UUID, a string with spaces, a number, a map, and a map that was not given in its normal form"
           texts-of-kinds (into {} (map (fn [[k row]] [k (:value row)])) rows))
    (check "each row gives back the UTF-8 of that text, and holds seven fields and no other; its time is the time on the act"
           [(into {} (map (fn [[k t]] [k (utf8-of t)])) texts-of-kinds) #{#{:value :by-act :by-index :who :permission :stamp :time}}
            #{(time-on (:thing landed))}]
           [(into {} (map (fn [[k row]] [k (value-bytes row)])) rows) (set (map (comp set keys) (vals rows)))
            (set (map :time (vals rows)))])
    (check "each row reads back as the value in its normal form: the value that the maker gave, for each that was given so"
           (into {} (map (fn [[k v]] [k [:value v]])) kinds-read-back)
           (into {} (map (fn [[k row]] [k (record/read-value row)])) rows))
    (check "the act in the depot's log is as the hand sent it: each fact holds its value plain, as given and not in its normal form, so the decimal keeps its trailing zero and the big integer is no fixed-width one"
           [facts "1.50" false]
           [(vec (:facts logged)) (str (:b logged-as-given)) (instance? Long (get-in logged-as-given [:a 1]))])
    (check "false, an empty string and an empty map are values and no retract, and a number inside 32 vectors is a value: the act is accepted, each row holds the text of its value, and each reads back as the value"
           [:accepted
            (into {} (map (fn [[k [_ t]]] [k t])) edges-of-a-value)
            (into {} (map (fn [[k [v _]]] [k [:value v]])) edges-of-a-value)]
           [(:word edges)
            (into {} (map (fn [[k row]] [k (:value row)])) edge-rows)
            (into {} (map (fn [[k row]] [k (record/read-value row)])) edge-rows)])
    (check "a number inside 33 vectors is no value: the act is refused as malformed, the refusal is recorded, and no row is written"
           [:malformed true nil] [(:word inside-33) (:mine? inside-33) (fact-row :canvas :edges :inside-33)])
    (check "whole or nothing: an act whose first fact is good and whose second names a fact that is not current is refused whole, with no row at either place"
           [:replaced-fact-not-current nil nil]
           [(:word refused) (fact-row :canvas :not-kept :text) (fact-row :canvas :not-kept-2 :text)])
    (check "each row holds who wrote it and the permission that the act named: the maker of the layer under her first permission"
           #{[:alice alice-in-canvas]} (set (map (fn [row] [(record/who-of row) (record/permission-of row)]) (vals rows))))
    (check "a second holder, given a permission in the kept form without the slot, acts: her rows hold her and her permission; a retract's row holds them too; the give's row holds the giver and the giver's permission"
           [:accepted :accepted :accepted
            [:grace graces [:value "grace's text"]] [:grace graces [:nothing-here]] [:alice alice-in-canvas [:value graces]]]
           [(:word give) (:word grace-writes) (:word retract)
            [(record/who-of graces-row) (record/permission-of graces-row) (record/read-value graces-row)]
            [(record/who-of retract-row) (record/permission-of retract-row) (record/read-value retract-row)]
            [(record/who-of give-row) (record/permission-of give-row) (record/read-value give-row)]])
    (check "the system made Alice's personal layer for her: the rows of that making hold the system as who, and Alice's permission; the two differ"
           [[:system alice-own] [:system alice-own]]
           [[(record/who-of personal-depot) (record/permission-of personal-depot)]
            [(record/who-of personal-permission) (record/permission-of personal-permission)]])
    (shown "a value of each kind, kept by the streaming gate as its one text in its row:"
           (pr-str (into (sorted-map) (map (fn [[k row]] [k (:value row)])) rows)))
    (shown "the act in the depot's log holds each value plain, as given: the map that was not in its normal form is" (pr-str logged-as-given)
           "in the log and" (:value (get rows :as-given)) "in its row")
    (shown "the edges of a value, each accepted with its text in its row:" (pr-str (into (sorted-map) (map (fn [[k row]] [k (:value row)])) edge-rows))
           "| a number inside 33 vectors:" (told inside-33))
    (shown "who wrote a fact, and under which permission:" (pr-str [(record/who-of graces-row) (record/permission-of graces-row)])
           "| the system's making of a personal layer:" (pr-str [(record/who-of personal-depot) (record/permission-of personal-depot)]))
    (shown "an act refused for its second fact leaves no row of its first:" (told refused))))

(deftest the-keys-that-take-a-second-write
  (let [team (fn [facts] (thing :team :alice alice-in-team facts))
        walk (fn [k v1 v2]
               (let [before (fact-row :team :team k)
                     first-write (decide! (team [{:e :team :k k :v v1}]))
                     row-1 (fact-row :team :team k)
                     second-write (decide! (team [{:e :team :k k :v v2}]))
                     row-2 (fact-row :team :team k)
                     named (decide! (team [{:e :team :k k :v v1 :replaces [(:act-id (:thing second-write)) 0]}]))
                     row-3 (fact-row :team :team k)
                     stale (decide! (team [{:e :team :k k :v v2 :replaces [(:act-id (:thing first-write)) 0]}]))
                     retract (decide! (team [{:e :team :k k}]))
                     elsewhere (decide! (team [{:e :plan :k k :v v1}]))]
                 {:before before :starts-with (first-act/starts-with :team k)
                  :words (mapv :word [first-write second-write named stale retract elsewhere])
                  :values (mapv record/read-value [row-1 row-2 row-3 (fact-row :team :team k)])
                  :written-by (mapv :by-act [row-1 row-2 row-3 (fact-row :team :team k)])
                  :acts (mapv #(nth (:act-id (:thing %)) 2) [first-write second-write named])
                  :stamps (mapv #(:stamp (:answer %)) [first-write second-write named])
                  :level-since (mapv record/marked-keys-of [row-1 row-2 row-3])
                  :who (record/who-of row-3) :permission (record/permission-of row-3)
                  :elsewhere-row (fact-row :team :plan k)}))
        seen (walk who-may-see :everyone :members)
        listed (walk builds-on [:base :alice] [])
        marked (walk marks #{:plan} #{})
        wrong (mapv (fn [fact] (:word (decide! (team [fact]))))
                    [{:e :team :k who-may-see :v :owner}
                     {:e :team :k builds-on :v #{:base}}
                     {:e :team :k builds-on :v (mapv #(keyword (str "layer-" %)) (range 65))}
                     {:e :team :k marks :v [:plan]}
                     {:e :team :k marks :v (set (map #(keyword (str "key-" %)) (range 33)))}
                     {:e :team :k marks :v #{:softland.store/permission}}])
        at-the-limits (decide! (team [{:e :team :k builds-on :v (mapv #(keyword (str "layer-" %)) (range 64))}
                                      {:e :team :k marks :v (set (map #(keyword (str "key-" %)) (range 32)))}]))
        read-at-the-limits [(count (second (record/read-value (fact-row :team :team builds-on)))) (count (marked-keys :team))]
        odd-list (decide! (team [{:e :team :k builds-on :v [:team :base :base :nobody-made-this]}]))
        odd-list-read (record/read-value (fact-row :team :team builds-on))
        bob-in-team [:bob :team :team alice-in-team]
        give (decide! (team [{:e :bob :k :softland.store/permission :v bob-in-team}]))
        give-again (decide! (team [{:e :bob :k :softland.store/permission :v bob-in-team}]))
        depot-again (decide! (team [{:e :team :k :softland.store/depot :v :streaming}]))
        by-bob (decide! (thing :team :bob bob-in-team [{:e :team :k who-may-see :v :everyone}]))
        not-made (decide! (thing :no-such-layer :alice [:alice :no-such-layer :no-such-layer]
                                 [{:e :no-such-layer :k who-may-see :v :everyone}]))
        making-with-a-third (decide! (update (making :third-fact :alice :alice) :facts conj
                                             {:e :third-fact :k who-may-see :v :everyone}))
        expected-words [:accepted :accepted :accepted :replaced-fact-not-current :store-key-retract-not-built
                        :setting-not-on-the-layer]]
    (check "before any fact says it, the place of each of the three keys holds no row, and the layer reads as: seen by its members, building on nothing, marking no key"
           [[nil nil nil] [:members [] #{}]]
           [(mapv :before [seen listed marked]) (mapv :starts-with [seen listed marked])])
    (check "who may see a layer: written, written a second time, written again naming the fact that is there; then a stale replaces, a retract and a write on another entity are each refused with a recorded word"
           expected-words (:words seen))
    (check "what a layer builds on: the same" expected-words (:words listed))
    (check "the keys that a layer marks: the same" expected-words (:words marked))
    (check "after each accepted write the place holds that write's value, by that act; after the three refusals it holds the last accepted one"
           [[[:value :everyone] [:value :members] [:value :everyone] [:value :everyone]]
            [[:value [:base :alice]] [:value []] [:value [:base :alice]] [:value [:base :alice]]]
            [[:value #{:plan}] [:value #{}] [:value #{:plan}] [:value #{:plan}]]
            true]
           [(:values seen) (:values listed) (:values marked)
            (every? (fn [walked] (= (conj (:acts walked) (last (:acts walked))) (:written-by walked))) [seen listed marked])])
    (check "the refused write on another entity wrote nothing there" [nil nil nil] (mapv :elsewhere-row [seen listed marked]))
    (check "the row of a setting holds who wrote it and under which permission" [[:alice alice-in-team] [:alice alice-in-team] [:alice alice-in-team]]
           (mapv (juxt :who :permission) [seen listed marked]))
    (check "the row of the marked keys also holds, derived by the gate, the stamp of each marked key: the stamp of the act that marked it; none when the set is empty; and a new stamp when the key is marked again"
           [{:plan (nth (:stamps marked) 0)} {} {:plan (nth (:stamps marked) 2)} true]
           (conj (:level-since marked) (< (nth (:stamps marked) 0) (nth (:stamps marked) 2))))
    (check "the rows of the setting and of the list hold no stamps of marked keys" [[{} {} {}] [{} {} {}]]
           [(:level-since seen) (:level-since listed)])
    (check "a wrong value under each key is refused by the declaration: a word that is neither; a set for a list; 65 layers; a vector for the marks; 33 keys; a key of the store's own namespace"
           [:who-may-see-fact-not-members-or-everyone :builds-on-fact-not-a-list-of-layers :builds-on-too-many-layers
            :marked-keys-fact-not-a-set-of-keys :too-many-marked-keys :marked-key-in-the-stores-namespace]
           wrong)
    (check "64 layers and 32 marked keys in one act are accepted; a list that names the layer itself, a layer twice and a layer that nobody made is taken as written"
           [:accepted [64 32] :accepted [:value [:team :base :base :nobody-made-this]]]
           [(:word at-the-limits) read-at-the-limits (:word odd-list) odd-list-read])
    (check "a permission's place and the layer's depot still take one write"
           [:accepted :store-key-takes-one-write :store-key-takes-one-write]
           [(:word give) (:word give-again) (:word depot-again)])
    (check "who may write the three is whoever may act in the layer, kept and not decided: a second holder of the layer sets it to everyone"
           [:accepted [:value :everyone] :bob] [(:word by-bob) (record/read-value (fact-row :team :team who-may-see))
                                               (record/who-of (fact-row :team :team who-may-see))])
    (check "a setting into a layer that is not made is refused as any act into it; a making act with a third fact, a setting, is no making act: nothing is written at the making but the depot and the first permission"
           [:layer-not-made :layer-not-made nil nil]
           [(:word not-made) (:word making-with-a-third) (depot-of-layer :third-fact) (fact-row :third-fact :third-fact who-may-see)])
    (shown "the three keys that take a second write, in a streaming layer. For each: written, written again, written again naming the fact there, a stale replaces, a retract, another entity:"
           (pr-str {who-may-see (:words seen) builds-on (:words listed) marks (:words marked)}))
    (shown "no fact reads as:" (pr-str (mapv :starts-with [seen listed marked])) "| a wrong value under each:" (pr-str wrong))
    (shown "the stamps of the marked keys, derived by the gate: after the first write" (pr-str (nth (:level-since marked) 0))
           "after the set was emptied" (pr-str (nth (:level-since marked) 1)) "after the key was marked again" (pr-str (nth (:level-since marked) 2)))
    (shown "a permission a second time:" (:word give-again) "| a depot after the making:" (:word depot-again)
           "| a second holder of the layer writes who may see it:" (:word by-bob))))

(deftest what-a-permission-allows
  (let [bob-sees [:bob :canvas :canvas alice-in-canvas :see]
        bob-without-the-slot [:bob :canvas :canvas alice-in-canvas]
        bob-acts-too [:bob :canvas :canvas alice-in-canvas :see-and-act]
        carols-here [:carol :canvas :canvas alice-in-canvas :see-and-act]
        franks [:frank :canvas :canvas carols-here :see]
        give-see (decide! (canvas-act [{:e :bob :k :softland.store/permission :v bob-sees}]))
        the-row (fact-row :canvas :bob :softland.store/permission)
        stamp-before (last-stamp :canvas)
        acts (decide! (thing :canvas :bob bob-sees [{:e :bobs-note :k :text}]))
        names-less (decide! (thing :canvas :bob bob-without-the-slot [{:e :bobs-note :k :text}]))
        names-more (decide! (thing :canvas :bob bob-acts-too [{:e :bobs-note :k :text}]))
        gives (decide! (thing :canvas :bob bob-sees [{:e :erin :k :softland.store/permission :v [:erin :canvas :canvas bob-sees :see]}]))
        sends-a-value (decide! (thing :canvas :bob bob-sees [(value-fact :bobs-value :text "a value of a holder of see")]))
        two-faults (decide! (thing :canvas :bob bob-sees [{:e :kept :k :kw :replaces [(new-act-id :canvas) 0]}]))
        raise (decide! (canvas-act [{:e :bob :k :softland.store/permission :v bob-acts-too}]))
        row-after (fact-row :canvas :bob :softland.store/permission)
        still-sees (decide! (thing :canvas :bob bob-acts-too [{:e :bobs-note :k :text}]))
        give-carol (decide! (canvas-act [{:e :carol :k :softland.store/permission :v carols-here}]))
        carol-writes (decide! (thing :canvas :carol carols-here [{:e :carols-note :k :text}]))
        carol-gives (decide! (thing :canvas :carol carols-here [{:e :frank :k :softland.store/permission :v franks}]))
        frank-acts (decide! (thing :canvas :frank franks [{:e :franks-note :k :text}]))
        by-another-giver (decide! (thing :canvas :carol carols-here
                                         [{:e :bob :k :softland.store/permission :v [:bob :canvas :canvas carols-here :see-and-act]}]))
        slotted [:alice :slotted :slotted [:x :slotted :slotted] :see-and-act]
        slotted-making (decide! (thing :slotted :alice slotted
                                       [{:e :slotted :k :softland.store/depot :v :streaming}
                                        {:e :alice :k :softland.store/permission :v slotted}]))
        right-making (decide! (making :slotted :alice :alice))]
    (check "a permission is given with the slot that says what it allows, see: accepted, and its row holds the whole text, slot and all"
           [:accepted [:value bob-sees] :see] [(:word give-see) (record/read-value the-row) (act/allows (record/value-of the-row))])
    (check "the holder of see acts and names her permission as the row has it: refused, an act needs act; the refusal is recorded, and nothing is written"
           [:permission-does-not-allow-acting true nil] [(:word acts) (:mine? acts) (fact-row :canvas :bobs-note :text)])
    (check "she names it without the slot, and with see and act in the slot: the bytes are not the row's, so it does not stand; nobody raises her right by naming it"
           [:permission-does-not-stand :permission-does-not-stand] [(:word names-less) (:word names-more)])
    (check "she cannot give: a give is an act; and her act with a value is refused with the same word, under its own act-id with the digest of what was sent, and writes no row"
           [:permission-does-not-allow-acting nil :permission-does-not-allow-acting true nil]
           [(:word gives) (fact-row :canvas :erin :softland.store/permission)
            (:word sends-a-value) (:mine? sends-a-value) (fact-row :canvas :bobs-value :text)])
    (check "an act of hers with a second fault, a stale replaces, gets the word of her permission: the standing comes before the places"
           :permission-does-not-allow-acting (:word two-faults))
    (check "a second give to her, of see and act, is refused: a permission's place takes one write, so see stays see; her row is as it was, and she still cannot act"
           [:store-key-takes-one-write true :permission-does-not-stand]
           [(:word raise) (= (value-bytes the-row) (value-bytes row-after)) (:word still-sees)])
    (check "a permission that says see and act: its holder acts, and gives one beneath her own that says see; the holder of that one cannot act"
           [:accepted :accepted :accepted :permission-does-not-allow-acting]
           [(:word give-carol) (:word carol-writes) (:word carol-gives) (:word frank-acts)])
    (check "a give to the holder of see by another giver, of see and act: refused with the same word; a holder has one permission in a layer, as built"
           [:store-key-takes-one-write true]
           [(:word by-another-giver) (= (value-bytes the-row) (value-bytes (fact-row :canvas :bob :softland.store/permission)))])
    (check "a making act whose permission has the slot is no making act: refused as an act into a layer that is not made; the layer is then made by a right act"
           [:layer-not-made :accepted :streaming] [(:word slotted-making) (:word right-making) (depot-of-layer :slotted)])
    (check "each of her six refusals got a stamp of its own in the layer: the stamps go up in the order sent, the first later than the layer's last stamp before them"
           true (apply < stamp-before (map (comp :stamp :answer) [acts names-less names-more gives sends-a-value two-faults])))
    (shown "a permission that says see, as its row holds it:" (pr-str (record/value-of the-row))
           "| its holder acts:" (told acts) "| names it without the slot:" (:word names-less) "| with see and act:" (:word names-more)
           "| gives:" (:word gives) "| sends a value:" (told sends-a-value))
    (shown "see stays see: a second give of see and act" (:word raise)
           "| a holder of see and act acts and gives:" (:word carol-writes) (:word carol-gives) "| a making act whose permission has the slot:" (:word slotted-making))))

(deftest what-points-at-a-thing
  (let [u (UUID/randomUUID)
        place (fn [e] (fact-row :canvas e :on))
        writes! (fn [e v & {:as more}] (decide! (canvas-act [(merge (value-fact e :on v) more)])))
        marks! (fn [keys] (decide! (canvas-act [{:e :canvas :k marks :v keys}])))
        before-the-mark (writes! :b0 :canvas-1)
        nothing-marked (marked-keys :canvas)
        mark-1 (marks! #{:on})
        s1 (:stamp (:answer mark-1))
        marked-1 (marked-keys :canvas)
        run-0 (run-of :canvas :on s1 :canvas-1)
        a1 (writes! :b1 :canvas-1)
        run-1 (entries-of :canvas :on s1 :canvas-1)
        a2 (writes! :b2 u)
        run-2 (run-of :canvas :on s1 u)
        a3 (writes! :b1 u :replaces [(:act-id (:thing a1)) 0])
        after-3 [(run-of :canvas :on s1 :canvas-1) (entry :canvas :on s1 :canvas-1 :b1) (run-of :canvas :on s1 u)]
        a4 (writes! :b1 :canvas-2)
        after-4 [(run-of :canvas :on s1 u) (entry :canvas :on s1 u :b1) (run-of :canvas :on s1 :canvas-2)]
        a5 (writes! :b1 :canvas-2)
        after-5 (entries-of :canvas :on s1 :canvas-2)
        spaced (writes! :b3 "a string with spaces")
        run-spaced (run-of :canvas :on s1 "a string with spaces")
        stale (decide! (canvas-act [(assoc (value-fact :b3 :on :never-written) :replaces [(new-act-id :canvas) 0])]))
        after-stale [(run-of :canvas :on s1 "a string with spaces") (run-of :canvas :on s1 :never-written)]
        ;; an entry that a later act put back is not taken out when an earlier act is sent again
        x1 (writes! :b9 :v1)
        x2 (writes! :b9 :v2)
        x3 (writes! :b9 :v1)
        again (send! (:thing x2))
        after-again [(run-of :canvas :on s1 :v1) (run-of :canvas :on s1 :v2) (:by-act (entry :canvas :on s1 :v1 :b9))]
        a6 (decide! (canvas-act [{:e :b1 :k :on}]))
        after-6 [(run-of :canvas :on s1 :canvas-2) (record/read-value (place :b1))]
        onto-the-retract (writes! :b1 :canvas-7)
        after-onto [(run-of :canvas :on s1 :canvas-7) (run-of :canvas :on s1 :canvas-2)]
        mark-off (marks! #{})
        marked-off (marked-keys :canvas)
        a8 (writes! :b2 :canvas-3)
        after-8 [(some? (entry :canvas :on s1 u :b2)) (entry :canvas :on s1 :canvas-3 :b2) (record/read-value (place :b2))]
        mark-2 (marks! #{:on})
        s2 (:stamp (:answer mark-2))
        marked-2 (marked-keys :canvas)
        after-mark-2 [(run-of :canvas :on s2 :canvas-3) (run-of :canvas :on s2 u) (run-of :canvas :on s2 :v1)]
        a10 (writes! :b2 :canvas-4)
        after-10 [(run-of :canvas :on s2 :canvas-4) (run-of :canvas :on s2 :canvas-3)]
        a11 (writes! :b2 :canvas-5)
        after-11 [(run-of :canvas :on s2 :canvas-4) (entry :canvas :on s2 :canvas-4 :b2) (run-of :canvas :on s2 :canvas-5)
                  (some? (entry :canvas :on s1 u :b2))]
        both-base (canvas-act [])
        both (decide! (assoc both-base :facts [{:e :canvas :k marks :v #{:on :in}} (value-fact :b5 :in :canvas-1)]))
        marked-after-both (marked-keys :canvas)
        in-since (get (marked-keys :canvas) :in)
        after-both [(run-of :canvas :in in-since :canvas-1) (:stamp (fact-row :canvas :b5 :in))]
        later (decide! (canvas-act [(value-fact :b6 :in :canvas-1)]))
        after-later (run-of :canvas :in in-since :canvas-1)
        unmarked (decide! (canvas-act [(value-fact :b1 :text :canvas-1)]))
        final-run (entries-of :canvas :on s2 :canvas-5)]
    (swap! kept update :canvas-before merge {:since s2 :value :canvas-5 :run final-run :marked (marked-keys :canvas)})
    (check "each act of this walk is accepted"
           #{:accepted}
           (set (map :word [before-the-mark mark-1 a1 a2 a3 a4 a5 spaced x1 x2 x3 a6 onto-the-retract mark-off a8 mark-2 a10 a11 both later
                            unmarked])))
    (check "before the mark no key is marked; the mark puts the key in the row of the marked keys with the stamp of the act that marked it"
           [{} {:on s1}] [nothing-marked marked-1])
    (check "nothing is filled from before: the fact that was written before the mark is in no run" [] run-0)
    (check "a fact under the marked key, with a value that is a keyword id: its place is the run of that value, and the row holds the fact that wrote it, its stamp, the time on its act, who and the permission, and no value"
           [[(record/by-value-key :canvas :on s1 (record/value-digest-of :canvas-1) :b1)
             {:by-act (nth (:act-id (:thing a1)) 2) :by-index 0 :stamp (:stamp (:answer a1)) :time (time-on (:thing a1))
              :who ":alice" :permission "[:alice :canvas :canvas]"}]]
           run-1)
    (check "a fact whose value is a UUID: the run of that thing holds its place" [:b2] run-2)
    (check "the fact is replaced, naming what it replaces: its place is out of the run of the value before, the key of that entry holds no row, and it is in the run of the new value beside the other place"
           [[] nil [:b1 :b2]] after-3)
    (check "the fact is written over with no replaces: the same, out of one run and into another"
           [[:b2] nil [:b1]] after-4)
    (check "the same value written again: the entry is written over, and holds the later fact, with the time on the later act"
           [[(record/by-value-key :canvas :on s1 (record/value-digest-of :canvas-2) :b1)
             {:by-act (nth (:act-id (:thing a5)) 2) :by-index 0 :stamp (:stamp (:answer a5)) :time (time-on (:thing a5))
              :who ":alice" :permission "[:alice :canvas :canvas]"}]]
           after-5)
    (check "a value that is a string with spaces has a run of its own" [:b3] run-spaced)
    (check "a fact under the marked key that names a fact which is not current is refused whole: the run of the value that is there is as it was, and the refused value has no run"
           [:replaced-fact-not-current [:b3] []] (into [(:word stale)] after-stale))
    (check "an act that took an entry out is sent a second time, after a later act put the entry back: the gate answers it from the record and writes nothing, so the entry is still there and is the later act's"
           [:acked [:b9] [] (nth (:act-id (:thing x3)) 2)] (into [again] after-again))
    (check "a retract: the place is out of the run, and the place holds a fact with no value" [[] [:nothing-here]] after-6)
    (check "a fact onto the retract, naming nothing: its place is in the run of its value, and nothing went out" [[:b1] []] after-onto)
    (check "the mark is taken off by a second write of the set without the key: no key is marked" {} marked-off)
    (check "a fact that is replaced while the key is not marked writes nothing in that order: the row of the earlier value stays, though its fact is no longer current, and the new value has no row"
           [true nil [:value :canvas-3]] after-8)
    (check "the key is marked again: it gets a new stamp, later than the first" [{:on s2} true] [marked-2 (< s1 s2)])
    (check "under the new stamp each of the three runs that the hand reads is empty: the row that stayed is under the old stamp, in no run that a read can name, and the fact from before the second mark is not filled in"
           [[] [] []] after-mark-2)
    (check "a fact written after the second mark, over a fact from before it: its place is in the run of its value, since the new stamp"
           [[:b2] []] after-10)
    (check "and replaced again: out of that run, the key of the entry holds no row, in the run of the new value; the row of the first marking is still where it was"
           [[] nil [:b2] true] after-11)
    (check "a mark and a fact under its key in one act: the act is decided with the marks as they were before it, so the fact is in no run, and its stamp is the key's stamp, not a later one"
           [[] in-since] after-both)
    (check "the act that marks the keys again with one key more, and names nothing to replace: the key that was marked before keeps its stamp, and the new key gets the stamp of that act, as in the base"
           {:on s2 :in (:stamp (:answer both))} marked-after-both)
    (check "a later fact under that key is in the order" [:b6] after-later)
    (check "a fact under a key that the layer does not mark has no row in the order by value"
           [] (rama/foreign-select [(path/sorted-map-range ":canvas :text " ":canvas :text!") path/MAP-KEYS] (:by-value-ps *hand*) {:pkey :canvas}))
    (shown "what points at a thing, in a streaming layer. The key :on is marked since" s1 "| a fact with a keyword id, one with a UUID:"
           (pr-str (mapv first run-1)) "| the place" (pr-str run-2) "points at the UUID")
    (shown "the fact replaced, written over, written again, retracted; after each, the run of the value before, the key of the entry that went out, the run of the new value:"
           (pr-str after-3) (pr-str after-4) "| the same value again is the later fact's:" (= (nth (:act-id (:thing a5)) 2) (:by-act (second (first after-5))))
           "| after the retract:" (pr-str after-6))
    (shown "the same act appended a second time, after a later act put back the entry that it had taken out: the run is as the later act left it:" (pr-str after-again))
    (shown "the mark taken off, the fact replaced, the key marked again since" s2 ": every run under the new stamp is empty" (pr-str after-mark-2)
           "| the stale row stays under" s1 "| then a fact written and replaced:" (pr-str after-10) (pr-str after-11))
    (shown "a mark and a fact under it in one act: the fact is in no run" (pr-str after-both) "| a later fact is:" (pr-str after-later))))

;; ----------------------------------------------- the checks of the base's gate

(deftest the-base-is-made
  (let [before (depot-of-layer :base)
        on-the-other-depot (decide! (thing :base :system system-own [{:e :early :k :note}]
                                           {:act-id [:base :streaming (UUID/randomUUID)] :depot :streaming}))
        early (decide-in-base! (system-act [{:e :early :k :note}]))
        [by-stranger for-another-holder]
        (decide-in-a-row! [(assoc (making :base :system :system) :who :mallory)
                           (making :base :alice :alice)])
        depot-then (depot-of-layer :base)
        ;; The line: three acts of which each needs the row of the one before
        ;; it, appended with no wait for the gate between them.
        the-making (making :base :system :system)
        the-give (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}])
        alices-first (thing :base :alice alice-in-base [(value-fact :alices-first-doc :text "alice's first act in the base")])
        [made give first-under :as line] (decide-in-a-row! [the-making the-give alices-first])
        depot-read (depot-of-layer :base)
        made-again (decide-in-base! (update (making :base :system :system) :facts (comp vec reverse)))
        depot-rows (rows-by-task :mb-facts-ps (record/place-key :base :base :softland.store/depot))
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :system :softland.store/permission))
        alices-rows (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission))
        after (decide-in-base! (system-act [{:e :early :k :note}]))]
    (swap! kept assoc :base-give give)
    (check "before its making, the depot of the base is nothing" nil before)
    (check "an act for the base on the streaming depot, before the base is made, is refused as an act for a layer of the other gate, under its streaming act-id with the digest of what was sent: the streaming gate tells it from the layer id, and reads no table of the other gate for it"
           [:layer-belongs-to-the-other-gate true] [(:word on-the-other-depot) (:mine? on-the-other-depot)])
    (check "an act into the base before it is made is refused. It was appended alone, with nothing sent after it, and one read by its act-id gave its answer when the gate had processed it: no answer of the base waits for a later run of the gate"
           [:appended :layer-not-made true] [(:ack early) (:word early) (:mine? early)])
    (check "a making act of the base by a who that is not the system, and one that names a person as the base's holder, are each refused, and the base is not made"
           [:layer-not-made :layer-not-made nil] [(:word by-stranger) (:word for-another-holder) depot-then])
    (check "the line: the base's making act, the give of a permission to a person, and her first act under the given permission, appended one after the other with no wait for the gate between them: each is accepted, with the digest of what was sent"
           [[:appended :appended :appended] [:accepted :accepted :accepted] [true true true]]
           [(mapv :ack line) (mapv :word line) (mapv :mine? line)])
    (check "their stamps go up in the order in which they were appended"
           true (apply < (map (comp :stamp :answer) line)))
    (check "a second making act of the base, its two facts in the other order, is refused: the base is made, and the permission that the act gives is not beneath its own"
           :permission-fact-not-beneath-the-acts-own (:word made-again))
    (check "the base made by the system: one read of depot-of-layer gives microbatch" :microbatch depot-read)
    (check "the depot row and the system's permission row are on every task, each with the stamp of the making act's answer"
           [tasks tasks #{(:stamp (:answer made))}]
           [(count depot-rows) (count permission-rows) (set (map :stamp (concat (vals depot-rows) (vals permission-rows))))])
    (check "the rows hold the depot as a fact on the layer's own entity, and the system's permission on its holder"
           [:microbatch system-own]
           [(record/value-of (first (vals depot-rows))) (record/value-of (first (vals permission-rows)))])
    (check "the row of the permission that was given is on every task with the stamp of the give's answer, and the fact of the first act under it on every task with the stamp of that act's answer"
           [tasks #{(:stamp (:answer give))}
            (on-every-task (nth (:act-id alices-first) 2) 0 (:stamp (:answer first-under)) "\"alice's first act in the base\"")]
           [(count alices-rows) (set (map :stamp (vals alices-rows))) (facts-on-every-task :alices-first-doc :text)])
    (check "after the making, the same act of the system into the base is accepted" :accepted (:word after))
    (shown "an act into the base before it is made refused:" (told early) "| read by one call when the gate had processed it, with nothing sent after it"
           "| a making act of the base by a stranger:" (:word by-stranger) "| for a holder that is not the system:" (:word for-another-holder))
    (shown "the line: the base's making act, a give, and the first act under the given permission, appended with no wait between them:"
           (told made) "|" (told give) "|" (told first-under))
    (shown "the base made by the system, with the depot microbatch: depot-of-layer :base =" depot-read
           "| the depot row is on" (count depot-rows) "tasks, the system's permission on" (count permission-rows)
           "| a second making act, its facts in the other order:" (:word made-again))))

(deftest an-act-lands-in-the-base
  (let [entities (mapv #(keyword (str "doc-" %)) (range 1 9))
        seen (decide-in-one-batch! (mapv (fn [e] (system-act [{:e e :k :seen}])) entities))
        seen-rows (mapv (fn [e] (rows-by-task :mb-facts-ps (record/place-key :base e :seen))) entities)
        records (vec (stored-in-base))
        partitions-of-seen (into #{} (mapcat (fn [result] (partitions-of records (:thing result)))) seen)
        base-task (first partitions-of-seen)
        e (first entities)
        fact (value-fact e :title "the first text of the base")
        landed (decide-in-base! (system-act [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (base-fact e :title)
        by-task (rows-by-task :mb-facts-ps (record/place-key :base e :title))
        other-keys [e :alice act-uuid "a text that is no id" 42]
        with-other-keys (mapv (fn [pkey] (rama/foreign-select-one (path/keypath (record/place-key :base e :title))
                                                                  (:mb-facts-ps *hand*) {:pkey pkey}))
                              other-keys)
        answer-tasks (vec (keys (rows-by-task :mb-answers-ps (record/answer-key :base act-uuid))))
        last-stamps (last-stamps-by-task :base)
        logged (record-of-act (stored-in-base) (:act-id (:thing landed)))
        never-sent (answer-of (new-act-id :base))]
    (swap! kept assoc :base-landed landed :base-entities (vec (take 3 entities)) :base-task base-task)
    (check "eight acts of one batch, each with one fact, on eight entities, are each accepted"
           (vec (repeat 8 :accepted)) (mapv :word seen))
    (check "the eight things, whose first facts are on eight different entities, are on one partition of the microbatch depot: the depot places a thing by its layer"
           1 (count partitions-of-seen))
    (check "the fact of each of the eight is on every task, the same row in every field"
           [(vec (repeat 8 tasks)) (vec (repeat 8 1))]
           [(mapv count seen-rows) (mapv (comp count set vals) seen-rows)])
    (check "one act with a plain value lands through the microbatch gate and is read back by act-id: accepted, with the digest of what was sent"
           [:appended :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the value as a String, the one text of the value in its normal form, and gives the UTF-8 of that text back; the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and the time on the act"
           ["\"the first text of the base\"" (utf8-of "\"the first text of the base\"") act-uuid 0 ":system" "[:system :base :base]" (:stamp (:answer landed))
            (time-on (:thing landed))]
           [(:value the-row) (value-bytes the-row) (:by-act the-row) (:by-index the-row) (:who the-row) (:permission the-row) (:stamp the-row)
            (:time the-row)])
    (check "the row holds those seven fields and no other, and every task holds it, the same in every field"
           [#{:value :by-act :by-index :who :permission :stamp :time} tasks #{the-row}]
           [(set (keys the-row)) (count by-task) (set (vals by-task))])
    (check "a select of the place gives that row whatever the partitioning key is: the place's entity, another layer's id, a UUID, a text and a number"
           (vec (repeat (count other-keys) the-row)) with-other-keys)
    (check "the answer's row is on one task, the task of the base's depot partition; and that task alone keeps a last stamp of the base, which is the stamp of this answer"
           [[base-task] {base-task (:stamp (:answer landed))}]
           [answer-tasks last-stamps])
    (check "the microbatch depot's log holds the act's fact as the hand sent it, the value plain, with no part added"
           [{:e e :k :title :v "the first text of the base"}] (vec (:facts logged)))
    (check "the microbatch depot's log holds the time that the hand wrote on the act, a reading of its clock, and the answer holds that time beside its stamp"
           [true (time-on (:thing landed)) (time-on (:thing landed))]
           [(int? (time-on (:thing landed))) (time-on logged) (:time (:answer landed))])
    (check "an act-id of the base that was never sent has no answer: nothing yet" nil never-sent)
    (shown "one act with a plain value landing through the microbatch gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "eight things of the base with eight different first entities are on one partition of the microbatch depot, partition" base-task
           "| the fact of each is on" (pr-str (mapv count seen-rows)) "tasks")
    (shown "a value of the base kept as its one text in its row, the same row on" (count by-task) "tasks:" (pr-str (:value the-row))
           "| read back:" (pr-str (record/read-value the-row)) "| the fields of the row:" (pr-str (vec (sort (keys the-row))))
           "| its answer is on task" (pr-str answer-tasks) "| the base's last stamp by task:" (pr-str last-stamps)
           "| the microbatch depot's log holds the value plain:" (pr-str (vec (:facts logged))))))

(defn- seen-at-the-commit
  "Appends one act with a value, alone, and watches the answers table of the
  microbatch gate itself, round the reads, on the base's task, for the row
  of the act's answer. At the first sight of the row it reads the answer as
  a door does, with one call of the query. Gives {:thing :row :read
  :selects}: the thing; the row as the select gave it, or nil when none
  showed within the hand's time limit; what the one call gave; and how many
  selects were made."
  []
  (let [x (system-act [(value-fact :seen-at-the-commit :note "read at the first sight of its answer's row")])
        act-id (:act-id x)
        k (record/answer-key :base (nth act-id 2))
        _ (append-to-base! x)
        deadline (+ (System/currentTimeMillis) time-limit-ms)
        [row selects] (loop [n 1]
                        (let [row (base-row :mb-answers-ps k)]
                          (cond row [row n]
                                (< deadline (System/currentTimeMillis)) [nil n]
                                :else (do (Thread/sleep 1) (recur (inc n))))))
        read (answer-of act-id)]
    {:thing x :row row :read read :selects selects}))

(deftest an-answer-is-readable-when-its-batch-is-committed
  (let [{:keys [thing row read selects]} (seen-at-the-commit)
        _ (wait-until-processed!)
        after (base-result thing :appended)
        act-id (:act-id thing)
        uuid (nth act-id 2)
        ask (fn [routing-id] (rama/foreign-invoke-query (:answer *hand*) routing-id act-id))
        by-layer (ask :base)
        by-uuid (ask uuid)
        by-another-layer (ask :alice)]
    (check "the row of an act's answer shows in the answers table on the base's task; at that first sight one call of answer-by-act-id, with the act-id's layer as its routing id, gives the answer, with the stamp of that row: the read waits for nothing behind the commit that showed the row"
           [true :accepted (:stamp row)]
           [(some? row) (:verdict read) (:stamp read)])
    (check "the answer that the read gave then is the answer that it gives when the gate has processed the thing, with the digest of what was sent"
           [read true read] [(:answer after) (:mine? after) by-layer])
    (check "the same query, called with the uuid of the act-id as its routing id, gives nothing: the head of the query finds that the routing id is not the act-id's layer and makes no read. `answer-read-by-routing-id`, called by hand with the same two, gives nil; with the layer it gives the depot word and the key of the answer"
           [nil nil [:microbatch (str ":base " uuid)]]
           [by-uuid (record/answer-read-by-routing-id uuid act-id) (record/answer-read-by-routing-id :base act-id)])
    (check "and called with another layer as its routing id it gives nothing either" nil by-another-layer)
    (shown "an answer of the base is readable when its batch is committed on the base's task: the row showed at select" selects
           "with stamp" (:stamp row) "| one call of answer-by-act-id with the layer as routing id gave then:" (pr-str read)
           "| with the uuid as routing id:" (pr-str by-uuid) "(the head makes no read)")))

(deftest refusals-and-repeats-in-the-base
  (let [landed (:base-landed @kept)
        accepted-thing (:thing landed)
        e (:e (first (:facts accepted-thing)))
        [_ e2 e3] (:base-entities @kept)
        [refused-one stale] (decide-in-one-batch!
                             [(system-act [(assoc (value-fact :doc-r1 :text "a value that names a fact which is not there")
                                                  :replaces [(new-act-id :base) 0])])
                              (system-act [(assoc (value-fact e :title "stale") :replaces [(new-act-id :base) 0])])])
        answer-before (answer-of (:act-id accepted-thing))
        row-before (base-fact e :title)
        stamp-before (base-last-stamp)
        _ (append-to-base! accepted-thing)
        _ (append-to-base! (:thing refused-one))
        _ (wait-until-processed!)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (answer-of (:act-id (:thing refused-one)))
        row-after (base-fact e :title)
        stamp-after (base-last-stamp)
        at-another-time (at-time accepted-thing (+ 5 (time-on accepted-thing)))
        other-time (decide-in-base! at-another-time)
        after-other-time [(base-last-stamp) (base-fact e :title)]
        other-under-used (decide-in-base! (assoc (system-act [{:e :doc-r2 :k :text}]) :act-id (:act-id accepted-thing)))
        twice-a (system-act [{:e e2 :k :twice}])
        twice-b (assoc (system-act [{:e e3 :k :twice}]) :act-id (:act-id twice-a))
        [two-a two-b] (decide-in-one-batch! [twice-a twice-b])
        written-a (rows-by-task :mb-facts-ps (record/place-key :base e2 :twice))
        written-b (rows-by-task :mb-facts-ps (record/place-key :base e3 :twice))
        ;; The walk of one place that the streaming group makes act by act,
        ;; here appended in a row: each act is decided against the rows that
        ;; the acts before it in the line wrote.
        junk {:act-id (new-act-id :base) :layer :base :junk 1 declaration/time-part (System/currentTimeMillis)}
        good-under-junk (assoc (system-act [{:e :walk-doc-0 :k :text}]) :act-id (:act-id junk))
        w0 (system-act [(value-fact :walk-doc :text "the first text")])
        empty-place (system-act [(assoc (value-fact :walk-doc-2 :text "x") :replaces [(:act-id w0) 0])])
        replaced (system-act [(assoc (value-fact :walk-doc :text "the second text") :replaces [(:act-id w0) 0])])
        stale-now (system-act [(assoc (value-fact :walk-doc :text "late") :replaces [(:act-id w0) 0])])
        over (system-act [(value-fact :walk-doc :text "the third text")])
        retract (system-act [{:e :walk-doc :k :text}])
        over-retract (system-act [(assoc (value-fact :walk-doc :text "after the retract") :replaces [(:act-id retract) 0])])
        retract-on-empty (system-act [{:e :walk-doc-3 :k :text}])
        retract-again (system-act [{:e :walk-doc-3 :k :text}])
        onto-retract (system-act [(value-fact :walk-doc-3 :text "onto a retract, naming nothing")])
        value-to-retract (system-act [(value-fact :walk-doc-4 :text "a value that is retracted")])
        last-retract (system-act [{:e :walk-doc-4 :k :text :replaces [(:act-id value-to-retract) 0]}])
        on-the-layer (system-act [(value-fact :base :title "a fact of the layer's own entity, under a plain key")])
        twice-in-one (system-act [{:e :walk-doc-5 :k :text} {:e :walk-doc-5 :k :text}])
        own-base (system-act [])
        own-fact (assoc own-base :facts [{:e :walk-doc-5 :k :text}
                                         {:e :walk-doc-6 :k :text :replaces [(:act-id own-base) 0]}])
        in-the-row [junk good-under-junk w0 empty-place replaced stale-now replaced over retract over-retract retract-on-empty
                    retract-again onto-retract value-to-retract last-retract on-the-layer twice-in-one own-fact]
        walked (decide-in-a-row! in-the-row)
        retract-rows (rows-by-task :mb-facts-ps (record/place-key :base :walk-doc-4 :text))
        retract-row (first (vals retract-rows))]
    (check "an act that is refused for a place gets a recorded refusal through the microbatch gate: its reason, a stamp and the digest of what was sent"
           [:refused :replaced-fact-not-current true true]
           [(:verdict (:answer refused-one)) (:reason (:answer refused-one)) (int? (:stamp (:answer refused-one))) (:mine? refused-one)])
    (check "a refusal writes nothing at the place, on any task" {} (rows-by-task :mb-facts-ps (record/place-key :base :doc-r1 :text)))
    (check "a stale replaces is refused through the microbatch gate, and the place is as it was"
           [:replaced-fact-not-current (:stamp (:answer landed))] [(:word stale) (:stamp row-before)])
    (check "a repeat is answered from the record: the same answer, with the stamp of the first decision"
           answer-before answer-after)
    (check "not decided twice: the row of its place is the row of the first decision, with the text of the first value"
           [(:stamp row-before) (:by-act row-before) "\"the first text of the base\""]
           [(:stamp row-after) (:by-act row-after) (:value row-after)])
    (check "a repeat of a refused act reads the recorded refusal" (:answer refused-one) refused-again)
    (check "not decided twice: the base's last stamp is the same after the two repeats" stamp-before stamp-after)
    (check "two things under one act-id that differ only in the time on them, the second 5 ms later: they are not equal, and they have one digest; the second reads the one answer, that of the first, which is of its own digest too"
           [false true :appended answer-before true]
           [(= accepted-thing at-another-time) (= (digest/hex-of accepted-thing) (digest/hex-of at-another-time))
            (:ack other-time) (:answer other-time) (:mine? other-time)])
    (check "and it is a repeat: nothing is decided a second time, so the base's last stamp and the row of the act's fact are as they were; the answer that the second reads holds the time on the first, and not its own"
           [stamp-before row-before (time-on accepted-thing)] (conj after-other-time (:time (:answer other-time))))
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false] [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written, on any task" {} (rows-by-task :mb-facts-ps (record/place-key :base :doc-r2 :text)))
    (check "one act-id under two different things in one batch: one answer, and it is of the thing that the depot holds first"
           [true [true false]] [(= (:answer two-a) (:answer two-b)) [(:mine? two-a) (:mine? two-b)]])
    (check "the first thing wrote its fact, on every task, and nothing of the second is written on any"
           [tasks {}] [(count written-a) written-b])
    (check "eighteen things appended in a row with no wait for the gate, each decided against the rows that the things before it wrote: a thing that is no act uses its act-id, and a good act under it reads that refusal; a fact is written; a replaces on an empty place is stale; the current fact is named and replaced; the replaced fact named again is stale; the accepted replace sent again is still accepted; a fact that names nothing writes over; a retract; a fact that names the retract; a retract on an empty place, twice; a value onto a retract; a value and its retract; a fact on the layer's own entity; a place named twice; and a fact that replaces a fact of its own act"
           [:unknown-part :unknown-part :accepted :replaced-fact-not-current :accepted :replaced-fact-not-current :accepted :accepted
            :accepted :accepted :accepted :accepted :accepted :accepted :accepted :accepted :place-named-twice :replaces-own-fact]
           (mapv :word walked))
    (check "each of the eighteen read an answer of its own digest, but the good act under the junk's act-id"
           (assoc (vec (repeat 18 true)) 1 false) (mapv :mine? walked))
    (check "at the end of the row the walked place holds, on every task, the fact that named the retract; the place of the stale replaces holds nothing; and the place of the value onto a retract holds that value"
           [(on-every-task (nth (:act-id over-retract) 2) 0 (:stamp (:answer (nth walked 9))) "\"after the retract\"")
            {}
            (on-every-task (nth (:act-id onto-retract) 2) 0 (:stamp (:answer (nth walked 12))) "\"onto a retract, naming nothing\"")]
           [(facts-on-every-task :walk-doc :text) (facts-on-every-task :walk-doc-2 :text) (facts-on-every-task :walk-doc-3 :text)])
    (check "a retract is recorded as the current fact of its place, on every task: a row with no value, which holds who retracted and under which permission"
           [tasks 1 (nth (:act-id last-retract) 2) false [:nothing-here] :system system-own]
           [(count retract-rows) (count (set (vals retract-rows))) (:by-act retract-row) (contains? retract-row :value)
            (record/read-value retract-row) (record/who-of retract-row) (record/permission-of retract-row)])
    (check "the two facts of the place named twice and of the act that replaces its own fact are on no task"
           [{} {}] [(facts-on-every-task :walk-doc-5 :text) (facts-on-every-task :walk-doc-6 :text)])
    (shown "a refusal through the microbatch gate, recorded with its reason:" (pr-str (:answer refused-one))
           "| a repeat of that refused act reads:" (pr-str refused-again))
    (shown "a stale replaces refused through the microbatch gate:" (told stale))
    (shown "a repeat (same act-id sent twice to the microbatch depot) answered from the record, not decided twice: stamp before"
           (:stamp answer-before) "after" (:stamp answer-after) "| the stamp of its place before" (:stamp row-before) "after" (:stamp row-after)
           "| the base's last stamp before" stamp-before "after" stamp-after)
    (shown "the same act of the base under its act-id with another time on it, 5 ms later, is a repeat: one digest"
           (subs (digest/hex-of at-another-time) 0 16) "| it reads the first's answer, stamp" (:stamp (:answer other-time))
           "| the base's last stamp after it" (first after-other-time))
    (shown "a second, different act under a used act-id of the base told so: the digest of the answer"
           (subs (:digest (:answer other-under-used)) 0 16) "is not the digest of what was sent"
           (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "one act-id under two things in one batch: one answer, of the first thing in the depot's order; written by the first on"
           (count written-a) "tasks, by the second on" (count written-b))
    (shown "the walk of a place in the base, eighteen things appended in a row with no wait:" (pr-str (mapv :word walked)))))

(deftest one-line-one-order
  (let [[ea eb] (:base-entities @kept)
        first-value (decide-in-base! (system-act [(value-fact ea :order "the fact before")]))
        f0 [(:act-id (:thing first-value)) 0]
        [a b] (decide-in-one-batch!
               [(system-act [(assoc (value-fact ea :order "from the first act") :replaces f0)])
                (system-act [{:e eb :k :order-other} (assoc (value-fact ea :order "from the second act") :replaces f0)])])
        other-fact (facts-on-every-task eb :order-other)
        one (system-act [(value-fact ea :line "from the first of two")])
        two (system-act [(assoc (value-fact ea :line "from the second of two") :replaces [(:act-id one) 0])])
        [one-result two-result] (decide-in-a-row! [one two])
        [first-mark second-mark] (decide-in-one-batch! [(system-act [{:e ea :k :mark}])
                                                        (system-act [{:e eb :k :mark-other} {:e ea :k :mark}])])
        mark (base-fact ea :mark)
        [give-carol carol-acts dave-acts give-dave]
        (decide-in-one-batch! [(system-act [{:e :carol :k :softland.store/permission :v carol-in-base}])
                               (thing :base :carol carol-in-base [{:e :carol :k :note}])
                               (thing :base :dave dave-in-base [{:e :dave :k :note}])
                               (system-act [{:e :dave :k :softland.store/permission :v dave-in-base}])])
        dave-later (decide-in-base! (thing :base :dave dave-in-base [{:e :dave :k :note}]))]
    (check "two acts of one batch that replace the same fact: the one that the depot holds first is accepted, and the second is refused as stale"
           [:accepted :replaced-fact-not-current] [(:word a) (:word b)])
    (check "the place holds the first act's fact on every task, with its stamp and the text of its value"
           (on-every-task (nth (:act-id (:thing a)) 2) 0 (:stamp (:answer a)) "\"from the first act\"")
           (facts-on-every-task ea :order))
    (check "the second is refused whole, with a later stamp: its other fact is on no task"
           [true {}] [(< (:stamp (:answer a)) (:stamp (:answer b))) other-fact])
    (check "two acts that write one place, appended one after the other with no wait for the gate: both are accepted, and the second names the first one's fact as the one that it replaces, so it was decided against the row that the first wrote"
           [:accepted :accepted] [(:word one-result) (:word two-result)])
    (check "the second one's fact is the place's fact, on every task"
           (on-every-task (nth (:act-id two) 2) 0 (:stamp (:answer two-result)) "\"from the second of two\"")
           (facts-on-every-task ea :line))
    (check "two acts of one batch that write one place and name nothing are both accepted, and the place holds the fact of the second, with its stamp"
           [:accepted :accepted (nth (:act-id (:thing second-mark)) 2) 1 (:stamp (:answer second-mark))]
           [(:word first-mark) (:word second-mark) (:by-act mark) (:by-index mark) (:stamp mark)])
    (check "in one batch: a give and then an act under it are both accepted; an act and then the give, the act is refused"
           [:accepted :accepted :permission-does-not-stand :accepted]
           [(:word give-carol) (:word carol-acts) (:word dave-acts) (:word give-dave)])
    (check "the same act under the permission, in a later batch, is accepted" :accepted (:word dave-later))
    (shown "two acts of one batch that replace the same fact: the first in the depot's order wins:" (told a) "|" (told b))
    (shown "two acts that write one place, appended with no wait, the second naming the first's fact:" (told one-result) "|" (told two-result)
           "| the place is written by" (short-id (:act-id two)) "on" (count (facts-on-every-task ea :line)) "tasks")
    (shown "two acts of one batch that write one place and name nothing:" (told first-mark) "|" (told second-mark) "| the place holds stamp" (:stamp mark))
    (shown "a give and an act under it in one batch, in both orders:" (:word give-carol) (:word carol-acts) "|" (:word dave-acts) (:word give-dave))))

(deftest a-chain-in-one-batch
  (let [n 100
        acts (reduce (fn [acts i]
                       (conj acts (system-act [(cond-> (value-fact :chain-doc :text (str "link " i))
                                                 (pos? i) (assoc :replaces [(:act-id (peek acts)) 0]))])))
                     []
                     (range n))
        ids (mapv :act-id acts)
        answered-while-paused (atom nil)
        results (decide-in-one-batch! acts (fn [] (reset! answered-while-paused (count (keep answer-of ids)))))
        stamps (mapv #(:stamp (:answer %)) results)
        stamp-of (zipmap ids stamps)
        in-the-chain (set ids)
        order-in-the-depot (into [] (keep (fn [[_ r]] (when (and (map? r) (contains? in-the-chain (:act-id r))) (:act-id r))))
                                 (stored-in-base))
        by-uuid (mapv (fn [act-id] (rama/foreign-invoke-query (:answer *hand*) (nth act-id 2) act-id)) ids)]
    (check "while the gate was paused, with the 100 acts of the chain on the depot, none of them had an answer"
           0 @answered-while-paused)
    (check "a chain of 100 acts appended while the gate was paused, each naming the fact of the act before it as the one that it replaces: every one is accepted, with the digest of what was sent. An act that was decided before the act before it would have been refused as stale"
           [{:accepted n} true] [(frequencies (map :word results)) (every? :mine? results)])
    (check "the depot holds the 100 in the order in which they were appended, and their stamps increase strictly in that order"
           [true true] [(= ids order-in-the-depot) (boolean (and (seq order-in-the-depot) (apply < (map stamp-of order-in-the-depot))))])
    (check "on every task the place holds the fact of the last act of the chain"
           (on-every-task (nth (peek ids) 2) 0 (peek stamps) "\"link 99\"")
           (facts-on-every-task :chain-doc :text))
    (check "asked with the uuid of its act-id as the routing id, each of the 100 gives nothing, on whichever task its uuid hashes to: the nothing is the head's"
           [n #{nil}] [(count by-uuid) (set by-uuid)])
    (shown "a chain of" n "acts in one batch, each replacing the fact of the act before it:" (pr-str (frequencies (map :word results)))
           "| answered while the gate was paused:" @answered-while-paused
           "| stamps strictly increasing in the depot's order, from" (first stamps) "to" (peek stamps))))

(deftest a-person-acts-in-the-base
  (let [give (:base-give @kept)
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission))
        writes (decide-in-base! (thing :base :alice alice-in-base [(value-fact :alices-doc :text "alice's text in the base")]))
        [give-again end-it depot-again depot-other setting-elsewhere give-bob unknown-key tools-own]
        (decide-in-one-batch!
         [(system-act [{:e :alice :k :softland.store/permission :v alice-in-base}])
          (system-act [{:e :alice :k :softland.store/permission}])
          (system-act [{:e :base :k :softland.store/depot :v :microbatch}])
          (system-act [{:e :base :k :softland.store/depot :v :streaming}])
          (system-act [{:e :team :k :softland.store/depot :v :microbatch}])
          (thing :base :alice alice-in-base [{:e :bob :k :softland.store/permission :v bob-in-base}])
          (system-act [{:e :base :k :softland.store/owner :v :system}])
          (system-act [(value-fact :alice :permission "a tool's own permission")])])
        bob-acts (decide-in-base! (thing :base :bob bob-in-base [{:e :bobs-doc :k :note}]))
        permission-rows-after (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission))
        tools-own-rows (rows-by-task :mb-facts-ps (record/place-key :base :alice :permission))
        ;; A stamp far above any that the base gave, carried on the fact of
        ;; the give: the greatest there is.
        far Long/MAX_VALUE
        fact-id [(:act-id (:thing give)) 0]
        last-before-the-batch (base-last-stamp)
        [not-given not-holder beside far-ahead far-ahead-no-standing :as the-batch]
        (decide-in-one-batch!
         [(thing :base :erin [:erin :base :base system-own] [{:e :erins-doc :k :note}])
          (thing :base :mallory alice-in-base [{:e :mallorys-doc :k :note}])
          (thing :base :alice alice-in-base [{:e :frank :k :softland.store/permission :v [:frank :base :base system-own]}])
          (system-act [{:e :far-doc :k :note}] {:based-on {fact-id far}})
          (thing :base :mallory alice-in-base [{:e :mallorys-doc :k :note}] {:based-on {fact-id far}})])
        next-one (decide-in-base! (system-act [{:e :far-doc :k :note}]))]
    (check "the system gave a person a permission in the base, beneath its own: accepted, and it sits on its holder as a fact"
           [:accepted alice-in-base] [(:word give) (record/value-of (first (vals permission-rows)))])
    (check "the permission's row is on every task" tasks (count permission-rows))
    (check "a person acts in the base under the permission that the system gave, with a value: accepted, and the row holds the text of her value"
           [:accepted true "\"alice's text in the base\""] [(:word writes) (:mine? writes) (:value (base-fact :alices-doc :text))])
    (check "an act under a permission that does not stand is refused" :permission-does-not-stand (:word not-given))
    (check "an act under a permission by a who that is not its holder is refused" :who-not-the-permissions-holder (:word not-holder))
    (check "a permission given beside the giver's own, not beneath it, is refused, and gives nothing"
           [:permission-fact-not-beneath-the-acts-own {}]
           [(:word beside) (rows-by-task :mb-facts-ps (record/place-key :base :frank :softland.store/permission))])
    (check "in the base a permission's place and the layer's depot take one write: a permission given again, the end of one, the depot again, another depot"
           [:store-key-takes-one-write :ending-a-permission-not-decided :store-key-takes-one-write :changing-a-depot-not-decided]
           [(:word give-again) (:word end-it) (:word depot-again) (:word depot-other)])
    (check "a setting key on an entity that is not the layer is refused in the base" :setting-not-on-the-layer (:word setting-elsewhere))
    (check "after those refusals the permission's rows and the depot are as they were"
           [(into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows) :microbatch]
           [(into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows-after) (depot-of-layer :base)])
    (check "a person gives a permission beneath her own, in the same batch as those refusals, and an act under it is accepted"
           [:accepted :accepted] [(:word give-bob) (:word bob-acts)])
    (check "a fact under a key in the store's namespace that the store does not know is refused by the microbatch gate too, and nothing is written"
           [:store-key-not-known {}]
           [(:word unknown-key) (rows-by-task :mb-facts-ps (record/place-key :base :base :softland.store/owner))])
    (check "a tool's own fact under the bare word :permission, on a holder's entity, is accepted in the base: its row is on every task, as any fact's, with the tool's value, beside the store's permission of that holder"
           [:accepted tasks #{[:value "a tool's own permission"]} tasks]
           [(:word tools-own) (count tools-own-rows) (set (map record/read-value (vals tools-own-rows))) (count permission-rows-after)])
    (check "an act that carries in `:based-on` a stamp far above any that the base gave, the greatest there is, 2^63 less one, is accepted: a carried stamp is what its maker saw, and no gate reads it"
           :accepted (:word far-ahead))
    (check "the same under a permission whose holder is not its who is refused for its standing, as any such act: the carried stamp has no word of its own"
           :who-not-the-permissions-holder (:word far-ahead-no-standing))
    (check "the five acts of that batch get the base's last stamp plus 1 to 5, in the order appended, the act that carried the far stamp at its place among them; and the next act of the base gets plus 6"
           [(mapv #(+ last-before-the-batch %) (range 1 6)) (+ last-before-the-batch 6)]
           [(mapv (comp :stamp :answer) the-batch) (:stamp (:answer next-one))])
    (shown "a person acting in the base under a permission that the system gave:" (told give) "| the permission's row is on"
           (count permission-rows) "tasks | her act, with a value:" (told writes))
    (shown "in the base, an act under a permission that does not stand:" (:word not-given) "| by a who that is not the holder:" (:word not-holder)
           "| a permission given beside the giver's own:" (:word beside))
    (shown "a permission's place takes one write in the base:" (:word give-again) "| ending a permission:" (:word end-it)
           "| the depot again:" (:word depot-again) (:word depot-other) "| a setting key on another entity:" (:word setting-elsewhere)
           "| a permission beneath a person's, and an act under it:" (:word give-bob) (:word bob-acts))
    (shown "in the base, a key in softland.store that the store does not know:" (:word unknown-key)
           "| a tool's own fact under the bare word :permission:" (:word tools-own) "| its row is on" (count tools-own-rows)
           "tasks, the store's permission of the same holder on" (count permission-rows-after))
    (shown "the greatest carried stamp, in the base:" (told far-ahead) "| the same with no standing:" (told far-ahead-no-standing)
           "| the next act of the base:" (told next-one))))

(deftest a-permission-deeper-than-four-in-the-base
  (let [chain (chain-of-permissions :base system-own "deep" 33)
        at (fn [depth] (nth chain (dec depth)))
        holder (fn [depth] (first (at depth)))
        rows-on-its-holder (fn [depth] (rows-by-task :mb-facts-ps (record/place-key :base (holder depth) :softland.store/permission)))
        gives (decide-in-a-row! (mapv (fn [depth] (thing :base (holder (dec depth)) (at (dec depth))
                                                         [{:e (holder depth) :k :softland.store/permission :v (at depth)}]))
                                      (range 2 34)))
        give-33 (peek gives)
        acts-too [:deep-seer-32 :base :base (at 31) :see-and-act]
        sees [:deep-looker-32 :base :base (at 31) :see]
        [under-5 under-32 own-33 give-acts-too give-sees slot-at-33]
        (decide-in-one-batch!
         [(thing :base (holder 5) (at 5) [(value-fact :deep-doc-5 :text "under depth 5")])
          (thing :base (holder 32) (at 32) [(value-fact :deep-doc-32 :text "under depth 32")])
          (thing :base (holder 33) (at 33) [{:e :deep-doc-33 :k :text}])
          (thing :base (holder 31) (at 31) [{:e :deep-seer-32 :k :softland.store/permission :v acts-too}])
          (thing :base (holder 31) (at 31) [{:e :deep-looker-32 :k :softland.store/permission :v sees}])
          (thing :base (holder 32) (at 32)
                 [{:e :deep-seer-33 :k :softland.store/permission :v [:deep-seer-33 :base :base (at 32) :see-and-act]}])])
        row-5 (base-fact :deep-doc-5 :text)
        row-32 (base-fact :deep-doc-32 :text)
        [seer-acts looker-acts]
        (decide-in-one-batch! [(thing :base :deep-seer-32 acts-too [(value-fact :deep-seers-doc :text "under depth 32, with the slot")])
                               (thing :base :deep-looker-32 sees [{:e :deep-lookers-doc :k :text}])])
        slot-rows (fn [e] (set (map (comp act/allows record/value-of)
                                    (vals (rows-by-task :mb-facts-ps (record/place-key :base e :softland.store/permission))))))]
    (check "in the base each holder gives a permission beneath her own, one at each depth from 2 to 32, appended one after the other with no wait for the gate: the 31 gives are each accepted, so each was decided against the row that the give before it wrote"
           (vec (repeat 31 :accepted)) (mapv :word (pop gives)))
    (check "the permissions of depth 4, 5, 31 and 32 each sit on their holder as they were given, on every task"
           (mapv (fn [depth] [tasks #{[:value (at depth)]}]) [4 5 31 32])
           (mapv (fn [depth] (let [rows (rows-on-its-holder depth)] [(count rows) (set (map record/read-value (vals rows)))])) [4 5 31 32]))
    (check "the give at depth 33, appended in the same row, is refused as malformed: the refusal is recorded under its own act-id with the digest of what was sent, and no task holds a row on that holder"
           [:malformed true {}] [(:word give-33) (:mine? give-33) (rows-on-its-holder 33)])
    (check "an act under the permission of depth 5, and one under the permission of depth 32, each with a value, are accepted"
           [:accepted :accepted "\"under depth 5\"" "\"under depth 32\""]
           [(:word under-5) (:word under-32) (:value row-5) (:value row-32)])
    (check "the row of each holds the whole permission that the act named: it reads back as that permission, its text opens 5 and 32 vectors, and no part of it is cut for its depth"
           [(at 5) (at 32) 5 32 false]
           [(record/permission-of row-5) (record/permission-of row-32)
            (count (filter #{\[} (:permission row-5))) (count (filter #{\[} (:permission row-32)))
            (str/includes? (:permission row-32) "#too-deep")])
    (check "an act that names as its own a permission of depth 33, deeper than any that was given, is refused as malformed, and writes nothing on any task"
           [:malformed true {}]
           [(:word own-33) (:mine? own-33) (rows-by-task :mb-facts-ps (record/place-key :base :deep-doc-33 :text))])
    (check "a permission of depth 32 with the fifth slot is given, with each of the slot's two words: every row holds the slot; the holder of see and act acts with a value, and the holder of see cannot act"
           [:accepted :accepted #{:see-and-act} #{:see} :accepted "\"under depth 32, with the slot\"" :permission-does-not-allow-acting]
           [(:word give-acts-too) (:word give-sees) (slot-rows :deep-seer-32) (slot-rows :deep-looker-32)
            (:word seer-acts) (:value (base-fact :deep-seers-doc :text)) (:word looker-acts)])
    (check "a give of depth 33 with the fifth slot is refused as malformed" :malformed (:word slot-at-33))
    (shown "a permission deeper than four, through the microbatch gate: the gives at depth 2 to 32, appended in a row with no wait:"
           (pr-str (frequencies (map :word (pop gives))))
           "| the give at depth 33:" (told give-33) "| an act under depth 5:" (told under-5) "| under depth 32:" (told under-32))
    (shown "in the base, the row of the act under depth 32 holds its permission whole:" (count (:permission row-32)) "characters of text that open"
           (count (filter #{\[} (:permission row-32))) "vectors | an own permission of depth 33:" (told own-33)
           "| the fifth slot at depth 32:" (:word give-acts-too) (:word give-sees) "| its holders act:" (:word seer-acts) (:word looker-acts)
           "| the fifth slot at depth 33:" (:word slot-at-33))))

(deftest the-bounds-in-the-base
  (let [[value-over value-at] (decide-in-one-batch! [(system-act [(value-fact :bound-doc-over :text over-the-bound-of-a-value)])
                                                     (system-act [(value-fact :bound-doc-at :text at-the-bound-of-a-value)])])
        at-rows (rows-by-task :mb-facts-ps (record/place-key :base :bound-doc-at :text))
        at-row (base-fact :bound-doc-at :text)
        act-over (decide-in-base! (act-over-its-bound :base))
        act-over-bytes (* 256 (+ 65536 (count (utf8-of (pr-str (:who (:thing act-over)))))
                                 (count (utf8-of (pr-str (:permission (:thing act-over)))))))
        records (vec (stored-in-base))
        value-over-logged (record-of-act records (:act-id (:thing value-over)))
        act-over-logged (record-of-act records (:act-id (:thing act-over)))
        many (fn [n] (system-act (map (fn [i] {:e (keyword (str "many-" i)) :k :text}) (range n))))
        based-on (fn [n] (into {} (map (fn [i] [[[:base :microbatch (UUID. 0 1)] i] 1])) (range n)))
        long-id (keyword (apply str (repeat 300 "a")))
        [facts-256 facts-257 based-1024 based-1025 long-entity]
        (decide-in-one-batch! [(many 256)
                               (many 257)
                               (system-act [{:e :based-doc :k :text}] {:based-on (based-on 1024)})
                               (system-act [{:e :based-doc :k :text}] {:based-on (based-on 1025)})
                               (system-act [{:e long-id :k :text}])])
        last-of-many (rows-by-task :mb-facts-ps (record/place-key :base :many-255 :text))]
    (check "through the microbatch gate, in one batch: an act of 256 facts is accepted, and its last fact is a row on every task, with its index; one of 257 is refused; 1,024 entries of based-on are taken, and 1,025 are refused; and an id that prints to more than 256 characters is refused, the refusal recorded under the act-id"
           [:accepted tasks #{255} :too-many-facts :accepted :too-many-based-on :id-too-long true]
           [(:word facts-256) (count last-of-many) (set (map :by-index (vals last-of-many))) (:word facts-257)
            (:word based-1024) (:word based-1025) (:word long-entity) (:mine? long-entity)])
    (check "through the microbatch gate, a value whose one text is 65,537 bytes is refused with the word of the bound on one value: the refusal is recorded under its own act-id with the digest of what was sent, no task holds a row of it, and the depot's log holds the act whole, the value plain"
           [:appended :value-too-large true {} true]
           [(:ack value-over) (:word value-over) (:mine? value-over)
            (rows-by-task :mb-facts-ps (record/place-key :base :bound-doc-over :text)) (= (:thing value-over) value-over-logged)])
    (check "a value whose one text is 65,536 bytes is accepted in the same batch, and its row holds those bytes: the string in its quotes, which reads back as the value; every task holds that row"
           [:accepted 65536 21848 true true tasks 1]
           [(:word value-at) (count (value-bytes at-row)) (count (:value at-row))
            (= (str "\"" at-the-bound-of-a-value "\"") (:value at-row))
            (= [:value at-the-bound-of-a-value] (record/read-value at-row))
            (count at-rows) (count (set (vals at-rows)))])
    (check "an act of 256 values at that bound, under a who and a permission of more than 8,192 bytes of text, would write more than 18,874,368 bytes into the rows of its facts, counted by the proof with Clojure's printer: it is refused with the word of the bound on an act, the refusal recorded under its own act-id; the depot's log holds it whole, its 256 values plain; and no task holds a row of it"
           [true :appended :act-too-large true true 256 {} {}]
           [(< 18874368 act-over-bytes) (:ack act-over) (:word act-over) (:mine? act-over) (= (:thing act-over) act-over-logged)
            (count (:facts act-over-logged))
            (rows-by-task :mb-facts-ps (record/place-key :base :over-0 :text))
            (rows-by-task :mb-facts-ps (record/place-key :base :over-255 :text))])
    (shown "the bound on one value, through the microbatch gate: a text of 65,537 bytes" (told value-over) "| of 65,536 bytes" (told value-at)
           "| its row holds" (count (value-bytes at-row)) "bytes in" (count (:value at-row)) "characters, on" (count at-rows) "tasks")
    (shown "the bound on an act, through the microbatch gate: an act whose rows would hold" act-over-bytes "bytes of text, over 18874368:"
           (told act-over) "| in the log whole, with" (count (:facts act-over-logged)) "values plain")
    (shown "in the base a thing over each limit is refused:" (:word facts-257) (:word based-1025) (:word long-entity)
           "| at each limit accepted:" (:word facts-256) (:word based-1024) "| the 256th fact is on" (count last-of-many) "tasks")))

(deftest a-long-batch
  (let [n 400
        entity (fn [i] (keyword (str "long-" (mod i 20))))
        k (fn [i] (keyword (str "k" i)))
        results (decide-in-one-batch! (mapv (fn [i] (system-act [{:e (entity i) :k (k i)}])) (range n)))
        stamps (mapv #(:stamp (:answer %)) results)
        sample (range 0 n 37)]
    (check "a batch of 400 acts on 20 entities: each is accepted, each with a stamp of its own"
           [#{:accepted} n] [(set (map :word results)) (count (set stamps))])
    (check "the base is one line: the stamps of the 400 go up in the order in which they were appended, whatever the entity of each"
           true (apply < stamps))
    (check "the fact of each act of a sample is on its place on every task, with the stamp of its answer"
           (mapv (fn [i] [tasks #{(nth stamps i)}]) sample)
           (mapv (fn [i] (let [rows (rows-by-task :mb-facts-ps (record/place-key :base (entity i) (k i)))]
                           [(count rows) (set (map :stamp (vals rows)))]))
                 sample))
    (shown "a batch of" n "acts on 20 entities: each accepted, with" (count (set stamps))
           "stamps, which go up in the order sent over the whole batch")))

(deftest the-copies-of-the-base
  (let [records (vec (stored-in-base))
        copies (check-the-copies "the copies, after the line, the chain and the long batch" records)]
    (check "by then each task holds more than 400 rows of the base's facts, and the base's task more than 500 answers"
           [true true]
           [(< 400 (first (:facts copies))) (< 500 (nth (:answers copies) (:base-task @kept)))])))

(deftest the-time-on-the-act-in-the-base
  (let [t chosen-time
        hour 3600000
        act-at (fn [e millis] (at-time (system-act [(value-fact e :text (str "the text of " (name e)))]) millis))
        last-before (base-last-stamp)
        at (fn [i] (+ last-before i))
        same (mapv #(act-at (keyword (str "same-" %)) t) (range 3))
        ;; Acts whose times go forward, back, and to the earliest there is.
        moved-times [[:later (inc t)] [:earlier (- t 5000)] [:at-zero 0]]
        moved (mapv (fn [[e millis]] (act-at e millis)) moved-times)
        no-times [(dissoc (act-at :no-time t) declaration/time-part)
                  (act-at :nil-time nil)
                  (act-at :time-below-zero -1)
                  (act-at :time-no-integer 1.5)]
        unknown-part (at-time (assoc (system-act [(value-fact :unknown-part :text "x")]) :a-part-that-no-form-names 1) (+ t hour))
        other-layers-permission (at-time (thing :base :system [:system :alice :alice] [(value-fact :another-fault :text "x")]) (+ t hour))
        ;; What an act carries is no measure of its stamp: a claim far
        ;; ahead, and the greatest stamp there is in `:based-on`.
        fact-id [[:base :microbatch (UUID. 0 1)] 0]
        claim (system-act [{:e :ahead-1 :k :text}] {declaration/time-part t :claimed-when Long/MAX_VALUE})
        carries-the-greatest (system-act [{:e :ahead-2 :k :text}]
                                         {declaration/time-part t :based-on {fact-id Long/MAX_VALUE} :because-of (new-act-id :base)})
        ;; Acts with times far ahead, and behind them an act with the chosen
        ;; time that writes a fact.
        far-times [[:an-hour-ahead (+ t hour)] [:in-3084 year-3084] [:at-the-greatest Long/MAX_VALUE]]
        far (mapv (fn [[e millis]] (act-at e millis)) far-times)
        written-after (act-at :written-after t)
        xs (vec (concat same moved no-times [unknown-part other-layers-permission claim carries-the-greatest] far [written-after]))
        results (decide-in-one-batch! xs)
        words (mapv :word results)
        stamps (mapv (comp :stamp :answer) results)
        times (mapv (comp :time :answer) results)
        last-after (base-last-stamp)
        ;; In a later batch, an act with the chosen time that carries the
        ;; stamp of the fact written behind the far times, and replaces it.
        fact-after [(:act-id written-after) 0]
        names-it (decide-in-base! (at-time (system-act [(assoc (value-fact :written-after :text "the text that replaces it") :replaces fact-after)]
                                                       {:based-on {fact-after (peek stamps)}})
                                           t))
        ;; Two layers of one task. A layer that starts streaming and whose
        ;; id hashes to the base's task is found without a send: a select
        ;; with it as the partitioning key finds the base's last stamp. Its
        ;; acts on the microbatch depot are refused, each with a stamp.
        beside (first (filter (fn [layer] (some? (rama/foreign-select-one (path/keypath :last-stamp ":base")
                                                                          (:mb-task-ps *hand*) {:pkey layer})))
                              (map #(keyword (str "beside-the-base-" %)) (range 64))))
        beside-act (fn [] (at-time (assoc (thing beside beside [beside beside beside] [{:e :beside-doc :k :note}])
                                          :act-id [beside :microbatch (UUID/randomUUID)] :depot :microbatch)
                                   chosen-time))
        [beside-1 base-between beside-2] (decide-in-a-row! [(beside-act)
                                                            (system-act [{:e :between-doc :k :note}] {declaration/time-part t})
                                                            (beside-act)])
        lasts-of-the-task [(base-last-stamp beside) (base-last-stamp) (last-stamps-by-task beside)]
        as-told (fn [from to] (pr-str (mapv vector (subvec words from to) (subvec stamps from to) (subvec times from to))))]
    (check "eighteen things of the base, with times that the hand chose, are appended while the gate is paused, so one batch takes them; each is answered with the digest of what was sent"
           (vec (repeat 18 true)) (mapv :mine? results))
    (check "their stamps are the base's last stamp before the batch plus 1 to 18, in the order appended, accepts and refusals alike, with no gap; the last of them is then the base's last stamp"
           [(mapv at (range 1 19)) (at 18)] [stamps last-after])
    (check "a. three acts with one time on them are each accepted, each with the base's last stamp plus one; the row of the third's fact holds its stamp and the time on its act, on every task"
           [[:accepted :accepted :accepted] [(at 1) (at 2) (at 3)]
            (on-every-task (nth (:act-id (nth same 2)) 2) 0 (at 3) "\"the text of same-2\"") (the-time-on-every-task t)]
           [(subvec words 0 3) (subvec stamps 0 3) (facts-on-every-task :same-2 :text) (times-on-every-task :same-2 :text)])
    (check "c. the time on an act moves no stamp. An act with a time one millisecond after the chosen one, an act with a time 5 seconds before it, and an act with the time 0, the earliest there is, are each accepted with the base's last stamp plus one. The answer of each holds the time on its own act, and so does the row of its fact on every task"
           [[:accepted :accepted :accepted] [(at 4) (at 5) (at 6)] (mapv second moved-times)
            (mapv (comp the-time-on-every-task second) moved-times)]
           [(subvec words 3 6) (subvec stamps 3 6) (subvec times 3 6) (mapv (fn [[e _]] (times-on-every-task e :text)) moved-times)])
    (check "d. a thing with a usable act-id and no time, one with a nil time, one with a time below zero and one with a time that is no integer: each is refused with the word of the time, with the base's last stamp plus one; none writes a row on any task; and the row of each answer holds no time, so the read gives none"
           [(vec (repeat 4 :no-door-time)) (mapv at (range 7 11)) (vec (repeat 4 {})) (vec (repeat 4 false)) (vec (repeat 4 nil))]
           [(subvec words 6 10) (subvec stamps 6 10)
            (mapv (fn [x] (facts-on-every-task (:e (first (:facts x))) :text)) no-times)
            (mapv #(some-> (answer-row %) (contains? :time)) no-times) (subvec times 6 10)])
    (check "d. a refused thing keeps the time that it carried, as a label, and its stamp holds nothing of it: a thing with a usable time one hour ahead that the declaration refuses for another fault, one with a part that no form names and one whose permission is for another layer, gets the base's last stamp plus one, and its answer holds the time one hour ahead"
           [[:unknown-part (at 11) (+ t hour)] [:permission-not-in-layer (at 12) (+ t hour)]]
           [[(nth words 10) (nth stamps 10) (nth times 10)] [(nth words 11) (nth stamps 11) (nth times 11)]])
    (check "claimed-when far ahead is a claim: the act is accepted, and its stamp does not follow it: it is the base's last stamp plus one"
           [:accepted (at 13)] [(nth words 12) (nth stamps 12)])
    (check "an act that carries the greatest stamp there is, 2^63 less one, in `:based-on`, based on a fact and because of an act that the record does not hold, is accepted with the base's last stamp plus one: a gate raises no act's stamp above the stamps that it carries"
           [:accepted (at 14)] [(nth words 13) (nth stamps 13)])
    (check "e. no time is too far ahead: an act with a time one hour after the chosen one, one with a time in the year 3084, 2^45 milliseconds, and one with the greatest fixed-width integer, 2^63 less one, are each accepted with the base's last stamp plus one; the answer of each holds its own time, and so does the row of its fact on every task"
           [[:accepted :accepted :accepted] [(at 15) (at 16) (at 17)] (mapv second far-times)
            (mapv (comp the-time-on-every-task second) far-times)]
           [(subvec words 14 17) (subvec stamps 14 17) (subvec times 14 17) (mapv (fn [[e _]] (times-on-every-task e :text)) far-times)])
    (check "e. a time far ahead on an accepted act holds back no later act. Behind those three in the batch, an act with the chosen time on it writes a fact, with the base's last stamp plus one; in a later batch an act with the chosen time on it that carries the stamp of that fact in `:based-on`, and names the fact as the one that it replaces, is accepted with the next stamp, and its fact is then the place's on every task"
           [[:accepted (at 18)] [:accepted (at 19)]
            (on-every-task (nth (:act-id (:thing names-it)) 2) 0 (at 19) "\"the text that replaces it\"")]
           [[(nth words 17) (nth stamps 17)] [(:word names-it) (:stamp (:answer names-it))] (facts-on-every-task :written-after :text)])
    (check "b. two layers of one task count apart in the microbatch gate too. A layer that starts streaming and whose id hashes to the base's task gets, for its first act on the microbatch depot, with a time of 2023 on it, a refusal whose stamp is 0: its own count, far below the base's last stamp on that task; and its answer keeps the time 2023. An act of the base, appended between, gets the base's last stamp plus one. The layer's second act, refused too, gets 1"
           [true [:layer-belongs-to-the-other-gate :accepted :layer-belongs-to-the-other-gate] [0 (at 20) 1] [chosen-time chosen-time]]
           [(some? beside) (mapv :word [beside-1 base-between beside-2]) (mapv (comp :stamp :answer) [beside-1 base-between beside-2])
            (mapv (comp :time :answer) [beside-1 beside-2])])
    (check "b. the base's task keeps a last stamp for each of the two layers, each its own, and no other task keeps one for that layer"
           [1 (at 20) {(:base-task @kept) 1}]
           lasts-of-the-task)
    (shown "two layers of one task count apart in the microbatch gate: the layer" beside "on the base's task," (told beside-1) "|" (told beside-2)
           "| an act of the base between them:" (told base-between) "| the last stamps that the task keeps, of that layer and of the base:"
           (pr-str (subvec lasts-of-the-task 0 2)))
    (shown "the time on the act is a label beside the stamp, in the microbatch gate, for eighteen things of one batch. The base's last stamp before it" last-before
           "| three acts with the chosen time" t ":" (pr-str (subvec stamps 0 3))
           "| a later time, a time 5 s earlier, and the time 0, as [word stamp time]:" (as-told 3 6))
    (shown "in the base a thing with no usable time is refused, with the last stamp plus one and no time in its answer: no time, nil, below zero, no integer:"
           (as-told 6 10) "| refused for another fault, with a time one hour ahead, which its answer keeps:" (as-told 10 12))
    (shown "in the base, what an act carries is no measure of its stamp: claimed-when far ahead:" (told (nth results 12))
           "| carried the greatest stamp:" (told (nth results 13))
           "| times far ahead, accepted: an hour, the year 3084, 2^63 less one:" (as-told 14 17)
           "| behind them, an act at the chosen time:" (told (nth results 17)) "| one that carries its stamp and replaces its fact:" (told names-it))))

(deftest an-act-of-several-facts-in-the-base
  (let [entities (:base-entities @kept)
        places (mapv (fn [e] [e :body]) entities)
        sent-facts (mapv (fn [[e k]] (value-fact e k (str "the body of " (name e)))) places)
        place-rows (fn [] (mapv (fn [[e k]] (facts-on-every-task e k)) places))
        refused (decide-in-base! (system-act (update sent-facts 2 assoc :replaces [(new-act-id :base) 0])))
        places-after-refusal (place-rows)
        last-stamp-before (base-last-stamp)
        accepted (decide-in-base! (system-act sent-facts))
        act-uuid (nth (:act-id (:thing accepted)) 2)
        stamp (:stamp (:answer accepted))
        places-after (place-rows)
        last-stamp-after (base-last-stamp)
        [first-entity other-entity] entities
        planted (system-act [{:e other-entity :k :crossed}])
        crossing (system-act [{:e first-entity :k :crossing}
                              {:e other-entity :k :crossed :replaces [(:act-id planted) 0]}])
        crossing-again (system-act [{:e first-entity :k :crossing-again}
                                    {:e other-entity :k :crossed :replaces [(:act-id planted) 0]}])
        [planted-result crossing-result crossing-again-result] (decide-in-a-row! [planted crossing crossing-again])]
    (swap! kept assoc :base-values {:places places :act-uuid act-uuid})
    (check "an act of three values on three entities, with its third fact stale, is refused whole" :replaced-fact-not-current (:word refused))
    (check "no row of it is on any task: none of its three places holds a fact" [{} {} {}] places-after-refusal)
    (check "the same act with each fact good is accepted whole" [:accepted true] [(:word accepted) (:mine? accepted)])
    (check "each fact's row is on every task, written by the act, with the index of the fact, the one stamp of the answer and the text of its value"
           (mapv (fn [[e _] i] (on-every-task act-uuid i stamp (str "\"the body of " (name e) "\""))) places (range))
           places-after)
    (check "the refusal took a stamp of the line: it was the base's last stamp before the act; the act's stamp is later, and is the base's last stamp after it"
           [(:stamp (:answer refused)) true stamp]
           [last-stamp-before (< last-stamp-before stamp) last-stamp-after])
    (check "three acts appended in a row with no wait: a retract; an act of two facts whose second names that retract as the fact that it replaces; and the same replaces again under a new act-id. The first two are accepted and the third is stale"
           [:accepted :accepted :replaced-fact-not-current]
           [(:word planted-result) (:word crossing-result) (:word crossing-again-result)])
    (check "the replaced place holds, on every task, the second fact of the act that replaced it, and nothing of the stale act is written on any task"
           [(on-every-task (nth (:act-id crossing) 2) 1 (:stamp (:answer crossing-result)) nil) {}]
           [(facts-on-every-task other-entity :crossed) (facts-on-every-task first-entity :crossing-again)])
    (shown "an act of three values on three entities accepted whole:" (told accepted) "| each of its facts is on"
           (pr-str (mapv count places-after)) "tasks | the text in each row:" (pr-str (mapv (fn [rows] (peek (first (vals rows)))) places-after)))
    (shown "one refused whole with no row of it on any task:" (told refused) "| rows at its three places:" (pr-str places-after-refusal))
    (shown "an act of two facts that replaces the fact of the act before it in the line:" (told crossing-result)
           "| the same replaces again:" (told crossing-again-result)
           "| the base's last stamp before the act of three values" last-stamp-before "after" last-stamp-after)))

(deftest each-gate-tells-the-other-gates-layers
  (let [base-on-streaming (decide! (thing :base :system system-own [{:e :wrong-depot :k :note}]
                                          {:act-id [:base :streaming (UUID/randomUUID)] :depot :streaming}))
        on-microbatch (fn [x] (assoc x :act-id [(:layer x) :microbatch (UUID/randomUUID)] :depot :microbatch))
        later-own [:made-later :made-later :made-later]
        later-act (fn [] (on-microbatch (thing :made-later :made-later later-own [{:e :wrong-depot :k :note}])))
        [alice-on-microbatch later-before later-making-on-microbatch]
        (decide-in-one-batch!
         [(on-microbatch (thing :alice :alice alice-own [{:e :wrong-depot :k :note}]))
          (later-act)
          (on-microbatch (making :made-later :made-later :made-later))])
        depot-before (depot-of-layer :made-later)
        made-by-streaming (decide! (making :made-later :made-later :made-later))
        depot-after (depot-of-layer :made-later)
        later-after (decide-in-base! (later-act))
        strays-before (base-strays)
        last-stamps-before [(base-last-stamp) (base-last-stamp :alice)]
        both (alice [{:e :both-depots :k :text}])
        on-streaming (decide! both)
        malformed {:act-id [:base :microbatch] :layer :base :marker (UUID/randomUUID)}
        long-id (keyword (apply str (repeat 300 "a")))
        too-long {:act-id [long-id :microbatch (UUID/randomUUID)] :layer long-id}
        other-layer (assoc (system-act [{:e :stray-doc :k :text}]) :layer :team)
        acks (mapv append-to-base! [both 5 5 malformed too-long other-layer])
        _ (wait-until-processed!)
        strays-after (base-strays)
        last-stamps-after [(base-last-stamp) (base-last-stamp :alice)]
        records (vec (stored-in-base))
        base-strays-sent [[both :act-id-of-the-other-gate] [5 :no-act-id] [malformed :act-id-malformed]
                          [too-long :id-too-long] [other-layer :act-id-of-another-layer]]
        alices-partitions (partitions-of records (:thing alice-on-microbatch))
        alices-answer-tasks (set (keys (rows-by-task :mb-answers-ps
                                                     (record/answer-key :alice (nth (:act-id (:thing alice-on-microbatch)) 2)))))
        code (source-of-the-store)
        in-file (fn [file pattern] (get (named-in code pattern) file 0))
        gate-code (some (fn [[path text]] (when (str/ends-with? path "microbatch_gate.clj") text)) code)
        three [#"\$\$streaming-" #"currentTimeMillis" #"depot-partition-append!"]
        of-the-walk [#"\(\|[a-z]" #"yield-if-overtime" #":allow-yield\?"]
        a-text "(|hash *k) (yield-if-overtime) (local-select> [ALL] $$streaming-facts {:allow-yield? true}) (depot-partition-append! *d 1)"]
    (check "an act for the base, a microbatch layer, sent to the streaming depot after the base is made is refused as an act into a layer of the other gate, under its streaming act-id: the word that such an act got before the base was made"
           [:acked :layer-belongs-to-the-other-gate true] [(:ack base-on-streaming) (:word base-on-streaming) (:mine? base-on-streaming)])
    (check "an act for a streaming layer that is made, sent to the microbatch depot, is refused the same way, and is read back with its layer as the routing id"
           [:layer-belongs-to-the-other-gate true] [(:word alice-on-microbatch) (:mine? alice-on-microbatch)])
    (check "an act for a layer that starts streaming and that no gate has made, sent to the microbatch depot, gets the same word, and so does a making act of that layer: the microbatch gate tells the layer from its id, and does not make it"
           [:layer-belongs-to-the-other-gate true :layer-belongs-to-the-other-gate nil]
           [(:word later-before) (:mine? later-before) (:word later-making-on-microbatch) depot-before])
    (check "the streaming gate then makes that layer, and an act for it on the microbatch depot gets the word that it got before the layer was made"
           [:accepted :streaming :layer-belongs-to-the-other-gate]
           [(:word made-by-streaming) depot-after (:word later-after)])
    (check "none wrote a fact, in either record: the base's place in the streaming gate's facts holds nothing, and no task of the microbatch gate holds a fact or a depot row of either streaming layer"
           [nil {} {} {}]
           [(fact-row :base :wrong-depot :note)
            (rows-by-task :mb-facts-ps (record/place-key :alice :wrong-depot :note))
            (rows-by-task :mb-facts-ps (record/place-key :made-later :wrong-depot :note))
            (rows-by-task :mb-facts-ps (record/place-key :made-later :made-later :softland.store/depot))])
    (check "the refusal of the act for Alice's layer is on one task of the microbatch gate, the partition of her layer on the microbatch depot, and that task keeps the stamp of that refusal as her layer's last stamp there"
           [1 alices-partitions (:stamp (:answer alice-on-microbatch))]
           [(count alices-partitions) alices-answer-tasks (second last-stamps-before)])
    (check "each thing was appended" (vec (repeat 6 :appended)) acks)
    (check "one act-id sent on both depots has one answer, the streaming gate's: the microbatch gate keeps the thing as a stray"
           [:accepted (:answer on-streaming)] [(:word on-streaming) (answer-of (:act-id both))])
    (check "a stray on the microbatch depot is kept under its word, once however often it comes: five different strays, one for each word, one of them sent twice"
           {:no-act-id 1 :act-id-malformed 1 :id-too-long 1 :act-id-of-the-other-gate 1 :act-id-of-another-layer 1}
           (merge-with - strays-after strays-before))
    (check "a stray gets no answer and writes no fact: an act-id whose layer is not the thing's layer has none, and its fact is on no task"
           [nil {}] [(answer-of (:act-id other-layer)) (facts-on-every-task :stray-doc :text)])
    (check "the digest of each stray is kept on the task of the depot partition where the stray arrived: one partition for each of the five, the base's for the thing whose layer is the base, and the partition of Alice's layer for the thing whose layer is hers"
           [[1 1 1 1 1] true true
            (mapv (fn [[x _]] (first (partitions-of records x))) base-strays-sent)]
           [(mapv (fn [[x _]] (count (partitions-of records x))) base-strays-sent)
            (= #{(:base-task @kept)} (partitions-of records malformed))
            (= alices-partitions (partitions-of records both))
            (mapv (fn [[x word]] (task-of-stray :mb-task-ps word (digest/hex-of x))) base-strays-sent)])
    (check "a stray gets no stamp: after the six strays the last stamp of the base, and the last stamp that the microbatch gate keeps for Alice's layer, are as they were"
           last-stamps-before last-stamps-after)
    (check "in the code of the store's source, strings and comments left out: the microbatch gate's file and the walk's name no table of the streaming gate, and hold no `currentTimeMillis` and no `depot-partition-append!`; and the walk's file holds no partitioner, no `yield-if-overtime` and no `:allow-yield?`"
           [[0 0 0] [0 0 0] [0 0 0]]
           [(mapv #(in-file "microbatch_gate.clj" %) three) (mapv #(in-file "walk.clj" %) three) (mapv #(in-file "walk.clj" %) of-the-walk)])
    (check "the search finds each word where it is: the microbatch gate's file names its own tables, the door's file reads the clock, and a text that the proof wrote with the six words in it gives each once"
           [true true [1 0 1] [1 1 1]]
           [(pos? (in-file "microbatch_gate.clj" #"\$\$microbatch-")) (pos? (in-file "door.clj" #"currentTimeMillis"))
            (mapv #(found-in-a-text a-text %) three) (mapv #(found-in-a-text a-text %) of-the-walk)])
    (check "the microbatch gate's file holds one partitioner, `|all`, and it stands behind the call of the walk: the copy goes out when the decision is written"
           [1 true]
           [(in-file "microbatch_gate.clj" #"\(\|[a-z]")
            (boolean (and gate-code (str/index-of gate-code "(walk/decide") (str/index-of gate-code "(|all)")
                          (< (str/index-of gate-code "(walk/decide") (str/index-of gate-code "(|all)"))))])
    (shown "rule 7 in both directions: an act for a microbatch layer sent to the streaming depot:" (told base-on-streaming)
           "| an act for a streaming layer sent to the microbatch depot:" (told alice-on-microbatch)
           "| for a layer that starts streaming, before it is made:" (:word later-before) "| a making act of it there:" (:word later-making-on-microbatch)
           "| after the streaming gate made it:" (:word later-after))
    (shown "one act-id on both depots has one answer:" (told on-streaming) "| strays of the microbatch gate, by word:"
           (pr-str (merge-with - strays-after strays-before)) "| the same stray sent twice counts once | each digest is on the task of the partition where its stray arrived:"
           (pr-str (mapv (fn [[x word]] [word (task-of-stray :mb-task-ps word (digest/hex-of x))]) base-strays-sent)))
    (shown "the code of the microbatch gate and of the walk: tables of the streaming gate, currentTimeMillis, depot-partition-append!, in each file:"
           (pr-str (mapv #(in-file "microbatch_gate.clj" %) three)) (pr-str (mapv #(in-file "walk.clj" %) three))
           "| partitioners, yield-if-overtime and :allow-yield? in the walk:" (pr-str (mapv #(in-file "walk.clj" %) of-the-walk))
           "| partitioners in the microbatch gate:" (in-file "microbatch_gate.clj" #"\(\|[a-z]"))))

;; ------------------------------------ what the microbatch gate keeps for a read

(deftest what-a-fact-keeps-in-the-base
  (let [facts (mapv (fn [[k v]] (value-fact :kept-doc k v)) kinds-of-value)
        landed (decide-in-base! (system-act facts))
        rows (into {} (map (fn [k] [k (base-fact :kept-doc k)])) (keys kinds-of-value))
        rows-on-tasks (into {} (map (fn [k] [k (rows-by-task :mb-facts-ps (record/place-key :base :kept-doc k))])) (keys kinds-of-value))
        logged (record-of-act (stored-in-base) (:act-id (:thing landed)))
        logged-as-given (:v (some (fn [fact] (when (= :as-given (:k fact)) fact)) (:facts logged)))
        edges (decide-in-base! (system-act (mapv (fn [[k [v _]]] (value-fact :edges-doc k v)) edges-of-a-value)))
        edge-rows (into {} (map (fn [k] [k (base-fact :edges-doc k)])) (keys edges-of-a-value))
        [inside-33 refused]
        (decide-in-one-batch! [(system-act [(value-fact :edges-doc :inside-33 (nested 33 1))])
                               (system-act [(value-fact :not-kept-doc :text "a good value, and then dropped")
                                            (assoc (value-fact :not-kept-doc-2 :text "x") :replaces [(new-act-id :base) 0])])])
        alices-row (base-fact :alices-doc :text)
        retract (decide-in-base! (thing :base :alice alice-in-base [{:e :alices-doc :k :gone}]))
        retract-row (base-fact :alices-doc :gone)
        give-row (base-fact :alice :softland.store/permission)]
    (check "an act with a plain value of each kind, on one entity of the base, is accepted" [:accepted true] [(:word landed) (:mine? landed)])
    (check "each row of the base holds the one text of its value in its normal form, as the proof wrote it out by hand"
           texts-of-kinds (into {} (map (fn [[k row]] [k (:value row)])) rows))
    (check "each row gives back the UTF-8 of that text, and holds seven fields and no other; its time is the time on the act"
           [(into {} (map (fn [[k t]] [k (utf8-of t)])) texts-of-kinds) #{#{:value :by-act :by-index :who :permission :stamp :time}}
            #{(time-on (:thing landed))}]
           [(into {} (map (fn [[k row]] [k (value-bytes row)])) rows) (set (map (comp set keys) (vals rows)))
            (set (map :time (vals rows)))])
    (check "each of those rows is on every task, the same in every field"
           (into {} (map (fn [[k row]] [k [tasks #{row}]])) rows)
           (into {} (map (fn [[k by-task]] [k [(count by-task) (set (vals by-task))]])) rows-on-tasks))
    (check "each row reads back as the value in its normal form: the value that the maker gave, for each that was given so"
           (into {} (map (fn [[k v]] [k [:value v]])) kinds-read-back)
           (into {} (map (fn [[k row]] [k (record/read-value row)])) rows))
    (check "the act in the microbatch depot's log is as the hand sent it: each fact holds its value plain, as given and not in its normal form, so the decimal keeps its trailing zero and the big integer is no fixed-width one"
           [facts "1.50" false]
           [(vec (:facts logged)) (str (:b logged-as-given)) (instance? Long (get-in logged-as-given [:a 1]))])
    (check "false, an empty string and an empty map are values and no retract, and a number inside 32 vectors is a value: the act is accepted, each row holds the text of its value, and each reads back as the value"
           [:accepted
            (into {} (map (fn [[k [_ t]]] [k t])) edges-of-a-value)
            (into {} (map (fn [[k [v _]]] [k [:value v]])) edges-of-a-value)]
           [(:word edges)
            (into {} (map (fn [[k row]] [k (:value row)])) edge-rows)
            (into {} (map (fn [[k row]] [k (record/read-value row)])) edge-rows)])
    (check "a number inside 33 vectors is no value: the act is refused as malformed, the refusal is recorded, and no task holds a row of it"
           [:malformed true {}]
           [(:word inside-33) (:mine? inside-33) (rows-by-task :mb-facts-ps (record/place-key :base :edges-doc :inside-33))])
    (check "whole or nothing: an act whose first fact is good and whose second names a fact that is not current is refused whole, with no row at either place on any task"
           [:replaced-fact-not-current {} {}]
           [(:word refused) (rows-by-task :mb-facts-ps (record/place-key :base :not-kept-doc :text))
            (rows-by-task :mb-facts-ps (record/place-key :base :not-kept-doc-2 :text))])
    (check "each row holds who wrote it and the permission that the act named: the system's rows the system and its first permission; a person's rows her and the permission that the system gave her; a retract's row too; the give's row the giver"
           [#{[:system system-own]} [:alice alice-in-base [:value "alice's text in the base"]] :accepted [:alice alice-in-base [:nothing-here]]
            [:system system-own [:value alice-in-base]]]
           [(set (map (fn [row] [(record/who-of row) (record/permission-of row)]) (vals rows)))
            [(record/who-of alices-row) (record/permission-of alices-row) (record/read-value alices-row)]
            (:word retract)
            [(record/who-of retract-row) (record/permission-of retract-row) (record/read-value retract-row)]
            [(record/who-of give-row) (record/permission-of give-row) (record/read-value give-row)]])
    (shown "a value of each kind, kept by the microbatch gate as its one text in its row, the same row on every task:"
           (pr-str (into (sorted-map) (map (fn [[k row]] [k (:value row)])) rows)))
    (shown "the act in the microbatch depot's log holds each value plain, as given: the map that was not in its normal form is" (pr-str logged-as-given)
           "in the log and" (:value (get rows :as-given)) "in its row")
    (shown "the edges of a value in the base, each accepted with its text in its row:"
           (pr-str (into (sorted-map) (map (fn [[k row]] [k (:value row)])) edge-rows)) "| a number inside 33 vectors:" (told inside-33))
    (shown "who wrote a fact of the base, and under which permission:" (pr-str [(record/who-of alices-row) (record/permission-of alices-row)])
           "| an act refused for its second fact leaves no row of its first:" (told refused))))

(deftest the-keys-that-take-a-second-write-in-the-base
  (let [place (fn [k] (record/place-key :base :base k))
        rows (fn [k] (rows-by-task :mb-facts-ps (place k)))
        before (mapv rows [who-may-see builds-on marks])
        write (fn [k v & {:as more}] (system-act [(merge {:e :base :k k :v v} more)]))
        firsts (decide-in-one-batch! [(write who-may-see :members) (write builds-on [:team :alice]) (write marks #{:tag})])
        after-first (mapv rows [who-may-see builds-on marks])
        first-marking (:stamp (:answer (nth firsts 2)))
        seconds (decide-in-one-batch! [(write who-may-see :everyone) (write builds-on []) (write marks #{})])
        after-second (mapv rows [who-may-see builds-on marks])
        fact-of (fn [results i] [(:act-id (:thing (nth results i))) 0])
        named (decide-in-one-batch! [(write who-may-see :members :replaces (fact-of seconds 0))
                                     (write builds-on [:team] :replaces (fact-of seconds 1))
                                     (write marks #{:tag} :replaces (fact-of seconds 2))])
        after-named (mapv rows [who-may-see builds-on marks])
        second-marking (:stamp (:answer (nth named 2)))
        refused (decide-in-one-batch!
                 [(write who-may-see :everyone :replaces (fact-of firsts 0))
                  (write builds-on [] :replaces (fact-of firsts 1))
                  (write marks #{} :replaces (fact-of firsts 2))
                  (system-act [{:e :base :k who-may-see}])
                  (system-act [{:e :base :k builds-on}])
                  (system-act [{:e :base :k marks}])
                  (system-act [{:e :team :k who-may-see :v :everyone}])
                  (system-act [{:e :team :k builds-on :v [:base]}])
                  (system-act [{:e :team :k marks :v #{:tag}}])
                  (write who-may-see :owner)
                  (write builds-on #{:team})
                  (write builds-on (mapv #(keyword (str "layer-" %)) (range 65)))
                  (write marks [:tag])
                  (write marks (set (map #(keyword (str "key-" %)) (range 33))))
                  (write marks #{:softland.store/permission})])
        after-refused (mapv rows [who-may-see builds-on marks])
        values-of (fn [by-task] (set (map record/read-value (vals by-task))))
        by-alice (decide-in-base! (thing :base :alice alice-in-base [{:e :base :k who-may-see :v :everyone}]))
        after-alice (rows who-may-see)
        on-the-other-depot (decide! (thing :base :system system-own [{:e :base :k who-may-see :v :members}]
                                           {:act-id [:base :streaming (UUID/randomUUID)] :depot :streaming}))
        after-the-other-depot (rows who-may-see)]
    (check "before any fact says it, no task holds a row at the place of any of the three keys, and the base reads as: seen by everyone, building on nothing, marking no key"
           [[{} {} {}] [:everyone [] #{}]]
           [before (mapv #(first-act/starts-with :base %) [who-may-see builds-on marks])])
    (check "each of the three keys is written in the base, in one batch: accepted, and its row is on every task, the same on each"
           [[:accepted :accepted :accepted] [tasks tasks tasks] [#{[:value :members]} #{[:value [:team :alice]]} #{[:value #{:tag}]}]]
           [(mapv :word firsts) (mapv count after-first) (mapv values-of after-first)])
    (check "each is written a second time, naming nothing: accepted, and every task holds the second write"
           [[:accepted :accepted :accepted] [tasks tasks tasks] [#{[:value :everyone]} #{[:value []]} #{[:value #{}]}]]
           [(mapv :word seconds) (mapv count after-second) (mapv values-of after-second)])
    (check "each is written again naming the fact that is there: accepted"
           [[:accepted :accepted :accepted] [#{[:value :members]} #{[:value [:team]]} #{[:value #{:tag}]}]]
           [(mapv :word named) (mapv values-of after-named)])
    (check "a stale replaces, a retract, another entity and a wrong value are each refused with a recorded word, for each of the three keys"
           (vec (concat (repeat 3 :replaced-fact-not-current) (repeat 3 :store-key-retract-not-built)
                        (repeat 3 :setting-not-on-the-layer)
                        [:who-may-see-fact-not-members-or-everyone :builds-on-fact-not-a-list-of-layers :builds-on-too-many-layers
                         :marked-keys-fact-not-a-set-of-keys :too-many-marked-keys :marked-key-in-the-stores-namespace]))
           (mapv :word refused))
    (check "after those fifteen refusals every task holds the rows that the last accepted writes left, with the same stamps"
           (mapv (fn [by-task] (into {} (map (fn [[task row]] [task (:stamp row)])) by-task)) after-named)
           (mapv (fn [by-task] (into {} (map (fn [[task row]] [task (:stamp row)])) by-task)) after-refused))
    (check "the row of the marked keys holds on every task, derived by the gate, the stamp of each marked key: the stamp of the act that marked it; none when the set is empty; and the stamp of the later act when the key is marked again"
           [#{{:tag first-marking}} #{{}} #{{:tag second-marking}} true]
           [(set (map record/marked-keys-of (vals (nth after-first 2)))) (set (map record/marked-keys-of (vals (nth after-second 2))))
            (set (map record/marked-keys-of (vals (nth after-named 2)))) (< first-marking second-marking)])
    (check "who may write the three is whoever may act in the layer, kept and not decided: a person with a permission in the base sets the base to everyone, on every task"
           [:accepted tasks #{[:value :everyone]} #{:alice}]
           [(:word by-alice) (count after-alice) (values-of after-alice) (set (map record/who-of (vals after-alice)))])
    (check "a setting for the base sent to the streaming depot is refused as any act into a layer of the other gate, and the base's rows are as they were"
           [:layer-belongs-to-the-other-gate (into {} (map (fn [[task row]] [task (:stamp row)])) after-alice)]
           [(:word on-the-other-depot) (into {} (map (fn [[task row]] [task (:stamp row)])) after-the-other-depot)])
    (shown "the three keys that take a second write, in the base: written, written again, written again naming the fact there:"
           (pr-str (mapv :word firsts)) (pr-str (mapv :word seconds)) (pr-str (mapv :word named))
           "| each row is on" (pr-str (mapv count after-named)) "tasks | refused, by word:" (pr-str (frequencies (map :word refused))))
    (shown "the stamps of the base's marked keys, derived by the gate, the same on every task: after the first write"
           (pr-str (set (map record/marked-keys-of (vals (nth after-first 2))))) "where the stamp of the marking act is" first-marking
           "| after the set was emptied" (pr-str (set (map record/marked-keys-of (vals (nth after-second 2)))))
           "| after the key was marked again" (pr-str (set (map record/marked-keys-of (vals (nth after-named 2)))))
           "where the stamp of that act is" second-marking)))

(deftest what-a-permission-allows-in-the-base
  (let [erin-sees [:erin :base :base system-own :see]
        erin-without-the-slot [:erin :base :base system-own]
        erin-acts-too [:erin :base :base system-own :see-and-act]
        gretas [:greta :base :base system-own :see-and-act]
        [give-see in-the-same-batch] (decide-in-one-batch!
                                      [(thing :base :system system-own [{:e :erin :k :softland.store/permission :v erin-sees}])
                                       (thing :base :erin erin-sees [{:e :erin :k :note}])])
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :erin :softland.store/permission))
        [acts names-less names-more gives raise give-greta]
        (decide-in-one-batch!
         [(thing :base :erin erin-sees [{:e :erins-doc :k :note}])
          (thing :base :erin erin-without-the-slot [{:e :erins-doc :k :note}])
          (thing :base :erin erin-acts-too [{:e :erins-doc :k :note}])
          (thing :base :erin erin-sees [{:e :frank :k :softland.store/permission :v [:frank :base :base erin-sees :see]}])
          (system-act [{:e :erin :k :softland.store/permission :v erin-acts-too}])
          (system-act [{:e :greta :k :softland.store/permission :v gretas}])])
        sends-a-value (decide-in-base! (thing :base :erin erin-sees [(value-fact :erins-value :text "a value of a holder of see")]))
        greta-writes (decide-in-base! (thing :base :greta gretas [{:e :gretas-doc :k :note}]))
        permission-rows-after (rows-by-task :mb-facts-ps (record/place-key :base :erin :softland.store/permission))]
    (check "the system gives a person a permission in the base that says see: accepted, and its row is on every task with the whole text, slot and all"
           [:accepted tasks #{[:value erin-sees]}]
           [(:word give-see) (count permission-rows) (set (map record/read-value (vals permission-rows)))])
    (check "her act in the batch of the give, after it, gets the word of a permission that stands and says see, so it was decided against the row that the give wrote in that batch: refused, an act needs act"
           :permission-does-not-allow-acting (:word in-the-same-batch))
    (check "in a later batch her act is refused with the same word, under its own act-id with the digest of what was sent, and no task holds a row of it"
           [:permission-does-not-allow-acting true {}]
           [(:word acts) (:mine? acts) (rows-by-task :mb-facts-ps (record/place-key :base :erins-doc :note))])
    (check "she names her permission without the slot, and with see and act in it: it does not stand"
           [:permission-does-not-stand :permission-does-not-stand] [(:word names-less) (:word names-more)])
    (check "she cannot give, and her act with a value is refused with the same word, under its own act-id with the digest of what was sent, and no task holds a row of it"
           [:permission-does-not-allow-acting {} :permission-does-not-allow-acting true {}]
           [(:word gives) (rows-by-task :mb-facts-ps (record/place-key :base :frank :softland.store/permission))
            (:word sends-a-value) (:mine? sends-a-value) (rows-by-task :mb-facts-ps (record/place-key :base :erins-value :text))])
    (check "a second give to her, of see and act, is refused: a permission's place takes one write, and her row on each task is as it was, in every field"
           [:store-key-takes-one-write tasks permission-rows]
           [(:word raise) (count permission-rows-after) permission-rows-after])
    (check "a permission that says see and act, given in the same batch: its holder acts in a later batch" [:accepted :accepted]
           [(:word give-greta) (:word greta-writes)])
    (shown "in the base, a permission that says see:" (pr-str (record/value-of (first (vals permission-rows)))) "on" (count permission-rows) "tasks"
           "| its holder acts in the batch of the give:" (:word in-the-same-batch) "| in a later batch:" (told acts)
           "| names it without the slot:" (:word names-less) "| with see and act:" (:word names-more) "| gives:" (:word gives)
           "| sends a value:" (told sends-a-value) "| a second give of see and act:" (:word raise))))

(deftest what-points-at-a-thing-in-the-base
  (let [[e1 e2 e3] (:base-entities @kept)
        u (UUID/randomUUID)
        place (fn [e] (base-fact e :on))
        writes! (fn [e v & {:as more}] (decide-in-base! (system-act [(merge (value-fact e :on v) more)])))
        marks! (fn [keys] (decide-in-base! (system-act [{:e :base :k marks :v keys}])))
        key-tasks (fn [k] (vec (keys (rows-by-task :mb-by-value-ps k))))
        every-task (vec (range tasks))
        before-the-mark (writes! e3 :canvas-1)
        mark-1 (marks! #{:on})
        s1 (:stamp (:answer mark-1))
        marked-1 (base-marks-rows)
        run-0 (base-run-of :on s1 :canvas-1)
        a1 (writes! e1 :canvas-1)
        a2 (writes! e2 :canvas-1)
        run-1 (base-entries-of :on s1 :canvas-1)
        tasks-of-run-1 (mapv (fn [[k _]] (key-tasks k)) run-1)
        a3 (writes! e1 u :replaces [(:act-id (:thing a1)) 0])
        after-3 [(base-run-of :on s1 :canvas-1) (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-1 e1)) (base-run-of :on s1 u)]
        a4 (writes! e1 :canvas-2)
        after-4 [(base-run-of :on s1 u) (rows-by-task :mb-by-value-ps (base-entry-key :on s1 u e1)) (base-run-of :on s1 :canvas-2)]
        a5 (writes! e1 :canvas-2)
        after-5 (base-entries-of :on s1 :canvas-2)
        _ (append-to-base! (:thing a4))
        _ (wait-until-processed!)
        after-again (base-entries-of :on s1 :canvas-2)
        ;; an entry that a later act put back is not taken out when an
        ;; earlier act is appended again
        x1 (writes! :doc-9 :v1)
        x2 (writes! :doc-9 :v2)
        x3 (writes! :doc-9 :v1)
        _ (append-to-base! (:thing x2))
        _ (wait-until-processed!)
        after-put-back [(base-runs-by-task :on s1 :v1) (base-runs-by-task :on s1 :v2)
                        (set (map :by-act (vals (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :v1 :doc-9)))))]
        a6 (decide-in-base! (system-act [{:e e1 :k :on}]))
        after-6 [(base-runs-by-task :on s1 :canvas-2) (record/read-value (place e1))]
        mark-off (marks! #{})
        marked-off (base-marked-keys)
        a8 (writes! e2 :canvas-3)
        after-8 [(count (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-1 e2)))
                 (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-3 e2)) (record/read-value (place e2))]
        mark-2 (marks! #{:on})
        s2 (:stamp (:answer mark-2))
        marked-2 (base-marks-rows)
        after-mark-2 [(base-run-of :on s2 :canvas-3) (base-run-of :on s2 :canvas-1)]
        a10 (writes! e2 :canvas-4)
        after-10 [(base-run-of :on s2 :canvas-4) (base-run-of :on s2 :canvas-3)]
        a11 (decide-in-base! (system-act [{:e e1 :k :beside} (value-fact e2 :on :canvas-5)]))
        after-11 [(base-run-of :on s2 :canvas-4) (rows-by-task :mb-by-value-ps (base-entry-key :on s2 :canvas-4 e2))
                  (base-runs-by-task :on s2 :canvas-5) (count (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-1 e2)))]
        final-run (base-entries-of :on s2 :canvas-5)]
    (swap! kept assoc :base-marked-since s2)
    (check "each act of this walk is accepted"
           #{:accepted} (set (map :word [before-the-mark mark-1 a1 a2 a3 a4 a5 x1 x2 x3 a6 mark-off a8 mark-2 a10 a11])))
    (check "the mark puts the key in the row of the marked keys, on every task, with the stamp of the act that marked it; the key that an earlier check had marked, and that this set does not hold, is marked no more"
           [tasks #{{:on s1}}]
           [(count marked-1) (set (map record/marked-keys-of (vals marked-1)))])
    (check "nothing is filled from before: the fact that was written before the mark is in no run" [] run-0)
    (check "two facts on two entities with one value: both places are in the run of that value, each row of the run is on every task, and the row holds the fact that wrote it, its stamp, the time on its act, who and the permission, and no value"
           [(vec (sort-by str [e1 e2])) [every-task every-task]
            {:by-act (nth (:act-id (:thing a1)) 2) :by-index 0 :stamp (:stamp (:answer a1)) :time (time-on (:thing a1))
             :who ":system" :permission "[:system :base :base]"}]
           [(vec (sort-by str (map (fn [[k _]] (record/by-value-entity k)) run-1)))
            tasks-of-run-1
            (some (fn [[k row]] (when (= e1 (record/by-value-entity k)) row)) run-1)])
    (check "the fact is replaced, naming what it replaces, with a UUID: its place is out of the run of the value before, no task holds a row under the key of that entry, and it is in the run of the UUID"
           [[e2] {} [e1]] after-3)
    (check "the fact is written over with no replaces: out of the run of the UUID, no row under that key on any task, into the run of the new value"
           [[] {} [e1]] after-4)
    (check "the same value written again: the entry is written over, and holds the later fact, with the time on the later act"
           [[(base-entry-key :on s1 :canvas-2 e1)
             {:by-act (nth (:act-id (:thing a5)) 2) :by-index 0 :stamp (:stamp (:answer a5)) :time (time-on (:thing a5))
              :who ":system" :permission "[:system :base :base]"}]]
           after-5)
    (check "an earlier act that wrote that entry is appended a second time: the gate answers it from the record, and the run is as the later act left it"
           after-5 after-again)
    (check "an act that took an entry out is appended a second time, after a later act put the entry back: the gate answers it from the record and copies nothing, so on every task the entry is still there and is the later act's"
           [(vec (repeat tasks [:doc-9])) (vec (repeat tasks [])) #{(nth (:act-id (:thing x3)) 2)}]
           after-put-back)
    (check "a retract: the place is out of the run on every task, and holds a fact with no value" [(vec (repeat tasks [])) [:nothing-here]] after-6)
    (check "the mark is taken off by a second write of the set without the key" {} marked-off)
    (check "a fact that is replaced while the key is not marked writes nothing in that order: the row of the earlier value stays, on every task, and the new value has no row on any"
           [tasks {} [:value :canvas-3]] after-8)
    (check "the key is marked again: every task holds the new stamp, the stamp of the act that marked it again, later than the first"
           [tasks #{{:on s2}} true]
           [(count marked-2) (set (map record/marked-keys-of (vals marked-2))) (< s1 s2)])
    (check "under the new stamp the run of the place's value and the run of the value of the row that stayed are both empty: that row is under the old stamp, in no run that a read can name"
           [[] []] after-mark-2)
    (check "a fact written after the second mark, over a fact from before it: its place is in the run of its value, and the run of the value before is empty" [[e2] []] after-10)
    (check "an act of two facts writes over it: the entry of the value before is out, no task holds a row under its key, and the place is in the run of the new value on every task; the row of the first marking is still there, on every task"
           [[] {} (vec (repeat tasks [e2])) tasks] after-11)
    (shown "what points at a thing, in the base. The key :on is marked since" s1 ", the stamp of the act that marked it | two places hold one value: each row of its run is on tasks"
           (pr-str (first tasks-of-run-1)))
    (shown "the fact replaced, written over, written again, retracted; after each, the run of the value before, the rows under the key of the entry that went out, the run of the new value:"
           (pr-str after-3) (pr-str after-4) "| after the retract, the run on each task:" (pr-str (first after-6)))
    (shown "in the base, an act that took an entry out appended a second time, after a later act put the entry back: the run of the value on each task"
           (pr-str (first after-put-back)) "| of the other value" (pr-str (second after-put-back)))
    (shown "the mark taken off, a fact replaced, the key marked again since" s2 ": every run under the new stamp is empty" (pr-str after-mark-2)
           "| then a fact written, and written over by an act of two facts:" (pr-str after-10) (pr-str (subvec after-11 0 2))
           "| the run that a read then finds:" (pr-str (mapv (fn [[k _]] (record/by-value-entity k)) final-run)))))

(deftest one-batch-under-a-marked-key
  (let [[e1 e2 _] (:base-entities @kept)
        s2 (:base-marked-since @kept)
        [first-on-one-place second-on-one-place]
        (decide-in-one-batch! [(system-act [(value-fact e2 :on :first-of-two)])
                               (system-act [(value-fact e2 :on :second-of-two)])])
        place (base-fact e2 :on)
        after-two [(base-run-of :on s2 :canvas-5) (base-run-of :on s2 :first-of-two) (base-runs-by-task :on s2 :second-of-two)
                   (rows-by-task :mb-by-value-ps (base-entry-key :on s2 :first-of-two e2))]
        ;; A mark and the facts after it, in one batch: the marking act
        ;; holds a fact under the key that it marks; the next act writes the
        ;; key on two entities; one more entity; a replace; a retract.
        marking (system-act [{:e :base :k marks :v #{:on :in}} (value-fact :m0 :in :target-1)])
        next-act (system-act [(value-fact :m1 :in :target-1) (value-fact :m3 :in :target-1)])
        third (system-act [(value-fact :m2 :in :target-1)])
        replace-it (system-act [(assoc (value-fact :m1 :in :target-2) :replaces [(:act-id next-act) 0])])
        retract-it (system-act [{:e :m2 :k :in :replaces [(:act-id third) 0]}])
        in-one-batch (decide-in-one-batch! [marking next-act third replace-it retract-it])
        since (:stamp (:answer (first in-one-batch)))
        marked (base-marks-rows)
        entries (base-entries-under :in since)
        expected-entries {(base-entry-key :in since :target-1 :m3) [(nth (:act-id next-act) 2) 1]
                          (base-entry-key :in since :target-2 :m1) [(nth (:act-id replace-it) 2) 0]}
        own-fact (facts-on-every-task :m0 :in)
        retracted (mapv record/read-value (vals (rows-by-task :mb-facts-ps (record/place-key :base :m2 :in))))
        later (decide-in-base! (system-act [(value-fact :m4 :in :target-1)]))
        after-later (base-runs-by-task :in since :target-1)
        [see-a see-b] (decide-in-one-batch! [(system-act [{:e :base :k who-may-see :v :members}])
                                             (system-act [{:e e1 :k :beside-2} {:e :base :k who-may-see :v :everyone}])])
        see-rows (rows-by-task :mb-facts-ps (record/place-key :base :base who-may-see))]
    (swap! kept assoc :base-run {:k :in :since since :value :target-1})
    (check "two acts of one batch on one place under a marked key, each with the place as its own: both are accepted"
           [:accepted :accepted] [(:word first-on-one-place) (:word second-on-one-place)])
    (check "the place holds the second act's value; the entry of the value before the batch is out, the first act's value has no row on any task, and the place is in the run of the second value alone, on every task"
           [[:value :second-of-two] (:stamp (:answer second-on-one-place)) [[] [] (vec (repeat tasks [e2])) {}]]
           [(record/read-value place) (:stamp place) after-two])
    (check "a mark and the facts after it, appended while the gate was paused: the act that marks a key, the next act with two facts under it, a third fact, a replace of one and a retract of another: each is accepted"
           (vec (repeat 5 :accepted)) (mapv :word in-one-batch))
    (check "the key is marked on every task with the stamp of the act that marked it, and the key that was marked before keeps its stamp"
           [tasks #{{:on s2 :in since}}]
           [(count marked) (set (map record/marked-keys-of (vals marked)))])
    (check "the mark holds from the next act: on every task the order by value holds, under that key, exactly two entries, the second fact of the act straight after the mark and the entry of the replacing value, each written by its act. The replaced value's entry and the retracted one are out, on every task"
           (vec (repeat tasks expected-entries)) entries)
    (check "the act is decided with the marks as they were before it: the fact that the marking act itself holds under the key is in no run, though its row is on every task, with the stamp of the key"
           (on-every-task (nth (:act-id marking) 2) 1 since ":target-1") own-fact)
    (check "the retracted place holds a fact with no value on every task" (vec (repeat tasks [:nothing-here])) retracted)
    (check "a fact under that key in a later batch is accepted, and is in the order on every task, beside the one that stayed"
           [:accepted (vec (repeat tasks [:m3 :m4]))] [(:word later) after-later])
    (check "two writes of who may see the base in one batch: both accepted, and every task holds one row, the second one's"
           [:accepted :accepted tasks #{(nth (:act-id (:thing see-b)) 2)}]
           [(:word see-a) (:word see-b) (count see-rows) (set (map :by-act (vals see-rows)))])
    (check-the-copies "the copies, after the marks" (vec (stored-in-base)))
    (shown "two acts of one batch on one place under a marked key:" (told first-on-one-place) "|" (told second-on-one-place)
           "| the runs of the value before and of the first act's value:" (pr-str (vec (take 2 after-two)))
           "| of the second's, on each task:" (pr-str (nth after-two 2)))
    (shown "a mark and the facts after it in one batch: the key :in is marked since" since ", the stamp of the marking act, on" (count marked)
           "tasks | entries under it on each task:" (pr-str (mapv count entries)) "of" (count expected-entries)
           "expected | the marking act's own fact is in no run | with a later fact, the run of the value on each task:" (pr-str after-later))
    (shown "two writes of who may see the base in one batch: every task holds the row of" (short-id (:act-id (:thing see-b))))))

(defn- base-snapshot
  "What the proof reads of the base before and after the module is started
  again: the answer of the act that landed, the depot, the base's last
  stamp, the count of strays, a permission's row on each task, the marked
  keys on each task with their stamps, the run of one value in the facts by
  value on each task, the row of a value that was not given in its normal
  form and what a reader gets from the row of another."
  []
  {:answer (answer-of (:act-id (:thing (:base-landed @kept))))
   :depot (depot-of-layer :base)
   :last-stamp (base-last-stamp)
   :strays (base-strays)
   :permission (into (sorted-map) (map (fn [[task row]] [task (:stamp row)])) (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission)))
   :marked-keys (into (sorted-map) (map (fn [[task row]] [task (record/marked-keys-of row)])) (base-marks-rows))
   :run (let [{:keys [k since value]} (:base-run @kept)] (base-runs-by-task k since value))
   :kept-value (record/read-value (base-fact :kept-doc :map))
   :kept-row (base-fact :kept-doc :as-given)})

(deftest the-module-keeps-no-lock
  (let [u (UUID/randomUUID)
        asked [(table-of-the-module "$$streaming-facts") (table-of-the-module "$$microbatch-facts")
               (query-of-the-module "depot-of-layer" :alice)
               (query-of-the-module "answer-by-act-id" :alice [:alice :streaming u])
               (table-of-the-module "$$the-modules-proof-names-no-table")
               (query-of-the-module "the-modules-proof-names-no-query" :alice)
               (table-of-the-module "$$streaming-locks") (table-of-the-module "$$microbatch-locks")
               (query-of-the-module "locks-of-request" :alice [:alice :streaming u] 0 255)
               (query-of-the-module "locks-of-request" u [:base :microbatch u] 0 255)]
        carrying (fn [layer]
                   [{:e :no-lock-0 :k :softland.store/lock-request :v 2}
                    {:e :no-lock-1 :k :text :sealed (byte-array 40)}
                    (assoc (value-fact :no-lock-2 :text "a value beside a lock id") :lock-id [(new-act-id layer) 0])
                    {:e :no-lock-3 :k :text :sealed (byte-array 40) :lock-id [(new-act-id layer) 0]}])
        places [[:no-lock-0 :softland.store/lock-request] [:no-lock-1 :text] [:no-lock-2 :text] [:no-lock-3 :text]]
        expected-words [:store-key-not-known :unknown-part :unknown-part :unknown-part]
        through-streaming (mapv (fn [fact] (decide! (alice [fact]))) (carrying :alice))
        through-microbatch (decide-in-one-batch! (mapv (fn [fact] (system-act [fact])) (carrying :base)))
        streaming-records (vec (stored))
        microbatch-records (vec (stored-in-base))
        as-sent (fn [results] (mapv (comp content :thing) results))
        as-logged (fn [records results] (mapv (fn [result] (content (record-of-act records (:act-id (:thing result))))) results))]
    (check "asked of the cluster by name, with a select of one key or a call: the two tables of facts and the module's two queries are there; a table and a query that the module never had are not; and neither are a table of locks of either gate, nor the query of a request's locks, called with a streaming act-id and with a microbatch one"
           [:there :there :there :there :not-there :not-there :not-there :not-there :not-there :not-there]
           asked)
    (check "through the streaming gate: a fact under :softland.store/lock-request is refused as a key of the store's namespace that the store does not know, and a fact that carries :sealed, a value with :lock-id, and a fact with both are each refused as a part that the form does not name; each refusal is recorded under its own act-id with the digest of what was sent"
           [expected-words [true true true true]] [(mapv :word through-streaming) (mapv :mine? through-streaming)])
    (check "through the microbatch gate, in one batch: the same four words, each recorded under its own act-id with the digest of what was sent"
           [expected-words [true true true true]] [(mapv :word through-microbatch) (mapv :mine? through-microbatch)])
    (check "none of the eight wrote a row at its place, in either record"
           [[nil nil nil nil] [{} {} {} {}]]
           [(mapv (fn [[e k]] (fact-row :alice e k)) places)
            (mapv (fn [[e k]] (rows-by-task :mb-facts-ps (record/place-key :base e k))) places)])
    (check "each of the eight is in its depot's log as it was sent, the fact with each part that it carried, a run of bytes by its content"
           [(as-sent through-streaming) (as-sent through-microbatch)]
           [(as-logged streaming-records through-streaming) (as-logged microbatch-records through-microbatch)])
    (shown "the module keeps no lock. Asked of the cluster by name: the tables of facts and the two queries" (pr-str (subvec asked 0 4))
           "| a table and a query that the module never had" (pr-str (subvec asked 4 6))
           "| $$streaming-locks, $$microbatch-locks and locks-of-request, called for each depot" (pr-str (subvec asked 6)))
    (shown "a fact under :softland.store/lock-request, a fact with :sealed, a value with :lock-id, and a fact with both: through the streaming gate"
           (pr-str (mapv :word through-streaming)) "| through the microbatch gate" (pr-str (mapv :word through-microbatch))
           "| each is in its depot's log as sent, and none wrote a row")))

(deftest a-place-of-the-base-never-gives-nothing
  (let [e :never-empty-doc
        written (decide-in-base! (system-act [(value-fact e :text "the text before")]))
        old-uuid (nth (:act-id (:thing written)) 2)
        replacing (system-act [(assoc (value-fact e :text "the text after")
                                      :replaces [(:act-id (:thing written)) 0])])
        new-uuid (nth (:act-id replacing) 2)
        on-the-tasks (fn [] (on-each-task :mb-facts-ps (path/keypath (record/place-key :base e :text))))
        before-the-append (on-the-tasks)
        ack (append-to-base! replacing)
        deadline (+ (System/currentTimeMillis) time-limit-ms)
        seen (loop [seen [before-the-append]]
               (let [rows (on-the-tasks)
                     seen (conj seen rows)]
                 (if (or (every? #(= new-uuid (:by-act %)) rows) (< deadline (System/currentTimeMillis)))
                   seen
                   (do (Thread/sleep 2) (recur seen)))))
        read-at-first-sight (answer-of (:act-id replacing))
        _ (wait-until-processed!)
        result (base-result replacing ack)
        at-rest (on-the-tasks)
        each-row (apply concat seen)
        rounds-of-two-facts (count (filter (fn [rows] (< 1 (count (set (map :by-act rows))))) seen))
        went-once? (fn [task]
                     (let [writers (mapv #(:by-act (nth % task)) seen)]
                       (= writers (vec (concat (take-while #(= old-uuid %) writers)
                                               (repeat (count (drop-while #(= old-uuid %) writers)) new-uuid))))))]
    (swap! kept assoc :base-before (base-snapshot) :base-tables-before (base-tables))
    (check "a fact with a value is written to a place of the base, and an act that names it replaces it: both accepted"
           [:accepted :accepted true] [(:word written) (:word result) (:mine? result)])
    (check "from before the append of the replacing act until its row shows on every task, each select of the place, on each of the four tasks, straight from the facts table, gave a row and never nothing"
           [true true] [(< 2 (count seen)) (every? some? each-row)])
    (check "each row that a select gave was the fact before or the fact after, with the text of the value before or after; the first round of selects gave the one before on every task, and the last round the one after on every task"
           [#{[old-uuid "\"the text before\""] [new-uuid "\"the text after\""]} #{old-uuid} #{new-uuid}]
           [(set (map (juxt :by-act :value) each-row)) (set (map :by-act (first seen))) (set (map :by-act (peek seen)))])
    (check "on each task the place went from the fact before to the fact after once, and never back: no task showed the older row again behind the newer"
           (vec (repeat tasks true)) (mapv went-once? (range tasks)))
    (check "the rows that the last round of selects gave are the rows that the four tasks hold when the gate has processed the act, in every field"
           (mapv content at-rest) (mapv content (peek seen)))
    (shown "a place of the base that is replaced never gives nothing to a select that goes straight to the table, on any task:"
           (count seen) "rounds of four selects from before the append until the new row showed on every task | selects that gave the fact before:"
           (count (filter #(= old-uuid (:by-act %)) each-row)) "| the fact after:" (count (filter #(= new-uuid (:by-act %)) each-row))
           "| rounds in which some tasks showed the fact after and others the fact before, counted and not held to a number:" rounds-of-two-facts
           "| when every task showed the new row the read by act-id gave"
           (if read-at-first-sight "its answer" "nothing yet"))))

(deftest after-the-module-is-started-again
  (let [landed (:landed @kept)
        accepted (:act-id (:thing landed))
        row-before (fact-row :canvas :kept :as-given)
        stamp-before (last-stamp :alice)
        strays-before (strays :act-id-malformed)
        timed (:timed @kept)
        _ (rtest/update-module! (:ipc *hand*) module/Store)
        answer-after (answer-of accepted)
        repeat-ack (send! (:thing landed))
        stamp-after-repeat (last-stamp :alice)
        timed-after (mapv last-stamp (:layers timed))
        row-after (fact-row :canvas :kept :as-given)
        value-after (decide! (alice [(value-fact :note-40 :text "a value after the start")]))
        by-bob (decide! (thing :alice :bob bobs [{:e :bobs-note-4 :k :text}]))
        {:keys [since value run marked kept-value]} (:canvas-before @kept)]
    (check "the answer that was read before is the answer after" (:answer landed) answer-after)
    (check "what the gates keep for a read is as it was: what a reader gets from a row, the layer's marked keys with their stamps, and the run of a value in the facts by value"
           [kept-value marked run]
           [(record/read-value (fact-row :canvas :kept :map)) (marked-keys :canvas) (entries-of :canvas :on since value)])
    (check "the row of a value from before is the row after, in every field, and holds the text of the value's normal form still"
           [row-before "{:a [1 2] :b 1.5M}"] [row-after (:value row-after)])
    (check "a repeat after it is answered from the record: the layer's last stamp is the one from before"
           [:acked stamp-before] [repeat-ack stamp-after-repeat])
    (check "the last stamps of the two layers that are on one task are as they were, each its own: the two differ"
           [(:last-stamps timed) true] [timed-after (apply not= timed-after)])
    (check "the depot and the strays are as they were" [:streaming strays-before] [(depot-of-layer :alice) (strays :act-id-malformed)])
    (check "an act with a value after the start is accepted, and its row holds the text of the value"
           [:accepted "\"a value after the start\""] [(:word value-after) (:value (fact-row :alice :note-40 :text))])
    (check "the first stamp after is the layer's last stamp before plus one: the gate counted on from the number that its table kept through the start"
           (inc stamp-before) (:stamp (:answer value-after)))
    (check "a permission that was given before stands after" :accepted (:word by-bob))
    (shown "after the module is started again in the cluster, the record is as it was: the same answer, the same row of a value;"
           "the layer's last stamp before" stamp-before "the first stamp after" (:stamp (:answer value-after))
           "| the last stamps of two layers of one task, before and after:" (pr-str (:last-stamps timed)) (pr-str timed-after))))

(deftest the-stamps-and-the-depot
  (let [hands-append (let [ack (client-append (:streaming-depot *hand*) 5)]
                       (swap! things-sent conj 5)
                       ack)
        records (vec (stored))
        partition-of (into {} (keep (fn [[p r]] (when (and (map? r) (contains? r :layer)) [(:layer r) p]))) records)
        made [:alice :team :carrier]
        answers @first-answers
        by-layer (group-by :layer answers)
        by-partition (group-by #(get partition-of (:layer %)) answers)
        apart-from-the-rule (stamps-apart-from-the-rule answers)
        one-moved (stamps-apart-from-the-rule (update-in answers [1 :stamp] inc))
        last-stamps-apart (vec (keep (fn [[layer of-the-layer]]
                                       (when-not (= (:stamp (peek of-the-layer)) (last-stamp layer)) layer))
                                     by-layer))
        sent-digests (frequencies (map digest/hex-of @things-sent))
        logged-digests (frequencies (map (fn [[_ r]] (digest/hex-of r)) records))
        sent-times (times-in @things-sent)
        logged-times (times-in (map second records))
        sent-values (values-in @things-sent)
        logged-values (values-in (map second records))
        answered (answered-of @things-sent :streaming)
        depots-asked (mapv depot-of-the-module
                           ["*streaming-depot" "*microbatch-depot" "*batch-clock-depot" "*microbatch-answer-depot"
                            "*the-modules-proof-names-no-depot" "*streaming-answer-depot"])
        tables-asked (mapv table-of-the-module ["$$microbatch-facts" "$$batch-clock"])
        code (source-of-the-store)
        in-the-streaming-gate (fn [pattern] (get (named-in code pattern) "streaming_gate.clj" 0))
        in-the-microbatch-gate (fn [pattern] (get (named-in code pattern) "microbatch_gate.clj" 0))
        two-files ["src-store/softland/store/door.clj" "src-store/softland/store/read_path.clj"]]
    (check "the stamps of the answers increase strictly in each layer, in the order in which the acts were sent"
           true (every? (fn [[_ of-the-layer]] (apply < (map :stamp of-the-layer))) by-layer))
    (check "the order is the layer's and not the task's: on a task that holds more than one layer the stamps do not all go up in the order sent"
           true (boolean (some (fn [[_ on-the-task]] (not (apply < (map :stamp on-the-task)))) by-partition)))
    (check "each stamp is the number that the gate's rule gives, by the proof's own count: the stamp before it in its layer plus one, or 0 with none before it, for an accepted act and a refused thing alike, whatever the time on the thing and whatever stamps it carries. So the stamps of each layer are 0, 1, 2 and on, with no gap. It is held for each of the more than 150 answers that the hand read, and the answer of each act-id that has one is among them"
           [true (count answered) []]
           [(< 150 (count answers)) (count answers) apart-from-the-rule])
    (check "the check can fail: the same answers with the stamp of the second moved by one give that answer first, and beside it at most the one behind it in its layer, whose stamp before is then another"
           [true (short-id (:act-id (nth answers 1)))]
           [(<= 1 (count one-moved) 2) (first (first one-moved))])
    (check "the last stamp that the gate keeps for each layer is the stamp of the last answer of that layer"
           [] last-stamps-apart)
    (check "the depot holds each thing that was sent: accepted, refused, repeated and stray" (count @things-sent) (count records))
    (check "the layers of the proof are on more than one task" true (< 1 (count (set (vals partition-of)))))
    (check "each thing is on the depot as it was sent: the digests that the proof takes of the records read back are the digests of the things sent, each as often"
           [] (counts-apart sent-digests logged-digests #(subs % 0 16)))
    (check "a digest leaves the time on the act out, so the time is held apart: under each act-id the log holds the time that the hand wrote, or none where the hand wrote none, each as often as it was sent"
           [true []] [(< 150 (count sent-times)) (counts-apart sent-times logged-times (fn [[act-id time]] [(short-id act-id) time]))])
    (check "each value is plain on the depot: every value that a fact of a thing sent holds is in the log under its act-id, its entity and its key, as it was given; the hand sent more than 300 of them"
           [true []] [(< 300 (reduce + (vals sent-values))) (counts-apart sent-values logged-values #(subvec % 0 3))])
    (shown "the stamps of" (count answers) "answers increase strictly in each of" (count by-layer) "layers, and each is the number that the rule gives; answers by partition:"
           (pr-str (into (sorted-map) (map (fn [[p on-the-task]] [p (count on-the-task)])) by-partition)))
    (check "the module has two depots. Asked of the cluster by name, for the number of its partitions: the two depots of acts are there; a depot of batch clocks, a depot of answers of the microbatch gate, a depot that the module never had and a depot of answers of the streaming gate are not. Asked for a table by name, with a select of one key: the microbatch gate's facts are there, and a table of a batch's clock is not. The same call, made first, appended a thing to the depot of acts"
           [:appended [:there :there :not-there :not-there :not-there :not-there] [:there :not-there]]
           [hands-append depots-asked tables-asked])
    (check "in the code of the store's source, strings and comments left out, no file names a depot of answers of either gate, and none names a clock of a batch; the store declares two depots, both in the module's file"
           [{} {} {} {"module.clj" 2}]
           [(named-in code #"streaming-answer-depot")
            (named-in code #"microbatch-answer-depot")
            (named-in code #"batch-clock")
            (named-in code #"\(declare-depot\s")])
    (check "the code of the streaming gate's file names no table of the microbatch gate, and holds no `depot-partition-append!`, no `currentTimeMillis` and no partitioner; and it does name its own tables. The four searches find each word where it is: a table of the microbatch gate and a partitioner in that gate's file, `currentTimeMillis` in the door's file, and `depot-partition-append!` in a text that the proof wrote"
           [[0 0 0 0] true [true true true 1]]
           [(mapv in-the-streaming-gate [#"\$\$microbatch-" #"depot-partition-append!" #"currentTimeMillis" #"\(\|[a-z]"])
            (pos? (in-the-streaming-gate #"\$\$streaming-"))
            [(pos? (in-the-microbatch-gate #"\$\$microbatch-")) (pos? (in-the-microbatch-gate #"\(\|[a-z]"))
             (pos? (get (named-in code #"currentTimeMillis") "door.clj" 0))
             (found-in-a-text "(depot-partition-append! *depot 1)" #"depot-partition-append!")]])
    (check "no topology of the store reads a depot of answers, and neither the door nor the read path: no `source>` names one, and neither `door.clj` nor `read_path.clj` holds the word answer-depot, in its code or in a docstring; the same search finds the `source>` of each gate's depot of acts, and the word depot in each of the two files"
           [{} {"streaming_gate.clj" 1 "microbatch_gate.clj" 1} [false false] [true true]]
           [(named-in code #"source>[^()]*answer-depot")
            (named-in code #"\(source>\s+\*(streaming|microbatch)-depot\s")
            (mapv #(str/includes? (slurp %) "answer-depot") two-files)
            (mapv #(str/includes? (slurp %) "depot") two-files)])
    (shown "layers by depot partition:" (pr-str (select-keys partition-of made)) "| the depot holds all" (count records)
           "things sent, each as it was sent, with the time that the hand wrote | each of the" (reduce + (vals sent-values))
           "values that the hand sent is plain on it")
    (shown "the depots of the module, asked of the cluster by name: *streaming-depot, *microbatch-depot, *batch-clock-depot,"
           "*microbatch-answer-depot, one that the module never had, *streaming-answer-depot:" (pr-str depots-asked)
           "| the tables $$microbatch-facts and $$batch-clock:" (pr-str tables-asked))
    (shown "where the store's code names a depot of answers: of the streaming gate" (pr-str (named-in code #"streaming-answer-depot"))
           "| of the microbatch gate" (pr-str (named-in code #"microbatch-answer-depot"))
           "| a clock of a batch:" (pr-str (named-in code #"batch-clock"))
           "| a source> of a depot of answers:" (pr-str (named-in code #"source>[^()]*answer-depot"))
           "| where a depot is declared:" (pr-str (named-in code #"\(declare-depot\s")))))

(deftest the-base-after-the-module-is-started-again
  (let [before (:base-before @kept)
        landed (:base-landed @kept)
        after (base-snapshot)
        tables-after (base-tables)
        _ (append-to-base! (:thing landed))
        _ (wait-until-processed!)
        answer-after-repeat (answer-of (:act-id (:thing landed)))
        stamp-after-repeat (base-last-stamp)
        last-before (:last-stamp before)
        value-after (decide-in-base! (system-act [(value-fact :after-doc :text "a value after the start")]))
        next-after (decide-in-base! (system-act [(value-fact :after-doc-2 :text "a value after that one")]))
        by-alice (decide-in-base! (thing :base :alice alice-in-base [{:e :alices-doc :k :after}]))
        {:keys [places act-uuid]} (:base-values @kept)]
    (check "the answer, the depot of the base, the base's last stamp, the strays, a permission's rows, the marked keys, a run of the facts by value on each task, the row of a value and what a reader gets from another are as they were"
           before after)
    (check "the run that is read holds places, the same on every task: the check of the run is of rows"
           [true 1] [(pos? (count (first (:run after)))) (count (set (:run after)))])
    (check "the copies are still on every task: each of the gate's four tables holds on each task the rows that it held before the start, in every key and every field"
           [] (tables-apart (:base-tables-before @kept) tables-after))
    (check "the row of a value from before holds the text of the value's normal form still" "{:a [1 2] :b 1.5M}" (:value (:kept-row after)))
    (check "a repeat after it is answered from the record: the same answer, and the base's last stamp is the one from before"
           [(:answer before) last-before] [answer-after-repeat stamp-after-repeat])
    (check "an act with a value after the start is accepted in the base, and its row holds the text of the value on every task"
           [:accepted (on-every-task (nth (:act-id (:thing value-after)) 2) 0 (:stamp (:answer value-after)) "\"a value after the start\"")]
           [(:word value-after) (facts-on-every-task :after-doc :text)])
    (check "the first stamp after is the base's last stamp before plus one: the gate counted on from the number that its table kept through the start"
           (inc last-before) (:stamp (:answer value-after)))
    (check "and the next act gets the stamp after it: the base's count goes on one by one through the start"
           [:accepted (+ 2 last-before)] [(:word next-after) (:stamp (:answer next-after))])
    (check "a permission that the system gave before stands after" :accepted (:word by-alice))
    (check "each value of the act of three facts is in its row still, on every task, as the text of what was sent, written by that act"
           (mapv (fn [[e _]] [tasks #{[(str "\"the body of " (name e) "\"") act-uuid]}]) places)
           (mapv (fn [[e k]] (let [rows (facts-on-every-task e k)]
                               [(count rows) (set (map (fn [[by-act _ _ value]] [value by-act]) (vals rows)))]))
                 places))
    (shown "after the module is started again in the cluster, the base's record is as it was: the same answer, depot" (:depot after)
           "| the same row of a value | each of the four tables the same on each task:" (pr-str (tables-apart (:base-tables-before @kept) tables-after))
           "tables apart | the base's last stamp before" last-before "the first stamp after" (:stamp (:answer value-after))
           "| the stamp of the act after it:" (:stamp (:answer next-after)))))

(deftest the-stamps-and-the-depot-of-the-base
  (let [records (vec (stored-in-base))
        decided (decided-on-the-microbatch-depot records)
        by-layer (group-by :layer decided)
        apart-from-the-rule (stamps-apart-from-the-rule decided)
        one-moved (stamps-apart-from-the-rule (update-in decided [1 :stamp] inc))
        last-stamps-apart (vec (keep (fn [[layer of-the-layer]]
                                       (when-not (= (:stamp (peek of-the-layer)) (base-last-stamp layer)) layer))
                                     by-layer))
        of-the-base? (fn [x] (and (map? x) (= :base (:layer x))))
        sent-hex (mapv digest/hex-of @base-things-sent)
        logged-hex (mapv (fn [[_ r]] (digest/hex-of r)) records)
        sent-digests (frequencies sent-hex)
        logged-digests (frequencies logged-hex)
        base-sent-in-order (vec (keep-indexed (fn [i x] (when (of-the-base? x) (nth sent-hex i))) @base-things-sent))
        base-logged-in-order (vec (keep-indexed (fn [i [_ r]] (when (of-the-base? r) (nth logged-hex i))) records))
        sent-values (values-in @base-things-sent)
        logged-values (values-in (map second records))
        sent-times (times-in @base-things-sent)
        logged-times (times-in (map second records))
        base-partitions (into #{} (keep (fn [[p r]] (when (of-the-base? r) p))) records)
        first-entities (into #{} (keep (fn [[_ r]] (when (and (of-the-base? r) (sequential? (:facts r)) (map? (first (:facts r))))
                                                     (:e (first (:facts r))))))
                             records)
        streaming-records (vec (stored))
        on-both-depots (let [streaming (partitions-of-layers streaming-records)
                             microbatch (partitions-of-layers records)]
                         (into (sorted-map)
                               (keep (fn [[layer partitions]] (when-let [others (get microbatch layer)] [layer [partitions others]])))
                               streaming))]
    (check "each thing of the microbatch depot that is no stray has an answer under its act-id: the gate decided more than 600 things, and none is without one"
           [true []] [(< 600 (count decided)) (vec (keep (fn [d] (when (= :nothing-yet (:word d)) (short-id (:act-id d)))) decided))])
    (check "the stamps of the answers increase strictly in each layer of the microbatch depot, in the depot's order"
           true (every? (fn [[_ of-the-layer]] (apply < (map :stamp of-the-layer))) by-layer))
    (check "each stamp is the number that the gate's rule gives, by the proof's own count, as for the streaming gate: the stamp before it in its layer plus one, or 0 with none before it, for an accepted act and a refused thing alike, whatever the time on the thing and whatever stamps it carries. So the stamps of each layer are 0, 1, 2 and on, with no gap. It is held for each thing that the microbatch gate decided, in the depot's order"
           [] apart-from-the-rule)
    (check "the check can fail: the same answers with the stamp of the second moved by one give that answer first, and beside it at most the one behind it in its layer, whose stamp before is then another"
           [true (short-id (:act-id (nth decided 1)))]
           [(<= 1 (count one-moved) 2) (first (first one-moved))])
    (check "the last stamp that the gate keeps for each layer, on the layer's task, is the stamp of the last answer of that layer"
           [] last-stamps-apart)
    (check "the microbatch depot holds each thing that was sent to it: accepted, refused, repeated and stray"
           (count @base-things-sent) (count records))
    (check "each thing is on the microbatch depot as it was sent: the digests that the proof takes of the records read back are the digests of the things sent, each as often"
           [] (counts-apart sent-digests logged-digests #(subs % 0 16)))
    (check "the things of the base are on one partition of the depot, in the order in which the hand appended them: more than 600 things, whose first facts are on more than 50 different entities"
           [1 true true true]
           [(count base-partitions) (< 600 (count base-logged-in-order)) (< 50 (count first-entities))
            (= base-sent-in-order base-logged-in-order)])
    (check "both depots put a thing on the partition of its layer: on each depot the things of every layer are on one partition, and each layer that has things on both depots is on the partition of one number in both"
           [[] [] true true]
           [(on-more-than-one-partition streaming-records) (on-more-than-one-partition records)
            (< 1 (count on-both-depots)) (every? (fn [[_ [in-streaming in-microbatch]]] (= in-streaming in-microbatch)) on-both-depots)])
    (check "a digest leaves the time on the act out, so the time is held apart: under each act-id the microbatch depot's log holds the time that the hand wrote, or none where the hand wrote none, as often as it was sent"
           [true []] [(< 600 (count sent-times)) (counts-apart sent-times logged-times (fn [[act-id time]] [(short-id act-id) time]))])
    (check "each value is plain on the microbatch depot: every value that a fact of a thing sent holds is in the log under its act-id, its entity and its key, as it was given; the hand sent more than 300 of them"
           [true []] [(< 300 (reduce + (vals sent-values))) (counts-apart sent-values logged-values #(subvec % 0 3))])
    (check-the-copies "the copies, at the end of the run" records)
    (shown "the stamps of" (count decided) "answers of the microbatch gate, in" (count by-layer)
           "layers, increase strictly in each layer in the depot's order, and each is the number that the rule gives")
    (shown "the microbatch depot holds all" (count records) "things sent, each as it was sent |" (count base-logged-in-order)
           "things of the base, with" (count first-entities) "different first entities, are on partition" (pr-str base-partitions)
           "in the order appended | each of the" (reduce + (vals sent-values)) "values that the hand sent is plain on it")
    (shown "layers that have things on both depots, each with its partition on the streaming depot and on the microbatch depot:"
           (pr-str on-both-depots))))

;; ------------------------------------------- a gate that fails and runs again

(def ^:private first-writes
  "`softland.store.gate.decision/writes` as the store defines it, taken when
  this namespace was loaded, before any var is changed."
  decision/writes)

(def ^:private first-stray-reason
  "`softland.store.door.declaration/stray-reason` as the store defines it."
  declaration/stray-reason)

(defonce ^{:private true
           :doc "The batch of the microbatch gate that the proof makes fail on its
  second cluster, and what came of it:
  {:armed? :run :throw-at :calls :left :throws}. `:run` holds the act-ids of
  the batch's acts. `:throw-at` holds the act-id at whose decision the
  attempt is to fail, or nil for a run that is to commit. `:armed?` is true
  from the hand's wish until the throw, or until the hand takes it away:
  while it is, the stand-in of `writes` hands the walk one extra row for
  each other act of the run. `:calls` holds each call that the stand-in
  got for an act of the run, in the order made:
  {:act-id :wall :thread :refused :stamp :rows :armed?}, where `:rows` are
  the writes that the store's own function gave, each [which key row] with
  its bytes by content, and `:armed?` says whether the call came before the
  throw. `:left` holds the key of each extra row that was handed out.
  `:throws` holds each throw that was made, {:wall :thread}."}
  attempt-failure (atom {:armed? false :run #{} :throw-at nil :calls [] :left [] :throws []}))

(defonce ^{:private true
           :doc "The events of the streaming gate that the proof watches on its
  second cluster, and the one that it makes fail: {:to-fail :watched :calls}.
  `:to-fail` holds the act-id whose first decision is to throw before its
  rows are written, or nil. `:watched` holds the act-ids whose events are
  noted. `:calls` holds each call that a stand-in got for a watched act-id,
  in the order made: {:where :act-id :wall :thread}. `:where` is :run for a
  call of `stray-reason`, the first step of an event of the streaming gate,
  which says that a run of the thing's event began; and :decision for a
  call of `writes`, which says that a run decided the act. A :decision
  holds three parts more: `:refused`, the word of a refusal that the gate
  gave the call, or nil; `:stamp`, the stamp that it gave; and `:rows`, the
  writes that the call made, each [which key row] with its bytes by
  content."}
  event-failures (atom {:to-fail nil :watched #{} :calls []}))

(def ^:private left-entity
  "The entity of each row that only a failed attempt of a batch is handed.
  No act of the proof writes it."
  :left-by-a-failed-attempt)

(defn- left-key
  "The key of the extra row that the failed attempt is handed for one act:
  the place of `left-entity` in the base under a key made of the act's
  uuid, as text, written out by the proof's own hand."
  [act-uuid]
  (str ":base " left-entity " :of-" act-uuid))

(defn noting-stray-reason
  "Stands in for `softland.store.door.declaration/stray-reason` on the
  proof's second cluster. Takes what it takes and gives what it gives. It
  is the first step of the walk of one thing, which the streaming gate
  calls with the word :streaming: such a call says that a run of the
  thing's event began, and it is noted when the proof watches the thing's
  act-id. The microbatch gate and the proof's hand call it with the word
  :microbatch; those calls are not noted. The note never throws: a thing
  whose act-id cannot be read is not noted."
  [gate-depot thing]
  (when (= :streaming gate-depot)
    (try
      (let [act-id (when (map? thing) (get thing :act-id))]
        (swap! event-failures
               (fn [state]
                 (if (contains? (:watched state) act-id)
                   (update state :calls conj {:where :run :act-id act-id :wall (System/currentTimeMillis)
                                              :thread (.getName (Thread/currentThread))})
                   state))))
      (catch Throwable _ nil)))
  (first-stray-reason gate-depot thing))

(defn throwing-writes
  "Stands in for `softland.store.gate.decision/writes` on the proof's second
  cluster. Takes what it takes and gives what it gives, but for the two
  failures that the proof asks for. The walk calls it behind its reads and
  before its first write, and only for a thing that it decides, in both
  gates.

  An event of the streaming gate. For an act-id that the proof watches it
  notes each call, with the word of a refusal and the stamp that the gate
  gave it and the writes that it made. The first call for the act-id that
  is to fail throws behind its note: that event fails with no row written.
  What must hold for the throw: the act-id that is to fail is watched.

  An attempt of a batch of the microbatch gate. For an act-id of the run
  that `attempt-failure` names it notes each call in the same way. While
  the failure is armed: at the act of `:throw-at` it disarms, waits 300 ms
  so that the other tasks can write the copies already sent, and throws,
  this once; for each other act of the run it adds one write to the
  store's result, the act's first fact row again at `left-key` of the act.
  In the batch that fails those are the acts before the throw. The walk
  writes that row and the gate copies it as any row, so it is on every task
  in the failed attempt, and no attempt after is handed it. A run that is
  armed with no act to throw at commits with its extra rows: the proof
  uses one to show that such a row is written and copied."
  [act-id act refused stamp time-millis digest in-hand]
  (let [out (first-writes act-id act refused stamp time-millis digest in-hand)
        [before _] (swap-vals! event-failures
                               (fn [state]
                                 (if (contains? (:watched state) act-id)
                                   (update state :calls conj {:where :decision :act-id act-id :wall (System/currentTimeMillis)
                                                              :thread (.getName (Thread/currentThread))
                                                              :refused refused :stamp stamp :rows (mapv content out)})
                                   state)))
        fact-row (some (fn [[which _ row]] (when (= :fact which) row)) out)
        extra (when fact-row [:fact (left-key (nth act-id 2 nil)) fact-row])
        [attempt _] (swap-vals! attempt-failure
                                (fn [state]
                                  (if (contains? (:run state) act-id)
                                    (let [state (update state :calls conj {:act-id act-id :wall (System/currentTimeMillis)
                                                                           :thread (.getName (Thread/currentThread))
                                                                           :refused refused :stamp stamp :rows (mapv content out)
                                                                           :armed? (:armed? state)})]
                                      (cond (not (:armed? state)) state
                                            (= act-id (:throw-at state)) (-> state
                                                                             (assoc :armed? false)
                                                                             (update :throws conj {:wall (System/currentTimeMillis)
                                                                                                   :thread (.getName (Thread/currentThread))}))
                                            extra (update state :left conj (second extra))
                                            :else state))
                                    state)))
        armed-for-this? (and (:armed? attempt) (contains? (:run attempt) act-id))]
    (when (and (some? (:to-fail before)) (= act-id (:to-fail before))
               (not-any? #(and (= :decision (:where %)) (= act-id (:act-id %))) (:calls before)))
      (throw (ex-info "the module's proof: a forced failure of an event of the streaming gate, before its first write" {})))
    (cond
      (and armed-for-this? (= act-id (:throw-at attempt)))
      (do (Thread/sleep 300)
          (throw (ex-info "the module's proof: a forced failure of an attempt of a batch of the microbatch gate, in the middle of the batch" {})))

      (and armed-for-this? extra)
      (conj out extra)

      :else
      out)))

(def ^:private twice-limit-ms
  "How long the hand reads for the answers of a batch or of an event that
  fails and runs again, in milliseconds. A second run came 1 to 2 seconds
  after the first in the runs; an attempt that Rama ends by its phase
  timeout takes about 31."
  60000)

(defn- put-noting-functions!
  "Puts the two stand-ins into the vars of the store's two functions. What
  must hold: this namespace has loaded the store's module, and with it the
  walk, while the vars held the store's own functions; the walk then reads
  each var at each call."
  []
  (alter-var-root #'decision/writes (constantly throwing-writes))
  (alter-var-root #'declaration/stray-reason (constantly noting-stray-reason)))

(defn- put-the-stores-functions-back!
  "Puts the store's own two functions back into their vars."
  []
  (alter-var-root #'decision/writes (constantly first-writes))
  (alter-var-root #'declaration/stray-reason (constantly first-stray-reason)))

(defn- empty-the-hand!
  "Empties each atom of the proof but the shown lines: what the hand keeps
  of one cluster means nothing on the next, and Rama counts the things that
  a gate processed from the launch of a cluster."
  []
  (reset! things-sent [])
  (reset! first-answers [])
  (reset! kept {})
  (reset! base-things-sent [])
  (reset! attempt-failure {:armed? false :run #{} :throw-at nil :calls [] :left [] :throws []})
  (reset! event-failures {:to-fail nil :watched #{} :calls []}))

(defn- decide-with-a-failed-attempt!
  "The hand for an attempt of a batch that fails. Appends acts to the
  microbatch depot while the gate is paused, so that one batch takes them
  all; arms the failure for them, with the throw at the act of the index
  given; and lets the topology go on. The first attempt of the batch then
  fails at that act, the worker is started again, and Rama processes the
  batch a second time. Gives {:results :answered? :waited}: the result of
  each act in the order sent; whether each was answered within
  `twice-limit-ms`; and the milliseconds from the topology's going on to
  the last answer.

  It reads the answers in a loop that takes a read that throws as nothing
  yet: for about half a second after a worker is started again, a read can
  throw. What must hold: the hand is on the proof's second cluster, where
  the stand-ins run."
  [acts throw-index]
  (let [ipc (:ipc *hand*)
        module-name (:module *hand*)
        ids (mapv :act-id acts)]
    (rtest/pause-microbatch-topology! ipc module-name "microbatch-gate")
    (let [acks (mapv append-to-base! acts)
          _ (reset! attempt-failure {:armed? true :run (set ids) :throw-at (nth ids throw-index) :calls [] :left [] :throws []})
          _ (rtest/resume-microbatch-topology! ipc module-name "microbatch-gate")
          went-on (System/currentTimeMillis)
          answer-or-nil (fn [x] (try (answer-of (:act-id x)) (catch Throwable _ nil)))
          answered? (loop []
                      (cond (every? answer-or-nil acts) true
                            (< (+ went-on twice-limit-ms) (System/currentTimeMillis)) false
                            :else (do (Thread/sleep 50) (recur))))
          waited (- (System/currentTimeMillis) went-on)]
      {:results (mapv (fn [x ack]
                        (let [answer (answer-or-nil x)]
                          {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
                           :word (cond (nil? answer) :nothing-yet
                                       (= :accepted (:verdict answer)) :accepted
                                       :else (:reason answer))}))
                      acts acks)
       :answered? answered?
       :waited waited})))

(defn- decide-behind-a-failure!
  "Sends one thing to the microbatch depot, alone, behind a batch that
  failed and ran again, and reads its answer by act-id in a loop until one
  is there or `twice-limit-ms` has passed; a read that throws counts as no
  read. Gives the result, as `decide!` does. The loop is there because the
  proof does not rest, behind a failed attempt, on what Rama's test tool
  counts as processed."
  [x]
  (let [ack (append-to-base! x)
        began (System/currentTimeMillis)
        _ (try (wait-until-processed!) (catch Throwable _ nil))
        answer (loop []
                 (or (try (answer-of (:act-id x)) (catch Throwable _ nil))
                     (when (< (System/currentTimeMillis) (+ began twice-limit-ms))
                       (Thread/sleep 20)
                       (recur))))]
    {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

(defn- calls-for
  "The calls that the stand-ins noted for one act-id of the streaming gate
  on the proof's second cluster, of one kind, :run or :decision, in the
  order made."
  [where act-id]
  (filterv #(and (= where (:where %)) (= act-id (:act-id %))) (:calls @event-failures)))

(defn- send-and-await!
  "The hand for an event of the streaming gate that fails. Appends one thing
  to the streaming depot on a thread of its own and, while Rama fails the
  thing's event and runs its record again, reads the answer by act-id in a
  loop, until one is there or `twice-limit-ms` has passed. A read that
  throws counts as no read: for about half a second after a worker is
  started again a read can throw. Gives {:ack :answer :waited}: what the
  append gave, or :no-ack-in-time when it did not come back ten seconds
  after the loop; the answer, or nil; and the milliseconds from the append
  to the end of the loop."
  [x]
  (let [act-id (:act-id x)
        began (System/currentTimeMillis)
        sent (future (send! x))
        answer (loop []
                 (or (try (answer-of act-id) (catch Throwable _ nil))
                     (when (< (System/currentTimeMillis) (+ began twice-limit-ms))
                       (Thread/sleep 20)
                       (recur))))]
    {:ack (deref sent 10000 :no-ack-in-time) :answer answer :waited (- (System/currentTimeMillis) began)}))

(deftest an-attempt-of-a-batch-fails-and-runs-again
  (let [;; What the batch stands on, each decided once: a streaming layer,
        ;; which the group behind this one acts in; the base, by a making
        ;; act whose two facts are in the other order; and a marked key.
        made-streaming (decide! (making :alice :system :alice))
        made (decide-in-base! (update (making :base :system :system) :facts (comp vec reverse)))
        marking (decide-in-base! (system-act [{:e :base :k marks :v #{:on}}]))
        since (:stamp (:answer marking))
        ;; The batch: 40 acts of the system, a chain on 5 places under the
        ;; marked key, so that each accept writes a fact and an entry of the
        ;; order by value, and from the sixth on takes an entry out. Each
        ;; carries a time of its own, one millisecond after the one before,
        ;; so that each row tells which act's time it holds.
        n 40
        throw-index 20
        places 5
        last-before (base-last-stamp)
        t0 chosen-time
        place-of (fn [i] (keyword (str "failed-place-" (mod i places))))
        value-of (fn [i] (keyword (str "target-" (mod i 7))))
        acts (reduce (fn [acts i]
                       (conj acts (system-act [(cond-> (value-fact (place-of i) :on (value-of i))
                                                 (<= places i) (assoc :replaces [(:act-id (nth acts (- i places))) 0]))]
                                              {declaration/time-part (+ t0 i)})))
                     []
                     (range n))
        ids (mapv :act-id acts)
        uuids (mapv #(nth % 2) ids)
        {:keys [results answered? waited]} (decide-with-a-failed-attempt! acts throw-index)
        noted @attempt-failure
        _ (wait-until-processed!)
        calls-of (fn [act-id] (filterv #(= act-id (:act-id %)) (:calls noted)))
        calls-by-act (mapv calls-of ids)
        attempts-apart (vec (keep (fn [[act-id calls]]
                                    (when-not (and (= 2 (count calls))
                                                   (= (select-keys (first calls) [:refused :stamp :rows])
                                                      (select-keys (second calls) [:refused :stamp :rows])))
                                      (short-id act-id)))
                                  (map vector (subvec ids 0 (inc throw-index)) calls-by-act)))
        expected-left (mapv left-key (subvec uuids 0 throw-index))
        left-rows (mapv (fn [k] (rows-by-task :mb-facts-ps k)) expected-left)
        records (vec (stored-in-base))
        facts-with-the-left-entity (mapv (fn [rows] (count (filter #(str/includes? % (str left-entity)) (keys rows))))
                                         (table-on-each-task :mb-facts-ps))
        last-of-each-place (take-last places (range n))
        expected-entries (into {} (map (fn [i] [(base-entry-key :on since (value-of i) (place-of i)) [(nth uuids i) 0]])) last-of-each-place)
        entries (base-entries-under :on since)
        decided (decided-on-the-microbatch-depot records)
        later (decide-behind-a-failure! (system-act [{:e :after-the-retry :k :note}]))
        ;; The check of the rows that were handed to the failed attempt must
        ;; be able to fail. One more act is decided with the function armed
        ;; and no throw: it is handed such a row in an attempt that commits.
        handed-once (system-act [(value-fact :handed-once :note "handed one row more, in an attempt that commits")])
        _ (reset! attempt-failure {:armed? true :run #{(:act-id handed-once)} :throw-at nil :calls [] :left [] :throws []})
        handed-result (decide-behind-a-failure! handed-once)
        handed-rows (rows-by-task :mb-facts-ps (left-key (nth (:act-id handed-once) 2)))]
    (swap! attempt-failure assoc :armed? false :run #{})
    (check "on the second cluster, with the proof's two functions in the vars of the store's `writes` and `stray-reason`: a streaming layer is made; the base is made by a making act whose two facts are in the other order; and a key of the base is marked"
           [:accepted :accepted :accepted] [(:word made-streaming) (:word made) (:word marking)])
    (check "the proof's function threw once, at the 21st act of a batch of 40 that was appended while the gate was paused; before the throw it had handed the walk one extra row for each of the 20 acts before it, at a key that no act writes"
           [1 expected-left false]
           [(count (:throws noted)) (:left noted) (:armed? noted)])
    (check "the failed attempt walked the acts in the depot's order up to the throw and no further: `writes` was called twice for each of the first 21 acts, once in each attempt, and once for each of the 19 behind the throw; each call before the throw was armed, and no call behind it"
           [(vec (concat (repeat (inc throw-index) 2) (repeat (- n throw-index 1) 1)))
            (vec (concat (repeat (inc throw-index) [true false]) (repeat (- n throw-index 1) [false])))]
           [(mapv count calls-by-act) (mapv #(mapv :armed? %) calls-by-act)])
    (check "the second attempt made each row as the first made it: for each of the 21 acts that both attempts decided, both calls were given the same stamp and no word of a refusal, and the store's own function gave the same writes, row for row"
           [] attempts-apart)
    (check "after the retry all 40 are accepted, each with the digest of what was sent; each stamp is the number that the rule gives, the base's last stamp before the batch plus the act's place in the batch, 1 to 40; and each answer holds the time on its own act"
           [true {:accepted n} true (mapv #(+ last-before 1 %) (range n)) (mapv #(+ t0 %) (range n))]
           [answered? (frequencies (map :word results)) (every? :mine? results) (mapv (comp :stamp :answer) results)
            (mapv (comp :time :answer) results)])
    (check "no task holds one of the rows that only the failed attempt was handed: a select of each of the 20 keys gives nothing on every task, and no key of the facts on any task names their entity"
           [(vec (repeat throw-index {})) (vec (repeat tasks 0))]
           [left-rows facts-with-the-left-entity])
    (check-the-copies "the copies, after the failed attempt and its retry" records)
    (check "the order by value holds, on every task, the last entry of each of the 5 places and no entry of an earlier act of the batch"
           (vec (repeat tasks expected-entries)) entries)
    (check "each of the 5 places holds, on every task, the fact of the last act that wrote it, with the stamp of that act and the time on it"
           (mapv (fn [i] [(on-every-task (nth uuids i) 0 (+ last-before 1 i) (str (value-of i))) (the-time-on-every-task (+ t0 i))])
                 last-of-each-place)
           (mapv (fn [i] [(facts-on-every-task (place-of i) :on) (times-on-every-task (place-of i) :on)]) last-of-each-place))
    (check "each thing that the microbatch gate decided on this cluster has the stamp that the rule gives, by the proof's own count; and the base's last stamp is the stamp of the last act of the batch"
           [[] (+ last-before n)]
           [(stamps-apart-from-the-rule decided) (:stamp (peek (filterv #(= :base (:layer %)) decided)))])
    (check "the gate goes on behind the retry: the next act of the base is accepted with the next stamp, the last of the batch plus one"
           [:accepted (+ last-before n 1)] [(:word later) (:stamp (:answer later))])
    (check "the check of those rows can fail: one more act, decided with the proof's function armed and no throw, is accepted, and the row that it was handed at such a key is on every task, written by that act. A row that the walk is handed this way is written and copied, so the 20 rows went with the attempt that failed"
           [:accepted tasks #{(nth (:act-id handed-once) 2)}]
           [(:word handed-result) (count handed-rows) (set (map :by-act (vals handed-rows)))])
    (shown "an attempt of a batch of the store's own microbatch gate failed and ran again: of" n "acts appended while the gate was paused, the proof's function in the var of `writes` threw at the"
           (inc throw-index) "st, on the thread" (pr-str (:thread (first (:throws noted)))) "| rows handed to the failed attempt only:" (count (:left noted))
           "| calls of writes for each act:" (pr-str (frequencies (map count calls-by-act)))
           "| all acts answered:" answered? "," waited "ms after the topology went on")
    (shown "a row handed to an attempt that commits, with no throw: on" (count handed-rows) "tasks | the act:" (told handed-result))
    (shown "the two attempts: acts whose two decisions were not the same in stamp, word and rows:" (count attempts-apart)
           "| after the retry:" (pr-str (frequencies (map :word results))) "| stamps from" (first (map (comp :stamp :answer) results))
           "to" (peek (mapv (comp :stamp :answer) results)) "| rows of the failed attempt left on each task:" (pr-str facts-with-the-left-entity)
           "| entries of the order by value on each task:" (pr-str (mapv count entries)))))

(deftest an-event-fails-and-runs-again
  (let [place (fn [i] (keyword (str "replayed-" i)))
        before-things (mapv (fn [i] (alice [(value-fact (place i) :text (str "sent before the throw, " i))])) (range 5))
        _ (swap! event-failures assoc :to-fail nil :watched (set (map :act-id before-things)) :calls [])
        befores (mapv decide! before-things)
        runs-at-first (mapv #(count (calls-for :run (:act-id %))) before-things)
        rows-before (mapv #(fact-row :alice (place %) :text) (range 5))
        last-before (last-stamp :alice)
        ;; The act that is to fail. Its stamp must be the layer's last
        ;; stamp plus one, exactly. Its fact names the fact of the first of
        ;; the five as the one that it replaces, so a run decides it against
        ;; the row at that place.
        x (alice [(assoc (value-fact (place 0) :text "decided by the second run")
                         :replaces [(:act-id (first before-things)) 0])])
        act-uuid (nth (:act-id x) 2)
        _ (swap! event-failures (fn [state] (-> state (update :watched conj (:act-id x)) (assoc :to-fail (:act-id x)))))
        {:keys [ack answer waited]} (send-and-await! x)
        [first-call second-call :as decisions] (calls-for :decision (:act-id x))
        the-row (fact-row :alice (place 0) :text)
        last-after (last-stamp :alice)
        as-the-tables-hold (mapv (fn [[which k noted]]
                                   (= noted (case which
                                              :fact (content (row :facts-ps :alice k))
                                              :answer (content (row :answers-ps :alice k))
                                              :last-stamp last-after
                                              :no-such-kind)))
                                 (:rows second-call))
        runs-of-the-befores (mapv #(count (calls-for :run (:act-id %))) before-things)
        decisions-of-the-befores (mapv #(count (calls-for :decision (:act-id %))) before-things)
        answers-of-the-befores (mapv #(answer-of (:act-id %)) before-things)
        rows-after (mapv #(fact-row :alice (place %) :text) (range 5))
        runs-then (count (calls-for :run (:act-id x)))
        repeat-ack (send! x)
        after-the-repeat [(answer-of (:act-id x)) (last-stamp :alice) (count (calls-for :decision (:act-id x)))
                          (count (calls-for :run (:act-id x)))]
        _ (swap! event-failures assoc :to-fail nil :watched #{})]
    (check "five acts are sent to the streaming gate right before the throw, with their events watched: each is accepted, the event of each ran once, and each was decided once"
           [(vec (repeat 5 :accepted)) [1 1 1 1 1] [1 1 1 1 1]]
           [(mapv :word befores) runs-at-first decisions-of-the-befores])
    (check "the stand-in of `writes` was called twice for the act: the first call threw, behind the event's reads and before its first write, and Rama ran the record again. The second run reached `writes` too, so it found no answer under the act-id: the run that threw left none"
           2 (count decisions))
    (check "the two runs made one decision. Each accepted the act, whose fact names the fact that it replaces: the second run found that place as the first run had found it, so the run that threw left no row there. Each was given the same stamp, the layer's last stamp from before plus one: the second run read the layer's last stamp as it was before the first, and no clock was read between the two"
           [[nil nil] [(inc last-before) (inc last-before)]]
           [(mapv :refused decisions) (mapv :stamp decisions)])
    (check "and each run made the same three writes, row for row: a fact, an answer and the layer's last stamp"
           [true {:fact 1 :answer 1 :last-stamp 1}]
           [(boolean (and first-call second-call (= (:rows first-call) (:rows second-call))))
            (frequencies (map first (:rows second-call)))])
    (check "the second run is the one that counts: its three writes are what the tables hold, row for row; the read by act-id gives the act as accepted, with the digest of what was sent, the stamp of both runs and the time on the act; the place holds the text of its value, written by it; and the layer's last stamp is that stamp"
           [[true true true] :accepted true (inc last-before) (time-on x) ["\"decided by the second run\"" act-uuid] (inc last-before)]
           [as-the-tables-hold (:verdict answer) (digest/is-digest-of? (:digest answer) x) (:stamp answer) (:time answer)
            [(:value the-row) (:by-act the-row)] last-after])
    (check "Rama ran the records before it again too: the event of each of the five acts began at least a second time, after its rows were committed and its append acked. None was decided a second time: a run that finds an answer under the act-id decides nothing. The answer of each is as it was, and so is the row of each of the four whose place the failed act did not replace"
           [true [1 1 1 1 1] (mapv :answer befores) (subvec rows-before 1)]
           [(every? #(<= 2 %) runs-of-the-befores) decisions-of-the-befores answers-of-the-befores (subvec rows-after 1)])
    (check "the act is decided once: appended a second time by hand, it is acked and reads the same answer; its event ran once more, `writes` has still two calls for it, and the layer's last stamp is the stamp of its answer"
           [:acked [answer (inc last-before) 2 (inc runs-then)]]
           [repeat-ack after-the-repeat])
    (shown "an event of the streaming gate failed before its first write and Rama ran its record again: calls of writes for the act" (count decisions)
           "| the first threw on the thread" (pr-str (:thread first-call)) "| the append gave" (pr-str ack)
           "| the stamps that the two runs were given:" (pr-str (mapv :stamp decisions)) "| the layer's last stamp before:" last-before
           "| the second run decided" (when (and first-call second-call) (- (:wall second-call) (:wall first-call))) "ms after the throw"
           "| the writes of each run, by kind:" (pr-str (mapv #(frequencies (map first (:rows %))) decisions))
           "| the answer was read" waited "ms after the append")
    (shown "the records before the throw: runs of the event of each of the five acts" (pr-str runs-of-the-befores)
           "| decisions of each" (pr-str decisions-of-the-befores) "| runs of the failed act's event before the hand's repeat:" runs-then
           "| after the repeat: runs" (peek after-the-repeat) "| decisions" (nth after-the-repeat 2) "| the layer's last stamp" (nth after-the-repeat 1))))

(defn- the-checks-on-the-store-as-defined
  "The checks of the proof's first cluster, in their order: each runs on the
  functions as the store defines them."
  []
  (what-rama-carries)
  (layers-are-made)
  (an-act-lands)
  (refusals-and-repeats)
  (settings-and-permissions)
  (a-permission-deeper-than-four)
  (the-time-on-the-act)
  (strays-and-limits)
  (the-reads-take-anything)
  (what-a-fact-keeps)
  (the-keys-that-take-a-second-write)
  (what-a-permission-allows)
  (what-points-at-a-thing)
  (the-base-is-made)
  (an-act-lands-in-the-base)
  (an-answer-is-readable-when-its-batch-is-committed)
  (refusals-and-repeats-in-the-base)
  (one-line-one-order)
  (a-chain-in-one-batch)
  (a-person-acts-in-the-base)
  (a-permission-deeper-than-four-in-the-base)
  (the-bounds-in-the-base)
  (a-long-batch)
  (the-copies-of-the-base)
  (the-time-on-the-act-in-the-base)
  (an-act-of-several-facts-in-the-base)
  (each-gate-tells-the-other-gates-layers)
  (what-a-fact-keeps-in-the-base)
  (the-keys-that-take-a-second-write-in-the-base)
  (what-a-permission-allows-in-the-base)
  (what-points-at-a-thing-in-the-base)
  (one-batch-under-a-marked-key)
  (the-module-keeps-no-lock)
  (a-place-of-the-base-never-gives-nothing)
  (after-the-module-is-started-again)
  (the-base-after-the-module-is-started-again)
  (the-stamps-and-the-depot)
  (the-stamps-and-the-depot-of-the-base))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them. Later checks
  build on what earlier ones made. On the proof's first cluster: the checks
  of the streaming gate come first; the base is made after them. On the
  second cluster: the two groups that make a gate fail, the microbatch
  gate's first and then the streaming gate's. The hand says which cluster
  it is."
  []
  (if (= :a-gate-fails (:checks *hand*))
    (do (an-attempt-of-a-batch-fails-and-runs-again)
        (an-event-fails-and-runs-again))
    (the-checks-on-the-store-as-defined)))

(defn- run-on-a-cluster
  "Creates an in-process cluster, launches the module `Store` in it with
  four tasks, two threads and two workers, runs the checks that `checks`
  names with the hand on that cluster, and closes the cluster. Gives what
  `run-tests` gives."
  [checks]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (binding [*hand* (assoc (open-hand ipc) :checks checks)]
      (run-tests 'softland.store.module-proof))))

(defn -main
  "Runs the proof on two clusters, one after the other, and exits: status 0
  when each check of both runs holds. It takes no argument.

  The first cluster runs every check but the two groups that make a gate
  fail, on the functions as the store defines them. When it is closed,
  `-main` empties the hand's atoms, all but the shown lines; puts the two
  stand-ins into the vars of the store's functions; starts the second
  cluster, which runs the two groups that make a gate fail; and puts the
  store's functions back when that cluster is closed, in a `finally`. The
  count line is one: the counts of the two runs, added."
  [& _]
  (let [counts #(select-keys % [:test :pass :fail :error])
        first-run (run-on-a-cluster :the-store-as-defined)
        _ (empty-the-hand!)
        second-run (try
                     (put-noting-functions!)
                     (run-on-a-cluster :a-gate-fails)
                     (finally (put-the-stores-functions-back!)))
        result (merge-with + (counts first-run) (counts second-run))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println result)
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
