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
  makes the act-id, with a random UUID; appends the thing, each value plain
  as a maker gives it; reads the answer by act-id; and compares the answer's
  digest with the digest of what it sent. The hand also plays a reader of
  the tables, since
  no reader is built in this part: it selects a fact's row, the row of a
  layer's marked keys, and a run of the facts by value, with the keys and
  the partitioning keys that `softland.store.record.rows` gives, and reads a
  row back with that namespace's functions. An error from the append is not an answer. It
  keeps each thing that it sent, so that a repeat is the same thing byte for
  byte.

  The hand also plays a rebuild's reader of the two logs of answers, since
  no rebuild is built in this part: with Rama's client it reads each
  partition of `*streaming-answer-depot` and of `*microbatch-answer-depot`
  from its start, and holds each entry against what the read by act-id
  gives and against the tables. No topology of the module reads those two
  depots, and neither the door nor the read path.

  The two depots. For a streaming layer the hand appends with the ack level
  :ack and reads the answer once: the ack waits for the gate. For the base,
  the one microbatch layer, no ack carries an answer: the hand appends with
  :append-ack and reads the answer by act-id in a loop with a time limit, as
  a door would. To put several things into one batch it pauses the
  microbatch topology with Rama's test tool, appends, and lets it go on.

  State. The proof starts two in-process clusters, one after the other in
  one JVM, each with four tasks and two workers, launches the module `Store`
  in each, and closes each; a cluster's files are temporary and are Rama's.
  Every check but four groups runs on the first cluster, on the functions
  as the store defines them: nothing is replaced in the JVM while it runs.
  The four groups that make a gate fail run on the second cluster: a batch of the
  microbatch gate that runs a second time, a run of that gate that closes a
  batch and fails behind its appends, and an event of the streaming gate
  that fails before its commit and one that fails behind it. For that
  cluster alone `-main` puts seven functions of this namespace into the
  vars of seven pure functions of the store that the gates' dataflow calls,
  before the launch, and puts the store's own back when the cluster is
  closed, in a `finally`: `base`, `decide-or-send`, `rows-to-send`,
  `records-of-closing` and `marks-places-asked` of
  `softland.store.gate.batch`, `writes` of `softland.store.gate.decision`,
  and `answer-record` of `softland.store.record.rows`. Each of the seven
  gives exactly what the store's function gives; four of them throw, once
  each, when a group asked for it. The module's source holds no hook for
  any of this. Rama takes the function that a var holds at each launch.

  The proof owns eleven atoms: the lines that it showed; the things that it
  sent to each depot; the first answers of each gate, in the order sent;
  what later checks come back to; and, for the second cluster, the note of
  each attempt of a batch, the wish for a throw, each throw that was made,
  what the proof asked of a closing and what came of it, and the act-ids
  whose event is to fail with each call that was made for them. `-main`
  empties all but the shown lines between the two clusters:
  Rama counts the things that a gate processed from the launch of a
  cluster. The notes are the memory of this process. They hold a first
  attempt's rows through the restart of a worker only because the in-process
  cluster is one JVM. The proof reads the clock to make a carried stamp and
  to time a wait.

  Approach. The checks run in one order on one cluster, and later checks
  build on the layers, the facts and the permissions that earlier ones
  made. The checks of the streaming gate run first. The base is made after
  the check that sends its making act to the streaming gate, because that
  check holds the word of a layer that is not made. Each check says what
  must be read back, and reads it back through the module's own queries.
  A check that reads a fact back holds the row's text against a text that
  the proof writes out, and the log's value against the value that the hand
  sent: never against a text that the store's own function makes from the
  same value.
  Where it shows a row, it selects the row from a gate's PState: for a
  streaming layer with the layer as the partitioning key, for the base with
  the row's own route, and for \"on which tasks is this row\" once on each
  task. Such a select goes round the two reads, so it sees a row of a
  batch that is not closed; one check uses that to catch an answer between
  its batch and the closing. Near the end the proof starts the module again
  in the cluster, by Rama's module update with the unchanged module, and
  reads both records once more; Rama then prints some lines of its own log,
  marked ERROR, that are no failed checks. So it does on the second cluster
  for the one throw that the proof makes: Rama says that it treats the throw
  as fatal for the worker, and starts the worker again.

  What it does not reach is in the folder's README."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.batch :as batch]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.module.microbatch-gate :as microbatch-gate]
            [softland.store.record.rows :as record]
            [softland.store.gate.stamp :as stamp])
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
  "Checks that what was observed is what must be. Gives what was observed."
  [what expected observed]
  (is (= expected observed) what)
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
  of the module, the two depots of acts, the two depots of answers, the two
  queries, the four PStates of each gate, the depot of batch clocks and the
  PState of the kept clock;
  and `:checks`, which says which of the proof's two clusters it is.
  `-main` binds it for the run on each cluster."
  nil)

