(ns softland.store.pure-proof
  "The proof of the gate's pure namespaces: the one text and the digest, the
  door's declaration, the stamp, the seal, the first act of a layer, the
  record's rows, and the checks of the decision. It needs no cluster and no
  Rama. One command, from the repository root:

    clojure -M:store -m softland.store.pure-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  Approach. As in the act's proof, each check is a case: what it is about, a
  function of no argument that calls the code, and what that function must
  give. Where the proof writes a declaration out a second time, as literals,
  it does so on purpose: a change of the declaration is then made in two
  places. A run of bytes is compared by its content.

  State. The proof owns one atom: the lines that it showed, for the recap.
  It takes random bytes from the JVM for locks and seals, and reads the
  clock for nothing.

  What it does not reach is in the folder's README: anything that needs a
  depot, a PState or a query, which the module's proof runs on a cluster."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [softland.store.act :as act]
            [softland.store.decision :as decision]
            [softland.store.declaration :as declaration]
            [softland.store.digest :as digest]
            [softland.store.first-act :as first-act]
            [softland.store.record :as record]
            [softland.store.seal :as seal]
            [softland.store.stamp :as stamp]
            [softland.store.text :as text])
  (:import [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(defonce ^:private shown-lines (atom []))

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

(defn- lock-id
  "The id of a lock of the request with the act-id `(act-id n)`."
  [n index]
  [(act-id n) index])

(defn- sealed-fact
  "A fact as it arrives sealed: three bytes under a lock of request 50."
  ([e k] (sealed-fact e k 0))
  ([e k index] {:e e :k k :sealed (bytes-of 3) :lock-id (lock-id 50 index)}))

(defn- arrived
  "Alice's act into her own layer as it arrives on the streaming depot, under
  the act-id `(act-id n)`, with these facts and no optional part."
  [n & facts]
  {:act-id (act-id n) :class :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :facts (vec facts)})

(defn- one-form
  "The one form on arrival of `arrived` for facts that are in their one form."
  [n & facts]
  {:act-id (act-id n) :class :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {} :because-of nil :claimed-when nil :subjects #{}
   :facts (vec facts)})

(def ^:private good
  "The least thing that the check takes: Alice's act with one sealed fact."
  (arrived 1 (sealed-fact :note-1 :text)))

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
     (vec (utf8 ":é"))]]))

(def ^:private digest-cases
  "The cases of the digest: its size; what it is the SHA-256 of; a thing that
  throws; the door's comparison."
  (let [no-bytes {:b [1 2N "x"] :a #{:k}}
        with-bytes {:sealed (bytes-of 5) :e :note-1}]
    [["a digest is 32 bytes, and its hex text 64 characters"
      (fn [] [(alength ^bytes (digest/digest no-bytes)) (count (digest/hex-of no-bytes))])
      [32 64]]
     ["for a thing with no bytes, the digest is the SHA-256 of the UTF-8 of its one text"
      (fn [] (digest/hex-of no-bytes))
      (sha-256 (utf8 (text/text no-bytes)))]
     ["the digest of {:a 1} is the SHA-256 of the six characters {:a 1}"
      (fn [] (digest/hex-of {:a 1}))
      (sha-256 (utf8 "{:a 1}"))]
     ["a run of bytes is fed as #bytes, its count and the bytes themselves, not as hex"
      (fn [] (digest/hex-of with-bytes))
      (sha-256 (utf8 "{:e :note-1 :sealed #bytes 5 ") (bytes-of 5) (utf8 "}"))]
     ["the digest of a thing that throws when it is read is the digest of the text #unreadable"
      (fn [] (digest/hex-of {:facts (throwing)}))
      (sha-256 (utf8 "#unreadable"))]
     ["the door's comparison: the digest of the thing sent, of another thing, and no digest"
      (fn [] [(digest/is-digest-of? (digest/hex-of good) good)
              (digest/is-digest-of? (digest/hex-of good) (arrived 1 (sealed-fact :note-2 :text)))
              (digest/is-digest-of? nil good)])
      [true false false]]
     ["the same act sealed again, with other sealed bytes, has another digest"
      (fn [] (= (digest/hex-of good)
                (digest/hex-of (arrived 1 (assoc (sealed-fact :note-1 :text) :sealed (byte-array [1 2 4]))))))
      false]]))

;; ---------------------------------------------------------- the declaration

(def ^:private declared-shapes
  "The five shapes that the door adds, written out here a second time."
  {:arrived-act
   [:parts
    [:act-id :act-id]
    [:class :class-word {:reason :class-not-the-act-ids-class}]
    [:version [:enum 1] {:reason :unknown-version}]
    [:who :id]
    [:layer :id]
    [:permission :permission-id {:absent :no-permission}]
    [:session :id {:absent :no-session}]
    [:based-on [:map-of :fact-id :carried-stamp] {:optional true :default {}}]
    [:because-of :act-id {:optional true}]
    [:claimed-when :int {:optional true}]
    [:subjects [:set-of :id {:max 256}] {:optional true :default #{}}]
    [:facts [:seq-of :arrived-fact] {:empty :empty-act}]]
   :arrived-fact
   [:parts
    [:e :id]
    [:k :key]
    [:v :value {:optional true}]
    [:replaces :fact-id {:optional true}]
    [:lock-id :lock-id {:optional true :reason :lock-id-malformed}]]
   :class-word [:enum :streaming :microbatch]
   :lock-id [:tuple :act-id [:int {:min 0 :below 256}]]
   :lock-count [:int {:min 1}]})

(def ^:private declared-rules
  "The rules of the declaration, written out here a second time."
  [[:permission-is-in-the-acts-layer :permission-not-in-layer]
   [:class-is-the-act-ids-class :class-not-the-act-ids-class]
   [:facts-within-the-limit :too-many-facts]
   [:based-on-within-the-limit :too-many-based-on]
   [:store-key-fact-has-no-seal :store-key-sealed]
   [:class-fact-holds-a-class-word :class-fact-not-a-class-word]
   [:permission-fact-holds-a-permission :permission-fact-not-a-permission]
   [:permission-fact-sits-on-its-holder :permission-fact-not-on-its-holder]
   [:lock-request-holds-a-count :lock-request-not-a-count]
   [:lock-request-within-the-limit :too-many-locks-requested]
   [:lock-request-sits-on-its-act :lock-request-not-on-its-act]
   [:plain-value-is-sealed :not-sealed]
   [:sealed-fact-has-no-plain-value :sealed-with-a-plain-value]
   [:seal-is-whole :seal-incomplete]
   [:sealed-is-bytes :sealed-not-bytes]
   [:sealed-within-the-limit :sealed-value-too-large]
   [:place-named-once :place-named-twice]
   [:no-fact-replaces-a-fact-of-its-act :replaces-own-fact]
   [:lock-named-once :lock-named-twice]
   [:ids-within-the-limit :id-too-long]])

(def ^:private declared-reasons
  "Each word with which the check refuses: `act.clj`'s seven and the door's
  twenty-one."
  #{:malformed :unknown-part :unknown-version :no-permission :no-session :empty-act
    :permission-not-in-layer
    :class-not-the-act-ids-class :lock-id-malformed :too-many-facts :too-many-based-on
    :store-key-sealed :class-fact-not-a-class-word :permission-fact-not-a-permission
    :permission-fact-not-on-its-holder :lock-request-not-a-count :too-many-locks-requested
    :lock-request-not-on-its-act :not-sealed :sealed-with-a-plain-value :seal-incomplete
    :sealed-not-bytes :sealed-value-too-large :place-named-twice :replaces-own-fact
    :lock-named-twice :id-too-long})

(def ^:private declaration-parts-cases
  "The declarations of the door, each against its second writing here, and
  what they say as sets."
  [["the door's map of shapes holds each shape of act.clj as act.clj declares it"
    (fn [] (= act/shapes (select-keys declaration/shapes (keys act/shapes))))
    true]
   ["the five shapes that the door adds"
    (fn [] (apply dissoc declaration/shapes (keys act/shapes)))
    declared-shapes]
   ["the twenty rules, the maker's first, each with its reason"
    (fn [] declaration/rules)
    declared-rules]
   ["the words with which the check refuses, read from the two declarations"
    (fn [] declaration/reasons)
    declared-reasons]
   ["the five words of a stray, in the order tested"
    (fn [] declaration/stray-reasons)
    [:no-act-id :act-id-malformed :id-too-long :act-id-of-the-other-gate :act-id-of-another-layer]]
   ["the five limits: facts, entries of based-on, sealed bytes, locks in a request, the text of an id"
    (fn [] [declaration/max-facts declaration/max-based-on declaration/max-sealed-bytes
            declaration/max-locks-in-request declaration/max-id-text])
    [256 1024 65536 256 256]]
   ["the three store keys, the shape of the value of each, and which is a setting"
    (fn [] [(into {} (map (fn [[k entry]] [k (:value entry)])) declaration/store-keys)
            declaration/setting-keys
            (mapv declaration/store-key? [:class :permission :lock-request :tool/class :owner])])
    [{:class :class-word :permission :permission-id :lock-request :lock-count}
     #{:class}
     [true true true false false]]]])

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
   ["an act-id with a class word that is none" (assoc good :act-id [:alice :stream (uuid 1)]) :act-id-malformed]
   ["an act-id whose last slot is no UUID" (assoc good :act-id [:alice :streaming "u"]) :act-id-malformed]
   ["an act-id whose layer is a string" (assoc good :act-id ["alice" :streaming (uuid 1)]) :act-id-malformed]
   ["an act-id that is a list" (assoc good :act-id (list :alice :streaming (uuid 1))) :act-id-malformed]
   ["an act-id whose layer prints to 257 characters"
    (assoc good :act-id [(long-id 257) :streaming (uuid 1)] :layer (long-id 257)) :id-too-long]
   ["an act-id of the class microbatch" (assoc good :act-id [:alice :microbatch (uuid 1)]) :act-id-of-the-other-gate]
   ["an act-id of another layer" (assoc good :act-id [:bob :streaming (uuid 1)]) :act-id-of-another-layer]
   ["a thing with an act-id and no layer" (dissoc good :layer) :act-id-of-another-layer]
   ["a good act" good nil]
   ["a thing that is no act, with a usable act-id" {:act-id (act-id 1) :layer :alice :junk 1} nil]
   ["an act-id whose layer prints to 256 characters"
    (assoc good :act-id [(long-id 256) :streaming (uuid 1)] :layer (long-id 256)) nil]])

