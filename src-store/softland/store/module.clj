;; Before changing this file, read the plans of the two parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-microbatch-gate.md

(ns softland.store.module
  "The Rama module `Store`: its two depots, and the calls that add the two
  gates and the reads. It requires Rama.

  What a client of the module opens, by name:

    *streaming-depot      the depot to append a thing for a streaming layer
    *microbatch-depot     the depot to append a thing for a microbatch layer
    answer-by-act-id      the query: the recorded answer under an act-id
    locks-of-request      the query: the locks that a request made
    class-of-layer        the query: the class of a layer
    $$streaming-task      the PState of the streaming gate's strays, for an
                          operator: select
                          [:strays (keypath word) (view count)] with a layer
                          as the partitioning key gives the number of
                          different strays of that word on that layer's task
    $$microbatch-task     the same for the microbatch gate. A stray of that
                          gate is kept on the task where the depot put it,
                          so an operator reads each task

  The name of the module, for a client, is `(get-module-name Store)`.

  State. The module owns the two depots. Each gate owns the PStates of its
  record: the streaming gate (`softland.store.streaming-gate`) those of the
  streaming layers, the microbatch gate (`softland.store.microbatch-gate`)
  those of the microbatch layers. The queries (`softland.store.door-reads`)
  own nothing.

  Each depot holds every thing sent for a layer of its class, as it was
  sent, and it is the replay log of those layers. Neither is ever trimmed:
  the module sets no trimming option, and Rama keeps every record by
  default. So nothing secret goes on a depot: a request for locks carries a
  count, and no code of the module appends a lock's bytes.

  The streaming depot partitions by the hash of the thing's `:layer`. The
  acts of one layer depend on each other's order (a give and the first act
  under it, a request and the first use of its lock, two acts that replace
  one fact), and one partition for a layer gives them the depot's order.
  That partition is the task on which every row of the layer lives, so the
  gate reaches the record with no hop. The partitioner applies the keyword
  `:layer` to the thing; on a thing that is no map a keyword gives nil and
  does not throw.

  The microbatch depot partitions by the hash of the entity of the thing's
  first fact (`softland.store.batch/first-entity`). A microbatch layer's
  facts are on the tasks of their entities, so an act on one entity arrives
  on the task where its places are, and a request for locks arrives where
  its locks and its answer will be. The acts with one first entity are one
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
            [softland.store.batch :as batch]
            [softland.store.door-reads :as door-reads]
            [softland.store.microbatch-gate :as microbatch-gate]
            [softland.store.streaming-gate :as streaming-gate]))

(defmodule Store [setup topologies]
  (declare-depot setup *streaming-depot (hash-by :layer))
  (declare-depot setup *microbatch-depot (hash-by batch/first-entity))
  (streaming-gate/add-to topologies)
  (microbatch-gate/add-to topologies)
  (door-reads/add-to topologies))
