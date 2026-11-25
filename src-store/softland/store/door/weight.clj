(ns softland.store.door.weight
  "The weight of what a part of a thing holds, for a part whose one text
  does not say it. Pure code.

  A gate keeps each thing that it decides a second time, for the read by
  stamp, when the thing weighs no more than a bound
  (`softland.store.gate.decision`, `max-kept-weight`). The weight of a thing
  is the bytes of the UTF-8 of its one text, counted in the walk that takes
  its digest (`softland.store.door.digest/weighed`). The text of a part
  that no check takes is a few bytes, whatever the part holds, and the text
  of a decimal has no trailing zeros, whatever its digits hold. So the walk
  hands each of those parts here.

  Every part weighs 1 at least, and the walk stops as soon as the sum
  passes its bound: so the work of one call is bounded by its bound, also
  for a part that holds no end. An array of a primitive is weighed from its
  length, with no walk. A big integer and a decimal are weighed from the
  length of their bits, with no division and no text: the text of a
  decimal strips its trailing zeros, and that costs time that grows with
  the square of its digits (run on JDK 21 on 22 November 2026: 8.2 seconds
  for a decimal of 83 KB).

  The weight is a measure of the gate's own, and not the bytes that Rama
  writes: Rama gives a topology no call that says the bytes of a value. It
  is the same in every process for the same thing.

  The function never throws: a part that throws while it is walked is
  `:not-weighed`."
  (:require [softland.store.door.text :as text])
  (:import [clojure.lang BigInt IPersistentMap IRecord Keyword Ratio Symbol]
           [java.math BigDecimal BigInteger]
           [java.util ArrayList Arrays Collection Iterator Map Map$Entry UUID]))

(deftype ^:private Rest [^Iterator it])

(def ^:private primitive-widths
  "The width in bytes of one item of each array of a primitive."
  {(Class/forName "[B") 1 (Class/forName "[Z") 1
   (Class/forName "[C") 2 (Class/forName "[S") 2
   (Class/forName "[I") 4 (Class/forName "[F") 4
   (Class/forName "[J") 8 (Class/forName "[D") 8})

(def ^:private object-array-class (Class/forName "[Ljava.lang.Object;"))

(defn- text-weight
  "The bytes of the UTF-8 of a string, or one more than what is left when
  its length alone passes that: UTF-8 has one byte at least for each
  character, so the walk of a long string stops before it begins."
  ^long [^String s ^long left]
  (if (> (.length s) left)
    (inc left)
    (long (text/utf8-count s))))

(defn- big-integer-weight
  "Its bits, in whole bytes, and one: what its two's complement holds.
  `bitLength` reads the length and walks no digit."
  ^long [^BigInteger b]
  (inc (quot (.bitLength b) 8)))

(defn- part-weight
  "The part's own weight; what the part holds goes on the stack. nil for a
  part of a class that this does not weigh. `left` is how much weight is
  left."
  [x ^ArrayList stack ^long left]
  (cond
    (nil? x) 1
    (instance? Boolean x) 1
    (instance? String x) (+ 2 (text-weight x left))
    (instance? Keyword x) (+ 1 (text-weight (name x) left)
                             (if-let [n (namespace x)] (inc (text-weight n left)) 0))
    (instance? Symbol x) (+ 1 (text-weight (name x) left)
                            (if-let [n (namespace x)] (inc (text-weight n left)) 0))
    (instance? Long x) 8
    (instance? Integer x) 4
    (instance? Short x) 2
    (instance? Byte x) 1
    (instance? Double x) 8
    (instance? Float x) 4
    (instance? Character x) 2
    (instance? UUID x) 16
    (instance? BigInteger x) (big-integer-weight x)
    (instance? BigInt x) (big-integer-weight (.toBigInteger ^BigInt x))
    (instance? BigDecimal x) (+ 4 (big-integer-weight (.unscaledValue ^BigDecimal x)))
    (instance? Ratio x) (+ (big-integer-weight (.numerator ^Ratio x))
                           (big-integer-weight (.denominator ^Ratio x)))
    (contains? primitive-widths (class x))
    (inc (* (long (java.lang.reflect.Array/getLength x)) (long (get primitive-widths (class x)))))
    (instance? object-array-class x)
    (do (.add stack (Rest. (.iterator (Arrays/asList ^objects x)))) 1)
    (instance? Map$Entry x)
    (do (.add stack (.getValue ^Map$Entry x)) (.add stack (.getKey ^Map$Entry x)) 1)
    ;; A record's own iterator, and a Clojure map's, give its entries and
    ;; hash nothing; `entrySet` of a record makes a set, which hashes each
    ;; value, and a value with no end has no hash.
    (instance? IRecord x)
    (do (.add stack (Rest. (.iterator ^Iterable x)))
        (inc (text-weight (.getName (class x)) left)))
    (instance? IPersistentMap x)
    (do (.add stack (Rest. (.iterator ^Iterable x))) 1)
    (instance? Map x)
    (do (.add stack (Rest. (.iterator (.entrySet ^Map x)))) 1)
    (instance? Collection x)
    (do (.add stack (Rest. (.iterator ^Collection x))) 1)
    :else nil))

(defn weight-of
  "A long not over the bound; `:too-heavy` when the weight passes the bound;
  or `:not-weighed` when the part holds a part of a class that is not
  weighed, or throws while it is walked. The part that the walk finds first
  decides between the two words. The walk keeps a stack of its own and
  calls itself nowhere, so no depth of a part overflows a thread's stack.
  It never throws."
  [x bound]
  (try
    (let [bound (long bound)
          stack (ArrayList.)]
      (.add stack x)
      (loop [total 0]
        (cond
          (> total bound) :too-heavy
          (.isEmpty stack) total
          :else
          (let [top (.remove stack (int (dec (.size stack))))]
            (if (instance? Rest top)
              (let [^Iterator it (.-it ^Rest top)]
                (when (.hasNext it)
                  (let [part (.next it)]
                    (.add stack top)
                    (.add stack part)))
                (recur total))
              (if-let [w (part-weight top stack (- bound total))]
                (recur (+ total (long w)))
                :not-weighed))))))
    (catch Throwable _ :not-weighed)))
