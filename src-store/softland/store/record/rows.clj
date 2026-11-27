(ns softland.store.record.rows
  "The record's rows: the text of each key, the making of each row, and the
  reading of each row. Pure code. The record is in a gate's PStates
  (`softland.store.record.tables`), which says on which task a row lives.

  Why the keys are strings. A key is a pair or a triple of ids, and an id is
  a keyword or a UUID: two classes. A string made of the parts' printed
  texts is one class, equal keys are equal strings, and Rama gives the order
  of string keys, on which each run that a read names rests. No printed
  text of an id holds a space, so one space parts the parts. The lane and
  the depot of an id are in no row: the key says the lane, and the gate
  that owns the PState says the depot.

  A fact's value is held as one String, its one text. So the field has one
  class, and a push on the row is told each change of it. Who wrote a row,
  and the permission that its act named, are each held as the printed text
  of an id.

  The functions that make a row give exactly the classes that the schemas
  declare. A row of another class throws in a topology, and a throw there
  stops the partition. So a gate writes no row that these functions did not
  make.

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
  "The depot slot of the act-id is not in the key: a gate answers only under
  an act-id of its own depot. The lane is in it, although the uuid alone is
  random, so that a uuid used in two lanes of one task is two rows."
  [lane uuid]
  (str (declaration/id-text lane) " " uuid))

(defn place-key
  "The keys of one entity are neighbours in the order, and the entities of
  one lane too."
  [lane entity k]
  (str (declaration/id-text lane) " " (declaration/id-text entity) " " (declaration/id-text k)))

;; ------------------------------------------------------------------ answers

(defn answer-row
  "An accepted act has no `:refused`, and a thing with no usable time no
  `:time`: there is one row for every act for ever, so the row keeps the
  smaller form. The time is the time on the thing, a label that no rule
  reads. The digest is the 32 bytes of the thing that was decided."
  [refused stamp time-millis digest]
  (cond-> {:stamp (long stamp) :digest digest}
    (some? time-millis) (assoc :time (long time-millis))
    refused (assoc :refused refused)))

(defn answer-of
  "nil, for no row, says \"nothing yet\"."
  [row]
  (when row
    {:verdict (if (:refused row) :refused :accepted)
     :reason  (:refused row)
     :stamp   (:stamp row)
     :time    (:time row)
     :digest  (text/hex (:digest row))}))

;; -------------------------------------------------------------------- facts

(defn plain-bytes
  "The plain bytes of a value: the UTF-8 of the String that a fact's row
  holds for it, which in a row that a gate wrote is the one text of the
  value in its normal form. The digest of the order by value is taken over
  them."
  [held]
  (.getBytes ^String held StandardCharsets/UTF_8))

(defn plain-bytes-of
  [row]
  (when-let [held (:value row)]
    (plain-bytes held)))

(defn fact-row
  "`:value` is the fact's `:text`: the one text of the value in its normal
  form, as the declaration's check made it
  (`softland.store.door.declaration/check`), under a plain key and under a
  store key alike. A retract has no `:text`: its row holds no value, and is
  there all the same, so that a later fact can replace the retract.

  `:who` and `:permission` are each the printed text of an id
  (`softland.store.door.declaration/id-text`): who the act named as its
  author, and the id of the permission that it named. An act that named no
  permission wrote a row with none. The two texts are given, and not made
  here, so that a caller makes them once for an act and not once for each
  fact.

  What must hold: the act passed the declaration's check, so the time is a
  whole number not below zero."
  [fact act-uuid index stamp time-millis who-text permission-text]
  (cond-> {:by-act act-uuid :by-index (long index) :stamp (long stamp)
           :time (long time-millis) :who who-text}
    (some? permission-text)
    (assoc :permission permission-text)

    (some? (:text fact))
    (assoc :value (:text fact))))

(defn use-row
  "The row of the use of a permission for one exact act, at the
  permission's place (`softland.store.gate.decision`,
  `use-of-its-permission`). It has no `:value`, and no `:by-index`: no fact
  of the act wrote it, so no fact id can be made of it (`written-by?` holds
  it to none), and that tells it from an end (`used?`). What must hold: as
  for `fact-row`."
  [act-uuid stamp time-millis who-text permission-text]
  {:by-act act-uuid :stamp (long stamp) :time (long time-millis) :who who-text :permission permission-text})

