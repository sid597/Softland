(ns softland.store.pure-proof
  "The proof of the gates' pure namespaces: the one text and the digest, the
  door's declaration, the time on the act, the stamp, the first act of a
  layer, the record's rows, and the checks of the decision, each alone and
  as the walk of one thing calls them, one thing after the other. And of
  the door's pure namespaces: its words, what it makes of one act, and the
  numbers of a maker's wait. And of the source under `src-store/`: that no
  code of it names a lock; and of the proofs under `test-store/`: that
  their code names one only where a proof means it. It needs no cluster and
  no Rama. One command, from the repository root:

    clojure -M:store -m softland.store.pure-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  Approach. As in the act's proof, each check is a case: what it is about, a
  function of no argument that calls the code, and what that function must
  give. Where the proof writes a declaration out a second time, as literals,
  it does so on purpose: a change of the declaration is then made in two
  places. A run of bytes is compared by its content. A row's value is held
  against a text that the proof writes out, or against what a second road
  gives: Java's own UTF-8, the proof's own SHA-256, Clojure's printer. A
  refusal is held by its word.

  The time. A thing on a depot holds the time on the act, and the
  declaration refuses a thing with no usable one. So each thing that the
  proof's hand builds holds one time, `now`, under the name that
  `softland.store.door.declaration/time-part` holds; a thing without it, or
  with another time, is built so on purpose. The proof holds: what the
  declaration takes as a time, with no bound above, the word for a thing
  with none, and where that word stands among the others; that the test for
  a stray reads no time; that the digest leaves the time out at the top
  level of a plain map and nowhere else; that the door's additions add the
  time that they are handed and test nothing of it; that the time is a
  label, which a decision writes beside the stamp in the answer and in each
  row that an accept writes, and which no check reads; and that the stamp
  is the layer's count of decisions, which neither the time on the thing
  nor a stamp that the act carries moves, in a layer of either gate. The
  name of the part is
  written out as a literal in three places, each held against the
  declaration's: the second writing of the shape, the text of the door's
  word, and the first field of the record `Timed`. That a door writes its
  server's clock there, and that no gate reads a clock, need a cluster and
  are not reached here.

  A layer of the other gate. The cases of the standing call
  `softland.store.gate.decision/standing` and `makes-the-layer?` with the
  gate's depot, the act and two rows, for each gate's depot word. They hold
  that an act for a layer that starts with the other gate's depot, of which
  the gate's record holds nothing, gets the word of such a layer from the
  act alone: the base on the streaming depot, and any other layer on the
  microbatch depot, also when the act has the form of a making act of the
  other gate.

  The walk in memory. Both gates decide a thing with one walk
  (`softland.store.module.walk`), which is Rama's dataflow over the pure
  functions. The proof has a function of its own, `walked`, that makes the
  reads, the calls and the writes of that walk in its order against a
  record in memory: what one gate holds on one task, each table a map. With
  it the proof decides lines of things, one thing after the other in the
  order of the depot, each against the rows that the things before it
  wrote, and holds the word of each answer, the rows, the stamps and the
  strays. It holds that a second run of a decision from the same record
  gives the same writes, and that a run on the record that the first left
  finds the answer and writes nothing; that the stamps of a layer are 0, 1,
  2 and on with no gap, for an accept and for a refusal, whatever the times
  on its acts and the stamps that they carry; and that a mark of a key
  holds from the next act, in a layer of each gate. What this shows is what the pure
  functions give when they are called in that order. That the walk on a
  cluster calls them so, that a gate hands it the things of a partition in
  the depot's order, and that the microbatch gate copies the rows of an
  accept to every task, are the module's proof's.

  What a gate may copy. `softland.store.module.walk/copies-of` is pure, but
  its namespace requires Rama. The proof reads the source file of the walk
  as data, makes the one `defn` of that name a function with Clojure's own
  names, and holds it against a filter of the proof's own. That reaches the
  text of the function, and not the var that a cluster loads.

  The order by value. Its cases decide one act at a time against a record in
  memory with a function of the proof's own, `streaming-step`, which makes
  the reads and the writes of steps 6 and 7 of the walk over the pure
  functions, with a stamp that the case gives. Each act goes through the
  declaration's check first, which gives each value its text.

  State. The proof owns one atom: the lines that it showed, for the recap.
  It reads the clock for nothing and draws no random number. Two checks
  read files and write none. `no-code-of-the-store-names-a-lock` reads each
  `.clj` file under the directory that holds `softland/store/act.clj` on
  the classpath, and each under the directory that holds this proof. The
  cases of what a gate may copy read `softland/store/module/walk.clj` on
  the classpath.

  The door. The cases of its three pure namespaces hold each rule of the
  door that needs no cluster: each kind of thing that is no act, the thing
  that it builds, and its one number.

  What it does not reach is in the folder's README: anything that needs a
  depot, a PState or a query, which the module's proof and the door's proof
  run on a cluster."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [clojure.walk :as walk]
            [softland.store.act :as act]
            [softland.store.gate.decision :as decision]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.first-act :as first-act]
            [softland.store.record.rows :as record]
            [softland.store.gate.stamp :as stamp]
            [softland.store.door.text :as text]
            [softland.store.door.wait :as wait]
            [softland.store.door.words :as words])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

;; ------------------------------------------------------------------ cases

(defn- comparable
  "Takes anything. Gives it with each run of bytes as [:bytes its content],
  so that two things that hold equal bytes are equal."
  [x]
  (cond
    (bytes? x) [:bytes (vec x)]
    (map? x) (into {} (map (fn [[k v]] [(comparable k) (comparable v)])) x)
    (vector? x) (mapv comparable x)
    (set? x) (into #{} (map comparable) x)
    (sequential? x) (map comparable x)
    :else x))

(defn- outcome
  "Takes a case. Gives what its function gave, with bytes made comparable, or
  [:threw the class] when it threw."
  [[_ f _]]
  (try (comparable (f))
       (catch Throwable t [:threw (.getSimpleName (class t))])))

(defn- holds
  "Checks each case of a group: its function must give what the case says.
  Prints each case's sentence after PASSED or FAILED, so that a run lists
  what it checked. Gives the number of cases."
  [cases]
  (doseq [[what _ expected :as c] cases]
    (println (if (is (= (comparable expected) (outcome c)) what) "PASSED" "FAILED") what))
  (count cases))

(defn- words
  "Rows [what x word] as text: each what with its word."
  [rows]
  (str/join " | " (map (fn [[what _ word]] (str what " " word)) rows)))

;; ---------------------------------------------------------------- fixtures

(defn- uuid
  "A fixed UUID, so that an id of the proof is the same in each run."
  [n]
  (UUID. 0 n))

(defn- bytes-of
  "A run of n bytes: 1, 2, 3 and so on."
  [n]
  (byte-array (map #(unchecked-byte (inc %)) (range n))))

(defn- sha-256
  "The SHA-256 of some runs of bytes, one after the other, as hex text."
  [& runs]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (doseq [^bytes run runs] (.update md run))
    (text/hex (.digest md))))

(defn- utf8
  "The UTF-8 bytes of a string."
  [^String s]
  (.getBytes s StandardCharsets/UTF_8))

(defn- throwing
  "A sequence that throws when it is read."
  []
  (lazy-seq (throw (ex-info "this sequence throws when it is read" {}))))

(defn- nested
  "The number 1 inside n vectors."
  [n]
  (loop [v 1 i 0] (if (< i n) (recur [v] (inc i)) v)))

(defn- long-id
  "A keyword whose printed text has n characters, the colon counted."
  [n]
  (keyword (apply str (repeat (dec n) "a"))))

(defrecord Thing [e k])

;; A record with a field of the name that `softland.store.door.declaration/time-part`
;; holds: `digest-cases` holds that name against the field.
(defrecord Timed [door-time e])

(def ^:private now
  "The one time of the proof's cases, in milliseconds since 1970. The proof's
  hand writes it on each thing that it builds, under
  `softland.store.door.declaration/time-part`, where a door writes the clock
  of its server. The cases of the decision give it as the time on the act,
  for an act of either gate."
  1700000000000)

(def ^:private far-ahead
  "A time far ahead of every clock: 2^50 milliseconds since 1970, more than
  30,000 years after `now`. Whatever appends a thing can write such a time
  under `softland.store.door.declaration/time-part`. The cases hold that it
  is a usable time, that a decision writes it as it is, and that it moves
  no stamp."
  (bit-shift-left 1 50))

(def ^:private alice-own "Alice's permission in her own layer." [:alice :alice :alice])
(def ^:private bobs "Bob's permission in Alice's layer, beneath hers." [:bob :alice :alice alice-own])

(defn- act-id
  "A streaming act-id in Alice's layer, with a fixed UUID."
  [n]
  [:alice :streaming (uuid n)])

(defn- value-at
  "A fact as a maker gives it, with a plain value: the string \"a text\", or
  the value given."
  ([e k] (value-at e k "a text"))
  ([e k v] {:e e :k k :v v}))

(defn- text-of-bytes
  "A string whose one text has n bytes: n - 2 letters, and the two quotes."
  [n]
  (apply str (repeat (- n 2) "a")))

(defn- deep-permission
  "A permission id of a layer that holds n permission ids, itself counted.
  Each of them is held by `holder` in that layer, the first one too."
  [holder layer n]
  (reduce (fn [parent _] [holder layer layer parent]) [holder layer layer] (range (dec n))))

(defn- arrived
  "Alice's act into her own layer as it arrives on the streaming depot, under
  the act-id `(act-id n)`, with the time `now` on it, these facts and no
  optional part."
  [n & facts]
  {:act-id (act-id n) :depot :streaming declaration/time-part now
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :facts (vec facts)})

(defn- one-form
  "The one form on arrival of `arrived` for facts that are in their one form."
  [n & facts]
  {:act-id (act-id n) :depot :streaming declaration/time-part now
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {} :because-of nil :claimed-when nil :subjects #{}
   :facts (vec facts)})

(def ^:private good
  "The least thing that the check takes: Alice's act with one fact that has
  a value."
  (arrived 1 (value-at :note-1 :text)))

(defn- without-time
  "Takes a thing that is a plain map. Gives it without the part that holds
  the time on the act."
  [thing]
  (dissoc thing declaration/time-part))

(defn- at-time
  "The least thing with `t` under the part that holds the time on the act."
  [t]
  (assoc good declaration/time-part t))

(defn- verdict
  "What the declaration's check gave, as one word: :ok, or its reason."
  [checked]
  (if (contains? checked :ok) :ok (:refuse checked)))

(defn- verdicts
  "Takes rows [what thing word]. Gives them as cases: the check of the thing
  must give that word, :ok or a reason."
  [rows]
  (mapv (fn [[what x word]] [what (fn [] (verdict (declaration/check x))) word]) rows))

(defn- act-of
  "The one form on arrival of a thing that the check takes."
  [thing]
  (:ok (declaration/check thing)))

(defn- arrived-fact
  "A fact in its one form on arrival, for the rows that the cases start from:
  with no value, nil for the value and for its text; with a value, its one
  text, and the value itself under a store key alone. The text is made here
  by the store's own function, so no case holds a row's value against this
  helper: `passing-cases` holds the form that the declaration's check gives
  against texts that are written out. What must hold: the value is in its
  normal form."
  ([e k] {:e e :k k :v nil :replaces nil :text nil})
  ([e k v] {:e e :k k :v (when (declaration/store-key? k) v) :replaces nil :text (text/text v)}))

;; ------------------------------------------------- the one text, the digest

(def ^:private text-rows
  "One thing of each kind, with its one text."
  [["nil" nil "nil"]
   ["true" true "true"]
   ["false" false "false"]
   ["a string, escaped" "a \"b\"\n\\" "\"a \\\"b\\\"\\n\\\\\""]
   ["a readable keyword" :tool/class ":tool/class"]
   ["a UUID" (uuid 7) "#uuid \"00000000-0000-0000-0000-000000000007\""]
   ["a long" 42 "42"]
   ["a narrow integer" (short -7) "-7"]
   ["a big integer" 100000000000000000000N "100000000000000000000N"]
   ["a Java big integer" (java.math.BigInteger. "7") "7N"]
   ["a double" 1.5 "1.5"]
   ["minus zero" -0.0 "0.0"]
   ["a double that is not finite" Double/NaN "#double \"NaN\""]
   ["a decimal, without its trailing zeros" 1.50M "1.5M"]
   ["a run of bytes" (byte-array [1 -1]) "#bytes 2 \"01ff\""]
   ["a vector" [1 :a "x"] "[1 :a \"x\"]"]
   ["a list" (list 1 2) "(1 2)"]
   ["a set, sorted by text" #{:b :a 10 9} "#{10 9 :a :b}"]
   ["a map, sorted by the text of the key" {:b 1 :a {:d 2 :c 3}} "{:a {:c 3 :d 2} :b 1}"]
   ["a keyword that does not read back" (keyword "a b") "#keyword nil \"a b\""]
   ["a record" (->Thing 1 2) "#other softland.store.pure_proof.Thing"]
   ["a symbol" 'x "#other clojure.lang.Symbol"]
   ["a ratio" 1/3 "#other clojure.lang.Ratio"]
   ["a float" (float 1.5) "#other java.lang.Float"]
   ["a Java list" (java.util.ArrayList. [1]) "#other java.util.ArrayList"]
   ["a thing that throws when it is read" (throwing) "#unreadable"]
   ["a thing that throws, inside a map" {:facts (throwing)} "#unreadable"]])

(def ^:private same-text-rows
  "Pairs that no check can tell apart, and that a serialiser may change. Each
  pair has one text."
  [["an Integer and a Long of one value" (int 5) 5]
   ["minus zero and zero" -0.0 0.0]
   ["a subvector and a vector" (subvec [0 1 2] 1) [1 2]]
   ["a map built in two orders" (array-map :a 1 :b 2) (array-map :b 2 :a 1)]
   ["a sorted map and a hash map" (sorted-map :a 1 :b 2) (hash-map :b 2 :a 1)]
   ["a decimal with and without trailing zeros" 2.500M 2.5M]
   ["two keys of one text, in two orders" (array-map 'x 1 'y 2) (array-map 'y 2 'x 1)]])

(def ^:private two-text-rows
  "Pairs that a check tells apart. Each pair has two texts."
  [["a list and a vector" (list 1 2) [1 2]]
   ["a long and a big integer" 7 7N]
   ["a long and a double" 1 1.0]
   ["a keyword and a string" :a ":a"]
   ["a string and a UUID" "00000000-0000-0000-0000-000000000007" (uuid 7)]
   ["a record and a map" (->Thing 1 2) {:e 1 :k 2}]
   ["two runs of bytes that differ in one bit" (byte-array [1 2]) (byte-array [1 3])]
   ["bytes and their hex as a string" (byte-array [1]) "01"]
   ["nil and the keyword :nil" nil :nil]
   ["a map and a vector of its entries" {:a 1} [[:a 1]]]])

(def ^:private edn-values
  "Values that a fact can hold, in their one normal form. The text of each
  is EDN, and reads back as the value."
  [nil true "a \"text\"" :tool/class (uuid 3) 42 -7 100000000000000000000N 1.5 0.0 1.5M 1E+2M
   [1 [2 #{3}]] {:a {:b [1 2]} "k" nil} #{:a "b" 3}
   [:agent-1 :alice :alice [:alice :alice :alice]]])

(def ^:private text-cases
  "The cases of the one text: the text of each kind; one text and one digest
  for the pairs that no check tells apart; two for the pairs that a check
  tells apart; the text of a value read back; the depth; the text of an id."
  (concat
   (map (fn [[what x expected]] [(str "the text of " what) (fn [] (text/text x)) expected]) text-rows)
   (map (fn [[what a b]] [(str "one text for " what) (fn [] (= (text/text a) (text/text b))) true])
        same-text-rows)
   (map (fn [[what a b]] [(str "one digest for " what) (fn [] (= (digest/hex-of a) (digest/hex-of b))) true])
        same-text-rows)
   (map (fn [[what a b]] [(str "two texts for " what) (fn [] (= (text/text a) (text/text b))) false])
        two-text-rows)
   (map (fn [[what a b]] [(str "two digests for " what) (fn [] (= (digest/hex-of a) (digest/hex-of b))) false])
        two-text-rows)
   (map (fn [v] [(str "the text of the value " (pr-str v) " reads back as the value")
                 (fn [] (let [back (edn/read-string (text/text v))]
                          [(= v back) (= (class v) (class back))]))
                 [true true]])
        (filter some? edn-values))
   [["a part inside 65 collections is written #too-deep, and one inside 64 is written out"
     (fn [] [(str/includes? (text/text (nested 65)) "#too-deep")
             (str/includes? (text/text (nested 64)) "#too-deep")])
     [true false]]
    ["the id of a row's key: a keyword as it prints, a UUID as its 36 characters"
     (fn [] [(declaration/id-text :tool/class) (declaration/id-text (uuid 7))])
     [":tool/class" "00000000-0000-0000-0000-000000000007"]]
    ["the hex text of bytes"
     (fn [] (text/hex (byte-array [0 15 16 -1])))
     "000f10ff"]
    ["the UTF-8 of the one text"
     (fn [] (vec (text/utf8 :é)))
     (vec (utf8 ":é"))]
    ["the count of the UTF-8 bytes of a string, with no array made: no character, a character of one byte, of two, of three, a pair of surrogates, and the four with spaces between; each is the length of what Java gives"
     (fn [] (let [strings ["" "a" "é" "✓" "𝄞" "a é ✓ 𝄞"]]
              [(mapv text/utf8-count strings) (mapv (fn [s] (alength ^bytes (utf8 s))) strings)]))
     [[0 1 2 3 4 13] [0 1 2 3 4 13]]]
    ["half of a pair of surrogates that stands alone counts one byte, as Java writes it"
     (fn [] [(text/utf8-count "a\ud834") (alength ^bytes (utf8 "a\ud834")) (text/utf8-count "\udd1e")])
     [2 2 1]]]))

(def ^:private digest-cases
  "The cases of the digest: its size; what it is the SHA-256 of; a thing that
  throws; the comparison of a sender that still holds its thing; the same
  act built twice; the digest that tells a throw; and the time on the act,
  which the digest leaves out at the top level of a plain map and nowhere
  else."
  (let [no-bytes {:b [1 2N "x"] :a #{:k}}
        with-bytes {:run (bytes-of 5) :e :note-1}
        unreadable (sha-256 (utf8 "#unreadable"))
        string-keys (fn [v] (sorted-map "a" v (name declaration/time-part) 5))]
    [["a digest is 32 bytes, and its hex text 64 characters"
      (fn [] [(alength ^bytes (digest/digest no-bytes)) (count (digest/hex-of no-bytes))])
      [32 64]]
     ["for a thing with no bytes and no time, the digest is the SHA-256 of the UTF-8 of its one text"
      (fn [] (digest/hex-of no-bytes))
      (sha-256 (utf8 (text/text no-bytes)))]
     ["the digest of {:a 1} is the SHA-256 of the six characters {:a 1}"
      (fn [] (digest/hex-of {:a 1}))
      (sha-256 (utf8 "{:a 1}"))]
     ["a run of bytes, which is a part that is no value, is fed as #bytes, its count and the bytes themselves, not as hex"
      (fn [] (digest/hex-of with-bytes))
      (sha-256 (utf8 "{:e :note-1 :run #bytes 5 ") (bytes-of 5) (utf8 "}"))]
     ["the digest of a thing that throws when it is read is the digest of the text #unreadable"
      (fn [] (digest/hex-of {:facts (throwing)}))
      (sha-256 (utf8 "#unreadable"))]
     ["the comparison of a sender that still holds its thing: the digest of the thing sent, of a thing with another entity, and no digest; and the thing sent, held without its time"
      (fn [] [(digest/is-digest-of? (digest/hex-of good) good)
              (digest/is-digest-of? (digest/hex-of good) (arrived 1 (value-at :note-2 :text)))
              (digest/is-digest-of? nil good)
              (digest/is-digest-of? (digest/hex-of good) (without-time good))])
      [true false false true]]
     ["the same act built twice has one digest, so a maker's second send of one act reads the answer of the first; the act with another value has another digest"
      (fn [] (let [built (fn [v] (digest/hex-of (arrived 1 (value-at :note-1 :text v))))]
               [(= (built "a text") (built "a text")) (= (built "a text") (digest/hex-of good))
                (= (built "a text") (built "a text."))]))
      [true true false]]
     ["the digest that tells a throw: the hex text that `hex-of` gives, for a thing that is read to its end; nothing for a thing that throws when it is read"
      (fn [] [(digest/readable-hex-of {:a 1}) (= (digest/readable-hex-of good) (digest/hex-of good))
              (digest/readable-hex-of {:facts (throwing)}) (digest/readable-hex-of (throwing))])
      [(sha-256 (utf8 "{:a 1}")) true nil nil]]
     ["two things that differ only in the time on the act have one digest, by `digest`, by `hex-of` and by `readable-hex-of`: the least thing with a time one millisecond later, as a second send carries, with a nil time, and with no time"
      (fn [] (let [others [(at-time (inc now)) (at-time nil) (without-time good)]]
               [(mapv (fn [x] (= (comparable (digest/digest good)) (comparable (digest/digest x)))) others)
                (mapv (fn [x] (= (digest/hex-of good) (digest/hex-of x))) others)
                (mapv (fn [x] (= (digest/readable-hex-of good) (digest/readable-hex-of x))) others)
                (= (digest/hex-of good) (digest/readable-hex-of good) (text/hex (digest/digest good)))]))
      [[true true true] [true true true] [true true true] true]]
     ["the digest of a thing with a time is the SHA-256 of the one text of the thing without it; the text of the thing holds the time, and the digest is not of that text"
      (fn [] [(digest/hex-of good)
              (str/includes? (text/text good) (str declaration/time-part " " now))
              (= (digest/hex-of good) (sha-256 (utf8 (text/text good))))])
      [(sha-256 (utf8 (text/text (without-time good)))) true false]]
     ["a thing that differs in anything else has another digest, with the same time on it: another value, another act-id, another who, a part more that holds the same number as the time"
      (fn [] (mapv (fn [x] (= (digest/hex-of good) (digest/hex-of x)))
                   [(arrived 1 (value-at :note-1 :text "a text.")) (arrived 2 (value-at :note-1 :text))
                    (assoc good :who :bob) (assoc good :claimed-when now)]))
      [false false false false]]
     ["a time deeper than the top level is not left out: two things that differ only in a time inside a value, inside a fact, or inside a vector have two digests, by `hex-of` and by `readable-hex-of`; the digest of the vector is the SHA-256 of its whole text"
      (fn [] (let [in-value (fn [t] (arrived 1 (value-at :note-1 :text {declaration/time-part t})))
                   in-fact (fn [t] (arrived 1 (assoc (value-at :note-1 :text) declaration/time-part t)))
                   in-vector (fn [t] [{declaration/time-part t}])]
               [(mapv (fn [made] (= (digest/hex-of (made 1)) (digest/hex-of (made 2)))) [in-value in-fact in-vector])
                (mapv (fn [made] (= (digest/readable-hex-of (made 1)) (digest/readable-hex-of (made 2)))) [in-value in-fact in-vector])
                (= (digest/hex-of (in-vector 1)) (sha-256 (utf8 (text/text (in-vector 1)))))]))
      [[false false false] [false false false] true]]
     ["a sorted map with string keys, from which Clojure cannot take a keyword's part, keeps a digest of its own: the SHA-256 of its own text, by each of the three functions; two such maps with different content have two digests; neither has the digest of #unreadable"
      (fn [] (let [a (string-keys 1)
                   b (string-keys 2)]
               [(try (dissoc a declaration/time-part) :taken (catch ClassCastException _ :threw))
                (text/text a)
                (= (digest/hex-of a) (sha-256 (utf8 (text/text a))))
                (= (digest/hex-of a) (digest/readable-hex-of a) (text/hex (digest/digest a)))
                (= (digest/hex-of a) (digest/hex-of b)) (= (digest/readable-hex-of a) (digest/readable-hex-of b))
                (= unreadable (digest/hex-of a)) (= unreadable (digest/hex-of b))]))
      [:threw (str "{\"a\" 1 \"" (name declaration/time-part) "\" 5}") true true false false false false]]
     ["a record is not changed: a record with a field of the time's name has the digest of a record, the SHA-256 of its text, by each of the three functions; with the field taken out it would be a plain map, which has another digest"
      (fn [] (let [timed (->Timed 5 :note-1)]
               [(contains? timed declaration/time-part) (record? (without-time timed)) (text/text timed)
                (= (digest/hex-of timed) (sha-256 (utf8 (text/text timed))))
                (= (digest/hex-of timed) (digest/readable-hex-of timed) (text/hex (digest/digest timed)))
                (= (digest/hex-of timed) (digest/hex-of (without-time timed)))]))
      [true false "#other softland.store.pure_proof.Timed" true true false]]]))

;; ---------------------------------------------------------- the declaration

(def ^:private declared-shapes
  "The five shapes that the door adds, written out here a second time. What
  arrives holds the time on the act after the depot: a fixed-width integer
  of 0 or more, with no bound above."
  {:arrived-act
   [:parts
    [:act-id :act-id]
    [:depot :depot-word {:reason :depot-not-the-act-ids-depot}]
    [:door-time [:int {:min 0}] {:reason :no-door-time}]
    [:version [:enum 1] {:reason :unknown-version}]
    [:who :id]
    [:layer :id]
    [:permission :permission-id {:absent :no-permission}]
    [:session :id {:absent :no-session}]
    [:based-on [:map-of :fact-id :carried-stamp] {:optional true :default {}}]
    [:because-of :act-id {:optional true}]
    [:claimed-when :int {:optional true}]
    [:subjects [:set-of :id {:max 256}] {:optional true :default #{}}]
    [:facts [:seq-of :fact] {:empty :empty-act}]]
   :depot-word [:enum :streaming :microbatch]
   :who-may-see-word [:enum :members :everyone]
   :layer-list [:seq-of :id]
   :key-set [:set-of :key]})

(def ^:private declared-rules
  "The rules of the declaration, written out here a second time."
  [[:permission-is-in-the-acts-layer :permission-not-in-layer]
   [:depot-is-the-act-ids-depot :depot-not-the-act-ids-depot]
   [:facts-within-the-limit :too-many-facts]
   [:based-on-within-the-limit :too-many-based-on]
   [:store-key-is-known :store-key-not-known]
   [:depot-fact-holds-a-depot-word :depot-fact-not-a-depot-word]
   [:permission-fact-holds-a-permission :permission-fact-not-a-permission]
   [:permission-fact-sits-on-its-holder :permission-fact-not-on-its-holder]
   [:who-may-see-fact-holds-its-word :who-may-see-fact-not-members-or-everyone]
   [:builds-on-fact-holds-layers :builds-on-fact-not-a-list-of-layers]
   [:builds-on-within-the-limit :builds-on-too-many-layers]
   [:marked-keys-fact-holds-keys :marked-keys-fact-not-a-set-of-keys]
   [:marked-keys-within-the-limit :too-many-marked-keys]
   [:marked-keys-are-plain-keys :marked-key-in-the-stores-namespace]
   [:place-named-once :place-named-twice]
   [:no-fact-replaces-a-fact-of-its-act :replaces-own-fact]
   [:ids-within-the-limit :id-too-long]])

(def ^:private declared-reasons
  "Each word with which the check refuses: `act.clj`'s seven and the door's
  nineteen, the word of the time's part and the two of the last step among
  them."
  #{:malformed :unknown-part :unknown-version :no-permission :no-session :empty-act
    :permission-not-in-layer
    :depot-not-the-act-ids-depot :no-door-time :too-many-facts :too-many-based-on
    :store-key-not-known
    :depot-fact-not-a-depot-word :permission-fact-not-a-permission
    :permission-fact-not-on-its-holder
    :who-may-see-fact-not-members-or-everyone :builds-on-fact-not-a-list-of-layers
    :builds-on-too-many-layers :marked-keys-fact-not-a-set-of-keys :too-many-marked-keys
    :marked-key-in-the-stores-namespace
    :place-named-twice :replaces-own-fact :id-too-long
    :value-too-large :act-too-large})

(def ^:private declaration-parts-cases
  "The declarations of the door, each against its second writing here, and
  what they say as sets."
  [["the door's map of shapes holds each shape of act.clj as act.clj declares it"
    (fn [] (= act/shapes (select-keys declaration/shapes (keys act/shapes))))
    true]
   ["the five shapes that the door adds: what arrives, with the time on the act after the depot, whose facts are the maker's own; a depot word; and the values of the three keys that take a second write"
    (fn [] (apply dissoc declaration/shapes (keys act/shapes)))
    declared-shapes]
   ["the name of the part that holds the time on the act is the one that the shape of what arrives holds, and that shape takes any fixed-width integer not below zero: it has no bound above"
    (fn [] (let [[part shape] (nth (:arrived-act declaration/shapes) 3)]
             [declaration/time-part (= part declaration/time-part) shape]))
    [:door-time true [:int {:min 0}]]]
   ["the seventeen rules, the maker's first, each with its reason; the six of the keys that take a second write stand after the rules of a permission fact"
    (fn [] declaration/rules)
    declared-rules]
   ["the words with which the check refuses, read from the two declarations, the word of the time's part among them, and the two of its last step"
    (fn [] [declaration/reasons (contains? declaration/reasons :no-door-time)])
    [declared-reasons true]]
   ["the five words of a stray, in the order tested"
    (fn [] declaration/stray-reasons)
    [:no-act-id :act-id-malformed :id-too-long :act-id-of-the-other-gate :act-id-of-another-layer]]
   ["the seven limits: facts, entries of based-on, the bytes of one value's text, the bytes of text that one act writes into the rows of its facts, the text of an id, the layers that a layer builds on, the keys that a layer marks"
    (fn [] [declaration/max-facts declaration/max-based-on declaration/max-value-bytes
            declaration/max-act-bytes declaration/max-id-text
            declaration/max-builds-on declaration/max-marked-keys])
    [256 1024 65536 18874368 256 64 32]]
   ["the bound on an act is 256 values at the bound of one value, and 8,192 bytes of who and permission for each of 256 facts"
    (fn [] (= declaration/max-act-bytes (* 256 (+ 65536 8192))))
    true]
   ["the five store keys, the shape of the value of each, which is a setting, and which is written again; a key in the store's namespace that is none of the five, and a bare word, is no store key"
    (fn [] [(into {} (map (fn [[k entry]] [k (:value entry)])) declaration/store-keys)
            declaration/setting-keys
            declaration/second-write-keys
            (mapv declaration/store-key?
                  [:softland.store/depot :softland.store/permission
                   :softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys
                   :softland.store/lock-request :softland.store/owner
                   :class :depot :permission :lock-request :tool/depot :owner
                   :who-may-see :builds-on :marked-keys :tool/marked-keys])])
    [{:softland.store/depot :depot-word :softland.store/permission :permission-id
      :softland.store/who-may-see :who-may-see-word
      :softland.store/builds-on :layer-list :softland.store/marked-keys :key-set}
     #{:softland.store/depot :softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys}
     #{:softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys}
     [true true true true true false false false false false false false false false false false false]]]
   ["the store's namespace, and each store key is in it"
    (fn [] [declaration/store-namespace (set (map namespace (keys declaration/store-keys))) (count declaration/store-keys)])
    ["softland.store" #{"softland.store"} 5]]])

(def ^:private stray-rows
  "One thing for each word of a stray, and things that are no stray."
  [["a number" 5 :no-act-id]
   ["nil" nil :no-act-id]
   ["a vector" [:alice :streaming (uuid 1)] :no-act-id]
   ["a record" (->Thing 1 2) :no-act-id]
   ["a map with no act-id" (dissoc good :act-id) :no-act-id]
   ["a map whose act-id is nil" (assoc good :act-id nil) :no-act-id]
   ["a map that throws when its act-id is tested" (assoc good :act-id (throwing)) :act-id-malformed]
   ["an act-id of two slots" (assoc good :act-id [:alice :streaming]) :act-id-malformed]
   ["an act-id of four slots" (assoc good :act-id [:alice :streaming (uuid 1) 0]) :act-id-malformed]
   ["an act-id with a depot word that is none" (assoc good :act-id [:alice :stream (uuid 1)]) :act-id-malformed]
   ["an act-id whose last slot is no UUID" (assoc good :act-id [:alice :streaming "u"]) :act-id-malformed]
   ["an act-id whose layer is a string" (assoc good :act-id ["alice" :streaming (uuid 1)]) :act-id-malformed]
   ["an act-id that is a list" (assoc good :act-id (list :alice :streaming (uuid 1))) :act-id-malformed]
   ["an act-id whose layer prints to 257 characters"
    (assoc good :act-id [(long-id 257) :streaming (uuid 1)] :layer (long-id 257)) :id-too-long]
   ["an act-id of the depot microbatch" (assoc good :act-id [:alice :microbatch (uuid 1)]) :act-id-of-the-other-gate]
   ["an act-id of another layer" (assoc good :act-id [:bob :streaming (uuid 1)]) :act-id-of-another-layer]
   ["a thing with an act-id and no layer" (dissoc good :layer) :act-id-of-another-layer]
   ["an act-id of the depot microbatch and of another layer: the depot is told first"
    (assoc good :act-id [:bob :microbatch (uuid 1)]) :act-id-of-the-other-gate]
   ["a good act" good nil]
   ["a thing that is no act, with a usable act-id" {:act-id (act-id 1) :layer :alice :junk 1} nil]
   ["an act-id whose layer prints to 256 characters"
    (assoc good :act-id [(long-id 256) :streaming (uuid 1)] :layer (long-id 256)) nil]])

(def ^:private stray-cases
  "The cases of the test for a stray: one for each row of `stray-rows`, the
  two that show that the test takes the gate's depot, and the two that show
  that it reads no time."
  (concat
   (map (fn [[what x word]] [(str "a stray? " what) (fn [] (declaration/stray-reason :streaming x)) word])
        stray-rows)
   [["the test takes the gate's depot: the same microbatch act-id at the microbatch gate is no stray"
     (fn [] (declaration/stray-reason :microbatch (assoc good :act-id [:alice :microbatch (uuid 1)])))
     nil]
    ["and a streaming act-id at the microbatch gate is a stray"
     (fn [] (declaration/stray-reason :microbatch good))
     :act-id-of-the-other-gate]
    ["the test for a stray reads no time: each of the 17 plain maps of the rows above, 16 of which hold a time, has one word at a gate, with its time as it is, without it, with a nil one and with a string; held at each gate's depot"
     (fn [] (let [maps (filter (fn [x] (and (map? x) (not (record? x)))) (map second stray-rows))
                  word (fn [x] [(declaration/stray-reason :streaming x) (declaration/stray-reason :microbatch x)])]
              [(count maps)
               (count (filter (fn [x] (contains? x declaration/time-part)) maps))
               (every? (fn [x] (= (word x) (word (without-time x)) (word (assoc x declaration/time-part nil))
                                  (word (assoc x declaration/time-part "no time"))))
                       maps)]))
     [17 16 true]]
    ["a thing with a usable act-id and no time is no stray on its act-id's depot: the check has the word for it, so a gate answers it under its act-id"
     (fn [] [(declaration/stray-reason :streaming (without-time good)) (verdict (declaration/check (without-time good)))])
     [nil :no-door-time]]]))

(def ^:private whole
  "A thing with each part, given with subvectors, narrow integers and a list
  of facts: a value that is not in its normal form, a retract, a permission
  given. Its time is a narrow integer too."
  {:act-id (subvec (into [:x] (act-id 9)) 1)
   :depot :streaming
   declaration/time-part (int 1700001)
   :version (int 1)
   :who :alice :layer :alice
   :permission (subvec [:x :alice :alice :alice] 1)
   :session :alice-s1
   :based-on {[(act-id 2) (int 0)] (int 41)}
   :because-of (act-id 2)
   :claimed-when (int 1700000)
   :subjects #{:alice :bob}
   :facts (list {:e :note-1 :k :text :v (sorted-map :b 1.50M :a (subvec [0 (int 1) 2N] 1))
                 :replaces [(act-id 2) (short 0)]}
                {:e :note-2 :k :text}
                {:e :bob :k :softland.store/permission :v (subvec [:x :bob :alice :alice alice-own] 1)})})

(def ^:private whole-form
  "The one form on arrival of `whole`."
  {:act-id (act-id 9) :depot :streaming declaration/time-part 1700001
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {[(act-id 2) 0] 41} :because-of (act-id 2) :claimed-when 1700000
   :subjects #{:alice :bob}
   :facts [{:e :note-1 :k :text :v nil :replaces [(act-id 2) 0] :text "{:a [1 2] :b 1.5M}"}
           {:e :note-2 :k :text :v nil :replaces nil :text nil}
           {:e :bob :k :softland.store/permission :v bobs :replaces nil
            :text "[:bob :alice :alice [:alice :alice :alice]]"}]})

(def ^:private tools-own-keys
  "Keys that are a tool's own, though each looks like one of the store's: the
  bare words depot and permission, which are store keys in the namespace
  `softland.store` alone; the bare words class and lock-request, which are
  store keys in no namespace, and which a gate refuses in the store's own
  as keys that the store does not know; the store's words in another
  namespace; and a namespace that only begins with the store's."
  [:class :depot :permission :lock-request
   :tool/depot :tool/permission :tool/lock-request
   :softland/depot :softland.store.tool/depot])

(defn- valued-under
  "The word of the check for Alice's act with one fact that has a value under
  a key."
  [k]
  (verdict (declaration/check (arrived 1 (value-at :note-1 k)))))

(def ^:private passing-cases
  "Things that the check takes, each shown in its one form on arrival."
  [["the least thing in its one form on arrival: each part, the time on the act as it arrived, and its fact with its five parts, the text of the value in the place of the value"
    (fn [] [(act-of good) (get (act-of good) declaration/time-part)])
    [(one-form 1 {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""}) now]]
   ["a thing with each part, given with subvectors, narrow integers and a list of facts: a fact under a plain key holds the text of its value's normal form and no value, a fact with no value holds neither, a fact under a store key holds both"
    (fn [] (act-of whole))
    whole-form]
   ["its one form has plain vectors and longs, the text is a String, and the time, given as a narrow integer, is a long"
    (fn [] (let [form (act-of whole)
                 [fact-id carried] (first (:based-on form))]
             (mapv #(.getSimpleName (class %))
                   [(:act-id form) (:permission form) (:facts form) fact-id (second fact-id) carried
                    (:claimed-when form) (second (:replaces (first (:facts form))))
                    (:v (nth (:facts form) 2)) (:text (first (:facts form)))
                    (get whole declaration/time-part) (get form declaration/time-part)])))
    ["PersistentVector" "PersistentVector" "PersistentVector" "PersistentVector" "Long" "Long"
     "Long" "Long" "PersistentVector" "String" "Integer" "Long"]]
   ["the text of a value under a plain key is the one text of its normal form, and the one form keeps no value beside it: an Integer, a decimal with a trailing zero, a sorted map, a UUID, a keyword, a string with a quote and a character of three bytes"
    (fn [] (mapv (fn [v] (let [fact (first (:facts (act-of (arrived 1 (value-at :note-1 :text v)))))]
                           [(:v fact) (:text fact)]))
                 [(int 7) 1.50M (sorted-map :b 1 :a 2) (uuid 7) :tool/class "a \"b\" ✓"]))
    [[nil "7"] [nil "1.5M"] [nil "{:a 2 :b 1}"] [nil "#uuid \"00000000-0000-0000-0000-000000000007\""]
     [nil ":tool/class"] [nil "\"a \\\"b\\\" ✓\""]]]
   ["false, an empty string, an empty map and an empty vector are values and no retract: each has its text; a fact with a nil :v has none, as a fact with no :v"
    (fn [] (mapv (fn [v] (:text (first (:facts (act-of (arrived 1 {:e :note-1 :k :text :v v}))))))
                 [false "" {} [] nil]))
    ["false" "\"\"" "{}" "[]" nil]]
   ["a making act: two facts under store keys, each with its value and the text of it"
    (fn [] (let [checked (declaration/check
                          (assoc (arrived 1 {:e :alice :k :softland.store/depot :v :streaming}
                                          {:e :alice :k :softland.store/permission :v alice-own})
                                 :who :system))]
             [(verdict checked) (mapv (juxt :v :text) (:facts (:ok checked)))]))
    [:ok [[:streaming ":streaming"] [alice-own "[:alice :alice :alice]"]]]]
   ["a retract under a plain key: no value"
    (fn [] (verdict (declaration/check (arrived 1 {:e :note-1 :k :text}))))
    :ok]
   ["each of the five store keys is known: a fact under each, with a right value, passes"
    (fn [] [(verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/depot :v :streaming})))
            (verdict (declaration/check (arrived 1 {:e :bob :k :softland.store/permission :v bobs})))
            (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/who-may-see :v :everyone})))
            (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base (uuid 7)]})))
            (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on :tool/parent}})))])
    [:ok :ok :ok :ok :ok]]
   ["the text of a set under a store key is sorted: the marked keys, with the set beside it"
    (fn [] ((juxt :v :text) (first (:facts (act-of (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on :tool/parent :b :a}}))))))
    [#{:on :tool/parent :b :a} "#{:a :b :on :tool/parent}"]]
   ["the values that the three keys of a second write take: each word of who may see; a list of layers that is empty, names the layer itself, names a layer twice or names a layer that nobody made; a set of keys that is empty"
    (fn [] (mapv (fn [[k v]] (verdict (declaration/check (arrived 1 {:e :alice :k k :v v}))))
                 [[:softland.store/who-may-see :members] [:softland.store/who-may-see :everyone]
                  [:softland.store/builds-on []] [:softland.store/builds-on [:alice]]
                  [:softland.store/builds-on [:base :base]] [:softland.store/builds-on [:nobody-made-this]]
                  [:softland.store/marked-keys #{}]]))
    [:ok :ok :ok :ok :ok :ok :ok]]
   ["a mark of a key in a namespace that only begins with the store's is a mark of a plain key: the store's namespace is taken exactly"
    (fn [] (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store.tool/on :softland/on}}))))
    :ok]
   ["a fact with no value under each of the three keys of a second write passes the declaration, as under the others: a gate refuses the retract"
    (fn [] (mapv (fn [k] (verdict (declaration/check (arrived 1 {:e :alice :k k}))))
                 [:softland.store/who-may-see :softland.store/builds-on :softland.store/marked-keys]))
    [:ok :ok :ok]]
   ["a permission fact whose value says what it allows passes: see, and see and act"
    (fn [] [(verdict (declaration/check (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice :alice alice-own :see]})))
            (verdict (declaration/check (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice :alice alice-own :see-and-act]})))])
    [:ok :ok]]
   ["a tool's own fact under the bare word :class is not read as the store's: it passes as a fact under a plain key, and of its value the one form keeps the text alone"
    (fn [] [(valued-under :class) (first (:facts (act-of (arrived 1 (value-at :note-1 :class :streaming)))))])
    [:ok {:e :note-1 :k :class :v nil :replaces nil :text ":streaming"}]]
   ["a tool's own fact under the bare word :permission passes; so does one under :lock-request and one under :depot"
    (fn [] (mapv valued-under [:permission :lock-request :depot]))
    [:ok :ok :ok]]
   ["a tool's own fact under one of the store's words in another namespace passes; a namespace that only begins with the store's is another namespace"
    (fn [] (mapv valued-under [:tool/depot :tool/permission :tool/lock-request :softland/depot :softland.store.tool/depot]))
    [:ok :ok :ok :ok :ok]]
   ["a key that looks like a store key and is none is a plain key: a value under it passes, one that the store's own key would not take too, and a retract under it passes"
    (fn [] [(mapv #(verdict (declaration/check (arrived 1 {:e :note-1 :k % :v :fast}))) [:class :depot :permission :tool/depot])
            (mapv #(verdict (declaration/check (arrived 1 {:e :note-1 :k %}))) [:class :depot :permission :tool/depot])])
    [[:ok :ok :ok :ok] [:ok :ok :ok :ok]]]])

(def ^:private makers-rows
  "One thing for each word of act.clj: the least thing with one fault of the
  maker's form."
  [["a part that neither the maker nor the door names" (assoc good :owner :alice) :unknown-part]
   ["a fact with a part that the form does not name" (arrived 1 (assoc (value-at :note-1 :text) :layer :bob)) :unknown-part]
   ["a fact with a value that carries :sealed: a fact has no such part" (arrived 1 (assoc (value-at :note-1 :text) :sealed (bytes-of 3))) :unknown-part]
   ["a fact with a value that carries :lock-id" (arrived 1 (assoc (value-at :note-1 :text) :lock-id [(act-id 50) 0])) :unknown-part]
   ["a fact with no value that carries :sealed and :lock-id" (arrived 1 {:e :note-1 :k :text :sealed (bytes-of 3) :lock-id [(act-id 50) 0]}) :unknown-part]
   ["a fact whose :sealed is nil: the part is there" (arrived 1 (assoc (value-at :note-1 :text) :sealed nil)) :unknown-part]
   ["a version that the code does not know" (assoc good :version 2) :unknown-version]
   ["no permission" (dissoc good :permission) :no-permission]
   ["no session" (dissoc good :session) :no-session]
   ["no fact" (assoc good :facts []) :empty-act]
   ["a permission for another layer" (assoc good :permission [:alice :bob :bob]) :permission-not-in-layer]
   ["a who that is a string" (assoc good :who "alice") :malformed]
   ["a permission 33 deep" (assoc good :permission (deep-permission :alice :alice 33)) :malformed]
   ["a value that is no value of the form: a ratio" (arrived 1 (value-at :note-1 :text 1/3)) :malformed]
   ["a value inside 33 collections" (arrived 1 (value-at :note-1 :text (nested 33))) :malformed]
   ["facts that are no sequence" (assoc good :facts {:e :note-1}) :malformed]
   ["a fact that is no map" (assoc good :facts [5]) :malformed]])

(def ^:private time-rows
  "The least thing with each kind of time on it. For each: the thing; the
  time that `softland.store.door.declaration/usable-time` gives for it, nil
  for none; and the word of the check. A double and a big integer hold the
  number of `now`, so their class alone is the fault. Below zero is the one
  bound: a time far ahead of every clock, and the greatest long, are
  usable."
  [["no time" (without-time good) nil :no-door-time]
   ["a time that is nil" (at-time nil) nil :no-door-time]
   ["a time that is a string" (at-time "1700000000000") nil :no-door-time]
   ["a time that is a double" (at-time 1.7E12) nil :no-door-time]
   ["a time that is a big integer" (at-time 1700000000000N) nil :no-door-time]
   ["a time that is a Java big integer" (at-time (java.math.BigInteger. "1700000000000")) nil :no-door-time]
   ["a time of -1" (at-time -1) nil :no-door-time]
   ["a time that is the least long" (at-time Long/MIN_VALUE) nil :no-door-time]
   ["a time of 0" (at-time 0) 0 :ok]
   ["a time that is a narrow integer" (at-time (int 1700001)) 1700001 :ok]
   ["the time of the proof's things" good now :ok]
   ["a time far ahead of every clock, 2^50 ms" (at-time far-ahead) far-ahead :ok]
   ["a time that is the greatest long" (at-time Long/MAX_VALUE) Long/MAX_VALUE :ok]])

(def ^:private doors-rows
  "One thing for each word that a part or a rule of the door's declaration
  gives: the least thing with one fault. The word of the time's part is
  earned in `time-rows`, by each kind of time that the shape does not take.
  The two words of the last step are earned in `limit-rows`, at their
  edges."
  [["no depot" (dissoc good :depot) :depot-not-the-act-ids-depot]
   ["a depot that is no depot word" (assoc good :depot :fast) :depot-not-the-act-ids-depot]
   ["a depot that is not the depot slot of the act-id" (assoc good :depot :microbatch) :depot-not-the-act-ids-depot]
   ["a value under a key in the store's namespace that the store does not know" (arrived 1 (value-at :alice :softland.store/owner :alice)) :store-key-not-known]
   ["a retract under such a key" (arrived 1 {:e :alice :k :softland.store/owner}) :store-key-not-known]
   ["the word class in the store's namespace: the store's key is depot" (arrived 1 {:e :alice :k :softland.store/class :v :streaming}) :store-key-not-known]
   ["a fact under :softland.store/lock-request: the store has no such key" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 4}) :store-key-not-known]
   ["a depot fact whose value is no depot word" (arrived 1 {:e :alice :k :softland.store/depot :v :fast}) :depot-fact-not-a-depot-word]
   ["a permission fact whose value is no permission" (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice]}) :permission-fact-not-a-permission]
   ["a permission fact that does not sit on its holder" (arrived 1 {:e :carol :k :softland.store/permission :v bobs}) :permission-fact-not-on-its-holder]
   ["who may see a layer, with a word that is neither" (arrived 1 {:e :alice :k :softland.store/who-may-see :v :owner}) :who-may-see-fact-not-members-or-everyone]
   ["who may see a layer, as a string" (arrived 1 {:e :alice :k :softland.store/who-may-see :v "everyone"}) :who-may-see-fact-not-members-or-everyone]
   ["who may see a layer, as a set of people" (arrived 1 {:e :alice :k :softland.store/who-may-see :v #{:bob}}) :who-may-see-fact-not-members-or-everyone]
   ["what a layer builds on, as one layer and no list" (arrived 1 {:e :alice :k :softland.store/builds-on :v :base}) :builds-on-fact-not-a-list-of-layers]
   ["what a layer builds on, as a set: a set has no order" (arrived 1 {:e :alice :k :softland.store/builds-on :v #{:base}}) :builds-on-fact-not-a-list-of-layers]
   ["what a layer builds on, with a string in it" (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base "team"]}) :builds-on-fact-not-a-list-of-layers]
   ["what a layer builds on, 65 layers" (arrived 1 {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "layer-" %)) (range 65))}) :builds-on-too-many-layers]
   ["the marked keys of a layer, as a vector" (arrived 1 {:e :alice :k :softland.store/marked-keys :v [:on]}) :marked-keys-fact-not-a-set-of-keys]
   ["the marked keys of a layer, as one key" (arrived 1 {:e :alice :k :softland.store/marked-keys :v :on}) :marked-keys-fact-not-a-set-of-keys]
   ["the marked keys of a layer, with a UUID in the set: a key is a keyword" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on (uuid 3)}}) :marked-keys-fact-not-a-set-of-keys]
   ["the marked keys of a layer, 33 keys" (arrived 1 {:e :alice :k :softland.store/marked-keys :v (set (map #(keyword (str "key-" %)) (range 33)))}) :too-many-marked-keys]
   ["a mark of a key of the store's own namespace" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on :softland.store/permission}}) :marked-key-in-the-stores-namespace]
   ["a mark of a key in the store's namespace that the store does not know" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store/owner}}) :marked-key-in-the-stores-namespace]
   ["two facts on one place" (arrived 1 (value-at :note-1 :text "a") (value-at :note-1 :text "b")) :place-named-twice]
   ["a fact that replaces a fact of its own act"
    (arrived 1 (value-at :note-1 :text) (assoc (value-at :note-2 :text) :replaces [(act-id 1) 0]))
    :replaces-own-fact]])

