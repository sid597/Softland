;; Before changing this file, read the plans of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-microbatch-gate.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/before-real-data/PLAN-answers-in-a-log.md

(ns softland.store.module.microbatch-gate
  "The microbatch gate: the microbatch topology that decides the things of
  the microbatch depot, the four PStates of its record, and its depot of
  answers. It requires Rama.

  What it takes. Each thing that arrives on `*microbatch-depot`, which the
  module declares (`softland.store.module`) and which partitions by the
  entity of the thing's first fact. Anything can arrive. Rama gives the gate
  the things in batches: a batch is what had gathered on the depot when a
  run of the gate began.

  What it changes. It owns four PStates, the record of the microbatch
  layers, and it alone writes them: `$$microbatch-answers`,
  `$$microbatch-facts`, `$$microbatch-facts-by-value`
  and `$$microbatch-task`. Their schemas are `softland.store.record.tables`'s
  and their rows `softland.store.record.rows`'s. They are durable state of
  the cluster. It borrows one PState of the streaming gate to read,
  `$$streaming-facts`, on the meeting task of a layer that starts streaming.
  And for the clock of a batch it appends one record to the depot
  `*batch-clock-depot`, which the module declares, and borrows the PState
  `$$batch-clock` of the topology that keeps that clock
  (`softland.store.module.batch-clock`), to read on the gate's task.
  It declares one depot, `*microbatch-answer-depot`, its depot of answers,
  and it alone appends to it: no client can, since the depot is declared
  :disallow, and no topology has a source on it. A record there holds what
  a batch decided: an entry for each answer and for each arrival of a
  stray, and with a batch's first record the batch's clock and its last
  stamp (`softland.store.gate.batch/records-of-closing`). No value of a
  fact is in it. The module sets no trimming option, so the depot keeps
  every record. Two fields of `$$microbatch-task` serve the depot:
  `:to-keep`, on the keeping task of a batch, and `:to-close`, on the gate's
  task.
  The gate keeps nothing in the memory of a process: what one step hands to
  the next is in three temporary PStates of Rama, `$$questions`, `$$replies`
  and `$$parcels`, which live for one attempt of one batch; and the row of
  a layer's marked keys is read from the PState in each batch that needs
  it, on every attempt.

  Where a row lives. The facts of a microbatch layer are on the task of
  their entity, so they are on several tasks. An answer is on the task of
  its act-id's uuid, so that a reader finds it from the act-id alone. A row
  of the facts by value is on
  the task of its value digest, so that every place that holds one value
  under one marked key is one run on one task, found from the value alone.
  A fact under one of five store keys is on every task: a layer's depot and
  each permission, so that each act finds the permission of its `:who` with
  one read on the task where it arrives; the layer's marked keys, which the
  home task of each place reads; and who may see the layer and what it
  builds on, which a reader finds on the task where it reads.

  Approach. The dataflow reads rows, calls pure functions and writes rows,
  and appends in two places. For a batch's clock: when none is kept for the
  batch it reads the clock, appends one record to the depot of batch clocks,
  and waits for the ack. For the keeping: the run that closes a batch
  appends what that batch left to keep to the depot of answers, and waits
  for each append. The pure code of a batch is
  `softland.store.gate.batch`'s; the checks are
  `softland.store.door.declaration`'s and `softland.store.gate.decision`'s, the same
  functions that the streaming gate calls. None of them throws, so no thing
  that arrives can stop the gate: a refusal is data. One function of this
  namespace throws, `clock-must-be-kept`, and not on what arrived: it fails
  an attempt whose clock cannot be read back after the append was acked, so
  that no stamp is made from a clock that is not kept. Its docstring says
  what that costs.

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
       added to the set of its reason word, its entry is sent to the
       batch's keeping task, a word that the batch has something to keep
       is sent to the gate's task, and it ends. Check (b), the
       door's declaration, whose last step makes the one text of each value
       and holds the bound on one value and the bound on the act: a thing
       that it refuses reads no permission and asks for its answer alone.
       The standing of the act, from this task's own
       copy of the layer's permissions. Then the act sends a question to the
       home task of each row that it needs: its answer, and the places of
       its facts.
    2. reply   The gate's task takes the batch's clock, gives the batch its
       base stamp, and writes the gate's last stamp. The clock, only for a
       batch with things to decide: the row of `$$batch-clock` when it is
       of this batch; else the task reads the clock, appends the batch's id
       and that reading to `*batch-clock-depot` with the ack level :ack,
       reads the row again, and takes what the row holds, never what it
       offered. The gate's task then writes `:to-close`, what this batch
       leaves for the next run to close: the batch's id, with its clock
       when it made a stamp; for a batch with nothing to keep it takes the
       key out, when the key was there. Then the closing, only when the
       gate's task read a `:to-close`: on the keeping task of the batch
       before, one range read of that batch's keys of `:to-keep`; one
       append to `*microbatch-answer-depot` for each 256 entries, with the
       ack level :append-ack, one after the other; and each of those keys
       taken out. Every task then writes `:closed-through`, when the batch
       that is closed made a stamp. Each home task first reads,
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
    4. write and meet   Each task writes the rows that it was sent, as they
       were sent: no order is needed between them, since each key of a
       batch has one sender, or all its senders are in one pass. A row of
       the facts by value that is nil takes its key out. The meeting task of a layer decides the
       acts that meet, one at a time in the order of their turns, each
       against the rows that earlier acts of the pass wrote, and sends their
       rows: a row under one of the five store keys that every task holds to
       every task, each other row to its home. With them goes what is to
       keep of this batch: the answers among the rows that the task wrote
       from its parcels and among the rows of its pass, and the strays'
       entries that step 1 sent to it, 256 to a value at most, each value
       to the batch's keeping task, which puts it into `:to-keep` under a
       key of this batch and of the sending task.

  Two acts of one batch on one place under a marked key meet, also when
  each has the place as its own. A row of the facts by value is on a third
  task, and what two tasks or two steps send there has no order; in the pass
  the two are decided in turn, and one row is sent for each key, the last
  (`softland.store.gate.batch`).

  The stamp. One clock for a batch, kept, the same on every attempt. An
  act's stamp is the
  base plus its turn: one stamp on its answer and on each of its facts,
  later than every stamp that the gate gave before on any task. The gate has
  one last stamp, on the gate's task.

  The clock of a batch. A batch with things to decide has one clock, and
  every attempt of the batch takes it from the row of `$$batch-clock`, on
  the gate's task, when that row is of the batch
  (`softland.store.gate.batch/kept-clock`). The first attempt finds no such
  row. It reads the clock of the task, the one reading; appends
  {:batch :clock} to the partition of `*batch-clock-depot` on this task; and
  waits, by the ack level :ack, until the topology `batch-clock` has
  processed the record and its row is durable. Then it reads the row again
  and uses the clock that the row holds. That is not always the reading
  that it offered: when an earlier attempt of the batch appended a reading
  and failed before its ack, the row can hold that reading, and first write
  wins. So for one batch every attempt that makes a stamp makes it from one
  clock. The clock is kept before `softland.store.gate.batch/base` makes the
  first stamp from it, and so before any task can show a row of the batch.
  A batch with no numbers, one that took no thing or only strays, reads no
  row and no clock and appends nothing: it has no stamp to make. No later
  step reads a clock: the base and the reply about each act's answer carry
  the batch's clock, and the 10-second rule is held against it. What a
  batch with things pays for it, on the gate's task, in the attempt that
  keeps the clock: three reads, one record and one row written, and the
  wait for the ack; a later attempt pays one read.

  Whole or nothing. An act is decided once, on one task, with every row
  that it needs in hand. A refusal writes its answer and nothing else. The
  rows of an accept go to several tasks inside one attempt, and Rama keeps
  or drops an attempt's writes on all tasks together, a key that was taken
  out too. A fact's row and its row of the facts by value are on two tasks,
  and Rama shows a batch task by task: a reader of the tables can see one
  before the other.

  Closing: what the door's reads may give. Rama makes a batch visible task
  by task, and it can run a batch again after one task has shown its part.
  A second attempt can give an act into a layer
  of the other gate the other of two words, so a reader could see a word
  that later changes. So each task
  holds `:closed-through`, and the door's two reads and the streaming
  gate's question about the base give a row only when the row's stamp
  is not later than it (`softland.store.record.rows/closed?`). The run after
  a batch closes it. Step 2 knows that it closes a batch by `:to-close`,
  which that batch wrote on the gate's task. It appends what the batch left
  to keep, and then every task writes `:closed-through` with the gate's
  last stamp as it was before the run, when the batch made a stamp. That
  run began only after every task had committed the batch before, so a
  closed batch is visible on every task and cannot run again. And
  `:closed-through` is written by the attempt that made the appends, and
  commits only when that attempt does: an answer of the base that the door
  gives is in the depot of answers. The run
  after a batch with things starts at once, also when it has nothing to
  process, so a batch is closed one short run after it. What closing does
  not give: two tasks do not open at one instant; and a select that goes
  straight to a PState of this gate sees rows of a batch that is not
  closed. A fact's row that such a select sees is the row that stays, by
  the paragraph on a batch that runs again.

  The keeping: what the depot of answers holds, and when. No attempt
  appends what it decided itself. An append from inside an attempt stays
  when the attempt fails, and the depot would then hold an answer that the
  tables never held. So step 4 puts each entry into `:to-keep` in the
  attempt that writes its answer's row. `:to-keep` is a table of this
  topology, and Rama keeps or drops an attempt's writes on all tasks
  together: an entry and its row are committed together or not at all. The
  next run begins only when that batch is committed on every task; its
  closing reads the entries from committed state, appends them, and takes
  them out. One task keeps a batch, the task that the hash of the batch's
  id gives: the closing reads on one task, and the records of the batches
  do not gather on one partition. The partition of a record says nothing of
  the acts in it. A batch of strays alone makes no stamp; it writes
  `:to-close` all the same, because each stray told the gate's task that
  the batch has something to keep, so a stray never waits for a later batch
  with a stamp. A batch whose things were all repeats made a stamp and
  wrote no answer: its closing appends one record, the batch's clock and
  last stamp. A run that closes nothing reads its two fields on the gate's
  task, as it did before the keeping, and appends nothing.

  What a failure leaves in the depot. An attempt of a batch that fails
  leaves nothing of that batch. An attempt of the run that closes a batch,
  when it fails after its appends, has left the batch's records, and the
  next attempt appends them again; each entry under one act-id is then a
  copy of the others, made from the same committed rows. A module that is
  stopped after a batch commits and before the next run holds answers with
  no entry until it runs again: both fields are durable, and the first run
  after the start closes the batch. How far that reaches: that Rama sets a
  topology's own table back with a failed attempt, and that an append from
  inside an attempt stays, were run on stand-ins on Rama 1.6.0 in-process,
  with a throw in the module's code. A failure of the commit itself, an
  append whose ack does not come, and a killed process were not run.

  When Rama runs a batch again. Each attempt takes the same things and
  starts from the PStates as they were after the batch before. The turns,
  which act has an act-id, which acts meet, and the order are the same. So
  is the batch's clock, and with it each stamp, the verdict of the 10-second
  rule, and each field of each fact's row and of each row of the facts by
  value. That holds while one version of the module's code runs every
  attempt of the batch: after a module update that changes the code from
  which a row is made, a second attempt makes its rows with the new code.
  One thing can be another on a second attempt, a word: the
  word for a layer of the other gate follows what that gate had made by
  then; the act is refused on each attempt, with the same stamp, and writes
  no fact. Every other row of a second attempt is the row of the first, in
  every field; so are its values of `:to-keep`, under the same keys, and
  its `:to-close`. The depot of answers is not set back: a second attempt
  of a run that closes a batch appends that batch's records once more.

  Yielding. Steps 2, 3 and 4 walk a task's keys, acts and rows in loops that
  give the task back when they have run long. What can run in between is a
  read of committed rows, an event of the streaming gate, the gathering of
  the same step, or a write of rows from another meeting task, which are
  rows of other layers. None writes a row that the walk reads. The append of
  a batch's clock also gives the gate's task back, while it waits for the
  ack. What can run on that task meanwhile: a read of committed rows, an
  event of the streaming gate, and the event of `batch-clock`, which must.
  No event of this batch can: step 1 is done on every task, and step 2 has
  not yet gone to every task. The values that step 2 read before the
  wait are not written in between: `:last-stamp` and `:to-close` of
  the gate's task are written by this step, after the wait, and
  `$$questions` is whole. The closing gives the keeping task back too: at
  each of its appends, while it waits for the ack, and in its range read
  and its loop over the keys when they run long. What can run there
  meanwhile: a read of committed rows, an event of the streaming gate, an
  event of `batch-clock`. No event of this batch reads or writes `:to-keep`
  in step 2: step 1 writes nothing there, and step 4 has not begun.

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
            [softland.store.record.tables :as record-pstates])
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

