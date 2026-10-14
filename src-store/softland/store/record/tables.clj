(ns softland.store.record.tables
  "The record as PStates: the schemas, written once. It requires Rama, and
  it declares nothing: a gate declares four PStates with these schemas under
  its own names, because in Rama only the topology that owns a PState can
  write it. So \"the same record shape\" for each gate is these four
  functions: the answers, the facts, the facts by value, and `task`, what a
  gate keeps for a task. Each is one schema for both gates, so one walk of
  one thing (`softland.store.module.walk`) reads and writes either gate's
  four.

  What each schema holds, field by field, is in the namespace docstring of
  `softland.store.record.rows`, which makes and reads the rows. Here is why each
  PState has its shape.

    answers  {String -> a row}. A point lookup by the key of an act-id, once
             for every act, so a top-level map: Rama keeps each key of a
             top-level map as its own entry on disk, and a read is one seek.
    facts    {String -> a row}. The current fact of each place, one seek for
             a place. Entity and key are one sorted key, not a map inside a
             map, which would cost one seek for each level.
    facts-by-value
             {String -> a row}. For each key that a layer marks, the same
             facts in one more order: layer, key, the stamp since which the
             key is marked, the digest of the value, then the entity. So
             the places that hold one value under one key are neighbours,
             and \"what points at this thing\" is one seek and one step for
             each place. A top-level map and not a map inside a map: Rama's
             push can watch a run of a top-level map's keys, and a second
             level would cost one seek more for each read and each write. A
             table of its own and not more keys of `facts`: its key has five
             parts where a place has three, so in one map a key of this
             order could sort into the run of an entity.
    task     a fixed-keys value for each task, what a gate keeps beside
             its three tables: under `:last-stamp`, for each layer that the
             gate has decided a thing of on the task, the last stamp that
             it gave in that layer; and the strays seen on the task. Two
             things that are no row of a fact or of an answer, in one
             PState so that they cost one partition and not two.
             `:last-stamp` is a map from the printed text of a layer id to
             a number, subindexed, with size tracking off. A map with one
             key for each layer, and not one number for the task, because
             a stamp is counted in its layer
             (`softland.store.gate.stamp`). The key is the text of the
             layer id, as in every key of a row, so that the position has
             one class: a layer id is a keyword or a
             UUID. Subindexed, because a task of the streaming gate holds
             every layer that hashes to it, a personal layer for each
             person among them, and a map that is not subindexed is read
             and written whole at each decision. Size tracking is off:
             nothing asks how many layers the map holds, and tracking
             costs a read at each write. The cost of the level: a read of
             one layer's last stamp goes through the field and then the
             layer's key, where a row of the three tables is one key;
             Rama's skill text counts each such level as a read on disk,
             and no run here measured it.

  Why flat maps and not one PState keyed by the layer. An answer is found
  by an act's uuid, a fact by its place, a row of the facts by value by the
  digest of its value: they share no key. With the layer as the top-level
  key and three subindexed maps under it, each row would cost two seeks to
  read and one more to write, where each costs one here.

  A row is placed by the task on which its gate writes it, not by a hash of
  its whole key. So nothing may route by a whole key. The streaming gate
  writes each row of a layer on the layer's task, and each read of a
  streaming layer routes by the layer. The microbatch gate writes an
  answer and a layer's last stamp on the layer's task, and each row of the
  facts and of the facts by value on every task: a read of an answer
  routes by the layer, and a read of a microbatch layer's facts finds each
  row on whichever task it runs. `softland.store.record.rows` says it row
  by row.

  No row holds a collection, and no position is of the class Object. Each
  row is a fixed-keys schema, each field of one class; a field that a row
  does not have is absent: `:value` in the row of a retract, `:level-since`
  in every row but the one at a layer's marked-keys place, `:time` in the
  answer of a thing that held no usable time. A fact's value, who wrote
  it, the permission that its act named and the stamps of the
  marked keys are each held as a String, the one text of what it is: the
  text of a value is the one that the declaration's check made
  (`softland.store.door.declaration/check`). The two tables that a reader is kept
  live on, the facts and the facts by value, hold no byte array; the answers
  do, and no read is kept live on them. A row of another class throws at
  the write, so the rows are made only by `softland.store.record.rows`.

  The strays. The outer map, a reason word to a set, is not subindexed: its
  keys are the words of `softland.store.door.declaration/stray-reasons`, a closed
  set, and nothing else writes it. Each inner set is subindexed, with size
  tracking on: it grows by one for each different stray, and its size is the
  count of the strays under that word. A set, not a counter: adding to a set
  is the same write however often Rama runs the event, and a counter would
  count a stray again at each retry.

  A last stamp is durable state because a stamp must increase through a
  restart and through a move of the task's leader. The walk reads the last
  stamp of the act's layer from the gate's PState at each decision, in both
  gates; neither gate keeps one in the memory of a process.

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
                              :time    Long
                              :digest  byte-array-class})})

(defn facts
  "Gives the schema of the current facts: the key of a place to the row of
  its current fact. A value is kept plain: `:value` is the one text of the
  value in its normal form (`softland.store.record.rows/fact-row`). It
  is a String and no byte array: Rama's push on rows that hold a
  byte array sent the whole value anew about once a second, and the same
  rows with a String got one change for each write (run on Rama 1.6.0
  in-process; the cause was not found). A reader that is kept live reads
  these rows, so the row takes the class that a push can tell a change of,
  and no field of a table that a reader is kept live on is a byte array."
  []
  {String (fixed-keys-schema {:value       String
                              :by-act      UUID
                              :by-index    Long
                              :stamp       Long
                              :time        Long
                              :who         String
                              :permission  String
                              :level-since String})})

(defn facts-by-value
  "Gives the schema of the facts by value: the key of a place in the order
  of its value, under a marked key, to the fact's row without its value.
  One schema for both gates."
  []
  {String (fixed-keys-schema {:by-act     UUID
                              :by-index   Long
                              :stamp      Long
                              :time       Long
                              :who        String
                              :permission String})})

(defn task
  "Gives the schema of what a gate keeps for a task beside its three
  tables. It serves both gates: the streaming gate declares
  `$$streaming-task` with it and the microbatch gate `$$microbatch-task`.
  `:last-stamp`: a map from the printed text of a layer id
  (`softland.store.door.declaration/id-text`) to the last stamp that the
  gate gave in that layer; a layer of which the gate has decided nothing on
  the task has no key. `:strays`: by reason word, the digests, as hex text,
  of the strays seen on the task.

  The map of `:last-stamp` is subindexed, with size tracking off. The
  choice and its reasons are in the namespace docstring: a task can hold
  very many layers, and the gate reads and writes one layer's number at
  each decision. What must hold of what is written: the key is a String of
  `softland.store.door.declaration/max-id-text` characters at most, which
  the layer of a thing that is no stray keeps, and the value is a Long; a
  value of another class throws at the write."
  []
  (fixed-keys-schema {:last-stamp (map-schema String Long {:subindex-options {:track-size? false}})
                      :strays     (map-schema Keyword (set-schema String {:subindex? true}))}))
