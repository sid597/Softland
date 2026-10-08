(ns softland.store.record.tables
  "The record as PStates: the schemas, written once. It requires Rama, and
  it declares nothing: a gate declares four PStates with these schemas under
  its own names, because in Rama only the topology that owns a PState can
  write it. So \"the same record shape\" for each gate is these functions:
  the answers, the facts and the locks are one schema for both gates, and
  what a gate keeps for a task is `task` for the streaming gate and
  `microbatch-task` for the microbatch gate.

  What each schema holds, field by field, is in the namespace docstring of
  `softland.store.record.rows`, which makes and reads the rows. Here is why each
  PState has its shape.

    answers  {String -> a row}. A point lookup by the key of an act-id, once
             for every act, so a top-level map: Rama keeps each key of a
             top-level map as its own entry on disk, and a read is one seek.
    facts    {String -> a row}. The current fact of each place, one seek for
             a place. Entity and key are one sorted key, not a map inside a
             map, which would cost one seek for each level.
    locks    {String -> a row}. One row for each lock, keyed so that the
             locks of one request are neighbours: the check of one seal is
             one seek, and the locks of one request are one seek and one
             step for each lock.
    task     a fixed-keys value for each task: the last stamp that the gate
             gave on the task, and the strays seen on it. Two things that
             belong to a task and to no layer, in one PState so that they
             cost one partition and not two.
    microbatch-task
             the same for the microbatch gate, with two fields more and one
             changed. `:closed-through`: each row on the task with a stamp
             not later than this is of a closed batch, and a read gives no
             other. `:turns`: the text of the microbatch id that the count
             belongs to, and how many things of that batch the task has
             given a turn. It is durable, and not a count in memory, because
             Rama sets a PState back when an attempt of a batch fails, so a
             second attempt counts from the same number. The text is beside
             the count so that the count starts again with each batch.
             `:last-stamp` is one for the whole gate, on the gate's task
             only.

  Why three flat maps and not one PState keyed by the layer. An answer is
  found by an act's uuid, a fact by its place, a lock by its id: they share
  no key. With the layer as the top-level key and three subindexed maps
  under it, each row would cost two seeks to read and one more to write;
  the decision of one common act would cost 6 to 8 seeks, against 4 here.
  A row is placed by the task on which its gate writes it, not by a hash of
  its whole key. So nothing may route by a whole key. The streaming gate
  writes each row of a layer on the layer's task, and each read of a
  streaming layer routes by the layer. The microbatch gate writes a row on
  the task that the second part of its key gives: the uuid of an answer,
  the entity of a fact, the request's uuid of a lock; a row under `:class`
  or `:permission` on every task; and the whole row of a lock that seals a
  value on the task of that value's entity. A read of a microbatch layer
  routes by that part. `softland.store.record.rows` says it row by row.

  No row holds a collection, and no position is of the class Object. Each
  row is a fixed-keys schema, each field of one class; a field that a row
  does not have is absent. A row of another class throws at the write, so
  the rows are made only by `softland.store.record.rows`.

  The strays. The outer map, a reason word to a set, is not subindexed: its
  keys are the words of `softland.store.door.declaration/stray-reasons`, a closed
  set, and nothing else writes it. Each inner set is subindexed, with size
  tracking on: it grows by one for each different stray, and its size is the
  count of the strays under that word. A set, not a counter: adding to a set
  is the same write however often Rama runs the event, and a counter would
  count a stray again at each retry.

  The last stamp is durable state because a stamp must increase through a
  restart and through a move of the task's leader. The streaming gate reads
  it from the PState at each decision, the microbatch gate once for each
  batch; neither keeps it in the memory of a process.

  State and effects. None: each function gives a schema, a value. The
  PStates that a gate declares with them are durable state of the cluster."
  (:require [com.rpl.rama :refer [fixed-keys-schema map-schema set-schema]])
  (:import [clojure.lang Keyword]
           [java.util UUID]))

(def ^:private byte-array-class
  "The JVM's class of a byte array, the class of a schema position that holds
  a run of bytes."
  (Class/forName "[B"))

(defn answers
  "Gives the schema of the answers: the key of an act-id to the answer's row."
  []
  {String (fixed-keys-schema {:refused Keyword
                              :stamp   Long
                              :digest  byte-array-class})})

(defn facts
  "Gives the schema of the current facts: the key of a place to the row of
  its current fact."
  []
  {String (fixed-keys-schema {:value      byte-array-class
                              :lock-act   UUID
                              :lock-index Long
                              :by-act     UUID
                              :by-index   Long
                              :stamp      Long})})

(defn locks
  "Gives the schema of the locks: the key of a lock to its row."
  []
  {String (fixed-keys-schema {:bytes       byte-array-class
                              :way         Long
                              :seals-act   UUID
                              :seals-index Long})})

(defn task
  "Gives the schema of what a gate keeps for a task as a whole: its last
  stamp, and by reason word the digests, as hex text, of the strays seen."
  []
  (fixed-keys-schema {:last-stamp Long
                      :strays     (map-schema Keyword (set-schema String {:subindex? true}))}))

(defn microbatch-task
  "Gives the schema of what the microbatch gate keeps for a task as a whole:
  the strays seen on it, as in `task`; the stamp through which its rows are
  of closed batches; its count of turns, with the text of the microbatch id
  that the count belongs to; and, on the gate's task only, the last stamp
  that the gate gave."
  []
  (fixed-keys-schema {:strays         (map-schema Keyword (set-schema String {:subindex? true}))
                      :closed-through Long
                      :turns          (fixed-keys-schema {:batch String :given Long})
                      :last-stamp     Long}))