(def ^:private id-rows
  "The same long id in each place where an act holds an id. Each is refused
  with the one word."
  (let [long (long-id 257)]
    [["who" (assoc good :who long)]
     ["the session" (assoc good :session long)]
     ["a subject" (assoc good :subjects #{long})]
     ["an entity" (arrived 1 (value-at long :text))]
     ["a fact's key" (arrived 1 (value-at :note-1 long))]
     ["who and the holder of the act's permission" (assoc good :who long :permission [long :alice :alice])]
     ["granted-in, in the act's permission" (assoc good :permission [:alice :alice long])]
     ["the layer of the act-id in because-of" (assoc good :because-of [long :streaming (uuid 2)])]
     ["the layer of a fact id in based-on" (assoc good :based-on {[[long :streaming (uuid 2)] 0] 1})]
     ["the layer of a replaced fact" (arrived 1 (assoc (value-at :note-1 :text) :replaces [[long :streaming (uuid 2)] 0]))]
     ["the holder in a permission that is given" (arrived 1 {:e long :k :softland.store/permission :v [long :alice :alice alice-own]})]
     ["granted-in, in a permission that is given" (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice long alice-own]})]
     ["who, in the permission above one that is given with the slot that says what it allows"
      (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice :alice [long :alice :alice] :see]})]
     ["a layer in the list that a layer builds on" (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base long]})]
     ["a key in the set that a layer marks" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on long}})]]))

(def ^:private time-cases
  "The cases of the time on the act as the declaration reads it: what
  `softland.store.door.declaration/usable-time` gives for each row of
  `time-rows`; its class; a thing that is no plain map; a thing that throws
  when the part is read; and where the word of the time stands among the
  words of the check, by each thing of `makers-rows` and of `doors-rows`
  with its time taken off. The word of the check for each row of
  `time-rows` is held in `refusals-of-the-declaration`."
  (concat
   (map (fn [[what x usable _]]
          [(str "the usable time of the least thing with " what) (fn [] (declaration/usable-time x)) usable])
        time-rows)
   [["a usable time is a long, whatever fixed-width class the thing held: a long, an Integer, a Short, a Byte"
     (fn [] (mapv (fn [t] (let [usable (declaration/usable-time (at-time t))] [usable (.getSimpleName (class usable))]))
                  [7 (int 7) (short 7) (byte 7)]))
     (vec (repeat 4 [7 "Long"]))]
    ["a thing that is no plain map has no usable time: nil, a number, a string, a vector that holds the part's name and a time, a record that holds the part, a record with a field of the part's name"
     (fn [] (mapv declaration/usable-time
                  [nil 5 "a thing" [declaration/time-part now] (assoc (->Thing 1 2) declaration/time-part now) (->Timed now :note-1)]))
     [nil nil nil nil nil nil]]
    ["nothing throws: a sorted map with string keys throws when a keyword's part is read from it, and has no usable time; neither has a sequence that throws when it is read, or a map whose time is such a sequence"
     (fn [] (let [string-keys (sorted-map "a" 1 (name declaration/time-part) now)]
              [(try (get string-keys declaration/time-part) :read (catch ClassCastException _ :threw))
               (mapv declaration/usable-time [string-keys (throwing) (at-time (throwing))])]))
     [:threw [nil nil nil]]]
    ["the time's word stands after the word of a part that the form does not know, and before the word of each of the maker's own parts: of the things of `makers-rows` with the time taken off, the first, whose fault is such a part of the thing itself, keeps its word, and each of the other sixteen, whose fault is in a part of the maker's, gets the time's"
     (fn [] (let [given (mapv (fn [[_ x _]] (verdict (declaration/check (without-time x)))) makers-rows)]
              [(first given) (frequencies (rest given))]))
     [:unknown-part {:no-door-time 16}]]
    ["the time's word stands after the word of a depot that is absent or no depot word, and before each rule: of the things of `doors-rows` with the time taken off, the first two keep the depot's word, and each of the other twenty-three, whose fault a rule finds, gets the time's; a depot word that is not the act-id's is one of them"
     (fn [] (let [given (mapv (fn [[_ x _]] (verdict (declaration/check (without-time x)))) doors-rows)]
              [(vec (take 2 given)) (frequencies (drop 2 given))]))
     [[:depot-not-the-act-ids-depot :depot-not-the-act-ids-depot] {:no-door-time 23}]]]))

;; ------------------------------------------- the bounds of the check's last step

(def ^:private at-the-bound
  "A string whose one text is 65,536 bytes, the bound on one value: 21,844
  characters of three bytes each, two letters and the two quotes. It is
  21,848 characters long, so a count of characters would be far below the
  bound."
  (str (apply str (repeat 21844 "✓")) "aa"))

(def ^:private over-the-bound
  "A string whose one text is 65,537 bytes, one over the bound on one value:
  `at-the-bound` with one letter more."
  (str at-the-bound "a"))

(defn- full-act
  "An act whose accept would write 18,874,368 + `more` bytes of text into the
  rows of its facts, so with 0 it is at the bound on an act. Its who, its
  layer and each id of its permission print to 256 characters, and the
  permission is 32 deep: 256 + 24,735 bytes of who and permission for each
  fact. It has 256 facts. The value of each is a string of 48,735 letters,
  48,737 bytes of text, and the last has `more` letters more. No value is
  over the bound on one value."
  [more]
  (let [id (long-id 256)
        value (apply str (repeat 48735 "a"))]
    {:act-id [id :streaming (uuid 1)] :depot :streaming declaration/time-part now
     :version 1 :who id :layer id :permission (deep-permission id id 32) :session :alice-s1
     :facts (mapv (fn [i] {:e (keyword (str "note-" i)) :k :text
                           :v (if (= i 255) (str value (apply str (repeat more "a"))) value)})
                  (range 256))}))

(defn- bytes-in-rows
  "The proof's own count of the bytes of text that an accept of a thing would
  write into the rows of its facts: for each fact, the UTF-8 bytes of its
  value, none for a fact with no value, and the bytes of the act's who and
  of its permission. Each is counted as Clojure prints it. What must hold:
  Clojure prints each as its one text, which is so for a keyword, a vector
  of keywords, and a string with nothing to escape."
  [thing]
  (let [printed (memoize (fn [x] (alength ^bytes (utf8 (pr-str x)))))
        for-each-fact (+ (printed (:who thing)) (printed (:permission thing)))]
    (reduce + (map (fn [fact] (+ for-each-fact (if (some? (:v fact)) (printed (:v fact)) 0))) (:facts thing)))))

(def ^:private bound-cases
  "The cases of the two bounds that the last step of the check holds: the
  proof's own count of each thing at an edge, beside the word that
  `limit-rows` holds for it; and what the step gives for a thing that it
  refuses. They do not reach a gate: that a thing over a bound is in the log
  whole and is refused there with its word is the module's proof's."
  [["the proof's own count of the bytes of the two values at the edge of the bound on one value, and of their characters"
    (fn [] [(alength ^bytes (utf8 (pr-str at-the-bound))) (alength ^bytes (utf8 (pr-str over-the-bound)))
            (count (pr-str at-the-bound)) (count (pr-str over-the-bound))])
    [65536 65537 21848 21849]]
   ["the value at the bound is kept whole: its text in the one form is the 65,536 bytes that the proof counts, and it reads back as the value"
    (fn [] (let [kept (:text (first (:facts (act-of (arrived 1 (value-at :note-1 :text at-the-bound))))))]
             [(alength ^bytes (utf8 kept)) (= at-the-bound (edn/read-string kept))]))
    [65536 true]]
   ["the proof's own count of the bytes that the act at the edge of the bound on an act would write, and of the act one byte over; for each fact, of who and permission"
    (fn [] [(bytes-in-rows (full-act 0)) (bytes-in-rows (full-act 1))
            (bytes-in-rows (assoc (full-act 0) :facts [{:e :note-1 :k :text}]))])
    [18874368 18874369 24991]]
   ["the bytes of who and permission are counted for a fact with no value too: an act of 255 values and one retract, which is one byte over the bound only when the retract's row is counted, is refused; with one byte less it passes"
    (fn [] (let [letters (fn [n] (apply str (repeat n "a")))
                 with-a-retract (fn [more]
                                  (update (full-act 0) :facts
                                          (fn [facts] (vec (map-indexed (fn [i fact]
                                                                          (cond (= i 255) (dissoc fact :v)
                                                                                (zero? i) (assoc fact :v (letters (+ 64981 more)))
                                                                                (< i 3) (assoc fact :v (letters 64981))
                                                                                :else fact))
                                                                        facts)))))]
             [(bytes-in-rows (with-a-retract 0)) (verdict (declaration/check (with-a-retract 0)))
              (bytes-in-rows (with-a-retract -1)) (verdict (declaration/check (with-a-retract -1)))
              (- (bytes-in-rows (with-a-retract 0)) (bytes-in-rows (update (with-a-retract 0) :facts pop)))]))
    [18874369 :act-too-large 18874368 :ok 24991]]
   ["a thing that the last step refuses gives its word and no act: no part of its one form comes out"
    (fn [] [(declaration/check (arrived 1 (value-at :note-1 :text over-the-bound)))
            (declaration/check (full-act 1))])
    [{:refuse :value-too-large} {:refuse :act-too-large}]]])

