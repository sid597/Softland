(ns softland.store.record.tables
  "The record as PStates: the schemas, written once. It requires Rama, and
  it declares nothing: a gate declares four PStates with these schemas under
  its own names, because in Rama only the topology that owns a PState can
  write it. So \"the same record shape\" for each gate is these functions:
  the answers, the facts and the facts by value are one schema
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
    task     a fixed-keys value for each task, what the streaming gate
             keeps beside its three tables: under `:last-stamp`, for each
             layer that the gate has decided a thing of on the task, the
             last stamp that it gave in that layer; and the strays seen on
             the task. Two things that are no row of a fact or of an
             answer, in one PState so that they cost one partition and not
             two. `:last-stamp` is a map from the printed text of a layer
             id to a number, subindexed, with size tracking off. A map with
             one key for each layer, and not one number for the task,
             because a stamp is counted in its layer (Sid, 13 October
             2026). The key is the text of the layer id, as in every key of
             a row, so that the position has one class: a layer id is a
             keyword or a UUID. Subindexed, because a task holds every
             layer that hashes to it, a personal layer for each person
             among them, and a map that is not subindexed is read and
             written whole at each decision. Size tracking is off: nothing
             asks how many layers the map holds, and tracking costs a read
             at each write. The cost of the level: a read of one layer's
             last stamp goes through the field and then the layer's key,
             where a row of the three tables is one key; Rama's skill text
             counts each such level as a read on disk, and no run here
             measured it.
    microbatch-task
             what the microbatch gate keeps for a task: the strays as in
             `task`, four fields more, and a `:last-stamp` that is one
             number. `:closed-through`: each row on the task with a stamp
             not later than this is of a closed batch, and the door's two
             reads give no other. `:turns`: the text of the microbatch id that the count
             belongs to, and how many things of that batch the task has
             given a turn. It is durable, and not a count in memory, because
             Rama sets a PState back when an attempt of a batch fails, so a
             second attempt counts from the same number. The text is beside
             the count so that the count starts again with each batch.
             `:last-stamp` is one for the whole gate, on the gate's task
             only.
             `:to-keep`: what a batch decided and the gate's depot of
             answers does not hold yet, on the batch's keeping task. A
             table of the gate and no memory, because Rama sets it back
             with the answers' rows when an attempt fails: an entry and
             its answer's row are committed together or not at all. A
             subindexed map from a text to a value: the batch's id in 19
             digits, the task that sent the value in 5 and a part number
             in 5, with a space between. So the keys of one batch are
             neighbours and one range read gives them, two tasks never
             write one key, and a value is read once and taken out. A
             text and no packed number, since the id of a batch goes up
             for as long as the module lives. Size tracking is off:
             nothing asks the map's size, and tracking costs a read at
             each write. A value holds the batch's id and two vectors,
             the answers and the strays, 256 entries at most between them
             by the code that makes it, so the vectors are small and are
             not subindexed. An answer is held by the text of its layer
             and its uuid, with its stamp, its digest and the word of a
             refusal: each position has one class. A value for each task
             and each 256 entries, and not a key for each answer, which
             would cost one put and one removal for every answer of a
             batch.
             `:to-close`: on the gate's task only, the id of the newest
             batch that left something to keep, with the batch's clock
             when it made a stamp. It is the mark by which the next run
             knows that it closes a batch, and where the closing takes
             the batch's clock from. It is with the gate's last stamp,
             which the closing needs with it.
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
  by an act's uuid, a fact by its place, a row of the facts by value by the
  digest of its value: they share no
  key. With the layer as the top-level key and three subindexed maps
  under it, each row would cost two seeks to read and one more to write,
  where each costs one here.
  A row is placed by the task on which its gate writes it, not by a hash of
  its whole key. So nothing may route by a whole key. The streaming gate
  writes each row of a layer on the layer's task, and each read of a
  streaming layer routes by the layer. The microbatch gate writes a row on
  the task that the second part of its key gives: the uuid of an answer,
  the entity of a fact; a row of the facts by
  value on the task of its value digest, the fourth part of its key, so
  that the run of one value is on one task; and a row under one of the five
  store keys that sit on every task (`softland.store.gate.batch/on-every-task?`)
  on every task. A read of a microbatch layer routes by that part.
  `softland.store.record.rows` says it row by row.

  No row holds a collection, and no position is of the class Object. Each
  row is a fixed-keys schema, each field of one class; a field that a row
  does not have is absent: `:value` in the row of a retract, `:level-since`
  in every row but the one at a layer's marked-keys place. A fact's value,
  who wrote it, the permission that its act named and the stamps of the
  marked keys are each held as a String, the one text of what it is: the
  text of a value is the one that the declaration's check made
  (`softland.store.door.declaration/check`). The two tables that a reader is kept
  live on, the facts and the facts by value, hold no byte array; the answers
  do, and so does a value of `:to-keep`, and no read is kept live on either.
  A row of another class throws at the write, so
  the rows are made only by `softland.store.record.rows`; the row of the
  kept clock, two numbers, is made only by
  `softland.store.gate.batch/clock-row-to-keep`; and what the microbatch
  gate writes under `:to-keep` and `:to-close` only by
  `softland.store.gate.batch/rows-to-keep` and
  `softland.store.gate.batch/to-close-of`.

  The strays. The outer map, a reason word to a set, is not subindexed: its
  keys are the words of `softland.store.door.declaration/stray-reasons`, a closed
  set, and nothing else writes it. Each inner set is subindexed, with size
  tracking on: it grows by one for each different stray, and its size is the
  count of the strays under that word. A set, not a counter: adding to a set
  is the same write however often Rama runs the event, and a counter would
  count a stray again at each retry.

  A last stamp is durable state because a stamp must increase through a
  restart and through a move of the task's leader. The streaming gate reads
  the last stamp of the act's layer from the PState at each decision, the
  microbatch gate its one last stamp once for each batch; neither keeps one
  in the memory of a process.

  State and effects. None: each function gives a schema, a value. The
  PStates that a gate declares with them are durable state of the cluster."
  (:require [com.rpl.rama :refer [fixed-keys-schema map-schema set-schema vector-schema]])
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

(defn task
  "Gives the schema of what the streaming gate keeps for a task beside its
  three tables. `:last-stamp`: a map from the printed text of a layer id
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

(defn microbatch-task
  "Gives the schema of what the microbatch gate keeps for a task as a whole:
  the strays seen on it, as in `task`; the stamp through which its rows are
  of closed batches; its count of turns, with the text of the microbatch id
  that the count belongs to; on the gate's task only, the last stamp that
  the gate gave, and `:to-close`, the batch that the next run of the gate
  closes, with its clock when it made a stamp; and, on the keeping task of
  a batch, `:to-keep`: under each key of
  `softland.store.gate.batch/to-keep-key` the answers and the strays that
  one task sent to keep of that batch, until the next run has appended
  them. The map of `:to-keep` is subindexed, with size tracking off; the
  two vectors of a value are not, since
  `softland.store.gate.batch/max-kept` bounds them. What must hold of what
  is written: a `:to-close` is `softland.store.gate.batch/to-close-of`'s
  and a value of `:to-keep` is `softland.store.gate.batch/rows-to-keep`'s;
  a value of another class throws at the write."
  []
  (fixed-keys-schema {:strays         (map-schema Keyword (set-schema String {:subindex? true}))
                      :closed-through Long
                      :turns          (fixed-keys-schema {:batch String :given Long})
                      :last-stamp     Long
                      :to-keep        (map-schema
                                       String
                                       (fixed-keys-schema
                                        {:batch   Long
                                         :answers (vector-schema
                                                   (fixed-keys-schema {:layer   String
                                                                       :uuid    UUID
                                                                       :stamp   Long
                                                                       :digest  byte-array-class
                                                                       :refused Keyword}))
                                         :strays  (vector-schema
                                                   (fixed-keys-schema {:stray  Keyword
                                                                       :digest String
                                                                       :task   Long}))})
                                       {:subindex-options {:track-size? false}})
                      :to-close       (fixed-keys-schema {:batch Long :clock Long})}))

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
