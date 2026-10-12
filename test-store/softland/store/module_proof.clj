(ns softland.store.module-proof
  "The proof of the store's module on Rama's in-process cluster: the two
  gates, their records, and the three reads. One command, from the
  repository root:

    clojure -M:store-module -m softland.store.module-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The proof plays the door by hand and does not use `softland.store.door`:
  the store stays proven without the door, and the hand stays the proof that
  anything can append to a depot without it. The hand is a few functions of
  this namespace, in the order of one act: it reads the depot of the layer;
  makes the act-id, with a random UUID; asks for locks by an act and reads
  them with one query; seals each value, the UTF-8 of the one text of the
  value, to its place under a lock that it has not used; appends the thing;
  reads the answer by act-id; and compares the answer's digest with the
  digest of what it sent. The hand also plays a reader of the tables, since
  no reader is built in this part: it selects a fact's row, the row of a
  layer's marked keys, and a run of the facts by value, with the keys and
  the partitioning keys that `softland.store.record.rows` gives, and reads a
  row back with that namespace's functions. An error from the append is not an answer. It
  keeps each thing that it sent, so that a repeat is the same thing byte for
  byte.

  The two depots. For a streaming layer the hand appends with the ack level
  :ack and reads the answer once: the ack waits for the gate. For the base,
  the one microbatch layer, no ack carries an answer: the hand appends with
  :append-ack and reads the answer by act-id in a loop with a time limit, as
  a door would. To put several things into one batch it pauses the
  microbatch topology with Rama's test tool, appends, and lets it go on.

  State. The proof starts two in-process clusters, one after the other in
  one JVM, each with four tasks and two workers, launches the module `Store`
  in each, and closes each; a cluster's files are temporary and are Rama's.
  Every check but one group runs on the first cluster, on the functions as
  the store defines them: nothing is replaced in the JVM while it runs. The
  one group that makes a batch of the microbatch gate run a second time runs
  on the second cluster. For that cluster alone `-main` puts three functions
  of this namespace into the vars of three functions of
  `softland.store.gate.batch` that the gate's dataflow calls, `base`,
  `decide-or-send` and `rows-to-send`, before the launch, and puts the
  store's own back when the cluster is closed, in a `finally`. Each of the
  three gives exactly what the store's function gives, and notes it; one of
  them throws once when the group asked for it. Rama takes the function that
  a var holds at each launch.

  The proof owns eleven atoms: the lines that it showed; the things that it
  sent to each depot; the first answers of each gate, in the order sent; the
  locks that it holds and has not used; the bytes of each lock that it read;
  what later checks come back to; and, for the second cluster, the note of
  each attempt of a batch, the wish for a throw, and each throw that was
  made. `-main` empties all but the shown lines between the two clusters:
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
  Where it shows a row, it selects the row from a gate's PState: for a
  streaming layer with the layer as the partitioning key, for the base with
  the row's own route, and for \"on which tasks is this row\" once on each
  task. Such a select goes round the three reads, so it sees a row of a
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
            [softland.store.gate.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.module.microbatch-gate :as microbatch-gate]
            [softland.store.record.rows :as record]
            [softland.store.door.seal :as seal]
            [softland.store.gate.stamp :as stamp]
            [softland.store.door.text :as text])
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
  of the module, the two depots of acts, the three queries, the five PStates
  of each gate, the depot of batch clocks and the PState of the kept clock;
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
           :doc "The locks that the hand has read and not yet used, by layer, each
  with its lock id."}
  locks-in-hand (atom {}))

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