(defonce ^{:private true
           :doc "Each thing that the hand appended to the streaming depot, in the
  order sent: what the depot's log is held against."}
  things-sent (atom []))

(defonce ^{:private true
           :doc "The first answer that the hand read for each act-id of a streaming
  layer, in the order sent, with its layer and its stamp: what the check of
  the stamps goes through."}
  first-answers (atom []))

(defonce ^{:private true
           :doc "What one group of checks hands to a later one, by name: rows and
  answers as they were before the module is started again, and the pairs
  that the reads of the base are held to."}
  kept (atom {}))

(defonce ^{:private true
           :doc "Each thing that the hand appended to the microbatch depot, in the
  order sent: its count is what the wait for a batch waits for, and the
  depot's log is held against it."}
  base-things-sent (atom []))

(defonce ^{:private true
           :doc "The answers that the hand read in the base, in groups, each group
  the acts whose answers were read together."}
  base-answers (atom []))

(defn- open-hand
  "Takes the cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc        ipc
     :streaming-depot (rama/foreign-depot ipc m "*streaming-depot")
     :streaming-answer-depot (rama/foreign-depot ipc m "*streaming-answer-depot")
     :answer     (rama/foreign-query ipc m "answer-by-act-id")
     :depot-of-layer (rama/foreign-query ipc m "depot-of-layer")
     :answers-ps (rama/foreign-pstate ipc m "$$streaming-answers")
     :facts-ps   (rama/foreign-pstate ipc m "$$streaming-facts")
     :by-value-ps (rama/foreign-pstate ipc m "$$streaming-facts-by-value")
     :task-ps    (rama/foreign-pstate ipc m "$$streaming-task")
     :module     m
     :mb-depot      (rama/foreign-depot ipc m "*microbatch-depot")
     :mb-answer-depot (rama/foreign-depot ipc m "*microbatch-answer-depot")
     :mb-answers-ps (rama/foreign-pstate ipc m "$$microbatch-answers")
     :mb-facts-ps   (rama/foreign-pstate ipc m "$$microbatch-facts")
     :mb-by-value-ps (rama/foreign-pstate ipc m "$$microbatch-facts-by-value")
     :mb-task-ps    (rama/foreign-pstate ipc m "$$microbatch-task")
     :clock-depot   (rama/foreign-depot ipc m "*batch-clock-depot")
     :clock-ps      (rama/foreign-pstate ipc m "$$batch-clock")}))

(defn- depot-of-layer
  "Reads the depot of a layer, or nil for a layer that is not made. The hand
  tests what it passes before it calls, and gives nil itself for a thing
  that is no id."
  [layer]
  (when (record/depot-read layer)
    (rama/foreign-invoke-query (:depot-of-layer *hand*) layer)))

(defn- answer-of
  "Reads the answer under an act-id, or nil for nothing yet. The hand passes
  the act-id's routing id as the first argument, the layer of a streaming
  act-id and the uuid of a microbatch one, and tests the act-id before it
  calls."
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
  "An act as the door sends it: the maker's parts, and the door's act-id and
  depot. `more` is merged in last, for the optional parts and for faults."
  ([layer who permission facts] (thing layer who permission facts {}))
  ([layer who permission facts more]
   (let [act-id (new-act-id layer)]
     (merge {:act-id act-id :depot (nth act-id 1)
             :version 1 :who who :layer layer :permission permission :session :proof-session
             :facts (vec facts)}
            more))))

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
        mine? (digest/is-digest-of? (:digest answer) x)]
    (when (and answer mine? (not-any? #(= (:act-id x) (:act-id %)) @first-answers))
      (swap! first-answers conj {:act-id (:act-id x) :layer (:layer x) :stamp (:stamp answer)}))
    {:thing x :ack ack :answer answer :mine? mine?
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

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
  "The last stamp that the gate gave on the task of a layer."
  [layer]
  (rama/foreign-select-one (path/keypath :last-stamp) (:task-ps *hand*) {:pkey layer}))

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

;; ------------------------------------------------------ the hand for the base

(def ^:private time-limit-ms
  "How long the hand reads for the answer of a microbatch act before it gives
  up, in milliseconds."
  30000)

(defn- append-to-base!
  "Appends a thing to the microbatch depot with the ack level :append-ack,
  and keeps it. Gives :appended, or what the append threw. The ack says that
  the depot holds the thing, and nothing about the decision."
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

(defn- await-answer
  "Reads the answer under a microbatch act-id in a loop, as a door would,
  until one is there or the time limit has passed. Gives the answer, or
  nil."
  [act-id]
  (let [deadline (+ (System/currentTimeMillis) time-limit-ms)]
    (loop []
      (let [answer (answer-of act-id)]
        (cond answer answer
              (< deadline (System/currentTimeMillis)) nil
              :else (do (Thread/sleep 20) (recur)))))))

(defn- note-base-answers!
  "Keeps the first answers of one batch as one group, for the check of the
  stamps: of each result whose answer is of its own thing, and whose act-id
  the proof has not seen answered before."
  [results]
  (let [seen (into #{} (comp cat (map :act-id)) @base-answers)
        group (into []
                    (comp (filter (fn [r] (and (:answer r) (:mine? r))))
                          (map (fn [r] {:act-id (:act-id (:thing r)) :stamp (:stamp (:answer r))}))
                          (remove (fn [a] (contains? seen (:act-id a))))
                          (distinct))
                    results)]
    (when (seq group) (swap! base-answers conj group))))

(defn- closed-through
  "The `:closed-through` of the microbatch gate on the task of a route."
  [route]
  (rama/foreign-select-one (path/keypath :closed-through) (:mb-task-ps *hand*) {:pkey route}))

(defn- base-result
  "A result as `decide!` gives one, for a thing that was sent to the
  microbatch depot and processed: the thing, the ack, the answer read in a
  loop, whether the answer's digest is the digest of what was sent, and one
  word. A stray has no answer to wait for, so its act-id is read once.

  When the read gave an answer under a microbatch act-id, the hand selects
  the `:closed-through` of the answer's task right after the read and keeps
  the pair in `kept`, under :base-reads. `:closed-through` never goes back,
  so an answer whose stamp is above it then was read from a batch that was
  not closed."
  [x ack]
  (let [act-id (when (map? x) (get x :act-id))
        stray? (declaration/stray-reason :microbatch x)
        answer (if stray?
                 (answer-of act-id)
                 (await-answer act-id))]
    (when (and answer (not stray?))
      (swap! kept update :base-reads (fnil conj [])
             {:act-id act-id :stamp (:stamp answer) :closed-through-after (closed-through (nth act-id 2))}))
    {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

(defn- decide-in-base!
  "Sends one thing to the microbatch depot, alone, and reads its answer by
  act-id until one is there. Gives the result, as `decide!` does."
  [x]
  (let [ack (append-to-base! x)
        _ (wait-until-processed!)
        result (base-result x ack)]
    (note-base-answers! [result])
    result))

(defn- decide-in-one-batch!
  "Sends things to the microbatch depot so that one batch of the gate takes
  them all: it pauses the microbatch topology, appends, and lets it go on.
  Gives the result of each, in the order given."
  [xs]
  (rtest/pause-microbatch-topology! (:ipc *hand*) (:module *hand*) "microbatch-gate")
  (let [acks (mapv append-to-base! xs)]
    (rtest/resume-microbatch-topology! (:ipc *hand*) (:module *hand*) "microbatch-gate")
    (wait-until-processed!)
    (let [results (mapv base-result xs acks)]
      (note-base-answers! results)
      results)))

(defn- base-row
  "Selects one row of a PState of the microbatch gate, by its key, on the
  task of a route: the entity of a fact, the uuid of an answer. The select
  goes round the reads, so it also sees a row of a batch that is not
  closed."
  [pstate route k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey route}))

(defn- base-fact
  "The row of a place of the base, on the task of its entity, or nil."
  [e k]
  (base-row :mb-facts-ps e (record/place-key :base e k)))

(defn- rows-by-task
  "Selects the row under a key on each task of the cluster. Gives a sorted
  map from the number of each task that holds a row to that row."
  [pstate k]
  (into (sorted-map)
        (keep-indexed (fn [task pkey]
                        (when-let [row (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey pkey})]
                          [task row])))
        (rtest/gen-hashing-index-keys tasks)))

(defn- task-of-row
  "The number of the task that holds the row under a key, or nil. For a row
  that is on one task."
  [pstate k]
  (first (keys (rows-by-task pstate k))))

(defn- base-marks-rows
  "The row at the base's marked-keys place, on each task that holds one."
  []
  (rows-by-task :mb-facts-ps (record/place-key :base :base :softland.store/marked-keys)))

(defn- base-marked-keys
  "The keys that the base marks, each with its stamp, as the task of the
  layer id holds them."
  []
  (record/marked-keys-of (base-row :mb-facts-ps :base (record/place-key :base :base :softland.store/marked-keys))))

(defn- base-entries-of
  "The rows of the run of one value under a marked key of the base, since a
  stamp of that key: [key row] of each. One select of a range of the facts
  by value, with the value's digest as the partitioning key: the run of one
  value is on one task."
  [k since v]
  (let [[from to] (record/by-value-run :base k since v)]
    (rama/foreign-select [(path/sorted-map-range from to) path/ALL] (:mb-by-value-ps *hand*) {:pkey (record/value-digest-of v)})))

(defn- base-run-of
  "The entities of the places in the run of one value of the base."
  [k since v]
  (mapv (fn [[k _]] (record/by-value-entity k)) (base-entries-of k since v)))

(defn- base-entry-key
  "The key in the facts by value of a place of the base, for a value."
  [k since v e]
  (record/by-value-key :base k since (record/value-digest-of v) e))

(defn- gates-last-stamp
  "The last stamp that the microbatch gate gave: one for the gate, on the
  gate's task, the task of the depot word."
  []
  (rama/foreign-select-one (path/keypath :last-stamp) (:mb-task-ps *hand*) {:pkey :microbatch}))

(defn- base-strays
  "The number of different strays of the microbatch gate under each word,
  over every task."
  []
  (into {}
        (map (fn [word]
               [word (reduce + (map (fn [k] (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                                                     (:mb-task-ps *hand*) {:pkey k}))
                                    (rtest/gen-hashing-index-keys tasks)))]))
        declaration/stray-reasons))

(defn- stored-in-base
  "Each record of the microbatch depot's log, read back: [partition record]."
  []
  (let [depot (:mb-depot *hand*)]
    (for [p (range (:num-partitions (rama/foreign-object-info depot)))
          :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
          r (rama/foreign-depot-read depot p start-offset end-offset)]
      [p r])))

(defn- kept-clock-row
  "The row that the module keeps for the newest batch of the microbatch gate
  that had things to decide, {:batch :clock}, or nil when no batch kept one:
  one select of `$$batch-clock` on the gate's task, the task of the depot
  word."
  []
  (rama/foreign-select-one (path/keypath :microbatch) (:clock-ps *hand*) {:pkey :microbatch}))

(defn- clock-records
  "Each record of the log of the depot of batch clocks, read back:
  [partition record]. A partition that holds nothing is not read."
  []
  (let [depot (:clock-depot *hand*)]
    (vec (for [p (range (:num-partitions (rama/foreign-object-info depot)))
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
               :when (< start-offset end-offset)
               r (rama/foreign-depot-read depot p start-offset end-offset)]
           [p r]))))

(defn- clock-record?
  "True for a record of the depot of batch clocks as the gate must append
  it: a map of exactly `:batch` and `:clock`, two integers."
  [r]
  (boolean (and (map? r) (= #{:batch :clock} (set (keys r))) (integer? (:batch r)) (integer? (:clock r)))))

;; ------------------------------------------- the hand for the logs of answers

(def ^:private quiet-ms
  "How long the proof waits to see that a quiet time adds no record to a
  depot, in milliseconds."
  1500)

(defn- hex-by-hand
  "The hex text of a run of bytes, two lower-case digits for each byte, made
  with Java's formatter and no function of the store."
  [^bytes b]
  (apply str (map (fn [x] (format "%02x" (bit-and 0xff (long x)))) b)))

(defn- answer-log
  "Each record of a gate's depot of answers, read back with Rama's client as
  a rebuild is given it: [partition record], in the order of the partitions
  and, within one, of the offsets. `gate` is :streaming or :microbatch. It
  asks for the number of partitions and for the two offsets of each, and
  reads each partition that holds something, whole."
  [gate]
  (let [depot (get *hand* (if (= :streaming gate) :streaming-answer-depot :mb-answer-depot))]
    (vec (for [p (range (:num-partitions (rama/foreign-object-info depot)))
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
               :when (< start-offset end-offset)
               r (rama/foreign-depot-read depot p start-offset end-offset)]
           [p r]))))

(defn- logged-answers
  "Takes the records of a depot of answers, each [partition record]. Gives
  each answer's entry among them, in the order read:
  {:partition :batch :entry}, with the partition and the batch's id of the
  record that holds it. The entry is as the log holds it but for its digest,
  which is given as hex text made by the proof's own hand, so that it can be
  held against the digest that the door's read gives."
  [records]
  (vec (for [[p r] records
             entry (:answers r)]
         {:partition p :batch (:batch r) :entry (update entry :digest hex-by-hand)})))

(defn- logged-strays
  "Takes the records of a depot of answers. Gives each stray's entry among
  them, in the order read: {:partition :batch :entry}."
  [records]
  (vec (for [[p r] records
             entry (:strays r)]
         {:partition p :batch (:batch r) :entry entry})))

(defn- entries-under
  "Takes answers as `logged-answers` gives them, and an act-id. Gives the
  entries under that act-id, in the order read."
  [logged act-id]
  (into [] (comp (map :entry) (filter #(= act-id (:act-id %)))) logged))

(defn- entry-of-answer
  "The entry that a log of answers must hold for an answer, made by the
  proof's own hand from the act-id and from what the read by act-id gave:
  {:act-id :stamp :digest}, the digest as hex, and `:refused` with the word
  for a refusal. It has no other part, so an entry that is equal to it holds
  no value of a fact."
  [act-id answer]
  (cond-> {:act-id act-id :stamp (:stamp answer) :digest (:digest answer)}
    (= :refused (:verdict answer)) (assoc :refused (:reason answer))))

(defn- entries-apart
  "Takes answers as `logged-answers` gives them, and pairs of an act-id and
  the answer that the read by act-id gave for it. Gives the short id of each
  act-id that has no entry, or an entry that is not `entry-of-answer` of its
  answer: an empty vector when each answer has its entry and every entry
  under one act-id is the same."
  [logged answered]
  (let [by-act-id (group-by (comp :act-id :entry) logged)]
    (vec (keep (fn [[act-id answer]]
                 (let [entries (map :entry (get by-act-id act-id))]
                   (when-not (and (seq entries) (every? #(= (entry-of-answer act-id answer) %) entries))
                     (short-id act-id))))
               answered))))

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

(defn- await-log
  "Reads a gate's depot of answers in a loop until `enough?` holds of its
  records or a time limit has passed, the hand's own when none is given.
  Gives the records of the last read that came back. A read that throws
  counts as no read: for about half a second after a worker is started
  again a read can throw. For what a later run of a gate appends, which no
  read by act-id waits for: a stray's entry in the base, and the copies
  that a run which runs again leaves."
  ([gate enough?] (await-log gate enough? time-limit-ms))
  ([gate enough? limit-ms]
   (let [deadline (+ (System/currentTimeMillis) limit-ms)]
     (loop [last-read []]
       (let [records (try (answer-log gate) (catch Throwable _ nil))]
         (if (or (and records (enough? records)) (< deadline (System/currentTimeMillis)))
           (or records last-read)
           (do (Thread/sleep 20) (recur (or records last-read)))))))))

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
  (first (keep-indexed (fn [task pkey]
                         (when (some #{digest} (rama/foreign-select [(path/keypath :strays word) path/ALL] (get *hand* pstate) {:pkey pkey}))
                           task))
                       (rtest/gen-hashing-index-keys tasks))))

(defn- to-keep-by-task
  "How many keys the microbatch gate's `:to-keep` holds on each task: what a
  batch that is committed and not closed has left to keep."
  []
  (mapv (fn [pkey] (count (rama/foreign-select [(path/keypath :to-keep) path/MAP-KEYS] (:mb-task-ps *hand*) {:pkey pkey})))
        (rtest/gen-hashing-index-keys tasks)))

(defn- to-close-by-task
  "The microbatch gate's `:to-close` on each task: the batch that the next
  run of the gate closes, on the gate's task, and nil elsewhere."
  []
  (mapv (fn [pkey] (rama/foreign-select-one (path/keypath :to-close) (:mb-task-ps *hand*) {:pkey pkey}))
        (rtest/gen-hashing-index-keys tasks)))

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
        early-entries (entries-under (logged-answers (answer-log :streaming)) (:act-id (:thing value-early)))
        by-stranger (decide! (assoc (making :alice :system :alice) :who :mallory))
        base-here (decide! (thing :base :system [:system :base :base]
                                  [{:e :base :k :softland.store/depot :v :microbatch}
                                   {:e :system :k :softland.store/permission :v [:system :base :base]}]
                                  {:act-id [:base :streaming (UUID/randomUUID)] :depot :streaming}))
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
    (check "that refusal has its entry in the streaming gate's log of answers, read back from the depot: the act-id, the stamp of the answer, the digest of what was sent and the word, and no other part"
           [{:act-id (:act-id (:thing value-early)) :stamp (:stamp (:answer value-early)) :digest (digest/hex-of (:thing value-early))
             :refused :layer-not-made}]
           early-entries)
    (check "a making act by a who that is neither the holder nor the system is refused" :layer-not-made (:word by-stranger))
    (check "a making act of the base at the streaming gate is refused" :layer-not-made (:word base-here))
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
           "| the row at its place:" (pr-str row-early) "| its entry in the log of answers:" (pr-str early-entries))
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
        landed-entries (entries-under (logged-answers (answer-log :streaming)) (:act-id (:thing landed)))
        listed (decide! (alice [] {:facts (list (value-fact :note-2 :text {:n 1}) (value-fact :note-3 :text #{:a}))}))
        list-uuid (nth (:act-id (:thing listed)) 2)
        logged-list (record-of-act (stored) (:act-id (:thing listed)))
        never-sent (answer-of (new-act-id :alice))]
    (swap! kept assoc :landed landed)
    (check "one act with a plain value lands through the streaming gate and is read back by act-id: accepted, with the digest of what was sent"
           [:acked :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the value as a String, the one text of the value in its normal form, and gives the UTF-8 of that text back; the fact that wrote it, who wrote it and the permission that the act named, and the stamp of the answer"
           ["\"the first text\"" (utf8-of "\"the first text\"") act-uuid 0 ":alice" "[:alice :alice :alice]" (:stamp (:answer landed))]
           [(:value the-row) (value-bytes the-row) (:by-act the-row) (:by-index the-row) (:who the-row) (:permission the-row) (:stamp the-row)])
    (check "the row holds those six fields and no other"
           #{:value :by-act :by-index :who :permission :stamp} (set (keys the-row)))
    (check "the depot's log holds the act's fact as the hand sent it, the value plain, with no part added"
           [{:e :note-1 :k :text :v "the first text"}] (vec (:facts logged)))
    (check "the accepted act has one entry in the streaming gate's log of answers: its act-id, the stamp of its answer and the digest of what was sent; it holds no word of a refusal and no value of the act's fact"
           [{:act-id (:act-id (:thing landed)) :stamp (:stamp (:answer landed)) :digest (digest/hex-of (:thing landed))}]
           landed-entries)
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
           "| facts sent as a list are in the log as a" (.getSimpleName (class (:facts logged-list))))
    (shown "the accepted act's entry in the streaming gate's log of answers:" (pr-str landed-entries))))

(deftest refusals-and-repeats
  (let [accepted-thing (:thing (:landed @kept))
        first-fact-id [(:act-id accepted-thing) 0]
        refused-one (decide! (alice [(assoc (value-fact :note-9 :text "a value that names a fact which is not there")
                                            :replaces [(new-act-id :alice) 0])]))
        answer-before (answer-of (:act-id accepted-thing))
        stamp-before (last-stamp :alice)
        log-before (logged-answers (answer-log :streaming))
        repeat-ack (send! accepted-thing)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (do (send! (:thing refused-one)) (answer-of (:act-id (:thing refused-one))))
        stamp-after (last-stamp :alice)
        log-after-repeats (logged-answers (answer-log :streaming))
        other-under-used (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id accepted-thing)))
        log-after-other (logged-answers (answer-log :streaming))
        accepted-entry {:act-id (:act-id accepted-thing) :stamp (:stamp answer-before) :digest (digest/hex-of accepted-thing)}
        refused-entry {:act-id (:act-id (:thing refused-one)) :stamp (:stamp (:answer refused-one))
                       :digest (digest/hex-of (:thing refused-one)) :refused :replaced-fact-not-current}
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
    (check "not decided twice: the task's last stamp is the same after two repeats" stamp-before stamp-after)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false]
           [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (fact-row :alice :note-8 :text))
    (check "a repeat leaves a copy in the streaming gate's log of answers: one entry under the act-id before, the answer's, and after the same act is appended a second time one entry more, the same"
           [[accepted-entry] [accepted-entry accepted-entry]]
           [(entries-under log-before (:act-id accepted-thing)) (entries-under log-after-repeats (:act-id accepted-thing))])
    (check "a refused act has its entry, with its word, and a repeat of it leaves a copy of that entry"
           [[refused-entry] [refused-entry refused-entry]]
           [(entries-under log-before (:act-id (:thing refused-one))) (entries-under log-after-repeats (:act-id (:thing refused-one)))])
    (check "another thing under the used act-id leaves a copy too, of the answer that the act-id has: a third entry, the same, with the digest of the first thing and not of the second"
           [[accepted-entry accepted-entry accepted-entry] false]
           [(entries-under log-after-other (:act-id accepted-thing))
            (= (digest/hex-of (:thing other-under-used)) (:digest accepted-entry))])
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
           "after" (:stamp answer-after) "| the task's last stamp before" stamp-before "after" stamp-after)
    (shown "a second, different act under a used act-id told so: the digest of the answer" (subs (:digest (:answer other-under-used)) 0 16)
           "is not the digest of what was sent" (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "a repeat leaves a copy in the streaming gate's log of answers: entries under the accepted act-id before" (count (entries-under log-before (:act-id accepted-thing)))
           "| after the same act a second time" (count (entries-under log-after-repeats (:act-id accepted-thing)))
           "| after another thing under it" (count (entries-under log-after-other (:act-id accepted-thing)))
           "| the refused act's entry, before and after its repeat:" (count (entries-under log-before (:act-id (:thing refused-one))))
           (count (entries-under log-after-repeats (:act-id (:thing refused-one)))) (pr-str refused-entry))
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
    (check "a layer for these checks is made, and its first permission has depth 1" [:accepted [:alice :deep :deep]] [(:word made) (at 1)])
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

(deftest strays-and-limits
  (let [counts (fn [] (into {} (map (fn [w] [w (strays w)])) declaration/stray-reasons))
        before (counts)
        long (keyword (apply str (repeat 300 "a")))
        no-act-id 5
        malformed {:act-id [:alice :streaming] :layer :alice :marker (UUID/randomUUID)}
        other-gate (assoc (alice [{:e :note-30 :k :text}]) :act-id [:alice :microbatch (UUID/randomUUID)] :depot :microbatch)
        other-layer (assoc (alice [{:e :note-30 :k :text}]) :layer :team)
        too-long {:act-id [long :streaming (UUID/randomUUID)] :layer long}
        acks (mapv send! [no-act-id malformed other-gate other-layer too-long other-gate other-gate])
        after (counts)
        second-of-a-word (do (send! (assoc malformed :marker (UUID/randomUUID))) (counts))
        strays-sent [[no-act-id :no-act-id 1] [malformed :act-id-malformed 1] [other-gate :act-id-of-the-other-gate 3]
                     [other-layer :act-id-of-another-layer 1] [too-long :id-too-long 1]]
        strays-logged (logged-strays (answer-log :streaming))
        entries-of-stray (fn [x] (filterv #(= (digest/hex-of x) (:digest (:entry %))) strays-logged))
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
        now (System/currentTimeMillis)
        fact-id [[:alice :streaming (UUID. 0 1)] 0]
        far-ahead (decide! (alice [{:e :note-33 :k :text}] {:based-on {fact-id (stamp/pack (+ now 60000) 0)}}))
        claim (decide! (alice [{:e :note-33 :k :text}] {:claimed-when Long/MAX_VALUE}))
        carried (stamp/pack (+ (System/currentTimeMillis) 5000) 7)
        near-ahead (decide! (alice [{:e :note-34 :k :text}] {:based-on {fact-id carried} :because-of (new-act-id :alice)}))
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
    (check "each arrival of a stray left an entry in the streaming gate's log of answers: one for each of four strays, and three for the one that came three times; each entry holds the stray's word, its digest and the task whose set holds that digest, and no other part; its record holds no batch, and is on the partition of that task"
           (mapv (fn [[x word arrivals]]
                   (let [task (task-of-stray :task-ps word (digest/hex-of x))]
                     (vec (repeat arrivals {:partition task :batch nil :entry {:stray word :digest (digest/hex-of x) :task task}}))))
                 strays-sent)
           (mapv (fn [[x _ _]] (entries-of-stray x)) strays-sent))
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
    (check "an act that carries a stamp more than 10 seconds ahead of the gate's clock is refused"
           :carried-stamp-too-far-ahead (:word far-ahead))
    (check "its answer holds an ordinary stamp, not one that follows the carried stamp"
           true (< (stamp/time-of (:stamp (:answer far-ahead))) (+ now 30000)))
    (check "claimed-when far ahead is a claim: the act is accepted, and its stamp does not follow it"
           [:accepted true] [(:word claim) (< (stamp/time-of (:stamp (:answer claim))) (+ now 30000))])
    (check "an act that carries a stamp 5 seconds ahead, based on a fact and because of an act that the record does not hold, is accepted, with a stamp later than the one it carries"
           [:accepted true] [(:word near-ahead) (> (:stamp (:answer near-ahead)) carried)])
    (check "and the next stamp of the task is later still" true (> (:stamp (:answer next-one)) (:stamp (:answer near-ahead))))
    (shown "a stray is kept under its word and gets no answer; strays seen, by word:" (pr-str (merge-with - after before))
           "| the same stray sent three times counts once | answer of a stray:" (pr-str (answer-of (:act-id other-gate))))
    (shown "each arrival of a stray has an entry in the streaming gate's log of answers; entries by word:"
           (pr-str (into (sorted-map) (map (fn [[x word _]] [word (count (entries-of-stray x))])) strays-sent))
           "| the three of one stray:" (pr-str (mapv :entry (entries-of-stray other-gate))))
    (shown "a thing over each limit is refused:" (:word facts-257) (:word based-1025) (:word value-over) (:word act-over) (:word long-entity)
           "| at each limit accepted:" (:word facts-256) (:word based-1024) (:word value-at))
    (shown "the bound on one value, through the streaming gate: a text of 65,537 bytes" (told value-over) "| of 65,536 bytes" (told value-at)
           "| its row holds" (count (value-bytes at-row)) "bytes in" (count (:value at-row)) "characters")
    (shown "the bound on an act, through the streaming gate: an act whose rows would hold" act-over-bytes "bytes of text, over 18874368:"
           (told act-over) "| in the log whole, with" (count (:facts act-over-logged)) "values plain")
    (shown "a carried stamp too far ahead:" (told far-ahead) "| 5 seconds ahead:" (told near-ahead) "carried" carried)))

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
    (check "an act-id of the depot microbatch, asked for with its layer and not with its uuid as the routing id, gets nothing" nil (ask :answer :alice [:alice :microbatch (nth accepted 2)]))
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
    (check "each row gives back the UTF-8 of that text, and holds six fields and no other"
           [(into {} (map (fn [[k t]] [k (utf8-of t)])) texts-of-kinds) #{#{:value :by-act :by-index :who :permission :stamp}}]
           [(into {} (map (fn [[k row]] [k (value-bytes row)])) rows) (set (map (comp set keys) (vals rows)))])
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
        right-making (decide! (making :slotted :alice :alice))
        log (logged-answers (answer-log :streaming))
        entry-of-refusal (fn [result word] {:act-id (:act-id (:thing result)) :stamp (:stamp (:answer result))
                                            :digest (digest/hex-of (:thing result)) :refused word})]
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
    (check "each act of a maker who may not act has its entry in the streaming gate's log of answers, with the word: the act of the holder of see, her act with a value, and her act under a permission that does not stand"
           [[(entry-of-refusal acts :permission-does-not-allow-acting)]
            [(entry-of-refusal sends-a-value :permission-does-not-allow-acting)]
            [(entry-of-refusal names-less :permission-does-not-stand)]]
           [(entries-under log (:act-id (:thing acts))) (entries-under log (:act-id (:thing sends-a-value)))
            (entries-under log (:act-id (:thing names-less)))])
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
    (check "each of those refusals got a stamp of its own on the task, later than the stamp before them" true (< stamp-before (:stamp (:answer acts))))
    (shown "a permission that says see, as its row holds it:" (pr-str (record/value-of the-row))
           "| its holder acts:" (told acts) "| names it without the slot:" (:word names-less) "| with see and act:" (:word names-more)
           "| gives:" (:word gives) "| sends a value:" (told sends-a-value)
           "| the entry of her refused act in the log of answers:" (pr-str (entries-under log (:act-id (:thing acts)))))
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
    (check "a fact under the marked key, with a value that is a keyword id: its place is the run of that value, and the row holds the fact that wrote it, its stamp, who and the permission, and no value"
           [[(record/by-value-key :canvas :on s1 (record/value-digest-of :canvas-1) :b1)
             {:by-act (nth (:act-id (:thing a1)) 2) :by-index 0 :stamp (:stamp (:answer a1))
              :who ":alice" :permission "[:alice :canvas :canvas]"}]]
           run-1)
    (check "a fact whose value is a UUID: the run of that thing holds its place" [:b2] run-2)
    (check "the fact is replaced, naming what it replaces: its place is out of the run of the value before, the key of that entry holds no row, and it is in the run of the new value beside the other place"
           [[] nil [:b1 :b2]] after-3)
    (check "the fact is written over with no replaces: the same, out of one run and into another"
           [[:b2] nil [:b1]] after-4)
    (check "the same value written again: the entry is written over, and holds the later fact"
           [[(record/by-value-key :canvas :on s1 (record/value-digest-of :canvas-2) :b1)
             {:by-act (nth (:act-id (:thing a5)) 2) :by-index 0 :stamp (:stamp (:answer a5))
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
    (check "under the new stamp every run is empty: the row that stayed is under the old stamp, in no run that a read can name, and the fact from before the second mark is not filled in"
           [[] [] []] after-mark-2)
    (check "a fact written after the second mark, over a fact from before it: its place is in the run of its value, since the new stamp"
           [[:b2] []] after-10)
    (check "and replaced again: out of that run, the key of the entry holds no row, in the run of the new value; the row of the first marking is still where it was"
           [[] nil [:b2] true] after-11)
    (check "a mark and a fact under its key in one act: the act is decided with the marks as they were before it, so the fact is in no run, and its stamp is the key's stamp, not a later one"
           [[] in-since] after-both)
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
        sent-at (System/currentTimeMillis)
        early (decide-in-base! (system-act [{:e :early :k :note}]))
        waited (- (System/currentTimeMillis) sent-at)
        [made-a made-b] (decide-in-one-batch! [(making :base :system :system)
                                               (update (making :base :system :system) :facts (comp vec reverse))])
        [winner loser] (if (= :accepted (:word made-a)) [made-a made-b] [made-b made-a])
        depot-rows (rows-by-task :mb-facts-ps (record/place-key :base :base :softland.store/depot))
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :system :softland.store/permission))
        after (decide-in-base! (system-act [{:e :early :k :note}]))]
    (check "before its making, the depot of the base is nothing" nil before)
    (check "an act for the base on the streaming depot, before the base is made, is refused as an act into a layer that is not made"
           :layer-not-made (:word on-the-other-depot))
    (check "an act into the base before it is made is refused, and its answer is found from its act-id alone"
           [:appended :layer-not-made true] [(:ack early) (:word early) (:mine? early)])
    (check "the answer of an act that is alone in its batch, with nothing sent after it, is readable within the hand's time limit"
           true (< waited time-limit-ms))
    (check "two making acts of the base in one batch, their facts in the two orders: one is accepted, the other refused"
           [:accepted true :permission-fact-not-beneath-the-acts-own] [(:word winner) (:mine? winner) (:word loser)])
    (check "the one that is accepted holds the earlier stamp" true (< (:stamp (:answer winner)) (:stamp (:answer loser))))
    (check "the base made by the system: its depot is microbatch" :microbatch (depot-of-layer :base))
    (check "the depot row and the system's permission row are on every task, each with the stamp of the making act's answer"
           [tasks tasks #{(:stamp (:answer winner))}]
           [(count depot-rows) (count permission-rows) (set (map :stamp (concat (vals depot-rows) (vals permission-rows))))])
    (check "the rows hold the depot as a fact on the layer's own entity, and the system's permission on its holder"
           [:microbatch system-own]
           [(record/value-of (first (vals depot-rows))) (record/value-of (first (vals permission-rows)))])
    (check "after the making, the same act of the system into the base is accepted" :accepted (:word after))
    (shown "an act into the base before it is made refused:" (told early) "| its answer was readable" waited
           "ms after the append, with nothing sent after it")
    (shown "the base made by the system, with the depot microbatch:" (told winner) "| depot-of-layer :base =" (depot-of-layer :base)
           "| the depot row is on" (count depot-rows) "tasks, the system's permission on" (count permission-rows))
    (shown "two making acts of the base in one batch: one accepted, the other" (:word loser))))

(deftest an-act-lands-in-the-base
  (let [candidates (mapv #(keyword (str "doc-" %)) (range 1 9))
        seen (decide-in-one-batch! (mapv (fn [e] (system-act [{:e e :k :seen}])) candidates))
        task-of-entity (into {} (map (fn [e] [e (task-of-row :mb-facts-ps (record/place-key :base e :seen))])) candidates)
        firsts (mapv first (vals (group-by task-of-entity candidates)))
        three (vec (take 3 (concat firsts (remove (set firsts) candidates))))
        e (first three)
        fact (value-fact e :title "the first text of the base")
        landed (decide-in-base! (system-act [fact]))
        landed-log (answer-log :microbatch)
        clock-then (kept-clock-row)
        last-stamp-then (gates-last-stamp)
        landed-entries (entries-under (logged-answers landed-log) (:act-id (:thing landed)))
        landed-record (some (fn [[_ r]] (when (some #(= (:act-id (:thing landed)) (:act-id %)) (:answers r)) r)) landed-log)
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (base-fact e :title)
        by-task (rows-by-task :mb-facts-ps (record/place-key :base e :title))
        logged (record-of-act (stored-in-base) (:act-id (:thing landed)))
        never-sent (answer-of (new-act-id :base))]
    (swap! kept assoc :base-landed landed :base-entities three :base-task-of-entity task-of-entity)
    (check "eight acts of one batch on eight entities are each accepted, and their facts are on more than one task"
           [(vec (repeat 8 :accepted)) true] [(mapv :word seen) (< 1 (count (set (vals task-of-entity))))])
    (check "one act with a plain value lands through the microbatch gate and is read back by act-id: accepted, with the digest of what was sent"
           [:appended :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the value as a String, the one text of the value in its normal form, and gives the UTF-8 of that text back; the fact that wrote it, who wrote it and the permission that the act named, and the stamp of the answer"
           ["\"the first text of the base\"" (utf8-of "\"the first text of the base\"") act-uuid 0 ":system" "[:system :base :base]" (:stamp (:answer landed))]
           [(:value the-row) (value-bytes the-row) (:by-act the-row) (:by-index the-row) (:who the-row) (:permission the-row) (:stamp the-row)])
    (check "the row holds those six fields and no other, and is on one task, the task of its entity"
           [#{:value :by-act :by-index :who :permission :stamp} [(task-of-entity e)]]
           [(set (keys the-row)) (vec (keys by-task))])
    (check "the microbatch depot's log holds the act's fact as the hand sent it, the value plain, with no part added"
           [{:e e :k :title :v "the first text of the base"}] (vec (:facts logged)))
    (check "read right after the door's read gave the answer, the act has one entry in the microbatch gate's log of answers: its act-id, the stamp of its answer and the digest of what was sent, with no word of a refusal and no value of the act's fact"
           [{:act-id (:act-id (:thing landed)) :stamp (:stamp (:answer landed)) :digest (digest/hex-of (:thing landed))}]
           landed-entries)
    (check "the record that holds the entry holds its answers and what the batch keeps of itself, and no other part: the batch's id and its clock, which are those of the kept clock's row, and its last stamp, which is the gate's"
           [#{:answers :batch :clock :last-stamp} (:batch clock-then) (:clock clock-then) last-stamp-then]
           [(set (keys landed-record)) (:batch landed-record) (:clock landed-record) (:last-stamp landed-record)])
    (check "an act-id of the base that was never sent has no answer: nothing yet" nil never-sent)
    (shown "one act with a plain value landing through the microbatch gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "its entry in the microbatch gate's log of answers, read right after the door's read:" (pr-str landed-entries)
           "| what its record holds of the batch:" (pr-str (select-keys landed-record [:batch :clock :last-stamp])))
    (shown "a value of the base kept as its one text in its row, on task" (task-of-entity e) "and no other:" (pr-str (:value the-row))
           "| read back:" (pr-str (record/read-value the-row)) "| the fields of the row:" (pr-str (vec (sort (keys the-row))))
           "| the microbatch depot's log holds the value plain:" (pr-str (vec (:facts logged))))))

(defn- seen-before-closing
  "Sends one act with a value, alone, on the entity that is the uuid of its
  own act-id, so that the row of its fact and the row of its answer are on
  one task. It watches the answers table itself, round the reads, for the
  row of the answer. At the first sight of the row it selects the row of the
  fact and the `:closed-through` of the task, then reads the answer as a
  door does. Before those it selects `:to-close` on the gate's task, the
  batch that the next run of the gate closes, for 20 ms at most while it is
  not there; and after them how many keys `:to-keep` holds on each task.
  Gives {:thing :row :value-row :closed-through :read :to-close :to-keep}."
  []
  (let [base-thing (system-act [])
        act-id (:act-id base-thing)
        uuid (nth act-id 2)
        x (assoc base-thing :facts [(value-fact uuid :note "seen before its batch is closed")])
        _ (append-to-base! x)
        deadline (+ (System/currentTimeMillis) time-limit-ms)
        row (loop []
              (or (base-row :mb-answers-ps uuid (record/answer-key :base uuid))
                  (when (< (System/currentTimeMillis) deadline)
                    (Thread/sleep 1)
                    (recur))))
        to-close (loop [n 0]
                   (or (rama/foreign-select-one (path/keypath :to-close) (:mb-task-ps *hand*) {:pkey :microbatch})
                       (when (< n 20)
                         (Thread/sleep 1)
                         (recur (inc n)))))
        value-row (base-fact uuid :note)
        closed (closed-through uuid)
        read (answer-of act-id)]
    {:thing x :row row :value-row value-row :closed-through closed :read read
     :to-close to-close :to-keep (to-keep-by-task)}))

(deftest an-answer-is-readable-when-its-batch-is-closed
  (let [tries (loop [n 1]
                (let [seen (seen-before-closing)
                      result (do (wait-until-processed!) (base-result (:thing seen) :appended))]
                  (note-base-answers! [result])
                  (if (or (and (:row seen) (nil? (:read seen))) (<= 5 n))
                    (assoc seen :after result :tries n)
                    (recur (inc n)))))
        {:keys [thing row value-row read after]} tries
        act-id (:act-id thing)
        uuid (nth act-id 2)]
    (check "the row of a maker's answer and the row of her act's fact are in the tables, the fact's row with the text of its value, and the answer's stamp is later than its task's closed-through"
           [true "\"seen before its batch is closed\"" true]
           [(some? row) (:value value-row)
            (boolean (and row (or (nil? (:closed-through tries)) (< (:closed-through tries) (:stamp row)))))])
    (check "and the read by act-id gives nothing of that answer: the batch is not closed" nil read)
    (check "while the batch is committed and not closed, the gate's task holds what the next run closes: `:to-close`, with a batch's id and its clock, and no other part"
           [#{:batch :clock} true true]
           [(set (keys (:to-close tries))) (integer? (:batch (:to-close tries))) (integer? (:clock (:to-close tries)))])
    (check "after the next run of the gate the read gives the answer, with the stamp of that row, and closed-through is not below it"
           [(:stamp row) :accepted true]
           [(:stamp (:answer after)) (:word after) (<= (:stamp row) (closed-through uuid))])
    (shown "an answer is not readable before its batch is closed: the row is in the table with stamp" (:stamp row)
           "| closed-through on its task" (:closed-through tries) "| answer-by-act-id gives" (pr-str read)
           "although the rows of the answer and of the fact are in the tables"
           "| :to-close on the gate's task then:" (pr-str (:to-close tries)) "| keys of :to-keep by task a moment later:" (pr-str (:to-keep tries))
           "| and is readable after:" (told after) "| closed-through" (closed-through uuid) "| caught at try" (:tries tries))))

(deftest refusals-and-repeats-in-the-base
  (let [landed (:base-landed @kept)
        accepted-thing (:thing landed)
        e (:e (first (:facts accepted-thing)))
        task-of-entity (:base-task-of-entity @kept)
        [e1 e2] (:base-entities @kept)
        [refused-one stale] (decide-in-one-batch!
                             [(system-act [(assoc (value-fact :doc-r1 :text "a value that names a fact which is not there")
                                                  :replaces [(new-act-id :base) 0])])
                              (system-act [(assoc (value-fact e :title "stale") :replaces [(new-act-id :base) 0])])])
        answer-before (answer-of (:act-id accepted-thing))
        row-before (base-fact e :title)
        last-stamp-before (gates-last-stamp)
        _ (append-to-base! accepted-thing)
        _ (append-to-base! (:thing refused-one))
        _ (wait-until-processed!)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (answer-of (:act-id (:thing refused-one)))
        row-after (base-fact e :title)
        other-under-used (decide-in-base! (assoc (system-act [{:e :doc-r2 :k :text}]) :act-id (:act-id accepted-thing)))
        twice-a (system-act [{:e e1 :k :twice}])
        twice-b (assoc (system-act [{:e e2 :k :twice}]) :act-id (:act-id twice-a))
        [two-a two-b] (decide-in-one-batch! [twice-a twice-b])
        written-a (base-fact e1 :twice)
        written-b (base-fact e2 :twice)]
    (check "an act that is refused for a place gets a recorded refusal through the microbatch gate: its reason, a stamp and the digest of what was sent"
           [:refused :replaced-fact-not-current true true]
           [(:verdict (:answer refused-one)) (:reason (:answer refused-one)) (int? (:stamp (:answer refused-one))) (:mine? refused-one)])
    (check "a refusal writes nothing at the place" nil (base-fact :doc-r1 :text))
    (check "a stale replaces is refused through the microbatch gate, and the place is as it was"
           [:replaced-fact-not-current (:stamp (:answer landed))] [(:word stale) (:stamp row-before)])
    (check "a repeat is answered from the record: the same answer, with the stamp of the first decision"
           answer-before answer-after)
    (check "not decided twice: the row of its place is the row of the first decision, with the text of the first value"
           [(:stamp row-before) (:by-act row-before) "\"the first text of the base\""]
           [(:stamp row-after) (:by-act row-after) (:value row-after)])
    (check "a repeat of a refused act reads the recorded refusal" (:answer refused-one) refused-again)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false] [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (base-fact :doc-r2 :text))
    (check "the two things of one act-id arrive on two tasks" true (not= (task-of-entity e1) (task-of-entity e2)))
    (check "one act-id under two different things in one batch: one answer, which is of one of the two"
           [true #{true false}] [(= (:answer two-a) (:answer two-b)) (set [(:mine? two-a) (:mine? two-b)])])
    (check "the thing that has the answer wrote its fact, and nothing of the other is written"
           [(:mine? two-a) (:mine? two-b)] [(some? written-a) (some? written-b)])
    (shown "a refusal through the microbatch gate, recorded with its reason:" (pr-str (:answer refused-one))
           "| a repeat of that refused act reads:" (pr-str refused-again))
    (shown "a stale replaces refused through the microbatch gate:" (told stale))
    (shown "a repeat (same act-id sent twice to the microbatch depot) answered from the record, not decided twice: stamp before"
           (:stamp answer-before) "after" (:stamp answer-after) "| the stamp of its place before" (:stamp row-before) "after" (:stamp row-after)
           "| the gate's last stamp moved from" last-stamp-before "to" (gates-last-stamp) "and gave the repeat no stamp")
    (shown "a second, different act under a used act-id of the base told so: the digest of the answer"
           (subs (:digest (:answer other-under-used)) 0 16) "is not the digest of what was sent"
           (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "one act-id under two things in one batch, on tasks" (task-of-entity e1) "and" (task-of-entity e2)
           ": one answer; written by the first thing?" (some? written-a) "by the second?" (some? written-b))))

(deftest one-batch-one-order
  (let [task-of-entity (:base-task-of-entity @kept)
        [ea eb] (:base-entities @kept)
        first-value (decide-in-base! (system-act [(value-fact ea :order "the fact before")]))
        f0 [(:act-id (:thing first-value)) 0]
        [a b] (decide-in-one-batch!
               [(system-act [(assoc (value-fact ea :order "from the first act") :replaces f0)])
                (system-act [{:e eb :k :order-other} (assoc (value-fact ea :order "from the second act") :replaces f0)])])
        [winner loser] (if (= :accepted (:word a)) [a b] [b a])
        place (base-fact ea :order)
        other-fact (base-fact eb :order-other)
        [first-mark second-mark] (decide-in-one-batch! [(system-act [{:e ea :k :mark}])
                                                        (system-act [{:e eb :k :mark-other} {:e ea :k :mark}])])
        mark (base-fact ea :mark)
        [give-carol carol-acts dave-acts give-dave]
        (decide-in-one-batch! [(system-act [{:e :carol :k :softland.store/permission :v carol-in-base}])
                               (thing :base :carol carol-in-base [{:e :carol :k :note}])
                               (thing :base :dave dave-in-base [{:e :dave :k :note}])
                               (system-act [{:e :dave :k :softland.store/permission :v dave-in-base}])])
        dave-later (decide-in-base! (thing :base :dave dave-in-base [{:e :dave :k :note}]))]
    (check "the two entities are on two tasks" true (not= (task-of-entity ea) (task-of-entity eb)))
    (check "two acts of one batch that replace the same fact, arriving on two tasks: one is accepted, the other refused as stale"
           [:accepted :replaced-fact-not-current] [(:word winner) (:word loser)])
    (check "the place holds the winner's fact, with the winner's stamp and the text of the winner's value"
           [(nth (:act-id (:thing winner)) 2) (:stamp (:answer winner)) (if (= a winner) "\"from the first act\"" "\"from the second act\"")]
           [(:by-act place) (:stamp place) (:value place)])
    (check "the winner holds the earlier stamp: it has the earlier turn" true (< (:stamp (:answer winner)) (:stamp (:answer loser))))
    (check "the loser is refused whole: its fact on the other task is there only when it is the winner's"
           (= b winner) (some? other-fact))
    (check "two acts of one batch that write one place and name nothing are both accepted, and the place holds the later stamp"
           [:accepted :accepted (max (:stamp (:answer first-mark)) (:stamp (:answer second-mark)))]
           [(:word first-mark) (:word second-mark) (:stamp mark)])
    (check "in one batch, on one entity: a give and then an act under it are both accepted; an act and then the give, the act is refused"
           [:accepted :accepted :permission-does-not-stand :accepted]
           [(:word give-carol) (:word carol-acts) (:word dave-acts) (:word give-dave)])
    (check "the same act under the permission, in a later batch, is accepted" :accepted (:word dave-later))
    (shown "two acts of one batch that replace the same fact, with one winner: on tasks" (task-of-entity ea) "and" (task-of-entity eb)
           "|" (told winner) "|" (told loser) "| the place is written by" (short-id [nil nil (:by-act place)]))
    (shown "two acts of one batch that write one place and name nothing:" (told first-mark) "|" (told second-mark) "| the place holds stamp" (:stamp mark))
    (shown "a give and an act under it in one batch, in both orders:" (:word give-carol) (:word carol-acts) "|" (:word dave-acts) (:word give-dave))))

(deftest a-person-acts-in-the-base
  (let [give (decide-in-base! (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}]))
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
        clock (System/currentTimeMillis)
        far (stamp/pack (+ clock 60000) 0)
        fact-id [(:act-id (:thing give)) 0]
        [not-given not-holder beside far-ahead far-ahead-no-standing]
        (decide-in-one-batch!
         [(thing :base :erin [:erin :base :base system-own] [{:e :erins-doc :k :note}])
          (thing :base :mallory alice-in-base [{:e :mallorys-doc :k :note}])
          (thing :base :alice alice-in-base [{:e :frank :k :softland.store/permission :v [:frank :base :base system-own]}])
          (system-act [{:e :far-doc :k :note}] {:based-on {fact-id far}})
          (thing :base :mallory alice-in-base [{:e :mallorys-doc :k :note}] {:based-on {fact-id far}})])
        next-one (decide-in-base! (system-act [{:e :far-doc :k :note}]))]
    (check "the system gives a person a permission in the base, beneath its own: accepted, and it sits on its holder as a fact"
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
    (check "a tool's own fact under the bare word :permission, on a holder's entity, is accepted in the base: its row is on one task, with the tool's value, and the store's permission is on every task"
           [:accepted 1 [:value "a tool's own permission"] tasks]
           [(:word tools-own) (count tools-own-rows) (record/read-value (first (vals tools-own-rows))) (count permission-rows-after)])
    (check "an act that carries a stamp more than 10 seconds ahead of the gate's clock is refused"
           :carried-stamp-too-far-ahead (:word far-ahead))
    (check "the same under a permission that does not stand is refused with the word of the 10-second rule, as in the streaming gate"
           :carried-stamp-too-far-ahead (:word far-ahead-no-standing))
    (check "the carried stamp puts the stamps of its batch 10 seconds ahead of the clock at most, and the next stamp is later still"
           [true true]
           [(< (stamp/time-of (:stamp (:answer far-ahead))) (+ (System/currentTimeMillis) 11000))
            (< (:stamp (:answer far-ahead)) (:stamp (:answer next-one)))])
    (shown "a person acting in the base under a permission that the system gave:" (told give) "| the permission's row is on"
           (count permission-rows) "tasks | her act, with a value:" (told writes))
    (shown "in the base, an act under a permission that does not stand:" (:word not-given) "| by a who that is not the holder:" (:word not-holder)
           "| a permission given beside the giver's own:" (:word beside))
    (shown "a permission's place takes one write in the base:" (:word give-again) "| ending a permission:" (:word end-it)
           "| the depot again:" (:word depot-again) (:word depot-other) "| a setting key on another entity:" (:word setting-elsewhere)
           "| a permission beneath a person's, and an act under it:" (:word give-bob) (:word bob-acts))
    (shown "in the base, a key in softland.store that the store does not know:" (:word unknown-key)
           "| a tool's own fact under the bare word :permission:" (:word tools-own) "| its row is on" (count tools-own-rows)
           "task, the store's permission of the same holder on" (count permission-rows-after))
    (shown "a carried stamp too far ahead, in the base:" (told far-ahead) "| the same with no standing:" (:word far-ahead-no-standing))))

(deftest a-permission-deeper-than-four-in-the-base
  (let [chain (chain-of-permissions :base system-own "deep" 33)
        at (fn [depth] (nth chain (dec depth)))
        holder (fn [depth] (first (at depth)))
        rows-on-its-holder (fn [depth] (rows-by-task :mb-facts-ps (record/place-key :base (holder depth) :softland.store/permission)))
        gives (mapv (fn [depth] (decide-in-base! (thing :base (holder (dec depth)) (at (dec depth))
                                                        [{:e (holder depth) :k :softland.store/permission :v (at depth)}])))
                    (range 2 34))
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
    (check "in the base each holder gives a permission beneath her own, one at each depth from 2 to 32, each in a batch of its own: the 31 gives are each accepted"
           (vec (repeat 31 :accepted)) (mapv :word (pop gives)))
    (check "the permissions of depth 4, 5, 31 and 32 each sit on their holder as they were given, on every task"
           (mapv (fn [depth] [tasks #{[:value (at depth)]}]) [4 5 31 32])
           (mapv (fn [depth] (let [rows (rows-on-its-holder depth)] [(count rows) (set (map record/read-value (vals rows)))])) [4 5 31 32]))
    (check "the give at depth 33 is refused as malformed: the refusal is recorded under its own act-id with the digest of what was sent, and no task holds a row on that holder"
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
    (shown "a permission deeper than four, through the microbatch gate: the gives at depth 2 to 32:" (pr-str (frequencies (map :word (pop gives))))
           "| the give at depth 33:" (told give-33) "| an act under depth 5:" (told under-5) "| under depth 32:" (told under-32))
    (shown "in the base, the row of the act under depth 32 holds its permission whole:" (count (:permission row-32)) "characters of text that open"
           (count (filter #{\[} (:permission row-32))) "vectors | an own permission of depth 33:" (told own-33)
           "| the fifth slot at depth 32:" (:word give-acts-too) (:word give-sees) "| its holders act:" (:word seer-acts) (:word looker-acts)
           "| the fifth slot at depth 33:" (:word slot-at-33))))

(deftest the-bounds-in-the-base
  (let [[value-over value-at] (decide-in-one-batch! [(system-act [(value-fact :bound-doc-over :text over-the-bound-of-a-value)])
                                                     (system-act [(value-fact :bound-doc-at :text at-the-bound-of-a-value)])])
        at-row (base-fact :bound-doc-at :text)
        act-over (decide-in-base! (act-over-its-bound :base))
        act-over-bytes (* 256 (+ 65536 (count (utf8-of (pr-str (:who (:thing act-over)))))
                                 (count (utf8-of (pr-str (:permission (:thing act-over)))))))
        records (vec (stored-in-base))
        value-over-logged (record-of-act records (:act-id (:thing value-over)))
        act-over-logged (record-of-act records (:act-id (:thing act-over)))]
    (check "through the microbatch gate, a value whose one text is 65,537 bytes is refused with the word of the bound on one value: the refusal is recorded under its own act-id with the digest of what was sent, no task holds a row of it, and the depot's log holds the act whole, the value plain"
           [:appended :value-too-large true {} true]
           [(:ack value-over) (:word value-over) (:mine? value-over)
            (rows-by-task :mb-facts-ps (record/place-key :base :bound-doc-over :text)) (= (:thing value-over) value-over-logged)])
    (check "a value whose one text is 65,536 bytes is accepted in the same batch, and its row holds those bytes: the string in its quotes, which reads back as the value"
           [:accepted 65536 21848 true true]
           [(:word value-at) (count (value-bytes at-row)) (count (:value at-row))
            (= (str "\"" at-the-bound-of-a-value "\"") (:value at-row))
            (= [:value at-the-bound-of-a-value] (record/read-value at-row))])
    (check "an act of 256 values at that bound, under a who and a permission of more than 8,192 bytes of text, would write more than 18,874,368 bytes into the rows of its facts, counted by the proof with Clojure's printer: it is refused with the word of the bound on an act, the refusal recorded under its own act-id; the depot's log holds it whole, its 256 values plain; and no task holds a row of it"
           [true :appended :act-too-large true true 256 {} {}]
           [(< 18874368 act-over-bytes) (:ack act-over) (:word act-over) (:mine? act-over) (= (:thing act-over) act-over-logged)
            (count (:facts act-over-logged))
            (rows-by-task :mb-facts-ps (record/place-key :base :over-0 :text))
            (rows-by-task :mb-facts-ps (record/place-key :base :over-255 :text))])
    (shown "the bound on one value, through the microbatch gate: a text of 65,537 bytes" (told value-over) "| of 65,536 bytes" (told value-at)
           "| its row holds" (count (value-bytes at-row)) "bytes in" (count (:value at-row)) "characters")
    (shown "the bound on an act, through the microbatch gate: an act whose rows would hold" act-over-bytes "bytes of text, over 18874368:"
           (told act-over) "| in the log whole, with" (count (:facts act-over-logged)) "values plain")))

(deftest a-long-batch
  (let [n 400
        entity (fn [i] (keyword (str "long-" (mod i 20))))
        k (fn [i] (keyword (str "k" i)))
        results (decide-in-one-batch! (mapv (fn [i] (system-act [{:e (entity i) :k (k i)}])) (range n)))
        stamps (mapv #(:stamp (:answer %)) results)
        sample (range 0 n 37)
        log (answer-log :microbatch)
        clock-then (kept-clock-row)
        last-stamp-then (gates-last-stamp)
        logged (logged-answers log)
        ids (set (map (comp :act-id :thing) results))
        of-the-batch (filterv #(contains? ids (:act-id (:entry %))) logged)
        batch-ids (set (map :batch of-the-batch))
        batch-records (filterv (fn [[_ r]] (contains? batch-ids (:batch r))) log)]
    (check "a batch of 400 acts on 20 entities: each is accepted, each with a stamp of its own"
           [#{:accepted} n] [(set (map :word results)) (count (set stamps))])
    (check "the acts on one entity keep the depot's order: their stamps go up in the order sent"
           true (every? (fn [[_ sent]] (apply < (map second sent))) (group-by #(entity (first %)) (map-indexed vector stamps))))
    (check "the fact of each act of a sample is on its place, with the stamp of its answer"
           (mapv #(nth stamps %) sample) (mapv #(:stamp (base-fact (entity %) (k %))) sample))
    (check "read right after the door's reads gave the 400 answers, each has one entry in the microbatch gate's log of answers, the entry of its answer"
           [n []] [(count of-the-batch) (entries-apart logged (map (fn [r] [(:act-id (:thing r)) (:answer r)]) results))])
    (check "the 400 entries are in the records of one batch, the batch of the kept clock's row, on one partition; no record holds more than 256 entries, so they are more than one; the first holds what the batch keeps of itself, its clock and its last stamp, which are the kept clock and the gate's last stamp; and no other record of the batch holds either"
           [#{(:batch clock-then)} 1 true true {:clock (:clock clock-then) :last-stamp last-stamp-then} #{{}}]
           [batch-ids (count (set (map first batch-records)))
            (every? (fn [[_ r]] (<= (+ (count (:answers r)) (count (:strays r))) 256)) batch-records)
            (< 1 (count batch-records))
            (select-keys (second (first batch-records)) [:clock :last-stamp])
            (set (map (fn [[_ r]] (select-keys r [:clock :last-stamp])) (rest batch-records)))])
    (check "the last stamp that the batch keeps of itself is not below the stamp of any of its answers" true (every? #(<= % last-stamp-then) stamps))
    (shown "a batch of" n "acts on 20 entities: each accepted, with" (count (set stamps))
           "stamps; on each entity the stamps go up in the order sent")
    (shown "the batch of" n "answers in the microbatch gate's log of answers:" (count batch-records) "records of batch" (pr-str batch-ids)
           "on partition" (pr-str (set (map first batch-records))) "with" (pr-str (mapv (fn [[_ r]] (count (:answers r))) batch-records))
           "entries | the first holds" (pr-str (select-keys (second (first batch-records)) [:clock :last-stamp])))))

(deftest an-act-on-several-tasks
  (let [task-of-entity (:base-task-of-entity @kept)
        entities (:base-entities @kept)
        places (mapv (fn [e] [e :body]) entities)
        sent-facts (mapv (fn [[e k]] (value-fact e k (str "the body of " (name e)))) places)
        place-rows (fn [] (mapv (fn [[e k]] (rows-by-task :mb-facts-ps (record/place-key :base e k))) places))
        refused (decide-in-base! (system-act (update sent-facts 2 assoc :replaces [(new-act-id :base) 0])))
        places-after-refusal (place-rows)
        earlier-stamps (keep (fn [[e task]] (when (contains? (set (map task-of-entity entities)) task) (:stamp (base-fact e :seen))))
                             task-of-entity)
        last-stamp-before (gates-last-stamp)
        accepted (decide-in-base! (system-act sent-facts))
        act-uuid (nth (:act-id (:thing accepted)) 2)
        stamp (:stamp (:answer accepted))
        places-after (place-rows)
        last-stamp-after (gates-last-stamp)
        [arrives-on other-entity] entities
        planted (decide-in-base! (system-act [{:e other-entity :k :crossed}]))
        crossing (decide-in-base! (system-act [{:e arrives-on :k :crossing}
                                               {:e other-entity :k :crossed :replaces [(:act-id (:thing planted)) 0]}]))
        crossed-row (base-fact other-entity :crossed)
        crossing-again (decide-in-base! (system-act [{:e arrives-on :k :crossing-again}
                                                     {:e other-entity :k :crossed :replaces [(:act-id (:thing planted)) 0]}]))]
    (swap! kept assoc :base-values {:places places :act-uuid act-uuid})
    (check "the three entities of the act are on more than one task" true (< 1 (count (set (map task-of-entity entities)))))
    (check "an act of three values that touches several tasks, with its third fact stale, is refused whole" :replaced-fact-not-current (:word refused))
    (check "no row of it is on any task: none of its three places holds a fact" [{} {} {}] places-after-refusal)
    (check "the same act with each fact good is accepted whole" [:accepted true] [(:word accepted) (:mine? accepted)])
    (check "each fact's row is on the task of its entity and no other, written by the act, with the stamp of its answer and the text of its value"
           (mapv (fn [[e _] i] [[(task-of-entity e)] act-uuid i stamp (str "\"the body of " (name e) "\"")]) places (range))
           (mapv (fn [rows] (let [row (first (vals rows))] [(vec (keys rows)) (:by-act row) (:by-index row) (:stamp row) (:value row)])) places-after))
    (check "the one stamp of builder's rule 18: later than the stamp of each row written before on those tasks, and than the refusal's"
           [true true] [(every? #(< % stamp) earlier-stamps) (< (:stamp (:answer refused)) stamp)])
    (check "the gate has one last stamp: it was below the act's stamp before the batch, and is not below it after"
           [true true] [(< last-stamp-before stamp) (<= stamp last-stamp-after)])
    (check "an act alone in its batch replaces the current fact of a place on another task than the one where it arrived: accepted, and the place holds its fact"
           [true :accepted :accepted (nth (:act-id (:thing crossing)) 2) 1 (:stamp (:answer crossing))]
           [(not= (task-of-entity arrives-on) (task-of-entity other-entity)) (:word planted) (:word crossing)
            (:by-act crossed-row) (:by-index crossed-row) (:stamp crossed-row)])
    (check "the same replaces sent again under a new act-id is stale, and nothing of that act is written on either task"
           [:replaced-fact-not-current nil (:stamp (:answer crossing))]
           [(:word crossing-again) (base-fact arrives-on :crossing-again) (:stamp (base-fact other-entity :crossed))])
    (shown "an act of three values that touches several tasks accepted whole:" (told accepted) "| its facts on tasks" (pr-str (mapv #(vec (keys %)) places-after))
           "| the text in each row:" (pr-str (mapv (fn [rows] (:value (first (vals rows)))) places-after)))
    (shown "one refused whole with no row of it on any task:" (told refused) "| rows at its three places:" (pr-str places-after-refusal))
    (shown "an act that arrived on task" (task-of-entity arrives-on) "replaces the current fact of a place on task" (task-of-entity other-entity)
           "and is accepted:" (told crossing) "| the same replaces again:" (told crossing-again))
    (shown "the one stamp of builder's rule 18: the act's answer and its three facts, on" (count (set (map task-of-entity entities)))
           "tasks, hold the stamp" stamp "| later than each of" (count earlier-stamps) "rows written before on those tasks"
           "| the gate's last stamp before" last-stamp-before "after" last-stamp-after)))

(deftest each-gate-tells-the-other-gates-layers
  (let [base-on-streaming (decide! (thing :base :system system-own [{:e :wrong-depot :k :note}]
                                          {:act-id [:base :streaming (UUID/randomUUID)] :depot :streaming}))
        [alice-on-microbatch nobody-on-microbatch]
        (decide-in-one-batch!
         [(thing :alice :alice alice-own [{:e :wrong-depot :k :note}] {:act-id [:alice :microbatch (UUID/randomUUID)] :depot :microbatch})
          (thing :nobody :nobody [:nobody :nobody :nobody] [{:e :wrong-depot :k :note}]
                 {:act-id [:nobody :microbatch (UUID/randomUUID)] :depot :microbatch})])
        strays-before (base-strays)
        both (alice [{:e :both-depots :k :text}])
        on-streaming (decide! both)
        malformed {:act-id [:base :microbatch] :layer :base :marker (UUID/randomUUID)}
        acks (mapv append-to-base! [both 5 5 malformed])
        _ (wait-until-processed!)
        strays-after (base-strays)
        base-strays-sent [[both :act-id-of-the-other-gate 1] [5 :no-act-id 2] [malformed :act-id-malformed 1]]
        stray-digests-sent (set (map (fn [[x _ _]] (digest/hex-of x)) base-strays-sent))
        log (await-log :microbatch (fn [records]
                                     (<= 4 (count (filter #(contains? stray-digests-sent (:digest (:entry %))) (logged-strays records))))))
        strays-logged (logged-strays log)
        entries-of-stray (fn [x] (into [] (comp (map :entry) (filter #(= (digest/hex-of x) (:digest %)))) strays-logged))
        records-of-strays (filterv (fn [[_ r]] (some #(contains? stray-digests-sent (:digest %)) (:strays r))) log)]
    (check "an act for the base, a microbatch layer, sent to the streaming depot is refused as an act into a layer of the other gate, under its streaming act-id"
           [:acked :layer-belongs-to-the-other-gate true] [(:ack base-on-streaming) (:word base-on-streaming) (:mine? base-on-streaming)])
    (check "an act for a streaming layer sent to the microbatch depot is refused the same way, and read back with the uuid of its act-id"
           [:layer-belongs-to-the-other-gate true] [(:word alice-on-microbatch) (:mine? alice-on-microbatch)])
    (check "an act for a layer that no gate made, sent to the microbatch depot, is refused as an act into a layer that is not made"
           :layer-not-made (:word nobody-on-microbatch))
    (check "neither wrote a fact, in either record"
           [nil {}] [(fact-row :base :wrong-depot :note) (rows-by-task :mb-facts-ps (record/place-key :alice :wrong-depot :note))])
    (check "each thing was appended" (vec (repeat 4 :appended)) acks)
    (check "one act-id sent on both depots has one answer, the streaming gate's: the microbatch gate keeps the thing as a stray"
           [:accepted (:answer on-streaming)] [(:word on-streaming) (answer-of (:act-id both))])
    (check "a stray on the microbatch depot is kept under its word, once however often it comes, and gets no answer"
           {:no-act-id 1 :act-id-malformed 1 :id-too-long 0 :act-id-of-the-other-gate 1 :act-id-of-another-layer 0}
           (merge-with - strays-after strays-before))
    (check "each arrival of a stray on the microbatch depot has an entry in the microbatch gate's log of answers once a later run of the gate has closed its batch: one for each of two strays, and two for the one that came twice; each entry holds the stray's word, its digest and the task whose set holds that digest, and no other part"
           (mapv (fn [[x word arrivals]]
                   (vec (repeat arrivals {:stray word :digest (digest/hex-of x) :task (task-of-stray :mb-task-ps word (digest/hex-of x))})))
                 base-strays-sent)
           (mapv (fn [[x _ _]] (entries-of-stray x)) base-strays-sent))
    (check "a batch of strays alone made no stamp: each record that holds one of those entries holds the batch's id and its strays, and no clock, no last stamp and no answer"
           #{#{:batch :strays}} (set (map (fn [[_ r]] (set (keys r))) records-of-strays)))
    (check "the act for a streaming layer that the microbatch gate refused has its entry in that gate's log, with the word of its answer's row"
           [{:act-id (:act-id (:thing alice-on-microbatch)) :stamp (:stamp (:answer alice-on-microbatch))
             :digest (digest/hex-of (:thing alice-on-microbatch)) :refused :layer-belongs-to-the-other-gate}]
           (entries-under (logged-answers log) (:act-id (:thing alice-on-microbatch))))
    (shown "each arrival of a stray on the microbatch depot has an entry in the microbatch gate's log of answers; entries by word:"
           (pr-str (into (sorted-map) (map (fn [[x word _]] [word (count (entries-of-stray x))])) base-strays-sent))
           "| the records that hold them:" (pr-str (mapv (fn [[p r]] [p (:batch r) (count (:strays r))]) records-of-strays)))
    (shown "rule 7 in both directions: an act for a microbatch layer sent to the streaming depot:" (told base-on-streaming)
           "| an act for a streaming layer sent to the microbatch depot:" (told alice-on-microbatch)
           "| for a layer that is not made:" (:word nobody-on-microbatch))
    (shown "one act-id on both depots has one answer:" (told on-streaming) "| strays of the microbatch gate, by word:"
           (pr-str (merge-with - strays-after strays-before)) "| the same stray sent twice counts once")))

;; ------------------------------------ what the microbatch gate keeps for a read

(deftest what-a-fact-keeps-in-the-base
  (let [facts (mapv (fn [[k v]] (value-fact :kept-doc k v)) kinds-of-value)
        landed (decide-in-base! (system-act facts))
        rows (into {} (map (fn [k] [k (base-fact :kept-doc k)])) (keys kinds-of-value))
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
        give-row (base-row :mb-facts-ps :alice (record/place-key :base :alice :softland.store/permission))]
    (check "an act with a plain value of each kind, on one entity of the base, is accepted" [:accepted true] [(:word landed) (:mine? landed)])
    (check "each row of the base holds the one text of its value in its normal form, as the proof wrote it out by hand"
           texts-of-kinds (into {} (map (fn [[k row]] [k (:value row)])) rows))
    (check "each row gives back the UTF-8 of that text, and holds six fields and no other"
           [(into {} (map (fn [[k t]] [k (utf8-of t)])) texts-of-kinds) #{#{:value :by-act :by-index :who :permission :stamp}}]
           [(into {} (map (fn [[k row]] [k (value-bytes row)])) rows) (set (map (comp set keys) (vals rows)))])
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
    (shown "a value of each kind, kept by the microbatch gate as its one text in its row:"
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
        end-of-first (gates-last-stamp)
        seconds (decide-in-one-batch! [(write who-may-see :everyone) (write builds-on []) (write marks #{})])
        after-second (mapv rows [who-may-see builds-on marks])
        fact-of (fn [results i] [(:act-id (:thing (nth results i))) 0])
        named (decide-in-one-batch! [(write who-may-see :members :replaces (fact-of seconds 0))
                                     (write builds-on [:team] :replaces (fact-of seconds 1))
                                     (write marks #{:tag} :replaces (fact-of seconds 2))])
        after-named (mapv rows [who-may-see builds-on marks])
        end-of-named (gates-last-stamp)
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
        after-the-other-depot (rows who-may-see)
        log (answer-log :microbatch)
        logged (logged-answers log)
        last-stamps-of-the-batch-of (fn [result]
                                      (let [batches (set (map :batch (filter #(= (:act-id (:thing result)) (:act-id (:entry %))) logged)))]
                                        (set (keep (fn [[_ r]] (when (contains? batches (:batch r)) (:last-stamp r))) log))))
        level-since-of (fn [by-task] (get (record/marked-keys-of (first (vals by-task))) :tag))]
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
    (check "the row of the marked keys holds on every task, derived by the gate, the stamp of each marked key: the last stamp of the batch that marked it; none when the set is empty; a new one when the key is marked again"
           [#{{:tag end-of-first}} #{{}} #{{:tag end-of-named}} true]
           [(set (map record/marked-keys-of (vals (nth after-first 2)))) (set (map record/marked-keys-of (vals (nth after-second 2))))
            (set (map record/marked-keys-of (vals (nth after-named 2)))) (< end-of-first end-of-named)])
    (check "the stamp of a newly marked key is not below the stamp of any act of its batch"
           true (every? #(<= (:stamp (:answer %)) end-of-first) firsts))
    (check "the last stamp of a batch is in a log: the records of the batch that holds the entry of the act which marked the key hold one `:last-stamp`, and it is the stamp that the row of the marked keys gives the key, at the first marking and when the key is marked again"
           [#{(level-since-of (nth after-first 2))} #{(level-since-of (nth after-named 2))}]
           [(last-stamps-of-the-batch-of (nth firsts 2)) (last-stamps-of-the-batch-of (nth named 2))])
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
           (pr-str (set (map record/marked-keys-of (vals (nth after-first 2))))) "where the last stamp of that batch is" end-of-first
           "| after the set was emptied" (pr-str (set (map record/marked-keys-of (vals (nth after-second 2)))))
           "| after the key was marked again" (pr-str (set (map record/marked-keys-of (vals (nth after-named 2)))))
           "| the last stamps that the microbatch gate's log of answers holds for the two batches that marked the key:"
           (pr-str (last-stamps-of-the-batch-of (nth firsts 2))) (pr-str (last-stamps-of-the-batch-of (nth named 2))))))

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
        permission-rows-after (rows-by-task :mb-facts-ps (record/place-key :base :erin :softland.store/permission))
        log (logged-answers (answer-log :microbatch))
        entry-of-refusal (fn [result word] {:act-id (:act-id (:thing result)) :stamp (:stamp (:answer result))
                                            :digest (digest/hex-of (:thing result)) :refused word})]
    (check "the system gives a person a permission in the base that says see: accepted, and its row is on every task with the whole text, slot and all"
           [:accepted tasks #{[:value erin-sees]}]
           [(:word give-see) (count permission-rows) (set (map record/read-value (vals permission-rows)))])
    (check "her act in the batch of the give, after it, is decided on the meeting task against the row that the give wrote: refused, an act needs act"
           :permission-does-not-allow-acting (:word in-the-same-batch))
    (check "in a later batch her act is refused for good where it arrives, with the same word, and nothing is written"
           [:permission-does-not-allow-acting true {}]
           [(:word acts) (:mine? acts) (rows-by-task :mb-facts-ps (record/place-key :base :erins-doc :note))])
    (check "she names her permission without the slot, and with see and act in it: it does not stand"
           [:permission-does-not-stand :permission-does-not-stand] [(:word names-less) (:word names-more)])
    (check "she cannot give, and her act with a value is refused with the same word, under its own act-id with the digest of what was sent, and no task holds a row of it"
           [:permission-does-not-allow-acting {} :permission-does-not-allow-acting true {}]
           [(:word gives) (rows-by-task :mb-facts-ps (record/place-key :base :frank :softland.store/permission))
            (:word sends-a-value) (:mine? sends-a-value) (rows-by-task :mb-facts-ps (record/place-key :base :erins-value :text))])
    (check "each act of a maker who may not act has its entry in the microbatch gate's log of answers once the door's read gave its answer, with the word: her act in the batch of the give, her act in a later batch, her act with a value, and her act under a permission that does not stand"
           [[(entry-of-refusal in-the-same-batch :permission-does-not-allow-acting)]
            [(entry-of-refusal acts :permission-does-not-allow-acting)]
            [(entry-of-refusal sends-a-value :permission-does-not-allow-acting)]
            [(entry-of-refusal names-less :permission-does-not-stand)]]
           (mapv (fn [result] (entries-under log (:act-id (:thing result)))) [in-the-same-batch acts sends-a-value names-less]))
    (check "a second give to her, of see and act, is refused: a permission's place takes one write, and her rows are as they were"
           [:store-key-takes-one-write (into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows)]
           [(:word raise) (into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows-after)])
    (check "a permission that says see and act, given in the same batch: its holder acts in a later batch" [:accepted :accepted]
           [(:word give-greta) (:word greta-writes)])
    (shown "in the base, a permission that says see:" (pr-str (record/value-of (first (vals permission-rows)))) "on" (count permission-rows) "tasks"
           "| its holder acts in the batch of the give:" (:word in-the-same-batch) "| in a later batch:" (told acts)
           "| names it without the slot:" (:word names-less) "| with see and act:" (:word names-more) "| gives:" (:word gives)
           "| sends a value:" (told sends-a-value) "| a second give of see and act:" (:word raise)
           "| the entry of her refused act in the log of answers:" (pr-str (entries-under log (:act-id (:thing acts)))))))

(deftest what-points-at-a-thing-in-the-base
  (let [task-of-entity (:base-task-of-entity @kept)
        [e1 e2 e3] (:base-entities @kept)
        u (UUID/randomUUID)
        place (fn [e] (base-fact e :on))
        writes! (fn [e v & {:as more}] (decide-in-base! (system-act [(merge (value-fact e :on v) more)])))
        marks! (fn [keys] (decide-in-base! (system-act [{:e :base :k marks :v keys}])))
        key-task (fn [k] (vec (keys (rows-by-task :mb-by-value-ps k))))
        before-the-mark (writes! e3 :canvas-1)
        mark-1 (marks! #{:on})
        s1 (gates-last-stamp)
        marked-1 (base-marks-rows)
        run-0 (base-run-of :on s1 :canvas-1)
        a1 (writes! e1 :canvas-1)
        a2 (writes! e2 :canvas-1)
        run-1 (base-entries-of :on s1 :canvas-1)
        tasks-of-run-1 (mapv (fn [[k _]] (key-task k)) run-1)
        a3 (writes! e1 u :replaces [(:act-id (:thing a1)) 0])
        after-3 [(base-run-of :on s1 :canvas-1) (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-1 e1)) (base-run-of :on s1 u)]
        a4 (writes! e1 :canvas-2)
        after-4 [(base-run-of :on s1 u) (rows-by-task :mb-by-value-ps (base-entry-key :on s1 u e1)) (base-run-of :on s1 :canvas-2)]
        a5 (writes! e1 :canvas-2)
        after-5 (base-entries-of :on s1 :canvas-2)
        _ (append-to-base! (:thing a4))
        _ (wait-until-processed!)
        after-again (base-entries-of :on s1 :canvas-2)
        a6 (decide-in-base! (system-act [{:e e1 :k :on}]))
        after-6 [(base-run-of :on s1 :canvas-2) (record/read-value (place e1))]
        mark-off (marks! #{})
        marked-off (base-marked-keys)
        a8 (writes! e2 :canvas-3)
        after-8 [(count (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-1 e2)))
                 (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-3 e2)) (record/read-value (place e2))]
        mark-2 (marks! #{:on})
        s2 (gates-last-stamp)
        marked-2 (base-marks-rows)
        after-mark-2 [(base-run-of :on s2 :canvas-3) (base-run-of :on s2 :canvas-1)]
        a10 (writes! e2 :canvas-4)
        after-10 [(base-run-of :on s2 :canvas-4) (base-run-of :on s2 :canvas-3)]
        a11 (decide-in-base! (system-act [{:e e1 :k :beside} (value-fact e2 :on :canvas-5)]))
        after-11 [(base-run-of :on s2 :canvas-4) (rows-by-task :mb-by-value-ps (base-entry-key :on s2 :canvas-4 e2))
                  (base-run-of :on s2 :canvas-5) (count (rows-by-task :mb-by-value-ps (base-entry-key :on s1 :canvas-1 e2)))]
        final-run (base-entries-of :on s2 :canvas-5)]
    (swap! kept assoc :base-run {:k :on :since s2 :value :canvas-5} :base-marked-since s2)
    (check "each act of this walk is accepted"
           #{:accepted} (set (map :word [before-the-mark mark-1 a1 a2 a3 a4 a5 a6 mark-off a8 mark-2 a10 a11])))
    (check "the two entities of the walk are on two tasks" true (not= (task-of-entity e1) (task-of-entity e2)))
    (check "the mark puts the key in the row of the marked keys, on every task, with the last stamp of the batch that marked it, which is not below the stamp of the marking act"
           [tasks #{{:on s1}} true]
           [(count marked-1) (set (map record/marked-keys-of (vals marked-1))) (<= (:stamp (:answer mark-1)) s1)])
    (check "nothing is filled from before: the fact that was written before the mark is in no run" [] run-0)
    (check "two facts on two tasks with one value: both places are in the run of that value, and their two rows are on one task, found from the value alone"
           [(vec (sort-by str [e1 e2])) true true
            {:by-act (nth (:act-id (:thing a1)) 2) :by-index 0 :stamp (:stamp (:answer a1)) :who ":system" :permission "[:system :base :base]"}]
           [(vec (sort-by str (map (fn [[k _]] (record/by-value-entity k)) run-1)))
            (apply = tasks-of-run-1) (= 1 (count (first tasks-of-run-1)))
            (some (fn [[k row]] (when (= e1 (record/by-value-entity k)) row)) run-1)])
    (check "the fact is replaced, naming what it replaces, with a UUID: its place is out of the run of the value before, no task holds a row under the key of that entry, and it is in the run of the UUID"
           [[e2] {} [e1]] after-3)
    (check "the fact is written over with no replaces: out of the run of the UUID, no row under that key on any task, into the run of the new value"
           [[] {} [e1]] after-4)
    (check "the same value written again: the entry is written over, and holds the later fact"
           [[(base-entry-key :on s1 :canvas-2 e1)
             {:by-act (nth (:act-id (:thing a5)) 2) :by-index 0 :stamp (:stamp (:answer a5)) :who ":system" :permission "[:system :base :base]"}]]
           after-5)
    (check "an earlier act that wrote that entry is appended a second time: the gate answers it from the record, and the run is as the later act left it"
           after-5 after-again)
    (check "a retract: the place is out of the run, and holds a fact with no value" [[] [:nothing-here]] after-6)
    (check "the mark is taken off by a second write of the set without the key" {} marked-off)
    (check "a fact that is replaced while the key is not marked writes nothing in that order: the row of the earlier value stays on its one task, and the new value has no row"
           [1 {} [:value :canvas-3]] after-8)
    (check "the key is marked again: every task holds the new stamp, the last stamp of that batch, later than the first"
           [tasks #{{:on s2}} true]
           [(count marked-2) (set (map record/marked-keys-of (vals marked-2))) (< s1 s2)])
    (check "under the new stamp every run is empty: the row that stayed is under the old stamp, in no run that a read can name"
           [[] []] after-mark-2)
    (check "a fact written after the second mark, over a fact from before it: its place is in the run of its value" [[e2] []] after-10)
    (check "an act that arrives on another task replaces it: the home task of the place sent the row before in its reply, so the entry of the value before is out, no task holds a row under its key, and the place is in the run of the new value; the row of the first marking is still there"
           [[] {} [e2] 1] after-11)
    (shown "what points at a thing, in the base. The key :on is marked since" s1 ", the last stamp of the batch that marked it | two places on tasks"
           (task-of-entity e1) "and" (task-of-entity e2) "hold one value: the rows of its run are both on task" (pr-str (first tasks-of-run-1)))
    (shown "the fact replaced, written over, written again, retracted; after each, the run of the value before, the rows under the key of the entry that went out, the run of the new value:"
           (pr-str after-3) (pr-str after-4) "| after the retract:" (pr-str after-6))
    (shown "the mark taken off, a fact replaced, the key marked again since" s2 ": every run under the new stamp is empty" (pr-str after-mark-2)
           "| then a fact written, and replaced by an act that arrived on another task:" (pr-str after-10) (pr-str after-11)
           "| the run that a read then finds:" (pr-str (mapv (fn [[k _]] (record/by-value-entity k)) final-run)))))

(deftest one-batch-under-a-marked-key
  (let [task-of-entity (:base-task-of-entity @kept)
        [e1 e2 e3] (:base-entities @kept)
        s2 (:base-marked-since @kept)
        [first-on-one-place second-on-one-place]
        (decide-in-one-batch! [(system-act [(value-fact e2 :on :first-of-two)])
                               (system-act [(value-fact e2 :on :second-of-two)])])
        place (base-fact e2 :on)
        after-two [(base-run-of :on s2 :canvas-5) (base-run-of :on s2 :first-of-two) (base-run-of :on s2 :second-of-two)
                   (rows-by-task :mb-by-value-ps (base-entry-key :on s2 :first-of-two e2))]
        [marking under-the-new-mark]
        (decide-in-one-batch! [(system-act [{:e :base :k marks :v #{:on :in}}])
                               (system-act [(value-fact e1 :in :canvas-1)])])
        end-of-marking (gates-last-stamp)
        marked (base-marks-rows)
        in-fact (base-fact e1 :in)
        after-marking (base-run-of :in end-of-marking :canvas-1)
        later (decide-in-base! (system-act [(value-fact e3 :in :canvas-1)]))
        after-later (base-run-of :in end-of-marking :canvas-1)
        [see-a see-b] (decide-in-one-batch! [(system-act [{:e :base :k who-may-see :v :members}])
                                             (system-act [{:e e1 :k :beside-2} {:e :base :k who-may-see :v :everyone}])])
        see-rows (rows-by-task :mb-facts-ps (record/place-key :base :base who-may-see))
        later-of-the-two (if (< (:stamp (:answer see-a)) (:stamp (:answer see-b))) see-b see-a)]
    (check "two acts of one batch on one place under a marked key, each with the place as its own: both are accepted"
           [:accepted :accepted] [(:word first-on-one-place) (:word second-on-one-place)])
    (check "the place holds the later turn's value; the entry of the value before the batch is out, the first act's value has no row on any task, and the place is in the run of the later value alone"
           [[:value :second-of-two] (:stamp (:answer second-on-one-place)) [[] [] [e2] {}]]
           [(record/read-value place) (:stamp place) after-two])
    (check "a mark and a fact under its key in one batch: both accepted; the key is marked on every task since the last stamp of that batch, and the stamp that it keeps for the other key is as it was"
           [:accepted :accepted tasks #{{:on s2 :in end-of-marking}}]
           [(:word marking) (:word under-the-new-mark) (count marked) (set (map record/marked-keys-of (vals marked)))])
    (check "the mark holds from the batch after: the fact of the mark's own batch is in no run, and its stamp is not later than the key's stamp"
           [[] true] [after-marking (<= (:stamp in-fact) end-of-marking)])
    (check "a fact under that key in a later batch is accepted, and is in the order" [:accepted [e3]] [(:word later) after-later])
    (check "two writes of who may see the base in one batch, from two tasks: both accepted, and every task holds one row, the later turn's"
           [:accepted :accepted tasks #{(nth (:act-id (:thing later-of-the-two)) 2)}]
           [(:word see-a) (:word see-b) (count see-rows) (set (map :by-act (vals see-rows)))])
    (shown "two acts of one batch on one place under a marked key:" (told first-on-one-place) "|" (told second-on-one-place)
           "| the runs of the value before, of the first act's value and of the second's:" (pr-str (vec (take 3 after-two))))
    (shown "a mark and a fact under its key in one batch: the fact's stamp" (:stamp in-fact) "is not later than the key's stamp" end-of-marking
           ", and the fact is in no run:" (pr-str after-marking) "| a fact of a later batch is:" (pr-str after-later))
    (shown "two writes of who may see the base in one batch, arriving on tasks" (task-of-entity e1) "and the task of the layer's entity: every task holds the row of"
           (short-id (:act-id (:thing later-of-the-two))))))

(defn- base-snapshot
  "What the proof reads of the base before and after the module is started
  again: the answer of the act that landed, the depot, the gate's last
  stamp, the count of strays, a permission's row on each task, the marked
  keys on each task with their stamps, the run of one value in the facts by
  value, the row of a value that was not given in its normal form and what
  a reader gets from the row of another, and the row of the kept clock of
  the newest batch."
  []
  {:answer (answer-of (:act-id (:thing (:base-landed @kept))))
   :depot (depot-of-layer :base)
   :last-stamp (gates-last-stamp)
   :kept-clock (kept-clock-row)
   :strays (base-strays)
   :permission (into (sorted-map) (map (fn [[task row]] [task (:stamp row)])) (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission)))
   :marked-keys (into (sorted-map) (map (fn [[task row]] [task (record/marked-keys-of row)])) (base-marks-rows))
   :run (let [{:keys [k since value]} (:base-run @kept)] (base-entries-of k since value))
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
        before-the-append (base-fact e :text)
        ack (append-to-base! replacing)
        deadline (+ (System/currentTimeMillis) time-limit-ms)
        seen (loop [seen [before-the-append]]
               (let [row (base-fact e :text)
                     seen (conj seen row)]
                 (if (or (= new-uuid (:by-act row)) (< deadline (System/currentTimeMillis)))
                   seen
                   (do (Thread/sleep 2) (recur seen)))))
        read-at-first-sight (answer-of (:act-id replacing))
        _ (wait-until-processed!)
        result (base-result replacing ack)
        _ (note-base-answers! [result])
        after-closing (base-fact e :text)]
    (swap! kept assoc :base-before (base-snapshot) :base-log-before (answer-log :microbatch))
    (check "a fact with a value is written to a place of the base, and an act that names it replaces it: both accepted"
           [:accepted :accepted true] [(:word written) (:word result) (:mine? result)])
    (check "from before the append of the replacing act until its row shows, each select of the place, straight from the facts table on the task of its entity, gave a row and never nothing"
           [true true] [(< 2 (count seen)) (every? some? seen)])
    (check "each row that a select gave was the fact before or the fact after, with the text of the value before or after, the first select the one before and the last the one after"
           [#{[old-uuid "\"the text before\""] [new-uuid "\"the text after\""]} old-uuid new-uuid]
           [(set (map (juxt :by-act :value) seen)) (:by-act (first seen)) (:by-act (peek seen))])
    (check "the row that the select gave at its first sight of the new fact is the row that the place holds after the batch is closed, in every field"
           (content after-closing) (content (peek seen)))
    (shown "a place of the base that is replaced never gives nothing to a select that goes straight to the table:"
           (count seen) "selects from before the append until the new row showed," (count (filter #(= old-uuid (:by-act %)) seen))
           "gave the fact before and" (count (filter #(= new-uuid (:by-act %)) seen)) "the fact after | at the first sight of the new row the door's read by act-id gave"
           (if read-at-first-sight "its answer: the batch was closed by then" "nothing yet: the batch was not closed")
           "| the row seen then is the row after the closing")))

(deftest after-the-module-is-started-again
  (let [landed (:landed @kept)
        accepted (:act-id (:thing landed))
        row-before (fact-row :canvas :kept :as-given)
        stamp-before (last-stamp :alice)
        strays-before (strays :act-id-malformed)
        log-before (answer-log :streaming)
        _ (rtest/update-module! (:ipc *hand*) module/Store)
        log-after (answer-log :streaming)
        entries-by-act-id (fn [log] (into {} (map (fn [[act-id found]] [act-id (set (map :entry found))]))
                                          (group-by (comp :act-id :entry) (logged-answers log))))
        entries-before (entries-by-act-id log-before)
        entries-after (entries-by-act-id log-after)
        answer-after (answer-of accepted)
        repeat-ack (send! (:thing landed))
        stamp-after-repeat (last-stamp :alice)
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
    (check "the answers' entries are in the streaming gate's log of answers after the start: each of the more than 100 act-ids that had an entry before has, before and after, one answer under it, the same, however many copies hold it"
           [true []]
           [(< 100 (count entries-before))
            (vec (keep (fn [[act-id entries]] (when-not (and (= 1 (count entries)) (= entries (get entries-after act-id))) (short-id act-id)))
                       entries-before))])
    (check "a repeat after it is answered from the record: the task's last stamp is the one from before"
           [:acked stamp-before] [repeat-ack stamp-after-repeat])
    (check "the depot and the strays are as they were" [:streaming strays-before] [(depot-of-layer :alice) (strays :act-id-malformed)])
    (check "an act with a value after the start is accepted, and its row holds the text of the value"
           [:accepted "\"a value after the start\""] [(:word value-after) (:value (fact-row :alice :note-40 :text))])
    (check "the first stamp after is later than the last stamp before" true (> (:stamp (:answer value-after)) stamp-before))
    (check "a permission that was given before stands after" :accepted (:word by-bob))
    (shown "after the module is started again in the cluster, the record is as it was: the same answer, the same row of a value;"
           "the last stamp before" stamp-before "the first stamp after" (:stamp (:answer value-after)))
    (shown "the streaming gate's log of answers held" (count log-before) "records before the start and" (count log-after)
           "right after it: a start that is no failure ran" (- (count log-after) (count log-before))
           "records again by then, counted and not held to a number | act-ids with an entry:" (count entries-before))))

(deftest the-stamps-and-the-depot
  (let [hands-append (let [ack (client-append (:streaming-depot *hand*) 5)]
                       (swap! things-sent conj 5)
                       ack)
        records (vec (stored))
        partition-of (into {} (keep (fn [[p r]] (when (and (map? r) (contains? r :layer)) [(:layer r) p]))) records)
        made [:alice :team :carrier]
        by-partition (group-by #(get partition-of (:layer %)) @first-answers)
        sent-digests (frequencies (map digest/hex-of @things-sent))
        logged-digests (frequencies (map (fn [[_ r]] (digest/hex-of r)) records))
        sent-values (values-in @things-sent)
        logged-values (values-in (map second records))
        answer-records (answer-log :streaming)
        logged (logged-answers answer-records)
        answered (answered-of @things-sent :streaming)
        answered-ids (set (map first answered))
        forged {:answers [{:act-id [:alice :streaming (UUID/randomUUID)] :stamp 1 :digest (byte-array 32)}]}
        forged-append (client-append (:streaming-answer-depot *hand*) forged)
        records-after-append (answer-log :streaming)
        code (source-of-the-store)
        two-files ["src-store/softland/store/door.clj" "src-store/softland/store/read_path.clj"]]
    (check "the stamps of the answers increase strictly on each task, in the order in which the acts were sent"
           true (every? (fn [[_ answers]] (apply < (map :stamp answers))) by-partition))
    (check "each stamp is below 2^62 and holds a clock time of today"
           true (every? #(and (< (:stamp %) act/max-carried-stamp)
                              (< (- (System/currentTimeMillis) 3600000) (stamp/time-of (:stamp %)) (+ (System/currentTimeMillis) 60000)))
                        @first-answers))
    (check "the depot holds each thing that was sent: accepted, refused, repeated and stray" (count @things-sent) (count records))
    (check "the layers of the proof are on more than one task" true (< 1 (count (set (vals partition-of)))))
    (check "each thing is on the depot as it was sent: the digests that the proof takes of the records read back are the digests of the things sent, each as often"
           [] (counts-apart sent-digests logged-digests #(subs % 0 16)))
    (check "each value is plain on the depot: every value that a fact of a thing sent holds is in the log under its act-id, its entity and its key, as it was given; the hand sent more than 300 of them"
           [true []] [(< 300 (reduce + (vals sent-values))) (counts-apart sent-values logged-values #(subvec % 0 3))])
    (shown "the stamps of" (count @first-answers) "answers increase strictly on each task; answers by partition:"
           (pr-str (into (sorted-map) (map (fn [[p answers]] [p (count answers)])) by-partition)))
    (check "each answer's row of the streaming gate has its entry in the log of answers: for each of the more than 100 act-ids that have an answer there is at least one entry, and each entry under it is the act-id with the stamp and the digest that the read by act-id gives, the word when it is a refusal, and no other part, so no value of a fact; every entry under one act-id is the same"
           [true []] [(< 100 (count answered)) (entries-apart logged answered)])
    (check "no entry is under an act-id that has no answer; and each entry is on the partition of its layer's task, where the depot of acts holds the act"
           [[] []]
           [(vec (distinct (keep (fn [{:keys [entry]}] (when-not (contains? answered-ids (:act-id entry)) (short-id (:act-id entry)))) logged)))
            (vec (distinct (keep (fn [{:keys [partition entry]}]
                                   (when-not (= partition (get partition-of (first (:act-id entry)))) (short-id (:act-id entry))))
                                 logged)))])
    (check "a record of the streaming gate's depot of answers holds one answer's entry or one stray's, and no other part; and each stray's entry is on the partition of its task"
           [#{[#{:answers} 1] [#{:strays} 1]} []]
           [(set (map (fn [[_ r]] [(set (keys r)) (+ (count (:answers r)) (count (:strays r)))]) answer-records))
            (vec (keep (fn [{:keys [partition entry]}] (when-not (= partition (:task entry)) entry)) (logged-strays answer-records)))])
    (check "a client's append to the streaming gate's depot of answers is refused, and the depot holds no record more for it; the same call, made first, appended a thing to the depot of acts"
           [:appended :refused (count answer-records)]
           [hands-append forged-append (count records-after-append)])
    (check "in the code of the store's source, strings and comments left out, each depot of answers is named twice, in its gate's file and no other: where it is declared, closed to a client's append, and where it is appended to"
           [{"streaming_gate.clj" 2} {"streaming_gate.clj" 1} {"streaming_gate.clj" 1}
            {"microbatch_gate.clj" 2} {"microbatch_gate.clj" 1} {"microbatch_gate.clj" 1}]
           [(named-in code #"\*streaming-answer-depot")
            (named-in code #"\(declare-depot\s+\S+\s+\*streaming-answer-depot\s+:disallow\)")
            (named-in code #"\(depot-partition-append!\s+\*streaming-answer-depot\s")
            (named-in code #"\*microbatch-answer-depot")
            (named-in code #"\(declare-depot\s+\S+\s+\*microbatch-answer-depot\s+:disallow\)")
            (named-in code #"\(depot-partition-append!\s+\*microbatch-answer-depot\s")])
    (check "no topology of the store reads a depot of answers, and neither the door nor the read path: no `source>` names one, and neither `door.clj` nor `read_path.clj` holds the word answer-depot, in its code or in a docstring; the same search finds the `source>` of each gate's depot of acts, and the word depot in each of the two files"
           [{} {"streaming_gate.clj" 1 "microbatch_gate.clj" 1} [false false] [true true]]
           [(named-in code #"source>[^()]*answer-depot")
            (named-in code #"\(source>\s+\*(streaming|microbatch)-depot\s")
            (mapv #(str/includes? (slurp %) "answer-depot") two-files)
            (mapv #(str/includes? (slurp %) "depot") two-files)])
    (shown "layers by depot partition:" (pr-str (select-keys partition-of made)) "| the depot holds all" (count records)
           "things sent, each as it was sent | each of the" (reduce + (vals sent-values)) "values that the hand sent is plain on it")
    (shown "the streaming gate's log of answers:" (count answer-records) "records, with" (count logged) "answers' entries for" (count answered)
           "act-ids that have an answer, each entry the answer's, and" (count (logged-strays answer-records)) "strays' entries"
           "| a client's append to it:" forged-append)
    (shown "where the store's code names the depots of answers:" (pr-str (named-in code #"\*streaming-answer-depot"))
           (pr-str (named-in code #"\*microbatch-answer-depot")) "| a source> of one:" (pr-str (named-in code #"source>[^()]*answer-depot")))))

(deftest the-base-after-the-module-is-started-again
  (let [before (:base-before @kept)
        landed (:base-landed @kept)
        after (base-snapshot)
        _ (append-to-base! (:thing landed))
        _ (wait-until-processed!)
        answer-after-repeat (answer-of (:act-id (:thing landed)))
        value-after (decide-in-base! (system-act [(value-fact :after-doc :text "a value after the start")]))
        by-alice (decide-in-base! (thing :base :alice alice-in-base [{:e :alices-doc :k :after}]))
        clock-before (:kept-clock before)
        clock-after (kept-clock-row)
        {:keys [places act-uuid]} (:base-values @kept)
        log-before (:base-log-before @kept)
        log-after (answer-log :microbatch)
        entries-by-act-id (fn [log] (into {} (map (fn [[act-id found]] [act-id (set (map :entry found))]))
                                          (group-by (comp :act-id :entry) (logged-answers log))))
        entries-before (entries-by-act-id log-before)
        entries-after (entries-by-act-id log-after)]
    (check "the answer, the depot of the base, the gate's last stamp, the strays, a permission's rows, the marked keys, a run of the facts by value, the row of a value and what a reader gets from another, and the kept clock of the newest batch are as they were"
           before after)
    (check "the row of a value from before holds the text of the value's normal form still" "{:a [1 2] :b 1.5M}" (:value (:kept-row after)))
    (check "the kept clock is one row of a batch's id and a clock; a batch with things after the start keeps its clock under a later id, and the clock is not earlier"
           [true true true true]
           [(clock-record? clock-before) (clock-record? clock-after)
            (boolean (and clock-before clock-after (< (:batch clock-before) (:batch clock-after))))
            (boolean (and clock-before clock-after (<= (:clock clock-before) (:clock clock-after))))])
    (check "the answers' entries are in the microbatch gate's log of answers after the start: each of the more than 300 act-ids that had an entry before has, before and after, one answer under it, the same, however many copies hold it"
           [true []]
           [(< 300 (count entries-before))
            (vec (keep (fn [[act-id entries]] (when-not (and (= 1 (count entries)) (= entries (get entries-after act-id))) (short-id act-id)))
                       entries-before))])
    (check "a repeat after it is answered from the record" (:answer before) answer-after-repeat)
    (check "an act with a value after the start is accepted in the base, and its row holds the text of the value"
           [:accepted "\"a value after the start\""] [(:word value-after) (:value (base-fact :after-doc :text))])
    (check "the first stamp after is later than the gate's last stamp before" true (< (:last-stamp before) (:stamp (:answer value-after))))
    (check "a permission that the system gave before stands after" :accepted (:word by-alice))
    (check "each value of the act on several tasks is in its row still, as the text of what was sent, written by that act"
           (mapv (fn [[e _]] [(str "\"the body of " (name e) "\"") act-uuid]) places)
           (mapv (fn [[e k]] ((juxt :value :by-act) (base-fact e k))) places))
    (shown "after the module is started again in the cluster, the base's record is as it was: the same answer, depot" (:depot after)
           "| the same row of a value | the gate's last stamp before" (:last-stamp before) "the first stamp after" (:stamp (:answer value-after)))
    (shown "the microbatch gate's log of answers held" (count log-before) "records before the start and" (count log-after)
           "after it and the batches since, with" (reduce + (map count (vals entries-after))) "different answers under" (count entries-after)
           "act-ids: whether a start that is no failure appends copies is counted and not held to a number")
    (check "the gate's one throw, called by hand: given the clock that is kept for a batch it gives that clock; given none it throws and names the batch, so that no stamp is made from a clock that is not kept"
           [(:clock clock-after) [:threw {:batch 41}]]
           [(microbatch-gate/clock-must-be-kept (:clock clock-after) (:batch clock-after))
            (try (microbatch-gate/clock-must-be-kept nil 41)
                 (catch clojure.lang.ExceptionInfo e [:threw (ex-data e)]))])
    (shown "the kept clock of the newest batch is as it was after the start:" (pr-str clock-before)
           "| a batch with things after it kept its clock under a later id:" (pr-str clock-after)
           "| the gate's one throw, called by hand with no clock kept, throws and names the batch; no run of this proof reached it in the gate")))

(deftest the-stamps-and-the-depot-of-the-base
  (let [records (vec (stored-in-base))
        groups @base-answers
        answers (vec (apply concat groups))
        sent-digests (frequencies (map digest/hex-of @base-things-sent))
        logged-digests (frequencies (map (fn [[_ r]] (digest/hex-of r)) records))
        sent-values (values-in @base-things-sent)
        logged-values (values-in (map second records))
        reads (:base-reads @kept)
        clocks (clock-records)
        answer-records (answer-log :microbatch)
        logged (logged-answers answer-records)
        answered (answered-of @base-things-sent :microbatch)
        answered-ids (set (map first answered))
        batches-with-an-answer (set (map :batch logged))
        of-itself (fn [batch] (set (keep (fn [[_ r]] (when (and (= batch (:batch r)) (or (contains? r :clock) (contains? r :last-stamp)))
                                                       (select-keys r [:clock :last-stamp])))
                                         answer-records)))
        clocks-by-batch (reduce (fn [by-batch [_ r]] (update by-batch (:batch r) (fnil conj #{}) (:clock r))) {} clocks)
        last-stamp-at-rest (gates-last-stamp)
        to-keep-at-rest (to-keep-by-task)
        to-close-at-rest (to-close-by-task)
        forged {:batch 1 :answers [{:act-id [:base :microbatch (UUID/randomUUID)] :stamp 1 :digest (byte-array 32)}]}
        forged-append (client-append (:mb-answer-depot *hand*) forged)
        counts-before-quiet [(count (answer-log :streaming)) (count (answer-log :microbatch))]
        _ (Thread/sleep quiet-ms)
        counts-after-quiet [(count (answer-log :streaming)) (count (answer-log :microbatch))]
        not-closed-at-the-read (remove (fn [{:keys [stamp closed-through-after]}]
                                         (and closed-through-after (<= stamp closed-through-after)))
                                       reads)
        not-closed (remove (fn [{:keys [act-id stamp]}] (<= stamp (or (closed-through (nth act-id 2)) -1))) answers)]
    (check "the stamps of the microbatch gate's answers go up in the order decided: each batch's stamps are later than those of the batch before"
           true (every? (fn [[earlier later]] (< (reduce max (map :stamp earlier)) (reduce min (map :stamp later)))) (partition 2 1 groups)))
    (check "no two answers of the gate share a stamp" true (apply distinct? (map :stamp answers)))
    (check "each stamp is below 2^62 and holds a clock time of today"
           true (every? #(and (< (:stamp %) act/max-carried-stamp)
                              (< (- (System/currentTimeMillis) 3600000) (stamp/time-of (:stamp %)) (+ (System/currentTimeMillis) 60000)))
                        answers))
    (check "each answer that a read gave was of a closed batch at the read: its stamp is not above the closed-through that the hand selected on its task right after the read"
           [true []] [(<= (count answers) (count reads)) (vec not-closed-at-the-read)])
    (check "and at the end of the run no answer is left in a batch that is not closed" [] (vec not-closed))
    (check "the microbatch depot holds each thing that was sent to it: accepted, refused, repeated and stray"
           (count @base-things-sent) (count records))
    (check "the things of the base are on more than one partition of the depot" true (< 1 (count (set (map first records)))))
    (check "each thing is on the microbatch depot as it was sent: the digests that the proof takes of the records read back are the digests of the things sent, each as often"
           [] (counts-apart sent-digests logged-digests #(subs % 0 16)))
    (check "each value is plain on the microbatch depot: every value that a fact of a thing sent holds is in the log under its act-id, its entity and its key, as it was given; the hand sent more than 300 of them"
           [true []] [(< 300 (reduce + (vals sent-values))) (counts-apart sent-values logged-values #(subvec % 0 3))])
    (check "the depot of batch clocks holds records, each a map of exactly a batch's id and a clock, two integers, all on one partition"
           [true true 1]
           [(< 30 (count clocks)) (every? (fn [[_ r]] (clock-record? r)) clocks) (count (set (map first clocks)))])
    (check "each answer's row of the base has its entry in the microbatch gate's log of answers: for each of the more than 300 act-ids that have an answer there is at least one entry, and each entry under it is the act-id with the stamp and the digest that the read by act-id gives, the word when it is a refusal, and no other part, so no value of a fact; every entry under one act-id is the same; and no entry is under an act-id that has no answer"
           [true [] []]
           [(< 300 (count answered)) (entries-apart logged answered)
            (vec (distinct (keep (fn [{:keys [entry]}] (when-not (contains? answered-ids (:act-id entry)) (short-id (:act-id entry)))) logged)))])
    (check "every record of that depot holds its batch's id, an integer, and beside it answers, strays, a clock and a last stamp, and no other part"
           [true #{}]
           [(every? (fn [[_ r]] (integer? (:batch r))) answer-records)
            (set (remove #{:batch :answers :strays :clock :last-stamp} (mapcat (fn [[_ r]] (keys r)) answer-records)))])
    (check "each of the more than 30 batches whose records hold an answer keeps of itself one clock and one last stamp, the same wherever a record of it holds them; and the clock is the clock of a record of that batch on the depot of batch clocks"
           [true []]
           [(< 30 (count batches-with-an-answer))
            (vec (keep (fn [batch]
                         (let [kept-of-itself (of-itself batch)
                               {:keys [clock last-stamp]} (first kept-of-itself)]
                           (when-not (and (= 1 (count kept-of-itself)) (integer? last-stamp) (contains? (get clocks-by-batch batch #{}) clock))
                             batch)))
                       batches-with-an-answer))])
    (check "the greatest last stamp that a record of the log holds is the gate's last stamp"
           last-stamp-at-rest (reduce max (keep (fn [[_ r]] (:last-stamp r)) answer-records)))
    (check "on the store at rest the microbatch gate's `:to-keep` holds no key on any task, and `:to-close` is absent on every task"
           [(vec (repeat tasks 0)) (vec (repeat tasks nil))] [to-keep-at-rest to-close-at-rest])
    (check "a client's append to the microbatch gate's depot of answers is refused; and a quiet time of 1.5 s adds no record to either depot of answers, the refused record among them"
           [:refused [(count (answer-log :streaming)) (count answer-records)] counts-before-quiet]
           [forged-append counts-before-quiet counts-after-quiet])
    (check "the row of the kept clock is the first record, in the depot's order, of the batch with the greatest id on that depot"
           (let [newest (reduce max (map (fn [[_ r]] (:batch r)) clocks))]
             (some (fn [[_ r]] (when (= newest (:batch r)) r)) clocks))
           (kept-clock-row))
    (shown "the stamps of" (count answers) "answers of the microbatch gate, in" (count groups) "batches, go up in the order decided;"
           "each of" (count reads) "reads that gave an answer gave one of a batch that was closed at the read")
    (shown "the microbatch depot holds all" (count records) "things sent, on" (count (set (map first records)))
           "partitions, each as it was sent | each of the" (reduce + (vals sent-values)) "values that the hand sent is plain on it")
    (shown "the depot of batch clocks holds" (count clocks) "records, each a batch's id and a clock and nothing else, for"
           (count (set (map (fn [[_ r]] (:batch r)) clocks))) "batches, all on partition" (pr-str (set (map first clocks)))
           "| the kept row is the newest batch's:" (pr-str (kept-clock-row)))
    (shown "the microbatch gate's log of answers:" (count answer-records) "records of" (count (set (map (fn [[_ r]] (:batch r)) answer-records)))
           "batches, with" (count logged) "answers' entries for" (count answered) "act-ids that have an answer, each entry the answer's, and"
           (count (logged-strays answer-records)) "strays' entries |" (count batches-with-an-answer)
           "batches keep a clock and a last stamp; the greatest last stamp is the gate's:" last-stamp-at-rest)
    (shown "the store at rest: keys of :to-keep by task" (pr-str to-keep-at-rest) "| :to-close by task" (pr-str to-close-at-rest)
           "| a client's append to the microbatch gate's depot of answers:" forged-append
           "| records of the two depots of answers before and after" quiet-ms "ms with nothing sent:" (pr-str counts-before-quiet) (pr-str counts-after-quiet))))

;; ------------------------------------- a batch that Rama runs a second time

(def ^:private first-base
  "`softland.store.gate.batch/base` as the store defines it, taken when this
  namespace was loaded, before any var is changed."
  batch/base)

(def ^:private first-decide-or-send
  "`softland.store.gate.batch/decide-or-send` as the store defines it."
  batch/decide-or-send)

(def ^:private first-rows-to-send
  "`softland.store.gate.batch/rows-to-send` as the store defines it."
  batch/rows-to-send)

(defonce ^{:private true
           :doc "The note of each attempt of a batch with things on the proof's
  second cluster, in the order in which the attempts began:
  {:wall :clock :last-stamp :numbers :base :rows}. `:rows` holds each row that
  the attempt made, as [which key row], in the order noted."}
  attempts (atom []))

(defonce ^{:private true
           :doc "The wish for a throw: true while the next call of
  `noting-rows-to-send` that gives rows is to throw."}
  throw-asked (atom false))

(defonce ^{:private true
           :doc "Each throw that `noting-rows-to-send` made:
  {:wall :rows :attempts :thread}, where :attempts is how many attempts were
  noted when it threw."}
  throws-made (atom []))

(def ^:private first-records-of-closing
  "`softland.store.gate.batch/records-of-closing` as the store defines it."
  batch/records-of-closing)

(def ^:private first-marks-places-asked
  "`softland.store.gate.batch/marks-places-asked` as the store defines it."
  batch/marks-places-asked)

(def ^:private first-writes
  "`softland.store.gate.decision/writes` as the store defines it."
  decision/writes)

(def ^:private first-answer-record
  "`softland.store.record.rows/answer-record` as the store defines it."
  record/answer-record)

(defonce ^{:private true
           :doc "What the proof asked of a closing on its second cluster, and what
  came of it: {:asked :noted :throws}. `:asked` is true while the next
  closing that gives records is to fail behind its appends; `:noted` is
  true from the call that gave those records until the throw; `:throws`
  holds each throw that was made, {:wall :thread}."}
  closing-failure (atom {:asked false :noted false :throws []}))

(defonce ^{:private true
           :doc "The events of the streaming gate that the proof makes fail on its
  second cluster: {:before-the-commit :behind-the-commit :calls}. The first
  holds the act-id whose decision is to throw before its rows are written,
  or nil; the second the act-id whose entry is to throw behind the commit
  of its rows, or nil; `:calls` holds each call that a stand-in got for one
  of the two, {:where :wall :thread}, in the order made. `:where` is
  :before-the-commit for a call of `writes` for the first act-id, whose
  first throws; :behind-the-commit for a call of `answer-record` for the
  second, whose first throws; and :decision for a call of `writes` for the
  second, which never throws and says that a run decided that act."}
  event-failures (atom {:before-the-commit nil :behind-the-commit nil :calls []}))

(defn noting-records-of-closing
  "Stands in for `softland.store.gate.batch/records-of-closing` on the
  proof's second cluster. Takes what it takes and gives what it gives. The
  keeping task of a batch calls it in the run that closes the batch, right
  before that run's appends to the depot of answers. When the proof asked
  for a failed closing and this call gave records, it notes that a closing
  has its records. It is public and held by a var, and it closes over
  nothing."
  [to-close last-stamp kept]
  (let [out (first-records-of-closing to-close last-stamp kept)]
    (when (seq out)
      (swap! closing-failure (fn [state] (if (:asked state) (assoc state :noted true) state))))
    out))

(defn throwing-marks-places-asked
  "Stands in for `softland.store.gate.batch/marks-places-asked` on the
  proof's second cluster. Takes what it takes and gives what it gives.
  Every task calls it in step 2 of each run, behind the closing and its
  appends. When the note of a closing is there, the first call takes the
  note and the wish away and throws, this once: the run that closes a batch
  then fails behind its appends, and Rama runs it again."
  [asked]
  (let [[before _] (swap-vals! closing-failure
                               (fn [state]
                                 (if (:noted state)
                                   (-> state
                                       (assoc :asked false :noted false)
                                       (update :throws conj {:wall (System/currentTimeMillis)
                                                             :thread (.getName (Thread/currentThread))}))
                                   state)))]
    (when (:noted before)
      (throw (ex-info "the module's proof: a forced failure of the run that closes a batch, behind its appends" {}))))
  (first-marks-places-asked asked))

(defn throwing-writes
  "Stands in for `softland.store.gate.decision/writes` on the proof's second
  cluster. Takes what it takes and gives what it gives. The streaming gate
  calls it in the first part of an event, behind the event's reads and
  before its first write, and only for a thing that it decides. It notes
  each call for one of the two act-ids that the proof marked. The first
  call for the act-id that is to fail before its commit throws: that event
  fails with no row written. A call for the other act-id never throws: it
  is noted so that the proof can count the runs that decided that act."
  [act-id act refused stamp digest in-hand]
  (let [fails? (fn [state] (and (some? (:before-the-commit state)) (= act-id (:before-the-commit state))))
        watched? (fn [state] (and (some? (:behind-the-commit state)) (= act-id (:behind-the-commit state))))
        note (fn [where] {:where where :wall (System/currentTimeMillis) :thread (.getName (Thread/currentThread))})
        [before _] (swap-vals! event-failures
                               (fn [state]
                                 (cond (fails? state) (update state :calls conj (note :before-the-commit))
                                       (watched? state) (update state :calls conj (note :decision))
                                       :else state)))]
    (when (and (fails? before) (not-any? #(= :before-the-commit (:where %)) (:calls before)))
      (throw (ex-info "the module's proof: a forced failure of an event of the streaming gate, before its commit" {}))))
  (first-writes act-id act refused stamp digest in-hand))

(defn throwing-answer-record
  "Stands in for `softland.store.record.rows/answer-record` on the proof's
  second cluster. Takes what it takes and gives what it gives. The
  streaming gate calls it in the second part of an event, behind the commit
  of the event's rows and before the append of the entry, for a decision and
  for an act-id that it found answered; the microbatch gate's closing calls
  it too, for other act-ids. It notes each call for the act-id that the
  proof marked, and the first of those calls throws: that event fails behind
  its commit, with its answer's row committed and no entry appended."
  [gate-depot layer act-uuid row]
  (let [marked? (fn [state] (and (some? (:behind-the-commit state)) (= [layer gate-depot act-uuid] (:behind-the-commit state))))
        [before _] (swap-vals! event-failures
                               (fn [state]
                                 (if (marked? state)
                                   (update state :calls conj {:where :behind-the-commit :wall (System/currentTimeMillis)
                                                              :thread (.getName (Thread/currentThread))})
                                   state)))]
    (when (and (marked? before) (not-any? #(= :behind-the-commit (:where %)) (:calls before)))
      (throw (ex-info "the module's proof: a forced failure of an event of the streaming gate, behind its commit and before its append" {}))))
  (first-answer-record gate-depot layer act-uuid row))

(def ^:private edge-ahead-ms
  "How far ahead of the hand's clock the stamp is that the edge act of the
  batch that runs twice carries, in milliseconds: the store's 10,000
  (`softland.store.gate.stamp/max-ahead-millis`) and 700. The act is at the
  edge when the first attempt takes its clock less than 700 ms after the
  hand's reading and the second attempt begins more than 700 ms after it;
  the runs of the plan and of its check gave 122 ms for the first and 1,001
  to 1,548 ms more for the second. The number changes what the check shows
  and never whether it passes."
  10700)

(def ^:private twice-limit-ms
  "How long the hand reads for the answers of the batch that runs twice, in
  milliseconds. A second attempt came 1 to 2 seconds after the first in the
  runs; an attempt that Rama ends by its phase timeout takes about 31."
  60000)

(defn- note-rows!
  "Adds rows, each [which key row], to the note of the newest attempt."
  [rows]
  (when (seq rows)
    (swap! attempts (fn [notes]
                      (if (seq notes)
                        (update-in notes [(dec (count notes)) :rows] into rows)
                        notes)))))

(defn noting-base
  "Stands in for `softland.store.gate.batch/base` on the proof's second
  cluster. Takes what it takes and gives what it gives. The gate calls it
  once in each attempt, on the gate's task; for a batch with numbers it
  begins the note of an attempt: the clock and the last stamp that the gate
  gave it, the numbers, the base, and the wall time of the call. It is
  public and held by a var, and it closes over nothing: Rama takes the
  function that the store's var holds at the launch."
  [clock last-stamp numbers]
  (let [base (first-base clock last-stamp numbers)]
    (when numbers
      (swap! attempts conj {:wall (System/currentTimeMillis) :clock clock :last-stamp last-stamp
                            :numbers numbers :base base :rows []}))
    base))

(defn noting-decide-or-send
  "Stands in for `softland.store.gate.batch/decide-or-send` on the proof's
  second cluster. Takes what it takes and gives what it gives, and notes
  each row that it gave: each fact that the task writes at once, and each
  row of a parcel of the kind :rows, a row of the kind :by-value that is nil
  among them."
  [item own-rows]
  (let [out (first-decide-or-send item own-rows)]
    (note-rows! (concat (map (fn [[k row]] [:fact k row]) (:now out))
                        (mapcat (fn [[_ kind payload]] (when (= :rows kind) payload)) (:parcels out))))
    out))

(defn noting-rows-to-send
  "Stands in for `softland.store.gate.batch/rows-to-send` on the proof's
  second cluster. Takes what it takes and gives what it gives, and notes
  each row: the rows of the acts that meet. Then, when the proof asked for a
  throw and this call gave rows, it throws, this once. Step 4 calls it on
  every task, and only the meeting task of a layer gets rows from it; step 3
  is done on every task by then. So when every act of a batch is into one
  layer, the throw comes after the one call that gives rows, and the note of
  the attempt is whole by the order of the code, wherever the tasks sit."
  [pass]
  (let [out (first-rows-to-send pass)]
    (note-rows! (map (juxt :which :key :row) out))
    (when (and (seq out) (compare-and-set! throw-asked true false))
      (swap! throws-made conj {:wall (System/currentTimeMillis) :rows (count out) :attempts (count @attempts)
                               :thread (.getName (Thread/currentThread))})
      (throw (ex-info "the module's proof: a forced failure of this attempt of a batch, after its rows were noted" {})))
    out))

(defn- put-noting-functions!
  "Puts the seven stand-ins into the vars of the store's seven functions.
  What must hold: no cluster of this JVM is launched; a launch takes what
  the vars hold then, and a replacement made behind a launch is not taken."
  []
  (alter-var-root #'batch/base (constantly noting-base))
  (alter-var-root #'batch/decide-or-send (constantly noting-decide-or-send))
  (alter-var-root #'batch/rows-to-send (constantly noting-rows-to-send))
  (alter-var-root #'batch/records-of-closing (constantly noting-records-of-closing))
  (alter-var-root #'batch/marks-places-asked (constantly throwing-marks-places-asked))
  (alter-var-root #'decision/writes (constantly throwing-writes))
  (alter-var-root #'record/answer-record (constantly throwing-answer-record)))

(defn- put-the-stores-functions-back!
  "Puts the store's own seven functions back into their vars."
  []
  (alter-var-root #'batch/base (constantly first-base))
  (alter-var-root #'batch/decide-or-send (constantly first-decide-or-send))
  (alter-var-root #'batch/rows-to-send (constantly first-rows-to-send))
  (alter-var-root #'batch/records-of-closing (constantly first-records-of-closing))
  (alter-var-root #'batch/marks-places-asked (constantly first-marks-places-asked))
  (alter-var-root #'decision/writes (constantly first-writes))
  (alter-var-root #'record/answer-record (constantly first-answer-record)))

(defn- empty-the-hand!
  "Empties each atom of the proof but the shown lines: what the hand keeps
  of one cluster means nothing on the next, and Rama counts the things that
  a gate processed from the launch of a cluster."
  []
  (reset! things-sent [])
  (reset! first-answers [])
  (reset! kept {})
  (reset! base-things-sent [])
  (reset! base-answers [])
  (reset! attempts [])
  (reset! throw-asked false)
  (reset! throws-made [])
  (reset! closing-failure {:asked false :noted false :throws []})
  (reset! event-failures {:before-the-commit nil :behind-the-commit nil :calls []}))

(defn- decide-twice!
  "The hand for a second attempt. Sends things to the microbatch depot so
  that one batch of the gate takes them all, as `decide-in-one-batch!` does,
  with a throw asked for: the first attempt of that batch fails in step 4,
  after the one call of `rows-to-send` that gives rows, and Rama runs the
  batch again. Takes the things, and a function of no argument that makes
  the last thing: the hand calls it right before it appends that thing, and
  lets the topology go on at once, so that the last thing can carry a
  reading of the clock that is fresh. Gives
  {:results :answered? :waited :notes :throws :last-thing}: the result of
  each thing in the order sent, the last thing last; whether each was
  answered within `twice-limit-ms`; the milliseconds from the topology's
  going on to the last answer; the notes of the attempts that began since
  the send; and the throws made since.

  It reads the answers in a loop that takes a read that throws as nothing
  yet: for about half a second after a worker is started again, a read can
  throw. What must hold: the hand is on the proof's second cluster, where
  the noting functions run, and every thing is an act into one layer."
  [xs make-last]
  (let [ipc (:ipc *hand*)
        module-name (:module *hand*)
        notes-before (count @attempts)
        throws-before (count @throws-made)]
    (rtest/pause-microbatch-topology! ipc module-name "microbatch-gate")
    (let [acks (mapv append-to-base! xs)
          _ (reset! throw-asked true)
          last-thing (make-last)
          last-ack (append-to-base! last-thing)
          _ (rtest/resume-microbatch-topology! ipc module-name "microbatch-gate")
          went-on (System/currentTimeMillis)
          all (conj (vec xs) last-thing)
          answer-or-nil (fn [x] (try (answer-of (:act-id x)) (catch Throwable _ nil)))
          answered? (loop []
                      (cond (every? answer-or-nil all) true
                            (< (+ went-on twice-limit-ms) (System/currentTimeMillis)) false
                            :else (do (Thread/sleep 50) (recur))))
          waited (- (System/currentTimeMillis) went-on)]
      (reset! throw-asked false)
      {:results (mapv (fn [x ack]
                        (let [answer (answer-or-nil x)]
                          {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
                           :word (cond (nil? answer) :nothing-yet
                                       (= :accepted (:verdict answer)) :accepted
                                       :else (:reason answer))}))
                      all (conj acks last-ack))
       :answered? answered?
       :waited waited
       :notes (subvec @attempts notes-before)
       :throws (subvec @throws-made throws-before)
       :last-thing last-thing})))

(defn- decide-with-a-failed-closing!
  "The hand for a closing that runs a second time. Sends things to the
  microbatch depot so that one batch of the gate takes them all, as
  `decide-in-one-batch!` does, with a failed closing asked for. The batch
  itself runs once. The next run of the gate, which closes it, appends the
  batch's records and then fails, on the first task that calls
  `marks-places-asked` behind the closing; Rama runs that run again. Gives
  {:results :answered? :waited :throws}: the result of each thing in the
  order sent; whether each thing that is no stray was answered within
  `twice-limit-ms`; the milliseconds from the topology's going on to the
  last answer; and the throws made since.

  It reads the answers in a loop that takes a read that throws as nothing
  yet. What must hold: the hand is on the proof's second cluster, where the
  stand-ins run; and the batch before is closed, so that the first closing
  that gives records is this batch's."
  [xs]
  (let [ipc (:ipc *hand*)
        module-name (:module *hand*)
        throws-before (count (:throws @closing-failure))]
    (rtest/pause-microbatch-topology! ipc module-name "microbatch-gate")
    (let [acks (mapv append-to-base! xs)
          _ (swap! closing-failure assoc :asked true :noted false)
          _ (rtest/resume-microbatch-topology! ipc module-name "microbatch-gate")
          went-on (System/currentTimeMillis)
          answer-or-nil (fn [x] (try (answer-of (:act-id x)) (catch Throwable _ nil)))
          acts (remove #(declaration/stray-reason :microbatch %) xs)
          answered? (loop []
                      (cond (every? answer-or-nil acts) true
                            (< (+ went-on twice-limit-ms) (System/currentTimeMillis)) false
                            :else (do (Thread/sleep 50) (recur))))
          waited (- (System/currentTimeMillis) went-on)]
      (swap! closing-failure assoc :asked false)
      {:results (mapv (fn [x ack]
                        (let [answer (when-not (declaration/stray-reason :microbatch x) (answer-or-nil x))]
                          {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
                           :word (cond (nil? answer) :nothing-yet
                                       (= :accepted (:verdict answer)) :accepted
                                       :else (:reason answer))}))
                      xs acks)
       :answered? answered?
       :waited waited
       :throws (subvec (:throws @closing-failure) throws-before)})))

(defn- send-and-watch!
  "The hand for an event of the streaming gate that fails. Appends one thing
  to the streaming depot on a thread of its own and, while Rama fails the
  thing's event and runs its record again, reads in a loop, one read right
  behind the other: the answer by act-id, then the entries under the act-id
  in the streaming gate's log of answers. A read that throws counts as no
  read. It stops when `done?` holds of the answers that a read found in the
  log, or at `twice-limit-ms`. Gives
  {:ack :sights :answer-with-no-entry? :entry-with-no-answer? :log}: what
  the append gave, or :no-ack-in-time when it did not come back ten seconds
  after the loop; each pair of reads that differs from the pair before, in
  the order read, as [was an answer read, the entries read right behind
  it]; the two findings below; and the records of the last read of the log
  that came back.

  An answer never goes away, and a log loses no entry. So a pair that reads
  an answer and then no entry says that the answer was readable while the
  log held no entry of it: `:answer-with-no-entry?`. Whether a pair meets
  that moment is a matter of the time of the reads, and no check rests on
  it. And a read that gives no answer behind a read of the log that found
  an entry would say that the log held an entry of an answer that no read
  gives: `:entry-with-no-answer?`."
  [x done?]
  (let [act-id (:act-id x)
        sent (future (send! x))
        deadline (+ (System/currentTimeMillis) twice-limit-ms)
        read-answer (fn [] (try (some? (answer-of act-id)) (catch Throwable _ nil)))
        read-log (fn [] (try (answer-log :streaming) (catch Throwable _ nil)))]
    (loop [sights []
           entry-read? false
           found {:answer-with-no-entry? false :entry-with-no-answer? false}
           last-records []]
      (let [answer? (read-answer)
            records (read-log)
            entries (when records (count (entries-under (logged-answers records) act-id)))
            pair [answer? entries]
            sights (if (and (some? answer?) (some? entries) (not= pair (peek sights))) (conj sights pair) sights)
            found (cond-> found
                    (and (true? answer?) (some? entries) (zero? entries)) (assoc :answer-with-no-entry? true)
                    (and entry-read? (false? answer?)) (assoc :entry-with-no-answer? true))
            entry-read? (or entry-read? (boolean (and entries (pos? entries))))
            last-records (or records last-records)]
        (if (or (and records (done? (logged-answers records))) (< deadline (System/currentTimeMillis)))
          (merge found {:ack (deref sent 10000 :no-ack-in-time) :sights sights :log last-records})
          (recur sights entry-read? found last-records))))))

(defn- rows-by-key
  "Takes the rows that an attempt noted, each [which key row]. Gives a map
  from [which key] to the rows noted under it, in the order noted, each with
  its bytes by content. A key that was noted with no row, as the key of a
  row of the facts by value that goes out is, holds a nil there: it is a
  key all the same."
  [rows]
  (reduce (fn [by-key [which k row]] (update by-key [which k] (fnil conj []) (content row))) {} rows))

(defn- fields-apart
  "Takes two rows, each a map or nil. Gives the fields that the two do not
  hold alike; [:row-or-no-row] when one of them is no row."
  [a b]
  (cond (= a b) []
        (or (nil? a) (nil? b)) [:row-or-no-row]
        :else (remove (fn [field] (= (get a field) (get b field))) (distinct (concat (keys a) (keys b))))))

(defn- rows-side-by-side
  "Sets the rows of two attempts side by side. Takes the notes of two
  attempts. Gives one entry for each key, [which key], whose rows are not
  the same in both: {:key :noted-in :differs}. :noted-in says whether the
  key is in the first note only, in the second only, or in both; for a key
  in both, :differs holds the fields in which its rows differ, a run of
  bytes compared by its content, or :how-many-rows. Gives an empty vector
  when both attempts made rows under the same keys, and at each key the same
  rows in every field. A key that was noted with no row is a key all the
  same, so a row that goes out in one attempt and is not noted in the other
  is told."
  [note-1 note-2]
  (let [first-rows (rows-by-key (:rows note-1))
        second-rows (rows-by-key (:rows note-2))]
    (vec (keep (fn [k]
                 (let [in-first (get first-rows k)
                       in-second (get second-rows k)]
                   (cond (nil? in-second) {:key k :noted-in :the-first-attempt-only}
                         (nil? in-first) {:key k :noted-in :the-second-attempt-only}
                         (= in-first in-second) nil
                         (not= (count in-first) (count in-second)) {:key k :noted-in :both :differs #{:how-many-rows}}
                         :else {:key k :noted-in :both :differs (set (mapcat fields-apart in-first in-second))})))
               (sort-by pr-str (distinct (concat (keys first-rows) (keys second-rows))))))))

(defn- as-the-tables-hold-it?
  "Takes a kind and a key as an attempt noted them, and the last row that
  the attempt noted under them, with its bytes by content. Selects the key
  on every task, straight from the gate's tables, and gives true when the
  tables hold that row and no other:

    :fact       one task holds the row, or every task for a row under one
                of the five store keys that sit on every task, each the same
    :answer     one task holds the row
    :by-value   one task holds the row; for a row that went out, no task
                holds a row under the key

  It gives false for a kind that is none of the three."
  [which k noted]
  (let [found (fn [pstate] (mapv content (vals (rows-by-task pstate k))))]
    (case which
      :fact (let [rows (found :mb-facts-ps)]
              (and (= (count rows) (if (batch/on-every-task? (nth (record/place-of-key k) 2)) tasks 1))
                   (every? #(= noted %) rows)))
      :answer (= [noted] (found :mb-answers-ps))
      :by-value (= (if noted [noted] []) (found :mb-by-value-ps))
      false)))

(deftest a-batch-runs-a-second-time
  (let [;; What the batch stands on, each in a batch that runs once: the base,
        ;; a marked key, a fact to replace, a value under the marked key, and
        ;; a person's permission.
        made (decide-in-base! (making :base :system :system))
        marking (decide-in-base! (system-act [{:e :base :k marks :v #{:on}}]))
        since (gates-last-stamp)
        [fact-before value-before give-alice]
        (decide-in-one-batch! [(system-act [(value-fact :twice-doc :title "the text before")])
                               (system-act [(value-fact :twice-pointer :on :canvas-1)])
                               (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}])])
        run-before (mapv first (base-entries-of :on since :canvas-1))
        note-of-the-batch-before (peek @attempts)
        ;; The batch that runs twice. Every act is into the base.
        fact-id [(:act-id (:thing fact-before)) 0]
        xs [(system-act [{:e :twice-1 :k :note}])
            (system-act [(value-fact :twice-2 :text "a plain value")])
            (system-act [{:e :twice-3 :k :note} (value-fact :twice-4 :text "a value on a second entity of one act")])
            (thing :base :alice alice-in-base [(value-fact :twice-5 :text "a person's text")])
            (system-act [(assoc (value-fact :twice-doc :title "the text after") :replaces fact-id)])
            (system-act [{:e :twice-doc :k :title}])
            (system-act [{:e :twice-holder :k :softland.store/permission :v [:twice-holder :base :base system-own]}])
            (system-act [{:e :twice-seer :k :softland.store/permission :v [:twice-seer :base :base system-own :see]}])
            (system-act [{:e :twice-8 :k :softland.store/lock-request :v 2}])
            (thing :base :mallory [:mallory :base :base system-own] [{:e :twice-6 :k :note}])
            (system-act [(value-fact :twice-7 :plain "a value under a key that no check before it wrote")])
            (system-act [(value-fact :twice-pointer :on :canvas-2)])
            (system-act [(value-fact :twice-pointer-2 :on :first-of-two)])
            (system-act [(value-fact :twice-pointer-2 :on :second-of-two)])
            (system-act [{:e :base :k marks :v #{:on :in}}])
            (system-act [{:e :base :k who-may-see :v :members}])
            (system-act [{:e :base :k builds-on :v [:team]}])]
        make-edge (fn [] (system-act [{:e :twice-edge :k :note}]
                                     {:based-on {fact-id (stamp/pack (+ (System/currentTimeMillis) edge-ahead-ms) 0)}}))
        {:keys [results answered? waited notes throws last-thing]} (decide-twice! xs make-edge)
        ;; Read at once, before another batch with things keeps its clock.
        kept-row (kept-clock-row)
        records (clock-records)
        notes-then (count @attempts)
        [note-1 note-2] notes
        the-throw (first throws)
        edge-carried (first (vals (:based-on last-thing)))
        edge-reading (- (stamp/time-of edge-carried) edge-ahead-ms)
        edge-word (when note-1
                    (if (stamp/too-far-ahead? (:clock note-1) edge-carried) :carried-stamp-too-far-ahead :accepted))
        at-the-edge? (boolean (and note-1 note-2
                                   (stamp/too-far-ahead? (:clock note-1) edge-carried)
                                   (not (stamp/too-far-ahead? (:wall note-2) edge-carried))))
        side-by-side (if (= 2 (count notes)) (rows-side-by-side note-1 note-2) :the-notes-are-not-two)
        first-rows (rows-by-key (:rows note-1))
        noted (rows-by-key (:rows note-2))
        kinds (frequencies (map first (keys noted)))
        not-as-noted (vec (keep (fn [[[which k] rows]] (when-not (as-the-tables-hold-it? which k (peek rows)) [which k])) noted))
        answers-apart (vec (keep (fn [{:keys [thing answer]}]
                                   (let [[layer _ uuid] (:act-id thing)
                                         row (peek (get noted [:answer (record/answer-key layer uuid)]))]
                                     (when-not (and row answer
                                                    (= [(if (:refused row) :refused :accepted) (:refused row) (:stamp row)]
                                                       [(:verdict answer) (:reason answer) (:stamp answer)]))
                                       (:act-id thing))))
                                 results))
        task-of-fact (fn [e k] (task-of-row :mb-facts-ps (record/place-key :base e k)))
        fact-tasks (set (map (fn [[e k]] (task-of-fact e k))
                             [[:twice-1 :note] [:twice-2 :text] [:twice-3 :note] [:twice-4 :text] [:twice-5 :text] [:twice-doc :title]]))
        alices-row (base-fact :twice-5 :text)
        marks-rows (base-marks-rows)
        batch-end (gates-last-stamp)
        seer-rows (rows-by-task :mb-facts-ps (record/place-key :base :twice-seer :softland.store/permission))
        runs-after [(mapv first (base-entries-of :on since :canvas-1)) (base-run-of :on since :canvas-2)
                    (base-run-of :on since :first-of-two) (base-run-of :on since :second-of-two)]
        ;; The batch's entries in the gate's log of answers, read when each
        ;; of its things had its answer from the door's read.
        twice-log (answer-log :microbatch)
        twice-logged (logged-answers twice-log)
        twice-ids (set (map (comp :act-id :thing) results))
        twice-batches (set (map :batch (filter #(contains? twice-ids (:act-id (:entry %))) twice-logged)))
        twice-records (filterv (fn [[_ r]] (contains? twice-batches (:batch r))) twice-log)
        ;; The check must be able to fail: the first note against itself with
        ;; one stamp moved, with one key left out, and with a removal left out.
        a-fact (some (fn [[[which _ :as k] rows]] (when (and (= :fact which) (:stamp (peek rows))) k)) (sort-by pr-str first-rows))
        a-removal (some (fn [[[which _ :as k] rows]] (when (and (= :by-value which) (nil? (peek rows))) k)) (sort-by pr-str first-rows))
        with-rows (fn [f] (update note-1 :rows (fn [rows] (vec (keep f rows)))))
        stamp-moved (with-rows (fn [[which k row]] [which k (if (and (= [which k] a-fact) row) (update row :stamp inc) row)]))
        fact-left-out (with-rows (fn [[which k row]] (when-not (= [which k] a-fact) [which k row])))
        removal-left-out (with-rows (fn [[which k row]] (when-not (= [which k] a-removal) [which k row])))
        ;; Rulings 17 (a) and (b): the records of the depot of batch clocks.
        of-the-batch (filterv (fn [[_ r]] (= (:batch kept-row) (:batch r))) records)
        _ (Thread/sleep quiet-ms)
        records-after-quiet (clock-records)
        later (decide-in-base! (system-act [{:e :twice-later :k :note}]))
        kept-later (kept-clock-row)
        records-later (clock-records)]
    (check "on a cluster of its own, with seven functions of the store's pure code replaced by ones that give what the store's give: the base is made, a key is marked, and a fact, a value under the marked key and a person's permission are written, each in a batch that runs once"
           [:accepted :accepted :accepted :accepted :accepted [(base-entry-key :on since :canvas-1 :twice-pointer)]]
           [(:word made) (:word marking) (:word fact-before) (:word value-before)
            (:word give-alice) run-before])
    (check "1. the notes hold two attempts of one batch: two notes since the send; one throw made between them, in a call of rows-to-send that gave rows; both attempts given the same last stamp and the same numbers; and the batch before was given another last stamp"
           [2 1 true true true true true]
           [(count notes) (count throws)
            (boolean (and the-throw (pos? (:rows the-throw)) (= (dec notes-then) (:attempts the-throw))))
            (boolean (and note-1 note-2 (some? (:last-stamp note-1)) (= (:last-stamp note-1) (:last-stamp note-2))))
            (boolean (and note-1 note-2 (= (:numbers note-1) (:numbers note-2))))
            (boolean (and note-1 note-of-the-batch-before (not= (:last-stamp note-of-the-batch-before) (:last-stamp note-1))))
            answered?])
    (check "2. both attempts were given one clock: the clock of the row that the module keeps for the batch, and of the batch's one record on the depot of batch clocks"
           [true true [(:clock note-1)]]
           [(boolean (and note-1 note-2 (some? (:clock note-1)) (= (:clock note-1) (:clock note-2))))
            (= (:clock note-1) (:clock kept-row))
            (mapv (fn [[_ r]] (:clock r)) of-the-batch)])
    (check "3. the second attempt began later than the batch's clock, so the one clock is not two readings of one millisecond; and each attempt made the same base from it"
           [true true]
           [(boolean (and note-1 note-2 (< (:clock note-1) (:wall note-2))))
            (boolean (and note-1 note-2 (some? (:base note-1)) (= (:base note-1) (:base note-2))))])
    (check "4. the rows of the two attempts side by side: both made rows under the same keys, and at each key the same row in every field, the stamp too; and the two notes are not empty: each holds as many keys as the other, an answer's key for each of the 18 things among them"
           [[] true 18 18]
           [side-by-side (= (count first-rows) (count noted))
            (count (filter (fn [[which _]] (= :answer which)) (keys first-rows))) (get kinds :answer)])
    (check "the batch held what it was sent to hold: rows of three kinds were noted, of a fact, of an answer and of the facts by value, and of no other kind; a row of the facts by value that goes out is among them; and each thing got the word that it must get, the edge act the word that the 10-second rule gives it against the kept clock"
           [#{:fact :answer :by-value} true true
            [:accepted :accepted :accepted :accepted :accepted :accepted :accepted :accepted :store-key-not-known
             :permission-does-not-stand :accepted :accepted :accepted :accepted :accepted :accepted :accepted edge-word]
            true]
           [(set (keys kinds))
            (boolean (some (fn [[[which _] rows]] (and (= :by-value which) (nil? (peek rows)))) noted))
            (boolean (some (fn [[[which _] rows]] (and (= :by-value which) (some? (peek rows)))) noted))
            (mapv :word results)
            (every? :mine? results)])
    (check "5. each row that the tables hold after the batch, selected on every task, is the row that both attempts made: each fact on the task of its entity, each row of a give, of the marks, of the setting and of the list on every task, each answer, each row of the facts by value, and no row under a key that went out"
           [] not-as-noted)
    (check "6. each answer as answer-by-act-id gives it has the verdict, the word and the stamp of the row that both attempts made"
           [] answers-apart)
    (check "7. once the batch is closed, each of its 18 answers has one entry in the microbatch gate's log of answers, and it is the entry of the answer that both attempts made: the attempt that failed left none"
           [(vec (repeat 18 1)) []]
           [(mapv (fn [r] (count (entries-under twice-logged (:act-id (:thing r))))) results)
            (entries-apart twice-logged (map (fn [r] [(:act-id (:thing r)) (:answer r)]) results))])
    (check "8. the batch's records hold one batch's id, the id of the row of the kept clock; and one of them holds what the batch keeps of itself: the one clock that both attempts were given, and the gate's last stamp behind the batch"
           [#{(:batch kept-row)} [{:clock (:clock note-1) :last-stamp batch-end}]]
           [twice-batches (vec (keep (fn [[_ r]] (when (contains? r :clock) (select-keys r [:clock :last-stamp]))) twice-records))])
    (check "each value of the batch is in its row as the text of what was sent, a value in an act of two entities and a value under a key that takes any value among them"
           ["\"a plain value\"" "\"a value on a second entity of one act\"" "\"a person's text\""
            "\"a value under a key that no check before it wrote\"" ":canvas-2" ":second-of-two"]
           (mapv (fn [[e k]] (:value (base-fact e k)))
                 [[:twice-2 :text] [:twice-4 :text] [:twice-5 :text] [:twice-7 :plain] [:twice-pointer :on] [:twice-pointer-2 :on]]))
    (check "the rows that the build of what the gates keep added are among them: a person's fact holds who wrote it and her permission; the row of the marked keys, on every task, keeps the stamp of the key from before and gives the new key the last stamp of this batch; the give with the slot holds it; and in the facts by value the value before went out, the new one came in, and of two acts on one place the later turn's value alone has a row"
           [[:alice alice-in-base] tasks #{{:on since :in batch-end}} #{[:value [:twice-seer :base :base system-own :see]]}
            [[] [:twice-pointer] [] [:twice-pointer-2]]]
           [[(record/who-of alices-row) (record/permission-of alices-row)]
            (count marks-rows) (set (map record/marked-keys-of (vals marks-rows)))
            (set (map record/read-value (vals seer-rows)))
            runs-after])
    (check "the facts of the batch are on more than one task" true (< 1 (count fact-tasks)))
    (check "the check can fail: the first attempt's note beside itself with one stamp moved gives that key and the stamp; with one fact left out, that key as noted in one attempt only; and with a row that goes out left out, that key too, so a key noted with no row is told from a key that was not noted"
           [[{:key a-fact :noted-in :both :differs #{:stamp}}]
            [{:key a-fact :noted-in :the-first-attempt-only}]
            [{:key a-removal :noted-in :the-first-attempt-only}]
            true]
           [(rows-side-by-side note-1 stamp-moved)
            (rows-side-by-side note-1 fact-left-out)
            (rows-side-by-side note-1 removal-left-out)
            (boolean (and a-fact a-removal))])
    (check "ruling 17 (a): the depot of batch clocks holds as many records as batches with things since the launch, which is the attempts noted less one, because the second attempt found its clock kept and appended nothing; a quiet time adds none; and a later batch adds one, under a later id"
           [(dec notes-then) (dec notes-then) notes-then true :accepted]
           [(count records) (count records-after-quiet) (count records-later)
            (boolean (and kept-row kept-later (< (:batch kept-row) (:batch kept-later))))
            (:word later)])
    (check "ruling 17 (b): each record of that depot, read back, is a map of exactly a batch's id and a clock, two integers; all are on one partition"
           [true 1]
           [(every? (fn [[_ r]] (clock-record? r)) records-later) (count (set (map first records-later)))])
    (shown "a batch of the store's own microbatch gate, of" (count results) "things, was sent with a throw asked for: attempts noted" (count notes)
           "| throws made" (count throws) ", in a call of rows-to-send that gave" (:rows the-throw) "rows, on the thread" (pr-str (:thread the-throw))
           "| the last stamps that the attempts were given:" (pr-str (mapv :last-stamp notes)) "and the numbers" (pr-str (mapv :numbers notes))
           "| all things answered:" answered? "," waited "ms after the topology went on")
    (shown "the clocks that the attempts were given:" (pr-str (mapv :clock notes))
           (if (and note-1 note-2 (= (:clock note-1) (:clock note-2))) "| one clock for both" "| NOT one clock")
           "| the kept row" (pr-str kept-row) "| the clocks of the batch's records on the depot" (pr-str (mapv (fn [[_ r]] (:clock r)) of-the-batch))
           "| the first attempt called base" (when note-1 (- (:wall note-1) (:clock note-1)))
           "ms after the clock that it was given: the append, the ack and the read back | the second attempt began"
           (when (and note-1 note-2) (- (:wall note-2) (:clock note-1)))
           "ms after the first attempt's clock: a clock read anew would have given each stamp another time")
    (shown "the two attempts side by side:" (count noted) "keys in the second attempt's note, of the kinds" (pr-str (into (sorted-map) kinds))
           "| keys whose rows are not the same in both attempts:" (if (vector? side-by-side) (count side-by-side) (pr-str side-by-side))
           (if (and (vector? side-by-side) (seq side-by-side)) (str "| the first of them: " (pr-str (vec (take 3 side-by-side)))) "")
           "| rows that the tables do not hold as the second attempt noted them:" (count not-as-noted)
           "| answers that answer-by-act-id gives otherwise:" (count answers-apart))
    (shown "the batch in the microbatch gate's log of answers, once closed: entries under each of its" (count results) "act-ids"
           (pr-str (frequencies (map (fn [r] (count (entries-under twice-logged (:act-id (:thing r))))) results)))
           "| its records:" (count twice-records) "of batch" (pr-str twice-batches)
           "| what it keeps of itself:" (pr-str (vec (keep (fn [[_ r]] (when (contains? r :clock) (select-keys r [:clock :last-stamp]))) twice-records))))
    (shown "the act at the edge of the 10 seconds: its carried stamp is" edge-ahead-ms "ms after the hand's reading; the first attempt was given a clock"
           (when note-1 (- (:clock note-1) edge-reading)) "ms after that reading and the second attempt began" (when note-2 (- (:wall note-2) edge-reading))
           "ms after it | the word that the 10-second rule gives it against the first attempt's clock:" edge-word
           "| the word that it got:" (:word (peek results))
           (if at-the-edge?
             "| it was at the edge: an attempt that read the clock anew at the second attempt's start would have accepted it"
             "| it was not at the edge in this run, so it shows nothing about the verdict"))
    (shown "the depot of batch clocks on that cluster:" (count records) "records for" notes-then "attempts of" (dec notes-then)
           "batches with things | after" quiet-ms "ms with nothing sent:" (count records-after-quiet) "| after one more batch:" (count records-later)
           "| the kept row then:" (pr-str kept-later))))

(deftest a-closing-runs-a-second-time
  (let [made-streaming (decide! (making :alice :system :alice))
        stray {:act-id [:base :microbatch] :layer :base :marker (UUID/randomUUID)}
        stray-digest (digest/hex-of stray)
        xs [(system-act [(value-fact :closing-1 :text "kept twice")])
            (system-act [{:e :closing-2 :k :note}])
            (thing :base :mallory [:mallory :base :base system-own] [{:e :closing-3 :k :note}])
            (thing :alice :alice alice-own [{:e :closing-4 :k :note}] {:act-id [:alice :microbatch (UUID/randomUUID)] :depot :microbatch})
            stray]
        {:keys [results answered? waited throws]} (decide-with-a-failed-closing! xs)
        acts (pop results)
        ids (set (map (comp :act-id :thing) acts))
        strays-of (fn [records] (into [] (comp (map :entry) (filter #(= stray-digest (:digest %)))) (logged-strays records)))
        log (await-log :microbatch
                       (fn [records]
                         (let [logged (logged-answers records)]
                           (and (every? #(<= 2 (count (entries-under logged %))) ids) (<= 2 (count (strays-of records))))))
                       twice-limit-ms)
        logged (logged-answers log)
        of-the-batch (filterv #(contains? ids (:act-id (:entry %))) logged)
        batch-ids (set (map :batch of-the-batch))
        batch-records (mapv (comp content second) (filter (fn [[_ r]] (contains? batch-ids (:batch r))) log))
        half (quot (count batch-records) 2)
        clock-then (kept-clock-row)
        last-stamp-then (gates-last-stamp)
        to-keep-after (to-keep-by-task)
        to-close-after (to-close-by-task)]
    (check "on the second cluster a streaming layer is made, and one batch of the base is sent with a failed closing asked for: two acts that are accepted, one of a maker who may not act, one into the streaming layer, and a stray; each act got its word, with the digest of what was sent"
           [:accepted [:accepted :accepted :permission-does-not-stand :layer-belongs-to-the-other-gate] [true true true true] true]
           [(:word made-streaming) (mapv :word acts) (mapv :mine? acts) answered?])
    (check "the run that closes the batch failed once, behind its appends: one throw was made, in a call of marks-places-asked"
           1 (count throws))
    (check "each entry of that batch is in the microbatch gate's log of answers twice, the same: each of the four answers has two entries, both the entry of its answer, the refused ones with the word of their row; and the stray has two, with its word, its digest and the task whose set holds it"
           [{2 4} []
            (vec (repeat 2 {:stray :act-id-malformed :digest stray-digest :task (task-of-stray :mb-task-ps :act-id-malformed stray-digest)}))]
           [(frequencies (map count (vals (group-by (comp :act-id :entry) of-the-batch))))
            (entries-apart logged (map (fn [r] [(:act-id (:thing r)) (:answer r)]) acts))
            (strays-of log)])
    (check "the records of the batch are there twice, the second run's the same as the first's in every part: they hold one batch's id, the id of the kept clock's row, and each run's first record holds the batch's clock and its last stamp, the kept clock and the gate's last stamp"
           [#{(:batch clock-then)} true true [{:clock (:clock clock-then) :last-stamp last-stamp-then} {:clock (:clock clock-then) :last-stamp last-stamp-then}]]
           [batch-ids (pos? half) (= (subvec batch-records 0 half) (subvec batch-records half))
            (vec (keep (fn [r] (when (contains? r :clock) (select-keys r [:clock :last-stamp]))) batch-records))])
    (check "when the second run of the closing has committed, nothing is left to keep: `:to-keep` holds no key on any task, and `:to-close` is absent on every task"
           [(vec (repeat tasks 0)) (vec (repeat tasks nil))] [to-keep-after to-close-after])
    (shown "a run of the microbatch gate that closes a batch failed behind its appends and ran again: throws made" (count throws)
           "on the thread" (pr-str (:thread (first throws))) "| all acts answered:" answered? "," waited "ms after the topology went on"
           "| words:" (pr-str (mapv :word acts)))
    (shown "each entry of that batch is in the log twice: entries by act-id" (pr-str (frequencies (map count (vals (group-by (comp :act-id :entry) of-the-batch)))))
           "| the stray's entries:" (pr-str (strays-of log)) "| records of the batch:" (count batch-records)
           "| each run's first record keeps:" (pr-str (vec (keep (fn [r] (when (contains? r :clock) (select-keys r [:clock :last-stamp]))) batch-records)))
           "| after it, keys of :to-keep by task" (pr-str to-keep-after) "and :to-close by task" (pr-str to-close-after))))

(deftest an-event-fails-before-its-commit
  (let [x (alice [(value-fact :fails-before :text "decided by the second run")])
        _ (swap! event-failures assoc :before-the-commit (:act-id x) :calls [])
        {:keys [ack sights entry-with-no-answer? log]} (send-and-watch! x (fn [logged] (seq (entries-under logged (:act-id x)))))
        calls (filterv #(= :before-the-commit (:where %)) (:calls @event-failures))
        _ (swap! event-failures assoc :before-the-commit nil)
        answer (answer-of (:act-id x))
        entries (entries-under (logged-answers log) (:act-id x))
        the-row (fact-row :alice :fails-before :text)]
    (check "the stand-in of `writes` was called twice for the act: the first call threw, behind the event's reads and before its first write, and Rama ran the record again"
           2 (count calls))
    (check "the second run is the first that counts, and it decided the act: accepted, with the digest of what was sent, under a stamp whose time is later than the throw, and the place holds the text of the value"
           [:accepted true true "\"decided by the second run\""]
           [(:verdict answer) (digest/is-digest-of? (:digest answer) x)
            (boolean (and (first calls) answer (< (:wall (first calls)) (stamp/time-of (:stamp answer)))))
            (:value the-row)])
    (check "the run that threw left nothing in the log: the act has one entry, the entry of its answer; and no read by act-id gave nothing behind a read of the log that found an entry"
           [[(entry-of-answer (:act-id x) answer)] false]
           [entries entry-with-no-answer?])
    (shown "an event of the streaming gate failed before its commit and Rama ran its record again: calls of writes for the act" (count calls)
           "| the first threw on the thread" (pr-str (:thread (first calls))) "| the append gave" (pr-str ack)
           "| the answer's stamp is" (when (and answer (first calls)) (- (stamp/time-of (:stamp answer)) (:wall (first calls))))
           "ms later than the throw | pairs of reads, [an answer was read, the entries read behind it]:" (pr-str sights))))

(deftest an-event-fails-behind-its-commit
  (let [before-things (mapv (fn [i] (alice [(value-fact (keyword (str "replayed-" i)) :text (str "sent before the throw, " i))])) (range 5))
        befores (mapv decide! before-things)
        logged-before (logged-answers (answer-log :streaming))
        counts-before (mapv #(count (entries-under logged-before (:act-id %))) before-things)
        x (alice [(value-fact :fails-behind :text "committed by the first run")])
        _ (swap! event-failures assoc :behind-the-commit (:act-id x) :calls [])
        {:keys [ack sights answer-with-no-entry? entry-with-no-answer? log]}
        (send-and-watch! x (fn [logged]
                             (and (seq (entries-under logged (:act-id x)))
                                  (every? #(<= 2 (count (entries-under logged (:act-id %)))) before-things))))
        calls (filterv #(= :behind-the-commit (:where %)) (:calls @event-failures))
        decisions (filterv #(= :decision (:where %)) (:calls @event-failures))
        _ (swap! event-failures assoc :behind-the-commit nil)
        answer (answer-of (:act-id x))
        the-row (fact-row :alice :fails-behind :text)
        logged (logged-answers log)
        entries (entries-under logged (:act-id x))
        answered (answered-of @things-sent :streaming)
        answered-ids (set (map first answered))]
    (check "five acts are sent to the streaming gate right before the throw: each is accepted and has one entry in the log of answers"
           [(vec (repeat 5 :accepted)) [1 1 1 1 1]] [(mapv :word befores) counts-before])
    (check "the stand-in of `answer-record` was called at least twice for the act-id; the first call threw, in the second part of the event, and each later call gave an entry: the act has one entry fewer than calls. And the stand-in of `writes` was called once for it, before the throw: one run decided the act, and each later run found the act-id answered and decided nothing"
           [true (dec (count calls)) 1 true]
           [(<= 2 (count calls)) (count entries) (count decisions)
            (boolean (and (first decisions) (first calls) (<= (:wall (first decisions)) (:wall (first calls)))))])
    (check "the answer's row was committed by the run that threw, before the entry was made: the act is accepted, with the digest of what was sent, its place holds the text of the value, and the time of its stamp is not later than the throw"
           [:accepted true "\"committed by the first run\"" true]
           [(:verdict answer) (digest/is-digest-of? (:digest answer) x) (:value the-row)
            (boolean (and (first calls) answer (<= (stamp/time-of (:stamp answer)) (:wall (first calls)))))])
    (check "each entry that the later runs left is a copy of that committed row: the entry of the answer; and no read by act-id gave nothing behind a read of the log that found an entry"
           [#{(entry-of-answer (:act-id x) answer)} false]
           [(set entries) entry-with-no-answer?])
    (check "the replay: each of the five acts that were sent right before the throw has one entry more, the entry of its answer"
           [[2 2 2 2 2] []]
           [(mapv #(count (entries-under logged (:act-id %))) before-things)
            (entries-apart logged (map (fn [r] [(:act-id (:thing r)) (:answer r)]) befores))])
    (check "over each thing that the hand sent to the streaming depot on this cluster: every row of an answer has an entry, every entry under one act-id is the entry of that answer, and no entry is under an act-id that has no answer"
           [true [] []]
           [(< 5 (count answered)) (entries-apart logged answered)
            (vec (distinct (keep (fn [{:keys [entry]}] (when-not (contains? answered-ids (:act-id entry)) (short-id (:act-id entry)))) logged)))])
    (shown "an event of the streaming gate failed behind its commit and before its append, and Rama ran its record again: calls of answer-record for the act" (count calls)
           "| the first threw on the thread" (pr-str (:thread (first calls))) "| calls of writes for the act" (count decisions)
           "| entries under the act-id:" (count entries) "| the append gave" (pr-str ack)
           "| the answer's stamp is" (when (and answer (first calls)) (- (:wall (first calls)) (stamp/time-of (:stamp answer))))
           "ms earlier than the throw")
    (shown "pairs of reads while the event failed and ran again, [an answer was read, the entries read behind it]:" (pr-str sights)
           "| a pair met the moment in which the answer is readable and the log holds no entry of it:" answer-with-no-entry?
           "(a matter of the time of the reads; the check rests on the stamp, the calls and the count of entries)")
    (shown "the replay behind that throw: entries under each of the five acts sent right before it, before and after:" (pr-str counts-before)
           (pr-str (mapv #(count (entries-under logged (:act-id %))) before-things))
           "| on this cluster the streaming gate's log holds" (count logged) "answers' entries for" (count answered) "act-ids that have an answer")))

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
  (strays-and-limits)
  (the-reads-take-anything)
  (what-a-fact-keeps)
  (the-keys-that-take-a-second-write)
  (what-a-permission-allows)
  (what-points-at-a-thing)
  (the-base-is-made)
  (an-act-lands-in-the-base)
  (an-answer-is-readable-when-its-batch-is-closed)
  (refusals-and-repeats-in-the-base)
  (one-batch-one-order)
  (a-person-acts-in-the-base)
  (a-permission-deeper-than-four-in-the-base)
  (the-bounds-in-the-base)
  (a-long-batch)
  (an-act-on-several-tasks)
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
  second cluster: the groups that make a gate fail, the microbatch gate's
  two first and then the streaming gate's two. The hand says which cluster
  it is."
  []
  (if (= :a-batch-runs-twice (:checks *hand*))
    (do (a-batch-runs-a-second-time)
        (a-closing-runs-a-second-time)
        (an-event-fails-before-its-commit)
        (an-event-fails-behind-its-commit))
    (the-checks-on-the-store-as-defined)))

(defn- run-on-a-cluster
  "Creates an in-process cluster, launches the module `Store` in it with
  four tasks, two threads and two workers, runs the checks that `checks`
  names with the hand on that cluster, and closes the cluster. Gives what
  `run-tests` gives. The launch takes the functions that the vars of the
  store's code hold at that moment."
  [checks]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (binding [*hand* (assoc (open-hand ipc) :checks checks)]
      (run-tests 'softland.store.module-proof))))

(defn -main
  "Runs the proof on two clusters, one after the other, and exits: status 0
  when each check of both runs holds. It takes no argument.

  The first cluster runs every check but the groups that make a gate fail,
  on the functions as the store defines them. When it is closed, `-main`
  empties the hand's atoms, all but the shown lines; puts the seven
  stand-ins into the vars of the store's functions; starts the second
  cluster, which runs the groups that make a gate fail; and puts the store's
  functions back when that cluster is closed, in a `finally`. The count line
  is one: the counts of the two runs, added."
  [& _]
  (let [counts #(select-keys % [:test :pass :fail :error])
        first-run (run-on-a-cluster :the-store-as-defined)
        _ (empty-the-hand!)
        second-run (try
                     (put-noting-functions!)
                     (run-on-a-cluster :a-batch-runs-twice)
                     (finally (put-the-stores-functions-back!)))
        result (merge-with + (counts first-run) (counts second-run))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println result)
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
