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
                {:value      the value's bytes: sealed when a lock is named,
                             the one text of a plain value when none is;
                             absent for a retract
                 :lock-act   the lock that seals :value, as the uuid of the
                 :lock-index act that asked for it and its index
                 :by-act     the fact that wrote this row, as the uuid of
                 :by-index   its act and its index in that act
                 :stamp      the stamp of that act's decision}
    a lock      key \"<layer> <request uuid> <index, three digits>\"
                {:bytes       the lock's bytes
                 :way         the number of the way of sealing it is for
                 :seals-act   the fact that it seals, once it seals one, as
                 :seals-index the uuid of that fact's act and its index}

  Every accepted fact is a row of the second kind, under a store key or a
  plain key: the depot of a layer at
  \"<layer> <layer> :softland.store/depot\", a permission at
  \"<layer> <holder> :softland.store/permission\", a request for locks at
  \"<layer> <act uuid> :softland.store/lock-request\". A plain value is held
  as the bytes of its one text, so the field has one class, and \"is this
  the permission that the act names\" is a comparison of bytes.

  Where a row lives. The rows are the same for both gates; the task is not.
  A row of a streaming layer is on the layer's task. A row of a microbatch
  layer is on the task that the second part of its key gives: an answer on
  the task of the act's uuid, a fact on the task of its entity, a lock on
  the task of its request's uuid. A fact under `:softland.store/depot` or
  `:softland.store/permission` of a microbatch layer is on every task. No
  row names a task.

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
  declare: a Long, a UUID, a Keyword, a byte array. A row of another class
  throws in a topology, and a throw there stops the partition. So a gate
  writes no row that these functions did not make.

  Inputs and results, by group:

    answer-key, place-key, lock-key   ids -> the text of a key
    answer-row, fact-row, lock-row, lock-row-sealing,
    lock-row-without-bytes                             -> a row to write
    answer-of, written-by?, holds-plain-value?, plain-value-of,
    seals-a-fact?, lock-of, locks-by-index,
    locks-by-index-used-without-bytes, closed?         a row -> what it says
    routing-id                        an act-id -> the first argument of a
                                      read by that act-id
    answer-read, lock-range-read, depot-read,
    answer-read-by-routing-id, lock-range-read-by-routing-id
                                      what a caller gave a read
                                      -> the keys to read, or nil

  State and effects. None. Each function is pure.

  Callers. `softland.store.gate.decision`, `softland.store.gate.batch`, the two gates,
  and `softland.store.module.door-reads`. The proof's hand calls `routing-id` and
  the `-read` functions to test what it passes before it calls a query."
  (:require [clojure.edn :as edn]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.util Arrays]))

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
  "Takes a fact in its one form on arrival, the uuid of its act's act-id, its
  index in the act, and the stamp of the act's decision. Gives the row of
  the fact's place. A sealed fact: its sealed bytes and its lock. A fact
  with a plain value, which is a fact under a store key: the bytes of the
  one text of the value, and no lock. A fact with no value, a retract: no
  value and no lock; the row is there all the same, so that a later fact
  can replace the retract. What must hold: the fact passed the declaration's
  check."
  [fact act-uuid index stamp]
  (let [sealed (:sealed fact)
        [[_ _ lock-uuid] lock-index] (:lock-id fact)]
    (cond-> {:by-act act-uuid :by-index (long index) :stamp (long stamp)}
      sealed
      (assoc :value sealed :lock-act lock-uuid :lock-index (long lock-index))

      (and (nil? sealed) (some? (:v fact)))
      (assoc :value (text/utf8 (:v fact))))))

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

(defn holds-plain-value?
  "Takes a fact's row or nil, and a value in its one normal form. Gives true
  when the row holds that value as a plain value: its bytes are the one text
  of the value, and no lock is named. No decoding."
  [row v]
  (boolean (and row
                (nil? (:lock-act row))
                (some? (:value row))
                (Arrays/equals ^bytes (:value row) ^bytes (text/utf8 v)))))

(defn plain-value-of
  "Takes a fact's row or nil. Gives the plain value that the row holds, read
  back from its one text, or nil when there is no row, the row holds no
  value, or its value is sealed. It never throws. A row that holds the plain
  value nil does not exist: a fact with no value is a retract."
  [row]
  (when (and row (some? (:value row)) (nil? (:lock-act row)))
    (try (edn/read-string (String. ^bytes (:value row) StandardCharsets/UTF_8))
         (catch Throwable _ nil))))

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
  `:closed-through`. False for no row, and before any batch is closed. A
  read gives such a row of a microbatch layer only when this is true. A
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
  cases, so that it gives the same on every cluster: the bytes of a lock
  that seals a value are read beside the value."
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
