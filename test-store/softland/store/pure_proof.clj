(ns softland.store.pure-proof
  "The proof of the gates' pure namespaces: the one text and the digest, the
  door's declaration, the stamp, the first act of a layer, the record's
  rows, the checks of the decision, and the pure code of a batch of the
  microbatch gate. And of the door's pure namespaces: its words, what it
  makes of one act, and the numbers of a maker's wait. And of the source
  under `src-store/`: that no code of it names a lock. It needs no cluster
  and no Rama. One command, from the repository root:

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

  The batch. Beside the cases of each function of `softland.store.gate.batch`,
  the proof runs whole batches in memory: the gate's four steps over the
  pure functions, with each task's rows in its own map and each message
  gathered on the task of its route, in many orders of arrival drawn with a
  fixed seed. It compares the record after each run with the record of
  deciding the same things one at a time in the order of their turns, with
  the checks of `softland.store.gate.decision` in their order. One thing in
  that model is not in the order of the turns, as in the gate: each act is
  decided with its layer's marked keys as the record held them before the
  batch. The record that is compared holds the facts by value too. The rows
  that a task is sent are written in the order in which they arrive, which
  is another in each run. It also runs one batch as two attempts, from the
  same record before it: with one clock every row is the same in every
  field; with a clock one second later each stamp is another, and an act at
  the edge of the 10 seconds gets the other verdict. That is what a kept
  clock gives, as a run of the pure code; that the gate keeps the clock is
  the module's proof's.

  The order by value. Its cases decide one act at a time against a record in
  memory with a function of the proof's own, `streaming-step`, which makes
  the reads and the writes of steps 6b and 7 of the streaming gate over the
  pure functions. Each act goes through the declaration's check first,
  which gives each value its text.

  State. The proof owns one atom: the lines that it showed, for the recap.
  It reads the clock for nothing. The orders of arrival come from a random
  source with a fixed seed, so each run draws the same. One check reads
  files and writes none: `no-code-of-the-store-names-a-lock` reads each
  `.clj` file under the directory that holds `softland/store/act.clj` on
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
            [softland.store.act :as act]
            [softland.store.gate.batch :as batch]
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
  Gives the number of cases."
  [cases]
  (doseq [[what _ expected :as c] cases]
    (is (= (comparable expected) (outcome c)) what))
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
  the act-id `(act-id n)`, with these facts and no optional part."
  [n & facts]
  {:act-id (act-id n) :depot :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :facts (vec facts)})

(defn- one-form
  "The one form on arrival of `arrived` for facts that are in their one form."
  [n & facts]
  {:act-id (act-id n) :depot :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {} :because-of nil :claimed-when nil :subjects #{}
   :facts (vec facts)})