(def ^:private stray-cases
  "The cases of the test for a stray: one for each row of `stray-rows`, and
  the two that show that the test takes the gate's class."
  (concat
   (map (fn [[what x word]] [(str "a stray? " what) (fn [] (declaration/stray-reason :streaming x)) word])
        stray-rows)
   [["the test takes the gate's class: the same microbatch act-id at the microbatch gate is no stray"
     (fn [] (declaration/stray-reason :microbatch (assoc good :act-id [:alice :microbatch (uuid 1)])))
     nil]
    ["and a streaming act-id at the microbatch gate is a stray"
     (fn [] (declaration/stray-reason :microbatch good))
     :act-id-of-the-other-gate]]))

(def ^:private whole
  "A thing with each part, given with subvectors, narrow integers and a list
  of facts: a sealed fact, a retract, a permission given, a request."
  {:act-id (subvec (into [:x] (act-id 9)) 1)
   :class :streaming
   :version (int 1)
   :who :alice :layer :alice
   :permission (subvec [:x :alice :alice :alice] 1)
   :session :alice-s1
   :based-on {[(act-id 2) (int 0)] (int 41)}
   :because-of (act-id 2)
   :claimed-when (int 1700000)
   :subjects #{:alice :bob}
   :facts (list {:e :note-1 :k :text :sealed (bytes-of 3) :lock-id [(act-id 50) (short 0)]
                 :replaces [(act-id 2) (short 0)]}
                {:e :note-2 :k :text}
                {:e :bob :k :permission :v [:bob :alice :alice alice-own]}
                {:e (uuid 9) :k :lock-request :v (int 16)})})

(def ^:private whole-form
  "The one form on arrival of `whole`."
  {:act-id (act-id 9) :class :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {[(act-id 2) 0] 41} :because-of (act-id 2) :claimed-when 1700000
   :subjects #{:alice :bob}
   :facts [{:e :note-1 :k :text :v nil :replaces [(act-id 2) 0] :lock-id (lock-id 50 0) :sealed (bytes-of 3)}
           {:e :note-2 :k :text :v nil :replaces nil :lock-id nil :sealed nil}
           {:e :bob :k :permission :v bobs :replaces nil :lock-id nil :sealed nil}
           {:e (uuid 9) :k :lock-request :v 16 :replaces nil :lock-id nil :sealed nil}]})

(def ^:private passing-cases
  "Things that the check takes, each shown in its one form on arrival."
  [["the least thing in its one form on arrival: each part, and each fact with its six parts"
    (fn [] (act-of good))
    (one-form 1 {:e :note-1 :k :text :v nil :replaces nil :lock-id (lock-id 50 0) :sealed (bytes-of 3)})]
   ["a thing with each part, given with subvectors, narrow integers and a list of facts"
    (fn [] (act-of whole))
    whole-form]
   ["its one form has plain vectors and longs"
    (fn [] (let [form (act-of whole)
                 [fact-id carried] (first (:based-on form))]
             (mapv #(.getSimpleName (class %))
                   [(:act-id form) (:permission form) (:facts form) fact-id (second fact-id) carried
                    (:claimed-when form) (second (:lock-id (first (:facts form))))
                    (:v (nth (:facts form) 3))])))
    ["PersistentVector" "PersistentVector" "PersistentVector" "PersistentVector" "Long" "Long"
     "Long" "Long" "Long"]]
   ["the sealed bytes of the one form are the array that arrived"
    (fn [] (let [sealed (bytes-of 3)
                 form (act-of (arrived 1 {:e :note-1 :k :text :sealed sealed :lock-id (lock-id 50 0)}))]
             (identical? sealed (:sealed (first (:facts form))))))
    true]
   ["a making act: two facts under store keys, with plain values"
    (fn [] (verdict (declaration/check
                     (assoc (arrived 1 {:e :alice :k :class :v :streaming}
                                     {:e :alice :k :permission :v alice-own})
                            :who :system))))
    :ok]
   ["a retract under a plain key: no value and no seal"
    (fn [] (verdict (declaration/check (arrived 1 {:e :note-1 :k :text}))))
    :ok]
   ["a key that looks like a store key and is none is a plain key: its value must be sealed"
    (fn [] [(verdict (declaration/check (arrived 1 {:e :note-1 :k :tool/class :v :a})))
            (verdict (declaration/check (arrived 1 (sealed-fact :note-1 :tool/class))))])
    [:not-sealed :ok]]])

