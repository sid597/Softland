;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/probes-the-bases-gate/RESULTS.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md

(ns softland.store.module.microbatch-gate
  "The microbatch gate: the microbatch topology that decides each thing of
  the microbatch depot, and the six PStates of its record. It requires
  Rama.

  Anything can arrive on the depot: the door is not the only sender. The
  gate alone writes its six PStates: in Rama only the owning topology
  writes a PState.

  Built for the base: a lane that few hands change and that does not need
  to be instant. A microbatch lane that many people write into is not
  designed: every act of a microbatch lane is decided on one task, one
  after the other.

  Where a row lives.

    - The row of a fact, and a row of the facts by value: on every task.
      So a read of a microbatch lane's facts needs one task, whichever it
      is.
    - The answer of an act, the lane's last stamp, the lane's line, its
      things and its rows by who: on the lane's task alone, the task of
      the lane's depot partition, with no copy. So a read of the base's
      acts reads the base's task, and the base's bytes are not held once
      on every task.
    - The digest of a stray: on the task of the partition where the stray
      arrived.

  The walk. Rama emits a record of partition p on task p, which is the
  lane's task, and the gate decides it there, at once
  (`softland.store.module.walk/decide`). Nothing of a batch is in a
  decision: not its id, not its size, not where it is cut. The next record
  of the partition is decided when this one's rows are written, so it
  reads them: an act that replaces the fact of the act before it in the
  same batch is accepted, a mark holds from the next act, and the stamps of
  a lane go up in the depot's order.

  The copy. The deciding task is passed over: it holds the rows already.
  Its own copy would come back to it after it has decided later records of
  the batch, and would set a key back to an older row until the later copy
  arrives. A stand-in gate that did write its own copies showed such a row
  for a time (the plan of this part, claim 3).

  The copies of one decision go out before the next record of the
  partition is decided, and Rama keeps the order of what one task sends to
  another. So each task writes the copies of a lane in the order of the
  decisions, and ends on the row that the lane's task ends on.

  What the copy costs, in a module of N tasks: for an accept that wrote c
  rows of facts and of the facts by value, one message to each of the N
  tasks, and c writes on each of the N - 1 other tasks. No read. One
  accepted act with one value under a plain key that is not marked is
  5 reads and 3 writes on the lane's task, by the walk's count
  (`softland.store.module.walk`), and 1 write on each other task:
  3 + (N - 1) writes in all. Not in that count: each message carries the c
  rows whole, and each row of a microbatch lane's facts is stored once on
  every task, N times what a streaming lane's row takes.

  A second attempt of a batch. When something throws while a batch runs,
  Rama sets each PState of the topology back to what it held before the
  batch, on every task, and processes the same records again. A failed
  attempt leaves no row. The second attempt reads the rows that the first
  read, and a decision holds no clock and nothing of the attempt: it writes
  the same rows with the same stamps, and sends the same copies. So a row
  that a reader was given does not change when a batch is tried again.

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
  the same tasks, however its batches are cut.

  What a reader sees, and when. A reader outside the topology sees nothing
  of a batch until the batch is committed on the task that it reads. Each
  task commits its part of a batch by itself, and that task's writes of the
  batch become visible together: on the lane's task, an answer with each
  fact of its act, the entry that came into the facts by value and the
  entry that went out. Two tasks do not commit at one moment. So for the
  time of a commit a reader of another task can see the copy of a fact
  whose answer is not yet visible on the lane's task, or not yet see the
  fact of an answer that is. Rama starts the next batch when every task
  has committed this one. This paragraph is from Rama's docs on the phases
  of a microbatch; no run here observed a commit from outside.

  What an ack means. An append to the depot does not wait for the gate: its
  ack says that the depot holds the thing, and nothing about the decision.
  The answer is read by act-id, on the lane's task, and it is there when
  the batch that decided the thing is committed on that task.

  What the gate rests on and does not test.

    - The depot's partitioner puts a thing on its lane's task. A thing
      that a topology appends to another partition is decided against that
      task's part of the record: its facts are on every task, and its
      lane's last stamp and its answers are not. So an act of the base
      on a wrong partition whose permission stands would be accepted
      there, with a stamp that follows nothing of the base's line, and its
      copies would write over the rows of the same places on every other
      task, the base's own among them. No client can put a thing there: a
      client's append goes through the depot's partitioner, and no
      topology of this module appends to a depot. Read from the dataflow;
      no run made such an append.
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
      another number of tasks does not get a lane's partition on the task
      that its own hash of the lane gives: its answers would be on a task
      where a read by the lane does not look. Rama's own way to add tasks
      has the new module copy the depots into depots of its own; that is
      not built here.

  The source starts from the beginning of the depot, so no thing that is in
  the depot is skipped whenever the topology first runs on a partition."
  {:softland/whose-call
   [{:state :decided
     :what "The microbatch gate is built for the base: the lane that the
            system provides to everyone, which few hands change and which
            does not need to be instant."
     :source "Sid, 13 October 2026"}
    {:state :not-decided
     :what "A microbatch lane that many people write into is not designed.
            Whether such a lane is a lane apart from the base is not decided,
            and nothing in the microbatch gate is built for it: every act of
            a microbatch lane is decided on one task, one after the other."}]}
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [softland.store.module.walk :as walk]
            [softland.store.record.tables :as record-pstates]))

(defn add-to
  "The one partitioner of the dataflow stands behind the walk's last write.
  What must hold: the module declares the depot `*microbatch-depot`, or a
  mirror of it under that name, partitioned by the thing's `:lane`, and
  has as many tasks as that depot has partitions."
  [topologies]
  (let [gate (microbatch-topology topologies "microbatch-gate")]
    (declare-pstate gate $$microbatch-answers (record-pstates/answers))
    (declare-pstate gate $$microbatch-facts (record-pstates/facts))
    (declare-pstate gate $$microbatch-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$microbatch-task (record-pstates/task))
    (declare-pstate gate $$microbatch-line (record-pstates/line))
    (declare-pstate gate $$microbatch-things (record-pstates/things))
    (<<sources gate
      (source> *microbatch-depot {:start-from :beginning} :> %things)
      ;; Each record of the batch, on the task of its partition, in the
      ;; depot's order.
      (%things :> *thing)

      (walk/decide $$microbatch-answers $$microbatch-facts $$microbatch-facts-by-value
                   $$microbatch-task $$microbatch-line $$microbatch-things :microbatch *thing
                   :> *copies)

      ;; The copy: written on every task but the deciding one, in the order
      ;; in which the walk wrote.
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
