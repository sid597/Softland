;; Before changing this file, read the plans of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-microbatch-gate.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md

(ns softland.store.module.microbatch-gate
  "The microbatch gate: the microbatch topology that decides the things of
  the microbatch depot, and the five PStates of its record. It requires
  Rama.

  What it takes. Each thing that arrives on `*microbatch-depot`, which the
  module declares (`softland.store.module`) and which partitions by the
  entity of the thing's first fact. Anything can arrive. Rama gives the gate
  the things in batches: a batch is what had gathered on the depot when a
  run of the gate began.

  What it changes. It owns five PStates, the record of the microbatch
  layers, and it alone writes them: `$$microbatch-answers`,
  `$$microbatch-facts`, `$$microbatch-facts-by-value`, `$$microbatch-locks`
  and `$$microbatch-task`. Their schemas are `softland.store.record.tables`'s
  and their rows `softland.store.record.rows`'s. They are durable state of
  the cluster. It borrows one PState of the streaming gate to read,
  `$$streaming-facts`, on the meeting task of a layer that starts streaming.
  The gate keeps nothing in the memory of a process: what one step hands to
  the next is in three temporary PStates of Rama, `$$questions`, `$$replies`
  and `$$parcels`, which live for one attempt of one batch; and the row of
  a layer's marked keys is read from the PState in each batch that needs
  it, on every attempt.

  Where a row lives. The facts of a microbatch layer are on the task of
  their entity, so they are on several tasks. An answer is on the task of
  its act-id's uuid, so that a reader finds it from the act-id alone. A lock
  is on the task of its request's uuid. A row of the facts by value is on
  the task of its value digest, so that every place that holds one value
  under one marked key is one run on one task, found from the value alone.
  A fact under one of five store keys is on every task: a layer's depot and
  each permission, so that each act finds the permission of its `:who` with
  one read on the task where it arrives; the layer's marked keys, which the
  home task of each place reads; and who may see the layer and what it
  builds on, which a reader finds on the task where it reads.

  Approach. The dataflow only reads rows, calls pure functions and writes
  rows. The pure code of a batch is `softland.store.gate.batch`'s; the checks are
  `softland.store.door.declaration`'s and `softland.store.gate.decision`'s, the same
  functions that the streaming gate calls. None of them throws, so no thing
  can stop the gate: a refusal is data.

  The one order. The decisions of a batch are those of deciding its acts one
  at a time in the order of their turns. A thing's turn is its position in
  its depot partition's part of the batch, times the number of tasks, plus
  the partition's number. The count of positions is in `$$microbatch-task`,
  which Rama sets back when an attempt fails, so a turn is the same on every
  attempt. One thing is not decided in the order of the turns: a mark of a
  key holds from the batch after. Each act of a batch is decided with the
  layer's marked keys as every task held them when the batch began; so a
  mark and a fact under its key in one batch leave no row in the facts by
  value, whatever their two turns are. The key gets the last stamp of that
  batch, so the order of a marked key holds every fact under that key whose
  stamp is later than the key's stamp, and no other.

  The walk of one batch: four steps, each a batch block of Rama. A step
  starts when the one before is done on every task.

    1. arrive and question   On the depot partition's task, each thing in
       the depot's order. Its turn. A stray gets no answer: its digest is
       added to the set of its reason word, and it ends. Check (b), the
       door's declaration. The standing of the act, from this task's own
       copy of the layer's permissions. Then the act sends a question to the
       home task of each row that it needs: its answer, the places of its
       facts, its locks.
    2. reply   The gate's task reads the clock once, gives the batch its
       base stamp, and writes the gate's last stamp. Every task writes
       `:closed-through`, when it has changed. Each home task first reads,
       from its own copy and once for a layer, the marked keys of each layer
       that a question about a place names. Then it reads each row that was
       asked for, once, and replies; a reply also says whether another act
       of the batch needs the same row. A place under a marked key is read
       for each asker that does not have it as its own, and its reply
       carries the key's stamp. The reply about an answer is check (a): the
       least turn has the act-id; it also carries the last stamp that the
       batch can hold.
    3. decide   On each task, in the order of the turns: an act that meets
       no act of another task is decided here, and its facts on this task
       are written at once, so that a later act of the task reads them. For
       a fact under a marked key the task first reads the act's own place,
       the row before. Each other row, a row of the facts by value among
       them, and each act that meets, is sent as a parcel.
    4. write and meet   Each task writes the rows that it was sent, the
       home's row of each lock before any row with a lock's bytes; a row of
       the facts by value that is nil takes its key out. A new lock's bytes
       are made here, at the write. The meeting task of a layer decides the
       acts that meet, one at a time in the order of their turns, each
       against the rows that earlier acts of the pass wrote, and sends their
       rows: a row under one of the five store keys that every task holds to
       every task, each other row to its home.

  Two acts of one batch on one place under a marked key meet, also when
  each has the place as its own. A row of the facts by value is on a third
  task, and what two tasks or two steps send there has no order; in the pass
  the two are decided in turn, and one row is sent for each key, the last
  (`softland.store.gate.batch`).

  The stamp. One reading of the clock for a batch. An act's stamp is the
  base plus its turn: one stamp on its answer and on each of its facts,
  later than every stamp that the gate gave before on any task. The gate has
  one last stamp, on the gate's task.

  Whole or nothing. An act is decided once, on one task, with every row
  that it needs in hand. A refusal writes its answer and nothing else. The
  rows of an accept go to several tasks inside one attempt, and Rama keeps
  or drops an attempt's writes on all tasks together, a key that was taken
  out too. A fact's row and its row of the facts by value are on two tasks,
  and Rama shows a batch task by task: a reader of the tables can see one
  before the other.

  Closing: what a read may give. Rama makes a batch visible task by task,
  and it can run a batch again after one task has shown its part; a second
  attempt reads the clock again and makes other lock bytes. So each task
  holds `:closed-through`, and a read gives a row only when the row's stamp
  is not later than it (`softland.store.record.rows/closed?`). Step 2 of each run
  writes `:closed-through` with the gate's last stamp as it was before the
  run. That run began only after every task had committed the batch before,
  so a closed batch is visible on every task and cannot run again. The run
  after a batch with things starts at once, also when it has nothing to
  process, so a batch is closed one short run after it. What closing does
  not give: two tasks do not open at one instant; and a select that goes
  straight to a PState of this gate sees rows of a batch that is not
  closed.

  When Rama runs a batch again. Each attempt takes the same things and
  starts from the PStates as they were after the batch before. The turns,
  which act has an act-id, which acts meet, and the order are the same. The
  stamps and a new lock's bytes are others; no read gave the first ones.
  Two words can differ: the 10-second rule reads the clock, and the word for
  a layer of the other gate follows what that gate had made.

  Yielding. Steps 2, 3 and 4 walk a task's keys, acts and rows in loops that
  give the task back when they have run long. What can run in between is a
  read of committed rows, an event of the streaming gate, the gathering of
  the same step, or a write of rows from another meeting task, which are
  rows of other layers. None writes a row that the walk reads.

  What the gate rests on. The depot's partitioner and the gate's hash
  partitioner give one task for one id. If they did not, an act would be
  decided against the true rows all the same, since every row is reached by
  the partitioner; only the read of the other gate's record on the meeting
  task rests on it.

  The source starts from the beginning of the depot."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.batch :as batch]
            [softland.store.gate.decision :as decision]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.record.rows :as record]
            [softland.store.record.tables :as record-pstates]
            [softland.store.door.seal :as seal])
  (:import [com.rpl.rama ModuleInstanceInfo]))