(def ^:private good
  "The least thing that the check takes: Alice's act with one fact that has
  a value."
  (arrived 1 (value-at :note-1 :text)))

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
  act built twice; the digest that tells a throw."
  (let [no-bytes {:b [1 2N "x"] :a #{:k}}
        with-bytes {:run (bytes-of 5) :e :note-1}]
    [["a digest is 32 bytes, and its hex text 64 characters"
      (fn [] [(alength ^bytes (digest/digest no-bytes)) (count (digest/hex-of no-bytes))])
      [32 64]]
     ["for a thing with no bytes, the digest is the SHA-256 of the UTF-8 of its one text"
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
     ["the comparison of a sender that still holds its thing: the digest of the thing sent, of a thing with another entity, and no digest"
      (fn [] [(digest/is-digest-of? (digest/hex-of good) good)
              (digest/is-digest-of? (digest/hex-of good) (arrived 1 (value-at :note-2 :text)))
              (digest/is-digest-of? nil good)])
      [true false false]]
     ["the same act built twice has one digest, so a maker's second send of one act reads the answer of the first; the act with another value has another digest"
      (fn [] (let [built (fn [v] (digest/hex-of (arrived 1 (value-at :note-1 :text v))))]
               [(= (built "a text") (built "a text")) (= (built "a text") (digest/hex-of good))
                (= (built "a text") (built "a text."))]))
      [true true false]]
     ["the digest that tells a throw: the hex text that `hex-of` gives, for a thing that is read to its end; nothing for a thing that throws when it is read"
      (fn [] [(digest/readable-hex-of {:a 1}) (= (digest/readable-hex-of good) (digest/hex-of good))
              (digest/readable-hex-of {:facts (throwing)}) (digest/readable-hex-of (throwing))])
      [(sha-256 (utf8 "{:a 1}")) true nil nil]]]))

;; ---------------------------------------------------------- the declaration

(def ^:private declared-shapes
  "The five shapes that the door adds, written out here a second time."
  {:arrived-act
   [:parts
    [:act-id :act-id]
    [:depot :depot-word {:reason :depot-not-the-act-ids-depot}]
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
  eighteen, the two of the last step among them."
  #{:malformed :unknown-part :unknown-version :no-permission :no-session :empty-act
    :permission-not-in-layer
    :depot-not-the-act-ids-depot :too-many-facts :too-many-based-on
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
   ["the five shapes that the door adds: what arrives, whose facts are the maker's own; a depot word; and the values of the three keys that take a second write"
    (fn [] (apply dissoc declaration/shapes (keys act/shapes)))
    declared-shapes]
   ["the seventeen rules, the maker's first, each with its reason; the six of the keys that take a second write stand after the rules of a permission fact"
    (fn [] declaration/rules)
    declared-rules]
   ["the words with which the check refuses, read from the two declarations, and the two of its last step"
    (fn [] declaration/reasons)
    declared-reasons]
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
   ["a good act" good nil]
   ["a thing that is no act, with a usable act-id" {:act-id (act-id 1) :layer :alice :junk 1} nil]
   ["an act-id whose layer prints to 256 characters"
    (assoc good :act-id [(long-id 256) :streaming (uuid 1)] :layer (long-id 256)) nil]])

(def ^:private stray-cases
  "The cases of the test for a stray: one for each row of `stray-rows`, and
  the two that show that the test takes the gate's depot."
  (concat
   (map (fn [[what x word]] [(str "a stray? " what) (fn [] (declaration/stray-reason :streaming x)) word])
        stray-rows)
   [["the test takes the gate's depot: the same microbatch act-id at the microbatch gate is no stray"
     (fn [] (declaration/stray-reason :microbatch (assoc good :act-id [:alice :microbatch (uuid 1)])))
     nil]
    ["and a streaming act-id at the microbatch gate is a stray"
     (fn [] (declaration/stray-reason :microbatch good))
     :act-id-of-the-other-gate]]))

(def ^:private whole
  "A thing with each part, given with subvectors, narrow integers and a list
  of facts: a value that is not in its normal form, a retract, a permission
  given."
  {:act-id (subvec (into [:x] (act-id 9)) 1)
   :depot :streaming
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
  {:act-id (act-id 9) :depot :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {[(act-id 2) 0] 41} :because-of (act-id 2) :claimed-when 1700000
   :subjects #{:alice :bob}
   :facts [{:e :note-1 :k :text :v nil :replaces [(act-id 2) 0] :text "{:a [1 2] :b 1.5M}"}
           {:e :note-2 :k :text :v nil :replaces nil :text nil}
           {:e :bob :k :softland.store/permission :v bobs :replaces nil
            :text "[:bob :alice :alice [:alice :alice :alice]]"}]})

(def ^:private tools-own-keys
  "Keys that are a tool's own, though each looks like one of the store's: the
  bare words that the store's keys were before they took the namespace
  `softland.store`, the bare word depot, the store's words in another
  namespace, and a namespace that only begins with the store's."
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
  [["the least thing in its one form on arrival: each part, and its fact with its five parts, the text of the value in the place of the value"
    (fn [] (act-of good))
    (one-form 1 {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""})]
   ["a thing with each part, given with subvectors, narrow integers and a list of facts: a fact under a plain key holds the text of its value's normal form and no value, a fact with no value holds neither, a fact under a store key holds both"
    (fn [] (act-of whole))
    whole-form]
   ["its one form has plain vectors and longs, and the text is a String"
    (fn [] (let [form (act-of whole)
                 [fact-id carried] (first (:based-on form))]
             (mapv #(.getSimpleName (class %))
                   [(:act-id form) (:permission form) (:facts form) fact-id (second fact-id) carried
                    (:claimed-when form) (second (:replaces (first (:facts form))))
                    (:v (nth (:facts form) 2)) (:text (first (:facts form)))])))
    ["PersistentVector" "PersistentVector" "PersistentVector" "PersistentVector" "Long" "Long"
     "Long" "Long" "PersistentVector" "String"]]
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
   ["the values that the three new keys take: each word of who may see; a list of layers that is empty, names the layer itself, names a layer twice or names a layer that nobody made; a set of keys that is empty"
    (fn [] (mapv (fn [[k v]] (verdict (declaration/check (arrived 1 {:e :alice :k k :v v}))))
                 [[:softland.store/who-may-see :members] [:softland.store/who-may-see :everyone]
                  [:softland.store/builds-on []] [:softland.store/builds-on [:alice]]
                  [:softland.store/builds-on [:base :base]] [:softland.store/builds-on [:nobody-made-this]]
                  [:softland.store/marked-keys #{}]]))
    [:ok :ok :ok :ok :ok :ok :ok]]
   ["a mark of a key in a namespace that only begins with the store's is a mark of a plain key: the store's namespace is taken exactly"
    (fn [] (verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/marked-keys :v #{:softland.store.tool/on :softland/on}}))))
    :ok]
   ["a fact with no value under each of the three new keys passes the declaration, as under the others: a gate refuses the retract"
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

(def ^:private doors-rows
  "One thing for each word that a part or a rule of the door's declaration
  gives: the least thing with one fault. The two words of the last step are
  earned in `limit-rows`, at their edges."
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
    {:act-id [id :streaming (uuid 1)] :depot :streaming
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
  over its bound."
  [["a part that is not named, and a wrong version" (assoc good :owner 1 :version 2) :unknown-part]
   ["no depot, and a wrong version" (assoc (dissoc good :depot) :version 2) :depot-not-the-act-ids-depot]
   ["a wrong version, and facts that are no sequence" (assoc good :version 2 :facts 5) :unknown-version]
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
      {:act-id [layer :streaming (uuid 1)] :depot :streaming :version 1 :who who :layer layer
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
  "The cases of the stamp: the packing, the next stamp, the 10-second rule,
  the greatest carried stamp, the year."
  (let [t 1700000000000
        s (fn [millis counter] (stamp/pack millis counter))]
    [["the packing: milliseconds x 65,536 + counter, and back"
      (fn [] [(stamp/pack 3 5) (stamp/time-of (stamp/pack t 77)) (stamp/counter-of (stamp/pack t 77))])
      [(+ (* 3 65536) 5) t 77]]
     ["the first decision of a task: the clock, with a counter of zero"
      (fn [] (stamp/next-stamp t nil nil))
      (s t 0)]
     ["the clock is ahead of the last stamp: the clock, with a counter of zero"
      (fn [] (stamp/next-stamp (+ t 5) (s t 9) nil))
      (s (+ t 5) 0)]
     ["a second decision in one tick: the last stamp plus one"
      (fn [] (stamp/next-stamp t (s t 0) nil))
      (s t 1)]
     ["the clock went back: the last stamp plus one all the same"
      (fn [] (stamp/next-stamp (- t 60000) (s t 3) nil))
      (s t 4)]
     ["more decisions in one tick than the counter holds: the stamp carries into the time and still increases"
      (fn [] (let [next (stamp/next-stamp t (s t 65535) nil)]
               [(> next (s t 65535)) (stamp/time-of next) (stamp/counter-of next)]))
      [true (inc t) 0]]
     ["an act carries a stamp ahead of the clock and of the last stamp: later than it"
      (fn [] (stamp/next-stamp t (s t 3) (s (+ t 2000) 8)))
      (s (+ t 2000) 9)]
     ["an act carries a stamp behind the clock: the clock"
      (fn [] (stamp/next-stamp t nil (s (- t 2000) 8)))
      (s t 0)]
     ["65,537 decisions in a row at one clock tick: each stamp later than the one before"
      (fn [] (let [stamps (reductions (fn [last _] (stamp/next-stamp t last nil)) (stamp/next-stamp t nil nil) (range 65536))]
               [(count stamps) (apply < stamps)]))
      [65537 true]]
     ["the 10-second rule at its edge: 10,000 ms ahead is not too far, 10,001 is"
      (fn [] [(stamp/too-far-ahead? t (s (+ t 10000) 65535)) (stamp/too-far-ahead? t (s (+ t 10001) 0))
              (stamp/too-far-ahead? t (s (- t 5) 0)) (stamp/too-far-ahead? t (dec act/max-carried-stamp))])
      [false true false true]]
     ["the greatest stamp that an act carries: of its based-on, never claimed-when"
      (fn [] [(stamp/greatest-carried {:based-on {[(act-id 2) 0] 5 [(act-id 2) 1] 9} :claimed-when 99})
              (stamp/greatest-carried {:based-on {} :claimed-when 99})])
      [9 nil]]
     ["the packing reaches 2^62 when the clock reaches 2^46 ms, in the year 4199"
      (fn [] [(stamp/pack (bit-shift-left 1 46) 0)
              (.getYear (.atZone (java.time.Instant/ofEpochMilli (bit-shift-left 1 46)) java.time.ZoneOffset/UTC))])
      [(bit-shift-left 1 62) 4199]]
     ["a stamp of today is below 2^62"
      (fn [] (< (stamp/next-stamp t nil nil) act/max-carried-stamp))
      true]]))

;; -------------------------------------------------------------- the first act

(defn- making
  "A making act in its one form on arrival: for a layer, by `who`, naming a
  first permission held by `holder`, with the depot that the layer starts
  with. `first-act-cases` holds it against what the declaration's check
  gives for the same act as its maker sends it."
  [layer who holder]
  (let [permission [holder layer layer]
        depot (first-act/depot-at-making layer)]
    {:act-id [layer depot (uuid 1)] :depot depot :version 1 :who who :layer layer
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
  "The cases of the first act: the depot that a layer starts with, and one
  case for each row of `first-act-rows`."
  (concat
   [["the depot that a layer starts with: microbatch for the base, streaming for any other"
     (fn [] (mapv first-act/depot-at-making [:base :alice :team (uuid 1)]))
     [:microbatch :streaming :streaming :streaming]]
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
  one form on arrival; the uuid of its act; its index; and the stamp."
  [[who-text permission-text] fact act-uuid index stamp]
  (record/fact-row fact act-uuid index stamp who-text permission-text))

(defn- by-value-row
  "A fact's row in the facts by value, as
  `softland.store.record.rows/by-value-row` gives it. Takes the texts of who
  and of the permission as a pair; the uuid of the act; the fact's index;
  and the stamp."
  [[who-text permission-text] act-uuid index stamp]
  (record/by-value-row who-text permission-text act-uuid index stamp))

(defn- digest-of-text
  "The SHA-256 of the UTF-8 of a string, as hex: what the order by value must
  hold for a value whose one text is that string."
  [s]
  (sha-256 (utf8 s)))

(def ^:private record-cases
  "The cases of the record's rows: the keys, each row with the class of each
  field, what each row says, the facts by value, and the heads of the two
  reads."
  (let [u (uuid 5)
        digest (digest/digest good)
        value-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} u (int 2) (int 900))
        plain-row (fact-row writer {:e :alice :k :softland.store/depot :v :streaming :replaces nil :text ":streaming"} u 0 900)
        retract-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text nil} u 1 900)
        uuid-who "#uuid \"00000000-0000-0000-0000-00000000004d\""
        by-uuid (fact-row [uuid-who (str "[" uuid-who " :alice :alice [:alice :alice :alice] :see-and-act]")]
                          {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} u 0 900)
        marks-row (record/with-level-since
                   (fact-row writer (arrived-fact :alice :softland.store/marked-keys #{:on :tool/parent}) u 0 900)
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
     ["an accepted answer's row has no :refused; a refused one has its word; each field of its declared class"
      (fn [] [(classes (record/answer-row nil (int 900) digest)) (classes (record/answer-row :layer-not-made 900 digest))])
      [{:stamp "Long" :digest "byte[]"} {:stamp "Long" :digest "byte[]" :refused "Keyword"}]]
     ["an answer as a caller gets it, accepted and refused; no row is nothing yet"
      (fn [] [(record/answer-of (record/answer-row nil 900 digest))
              (record/answer-of (record/answer-row :layer-not-made 901 digest))
              (record/answer-of nil)])
      [{:verdict :accepted :reason nil :stamp 900 :digest (text/hex digest)}
       {:verdict :refused :reason :layer-not-made :stamp 901 :digest (text/hex digest)}
       nil]]
     ["the row of a fact with a value under a plain key: its value is the fact's text as it is, a String, and the row gives the UTF-8 of it back; the fact that wrote it, the stamp, who wrote it and the permission that the act named; each field of its declared class, and none a byte array"
      (fn [] [value-row (classes value-row) (record/plain-bytes-of value-row)])
      [(merge {:value "\"a text\"" :by-act u :by-index 2 :stamp 900} written-by-alice)
       {:value "String" :by-act "UUID" :by-index "Long" :stamp "Long" :who "String" :permission "String"}
       (utf8 "\"a text\"")]]
     ["the row of a fact under a store key: the text of its value as a String, whose bytes the row gives back; the fact that wrote it, the stamp, who and the permission, and no other field"
      (fn [] [plain-row (record/plain-bytes-of plain-row)])
      [(merge {:value ":streaming" :by-act u :by-index 0 :stamp 900} written-by-alice)
       (utf8 ":streaming")]]
     ["the row takes the fact's text and reads nothing of the fact's value: a fact that holds a value and no text gives the row of a retract, and one that holds a text and another value gives the text"
      (fn [] [(contains? (fact-row writer {:e :note-1 :k :text :v :a-value :replaces nil :text nil} u 0 900) :value)
              (:value (fact-row writer {:e :alice :k :softland.store/depot :v :streaming :replaces nil :text ":another"} u 0 900))])
      [false ":another"]]
     ["the plain bytes of a row's String are its UTF-8, written out here byte by byte: characters of two, three and four bytes; a String that begins with U+0000 has that byte first; nothing for no row and for the row of a retract"
      (fn [] [(record/plain-bytes "é✓𝄞") (record/plain-bytes "\u0000a")
              (record/plain-bytes-of nil) (record/plain-bytes-of retract-row)])
      [(the-bytes -61 -87 -30 -100 -109 -16 -99 -124 -98) (the-bytes 0 97) nil nil]]
     ["the row of a retract: no value, and the row is there; it holds who retracted and under which permission, and no other field"
      (fn [] retract-row)
      (merge {:by-act u :by-index 1 :stamp 900} written-by-alice)]
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
       {:value "String" :by-act "UUID" :by-index "Long" :stamp "Long" :who "String" :permission "String" :level-since "String"}
       {:on 100 :tool/parent 140} #{:on :tool/parent}
       {} {} {} {} {} {}]]
     ["the value digest: the SHA-256 of a row's value bytes, 64 hex characters; of a value, the digest of the UTF-8 of its one text"
      (fn [] [(record/value-digest (utf8 ":c1")) (count (record/value-digest (byte-array 0)))
              (record/value-digest-of :c1) (record/value-digest-of (uuid 9)) (record/value-digest-of "a string with spaces")
              (record/value-digest-of 42) (record/value-digest-of {:b 1 :a [2 3]}) (record/value-digest-of (int 42))])
      [d-c1 64
       d-c1 (digest-of-text "#uuid \"00000000-0000-0000-0000-000000000009\"") (digest-of-text "\"a string with spaces\"")
       (digest-of-text "42") (digest-of-text "{:a [2 3] :b 1}") (digest-of-text "42")]]
     ["a key of the facts by value: layer, key, the stamp since which the key is marked, the value digest, then the entity; its row is the fact's row without its value, with the two texts as they were given"
      (fn [] (let [row (by-value-row writer u (int 2) (int 900))]
               [(record/by-value-key :alice :on 100 d-c1 :b1)
                (record/by-value-key (uuid 1) :tool/on 140 d-c1 (uuid 2))
                row (classes row)]))
      [(str ":alice :on 100 " d-c1 " :b1")
       (str "00000000-0000-0000-0000-000000000001 :tool/on 140 " d-c1 " 00000000-0000-0000-0000-000000000002")
       (merge {:by-act u :by-index 2 :stamp 900} written-by-alice)
       {:by-act "UUID" :by-index "Long" :stamp "Long" :who "String" :permission "String"}]]
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
     ["a key back to its parts: a printed id to the id, a place's key to layer, entity and key, a key of the facts by value to its entity and to its digest, which is its route in a microbatch layer; nothing for a text that is none"
      (fn [] [(mapv record/id-of-text [":alice" ":tool/on" "00000000-0000-0000-0000-000000000005" "alice" ":a b" ":" "" "5-5-5-5-5" nil 7])
              (record/place-of-key (record/place-key :alice (uuid 2) :tool/text))
              (record/place-of-key (record/place-key :base :base :softland.store/marked-keys))
              (mapv record/place-of-key [":alice :note-1" ":alice :note-1 :text :more" ":alice :note-1 00000000-0000-0000-0000-000000000005" "" nil])
              (record/by-value-entity (record/by-value-key :alice :on 100 d-c1 (uuid 2)))
              (record/by-value-entity (record/by-value-key :alice :on 100 d-c1 :b1))
              (record/by-value-route (record/by-value-key :alice :on 100 d-c1 :b1))
              (mapv record/by-value-entity [":alice :on 100" "" nil])])
      [[:alice :tool/on (uuid 5) nil nil nil nil nil nil nil]
       [:alice (uuid 2) :tool/text]
       [:base :base :softland.store/marked-keys]
       [nil nil nil nil nil]
       (uuid 2) :b1 d-c1
       [nil nil nil]]]
     ["the head of the read by act-id: the depot and the key; nothing for what is no act-id of the layer"
      (fn [] [(record/answer-read :alice [:alice :streaming u]) (record/answer-read :alice [:alice :microbatch u])
              (record/answer-read :bob [:alice :streaming u]) (record/answer-read :alice [:alice :streaming])
              (record/answer-read nil nil) (record/answer-read (long-id 257) [(long-id 257) :streaming u])
              (record/answer-read :alice 5)])
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":alice 00000000-0000-0000-0000-000000000005"]
       nil nil nil nil nil]]
     ["the head of the read of a depot: the layer's depot place; nothing for what is no id"
      (fn [] [(record/depot-read :alice) (record/depot-read "alice") (record/depot-read nil)
              (record/depot-read (long-id 257)) (record/depot-read [:alice])])
      [":alice :alice :softland.store/depot" nil nil nil nil]]]))

;; --------------------------------------------------------------- the decision

(def ^:private now "The gate's clock in the cases of the decision." 1700000000000)

(defn- permission-row
  "The row of a permission fact, as an accept writes it."
  [permission]
  (fact-row writer (arrived-fact (first permission) :softland.store/permission permission) (uuid 90) 1 (stamp/pack now 0)))

(def ^:private depot-row
  "The row of the depot fact of Alice's layer."
  (fact-row writer (arrived-fact :alice :softland.store/depot :streaming) (uuid 90) 0 (stamp/pack now 0)))

(def ^:private bobs-see
  "Bob's permission in Alice's layer, beneath hers, that says see."
  [:bob :alice :alice alice-own :see])

(def ^:private bobs-see-and-act
  "Bob's permission in Alice's layer, beneath hers, that says see and act."
  [:bob :alice :alice alice-own :see-and-act])

(def ^:private standing-rows
  "For each: the act, the row at the place of who's permission, the row at
  the layer's depot place, whether the other gate holds the layer; then the
  word of `standing`, and whether the act makes the layer."
  (let [alices (act-of good)
        bobs-act (assoc alices :who :bob :permission bobs)
        personal (making :alice :system :alice)
        group (making :team :alice :alice)]
    [["the act's permission is at its holder's place" alices (permission-row alice-own) nil false nil false]
     ["an act under a permission given beneath another, at its holder's place" bobs-act (permission-row bobs) nil false nil false]
     ["the place holds another permission of the same holder" (assoc alices :permission [:alice :alice :alice [:x :alice :alice]])
      (permission-row alice-own) nil false :permission-does-not-stand false]
     ["the place is empty and the layer is made: the record does not hold the permission" bobs-act nil depot-row false
      :permission-does-not-stand false]
     ["an act under Alice's permission by a who that is not Alice, in a layer that is made"
      (assoc alices :who :mallory) nil depot-row false :who-not-the-permissions-holder false]
     ["the system acts again in a personal layer that it made" personal nil depot-row false
      :who-not-the-permissions-holder false]
     ["a making act by the holder for a layer that is made: its permission stands, and it makes nothing"
      (making :alice :alice :alice) (permission-row alice-own) nil false nil false]
     ["an act into a layer that is not made" alices nil nil false :layer-not-made false]
     ["a right making act of a personal layer, into a layer that is not made" personal nil nil false nil true]
     ["a right making act of a group's layer" group nil nil false nil true]
     ["a making act of the base at the streaming gate" (making :base :system :system) nil nil false :layer-not-made false]
     ["an act into a layer that the other gate's record holds" alices nil nil true :layer-belongs-to-the-other-gate false]
     ["a making act for a layer that the other gate's record holds" group nil nil true :layer-belongs-to-the-other-gate false]
     ["a holder of see who names her permission as the row has it: an act needs act"
      (assoc alices :who :bob :permission bobs-see) (permission-row bobs-see) nil false :permission-does-not-allow-acting false]
     ["a holder of see who names her permission without the slot: the bytes are not the row's, so naming more does not stand"
      bobs-act (permission-row bobs-see) nil false :permission-does-not-stand false]
     ["a holder of see who names her permission with see and act in the slot"
      (assoc alices :who :bob :permission bobs-see-and-act) (permission-row bobs-see) nil false :permission-does-not-stand false]
     ["a holder of a permission that says see and act, named as the row has it"
      (assoc alices :who :bob :permission bobs-see-and-act) (permission-row bobs-see-and-act) nil false nil false]
     ["a holder of a permission that was given without the slot: it reads as see and act"
      bobs-act (permission-row bobs) nil false nil false]
     ["a holder of see and act who names her permission with see in the slot: it does not stand, so nobody acts under less by naming less either"
      (assoc alices :who :bob :permission bobs-see) (permission-row bobs-see-and-act) nil false :permission-does-not-stand false]
     ["a holder of see whose act is by another who: the word of the holder comes first"
      (assoc alices :who :mallory :permission bobs-see) nil depot-row false :who-not-the-permissions-holder false]
     ["a making act whose permission has the slot, into a layer that is not made: it is no making act"
      (-> group
          (assoc :permission [:alice :team :team [:x :team :team] :see-and-act])
          (assoc-in [:facts 1 :v] [:alice :team :team [:x :team :team] :see-and-act]))
      nil nil false :layer-not-made false]]))

(def ^:private standing-cases
  "The cases of the standing: the two places that are read, and one case for
  each row of `standing-rows`."
  (concat
   [["the places that the standing reads: who's permission, then the layer's depot"
     (fn [] [(decision/permission-place (act-of good)) (decision/depot-place (act-of good))])
     [":alice :alice :softland.store/permission" ":alice :alice :softland.store/depot"]]
    ["what a permission allows, read from its one form: a first permission and one given without the slot read as see and act; with the slot, what the slot says; the slot of a permission above it is not read"
     (fn [] (mapv act/allows [alice-own bobs bobs-see bobs-see-and-act [:carol :alice :alice bobs-see]]))
     [:see-and-act :see-and-act :see :see-and-act :see-and-act]]]
   (map (fn [[what act p-row c-row other? word makes?]]
          [(str "standing: " what)
           (fn [] [(decision/standing :streaming act p-row c-row other?)
                   (decision/makes-the-layer? :streaming act p-row c-row other?)])
           [word makes?]])
        standing-rows)))

(def ^:private early-cases
  "The cases of the checks that read nothing of the record: the
  declaration's word, and the 10-second rule."
  (let [carrying (fn [millis] (act-of (assoc good :based-on {[(act-id 2) 0] (stamp/pack millis 0)})))]
    [["the declaration's word comes first"
      (fn [] (decision/refused-early now nil :value-too-large))
      :value-too-large]
     ["an act that carries no stamp, or one in the past, or one 10 seconds ahead, goes on"
      (fn [] [(decision/refused-early now (act-of good) nil)
              (decision/refused-early now (carrying (- now 5000)) nil)
              (decision/refused-early now (carrying (+ now 10000)) nil)])
      [nil nil nil]]
     ["an act that carries a stamp more than 10 seconds ahead of the clock is refused"
      (fn [] (decision/refused-early now (carrying (+ now 10001)) nil))
      :carried-stamp-too-far-ahead]
     ["claimed-when far ahead is a claim: no refusal"
      (fn [] (decision/refused-early now (act-of (assoc good :claimed-when Long/MAX_VALUE)) nil))
      nil]]))

(defn- row-by
  "The row that the fact `index` of the act `(act-id n)` wrote: a value at
  :note-1 :text."
  [n index]
  (fact-row writer (arrived-fact :note-1 :text "a text before") (uuid n) index (stamp/pack now 0)))

(defn- setting-row
  "The row that the fact 0 of the act `(act-id n)` wrote under a store key on
  Alice's layer."
  [n k v]
  (fact-row writer (arrived-fact :alice k v) (uuid n) 0 (stamp/pack now 0)))

(def ^:private places-rows
  "For each: the facts of Alice's act, the rows of the places read (by
  entity and key), and the word of `places`."
  (let [replacing (fn [fact-id] (assoc (value-at :note-1 :text) :replaces fact-id))
        retract-row (fact-row writer (arrived-fact :note-1 :text) (uuid 8) 0 (stamp/pack now 0))
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
  "The cases of the stamp of a decision and of the rows that a decision
  writes: a refusal, an accept, a making act, a retract, a write of the
  marked keys."
  (let [stamp (stamp/pack now 0)
        digest (digest/digest good)
        value-act (act-of good)
        answer-key (record/answer-key :alice (uuid 1))
        note-place ":alice :note-1 :text"
        marks-place ":alice :alice :softland.store/marked-keys"
        marking (fn [keys] (act-of (arrived 1 {:e :alice :k :softland.store/marked-keys :v keys})))
        marks-before (record/with-level-since (setting-row 7 :softland.store/marked-keys #{:on :gone}) {:on 100 :gone 100})
        level-since-of (fn [act row-before level-from]
                         (let [[[_ _ row]] (decision/writes (act-id 1) act nil stamp digest
                                                            {:place-rows {marks-place row-before} :level-from level-from})]
                           (record/marked-keys-of row)))
        uuid-who "#uuid \"00000000-0000-0000-0000-00000000004d\""]
    [["the stamp of a decision: later than the last stamp and than each stamp that the act carries"
      (fn [] (let [carrying (act-of (assoc good :based-on {[(act-id 2) 0] (stamp/pack (+ now 3000) 5)}))]
               [(decision/stamp-of now nil nil :malformed)
                (decision/stamp-of now (stamp/pack now 4) (act-of good) nil)
                (decision/stamp-of now (stamp/pack now 4) carrying nil)
                (decision/stamp-of now (stamp/pack now 4) carrying :layer-not-made)]))
      [(stamp/pack now 0) (stamp/pack now 5) (stamp/pack (+ now 3000) 6) (stamp/pack (+ now 3000) 6)]]
     ["an act refused for a carried stamp too far ahead gets an ordinary stamp"
      (fn [] (decision/stamp-of now (stamp/pack now 4)
                                (act-of (assoc good :based-on {[(act-id 2) 0] (stamp/pack (+ now 60000) 0)}))
                                :carried-stamp-too-far-ahead))
      (stamp/pack now 5)]
     ["a refusal writes the answer and the last stamp, and nothing else; what the gate has in hand is not read: with the stamp of a marked key and a row before in hand, an accept of the same act writes the fact's row and two rows of the facts by value beside them"
      (fn [] (let [in-hand {:since-by-place {note-place 100} :place-rows {note-place (row-by 7 0)} :level-from stamp}]
               [(decision/writes (act-id 1) value-act :replaced-fact-not-current stamp digest {})
                (decision/writes (act-id 1) value-act :replaced-fact-not-current stamp digest in-hand)
                (mapv first (decision/writes (act-id 1) value-act nil stamp digest in-hand))]))
      [[[:answer answer-key {:refused :replaced-fact-not-current :stamp stamp :digest digest}]
        [:last-stamp nil stamp]]
       [[:answer answer-key {:refused :replaced-fact-not-current :stamp stamp :digest digest}]
        [:last-stamp nil stamp]]
       [:fact :by-value :by-value :answer :last-stamp]]]
     ["a thing that the declaration refused, which is no act, writes the same two"
      (fn [] (decision/writes (act-id 1) nil :malformed stamp digest {}))
      [[:answer answer-key {:refused :malformed :stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of one value writes three rows: the fact, with the one text of its value, who wrote it and the permission that the act named; the answer; the last stamp"
      (fn [] (decision/writes (act-id 1) value-act nil stamp digest {}))
      [[:fact note-place (merge {:value "\"a text\"" :by-act (uuid 1) :by-index 0 :stamp stamp} written-by-alice)]
       [:answer answer-key {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["the fact's row holds the text of the value in its normal form, whatever form its maker gave: its String as written out here, the value that it reads back as, its plain bytes, and the six fields of the row"
      (fn [] (let [act (act-of (arrived 1 (value-at :note-1 :text (array-map :b 1.50M :a [(int 1) 2N]))))
                   [[_ _ row]] (decision/writes (act-id 1) act nil stamp digest {})]
               [(:value row) (record/read-value row) (record/plain-bytes-of row) (sort (keys row))]))
      ["{:a [1 2] :b 1.5M}" [:value {:a [1 2] :b 1.5M}] (utf8 "{:a [1 2] :b 1.5M}")
       [:by-act :by-index :permission :stamp :value :who]]]
     ["the texts of who and of the permission are made once for the act and each row holds them: an act by a who that is a UUID, with a value and a retract"
      (fn [] (let [act (act-of (assoc (arrived 1 (value-at :note-1 :text) {:e :note-2 :k :text})
                                      :who (uuid 77) :permission [(uuid 77) :alice :alice alice-own :see-and-act]))]
               (mapv (fn [[_ _ row]] (select-keys row [:who :permission]))
                     (take 2 (decision/writes (act-id 1) act nil stamp digest {})))))
      (vec (repeat 2 {:who uuid-who :permission (str "[" uuid-who " :alice :alice [:alice :alice :alice] :see-and-act]")}))]
     ["an accept of a making act writes the depot, the first permission, the answer, the last stamp; each row holds who made the layer and the permission that the act named"
      (fn [] (decision/writes [:team :streaming (uuid 1)] (making :team :alice :alice) nil stamp digest {}))
      [[:fact ":team :team :softland.store/depot"
        {:value ":streaming" :by-act (uuid 1) :by-index 0 :stamp stamp :who ":alice" :permission "[:alice :team :team]"}]
       [:fact ":team :alice :softland.store/permission"
        {:value "[:alice :team :team]" :by-act (uuid 1) :by-index 1 :stamp stamp :who ":alice" :permission "[:alice :team :team]"}]
       [:answer (record/answer-key :team (uuid 1)) {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["the system makes a personal layer for its person: the rows hold the system as who, and the person's permission"
      (fn [] (mapv (fn [[_ _ row]] (select-keys row [:who :permission]))
                   (take 2 (decision/writes [:alice :streaming (uuid 1)] (making :alice :system :alice) nil stamp digest {}))))
      [{:who ":system" :permission "[:alice :alice :alice]"} {:who ":system" :permission "[:alice :alice :alice]"}]]
     ["an accept of a retract and a permission given writes a row for each: the retract's has no value; each holds who and the permission"
      (fn [] (mapv (fn [[which key row]] [which key (if (map? row) (dissoc row :by-act :stamp) row)])
                   (decision/writes (act-id 1)
                                    (act-of (arrived 1 {:e :note-1 :k :text} {:e :bob :k :softland.store/permission :v bobs}))
                                    nil stamp digest {})))
      [[:fact note-place (merge {:by-index 0} written-by-alice)]
       [:fact ":alice :bob :softland.store/permission"
        (merge {:value "[:bob :alice :alice [:alice :alice :alice]]" :by-index 1} written-by-alice)]
       [:answer answer-key {:digest digest}]
       [:last-stamp nil stamp]]]
     ["a give with the slot writes the permission's whole text, slot and all: what the gate later compares with the permission that an act names"
      (fn [] (let [[[_ k row]] (decision/writes (act-id 1) (act-of (arrived 1 {:e :bob :k :softland.store/permission :v bobs-see}))
                                                nil stamp digest {})]
               [k (:value row) (record/holds-value? row bobs-see) (record/holds-value? row bobs)]))
      [":alice :bob :softland.store/permission" "[:bob :alice :alice [:alice :alice :alice] :see]" true false]]
     ["a give of a permission 32 deep writes its whole text, as Clojure prints the permission, with its 32 vectors; the row holds that permission and not the one above it"
      (fn [] (let [given (reduce (fn [parent _] [:bob :alice :alice parent]) alice-own (range 31))
                   [[_ _ row]] (decision/writes (act-id 1) (act-of (arrived 1 {:e :bob :k :softland.store/permission :v given}))
                                                nil stamp digest {})]
               [(= (:value row) (pr-str given)) (count (re-seq #"\[" (:value row)))
                (record/holds-value? row given) (record/holds-value? row (nth given 3))]))
      [true 32 true false]]
     ["a write of the marked keys: the row holds the set that the maker wrote and, derived by the gate, the stamp of each key; a newly marked key gets the stamp that the gate hands it"
      (fn [] (let [[[which k row] & more] (decision/writes (act-id 1) (marking #{:on :tool/parent}) nil stamp digest
                                                           {:place-rows {marks-place nil} :level-from 500})]
               [which k (:value row) (record/read-value row) (record/marked-keys-of row) (mapv first more)]))
      [:fact marks-place "#{:on :tool/parent}" [:value #{:on :tool/parent}] {:on 500 :tool/parent 500} [:answer :last-stamp]]]
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
                                            nil stamp digest {}))))
      [[":alice :alice :softland.store/who-may-see" ":everyone" [:value :everyone] false]
       [":alice :alice :softland.store/builds-on" "[:base :team]" [:value [:base :team]] false]]]]))

;; --------------------------------------- the facts by value, one act at a time

(defn- streaming-step
  "Decides one accepted act of Alice's in her streaming layer against a
  record in memory, as steps 6b and 7 of the streaming gate do: the row at
  the layer's marked-keys place when the act needs it, the stamp of each of
  its places under a marked key, the row before of each such place, then
  `softland.store.gate.decision/writes` with the act's stamp as the stamp of
  a newly marked key. Takes the record, {:facts :by-value}, each a map from
  key to row; the number of the act's act-id; its stamp; and its facts as a
  maker gives them. The act goes through the declaration's check, which
  gives each value its text. Gives the record after, with :writes, the rows
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
                       (decision/writes (act-id n) act nil stamp (digest/digest thing)
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
       (merge {:by-index 0 :stamp 110} written-by-alice)
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

;; ------------------------------------------------------------------ the batch

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
  act-id `(base-id n)`, by `who` under `permission`, with these facts."
  [n who permission & facts]
  {:act-id (base-id n) :depot :microbatch
   :version 1 :who who :layer :base :permission permission :session :s1
   :facts (vec facts)})

(def ^:private base-writer
  "The two texts that each row of a fact written by the system under its own
  permission in the base holds: of who and of the permission."
  [":system" "[:system :base :base]"])

(def ^:private batch-first-rows
  "For each: a thing, and what the depot derives from it."
  [["an act: the entity of its first fact" (base-thing 1 :system system-own {:e :doc-1 :k :note} {:e :doc-2 :k :note}) :doc-1]
   ["facts as a list" {:facts (list {:e :doc-3 :k :note})} :doc-3]
   ["an entity that is a UUID" {:facts [{:e (uuid 3) :k :note}]} (uuid 3)]
   ["no facts" {:act-id (base-id 1)} nil]
   ["facts that are no sequence" {:facts 5} nil]
   ["a first fact that is no map" {:facts [5 {:e :doc-1}]} nil]
   ["no map" 5 nil]
   ["nothing" nil nil]
   ["a record" (->Thing :doc-1 :note) nil]
   ["facts that throw when they are read" {:facts (throwing)} nil]])

(def ^:private batch-turn-cases
  "The cases of the turn, of the gathering and of the stamps of a batch."
  (let [bound (stamp/pack (+ now 10000) 65535)]
    [["the position of a thing: the count given so far in this batch; 0 for another batch and for no count"
      (fn [] [(batch/position {:batch "7" :given 3} "7") (batch/position {:batch "6" :given 3} "7") (batch/position nil "7")])
      [3 0 0]]
     ["the count after a position was given"
      (fn [] (batch/turns-after "7" 3))
      {:batch "7" :given 4}]
     ["the turn: position x tasks + partition"
      (fn [] [(batch/turn 0 4 0) (batch/turn 0 4 3) (batch/turn 1 4 0) (batch/turn 2 4 1)])
      [0 3 4 9]]
     ["on one partition the turns keep the depot's order; between two partitions the lower position goes first"
      (fn [] [(< (batch/turn 5 128 127) (batch/turn 6 128 127)) (< (batch/turn 0 128 127) (batch/turn 1 128 0))])
      [true true]]
     ["gathering: two vectors are one, in either order of arrival the same things; the numbers keep the greater of each; nil is empty"
      (fn [] [(batch/gather [1] [2 3]) (set (batch/gather [2 3] [1])) (batch/gather nil [1]) (batch/gather [1] nil)
              (batch/gather {:carried 5 :position 2} {:carried nil :position 7})
              (batch/gather {:carried nil :position 1} {:carried 9 :position 0})])
      [[1 2 3] #{1 2 3} [1] [1] {:carried 5 :position 7} {:carried 9 :position 1}]]
     ["the base: nothing for a batch with no numbers; the clock; later than the last stamp; later than a carried stamp"
      (fn [] [(batch/base now 5 nil)
              (batch/base now nil {:carried nil :position 0})
              (batch/base now (stamp/pack now 9) {:carried nil :position 0})
              (batch/base now nil {:carried (stamp/pack (+ now 3000) 5) :position 0})])
      [nil (stamp/pack now 0) (stamp/pack now 10) (stamp/pack (+ now 3000) 6)]]
     ["a carried stamp far ahead puts the base 10 seconds ahead and no further"
      (fn [] (batch/base now nil {:carried (stamp/pack (+ now 3600000) 0) :position 0}))
      (inc bound)]
     ["the stamp of a turn is the base plus the turn; the gate's last stamp is not below any stamp of the batch"
      (fn [] (let [base (stamp/pack now 0)
                   last-stamp (batch/last-stamp-of-batch base {:carried nil :position 2} 4)]
               [(- (batch/stamp-of-turn base 9) base) (- last-stamp base)
                (<= (batch/stamp-of-turn base (batch/turn 2 4 3)) last-stamp)]))
      [9 11 true]]
     ["the tasks write a new closed-through once after a batch that gave a stamp, and not in a quiet time"
      (fn [] [(batch/closes? 7 5) (batch/closes? 7 nil) (batch/closes? 7 7) (batch/closes? nil nil)])
      [true true false false]]]))

(def ^:private not-a-clock-row
  "Things that are no row of the kept clock and no record of the depot of
  batch clocks: each of the three functions of a batch's clock is given
  them, since none may throw."
  [nil 5 "a text" [] [41 5] {} {:batch 41} {:clock 5} {:batch "41" :clock 5} {:batch 41 :clock 1.5}
   {:batch 41 :clock nil} {:batch (bigint "99999999999999999999") :clock 5} (throwing) (->Thing :doc-1 :note)])

(defn- kept-after
  "Takes records of the depot of batch clocks in the order in which the
  topology that keeps the clock is handed them. Gives the row that the table
  holds after them, from an empty table: each record through
  `batch/clock-row-to-keep`, written when it gives a row."
  [records]
  (reduce (fn [held record] (or (batch/clock-row-to-keep held record) held)) nil records))

(def ^:private batch-clock-cases
  "The cases of the pure code of a batch's clock: the record that the gate
  appends, the clock that a row holds for a batch, and the rule of the
  topology that keeps the row."
  (let [c1 now
        c2 (+ now 1088)]
    [["the record of a batch's clock holds the batch's id and the reading, both as longs, and no other part"
      (fn [] (let [record (batch/clock-record (int 41) c1)]
               [record (every? #(instance? Long %) (vals record)) (batch/clock-record 41N 7)]))
      [{:batch 41 :clock c1} true {:batch 41 :clock 7}]]
     ["an id or a reading that is no integer, or that no long holds, gives no record, and nothing throws"
      (fn [] [(batch/clock-record nil c1) (batch/clock-record "41" c1) (batch/clock-record 41 nil) (batch/clock-record 41 1.5)
              (batch/clock-record (bigint "99999999999999999999") c1) (batch/clock-record (throwing) c1)])
      [nil nil nil nil nil nil]]
     ["the kept clock of a batch: the clock of the row when the row is of that batch; none for no row, and for a row of an older or of a later batch"
      (fn [] [(batch/kept-clock {:batch 41 :clock c1} 41) (batch/kept-clock nil 41)
              (batch/kept-clock {:batch 39 :clock c1} 41) (batch/kept-clock {:batch 43 :clock c1} 41)])
      [c1 nil nil nil]]
     ["the kept clock, given a thing that is no row, or an id that is no integer: none, and nothing throws"
      (fn [] [(mapv #(batch/kept-clock % 41) not-a-clock-row)
              (mapv #(batch/kept-clock {:batch 41 :clock c1} %) [nil "41" 41.0 (throwing)])])
      [(vec (repeat (count not-a-clock-row) nil)) [nil nil nil nil]]]
     ["the row to keep: the record's batch and clock when the table holds no row, or a row of an older batch"
      (fn [] [(batch/clock-row-to-keep nil {:batch 41 :clock c1})
              (batch/clock-row-to-keep {:batch 39 :clock 5} {:batch 41 :clock c1})])
      [{:batch 41 :clock c1} {:batch 41 :clock c1}]]
     ["nothing to keep for a record of the row's own batch, with the same clock or with another, and for a record of an older batch than the row's"
      (fn [] [(batch/clock-row-to-keep {:batch 41 :clock c1} {:batch 41 :clock c1})
              (batch/clock-row-to-keep {:batch 41 :clock c1} {:batch 41 :clock c2})
              (batch/clock-row-to-keep {:batch 43 :clock c2} {:batch 41 :clock c1})])
      [nil nil nil]]
     ["the row to keep holds two longs and no other part of the record"
      (fn [] (let [row (batch/clock-row-to-keep nil {:batch (int 41) :clock (int 5) :more "of the record"})]
               [row (every? #(instance? Long %) (vals row))]))
      [{:batch 41 :clock 5} true]]
     ["a thing that is no record of the depot is not kept, whatever the table holds, and nothing throws; a held thing that is no row counts as no row"
      (fn [] [(mapv #(batch/clock-row-to-keep nil %) not-a-clock-row)
              (mapv #(batch/clock-row-to-keep {:batch 39 :clock 5} %) not-a-clock-row)
              (batch/clock-row-to-keep 5 {:batch 41 :clock c1})
              (batch/clock-row-to-keep (throwing) {:batch 41 :clock c1})])
      [(vec (repeat (count not-a-clock-row) nil)) (vec (repeat (count not-a-clock-row) nil))
       {:batch 41 :clock c1} {:batch 41 :clock c1}]]
     ["first write wins: of two records of one batch with two clocks, the one that is processed first stays, in either order; a record handed over twice changes nothing"
      (fn [] [(kept-after [{:batch 41 :clock c1} {:batch 41 :clock c2}])
              (kept-after [{:batch 41 :clock c2} {:batch 41 :clock c1}])
              (kept-after [{:batch 41 :clock c1} {:batch 41 :clock c1} {:batch 41 :clock c1}])])
      [{:batch 41 :clock c1} {:batch 41 :clock c2} {:batch 41 :clock c1}]]
     ["a later batch's record replaces the row, and a record of a closed batch that comes late never puts the row back"
      (fn [] [(kept-after [{:batch 41 :clock c1} {:batch 43 :clock c2}])
              (kept-after [{:batch 41 :clock c1} {:batch 43 :clock c2} {:batch 41 :clock c1} {:batch 42 :clock c1}])
              (batch/kept-clock (kept-after [{:batch 41 :clock c1} {:batch 43 :clock c2}]) 41)])
      [{:batch 43 :clock c2} {:batch 43 :clock c2} nil]]
     ["a batch with no numbers has no base, also when it was given no clock: nothing is read for it"
      (fn [] [(batch/base nil 5 nil) (batch/base nil nil nil)])
      [nil nil]]]))

(def ^:private base-depot-row
  "The row of the depot fact of the base."
  (fact-row base-writer (arrived-fact :base :softland.store/depot :microbatch) (uuid 800) 0 (stamp/pack (- now 5000) 0)))

(defn- base-permission-row
  "The row of a permission fact of the base, as an accept writes it."
  [permission]
  (fact-row base-writer (arrived-fact (first permission) :softland.store/permission permission) (uuid 800) 1 (stamp/pack (- now 5000) 0)))

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
             (stamp/pack (- now 5000) 0))
   since-by-key))

(def ^:private arrival-rows
  "For each: an act, the row at the place of its who's permission, the row at
  its layer's depot place, and what step 1 says of its standing."
  (let [act (act-of (base-thing 1 :carol carols {:e :doc-1 :k :note}))
        seeing (act-of (base-thing 1 :carol carols-see {:e :doc-1 :k :note}))
        by-another (act-of (base-thing 1 :mallory carols {:e :doc-1 :k :note}))]
    [["the row holds the act's permission" act (base-permission-row carols) nil {:standing :stands}]
     ["the row holds the act's permission, and it says see: refused for good, and the act asks for none of its rows" seeing
      (base-permission-row carols-see) nil {:standing :final :word :permission-does-not-allow-acting}]
     ["the row holds a permission that says see, and the act names it without the slot" act (base-permission-row carols-see) nil
      {:standing :final :word :permission-does-not-stand}]
     ["the row holds another permission" act (base-permission-row [:carol :base :base carols]) nil
      {:standing :final :word :permission-does-not-stand}]
     ["the place is empty in a layer that is made: a give of this batch may fill it" act nil base-depot-row
      {:standing :open :layer-in-record? true}]
     ["the place is empty, and the holder is not the who: no give can fill it" by-another nil base-depot-row
      {:standing :final :word :who-not-the-permissions-holder}]
     ["the record holds nothing of the layer" act nil nil {:standing :open :layer-in-record? false}]
     ["a making act, with nothing in the record" (act-of (assoc (base-thing 1 :system system-own {:e :base :k :softland.store/depot :v :microbatch}
                                                                              {:e :system :k :softland.store/permission :v system-own})
                                                             :layer :base))
      nil nil {:standing :open :layer-in-record? false}]]))

(defn- kinds-of
  "Takes messages of step 1. Gives [kind route key] of each, with the payload
  left out."
  [messages]
  (mapv (fn [[route kind k _]] [kind route k]) messages))

(def ^:private arrival-cases
  "The cases of step 1: the standing on arrival, the questions of an act, and
  the messages of a thing."
  (let [u (uuid 1)
        act (act-of (base-thing 1 :system system-own
                                (assoc (value-at :doc-1 :title "a") :replaces [(base-id 800) 0])
                                (value-at :doc-1 :body "b")
                                (value-at :doc-2 :title "c")
                                {:e :doc-3 :k :note}
                                {:e :carol :k :softland.store/permission :v carols}))
        questions (batch/questions act 7 :doc-1 true)
        payload-of (fn [kind k] (first (keep (fn [[_ kind' k' [q]]] (when (and (= kind kind') (= k k')) q)) questions)))
        system-row (base-permission-row system-own)
        simple (base-thing 2 :system system-own {:e :doc-1 :k :note})
        simple-act (act-of simple)
        refused (base-thing 3 :system system-own {:e :doc-1 :k :softland.store/owner :v :system})]
    (concat
     (map (fn [[what an-act permission-row depot-row expected]]
            [(str "the standing on arrival: " what)
             (fn [] (batch/standing-on-arrival an-act permission-row depot-row))
             expected])
          arrival-rows)
     [["the questions of an act are of two kinds: its answer; the place of each fact under a plain key, with a value or with none; nothing for a permission fact"
       (fn [] (kinds-of questions))
       [[:answer u (record/answer-key :base u)]
        [:place :doc-1 ":base :doc-1 :title"]
        [:place :doc-1 ":base :doc-1 :body"]
        [:place :doc-2 ":base :doc-2 :title"]
        [:place :doc-3 ":base :doc-3 :note"]]]
      ["a question about a place says whether the row is wanted and whether the place is the act's own; and it holds the key of the layer's marked-keys place and the fact's key, from which the home task reads whether the place's key is marked"
       (fn [] [(payload-of :place ":base :doc-1 :title") (payload-of :place ":base :doc-1 :body")
               (payload-of :place ":base :doc-2 :title")])
       [{:turn 7 :to :doc-1 :question :place :wanted? true :own? true :marks-place base-marks-place :k :title}
        {:turn 7 :to :doc-1 :question :place :wanted? false :own? true :marks-place base-marks-place :k :body}
        {:turn 7 :to :doc-1 :question :place :wanted? false :own? false :marks-place base-marks-place :k :title}]]
      ["a fact under each of the five store keys asks nothing, since every task holds its row: a depot, a permission, who may see, what the layer builds on, the marked keys; a bare word that looks like one of them is a plain key"
       (fn [] (let [five (act-of (base-thing 4 :system system-own
                                             {:e :base :k :softland.store/depot :v :microbatch}
                                             {:e :carol :k :softland.store/permission :v carols}
                                             {:e :base :k :softland.store/who-may-see :v :members}
                                             {:e :base :k :softland.store/builds-on :v [:team]}
                                             {:e :base :k :softland.store/marked-keys :v #{:on}}))]
                [(kinds-of (batch/questions five 7 :base true))
                 (mapv batch/on-every-task?
                       [:softland.store/depot :softland.store/permission :softland.store/who-may-see :softland.store/builds-on
                        :softland.store/marked-keys :on :permission :marked-keys])
                 (= (set (filter batch/on-every-task? (keys declaration/store-keys))) (set (keys declaration/store-keys)))]))
       [[[:answer (uuid 4) (record/answer-key :base (uuid 4))]]
        [true true true true true false false false]
        true]]
      ["an act that asks for no rows asks for its answer only"
       (fn [] (kinds-of (batch/questions act 7 :doc-1 false)))
       [[:answer u (record/answer-key :base u)]]]
      ["the messages of an act that stands: its entry to the task of its first entity, its questions, the numbers to the gate's task"
       (fn [] (let [messages (batch/arrive simple simple-act nil 9 2 system-row nil)]
                [(kinds-of messages) (dissoc (first (nth (first messages) 3)) :thing :act :permission-row)
                 (nth (last messages) 3)]))
       [[[:entry :doc-1 9] [:answer (uuid 2) (record/answer-key :base (uuid 2))] [:place :doc-1 ":base :doc-1 :note"]
         [:numbers :microbatch :batch]]
        {:turn 9 :act-id (base-id 2) :to :doc-1 :standing :stands :asks-rows? true :depot-row nil}
        {:carried nil :position 2}]]
      ["the numbers hold the greatest stamp that the act carries"
       (fn [] (let [carrying (assoc simple :based-on {[(base-id 800) 0] 77 [(base-id 800) 1] 99})]
                (nth (last (batch/arrive carrying (act-of carrying) nil 9 2 system-row nil)) 3)))
       {:carried 99 :position 2}]
      ["a thing that the declaration refused: a small entry with its word and its digest, to the task of its uuid, and one question"
       (fn [] (let [messages (batch/arrive refused nil (:refuse (declaration/check refused)) 5 1 nil nil)]
                [(kinds-of messages) (first (nth (first messages) 3))]))
       [[[:entry (uuid 3) 5] [:answer (uuid 3) (record/answer-key :base (uuid 3))] [:numbers :microbatch :batch]]
        {:turn 5 :act-id (base-id 3) :to (uuid 3) :refused :store-key-not-known :digest (digest/digest refused)}]]
      ["an act refused for good, and an act into a layer of which the record holds nothing, ask for their answer only"
       (fn [] [(map first (kinds-of (batch/arrive simple simple-act nil 9 2 (base-permission-row carols) nil)))
               (map first (kinds-of (batch/arrive simple simple-act nil 9 2 nil nil)))
               (map first (kinds-of (batch/arrive simple simple-act nil 9 2 nil base-depot-row)))])
       [[:entry :answer :numbers] [:entry :answer :numbers] [:entry :answer :place :numbers]]]
      ["a holder of see: her act is refused for good on arrival, with the word, and asks for its answer only"
       (fn [] (let [thing (base-thing 2 :carol carols-see {:e :doc-1 :k :note})
                    messages (batch/arrive thing (act-of thing) nil 9 2 (base-permission-row carols-see) nil)]
                [(map first (kinds-of messages))
                 (select-keys (first (nth (first messages) 3)) [:standing :final-word :asks-rows?])]))
       [[:entry :answer :numbers]
        {:standing :final :final-word :permission-does-not-allow-acting :asks-rows? false}]]
      ["a holder of see who sends a value: her act is refused for good on arrival with the same word, and asks about no place of its value"
       (fn [] (let [thing (base-thing 2 :carol carols-see (value-at :doc-1 :note "a value"))
                    messages (batch/arrive thing (act-of thing) nil 9 2 (base-permission-row carols-see) nil)]
                [(kinds-of messages)
                 (select-keys (first (nth (first messages) 3)) [:standing :final-word :asks-rows?])]))
       [[[:entry :doc-1 9] [:answer (uuid 2) (record/answer-key :base (uuid 2))] [:numbers :microbatch :batch]]
        {:standing :final :final-word :permission-does-not-allow-acting :asks-rows? false}]]])))

(def ^:private reply-cases
  "The cases of step 2: the reads of a task, and its replies about an answer
  and about a place."
  (let [answer-key (record/answer-key :base (uuid 1))
        place ":base :doc-1 :title"
        asker (fn [turn to & {:as more}] (merge {:turn turn :to to} more))
        about (fn [k q] (assoc q :marks-place base-marks-place :k k))
        a-row (record/answer-row nil 900 (digest/digest good))
        f-row (fact-row base-writer (arrived-fact :doc-1 :title "a title") (uuid 800) 0 900)
        says (fn [replies] (mapv (fn [[to turn [reply]]] [to turn (:says reply)]) replies))
        shared (fn [replies] (mapv (fn [[_ turn [reply]]] [turn (:about reply) (:shared? reply)]) replies))
        about-place (fn [qs] (batch/replies {:about :place :key place :questions qs} f-row now 50 61))
        about-marked (fn [qs] (batch/replies {:about :place :key place :questions qs :since 1000} f-row now 50 61))]
    [["the reads of a task are of two kinds, one for each key asked for: an answer; and a place, which under a key that is not marked is read when an asker that does not own it wants it. What a task gathered that is no question gives no read"
      (fn [] (set (batch/reads-of
                   {:answer {answer-key [(asker 3 :doc-1)]}
                    :place {place [(about :title (asker 3 :doc-1 :wanted? true :own? true))]
                            ":base :doc-2 :title" [(about :title (asker 3 :doc-1 :wanted? true :own? false))]}
                    :entry {3 [{:turn 3}]}
                    :numbers {:batch {:position 0}}}
                   {base-marks-place nil})))
      #{{:about :answer :key answer-key :questions [(asker 3 :doc-1)]}
        {:about :place :key place :questions [(about :title (asker 3 :doc-1 :wanted? true :own? true))] :read? false}
        {:about :place :key ":base :doc-2 :title" :questions [(about :title (asker 3 :doc-1 :wanted? true :own? false))] :read? true}}]
     ["the marked-keys places that a task reads before it replies: the one of each layer that a question about a place names, once; none when no place was asked about"
      (fn [] [(batch/marks-places-asked
               {:place {place [(about :title (asker 3 :doc-1)) (about :title (asker 7 :doc-9))]
                        ":base :doc-2 :on" [(about :on (asker 3 :doc-1))]
                        ":team :doc-2 :on" [(assoc (asker 5 :doc-2) :marks-place ":team :team :softland.store/marked-keys" :k :on)]}})
              (batch/marks-places-asked {:answer {answer-key [(asker 3 :doc-1)]}})])
      [[base-marks-place ":team :team :softland.store/marked-keys"] []]]
     ["the reads of a task under a marked key: the read holds the key's stamp, and the place is read when an asker does not have it as its own, whether its fact names something to replace or not; a place under a key that the row does not mark is as before"
      (fn [] (let [asked {:place {":base :doc-1 :on" [(about :on (asker 3 :doc-1 :wanted? false :own? true))]
                                  ":base :doc-2 :on" [(about :on (asker 3 :doc-1 :wanted? false :own? false))]
                                  ":base :doc-3 :on" [(about :on (asker 3 :doc-1 :wanted? false :own? true))
                                                      (about :on (asker 7 :doc-9 :wanted? false :own? false))]
                                  ":base :doc-2 :title" [(about :title (asker 3 :doc-1 :wanted? false :own? false))]}}
                   reads (fn [marks-row] (into {} (map (juxt :key #(select-keys % [:read? :since])))
                                               (batch/reads-of asked {base-marks-place marks-row})))]
               [(reads (base-marks-row {:on 1000}))
                (reads (base-marks-row {:other 1000}))
                (reads nil)]))
      [{":base :doc-1 :on" {:read? false :since 1000}
        ":base :doc-2 :on" {:read? true :since 1000}
        ":base :doc-3 :on" {:read? true :since 1000}
        ":base :doc-2 :title" {:read? false}}
       {":base :doc-1 :on" {:read? false} ":base :doc-2 :on" {:read? false} ":base :doc-3 :on" {:read? false}
        ":base :doc-2 :title" {:read? false}}
       {":base :doc-1 :on" {:read? false} ":base :doc-2 :on" {:read? false} ":base :doc-3 :on" {:read? false}
        ":base :doc-2 :title" {:read? false}}]]
     ["the last stamp that a batch can hold, which a newly marked key gets: nothing for a batch with no base; else the gate's new last stamp, not below any stamp of the batch"
      (fn [] (let [base (stamp/pack now 0)]
               [(batch/batch-end nil {:carried nil :position 2} 4)
                (- (batch/batch-end base {:carried nil :position 2} 4) base)
                (= (batch/batch-end base {:carried nil :position 2} 4) (batch/last-stamp-of-batch base {:carried nil :position 2} 4))
                (<= (batch/stamp-of-turn base (batch/turn 2 4 3)) (batch/batch-end base {:carried nil :position 2} 4))]))
      [nil 11 true true]]
     ["check (a) in a batch: with no answer in the record the least turn has the act-id and each other is told that it is taken; the reply carries the clock, the base and the last stamp that the batch can hold"
      (fn [] (let [replies (batch/replies {:about :answer :key answer-key :questions [(asker 9 :doc-2) (asker 3 :doc-1) (asker 12 :doc-1)]}
                                          nil now 50 61)]
               [(says replies) (nth (nth (first replies) 2) 0)]))
      [[[:doc-2 9 :taken-in-this-batch] [:doc-1 3 :yours] [:doc-1 12 :taken-in-this-batch]]
       {:about :answer :says :taken-in-this-batch :clock now :base 50 :batch-end 61}]]
     ["check (a): with an answer in the record each asker is told that the act-id is used"
      (fn [] (says (batch/replies {:about :answer :key answer-key :questions [(asker 9 :doc-2) (asker 3 :doc-1)]} a-row now 50 61)))
      [[:doc-2 9 :used] [:doc-1 3 :used]]]
     ["under a key that is not marked: a place named by one act is not shared; the row, which holds the value's text, goes to an asker that wants it and does not own it"
      (fn [] [(mapv #(nth % 2) (about-place [(asker 3 :doc-9 :wanted? true :own? false)]))
              (mapv #(nth % 2) (about-place [(asker 3 :doc-1 :wanted? true :own? true)]))
              (mapv #(nth % 2) (about-place [(asker 3 :doc-9 :wanted? false :own? false)]))
              (:value f-row)])
      [[[{:about :place :key place :shared? false :row f-row}]]
       [[{:about :place :key place :shared? false}]]
       [[{:about :place :key place :shared? false}]]
       "\"a title\""]]
     ["under a key that is not marked: a place named by two acts is shared, unless each of them has it as its own"
      (fn [] [(shared (about-place [(asker 3 :doc-1 :own? true) (asker 7 :doc-1 :own? true)]))
              (shared (about-place [(asker 3 :doc-1 :own? true) (asker 7 :doc-9 :own? false)]))
              (shared (about-place [(asker 3 :doc-8 :own? false) (asker 7 :doc-9 :own? false)]))])
      [[[3 :place false] [7 :place false]] [[3 :place true] [7 :place true]] [[3 :place true] [7 :place true]]]]
     ["under a marked key: each asker is told the key's stamp; the row goes to each asker that does not have the place as its own, whether it wanted it or not; an asker that has the place as its own gets no row, and reads it at its turn"
      (fn [] [(mapv #(nth % 2) (about-marked [(asker 3 :doc-9 :wanted? false :own? false)]))
              (mapv #(nth % 2) (about-marked [(asker 3 :doc-9 :wanted? true :own? false)]))
              (mapv #(nth % 2) (about-marked [(asker 3 :doc-1 :wanted? false :own? true)]))
              (mapv #(nth % 2) (about-marked [(asker 3 :doc-1 :wanted? true :own? true)]))])
      [[[{:about :place :key place :shared? false :since 1000 :row f-row}]]
       [[{:about :place :key place :shared? false :since 1000 :row f-row}]]
       [[{:about :place :key place :shared? false :since 1000}]]
       [[{:about :place :key place :shared? false :since 1000}]]]]
     ["under a marked key: a place named by two acts is shared whenever two acts name it, also when each has it as its own, so both go to the meeting task; two questions of one act share nothing"
      (fn [] [(shared (about-marked [(asker 3 :doc-1 :own? true) (asker 7 :doc-1 :own? true)]))
              (shared (about-marked [(asker 3 :doc-1 :own? true) (asker 7 :doc-9 :own? false)]))
              (shared (about-marked [(asker 3 :doc-1 :own? true)]))])
      [[[3 :place true] [7 :place true]] [[3 :place true] [7 :place true]] [[3 :place false]]]]]))

(defn- item
  "An item of step 3 as the gate builds it: an act with its turn, told that
  its act-id is its own, standing, with nothing shared; and what `more`
  changes."
  [turn thing & {:as more}]
  (merge {:turn turn :thing thing :act (act-of thing) :act-id (:act-id thing) :to (batch/first-entity thing)
          :says :yours :standing :stands :asks-rows? true :shared? false :clock now :base (stamp/pack now 0)
          :permission-row (base-permission-row system-own) :place-rows {}}
         more))

(def ^:private meeting-cases
  "The cases of who goes to the meeting task: one for each rule, and the acts
  that stay."
  (let [on-doc-1 (fn [n k] (base-thing n :system system-own {:e :doc-1 :k k}))
        give (base-thing 5 :system system-own {:e :carol :k :softland.store/permission :v carols})
        depot-fact (base-thing 6 :system system-own {:e :base :k :softland.store/depot :v :microbatch})]
    [["an act that stands, on its own place, with nothing shared, is decided where it arrived"
      (fn [] (batch/who-meets [(item 3 (on-doc-1 1 :title))]))
      #{}]
     ["rule 1: an act with a fact under one of the five store keys whose row every task holds goes to the meeting task: a permission, a depot, who may see, what the layer builds on, the marked keys"
      (fn [] (batch/who-meets [(item 3 give) (item 7 depot-fact) (item 11 (on-doc-1 1 :title))
                               (item 15 (base-thing 7 :system system-own {:e :base :k :softland.store/who-may-see :v :members}))
                               (item 19 (base-thing 8 :system system-own {:e :base :k :softland.store/builds-on :v [:team]}))
                               (item 23 (base-thing 9 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on}}))
                               (item 27 (base-thing 10 :system system-own {:e :doc-9 :k :marked-keys} {:e :doc-9 :k :who-may-see}))]))
      #{3 7 15 19 23}]
     ["an act with a tool's own facts under the bare words :permission and :class stays: they are not the store's keys"
      (fn [] (batch/who-meets [(item 3 (base-thing 7 :system system-own {:e :doc-1 :k :permission} {:e :doc-1 :k :class}))]))
      #{}]
     ["rule 2: an act whose permission's place is empty goes"
      (fn [] (batch/who-meets [(item 3 (on-doc-1 1 :title) :standing :open) (item 7 (on-doc-1 2 :body))]))
      #{3}]
     ["rule 3: an act that was told that a row is shared goes"
      (fn [] (batch/who-meets [(item 3 (on-doc-1 1 :title) :shared? true) (item 7 (on-doc-1 2 :body))]))
      #{3}]
     ["rule 4: a later act that names an own place of an earlier act that goes, goes too; an earlier one stays; so does one on another place"
      (fn [] (batch/who-meets [(item 1 (on-doc-1 1 :title))
                               (item 5 (on-doc-1 2 :title) :shared? true)
                               (item 9 (on-doc-1 3 :title))
                               (item 13 (on-doc-1 4 :body))]))
      #{5 9}]
     ["rule 4 carries on: an act pulled along pulls a later act on another of its places"
      (fn [] (batch/who-meets [(item 5 (base-thing 2 :system system-own {:e :doc-1 :k :title}) :shared? true)
                               (item 9 (base-thing 3 :system system-own {:e :doc-1 :k :title} {:e :doc-1 :k :body}))
                               (item 13 (on-doc-1 4 :body))]))
      #{5 9 13}]
     ["the items are walked in the order of their turns, whatever the order given"
      (fn [] (batch/who-meets [(item 9 (on-doc-1 3 :title)) (item 5 (on-doc-1 2 :title) :shared? true) (item 1 (on-doc-1 1 :title))]))
      #{5 9}]
     ["an act that is not decided goes nowhere: a repeat, an act-id taken in this batch, a refusal of the declaration, a refusal for good"
      (fn [] (batch/who-meets [(item 1 (on-doc-1 1 :title) :says :used :shared? true)
                               (item 5 (on-doc-1 2 :title) :says :taken-in-this-batch :shared? true)
                               (item 9 (on-doc-1 3 :title) :refused :malformed :shared? true)
                               (item 13 (on-doc-1 4 :title) :standing :final :shared? true)
                               (item 17 (on-doc-1 5 :title))]))
      #{}]
     ["the items of a task: each entry with what its replies say, in the order of the turns, and whether it meets"
      (fn [] (let [entry (fn [turn n k] (dissoc (item turn (on-doc-1 n k)) :says :clock :base :shared? :place-rows))
                   items (batch/items-of-task
                          {9 [(entry 9 2 :body)] 3 [(entry 3 1 :title)]}
                          {3 [{:about :place :key ":base :doc-1 :title" :shared? true :row {:stamp 5}}
                              {:about :answer :says :yours :clock now :base 50 :batch-end 61}]
                           9 [{:about :answer :says :used :clock now :base 50 :batch-end 61}
                              {:about :place :key ":base :doc-1 :body" :shared? false :row {:stamp 7}}]})]
               (mapv #(select-keys % [:turn :says :clock :base :batch-end :shared? :place-rows :since-by-place :meets?]) items)))
      [{:turn 3 :says :yours :clock now :base 50 :batch-end 61 :shared? true :place-rows {":base :doc-1 :title" {:stamp 5}}
        :since-by-place {} :meets? true}
       {:turn 9 :says :used :clock now :base 50 :batch-end 61 :shared? false :place-rows {":base :doc-1 :body" {:stamp 7}}
        :since-by-place {} :meets? false}]]
     ["an item holds the stamp of each of its places whose key is marked, from the replies about places; the row of such a place when it was sent"
      (fn [] (let [entry (dissoc (item 3 (base-thing 1 :system system-own {:e :doc-1 :k :on} {:e :doc-2 :k :on} {:e :doc-2 :k :title}))
                                 :says :clock :base :shared? :place-rows)
                   [an-item] (batch/items-of-task
                              {3 [entry]}
                              {3 [{:about :answer :says :yours :clock now :base 50 :batch-end 61}
                                  {:about :place :key ":base :doc-1 :on" :shared? false :since 1000}
                                  {:about :place :key ":base :doc-2 :on" :shared? false :since 1000 :row {:stamp 5}}
                                  {:about :place :key ":base :doc-2 :title" :shared? false}]})]
               (select-keys an-item [:since-by-place :place-rows :meets?])))
      {:since-by-place {":base :doc-1 :on" 1000 ":base :doc-2 :on" 1000} :place-rows {":base :doc-2 :on" {:stamp 5}} :meets? false}]]))

(def ^:private deciding-cases
  "The cases of step 3 and of the rows of a decision: the rows that a task
  reads for an act at its turn, the home of each row, and what a task writes
  at once and what it sends."
  (let [stamp (stamp/pack now 0)
        value (base-thing 1 :system system-own (value-at :doc-1 :title "a") (value-at :doc-2 :title "b"))
        replacing (base-thing 2 :system system-own
                              (assoc (value-at :doc-1 :title "a") :replaces [(base-id 800) 0])
                              (assoc (value-at :doc-2 :title "b") :replaces [(base-id 800) 1])
                              {:e :doc-1 :k :body})
        which-homes (fn [rows] (mapv (juxt :which :home :every-task?) rows))
        on-marked (base-thing 4 :system system-own (value-at :doc-1 :on :c1) (value-at :doc-2 :on :c1) {:e :doc-1 :k :body})
        marked-places {":base :doc-1 :on" 1000 ":base :doc-2 :on" 1000}
        before (fn [e v] (fact-row base-writer (arrived-fact e :on v) (uuid 800) 0 2000))
        parcels-of (fn [result] (mapv (fn [[route kind payload]] [route kind (mapv (fn [x] (if (vector? x) (first x) :an-act)) payload)])
                                      (:parcels result)))
        decided-value (batch/decide-or-send (item 5 value) {})
        far-ahead (assoc value :based-on {[(base-id 800) 0] (stamp/pack (+ now 60000) 0)})]
    [["the rows that a task reads for an act at its turn: its own places whose fact names something to replace"
      (fn [] [(batch/own-places-wanted (item 5 replacing))
              (batch/own-places-wanted (item 5 replacing :says :used))
              (batch/own-places-wanted (item 5 replacing :standing :final))
              (batch/own-places-wanted (item 5 replacing :asks-rows? false))
              (batch/own-places-wanted (item 5 (assoc replacing :based-on {[(base-id 800) 0] (stamp/pack (+ now 60000) 0)})))])
      [[":base :doc-1 :title"] [] [] [] []]]
     ["and its own places whose key is marked, whether the fact names something to replace or not: the row before gives the entry to take out; not a place on another entity, whose row came in the reply; none when no key of the act is marked"
      (fn [] [(batch/own-places-wanted (item 5 on-marked :since-by-place marked-places))
              (batch/own-places-wanted (item 5 on-marked :since-by-place {}))
              (batch/own-places-wanted (item 5 on-marked))
              (batch/own-places-wanted (item 5 on-marked :since-by-place marked-places :standing :final))])
      [[":base :doc-1 :on"] [] [] []]]
     ["the home of each row of an act with two values: each fact on its entity, with the text of its value; the answer on the act's uuid"
      (fn [] (let [act (act-of value)
                   rows (batch/routed (base-id 1) act (decision/writes (base-id 1) act nil stamp (digest/digest value) {}))]
               [(which-homes rows) (mapv (comp :value :row) (filter #(= :fact (:which %)) rows))]))
      [[[:fact :doc-1 false] [:fact :doc-2 false] [:answer (uuid 1) false]]
       ["\"a\"" "\"b\""]]]
     ["the home of each row: a fact under each of the five store keys, on every task; a refusal, the answer alone; the last stamp is left out"
      (fn [] (let [thing (base-thing 3 :system system-own {:e :carol :k :softland.store/permission :v carols})
                   act (act-of thing)
                   settings (base-thing 3 :system system-own
                                        {:e :base :k :softland.store/depot :v :microbatch}
                                        {:e :base :k :softland.store/who-may-see :v :members}
                                        {:e :base :k :softland.store/builds-on :v [:team]}
                                        {:e :base :k :softland.store/marked-keys :v #{:on}})
                   settings-act (act-of settings)]
               [(which-homes (batch/routed (base-id 3) act (decision/writes (base-id 3) act nil stamp (digest/digest thing) {})))
                (which-homes (batch/routed (base-id 3) act (decision/writes (base-id 3) act :layer-not-made stamp (digest/digest thing) {})))
                (which-homes (batch/routed (base-id 3) nil (decision/writes (base-id 3) nil :malformed stamp (digest/digest thing) {})))
                (which-homes (batch/routed (base-id 3) settings-act
                                           (decision/writes (base-id 3) settings-act nil stamp (digest/digest settings)
                                                            {:place-rows {} :level-from 77})))]))
      [[[:fact :carol true] [:answer (uuid 3) false]]
       [[:answer (uuid 3) false]]
       [[:answer (uuid 3) false]]
       [[:fact :base true] [:fact :base true] [:fact :base true] [:fact :base true] [:answer (uuid 3) false]]]]
     ["the home of a tool's own fact under the bare word :permission or :class is its entity's task alone"
      (fn [] (let [thing (base-thing 7 :system system-own {:e :doc-1 :k :permission} {:e :doc-2 :k :class})
                   act (act-of thing)]
               (which-homes (batch/routed (base-id 7) act (decision/writes (base-id 7) act nil stamp (digest/digest thing) {})))))
      [[:fact :doc-1 false] [:fact :doc-2 false] [:answer (uuid 7) false]]]
     ["the home of a row of the facts by value is the value digest that its key holds, so the run of one value is on one task whatever the entities are; a row that is nil, a key to take out, has the same home"
      (fn [] (let [act (act-of on-marked)
                   rows (batch/routed (base-id 4) act
                                      (decision/writes (base-id 4) act nil stamp (digest/digest on-marked)
                                                       {:since-by-place marked-places
                                                        :place-rows {":base :doc-1 :on" (before :doc-1 :c0) ":base :doc-2 :on" nil}}))]
               (mapv (juxt :which :home :every-task? (comp some? :row) :key) (filter #(= :by-value (:which %)) rows))))
      [[:by-value (digest-of-text ":c0") false false (record/by-value-key :base :on 1000 (digest-of-text ":c0") :doc-1)]
       [:by-value (digest-of-text ":c1") false true (record/by-value-key :base :on 1000 (digest-of-text ":c1") :doc-1)]
       [:by-value (digest-of-text ":c1") false true (record/by-value-key :base :on 1000 (digest-of-text ":c1") :doc-2)]]]
     ["the rows that the meeting task reads from its own copy: the place of each permission fact; of a depot fact, a setting or a list that names something to replace; of a marks fact always; none for a tool's own fact under a bare word; none that the pass wrote"
      (fn [] (let [sent (fn [thing] {:act (act-of thing) :permission-row (base-permission-row system-own)})
                   replaces [(base-id 800) 0]
                   marking (sent (base-thing 10 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on}}))]
               [(batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 8 :system system-own
                                                       {:e :carol :k :softland.store/permission :v carols}
                                                       {:e :base :k :softland.store/depot :v :microbatch :replaces replaces}
                                                       {:e :doc-1 :k :permission}
                                                       {:e :doc-1 :k :class :replaces replaces})))
                (batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 9 :system system-own {:e :base :k :softland.store/depot :v :microbatch})))
                (batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 9 :system system-own
                                                       {:e :base :k :softland.store/who-may-see :v :members}
                                                       {:e :base :k :softland.store/builds-on :v [:team]})))
                (batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 9 :system system-own
                                                       {:e :base :k :softland.store/who-may-see :v :members :replaces replaces}
                                                       {:e :base :k :softland.store/builds-on :v [:team] :replaces replaces})))
                (batch/meeting-reads (batch/new-pass) marking)
                (batch/meeting-reads (assoc-in (batch/new-pass) [:facts base-marks-place] {:stamp 1}) marking)
                (batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 11 :system system-own {:e :carol :k :softland.store/permission :v carols}
                                                       {:e :doc-1 :k :on})))]))
      [[":base :carol :softland.store/permission" ":base :base :softland.store/depot"] []
       []
       [":base :base :softland.store/who-may-see" ":base :base :softland.store/builds-on"]
       [base-marks-place]
       []
       [":base :carol :softland.store/permission"]]]
     ["an act with facts under a marked key, decided where it arrived: the fact on its own place is written at once; the entry of the value before goes out and the new entries come in, each as a parcel to the task of its value digest"
      (fn [] (let [result (batch/decide-or-send (item 5 on-marked :since-by-place marked-places :batch-end 61
                                                      :place-rows {":base :doc-2 :on" nil})
                                                {":base :doc-1 :on" (before :doc-1 :c0)})]
               [(mapv first (:now result))
                (mapv (fn [[route kind [[which _ row]]]] [route kind which (some? row)])
                      (filter (fn [[_ _ [[which]]]] (= :by-value which)) (:parcels result)))]))
      [[":base :doc-1 :on" ":base :doc-1 :body"]
       [[(digest-of-text ":c0") :rows :by-value false]
        [(digest-of-text ":c1") :rows :by-value true]
        [(digest-of-text ":c1") :rows :by-value true]]]]
     ["an act decided where it arrived: the fact on its own place is written at once, with the text of its value; each other row is a parcel to its home"
      (fn [] [(mapv (fn [[k row]] [k (:value row)]) (:now decided-value)) (parcels-of decided-value)])
      [[[":base :doc-1 :title" "\"a\""]]
       [[:doc-2 :rows [:fact]] [(uuid 1) :rows [:answer]]]]]
     ["its rows hold one stamp, the base plus its turn"
      (fn [] (let [stamps (concat (map (comp :stamp second) (:now decided-value))
                                  (keep (fn [[_ _ [[which _ row]]]] (when (#{:fact :answer} which) (:stamp row))) (:parcels decided-value)))]
               [(count stamps) (set stamps)]))
      [3 #{(+ stamp 5)}]]
     ["a repeat, and a thing under an act-id that an earlier turn of the batch took, write nothing and send nothing"
      (fn [] [(batch/decide-or-send (item 5 value :says :used) {}) (batch/decide-or-send (item 5 value :says :taken-in-this-batch) {})])
      [{:now [] :parcels []} {:now [] :parcels []}]]
     ["a thing that the declaration refused sends its answer, with its word, to the task of its uuid"
      (fn [] (let [result (batch/decide-or-send {:turn 5 :act-id (base-id 4) :to (uuid 4) :refused :value-too-large :digest (digest/digest good)
                                                 :says :yours :clock now :base stamp}
                                                {})
                   [route kind [[which k row]]] (first (:parcels result))]
               [(:now result) (count (:parcels result)) route kind which k (:refused row) (:stamp row)]))
      [[] 1 (uuid 4) :rows :answer (record/answer-key :base (uuid 4)) :value-too-large (+ stamp 5)]]
     ["an act refused for good in step 1 sends its answer with that word; with a carried stamp too far ahead as well, the word of the 10-second rule, which comes first"
      (fn [] (let [word-of (fn [thing] (let [[_ _ [[_ _ row]]] (first (:parcels (batch/decide-or-send
                                                                                  (item 5 thing :standing :final :final-word :who-not-the-permissions-holder)
                                                                                  {})))]
                                         (:refused row)))]
               [(word-of value) (word-of far-ahead)]))
      [:who-not-the-permissions-holder :carried-stamp-too-far-ahead]]
     ["a holder of see who sends a value: her act is refused where it arrived, with the word of step 1, under its own act-id; one parcel, the answer, and no row of a fact"
      (fn [] (let [thing (base-thing 6 :carol carols-see (value-at :doc-1 :title "a"))
                   result (batch/decide-or-send (item 5 thing :standing :final :final-word :permission-does-not-allow-acting
                                                      :asks-rows? false :permission-row (base-permission-row carols-see))
                                                {})
                   [route kind [[which k row]]] (first (:parcels result))]
               [(:now result) (count (:parcels result)) route kind which k (:refused row)]))
      [[] 1 (uuid 6) :rows :answer (record/answer-key :base (uuid 6)) :permission-does-not-allow-acting]]
     ["an act that meets is sent whole to its layer's task, with its digest, the clock, the base, the last stamp that the batch can hold, its rows and the stamp of each of its places whose key is marked, and writes nothing here"
      (fn [] (let [result (batch/decide-or-send (item 5 replacing :meets? true :place-rows {":base :doc-2 :title" {:stamp 1}}
                                                      :since-by-place {":base :doc-1 :title" 1000} :batch-end 61)
                                                {":base :doc-1 :title" {:stamp 2}})
                   [route kind [sent]] (first (:parcels result))]
               [(:now result) (count (:parcels result)) route kind (:place-rows sent)
                (select-keys sent [:turn :act-id :clock :base :batch-end :since-by-place])
                (= (vec (:digest sent)) (vec (digest/digest replacing)))
                (contains? sent :thing)
                (sort (keys sent))]))
      [[] 1 :base :meet {":base :doc-2 :title" {:stamp 1} ":base :doc-1 :title" {:stamp 2}}
       {:turn 5 :act-id (base-id 2) :clock now :base stamp :batch-end 61 :since-by-place {":base :doc-1 :title" 1000}} true false
       [:act :act-id :base :batch-end :clock :depot-row :digest :permission-row :place-rows :since-by-place :turn]]]
     ["the acts sent to a meeting task are taken in the order of their turns"
      (fn [] (mapv :turn (batch/acts-that-meet {:meet [{:turn 9} {:turn 3} {:turn 5}]})))
      [3 5 9]]
     ["a new pass holds no fact row and nothing to send"
      (fn [] (batch/new-pass))
      {:facts {} :out []}]
     ["a pass: of two acts that write one place, the second is decided with the row that the first wrote, so a second act that names the fact from before the pass is stale; each row that an act wrote is laid over the pass and added to what it sends"
      (fn [] (let [sent (fn [n turn & facts]
                          (let [thing (apply base-thing n :system system-own facts)]
                            {:turn turn :act-id (base-id n) :act (act-of thing) :digest (digest/digest thing)
                             :clock now :base stamp :batch-end 61 :since-by-place {}
                             :permission-row (base-permission-row system-own) :depot-row nil
                             :place-rows {":base :doc-1 :title" (fact-row base-writer (arrived-fact :doc-1 :title "before") (uuid 800) 0 2000)}}))
                   replaces [(base-id 800) 0]
                   one (batch/decide-next (batch/new-pass) (sent 1 3 (assoc (value-at :doc-1 :title "one") :replaces replaces)) {} nil)
                   two (batch/decide-next one (sent 2 7 (assoc (value-at :doc-1 :title "two") :replaces replaces)) {} nil)]
               [(mapv (juxt :which (comp :refused :row)) (:out two))
                (:value (get (:facts two) ":base :doc-1 :title"))
                (:by-act (get (:facts two) ":base :doc-1 :title"))]))
      [[[:fact nil] [:answer nil] [:answer :replaced-fact-not-current]]
       "\"one\""
       (uuid 1)]]
     ["a pass sends one row for a kind and key, the last; a fact's row and a row of the facts by value under one key are two rows"
      (fn [] (let [rows (batch/rows-to-send {:out [{:which :fact :key "p" :row 1} {:which :answer :key "a" :row 4}
                                                   {:which :fact :key "p" :row 5} {:which :by-value :key "p" :row 6}]})]
               [(count rows) (set (map (juxt :which :key :row) rows))]))
      [3 #{[:answer "a" 4] [:fact "p" 5] [:by-value "p" 6]}]]
     ["for a key of the facts by value the last row of a pass can be a removal: an entry that an earlier act put and a later one took out is sent as nil, and one that was taken out and put again as the row"
      (fn [] (let [rows (batch/rows-to-send {:out [{:which :by-value :key "v0" :row nil} {:which :by-value :key "t" :row 1}
                                                   {:which :by-value :key "t" :row nil} {:which :by-value :key "u" :row 2}
                                                   {:which :by-value :key "w" :row nil} {:which :by-value :key "w" :row 3}]})]
               [(count rows) (set (map (juxt :which :key :row) rows))]))
      [4 #{[:by-value "v0" nil] [:by-value "t" nil] [:by-value "u" 2] [:by-value "w" 3]}]]]))

(def ^:private routing-cases
  "The cases of what the microbatch gate adds to the record's rows: the
  routing id, the head of the read by routing id, and a closed row."
  (let [u (uuid 5)]
    [["the routing id of an act-id: the layer of a streaming one, the uuid of a microbatch one; nothing for what is no act-id"
      (fn [] [(record/routing-id [:alice :streaming u]) (record/routing-id [:base :microbatch u]) (record/routing-id [:base :microbatch])
              (record/routing-id 5) (record/routing-id nil) (record/routing-id (list :base :microbatch u))])
      [:alice u nil nil nil nil]]
     ["the head of the read by act-id, with the routing id: what `answer-read` gives for the act-id's own layer; nothing for another routing id"
      (fn [] [(record/answer-read-by-routing-id :alice [:alice :streaming u])
              (record/answer-read-by-routing-id u [:base :microbatch u])
              (record/answer-read-by-routing-id :base [:base :microbatch u])
              (record/answer-read-by-routing-id u [:alice :streaming u])
              (record/answer-read-by-routing-id (uuid 6) [:base :microbatch u])
              (record/answer-read-by-routing-id nil nil)
              (record/answer-read-by-routing-id 5 5)
              (record/answer-read-by-routing-id (long-id 257) [(long-id 257) :streaming u])])
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":base 00000000-0000-0000-0000-000000000005"]
       nil nil nil nil nil nil]]
     ["a row is of a closed batch when its stamp is not later than the task's closed-through"
      (fn [] [(record/closed? {:stamp 5} 5) (record/closed? {:stamp 5} 9) (record/closed? {:stamp 5} 4)
              (record/closed? {:stamp 5} nil) (record/closed? nil 9)])
      [true true false false false]]]))

;; ------------------------------------- a batch run in memory, and one at a time

(def ^:private sim-tasks "The number of tasks of the batch that the proof runs in memory." 4)

(defn- task-of
  "The task of a route in the proof's batch: a fixed function of the route's
  printed text."
  [route]
  (mod (hash (str route)) sim-tasks))

(defn- entity-on
  "The nth keyword of the form :e<i> whose task is the one given."
  [task n]
  (nth (filter #(= task (task-of %)) (map #(keyword (str "e" %)) (range))) n))

(defn- gathered
  "Takes messages [task path payload], in the order in which they arrive.
  Gives what each task gathers, as the gate's aggregations do: by task, then
  by the path, the payloads put together with `batch/gather`."
  [messages]
  (reduce (fn [m [task path payload]] (update-in m (into [task] path) batch/gather payload)) {} messages))

(defn- write-row
  "Writes one row of a decision on a task of the record in memory. A row of
  the facts by value that is nil takes its key out."
  [record task [which k row]]
  (case which
    :fact (assoc-in record [:facts task k] row)
    :by-value (if row
                (assoc-in record [:by-value task k] row)
                (update-in record [:by-value task] dissoc k))
    :answer (assoc-in record [:answers task k] row)
    record))

(defn- turns-of
  "Takes the things of a batch in the depot's order. Gives [turn partition
  position thing] of each: its partition is the task of its first entity,
  its position its number among the things of that partition."
  [things]
  (for [[partition on-partition] (group-by #(task-of (batch/first-entity %)) things)
        [position thing] (map-indexed vector on-partition)]
    [(batch/turn position sim-tasks partition) partition position thing]))

(defn- run-batch
  "Runs one batch as the microbatch gate does, in memory, with the pure
  functions of `softland.store.gate.batch` and none of Rama: the four steps, each
  task's rows in its own map, each message gathered on the task of its
  route. `shuffled` puts in another order what Rama delivers in no order:
  the messages of a step, and the tasks. So the rows that a task is sent in
  step 3 are written in step 4 in the order in which they arrived. Takes
  the record, {:answers :facts :by-value}, each a map from task to key to
  row, and :last-stamp; the other gate's facts, a map from key to row; the
  things, in the depot's order; and the clock. Gives the record after the
  batch, with :base. As in the gate, each task reads the layer's marked
  keys from its own copy in step 2, before any row of the batch is
  written."
  [record other-facts things clock shuffled]
  (let [questions (gathered
                   (shuffled
                    (for [[turn partition position thing] (turns-of things)
                          :when (nil? (declaration/stray-reason :microbatch thing))
                          :let [{act :ok reason :refuse} (declaration/check thing)
                                facts (get-in record [:facts partition])
                                permission-row (when act (get facts (decision/permission-place act)))
                                depot-row (when (and act (nil? permission-row)) (get facts (decision/depot-place act)))]
                          [route kind k payload] (batch/arrive thing act reason turn position permission-row depot-row)]
                      [(task-of route) [kind k] payload])))
        numbers (get-in questions [(task-of batch/gates-depot) :numbers :batch])
        base (batch/base clock (:last-stamp record) numbers)
        batch-end (batch/batch-end base numbers sim-tasks)
        replies (gathered
                 (shuffled
                  (for [task (range sim-tasks)
                        :let [asked (get questions task)
                              marks-rows (into {} (map (fn [k] [k (get-in record [:facts task k])])) (batch/marks-places-asked asked))]
                        read (batch/reads-of asked marks-rows)
                        :let [found (case (:about read)
                                      :answer (get-in record [:answers task (:key read)])
                                      :place (when (:read? read) (get-in record [:facts task (:key read)])))]
                        [route turn reply] (batch/replies read found clock base batch-end)]
                    [(task-of route) [turn] reply])))
        [record parcels]
        (reduce (fn [[record parcels] task]
                  (reduce (fn [[record parcels] an-item]
                            (let [own-rows (into {} (map (fn [k] [k (get-in record [:facts task k])])) (batch/own-places-wanted an-item))
                                  result (batch/decide-or-send an-item own-rows)]
                              [(reduce (fn [record [k row]] (assoc-in record [:facts task k] row)) record (:now result))
                               (into parcels (map (fn [[route kind payload]] [(task-of route) [kind] payload])) (:parcels result))]))
                          [record parcels]
                          (batch/items-of-task (get-in questions [task :entry]) (get replies task))))
                [record []]
                (shuffled (range sim-tasks)))
        sent (gathered (shuffled parcels))
        [record rows-out]
        (reduce (fn [[record rows-out] task]
                  (let [record (reduce (fn [record row] (write-row record task row)) record (:rows (get sent task)))
                        pass (reduce (fn [pass met]
                                       (let [local-rows (into {} (map (fn [k] [k (get-in record [:facts task k])])) (batch/meeting-reads pass met))
                                             other-depot-row (when (batch/reads-other-record? pass met)
                                                               (get other-facts (batch/depot-place-of met)))]
                                         (batch/decide-next pass met local-rows other-depot-row)))
                                     (batch/new-pass)
                                     (batch/acts-that-meet (get sent task)))]
                    [record (into rows-out (batch/rows-to-send pass))]))
                [record []]
                (shuffled (range sim-tasks)))
        record (reduce (fn [record {:keys [every-task? home which key row]}]
                         (reduce (fn [record task] (write-row record task [which key row]))
                                 record
                                 (if every-task? (range sim-tasks) [(task-of home)])))
                       record
                       (shuffled rows-out))]
    (assoc record
           :base base
           :last-stamp (if base (batch/last-stamp-of-batch base numbers sim-tasks) (:last-stamp record)))))

(defn- one-at-a-time
  "Decides the things of a batch one at a time in the order of their turns,
  against one flat record, with the checks of `softland.store.gate.decision` in
  their order and the depot :microbatch: what the decisions of a batch must be.
  Takes the flat record, {:answers :facts :by-value}, each a map from key to
  row; the other gate's facts; the things in the depot's order; the clock;
  and the batch's base. Gives the flat record after.

  One thing is not taken in the order of the turns, here as in the gate: a
  mark of a key holds from the batch after. Each act is decided with the
  marked keys of its layer as the record held them before the batch, and a
  key that an act newly marks gets the last stamp that the batch can hold.
  The row before of a place, and the row before of the marks, are the
  record's at the act's turn."
  [flat other-facts things clock base]
  (let [marks-as-the-batch-began (:facts flat)
        greatest-position (reduce max 0 (keep (fn [[_ _ position thing]]
                                                (when-not (declaration/stray-reason :microbatch thing) position))
                                              (turns-of things)))
        batch-end (when base
                    (batch/last-stamp-of-batch base {:position greatest-position} sim-tasks))]
   (reduce
   (fn [flat [turn _ _ thing]]
     (let [[layer _ act-uuid :as act-id] (when-not (declaration/stray-reason :microbatch thing) (:act-id thing))]
       (if (or (nil? act-id) (get-in flat [:answers (record/answer-key layer act-uuid)]))
         flat
         (let [{act :ok reason :refuse} (declaration/check thing)
               early (decision/refused-early clock act reason)
               permission-row (when-not early (get-in flat [:facts (decision/permission-place act)]))
               depot-row (when (and (not early) (nil? permission-row)) (get-in flat [:facts (decision/depot-place act)]))
               other-holds? (boolean (and (not early) (nil? permission-row) (nil? depot-row)
                                          (not= :microbatch (first-act/depot-at-making layer))
                                          (get other-facts (decision/depot-place act))))
               refused (or early
                           (decision/standing :microbatch act permission-row depot-row other-holds?)
                           (decision/places :microbatch act
                                            (decision/makes-the-layer? :microbatch act permission-row depot-row other-holds?)
                                            (:facts flat)))
               in-hand (when-not refused
                         {:since-by-place (decision/since-by-place act (get marks-as-the-batch-began (decision/marked-keys-place act)))
                          :place-rows (:facts flat)
                          :level-from batch-end})]
           (reduce (fn [flat [which k row]]
                     (case which
                       :fact (assoc-in flat [:facts k] row)
                       :by-value (if row (assoc-in flat [:by-value k] row) (update flat :by-value dissoc k))
                       :answer (assoc-in flat [:answers k] row)
                       flat))
                   flat
                   (decision/writes act-id act refused (batch/stamp-of-turn base turn) (digest/digest thing) (or in-hand {})))))))
   flat
   (sort-by first (turns-of things)))))

(defn- differences
  "Compares a record after `run-batch` with the flat record after
  `one-at-a-time`. Gives a list of what differs, empty when nothing does.
  What must be the same: each answer, on one task; each fact row, on one
  task, or on every task for a row under one of the five store keys, whose
  row every task holds; each row of the facts by value, on one task, the
  task of the value digest that its key holds, and no key there that the
  flat record does not hold. A row is compared in each field, the text of
  its value among them."
  [record flat]
  (let [by-key (fn [table] (reduce (fn [m [task rows]] (reduce (fn [m [k row]] (update m k (fnil conj []) [task row])) m rows)) {} (get record table)))
        rows-of (fn [table] (into {} (map (fn [[k placed]] [k (mapv second placed)])) (by-key table)))
        answers (rows-of :answers)
        facts (rows-of :facts)
        by-value (by-key :by-value)
        same? (fn [a b] (= (comparable a) (comparable b)))]
    (concat
     (when (not= (set (keys answers)) (set (keys (:answers flat)))) [[:answers-under-other-keys]])
     (for [[k row] (:answers flat) :when (not (and (= 1 (count (get answers k))) (same? row (first (get answers k)))))]
       [:answer k (:refused row) (mapv :refused (get answers k))])
     (when (not= (set (keys facts)) (set (keys (:facts flat)))) [[:facts-under-other-keys]])
     (for [[k row] (:facts flat)
           :let [rows (get facts k)
                 everywhere? (batch/on-every-task? (nth (record/place-of-key k) 2))]
           :when (not (and (= (count rows) (if everywhere? sim-tasks 1)) (every? #(same? row %) rows)))]
       [:fact k (count rows)])
     (when (not= (set (keys by-value)) (set (keys (:by-value flat)))) [[:facts-by-value-under-other-keys]])
     (for [[k row] (:by-value flat)
           :let [placed (get by-value k)]
           :when (not (and (= 1 (count placed))
                           (= (task-of (record/by-value-route k)) (first (first placed)))
                           (same? row (second (first placed)))))]
       [:fact-by-value k (mapv first placed)]))))

(defn- flat-of
  "Takes rows [table route key row] of a record before a batch. Gives the
  flat record."
  [rows]
  (reduce (fn [flat [table _ k row]] (assoc-in flat [table k] row)) {:answers {} :facts {} :by-value {}} rows))

(defn- record-of
  "Takes rows [table route key row] of a record before a batch. Gives the
  record by task: each row on the task of its route, or on every task for
  the route :every-task."
  [rows last-stamp]
  (reduce (fn [record [table route k row]]
            (reduce (fn [record task] (assoc-in record [table task k] row))
                    record
                    (if (= :every-task route) (range sim-tasks) [(task-of route)])))
          {:answers {} :facts {} :by-value {} :last-stamp last-stamp}
          rows))

(def ^:private batch-fixture
  "A batch of things into a base that is made, and the record before it. The
  things are in the depot's order. Among them: two acts that replace one
  fact from two tasks; two that write one place and name nothing; a give and
  an act under it, in both orders; a value alone on its entity; a repeat;
  one act-id under two things; an act for a layer of the other gate, made
  and not made; a plain value under a plain key; strays; acts with no
  standing; an act with values on three tasks accepted, and one refused; an
  act that writes a place and names nothing, beside one that replaces the
  fact that was there before the batch; a thing that the declaration
  refuses."
  (let [[p1 p2 p3 _ q2 q3 r1 r2 r3] (for [n (range 3) task [1 2 3]] (entity-on task n))
        [z1 z2] [(entity-on 0 0) (entity-on 0 1)]
        before (stamp/pack (- now 5000) 0)
        answered (base-thing 801 :system system-own {:e p1 :k :repeat})
        text-on (fn [e index] (value-at e :text (str "text " index)))]
    {:rows [[:facts :every-task ":base :base :softland.store/depot" (fact-row base-writer (arrived-fact :base :softland.store/depot :microbatch) (uuid 800) 0 before)]
            [:facts :every-task ":base :system :softland.store/permission" (fact-row base-writer (arrived-fact :system :softland.store/permission system-own) (uuid 800) 1 before)]
            [:facts p1 (record/place-key :base p1 :title) (fact-row base-writer (arrived-fact p1 :title "the fact before") (uuid 802) 0 before)]
            [:facts p2 (record/place-key :base p2 :old) (fact-row base-writer (arrived-fact p2 :old) (uuid 803) 0 before)]
            [:answers (uuid 801) (record/answer-key :base (uuid 801)) (record/answer-row nil before (digest/digest answered))]]
     :other-facts {":alice :alice :softland.store/depot" (fact-row writer (arrived-fact :alice :softland.store/depot :streaming) (uuid 90) 0 before)}
     :last-stamp before
     :things
     [;; two acts that replace one fact, arriving on two tasks
      (base-thing 1 :system system-own (assoc (value-at p1 :title "a") :replaces [(base-id 802) 0]))
      (base-thing 2 :system system-own {:e p2 :k :note} (assoc (value-at p1 :title "b") :replaces [(base-id 802) 0]))
      ;; two acts that write one place and name nothing
      (base-thing 3 :system system-own {:e p3 :k :mark})
      (base-thing 4 :system system-own {:e z1 :k :note} {:e p3 :k :mark})
      ;; a give and an act under it; an act and then the give
      (base-thing 5 :system system-own {:e :carol :k :softland.store/permission :v carols})
      (base-thing 6 :carol carols {:e :carol :k :note})
      (base-thing 7 :dave daves-in-base {:e :dave :k :note})
      (base-thing 8 :system system-own {:e :dave :k :softland.store/permission :v daves-in-base})
      ;; a value alone on its entity
      (base-thing 9 :system system-own (text-on z2 2))
      ;; a repeat, and one act-id under two things
      answered
      (base-thing 14 :system system-own {:e p1 :k :twice})
      (base-thing 14 :system system-own {:e p2 :k :twice})
      ;; an act for a layer of the other gate, made and not made
      {:act-id [:alice :microbatch (uuid 15)] :depot :microbatch :version 1 :who :alice :layer :alice
       :permission alice-own :session :s1 :facts [{:e p1 :k :stranger}]}
      {:act-id [:nobody :microbatch (uuid 16)] :depot :microbatch :version 1 :who :alice :layer :nobody
       :permission [:alice :nobody :nobody] :session :s1 :facts [{:e p2 :k :stranger}]}
      ;; a plain value under a plain key, and two strays
      (base-thing 17 :system system-own {:e p1 :k :plain :v "a plain value"})
      5
      {:act-id [:base :streaming (uuid 18)] :depot :streaming :layer :base :facts [{:e p1 :k :stray}]}
      ;; no standing: with a stamp too far ahead, with a permission that no act gives, by a who that is not the holder
      (assoc (base-thing 19 :mallory carols {:e p3 :k :m}) :based-on {[(base-id 802) 0] (stamp/pack (+ now 60000) 0)})
      (base-thing 20 :mallory mallorys {:e p3 :k :m})
      (base-thing 21 :mallory system-own {:e p3 :k :m})
      ;; an act with values on three tasks, accepted; and one refused whole
      (base-thing 22 :system system-own (text-on r1 4) (text-on r2 5) (text-on r3 6))
      (base-thing 23 :system system-own (text-on z1 7) (text-on q2 8) (assoc (text-on q3 9) :replaces [(base-id 999) 0]))
      ;; one act writes a place and names nothing; another replaces the fact that was there before the batch
      (base-thing 25 :system system-own {:e p2 :k :old})
      (base-thing 26 :system system-own {:e z2 :k :note} {:e p2 :k :old :replaces [(base-id 803) 0]})
      ;; things that the declaration refuses: a key of the store's namespace that the store does not know, and a value over its bound
      (base-thing 27 :system system-own {:e p1 :k :softland.store/lock-request :v 2})
      (base-thing 28 :system system-own (value-at p2 :large (text-of-bytes 65537)))]}))

(def ^:private making-fixture
  "A batch into a base that is not made: an act before the making, two making
  acts with their facts in the two orders, and acts after."
  {:rows []
   :other-facts {}
   :last-stamp nil
   :things [(base-thing 30 :system system-own {:e :base :k :note})
            (base-thing 31 :system system-own {:e :base :k :softland.store/depot :v :microbatch} {:e :system :k :softland.store/permission :v system-own})
            (base-thing 32 :system system-own {:e :system :k :softland.store/permission :v system-own} {:e :base :k :softland.store/depot :v :microbatch})
            (base-thing 33 :system system-own {:e :base :k :note-2})
            (base-thing 34 :system system-own {:e :carol :k :softland.store/permission :v carols})
            (base-thing 35 :carol carols {:e :carol :k :note})
            (base-thing 36 :carol carols (value-at :carol :text "a value in a base made in this batch"))]})

(def ^:private marks-fixture
  "A batch into a base that marks two keys, :on and :gone, and the record
  before it. The things are in the depot's order. Among them: two acts on
  one place under a marked key, each with the place as its own; the same
  value written again; a retract of a fact from before the mark, and one of
  a fact since it; an act whose marked place is on another task and holds a
  value since the mark, which comes to hold the value of another place; a fact that names what it replaces; a
  write of the marks that takes one key off and marks a new one, a fact
  under the new key and a fact under the key that was taken off, a second
  write of the marks, and a stale fact under a marked key; two writes of who may see the layer from two
  tasks; two writes of what the layer builds on that name the same fact to
  replace; a holder of see who acts, who names her permission without its
  slot, and who gives; a retract of the marks; marks on another entity."
  (let [[p1 p2 p3 q1 q2 q3 r1 r2 r3] (for [n (range 3) task [1 2 3]] (entity-on task n))
        z1 (entity-on 0 0)
        since (stamp/pack (- now 9000) 0)
        before (stamp/pack (- now 5000) 0)
        older (stamp/pack (- now 20000) 0)
        fact-before (fn [e k v n stamp] (fact-row base-writer (arrived-fact e k v) (uuid n) 0 stamp))
        entry-before (fn [e k v n]
                       [:by-value (record/value-digest-of v) (record/by-value-key :base k since (record/value-digest-of v) e)
                        (by-value-row base-writer (uuid n) 0 before)])
        setting (fn [k v n] (fact-row base-writer (arrived-fact :base k v) (uuid n) 0 before))]
    {:since since
     :entities {:p1 p1 :p2 p2 :p3 p3 :q1 q1 :q2 q2 :q3 q3 :r1 r1 :r2 r2 :r3 r3}
     :rows [[:facts :every-task ":base :base :softland.store/depot" (setting :softland.store/depot :microbatch 800)]
            [:facts :every-task ":base :system :softland.store/permission"
             (fact-row base-writer (arrived-fact :system :softland.store/permission system-own) (uuid 800) 1 before)]
            [:facts :every-task ":base :carol :softland.store/permission"
             (fact-row base-writer (arrived-fact :carol :softland.store/permission carols-see) (uuid 806) 0 before)]
            [:facts :every-task base-marks-place (base-marks-row {:on since :gone since})]
            [:facts :every-task ":base :base :softland.store/builds-on" (setting :softland.store/builds-on [:older-base] 804)]
            [:facts p1 (record/place-key :base p1 :on) (fact-before p1 :on :c0 810 before)] (entry-before p1 :on :c0 810)
            [:facts q1 (record/place-key :base q1 :on) (fact-before q1 :on :c0 811 before)] (entry-before q1 :on :c0 811)
            [:facts r1 (record/place-key :base r1 :on) (fact-before r1 :on :from-before-the-mark 812 older)]
            [:facts q3 (record/place-key :base q3 :on) (fact-before q3 :on :c5 813 before)] (entry-before q3 :on :c5 813)
            [:facts r2 (record/place-key :base r2 :on) (fact-before r2 :on :c6 805 before)] (entry-before r2 :on :c6 805)
            [:facts q2 (record/place-key :base q2 :on) (fact-before q2 :on :c8 814 before)] (entry-before q2 :on :c8 814)]
     :other-facts {}
     :last-stamp before
     :things
     [;; two acts on one place under a marked key, each with the place as its own
      (base-thing 40 :system system-own (value-at p1 :on :c1))
      (base-thing 41 :system system-own (value-at p1 :on :c2))
      ;; the same value again; a retract of a fact from before the mark
      (base-thing 42 :system system-own (value-at q1 :on :c0))
      (base-thing 43 :system system-own {:e r1 :k :on})
      ;; a marked place on another task than the act's own, over a value since the mark, which comes to hold the value of p1
      (base-thing 44 :system system-own {:e z1 :k :note} (value-at q2 :on :c2))
      ;; a write of the marks: :gone off, :in on; a fact under each; a second write of the marks
      (base-thing 45 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on :in}})
      (base-thing 46 :system system-own (value-at p2 :in :c1))
      (base-thing 47 :system system-own {:e :base :k :softland.store/marked-keys :v #{:on :in :late}})
      (base-thing 59 :system system-own (value-at p3 :gone :c1))
      ;; a fact under a marked key that names a fact which is not there: refused, and nothing of it in the order
      (base-thing 60 :system system-own (assoc (value-at q1 :on :c9) :replaces [(base-id 999) 0]))
      ;; two writes of who may see, from two tasks; two of the list that name one fact to replace
      (base-thing 48 :system system-own {:e :base :k :softland.store/who-may-see :v :members})
      (base-thing 49 :system system-own {:e r3 :k :note} {:e :base :k :softland.store/who-may-see :v :everyone})
      (base-thing 50 :system system-own {:e :base :k :softland.store/builds-on :v [:team] :replaces [(base-id 804) 0]})
      (base-thing 51 :system system-own {:e r3 :k :note-2} {:e :base :k :softland.store/builds-on :v [] :replaces [(base-id 804) 0]})
      ;; a holder of see: she acts, she names her permission without its slot, she gives
      (base-thing 52 :carol carols-see {:e :carol :k :note})
      (base-thing 53 :carol carols {:e :carol :k :note})
      (base-thing 54 :carol carols-see {:e :dave :k :softland.store/permission :v [:dave :base :base carols-see]})
      ;; a retract of a fact since the mark; a fact that names what it replaces
      (base-thing 55 :system system-own {:e q3 :k :on})
      (base-thing 56 :system system-own (assoc (value-at r2 :on :c7) :replaces [(base-id 805) 0]))
      ;; a retract of the marks; marks on another entity
      (base-thing 57 :system system-own {:e :base :k :softland.store/marked-keys})
      (base-thing 58 :system system-own {:e p1 :k :softland.store/marked-keys :v #{:on}})
      ;; a holder of see who sends a value: refused under its own act-id, and no row of its value
      (base-thing 61 :carol carols-see (value-at :carol :on :c1))]}))

(defn- words-by-number
  "Takes a flat record after a batch. Gives, for each answer, the number of
  its act-id's uuid and its word: :accepted, or the reason."
  [flat]
  (into (sorted-map)
        (map (fn [[k row]] [(.getLeastSignificantBits (UUID/fromString (subs k (- (count k) 36)))) (or (:refused row) :accepted)]))
        (:answers flat)))

(defn- orders-compared
  "Runs a fixture's batch in memory in `n` orders of arrival, drawn with a
  fixed seed, and one at a time. Gives {:differ the number of orders whose
  record is not the record of deciding one at a time, :first the first
  difference found, :words the word of each answer}."
  [fixture n]
  (let [{:keys [rows other-facts last-stamp things]} fixture
        random (java.util.Random. 20261008)
        shuffled (fn [xs] (let [l (java.util.ArrayList. ^java.util.Collection (vec xs))]
                            (java.util.Collections/shuffle l random)
                            (vec l)))
        runs (repeatedly n #(run-batch (record-of rows last-stamp) other-facts things now shuffled))
        results (mapv (fn [record] (differences record (one-at-a-time (flat-of rows) other-facts things now (:base record)))) runs)
        in-depot-order (run-batch (record-of rows last-stamp) other-facts things now vec)]
    {:differ (count (remove empty? results))
     :first (first (remove empty? results))
     :words (words-by-number (one-at-a-time (flat-of rows) other-facts things now (:base in-depot-order)))
     :record in-depot-order}))

(def ^:private edge-act
  "An act at the edge of the 10 seconds: it carries a stamp whose time is
  10,400 ms after the clock `now`. Held to the clock `now` it is refused;
  held to a clock one second later it is accepted."
  (assoc (base-thing 70 :system system-own {:e (entity-on 0 2) :k :edge})
         :based-on {[(base-id 802) 0] (stamp/pack (+ now 10400) 0)}))

(def ^:private twice-fixture
  "The batch of `batch-fixture` with `edge-act` as its last thing, and the
  record before it: the batch that the proof runs as two attempts."
  (update batch-fixture :things conj edge-act))

(defn- attempt-of
  "Runs a fixture's batch once in memory, in the depot's order, from the
  record before it, with this clock: one attempt of the batch. Gives the
  record after."
  [{:keys [rows other-facts last-stamp things]} clock]
  (run-batch (record-of rows last-stamp) other-facts things clock vec))

(defn- rows-by-place
  "Takes a record by task. Gives a map from [table task key] to the row at
  that key on that task."
  [record]
  (into {}
        (for [table [:answers :facts :by-value]
              [task rows] (get record table)
              [k row] rows]
          [[table task k] row])))

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
  {:verdict :accepted :reason nil :stamp 77 :digest "aa"})

(def ^:private read-refused
  "What the read by act-id gives for a refused act whose digest is \"aa\"."
  {:verdict :refused :reason :layer-not-made :stamp 78 :digest "aa"})

(def ^:private door-words-cases
  "The cases of the door's words: the table of the seven; that none is a
  gate's word; a word as a maker gets it; the four answers from what a read
  gave; the two acks."
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
   ["the answer from a read of an accept with the digest given: accepted, with its stamp"
    (fn [] (words/answer read-accepted "aa"))
    {:answer :accepted :stamp 77}]
   ["the answer from a read of a refusal with the digest given: refused, with the gate's reason word and its stamp"
    (fn [] (words/answer read-refused "aa"))
    {:answer :refused :reason :layer-not-made :stamp 78}]
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
  {:thing the thing to append}, under the act-id given or `(act-id 70)`; or
  the door's word."
  ([thing] (built thing nil))
  ([thing given]
   (let [reading (additions/reading thing given)]
     (if (:could-not reading)
       reading
       (additions/thing-to-append reading (or given (act-id 70)))))))

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
  back; the kinds of fact; the order of the tests; the thing that is built;
  things that have no end or that throw; the count of a series. They do not
  reach the making of an act-id, the append or the read of an answer, which
  need a cluster: those are the door's proof's."
  (let [given (fn [depot] [:alice depot (uuid 9)])
        permission-fact {:e :bob :k :softland.store/permission :v bobs}
        retract {:e :note-2 :k :text}
        mixed (fn [facts] (assoc makers-act :facts facts))
        three [value-fact retract permission-fact]
        hands-thing (fn [facts] (assoc makers-act :act-id (act-id 70) :depot :streaming :facts facts))]
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
     ["the thing is the maker's map with two parts more: each part that the maker gave is the maker's own object, a part that the form does not know too, and nothing is filled in"
      (fn [] (let [act (assoc (mixed three) :subjects #{:bob} :based-on {[(act-id 5) 0] 7} :note {:a 1})
                   thing (:thing (built act))]
               [(every? (fn [part] (identical? (get act part) (get thing part)))
                        [:version :who :layer :permission :session :subjects :based-on :note :facts])
                (sort (remove (set (keys act)) (keys thing)))
                (select-keys thing [:act-id :depot])
                (sort (remove (set (keys (:thing (built (maker value-fact))))) [:based-on :because-of :claimed-when :subjects]))]))
      [true [:act-id :depot] {:act-id (act-id 70) :depot :streaming}
       [:based-on :because-of :claimed-when :subjects]]]
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
     ["the digest of the thing that the door builds follows the form of the facts as the maker gave them: a list and a lazy sequence have one digest, a vector has another, and each is the digest of the same thing as a hand would send it"
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
      [:read :read :read true true [:act-id :depot]]]
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
    (fn [] (wait/numbers {:read-every-ms 400 :request-wait-ms 400 :fewest-at-once 2 :most-at-once-streaming 16 :other 1}))
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

(defn- source-files
  "The `.clj` files of the store's source: each one under the directory of
  the classpath that holds `softland/store/act.clj`, as [its path from that
  directory, the file], in the order of the paths. The proofs are in
  another directory and are not among them."
  []
  (let [root (-> (io/resource "softland/store/act.clj") io/file .getParentFile .getParentFile .getParentFile)
        from-root (fn [^java.io.File f] (str (.relativize (.toPath ^java.io.File root) (.toPath f))))]
    (->> (file-seq root)
         (filter (fn [^java.io.File f] (and (.isFile f) (str/ends-with? (.getName f) ".clj"))))
         (map (fn [f] [(from-root f) f]))
         (sort-by first))))

(defn- lock-names-in-the-source
  "Reads each file of `source-files` as data. Gives {:files how many were
  read, :names how many symbols and keywords they hold, :saw-act? whether
  the namespace of the act was among the names, :found a vector of [file
  name] for each name that names a lock, and of [file :not-read class] for
  a file that the reader could not read}."
  []
  (let [read-one (fn [[path file]]
                   (try {:file path :names (vec (mapcat names-in (forms-of file)))}
                        (catch Throwable t {:file path :not-read (.getSimpleName (class t))})))
        files (mapv read-one (source-files))]
    {:files (count files)
     :names (reduce + (map (comp count :names) files))
     :saw-act? (boolean (some (fn [{:keys [names]}] (some #{'softland.store.act} names)) files))
     :found (vec (distinct
                  (mapcat (fn [{:keys [file names not-read]}]
                            (if not-read
                              [[file :not-read not-read]]
                              (map (fn [x] [file x]) (filter names-a-lock? names))))
                          files)))}))

(def ^:private lock-name-cases
  "The cases of the test of a name and of the walk of a form, on names and
  forms that the proof writes out: so that the reading of the source can
  find a lock's name if there is one. The reading does not reach a string or
  a comment, a name that code makes while it runs, a file that is no `.clj`
  file, or anything outside the source: the proofs and the pages are not
  read."
  [["names that name a lock: a lock's key, the seal's parts, a stock, the store key of a request, a table of locks, a Java lock, a fact's two parts"
    (fn [] (mapv names-a-lock? ['lock-key 'seal/open 'stock/take-locks 'softland.store.door.seal 'softland.store.door.stock
                                :softland.store/lock-request '$$streaming-locks 'locks-of-request
                                'java.util.concurrent.locks.ReentrantLock 'ReentrantLock :lock-id :sealed 'unsealed 'max-sealed-bytes
                                'with-locks-read '*lock-rows 'Lock]))
    (vec (repeat 17 true))]
   ["names that name none: a clock, a block, Clojure's locking, a deadline, the last stamp, a sale"
    (fn [] (mapv names-a-lock? ['clock-record '$$batch-clock :clock 'kept-clock 'block 'locking 'java.time.Clock
                                'deadline :last-stamp 'sale 'softland.store.gate.batch/clock-row-to-keep]))
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
  (shown "the digest is SHA-256 over the one text, 32 bytes; bytes are fed as they are; the digest of the least act is"
         (digest/hex-of good) "| the same act built a second time has the digest"
         (digest/hex-of (arrived 1 (value-at :note-1 :text)))
         "| the digest that tells a throw, for a thing whose facts throw:" (pr-str (digest/readable-hex-of {:facts (throwing)}))))

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
  (let [rows (concat makers-rows doors-rows)
        n (+ (holds (verdicts rows))
             (holds (verdicts (map (fn [[what x]] [what x :id-too-long]) id-rows)))
             (holds (verdicts limit-rows)))
        earned (into #{} (map #(nth % 2)) (concat rows limit-rows [[nil nil :id-too-long]]))]
    (is (= declaration/reasons (disj earned :ok)) "each word of the declaration comes from a thing that earns it")
    (shown "the check refuses" n "things, each for one fault. act.clj's words, by the maker's own faults:" (words makers-rows))
    (shown "the door's words:" (words doors-rows))
    (shown "an id that prints to 257 characters is refused :id-too-long in each place:" (str/join "; " (map first id-rows)))
    (shown "the limits at their edges:" (words limit-rows))
    (shown "each of the" (count declaration/reasons) "reason words comes from a thing that earns it")))

(deftest the-maker's-check-inside-the-door's
  (let [makers-part (fn [thing] (dissoc thing :act-id :depot))
        rows (filter (fn [[_ x _]] (:refuse (act/parse (makers-part x)))) makers-rows)]
    (doseq [[what x _] rows]
      (is (= (:refuse (act/parse (makers-part x))) (verdict (declaration/check x)))
          (str "the door's check gives act.clj's own word for: " what)))
    (is (= (count makers-rows) (count rows)) "each of the maker's faults is refused by act.clj's own parse")
    (is (= {:ok (dissoc (one-form 1 {:e :note-1 :k :text :v "a text" :replaces nil}) :act-id :depot)}
           (act/parse (makers-part good)))
        "the maker's part of the least thing, which is the thing without its act-id and its depot, is an act that act.clj's own parse takes")
    (shown "for" (count rows) "faults of the maker's form, the door's check gives the word that act.clj's own parse gives for the maker's part, which is the thing without :act-id and :depot")))

(deftest the-order-of-the-reasons
  (let [n (holds (verdicts order-rows))]
    (shown "the order of the declarations gives the reason," n "things with two faults:" (words order-rows))))

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
    (shown "the stamp," n "cases: milliseconds x 65,536 + counter; the next stamp is later than the last when the clock stands still, goes back, or one tick has more than 65,536 decisions; later than a carried stamp; 10,000 ms ahead is the edge of the 10-second rule; the packing reaches 2^62 in the year 4199")
    (shown "a stamp of the clock" now "with the counter 0 is" (stamp/pack now 0))))

(deftest the-first-act
  (holds first-act-cases)
  (shown "the form of a making act:"
         (str/join " | " (map (fn [[what gate-depot _ expected]] (str what " [" (name gate-depot) " gate] " expected)) first-act-rows))))

(deftest the-records-rows
  (let [n (holds record-cases)
        value-row (fact-row writer {:e :note-1 :k :text :v nil :replaces nil :text "\"a text\""} (uuid 5) 2 900)]
    (shown "the record's rows," n "cases: the keys" (pr-str [(record/answer-key :alice (uuid 5)) (record/place-key :alice :note-1 :text)])
           "; each field of a row of its declared class; a retract is a row with no value; every value is kept plain and as a String, the one text of the value in its normal form, which the row takes from the fact as it is; each row of a fact holds who wrote it and the permission that the act named; the heads of the two reads give nothing for what is no id or no act-id")
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
  (holds places-cases)
  (holds writes-cases)
  (holds by-value-cases)
  (shown "the standing of an act:"
         (str/join " | " (map (fn [[what _ _ _ _ word makes?]] (str what " " (or word (if makes? "makes the layer" "stands")))) standing-rows)))
  (shown "the checks of the places:"
         (str/join " | " (map (fn [[what _ _ word]] (str what " " (or word "passes"))) places-rows)))
  (shown "the writes of a decision: a refusal writes the answer and the last stamp; an accept of one value writes 3 rows, the fact, the answer and the last stamp, and the fact's row holds the one text of its value, who wrote it and the permission that the act named:"
         (pr-str (mapv (fn [[which k row]] [which k (if (map? row) (dissoc row :digest) row)])
                       (decision/writes (act-id 1) (act-of good) nil 5 (digest/digest good) {}))))
  (shown "what a permission allows:" (pr-str (mapv (juxt identity act/allows) [alice-own bobs bobs-see bobs-see-and-act]))
         "| a holder of see who names her permission as the row has it, without the slot, and with see and act in it:"
         (pr-str (mapv (fn [named] (decision/standing :streaming (assoc (act-of good) :who :bob :permission named)
                                                      (permission-row bobs-see) nil false))
                       [bobs-see bobs bobs-see-and-act])))
  (shown "a value given as {:b 1.50M, :a [1 2N]} is in its fact's row as"
         (pr-str (:value (nth (first (decision/writes (act-id 1) (act-of (arrived 1 (value-at :note-1 :text (array-map :b 1.50M :a [1 2N]))))
                                                      nil 5 (digest/digest good) {}))
                              2)))
         "and the row reads back as"
         (pr-str (record/read-value (nth (first (decision/writes (act-id 1) (act-of (arrived 1 (value-at :note-1 :text (array-map :b 1.50M :a [1 2N]))))
                                                                 nil 5 (digest/digest good) {}))
                                         2))))
  (shown "the order by value," (count by-value-cases) "cases, each act decided as steps 6b and 7 of the streaming gate decide it: a key of that order is"
         (pr-str (entry-key 100 :c1 :b1))
         "| a mark at stamp 100, a fact, the mark off, the fact replaced, the mark again at 140, the fact replaced twice: the rows after each act are those of the plan's table, and the row of the first marking stays under 100 where no read names it"))

(deftest the-batch
  (let [n (+ (holds (map (fn [[what x expected]] [(str "what the depot derives: " what) (fn [] (batch/first-entity x)) expected])
                         batch-first-rows))
             (holds batch-turn-cases)
             (holds batch-clock-cases)
             (holds arrival-cases)
             (holds reply-cases)
             (holds meeting-cases)
             (holds deciding-cases)
             (holds routing-cases))]
    (shown "the pure code of a batch," n "cases: what the depot derives from a thing, never throwing; the turn is position x tasks + partition; the base is later than the gate's last stamp and than each carried stamp, and 10 seconds ahead at most; an act's stamp is the base plus its turn")
    (shown "what the depot derives:" (str/join " | " (map (fn [[what _ e]] (str what " " (pr-str e))) batch-first-rows)))
    (shown "the clock of a batch," (count batch-clock-cases) "cases: the record that the gate appends is" (pr-str (batch/clock-record 41 now))
           "; an attempt takes the clock of the kept row only when the row is of its batch; the row is written when the table holds none or one of an older batch, so of two records of one batch the first that is processed stays:"
           (pr-str (kept-after [{:batch 41 :clock now} {:batch 41 :clock (+ now 1088)}]))
           "| a record of a closed batch that comes late writes nothing | none of the three throws on a thing that is no row and no record")
    (shown "the standing on arrival:"
           (str/join " | " (map (fn [[what _ _ _ expected]] (str what " " (name (:standing expected)) (when-let [w (:word expected)] (str " " w))))
                                arrival-rows)))
    (shown "who goes to the meeting task:" (str/join " | " (map first (take 9 meeting-cases))))
    (shown "the questions of an act with three values, a fact with no value and a give, as [kind route key]:"
           (pr-str (kinds-of (batch/questions (act-of (base-thing 1 :system system-own (value-at :doc-1 :title "a") (value-at :doc-1 :body "b")
                                                                  (value-at :doc-2 :title "c") {:e :doc-3 :k :note}
                                                                  {:e :carol :k :softland.store/permission :v carols}))
                                              7 :doc-1 true)))
           "| the rows of an act with two values, as [kind home on-every-task?]:"
           (pr-str (let [thing (base-thing 1 :system system-own (value-at :doc-1 :title "a") (value-at :doc-2 :title "b"))
                         act (act-of thing)]
                     (mapv (juxt :which :home :every-task?)
                           (batch/routed (base-id 1) act (decision/writes (base-id 1) act nil 5 (digest/digest thing) {}))))))
    (shown "the marked keys in a batch: a question about a place holds" (pr-str [:marks-place :k])
           "; the home task reads the marks once for a layer; a reply about a place under a marked key holds :since, and the row for each asker that does not have the place as its own; such a place is shared whenever two acts name it; the reply about an answer holds :batch-end; a row of the facts by value goes to the task of its value digest")
    (shown "a pass: a new one is" (pr-str (batch/new-pass)) "| it sends one row for a kind and key, the last:"
           (pr-str (mapv (juxt :which :key :row)
                         (batch/rows-to-send {:out [{:which :fact :key "p" :row 1} {:which :answer :key "a" :row 4}
                                                    {:which :fact :key "p" :row 5} {:which :by-value :key "p" :row 6}]}))))
    (shown "the routing id of an act-id:" (pr-str (record/routing-id [:alice :streaming (uuid 5)])) "for a streaming one,"
           (str (record/routing-id [:base :microbatch (uuid 5)])) "for a microbatch one; a row is of a closed batch when its stamp is not later than closed-through")))

(deftest a-batch-is-decided-in-one-order
  (let [orders 200
        made (orders-compared batch-fixture orders)
        making (orders-compared making-fixture orders)
        words (:words made)
        record (:record made)
        rows-under (fn [table k] (keep (fn [[task rows]] (when-let [row (get rows k)] [task row])) (get record table)))
        words-of (fn [& ns] (set (map words ns)))
        facts-of (fn [n] (:facts (first (filter #(= (base-id n) (:act-id %)) (:things batch-fixture)))))
        rows-of-fact (fn [fact] (rows-under :facts (record/place-key :base (:e fact) (:k fact))))]
    (is (= [0 nil] [(:differ made) (:first made)])
        "a batch into a base that is made: in each order of arrival the record is the record of deciding one at a time")
    (is (= [0 nil] [(:differ making) (:first making)])
        "a batch into a base that is not made: in each order of arrival the record is the record of deciding one at a time")
    (is (= #{:accepted :replaced-fact-not-current} (words-of 1 2)) "two acts of one batch that replace the same fact: one winner")
    (is (= #{:accepted} (words-of 3 4)) "two acts that write one place and name nothing are both accepted")
    (is (= [:accepted :accepted :permission-does-not-stand :accepted] (mapv words [5 6 7 8]))
        "a give and then an act under it: accepted; an act and then the give: the act is refused")
    (is (= [:accepted [[1 true (uuid 9) "\"text 2\""]]]
           [(words 9)
            (mapv (fn [fact] (let [rows (rows-of-fact fact)
                                   [task row] (first rows)]
                               [(count rows) (= task (task-of (:e fact))) (:by-act row) (:value row)]))
                  (facts-of 9))])
        "a value alone on its entity is accepted: one row, on the task of its entity, with the text of the value")
    (is (= [:accepted (stamp/pack (- now 5000) 0)]
           [(words 14) (:stamp (second (first (rows-under :answers (record/answer-key :base (uuid 801))))))])
        "one act-id under two things has one answer; a repeat leaves the answer of the first decision")
    (is (= [:layer-belongs-to-the-other-gate :layer-not-made nil] (mapv words [15 16 18]))
        "an act for a layer of the other gate, made and not made; a stray has no answer")
    (is (= [:accepted [[1 "\"a plain value\""]]]
           [(words 17) (mapv (fn [fact] [(count (rows-of-fact fact)) (:value (second (first (rows-of-fact fact))))]) (facts-of 17))])
        "a plain value under a plain key is accepted, and its row holds the text of the value")
    (is (= [:store-key-not-known :value-too-large [0 0]]
           [(words 27) (words 28) (mapv (fn [n] (count (mapcat rows-of-fact (facts-of n)))) [27 28])])
        "a thing that the declaration refuses has its word under its own act-id and no row: a key of the store's namespace that the store does not know, and a value over its bound")
    (is (= [:carried-stamp-too-far-ahead :permission-does-not-stand :who-not-the-permissions-holder] (mapv words [19 20 21]))
        "no standing: the 10-second rule first, a permission that no act gave, a who that is not the holder")
    (is (= [:accepted :replaced-fact-not-current] (mapv words [22 23])) "an act with values on three tasks is accepted, and one whose third fact is stale is refused")
    (is (= [[1 true (uuid 22) "\"text 4\""] [1 true (uuid 22) "\"text 5\""] [1 true (uuid 22) "\"text 6\""]]
           (mapv (fn [fact] (let [rows (rows-of-fact fact)
                                  [task row] (first rows)]
                              [(count rows) (= task (task-of (:e fact))) (:by-act row) (:value row)]))
                 (facts-of 22)))
        "accepted whole: each fact has one row, on the task of its entity, written by the act, with the text of its value")
    (is (= [3 3 []]
           [(count (facts-of 23))
            (count (set (map (fn [fact] (task-of (:e fact))) (facts-of 23))))
            (vec (filter (fn [[_ row]] (= (uuid 23) (:by-act row))) (mapcat rows-of-fact (facts-of 23))))])
        "refused whole: the act has three facts on three tasks, and no row of it is on any task")
    (is (= [:accepted true] [(words 25) (contains? #{:accepted :replaced-fact-not-current} (words 26))])
        "one act writes a place and names nothing, another replaces the fact that was there before: the order of the turns says whether the second is stale")
    (is (= #{:accepted :permission-fact-not-beneath-the-acts-own} (set (map (:words making) [31 32])))
        "two making acts of the base in one batch: one is accepted")
    (shown "the decisions of a batch of" (count (:things batch-fixture)) "things, run in memory in" orders
           "orders of arrival, are each time those of deciding its acts one at a time in the order of their turns:" (pr-str words))
    (shown "two acts of one batch that replace the same fact, arriving on two tasks: the same winner in every order of arrival:"
           (pr-str (select-keys words [1 2])) "| an act with values on three tasks, each row with the text of its value:"
           (pr-str (mapv (fn [fact] (:value (second (first (rows-of-fact fact))))) (facts-of 22))))
    (shown "a batch into a base that is not made, in" orders "orders: an act before the making, two making acts, acts after:" (pr-str (:words making)))))

(deftest a-batch-under-marked-keys
  (let [orders 200
        marked (orders-compared marks-fixture orders)
        words (:words marked)
        record (:record marked)
        since (:since marks-fixture)
        {:keys [p1 p2 p3 q1 q2 r2]} (:entities marks-fixture)
        batch-end (:last-stamp record)
        placed (for [[task rows] (:by-value record) [k row] rows] [k task row])
        entry (fn [k since v e] (record/by-value-key :base k since (record/value-digest-of v) e))
        on-every-task (fn [k] (mapv (fn [task] (get-in record [:facts task k])) (range sim-tasks)))
        marks-rows (on-every-task base-marks-place)
        see-rows (on-every-task ":base :base :softland.store/who-may-see")
        list-rows (on-every-task ":base :base :softland.store/builds-on")
        run-of (fn [k since v] (let [[from to] (record/by-value-run :base k since v)]
                                 (sort (filter #(and (<= (compare from %) 0) (neg? (compare % to))) (map first placed)))))]
    (is (= [0 nil] [(:differ marked) (:first marked)])
        "a batch under marked keys: in each order of arrival the record, the facts by value too, is the record of deciding one at a time with the marks as the batch began")
    (is (= [:accepted :accepted] (mapv words [40 41])) "two acts of one batch on one place under a marked key are both accepted")
    (is (= #{(entry :on since :c2 p1) (entry :on since :c0 q1) (entry :on since :c2 q2) (entry :on since :c7 r2) (entry :gone since :c1 p3)}
           (set (map first placed)))
        "the facts by value after the batch: of two acts on one place the later turn's value alone, and neither the value before nor the first act's; a value written again; no row for a retract, of a fact from before the mark or since it; the entry of a replaced fact gone and the new one there, also where the place is on another task than the act's and the row before came in a reply; a fact under a key whose mark was taken off in this batch still has its row, under the old stamp")
    (is (= (str ":base :on " since " " (digest-of-text ":c2") " " p1) (entry :on since :c2 p1))
        "a key of that order holds the proof's own SHA-256 of the value's text")
    (is (= [:c2 ":c2"] (let [row (get-in record [:facts (task-of p1) (record/place-key :base p1 :on)])]
                         [(second (record/read-value row)) (:value row)]))
        "the place that two acts wrote holds the later turn's value")
    (is (= [(uuid 42) true] (let [[_ _ row] (first (filter #(= (entry :on since :c0 q1) (first %)) placed))]
                              [(:by-act row) (< since (:stamp row))]))
        "the same value again: the entry is written over with the new fact")
    (is (= [(sort [(entry :on since :c2 p1) (entry :on since :c2 q2)]) 1 true]
           [(run-of :on since :c2)
            (count (set (map second (filter #(= (record/value-digest-of :c2) (record/by-value-route (first %))) placed))))
            (not= (task-of p1) (task-of q2))])
        "two places on two tasks that hold one value: their rows are neighbours in one run, on one task, the task of the value's digest")
    (is (= [:accepted :accepted :accepted :accepted] (mapv words [45 46 47 59])) "a mark, a fact under the new key, a second write of the marks and a fact under the key whose mark went are all accepted")
    (is (= [:replaced-fact-not-current []] [(words 60) (run-of :on since :c9)])
        "a fact under a marked key that names a fact which is not current is refused whole: its value has no row in the order, and the entry of the place is the accepted act's")
    (is (empty? (filter #(str/includes? (first %) " :in ") placed))
        "a mark and a fact under its key in one batch: the fact has no row in the order, whatever the two turns are")
    (is (= [1 #{:on :in :late} {:on since :in batch-end :late batch-end}]
           [(count (set (map comparable marks-rows))) (second (record/read-value (first marks-rows))) (record/marked-keys-of (first marks-rows))])
        "two writes of the marks in one batch: every task holds the later one's row; a key from before keeps its stamp, and a key that both acts newly hold has one stamp, the last stamp that the batch can hold")
    (is (= [true true] (let [in-fact (get-in record [:facts (task-of p2) (record/place-key :base p2 :in)])]
                         [(<= (:stamp in-fact) batch-end) (< since (:stamp in-fact))]))
        "the fact under the newly marked key has a stamp that is not later than the key's stamp: every fact with a later stamp is in the order, and no other")
    (is (= [:accepted :accepted 1 true]
           [(words 48) (words 49) (count (set (map comparable see-rows)))
            (contains? #{[:value :members] [:value :everyone]} (record/read-value (first see-rows)))])
        "two writes of who may see the layer, from two tasks, naming nothing: both accepted, and every task holds one row, the later turn's")
    (is (= [#{:accepted :replaced-fact-not-current} 1]
           [(set (map words [50 51])) (count (set (map comparable list-rows)))])
        "two writes of what the layer builds on that name the same fact to replace: one winner, and every task holds its row")
    (is (= [:permission-does-not-allow-acting :permission-does-not-stand :permission-does-not-allow-acting] (mapv words [52 53 54]))
        "a holder of see: her act is refused; naming her permission without the slot does not stand; her give is refused, so no permission is beneath a see")
    (is (= [:permission-does-not-allow-acting [] nil]
           [(words 61) (run-of :on since :c1)
            (some (fn [task] (get-in record [:facts task (record/place-key :base :carol :on)])) (range sim-tasks))])
        "a holder of see who sends a value under a marked key: refused with the word under her act's own act-id, and no row of the value, in its place or in the order")
    (is (= [:accepted :accepted :store-key-retract-not-built :setting-not-on-the-layer] (mapv words [55 56 57 58]))
        "a retract and a replace under a marked key are accepted; a retract of the marks and marks on another entity are refused")
    (shown "a batch of" (count (:things marks-fixture)) "things into a base that marks two keys, run in memory in" orders
           "orders of arrival: the record, with the facts by value, is each time the record of deciding one at a time, where each act is decided with the marks as the batch began:"
           (pr-str words))
    (shown "two acts of that batch on one place under a marked key, each with the place as its own: both accepted, and the order by value holds the later turn's value alone:"
           (pr-str (mapv (fn [v] [v (count (run-of :on since v))]) [:c0 :c1 :c2 :c7]))
           "| the run of :c2 holds two places that are on two tasks, and both rows are on the task of the value's digest")
    (shown "a write of the marks, a fact under the newly marked key and a second write of the marks, in one batch: the marks hold from the batch after, so the fact has no row; the stamps of the marked keys after the batch:"
           (pr-str (record/marked-keys-of (first marks-rows))) "where the key from before keeps" since "and the last stamp of the batch is" batch-end)))

(deftest a-batch-run-twice-with-one-clock
  (let [before (rows-by-place (record-of (:rows twice-fixture) (:last-stamp twice-fixture)))
        first-attempt (attempt-of twice-fixture now)
        second-attempt (attempt-of twice-fixture now)
        a-second-later (attempt-of twice-fixture (+ now 1000))
        rows-1 (rows-by-place first-attempt)
        rows-2 (rows-by-place second-attempt)
        rows-3 (rows-by-place a-second-later)
        compared (distinct (concat (keys rows-1) (keys rows-2)))
        one-clock (rows-that-differ rows-1 rows-2)
        stamped (filter (fn [at] (and (contains? #{:answers :facts :by-value} (first at))
                                      (seq (fields-that-differ (get before at) (get rows-1 at)))))
                        (keys rows-1))
        same-stamp (filter (fn [at] (= (:stamp (get rows-1 at)) (:stamp (get rows-3 at)))) stamped)
        edge-at [:answers (task-of (uuid 70)) (record/answer-key :base (uuid 70))]
        other-word (into {}
                         (keep (fn [at] (let [words [(:refused (get rows-1 at)) (:refused (get rows-3 at))]]
                                          (when (and (= :answers (first at)) (apply not= words)) [at words]))))
                         (distinct (concat (keys rows-1) (keys rows-3))))]
    (is (= [{} true true] [one-clock (= (set (keys rows-1)) (set (keys rows-2))) (< 20 (count stamped) (inc (count compared)))])
        "a batch run as two attempts with one clock gives the same record: no row differs in any field, the rows of both attempts are at the same places, and more than 20 of them are rows that the batch wrote")
    (is (= {[:facts 0 "k"] #{:value}}
           (rows-that-differ {[:facts 0 "k"] {:value "\"a\"" :stamp 1}} {[:facts 0 "k"] {:value "\"b\"" :stamp 1}}))
        "the comparison of two attempts tells a row whose value alone differs")
    (is (= [(:base first-attempt) (:last-stamp first-attempt)] [(:base second-attempt) (:last-stamp second-attempt)])
        "and the same base and the same last stamp of the gate")
    (is (= [true []] [(< 20 (count stamped)) (vec same-stamp)])
        "the same batch with a clock one second later: each answer and each fact that the batch wrote has another stamp")
    (is (= {edge-at [:carried-stamp-too-far-ahead nil]} other-word)
        "and one act gets the other verdict: the act at the edge of the 10 seconds is refused under the first clock and accepted under the later one; each other answer keeps its word")
    (shown "a batch of" (count (:things twice-fixture)) "things run in memory as two attempts with one clock:" (count compared)
           "rows compared, and" (count one-clock) "differ in any field | with a clock one second later, each of the" (count stamped)
           "answers and facts that the batch wrote has another stamp, and the act at the edge of the 10 seconds gets the other verdict:"
           (pr-str (get other-word edge-at)))))

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
                                (filter (fn [[what _ _]] (re-find #"^no act: (a map with no :layer|a thing whose :layer cannot|a record with a :layer|an act that already carries :depot|257 facts|a fact that is a record|a fact that carries :sealed|a value that is no value|a value whose text|1,025)" what))
                                        not-an-act-cases))))
    (shown "a reading of a maker's act holds" (pr-str (sort (keys (additions/reading (maker value-fact) nil))))
           "| the thing that the door builds is the maker's act with :act-id and :depot, each other part as given:"
           (pr-str (:thing (built (maker (assoc value-fact :v (array-map :b 1.50M :a [1 2N])) {:e :note-2 :k :text}))))
           "| the declaration's check takes it, and gives the value the text"
           (pr-str (:text (first (:facts (act-of (:thing (built (maker (assoc value-fact :v (array-map :b 1.50M :a [1 2N])))))))))))
    (shown "the digest of the thing with its facts as a list, as a lazy sequence and as a vector:"
           (pr-str (mapv (fn [facts] (subs (digest/hex-of (:thing (built (assoc makers-act :facts facts)))) 0 12))
                         [(list value-fact) (map identity [value-fact]) [value-fact]])))))

(deftest the-doors-wait
  (let [n (holds wait-cases)]
    (shown "the numbers of a maker's wait," n "cases: the door's one number is" (pr-str wait/default-numbers)
           "; an opener's number is taken when it is a fixed-width integer of 1 or more, and no other name is read:"
           (pr-str (wait/numbers {:read-every-ms 50 :request-wait-ms 400}))
           "| the longest wait that the door counts is" wait/longest-wait-ms "ms, and a greater number is cut to it:"
           (pr-str (wait/numbers {:read-every-ms Long/MAX_VALUE})))))

(deftest no-code-of-the-store-names-a-lock
  (let [n (holds lock-name-cases)
        {:keys [files names saw-act? found]} (lock-names-in-the-source)]
    (is (= [] found)
        "no symbol and no keyword in the code of the store's source has a word that begins with lock, seal or stock, but Clojure's locking; a string and a comment are not code")
    (is (= [true true true] [(<= 20 files) (<= 5000 names) saw-act?])
        "the reading of the source read 20 files or more, 5,000 names or more, and the namespace of the act among them: it did not pass for having read nothing")
    (shown "no code of the store names a lock," n "cases of the test of a name, then the source read as data:" files
           "files under the directory that holds softland/store/act.clj," names
           "symbols and keywords, of which" (count found) "name a lock, a seal or a stock" (pr-str found))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-one-text-and-the-digest)
  (the-declaration)
  (refusals-of-the-declaration)
  (the-maker's-check-inside-the-door's)
  (the-order-of-the-reasons)
  (the-bounds)
  (nothing-throws)
  (the-stamp)
  (the-first-act)
  (the-records-rows)
  (the-decision)
  (the-batch)
  (a-batch-is-decided-in-one-order)
  (a-batch-under-marked-keys)
  (a-batch-run-twice-with-one-clock)
  (the-doors-words)
  (the-doors-additions)
  (the-doors-wait)
  (no-code-of-the-store-names-a-lock))

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
