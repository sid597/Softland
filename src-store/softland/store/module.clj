;; Before changing this file, read the plan of this part again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md

(ns softland.store.module
  "The Rama module `Store`: its depot, and the calls that add the gate and
  the reads. It requires Rama.

  What a client of the module opens, by name:

    *streaming-depot      the depot to append a thing for a streaming layer
    answer-by-act-id      the query: the recorded answer under an act-id
    locks-of-request      the query: the locks that a request made
    class-of-layer        the query: the class of a layer
    $$streaming-task      the PState of the strays, for an operator: select
                          [:strays (keypath word) (view count)] with a layer
                          as the partitioning key gives the number of
                          different strays of that word on that layer's task

  The name of the module, for a client, is `(get-module-name Store)`.

  State. The module owns the streaming depot. The streaming gate
  (`softland.store.streaming-gate`) owns the record's PStates. The queries
  (`softland.store.door-reads`) own nothing.

  The streaming depot holds every thing sent for a streaming layer, as it
  was sent, and it is the replay log of those layers. It is never trimmed:
  the module sets no trimming option, and Rama keeps every record by
  default. So nothing secret goes on it: a request for locks carries a
  count, and no code of the module appends a lock's bytes.

  It partitions by the hash of the thing's `:layer`. The acts of one layer
  depend on each other's order (a give and the first act under it, a request
  and the first use of its lock, two acts that replace one fact), and one
  partition for a layer gives them the depot's order. That partition is the
  task on which every row of the layer lives, so the gate reaches the record
  with no hop. The partitioner applies the keyword `:layer` to the thing; on
  a thing that is no map a keyword gives nil and does not throw, and it runs
  on the appending client, so no thing can stop a partition through it.

  A client appends with the ack level :ack: the append returns when the gate
  has processed the thing and its rows are visible. An ack can fail for an
  act that the gate accepted, so the ack is a signal; the sender reads the
  answer by act-id.

  Not built: the microbatch depot and its gate, and with them the base."
  (:require [com.rpl.rama :refer :all]
            [softland.store.door-reads :as door-reads]
            [softland.store.streaming-gate :as streaming-gate]))

(defmodule Store [setup topologies]
  (declare-depot setup *streaming-depot (hash-by :layer))
  (streaming-gate/add-to topologies)
  (door-reads/add-to topologies))