(def ^:private makers-rows
  "One thing for each word of act.clj: the least thing with one fault of the
  maker's form."
  [["a part that neither the maker nor the door names" (assoc good :owner :alice) :unknown-part]
   ["a fact with a part that the form does not name" (arrived 1 (assoc (sealed-fact :note-1 :text) :layer :bob)) :unknown-part]
   ["a version that the code does not know" (assoc good :version 2) :unknown-version]
   ["no permission" (dissoc good :permission) :no-permission]
   ["no session" (dissoc good :session) :no-session]
   ["no fact" (assoc good :facts []) :empty-act]
   ["a permission for another layer" (assoc good :permission [:alice :bob :bob]) :permission-not-in-layer]
   ["a who that is a string" (assoc good :who "alice") :malformed]
   ["a permission five deep" (assoc good :permission (reduce (fn [p _] [:alice :alice :alice p]) alice-own (range 4))) :malformed]
   ["facts that are no sequence" (assoc good :facts {:e :note-1}) :malformed]
   ["a fact that is no map" (assoc good :facts [5]) :malformed]])

(def ^:private doors-rows
  "One thing for each word of the door's declaration: the least thing with
  one fault."
  [["no class" (dissoc good :class) :class-not-the-act-ids-class]
   ["a class that is no class word" (assoc good :class :fast) :class-not-the-act-ids-class]
   ["a class that is not the class slot of the act-id" (assoc good :class :microbatch) :class-not-the-act-ids-class]
   ["a lock id that is no lock id" (arrived 1 (assoc (sealed-fact :note-1 :text) :lock-id [(act-id 50)])) :lock-id-malformed]
   ["a lock index of 256" (arrived 1 (sealed-fact :note-1 :text 256)) :lock-id-malformed]
   ["a fact under a store key that arrives sealed" (arrived 1 (sealed-fact :alice :class)) :store-key-sealed]
   ["a class fact whose value is no class word" (arrived 1 {:e :alice :k :class :v :fast}) :class-fact-not-a-class-word]
   ["a permission fact whose value is no permission" (arrived 1 {:e :bob :k :permission :v [:bob :alice]}) :permission-fact-not-a-permission]
   ["a permission fact whose value is five deep"
    (arrived 1 {:e :bob :k :permission :v (reduce (fn [p _] [:bob :alice :alice p]) alice-own (range 4))})
    :permission-fact-not-a-permission]
   ["a permission fact that does not sit on its holder" (arrived 1 {:e :carol :k :permission :v bobs}) :permission-fact-not-on-its-holder]
   ["a request for no locks" (arrived 1 {:e (uuid 1) :k :lock-request :v 0}) :lock-request-not-a-count]
   ["a request whose count is a string" (arrived 1 {:e (uuid 1) :k :lock-request :v "4"}) :lock-request-not-a-count]
   ["a request for 257 locks" (arrived 1 {:e (uuid 1) :k :lock-request :v 257}) :too-many-locks-requested]
   ["a request that does not sit on the uuid of its act" (arrived 1 {:e (uuid 2) :k :lock-request :v 4}) :lock-request-not-on-its-act]
   ["a plain value under a plain key" (arrived 1 {:e :note-1 :k :text :v "a text"}) :not-sealed]
   ["a fact with sealed bytes and a plain value" (arrived 1 (assoc (sealed-fact :note-1 :text) :v "a text")) :sealed-with-a-plain-value]
   ["sealed bytes with no lock id" (arrived 1 (dissoc (sealed-fact :note-1 :text) :lock-id)) :seal-incomplete]
   ["a lock id with no sealed bytes" (arrived 1 (dissoc (sealed-fact :note-1 :text) :sealed)) :seal-incomplete]
   ["sealed that is a string" (arrived 1 (assoc (sealed-fact :note-1 :text) :sealed "abc")) :sealed-not-bytes]
   ["sealed that holds no byte" (arrived 1 (assoc (sealed-fact :note-1 :text) :sealed (byte-array 0))) :sealed-not-bytes]
   ["two facts on one place" (arrived 1 (sealed-fact :note-1 :text 0) (sealed-fact :note-1 :text 1)) :place-named-twice]
   ["a fact that replaces a fact of its own act"
    (arrived 1 (sealed-fact :note-1 :text 0) (assoc (sealed-fact :note-2 :text 1) :replaces [(act-id 1) 0]))
    :replaces-own-fact]
   ["two facts under one lock" (arrived 1 (sealed-fact :note-1 :text 0) (sealed-fact :note-2 :text 0)) :lock-named-twice]])