(def +gathered
  "The aggregator of the gate's three temporary PStates: Rama's combiner
  over `softland.store.gate.batch/gather`. A combiner, so that each task puts its
  own values together before it sends them."
  (combiner batch/gather :init-fn (fn [] nil)))

(defn tasks-of
  "Takes Rama's information about the module instance. Gives the module's
  number of tasks, which never changes."
  [info]
  (let [^ModuleInstanceInfo info info]
    (.getNumTasks info)))

(defn new-lock-row
  "Gives the row of a new lock: fresh random bytes and the number of the way
  that new locks are made for. The gate calls it at the write of a new
  lock, on the lock's home task, and the bytes go to the locks PState only;
  they are never on a depot. It also calls it for the row that the check of
  a seal is given for a lock that is not made yet. Each call gives other
  bytes."
  []
  (record/lock-row (seal/new-lock)))

(defn rows-as-map
  "Takes the rows of a range of a PState, as the range navigator gives them.
  Gives them as a plain map from key to row."
  [rows]
  (into {} rows))

(deframafn rows-at
  "Takes a PState of this task whose top level is a map, and keys. Gives a
  map from each key to its row, nil for a key that holds none. One read for
  each key."
  [$$rows *keys]
  (loop<- [*keys-left *keys
           *rows-read {}
           :> *all-rows]
    (<<if (empty? *keys-left)
      (:> *rows-read)
     (else>)
      (first *keys-left :> *one-key)
      (local-select> (keypath *one-key) $$rows :> *one-row)
      (continue> (rest *keys-left) (assoc *rows-read *one-key *one-row))))
  (:> *all-rows))

