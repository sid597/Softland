;; Before changing this file, read the plans of the parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PART-A.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md

(ns softland.store.module
  "The Rama module `Store`: its two depots of acts, and the call that adds
  the two gates and the door's two reads. It requires Rama.

  What a client of the module opens, by name:

    *streaming-depot      the depot to append a thing for a streaming layer
    *microbatch-depot     the depot to append a thing for a microbatch layer
    answer-by-act-id      the query: the recorded answer under an act-id
    depot-of-layer        the query: the depot of a layer
    $$streaming-task      the PState of the streaming gate's strays, for an
                          operator: select
                          [:strays (keypath word) (view count)] with a layer
                          as the partitioning key gives the number of
                          different strays of that word on that layer's task
    $$microbatch-task     the same for the microbatch gate
    $$streaming-facts     the current facts of the streaming layers, for
                          the read path (`softland.store.read-path`):
                          select and push, with a partitioning key
    $$microbatch-facts    the current facts of the microbatch layers, the
                          same rows on every task, for the read path
    $$streaming-facts-by-value
                          the facts of the streaming layers in the order of
                          their values, for the read path: select and push,
                          with a partitioning key
    $$microbatch-facts-by-value
                          the same order for the microbatch layers, the
                          same rows on every task

  The name of the module, for a client, is `(get-module-name Store)`.

  `add-gates-and-reads` adds everything of the module but its two depots,
  and `Store` is those two depots and that one call; so a second module
  that has a depot, or a mirror of one, under each of the two names runs
  the same gates and reads by this one definition.

  State. The module has two depots, and this namespace declares both. Each
  gate owns the four PStates of its record: the streaming gate
  (`softland.store.module.streaming-gate`) those of the streaming layers,
  the microbatch gate (`softland.store.module.microbatch-gate`) those of
  the microbatch layers. The queries (`softland.store.module.door-reads`)
  own nothing.

  Each depot holds every thing sent for a layer whose depot it is, as it
  was sent, with the time that the door wrote on it. What a gate decided of
  a thing is in the gate's tables and in no depot. A gate decides from the
  thing and from its own rows, which is what lets a second module over the
  two depots, read from their beginning, make the same tables; each gate's
  namespace says how far that holds. No topology appends to any depot:
  each depot holds what clients appended, and nothing else. No depot is
  ever trimmed: the module sets no trimming option, and Rama keeps every
  record by default. Every value is plain on a depot: a client of the
  cluster that reads one, or a copy of its log taken alone, holds every
  value that was sent.

  How a depot partitions. Both depots partition by the hash of the thing's
  `:layer`. The acts of one layer depend on each other's order (a give and
  the first act under it, two acts that replace one fact), and one
  partition for a layer gives them the depot's order: a layer is one line,
  the base too. The partition's task is the layer's task. In a streaming
  layer every row of the layer lives there, so the gate reaches the record
  with no hop. In a microbatch layer that task decides each act of the
  layer and holds its answers, and the gate copies the rows of its facts
  from there to every task. The partitioner applies the keyword `:layer`
  to the thing. On a thing that is no map a keyword gives nil and does not
  throw. One kind of map is the exception: a sorted map whose keys do not
  compare with a keyword, one with string keys for one, throws when a
  keyword is looked up in it. The partitioner runs on the appending
  client, so that throw is the client's, and no thing can stop a partition
  through it. No run here made such an append.

  What an ack says. A client appends to the streaming depot with the ack
  level :ack: the append returns when the gate has processed the thing and
  its rows are visible. An ack can fail for an act that the gate accepted,
  so the ack is a signal; the sender reads the answer by act-id. An ack of
  the microbatch depot says that the depot holds the thing and nothing
  about the decision: the sender reads the answer by act-id until one is
  there, which is when the batch that decided the thing is committed on
  the layer's task."
  (:require [com.rpl.rama :refer :all]
            [softland.store.module.door-reads :as door-reads]
            [softland.store.module.microbatch-gate :as microbatch-gate]
            [softland.store.module.streaming-gate :as streaming-gate]))

(defn add-gates-and-reads
  "Adds everything of the module `Store` but its two depots. Takes a
  module's `topologies`, and nothing else: no part that it adds needs the
  module's `setup`, since none declares a depot or a mirror. Adds the
  streaming gate, the microbatch gate and the door's two reads, each by its
  own `add-to`. What must hold: the module has a depot, or a mirror of one,
  under each of the names `*streaming-depot` and `*microbatch-depot`, each
  partitioned by the thing's `:layer`. `Store` calls it behind the
  declarations of its two depots; a module that mirrors those two depots,
  with the same number of tasks, and calls it runs the same gates over
  them from their beginning, into tables of its own."
  [topologies]
  (streaming-gate/add-to topologies)
  (microbatch-gate/add-to topologies)
  (door-reads/add-to topologies))

;; The two depots and the one call. `defmodule` takes no docstring; it is
;; set below.
(defmodule Store [setup topologies]
  (declare-depot setup *streaming-depot (hash-by :layer))
  (declare-depot setup *microbatch-depot (hash-by :layer))
  (add-gates-and-reads topologies))

(alter-meta! #'Store assoc
             :doc "The store's module: the two depots of acts, `*streaming-depot` and
  `*microbatch-depot`, each partitioned by the hash of the thing's `:layer`,
  and the two gates and the door's two reads (`add-gates-and-reads`).")
