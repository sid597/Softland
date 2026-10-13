;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/probes-the-bases-gate/RESULTS.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md

(ns softland.store.module.microbatch-gate
  "The microbatch gate: the microbatch topology that decides each thing of
  the microbatch depot, and the four PStates of its record. It requires
  Rama.

  What it takes. Each thing that arrives on `*microbatch-depot`, which the
  module declares (`softland.store.module`) and which partitions by the
  thing's `:layer`. So a layer is one partition of the depot, one line, and
  its things have the depot's order. Anything can arrive: the door is not
  the only sender.

  What it changes. It owns four PStates, the record of the microbatch
  layers, and it alone writes them (in Rama only the owning topology writes
  a PState): `$$microbatch-answers`, `$$microbatch-facts`,
  `$$microbatch-facts-by-value` and `$$microbatch-task`. Their schemas are
  `softland.store.record.tables`'s, the same four as the streaming gate's,
  and their rows `softland.store.record.rows`'s. They are durable state of
  the cluster. The gate keeps nothing in the memory of a process. It
  declares no depot, appends to none, and borrows no PState.

  What it is built for. The base: the layer that the system provides to
  everyone, which few hands change and which does not need to be instant
  (Sid, 13 October 2026). A microbatch layer that many people write into is
  not designed. Whether such a layer is a layer apart from the base is not
  decided, and nothing here is built for it: every act of a microbatch
  layer is decided on one task, one after the other.

  Where a row lives.

    - The row of a fact, and a row of the facts by value: on every task.
      The layer's task writes it in the decision, and the gate copies it to
      each other task. So a read of a microbatch layer's facts needs one
      task, whichever it is.
    - The answer of an act, and the layer's last stamp: on the layer's task
      alone, the task of the layer's depot partition. A maker reads her
      answer there, as in a streaming layer.
    - The digest of a stray: on the task of the partition where the stray
      arrived, under its reason word.

  The walk. One task decides: Rama emits a record of partition p on task p,
  which is the layer's task, and the gate decides it there, at once, with
  `softland.store.module.walk/decide`, the walk of one thing that the
  streaming gate calls too. The gate hands it its four tables and the word
  `:microbatch`. The walk's namespace docstring holds what a decision is
  made of, the steps, what each kind of thing costs, how a stamp is
  counted in its layer, and the contract that the walk never gives its
  task back. A decision is made of the thing and of rows of this gate's
  record on the layer's task. No gate reads a clock, and nothing of a
  batch is in a decision: not its id, not its size, not where it is cut.
  The next record of the partition is decided when this one's rows are
  written, so it reads them: an act that replaces the fact of the act
  before it in the same batch is accepted, a mark holds from the next act,
  and the stamps of a layer go up in the depot's order.

  The copy. The walk gives the rows of an accept that are a fact's or of
  the facts by value, in the order in which it wrote them. When there are
  some, the gate remembers the deciding task, goes to every task, and on
  every task but the deciding one writes those rows in that order: a fact's
  row at its key, a row of the facts by value at its key, or that key taken
  out when the row is nil. A refusal, a thing under an act-id that has an
  answer and a stray give no rows, and the gate then goes nowhere.

  The deciding task is passed over. It holds the rows already. Its own copy
  would come back to it after it has decided later records of the batch,
  and would set a key back to an older row until the later copy arrives. A
  stand-in gate that did write its own copies showed such a row for a time
  (the plan of this part, claim 3). The pass costs one comparison and
  takes the question away.

  The copies of one decision go out before the next record of the
  partition is decided, and Rama keeps the order of what one task sends to
  another. So each task writes the copies of a layer in the order of the
  decisions, and ends on the row that the layer's task ends on.

  What the copy costs, in a module of N tasks: for an accept that wrote c
  rows of facts and of the facts by value, one message to each of the N
  tasks, and c writes on each of the N - 1 other tasks. No read. One
  accepted act with one value under a plain key that is not marked is
  4 reads and 3 writes on the layer's task, by the walk, and 1 write on
  each other task: 3 + (N - 1) writes in all.

  A second attempt of a batch. When something throws while a batch runs,
  Rama sets each PState of the topology back to what it held before the
  batch, on every task, and processes the same records again. A failed
  attempt leaves no row, on the layer's task or on a task that had written
  a copy. The second attempt reads the rows that the first read, and a
  decision holds no clock and nothing of the attempt: it writes the same
  rows with the same stamps, and sends the same copies. So a row that a
  reader was given does not change when a batch is tried again.

  A throw is no quiet retry. Rama treats a throw in the gate's code as
  fatal for the worker whose task thread ran it: that worker shuts down,
  is launched again, and the batch is then tried again. Run in Rama's
  in-process cluster, on a stand-in gate and on the draft of this gate,
  with a function that the gate calls made to throw once: the worker
  restarted, and after the retry every table on every task was equal to
  that of a module that did not fail. The gate's own code does not throw
  on what arrives. A thing that made it throw each time would stop the
  topology at that batch, with a worker restart at each try.

  When the store is run again. A second module that reads the same depot
  from its beginning, with the same number of tasks, makes the same rows on
  the same tasks, however its batches are cut: a decision reads the rows
  that the things before it in the partition wrote, and those are the same.

  What a reader sees, and when. A reader outside the topology sees nothing
  of a batch until the batch is committed on the task that it reads. Rama
  shows a batch task by task: each task commits its part of a batch by
  itself, and that task's writes of the batch become visible together. On
  the layer's task that is an answer with each fact of its act, the entry
  that came into the facts by value and the entry that went out. Two tasks
  do not commit at one moment. So for the time of a commit a reader of
  another task can see the copy of a fact whose answer is not yet visible
  on the layer's task, or not yet see the fact of an answer that is. Rama
  starts the next batch when every task has committed this one. This
  paragraph is from Rama's docs on the phases of a microbatch; no run here
  observed a commit from outside.

  What an ack means. An append to the depot does not wait for the gate: the
  ack of an append says that the depot holds the thing, and nothing about
  the decision. The answer is read by act-id, on the layer's task, and it
  is there when the batch that decided the thing is committed on that
  task.

  What the gate rests on and does not test.

    - The depot's partitioner puts a thing on its layer's task. A thing
      that a topology appends to another partition is decided against that
      task's part of the record: its facts are on every task, and its
      layer's last stamp and its answers are not.
    - Rama emits the records of one partition in the depot's order, and
      runs the dataflow of one record up to its first partitioner before it
      takes the next. The walk has no partitioner, so a record is decided
      whole before the next is taken.
    - No code path of the walk gives its task back
      (`softland.store.module.walk`). No check of the gate looks for a
      yield at run time.
    - Rama keeps the order of what one task sends to another task.
    - The number of tasks of a module that reads this depot is the depot's
      number of partitions. A second module that mirrors the depot with
      another number of tasks does not get a layer's partition on the task
      that its own hash of the layer gives: its answers would be on a task
      where a read by the layer does not look. Rama's own way to add tasks
      has the new module copy the depots into depots of its own; that is
      not built here.

  The source starts from the beginning of the depot, so no thing that is in
  the depot is skipped whenever the topology first runs on a partition."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [softland.store.module.walk :as walk]
            [softland.store.record.tables :as record-pstates]))

