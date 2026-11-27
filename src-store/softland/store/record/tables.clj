(ns softland.store.record.tables
  "The record as PStates: the schemas, written once. It declares nothing: a
  gate declares seven PStates with these schemas under its own names, because
  in Rama only the topology that owns a PState can write it. Each is one
  schema for both gates, so one walk of one thing
  (`softland.store.module.walk`) reads and writes either gate's seven.

  Why flat maps and not one PState keyed by the lane. An answer is found by
  an act's uuid, a fact by its place, a row of the facts by value by the
  digest of its value, a permission by holder by its holder and sitting:
  they share no key. With the lane as the top-level key and subindexed maps
  under it, each row would cost two seeks to read and one more to write,
  where each costs one here.

  A row is placed by the task on which its gate writes it, not by a hash of
  its whole key. So nothing may route by a whole key. The streaming gate
  writes each row of a lane on the lane's task. The microbatch gate writes
  an answer, a lane's last stamp, its owners, its line, its things and its
  rows by who on the lane's task, and each row of the facts, of the facts by
  value, and of the permissions by holder on every task: a read of a
  microbatch lane's facts and rights finds each row on whichever task it
  runs. A read of an answer, or of the line, routes by the lane for either
  gate.

  No row holds a collection, and no position is of the class Object: each
  field has one class, and a field that a row does not have is absent. A
  fact's value, who wrote it, the permission that its act named and the
  stamps of the marked keys are each a String, the one text of what it is.
  A row of another class throws at the write, so the rows are made only by
  `softland.store.record.rows`. The one position that holds a collection
  is a thing's, in `things`."
  (:require [com.rpl.rama :refer [fixed-keys-schema map-schema set-schema]])
  (:import [clojure.lang IPersistentMap Keyword]
           [java.util UUID]))

(def ^:private byte-array-class
  (Class/forName "[B"))

(defn answers
  "A point lookup by the key of an act-id, once for every act, so a
  top-level map: Rama keeps each key of a top-level map as its own entry on
  disk, and a read is one seek. The digest is a byte array: no read is kept
  live on the answers."
  []
  {String (fixed-keys-schema {:refused Keyword
                              :stamp   Long
                              :time    Long
                              :digest  byte-array-class})})

(defn facts
  "The current fact of each place, one seek for a place. Entity and key are
  one sorted key, not a map inside a map, which would cost one seek for
  each level.

  `:value` is a String and no byte array: Rama's push on rows that hold a
  byte array sent the whole value anew about once a second, and the same
  rows with a String got one change for each write (run on Rama 1.6.0
  in-process; the cause was not found). A reader that is kept live reads
  these rows and the facts by value, so no field of either is a byte
  array."
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
  "For each key that a lane marks, the same facts in one more order, the
  entity last. So the places that hold one value under one key are
  neighbours, and \"what points at this thing\" is one seek and one step
  for each place.

  A top-level map and not a map inside a map: Rama's push can watch a run
  of a top-level map's keys, and a second level would cost one seek more
  for each read and each write. A table of its own and not more keys of
  `facts`: its key has five parts where a place has three, so in one map a
  key of this order could sort into the run of an entity."
  []
  {String (fixed-keys-schema {:by-act     UUID
                              :by-index   Long
                              :stamp      Long
                              :time       Long
                              :who        String
                              :permission String})})

(defn permissions-by-holder
  "For each permission that covers a lane's default group and has not been
  ended, its key by holder (`softland.store.record.rows/by-holder-key`) to
  the stamp of the last decision that gave it or changed it in place.
  Removed via `NONE>` on end. Top-level `{String Long}` so a holder-sitting
  prefix is one sorted range scan and one reactive push."
  []
  {String Long})