(defonce ^{:private true
           :doc "The bytes of each lock that a read of locks gave the hand, as hex:
  what the depots' logs are searched for, to hold that no lock's bytes are
  on a depot."}
  lock-bytes-read (atom #{}))

(defn- open-hand
  "Takes the cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc        ipc
     :streaming-depot (rama/foreign-depot ipc m "*streaming-depot")
     :answer     (rama/foreign-query ipc m "answer-by-act-id")
     :locks      (rama/foreign-query ipc m "locks-of-request")
     :depot-of-layer (rama/foreign-query ipc m "depot-of-layer")
     :answers-ps (rama/foreign-pstate ipc m "$$streaming-answers")
     :facts-ps   (rama/foreign-pstate ipc m "$$streaming-facts")
     :by-value-ps (rama/foreign-pstate ipc m "$$streaming-facts-by-value")
     :locks-ps   (rama/foreign-pstate ipc m "$$streaming-locks")
     :task-ps    (rama/foreign-pstate ipc m "$$streaming-task")
     :module     m
     :mb-depot      (rama/foreign-depot ipc m "*microbatch-depot")
     :mb-answers-ps (rama/foreign-pstate ipc m "$$microbatch-answers")
     :mb-facts-ps   (rama/foreign-pstate ipc m "$$microbatch-facts")
     :mb-by-value-ps (rama/foreign-pstate ipc m "$$microbatch-facts-by-value")
     :mb-locks-ps   (rama/foreign-pstate ipc m "$$microbatch-locks")
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

(defn- locks-of
  "Reads the locks of a request, of the indexes from `from` to `to`: a map
  from index to {:bytes :way}. It keeps the hex of each lock's bytes that it
  read, for the check that no lock's bytes are on a depot."
  [request-act-id from to]
  (let [routing-id (record/routing-id request-act-id)
        locks (if (record/lock-range-read-by-routing-id routing-id request-act-id from to)
                (rama/foreign-invoke-query (:locks *hand*) routing-id request-act-id from to)
                {})]
    (swap! lock-bytes-read into (keep #(some-> (:bytes %) text/hex)) (vals locks))
    locks))

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

(defn- ask-for-locks!
  "Asks for n locks by an act of `who` under `permission`: one fact under
  :softland.store/lock-request on the uuid of that act's own act-id. Reads
  the answer, then the locks with one query. Gives the result of the act
  with :locks, a map from index to lock, and :request, the request's act-id.
  The locks are put in the hand for the layer."
  [layer who permission n]
  (let [base (thing layer who permission [])
        request (:act-id base)
        result (decide! (assoc base :facts [{:e (nth request 2) :k :softland.store/lock-request :v n}]))
        locks (locks-of request 0 255)]
    (swap! locks-in-hand update layer (fnil into [])
           (map (fn [[index lock]] {:lock-id [request index] :lock lock}) (sort-by key locks)))
    (assoc result :request request :locks locks)))

(defn- take-lock!
  "Takes one lock that the hand holds for a layer and has not used. Gives
  {:lock-id :lock}. Throws when the hand holds none: the proof then asked
  for too few."
  [layer]
  (let [taken (first (get @locks-in-hand layer))]
    (when-not taken (throw (ex-info "the hand holds no lock for the layer" {:layer layer})))
    (swap! locks-in-hand update layer (comp vec rest))
    taken))

(defn- plain-bytes
  "The plain bytes that the hand seals for a value: the UTF-8 of the one text
  of the value in its normal form."
  [v]
  (text/utf8 (act/normal-value v)))

(defn- seal-with
  "Seals a value to the place [layer e k] under a lock in hand,
  {:lock-id :lock}. Gives the fact as it arrives: {:e :k :sealed :lock-id}."
  [{:keys [lock-id lock]} layer e k v]
  {:e e :k k :sealed (seal/seal lock [layer e k] (plain-bytes v)) :lock-id lock-id})

(defn- sealed
  "A value fact as it arrives, sealed under a lock that the hand has not
  used."
  [layer e k v]
  (seal-with (take-lock! layer) layer e k v))

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

(defn- lock-row
  "The row of a lock, by its id."
  [[[layer _ request-uuid] index]]
  (row :locks-ps layer (record/lock-key layer request-uuid index)))

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

(defn- ask-for-base-locks!
  "Asks for n locks in the base by an act of `who` under `permission`, as
  `ask-for-locks!` does in a streaming layer. Gives the result of the act
  with :locks and :request. The locks are put in the hand for the base."
  [who permission n]
  (let [request-thing (thing :base who permission [])
        request (:act-id request-thing)
        result (decide-in-base! (assoc request-thing :facts [{:e (nth request 2) :k :softland.store/lock-request :v n}]))
        locks (locks-of request 0 255)]
    (swap! locks-in-hand update :base (fnil into [])
           (map (fn [[index lock]] {:lock-id [request index] :lock lock}) (sort-by key locks)))
    (assoc result :request request :locks locks)))

(defn- base-row
  "Selects one row of a PState of the microbatch gate, by its key, on the
  task of a route: the entity of a fact, the uuid of an answer, the uuid of
  a lock's request. The select goes round the three reads, so it also sees a
  row of a batch that is not closed."
  [pstate route k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey route}))

(defn- base-fact
  "The row of a place of the base, on the task of its entity, or nil."
  [e k]
  (base-row :mb-facts-ps e (record/place-key :base e k)))

(defn- base-lock-key
  "The key of a lock of the base, from its id."
  [[[_ _ request-uuid] index]]
  (record/lock-key :base request-uuid index))

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
  "One value of each kind that the checks of a plain value write: an id that
  is a keyword, a string with spaces, a number, a map; the proof adds an id
  that is a UUID."
  {:kw :canvas-1 :string "a string with spaces" :number 42 :map {:a [1 #{2}] "b c" 1.5}})

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
        request-early (ask-for-locks! :alice :alice alice-own 4)
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
    (check "a request for locks into a layer that is not made is refused, and makes no lock"
           [:layer-not-made {}] [(:word request-early) (:locks request-early)])
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
    (shown "an act into a layer that is not made refused:" (told not-made))
    (shown "a person's layer made by the system for that person, with the depot streaming:" (told personal)
           "| depot-of-layer :alice =" (depot-of-layer :alice))
    (shown "a group's layer made by the person who creates it:" (told group) "| depot-of-layer :team =" (depot-of-layer :team))
    (shown "a making act for a layer that is made is refused: by the system" (:word again-by-system)
           "| by its holder" (:word again-by-holder) "| a second act of the system in a personal layer" (:word system-again))))

(deftest an-act-lands
  (let [request (ask-for-locks! :alice :alice alice-own 16)
        in-hand (first (get @locks-in-hand :alice))
        fact (sealed :alice :note-1 :text "the first text")
        landed (decide! (alice [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (fact-row :alice :note-1 :text)
        the-lock (lock-row (:lock-id in-hand))
        listed (decide! (alice [] {:facts (list (sealed :alice :note-2 :text {:n 1}) (sealed :alice :note-3 :text #{:a}))}))
        list-uuid (nth (:act-id (:thing listed)) 2)
        never-sent (answer-of (new-act-id :alice))]
    (swap! kept assoc :landed landed :landed-lock in-hand)
    (check "a request for 16 locks is accepted" :accepted (:word request))
    (check "one query gives its 16 locks, each 32 bytes of way 1, no two the same"
           [16 #{32} #{1} 16]
           [(count (:locks request)) (set (map #(alength ^bytes (:bytes %)) (vals (:locks request))))
            (set (map :way (vals (:locks request)))) (count (set (map #(vec (:bytes %)) (vals (:locks request)))))])
    (check "the request's own fact is a row: its count, as a plain value on the uuid of its act"
           16 (record/value-of (fact-row :alice (nth (:request request) 2) :softland.store/lock-request)))
    (check "one act lands through the streaming gate and is read back by act-id: accepted, with the digest of what was sent"
           [:acked :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the value as a String, the text itself of its plain bytes, and gives those bytes back; the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and no lock; the bytes are not the sealed bytes that were sent"
           ["\"the first text\"" (vec (plain-bytes "the first text")) act-uuid 0 ":alice" (text/text alice-own) (:stamp (:answer landed)) false false false]
           [(:value the-row) (value-bytes the-row) (:by-act the-row) (:by-index the-row) (:who the-row) (:permission the-row) (:stamp the-row)
            (contains? the-row :lock-act) (contains? the-row :lock-index) (= (value-bytes the-row) (vec (:sealed fact)))])
    (check "the lock's row has the bytes that the door read, and now names the fact that it seals"
           [(vec (:bytes (:lock in-hand))) 1 act-uuid 0]
           [(vec (:bytes the-lock)) (:way the-lock) (:seals-act the-lock) (:seals-index the-lock)])
    (check "the lock opens the sealed bytes that the hand sent, at their place, and what it opens is the row's value"
           [(vec (plain-bytes "the first text")) true]
           (let [opened (seal/open (record/lock-of the-lock) [:alice :note-1 :text] (:sealed fact))]
             [(vec opened) (= (vec opened) (value-bytes the-row))]))
    (check "an act whose facts came as a list is accepted, and each fact has its index in the order sent"
           [:accepted [list-uuid 0] [list-uuid 1]]
           [(:word listed)
            ((juxt :by-act :by-index) (fact-row :alice :note-2 :text))
            ((juxt :by-act :by-index) (fact-row :alice :note-3 :text))])
    (check "an act-id that was never sent has no answer: nothing yet" nil never-sent)
    (shown "a request for 16 locks, read with one query:" (told request) "| locks" (count (:locks request)))
    (shown "one act landing through the streaming gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "its place in the record: written by" (str (:by-act the-row)) "index" (:by-index the-row)
           "stamp" (:stamp the-row) "| its value, kept plain:" (pr-str (record/read-value the-row))
           "| who wrote it:" (pr-str (record/who-of the-row)) "under" (pr-str (record/permission-of the-row))
           "| the row names no lock; the lock's own row names the fact:" (str (:seals-act the-lock)) (:seals-index the-lock))))

(deftest refusals-and-repeats
  (let [accepted-thing (:thing (:landed @kept))
        first-fact-id [(:act-id accepted-thing) 0]
        unsealed (decide! (alice [{:e :note-9 :k :text :v "a plain value"}]))
        answer-before (answer-of (:act-id accepted-thing))
        stamp-before (last-stamp :alice)
        repeat-ack (send! accepted-thing)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (do (send! (:thing unsealed)) (answer-of (:act-id (:thing unsealed))))
        stamp-after (last-stamp :alice)
        other-under-used (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id accepted-thing)))
        junk (decide! {:act-id (new-act-id :alice) :layer :alice :junk 1})
        good-under-junk (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id (:thing junk))))
        stale (decide! (alice [(assoc (sealed :alice :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        empty-place (decide! (alice [(assoc (sealed :alice :note-7 :text "x") :replaces first-fact-id)]))
        replaced (decide! (alice [(assoc (sealed :alice :note-1 :text "the second text") :replaces first-fact-id)]))
        second-fact-id [(:act-id (:thing replaced)) 0]
        stale-now (decide! (alice [(assoc (sealed :alice :note-1 :text "late") :replaces first-fact-id)]))
        repeat-of-replace (do (send! (:thing replaced)) (answer-of (:act-id (:thing replaced))))
        over (decide! (alice [(sealed :alice :note-1 :text "the third text")]))
        over-row (fact-row :alice :note-1 :text)
        retract (decide! (alice [{:e :note-1 :k :text}]))
        retract-row (fact-row :alice :note-1 :text)
        over-retract (decide! (alice [(assoc (sealed :alice :note-1 :text "after the retract")
                                             :replaces [(:act-id (:thing retract)) 0])]))
        retract-of-retract (decide! (alice [{:e :note-6 :k :text}]))
        retract-again (decide! (alice [{:e :note-6 :k :text}]))
        onto-retract (decide! (alice [(sealed :alice :note-6 :text "onto a retract, naming nothing")]))
        on-the-layer (decide! (alice [(sealed :alice :alice :title "a fact of the layer's own entity, under a plain key")]))
        twice (decide! (alice [{:e :note-10 :k :text} {:e :note-10 :k :text}]))
        own-base (alice [])
        own-fact (decide! (assoc own-base :facts [{:e :note-10 :k :text}
                                                  {:e :note-11 :k :text :replaces [(:act-id own-base) 0]}]))
        one-lock (take-lock! :alice)
        lock-twice (decide! (alice [(seal-with one-lock :alice :note-10 :text "a") (seal-with one-lock :alice :note-11 :text "b")]))
        lock-then-used (decide! (alice [(seal-with one-lock :alice :note-10 :text "a")]))]
    (check "an unsealed value is refused, and the refusal is recorded with its reason, a stamp and the digest"
           [:refused :not-sealed true true]
           [(:verdict (:answer unsealed)) (:reason (:answer unsealed)) (int? (:stamp (:answer unsealed))) (:mine? unsealed)])
    (check "a refusal writes nothing at the place" nil (fact-row :alice :note-9 :text))
    (check "a repeat is acked and answered from the record: the same answer, with the stamp of the first decision"
           [:acked answer-before] [repeat-ack answer-after])
    (check "a repeat of a refused act reads the recorded refusal" (:answer unsealed) refused-again)
    (check "not decided twice: the task's last stamp is the same after two repeats" stamp-before stamp-after)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false]
           [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (fact-row :alice :note-8 :text))
    (check "a thing that is no act uses its act-id: a good act sent under it reads the first thing's refusal"
           [:unknown-part true :unknown-part false]
           [(:word junk) (:mine? junk) (:word good-under-junk) (:mine? good-under-junk)])
    (check "a stale replaces is refused: a fact that is not the current fact of the place" :replaced-fact-not-current (:word stale))
    (check "a fact that names something to replace on an empty place is stale" :replaced-fact-not-current (:word empty-place))
    (check "a fact that names the current fact of its place is accepted" :accepted (:word replaced))
    (check "a second act that names the fact that was replaced is stale" :replaced-fact-not-current (:word stale-now))
    (check "a repeat of the accepted replace is still accepted, although its replaces would now be stale"
           (:answer replaced) repeat-of-replace)
    (check "a fact that names nothing to replace is never stale: it becomes the current fact of a place that holds one"
           [:accepted (nth (:act-id (:thing over)) 2)] [(:word over) (:by-act over-row)])
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
    (check "two facts of one act under one lock are refused, and the lock is free for a later act"
           [:lock-named-twice :accepted] [(:word lock-twice) (:word lock-then-used)])
    (shown "a refusal recorded with its reason:" (pr-str (:answer unsealed)))
    (shown "an unsealed value refused:" (told unsealed))
    (shown "a repeat (same act-id sent twice) answered from the record, not decided twice: stamp before" (:stamp answer-before)
           "after" (:stamp answer-after) "| the task's last stamp before" stamp-before "after" stamp-after)
    (shown "a second, different act under a used act-id told so: the digest of the answer" (subs (:digest (:answer other-under-used)) 0 16)
           "is not the digest of what was sent" (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "a stale replaces refused:" (told stale) "| the current fact named:" (told replaced) "| the replaced fact named again:" (told stale-now))
    (shown "a place named twice:" (:word twice) "| a fact that replaces a fact of its own act:" (:word own-fact)
           "| two facts under one lock:" (:word lock-twice))
    (shown "a fact that names nothing is never stale:" (told over) "| a retract is the current fact of its place:" (told retract)
           "| a later fact replaces the retract:" (told over-retract) "(second fact id" (short-id (first second-fact-id)) ")")))

(deftest settings-and-permissions
  (let [setting (decide! (alice [{:e :team :k :softland.store/depot :v :streaming}]))
        other-layer (decide! (thing :alice :alice alice-in-team [{:e :note-5 :k :text}]))
        not-yet (decide! (thing :alice :bob bobs [{:e :note-5 :k :text}]))
        give (decide! (alice [{:e :bob :k :softland.store/permission :v bobs}]))
        not-yet-again (do (send! (:thing not-yet)) (answer-of (:act-id (:thing not-yet))))
        bob-asks (ask-for-locks! :alice :bob bobs 2)
        bob-writes (decide! (thing :alice :bob bobs [(sealed :alice :bobs-note :text "bob's text")]))
        beside (decide! (thing :alice :bob bobs [{:e :carol :k :softland.store/permission :v [:carol :alice :alice alice-own]}]))
        by-no-holder (decide! (thing :alice :carol bobs [{:e :dave :k :softland.store/permission :v [:dave :alice :alice bobs]}]))
        carol-not-given (decide! (thing :alice :carol [:carol :alice :alice alice-own] [{:e :note-5 :k :text}]))
        give-carol (decide! (thing :alice :bob bobs [{:e :carol :k :softland.store/permission :v carols}]))
        give-dave (decide! (thing :alice :carol carols [{:e :dave :k :softland.store/permission :v daves}]))
        dave-acts (decide! (thing :alice :dave daves [{:e :daves-note :k :text}]))
        five-deep (decide! (thing :alice :dave daves [{:e :erin :k :softland.store/permission :v [:erin :alice :alice daves]}]))
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
                                    (assoc (sealed :alice :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        erin-acts (decide! (thing :alice :erin [:erin :alice :alice alice-own] [{:e :note-5 :k :text}]))
        tool-locks (ask-for-locks! :alice :alice alice-own 2)
        tools-own (decide! (alice [(sealed :alice :alice :class "a tool's own class")
                                   (sealed :alice :bob :permission "a tool's own permission")]))
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
    (check "an act under it is accepted: a request for locks, and a value sealed under one of them"
           [:accepted :accepted] [(:word bob-asks) (:word bob-writes)])
    (check "a permission given by one who does not hold the one above is refused: it is not beneath the giver's own"
           :permission-fact-not-beneath-the-acts-own (:word beside))
    (check "an act under a permission by a who that is not its holder is refused" :who-not-the-permissions-holder (:word by-no-holder))
    (check "the refused give gave nothing: an act under that permission is refused" :permission-does-not-stand (:word carol-not-given))
    (check "a chain of four: each holder gives one beneath its own, and the last acts under it"
           [:accepted :accepted :accepted] [(:word give-carol) (:word give-dave) (:word dave-acts)])
    (check "a permission five deep is no permission: the holder of one four deep cannot give"
           :permission-fact-not-a-permission (:word five-deep))
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
    (check "a tool's own facts under the bare words :class and :permission, sealed, on the layer's entity and on a holder's, are accepted as facts under plain keys"
           [:accepted :accepted true] [(:word tool-locks) (:word tools-own) (:mine? tools-own)])
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
    (shown "a chain four deep stands, and a fifth is refused:" (:word give-carol) (:word give-dave) (:word dave-acts) (:word five-deep))
    (shown "granted-in must be the act's layer:" (:word granted-elsewhere) "| a permission's place takes one write:" (:word give-again)
           "| ending a permission:" (:word end-it) "| a depot after the making:" (:word depot-again) (:word depot-other) (:word depot-retract))
    (shown "whole or nothing: a give with a stale fact" (:word with-stale) "| an act under the permission that was not given" (:word erin-acts))
    (shown "a tool's own facts under the bare words :class and :permission, sealed, accepted as facts under plain keys:" (told tools-own)
           "| depot-of-layer :alice =" (depot-of-layer :alice) "| an act under Bob's permission after:" (:word bob-after)
           "| a key in softland.store that the store does not know refused:" (told unknown-key))))

(deftest the-path-of-a-lock
  (let [used-lock (:landed-lock @kept)
        second-value (decide! (alice [(seal-with used-lock :alice :note-20 :text "a second value")]))
        free (take-lock! :alice)
        refused-first (decide! (alice [(assoc (seal-with free :alice :note-21 :text "v") :replaces [(new-act-id :alice) 0])]))
        free-after (lock-row (:lock-id free))
        copied-bytes (seal-with free :alice :note-21 :text "v")
        copied (decide! (alice [(assoc copied-bytes :e :note-22)]))
        any-bytes (decide! (alice [(assoc copied-bytes :sealed (byte-array 40))]))
        then-used (decide! (alice [copied-bytes]))
        unknown-lock (decide! (alice [(seal-with {:lock-id [(new-act-id :alice) 0] :lock (:lock free)} :alice :note-23 :text "v")]))
        team-request (ask-for-locks! :team :alice alice-in-team 2)
        team-lock (take-lock! :team)
        in-other-layer (decide! (alice [(seal-with team-lock :alice :note-24 :text "v")]))
        renamed (decide! (alice [(seal-with (assoc team-lock :lock-id [[:alice :streaming (nth (first (:lock-id team-lock)) 2)] 0])
                                            :alice :note-24 :text "v")]))
        team-uses (decide! (thing :team :alice alice-in-team [(seal-with team-lock :team :plan :text "the team's text")]))
        by-stranger (ask-for-locks! :alice :mallory alice-own 3)
        too-many (ask-for-locks! :alice :alice alice-own 257)
        request (ask-for-locks! :alice :alice alice-own 16)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (:locks request))
        repeat-ack (send! (:thing request))
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of (:request request) 0 255))
        bobs-lock (take-lock! :alice)
        by-another-who (decide! (thing :alice :bob bobs [(seal-with bobs-lock :alice :bobs-note-3 :text "sealed under a lock that Alice asked for")]))
        both-base (alice [])
        both (decide! (assoc both-base :facts [{:e (nth (:act-id both-base) 2) :k :softland.store/lock-request :v 2}
                                               (sealed :alice :note-25 :text "with a request")]))]
    (check "a second value under a lock that already seals one is refused" :lock-already-seals-a-value (:word second-value))
    (check "a lock that a refused act named is free: its row names no fact"
           [:replaced-fact-not-current false] [(:word refused-first) (record/seals-a-fact? free-after)])
    (check "sealed bytes copied to another place do not open" :lock-does-not-open-the-value (:word copied))
    (check "any bytes under a real lock id do not open" :lock-does-not-open-the-value (:word any-bytes))
    (check "after those three refusals the lock seals a value of a later act" :accepted (:word then-used))
    (check "a sealed value under a lock id that the store did not make is refused" :lock-not-made-for-this-layer (:word unknown-lock))
    (check "a sealed value under a lock that the store made for another layer is refused, under its own id and under this layer's"
           [:accepted :lock-not-made-for-this-layer :lock-not-made-for-this-layer]
           [(:word team-request) (:word in-other-layer) (:word renamed)])
    (check "that lock is still free in its own layer" :accepted (:word team-uses))
    (check "a refused request makes no lock: by a who that is not the holder, and for 257 locks"
           [:who-not-the-permissions-holder {} :too-many-locks-requested {}]
           [(:word by-stranger) (:locks by-stranger) (:word too-many) (:locks too-many)])
    (check "a request sent twice makes no lock again: each lock has the bytes that the first run made"
           [:acked bytes-before] [repeat-ack bytes-after])
    (check "a read of a part of a request's locks: one index, the last two, a range cut to 0 to 255, an empty range"
           [[10] [14 15] 16 {}]
           [(vec (keys (locks-of (:request request) 10 10))) (vec (sort (keys (locks-of (:request request) 14 99))))
            (count (locks-of (:request request) -5 999)) (locks-of (:request request) 9 3)])
    (check "a lock in each state gives the same bytes: one that seals a value reads as the door first read it"
           (vec (:bytes (:lock free))) (vec (:bytes (get (locks-of (first (:lock-id free)) 0 255) (second (:lock-id free))))))
    (check "a lock is bound to its layer, not to who asked for it: another holder of the layer seals under it" :accepted (:word by-another-who))
    (check "one act asks for locks and carries a sealed value: accepted, and its two locks are made"
           [:accepted 2] [(:word both) (count (locks-of (:act-id (:thing both)) 0 255))])
    (shown "the path of a lock: asked for and read (" (count (:locks request)) "locks ), used, and then a second value under it refused:" (told second-value))
    (shown "the lock of a refused act is still free:" (told refused-first) "then" (told then-used))
    (shown "sealed bytes copied to another place do not open:" (told copied) "| any bytes under a real lock:" (:word any-bytes))
    (shown "a lock that the store did not make for this layer:" (:word unknown-lock) "| a lock of another layer:" (:word in-other-layer)
           "| a refused request makes no lock:" (:word by-stranger) (pr-str (:locks by-stranger)))
    (shown "a request sent twice gives the same" (count bytes-after) "locks, byte for byte")))

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
        many (fn [n] (alice (map (fn [i] {:e (keyword (str "many-" i)) :k :text}) (range n))))
        facts-256 (decide! (many 256))
        facts-257 (decide! (many 257))
        based-on (fn [n] (into {} (map (fn [i] [[[:alice :streaming (UUID. 0 1)] i] 1])) (range n)))
        based-1024 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1024)}))
        based-1025 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1025)}))
        big (take-lock! :alice)
        sealed-over (decide! (alice [(assoc (seal-with big :alice :note-32 :text "v") :sealed (byte-array 65537))]))
        sealed-at (decide! (alice [{:e :note-32 :k :text :lock-id (:lock-id big)
                                    :sealed (seal/seal (:lock big) [:alice :note-32 :text] (byte-array (- 65536 28)))}]))
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
    (check "an act of 256 facts is accepted, and each fact is a row; one of 257 is refused"
           [:accepted 255 :too-many-facts] [(:word facts-256) (:by-index (fact-row :alice :many-255 :text)) (:word facts-257)])
    (check "1,024 entries of based-on are taken; 1,025 are refused" [:accepted :too-many-based-on] [(:word based-1024) (:word based-1025)])
    (check "a sealed value of 65,537 bytes is refused; one of 65,536 is accepted, under the same lock, and its row holds the 65,508 plain bytes that the lock opened: as a String that begins with U+0000 and has one character more than the bytes, since bytes that begin with a zero are not held as their text"
           [:sealed-value-too-large :accepted 65508 65509 true]
           (let [row (fact-row :alice :note-32 :text)]
             [(:word sealed-over) (:word sealed-at) (count (value-bytes row)) (count (:value row)) (.startsWith ^String (:value row) "\u0000")]))
    (check "those 65,508 bytes are zeros, which the hand sealed without the door and which are no value of the form: the gate kept what the lock opened, whatever it is, and a reader is told so"
           [:not-a-value] (record/read-value (fact-row :alice :note-32 :text)))
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
    (shown "a thing over each limit is refused:" (:word facts-257) (:word based-1025) (:word sealed-over) (:word long-entity)
           "| at each limit accepted:" (:word facts-256) (:word based-1024) (:word sealed-at))
    (shown "a carried stamp too far ahead:" (told far-ahead) "| 5 seconds ahead:" (told near-ahead) "carried" carried)))

(deftest the-reads-take-anything
  (let [u (UUID/randomUUID)
        long (keyword (apply str (repeat 300 "a")))
        accepted (:act-id (:thing (:landed @kept)))
        ask (fn [query & args] (apply rama/foreign-invoke-query (get *hand* query) args))]
    (check "the hand gives nothing itself for what is no id or no act-id, and calls no query"
           [nil nil nil nil {} {}]
           [(depot-of-layer nil) (depot-of-layer "alice") (answer-of nil) (answer-of [:alice :streaming]) (locks-of nil 0 1)
            (locks-of accepted "0" 1)])
    (check "the read by act-id, called with what is no act-id of the layer given, gives nothing"
           [nil nil nil nil nil true]
           [(ask :answer :alice [:team :streaming u]) (ask :answer :alice 5) (ask :answer :alice [:alice :streaming])
            (ask :answer "alice" ["alice" :streaming u]) (ask :answer long [long :streaming u])
            (some? (ask :answer :alice accepted))])
    (check "an act-id of the depot microbatch, asked for with its layer and not with its uuid as the routing id, gets nothing" nil (ask :answer :alice [:alice :microbatch (nth accepted 2)]))
    (check "the read of a depot, called with what is no id, or with a layer that is not made, gives nothing"
           [nil nil nil] [(ask :depot-of-layer "alice") (ask :depot-of-layer long) (ask :depot-of-layer :nobody)])
    (check "the read of locks, called with an act-id that made no lock, indexes that are no integers, or the other depot, gives an empty map"
           [{} {} {} {}]
           [(ask :locks :alice accepted 0 255) (ask :locks :alice accepted "0" 3) (ask :locks :alice [:alice :microbatch u] 0 3)
            (ask :locks :alice [:team :streaming u] 0 3)])
    (check "the same uuid in two layers is two answers: an answer is found by its layer and its uuid"
           [nil nil] [(ask :answer :team [:team :streaming (nth accepted 2)]) (answer-of [:team :streaming (nth accepted 2)])])
    (shown "the three reads give nothing for what is no id, no act-id, another layer's act-id or the other depot, and do not fail")))

;; ------------------------------------ what the streaming gate keeps for a read

(deftest what-a-fact-keeps
  (let [made (decide! (making :canvas :alice :alice))
        request (ask-for-locks! :canvas :alice alice-in-canvas 64)
        u (UUID/randomUUID)
        values (assoc kinds-of-value :uuid u)
        facts (mapv (fn [[k v]] (sealed :canvas :kept k v)) values)
        landed (decide! (canvas-act facts))
        act-uuid (nth (:act-id (:thing landed)) 2)
        rows (into {} (map (fn [k] [k (fact-row :canvas :kept k)])) (keys values))
        logged (record-of-act (stored) (:act-id (:thing landed)))
        lock-rows (mapv (fn [fact] (lock-row (:lock-id fact))) facts)
        lock-a (take-lock! :canvas)
        lock-b (take-lock! :canvas)
        refused (decide! (canvas-act [(seal-with lock-a :canvas :not-kept :text "opened, and then dropped")
                                      (assoc (seal-with lock-b :canvas :not-kept-2 :text "x") :sealed (byte-array 40))]))
        unsealed (decide! (canvas-act [{:e :not-kept :k :text :v "a plain value"}]))
        graces [:grace :canvas :canvas alice-in-canvas]
        give (decide! (canvas-act [{:e :grace :k :softland.store/permission :v graces}]))
        grace-asks (ask-for-locks! :canvas :grace graces 2)
        grace-writes (decide! (thing :canvas :grace graces [(sealed :canvas :graces-note :text "grace's text")]))
        retract (decide! (thing :canvas :grace graces [{:e :graces-note :k :gone}]))
        graces-row (fact-row :canvas :graces-note :text)
        retract-row (fact-row :canvas :graces-note :gone)
        give-row (fact-row :canvas :grace :softland.store/permission)
        personal-depot (fact-row :alice :alice :softland.store/depot)
        personal-permission (fact-row :alice :alice :softland.store/permission)]
    (swap! kept assoc-in [:canvas-before :kept-value] (record/read-value (get rows :map)))
    (check "a layer for these checks is made, and a request for 64 locks in it is accepted" [:accepted :accepted 64]
           [(:word made) (:word request) (count (:locks request))])
    (check "an act with a value of each kind is accepted" [:accepted true] [(:word landed) (:mine? landed)])
    (check "each row holds its value plain: it reads back as the value that the maker gave, a keyword, a UUID, a string with spaces, a number and a map"
           (into {} (map (fn [[k v]] [k [:value v]])) values)
           (into {} (map (fn [[k row]] [k (record/read-value row)])) rows))
    (check "each row holds the plain bytes that the hand sealed, and no field of a lock; the bytes are not the sealed bytes that were sent"
           (mapv (fn [[_ v]] [(vec (plain-bytes v)) false false false]) values)
           (mapv (fn [[k _] fact] (let [row (get rows k)]
                                    [(value-bytes row) (contains? row :lock-act) (contains? row :lock-index)
                                     (= (value-bytes row) (vec (:sealed fact)))]))
                 values facts))
    (check "the act in the depot's log is as the hand sent it: each fact holds the sealed bytes and its lock id, and no plain value"
           (mapv (fn [fact] [(vec (:sealed fact)) (:lock-id fact) false]) facts)
           (mapv (fn [fact] [(vec (:sealed fact)) (:lock-id fact) (contains? fact :v)]) (:facts logged)))
    (check "the lock of each value still names the fact that it seals, and opens the log's sealed bytes to the row's bytes: the lock is found from the act in the log, not from the row"
           (mapv (fn [i] [act-uuid i true]) (range (count facts)))
           (mapv (fn [i [k _] the-lock]
                   [(:seals-act the-lock) (:seals-index the-lock)
                    (= (vec (seal/open (record/lock-of the-lock) [:canvas :kept k] (:sealed (nth (:facts logged) i))))
                       (value-bytes (get rows k)))])
                 (range) values lock-rows))
    (check "an act whose first value opens and whose second does not is refused whole: no row at either place, and both locks are free; the bytes that were opened are kept nowhere"
           [:lock-does-not-open-the-value nil nil false false]
           [(:word refused) (fact-row :canvas :not-kept :text) (fact-row :canvas :not-kept-2 :text)
            (record/seals-a-fact? (lock-row (:lock-id lock-a))) (record/seals-a-fact? (lock-row (:lock-id lock-b)))])
    (check "a gate still refuses an unsealed value that arrives, and writes no row" [:not-sealed nil]
           [(:word unsealed) (fact-row :canvas :not-kept :text)])
    (check "each row holds who wrote it and the permission that the act named: the maker of the layer under her first permission"
           #{[:alice alice-in-canvas]} (set (map (fn [row] [(record/who-of row) (record/permission-of row)]) (vals rows))))
    (check "a second holder, given a permission in the kept form without the slot, acts: her rows hold her and her permission; a retract's row holds them too; the give's row holds the giver and the giver's permission"
           [:accepted :accepted :accepted :accepted
            [:grace graces [:value "grace's text"]] [:grace graces [:nothing-here]] [:alice alice-in-canvas [:value graces]]]
           [(:word give) (:word grace-asks) (:word grace-writes) (:word retract)
            [(record/who-of graces-row) (record/permission-of graces-row) (record/read-value graces-row)]
            [(record/who-of retract-row) (record/permission-of retract-row) (record/read-value retract-row)]
            [(record/who-of give-row) (record/permission-of give-row) (record/read-value give-row)]])
    (check "the system made Alice's personal layer for her: the rows of that making hold the system as who, and Alice's permission; the two differ"
           [[:system alice-own] [:system alice-own]]
           [[(record/who-of personal-depot) (record/permission-of personal-depot)]
            [(record/who-of personal-permission) (record/permission-of personal-permission)]])
    (shown "a value of each kind, kept plain in its row by the streaming gate and read back:"
           (pr-str (into (sorted-map) (map (fn [[k row]] [k (record/read-value row)])) rows)))
    (shown "the act in the depot's log keeps the sealed bytes and the lock ids, and no plain value: for the first fact" (alength ^bytes (:sealed (first (:facts logged))))
           "sealed bytes in the log," (count (value-bytes (get rows (first (keys values))))) "plain bytes in the row; the row names no lock, and the lock's row names the fact")
    (shown "who wrote a fact, and under which permission:" (pr-str [(record/who-of graces-row) (record/permission-of graces-row)])
           "| the system's making of a personal layer:" (pr-str [(record/who-of personal-depot) (record/permission-of personal-depot)]))
    (shown "an act refused at its second seal leaves no row and no used lock:" (told refused) "| an unsealed value:" (:word unsealed))))

(deftest the-keys-that-take-a-second-write
  (let [more-locks (ask-for-locks! :team :alice alice-in-team 4)
        team (fn [facts] (thing :team :alice alice-in-team facts))
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
                     elsewhere (decide! (team [{:e :plan :k k :v v1}]))
                     sealed-one (decide! (team [(sealed :team :team k v1)]))]
                 {:before before :starts-with (first-act/starts-with :team k)
                  :words (mapv :word [first-write second-write named stale retract elsewhere sealed-one])
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
                        :setting-not-on-the-layer :store-key-sealed]]
    (check "the hand has locks in the layer :team" :accepted (:word more-locks))
    (check "before any fact says it, the place of each of the three keys holds no row, and the layer reads as: seen by its members, building on nothing, marking no key"
           [[nil nil nil] [:members [] #{}]]
           [(mapv :before [seen listed marked]) (mapv :starts-with [seen listed marked])])
    (check "who may see a layer: written, written a second time, written again naming the fact that is there; then a stale replaces, a retract, another entity and a sealed value are each refused with a recorded word"
           expected-words (:words seen))
    (check "what a layer builds on: the same" expected-words (:words listed))
    (check "the keys that a layer marks: the same" expected-words (:words marked))
    (check "after each accepted write the place holds that write's value, by that act; after the four refusals it holds the last accepted one"
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
    (shown "the three keys that take a second write, in a streaming layer. For each: written, written again, written again naming the fact there, a stale replaces, a retract, another entity, sealed:"
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
        asks (ask-for-locks! :canvas :bob bob-sees 2)
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
    (check "she cannot give: a give is an act; and her request for locks is refused with the same word and makes no lock"
           [:permission-does-not-allow-acting nil :permission-does-not-allow-acting {}]
           [(:word gives) (fact-row :canvas :erin :softland.store/permission) (:word asks) (:locks asks)])
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
           "| gives:" (:word gives) "| asks for locks:" (:word asks))
    (shown "see stays see: a second give of see and act" (:word raise)
           "| a holder of see and act acts and gives:" (:word carol-writes) (:word carol-gives) "| a making act whose permission has the slot:" (:word slotted-making))))

(deftest what-points-at-a-thing
  (let [u (UUID/randomUUID)
        place (fn [e] (fact-row :canvas e :on))
        writes! (fn [e v & {:as more}] (decide! (canvas-act [(merge (sealed :canvas e :on v) more)])))
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
        stale (decide! (canvas-act [(assoc (sealed :canvas :b3 :on :never-written) :replaces [(new-act-id :canvas) 0])]))
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
        both (decide! (assoc both-base :facts [{:e :canvas :k marks :v #{:on :in}} (sealed :canvas :b5 :in :canvas-1)]))
        in-since (get (marked-keys :canvas) :in)
        after-both [(run-of :canvas :in in-since :canvas-1) (:stamp (fact-row :canvas :b5 :in))]
        later (decide! (canvas-act [(sealed :canvas :b6 :in :canvas-1)]))
        after-later (run-of :canvas :in in-since :canvas-1)
        unmarked (decide! (canvas-act [(sealed :canvas :b1 :text :canvas-1)]))
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
              :who ":alice" :permission (text/text alice-in-canvas)}]]
           run-1)
    (check "a fact whose value is a UUID: the run of that thing holds its place" [:b2] run-2)
    (check "the fact is replaced, naming what it replaces: its place is out of the run of the value before, the key of that entry holds no row, and it is in the run of the new value beside the other place"
           [[] nil [:b1 :b2]] after-3)
    (check "the fact is written over with no replaces: the same, out of one run and into another"
           [[:b2] nil [:b1]] after-4)
    (check "the same value written again: the entry is written over, and holds the later fact"
           [[(record/by-value-key :canvas :on s1 (record/value-digest-of :canvas-2) :b1)
             {:by-act (nth (:act-id (:thing a5)) 2) :by-index 0 :stamp (:stamp (:answer a5))
              :who ":alice" :permission (text/text alice-in-canvas)}]]
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
        request (ask-for-base-locks! :system system-own 16)
        request-uuid (nth (:request request) 2)
        in-hand (first (get @locks-in-hand :base))
        e (first three)
        fact (sealed :base e :title "the first text of the base")
        landed (decide-in-base! (system-act [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (base-fact e :title)
        lock-key (base-lock-key (:lock-id in-hand))
        beside (base-row :mb-locks-ps e lock-key)
        at-home (base-row :mb-locks-ps request-uuid lock-key)
        by-task (rows-by-task :mb-locks-ps lock-key)
        home-task (task-of-row :mb-answers-ps (record/answer-key :base request-uuid))
        read-after (locks-of (:request request) 0 255)
        never-sent (answer-of (new-act-id :base))]
    (swap! kept assoc :base-landed landed :base-landed-lock in-hand :base-entities three :base-task-of-entity task-of-entity)
    (check "eight acts of one batch on eight entities are each accepted, and their facts are on more than one task"
           [(vec (repeat 8 :accepted)) true] [(mapv :word seen) (< 1 (count (set (vals task-of-entity))))])
    (check "a request for 16 locks in the base is accepted, and one query gives its 16 locks, each 32 bytes of way 1, no two the same"
           [:accepted 16 #{32} #{1} 16]
           [(:word request) (count (:locks request)) (set (map #(alength ^bytes (:bytes %)) (vals (:locks request))))
            (set (map :way (vals (:locks request)))) (count (set (map #(vec (:bytes %)) (vals (:locks request)))))])
    (check "the request's fact, its answer and its locks are on one task, the task of its uuid"
           [home-task home-task]
           [(task-of-row :mb-facts-ps (record/place-key :base request-uuid :softland.store/lock-request))
            (task-of-row :mb-locks-ps (record/lock-key :base request-uuid 15))])
    (check "one act lands through the microbatch gate and is read back by act-id: accepted, with the digest of what was sent"
           [:appended :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the value as a String, the text itself of its plain bytes, and gives those bytes back; the fact that wrote it, who wrote it and the permission that the act named, the stamp of the answer, and no lock"
           ["\"the first text of the base\"" (vec (plain-bytes "the first text of the base")) act-uuid 0 ":system" (text/text system-own) (:stamp (:answer landed)) false false]
           [(:value the-row) (value-bytes the-row) (:by-act the-row) (:by-index the-row) (:who the-row) (:permission the-row) (:stamp the-row)
            (contains? the-row :lock-act) (contains? the-row :lock-index)])
    (check "the lock's row beside the value, on the value's task, has the bytes that the door read and names the fact that it seals"
           [(vec (:bytes (:lock in-hand))) 1 act-uuid 0]
           [(vec (:bytes beside)) (:way beside) (:seals-act beside) (:seals-index beside)])
    (check "the lock's row at its home names the fact, and holds no bytes unless the home is the value's task"
           [act-uuid 0 (= home-task (task-of-entity e))]
           [(:seals-act at-home) (:seals-index at-home) (contains? at-home :bytes)])
    (check "one copy of the lock's bytes: over every task, one row under the lock's key holds bytes"
           1 (count (filter #(contains? % :bytes) (vals by-task))))
    (check "the lock's bytes, beside the value, open the sealed bytes that the hand sent, and what they open is the row's value"
           [(vec (plain-bytes "the first text of the base")) true]
           (let [opened (seal/open (record/lock-of beside) [:base e :title] (:sealed fact))]
             [(vec opened) (= (vec opened) (value-bytes the-row))]))
    (check "the read of the request's locks then gives the used lock with its way and no bytes, and each other lock with its bytes"
           [{:bytes nil :way 1} 15]
           [(get read-after (second (:lock-id in-hand))) (count (filter :bytes (vals read-after)))])
    (check "an act-id of the base that was never sent has no answer: nothing yet" nil never-sent)
    (shown "a request for 16 locks in the base, read with one query:" (told request) "| locks" (count (:locks request)))
    (shown "one act landing through the microbatch gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "a value of the base kept plain in its row, with one copy of its lock's bytes: the value on task" (task-of-entity e)
           "| rows under the lock's key, by task, with bytes?" (pr-str (into (sorted-map) (map (fn [[task row]] [task (contains? row :bytes)])) by-task))
           "| the lock's home is task" home-task "| the row's value:" (pr-str (record/read-value the-row))
           "| what the lock opens of the sealed bytes that were sent:" (String. ^bytes (seal/open (record/lock-of beside) [:base e :title] (:sealed fact)) "UTF-8"))))

(defn- seen-before-closing
  "Sends one request for a lock, alone, and watches the answers table
  itself, round the reads, for the row of its answer. At the first sight of
  the row it selects the row of the new lock and the `:closed-through` of
  the task, then reads the answer and the locks as a door does. Gives
  {:thing :row :lock-row :closed-through :read :locks-read}."
  []
  (let [request-thing (system-act [])
        act-id (:act-id request-thing)
        uuid (nth act-id 2)
        x (assoc request-thing :facts [{:e uuid :k :softland.store/lock-request :v 1}])
        _ (append-to-base! x)
        deadline (+ (System/currentTimeMillis) time-limit-ms)
        row (loop []
              (or (base-row :mb-answers-ps uuid (record/answer-key :base uuid))
                  (when (< (System/currentTimeMillis) deadline)
                    (Thread/sleep 1)
                    (recur))))
        lock-row (base-row :mb-locks-ps uuid (record/lock-key :base uuid 0))
        closed (closed-through uuid)]
    {:thing x :row row :lock-row lock-row :closed-through closed
     :read (answer-of act-id) :locks-read (locks-of act-id 0 255)}))

(deftest an-answer-is-readable-when-its-batch-is-closed
  (let [tries (loop [n 1]
                (let [seen (seen-before-closing)
                      result (do (wait-until-processed!) (base-result (:thing seen) :appended))]
                  (note-base-answers! [result])
                  (if (or (and (:row seen) (nil? (:read seen))) (<= 5 n))
                    (assoc seen :after result :tries n)
                    (recur (inc n)))))
        {:keys [thing row lock-row read locks-read after]} tries
        act-id (:act-id thing)
        uuid (nth act-id 2)]
    (check "the row of the answer and the row of its new lock are in the tables, the answer's stamp later than its task's closed-through"
           [true true true]
           [(some? row) (some? (:bytes lock-row))
            (boolean (and row (or (nil? (:closed-through tries)) (< (:closed-through tries) (:stamp row)))))])
    (check "and the reads give nothing of them: no answer by act-id, and no lock: the batch is not closed" [nil {}] [read locks-read])
    (check "after the next run of the gate the read gives the answer, with the stamp of that row, and closed-through is not below it"
           [(:stamp row) :accepted true]
           [(:stamp (:answer after)) (:word after) (<= (:stamp row) (closed-through uuid))])
    (check "and the read of locks gives the lock, with the bytes that were in the table"
           (vec (:bytes lock-row)) (vec (:bytes (get (locks-of act-id 0 255) 0))))
    (shown "an answer is not readable before its batch is closed: the row is in the table with stamp" (:stamp row)
           "| closed-through on its task" (:closed-through tries) "| answer-by-act-id gives" (pr-str read)
           "| locks-of-request gives" (pr-str locks-read) "although the lock's row is in the table"
           "| and is readable after:" (told after) "| closed-through" (closed-through uuid) "| caught at try" (:tries tries))))

(deftest refusals-and-repeats-in-the-base
  (let [landed (:base-landed @kept)
        accepted-thing (:thing landed)
        e (:e (first (:facts accepted-thing)))
        task-of-entity (:base-task-of-entity @kept)
        [e1 e2] (:base-entities @kept)
        [unsealed stale] (decide-in-one-batch!
                          [(system-act [{:e :doc-r1 :k :text :v "a plain value"}])
                           (system-act [(assoc (sealed :base e :title "stale") :replaces [(new-act-id :base) 0])])])
        answer-before (answer-of (:act-id accepted-thing))
        row-before (base-fact e :title)
        last-stamp-before (gates-last-stamp)
        _ (append-to-base! accepted-thing)
        _ (append-to-base! (:thing unsealed))
        _ (wait-until-processed!)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (answer-of (:act-id (:thing unsealed)))
        row-after (base-fact e :title)
        other-under-used (decide-in-base! (assoc (system-act [{:e :doc-r2 :k :text}]) :act-id (:act-id accepted-thing)))
        twice-a (system-act [{:e e1 :k :twice}])
        twice-b (assoc (system-act [{:e e2 :k :twice}]) :act-id (:act-id twice-a))
        [two-a two-b] (decide-in-one-batch! [twice-a twice-b])
        written-a (base-fact e1 :twice)
        written-b (base-fact e2 :twice)]
    (check "an unsealed value is refused through the microbatch gate, and the refusal is recorded with its reason, a stamp and the digest"
           [:refused :not-sealed true true]
           [(:verdict (:answer unsealed)) (:reason (:answer unsealed)) (int? (:stamp (:answer unsealed))) (:mine? unsealed)])
    (check "a refusal writes nothing at the place" nil (base-fact :doc-r1 :text))
    (check "a stale replaces is refused through the microbatch gate, and the place is as it was"
           [:replaced-fact-not-current (:stamp (:answer landed))] [(:word stale) (:stamp row-before)])
    (check "a repeat is answered from the record: the same answer, with the stamp of the first decision"
           answer-before answer-after)
    (check "not decided twice: the row of its place is the row of the first decision"
           [(:stamp row-before) (:by-act row-before) (value-bytes row-before)]
           [(:stamp row-after) (:by-act row-after) (value-bytes row-after)])
    (check "a repeat of a refused act reads the recorded refusal" (:answer unsealed) refused-again)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false] [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (base-fact :doc-r2 :text))
    (check "the two things of one act-id arrive on two tasks" true (not= (task-of-entity e1) (task-of-entity e2)))
    (check "one act-id under two different things in one batch: one answer, which is of one of the two"
           [true #{true false}] [(= (:answer two-a) (:answer two-b)) (set [(:mine? two-a) (:mine? two-b)])])
    (check "the thing that has the answer wrote its fact, and nothing of the other is written"
           [(:mine? two-a) (:mine? two-b)] [(some? written-a) (some? written-b)])
    (shown "a refusal through the microbatch gate, recorded with its reason:" (pr-str (:answer unsealed)))
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
        first-value (decide-in-base! (system-act [(sealed :base ea :order "the fact before")]))
        f0 [(:act-id (:thing first-value)) 0]
        [a b] (decide-in-one-batch!
               [(system-act [(assoc (sealed :base ea :order "from the first act") :replaces f0)])
                (system-act [{:e eb :k :order-other} (assoc (sealed :base ea :order "from the second act") :replaces f0)])])
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
        dave-later (decide-in-base! (thing :base :dave dave-in-base [{:e :dave :k :note}]))
        one-lock (take-lock! :base)
        under-one-lock (decide-in-one-batch! [(system-act [(seal-with one-lock :base ea :under-one-lock "a")])
                                              (system-act [(seal-with one-lock :base eb :under-one-lock "b")])])]
    (check "the two entities are on two tasks" true (not= (task-of-entity ea) (task-of-entity eb)))
    (check "two acts of one batch that replace the same fact, arriving on two tasks: one is accepted, the other refused as stale"
           [:accepted :replaced-fact-not-current] [(:word winner) (:word loser)])
    (check "the place holds the winner's fact, with the winner's stamp"
           [(nth (:act-id (:thing winner)) 2) (:stamp (:answer winner))] [(:by-act place) (:stamp place)])
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
    (check "two acts of one batch under one lock, on two tasks: one seals with it, the other is refused"
           #{:accepted :lock-already-seals-a-value} (set (map :word under-one-lock)))
    (shown "two acts of one batch that replace the same fact, with one winner: on tasks" (task-of-entity ea) "and" (task-of-entity eb)
           "|" (told winner) "|" (told loser) "| the place is written by" (short-id [nil nil (:by-act place)]))
    (shown "two acts of one batch that write one place and name nothing:" (told first-mark) "|" (told second-mark) "| the place holds stamp" (:stamp mark))
    (shown "a give and an act under it in one batch, in both orders:" (:word give-carol) (:word carol-acts) "|" (:word dave-acts) (:word give-dave)
           "| two acts under one lock:" (pr-str (mapv :word under-one-lock)))))

(deftest a-person-acts-in-the-base
  (let [give (decide-in-base! (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}]))
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission))
        asks (ask-for-base-locks! :alice alice-in-base 2)
        writes (decide-in-base! (thing :base :alice alice-in-base [(sealed :base :alices-doc :text "alice's text in the base")]))
        [give-again end-it depot-again depot-other setting-elsewhere give-bob unknown-key tools-own]
        (decide-in-one-batch!
         [(system-act [{:e :alice :k :softland.store/permission :v alice-in-base}])
          (system-act [{:e :alice :k :softland.store/permission}])
          (system-act [{:e :base :k :softland.store/depot :v :microbatch}])
          (system-act [{:e :base :k :softland.store/depot :v :streaming}])
          (system-act [{:e :team :k :softland.store/depot :v :microbatch}])
          (thing :base :alice alice-in-base [{:e :bob :k :softland.store/permission :v bob-in-base}])
          (system-act [{:e :base :k :softland.store/owner :v :system}])
          (system-act [(sealed :base :alice :permission "a tool's own permission")])])
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
    (check "a person acts in the base under the permission that the system gave: a request for locks, and a sealed value"
           [:accepted 2 :accepted true] [(:word asks) (count (:locks asks)) (:word writes) (:mine? writes)])
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
           (count permission-rows) "tasks | her request for locks:" (told asks) "| her act:" (told writes))
    (shown "in the base, an act under a permission that does not stand:" (:word not-given) "| by a who that is not the holder:" (:word not-holder)
           "| a permission given beside the giver's own:" (:word beside))
    (shown "a permission's place takes one write in the base:" (:word give-again) "| ending a permission:" (:word end-it)
           "| the depot again:" (:word depot-again) (:word depot-other) "| a setting key on another entity:" (:word setting-elsewhere)
           "| a permission beneath a person's, and an act under it:" (:word give-bob) (:word bob-acts))
    (shown "in the base, a key in softland.store that the store does not know:" (:word unknown-key)
           "| a tool's own fact under the bare word :permission:" (:word tools-own) "| its row is on" (count tools-own-rows)
           "task, the store's permission of the same holder on" (count permission-rows-after))
    (shown "a carried stamp too far ahead, in the base:" (told far-ahead) "| the same with no standing:" (:word far-ahead-no-standing))))

(deftest a-long-batch
  (let [n 400
        entity (fn [i] (keyword (str "long-" (mod i 20))))
        k (fn [i] (keyword (str "k" i)))
        results (decide-in-one-batch! (mapv (fn [i] (system-act [{:e (entity i) :k (k i)}])) (range n)))
        stamps (mapv #(:stamp (:answer %)) results)
        sample (range 0 n 37)]
    (check "a batch of 400 acts on 20 entities: each is accepted, each with a stamp of its own"
           [#{:accepted} n] [(set (map :word results)) (count (set stamps))])
    (check "the acts on one entity keep the depot's order: their stamps go up in the order sent"
           true (every? (fn [[_ sent]] (apply < (map second sent))) (group-by #(entity (first %)) (map-indexed vector stamps))))
    (check "the fact of each act of a sample is on its place, with the stamp of its answer"
           (mapv #(nth stamps %) sample) (mapv #(:stamp (base-fact (entity %) (k %))) sample))
    (shown "a batch of" n "acts on 20 entities: each accepted, with" (count (set stamps))
           "stamps; on each entity the stamps go up in the order sent")))

(deftest an-act-on-several-tasks
  (let [task-of-entity (:base-task-of-entity @kept)
        entities (:base-entities @kept)
        request-thing (system-act [])
        request (:act-id request-thing)
        request-uuid (nth request 2)
        asked (decide-in-base! (assoc request-thing :facts [{:e request-uuid :k :softland.store/lock-request :v 3}]))
        door-locks (locks-of request 0 255)
        locks (mapv (fn [i] {:lock-id [request i] :lock (get door-locks i)}) (range 3))
        places (mapv (fn [e] [e :body]) entities)
        facts (fn [] (mapv (fn [lock [e k]] (seal-with lock :base e k (str "the body of " (name e)))) locks places))
        place-rows (fn [] (mapv (fn [[e k]] (rows-by-task :mb-facts-ps (record/place-key :base e k))) places))
        lock-rows (fn [] (mapv (fn [lock] (rows-by-task :mb-locks-ps (base-lock-key (:lock-id lock)))) locks))
        home-task (task-of-row :mb-answers-ps (record/answer-key :base request-uuid))
        refused (decide-in-base! (system-act (update (facts) 2 assoc :replaces [(new-act-id :base) 0])))
        places-after-refusal (place-rows)
        locks-after-refusal (lock-rows)
        earlier-stamps (keep (fn [[e task]] (when (contains? (set (map task-of-entity entities)) task) (:stamp (base-fact e :seen))))
                             task-of-entity)
        last-stamp-before (gates-last-stamp)
        sent-facts (facts)
        accepted (decide-in-base! (system-act sent-facts))
        act-uuid (nth (:act-id (:thing accepted)) 2)
        stamp (:stamp (:answer accepted))
        places-after (place-rows)
        locks-after (lock-rows)
        beside (mapv (fn [rows [e _]] (get rows (task-of-entity e))) locks-after places)
        at-home (mapv (fn [rows] (get rows home-task)) locks-after)
        last-stamp-after (gates-last-stamp)
        [arrives-on other-entity] entities
        planted (decide-in-base! (system-act [{:e other-entity :k :crossed}]))
        crossing (decide-in-base! (system-act [{:e arrives-on :k :crossing}
                                               {:e other-entity :k :crossed :replaces [(:act-id (:thing planted)) 0]}]))
        crossed-row (base-fact other-entity :crossed)
        crossing-again (decide-in-base! (system-act [{:e arrives-on :k :crossing-again}
                                                     {:e other-entity :k :crossed :replaces [(:act-id (:thing planted)) 0]}]))]
    (swap! kept assoc :base-values {:places places :locks locks :sent sent-facts :act-uuid act-uuid})
    (check "the three entities of the act are on more than one task" true (< 1 (count (set (map task-of-entity entities)))))
    (check "the request for its three locks is accepted" [:accepted 3] [(:word asked) (count door-locks)])
    (check "an act that touches several tasks, with its third fact stale, is refused whole" :replaced-fact-not-current (:word refused))
    (check "no row of it is on any task: none of its three places holds a fact" [{} {} {}] places-after-refusal)
    (check "its three locks are still free: each is one row, at its home, with its bytes, and names no fact"
           (mapv (fn [lock] [[home-task] (vec (:bytes (:lock lock))) false]) locks)
           (mapv (fn [rows] [(vec (keys rows)) (vec (:bytes (first (vals rows)))) (record/seals-a-fact? (first (vals rows)))])
                 locks-after-refusal))
    (check "the same act with each fact good is accepted whole" [:accepted true] [(:word accepted) (:mine? accepted)])
    (check "each fact's row is on the task of its entity and no other, written by the act, with the stamp of its answer"
           (mapv (fn [[e _] i] [[(task-of-entity e)] act-uuid i stamp]) places (range))
           (mapv (fn [rows] (let [row (first (vals rows))] [(vec (keys rows)) (:by-act row) (:by-index row) (:stamp row)])) places-after))
    (check "each lock's row with its bytes is beside its value, and names the fact"
           (mapv (fn [lock i] [(vec (:bytes (:lock lock))) act-uuid i]) locks (range))
           (mapv (fn [row] [(vec (:bytes row)) (:seals-act row) (:seals-index row)]) beside))
    (check "each lock's row at its home names the fact, and holds no bytes unless the home is the value's task"
           (mapv (fn [[e _] i] [act-uuid i (= home-task (task-of-entity e))]) places (range))
           (mapv (fn [row] [(:seals-act row) (:seals-index row) (contains? row :bytes)]) at-home))
    (check "one copy of each lock's bytes over every task, and at least one lock's home is another task than its value's"
           [[1 1 1] true]
           [(mapv (fn [rows] (count (filter #(contains? % :bytes) (vals rows)))) locks-after)
            (boolean (some (fn [rows] (< 1 (count rows))) locks-after))])
    (check "each lock's bytes, beside its value, open the sealed bytes that the hand sent, and what they open is the row's value"
           (mapv (fn [[e _]] [(vec (plain-bytes (str "the body of " (name e)))) true]) places)
           (mapv (fn [row [e k] rows fact]
                   (let [opened (seal/open (record/lock-of row) [:base e k] (:sealed fact))]
                     [(vec opened) (= (vec opened) (value-bytes (first (vals rows))))]))
                 beside places places-after sent-facts))
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
    (shown "an act that touches several tasks accepted whole:" (told accepted) "| its facts on tasks" (pr-str (mapv #(vec (keys %)) places-after))
           "| each lock's bytes beside its value, rows under each lock's key by task with bytes?"
           (pr-str (mapv (fn [rows] (into (sorted-map) (map (fn [[task row]] [task (contains? row :bytes)])) rows)) locks-after))
           "| the locks' home is task" home-task)
    (shown "one refused whole with no row of it on any task:" (told refused) "| rows at its three places:" (pr-str places-after-refusal)
           "| its three locks still free at their home")
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
        strays-after (base-strays)]
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
    (shown "rule 7 in both directions: an act for a microbatch layer sent to the streaming depot:" (told base-on-streaming)
           "| an act for a streaming layer sent to the microbatch depot:" (told alice-on-microbatch)
           "| for a layer that is not made:" (:word nobody-on-microbatch))
    (shown "one act-id on both depots has one answer:" (told on-streaming) "| strays of the microbatch gate, by word:"
           (pr-str (merge-with - strays-after strays-before)) "| the same stray sent twice counts once")))

;; ------------------------------------ what the microbatch gate keeps for a read

(deftest what-a-fact-keeps-in-the-base
  (let [request (ask-for-base-locks! :system system-own 64)
        u (UUID/randomUUID)
        values (assoc kinds-of-value :uuid u)
        facts (mapv (fn [[k v]] (sealed :base :kept-doc k v)) values)
        landed (decide-in-base! (system-act facts))
        act-uuid (nth (:act-id (:thing landed)) 2)
        rows (into {} (map (fn [k] [k (base-fact :kept-doc k)])) (keys values))
        logged (record-of-act (stored-in-base) (:act-id (:thing landed)))
        beside (mapv (fn [fact] (base-row :mb-locks-ps :kept-doc (base-lock-key (:lock-id fact)))) facts)
        lock-a (take-lock! :base)
        lock-b (take-lock! :base)
        refused (decide-in-base! (system-act [(seal-with lock-a :base :not-kept-doc :text "opened, and then dropped")
                                              (assoc (seal-with lock-b :base :not-kept-doc-2 :text "x") :sealed (byte-array 40))]))
        alices-row (base-fact :alices-doc :text)
        retract (decide-in-base! (thing :base :alice alice-in-base [{:e :alices-doc :k :gone}]))
        retract-row (base-fact :alices-doc :gone)
        give-row (base-row :mb-facts-ps :alice (record/place-key :base :alice :softland.store/permission))]
    (check "a request for 64 locks in the base, and an act with a value of each kind on one entity, are accepted"
           [:accepted 64 :accepted true] [(:word request) (count (:locks request)) (:word landed) (:mine? landed)])
    (check "each row of the base holds its value plain: it reads back as the value that the maker gave"
           (into {} (map (fn [[k v]] [k [:value v]])) values)
           (into {} (map (fn [[k row]] [k (record/read-value row)])) rows))
    (check "each row holds the plain bytes that the hand sealed, and no field of a lock"
           (mapv (fn [[_ v]] [(vec (plain-bytes v)) false false]) values)
           (mapv (fn [[k _]] (let [row (get rows k)] [(value-bytes row) (contains? row :lock-act) (contains? row :lock-index)])) values))
    (check "the act in the microbatch depot's log is as the hand sent it: each fact holds the sealed bytes and its lock id, and no plain value"
           (mapv (fn [fact] [(vec (:sealed fact)) (:lock-id fact) false]) facts)
           (mapv (fn [fact] [(vec (:sealed fact)) (:lock-id fact) (contains? fact :v)]) (:facts logged)))
    (check "the whole row of each lock is beside its value, names the fact, and opens the log's sealed bytes to the row's bytes"
           (mapv (fn [i] [act-uuid i true]) (range (count facts)))
           (mapv (fn [i [k _] the-lock]
                   [(:seals-act the-lock) (:seals-index the-lock)
                    (= (vec (seal/open (record/lock-of the-lock) [:base :kept-doc k] (:sealed (nth (:facts logged) i))))
                       (value-bytes (get rows k)))])
                 (range) values beside))
    (check "an act whose first value opens and whose second does not is refused whole: no row at either place on any task, and both locks are free"
           [:lock-does-not-open-the-value {} {} false false]
           [(:word refused) (rows-by-task :mb-facts-ps (record/place-key :base :not-kept-doc :text))
            (rows-by-task :mb-facts-ps (record/place-key :base :not-kept-doc-2 :text))
            (boolean (some record/seals-a-fact? (vals (rows-by-task :mb-locks-ps (base-lock-key (:lock-id lock-a))))))
            (boolean (some record/seals-a-fact? (vals (rows-by-task :mb-locks-ps (base-lock-key (:lock-id lock-b))))))])
    (check "each row holds who wrote it and the permission that the act named: the system's rows the system and its first permission; a person's rows her and the permission that the system gave her; a retract's row too; the give's row the giver"
           [#{[:system system-own]} [:alice alice-in-base [:value "alice's text in the base"]] :accepted [:alice alice-in-base [:nothing-here]]
            [:system system-own [:value alice-in-base]]]
           [(set (map (fn [row] [(record/who-of row) (record/permission-of row)]) (vals rows)))
            [(record/who-of alices-row) (record/permission-of alices-row) (record/read-value alices-row)]
            (:word retract)
            [(record/who-of retract-row) (record/permission-of retract-row) (record/read-value retract-row)]
            [(record/who-of give-row) (record/permission-of give-row) (record/read-value give-row)]])
    (shown "a value of each kind, kept plain in its row by the microbatch gate and read back:"
           (pr-str (into (sorted-map) (map (fn [[k row]] [k (record/read-value row)])) rows)))
    (shown "the act in the microbatch depot's log keeps the sealed bytes and the lock ids; who wrote a fact of the base, and under which permission:"
           (pr-str [(record/who-of alices-row) (record/permission-of alices-row)])
           "| an act refused at its second seal leaves no row:" (told refused))))

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
                  (system-act [(sealed :base :base who-may-see :everyone)])
                  (system-act [(sealed :base :base builds-on [:team])])
                  (system-act [(sealed :base :base marks #{:tag})])
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
    (check "a stale replaces, a retract, another entity, a sealed value and a wrong value are each refused with a recorded word, for each of the three keys"
           (vec (concat (repeat 3 :replaced-fact-not-current) (repeat 3 :store-key-retract-not-built)
                        (repeat 3 :setting-not-on-the-layer) (repeat 3 :store-key-sealed)
                        [:who-may-see-fact-not-members-or-everyone :builds-on-fact-not-a-list-of-layers :builds-on-too-many-layers
                         :marked-keys-fact-not-a-set-of-keys :too-many-marked-keys :marked-key-in-the-stores-namespace]))
           (mapv :word refused))
    (check "after those eighteen refusals every task holds the rows that the last accepted writes left, with the same stamps"
           (mapv (fn [by-task] (into {} (map (fn [[task row]] [task (:stamp row)])) by-task)) after-named)
           (mapv (fn [by-task] (into {} (map (fn [[task row]] [task (:stamp row)])) by-task)) after-refused))
    (check "the row of the marked keys holds on every task, derived by the gate, the stamp of each marked key: the last stamp of the batch that marked it; none when the set is empty; a new one when the key is marked again"
           [#{{:tag end-of-first}} #{{}} #{{:tag end-of-named}} true]
           [(set (map record/marked-keys-of (vals (nth after-first 2)))) (set (map record/marked-keys-of (vals (nth after-second 2))))
            (set (map record/marked-keys-of (vals (nth after-named 2)))) (< end-of-first end-of-named)])
    (check "the stamp of a newly marked key is not below the stamp of any act of its batch"
           true (every? #(<= (:stamp (:answer %)) end-of-first) firsts))
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
           "| after the key was marked again" (pr-str (set (map record/marked-keys-of (vals (nth after-named 2))))))))

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
        asks (ask-for-base-locks! :erin erin-sees 2)
        greta-writes (decide-in-base! (thing :base :greta gretas [{:e :gretas-doc :k :note}]))
        permission-rows-after (rows-by-task :mb-facts-ps (record/place-key :base :erin :softland.store/permission))]
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
    (check "she cannot give, and her request for locks is refused with the same word and makes no lock"
           [:permission-does-not-allow-acting {} :permission-does-not-allow-acting {}]
           [(:word gives) (rows-by-task :mb-facts-ps (record/place-key :base :frank :softland.store/permission)) (:word asks) (:locks asks)])
    (check "a second give to her, of see and act, is refused: a permission's place takes one write, and her rows are as they were"
           [:store-key-takes-one-write (into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows)]
           [(:word raise) (into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows-after)])
    (check "a permission that says see and act, given in the same batch: its holder acts in a later batch" [:accepted :accepted]
           [(:word give-greta) (:word greta-writes)])
    (shown "in the base, a permission that says see:" (pr-str (record/value-of (first (vals permission-rows)))) "on" (count permission-rows) "tasks"
           "| its holder acts in the batch of the give:" (:word in-the-same-batch) "| in a later batch:" (told acts)
           "| names it without the slot:" (:word names-less) "| with see and act:" (:word names-more) "| gives:" (:word gives)
           "| asks for locks:" (:word asks) "| a second give of see and act:" (:word raise))))

(deftest what-points-at-a-thing-in-the-base
  (let [task-of-entity (:base-task-of-entity @kept)
        [e1 e2 e3] (:base-entities @kept)
        u (UUID/randomUUID)
        place (fn [e] (base-fact e :on))
        writes! (fn [e v & {:as more}] (decide-in-base! (system-act [(merge (sealed :base e :on v) more)])))
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
        a11 (decide-in-base! (system-act [{:e e1 :k :beside} (sealed :base e2 :on :canvas-5)]))
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
            {:by-act (nth (:act-id (:thing a1)) 2) :by-index 0 :stamp (:stamp (:answer a1)) :who ":system" :permission (text/text system-own)}]
           [(vec (sort-by str (map (fn [[k _]] (record/by-value-entity k)) run-1)))
            (apply = tasks-of-run-1) (= 1 (count (first tasks-of-run-1)))
            (some (fn [[k row]] (when (= e1 (record/by-value-entity k)) row)) run-1)])
    (check "the fact is replaced, naming what it replaces, with a UUID: its place is out of the run of the value before, no task holds a row under the key of that entry, and it is in the run of the UUID"
           [[e2] {} [e1]] after-3)
    (check "the fact is written over with no replaces: out of the run of the UUID, no row under that key on any task, into the run of the new value"
           [[] {} [e1]] after-4)
    (check "the same value written again: the entry is written over, and holds the later fact"
           [[(base-entry-key :on s1 :canvas-2 e1)
             {:by-act (nth (:act-id (:thing a5)) 2) :by-index 0 :stamp (:stamp (:answer a5)) :who ":system" :permission (text/text system-own)}]]
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
        (decide-in-one-batch! [(system-act [(sealed :base e2 :on :first-of-two)])
                               (system-act [(sealed :base e2 :on :second-of-two)])])
        place (base-fact e2 :on)
        after-two [(base-run-of :on s2 :canvas-5) (base-run-of :on s2 :first-of-two) (base-run-of :on s2 :second-of-two)
                   (rows-by-task :mb-by-value-ps (base-entry-key :on s2 :first-of-two e2))]
        [marking under-the-new-mark]
        (decide-in-one-batch! [(system-act [{:e :base :k marks :v #{:on :in}}])
                               (system-act [(sealed :base e1 :in :canvas-1)])])
        end-of-marking (gates-last-stamp)
        marked (base-marks-rows)
        in-fact (base-fact e1 :in)
        after-marking (base-run-of :in end-of-marking :canvas-1)
        later (decide-in-base! (system-act [(sealed :base e3 :in :canvas-1)]))
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
  stamp, the count of strays, a permission's row on each task, the bytes
  of the locks of one request, the marked keys on each task with their
  stamps, the run of one value in the facts by value, a value kept
  plain, and the row of the kept clock of the newest batch."
  [request]
  {:answer (answer-of (:act-id (:thing (:base-landed @kept))))
   :depot (depot-of-layer :base)
   :last-stamp (gates-last-stamp)
   :kept-clock (kept-clock-row)
   :strays (base-strays)
   :permission (into (sorted-map) (map (fn [[task row]] [task (:stamp row)])) (rows-by-task :mb-facts-ps (record/place-key :base :alice :softland.store/permission)))
   :marked-keys (into (sorted-map) (map (fn [[task row]] [task (record/marked-keys-of row)])) (base-marks-rows))
   :run (let [{:keys [k since value]} (:base-run @kept)] (base-entries-of k since value))
   :kept-value (record/read-value (base-fact :kept-doc :map))
   :locks (into (sorted-map) (map (fn [[i lock]] [i (some-> (:bytes lock) vec)])) (locks-of request 0 255))})

(deftest the-path-of-a-lock-in-the-base
  (let [used-lock (:base-landed-lock @kept)
        second-value (decide-in-base! (system-act [(seal-with used-lock :base :lock-doc :second "a second value")]))
        request-thing (system-act [])
        request (:act-id request-thing)
        request-uuid (nth request 2)
        asked (decide-in-base! (assoc request-thing :facts [{:e request-uuid :k :softland.store/lock-request :v 2}]))
        door-locks (locks-of request 0 255)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) door-locks)
        _ (append-to-base! (:thing asked))
        _ (wait-until-processed!)
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of request 0 255))
        on-its-home (decide-in-base! (system-act [(seal-with {:lock-id [request 0] :lock (get door-locks 0)}
                                                             :base request-uuid :note "a value on its lock's home")]))
        rows (rows-by-task :mb-locks-ps (record/lock-key :base request-uuid 0))
        the-lock (first (vals rows))
        the-value (base-fact request-uuid :note)
        read-after (locks-of request 0 255)
        new-request (system-act [])
        forged-lock {:lock-id [(:act-id new-request) 0] :lock (seal/new-lock)}
        [requested forged] (decide-in-one-batch!
                            [(assoc new-request :facts [{:e (nth (:act-id new-request) 2) :k :softland.store/lock-request :v 1}])
                             (system-act [(seal-with forged-lock :base :lock-doc :forged "sealed with bytes that the store did not make")])])
        by-stranger (ask-for-base-locks! :mallory alice-in-base 2)
        kept-request (ask-for-base-locks! :system system-own 2)]
    (swap! kept assoc :base-kept-request (:request kept-request))
    (check "a second value under a lock that already seals one is refused, although the lock's row at its home holds no bytes"
           :lock-already-seals-a-value (:word second-value))
    (check "a request sent twice makes no lock again: each lock has the bytes that the first run made"
           [:accepted 2 bytes-before] [(:word asked) (count bytes-after) bytes-after])
    (check "a value on the entity that is its lock's request's uuid, so that the lock's home is the value's task, is accepted"
           :accepted (:word on-its-home))
    (check "one copy of a lock's bytes also then: the one row under the lock's key, on one task, holds the bytes and the fact"
           [1 (get bytes-before 0) (nth (:act-id (:thing on-its-home)) 2) 0]
           [(count rows) (vec (:bytes the-lock)) (:seals-act the-lock) (:seals-index the-lock)])
    (check "and its bytes open the sealed bytes that the hand sent, which are the row's value"
           [(vec (plain-bytes "a value on its lock's home")) true]
           (let [opened (seal/open (record/lock-of the-lock) [:base request-uuid :note] (:sealed (first (:facts (:thing on-its-home)))))]
             [(vec opened) (= (vec opened) (value-bytes the-value))]))
    (check "the read of locks gives that lock with no bytes all the same, and the lock that is not used with its bytes"
           [{:bytes nil :way 1} (get bytes-before 1)] [(get read-after 0) (vec (:bytes (get read-after 1)))])
    (check "a request for locks and a use of its lock in one batch: the request is accepted, and the use is refused"
           [:accepted true]
           [(:word requested) (contains? #{:lock-does-not-open-the-value :lock-not-made-for-this-layer} (:word forged))])
    (check "a refused request for locks in the base makes no lock: the read of locks gives an empty map, and no task holds a row of a lock of it"
           [:who-not-the-permissions-holder {} {}]
           [(:word by-stranger) (:locks by-stranger)
            (rows-by-task :mb-locks-ps (record/lock-key :base (nth (:request by-stranger) 2) 0))])
    (shown "a refused request for locks in the base makes no lock:" (:word by-stranger) (pr-str (:locks by-stranger)))
    (shown "the path of a lock in the base: asked for and read, used, its bytes beside its value and no longer at its home, and then a second value under it refused:"
           (told second-value))
    (shown "one copy of a lock's bytes when the lock's home is its value's task: rows under the lock's key" (count rows)
           "| holds bytes" (contains? the-lock :bytes) "| names the fact" (some? (:seals-act the-lock))
           "| locks-of-request gives it as" (pr-str (get read-after 0)))
    (shown "a request sent twice to the microbatch depot gives the same" (count bytes-after) "locks, byte for byte | a use of a lock in the batch that asks for it:" (:word forged))))

(deftest a-place-of-the-base-never-gives-nothing
  (let [request (ask-for-base-locks! :system system-own 2)
        lock (fn [i] {:lock-id [(:request request) i] :lock (get (:locks request) i)})
        _ (swap! locks-in-hand update :base (fn [held] (vec (remove #(= (:request request) (first (:lock-id %))) held))))
        e :never-empty-doc
        written (decide-in-base! (system-act [(seal-with (lock 0) :base e :text "the text before")]))
        old-uuid (nth (:act-id (:thing written)) 2)
        replacing (system-act [(assoc (seal-with (lock 1) :base e :text "the text after")
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
    (swap! kept assoc :base-before (base-snapshot (:base-kept-request @kept)))
    (check "a fact with a value is written to a place of the base, and an act that names it replaces it: both accepted"
           [:accepted :accepted true] [(:word written) (:word result) (:mine? result)])
    (check "from before the append of the replacing act until its row shows, each select of the place, straight from the facts table on the task of its entity, gave a row and never nothing"
           [true true] [(< 2 (count seen)) (every? some? seen)])
    (check "each row that a select gave was the fact before or the fact after, the first select the one before and the last the one after"
           [#{old-uuid new-uuid} old-uuid new-uuid]
           [(set (map :by-act seen)) (:by-act (first seen)) (:by-act (peek seen))])
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
        request (ask-for-locks! :alice :alice alice-own 2)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (:locks request))
        stamp-before (last-stamp :alice)
        strays-before (strays :act-id-malformed)
        _ (rtest/update-module! (:ipc *hand*) module/Store)
        answer-after (answer-of accepted)
        repeat-ack (send! (:thing landed))
        stamp-after-repeat (last-stamp :alice)
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of (:request request) 0 255))
        with-old-lock (decide! (alice [(sealed :alice :note-40 :text "sealed under a lock from before")]))
        by-bob (decide! (thing :alice :bob bobs [{:e :bobs-note-4 :k :text}]))
        {:keys [since value run marked kept-value]} (:canvas-before @kept)]
    (check "the answer that was read before is the answer after" (:answer landed) answer-after)
    (check "what the gates keep for a read is as it was: a value kept plain, the layer's marked keys with their stamps, and the run of a value in the facts by value"
           [kept-value marked run]
           [(record/read-value (fact-row :canvas :kept :map)) (marked-keys :canvas) (entries-of :canvas :on since value)])
    (check "a repeat after it is answered from the record: the task's last stamp is the one from before"
           [:acked stamp-before] [repeat-ack stamp-after-repeat])
    (check "the depot, the strays and the locks are as they were: each lock has the same bytes"
           [:streaming strays-before bytes-before] [(depot-of-layer :alice) (strays :act-id-malformed) bytes-after])
    (check "a lock that was asked for before seals a value after" :accepted (:word with-old-lock))
    (check "the first stamp after is later than the last stamp before" true (> (:stamp (:answer with-old-lock)) stamp-before))
    (check "a permission that was given before stands after" :accepted (:word by-bob))
    (shown "after the module is started again in the cluster, the record is as it was: the same answer, the same locks;"
           "the last stamp before" stamp-before "the first stamp after" (:stamp (:answer with-old-lock)))))

(deftest the-stamps-and-the-depot
  (let [records (vec (stored))
        partition-of (into {} (keep (fn [[p r]] (when (and (map? r) (contains? r :layer)) [(:layer r) p]))) records)
        made [:alice :team :carrier]
        by-partition (group-by #(get partition-of (:layer %)) @first-answers)
        lock-texts (into #{} (comp (filter #(and (map? %) (vector? (:facts %))))
                                   (map :act-id)
                                   (mapcat (fn [act-id] (vals (locks-of act-id 0 255))))
                                   (map #(text/hex (:bytes %))))
                         @things-sent)
        depot-text (apply str (map (fn [[_ r]] (text/text r)) records))]
    (check "the stamps of the answers increase strictly on each task, in the order in which the acts were sent"
           true (every? (fn [[_ answers]] (apply < (map :stamp answers))) by-partition))
    (check "each stamp is below 2^62 and holds a clock time of today"
           true (every? #(and (< (:stamp %) act/max-carried-stamp)
                              (< (- (System/currentTimeMillis) 3600000) (stamp/time-of (:stamp %)) (+ (System/currentTimeMillis) 60000)))
                        @first-answers))
    (check "the depot holds each thing that was sent: accepted, refused, repeated and stray" (count @things-sent) (count records))
    (check "the layers of the proof are on more than one task" true (< 1 (count (set (vals partition-of)))))
    (check "no lock's bytes are on the depot: the hex of each lock that the door read is in no stored record"
           [true false] [(< 30 (count lock-texts)) (boolean (some #(str/includes? depot-text %) lock-texts))])
    (shown "the stamps of" (count @first-answers) "answers increase strictly on each task; answers by partition:"
           (pr-str (into (sorted-map) (map (fn [[p answers]] [p (count answers)])) by-partition)))
    (shown "layers by depot partition:" (pr-str (select-keys partition-of made)) "| the depot holds all" (count records)
           "things sent | none of" (count lock-texts) "locks' bytes is on the depot")))

(deftest the-base-after-the-module-is-started-again
  (let [before (:base-before @kept)
        request (:base-kept-request @kept)
        landed (:base-landed @kept)
        after (base-snapshot request)
        _ (append-to-base! (:thing landed))
        _ (wait-until-processed!)
        answer-after-repeat (answer-of (:act-id (:thing landed)))
        with-old-lock (decide-in-base! (system-act [(seal-with {:lock-id [request 0] :lock {:bytes (byte-array (get (:locks before) 0)) :way 1}}
                                                               :base :after-doc :text "sealed under a lock from before")]))
        by-alice (decide-in-base! (thing :base :alice alice-in-base [{:e :alices-doc :k :after}]))
        clock-before (:kept-clock before)
        clock-after (kept-clock-row)
        {:keys [places locks sent]} (:base-values @kept)
        opened (mapv (fn [[e k] lock fact]
                       (let [value (value-bytes (base-fact e k))
                             beside (base-row :mb-locks-ps e (base-lock-key (:lock-id lock)))
                             plain (seal/open (record/lock-of beside) [:base e k] (:sealed fact))]
                         [value (and (some? plain) (= (vec plain) value))]))
                     places locks sent)]
    (check "the answer, the depot of the base, the gate's last stamp, the strays, a permission's rows, the locks, the marked keys, a run of the facts by value, a value kept plain and the kept clock of the newest batch are as they were"
           before after)
    (check "the kept clock is one row of a batch's id and a clock; a batch with things after the start keeps its clock under a later id, and the clock is not earlier"
           [true true true true]
           [(clock-record? clock-before) (clock-record? clock-after)
            (boolean (and clock-before clock-after (< (:batch clock-before) (:batch clock-after))))
            (boolean (and clock-before clock-after (<= (:clock clock-before) (:clock clock-after))))])
    (check "a repeat after it is answered from the record" (:answer before) answer-after-repeat)
    (check "a lock of the base that was asked for before seals a value after" :accepted (:word with-old-lock))
    (check "the first stamp after is later than the gate's last stamp before" true (< (:last-stamp before) (:stamp (:answer with-old-lock))))
    (check "a permission that the system gave before stands after" :accepted (:word by-alice))
    (check "each value of the act on several tasks is in its row as the plain bytes of what was sent, and the bytes beside it still open the sealed bytes that were sent, to those bytes"
           (mapv (fn [[e _]] [(vec (plain-bytes (str "the body of " (name e)))) true]) places) opened)
    (shown "after the module is started again in the cluster, the base's record is as it was: the same answer, depot" (:depot after)
           "| the same locks | the gate's last stamp before" (:last-stamp before) "the first stamp after" (:stamp (:answer with-old-lock)))
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
        depot-text (apply str (map (fn [[_ r]] (text/text r)) records))
        lock-texts @lock-bytes-read
        reads (:base-reads @kept)
        clocks (clock-records)
        clock-text (apply str (map (fn [[_ r]] (text/text r)) clocks))
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
    (check "no lock's bytes are on the microbatch depot: the hex of each lock that the door read is in no stored record"
           [true false] [(< 30 (count lock-texts)) (boolean (some #(str/includes? depot-text %) lock-texts))])
    (check "the depot of batch clocks holds records, each a map of exactly a batch's id and a clock, two integers, all on one partition; and no lock's bytes are on it"
           [true true 1 false]
           [(< 30 (count clocks)) (every? (fn [[_ r]] (clock-record? r)) clocks) (count (set (map first clocks)))
            (boolean (some #(str/includes? clock-text %) lock-texts))])
    (check "the row of the kept clock is the first record, in the depot's order, of the batch with the greatest id on that depot"
           (let [newest (reduce max (map (fn [[_ r]] (:batch r)) clocks))]
             (some (fn [[_ r]] (when (= newest (:batch r)) r)) clocks))
           (kept-clock-row))
    (shown "the stamps of" (count answers) "answers of the microbatch gate, in" (count groups) "batches, go up in the order decided;"
           "each of" (count reads) "reads that gave an answer gave one of a batch that was closed at the read")
    (shown "the microbatch depot holds all" (count records) "things sent, on" (count (set (map first records)))
           "partitions | none of" (count lock-texts) "locks' bytes that the door read is on it")
    (shown "the depot of batch clocks holds" (count clocks) "records, each a batch's id and a clock and nothing else, for"
           (count (set (map (fn [[_ r]] (:batch r)) clocks))) "batches, all on partition" (pr-str (set (map first clocks)))
           "| none of those locks' bytes is on it | the kept row is the newest batch's:" (pr-str (kept-clock-row)))))

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

(def ^:private quiet-ms
  "How long the proof waits to see that a quiet time adds no record to the
  depot of batch clocks, in milliseconds."
  1500)

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
  "Puts the three noting functions into the vars of the store's three. What
  must hold: no cluster of this JVM is launched; a launch takes what the
  vars hold then."
  []
  (alter-var-root #'batch/base (constantly noting-base))
  (alter-var-root #'batch/decide-or-send (constantly noting-decide-or-send))
  (alter-var-root #'batch/rows-to-send (constantly noting-rows-to-send)))

(defn- put-the-stores-functions-back!
  "Puts the store's own three functions back into their vars."
  []
  (alter-var-root #'batch/base (constantly first-base))
  (alter-var-root #'batch/decide-or-send (constantly first-decide-or-send))
  (alter-var-root #'batch/rows-to-send (constantly first-rows-to-send)))

(defn- empty-the-hand!
  "Empties each atom of the proof but the shown lines: what the hand keeps
  of one cluster means nothing on the next, and Rama counts the things that
  a gate processed from the launch of a cluster."
  []
  (reset! things-sent [])
  (reset! first-answers [])
  (reset! locks-in-hand {})
  (reset! kept {})
  (reset! base-things-sent [])
  (reset! base-answers [])
  (reset! lock-bytes-read #{})
  (reset! attempts [])
  (reset! throw-asked false)
  (reset! throws-made []))

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

(defn- rows-by-key
  "Takes the rows that an attempt noted, each [which key row]. Gives a map
  from [which key] to the rows noted under it, in the order noted, each with
  its bytes by content. A key that was noted with no row, as the key of a
  new lock is, and the key of a row of the facts by value that goes out,
  holds a nil there: it is a key all the same."
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
    :lock       one row under the key is the whole row that was noted
    :lock-home  each row under the key that holds no bytes is the home's row
                that was noted; where the lock's home is the value's task
                the whole row is the one that stays
    :new-lock   one task holds a row with 32 bytes. Its bytes are drawn at
                the write, so no attempt noted them"
  [which k noted]
  (let [found (fn [pstate] (mapv content (vals (rows-by-task pstate k))))]
    (case which
      :fact (let [rows (found :mb-facts-ps)]
              (and (= (count rows) (if (batch/on-every-task? (nth (record/place-of-key k) 2)) tasks 1))
                   (every? #(= noted %) rows)))
      :answer (= [noted] (found :mb-answers-ps))
      :by-value (= (if noted [noted] []) (found :mb-by-value-ps))
      :lock (= 1 (count (filter #(= noted %) (found :mb-locks-ps))))
      :lock-home (let [rows (found :mb-locks-ps)]
                   (boolean (and (seq rows) (every? #(or (contains? % :bytes) (= noted %)) rows))))
      :new-lock (= [32] (mapv #(count (:bytes %)) (found :mb-locks-ps)))
      false)))

(deftest a-batch-runs-a-second-time
  (let [;; What the batch stands on, each in a batch that runs once: the base,
        ;; locks, a marked key, a fact to replace, a value under the marked
        ;; key, and a person's permission.
        made (decide-in-base! (making :base :system :system))
        request (ask-for-base-locks! :system system-own 16)
        marking (decide-in-base! (system-act [{:e :base :k marks :v #{:on}}]))
        since (gates-last-stamp)
        [fact-before value-before give-alice]
        (decide-in-one-batch! [(system-act [(sealed :base :twice-doc :title "the text before")])
                               (system-act [(sealed :base :twice-pointer :on :canvas-1)])
                               (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}])])
        run-before (mapv first (base-entries-of :on since :canvas-1))
        note-of-the-batch-before (peek @attempts)
        ;; The batch that runs twice. Every act is into the base.
        lock-request (system-act [])
        request-uuid (nth (:act-id lock-request) 2)
        fact-id [(:act-id (:thing fact-before)) 0]
        xs [(system-act [{:e :twice-1 :k :note}])
            (system-act [(sealed :base :twice-2 :text "a sealed value")])
            (system-act [{:e :twice-3 :k :note} (sealed :base :twice-4 :text "a value on a second entity of one act")])
            (thing :base :alice alice-in-base [(sealed :base :twice-5 :text "a person's text")])
            (system-act [(assoc (sealed :base :twice-doc :title "the text after") :replaces fact-id)])
            (system-act [{:e :twice-doc :k :title}])
            (system-act [{:e :twice-holder :k :softland.store/permission :v [:twice-holder :base :base system-own]}])
            (system-act [{:e :twice-seer :k :softland.store/permission :v [:twice-seer :base :base system-own :see]}])
            (assoc lock-request :facts [{:e request-uuid :k :softland.store/lock-request :v 2}])
            (thing :base :mallory [:mallory :base :base system-own] [{:e :twice-6 :k :note}])
            (system-act [{:e :twice-7 :k :plain :v "not sealed"}])
            (system-act [(sealed :base :twice-pointer :on :canvas-2)])
            (system-act [(sealed :base :twice-pointer-2 :on :first-of-two)])
            (system-act [(sealed :base :twice-pointer-2 :on :second-of-two)])
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
        ;; The check must be able to fail: the first note against itself with
        ;; one stamp moved, with one key left out, and with a removal left out.
        first-rows (rows-by-key (:rows note-1))
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
    (check "on a cluster of its own, with three functions of the gate's pure code replaced by ones that give what the store's give: the base is made, locks are asked for, a key is marked, and a fact, a value under the marked key and a person's permission are written, each in a batch that runs once"
           [:accepted :accepted 16 :accepted :accepted :accepted :accepted [(base-entry-key :on since :canvas-1 :twice-pointer)]]
           [(:word made) (:word request) (count (:locks request)) (:word marking) (:word fact-before) (:word value-before)
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
    (check "4. the rows of the two attempts side by side: both made rows under the same keys, and at each key the same row in every field, the stamp too"
           [] side-by-side)
    (check "the batch held what it was sent to hold: rows of each kind were noted, a row of the facts by value that goes out among them; and each thing got the word that it must get, the edge act the word that the 10-second rule gives it against the kept clock"
           [true true true
            [:accepted :accepted :accepted :accepted :accepted :accepted :accepted :accepted :accepted
             :permission-does-not-stand :not-sealed :accepted :accepted :accepted :accepted :accepted :accepted edge-word]
            true]
           [(every? #(pos? (get kinds % 0)) [:fact :answer :by-value :lock :lock-home :new-lock])
            (boolean (some (fn [[[which _] rows]] (and (= :by-value which) (nil? (peek rows)))) noted))
            (boolean (some (fn [[[which _] rows]] (and (= :by-value which) (some? (peek rows)))) noted))
            (mapv :word results)
            (every? :mine? results)])
    (check "5. each row that the tables hold after the batch, selected on every task, is the row that both attempts made: each fact on the task of its entity, each row of a give, of the marks, of the setting and of the list on every task, each answer, each row of the facts by value, and no row under a key that went out"
           [] not-as-noted)
    (check "6. each answer as answer-by-act-id gives it has the verdict, the word and the stamp of the row that both attempts made"
           [] answers-apart)
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

(defn- the-checks-on-the-store-as-defined
  "The checks of the proof's first cluster, in their order: each runs on the
  functions as the store defines them."
  []
  (what-rama-carries)
  (layers-are-made)
  (an-act-lands)
  (refusals-and-repeats)
  (settings-and-permissions)
  (the-path-of-a-lock)
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
  (a-long-batch)
  (an-act-on-several-tasks)
  (each-gate-tells-the-other-gates-layers)
  (what-a-fact-keeps-in-the-base)
  (the-keys-that-take-a-second-write-in-the-base)
  (what-a-permission-allows-in-the-base)
  (what-points-at-a-thing-in-the-base)
  (one-batch-under-a-marked-key)
  (the-path-of-a-lock-in-the-base)
  (a-place-of-the-base-never-gives-nothing)
  (after-the-module-is-started-again)
  (the-base-after-the-module-is-started-again)
  (the-stamps-and-the-depot)
  (the-stamps-and-the-depot-of-the-base))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them. Later checks
  build on what earlier ones made. On the proof's first cluster: the checks
  of the streaming gate come first; the base is made after them. On the
  second cluster: the one group that makes a batch run a second time. The
  hand says which cluster it is."
  []
  (if (= :a-batch-runs-twice (:checks *hand*))
    (a-batch-runs-a-second-time)
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

  The first cluster runs every check but one group, on the functions as the
  store defines them. When it is closed, `-main` empties the hand's atoms,
  all but the shown lines; puts the three noting functions into the vars of
  `softland.store.gate.batch`; starts the second cluster, which runs the one
  group that makes a batch run a second time; and puts the store's functions
  back when that cluster is closed, in a `finally`. The count line is one:
  the counts of the two runs, added."
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
