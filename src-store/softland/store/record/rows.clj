(ns softland.store.record.rows
  "The record's rows: the text of each key, the making of each row, and the
  reading of each row. Pure code. It needs no cluster, and it holds no
  record: the record is in a gate's PStates
  (`softland.store.record.tables`), and a gate reads and writes it.

  The record has three kinds of row, each under a string key that begins
  with its layer, and one value for each task.

    an answer   key \"<layer> <act uuid>\"
                {:refused the reason word, absent when the act was accepted
                 :stamp   the stamp of the decision
                 :digest  32 bytes, the digest of the thing that was decided}
    a fact      key \"<layer> <entity> <key>\": the current fact of a place
                {:value       the value, plain, as a String: the one text of
                              the value in its normal form
                              (`softland.store.door.text`), as the
                              declaration's check made it
                              (`softland.store.door.declaration/check`).
                              Absent for a retract
                 :by-act      the fact that wrote this row, as the uuid of
                 :by-index    its act and its index in that act
                 :stamp       the stamp of that act's decision
                 :who         the one text of the act's `:who`
                 :permission  the one text of the permission that the act
                              named
                 :level-since only in the row of the fact under
                              `:softland.store/marked-keys`: the one text of
                              a map from each marked key to the stamp since
                              which its order by value is level}
    a fact by value
                key \"<layer> <key> <since> <value digest> <entity>\": one
                place under a marked key, in the order of its value
                {:by-act :by-index :stamp :who :permission}: the fact's row
                without its value. The value is the one whose digest the key
                holds

  Every accepted fact is a row of the second kind, under a store key or a
  plain key: the depot of a layer at
  \"<layer> <layer> :softland.store/depot\", a permission at
  \"<layer> <holder> :softland.store/permission\", and at the layer's own
  entity who may see it, what it builds on and its marked keys. A value is
  held as one String, its one text, so the field has one class, a push on
  the row is told each change of it, and \"is this the permission that the
  act names\" is a comparison of two Strings that are equal only when the
  values are. `plain-bytes` gives the plain bytes of a value, the UTF-8 of
  that String, over which the digest of the order by value is taken. Why
  the field is a String and no byte array is with its schema
  (`softland.store.record.tables/facts`).

  The third kind, the facts by value. A layer marks a key by a fact
  (`softland.store.door.declaration/store-keys`). For a marked key a gate
  keeps each fact that has a value in this order too, so that every place
  that holds one value under one key is a run of neighbouring keys: the keys
  that begin with \"<layer> <key> <since> <value digest> \" (`by-value-run`).
  The value digest is the SHA-256 of the plain bytes of the row's value, as 64 hex
  characters: a key then has a bound whatever the value is, a value's text
  may hold a space, and two values share a run only by a collision of the
  hash. <since> is the stamp that the marks row gives for the key
  (`marked-keys-of`): the order of a marked key holds every fact under that
  key whose stamp is later than it, and no other. A mark that is taken off
  and put on again gets another stamp, so it names other keys, and the rows
  of the earlier marking stay where they are and no read names them. A
  retract has no row in this order.

  Where a row lives. The rows are the same for both gates; the task is not.
  A row of a streaming layer is on the layer's task. A row of a microbatch
  layer is on the task that one part of its key gives: an answer on the task
  of the act's uuid, a fact on the task of its entity, a fact by value on
  the task of its value digest (`by-value-route`), so that the run of one
  value is on one task. A fact of
  a microbatch layer under `:softland.store/depot`,
  `:softland.store/permission`, `:softland.store/who-may-see`,
  `:softland.store/builds-on` or `:softland.store/marked-keys` is on every
  task. No row names a task.

  Why the keys are strings. A key is a pair or a triple of ids, and an id is
  a keyword or a UUID: two classes. A string made of the parts' printed
  texts is one class, equal keys are equal strings, and Rama gives the order
  of string keys, on which the run of one value in the facts by value, and
  each run of places that a read names, rest. No
  printed text of an id holds a space, so one space parts the parts. The
  layer and the depot of an id are in no row: the key says the layer, and
  the gate that owns the PState says the depot.

  The functions that make a row give exactly the classes that the schemas
  declare: a Long, a UUID, a Keyword, a String, a byte array. A row of another class
  throws in a topology, and a throw there stops the partition. So a gate
  writes no row that these functions did not make.

  What a depot of answers holds. Each gate also appends what it decided to
  a depot of its own: a log from which, with the depot of acts, the tables
  can be made again. The form of what such a log holds is made here, for
  both gates:

    an answer's entry  {:act-id [layer depot uuid] :stamp :digest}, and
                       `:refused` with the word for a refusal
                       (`answer-record`): the answer's row with the act-id
                       beside it, and no value of a fact
    a stray's entry    {:stray the word, :digest the digest as hex text,
                       :task the task on which it arrived}
                       (`stray-record`), one for each arrival
    a record           a map of entries: `:answers` and `:strays`, each a
                       vector, each there only when it holds an entry
                       (`log-record`). The microbatch gate adds what a
                       batch keeps of itself (`softland.store.gate.batch`)

  Inputs and results, by group:

    answer-key, place-key, by-value-key                -> the text of a key
    answer-record, stray-record, log-record
                                      an answer's row with its act-id, a
                                      stray, or entries -> an entry, or a
                                      record, of a depot of answers
    by-value-run                      a layer, a key, a stamp, a value
                                      -> the two ends of that value's run
    entity-run, own-rows-run, run-of-ends
                                      a layer, and an entity or two ends
                                      -> the two ends of a run of places
    before?                           two texts -> whether the first is
                                      before the second in the table's order
    plain-bytes                       the String that a fact's row holds for
                                      its value -> the value's plain bytes
    value-digest, value-digest-of     bytes, or a value -> 64 hex characters
    place-of-key, by-value-entity, by-value-route, id-of-text,
    answer-key-parts                  the text of a key -> its parts
    answer-row, fact-row, with-level-since, by-value-row
                                      -> a row to write
    answer-of, written-by?, holds-value?, value-of, read-value,
    plain-bytes-of, who-of,
    permission-of, marked-keys-of, closed?             a row -> what it says
    same-row?, holds-this-value?      a row, and a row or a value
                                      -> true or false
    routing-id                        an act-id -> the first argument of a
                                      read by that act-id
    answer-read, depot-read, answer-read-by-routing-id
                                      what a caller gave a read
                                      -> the keys to read, or nil

  State and effects. None. Each function is pure.

  Callers. `softland.store.gate.decision`, `softland.store.gate.batch`, the
  streaming gate, and `softland.store.module.door-reads`. The door (`softland.store.door`) and
  the proof's hand call `routing-id` and the `-read` functions to test what
  they pass before they call a query. `read-value`, `who-of`,
  `permission-of`, `marked-keys-of`, `by-value-run`, `by-value-entity`,
  `place-of-key` and `value-digest-of` are for a reader of the tables; no
  gate calls `read-value`, `who-of` or `permission-of`. The reader is the
  read path: its pure namespaces under `softland.store.read-path` call
  those, and six that only a read needs, which no gate calls:
  `entity-run`, `own-rows-run`, `run-of-ends`, `before?`, `same-row?` and
  `holds-this-value?`.

  What must hold before a key is made. The functions that make a key, or
  the two ends of a run, test nothing of what they are given. A thing that
  is no id can print with a space in it, and a key made of it can name a row
  of another place. So a caller that holds what somebody gave it tests
  first: each id with `softland.store.door.declaration/usable-id?`, a fact's
  key as a readable keyword within the limit, a value with
  `softland.store.act/value?`. A gate makes keys only of an act that passed
  the declaration."
  (:require [clojure.edn :as edn]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util Arrays UUID]))