(def ^:private order-rows
  "Things with two faults. The part that is declared first, the shape before
  a rule, the rule that is listed first, and a rule before the last step
  give the reason; in the last step, a value over its bound before an act
  over its bound. The time on the act is declared after the depot and
  before each of the maker's parts."
  [["a part that is not named, and a wrong version" (assoc good :owner 1 :version 2) :unknown-part]
   ["no depot, and a wrong version" (assoc (dissoc good :depot) :version 2) :depot-not-the-act-ids-depot]
   ["a part that is not named, and no time: the part that the form does not know" (assoc (without-time good) :owner 1) :unknown-part]
   ["a depot that is no depot word, and no time: the depot is declared before the time" (assoc (without-time good) :depot :fast) :depot-not-the-act-ids-depot]
   ["no depot, and no time" (dissoc (without-time good) :depot) :depot-not-the-act-ids-depot]
   ["no time, and a wrong version: the time is declared before each of the maker's parts" (assoc (without-time good) :version 2) :no-door-time]
   ["no time, and a who that is a string" (assoc (without-time good) :who "alice") :no-door-time]
   ["no time, and no permission" (dissoc (without-time good) :permission) :no-door-time]
   ["no time, and no fact" (assoc (without-time good) :facts []) :no-door-time]
   ["no time, and facts that throw when they are read" (assoc (without-time good) :facts (throwing)) :no-door-time]
   ["no time, and a fact with a part that the form does not name: that fault is inside a part of the maker's"
    (without-time (arrived 1 (assoc (value-at :note-1 :text) :layer :bob))) :no-door-time]
   ["no time, and a depot word that is not the act-id's: the shape comes before a rule" (assoc (without-time good) :depot :microbatch) :no-door-time]
   ["a time of -1, and a value over the bound: the shape comes before the last step"
    (assoc (arrived 1 (value-at :note-1 :text over-the-bound)) declaration/time-part -1) :no-door-time]   ["a wrong version, and facts that are no sequence" (assoc good :version 2 :facts 5) :unknown-version]
   ["a wrong version, and facts that throw when they are read" (assoc good :version 2 :facts (throwing)) :unknown-version]
   ["a good version, and facts that throw when they are read" (assoc good :facts (throwing)) :malformed]
   ["no session, and a value over the bound: the shape comes before the last step"
    (dissoc (arrived 1 (value-at :note-1 :text over-the-bound)) :session) :no-session]
   ["a fact that is no map, and another that carries :sealed: the first fact's"
    (assoc good :facts [5 (assoc (value-at :note-1 :text) :sealed (bytes-of 3))]) :malformed]
   ["the same two, the other way round"
    (assoc good :facts [(assoc (value-at :note-1 :text) :sealed (bytes-of 3)) 5]) :unknown-part]
   ["a permission for another layer, and a depot that is not the act-id's" (assoc good :permission [:alice :bob :bob] :depot :microbatch) :permission-not-in-layer]
   ["257 facts, each with a value over the bound: the limit of the facts, before one text is made"
    (apply arrived 1 (map (fn [i] (value-at (keyword (str "note-" i)) :text over-the-bound)) (range 257))) :too-many-facts]
   ["a depot fact whose value is no depot word, and a key in the store's namespace that the store does not know: the key gives the word"
    (arrived 1 {:e :alice :k :softland.store/depot :v :fast} (value-at :note-1 :softland.store/owner)) :store-key-not-known]
   ["a wrong word of who may see, and a permission fact that does not sit on its holder: the rules of a permission come first"
    (arrived 1 {:e :alice :k :softland.store/who-may-see :v :owner} {:e :carol :k :softland.store/permission :v bobs}) :permission-fact-not-on-its-holder]
   ["a wrong word of who may see, and a list that is no list: the rules of the three keys in their order"
    (arrived 1 {:e :alice :k :softland.store/builds-on :v :base} {:e :alice :k :softland.store/who-may-see :v :owner}) :who-may-see-fact-not-members-or-everyone]
   ["65 layers, and marked keys that are no set: the limit of the list before the shape of the marks"
    (arrived 1 {:e :alice :k :softland.store/marked-keys :v [:on]}
             {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "layer-" %)) (range 65))}) :builds-on-too-many-layers]
   ["33 marked keys with one of the store's namespace among them: the limit first"
    (arrived 1 {:e :alice :k :softland.store/marked-keys :v (conj (set (map #(keyword (str "key-" %)) (range 32))) :softland.store/depot)}) :too-many-marked-keys]
   ["a mark of a key of the store's namespace, and a value over the bound under a plain key: a rule comes before the last step"
    (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store/depot}} (value-at :note-1 :text over-the-bound)) :marked-key-in-the-stores-namespace]
   ["a value over the bound, and two facts on one place: the rule's word"
    (arrived 1 (value-at :note-1 :text over-the-bound) (value-at :note-1 :text "b")) :place-named-twice]
   ["a list of layers with a long id in it, and two facts on one place: the long id is the last rule"
    (arrived 1 {:e :alice :k :softland.store/builds-on :v [(long-id 257)]} (value-at :note-1 :text "a") (value-at :note-1 :text "b")) :place-named-twice]
   ["a value over the bound on an entity whose id is too long: the last rule comes before the last step"
    (arrived 1 (value-at (long-id 257) :text over-the-bound)) :id-too-long]
   ["a value over its bound in an act that is over its bound: the value's word, the first of the two"
    (assoc-in (full-act 1) [:facts 0 :v] over-the-bound) :value-too-large]
   ["the same with the value as the last fact of the act: the value's word still"
    (assoc-in (full-act 1) [:facts 255 :v] over-the-bound) :value-too-large]])

(defn- many-facts
  "An act with n facts, each with a value on its own place."
  [n]
  (apply arrived 1 (map (fn [i] (value-at (keyword (str "note-" i)) :text i)) (range n))))

(def ^:private limit-rows
  "Each limit at its edge."
  [["256 facts" (many-facts 256) :ok]
   ["257 facts" (many-facts 257) :too-many-facts]
   ["1,024 entries of based-on" (assoc good :based-on (into {} (map (fn [i] [[(act-id 2) i] 1])) (range 1024))) :ok]
   ["1,025 entries of based-on" (assoc good :based-on (into {} (map (fn [i] [[(act-id 2) i] 1])) (range 1025))) :too-many-based-on]
   ["a carried stamp of 2^62, and one that is the greatest long: a carried stamp has no bound above, since no gate reads it"
    (assoc good :based-on {[(act-id 2) 0] (bit-shift-left 1 62) [(act-id 2) 1] Long/MAX_VALUE}) :ok]
   ["a carried stamp of -1: no stamp is below zero" (assoc good :based-on {[(act-id 2) 0] -1}) :malformed]
   ["a value whose one text is 65,536 bytes, in 21,848 characters" (arrived 1 (value-at :note-1 :text at-the-bound)) :ok]
   ["a value whose one text is 65,537 bytes, in 21,849 characters: the bound counts bytes" (arrived 1 (value-at :note-1 :text over-the-bound)) :value-too-large]
   ["a string of 65,534 letters, 65,536 bytes of text" (arrived 1 (value-at :note-1 :text (text-of-bytes 65536))) :ok]
   ["a string of 65,535 letters, 65,537 bytes of text" (arrived 1 (value-at :note-1 :text (text-of-bytes 65537))) :value-too-large]
   ["an act whose accept would write 18,874,368 bytes of text into the rows of its facts" (full-act 0) :ok]
   ["the same act with one byte more in one value" (full-act 1) :act-too-large]
   ["an entity of 256 characters" (arrived 1 (value-at (long-id 256) :text)) :ok]
   ["an entity of 257 characters" (arrived 1 (value-at (long-id 257) :text)) :id-too-long]
   ["what a layer builds on, 64 layers" (arrived 1 {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "layer-" %)) (range 64))}) :ok]
   ["what a layer builds on, 65 layers" (arrived 1 {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "layer-" %)) (range 65))}) :builds-on-too-many-layers]
   ["32 marked keys" (arrived 1 {:e :alice :k :softland.store/marked-keys :v (set (map #(keyword (str "key-" %)) (range 32)))}) :ok]
   ["33 marked keys" (arrived 1 {:e :alice :k :softland.store/marked-keys :v (set (map #(keyword (str "key-" %)) (range 33)))}) :too-many-marked-keys]
   ["a layer of 256 characters in the list that a layer builds on, and a marked key of 256" (arrived 1 {:e :alice :k :softland.store/builds-on :v [(long-id 256)]} {:e :alice :k :softland.store/marked-keys :v #{(long-id 256)}}) :ok]
   ["an act under its own permission five deep" (assoc good :permission (deep-permission :alice :alice 5)) :ok]
   ["an act under its own permission 32 deep" (assoc good :permission (deep-permission :alice :alice 32)) :ok]
   ["an act under its own permission 32 deep that says what it allows"
    (assoc good :permission [:alice :alice :alice (deep-permission :alice :alice 31) :see-and-act]) :ok]
   ["an act under its own permission 33 deep" (assoc good :permission (deep-permission :alice :alice 33)) :malformed]
   ["a permission fact whose value is five deep" (arrived 1 {:e :bob :k :softland.store/permission :v (deep-permission :bob :alice 5)}) :ok]
   ["a permission fact whose value is 32 deep" (arrived 1 {:e :bob :k :softland.store/permission :v (deep-permission :bob :alice 32)}) :ok]
   ["a permission fact whose value is 32 deep and says what it allows"
    (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice :alice (deep-permission :bob :alice 31) :see]}) :ok]
   ["a permission fact whose value is 33 deep: it is no value of the form, so the shape's word and not the rule's"
    (arrived 1 {:e :bob :k :softland.store/permission :v (deep-permission :bob :alice 33)}) :malformed]
   ["ids that are UUIDs: who, the layer, an entity"
    (let [who (uuid 31) layer (uuid 32)]
      {:act-id [layer :streaming (uuid 1)] :depot :streaming declaration/time-part now :version 1 :who who :layer layer
       :permission [who layer layer] :session (uuid 33)
       :facts [{:e (uuid 34) :k :text :v "a text"}]})
    :ok]])

(def ^:private not-acts
  "Things that are no acts. The check gives data for each and never throws."
  [["nil" nil :malformed]
   ["a number" 5 :malformed]
   ["a string" "act" :malformed]
   ["a record" (->Thing 1 2) :malformed]
   ["a sequence that throws" (throwing) :malformed]
   ["a map with only an act-id" {:act-id (act-id 1)} :depot-not-the-act-ids-depot]
   ["an act whose facts throw when they are read" (assoc good :facts (throwing)) :malformed]
   ["an act whose based-on throws" (assoc good :based-on (throwing)) :malformed]])

(def ^:private read-predicate-cases
  "The two tests of what a caller gave a read: an id, and an act-id of a
  layer."
  [["an id that a key can be made of: a keyword, a UUID; not a string, nil, a long keyword, a keyword that does not read back"
    (fn [] (mapv declaration/usable-id? [:alice (uuid 1) "alice" nil (long-id 257) (keyword "a b") (long-id 256)]))
    [true true false false false false true]]
   ["an act-id of a layer: its shape and its layer slot; anything else is none, and nothing throws"
    (fn [] (mapv #(declaration/act-id-in-layer? :alice %)
                 [(act-id 1) [:alice :microbatch (uuid 1)] [:bob :streaming (uuid 1)] [:alice :streaming]
                  nil 5 (throwing)]))
    [true true false false false false false]]])

;; ------------------------------------------------------------------ the stamp

(def ^:private stamp-cases
  "The cases of the stamp: the first stamp of a layer, the next one, a run
  of them, the class of a stamp, its limit, and what the function takes. A
  stamp holds no time: what a stamp is not made of is held where a decision
  is made, in `writes-cases` and `stamp-line-cases`."
  [["the first decision in a layer, with no last stamp: the stamp is 0, a long"
    (fn [] (let [first-stamp (stamp/next-stamp nil)] [first-stamp (.getSimpleName (class first-stamp))]))
    [0 "Long"]]
   ["a decision after a last stamp: that stamp plus one, a long also when the last stamp is given as an Integer"
    (fn [] (mapv (fn [last-stamp] (let [s (stamp/next-stamp last-stamp)] [s (.getSimpleName (class s))])) [0 41 (int 41)]))
    [[1 "Long"] [42 "Long"] [42 "Long"]]]
   ["1,000 decisions in a row in one layer: the stamps are 0, 1, 2 and on to 999, with no gap"
    (fn [] (= (range 1000) (take 1000 (iterate stamp/next-stamp (stamp/next-stamp nil)))))
    true]
   ["the limit: after the greatest long the next stamp wraps to the least, and nothing throws"
    (fn [] (stamp/next-stamp Long/MAX_VALUE))
    Long/MIN_VALUE]
   ["the stamp's namespace gives one function, and it takes the layer's last stamp alone: no time, and nothing of the act, goes into a stamp"
    (fn [] [(vec (sort (map name (keys (ns-publics 'softland.store.gate.stamp)))))
            (:arglists (meta #'stamp/next-stamp))])
    [["next-stamp"] '([last-stamp])]]])

;; -------------------------------------------------------------- the first act

(defn- making
  "A making act in its one form on arrival: for a layer, by `who`, naming a
  first permission held by `holder`, with the depot that the layer starts
  with. `first-act-cases` holds it against what the declaration's check
  gives for the same act as its maker sends it."
  [layer who holder]
  (let [permission [holder layer layer]
        depot (first-act/depot-at-making layer)]
    {:act-id [layer depot (uuid 1)] :depot depot declaration/time-part now :version 1 :who who :layer layer
     :permission permission :session :s1 :based-on {} :because-of nil :claimed-when nil :subjects #{}
     :facts [(arrived-fact layer :softland.store/depot depot)
             (arrived-fact holder :softland.store/permission permission)]}))

(def ^:private first-act-rows
  "Acts into a layer that is not made. For each: the gate's depot, the act,
  and whether it has the form of a making act."
  (let [personal (making :alice :system :alice)
        group (making :team :alice :alice)
        base (making :base :system :system)
        fact (fn [act i] (get-in act [:facts i]))]
    [["a personal layer, made by the system for its person" :streaming personal true]
     ["a group's layer, made by the person who creates it" :streaming group true]
     ["the two facts in the other order" :streaming (update group :facts (comp vec reverse)) true]
     ["a layer whose first holder is the system" :streaming (making :tools :system :system) true]
     ["the base, made by the system" :microbatch base true]
     ["the base" :streaming base false]
     ["the base by a who that is not the system" :microbatch (assoc base :who :alice) false]
     ["the base with a first permission that is not the system's" :microbatch (making :base :system :alice) false]
     ["the base, made by a person who holds its first permission herself: she is the holder, as for a group's layer, and for the base the holder is the system"
      :microbatch (making :base :alice :alice) false]
     ["a layer that is not the base" :microbatch group false]
     ["a who that is neither the holder nor the system" :streaming (assoc group :who :bob) false]
     ["no depot fact" :streaming (assoc group :facts [(fact group 1) (arrived-fact :note :text)]) false]
     ["no permission fact" :streaming (assoc group :facts [(fact group 0) (arrived-fact :note :text)]) false]
     ["one fact only" :streaming (assoc group :facts [(fact group 0)]) false]
     ["a third fact beside the two" :streaming (update group :facts conj (arrived-fact :note :text)) false]
     ["a third fact that says who may see the layer: nothing is written at the making but the depot and the first permission"
      :streaming (update group :facts conj (arrived-fact :team :softland.store/who-may-see :everyone)) false]
     ["the depot microbatch for a layer that is not the base" :streaming (assoc-in group [:facts 0 :v] :microbatch) false]
     ["a depot fact on an entity that is not the layer" :streaming (assoc-in group [:facts 0 :e] :other) false]
     ["a permission fact that is not the permission that the act names"
      :streaming (-> group (assoc-in [:facts 1 :v] [:bob :team :team]) (assoc-in [:facts 1 :e] :bob)) false]
     ["a first permission with a permission above it"
      :streaming (-> group
                     (assoc :permission [:alice :team :team [:x :team :team]])
                     (assoc-in [:facts 1 :v] [:alice :team :team [:x :team :team]]))
      false]
     ["a first permission granted in another layer"
      :streaming (-> group (assoc :permission [:alice :team :elsewhere]) (assoc-in [:facts 1 :v] [:alice :team :elsewhere]))
      false]
     ["a permission that says what it allows, see and act: it has a permission above it and a slot, so it is no first permission"
      :streaming (-> group
                     (assoc :permission [:alice :team :team [:x :team :team] :see-and-act])
                     (assoc-in [:facts 1 :v] [:alice :team :team [:x :team :team] :see-and-act]))
      false]]))

(def ^:private first-act-cases
  "The cases of the first act: the depot that a layer starts with, whether a
  layer starts with the other gate's depot, and one case for each row of
  `first-act-rows`."
  (concat
   [["the depot that a layer starts with: microbatch for the base, streaming for any other"
     (fn [] (mapv first-act/depot-at-making [:base :alice :team (uuid 1)]))
     [:microbatch :streaming :streaming :streaming]]
    ["a layer that starts with the other gate's depot: at the streaming gate's depot the base, and no other layer, a keyword or a UUID; at the microbatch gate's depot each layer but the base"
     (fn [] [(mapv #(first-act/starts-with-the-other-gates-depot? :streaming %) [:base :alice :team (uuid 1)])
             (mapv #(first-act/starts-with-the-other-gates-depot? :microbatch %) [:base :alice :team (uuid 1)])])
     [[true false false false] [false true true true]]]
    ["no act that has the form of a making act for a gate is for a layer that starts with the other gate's depot: each of the five such rows below, at its gate; and the base's making act at the streaming gate's depot is for such a layer"
     (fn [] [(mapv (fn [[_ gate-depot act _]] (first-act/starts-with-the-other-gates-depot? gate-depot (:layer act)))
                   (filter (fn [[_ _ _ making?]] making?) first-act-rows))
             (first-act/starts-with-the-other-gates-depot? :streaming (:layer (making :base :system :system)))])
     [[false false false false false] true]]
    ["the two ids that the form knows"
     (fn [] [first-act/system first-act/base])
     [:system :base]]
    ["the making acts of these cases are in the one form on arrival: each is what the declaration's check gives for the same act as its maker sends it"
     (fn [] (mapv (fn [form] (= form (act-of (-> form
                                                 (dissoc :based-on :because-of :claimed-when :subjects)
                                                 (update :facts (fn [facts] (mapv #(select-keys % [:e :k :v]) facts)))))))
                  [(making :alice :system :alice) (making :team :alice :alice) (making :base :system :system)]))
     [true true true]]
    ["what a layer starts with, where no fact says it: the base is seen by everyone and any other layer by its members; a layer builds on nothing; no key is marked; nothing for any other key"
     (fn [] [(mapv #(first-act/starts-with % :softland.store/who-may-see) [:base :alice :team (uuid 1)])
             (mapv #(first-act/starts-with % :softland.store/builds-on) [:base :alice])
             (mapv #(first-act/starts-with % :softland.store/marked-keys) [:base :alice])
             (mapv #(first-act/starts-with :alice %) [:softland.store/depot :softland.store/permission :text])])
     [[:everyone :members :members :members] [[] []] [#{} #{}] [nil nil nil]]]]
   (map (fn [[what gate-depot act expected]]
          [(str "a making act? " what) (fn [] (first-act/making-act? gate-depot act)) expected])
        first-act-rows)))

;; ---------------------------------------------------------- the record's rows

(defn- classes
  "The simple class name of each value of a row, by field."
  [row]
  (into {} (map (fn [[k v]] [k (.getSimpleName (class v))])) row))

(def ^:private writer
  "The two texts that each row of a fact written by Alice under her own
  permission holds: of who wrote it and of the permission that the act
  named. Written out here, where a gate makes them once for an act."
  [":alice" "[:alice :alice :alice]"])

(def ^:private written-by-alice
  "The two fields that each row of a fact written by `writer` holds: who
  wrote it and the permission that the act named, each as its one text."
  {:who ":alice" :permission "[:alice :alice :alice]"})

(defn- fact-row
  "The row of a fact's place, as `softland.store.record.rows/fact-row` gives
  it. Takes the texts of who and of the permission as a pair; a fact in its
  one form on arrival; the uuid of its act; its index; the stamp; and the
  time on the act."
  [[who-text permission-text] fact act-uuid index stamp time-millis]
  (record/fact-row fact act-uuid index stamp time-millis who-text permission-text))

(defn- by-value-row
  "A fact's row in the facts by value, as
  `softland.store.record.rows/by-value-row` gives it. Takes the texts of who
  and of the permission as a pair; the uuid of the act; the fact's index;
  the stamp; and the time on the act."
  [[who-text permission-text] act-uuid index stamp time-millis]
  (record/by-value-row who-text permission-text act-uuid index stamp time-millis))

(defn- digest-of-text
  "The SHA-256 of the UTF-8 of a string, as hex: what the order by value must
  hold for a value whose one text is that string."
  [s]
  (sha-256 (utf8 s)))

(def ^:private record-cases
  "The cases of the record's rows: the keys, each row with the class of each
  field, what each row says, the facts by value, the heads of the two
  reads, the routing id of an act-id, and the parts of an answer's key."
  (let [u (uuid 5)
        digest (digest/digest good)
        value-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} u (int 2) (int 900) (int 1700001))
        plain-row (fact-row writer {:e :alice :k :softland.store/depot :v :streaming :replaces nil :text ":streaming"} u 0 900 now)
        retract-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text nil} u 1 900 now)
        uuid-who "#uuid \"00000000-0000-0000-0000-00000000004d\""
        by-uuid (fact-row [uuid-who (str "[" uuid-who " :alice :alice [:alice :alice :alice] :see-and-act]")]
                          {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} u 0 900 now)
        marks-row (record/with-level-since
                   (fact-row writer (arrived-fact :alice :softland.store/marked-keys #{:on :tool/parent}) u 0 900 now)
                   {:on 100 :tool/parent 140})
        row-holding (fn [a-text] {:value a-text :by-act u :by-index 0 :stamp 900})
        the-bytes (fn [& xs] (byte-array (map unchecked-byte xs)))
        no-text (str "\u0000" (char 255) (char 254))
        d-c1 (digest-of-text ":c1")]
    [["the two keys: an answer, a place"
      (fn [] [(record/answer-key :alice u) (record/place-key :alice :note-1 :tool/text)
              (record/place-key (uuid 1) (uuid 2) :text)])
      [":alice 00000000-0000-0000-0000-000000000005"
       ":alice :note-1 :tool/text"
       "00000000-0000-0000-0000-000000000001 00000000-0000-0000-0000-000000000002 :text"]]
     ["the longest keys that the limits allow: an answer 293 characters, a place 770, a key of the facts by value 855"
      (fn [] (let [id (long-id 256)]
               [(count (record/answer-key id u)) (count (record/place-key id id id))
                (count (record/by-value-key id id Long/MAX_VALUE (record/value-digest (byte-array 0)) id))]))
      [293 770 855]]
     ["an accepted answer's row has no :refused; a refused one has its word; each holds the time on the thing beside the stamp; each field of its declared class"
      (fn [] [(classes (record/answer-row nil (int 900) (int 1700001) digest)) (classes (record/answer-row :layer-not-made 900 now digest))])
      [{:stamp "Long" :time "Long" :digest "byte[]"} {:stamp "Long" :time "Long" :digest "byte[]" :refused "Keyword"}]]
     ["the time in an answer's row is the time given, as it is: a time of 0 and a time far ahead of every clock; a thing with no usable time gives a row with no :time, refused or accepted"
      (fn [] [(:time (record/answer-row nil 900 0 digest)) (:time (record/answer-row nil 900 far-ahead digest))
              (record/answer-row :no-door-time 900 nil digest) (contains? (record/answer-row nil 900 nil digest) :time)])
      [0 far-ahead {:refused :no-door-time :stamp 900 :digest digest} false]]
     ["an answer as a caller gets it, accepted and refused, with the time on the thing; a nil time for a row with none; no row is nothing yet"
      (fn [] [(record/answer-of (record/answer-row nil 900 now digest))
              (record/answer-of (record/answer-row :layer-not-made 901 now digest))
              (record/answer-of (record/answer-row :no-door-time 902 nil digest))
              (record/answer-of nil)])
      [{:verdict :accepted :reason nil :stamp 900 :time now :digest (text/hex digest)}
       {:verdict :refused :reason :layer-not-made :stamp 901 :time now :digest (text/hex digest)}
       {:verdict :refused :reason :no-door-time :stamp 902 :time nil :digest (text/hex digest)}
       nil]]
     ["the row of a fact with a value under a plain key: its value is the fact's text as it is, a String, and the row gives the UTF-8 of it back; the fact that wrote it, the stamp, the time on the act, who wrote it and the permission that the act named; each field of its declared class, and none a byte array"
      (fn [] [value-row (classes value-row) (record/plain-bytes-of value-row)])
      [(merge {:value "\"a text\"" :by-act u :by-index 2 :stamp 900 :time 1700001} written-by-alice)
       {:value "String" :by-act "UUID" :by-index "Long" :stamp "Long" :time "Long" :who "String" :permission "String"}
       (utf8 "\"a text\"")]]
     ["the row of a fact under a store key: the text of its value as a String, whose bytes the row gives back; the fact that wrote it, the stamp, the time, who and the permission, and no other field"
      (fn [] [plain-row (record/plain-bytes-of plain-row)])
      [(merge {:value ":streaming" :by-act u :by-index 0 :stamp 900 :time now} written-by-alice)
       (utf8 ":streaming")]]
     ["the row takes the fact's text and reads nothing of the fact's value: a fact that holds a value and no text gives the row of a retract, and one that holds a text and another value gives the text"
      (fn [] [(contains? (fact-row writer {:e :note-1 :k :text :v :a-value :replaces nil :text nil} u 0 900 now) :value)
              (:value (fact-row writer {:e :alice :k :softland.store/depot :v :streaming :replaces nil :text ":another"} u 0 900 now))])
      [false ":another"]]
     ["the plain bytes of a row's String are its UTF-8, written out here byte by byte: characters of two, three and four bytes; a String that begins with U+0000 has that byte first; nothing for no row and for the row of a retract"
      (fn [] [(record/plain-bytes "é✓𝄞") (record/plain-bytes "\u0000a")
              (record/plain-bytes-of nil) (record/plain-bytes-of retract-row)])
      [(the-bytes -61 -87 -30 -100 -109 -16 -99 -124 -98) (the-bytes 0 97) nil nil]]
     ["the row of a retract: no value, and the row is there; it holds who retracted, under which permission and at what time, and no other field"
      (fn [] retract-row)
      (merge {:by-act u :by-index 1 :stamp 900 :time now} written-by-alice)]
     ["who wrote a row and the permission that its act named are each the one text, so a who that is a UUID reads back as a UUID; nothing for no row, for a row with neither, and for a text that is no id or no permission"
      (fn [] [(record/who-of value-row) (record/permission-of value-row)
              (:who by-uuid) (record/who-of by-uuid) (record/permission-of by-uuid)
              (act/allows (record/permission-of by-uuid))
              (record/who-of nil) (record/permission-of nil) (record/who-of (row-holding "x")) (record/permission-of (row-holding "x"))
              (record/who-of {:who "\"alice\""}) (record/who-of {:who "(((("}) (record/who-of {:who ""})
              (record/permission-of {:permission "[:alice :alice]"}) (record/permission-of {:permission "#bogus 1"})])
      [:alice alice-own
       "#uuid \"00000000-0000-0000-0000-00000000004d\"" (uuid 77) [(uuid 77) :alice :alice alice-own :see-and-act]
       :see-and-act
       nil nil nil nil nil nil nil nil nil]]
     ["which fact wrote a row: the one named; not another index, uuid, layer or depot; not an empty place"
      (fn [] (mapv #(record/written-by? :streaming :alice value-row %)
                   [[[:alice :streaming u] 2] [[:alice :streaming u] 1] [[:alice :streaming (uuid 6)] 2]
                    [[:bob :streaming u] 2] [[:alice :microbatch u] 2]]))
      [true false false false false]]
     ["no fact wrote an empty place"
      (fn [] (record/written-by? :streaming :alice nil [[:alice :streaming u] 2]))
      false]
     ["a row holds a value when its String is the one text of that value: a depot word, a tool's value, and a map given in another order and with a decimal that has a trailing zero; not another value, not the text of the value as a string, not a retract, not an empty place"
      (fn [] [(record/holds-value? plain-row :streaming) (record/holds-value? plain-row :microbatch)
              (record/holds-value? value-row "a text") (record/holds-value? value-row :streaming)
              (record/holds-value? (row-holding "{:a [1 2] :b 1.5M}") (array-map :b 1.50M :a [1 2]))
              (record/holds-value? plain-row ":streaming")
              (record/holds-value? retract-row :streaming) (record/holds-value? nil :streaming)])
      [true false true false true false false false]]
     ["the first form of a row's String, read back: a depot, a permission, a tool's value; nothing for a retract, for no row, and for a String that reads as no form"
      (fn [] [(record/value-of plain-row)
              (record/value-of (row-holding "[:bob :alice :alice [:alice :alice :alice]]"))
              (record/value-of value-row) (record/value-of retract-row) (record/value-of nil)
              (record/value-of (row-holding "(((("))])
      [:streaming bobs "a text" nil nil nil]]
     ["what a row says of its value, for a reader: no fact; nothing here; the value as its maker gave it; a String that is no value of the form"
      (fn [] [(record/read-value nil) (record/read-value retract-row) (record/read-value value-row) (record/read-value plain-row)
              (record/read-value (row-holding "#uuid \"00000000-0000-0000-0000-000000000009\""))
              (record/read-value (row-holding (text/text {:a [1 #{2}] "b c" 1.5})))
              (record/read-value (row-holding "42")) (record/read-value (row-holding "true"))])
      [[:no-fact] [:nothing-here] [:value "a text"] [:value :streaming]
       [:value (uuid 9)] [:value {:a [1 #{2}] "b c" 1.5}] [:value 42] [:value true]]]
     ["a String that is no value of the form is told so, and nothing throws: no form, a form with more after it, a value in another text than its one text, a list, a symbol, a tag that is not known, a String that begins with U+0000, with a text after it too, and no character at all. Each is a row that the proof writes by hand: no write of a gate gives one"
      (fn [] (mapv record/read-value
                   [(row-holding "((((") (row-holding ":a :b") (row-holding " :a") (row-holding "{:b 1, :a 2}")
                    (row-holding "(1 2)") (row-holding "sym") (row-holding "#bogus 1")
                    (row-holding no-text) (row-holding "\u0000\"a\"") (row-holding "")]))
      (vec (repeat 10 [:not-a-value]))]
     ["a row whose String is no value of the form: a reader is told so, it holds no value that a maker can give, and it is the same row as one with the same String only; a row of a value's text holds that value"
      (fn [] [(record/read-value (row-holding no-text))
              (record/holds-value? (row-holding no-text) "a text") (record/holds-this-value? (row-holding no-text) "a text")
              (record/same-row? (row-holding no-text) (row-holding no-text))
              (record/same-row? (row-holding no-text) (row-holding (str no-text "!")))
              (record/holds-this-value? (row-holding ":c1") :c1) (record/holds-value? (row-holding ":c1") :c1)])
      [[:not-a-value] false false true false true true]]
     ["the row at a layer's marked-keys place holds, beside the set that a maker wrote, the stamp of each marked key as one text; it reads back as a map; nothing is marked for no row, for a row without the field and for a text that is no such map"
      (fn [] [(:level-since marks-row) (classes marks-row) (record/marked-keys-of marks-row) (record/value-of marks-row)
              (record/marked-keys-of nil) (record/marked-keys-of plain-row)
              (record/marked-keys-of {:level-since "(((("}) (record/marked-keys-of {:level-since "[:on 100]"})
              (record/marked-keys-of {:level-since "{:on \"100\"}"}) (record/marked-keys-of {:level-since "{\"on\" 100}"})])
      ["{:on 100 :tool/parent 140}"
       {:value "String" :by-act "UUID" :by-index "Long" :stamp "Long" :time "Long" :who "String" :permission "String" :level-since "String"}
       {:on 100 :tool/parent 140} #{:on :tool/parent}
       {} {} {} {} {} {}]]
     ["the value digest: the SHA-256 of a row's value bytes, 64 hex characters; of a value, the digest of the UTF-8 of its one text"
      (fn [] [(record/value-digest (utf8 ":c1")) (count (record/value-digest (byte-array 0)))
              (record/value-digest-of :c1) (record/value-digest-of (uuid 9)) (record/value-digest-of "a string with spaces")
              (record/value-digest-of 42) (record/value-digest-of {:b 1 :a [2 3]}) (record/value-digest-of (int 42))])
      [d-c1 64
       d-c1 (digest-of-text "#uuid \"00000000-0000-0000-0000-000000000009\"") (digest-of-text "\"a string with spaces\"")
       (digest-of-text "42") (digest-of-text "{:a [2 3] :b 1}") (digest-of-text "42")]]
     ["a key of the facts by value: layer, key, the stamp since which the key is marked, the value digest, then the entity; its row is the fact's row without its value, with the two texts and the time as they were given"
      (fn [] (let [row (by-value-row writer u (int 2) (int 900) (int 1700001))]
               [(record/by-value-key :alice :on 100 d-c1 :b1)
                (record/by-value-key (uuid 1) :tool/on 140 d-c1 (uuid 2))
                row (classes row)
                (= (dissoc value-row :value) row)]))
      [(str ":alice :on 100 " d-c1 " :b1")
       (str "00000000-0000-0000-0000-000000000001 :tool/on 140 " d-c1 " 00000000-0000-0000-0000-000000000002")
       (merge {:by-act u :by-index 2 :stamp 900 :time 1700001} written-by-alice)
       {:by-act "UUID" :by-index "Long" :stamp "Long" :time "Long" :who "String" :permission "String"}
       true]]
     ["the run of one value: from the run's text with a space to the same text with an exclamation mark; a key of that value is in it, whatever its entity"
      (fn [] (let [[from to] (record/by-value-run :alice :on 100 :c1)
                   in? (fn [k] (and (<= (compare from k) 0) (neg? (compare k to))))]
               [from to
                (mapv #(in? (record/by-value-key :alice :on 100 d-c1 %)) [:b1 :zzzz (uuid 0) (UUID. -1 -1) (long-id 256)])]))
      [(str ":alice :on 100 " d-c1 " ") (str ":alice :on 100 " d-c1 "!")
       [true true true true true]]]
     ["no run holds a row of another value, layer, key or stamp: for a keyword, a UUID, a string with spaces, a number, a map and a value whose text is 65,536 bytes, the keys of the five others, and of the same value in another layer, under another key and under another stamp, are outside its run"
      (fn [] (let [values [:c1 (uuid 9) "a string with spaces" 42 {:a 1} (text-of-bytes 65536)]
                   in-run? (fn [[from to] k] (and (<= (compare from k) 0) (neg? (compare k to))))
                   key-of (fn [layer k since v] (record/by-value-key layer k since (record/value-digest-of v) :b1))]
               [(mapv (fn [v] (alength ^bytes (utf8 (pr-str v)))) [(last values)])
                (vec (for [v values]
                       (let [run (record/by-value-run :alice :on 100 v)]
                         [(in-run? run (key-of :alice :on 100 v))
                          (count (filter #(in-run? run (key-of :alice :on 100 %)) values))
                          (in-run? run (key-of :alice2 :on 100 v))
                          (in-run? run (key-of :alice :on2 100 v))
                          (in-run? run (key-of :alice :on 1000 v))
                          (in-run? run (key-of :alice :on 10 v))])))
                (count (set (map record/value-digest-of values)))
                (every? #(re-matches #"[0-9a-f]{64}" (record/value-digest-of %)) values)]))
      [[65536]
       (vec (repeat 6 [true 1 false false false false]))
       6 true]]
     ["the two texts of a UUID do not meet: the digest is always taken of its one text, never of its 36 characters; 1 and 1.0 are two values with two runs"
      (fn [] [(= (record/value-digest-of (uuid 9)) (digest-of-text (str (uuid 9))))
              (= (record/value-digest-of (uuid 9)) (digest-of-text "#uuid \"00000000-0000-0000-0000-000000000009\""))
              (= (record/by-value-run :alice :on 100 1) (record/by-value-run :alice :on 100 1.0))])
      [false true false]]
     ["a key back to its parts: a printed id to the id, a place's key to layer, entity and key, a key of the facts by value to its entity; nothing for a text that is none"
      (fn [] [(mapv record/id-of-text [":alice" ":tool/on" "00000000-0000-0000-0000-000000000005" "alice" ":a b" ":" "" "5-5-5-5-5" nil 7])
              (record/place-of-key (record/place-key :alice (uuid 2) :tool/text))
              (record/place-of-key (record/place-key :base :base :softland.store/marked-keys))
              (mapv record/place-of-key [":alice :note-1" ":alice :note-1 :text :more" ":alice :note-1 00000000-0000-0000-0000-000000000005" "" nil])
              (record/by-value-entity (record/by-value-key :alice :on 100 d-c1 (uuid 2)))
              (record/by-value-entity (record/by-value-key :alice :on 100 d-c1 :b1))
              (mapv record/by-value-entity [":alice :on 100" "" nil])])
      [[:alice :tool/on (uuid 5) nil nil nil nil nil nil nil]
       [:alice (uuid 2) :tool/text]
       [:base :base :softland.store/marked-keys]
       [nil nil nil nil nil]
       (uuid 2) :b1
       [nil nil nil]]]
     ["the head of the read by act-id: the depot and the key; nothing for what is no act-id of the layer"
      (fn [] [(record/answer-read :alice [:alice :streaming u]) (record/answer-read :alice [:alice :microbatch u])
              (record/answer-read :bob [:alice :streaming u]) (record/answer-read :alice [:alice :streaming])
              (record/answer-read nil nil) (record/answer-read (long-id 257) [(long-id 257) :streaming u])
              (record/answer-read :alice 5)])
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":alice 00000000-0000-0000-0000-000000000005"]
       nil nil nil nil nil]]
     ["the routing id of an act-id is its layer, for an act-id of each depot: a streaming one and a microbatch one, of a layer that is a keyword and of one that is a UUID. The uuid plays no part: two act-ids of one layer have one routing id"
      (fn [] [(record/routing-id [:alice :streaming u]) (record/routing-id [:base :microbatch u])
              (record/routing-id [(uuid 3) :streaming u]) (record/routing-id [(uuid 3) :microbatch u])
              (record/routing-id [:alice :microbatch u])
              (= (record/routing-id [:base :microbatch u]) (record/routing-id [:base :microbatch (uuid 6)]))])
      [:alice :base (uuid 3) (uuid 3) :alice true]]
     ["no routing id for a thing that is no act-id: two slots, four slots, a depot word that is none, a uuid slot that holds a text, a list, a number, a text, nothing, a thing that throws when it is read; and nothing throws"
      (fn [] (mapv record/routing-id [[:base :microbatch] [:base :microbatch u u] [:base :fast u] [:base :microbatch (str u)]
                                      (list :base :microbatch u) 5 "an act-id" nil (throwing)]))
      (vec (repeat 9 nil))]
     ["the head of the read by act-id, with the routing id: with the act-id's layer it gives the depot and the key, which is what `answer-read` gives, for an act-id of each depot; the routing id of an act-id is the one that its own read takes"
      (fn [] (let [of-alice [:alice :streaming u]
                   of-the-base [:base :microbatch u]]
               [(record/answer-read-by-routing-id :alice of-alice)
                (record/answer-read-by-routing-id :base of-the-base)
                (mapv (fn [id] (= (record/answer-read (nth id 0) id) (record/answer-read-by-routing-id (record/routing-id id) id)))
                      [of-alice of-the-base])]))
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":base 00000000-0000-0000-0000-000000000005"]
       [true true]]]
     ["nothing for another routing id: the act-id's uuid, another layer, nothing; nothing for a thing that is no act-id; nothing for a layer over the limit, whose act-id has a routing id all the same"
      (fn [] [(record/answer-read-by-routing-id u [:base :microbatch u])
              (record/answer-read-by-routing-id u [:alice :streaming u])
              (record/answer-read-by-routing-id :alice [:base :microbatch u])
              (record/answer-read-by-routing-id nil [:base :microbatch u])
              (record/answer-read-by-routing-id nil nil)
              (record/answer-read-by-routing-id 5 5)
              (record/answer-read-by-routing-id (long-id 257) [(long-id 257) :streaming u])
              (= (long-id 257) (record/routing-id [(long-id 257) :streaming u]))])
      [nil nil nil nil nil nil nil true]]
     ["the two parts of an answer's key, for a reader of the answers' keys: the text of the layer and the uuid; the text gives the layer back, a keyword, a keyword with a namespace, a UUID"
      (fn [] (mapv (fn [layer] (let [[layer-text act-uuid] (record/answer-key-parts (record/answer-key layer u))]
                                 [layer-text act-uuid (= layer (record/id-of-text layer-text))]))
                   [:alice :tool/layer (uuid 3)]))
      [[":alice" u true] [":tool/layer" u true] ["00000000-0000-0000-0000-000000000003" u true]]]
     ["a layer whose text is 256 characters comes back from its key too"
      (fn [] (let [layer (long-id 256)
                   [layer-text act-uuid] (record/answer-key-parts (record/answer-key layer u))]
               [(count layer-text) act-uuid (= layer (record/id-of-text layer-text))]))
      [256 u true]]
     ["nothing for a text that is no answer's key: a place's key of three parts, one part, a layer with no text, a uuid that is not written as a UUID prints, a uuid in capitals, no character, nothing, a number, a thing that throws; and nothing throws"
      (fn [] (mapv record/answer-key-parts
                   [":alice :note-1 :text" ":alice" (str " " u) ":alice 5-5-5-5-5" (str ":alice " (str/upper-case (str (UUID. -1 -1))))
                    "" nil 5 (throwing)]))
      (vec (repeat 9 nil))]
     ["the head of the read of a depot: the layer's depot place; nothing for what is no id"
      (fn [] [(record/depot-read :alice) (record/depot-read "alice") (record/depot-read nil)
              (record/depot-read (long-id 257)) (record/depot-read [:alice])])
      [":alice :alice :softland.store/depot" nil nil nil nil]]]))

;; --------------------------------------------------------------- the decision

(defn- permission-row
  "The row of a permission fact, as an accept writes it: by the making act
  of Alice's layer, with the stamp 0 and the time `now`."
  [permission]
  (fact-row writer (arrived-fact (first permission) :softland.store/permission permission) (uuid 90) 1 0 now))

(def ^:private depot-row
  "The row of the depot fact of Alice's layer."
  (fact-row writer (arrived-fact :alice :softland.store/depot :streaming) (uuid 90) 0 0 now))

(def ^:private bobs-see
  "Bob's permission in Alice's layer, beneath hers, that says see."
  [:bob :alice :alice alice-own :see])

(def ^:private bobs-see-and-act
  "Bob's permission in Alice's layer, beneath hers, that says see and act."
  [:bob :alice :alice alice-own :see-and-act])

(def ^:private base-on-the-streaming-depot
  "An act into the base as it arrives on the streaming depot: by the system
  under its own permission in the base, with one fact that has a value. It
  is no stray on that depot, and the declaration's check takes it."
  {:act-id [:base :streaming (uuid 1)] :depot :streaming declaration/time-part now
   :version 1 :who :system :layer :base :permission [:system :base :base] :session :s1
   :facts [(value-at :doc-1 :note)]})

(def ^:private standing-rows
  "For each: the act, the row at the place of who's permission, and the row
  at the layer's depot place; then the word of `standing`, and whether the
  act makes the layer. Each row is held at the streaming gate's depot. No
  row says whether the layer is the other gate's: `standing` works that out
  from the gate's depot and the act's layer. Two rows hand a row of the base
  with an act into the base. No gate writes such a row into the streaming
  gate's record; they hold what `standing` reads first. `base-standing-rows`
  holds the standing at the microbatch gate's depot."
  (let [alices (act-of good)
        bobs-act (assoc alices :who :bob :permission bobs)
        personal (making :alice :system :alice)
        group (making :team :alice :alice)
        into-base (act-of base-on-the-streaming-depot)
        base-making (making :base :system :system)
        systems-texts [":system" "[:system :base :base]"]
        a-depot-row-of-the-base (fact-row systems-texts (arrived-fact :base :softland.store/depot :microbatch)
                                          (uuid 90) 0 0 now)
        a-permission-row-of-the-base (fact-row systems-texts (arrived-fact :system :softland.store/permission [:system :base :base])
                                               (uuid 90) 1 0 now)]
    [["the act's permission is at its holder's place" alices (permission-row alice-own) nil nil false]
     ["an act under a permission given beneath another, at its holder's place" bobs-act (permission-row bobs) nil nil false]
     ["the place holds another permission of the same holder" (assoc alices :permission [:alice :alice :alice [:x :alice :alice]])
      (permission-row alice-own) nil :permission-does-not-stand false]
     ["the place is empty and the layer is made: the record does not hold the permission" bobs-act nil depot-row
      :permission-does-not-stand false]
     ["an act under Alice's permission by a who that is not Alice, in a layer that is made"
      (assoc alices :who :mallory) nil depot-row :who-not-the-permissions-holder false]
     ["the system acts again in a personal layer that it made" personal nil depot-row
      :who-not-the-permissions-holder false]
     ["a making act by the holder for a layer that is made: its permission stands, and it makes nothing"
      (making :alice :alice :alice) (permission-row alice-own) nil nil false]
     ["an act into a layer that is not made" alices nil nil :layer-not-made false]
     ["a right making act of a personal layer, into a layer that is not made" personal nil nil nil true]
     ["a right making act of a group's layer" group nil nil nil true]
     ["an act into the base, a layer of the other gate, of which this gate's record holds nothing" into-base nil nil
      :layer-belongs-to-the-other-gate false]
     ["the base's making act, which has the form of a making act for the other gate and not for this one: it makes nothing here, and the word is that of a layer of the other gate, not that of a layer that is not made"
      base-making nil nil :layer-belongs-to-the-other-gate false]
     ["that a layer is the other gate's counts only when both rows are nil: an act into the base with the row of its permission there stands"
      into-base a-permission-row-of-the-base nil nil false]
     ["and with a row at the base's depot place there, the layer reads as made here: the word of a permission that the record does not hold"
      into-base nil a-depot-row-of-the-base :permission-does-not-stand false]
     ["a holder of see who names her permission as the row has it: an act needs act"
      (assoc alices :who :bob :permission bobs-see) (permission-row bobs-see) nil :permission-does-not-allow-acting false]
     ["a holder of see who names her permission without the slot: the bytes are not the row's, so naming more does not stand"
      bobs-act (permission-row bobs-see) nil :permission-does-not-stand false]
     ["a holder of see who names her permission with see and act in the slot"
      (assoc alices :who :bob :permission bobs-see-and-act) (permission-row bobs-see) nil :permission-does-not-stand false]
     ["a holder of a permission that says see and act, named as the row has it"
      (assoc alices :who :bob :permission bobs-see-and-act) (permission-row bobs-see-and-act) nil nil false]
     ["a holder of a permission that was given without the slot: it reads as see and act"
      bobs-act (permission-row bobs) nil nil false]
     ["a holder of see and act who names her permission with see in the slot: it does not stand, so nobody acts under less by naming less either"
      (assoc alices :who :bob :permission bobs-see) (permission-row bobs-see-and-act) nil :permission-does-not-stand false]
     ["a holder of see whose act is by another who: the word of the holder comes first"
      (assoc alices :who :mallory :permission bobs-see) nil depot-row :who-not-the-permissions-holder false]
     ["a making act whose permission has the slot, into a layer that is not made: it is no making act"
      (-> group
          (assoc :permission [:alice :team :team [:x :team :team] :see-and-act])
          (assoc-in [:facts 1 :v] [:alice :team :team [:x :team :team] :see-and-act]))
      nil nil :layer-not-made false]]))

(def ^:private standing-cases
  "The cases of the standing at the streaming gate's depot: the two places
  that are read, an act into the base as it arrives on that depot, what a
  permission allows, and one case for each row of `standing-rows`.
  `other-gate-cases` holds a layer of the other gate for both gates."
  (concat
   [["the places that the standing reads: who's permission, then the layer's depot"
     (fn [] [(decision/permission-place (act-of good)) (decision/depot-place (act-of good))])
     [":alice :alice :softland.store/permission" ":alice :alice :softland.store/depot"]]
    ["an act into the base on the streaming depot is no stray there, and the declaration's check, the one check before the standing, takes it: it is the standing that has the word"
     (fn [] [(declaration/stray-reason :streaming base-on-the-streaming-depot)
             (verdict (declaration/check base-on-the-streaming-depot))])
     [nil :ok]]
    ["what a permission allows, read from its one form: a first permission and one given without the slot read as see and act; with the slot, what the slot says; the slot of a permission above it is not read"
     (fn [] (mapv act/allows [alice-own bobs bobs-see bobs-see-and-act [:carol :alice :alice bobs-see]]))
     [:see-and-act :see-and-act :see :see-and-act :see-and-act]]]
   (map (fn [[what act p-row c-row word makes?]]
          [(str "standing: " what)
           (fn [] [(decision/standing :streaming act p-row c-row)
                   (decision/makes-the-layer? :streaming act p-row c-row)])
           [word makes?]])
        standing-rows)))

(def ^:private early-cases
  "The cases of what the walk takes before it reads the record: the time on
  a thing, which the walk hands to `writes` for both gates, and the
  declaration's word, which the walk takes as it is. No check reads the
  time on the act or a stamp that the act carries: before the record, a
  thing is refused for its form alone."
  [["the time on a thing is the usable time that it holds: `now` for the least thing"
    (fn [] (decision/time-on good))
    now]
   ["a thing that the declaration refuses for another fault keeps its time: a wrong version, a part that the form does not know, facts that throw when they are read, a value over the bound; each with the check's word"
    (fn [] (mapv (fn [x] [(decision/time-on x) (verdict (declaration/check x))])
                 [(assoc good :version 2) (assoc good :owner 1) (assoc good :facts (throwing))
                  (arrived 1 (value-at :note-1 :text over-the-bound))]))
    [[now :unknown-version] [now :unknown-part] [now :malformed] [now :value-too-large]]]
   ["a thing with no usable time has none: no time, a nil one, a double, a time below zero; and a thing that is no map, or that throws when it is read"
    (fn [] (mapv decision/time-on [(without-time good) (at-time nil) (at-time 1.7E12) (at-time -1)
                                   nil 5 (throwing)]))
    [nil nil nil nil nil nil nil]]
   ["a time far ahead of every clock is the time on the thing as it is, and the declaration takes the thing: nothing holds the time against a clock or a bound"
    (fn [] (let [thing (at-time far-ahead)] [(decision/time-on thing) (verdict (declaration/check thing))]))
    [far-ahead :ok]]
   ["an act with no time gets the declaration's word, whatever stamp it carries: no act goes on without a time"
    (fn [] (verdict (declaration/check (without-time (assoc good :based-on {[(act-id 2) 0] Long/MAX_VALUE})))))
    :no-door-time]
   ["claimed-when far ahead is a claim: no refusal"
    (fn [] (verdict (declaration/check (assoc good :claimed-when Long/MAX_VALUE))))
    :ok]])

(defn- row-by
  "The row that the fact `index` of the act `(act-id n)` wrote: a value at
  :note-1 :text. Its stamp is 200, later than the stamp 100 since which the
  cases mark a key, so that such a row before under a marked key has an
  entry in the facts by value; and its time is `now`."
  [n index]
  (fact-row writer (arrived-fact :note-1 :text "a text before") (uuid n) index 200 now))

(defn- setting-row
  "The row that the fact 0 of the act `(act-id n)` wrote under a store key on
  Alice's layer, with the stamp and the time of `row-by`."
  [n k v]
  (fact-row writer (arrived-fact :alice k v) (uuid n) 0 200 now))

(def ^:private places-rows
  "For each: the facts of Alice's act, the rows of the places read (by
  entity and key), and the word of `places`."
  (let [replacing (fn [fact-id] (assoc (value-at :note-1 :text) :replaces fact-id))
        retract-row (fact-row writer (arrived-fact :note-1 :text) (uuid 8) 0 200 now)
        see :softland.store/who-may-see
        on :softland.store/builds-on
        marks :softland.store/marked-keys]
    [["a value that names nothing, on an empty place: never stale" [(value-at :note-1 :text)] {} nil]
     ["a value that replaces the current fact of its place" [(replacing [(act-id 7) 0])] {[:note-1 :text] (row-by 7 0)} nil]
     ["a retract that replaces the current fact" [{:e :note-1 :k :text :replaces [(act-id 7) 0]}] {[:note-1 :text] (row-by 7 0)} nil]
     ["a value that replaces a retract, which is the current fact of its place" [(replacing [(act-id 8) 0])] {[:note-1 :text] retract-row} nil]
     ["a value that replaces an earlier fact of the place" [(replacing [(act-id 6) 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces another index of the same act" [(replacing [(act-id 7) 1])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces something on an empty place" [(replacing [(act-id 7) 0])] {} :replaced-fact-not-current]
     ["a value that replaces a fact of another layer" [(replacing [[:bob :streaming (uuid 7)] 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces a fact of the other depot" [(replacing [[:alice :microbatch (uuid 7)] 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a depot fact on an entity that is not the layer" [{:e :bob :k :softland.store/depot :v :streaming}] {} :setting-not-on-the-layer]
     ["a depot retract on an entity that is not the layer" [{:e :bob :k :softland.store/depot}] {} :setting-not-on-the-layer]
     ["a permission given straight beneath the act's own, on an empty place" [{:e :bob :k :softland.store/permission :v bobs}] {} nil]
     ["a holder gives itself a second permission beneath its own: its place holds one"
      [{:e :alice :k :softland.store/permission :v [:alice :alice :alice alice-own]}] {[:alice :softland.store/permission] (permission-row alice-own)}
      :store-key-takes-one-write]
     ["a permission given for another layer" [{:e :bob :k :softland.store/permission :v [:bob :team :alice alice-own]}] {} :permission-fact-for-another-layer]
     ["a permission given with a granted-in that is not the act's layer" [{:e :bob :k :softland.store/permission :v [:bob :alice :team alice-own]}] {} :permission-fact-granted-in-another-layer]
     ["a permission given with nothing above it, in a layer that is made" [{:e :bob :k :softland.store/permission :v [:bob :alice :alice]}] {} :permission-fact-not-beneath-the-acts-own]
     ["a permission given beneath a permission that is not the act's own" [{:e :bob :k :softland.store/permission :v [:bob :alice :alice [:carol :alice :alice alice-own]]}] {}
      :permission-fact-not-beneath-the-acts-own]
     ["the same permission given a second time" [{:e :bob :k :softland.store/permission :v bobs}] {[:bob :softland.store/permission] (permission-row bobs)} :store-key-takes-one-write]
     ["a retract of a permission" [{:e :bob :k :softland.store/permission}] {[:bob :softland.store/permission] (permission-row bobs)} :ending-a-permission-not-decided]
     ["a retract of the layer's depot" [{:e :alice :k :softland.store/depot}] {} :store-key-retract-not-built]
     ["the other depot for a layer that is made" [{:e :alice :k :softland.store/depot :v :microbatch}] {} :changing-a-depot-not-decided]
     ["the layer's own depot a second time" [{:e :alice :k :softland.store/depot :v :streaming}] {} :store-key-takes-one-write]
     ["a stale fact and a depot fact on another entity: the stale fact gives the word"
      [(replacing [(act-id 6) 0]) {:e :bob :k :softland.store/depot :v :streaming}] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a depot fact on another entity before a permission for another layer: the setting gives the word"
      [{:e :bob :k :softland.store/permission :v [:bob :team :alice alice-own]} {:e :carol :k :softland.store/depot :v :streaming}] {} :setting-not-on-the-layer]
     ["who may see the layer, on an empty place" [{:e :alice :k see :v :everyone}] {} nil]
     ["what the layer builds on, on an empty place" [{:e :alice :k on :v [:base]}] {} nil]
     ["the layer's marked keys, on an empty place" [{:e :alice :k marks :v #{:on}}] {} nil]
     ["who may see the layer, written a second time on a place that holds one, naming nothing: a second write is taken"
      [{:e :alice :k see :v :members}] {[:alice see] (setting-row 7 see :everyone)} nil]
     ["what the layer builds on, written a second time, naming nothing" [{:e :alice :k on :v []}] {[:alice on] (setting-row 7 on [:base])} nil]
     ["the layer's marked keys, written a second time, naming nothing" [{:e :alice :k marks :v #{}}] {[:alice marks] (setting-row 7 marks #{:on})} nil]
     ["who may see the layer, written again naming the fact that is there" [{:e :alice :k see :v :members :replaces [(act-id 7) 0]}]
      {[:alice see] (setting-row 7 see :everyone)} nil]
     ["who may see the layer, written again naming a fact that is no longer there: a writer who must not lose a write names what she replaces"
      [{:e :alice :k see :v :members :replaces [(act-id 6) 0]}] {[:alice see] (setting-row 7 see :everyone)} :replaced-fact-not-current]
     ["what the layer builds on, written again naming a stale fact" [{:e :alice :k on :v [:base] :replaces [(act-id 6) 0]}]
      {[:alice on] (setting-row 7 on [])} :replaced-fact-not-current]
     ["the layer's marked keys, written again naming a stale fact" [{:e :alice :k marks :v #{:on} :replaces [(act-id 6) 0]}]
      {[:alice marks] (setting-row 7 marks #{})} :replaced-fact-not-current]
     ["a retract of who may see the layer" [{:e :alice :k see}] {[:alice see] (setting-row 7 see :everyone)} :store-key-retract-not-built]
     ["a retract of what the layer builds on: a list is emptied by a second write of []" [{:e :alice :k on}] {} :store-key-retract-not-built]
     ["a retract of the layer's marked keys: a mark is taken off by a second write of the set without it" [{:e :alice :k marks}] {} :store-key-retract-not-built]
     ["who may see, on an entity that is not the layer" [{:e :bob :k see :v :everyone}] {} :setting-not-on-the-layer]
     ["what a layer builds on, on an entity that is not the layer" [{:e :bob :k on :v [:base]}] {} :setting-not-on-the-layer]
     ["marked keys, on an entity that is not the layer" [{:e :bob :k marks :v #{:on}}] {} :setting-not-on-the-layer]
     ["a retract of who may see on an entity that is not the layer: the setting gives the word" [{:e :bob :k see}] {} :setting-not-on-the-layer]
     ["a second give to a holder of see, of see and act: a permission's place takes one write, so see stays see"
      [{:e :bob :k :softland.store/permission :v bobs-see-and-act}] {[:bob :softland.store/permission] (permission-row bobs-see)} :store-key-takes-one-write]
     ["a permission given with the slot, straight beneath the act's own, on an empty place" [{:e :bob :k :softland.store/permission :v bobs-see}] {} nil]
     ["a permission given with the slot beneath a permission that is not the act's own"
      [{:e :bob :k :softland.store/permission :v [:bob :alice :alice [:carol :alice :alice alice-own] :see]}] {} :permission-fact-not-beneath-the-acts-own]
     ["a tool's own fact under the bare word :class, on an entity that is not the layer: no setting" [(value-at :bob :class)] {} nil]
     ["a tool's own fact under the bare word :permission, on an entity that holds a permission: no permission fact"
      [(value-at :bob :permission)] {[:bob :softland.store/permission] (permission-row bobs)} nil]
     ["a fact copied from the log into another act, on the place where it is the current fact: a value like any other, so it is taken"
      [(value-at :note-1 :text "a text before")] {[:note-1 :text] (row-by 7 0)} nil]]))

(defn- keyed
  "Rows by [entity key], as rows by the key of the place in Alice's layer."
  [rows]
  (into {} (map (fn [[[e k] row]] [(record/place-key :alice e k) row])) rows))

(def ^:private places-cases
  "The cases of the places: one for each row of `places-rows`, the places
  whose rows are read, and a making act."
  (concat
   (map (fn [[what facts rows word]]
          [(str "places: " what)
           (fn [] (decision/places :streaming (act-of (apply arrived 1 facts)) false (keyed rows)))
           word])
        places-rows)
   [["the places whose rows are read: of each fact that replaces something and of each permission fact, no other; not of a tool's own fact under the bare word :permission"
     (fn [] (decision/places-needed
             (act-of (arrived 1 (value-at :note-1 :text)
                              (assoc (value-at :note-2 :text) :replaces [(act-id 7) 0])
                              {:e :bob :k :softland.store/permission :v bobs}
                              (value-at :carol :permission)))
             false))
     [":alice :note-2 :text" ":alice :bob :softland.store/permission"]]
    ["a fact under a key that takes a second write reads its place for the checks only when it names something to replace"
     (fn [] (decision/places-needed
             (act-of (arrived 1 {:e :alice :k :softland.store/who-may-see :v :everyone}
                              {:e :alice :k :softland.store/builds-on :v [:base] :replaces [(act-id 7) 0]}
                              {:e :alice :k :softland.store/marked-keys :v #{:on}}))
             false))
     [":alice :alice :softland.store/builds-on"]]
    ["who may write the setting, the list and the marks: whoever may act in the layer, so the check gives no word; it is kept, and not decided"
     (fn [] (let [act (act-of (arrived 1 {:e :alice :k :softland.store/who-may-see :v :everyone}))]
              [(decision/who-may-write-fault act (first (:facts act)))
               (decision/who-may-write-fault (assoc act :who :bob :permission bobs) (first (:facts act)))]))
     [nil nil]]
    ["a making act reads no place, and is not held to the check of a permission fact or to one write for a place"
     (fn [] (let [act (making :team :alice :alice)]
              [(decision/places-needed act true) (decision/places :streaming act true {})
               (decision/places :streaming act false {})]))
     [[] nil :permission-fact-not-beneath-the-acts-own]]
    ["a making act that names something to replace is stale: nothing is current in a layer that is not made"
     (fn [] (decision/places :streaming (assoc-in (making :team :alice :alice) [:facts 0 :replaces] [[:team :streaming (uuid 7)] 0]) true {}))
     :replaced-fact-not-current]]))

(def ^:private writes-cases
  "The cases of the rows that a decision writes: a refusal, an accept, a
  making act, a retract, and a write of the marked keys, with the stamp of a
  newly marked key as the walk hands it for an act of either gate; and the
  time on the act, which a decision writes beside the stamp as a label.
  `writes` makes no stamp: it writes the one that the walk gives it. The
  stamp here is 300, later than the rows before of `row-by`."
  (let [stamp 300
        digest (digest/digest good)
        value-act (act-of good)
        answer-key (record/answer-key :alice (uuid 1))
        note-place ":alice :note-1 :text"
        marks-place ":alice :alice :softland.store/marked-keys"
        marking (fn [keys] (act-of (arrived 1 {:e :alice :k :softland.store/marked-keys :v keys})))
        marks-before (record/with-level-since (setting-row 7 :softland.store/marked-keys #{:on :gone}) {:on 100 :gone 100})
        level-since-of (fn [act row-before level-from]
                         (let [[[_ _ row]] (decision/writes (act-id 1) act nil stamp now digest
                                                            {:place-rows {marks-place row-before} :level-from level-from})]
                           (record/marked-keys-of row)))
        ;; the stamp of a marked key and a row before, in hand for an accept of `value-act`
        in-hand {:since-by-place {note-place 100} :place-rows {note-place (row-by 7 0)} :level-from stamp}
        uuid-who "#uuid \"00000000-0000-0000-0000-00000000004d\""]
    [["a refusal writes the answer, with the time on the thing, and the last stamp, and nothing else; what the walk has in hand is not read: with the stamp of a marked key and a row before in hand, an accept of the same act writes the fact's row and two rows of the facts by value beside them"
      (fn [] [(decision/writes (act-id 1) value-act :replaced-fact-not-current stamp now digest {})
              (decision/writes (act-id 1) value-act :replaced-fact-not-current stamp now digest in-hand)
              (mapv first (decision/writes (act-id 1) value-act nil stamp now digest in-hand))])
      [[[:answer answer-key {:refused :replaced-fact-not-current :stamp stamp :time now :digest digest}]
        [:last-stamp nil stamp]]
       [[:answer answer-key {:refused :replaced-fact-not-current :stamp stamp :time now :digest digest}]
        [:last-stamp nil stamp]]
       [:fact :by-value :by-value :answer :last-stamp]]]
     ["a thing that the declaration refused, which is no act, writes the same two, and its answer keeps the time that the thing carried"
      (fn [] (decision/writes (act-id 1) nil :malformed stamp now digest {}))
      [[:answer answer-key {:refused :malformed :stamp stamp :time now :digest digest}]
       [:last-stamp nil stamp]]]
     ["a thing with no usable time, as the walk hands it, with what `time-on` gives and the declaration's word: the same two writes, and its answer holds no :time"
      (fn [] (let [thing (without-time good)
                   time-millis (decision/time-on thing)
                   word (:refuse (declaration/check thing))]
               [time-millis word (decision/writes (act-id 1) nil word stamp time-millis digest {})]))
      [nil :no-door-time
       [[:answer answer-key {:refused :no-door-time :stamp stamp :digest digest}]
        [:last-stamp nil stamp]]]]
     ["an accept of one value writes three rows: the fact, with the one text of its value, the time on the act, who wrote it and the permission that the act named; the answer, with the time on the act; the last stamp"
      (fn [] (decision/writes (act-id 1) value-act nil stamp now digest {}))
      [[:fact note-place (merge {:value "\"a text\"" :by-act (uuid 1) :by-index 0 :stamp stamp :time now} written-by-alice)]
       [:answer answer-key {:stamp stamp :time now :digest digest}]
       [:last-stamp nil stamp]]]
     ["the time is a label beside the stamp in each row that an accept writes: the fact's row, the entry of its value in the facts by value, and the answer each hold the stamp given and the time on the act, as [stamp, time]; the entry of the value before is taken out and holds nothing"
      (fn [] (mapv (fn [[which _ row]] [which (if (map? row) [(:stamp row) (:time row)] row)])
                   (decision/writes (act-id 1) value-act nil stamp now digest in-hand)))
      [[:fact [stamp now]] [:by-value nil] [:by-value [stamp now]] [:answer [stamp now]] [:last-stamp stamp]]]
     ["a time far ahead of every clock is written as it is and changes nothing else: an accept, and a refusal, with the time 2^50 ms give the rows that they give with the time `now`, but for :time; the stamp is the one given"
      (fn [] (let [without-the-time (fn [writes] (mapv (fn [[which k row]] [which k (if (map? row) (dissoc row :time) row)]) writes))
                   accept-at (fn [t] (decision/writes (act-id 1) value-act nil stamp t digest in-hand))
                   refusal-at (fn [t] (decision/writes (act-id 1) value-act :layer-not-made stamp t digest {}))]
               [(= (comparable (without-the-time (accept-at far-ahead))) (comparable (without-the-time (accept-at now))))
                (= (comparable (without-the-time (refusal-at far-ahead))) (comparable (without-the-time (refusal-at now))))
                (vec (keep (fn [[_ _ row]] (when (map? row) (:time row))) (accept-at far-ahead)))
                (mapv (fn [[which _ row]] (if (map? row) (:stamp row) row)) (refusal-at far-ahead))]))
      [true true [far-ahead far-ahead far-ahead] [stamp stamp]]]
     ["the fact's row holds the text of the value in its normal form, whatever form its maker gave: its String as written out here, the value that it reads back as, its plain bytes, and the seven fields of the row"
      (fn [] (let [act (act-of (arrived 1 (value-at :note-1 :text (array-map :b 1.50M :a [(int 1) 2N]))))
                   [[_ _ row]] (decision/writes (act-id 1) act nil stamp now digest {})]
               [(:value row) (record/read-value row) (record/plain-bytes-of row) (sort (keys row))]))
      ["{:a [1 2] :b 1.5M}" [:value {:a [1 2] :b 1.5M}] (utf8 "{:a [1 2] :b 1.5M}")
       [:by-act :by-index :permission :stamp :time :value :who]]]
     ["the texts of who and of the permission are made once for the act and each row holds them: an act by a who that is a UUID, with a value and a retract"
      (fn [] (let [act (act-of (assoc (arrived 1 (value-at :note-1 :text) {:e :note-2 :k :text})
                                      :who (uuid 77) :permission [(uuid 77) :alice :alice alice-own :see-and-act]))]
               (mapv (fn [[_ _ row]] (select-keys row [:who :permission]))
                     (take 2 (decision/writes (act-id 1) act nil stamp now digest {})))))
      (vec (repeat 2 {:who uuid-who :permission (str "[" uuid-who " :alice :alice [:alice :alice :alice] :see-and-act]")}))]
     ["an accept of a making act writes the depot, the first permission, the answer, the last stamp; each row holds who made the layer, the permission that the act named and the time on the act"
      (fn [] (decision/writes [:team :streaming (uuid 1)] (making :team :alice :alice) nil stamp now digest {}))
      [[:fact ":team :team :softland.store/depot"
        {:value ":streaming" :by-act (uuid 1) :by-index 0 :stamp stamp :time now :who ":alice" :permission "[:alice :team :team]"}]
       [:fact ":team :alice :softland.store/permission"
        {:value "[:alice :team :team]" :by-act (uuid 1) :by-index 1 :stamp stamp :time now :who ":alice" :permission "[:alice :team :team]"}]
       [:answer (record/answer-key :team (uuid 1)) {:stamp stamp :time now :digest digest}]
       [:last-stamp nil stamp]]]
     ["the system makes a personal layer for its person: the rows hold the system as who, and the person's permission"
      (fn [] (mapv (fn [[_ _ row]] (select-keys row [:who :permission]))
                   (take 2 (decision/writes [:alice :streaming (uuid 1)] (making :alice :system :alice) nil stamp now digest {}))))
      [{:who ":system" :permission "[:alice :alice :alice]"} {:who ":system" :permission "[:alice :alice :alice]"}]]
     ["an accept of a retract and a permission given writes a row for each: the retract's has no value; each holds who, the permission and the time"
      (fn [] (mapv (fn [[which key row]] [which key (if (map? row) (dissoc row :by-act :stamp) row)])
                   (decision/writes (act-id 1)
                                    (act-of (arrived 1 {:e :note-1 :k :text} {:e :bob :k :softland.store/permission :v bobs}))
                                    nil stamp now digest {})))
      [[:fact note-place (merge {:by-index 0 :time now} written-by-alice)]
       [:fact ":alice :bob :softland.store/permission"
        (merge {:value "[:bob :alice :alice [:alice :alice :alice]]" :by-index 1 :time now} written-by-alice)]
       [:answer answer-key {:time now :digest digest}]
       [:last-stamp nil stamp]]]
     ["a give with the slot writes the permission's whole text, slot and all: what the gate later compares with the permission that an act names"
      (fn [] (let [[[_ k row]] (decision/writes (act-id 1) (act-of (arrived 1 {:e :bob :k :softland.store/permission :v bobs-see}))
                                                nil stamp now digest {})]
               [k (:value row) (record/holds-value? row bobs-see) (record/holds-value? row bobs)]))
      [":alice :bob :softland.store/permission" "[:bob :alice :alice [:alice :alice :alice] :see]" true false]]
     ["a give of a permission 32 deep writes its whole text, as Clojure prints the permission, with its 32 vectors; the row holds that permission and not the one above it"
      (fn [] (let [given (reduce (fn [parent _] [:bob :alice :alice parent]) alice-own (range 31))
                   [[_ _ row]] (decision/writes (act-id 1) (act-of (arrived 1 {:e :bob :k :softland.store/permission :v given}))
                                                nil stamp now digest {})]
               [(= (:value row) (pr-str given)) (count (re-seq #"\[" (:value row)))
                (record/holds-value? row given) (record/holds-value? row (nth given 3))]))
      [true 32 true false]]
     ["a write of the marked keys: the row holds the set that the maker wrote and, derived at the write, the stamp of each key; a newly marked key gets the number under :level-from, here 500, which is not the stamp of the act, so that the two are told apart"
      (fn [] (let [[[which k row] & more] (decision/writes (act-id 1) (marking #{:on :tool/parent}) nil stamp now digest
                                                           {:place-rows {marks-place nil} :level-from 500})]
               [which k (:value row) (record/read-value row) (record/marked-keys-of row) (mapv first more)]))
      [:fact marks-place "#{:on :tool/parent}" [:value #{:on :tool/parent}] {:on 500 :tool/parent 500} [:answer :last-stamp]]]
     ["what the walk hands under :level-from is the act's own stamp, for an act of either gate: a write of the marks in a streaming layer, and one in the base under a microbatch act-id, each give the newly marked key the stamp that the row of the marks itself holds"
      (fn [] (mapv (fn [thing]
                     (let [act (act-of thing)
                           place (decision/marked-keys-place act)
                           [[_ k row]] (decision/writes (:act-id thing) act nil stamp (decision/time-on thing) (digest/digest thing)
                                                        {:place-rows {place nil} :level-from stamp})]
                       [k (:stamp row) (record/marked-keys-of row)]))
                   [(arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on}})
                    (assoc base-on-the-streaming-depot :act-id [:base :microbatch (uuid 1)] :depot :microbatch
                           :facts [{:e :base :k :softland.store/marked-keys :v #{:on}}])]))
      [[marks-place stamp {:on stamp}] [":base :base :softland.store/marked-keys" stamp {:on stamp}]]]
     ["a second write of the marked keys: a key that stays marked keeps its stamp, a key that is newly marked gets the new one, a key whose mark is taken off is gone; the empty set marks nothing"
      (fn [] [(level-since-of (marking #{:on :new}) marks-before 500)
              (level-since-of (marking #{:gone}) marks-before 500)
              (level-since-of (marking #{}) marks-before 500)
              (level-since-of (marking #{:on}) (record/with-level-since (setting-row 8 :softland.store/marked-keys #{}) {}) 700)])
      [{:on 100 :new 500} {:gone 100} {} {:on 700}]]
     ["a setting and a list are written as any fact under a store key: the one text of the value, and no stamps of marked keys"
      (fn [] (mapv (fn [[_ k row]] [k (:value row) (record/read-value row) (contains? row :level-since)])
                   (take 2 (decision/writes (act-id 1)
                                            (act-of (arrived 1 {:e :alice :k :softland.store/who-may-see :v :everyone}
                                                             {:e :alice :k :softland.store/builds-on :v [:base :team]}))
                                            nil stamp now digest {}))))
      [[":alice :alice :softland.store/who-may-see" ":everyone" [:value :everyone] false]
       [":alice :alice :softland.store/builds-on" "[:base :team]" [:value [:base :team]] false]]]]))

;; --------------------------------------- the facts by value, one act at a time

(defn- streaming-step
  "Decides one accepted act of Alice's in her streaming layer against a
  record in memory, as steps 6 and 7 of the walk of one thing do: the row at
  the layer's marked-keys place when the act needs it, the stamp of each of
  its places under a marked key, the row before of each such place, then
  `softland.store.gate.decision/writes` with the act's stamp as the stamp of
  a newly marked key. Takes the record, {:facts :by-value}, each a map from
  key to row; the number of the act's act-id; its stamp; and its facts as a
  maker gives them. The act goes through the declaration's check, which
  gives each value its text, and its time, `now`, goes to `writes` as the
  walk hands it. Gives the record after, with :writes, the rows
  of the decision but the answer and the last stamp."
  [record n stamp & facts]
  (let [thing (apply arrived n facts)
        act (act-of thing)
        marks-key (decision/marked-keys-place act)
        marks-row (when (decision/reads-marked-keys? act) (get-in record [:facts marks-key]))
        since (decision/since-by-place act marks-row)
        rows-before (into {marks-key marks-row}
                          (map (fn [k] [k (get-in record [:facts k])]))
                          (decision/rows-before-needed since {}))
        writes (remove (fn [[which]] (contains? #{:answer :last-stamp} which))
                       (decision/writes (act-id n) act nil stamp (decision/time-on thing) (digest/digest thing)
                                        {:since-by-place since :place-rows rows-before :level-from stamp}))]
    (assoc (reduce (fn [record [which k row]]
                     (case which
                       :fact (assoc-in record [:facts k] row)
                       :by-value (if row (assoc-in record [:by-value k] row) (update record :by-value dissoc k))
                       record))
                   record
                   writes)
           :writes (vec writes))))

(defn- on-fact
  "A fact of Alice's layer under the key :on, with a value."
  [e v]
  (value-at e :on v))

(defn- entry-key
  "The key in the facts by value of the place `e` :on of Alice's layer, for a
  value, under a stamp of the key."
  [since v e]
  (record/by-value-key :alice :on since (record/value-digest-of v) e))

(def ^:private by-value-cases
  "The cases of the order by value in the pure code: which rows a decision
  puts and takes out for a fact under a marked key, over a mark that is put,
  taken off and put again, and over each way in which a fact changes."
  (let [marks (fn [keys] {:e :alice :k :softland.store/marked-keys :v keys})
        empty-record {:facts {} :by-value {}}
        ;; a mark, a fact, the mark taken off, the fact replaced, the mark again
        s1 (streaming-step empty-record 101 100 (marks #{:on}))
        s2 (streaming-step s1 102 110 (on-fact :b1 :c1))
        s3 (streaming-step s2 103 120 (marks #{}))
        s4 (streaming-step s3 104 130 (on-fact :b1 :c2))
        s5 (streaming-step s4 105 140 (marks #{:on}))
        s6 (streaming-step s5 106 150 (on-fact :b1 :c3))
        s7 (streaming-step s6 107 160 (on-fact :b1 :c1))
        keys-of (fn [record] (set (keys (:by-value record))))
        stamps-of (fn [record] (record/marked-keys-of (get-in record [:facts ":alice :alice :softland.store/marked-keys"])))
        run-of (fn [record since v] (let [[from to] (record/by-value-run :alice :on since v)]
                                      (vec (sort (filter #(and (<= (compare from %) 0) (neg? (compare % to))) (keys (:by-value record)))))))
        ;; [which key, and whether a row is written: false for a key that is taken out]
        which-keys (fn [record] (mapv (fn [[which k row]] [which k (some? row)]) (:writes record)))
        ;; one marking, since 100: the ways in which a fact changes
        m (streaming-step empty-record 201 100 (marks #{:on}))
        first-value (streaming-step m 202 110 (on-fact :b1 :c1))
        long-text (text-of-bytes 65536)]
    [["a fact under a key that is not marked puts no row in the order by value; a mark puts none either: nothing is filled from before"
      (fn [] (let [before (streaming-step empty-record 100 90 (on-fact :b1 :c0))
                   marked (streaming-step before 101 100 (marks #{:on}))]
               [(which-keys before) (keys-of before) (keys-of marked) (stamps-of marked)]))
      [[[:fact ":alice :b1 :on" true]] #{} #{} {:on 100}]]
     ["a mark, then a fact under the marked key: one row comes in, under the key's stamp and the digest of the value, which is the proof's own SHA-256 of the three characters :c1; the fact's row holds that text"
      (fn [] [(stamps-of s1) (which-keys s2) (keys-of s2)
              (dissoc (get (:by-value s2) (entry-key 100 :c1 :b1)) :by-act)
              (:value (get-in s2 [:facts ":alice :b1 :on"]))])
      [{:on 100}
       [[:fact ":alice :b1 :on" true] [:by-value (entry-key 100 :c1 :b1) true]]
       #{(str ":alice :on 100 " (digest-of-text ":c1") " :b1")}
       (merge {:by-index 0 :stamp 110 :time now} written-by-alice)
       ":c1"]]
     ["the mark is taken off: no key is marked, and the row stays; a fact that is then replaced writes nothing in that order, so the row that stays is of a fact that is no longer current"
      (fn [] [(stamps-of s3) (keys-of s3) (which-keys s4) (keys-of s4) (record/read-value (get-in s4 [:facts ":alice :b1 :on"]))])
      [{} #{(entry-key 100 :c1 :b1)} [[:fact ":alice :b1 :on" true]] #{(entry-key 100 :c1 :b1)} [:value :c2]]]
     ["the key is marked again: it gets a new stamp, so it names other keys; the run of the old value under the new stamp is empty, and the row of the earlier marking is in no run that a read can name"
      (fn [] [(stamps-of s5) (run-of s5 140 :c1) (run-of s5 140 :c2) (run-of s5 100 :c1)])
      [{:on 140} [] [] [(entry-key 100 :c1 :b1)]]]
     ["a fact written after the second mark, over a fact from before it: the row before is not later than the key's stamp, so nothing is taken out; its row comes in under the new stamp"
      (fn [] [(which-keys s6) (run-of s6 140 :c3) (run-of s6 140 :c2)])
      [[[:fact ":alice :b1 :on" true] [:by-value (entry-key 140 :c3 :b1) true]]
       [(entry-key 140 :c3 :b1)] []]]
     ["a fact written over a fact of the same marking: the entry of the value before goes out and the new one comes in; a run under the current stamp holds a place exactly when its current fact has that value"
      (fn [] [(which-keys s7) (run-of s7 140 :c3) (run-of s7 140 :c1) (keys-of s7)])
      [[[:fact ":alice :b1 :on" true] [:by-value (entry-key 140 :c3 :b1) false] [:by-value (entry-key 140 :c1 :b1) true]]
       [] [(entry-key 140 :c1 :b1)]
       #{(entry-key 100 :c1 :b1) (entry-key 140 :c1 :b1)}]]
     ["a mark and a fact under its key in one act: the act is decided with the marks as they were before it, so the fact is in no run; the key gets the act's stamp, and the fact's stamp is not later"
      (fn [] (let [both (streaming-step empty-record 301 100 (marks #{:on}) (on-fact :b1 :c1))
                   after (streaming-step both 302 110 (on-fact :b2 :c1))]
               [(keys-of both) (stamps-of both) (:stamp (get-in both [:facts ":alice :b1 :on"])) (run-of after 100 :c1)]))
      [#{} {:on 100} 100 [(entry-key 100 :c1 :b2)]]]
     ["a fact that is replaced, naming what it replaces or naming nothing: the entry of the value before goes out, the new one comes in, and the rows written are the same either way"
      (fn [] (let [named (streaming-step first-value 203 120 (assoc (on-fact :b1 :c2) :replaces [(act-id 202) 0]))
                   unnamed (streaming-step first-value 203 120 (on-fact :b1 :c2))]
               [(which-keys named) (= (:writes named) (:writes unnamed)) (keys-of named)]))
      [[[:fact ":alice :b1 :on" true] [:by-value (entry-key 100 :c1 :b1) false] [:by-value (entry-key 100 :c2 :b1) true]]
       true
       #{(entry-key 100 :c2 :b1)}]]
     ["a fact written again with the same value: one digest, so nothing goes out; the entry is written over, with the new stamp and the new fact"
      (fn [] (let [again (streaming-step first-value 203 120 (on-fact :b1 :c1))]
               [(which-keys again) (keys-of again)
                (select-keys (get (:by-value again) (entry-key 100 :c1 :b1)) [:by-act :stamp])]))
      [[[:fact ":alice :b1 :on" true] [:by-value (entry-key 100 :c1 :b1) true]]
       #{(entry-key 100 :c1 :b1)}
       {:by-act (uuid 203) :stamp 120}]]
     ["a retract: the entry of the value before goes out and nothing comes in; a fact after the retract takes nothing out"
      (fn [] (let [retracted (streaming-step first-value 203 120 {:e :b1 :k :on})
                   after (streaming-step retracted 204 130 (on-fact :b1 :c9))]
               [(which-keys retracted) (keys-of retracted) (which-keys after) (keys-of after)]))
      [[[:fact ":alice :b1 :on" true] [:by-value (entry-key 100 :c1 :b1) false]]
       #{}
       [[:fact ":alice :b1 :on" true] [:by-value (entry-key 100 :c9 :b1) true]]
       #{(entry-key 100 :c9 :b1)}]]
     ["a retract on a place that holds no fact, under a marked key: nothing in the order"
      (fn [] (let [retracted (streaming-step m 203 120 {:e :b7 :k :on})]
               [(which-keys retracted) (keys-of retracted)]))
      [[[:fact ":alice :b7 :on" true]] #{}]]
     ["each kind of value is kept, by its digest: a keyword, a UUID, a string with spaces, a number, a map, a value whose text is 65,536 bytes; each key holds 64 hex characters, and each value is alone in its run"
      (fn [] (let [values [:canvas-1 (uuid 9) "a string with spaces" 42 {:a 1} long-text]
                   record (reduce (fn [record [i v]] (streaming-step record (+ 210 i) (+ 110 i) (on-fact (keyword (str "b" i)) v)))
                                  m
                                  (map-indexed vector values))]
               [(count (:by-value record))
                (every? #(re-matches #":alice :on 100 [0-9a-f]{64} :b\d" %) (keys (:by-value record)))
                (mapv (fn [[i v]] (= (run-of record 100 v) [(entry-key 100 v (keyword (str "b" i)))])) (map-indexed vector values))]))
      [6 true [true true true true true true]]]
     ["a value that is given in two forms has one entry: a map in another order and with a narrow integer is in the run of the map"
      (fn [] (let [record (streaming-step m 215 110 (on-fact :b1 (array-map :b (int 2) :a 1)))]
               [(keys-of record) (:value (get-in record [:facts ":alice :b1 :on"]))]))
      [#{(str ":alice :on 100 " (digest-of-text "{:a 1 :b 2}") " :b1")} "{:a 1 :b 2}"]]
     ["two places that hold one value under one marked key are neighbours in one run; a place that holds another value is outside it"
      (fn [] (let [record (-> m
                              (streaming-step 221 110 (on-fact :b1 :canvas-1) (on-fact :b2 :canvas-1) (on-fact :b3 :canvas-2))
                              (streaming-step 222 120 (on-fact (uuid 4) :canvas-1)))]
               [(run-of record 100 :canvas-1) (run-of record 100 :canvas-2) (run-of record 100 :canvas-3)]))
      [[(entry-key 100 :canvas-1 (uuid 4)) (entry-key 100 :canvas-1 :b1) (entry-key 100 :canvas-1 :b2)]
       [(entry-key 100 :canvas-2 :b3)] []]]
     ["a fact under a key that the layer does not mark, beside a marked one, and a fact under a store key: no row in the order"
      (fn [] (let [record (streaming-step m 231 110 (value-at :b1 :text :c1) {:e :bob :k :softland.store/permission :v bobs})]
               [(which-keys record) (keys-of record)]))
      [[[:fact ":alice :b1 :text" true] [:fact ":alice :bob :softland.store/permission" true]] #{}]]
     ["which acts read the layer's marked keys: one with a fact under a plain key, or under the marked keys; not a give, a setting or a list; and the rows before that are still to be read"
      (fn [] [(mapv (fn [facts] (decision/reads-marked-keys? (act-of (apply arrived 1 facts))))
                    [[(on-fact :b1 :c1)] [{:e :b1 :k :on}] [{:e :alice :k :softland.store/marked-keys :v #{:on}}]
                     [{:e :bob :k :softland.store/permission :v bobs}]
                     [{:e :alice :k :softland.store/who-may-see :v :everyone}] [{:e :alice :k :softland.store/builds-on :v []}]])
              (decision/reads-marked-keys? nil)
              (decision/marked-keys-place (act-of good))
              (let [act (act-of (arrived 1 (on-fact :b1 :c1) (assoc (on-fact :b2 :c1) :replaces [(act-id 7) 0]) (value-at :b3 :text)))
                    since (decision/since-by-place act (get-in m [:facts ":alice :alice :softland.store/marked-keys"]))]
                [since
                 (decision/rows-before-needed since {})
                 (decision/rows-before-needed since {":alice :b2 :on" nil})
                 (decision/since-by-place act nil)])])
      [[true true true false false false]
       false
       ":alice :alice :softland.store/marked-keys"
       [{":alice :b1 :on" 100 ":alice :b2 :on" 100}
        [":alice :b1 :on" ":alice :b2 :on"]
        [":alice :b1 :on"]
        {}]]]]))

;; ------------------------------------------------------------------- the base

(def ^:private system-own "The system's permission in the base." [:system :base :base])
(def ^:private carols "Carol's permission in the base, beneath the system's." [:carol :base :base system-own])
(def ^:private daves-in-base "Dave's permission in the base, beneath the system's." [:dave :base :base system-own])
(def ^:private mallorys "A permission in the base that no act gives." [:mallory :base :base system-own])

(defn- base-id
  "A microbatch act-id in the base, with a fixed UUID."
  [n]
  [:base :microbatch (uuid n)])

(defn- base-thing
  "An act into the base as it arrives on the microbatch depot, under the
  act-id `(base-id n)`, by `who` under `permission`, with the time `now` on
  it and these facts."
  [n who permission & facts]
  {:act-id (base-id n) :depot :microbatch declaration/time-part now
   :version 1 :who who :layer :base :permission permission :session :s1
   :facts (vec facts)})

(def ^:private base-writer
  "The two texts that each row of a fact written by the system under its own
  permission in the base holds: of who and of the permission."
  [":system" "[:system :base :base]"])

(def ^:private before-the-line
  "The last stamp of the base in each record that a line of the proof starts
  from, 30: thirty-one decisions of the base came before the line, from 0.
  It is also the stamp of each row that such a record holds but the rows of
  facts from before a mark of their key (`marks-line`)."
  30)

(def ^:private time-before-the-line
  "The time on each act whose rows a record before a line holds: five
  seconds before `now`."
  (- now 5000))

(def ^:private base-depot-row
  "The row of the depot fact of the base."
  (fact-row base-writer (arrived-fact :base :softland.store/depot :microbatch) (uuid 800) 0 before-the-line time-before-the-line))

(defn- base-permission-row
  "The row of a permission fact of the base, as an accept writes it."
  [permission]
  (fact-row base-writer (arrived-fact (first permission) :softland.store/permission permission) (uuid 800) 1 before-the-line time-before-the-line))

(def ^:private carols-see
  "Carol's permission in the base, beneath the system's, that says see."
  [:carol :base :base system-own :see])

(def ^:private base-marks-place
  "The key of the base's marked-keys place."
  ":base :base :softland.store/marked-keys")

(defn- base-marks-row
  "The row at the base's marked-keys place: these keys, each with its stamp."
  [since-by-key]
  (record/with-level-since
   (fact-row base-writer (arrived-fact :base :softland.store/marked-keys (set (keys since-by-key))) (uuid 800) 2
             before-the-line time-before-the-line)
   since-by-key))

(def ^:private base-made
  "The rows of a base that is made and holds nothing else, each as
  [table key row]: the depot fact and the system's permission."
  [[:facts ":base :base :softland.store/depot" base-depot-row]
   [:facts ":base :system :softland.store/permission" (base-permission-row system-own)]])

(def ^:private base-standing-rows
  "For each: an act into the base as it arrives on the microbatch depot, the
  row at the place of its who's permission, and the row at the base's depot
  place; then the word of `standing` at the microbatch gate's depot, and
  whether the act makes the layer. An act is decided against the record as
  the acts before it left it, so an empty place of a permission in a base
  that is made is a permission that the record does not hold."
  (let [act (act-of (base-thing 1 :carol carols {:e :doc-1 :k :note}))
        seeing (act-of (base-thing 1 :carol carols-see {:e :doc-1 :k :note}))
        by-another (act-of (base-thing 1 :mallory carols {:e :doc-1 :k :note}))
        base-making (act-of (base-thing 1 :system system-own
                                        {:e :base :k :softland.store/depot :v :microbatch}
                                        {:e :system :k :softland.store/permission :v system-own}))]
    [["the row holds the act's permission" act (base-permission-row carols) nil nil false]
     ["the row holds the act's permission, and it says see: an act needs act" seeing (base-permission-row carols-see) nil
      :permission-does-not-allow-acting false]
     ["the row holds a permission that says see, and the act names it without the slot" act (base-permission-row carols-see) nil
      :permission-does-not-stand false]
     ["the row holds another permission" act (base-permission-row [:carol :base :base carols]) nil
      :permission-does-not-stand false]
     ["the place is empty in a base that is made: the record does not hold the permission" act nil base-depot-row
      :permission-does-not-stand false]
     ["the place is empty, and the holder is not the who" by-another nil base-depot-row
      :who-not-the-permissions-holder false]
     ["the record holds nothing of the base, and the act is no making act: the base is this gate's layer, so the word is that of a layer that is not made"
      act nil nil :layer-not-made false]
     ["the base's making act, with nothing in the record: it makes the layer" base-making nil nil nil true]
     ["the base's making act for a base that is made: its permission stands, and it makes nothing"
      base-making (base-permission-row system-own) nil nil false]]))

(def ^:private base-standing-cases
  "The cases of the standing at the microbatch gate's depot: one for each
  row of `base-standing-rows`."
  (map (fn [[what act p-row c-row word makes?]]
         [(str "standing at the microbatch gate's depot: " what)
          (fn [] [(decision/standing :microbatch act p-row c-row)
                  (decision/makes-the-layer? :microbatch act p-row c-row)])
          [word makes?]])
       base-standing-rows))

;; --------------------------------------------- a layer of the other gate

(defn- as-sent
  "Takes an act in its one form on arrival, as `making` gives it, and a
  depot word. Gives the same act as it arrives on that depot from its
  maker: the depot word in the depot slot of its act-id and under `:depot`,
  each fact as its maker gives it, and without the parts that the form
  fills in. Its facts are as they were, so a making act still names the
  depot that its layer starts with."
  [form depot]
  (-> form
      (dissoc :based-on :because-of :claimed-when :subjects)
      (assoc :act-id (assoc (:act-id form) 1 depot) :depot depot)
      (update :facts (fn [facts] (mapv #(select-keys % [:e :k :v]) facts)))))

(def ^:private other-gate-rows
  "Things as they arrive on a gate's depot. For each: the gate's depot; the
  thing; whether its layer starts with the other gate's depot; whether the
  act has the form of a making act for this gate, and for the other gate;
  then the word of `standing` with no row of the layer, and whether the act
  makes the layer. The first six are for a layer of the other gate. The
  last four are the same kinds of act on their own gate's depot."
  (let [base-making (making :base :system :system)
        personal (making :alice :system :alice)
        group (making :team :alice :alice)
        of-a-uuid (making (uuid 7) :system (uuid 8))
        word :layer-belongs-to-the-other-gate]
    [["an act into the base, on the streaming depot" :streaming base-on-the-streaming-depot true false false word false]
     ["the base's making act, on the streaming depot" :streaming (as-sent base-making :streaming) true false true word false]
     ["an act into a personal layer, on the microbatch depot"
      :microbatch (assoc good :act-id [:alice :microbatch (uuid 1)] :depot :microbatch) true false false word false]
     ["the making act of a personal layer, on the microbatch depot" :microbatch (as-sent personal :microbatch) true false true word false]
     ["the making act of a group's layer, on the microbatch depot" :microbatch (as-sent group :microbatch) true false true word false]
     ["the making act of a layer whose id is a UUID, on the microbatch depot" :microbatch (as-sent of-a-uuid :microbatch) true false true word false]
     ["an act into a personal layer that is not made, on the streaming depot, its own" :streaming good false false false :layer-not-made false]
     ["the making act of a group's layer, on the streaming depot, its own: it is taken" :streaming (as-sent group :streaming) false true false nil true]
     ["an act into a base that is not made, on the microbatch depot, its own"
      :microbatch (base-thing 1 :system system-own {:e :doc-1 :k :note}) false false false :layer-not-made false]
     ["the base's making act, on the microbatch depot, its own: it is taken" :microbatch (as-sent base-making :microbatch) false true false nil true]]))

(def ^:private other-gate-cases
  "The cases of a layer of the other gate, for both gates: one for each row
  of `other-gate-rows`. Each thing is no stray on its depot and the
  declaration's check takes it, so the standing has the word. `standing` and `makes-the-layer?` are handed nil
  for both rows: the word comes from the gate's depot and the act alone.
  They take no row of the other gate's record, so whether the other gate
  has made the layer cannot change it."
  (map (fn [[what gate thing other? form-here? form-there? word makes?]]
         [(str "a layer of the other gate, from the act alone: " what)
          (fn [] (let [act (act-of thing)
                       other-gate (if (= :streaming gate) :microbatch :streaming)]
                   [(declaration/stray-reason gate thing)
                    (verdict (declaration/check thing))
                    (first-act/starts-with-the-other-gates-depot? gate (:layer act))
                    (first-act/making-act? gate act)
                    (first-act/making-act? other-gate act)
                    (decision/standing gate act nil nil)
                    (decision/makes-the-layer? gate act nil nil)]))
          [nil :ok other? form-here? form-there? word makes?]])
       other-gate-rows))

;; ------------------------------------------ the walk of one thing, in memory

(def ^:private empty-task
  "What one gate holds on one task, in memory, with nothing in it: the
  answers, the facts and the facts by value, each a map from key to row;
  and the two parts of what a gate keeps for a task: the last stamps, a map
  from the text of a layer id to a number, and the strays, a map from a
  reason word to a set of digests as hex text."
  {:answers {} :facts {} :by-value {} :last-stamp {} :strays {}})

(defn- task-with
  "Takes rows, each [table key row], and the last stamps, a map from the text
  of a layer id to a number. Gives a record in the form of `empty-task`
  that holds them."
  [rows last-stamps]
  (reduce (fn [record [table k row]] (assoc-in record [table k] row))
          (assoc empty-task :last-stamp last-stamps)
          rows))

(defn- to-copy
  "The proof's own filter of what a gate may copy to other tasks. Takes the
  writes of one decision, as `softland.store.gate.decision/writes` gives
  them. Gives each [:fact key row] and each [:by-value key row], in their
  order, and no other write. `softland.store.module.walk/copies-of` says
  the same; `copy-cases` holds that function, as its file writes it,
  against this one."
  [writes]
  (filterv (fn [[which]] (contains? #{:fact :by-value} which)) writes))

(defn- walked
  "One thing through the steps of the walk of one thing
  (`softland.store.module.walk/decide`), in memory: the reads, the pure
  functions and the writes of that walk, in its order, with a map in the
  place of each table and no Rama. Takes a record in the form of
  `empty-task`, the rows of one gate on the task of the call; the gate's
  depot word; and a thing as it arrived, which can be anything. Gives the
  record after, with `:wrote`: the writes that
  `softland.store.gate.decision/writes` gave for the thing, or an empty
  vector for a stray and for a thing under an act-id that has an answer.

  The steps. A stray's digest goes to the set of its word. A thing under an
  act-id that has an answer changes nothing. Else: the declaration's check,
  whose word the walk takes as it is, and the time on the thing; the row of
  who's permission, and the row of the layer's depot only when the first is
  nil; the rows that `places-needed` names; for an act that no check
  refused and that reads the marked keys, the marks row and the rows before
  that are still to be read; the layer's last stamp, the stamp
  (`softland.store.gate.stamp/next-stamp` of that last stamp alone), and
  the writes, with the time on the thing and with the act's own stamp as
  the stamp of a newly marked key. The last stamp is kept under the text
  of the thing's layer.

  It is the proof's own function. What it shows is what the pure functions
  give when they are called in this order, each against the rows that the
  calls before it wrote. That the walk on a cluster calls them so is the
  module's proof's."
  [record gate thing]
  (if-let [stray (declaration/stray-reason gate thing)]
    (-> record
        (update-in [:strays stray] (fnil conj #{}) (digest/hex-of thing))
        (assoc :wrote []))
    (let [[layer _ act-uuid :as act-id] (:act-id thing)]
      (if (get-in record [:answers (record/answer-key layer act-uuid)])
        (assoc record :wrote [])
        (let [facts (:facts record)
              rows-at (fn [ks] (into {} (map (fn [k] [k (get facts k)])) ks))
              {act :ok reason :refuse} (declaration/check thing)
              time-millis (decision/time-on thing)
              early reason
              permission-row (when-not early (get facts (decision/permission-place act)))
              depot-row (when (and (not early) (nil? permission-row)) (get facts (decision/depot-place act)))
              no-standing (or early (decision/standing gate act permission-row depot-row))
              makes? (and (not early) (decision/makes-the-layer? gate act permission-row depot-row))
              place-rows (if no-standing {} (rows-at (decision/places-needed act makes?)))
              refused (or no-standing (decision/places gate act makes? place-rows))
              reads-marks? (and (nil? refused) (decision/reads-marked-keys? act))
              marks-key (when reads-marks? (decision/marked-keys-place act))
              marks-row (when reads-marks?
                          (if (contains? place-rows marks-key) (get place-rows marks-key) (get facts marks-key)))
              since (if reads-marks? (decision/since-by-place act marks-row) {})
              rows-in-hand (if reads-marks?
                             (assoc (merge place-rows (rows-at (decision/rows-before-needed since place-rows)))
                                    marks-key marks-row)
                             place-rows)
              layer-text (declaration/id-text layer)
              stamp (stamp/next-stamp (get-in record [:last-stamp layer-text]))
              writes (decision/writes act-id act refused stamp time-millis (digest/digest thing)
                                      (if refused {} {:since-by-place since :place-rows rows-in-hand :level-from stamp}))]
          (assoc (reduce (fn [record [which k row]]
                           (case which
                             :fact (assoc-in record [:facts k] row)
                             :by-value (if row (assoc-in record [:by-value k] row) (update record :by-value dissoc k))
                             :answer (assoc-in record [:answers k] row)
                             :last-stamp (assoc-in record [:last-stamp layer-text] row)
                             record))
                         record
                         writes)
                 :wrote writes))))))

(defn- walked-line
  "Takes a record in the form of `empty-task`, a gate's depot word, and
  things in the order in which their depot partition holds them. Gives the
  record after `walked` decided each, one after the other: each against the
  rows that the things before it wrote. `:wrote` is not in it."
  [record gate things]
  (dissoc (reduce (fn [record thing] (walked record gate thing)) record things) :wrote))

(defn- writes-of-line
  "Takes what `walked-line` takes. Gives the writes of each thing, in the
  order of the things: what `walked` gave under `:wrote`."
  [record gate things]
  (mapv :wrote (rest (reductions (fn [record thing] (walked record gate thing)) record things))))

(defn- words-by-number
  "Takes a record. Gives, for each answer, the number of its act-id's uuid
  and its word: :accepted, or the reason."
  [record]
  (into (sorted-map)
        (map (fn [[k row]] [(.getLeastSignificantBits (UUID/fromString (subs k (- (count k) 36)))) (or (:refused row) :accepted)]))
        (:answers record)))

(defn- rows-by-place
  "Takes a record in the form of `empty-task`. Gives a map from [table key]
  to the row at that key, for the answers, the facts and the facts by
  value."
  [record]
  (into {}
        (for [table [:answers :facts :by-value]
              [k row] (get record table)]
          [[table k] row])))

(defn- fields-that-differ
  "Takes two rows, each a map or nil. Gives the set of the fields that the
  two do not hold alike, a run of bytes compared by its content; #{:no-row}
  when one of the two is no row."
  [a b]
  (cond (and (nil? a) (nil? b)) #{}
        (or (nil? a) (nil? b)) #{:no-row}
        :else (into #{}
                    (remove (fn [field] (= (comparable (get a field)) (comparable (get b field)))))
                    (concat (keys a) (keys b)))))

(defn- rows-that-differ
  "Takes two maps of `rows-by-place`. Gives a map from each place whose row
  is not the same in both to the fields that differ."
  [rows-a rows-b]
  (into {}
        (keep (fn [at] (let [fields (fields-that-differ (get rows-a at) (get rows-b at))]
                         (when (seq fields) [at fields]))))
        (distinct (concat (keys rows-a) (keys rows-b)))))

;; ------------------------------------------------- the lines that are decided

(def ^:private stray-of-the-other-gate
  "A thing with an act-id of the streaming gate, for the base: on the
  microbatch depot it is a stray. It holds no time."
  {:act-id [:base :streaming (uuid 18)] :depot :streaming :layer :base :facts [{:e :p1 :k :stray}]})

(def ^:private line
  "A line of things into a base that is made, and the record before it. The
  things are in the order of the depot. Among them: two acts that replace
  one fact; two that write one place and name nothing; a give and an act
  under it, in both orders; a value alone on its entity; a repeat; one
  act-id under two things; two acts for layers of the other gate; a plain
  value under a plain key; two strays; acts with no standing, one of which
  carries the greatest stamp there is; an act with
  three values that is accepted, and one that is refused whole; an act that
  writes a place and names nothing, then one that replaces the fact that
  was there before the line; and two things that the declaration refuses."
  (let [before before-the-line
        answered (base-thing 801 :system system-own {:e :p1 :k :repeat})
        text-on (fn [e index] (value-at e :text (str "text " index)))]
    {:record
     (task-with
      (into base-made
            [[:facts ":base :p1 :title" (fact-row base-writer (arrived-fact :p1 :title "the fact before") (uuid 802) 0 before time-before-the-line)]
             [:facts ":base :p2 :old" (fact-row base-writer (arrived-fact :p2 :old) (uuid 803) 0 before time-before-the-line)]
             [:answers (record/answer-key :base (uuid 801)) (record/answer-row nil before time-before-the-line (digest/digest answered))]])
      {":base" before})
     :things
     [;; two acts that replace one fact
      (base-thing 1 :system system-own (assoc (value-at :p1 :title "a") :replaces [(base-id 802) 0]))
      (base-thing 2 :system system-own {:e :p2 :k :note} (assoc (value-at :p1 :title "b") :replaces [(base-id 802) 0]))
      ;; two acts that write one place and name nothing
      (base-thing 3 :system system-own {:e :p3 :k :mark})
      (base-thing 4 :system system-own {:e :z1 :k :note} {:e :p3 :k :mark})
      ;; a give and an act under it; an act and then the give
      (base-thing 5 :system system-own {:e :carol :k :softland.store/permission :v carols})
      (base-thing 6 :carol carols {:e :carol :k :note})
      (base-thing 7 :dave daves-in-base {:e :dave :k :note})
      (base-thing 8 :system system-own {:e :dave :k :softland.store/permission :v daves-in-base})
      ;; a value alone on its entity
      (base-thing 9 :system system-own (text-on :z2 2))
      ;; a repeat, and one act-id under two things
      answered
      (base-thing 14 :system system-own {:e :p1 :k :twice})
      (base-thing 14 :system system-own {:e :p2 :k :twice})
      ;; two acts for layers of the other gate
      {:act-id [:alice :microbatch (uuid 15)] :depot :microbatch declaration/time-part now :version 1 :who :alice :layer :alice
       :permission alice-own :session :s1 :facts [{:e :p1 :k :stranger}]}
      {:act-id [:nobody :microbatch (uuid 16)] :depot :microbatch declaration/time-part now :version 1 :who :alice :layer :nobody
       :permission [:alice :nobody :nobody] :session :s1 :facts [{:e :p2 :k :stranger}]}
      ;; a plain value under a plain key, and two strays
      (base-thing 17 :system system-own {:e :p1 :k :plain :v "a plain value"})
      5
      stray-of-the-other-gate
      ;; no standing: by a who that is not the holder, carrying the greatest stamp there is; with a permission that no act gives; by a who that is not the holder
      (assoc (base-thing 19 :mallory carols {:e :p3 :k :m}) :based-on {[(base-id 802) 0] Long/MAX_VALUE})
      (base-thing 20 :mallory mallorys {:e :p3 :k :m})
      (base-thing 21 :mallory system-own {:e :p3 :k :m})
      ;; an act with three values, accepted; and one refused whole
      (base-thing 22 :system system-own (text-on :r1 4) (text-on :r2 5) (text-on :r3 6))
      (base-thing 23 :system system-own (text-on :z1 7) (text-on :q2 8) (assoc (text-on :q3 9) :replaces [(base-id 999) 0]))
      ;; one act writes a place and names nothing; the next replaces the fact that was there before the line
      (base-thing 25 :system system-own {:e :p2 :k :old})
      (base-thing 26 :system system-own {:e :z2 :k :note} {:e :p2 :k :old :replaces [(base-id 803) 0]})
      ;; things that the declaration refuses: a key of the store's namespace that the store does not know, and a value over its bound
      (base-thing 27 :system system-own {:e :p1 :k :softland.store/lock-request :v 2})
      (base-thing 28 :system system-own (value-at :p2 :large (text-of-bytes 65537)))]}))

(def ^:private making-line
  "A line into a base that is not made: an act before the making, two making
  acts with their facts in the two orders, and acts after. The record
  before it holds nothing."
  {:record empty-task
   :things [(base-thing 30 :system system-own {:e :base :k :note})
            (base-thing 31 :system system-own {:e :base :k :softland.store/depot :v :microbatch} {:e :system :k :softland.store/permission :v system-own})
            (base-thing 32 :system system-own {:e :system :k :softland.store/permission :v system-own} {:e :base :k :softland.store/depot :v :microbatch})
            (base-thing 33 :system system-own {:e :base :k :note-2})
            (base-thing 34 :system system-own {:e :carol :k :softland.store/permission :v carols})
            (base-thing 35 :carol carols {:e :carol :k :note})
            (base-thing 36 :carol carols (value-at :carol :text "a value in a base made in this line"))]})

(def ^:private marks-line
  "A line of things into a base that marks two keys, :on and :gone, and the
  record before it. The things are in the order of the depot. Among them:
  two acts on one place under a marked key; the same value written again; a
  retract of a fact from before the mark, and one of a fact since it; a
  place that comes to hold the value of another place; a fact that names
  what it replaces; a write of the marks that takes one key off and marks a
  new one, then a fact under the new key and a fact under the key that was
  taken off, a second write of the marks, and a stale fact under a marked
  key; two writes of who may see the layer; two writes of what the layer
  builds on that name the same fact to replace; a holder of see who acts,
  who names her permission without its slot, who gives, and who sends a
  value under a marked key; a retract of the marks; marks on another
  entity. `:since` is the stamp since which the two keys are marked, 20:
  a fact written before the mark has an earlier stamp, 10, and a fact
  written since it a later one, the base's last stamp before the line."
  (let [since 20
        before before-the-line
        older 10
        fact-before (fn [e k v n stamp]
                      [:facts (record/place-key :base e k)
                       (fact-row base-writer (arrived-fact e k v) (uuid n) 0 stamp time-before-the-line)])
        entry-before (fn [e k v n]
                       [:by-value (record/by-value-key :base k since (record/value-digest-of v) e)
                        (by-value-row base-writer (uuid n) 0 before time-before-the-line)])]
    {:since since
     :record
     (task-with
      (into base-made
            [[:facts ":base :carol :softland.store/permission"
              (fact-row base-writer (arrived-fact :carol :softland.store/permission carols-see) (uuid 806) 0 before time-before-the-line)]
             [:facts base-marks-place (base-marks-row {:on since :gone since})]
             [:facts ":base :base :softland.store/builds-on"
              (fact-row base-writer (arrived-fact :base :softland.store/builds-on [:older-base]) (uuid 804) 0 before time-before-the-line)]
             (fact-before :p1 :on :c0 810 before) (entry-before :p1 :on :c0 810)
             (fact-before :q1 :on :c0 811 before) (entry-before :q1 :on :c0 811)
             (fact-before :r1 :on :from-before-the-mark 812 older)
             (fact-before :q3 :on :c5 813 before) (entry-before :q3 :on :c5 813)
             (fact-before :r2 :on :c6 805 before) (entry-before :r2 :on :c6 805)
             (fact-before :q2 :on :c8 814 before) (entry-before :q2 :on :c8 814)])
      {":base" before})
     :things
     [;; two acts on one place under a marked key
      (base-thing 40 :system system-own (value-at :p1 :on :c1))
      (base-thing 41 :system system-own (value-at :p1 :on :c2))
      ;; the same value again; a retract of a fact from before the mark
      (base-thing 42 :system system-own (value-at :q1 :on :c0))
      (base-thing 43 :system system-own {:e :r1 :k :on})
      ;; a place that holds a value since the mark comes to hold the value of :p1
      (base-thing 44 :system system-own {:e :z1 :k :note} (value-at :q2 :on :c2))
      ;; a write of the marks: :gone off, :in on; a fact under the new key; a second write of the marks; a fact under the key whose mark went
      (base-thing 45 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on :in}})
      (base-thing 46 :system system-own (value-at :p2 :in :c1))
      (base-thing 47 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on :in :late}})
      (base-thing 59 :system system-own (value-at :p3 :gone :c1))
      ;; a fact under a marked key that names a fact which is not there: refused, and nothing of it in the order
      (base-thing 60 :system system-own (assoc (value-at :q1 :on :c9) :replaces [(base-id 999) 0]))
      ;; two writes of who may see; two of the list that name one fact to replace
      (base-thing 48 :system system-own {:e :base :k :softland.store/who-may-see :v :members})
      (base-thing 49 :system system-own {:e :r3 :k :note} {:e :base :k :softland.store/who-may-see :v :everyone})
      (base-thing 50 :system system-own {:e :base :k :softland.store/builds-on :v [:team] :replaces [(base-id 804) 0]})
      (base-thing 51 :system system-own {:e :r3 :k :note-2} {:e :base :k :softland.store/builds-on :v [] :replaces [(base-id 804) 0]})
      ;; a holder of see: she acts, she names her permission without its slot, she gives
      (base-thing 52 :carol carols-see {:e :carol :k :note})
      (base-thing 53 :carol carols {:e :carol :k :note})
      (base-thing 54 :carol carols-see {:e :dave :k :softland.store/permission :v [:dave :base :base carols-see]})
      ;; a retract of a fact since the mark; a fact that names what it replaces
      (base-thing 55 :system system-own {:e :q3 :k :on})
      (base-thing 56 :system system-own (assoc (value-at :r2 :on :c7) :replaces [(base-id 805) 0]))
      ;; a retract of the marks; marks on another entity
      (base-thing 57 :system system-own {:e :base :k :softland.store/marked-keys})
      (base-thing 58 :system system-own {:e :p1 :k :softland.store/marked-keys :v #{:on}})
      ;; a holder of see who sends a value under a marked key: refused under its own act-id, and no row of its value
      (base-thing 61 :carol carols-see (value-at :carol :on :c1))]}))

;; ---------------------------------------------------- what a gate may copy

;; `forms-of` reads a source file as data. It is defined further down, with
;; the reading of the source for the names of a lock.
(declare forms-of)

(defn- copies-of-as-written
  "Gives the function `copies-of` as the source file of the walk writes it,
  or nil when the file does not hold exactly one `defn` of that name. The
  file, `softland/store/module/walk.clj` on the classpath, is read as data
  and not loaded: its namespace requires Rama, which this proof's classpath
  does not hold. The parameters and the body of that `defn` are made a
  function here, with Clojure's own names. So this gives what the text of
  the function says. The var that a cluster loads is compiled in the walk's
  namespace, beside Rama's names: that is not reached here."
  []
  (let [forms (filter (fn [form] (and (seq? form) (= 'defn (first form)) (= 'copies-of (second form))))
                      (forms-of (io/resource "softland/store/module/walk.clj")))
        form (when (= 1 (count forms)) (first forms))
        after-the-name (drop 2 form)
        body (if (string? (first after-the-name)) (rest after-the-name) after-the-name)]
    (when form
      (binding [*ns* (the-ns 'clojure.core)]
        (eval (cons 'fn body))))))

(def ^:private copy-cases
  "The cases of what a gate may copy to other tasks: the writes of an
  accept, of a refusal and of a thing that the declaration refused, as
  `softland.store.gate.decision/writes` gives them, through the proof's own
  filter `to-copy` and through `copies-of` as its source file writes it.
  The accept is of one act with a fact under a plain key, a fact under a
  marked key that takes the entry of the value before out and puts its own
  in, and a write of the marks."
  (let [stamp 300
        thing (arrived 1 (value-at :note-1 :text) (value-at :b1 :on :c1) {:e :alice :k :softland.store/marked-keys :v #{:on :new}})
        act (act-of thing)
        note-place ":alice :note-1 :text"
        on-place ":alice :b1 :on"
        marks-place ":alice :alice :softland.store/marked-keys"
        marks-before (record/with-level-since (setting-row 7 :softland.store/marked-keys #{:on}) {:on 100})
        row-before (fact-row writer (arrived-fact :b1 :on :c0) (uuid 6) 0 200 now)
        entry (fn [v] (record/by-value-key :alice :on 100 (record/value-digest-of v) :b1))
        accept (decision/writes (act-id 1) act nil stamp now (digest/digest thing)
                                {:since-by-place (decision/since-by-place act marks-before)
                                 :place-rows {on-place row-before marks-place marks-before}
                                 :level-from stamp})
        refusal (decision/writes (act-id 1) act :replaced-fact-not-current stamp now (digest/digest thing) {})
        no-act (decision/writes (act-id 1) nil :malformed stamp now (digest/digest thing) {})
        shape (fn [writes] (mapv (fn [[which k row]] [which k (some? row)]) writes))
        of-the-lines (fn [] (concat (writes-of-line (:record line) :microbatch (:things line))
                                    (writes-of-line (:record marks-line) :microbatch (:things marks-line))))]
    [["the writes of that accept, in the act's order, as [which, key, whether a row is written]: the fact under the plain key; the fact under the marked key, the entry of the value before taken out, and the entry of the new value; the row of the marks; then the answer and the last stamp"
      (fn [] (shape accept))
      [[:fact note-place true] [:fact on-place true] [:by-value (entry :c0) false] [:by-value (entry :c1) true]
       [:fact marks-place true] [:answer (record/answer-key :alice (uuid 1)) true] [:last-stamp nil true]]]
     ["what a gate may copy of them, by the proof's own filter: exactly the writes of a fact and of the facts by value, in their order, the row that is nil among them; they are the first five of the seven, each as it was written"
      (fn [] [(shape (to-copy accept)) (= (to-copy accept) (subvec accept 0 5))])
      [[[:fact note-place true] [:fact on-place true] [:by-value (entry :c0) false] [:by-value (entry :c1) true]
        [:fact marks-place true]]
       true]]
     ["the answer and the last stamp are never among them; a refusal's writes and those of a thing that the declaration refused give an empty vector"
      (fn [] [(filterv #{:answer :last-stamp} (map first (to-copy accept))) (mapv first refusal) (to-copy refusal)
              (mapv first no-act) (to-copy no-act) (to-copy [])])
      [[] [:answer :last-stamp] [] [:answer :last-stamp] [] []]]
     ["`copies-of`, as the source file of the walk writes it: for the accept, the same five writes in the same order, the nil row among them; an empty vector for the refusal, for the thing that the declaration refused, and for no write; and a vector each time"
      (fn [] (let [copies-of (copies-of-as-written)]
               [(some? copies-of) (= (copies-of accept) (subvec accept 0 5)) (shape (copies-of accept))
                (copies-of refusal) (copies-of no-act) (copies-of [])
                (mapv vector? [(copies-of accept) (copies-of refusal)])]))
      [true true
       [[:fact note-place true] [:fact on-place true] [:by-value (entry :c0) false] [:by-value (entry :c1) true]
        [:fact marks-place true]]
       [] [] [] [true true]]]
     ["over the writes of each thing of the two lines that the proof decides in memory, `copies-of` as written gives what the proof's own filter gives: how many things, how many of them with something to copy, and how many on which the two differ"
      (fn [] (let [copies-of (copies-of-as-written)
                   all (of-the-lines)]
               [(count all) (count (filter (comp seq to-copy) all))
                (count (remove (fn [writes] (= (copies-of writes) (to-copy writes))) all))]))
      [48 25 0]]]))

;; ------------------------------------------------ one decision, run again

(def ^:private second-run-rows
  "For each: a gate's depot word, the record before, a thing, the kinds of
  the writes of its decision, and the word of its answer, nil for an
  accept. Five kinds of thing for each gate: a making act, a fact with a
  value, a fact that replaces the current fact of its place, a write of the
  marks, and an act that is refused."
  (let [alice-made (task-with [[:facts ":alice :alice :softland.store/depot" depot-row]
                               [:facts ":alice :alice :softland.store/permission" (permission-row alice-own)]
                               [:facts ":alice :note-1 :text" (row-by 7 0)]]
                              {":alice" 200})
        base-with-a-fact (task-with (conj base-made
                                          [:facts ":base :doc-1 :title"
                                           (fact-row base-writer (arrived-fact :doc-1 :title "before") (uuid 802) 0 before-the-line
                                                     time-before-the-line)])
                                    {":base" before-the-line})]
    [["at the streaming gate, the making act of a group's layer" :streaming empty-task (as-sent (making :team :alice :alice) :streaming)
      [:fact :fact :answer :last-stamp] nil]
     ["at the streaming gate, a fact with a value" :streaming alice-made (arrived 2 (value-at :note-2 :text))
      [:fact :answer :last-stamp] nil]
     ["at the streaming gate, a fact that replaces the current fact of its place"
      :streaming alice-made (arrived 3 (assoc (value-at :note-1 :text "after") :replaces [(act-id 7) 0]))
      [:fact :answer :last-stamp] nil]
     ["at the streaming gate, a write of the marks" :streaming alice-made (arrived 4 {:e :alice :k :softland.store/marked-keys :v #{:on}})
      [:fact :answer :last-stamp] nil]
     ["at the streaming gate, an act that is refused: its fact names a fact that is not current"
      :streaming alice-made (arrived 5 (assoc (value-at :note-1 :text "after") :replaces [(act-id 6) 0]))
      [:answer :last-stamp] :replaced-fact-not-current]
     ["at the microbatch gate, the base's making act"
      :microbatch empty-task (base-thing 31 :system system-own {:e :base :k :softland.store/depot :v :microbatch}
                                         {:e :system :k :softland.store/permission :v system-own})
      [:fact :fact :answer :last-stamp] nil]
     ["at the microbatch gate, a fact with a value" :microbatch base-with-a-fact (base-thing 2 :system system-own (value-at :doc-2 :title "a"))
      [:fact :answer :last-stamp] nil]
     ["at the microbatch gate, a fact that replaces the current fact of its place"
      :microbatch base-with-a-fact (base-thing 3 :system system-own (assoc (value-at :doc-1 :title "after") :replaces [(base-id 802) 0]))
      [:fact :answer :last-stamp] nil]
     ["at the microbatch gate, a write of the marks"
      :microbatch base-with-a-fact (base-thing 4 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on}})
      [:fact :answer :last-stamp] nil]
     ["at the microbatch gate, an act that is refused: its fact names a fact that is not current"
      :microbatch base-with-a-fact (base-thing 5 :system system-own (assoc (value-at :doc-1 :title "after") :replaces [(base-id 999) 0]))
      [:answer :last-stamp] :replaced-fact-not-current]]))

(def ^:private second-run-cases
  "The cases of a decision that is run again: one for each row of
  `second-run-rows`. Each gives five things. The kinds of the writes of the
  first run, and the word of its answer. Whether a second run from the same
  record, which reads the rows that the first read and makes the same
  stamp, gives the same writes in every field: what a second attempt of a
  batch does. What a run writes on the record that the first run left,
  where the act-id's answer is, so that check (a) ends it: nothing. And
  whether that run left every table as it was, the layer's last stamp
  too."
  (map (fn [[what gate record thing kinds word]]
         [(str "a second run of one decision: " what)
          (fn [] (let [first-run (walked record gate thing)
                       second-run (walked record gate thing)
                       on-what-it-left (walked first-run gate thing)
                       [layer _ act-uuid] (:act-id thing)
                       answer (get-in first-run [:answers (record/answer-key layer act-uuid)])]
                   [(mapv first (:wrote first-run))
                    (if answer (:refused answer) :no-answer)
                    (= (comparable (:wrote first-run)) (comparable (:wrote second-run)))
                    (:wrote on-what-it-left)
                    (= (comparable (dissoc first-run :wrote)) (comparable (dissoc on-what-it-left :wrote)))]))
          [kinds word true [] true]])
       second-run-rows))

;; ----------------------------------------------------- the stamp in a line

(def ^:private stamp-line-cases
  "The cases of the stamp in a line, each decided by the walk in memory, in
  a layer of each gate: ten acts whose times rise, stand still, go back by
  two milliseconds and by a minute, leap far ahead, come back, fall to 0,
  leap to the greatest long and rise again; the same ten acts all with the
  time `now`; a line of accepts and refusals, one of them with no time and
  one with a time far ahead, and an act that carries the greatest stamp
  there is; and an act with a time far ahead, then two acts after it that
  each replace the fact before and carry the stamp that its answer gave.
  Alice's layer is made with the stamp 0, so its line counts on from 1; the
  base's from `before-the-line` plus one."
  (let [times [now (+ now 5) (+ now 5) (+ now 3) (- now 60000) far-ahead (+ now 6) 0 Long/MAX_VALUE (+ now 2000)]
        timed (fn [thing t] (assoc thing declaration/time-part t))
        alices-at (fn [times] (vec (map-indexed (fn [i t] (timed (arrived (+ 400 i) (value-at (keyword (str "n" i)) :text)) t)) times)))
        bases-at (fn [times] (vec (map-indexed (fn [i t] (timed (base-thing (+ 400 i) :system system-own (value-at (keyword (str "n" i)) :text)) t))
                                               times)))
        alices (alices-at times)
        bases (bases-at times)
        alice-made (task-with [[:facts ":alice :alice :softland.store/depot" depot-row]
                               [:facts ":alice :alice :softland.store/permission" (permission-row alice-own)]]
                              {":alice" 0})
        base-is-made (task-with base-made {":base" before-the-line})
        answer-in (fn [record thing] (let [[layer _ act-uuid] (:act-id thing)] (get-in record [:answers (record/answer-key layer act-uuid)])))
        stamps-in (fn [record things] (mapv #(:stamp (answer-in record %)) things))
        times-in (fn [record things] (mapv #(:time (answer-in record %)) things))
        fact-times (fn [record layer] (mapv (fn [i] (:time (get-in record [:facts (record/place-key layer (keyword (str "n" i)) :text)])))
                                            (range 10)))
        of-alices (walked-line alice-made :streaming alices)
        of-the-base (walked-line base-is-made :microbatch bases)
        ;; accepts and refusals in Alice's layer, one after the other
        mixed [(arrived 510 (value-at :m1 :text))
               (without-time (arrived 511 (value-at :m2 :text)))
               (arrived 512 (value-at :m3 :text))
               (assoc (timed (arrived 513 (value-at :m4 :text)) far-ahead) :version 2)
               (assoc (arrived 514 (value-at :m5 :text)) :who :mallory)
               (arrived 515 (assoc (value-at :m1 :text "b") :replaces [(act-id 999) 0]))
               (assoc (arrived 516 (value-at :m6 :text)) :based-on {[(act-id 510) 0] Long/MAX_VALUE})
               (arrived 517 (value-at :m7 :text))]
        of-mixed (walked-line alice-made :streaming mixed)
        ;; an act with a time far ahead; then an act that replaces its fact and carries its stamp; then one that does the same for that act
        honest-after-far (fn [gate thing-of id-of record]
                           (let [layer (first (id-of 520))
                                 last-stamp (get-in record [:last-stamp (declaration/id-text layer)])
                                 things [(timed (thing-of 520 (value-at :k1 :text "a")) far-ahead)
                                         (assoc (thing-of 521 (assoc (value-at :k1 :text "b") :replaces [(id-of 520) 0]))
                                                :based-on {[(id-of 520) 0] (+ last-stamp 1)})
                                         (assoc (thing-of 522 (assoc (value-at :k1 :text "c") :replaces [(id-of 521) 0]))
                                                :based-on {[(id-of 521) 0] (+ last-stamp 2)})]
                                 after (walked-line record gate things)]
                             [(vec (vals (words-by-number after))) (mapv #(- % last-stamp) (stamps-in after things)) (times-in after things)
                              (:value (get-in after [:facts (record/place-key layer :k1 :text)]))]))]
    [["ten acts of a streaming layer, whose times go back, leap ahead to 2^50 ms and to the greatest long, and fall to 0: each is accepted, the stamps of their answers are 1 to 10 after the making act's 0, and the last stamp kept for the layer is 10"
      (fn [] [(vec (vals (words-by-number of-alices))) (stamps-in of-alices alices) (get-in of-alices [:last-stamp ":alice"])])
      [(vec (repeat 10 :accepted)) (vec (range 1 11)) 10]]
     ["the same ten acts in the base: the stamps count on from the base's last stamp before them, one by one"
      (fn [] [(vec (vals (words-by-number of-the-base))) (stamps-in of-the-base bases) (get-in of-the-base [:last-stamp ":base"])])
      [(vec (repeat 10 :accepted)) (vec (range (+ before-the-line 1) (+ before-the-line 11))) (+ before-the-line 10)]]
     ["each answer and each row of a fact holds the time on its act as it is, beside the stamp, in a layer of each gate: the time far ahead, the greatest long and 0 among them"
      (fn [] [(times-in of-alices alices) (fact-times of-alices :alice) (times-in of-the-base bases) (fact-times of-the-base :base)])
      [times times times times]]
     ["the times move no stamp and change no other field: the same ten acts with the time `now` on each give records that differ only in :time, in the answers and the rows of the nine acts whose time is not `now`, and the same last stamps, at a gate of each kind"
      (fn [] (mapv (fn [[of-the-times of-now]]
                     (let [differ (rows-that-differ (rows-by-place of-the-times) (rows-by-place of-now))]
                       [(count differ) (set (vals differ)) (= (:last-stamp of-the-times) (:last-stamp of-now))]))
                   [[of-alices (walked-line alice-made :streaming (alices-at (repeat 10 now)))]
                    [of-the-base (walked-line base-is-made :microbatch (bases-at (repeat 10 now)))]]))
      [[18 #{#{:time}} true] [18 #{#{:time}} true]]]
     ["accepts and refusals of one layer take their stamps by one rule: an accept, a thing with no time, an accept, a wrong version with a time far ahead, an act by a who that is not the holder, an act that names a fact that is not current, an accept that carries the greatest stamp there is, and an accept after it get the stamps 1 to 8 in their order, with no gap; each answer holds the time on its thing, and the one with no time holds none"
      (fn [] [(words-by-number of-mixed) (stamps-in of-mixed mixed) (times-in of-mixed mixed)
              (contains? (answer-in of-mixed (second mixed)) :time)])
      [{510 :accepted 511 :no-door-time 512 :accepted 513 :unknown-version 514 :who-not-the-permissions-holder
        515 :replaced-fact-not-current 516 :accepted 517 :accepted}
       (vec (range 1 9)) [now nil now far-ahead now now now now] false]]
     ["an accepted act with a time far ahead shuts no one out: after it, an act with the time `now` that replaces its fact and carries the stamp that its answer gave is accepted, and so is the act after that one; their stamps are the next two, and the place holds the last value. As [words, stamps after the last stamp before them, times, the value], at a gate of each kind"
      (fn [] [(honest-after-far :streaming arrived act-id alice-made)
              (honest-after-far :microbatch (fn [n & facts] (apply base-thing n :system system-own facts)) base-id base-is-made)])
      [[[:accepted :accepted :accepted] [1 2 3] [far-ahead now now] "\"c\""]
       [[:accepted :accepted :accepted] [1 2 3] [far-ahead now now] "\"c\""]]]]))

;; ------------------------------------------- a mark holds from the next act

(def ^:private mark-cases
  "The cases of a mark and of a fact under its key in the next act, for a
  layer of each gate: by hand over `softland.store.gate.decision/writes`,
  where the stamp of each place of the second act is taken from the mark's
  row as the first act wrote it; and by the walk in memory, for both gate
  words. In a layer that the line makes, the making act has the stamp 0,
  the mark 1 and the fact 2."
  (let [of-the-mark 1
        of-the-fact 2
        by-hand (fn [mark-thing fact-thing]
                  (let [mark-act (act-of mark-thing)
                        marks-key (decision/marked-keys-place mark-act)
                        [[_ _ marks-row]] (decision/writes (:act-id mark-thing) mark-act nil of-the-mark (decision/time-on mark-thing)
                                                           (digest/digest mark-thing)
                                                           {:since-by-place (decision/since-by-place mark-act nil)
                                                            :place-rows {marks-key nil}
                                                            :level-from of-the-mark})
                        fact-act (act-of fact-thing)
                        since (decision/since-by-place fact-act marks-row)
                        rows-before (into {marks-key marks-row} (map (fn [k] [k nil])) (decision/rows-before-needed since {}))
                        writes (decision/writes (:act-id fact-thing) fact-act nil of-the-fact (decision/time-on fact-thing)
                                                (digest/digest fact-thing)
                                                {:since-by-place since :place-rows rows-before :level-from of-the-fact})]
                    [(record/marked-keys-of marks-row) since (mapv (fn [[which k row]] [which k (:stamp row)]) (to-copy writes))]))
        marks (fn [layer keys] {:e layer :k :softland.store/marked-keys :v keys})
        alices-making (as-sent (making :alice :alice :alice) :streaming)
        bases-making (base-thing 1 :system system-own {:e :base :k :softland.store/depot :v :microbatch}
                                 {:e :system :k :softland.store/permission :v system-own})
        in-the-line (fn [gate layer things]
                      (let [after (walked-line empty-task gate things)
                            [_ mark-thing fact-thing] things
                            stamp-under (fn [thing] (:stamp (get-in after [:answers (record/answer-key layer (nth (:act-id thing) 2))])))]
                        [(vec (vals (words-by-number after)))
                         (stamp-under mark-thing)
                         (record/marked-keys-of (get-in after [:facts (record/place-key layer layer :softland.store/marked-keys)]))
                         (set (keys (:by-value after)))
                         (:stamp (get-in after [:facts (record/place-key layer :b1 :on)]))
                         (mapv first (last (writes-of-line empty-task gate things)))]))
        entry (fn [layer since e] (record/by-value-key layer :on since (digest-of-text ":c1") e))]
    [["in a streaming layer, by hand: the row of the marks holds the stamp of the act that wrote it for the key; the next act's place under that key has that stamp; and among its writes is the fact's entry of the facts by value, under the key's stamp and with the fact's own stamp"
      (fn [] (by-hand (arrived 2 (marks :alice #{:on})) (arrived 3 (value-at :b1 :on :c1))))
      [{:on of-the-mark} {":alice :b1 :on" of-the-mark}
       [[:fact ":alice :b1 :on" of-the-fact] [:by-value (entry :alice of-the-mark :b1) of-the-fact]]]]
     ["in the base, by hand: the same"
      (fn [] (by-hand (base-thing 2 :system system-own (marks :base #{:on})) (base-thing 3 :system system-own (value-at :b1 :on :c1))))
      [{:on of-the-mark} {":base :b1 :on" of-the-mark}
       [[:fact ":base :b1 :on" of-the-fact] [:by-value (entry :base of-the-mark :b1) of-the-fact]]]]
     ["at the streaming gate, by the walk in memory: a making act, a write of the marks and a fact under the marked key, one after the other, are accepted; the key's stamp is the stamp of the answer of the act that marked it; the fact's entry is in the facts by value under that stamp; the fact's stamp is later; and the fact's act writes the fact, its entry, the answer and the last stamp"
      (fn [] (in-the-line :streaming :alice [alices-making (arrived 2 (marks :alice #{:on})) (arrived 3 (value-at :b1 :on :c1))]))
      [[:accepted :accepted :accepted] of-the-mark {:on of-the-mark} #{(entry :alice of-the-mark :b1)} of-the-fact
       [:fact :by-value :answer :last-stamp]]]
     ["at the microbatch gate, by the walk in memory: the same for the base"
      (fn [] (in-the-line :microbatch :base [bases-making (base-thing 2 :system system-own (marks :base #{:on}))
                                             (base-thing 3 :system system-own (value-at :b1 :on :c1))]))
      [[:accepted :accepted :accepted] of-the-mark {:on of-the-mark} #{(entry :base of-the-mark :b1)} of-the-fact
       [:fact :by-value :answer :last-stamp]]]
     ["a fact in the act that marks its own key is in no run, at either gate: the act is decided with the marks as they were before it; a fact under that key in the next act is in the order, under the stamp of the act that marked the key"
      (fn [] (mapv (fn [[gate things]]
                     (let [with-the-mark (walked-line empty-task gate (take 2 things))
                           after (walked-line empty-task gate things)]
                       [(vec (vals (words-by-number after))) (set (keys (:by-value with-the-mark))) (set (keys (:by-value after)))]))
                   [[:streaming [alices-making (arrived 2 (marks :alice #{:on}) (value-at :b1 :on :c1)) (arrived 3 (value-at :b2 :on :c1))]]
                    [:microbatch [bases-making (base-thing 2 :system system-own (marks :base #{:on}) (value-at :b1 :on :c1))
                                  (base-thing 3 :system system-own (value-at :b2 :on :c1))]]]))
      [[[:accepted :accepted :accepted] #{} #{(entry :alice of-the-mark :b2)}]
       [[:accepted :accepted :accepted] #{} #{(entry :base of-the-mark :b2)}]]]]))

;; ----------------------------------------- a line, decided in memory: cases

(def ^:private far-act
  "An act of the base that carries the greatest stamp there is, for the fact
  of act 802 that a record before a line holds. `line-cases` decides it with
  the time `now` on it and with a time far ahead."
  (assoc (base-thing 70 :system system-own {:e :z3 :k :edge})
         :based-on {[(base-id 802) 0] Long/MAX_VALUE}))

(def ^:private line-cases
  "The cases of `line` and of `making-line`, each decided by the walk in
  memory at the microbatch gate, one thing after the other in the order of
  the depot: the word of each answer, the rows, the stamps and the strays;
  the line decided a second time; a line of strays alone and one of repeats
  alone; and `far-act` with two times on it. `s` gives the stamp of the
  n-th decision of the base in a line, counted from 0."
  (let [{:keys [record things]} line
        after (walked-line record :microbatch things)
        words (words-by-number after)
        facts (:facts after)
        s (fn [n] (+ before-the-line 1 n))
        rows-by (fn [n] (filter (fn [[_ row]] (= (uuid n) (:by-act row))) facts))
        made (walked-line (:record making-line) :microbatch (:things making-line))
        base-is-made (task-with base-made {":base" before-the-line})
        repeat-a (base-thing 801 :system system-own {:e :k1 :k :note})
        repeat-b (base-thing 802 :system system-own {:e :k2 :k :note})
        answered-twice (task-with (into base-made
                                        [[:answers (record/answer-key :base (uuid 801))
                                          (record/answer-row nil before-the-line time-before-the-line (digest/digest repeat-a))]
                                         [:answers (record/answer-key :base (uuid 802))
                                          (record/answer-row :layer-not-made before-the-line time-before-the-line (digest/digest repeat-b))]])
                                  {":base" before-the-line})
        answer-of-far (fn [thing] (let [row (get-in (walked base-is-made :microbatch thing) [:answers (record/answer-key :base (uuid 70))])]
                                    [(:refused row) (:stamp row) (:time row)]))]
    [["the word of each answer after the line, by the number of its act-id; the two strays have none"
      (fn [] words)
      {1 :accepted 2 :replaced-fact-not-current 3 :accepted 4 :accepted 5 :accepted 6 :accepted 7 :permission-does-not-stand
       8 :accepted 9 :accepted 14 :accepted 15 :layer-belongs-to-the-other-gate 16 :layer-belongs-to-the-other-gate
       17 :accepted 19 :who-not-the-permissions-holder 20 :permission-does-not-stand 21 :who-not-the-permissions-holder
       22 :accepted 23 :replaced-fact-not-current 25 :accepted 26 :replaced-fact-not-current 27 :store-key-not-known
       28 :value-too-large 801 :accepted}]
     ["two acts that replace the same fact: the first in the depot's order is accepted and the second is stale; the place holds the first one's value"
      (fn [] (let [row (get facts ":base :p1 :title")] [(words 1) (words 2) (:by-act row) (:value row)]))
      [:accepted :replaced-fact-not-current (uuid 1) "\"a\""]]
     ["two acts that write one place and name nothing are both accepted, and the place holds the later one's row"
      (fn [] [(words 3) (words 4) (:by-act (get facts ":base :p3 :mark"))])
      [:accepted :accepted (uuid 4)]]
     ["a give and then an act under it: both accepted. An act and then the give that it needs: the act is refused and the give is accepted, since an act is decided against the record as the acts before it left it"
      (fn [] (mapv words [5 6 7 8]))
      [:accepted :accepted :permission-does-not-stand :accepted]]
     ["a value alone on its entity is accepted: its row holds the act and the text of the value"
      (fn [] [(words 9) (select-keys (get facts ":base :z2 :text") [:by-act :value])])
      [:accepted {:by-act (uuid 9) :value "\"text 2\""}]]
     ["one act-id under two things has one answer, that of the first thing: its fact is in the record and the second thing's is not. A thing under an act-id that had an answer before the line leaves that answer with its stamp, and writes no fact"
      (fn [] [(words 14) (:by-act (get facts ":base :p1 :twice")) (get facts ":base :p2 :twice")
              (:stamp (get-in after [:answers (record/answer-key :base (uuid 801))])) (get facts ":base :p1 :repeat")])
      [:accepted (uuid 14) nil before-the-line nil]]
     ["an act for a layer of the other gate has the word of such a layer, for each of two layers that start with the streaming depot: the record of this gate holds nothing of either, and the walk in memory holds no other record to read. Neither writes a fact"
      (fn [] [(words 15) (words 16) (count (filter #(or (str/starts-with? % ":alice ") (str/starts-with? % ":nobody ")) (keys facts)))])
      [:layer-belongs-to-the-other-gate :layer-belongs-to-the-other-gate 0]]
     ["a plain value under a plain key is accepted, and its row holds the text of the value"
      (fn [] [(words 17) (:value (get facts ":base :p1 :plain"))])
      [:accepted "\"a plain value\""]]
     ["the strays have no answer: the digest of each is in the set of its word, the proof's own SHA-256 of the thing's text"
      (fn [] [(words 18) (:strays after)])
      [nil {:no-act-id #{(sha-256 (utf8 "5"))}
            :act-id-of-the-other-gate #{(sha-256 (utf8 (text/text stray-of-the-other-gate)))}}]]
     ["no standing: a who that is not the holder, whose act carries the greatest stamp there is and gets the word of its standing, since no check reads a carried stamp; a permission that no act gave; a who that is not the holder"
      (fn [] (mapv words [19 20 21]))
      [:who-not-the-permissions-holder :permission-does-not-stand :who-not-the-permissions-holder]]
     ["an act with three values is accepted whole: each fact has its row, written by the act, with the text of its value. An act whose third fact is stale is refused whole: no row of it is in the record"
      (fn [] [(words 22) (mapv (fn [e] (select-keys (get facts (record/place-key :base e :text)) [:by-act :by-index :value])) [:r1 :r2 :r3])
              (words 23) (count (rows-by 23)) (mapv (fn [e] (get facts (record/place-key :base e :text))) [:z1 :q2 :q3])])
      [:accepted [{:by-act (uuid 22) :by-index 0 :value "\"text 4\""} {:by-act (uuid 22) :by-index 1 :value "\"text 5\""}
                  {:by-act (uuid 22) :by-index 2 :value "\"text 6\""}]
       :replaced-fact-not-current 0 [nil nil nil]]]
     ["one act writes a place and names nothing, and the next replaces the fact that was there before the line: the second is stale, and the place holds the first one's row"
      (fn [] [(words 25) (words 26) (:by-act (get facts ":base :p2 :old"))])
      [:accepted :replaced-fact-not-current (uuid 25)]]
     ["a thing that the declaration refuses has its word under its own act-id and no row of a fact: a key of the store's namespace that the store does not know, and a value over its bound"
      (fn [] [(words 27) (words 28) (count (rows-by 27)) (count (rows-by 28))])
      [:store-key-not-known :value-too-large 0 0]]
     ["no refusal writes a fact: each row of a fact after the line was written by an act that was accepted, or was there before the line; the numbers of the acts whose rows the record holds"
      (fn [] (vec (sort (distinct (map (fn [[_ row]] (.getLeastSignificantBits ^UUID (:by-act row))) facts)))))
      [1 4 5 6 8 9 14 17 22 25 800]]
     ["the stamps of the base's answers: the 20 decisions of the line have the stamps that count on from the base's last stamp before it, one by one with no gap, in the order of the depot, a refusal's as an accept's; the numbers of the act-ids in the order of their stamps are the order of the line"
      (fn [] (let [stamped (sort-by second
                                    (keep (fn [[k row]] (when (and (str/starts-with? k ":base ") (not= before-the-line (:stamp row)))
                                                          [(.getLeastSignificantBits (UUID/fromString (subs k (- (count k) 36)))) (:stamp row)]))
                                          (:answers after)))]
               [(mapv first stamped) (= (mapv second stamped) (mapv s (range 20)))]))
      [[1 2 3 4 5 6 7 8 9 14 17 19 20 21 22 23 25 26 27 28] true]]
     ["the last stamp is kept for a layer: the base's is the stamp of its last decision; each layer of the other gate, of which one thing was decided and refused, has 0, the stamp of the first decision of a layer. So a decision in one layer does not raise the stamps of another"
      (fn [] (:last-stamp after))
      {":base" (s 19) ":alice" 0 ":nobody" 0}]
     ["the line decided a second time from the same record gives the same record: no row differs in any field, the last stamps and the strays are the same, and more than 20 rows are rows that the line wrote"
      (fn [] (let [again (walked-line record :microbatch things)]
               [(rows-that-differ (rows-by-place after) (rows-by-place again))
                (= (:last-stamp after) (:last-stamp again)) (= (:strays after) (:strays again))
                (< 20 (count (rows-that-differ (rows-by-place record) (rows-by-place after))))]))
      [{} true true true]]
     ["the comparison of two records tells a row whose value alone differs"
      (fn [] (rows-that-differ {[:facts "k"] {:value "\"a\"" :stamp 1}} {[:facts "k"] {:value "\"b\"" :stamp 1}}))
      {[:facts "k"] #{:value}}]
     ["the line decided again on the record that it left: each thing finds its answer or is a stray, nothing is written, and every table is as it was, the last stamps and the strays too"
      (fn [] [(every? empty? (writes-of-line after :microbatch things))
              (= (comparable after) (comparable (walked-line after :microbatch things)))])
      [true true]]
     ["a line into a base that is not made: an act before the making is refused; the first making act makes the base; a second one names a permission that is there and gives one that is not beneath its own; then an act, a give and two acts under it, one after the other, are accepted"
      (fn [] [(words-by-number made) (:by-act (get-in made [:facts ":base :base :softland.store/depot"]))
              (:value (get-in made [:facts ":base :carol :text"]))])
      [{30 :layer-not-made 31 :accepted 32 :permission-fact-not-beneath-the-acts-own 33 :accepted 34 :accepted 35 :accepted 36 :accepted}
       (uuid 31) "\"a value in a base made in this line\""]]
     ["a line of strays alone: no answer, no fact and no stamp is written, and the last stamp stays; the digest of each stray is in the set of its word, and a thing that arrives twice is in it once"
      (fn [] (let [after-strays (walked-line base-is-made :microbatch [5 "a stray" 5 stray-of-the-other-gate])]
               [(= (comparable (dissoc base-is-made :strays)) (comparable (dissoc after-strays :strays)))
                (:strays after-strays)]))
      [true {:no-act-id #{(sha-256 (utf8 "5")) (sha-256 (utf8 "\"a stray\""))}
             :act-id-of-the-other-gate #{(sha-256 (utf8 (text/text stray-of-the-other-gate)))}}]]
     ["a line of repeats alone, an act-id with an accepted answer and one with a refused answer: nothing is written, no stamp is made, and the record is as it was"
      (fn [] [(= (comparable answered-twice) (comparable (walked-line answered-twice :microbatch [repeat-a repeat-b repeat-a])))
              (writes-of-line answered-twice :microbatch [repeat-a repeat-b repeat-a])])
      [true [[] [] []]]]
     ["neither the time on the act nor a stamp that it carries decides, at the microbatch gate too: an act that carries the greatest stamp there is is accepted with the time `now` on it and with a time far ahead; each time its stamp is the base's last stamp plus one, and its answer holds the time on it. As [word, stamp, time]"
      (fn [] [(answer-of-far far-act) (answer-of-far (assoc far-act declaration/time-part far-ahead))])
      [[nil (inc before-the-line) now] [nil (inc before-the-line) far-ahead]]]]))

(def ^:private marks-line-cases
  "The cases of `marks-line`, decided by the walk in memory at the
  microbatch gate, one thing after the other in the order of the depot: the
  word of each answer, the facts by value after the line, and that a mark
  holds from the act after it. `s` gives the stamp of the n-th decision of
  the line, counted from 0."
  (let [{:keys [record things since]} marks-line
        after (walked-line record :microbatch things)
        words (words-by-number after)
        facts (:facts after)
        entries (:by-value after)
        s (fn [n] (+ before-the-line 1 n))
        entry (fn [k since v e] (record/by-value-key :base k since (record/value-digest-of v) e))
        run-of (fn [k since v] (let [[from to] (record/by-value-run :base k since v)]
                                 (vec (sort (filter #(and (<= (compare from %) 0) (neg? (compare % to))) (keys entries))))))
        marks-row (get facts base-marks-place)]
    [["the word of each answer after the line, by the number of its act-id"
      (fn [] words)
      {40 :accepted 41 :accepted 42 :accepted 43 :accepted 44 :accepted 45 :accepted 46 :accepted 47 :accepted
       48 :accepted 49 :accepted 50 :accepted 51 :replaced-fact-not-current 52 :permission-does-not-allow-acting
       53 :permission-does-not-stand 54 :permission-does-not-allow-acting 55 :accepted 56 :accepted
       57 :store-key-retract-not-built 58 :setting-not-on-the-layer 59 :accepted 60 :replaced-fact-not-current
       61 :permission-does-not-allow-acting}]
     ["the facts by value after the line: of two acts on one place the later one's value alone; a value written again; no row for a retract, of a fact from before the mark or since it; the entry of a replaced fact gone and the new one there; the entry of a fact under the key that an act of the line marked, under that act's stamp; and none under the key whose mark an act of the line took off"
      (fn [] (set (keys entries)))
      #{(entry :on since :c2 :p1) (entry :on since :c0 :q1) (entry :on since :c2 :q2) (entry :on since :c7 :r2)
        (entry :in (s 5) :c1 :p2)}]
     ["a key of that order holds the proof's own SHA-256 of the value's text"
      (fn [] (entry :on since :c2 :p1))
      (str ":base :on " since " " (digest-of-text ":c2") " :p1")]
     ["two acts on one place under a marked key are both accepted, and the place holds the later one's value"
      (fn [] (let [row (get facts ":base :p1 :on")] [(words 40) (words 41) (record/read-value row) (:value row) (:by-act row)]))
      [:accepted :accepted [:value :c2] ":c2" (uuid 41)]]
     ["the same value again: the entry is written over with the new fact and its stamp"
      (fn [] (select-keys (get entries (entry :on since :c0 :q1)) [:by-act :stamp]))
      {:by-act (uuid 42) :stamp (s 2)}]
     ["two places that hold one value: their rows are neighbours in one run"
      (fn [] (run-of :on since :c2))
      [(entry :on since :c2 :p1) (entry :on since :c2 :q2)]]
     ["a retract of a fact from before the mark takes nothing out; a retract of a fact since the mark takes its entry out; each place holds a retract"
      (fn [] [(words 43) (record/read-value (get facts ":base :r1 :on")) (run-of :on since :from-before-the-mark)
              (words 55) (record/read-value (get facts ":base :q3 :on")) (run-of :on since :c5)])
      [:accepted [:nothing-here] [] :accepted [:nothing-here] []]]
     ["a fact under a marked key that names the fact that it replaces: the entry of the value before goes out and the new one comes in"
      (fn [] [(words 56) (run-of :on since :c6) (run-of :on since :c7)])
      [:accepted [] [(entry :on since :c7 :r2)]]]
     ["a mark holds from the next act: a write of the marks marks a new key, and the act after it writes a fact under that key. The key's stamp is the stamp of the answer of the act that marked it, the fact is in the order under that stamp, and the fact's own stamp is later"
      (fn [] [(words 45) (words 46) (:stamp (get-in after [:answers (record/answer-key :base (uuid 45))]))
              (run-of :in (s 5) :c1) (:stamp (get facts ":base :p2 :in"))])
      [:accepted :accepted (s 5) [(entry :in (s 5) :c1 :p2)] (s 6)]]
     ["two writes of the marks in one line: the row is the later one's; a key from before keeps its stamp, and each newly marked key has the stamp of the act that marked it"
      (fn [] [(words 47) (:by-act marks-row) (record/read-value marks-row) (record/marked-keys-of marks-row)])
      [:accepted (uuid 47) [:value #{:on :in :late}] {:on since :in (s 5) :late (s 7)}]]
     ["a fact under a key whose mark an act before it took off: its row is written, and it has no entry in the order"
      (fn [] [(words 59) (:value (get facts ":base :p3 :gone")) (run-of :gone since :c1)
              (count (filter #(str/starts-with? % ":base :gone ") (keys entries)))])
      [:accepted ":c1" [] 0]]
     ["a fact under a marked key that names a fact which is not current is refused whole: its value has no row in the order, and the entry of the place is the accepted act's"
      (fn [] [(words 60) (run-of :on since :c9) (:by-act (get entries (entry :on since :c0 :q1)))])
      [:replaced-fact-not-current [] (uuid 42)]]
     ["two writes of who may see the layer that name nothing are both accepted, and the row is the later one's"
      (fn [] [(words 48) (words 49) (record/read-value (get facts ":base :base :softland.store/who-may-see"))])
      [:accepted :accepted [:value :everyone]]]
     ["two writes of what the layer builds on that name the same fact to replace: the first is accepted, the second is stale, and the row is the first one's"
      (fn [] [(words 50) (words 51) (record/read-value (get facts ":base :base :softland.store/builds-on"))])
      [:accepted :replaced-fact-not-current [:value [:team]]]]
     ["a holder of see: her act is refused; naming her permission without the slot does not stand; her give is refused, so no permission is beneath a see"
      (fn [] [(mapv words [52 53 54]) (get facts ":base :dave :softland.store/permission")])
      [[:permission-does-not-allow-acting :permission-does-not-stand :permission-does-not-allow-acting] nil]]
     ["a holder of see who sends a value under a marked key: refused with the word under her act's own act-id, and no row of the value, in its place or in the order"
      (fn [] [(words 61) (run-of :on since :c1) (get facts ":base :carol :on")])
      [:permission-does-not-allow-acting [] nil]]
     ["a retract of the marks and marks on another entity are refused"
      (fn [] [(words 57) (words 58) (get facts ":base :p1 :softland.store/marked-keys")])
      [:store-key-retract-not-built :setting-not-on-the-layer nil]]]))

;; ----------------------------------------------------------- the door's words

(def ^:private door-words
  "The door's own seven words, written out a second time, each with its
  `:appended`: whether nothing was appended for the maker's act, or the door
  does not know."
  {:find-the-module       :nothing
   :make-an-act-id        :nothing
   :read-the-layers-depot :nothing
   :read-the-act          :nothing
   :add-its-parts         :nothing
   :append-cleanly        :not-known
   :read-the-answer       :nothing})

(def ^:private read-accepted
  "What the read by act-id gives for an accepted act whose digest is \"aa\"."
  {:verdict :accepted :reason nil :stamp 77 :time now :digest "aa"})

(def ^:private read-refused
  "What the read by act-id gives for a refused act whose digest is \"aa\"."
  {:verdict :refused :reason :layer-not-made :stamp 78 :time now :digest "aa"})

(def ^:private door-words-cases
  "The cases of the door's words: the table of the seven; that none is a
  gate's word; a word as a maker gets it; the four answers from what a read
  gave, and the time in them; the two acks."
  [["the door's seven words, each with whether nothing was appended for the act or the door does not know"
    (fn [] (into {} (map (fn [[w entry]] [w (:appended entry)])) words/words))
    door-words]
   ["each word names the calls that can give it: the three calls that send or make an act-id, the opening of a door, and the read of an answer"
    (fn [] (into {} (map (fn [[w entry]] [w (:from entry)])) words/words))
    {:find-the-module       #{:open}
     :make-an-act-id        #{:act-id-for :act-ids-for :send-act}
     :read-the-layers-depot #{:act-id-for :act-ids-for :send-act}
     :read-the-act          #{:send-act}
     :add-its-parts         #{:send-act}
     :append-cleanly        #{:send-act}
     :read-the-answer       #{:answer-of}}]
   ["what a word can carry, by name"
    (fn [] (set (keys words/carried)))
    #{:at :module :layer :act-id :digest :threw :said}]
   ["no word of the door's is a reason word of the act or of the declaration, or a word of a stray"
    (fn [] (filterv (set (concat act/reasons declaration/reasons declaration/stray-reasons)) (keys words/words)))
    []]
   ["a word as a maker gets it holds :could-not, its own :appended and what it carries, and no :answer and no :reason"
    (fn [] (words/word :append-cleanly {:act-id (act-id 1) :digest "aa" :answer :accepted :reason :layer-not-made :appended :nothing}))
    {:could-not :append-cleanly :appended :not-known :act-id (act-id 1) :digest "aa"}]
   ["a word that the table does not hold is not made: one of the four answers, a gate's reason word, nothing"
    (fn [] [(words/word :refused {:at :layer}) (words/word :layer-not-made nil) (words/word nil nil)])
    [nil nil nil]]
   ["the answer from a read of an accept with the digest given: accepted, with its stamp and the time on the act"
    (fn [] (words/answer read-accepted "aa"))
    {:answer :accepted :stamp 77 :time now}]
   ["the answer from a read of a refusal with the digest given: refused, with the gate's reason word, its stamp and the time on the thing"
    (fn [] (words/answer read-refused "aa"))
    {:answer :refused :reason :layer-not-made :stamp 78 :time now}]
   ["the answer from a read of a refusal of a thing that held no usable time: refused, with the word and the stamp, and no :time"
    (fn [] (words/answer (assoc read-refused :reason :no-door-time :time nil) "aa"))
    {:answer :refused :reason :no-door-time :stamp 78}]
   ["from an answer's row to the maker's answer, through the read's form: the stamp and the time of the row as they are, a time far ahead too; a row with no time gives no :time"
    (fn [] (let [digest (digest/digest good)
                 through (fn [row] (words/answer (record/answer-of row) (text/hex digest)))]
             [(through (record/answer-row nil 77 now digest))
              (through (record/answer-row nil 77 far-ahead digest))
              (through (record/answer-row :no-door-time 78 nil digest))]))
    [{:answer :accepted :stamp 77 :time now} {:answer :accepted :stamp 77 :time far-ahead}
     {:answer :refused :reason :no-door-time :stamp 78}]]
   ["the answer from a read that gave nothing: nothing yet"
    (fn [] (words/answer nil "aa"))
    {:answer :nothing-yet}]
   ["the answer from a read of an answer with another digest: not mine, and nothing more, for an accept and for a refusal"
    (fn [] [(words/answer read-accepted "bb") (words/answer read-refused "bb")])
    [{:answer :not-mine} {:answer :not-mine}]]
   ["a digest that is nil, or is no text, with an answer there: not mine"
    (fn [] [(words/answer read-accepted nil) (words/answer (assoc read-accepted :digest nil) nil) (words/answer read-accepted 7)])
    [{:answer :not-mine} {:answer :not-mine} {:answer :not-mine}]]
   ["a digest that is nil with no answer there: nothing yet"
    (fn [] (words/answer nil nil))
    {:answer :nothing-yet}]
   ["the two acks, by the depot word: the streaming gate ran, the microbatch depot holds the thing"
    (fn [] words/acks)
    {:streaming :gate-ran :microbatch :depot-holds}]])

;; ------------------------------------------------------- the door's additions

(def ^:private makers-act
  "Alice's act as a maker gives it, with no facts: the parts that the form
  asks for, plain."
  {:version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1})

(defn- maker
  "Alice's act as a maker gives it, with these facts as a vector."
  [& facts]
  (assoc makers-act :facts (vec facts)))

(def ^:private value-fact
  "A value fact as a maker gives it: a plain value under a plain key."
  {:e :note-1 :k :text :v "a text"})

(defn- said
  "Takes one of the door's words as a maker gets it. Gives what the cases
  hold of it: the word, its `:appended`, and the place that it carries."
  [word]
  (select-keys word [:could-not :appended :at]))

(defn- could-not
  "One of the door's words as `said` gives it, for what a case must give:
  the word with `:appended :nothing`, and its place when one is given."
  ([word] {:could-not word :appended :nothing})
  ([word at] (assoc (could-not word) :at at)))

(defn- built
  "What the door's pure parts make of a maker's thing, with no cluster:
  {:thing the thing to append}, under the act-id given or `(act-id 70)`,
  with the time `now` where the door hands the clock of its server; or the
  door's word."
  ([thing] (built thing nil))
  ([thing given]
   (let [reading (additions/reading thing given)]
     (if (:could-not reading)
       reading
       (additions/thing-to-append reading (or given (act-id 70)) now)))))

(defn- through
  "What comes of a maker's thing: the door's word, as `said` gives it; or
  [:goes-in word], where the word is what the declaration's check gives for
  the thing that the door would append, :ok or a gate's reason word. What
  must hold: the thing has an end, since the check reads it whole."
  ([thing] (through thing nil))
  ([thing given]
   (let [made (built thing given)]
     (if (:could-not made)
       (said made)
       [:goes-in (verdict (declaration/check (:thing made)))]))))

(defn- read-word
  "The door's word for a reading of a thing, as `said` gives it, or :read
  when the door read the thing."
  [thing given]
  (let [reading (additions/reading thing given)]
    (if (:could-not reading) (said reading) :read)))

(defn- numbered-fact
  "A value fact on the entity `n<i>`, whose value is i."
  [i]
  {:e (keyword (str "n" i)) :k :text :v i})

(defn- thrower
  "An object that throws when it is hashed, compared or printed."
  []
  (reify Object
    (hashCode [_] (throw (ex-info "this object throws when it is hashed" {})))
    (equals [_ _] (throw (ex-info "this object throws when it is compared" {})))
    (toString [_] (throw (ex-info "this object throws when it is printed" {})))))

(def ^:private not-an-act-cases
  "The cases of things given to the door that are no act, one kind of thing
  after the other. A thing that ends in one of the door's words gives the
  word. A thing that goes in gives the word that the declaration's check has
  for the thing that the door would append: the door reads the layer and no
  other part, so each fault of a fact, of a value or of a limit is a gate's
  to name."
  [["no act: no map: nil, a number, a string, a vector"
    (fn [] (mapv through [nil 7 "x" [1 2]]))
    (vec (repeat 4 (could-not :read-the-act)))]
   ["no act: a map with no :layer, or a nil one"
    (fn [] (mapv through [{:facts []} (assoc (maker value-fact) :layer nil)]))
    (vec (repeat 2 (could-not :make-an-act-id :layer)))]
   ["no act: a thing whose :layer cannot be read without a throw: a sorted map with string keys"
    (fn [] (through (sorted-map "layer" :alice "facts" [])))
    (could-not :read-the-act :layer)]
   ["no act: a record with a :layer"
    (fn [] (through (assoc (->Thing :note-1 :text) :layer :alice :facts [])))
    (could-not :read-the-act)]
   ["no act: a :layer that is no id: a string, a number, a keyword that does not read back"
    (fn [] (mapv (fn [layer] (through (assoc (maker value-fact) :layer layer))) ["notes" 7 (keyword "a b")]))
    (vec (repeat 3 (could-not :make-an-act-id :layer)))]
   ["no act: a :layer that prints to 257 characters; one of 256 is read"
    (fn [] [(read-word (assoc (maker) :layer (long-id 257)) nil) (read-word (assoc (maker) :layer (long-id 256)) nil)])
    [(could-not :make-an-act-id :layer) :read]]
   ["no act: an act that already carries :act-id, also a nil one"
    (fn [] [(through (assoc (maker value-fact) :act-id (act-id 1))) (through (assoc (maker value-fact) :act-id nil))])
    (vec (repeat 2 (could-not :add-its-parts :act-id)))]
   ["no act: an act that already carries :depot"
    (fn [] (through (assoc (maker value-fact) :depot :streaming)))
    (could-not :add-its-parts :depot)]
   ["no act: an act that already carries the part of the time, also a nil one"
    (fn [] [(through (assoc (maker value-fact) declaration/time-part now)) (through (assoc (maker value-fact) declaration/time-part nil))])
    (vec (repeat 2 (could-not :add-its-parts declaration/time-part)))]
   ["no act: :facts absent or nil: the thing goes in as given"
    (fn [] [(through (dissoc (maker) :facts)) (through (assoc (maker) :facts nil))
            (contains? (:thing (built (dissoc (maker) :facts))) :facts)])
    [[:goes-in :malformed] [:goes-in :malformed] false]]
   ["no act: :facts that is no sequence, one fact map, a set, a string: as given, the maker's own object"
    (fn [] [(mapv (fn [facts] (through (assoc (maker) :facts facts))) [value-fact #{value-fact} "facts"])
            (identical? value-fact (:facts (:thing (built (assoc (maker) :facts value-fact)))))])
    [(vec (repeat 3 [:goes-in :malformed])) true]]
   ["no act: 257 facts, each with a value: the thing goes in, and a gate has the word"
    (fn [] (through (assoc (maker) :facts (map numbered-fact (range 257)))))
    [:goes-in :too-many-facts]]
   ["no act: a fact that is no map, a string, a number, a vector: as given"
    (fn [] [(through (maker "x" 7 [1])) (:facts (:thing (built (maker "x" 7 [1]))))])
    [[:goes-in :malformed] ["x" 7 [1]]]]
   ["no act: a fact that is a record, with a value and with none: as given"
    (fn [] [(through (maker (assoc (->Thing :note-1 :text) :v "a text"))) (through (maker (->Thing :note-1 :text)))])
    [[:goes-in :malformed] [:goes-in :malformed]]]
   ["no act: a fact that carries :sealed and :lock-id and no :v: as given, the maker's own object, and a gate refuses the part"
    (fn [] (let [fact {:e :note-1 :k :text :sealed (bytes-of 3) :lock-id [(act-id 50) 0]}
                 act (maker fact)]
             [(through act) (identical? fact (first (:facts (:thing (built act)))))]))
    [[:goes-in :unknown-part] true]]
   ["no act: a fact with a value that also holds :sealed, or :lock-id, or a nil :sealed; under a plain key and under a store key"
    (fn [] [(through (maker (assoc value-fact :sealed (bytes-of 3))))
            (through (maker (assoc value-fact :lock-id [(act-id 50) 0])))
            (through (maker (assoc value-fact :sealed nil)))
            (through (maker {:e :alice :k :softland.store/depot :v :streaming :sealed (bytes-of 3)}))])
    (vec (repeat 4 [:goes-in :unknown-part]))]
   ["no act: a value that is no value of the form: a ratio, a list, a record, a number that is not finite, a value inside 33 collections"
    (fn [] (mapv (fn [v] (through (maker (assoc value-fact :v v))))
                 [1/3 (list 1 2) (->Thing 1 2) Double/NaN (nested 33)]))
    (vec (repeat 5 [:goes-in :malformed]))]
   ["no act: a fact with a value whose :e is no id, or whose :k is no key: as given"
    (fn [] (mapv (fn [fact] (through (maker fact)))
                 [{:e "n1" :k :text :v "a text"} {:e :note-1 :k "text" :v "a text"}]))
    [[:goes-in :malformed] [:goes-in :malformed]]]
   ["no act: a part that the form does not know, on the act and on a fact, and one named as the door's own words are: as given"
    (fn [] [(through (assoc (maker value-fact) :note "x"))
            (through (maker (assoc value-fact :note "x")))
            (:note (first (:facts (:thing (built (maker (assoc value-fact :note "x")))))))
            (through (assoc (maker value-fact) :could-not :read-the-act))
            (:could-not (:thing (built (assoc (maker value-fact) :could-not :read-the-act))))])
    [[:goes-in :unknown-part] [:goes-in :unknown-part] "x" [:goes-in :unknown-part] :read-the-act]]
   ["no act: 1,025 entries of :based-on"
    (fn [] (through (assoc (maker value-fact) :based-on (into {} (map (fn [i] [[(act-id i) 0] 1])) (range 1025)))))
    [:goes-in :too-many-based-on]]
   ["no act: an id of 300 characters that is not the layer"
    (fn [] (through (maker {:e (long-id 300) :k :text})))
    [:goes-in :id-too-long]]
   ["no act: a value whose text is 65,537 bytes goes in, and a gate has the word; one of 65,536 is taken"
    (fn [] [(through (maker (assoc value-fact :v (text-of-bytes 65537)))) (through (maker (assoc value-fact :v (text-of-bytes 65536))))])
    [[:goes-in :value-too-large] [:goes-in :ok]]]
   ["no act: a fact under :softland.store/lock-request, as given: a gate refuses the key"
    (fn [] (through (maker {:e (uuid 70) :k :softland.store/lock-request :v 4})))
    [:goes-in :store-key-not-known]]])

(def ^:private additions-cases
  "The cases of the door's additions beside the table: an act-id given
  back; the kinds of fact; the order of the tests; the thing that is built,
  with the three parts that the door adds, the time that it is handed among
  them; things that have no end or that throw; the count of a series. They
  do not reach the making of an act-id, the read of the clock, the append or
  the read of an answer, which need a door on a cluster: those are the
  door's proof's."
  (let [given (fn [depot] [:alice depot (uuid 9)])
        permission-fact {:e :bob :k :softland.store/permission :v bobs}
        retract {:e :note-2 :k :text}
        mixed (fn [facts] (assoc makers-act :facts facts))
        three [value-fact retract permission-fact]
        doors-parts [:act-id :depot declaration/time-part]
        hands-thing (fn [facts] (assoc makers-act :act-id (act-id 70) :depot :streaming declaration/time-part (+ now 60000)
                                       :facts facts))]
    [;; a reading
     ["a reading holds two parts: the maker's own map, and its layer"
      (fn [] (let [act (maker value-fact)
                   reading (additions/reading act nil)]
               [(sort (keys reading)) (identical? act (:thing reading)) (:layer reading)]))
      [[:layer :thing] true :alice]]

     ;; an act-id given back
     ["an act-id given back, of the act's layer: used as given"
      (fn [] (select-keys (:thing (built (maker value-fact) (given :streaming))) [:act-id :depot]))
      {:act-id (given :streaming) :depot :streaming}]
     ["an act-id given back, of another layer"
      (fn [] (read-word (maker value-fact) [:bob :streaming (uuid 9)]))
      (could-not :make-an-act-id :act-id)]
     ["an act-id given back that is not of the shape: two slots, a string, a depot slot that is no depot word, a word of the door's"
      (fn [] (mapv (fn [id] (read-word (maker value-fact) id))
                   [[:alice :streaming] "an act-id" [:alice :nowhere (uuid 9)] (could-not :make-an-act-id :layer)]))
      (vec (repeat 4 (could-not :make-an-act-id :act-id)))]
     ["an act-id given back with the other depot word: used as given, and the thing names that depot"
      (fn [] [(select-keys (:thing (built (maker value-fact) (given :microbatch))) [:act-id :depot])
              (declaration/stray-reason :microbatch (:thing (built (maker value-fact) (given :microbatch))))])
      [{:act-id (given :microbatch) :depot :microbatch} nil]]

     ;; the kinds of fact
     ["a fact under each of the five store keys is the maker's own object in the thing"
      (fn [] (let [facts [{:e :alice :k :softland.store/depot :v :streaming}
                          permission-fact
                          {:e :alice :k :softland.store/who-may-see :v :everyone}
                          {:e :alice :k :softland.store/builds-on :v [:base]}
                          {:e :alice :k :softland.store/marked-keys :v #{:on}}]
                   built-facts (:facts (:thing (built (mixed facts))))]
               [(count built-facts) (every? true? (map identical? facts built-facts))]))
      [5 true]]
     ["a fact with a value under a key in the store's namespace that is no store key: as given, and a gate refuses it"
      (fn [] (let [fact {:e :note-1 :k :softland.store/owner :v :alice}
                   act (maker fact)]
               [(identical? fact (first (:facts (:thing (built act))))) (through act)]))
      [true [:goes-in :store-key-not-known]]]
     ["a retract, a fact with no :v, and a fact with a nil :v: each as given"
      (fn [] (let [with-nil {:e :note-3 :k :text :v nil}
                   act (maker retract with-nil)
                   facts (:facts (:thing (built act)))]
               [(identical? retract (nth facts 0)) (identical? with-nil (nth facts 1)) (nth facts 1) (through act)]))
      [true true {:e :note-3 :k :text :v nil} [:goes-in :ok]]]
     ["one act with a store-key fact, a retract and a value: each fact is the maker's own object at its position, the fact with a value too, and the declaration's check takes the thing"
      (fn [] (let [facts [permission-fact retract value-fact]
                   act (mixed facts)]
               [(mapv identical? facts (:facts (:thing (built act)))) (through act)]))
      [[true true true] [:goes-in :ok]]]

     ;; the order of the tests
     ["two faults, the thing is no plain map and it carries :act-id: the first test gives the word"
      (fn [] (through (assoc (->Thing :note-1 :text) :layer :alice :act-id (act-id 1))))
      (could-not :read-the-act)]
     ["two faults, it carries :act-id and has no :layer: the door's own parts come before the layer"
      (fn [] (through {:act-id (act-id 1) :facts [value-fact]}))
      (could-not :add-its-parts :act-id)]
     ["two faults, it carries the part of the time and has no :layer: the same order"
      (fn [] (through {declaration/time-part now :facts [value-fact]}))
      (could-not :add-its-parts declaration/time-part)]
     ["a maker's thing that already holds the part of the time, also a nil one: the reading is the word :add-its-parts with nothing appended, and its place is the name that the declaration holds; the text of the word in the door's table names that part beside the two others"
      (fn [] (let [says (:says (get words/words :add-its-parts))
                   readings (mapv (fn [t] (additions/reading (assoc (maker value-fact) declaration/time-part t) nil)) [now nil])]
               [readings
                (mapv (fn [reading] (= declaration/time-part (:at reading))) readings)
                (mapv (fn [part] (str/includes? says (str part))) doors-parts)
                says]))
      [(vec (repeat 2 {:could-not :add-its-parts :appended :nothing :at declaration/time-part}))
       [true true] [true true true]
       "the thing already holds a part that the door adds, :act-id, :depot or :door-time on the act; the door writes over nothing"]]
     ["the door's own parts are tested in their order, the act-id, the depot, the time: a thing that holds more than one of them, each with nil, gets the word at the first"
      (fn [] (let [at (fn [& parts] (:at (additions/reading (reduce (fn [thing part] (assoc thing part nil)) (maker value-fact) parts) nil)))]
               [(at :act-id :depot declaration/time-part) (at :depot declaration/time-part) (at :act-id declaration/time-part)
                (at declaration/time-part)]))
      [:act-id :depot :act-id declaration/time-part]]
     ["two faults, a :layer that is no id and a value that is no value: the door reads no fact, so the layer gives the word"
      (fn [] (through (assoc (maker (assoc value-fact :v 1/3)) :layer "notes")))
      (could-not :make-an-act-id :layer)]

     ;; the thing that is built
     ["the thing holds :facts as the very object that the maker gave, for a vector, a list and a lazy sequence; the building does not walk the lazy sequence"
      (fn [] (let [walked (atom 0)
                   lazy (map (fn [fact] (swap! walked inc) fact) three)
                   same? (fn [facts] (identical? facts (:facts (:thing (built (mixed facts))))))]
               [(same? three) (same? (apply list three)) (same? lazy) @walked (realized? lazy)]))
      [true true true 0 false]]
     ["the thing is the maker's map with three parts more: each part that the maker gave is the maker's own object, a part that the form does not know too, and nothing is filled in"
      (fn [] (let [act (assoc (mixed three) :subjects #{:bob} :based-on {[(act-id 5) 0] 7} :note {:a 1})
                   thing (:thing (built act))]
               [(every? (fn [part] (identical? (get act part) (get thing part)))
                        [:version :who :layer :permission :session :subjects :based-on :note :facts])
                (sort (remove (set (keys act)) (keys thing)))
                (select-keys thing doors-parts)
                (sort (remove (set (keys (:thing (built (maker value-fact))))) [:based-on :because-of :claimed-when :subjects]))]))
      [true (sort doors-parts) {:act-id (act-id 70) :depot :streaming declaration/time-part now}
       [:based-on :because-of :claimed-when :subjects]]]
     ["the thing to append is the maker's map with the three parts added and nothing else changed: with the three taken off again it is equal to the maker's map; the depot is the depot slot of the act-id; the time is the number that the door hands, under the name that the declaration holds. No clock is read: two calls with one time give one thing, and a call with another time gives a thing that differs in that part alone"
      (fn [] (let [act (assoc (mixed three) :subjects #{:bob} :note {:a 1})
                   reading (additions/reading act nil)
                   made (additions/thing-to-append reading (given :microbatch) 1234)
                   thing-at (fn [t] (:thing (additions/thing-to-append reading (given :microbatch) t)))]
               [(sort (keys made))
                (= act (apply dissoc (:thing made) doors-parts))
                (select-keys (:thing made) doors-parts)
                (= (:thing made) (thing-at 1234))
                (= (without-time (:thing made)) (without-time (thing-at 1235)))
                (get (thing-at 1235) declaration/time-part)]))
      [[:thing] true {:act-id (given :microbatch) :depot :microbatch declaration/time-part 1234} true true 1235]]
     ["the time is added as the door hands it and is not tested here: a nil, a string and a time of -1 are in the thing as given, and it is the declaration's check that refuses each; a time far ahead, 2^50 ms, goes in"
      (fn [] (let [reading (additions/reading (maker value-fact) nil)]
               (mapv (fn [t] (let [thing (:thing (additions/thing-to-append reading (act-id 70) t))]
                               [(contains? thing declaration/time-part) (= t (get thing declaration/time-part))
                                (verdict (declaration/check thing))]))
                     [nil "a time" -1 far-ahead])))
      [[true true :no-door-time] [true true :no-door-time] [true true :no-door-time] [true true :ok]]]
     ["a value is in the thing as its maker gave it and not in its normal form; the declaration's check takes the thing, and its one form holds the text of the normal form"
      (fn [] (let [v (array-map :b 1.50M :a [1 2N])
                   thing (:thing (built (mixed [(assoc value-fact :v v) retract permission-fact])))
                   checked (declaration/check thing)]
               [(identical? v (:v (first (:facts thing)))) (pr-str (:v (first (:facts thing))))
                (verdict checked) (:text (first (:facts (:ok checked))))]))
      [true "{:b 1.50M, :a [1 2N]}" :ok "{:a [1 2] :b 1.5M}"]]
     ["a thing that the door reads is no stray on the depot that its act-id names; a record that carries a right act-id is one, and the door does not read it"
      (fn [] [(mapv (fn [[thing id]] (declaration/stray-reason (nth id 1) (:thing (built thing id))))
                    [[(maker value-fact) (given :streaming)]
                     [(maker value-fact) (given :microbatch)]
                     [(dissoc (maker) :facts) (given :streaming)]
                     [(assoc (maker value-fact) :note "x") (given :streaming)]])
              (declaration/stray-reason :streaming (assoc (->Thing :note-1 :text) :layer :alice :act-id (given :streaming)))
              (read-word (assoc (->Thing :note-1 :text) :layer :alice) (given :streaming))])
      [[nil nil nil nil] :no-act-id (could-not :read-the-act)]]
     ["the digest of the thing that the door builds follows the form of the facts as the maker gave them: a list and a lazy sequence have one digest, a vector has another, and each is the digest of the same thing as a hand would send it a minute later, since the time is no part of the digest"
      (fn [] (let [of (fn [facts] (digest/hex-of (:thing (built (mixed facts)))))
                   as-list (of (apply list three))
                   as-lazy (of (map identity three))
                   as-vector (of three)]
               [(= as-list as-lazy) (= as-list as-vector)
                (= as-vector (digest/hex-of (hands-thing three)))
                (= as-list (digest/hex-of (hands-thing (apply list three))))
                (= as-vector (digest/readable-hex-of (:thing (built (mixed three)))))]))
      [true false true true true]]

     ;; things that have no end, and things that throw
     ["the door comes back for a thing whose facts have no end, for a who that throws when it is hashed and for a permission that throws when it is read: it reads none of them, and builds the thing with each as given"
      (fn [] (let [endless (assoc (maker) :facts (map numbered-fact (range)))
                   who (thrower)
                   odd-who (assoc (maker value-fact) :who who)
                   odd-permission (assoc (maker value-fact) :permission (throwing))]
               [(read-word endless nil) (read-word odd-who nil) (read-word odd-permission nil)
                (identical? (:facts endless) (:facts (:thing (built endless))))
                (identical? who (:who (:thing (built odd-who))))
                (sort (remove (set (keys endless)) (keys (:thing (built endless)))))]))
      [:read :read :read true true (sort doors-parts)]]
     ["nothing throws while the three parts are added: a reading whose thing is a sorted map with string keys, which takes no keyword's part, and an act-id that is no vector, each end in the word :read-the-act"
      (fn [] [(said (additions/thing-to-append {:thing (sorted-map "layer" :alice) :layer :alice} (act-id 70) now))
              (said (additions/thing-to-append (additions/reading (maker value-fact) nil) 5 now))])
      [(could-not :read-the-act) (could-not :read-the-act)]]
     ["the digest that the door takes tells a thing that throws while it is read: nothing for a thing whose facts throw, where the plain digest is that of #unreadable"
      (fn [] (let [thing (:thing (built (assoc (maker) :facts (throwing))))]
               [(digest/readable-hex-of thing) (= (digest/hex-of thing) (sha-256 (utf8 "#unreadable")))]))
      [nil true]]
     ["nothing throws for facts that throw when they are read, or for a fact that throws: the door reads no fact, the thing goes in, and the declaration's check gives a word"
      (fn [] [(through (assoc (maker) :facts (throwing)))
              (through (assoc (maker) :facts (concat [value-fact retract] (throwing))))
              (first (through (maker value-fact (sorted-map "e" :note-1 "v" 1))))])
      [[:goes-in :malformed] [:goes-in :malformed] :goes-in]]
     ["nothing throws for a thing that is anything at all"
      (fn [] (mapv (fn [x] (:could-not (additions/reading x x))) [(throwing) (Object.) 'x {:layer (throwing)}]))
      [:read-the-act :read-the-act :read-the-act :make-an-act-id]]

     ;; the count of a series
     ["the count of a series of act-ids: a fixed-width integer of 1 or more"
      (fn [] (mapv additions/series-word [1 20 (int 3)]))
      [nil nil nil]]
     ["a count that is no fixed-width integer of 1 or more, a big integer among them: no act-id is made"
      (fn [] (mapv (fn [n] (said (additions/series-word n))) [0 -1 1.5 "3" nil 3N]))
      (vec (repeat 6 (could-not :make-an-act-id :count)))]]))

;; ------------------------------------------------------------------- the wait

(def ^:private wait-cases
  "The cases of the numbers of a maker's wait for an answer: the door's one
  number with its default, what an opener may give for it, and the longest
  wait that the door counts. They do not reach the wait itself, with its
  clock, its sleeps and its reads: that is the door's proof's."
  [["the door's one number, with its default, written out a second time: what an opener who gives nothing gets"
    (fn [] [wait/default-numbers (wait/numbers nil) (wait/numbers {})])
    [{:read-every-ms 200} {:read-every-ms 200} {:read-every-ms 200}]]
   ["an opener's number is taken when it is a fixed-width integer of 1 or more, and is held as a long"
    (fn [] (let [given (mapv (fn [n] (wait/numbers {:read-every-ms n})) [1 50 (int 20) (short 7)])]
             [given (mapv (fn [numbers] (.getSimpleName (class (:read-every-ms numbers)))) given)]))
    [[{:read-every-ms 1} {:read-every-ms 50} {:read-every-ms 20} {:read-every-ms 7}]
     ["Long" "Long" "Long" "Long"]]]
   ["a number that is below 1, or is no fixed-width integer, takes the default: zero, a negative, a number with a fraction, a string, a big integer, nil"
    (fn [] (mapv (fn [n] (wait/numbers {:read-every-ms n})) [0 -5 2.5 "64" 3N nil]))
    (vec (repeat 6 {:read-every-ms 200}))]
   ["what an opener gave that is no map, and a map that throws when it is read, give the default, and nothing throws"
    (fn [] (mapv wait/numbers [7 "numbers" [200] (sorted-map "a" 1) (throwing)]))
    (vec (repeat 5 {:read-every-ms 200}))]
   ["any other name in what an opener gave is not read: the numbers hold the one name"
    (fn [] (wait/numbers {:read-every-ms 400 :no-number-of-the-door 400 :another-name 2 :read-every 16 :other 1}))
    {:read-every-ms 400}]
   ["a number that the opener gives is cut to the longest wait that the door counts, 2,305,843,009,213 ms; that many milliseconds, as nanoseconds and four times over, stay in the range of a long, and one more does not; it is longer than 70 years"
    (fn [] [(wait/numbers {:read-every-ms Long/MAX_VALUE})
            (wait/numbers {:read-every-ms 2305843009214})
            (wait/numbers {:read-every-ms 2305843009212})
            wait/longest-wait-ms
            (<= (* 4N 1000000 wait/longest-wait-ms) Long/MAX_VALUE)
            (<= (* 4N 1000000 (inc wait/longest-wait-ms)) Long/MAX_VALUE)
            (< (* 1000 60 60 24 365 70) wait/longest-wait-ms)])
    [{:read-every-ms 2305843009213} {:read-every-ms 2305843009213} {:read-every-ms 2305843009212}
     2305843009213 true false true]]])

;; ------------------------------------------------------ no code names a lock

(defn- names-a-lock?
  "Takes a symbol or a keyword. Gives true when a word of its name, or of
  its namespace, begins with lock, seal or stock, or with unlock or unseal.
  The name is cut into words at each character that is no letter, and
  before each capital that follows a small letter, and the words are held
  in small letters. So `clock` and `block` name no lock, and `lock-key`,
  `:sealed`, `stock/take` and `ReentrantLock` each name one. `locking`,
  Clojure's own, is the one name that is let through."
  [x]
  (let [whole (str (when-let [n (namespace x)] (str n "/")) (name x))
        parts (map str/lower-case (str/split (str/replace whole #"([a-z])([A-Z])" "$1 $2") #"[^A-Za-z]+"))]
    (boolean (and (not= 'locking x)
                  (some (fn [part] (some #(str/starts-with? part %) ["lock" "seal" "stock" "unlock" "unseal"])) parts)))))

(defn- names-in
  "Takes a form as the reader gives it. Gives each symbol and each keyword
  that it holds, wherever it sits: in a collection, as the key or the value
  of a map, and in the metadata of a part, where a type hint is. A string
  is no name, so a docstring gives none, and a comment is not read."
  [form]
  (filter (fn [x] (or (symbol? x) (keyword? x)))
          (tree-seq (fn [x] (or (coll? x) (some? (meta x))))
                    (fn [x] (concat (when (coll? x) (seq x)) (when-let [m (meta x)] [m])))
                    form)))

(defn- forms-of
  "Takes a source file. Gives its top-level forms, read as data and not
  loaded: no code of the file runs. A tag that the reader does not know is
  kept as a tagged literal."
  [file]
  (binding [*read-eval* false
            *default-data-reader-fn* tagged-literal]
    (with-open [reader (java.io.PushbackReader. (io/reader file))]
      (let [end (Object.)]
        (loop [forms []]
          (let [form (read {:eof end :read-cond :preserve} reader)]
            (if (identical? end form)
              forms
              (recur (conj forms form)))))))))

(defn- clj-files-of
  "The `.clj` files of one directory of the classpath: the directory that
  holds the resource named, which sits two folders deep in it. Gives each
  file under it as [its path from that directory, the file], in the order
  of the paths."
  [resource-name]
  (let [root (-> (io/resource resource-name) io/file .getParentFile .getParentFile .getParentFile)
        from-root (fn [^java.io.File f] (str (.relativize (.toPath ^java.io.File root) (.toPath f))))]
    (->> (file-seq root)
         (filter (fn [^java.io.File f] (and (.isFile f) (str/ends-with? (.getName f) ".clj"))))
         (map (fn [f] [(from-root f) f]))
         (sort-by first))))

(defn- source-files
  "The `.clj` files of the store's source: each one under the directory of
  the classpath that holds `softland/store/act.clj` (`clj-files-of`). The
  proofs are in another directory and are not among them."
  []
  (clj-files-of "softland/store/act.clj"))

(defn- proof-files
  "The `.clj` files of the proofs: each one under the directory of the
  classpath that holds this proof (`clj-files-of`). The source is in another
  directory and is not among them."
  []
  (clj-files-of "softland/store/pure_proof.clj"))

(defn- lock-names-in
  "Reads each of the files given as data. Takes files as `clj-files-of`
  gives them, and a symbol that one of them must hold as a name. Gives
  {:files how many were read, :names how many symbols and keywords they
  hold, :saw? whether that symbol was among the names, :found a vector of
  [file name] for each name that names a lock, and of [file :not-read
  class] for a file that the reader could not read}."
  [clj-files must-hold]
  (let [read-one (fn [[path file]]
                   (try {:file path :names (vec (mapcat names-in (forms-of file)))}
                        (catch Throwable t {:file path :not-read (.getSimpleName (class t))})))
        files (mapv read-one clj-files)]
    {:files (count files)
     :names (reduce + (map (comp count :names) files))
     :saw? (boolean (some (fn [{:keys [names]}] (some #{must-hold} names)) files))
     :found (vec (distinct
                  (mapcat (fn [{:keys [file names not-read]}]
                            (if not-read
                              [[file :not-read not-read]]
                              (map (fn [x] [file x]) (filter names-a-lock? names))))
                          files)))}))

(def ^:private names-that-the-proofs-mean
  "The names in the code of the proofs that name a lock, a seal or a stock
  and are meant, by where they may stand. The store keeps no lock, and a
  check that it keeps none must name what it looks for.

    :in-any-proof   the two parts of a sealed fact and the key of a request
                    for locks, in the store's namespace, bare and in a
                    tool's: a proof sends them so that a gate refuses them
                    or takes the word as a tool's own key. The entities of
                    those facts. And the names of the checks, and of their
                    helpers, that hold that the store keeps no lock
    :in-this-proof  the names that `lock-name-cases` writes out as examples
                    of a name that names a lock, in this file alone

  Any other such name in the code of a proof is a check of a lock or of the
  seal that stayed behind what it checked."
  {:in-any-proof '#{:sealed :lock-id :softland.store/lock-request :lock-request :tool/lock-request
                    :n-sealed :no-lock-0 :no-lock-1 :no-lock-2 :no-lock-3
                    the-module-keeps-no-lock no-code-of-the-store-names-a-lock
                    names-a-lock? lock-names-in lock-name-cases}
   :in-this-proof '#{lock-key seal/open stock/take-locks softland.store.door.seal softland.store.door.stock
                     $$streaming-locks locks-of-request java.util.concurrent.locks.ReentrantLock ReentrantLock
                     unsealed max-sealed-bytes with-locks-read *lock-rows Lock stock}})

(defn- not-meant
  "Takes what `lock-names-in` found in the proofs, each [file name] or
  [file :not-read class]. Gives those that `names-that-the-proofs-mean`
  does not hold for their file: an empty vector when each name that names a
  lock is meant where it stands, and each file was read."
  [found]
  (vec (remove (fn [[file x]]
                 (or (contains? (:in-any-proof names-that-the-proofs-mean) x)
                     (and (= "softland/store/pure_proof.clj" file)
                          (contains? (:in-this-proof names-that-the-proofs-mean) x))))
               found)))

(def ^:private lock-name-cases
  "The cases of the test of a name and of the walk of a form, on names and
  forms that the proof writes out: so that the reading of the source can
  find a lock's name if there is one. The reading does not reach a string or
  a comment, a name that code makes while it runs, or a file that is no
  `.clj` file. So of a proof it reaches the names in its code and not the
  words of a check's text; and the pages are not read."
  [["names that name a lock: a lock's key, the seal's parts, a stock, the store key of a request, a table of locks, a Java lock, a fact's two parts"
    (fn [] (mapv names-a-lock? ['lock-key 'seal/open 'stock/take-locks 'softland.store.door.seal 'softland.store.door.stock
                                :softland.store/lock-request '$$streaming-locks 'locks-of-request
                                'java.util.concurrent.locks.ReentrantLock 'ReentrantLock :lock-id :sealed 'unsealed 'max-sealed-bytes
                                'with-locks-read '*lock-rows 'Lock]))
    (vec (repeat 17 true))]
   ["names that name none: a clock, a block, Clojure's locking, a deadline, the last stamp, a sale"
    (fn [] (mapv names-a-lock? ['server-clock '$$a-clock :clock 'the-clock 'block 'locking 'java.time.Clock
                                'deadline :last-stamp 'sale 'softland.store.door/server-clock]))
    (vec (repeat 11 false))]
   ["the walk of a form finds a name in a list, in a vector, as a key and as a value of a map, in a set and in a type hint; a string and a number are no names"
    (fn [] (set (names-in (read-string "(defn f \"a lock in a docstring\" [^Lock x] {:a lock-key, stock [#{:sealed} 7 \"seal\"]})"))))
    #{'defn 'f 'x :tag 'Lock :a 'lock-key 'stock :sealed}]
   ["of that form, the names that name a lock"
    (fn [] (set (filter names-a-lock? (names-in (read-string "(defn f \"a lock in a docstring\" [^Lock x] {:a lock-key, stock [#{:sealed} 7 \"seal\"]})")))))
    #{'Lock 'lock-key 'stock :sealed}]])

;; ---------------------------------------------------------------- the checks

(deftest the-one-text-and-the-digest
  (holds text-cases)
  (holds digest-cases)
  (shown "the one text of" (count text-rows) "kinds of thing:"
         (str/join " | " (map (fn [[what x _]] (str what " " (text/text x))) text-rows)))
  (shown "one text and one digest for" (count same-text-rows) "pairs that no check can tell apart:"
         (str/join "; " (map first same-text-rows)))
  (shown "two texts and two digests for" (count two-text-rows) "pairs that a check tells apart:"
         (str/join "; " (map first two-text-rows)))
  (shown "the text of each of" (count (filter some? edn-values)) "values that a fact can hold is EDN and reads back as the value")
  (shown "the count of the UTF-8 bytes of a string, with no array made:"
         (pr-str (mapv text/utf8-count ["" "a" "é" "✓" "𝄞"])) "for no character and for a character of one, two, three and four bytes")
  (shown "the digest is SHA-256 over the one text of the thing without the time on the act, 32 bytes; bytes are fed as they are; the digest of the least act is"
         (digest/hex-of good) "| the same act built a second time has the digest"
         (digest/hex-of (arrived 1 (value-at :note-1 :text)))
         "| the digest that tells a throw, for a thing whose facts throw:" (pr-str (digest/readable-hex-of {:facts (throwing)})))
  (shown "the digest leaves the time on the act out, the part" declaration/time-part "at the top level of a plain map: the least act with the time" now
         "has the digest above, and so has it with the time" (inc now) "and with none:"
         (pr-str (mapv (fn [x] (subs (digest/hex-of x) 0 12)) [good (at-time (inc now)) (without-time good)]))
         "| with another value:" (pr-str (subs (digest/hex-of (arrived 1 (value-at :note-1 :text "a text."))) 0 12))
         "| a time inside a value is not left out, two times there give:"
         (pr-str (mapv (fn [t] (subs (digest/hex-of (arrived 1 (value-at :note-1 :text {declaration/time-part t}))) 0 12)) [1 2]))
         "| a sorted map with string keys keeps the digest of its own text, two with different content:"
         (pr-str (mapv (fn [v] (subs (digest/hex-of (sorted-map "a" v (name declaration/time-part) 5)) 0 12)) [1 2]))
         "and #unreadable has" (pr-str (subs (sha-256 (utf8 "#unreadable")) 0 12))
         "| a record with a field of that name keeps the digest of a record:" (pr-str (subs (digest/hex-of (->Timed 5 :note-1)) 0 12))))

(deftest the-declaration
  (holds declaration-parts-cases)
  (holds stray-cases)
  (holds passing-cases)
  (shown "the door's declaration: act.clj's" (count act/shapes) "shapes as act.clj declares them, and"
         (count declared-shapes) "more:" (pr-str (sort (keys declared-shapes))))
  (shown "its" (count declaration/rules) "rules, in order:" (pr-str (mapv first declaration/rules)))
  (shown "the" (count declaration/store-keys) "store keys:" (pr-str (sort (keys declaration/store-keys)))
         "the settings, which a gate takes only on the layer's own entity:" (pr-str (sort declaration/setting-keys))
         "the keys that take a second write:" (pr-str (sort declaration/second-write-keys))
         "the limits:" (pr-str {:facts declaration/max-facts :based-on declaration/max-based-on
                                :value-bytes declaration/max-value-bytes :act-bytes declaration/max-act-bytes
                                :id-text declaration/max-id-text :builds-on declaration/max-builds-on
                                :marked-keys declaration/max-marked-keys}))
  (shown "the three keys that take a second write, each with a right value and a wrong one:"
         (pr-str (mapv (fn [[k v]] [k v (verdict (declaration/check (arrived 1 {:e :alice :k k :v v})))])
                       [[:softland.store/who-may-see :everyone] [:softland.store/who-may-see :owner]
                        [:softland.store/builds-on [:base]] [:softland.store/builds-on #{:base}]
                        [:softland.store/marked-keys #{:on}] [:softland.store/marked-keys #{:softland.store/permission}]])))
  (shown "the store's namespace is" declaration/store-namespace
         "| a fact with a value under a key in it that the store does not know:"
         (pr-str (mapv (juxt identity valued-under) [:softland.store/owner :softland.store/class :softland.store/lock-request]))
         "| a tool's own fact with a value under a bare word or under another namespace is a fact under a plain key:"
         (pr-str (mapv (juxt identity valued-under) tools-own-keys)))
  (shown "a stray, by word:"
         (str/join " | " (map (fn [[what _ word]] (str what " " (or word "no stray"))) stray-rows)))
  (shown "the one form on arrival of the least thing:" (pr-str (act-of good)))
  (shown "the one form on arrival of a thing with a value that is not in its normal form, a retract and a permission given: each fact as [:k :v :text]"
         (pr-str (mapv (juxt :k :v :text) (:facts (act-of whole))))))

(deftest refusals-of-the-declaration
  (let [times (map (fn [[what x _ word]] [what x word]) time-rows)
        rows (concat makers-rows doors-rows)
        n (+ (holds (verdicts rows))
             (holds (verdicts times))
             (holds (verdicts (map (fn [[what x]] [what x :id-too-long]) id-rows)))
             (holds (verdicts limit-rows)))
        earned (into #{} (map #(nth % 2)) (concat rows times limit-rows [[nil nil :id-too-long]]))]
    (is (= declaration/reasons (disj earned :ok)) "each word of the declaration comes from a thing that earns it")
    (shown "the check gives the word that the proof holds for" n "things, each with one fault, or at the edge of a limit or of the time's range. act.clj's words, by the maker's own faults:" (words makers-rows))
    (shown "the door's words:" (words doors-rows))
    (shown "the time on the act, the least thing with each kind of time:" (words times))
    (shown "an id that prints to 257 characters is refused :id-too-long in each place:" (str/join "; " (map first id-rows)))
    (shown "the limits at their edges:" (words limit-rows))
    (shown "each of the" (count declaration/reasons) "reason words comes from a thing that earns it")))

(deftest the-maker's-check-inside-the-door's
  (let [doors-parts [:act-id :depot declaration/time-part]
        makers-part (fn [thing] (apply dissoc thing doors-parts))
        rows (filter (fn [[_ x _]] (:refuse (act/parse (makers-part x)))) makers-rows)]
    (doseq [[what x _] rows]
      (is (= (:refuse (act/parse (makers-part x))) (verdict (declaration/check x)))
          (str "the door's check gives act.clj's own word for: " what)))
    (is (= (count makers-rows) (count rows)) "each of the maker's faults is refused by act.clj's own parse")
    (is (= {:ok (apply dissoc (one-form 1 {:e :note-1 :k :text :v "a text" :replaces nil}) doors-parts)}
           (act/parse (makers-part good)))
        "the maker's part of the least thing, which is the thing without its act-id, its depot and its time, is an act that act.clj's own parse takes")
    (is (= [:unknown-part :unknown-part :unknown-part]
           (mapv (fn [part] (:refuse (act/parse (assoc (makers-part good) part (get good part))))) doors-parts))
        "each of the three parts that the door adds is no part of the maker's act: act.clj's own parse refuses the maker's part with any one of them put back")
    (shown "for" (count rows) "faults of the maker's form, the door's check gives the word that act.clj's own parse gives for the maker's part, which is the thing without"
           (str/join ", " doors-parts))))

(deftest the-order-of-the-reasons
  (let [n (holds (verdicts order-rows))]
    (shown "the order of the declarations gives the reason," n "things with two faults:" (words order-rows))))

(deftest the-time-on-the-act
  (let [n (holds time-cases)]
    (shown "the time on the act," n "cases. Its part is" declaration/time-part "and a usable time is a fixed-width integer of 0 or more, with no bound above"
           "| the usable time of the least thing, by its time:"
           (str/join " | " (map (fn [[what x _ _]] (str what " " (pr-str (declaration/usable-time x)))) time-rows)))
    (shown "the usable time and the check's word at the edge of the range and far above it, as [time usable-time word]:"
           (pr-str (mapv (fn [t] [t (declaration/usable-time (at-time t)) (verdict (declaration/check (at-time t)))])
                         [-1 0 far-ahead Long/MAX_VALUE]))
           "| a thing that is no plain map, and a sorted map with string keys, which throws when the part is read:"
           (pr-str (mapv declaration/usable-time [nil 5 (->Timed now :note-1) (sorted-map "a" 1 (name declaration/time-part) now)])))
    (shown "where the time's word stands: a thing with no time and"
           (str/join " | " (map (fn [[what fault]] (str what " " (verdict (declaration/check (fault (without-time good))))))
                                [["a part that the form does not know" #(assoc % :owner 1)]
                                 ["a depot that is no depot word" #(assoc % :depot :fast)]
                                 ["a depot word that is not the act-id's" #(assoc % :depot :microbatch)]
                                 ["a wrong version" #(assoc % :version 2)]
                                 ["no permission" #(dissoc % :permission)]
                                 ["no fact" #(assoc % :facts [])]]))
           "| the test for a stray reads no time: the least thing with no time is"
           (pr-str (or (declaration/stray-reason :streaming (without-time good)) "no stray")))))

(deftest the-bounds
  (let [n (holds bound-cases)]
    (shown "the two bounds of the check's last step," n "cases. One value: a text of" declaration/max-value-bytes
           "bytes at most, counted in UTF-8, under a plain key; the proof's own count of the two values at the edge is"
           (pr-str [(alength ^bytes (utf8 (pr-str at-the-bound))) (alength ^bytes (utf8 (pr-str over-the-bound)))])
           "bytes, in" (pr-str [(count (pr-str at-the-bound)) (count (pr-str over-the-bound))]) "characters, and the check gives"
           (pr-str (mapv (fn [v] (verdict (declaration/check (arrived 1 (value-at :note-1 :text v))))) [at-the-bound over-the-bound])))
    (shown "one act:" declaration/max-act-bytes "bytes of text at most in the rows of its facts, the text of each value and, for each fact, of who and of the permission; the proof's own count of the two acts at the edge is"
           (pr-str [(bytes-in-rows (full-act 0)) (bytes-in-rows (full-act 1))]) "and the check gives"
           (pr-str (mapv (fn [more] (verdict (declaration/check (full-act more)))) [0 1]))
           "| a value over its bound in an act over its bound:"
           (pr-str (verdict (declaration/check (assoc-in (full-act 1) [:facts 0 :v] over-the-bound)))))))

(deftest nothing-throws
  (holds (verdicts not-acts))
  (holds read-predicate-cases)
  (shown "nothing throws. The check refuses" (count not-acts) "things that are no acts:" (words not-acts)))

(deftest the-stamp
  (let [n (holds stamp-cases)]
    (shown "the stamp," n "cases: the layer's count of decisions, 0 for the first and the last stamp plus one after it, a long; it takes the last stamp alone, so no time and nothing of the act is in it")
    (shown "the stamp after each last stamp, as [last stamp, stamp]:"
           (pr-str (mapv (juxt identity stamp/next-stamp) [nil 0 1 41 (dec Long/MAX_VALUE) Long/MAX_VALUE])))))

(deftest the-first-act
  (holds first-act-cases)
  (shown "the form of a making act:"
         (str/join " | " (map (fn [[what gate-depot _ expected]] (str what " [" (name gate-depot) " gate] " expected)) first-act-rows)))
  (shown "a layer that starts with the other gate's depot, as [gate's depot, layer, the answer]:"
         (pr-str (vec (for [gate-depot [:streaming :microbatch]
                            layer [first-act/base :alice :team (uuid 1)]]
                        [gate-depot layer (first-act/starts-with-the-other-gates-depot? gate-depot layer)])))))

(deftest the-records-rows
  (let [n (holds record-cases)
        value-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} (uuid 5) 2 900 now)]
    (shown "the record's rows," n "cases: the keys" (pr-str [(record/answer-key :alice (uuid 5)) (record/place-key :alice :note-1 :text)])
           "; each field of a row of its declared class; a retract is a row with no value; every value is kept plain and as a String, the one text of the value in its normal form, which the row takes from the fact as it is; each row of a fact holds who wrote it and the permission that the act named; the heads of the two reads give nothing for what is no id or no act-id")
    (shown "the routing id of an act-id is its layer, for an act-id of each depot:"
           (pr-str (mapv (juxt identity record/routing-id) [[:alice :streaming (uuid 5)] [:base :microbatch (uuid 5)]]))
           "| the head of the read by act-id with that routing id, and with the act-id's uuid in its place:"
           (pr-str (mapv (fn [given] (record/answer-read-by-routing-id given [:base :microbatch (uuid 5)])) [:base (uuid 5)]))
           "| an act-id's layer and uuid come back from the text of its answer's key, for a layer that is a keyword and one that is a UUID:"
           (pr-str (mapv (fn [layer] (let [[layer-text act-uuid] (record/answer-key-parts (record/answer-key layer (uuid 5)))]
                                       [(record/id-of-text layer-text) act-uuid]))
                         [:alice (uuid 3)])))
    (shown "the row of a fact whose text is \"a text\" in its quotes:" (pr-str value-row)
           "| its plain bytes, read as UTF-8:" (pr-str (String. ^bytes (record/plain-bytes-of value-row) StandardCharsets/UTF_8)))
    (shown "what a row says of its value, for a reader:"
           (pr-str (mapv record/read-value [nil {:stamp 1} {:value ":canvas-1" :stamp 1} {:value "(1 2)" :stamp 1}
                                            {:value "\u0000\"a\"" :stamp 1}])))
    (shown "the facts by value: a key is" (pr-str (record/by-value-key :alice :on 100 (record/value-digest-of :c1) :b1))
           "| the run of the value :c1 under the key :on since the stamp 100 is" (pr-str (record/by-value-run :alice :on 100 :c1))
           "| a keyword, a UUID, a string with spaces, a number, a map and a value whose text is 65,536 bytes each have one digest of 64 hex characters, and no run holds a key of another value, layer, key or stamp")))

(deftest the-decision
  (holds early-cases)
  (holds standing-cases)
  (holds base-standing-cases)
  (holds other-gate-cases)
  (holds places-cases)
  (holds writes-cases)
  (holds by-value-cases)
  (shown "the time on the thing is a label: a decision writes it in the answer beside the stamp, here the stamp 42 after a last stamp of 41, and a thing with no usable time has none. As [what, the time, the check's word, the answer's row without its digest]:"
         (pr-str (mapv (fn [[what thing]]
                         (let [{act :ok word :refuse} (declaration/check thing)
                               time-millis (decision/time-on thing)
                               [[_ _ answer]] (filter (fn [[which]] (= :answer which))
                                                      (decision/writes (:act-id thing) act word (stamp/next-stamp 41) time-millis
                                                                       (digest/digest thing) {}))]
                           [what time-millis (or word :ok) (dissoc answer :digest)]))
                       [["the least thing" good]
                        ["a wrong version" (assoc good :version 2)]
                        ["no time" (without-time good)]
                        ["a time below zero" (at-time -1)]
                        ["a time far ahead" (at-time far-ahead)]])))
  (shown "the standing of an act, at the streaming gate's depot:"
         (str/join " | " (map (fn [[what _ _ _ word makes?]] (str what " " (or word (if makes? "makes the layer" "stands")))) standing-rows)))
  (shown "the standing of an act into the base, at the microbatch gate's depot:"
         (str/join " | " (map (fn [[what _ _ _ word makes?]] (str what " " (or word (if makes? "makes the layer" "stands")))) base-standing-rows)))
  (shown "a layer of the other gate, from the gate's depot and the act alone, with no row of the layer, as [the gate's depot, what arrives, the word or what the act does]:"
         (pr-str (mapv (fn [[what gate thing _ _ _ _ _]]
                         (let [act (act-of thing)]
                           [gate what (or (decision/standing gate act nil nil)
                                          (if (decision/makes-the-layer? gate act nil nil) :makes-the-layer :stands))]))
                       other-gate-rows)))
  (shown "the checks of the places:"
         (str/join " | " (map (fn [[what _ _ word]] (str what " " (or word "passes"))) places-rows)))
  (shown "the writes of a decision: a refusal writes the answer and the last stamp; an accept of one value writes 3 rows, the fact, the answer and the last stamp, and the fact's row holds the one text of its value, the time on the act, who wrote it and the permission that the act named:"
         (pr-str (mapv (fn [[which k row]] [which k (if (map? row) (dissoc row :digest) row)])
                       (decision/writes (act-id 1) (act-of good) nil 5 now (digest/digest good) {}))))
  (shown "what a permission allows:" (pr-str (mapv (juxt identity act/allows) [alice-own bobs bobs-see bobs-see-and-act]))
         "| a holder of see who names her permission as the row has it, without the slot, and with see and act in it:"
         (pr-str (mapv (fn [named] (decision/standing :streaming (assoc (act-of good) :who :bob :permission named)
                                                      (permission-row bobs-see) nil))
                       [bobs-see bobs bobs-see-and-act])))
  (shown "a value given as {:b 1.50M, :a [1 2N]} is in its fact's row as"
         (pr-str (:value (nth (first (decision/writes (act-id 1) (act-of (arrived 1 (value-at :note-1 :text (array-map :b 1.50M :a [1 2N]))))
                                                      nil 5 now (digest/digest good) {}))
                              2)))
         "and the row reads back as"
         (pr-str (record/read-value (nth (first (decision/writes (act-id 1) (act-of (arrived 1 (value-at :note-1 :text (array-map :b 1.50M :a [1 2N]))))
                                                                 nil 5 now (digest/digest good) {}))
                                         2))))
  (shown "the order by value," (count by-value-cases) "cases, each act decided as steps 6 and 7 of the walk decide it: a key of that order is"
         (pr-str (entry-key 100 :c1 :b1))
         "| a mark at stamp 100, a fact, the mark off, the fact replaced, the mark again at 140, the fact replaced twice: the rows after each act are those of the plan's table, and the row of the first marking stays under 100 where no read names it"))

(deftest the-walk-of-one-thing-in-memory
  (let [copies (holds copy-cases)
        again (holds second-run-cases)
        stamps (holds stamp-line-cases)
        marks (holds mark-cases)
        copies-of (copies-of-as-written)
        writes-of (fn [refused] (decision/writes (act-id 1) (act-of good) refused 5 now (digest/digest good) {}))
        times [now (+ now 5) (- now 60000) far-ahead 0]
        timed-acts (vec (map-indexed (fn [i t] (assoc (arrived (+ 450 i) (value-at (keyword (str "t" i)) :text)) declaration/time-part t))
                                     times))
        after-the-acts (walked-line empty-task :streaming (cons (as-sent (making :alice :alice :alice) :streaming) timed-acts))]
    (shown "what a gate may copy to other tasks," copies "cases: of the writes of a decision, each row of a fact and of the facts by value, in their order, a key that is taken out among them, and never the answer or the last stamp; nothing of a refusal. `copies-of`, read as data from the source file of the walk and made a function here with no Rama, gives what the proof's own filter gives. Of the writes of an accept of one value, and of a refusal, as the kinds of the writes and of what is copied:"
           (pr-str (mapv (fn [writes] [(mapv first writes) (mapv first (copies-of writes))]) [(writes-of nil) (writes-of :layer-not-made)])))
    (shown "a second run of one decision," again "cases, by the walk in memory: from the same record it gives the same writes in every field; on the record that the first run left it finds the answer under the act-id, writes nothing and leaves every table as it was. As [what, the kinds of the writes, the word]:"
           (str/join " | " (map (fn [[what _ _ _ kinds word]] (str what " " (pr-str kinds) " " (or word "accepted"))) second-run-rows)))
    (shown "the stamp in a line," stamps "cases, by the walk in memory, for a layer of each gate: a layer's stamps are 0, 1, 2 and on, an accept's and a refusal's alike, whatever the times on its acts and the stamps that they carry. Five acts after the making act of Alice's layer, with times that rise, go back by a minute, leap 2^50 ms ahead and fall to 0, as [time, stamp] from their answers:"
           (pr-str (mapv (fn [thing] ((juxt :time :stamp) (get-in after-the-acts [:answers (record/answer-key :alice (nth (:act-id thing) 2))])))
                         timed-acts)))
    (shown "a mark holds from the next act," marks "cases, for a layer of each gate: the key gets the stamp of the act that marks it; a fact under the key in that act is in no run; a fact under it in the next act has its entry in the facts by value under that stamp")))

(deftest a-line-is-decided-in-the-depots-order
  (let [n (holds line-cases)
        after (walked-line (:record line) :microbatch (:things line))]
    (shown "a line of" (count (:things line)) "things into a base that is made, decided in memory one thing after the other in the order of the depot, with the pure functions in the walk's order;" n
           "cases with a line into a base that is not made, a line of strays, a line of repeats, and the line decided a second time. The word of each answer, by the number of its act-id:"
           (pr-str (words-by-number after)))
    (shown "after that line, the last stamp kept for each layer, the base's" before-the-line "before it:"
           (pr-str (into (sorted-map) (:last-stamp after)))
           "| the strays, how many digests under each word:"
           (pr-str (into (sorted-map) (map (fn [[word digests]] [word (count digests)])) (:strays after))))
    (shown "a line into a base that is not made: an act before the making, two making acts, acts after:"
           (pr-str (words-by-number (walked-line (:record making-line) :microbatch (:things making-line)))))))

(deftest a-line-under-marked-keys
  (let [n (holds marks-line-cases)
        after (walked-line (:record marks-line) :microbatch (:things marks-line))]
    (shown "a line of" (count (:things marks-line)) "things into a base that marks two keys, decided in memory one thing after the other in the order of the depot," n
           "cases. The word of each answer, by the number of its act-id:" (pr-str (words-by-number after)))
    (shown "the stamps of the marked keys after that line:" (pr-str (record/marked-keys-of (get-in after [:facts base-marks-place])))
           "where the key from before the line keeps" (:since marks-line)
           "and each key that an act of the line marked has the stamp of that act; a fact under such a key in the act after is in the order by value, which holds"
           (count (:by-value after)) "rows after the line")))

(deftest the-doors-words
  (let [n (holds door-words-cases)]
    (shown "the door's words," n "cases. Its own seven, each read after \"the door could not\":"
           (str/join " " (map (fn [[word appended]] (str word (when (= :not-known appended) " (the door does not know whether the depot holds the thing)")))
                              (sort-by key door-words)))
           "| none is a gate's reason word or a word of a stray | what a word can carry:" (pr-str (sort (keys words/carried))))
    (shown "the four answers from what a read by act-id gave:"
           (pr-str (words/answer read-accepted "aa")) (pr-str (words/answer read-refused "aa"))
           (pr-str (words/answer nil "aa")) (pr-str (words/answer read-accepted "bb"))
           "| the two acks:" (pr-str words/acks))))

(deftest the-doors-additions
  (let [rows (holds not-an-act-cases)
        others (holds additions-cases)]
    (shown "what the door makes of one act," (+ rows others) "cases." rows
           "of them are things that are no act, one kind after the other: a thing ends in one of the door's words and nothing is appended, or it goes in and a gate's check has the word:"
           (str/join " | " (map (fn [[what f _]] (str (second (str/split what #": ")) " -> " (pr-str (f))))
                                (filter (fn [[what _ _]] (re-find #"^no act: (a map with no :layer|a thing whose :layer cannot|a record with a :layer|an act that already carries :depot|an act that already carries the part of the time|257 facts|a fact that is a record|a fact that carries :sealed|a value that is no value|a value whose text|1,025)" what))
                                        not-an-act-cases))))
    (shown "a reading of a maker's act holds" (pr-str (sort (keys (additions/reading (maker value-fact) nil))))
           "| the thing that the door builds is the maker's act with :act-id, :depot and the time that the door hands, here" now
           "under" (str declaration/time-part ",") "each other part as given:"
           (pr-str (:thing (built (maker (assoc value-fact :v (array-map :b 1.50M :a [1 2N])) {:e :note-2 :k :text}))))
           "| the declaration's check takes it, and gives the value the text"
           (pr-str (:text (first (:facts (act-of (:thing (built (maker (assoc value-fact :v (array-map :b 1.50M :a [1 2N]))))))))))
           "| a maker's thing that already holds that part, also with nil:"
           (pr-str (additions/reading (assoc (maker value-fact) declaration/time-part nil) nil))
           "| the word says:" (pr-str (:says (get words/words :add-its-parts))))
    (shown "the digest of the thing with its facts as a list, as a lazy sequence and as a vector:"
           (pr-str (mapv (fn [facts] (subs (digest/hex-of (:thing (built (assoc makers-act :facts facts)))) 0 12))
                         [(list value-fact) (map identity [value-fact]) [value-fact]])))))

(deftest the-doors-wait
  (let [n (holds wait-cases)]
    (shown "the numbers of a maker's wait," n "cases: the door's one number is" (pr-str wait/default-numbers)
           "; an opener's number is taken when it is a fixed-width integer of 1 or more, and no other name is read:"
           (pr-str (wait/numbers {:read-every-ms 50 :no-number-of-the-door 400}))
           "| the longest wait that the door counts is" wait/longest-wait-ms "ms, and a greater number is cut to it:"
           (pr-str (wait/numbers {:read-every-ms Long/MAX_VALUE})))))

;; ------------------------------------------- what no run in process can see

(defn- source-forms
  "Takes the path of a file of the store's source, as `source-files` gives
  it. Gives the file's top-level forms, read as data (`forms-of`)."
  [path]
  (forms-of (some (fn [[p f]] (when (= path p) f)) (source-files))))

(defn- lists-headed-by
  "Takes forms and a symbol. Gives each list among them, at any depth, whose
  first element is that symbol, in the order of a walk of the forms."
  [forms head]
  (filter (fn [x] (and (seq? x) (= head (first x)))) (tree-seq coll? seq forms)))

(defn- holds-a?
  "Takes a form and a symbol. Gives true when a list headed by that symbol
  is in the form, at any depth."
  [form head]
  (boolean (seq (lists-headed-by [form] head))))

(defn- retry-mode-of
  "Takes the forms of the streaming gate's file. Gives the retry mode that
  its `source>` declares, or nil when it declares none."
  [forms]
  (some (fn [form] (:retry-mode (some #(when (map? %) %) form)))
        (lists-headed-by forms 'source>)))

(defn- copy-of
  "Takes the forms of the microbatch gate's file. Gives the copy: the one
  `<<if` that holds the partitioner `|all`, or nil when there is not one."
  [forms]
  (let [found (filter #(holds-a? % '|all) (lists-headed-by forms '<<if))]
    (when (= 1 (count found)) (first found))))

(defn- copy-goes-only-for-an-accept?
  "Takes the forms of the microbatch gate's file. Gives true when the copy
  goes out only behind a test that the walk gave rows to copy: its `<<if`
  tests `(seq x)`. A refusal, a repeat and a stray give an empty vector,
  which `seq` turns down, and `some?` would not."
  [forms]
  (let [copy (copy-of forms)
        test-form (second copy)]
    (boolean (and (seq? test-form) (= 'seq (first test-form))))))

(defn- copy-passes-over-the-deciding-task?
  "Takes the forms of the microbatch gate's file. Gives true when every
  write of the copy stands behind one `<<if` whose test is `(not= a b)`,
  where `a` and `b` are the ids of the task that `ops/current-task-id`
  gives, one taken before the partitioner `|all` (the deciding task) and one
  after it (the task where the copy arrived)."
  [forms]
  (let [copy (copy-of forms)
        body (vec (drop 2 copy))
        at (first (keep-indexed (fn [i x] (when (= '(|all) x) i)) body))
        task-id (fn [x] (when (and (seq? x) (= 'ops/current-task-id (first x))) (last x)))
        before (set (keep task-id (take (or at 0) body)))
        after (set (keep task-id (drop (inc (or at 0)) body)))
        guards (filter #(holds-a? % 'local-transform>) (drop (inc (or at 0)) body))
        guard (first guards)
        [op a b] (when (and (seq? guard) (= '<<if (first guard))) (second guard))
        writes (fn [form] (count (lists-headed-by [form] 'local-transform>)))]
    (boolean (and at (= 1 (count guards)) (= 'not= op)
                  (or (and (before a) (after b)) (and (before b) (after a)))
                  (pos? (writes guard)) (= (writes copy) (writes guard))))))

(def ^:private unseen-cases
  "The cases of what the gates' code holds that no run in process can see:
  each read of the source, on the file as it is and on the same forms with
  the bug that a review put in on 14 October, which no test that runs the
  module caught."
  (let [streaming (source-forms "softland/store/module/streaming_gate.clj")
        microbatch (source-forms "softland/store/module/microbatch_gate.clj")
        with (fn [forms smap] (walk/postwalk-replace smap forms))]
    [["the streaming gate's source declares the retry mode \"all after\", which the store's page gives as chosen. A run in process gave the same with \"individual\", so only a reading of the source holds it; with \"individual\" put in, the reading says so"
      (fn [] [(retry-mode-of streaming) (retry-mode-of (with streaming {:all-after :individual}))])
      [:all-after :individual]]
     ["the microbatch gate copies only for an accept: its copy stands behind a test that the walk gave rows to copy, so a refusal, a repeat and a stray go to no other task. The tables come out the same when they do go, so only a reading of the source holds it; with `some?` in the place of `seq`, the reading fails"
      (fn [] [(copy-goes-only-for-an-accept? microbatch)
              (copy-goes-only-for-an-accept? (with microbatch {'(seq *copies) '(some? *copies)}))])
      [true false]]
     ["the microbatch gate's copy passes over the deciding task: each of its writes stands behind a test that the task where the copy arrived is not the task that decided. No run in process showed a row that a copy to itself sets back, so only a reading of the source holds it; with the test made `(some? *this-task)`, the reading fails"
      (fn [] [(copy-passes-over-the-deciding-task? microbatch)
              (copy-passes-over-the-deciding-task? (with microbatch {'(not= *this-task *deciding-task) '(some? *this-task)}))])
      [true false]]]))

(deftest what-no-run-in-process-can-see
  (let [n (holds unseen-cases)]
    (shown "what the gates' code holds that no run in process can see," n
           "cases, each a reading of the source and the same reading with the bug that a review put in: the streaming gate's retry mode,"
           (pr-str (retry-mode-of (source-forms "softland/store/module/streaming_gate.clj")))
           "| the microbatch gate's copy, only for an accept and never to the deciding task:"
           (pr-str [(copy-goes-only-for-an-accept? (source-forms "softland/store/module/microbatch_gate.clj"))
                    (copy-passes-over-the-deciding-task? (source-forms "softland/store/module/microbatch_gate.clj"))]))))

(deftest no-code-of-the-store-names-a-lock
  (let [n (holds lock-name-cases)
        {:keys [files names found] saw-act? :saw?} (lock-names-in (source-files) 'softland.store.act)
        of-the-proofs (lock-names-in (proof-files) 'softland.store.pure-proof)
        meant-there (remove (fn [[_ x]] (= :not-read x)) (:found of-the-proofs))]
    (is (= [] found)
        "no symbol and no keyword in the code of the store's source has a word that begins with lock, seal or stock, but Clojure's locking; a string and a comment are not code")
    (is (= [true true true] [(<= 20 files) (<= 5000 names) saw-act?])
        "the reading of the source read 20 files or more, 5,000 names or more, and the namespace of the act among them: it did not pass for having read nothing")
    (is (= [] (not-meant (:found of-the-proofs)))
        "the checks of the locks and of the seal went with what they checked: in the code of the proofs no symbol and no keyword names a lock, a seal or a stock but those that a proof means, where it means them: a seal's part or a request for locks that is sent to be refused or to pass as a tool's own key, the entity of such a fact, the names of the checks that the store keeps no lock, and in this proof the examples that its own cases write out")
    (is (= [true true true true]
           [(<= 8 (:files of-the-proofs)) (<= 5000 (:names of-the-proofs)) (:saw? of-the-proofs) (<= 20 (count meant-there))])
        "the reading of the proofs read 8 files or more, 5,000 names or more, this proof's namespace among them, and found 20 names or more that name a lock: it did not pass for having read nothing, or for a test of a name that finds none")
    (is (= [[["softland/store/some_proof.clj" 'seal/open] ["softland/store/some_proof.clj" 'with-locks-read]
             ["softland/store/some_proof.clj" :not-read "Exception"]]
            []]
           [(not-meant [["softland/store/some_proof.clj" :sealed] ["softland/store/some_proof.clj" 'seal/open]
                        ["softland/store/pure_proof.clj" 'seal/open] ["softland/store/some_proof.clj" 'with-locks-read]
                        ["softland/store/some_proof.clj" :not-read "Exception"]])
            (not-meant [])])
        "the test of what is meant can fail: a seal's part that is sent to be refused is meant in any proof; an example of this proof's cases is meant in this proof and in no other, a call of the seal's code and a read of a lock's rows among them; and a file that could not be read is not meant")
    (shown "no code of the store names a lock," n "cases of the test of a name, then the source read as data:" files
           "files under the directory that holds softland/store/act.clj," names
           "symbols and keywords, of which" (count found) "name a lock, a seal or a stock" (pr-str found))
    (shown "the proofs read the same way:" (:files of-the-proofs) "files under the directory that holds this proof," (:names of-the-proofs)
           "symbols and keywords, of which" (count meant-there) "name a lock, a seal or a stock, each of them meant where it stands; not meant:"
           (pr-str (not-meant (:found of-the-proofs))) "| by file:"
           (pr-str (into (sorted-map) (map (fn [[file found-there]] [file (count found-there)])) (group-by first meant-there))))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-one-text-and-the-digest)
  (the-declaration)
  (refusals-of-the-declaration)
  (the-maker's-check-inside-the-door's)
  (the-order-of-the-reasons)
  (the-time-on-the-act)
  (the-bounds)
  (nothing-throws)
  (the-stamp)
  (the-first-act)
  (the-records-rows)
  (the-decision)
  (the-walk-of-one-thing-in-memory)
  (a-line-is-decided-in-the-depots-order)
  (a-line-under-marked-keys)
  (the-doors-words)
  (the-doors-additions)
  (the-doors-wait)
  (no-code-of-the-store-names-a-lock)
  (what-no-run-in-process-can-see))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.pure-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
