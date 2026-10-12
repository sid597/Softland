(ns softland.store.record.rows
  "The record's rows: the text of each key, the making of each row, and the
  reading of each row. Pure code. It needs no cluster, and it holds no
  record: the record is in a gate's PStates
  (`softland.store.record.tables`), and a gate reads and writes it.

  The record has four kinds of row, each under a string key that begins
  with its layer, and one value for each task.

    an answer   key \"<layer> <act uuid>\"
                {:refused the reason word, absent when the act was accepted
                 :stamp   the stamp of the decision
                 :digest  32 bytes, the digest of the thing that was decided}
    a fact      key \"<layer> <entity> <key>\": the current fact of a place
                {:value       the value's bytes, plain: for a fact under a
                              plain key the bytes that its lock opened; for
                              a fact under a store key the one text of its
                              value. Absent for a retract
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
    a lock      key \"<layer> <request uuid> <index, three digits>\"
                {:bytes       the lock's bytes
                 :way         the number of the way of sealing it is for
                 :seals-act   the fact that it seals, once it seals one, as
                 :seals-index the uuid of that fact's act and its index}
    a fact by value
                key \"<layer> <key> <since> <value digest> <entity>\": one
                place under a marked key, in the order of its value
                {:by-act :by-index :stamp :who :permission}: the fact's row
                without its value. The value is the one whose digest the key
                holds

  Every accepted fact is a row of the second kind, under a store key or a
  plain key: the depot of a layer at
  \"<layer> <layer> :softland.store/depot\", a permission at
  \"<layer> <holder> :softland.store/permission\", a request for locks at
  \"<layer> <act uuid> :softland.store/lock-request\", and at the layer's own
  entity who may see it, what it builds on and its marked keys. A value is
  held as bytes, so the field has one class, and \"is this the permission
  that the act names\" is a comparison of bytes. No row names a lock: the
  lock of a value is found from the act in the depot, which holds each
  fact's lock id, and from the lock's own row, which names the fact that it
  seals.

  The fourth kind, the facts by value. A layer marks a key by a fact
  (`softland.store.door.declaration/store-keys`). For a marked key a gate
  keeps each fact that has a value in this order too, so that every place
  that holds one value under one key is a run of neighbouring keys: the keys
  that begin with \"<layer> <key> <since> <value digest> \" (`by-value-run`).
  The value digest is the SHA-256 of the row's value bytes, as 64 hex
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
  of the act's uuid, a fact on the task of its entity, a lock on the task of
  its request's uuid, a fact by value on the task of its value digest
  (`by-value-route`), so that the run of one value is on one task. A fact of
  a microbatch layer under `:softland.store/depot`,
  `:softland.store/permission`, `:softland.store/who-may-see`,
  `:softland.store/builds-on` or `:softland.store/marked-keys` is on every
  task. No row names a task.

  One copy of a lock's bytes. In a streaming layer a lock has one row, and
  the row keeps its bytes when it comes to seal a fact. In a microbatch
  layer a lock that seals a fact has two rows under its one key: on the
  task of the value's entity the whole row, with the bytes; and on the task
  of its request's uuid the home's row, which holds the way and the fact
  and no `:bytes` (`lock-row-without-bytes`). When the two tasks are one,
  the one row is the whole row.

  Why the keys are strings. A key is a pair or a triple of ids, and an id is
  a keyword or a UUID: two classes. A string made of the parts' printed
  texts is one class, equal keys are equal strings, and Rama gives the order
  of string keys, on which the range read of a request's locks rests. No
  printed text of an id holds a space, so one space parts the parts. The
  layer and the depot of an id are in no row: the key says the layer, and
  the gate that owns the PState says the depot.

  The functions that make a row give exactly the classes that the schemas
  declare: a Long, a UUID, a Keyword, a String, a byte array. A row of another class
  throws in a topology, and a throw there stops the partition. So a gate
  writes no row that these functions did not make.

  Inputs and results, by group:

    answer-key, place-key, lock-key, by-value-key      -> the text of a key
    by-value-run                      a layer, a key, a stamp, a value
                                      -> the two ends of that value's run
    entity-run, own-rows-run, run-of-ends
                                      a layer, and an entity or two ends
                                      -> the two ends of a run of places
    before?                           two texts -> whether the first is
                                      before the second in the table's order
    value-digest, value-digest-of     bytes, or a value -> 64 hex characters
    place-of-key, by-value-entity, by-value-route, id-of-text
                                      the text of a key -> its parts
    answer-row, fact-row, with-level-since, by-value-row, lock-row,
    lock-row-sealing, lock-row-without-bytes           -> a row to write
    answer-of, written-by?, holds-value?, value-of, read-value, who-of,
    permission-of, marked-keys-of, seals-a-fact?, lock-of, locks-by-index,
    locks-by-index-used-without-bytes, closed?         a row -> what it says
    same-row?, holds-this-value?      a row, and a row or a value
                                      -> true or false
    routing-id                        an act-id -> the first argument of a
                                      read by that act-id
    answer-read, lock-range-read, depot-read,
    answer-read-by-routing-id, lock-range-read-by-routing-id
                                      what a caller gave a read
                                      -> the keys to read, or nil

  State and effects. None. Each function is pure.

  Callers. `softland.store.gate.decision`, `softland.store.gate.batch`, the two gates,
  and `softland.store.module.door-reads`. The door (`softland.store.door`) and
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

(defn lock-key
  "Takes a layer id, the uuid of a request's act-id and the index of a lock,
  0 to 255. Gives the key of that lock:
  \"<layer> <uuid> <index, three digits>\". Three digits, so that the order
  of the keys is the order of the indexes and the locks of one request are
  one range. It uses no locale."
  [layer request-uuid index]
  (let [digits (Long/toString (long index))]
    (str (declaration/id-text layer) " " request-uuid " "
         (subs "000" (min 3 (count digits))) digits)))

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

;; -------------------------------------------------------------------- facts

(defn fact-row
  "Takes the act in its one form on arrival; one of its facts; the plain
  bytes that the fact's lock opened, or nil for a fact that is not sealed;
  the uuid of the act's act-id; the fact's index in the act; and the stamp
  of the act's decision. Gives the row of the fact's place. A sealed fact:
  the plain bytes. A fact with a plain value, which is a fact under a store
  key: the bytes of the one text of the value. A fact with no value, a
  retract: no value; the row is there all the same, so that a later fact can
  replace the retract. Each row holds who wrote it and the permission that
  the act named, each as its one text: a UUID has two texts, and the one
  text reads back as a UUID. It names no lock.

  What must hold: the act passed the declaration's check; for a sealed fact
  the bytes are what its lock opened
  (`softland.store.gate.decision/opened`)."
  [act fact plain act-uuid index stamp]
  (cond-> {:by-act act-uuid :by-index (long index) :stamp (long stamp)
           :who (text/text (:who act)) :permission (text/text (:permission act))}
    (some? plain)
    (assoc :value plain)

    (and (nil? plain) (some? (:v fact)))
    (assoc :value (text/utf8 (:v fact)))))

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
  when the row's bytes are the one text of that value. No decoding. The
  gates ask it of the row at a permission's place, where only a fact under
  that store key can be."
  [row v]
  (boolean (and row
                (some? (:value row))
                (Arrays/equals ^bytes (:value row) ^bytes (text/utf8 v)))))

(defn- first-form
  "Takes a string. Gives the first EDN form that it holds, or nil when it
  holds none or does not read. It never throws. nil is also what the text
  `nil` reads as; a caller that must tell the two apart compares the text."
  [s]
  (try (edn/read-string s)
       (catch Throwable _ nil)))

(defn value-of
  "Takes a fact's row or nil. Gives the first form that the row's bytes read
  as, or nil when there is no row, the row holds no value, or its bytes do
  not read. It never throws. `softland.store.module.door-reads` asks it of
  the row at a depot's place and gets the depot word. It tests nothing of
  the form that it gives: a reader of a tool's fact calls `read-value`."
  [row]
  (when (and row (some? (:value row)))
    (first-form (String. ^bytes (:value row) StandardCharsets/UTF_8))))

(defn read-value
  "What a fact's row says of its value, for a reader of the tables. Takes a
  fact's row, or nil. Gives one of four:

    [:no-fact]       no row: no fact was ever accepted at the place
    [:nothing-here]  a row with no value: the current fact is a retract
    [:value v]       the value, as its maker gave it
    [:not-a-value]   the row's bytes are no value of the form

  `[:value v]` only when the bytes read as one EDN form `v`, `v` is a value
  that a fact can hold (`softland.store.act/value?`), and the UTF-8 of the
  one text of its normal form is exactly the row's bytes: so nothing stands
  after the form, and the value was written as the door writes one. A gate
  keeps what a lock opened, whatever the bytes are, and does not check that
  they are a value; a thing that was appended without the door can leave
  bytes that earn the fourth. The bytes of the text `nil` read as the value
  nil: the door never seals them, since a fact with no value is a retract.
  It never throws."
  [row]
  (cond
    (nil? row) [:no-fact]
    (nil? (:value row)) [:nothing-here]
    :else
    (try
      (let [^bytes held (:value row)
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
  "Takes the bytes of a value, as a fact's row holds them. Gives the value's
  text in the order by value: the SHA-256 of the bytes, as 64 lower-case hex
  characters. Every JVM has the algorithm. Cost: one hash over the bytes,
  65,508 at most for a value that came sealed."
  [value-bytes]
  (text/hex (.digest (MessageDigest/getInstance "SHA-256") ^bytes value-bytes)))

(defn value-digest-of
  "Takes a value, as a maker would give it. Gives `value-digest` of the bytes
  that a row holds for it: the UTF-8 of the one text of its normal form. So
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
  "Takes the act in its one form on arrival, the uuid of its act-id, the
  index of one of its facts and the stamp of the act's decision. Gives the
  row of that fact in the facts by value: the fact's row without its value.
  A read of a run then gets who wrote each place and under which permission
  with no second read, and a fact that is written again with the same value
  changes the row."
  [act act-uuid index stamp]
  {:by-act act-uuid :by-index (long index) :stamp (long stamp)
   :who (text/text (:who act)) :permission (text/text (:permission act))})

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
  same field of the other, a run of bytes by its content. False when one is
  nil, or a field differs or is in one only. It never throws. A reader of
  the tables asks it of the row before and the row after a change: a row
  that a second attempt of a batch wrote again is the same row."
  [row-a row-b]
  (try
    (cond
      (and (nil? row-a) (nil? row-b)) true
      (or (nil? row-a) (nil? row-b)) false
      (not (and (map? row-a) (map? row-b))) (= row-a row-b)
      :else (every? (fn [field]
                      (let [a (get row-a field)
                            b (get row-b field)]
                        (if (and (bytes? a) (bytes? b))
                          (Arrays/equals ^bytes a ^bytes b)
                          (= a b))))
                    (into #{} (concat (keys row-a) (keys row-b)))))
    (catch Throwable _ false)))

(defn holds-this-value?
  "Takes a fact's row or nil, and a value as a maker would give it. Gives
  true when the row holds bytes and `value-digest` of them is
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
                  (= (value-digest (:value row)) (value-digest-of v))))
    (catch Throwable _ false)))

;; -------------------------------------------------------------------- locks

(defn lock-row
  "Takes a lock, {:bytes :way}, that seals nothing yet. Gives its row."
  [lock]
  {:bytes (:bytes lock) :way (long (:way lock))})

(defn lock-row-sealing
  "Takes a lock's row as it was read, and the fact that the lock now seals,
  as the uuid of its act's act-id and its index. Gives the row to write
  again: the bytes and the way as read, and the fact. The whole row is
  given, so that the write needs no read."
  [row act-uuid index]
  {:bytes (:bytes row) :way (:way row) :seals-act act-uuid :seals-index (long index)})

(defn lock-row-without-bytes
  "Takes the row of a lock that now seals a fact, as `lock-row-sealing` gives
  it. Gives the home's row of that lock in a microbatch layer: the way and
  the fact, and no `:bytes`. The bytes are then in one row only, the whole
  row beside the value. `seals-a-fact?` is true of it, so a second value
  under the lock is refused before any bytes are asked of it."
  [row]
  {:way (:way row) :seals-act (:seals-act row) :seals-index (:seals-index row)})

(defn seals-a-fact?
  "Takes a lock's row. Gives true when the lock seals an accepted value."
  [row]
  (some? (:seals-act row)))

(defn lock-of
  "Takes a lock's row. Gives the lock as `softland.store.door.seal` takes one, and
  as a caller of the read of locks gets one: {:bytes :way}."
  [row]
  {:bytes (:bytes row) :way (:way row)})

(defn locks-by-index
  "Takes rows of locks of one request, as a map or a sequence of pairs from a
  lock's key to its row. Gives a plain map from each lock's index to the
  lock, {:bytes :way}. The index is read from the last three characters of
  the key."
  [rows]
  (into {}
        (map (fn [[k row]]
               [(Long/parseLong (subs k (- (count k) 3))) (lock-of row)]))
        rows))

(defn closed?
  "Takes a row that holds a stamp, an answer's or a fact's, or nil; and the
  `:closed-through` of the row's task, or nil. Gives true when the row is of
  a closed batch of the microbatch gate: its stamp is not later than
  `:closed-through`. False for no row, and before any batch is closed. The
  door's three reads and the streaming gate's question about the base give
  such a row of a microbatch layer only when this is true. A
  lock's row holds no stamp; the read of locks holds its request's answer
  to this test."
  [row closed-through]
  (boolean (and row closed-through (<= (long (:stamp row)) (long closed-through)))))

(defn locks-by-index-used-without-bytes
  "Takes rows of locks of one request in a microbatch layer, as a map or a
  sequence of pairs from a lock's key to its row. Gives what
  `locks-by-index` gives, with one difference: a lock that seals a value is
  given with its way and with nil for its bytes. On most tasks the home's
  row of such a lock holds no bytes; where the lock's home is also the task
  of its value, the one row holds them. The read leaves them out in both
  cases, so that it gives the same on every cluster: the whole row of such a
  lock, with its bytes, is on the task of its value."
  [rows]
  (locks-by-index
   (map (fn [[k row]] [k (if (seals-a-fact? row) (lock-row-without-bytes row) row)]) rows)))

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

(defn lock-range-read
  "The test at the head of the read of a request's locks. Takes what a caller
  gave: a layer, the act-id of a request, and the first and the last index
  wanted. Gives [depot from-key to-key], both keys in the range; or nil when
  no read is to be made: the layer or the act-id is not usable, as in
  `answer-read`, an index is no integer, or the range holds no index. The
  range is cut to 0 to 255, so one read iterates 256 rows at most. It never
  throws."
  [layer request-act-id from to]
  (when (and (declaration/usable-id? layer)
             (declaration/act-id-in-layer? layer request-act-id)
             (int? from)
             (int? to))
    (let [from (max 0 (long from))
          to (min (dec declaration/max-locks-in-request) (long to))
          uuid (nth request-act-id 2)]
      (when (<= from to)
        [(nth request-act-id 1) (lock-key layer uuid from) (lock-key layer uuid to)]))))

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
  act-id and of the read of a request's locks: the layer of a streaming
  act-id, the uuid of a microbatch act-id. Rama's client hashes it, so the
  read goes straight to the task where the answer is. Gives nil for a thing
  that is no act-id. It never throws."
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

(defn lock-range-read-by-routing-id
  "The test at the head of the query of a request's locks. Takes what a
  caller gave: a routing id, the act-id of a request, and the first and the
  last index wanted. Gives what `lock-range-read` gives for the act-id and
  its own layer slot, [depot from-key to-key]; or nil when no read is to be
  made, as in `answer-read-by-routing-id` and `lock-range-read`. It never
  throws."
  [given request-act-id from to]
  (let [own (routing-id request-act-id)]
    (when (and (some? own) (= own given))
      (lock-range-read (nth request-act-id 0) request-act-id from to))))
