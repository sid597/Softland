;; Before changing this file, read the plan of this part again:
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md

(ns softland.store.module.batch-clock
  "What keeps the clock of each batch of the microbatch gate: the stream
  topology `batch-clock` and its one PState, `$$batch-clock`. It requires
  Rama.

  What it is for. Rama can run a batch of the microbatch gate a second time,
  also after one task has shown its part of the first attempt. A fact's row
  holds a stamp, and a stamp holds the clock. So the gate takes one clock
  for a batch and keeps it where a later attempt of the same batch reads it;
  each attempt then makes each fact's row, and each answer's stamp and
  verdict, as the first made them. This topology is where the clock is kept.

  What it takes. Each record of `*batch-clock-depot`, which the module
  declares (`softland.store.module`) and no client can append to. A record
  is {:batch :clock}: the microbatch id of a run of the microbatch gate, and
  a reading of the clock in milliseconds. Step 2 of that gate appends one,
  on the gate's task, with the ack level :ack, in an attempt of a batch with
  things to decide that finds no clock kept for its batch
  (`softland.store.module.microbatch-gate`). A depot partitions by task, and
  an append from a topology goes to the partition of the task that the code
  is on; so every record is on the one partition of the gate's task, and
  the other partitions stay empty.

  What it changes. It owns `$$batch-clock` and it alone writes it: one row,
  under the depot word `:microbatch`, on the gate's task, which holds the
  clock of the newest batch that kept one
  (`softland.store.record.tables/batch-clock`). It is durable state of the
  cluster. The microbatch gate borrows it to read, on the same task. Nothing
  is kept in the memory of a process: a restart of a worker is one cause of
  a second attempt.

  The event of one record, on the task of the depot's partition, with no
  partitioner and so no hop:

    1. read the row under `:microbatch`;
    2. `softland.store.gate.batch/clock-row-to-keep` of the row and the
       record: the row to write, or nil;
    3. for a row, write it whole at that key.

  First write wins. The row is written when the table holds none, or one of
  an older batch. A record of the row's own batch writes nothing, so the
  clock that was kept first for a batch stays. A record of an older batch
  writes nothing, so a record that Rama hands over late never puts the row
  back. A thing that is no record writes nothing, and nothing throws: a
  record that throws would be tried again for ever.

  When Rama runs the event again. A stream topology can process a record
  again, also after its write was committed, and after a worker is started
  again it can be handed records that it has processed. The one write is a
  whole row at a fixed key, and a second run for the same record finds a row
  of that batch and writes nothing. The topology appends to no depot.

  Why a stream topology, and one of its own. The gate's append waits, by
  the ack level :ack, until this topology has processed the record and the
  row is durable; only a stream topology that is colocated with the depot
  answers such an ack. A microbatch topology cannot write the row itself: a
  topology writes only its own PStates, and Rama sets the microbatch gate's
  PStates back when an attempt fails. It is not a second source of the
  streaming gate: every batch of the base would then wait in that gate's
  queue on one task, behind the acts of the streaming layers of that task,
  and a streaming layer that is stuck there would stop the base too. Here
  the queue holds clocks only.

  What it does not handle. It does not know whether a batch was committed,
  and it keeps no clock of a batch before the newest: the depot holds each
  record, and no code of the store reads the old ones. It checks nothing
  of who appended:
  the depot takes no client's append, so only the module's own code can.

  The source starts from the beginning of the depot, with Rama's default
  retry mode, a failed record alone: by the rule above, the order of two
  records and a record read again write nothing wrong. The module sets no
  option of Rama for the topology."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [softland.store.gate.batch :as batch]
            [softland.store.record.tables :as record-pstates]))

(defn add-to
  "Adds what keeps a batch's clock to a module. Takes the module's
  `topologies`. Declares the stream topology `batch-clock`, its PState
  `$$batch-clock`, and the event of one record, as the namespace docstring
  walks it. What must hold: the module declares the depot
  `*batch-clock-depot`."
  [topologies]
  (let [topology (stream-topology topologies "batch-clock")]
    (declare-pstate topology $$batch-clock (record-pstates/batch-clock))
    (<<sources topology
      (source> *batch-clock-depot {:start-from :beginning} :> *record)
      (identity batch/gates-depot :> *gates-route)
      (local-select> (keypath *gates-route) $$batch-clock :> *held)
      (batch/clock-row-to-keep *held *record :> *row)
      (<<if *row
        (local-transform> [(keypath *gates-route) (termval *row)] $$batch-clock)))))
