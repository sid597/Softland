(ns softland.store.door.text
  "The one text of a thing: one function from anything to a string, the same
  in every process. Pure code. It needs no cluster.

  Why it is here. The digest of a thing is taken over this text
  (`softland.store.door.digest`), so that the door and a gate get one digest for
  one thing. The record keeps a value by this text
  (`softland.store.record.rows`): a fact's row holds the text itself, a
  String, of the value in its normal form. The last step of the
  declaration's check makes it, once, and counts its bytes
  (`softland.store.door.declaration`). A fact's row holds who wrote it and
  the permission that its act named as this text too, and the order by
  value is kept by the digest of the UTF-8 bytes of this text.

  Inputs and results, by call:

    text  anything -> its one text, a string
    utf8  anything -> the UTF-8 bytes of its one text
    utf8-count  a string -> how many bytes its UTF-8 has
    feed  anything, two functions that take the pieces, and a third that
          takes each part whose text does not hold what it holds -> nil
    hex   a run of bytes -> its hex text

  The text of each kind of thing:

    nil, true, false          nil  true  false
    a string                  in double quotes, escaped as Clojure prints it
    a readable keyword        its printed form, :tool/class
    a UUID                    #uuid \"...\"
    a fixed-width integer     its digits
    a big integer             its digits and N
    a double                  Double/toString, with -0.0 as 0.0; what is not
                              finite as #double \"NaN\" and the like
    a decimal                 its text without trailing zeros, and M
    a run of bytes            #bytes, the count, and the bytes as hex in a
                              string
    a vector                  [ its parts ]
    any other sequence        ( its parts )
    a set                     #{ the texts of its parts, sorted }
    a plain map               { its entries, sorted by the text of the key,
                              and by the text of the value where two keys
                              have one text }
    any other keyword         #keyword, its namespace and its name as two
                              strings
    anything else             #other and the name of its class: a record, a
                              symbol, a ratio, a float, a Java collection
    a part inside more than
    `max-depth` collections   #too-deep
    a thing that throws
    while it is read          the whole text is #unreadable

  Two properties. The same thing has the same text everywhere: nothing in
  the text follows the order of a hash map, the identity of an object or the
  process. Two things that a check of the act or of the door's declaration
  can tell apart have two texts. Where no check can tell two things apart
  the text may be the same, on purpose: an Integer and a Long of one value,
  -0.0 and 0.0, a subvector and a vector, two objects of a class that the
  form does not know. A serialiser may change exactly those, and a text that
  kept them apart would give the door and the gate two digests for one
  thing.

  For a value that a fact can hold (`softland.store.act/value?`) the text is
  EDN, and `clojure.edn` reads it back.

  What a text does not hold. The text of a part that is written as #other or
  #too-deep is a few bytes, whatever the part holds, and the text of a
  decimal leaves out its trailing zeros, whatever its digits hold. `feed`
  hands each such part to a third function, so that one walk that feeds a
  digest can also weigh the thing
  (`softland.store.door.digest/weighed`); `text` hands them to none.

  State and effects. None. Each function is pure.

  Limit. A sequence with no end does not come back from it. A thing that
  came off a depot has an end."
  (:require [softland.store.act :as act])
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]
           [java.nio.charset StandardCharsets]))

(def max-depth
  "How many collections a part can be inside and still be written out. A
  part that is deeper is written as #too-deep. A value of the form is inside
  32 collections at most (`softland.store.act/max-depth`), and it sits in a
  fact, in the facts, in the act; so each thing that a check accepts is
  written out whole."
  64)

(def ^:private hex-digits
  "The sixteen digits of the hex text of a byte."
  "0123456789abcdef")

(defn hex
  "Takes a run of bytes. Gives its hex text, two lower-case digits for each
  byte. What must hold: x is a byte array."
  [x]
  (let [^bytes b x
        sb (StringBuilder. (* 2 (alength b)))]
    (dotimes [i (alength b)]
      (let [v (bit-and 0xff (aget b i))]
        (.append sb (.charAt ^String hex-digits (bit-shift-right v 4)))
        (.append sb (.charAt ^String hex-digits (bit-and v 0xf)))))
    (.toString sb)))