;; ----------------------------------------------------------------- the keys

(defn answer-key
  "Takes a layer id and the uuid of an act-id. Gives the key of that act's
  answer: \"<layer> <uuid>\". The depot slot of the act-id is not in it: a
  gate answers only under an act-id of its own depot. The layer is in it,
  although the uuid alone is random, so that a uuid used in two layers of
  one task is two rows. What must hold: the layer is an id within the
  limit."
  [layer uuid]
  (str (declaration/id-text layer) " " uuid))

(defn place-key
  "Takes a layer id, an entity id and a fact's key. Gives the key of that
  place: \"<layer> <entity> <key>\". The keys of one entity are neighbours in
  the order, and the entities of one layer too. What must hold: each of the
  three is within the limit."
  [layer entity k]
  (str (declaration/id-text layer) " " (declaration/id-text entity) " " (declaration/id-text k)))

;; ------------------------------------------------------------------ answers

(defn answer-row
  "Takes the reason word of a refusal, or nil for an accepted act; the stamp
  of the decision; and the digest of the thing, 32 bytes. Gives the answer's
  row. An accepted act has no `:refused`: there is one row for every act for
  ever, so the row keeps the smaller form."
  [refused stamp digest]
  (cond-> {:stamp (long stamp) :digest digest}
    refused (assoc :refused refused)))