(deframafn write-row
  "Writes one row of a batch on this task: a whole row at a fixed key, or
  a fixed key taken out; no read. Takes the answers, the facts and the
  facts by value PState of the gate and its PState for a task, and
  [which key row] as `softland.store.gate.batch` gives it, of one of four
  kinds: :fact, :by-value, :answer or :to-keep. A row of the kind :by-value
  that is nil takes its key out of the facts by value; taking out a key
  that holds no row changes nothing. A row of the kind :to-keep is a value
  of what the batch has to keep, put under its key of `:to-keep`
  (`softland.store.gate.batch/rows-to-keep`). A row of any other kind
  writes nothing. Each write is the same however often it runs. What must
  hold: the task is the row's home, which for a row to keep is the batch's
  keeping task."
  [$$answers $$facts $$facts-by-value $$task *which *key *row]
  (<<cond
    (case> (= :fact *which))
    (local-transform> [(keypath *key) (termval *row)] $$facts)

    (case> (and> (= :by-value *which) (nil? *row)))
    (local-transform> [(keypath *key) NONE>] $$facts-by-value)

    (case> (= :by-value *which))
    (local-transform> [(keypath *key) (termval *row)] $$facts-by-value)

    (case> (= :answer *which))
    (local-transform> [(keypath *key) (termval *row)] $$answers)

    (case> (= :to-keep *which))
    (local-transform> [(keypath :to-keep *key) (termval *row)] $$task)

    (default>)
    (identity nil :> *no-row))
  (:> nil))

