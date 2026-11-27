;; Before changing this file, read the plans of the parts again:
;; src/proposal/backend-on-rama-2026-10-02/store-module/PLAN-streaming-gate-and-record.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PLAN-the-bases-gate.md
;; src/proposal/backend-on-rama-2026-10-02/run-again/PART-A.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-what-the-gates-keep.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-base-for-a-reader.md
;; src/proposal/backend-on-rama-2026-10-02/reads/PLAN-read-path.md

(ns softland.store.module
  "The Rama module `Store`: its two depots of acts, and the call that adds
  the two gates and the three query reads. It requires Rama. For a client,
  the name of the module is `(get-module-name Store)`.

  `add-gates-and-reads` adds everything but the two depots, so a second
  module that has a depot, or a mirror of one, under each of the two names
  runs the same gates and reads by this one definition.

  For an operator, the strays: a select of
  `[:strays (keypath word) (view count)]` on `$$streaming-task` or
  `$$microbatch-task`, with a lane as the partitioning key, gives the
  number of different strays of that word on that lane's task.

  The depots. Each holds every thing sent for a lane whose depot it is, as
  it was sent, with the time that the door wrote on it. What a gate decided
  of a thing is in the gate's tables and in no depot. No topology appends
  to a depot, and no depot is trimmed: the module sets no trimming option,
  and Rama keeps every record by default. A gate decides from the thing and
  from its own rows, so a second module over the two depots, read from
  their beginning, makes the same tables; each gate's namespace says how
  far that holds. Every value is plain on a depot: a client of the cluster
  that reads one, or a copy of its log taken alone, holds every value that
  was sent.

  Both depots partition by the hash of the thing's `:lane`. The acts of one
  lane depend on each other's order (a give and the first act under it,
  two acts that replace one fact), and one partition for a lane gives them
  the depot's order: a lane is one line, the base too. The partition's task
  is the lane's task.

  The partitioner applies the keyword `:lane` to the thing. On a thing that
  is no map a keyword gives nil and does not throw. A sorted map whose keys
  do not compare with a keyword, one with string keys for one, throws when
  a keyword is looked up in it. The partitioner runs on the appending
  client, so that throw is the client's, and no thing can stop a partition
  through it. No run here made such an append.

  What an ack says. An append to the streaming depot with the ack level
  :ack returns when the gate has processed the thing, and the ack can fail
  for an act that the gate accepted. An ack of the microbatch depot says
  that the depot holds the thing and nothing about the decision. So an ack
  is a signal, and the sender reads the answer by act-id; each gate's
  namespace says when the answer is there."
  (:require [com.rpl.rama :refer :all]
            [softland.store.module.door-reads :as door-reads]
            [softland.store.module.microbatch-gate :as microbatch-gate]
            [softland.store.module.streaming-gate :as streaming-gate]))

(defn add-gates-and-reads
  "Takes `topologies` alone: no part that it adds needs the module's
  `setup`, since none declares a depot or a mirror. What must hold: the
  module has a depot, or a mirror of one, under each of the names
  `*streaming-depot` and `*microbatch-depot`, each partitioned by the
  thing's `:lane`. A module that mirrors those two depots, with the same
  number of tasks, and calls it runs the same gates over them from their
  beginning, into tables of its own."
  [topologies]
  (streaming-gate/add-to topologies)
  (microbatch-gate/add-to topologies)
  (door-reads/add-to topologies))

;; `defmodule` takes no docstring; it is set below.
(defmodule Store [setup topologies]
  (declare-depot setup *streaming-depot (hash-by :lane))
  (declare-depot setup *microbatch-depot (hash-by :lane))
  (add-gates-and-reads topologies))

(alter-meta! #'Store assoc
             :doc "The store's module: the two depots of acts, the two gates and the
  three query reads.")