(defn answer-of
  "Takes an answer's row, or nil. Gives the answer as a caller gets it:
  {:verdict :accepted or :refused, :reason the word or nil, :stamp the
  stamp, :digest the digest as hex text}; or nil for no row, which is
  \"nothing yet\"."
  [row]
  (when row
    {:verdict (if (:refused row) :refused :accepted)
     :reason  (:refused row)
     :stamp   (:stamp row)
     :digest  (text/hex (:digest row))}))

;; ---------------------------------------------- what a depot of answers holds

(defn answer-record
  "The entry of one answer in a gate's depot of answers. Takes the depot
  word of the gate that gave the answer; the layer and the uuid of the
  act-id under which the thing arrived; and the answer's row, as
  `answer-row` makes it or as the gate read it. Gives
  {:act-id [layer depot uuid] :stamp :digest}, and `:refused` with the word
  when the row holds one: the answer's row with the act-id beside it, and
  no value of a fact. The digest is the row's, 32 bytes. The act-id is held
  as data and not as the text of the answer's key, so that a reader of the
  log makes the key with the `answer-key` of its own day. A gate answers
  only under an act-id of its own depot, so the gate's depot word is the
  depot slot of the act-id. Of the row it reads `:stamp`, `:digest` and
  `:refused`, and nothing else. It never throws. What must hold: the row is
  an answer's row."
  [gate-depot layer act-uuid row]
  (cond-> {:act-id [layer gate-depot act-uuid]
           :stamp  (:stamp row)
           :digest (:digest row)}
    (:refused row) (assoc :refused (:refused row))))

(defn stray-record
  "The entry of one arrival of a stray in a gate's depot of answers. Takes
  the stray's word (`softland.store.door.declaration/stray-reason`); the
  digest of the thing as hex text, as the set of that word holds it
  (`softland.store.door.digest/hex-of`); and the number of the task on which
  the stray arrived, which is the task of that set. Gives
  {:stray :digest :task}, the task as a long. The task is in the entry
  because the partition of a record need not be the task of the set: the
  microbatch gate keeps a batch on one task. What must hold: the task is an
  integer."
  [word digest-hex task]
  {:stray word :digest digest-hex :task (long task)})

(defn log-record
  "A record of a depot of answers: a map of entries. Takes answer records
  (`answer-record`) and stray records (`stray-record`), each a sequence or
  nil. Gives a map with `:answers`, a vector, when there is an answer
  record, and `:strays`, a vector, when there is a stray record. A part
  with nothing in it is absent, so the record of one answer is
  {:answers [entry]} and the record of one stray {:strays [entry]}. The
  streaming gate appends what this gives for one entry. The microbatch gate
  adds what a batch keeps of itself
  (`softland.store.gate.batch/records-of-closing`)."
  [answer-records stray-records]
  (cond-> {}
    (seq answer-records) (assoc :answers (vec answer-records))
    (seq stray-records) (assoc :strays (vec stray-records))))

;; -------------------------------------------------------------------- facts

(defn plain-bytes
  "Takes the String that a fact's row holds as its value. Gives the plain
  bytes of the value, as a new array: the UTF-8 of the String. In a row that
  a gate wrote the String is the one text of the value in its normal form,
  so these are the bytes of that text. What must hold: the thing is a
  String. Cost: one encoding of the String."
  [held]
  (.getBytes ^String held StandardCharsets/UTF_8))