(defn clock-must-be-kept
  "The one throw of the module's own code. Takes what
  `softland.store.gate.batch/kept-clock` gave for the row that step 2 read
  after its append of the batch's clock was acked, and the microbatch id of
  the run. Gives that clock. Throws when there is none: the row is then not
  of this batch, although the topology that keeps the clock has processed
  this attempt's record.

  What it guards. A stamp from a clock that is not kept is the one thing
  that the gate must not make: a second attempt of the batch would read
  another clock, and a fact's row that a task had shown would change. The
  throw fails the attempt before `softland.store.gate.batch/base` makes the
  first stamp, and Rama runs the batch again. It is not a throw on what
  arrived: no thing on the depot can reach it.

  What it costs. On Rama's in-process cluster a throw on a task's thread
  stops the worker, and Rama starts the worker again. Every task of that
  worker stops for that time, the streaming gate's and the reads' too.
  Whether a worker of a real cluster ends its process there was not run.
  And while the cause stays, the batch throws on each attempt and Rama runs
  it again for ever: the base decides nothing, the worker goes down and up,
  and the depot of batch clocks gains one record at each attempt.

  When it is reached. In no case that was traced without a fault of the
  cluster or of this module. Two causes are known, and neither was seen in a
  run: an id of a batch that comes again, when the microbatch topology is
  deleted and declared again under its name while `$$batch-clock` stays, so
  that the row is of a later batch for every new batch; and an ack that
  returns although the row was not written. The other choices here are to
  go on with the clock that was only read, which is the fault that the kept
  clock closes, or to skip the batch, which loses its things."
  [kept batch-id]
  (when (nil? kept)
    (throw (ex-info "the clock of a batch is not kept after its append was acked; no stamp is made from a clock that is not kept"
                    {:batch batch-id})))
  kept)