(defn with-level-since
  "For the row of a fact under `:softland.store/marked-keys`. `:level-since`
  is the one text of a map from each key that the fact marks to the stamp
  since which the order of that key is level. A gate derives the map at the
  write (`softland.store.gate.decision/writes`): no maker can write a
  gate's stamp."
  [row since-by-key]
  (assoc row :level-since (text/text since-by-key)))

(defn written-by?
  [gate-depot lane row fact-id]
  (let [[[named-lane named-depot named-uuid] named-index] fact-id]
    (boolean (and row
                  (= lane named-lane)
                  (= gate-depot named-depot)
                  (= (:by-act row) named-uuid)
                  (= (:by-index row) named-index)))))

(defn- first-form
  "nil is also what the text `nil` reads as; a caller that must tell the two
  apart compares the text.

  It never throws."
  [s]
  (try (edn/read-string s)
       (catch Throwable _ nil)))

(defn value-of
  "It tests nothing of the form that it gives: a reader of a tool's fact
  calls `read-value`.

  It never throws."
  [row]
  (when-let [plain (plain-bytes-of row)]
    (first-form (String. ^bytes plain StandardCharsets/UTF_8))))

(defn read-value
  "`[:no-fact]`: no fact was ever accepted at the place. `[:nothing-here]`:
  the current fact is a retract.

  `[:value v]` only when the UTF-8 of the one text of the normal form of
  `v` is exactly the row's bytes: so nothing stands after the form. A gate
  writes a fact's row only with the text that the declaration's check made,
  so no write of a gate earns `[:not-a-value]`: that answer is for a row
  that came to the table by another hand.

  The text `nil` reads as the value nil; no gate writes that text, since a
  fact with no value is a retract and its row holds no value.

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

(declare id-of-text)

(defn who-of
  "The id is what the program that sent the act said: a gate checked no
  sender (src-store/softland/store/README.md, \"What the gate does not
  check, said plainly\").

  It never throws."
  [row]
  (id-of-text (:who row)))

(defn permission-of
  "The id of the permission that the act which wrote the row named; nil for
  a row of an act that named none. It stood when the act was decided. What
  it allowed then is not in the row: a permission can be changed after.

  It never throws."
  [row]
  (let [id (id-of-text (:permission row))]
    (when (uuid? id) id)))

(defn- says-of
  "What a row's value says as a permission, read from its text."
  [row]
  (when-let [held (:value row)]
    (declaration/permission-value (first-form held))))

(defn read-as-a-permission
  "A row that was read at a permission's place, with what it says beside
  its fields, so that the text is read once however often
  `held-permission` is asked of the row. The row is for the checks of the
  one read that made it, and is never written: a gate writes only rows
  that the functions above made. nil for no row."
  [row]
  (when row
    (assoc row ::says (says-of row))))

(defn permission-place?
  "True for the key of a place under the permission key."
  [k]
  (and (string? k)
       (.endsWith ^String k (str " " (declaration/id-text declaration/permission-key)))))

(defn held-permission
  "What the permission at a place says: the value of the row at a
  permission's place, in its one form
  (`softland.store.door.declaration/permission-value`). nil for a row with
  no value, which is the end of a permission or its use; for a row whose
  value is no permission's, which no gate writes there; and for no row.
  `permission-of` is another thing: the permission that the writing act
  named.

  It never throws."
  [row]
  (if (contains? row ::says)
    (::says row)
    (says-of row)))

(defn used?
  "The use of a permission for one exact act: a row with no value that no
  fact wrote (`use-row`). What must hold: the row is at a permission's
  place."
  [row]
  (boolean (and row (nil? (:value row)) (nil? (:by-index row)))))

(defn ended?
  "An end: a row with no value that a taken fact with no value wrote. What
  must hold: the row is at a permission's place."
  [row]
  (boolean (and row (nil? (:value row)) (some? (:by-index row)))))

(defn marked-keys-of
  "For the row at a lane's marked-keys place. A read of \"what points at a
  thing\" reads it first: the stamp is a part of each key of that order.

  It never throws."
  [row]
  (let [held (when-let [text (:level-since row)] (first-form text))]
    (if (and (map? held)
             (every? (fn [[k since]] (and (act/readable-keyword? k) (int? since))) held))
      held
      {})))

;; ------------------------------------------------------- the facts by value

(defn value-digest
  "The SHA-256 of a value's plain bytes, as 64 hex characters. So a key has a
  bound whatever the value is, a value's text may hold a space, and two
  values share a run only by a collision of the hash. Every JVM has the
  algorithm. Cost: one hash over the bytes, 65,536 at most for a value that
  a gate accepted under a plain key
  (`softland.store.door.declaration/max-value-bytes`)."
  [value-bytes]
  (text/hex (.digest (MessageDigest/getInstance "SHA-256") ^bytes value-bytes)))

(defn value-digest-of
  "A UUID is always taken as `#uuid \"...\"`, never as its 36 characters, and
  no caller chooses a text. What must hold: the thing is a value
  (`softland.store.act/value?`)."
  [v]
  (value-digest (text/utf8 (act/normal-value v))))

(defn by-value-key
  "The entity is last, so the places that hold one value under one key are
  neighbours. No part before the entity holds a space, and the digest has a
  fixed length, so a key begins with a run's text only when its lane, key,
  stamp and digest are that run's.

  `since` is the stamp that the marks row gives for the key
  (`marked-keys-of`): the order of a marked key holds every fact under that
  key whose stamp is later than it, and no other. A mark that is taken off
  and put on again gets another stamp, so it names other keys; the rows of
  the earlier marking stay where they are and no read names them. A retract
  has no row in this order.

  What must hold: the digest is `value-digest`'s."
  [lane k since digest entity]
  (str (declaration/id-text lane) " " (declaration/id-text k) " " (long since) " " digest " "
       (declaration/id-text entity)))

(defn by-value-run
  "Every key from `from` up to `to`, with `to` not in the run, is a place of
  that lane whose current fact under that key has that value and was
  written since that stamp. `to` is the run's text with `!` in the place of
  the space at its end: `!` is the character after a space, so the range
  holds the keys that begin with `from` and no other. `since` is the key's
  stamp as the marks row gives it (`marked-keys-of`)."
  [lane k since v]
  (let [run (str (declaration/id-text lane) " " (declaration/id-text k) " " (long since) " "
                 (value-digest-of v))]
    [(str run " ") (str run "!")]))

(defn- parts-of-key
  [k]
  (vec (.split ^String k " " -1)))

(defn by-value-row
  "The fact's row without its value. So a read of a run gets who wrote each
  place, under which permission and at what time with no second read, and a
  fact that is written again with the same value changes the row. What must
  hold: the two texts and the time are those that the fact's row holds
  (`fact-row`)."
  [who-text permission-text act-uuid index stamp time-millis]
  (cond-> {:by-act act-uuid :by-index (long index) :stamp (long stamp)
           :time (long time-millis) :who who-text}
    (some? permission-text) (assoc :permission permission-text)))

(defn id-of-text
  "It never throws."
  [id-text]
  (try
    (if (and (string? id-text) (.startsWith ^String id-text ":"))
      (let [k (keyword (subs id-text 1))]
        (when (and (act/readable-keyword? k) (= id-text (str k))) k))
      (let [uuid (UUID/fromString id-text)]
        (when (= id-text (str uuid)) uuid)))
    (catch Throwable _ nil)))

(defn place-of-key
  "It never throws."
  [place-key-text]
  (try
    (let [parts (mapv id-of-text (parts-of-key place-key-text))]
      (when (and (= 3 (count parts)) (every? some? parts) (keyword? (nth parts 2)))
        parts))
    (catch Throwable _ nil)))

(defn by-value-entity
  "It never throws."
  [k]
  (try
    (let [parts (parts-of-key k)]
      (when (= 5 (count parts)) (id-of-text (nth parts 4))))
    (catch Throwable _ nil)))

(defn answer-key-parts
  "It never throws."
  [answer-key-text]
  (try
    (let [parts (parts-of-key answer-key-text)]
      (when (and (= 2 (count parts)) (seq (nth parts 0)))
        (let [uuid (UUID/fromString (nth parts 1))]
          (when (= (nth parts 1) (str uuid))
            [(nth parts 0) uuid]))))
    (catch Throwable _ nil)))

;; ----------------------------------------------------------------- the line

(def ^:private stamp-zeros
  "A stamp in a key is written with zeros in front to 19 digits, the digits
  of 2^63 - 1, the greatest stamp (`softland.store.gate.stamp`). Rama keeps
  the String keys of a table in the order of their bytes, where \"10\" is
  before \"9\"; with a fixed width the order of the keys of one lane is the
  order of its stamps."
  "0000000000000000000")

(defn- stamp-text
  "What must hold: the stamp is a whole number from 0 to 2^63 - 1."
  [stamp]
  (let [s (Long/toString (long stamp))]
    (str (subs stamp-zeros (count s)) s)))

(defn line-key
  "The key of a decision in the lane's line, and of its thing."
  [lane stamp]
  (str (declaration/id-text lane) " " (stamp-text stamp)))

(defn line-run
  "The lane's line from that stamp on, in the order of the stamps; `to` is
  not in the run."
  [lane stamp]
  [(line-key lane stamp) (str (declaration/id-text lane) "!")])

(defn by-who-key
  [lane who stamp]
  (str (declaration/id-text lane) " " (declaration/id-text who) " " (stamp-text stamp)))

(defn by-who-run
  "One who's rows by who in a lane from that stamp on; `to` is not in the
  run. No printed text of an id holds a space, so the run holds no row of
  another who whose text begins with this one's."
  [lane who stamp]
  (let [run (str (declaration/id-text lane) " " (declaration/id-text who))]
    [(str run " " (stamp-text stamp)) (str run "!")]))

(defn- stamp-at-the-end
  "It never throws."
  [k]
  (try
    (let [^String k k
          i (.lastIndexOf k " ")]
      (when (and (pos? i) (= 19 (- (.length k) i 1)))
        (let [digits (subs k (inc i))]
          (when (every? #(Character/isDigit ^char %) digits)
            (Long/parseLong digits)))))
    (catch Throwable _ nil)))

(defn stamp-of-line-key
  "Also for a key of the things.

  It never throws."
  [k]
  (stamp-at-the-end k))

(defn stamp-of-by-who-key
  "It never throws."
  [k]
  (stamp-at-the-end k))

(defn line-row
  "`:digest` is hex text, where the answer's row holds the 32 bytes given:
  no field of the line is a byte array
  (`softland.store.record.tables/line`). `:weight` only when the gate
  keeps the thing; `:not-kept`, `:too-heavy` or `:not-weighed`, only when it
  does not. `:refused` and `:time` are those of the answer's row under the
  act-id, made of the same values in the same decision. `:who` is the
  printed text of the who that the thing names; absent when it names no
  usable one.

  What must hold: the who is nil or a usable id
  (`softland.store.door.declaration/usable-id?`)."
  [act-uuid refused time-millis digest who weight not-kept]
  (cond-> {:act act-uuid :digest (text/hex digest)}
    refused (assoc :refused refused)
    (some? time-millis) (assoc :time (long time-millis))
    (some? who) (assoc :who (declaration/id-text who))
    (some? weight) (assoc :weight (long weight))
    not-kept (assoc :not-kept not-kept)))

(defn line-answer
  "The answer as `answer-of` gives it of the answer's row."
  [stamp row]
  {:verdict (if (:refused row) :refused :accepted)
   :reason  (:refused row)
   :stamp   (long stamp)
   :time    (:time row)
   :digest  (:digest row)})

;; ------------------------------------------------ the permissions by holder

(def any-sitting
  "What stands in a key by holder where a sitting would, for a permission
  that is good in every sitting. The printed text of an id begins with a
  colon or is a UUID's, so no sitting has this text."
  "*")

(defn by-holder-key
  "The key of a permission in the order by holder: the lane, the holder,
  the one sitting that the permission is good for or `any-sitting`, and the
  permission's own id. So the permissions of one holder in one lane are
  neighbours, those for any sitting in one run and those for each sitting
  in a run of its own, and a read finds them without knowing an id."
  [lane holder session permission]
  (str (declaration/id-text lane) " " (declaration/id-text holder) " "
       (if (some? session) (declaration/id-text session) any-sitting) " "
       permission))

(defn by-holder-run
  "The keys of the permissions that one holder has in a lane for one
  sitting, or with nil for any sitting; `to` is not in the run."
  [lane holder session]
  (let [run (str (declaration/id-text lane) " " (declaration/id-text holder) " "
                 (if (some? session) (declaration/id-text session) any-sitting))]
    [(str run " ") (str run "!")]))

(defn permission-of-by-holder-key
  "The id of the permission that a key by holder names, its last part.

  It never throws."
  [k]
  (try
    (let [parts (parts-of-key k)]
      (when (= 4 (count parts))
        (let [id (id-of-text (nth parts 3))]
          (when (uuid? id) id))))
    (catch Throwable _ nil)))

;; ------------------------------------------------ the runs that a read names

(defn entity-run
  "`to` is `from` with `!` in the place of its last space: `!` is the
  character after a space, so the run holds no place of an entity whose
  text only begins with this entity's text. `to` is not in the run."
  [lane entity]
  (let [run (str (declaration/id-text lane) " " (declaration/id-text entity))]
    [(str run " ") (str run "!")]))

(defn own-rows-run
  "The lane's own rows: those of the lane's own entity under the keys of the
  store's namespace (`softland.store.door.declaration/store-namespace`).
  They are the lane's depot, who may read its acts whole, what it builds
  on and its marked keys; and a permission, when its giver made it on the
  id that is also the lane's.

  `to` is `from` with `0`, the character after `/`, in the place of its
  last character, and is not in the run."
  [lane]
  (let [run (str (declaration/id-text lane) " " (declaration/id-text lane) " :" declaration/store-namespace)]
    [(str run "/") (str run "0")]))

(defn before?
  "The order in which Rama keeps the String keys of a table: by code point,
  which is the order of the bytes of UTF-8. Not Java's `compareTo`, which
  compares UTF-16 units and so puts a letter outside the first plane before
  U+E000.

  That a table goes through its keys in this order was run on a probe's
  table and on the store's own. For a key that holds a letter from U+E000
  on, or one outside the first plane, Rama was also seen to use Java's
  order: at the end of a range, and in the map that its client makes of a
  run. A reader guards against that where it reads
  (`softland.store.read-path.sources`)."
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
  "The two ends are as a query names them. A place as the last end gives the
  place's key and one space, so that the place is in the run; `to` is not
  in it. Both texts begin with the lane's text and a space or `!`, so a run
  never leaves its lane."
  [lane first-end last-end]
  (let [l (declaration/id-text lane)
        from (cond
               (= :first first-end) (str l " ")
               (and (vector? first-end) (= 1 (count first-end))) (first (entity-run lane (nth first-end 0)))
               (and (vector? first-end) (= 2 (count first-end))) (place-key lane (nth first-end 0) (nth first-end 1)))
        to (cond
             (= :last last-end) (str l "!")
             (and (vector? last-end) (= 1 (count last-end))) (second (entity-run lane (nth last-end 0)))
             (and (vector? last-end) (= 2 (count last-end))) (str (place-key lane (nth last-end 0) (nth last-end 1)) " "))]
    (when (and from to (not (before? to from)))
      [from to])))

;; ------------------------------------------------- one row against another

(defn same-row?
  "Each field of a row of the facts or of the facts by value is a Long, a
  UUID or a String, and none a byte array, so the equality of the two maps
  says it. A reader asks it of the row before and the row after a change: a
  row that a second attempt of a batch wrote again is the same row.

  It never throws."
  [row-a row-b]
  (try
    (= row-a row-b)
    (catch Throwable _ false)))

(defn holds-this-value?
  "The test by which a gate puts a fact in a value's order by value. A reader
  asks it of a nearer lane's row at a place that a farther lane's order
  gives: the place is given from the nearer row exactly when a mark would
  have put that row in the nearer lane's own order for the value. What must
  hold: the value passes `softland.store.act/value?`.

  It never throws."
  [row v]
  (try
    (boolean (and row
                  (some? (:value row))
                  (= (value-digest (plain-bytes (:value row))) (value-digest-of v))))
    (catch Throwable _ false)))

;; ------------------------------------------- what a caller gave a read

(defn answer-read
  "The test at the head of the read by act-id: nil when no read is to be
  made.

  It never throws."
  [lane act-id]
  (when (and (declaration/usable-id? lane)
             (declaration/act-id-in-lane? lane act-id))
    [(nth act-id 1) (answer-key lane (nth act-id 2))]))

(defn depot-read
  "The test at the head of the read of a lane's depot: nil when the thing is
  no id within the limit.

  It never throws."
  [lane]
  (when (declaration/usable-id? lane)
    (place-key lane lane :softland.store/depot)))

(defn routing-id
  "The first argument of the read by act-id: the act-id's lane, for either
  depot. Rama's client hashes it, so the read goes straight to the lane's
  task, where the answer is in either gate's record.

  It never throws."
  [act-id]
  (when (and (vector? act-id)
             (declaration/act-id-in-lane? (nth act-id 0 nil) act-id))
    (nth act-id 0)))

(defn answer-read-by-routing-id
  "The test at the head of the query by act-id: nil also when the routing id
  is not the act-id's lane.

  It never throws."
  [given act-id]
  (let [own (routing-id act-id)]
    (when (and (some? own) (= own given))
      (answer-read (nth act-id 0) act-id))))