(defn plain-bytes-of
  "Takes a fact's row or nil. Gives the plain bytes of the row's value
  (`plain-bytes`), or nil when there is no row or the row holds no value. A
  gate asks it of a row to take the digest of the row's value."
  [row]
  (when-let [held (:value row)]
    (plain-bytes held)))

(defn fact-row
  "Takes one fact of an act, in the act's one form on arrival; the uuid of
  the act's act-id; the fact's index in the act; the stamp of the act's
  decision; the one text of the act's `:who`; and the one text of the
  permission that the act named. Gives the row of the fact's place. Its
  `:value` is the fact's `:text`, a String: the one text of the value in its
  normal form, as the declaration's check made it
  (`softland.store.door.declaration/check`), under a plain key and under a
  store key alike. A fact with no value, a retract, has no `:text`: the row
  holds no value, and is there all the same, so that a later fact can
  replace the retract. Each row holds who wrote it and the permission that
  the act named, each as its one text: a UUID has two texts, and the one
  text reads back as a UUID. The two texts are given, and not made here, so
  that a caller makes them once for an act and not once for each fact.

  What must hold: the act passed the declaration's check, and the two texts
  are `softland.store.door.text/text` of its `:who` and of its
  `:permission`."
  [fact act-uuid index stamp who-text permission-text]
  (cond-> {:by-act act-uuid :by-index (long index) :stamp (long stamp)
           :who who-text :permission permission-text}
    (some? (:text fact))
    (assoc :value (:text fact))))

(defn with-level-since
  "Takes the row of a fact under `:softland.store/marked-keys`, as `fact-row`
  gives it, and a map from each key that the fact marks to the stamp since
  which the order of that key is level. Gives the row with `:level-since`,
  the one text of the map. A gate derives the map at the write
  (`softland.store.gate.decision/writes`): no maker can write a gate's
  stamp. `marked-keys-of` reads it back."
  [row since-by-key]
  (assoc row :level-since (text/text since-by-key)))

(defn written-by?
  "Takes the depot of the gate that owns the record, the layer, a fact's row
  or nil, and a fact id, [[layer depot uuid] index]. Gives true when the
  fact that wrote the row is the one named: the fact id is of this layer and
  this depot, and its uuid and index are the row's. False for no row, for a
  fact of another layer or depot, and for any other fact."
  [gate-depot layer row fact-id]
  (let [[[named-layer named-depot named-uuid] named-index] fact-id]
    (boolean (and row
                  (= layer named-layer)
                  (= gate-depot named-depot)
                  (= (:by-act row) named-uuid)
                  (= (:by-index row) named-index)))))

(defn holds-value?
  "Takes a fact's row or nil, and a value in its one normal form. Gives true
  when the row's value is that value: it compares the row's String with the
  one text of the value given, and two values of the form have one text
  only when they are one value. The row's value is not read as a form. The
  gates ask it of the row at a permission's place, where only a fact under
  that store key can be. Cost: one making of the value's text."
  [row v]
  (boolean (and row
                (some? (:value row))
                (= (:value row) (text/text v)))))

(defn- first-form
  "Takes a string. Gives the first EDN form that it holds, or nil when it
  holds none or does not read. It never throws. nil is also what the text
  `nil` reads as; a caller that must tell the two apart compares the text."
  [s]
  (try (edn/read-string s)
       (catch Throwable _ nil)))

(defn value-of
  "Takes a fact's row or nil. Gives the first form that the plain bytes of
  the row's value read as (`plain-bytes-of`), or nil when there is no row,
  the row holds no value, or its bytes do not read. It never throws.
  `softland.store.module.door-reads` asks it of the row at a depot's place
  and gets the depot word. It tests nothing of the form that it gives: a
  reader of a tool's fact calls `read-value`."
  [row]
  (when-let [plain (plain-bytes-of row)]
    (first-form (String. ^bytes plain StandardCharsets/UTF_8))))

