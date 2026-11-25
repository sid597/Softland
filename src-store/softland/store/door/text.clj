(ns softland.store.door.text
  "The one text of a thing: one function from anything to a string, the same
  in every process. Pure code.

  The digest of a thing is taken over this text
  (`softland.store.door.digest`), so that the door and a gate get one digest
  for one thing. A fact's row holds the text of its value, of who wrote it
  and of the permission that its act named, and the order by value is kept
  by the digest of the UTF-8 bytes of this text.

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

  Limit. A sequence with no end does not come back from it. A thing that
  came off a depot has an end."
  (:require [softland.store.act :as act])
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]
           [java.nio.charset StandardCharsets]))

(def max-depth
  "A part inside more collections is written as #too-deep. A value of the
  form is inside 32 collections at most (`softland.store.act/max-depth`),
  and it sits in a fact, in the facts, in the act; so each thing that a
  check accepts is written out whole."
  64)

(def ^:private hex-digits
  "0123456789abcdef")

(defn hex
  "Two lower-case digits for each byte. What must hold: x is a byte array."
  [x]
  (let [^bytes b x
        sb (StringBuilder. (* 2 (alength b)))]
    (dotimes [i (alength b)]
      (let [v (bit-and 0xff (aget b i))]
        (.append sb (.charAt ^String hex-digits (bit-shift-right v 4)))
        (.append sb (.charAt ^String hex-digits (bit-and v 0xf)))))
    (.toString sb)))

(defn- quoted
  "In double quotes, escaped as Clojure prints a string that is to be read
  back. It does not use the printer, so no binding of a print var changes
  the text."
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
  [x]
  (let [d (double x)]
    (cond
      (Double/isNaN d) "#double \"NaN\""
      (Double/isInfinite d) (if (pos? d) "#double \"Infinity\"" "#double \"-Infinity\"")
      (zero? d) "0.0"
      :else (Double/toString d))))

(defn- scalar-text
  "What must hold: x is no collection and no run of bytes."
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
  "True when the text of x holds what x holds. A decimal is not, since its
  text leaves out its trailing zeros; nor is a thing of any class that the
  text writes as #other. What must hold: x is no collection and no run of
  bytes."
  [x]
  (boolean (or (nil? x) (boolean? x) (string? x) (keyword? x) (uuid? x) (int? x)
               (instance? BigInt x) (instance? BigInteger x) (instance? Double x))))

(defn- none
  "The third function of `feed` that takes nothing: what `text` hands."
  [_]
  nil)

(declare feed*)

(defn- text*
  "`depth` is the number of collections around x. It can throw, as `feed*`
  can."
  [x depth take-held]
  (let [sb (StringBuilder.)]
    (feed* x depth
           (fn [^String s] (.append sb s))
           (fn [b] (.append sb (quoted (hex b))))
           take-held)
    (.toString sb)))

(defn- feed-parts
  [open parts close depth take-text take-bytes take-held]
  (take-text open)
  (loop [parts (seq parts) first? true]
    (when parts
      (when-not first? (take-text " "))
      (feed* (first parts) (inc depth) take-text take-bytes take-held)
      (recur (next parts) false)))
  (take-text close))

(defn- feed-map
  "Entries are sorted by the text of the key, and by the text of the value
  where two keys have one text. The text of a value is built only for such
  a tie; so a large value is fed piece by piece and never built as one
  string. The text that orders two tied entries hands no part to
  `take-held`, since the value is fed after."
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
  "The walk. It can throw, for a thing that throws when it is read."
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
  "Gives the one text of a thing piece by piece, in order. `take-text` is
  called with each piece that is text; `take-bytes` with each run of bytes
  that the thing holds, the array itself, right after the piece `#bytes n `.
  The text as a string has the bytes as hex in double quotes at that point;
  a digest takes the bytes as they are. One walk feeds both, so the two
  cannot drift apart.

  `take-held`, when it is given, is called with each part whose text does
  not hold what the part holds, right before that part's text: a part
  inside more than `max-depth` collections, a record, a thing of a class
  that the text writes as #other, a decimal. It is called once for each
  such part, in the order of the walk, and with no part inside one that it
  was called with: the text does not walk into it.

  It can throw, for a thing that throws when it is read. `text` and
  `softland.store.door.digest/digest` turn a throw into #unreadable."
  ([x take-text take-bytes]
   (feed x take-text take-bytes none))
  ([x take-text take-bytes take-held]
   (feed* x 0 take-text take-bytes take-held)
   nil))

(defn feed-part
  "The walk of `feed` for one part of a thing. `depth` is the number of
  collections around the part in the thing, so the pieces are the part's
  text inside the thing's one text: a part deeper than `max-depth`
  collections is written #too-deep as it is there. It can throw, as `feed`
  can."
  [x depth take-text take-bytes take-held]
  (feed* x depth take-text take-bytes take-held)
  nil)

(defn text
  "It never throws: a thing that throws while it is read has the text
  #unreadable."
  [x]
  (try (text* x 0 none)
       (catch Throwable _ "#unreadable")))

(defn utf8
  [x]
  (.getBytes ^String (text x) StandardCharsets/UTF_8))

(defn utf8-count
  "How many bytes the UTF-8 of a string has, the length of what
  `String.getBytes` gives for UTF-8, with no array made. Half of a surrogate
  pair that stands alone counts 1: the JVM writes it as `?`. What must
  hold: x is a String."
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