(deframaop lock-rows-in
  "Takes the locks PState of this task, and ranges of lock keys, each as
  [first key, last key], both in the range. Emits once: the rows of those
  ranges as one map from key to row. One read for each range, and one step
  for each lock. A range holds up to 256 locks, so the read may give the
  task back while it walks; it then goes on over the same snapshot. It is
  an operation and not a function of Rama because it may give the task
  back."
  [$$locks *ranges]
  (loop<- [*ranges-left *ranges
           *locks-read {}
           :> *all-locks]
    (<<if (empty? *ranges-left)
      (:> *locks-read)
     (else>)
      (first *ranges-left :> [*first-key *last-key])
      (local-select> [(sorted-map-range *first-key *last-key {:inclusive-end? true})
                      (view rows-as-map)]
                     $$locks
                     {:allow-yield? true}
                     :> *range-rows)
      (continue> (rest *ranges-left) (merge *locks-read *range-rows))))
  (:> *all-locks))

(deframafn write-row
  "Writes one row of a decision on this task: a whole row at a fixed key, or
  a fixed key taken out; no read. Takes the answers, the facts, the facts by
  value and the locks PState of the gate, and [which key row] as
  `softland.store.gate.batch` gives it. A row of the kind :by-value that is
  nil takes its key out of the facts by value; taking out a key that holds
  no row changes nothing. For :new-lock it makes the lock here, at the
  write."
  [$$answers $$facts $$facts-by-value $$locks *which *key *row]
  (<<cond
    (case> (= :fact *which))
    (local-transform> [(keypath *key) (termval *row)] $$facts)

    (case> (and> (= :by-value *which) (nil? *row)))
    (local-transform> [(keypath *key) NONE>] $$facts-by-value)

    (case> (= :by-value *which))
    (local-transform> [(keypath *key) (termval *row)] $$facts-by-value)

    (case> (or> (= :lock *which) (= :lock-home *which)))
    (local-transform> [(keypath *key) (termval *row)] $$locks)

    (case> (= :new-lock *which))
    (new-lock-row :> *new-lock)
    (local-transform> [(keypath *key) (termval *new-lock)] $$locks)

    (case> (= :answer *which))
    (local-transform> [(keypath *key) (termval *row)] $$answers)

    (default>)
    (identity nil :> *no-row))
  (:> nil))