(defn add-to
  "Adds the microbatch gate to a module. Takes the module's `topologies`.
  Declares the microbatch topology `microbatch-gate` and its four PStates,
  and the dataflow of one record: the walk on the task where the record is
  emitted, then, for an accept, the copy of its rows to every other task,
  as the namespace docstring says. Each write is a whole row at a fixed key,
  or a fixed key taken out. The one partitioner of the dataflow stands
  behind the walk's last write. The source starts from the beginning of the
  depot. What must hold: the module declares the depot `*microbatch-depot`,
  or a mirror of it under that name, partitioned by the thing's `:layer`,
  and has as many tasks as that depot has partitions. The gate needs no
  other part of the module: it declares no depot and reads no PState of
  another topology."
  [topologies]
  (let [gate (microbatch-topology topologies "microbatch-gate")]
    (declare-pstate gate $$microbatch-answers (record-pstates/answers))
    (declare-pstate gate $$microbatch-facts (record-pstates/facts))
    (declare-pstate gate $$microbatch-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$microbatch-task (record-pstates/task))
    (<<sources gate
      (source> *microbatch-depot {:start-from :beginning} :> %things)
      ;; Each record of the batch, on the task of its partition, in the
      ;; depot's order.
      (%things :> *thing)

      ;; The decision, whole, on this task: every read and every write of
      ;; it. What comes back is what may be copied.
      (walk/decide $$microbatch-answers $$microbatch-facts $$microbatch-facts-by-value
                   $$microbatch-task :microbatch *thing
                   :> *copies)

      ;; The copy, only for an accept: to every task, and written on every
      ;; task but the deciding one, in the order in which the walk wrote.
      (<<if (seq *copies)
        (ops/current-task-id :> *deciding-task)
        (|all)
        (ops/current-task-id :> *this-task)
        (<<if (not= *this-task *deciding-task)
          (ops/explode *copies :> [*which *key *row])
          (<<cond
            (case> (= *which :fact))
            (local-transform> [(keypath *key) (termval *row)] $$microbatch-facts)

            (case> (and> (= *which :by-value) (nil? *row)))
            (local-transform> [(keypath *key) NONE>] $$microbatch-facts-by-value)

            (case> (= *which :by-value))
            (local-transform> [(keypath *key) (termval *row)] $$microbatch-facts-by-value)

            (default>)
            (identity nil :> *no-row)))))))
