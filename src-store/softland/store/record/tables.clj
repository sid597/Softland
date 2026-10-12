(ns softland.store.record.tables
  "The record as PStates: the schemas, written once. It requires Rama, and
  it declares nothing: a gate declares five PStates with these schemas under
  its own names, because in Rama only the topology that owns a PState can
  write it. So \"the same record shape\" for each gate is these functions:
  the answers, the facts, the facts by value and the locks are one schema
  for both gates, and what a gate keeps for a task is `task` for the
  streaming gate and `microbatch-task` for the microbatch gate. One schema
  more is no part of a record: `batch-clock`, the clock that is kept for the
  newest batch of the microbatch gate, which the topology of
  `softland.store.module.batch-clock` declares and writes.

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
             order could sort into the run of an entity; and in a
             microbatch layer the two are placed by different parts.
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
             not later than this is of a closed batch, and the door's three
             reads and the streaming gate's question about the base give no
             other. `:turns`: the text of the microbatch id that the count
             belongs to, and how many things of that batch the task has
             given a turn. It is durable, and not a count in memory, because
             Rama sets a PState back when an attempt of a batch fails, so a
             second attempt counts from the same number. The text is beside
             the count so that the count starts again with each batch.
             `:last-stamp` is one for the whole gate, on the gate's task
             only.
    batch-clock
             {Keyword -> a row}, with one key, the depot word of the gate
             whose clock it is, `:microbatch`, and so one row: the batch
             that the clock belongs to, as the number of its microbatch id,
             and the clock in milliseconds. A read is one seek and a write
             one put. One row and not a row for each batch: a kept clock
             has one use, a later attempt of the same batch, and Rama
             starts no batch before the one before is committed on every
             task; so when a later batch keeps its clock, nothing can read
             the earlier one again. The depot of batch clocks keeps every
             record (`softland.store.module`). The row is not in the
             microbatch gate's own PStates because Rama sets those back
             when an attempt of a batch fails, and this row is what a
             second attempt must find.

  Why flat maps and not one PState keyed by the layer. An answer is found
  by an act's uuid, a fact by its place, a lock by its id: they share no
  key. With the layer as the top-level key and three subindexed maps
  under it, each row would cost two seeks to read and one more to write;
  the decision of one common act would cost 6 to 8 seeks, against 4 here.
  A row is placed by the task on which its gate writes it, not by a hash of
  its whole key. So nothing may route by a whole key. The streaming gate
  writes each row of a layer on the layer's task, and each read of a
  streaming layer routes by the layer. The microbatch gate writes a row on
  the task that the second part of its key gives: the uuid of an answer,
  the entity of a fact, the request's uuid of a lock; a row of the facts by
  value on the task of its value digest, the fourth part of its key, so
  that the run of one value is on one task; a row under one of the five
  store keys that sit on every task (`softland.store.gate.batch/on-every-task?`)
  on every task; and the whole row of a lock that seals a value on the task
  of that value's entity. A read of a microbatch layer routes by that part.
  `softland.store.record.rows` says it row by row.

  No row holds a collection, and no position is of the class Object. Each
  row is a fixed-keys schema, each field of one class; a field that a row
  does not have is absent: `:value` in the row of a retract, `:level-since`
  in every row but the one at a layer's marked-keys place. Who wrote a fact,
  the permission that its act named and the stamps of the marked keys are
  each held as a String, the one text of what it is. A row of another class throws at the write, so
  the rows are made only by `softland.store.record.rows`; the row of the
  kept clock, two numbers, is made only by
  `softland.store.gate.batch/clock-row-to-keep`.

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
  its current fact. No field names a lock: a value is kept plain."
  []
  {String (fixed-keys-schema {:value       byte-array-class
                              :by-act      UUID
                              :by-index    Long
                              :stamp       Long
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
                              :who        String
                              :permission String})})

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

(defn batch-clock
  "Gives the schema of the kept clock of a batch: the depot word of the
  gate, `:microbatch`, to one row, {:batch :clock}. `:batch` is the batch
  that the clock belongs to, as the number of its microbatch id; in `:turns`
  the same id is held as text. `:clock` is the clock time in milliseconds
  since 1970. What must hold of a row that is written: both are of the class
  Long, as `softland.store.gate.batch/clock-row-to-keep` makes them; a row of
  another class throws at the write. The table rests on one thing of Rama's:
  the id of a batch goes up and never comes again. If a microbatch topology
  were deleted and declared again under its name, its ids would start anew,
  the row would be of a later batch for ever, and no attempt could keep a
  clock (`softland.store.module.microbatch-gate/clock-must-be-kept`)."
  []
  {Keyword (fixed-keys-schema {:batch Long :clock Long})})
