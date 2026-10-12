;; Before changing this file, read the plans of the two parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-microbatch-gate.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md
;; src/proposal/backend-on-rama-2026-10-02/before-real-data/PLAN-answers-in-a-log.md

(ns softland.store.module
  "The Rama module `Store`: its two depots of acts and its depot of batch
  clocks, and the calls that add the two gates, each with its depot of
  answers, the topology that keeps a batch's clock, and the reads. It
  requires Rama.

  What a client of the module opens, by name:

    *streaming-depot      the depot to append a thing for a streaming layer
    *microbatch-depot     the depot to append a thing for a microbatch layer
    *batch-clock-depot    the depot of batch clocks. No client can append to
                          it; a client can read its log
    *streaming-answer-depot
                          the streaming gate's depot of answers: what that
                          gate decided. No client can append to it; a
                          rebuild reads its log
    *microbatch-answer-depot
                          the same for the microbatch gate
    answer-by-act-id      the query: the recorded answer under an act-id
    depot-of-layer        the query: the depot of a layer
    $$streaming-task      the PState of the streaming gate's strays, for an
                          operator: select
                          [:strays (keypath word) (view count)] with a layer
                          as the partitioning key gives the number of
                          different strays of that word on that layer's task
    $$microbatch-task     the same for the microbatch gate. A stray of that
                          gate is kept on the task where the depot put it,
                          so an operator reads each task
    $$batch-clock         the clock that is kept for the newest batch of the
                          microbatch gate that had things to decide, for an
                          operator: select (keypath :microbatch), which
                          also routes the select to the gate's task
    $$streaming-facts     the current facts of the streaming layers, for
                          the read path (`softland.store.read-path`):
                          select and push, with a partitioning key
    $$microbatch-facts    the same for the microbatch layers
    $$streaming-facts-by-value
                          the facts of the streaming layers in the order of
                          their values, for the read path: select and push,
                          with a partitioning key
    $$microbatch-facts-by-value
                          the same for the microbatch layers

  The name of the module, for a client, is `(get-module-name Store)`.

  State. The module has five depots. This namespace declares three: the two
  depots of acts and the depot of batch clocks. Each gate declares its own
  depot of answers, in its `add-to`, which is handed the module's `setup`
  for that; no code here names one. Each gate owns the PStates of its
  record: the streaming gate (`softland.store.module.streaming-gate`) those of the
  streaming layers, the microbatch gate (`softland.store.module.microbatch-gate`)
  those of the microbatch layers. The topology that keeps a batch's clock
  (`softland.store.module.batch-clock`) owns `$$batch-clock`. The queries
  (`softland.store.module.door-reads`) own nothing.

  Each depot of acts holds every thing sent for a layer whose depot it is,
  as it was sent. What a gate decided of a thing is in the gate's depot of
  answers: the act-id, the stamp, the digest, and the word of a refusal,
  and for a stray its word, its digest and its task. No value of a fact is
  there. From a depot of acts and its gate's depot of answers the tables
  can be made again; the tables stay the record that is read, and no
  topology of the module has a source on a depot of answers. No depot is
  ever trimmed: the module sets no trimming option, and Rama keeps every
  record by default. Every value is plain on a depot of acts: a client of
  the cluster that reads one, or a copy of its log taken alone, holds every
  value that was sent.

  A topology appends in three places, and to no depot of acts: each gate to
  its depot of answers, and the microbatch gate to the depot of batch
  clocks. The streaming gate appends an entry behind the commit of its
  answer's row; the microbatch gate appends what a batch decided in the
  next run of the gate, on the batch's keeping task. Each gate's namespace
  says how.

  The depot of batch clocks takes no act, and no gate decides from it: the
  depots of acts are two, one for each gate. It is declared :disallow, so no
  client can append to it. One place appends to it: step 2 of
  the microbatch gate, on the gate's task, in an attempt of a batch with
  things to decide that finds no clock kept for its batch. A record is
  {:batch :clock}: the microbatch id of the run, and a reading of the clock
  in milliseconds. So the depot holds one record for each batch that had
  things, and one more for each later attempt of a batch that found no
  clock kept. An append from a topology goes to the partition of the task
  that the code is on, so every record is on the one partition of the gate's
  task. The log is what Rama does not set back when an attempt of a batch
  fails; the topology `batch-clock` reads it and keeps the newest batch's
  clock in one row. No topology reads its old records. A rebuild
  (`softland.store.rebuild`) is given the log whole, and holds against it
  the clock that the microbatch gate's depot of answers keeps for a batch.
  It is never trimmed, as no log of the module is. That the log is kept
  whole is Sid's decision (src-store/README.md, the decisions of 12 October
  2026, entry 3); that entry says how it reaches this log too.

  The streaming depot partitions by the hash of the thing's `:layer`. The
  acts of one layer depend on each other's order (a give and the first act
  under it, two acts that replace
  one fact), and one partition for a layer gives them the depot's order.
  That partition is the task on which every row of the layer lives, so the
  gate reaches the record with no hop. The partitioner applies the keyword
  `:layer` to the thing; on a thing that is no map a keyword gives nil and
  does not throw.

  The microbatch depot partitions by the hash of the entity of the thing's
  first fact (`softland.store.gate.batch/first-entity`). A microbatch layer's
  facts are on the tasks of their entities, so an act on one entity arrives
  on the task where its places are. Five kinds of fact are on every task
  instead (`softland.store.gate.batch/on-every-task?`): the layer's depot,
  each permission, who may see the layer, what it builds on and its marked
  keys. And a row of the facts by value is on the task of its value's
  digest, wherever its fact is. The acts with one first entity are one
  partition and keep the depot's order. Acts on two partitions have no
  order in the depot; the microbatch gate gives each batch one. The
  function gives nil, and does not throw, for a thing with no first fact.

  Both partitioners run on the appending client, so no thing can stop a
  partition through them.

  A client appends to the streaming depot with the ack level :ack: the
  append returns when the gate has processed the thing and its rows are
  visible. An ack can fail for an act that the gate accepted, so the ack is
  a signal; the sender reads the answer by act-id. An ack of the microbatch
  depot says that the depot holds the thing and nothing about the decision:
  the sender reads the answer by act-id until one is there."
  (:require [com.rpl.rama :refer :all]
            [softland.store.gate.batch :as batch]
            [softland.store.module.batch-clock :as batch-clock]
            [softland.store.module.door-reads :as door-reads]
            [softland.store.module.microbatch-gate :as microbatch-gate]
            [softland.store.module.streaming-gate :as streaming-gate]))

(defmodule Store [setup topologies]
  (declare-depot setup *streaming-depot (hash-by :layer))
  (declare-depot setup *microbatch-depot (hash-by batch/first-entity))
  (declare-depot setup *batch-clock-depot :disallow)
  (streaming-gate/add-to setup topologies)
  (batch-clock/add-to topologies)
  (microbatch-gate/add-to setup topologies)
  (door-reads/add-to topologies))