(defn read-value
  "What a fact's row says of its value, for a reader of the tables. Takes a
  fact's row, or nil. Gives one of four:

    [:no-fact]       no row: no fact was ever accepted at the place
    [:nothing-here]  a row with no value: the current fact is a retract
    [:value v]       the value, as its maker gave it
    [:not-a-value]   the row's bytes are no value of the form

  The row's bytes are the plain bytes of its value (`plain-bytes-of`).
  `[:value v]` only when the bytes read as one EDN form `v`, `v` is a value
  that a fact can hold (`softland.store.act/value?`), and the UTF-8 of the
  one text of its normal form is exactly the row's bytes: so nothing stands
  after the form, and the row's String is the one text of the value. A gate
  writes a fact's row only with the text that the declaration's check made
  of a value of the form, so no write of a gate gives a row that earns the
  fourth: that answer is for a row that came to the table by another hand.
  The bytes of the text `nil` read as the value nil; no gate writes that
  text, since a fact with no value is a retract and its row holds no value.
  It never throws."
  [row]
  (cond
    (nil? row) [:no-fact]
    (nil? (:value row)) [:nothing-here]
    :else
    (try
      (let [^bytes held (plain-bytes (:value row))
            v (edn/read-string (String. held StandardCharsets/UTF_8))]
        (if (and (act/value? v)
                 (Arrays/equals held ^bytes (text/utf8 (act/normal-value v))))
          [:value (act/normal-value v)]
          [:not-a-value]))
      (catch Throwable _ [:not-a-value]))))

(defn who-of
  "Takes a fact's row or a row of the facts by value, or nil. Gives the id
  that the act which wrote the row gave as its `:who`, read back from its
  one text: a keyword or a UUID. nil when there is no row or the text does
  not read as an id. It never throws. The id is a claim until sign-in is
  built: a gate cannot check `:who`."
  [row]
  (when-let [held (:who row)]
    (let [id (first-form held)]
      (when (or (act/readable-keyword? id) (uuid? id)) id))))

(defn permission-of
  "Takes a fact's row or a row of the facts by value, or nil. Gives the
  permission that the act which wrote the row named, read back from its one
  text, in its one form. That permission stood when the act was decided.
  nil when there is no row or the text does not read as a permission id. It
  never throws. What the permission allows is
  `softland.store.act/allows` of it."
  [row]
  (when-let [held (:permission row)]
    (let [permission (first-form held)]
      (when (act/has-shape? :permission-id permission) permission))))

(defn marked-keys-of
  "Takes the row at a layer's marked-keys place, or nil. Gives a map from
  each key that the layer marks to the stamp since which the order of that
  key is level: the row's `:level-since`, read back. `{}` for no row, for a
  row with no such field, and for a text that does not read as a map from
  readable keywords to integers. It never throws. A gate reads it where an
  act is decided, and a read of \"what points at a thing\" reads it first:
  the stamp is a part of each key of that order."
  [row]
  (let [held (when-let [text (:level-since row)] (first-form text))]
    (if (and (map? held)
             (every? (fn [[k since]] (and (act/readable-keyword? k) (int? since))) held))
      held
      {})))

;; ------------------------------------------------------- the facts by value

(defn value-digest
  "Takes the plain bytes of a value, as `plain-bytes-of` gives them for a
  fact's row. Gives the value's
  text in the order by value: the SHA-256 of the bytes, as 64 lower-case hex
  characters. Every JVM has the algorithm. Cost: one hash over the bytes,
  65,536 at most for a value that a gate accepted under a plain key
  (`softland.store.door.declaration/max-value-bytes`)."
  [value-bytes]
  (text/hex (.digest (MessageDigest/getInstance "SHA-256") ^bytes value-bytes)))

(defn value-digest-of
  "Takes a value, as a maker would give it. Gives `value-digest` of the plain
  bytes that a row holds for it: the UTF-8 of the one text of its normal form. So
  a UUID is always taken as `#uuid \"...\"`, never as its 36 characters,
  and no caller chooses a text. In a microbatch layer this is also the
  partitioning key of the value's run. What must hold: the thing is a value
  (`softland.store.act/value?`)."
  [v]
  (value-digest (text/utf8 (act/normal-value v))))

