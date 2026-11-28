(ns softland.store.pure-proof
  "The proof of the gates' pure namespaces: the one text and the digest, the
  door's declaration, the time on the act, the stamp, the first act of a
  lane, the record's rows, and the checks of the decision, each alone and
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
  is the lane's count of decisions, which neither the time on the thing
  nor a stamp that the act carries moves, in a lane of either gate. The
  name of the part is
  written out as a literal in three places, each held against the
  declaration's: the second writing of the shape, the text of the door's
  word, and the first field of the record `Timed`. That a door writes its
  server's clock there, and that no gate reads a clock, need a cluster and
  are not reached here.

  A lane of the other gate. The cases of the standing call
  `softland.store.gate.decision/standing` and `makes-the-lane?` with the
  gate's depot, the act and two rows, for each gate's depot word. They hold
  that an act for a lane that starts with the other gate's depot, of which
  the gate's record holds nothing, gets the word of such a lane from the
  act alone: the base on the streaming depot, and any other lane on the
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
  finds the answer and writes nothing; that the stamps of a lane are 0, 1,
  2 and on with no gap, for an accept and for a refusal, whatever the times
  on its acts and the stamps that they carry; and that a mark of a key
  holds from the next act, in a lane of each gate. What this shows is what the pure
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
  (:refer-clojure :exclude [agent])
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [clojure.walk :as walk]
            [softland.store.act :as act]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.decision :as decision]
            [softland.store.gate.walk :as gate-walk]
            [softland.store.door.additions :as additions]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.gate.first-act :as first-act]
            [softland.store.permissions :as perm :refer [person agent run]]
            [softland.store.record.in-memory :as memory]
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

(def ^:private alice-own "Alice's permission in her own lane." (perm/first-permission (person :alice) :alice))
(def ^:private bobs "Bob's permission in Alice's lane, beneath hers." (perm/given-permission (person :bob) :alice alice-own))

(defn- act-id
  "A streaming act-id in Alice's lane, with a fixed UUID."
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
  "A permission id of a lane that holds n permission ids, itself counted.
  Each of them is held by `holder` in that lane, the first one too."
  [holder lane n]
  (perm/deep-permission holder lane n))

(defn- arrived
  "Alice's act into her own lane as it arrives on the streaming depot, under
  the act-id `(act-id n)`, with the time `now` on it, these facts and no
  optional part."
  [n & facts]
  {:act-id (act-id n) :depot :streaming declaration/time-part now
   :version 1 :who (person :alice) :lane :alice :permission (perm/permission-id alice-own) :session :alice-s1
   :facts (vec facts)})

(defn- one-form
  "The one form on arrival of `arrived` for facts that are in their one form."
  [n & facts]
  {:act-id (act-id n) :depot :streaming declaration/time-part now
   :version 1 :who (person :alice) :lane :alice :permission (perm/permission-id alice-own) :session :alice-s1
   :based-on {} :because-of nil
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
                    (assoc good :who (person :bob)) (assoc good :a-part-more now)]))
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
  "The eight shapes that the door adds, written out here a second time. What
  arrives holds the time on the act after the depot: a fixed-width integer
  of 0 or more, with no bound above."
  {:arrived-act
   [:parts
    [:act-id :act-id]
    [:depot :depot-word {:reason :depot-not-the-act-ids-depot}]
    [:door-time [:int {:min 0}] {:reason :no-door-time}]
    [:version [:enum 1] {:reason :unknown-version :edition true}]
    [:who :author-party {:reason :who-not-a-party}]
    [:lane :id]
    [:permission :uuid {:optional true :reason :permission-not-an-id}]
    [:session :id {:absent :no-session}]
    [:based-on [:map-of :fact-id :carried-stamp] {:optional true :default {}}]
    [:because-of :act-id {:optional true}]
    [:facts [:seq-of :fact] {:empty :empty-act}]]
   :depot-word [:enum :streaming :microbatch]
   :who-may-read-acts-word [:enum :members :everyone]
   :lane-list [:seq-of :id]
   :key-set [:set-of :key]
   :permission-value
   [:parts
    [:holder :holder-party]
    [:allows :allows]
    [:covers :covers]
    [:session :id {:optional true :left-out true}]
    [:given-by :author-party]
    [:under :uuid {:optional true :left-out true}]
    [:lent-from :uuid {:optional true :left-out true}]]
   :allows
   [:parts
    [:see [:enum true] {:optional true :left-out true}]
    [:write [:enum :all] {:optional true :left-out true}]
    [:give [:set-of [:enum :see :write :give :owner :everyone]] {:optional true :left-out true :empty :malformed}]
    [:owner [:enum true] {:optional true :left-out true}]]
   :covers
   [:or [:tuple [:enum :default-group] :id]
        [:tuple [:enum :exact-act] :digest-hex]]})

(def ^:private declared-rules
  "The rules of the declaration, written out here a second time."
  [[:depot-is-the-act-ids-depot :depot-not-the-act-ids-depot]
   [:facts-within-the-limit :too-many-facts]
   [:based-on-within-the-limit :too-many-based-on]
   [:store-key-is-known :store-key-not-known]
   [:depot-fact-holds-a-depot-word :depot-fact-not-a-depot-word]
   [:permission-fact-sits-on-an-id :permission-fact-not-on-an-id]
   [:permission-fact-holds-a-permission :permission-fact-not-a-permission]
   [:who-may-read-acts-fact-holds-its-word :who-may-read-acts-fact-not-members-or-everyone]
   [:builds-on-fact-holds-lanes :builds-on-fact-not-a-list-of-lanes]
   [:builds-on-within-the-limit :builds-on-too-many-lanes]
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
  #{:malformed :unknown-part :unknown-version :who-not-a-party :permission-not-an-id :no-session :empty-act
    :depot-not-the-act-ids-depot :no-door-time :too-many-facts :too-many-based-on
    :store-key-not-known
    :depot-fact-not-a-depot-word :permission-fact-not-on-an-id :permission-fact-not-a-permission
    :who-may-read-acts-fact-not-members-or-everyone
    :builds-on-fact-not-a-list-of-lanes
    :builds-on-too-many-lanes :marked-keys-fact-not-a-set-of-keys :too-many-marked-keys
    :marked-key-in-the-stores-namespace
    :place-named-twice :replaces-own-fact :id-too-long
    :value-too-large :act-too-large})

(def ^:private declaration-parts-cases
  "The declarations of the door, each against its second writing here, and
  what they say as sets."
  [["the door's map of shapes holds each shape of act.clj as act.clj declares it"
    (fn [] (= act/shapes (select-keys declaration/shapes (keys act/shapes))))
    true]
   ["the eight shapes that the door adds: what arrives, with the time on the act after the depot, whose facts are the maker's own; a depot word; the three shapes of what a permission says; and the values of the three settings that take a second write"
    (fn [] (apply dissoc declaration/shapes (keys act/shapes)))
    declared-shapes]
   ["the name of the part that holds the time on the act is the one that the shape of what arrives holds, and that shape takes any fixed-width integer not below zero: it has no bound above"
    (fn [] (let [[part shape] (nth (:arrived-act declaration/shapes) 3)]
             [declaration/time-part (= part declaration/time-part) shape]))
    [:door-time true [:int {:min 0}]]]
   ["the sixteen rules, each with its reason; the six of the settings that take a second write stand after the two rules of a permission fact"
    (fn [] declaration/rules)
    declared-rules]
   ["the words with which the check refuses, read from the two declarations, the word of the time's part among them, and the two of its last step"
    (fn [] [declaration/reasons (contains? declaration/reasons :no-door-time)])
    [declared-reasons true]]
   ["the five words of a stray, in the order tested"
    (fn [] declaration/stray-reasons)
    [:no-act-id :act-id-malformed :id-too-long :act-id-of-the-other-gate :act-id-of-another-lane]]
   ["the seven limits: facts, entries of based-on, the bytes of one value's text, the bytes of text that one act writes into the rows of its facts, the text of an id, the lanes that a lane builds on, the keys that a lane marks"
    (fn [] [declaration/max-facts declaration/max-based-on declaration/max-value-bytes
            declaration/max-act-bytes declaration/max-id-text
            declaration/max-builds-on declaration/max-marked-keys])
    [256 1024 65536 18874368 256 64 32]]
   ["the bound on an act is 256 values at the bound of one value, and 8,192 bytes of who and permission for each of 256 facts"
    (fn [] (= declaration/max-act-bytes (* 256 (+ 65536 8192))))
    true]
   ["the five store keys, the shape of the value of each, which is a setting, and which setting is written again; the three old keys of permissions and of who may see, any other key in the store's namespace, and a bare word are no store keys"
    (fn [] [(into {} (map (fn [[k entry]] [k (:value entry)])) declaration/store-keys)
            declaration/setting-keys
            declaration/second-write-settings
            declaration/permission-key
            (mapv declaration/store-key?
                  [:softland.store/depot :softland.store/permission
                   :softland.store/run-permission :softland.store/act-permission
                   :softland.store/who-may-see :softland.store/who-may-read-acts
                   :softland.store/builds-on :softland.store/marked-keys
                   :softland.store/lock-request :softland.store/owner
                   :class :depot :permission :lock-request :tool/depot :owner
                   :who-may-see :who-may-read-acts :builds-on :marked-keys :tool/marked-keys])])
    [{:softland.store/depot :depot-word :softland.store/permission :permission-value
      :softland.store/who-may-read-acts :who-may-read-acts-word
      :softland.store/builds-on :lane-list :softland.store/marked-keys :key-set}
     #{:softland.store/depot :softland.store/who-may-read-acts
       :softland.store/builds-on :softland.store/marked-keys}
     #{:softland.store/who-may-read-acts :softland.store/builds-on :softland.store/marked-keys}
     :softland.store/permission
     [true true false false false true true true false false false false false false false false false false false false false]]]
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
   ["an act-id whose lane is a string" (assoc good :act-id ["alice" :streaming (uuid 1)]) :act-id-malformed]
   ["an act-id that is a list" (assoc good :act-id (list :alice :streaming (uuid 1))) :act-id-malformed]
   ["an act-id whose lane prints to 257 characters"
    (assoc good :act-id [(long-id 257) :streaming (uuid 1)] :lane (long-id 257)) :id-too-long]
   ["an act-id of the depot microbatch" (assoc good :act-id [:alice :microbatch (uuid 1)]) :act-id-of-the-other-gate]
   ["an act-id of another lane" (assoc good :act-id [:bob :streaming (uuid 1)]) :act-id-of-another-lane]
   ["a thing with an act-id and no lane" (dissoc good :lane) :act-id-of-another-lane]
   ["an act-id of the depot microbatch and of another lane: the depot is told first"
    (assoc good :act-id [:bob :microbatch (uuid 1)]) :act-id-of-the-other-gate]
   ["a good act" good nil]
   ["a thing that is no act, with a usable act-id" {:act-id (act-id 1) :lane :alice :junk 1} nil]
   ["an act-id whose lane prints to 256 characters"
    (assoc good :act-id [(long-id 256) :streaming (uuid 1)] :lane (long-id 256)) nil]])

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
  given with a nil optional key. Its time is a narrow integer too."
  {:act-id (subvec (into [:x] (act-id 9)) 1)
   :depot :streaming
   declaration/time-part (int 1700001)
   :version (int 1)
   :who (person :alice) :lane :alice
   :permission (perm/permission-id alice-own)
   :session :alice-s1
   :based-on {[(act-id 2) (int 0)] (int 41)}
   :because-of (act-id 2)
   :facts (list {:e :note-1 :k :text :v (sorted-map :b 1.50M :a (subvec [0 (int 1) 2N] 1))
                 :replaces [(act-id 2) (short 0)]}
                {:e :note-2 :k :text}
                {:e (perm/permission-id bobs) :k :softland.store/permission
                 :v (assoc (perm/permission-value bobs) :session nil)})})

(def ^:private whole-form
  "The one form on arrival of `whole`."
  {:act-id (act-id 9) :depot :streaming declaration/time-part 1700001
   :version 1 :who (person :alice) :lane :alice :permission (perm/permission-id alice-own) :session :alice-s1
   :based-on {[(act-id 2) 0] 41} :because-of (act-id 2)
   :facts [{:e :note-1 :k :text :v nil :replaces [(act-id 2) 0] :text "{:a [1 2] :b 1.5M}"}
           {:e :note-2 :k :text :v nil :replaces nil :text nil}
           (assoc (perm/permission-fact bobs) :replaces nil
                  :text (text/text (perm/permission-value bobs)))]})

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
  [["the least thing in its one form on arrival: each part, the time on the act as it arrived, and its fact with its five parts, the text of the value in the place of the value; and an act with no permission holds nil for it"
    (fn [] [(act-of good) (get (act-of good) declaration/time-part)
            (:permission (act-of (dissoc good :permission)))])
    [(one-form 1 {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""}) now nil]]
   ["a thing with each part, given with subvectors, narrow integers and a list of facts: a fact under a plain key holds the text of its value's normal form and no value, a fact with no value holds neither, a fact under a store key holds both"
    (fn [] (act-of whole))
    whole-form]
   ["its one form has plain vectors and longs, the text is a String, and the time, given as a narrow integer, is a long"
    (fn [] (let [form (act-of whole)
                 [fact-id carried] (first (:based-on form))]
             (mapv #(.getSimpleName (class %))
                   [(:act-id form) (:permission form) (:facts form) fact-id (second fact-id) carried
                    (second (:replaces (first (:facts form))))
                    (:v (nth (:facts form) 2)) (:text (first (:facts form)))
                    (get whole declaration/time-part) (get form declaration/time-part)])))
    ["PersistentVector" "UUID" "PersistentVector" "PersistentVector" "Long" "Long"
     "Long" "PersistentArrayMap" "String" "Integer" "Long"]]
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
   ["a making act: no permission named, and two facts under store keys, or three when opened to anyone at making, each with its value and the text of it"
    (fn [] (let [own (perm/first-permission (person :alice) :alice {:given-by :system})
                 checked (declaration/check
                          (-> (apply arrived 1 (perm/making-facts :alice (person :alice) :system))
                              (assoc :who :system)
                              (dissoc :permission)))
                 opened (declaration/check
                         (-> (apply arrived 2 (perm/making-facts :alice (person :alice) (person :alice) {:open? true}))
                             (dissoc :permission)))]
             [(verdict checked) (:permission (:ok checked)) (mapv (juxt :v :text) (:facts (:ok checked)))
              (verdict opened) (count (:facts (:ok opened)))]))
    [:ok nil [[:streaming ":streaming"] [(perm/permission-value (perm/first-permission (person :alice) :alice {:given-by :system}))
                                         (text/text (perm/permission-value (perm/first-permission (person :alice) :alice {:given-by :system})))]]
     :ok 3]]
   ["a retract under a plain key: no value"
    (fn [] (verdict (declaration/check (arrived 1 {:e :note-1 :k :text}))))
    :ok]
   ["each of the five store keys is known: a fact under each, with a right value, passes; a permission sits on its own uuid, whether for the lane's default group, for one sitting, or for one exact act"
    (fn [] [(verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/depot :v :streaming})))
            (verdict (declaration/check (arrived 1 (perm/permission-fact bobs))))
            (verdict (declaration/check (arrived 1 (perm/permission-fact
                                                    (perm/run-permission (person :bob) :alice alice-own :run-7 :see)))))
            (verdict (declaration/check (arrived 1 (perm/permission-fact
                                                    (perm/act-permission (person :bob) :alice alice-own
                                                                         (uuid 9) (apply str (repeat 64 "a")))))))
            (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v :everyone})))
            (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base (uuid 7)]})))
            (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on :tool/parent}})))])
    [:ok :ok :ok :ok :ok :ok :ok]]
   ["the text of a set under a store key is sorted: the marked keys, with the set beside it"
    (fn [] ((juxt :v :text) (first (:facts (act-of (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on :tool/parent :b :a}}))))))
    [#{:on :tool/parent :b :a} "#{:a :b :on :tool/parent}"]]
   ["the values that the three settings of a second write take: each word of who may read a lane's acts; a list of lanes that is empty, names the lane itself, names a lane twice or names a lane that nobody made; a set of keys that is empty"
    (fn [] (mapv (fn [[k v]] (verdict (declaration/check (arrived 1 {:e :alice :k k :v v}))))
                 [[:softland.store/who-may-read-acts :members] [:softland.store/who-may-read-acts :everyone]
                  [:softland.store/builds-on []] [:softland.store/builds-on [:alice]]
                  [:softland.store/builds-on [:base :base]] [:softland.store/builds-on [:nobody-made-this]]
                  [:softland.store/marked-keys #{}]]))
    [:ok :ok :ok :ok :ok :ok :ok]]
   ["a mark of a key in a namespace that only begins with the store's is a mark of a plain key: the store's namespace is taken exactly"
    (fn [] (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store.tool/on :softland/on}}))))
    :ok]
   ["a fact with no value under each of the three settings of a second write passes the declaration, as under the others: a gate refuses the retract"
    (fn [] (mapv (fn [k] (verdict (declaration/check (arrived 1 {:e :alice :k k}))))
                 [:softland.store/who-may-read-acts :softland.store/builds-on :softland.store/marked-keys]))
    [:ok :ok :ok]]
   ["a permission fact whose value says what it allows passes: see, see and write, member, publisher, and owner"
    (fn [] [(verdict (declaration/check (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice alice-own :see)))))
            (verdict (declaration/check (arrived 1 (perm/permission-fact (perm/given-permission (agent :bot) :alice alice-own :see-and-act)))))
            (verdict (declaration/check (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice alice-own :see-and-act)))))
            (verdict (declaration/check (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice alice-own :publisher)))))
            (verdict (declaration/check (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice alice-own :owner)))))])
    [:ok :ok :ok :ok :ok]]
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
   ["a fact with a part that the form does not name" (arrived 1 (assoc (value-at :note-1 :text) :lane :bob)) :unknown-part]
   ["a fact with a value that carries :sealed: a fact has no such part" (arrived 1 (assoc (value-at :note-1 :text) :sealed (bytes-of 3))) :unknown-part]
   ["a fact with a value that carries :lock-id" (arrived 1 (assoc (value-at :note-1 :text) :lock-id [(act-id 50) 0])) :unknown-part]
   ["a fact with no value that carries :sealed and :lock-id" (arrived 1 {:e :note-1 :k :text :sealed (bytes-of 3) :lock-id [(act-id 50) 0]}) :unknown-part]
   ["a fact whose :sealed is nil: the part is there" (arrived 1 (assoc (value-at :note-1 :text) :sealed nil)) :unknown-part]
   ["a version that the code does not know" (assoc good :version 2) :unknown-version]
   ["a permission that is no uuid" (assoc good :permission [(person :alice) :alice :alice]) :permission-not-an-id]
   ["no session" (dissoc good :session) :no-session]
   ["no fact" (assoc good :facts []) :empty-act]
   ["a who that is a bare keyword with no party kind" (assoc good :who :alice) :who-not-a-party]
   ["a who that is a string" (assoc good :who "alice") :who-not-a-party]
   ["a who that is everyone: everyone holds and never acts" (assoc good :who :anyone) :who-not-a-party]
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
   ["a fact under the removed run-permission store key" (arrived 1 {:e :run-7 :k :softland.store/run-permission :v (perm/permission-value bobs)}) :store-key-not-known]
   ["a fact under the removed act-permission store key" (arrived 1 {:e (uuid 9) :k :softland.store/act-permission :v (perm/permission-value bobs)}) :store-key-not-known]
   ["a fact under the removed who-may-see store key" (arrived 1 {:e :alice :k :softland.store/who-may-see :v :everyone}) :store-key-not-known]
   ["a depot fact whose value is no depot word" (arrived 1 {:e :alice :k :softland.store/depot :v :fast}) :depot-fact-not-a-depot-word]
   ["a permission fact that sits on a party keyword and no uuid" (arrived 1 {:e (person :bob) :k :softland.store/permission :v (perm/permission-value bobs)}) :permission-fact-not-on-an-id]
   ["a permission fact for one sitting that sits on the sitting keyword and no uuid"
    (arrived 1 {:e :run-7 :k :softland.store/permission
                :v (perm/permission-value (perm/run-permission (person :bob) :alice alice-own :run-7 :see-and-act))})
    :permission-fact-not-on-an-id]
   ["a permission fact whose value is no permission" (arrived 1 {:e (uuid 9) :k :softland.store/permission :v [(person :bob) :alice]}) :permission-fact-not-a-permission]
   ["a permission fact whose holder is no party"
    (arrived 1 {:e (uuid 9) :k :softland.store/permission :v (assoc (perm/permission-value bobs) :holder :bob)})
    :permission-fact-not-a-permission]
   ["a permission fact whose giver is everyone, which never acts"
    (arrived 1 {:e (uuid 9) :k :softland.store/permission :v (assoc (perm/permission-value bobs) :given-by :anyone)})
    :permission-fact-not-a-permission]
   ["a permission fact that allows nothing"
    (arrived 1 {:e (uuid 9) :k :softland.store/permission :v (assoc (perm/permission-value bobs) :allows {})})
    :permission-fact-not-a-permission]
   ["a permission fact with an empty set of what may be given"
    (arrived 1 {:e (uuid 9) :k :softland.store/permission :v (assoc (perm/permission-value bobs) :allows {:see true :give #{}})})
    :permission-fact-not-a-permission]
   ["a permission fact that allows owning and less than everything"
    (arrived 1 {:e (uuid 9) :k :softland.store/permission
                :v (assoc (perm/permission-value bobs) :allows (assoc declaration/owner-allows :give #{:see :write :give :owner}))})
    :permission-fact-not-a-permission]
   ["a permission for an act whose digest is no text of 64 hex characters"
    (arrived 1 (perm/permission-fact (perm/act-permission (person :bob) :alice alice-own (uuid 9) "abc")))
    :permission-fact-not-a-permission]
   ["who may read a lane's acts, with a word that is neither" (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v :visitors}) :who-may-read-acts-fact-not-members-or-everyone]
   ["who may read a lane's acts, as a string" (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v "everyone"}) :who-may-read-acts-fact-not-members-or-everyone]
   ["what a lane builds on, as one lane and no list" (arrived 1 {:e :alice :k :softland.store/builds-on :v :base}) :builds-on-fact-not-a-list-of-lanes]
   ["what a lane builds on, as a set: a set has no order" (arrived 1 {:e :alice :k :softland.store/builds-on :v #{:base}}) :builds-on-fact-not-a-list-of-lanes]
   ["what a lane builds on, with a string in it" (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base "team"]}) :builds-on-fact-not-a-list-of-lanes]
   ["what a lane builds on, 65 lanes" (arrived 1 {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "lane-" %)) (range 65))}) :builds-on-too-many-lanes]
   ["the marked keys of a lane, as a vector" (arrived 1 {:e :alice :k :softland.store/marked-keys :v [:on]}) :marked-keys-fact-not-a-set-of-keys]
   ["the marked keys of a lane, as one key" (arrived 1 {:e :alice :k :softland.store/marked-keys :v :on}) :marked-keys-fact-not-a-set-of-keys]
   ["the marked keys of a lane, with a UUID in the set: a key is a keyword" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on (uuid 3)}}) :marked-keys-fact-not-a-set-of-keys]
   ["the marked keys of a lane, 33 keys" (arrived 1 {:e :alice :k :softland.store/marked-keys :v (set (map #(keyword (str "key-" %)) (range 33)))}) :too-many-marked-keys]
   ["a mark of a key of the store's own namespace" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on :softland.store/permission}}) :marked-key-in-the-stores-namespace]
   ["a mark of a key in the store's namespace that the store does not know" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store/owner}}) :marked-key-in-the-stores-namespace]
   ["two facts on one place" (arrived 1 (value-at :note-1 :text "a") (value-at :note-1 :text "b")) :place-named-twice]
   ["a fact that replaces a fact of its own act"
    (arrived 1 (value-at :note-1 :text) (assoc (value-at :note-2 :text) :replaces [(act-id 1) 0]))
    :replaces-own-fact]])

(def ^:private id-rows
  "The same long id in each place where an act holds an id. Each is refused
  with the one word."
  (let [long (long-id 257)
        long-who (perm/long-person 257)]
    [["who" (assoc good :who long-who)]
     ["the session" (assoc good :session long)]
     ["an entity" (arrived 1 (value-at long :text))]
     ["a fact's key" (arrived 1 (value-at :note-1 long))]
     ["the sitting, in a permission that is given"
      (arrived 1 (perm/permission-fact (perm/run-permission (person :bob) :alice alice-own long :see-and-act)))]
     ["the lane of the act-id in because-of" (assoc good :because-of [long :streaming (uuid 2)])]
     ["the lane of a fact id in based-on" (assoc good :based-on {[[long :streaming (uuid 2)] 0] 1})]
     ["the lane of a replaced fact" (arrived 1 (assoc (value-at :note-1 :text) :replaces [[long :streaming (uuid 2)] 0]))]
     ["the holder in a permission that is given"
      (arrived 1 (perm/permission-fact (perm/given-permission long-who :alice alice-own)))]
     ["the lane covered, in a permission that is given"
      (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:granted-in long})))]
     ["given-by, in a permission that is given"
      (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:given-by long-who})))]
     ["a lane in the list that a lane builds on" (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base long]})]
     ["a key in the set that a lane marks" (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:on long}})]]))

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
    ["the time's word stands after the word of a depot that is absent or no depot word, and before each rule: of the things of `doors-rows` with the time taken off, the first two keep the depot's word, and each of the other thirty-two, whose fault a rule finds, gets the time's; a depot word that is not the act-id's is one of them"
     (fn [] (let [given (mapv (fn [[_ x _]] (verdict (declaration/check (without-time x)))) doors-rows)]
              [(vec (take 2 given)) (frequencies (drop 2 given))]))
     [[:depot-not-the-act-ids-depot :depot-not-the-act-ids-depot] {:no-door-time 32}]]]))

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
  "An act of 256 facts at the bound of one value, 65,536 bytes each, under a
  who of 256 characters and a permission UUID of 36 characters: 256 times
  (65,536 + 292) = 16,851,968 bytes of text into the rows of its facts,
  within `max-act-bytes` (18,874,368). With `more` > 0 the last value has
  `more` letters more."
  [more]
  (let [id (long-id 256)
        who (perm/long-person 256)
        value at-the-bound]
    {:act-id [id :streaming (uuid 1)] :depot :streaming declaration/time-part now
     :version 1 :who who :lane id :permission (perm/permission-id (perm/first-permission who id)) :session :alice-s1
     :facts (mapv (fn [i] {:e (keyword (str "note-" i)) :k :text
                           :v (if (= i 255) (str value (apply str (repeat more "a"))) value)})
                  (range 256))}))