(def ^:private id-rows
  "The same long id in each place where an act holds an id. Each is refused
  with the one word."
  (let [long (long-id 257)]
    [["who" (assoc good :who long)]
     ["the session" (assoc good :session long)]
     ["a subject" (assoc good :subjects #{long})]
     ["an entity" (arrived 1 (sealed-fact long :text))]
     ["a fact's key" (arrived 1 (sealed-fact :note-1 long))]
     ["who and the holder of the act's permission" (assoc good :who long :permission [long :alice :alice])]
     ["granted-in, in the act's permission" (assoc good :permission [:alice :alice long])]
     ["the layer of the act-id in because-of" (assoc good :because-of [long :streaming (uuid 2)])]
     ["the layer of a fact id in based-on" (assoc good :based-on {[[long :streaming (uuid 2)] 0] 1})]
     ["the layer of a replaced fact" (arrived 1 (assoc (sealed-fact :note-1 :text) :replaces [[long :streaming (uuid 2)] 0]))]
     ["the layer of a lock id" (arrived 1 (assoc (sealed-fact :note-1 :text) :lock-id [[long :streaming (uuid 50)] 0]))]
     ["the holder in a permission that is given" (arrived 1 {:e long :k :permission :v [long :alice :alice alice-own]})]]))

(def ^:private order-rows
  "Things with two faults. The part that is declared first, the shape before
  a rule, and the rule that is listed first give the reason."
  [["a part that is not named, and a wrong version" (assoc good :owner 1 :version 2) :unknown-part]
   ["no class, and a wrong version" (assoc (dissoc good :class) :version 2) :class-not-the-act-ids-class]
   ["a wrong version, and facts that are no sequence" (assoc good :version 2 :facts 5) :unknown-version]
   ["a wrong version, and facts that throw when they are read" (assoc good :version 2 :facts (throwing)) :unknown-version]
   ["a good version, and facts that throw when they are read" (assoc good :facts (throwing)) :malformed]
   ["no session, and a sealed fact with a plain value" (dissoc (arrived 1 (assoc (sealed-fact :note-1 :text) :v 1)) :session) :no-session]
   ["a fact that is no map, and another with sealed bytes that are a string"
    (assoc good :facts [5 (assoc (sealed-fact :note-1 :text) :sealed "abc")]) :malformed]
   ["a permission for another layer, and a class that is not the act-id's" (assoc good :permission [:alice :bob :bob] :class :microbatch) :permission-not-in-layer]
   ["a plain value, and two facts on one place" (arrived 1 {:e :note-1 :k :text :v 1} {:e :note-1 :k :text :v 2}) :not-sealed]
   ["a store-key fact that is sealed, and a request for no locks"
    (arrived 1 (sealed-fact :alice :class) {:e (uuid 1) :k :lock-request :v 0}) :store-key-sealed]
   ["two facts under one lock, and a long id" (arrived 1 (sealed-fact :note-1 :text 0) (sealed-fact (long-id 257) :text 0)) :lock-named-twice]])

(defn- many-facts
  "An act with n sealed facts, each on its own place under its own lock."
  [n]
  (apply arrived 1 (map (fn [i] {:e (keyword (str "note-" i)) :k :text :sealed (bytes-of 3)
                                 :lock-id [(act-id (+ 100 (quot i 256))) (rem i 256)]})
                        (range n))))

(def ^:private limit-rows
  "Each limit at its edge."
  [["256 facts" (many-facts 256) :ok]
   ["257 facts" (many-facts 257) :too-many-facts]
   ["1,024 entries of based-on" (assoc good :based-on (into {} (map (fn [i] [[(act-id 2) i] 1])) (range 1024))) :ok]
   ["1,025 entries of based-on" (assoc good :based-on (into {} (map (fn [i] [[(act-id 2) i] 1])) (range 1025))) :too-many-based-on]
   ["65,536 sealed bytes" (arrived 1 (assoc (sealed-fact :note-1 :text) :sealed (byte-array 65536))) :ok]
   ["65,537 sealed bytes" (arrived 1 (assoc (sealed-fact :note-1 :text) :sealed (byte-array 65537))) :sealed-value-too-large]
   ["a request for 256 locks" (arrived 1 {:e (uuid 1) :k :lock-request :v 256}) :ok]
   ["a request for 1 lock" (arrived 1 {:e (uuid 1) :k :lock-request :v 1}) :ok]
   ["a lock index of 255" (arrived 1 (sealed-fact :note-1 :text 255)) :ok]
   ["an entity of 256 characters" (arrived 1 (sealed-fact (long-id 256) :text)) :ok]
   ["an entity of 257 characters" (arrived 1 (sealed-fact (long-id 257) :text)) :id-too-long]
   ["ids that are UUIDs: who, the layer, an entity"
    (let [who (uuid 31) layer (uuid 32)]
      {:act-id [layer :streaming (uuid 1)] :class :streaming :version 1 :who who :layer layer
       :permission [who layer layer] :session (uuid 33)
       :facts [{:e (uuid 34) :k :text :sealed (bytes-of 3) :lock-id [[layer :streaming (uuid 50)] 0]}]})
    :ok]])

(def ^:private not-acts
  "Things that are no acts. The check gives data for each and never throws."
  [["nil" nil :malformed]
   ["a number" 5 :malformed]
   ["a string" "act" :malformed]
   ["a record" (->Thing 1 2) :malformed]
   ["a sequence that throws" (throwing) :malformed]
   ["a map with only an act-id" {:act-id (act-id 1)} :class-not-the-act-ids-class]
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

;; ------------------------------------------------------------------- the seal

(def ^:private seal-cases
  "The cases of the seal, on one lock, one place and one sealed value that are
  made when the namespace loads."
  (let [lock (seal/new-lock)
        other-lock (seal/new-lock)
        place [:alice :note-1 :text]
        plain (text/utf8 "a text")
        sealed (seal/seal lock place plain)
        flipped (let [b (aclone ^bytes sealed)] (aset-byte b 20 (unchecked-byte (bit-xor 1 (aget b 20)))) b)]
    [["a new lock: 32 bytes, of way 1, whose word is :aes-256-gcm; two locks differ"
      (fn [] [(alength ^bytes (:bytes lock)) (:way lock) (get seal/ways (:way lock))
              (= (vec (:bytes lock)) (vec (:bytes other-lock)))])
      [32 1 :aes-256-gcm false]]
     ["sealed bytes are 28 more than the plain bytes, and carry no mark of their way"
      (fn [] (- (alength ^bytes sealed) (alength ^bytes plain)))
      28]
     ["the lock opens the sealed bytes at their place, and gives the plain bytes"
      (fn [] [(vec (seal/open lock place sealed)) (seal/opens? lock place sealed)])
      [(vec plain) true]]
     ["sealing the same value twice gives two runs of bytes, and both open"
      (fn [] (let [again (seal/seal lock place plain)]
               [(= (vec sealed) (vec again)) (seal/opens? lock place again)]))
      [false true]]
     ["sealed bytes copied to another entity, key or layer do not open"
      (fn [] [(seal/opens? lock [:alice :note-2 :text] sealed)
              (seal/opens? lock [:alice :note-1 :title] sealed)
              (seal/opens? lock [:bob :note-1 :text] sealed)])
      [false false false]]
     ["another lock does not open them; one changed bit does not open; too few bytes do not open"
      (fn [] [(seal/opens? other-lock place sealed) (seal/opens? lock place flipped)
              (seal/opens? lock place (byte-array 5)) (seal/open lock place (byte-array 0))])
      [false false false nil]]
     ["a lock whose way the code does not know seals nothing and opens nothing"
      (fn [] [(seal/known-way? 1) (seal/known-way? 2) (seal/seal (assoc lock :way 2) place plain)
              (seal/open (assoc lock :way 2) place sealed)])
      [true false nil nil]]
     ["bytes that are no lock give nil, and nothing throws"
      (fn [] [(seal/seal {:bytes (byte-array 5) :way 1} place plain)
              (seal/open {:bytes (byte-array 5) :way 1} place sealed)
              (seal/open {:bytes nil :way 1} place sealed)])
      [nil nil nil]]
     ["a value of no bytes seals and opens"
      (fn [] (vec (seal/open lock place (seal/seal lock place (byte-array 0)))))
      []]]))

;; -------------------------------------------------------------- the first act

(defn- making
  "A making act in its one form: for a layer, by `who`, naming a first
  permission held by `holder`, with the class that the layer starts with."
  [layer who holder]
  (let [permission [holder layer layer]
        class (first-act/class-at-making layer)]
    {:act-id [layer class (uuid 1)] :class class :version 1 :who who :layer layer
     :permission permission :session :s1 :based-on {} :because-of nil :claimed-when nil :subjects #{}
     :facts [{:e layer :k :class :v class :replaces nil :lock-id nil :sealed nil}
             {:e holder :k :permission :v permission :replaces nil :lock-id nil :sealed nil}]}))

(def ^:private first-act-rows
  "Acts into a layer that is not made. For each: the gate's class, the act,
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
     ["no class fact" :streaming (assoc group :facts [(fact group 1) {:e :note :k :text :v nil}]) false]
     ["no permission fact" :streaming (assoc group :facts [(fact group 0) {:e :note :k :text :v nil}]) false]
     ["one fact only" :streaming (assoc group :facts [(fact group 0)]) false]
     ["a third fact beside the two" :streaming (update group :facts conj {:e :note :k :text :v nil}) false]
     ["the class microbatch for a layer that is not the base" :streaming (assoc-in group [:facts 0 :v] :microbatch) false]
     ["a class fact on an entity that is not the layer" :streaming (assoc-in group [:facts 0 :e] :other) false]
     ["a permission fact that is not the permission that the act names"
      :streaming (-> group (assoc-in [:facts 1 :v] [:bob :team :team]) (assoc-in [:facts 1 :e] :bob)) false]
     ["a first permission with a permission above it"
      :streaming (-> group
                     (assoc :permission [:alice :team :team [:x :team :team]])
                     (assoc-in [:facts 1 :v] [:alice :team :team [:x :team :team]]))
      false]
     ["a first permission granted in another layer"
      :streaming (-> group (assoc :permission [:alice :team :elsewhere]) (assoc-in [:facts 1 :v] [:alice :team :elsewhere]))
      false]]))

(def ^:private first-act-cases
  "The cases of the first act: the class that a layer starts with, and one
  case for each row of `first-act-rows`."
  (concat
   [["the class that a layer starts with: microbatch for the base, streaming for any other"
     (fn [] (mapv first-act/class-at-making [:base :alice :team (uuid 1)]))
     [:microbatch :streaming :streaming :streaming]]
    ["the two ids that the form knows"
     (fn [] [first-act/system first-act/base])
     [:system :base]]]
   (map (fn [[what gate-class act expected]]
          [(str "a making act? " what) (fn [] (first-act/making-act? gate-class act)) expected])
        first-act-rows)))

;; ---------------------------------------------------------- the record's rows

(defn- classes
  "The simple class name of each value of a row, by field."
  [row]
  (into {} (map (fn [[k v]] [k (.getSimpleName (class v))])) row))

(def ^:private record-cases
  "The cases of the record's rows: the keys, each row with the class of each
  field, what each row says, and the heads of the three reads."
  (let [u (uuid 5)
        sealed (bytes-of 4)
        digest (digest/digest good)
        sealed-row (record/fact-row {:e :note-1 :k :text :v nil :sealed sealed :lock-id (lock-id 50 3)} u (int 2) (int 900))
        plain-row (record/fact-row {:e :alice :k :class :v :streaming :sealed nil :lock-id nil} u 0 900)
        retract-row (record/fact-row {:e :note-1 :k :text :v nil :sealed nil :lock-id nil} u 1 900)
        lock (seal/new-lock)
        lock-row (record/lock-row lock)
        used-row (record/lock-row-sealing lock-row u (int 2))]
    [["the three keys: an answer, a place, a lock"
      (fn [] [(record/answer-key :alice u) (record/place-key :alice :note-1 :tool/text)
              (record/place-key (uuid 1) (uuid 2) :text) (record/lock-key :alice u 7)])
      [":alice 00000000-0000-0000-0000-000000000005"
       ":alice :note-1 :tool/text"
       "00000000-0000-0000-0000-000000000001 00000000-0000-0000-0000-000000000002 :text"
       ":alice 00000000-0000-0000-0000-000000000005 007"]]
     ["the keys of a request's 256 locks sort as their indexes do"
      (fn [] (let [keys (map #(record/lock-key :alice u %) (range 256))]
               [(= keys (sort keys)) (first keys) (last keys) (count (set keys))]))
      [true ":alice 00000000-0000-0000-0000-000000000005 000" ":alice 00000000-0000-0000-0000-000000000005 255" 256]]
     ["the longest keys that the limits allow: an answer 293 characters, a lock 297, a place 770"
      (fn [] (let [id (long-id 256)]
               [(count (record/answer-key id u)) (count (record/lock-key id u 255)) (count (record/place-key id id id))]))
      [293 297 770]]
     ["an accepted answer's row has no :refused; a refused one has its word; each field of its declared class"
      (fn [] [(classes (record/answer-row nil (int 900) digest)) (classes (record/answer-row :not-sealed 900 digest))])
      [{:stamp "Long" :digest "byte[]"} {:stamp "Long" :digest "byte[]" :refused "Keyword"}]]
     ["an answer as a caller gets it, accepted and refused; no row is nothing yet"
      (fn [] [(record/answer-of (record/answer-row nil 900 digest))
              (record/answer-of (record/answer-row :not-sealed 901 digest))
              (record/answer-of nil)])
      [{:verdict :accepted :reason nil :stamp 900 :digest (text/hex digest)}
       {:verdict :refused :reason :not-sealed :stamp 901 :digest (text/hex digest)}
       nil]]
     ["the row of a sealed fact: the sealed bytes, its lock, the fact that wrote it, the stamp"
      (fn [] [sealed-row (classes sealed-row)])
      [{:value sealed :lock-act (uuid 50) :lock-index 3 :by-act u :by-index 2 :stamp 900}
       {:value "byte[]" :lock-act "UUID" :lock-index "Long" :by-act "UUID" :by-index "Long" :stamp "Long"}]]
     ["the row of a fact under a store key: the bytes of the one text of its plain value, and no lock"
      (fn [] plain-row)
      {:value (utf8 ":streaming") :by-act u :by-index 0 :stamp 900}]
     ["the row of a retract: no value and no lock, and the row is there"
      (fn [] retract-row)
      {:by-act u :by-index 1 :stamp 900}]
     ["which fact wrote a row: the one named; not another index, uuid, layer or class; not an empty place"
      (fn [] (mapv #(record/written-by? :streaming :alice sealed-row %)
                   [[[:alice :streaming u] 2] [[:alice :streaming u] 1] [[:alice :streaming (uuid 6)] 2]
                    [[:bob :streaming u] 2] [[:alice :microbatch u] 2]]))
      [true false false false false]]
     ["no fact wrote an empty place"
      (fn [] (record/written-by? :streaming :alice nil [[:alice :streaming u] 2]))
      false]
     ["a row holds a plain value: that value; not another, not a sealed row, not a retract, not an empty place"
      (fn [] [(record/holds-plain-value? plain-row :streaming) (record/holds-plain-value? plain-row :microbatch)
              (record/holds-plain-value? sealed-row :streaming) (record/holds-plain-value? retract-row :streaming)
              (record/holds-plain-value? nil :streaming)])
      [true false false false false]]
     ["the plain value of a row, read back: a class, a permission, a count; nothing for a sealed row or a retract"
      (fn [] [(record/plain-value-of plain-row)
              (record/plain-value-of (record/fact-row {:e :bob :k :permission :v bobs} u 0 900))
              (record/plain-value-of (record/fact-row {:e u :k :lock-request :v 16} u 0 900))
              (record/plain-value-of sealed-row) (record/plain-value-of retract-row) (record/plain-value-of nil)])
      [:streaming bobs 16 nil nil nil]]
     ["a lock's row before it seals a fact and after: the same bytes and way, then the fact"
      (fn [] [lock-row (classes lock-row) (record/seals-a-fact? lock-row)
              used-row (classes used-row) (record/seals-a-fact? used-row) (record/lock-of used-row)])
      [{:bytes (:bytes lock) :way 1} {:bytes "byte[]" :way "Long"} false
       {:bytes (:bytes lock) :way 1 :seals-act u :seals-index 2}
       {:bytes "byte[]" :way "Long" :seals-act "UUID" :seals-index "Long"} true
       {:bytes (:bytes lock) :way 1}]]
     ["the locks of a request by index, in each state of a lock"
      (fn [] (record/locks-by-index {(record/lock-key :alice u 0) lock-row (record/lock-key :alice u 12) used-row}))
      {0 {:bytes (:bytes lock) :way 1} 12 {:bytes (:bytes lock) :way 1}}]
     ["the head of the read by act-id: the class and the key; nothing for what is no act-id of the layer"
      (fn [] [(record/answer-read :alice [:alice :streaming u]) (record/answer-read :alice [:alice :microbatch u])
              (record/answer-read :bob [:alice :streaming u]) (record/answer-read :alice [:alice :streaming])
              (record/answer-read nil nil) (record/answer-read (long-id 257) [(long-id 257) :streaming u])
              (record/answer-read :alice 5)])
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005"]
       [:microbatch ":alice 00000000-0000-0000-0000-000000000005"]
       nil nil nil nil nil]]
     ["the head of the read of locks: the range, cut to 0 to 255; nothing for an empty range or what is no integer"
      (fn [] [(record/lock-range-read :alice [:alice :streaming u] 0 15)
              (record/lock-range-read :alice [:alice :streaming u] -5 999)
              (record/lock-range-read :alice [:alice :streaming u] 9 3)
              (record/lock-range-read :alice [:alice :streaming u] "0" 3)
              (record/lock-range-read :alice [:alice :streaming u] 0 nil)
              (record/lock-range-read :bob [:alice :streaming u] 0 3)
              (record/lock-range-read :alice [:alice :streaming u] 300 400)])
      [[:streaming ":alice 00000000-0000-0000-0000-000000000005 000" ":alice 00000000-0000-0000-0000-000000000005 015"]
       [:streaming ":alice 00000000-0000-0000-0000-000000000005 000" ":alice 00000000-0000-0000-0000-000000000005 255"]
       nil nil nil nil nil]]
     ["the head of the read of a class: the layer's class place; nothing for what is no id"
      (fn [] [(record/class-read :alice) (record/class-read "alice") (record/class-read nil)
              (record/class-read (long-id 257)) (record/class-read [:alice])])
      [":alice :alice :class" nil nil nil nil]]]))

;; --------------------------------------------------------------- the decision

(def ^:private now "The gate's clock in the cases of the decision." 1700000000000)

(defn- permission-row
  "The row of a permission fact, as an accept writes it."
  [permission]
  (record/fact-row {:e (first permission) :k :permission :v permission} (uuid 90) 1 (stamp/pack now 0)))

(def ^:private class-row
  "The row of the class fact of Alice's layer."
  (record/fact-row {:e :alice :k :class :v :streaming} (uuid 90) 0 (stamp/pack now 0)))

(def ^:private standing-rows
  "For each: the act, the row at the place of who's permission, the row at
  the layer's class place, whether the other gate holds the layer; then the
  word of `standing`, and whether the act makes the layer."
  (let [alices (act-of good)
        bobs-act (assoc alices :who :bob :permission bobs)
        personal (making :alice :system :alice)
        group (making :team :alice :alice)]
    [["the act's permission is at its holder's place" alices (permission-row alice-own) nil false nil false]
     ["an act under a permission given beneath another, at its holder's place" bobs-act (permission-row bobs) nil false nil false]
     ["the place holds another permission of the same holder" (assoc alices :permission [:alice :alice :alice [:x :alice :alice]])
      (permission-row alice-own) nil false :permission-does-not-stand false]
     ["the place is empty and the layer is made: the record does not hold the permission" bobs-act nil class-row false
      :permission-does-not-stand false]
     ["an act under Alice's permission by a who that is not Alice, in a layer that is made"
      (assoc alices :who :mallory) nil class-row false :who-not-the-permissions-holder false]
     ["the system acts again in a personal layer that it made" personal nil class-row false
      :who-not-the-permissions-holder false]
     ["a making act by the holder for a layer that is made: its permission stands, and it makes nothing"
      (making :alice :alice :alice) (permission-row alice-own) nil false nil false]
     ["an act into a layer that is not made" alices nil nil false :layer-not-made false]
     ["a right making act of a personal layer, into a layer that is not made" personal nil nil false nil true]
     ["a right making act of a group's layer" group nil nil false nil true]
     ["a making act of the base at the streaming gate" (making :base :system :system) nil nil false :layer-not-made false]
     ["an act into a layer that the other gate's record holds" alices nil nil true :layer-belongs-to-the-other-gate false]
     ["a making act for a layer that the other gate's record holds" group nil nil true :layer-belongs-to-the-other-gate false]]))

(def ^:private standing-cases
  "The cases of the standing: the two places that are read, and one case for
  each row of `standing-rows`."
  (concat
   [["the places that the standing reads: who's permission, then the layer's class"
     (fn [] [(decision/permission-place (act-of good)) (decision/class-place (act-of good))])
     [":alice :alice :permission" ":alice :alice :class"]]]
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
      (fn [] (decision/refused-early now nil :not-sealed))
      :not-sealed]
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
  "The row that the fact `index` of the act `(act-id n)` wrote."
  [n index]
  (record/fact-row {:e :note-1 :k :text :v nil :sealed (bytes-of 3) :lock-id (lock-id 50 0)} (uuid n) index (stamp/pack now 0)))

(def ^:private places-rows
  "For each: the facts of Alice's act, the rows of the places read (by
  entity and key), and the word of `places`."
  (let [replacing (fn [fact-id] (assoc (sealed-fact :note-1 :text) :replaces fact-id))
        retract-row (record/fact-row {:e :note-1 :k :text :v nil} (uuid 8) 0 (stamp/pack now 0))]
    [["a value that names nothing, on an empty place: never stale" [(sealed-fact :note-1 :text)] {} nil]
     ["a value that replaces the current fact of its place" [(replacing [(act-id 7) 0])] {[:note-1 :text] (row-by 7 0)} nil]
     ["a retract that replaces the current fact" [{:e :note-1 :k :text :replaces [(act-id 7) 0]}] {[:note-1 :text] (row-by 7 0)} nil]
     ["a value that replaces a retract, which is the current fact of its place" [(replacing [(act-id 8) 0])] {[:note-1 :text] retract-row} nil]
     ["a value that replaces an earlier fact of the place" [(replacing [(act-id 6) 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces another index of the same act" [(replacing [(act-id 7) 1])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces something on an empty place" [(replacing [(act-id 7) 0])] {} :replaced-fact-not-current]
     ["a value that replaces a fact of another layer" [(replacing [[:bob :streaming (uuid 7)] 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a value that replaces a fact of the other class" [(replacing [[:alice :microbatch (uuid 7)] 0])] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a class fact on an entity that is not the layer" [{:e :bob :k :class :v :streaming}] {} :setting-not-on-the-layer]
     ["a class retract on an entity that is not the layer" [{:e :bob :k :class}] {} :setting-not-on-the-layer]
     ["a permission given straight beneath the act's own, on an empty place" [{:e :bob :k :permission :v bobs}] {} nil]
     ["a holder gives itself a second permission beneath its own: its place holds one"
      [{:e :alice :k :permission :v [:alice :alice :alice alice-own]}] {[:alice :permission] (permission-row alice-own)}
      :store-key-takes-one-write]
     ["a permission given for another layer" [{:e :bob :k :permission :v [:bob :team :alice alice-own]}] {} :permission-fact-for-another-layer]
     ["a permission given with a granted-in that is not the act's layer" [{:e :bob :k :permission :v [:bob :alice :team alice-own]}] {} :permission-fact-granted-in-another-layer]
     ["a permission given with nothing above it, in a layer that is made" [{:e :bob :k :permission :v [:bob :alice :alice]}] {} :permission-fact-not-beneath-the-acts-own]
     ["a permission given beneath a permission that is not the act's own" [{:e :bob :k :permission :v [:bob :alice :alice [:carol :alice :alice alice-own]]}] {}
      :permission-fact-not-beneath-the-acts-own]
     ["the same permission given a second time" [{:e :bob :k :permission :v bobs}] {[:bob :permission] (permission-row bobs)} :store-key-takes-one-write]
     ["a retract of a permission" [{:e :bob :k :permission}] {[:bob :permission] (permission-row bobs)} :ending-a-permission-not-decided]
     ["a retract of the layer's class" [{:e :alice :k :class}] {} :store-key-retract-not-built]
     ["a retract of a request for locks" [{:e (uuid 1) :k :lock-request}] {} :store-key-retract-not-built]
     ["the other class for a layer that is made" [{:e :alice :k :class :v :microbatch}] {} :changing-a-class-not-decided]
     ["the layer's own class a second time" [{:e :alice :k :class :v :streaming}] {} :store-key-takes-one-write]
     ["a request for locks" [{:e (uuid 1) :k :lock-request :v 4}] {} nil]
     ["a request for locks that names something to replace: its place is empty, so it is stale"
      [{:e (uuid 1) :k :lock-request :v 4 :replaces [(act-id 7) 0]}] {} :replaced-fact-not-current]
     ["a stale fact and a class fact on another entity: the stale fact gives the word"
      [(replacing [(act-id 6) 0]) {:e :bob :k :class :v :streaming}] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a class fact on another entity before a permission for another layer: the setting gives the word"
      [{:e :bob :k :permission :v [:bob :team :alice alice-own]} {:e :carol :k :class :v :streaming}] {} :setting-not-on-the-layer]]))

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
   [["the places whose rows are read: of each fact that replaces something and of each permission fact, no other"
     (fn [] (decision/places-needed
             (act-of (arrived 1 (sealed-fact :note-1 :text 0)
                              (assoc (sealed-fact :note-2 :text 1) :replaces [(act-id 7) 0])
                              {:e :bob :k :permission :v bobs}
                              {:e (uuid 1) :k :lock-request :v 2}))
             false))
     [":alice :note-2 :text" ":alice :bob :permission"]]
    ["a making act reads no place, and is not held to the check of a permission fact or to one write for a place"
     (fn [] (let [act (making :team :alice :alice)]
              [(decision/places-needed act true) (decision/places :streaming act true {})
               (decision/places :streaming act false {})]))
     [[] nil :permission-fact-not-beneath-the-acts-own]]
    ["a making act that names something to replace is stale: nothing is current in a layer that is not made"
     (fn [] (decision/places :streaming (assoc-in (making :team :alice :alice) [:facts 0 :replaces] [[:team :streaming (uuid 7)] 0]) true {}))
     :replaced-fact-not-current]]))

(def ^:private seal-fixture
  "Two locks of request 50, a value sealed under the first at its place, and
  the lock rows as the record holds them."
  (let [lock-0 (seal/new-lock)
        lock-1 (seal/new-lock)
        place [:alice :note-1 :text]]
    {:lock-0 lock-0
     :lock-1 lock-1
     :sealed (seal/seal lock-0 place (text/utf8 "a text"))
     :rows {(record/lock-key :alice (uuid 50) 0) (record/lock-row lock-0)
            (record/lock-key :alice (uuid 50) 1) (record/lock-row lock-1)}}))

(defn- sealing
  "Alice's act with one fact at :note-1 :text, sealed under the first lock of
  the fixture, with what `change` changes in the fact."
  [change]
  (act-of (arrived 1 (merge {:e :note-1 :k :text :sealed (:sealed seal-fixture) :lock-id (lock-id 50 0)} change))))

(def ^:private seals-rows
  "For each: the act, the lock rows read, and the word of `seals`."
  (let [rows (:rows seal-fixture)
        key-0 (record/lock-key :alice (uuid 50) 0)]
    [["a value sealed under a lock that the store made, at its place" (sealing {}) rows nil]
     ["an act with no sealed value" (act-of (arrived 1 {:e :note-1 :k :text})) {} nil]
     ["a lock id that the record does not hold" (sealing {:lock-id (lock-id 51 0)}) rows :lock-not-made-for-this-layer]
     ["a lock id of another layer" (sealing {:lock-id [[:bob :streaming (uuid 50)] 0]}) rows :lock-not-made-for-this-layer]
     ["a lock id of the other class" (sealing {:lock-id [[:alice :microbatch (uuid 50)] 0]}) rows :lock-not-made-for-this-layer]
     ["a lock that already seals a value" (sealing {}) (update rows key-0 record/lock-row-sealing (uuid 7) 0) :lock-already-seals-a-value]
     ["a lock whose row names a way that the code does not know" (sealing {}) (assoc-in rows [key-0 :way] 2) :lock-way-not-known]
     ["sealed bytes under another lock of the request" (sealing {:lock-id (lock-id 50 1)}) rows :lock-does-not-open-the-value]
     ["sealed bytes copied to another entity" (sealing {:e :note-2}) rows :lock-does-not-open-the-value]
     ["sealed bytes copied to another key" (sealing {:k :title}) rows :lock-does-not-open-the-value]
     ["any bytes under a real lock id" (sealing {:sealed (bytes-of 40)}) rows :lock-does-not-open-the-value]]))

(def ^:private seals-cases
  "The cases of the seal step: one for each row of `seals-rows`, and the
  ranges of locks that are read."
  (concat
   (map (fn [[what act rows word]] [(str "the seal: " what) (fn [] (decision/seals :streaming act rows)) word])
        seals-rows)
   [["the ranges of locks that are read: one for each request, from the least to the greatest index used"
     (fn [] (set (decision/lock-ranges
                  :streaming
                  (act-of (arrived 1 (sealed-fact :n1 :text 4) (sealed-fact :n2 :text 9) (sealed-fact :n3 :text 6)
                                   {:e :n4 :k :text :sealed (bytes-of 3) :lock-id (lock-id 51 2)}
                                   {:e :n5 :k :text})))))
     #{[(record/lock-key :alice (uuid 50) 4) (record/lock-key :alice (uuid 50) 9)]
       [(record/lock-key :alice (uuid 51) 2) (record/lock-key :alice (uuid 51) 2)]}]
    ["a lock id of another layer or of the other class costs no read"
     (fn [] (decision/lock-ranges
             :streaming
             (act-of (arrived 1 {:e :n1 :k :text :sealed (bytes-of 3) :lock-id [[:bob :streaming (uuid 50)] 0]}
                              {:e :n2 :k :text :sealed (bytes-of 3) :lock-id [[:alice :microbatch (uuid 50)] 1]}))))
     []]]))

(def ^:private writes-cases
  "The cases of the stamp of a decision and of the rows that a decision
  writes: a refusal, an accept, a request, a making act, a retract."
  (let [stamp (stamp/pack now 0)
        digest (digest/digest good)
        rows (:rows seal-fixture)
        key-0 (record/lock-key :alice (uuid 50) 0)
        sealing-act (sealing {})
        answer-key (record/answer-key :alice (uuid 1))]
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
     ["a refusal writes the answer and the last stamp, and nothing else"
      (fn [] (decision/writes (act-id 1) sealing-act :replaced-fact-not-current stamp digest rows))
      [[:answer answer-key {:refused :replaced-fact-not-current :stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["a thing that the declaration refused, which is no act, writes the same two"
      (fn [] (decision/writes (act-id 1) nil :malformed stamp digest {}))
      [[:answer answer-key {:refused :malformed :stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of one sealed value writes four rows: the fact, its lock again, the answer, the last stamp"
      (fn [] (decision/writes (act-id 1) sealing-act nil stamp digest rows))
      [[:fact ":alice :note-1 :text" {:value (:sealed seal-fixture) :lock-act (uuid 50) :lock-index 0
                                      :by-act (uuid 1) :by-index 0 :stamp stamp}]
       [:lock key-0 {:bytes (:bytes (:lock-0 seal-fixture)) :way 1 :seals-act (uuid 1) :seals-index 0}]
       [:answer answer-key {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of a request for 3 locks writes its fact, three keys for new locks with no bytes, the answer, the last stamp"
      (fn [] (decision/writes (act-id 1) (act-of (arrived 1 {:e (uuid 1) :k :lock-request :v 3})) nil stamp digest {}))
      [[:fact (str ":alice " (uuid 1) " :lock-request") {:value (utf8 "3") :by-act (uuid 1) :by-index 0 :stamp stamp}]
       [:new-lock (record/lock-key :alice (uuid 1) 0) nil]
       [:new-lock (record/lock-key :alice (uuid 1) 1) nil]
       [:new-lock (record/lock-key :alice (uuid 1) 2) nil]
       [:answer answer-key {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of a making act writes the class, the first permission, the answer, the last stamp"
      (fn [] (decision/writes [:team :streaming (uuid 1)] (making :team :alice :alice) nil stamp digest {}))
      [[:fact ":team :team :class" {:value (utf8 ":streaming") :by-act (uuid 1) :by-index 0 :stamp stamp}]
       [:fact ":team :alice :permission" {:value (utf8 "[:alice :team :team]") :by-act (uuid 1) :by-index 1 :stamp stamp}]
       [:answer (record/answer-key :team (uuid 1)) {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of a retract and a permission given writes a row for each: the retract's has no value"
      (fn [] (mapv (fn [[which key row]] [which key (if (map? row) (dissoc row :by-act :stamp) row)])
                   (decision/writes (act-id 1)
                                    (act-of (arrived 1 {:e :note-1 :k :text} {:e :bob :k :permission :v bobs}))
                                    nil stamp digest {})))
      [[:fact ":alice :note-1 :text" {:by-index 0}]
       [:fact ":alice :bob :permission" {:value (utf8 "[:bob :alice :alice [:alice :alice :alice]]") :by-index 1}]
       [:answer answer-key {:digest digest}]
       [:last-stamp nil stamp]]]]))

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
  (shown "the digest is SHA-256 over the one text, 32 bytes; bytes are fed as they are; the digest of the least act is"
         (digest/hex-of good)))

(deftest the-declaration
  (holds declaration-parts-cases)
  (holds stray-cases)
  (holds passing-cases)
  (shown "the door's declaration: act.clj's" (count act/shapes) "shapes as act.clj declares them, and five more:"
         (pr-str (sort (keys declared-shapes))))
  (shown "its" (count declaration/rules) "rules, in order:" (pr-str (mapv first declaration/rules)))
  (shown "the store keys:" (pr-str (sort (keys declaration/store-keys))) "the settings:" (pr-str declaration/setting-keys)
         "the limits:" (pr-str {:facts declaration/max-facts :based-on declaration/max-based-on
                                :sealed-bytes declaration/max-sealed-bytes :locks-in-request declaration/max-locks-in-request
                                :id-text declaration/max-id-text}))
  (shown "a stray, by word:"
         (str/join " | " (map (fn [[what _ word]] (str what " " (or word "no stray"))) stray-rows)))
  (shown "the one form on arrival of the least thing:" (pr-str (comparable (act-of good)))))

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
  (let [makers-part (fn [thing] (-> thing
                                    (dissoc :act-id :class)
                                    (update :facts (fn [facts]
                                                     (if (sequential? facts)
                                                       (map (fn [f] (if (map? f) (-> f (dissoc :sealed :lock-id) (assoc :v 1)) f)) facts)
                                                       facts)))))
        rows (filter (fn [[_ x _]] (:refuse (act/parse (makers-part x)))) makers-rows)]
    (doseq [[what x _] rows]
      (is (= (:refuse (act/parse (makers-part x))) (verdict (declaration/check x)))
          (str "the door's check gives act.clj's own word for: " what)))
    (is (= (count makers-rows) (count rows)) "each of the maker's faults is refused by act.clj's own parse")
    (shown "for" (count rows) "faults of the maker's form, the door's check gives the word that act.clj's own parse gives for the maker's part")))

(deftest the-order-of-the-reasons
  (let [n (holds (verdicts order-rows))]
    (shown "the order of the declarations gives the reason," n "things with two faults:" (words order-rows))))

(deftest nothing-throws
  (holds (verdicts not-acts))
  (holds read-predicate-cases)
  (shown "nothing throws. The check refuses" (count not-acts) "things that are no acts:" (words not-acts)))

(deftest the-stamp
  (let [n (holds stamp-cases)]
    (shown "the stamp," n "cases: milliseconds x 65,536 + counter; the next stamp is later than the last when the clock stands still, goes back, or one tick has more than 65,536 decisions; later than a carried stamp; 10,000 ms ahead is the edge of the 10-second rule; the packing reaches 2^62 in the year 4199")
    (shown "a stamp of the clock" now "with the counter 0 is" (stamp/pack now 0))))

(deftest the-seal
  (let [n (holds seal-cases)]
    (shown "the seal," n "cases: a lock is 32 random bytes of way 1 (:aes-256-gcm); sealed bytes are 28 more than the plain; the lock opens them at their place only; another lock, a changed bit, another entity, key or layer do not open; a way that is not known seals and opens nothing; nothing throws")))

(deftest the-first-act
  (holds first-act-cases)
  (shown "the form of a making act:"
         (str/join " | " (map (fn [[what gate-class _ expected]] (str what " [" (name gate-class) " gate] " expected)) first-act-rows))))

(deftest the-records-rows
  (let [n (holds record-cases)]
    (shown "the record's rows," n "cases: the keys" (pr-str [(record/answer-key :alice (uuid 5)) (record/place-key :alice :note-1 :text) (record/lock-key :alice (uuid 5) 7)])
           "; each field of a row of its declared class; a retract is a row with no value; a plain value is the bytes of its one text; the heads of the three reads give nothing for what is no id or no act-id")))

(deftest the-decision
  (holds early-cases)
  (holds standing-cases)
  (holds places-cases)
  (holds seals-cases)
  (holds writes-cases)
  (shown "the standing of an act:"
         (str/join " | " (map (fn [[what _ _ _ _ word makes?]] (str what " " (or word (if makes? "makes the layer" "stands")))) standing-rows)))
  (shown "the checks of the places:"
         (str/join " | " (map (fn [[what _ _ word]] (str what " " (or word "passes"))) places-rows)))
  (shown "the checks of the seal:"
         (str/join " | " (map (fn [[what _ _ word]] (str what " " (or word "passes"))) seals-rows)))
  (shown "the writes of a decision: a refusal writes the answer and the last stamp; an accept of one sealed value writes 4 rows; a request for n locks writes n keys for new locks and no bytes"))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-one-text-and-the-digest)
  (the-declaration)
  (refusals-of-the-declaration)
  (the-maker's-check-inside-the-door's)
  (the-order-of-the-reasons)
  (nothing-throws)
  (the-stamp)
  (the-seal)
  (the-first-act)
  (the-records-rows)
  (the-decision))

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