(defn by-value-key
  "Takes a layer id, a fact's key, the stamp since which the key's order is
  level, a value digest and an entity id. Gives the key of that place in the
  facts by value: \"<layer> <key> <since> <value digest> <entity>\". The
  entity is last, so the places that hold one value under one key are
  neighbours. No part before the entity holds a space, and the digest has a
  fixed length, so a key begins with a run's text only when its layer, key,
  stamp and digest are that run's. What must hold: each id is within the
  limit, and the digest is `value-digest`'s."
  [layer k since digest entity]
  (str (declaration/id-text layer) " " (declaration/id-text k) " " (long since) " " digest " "
       (declaration/id-text entity)))

(defn by-value-run
  "Takes a layer id, a marked key, the key's stamp as the marks row gives it
  (`marked-keys-of`), and a value. Gives [from to], the two ends of the run
  of that value in the facts by value: every key from `from` up to `to`,
  with `to` not in the run, is a place of that layer whose current fact
  under that key has that value and was written since that stamp. `from` is
  the run's text with a space at its end; `to` is the same text with `!` in
  the place of that space. `!` is the character after a space, so the range
  holds the keys that begin with `from` and no other. What must hold: the
  ids are within the limit and the thing is a value; see the namespace
  docstring."
  [layer k since v]
  (let [run (str (declaration/id-text layer) " " (declaration/id-text k) " " (long since) " "
                 (value-digest-of v))]
    [(str run " ") (str run "!")]))

(defn- parts-of-key
  "Takes the text of a key. Gives its parts, the texts between its spaces,
  as a vector. No printed text of an id holds a space."
  [k]
  (vec (.split ^String k " " -1)))

(defn by-value-route
  "Takes a key of the facts by value. Gives its value digest, the fourth
  part: the route of the row in a microbatch layer, whose hash gives the
  row's task. What must hold: the key is `by-value-key`'s."
  [k]
  (nth (parts-of-key k) 3 nil))

(defn by-value-row
  "Takes the one text of an act's `:who`; the one text of the permission
  that the act named; the uuid of the act's act-id; the index of one of its
  facts; and the stamp of the act's decision. Gives the row of that fact in
  the facts by value: the fact's row without its value. A read of a run
  then gets who wrote each place and under which permission with no second
  read, and a fact that is written again with the same value changes the
  row. What must hold: the two texts are those that the fact's row holds
  (`fact-row`)."
  [who-text permission-text act-uuid index stamp]
  {:by-act act-uuid :by-index (long index) :stamp (long stamp)
   :who who-text :permission permission-text})

(defn id-of-text
  "Takes the printed text of an id or of a fact's key
  (`softland.store.door.declaration/id-text`). Gives the id: a keyword for a
  text that begins with a colon, else a UUID. nil for a text that is
  neither, and for one whose id does not print back as that text. It never
  throws."
  [id-text]
  (try
    (if (and (string? id-text) (.startsWith ^String id-text ":"))
      (let [k (keyword (subs id-text 1))]
        (when (and (act/readable-keyword? k) (= id-text (str k))) k))
      (let [uuid (UUID/fromString id-text)]
        (when (= id-text (str uuid)) uuid)))
    (catch Throwable _ nil)))

(defn place-of-key
  "Takes a key of the facts table, the text of a place. Gives
  [layer entity key], each as an id; or nil for a text that is no place's
  key. It never throws. A row of a run is under one of the store's own keys
  when `softland.store.door.declaration/store-key?` of the third part is
  true."
  [place-key-text]
  (try
    (let [parts (mapv id-of-text (parts-of-key place-key-text))]
      (when (and (= 3 (count parts)) (every? some? parts) (keyword? (nth parts 2)))
        parts))
    (catch Throwable _ nil)))

(defn by-value-entity
  "Takes a key of the facts by value. Gives the entity of its place, the
  last part, as an id; nil for a text that is no such key. It never throws.
  The place is then [layer entity key], with the layer and the key that the
  run was asked for."
  [k]
  (try
    (let [parts (parts-of-key k)]
      (when (= 5 (count parts)) (id-of-text (nth parts 4))))
    (catch Throwable _ nil)))

