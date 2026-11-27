;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PROGRESS.md

(ns softland.store.module.streaming-gate
  "The streaming gate: the stream topology that decides each thing of the
  streaming depot, and the seven PStates of its record. It requires Rama.

  Anything can arrive on the depot: the door is not the only sender. The
  gate alone writes its seven PStates: in Rama only the owning topology
  writes a PState. Every row of a streaming lane is on the lane's task,
  and on no other.

  One event is one call of `softland.store.module.walk/decide`, on the task
  of the depot's partition, which is the lane's task. What the walk gives
  back, the rows that a gate may copy to other tasks, is not used: a
  streaming gate that copied would not hold the walk's limit for a gate
  that copies.

  So an event makes no hop to another task, appends nothing and waits for
  nothing. It starts and ends on the task of the depot's partition, so all
  its writes are one group: they become visible together or not at all,
  and a reader sees an accepted answer, each fact of its act, the entry
  that came into the facts by value and the entry that went out at once.
  Rama's docs say this of a batch of streaming events on one task. No
  other event of the task runs between this event's reads and its writes.

  What an ack means. Nothing is given back in the ack: the ack is a signal,
  and the record is the truth, read by act-id. A sender's append with the
  ack level :ack returns when the event is done and its rows are visible:
  the walk's last write is the last step before its result, and the event
  ends with that result. An ack can fail for an act that the gate accepted.

  When Rama runs an event again. A stream topology can process a record
  again, also after its writes were committed; the walk's namespace
  docstring says what a second run gives. The rows of a discarded run were
  never visible, and a second run that reads the rows that the discarded
  run read writes the same rows with the same stamp. The source's retry
  mode is \"all after\": when a record fails, each record after it on the
  partition is processed again, in order.

  The order of a lane's acts after a failure. Without a failure, the acts
  of a lane are decided in the depot's order. A throw is no quiet retry:
  Rama treats a throw in an event as fatal for the worker whose task
  thread ran it and starts that worker again. The writes of the failed
  batch are gone, and the records are then decided in the depot's order,
  an act behind the failed one once, after it. Whether any failure can
  make acts of a lane that were on their way be decided in another order
  than the depot's, so that their stamps and their answers differ from
  those of a run of the gate over the same depot that meets no failure,
  was run for Sid's decision 7 of 13 October 2026. What the runs found:

    - On a stand-in gate of this gate's form, in Rama's in-process
      cluster, no failure that could be made in process brought a run
      again out in another order, in 87 of 87 runs: a throw, an answer
      that Rama could not write, a stall on the task's thread past the
      stream timeout, a slow write of the batch, a module update under
      load.
    - On this gate, inside the run-again proof: after a throw in an act's
      event, with the next act of the lane right behind it and with the
      next act in the depot before the event began, and after a stall
      with the thread held, a run again gave the same tables.
    - Not reached: a batch that Rama fails with no restart of the worker
      and whose rows are dropped, while a later act of the lane is in
      flight behind it. Computed with the stand-in gate's rule and not
      run: the later act would be decided before the failed one, and a run
      again would hold other answers for the two. No cheap way to close it
      was found.

  The gate's own code does not throw on what arrives, so a failure needs a
  fault of the machine, of Rama or of this module. The store's map, one
  folder up, holds the runs in full, the setting of Rama that orders a
  lane's acts after a failure and what it costs, and how the run-again
  proof names an act that is decided out of its line.

  What the gate rests on and does not test. The depot's partitioner puts a
  thing on its lane's task, and the gate makes no hop. A thing that a
  topology appends to another partition is decided against that task's
  record, with that task's last stamp of the lane; the store's map, one
  folder up, says what follows. And the walk's own list, in its namespace
  docstring.

  The source starts from the beginning of the depot, so no thing that is in
  the depot is skipped whenever the topology first runs on a partition."
  (:require [com.rpl.rama :refer :all]
            [softland.store.module.walk :as walk]
            [softland.store.record.tables :as record-pstates]))

(defn add-to
  "What must hold: the module declares the depot `*streaming-depot`, or a
  mirror of it under that name, partitioned by the thing's `:lane`."
  [topologies]
  (let [gate (stream-topology topologies "streaming-gate")]
    (declare-pstate gate $$streaming-answers (record-pstates/answers))
    (declare-pstate gate $$streaming-facts (record-pstates/facts))
    (declare-pstate gate $$streaming-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$streaming-permissions-by-holder (record-pstates/permissions-by-holder))
    (declare-pstate gate $$streaming-task (record-pstates/task))
    (declare-pstate gate $$streaming-line (record-pstates/line))
    (declare-pstate gate $$streaming-things (record-pstates/things))
    (<<sources gate
      (source> *streaming-depot {:start-from :beginning :retry-mode :all-after} :> *thing)
      (walk/decide $$streaming-answers $$streaming-facts $$streaming-facts-by-value
                   $$streaming-permissions-by-holder
                   $$streaming-task $$streaming-line $$streaming-things :streaming *thing))))