(defn- step-act-at-the-bound
  "A pre-conformed act for `with-texts`, the check's last step, whose rows
  hold 18,874,368 + `more` bytes of text: 288 facts under a 256-character
  who and a 36-character permission UUID (292 bytes each, 84,096 in all),
  286 values of 65,536 bytes, one of 30,000 bytes, and one of 16,976 + `more`
  bytes, none over `max-value-bytes`."
  [more]
  (let [id (long-id 256)
        who (perm/long-person 256)
        perm-id (perm/permission-id (perm/first-permission who id))]
    {:act-id [id :streaming (uuid 1)] :depot :streaming declaration/time-part now
     :version 1 :who who :lane id :permission perm-id :session :alice-s1
     :based-on {} :because-of nil
     :facts (mapv (fn [i] {:e (keyword (str "note-" i)) :k :text :replaces nil
                           :v (cond (< i 286) at-the-bound
                                    (= i 286) (text-of-bytes 30000)
                                    :else (text-of-bytes (+ 16976 more)))})
                  (range 288))}))

(defn- bytes-in-rows
  "The proof's own count of the bytes of text that an accept of a thing would
  write into the rows of its facts: for each fact, the UTF-8 bytes of its
  value, none for a fact with no value, and the bytes of the act's who and
  of its permission id."
  [thing]
  (let [printed (memoize (fn [x] (alength ^bytes (utf8 (pr-str x)))))
        id-bytes (fn [x] (if (some? x) (alength ^bytes (utf8 (str x))) 0))
        for-each-fact (+ (id-bytes (:who thing)) (id-bytes (:permission thing)))]
    (reduce + (map (fn [fact] (+ for-each-fact (if (some? (:v fact)) (printed (:v fact)) 0))) (:facts thing)))))