(defn answer-key-parts
  "Takes a key of the answers, as `answer-key` makes it. Gives
  [layer-text uuid]: the printed text of the layer, and the uuid of the
  act-id as a UUID; nil for a text that is no such key. It never throws.
  `id-of-text` of the first gives the layer back, a keyword or a UUID. The
  microbatch gate holds an answer by these two, each of one class, until
  its entry is appended (`softland.store.gate.batch/rows-to-keep`)."
  [answer-key-text]
  (try
    (let [parts (parts-of-key answer-key-text)]
      (when (and (= 2 (count parts)) (seq (nth parts 0)))
        (let [uuid (UUID/fromString (nth parts 1))]
          (when (= (nth parts 1) (str uuid))
            [(nth parts 0) uuid]))))
    (catch Throwable _ nil)))

;; ------------------------------------------------ the runs that a read names

(defn entity-run
  "Takes a layer id and an entity id. Gives [from to], the two ends of the
  run of that entity's places in the facts table: every key from `from` up
  to `to`, with `to` not in the run, is a place of that entity in that
  layer. `from` is \"<layer> <entity> \"; `to` is the same text with `!` in
  the place of its last space. `!` is the character after a space, so the
  range holds the keys that begin with `from` and no other: not a place of
  an entity whose text only begins with this entity's text. What must hold:
  both are ids within the limit."
  [layer entity]
  (let [run (str (declaration/id-text layer) " " (declaration/id-text entity))]
    [(str run " ") (str run "!")]))

(defn own-rows-run
  "Takes a layer id. Gives [from to], the two ends of the run of the layer's
  own rows in the facts table: the rows of the layer's own entity under the
  keys of the store's namespace
  (`softland.store.door.declaration/store-namespace`), `to` not in the run.
  Those are the layer's depot, who may see it, what it builds on and its
  marked keys; and its holder's permission, when the layer's id is also a
  holder's id. Every key of that namespace prints as `:softland.store/...`,
  so `from` is \"<layer> <layer> :softland.store/\", and `to` is the same
  text with `0`, the character after `/`, in the place of its last
  character. What must hold: the layer is an id within the limit."
  [layer]
  (let [run (str (declaration/id-text layer) " " (declaration/id-text layer) " :" declaration/store-namespace)]
    [(str run "/") (str run "0")]))

(defn before?
  "Takes two texts. Gives true when the first is before the second in the
  order in which Rama keeps the String keys of a table: by code point,
  which is the order of the bytes of UTF-8. A text that is the start of
  another is before it; a text is not before itself. Not by Java's
  `compareTo`, which compares UTF-16 units and so puts a letter outside the
  first plane before U+E000. That a table goes through its keys in this
  order was run on a probe's table and on the store's own. For a key that
  holds a letter from U+E000 on, or one outside the first plane, Rama was
  also seen to use Java's order: at the end of a range, and in the map that
  its client makes of a run. A reader guards against that where it reads
  (`softland.store.read-path.sources`). What must hold: both are Strings."
  [a b]
  (let [^String a a
        ^String b b
        na (.length a)
        nb (.length b)]
    (loop [i 0 j 0]
      (cond
        (>= j nb) false
        (>= i na) true
        :else (let [ca (.codePointAt a i)
                    cb (.codePointAt b j)]
                (cond
                  (< ca cb) true
                  (> ca cb) false
                  :else (recur (+ i (Character/charCount ca)) (+ j (Character/charCount cb)))))))))

(defn run-of-ends
  "Takes a layer id and the two ends of a run of its places, as a query
  names them. Gives [from to], the two texts between which the run lies in
  the facts table, `to` not in the run; or nil when the first end is after
  the last, or an end is none of these:

    as the first end   :first         from the layer's first place
                       [entity]       from the entity's first place
                       [entity key]   from that place
    as the last end    [entity]       to the entity's last place
                       [entity key]   to that place: the place's key and
                                      one space, so that the place is in
                                      the run
                       :last          to the layer's last place

  Both texts begin with the layer's text and a space or `!`, so a run never
  leaves its layer. What must hold: each id is within the limit and each
  key a readable keyword within it."
  [layer first-end last-end]
  (let [l (declaration/id-text layer)
        from (cond
               (= :first first-end) (str l " ")
               (and (vector? first-end) (= 1 (count first-end))) (first (entity-run layer (nth first-end 0)))
               (and (vector? first-end) (= 2 (count first-end))) (place-key layer (nth first-end 0) (nth first-end 1)))
        to (cond
             (= :last last-end) (str l "!")
             (and (vector? last-end) (= 1 (count last-end))) (second (entity-run layer (nth last-end 0)))
             (and (vector? last-end) (= 2 (count last-end))) (str (place-key layer (nth last-end 0) (nth last-end 1)) " "))]
    (when (and from to (not (before? to from)))
      [from to])))

