(ns softland.store.run-again-proof
  "The proof that the store comes out the same when it is run again from its
  depots of acts. One proof: a rule held on lines made by hand, four checks
  on a cluster each, and one mode that makes the fixture which two of the
  checks read. From the repository root:

    clojure -M:store-module -m softland.store.run-again-proof
    clojure -M:store-module -m softland.store.run-again-proof make-fixture [folder]

  The first command holds the rule and runs the four checks, each on an
  in-process cluster of its own, one after the other. It prints each thing
  that it showed on a line that starts with SHOWN, prints those lines again
  together at the end, and exits with a status that is not zero when a
  check fails. Rows that differ are printed on lines that start with
  DIFFERENT, five to a table at most, each with its gate, table, part and
  key, what differs, and both rows, cut to 400 characters. It writes no
  file. The second command makes the fixture and runs no check; \"Making
  the fixture\" below says what it does.

  Check 1, the run again. It launches `Store` with four tasks. A hand sends
  things of every kind to both depots of acts (`plays-the-hand!` names
  them), some through the door (`softland.store.door`) and some with no
  door, and the first module decides them as they arrive. Among them, in a
  streaming layer and in the base, are acts whose word rests on the rules
  of sign-in (`rights-and-claims!`): rights that list keys, a give of more
  than the giver's, ends that reach down and a right given again above, and
  two first claims of one place under a key that the layer marks as taking
  one write. When that module
  is at rest the check launches `StoreAgain`, a second module that mirrors
  the two depots and runs the store's own gates and reads over them from
  their beginning. Every row of every table of both gates is then held,
  task by task and key by key, against the same row of the other module.
  The check is that no row differs, and that every task of each module
  holds one and the same map of the microbatch gate's facts and of its
  facts by value, and that the naming rule names no act in either module.
  Beside the check, and held to nothing, it prints what each module holds
  of the hand's cases.

  The naming rule (`out-of-their-line`). A gate decides the acts of a layer
  in the order of the layer's depot partition, and gives each a stamp later
  than the one before. So a client can read, with no gate, whether a store
  kept that order: it reads a partition's records in their order and each
  act's answer. Within one layer an act is out of its line when the depot
  holds it and it has no answer, or when an act of its layer stands before
  it with a higher stamp, or behind it with a lower one. A stray is no act,
  and of two records under one act-id the first is the act. The rule is
  held on lines made by hand, with no cluster. Every check then applies it
  to each partition of both depots of each store that it reads, prints
  each named act on a line that begins OUT OF ITS LINE, with its gate,
  layer, act-id, place and the part of the rule, and fails when a store
  that had no failure holds one.

  Checks 2 and 3, the fixture. The fixture is two files in the folder
  `run_again_fixture` beside this file, read as resources of the classpath:

    acts.edn    the records of both depots of acts as they stood on a store
                of four tasks: for each depot, for each partition in order,
                its records in the partition's order, each as it was
                appended. And a head: the date, the number of tasks, how
                many records, and when the fixture may be made again
    tables.edn  the tables that those records gave, in a form that names no
                task (`task-free`). And a head

  Each of the two checks launches `Store` on a fresh cluster, with 4 tasks
  and then with 8, and appends the fixture's records to the two depots by
  hand, as they are: the time on a record is its own, and the check writes
  no other. When the store is at rest it reads every table on every task
  and compares three times:

    1. the tables, brought into the form with no task, against `tables.edn`,
       row by row;
    2. each task's map of the microbatch gate's facts, and of its facts by
       value, against the fixture's one map of each;
    3. placement, with no task number: for each layer of the fixture, a
       select with that layer as the partitioning key reads one task, and
       that task must hold the layer's answers and its last stamp, and for
       the streaming gate its facts and its facts by value, each equal to
       the fixture's rows of that layer.

  A check passes when no row differs in any of the three. When a file of
  the fixture is absent, each of the two checks fails with the line \"the
  fixture is not made: run make-fixture\" and launches nothing.

  Check 4, after a failure. The prompt of 13 October 2026, in a session's
  text that Sid pasted into it and did not type: \"Nobody has shown
  whether a failure in a fast layer can make acts that were in flight come
  out in another order when the store is run again. Run it and find out.\"
  The check runs it on the store's own module and gates, with no option of
  Rama's set: the store sets none, and
  `topology.stream.max.executing.per.task` stays as Rama has it. It puts
  functions of the proof into the vars of three pure functions that the
  walk calls (`with-failures-in-the-walk`), and through them makes three
  kinds of failure, each once, in the first attempt of one act, each with
  acts behind it whose answer depends on the order
  (`a-throw-in-a-batch-of-the-base!`, `a-stall-with-the-thread-held!`,
  `a-throw-with-the-next-act-right-behind!`,
  `a-throw-with-the-next-act-in-the-depot!`):

    a throw in the base      in the decision of an act in the middle of a
                             batch, with an act behind it in the batch and
                             one appended after the throw
    a stall, the thread held an event of a streaming layer sleeps on its
                             task's thread for longer than Rama's stream
                             timeout, with one act in the depot before the
                             event began and one appended a second into it
    a throw in a streaming   in an act's event, twice in two layers: with
    layer                    the next act appended right behind it, and
                             with the next act in the depot before its
                             event began

  For each it prints what each maker was told, which acts the depot holds,
  the attempts as the walk began them, and the order of the decisions by
  stamp in both modules. Then it puts the store's functions back, launches
  the second module, which decides both depots from their beginning with
  no failure, and compares as check 1 does. The check: no row differs, and
  the rule names no act. Should rows differ, the check is another: each row
  that differs must be of an act that the rule names or that stands behind
  a named act in its layer (`not-accounted-for`), and the line of the
  comparison says that rows differ. An append that a failure swallowed is
  no act of the depot: its maker was told that the append may not have
  gone through, the check says which were lost, and they are no difference.

  The order between two partitions must not matter, and that is the point
  of the feeding. A layer is one partition of its depot, and Rama gives an
  order inside one partition only. So the feeding keeps each partition's
  records in their order, which keeps each layer's, and appends the
  partitions one after the other in an order that differs between the two
  checks: with 4 tasks the streaming depot before the microbatch depot and
  each depot's partitions from the first to the last; with 8 tasks all of
  it reversed. A store whose tables hung on the order of two partitions, or
  of the two depots, would not give the fixture's tables twice.

  The form with no task. A store with another number of tasks puts a layer
  on another task, so a row is compared by its key alone. Of the microbatch
  gate's facts and its facts by value every task holds a whole copy: the
  form holds that one map. Every other table holds a row on the task of
  the row's layer alone: the form holds the union of the tasks' rows. What
  a gate keeps for a task is in the form as rows too: each layer's last
  stamp under [:last-stamp text], with the printed text of the layer id,
  and the strays under [:strays word], as the sorted digests of all tasks.

  Bytes. A run of bytes is written into `tables.edn` as hex text. It is
  told from a string by its place: the one position of a row that is a run
  of bytes is `:digest` of an answer's row, and every other string of the
  file is a string in its table. The making holds the store to that: it
  writes nothing when it finds a run of bytes anywhere else. `acts.edn`
  holds no run of bytes: the hand sends none.

  Making the fixture. `make-fixture` launches `Store` with four tasks,
  plays the hand with the fixture's numbers, waits for rest, reads each
  depot partition's records in order and every table, and writes the two
  files into the folder named, or into the fixture's own folder when none
  is named. Before it writes it holds the store to what the fixture is
  for (`what-the-fixture-must-hold`), and each file's text to its value:
  the text is read as EDN and must give the value back, part for part and
  class for class (`first-not-same`). A thing that EDN does not hold as
  itself, a record, a sorted map, a run of bytes, fails there, and nothing
  is written. So the hand sends only what EDN holds. A proof run never
  writes the files.

  When the fixture may be made again. The rule is a session's text, which
  Sid pasted into his prompt of 13 October 2026 as his decision 6; he did
  not type it: \"A change in what a gate accepts is a new version of the
  act. The old version's rules stay in the code. This starts with the
  first real record. Until then the fixture is made again only when a rule
  is changed on purpose, and the receipt says so.\" A fixture that was made
  again to make a failing check pass shows nothing.

  The time on a thing. A gate reads no clock, and no rule reads the time:
  the stamp of a decision is its layer's last stamp plus one, and the time
  on the thing is a label beside it, in the answer and in the row of each
  fact of an accepted act. The door writes the time on what goes through
  it. On a thing that it appends with no door the hand writes its own
  reading of this process's clock at the send, as a door does. A few things
  are appended as the proof built them, with a chosen time or with none
  (`as-it-is!`). A thing that is no map takes no time.

  The second module. `StoreAgain` declares a mirror of each of the two
  depots of acts under the depot's own name, and then calls
  `softland.store.module/add-gates-and-reads`, the one function that
  `Store` calls behind its own two depots. So it cannot be another gate
  than the store's. Its tables are its own. Each gate declares its source
  with `{:start-from :beginning}`, so a module that is launched when the
  depots are full reads them whole.

  How it reaches one task's part of a table. A select with `{:pkey k}`,
  where `k` is the key that Rama's test tool gives for that task
  (`com.rpl.rama.test/gen-hashing-index-keys`). The select goes round the
  door's reads, so it sees every row that is there.

  How it knows that a store is at rest. Rama's test tool waits until the
  microbatch gate has processed as many things as its depot holds; each
  append to the streaming depot was made with the ack level :ack, which
  waits for the gate's event; the answer of each act that went through the
  door is there; and across a quiet time neither depot of acts grew and
  nothing changed of what the proof polls on each task: for each gate the
  number of its answers, of its facts, of its facts by value and of its
  strays, and each layer's last stamp. For the second module of check 1 an
  append to a mirrored depot does not wait for its stream topology, so the
  proof polls until that module's streaming tables hold as many answers and
  strays on each task as the first's, and then waits for the same quiet
  time. In check 4 the first module is at rest when every act that the
  depots hold has its answer and the quiet time passed: an append that a
  failure swallowed is in no depot, so the hand's count of what it
  appended does not say there what the gate has to process.

  State. The proof owns two atoms: the lines that it showed, and
  `failures`, what check 4 plans and notes. A hand holds its own counts for
  the one cluster that it works on (`open-hand`). For the time of check 4
  three vars of the store hold a function of the proof; the store's own
  are put back when the check ends, however it ends. The clusters' files
  are temporary and are Rama's. The checks write no file; `make-fixture`
  writes the two files of the fixture.

  What a pass does not show. A real cluster, a process that is killed, a
  leader that moves. A run again while the first module still takes acts.
  A failure other than the three kinds of check 4, each run once: above
  all a batch of a streaming layer that Rama forces to fail with no
  restart, its rows dropped, which the in-process cluster did not make
  (src/proposal/backend-on-rama-2026-10-02/run-again/probes-order-after-a-failure/RESULTS.md,
  \"Not settled\"). A store larger than memory. A number of
  tasks other than 4 and 8. A second module that mirrors the depots with
  another number of tasks than the first: checks 2 and 3 feed a fresh
  store. That the rows are right: check 1 shows that the second module's
  rows are the first's, and checks 2 and 3 that a store gives the rows
  which the store gave that the fixture was made of."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [clojure.walk :as walk]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.gate.decision :as decision]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.time LocalDate]
           [java.util UUID]
           [java.util.concurrent CompletableFuture TimeUnit]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(def ^:dynamic ^:private *lead*
  "The words that each SHOWN line of the running check begins with, so that
  a line says which check it is of. nil outside a check."
  nil)

(defn- shown
  "Prints one line of what the proof observed, behind the words of the
  running check (`*lead*`), and keeps it for the recap that `-main` prints
  at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" (concat (when *lead* [*lead* "|"]) xs))]
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
  (module/add-gates-and-reads topologies))

(alter-meta! #'StoreAgain assoc
             :doc "The second module of check 1: a mirror of each of the two depots of
  acts of `Store`, under the depot's own name, and behind them everything
  else of `Store` by the store's one definition
  (`softland.store.module/add-gates-and-reads`). It stores no act: its
  gates read the depots of `Store`, from their beginning. Its tables are
  its own. What must hold at its launch: `Store` runs in the same cluster,
  with the same number of tasks.")

;; ------------------------------------------------------------------ the hand

(def ^:private gates
  "What the proof knows of each gate of the store: its depot of acts and
  the ack level of an append by the hand; the names of its four tables;
  and, for the gate that decides in batches, the name of its topology."
  {:streaming {:depot "*streaming-depot" :ack :ack
               :tables {:answers "$$streaming-answers" :facts "$$streaming-facts"
                        :facts-by-value "$$streaming-facts-by-value" :task "$$streaming-task"}}
   :microbatch {:depot "*microbatch-depot" :ack :append-ack
                :tables {:answers "$$microbatch-answers" :facts "$$microbatch-facts"
                         :facts-by-value "$$microbatch-facts-by-value" :task "$$microbatch-task"}
                :topology "microbatch-gate"}})

(def ^:private gate-order "The two gates, in the order of the proof's lines and files." [:streaming :microbatch])

(def ^:private table-order
  "The four tables of a gate, in the order of the proof's lines and files."
  [:answers :facts :facts-by-value :task])

(def ^:private copied-tables
  "The tables of which the store keeps one whole copy on every task: the
  microbatch gate's facts and its facts by value. Every other table holds a
  row on one task, the task of the row's layer."
  #{[:microbatch :facts] [:microbatch :facts-by-value]})

(def ^:private tasks-of-check-1 "The number of tasks of each of the two modules of check 1." 4)

(def ^:private tasks-of-the-fixture "The number of tasks of the store that the fixture is made of." 4)

(def ^:private wait-limit-ms
  "How long the proof waits for one thing to come true, in milliseconds."
  180000)

(def ^:private quiet-ms
  "The quiet time across which nothing of a store may change, in
  milliseconds."
  2000)

(def ^:private poll-ms
  "How long the proof sleeps between two looks at a thing that it waits
  for, in milliseconds."
  100)

(def ^:dynamic ^:private *hand*
  "What the proof's hand holds of the one cluster that it works on, as
  `open-hand` gives it. `on-a-store` binds it for one check, or for the
  making of the fixture."
  nil)

(defn- open-reader
  "Takes the cluster, the name of a module that runs the store's gates, and
  that module's number of tasks. Gives the handles that the proof reads
  the module with: its name; its number of tasks; `:index-keys`, for each
  task a key that Rama's hash partitioner sends to it, the i-th to task i,
  so that a select with one of them as `:pkey` reads that task's part of a
  table; and for each gate its four tables."
  [ipc module-name tasks]
  {:module module-name
   :tasks tasks
   :index-keys (vec (rtest/gen-hashing-index-keys tasks))
   :tables (into {}
                 (map (fn [[gate {:keys [tables]}]]
                        [gate (into {} (map (fn [[table table-name]] [table (rama/foreign-pstate ipc module-name table-name)])) tables)]))
                 gates)})

(defn- open-hand
  "Takes the cluster, in which `Store` is launched, and the number of its
  tasks. Gives what the hand works with: the cluster, the name of the
  module, the door, the query of an answer, the two depots of acts, and
  `:first`, the handles of that module's tables as `open-reader` gives
  them. And five atoms, the hand's own counts for this cluster:

    :appended-to-base  how many things it appended to the microbatch depot:
                       what the wait for that gate waits for
    :to-be-answered    the act-ids of the acts that the door appended: the
                       proof reads for the answer of each before it takes
                       the store to be at rest
    :by-door           what the door gave for each act that went through it
    :by-hand           each thing that the hand appended with no door, as
                       [gate thing], in the order of the appends
    :not-appended      what each append by the hand gave that did not come
                       back acked"
  [ipc tasks]
  (let [m (rama/get-module-name module/Store)]
    {:ipc ipc
     :module m
     :door (door/open ipc m)
     :answer (rama/foreign-query ipc m "answer-by-act-id")
     :depots (into {} (map (fn [[gate {:keys [depot]}]] [gate (rama/foreign-depot ipc m depot)])) gates)
     :first (open-reader ipc m tasks)
     :appended-to-base (atom 0)
     :to-be-answered (atom [])
     :by-door (atom [])
     :by-hand (atom [])
     :not-appended (atom [])}))

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
  "Appends a thing to a gate's depot of acts with no door, as it is given,
  with the ack level of `gates`: for the streaming depot it waits for the
  gate's event, for the microbatch depot only until the depot holds the
  thing. It writes no time on the thing: a case whose time is chosen calls
  it, and so does the feeding of the fixture's records. It keeps the thing
  among those that the hand appended. Gives :appended, or what the append
  threw, which it also keeps in the hand's `:not-appended`."
  [gate thing]
  (when (= :append-ack (get-in gates [gate :ack]))
    (swap! (:appended-to-base *hand*) inc))
  (swap! (:by-hand *hand*) conj [gate thing])
  (try (rama/foreign-append! (get-in *hand* [:depots gate]) thing (get-in gates [gate :ack]))
       :appended
       (catch Throwable t
         (let [failed [:append-failed gate (.getSimpleName (class t))]]
           (swap! (:not-appended *hand*) conj failed)
           failed))))

(defn- by-hand!
  "Appends a thing to a gate's depot of acts with no door, with the hand's
  reading of the clock at this send as the time on it
  (`with-the-hands-clock`), so a thing that is sent twice holds two times.
  The append and what it gives are `as-it-is!`'s."
  [gate thing]
  (as-it-is! gate (with-the-hands-clock thing)))

(defn- by-door!
  "Sends a maker's act through the door, with an act-id that the maker
  holds already, or with none: the door then makes one. Gives what the
  door gave: {:act-id :digest :ack}, or one of the door's words. The hand
  keeps what the door gave; an act that the door appended to the
  microbatch depot is counted, and the act-id of each act that the door
  appended is kept among those that the proof reads for."
  ([act] (by-door! act nil))
  ([act act-id]
   (let [got (door/send-act (:door *hand*) act act-id)]
     (swap! (:by-door *hand*) conj got)
     (when (= :microbatch (get-in got [:act-id 1]))
       (swap! (:appended-to-base *hand*) inc))
     (when (:act-id got)
       (swap! (:to-be-answered *hand*) conj (:act-id got)))
     got)))

(defn- settle!
  "Waits until the store's microbatch gate has processed each thing that
  the hand appended to its depot. Rama's test tool, which throws when
  `wait-limit-ms` passes first."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module *hand*)
                                             (get-in gates [:microbatch :topology])
                                             @(:appended-to-base *hand*)
                                             wait-limit-ms))

(defn- alone!
  "Appends one thing to the microbatch depot by the hand and waits until the
  store's gate has processed it, so that the next thing is of a later batch
  of that module."
  [thing]
  (let [ack (by-hand! :microbatch thing)]
    (settle!)
    ack))

(defn- with-the-gate-paused!
  "Pauses the store's microbatch gate, calls a function of no argument,
  lets the gate go on, and waits until it has processed each thing that
  the hand appended. So one batch of that gate takes what the function
  appended, up to 1,000 records of a partition by Rama's test docs. Gives
  what the function gave."
  [f]
  (let [{:keys [ipc module]} *hand*
        topology (get-in gates [:microbatch :topology])]
    (rtest/pause-microbatch-topology! ipc module topology)
    (let [got (try (f)
                   (finally (rtest/resume-microbatch-topology! ipc module topology)))]
      (settle!)
      got)))

(defn- in-one-batch!
  "Appends things to the microbatch depot by the hand so that one batch of
  the store's gate takes them all (`with-the-gate-paused!`). Gives what
  each append gave."
  [things]
  (with-the-gate-paused! (fn [] (mapv (fn [thing] (by-hand! :microbatch thing)) things))))

(defn- answer-of
  "Reads the answer under an act-id through the store's query, as the door
  does. Gives the answer, or nil for nothing yet and for a read that
  threw."
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

(defn- records-of
  "Takes a gate. Gives the records of its depot of acts: a vector with one
  vector for each partition, in the order of the partitions, each with
  that partition's records in the partition's order, as Rama's client
  reads them back (`foreign-depot-partition-info`, `foreign-depot-read`),
  500 at a time. What must hold for it to be the whole depot: nothing is
  appended meanwhile."
  [gate]
  (let [depot (get-in *hand* [:depots gate])]
    (mapv (fn [p]
            (let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
              (loop [from start-offset
                     out []]
                (if (< from end-offset)
                  (let [to (min end-offset (+ from 500))]
                    (recur to (into out (rama/foreign-depot-read depot p from to))))
                  out))))
          (range (:num-partitions (rama/foreign-object-info depot))))))

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
  keyword, one that is a UUID, a string with spaces, a string with a quote,
  a new line and letters outside ASCII, a number, true, a map, and a map
  that is not in its normal form."
  [entity]
  [{:e entity :k :kw :v :canvas-1}
   {:e entity :k :uuid :v #uuid "0b0f1c6e-5a52-4c8e-9a43-6f1d2e7c8a90"}
   {:e entity :k :string :v "a string with spaces"}
   {:e entity :k :letters :v "a \"quoted\" word,\na second line, and größer, 日本"}
   {:e entity :k :number :v 42}
   {:e entity :k :flag :v true}
   {:e entity :k :map :v {:a [1 #{2}] "b c" 1.5}}
   {:e entity :k :as-given :v {:b 1.50M :a [1 2N]}}])

;; ------------------------------------------- what is sent: a thing of each kind

(defn- sends-to-the-streaming-gate!
  "Sends a thing of each kind to the streaming depot, into Alice's layer,
  some through the door and some by the hand: the first act of the layer,
  values of several kinds, a mark and facts under it, a retract, a maker
  who may not act, a refused act, a repeat under one act-id, strays, and a
  thing that the declaration refuses."
  []
  (let [door! (fn [who permission & facts] (by-door! (act :alice who permission facts)))
        hand! (fn [who permission & facts] (by-hand! :streaming (arrived :streaming :alice who permission facts)))
        marks (fn [ks] {:e :alice :k marks-key :v ks})
        twice (arrived :streaming :alice :alice alice-own [{:e :note-6 :k :text :v "sent twice"}])
        of-the-other-gate (arrived :microbatch :alice :alice alice-own [{:e :note-7 :k :text :v "a stray"}])]
    ;; the first act of the layer
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
    (door! :bob bob-sees {:e :note-5 :k :text :v "bob may see"})
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
    nil))

(defn- opens-the-base!
  "Sends the three things that the base opens with: its making act, by the
  hand; the give to Alice, through the door; and Alice's first act under
  the given permission, through the door. Takes how they are sent:

    :each-when-the-one-before-is-decided
                   each is sent when the store's gate has decided the one
                   before
    :with-no-wait  the three are appended one after the other while the
                   gate is paused, so one batch of it takes them all

  Gives {:making :making-at :give :give-at :first-under-the-give}: the
  making act as it was sent, and where it stands in the depot; what the
  door gave for the give, and where that stands; and what the door gave
  for Alice's first act. The door reads the base's depot for the give
  while the record holds nothing of the base, and takes the depot that the
  base starts with."
  [how]
  (let [making (arrived :microbatch :base :system system-own
                        [{:e :base :k depot-key :v :microbatch} {:e :system :k permission-key :v system-own}])
        gives-alice (act :base :system system-own [{:e :alice :k permission-key :v alice-in-base}])
        alices-first (act :base :alice alice-in-base [{:e :thing-0 :k :name :v "Alice's first act in the base"}])
        sends (fn [after-each!]
                (let [ends-before (ends-of :microbatch)
                      _ (by-hand! :microbatch making)
                      _ (after-each!)
                      ends-between (ends-of :microbatch)
                      give (by-door! gives-alice)
                      _ (after-each!)
                      ends-after (ends-of :microbatch)
                      first-under-the-give (by-door! alices-first)
                      _ (after-each!)]
                  {:making making :making-at (where-it-stands ends-before ends-between)
                   :give give :give-at (where-it-stands ends-between ends-after)
                   :first-under-the-give first-under-the-give}))]
    (if (= :with-no-wait how)
      (with-the-gate-paused! (fn [] (sends (fn [] nil))))
      (sends settle!))))

(defn- sends-to-the-microbatch-gate!
  "Sends a thing of each kind to the microbatch depot, into the base, some
  through the door and some by the hand, and waits for the store's gate
  after each batch: marks and facts under them in one batch and in several,
  values of several kinds, two acts of one batch on one place, retracts,
  refused acts, repeats under one act-id, strays, and a thing that the
  declaration refuses. What must hold: the base is made and Alice holds her
  permission in it (`opens-the-base!`)."
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
        of-the-other-gate (arrived :streaming :alice :alice alice-own [{:e :note-7 :k :text :v "a stray of the base"}])]
    ;; a mark and a fact under its key in one batch
    (in-one-batch! [(system (marks #{:kind})) (alice {:e :thing-1 :k :kind :v "x"})])
    ;; a fact under the marked key, replaced
    (door! :alice alice-in-base {:e :thing-1 :k :kind :v "y"})
    (door! :alice alice-in-base {:e :thing-1 :k :kind :v "z"})
    ;; a value of several kinds
    (apply door! :alice alice-in-base (of-kinds :kinds))
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
    (door! :bob bob-sees-base {:e :thing-10 :k :name :v "bob may see"})
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
    nil))

;; ------------------------------- what is sent: the order, the time, and a load

(defn- order-in-a-streaming-layer!
  "Sends by the hand, into Alice's layer, three shapes whose answer depends
  on the order in which the gate meets the acts: a give and the first act
  under the given permission; two acts that write one place, one after the
  other; and a mark of one more key and two facts under it. Gives the first
  act under the given permission, as it was sent."
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

(defn- order-in-the-base!
  "Sends by the hand, into the base, the same three shapes, each act when
  the store's gate has decided the one before: a give and the first act
  under the given permission; two acts that write one place; and a mark of
  one more key and two facts under it. Gives the first act under the given
  permission, as it was sent."
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

(def ^:private behind-ms
  "How far behind the hand's clock the time of the act with a time behind
  is: one hour, in milliseconds."
  3600000)

(def ^:private far-ahead-time
  "A time far ahead of every clock: 2^45 milliseconds since 1970, which is
  in the year 3084. What a door whose clock is wrong, or code that appends
  with no door, can write on a thing. The declaration takes it: a time is
  any whole number not below zero."
  (bit-shift-left 1 45))

(def ^:private huge-carried-stamp
  "The stamp that one act of the hand carries in its `:based-on`: the
  greatest long, far above any stamp that a layer of the hand comes to.
  The declaration takes it: a carried stamp is any whole number not below
  zero, and no gate reads it."
  Long/MAX_VALUE)

(def ^:private time-cases
  "The things of `times-in-a-layer!`, in the order in which it sends them."
  [:no-time :behind :carries :far-ahead :after-the-far-one :names-a-later-fact])

(defn- times-in-a-layer!
  "Sends by the hand, to the depot of the gate given, six things one behind
  the other, each with a chosen time or with the hand's reading of the
  clock, and nothing else of the layer between them:

    :no-time             an act with no time on it
    :behind              an act whose time is `behind-ms` behind the hand's
                         reading of the clock
    :carries             an act with the hand's clock that carries
                         `huge-carried-stamp`, for a fact of no act
    :far-ahead           an act whose time is `far-ahead-time`
    :after-the-far-one   an act with the hand's clock
    :names-a-later-fact  an act with the hand's clock that is based on the
                         fact of `:after-the-far-one`, with the stamp of
                         that act's answer, read through the store's query
                         when the gate has decided it: what a maker writes
                         of a fact that she saw

  The last two are an honest act behind one whose time is far ahead, and
  an honest act that names a fact written after it. Takes the gate, a layer
  that it has made, a maker, her permission in the layer, and the key of
  the facts. Waits for the microbatch gate before it reads the answer and
  at its end, when the things are its. Gives a map from each word of
  `time-cases` to the thing as it was sent, with its time. What must hold
  for the second to be behind the time on the acts of its layer before it:
  the gate has decided an act of the layer whose time was a reading of
  this process's clock in the hour before."
  [gate layer who permission k]
  (let [now (System/currentTimeMillis)
        mine (fn [entity says] (arrived gate layer who permission [{:e entity :k k :v says}]))
        carried-from [[layer gate (UUID/randomUUID)] 0]
        first-five {:no-time (mine :time-1 "no time on it")
                    :behind (assoc (mine :time-2 "a time behind") declaration/time-part (- now behind-ms))
                    :carries (assoc (mine :time-3 "it carries a stamp far above any of its layer")
                                    declaration/time-part now
                                    :based-on {carried-from huge-carried-stamp})
                    :far-ahead (assoc (mine :time-4 "a time in the year 3084") declaration/time-part far-ahead-time)
                    :after-the-far-one (with-the-hands-clock (mine :time-5 "written after the one far ahead"))}
        _ (run! (fn [which] (as-it-is! gate (get first-five which))) (take 5 time-cases))
        _ (when (= :microbatch gate) (settle!))
        later-fact [(get-in first-five [:after-the-far-one :act-id]) 0]
        seen (:stamp (answer-of (first later-fact)))
        names-it (with-the-hands-clock
                   (assoc (mine :time-6 "it names a fact written after the one far ahead")
                          :based-on {later-fact seen}))]
    (as-it-is! gate names-it)
    (when (= :microbatch gate) (settle!))
    (assoc first-five :names-a-later-fact names-it)))

(def ^:private again-later-ms
  "How much later than its first send the time on the second send of the
  act is that the hand sends twice with chosen times, in milliseconds."
  1500)

(defn- sent-again!
  "Sends one act twice under one act-id, with two times, to the depot of
  the gate given, in two ways. By the hand, as the proof built it: the
  second send holds a time `again-later-ms` later than the first. And
  through the door, which is handed the act-id of its first send for the
  second, and writes its own clock at each. Takes the gate, a layer that
  it has made, a maker, her permission, and the key of the facts. Waits
  for the microbatch gate when the things are its. Gives
  {:by-hand :by-door}: the hand's thing with no time, and what the door
  gave for its first send."
  [gate layer who permission k]
  (let [now (System/currentTimeMillis)
        thing (arrived gate layer who permission [{:e :again-1 :k k :v "sent again by the hand, with another time"}])
        makers (act layer who permission [{:e :again-2 :k k :v "sent again through the door"}])
        _ (as-it-is! gate (assoc thing declaration/time-part now))
        _ (as-it-is! gate (assoc thing declaration/time-part (+ now again-later-ms)))
        first-send (by-door! makers)]
    (Thread/sleep 5)
    (when (:act-id first-send)
      (by-door! makers (:act-id first-send)))
    (when (= :microbatch gate) (settle!))
    {:by-hand thing :by-door first-send}))

(defn- chain-of-replaces
  "Builds a chain and sends nothing. Takes a depot word, a layer, a maker,
  her permission, an entity, a key and a count. Gives that many things as
  they arrive with no door and with no time. The first writes the place of
  the entity and the key. Each later one writes it again and names the fact
  of the one before as the fact that it replaces. A gate refuses a fact
  that names another than the current fact of its place: so each link is
  accepted only when the gate decides it after the one before it and before
  the one behind it."
  [depot layer who permission entity k n]
  (loop [i 0
         before nil
         out []]
    (if (= i n)
      out
      (let [fact (cond-> {:e entity :k k :v (str "link " i)}
                   before (assoc :replaces [(:act-id before) 0]))
            thing (arrived depot layer who permission [fact])]
        (recur (inc i) thing (conj out thing))))))

(def ^:private chain-length "How many acts a chain of the hand holds, its last act counted." 12)

(defn- chain-with-its-end
  "Builds a chain of `chain-length` acts and sends nothing: the links of
  `chain-of-replaces`, one fewer than that number, and behind them a
  retract of the chain's place that names the fact of the last link as the
  fact that it replaces. Takes what `chain-of-replaces` takes, but for the
  count."
  [depot layer who permission entity k]
  (let [links (chain-of-replaces depot layer who permission entity k (dec chain-length))]
    (conj links (arrived depot layer who permission [{:e entity :k k :replaces [(:act-id (peek links)) 0]}]))))

(defn- chains!
  "Sends two chains (`chain-with-its-end`) by the hand, each under a key
  that its layer marks, so that each link takes the entry of the link
  before out of the facts by value and the last act takes the last entry
  out: one into Alice's layer, each act acked before the next is sent; and
  one into the base, so that one batch of the store's gate takes it whole.
  Gives {:streaming :microbatch}: the acts of each as they were sent. What
  must hold for the keys to be marked: `order-in-a-streaming-layer!` and
  `order-in-the-base!` were played."
  []
  (let [in-alices (chain-with-its-end :streaming :alice :alice alice-own :chain :text)
        in-the-base (chain-with-its-end :microbatch :base :alice alice-in-base :chain :kind)]
    (run! (fn [thing] (by-hand! :streaming thing)) in-alices)
    (in-one-batch! in-the-base)
    {:streaming in-alices :microbatch in-the-base}))

(defn- with-each-optional-part!
  "Sends by the hand, to the depot of the gate given, one act that holds
  each part that an act can leave out: what it is because of, when its
  maker claims it was made, its subjects, and what it is based on, with a
  carried stamp. Takes the gate, a layer that it has
  made, a maker, her permission, and the key of the fact. Waits for the
  microbatch gate when the thing is its. Gives the thing with no time."
  [gate layer who permission k]
  (let [another [layer gate (UUID/randomUUID)]
        thing (assoc (arrived gate layer who permission [{:e :each-part :k k :v "an act with each optional part"}])
                     :because-of another
                     :claimed-when 1700000000000
                     :subjects #{:each-part :another-subject}
                     :based-on {[another 0] 1})]
    (by-hand! gate thing)
    (when (= :microbatch gate) (settle!))
    thing))

(def ^:private too-long-a-layer
  "A layer id whose printed text is longer than
  `softland.store.door.declaration/max-id-text`."
  (keyword (apply str (repeat (inc declaration/max-id-text) "l"))))

(defn- strays-of-each-word!
  "Appends by the hand, to the depot of the gate given, one stray of each
  word of `softland.store.door.declaration/stray-reasons`: a map with no
  act-id; an act-id of two slots; an act-id whose layer is
  `too-long-a-layer`; an act-id of the other gate; and an act-id of another
  layer than the thing's. Each is a thing that EDN holds. Takes the gate, a
  layer, a maker and her permission. Waits for the microbatch gate when the
  things are its. Gives a map from each word to the thing as it was built."
  [gate layer who permission]
  (let [other (if (= :streaming gate) :microbatch :streaming)
        fact (fn [says] [{:e :stray :k :text :v says}])
        mine (fn [says] (arrived gate layer who permission (fact says)))
        things {:no-act-id (dissoc (mine "a map with no act-id") :act-id)
                :act-id-malformed (assoc (mine "an act-id of two slots") :act-id [layer gate])
                :id-too-long (arrived gate too-long-a-layer who permission (fact "an act-id whose layer is too long"))
                :act-id-of-the-other-gate (arrived other layer who permission (fact "an act-id of the other gate"))
                :act-id-of-another-layer (assoc (mine "an act-id of another layer")
                                                :act-id [:another-layer gate (UUID/randomUUID)])}]
    (run! (fn [word] (by-hand! gate (get things word))) declaration/stray-reasons)
    (when (= :microbatch gate) (settle!))
    things))

(defn- refused-for-several-reasons!
  "Sends by the hand, to the depot of the gate given, acts that each have
  one fault of another kind: a version that the code does not know, a part
  that the form does not know, no fact, no permission, one place named
  twice, a key of the store's namespace that is no store key, a fact that
  replaces one that is not there, a permission of another layer, a maker
  who is not the permission's holder, and a depot fact on an entity that is
  not the layer. Takes the gate, a layer that it has made, a maker, her
  permission in the layer, and the key of the facts. Waits for the
  microbatch gate when the things are its. Gives the things as they were
  built. The proof names no word for them: it prints the words that the
  gate gave."
  [gate layer who permission k]
  (let [mine (fn [entity says] (arrived gate layer who permission [{:e entity :k k :v says}]))
        nobodys-fact [[layer gate (UUID/randomUUID)] 0]
        things [(assoc (mine :refused-1 "a version that the code does not know") :version 2)
                (assoc (mine :refused-2 "a part that the form does not know") :colour "red")
                (assoc (mine :refused-3 "no fact") :facts [])
                (dissoc (mine :refused-4 "no permission") :permission)
                (arrived gate layer who permission [{:e :refused-5 :k k :v "one"} {:e :refused-5 :k k :v "two"}])
                (arrived gate layer who permission [{:e layer :k :softland.store/no-such-key :v 1}])
                (arrived gate layer who permission
                         [{:e :refused-7 :k k :v "it replaces a fact that is not there" :replaces nobodys-fact}])
                (arrived gate layer who [who :elsewhere :elsewhere] [{:e :refused-8 :k k :v "a permission of another layer"}])
                (arrived gate layer :mallory permission [{:e :refused-9 :k k :v "not the permission's holder"}])
                (arrived gate layer who permission [{:e :somebody :k depot-key :v gate}])]]
    (run! (fn [thing] (by-hand! gate thing)) things)
    (when (= :microbatch gate) (settle!))
    things))

(def ^:private one-write-key "The store key of the keys under which a layer takes one write for a place." :softland.store/one-write-keys)

(defn- rights-and-claims-steps
  "The acts of `rights-and-claims!` for a gate, in groups, in the order in
  which they are sent: each group a vector of things, with the word that
  each must get. In a streaming layer the things of a group are sent one
  after the other; in the base the things of a group of more than one go
  in one batch. The streaming gate's layer is `:rights`, which Alice makes
  first; the base's first permission is the system's. Beneath the first
  permission: Dora, whose right lists the pattern `:tool/*` and the store
  key of a permission; Eli beneath Dora, with two exact keys; Finn with the
  pattern `:*`; Gus with see, then see and act.

  What the groups show, in order: a right that lists keys given, and one
  beneath it; a key on the list taken, and one not on it refused
  `:key-not-allowed`; a give that allows more than the giver's refused;
  `:*` takes plain keys and never a store key; an end by one beneath the
  right refused `:end-not-from-above`; in one group, the end by the giver
  taken, an act under the ended right refused, and one beneath it refused
  `:permission-above-has-ended`; the same right given again above, the act
  beneath still refused, and the giver acting again; see raised by an end
  and then a give; and a layer that marks one-write keys, two first claims
  of one place in one group, one taken and one refused
  `:place-takes-one-write`, a retract, and a claim after it taken."
  [gate]
  (let [streaming? (= :streaming gate)
        layer (if streaming? :rights :base)
        first-holder (if streaming? :alice :system)
        first-right (if streaming? [:alice :rights :rights] system-own)
        dora [:dora layer layer first-right #{:tool/* permission-key}]
        eli [:eli layer layer dora #{:tool/a permission-key}]
        finn [:finn layer layer first-right #{:*}]
        gus-sees [:gus layer layer first-right :see]
        gus-acts [:gus layer layer first-right :see-and-act]
        thing (fn [who permission & facts] (arrived gate layer who permission facts))
        by-first (fn [& facts] (apply thing first-holder first-right facts))
        give (fn [right] (by-first {:e (first right) :k permission-key :v right}))
        end-by (fn [who permission holder] (thing who permission {:e holder :k permission-key}))
        claim (fn [who permission says] (thing who permission {:e :n-1 :k :name/of :v says}))]
    (cond->> [[[(give dora) :accepted]]
              [[(thing :dora dora {:e :eli :k permission-key :v eli}) :accepted]]
              [[(thing :eli eli {:e :t-1 :k :tool/a :v "a key of Eli's list"}) :accepted]]
              [[(thing :eli eli {:e :t-1 :k :text :v "a key not on Eli's list"}) :key-not-allowed]]
              [[(thing :dora dora {:e :hal :k permission-key :v [:hal layer layer dora #{:*}]})
                :permission-fact-allows-more-than-the-givers]]
              [[(give finn) :accepted]]
              [[(thing :finn finn {:e :t-2 :k :tool/x :v "a key of a namespace"} {:e :t-2 :k :text :v "a key of none"}) :accepted]]
              [[(thing :finn finn {:e layer :k :softland.store/who-may-see :v :everyone}) :key-not-allowed]]
              [[(end-by :eli eli :dora) :end-not-from-above]]
              [[(end-by first-holder first-right :dora) :accepted]
               [(thing :dora dora {:e :t-3 :k :tool/b :v "under the ended right"}) :permission-does-not-stand]
               [(thing :eli eli {:e :t-4 :k :tool/a :v "beneath the ended right"}) :permission-above-has-ended]]
              [[(give dora) :accepted]]
              [[(thing :eli eli {:e :t-5 :k :tool/a :v "beneath the right given again"}) :permission-above-has-ended]]
              [[(thing :dora dora {:e :t-6 :k :tool/c :v "the right given again acts"}) :accepted]]
              [[(give gus-sees) :accepted]]
              [[(thing :gus gus-sees {:e :t-7 :k :text :v "a holder of see"}) :permission-does-not-allow-acting]]
              [[(give gus-acts) :store-key-takes-one-write]]
              [[(end-by first-holder first-right :gus) :accepted]]
              [[(give gus-acts) :accepted]]
              [[(thing :gus gus-acts {:e :t-8 :k :text :v "see raised to see and act"}) :accepted]]
              [[(by-first {:e layer :k one-write-key :v #{:name/of}}) :accepted]]
              [[(claim first-holder first-right "the first claim") :accepted]
               [(claim :finn finn "a second first claim") :place-takes-one-write]]
              [[(by-first {:e :n-1 :k :name/of}) :accepted]]
              [[(claim :finn finn "a claim after the retract") :accepted]]]
      streaming? (into [[[(thing :alice first-right {:e :rights :k depot-key :v :streaming}
                                {:e :alice :k permission-key :v first-right})
                         :accepted]]]))))

(defn- rights-and-claims!
  "Sends the acts of `rights-and-claims-steps` for the gate given, by the
  hand, and waits for the microbatch gate when they are its. Gives the
  things as they were built, in the order sent. The word that each must get
  is the steps'; `what-the-fixture-must-hold` holds the store to them."
  [gate]
  (let [groups (mapv (fn [group] (mapv first group)) (rights-and-claims-steps gate))]
    (doseq [group groups]
      (cond
        (= :streaming gate) (run! (fn [x] (by-hand! :streaming x)) group)
        (= 1 (count group)) (alone! (first group))
        :else (in-one-batch! group)))
    (into [] cat groups)))

(defn- rights-and-claims-words
  "The words that the acts of `rights-and-claims!` must get in a gate, in
  the order sent."
  [gate]
  (into [] (comp cat (map second)) (rights-and-claims-steps gate)))

(def ^:private rooms
  "Seven small streaming layers, the rooms: for each its id, its first
  holder, and who makes it. Six are Alice's, made by her. The seventh is a
  personal layer of Bob's, made for him by the system; its id is a UUID,
  where every other layer of the hand has a keyword. With Alice's own layer
  and the layer of rights (`rights-and-claims!`) that is nine layers of the
  streaming depot, more than a store of four tasks has tasks: so two of
  them at least live on one task."
  (conj (mapv (fn [i] {:room (keyword (str "room-" i)) :holder :alice :made-by :alice}) (range 1 7))
        {:room #uuid "5a7c0d3e-8f21-4b6a-9c1d-2e3f4a5b6c7d" :holder :bob :made-by :system}))

(def ^:private rounds-in-a-room "How many acts the hand sends into each room behind its making act." 4)

(defn- rooms!
  "Makes each of `rooms` by the hand, and then sends `rounds-in-a-room`
  rounds of acts: in a round one act of the room's holder into each room in
  turn. So the records of two rooms that the depot puts on one partition
  stand between each other there. In a room the acts are a chain
  (`chain-of-replaces`), so each is accepted only in the room's own order.
  Gives a map from each room's id to its things as they were sent, the
  making act first."
  []
  (let [own (fn [{:keys [room holder]}] [holder room room])
        makings (mapv (fn [{:keys [room holder made-by] :as of-room}]
                        (arrived :streaming room made-by (own of-room)
                                 [{:e room :k depot-key :v :streaming} {:e holder :k permission-key :v (own of-room)}]))
                      rooms)
        chains (mapv (fn [{:keys [room holder] :as of-room}]
                       (chain-of-replaces :streaming room holder (own of-room) :table :text rounds-in-a-room))
                     rooms)
        round-by-round (apply mapcat vector chains)]
    (run! (fn [thing] (by-hand! :streaming thing)) (concat makings round-by-round))
    (zipmap (map :room rooms) (map (fn [making chain] (into [making] chain)) makings chains))))

(def ^:private numbers-of-check-1
  "The numbers of the hand of check 1: how the base opens; how many acts
  the load puts into the base, on how many entities, and how many places
  the load's acts of the system meet at; and how many acts the load puts
  into Alice's layer, on how many entities."
  {:base-opens :each-when-the-one-before-is-decided
   :base-fillers 2000 :base-entities 400 :met-places 37
   :streaming-fillers 300 :streaming-entities 30})

(def ^:private numbers-of-the-fixture
  "The numbers of the hand that the fixture is made with: the base opens
  with no wait, and the load is small, so that the two files stay small.
  The parts are those of `numbers-of-check-1`."
  {:base-opens :with-no-wait
   :base-fillers 100 :base-entities 20 :met-places 5
   :streaming-fillers 50 :streaming-entities 10})

(defn- places-met-by-the-load
  "Takes the numbers of a hand. Gives the keys of the places that the
  load's acts of the system write as their second fact, each from acts
  with several first entities."
  [numbers]
  (mapv (fn [j] (record/place-key :base (keyword (str "met-" j)) :name)) (range (:met-places numbers))))

(defn- base-filler
  "The i-th act of the load of the base, for the numbers of a hand. Its
  first fact is on one of `:base-entities` entities, under a plain key, or
  for one act in ten under the key that the base marks. Four acts of five
  are Alice's, under the permission that the system gave her. Every fifth
  act is the system's, under the base's first permission, and has a second
  fact, on one of the `:met-places` entities of `places-met-by-the-load`,
  which acts with other first entities write too."
  [numbers i]
  (let [own {:e (keyword (str "f-" (mod i (:base-entities numbers)))) :k (if (= 3 (mod i 10)) :kind :name) :v (str "filler " i)}]
    (if (zero? (mod i 5))
      (arrived :microbatch :base :system system-own
               [own {:e (keyword (str "met-" (mod (quot i 5) (:met-places numbers)))) :k :name :v (str "filler " i)}])
      (arrived :microbatch :base :alice alice-in-base [own]))))

(defn- streaming-filler
  "The i-th act of the load of Alice's layer, by Alice, for the numbers of
  a hand: one fact on one of `:streaming-entities` entities, under a key
  that the layer marks."
  [numbers i]
  (arrived :streaming :alice :alice alice-own
           [{:e (keyword (str "n-" (mod i (:streaming-entities numbers)))) :k :text :v (str "filler " i)}]))

(defn- load!
  "Sends the load, for the numbers of a hand. The acts of the base are
  appended one after the other with no wait for the gate: each append
  returns when the depot holds the thing, so the store's gate takes them in
  as many batches as it comes to. The acts of Alice's layer follow while
  that gate works, each with the streaming depot's ack. Then it waits until
  the microbatch gate has processed everything."
  [numbers]
  (run! (fn [i] (by-hand! :microbatch (base-filler numbers i))) (range (:base-fillers numbers)))
  (run! (fn [i] (by-hand! :streaming (streaming-filler numbers i))) (range (:streaming-fillers numbers)))
  (settle!))

(defn- row-now
  "Reads one row of a table of the store's microbatch gate straight from
  the PState, on the first task that holds it. Takes the table's word and
  the row's key. Gives the row, or nil."
  [table k]
  (let [handle (get-in *hand* [:first :tables :microbatch table])]
    (some (fn [pkey] (rama/foreign-select-one (path/keypath k) handle {:pkey pkey}))
          (get-in *hand* [:first :index-keys]))))

(def ^:private met-place
  "The key of the place that the acts X and Y both write."
  (record/place-key :base :met :name))

(defn- two-acts-on-one-place!
  "The acts X and Y: two acts that write one place, sent one after the
  other. Both are the system's, under the base's first permission, so that
  the standing of neither hangs on a give. The first fact of each is on an
  entity of its own; the second fact of each writes the one place
  `met-place`. X is appended and decided alone; then Y is appended. Both
  are acts of the base, which is one partition of the depot: X stands
  before Y there, and that is the one order which a gate has for them. So
  the place holds the fact of Y, however a later run cuts its batches.
  Gives {:x :y :x-at :y-at :x-answered :by-act-before-y}: the two acts;
  where each stands in the depot, as [partition position]; X's answer as
  the table held it before Y was sent; and the act whose fact the place
  held then."
  []
  (let [system (fn [& facts] (arrived :microbatch :base :system system-own facts))
        x (system {:e :stand-x :k :name :v "X stands here"} {:e :met :k :name :v "X"})
        before-x (ends-of :microbatch)
        _ (alone! x)
        before-y (ends-of :microbatch)
        x-answered (row-now :answers (record/answer-key :base (nth (:act-id x) 2)))
        by-act-before-y (:by-act (row-now :facts met-place))
        y (system {:e :stand-y :k :name :v "Y stands here"} {:e :met :k :name :v "Y"})
        _ (alone! y)]
    {:x x :y y :x-at (where-it-stands before-x before-y) :y-at (where-it-stands before-y (ends-of :microbatch))
     :x-answered x-answered :by-act-before-y by-act-before-y}))

(defn- plays-the-hand!
  "Plays the whole hand on the store of `*hand*`, for the numbers given
  (`numbers-of-check-1`, `numbers-of-the-fixture`). In this order:

    1. before a layer is made: an act into Alice's layer and an act into
       the base, and an act for a layer of the other gate on each depot
    2. the base opens: its making act, a give and the first act under it
       (`opens-the-base!`)
    3. a thing of each kind in Alice's layer, and three shapes whose answer
       depends on the order (`sends-to-the-streaming-gate!`,
       `order-in-a-streaming-layer!`)
    4. the same in the base (`sends-to-the-microbatch-gate!`,
       `order-in-the-base!`)
    5. again an act for a layer of the other gate on each depot, now that
       both layers are made
    6. in each gate: the things whose time is chosen (`times-in-a-layer!`),
       an act sent twice with two times (`sent-again!`), a chain of acts
       that each replace the one before (`chains!`), an act with each
       optional part (`with-each-optional-part!`), a stray of each word
       (`strays-of-each-word!`), acts refused for several reasons
       (`refused-for-several-reasons!`), and rights and claims: rights that
       list keys, given, ended, given again and raised, and one write for a
       place (`rights-and-claims!`), in the streaming layer `:rights` and in
       the base
    7. seven more streaming layers, with their acts in turn (`rooms!`)
    8. the load (`load!`), and two acts that write one place
       (`two-acts-on-one-place!`)

  Gives the cases by name, each with the things as they were sent or with
  what the door gave: `what-the-cases-gave` reads a store's tables for
  them."
  [numbers]
  (let [alices-before (arrived :streaming :alice :alice alice-own [{:e :note-0 :k :text :v "before Alice's layer is made"}])
        base-before (arrived :microbatch :base :alice alice-in-base [{:e :thing-00 :k :name :v "before the base is made"}])
        base-on-streaming-before (arrived :streaming :base :system system-own [{:e :thing-8 :k :name :v "before the base is made"}])
        alices-on-microbatch-before (arrived :microbatch :alice :alice alice-own [{:e :note-11 :k :text :v "before Alice's layer is made"}])
        _ (by-hand! :streaming alices-before)
        _ (by-hand! :streaming base-on-streaming-before)
        _ (alone! base-before)
        _ (alone! alices-on-microbatch-before)
        opening (opens-the-base! (:base-opens numbers))
        _ (sends-to-the-streaming-gate!)
        streaming-under-the-give (order-in-a-streaming-layer!)
        _ (sends-to-the-microbatch-gate!)
        base-under-the-give (order-in-the-base!)
        base-on-streaming-after (arrived :streaming :base :system system-own [{:e :thing-8 :k :name :v "after the base is made"}])
        alices-on-microbatch-after (arrived :microbatch :alice :alice alice-own [{:e :note-10 :k :text :v "after Alice's layer is made"}])
        _ (by-hand! :streaming base-on-streaming-after)
        _ (alone! alices-on-microbatch-after)
        in-each (fn [f] {:streaming (f :streaming :alice :alice alice-own :text)
                         :microbatch (f :microbatch :base :alice alice-in-base :name)})
        times (in-each times-in-a-layer!)
        again (in-each sent-again!)
        chains (chains!)
        each-part (in-each with-each-optional-part!)
        strays (in-each (fn [gate layer who permission _] (strays-of-each-word! gate layer who permission)))
        refused (in-each refused-for-several-reasons!)
        rights {:streaming (rights-and-claims! :streaming) :microbatch (rights-and-claims! :microbatch)}
        in-rooms (rooms!)
        _ (load! numbers)
        x-and-y (two-acts-on-one-place!)]
    {:numbers numbers
     :before-made {:streaming alices-before :microbatch base-before}
     :other-gate {:on-the-streaming-depot-before [:streaming base-on-streaming-before]
                  :on-the-streaming-depot-after [:streaming base-on-streaming-after]
                  :on-the-microbatch-depot-before [:microbatch alices-on-microbatch-before]
                  :on-the-microbatch-depot-after [:microbatch alices-on-microbatch-after]}
     :opening opening
     :under-the-give {:streaming streaming-under-the-give :microbatch base-under-the-give}
     :times times
     :sent-again again
     :chains chains
     :each-part each-part
     :strays strays
     :refused refused
     :rights rights
     :rooms in-rooms
     :x-and-y x-and-y}))

;; ------------------------------------------------------------------ at rest

(defn- until-within
  "Takes a limit in milliseconds and a function of no argument. Calls the
  function every `poll-ms` until it gives a true thing or the limit has
  passed. Gives what it gave last."
  [limit-ms f]
  (let [deadline (+ (System/currentTimeMillis) limit-ms)]
    (loop []
      (let [got (f)]
        (cond got got
              (< deadline (System/currentTimeMillis)) got
              :else (do (Thread/sleep (long poll-ms)) (recur)))))))

(defn- until-true
  "Calls a function of no argument every `poll-ms` until it gives a true
  thing or `wait-limit-ms` has passed. Gives what it gave last."
  [f]
  (until-within wait-limit-ms f))

(defn- decided-so-far
  "Takes a module's handles. Gives, for each gate and each task in order,
  what says how far the gate has come there, read with small selects: how
  many answers, facts and facts by value the task holds, how many digests
  of strays, and each layer's last stamp. nil when a read threw."
  [reader]
  (try
    (into {}
          (map (fn [[gate {:keys [answers facts facts-by-value task]}]]
                 [gate (mapv (fn [k]
                               {:answers (count (rama/foreign-select [path/MAP-KEYS] answers {:pkey k}))
                                :facts (count (rama/foreign-select [path/MAP-KEYS] facts {:pkey k}))
                                :facts-by-value (count (rama/foreign-select [path/MAP-KEYS] facts-by-value {:pkey k}))
                                :strays (reduce + (for [word (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] task {:pkey k})]
                                                    (or (rama/foreign-select-one [(path/keypath :strays word) (path/view count)] task {:pkey k}) 0)))
                                :last-stamps (into {} (rama/foreign-select [(path/keypath :last-stamp) path/ALL] task {:pkey k}))})
                             (:index-keys reader))]))
          (:tables reader))
    (catch Throwable _ nil)))

(defn- quiet-now?
  "Takes a module's handles. Waits `quiet-ms` once. True when nothing
  changed across that time: what `decided-so-far` gives, and the ends of
  the two depots of acts."
  [reader]
  (let [look (fn [] [(decided-so-far reader) (ends-of :streaming) (ends-of :microbatch)])
        before (look)]
    (Thread/sleep (long quiet-ms))
    (boolean (and (every? some? before) (= before (look))))))

(defn- first-at-rest!
  "Waits until the store of `*hand*` is at rest, as the namespace docstring
  says. Gives {:processed? :answered? :quiet? :waited-ms}: whether its
  microbatch gate processed each thing that the hand appended to its depot;
  whether each act that went through the door has its answer; whether a
  quiet time passed with no change; and how long it waited."
  []
  (let [began (System/currentTimeMillis)
        reader (:first *hand*)
        processed? (try (settle!) true (catch Throwable _ false))
        answered? (boolean (until-true (fn [] (every? answer-of @(:to-be-answered *hand*)))))
        quiet? (boolean (until-true (fn [] (quiet-now? reader))))]
    {:processed? processed? :answered? answered? :quiet? quiet?
     :waited-ms (- (System/currentTimeMillis) began)}))

(defn- run-again-at-rest!
  "Waits until the second module has decided every thing of both depots and
  is at rest. Takes its handles and the number of records that the
  microbatch depot holds. Gives
  {:base-decided? :streaming-decided? :quiet? :waited-ms}: whether Rama's
  test tool saw its microbatch gate process that number of things within
  `wait-limit-ms`; whether its streaming tables came to hold as many
  answers and strays on each task as the first module's; whether a quiet
  time passed with no change; and how long it waited. What the test tool
  threw, when it threw, is printed on a line that starts with NOT REACHED."
  [again records-in-base]
  (let [began (System/currentTimeMillis)
        counts (fn [reader] (some->> (decided-so-far reader) :streaming (mapv (fn [of-task] (select-keys of-task [:answers :strays])))))
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
        quiet? (boolean (until-true (fn [] (quiet-now? again))))]
    {:base-decided? base-decided? :streaming-decided? streaming-decided? :quiet? quiet?
     :waited-ms (- (System/currentTimeMillis) began)}))

;; ------------------------------------------------------- what the tables hold

(defn- bytes-as-hex
  "Takes plain data. Gives it with each run of bytes as its hex text, so
  that `=` compares two of them: two byte arrays are equal to `=` only when
  they are one object. An answer's row holds its digest as bytes."
  [x]
  (walk/postwalk (fn [part] (if (bytes? part) (text/hex part) part)) x))

(defn- task-rows-held
  "What a gate keeps for one task, as the table holds it, in the form of a
  table of rows. Takes the table's handle and a partitioning key, which
  names the task. Gives a map: under [:last-stamp text] the last stamp of
  each layer of which the gate decided a thing on the task, with the
  printed text of the layer id; and under [:strays word] the set of the
  digests of the strays of that word, for each word of which the task holds
  one. So two stores are held against each other layer by layer and word by
  word."
  [handle pkey]
  (let [stamps (into {}
                     (map (fn [[layer-text n]] [[:last-stamp layer-text] n]))
                     (rama/foreign-select [(path/keypath :last-stamp) path/ALL] handle {:pkey pkey}))
        words (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] handle {:pkey pkey})]
    (into stamps
          (keep (fn [word]
                  (let [digests (set (rama/foreign-select [(path/keypath :strays word) path/ALL] handle {:pkey pkey}))]
                    (when (seq digests) [[:strays word] digests]))))
          words)))

(defn- part-held
  "Takes a table's word, its handle and a partitioning key. Gives the rows
  of the table on the task that the key hashes to, selected straight from
  the PState: key -> row, with a run of bytes as it is. For the table of
  what a gate keeps for a task the keys and rows are `task-rows-held`'s.
  One select of the task's whole part; the tables of the proof fit in
  memory."
  [table handle pkey]
  (if (= :task table)
    (task-rows-held handle pkey)
    (into {} (rama/foreign-select [path/ALL] handle {:pkey pkey}))))

(defn- tables-read
  "Takes a module's handles. Gives every row of every table of each gate on
  every task, with a run of bytes as it is: gate -> table -> task -> key ->
  row (`part-held`)."
  [reader]
  (into {}
        (map (fn [[gate tables]]
               [gate (into {}
                           (map (fn [[table handle]]
                                  [table (into {}
                                               (map-indexed (fn [task k] [task (part-held table handle k)]))
                                               (:index-keys reader))]))
                           tables)]))
        (:tables reader)))

(defn- tables-held
  "Takes a module's handles. Gives what `tables-read` gives, with each run
  of bytes as hex text, so that two of them are compared with `=`."
  [reader]
  (bytes-as-hex (tables-read reader)))

(defn- bytes-outside-the-digests
  "Takes what `tables-read` gives. Gives [gate table task key] of each row
  that holds a run of bytes anywhere but under `:digest` of an answer's
  row. The fixture writes a run of bytes as hex text and tells it from a
  string by that one place, so the making writes nothing when this gives a
  row."
  [raw]
  (vec (for [[gate tables] raw
             [table by-task] tables
             [task rows] by-task
             [k row] rows
             :let [rest-of-row (if (and (= :answers table) (map? row)) (dissoc row :digest) row)]
             :when (some bytes? (tree-seq coll? seq rest-of-row))]
         [gate table task k])))

;; ----------------------------------------------------------- the comparison

(defn- kind-of
  "Takes the two rows that two sides hold under one key, of which one can be
  nil. Gives the kind of their difference: `:only-the-first-holds-it`,
  `:only-the-second-holds-it`, for two rows that are maps the sorted vector
  of the fields that differ, and for anything else `:the-value`."
  [of-first of-second]
  (cond
    (nil? of-second) :only-the-first-holds-it
    (nil? of-first) :only-the-second-holds-it
    (and (map? of-first) (map? of-second))
    (vec (sort (filter (fn [field] (not= (get of-first field) (get of-second field)))
                       (distinct (concat (keys of-first) (keys of-second))))))
    :else :the-value))

(defn- in-order
  "Takes keys of one level of what is compared: task numbers, texts, words
  or vectors. Gives them each once, the numbers first and in their order,
  the rest in the order of their printed texts."
  [ks]
  (sort-by (fn [k] (if (number? k) [0 k ""] [1 0 (pr-str k)])) (distinct ks)))

(defn- rows-apart
  "Takes a word for a gate and what two sides hold of it, each as table ->
  part -> key -> row. A part is what the rows are grouped by: a task
  number, the word `:any-task` for rows in the form with no task, or the
  text of a layer. Gives each key whose row is not the same on both sides,
  as {:gate :table :part :key :first :second :kind}, in the order of the
  tables, the parts and the keys. A key that one side lacks has nil there.
  A row is compared in the part where it is: the same row in another part
  of the other side differs twice."
  [gate of-first of-second]
  (vec (for [table (in-order (concat (keys of-first) (keys of-second)))
             part (in-order (concat (keys (get of-first table)) (keys (get of-second table))))
             :let [first-rows (get-in of-first [table part])
                   second-rows (get-in of-second [table part])]
             k (in-order (concat (keys first-rows) (keys second-rows)))
             :when (not= (get first-rows k) (get second-rows k))]
         {:gate gate :table table :part part :key k :first (get first-rows k) :second (get second-rows k)
          :kind (kind-of (get first-rows k) (get second-rows k))})))

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

(defn- says-different!
  "Prints rows that differ on lines that start with DIFFERENT, five at most
  for each gate and table (`examples`). Takes {:part :first :second}: a
  word for what a part is, and a word for each of the two sides; and the
  rows, as `rows-apart` gives them. Each row is cut to 400 characters."
  [{part-word :part first-side :first second-side :second} apart]
  (doseq [[_ of-table] (sort-by (comp pr-str key) (group-by (juxt :gate :table) apart))
          row (examples of-table)]
    (println "DIFFERENT" (name (:gate row)) (name (:table row)) part-word (pr-str (:part row))
             "key" (pr-str (:key row)) "| differs:" (pr-str (:kind row))
             (str "| " first-side ":") (cut (:first row)) (str "| " second-side ":") (cut (:second row))))
  (flush))

(defn- kinds-of
  "Takes rows that differ. Gives how many differ in each kind."
  [apart]
  (into {} (map (fn [[kind rows]] [kind (count rows)])) (group-by :kind apart)))

(defn- rows-in
  "Takes what one side holds of one table, part -> key -> row. Gives how
  many rows that is, over the parts."
  [by-part]
  (reduce + (map count (vals by-part))))

(defn- row-held
  "Takes what a store holds, a gate, a table's word and a key. Gives the
  row under that key on the first task that holds it, or nil."
  [held gate table k]
  (some (fn [rows] (get rows k)) (vals (get-in held [gate table]))))

(defn- word-held
  "Takes what a store holds, a gate and a thing that was sent to that
  gate's depot, or what the door gave for one. Gives the word of the answer
  under its act-id: the word of a refusal, :accepted, or :no-answer."
  [held gate thing]
  (let [[layer _ act-uuid] (:act-id thing)
        row (when (and layer act-uuid) (row-held held gate :answers (record/answer-key layer act-uuid)))]
    (if row (get row :refused :accepted) :no-answer)))

(defn- stamp-held
  "Takes what a store holds, a gate and a thing that was sent to that
  gate's depot. Gives the stamp of the answer under its act-id, or nil when
  the store holds no answer under it."
  [held gate thing]
  (let [[layer _ act-uuid] (:act-id thing)]
    (:stamp (row-held held gate :answers (record/answer-key layer act-uuid)))))

(defn- words-of-answers
  "Takes what a store holds of one gate. Gives how many answers hold each
  word, with :accepted for an answer with none."
  [held-of-gate]
  (into (sorted-map) (frequencies (map (fn [row] (get row :refused :accepted)) (mapcat vals (vals (:answers held-of-gate)))))))

(defn- streaming-layers-by-task
  "Takes what a store holds. Gives, for each task that holds one, the
  printed texts of the layers that the streaming gate made there, in order:
  each layer whose depot fact is in the task's part of the streaming
  facts."
  [held]
  (into (sorted-map)
        (keep (fn [[task rows]]
                (let [made (sort (keep (fn [k]
                                         (let [[layer entity fact-key] (record/place-of-key k)]
                                           (when (and (some? layer) (= layer entity) (= depot-key fact-key))
                                             (declaration/id-text layer))))
                                       (keys rows)))]
                  (when (seq made) [task (vec made)]))))
        (get-in held [:streaming :facts])))

(defn- what-the-cases-gave
  "Takes what a store holds (`tables-held`) and the hand's cases
  (`plays-the-hand!`). Gives, by the names of `cases-in-words`, what the
  store's tables say of each case: the word of an answer, how many things
  hold each word, or a few rows. It reads the tables alone, so it says the
  same of the first module and of the run again when their rows are the
  same."
  [held cases]
  (let [word (fn [gate thing] (word-held held gate thing))
        by-word (fn [gate things] (into (sorted-map) (frequencies (map (fn [thing] (word gate thing)) things))))
        ;; of one of the things whose time is chosen: its word; its stamp less the stamp of the first of
        ;; them; and the time that its answer holds and that its fact's row holds, :its-own for the time
        ;; on the thing, :none for a row with no time, :no-row for no row
        timed (fn [gate things thing]
                (let [own (get thing declaration/time-part)
                      [layer _ act-uuid] (:act-id thing)
                      {:keys [e k]} (first (:facts thing))
                      label (fn [row] (cond (nil? row) :no-row
                                            (not (contains? row :time)) :none
                                            (= own (:time row)) :its-own
                                            :else (:time row)))
                      stamp (stamp-held held gate thing)
                      first-stamp (stamp-held held gate (get things (first time-cases)))]
                  [(word gate thing)
                   (when (and stamp first-stamp) (- stamp first-stamp))
                   (label (row-held held gate :answers (record/answer-key layer act-uuid)))
                   (label (row-held held gate :facts (record/place-key layer e k)))]))
        ;; of one gate: each layer's stamps of its answers, and its last stamp, by the text of the layer id
        stamps-by-layer (fn [gate]
                          (reduce (fn [out [layer-text n]] (update out layer-text (fnil conj []) n))
                                  {}
                                  (for [rows (vals (get-in held [gate :answers]))
                                        [k row] rows]
                                    [(first (record/answer-key-parts k)) (:stamp row)])))
        last-by-layer (fn [gate]
                        (into {} (for [rows (vals (get-in held [gate :task]))
                                       [k n] rows
                                       :when (and (vector? k) (= :last-stamp (first k)))]
                                   [(second k) n])))
        in-each (fn [f] (into (sorted-map) (map (fn [gate] [gate (f gate)])) gate-order))
        wrote (:by-act (row-held held :microbatch :facts met-place))
        {:keys [x y]} (:x-and-y cases)]
    {:words (in-each (fn [gate] (words-of-answers (get held gate))))
     :before-made (in-each (fn [gate] (word gate (get-in cases [:before-made gate]))))
     :other-gate (into (sorted-map) (map (fn [[k [gate thing]]] [k (word gate thing)])) (:other-gate cases))
     :opening (mapv (fn [thing] (word :microbatch thing))
                    ((juxt :making :give :first-under-the-give) (:opening cases)))
     :under-the-give (in-each (fn [gate] (word gate (get-in cases [:under-the-give gate]))))
     :times (in-each (fn [gate]
                       (let [things (get-in cases [:times gate])]
                         (into (array-map) (map (fn [which] [which (timed gate things (get things which))])) time-cases))))
     :sent-again (in-each (fn [gate]
                            (let [{:keys [by-hand by-door]} (get-in cases [:sent-again gate])]
                              {:by-hand (word gate by-hand) :by-door (word gate by-door)})))
     :chains (in-each (fn [gate] (by-word gate (get-in cases [:chains gate]))))
     :each-part (in-each (fn [gate] (word gate (get-in cases [:each-part gate]))))
     :rooms (by-word :streaming (mapcat val (:rooms cases)))
     :refused (in-each (fn [gate] (by-word gate (get-in cases [:refused gate]))))
     :rights (in-each (fn [gate] (mapv (fn [thing] (word gate thing)) (get-in cases [:rights gate]))))
     :strays (in-each (fn [gate]
                        (apply merge-with + (sorted-map)
                               (for [rows (vals (get-in held [gate :task]))
                                     [k digests] rows
                                     :when (and (vector? k) (= :strays (first k)))]
                                 {(second k) (count digests)}))))
     :x-and-y (cond (nil? wrote) "no act"
                    (= wrote (nth (:act-id x) 2)) "X"
                    (= wrote (nth (:act-id y) 2)) "Y"
                    :else "another act")
     :layers-by-task (streaming-layers-by-task held)
     :last-stamps (in-each (fn [gate]
                             (into (sorted-map)
                                   (keep (fn [[task rows]]
                                           (let [of-layers (into (sorted-map)
                                                                 (keep (fn [[k n]] (when (and (vector? k) (= :last-stamp (first k))) [(second k) n])))
                                                                 rows)]
                                             (when (seq of-layers) [task of-layers]))))
                                   (get-in held [gate :task]))))
     :counted (in-each (fn [gate]
                         (let [lasts (last-by-layer gate)
                               of-layers (stamps-by-layer gate)]
                           {:layers (count of-layers)
                            :not-counted (vec (sort (keep (fn [[layer-text stamps]]
                                                            (when-not (and (= (sort stamps) (range (count stamps)))
                                                                           (= (get lasts layer-text) (dec (count stamps))))
                                                              layer-text))
                                                          of-layers)))})))}))

(def ^:private cases-in-words
  "The names of what `what-the-cases-gave` gives, in the order of the
  proof's lines, each with the words that its line begins with."
  [[:words "what each gate decided, by word, with :accepted for an accept"]
   [:before-made "an act into a layer before the layer is made, by gate"]
   [:other-gate "an act for a layer of the other gate, on each depot, before that layer is made and after"]
   [:opening "the base's making act, the give to Alice, and her first act under it"]
   [:under-the-give "the first act under a permission that was given later, by gate"]
   [:times (str "the things whose time is chosen, sent one behind the other, by gate, each as [word, its stamp less "
                "the stamp of the first, the time in its answer, the time in its fact's row], with :its-own for the time "
                "on the thing: a thing with no time; an act whose time is an hour behind; an act that carries the "
                "greatest long as a stamp; an act whose time is in the year 3084; an honest act behind it; and an honest "
                "act based on the fact of that one, with the stamp of its answer")]
   [:sent-again "an act sent twice under one act-id with two times, by the hand and through the door: the word of its one answer, by gate"]
   [:chains "the chain of acts that each replace the fact of the one before, under a marked key, with a retract at its end: how many of its acts hold each word, by gate"]
   [:each-part "an act with each optional part, by gate"]
   [:rooms "the acts into the seven rooms, a making act and a chain in each, sent in turn: how many hold each word"]
   [:refused "the acts that each have one fault of another kind: how many hold each word, by gate"]
   [:rights (str "rights that list keys, given, ended, given again and raised, and one write for a place: the word of "
                 "each act in the order sent, by gate")]
   [:strays "the strays that each gate keeps: how many digests under each word"]
   [:x-and-y "the place that X wrote and then Y holds the fact of"]
   [:layers-by-task "the layers that the streaming gate made, by the task that holds them"]
   [:last-stamps "each gate's last stamp of each layer, by task and by the text of the layer id"]
   [:counted (str "in each layer, whether the stamps of its answers are 0, 1, 2 and on with no gap and its last stamp "
                  "is the greatest of them: how many layers hold answers, and the layers where that does not hold, by gate")]])

;; ----------------------------------------------------- the form with no task

(defn- task-free
  "Brings what a store holds into the form with no task. Takes gate ->
  table -> task -> key -> row (`tables-held`). Gives {:tables :faults}.
  `:tables` is gate -> table -> key -> row:

    a table of `copied-tables`   the map of the first task: every task
                                 holds a whole copy
    any other table              the union of the tasks' rows
    under [:strays word]         the digests of all tasks, as a sorted
                                 vector

  `:faults` holds what the form cannot say and a caller must not pass
  over: {:fault :a-task-holds-another-copy ...} for each task whose map of
  a copied table is not the first task's, and {:fault :a-key-on-two-tasks
  ...} for each key of another table that a second task holds too. A
  layer's rows are on one task, so a store that is right gives no fault."
  [held]
  (let [stray-key? (fn [k] (and (vector? k) (= :strays (first k))))]
    (reduce
     (fn [out [gate table by-task]]
       (let [tasks (sort (keys by-task))]
         (if (contains? copied-tables [gate table])
           (let [one (get by-task (first tasks) {})]
             (-> out
                 (assoc-in [:tables gate table] one)
                 (update :faults into
                         (keep (fn [task]
                                 (when (not= one (get by-task task))
                                   {:fault :a-task-holds-another-copy :gate gate :table table :task task})))
                         (rest tasks))))
           (reduce
            (fn [out [task k row]]
              (let [at [:tables gate table k]
                    there (get-in out at ::none)]
                (cond
                  (stray-key? k) (assoc-in out at (vec (sort (into (set (when (not= ::none there) there)) row))))
                  (not= ::none there) (update out :faults conj {:fault :a-key-on-two-tasks :gate gate :table table :task task :key k})
                  :else (assoc-in out at row))))
            (assoc-in out [:tables gate table] {})
            (for [task tasks
                  [k row] (get by-task task)]
              [task k row])))))
     {:tables {} :faults []}
     (for [[gate tables] held
           [table by-task] tables]
       [gate table by-task]))))

(defn- layer-text-of
  "Takes a key of a table in the proof's form: the text of a row's key, or
  [:last-stamp text], or [:strays word]. Gives the printed text of the
  layer that the row belongs to: the first part of a row's key, which every
  key of a row begins with, or the text of a last stamp. nil for a
  stray's."
  [k]
  (cond (string? k) (first (str/split k #" " 2))
        (and (vector? k) (= :last-stamp (first k))) (second k)
        :else nil))

(defn- rows-of-layer
  "Takes rows, key -> row, and the printed text of a layer id. Gives the
  rows of that layer (`layer-text-of`)."
  [rows layer-text]
  (into {} (filter (fn [[k _]] (= layer-text (layer-text-of k)))) rows))

(defn- by-layer-in-order
  "Takes one depot's records by partition (`records-of`). Gives a map from
  each layer to its records, in the order of the partitions and of each
  partition's records. The layer of a thing is its `:layer`, what the
  depot's partitioner reads; nil for a thing that is no map or holds none.
  A layer is one partition, so this is each layer's order in the depot,
  said with no partition number."
  [records]
  (reduce (fn [out thing] (update out (when (map? thing) (:layer thing)) (fnil conj []) thing))
          {}
          (apply concat records)))

(defn- times-under
  "Takes one depot's records by partition and an act-id. Gives the times on
  the records under that act-id, in the depot's order."
  [records act-id]
  (vec (for [of-partition records
             thing of-partition
             :when (and (map? thing) (= act-id (:act-id thing)))]
         (get thing declaration/time-part))))

;; ---------------------------------------------------------- the naming rule

(def ^:private stores-own
  "The store's own three pure functions that check 4 puts functions of the
  proof in the place of, taken when this namespace is loaded, which is
  after the store's module is: the test for a stray and the declaration's
  check (`softland.store.door.declaration`), and the writes of a decision
  (`softland.store.gate.decision/writes`). The naming rule calls the test
  for a stray from here, so that it never calls a function of the proof;
  the proof's functions call these; and `with-failures-in-the-walk` puts
  them back."
  {:stray-reason declaration/stray-reason
   :check declaration/check
   :writes decision/writes})

(defn- acts-in-order
  "Takes a gate's depot word and one partition's records of that gate's
  depot, in the partition's order. Gives the acts of the partition, in that
  order, each as {:position :layer :act-id}: each record that is no stray
  of the gate (the store's own `stray-reason`) and is the first of the
  partition under its act-id. A later record under an act-id that an
  earlier one has is a repeat: a gate decides the first and writes nothing
  for the rest. The position is the record's place in the vector, from 0,
  which is its offset in a depot that is never trimmed."
  [gate records]
  (first
   (reduce (fn [[out seen] [position thing]]
             (if (some? ((:stray-reason stores-own) gate thing))
               [out seen]
               (let [act-id (get thing :act-id)]
                 (if (contains? seen act-id)
                   [out seen]
                   [(conj out {:position position :layer (nth act-id 0) :act-id act-id}) (conj seen act-id)]))))
           [[] #{}]
           (map-indexed vector records))))

(defn- out-of-their-line
  "The naming rule. Pure. Takes a gate's depot word; one partition's
  records of that gate's depot, in the partition's order; and a function
  from an act-id to the stamp of its answer, or nil for an act with none.
  Gives the acts that are out of their line, in the partition's order,
  each as {:position :layer :act-id :stamp :rule} and, by the second part,
  `:against`. Within ONE layer an act (`acts-in-order`) is out of its line
  when

    1. `:no-answer`: the depot holds it and it has no answer; or
    2. `:stamp-not-in-the-order-of-its-place`: an act of its layer stands
       before it in the partition with a higher stamp, or behind it with a
       lower one. `:against` then holds the act before it with the highest
       stamp, the act behind it with the lowest, or both, each as
       {:position :act-id :stamp :stands}.

  So both acts of a pair that was decided in the other order are named.
  Nothing is said across two layers: two layers of one partition count
  apart, and can hold the same stamp. A stray is not named, and a repeat is
  not judged by itself. Two acts of a layer with one stamp are not named:
  the rule says higher and lower.

  What must hold for the first part to say something: the store is at
  rest. What the rule does not say: which acts behind a named one hold
  another answer than a run again gives; each act of the layer behind the
  first named one may."
  [gate records stamp-of]
  (let [acts (mapv (fn [an-act] (assoc an-act :stamp (stamp-of (:act-id an-act)))) (acts-in-order gate records))
        seen-against (fn [other stands] (assoc (select-keys other [:position :act-id :stamp]) :stands stands))
        ;; of the acts of one layer that have a stamp, in their order
        named-in (fn [line]
                   (let [line (vec line)
                         best (fn [better?] (fn [best an-act] (if (or (nil? best) (better? (:stamp an-act) (:stamp best))) an-act best)))
                         ;; for each place: the act before it with the highest stamp, and the act behind it with the lowest
                         highest-before (vec (butlast (reductions (best >) nil line)))
                         lowest-behind (vec (rest (reverse (reductions (best <) nil (reverse line)))))]
                     (keep (fn [i]
                             (let [an-act (nth line i)
                                   before (nth highest-before i)
                                   behind (nth lowest-behind i)
                                   against (cond-> []
                                             (and before (> (:stamp before) (:stamp an-act)))
                                             (conj (seen-against before :before-it-with-a-higher-stamp))
                                             (and behind (< (:stamp behind) (:stamp an-act)))
                                             (conj (seen-against behind :behind-it-with-a-lower-stamp)))]
                               (when (seq against)
                                 (assoc an-act :rule :stamp-not-in-the-order-of-its-place :against against))))
                           (range (count line)))))
        with-no-answer (map (fn [an-act] (assoc an-act :rule :no-answer)) (remove :stamp acts))
        not-in-order (mapcat named-in (vals (group-by :layer (filter :stamp acts))))]
    (vec (sort-by :position (concat with-no-answer not-in-order)))))

(defn- named-in-a-store
  "Applies the naming rule to every partition of both depots of a store.
  Takes what the store holds (`tables-held`), and the records of the two
  depots of acts by gate and partition (`records-of`); a second module that
  mirrors the depots is read against the same records. Gives the named
  acts, as `out-of-their-line` gives them, each with its `:gate` and its
  `:partition`."
  [held records]
  (vec (for [gate gate-order
             :let [stamps (into {} (for [rows (vals (get-in held [gate :answers]))
                                         [k row] rows]
                                     [k (:stamp row)]))
                   stamp-of (fn [[layer _ act-uuid]] (get stamps (record/answer-key layer act-uuid)))]
             [partition of-partition] (map-indexed vector (get records gate))
             named (out-of-their-line gate of-partition stamp-of)]
         (assoc named :gate gate :partition partition))))

(def ^:private most-lines-out-of-line
  "How many named acts of one store the proof prints, each on a line of its
  own; the count of the rest is printed on one line more."
  100)

(defn- says-out-of-their-line!
  "Prints each named act on a line that begins OUT OF ITS LINE: whose store
  it is, the gate, the layer, the act-id, its partition and position, the
  part of the rule, its stamp, and for the second part the acts that it is
  out of order against. Takes a word for the store and the named acts
  (`named-in-a-store`). It prints `most-lines-out-of-line` acts at most,
  and then how many it left out."
  [whose named]
  (doseq [{:keys [gate layer act-id partition position rule stamp against]} (take most-lines-out-of-line named)]
    (println "OUT OF ITS LINE" whose "|" (name gate) "gate | layer" (pr-str layer) "| act-id" (pr-str act-id)
             "| partition" partition "position" position "| rule:" (name rule) "| stamp:" (pr-str stamp)
             (if (seq against) (str "| against: " (cut against)) "")))
  (when (< most-lines-out-of-line (count named))
    (println "OUT OF ITS LINE" whose "| and" (- (count named) most-lines-out-of-line) "more acts, not printed"))
  (flush))

(defn- not-accounted-for
  "Takes the rows that differ between a store and its run again
  (`rows-apart`, over both gates), the acts that the naming rule names in
  the store (`named-in-a-store`), and the records of the two depots by gate
  and partition. Gives the rows that differ and that the rule does not
  account for. A row is accounted for when its layer holds a named act,
  and each act that the row is of stands at or behind the first named act
  of the layer in the depot: the act of an answer's key; for a fact or a
  fact by value, the act that wrote the row on either side. The row of a
  layer's last stamp is of the layer alone. A row of strays, and a row of
  an act that the depot does not hold, are never accounted for."
  [apart named records]
  (let [place-of (into {}
                       (for [gate gate-order
                             of-partition (get records gate)
                             {:keys [position layer act-id]} (acts-in-order gate of-partition)]
                         [[gate (declaration/id-text layer) (nth act-id 2)] position]))
        first-named (reduce (fn [out {:keys [gate layer position]}]
                              (update out [gate (declaration/id-text layer)] (fnil min position) position))
                            {}
                            named)]
    (filterv (fn [{:keys [gate table] k :key of-first :first of-second :second}]
               (let [layer-text (layer-text-of k)
                     from (get first-named [gate layer-text])
                     act-uuids (cond
                                 (= :answers table) [(second (record/answer-key-parts k))]
                                 (= :task table) []
                                 :else (distinct (keep :by-act [of-first of-second])))
                     places (map (fn [act-uuid] (get place-of [gate layer-text act-uuid])) act-uuids)]
                 (not (and (some? layer-text)
                           (some? from)
                           (every? (fn [place] (and (some? place) (<= from place))) places)))))
             apart)))

;; ------------------------------------------------------- the fixture's files

(def ^:private fixture-folder
  "The folder of the fixture's two files, from the repository root: where
  `make-fixture` writes when no folder is named."
  "test-store/softland/store/run_again_fixture")

(def ^:private fixture-files
  "The names of the fixture's two files, in their folder and as resources
  of the classpath under `softland/store/run_again_fixture/`."
  {:acts "acts.edn" :tables "tables.edn"})

(def ^:private made-again-when
  "When the fixture may be made again: one sentence of a rule that Sid
  pasted into his prompt of 13 October 2026 as his decision 6, a session's
  text that he did not type, with what its first words point back to. Each
  file's head holds it."
  (str "A session's text, which Sid pasted into his prompt of 13 October 2026 as his decision 6, "
       "of a change in what a gate accepts, before the first real record: "
       "\"Until then the fixture is made again only when a rule is changed on purpose, and the receipt says so.\""))

(def ^:private head-order
  "The parts of a file's head, in the order in which they are written."
  [:what :made :tasks :records :rows :bytes :made-again-when])

(defn- edn-text
  "Takes a thing. Gives its printed text on one line, as EDN when the thing
  is one that EDN holds: no length or depth is cut, and a map prints each
  key whole."
  [x]
  (binding [*print-length* nil
            *print-level* nil
            *print-namespace-maps* false
            *print-meta* false
            *print-dup* false
            *print-readably* true]
    (pr-str x)))

(defn- kind-in-edn
  "Takes anything. Gives the kind of thing that it is for a file of the
  fixture: :nil; :map, :set, :vector or :list for a plain one of those; for
  anything else its class. A record, a sorted map, a sorted set and a
  sequence that is no list each have a kind of their own, which nothing
  that the EDN reader gives has: so a thing that holds one does not read
  back as itself."
  [x]
  (cond
    (nil? x) :nil
    (record? x) [:a-record (class x)]
    (map? x) (if (sorted? x) :a-sorted-map :map)
    (set? x) (if (sorted? x) :a-sorted-set :set)
    (vector? x) :vector
    (list? x) :list
    (seq? x) [:a-sequence (class x)]
    :else (class x)))

(defn- first-not-same
  "Takes a thing that is to be written, the thing that the EDN reader gave
  for its text, and the path so far. Gives nil when the second is the
  first, part for part and class for class: the same kind (`kind-in-edn`)
  at each place, the same count, each key and each element the same, and
  each leaf equal and of one class. Else {:at :wrote :read}: the path of
  the first place that differs, and both things there. `=` alone would
  pass a long that came back as a big integer, and a sorted map that came
  back as a plain one."
  [wrote read at]
  (let [kind (kind-in-edn wrote)
        differs {:at at :wrote wrote :read read}]
    (cond
      (not= kind (kind-in-edn read)) differs
      (= :map kind) (if (not= (count wrote) (count read))
                      differs
                      (some (fn [[k v]]
                              (let [entry (find read k)]
                                (if (nil? entry)
                                  {:at (conj at k) :wrote v :read :no-such-key}
                                  (or (first-not-same k (key entry) (conj at k :the-key))
                                      (first-not-same v (val entry) (conj at k))))))
                            wrote))
      (= :set kind) (if (not= (count wrote) (count read))
                      differs
                      (some (fn [x]
                              (let [y (get read x ::none)]
                                (if (= ::none y)
                                  {:at (conj at x) :wrote x :read :no-such-element}
                                  (first-not-same x y (conj at x)))))
                            wrote))
      (contains? #{:vector :list} kind) (if (not= (count wrote) (count read))
                                          differs
                                          (some identity
                                                (map (fn [i a b] (first-not-same a b (conj at i)))
                                                     (range) wrote read)))
      :else (when (not= wrote read) differs))))

(defn- not-read-back
  "Takes a thing and the text that is to be written for it. Reads the text
  as EDN. Gives nil when what was read is the thing (`first-not-same`);
  else the first place that differs, or what the reader threw."
  [thing text]
  (try
    (first-not-same thing (edn/read-string text) [])
    (catch Throwable t
      {:the-reader-threw (str t)})))

(defn- head-text
  "Takes a file's head, a map. Gives its text: the parts of `head-order`
  that it holds, one on each line."
  [head]
  (str "{"
       (str/join "\n  " (for [part head-order
                              :when (contains? head part)]
                          (str (edn-text part) " " (edn-text (get head part)))))
       "}"))

(defn- acts-of
  "Takes the number of tasks of a store and its records by depot and
  partition (`records-of`). Gives the value of the file `acts.edn`:
  {:head :depots}. `:depots` is depot -> a vector with one vector for each
  partition in order, each with the partition's records in its order."
  [tasks records]
  {:head {:what (str "The records of the two depots of acts of a store, as the hand of softland.store.run-again-proof "
                     "left them: for each depot, for each partition in order, its records in the partition's order, "
                     "each as it was appended.")
          :made (str (LocalDate/now))
          :tasks tasks
          :records (into {} (map (fn [gate] [gate (mapv (fn [of-partition] (long (count of-partition))) (get records gate))])) gate-order)
          :made-again-when made-again-when}
   :depots (into {} (map (fn [gate] [gate (mapv vec (get records gate))])) gate-order)})

(defn- acts-text
  "Takes the value of the file `acts.edn` (`acts-of`). Gives the file's
  text: the head, then each depot, each partition and each record, one
  record on a line."
  [{:keys [head depots]}]
  (str "{:head\n " (head-text head) "\n :depots\n {"
       (str/join "\n  "
                 (for [gate gate-order]
                   (str (edn-text gate) "\n  ["
                        (str/join "\n   "
                                  (for [records (get depots gate)]
                                    (str "[" (str/join "\n    " (map edn-text records)) "]")))
                        "]")))
       "}}\n"))

(defn- rows-in-file-order
  "Takes the rows of one table, key -> row. Gives them as a vector of
  [key row], in the order of the printed texts of their keys."
  [rows]
  (vec (sort-by (comp pr-str first) (map (fn [[k row]] [k row]) rows))))

(defn- tables-of
  "Takes the number of tasks of a store and its tables in the form with no
  task (`task-free`). Gives the value of the file `tables.edn`:
  {:head :tables}. `:tables` is gate -> table -> the rows as a sorted
  vector of [key row] (`rows-in-file-order`)."
  [tasks tables]
  (let [sorted (into {}
                     (map (fn [gate]
                            [gate (into {} (map (fn [table] [table (rows-in-file-order (get-in tables [gate table]))])) table-order)]))
                     gate-order)]
    {:head {:what (str "The tables that the records of acts.edn gave, in a form that names no task: for each gate and "
                       "each of its four tables, the rows as a sorted vector of [key row]. The microbatch gate's facts "
                       "and facts by value: the one map that every task holds. Every other table: the union of the "
                       "tasks' rows. What a gate keeps for a task: each layer's last stamp under [:last-stamp text], "
                       "and the strays under [:strays word] as the sorted digests of all tasks.")
            :made (str (LocalDate/now))
            :tasks tasks
            :rows (into {} (map (fn [[gate of-gate]] [gate (into {} (map (fn [[table rows]] [table (long (count rows))])) of-gate)])) sorted)
            :bytes (str "A run of bytes is written as hex text. The one position that is a run of bytes is :digest "
                        "of an answer's row; every other string is a string in its table.")
            :made-again-when made-again-when}
     :tables sorted}))

(defn- tables-text
  "Takes the value of the file `tables.edn` (`tables-of`). Gives the file's
  text: the head, then each gate and each table, one row on a line."
  [{:keys [head tables]}]
  (str "{:head\n " (head-text head) "\n :tables\n {"
       (str/join "\n  "
                 (for [gate gate-order]
                   (str (edn-text gate) "\n  {"
                        (str/join "\n   "
                                  (for [table table-order]
                                    (str (edn-text table) "\n   ["
                                         (str/join "\n    " (map edn-text (get-in tables [gate table])))
                                         "]")))
                        "}")))
       "}}\n"))

(defn- fixture-read
  "Reads the fixture's two files as resources of the classpath. Gives
  {:acts :tables}, the value of each file, or nil when one of them is
  absent."
  []
  (let [resource (fn [which] (io/resource (str "softland/store/run_again_fixture/" (get fixture-files which))))
        acts (resource :acts)
        tables (resource :tables)]
    (when (and acts tables)
      {:acts (edn/read-string (slurp acts))
       :tables (edn/read-string (slurp tables))})))

(defn- tables-of-the-file
  "Takes the value of the file `tables.edn`. Gives its tables as gate ->
  table -> key -> row: the form that `task-free` gives of a store."
  [file]
  (update-vals (:tables file) (fn [of-gate] (update-vals of-gate (fn [rows] (into {} rows))))))

;; ---------------------------------------------------------------- the run

(defn- on-a-store
  "Creates an in-process cluster, launches the module `Store` in it with
  the number of tasks given, two threads and two workers, calls a function
  of no argument with the hand on that cluster, and closes the cluster.
  Gives what the function gave. A cluster for each call: a module of
  another number of tasks cannot share a cluster's `Store`."
  [tasks f]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (binding [*hand* (open-hand ipc tasks)]
      (f))))

;; ------------------------------------------- the naming rule, on lines made by hand

(deftest the-naming-rule-names-the-acts-out-of-their-line
  (binding [*lead* "the naming rule, on lines made by hand, with no cluster"]
    (let [uuid-of-number (fn [n] (UUID. 0 n))
          ;; a thing of the streaming depot that is no stray: the n-th act of a layer
          an-act (fn [layer n] {:act-id [layer :streaming (uuid-of-number n)] :depot :streaming :layer layer
                                :version 1 :who :alice :permission [:alice layer layer] :session :proof-session
                                :facts [{:e :place :k :text :v (str "act " n)}]})
          [a b c d] (mapv (fn [n] (an-act :line n)) [1 2 3 4])
          [x y] (mapv (fn [n] (an-act :other n)) [5 6])
          id :act-id
          ;; the named acts of one partition, as [position rule the positions that it is out of order against]
          named (fn [records stamps]
                  (mapv (fn [{:keys [position rule against]}] [position rule (mapv :position against)])
                        (out-of-their-line :streaming records (fn [act-id] (get stamps act-id)))))
          in-order (named [a b c d] {(id a) 10 (id b) 20 (id c) 30 (id d) 40})
          swapped (named [a b c d] {(id a) 10 (id b) 30 (id c) 20 (id d) 40})
          last-first (named [a b c d] {(id a) 50 (id b) 20 (id c) 30 (id d) 40})
          no-answer (named [a b c d] {(id a) 10 (id b) 20 (id d) 40})
          a-repeat (named [a b (assoc b :door-time 99) (assoc b :facts [{:e :place :k :text :v "another thing under a used act-id"}]) c]
                          {(id a) 10 (id b) 20 (id c) 30})
          strays (named [a 5 (dissoc b :act-id) (assoc c :act-id [:line :streaming]) (assoc c :act-id [:line :microbatch (uuid-of-number 7)])
                         (assoc d :act-id [:elsewhere :streaming (uuid-of-number 8)]) b]
                        {(id a) 10 (id b) 20})
          two-layers (named [a x b y] {(id a) 10 (id x) 5 (id b) 20 (id y) 7})
          ;; rows that differ, held against one named act: b, at position 1 of the layer :line
          key-of (fn [thing] (record/answer-key (nth (id thing) 0) (nth (id thing) 2)))
          differs (fn [table k of-first] {:gate :streaming :table table :part 0 :key k :first of-first :second nil :kind :only-the-first-holds-it})
          apart [(differs :answers (key-of a) {:stamp 10})
                 (differs :answers (key-of b) {:stamp 30})
                 (differs :facts ":line :place :text" {:by-act (nth (id c) 2)})
                 (differs :task [:last-stamp ":line"] 40)
                 (differs :answers (key-of x) {:stamp 5})
                 (differs :task [:strays :no-act-id] #{"00"})]
          left (mapv :key (not-accounted-for apart
                                             [{:gate :streaming :partition 0 :position 1 :layer :line :act-id (id b) :rule :no-answer}]
                                             {:streaming [[a b c d x]] :microbatch [[]]}))]
      (is (= [] in-order))
      (is (= [[1 :stamp-not-in-the-order-of-its-place [2]] [2 :stamp-not-in-the-order-of-its-place [1]]] swapped))
      (is (= [[0 :stamp-not-in-the-order-of-its-place [1]]
              [1 :stamp-not-in-the-order-of-its-place [0]]
              [2 :stamp-not-in-the-order-of-its-place [0]]
              [3 :stamp-not-in-the-order-of-its-place [0]]]
             last-first))
      (is (= [[2 :no-answer []]] no-answer))
      (is (= [] a-repeat))
      (is (= [] strays))
      (is (= [] two-layers))
      (is (= [(key-of a) (key-of x) [:strays :no-act-id]] left))
      (shown "named, as [position, part of the rule, the positions that it is out of order against] | four acts of one layer with stamps in the order of their places:"
             (pr-str in-order) "| the stamps of the second and the third swapped:" (pr-str swapped)
             "| the first act with the highest stamp of all:" (pr-str last-first)
             "| the third act with no answer, behind two decided ones and before one:" (pr-str no-answer)
             "| two repeats under the act-id of the second act:" (pr-str a-repeat)
             "| five strays between two acts:" (pr-str strays)
             "| two layers on one partition, each in its own order, their stamps not in one order together:" (pr-str two-layers))
      (shown "rows that differ, held against one named act at position 1 of its layer | of six rows, of the act at position 0, of the named act, of an act behind it, of the layer's last stamp, of an act of another layer, and of strays, the rule does not account for:"
             (pr-str left)))))

;; ------------------------------------------------------- check 1: the run again

(deftest the-gates-run-again-decide-the-same
  (binding [*lead* "check 1, the run again"]
    (on-a-store
     tasks-of-check-1
     (fn []
       (let [{:keys [ipc module]} *hand*
             numbers numbers-of-check-1
             again-name (rama/get-module-name StoreAgain)
             cases (plays-the-hand! numbers)
             {:keys [making-at give-at]} (:opening cases)
             {:keys [x x-at y-at x-answered by-act-before-y]} (:x-and-y cases)
             rested (first-at-rest!)
             ends {:streaming (ends-of :streaming) :microbatch (ends-of :microbatch)}
             records-in-base (reduce + (:microbatch ends))
             first-before (tables-held (:first *hand*))
             ;; the run again
             launch-began (System/currentTimeMillis)
             _ (rtest/launch-module! ipc StoreAgain {:tasks tasks-of-check-1 :threads 2 :workers 2})
             again (open-reader ipc again-name tasks-of-check-1)
             again-rested (run-again-at-rest! again records-in-base)
             run-again-ms (- (System/currentTimeMillis) launch-began)
             first-held (tables-held (:first *hand*))
             again-held (tables-held again)
             records (into {} (map (fn [gate] [gate (records-of gate)])) gate-order)
             named-first (named-in-a-store first-held records)
             named-again (named-in-a-store again-held records)
             by-door @(:by-door *hand*)
             by-hand @(:by-hand *hand*)
             not-appended @(:not-appended *hand*)
             gave-first (what-the-cases-gave first-held cases)
             gave-again (what-the-cases-gave again-held cases)
             met (places-met-by-the-load numbers)
             ;; of one copied table of one module: how many rows each task holds, and whether every task holds one map
             copies (fn [held]
                      (into {}
                            (map (fn [[gate table]]
                                   (let [by-task (get-in held [gate table])]
                                     [table {:rows-by-task (mapv (fn [task] (count (get by-task task))) (sort (keys by-task)))
                                             :one-map? (boolean (and (seq by-task) (apply = (vals by-task))))}])))
                            (sort copied-tables)))
             copies-first (copies first-held)
             copies-again (copies again-held)]
         ;; what was sent
         (is (every? :act-id by-door))
         (is (zero? (count not-appended)))
         (shown "sent to the module" module "of" tasks-of-check-1 "tasks |" (count by-door)
                "acts through the door, each appended:" (every? :act-id by-door) "|" (count by-hand)
                "things by the hand, with no door, each acked:" (zero? (count not-appended))
                "| in them the load:" (:base-fillers numbers) "acts into the base with no wait for the gate, and"
                (:streaming-fillers numbers) "acts into one streaming layer")
         (shown "records in the depots of acts, by partition | streaming:" (pr-str (:streaming ends))
                "| microbatch:" (pr-str (:microbatch ends)))
         (is (= @(:appended-to-base *hand*) records-in-base))
         ;; the base opens on one line
         (is (and making-at give-at (= (first making-at) (first give-at)) (< (second making-at) (second give-at))))
         (shown "the base's making act and the give to Alice, which the door sent when the making act was decided: the making act is at position"
                (second making-at) "of partition" (first making-at) "of the microbatch depot, the give at position"
                (second give-at) "of partition" (first give-at) "| the base is one partition, and the give stands behind the making act")
         ;; the two acts on one place stand as the proof says
         (is (and x-at y-at (= (first x-at) (first y-at)) (< (second x-at) (second y-at))))
         (is (and (some? x-answered) (= by-act-before-y (nth (:act-id x) 2))))
         (shown "the acts X and Y of the system, two acts that write one place, sent one after the other: X is at position"
                (second x-at) "of partition" (first x-at) "of the microbatch depot, Y at position" (second y-at)
                "of partition" (first y-at) "| before Y was sent the first module had answered X:"
                (pr-str (if x-answered (get x-answered :refused :accepted) :no-answer))
                "and the place held the fact of X:" (= by-act-before-y (nth (:act-id x) 2))
                "| both stand on the base's one partition, X before Y, so a gate has one order for them however it cuts its batches")
         ;; the first module at rest
         (is (= {:processed? true :answered? true :quiet? true} (dissoc rested :waited-ms)))
         (shown "the first module is at rest: its microbatch gate processed each of the" @(:appended-to-base *hand*)
                "things of its depot; each of the" (count @(:to-be-answered *hand*))
                "sends through the door has its answer; nothing changed in" quiet-ms "ms:"
                (pr-str (dissoc rested :waited-ms)) "| waited" (:waited-ms rested) "ms")
         ;; the second module decided everything
         (is (= {:base-decided? true :streaming-decided? true :quiet? true} (dissoc again-rested :waited-ms)))
         (shown "the second module," again-name
                "was launched when the first was at rest, with mirrors of the two depots of acts and the store's own gates and reads; its microbatch gate processed"
                records-in-base "things, its streaming tables came to hold as many answers and strays on each task as the first module's, and nothing changed in"
                quiet-ms "ms:" (pr-str (dissoc again-rested :waited-ms)) "| launch and wait together:" run-again-ms "ms")
         (is (true? (= first-before first-held)))
         (shown "the first module's tables are the same before the second module was launched and after it came to rest:"
                (= first-before first-held))
         ;; the cases by name: what each module holds of them. These lines say; the check is below.
         (doseq [[k says] cases-in-words]
           (shown says "| the first module:" (pr-str (get gave-first k)) "| the run again:" (pr-str (get gave-again k))))
         (shown "of the" (count met)
                "places that the load's acts of the system write from several first entities, the two modules hold the fact of two different acts at"
                (count (filter (fn [k] (not= (:by-act (row-held first-held :microbatch :facts k))
                                             (:by-act (row-held again-held :microbatch :facts k))))
                               met)))
         ;; the copies on every task
         (is (every? :one-map? (vals copies-first)))
         (is (every? :one-map? (vals copies-again)))
         (shown "the microbatch gate's facts and its facts by value, of which every task holds a copy: rows on each task, and whether every task holds one and the same map | the first module:"
                (pr-str copies-first) "| the run again:" (pr-str copies-again))
         ;; the comparison
         (let [apart (vec (mapcat (fn [gate] (rows-apart gate (get first-held gate) (get again-held gate))) gate-order))
               apart-of (fn [gate] (count (filter (fn [row] (= gate (:gate row))) apart)))]
           (doseq [gate gate-order
                   table table-order
                   :let [of-table (filterv (fn [row] (and (= gate (:gate row)) (= table (:table row)))) apart)]]
             (shown "the" (name gate) "gate's table" (str (name table) ":")
                    "the first module holds" (rows-in (get-in first-held [gate table])) "rows over the" tasks-of-check-1
                    "tasks, the run again" (rows-in (get-in again-held [gate table]))
                    "| rows that differ:" (count of-table) "| by what differs:" (pr-str (kinds-of of-table))))
           (says-different! {:part "task" :first "first" :second "again"} apart)
           (is (zero? (count apart)))
           (shown "every row of the answers, the facts, the facts by value and what a gate keeps for a task, of both gates, on each of the"
                  tasks-of-check-1 "tasks, the first module held against the run again | rows that differ:" (count apart)
                  "| of the streaming gate:" (apart-of :streaming) "| of the microbatch gate:" (apart-of :microbatch)))
         ;; the naming rule: no act of either module is out of its line
         (says-out-of-their-line! "the first module" named-first)
         (says-out-of-their-line! "the run again" named-again)
         (is (zero? (count named-first)))
         (is (zero? (count named-again)))
         (shown "the naming rule, applied to each partition of both depots, against each module's answers | acts that the depots hold, strays and repeats left out:"
                (reduce + (for [gate gate-order of-partition (get records gate)] (count (acts-in-order gate of-partition))))
                "| acts out of their line in the first module:" (count named-first) "| in the run again:" (count named-again)))))))

;; ------------------------------------------------- checks 2 and 3: the fixture

(defn- feeds-the-fixtures-records!
  "Appends the fixture's records to the two depots of the store of `*hand*`
  by hand, each as it is: the time on a record is its own (`as-it-is!`).
  Takes the value of the file `acts.edn`, and the order: `:as-written`, the
  streaming depot before the microbatch depot and each depot's partitions
  from the first to the last; or `:reversed`, the microbatch depot first
  and each depot's partitions from the last to the first. In both, the
  records of one partition are appended in their order, one after the
  other from this one thread, each acked before the next is sent. That
  keeps each layer's order, since a layer is in one partition. The order
  between two partitions, and between the two depots, is the caller's to
  choose because it must not matter: no order holds between two
  partitions, and no gate reads the other's tables."
  [acts order]
  (let [turned (fn [xs] (if (= :reversed order) (reverse xs) xs))]
    (doseq [gate (turned gate-order)
            records (turned (get-in acts [:depots gate]))
            thing records]
      (as-it-is! gate thing))))

(defn- placed-by-layer
  "The check of placement with no task number. Takes a store's handles and
  the fixture's tables with no task (`tables-of-the-file`). For each gate
  and each layer of which the fixture holds an answer, a last stamp or,
  in the streaming gate, a fact, it reads one task: the task that a select
  with the layer as its partitioning key goes to. Of that task's part of
  the gate's answers, of what the gate keeps for a task, and for the
  streaming gate of its facts and its facts by value, it keeps the rows of
  the layer, and holds them against the fixture's rows of the layer. The
  microbatch gate's facts are left to the check of the copies: every task
  holds them. Gives {:layers :rows :apart}: for each gate the texts of the
  layers; how many rows of the fixture were looked for; and the rows that
  differ, as `rows-apart` gives them with the layer's text as the part. A
  layer whose text is no id is looked for nowhere, so each of its rows
  differs."
  [reader expected]
  (let [tables-of-gate {:streaming [:answers :task :facts :facts-by-value]
                        :microbatch [:answers :task]}
        layers (into {}
                     (map (fn [[gate tables]]
                            [gate (vec (sort (distinct (keep layer-text-of
                                                             (mapcat (fn [table] (keys (get-in expected [gate table]))) tables)))))]))
                     tables-of-gate)
        looked (vec (for [gate gate-order
                          layer-text (get layers gate)
                          table (get tables-of-gate gate)
                          :let [wanted (rows-of-layer (get-in expected [gate table]) layer-text)
                                layer (record/id-of-text layer-text)
                                found (if (some? layer)
                                        (rows-of-layer (bytes-as-hex (part-held table (get-in reader [:tables gate table]) layer))
                                                       layer-text)
                                        {})]]
                      {:gate gate :table table :layer layer-text :wanted wanted :found found}))]
    {:layers layers
     :rows (reduce + (map (comp count :wanted) looked))
     :apart (vec (mapcat (fn [{:keys [gate table layer wanted found]}]
                           (rows-apart gate {table {layer wanted}} {table {layer found}}))
                         looked))}))

(defn- holds-the-fixture!
  "The check of the fixture on a store of the number of tasks given, with
  the order of the feeding given (`feeds-the-fixtures-records!`). It reads
  the fixture's files, and when one is absent it fails with the line \"the
  fixture is not made: run make-fixture\" and launches nothing. Else it
  launches `Store` on a cluster of its own, feeds the fixture's records,
  waits for rest, and compares three times, as the namespace docstring
  says: the tables with no task, row by row; each task's copy of the
  microbatch gate's facts and of its facts by value; and the placement of
  each layer with no task number. It also holds the feeding to what it
  rests on: each append acked, and each layer's records in the fed depot
  in the fixture's order. It passes when no row differs in any of the
  three and the form with no task found no fault. The stray sets of a task
  are compared in the first of the three only."
  [tasks order]
  (if-let [{:keys [acts] file :tables} (fixture-read)]
    (on-a-store
     tasks
     (fn []
       (let [expected (tables-of-the-file file)
             began (System/currentTimeMillis)
             _ (feeds-the-fixtures-records! acts order)
             rested (first-at-rest!)
             fed (into {} (map (fn [gate] [gate (records-of gate)])) gate-order)
             reader (:first *hand*)
             held (tables-held reader)
             {:keys [faults] here :tables} (task-free held)
             in-one-part (fn [of-gate] (update-vals of-gate (fn [rows] {:any-task rows})))
             apart (vec (mapcat (fn [gate] (rows-apart gate (in-one-part (get expected gate)) (in-one-part (get here gate))))
                                gate-order))
             copies-apart (vec (for [[gate table] (sort copied-tables)
                                     task (range tasks)
                                     row (rows-apart gate
                                                     {table {task (get-in expected [gate table])}}
                                                     {table {task (get-in held [gate table task] {})}})]
                                 row))
             placed (placed-by-layer reader expected)
             named (named-in-a-store held fed)
             in-order? (every? (fn [gate] (= (by-layer-in-order (get-in acts [:depots gate])) (by-layer-in-order (get fed gate))))
                               gate-order)
             not-appended @(:not-appended *hand*)
             sides {:first "fixture" :second "store"}]
         ;; the feeding
         (is (zero? (count not-appended)))
         (is (true? in-order?))
         (shown "the fixture of" (get-in acts [:head :made]) "holds" (pr-str (get-in acts [:head :records]))
                "records by depot and partition of a store of" (get-in acts [:head :tasks])
                "tasks | they were appended by hand to a fresh store of" tasks "tasks, each as it is, with its own time; the order of the depots and of each depot's partitions:"
                (name order) "| each acked:" (zero? (count not-appended)) "| records in this store by partition | streaming:"
                (pr-str (mapv count (:streaming fed))) "| microbatch:" (pr-str (mapv count (:microbatch fed)))
                "| each layer's records stand in this store's depots in the fixture's order:" in-order?)
         (is (= {:processed? true :answered? true :quiet? true} (dissoc rested :waited-ms)))
         (shown "the store is at rest: its microbatch gate processed each of the" @(:appended-to-base *hand*)
                "things of its depot, each append to the streaming depot waited for the gate's event, and nothing changed in"
                quiet-ms "ms:" (pr-str (dissoc rested :waited-ms)) "| feeding and wait together:" (- (System/currentTimeMillis) began) "ms")
         (shown "the layers that the streaming gate made, by the task that holds them in this store:"
                (pr-str (streaming-layers-by-task held)))
         ;; 1. the tables with no task, row by row
         (doseq [gate gate-order
                 table table-order
                 :let [of-table (filterv (fn [row] (and (= gate (:gate row)) (= table (:table row)))) apart)]]
           (shown "the" (name gate) "gate's table" (name table) "in the form with no task: the fixture holds"
                  (count (get-in expected [gate table])) "rows, this store" (count (get-in here [gate table]))
                  "| rows that differ:" (count of-table) "| by what differs:" (pr-str (kinds-of of-table))))
         (says-different! (assoc sides :part "at") apart)
         (run! (fn [fault] (println "FAULT of the form with no task:" (cut fault))) (take 10 faults))
         (is (zero? (count apart)))
         (is (zero? (count faults)))
         (shown "every row of the eight tables, in the form with no task, the fixture held against this store of" tasks
                "tasks | rows of the fixture:" (reduce + (for [gate gate-order table table-order] (count (get-in expected [gate table]))))
                "| rows that differ:" (count apart) "| a task with another copy, or a key on two tasks:" (count faults))
         ;; 2. the copies on every task
         (says-different! (assoc sides :part "task") copies-apart)
         (is (zero? (count copies-apart)))
         (shown "each of the" tasks "tasks holds exactly the fixture's map of the microbatch gate's facts,"
                (count (get-in expected [:microbatch :facts])) "rows, and of its facts by value,"
                (count (get-in expected [:microbatch :facts-by-value])) "rows | rows on each task | facts:"
                (pr-str (mapv (fn [task] (count (get-in held [:microbatch :facts task]))) (range tasks)))
                "| facts by value:" (pr-str (mapv (fn [task] (count (get-in held [:microbatch :facts-by-value task]))) (range tasks)))
                "| rows compared:" (* tasks (+ (count (get-in expected [:microbatch :facts])) (count (get-in expected [:microbatch :facts-by-value]))))
                "| rows that differ:" (count copies-apart))
         ;; 3. placement, with no task number
         (says-different! (assoc sides :part "layer") (:apart placed))
         (is (zero? (count (:apart placed))))
         (shown "placement with no task number: for each layer of the fixture, a select with the layer as the partitioning key read one task, and that task was held to the fixture's rows of the layer: its answers and its last stamp, and for the streaming gate its facts and its facts by value | layers of the streaming gate:"
                (pr-str (get-in placed [:layers :streaming])) "| of the microbatch gate:" (pr-str (get-in placed [:layers :microbatch]))
                "| rows of the fixture looked for:" (:rows placed) "| rows that differ:" (count (:apart placed)))
         ;; the naming rule
         (says-out-of-their-line! "this store" named)
         (is (zero? (count named)))
         (shown "the naming rule, applied to each partition of both depots of this store, against its answers | acts that the depots hold, strays and repeats left out:"
                (reduce + (for [gate gate-order of-partition (get fed gate)] (count (acts-in-order gate of-partition))))
                "| acts out of their line:" (count named)))))
    (do
      (shown "the fixture is not made: run make-fixture | the check with" tasks "tasks ran nothing")
      (is false "the fixture is not made: run make-fixture"))))

(deftest the-fixtures-records-give-its-tables-with-4-tasks
  (binding [*lead* "check 2, the fixture with 4 tasks"]
    (holds-the-fixture! 4 :as-written)))

(deftest the-fixtures-records-give-its-tables-with-8-tasks
  (binding [*lead* "check 3, the fixture with 8 tasks"]
    (holds-the-fixture! 8 :reversed)))

;; ------------------------------------------------- check 4: after a failure

(defonce ^{:private true
           :doc "What check 4 plans and notes, for the functions that it puts into the walk:

    :plan     the uuid of an act -> the failure that waits for it:
              {:at :sleep-ms :throw?}, with :check or :writes for the place
              of the walk. An entry is taken out when it happens, so each
              happens once, in the first attempt that comes to that place
    :watched  the uuid of an act -> its name on the proof's lines
    :notes    what the walk did with a watched act, in order, each as
              {:uuid :what}: :begins, :sleeps, :wakes, :throws or :decides"}
  failures (atom {:plan {} :watched {} :notes []}))

(defn- uuid-of
  "Takes a thing as it arrived on a depot. Gives the uuid of its act-id, or
  nil for a thing that is no map or holds no act-id of three slots."
  [thing]
  (when (map? thing)
    (let [act-id (get thing :act-id)]
      (when (and (vector? act-id) (= 3 (count act-id)))
        (nth act-id 2)))))

(defn- notes!
  "Takes the uuid of an act and a word for what the walk does with it.
  Keeps the note in `failures` when the act is a watched one."
  [act-uuid what]
  (swap! failures (fn [state]
                    (if (contains? (:watched state) act-uuid)
                      (update state :notes conj {:uuid act-uuid :what what})
                      state))))

(defn- does-what-is-planned!
  "Takes a place of the walk, :check or :writes, and the uuid of the act
  that the walk is deciding. When the plan holds a failure for that act at
  that place, it takes the failure out of the plan, so that it happens
  once, and does it on the calling thread, which is the task's: it sleeps
  `:sleep-ms` when there is one, and then throws when `:throw?` is true. It
  notes each. A second attempt of the act finds no plan and does nothing."
  [place act-uuid]
  (let [[before _] (swap-vals! failures (fn [state]
                                          (if (= place (get-in state [:plan act-uuid :at]))
                                            (update state :plan dissoc act-uuid)
                                            state)))
        {:keys [at sleep-ms throw?]} (get-in before [:plan act-uuid])]
    (when (and (some? at) (= place at))
      (when sleep-ms
        (notes! act-uuid :sleeps)
        (Thread/sleep (long sleep-ms))
        (notes! act-uuid :wakes))
      (when throw?
        (notes! act-uuid :throws)
        (throw (ex-info "thrown on purpose, once, by softland.store.run-again-proof" {:act act-uuid}))))))

(defn- stray-reason-that-notes
  "What check 4 puts into the var of
  `softland.store.door.declaration/stray-reason`, the first call of the
  walk for each thing. It notes that the walk begins with the thing, when
  the thing is a watched act, and gives what the store's own gives."
  [& args]
  (when-let [act-uuid (uuid-of (second args))]
    (notes! act-uuid :begins))
  (apply (:stray-reason stores-own) args))

(defn- check-with-the-plan
  "What check 4 puts into the var of
  `softland.store.door.declaration/check`, which the walk calls early: for
  a thing that is no stray and has no answer, before it reads a row of the
  act's standing. It does what is planned for the act at `:check`
  (`does-what-is-planned!`), and gives what the store's own gives."
  [& args]
  (when-let [act-uuid (uuid-of (first args))]
    (does-what-is-planned! :check act-uuid))
  (apply (:check stores-own) args))

(defn- writes-with-the-plan
  "What check 4 puts into the var of `softland.store.gate.decision/writes`,
  which the walk calls once in each decision, late: behind every read, and
  before the first write. It does what is planned for the act at `:writes`
  (`does-what-is-planned!`), notes that the act is decided, and gives what
  the store's own gives."
  [& args]
  (let [act-uuid (nth (first args) 2 nil)]
    (does-what-is-planned! :writes act-uuid)
    (notes! act-uuid :decides)
    (apply (:writes stores-own) args)))

(defn- with-failures-in-the-walk
  "Puts the proof's three functions into the store's vars, calls a function
  of no argument, and puts the store's own back, whatever the function
  did. Gives what the function gave. The walk reads each var at each call,
  since its namespace was loaded with the store's own function in the var
  (run, src/proposal/backend-on-rama-2026-10-02/run-again/draft-the-bases-gate/checks/):
  so a module that runs calls the proof's functions from the next thing
  on, and the store's own again once they are back. With an empty plan
  and no watched act the proof's functions change nothing of a decision."
  [f]
  (alter-var-root #'declaration/stray-reason (constantly stray-reason-that-notes))
  (alter-var-root #'declaration/check (constantly check-with-the-plan))
  (alter-var-root #'decision/writes (constantly writes-with-the-plan))
  (try
    (f)
    (finally
      (alter-var-root #'declaration/stray-reason (constantly (:stray-reason stores-own)))
      (alter-var-root #'declaration/check (constantly (:check stores-own)))
      (alter-var-root #'decision/writes (constantly (:writes stores-own))))))

(defn- said-by
  "Takes what a call of Rama's client threw. Gives the message of its
  innermost cause, or the name of that cause's class when it has none."
  [^Throwable t]
  (let [^Throwable root (loop [t t] (if-let [cause (.getCause ^Throwable t)] (recur cause) t))]
    (or (.getMessage root) (.getSimpleName (class root)))))

(defn- maker-by-hand!
  "A maker who appends a thing with no door, as it is, with the ack level
  of `gates`, by the client's call that does not wait. So two calls one
  behind the other from one thread go out in that order, the second with
  no wait for the ack of the first. Gives a future of what the maker was
  told: :acked, or [:threw what Rama's client said]. An :ack of the
  streaming depot says that the gate ran; an ack of the microbatch depot
  that the depot holds the thing."
  [gate thing]
  (let [sent (try
               (rama/foreign-append-async! (get-in *hand* [:depots gate]) thing (get-in gates [gate :ack]))
               (catch Throwable t t))]
    (future
      (if (instance? Throwable sent)
        [:threw (said-by sent)]
        (try
          (.get ^CompletableFuture sent 90 TimeUnit/SECONDS)
          :acked
          (catch Throwable t
            [:threw (said-by t)]))))))

(defn- maker-through-the-door!
  "A maker who sends her act through the door on a thread of her own, under
  an act-id that she holds already. Gives a future of what she was told:
  the door's ack word, `:gate-ran` or `:depot-holds`; or [the door's own
  word, what Rama's client said], which for a failed append is
  `:append-cleanly`: the door does not know whether the depot holds the
  thing."
  [act act-id]
  (future
    (let [got (door/send-act (:door *hand*) act act-id)]
      (if (:could-not got)
        [(:could-not got) (:said got)]
        (:ack got)))))

(defn- noted?
  "Takes an act-id and a word of a note. True when the walk has done that
  with the act (`failures`)."
  [act-id what]
  (let [act-uuid (nth act-id 2)]
    (boolean (some (fn [note] (and (= act-uuid (:uuid note)) (= what (:what note)))) (:notes @failures)))))

(defn- acts-with-no-answer
  "Takes the handles of a module that decides the two depots of the store
  of `*hand*`: the store's own, or a second module's. Reads the two depots,
  and for each act that they hold (`acts-in-order`) its answer in that
  module, on the task of its layer. Gives the acts that have none, each
  with its `:gate`. nil when a read threw: a worker that is starting again
  answers no read."
  [reader]
  (try
    (vec (for [gate gate-order
               of-partition (records-of gate)
               {:keys [layer act-id] :as an-act} (acts-in-order gate of-partition)
               :when (nil? (rama/foreign-select-one (path/keypath (record/answer-key layer (nth act-id 2)))
                                                    (get-in reader [:tables gate :answers])
                                                    {:pkey layer}))]
           (assoc an-act :gate gate)))
    (catch Throwable _ nil)))

(def ^:private answers-limit-ms
  "How long check 4 waits for every act of the depots to have its answer in
  one module, in milliseconds."
  120000)

(defn- until-each-act-is-answered!
  "Takes a module's handles. Waits until every act that the two depots of
  the store hold has its answer in that module, `answers-limit-ms` at
  most. Gives true when that came. After a failure this is the wait, and
  not the count of what the hand appended: an append that a failure
  swallowed is in no depot."
  [reader]
  (boolean (until-within answers-limit-ms (fn [] (= [] (acts-with-no-answer reader))))))

(defn- run-again-after-failures-at-rest!
  "Waits until the second module of check 4 has decided every thing of both
  depots and is at rest. Takes its handles and the number of records that
  the microbatch depot holds. Gives
  {:base-decided? :each-act-answered? :quiet? :waited-ms}: whether Rama's
  test tool saw its microbatch gate process that number of things; whether
  every act that the depots hold has its answer in it; whether a quiet
  time passed with no change; and how long it waited. It does not hold the
  second module to the first module's counts, as `run-again-at-rest!`
  does: a first module that a failure left without an answer holds fewer."
  [again records-in-base]
  (let [began (System/currentTimeMillis)
        base-decided? (try
                        (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module again)
                                                                   (get-in gates [:microbatch :topology])
                                                                   records-in-base wait-limit-ms)
                        true
                        (catch Throwable t
                          (println "NOT REACHED the second module's microbatch gate:" (cut (str t)))
                          false))
        answered? (until-each-act-is-answered! again)
        quiet? (boolean (until-true (fn [] (quiet-now? again))))]
    {:base-decided? base-decided? :each-act-answered? answered? :quiet? quiet?
     :waited-ms (- (System/currentTimeMillis) began)}))

(defn- begins-a-failure!
  "Takes what a failure is, in words; its gate and its layer; the names and
  act-ids of its acts, in the order in which they are sent; and the
  failures planned, by act-id. Puts the acts among the watched ones and
  the failures into the plan, and prints a line that marks where the
  failure begins in the output, so that what Rama logs can be told apart.
  Gives the failure's description: {:failure :gate :layer :acts}."
  [says gate layer acts planned]
  (swap! failures (fn [state]
                    (-> state
                        (update :watched into (map (fn [[who act-id]] [(nth act-id 2) who])) acts)
                        (update :plan into (map (fn [[act-id what]] [(nth act-id 2) what])) planned))))
  (println "AFTER A FAILURE begins:" says)
  (flush)
  {:failure says :gate gate :layer layer :acts acts})

(defn- ends-a-failure!
  "Takes a failure's description, whether what was planned was seen to
  happen, and its makers by name, each a future of what she was told.
  Waits until each maker was told, 100 s at most for each, and then until
  every act that the depots hold has its answer. Prints a line that marks
  where the failure ends in the output. Gives the description with
  `:happened?`; with `:told`, name -> what she was told; with
  `:each-act-answered?`; and with `:notes-until`, how many notes the walk
  had made by then: a note of one of the failure's acts behind that
  number is of a later failure, which ran the act's record again."
  [failure happened? makers]
  (let [told (into (sorted-map) (map (fn [[who was-told]] [who (deref was-told 100000 :not-told-within-100-s)])) makers)
        answered? (until-each-act-is-answered! (:first *hand*))]
    (println "AFTER A FAILURE ends:" (:failure failure))
    (flush)
    (assoc failure :happened? (boolean happened?) :told told :each-act-answered? answered?
           :notes-until (count (:notes @failures)))))

(def ^:private holder-ms
  "How long the event of a holder keeps its task's thread, in milliseconds:
  time in which the acts behind it are appended, so that the depot holds
  them before the event of the first of them begins."
  400)

(def ^:private stall-ms
  "How long the stalled event sleeps on its task's thread, in milliseconds:
  longer than Rama's own stream timeout, which the runs of the probe gave
  as 5 s (probes-order-after-a-failure/RESULTS.md, S2). The store sets no
  option of Rama's, and neither does this check."
  8000)

(defn- a-throw-in-a-batch-of-the-base!
  "The failure in the base. Alice's acts E, A and B are appended while the
  store's microbatch gate is paused, so one batch takes the three: E on a
  place of its own, A on one place, and B on that place, naming A's fact as
  the fact that it replaces. In A's decision the walk waits 300 ms, so
  that the other tasks can write the copies of E, and throws, once: the
  attempt of the batch fails with E's rows written and A's not. When the
  throw is noted, C is appended: it names B's fact as the fact that it
  replaces. E, A and C are appended by hand, B through the door. What must
  hold: the base is made and Alice holds her permission in it. Gives what
  `ends-a-failure!` gives."
  []
  (let [{:keys [ipc module]} *hand*
        topology (get-in gates [:microbatch :topology])
        alices (fn [fact] (arrived :microbatch :base :alice alice-in-base [fact]))
        e (with-the-hands-clock (alices {:e :before-the-throw :k :name :v "E"}))
        a (with-the-hands-clock (alices {:e :place :k :name :v "A"}))
        b-id [:base :microbatch (UUID/randomUUID)]
        b (act :base :alice alice-in-base [{:e :place :k :name :v "B" :replaces [(:act-id a) 0]}])
        c (alices {:e :place :k :name :v "C" :replaces [b-id 0]})
        failure (begins-a-failure! "a throw in the base, in the decision of A in the middle of a batch: E, A and B in one batch, B naming A's fact to replace, and C, naming B's, appended after the throw"
                                   :microbatch :base
                                   [["E" (:act-id e)] ["A" (:act-id a)] ["B" b-id] ["C" (:act-id c)]]
                                   {(:act-id a) {:at :writes :sleep-ms 300 :throw? true}})
        _ (rtest/pause-microbatch-topology! ipc module topology)
        makers (try
                 (let [told-e (maker-by-hand! :microbatch e)
                       told-a (maker-by-hand! :microbatch a)
                       _ (deref told-a 30000 nil)
                       told-b (maker-through-the-door! b b-id)
                       _ (deref told-b 30000 nil)]
                   {"E" told-e "A" told-a "B" told-b})
                 (finally
                   (rtest/resume-microbatch-topology! ipc module topology)))
        threw? (until-within 60000 (fn [] (noted? (:act-id a) :throws)))
        told-c (maker-by-hand! :microbatch (with-the-hands-clock c))]
    (ends-a-failure! failure threw? (assoc makers "C" told-c))))

(defn- a-stall-with-the-thread-held!
  "The stall, in a streaming layer of Alice's that is made. Four acts of
  hers. H is a holder: its event keeps the task's thread for `holder-ms`.
  Meanwhile A and Ba are appended by hand, one behind the other from one
  thread with no wait for an ack, so the depot holds both before A's
  event begins. A's event sleeps `stall-ms` on the task's thread, once,
  behind its reads and before its writes. One second into that sleep Bb
  is sent through the door from a thread of its own. A and Ba write one
  place, and neither names a fact to replace: the place holds the fact of
  the one that was decided last. Bb writes it too and names Ba's fact as
  the fact that it replaces. Gives what `ends-a-failure!` gives."
  [layer]
  (let [own [:alice layer layer]
        alices (fn [fact] (arrived :streaming layer :alice own [fact]))
        h (alices {:e :holder :k :text :v "H"})
        a (alices {:e :place :k :text :v "A"})
        ba (alices {:e :place :k :text :v "Ba"})
        bb-id [layer :streaming (UUID/randomUUID)]
        bb (act layer :alice own [{:e :place :k :text :v "Bb" :replaces [(:act-id ba) 0]}])
        failure (begins-a-failure! (str "a stall with the thread held: A's event sleeps " stall-ms
                                        " ms on its task's thread; Ba is in the depot before A's event begins, and Bb is sent 1 s into the sleep; A and Ba write one place, Bb names Ba's fact to replace")
                                   :streaming layer
                                   [["H" (:act-id h)] ["A" (:act-id a)] ["Ba" (:act-id ba)] ["Bb" bb-id]]
                                   {(:act-id h) {:at :check :sleep-ms holder-ms}
                                    (:act-id a) {:at :writes :sleep-ms stall-ms}})
        told-h (maker-by-hand! :streaming (with-the-hands-clock h))
        _ (until-within 30000 (fn [] (noted? (:act-id h) :sleeps)))
        told-a (maker-by-hand! :streaming (with-the-hands-clock a))
        told-ba (maker-by-hand! :streaming (with-the-hands-clock ba))
        asleep? (until-within 30000 (fn [] (noted? (:act-id a) :sleeps)))
        _ (Thread/sleep 1000)
        told-bb (maker-through-the-door! bb bb-id)
        awake? (until-within (+ stall-ms 30000) (fn [] (noted? (:act-id a) :wakes)))]
    (ends-a-failure! failure (and asleep? awake?) {"H" told-h "A" told-a "Ba" told-ba "Bb" told-bb})))

(defn- a-throw-with-the-next-act-right-behind!
  "The first throw, in a streaming layer of Alice's that is made. A and B
  are appended by hand, one behind the other from one thread with no wait
  for an ack. A's event throws, once, behind its reads and before its
  writes. B writes A's place and names A's fact as the fact that it
  replaces. Whether the depot holds B before the worker is started again
  is a race, and the check says how it went. Gives what `ends-a-failure!`
  gives."
  [layer]
  (let [own [:alice layer layer]
        alices (fn [fact] (arrived :streaming layer :alice own [fact]))
        a (alices {:e :place :k :text :v "A"})
        b (alices {:e :place :k :text :v "B" :replaces [(:act-id a) 0]})
        failure (begins-a-failure! "a throw in A's event, with B appended right behind A; B names A's fact to replace"
                                   :streaming layer
                                   [["A" (:act-id a)] ["B" (:act-id b)]]
                                   {(:act-id a) {:at :writes :throw? true}})
        told-a (maker-by-hand! :streaming (with-the-hands-clock a))
        told-b (maker-by-hand! :streaming (with-the-hands-clock b))
        threw? (until-within 30000 (fn [] (noted? (:act-id a) :throws)))]
    (ends-a-failure! failure threw? {"A" told-a "B" told-b})))

(defn- a-throw-with-the-next-act-in-the-depot!
  "The second throw, in a streaming layer of Alice's that is made. H is a
  holder: its event keeps the task's thread for `holder-ms`. Meanwhile A
  and then B are sent through the door, each from a thread of its own, 60
  ms apart, so the depot holds both before A's event begins. A is Alice's
  give of a permission to Carol, and its event throws, once, behind its
  reads and before its writes. B is Carol's first act under that
  permission. Gives what `ends-a-failure!` gives."
  [layer]
  (let [own [:alice layer layer]
        carols [:carol layer layer own]
        h (arrived :streaming layer :alice own [{:e :holder :k :text :v "H"}])
        a-id [layer :streaming (UUID/randomUUID)]
        a (act layer :alice own [{:e :carol :k permission-key :v carols}])
        b-id [layer :streaming (UUID/randomUUID)]
        b (act layer :carol carols [{:e :place :k :text :v "B, Carol's first act"}])
        failure (begins-a-failure! "a throw in A's event, with B in the depot before A's event begins; A is a give to Carol and B her first act under it"
                                   :streaming layer
                                   [["H" (:act-id h)] ["A" a-id] ["B" b-id]]
                                   {(:act-id h) {:at :check :sleep-ms holder-ms}
                                    a-id {:at :writes :throw? true}})
        told-h (maker-by-hand! :streaming (with-the-hands-clock h))
        _ (until-within 30000 (fn [] (noted? (:act-id h) :sleeps)))
        told-a (maker-through-the-door! a a-id)
        _ (Thread/sleep 60)
        told-b (maker-through-the-door! b b-id)
        threw? (until-within 30000 (fn [] (noted? a-id :throws)))]
    (ends-a-failure! failure threw? {"H" told-h "A" told-a "B" told-b})))

(defn- what-a-failure-left
  "Takes a failure's description (`ends-a-failure!`), the records of the
  two depots by gate and partition, what the first module and the run
  again hold (`tables-held`), and the notes of the walk. Gives, with the
  names of the failure's acts:

    :in-the-depot  the acts that the depot holds, in the depot's order,
                   each as [name [partition position]]
    :lost          the acts that the depot does not hold: appends that the
                   failure swallowed
    :walk          what the walk did with the acts until the failure
                   ended, in order, as texts
    :walk-later    what it did with them after that: a later failure that
                   starts a worker again runs earlier records again
    :first :again  the acts that the depot holds in the order of the
                   stamps of their answers in that module, each as
                   [name word], an act with no answer last"
  [{:keys [gate acts notes-until]} records held-first held-again notes]
  (let [place-of (into {}
                       (for [[partition of-partition] (map-indexed vector (get records gate))
                             {:keys [position act-id]} (acts-in-order gate of-partition)]
                         [act-id [partition position]]))
        held (sort-by (fn [[_ act-id]] (place-of act-id)) (filter (fn [[_ act-id]] (place-of act-id)) acts))
        who-of (into {} (map (fn [[who act-id]] [(nth act-id 2) who])) acts)
        in-words (fn [some-notes]
                   (vec (keep (fn [{:keys [uuid what]}] (when-let [who (who-of uuid)] (str who " " (name what)))) some-notes)))
        by-stamp (fn [of-module]
                   (mapv (fn [[who act-id]] [who (word-held of-module gate {:act-id act-id})])
                         (sort-by (fn [[_ act-id]] (or (stamp-held of-module gate {:act-id act-id}) Long/MAX_VALUE)) held)))]
    {:in-the-depot (mapv (fn [[who act-id]] [who (place-of act-id)]) held)
     :lost (mapv first (remove (fn [[_ act-id]] (place-of act-id)) acts))
     :walk (in-words (take notes-until notes))
     :walk-later (in-words (drop notes-until notes))
     :first (by-stamp held-first)
     :again (by-stamp held-again)}))

(defn- makes-the-failures!
  "Makes the failures of check 4 on the store of `*hand*`, one after the
  other, each waited for until every act of the depots has its answer:
  the throw in a batch of the base; the stall, which starts no worker
  again; and the two throws in a streaming layer. Takes three streaming
  layers of Alice's that are made, one for the stall and one for each
  throw. What must hold: the proof's functions are in the walk
  (`with-failures-in-the-walk`), and the base is made with Alice in it.
  Gives what `ends-a-failure!` gave for each, in that order."
  [layers]
  [(a-throw-in-a-batch-of-the-base!)
   (a-stall-with-the-thread-held! (nth layers 0))
   (a-throw-with-the-next-act-right-behind! (nth layers 1))
   (a-throw-with-the-next-act-in-the-depot! (nth layers 2))])

(deftest a-failure-leaves-each-layers-acts-in-their-line
  (binding [*lead* "check 4, after a failure"]
    (reset! failures {:plan {} :watched {} :notes []})
    (on-a-store
     tasks-of-check-1
     (fn []
       (let [{:keys [ipc]} *hand*
             again-name (rama/get-module-name StoreAgain)
             layers [:stalled :thrown-right-behind :thrown-in-the-depot]
             ;; the ground, with no failure: the base, Alice in it, and three streaming layers of hers
             _ (alone! (arrived :microbatch :base :system system-own
                                [{:e :base :k depot-key :v :microbatch} {:e :system :k permission-key :v system-own}]))
             _ (alone! (arrived :microbatch :base :system system-own [{:e :alice :k permission-key :v alice-in-base}]))
             _ (run! (fn [layer]
                       (by-hand! :streaming (arrived :streaming layer :alice [:alice layer layer]
                                                     [{:e layer :k depot-key :v :streaming}
                                                      {:e :alice :k permission-key :v [:alice layer layer]}])))
                     layers)
             ground-not-appended @(:not-appended *hand*)
             ;; the failures, each once, with the proof's functions in the walk
             happened (with-failures-in-the-walk (fn [] (makes-the-failures! layers)))
             stores-own-are-back? (and (identical? declaration/stray-reason (:stray-reason stores-own))
                                       (identical? declaration/check (:check stores-own))
                                       (identical? decision/writes (:writes stores-own)))
             {:keys [plan notes]} @failures
             ;; the first module at rest
             each-answered? (until-each-act-is-answered! (:first *hand*))
             quiet? (boolean (until-true (fn [] (quiet-now? (:first *hand*)))))
             records (into {} (map (fn [gate] [gate (records-of gate)])) gate-order)
             records-in-base (reduce + (map count (:microbatch records)))
             first-before (tables-held (:first *hand*))
             ;; the run again, with the store's own functions and no failure
             _ (rtest/launch-module! ipc StoreAgain {:tasks tasks-of-check-1 :threads 2 :workers 2})
             again (open-reader ipc again-name tasks-of-check-1)
             again-rested (run-again-after-failures-at-rest! again records-in-base)
             first-held (tables-held (:first *hand*))
             again-held (tables-held again)
             apart (vec (mapcat (fn [gate] (rows-apart gate (get first-held gate) (get again-held gate))) gate-order))
             named-first (named-in-a-store first-held records)
             named-again (named-in-a-store again-held records)
             left (not-accounted-for apart named-first records)]
         (is (zero? (count ground-not-appended)))
         (is (every? :happened? happened))
         (is (empty? plan))
         (is (true? stores-own-are-back?))
         (shown "on the store's own module and gates, with no option of Rama's set, the proof put its functions into the vars of three pure functions that the walk calls, made each failure once, in the first attempt of one act, and put the store's own back | each planned failure was seen to happen:"
                (every? :happened? happened) "| failures left in the plan:" (count plan)
                "| the store's own functions are back in their vars:" stores-own-are-back?)
         (doseq [failure happened
                 :let [{:keys [in-the-depot lost walk walk-later] of-first :first of-again :again}
                       (what-a-failure-left failure records first-held again-held notes)]]
           (shown (:failure failure) "| layer" (pr-str (:layer failure)) "| what each maker was told:" (pr-str (:told failure))
                  "| acts that the depot holds, each with [partition position]:" (pr-str in-the-depot)
                  "| appends that the failure swallowed, which are no acts of the depot:" (pr-str lost)
                  "| what the walk did with them until the failure was over, in order:" (pr-str walk)
                  "| and after that, when a later failure ran their records again:" (pr-str walk-later)
                  "| the decisions in the order of their stamps | the first module:" (pr-str of-first)
                  "| the run again:" (pr-str of-again)))
         (is (= {:base-decided? true :each-act-answered? true :quiet? true} (dissoc again-rested :waited-ms)))
         (is (true? (= first-before first-held)))
         (shown "after the failures the first module came to rest: every act that the depots hold has its answer:" each-answered?
                "and nothing changed in" quiet-ms "ms:" quiet? "| records in the depots by partition | streaming:"
                (pr-str (mapv count (:streaming records))) "| microbatch:" (pr-str (mapv count (:microbatch records)))
                "| then the second module," again-name "decided both depots from their beginning, with no failure:"
                (pr-str (dissoc again-rested :waited-ms)) "| the first module's tables are the same before its launch and after:"
                (= first-before first-held))
         ;; the comparison, and the naming rule
         (says-different! {:part "task" :first "first" :second "again"} apart)
         (says-out-of-their-line! "the first module" named-first)
         (says-out-of-their-line! "the run again" named-again)
         (is (zero? (count named-again)))
         (if (zero? (count apart))
           (do
             (is (zero? (count named-first)))
             (shown "every row of every table of both gates on each of the" tasks-of-check-1
                    "tasks, the first module after its failures held against the run again | rows of the first module:"
                    (reduce + (for [gate gate-order table table-order] (rows-in (get-in first-held [gate table]))))
                    "| rows that differ: 0 | acts that the naming rule names | in the first module:" (count named-first)
                    "| in the run again:" (count named-again)))
           (do
             (run! (fn [row] (println "NOT ACCOUNTED FOR by the naming rule:" (cut (select-keys row [:gate :table :part :key :kind])))) (take 20 left))
             (is (zero? (count left)))
             (shown "ROWS DIFFER after a failure: the first module does not hold what the run again holds | rows that differ:" (count apart)
                    "| by gate and table:" (pr-str (into (sorted-map) (frequencies (map (juxt :gate :table) apart))))
                    "| acts that the naming rule names in the first module, each on a line OUT OF ITS LINE:" (count named-first)
                    "| in the run again:" (count named-again)
                    "| rows that differ and are of no named act and of no act behind one in its layer:" (count left)))))))))

(defn test-ns-hook
  "Holds the naming rule on lines made by hand, and then runs the four
  checks in their order, each on a cluster of its own.
  `clojure.test/run-tests` calls it in place of running every test of the
  namespace in an order of its own."
  []
  (the-naming-rule-names-the-acts-out-of-their-line)
  (the-gates-run-again-decide-the-same)
  (the-fixtures-records-give-its-tables-with-4-tasks)
  (the-fixtures-records-give-its-tables-with-8-tasks)
  (a-failure-leaves-each-layers-acts-in-their-line))

;; ------------------------------------------------------- making the fixture

(defn- what-the-fixture-must-hold
  "What the store that the fixture is made of must hold of the hand's
  cases, so that the fixture holds each kind of thing that it is for. Takes
  what `what-the-cases-gave` gave for that store, and the records of the
  two depots by partition, with the hand's cases. Gives a vector of
  [sentence true-or-false], in the order of the hand. The making writes
  nothing while one is false."
  [gave records cases]
  (let [each-gate? (fn [f] (every? f gate-order))
        five (set declaration/stray-reasons)
        two-times? (fn [gate act-id]
                     (let [times (times-under (get records gate) act-id)]
                       (and (= 2 (count times)) (every? some? times) (apply not= times))))]
    [["an act into a layer that is not made is refused :layer-not-made, in each gate"
      (each-gate? (fn [gate] (= :layer-not-made (get-in gave [:before-made gate]))))]
     ["an act for a layer of the other gate is refused :layer-belongs-to-the-other-gate on each depot, before that layer is made and after"
      (and (= 4 (count (:other-gate gave)))
           (every? (fn [word] (= :layer-belongs-to-the-other-gate word)) (vals (:other-gate gave))))]
     ["the base's making act, the give and the first act under it, sent with no wait, are each accepted"
      (= [:accepted :accepted :accepted] (:opening gave))]
     ["the first act under a permission that was given later is accepted, in each gate"
      (each-gate? (fn [gate] (= :accepted (get-in gave [:under-the-give gate]))))]
     ["a thing with no time is refused :no-door-time, and its answer holds no time, in each gate"
      (each-gate? (fn [gate] (let [[word _ in-answer] (get-in gave [:times gate :no-time])]
                               (and (= :no-door-time word) (= :none in-answer)))))]
     ["the act whose time is an hour behind the acts of its layer before it is accepted, and its answer and its fact's row hold its own time, in each gate"
      (each-gate? (fn [gate] (let [[word _ in-answer in-fact] (get-in gave [:times gate :behind])]
                               (and (= :accepted word) (= :its-own in-answer in-fact)))))]
     ["the act that carries the greatest long as a stamp is accepted, and its answer and its fact's row hold its own time, in each gate"
      (each-gate? (fn [gate] (let [[word _ in-answer in-fact] (get-in gave [:times gate :carries])]
                               (and (= :accepted word) (= :its-own in-answer in-fact)))))]
     ["the act whose time is in the year 3084 is accepted, and its answer and its fact's row hold that time, in each gate"
      (each-gate? (fn [gate] (let [[word _ in-answer in-fact] (get-in gave [:times gate :far-ahead])]
                               (and (= :accepted word) (= :its-own in-answer in-fact)))))]
     ["behind the act in the year 3084, an honest act is accepted, and an honest act based on its fact, with the stamp of its answer, is accepted too, in each gate"
      (each-gate? (fn [gate] (= [:accepted :accepted]
                                (mapv (fn [which] (first (get-in gave [:times gate which]))) [:after-the-far-one :names-a-later-fact]))))]
     ["the six things whose time is chosen have the stamps of the first plus 0, 1, 2, 3, 4 and 5, in the order in which they were sent: a thing with no time, a time behind, a carried stamp and a time far ahead each take the layer's next stamp and no other, in each gate"
      (each-gate? (fn [gate] (= [0 1 2 3 4 5] (mapv (fn [which] (second (get-in gave [:times gate which]))) time-cases))))]
     ["the act that was sent twice under one act-id has an answer, and its depot holds two records under that act-id with two times, by the hand and through the door, in each gate"
      (each-gate? (fn [gate]
                    (let [{:keys [by-hand by-door]} (get-in cases [:sent-again gate])]
                      (and (= {:by-hand :accepted :by-door :accepted} (get-in gave [:sent-again gate]))
                           (two-times? gate (:act-id by-hand))
                           (two-times? gate (:act-id by-door))))))]
     ["each act of the chain of acts that each replace the one before is accepted, in each gate"
      (each-gate? (fn [gate] (= {:accepted chain-length} (get-in gave [:chains gate]))))]
     ["the act with each optional part is accepted, in each gate"
      (each-gate? (fn [gate] (= :accepted (get-in gave [:each-part gate]))))]
     ["the acts of rights and claims each get their word, in the order sent, in each gate: a right that lists keys and one beneath it given; a key on the list taken and one off it refused; a give of more refused; :* takes plain keys and no store key; an end from beneath refused; the end by the giver taken, an act under the ended right and one beneath it refused; the right given again, the act beneath still refused, the giver acting; see raised by an end and a give; one-write keys marked, two first claims of one place, one taken and one refused, a retract, a claim after it taken"
      (each-gate? (fn [gate] (= (rights-and-claims-words gate) (get-in gave [:rights gate]))))]
     ["each act into each room is accepted"
      (= {:accepted (* (count rooms) (inc rounds-in-a-room))} (:rooms gave))]
     ["each gate keeps a stray of each of the five words"
      (each-gate? (fn [gate] (= five (set (keys (get-in gave [:strays gate]))))))]
     ["each gate refused with eight words at least"
      (each-gate? (fn [gate] (<= 8 (count (dissoc (get-in gave [:words gate]) :accepted)))))]
     ["two layers that the streaming gate made live on one task"
      (boolean (some (fn [[_ layers]] (<= 2 (count layers))) (:layers-by-task gave)))]
     ["in each layer of each gate the stamps of the answers are 0, 1, 2 and on, one for each answer with no gap, and the layer's last stamp is the greatest of them"
      (each-gate? (fn [gate] (let [{:keys [layers not-counted]} (get-in gave [:counted gate])]
                               (and (pos? layers) (empty? not-counted)))))]
     ["the place that X wrote and then Y holds the fact of Y"
      (= "Y" (:x-and-y gave))]]))

(defn- makes-the-fixture!
  "Makes the fixture. Takes the folder to write into. Launches `Store` with
  `tasks-of-the-fixture` tasks on a cluster of its own, plays the hand with
  `numbers-of-the-fixture`, waits for rest, reads each depot partition's
  records in order and every table on every task, and brings the tables
  into the form with no task. It writes the two files only when all of
  this holds: each send went through and the store came to rest; the
  depots hold as many records as the hand sent, and each thing that the
  hand appended with no door as often as it appended it; the form with no
  task found no fault; no row holds a run of bytes outside an answer's
  digest; the store holds what the fixture is for
  (`what-the-fixture-must-hold`); and the text of each file reads back as
  its value (`not-read-back`). It prints what it held to on lines that
  start with SHOWN, each thing that did not hold on a line that starts
  with NOT HELD, and what it wrote. Gives true when it wrote the files. It
  runs no check of the proof: the checks read the files in a later run."
  [folder]
  (binding [*lead* "making the fixture"]
    (on-a-store
     tasks-of-the-fixture
     (fn []
       (let [numbers numbers-of-the-fixture
             cases (plays-the-hand! numbers)
             rested (first-at-rest!)
             reader (:first *hand*)
             raw (tables-read reader)
             held (bytes-as-hex raw)
             records (into {} (map (fn [gate] [gate (records-of gate)])) gate-order)
             {:keys [tables faults]} (task-free held)
             gave (what-the-cases-gave held cases)
             by-door @(:by-door *hand*)
             by-hand @(:by-hand *hand*)
             not-appended @(:not-appended *hand*)
             in-the-depots (reduce + (for [gate gate-order of-partition (get records gate)] (count of-partition)))
             ;; each thing that the hand appended with no door is in its depot as often as it was appended
             by-hand-held? (every? (fn [gate]
                                     (let [in-depot (frequencies (apply concat (get records gate)))
                                           sent (frequencies (keep (fn [[to thing]] (when (= gate to) [thing])) by-hand))]
                                       (every? (fn [[[thing] n]] (<= n (get in-depot thing 0))) sent)))
                                   gate-order)
             outside (bytes-outside-the-digests raw)
             acts (acts-of tasks-of-the-fixture records)
             file (tables-of tasks-of-the-fixture tables)
             texts {:acts (acts-text acts) :tables (tables-text file)}
             not-back {:acts (not-read-back acts (:acts texts)) :tables (not-read-back file (:tables texts))}
             must (vec (concat
                        [["each act through the door was appended, and each append by the hand was acked"
                          (and (every? :act-id by-door) (zero? (count not-appended)))]
                         ["the store came to rest"
                          (= {:processed? true :answered? true :quiet? true} (dissoc rested :waited-ms))]
                         ["the depots hold as many records as the hand sent"
                          (= in-the-depots (+ (count by-door) (count by-hand)))]
                         ["each thing that the hand appended with no door is in its depot as often as it was appended, equal to ="
                          by-hand-held?]
                         ["every task holds one map of each copied table, and no other key is on two tasks"
                          (zero? (count faults))]
                         ["no row holds a run of bytes but an answer's row under :digest"
                          (zero? (count outside))]]
                        (what-the-fixture-must-hold gave records cases)
                        [["the text of acts.edn reads back as its value, part for part and class for class"
                          (nil? (:acts not-back))]
                         ["the text of tables.edn reads back as its value, part for part and class for class"
                          (nil? (:tables not-back))]]))
             holds? (every? second must)]
         (shown "the hand of the fixture on a store of" tasks-of-the-fixture "tasks |" (count by-door)
                "acts through the door and" (count by-hand) "things by the hand, with no door | in them the load:"
                (:base-fillers numbers) "acts into the base with no wait for the gate, and" (:streaming-fillers numbers)
                "acts into one streaming layer | at rest:" (pr-str (dissoc rested :waited-ms)))
         (doseq [[k says] cases-in-words]
           (shown says "|" (pr-str (get gave k))))
         (doseq [[says ok?] must]
           (shown "held to:" says "|" ok?))
         (doseq [[says ok?] must
                 :when (not ok?)]
           (println "NOT HELD" says))
         (run! (fn [fault] (println "NOT HELD a fault of the form with no task:" (cut fault))) (take 10 faults))
         (run! (fn [at] (println "NOT HELD a run of bytes at:" (cut at))) (take 10 outside))
         (doseq [[which differs] not-back
                 :when differs]
           (println "NOT HELD the text of" (get fixture-files which) "does not read back as its value:" (cut differs)))
         (if holds?
           (let [paths (into {} (map (fn [[which file-name]] [which (io/file folder file-name)])) fixture-files)]
             (doseq [[which path] paths]
               (io/make-parents path)
               (spit path (get texts which)))
             (shown "wrote" (str (:acts paths)) "|" (count (.getBytes ^String (:acts texts) "UTF-8"))
                    "bytes | records by depot and partition:" (pr-str (get-in acts [:head :records])) "| records in all:" in-the-depots)
             (shown "wrote" (str (:tables paths)) "|" (count (.getBytes ^String (:tables texts) "UTF-8"))
                    "bytes | rows by gate and table, in the form with no task:" (pr-str (get-in file [:head :rows]))
                    "| rows in all:" (reduce + (mapcat vals (vals (get-in file [:head :rows])))))
             true)
           (do
             (println "NOT WRITTEN the fixture: something that it is held to did not hold")
             false)))))))

;; ---------------------------------------------------------------- the run

(defn -main
  "With no argument: holds the naming rule on lines made by hand and runs
  the four checks, each on a cluster of its own, prints the SHOWN lines
  again in one place and the count line, and exits with status 0 only when
  all of them hold. With `make-fixture` and, when
  wanted, a folder: makes the fixture into that folder, or into
  `fixture-folder` when none is named, runs no check, and exits with
  status 0 only when the two files were written. With anything else it
  says so and exits with status 2."
  [& args]
  (let [[mode folder] args]
    (cond
      (nil? mode)
      (let [result (select-keys (run-tests 'softland.store.run-again-proof) [:test :pass :fail :error])
            {:keys [fail error]} result]
        (println)
        (println "THE PROOF, IN ONE PLACE")
        (run! println @shown-lines)
        (println result)
        (shutdown-agents)
        (System/exit (if (zero? (+ fail error)) 0 1)))

      (= "make-fixture" mode)
      (let [written? (try
                       (makes-the-fixture! (or folder fixture-folder))
                       (catch Throwable t
                         (println "NOT WRITTEN the fixture: the making threw" (cut (str t)))
                         false))]
        (println)
        (println "THE MAKING OF THE FIXTURE, IN ONE PLACE")
        (run! println @shown-lines)
        (println {:written written?})
        (shutdown-agents)
        (System/exit (if written? 0 1)))

      :else
      (do
        (println "not a mode of this proof:" (pr-str mode) "| run it with no argument, or with: make-fixture [folder]")
        (System/exit 2)))))