(defn add-to
  "Adds the microbatch gate to a module. Takes the module's `topologies`.
  Declares the microbatch topology `microbatch-gate`, its five PStates, and
  the dataflow of one batch, as the namespace docstring walks it. What must
  hold: the module declares the depot `*microbatch-depot`, and has the
  streaming gate, whose `$$streaming-facts` step 4 reads."
  [topologies]
  (let [gate (microbatch-topology topologies "microbatch-gate")]
    (declare-pstate gate $$microbatch-answers (record-pstates/answers))
    (declare-pstate gate $$microbatch-facts (record-pstates/facts))
    (declare-pstate gate $$microbatch-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$microbatch-locks (record-pstates/locks))
    (declare-pstate gate $$microbatch-task (record-pstates/microbatch-task))
    (<<sources gate
      (source> *microbatch-depot {:start-from :beginning} :> %things)

      ;; 1. Arrive and question: each thing, on its depot partition's task.
      (<<batch
        (%things :> *thing)
        (ops/current-microbatch-id :> *batch-id)
        (str *batch-id :> *batch-text)
        (ops/current-task-id :> *partition)
        (ops/module-instance-info :> *info)
        (tasks-of *info :> *tasks)
        (local-select> (keypath :turns) $$microbatch-task :> *turns)
        (batch/position *turns *batch-text :> *position)
        (batch/turns-after *batch-text *position :> *turns-now)
        (local-transform> [(keypath :turns) (termval *turns-now)] $$microbatch-task)
        (batch/turn *position *tasks *partition :> *turn)
        (declaration/stray-reason :microbatch *thing :> *stray)
        (<<if *stray
          ;; A stray gets no answer: its digest goes to the set of its word.
          (digest/hex-of *thing :> *stray-digest)
          (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)]
                            $$microbatch-task)
          (identity [] :> *messages)
         (else>)
          ;; Check (b), and the standing from this task's copy of the permissions.
          (declaration/check *thing :> {*act :ok *declarations-reason :refuse})
          (<<if *declarations-reason
            (identity nil :> *permission-row)
            (identity nil :> *depot-row)
           (else>)
            (decision/permission-place *act :> *permission-key)
            (local-select> (keypath *permission-key) $$microbatch-facts :> *permission-row)
            (<<if (nil? *permission-row)
              (decision/depot-place *act :> *depot-key)
              (local-select> (keypath *depot-key) $$microbatch-facts :> *depot-row)
             (else>)
              (identity nil :> *depot-row)))
          (batch/arrive *thing *act *declarations-reason *turn *position *permission-row *depot-row
                        :> *messages))
        (ops/explode *messages :> [*route *kind *asked-key *payload])
        (|hash *route)
        (+compound {*kind {*asked-key (+gathered *payload)}} :> $$questions))

      ;; 2. Reply: the base on the gate's task, the closing, then each home task.
      (<<batch
        (identity batch/gates-depot :> *gates-route)
        (|hash *gates-route)
        (local-select> (keypath :last-stamp) $$microbatch-task :> *last-stamp)
        (local-select> (keypath :closed-through) $$microbatch-task :> *closed-through)
        (local-select> (keypath :numbers :batch) $$questions :> *numbers)
        (System/currentTimeMillis :> *now)
        (batch/base *now *last-stamp *numbers :> *base)
        (<<if *base
          (ops/module-instance-info :> *info-2)
          (batch/last-stamp-of-batch *base *numbers (tasks-of *info-2) :> *new-last-stamp)
          (local-transform> [(keypath :last-stamp) (termval *new-last-stamp)] $$microbatch-task))
        (batch/closes? *last-stamp *closed-through :> *closes)
        (|all)
        (<<if *closes
          (local-transform> [(keypath :closed-through) (termval *last-stamp)] $$microbatch-task))
        (local-select> STAY $$questions :> *asked)
        ;; The marked keys of each layer that a question about a place names,
        ;; from this task's own copy, once for a layer; and the last stamp
        ;; that the batch can hold. Neither reads a clock.
        (batch/marks-places-asked *asked :> *marks-keys)
        (rows-at $$microbatch-facts *marks-keys :> *marks-rows)
        (batch/reads-of *asked *marks-rows :> *reads)
        (ops/module-instance-info :> *info-here)
        (batch/batch-end *base *numbers (tasks-of *info-here) :> *batch-end)
        (loop<- [*reads-left *reads
                 *replies-so-far []
                 :> *replies]
          (yield-if-overtime)
          (<<if (empty? *reads-left)
            (:> *replies-so-far)
           (else>)
            (first *reads-left :> *read)
            (get *read :about :> *about)
            (get *read :key :> *read-key)
            (<<cond
              (case> (= :answer *about))
              (local-select> (keypath *read-key) $$microbatch-answers :> *found)

              (case> (and> (= :place *about) (get *read :read?)))
              (local-select> (keypath *read-key) $$microbatch-facts :> *found)

              (case> (= :locks *about))
              (get *read :ranges :> *ranges)
              (lock-rows-in $$microbatch-locks *ranges :> *found)

              (default>)
              (identity nil :> *found))
            (batch/replies *read *found *now *base *batch-end :> *more-replies)
            (continue> (rest *reads-left) (into *replies-so-far *more-replies))))
        (ops/explode *replies :> [*reply-route *reply-turn *reply])
        (|hash *reply-route)
        (+compound {*reply-turn (+gathered *reply)} :> $$replies))

      ;; 3. Decide: on each task, its acts in the order of their turns.
      (<<batch
        (|all)
        (local-select> (keypath :entry) $$questions :> *entries)
        (local-select> STAY $$replies :> *replies-here)
        (batch/items-of-task *entries *replies-here :> *items)
        (loop<- [*items-left *items
                 *parcels-so-far []
                 :> *parcels]
          (yield-if-overtime)
          (<<if (empty? *items-left)
            (:> *parcels-so-far)
           (else>)
            (first *items-left :> *item)
            (batch/own-places-wanted *item :> *own-keys)
            (rows-at $$microbatch-facts *own-keys :> *own-rows)
            (batch/decide-or-send *item *own-rows :> {*rows-now :now *item-parcels :parcels})
            (<<atomic
              (ops/explode *rows-now :> [*now-key *now-row])
              (local-transform> [(keypath *now-key) (termval *now-row)] $$microbatch-facts))
            (continue> (rest *items-left) (into *parcels-so-far *item-parcels))))
        (ops/explode *parcels :> [*parcel-route *parcel-kind *parcel])
        (|hash *parcel-route)
        (+compound {*parcel-kind (+gathered *parcel)} :> $$parcels))

      ;; 4. Write and meet: the rows sent here, then the acts that meet here.
      (<<batch
        (|all)
        (local-select> STAY $$parcels :> *sent-here)
        (batch/rows-in-write-order *sent-here :> *rows-here)
        (loop<- [*rows-left *rows-here
                 :> *rows-written]
          (yield-if-overtime)
          (<<if (empty? *rows-left)
            (:> true)
           (else>)
            (first *rows-left :> [*sent-which *sent-key *sent-row])
            (write-row $$microbatch-answers $$microbatch-facts $$microbatch-facts-by-value
                       $$microbatch-locks
                       *sent-which *sent-key *sent-row
                       :> *sent-written)
            (continue> (rest *rows-left))))
        (batch/acts-that-meet *sent-here :> *meeting)
        (batch/new-pass :> *empty-pass)
        (loop<- [*meeting-left *meeting
                 *pass *empty-pass
                 :> *pass-done]
          (yield-if-overtime)
          (<<if (empty? *meeting-left)
            (:> *pass)
           (else>)
            (first *meeting-left :> *met)
            (batch/meeting-reads *pass *met :> *local-keys)
            (rows-at $$microbatch-facts *local-keys :> *local-rows)
            ;; Rule 7: a layer of which this record holds nothing, and that
            ;; starts with the other depot, is asked of the other gate's record.
            (<<if (batch/reads-other-record? *pass *met)
              (batch/depot-place-of *met :> *other-depot-key)
              (local-select> (keypath *other-depot-key) $$streaming-facts :> *other-depot-row)
             (else>)
              (identity nil :> *other-depot-row))
            (new-lock-row :> *drawn-lock-row)
            (batch/decide-next *pass *met *local-rows *other-depot-row *drawn-lock-row :> *pass-after)
            (continue> (rest *meeting-left) *pass-after)))
        (batch/rows-to-send *pass-done :> *rows-out)
        (ops/explode *rows-out :> {:keys [*every-task? *home *which *key *row]})
        (<<if *every-task?
          (|all)
         (else>)
          (|hash *home))
        (write-row $$microbatch-answers $$microbatch-facts $$microbatch-facts-by-value
                   $$microbatch-locks
                   *which *key *row
                   :> *written)))))