;; ------------------------------------------------- one row against another

(defn same-row?
  "Takes two rows, each a row of the facts table or of the facts by value,
  or nil. Gives true when both are nil, or each field of one equals the
  same field of the other. False when one is nil, or a field differs or is
  in one only. Each field of such a row is a Long, a UUID or a String, and
  none a byte array, so the equality of the two maps says it. It never
  throws. A reader of the tables asks it of the row before and the row
  after a change: a row that a second attempt of a batch wrote again is the
  same row."
  [row-a row-b]
  (try
    (= row-a row-b)
    (catch Throwable _ false)))

(defn holds-this-value?
  "Takes a fact's row or nil, and a value as a maker would give it. Gives
  true when the row holds a value and `value-digest` of its plain bytes is
  `value-digest-of` of the value: the test by which a gate puts a fact in
  that value's order by value. False for no row and for a retract. A reader
  asks it of a nearer layer's row at a place that a farther layer's order
  gives: the place is given from the nearer row exactly when a mark would
  have put that row in the nearer layer's own order for the value. What
  must hold: the value passes `softland.store.act/value?`. It never
  throws."
  [row v]
  (try
    (boolean (and row
                  (some? (:value row))
                  (= (value-digest (plain-bytes (:value row))) (value-digest-of v))))
    (catch Throwable _ false)))

;; ------------------------------------------------- a row of a closed batch

(defn closed?
  "Takes a row that holds a stamp, an answer's or a fact's, or nil; and the
  `:closed-through` of the row's task, or nil. Gives true when the row is of
  a closed batch of the microbatch gate: its stamp is not later than
  `:closed-through`. False for no row, and before any batch is closed. The
  door's two reads and the streaming gate's question about the base give
  such a row of a microbatch layer only when this is true."
  [row closed-through]
  (boolean (and row closed-through (<= (long (:stamp row)) (long closed-through)))))

;; ------------------------------------------- what a caller gave a read

(defn answer-read
  "The test at the head of the read by act-id. Takes what a caller gave: a
  layer and an act-id. Gives [depot key]: the depot slot of the act-id, and
  the key of its answer; or nil when no read is to be made: the layer is no
  id within the limit, the thing is no act-id, or its layer slot is not the
  layer given. It never throws."
  [layer act-id]
  (when (and (declaration/usable-id? layer)
             (declaration/act-id-in-layer? layer act-id))
    [(nth act-id 1) (answer-key layer (nth act-id 2))]))

(defn depot-read
  "The test at the head of the read of a layer's depot. Takes what a caller
  gave as a layer. Gives the key of the layer's depot fact, the place of the
  layer as entity under `:softland.store/depot`; or nil when the thing is no
  id within the limit. It never throws."
  [layer]
  (when (declaration/usable-id? layer)
    (place-key layer layer :softland.store/depot)))

(defn routing-id
  "Takes an act-id. Gives its routing id, the first argument of the read by
  act-id: the layer of a streaming act-id, the uuid of a microbatch act-id.
  Rama's client hashes it, so the read goes straight to the task where the
  answer is. Gives nil for a thing that is no act-id. It never throws."
  [act-id]
  (when (and (vector? act-id)
             (declaration/act-id-in-layer? (nth act-id 0 nil) act-id))
    (if (= :microbatch (nth act-id 1)) (nth act-id 2) (nth act-id 0))))

(defn answer-read-by-routing-id
  "The test at the head of the query by act-id. Takes what a caller gave: a
  routing id and an act-id. Gives what `answer-read` gives for the act-id
  and its own layer slot, [depot key]; or nil when no read is to be made:
  the thing is no act-id, the routing id is not the one of the act-id's
  depot, or the layer is over the limit. It never throws."
  [given act-id]
  (let [own (routing-id act-id)]
    (when (and (some? own) (= own given))
      (answer-read (nth act-id 0) act-id))))
