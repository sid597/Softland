;; Before changing this file, read the papers of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PROGRESS.md

(ns softland.store.module.streaming-gate
  "The streaming gate: the stream topology that decides each thing of the
  streaming depot, and the four PStates of its record. It requires Rama.

  What it takes. Each thing that arrives on `*streaming-depot`, which the
  module declares (`softland.store.module`) and which partitions by the
  thing's `:layer`. So a layer is one partition of the depot, and its
  things have the depot's order. Anything can arrive: the door is not the
  only sender.

  What it changes. It owns four PStates, the record of the streaming layers,
  and it alone writes them (in Rama only the owning topology writes a
  PState): `$$streaming-answers`, `$$streaming-facts`,
  `$$streaming-facts-by-value` and `$$streaming-task`. Their schemas are
  `softland.store.record.tables`'s and their rows
  `softland.store.record.rows`'s. They are durable state of the cluster.
  Every row of a streaming layer is on the layer's task, and on no other.
  The gate keeps nothing in the memory of a process. It declares no depot,
  appends to none, and borrows no PState.

  The walk. One event is one call of `softland.store.module.walk/decide`,
  the walk of one thing that the microbatch gate calls too, with the gate's
  four tables and the word `:streaming`, on the task of the depot's
  partition, which is the layer's task. The walk's namespace docstring
  holds what a decision is made of, the steps, what each kind of thing
  costs in reads and writes, how a stamp is counted in its layer, and what
  holds of the marked keys. No gate reads a clock: the time of a decision
  is the time on the act, which the door wrote. What the walk gives back,
  the rows that a gate may copy to other tasks, is not used here: a
  streaming gate that copied would not hold the walk's limit for a gate
  that copies, and a row of a streaming layer lives on one task.

  So an event makes no hop to another task, appends nothing and waits for
  nothing. Between its first read and its last write stands nothing that
  gives the task back, which is the walk's contract. The event starts and
  ends on the task of the depot's partition, so all its writes are one
  group: they become visible together or not at all, and a reader sees an
  accepted answer, each fact of its act, the entry that came into the facts
  by value and the entry that went out at once. Rama's docs say this of a
  batch of streaming events on one task. No other event of the task runs
  between this event's reads and its writes.

  What an ack means. Nothing is given back in the ack: the ack is a signal,
  and the record is the truth, read by act-id. A sender's append with the
  ack level :ack returns when the event is done and its rows are visible;
  the walk's last write is the last step before its result, and the event
  ends with that result. An ack can fail for an act that the gate accepted.

  When Rama runs an event again. A stream topology can process a record
  again, also after its writes were committed. Each write of a decision
  stands behind the walk's check of the answer under the act-id, and the
  answer commits with the rest. So a second run after a commit writes no
  row: one act-id has one answer, and a layer's last stamp is not raised
  twice for one thing. A second run after a discard is the first that
  counts; the rows of the discarded run were never visible, and when the
  second run reads the rows that the discarded run read, it writes the
  same rows with the same stamp. The walk's namespace docstring says this
  in full, under \"When a thing is run again\". The source's retry mode is
  \"all after\": when a record fails, each record after it on the partition
  is processed again, in order.

  What this does not close: the order of a layer's acts after a failure.
  Without a failure, the acts of a layer are decided in the depot's order.
  When something throws while a batch of events runs, Rama discards the
  writes of that batch and processes its records again. It is not shown
  whether acts of a layer that were on their way then can be decided in
  another order than the depot's. If they can, their stamps and their
  answers can differ from those of a run of the gate over the same depot
  that meets no failure there. A run of this build looks into that (Sid's
  decision 7 of 13 October 2026), and this docstring says nothing of what
  it finds. The gate's own code does not throw on what arrives, so such a
  failure needs a fault of the machine, of Rama or of this module. The
  store's map, one folder up, names the setting of Rama that orders a
  layer's acts after a failure, and what it costs.

  What the gate rests on and does not test. The depot's partitioner puts a
  thing on its layer's task, and the gate makes no hop. A thing that a
  topology appends to another partition is decided against that task's
  record, with that task's last stamp of the layer; the store's map, one
  folder up, says what follows. And the walk's own list, in its namespace
  docstring.

  The source starts from the beginning of the depot, so no thing that is in
  the depot is skipped whenever the topology first runs on a partition."
  (:require [com.rpl.rama :refer :all]
            [softland.store.module.walk :as walk]
            [softland.store.record.tables :as record-pstates]))

(defn add-to
  "Adds the streaming gate to a module. Takes the module's `topologies`.
  Declares the stream topology `streaming-gate` and its four PStates, and
  the dataflow of one event: one call of the walk
  (`softland.store.module.walk/decide`) with the four tables and the word
  `:streaming`, whose result is not used. Every read and every write of the
  event are the walk's, on the task of the depot's partition: the event has
  no partitioner, no append and no wait. The source starts from the
  beginning of the depot, with the retry mode \"all after\". What must hold:
  the module declares the depot `*streaming-depot`, or a mirror of it under
  that name, partitioned by the thing's `:layer`. The gate needs no other
  part of the module: it declares no depot and reads no PState of another
  topology."
  [topologies]
  (let [gate (stream-topology topologies "streaming-gate")]
    (declare-pstate gate $$streaming-answers (record-pstates/answers))
    (declare-pstate gate $$streaming-facts (record-pstates/facts))
    (declare-pstate gate $$streaming-facts-by-value (record-pstates/facts-by-value))
    (declare-pstate gate $$streaming-task (record-pstates/task))
    (<<sources gate
      (source> *streaming-depot {:start-from :beginning :retry-mode :all-after} :> *thing)
      (walk/decide $$streaming-answers $$streaming-facts $$streaming-facts-by-value
                   $$streaming-task :streaming *thing))))
