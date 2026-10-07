(ns softland.store.record-pstates
  "The record as PStates: the four schemas, written once. It requires Rama,
  and it declares nothing: a gate declares four PStates with these schemas
  under its own names, because in Rama only the topology that owns a PState
  can write it. So \"the same record shape\" for each gate is these four
  functions.

  What each schema holds, field by field, is in the namespace docstring of
  `softland.store.record`, which makes and reads the rows. Here is why each
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

  Why three flat maps and not one PState keyed by the layer. An answer is
  found by an act's uuid, a fact by its place, a lock by its id: they share
  no key. With the layer as the top-level key and three subindexed maps
  under it, each row would cost two seeks to read and one more to write;
  the decision of one common act would cost 6 to 8 seeks, against 4 here.
  The rows of one layer are on one task all the same: a row is placed by the
  task on which its gate writes it, which is the layer's task, not by a hash
  of its whole key. So nothing may route by a whole key: each read routes by
  the layer.

  No row holds a collection, and no position is of the class Object. Each
  row is a fixed-keys schema, each field of one class; a field that a row
  does not have is absent. A row of another class throws at the write, so
  the rows are made only by `softland.store.record`.

  The strays. The outer map, a reason word to a set, is not subindexed: its
  keys are the words of `softland.store.declaration/stray-reasons`, a closed
  set, and nothing else writes it. Each inner set is subindexed, with size
  tracking on: it grows by one for each different stray, and its size is the
  count of the strays under that word. A set, not a counter: adding to a set
  is the same write however often Rama runs the event, and a counter would
  count a stray again at each retry.

  The last stamp is durable state because a stamp must increase through a
  restart and through a move of the task's leader. It is read from the
  PState at each decision and is not kept in the memory of a process.

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
