(ns softland.store.run-again-proof
  "The proof that the store's gates, run again over its depots of acts,
  decide the same. A second module mirrors the two depots of acts of the
  module `Store`, runs the same gates and reads over them from their
  beginning, and every row of every table of the two modules is held, row
  by row and task by task, against the other. One command, from the
  repository root:

    clojure -M:store-module -m softland.store.run-again-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status
  that is not zero when a check fails. Rows that differ are printed on
  lines that start with DIFFERENT, five to a table at most, each with its
  gate, table, task and key, what differs, and both rows.

  The check is that no row differs: of the gates' tables, and of the table
  of the kept clock. The checks before it hold the hand and the two waits
  to what this docstring says of them. Beside the check, and held to
  nothing, the proof prints what each module holds of the cases that it
  added.

  On the store as it is the check fails, and for the microbatch gate only.
  What a run of this proof printed: no row differs in the streaming gate's
  four tables; rows differ in each of the microbatch gate's four tables,
  in the stamp and for many answers in the word too, and in the table of
  the kept clock; and the second module decided the microbatch depot in
  fewer batches than the first. What the source says of the two gates: the
  streaming gate decides of the thing, which holds the time that was
  written on it before the depot, and of its own rows
  (`softland.store.module.streaming-gate`); the microbatch gate makes its
  stamps of a clock that it reads for each batch, and gives the acts of a
  batch that stand on two partitions of the depot an order of its own
  (`softland.store.module`). So the streaming gate decides the same when
  it is run again, as far as a run shows, and the microbatch gate does not.
  src/proposal/backend-on-rama-2026-10-02/run-again/runs/ holds the cuts
  of the runs.

  What it does, on one in-process cluster. It launches `Store` with four
  tasks. A hand sends things of every kind to both depots of acts, some
  through the door (`softland.store.door`) and some with no door: an
  append of a thing that the proof built, act-id, depot word, time and
  all. To those it adds the cases whose answer depends on the order in
  which a gate meets the acts, the cases of the time on a thing, and a
  load:

    in a streaming layer   a give and the first act under the given
                           permission; two acts that replace one fact; a
                           mark of a key and facts under that key; a thing
                           with no time on it; an act whose time is an hour
                           behind the hand's clock; and some hundreds of
                           acts on a few entities
    in the base            the same three, each act sent when the one before
                           was decided; a thing with no time on it; some
                           thousands of acts sent without
                           a wait for the gate, on some hundreds of
                           entities, four of five Alice's under the
                           permission that the system gave her, and every
                           fifth the system's, which also writes one of a
                           few places that acts on other entities write
                           too; and two acts of the system, X and Y, whose
                           first facts are on two entities that the depot
                           puts on two partitions, and whose second facts
                           write one place. X is sent first, and Y only
                           when X is decided; Y stands earlier in its
                           partition than X in its own
    to the other depot     an act into a layer of the other gate, sent to
                           each depot before that layer is made and after

  It waits until the first module is at rest and reads its tables. Then it
  launches `StoreAgain`, the second module, waits until that one has
  decided every thing of both depots, reads the tables of both, and
  compares.

  The time on a thing. A gate refuses, with `:no-door-time`, a thing that
  is no stray and holds no usable time
  (`softland.store.door.declaration/time-part`), and the streaming gate
  makes the stamp of a decision of that time. The door writes the time on
  what goes through it. On a thing that it appends with no door the hand
  writes its own reading of this process's clock at the send, as a door
  does: a thing that it sends twice holds two times. Three things are
  appended as the proof built them, and their time is chosen: the two with
  no time, and the act whose time is an hour behind. A thing that is no
  map takes no time.

  The second module. `StoreAgain` declares a mirror of each of the two
  depots of acts under the depot's own name, and then calls
  `softland.store.module/add-gates-and-reads`, the one function that
  `Store` calls behind its own two depots. So it cannot be another gate than
  the store's. Its tables, its depot of answers and its depot of batch
  clocks are its own. Each gate declares its source with
  `{:start-from :beginning}`, so a module that is launched when the depots
  are full reads them whole. That is the way Rama's docs give for making
  every PState again from the depots (reference/rama/docs/19-operating-rama.md,
  \"Task scaling\"), and the way of the two runs of 13 October 2026
  (src/proposal/backend-on-rama-2026-10-02/run-again-probe-2026-10-13/).

  How it reaches one task's part of a table. As the module's proof does: a
  select with `{:pkey k}`, where `k` is the key that Rama's test tool gives
  for that task (`com.rpl.rama.test/gen-hashing-index-keys`). The select
  goes round the door's reads, so it sees every row that is there.

  How it knows that a module is at rest. For the first module: Rama's test
  tool waits until the microbatch gate has processed as many things as the
  proof appended, and each append to the streaming depot was made with the
  ack level :ack, which waits for the gate's event; the answer of each act
  that went through the door is there; on every task the microbatch gate's
  row holds no `:to-close` and no key under `:to-keep`, and its
  `:closed-through` is the gate's last stamp; and across a quiet time
  neither depot of acts grew and nothing changed of the counts that the
  proof polls: for each gate the number of its answers, the number of its
  strays, and the numbers of each task's row. For the second module: the
  test tool waits until its microbatch gate has processed as many things as
  the microbatch depot holds; an append to a mirrored depot does not wait
  for a stream topology of another module, so the proof polls until the
  second module's streaming tables hold as many answers and strays as the
  first's; then the same test of the batches and the same quiet time.

  What is left out of the comparison, by name: three parts of what the
  microbatch gate keeps for a task. `:turns` is the working state of one
  run of the gate: the id that Rama gave the running batch, and how many
  things of it the task has given a turn. Two modules do not cut a depot
  into the same batches, and nothing reads the part once its batch is
  committed. `:to-keep` holds what a batch has still to append to the
  gate's depot of answers, and `:to-close` what the next run has to close:
  on a module at rest the first holds no key and the second is absent. The
  proof reads the last two of both modules, to know that each is at rest,
  and never `:turns`. Every other part is compared: the answers, the facts,
  the facts by value, and what a gate keeps for a task. Of that the
  streaming gate holds a last stamp for each layer, a map from the text of
  a layer id to a number, and each layer's number is compared under a key
  of its own; the microbatch gate holds one last stamp and
  `:closed-through`; and each holds the strays. The table of the kept
  clock, `$$batch-clock`, is compared too and reported on lines of its own.

  State. The proof owns four atoms: the lines that it showed; how many
  things it appended to the microbatch depot; the act-ids of the acts that
  went through the door, whose answers it reads for; and each append by the
  hand that did not come back acked. The cluster's files are temporary and
  are Rama's. It writes no file.

  What a pass does not show. A real cluster, a process that is killed, a
  leader that moves. A second module with another number of tasks than the
  first. A run again while the first module still takes acts. A failed
  attempt of a gate in either module. A store larger than memory. That the
  second module's rows are right: it shows only that they are the first
  module's rows."
  (:require [clojure.test :refer [deftest is run-tests]]
            [clojure.walk :as walk]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.gate.stamp :as stamp]
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

(defn- cut
  "Takes anything. Gives its printed text, cut to 400 characters, for a
  line that a person reads."
  [x]
  (let [text (pr-str x)]
    (if (< 400 (count text)) (str (subs text 0 400) " ...") text)))

;; ---------------------------------------------------------- the second module

;; The gates of `Store` over mirrors of its two depots of acts. Its docstring
;; is set below: `defmodule` takes none.
(rama/defmodule StoreAgain [setup topologies]
  (rama/mirror-depot setup *streaming-depot (rama/get-module-name module/Store) "*streaming-depot")
  (rama/mirror-depot setup *microbatch-depot (rama/get-module-name module/Store) "*microbatch-depot")
  (module/add-gates-and-reads setup topologies))

(alter-meta! #'StoreAgain assoc
             :doc "The second module of the proof: a mirror of each of the two depots of
  acts of `Store`, under the depot's own name, and behind them everything
  else of `Store` by the store's one definition
  (`softland.store.module/add-gates-and-reads`). It stores no act: its
  gates read the depots of `Store`, from their beginning. Its tables, its
  depot of answers and its depot of batch clocks are its own. What must
  hold at its launch: `Store` runs in the same cluster.")

;; ------------------------------------------------------------------ the hand

(def ^:private tasks "The number of tasks of each of the two modules." 4)

(def ^:private index-keys
  "For each task, a key that Rama's hash partitioner sends to it: the i-th
  to task i. A select with one of them as `:pkey` reads that task's part of
  a table."
  (vec (rtest/gen-hashing-index-keys tasks)))

(def ^:private gates
  "What the proof knows of each gate of the store: its depot of acts and
  the ack level of an append by the hand; the names of its four tables; the
  numbers that its row for a task holds; and, for a gate that decides in
  batches, the name of its topology. `:numbers` names each part of the
  row that is one number. `:numbers-by-layer` names each part that is a map
  from the text of a layer id to a number: the streaming gate's last stamp
  is counted in the layer."
  {:streaming {:depot "*streaming-depot" :ack :ack
               :tables {:answers "$$streaming-answers" :facts "$$streaming-facts"
                        :facts-by-value "$$streaming-facts-by-value" :task "$$streaming-task"}
               :numbers []
               :numbers-by-layer [:last-stamp]}
   :microbatch {:depot "*microbatch-depot" :ack :append-ack
                :tables {:answers "$$microbatch-answers" :facts "$$microbatch-facts"
                         :facts-by-value "$$microbatch-facts-by-value" :task "$$microbatch-task"}
                :numbers [:last-stamp :closed-through]
                :numbers-by-layer []
                :topology "microbatch-gate"}})

(def ^:private wait-limit-ms
  "How long the proof waits for one thing to come true, in milliseconds."
  180000)

(def ^:private quiet-ms
  "The quiet time across which nothing of a module may change, in
  milliseconds."
  2000)

(def ^:private poll-ms
  "How long the proof sleeps between two looks at a thing that it waits
  for, in milliseconds."
  100)

(def ^:dynamic ^:private *hand*
  "What the proof's hand holds of the cluster: the cluster, the name of the
  module `Store`, the door, the query of an answer, the two depots of acts,
  and `:first`, the handles of the first module's tables as `open-reader`
  gives them. `run-on-a-cluster` binds it for the run."
  nil)

(defonce ^{:private true
           :doc "How many things the proof appended to the microbatch depot: what the
  wait for the first module's gate waits for."}
  appended-to-base (atom 0))

(defonce ^{:private true
           :doc "The act-ids of the acts that the door appended: the proof reads for
  the answer of each before it takes the first module to be at rest."}
  to-be-answered (atom []))

(defonce ^{:private true
           :doc "What each append by the hand gave that did not come back acked."}
  not-appended (atom []))

(defn- open-reader
  "Takes the cluster and the name of a module that runs the store's gates.
  Gives the handles that the proof reads that module with: its name, for
  each gate its four tables, and the table of the kept clock."
  [ipc module-name]
  {:module module-name
   :tables (into {}
                 (map (fn [[gate {:keys [tables]}]]
                        [gate (into {} (map (fn [[table table-name]] [table (rama/foreign-pstate ipc module-name table-name)])) tables)]))
                 gates)
   :clock (rama/foreign-pstate ipc module-name "$$batch-clock")})

(defn- open-hand
  "Takes the cluster, in which `Store` is launched. Gives the handles that
  the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc ipc
     :module m
     :door (door/open ipc m)
     :answer (rama/foreign-query ipc m "answer-by-act-id")
     :depots (into {} (map (fn [[gate {:keys [depot]}]] [gate (rama/foreign-depot ipc m depot)])) gates)
     :first (open-reader ipc m)}))

(defn- act
  "A maker's act, as she hands it to the door: no act-id and no depot."
  [layer who permission facts]
  {:version 1 :who who :layer layer :permission permission :session :proof-session :facts (vec facts)})

(defn- arrived
  "A thing as it arrives with no door, but for its time: a maker's act with
  an act-id that the proof makes, under the depot word given, and that word
  as `:depot`. It holds no time: `by-hand!` writes one at the send, and a
  case whose time is chosen adds its own or sends the thing with none."
  [depot layer who permission facts]
  (assoc (act layer who permission facts) :act-id [layer depot (UUID/randomUUID)] :depot depot))

(defn- with-the-hands-clock
  "Takes a thing that the hand is about to append. Gives it with the hand's
  reading of this process's clock, in milliseconds since 1970, under the
  part of the time on an act (`softland.store.door.declaration/time-part`):
  what a door writes at a send. A thing that is no map is given as it is."
  [thing]
  (if (map? thing)
    (assoc thing declaration/time-part (System/currentTimeMillis))
    thing))

(defn- as-it-is!
  "Appends a thing to a gate's depot of acts with no door, as the proof
  built it, with the ack level of `gates`: for the streaming depot it waits
  for the gate's event, for the microbatch depot only until the depot holds
  the thing. It writes no time on the thing: a case whose time is chosen
  calls it. Gives :appended, or what the append threw, which it also keeps
  in `not-appended`."
  [gate thing]
  (when (= :append-ack (get-in gates [gate :ack]))
    (swap! appended-to-base inc))
  (try (rama/foreign-append! (get-in *hand* [:depots gate]) thing (get-in gates [gate :ack]))
       :appended
       (catch Throwable t
         (let [failed [:append-failed gate (.getSimpleName (class t))]]
           (swap! not-appended conj failed)
           failed))))

(defn- by-hand!
  "Appends a thing to a gate's depot of acts with no door, with the hand's
  reading of the clock at this send as the time on it
  (`with-the-hands-clock`), so a thing that is sent twice holds two times.
  The append and what it gives are `as-it-is!`'s."
  [gate thing]
  (as-it-is! gate (with-the-hands-clock thing)))

(defn- by-door!
  "Sends a maker's act through the door. Gives what the door gave:
  {:act-id :digest :ack}, or one of the door's words. An act that the door
  appended to the microbatch depot is counted, and the act-id of each act
  that the door appended is kept among those that the proof reads for."
  [act]
  (let [got (door/send-act (:door *hand*) act)]
    (when (= :microbatch (get-in got [:act-id 1]))
      (swap! appended-to-base inc))
    (when (:act-id got)
      (swap! to-be-answered conj (:act-id got)))
    got))

(defn- settle!
  "Waits until the first module's microbatch gate has processed each thing
  that the proof appended to its depot. Rama's test tool, which throws when
  `wait-limit-ms` passes first."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module *hand*)
                                             (get-in gates [:microbatch :topology]) @appended-to-base
                                             wait-limit-ms))

(defn- alone!
  "Appends one thing to the microbatch depot by the hand and waits until the
  first module's gate has processed it, so that the next thing is of a
  later batch of that module."
  [thing]
  (let [ack (by-hand! :microbatch thing)]
    (settle!)
    ack))

(defn- in-one-batch!
  "Appends things to the microbatch depot by the hand so that one batch of
  the first module's gate takes them all: it pauses the topology, appends,
  and lets it go on. Waits until the gate has processed them."
  [things]
  (let [{:keys [ipc module]} *hand*
        topology (get-in gates [:microbatch :topology])]
    (rtest/pause-microbatch-topology! ipc module topology)
    (let [acks (mapv (fn [thing] (by-hand! :microbatch thing)) things)]
      (rtest/resume-microbatch-topology! ipc module topology)
      (settle!)
      acks)))

(defn- answer-of
  "Reads the answer under an act-id through the first module's query, as
  the door does. Gives the answer, or nil for nothing yet and for a read
  that threw."
  [act-id]
  (try
    (let [routing-id (record/routing-id act-id)]
      (when (record/answer-read-by-routing-id routing-id act-id)
        (rama/foreign-invoke-query (:answer *hand*) routing-id act-id)))
    (catch Throwable _ nil)))

(defn- ends-of
  "Takes a gate. Gives the end offset of each partition of its depot of
  acts, in the order of the partitions: how many records each holds, since
  no depot is trimmed. nil when a call threw."
  [gate]
  (try
    (let [depot (get-in *hand* [:depots gate])]
      (mapv (fn [p] (:end-offset (rama/foreign-depot-partition-info depot p)))
            (range (:num-partitions (rama/foreign-object-info depot)))))
    (catch Throwable _ nil)))

(defn- where-it-stands
  "Takes the ends of a depot before one append and after it, as `ends-of`
  gives them. Gives [partition position] of the record that was added: the
  one partition that grew, and the offset of its new record, from 0. nil
  when either is nil, or when not exactly one partition grew by one. What
  must hold for the answer to be of that append: nothing else appended to
  the depot between the two readings."
  [before after]
  (when (and before after (= (count before) (count after)))
    (let [grew (filterv (fn [p] (not= (nth before p) (nth after p))) (range (count after)))]
      (when (and (= 1 (count grew))
                 (= (inc (nth before (first grew))) (nth after (first grew))))
        [(first grew) (nth before (first grew))]))))

;; ---------------------------------------------------------------- the people

(def ^:private alice-own "Alice's permission in her own layer, its first." [:alice :alice :alice])
(def ^:private bob-sees "Bob's permission in Alice's layer: he may see and not act." [:bob :alice :alice alice-own :see])
(def ^:private carol-in-alices "Carol's permission in Alice's layer, beneath Alice's." [:carol :alice :alice alice-own])
(def ^:private system-own "The system's permission in the base, the base's first." [:system :base :base])
(def ^:private alice-in-base "Alice's permission in the base, beneath the system's." [:alice :base :base system-own])
(def ^:private bob-sees-base "Bob's permission in the base: he may see and not act." [:bob :base :base system-own :see])
(def ^:private carol-in-base "Carol's permission in the base, beneath the system's." [:carol :base :base system-own])

(def ^:private depot-key "The store key of a layer's depot." :softland.store/depot)
(def ^:private permission-key "The store key of a permission." :softland.store/permission)
(def ^:private marks-key "The store key of the keys that a layer marks." :softland.store/marked-keys)

(defn- of-kinds
  "Facts on one entity that hold a value of several kinds: an id that is a
  keyword, one that is a UUID, a string with spaces, a number, a map, and a
  map that is not in its normal form."
  [entity]
  [{:e entity :k :kw :v :canvas-1}
   {:e entity :k :uuid :v #uuid "0b0f1c6e-5a52-4c8e-9a43-6f1d2e7c8a90"}
   {:e entity :k :string :v "a string with spaces"}
   {:e entity :k :number :v 42}
   {:e entity :k :map :v {:a [1 #{2}] "b c" 1.5}}
   {:e entity :k :as-given :v {:b 1.50M :a [1 2N]}}])

;; ------------------------------------------- what is sent: a thing of each kind

(defn- sends-to-the-streaming-gate!
  "Sends a thing of each kind to the streaming depot, into Alice's layer,
  some through the door and some by the hand: the first act of the layer,
  values of several kinds, a mark and facts under it, a retract, a maker
  who may not act, a refused act, a repeat under one act-id, strays, and a
  thing that the declaration refuses. Gives what the door gave for each act
  that went through it."
  []
  (let [door! (fn [who permission & facts] (by-door! (act :alice who permission facts)))
        hand! (fn [who permission & facts] (by-hand! :streaming (arrived :streaming :alice who permission facts)))
        marks (fn [ks] {:e :alice :k marks-key :v ks})
        twice (arrived :streaming :alice :alice alice-own [{:e :note-6 :k :text :v "sent twice"}])
        of-the-other-gate (arrived :microbatch :alice :alice alice-own [{:e :note-7 :k :text :v "a stray"}])
        through-the-door
        [;; the first act of the layer
         (door! :alice alice-own {:e :alice :k depot-key :v :streaming} {:e :alice :k permission-key :v alice-own})
         ;; a value of several kinds
         (apply door! :alice alice-own (of-kinds :kinds))
         ;; a mark, a fact under it, replaced
         (door! :alice alice-own (marks #{:text}))
         (door! :alice alice-own {:e :note-1 :k :text :v "first"})
         (door! :alice alice-own {:e :note-1 :k :text :v "second"})
         ;; a retract
         (door! :alice alice-own {:e :note-2 :k :text :v "to go"})
         (door! :alice alice-own {:e :note-2 :k :text})
         ;; the mark taken off and put on again, with a fact under each marking
         (door! :alice alice-own (marks #{}))
         (door! :alice alice-own {:e :note-3 :k :text :v "while the mark is off"})
         (door! :alice alice-own (marks #{:text}))
         (door! :alice alice-own {:e :note-1 :k :text :v "third"})
         ;; a maker who may not act
         (door! :alice alice-own {:e :bob :k permission-key :v bob-sees})
         (door! :bob bob-sees {:e :note-5 :k :text :v "bob may see"})]]
    ;; a refused act: a permission that nobody gave
    (hand! :zed [:zed :alice :alice alice-own] {:e :note-4 :k :text :v "no permission"})
    ;; an act sent twice, and a second and different act under its act-id
    (by-hand! :streaming twice)
    (by-hand! :streaming twice)
    (by-hand! :streaming (assoc twice :facts [{:e :note-8 :k :text :v "another thing under a used act-id"}]))
    ;; strays: no act-id; and an act-id of the other gate, twice
    (by-hand! :streaming 5)
    (by-hand! :streaming of-the-other-gate)
    (by-hand! :streaming of-the-other-gate)
    ;; a thing that the declaration refuses
    (by-hand! :streaming (dissoc (arrived :streaming :alice :alice alice-own [{:e :note-9 :k :text :v "no session"}]) :session))
    through-the-door))

(defn- sends-to-the-microbatch-gate!
  "Sends a thing of each kind to the microbatch depot, into the base, some
  through the door and some by the hand, and waits for the first module's
  gate after each batch: the first act of the layer, a give,
  marks and facts under them in one batch and in several, values of several
  kinds, two acts of one batch on one place, retracts, refused acts,
  repeats under one act-id, strays, and a thing that the declaration
  refuses. Gives {:through-the-door :making :making-at :give :give-at}:
  what the door gave for each act that went through it; the first act of
  the layer, as it was sent, and where it stands in the depot; and what the
  door gave for the give to Alice, which is sent when the first act is
  decided, and where that stands."
  []
  (let [system (fn [& facts] (arrived :microbatch :base :system system-own facts))
        alice (fn [& facts] (arrived :microbatch :base :alice alice-in-base facts))
        door! (fn [who permission & facts]
                (let [got (by-door! (act :base who permission facts))]
                  (settle!)
                  got))
        marks (fn [ks] {:e :base :k marks-key :v ks})
        twice (alice {:e :thing-6 :k :name :v "sent twice"})
        twice-in-a-batch (alice {:e :thing-7 :k :name :v "twice in one batch"})
        of-the-other-gate (arrived :streaming :alice :alice alice-own [{:e :note-7 :k :text :v "a stray of the base"}])
        ;; the first act of the layer
        making (system {:e :base :k depot-key :v :microbatch} {:e :system :k permission-key :v system-own})
        ends-before (ends-of :microbatch)
        _ (alone! making)
        ends-between (ends-of :microbatch)
        ;; a fact under a key that is on every task: the give to Alice
        give (door! :system system-own {:e :alice :k permission-key :v alice-in-base})
        ends-after (ends-of :microbatch)
        stands {:making making :making-at (where-it-stands ends-before ends-between)
                :give give :give-at (where-it-stands ends-between ends-after)}]
    (let [through-the-door [give]
          ;; a mark and a fact under its key in one batch
          _ (in-one-batch! [(system (marks #{:kind})) (alice {:e :thing-1 :k :kind :v "x"})])
          through-the-door
          (into through-the-door
                [;; a fact under the marked key, replaced
                 (door! :alice alice-in-base {:e :thing-1 :k :kind :v "y"})
                 (door! :alice alice-in-base {:e :thing-1 :k :kind :v "z"})
                 ;; a value of several kinds
                 (apply door! :alice alice-in-base (of-kinds :kinds))])]
      ;; two acts of one batch on one place, under a plain key and under the marked key
      (in-one-batch! [(alice {:e :thing-2 :k :name :v "first"}) (alice {:e :thing-2 :k :name :v "second"})
                      (alice {:e :thing-3 :k :kind :v "p"}) (alice {:e :thing-3 :k :kind :v "q"})])
      ;; a retract, under a plain key and under the marked key
      (alone! (alice {:e :thing-2 :k :name}))
      (alone! (alice {:e :thing-3 :k :kind}))
      ;; the mark taken off, with a fact under the key in the same batch; a
      ;; fact while it is off; the mark put on again, and a fact under it
      (in-one-batch! [(system (marks #{})) (alice {:e :thing-5 :k :kind :v "in the batch that takes the mark off"})])
      (alone! (alice {:e :thing-4 :k :kind :v "while the mark is off"}))
      (alone! (system (marks #{:kind})))
      (alone! (alice {:e :thing-1 :k :kind :v "under the second marking"}))
      ;; a refused act, and a maker who may not act
      (alone! (arrived :microbatch :base :zed [:zed :base :base system-own] [{:e :thing-9 :k :name :v "no permission"}]))
      (alone! (system {:e :bob :k permission-key :v bob-sees-base}))
      (let [through-the-door (conj through-the-door (door! :bob bob-sees-base {:e :thing-10 :k :name :v "bob may see"}))]
        ;; an act sent twice, in two batches and in one; a second and different act under a used act-id
        (alone! twice)
        (alone! twice)
        (in-one-batch! [twice-in-a-batch twice-in-a-batch
                        (assoc twice-in-a-batch :facts [{:e :thing-7 :k :name :v "another thing under a used act-id"}])])
        ;; strays: no act-id; and an act-id of the other gate, twice
        (alone! 7)
        (alone! of-the-other-gate)
        (alone! of-the-other-gate)
        ;; a stray in one batch with an act
        (in-one-batch! [9 (alice {:e :thing-12 :k :name :v "in a batch with a stray"})])
        ;; a thing that the declaration refuses
        (alone! (dissoc (alice {:e :thing-11 :k :name :v "no session"}) :session))
        (assoc stands :through-the-door through-the-door)))))

;; -------------------------------------- what is added: the order, and a load

(defn- order-in-a-streaming-layer!
  "Sends by the hand, into Alice's layer, the three shapes whose answer
  depends on the order in which the gate meets the acts: a give and the
  first act under the given permission; two acts that replace one fact, one
  after the other; and a mark of one more key and two facts under it. Gives
  the first act under the given permission, as it was sent."
  []
  (let [alice! (fn [& facts] (by-hand! :streaming (arrived :streaming :alice :alice alice-own facts)))
        first-under-the-give (arrived :streaming :alice :carol carol-in-alices
                                      [{:e :note-12 :k :text :v "Carol's first act"}])]
    (alice! {:e :carol :k permission-key :v carol-in-alices})
    (by-hand! :streaming first-under-the-give)
    (alice! {:e :note-13 :k :text :v "to be replaced"})
    (alice! {:e :note-13 :k :text :v "the one that replaced it"})
    (alice! {:e :alice :k marks-key :v #{:text :title}})
    (alice! {:e :note-14 :k :title :v "under the new mark"})
    (alice! {:e :note-15 :k :title :v "under the new mark too"})
    first-under-the-give))

(def ^:private behind-ms
  "How far behind the hand's clock the time of the act with a time behind
  is: one hour, in milliseconds."
  3600000)

(defn- times-in-a-streaming-layer!
  "Sends by the hand, into Alice's layer, two things whose time is chosen,
  each as the proof built it: an act of Alice with no time on it, and an
  act of Alice whose time is `behind-ms` behind the hand's reading of the
  clock. Gives {:no-time :behind}: the two things as they were sent. What
  must hold for the second to be behind the time in the layer's last
  stamp: the gate has decided a thing of the layer whose time was a
  reading of this process's clock in the hour before."
  []
  (let [alices (fn [fact] (arrived :streaming :alice :alice alice-own [fact]))
        no-time (alices {:e :note-16 :k :text :v "no time on it"})
        behind (assoc (alices {:e :note-17 :k :text :v "a time behind"})
                      declaration/time-part (- (System/currentTimeMillis) behind-ms))]
    (as-it-is! :streaming no-time)
    (as-it-is! :streaming behind)
    {:no-time no-time :behind behind}))

(defn- order-in-the-base!
  "Sends by the hand, into the base, the same three shapes, each act when
  the first module's gate has decided the one before: a give and the first
  act under the given permission; two acts that replace one fact; and a
  mark of one more key and two facts under it. The acts of one shape have
  their first facts on different entities, so the depot may put them on
  different partitions. Gives the first act under the given permission, as
  it was sent."
  []
  (let [system! (fn [& facts] (alone! (arrived :microbatch :base :system system-own facts)))
        alice! (fn [& facts] (alone! (arrived :microbatch :base :alice alice-in-base facts)))
        first-under-the-give (arrived :microbatch :base :carol carol-in-base
                                      [{:e :thing-13 :k :name :v "Carol's first act"}])]
    (system! {:e :carol :k permission-key :v carol-in-base})
    (alone! first-under-the-give)
    (alice! {:e :thing-14 :k :name :v "to be replaced"})
    (alice! {:e :thing-14 :k :name :v "the one that replaced it"})
    (system! {:e :base :k marks-key :v #{:kind :colour}})
    (alice! {:e :thing-15 :k :colour :v "under the new mark"})
    (alice! {:e :thing-16 :k :colour :v "under the new mark too"})
    first-under-the-give))

(defn- places-to-stand!
  "Finds entities whose acts the microbatch depot puts on different
  partitions. For each of 24 entities it appends one act of the system on
  that entity, and reads which partition of the depot grew. Gives a map
  from a partition to the first entity that was found on it. It reads the
  placing off the depot and computes none. It waits for the first module's
  gate at its end. What must hold: nothing else appends to the depot
  meanwhile."
  []
  (let [found (reduce (fn [found i]
                        (let [entity (keyword (str "stand-" i))
                              before (ends-of :microbatch)
                              _ (by-hand! :microbatch (arrived :microbatch :base :system system-own
                                                               [{:e entity :k :name :v "a place to stand"}]))
                              [partition _] (where-it-stands before (ends-of :microbatch))]
                          (if (and partition (not (contains? found partition)))
                            (assoc found partition entity)
                            found)))
                      {}
                      (range 24))]
    (settle!)
    found))

(def ^:private base-fillers "How many acts the load puts into the base." 2000)

(def ^:private streaming-fillers "How many acts the load puts into Alice's layer." 300)

(def ^:private places-met-by-the-load
  "The keys of the 37 places that the load's acts of the system write as
  their second fact, each from acts with several first entities."
  (mapv (fn [j] (record/place-key :base (keyword (str "met-" j)) :name)) (range 37)))

(defn- base-filler
  "The i-th act of the load of the base. Its first fact is on one of 400
  entities, under a plain key, or for one act in ten under the key that the
  base marks. Four acts of five are Alice's, under the permission that the
  system gave her. Every fifth act is the system's, under the base's first
  permission, and has a second fact, on one of the 37 entities of
  `places-met-by-the-load`, which acts with other first entities write too:
  those places are written from several partitions of the depot, by acts
  whose standing hangs on no give."
  [i]
  (let [own {:e (keyword (str "f-" (mod i 400))) :k (if (= 3 (mod i 10)) :kind :name) :v (str "filler " i)}]
    (if (zero? (mod i 5))
      (arrived :microbatch :base :system system-own
               [own {:e (keyword (str "met-" (mod (quot i 5) (count places-met-by-the-load)))) :k :name :v (str "filler " i)}])
      (arrived :microbatch :base :alice alice-in-base [own]))))

(defn- streaming-filler
  "The i-th act of the load of Alice's layer, by Alice: one fact on one of
  30 entities, under a key that the layer marks."
  [i]
  (arrived :streaming :alice :alice alice-own [{:e (keyword (str "n-" (mod i 30))) :k :text :v (str "filler " i)}]))

(defn- load!
  "Sends the load. The acts of the base are appended one after the other
  with no wait for the gate: each append returns when the depot holds the
  thing, so the first module's gate takes them in as many batches as it
  comes to. The acts of Alice's layer follow while that gate works, each
  with the streaming depot's ack. Then it waits until the first module's
  microbatch gate has processed everything."
  []
  (run! (fn [i] (by-hand! :microbatch (base-filler i))) (range base-fillers))
  (run! (fn [i] (by-hand! :streaming (streaming-filler i))) (range streaming-fillers))
  (settle!))

(defn- row-now
  "Reads one row of a table of the first module's microbatch gate straight
  from the PState, on whichever task holds it. Takes the table's word and
  the row's key. Gives the row, or nil."
  [table k]
  (let [handle (get-in *hand* [:first :tables :microbatch table])]
    (some (fn [pkey] (rama/foreign-select-one (path/keypath k) handle {:pkey pkey})) index-keys)))

(def ^:private met-place
  "The key of the place that the acts X and Y both write."
  (record/place-key :base :met :name))

(defn- two-acts-on-one-place!
  "The acts X and Y. Takes the map of `places-to-stand!`. Both are the
  system's, under the base's first permission, so that the standing of
  neither hangs on a give. X's first fact is on an entity of the partition
  that holds the most records, Y's on an entity of the partition that holds
  the fewest; the second fact of each writes the one place `met-place`. X
  is appended and decided alone; then Y is appended. So X is before Y for
  the first module, and Y stands earlier in its partition than X in its
  own. Should the two partitions hold the same number of records, acts are
  added to X's until it holds more, 64 at most. Gives
  {:x :y :x-at :y-at :x-answered :by-act-before-y}: the two acts; where
  each stands in the depot, as [partition position]; X's answer as the
  table held it before Y was sent; and the act whose fact the place held
  then. It throws when the map names fewer than two partitions."
  [stands]
  (when (< (count stands) 2)
    (throw (ex-info "no two entities on two partitions of the microbatch depot" {:stands stands})))
  (let [system (fn [& facts] (arrived :microbatch :base :system system-own facts))
        ends (ends-of :microbatch)
        by-count (sort-by (fn [p] [(nth ends p) p]) (keys stands))
        y-partition (first by-count)
        x-partition (last by-count)
        _ (loop [added 0]
            (let [ends (ends-of :microbatch)]
              (when (and (< added 64) (<= (nth ends x-partition) (nth ends y-partition)))
                (by-hand! :microbatch (system {:e (get stands x-partition) :k :name :v "one more before X"}))
                (recur (inc added)))))
        x (system {:e (get stands x-partition) :k :name :v "X stands here"} {:e :met :k :name :v "X"})
        before-x (ends-of :microbatch)
        _ (alone! x)
        before-y (ends-of :microbatch)
        x-answered (row-now :answers (record/answer-key :base (nth (:act-id x) 2)))
        by-act-before-y (:by-act (row-now :facts met-place))
        y (system {:e (get stands y-partition) :k :name :v "Y stands here"} {:e :met :k :name :v "Y"})
        _ (alone! y)]
    {:x x :y y :x-at (where-it-stands before-x before-y) :y-at (where-it-stands before-y (ends-of :microbatch))
     :x-answered x-answered :by-act-before-y by-act-before-y}))

;; ------------------------------------------------------------------ at rest

(defn- until-true
  "Calls a function of no argument every `poll-ms` until it gives a true
  thing or `wait-limit-ms` has passed. Gives what it gave last."
  [f]
  (let [deadline (+ (System/currentTimeMillis) wait-limit-ms)]
    (loop []
      (let [got (f)]
        (cond got got
              (< deadline (System/currentTimeMillis)) got
              :else (do (Thread/sleep (long poll-ms)) (recur)))))))

(defn- batches-closed?
  "Takes a module's handles. True when, on every task, the microbatch gate's
  row holds no `:to-close` and no key under `:to-keep`, and its
  `:closed-through` is the gate's last stamp, which one task holds; or no
  task holds either number. A read that throws gives false."
  [reader]
  (try
    (let [table (get-in reader [:tables :microbatch :task])
          part (fn [k]
                 {:last-stamp (rama/foreign-select-one (path/keypath :last-stamp) table {:pkey k})
                  :closed-through (rama/foreign-select-one (path/keypath :closed-through) table {:pkey k})
                  :to-close (rama/foreign-select-one (path/keypath :to-close) table {:pkey k})
                  :to-keep (rama/foreign-select [(path/keypath :to-keep) path/MAP-KEYS] table {:pkey k})})
          parts (mapv part index-keys)
          last-stamps (keep :last-stamp parts)]
      (and (every? (comp nil? :to-close) parts)
           (every? (comp empty? :to-keep) parts)
           (<= (count last-stamps) 1)
           (every? (fn [p] (= (first last-stamps) (:closed-through p))) parts)))
    (catch Throwable _ false)))

(defn- numbers-held
  "Takes a gate, the handle of the table of what it keeps for a task, and
  the key of one task. Gives the numbers of that task's row, each under a
  key of its own. A part that `gates` names under `:numbers` is one number,
  under the part's name. A part that it names under `:numbers-by-layer` is
  a map from the text of a layer id
  (`softland.store.door.declaration/id-text`) to a number, and is read
  entry by entry: each layer's number is under [part text]. So the
  streaming gate's last stamp of the layer `:alice` is under
  [:last-stamp \":alice\"]. A part that the task does not hold, and a layer
  of which the gate decided nothing on the task, gives no key."
  [gate table k]
  (let [{:keys [numbers numbers-by-layer]} (get gates gate)]
    (into {}
          (concat (keep (fn [part]
                          (when-let [n (rama/foreign-select-one (path/keypath part) table {:pkey k})]
                            [part n]))
                        numbers)
                  (for [part numbers-by-layer
                        [layer-text n] (rama/foreign-select [(path/keypath part) path/ALL] table {:pkey k})]
                    [[part layer-text] n])))))

(defn- decided-so-far
  "Takes a module's handles. Gives, for each gate, what says how far it has
  come, read with small selects: `:answers`, how many answers its table
  holds over the tasks; `:strays`, how many digests of strays; and
  `:numbers`, the numbers of each task's row, as `numbers-held` gives them.
  nil when a read threw."
  [reader]
  (try
    (into {}
          (map (fn [[gate {:keys [answers task]}]]
                 [gate {:answers (reduce + (map (fn [k] (count (rama/foreign-select [path/MAP-KEYS] answers {:pkey k}))) index-keys))
                        :strays (reduce + (for [k index-keys
                                                word (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] task {:pkey k})]
                                            (or (rama/foreign-select-one [(path/keypath :strays word) (path/view count)] task {:pkey k}) 0)))
                        :numbers (mapv (fn [k] (numbers-held gate task k)) index-keys)}]))
          (:tables reader))
    (catch Throwable _ nil)))

(defn- quiet-now?
  "Takes a module's handles. Waits `quiet-ms` once. True when the module's
  batches are closed and nothing changed across that time: what
  `decided-so-far` gives, and the ends of the two depots of acts."
  [reader]
  (let [look (fn [] [(decided-so-far reader) (ends-of :streaming) (ends-of :microbatch)])
        before (look)]
    (Thread/sleep (long quiet-ms))
    (boolean (and (every? some? before) (batches-closed? reader) (= before (look))))))

(defn- first-at-rest!
  "Waits until the first module is at rest, as the namespace docstring says.
  Gives {:processed? :answered? :closed? :quiet? :waited-ms}: whether its
  microbatch gate processed each thing that the proof appended; whether
  each act that went through the door has its answer; whether every batch
  is closed; whether a quiet time passed with no change; and how long it
  waited."
  []
  (let [began (System/currentTimeMillis)
        reader (:first *hand*)
        processed? (try (settle!) true (catch Throwable _ false))
        answered? (boolean (until-true (fn [] (every? answer-of @to-be-answered))))
        closed? (boolean (until-true (fn [] (batches-closed? reader))))
        quiet? (boolean (until-true (fn [] (quiet-now? reader))))]
    {:processed? processed? :answered? answered? :closed? closed? :quiet? quiet?
     :waited-ms (- (System/currentTimeMillis) began)}))

(defn- run-again-at-rest!
  "Waits until the second module has decided every thing of both depots and
  is at rest. Takes its handles and the number of records that the
  microbatch depot holds. Gives
  {:base-decided? :streaming-decided? :closed? :quiet? :waited-ms}: whether
  Rama's test tool saw its microbatch gate process that number of things
  within `wait-limit-ms`; whether its streaming tables came to hold as many
  answers and strays as the first module's; whether every batch of it is
  closed; whether a quiet time passed with no change; and how long it
  waited. What the test tool threw, when it threw, is printed on a line
  that starts with NOT REACHED."
  [again records-in-base]
  (let [began (System/currentTimeMillis)
        counts (fn [reader] (some-> (decided-so-far reader) :streaming (select-keys [:answers :strays])))
        base-decided? (try
                        (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module again)
                                                                   (get-in gates [:microbatch :topology])
                                                                   records-in-base wait-limit-ms)
                        true
                        (catch Throwable t
                          (println "NOT REACHED the second module's microbatch gate:" (cut (str t)))
                          false))
        of-the-first (counts (:first *hand*))
        streaming-decided? (boolean (until-true (fn [] (and of-the-first (= of-the-first (counts again))))))
        closed? (boolean (until-true (fn [] (batches-closed? again))))
        quiet? (boolean (until-true (fn [] (quiet-now? again))))]
    {:base-decided? base-decided? :streaming-decided? streaming-decided? :closed? closed? :quiet? quiet?
     :waited-ms (- (System/currentTimeMillis) began)}))

;; ----------------------------------------------------------- the comparison

(defn- bytes-as-hex
  "Takes plain data. Gives it with each run of bytes as its hex text, so
  that `=` compares two of them: two byte arrays are equal to `=` only when
  they are one object. An answer's row holds its digest as bytes."
  [x]
  (walk/postwalk (fn [part] (if (bytes? part) (text/hex part) part)) x))

(defn- task-row-held
  "What a gate keeps for one task, as the table holds it, in the form of a
  table of rows: a key for each thing that is compared alone, and its
  value. The numbers are `numbers-held`'s. The microbatch gate's last stamp
  is one number, under `:last-stamp`. The streaming gate's `:last-stamp` is
  a map from the text of a layer id to that layer's last stamp, and it is
  not compared as one value: each layer's number is under its own key,
  [:last-stamp text], so the two modules are held against each other layer
  by layer, and a layer that one module lacks on the task differs alone.
  `:strays` is a map from each word to its set of digests, there when the
  task holds a stray. `:turns`, `:to-keep` and `:to-close` are not read:
  they are left out of the comparison."
  [gate table k]
  (let [numbers (numbers-held gate table k)
        words (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] table {:pkey k})
        strays (into {}
                     (keep (fn [word]
                             (let [digests (set (rama/foreign-select [(path/keypath :strays word) path/ALL] table {:pkey k}))]
                               (when (seq digests) [word digests]))))
                     words)]
    (cond-> numbers
      (seq strays) (assoc :strays strays))))

(defn- tables-held
  "Takes a module's handles. Gives every row of every table of each gate on
  every task, selected straight from the PStates, with each run of bytes as
  hex text: gate -> table -> task -> key -> row. For the table of what a
  gate keeps for a task, the keys and their values are `task-row-held`'s: a
  part's name, or a part's name with the text of a layer id. One select of
  a whole part for each table and task; the tables of the proof fit in
  memory."
  [reader]
  (bytes-as-hex
   (into {}
         (map (fn [[gate tables]]
                [gate (into {}
                            (map (fn [[table handle]]
                                   [table (into {}
                                                (map-indexed
                                                 (fn [task k]
                                                   [task (if (= :task table)
                                                           (task-row-held gate handle k)
                                                           (into {} (rama/foreign-select [path/ALL] handle {:pkey k})))]))
                                                index-keys)]))
                            tables)]))
         (:tables reader))))

(defn- clock-held
  "Takes a module's handles. Gives the rows of its table of the kept clock
  on every task, in the form of one gate's tables: table -> task -> key ->
  row, with the one table `:batch-clock`."
  [reader]
  {:batch-clock (into {}
                      (map-indexed (fn [task k] [task (into {} (rama/foreign-select [path/ALL] (:clock reader) {:pkey k}))]))
                      index-keys)})

(defn- kind-of
  "Takes the two rows that the two modules hold under one key, of which one
  can be nil. Gives the kind of their difference: `:only-the-first-holds-it`,
  `:only-the-run-again-holds-it`, for two rows that are maps the sorted
  vector of the fields that differ, and for anything else `:the-value`."
  [of-first of-again]
  (cond
    (nil? of-again) :only-the-first-holds-it
    (nil? of-first) :only-the-run-again-holds-it
    (and (map? of-first) (map? of-again))
    (vec (sort (filter (fn [field] (not= (get of-first field) (get of-again field)))
                       (distinct (concat (keys of-first) (keys of-again))))))
    :else :the-value))

(defn- rows-apart
  "Takes a word for a gate and what each module holds of it, table -> task
  -> key -> row. Gives each key whose row is not the same in both, as
  {:gate :table :task :key :first :again :kind}, in the order of the
  tables, the tasks and the keys. A key that one side lacks has nil there.
  A row is compared on the task where it is: the same row on another task
  of the other module differs twice."
  [gate of-first of-again]
  (vec (for [table (sort (distinct (concat (keys of-first) (keys of-again))))
             task (sort (distinct (concat (keys (get of-first table)) (keys (get of-again table)))))
             :let [first-rows (get-in of-first [table task])
                   again-rows (get-in of-again [table task])]
             k (sort-by str (distinct (concat (keys first-rows) (keys again-rows))))
             :when (not= (get first-rows k) (get again-rows k))]
         {:gate gate :table table :task task :key k :first (get first-rows k) :again (get again-rows k)
          :kind (kind-of (get first-rows k) (get again-rows k))})))

(defn- examples
  "Takes the rows of one table that differ. Gives five of them at most: the
  first of each kind of difference in turn, then the second of each, so
  that each kind is seen before one kind is seen twice."
  [apart]
  (loop [left (mapv val (sort-by (comp pr-str key) (group-by :kind apart)))
         out []]
    (if (or (empty? left) (<= 5 (count out)))
      (vec (take 5 out))
      (recur (vec (remove empty? (map rest left))) (into out (map first left))))))

(defn- rows-in
  "Takes what a module holds of one table, task -> key -> row. Gives how
  many rows that is, over the tasks."
  [by-task]
  (reduce + (map count (vals by-task))))

(defn- words-of-answers
  "Takes what a module holds of one gate. Gives how many answers hold each
  word, with :accepted for an answer with none."
  [held-of-gate]
  (into (sorted-map) (frequencies (map (fn [row] (get row :refused :accepted)) (mapcat vals (vals (:answers held-of-gate)))))))

(defn- row-held
  "Takes what a module holds, a gate, a table's word and a key. Gives the
  row under that key on whichever task holds it, or nil."
  [held gate table k]
  (some (fn [rows] (get rows k)) (vals (get-in held [gate table]))))

(defn- word-held
  "Takes what a module holds, a gate and a thing that was sent to that
  gate's depot. Gives the word of the answer under its act-id: the word of
  a refusal, :accepted, or :no-answer."
  [held gate thing]
  (let [[layer _ act-uuid] (:act-id thing)
        row (row-held held gate :answers (record/answer-key layer act-uuid))]
    (if row (get row :refused :accepted) :no-answer)))

(defn- stamp-held
  "Takes what a module holds, a gate and a thing that was sent to that
  gate's depot. Gives the stamp of the answer under its act-id, or nil when
  the module holds no answer under it."
  [held gate thing]
  (let [[layer _ act-uuid] (:act-id thing)]
    (:stamp (row-held held gate :answers (record/answer-key layer act-uuid)))))

(defn- batches-with-things
  "Takes a module's name. Gives in how many batches its microbatch gate
  decided things, read as the number of records of its depot of batch
  clocks: the gate appends one for each batch that had things to decide,
  and one more for each later attempt of a batch that found no clock kept.
  nil when the depot could not be read."
  [module-name]
  (try
    (let [depot (rama/foreign-depot (:ipc *hand*) module-name "*batch-clock-depot")]
      (reduce + (map (fn [p] (:end-offset (rama/foreign-depot-partition-info depot p)))
                     (range (:num-partitions (rama/foreign-object-info depot))))))
    (catch Throwable _ nil)))

(defn- compared
  "Takes what each module holds: its tables as `tables-held` gives them and
  its kept clock as `clock-held` gives it. Prints, for each table of each
  gate, one SHOWN line of how many rows each module holds, how many differ,
  and how many differ in each kind; and up to five of the rows that differ
  on lines that start with DIFFERENT. Gives {:apart :clock-apart}: each row
  of the gates' tables that differs, and each row of the kept clock that
  differs."
  [first-held again-held first-clock again-clock]
  (let [apart (vec (mapcat (fn [gate] (rows-apart gate (get first-held gate) (get again-held gate)))
                           (sort (distinct (concat (keys first-held) (keys again-held))))))
        clock-apart (rows-apart :the-kept-clock first-clock again-clock)
        say-different (fn [row]
                        (println "DIFFERENT" (name (:gate row)) (name (:table row)) "task" (:task row)
                                 "key" (pr-str (:key row)) "| differs:" (pr-str (:kind row))
                                 "| first:" (cut (:first row)) "| again:" (cut (:again row))))]
    (doseq [gate (sort (keys first-held))
            table (sort (keys (get first-held gate)))
            :let [of-table (filterv (fn [row] (and (= gate (:gate row)) (= table (:table row)))) apart)]]
      (shown "the" (name gate) "gate's table" (str (name table) ":")
             "the first module holds" (rows-in (get-in first-held [gate table])) "rows over the" tasks
             "tasks, the run again" (rows-in (get-in again-held [gate table]))
             "| rows that differ:" (count of-table)
             "| by what differs:" (pr-str (into {} (map (fn [[kind rows]] [kind (count rows)])) (group-by :kind of-table))))
      (run! say-different (examples of-table)))
    (run! say-different clock-apart)
    (flush)
    {:apart apart :clock-apart clock-apart}))

;; ------------------------------------------------------------------ the check

(deftest the-gates-run-again-decide-the-same
  (let [{:keys [ipc module]} *hand*
        again-name (rama/get-module-name StoreAgain)
        ;; an act into a layer of the other gate, to each depot, before that layer is made
        base-on-streaming-before (arrived :streaming :base :system system-own [{:e :thing-8 :k :name :v "before the base is made"}])
        alices-on-microbatch-before (arrived :microbatch :alice :alice alice-own [{:e :note-11 :k :text :v "before Alice's layer is made"}])
        _ (by-hand! :streaming base-on-streaming-before)
        _ (alone! alices-on-microbatch-before)
        ;; a thing of each kind, the order in a streaming layer, and the chosen times
        by-door-streaming (sends-to-the-streaming-gate!)
        streaming-under-the-give (order-in-a-streaming-layer!)
        {streaming-with-no-time :no-time streaming-behind :behind} (times-in-a-streaming-layer!)
        {by-door-base :through-the-door :keys [making making-at give give-at]} (sends-to-the-microbatch-gate!)
        ;; the same two acts, now that both layers are made
        base-on-streaming-after (arrived :streaming :base :system system-own [{:e :thing-8 :k :name :v "after the base is made"}])
        alices-on-microbatch-after (arrived :microbatch :alice :alice alice-own [{:e :note-10 :k :text :v "after Alice's layer is made"}])
        _ (by-hand! :streaming base-on-streaming-after)
        _ (alone! alices-on-microbatch-after)
        ;; the order in the base, a thing with no time, the load, and the two acts on one place
        base-under-the-give (order-in-the-base!)
        base-with-no-time (arrived :microbatch :base :alice alice-in-base [{:e :thing-17 :k :name :v "no time on it"}])
        _ (as-it-is! :microbatch base-with-no-time)
        _ (settle!)
        stands (places-to-stand!)
        _ (load!)
        {:keys [x y x-at y-at x-answered by-act-before-y]} (two-acts-on-one-place! stands)
        through-the-door (concat by-door-streaming by-door-base)
        rested (first-at-rest!)
        ends {:streaming (ends-of :streaming) :microbatch (ends-of :microbatch)}
        records-in-base (reduce + (:microbatch ends))
        first-before (tables-held (:first *hand*))
        ;; the run again
        launch-began (System/currentTimeMillis)
        _ (rtest/launch-module! ipc StoreAgain {:tasks tasks :threads 2 :workers 2})
        again (open-reader ipc again-name)
        again-rested (run-again-at-rest! again records-in-base)
        run-again-ms (- (System/currentTimeMillis) launch-began)
        first-held (tables-held (:first *hand*))
        again-held (tables-held again)
        first-clock (clock-held (:first *hand*))
        again-clock (clock-held again)
        wrote-the-place (fn [held]
                          (let [by-act (:by-act (row-held held :microbatch :facts met-place))]
                            (cond (nil? by-act) "no act"
                                  (= by-act (nth (:act-id x) 2)) "X"
                                  (= by-act (nth (:act-id y) 2)) "Y"
                                  :else "another act")))
        ;; by how many milliseconds the time in the stamp of the act with a time behind is later than the time on it
        ahead-of-its-time (fn [held]
                            (when-let [stamp (stamp-held held :streaming streaming-behind)]
                              (- (stamp/time-of stamp) (get streaming-behind declaration/time-part))))
        ;; the streaming gate's last stamp of each layer: task -> the text of the layer id -> the number
        last-stamps-by-layer (fn [held]
                               (into (sorted-map)
                                     (keep (fn [[task rows]]
                                             (let [of-layers (into (sorted-map)
                                                                   (keep (fn [[k n]] (when (vector? k) [(second k) n])))
                                                                   rows)]
                                               (when (seq of-layers) [task of-layers]))))
                                     (get-in held [:streaming :task])))]
    ;; what was sent
    (is (every? :act-id through-the-door))
    (is (zero? (count @not-appended)))
    (shown "sent to the module" module "| through the door:" (count by-door-streaming) "acts to the streaming depot and"
           (count by-door-base) "to the microbatch depot, each appended; the rest by the hand, with no door, each acked"
           "| the load:" base-fillers "acts into the base with no wait for the gate, and" streaming-fillers
           "acts into one streaming layer")
    (shown "records in the depots of acts, by partition | streaming:" (pr-str (:streaming ends))
           "| microbatch:" (pr-str (:microbatch ends)))
    (is (= @appended-to-base records-in-base))
    ;; the two acts on one place stand as the proof says
    (is (and x-at y-at (not= (first x-at) (first y-at)) (< (second y-at) (second x-at))))
    (is (and (some? x-answered) (= by-act-before-y (nth (:act-id x) 2))))
    (shown "the acts X and Y of the system, whose first facts are on two entities and whose second facts write one place: X is at position"
           (second x-at) "of partition" (first x-at) "of the microbatch depot, Y at position" (second y-at) "of partition" (first y-at)
           "| before Y was sent the first module had answered X:"
           (pr-str (if x-answered (get x-answered :refused :accepted) :no-answer))
           "and the place held the fact of X:" (= by-act-before-y (nth (:act-id x) 2)))
    (is (and making-at give-at))
    (shown "the first act of the base and the give to Alice, which the door sent when the first act was decided: the first act is at position"
           (second making-at) "of partition" (first making-at) "of the microbatch depot, the give at position"
           (second give-at) "of partition" (first give-at))
    ;; the first module at rest
    (is (= {:processed? true :answered? true :closed? true :quiet? true} (dissoc rested :waited-ms)))
    (shown "the first module is at rest: its microbatch gate processed each of the" @appended-to-base
           "things of its depot; each of the" (count @to-be-answered)
           "acts that went through the door has its answer; no task holds a :to-close or a key under :to-keep; :closed-through is the gate's last stamp on every task; nothing changed in"
           quiet-ms "ms:" (pr-str (dissoc rested :waited-ms)) "| waited" (:waited-ms rested) "ms")
    ;; the second module decided everything
    (is (= {:base-decided? true :streaming-decided? true :closed? true :quiet? true} (dissoc again-rested :waited-ms)))
    (shown "the second module," again-name
           "was launched when the first was at rest, with mirrors of the two depots of acts and the store's own gates and reads; its microbatch gate processed"
           records-in-base "things, its streaming tables came to hold as many answers and strays as the first module's, its batches are closed, and nothing changed in"
           quiet-ms "ms:" (pr-str (dissoc again-rested :waited-ms)) "| launch and wait together:" run-again-ms "ms")
    (is (true? (= first-before first-held)))
    (shown "the first module's tables are the same before the second module was launched and after it came to rest:"
           (= first-before first-held))
    (shown "batches in which the microbatch gate decided things, by the records of each module's depot of batch clocks | the first module:"
           (batches-with-things module) "| the run again:" (batches-with-things again-name))
    (shown "what the gates decided, by word | streaming, the first module:" (pr-str (words-of-answers (:streaming first-held)))
           "| streaming, the run again:" (pr-str (words-of-answers (:streaming again-held)))
           "| microbatch, the first module:" (pr-str (words-of-answers (:microbatch first-held)))
           "| microbatch, the run again:" (pr-str (words-of-answers (:microbatch again-held))))
    ;; the cases by name: what each module holds of them. These lines say; the check is below.
    (shown "the place that X and Y both write holds, in the first module, the fact of" (wrote-the-place first-held)
           "| in the run again, the fact of" (wrote-the-place again-held))
    (shown "of the" (count places-met-by-the-load)
           "places that the load's acts of the system write from several first entities, the two modules hold the fact of two different acts at"
           (count (filter (fn [k] (not= (:by-act (row-held first-held :microbatch :facts k))
                                        (:by-act (row-held again-held :microbatch :facts k))))
                          places-met-by-the-load)))
    (shown "the first act of the base: the first module" (word-held first-held :microbatch making)
           "the run again" (word-held again-held :microbatch making)
           "| the give to Alice: the first module" (word-held first-held :microbatch give)
           "the run again" (word-held again-held :microbatch give))
    (shown "the first act under a given permission | in the streaming layer: the first module" (word-held first-held :streaming streaming-under-the-give)
           "the run again" (word-held again-held :streaming streaming-under-the-give)
           "| in the base: the first module" (word-held first-held :microbatch base-under-the-give)
           "the run again" (word-held again-held :microbatch base-under-the-give))
    (shown "an act into a layer of the other gate | on the streaming depot, before the base is made: the first module"
           (word-held first-held :streaming base-on-streaming-before) "the run again" (word-held again-held :streaming base-on-streaming-before)
           "| after: the first module" (word-held first-held :streaming base-on-streaming-after)
           "the run again" (word-held again-held :streaming base-on-streaming-after)
           "| on the microbatch depot, before Alice's layer is made: the first module"
           (word-held first-held :microbatch alices-on-microbatch-before) "the run again" (word-held again-held :microbatch alices-on-microbatch-before)
           "| after: the first module" (word-held first-held :microbatch alices-on-microbatch-after)
           "the run again" (word-held again-held :microbatch alices-on-microbatch-after))
    (shown "the time on a thing that the hand appended as the proof built it | a thing with no time, in the streaming layer: the first module"
           (word-held first-held :streaming streaming-with-no-time) "the run again" (word-held again-held :streaming streaming-with-no-time)
           "| in the base: the first module" (word-held first-held :microbatch base-with-no-time)
           "the run again" (word-held again-held :microbatch base-with-no-time)
           "| an act in the streaming layer whose time is" behind-ms "ms behind the hand's clock: the first module"
           (word-held first-held :streaming streaming-behind) "the run again" (word-held again-held :streaming streaming-behind)
           "| the time in its stamp is later than the time on it by, in ms: the first module" (pr-str (ahead-of-its-time first-held))
           "the run again" (pr-str (ahead-of-its-time again-held)))
    (shown "the streaming gate's last stamp of each layer, by task and by the text of the layer id, as each is compared under a key of its own | the first module:"
           (pr-str (last-stamps-by-layer first-held)) "| the run again:" (pr-str (last-stamps-by-layer again-held)))
    ;; the comparison
    (let [{:keys [apart clock-apart]} (compared first-held again-held first-clock again-clock)
          apart-of (fn [gate] (count (filter (fn [row] (= gate (:gate row))) apart)))]
      (is (zero? (count apart)))
      (shown "every row of the answers, the facts, the facts by value and what a gate keeps for a task, of both gates, on each of the"
             tasks "tasks, the first module held against the run again | rows that differ:" (count apart)
             "| of the streaming gate:" (apart-of :streaming) "| of the microbatch gate:" (apart-of :microbatch)
             "| left out, by name: :turns, :to-keep and :to-close of what the microbatch gate keeps for a task")
      (is (zero? (count clock-apart)))
      (shown "the table of the kept clock | the first module:" (pr-str (:batch-clock first-clock))
             "| the run again:" (pr-str (:batch-clock again-clock)) "| rows that differ:" (count clock-apart)))))

;; ---------------------------------------------------------------- the run

(defn- run-on-a-cluster
  "Creates an in-process cluster, launches the module `Store` in it with
  four tasks, two threads and two workers, runs the check with the hand on
  that cluster, and closes the cluster. The check launches `StoreAgain`
  itself, with the same numbers, when the first module is at rest. Gives
  what `run-tests` gives."
  []
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (binding [*hand* (open-hand ipc)]
      (run-tests 'softland.store.run-again-proof))))

(defn -main
  "Runs the proof on one cluster and exits: status 0 when the check holds,
  which is when no row differs and each thing that the proof waited for
  came."
  [& _]
  (let [result (select-keys (run-on-a-cluster) [:test :pass :fail :error])
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println result)
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