(def ^:private check-bound-cases
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
   ["the proof's own count of the bytes of 256 full values under a 256-character who and a permission uuid, and of the last step's own edge at 18,874,368 and 18,874,369 bytes; 292 bytes of who and permission id for each fact"
    (fn [] [(bytes-in-rows (full-act 0))
            (bytes-in-rows (step-act-at-the-bound 0))
            (bytes-in-rows (step-act-at-the-bound 1))
            (bytes-in-rows (assoc (full-act 0) :facts [{:e :note-1 :k :text}]))])
    [16851968 18874368 18874369 292]]
   ["the bytes of who and permission id are counted for a fact with no value too: in the last step, an act with a retract that is one byte over 18,874,368 only when the retract's 292 bytes are counted is refused, and with one byte less it passes"
    (fn [] (let [with-a-retract (fn [more]
                                  (-> (step-act-at-the-bound 0)
                                      (assoc-in [:facts 286 :v] (text-of-bytes (+ 46976 more)))
                                      (assoc-in [:facts 287 :v] nil)))]
             [(bytes-in-rows (with-a-retract 1)) (verdict (#'declaration/with-texts (with-a-retract 1)))
              (bytes-in-rows (with-a-retract 0)) (verdict (#'declaration/with-texts (with-a-retract 0)))
              (- (bytes-in-rows (with-a-retract 1)) (bytes-in-rows (update (with-a-retract 1) :facts pop)))]))
    [18874369 :act-too-large 18874368 :ok 292]]
   ["a thing that the last step refuses gives its word and no act: no part of its one form comes out"
    (fn [] [(declaration/check (arrived 1 (value-at :note-1 :text over-the-bound)))
            (#'declaration/with-texts (step-act-at-the-bound 1))])
    [{:refuse :value-too-large} {:refuse :act-too-large}]]])

(def ^:private order-rows
  "Things with two faults. The part that is declared first, the shape before
  a rule, the rule that is listed first, and a rule before the last step
  give the reason; in the last step, a value over its bound before an act
  over its bound. The time on the act is declared after the depot and
  before each of the maker's parts. A part that the form does not name
  comes before each, but for the version."
  [["a part that is not named, and a wrong version: the version is read before the part is called unknown" (assoc good :owner 1 :version 2) :unknown-version]
   ["a part that is not named, and a who that is a string: the part that the form does not know" (assoc good :owner 1 :who "alice") :unknown-part]
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
    (without-time (arrived 1 (assoc (value-at :note-1 :text) :lane :bob))) :no-door-time]
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
   ["a permission that is no uuid, and a depot that is not the act-id's: the shape comes before a rule"
    (assoc good :permission [(person :alice) :alice :alice] :depot :microbatch)
    :permission-not-an-id]
   ["257 facts, each with a value over the bound: the limit of the facts, before one text is made"
    (apply arrived 1 (map (fn [i] (value-at (keyword (str "note-" i)) :text over-the-bound)) (range 257))) :too-many-facts]
   ["a depot fact whose value is no depot word, and a key in the store's namespace that the store does not know: the key gives the word"
    (arrived 1 {:e :alice :k :softland.store/depot :v :fast} (value-at :note-1 :softland.store/owner)) :store-key-not-known]
   ["a wrong word of who may read a lane's acts, and a permission fact that does not sit on an id: the rules of a permission come first"
    (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v :owner}
             {:e (person :carol) :k :softland.store/permission :v (perm/permission-value bobs)}) :permission-fact-not-on-an-id]
   ["a wrong word of who may read a lane's acts, and a list that is no list: the rules of the three settings in their order"
    (arrived 1 {:e :alice :k :softland.store/builds-on :v :base}
             {:e :alice :k :softland.store/who-may-read-acts :v :owner}) :who-may-read-acts-fact-not-members-or-everyone]
   ["65 lanes, and marked keys that are no set: the limit of the list before the shape of the marks"
    (arrived 1 {:e :alice :k :softland.store/marked-keys :v [:on]}
             {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "lane-" %)) (range 65))}) :builds-on-too-many-lanes]
   ["33 marked keys with one of the store's namespace among them: the limit first"
    (arrived 1 {:e :alice :k :softland.store/marked-keys :v (conj (set (map #(keyword (str "key-" %)) (range 32))) :softland.store/depot)}) :too-many-marked-keys]
   ["a mark of a key of the store's namespace, and a value over the bound under a plain key: a rule comes before the last step"
    (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store/depot}} (value-at :note-1 :text over-the-bound)) :marked-key-in-the-stores-namespace]
   ["a value over the bound, and two facts on one place: the rule's word"
    (arrived 1 (value-at :note-1 :text over-the-bound) (value-at :note-1 :text "b")) :place-named-twice]
   ["a list of lanes with a long id in it, and two facts on one place: the long id is the last rule"
    (arrived 1 {:e :alice :k :softland.store/builds-on :v [(long-id 257)]} (value-at :note-1 :text "a") (value-at :note-1 :text "b")) :place-named-twice]
   ["a value over the bound on an entity whose id is too long: the last rule comes before the last step"
    (arrived 1 (value-at (long-id 257) :text over-the-bound)) :id-too-long]
   ["a value over its bound in an act that is over its bound: the value's word, the first of the two"
    (assoc-in (full-act 0) [:facts 0 :v] (text-of-bytes 2100000)) :value-too-large]
   ["the same with the value as the last fact of the act: the value's word still"
    (assoc-in (full-act 0) [:facts 255 :v] (text-of-bytes 2100000)) :value-too-large]])

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
   ["an act of 256 full values whose accept would write 16,851,968 bytes of text into the rows of its facts" (full-act 0) :ok]
   ["the same act with one byte more in one value" (full-act 1) :value-too-large]
   ["an entity of 256 characters" (arrived 1 (value-at (long-id 256) :text)) :ok]
   ["an entity of 257 characters" (arrived 1 (value-at (long-id 257) :text)) :id-too-long]
   ["what a lane builds on, 64 lanes" (arrived 1 {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "lane-" %)) (range 64))}) :ok]
   ["what a lane builds on, 65 lanes" (arrived 1 {:e :alice :k :softland.store/builds-on :v (mapv #(keyword (str "lane-" %)) (range 65))}) :builds-on-too-many-lanes]
   ["32 marked keys" (arrived 1 {:e :alice :k :softland.store/marked-keys :v (set (map #(keyword (str "key-" %)) (range 32)))}) :ok]
   ["33 marked keys" (arrived 1 {:e :alice :k :softland.store/marked-keys :v (set (map #(keyword (str "key-" %)) (range 33)))}) :too-many-marked-keys]
   ["a lane of 256 characters in the list that a lane builds on, and a marked key of 256" (arrived 1 {:e :alice :k :softland.store/builds-on :v [(long-id 256)]} {:e :alice :k :softland.store/marked-keys :v #{(long-id 256)}}) :ok]
   ["an act under the uuid of a permission five deep" (assoc good :permission (perm/permission-id (deep-permission (person :alice) :alice 5))) :ok]
   ["an act under the uuid of a permission 32 deep" (assoc good :permission (perm/permission-id (deep-permission (person :alice) :alice 32))) :ok]
   ["an act under the uuid of a permission 32 deep that says what it allows"
    (assoc good :permission (perm/permission-id (perm/given-permission (person :alice) :alice (deep-permission (person :alice) :alice 31) :see-and-act))) :ok]
   ["an act whose permission is a written-out vector and no uuid" (assoc good :permission [(person :alice) :alice :alice]) :permission-not-an-id]
   ["a permission fact for a permission five deep" (arrived 1 (perm/permission-fact (deep-permission (person :bob) :alice 5))) :ok]
   ["a permission fact for a permission 32 deep" (arrived 1 (perm/permission-fact (deep-permission (person :bob) :alice 32))) :ok]
   ["a permission fact for a permission 32 deep that says see"
    (arrived 1 (perm/permission-fact (perm/given-permission (person :bob) :alice (deep-permission (person :bob) :alice 31) :see))) :ok]
   ["a permission fact whose value is 33 collections deep: it is no value of the form, so the shape's word and not the rule's"
    (arrived 1 {:e (uuid 9) :k :softland.store/permission :v (nested 33)}) :malformed]
   ["ids that hold UUIDs: a person's party id, the lane, the permission, the session, an entity"
    (let [who (person (uuid 31)) lane (uuid 32)]
      {:act-id [lane :streaming (uuid 1)] :depot :streaming declaration/time-part now :version 1 :who who :lane lane
       :permission (perm/permission-id (perm/first-permission who lane)) :session (uuid 33)
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
  lane."
  [["an id that a key can be made of: a keyword, a UUID; not a string, nil, a long keyword, a keyword that does not read back"
    (fn [] (mapv declaration/usable-id? [:alice (uuid 1) "alice" nil (long-id 257) (keyword "a b") (long-id 256)]))
    [true true false false false false true]]
   ["an act-id of a lane: its shape and its lane slot; anything else is none, and nothing throws"
    (fn [] (mapv #(declaration/act-id-in-lane? :alice %)
                 [(act-id 1) [:alice :microbatch (uuid 1)] [:bob :streaming (uuid 1)] [:alice :streaming]
                  nil 5 (throwing)]))
    [true true false false false false false]]])

;; ------------------------------------------------------------------ the stamp

(def ^:private stamp-cases
  "The cases of the stamp: the first stamp of a lane, the next one, a run
  of them, the class of a stamp, its limit, and what the function takes. A
  stamp holds no time: what a stamp is not made of is held where a decision
  is made, in `writes-cases` and `stamp-line-cases`."
  [["the first decision in a lane, with no last stamp: the stamp is 0, a long"
    (fn [] (let [first-stamp (stamp/next-stamp nil)] [first-stamp (.getSimpleName (class first-stamp))]))
    [0 "Long"]]
   ["a decision after a last stamp: that stamp plus one, a long also when the last stamp is given as an Integer"
    (fn [] (mapv (fn [last-stamp] (let [s (stamp/next-stamp last-stamp)] [s (.getSimpleName (class s))])) [0 41 (int 41)]))
    [[1 "Long"] [42 "Long"] [42 "Long"]]]
   ["1,000 decisions in a row in one lane: the stamps are 0, 1, 2 and on to 999, with no gap"
    (fn [] (= (range 1000) (take 1000 (iterate stamp/next-stamp (stamp/next-stamp nil)))))
    true]
   ["the limit: after the greatest long the next stamp wraps to the least, and nothing throws"
    (fn [] (stamp/next-stamp Long/MAX_VALUE))
    Long/MIN_VALUE]
   ["the stamp's namespace gives one function, and it takes the lane's last stamp alone: no time, and nothing of the act, goes into a stamp"
    (fn [] [(vec (sort (map name (keys (ns-publics 'softland.store.gate.stamp)))))
            (:arglists (meta #'stamp/next-stamp))])
    [["next-stamp"] '([last-stamp])]]])

;; -------------------------------------------------------------- the first act

(defn- making
  "A making act in its one form on arrival: for a lane, by `who`, naming no
  permission and giving a first permission held by `holder`, with the depot
  that the lane starts with. `first-act-cases` holds it against what the
  declaration's check gives for the same act as its maker sends it."
  [lane who holder]
  (let [permission (perm/first-permission holder lane {:given-by who})
        {:keys [e k v]} (perm/permission-fact permission)
        depot (first-act/depot-at-making lane)]
    {:act-id [lane depot (uuid 1)] :depot depot declaration/time-part now :version 1 :who who :lane lane
     :permission nil :session :s1 :based-on {} :because-of nil
     :facts [(arrived-fact lane :softland.store/depot depot)
             (arrived-fact e k v)]}))

(def ^:private first-act-rows
  "Acts into a lane that is not made. For each: the gate's depot, the act,
  and whether it has the form of a making act."
  (let [personal (making :alice :system (person :alice))
        group (making :team (person :alice) (person :alice))
        base (making :base :system :system)
        x-team (perm/first-permission (person :x) :team)
        above-team (perm/given-permission (person :alice) :team x-team {:given-by (person :alice) :under nil})
        elsewhere-team (perm/first-permission (person :alice) :team {:granted-in :elsewhere})
        member-team (perm/first-permission (person :alice) :team {:allows perm/member-allows})
        fact (fn [act i] (get-in act [:facts i]))
        as-arrived-perm (fn [p] (let [{:keys [e k v]} (perm/permission-fact p)] (arrived-fact e k v)))]
    [["a personal lane, made by the system for its person" :streaming personal true]
     ["a group's lane, made by the person who creates it" :streaming group true]
     ["the two facts in the other order" :streaming (update group :facts (comp vec reverse)) true]
     ["a lane whose first holder is the system" :streaming (making :tools :system :system) true]
     ["the base, made by the system" :microbatch base true]
     ["the base" :streaming base false]
     ["the base by a who that is not the system" :microbatch (assoc base :who (person :alice)) false]
     ["the base with a first permission that is not the system's" :microbatch (making :base :system (person :alice)) false]
     ["the base, made by a person who holds its first permission herself: she is the holder, as for a group's lane, and for the base the holder is the system"
      :microbatch (making :base (person :alice) (person :alice)) false]
     ["a lane that is not the base" :microbatch group false]
     ["a who that is neither the holder nor the system" :streaming (assoc group :who (person :bob)) false]
     ["no depot fact" :streaming (assoc group :facts [(fact group 1) (arrived-fact :note :text)]) false]
     ["no permission fact" :streaming (assoc group :facts [(fact group 0) (arrived-fact :note :text)]) false]
     ["one fact only" :streaming (assoc group :facts [(fact group 0)]) false]
     ["a third fact under a plain key beside the two" :streaming (update group :facts conj (arrived-fact :note :text)) false]
     ["a third fact that writes a setting: nothing is written at the making but the depot, the first permission, and an optional permission to anyone at all"
      :streaming (update group :facts conj (arrived-fact :team :softland.store/who-may-read-acts :everyone)) false]
     ["the depot microbatch for a lane that is not the base" :streaming (assoc-in group [:facts 0 :v] :microbatch) false]
     ["a depot fact on an entity that is not the lane" :streaming (assoc-in group [:facts 0 :e] :other) false]
     ["a making act that names a permission on the act"
      :streaming (assoc group :permission (perm/permission-id (perm/first-permission (person :alice) :team))) false]
     ["a first permission that is lent from another"
      :streaming (assoc-in group [:facts 1] (as-arrived-perm above-team))
      false]
     ["a first permission that covers another lane"
      :streaming (assoc-in group [:facts 1] (as-arrived-perm elsewhere-team))
      false]
     ["a first permission that allows less than an owner's"
      :streaming (assoc-in group [:facts 1] (as-arrived-perm member-team))
      false]]))

(def ^:private first-act-cases
  "The cases of the first act: the depot that a lane starts with, whether a
  lane starts with the other gate's depot, and one case for each row of
  `first-act-rows`."
  (concat
   [["the depot that a lane starts with: microbatch for the base, streaming for any other"
     (fn [] (mapv first-act/depot-at-making [:base :alice :team (uuid 1)]))
     [:microbatch :streaming :streaming :streaming]]
    ["a lane that starts with the other gate's depot: at the streaming gate's depot the base, and no other lane, a keyword or a UUID; at the microbatch gate's depot each lane but the base"
     (fn [] [(mapv #(first-act/starts-with-the-other-gates-depot? :streaming %) [:base :alice :team (uuid 1)])
             (mapv #(first-act/starts-with-the-other-gates-depot? :microbatch %) [:base :alice :team (uuid 1)])])
     [[true false false false] [false true true true]]]
    ["no act that has the form of a making act for a gate is for a lane that starts with the other gate's depot: each of the five such rows below, at its gate; and the base's making act at the streaming gate's depot is for such a lane"
     (fn [] [(mapv (fn [[_ gate-depot act _]] (first-act/starts-with-the-other-gates-depot? gate-depot (:lane act)))
                   (filter (fn [[_ _ _ making?]] making?) first-act-rows))
             (first-act/starts-with-the-other-gates-depot? :streaming (:lane (making :base :system :system)))])
     [[false false false false false] true]]
    ["the two ids that the form knows"
     (fn [] [first-act/system first-act/base])
     [:system :base]]
    ["the making acts of these cases are in the one form on arrival: each is what the declaration's check gives for the same act as its maker sends it"
     (fn [] (mapv (fn [form] (= form (act-of (-> form
                                                 (dissoc :permission :based-on :because-of)
                                                 (update :facts (fn [facts] (mapv #(select-keys % [:e :k :v]) facts)))))))
                  [(making :alice :system (person :alice)) (making :team (person :alice) (person :alice)) (making :base :system :system)]))
     [true true true]]
    ["what a lane starts with, where no fact says it: who may read its acts is members, a lane builds on nothing, no key is marked, and nothing for who may see or any other key; opening a lane at making is a third fact to anyone at all"
     (fn [] (let [open-group (let [{:keys [e k v]} (perm/open-fact :team (person :alice))]
                               (update (making :team (person :alice) (person :alice)) :facts conj (arrived-fact e k v)))]
              [(mapv #(first-act/starts-with % :softland.store/who-may-read-acts) [:base :alice :team (uuid 1)])
               (mapv #(first-act/starts-with % :softland.store/builds-on) [:base :alice])
               (mapv #(first-act/starts-with % :softland.store/marked-keys) [:base :alice])
               (mapv #(first-act/starts-with :alice %) [:softland.store/who-may-see :softland.store/depot :softland.store/permission :text])
               (first-act/making-act? :streaming open-group)]))
     [[:members :members :members :members] [[] []] [#{} #{}] [nil nil nil nil] true]]]
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
  permission holds: of who wrote it and of the id of the permission that the
  act named. Written out here, where a gate makes them once for an act."
  [":person.alice" (str (perm/permission-id alice-own))])

(def ^:private written-by-alice
  "The two fields that each row of a fact written by `writer` holds: who
  wrote it and the id of the permission that the act named, each as its
  printed text."
  {:who ":person.alice" :permission (str (perm/permission-id alice-own))})

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
        uuid-party (person (uuid 77))
        uuid-who (declaration/id-text uuid-party)
        uuid-perm (uuid 78)
        by-uuid (fact-row [uuid-who (declaration/id-text uuid-perm)]
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
      (fn [] [(classes (record/answer-row nil (int 900) (int 1700001) digest)) (classes (record/answer-row :lane-not-made 900 now digest))])
      [{:stamp "Long" :time "Long" :digest "byte[]"} {:stamp "Long" :time "Long" :digest "byte[]" :refused "Keyword"}]]
     ["the time in an answer's row is the time given, as it is: a time of 0 and a time far ahead of every clock; a thing with no usable time gives a row with no :time, refused or accepted"
      (fn [] [(:time (record/answer-row nil 900 0 digest)) (:time (record/answer-row nil 900 far-ahead digest))
              (record/answer-row :no-door-time 900 nil digest) (contains? (record/answer-row nil 900 nil digest) :time)])
      [0 far-ahead {:refused :no-door-time :stamp 900 :digest digest} false]]
     ["an answer as a caller gets it, accepted and refused, with the time on the thing; a nil time for a row with none; no row is nothing yet"
      (fn [] [(record/answer-of (record/answer-row nil 900 now digest))
              (record/answer-of (record/answer-row :lane-not-made 901 now digest))
              (record/answer-of (record/answer-row :no-door-time 902 nil digest))
              (record/answer-of nil)])
      [{:verdict :accepted :reason nil :stamp 900 :time now :digest (text/hex digest)}
       {:verdict :refused :reason :lane-not-made :stamp 901 :time now :digest (text/hex digest)}
       {:verdict :refused :reason :no-door-time :stamp 902 :time nil :digest (text/hex digest)}
       nil]]
     ["the row of a fact with a value under a plain key: its value is the fact's text as it is, a String, and the row gives the UTF-8 of it back; the fact that wrote it, the stamp, the time on the act, who wrote it and the id of the permission that the act named; each field of its declared class, and none a byte array"
      (fn [] [value-row (classes value-row) (record/plain-bytes-of value-row)])
      [(merge {:value "\"a text\"" :by-act u :by-index 2 :stamp 900 :time 1700001} written-by-alice)
       {:value "String" :by-act "UUID" :by-index "Long" :stamp "Long" :time "Long" :who "String" :permission "String"}
       (utf8 "\"a text\"")]]
     ["the row of a fact under a store key: the text of its value as a String, whose bytes the row gives back; the fact that wrote it, the stamp, the time, who and the permission id, and no other field; an act that named no permission omits :permission"
      (fn [] [plain-row (record/plain-bytes-of plain-row)
              (contains? (fact-row [":system" nil] {:e :alice :k :softland.store/depot :v :streaming :replaces nil :text ":streaming"} u 0 900 now) :permission)])
      [(merge {:value ":streaming" :by-act u :by-index 0 :stamp 900 :time now} written-by-alice)
       (utf8 ":streaming")
       false]]
     ["the row takes the fact's text and reads nothing of the fact's value: a fact that holds a value and no text gives the row of a retract, and one that holds a text and another value gives the text"
      (fn [] [(contains? (fact-row writer {:e :note-1 :k :text :v :a-value :replaces nil :text nil} u 0 900 now) :value)
              (:value (fact-row writer {:e :alice :k :softland.store/depot :v :streaming :replaces nil :text ":another"} u 0 900 now))])
      [false ":another"]]
     ["the plain bytes of a row's String are its UTF-8, written out here byte by byte: characters of two, three and four bytes; a String that begins with U+0000 has that byte first; nothing for no row and for the row of a retract"
      (fn [] [(record/plain-bytes "é✓𝄞") (record/plain-bytes "\u0000a")
              (record/plain-bytes-of nil) (record/plain-bytes-of retract-row)])
      [(the-bytes -61 -87 -30 -100 -109 -16 -99 -124 -98) (the-bytes 0 97) nil nil]]
     ["the row of a retract: no value, and the row is there; it holds who retracted, under which permission id and at what time, and no other field"
      (fn [] retract-row)
      (merge {:by-act u :by-index 1 :stamp 900 :time now} written-by-alice)]
     ["who wrote a row and the id of the permission that its act named are each the printed text of an id: who reads back as that party keyword and the permission as a UUID; nothing for no row, for a row with neither, and for a text that is no id or no UUID"
      (fn [] [(record/who-of value-row) (record/permission-of value-row)
              (:who by-uuid) (record/who-of by-uuid) (record/permission-of by-uuid)
              (record/by-holder-key :alice (person :bob) nil uuid-perm)
              (record/by-holder-key :alice (person :bob) :sit-7 uuid-perm)
              (record/permission-of-by-holder-key (record/by-holder-key :alice (person :bob) :sit-7 uuid-perm))
              (record/who-of nil) (record/permission-of nil) (record/who-of (row-holding "x")) (record/permission-of (row-holding "x"))
              (record/who-of {:who "\"alice\""}) (record/who-of {:who "(((("}) (record/who-of {:who ""})
              (record/permission-of {:permission ":person.alice"}) (record/permission-of {:permission "[:alice :alice]"})])
      [(person :alice) (perm/permission-id alice-own)
       ":person.00000000-0000-0000-0000-00000000004d" uuid-party uuid-perm
       (str ":alice :person.bob * " uuid-perm)
       (str ":alice :person.bob :sit-7 " uuid-perm)
       uuid-perm
       nil nil nil nil nil nil nil nil nil]]
     ["which fact wrote a row: the one named; not another index, uuid, lane or depot; not an empty place"
      (fn [] (mapv #(record/written-by? :streaming :alice value-row %)
                   [[[:alice :streaming u] 2] [[:alice :streaming u] 1] [[:alice :streaming (uuid 6)] 2]
                    [[:bob :streaming u] 2] [[:alice :microbatch u] 2]]))
      [true false false false false]]
     ["no fact wrote an empty place"
      (fn [] (record/written-by? :streaming :alice nil [[:alice :streaming u] 2]))
      false]
     ["a row holds a value when its String is the one text of that value: a depot word, a tool's value, and a map given in another order and with a decimal that has a trailing zero; not another value, not the text of the value as a string, not a retract, not an empty place"
      (fn [] [(record/holds-this-value? plain-row :streaming) (record/holds-this-value? plain-row :microbatch)
              (record/holds-this-value? value-row "a text") (record/holds-this-value? value-row :streaming)
              (record/holds-this-value? (row-holding "{:a [1 2] :b 1.5M}") (array-map :b 1.50M :a [1 2]))
              (record/holds-this-value? plain-row ":streaming")
              (record/holds-this-value? retract-row :streaming) (record/holds-this-value? nil :streaming)])
      [true false true false true false false false]]
     ["the first form of a row's String, read back: a depot, a permission value map, a tool's value; nothing for a retract, for no row, and for a String that reads as no form"
      (fn [] [(record/value-of plain-row)
              (record/value-of (row-holding (text/text (perm/permission-value bobs))))
              (record/value-of value-row) (record/value-of retract-row) (record/value-of nil)
              (record/value-of (row-holding "(((("))])
      [:streaming (perm/permission-value bobs) "a text" nil nil nil]]
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
              (record/holds-this-value? (row-holding no-text) "a text")
              (record/same-row? (row-holding no-text) (row-holding no-text))
              (record/same-row? (row-holding no-text) (row-holding (str no-text "!")))
              (record/holds-this-value? (row-holding ":c1") :c1)])
      [[:not-a-value] false true false true]]
     ["the row at a lane's marked-keys place holds, beside the set that a maker wrote, the stamp of each marked key as one text; it reads back as a map; nothing is marked for no row, for a row without the field and for a text that is no such map"
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
     ["a key of the facts by value: lane, key, the stamp since which the key is marked, the value digest, then the entity; its row is the fact's row without its value, with who, the permission id and the time as they were given"
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
     ["no run holds a row of another value, lane, key or stamp: for a keyword, a UUID, a string with spaces, a number, a map and a value whose text is 65,536 bytes, the keys of the five others, and of the same value in another lane, under another key and under another stamp, are outside its run"
      (fn [] (let [values [:c1 (uuid 9) "a string with spaces" 42 {:a 1} (text-of-bytes 65536)]
                   in-run? (fn [[from to] k] (and (<= (compare from k) 0) (neg? (compare k to))))
                   key-of (fn [lane k since v] (record/by-value-key lane k since (record/value-digest-of v) :b1))]
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
     ["a key back to its parts: a printed id to the id, a place's key to lane, entity and key, a key of the facts by value to its entity; nothing for a text that is none"
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
     ["the head of the read by act-id: the depot and the key; nothing for what is no act-id of the lane"
      (fn [] [(record/answer-read :alice [:alice :streaming u]) (record/answer-read :alice [:alice :microbatch u])
              (record/answer-read :bob [:alice :streaming u]) (record/answer-read :alice [:alice :streaming])
              (record/answer-read nil nil) (record/answer-read (long-id 257) [(long-id 257) :streaming u])
              (record/answer-read :alice 5)])
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":alice 00000000-0000-0000-0000-000000000005"]
       nil nil nil nil nil]]
     ["the routing id of an act-id is its lane, for an act-id of each depot: a streaming one and a microbatch one, of a lane that is a keyword and of one that is a UUID. The uuid plays no part: two act-ids of one lane have one routing id"
      (fn [] [(record/routing-id [:alice :streaming u]) (record/routing-id [:base :microbatch u])
              (record/routing-id [(uuid 3) :streaming u]) (record/routing-id [(uuid 3) :microbatch u])
              (record/routing-id [:alice :microbatch u])
              (= (record/routing-id [:base :microbatch u]) (record/routing-id [:base :microbatch (uuid 6)]))])
      [:alice :base (uuid 3) (uuid 3) :alice true]]
     ["no routing id for a thing that is no act-id: two slots, four slots, a depot word that is none, a uuid slot that holds a text, a list, a number, a text, nothing, a thing that throws when it is read; and nothing throws"
      (fn [] (mapv record/routing-id [[:base :microbatch] [:base :microbatch u u] [:base :fast u] [:base :microbatch (str u)]
                                      (list :base :microbatch u) 5 "an act-id" nil (throwing)]))
      (vec (repeat 9 nil))]
     ["the head of the read by act-id, with the routing id: with the act-id's lane it gives the depot and the key, which is what `answer-read` gives, for an act-id of each depot; the routing id of an act-id is the one that its own read takes"
      (fn [] (let [of-alice [:alice :streaming u]
                   of-the-base [:base :microbatch u]]
               [(record/answer-read-by-routing-id :alice of-alice)
                (record/answer-read-by-routing-id :base of-the-base)
                (mapv (fn [id] (= (record/answer-read (nth id 0) id) (record/answer-read-by-routing-id (record/routing-id id) id)))
                      [of-alice of-the-base])]))
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":base 00000000-0000-0000-0000-000000000005"]
       [true true]]]
     ["nothing for another routing id: the act-id's uuid, another lane, nothing; nothing for a thing that is no act-id; nothing for a lane over the limit, whose act-id has a routing id all the same"
      (fn [] [(record/answer-read-by-routing-id u [:base :microbatch u])
              (record/answer-read-by-routing-id u [:alice :streaming u])
              (record/answer-read-by-routing-id :alice [:base :microbatch u])
              (record/answer-read-by-routing-id nil [:base :microbatch u])
              (record/answer-read-by-routing-id nil nil)
              (record/answer-read-by-routing-id 5 5)
              (record/answer-read-by-routing-id (long-id 257) [(long-id 257) :streaming u])
              (= (long-id 257) (record/routing-id [(long-id 257) :streaming u]))])
      [nil nil nil nil nil nil nil true]]
     ["the two parts of an answer's key, for a reader of the answers' keys: the text of the lane and the uuid; the text gives the lane back, a keyword, a keyword with a namespace, a UUID"
      (fn [] (mapv (fn [lane] (let [[lane-text act-uuid] (record/answer-key-parts (record/answer-key lane u))]
                                 [lane-text act-uuid (= lane (record/id-of-text lane-text))]))
                   [:alice :tool/lane (uuid 3)]))
      [[":alice" u true] [":tool/lane" u true] ["00000000-0000-0000-0000-000000000003" u true]]]
     ["a lane whose text is 256 characters comes back from its key too"
      (fn [] (let [lane (long-id 256)
                   [lane-text act-uuid] (record/answer-key-parts (record/answer-key lane u))]
               [(count lane-text) act-uuid (= lane (record/id-of-text lane-text))]))
      [256 u true]]
     ["nothing for a text that is no answer's key: a place's key of three parts, one part, a lane with no text, a uuid that is not written as a UUID prints, a uuid in capitals, no character, nothing, a number, a thing that throws; and nothing throws"
      (fn [] (mapv record/answer-key-parts
                   [":alice :note-1 :text" ":alice" (str " " u) ":alice 5-5-5-5-5" (str ":alice " (str/upper-case (str (UUID. -1 -1))))
                    "" nil 5 (throwing)]))
      (vec (repeat 9 nil))]
     ["the head of the read of a depot: the lane's depot place; nothing for what is no id"
      (fn [] [(record/depot-read :alice) (record/depot-read "alice") (record/depot-read nil)
              (record/depot-read (long-id 257)) (record/depot-read [:alice])])
      [":alice :alice :softland.store/depot" nil nil nil nil]]]))

;; --------------------------------------------------------------- the decision

(defn- permission-row
  "The row of a permission fact, as an accept writes it: by the making act
  of Alice's lane, with the time `now`, and with the stamp 0 or the stamp
  given."
  ([permission] (permission-row permission 0))
  ([permission stamp]
   (let [{:keys [e k v]} (perm/permission-fact permission)]
     (fact-row writer (arrived-fact e k v) (uuid 90) 1 stamp now))))

(defn- end-row
  "The row of an end at a permission's place, as an accept writes it
  for a permission fact with no value: a row with no value, with the stamp
  given."
  [permission stamp]
  (let [{:keys [e k]} (perm/end-fact permission)]
    (fact-row writer (arrived-fact e k) (uuid 91) 0 stamp now)))

(def ^:private alices-place
  "The key of the place of Alice's first permission in her own lane."
  (chain/place :alice (perm/permission-id alice-own)))

(def ^:private depot-row
  "The row of the depot fact of Alice's lane."
  (fact-row [":system" nil] (arrived-fact :alice :softland.store/depot :streaming) (uuid 90) 0 0 now))

(def ^:private bobs-see
  "Bob's permission in Alice's lane, beneath hers, that says see."
  (perm/given-permission (person :bob) :alice alice-own :see))

(def ^:private bobs-see-and-act
  "Bob's permission in Alice's lane, beneath hers, as a member."
  (perm/given-permission (person :bob) :alice alice-own :see-and-act))

(def ^:private base-on-the-streaming-depot
  "An act into the base as it arrives on the streaming depot: by the system
  under its own permission in the base, with one fact that has a value. It
  is no stray on that depot, and the declaration's check takes it."
  {:act-id [:base :streaming (uuid 1)] :depot :streaming declaration/time-part now
   :version 1 :who :system :lane :base :permission (perm/permission-id (perm/first-permission :system :base)) :session :s1
   :facts [(value-at :doc-1 :note)]})

(def ^:private standing-rows
  "For each: the act, the rows map handed to `standing`, then the word of
  `standing`, and whether the act makes the lane. Each row is held at the
  streaming gate's depot. No row says whether the lane is the other gate's:
  `standing` works that out from the gate's depot and the act's lane. Two
  rows hand a row of the base with an act into the base. No gate writes such
  a row into the streaming gate's record; they hold what `standing` reads
  first. `base-standing-rows` holds the standing at the microbatch gate's
  depot."
  (let [alices (act-of good)
        bobs-place (chain/place :alice (perm/permission-id bobs))
        bobs-see-place (chain/place :alice (perm/permission-id bobs-see))
        bobs-see-and-act-place (chain/place :alice (perm/permission-id bobs-see-and-act))
        bobs-act (assoc alices :who (person :bob) :permission (perm/permission-id bobs))
        personal (making :alice :system (person :alice))
        group (making :team (person :alice) (person :alice))
        into-base (act-of base-on-the-streaming-depot)
        base-making (making :base :system :system)
        systems-own (perm/first-permission :system :base)
        systems-fact (perm/permission-fact systems-own)
        systems-texts [":system" nil]
        a-depot-row-of-the-base (fact-row systems-texts (arrived-fact :base :softland.store/depot :microbatch)
                                          (uuid 90) 0 0 now)
        a-permission-row-of-the-base (fact-row systems-texts (arrived-fact (:e systems-fact) (:k systems-fact) (:v systems-fact))
                                               (uuid 90) 1 0 now)
        anyone-write {:id (uuid 95)
                      :value {:holder :anyone-signed-in
                              :allows {:see true :write :all}
                              :covers [:default-group :alice]
                              :given-by (person :alice)
                              :under (perm/permission-id alice-own)}}
        anyone-write-place (chain/place :alice (uuid 95))
        member-team (perm/first-permission (person :alice) :team {:allows perm/member-allows})
        member-team-fact (perm/permission-fact member-team)
        depot-key ":alice :alice :softland.store/depot"]
    [["the act's permission is at its own id's place" alices {alices-place (permission-row alice-own)} nil false]
     ["an act under a permission lent from another, with both at their places"
      bobs-act {bobs-place (permission-row bobs 1) alices-place (permission-row alice-own)} nil false]
     ["an act that names another id than the one whose place holds a permission"
      (assoc alices :permission (uuid 99)) {alices-place (permission-row alice-own) depot-key depot-row} :permission-never-given false]
     ["the place is empty and the lane is made: the record does not hold the permission"
      bobs-act {depot-key depot-row} :permission-never-given false]
     ["an act under Alice's permission by a who that is not Alice, in a lane that is made"
      (assoc alices :who (person :mallory)) {alices-place (permission-row alice-own) depot-key depot-row} :who-not-the-permissions-holder false]
     ["the system acts again in a personal lane that it made"
      personal {depot-key depot-row} :no-permission false]
     ["a making act by the holder for a lane that is made: it names no permission and makes nothing"
      (making :alice (person :alice) (person :alice)) {depot-key depot-row} :no-permission false]
     ["an act into a lane that is not made" alices {} :lane-not-made false]
     ["a right making act of a personal lane, into a lane that is not made" personal {} nil true]
     ["a right making act of a group's lane" group {} nil true]
     ["an act into the base, a lane of the other gate, of which this gate's record holds nothing" into-base {}
      :lane-belongs-to-the-other-gate false]
     ["the base's making act, which has the form of a making act for the other gate and not for this one: it makes nothing here, and the word is that of a lane of the other gate, not that of a lane that is not made"
      base-making {} :lane-belongs-to-the-other-gate false]
     ["that a lane is the other gate's counts only when both rows are nil: an act into the base with the row of its permission there stands"
      into-base {(chain/place :base (perm/permission-id systems-own)) a-permission-row-of-the-base} nil false]
     ["and with a row at the base's depot place there, the lane reads as made here: the word of a permission that the record does not hold"
      into-base {":base :base :softland.store/depot" a-depot-row-of-the-base} :permission-never-given false]
     ["a holder of see who names her permission's id: standing hears her, and the places check what she writes"
      (assoc alices :who (person :bob) :permission (perm/permission-id bobs-see))
      {bobs-see-place (permission-row bobs-see 1) alices-place (permission-row alice-own)} nil false]
     ["a holder of see who names an id that nobody gave: it was never given"
      bobs-act {bobs-see-place (permission-row bobs-see 1) depot-key depot-row} :permission-never-given false]
     ["a holder of see who ends her own permission in an act that names none"
      (act-of (-> (arrived 1 (perm/end-fact bobs-see)) (assoc :who (person :bob)) (dissoc :permission)))
      {depot-key depot-row bobs-see-place (permission-row bobs-see 1)} nil false]
     ["a holder of a member's permission, named by its id"
      (assoc alices :who (person :bob) :permission (perm/permission-id bobs-see-and-act))
      {bobs-see-and-act-place (permission-row bobs-see-and-act 1) alices-place (permission-row alice-own)} nil false]
     ["a person who acts under a permission held by anyone signed in"
      (assoc alices :who (person :zed) :permission (uuid 95))
      {anyone-write-place (permission-row anyone-write 1)} nil false]
     ["an agent is never everyone on its own: naming a permission held by anyone signed in is refused"
      (assoc alices :who (agent :bot) :permission (uuid 95))
      {anyone-write-place (permission-row anyone-write 1)} :who-not-the-permissions-holder false]
     ["a holder of see whose act is by another who: the word of the holder comes first"
      (assoc alices :who (person :mallory) :permission (perm/permission-id bobs-see))
      {bobs-see-place (permission-row bobs-see 1) depot-key depot-row} :who-not-the-permissions-holder false]
     ["a making act whose first permission allows less than an owner's, into a lane that is not made: it is no making act"
      (assoc-in group [:facts 1] (arrived-fact (:e member-team-fact) (:k member-team-fact) (:v member-team-fact)))
      {} :lane-not-made false]]))

(def ^:private standing-cases
  "The cases of the standing at the streaming gate's depot: the two places
  that are read, an act into the base as it arrives on that depot, what a
  chain of permissions allows now, and one case for each row of
  `standing-rows`. `other-gate-cases` holds a lane of the other gate for
  both gates."
  (concat
   [["the places that the standing reads: the act's permission id, then the lane's depot, and nil for an act that names no permission"
     (fn [] [(decision/permission-place (act-of good))
             (decision/permission-place (act-of (dissoc good :permission)))
             (decision/depot-place (act-of good))])
     [alices-place nil ":alice :alice :softland.store/depot"]]
    ["an act into the base on the streaming depot is no stray there, and the declaration's check, the one check before the standing, takes it: it is the standing that has the word"
     (fn [] [(declaration/stray-reason :streaming base-on-the-streaming-depot)
             (verdict (declaration/check base-on-the-streaming-depot))])
     [nil :ok]]
    ["what a permission allows at this moment, read from its chain's values: a standing permission holds what it says, and a lent permission is capped by each permission above it"
     (fn [] [(chain/allows-now [(perm/permission-value alice-own)])
             (chain/allows-now [(perm/permission-value bobs) (perm/permission-value alice-own)])
             (chain/allows-now [(perm/permission-value bobs-see) (perm/permission-value alice-own)])
             (chain/allows-now [(perm/permission-value (perm/given-permission (agent :bot) :alice bobs :see-and-act))
                                (perm/permission-value bobs-see)
                                (perm/permission-value alice-own)])])
     [declaration/owner-allows perm/member-allows {:see true} {:see true}]]]
   (map (fn [[what act rows word makes?]]
          [(str "standing: " what)
           (fn [] [(decision/standing :streaming act rows nil)
                   (decision/makes-the-lane? :streaming act rows)])
           [word makes?]])
        standing-rows)))

(def ^:private chain-cases
  "The cases of a permission that ends: when a permission stands
  (`softland.store.gate.chain`), which rows the standing of an act wants in
  steps, and the word of an act whose permission, or one that it is lent
  from, has ended."
  (let [carols-here (perm/given-permission (person :carol) :alice bobs)
        carols-standing (perm/given-permission (person :carol) :alice bobs {:lent-from nil})
        bobs-2 (perm/given-permission (person :bob) :alice alice-own {:salt 2})
        carols-2 (perm/given-permission (person :carol) :alice bobs-2)
        bobs-place (chain/place :alice (perm/permission-id bobs))
        bobs-2-place (chain/place :alice (perm/permission-id bobs-2))
        bobs-see-place (chain/place :alice (perm/permission-id bobs-see))
        carols-place (chain/place :alice (perm/permission-id carols-here))
        carols-standing-place (chain/place :alice (perm/permission-id carols-standing))
        carols-2-place (chain/place :alice (perm/permission-id carols-2))
        alices (act-of good)
        bobs-act (assoc alices :who (person :bob) :permission (perm/permission-id bobs))
        bobs-2-act (assoc alices :who (person :bob) :permission (perm/permission-id bobs-2))
        carols-act (assoc alices :who (person :carol) :permission (perm/permission-id carols-here))
        carols-standing-act (assoc alices :who (person :carol) :permission (perm/permission-id carols-standing))
        carols-2-act (assoc alices :who (person :carol) :permission (perm/permission-id carols-2))
        rows (fn [alice-stamp bob-row carol-stamp]
               {alices-place (permission-row alice-own alice-stamp)
                bobs-place bob-row
                carols-place (permission-row carols-here carol-stamp)})
        word (fn [act rmap]
               (decision/standing :streaming act rmap nil))]
    [["the chain of a permission is climbed from its rows one lent-from at a time: values, ids, wants when a row is missing, and whole? when the top stands on its own"
      (fn [] [(chain/climb :alice (perm/permission-id carols-here) {carols-place (permission-row carols-here 2)})
              (chain/climb :alice (perm/permission-id carols-here)
                           {carols-place (permission-row carols-here 2)
                            bobs-place (permission-row bobs 1)
                            alices-place (permission-row alice-own 0)})
              (chain/climb :alice (perm/permission-id alice-own) {alices-place (permission-row alice-own 0)})
              (chain/place :alice (perm/permission-id carols-here))])
      [{:values [(perm/permission-value carols-here)] :ids [(perm/permission-id carols-here)] :wants bobs-place}
       {:values [(perm/permission-value carols-here) (perm/permission-value bobs) (perm/permission-value alice-own)]
        :ids [(perm/permission-id carols-here) (perm/permission-id bobs) (perm/permission-id alice-own)]
        :whole? true}
       {:values [(perm/permission-value alice-own)] :ids [(perm/permission-id alice-own)] :whole? true}
       carols-place]]
     ["one permission is above another when the lower is lent from it, directly or further up, as the rows say: not itself, not one that stands on its own, not above an end"
      (fn [] (let [rmap {carols-place (permission-row carols-here 2)
                         carols-standing-place (permission-row carols-standing 2)
                         bobs-place (permission-row bobs 1)
                         alices-place (permission-row alice-own 0)}]
               [(chain/above? :alice (perm/permission-id bobs) (perm/permission-id carols-here) rmap)
                (chain/above? :alice (perm/permission-id alice-own) (perm/permission-id carols-here) rmap)
                (chain/above? :alice (perm/permission-id carols-here) (perm/permission-id carols-here) rmap)
                (chain/above? :alice (perm/permission-id bobs) (perm/permission-id carols-standing) rmap)
                (chain/above? :alice (perm/permission-id alice-own) (perm/permission-id carols-here)
                              (assoc rmap bobs-place (end-row bobs 3)))]))
      [true true false false false]]
     ["the rows that the standing wants one step at a time: the permission's place, then each lent-from above it, or the depot when empty, and none when holder or sitting already refuses"
      (fn [] [(decision/standing-wanted carols-act {})
              (decision/standing-wanted carols-act {carols-place (permission-row carols-here 2)})
              (decision/standing-wanted carols-act {carols-place (permission-row carols-here 2) bobs-place (permission-row bobs 1)})
              (decision/standing-wanted carols-act {carols-place (end-row carols-here 3)})
              (decision/standing-wanted carols-act {carols-place nil})
              (decision/standing-wanted (assoc carols-act :who (person :mallory)) {carols-place (permission-row carols-here 2)})
              (decision/standing-wanted alices {alices-place (permission-row alice-own)})])
      [[carols-place] [bobs-place] [alices-place] [] [":alice :alice :softland.store/depot"] [] []]]
     ["a lent permission stands when its row holds a value and each permission that it is lent from stands"
      (fn [] (word carols-act (rows 0 (permission-row bobs 1) 2)))
      nil]
     ["Bob's permission is ended: Bob's own act is refused, with the word of an end"
      (fn [] (word bobs-act (rows 0 (end-row bobs 3) 2)))
      :permission-ended]
     ["what was lent from it falls with it, while a permission to a person that stands on its own stays when its giver's ends"
      (fn [] [(word carols-act (rows 0 (end-row bobs 3) 2))
              (word carols-standing-act {alices-place (permission-row alice-own 0)
                                         bobs-place (end-row bobs 3)
                                         carols-standing-place (permission-row carols-standing 2)})])
      [:permission-above-ended nil]]
     ["Bob is given a new permission on a new id after the end: his act under the new id is taken"
      (fn [] (word bobs-2-act {alices-place (permission-row alice-own 0)
                               bobs-place (end-row bobs 3)
                               bobs-2-place (permission-row bobs-2 4)}))
      nil]
     ["and Carol's lent permission does not come back: it still names the ended permission's id"
      (fn [] (word carols-act {alices-place (permission-row alice-own 0)
                               bobs-place (end-row bobs 3)
                               bobs-2-place (permission-row bobs-2 4)
                               carols-place (permission-row carols-here 2)}))
      :permission-above-ended]
     ["Carol is given hers again, after Bob's second give: her act is taken"
      (fn [] (word carols-2-act {alices-place (permission-row alice-own 0)
                                 bobs-2-place (permission-row bobs-2 4)
                                 carols-2-place (permission-row carols-2 5)}))
      nil]
     ["Bob is given another permission, one that says see: Carol's names the one that ended, and does not stand"
      (fn [] (word carols-act {alices-place (permission-row alice-own 0)
                               bobs-place (end-row bobs 3)
                               bobs-see-place (permission-row bobs-see 4)
                               carols-place (permission-row carols-here 2)}))
      :permission-above-ended]
     ["a row above that is missing from what was read is as an empty place: the permission does not stand"
      (fn [] (word carols-act (rows 0 nil 2)))
      :permission-above-ended]
     ["a holder of see beneath an end: the word of the end comes before the word of what her permission allows"
      (fn [] (let [carols-see-here (perm/given-permission (person :carol) :alice bobs :see)
                   carols-see-place (chain/place :alice (perm/permission-id carols-see-here))]
               (decision/standing :streaming
                                  (assoc alices :who (person :carol) :permission (perm/permission-id carols-see-here))
                                  {carols-see-place (permission-row carols-see-here 2)
                                   bobs-place (end-row bobs 3)
                                   alices-place (permission-row alice-own 0)}
                                  nil)))
      :permission-above-ended]
     ["`stands?` reads the permission's own row from the rows too: Alice's stands with her row, Bob's with both, and nothing stands with no row"
      (fn [] [(chain/stands? :alice (perm/permission-id alice-own) {alices-place (permission-row alice-own)})
              (chain/stands? :alice (perm/permission-id bobs) {alices-place (permission-row alice-own) bobs-place (permission-row bobs 1)})
              (chain/stands? :alice (perm/permission-id bobs) {alices-place (end-row alice-own 3) bobs-place (permission-row bobs 1)})
              (chain/stands? :alice (perm/permission-id bobs) {bobs-place (permission-row bobs 1)})
              (chain/stands? :alice (perm/permission-id alice-own) {})])
      [true true false false false]]]))

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
   ["an act holds no clock of its maker's: a thing that carries :claimed-when, or :subjects, is refused as any thing with a part that the form does not name"
    (fn [] (mapv #(verdict (declaration/check %))
                 [(assoc good :claimed-when Long/MAX_VALUE) (assoc good :subjects #{(person :alice)})]))
    [:unknown-part :unknown-part]]])

(defn- row-by
  "The row that the fact `index` of the act `(act-id n)` wrote: a value at
  :note-1 :text. Its stamp is 200, later than the stamp 100 since which the
  cases mark a key, so that such a row before under a marked key has an
  entry in the facts by value; and its time is `now`."
  [n index]
  (fact-row writer (arrived-fact :note-1 :text "a text before") (uuid n) index 200 now))

(defn- setting-row
  "The row that the fact 0 of the act `(act-id n)` wrote under a store key on
  Alice's lane, with the stamp and the time of `row-by`."
  [n k v]
  (fact-row writer (arrived-fact :alice k v) (uuid n) 0 200 now))

(def ^:private places-rows
  "For each: the facts of Alice's act, the rows of the places read (by
  entity and key), and the word of `places`."
  (let [replacing (fn [fact-id] (assoc (value-at :note-1 :text) :replaces fact-id))
        retract-row (fact-row writer (arrived-fact :note-1 :text) (uuid 8) 0 200 now)
        reads-acts :softland.store/who-may-read-acts
        on :softland.store/builds-on
        marks :softland.store/marked-keys
        carols-lent (perm/given-permission (person :carol) :alice bobs)
        bobs-id (perm/permission-id bobs)
        bobs-see-id (perm/permission-id bobs-see)
        carols-id (perm/permission-id carols-lent)
        bobs-lowered {:id bobs-id :value (assoc (perm/permission-value bobs) :allows {:see true})}
        bobs-raised {:id bobs-see-id :value (assoc (perm/permission-value bobs-see) :allows perm/member-allows)}]
    [["a value that names nothing, on an empty place: never stale" [(value-at :note-1 :text)] {} nil]
     ["a value that replaces the current fact of its place" [(replacing [(act-id 7) 0])] {[:note-1 :text] (row-by 7 0)} nil]
     ["a retract that replaces the current fact" [{:e :note-1 :k :text :replaces [(act-id 7) 0]}] {[:note-1 :text] (row-by 7 0)} nil]
     ["a value that replaces a retract, which is the current fact of its place" [(replacing [(act-id 8) 0])] {[:note-1 :text] retract-row} nil]
     ["a value that replaces an earlier fact of the place" [(replacing [(act-id 6) 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces another index of the same act" [(replacing [(act-id 7) 1])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces something on an empty place" [(replacing [(act-id 7) 0])] {} :replaced-fact-not-current]
     ["a value that replaces a fact of another lane" [(replacing [[:bob :streaming (uuid 7)] 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces a fact of the other depot" [(replacing [[:alice :microbatch (uuid 7)] 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a depot fact on an entity that is not the lane" [{:e :bob :k :softland.store/depot :v :streaming}] {} :setting-not-on-the-lane]
     ["a depot retract on an entity that is not the lane" [{:e :bob :k :softland.store/depot}] {} :setting-not-on-the-lane]
     ["a permission given under the act's own, on an empty id" [(perm/permission-fact bobs)] {} nil]
     ["a holder gives herself a second permission on a new id while her first stands"
      [(perm/permission-fact (perm/given-permission (person :alice) :alice alice-own))]
      {}
      nil]
     ["a permission given that covers another lane"
      [(perm/permission-fact (perm/given-permission (person :bob) :team alice-own))] {} :permission-covers-more-than-the-acts-own]
     ["a permission given by one who is not the act's who"
      [(perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:given-by (person :carol)}))] {} :permission-not-given-by-the-acts-who]
     ["a permission given to a person that stands on its own, with no lent-from"
      [(perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:lent-from nil}))] {} nil]
     ["a permission given with under naming another permission than the act's own"
      [(perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:under (uuid 98) :lent-from nil}))] {}
      :permission-not-under-the-acts-own]
     ["an in-place change at an id that holds the same allows: a change that changes nothing is refused"
      [(perm/permission-fact bobs)]
      {[bobs-id :softland.store/permission] (permission-row bobs 1)} :change-changes-nothing]
     ["an end of a permission, by the holder above it: a permission fact with no value, at a place that holds a permission"
      [(perm/end-fact bobs)] {[bobs-id :softland.store/permission] (permission-row bobs 1)} nil]
     ["an end that names the give which it ends"
      [(perm/end-fact bobs {:replaces [[:alice :streaming (uuid 90)] 1]})]
      {[bobs-id :softland.store/permission] (permission-row bobs 1)} nil]
     ["an end that names the give at the place, where another act wrote the row: the word of a stale fact"
      [(perm/end-fact bobs {:replaces [[:alice :streaming (uuid 89)] 1]})]
      {[bobs-id :softland.store/permission] (permission-row bobs 1)} :replaced-fact-not-current]
     ["an end at an empty place" [(perm/end-fact bobs)] {} :no-permission-to-end]
     ["an end at a place that holds an end" [(perm/end-fact bobs)] {[bobs-id :softland.store/permission] (end-row bobs 3)} :permission-already-ended]
     ["an end of the lane's only owner permission, leaving no owner"
      [(perm/end-fact alice-own)] {} :lane-would-have-no-owner]
     ["an end of a lent permission two beneath the act's own: a lender further up and an owner end it"
      [(perm/end-fact carols-lent)]
      {[carols-id :softland.store/permission] (permission-row carols-lent 2)
       [bobs-id :softland.store/permission] (permission-row bobs 1)} nil]
     ["a give at an id whose permission ended: an id takes one permission for good"
      [(perm/permission-fact bobs)] {[bobs-id :softland.store/permission] (end-row bobs 3)} :a-permission-id-takes-one-permission]
     ["a give after an end, on a new id"
      [(perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:salt 2}))]
      {[bobs-id :softland.store/permission] (end-row bobs 3)} nil]
     ["an in-place change that lowers a standing permission from member to see"
      [(perm/permission-fact bobs-lowered)]
      {[bobs-id :softland.store/permission] (permission-row bobs 1)}
      nil]
     ["a give to an agent with no lent-from: an agent or a run only borrows"
      [(perm/permission-fact (perm/given-permission (agent :bot) :alice alice-own {:lent-from nil}))]
      {}
      :an-agent-or-a-run-only-borrows]
     ["a retract of the lane's depot" [{:e :alice :k :softland.store/depot}] {} :store-key-retract-not-built]
     ["the other depot for a lane that is made" [{:e :alice :k :softland.store/depot :v :microbatch}] {} :changing-a-depot-not-decided]
     ["the lane's own depot a second time" [{:e :alice :k :softland.store/depot :v :streaming}] {} :store-key-takes-one-write]
     ["a stale fact and a depot fact on another entity: the stale fact gives the word"
      [(replacing [(act-id 6) 0]) {:e :bob :k :softland.store/depot :v :streaming}] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a depot fact on another entity before a permission that covers another lane: the setting gives the word"
      [(perm/permission-fact (perm/given-permission (person :bob) :team alice-own))
       {:e :carol :k :softland.store/depot :v :streaming}] {} :setting-not-on-the-lane]
     ["who may read the lane's acts, on an empty place" [{:e :alice :k reads-acts :v :everyone}] {} nil]
     ["what the lane builds on, on an empty place" [{:e :alice :k on :v [:base]}] {} nil]
     ["the lane's marked keys, on an empty place" [{:e :alice :k marks :v #{:on}}] {} nil]
     ["who may read the lane's acts, written a second time on a place that holds one, naming nothing: a second write is taken"
      [{:e :alice :k reads-acts :v :members}] {[:alice reads-acts] (setting-row 7 reads-acts :everyone)} nil]
     ["what the lane builds on, written a second time, naming nothing" [{:e :alice :k on :v []}] {[:alice on] (setting-row 7 on [:base])} nil]
     ["the lane's marked keys, written a second time, naming nothing" [{:e :alice :k marks :v #{}}] {[:alice marks] (setting-row 7 marks #{:on})} nil]
     ["who may read the lane's acts, written again naming the fact that is there" [{:e :alice :k reads-acts :v :members :replaces [(act-id 7) 0]}]
      {[:alice reads-acts] (setting-row 7 reads-acts :everyone)} nil]
     ["who may read the lane's acts, written again naming a fact that is no longer there: a writer who must not lose a write names what she replaces"
      [{:e :alice :k reads-acts :v :members :replaces [(act-id 6) 0]}] {[:alice reads-acts] (setting-row 7 reads-acts :everyone)} :replaced-fact-not-current]
     ["what the lane builds on, written again naming a stale fact" [{:e :alice :k on :v [:base] :replaces [(act-id 6) 0]}]
      {[:alice on] (setting-row 7 on [])} :replaced-fact-not-current]
     ["the lane's marked keys, written again naming a stale fact" [{:e :alice :k marks :v #{:on} :replaces [(act-id 6) 0]}]
      {[:alice marks] (setting-row 7 marks #{})} :replaced-fact-not-current]
     ["a retract of who may read the lane's acts" [{:e :alice :k reads-acts}] {[:alice reads-acts] (setting-row 7 reads-acts :everyone)} :store-key-retract-not-built]
     ["a retract of what the lane builds on: a list is emptied by a second write of []" [{:e :alice :k on}] {} :store-key-retract-not-built]
     ["a retract of the lane's marked keys: a mark is taken off by a second write of the set without it" [{:e :alice :k marks}] {} :store-key-retract-not-built]
     ["who may read the lane's acts, on an entity that is not the lane" [{:e :bob :k reads-acts :v :everyone}] {} :setting-not-on-the-lane]
     ["what a lane builds on, on an entity that is not the lane" [{:e :bob :k on :v [:base]}] {} :setting-not-on-the-lane]
     ["marked keys, on an entity that is not the lane" [{:e :bob :k marks :v #{:on}}] {} :setting-not-on-the-lane]
     ["a retract of who may read the lane's acts on an entity that is not the lane: the setting gives the word" [{:e :bob :k reads-acts}] {} :setting-not-on-the-lane]
     ["an in-place change that raises a holder of see to member, keeping the permission's id"
      [(perm/permission-fact bobs-raised)]
      {[bobs-see-id :softland.store/permission] (permission-row bobs-see 1)} nil]
     ["a permission given with see only, under the act's own, on an empty id" [(perm/permission-fact bobs-see)] {} nil]
     ["a permission lent from another permission than the act's own"
      [(perm/permission-fact (perm/given-permission (person :bob) :alice alice-own {:lent-from (uuid 97)}))] {}
      :permission-not-lent-from-the-acts-own]
     ["a tool's own fact under the bare word :class, on an entity that is not the lane: no setting" [(value-at :bob :class)] {} nil]
     ["a tool's own fact under the bare word :permission, on a permission's id: no permission fact"
      [(value-at bobs-id :permission)] {[bobs-id :softland.store/permission] (permission-row bobs)} nil]
     ["a fact copied from the log into another act, on the place where it is the current fact: a value like any other, so it is taken"
      [(value-at :note-1 :text "a text before")] {[:note-1 :text] (row-by 7 0)} nil]]))

(defn- keyed
  "Rows by [entity key], as rows by the key of the place in Alice's lane,
  with Alice's depot, first permission and owner count in hand."
  [rows]
  (into {":alice :alice :softland.store/depot" depot-row
         alices-place (permission-row alice-own)
         decision/owners-key 1}
        (map (fn [[[e k] row]] [(record/place-key :alice e k) row]))
        rows))

(def ^:private places-cases
  "The cases of the places: one for each row of `places-rows`, the places
  whose rows are read, and a making act."
  (let [bobs-place (chain/place :alice (perm/permission-id bobs))
        co-owner (perm/given-permission (person :carol) :alice alice-own {:allows :owner :lent-from nil})
        co-owner-place (chain/place :alice (perm/permission-id co-owner))
        bots-1 (perm/given-permission (agent :bot) :alice bobs :see)
        bots-2 (perm/given-permission (agent :ada) :alice bots-1 :see)
        bots-1-place (chain/place :alice (perm/permission-id bots-1))
        bots-2-place (chain/place :alice (perm/permission-id bots-2))
        standing-base {":alice :alice :softland.store/depot" depot-row
                       alices-place (permission-row alice-own)}]
    (concat
     (map (fn [[what facts rows word]]
            [(str "places: " what)
             (fn [] (decision/places :streaming (act-of (apply arrived 1 facts)) (keyed rows)))
             word])
          places-rows)
     [["the places whose rows are read: of each fact that replaces something and of each permission fact, no other; not of a tool's own fact under the bare word :permission"
       (fn [] (decision/places-wanted
               :streaming
               (act-of (arrived 1 (value-at :note-1 :text)
                                (assoc (value-at :note-2 :text) :replaces [(act-id 7) 0])
                                (perm/permission-fact bobs)
                                (value-at :carol :permission)))
               standing-base))
       [":alice :note-2 :text" bobs-place]]
      ["a fact under a key that takes a second write reads its place for the checks only when it names something to replace"
       (fn [] (decision/places-wanted
               :streaming
               (act-of (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v :everyone}
                                {:e :alice :k :softland.store/builds-on :v [:base] :replaces [(act-id 7) 0]}
                                {:e :alice :k :softland.store/marked-keys :v #{:on}}))
               standing-base))
       [":alice :alice :softland.store/builds-on"]]
      ["who may write the three settings: any owner of the lane, the first holder and a second owner alike, and no holder who is no owner"
       (fn [] (let [act (act-of (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v :everyone}))
                    rmap (assoc standing-base
                                bobs-place (permission-row bobs 1)
                                co-owner-place (permission-row co-owner 1))]
                [(decision/places :streaming act rmap)
                 (decision/places :streaming (assoc act :who (person :carol) :permission (perm/permission-id co-owner)) rmap)
                 (decision/places :streaming (assoc act :who (person :bob) :permission (perm/permission-id bobs)) rmap)]))
       [nil nil :only-an-owner-writes-a-setting]]
      ["the three settings by a member who is no owner are refused, each with that word; a second owner's write and a fact of a tool by the member are taken"
       (fn [] (let [rmap (assoc standing-base
                                bobs-place (permission-row bobs 1)
                                co-owner-place (permission-row co-owner 1))]
                [(mapv (fn [fact] (decision/places :streaming (assoc (act-of (arrived 1 fact)) :who (person :bob) :permission (perm/permission-id bobs)) rmap))
                       [{:e :alice :k :softland.store/who-may-read-acts :v :everyone}
                        {:e :alice :k :softland.store/builds-on :v [:base]}
                        {:e :alice :k :softland.store/marked-keys :v #{:on}}
                        (value-at :note-1 :text)])
                 (decision/places :streaming
                                  (assoc (act-of (arrived 1 {:e :alice :k :softland.store/builds-on :v [:base]}))
                                         :who (person :carol) :permission (perm/permission-id co-owner))
                                  rmap)]))
       [[:only-an-owner-writes-a-setting :only-an-owner-writes-a-setting :only-an-owner-writes-a-setting nil] nil]]
      ["who ends a permission, when the act is Bob's, under his member permission: his own; one that he gave; one lent from his; not another member's, not an owner's"
       (fn [] (let [carols-standing (perm/given-permission (person :carol) :alice bobs {:lent-from nil})
                    bots-lent (perm/given-permission (agent :bot) :alice bobs :see)
                    daves (perm/given-permission (person :dave) :alice alice-own {:lent-from nil})
                    ends (fn [target held]
                           (let [{:keys [e k]} (perm/end-fact target)]
                             (decision/places :streaming
                                              (assoc (act-of (arrived 1 (perm/end-fact target))) :who (person :bob) :permission (perm/permission-id bobs))
                                              (assoc (keyed {[e k] held}) bobs-place (permission-row bobs 1)))))]
                [(ends bobs (permission-row bobs 1))
                 (ends carols-standing (permission-row carols-standing 2))
                 (ends bots-lent (permission-row bots-lent 2))
                 (ends daves (permission-row daves 1))
                 (ends alice-own (permission-row alice-own))]))
       [nil nil nil :may-not-end-the-permission :may-not-end-the-permission]]
      ["the rows that places-wanted asks for in steps: first replaced facts and permission places, then lender-above chains when needed, then owners when an owner is taken out and none made; none for a making act"
       (fn [] (let [end-deep (assoc (act-of (arrived 1 (perm/end-fact bots-2))) :who (person :bob) :permission (perm/permission-id bobs))
                    end-owner (act-of (arrived 1 (perm/end-fact alice-own)))
                    hand-over (act-of (arrived 1 (perm/permission-fact co-owner) (perm/end-fact alice-own)))
                    making-act (making :team (person :alice) (person :alice))]
                [(decision/places-wanted :streaming end-deep (assoc standing-base bobs-place (permission-row bobs 1)))
                 (decision/places-wanted :streaming end-deep (assoc standing-base bobs-place (permission-row bobs 1) bots-2-place (permission-row bots-2 3)))
                 (decision/places-wanted :streaming end-deep (assoc standing-base bobs-place (permission-row bobs 1)
                                                                    bots-2-place (permission-row bots-2 3)
                                                                    bots-1-place (permission-row bots-1 2)))
                 (decision/places-wanted :streaming end-owner standing-base)
                 (decision/places-wanted :streaming hand-over (assoc standing-base co-owner-place nil))
                 (decision/places-wanted :streaming making-act {":team :team :softland.store/depot" nil})]))
       [[bots-2-place] [bots-1-place] [] [decision/owners-key] [] []]]
      ["a making act reads no place, and is not held to the check of a permission fact or to one write for a place"
       (fn [] (let [act (making :team (person :alice) (person :alice))]
                [(decision/places-wanted :streaming act {":team :team :softland.store/depot" nil})
                 (decision/places :streaming act {":team :team :softland.store/depot" nil})
                 (decision/places :streaming act {":team :team :softland.store/depot" depot-row})]))
       [[] nil :store-key-takes-one-write]]
      ["a making act that names something to replace is stale: nothing is current in a lane that is not made"
       (fn [] (decision/places :streaming
                               (assoc-in (making :team (person :alice) (person :alice)) [:facts 0 :replaces] [[:team :streaming (uuid 7)] 0])
                               {":team :team :softland.store/depot" nil}))
       :replaced-fact-not-current]])))

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
        uuid-party (person (uuid 77))
        uuid-who (declaration/id-text uuid-party)
        uuid-perm (perm/permission-id (perm/given-permission uuid-party :alice alice-own :see-and-act))
        team-own (perm/first-permission (person :alice) :team)]
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
     ["an accept of one value writes three rows: the fact, with the one text of its value, the time on the act, who wrote it and the id of the permission that the act named; the answer, with the time on the act; the last stamp"
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
                   refusal-at (fn [t] (decision/writes (act-id 1) value-act :lane-not-made stamp t digest {}))]
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
     ["the texts of who and of the permission id are made once for the act and each row holds them: an act by a person whose id holds a UUID, with a value and a retract"
      (fn [] (let [act (act-of (assoc (arrived 1 (value-at :note-1 :text) {:e :note-2 :k :text})
                                      :who uuid-party
                                      :permission uuid-perm))]
               (mapv (fn [[_ _ row]] (select-keys row [:who :permission]))
                     (take 2 (decision/writes (act-id 1) act nil stamp now digest {})))))
      (vec (repeat 2 {:who uuid-who :permission (str uuid-perm)}))]
     ["an accept of a making act writes the depot, the first permission, its entry by holder, its owner entry, the answer and the last stamp; each fact row holds who made the lane, no permission on the act, and the time on the act"
      (fn [] (decision/writes [:team :streaming (uuid 1)] (making :team (person :alice) (person :alice)) nil stamp now digest {}))
      [[:fact ":team :team :softland.store/depot"
        {:value ":streaming" :by-act (uuid 1) :by-index 0 :stamp stamp :time now :who ":person.alice"}]
       [:fact (chain/place :team (perm/permission-id team-own))
        {:value (text/text (perm/permission-value team-own)) :by-act (uuid 1) :by-index 1 :stamp stamp :time now :who ":person.alice"}]
       [:holder-permission (record/by-holder-key :team (person :alice) nil (perm/permission-id team-own)) stamp]
       [:owner (perm/permission-id team-own) true]
       [:answer (record/answer-key :team (uuid 1)) {:stamp stamp :time now :digest digest}]
       [:last-stamp nil stamp]]]
     ["the system makes a personal lane for its person: the rows hold the system as who and no permission on the act, while the permission value names the person as holder and the system as giver"
      (fn [] (let [w (decision/writes [:alice :streaming (uuid 1)] (making :alice :system (person :alice)) nil stamp now digest {})]
               [(mapv (fn [[_ _ row]] (select-keys row [:who :permission])) (take 2 w))
                (select-keys (record/held-permission (nth (second w) 2)) [:holder :given-by])]))
      [[{:who ":system"} {:who ":system"}] {:holder (person :alice) :given-by :system}]]
     ["an accept of a retract and a permission given writes a row for each, and the permission's entry by holder: the retract's has no value; each fact row holds who, the permission id and the time"
      (fn [] (mapv (fn [[which key row]] [which key (if (map? row) (dissoc row :by-act :stamp) row)])
                   (decision/writes (act-id 1)
                                    (act-of (arrived 1 {:e :note-1 :k :text} (perm/permission-fact bobs)))
                                    nil stamp now digest {})))
      [[:fact note-place (merge {:by-index 0 :time now} written-by-alice)]
       [:fact (chain/place :alice (perm/permission-id bobs))
        (merge {:value (text/text (perm/permission-value bobs)) :by-index 1 :time now} written-by-alice)]
       [:holder-permission (record/by-holder-key :alice (person :bob) nil (perm/permission-id bobs)) stamp]
       [:answer answer-key {:time now :digest digest}]
       [:last-stamp nil stamp]]]
     ["a give of see writes the permission value's one text at the permission's own id, and puts its key in the order by holder with the decision's stamp"
      (fn [] (let [[[_ k row] [_ hk hstamp]] (decision/writes (act-id 1) (act-of (arrived 1 (perm/permission-fact bobs-see)))
                                                              nil stamp now digest {})]
               [k (:value row) (record/held-permission row) hk hstamp]))
      [(chain/place :alice (perm/permission-id bobs-see))
       (text/text (perm/permission-value bobs-see))
       (perm/permission-value bobs-see)
       (record/by-holder-key :alice (person :bob) nil (perm/permission-id bobs-see))
       stamp]]
     ["an in-place change writes the new decision's stamp in the order by holder, and an end writes nil there and updates owners when an owner ends"
      (fn [] (let [bobs-place (chain/place :alice (perm/permission-id bobs))
                   changed (decision/writes (act-id 1) (act-of (arrived 1 (perm/permission-fact {:id (perm/permission-id bobs)
                                                                                                 :value (perm/permission-value bobs-see)})))
                                            nil stamp now digest {:place-rows {bobs-place (permission-row bobs 1)}})
                   ended (decision/writes (act-id 2) (act-of (arrived 2 (perm/end-fact alice-own)))
                                          nil stamp now digest {:place-rows {alices-place (permission-row alice-own)}})]
               [(second changed) (subvec ended 1 3)]))
      [[:holder-permission (record/by-holder-key :alice (person :bob) nil (perm/permission-id bobs)) stamp]
       [[:holder-permission (record/by-holder-key :alice (person :alice) nil (perm/permission-id alice-own)) nil]
        [:owner (perm/permission-id alice-own) nil]]]]
     ["a write of the marked keys: the row holds the set that the maker wrote and, derived at the write, the stamp of each key; a newly marked key gets the number under :level-from, here 500, which is not the stamp of the act, so that the two are told apart"
      (fn [] (let [[[which k row] & more] (decision/writes (act-id 1) (marking #{:on :tool/parent}) nil stamp now digest
                                                           {:place-rows {marks-place nil} :level-from 500})]
               [which k (:value row) (record/read-value row) (record/marked-keys-of row) (mapv first more)]))
      [:fact marks-place "#{:on :tool/parent}" [:value #{:on :tool/parent}] {:on 500 :tool/parent 500} [:answer :last-stamp]]]
     ["what the walk hands under :level-from is the act's own stamp, for an act of either gate: a write of the marks in a streaming lane, and one in the base under a microbatch act-id, each give the newly marked key the stamp that the row of the marks itself holds"
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
                                            (act-of (arrived 1 {:e :alice :k :softland.store/who-may-read-acts :v :everyone}
                                                             {:e :alice :k :softland.store/builds-on :v [:base :team]}))
                                            nil stamp now digest {}))))
      [[":alice :alice :softland.store/who-may-read-acts" ":everyone" [:value :everyone] false]
       [":alice :alice :softland.store/builds-on" "[:base :team]" [:value [:base :team]] false]]]]))

;; --------------------------------------- the facts by value, one act at a time

(defn- streaming-step
  "Decides one accepted act of Alice's in her streaming lane against a
  record in memory, as steps 6 and 7 of the walk of one thing do: the row at
  the lane's marked-keys place when the act needs it, the stamp of each of
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
  "A fact of Alice's lane under the key :on, with a value."
  [e v]
  (value-at e :on v))

(defn- entry-key
  "The key in the facts by value of the place `e` :on of Alice's lane, for a
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
     ["a fact under a key that the lane does not mark, beside a marked one, and a fact under a store key: no row in the order"
      (fn [] (let [record (streaming-step m 231 110 (value-at :b1 :text :c1) (perm/permission-fact bobs))]
               [(which-keys record) (keys-of record)]))
      [[[:fact ":alice :b1 :text" true]
        [:fact (chain/place :alice (perm/permission-id bobs)) true]
        [:holder-permission (record/by-holder-key :alice (person :bob) nil (perm/permission-id bobs)) true]]
       #{}]]
     ["which acts read the lane's marked keys: one with a fact under a plain key, or under the marked keys; not a give, a setting or a list; and the rows before that are still to be read"
      (fn [] [(mapv (fn [facts] (decision/reads-marked-keys? (act-of (apply arrived 1 facts))))
                    [[(on-fact :b1 :c1)] [{:e :b1 :k :on}] [{:e :alice :k :softland.store/marked-keys :v #{:on}}]
                     [(perm/permission-fact bobs)]
                     [{:e :alice :k :softland.store/who-may-read-acts :v :everyone}] [{:e :alice :k :softland.store/builds-on :v []}]])
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

(def ^:private system-own "The system's permission in the base." (perm/first-permission :system :base))
(def ^:private carols "Carol's permission in the base, beneath the system's." (perm/given-permission (person :carol) :base system-own))
(def ^:private daves-in-base "Dave's permission in the base, beneath the system's." (perm/given-permission (person :dave) :base system-own))
(def ^:private mallorys "A permission in the base that no act gives." (perm/given-permission (person :mallory) :base system-own))

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
   :version 1 :who who :lane :base :permission (perm/permission-id permission) :session :s1
   :facts (vec facts)})

(def ^:private base-writer
  "The two texts that each row of a fact written by the system under its own
  permission in the base holds: of who and of the permission id."
  [":system" (str (perm/permission-id system-own))])

(def ^:private before-the-line
  "The last stamp of the base in each record that a line of the proof starts
  from, 30: thirty-one decisions of the base came before the line, from 0.
  It is also the stamp of each row that such a record holds but the row of
  the system's own permission, which the base's making act wrote at the
  stamp 0 (`base-permission-row`), and the rows of facts from before a mark
  of their key (`marks-line`)."
  30)

(def ^:private time-before-the-line
  "The time on each act whose rows a record before a line holds: five
  seconds before `now`."
  (- now 5000))

(def ^:private base-depot-row
  "The row of the depot fact of the base."
  (fact-row [":system" nil] (arrived-fact :base :softland.store/depot :microbatch) (uuid 800) 0 before-the-line time-before-the-line))

(defn- base-permission-row
  "The row of a permission fact of the base, as an accept writes it: with
  the stamp 0 for a first permission, which the base's making act wrote,
  and with the stamp `before-the-line` for a given one."
  [permission]
  (let [{:keys [e k v]} (perm/permission-fact permission)
        first? (nil? (:under v))]
    (fact-row (if first? [":system" nil] base-writer) (arrived-fact e k v) (uuid 800) 1
              (if first? 0 before-the-line) time-before-the-line)))

(def ^:private carols-see
  "Carol's permission in the base, beneath the system's, that says see."
  (perm/given-permission (person :carol) :base system-own :see))

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
  [table key row]: the depot fact, the system's permission, its entry by
  holder and its owner set."
  [[:facts ":base :base :softland.store/depot" base-depot-row]
   [:facts (chain/place :base (perm/permission-id system-own)) (base-permission-row system-own)]
   [:by-holder (record/by-holder-key :base :system nil (perm/permission-id system-own)) 0]
   [:owners ":base" #{(perm/permission-id system-own)}]])

(def ^:private base-standing-rows
  "For each: an act into the base as it arrives on the microbatch depot, the
  rows map handed to `standing` and `places`, then the first refusal word of
  the two (or nil), and whether the act makes the lane."
  (let [act (act-of (base-thing 1 (person :carol) carols {:e :doc-1 :k :note}))
        seeing (act-of (base-thing 1 (person :carol) carols-see {:e :doc-1 :k :note}))
        own-end (act-of (-> (base-thing 1 (person :carol) carols-see (perm/end-fact carols-see))
                            (dissoc :permission)))
        by-another (act-of (base-thing 1 (person :mallory) carols {:e :doc-1 :k :note}))
        base-making (act-of (-> (apply base-thing 1 :system nil (perm/making-facts :base :system))
                                (dissoc :permission)))
        depot-key ":base :base :softland.store/depot"
        system-place (chain/place :base (perm/permission-id system-own))
        carols-place (chain/place :base (perm/permission-id carols))
        carols-see-place (chain/place :base (perm/permission-id carols-see))]
    [["the row holds the act's permission"
      act {carols-place (base-permission-row carols) system-place (base-permission-row system-own)} nil false]
     ["the row holds the act's permission, and it says see: an act needs act"
      seeing {carols-see-place (base-permission-row carols-see) system-place (base-permission-row system-own)}
      :permission-does-not-allow-writing false]
     ["a holder of see who ends her own permission in an act that names none"
      own-end {depot-key base-depot-row carols-see-place (base-permission-row carols-see)}
      nil false]
     ["an act that names another id than the one whose place holds a permission"
      (assoc act :permission (uuid 99)) {depot-key base-depot-row carols-place (base-permission-row carols)}
      :permission-never-given false]
     ["the place is empty in a base that is made: the record does not hold the permission"
      act {depot-key base-depot-row}
      :permission-never-given false]
     ["the place is empty, and the holder is not the who"
      by-another {depot-key base-depot-row}
      :permission-never-given false]
     ["the record holds nothing of the base, and the act is no making act: the base is this gate's lane, so the word is that of a lane that is not made"
      act {} :lane-not-made false]
     ["the base's making act, with nothing in the record: it makes the lane"
      base-making {} nil true]
     ["the base's making act for a base that is made: it names no permission and makes nothing"
      base-making {depot-key base-depot-row} :no-permission false]]))

(def ^:private base-standing-cases
  "The cases of the standing at the microbatch gate's depot: one for each
  row of `base-standing-rows`."
  (map (fn [[what act rows word makes?]]
         [(str "standing at the microbatch gate's depot: " what)
          (fn [] [(or (decision/standing :microbatch act rows nil)
                      (decision/places :microbatch act rows))
                  (decision/makes-the-lane? :microbatch act rows)])
          [word makes?]])
       base-standing-rows))

;; --------------------------------------------- a lane of the other gate

(defn- as-sent
  "Takes an act in its one form on arrival, as `making` gives it, and a
  depot word. Gives the same act as it arrives on that depot from its
  maker: the depot word in the depot slot of its act-id and under `:depot`,
  each fact as its maker gives it, and without the parts that the form
  fills in. Its facts are as they were, so a making act still names the
  depot that its lane starts with."
  [form depot]
  (-> form
      (dissoc :permission :based-on :because-of)
      (assoc :act-id (assoc (:act-id form) 1 depot) :depot depot)
      (update :facts (fn [facts] (mapv #(select-keys % [:e :k :v]) facts)))))

(def ^:private other-gate-rows
  "Things as they arrive on a gate's depot. For each: the gate's depot; the
  thing; whether its lane starts with the other gate's depot; whether the
  act has the form of a making act for this gate, and for the other gate;
  then the word of `standing` with no row of the lane, and whether the act
  makes the lane. The first six are for a lane of the other gate. The
  last four are the same kinds of act on their own gate's depot."
  (let [base-making (making :base :system :system)
        personal (making :alice :system (person :alice))
        group (making :team (person :alice) (person :alice))
        of-a-uuid (making (uuid 7) :system (person (uuid 8)))
        word :lane-belongs-to-the-other-gate]
    [["an act into the base, on the streaming depot" :streaming base-on-the-streaming-depot true false false word false]
     ["the base's making act, on the streaming depot" :streaming (as-sent base-making :streaming) true false true word false]
     ["an act into a personal lane, on the microbatch depot"
      :microbatch (assoc good :act-id [:alice :microbatch (uuid 1)] :depot :microbatch) true false false word false]
     ["the making act of a personal lane, on the microbatch depot" :microbatch (as-sent personal :microbatch) true false true word false]
     ["the making act of a group's lane, on the microbatch depot" :microbatch (as-sent group :microbatch) true false true word false]
     ["the making act of a lane whose id is a UUID, on the microbatch depot" :microbatch (as-sent of-a-uuid :microbatch) true false true word false]
     ["an act into a personal lane that is not made, on the streaming depot, its own" :streaming good false false false :lane-not-made false]
     ["the making act of a group's lane, on the streaming depot, its own: it is taken" :streaming (as-sent group :streaming) false true false nil true]
     ["an act into a base that is not made, on the microbatch depot, its own"
      :microbatch (base-thing 1 :system system-own {:e :doc-1 :k :note}) false false false :lane-not-made false]
     ["the base's making act, on the microbatch depot, its own: it is taken" :microbatch (as-sent base-making :microbatch) false true false nil true]]))

(def ^:private other-gate-cases
  "The cases of a lane of the other gate, for both gates: one for each row
  of `other-gate-rows`. Each thing is no stray on its depot and the
  declaration's check takes it, so the standing has the word. `standing` and `makes-the-lane?` are handed nil
  for both rows: the word comes from the gate's depot and the act alone.
  They take no row of the other gate's record, so whether the other gate
  has made the lane cannot change it."
  (map (fn [[what gate thing other? form-here? form-there? word makes?]]
         [(str "a lane of the other gate, from the act alone: " what)
          (fn [] (let [act (act-of thing)
                       other-gate (if (= :streaming gate) :microbatch :streaming)]
                   [(declaration/stray-reason gate thing)
                    (verdict (declaration/check thing))
                    (first-act/starts-with-the-other-gates-depot? gate (:lane act))
                    (first-act/making-act? gate act)
                    (first-act/making-act? other-gate act)
                    (decision/standing gate act {} nil)
                    (decision/makes-the-lane? gate act {})]))
          [nil :ok other? form-here? form-there? word makes?]])
       other-gate-rows))

;; ------------------------------------------ the walk of one thing, in memory

(def ^:private empty-task
  "What one gate holds on one task, in memory, with nothing in it (`softland.store.record.in-memory/empty-record`)."
  memory/empty-record)

(defn- task-with
  "Takes rows, each [table key row], and the last stamps, a map from the text
  of a lane id to a number. Gives a record in the form of `empty-task`
  that holds them."
  [rows last-stamps]
  (reduce (fn [record [table k row]] (assoc-in record [table k] row))
          (assoc empty-task :last-stamp last-stamps)
          rows))

(defn- to-copy
  "The proof's own filter of what a gate may copy to other tasks. Takes the
  writes of one decision, as `softland.store.gate.decision/writes` gives
  them. Gives each `:fact`, `:by-value`, and `:holder-permission` write, in
  their order, and no other write. `softland.store.module.walk/copies-of`
  says the same; `copy-cases` holds that function, as its file writes it,
  against this one."
  [writes]
  (filterv (fn [[which]] (contains? #{:fact :by-value :holder-permission} which)) writes))

(defn- walked
  "One thing through the steps of the walk of one thing
  (`softland.store.gate.walk` against `softland.store.record.in-memory`), in
  memory. Gives the record after, with `:wrote` (the record writes of
  `softland.store.gate.decision/writes`, or `[]` for a stray or repeat) and
  `:wrote-line` (the line writes of `softland.store.gate.decision/line-writes`,
  or `[]`)."
  [record gate thing]
  (let [{new-record :record {:keys [stray repeat writes]} :outcome} (memory/decide record gate thing)
        line-which? #{:line :thing :by-who}]
    (if (or stray repeat)
      (assoc new-record :wrote [] :wrote-line [])
      (assoc new-record
             :wrote (vec (remove (comp line-which? first) writes))
             :wrote-line (filterv (comp line-which? first) writes)))))

(defn- walked-line
  "Takes a record in the form of `empty-task`, a gate's depot word, and
  things in the order in which their depot partition holds them. Gives the
  record after `walked` decided each, one after the other: each against the
  rows that the things before it wrote. `:wrote` is not in it."
  [record gate things]
  (dissoc (reduce (fn [record thing] (walked record gate thing)) record things) :wrote :wrote-line))

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

(def ^:private covers-cases
  "The cases of what a permission covers: one sitting (`:session` on a
  permission value); a yes for one act has its own cases (`yes-cases`). The
  line is a lane's acts walked in memory, each against the rows that the
  acts before it wrote."
  (let [thing (fn [n who permission session & facts]
                {:act-id (act-id n) :depot :streaming declaration/time-part now
                 :version 1 :who who :lane :alice :permission (perm/permission-id permission) :session session
                 :facts (vec facts)})
        note (fn [n] {:e (keyword (str "covered-" n)) :k :text :v (str "act " n)})
        for-run (fn [who session allows] (perm/run-permission who :alice alice-own session allows))
        gives (fn [n giver held given]
                (thing n giver held :alice-s1 (perm/permission-fact given)))
        r7 (for-run (person :bob) :run-7 :see-and-act)
        r7-2 (perm/run-permission (person :bob) :alice alice-own :run-7 :see-and-act {:salt 2})
        r8 (for-run (person :bob) :run-8 :see-and-act)
        rb-bot (perm/run-permission (agent :bot) :alice r7 :run-7 :see-and-act)
        bobs-standing-see (perm/given-permission (person :bob) :alice alice-own :see)
        carols (perm/given-permission (person :carol) :alice alice-own)
        rc (for-run (person :carol) :run-c :see-and-act)
        daves (perm/given-permission (person :dave) :alice alice-own)
        re (perm/run-permission (person :erin) :alice daves :run-e :see-and-act)
        rf (for-run (person :frank) :run-f :see)
        line [(apply thing 1 (person :alice) nil :alice-s1 (perm/making-facts :alice (person :alice)))
              (gives 2 (person :alice) alice-own r7)
              (thing 3 (person :bob) r7 :run-7 (note 3))
              (thing 4 (person :bob) r7 :run-8 (note 4))
              (thing 5 (person :carol) r7 :run-7 (note 5))
              (thing 6 (person :bob) r7 :run-7 (perm/permission-fact (perm/given-permission (person :bob) :alice r7 {:allows :see})))
              (thing 7 (person :bob) r7 :run-7 {:e :alice :k :softland.store/who-may-read-acts :v :everyone})
              (gives 8 (person :alice) alice-own bobs-standing-see)
              (thing 9 (person :bob) bobs-standing-see :run-7 (note 9))
              (thing 10 (person :bob) r7 :run-7 (note 10))
              (gives 11 (person :alice) alice-own r8)
              (thing 60 (person :bob) r7 :run-7 (perm/permission-fact rb-bot))
              (thing 12 (agent :bot) rb-bot :run-7 (perm/end-fact r7))
              (thing 13 (person :bob) r7 :run-7 (perm/end-fact r7))
              (thing 14 (person :bob) r7 :run-7 (note 14))
              (thing 15 (person :bob) r8 :run-8 (note 15))
              (gives 16 (person :alice) alice-own r7-2)
              (thing 17 (person :bob) r7-2 :run-7 (note 17))
              (thing 18 (person :alice) alice-own :alice-s1 (perm/end-fact r8))
              (thing 19 (person :bob) r8 :run-8 (note 19))
              (gives 40 (person :alice) alice-own carols)
              (gives 41 (person :alice) alice-own rc)
              (thing 42 (person :carol) carols :carol-s1 (perm/end-fact rc))
              (thing 43 (person :carol) rc :run-c (note 43))
              (gives 44 (person :alice) alice-own daves)
              (gives 45 (person :dave) daves re)
              (thing 46 (person :erin) re :run-e (note 46))
              (thing 47 (person :alice) alice-own :alice-s1 (perm/end-fact daves))
              (thing 48 (person :erin) re :run-e (note 48))
              (gives 49 (person :alice) alice-own (for-run (person :carol) :run-7 :see-and-act))
              (gives 50 (person :alice) alice-own rf)
              (thing 51 (person :frank) rf :run-f (note 51))
              (thing 52 (person :frank) rf :run-f (perm/end-fact rf))]
        after (walked-line empty-task :streaming line)]
    [["what a permission covers, its sitting scope, and what it allows are fields of its value map"
      (fn [] [(mapv (comp (juxt :covers :session) perm/permission-value) [alice-own bobs bobs-see r7])
              (mapv (comp :allows perm/permission-value) [r7 rf])])
      [[[[:default-group :alice] nil] [[:default-group :alice] nil] [[:default-group :alice] nil] [[:default-group :alice] :run-7]]
       [{:see true :write :all} {:see true}]]]
     ["every permission sits on its own uuid under :softland.store/permission, and is indexed by holder and sitting"
      (fn [] [(mapv #(chain/place :alice (perm/permission-id %)) [bobs r7])
              (record/by-holder-key :alice (person :bob) nil (perm/permission-id bobs))
              (record/by-holder-key :alice (person :bob) :run-7 (perm/permission-id r7))])
      [(mapv #(str ":alice " (perm/permission-id %) " :softland.store/permission") [bobs r7])
       (str ":alice :person.bob * " (perm/permission-id bobs))
       (str ":alice :person.bob :run-7 " (perm/permission-id r7))]]
     ["a line of a lane's acts: a sitting permission covers the acts of that sitting by its holder, and no other sitting or who; it cannot widen its sitting scope or write a setting, while it may lend to an agent; it stands beside her permission for the lane; an agent beneath it cannot end it, while its holder ends it, leaving her other sitting standing; a new sitting permission on a new id is taken, and two holders can hold permissions for one sitting at once; a lent sitting permission falls with the permission above it; a sitting permission that says see cannot write, and still ends itself"
      (fn [] (words-by-number after))
      {1 :accepted
       2 :accepted 3 :accepted 4 :permission-for-another-sitting 5 :who-not-the-permissions-holder
       6 :permission-covers-more-than-the-acts-own 7 :only-an-owner-writes-a-setting
       8 :accepted 9 :permission-does-not-allow-writing 10 :accepted
       11 :accepted 60 :accepted 12 :may-not-end-the-permission 13 :accepted 14 :permission-ended 15 :accepted
       16 :accepted 17 :accepted 18 :accepted 19 :permission-ended
       40 :accepted 41 :accepted 42 :accepted 43 :permission-ended
       44 :accepted 45 :accepted 46 :accepted 47 :accepted 48 :permission-above-ended
       49 :accepted
       50 :accepted 51 :permission-does-not-allow-writing 52 :accepted}]
     ["after that line a holder has a permission for the lane and one for each of two runs, each on its own place: Bob's for the lane and for the run :run-7 stand, and his for :run-8 is an end"
      (fn [] (let [facts (:facts after)]
               [(record/held-permission (get facts (chain/place :alice (perm/permission-id bobs-standing-see))))
                (record/held-permission (get facts (chain/place :alice (perm/permission-id r7-2))))
                (record/ended? (get facts (chain/place :alice (perm/permission-id r8))))]))
      [(perm/permission-value bobs-standing-see) (perm/permission-value r7-2) true]]]))

(declare copies-of-as-written)

(def ^:private yes-cases
  "The cases of a yes for one act, in the form of 22 November 2026
  (`softland.store.act`): `[who lane granted-in above :see-and-act [:act
  yes digest]]`, on the yes's own id, naming the act as its maker made it
  (`softland.store.door.digest/hex-for-a-yes`), used once. The first cases
  hold the digest; then a line of a lane's acts walked in memory, each
  against the rows that the acts before it wrote. Alice is the lane's first
  holder; she gives Bob and Erin a permission for the lane, and Bob gives
  Dan one beneath his. Each yes is for Erin: for her act X, a fact of one
  value under a plain key in her run :run-x, unless a case says other. A
  number is the uuid of an act-id, and of a yes's id from 201 on."
  (let [thing (fn [n who permission session & facts]
                {:act-id (act-id n) :depot :streaming declaration/time-part now
                 :version 1 :who who :lane :alice :permission (perm/permission-id permission) :session session
                 :facts (vec facts)})
        maker (fn [who session & facts] {:version 1 :who who :lane :alice :session session :facts (vec facts)})
        under (fn [n permission maker-act]
                (merge maker-act {:act-id (act-id n) :depot :streaming declaration/time-part now
                                  :permission (perm/permission-id permission)}))
        yes (fn [n holder above maker-act]
              (perm/act-permission holder :alice above (uuid n) (digest/hex-for-a-yes maker-act)))
        give (fn [n giver held given]
               (thing n giver held :giver-s1 (perm/permission-fact given)))
        end-yes (fn [n who held a-yes] (thing n who held :ender-s1 (perm/end-fact a-yes)))
        erins (perm/given-permission (person :erin) :alice alice-own)
        bobs-2 (perm/given-permission (person :bob) :alice alice-own {:salt 2})
        dans (perm/given-permission (person :dan) :alice bobs)
        x (maker (person :erin) :run-x {:e :experiment-3 :k :dose :v 5})
        x-another-way {:version 1 :who (person :erin) :lane :alice :session :run-x :based-on {}
                       :because-of nil :facts (list {:e :experiment-3 :k :dose :v 5N :replaces nil})}
        x-changed (maker (person :erin) :run-x {:e :experiment-3 :k :dose :v 50})
        x-in-another-run (maker (person :erin) :run-y {:e :experiment-3 :k :dose :v 5})
        end-of-dans (maker (person :erin) :run-x (perm/end-fact dans))
        its-own-end (maker (person :erin) :run-x (perm/end-act-id-fact (uuid 210)))
        ya (yes 201 (person :erin) bobs x)
        yb (yes 202 (person :erin) alice-own x)
        yc (yes 203 (person :erin) bobs x)
        yd (yes 201 (person :erin) bobs x-changed)
        ye (yes 204 (person :erin) bobs x)
        yg (yes 205 (person :erin) dans x)
        yh (yes 206 (person :erin) dans x)
        yi (yes 207 (person :erin) dans x)
        yk (yes 209 (person :erin) alice-own end-of-dans)
        yl (yes 210 (person :erin) alice-own its-own-end)
        ym (yes 211 (person :erin) dans x)
        a-give (maker (person :erin) :run-x
                      (perm/permission-fact (perm/act-permission (person :fay) :alice {:id (uuid 212) :value {:holder (person :erin)}}
                                                                 (uuid 299) (digest/hex-for-a-yes x))))
        yn (yes 212 (person :erin) alice-own a-give)
        yes-ids (set (map perm/permission-id [ya yb yc yd ye yg yh yi yk yl ym yn]))
        line [(apply thing 1 (person :alice) nil :alice-s1 (perm/making-facts :alice (person :alice)))
              (give 2 (person :alice) alice-own bobs)
              (give 3 (person :alice) alice-own erins)
              (give 4 (person :bob) bobs dans)
              (under 5 ya x)
              (give 6 (person :bob) bobs ya)
              (under 7 ya x-changed)
              (under 8 ya x-in-another-run)
              (under 5 ya x)
              (under 10 ya x-another-way)
              (under 11 ya x)
              (end-yes 12 (person :bob) bobs ya)
              (give 13 (person :bob) bobs yd)
              (give 14 (person :alice) alice-own yb)
              (give 15 (person :bob) bobs yc)
              (under 16 yc x)
              (under 17 yb x)
              (under 18 yb x)
              (give 19 (person :bob) bobs ye)
              (end-yes 20 (person :bob) bobs ye)
              (under 21 ye x)
              (give 22 (person :bob) bobs ye)
              (give 23 (person :dan) dans yg)
              (end-yes 24 (person :bob) bobs yg)
              (under 25 yg x)
              (give 26 (person :dan) dans yh)
              (end-yes 27 (person :erin) erins yh)
              (give 28 (person :dan) dans yi)
              (give 29 (person :alice) alice-own yk)
              (under 30 yk end-of-dans)
              (give 31 (person :alice) alice-own yl)
              (under 32 yl its-own-end)
              (under 33 yl its-own-end)
              (give 34 (person :alice) alice-own yn)
              (under 35 yn a-give)
              (give 36 (person :dan) dans ym)
              (thing 37 (person :alice) alice-own :alice-s1 (perm/end-fact bobs))
              (under 38 ym x)
              (give 39 (person :alice) alice-own bobs-2)
              (under 40 ym x)
              (under 41 yi x)
              (end-yes 42 (person :erin) erins ym)]
        after (walked-line empty-task :streaming line)
        writes (writes-of-line empty-task :streaming line)
        facts (:facts after)
        row-of (fn [a-yes] (get facts (chain/place :alice (perm/permission-id a-yes))))
        hex (digest/hex-for-a-yes x)]
    [["a yes for one act sits on its own id, the uuid that its giver made, under :softland.store/permission; what it covers is [:exact-act digest]"
      (fn [] [(chain/place :alice (perm/permission-id ya))
              (:covers (perm/permission-value ya))
              (:allows (perm/permission-value ya))])
      [(str ":alice " (uuid 201) " :softland.store/permission") [:exact-act hex] {:write :all}]]
     ["the digest that a yes holds is of the act as its maker made it, in its one form: 64 hex characters; the same for 5 and 5N, facts as a list or a vector, an empty part left out or given, with a permission or none, and as the thing arrives under any act-id with the time on it (the review's 1C, both halves)"
      (fn [] [(count hex) (some? (re-matches #"[0-9a-f]{64}" hex))
              (mapv #(= hex (digest/hex-for-a-yes %))
                    [x-another-way (assoc x :permission (perm/permission-id ya)) (assoc x :permission (perm/permission-id alice-own))
                     (under 10 ya x) (under 99 ya x-another-way) (assoc (under 10 ya x) declaration/time-part (inc now))])])
      [64 true [true true true true true true]]]
     ["it is the act's one form without its permission: what `softland.store.act/parse` gives, part for part, less the permission"
      (fn [] [(= (digest/act-for-a-yes x) (dissoc (:ok (act/parse (assoc x :permission (perm/permission-id ya)))) :permission))
              (= (digest/act-for-a-yes x-another-way) (digest/act-for-a-yes x))])
      [true true]]
     ["another act has another digest: another value, another who, another session, another lane, an act that it is because of, a fact that it is based on"
      (fn [] (mapv #(= hex (digest/hex-for-a-yes %))
                   [x-changed (assoc x :who (person :bob)) x-in-another-run (assoc x :lane :other)
                    (assoc x :because-of (act-id 1)) (assoc x :based-on {[(act-id 1) 0] 3})]))
      [false false false false false false]]
     ["no digest of what is no act: no map, a part that the form does not name, a fact that is wrong"
      (fn [] (mapv digest/hex-for-a-yes [nil 5 (assoc x :sealed 1) (assoc x :facts [{:e :x :k "no key"}])]))
      [nil nil nil nil]]
     ["the gate makes the digest of a thing under a yes as the maker makes it of her act, and makes none for an act under another permission"
      (fn [] (let [ya-place (chain/place :alice (perm/permission-id ya))]
               [(= hex (decision/hex-for-its-permission (under 10 ya x-another-way)
                                                        (act-of (under 10 ya x-another-way))
                                                        {ya-place (permission-row ya 1)}))
                (decision/hex-for-its-permission good (act-of good) {alices-place (permission-row alice-own)})]))
      [true nil]]
     ["a line of a lane's acts under yeses for one act: a try before the yes lands, one with another value and one in another run are refused and use nothing; the same thing again under its act-id is not decided; the act written another way is taken under any act-id of its maker's and uses the yes up, so the same act again is refused and the used yes has nothing to end; its id takes no permission again. Two yeses for one act stand at once, and each is used once. A yes is ended by its giver, by a lender above it, by an owner, and by its holder; under a yes she cannot end another's permission, while a yes whose act is its own end ends itself. A yes covers only its own act, so giving another permission under it is refused and uses nothing. A lent yes falls when a permission above it ends, does not come back when a new one is given, and its holder ends it all the same"
      (fn [] (words-by-number after))
      {1 :accepted 2 :accepted 3 :accepted 4 :accepted
       5 :permission-never-given 6 :accepted 7 :act-not-the-one-permitted 8 :act-not-the-one-permitted
       10 :accepted 11 :permission-used-up 12 :permission-already-used 13 :a-permission-id-takes-one-permission
       14 :accepted 15 :accepted 16 :accepted 17 :accepted 18 :permission-used-up
       19 :accepted 20 :accepted 21 :permission-ended 22 :a-permission-id-takes-one-permission
       23 :accepted 24 :accepted 25 :permission-ended
       26 :accepted 27 :accepted 28 :accepted 29 :accepted 30 :may-not-end-the-permission
       31 :accepted 32 :accepted 33 :permission-ended 34 :accepted 35 :permission-covers-more-than-the-acts-own
       36 :accepted 37 :accepted 38 :permission-above-ended 39 :accepted 40 :permission-above-ended
       41 :permission-above-ended 42 :accepted}]
     ["the same thing again under the act-id of a refused try is not decided and writes nothing; the try's answer stays its own"
      (fn [] [(nth writes 8) (:refused (get-in after [:answers (record/answer-key :alice (uuid 5))]))])
      [[] :permission-never-given]]
     ["the accept under a yes writes the act's fact, the yes's use, the answer and the last stamp; the use is an end at the yes's place, written by the act and by no fact of it, so it names the act and no index, and the yes as the permission that its act named; a gate may copy it to other tasks with the fact"
      (fn [] (let [w (nth writes 9)
                   [_ use-key use-row] (second w)]
               [(mapv first w) use-key (record/used? use-row) (:by-act use-row) (contains? use-row :by-index)
                (:permission use-row) (mapv first ((copies-of-as-written) w))]))
      [[:fact :fact :answer :last-stamp] (chain/place :alice (perm/permission-id ya)) true (uuid 10) false
       (str (perm/permission-id ya)) [:fact :fact]]]
     ["after the line: the places of the used yeses hold their use, by the act that used each; a yes that ends itself holds its own end, which is its use; an ended yes holds an end that is no use; a yes under which a try was refused stands still"
      (fn [] [(mapv (fn [a-yes]
                      (let [r (row-of a-yes)]
                        [(or (record/used? r) (= (perm/permission-id a-yes) (record/permission-of r)))
                         (.getLeastSignificantBits ^UUID (:by-act r))]))
                    [ya yb yc yl ye yg yh])
              (mapv #(record/held-permission (row-of %)) [yk yn])])
      [[[true 10] [true 17] [true 16] [true 32] [false 20] [false 24] [false 27]]
       [(perm/permission-value yk) (perm/permission-value yn)]]]
     ["no other act is taken under a yes: of the line's acts under a yes, the taken are the act once for each yes that stood, and the yes's own end"
      (fn [] (into (sorted-set)
                   (keep (fn [x] (let [n (.getLeastSignificantBits ^UUID (nth (:act-id x) 2))]
                                   (when (and (contains? yes-ids (:permission x))
                                              (nil? (:refused (get-in after [:answers (record/answer-key :alice (nth (:act-id x) 2))]))))
                                     n))))
                   line))
      #{10 16 17 32}]]))

(def ^:private review-cases
  "The histories of the overnight review's probe of the gate
  (src/proposal/runtime-2026-11-21/overnight-audit/probes/break_piece_2.clj,
  probes 1A, 1B, 2, 6C and 6D), written again for a yes with an id of its
  own, since the probe was written for the form before it and no longer
  loads: a yes then sat on the uuid of its act's act-id, and its digest was
  of the thing with its act-id. Each history keeps the probe's who, lanes,
  acts and order, with the yes on an id of its own and its digest of the act
  as its maker made it; the act goes under an act-id of its maker's. Walked
  in memory, each act against the rows that the acts before it wrote."
  (let [thing (fn [lane n who permission session & facts]
                {:act-id [lane :streaming (uuid n)] :depot :streaming declaration/time-part now
                 :version 1 :who who :lane lane :permission (perm/permission-id permission) :session session :facts (vec facts)})
        making (fn [lane who]
                 (apply thing lane 1 who nil :s0 (perm/making-facts lane who)))
        under (fn [lane n permission maker-act]
                (merge maker-act {:act-id [lane :streaming (uuid n)] :depot :streaming
                                  declaration/time-part now :permission (perm/permission-id permission)}))
        words (fn [lane line] (let [after (walked-line empty-task :streaming line)]
                                 (mapv (fn [x] (or (:refused (get-in after [:answers (record/answer-key lane (nth (:act-id x) 2))]))
                                                   :accepted))
                                       line)))
        ;; 1A: Asha's lane; Ben's yes, ended, a try while it is ended, Cal given a permission, a yes again
        lab-root (perm/first-permission (person :asha) :lab)
        x {:version 1 :who (person :ben) :lane :lab :session :run-1 :facts [{:e :exp-1 :k :dose :v "5 mg"}]}
        pa1 (perm/act-permission (person :ben) :lab lab-root (uuid 301) (digest/hex-for-a-yes x))
        cal-perm (perm/given-permission (person :cal) :lab lab-root :see-and-act)
        pa2-same-id (perm/act-permission (person :ben) :lab cal-perm (uuid 301) (digest/hex-for-a-yes x))
        pa2 (perm/act-permission (person :ben) :lab cal-perm (uuid 302) (digest/hex-for-a-yes x))
        history-1a [(making :lab (person :asha))
                    (thing :lab 2 (person :asha) lab-root :s0 (perm/permission-fact pa1))
                    (thing :lab 3 (person :asha) lab-root :s0 (perm/end-fact pa1))
                    (under :lab 4 pa1 x)
                    (thing :lab 5 (person :asha) lab-root :s0 (perm/permission-fact cal-perm))
                    (thing :lab 6 (person :cal) cal-perm :s0 (perm/permission-fact pa2-same-id))
                    (thing :lab 7 (person :cal) cal-perm :s0 (perm/permission-fact pa2))
                    (under :lab 8 pa2 x)]
        ;; 1B: Ida's lane; Cal may only see; Joe's yes; Cal sends under an act-id whose uuid is the yes's id
        ida-own (perm/first-permission (person :ida) :studio)
        cal-see (perm/given-permission (person :cal) :studio ida-own :see)
        joes {:version 1 :who (person :joe) :lane :studio :session :run-1 :facts [{:e :secret-exp :k :dose :v "5 mg"}]}
        pa (perm/act-permission (person :joe) :studio ida-own (uuid 399) (digest/hex-for-a-yes joes))
        history-1b [(making :studio (person :ida))
                    (thing :studio 2 (person :ida) ida-own :s0 (perm/permission-fact cal-see))
                    (thing :studio 3 (person :ida) ida-own :s0 (perm/permission-fact pa))
                    (thing :studio 399 (person :cal) cal-see :cal-s {:e :anything :k :v :v 1})
                    (thing :studio 398 (person :zed) (perm/given-permission (person :zed) :studio ida-own) :zed-s {:e :anything :k :v :v 2})
                    (under :studio 5 pa joes)]
        ;; 6C: Ben's yes for the act that ends it, and his run's permission that says see
        u50 (uuid 350)
        ends-itself {:version 1 :who (person :ben) :lane :lab6 :session :run-1 :facts [(perm/end-act-id-fact u50)]}
        a-doc {:version 1 :who (person :ben) :lane :lab6 :session :run-1 :facts [{:e :doc :k :v :v 1}]}
        root6 (perm/first-permission (person :asha) :lab6)
        pa-doc (perm/act-permission (person :ben) :lab6 root6 (uuid 351) (digest/hex-for-a-yes a-doc))
        pa-end (perm/act-permission (person :ben) :lab6 root6 u50 (digest/hex-for-a-yes ends-itself))
        r-see (perm/run-permission (person :ben) :lab6 root6 :run-1 :see)
        history-6c [(making :lab6 (person :asha))
                    (thing :lab6 2 (person :asha) root6 :s0
                           (perm/permission-fact pa-doc) (perm/permission-fact pa-end)
                           (perm/permission-fact r-see))
                    (under :lab6 3 pa-doc (assoc a-doc :facts [(perm/end-fact pa-doc)]))
                    (under :lab6 4 pa-end ends-itself)
                    (under :lab6 5 pa-end ends-itself)
                    (thing :lab6 6 (person :ben) r-see :run-1 (perm/end-fact r-see))]
        ;; 2 and 6D: the maker's act, the thing that the door appends, the act written another way
        maker-act {:version 1 :who (person :joe) :lane :studio :session :run-1 :facts [{:e :exp :k :dose :v "5 mg"}]}
        door-id [:studio :streaming (uuid 77)]
        appended (fn [an-act permission]
                   (:thing (additions/thing-to-append (additions/reading (assoc an-act :permission (perm/permission-id permission)) door-id)
                                                      door-id now)))
        a1 {:version 1 :who (person :ben) :lane :lab :session :s1 :facts [{:e :x :k :n :v 5}]}
        a2 {:version 1 :who (person :ben) :lane :lab :session :s1 :because-of nil :based-on {} :facts (list {:e :x :k :n :v 5N})}]
    [["probe 1A, a yes given again after an end and a refused try: the try under the ended yes is refused and uses nothing; a give at the ended yes's id is refused, a yes's id taking one give; a yes on an id of its own, given by Cal beneath his permission, is taken, and so is Ben's act under it"
      (fn [] (words :lab history-1a))
      [:accepted :accepted :accepted :permission-ended :accepted :a-permission-id-takes-one-permission :accepted :accepted]]
     ["probe 1A: Ben's act is in the store, and the yes that took it is used up"
      (fn [] (let [after (walked-line empty-task :streaming history-1a)]
               [(:value (get-in after [:facts (record/place-key :lab :exp-1 :dose)]))
                (record/used? (get-in after [:facts (chain/place :lab (perm/permission-id pa2))]))]))
      ["\"5 mg\"" true]]
     ["probe 1B, an outsider sends under an act-id whose uuid is the yes's own id: Cal, who may only see, is refused, and so is Zed, who names a permission that does not stand; Joe's act under the yes, under an act-id of his own, is taken: the yes's id is no act-id, and nothing another sends under it touches the yes"
      (fn [] (words :studio history-1b))
      [:accepted :accepted :accepted :permission-does-not-allow-writing :permission-never-given :accepted]]
     ["probe 2, the maker's act and the thing that the door appends: the digest that the maker makes of her act is the gate's of the thing, under the door's act-id, and the door takes her act as she gave it"
      (fn [] (let [hex (digest/hex-for-a-yes maker-act)
                   permission (perm/act-permission (person :joe) :studio (perm/first-permission (person :ida) :studio) (uuid 377) hex)
                   thing (appended maker-act permission)]
               [(= hex (decision/hex-for-its-permission thing (act-of thing)
                                                        {(chain/place :studio (perm/permission-id permission))
                                                         (permission-row permission)}))
                (contains? thing :act-id)]))
      [true true]]
     ["probe 6C, a holder ends her yes or her sitting permission: a yes for another act cannot end itself, its act being another; a yes for the act that ends it does, and the same end again is refused as ended; a sitting's permission that says see still lets its holder end her own permission"
      (fn [] (words :lab6 history-6c))
      [:accepted :accepted :act-not-the-one-permitted :accepted :permission-ended :accepted]]
     ["probe 6D, the act written two ways, 5 and 5N, a vector and a list, empty parts left out and given: one act in its one form, and one digest"
      (fn [] [(= (dissoc (:ok (act/parse (assoc a1 :permission (perm/permission-id (perm/first-permission (person :ben) :lab))))) :permission)
                 (dissoc (:ok (act/parse (assoc a2 :permission (perm/permission-id (perm/first-permission (person :ben) :lab))))) :permission))
              (= (digest/hex-for-a-yes a1) (digest/hex-for-a-yes a2))])
      [true true]]]))

(def ^:private second-review-cases
  "The faults that the review of 22 November 2026 found in the gate's half
  of piece 2 (src/proposal/runtime-2026-11-21/overnight-audit/REVIEW-store-2026-11-22.md,
  P2 and two of its P3), written as cases of the store's own."
  (let [root (perm/first-permission (person :asha) :lab)
        cal-p (perm/given-permission (person :cal) :lab root :see-and-act)
        thing (fn [n who permission session & facts]
                {:act-id [:lab :streaming (uuid n)] :depot :streaming declaration/time-part now
                 :version 1 :who who :lane :lab :permission (perm/permission-id permission) :session session :facts (vec facts)})
        maker (fn [session & facts] {:version 1 :who (person :ben) :lane :lab :session session :facts (vec facts)})
        under (fn [n permission maker-act]
                (merge maker-act {:act-id [:lab :streaming (uuid n)] :depot :streaming
                                  declaration/time-part now :permission (perm/permission-id permission)}))
        act-a (maker :run-1 {:e :doc :k :v :v "act-A"})
        act-b (maker :run-1 {:e :doc :k :v :v "act-B"})
        hex-a (digest/hex-for-a-yes act-a)
        y-used (uuid 501)
        y-ended (uuid 502)
        y-live (uuid 503)
        pa-used (perm/act-permission (person :ben) :lab root y-used hex-a)
        pa-ended (perm/act-permission (person :ben) :lab root y-ended hex-a)
        pa-live (perm/act-permission (person :ben) :lab root y-live hex-a)
        run-p (perm/run-permission (person :ben) :lab root :run-1 :see-and-act)
        run-live (perm/run-permission (person :ben) :lab root :run-1 :see-and-act {:salt 2})
        setup [(apply thing 1 (person :asha) nil :s0 (perm/making-facts :lab (person :asha)))
               (thing 2 (person :asha) root :s0 (perm/permission-fact cal-p)
                      (perm/permission-fact pa-used) (perm/permission-fact pa-ended)
                      (perm/permission-fact pa-live) (perm/permission-fact run-p)
                      (perm/permission-fact run-live))
               (under 3 pa-used act-a)
               (thing 4 (person :asha) root :s0 (perm/end-fact pa-ended) (perm/end-fact run-p))]
        tries [(under 10 pa-used act-b)
               (under 11 pa-ended act-b)
               (thing 12 (person :ben) run-p :run-2 {:e :doc :k :v :v "x"})
               (under 13 pa-live act-b)
               (thing 14 (person :ben) run-live :run-2 {:e :doc :k :v :v "x"})
               (under 15 (uuid 599) act-a)]
        word-of (fn [x] (let [after (walked-line empty-task :streaming (conj setup x))]
                          (or (:refused (get-in after [:answers (record/answer-key :lab (nth (:act-id x) 2))])) :accepted)))
        long-id (keyword (apply str (repeat 260 \a)))
        refused-makers [(apply maker :s1 (map (fn [i] {:e (keyword (str "e" i)) :k :v :v i}) (range 257)))
                        (maker :s1 {:e :doc :k :body :v (apply str (repeat 70000 \x))})
                        (maker :s1 {:e :doc :k :v :v 1} {:e :doc :k :v :v 2})
                        (maker :s1 {:e :doc :k :softland.store/not-a-key :v 1})
                        (maker :s1 {:e long-id :k :v :v 1})]
        at-the-limits [(apply maker :s1 (map (fn [i] {:e (keyword (str "e" i)) :k :v :v i}) (range 256)))
                       (maker :s1 {:e :doc :k :body :v (apply str (repeat 65000 \x))})
                       (maker :s1 {:e :doc :k :v :v 1 :replaces [[:lab :streaming (java.util.UUID. 0 0)] 0]})]
        after (walked-line empty-task :streaming setup)
        use-row (get-in after [:facts (chain/place :lab (perm/permission-id pa-used))])]
    [["a yes or a sitting permission whose place holds a use or an end is answered from that row first: used up, and ended"
      (fn [] (mapv word-of (take 3 tries)))
      [:permission-used-up :permission-ended :permission-ended]]
     ["while a yes or a sitting permission stands, another act or sitting is refused for what the row says, and an id never given is answered as never given"
      (fn [] (mapv word-of (drop 3 tries)))
      [:act-not-the-one-permitted :permission-for-another-sitting :permission-never-given]]
     ["the maker's digest of an act that the declaration refuses under every yes is none: too many facts, a value over its bound, a place named twice, a key of the store's that is no store key, an id over its limit; the gate's digest of a thing that its declaration took still holds such an act's text, and is asked of no such act"
      (fn [] [(mapv digest/hex-for-a-yes refused-makers) (mapv #(some? (digest/hex-of-the-act %)) refused-makers)])
      [[nil nil nil nil nil] [true true true true true]]]
     ["an act at the declaration's limits has its digest, the same as the gate's: 256 facts, a value just under its bound, a fact that replaces a fact of an act-id that a door could have given it"
      (fn [] (mapv #(and (some? (digest/hex-for-a-yes %)) (= (digest/hex-for-a-yes %) (digest/hex-of-the-act %))) at-the-limits))
      [true true true]]
     ["the use of a yes names the act that used it and no fact of it: its row holds no index, and no fact id, the act's count of facts among them, names it"
      (fn [] [(record/used? use-row) (contains? use-row :by-index) (:by-act use-row)
              (record/written-by? :streaming :lab use-row [[:lab :streaming (uuid 3)] 1])
              (record/written-by? :streaming :lab use-row [[:lab :streaming (uuid 3)] 0])])
      [true false (uuid 3) false false]]]))

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
  {:act-id [:base :streaming (uuid 18)] :depot :streaming :lane :base :facts [{:e :p1 :k :stray}]})

(def ^:private line
  "A line of things into a base that is made, and the record before it. The
  things are in the order of the depot. Among them: two acts that replace
  one fact; two that write one place and name nothing; a give and an act
  under it, in both orders; a value alone on its entity; a repeat; one
  act-id under two things; two acts for lanes of the other gate; a plain
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
      (base-thing 5 :system system-own (perm/permission-fact carols))
      (base-thing 6 (person :carol) carols {:e :carol :k :note})
      (base-thing 7 (person :dave) daves-in-base {:e :dave :k :note})
      (base-thing 8 :system system-own (perm/permission-fact daves-in-base))
      ;; a value alone on its entity
      (base-thing 9 :system system-own (text-on :z2 2))
      ;; a repeat, and one act-id under two things
      answered
      (base-thing 14 :system system-own {:e :p1 :k :twice})
      (base-thing 14 :system system-own {:e :p2 :k :twice})
      ;; two acts for lanes of the other gate
      {:act-id [:alice :microbatch (uuid 15)] :depot :microbatch declaration/time-part now :version 1 :who (person :alice) :lane :alice
       :permission (perm/permission-id alice-own) :session :s1 :facts [{:e :p1 :k :stranger}]}
      {:act-id [:nobody :microbatch (uuid 16)] :depot :microbatch declaration/time-part now :version 1 :who (person :alice) :lane :nobody
       :permission (perm/permission-id (perm/first-permission (person :alice) :nobody)) :session :s1 :facts [{:e :p2 :k :stranger}]}
      ;; a plain value under a plain key, and two strays
      (base-thing 17 :system system-own {:e :p1 :k :plain :v "a plain value"})
      5
      stray-of-the-other-gate
      ;; no standing: by a who that is not the holder, carrying the greatest stamp there is; with a permission that no act gives; by a who that is not the holder
      (assoc (base-thing 19 (person :mallory) carols {:e :p3 :k :m}) :based-on {[(base-id 802) 0] Long/MAX_VALUE})
      (base-thing 20 (person :mallory) mallorys {:e :p3 :k :m})
      (base-thing 21 (person :mallory) system-own {:e :p3 :k :m})
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
            (dissoc (apply base-thing 31 :system nil (perm/making-facts :base :system)) :permission)
            (dissoc (apply base-thing 32 :system nil (reverse (perm/making-facts :base :system))) :permission)
            (base-thing 33 :system system-own {:e :base :k :note-2})
            (base-thing 34 :system system-own (perm/permission-fact carols))
            (base-thing 35 (person :carol) carols {:e :carol :k :note})
            (base-thing 36 (person :carol) carols (value-at :carol :text "a value in a base made in this line"))]})

(def ^:private marks-line
  "A line of things into a base that marks two keys, :on and :gone, and the
  record before it."
  (let [since 20
        before before-the-line
        older 10
        daves-from-see (perm/given-permission (person :dave) :base carols-see :see)
        fact-before (fn [e k v n stamp]
                      [:facts (record/place-key :base e k)
                       (fact-row base-writer (arrived-fact e k v) (uuid n) 0 stamp time-before-the-line)])
        entry-before (fn [e k v n]
                       [:by-value (record/by-value-key :base k since (record/value-digest-of v) e)
                        (by-value-row base-writer (uuid n) 0 before time-before-the-line)])]
    {:since since
     :daves-from-see daves-from-see
     :record
     (task-with
      (into base-made
            [[:facts (chain/place :base (perm/permission-id carols-see))
              (let [{:keys [e k v]} (perm/permission-fact carols-see)]
                (fact-row base-writer (arrived-fact e k v) (uuid 806) 0 before time-before-the-line))]
             [:by-holder (record/by-holder-key :base (person :carol) nil (perm/permission-id carols-see)) before]
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
      ;; two writes of who may read acts; two of the list that name one fact to replace
      (base-thing 48 :system system-own {:e :base :k :softland.store/who-may-read-acts :v :members})
      (base-thing 49 :system system-own {:e :r3 :k :note} {:e :base :k :softland.store/who-may-read-acts :v :everyone})
      (base-thing 50 :system system-own {:e :base :k :softland.store/builds-on :v [:team] :replaces [(base-id 804) 0]})
      (base-thing 51 :system system-own {:e :r3 :k :note-2} {:e :base :k :softland.store/builds-on :v [] :replaces [(base-id 804) 0]})
      ;; a holder of see: she writes, she names a permission never given, she tries to pass see to another person
      (base-thing 52 (person :carol) carols-see {:e :carol :k :note})
      (base-thing 53 (person :carol) carols {:e :carol :k :note})
      (base-thing 54 (person :carol) carols-see (perm/permission-fact daves-from-see))
      ;; a retract of a fact since the mark; a fact that names what it replaces
      (base-thing 55 :system system-own {:e :q3 :k :on})
      (base-thing 56 :system system-own (assoc (value-at :r2 :on :c7) :replaces [(base-id 805) 0]))
      ;; a retract of the marks; marks on another entity
      (base-thing 57 :system system-own {:e :base :k :softland.store/marked-keys})
      (base-thing 58 :system system-own {:e :p1 :k :softland.store/marked-keys :v #{:on}})
      ;; a holder of see who sends a value under a marked key: refused under its own act-id, and no row of its value
      (base-thing 61 (person :carol) carols-see (value-at :carol :on :c1))]}))

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
     ["what a gate may copy of them, by the proof's own filter and by `copies-of`: `:fact`, `:by-value`, and `:holder-permission` in their order, a nil row among them, and never `:owner`, `:answer` or `:last-stamp`"
      (fn [] (let [making-writes (decision/writes [:team :streaming (uuid 1)] (making :team (person :alice) (person :alice))
                                                  nil stamp now (digest/digest good) {})
                   copies-of (copies-of-as-written)]
               [(shape (to-copy accept))
                (= (to-copy accept) (subvec accept 0 5))
                (mapv first making-writes)
                (mapv first (to-copy making-writes))
                (= (to-copy making-writes) (copies-of making-writes))]))
      [[[:fact note-place true] [:fact on-place true] [:by-value (entry :c0) false] [:by-value (entry :c1) true]
        [:fact marks-place true]]
       true
       [:fact :fact :holder-permission :owner :answer :last-stamp]
       [:fact :fact :holder-permission]
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
                               [:facts alices-place (permission-row alice-own)]
                               [:by-holder (record/by-holder-key :alice (person :alice) nil (perm/permission-id alice-own)) 0]
                               [:owners ":alice" #{(perm/permission-id alice-own)}]
                               [:facts ":alice :note-1 :text" (row-by 7 0)]]
                              {":alice" 200})
        base-with-a-fact (task-with (conj base-made
                                          [:facts ":base :doc-1 :title"
                                           (fact-row base-writer (arrived-fact :doc-1 :title "before") (uuid 802) 0 before-the-line
                                                     time-before-the-line)])
                                    {":base" before-the-line})]
    [["at the streaming gate, the making act of a group's lane" :streaming empty-task (as-sent (making :team (person :alice) (person :alice)) :streaming)
      [:fact :fact :holder-permission :owner :answer :last-stamp] nil]
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
      :microbatch empty-task (dissoc (apply base-thing 31 :system nil (perm/making-facts :base :system)) :permission)
      [:fact :fact :holder-permission :owner :answer :last-stamp] nil]
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
  whether that run left every table as it was, the lane's last stamp
  too."
  (map (fn [[what gate record thing kinds word]]
         [(str "a second run of one decision: " what)
          (fn [] (let [first-run (walked record gate thing)
                       second-run (walked record gate thing)
                       on-what-it-left (walked first-run gate thing)
                       [lane _ act-uuid] (:act-id thing)
                       answer (get-in first-run [:answers (record/answer-key lane act-uuid)])]
                   [(mapv first (:wrote first-run))
                    (if answer (:refused answer) :no-answer)
                    (= (comparable (:wrote first-run)) (comparable (:wrote second-run)))
                    (:wrote on-what-it-left)
                    (= (comparable (dissoc first-run :wrote :wrote-line)) (comparable (dissoc on-what-it-left :wrote :wrote-line)))]))
          [kinds word true [] true]])
       second-run-rows))

;; ----------------------------------------------------- the stamp in a line

(def ^:private stamp-line-cases
  "The cases of the stamp in a line, each decided by the walk in memory, in
  a lane of each gate: ten acts whose times rise, stand still, go back by
  two milliseconds and by a minute, leap far ahead, come back, fall to 0,
  leap to the greatest long and rise again; the same ten acts all with the
  time `now`; a line of accepts and refusals, one of them with no time and
  one with a time far ahead, and an act that carries the greatest stamp
  there is; and an act with a time far ahead, then two acts after it that
  each replace the fact before and carry the stamp that its answer gave.
  Alice's lane is made with the stamp 0, so its line counts on from 1; the
  base's from `before-the-line` plus one."
  (let [times [now (+ now 5) (+ now 5) (+ now 3) (- now 60000) far-ahead (+ now 6) 0 Long/MAX_VALUE (+ now 2000)]
        timed (fn [thing t] (assoc thing declaration/time-part t))
        alices-at (fn [times] (vec (map-indexed (fn [i t] (timed (arrived (+ 400 i) (value-at (keyword (str "n" i)) :text)) t)) times)))
        bases-at (fn [times] (vec (map-indexed (fn [i t] (timed (base-thing (+ 400 i) :system system-own (value-at (keyword (str "n" i)) :text)) t))
                                               times)))
        alices (alices-at times)
        bases (bases-at times)
        alice-made (task-with [[:facts ":alice :alice :softland.store/depot" depot-row]
                               [:facts alices-place (permission-row alice-own)]
                               [:by-holder (record/by-holder-key :alice (person :alice) nil (perm/permission-id alice-own)) 0]
                               [:owners ":alice" #{(perm/permission-id alice-own)}]]
                              {":alice" 0})
        base-is-made (task-with base-made {":base" before-the-line})
        answer-in (fn [record thing] (let [[lane _ act-uuid] (:act-id thing)] (get-in record [:answers (record/answer-key lane act-uuid)])))
        stamps-in (fn [record things] (mapv #(:stamp (answer-in record %)) things))
        times-in (fn [record things] (mapv #(:time (answer-in record %)) things))
        fact-times (fn [record lane] (mapv (fn [i] (:time (get-in record [:facts (record/place-key lane (keyword (str "n" i)) :text)])))
                                            (range 10)))
        of-alices (walked-line alice-made :streaming alices)
        of-the-base (walked-line base-is-made :microbatch bases)
        ;; accepts and refusals in Alice's lane, one after the other
        mixed [(arrived 510 (value-at :m1 :text))
               (without-time (arrived 511 (value-at :m2 :text)))
               (arrived 512 (value-at :m3 :text))
               (assoc (timed (arrived 513 (value-at :m4 :text)) far-ahead) :version 2)
               (assoc (arrived 514 (value-at :m5 :text)) :who (person :mallory))
               (arrived 515 (assoc (value-at :m1 :text "b") :replaces [(act-id 999) 0]))
               (assoc (arrived 516 (value-at :m6 :text)) :based-on {[(act-id 510) 0] Long/MAX_VALUE})
               (arrived 517 (value-at :m7 :text))]
        of-mixed (walked-line alice-made :streaming mixed)
        ;; an act with a time far ahead; then an act that replaces its fact and carries its stamp; then one that does the same for that act
        honest-after-far (fn [gate thing-of id-of record]
                           (let [lane (first (id-of 520))
                                 last-stamp (get-in record [:last-stamp (declaration/id-text lane)])
                                 things [(timed (thing-of 520 (value-at :k1 :text "a")) far-ahead)
                                         (assoc (thing-of 521 (assoc (value-at :k1 :text "b") :replaces [(id-of 520) 0]))
                                                :based-on {[(id-of 520) 0] (+ last-stamp 1)})
                                         (assoc (thing-of 522 (assoc (value-at :k1 :text "c") :replaces [(id-of 521) 0]))
                                                :based-on {[(id-of 521) 0] (+ last-stamp 2)})]
                                 after (walked-line record gate things)]
                             [(vec (vals (words-by-number after))) (mapv #(- % last-stamp) (stamps-in after things)) (times-in after things)
                              (:value (get-in after [:facts (record/place-key lane :k1 :text)]))]))]
    [["ten acts of a streaming lane, whose times go back, leap ahead to 2^50 ms and to the greatest long, and fall to 0: each is accepted, the stamps of their answers are 1 to 10 after the making act's 0, and the last stamp kept for the lane is 10"
      (fn [] [(vec (vals (words-by-number of-alices))) (stamps-in of-alices alices) (get-in of-alices [:last-stamp ":alice"])])
      [(vec (repeat 10 :accepted)) (vec (range 1 11)) 10]]
     ["the same ten acts in the base: the stamps count on from the base's last stamp before them, one by one"
      (fn [] [(vec (vals (words-by-number of-the-base))) (stamps-in of-the-base bases) (get-in of-the-base [:last-stamp ":base"])])
      [(vec (repeat 10 :accepted)) (vec (range (+ before-the-line 1) (+ before-the-line 11))) (+ before-the-line 10)]]
     ["each answer and each row of a fact holds the time on its act as it is, beside the stamp, in a lane of each gate: the time far ahead, the greatest long and 0 among them"
      (fn [] [(times-in of-alices alices) (fact-times of-alices :alice) (times-in of-the-base bases) (fact-times of-the-base :base)])
      [times times times times]]
     ["the times move no stamp and change no other field: the same ten acts with the time `now` on each give records that differ only in :time, in the answers and the rows of the nine acts whose time is not `now`, and the same last stamps, at a gate of each kind"
      (fn [] (mapv (fn [[of-the-times of-now]]
                     (let [differ (rows-that-differ (rows-by-place of-the-times) (rows-by-place of-now))]
                       [(count differ) (set (vals differ)) (= (:last-stamp of-the-times) (:last-stamp of-now))]))
                   [[of-alices (walked-line alice-made :streaming (alices-at (repeat 10 now)))]
                    [of-the-base (walked-line base-is-made :microbatch (bases-at (repeat 10 now)))]]))
      [[18 #{#{:time}} true] [18 #{#{:time}} true]]]
     ["accepts and refusals of one lane take their stamps by one rule: an accept, a thing with no time, an accept, a wrong version with a time far ahead, an act by a who that is not the holder, an act that names a fact that is not current, an accept that carries the greatest stamp there is, and an accept after it get the stamps 1 to 8 in their order, with no gap; each answer holds the time on its thing, and the one with no time holds none"
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
  lane of each gate: by hand over `softland.store.gate.decision/writes`,
  where the stamp of each place of the second act is taken from the mark's
  row as the first act wrote it; and by the walk in memory, for both gate
  words. In a lane that the line makes, the making act has the stamp 0,
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
        marks (fn [lane keys] {:e lane :k :softland.store/marked-keys :v keys})
        alices-making (as-sent (making :alice (person :alice) (person :alice)) :streaming)
        bases-making (dissoc (apply base-thing 1 :system nil (perm/making-facts :base :system)) :permission)
        in-the-line (fn [gate lane things]
                      (let [after (walked-line empty-task gate things)
                            [_ mark-thing fact-thing] things
                            stamp-under (fn [thing] (:stamp (get-in after [:answers (record/answer-key lane (nth (:act-id thing) 2))])))]
                        [(vec (vals (words-by-number after)))
                         (stamp-under mark-thing)
                         (record/marked-keys-of (get-in after [:facts (record/place-key lane lane :softland.store/marked-keys)]))
                         (set (keys (:by-value after)))
                         (:stamp (get-in after [:facts (record/place-key lane :b1 :on)]))
                         (mapv first (last (writes-of-line empty-task gate things)))]))
        entry (fn [lane since e] (record/by-value-key lane :on since (digest-of-text ":c1") e))]
    [["in a streaming lane, by hand: the row of the marks holds the stamp of the act that wrote it for the key; the next act's place under that key has that stamp; and among its writes is the fact's entry of the facts by value, under the key's stamp and with the fact's own stamp"
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
                                          (record/answer-row :lane-not-made before-the-line time-before-the-line (digest/digest repeat-b))]])
                                  {":base" before-the-line})
        answer-of-far (fn [thing] (let [row (get-in (walked base-is-made :microbatch thing) [:answers (record/answer-key :base (uuid 70))])]
                                    [(:refused row) (:stamp row) (:time row)]))]
    [["the word of each answer after the line, by the number of its act-id; the two strays have none"
      (fn [] words)
      {1 :accepted 2 :replaced-fact-not-current 3 :accepted 4 :accepted 5 :accepted 6 :accepted 7 :permission-never-given
       8 :accepted 9 :accepted 14 :accepted 15 :lane-belongs-to-the-other-gate 16 :lane-belongs-to-the-other-gate
       17 :accepted 19 :who-not-the-permissions-holder 20 :permission-never-given 21 :who-not-the-permissions-holder
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
      [:accepted :accepted :permission-never-given :accepted]]
     ["a value alone on its entity is accepted: its row holds the act and the text of the value"
      (fn [] [(words 9) (select-keys (get facts ":base :z2 :text") [:by-act :value])])
      [:accepted {:by-act (uuid 9) :value "\"text 2\""}]]
     ["one act-id under two things has one answer, that of the first thing: its fact is in the record and the second thing's is not. A thing under an act-id that had an answer before the line leaves that answer with its stamp, and writes no fact"
      (fn [] [(words 14) (:by-act (get facts ":base :p1 :twice")) (get facts ":base :p2 :twice")
              (:stamp (get-in after [:answers (record/answer-key :base (uuid 801))])) (get facts ":base :p1 :repeat")])
      [:accepted (uuid 14) nil before-the-line nil]]
     ["an act for a lane of the other gate has the word of such a lane, for each of two lanes that start with the streaming depot: the record of this gate holds nothing of either, and the walk in memory holds no other record to read. Neither writes a fact"
      (fn [] [(words 15) (words 16) (count (filter #(or (str/starts-with? % ":alice ") (str/starts-with? % ":nobody ")) (keys facts)))])
      [:lane-belongs-to-the-other-gate :lane-belongs-to-the-other-gate 0]]
     ["a plain value under a plain key is accepted, and its row holds the text of the value"
      (fn [] [(words 17) (:value (get facts ":base :p1 :plain"))])
      [:accepted "\"a plain value\""]]
     ["the strays have no answer: the digest of each is in the set of its word, the proof's own SHA-256 of the thing's text"
      (fn [] [(words 18) (:strays after)])
      [nil {:no-act-id #{(sha-256 (utf8 "5"))}
            :act-id-of-the-other-gate #{(sha-256 (utf8 (text/text stray-of-the-other-gate)))}}]]
     ["no standing: a who that is not the holder, whose act carries the greatest stamp there is and gets the word of its standing, since no check reads a carried stamp; a permission that no act gave; a who that is not the holder"
      (fn [] (mapv words [19 20 21]))
      [:who-not-the-permissions-holder :permission-never-given :who-not-the-permissions-holder]]
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
     ["the last stamp is kept for a lane: the base's is the stamp of its last decision; each lane of the other gate, of which one thing was decided and refused, has 0, the stamp of the first decision of a lane. So a decision in one lane does not raise the stamps of another"
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
     ["a line into a base that is not made: an act before the making is refused; the first making act makes the base; a second one names no permission in a base that is made and is refused; then an act, a give and two acts under it, one after the other, are accepted"
      (fn [] [(words-by-number made) (:by-act (get-in made [:facts ":base :base :softland.store/depot"]))
              (:value (get-in made [:facts ":base :carol :text"]))])
      [{30 :lane-not-made 31 :accepted 32 :no-permission 33 :accepted 34 :accepted 35 :accepted 36 :accepted}
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
       48 :accepted 49 :accepted 50 :accepted 51 :replaced-fact-not-current 52 :permission-does-not-allow-writing
       53 :permission-never-given 54 :permission-gives-more-than-its-giver-may 55 :accepted 56 :accepted
       57 :store-key-retract-not-built 58 :setting-not-on-the-lane 59 :accepted 60 :replaced-fact-not-current
       61 :permission-does-not-allow-writing}]
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
     ["two writes of who may read the lane's acts that name nothing are both accepted, and the row is the later one's"
      (fn [] [(words 48) (words 49) (record/read-value (get facts ":base :base :softland.store/who-may-read-acts"))])
      [:accepted :accepted [:value :everyone]]]
     ["two writes of what the lane builds on that name the same fact to replace: the first is accepted, the second is stale, and the row is the first one's"
      (fn [] [(words 50) (words 51) (record/read-value (get facts ":base :base :softland.store/builds-on"))])
      [:accepted :replaced-fact-not-current [:value [:team]]]]
     ["a holder of see: her write is refused; naming an id never given is refused; passing see to another person without the right to give is refused"
      (fn [] [(mapv words [52 53 54]) (get facts (chain/place :base (perm/permission-id (:daves-from-see marks-line))))])
      [[:permission-does-not-allow-writing :permission-never-given :permission-gives-more-than-its-giver-may] nil]]
     ["a holder of see who sends a value under a marked key: refused with the word under her act's own act-id, and no row of the value, in its place or in the order"
      (fn [] [(words 61) (run-of :on since :c1) (get facts ":base :carol :on")])
      [:permission-does-not-allow-writing [] nil]]
     ["a retract of the marks and marks on another entity are refused"
      (fn [] [(words 57) (words 58) (get facts ":base :p1 :softland.store/marked-keys")])
      [:store-key-retract-not-built :setting-not-on-the-lane nil]]]))

;; ----------------------------------------------------------- the door's words

(def ^:private door-words
  "The door's own seven words, written out a second time, each with its
  `:appended`: whether nothing was appended for the maker's act, or the door
  does not know."
  {:find-the-module       :nothing
   :make-an-act-id        :nothing
   :read-the-lanes-depot :nothing
   :read-the-act          :nothing
   :add-its-parts         :nothing
   :append-cleanly        :not-known
   :read-the-answer       :nothing})

(def ^:private read-accepted
  "What the read by act-id gives for an accepted act whose digest is \"aa\"."
  {:verdict :accepted :reason nil :stamp 77 :time now :digest "aa"})

(def ^:private read-refused
  "What the read by act-id gives for a refused act whose digest is \"aa\"."
  {:verdict :refused :reason :lane-not-made :stamp 78 :time now :digest "aa"})

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
     :read-the-lanes-depot #{:act-id-for :act-ids-for :send-act}
     :read-the-act          #{:send-act}
     :add-its-parts         #{:send-act}
     :append-cleanly        #{:send-act}
     :read-the-answer       #{:answer-of}}]
   ["what a word can carry, by name"
    (fn [] (set (keys words/carried)))
    #{:at :module :lane :act-id :digest :threw :said}]
   ["no word of the door's is a reason word of the act or of the declaration, or a word of a stray"
    (fn [] (filterv (set (concat act/reasons declaration/reasons declaration/stray-reasons)) (keys words/words)))
    []]
   ["a word as a maker gets it holds :could-not, its own :appended and what it carries, and no :answer and no :reason"
    (fn [] (words/word :append-cleanly {:act-id (act-id 1) :digest "aa" :answer :accepted :reason :lane-not-made :appended :nothing}))
    {:could-not :append-cleanly :appended :not-known :act-id (act-id 1) :digest "aa"}]
   ["a word that the table does not hold is not made: one of the four answers, a gate's reason word, nothing"
    (fn [] [(words/word :refused {:at :lane}) (words/word :lane-not-made nil) (words/word nil nil)])
    [nil nil nil]]
   ["the answer from a read of an accept with the digest given: accepted, with its stamp and the time on the act"
    (fn [] (words/answer read-accepted "aa"))
    {:answer :accepted :stamp 77 :time now}]
   ["the answer from a read of a refusal with the digest given: refused, with the gate's reason word, its stamp and the time on the thing"
    (fn [] (words/answer read-refused "aa"))
    {:answer :refused :reason :lane-not-made :stamp 78 :time now}]
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
  {:version 1 :who (person :alice) :lane :alice :permission (perm/permission-id alice-own) :session :alice-s1})

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
  for the thing that the door would append: the door reads the lane and no
  other part, so each fault of a fact, of a value or of a limit is a gate's
  to name."
  [["no act: no map: nil, a number, a string, a vector"
    (fn [] (mapv through [nil 7 "x" [1 2]]))
    (vec (repeat 4 (could-not :read-the-act)))]
   ["no act: a map with no :lane, or a nil one"
    (fn [] (mapv through [{:facts []} (assoc (maker value-fact) :lane nil)]))
    (vec (repeat 2 (could-not :make-an-act-id :lane)))]
   ["no act: a thing whose :lane cannot be read without a throw: a sorted map with string keys"
    (fn [] (through (sorted-map "lane" :alice "facts" [])))
    (could-not :read-the-act :lane)]
   ["no act: a record with a :lane"
    (fn [] (through (assoc (->Thing :note-1 :text) :lane :alice :facts [])))
    (could-not :read-the-act)]
   ["no act: a :lane that is no id: a string, a number, a keyword that does not read back"
    (fn [] (mapv (fn [lane] (through (assoc (maker value-fact) :lane lane))) ["notes" 7 (keyword "a b")]))
    (vec (repeat 3 (could-not :make-an-act-id :lane)))]
   ["no act: a :lane that prints to 257 characters; one of 256 is read"
    (fn [] [(read-word (assoc (maker) :lane (long-id 257)) nil) (read-word (assoc (maker) :lane (long-id 256)) nil)])
    [(could-not :make-an-act-id :lane) :read]]
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
   ["no act: an id of 300 characters that is not the lane"
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
        permission-fact (perm/permission-fact bobs)
        retract {:e :note-2 :k :text}
        mixed (fn [facts] (assoc makers-act :facts facts))
        three [value-fact retract permission-fact]
        doors-parts [:act-id :depot declaration/time-part]
        hands-thing (fn [facts] (assoc makers-act :act-id (act-id 70) :depot :streaming declaration/time-part (+ now 60000)
                                       :facts facts))]
    [;; a reading
     ["a reading holds two parts: the maker's own map, and its lane"
      (fn [] (let [act (maker value-fact)
                   reading (additions/reading act nil)]
               [(sort (keys reading)) (identical? act (:thing reading)) (:lane reading)]))
      [[:lane :thing] true :alice]]

     ;; an act-id given back
     ["an act-id given back, of the act's lane: used as given"
      (fn [] (select-keys (:thing (built (maker value-fact) (given :streaming))) [:act-id :depot]))
      {:act-id (given :streaming) :depot :streaming}]
     ["an act-id given back, of another lane"
      (fn [] (read-word (maker value-fact) [:bob :streaming (uuid 9)]))
      (could-not :make-an-act-id :act-id)]
     ["an act-id given back that is not of the shape: two slots, a string, a depot slot that is no depot word, a word of the door's"
      (fn [] (mapv (fn [id] (read-word (maker value-fact) id))
                   [[:alice :streaming] "an act-id" [:alice :nowhere (uuid 9)] (could-not :make-an-act-id :lane)]))
      (vec (repeat 4 (could-not :make-an-act-id :act-id)))]
     ["an act-id given back with the other depot word: used as given, and the thing names that depot"
      (fn [] [(select-keys (:thing (built (maker value-fact) (given :microbatch))) [:act-id :depot])
              (declaration/stray-reason :microbatch (:thing (built (maker value-fact) (given :microbatch))))])
      [{:act-id (given :microbatch) :depot :microbatch} nil]]

     ;; the kinds of fact
     ["a fact under each of the five store keys is the maker's own object in the thing"
      (fn [] (let [facts [{:e :alice :k :softland.store/depot :v :streaming}
                          permission-fact
                          {:e :alice :k :softland.store/who-may-read-acts :v :everyone}
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
      (fn [] (through (assoc (->Thing :note-1 :text) :lane :alice :act-id (act-id 1))))
      (could-not :read-the-act)]
     ["two faults, it carries :act-id and has no :lane: the door's own parts come before the lane"
      (fn [] (through {:act-id (act-id 1) :facts [value-fact]}))
      (could-not :add-its-parts :act-id)]
     ["two faults, it carries the part of the time and has no :lane: the same order"
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
     ["two faults, a :lane that is no id and a value that is no value: the door reads no fact, so the lane gives the word"
      (fn [] (through (assoc (maker (assoc value-fact :v 1/3)) :lane "notes")))
      (could-not :make-an-act-id :lane)]

     ;; the thing that is built
     ["the thing holds :facts as the very object that the maker gave, for a vector, a list and a lazy sequence; the building does not walk the lazy sequence"
      (fn [] (let [walked (atom 0)
                   lazy (map (fn [fact] (swap! walked inc) fact) three)
                   same? (fn [facts] (identical? facts (:facts (:thing (built (mixed facts))))))]
               [(same? three) (same? (apply list three)) (same? lazy) @walked (realized? lazy)]))
      [true true true 0 false]]
     ["the thing is the maker's map with three parts more: each part that the maker gave is the maker's own object, a part that the form does not know too, and nothing is filled in"
      (fn [] (let [act (assoc (mixed three) :because-of (act-id 6) :based-on {[(act-id 5) 0] 7} :note {:a 1})
                   thing (:thing (built act))]
               [(every? (fn [part] (identical? (get act part) (get thing part)))
                        [:version :who :lane :permission :session :because-of :based-on :note :facts])
                (sort (remove (set (keys act)) (keys thing)))
                (select-keys thing doors-parts)
                (sort (remove (set (keys (:thing (built (maker value-fact))))) [:based-on :because-of]))]))
      [true (sort doors-parts) {:act-id (act-id 70) :depot :streaming declaration/time-part now}
       [:based-on :because-of]]]
     ["the thing to append is the maker's map with the three parts added and nothing else changed: with the three taken off again it is equal to the maker's map; the depot is the depot slot of the act-id; the time is the number that the door hands, under the name that the declaration holds. No clock is read: two calls with one time give one thing, and a call with another time gives a thing that differs in that part alone"
      (fn [] (let [act (assoc (mixed three) :because-of (act-id 6) :note {:a 1})
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
              (declaration/stray-reason :streaming (assoc (->Thing :note-1 :text) :lane :alice :act-id (given :streaming)))
              (read-word (assoc (->Thing :note-1 :text) :lane :alice) (given :streaming))])
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
      (fn [] [(said (additions/thing-to-append {:thing (sorted-map "lane" :alice) :lane :alice} (act-id 70) now))
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
      (fn [] (mapv (fn [x] (:could-not (additions/reading x x))) [(throwing) (Object.) 'x {:lane (throwing)}]))
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

;; ----------------------------------------------------------------- the line

(defrecord Holds [part])

(def ^:private plain-things
  "Things that the one text writes out whole: an act, an act with its
  optional parts, a thing that is no act, one with a run of bytes and a time,
  one with no time, and values of each kind."
  [good
   (assoc good :based-on {[(act-id 9) 0] 3} :because-of (act-id 8))
   {:act-id (act-id 2) :lane :alice}
   {:act-id (act-id 3) :lane :alice :bytes (byte-array [1 2 3]) declaration/time-part 7}
   (without-time good)
   {:x #{:a "b" 3} :y {:k [1 2.5 -0.0 nil true]} declaration/time-part 12}
   "a string" [1 2 3] 42])

(defn- weight-of
  "The weight that the one walk gives a thing, with a bound far above it."
  [x]
  (:weight (digest/weighed x 100000000)))

(defn- held
  "What the weight of `{:p part}` is beyond the bytes of its one text: what
  the part holds, for a part that the text does not write out whole."
  [part]
  (let [x {:p part}]
    (- (weight-of x) (text/utf8-count (text/text x)))))

(defn- at-the-limits
  "An act at the declaration's limits: 256 facts, each a value at the bound
  of one value's text, and 1,024 entries of based-on."
  []
  (let [value (text-of-bytes 65000)]
    (assoc (apply arrived 77 (map (fn [i] (value-at (keyword (str "e" i)) :text value)) (range 256)))
           :based-on (into {} (map (fn [i] [[(act-id i) 0] i])) (range 1024)))))

(def ^:private weight-cases
  "The weight of a thing, counted in the walk of its digest
  (`softland.store.door.digest/weighed`), and what a part that the text
  does not write out holds (`softland.store.door.weight`)."
  [["the weight of a thing that the one text writes out whole is the number of bytes of the UTF-8 of its one text, the time on it too and a run of bytes as its hex; and the digest of the one walk is the digest"
    (fn [] (mapv (fn [x] (let [{d :digest w :weight} (digest/weighed x 100000000)]
                           [(= (vec d) (vec (digest/digest x))) (= w (text/utf8-count (text/text x)))]))
                 plain-things))
    (vec (repeat (count plain-things) [true true]))]
   ["the time on a thing weighs the bytes of its own text, as the thing's one text writes it, whatever the time holds: a string, a vector, a run of bytes, a map that holds a set"
    (fn [] (mapv (fn [t] (let [x {:p 1 declaration/time-part t}] (= (weight-of x) (text/utf8-count (text/text x)))))
                 ["noon" [1 2 3] (byte-array 10) {:a [1 #{2 3}]}]))
    [true true true true]]
   ["a time that is a run of 200,000 bytes, or 30,000 longs of nineteen digits, weighs its text, more than 256 KiB: a thing that holds one is too heavy at that bound, where what the time holds would be under it"
    (fn [] [(:weight (digest/weighed {:p 1 declaration/time-part (byte-array 200000)} 262144))
            (:weight (digest/weighed {:p 1 declaration/time-part (vec (repeat 30000 1000000000000000000))} 262144))])
    [:too-heavy :too-heavy]]
   ["a part that the text writes as #other weighs, beside its text, what it holds: a record of one string, a Java list of two longs, a symbol with a namespace, a ratio, a float, a character, an array of 100 ints, an array of three objects"
    (fn [] (mapv held [(->Holds "abc") (java.util.ArrayList. [1 2]) 'a.b/cd 1/3 (float 1.5) \x
                       (int-array 100) (object-array [1 "ab" nil])]))
    [43 17 7 2 4 2 401 14]]
   ["a decimal weighs, beside its text, its unscaled digits: 10^2000 at the scale 2000, whose text is 1M, holds 835 bytes, read from the length of its bits"
    (fn [] (let [d (BigDecimal. (.pow BigInteger/TEN 2000) 2000)]
             [(text/text d) (held d)]))
    ["1M" 835]]
   ["a part nested deeper than the text writes is weighed whole, with a stack of its own: a vector 100,000 deep weighs its 99,936 vectors past the text's 64, and its long"
    (fn [] (held (nested 100000)))
    99944]
   ["a part of a class that the weight does not know, and a thing that throws when it is read, are not weighed; the second has the digest of #unreadable, and neither throws"
    (fn [] [(weight-of {:p (Object.)}) (weight-of {:facts (throwing)})
            (= (vec (:digest (digest/weighed {:facts (throwing)} 100))) (vec (digest/digest {:facts (throwing)})))])
    [:not-weighed :not-weighed true]]
   ["the weighing stops at the bound and one: a record that holds a sequence with no end is too heavy, and the walk ends; so is a string over the bound, with no walk of its characters"
    (fn [] [(:weight (digest/weighed {:p (->Holds (range))} 262144))
            (:weight (digest/weighed {:p (apply str (repeat 300000 "s"))} 262144))])
    [:too-heavy :too-heavy]]
   ["an array of a primitive is weighed from its length: 100,000 longs are too heavy at 256 KiB and weigh 800,001 bytes beside their text under a bound above that"
    (fn [] [(:weight (digest/weighed {:p (long-array 100000)} 262144)) (held (long-array 100000))])
    [:too-heavy 800001]]
   ["a thing that is too heavy keeps no weight: at a bound one below its weight it is too heavy, at its weight it weighs that"
    (fn [] (let [w (weight-of good)]
             [(:weight (digest/weighed good (dec w))) (= w (:weight (digest/weighed good w)))]))
    [:too-heavy true]]
   ["an act at the declaration's limits, 256 values at the bound of one value and 1,024 entries of based-on, passes the declaration and weighs less than the bound on what is kept with standing"
    (fn [] (let [x (at-the-limits)]
             [(verdict (declaration/check x)) (< (long (weight-of x)) decision/max-kept-weight)]))
    [:ok true]]])

(def ^:private kept-bound-cases
  "The bounds on what a gate keeps of a thing (Sid's call of 22 November
  2026): 32 MiB from a sender with standing, 256 KiB from one with none or
  for a thing that the declaration refused."
  [["the two bounds, in bytes of weight: 32 MiB and 256 KiB"
    (fn [] [decision/max-kept-weight decision/max-kept-weight-with-no-standing])
    [33554432 262144]]
   ["the walk weighs up to 256 KiB a thing that the declaration refused, and up to 32 MiB one that it took"
    (fn [] [(decision/weighing-bound :malformed) (decision/weighing-bound :value-too-large) (decision/weighing-bound nil)])
    [262144 262144 33554432]]
   ["a sender has standing when the declaration took the thing and `standing` gave nil, except that a permission held through everyone gets 32 MiB only when taken: an accept, a places refusal of a named party, and a named holder who may only see are kept up to 32 MiB; a places refusal under everyone, any refusal of standing, and any refusal of the declaration, up to 256 KiB"
    (fn [] (mapv (fn [[decl standing refused through-everyone?]]
                   (decision/kept-bound decl standing refused through-everyone?))
                 [[nil nil nil false]
                  [nil nil nil true]
                  [nil nil :replaced-fact-not-current false]
                  [nil nil :permission-does-not-allow-writing false]
                  [nil nil :permission-does-not-allow-writing true]
                  [nil :permission-never-given :permission-never-given false]
                  [nil :permission-ended :permission-ended false]
                  [nil :permission-above-ended :permission-above-ended false]
                  [nil :permission-used-up :permission-used-up false]
                  [nil :who-not-the-permissions-holder :who-not-the-permissions-holder false]
                  [nil :permission-for-another-sitting :permission-for-another-sitting false]
                  [nil :act-not-the-one-permitted :act-not-the-one-permitted false]
                  [nil :no-permission :no-permission false]
                  [nil :lane-not-made :lane-not-made false]
                  [nil :lane-belongs-to-the-other-gate :lane-belongs-to-the-other-gate false]
                  [:malformed :malformed :malformed false]
                  [:no-door-time :no-door-time :no-door-time false]]))
    [33554432 33554432 33554432 33554432
     262144 262144 262144 262144 262144 262144 262144 262144 262144 262144 262144 262144 262144]]])

(def ^:private line-key-cases
  "The keys of the line and of the rows by who."
  [["the keys of the line put the stamps 9, 10 and 2^63 - 1 in their numeric order, in the order in which a table keeps its keys; a stamp is 19 digits"
    (fn [] [(record/line-key :a 9)
            (record/before? (record/line-key :a 9) (record/line-key :a 10))
            (record/before? (record/line-key :a 10) (record/line-key :a 9223372036854775807))])
    [":a 0000000000000000009" true true]]
   ["a lane's run of its line, from a stamp on, holds its keys from that stamp and no key of a lane whose text begins with its text"
    (fn [] (let [[from to] (record/line-run :a 5)
                 in? (fn [k] (and (not (record/before? k from)) (record/before? k to)))]
             [from to (in? (record/line-key :a 5)) (in? (record/line-key :a 9223372036854775807))
              (in? (record/line-key :a 4)) (in? (record/line-key :ab 5))]))
    [":a 0000000000000000005" ":a!" true true false false]]
   ["one who's rows by who in one lane are one run, from a stamp on, which holds no row of a who whose text begins with hers or with which hers begins"
    (fn [] (let [[from to] (record/by-who-run :a (person :asha) 0)
                 in? (fn [k] (and (not (record/before? k from)) (record/before? k to)))]
             [(record/by-who-key :a (person :asha) 3) (in? (record/by-who-key :a (person :asha) 3))
              (in? (record/by-who-key :a (person :ashab) 3)) (in? (record/by-who-key :a (person :ash) 3))
              (in? (record/by-who-key :ab (person :asha) 3))]))
    [":a :person.asha 0000000000000000003" true false false false]]
   ["the stamp of a key of the line and of a row by who reads back; a text that is no such key gives none, and nothing throws"
    (fn [] [(record/stamp-of-line-key (record/line-key :a 42))
            (record/stamp-of-by-who-key (record/by-who-key (uuid 3) (person :asha) 7))
            (record/stamp-of-line-key ":a 12") (record/stamp-of-line-key "nonsense") (record/stamp-of-line-key nil)])
    [42 7 nil nil nil]]])

(def ^:private line-writes-cases
  "The rows that one decision writes in the lane's line
  (`softland.store.gate.decision/line-writes`), against rows written out."
  (let [thing {:act-id (act-id 5) :lane :alice :who (person :bob) :junk 1}
        d (byte-array (range 32))
        hex (text/hex d)
        k ":alice 0000000000000000005"]
    [["a refusal of the declaration of a thing that names a usable who: its line row with the uuid, the word, the time, the digest as hex, the who and the weight; the thing under the same key; and its row by who"
      (fn [] (decision/line-writes (act-id 5) thing :malformed 5 now d 40 262144))
      [[:line k {:act (uuid 5) :refused :malformed :time now :digest hex :who ":person.bob" :weight 40}]
       [:thing k thing]
       [:by-who ":alice :person.bob 0000000000000000005" 5]]]
     ["a thing heavier than its bound writes no thing, and its row says too heavy and keeps no weight; one that is not weighed says so; one that names no usable who writes no row by who"
      (fn [] [(decision/line-writes (act-id 5) thing :malformed 5 now d 300000 262144)
              (decision/line-writes (act-id 5) thing :malformed 5 now d :too-heavy 262144)
              (decision/line-writes (act-id 5) (assoc thing :who "bob") :malformed 5 now d :not-weighed 262144)])
      [[[:line k {:act (uuid 5) :refused :malformed :time now :digest hex :who ":person.bob" :not-kept :too-heavy}]
        [:by-who ":alice :person.bob 0000000000000000005" 5]]
       [[:line k {:act (uuid 5) :refused :malformed :time now :digest hex :who ":person.bob" :not-kept :too-heavy}]
        [:by-who ":alice :person.bob 0000000000000000005" 5]]
       [[:line k {:act (uuid 5) :refused :malformed :time now :digest hex :not-kept :not-weighed}]]]]
     ["an accept, and a thing with no usable time: the line row holds no word, and no time"
      (fn [] [(first (decision/line-writes (act-id 5) thing nil 5 now d 40 33554432))
              (first (decision/line-writes (act-id 5) thing :no-door-time 5 nil d 40 262144))])
      [[:line k {:act (uuid 5) :time now :digest hex :who ":person.bob" :weight 40}]
       [:line k {:act (uuid 5) :refused :no-door-time :digest hex :who ":person.bob" :weight 40}]]]
     ["the answer that a line row gives is the answer that its answer's row gives: the same verdict, reason, stamp, time and digest"
      (fn [] (let [[[_ _ row]] (decision/line-writes (act-id 5) thing :malformed 5 now d 40 262144)
                   [[_ _ accepted]] (decision/line-writes (act-id 5) thing nil 6 nil d 40 262144)]
               [(= (record/line-answer 5 row) (record/answer-of (record/answer-row :malformed 5 now d)))
                (= (record/line-answer 6 accepted) (record/answer-of (record/answer-row nil 6 nil d)))]))
      [true true]]]))

(def ^:private alice-making
  "The making act of Alice's lane, as it arrives on the streaming depot."
  (as-sent (making :alice (person :alice) (person :alice)) :streaming))

(defn- with-facts
  "Alice's act under the act-id `(act-id n)`, by `who` under `permission`,
  with these facts."
  [n who permission & facts]
  (assoc (apply arrived n facts) :who who :permission (perm/permission-id permission)))

(def ^:private bob-sees "Bob's permission in Alice's lane: see, and not act."
  (perm/given-permission (person :bob) :alice alice-own :see))

(def ^:private a-line
  "A line of Alice's lane, each kind of thing that the line is for: its
  making act; a taken act; a thing that the declaration refused; a stranger's
  act refused for its standing; a stray; the taken act again under its
  act-id; another thing under that act-id; heavy things of 300 KB: a
  stranger's, a holder's refused at the places, one that the declaration
  refused, and one of a holder who may only see; and a thing whose part the
  weight does not know."
  (let [heavy (fn [] (map (fn [i] (value-at (keyword (str "h" i)) :text (text-of-bytes 60000))) (range 5)))
        taken (arrived 2 (value-at :note-2 :text))
        zeds (perm/given-permission (person :zed) :alice alice-own)]
    [alice-making
     taken
     {:act-id (act-id 3) :lane :alice :who (person :carol) :junk 1 declaration/time-part now}
     (with-facts 4 (person :zed) zeds (value-at :n4 :text))
     5
     taken
     (arrived 2 (value-at :other :text))
     (apply with-facts 6 (person :zed) zeds (heavy))
     (apply arrived 7 (assoc (first (heavy)) :replaces [(act-id 99) 0]) (rest (heavy)))
     (arrived 8 (value-at :big :text (text-of-bytes 300000)))
     (arrived 9 (perm/permission-fact bob-sees))
     (apply with-facts 10 (person :bob) bob-sees (heavy))
     {:act-id (act-id 11) :lane :alice :who (person :dee) declaration/time-part now :p (Object.)}]))

(def ^:private a-line-walked
  "What `a-line` leaves, walked in memory from nothing."
  (delay (walked-line empty-task :streaming a-line)))

(defn- line-row-at
  "The line row at a stamp of Alice's lane in a record."
  [record stamp]
  (get-in record [:line (record/line-key :alice stamp)]))

(def ^:private line-walk-cases
  "A line of things decided in memory, one after the other, by the walk's
  steps (`walked`), and what its line holds."
  [["each decision of the line has one line row at its stamp, from 0 to the lane's last stamp, and nothing else is in the line: a stray, the act again under its act-id and another thing under it add none"
    (fn [] (let [after @a-line-walked]
             [(get-in after [:last-stamp ":alice"])
              (mapv record/stamp-of-line-key (sort (keys (:line after))))]))
    [9 (vec (range 10))]]
   ["the line row's answer is the answer of the decision, by its act-id: the word of each, by stamp"
    (fn [] (let [after @a-line-walked]
             [(mapv (fn [s] (:refused (line-row-at after s))) (range 10))
              (every? (fn [s] (let [row (line-row-at after s)
                                    answer (get-in after [:answers (record/answer-key :alice (:act row))])]
                                (= (record/line-answer s row) (record/answer-of answer))))
                      (range 10))]))
    [[nil nil :unknown-version :permission-never-given :permission-never-given :replaced-fact-not-current
      :value-too-large nil :permission-does-not-allow-writing :unknown-version] true]]
   ["the thing kept under a stamp is the thing that was decided there: the taken act, not the other thing under its act-id; the stranger's small refused thing and the declaration's"
    (fn [] (let [after @a-line-walked
                 thing-at (fn [s] (get-in after [:things (record/line-key :alice s)]))]
             [(= (thing-at 1) (nth a-line 1)) (= (thing-at 2) (nth a-line 2)) (= (thing-at 3) (nth a-line 3))]))
    [true true true]]
   ["of the things of 300 KB: a stranger's refused for its standing, and one that the declaration refused, are not kept and keep no weight; a holder's refused at the places, and one of a holder who may only see, are kept with their weight"
    (fn [] (let [after @a-line-walked
                 at (fn [s] (let [row (line-row-at after s)]
                              [(:not-kept row) (some? (:weight row))
                               (contains? (:things after) (record/line-key :alice s))]))]
             (mapv at [4 6 5 8])))
    [[:too-heavy false false] [:too-heavy false false] [nil true true] [nil true true]]]
   ["a thing whose part the weight does not know is not kept, and says so"
    (fn [] (let [row (line-row-at @a-line-walked 9)] [(:not-kept row) (:weight row) (:who row)]))
    [:not-weighed nil ":person.dee"]]
   ["each decision whose thing names a usable who has its row by who, the stranger's refused things among them; the who of each line row"
    (fn [] (let [after @a-line-walked]
             [(into (sorted-map) (:by-who after))
              (mapv (fn [s] (:who (line-row-at after s))) (range 10))]))
    [(into (sorted-map)
           {":alice :person.alice 0000000000000000000" 0 ":alice :person.alice 0000000000000000001" 1
            ":alice :person.carol 0000000000000000002" 2 ":alice :person.zed 0000000000000000003" 3
            ":alice :person.zed 0000000000000000004" 4 ":alice :person.alice 0000000000000000005" 5
            ":alice :person.alice 0000000000000000006" 6 ":alice :person.alice 0000000000000000007" 7
            ":alice :person.bob 0000000000000000008" 8 ":alice :person.dee 0000000000000000009" 9})
     [":person.alice" ":person.alice" ":person.carol" ":person.zed" ":person.zed"
      ":person.alice" ":person.alice" ":person.alice" ":person.bob" ":person.dee"]]]
   ["the writes of a decision put the line first and the answer and the last stamp last; a decision run again on the record that it left writes nothing, of the line neither"
    (fn [] (let [one (walked empty-task :streaming alice-making)
                 again (walked one :streaming alice-making)]
             [(mapv first (into (:wrote-line one) (:wrote one))) (:wrote again) (:wrote-line again)]))
    [[:line :thing :by-who :fact :fact :holder-permission :owner :answer :last-stamp] [] []]]])

(def ^:private reads-acts-key :softland.store/who-may-read-acts)

(def ^:private carols-owner
  (perm/given-permission (person :carol) :alice alice-own {:allows :owner :lent-from nil}))

(def ^:private acts-setting-line
  "A line of Alice's lane that writes who may read its acts, each way that
  a gate takes or refuses: the making act; a give to Bob and a co-owner give
  to Carol; Alice and Carol write everyone; Bob writes it; Alice writes it on
  another entity, with no value, with a word that is neither, as a string;
  Alice marks it; Alice writes members again."
  [alice-making
   (arrived 2 (perm/permission-fact bobs) (perm/permission-fact carols-owner))
   (arrived 3 {:e :alice :k reads-acts-key :v :everyone})
   (with-facts 11 (person :carol) carols-owner {:e :alice :k reads-acts-key :v :everyone})
   (with-facts 4 (person :bob) bobs {:e :alice :k reads-acts-key :v :everyone})
   (arrived 5 {:e :chair-7 :k reads-acts-key :v :everyone})
   (arrived 6 {:e :alice :k reads-acts-key})
   (arrived 7 {:e :alice :k reads-acts-key :v :strangers})
   (arrived 8 {:e :alice :k reads-acts-key :v "everyone"})
   (arrived 9 {:e :alice :k :softland.store/marked-keys :v #{reads-acts-key}})
   (arrived 10 {:e :alice :k reads-acts-key :v :members})])

(def ^:private acts-setting-cases
  "Who may read a lane's acts whole, as a rule of a gate: the fifth store
  key."
  [["with no fact, who may read a lane's acts reads as members, for a lane and for the base"
    (fn [] [(first-act/starts-with :alice reads-acts-key) (first-act/starts-with :base reads-acts-key)])
    [:members :members]]
   ["the key is a store key, a setting that takes a second write, of the shape of its two words"
    (fn [] [(declaration/store-key? reads-acts-key) (contains? declaration/setting-keys reads-acts-key)
            (contains? declaration/second-write-settings reads-acts-key)
            (get-in declaration/store-keys [reads-acts-key :value])])
    [true true true :who-may-read-acts-word]]
   ["any owner writes everyone and members, while a member who is no owner is refused as for every setting; on another entity, with no value, with a word that is neither or a string, and as a marked key, each is refused with the word of the other settings or of its own"
    (fn [] (words-by-number (walked-line empty-task :streaming acts-setting-line)))
    (into (sorted-map) {1 :accepted 2 :accepted 3 :accepted 4 :only-an-owner-writes-a-setting
                        5 :setting-not-on-the-lane 6 :store-key-retract-not-built
                        7 :who-may-read-acts-fact-not-members-or-everyone
                        8 :who-may-read-acts-fact-not-members-or-everyone
                        9 :marked-key-in-the-stores-namespace 10 :accepted 11 :accepted})]
   ["the row that the first holder writes is the setting's, at the lane's own entity, and holds the last word written"
    (fn [] (let [after (walked-line empty-task :streaming acts-setting-line)]
             (:value (get-in after [:facts (record/place-key :alice :alice reads-acts-key)]))))
    ":members"]
   ["a making act that carries the key as a third fact is no making act: refused, the lane not made"
    (fn [] (words-by-number (walked-line empty-task :streaming
                                         [(update alice-making :facts conj {:e :alice :k reads-acts-key :v :everyone})])))
    (sorted-map 1 :lane-not-made)]
   ["a thing refused store-key-not-known for the key before it was known now has the word of its other fault, when a later rule names one: no place of the new rule keeps the old word (the plan's check, item d); a thing that an earlier rule refuses keeps its word"
    (fn [] [(verdict (declaration/check (arrived 1 {:e :alice :k reads-acts-key :v :everyone}
                                                  {:e :other :k :softland.store/builds-on :v :invalid})))
            (verdict (declaration/check (assoc (arrived 1 {:e :alice :k reads-acts-key :v :everyone}) :depot :microbatch)))])
    [:builds-on-fact-not-a-list-of-lanes :depot-not-the-act-ids-depot]]])

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
         "the settings, which a gate takes only on the lane's own entity:" (pr-str (sort declaration/setting-keys))
         "the settings that take a second write:" (pr-str (sort declaration/second-write-settings))
         "the limits:" (pr-str {:facts declaration/max-facts :based-on declaration/max-based-on
                                :value-bytes declaration/max-value-bytes :act-bytes declaration/max-act-bytes
                                :id-text declaration/max-id-text :builds-on declaration/max-builds-on
                                :marked-keys declaration/max-marked-keys}))
  (shown "the three settings that take a second write, each with a right value and a wrong one:"
         (pr-str (mapv (fn [[k v]] [k v (verdict (declaration/check (arrived 1 {:e :alice :k k :v v})))])
                       [[:softland.store/who-may-read-acts :everyone] [:softland.store/who-may-read-acts :owner]
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
        step-word (:refuse (#'declaration/with-texts (step-act-at-the-bound 1)))
        earned (conj (into #{} (map #(nth % 2)) (concat rows times limit-rows [[nil nil :id-too-long]]))
                     step-word)]
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
                                 ["no session" #(dissoc % :session)]
                                 ["no fact" #(assoc % :facts [])]]))
           "| the test for a stray reads no time: the least thing with no time is"
           (pr-str (or (declaration/stray-reason :streaming (without-time good)) "no stray")))))

(deftest the-bounds
  (let [n (holds check-bound-cases)]
    (shown "the two bounds of the check's last step," n "cases. One value: a text of" declaration/max-value-bytes
           "bytes at most, counted in UTF-8, under a plain key; the proof's own count of the two values at the edge is"
           (pr-str [(alength ^bytes (utf8 (pr-str at-the-bound))) (alength ^bytes (utf8 (pr-str over-the-bound)))])
           "bytes, in" (pr-str [(count (pr-str at-the-bound)) (count (pr-str over-the-bound))]) "characters, and the check gives"
           (pr-str (mapv (fn [v] (verdict (declaration/check (arrived 1 (value-at :note-1 :text v))))) [at-the-bound over-the-bound])))
    (shown "one act:" declaration/max-act-bytes "bytes of text at most in the rows of its facts, the text of each value and, for each fact, of who and of the permission id; 256 full values count"
           (bytes-in-rows (full-act 0)) "bytes, and at the last step's own edge"
           (pr-str [(bytes-in-rows (step-act-at-the-bound 0)) (bytes-in-rows (step-act-at-the-bound 1))]) "gives"
           (pr-str (mapv (fn [more] (verdict (#'declaration/with-texts (step-act-at-the-bound more)))) [0 1]))
           "| a value over its bound in an act over its bound:"
           (pr-str (verdict (declaration/check (assoc-in (full-act 0) [:facts 0 :v] (text-of-bytes 2100000))))))))

(deftest nothing-throws
  (holds (verdicts not-acts))
  (holds read-predicate-cases)
  (shown "nothing throws. The check refuses" (count not-acts) "things that are no acts:" (words not-acts)))

(deftest the-stamp
  (let [n (holds stamp-cases)]
    (shown "the stamp," n "cases: the lane's count of decisions, 0 for the first and the last stamp plus one after it, a long; it takes the last stamp alone, so no time and nothing of the act is in it")
    (shown "the stamp after each last stamp, as [last stamp, stamp]:"
           (pr-str (mapv (juxt identity stamp/next-stamp) [nil 0 1 41 (dec Long/MAX_VALUE) Long/MAX_VALUE])))))

(deftest the-first-act
  (holds first-act-cases)
  (shown "the form of a making act:"
         (str/join " | " (map (fn [[what gate-depot _ expected]] (str what " [" (name gate-depot) " gate] " expected)) first-act-rows)))
  (shown "a lane that starts with the other gate's depot, as [gate's depot, lane, the answer]:"
         (pr-str (vec (for [gate-depot [:streaming :microbatch]
                            lane [first-act/base :alice :team (uuid 1)]]
                        [gate-depot lane (first-act/starts-with-the-other-gates-depot? gate-depot lane)])))))

(deftest the-records-rows
  (let [n (holds record-cases)
        value-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} (uuid 5) 2 900 now)]
    (shown "the record's rows," n "cases: the keys" (pr-str [(record/answer-key :alice (uuid 5)) (record/place-key :alice :note-1 :text)])
           "; each field of a row of its declared class; a retract is a row with no value; every value is kept plain and as a String, the one text of the value in its normal form, which the row takes from the fact as it is; each row of a fact holds who wrote it and the permission that the act named; the heads of the two reads give nothing for what is no id or no act-id")
    (shown "the routing id of an act-id is its lane, for an act-id of each depot:"
           (pr-str (mapv (juxt identity record/routing-id) [[:alice :streaming (uuid 5)] [:base :microbatch (uuid 5)]]))
           "| the head of the read by act-id with that routing id, and with the act-id's uuid in its place:"
           (pr-str (mapv (fn [given] (record/answer-read-by-routing-id given [:base :microbatch (uuid 5)])) [:base (uuid 5)]))
           "| an act-id's lane and uuid come back from the text of its answer's key, for a lane that is a keyword and one that is a UUID:"
           (pr-str (mapv (fn [lane] (let [[lane-text act-uuid] (record/answer-key-parts (record/answer-key lane (uuid 5)))]
                                       [(record/id-of-text lane-text) act-uuid]))
                         [:alice (uuid 3)])))
    (shown "the row of a fact whose text is \"a text\" in its quotes:" (pr-str value-row)
           "| its plain bytes, read as UTF-8:" (pr-str (String. ^bytes (record/plain-bytes-of value-row) StandardCharsets/UTF_8)))
    (shown "what a row says of its value, for a reader:"
           (pr-str (mapv record/read-value [nil {:stamp 1} {:value ":canvas-1" :stamp 1} {:value "(1 2)" :stamp 1}
                                            {:value "\u0000\"a\"" :stamp 1}])))
    (shown "the facts by value: a key is" (pr-str (record/by-value-key :alice :on 100 (record/value-digest-of :c1) :b1))
           "| the run of the value :c1 under the key :on since the stamp 100 is" (pr-str (record/by-value-run :alice :on 100 :c1))
           "| a keyword, a UUID, a string with spaces, a number, a map and a value whose text is 65,536 bytes each have one digest of 64 hex characters, and no run holds a key of another value, lane, key or stamp")))

(deftest the-decision
  (holds early-cases)
  (holds standing-cases)
  (holds chain-cases)
  (holds covers-cases)
  (holds yes-cases)
  (holds review-cases)
  (holds second-review-cases)
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
         (str/join " | " (map (fn [[what _ _ word makes?]] (str what " " (or word (if makes? "makes the lane" "stands")))) standing-rows)))
  (shown "the standing of an act into the base, at the microbatch gate's depot:"
         (str/join " | " (map (fn [[what _ _ word makes?]] (str what " " (or word (if makes? "makes the lane" "stands")))) base-standing-rows)))
  (shown "a lane of the other gate, from the gate's depot and the act alone, with no row of the lane, as [the gate's depot, what arrives, the word or what the act does]:"
         (pr-str (mapv (fn [[what gate thing _ _ _ _ _]]
                         (let [act (act-of thing)]
                           [gate what (or (decision/standing gate act {} nil)
                                          (if (decision/makes-the-lane? gate act {}) :makes-the-lane :stands))]))
                       other-gate-rows)))
  (shown "the checks of the places:"
         (str/join " | " (map (fn [[what _ _ word]] (str what " " (or word "passes"))) places-rows)))
  (shown "the writes of a decision: a refusal writes the answer and the last stamp; an accept of one value writes 3 rows, the fact, the answer and the last stamp, and the fact's row holds the one text of its value, the time on the act, who wrote it and the permission id that the act named:"
         (pr-str (mapv (fn [[which k row]] [which k (if (map? row) (dissoc row :digest) row)])
                       (decision/writes (act-id 1) (act-of good) nil 5 now (digest/digest good) {}))))
  (shown "what a permission allows:" (pr-str (mapv (fn [p] [(perm/permission-id p) (:allows (perm/permission-value p))])
                                                   [alice-own bobs bobs-see bobs-see-and-act]))
         "| standing and places for a holder of see, an id never given, and a member:"
         (let [rmap {alices-place (permission-row alice-own)
                     (chain/place :alice (perm/permission-id bobs-see)) (permission-row bobs-see 1)
                     (chain/place :alice (perm/permission-id bobs-see-and-act)) (permission-row bobs-see-and-act 1)
                     ":alice :alice :softland.store/depot" depot-row}]
           (pr-str (mapv (fn [id]
                           (let [act (assoc (act-of good) :who (person :bob) :permission id)]
                             [(decision/standing :streaming act rmap nil)
                              (when-not (decision/standing :streaming act rmap nil)
                                (decision/places :streaming act rmap))]))
                         [(perm/permission-id bobs-see) (uuid 99) (perm/permission-id bobs-see-and-act)]))))
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
        after-the-acts (walked-line empty-task :streaming (cons (as-sent (making :alice (person :alice) (person :alice)) :streaming) timed-acts))]
    (shown "what a gate may copy to other tasks," copies "cases: of the writes of a decision, each row of a fact and of the facts by value, in their order, a key that is taken out among them, and never the answer or the last stamp; nothing of a refusal. `copies-of`, read as data from the source file of the walk and made a function here with no Rama, gives what the proof's own filter gives. Of the writes of an accept of one value, and of a refusal, as the kinds of the writes and of what is copied:"
           (pr-str (mapv (fn [writes] [(mapv first writes) (mapv first (copies-of writes))]) [(writes-of nil) (writes-of :lane-not-made)])))
    (shown "a second run of one decision," again "cases, by the walk in memory: from the same record it gives the same writes in every field; on the record that the first run left it finds the answer under the act-id, writes nothing and leaves every table as it was. As [what, the kinds of the writes, the word]:"
           (str/join " | " (map (fn [[what _ _ _ kinds word]] (str what " " (pr-str kinds) " " (or word "accepted"))) second-run-rows)))
    (shown "the stamp in a line," stamps "cases, by the walk in memory, for a lane of each gate: a lane's stamps are 0, 1, 2 and on, an accept's and a refusal's alike, whatever the times on its acts and the stamps that they carry. Five acts after the making act of Alice's lane, with times that rise, go back by a minute, leap 2^50 ms ahead and fall to 0, as [time, stamp] from their answers:"
           (pr-str (mapv (fn [thing] ((juxt :time :stamp) (get-in after-the-acts [:answers (record/answer-key :alice (nth (:act-id thing) 2))])))
                         timed-acts)))
    (shown "a mark holds from the next act," marks "cases, for a lane of each gate: the key gets the stamp of the act that marks it; a fact under the key in that act is in no run; a fact under it in the next act has its entry in the facts by value under that stamp")))

(deftest the-line-of-a-lane
  (let [weights (holds weight-cases)
        bounds (holds kept-bound-cases)
        keys-n (holds line-key-cases)
        rows-n (holds line-writes-cases)
        walk-n (holds line-walk-cases)
        setting (holds acts-setting-cases)
        after @a-line-walked]
    (shown "the weight of a thing," weights "cases: the bytes of its one text, counted in the walk of its digest, and what a part that the text does not write out holds, up to a bound. Of the plain things of the cases, as [the weight, the bytes of the text]:"
           (pr-str (mapv (fn [x] [(weight-of x) (text/utf8-count (text/text x))]) plain-things)))
    (shown "the bounds on what a gate keeps," bounds "cases: 32 MiB with standing, 256 KiB with none or for a refusal of the declaration")
    (shown "the keys of the line," keys-n "cases, and its rows," rows-n "cases")
    (shown "a line of" (count a-line) "things walked in memory," walk-n "cases. Its line, by stamp, as [the word, the who, the weight or why no thing is kept]:"
           (pr-str (mapv (fn [s] (let [row (line-row-at after s)] [(:refused row) (:who row) (or (:weight row) (:not-kept row))]))
                         (range (count (:line after))))))
    (shown "who may read a lane's acts whole, the eighth store key," setting "cases")))

(deftest a-line-is-decided-in-the-depots-order
  (let [n (holds line-cases)
        after (walked-line (:record line) :microbatch (:things line))]
    (shown "a line of" (count (:things line)) "things into a base that is made, decided in memory one thing after the other in the order of the depot, with the pure functions in the walk's order;" n
           "cases with a line into a base that is not made, a line of strays, a line of repeats, and the line decided a second time. The word of each answer, by the number of its act-id:"
           (pr-str (words-by-number after)))
    (shown "after that line, the last stamp kept for each lane, the base's" before-the-line "before it:"
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
                                (filter (fn [[what _ _]] (re-find #"^no act: (a map with no :lane|a thing whose :lane cannot|a record with a :lane|an act that already carries :depot|an act that already carries the part of the time|257 facts|a fact that is a record|a fact that carries :sealed|a value that is no value|a value whose text|1,025)" what))
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
  (the-line-of-a-lane)
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
