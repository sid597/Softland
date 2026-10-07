(ns softland.store.record
  "The record's rows: the text of each key, the making of each row, and the
  reading of each row. Pure code. It needs no cluster, and it holds no
  record: the record is in a gate's PStates
  (`softland.store.record-pstates`), and a gate reads and writes it.

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
  plain key: the class of a layer at \"<layer> <layer> :class\", a permission
  at \"<layer> <holder> :permission\", a request for locks at
  \"<layer> <act uuid> :lock-request\". A plain value is held as the bytes of
  its one text, so the field has one class, and \"is this the permission that
  the act names\" is a comparison of bytes.

  Why the keys are strings. A key is a pair or a triple of ids, and an id is
  a keyword or a UUID: two classes. A string made of the parts' printed
  texts is one class, equal keys are equal strings, and Rama gives the order
  of string keys, on which the range read of a request's locks rests. No
  printed text of an id holds a space, so one space parts the parts. The
  layer and the class of an id are in no row: the key says the layer, and
  the gate that owns the PState says the class.

  The functions that make a row give exactly the classes that the schemas
  declare: a Long, a UUID, a Keyword, a byte array. A row of another class
  throws in a topology, and a throw there stops the partition. So a gate
  writes no row that these functions did not make.

  Inputs and results, by group:

    answer-key, place-key, lock-key   ids -> the text of a key
    answer-row, fact-row, lock-row, lock-row-sealing   -> a row to write
    answer-of, written-by?, holds-plain-value?, plain-value-of,
    seals-a-fact?, lock-of, locks-by-index             a row -> what it says
    answer-read, lock-range-read, class-read   what a caller gave a read
                                               -> the keys to read, or nil

  State and effects. None. Each function is pure.

  Callers. `softland.store.decision`, the gate, and
  `softland.store.door-reads`. The proof's hand calls the three `-read`
  functions to test what it passes before it calls a query."
  (:require [clojure.edn :as edn]
            [softland.store.declaration :as declaration]
            [softland.store.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.util Arrays]))

;; ----------------------------------------------------------------- the keys

(defn answer-key
  "Takes a layer id and the uuid of an act-id. Gives the key of that act's
  answer: \"<layer> <uuid>\". The class slot of the act-id is not in it: a
  gate answers only under an act-id of its own class. The layer is in it,
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
  "Takes the class of the gate that owns the record, the layer, a fact's row
  or nil, and a fact id, [[layer class uuid] index]. Gives true when the
  fact that wrote the row is the one named: the fact id is of this layer and
  this class, and its uuid and index are the row's. False for no row, for a
  fact of another layer or class, and for any other fact."
  [gate-class layer row fact-id]
  (let [[[named-layer named-class named-uuid] named-index] fact-id]
    (boolean (and row
                  (= layer named-layer)
                  (= gate-class named-class)
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

(defn seals-a-fact?
  "Takes a lock's row. Gives true when the lock seals an accepted value."
  [row]
  (some? (:seals-act row)))

(defn lock-of
  "Takes a lock's row. Gives the lock as `softland.store.seal` takes one, and
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

;; ------------------------------------------- what a caller gave a read

(defn answer-read
  "The test at the head of the read by act-id. Takes what a caller gave: a
  layer and an act-id. Gives [class key]: the class slot of the act-id, and
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
  wanted. Gives [class from-key to-key], both keys in the range; or nil when
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

(defn class-read
  "The test at the head of the read of a layer's class. Takes what a caller
  gave as a layer. Gives the key of the layer's class fact, the place of the
  layer as entity under `:class`; or nil when the thing is no id within the
  limit. It never throws."
  [layer]
  (when (declaration/usable-id? layer)
    (place-key layer layer :class)))