(defn- quoted
  "Takes a string. Gives it in double quotes, escaped as Clojure prints a
  string that is to be read back. It does not use the printer, so no
  binding of a print var changes the text."
  [x]
  (let [^String s x
        sb (StringBuilder. (+ 2 (.length s)))]
    (.append sb \")
    (dotimes [i (.length s)]
      (let [c (.charAt s i)]
        (if-let [escaped (char-escape-string c)]
          (.append sb ^String escaped)
          (.append sb c))))
    (.append sb \")
    (.toString sb)))

(defn- double-text
  "Takes a double. Gives its text: -0.0 as 0.0, and a double that is not
  finite as #double with its name in a string."
  [x]
  (let [d (double x)]
    (cond
      (Double/isNaN d) "#double \"NaN\""
      (Double/isInfinite d) (if (pos? d) "#double \"Infinity\"" "#double \"-Infinity\"")
      (zero? d) "0.0"
      :else (Double/toString d))))

(defn- scalar-text
  "Takes a thing that is no collection and no run of bytes. Gives its text,
  as the table of the namespace docstring has it. A thing of a class that
  the form does not know gives #other and the name of its class."
  [x]
  (cond
    (nil? x) "nil"
    (true? x) "true"
    (false? x) "false"
    (string? x) (quoted x)
    (keyword? x) (if (act/readable-keyword? x)
                   (str x)
                   (str "#keyword " (if-let [n (namespace x)] (quoted n) "nil") " " (quoted (name x))))
    (uuid? x) (str "#uuid \"" x "\"")
    (int? x) (Long/toString (long x))
    (instance? BigInt x) (str (.toBigInteger ^BigInt x) "N")
    (instance? BigInteger x) (str x "N")
    (instance? Double x) (double-text x)
    (decimal? x) (str (.stripTrailingZeros ^BigDecimal x) "M")
    :else (str "#other " (.getName (class x)))))

(defn- written-whole?
  "Takes a thing that is no collection and no run of bytes. Gives true when
  its text holds what it holds: nil, a boolean, a string, a keyword, a UUID,
  an integer of any width, a double. A decimal is not, since its text
  leaves out its trailing zeros; nor is a thing of any class that the text
  writes as #other."
  [x]
  (boolean (or (nil? x) (boolean? x) (string? x) (keyword? x) (uuid? x) (int? x)
               (instance? BigInt x) (instance? BigInteger x) (instance? Double x))))

(defn- none
  "The third function of `feed` that takes nothing: what `text` hands."
  [_]
  nil)

(declare feed*)

(defn- text*
  "Takes a thing, the number of collections around it, and the third
  function of `feed`. Gives its text as one string. It can throw, as
  `feed*` can."
  [x depth take-held]
  (let [sb (StringBuilder.)]
    (feed* x depth
           (fn [^String s] (.append sb s))
           (fn [b] (.append sb (quoted (hex b))))
           take-held)
    (.toString sb)))

(defn- feed-parts
  "Feeds an opening mark, the parts with one space between them, and a
  closing mark. Each part is fed by `feed*`, one collection deeper."
  [open parts close depth take-text take-bytes take-held]
  (take-text open)
  (loop [parts (seq parts) first? true]
    (when parts
      (when-not first? (take-text " "))
      (feed* (first parts) (inc depth) take-text take-bytes take-held)
      (recur (next parts) false)))
  (take-text close))

(defn- feed-map
  "Feeds a plain map: its entries sorted by the text of the key. The text of
  a value is built only where two keys have one text, to order those
  entries; so a large value is fed piece by piece and never built as one
  string. Each key is walked once, when its text is made, and each value
  once, when it is fed: the text that orders two tied entries hands no
  part to `take-held`, since the value is fed after."
  [m depth take-text take-bytes take-held]
  (let [keyed (map (fn [[k v]] [(text* k (inc depth) take-held) v]) m)
        tied? (and (seq keyed) (not (apply distinct? (map first keyed))))
        ordered (if tied?
                  (sort-by (fn [[kt v]] [kt (text* v (inc depth) none)]) keyed)
                  (sort-by first keyed))]
    (take-text "{")
    (loop [entries (seq ordered) first? true]
      (when entries
        (let [[kt v] (first entries)]
          (when-not first? (take-text " "))
          (take-text kt)
          (take-text " ")
          (feed* v (inc depth) take-text take-bytes take-held))
        (recur (next entries) false)))
    (take-text "}")))

(defn- feed*
  "The walk. Takes a thing, the number of collections around it, and the
  three functions of `feed`. It can throw, for a thing that throws when it
  is read."
  [x depth take-text take-bytes take-held]
  (cond
    (> depth max-depth) (do (take-held x) (take-text "#too-deep"))
    (bytes? x) (do (take-text (str "#bytes " (alength ^bytes x) " "))
                   (take-bytes x))
    (vector? x) (feed-parts "[" x "]" depth take-text take-bytes take-held)
    (record? x) (do (take-held x) (take-text (scalar-text x)))
    (map? x) (feed-map x depth take-text take-bytes take-held)
    (set? x) (do (take-text "#{")
                 (take-text (apply str (interpose " " (sort (map #(text* % (inc depth) take-held) x)))))
                 (take-text "}"))
    (sequential? x) (feed-parts "(" x ")" depth take-text take-bytes take-held)
    :else (do (when-not (written-whole? x) (take-held x))
              (take-text (scalar-text x)))))

(defn feed
  "Walks a thing and gives its one text piece by piece, in order. Takes
  anything, and two functions: `take-text` is called with each piece that is
  text, a string; `take-bytes` is called with each run of bytes that the
  thing holds, the array itself, right after the piece `#bytes n `. The text
  as a string has the bytes as hex in double quotes at that point; a digest
  takes the bytes as they are. One walk feeds both, so the two cannot drift
  apart. Gives nil.

  A third function, `take-held`, when it is given, is called with each part
  whose text does not hold what the part holds, right before that part's
  text: a part inside more than `max-depth` collections, before its
  #too-deep; a record, and a thing of a class that the text writes as
  #other, before its #other; a decimal, before its text. It is called once
  for each such part, in the order of the walk, and with no part inside one
  that it was called with: the text does not walk into it. With two
  functions it is called with nothing.

  It can throw, for a thing that throws when it is read. `text` and
  `softland.store.door.digest/digest` turn a throw into #unreadable."
  ([x take-text take-bytes]
   (feed x take-text take-bytes none))
  ([x take-text take-bytes take-held]
   (feed* x 0 take-text take-bytes take-held)
   nil))

(defn text
  "Takes anything. Gives its one text, a string. It never throws: a thing
  that throws while it is read has the text #unreadable."
  [x]
  (try (text* x 0 none)
       (catch Throwable _ "#unreadable")))

(defn utf8
  "Takes anything. Gives the UTF-8 bytes of its one text, as a new array."
  [x]
  (.getBytes ^String (text x) StandardCharsets/UTF_8))

(defn utf8-count
  "Takes a string. Gives how many bytes its UTF-8 has, the length of what
  `String.getBytes` gives for UTF-8, with no array made: one walk of the
  characters. A character below U+0080 is 1 byte, one below U+0800 is 2, a
  pair of surrogates is 4, and any other is 3. Half of a surrogate pair
  that stands alone counts 1: the JVM writes it as `?`. The declaration's
  check counts the text of a value with it. What must hold: x is a
  String."
  [x]
  (let [^String s x
        n (.length s)]
    (loop [i 0 total 0]
      (if (>= i n)
        total
        (let [c (.charAt s i)
              code (int c)]
          (cond
            (< code 0x80) (recur (inc i) (inc total))
            (< code 0x800) (recur (inc i) (+ total 2))
            (Character/isHighSurrogate c)
            (if (and (< (inc i) n) (Character/isLowSurrogate (.charAt s (inc i))))
              (recur (+ i 2) (+ total 4))
              (recur (inc i) (inc total)))
            (Character/isLowSurrogate c) (recur (inc i) (inc total))
            :else (recur (inc i) (+ total 3))))))))