(defn add-to
  "Adds the microbatch gate to a module. Takes the module's `setup` and
  `topologies`. Declares the depot `*microbatch-answer-depot`, closed to a
  client's append and with no trimming option; the microbatch topology
  `microbatch-gate`; its four PStates; and
  the dataflow of one batch, as the namespace docstring walks it. Each read
  and each write of `:to-close` is on the gate's task; the closing's read,
  its appends and its removals are on the keeping task of the batch that is
  closed; and a value to keep is put on the keeping task of its own batch.
  What must
  hold: the module declares the depots `*microbatch-depot` and
  `*batch-clock-depot`; it has the streaming gate, whose `$$streaming-facts`
  step 4 reads; and it has the topology `batch-clock`
  (`softland.store.module.batch-clock`), whose `$$batch-clock` step 2 reads
  and which answers the ack of step 2's append. Without that topology
  nothing would write the row, and each batch with things would fail at
  `clock-must-be-kept`; that is read from what the ack level waits for, and
  was not run."
  [setup topologies]
  (declare-depot setup *microbatch-answer-depot :disallow)
  (let [gate (microbatch-topology topologies "microbatch-gate")]
    (declare-pstate gate $$microbatch-answers (record-pstates/answers))
    (declare-pstate gate $$microbatch-facts (record-pstates/facts))
    (declare-pstate gate $$microbatch-facts-by-value (record-pstates/facts-by-value))
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
          ;; Its entry goes to the batch's keeping task, and a word to the
          ;; gate's task that the batch has something to keep.
          (digest/hex-of *thing :> *stray-digest)
          (local-transform> [(keypath :strays *stray) NONE-ELEM (termval *stray-digest)]
                            $$microbatch-task)
          (batch/stray-sends *batch-id *stray *stray-digest *partition :> *messages)
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

      ;; 2. Reply: the batch's clock and the base on the gate's task, the
      ;; closing of the batch before on its keeping task, then each home task.
      (<<batch
        (identity batch/gates-depot :> *gates-route)
        (|hash *gates-route)
        (ops/current-microbatch-id :> *this-batch)
        (local-select> (keypath :last-stamp) $$microbatch-task :> *last-stamp)
        (local-select> (keypath :to-close) $$microbatch-task :> *to-close)
        (local-select> (keypath :numbers :batch) $$questions :> *numbers)
        (local-select> (keypath :stray-sent :batch) $$questions :> *stray-sent)
        ;; The batch's clock, only for a batch with numbers: the clock that
        ;; is kept for this batch; or, when none is, a reading that is kept
        ;; first. The clock is taken out of the row, never from the reading.
        (<<if *numbers
          (local-select> (keypath *gates-route) $$batch-clock :> *clock-row)
          (batch/kept-clock *clock-row *this-batch :> *clock-found)
          (<<if *clock-found
            (identity *clock-found :> *batch-clock)
           (else>)
            (System/currentTimeMillis :> *reading)
            (batch/clock-record *this-batch *reading :> *clock-record)
            (depot-partition-append! *batch-clock-depot *clock-record :ack)
            (local-select> (keypath *gates-route) $$batch-clock :> *clock-row-after)
            (batch/kept-clock *clock-row-after *this-batch :> *clock-after)
            (clock-must-be-kept *clock-after *this-batch :> *batch-clock))
         (else>)
          (identity nil :> *batch-clock))
        (batch/base *batch-clock *last-stamp *numbers :> *base)
        (<<if *base
          (ops/module-instance-info :> *info-2)
          (batch/last-stamp-of-batch *base *numbers (tasks-of *info-2) :> *new-last-stamp)
          (local-transform> [(keypath :last-stamp) (termval *new-last-stamp)] $$microbatch-task))
        ;; What this batch leaves for the next run to close: its id, and its
        ;; clock when it made a stamp. A batch with nothing to keep takes the
        ;; key out when it was there, and else writes nothing.
        (batch/to-close-of *this-batch *batch-clock *stray-sent :> *to-close-now)
        (<<cond
          (case> (some? *to-close-now))
          (local-transform> [(keypath :to-close) (termval *to-close-now)] $$microbatch-task)

          (case> (some? *to-close))
          (local-transform> [(keypath :to-close) NONE>] $$microbatch-task)

          (default>)
          (identity nil :> *to-close-as-it-was))
        ;; The closing, only when the head read a `:to-close`: on the keeping
        ;; task of the batch before, what that batch left to keep is read,
        ;; appended to the gate's depot of answers, and taken out. Each
        ;; append is waited for. The entries are of rows that the batch
        ;; before committed, so an attempt that runs again appends copies.
        (batch/closes? *to-close :> *closes)
        (<<if *closes
          (get *to-close :batch :> *closed-batch)
          (batch/keeping-route *closed-batch :> *keeping-route)
          (|hash *keeping-route)
          (batch/to-keep-range *closed-batch :> [*keep-from *keep-to])
          (local-select> [(keypath :to-keep) (sorted-map-range *keep-from *keep-to) (subselect ALL)]
                         $$microbatch-task
                         {:allow-yield? true}
                         :> *kept)
          (batch/records-of-closing *to-close *last-stamp *kept :> *records)
          (loop<- [*records-left *records
                   :> *appended]
            (<<if (empty? *records-left)
              (:> true)
             (else>)
              (first *records-left :> *record)
              (depot-partition-append! *microbatch-answer-depot *record :append-ack)
              (continue> (rest *records-left))))
          (batch/keys-kept *kept :> *kept-keys)
          (loop<- [*kept-keys-left *kept-keys
                   :> *taken-out]
            (yield-if-overtime)
            (<<if (empty? *kept-keys-left)
              (:> true)
             (else>)
              (first *kept-keys-left :> *kept-key)
              (local-transform> [(keypath :to-keep *kept-key) NONE>] $$microbatch-task)
              (continue> (rest *kept-keys-left)))))
        (|all)
        ;; A batch that made a stamp is closed on every task, behind the
        ;; appends of its entries. A batch of strays alone made no stamp,
        ;; and `:closed-through` holds the gate's last stamp already.
        (<<if (and> *closes *last-stamp (some? (get *to-close :clock)))
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

              (default>)
              (identity nil :> *found))
            (batch/replies *read *found *batch-clock *base *batch-end :> *more-replies)
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
        ;; The rows are written as they were sent: no order is needed
        ;; between them.
        (get *sent-here :rows :> *rows-here)
        (loop<- [*rows-left *rows-here
                 :> *rows-written]
          (yield-if-overtime)
          (<<if (empty? *rows-left)
            (:> true)
           (else>)
            (first *rows-left :> [*sent-which *sent-key *sent-row])
            (write-row $$microbatch-answers $$microbatch-facts $$microbatch-facts-by-value
                       $$microbatch-task
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
            (batch/decide-next *pass *met *local-rows *other-depot-row :> *pass-after)
            (continue> (rest *meeting-left) *pass-after)))
        (batch/rows-to-send *pass-done :> *rows-out)
        ;; What is to keep of this batch: the answers that this task wrote
        ;; from its parcels or sends from its pass, and the strays' entries
        ;; that step 1 sent here. It rides the send of the pass's rows, to
        ;; the batch's keeping task.
        (local-select> (keypath :stray :batch) $$questions :> *strays-here)
        (ops/current-microbatch-id :> *batch-of-rows)
        (ops/current-task-id :> *task-of-rows)
        (batch/rows-to-keep *batch-of-rows *task-of-rows *rows-here *rows-out *strays-here
                            :> *rows-to-keep)
        (into *rows-out *rows-to-keep :> *rows-and-kept)
        (ops/explode *rows-and-kept :> {:keys [*every-task? *home *which *key *row]})
        (<<if *every-task?
          (|all)
         (else>)
          (|hash *home))
        (write-row $$microbatch-answers $$microbatch-facts $$microbatch-facts-by-value
                   $$microbatch-task
                   *which *key *row
                   :> *written)))))