(defn line
  "One row for each decision, under \"<lane> <stamp>\" with the stamp in 19
  digits. So a lane's decisions are one run of neighbouring keys in the
  order of their stamps, and a page of n is one seek and n steps.

  A top-level map, as the facts: a read of a run of its keys is the form
  that Rama's push was seen to watch, and a second level would cost one
  seek more. No field is a byte array, the digest is hex text, should a
  read of the line be kept live."
  []
  {String (fixed-keys-schema {:act      UUID
                              :refused  Keyword
                              :time     Long
                              :digest   String
                              :who      String
                              :weight   Long
                              :not-kept Keyword})})

(defn things
  "The thing as it arrived, under the key of its line row, when the gate
  keeps it. A table of its own and not a field of the line row, although
  the two share a key and a partitioner: a value inside a top-level map is
  one serialised value, so a row that held its thing would load the thing
  at every read of the row. A read that withholds a decision, or that
  weighs a page before it takes its things, reads the row alone.

  The class is a Clojure map, which every thing that is no stray is
  (`softland.store.door.declaration/stray-reason`); a thing of another
  class throws at the write. What is inside has no schema: a thing can be
  anything."
  []
  {String IPersistentMap})

(defn task
  "What a gate keeps for a task beside its five tables: three things that
  are no row of a fact, of an answer or of the line, in one PState so that
  they cost one partition and not three.

  `:last-stamp`, by the printed text of a lane id
  (`softland.store.door.declaration/id-text`). One key for each lane, and
  not one number for the task, because a stamp is counted in its lane
  (`softland.store.gate.stamp`). The text, so that the position has one
  class: a lane id is a keyword or a UUID. A lane of which the gate has
  decided nothing on the task has no key. Subindexed, because a task of
  the streaming gate holds every lane that hashes to it, a personal lane
  for each person among them, and a map that is not subindexed is read and
  written whole at each decision. Size tracking is off: nothing asks how
  many lanes the map holds, and tracking costs a read at each write. The
  cost of the level: a read of one lane's last stamp goes through the
  field and then the lane's key; Rama's skill text counts each such level
  as a read on disk, and no run here measured it.

  A last stamp is durable state because a stamp must increase through a
  restart and through a move of the task's leader. Neither gate keeps one
  in the memory of a process.

  `:by-who`, from the key of a row by who
  (`softland.store.record.rows/by-who-key`) to the stamp of its decision:
  one write for each decision whose thing names a usable who, and a read
  of one who's decisions in one lane is a run of it. A field here and not
  a PState of its own: it is read only by an asker who may not see a lane,
  for her own things, so the one seek more that the field's level costs at
  that read saves a partition on every task.

  `:owners`, by the printed text of a lane id: the UUIDs of the owner
  permissions of that lane that stand on their own. Kept on the deciding
  task alone (zero `|all` copy). The outer map tracks no size; the inner set
  is subindexed with size tracking on so `[(keypath :owners lane-text)
  (view count)]` reads how many non-lent owners remain in O(1).

  `:strays`: by reason word, the digests, as hex text, of the strays seen
  on the task. The outer map is not subindexed: its keys are the words of
  `softland.store.door.declaration/stray-reasons`, a closed set. Each
  inner set is subindexed, with size tracking on: its size is the count of
  the strays under that word. A set, not a counter: adding to a set is the
  same write however often Rama runs the event, and a counter would count
  a stray again at each retry.

  Not checked here: a key of `:last-stamp` or `:owners` is a String of
  `softland.store.door.declaration/max-id-text` characters at most, which
  the lane of a thing that is no stray keeps; a key of `:by-who` is made
  of two such ids and a stamp."
  []
  (fixed-keys-schema {:last-stamp (map-schema String Long {:subindex-options {:track-size? false}})
                      :owners     (map-schema String (set-schema UUID {:subindex? true})
                                              {:subindex-options {:track-size? false}})
                      :by-who     (map-schema String Long {:subindex-options {:track-size? false}})
                      :strays     (map-schema Keyword (set-schema String {:subindex? true}))}))
