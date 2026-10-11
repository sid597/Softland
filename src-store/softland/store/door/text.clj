(ns softland.store.door.text
  "The one text of a thing: one function from anything to a string, the same
  in every process. Pure code. It needs no cluster.

  Why it is here. The digest of a thing is taken over this text
  (`softland.store.door.digest`), so that the door and a gate get one digest for
  one thing. The record keeps a value as the bytes of this text
  (`softland.store.record.rows`): a value under a store key as a gate writes
  them, and a value under a plain key as the door sealed them and its lock
  opened them. A fact's row holds who wrote it and the permission that its
  act named as this text too, and the order by value is kept by the digest
  of these bytes. The place that a seal is bound to is the bytes of this
  text (`softland.store.door.seal`).

  Inputs and results, by call:

    text  anything -> its one text, a string
    utf8  anything -> the UTF-8 bytes of its one text
    feed  anything, and two functions that take the pieces -> nil
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

(declare feed*)

(defn- text*
  "Takes a thing and the number of collections around it. Gives its text as
  one string. It can throw, as `feed*` can."
  [x depth]
  (let [sb (StringBuilder.)]
    (feed* x depth
           (fn [^String s] (.append sb s))
           (fn [b] (.append sb (quoted (hex b)))))
    (.toString sb)))

(defn- feed-parts
  "Feeds an opening mark, the parts with one space between them, and a
  closing mark. Each part is fed by `feed*`, one collection deeper."
  [open parts close depth take-text take-bytes]
  (take-text open)
  (loop [parts (seq parts) first? true]
    (when parts
      (when-not first? (take-text " "))
      (feed* (first parts) (inc depth) take-text take-bytes)
      (recur (next parts) false)))
  (take-text close))

(defn- feed-map
  "Feeds a plain map: its entries sorted by the text of the key. The text of
  a value is built only where two keys have one text, to order those
  entries; so a large value, such as the sealed bytes of a fact, is fed
  piece by piece and never built as one string."
  [m depth take-text take-bytes]
  (let [keyed (map (fn [[k v]] [(text* k (inc depth)) v]) m)
        tied? (and (seq keyed) (not (apply distinct? (map first keyed))))
        ordered (if tied?
                  (sort-by (fn [[kt v]] [kt (text* v (inc depth))]) keyed)
                  (sort-by first keyed))]
    (take-text "{")
    (loop [entries (seq ordered) first? true]
      (when entries
        (let [[kt v] (first entries)]
          (when-not first? (take-text " "))
          (take-text kt)
          (take-text " ")
          (feed* v (inc depth) take-text take-bytes))
        (recur (next entries) false)))
    (take-text "}")))

(defn- feed*
  "The walk. Takes a thing, the number of collections around it, and the two
  functions of `feed`. It can throw, for a thing that throws when it is
  read."
  [x depth take-text take-bytes]
  (cond
    (> depth max-depth) (take-text "#too-deep")
    (bytes? x) (do (take-text (str "#bytes " (alength ^bytes x) " "))
                   (take-bytes x))
    (vector? x) (feed-parts "[" x "]" depth take-text take-bytes)
    (record? x) (take-text (scalar-text x))
    (map? x) (feed-map x depth take-text take-bytes)
    (set? x) (do (take-text "#{")
                 (take-text (apply str (interpose " " (sort (map #(text* % (inc depth)) x)))))
                 (take-text "}"))
    (sequential? x) (feed-parts "(" x ")" depth take-text take-bytes)
    :else (take-text (scalar-text x))))

(defn feed
  "Walks a thing and gives its one text piece by piece, in order. Takes
  anything, and two functions: `take-text` is called with each piece that is
  text, a string; `take-bytes` is called with each run of bytes that the
  thing holds, the array itself, right after the piece `#bytes n `. The text
  as a string has the bytes as hex in double quotes at that point; a digest
  takes the bytes as they are. One walk feeds both, so the two cannot drift
  apart. Gives nil.

  It can throw, for a thing that throws when it is read. `text` and
  `softland.store.door.digest/digest` turn a throw into #unreadable."
  [x take-text take-bytes]
  (feed* x 0 take-text take-bytes)
  nil)

(defn text
  "Takes anything. Gives its one text, a string. It never throws: a thing
  that throws while it is read has the text #unreadable."
  [x]
  (try (text* x 0)
       (catch Throwable _ "#unreadable")))

(defn utf8
  "Takes anything. Gives the UTF-8 bytes of its one text, as a new array."
  [x]
  (.getBytes ^String (text x) StandardCharsets/UTF_8))
