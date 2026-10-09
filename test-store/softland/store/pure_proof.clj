(ns softland.store.pure-proof
  "The proof of the gates' pure namespaces: the one text and the digest, the
  door's declaration, the stamp, the seal, the first act of a layer, the
  record's rows, the checks of the decision, and the pure code of a batch of
  the microbatch gate. And of the door's pure namespaces: its words, what it
  makes of one act, and its stock of locks as a value. It needs no cluster
  and no Rama. One command, from the repository root:

    clojure -M:store -m softland.store.pure-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  Approach. As in the act's proof, each check is a case: what it is about, a
  function of no argument that calls the code, and what that function must
  give. Where the proof writes a declaration out a second time, as literals,
  it does so on purpose: a change of the declaration is then made in two
  places. A run of bytes is compared by its content.

  The batch. Beside the cases of each function of `softland.store.gate.batch`,
  the proof runs whole batches in memory: the gate's four steps over the
  pure functions, with each task's rows in its own map and each message
  gathered on the task of its route, in many orders of arrival drawn with a
  fixed seed. It compares the record after each run with the record of
  deciding the same things one at a time in the order of their turns, with
  the checks of `softland.store.gate.decision` in their order.

  State. The proof owns one atom: the lines that it showed, for the recap.
  It takes random bytes from the JVM for locks and seals, and reads the
  clock for nothing. The orders of arrival come from a random source with a
  fixed seed, so each run draws the same.

  The door. The cases of its three pure namespaces hold each rule of the
  door that needs no cluster: each kind of thing that is no act, the
  limits, the thing that it builds, its request for locks, the
  taking of locks from a stock, and the size rule. Where a case needs a
  lock, it makes one itself, as a gate would.

  What it does not reach is in the folder's README: anything that needs a
  depot, a PState or a query, which the module's proof and the door's proof
  run on a cluster."
  (:require [clojure.edn :as edn]
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
            [softland.store.door.seal :as seal]
            [softland.store.gate.stamp :as stamp]
            [softland.store.door.stock :as stock]
            [softland.store.door.text :as text]
            [softland.store.door.words :as words])
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
  throws; the comparison of a sender that still holds its thing."
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
     ["the comparison of a sender that still holds its thing: the digest of the thing sent, of another thing, and no digest"
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
    [:facts [:seq-of :arrived-fact] {:empty :empty-act}]]
   :arrived-fact
   [:parts
    [:e :id]
    [:k :key]
    [:v :value {:optional true}]
    [:replaces :fact-id {:optional true}]
    [:lock-id :lock-id {:optional true :reason :lock-id-malformed}]]
   :depot-word [:enum :streaming :microbatch]
   :lock-id [:tuple :act-id [:int {:min 0 :below 256}]]
   :lock-count [:int {:min 1}]})

(def ^:private declared-rules
  "The rules of the declaration, written out here a second time."
  [[:permission-is-in-the-acts-layer :permission-not-in-layer]
   [:depot-is-the-act-ids-depot :depot-not-the-act-ids-depot]
   [:facts-within-the-limit :too-many-facts]
   [:based-on-within-the-limit :too-many-based-on]
   [:store-key-is-known :store-key-not-known]
   [:store-key-fact-has-no-seal :store-key-sealed]
   [:depot-fact-holds-a-depot-word :depot-fact-not-a-depot-word]
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
    :depot-not-the-act-ids-depot :lock-id-malformed :too-many-facts :too-many-based-on
    :store-key-not-known
    :store-key-sealed :depot-fact-not-a-depot-word :permission-fact-not-a-permission
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
   ["the twenty-one rules, the maker's first, each with its reason"
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
            (mapv declaration/store-key?
                  [:softland.store/depot :softland.store/permission :softland.store/lock-request
                   :class :depot :permission :lock-request :tool/depot :owner])])
    [{:softland.store/depot :depot-word :softland.store/permission :permission-id
      :softland.store/lock-request :lock-count}
     #{:softland.store/depot}
     [true true true false false false false false false]]]
   ["the store's namespace, and each store key is in it"
    (fn [] [declaration/store-namespace (mapv namespace (keys declaration/store-keys))])
    ["softland.store" ["softland.store" "softland.store" "softland.store"]]]])

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
  of facts: a sealed fact, a retract, a permission given, a request."
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
   :facts (list {:e :note-1 :k :text :sealed (bytes-of 3) :lock-id [(act-id 50) (short 0)]
                 :replaces [(act-id 2) (short 0)]}
                {:e :note-2 :k :text}
                {:e :bob :k :softland.store/permission :v [:bob :alice :alice alice-own]}
                {:e (uuid 9) :k :softland.store/lock-request :v (int 16)})})

(def ^:private whole-form
  "The one form on arrival of `whole`."
  {:act-id (act-id 9) :depot :streaming
   :version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {[(act-id 2) 0] 41} :because-of (act-id 2) :claimed-when 1700000
   :subjects #{:alice :bob}
   :facts [{:e :note-1 :k :text :v nil :replaces [(act-id 2) 0] :lock-id (lock-id 50 0) :sealed (bytes-of 3)}
           {:e :note-2 :k :text :v nil :replaces nil :lock-id nil :sealed nil}
           {:e :bob :k :softland.store/permission :v bobs :replaces nil :lock-id nil :sealed nil}
           {:e (uuid 9) :k :softland.store/lock-request :v 16 :replaces nil :lock-id nil :sealed nil}]})

(def ^:private tools-own-keys
  "Keys that are a tool's own, though each looks like one of the store's: the
  bare words that the store's keys were before they took the namespace
  `softland.store`, the bare word depot, the store's words in another
  namespace, and a namespace that only begins with the store's."
  [:class :depot :permission :lock-request
   :tool/depot :tool/permission :tool/lock-request
   :softland/depot :softland.store.tool/depot])

(defn- sealed-under
  "The word of the check for Alice's act with one sealed fact under a key."
  [k]
  (verdict (declaration/check (arrived 1 (sealed-fact :note-1 k)))))

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
                     (assoc (arrived 1 {:e :alice :k :softland.store/depot :v :streaming}
                                     {:e :alice :k :softland.store/permission :v alice-own})
                            :who :system))))
    :ok]
   ["a retract under a plain key: no value and no seal"
    (fn [] (verdict (declaration/check (arrived 1 {:e :note-1 :k :text}))))
    :ok]
   ["each of the three store keys is known: a fact under each, with a plain value, passes"
    (fn [] [(verdict (declaration/check (arrived 1 {:e :alice :k :softland.store/depot :v :streaming})))
            (verdict (declaration/check (arrived 1 {:e :bob :k :softland.store/permission :v bobs})))
            (verdict (declaration/check (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 4})))])
    [:ok :ok :ok]]
   ["a tool's own fact under the bare word :class, sealed, is not read as the store's: it passes as a fact under a plain key"
    (fn [] (sealed-under :class))
    :ok]
   ["a tool's own fact under the bare word :permission, sealed, passes; so does one under :lock-request and one under :depot"
    (fn [] (mapv sealed-under [:permission :lock-request :depot]))
    [:ok :ok :ok]]
   ["a tool's own fact under one of the store's words in another namespace, sealed, passes; a namespace that only begins with the store's is another namespace"
    (fn [] (mapv sealed-under [:tool/depot :tool/permission :tool/lock-request :softland/depot :softland.store.tool/depot]))
    [:ok :ok :ok :ok :ok]]
   ["a key that looks like a store key and is none is a plain key: its value must be sealed, and a retract under it passes"
    (fn [] [(mapv #(verdict (declaration/check (arrived 1 {:e :note-1 :k % :v :streaming}))) [:class :permission :tool/depot])
            (mapv #(verdict (declaration/check (arrived 1 {:e :note-1 :k %}))) [:class :permission :tool/depot])])
    [[:not-sealed :not-sealed :not-sealed] [:ok :ok :ok]]]])

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
  [["no depot" (dissoc good :depot) :depot-not-the-act-ids-depot]
   ["a depot that is no depot word" (assoc good :depot :fast) :depot-not-the-act-ids-depot]
   ["a depot that is not the depot slot of the act-id" (assoc good :depot :microbatch) :depot-not-the-act-ids-depot]
   ["a lock id that is no lock id" (arrived 1 (assoc (sealed-fact :note-1 :text) :lock-id [(act-id 50)])) :lock-id-malformed]
   ["a lock index of 256" (arrived 1 (sealed-fact :note-1 :text 256)) :lock-id-malformed]
   ["a lock index below zero" (arrived 1 (sealed-fact :note-1 :text -1)) :lock-id-malformed]
   ["a sealed fact under a key in the store's namespace that the store does not know" (arrived 1 (sealed-fact :note-1 :softland.store/owner)) :store-key-not-known]
   ["a plain value under such a key" (arrived 1 {:e :alice :k :softland.store/owner :v :alice}) :store-key-not-known]
   ["a retract under such a key" (arrived 1 {:e :alice :k :softland.store/owner}) :store-key-not-known]
   ["the word class in the store's namespace: the store's key is depot" (arrived 1 {:e :alice :k :softland.store/class :v :streaming}) :store-key-not-known]
   ["a fact under a store key that arrives sealed" (arrived 1 (sealed-fact :alice :softland.store/depot)) :store-key-sealed]
   ["a depot fact whose value is no depot word" (arrived 1 {:e :alice :k :softland.store/depot :v :fast}) :depot-fact-not-a-depot-word]
   ["a permission fact whose value is no permission" (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice]}) :permission-fact-not-a-permission]
   ["a permission fact whose value is five deep"
    (arrived 1 {:e :bob :k :softland.store/permission :v (reduce (fn [p _] [:bob :alice :alice p]) alice-own (range 4))})
    :permission-fact-not-a-permission]
   ["a permission fact that does not sit on its holder" (arrived 1 {:e :carol :k :softland.store/permission :v bobs}) :permission-fact-not-on-its-holder]
   ["a request for no locks" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 0}) :lock-request-not-a-count]
   ["a request for a count below zero" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v -1}) :lock-request-not-a-count]
   ["a request whose count is a string" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v "4"}) :lock-request-not-a-count]
   ["a request whose count is no whole number" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 2.5}) :lock-request-not-a-count]
   ["a request for 257 locks" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 257}) :too-many-locks-requested]
   ["a request that does not sit on the uuid of its act" (arrived 1 {:e (uuid 2) :k :softland.store/lock-request :v 4}) :lock-request-not-on-its-act]
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
     ["the holder in a permission that is given" (arrived 1 {:e long :k :softland.store/permission :v [long :alice :alice alice-own]})]
     ["granted-in, in a permission that is given" (arrived 1 {:e :bob :k :softland.store/permission :v [:bob :alice long alice-own]})]]))

(def ^:private order-rows
  "Things with two faults. The part that is declared first, the shape before
  a rule, and the rule that is listed first give the reason."
  [["a part that is not named, and a wrong version" (assoc good :owner 1 :version 2) :unknown-part]
   ["no depot, and a wrong version" (assoc (dissoc good :depot) :version 2) :depot-not-the-act-ids-depot]
   ["a wrong version, and facts that are no sequence" (assoc good :version 2 :facts 5) :unknown-version]
   ["a wrong version, and facts that throw when they are read" (assoc good :version 2 :facts (throwing)) :unknown-version]
   ["a good version, and facts that throw when they are read" (assoc good :facts (throwing)) :malformed]
   ["no session, and a sealed fact with a plain value" (dissoc (arrived 1 (assoc (sealed-fact :note-1 :text) :v 1)) :session) :no-session]
   ["a fact that is no map, and another with sealed bytes that are a string"
    (assoc good :facts [5 (assoc (sealed-fact :note-1 :text) :sealed "abc")]) :malformed]
   ["a permission for another layer, and a depot that is not the act-id's" (assoc good :permission [:alice :bob :bob] :depot :microbatch) :permission-not-in-layer]
   ["a plain value, and two facts on one place" (arrived 1 {:e :note-1 :k :text :v 1} {:e :note-1 :k :text :v 2}) :not-sealed]
   ["a store-key fact that is sealed, and a key in the store's namespace that the store does not know: the key gives the word"
    (arrived 1 (sealed-fact :alice :softland.store/depot) (sealed-fact :note-1 :softland.store/owner 1)) :store-key-not-known]
   ["a store-key fact that is sealed, and a request for no locks"
    (arrived 1 (sealed-fact :alice :softland.store/depot) {:e (uuid 1) :k :softland.store/lock-request :v 0}) :store-key-sealed]
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
   ["a request for 256 locks" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 256}) :ok]
   ["a request for 1 lock" (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 1}) :ok]
   ["a lock index of 255" (arrived 1 (sealed-fact :note-1 :text 255)) :ok]
   ["an entity of 256 characters" (arrived 1 (sealed-fact (long-id 256) :text)) :ok]
   ["an entity of 257 characters" (arrived 1 (sealed-fact (long-id 257) :text)) :id-too-long]
   ["ids that are UUIDs: who, the layer, an entity"
    (let [who (uuid 31) layer (uuid 32)]
      {:act-id [layer :streaming (uuid 1)] :depot :streaming :version 1 :who who :layer layer
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
  permission held by `holder`, with the depot that the layer starts with."
  [layer who holder]
  (let [permission [holder layer layer]
        depot (first-act/depot-at-making layer)]
    {:act-id [layer depot (uuid 1)] :depot depot :version 1 :who who :layer layer
     :permission permission :session :s1 :based-on {} :because-of nil :claimed-when nil :subjects #{}
     :facts [{:e layer :k :softland.store/depot :v depot :replaces nil :lock-id nil :sealed nil}
             {:e holder :k :softland.store/permission :v permission :replaces nil :lock-id nil :sealed nil}]}))

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
     ["no depot fact" :streaming (assoc group :facts [(fact group 1) {:e :note :k :text :v nil}]) false]
     ["no permission fact" :streaming (assoc group :facts [(fact group 0) {:e :note :k :text :v nil}]) false]
     ["one fact only" :streaming (assoc group :facts [(fact group 0)]) false]
     ["a third fact beside the two" :streaming (update group :facts conj {:e :note :k :text :v nil}) false]
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
     [:system :base]]]
   (map (fn [[what gate-depot act expected]]
          [(str "a making act? " what) (fn [] (first-act/making-act? gate-depot act)) expected])
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
        plain-row (record/fact-row {:e :alice :k :softland.store/depot :v :streaming :sealed nil :lock-id nil} u 0 900)
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
     ["which fact wrote a row: the one named; not another index, uuid, layer or depot; not an empty place"
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
     ["the plain value of a row, read back: a depot, a permission, a count; nothing for a sealed row or a retract"
      (fn [] [(record/plain-value-of plain-row)
              (record/plain-value-of (record/fact-row {:e :bob :k :softland.store/permission :v bobs} u 0 900))
              (record/plain-value-of (record/fact-row {:e u :k :softland.store/lock-request :v 16} u 0 900))
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
     ["the head of the read by act-id: the depot and the key; nothing for what is no act-id of the layer"
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
     ["the head of the read of a depot: the layer's depot place; nothing for what is no id"
      (fn [] [(record/depot-read :alice) (record/depot-read "alice") (record/depot-read nil)
              (record/depot-read (long-id 257)) (record/depot-read [:alice])])
      [":alice :alice :softland.store/depot" nil nil nil nil]]]))

;; --------------------------------------------------------------- the decision

(def ^:private now "The gate's clock in the cases of the decision." 1700000000000)

(defn- permission-row
  "The row of a permission fact, as an accept writes it."
  [permission]
  (record/fact-row {:e (first permission) :k :softland.store/permission :v permission} (uuid 90) 1 (stamp/pack now 0)))

(def ^:private depot-row
  "The row of the depot fact of Alice's layer."
  (record/fact-row {:e :alice :k :softland.store/depot :v :streaming} (uuid 90) 0 (stamp/pack now 0)))

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
     ["a making act for a layer that the other gate's record holds" group nil nil true :layer-belongs-to-the-other-gate false]]))

(def ^:private standing-cases
  "The cases of the standing: the two places that are read, and one case for
  each row of `standing-rows`."
  (concat
   [["the places that the standing reads: who's permission, then the layer's depot"
     (fn [] [(decision/permission-place (act-of good)) (decision/depot-place (act-of good))])
     [":alice :alice :softland.store/permission" ":alice :alice :softland.store/depot"]]]
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
     ["a retract of a request for locks" [{:e (uuid 1) :k :softland.store/lock-request}] {} :store-key-retract-not-built]
     ["the other depot for a layer that is made" [{:e :alice :k :softland.store/depot :v :microbatch}] {} :changing-a-depot-not-decided]
     ["the layer's own depot a second time" [{:e :alice :k :softland.store/depot :v :streaming}] {} :store-key-takes-one-write]
     ["a request for locks" [{:e (uuid 1) :k :softland.store/lock-request :v 4}] {} nil]
     ["a request for locks that names something to replace: its place is empty, so it is stale"
      [{:e (uuid 1) :k :softland.store/lock-request :v 4 :replaces [(act-id 7) 0]}] {} :replaced-fact-not-current]
     ["a stale fact and a depot fact on another entity: the stale fact gives the word"
      [(replacing [(act-id 6) 0]) {:e :bob :k :softland.store/depot :v :streaming}] {[:note-1 :text] (row-by 7 0)} :replaced-fact-not-current]
     ["a depot fact on another entity before a permission for another layer: the setting gives the word"
      [{:e :bob :k :softland.store/permission :v [:bob :team :alice alice-own]} {:e :carol :k :softland.store/depot :v :streaming}] {} :setting-not-on-the-layer]
     ["a tool's own fact under the bare word :class, on an entity that is not the layer: no setting" [(sealed-fact :bob :class)] {} nil]
     ["a tool's own fact under the bare word :permission, on an entity that holds a permission: no permission fact"
      [(sealed-fact :bob :permission)] {[:bob :softland.store/permission] (permission-row bobs)} nil]]))

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
             (act-of (arrived 1 (sealed-fact :note-1 :text 0)
                              (assoc (sealed-fact :note-2 :text 1) :replaces [(act-id 7) 0])
                              {:e :bob :k :softland.store/permission :v bobs}
                              {:e (uuid 1) :k :softland.store/lock-request :v 2}
                              (sealed-fact :carol :permission 2)))
             false))
     [":alice :note-2 :text" ":alice :bob :softland.store/permission"]]
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
     ["a lock id of the other depot" (sealing {:lock-id [[:alice :microbatch (uuid 50)] 0]}) rows :lock-not-made-for-this-layer]
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
    ["a lock id of another layer or of the other depot costs no read"
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
      (fn [] (decision/writes (act-id 1) (act-of (arrived 1 {:e (uuid 1) :k :softland.store/lock-request :v 3})) nil stamp digest {}))
      [[:fact (str ":alice " (uuid 1) " :softland.store/lock-request") {:value (utf8 "3") :by-act (uuid 1) :by-index 0 :stamp stamp}]
       [:new-lock (record/lock-key :alice (uuid 1) 0) nil]
       [:new-lock (record/lock-key :alice (uuid 1) 1) nil]
       [:new-lock (record/lock-key :alice (uuid 1) 2) nil]
       [:answer answer-key {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of a making act writes the depot, the first permission, the answer, the last stamp"
      (fn [] (decision/writes [:team :streaming (uuid 1)] (making :team :alice :alice) nil stamp digest {}))
      [[:fact ":team :team :softland.store/depot" {:value (utf8 ":streaming") :by-act (uuid 1) :by-index 0 :stamp stamp}]
       [:fact ":team :alice :softland.store/permission" {:value (utf8 "[:alice :team :team]") :by-act (uuid 1) :by-index 1 :stamp stamp}]
       [:answer (record/answer-key :team (uuid 1)) {:stamp stamp :digest digest}]
       [:last-stamp nil stamp]]]
     ["an accept of a retract and a permission given writes a row for each: the retract's has no value"
      (fn [] (mapv (fn [[which key row]] [which key (if (map? row) (dissoc row :by-act :stamp) row)])
                   (decision/writes (act-id 1)
                                    (act-of (arrived 1 {:e :note-1 :k :text} {:e :bob :k :softland.store/permission :v bobs}))
                                    nil stamp digest {})))
      [[:fact ":alice :note-1 :text" {:by-index 0}]
       [:fact ":alice :bob :softland.store/permission" {:value (utf8 "[:bob :alice :alice [:alice :alice :alice]]") :by-index 1}]
       [:answer answer-key {:digest digest}]
       [:last-stamp nil stamp]]]]))

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

(def ^:private base-locks
  "Twelve locks of the request with the act-id `(base-id 900)`, as the store
  made them."
  (vec (repeatedly 12 seal/new-lock)))

(defn- base-sealed
  "A fact of the base as it arrives sealed: a text, sealed to its place under
  the lock of `base-locks` with this index."
  [e k index a-text]
  {:e e :k k :sealed (seal/seal (nth base-locks index) [:base e k] (text/utf8 a-text))
   :lock-id [(base-id 900) index]})

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

(def ^:private base-depot-row
  "The row of the depot fact of the base."
  (record/fact-row {:e :base :k :softland.store/depot :v :microbatch} (uuid 800) 0 (stamp/pack (- now 5000) 0)))

(defn- base-permission-row
  "The row of a permission fact of the base, as an accept writes it."
  [permission]
  (record/fact-row {:e (first permission) :k :softland.store/permission :v permission} (uuid 800) 1 (stamp/pack (- now 5000) 0)))

(def ^:private arrival-rows
  "For each: an act, the row at the place of its who's permission, the row at
  its layer's depot place, and what step 1 says of its standing."
  (let [act (act-of (base-thing 1 :carol carols {:e :doc-1 :k :note}))
        by-another (act-of (base-thing 1 :mallory carols {:e :doc-1 :k :note}))]
    [["the row holds the act's permission" act (base-permission-row carols) nil {:standing :stands}]
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
                                (assoc (base-sealed :doc-1 :title 4 "a") :replaces [(base-id 800) 0])
                                (base-sealed :doc-1 :body 9 "b")
                                (base-sealed :doc-2 :title 6 "c")
                                {:e :doc-3 :k :note :sealed (bytes-of 40) :lock-id [(base-id 901) 2]}
                                {:e :doc-4 :k :note :sealed (bytes-of 40) :lock-id [[:alice :microbatch (uuid 900)] 1]}
                                {:e :carol :k :softland.store/permission :v carols}
                                {:e u :k :softland.store/lock-request :v 3}))
        questions (batch/questions act 7 :doc-1 true)
        payload-of (fn [kind k] (first (keep (fn [[_ kind' k' [q]]] (when (and (= kind kind') (= k k')) q)) questions)))
        system-row (base-permission-row system-own)
        simple (base-thing 2 :system system-own {:e :doc-1 :k :note})
        simple-act (act-of simple)
        refused (base-thing 3 :system system-own {:e :doc-1 :k :note :v "plain"})]
    (concat
     (map (fn [[what an-act permission-row depot-row expected]]
            [(str "the standing on arrival: " what)
             (fn [] (batch/standing-on-arrival an-act permission-row depot-row))
             expected])
          arrival-rows)
     [["the questions of an act: its answer; each place under a plain key or :softland.store/lock-request; the locks of each request; its own new locks; nothing for a permission fact"
       (fn [] (kinds-of questions))
       [[:answer u (record/answer-key :base u)]
        [:place :doc-1 ":base :doc-1 :title"]
        [:place :doc-1 ":base :doc-1 :body"]
        [:place :doc-2 ":base :doc-2 :title"]
        [:place :doc-3 ":base :doc-3 :note"]
        [:place :doc-4 ":base :doc-4 :note"]
        [:place u (str ":base " u " :softland.store/lock-request")]
        [:locks (uuid 900) (record/answer-key :base (uuid 900))]
        [:locks (uuid 901) (record/answer-key :base (uuid 901))]
        [:locks u (record/answer-key :base u)]]]
      ["a question about a place says whether the row is wanted and whether the place is the act's own"
       (fn [] [(payload-of :place ":base :doc-1 :title") (payload-of :place ":base :doc-1 :body")
               (payload-of :place ":base :doc-2 :title")])
       [{:turn 7 :to :doc-1 :question :place :wanted? true :own? true}
        {:turn 7 :to :doc-1 :question :place :wanted? false :own? true}
        {:turn 7 :to :doc-1 :question :place :wanted? false :own? false}]]
      ["a question about locks holds the range that the decision reads and the indexes named; a lock id of another layer asks nothing"
       (fn [] [(payload-of :locks (record/answer-key :base (uuid 900)))
               (payload-of :locks (record/answer-key :base (uuid 901)))
               (payload-of :locks (record/answer-key :base u))
               (= (set (decision/lock-ranges :microbatch act))
                  (set (keep (fn [[_ kind _ [q]]] (when (= :locks kind) (:range q))) questions)))])
       [{:turn 7 :to :doc-1 :question :locks :indexes #{4 6 9}
         :range [(record/lock-key :base (uuid 900) 4) (record/lock-key :base (uuid 900) 9)]}
        {:turn 7 :to :doc-1 :question :locks :indexes #{2}
         :range [(record/lock-key :base (uuid 901) 2) (record/lock-key :base (uuid 901) 2)]}
        {:turn 7 :to :doc-1 :question :new-locks :makes 3}
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
       (fn [] (let [messages (batch/arrive refused nil :not-sealed 5 1 nil nil)]
                [(kinds-of messages) (first (nth (first messages) 3))]))
       [[[:entry (uuid 3) 5] [:answer (uuid 3) (record/answer-key :base (uuid 3))] [:numbers :microbatch :batch]]
        {:turn 5 :act-id (base-id 3) :to (uuid 3) :refused :not-sealed :digest (digest/digest refused)}]]
      ["an act refused for good, and an act into a layer of which the record holds nothing, ask for their answer only"
       (fn [] [(map first (kinds-of (batch/arrive simple simple-act nil 9 2 (base-permission-row carols) nil)))
               (map first (kinds-of (batch/arrive simple simple-act nil 9 2 nil nil)))
               (map first (kinds-of (batch/arrive simple simple-act nil 9 2 nil base-depot-row)))])
       [[:entry :answer :numbers] [:entry :answer :numbers] [:entry :answer :place :numbers]]]])))

(def ^:private reply-cases
  "The cases of step 2: the reads of a task, and its replies about an answer,
  a place and the locks of a request."
  (let [answer-key (record/answer-key :base (uuid 1))
        place ":base :doc-1 :title"
        request-key (record/answer-key :base (uuid 900))
        lock-key (fn [i] (record/lock-key :base (uuid 900) i))
        asker (fn [turn to & {:as more}] (merge {:turn turn :to to} more))
        a-row (record/answer-row nil 900 (digest/digest good))
        f-row (record/fact-row {:e :doc-1 :k :title :v nil :sealed (bytes-of 4) :lock-id [(base-id 900) 0]} (uuid 800) 0 900)
        lock-rows (into {} (map (fn [i] [(lock-key i) (record/lock-row (nth base-locks i))])) (range 6))
        says (fn [replies] (mapv (fn [[to turn [reply]]] [to turn (:says reply)]) replies))
        shared (fn [replies] (mapv (fn [[to turn [reply]]] [turn (:about reply) (:shared? reply)]) replies))
        about-place (fn [qs] (batch/replies {:about :place :key place :questions qs} f-row now 50))
        about-locks (fn [qs] (batch/replies {:about :locks :key request-key :questions qs} lock-rows now 50))]
    [["the reads of a task: one for each key asked for; a place is read when an asker that does not own it wants it; each range of locks once"
      (fn [] (set (batch/reads-of
                   {:answer {answer-key [(asker 3 :doc-1)]}
                    :place {place [(asker 3 :doc-1 :wanted? true :own? true)]
                            ":base :doc-2 :title" [(asker 3 :doc-1 :wanted? true :own? false)]}
                    :locks {request-key [(asker 3 :doc-1 :question :locks :range ["a" "b"] :indexes #{0})
                                         (asker 7 :doc-2 :question :locks :range ["a" "b"] :indexes #{1})
                                         (asker 9 (uuid 900) :question :new-locks :makes 2)]}
                    :entry {3 [{:turn 3}]}
                    :numbers {:batch {:position 0}}})))
      #{{:about :answer :key answer-key :questions [(asker 3 :doc-1)]}
        {:about :place :key place :questions [(asker 3 :doc-1 :wanted? true :own? true)] :read? false}
        {:about :place :key ":base :doc-2 :title" :questions [(asker 3 :doc-1 :wanted? true :own? false)] :read? true}
        {:about :locks :key request-key :ranges [["a" "b"]]
         :questions [(asker 3 :doc-1 :question :locks :range ["a" "b"] :indexes #{0})
                     (asker 7 :doc-2 :question :locks :range ["a" "b"] :indexes #{1})
                     (asker 9 (uuid 900) :question :new-locks :makes 2)]}}]
     ["check (a) in a batch: with no answer in the record the least turn has the act-id and each other is told that it is taken; the reply carries the clock and the base"
      (fn [] (let [replies (batch/replies {:about :answer :key answer-key :questions [(asker 9 :doc-2) (asker 3 :doc-1) (asker 12 :doc-1)]}
                                          nil now 50)]
               [(says replies) (nth (nth (first replies) 2) 0)]))
      [[[:doc-2 9 :taken-in-this-batch] [:doc-1 3 :yours] [:doc-1 12 :taken-in-this-batch]]
       {:about :answer :says :taken-in-this-batch :clock now :base 50}]]
     ["check (a): with an answer in the record each asker is told that the act-id is used"
      (fn [] (says (batch/replies {:about :answer :key answer-key :questions [(asker 9 :doc-2) (asker 3 :doc-1)]} a-row now 50)))
      [[:doc-2 9 :used] [:doc-1 3 :used]]]
     ["a place named by one act is not shared; the row goes to an asker that wants it and does not own it"
      (fn [] [(mapv #(nth % 2) (about-place [(asker 3 :doc-9 :wanted? true :own? false)]))
              (mapv #(nth % 2) (about-place [(asker 3 :doc-1 :wanted? true :own? true)]))
              (mapv #(nth % 2) (about-place [(asker 3 :doc-9 :wanted? false :own? false)]))])
      [[[{:about :place :key place :shared? false :row f-row}]]
       [[{:about :place :key place :shared? false}]]
       [[{:about :place :key place :shared? false}]]]]
     ["a place named by two acts is shared, unless each of them has it as its own"
      (fn [] [(shared (about-place [(asker 3 :doc-1 :own? true) (asker 7 :doc-1 :own? true)]))
              (shared (about-place [(asker 3 :doc-1 :own? true) (asker 7 :doc-9 :own? false)]))
              (shared (about-place [(asker 3 :doc-8 :own? false) (asker 7 :doc-9 :own? false)]))])
      [[[3 :place false] [7 :place false]] [[3 :place true] [7 :place true]] [[3 :place true] [7 :place true]]]]
     ["locks: each asker gets the rows of its own range; two acts that name two locks of one request do not share"
      (fn [] (let [replies (about-locks [(asker 3 :doc-1 :question :locks :range [(lock-key 0) (lock-key 1)] :indexes #{0 1})
                                         (asker 7 :doc-2 :question :locks :range [(lock-key 4) (lock-key 4)] :indexes #{4})])]
               [(shared replies) (mapv (fn [[_ _ [reply]]] (sort (keys (:rows reply)))) replies)]))
      [[[3 :locks false] [7 :locks false]] [[(lock-key 0) (lock-key 1)] [(lock-key 4)]]]]
     ["locks: a lock that two acts name is shared for both"
      (fn [] (shared (about-locks [(asker 3 :doc-1 :question :locks :range [(lock-key 0) (lock-key 1)] :indexes #{0 1})
                                   (asker 7 :doc-2 :question :locks :range [(lock-key 1) (lock-key 1)] :indexes #{1})
                                   (asker 9 :doc-3 :question :locks :range [(lock-key 5) (lock-key 5)] :indexes #{5})])))
      [[3 :locks true] [7 :locks true] [9 :locks false]]]
     ["locks: a lock that a request of this batch will make is shared for the request and for the act that names it; an act's own new lock is not"
      (fn [] [(shared (about-locks [(asker 3 (uuid 900) :question :new-locks :makes 2)
                                    (asker 7 :doc-2 :question :locks :range [(lock-key 1) (lock-key 1)] :indexes #{1})
                                    (asker 9 :doc-3 :question :locks :range [(lock-key 2) (lock-key 2)] :indexes #{2})]))
              (shared (about-locks [(asker 3 (uuid 900) :question :new-locks :makes 2)
                                    (asker 3 (uuid 900) :question :locks :range [(lock-key 0) (lock-key 0)] :indexes #{0})]))])
      [[[3 :new-locks true] [7 :locks true] [9 :locks false]]
       [[3 :new-locks false] [3 :locks false]]]]]))

(defn- item
  "An item of step 3 as the gate builds it: an act with its turn, told that
  its act-id is its own, standing, with nothing shared; and what `more`
  changes."
  [turn thing & {:as more}]
  (merge {:turn turn :thing thing :act (act-of thing) :act-id (:act-id thing) :to (batch/first-entity thing)
          :says :yours :standing :stands :asks-rows? true :shared? false :clock now :base (stamp/pack now 0)
          :permission-row (base-permission-row system-own) :place-rows {} :lock-rows {}}
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
     ["rule 1: an act with a fact under :softland.store/permission, or under :softland.store/depot, goes to the meeting task"
      (fn [] (batch/who-meets [(item 3 give) (item 7 depot-fact) (item 11 (on-doc-1 1 :title))]))
      #{3 7}]
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
                               (item 9 (on-doc-1 3 :title) :refused :not-sealed :shared? true)
                               (item 13 (on-doc-1 4 :title) :standing :final :shared? true)
                               (item 17 (on-doc-1 5 :title))]))
      #{}]
     ["the items of a task: each entry with what its replies say, in the order of the turns, and whether it meets"
      (fn [] (let [entry (fn [turn n k] (dissoc (item turn (on-doc-1 n k)) :says :clock :base :shared? :place-rows :lock-rows))
                   items (batch/items-of-task
                          {9 [(entry 9 2 :body)] 3 [(entry 3 1 :title)]}
                          {3 [{:about :place :key ":base :doc-1 :title" :shared? true :row {:stamp 5}}
                              {:about :answer :says :yours :clock now :base 50}]
                           9 [{:about :answer :says :used :clock now :base 50}
                              {:about :locks :shared? false :rows {"k" {:way 1}}}]})]
               (mapv #(select-keys % [:turn :says :clock :base :shared? :place-rows :lock-rows :meets?]) items)))
      [{:turn 3 :says :yours :clock now :base 50 :shared? true :place-rows {":base :doc-1 :title" {:stamp 5}} :lock-rows {} :meets? true}
       {:turn 9 :says :used :clock now :base 50 :shared? false :place-rows {} :lock-rows {"k" {:way 1}} :meets? false}]]]))

(def ^:private deciding-cases
  "The cases of step 3 and of the rows of a decision: the rows that a task
  reads for an act at its turn, the home of each row, and what a task writes
  at once and what it sends."
  (let [stamp (stamp/pack now 0)
        lock-key (fn [i] (record/lock-key :base (uuid 900) i))
        lock-rows (into {} (map (fn [i] [(lock-key i) (record/lock-row (nth base-locks i))])) (range 12))
        value (base-thing 1 :system system-own (base-sealed :doc-1 :title 0 "a") (base-sealed :doc-2 :title 1 "b"))
        replacing (base-thing 2 :system system-own
                              (assoc (base-sealed :doc-1 :title 2 "a") :replaces [(base-id 800) 0])
                              (assoc (base-sealed :doc-2 :title 3 "b") :replaces [(base-id 800) 1])
                              {:e :doc-1 :k :body})
        which-homes (fn [rows] (mapv (juxt :which :home :every-task?) rows))
        parcels-of (fn [result] (mapv (fn [[route kind payload]] [route kind (mapv (fn [x] (if (vector? x) (first x) :an-act)) payload)])
                                      (:parcels result)))
        decided-value (batch/decide-or-send (item 5 value :lock-rows lock-rows) {})
        far-ahead (assoc value :based-on {[(base-id 800) 0] (stamp/pack (+ now 60000) 0)})]
    [["the rows that a task reads for an act at its turn: its own places whose fact names something to replace"
      (fn [] [(batch/own-places-wanted (item 5 replacing))
              (batch/own-places-wanted (item 5 replacing :says :used))
              (batch/own-places-wanted (item 5 replacing :standing :final))
              (batch/own-places-wanted (item 5 replacing :asks-rows? false))
              (batch/own-places-wanted (item 5 (assoc replacing :based-on {[(base-id 800) 0] (stamp/pack (+ now 60000) 0)})))])
      [[":base :doc-1 :title"] [] [] [] []]]
     ["the home of each row: a fact on its entity; a used lock as two rows, the home's row without bytes first, on the request's uuid, and the whole row on the value's entity; the answer on the act's uuid"
      (fn [] (let [act (act-of value)
                   rows (batch/routed (base-id 1) act (decision/writes (base-id 1) act nil stamp (digest/digest value) lock-rows))]
               [(which-homes rows) (mapv #(contains? (:row %) :bytes) (filter #(#{:lock :lock-home} (:which %)) rows))
                (= (:key (nth rows 1)) (:key (nth rows 2)) (lock-key 0))]))
      [[[:fact :doc-1 false] [:lock-home (uuid 900) false] [:lock :doc-1 false]
        [:fact :doc-2 false] [:lock-home (uuid 900) false] [:lock :doc-2 false]
        [:answer (uuid 1) false]]
       [false true false true]
       true]]
     ["the home of each row: a fact under :softland.store/depot or :softland.store/permission on every task; a new lock on the act's uuid; a refusal, the answer alone; the last stamp is left out"
      (fn [] (let [thing (base-thing 3 :system system-own {:e :carol :k :softland.store/permission :v carols} {:e (uuid 3) :k :softland.store/lock-request :v 2})
                   act (act-of thing)]
               [(which-homes (batch/routed (base-id 3) act (decision/writes (base-id 3) act nil stamp (digest/digest thing) {})))
                (which-homes (batch/routed (base-id 3) act (decision/writes (base-id 3) act :layer-not-made stamp (digest/digest thing) {})))
                (which-homes (batch/routed (base-id 3) nil (decision/writes (base-id 3) nil :malformed stamp (digest/digest thing) {})))]))
      [[[:fact :carol true] [:fact (uuid 3) false] [:new-lock (uuid 3) false] [:new-lock (uuid 3) false] [:answer (uuid 3) false]]
       [[:answer (uuid 3) false]]
       [[:answer (uuid 3) false]]]]
     ["the home of a tool's own fact under the bare word :permission or :class is its entity's task alone"
      (fn [] (let [thing (base-thing 7 :system system-own {:e :doc-1 :k :permission} {:e :doc-2 :k :class})
                   act (act-of thing)]
               (which-homes (batch/routed (base-id 7) act (decision/writes (base-id 7) act nil stamp (digest/digest thing) {})))))
      [[:fact :doc-1 false] [:fact :doc-2 false] [:answer (uuid 7) false]]]
     ["the rows that the meeting task reads from its own copy: the place of each permission fact, and of a depot fact that names something to replace; none for a tool's own fact under a bare word"
      (fn [] (let [sent (fn [thing] {:act (act-of thing) :permission-row (base-permission-row system-own)})]
               [(batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 8 :system system-own
                                                       {:e :carol :k :softland.store/permission :v carols}
                                                       {:e :base :k :softland.store/depot :v :microbatch :replaces [(base-id 800) 0]}
                                                       {:e :doc-1 :k :permission}
                                                       {:e :doc-1 :k :class :replaces [(base-id 800) 0]})))
                (batch/meeting-reads (batch/new-pass)
                                     (sent (base-thing 9 :system system-own {:e :base :k :softland.store/depot :v :microbatch})))]))
      [[":base :carol :softland.store/permission" ":base :base :softland.store/depot"] []]]
     ["an act decided where it arrived: the fact on its own place is written at once; each other row is a parcel to its home, both rows of each lock too"
      (fn [] [(mapv first (:now decided-value)) (parcels-of decided-value)])
      [[":base :doc-1 :title"]
       [[(uuid 900) :rows [:lock-home]] [:doc-1 :rows [:lock]] [:doc-2 :rows [:fact]] [(uuid 900) :rows [:lock-home]]
        [:doc-2 :rows [:lock]] [(uuid 1) :rows [:answer]]]]]
     ["its rows hold one stamp, the base plus its turn"
      (fn [] (let [stamps (concat (map (comp :stamp second) (:now decided-value))
                                  (keep (fn [[_ _ [[which _ row]]]] (when (#{:fact :answer} which) (:stamp row))) (:parcels decided-value)))]
               [(count stamps) (set stamps)]))
      [3 #{(+ stamp 5)}]]
     ["a repeat, and a thing under an act-id that an earlier turn of the batch took, write nothing and send nothing"
      (fn [] [(batch/decide-or-send (item 5 value :says :used) {}) (batch/decide-or-send (item 5 value :says :taken-in-this-batch) {})])
      [{:now [] :parcels []} {:now [] :parcels []}]]
     ["a thing that the declaration refused sends its answer, with its word, to the task of its uuid"
      (fn [] (let [result (batch/decide-or-send {:turn 5 :act-id (base-id 4) :to (uuid 4) :refused :not-sealed :digest (digest/digest good)
                                                 :says :yours :clock now :base stamp}
                                                {})
                   [route kind [[which k row]]] (first (:parcels result))]
               [(:now result) (count (:parcels result)) route kind which k (:refused row) (:stamp row)]))
      [[] 1 (uuid 4) :rows :answer (record/answer-key :base (uuid 4)) :not-sealed (+ stamp 5)]]
     ["an act refused for good in step 1 sends its answer with that word; with a carried stamp too far ahead as well, the word of the 10-second rule, which comes first"
      (fn [] (let [word-of (fn [thing] (let [[_ _ [[_ _ row]]] (first (:parcels (batch/decide-or-send
                                                                                  (item 5 thing :standing :final :final-word :who-not-the-permissions-holder)
                                                                                  {})))]
                                         (:refused row)))]
               [(word-of value) (word-of far-ahead)]))
      [:who-not-the-permissions-holder :carried-stamp-too-far-ahead]]
     ["an act that meets is sent whole to its layer's task, with its digest, the clock, the base and its rows, and writes nothing here"
      (fn [] (let [result (batch/decide-or-send (item 5 replacing :meets? true :place-rows {":base :doc-2 :title" {:stamp 1}} :lock-rows lock-rows)
                                                {":base :doc-1 :title" {:stamp 2}})
                   [route kind [sent]] (first (:parcels result))]
               [(:now result) (count (:parcels result)) route kind (:place-rows sent)
                (select-keys sent [:turn :act-id :clock :base]) (= (vec (:digest sent)) (vec (digest/digest replacing)))
                (contains? sent :thing)]))
      [[] 1 :base :meet {":base :doc-2 :title" {:stamp 1} ":base :doc-1 :title" {:stamp 2}}
       {:turn 5 :act-id (base-id 2) :clock now :base stamp} true false]]
     ["a task writes every home's row of a lock before any other row that it was sent"
      (fn [] (batch/rows-in-write-order {:rows [[:fact "f" 1] [:lock "k" 2] [:lock-home "k" 3] [:answer "a" 4] [:lock-home "j" 5]]
                                         :meet [{:turn 9} {:turn 3}]}))
      [[:lock-home "k" 3] [:lock-home "j" 5] [:fact "f" 1] [:lock "k" 2] [:answer "a" 4]]]
     ["the acts sent to a meeting task are taken in the order of their turns"
      (fn [] (mapv :turn (batch/acts-that-meet {:meet [{:turn 9} {:turn 3} {:turn 5}]})))
      [3 5 9]]
     ["a pass sends one row for a kind and key, the last, and every home's row of a lock first"
      (fn [] (mapv (juxt :which :key :row)
                   (batch/rows-to-send {:out [{:which :fact :key "p" :row 1} {:which :lock-home :key "k" :row 2} {:which :lock :key "k" :row 3}
                                              {:which :answer :key "a" :row 4} {:which :fact :key "p" :row 5} {:which :lock-home :key "j" :row 6}]})))
      [[:lock-home "k" 2] [:lock-home "j" 6] [:lock "k" 3] [:answer "a" 4] [:fact "p" 5]]]]))

(def ^:private routing-cases
  "The cases of what this part adds to the record's rows: the routing id, the
  heads of the two reads by routing id, a closed row, and the two forms of
  a used lock's row."
  (let [u (uuid 5)
        lock (seal/new-lock)
        used (record/lock-row-sealing (record/lock-row lock) u 2)
        home (record/lock-row-without-bytes used)]
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
     ["the head of the read of locks, with the routing id"
      (fn [] [(record/lock-range-read-by-routing-id u [:base :microbatch u] 0 15)
              (record/lock-range-read-by-routing-id :alice [:alice :streaming u] -5 999)
              (record/lock-range-read-by-routing-id :base [:base :microbatch u] 0 15)
              (record/lock-range-read-by-routing-id u [:base :microbatch u] 9 3)
              (record/lock-range-read-by-routing-id u [:base :microbatch u] "0" 3)
              (record/lock-range-read-by-routing-id nil 5 0 3)])
      [[:microbatch ":base 00000000-0000-0000-0000-000000000005 000" ":base 00000000-0000-0000-0000-000000000005 015"]
       [:streaming ":alice 00000000-0000-0000-0000-000000000005 000" ":alice 00000000-0000-0000-0000-000000000005 255"]
       nil nil nil nil]]
     ["a row is of a closed batch when its stamp is not later than the task's closed-through"
      (fn [] [(record/closed? {:stamp 5} 5) (record/closed? {:stamp 5} 9) (record/closed? {:stamp 5} 4)
              (record/closed? {:stamp 5} nil) (record/closed? nil 9)])
      [true true false false false]]
     ["the home's row of a lock that seals a value: the way and the fact, no bytes; it still says that the lock seals a fact"
      (fn [] [home (classes home) (record/seals-a-fact? home)])
      [{:way 1 :seals-act u :seals-index 2} {:way "Long" :seals-act "UUID" :seals-index "Long"} true]]
     ["the read of a microbatch request's locks: a lock that seals nothing with its bytes; one that seals a value without them, from the whole row and from the home's row alike"
      (fn [] (record/locks-by-index-used-without-bytes
              {(record/lock-key :base u 0) (record/lock-row lock) (record/lock-key :base u 1) used (record/lock-key :base u 2) home}))
      {0 {:bytes (:bytes lock) :way 1} 1 {:bytes nil :way 1} 2 {:bytes nil :way 1}}]
     ["a second value under a lock whose home's row holds no bytes is refused as a lock that already seals a value"
      (fn [] (let [fact (base-sealed :doc-1 :title 0 "v")
                   k (record/lock-key :base (uuid 900) 0)]
               (decision/seals :microbatch (act-of (base-thing 1 :system system-own fact))
                               {k (record/lock-row-without-bytes (record/lock-row-sealing (record/lock-row (nth base-locks 0)) u 0))})))
      :lock-already-seals-a-value]]))

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
  "Writes one row of a decision on a task of the record in memory."
  [record task [which k row]]
  (case which
    :fact (assoc-in record [:facts task k] row)
    (:lock :lock-home) (assoc-in record [:locks task k] row)
    :new-lock (assoc-in record [:locks task k] (record/lock-row (seal/new-lock)))
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
  the messages of a step, and the tasks. Takes the record, {:answers :facts
  :locks}, each a map from task to key to row, and :last-stamp; the other
  gate's facts, a map from key to row; the things, in the depot's order; and
  the clock. Gives the record after the batch, with :base."
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
        replies (gathered
                 (shuffled
                  (for [task (range sim-tasks)
                        read (batch/reads-of (get questions task))
                        :let [found (case (:about read)
                                      :answer (get-in record [:answers task (:key read)])
                                      :place (when (:read? read) (get-in record [:facts task (:key read)]))
                                      :locks (into {}
                                                   (for [[first-key last-key] (:ranges read)
                                                         [k row] (get-in record [:locks task])
                                                         :when (and (<= (compare first-key k) 0) (<= (compare k last-key) 0))]
                                                     [k row])))]
                        [route turn reply] (batch/replies read found clock base)]
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
                  (let [record (reduce (fn [record row] (write-row record task row)) record (batch/rows-in-write-order (get sent task)))
                        pass (reduce (fn [pass met]
                                       (let [local-rows (into {} (map (fn [k] [k (get-in record [:facts task k])])) (batch/meeting-reads pass met))
                                             other-depot-row (when (batch/reads-other-record? pass met)
                                                               (get other-facts (batch/depot-place-of met)))]
                                         (batch/decide-next pass met local-rows other-depot-row (record/lock-row (seal/new-lock)))))
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
                       rows-out)]
    (assoc record
           :base base
           :last-stamp (if base (batch/last-stamp-of-batch base numbers sim-tasks) (:last-stamp record)))))

(defn- one-at-a-time
  "Decides the things of a batch one at a time in the order of their turns,
  against one flat record, with the checks of `softland.store.gate.decision` in
  their order and the depot :microbatch: what the decisions of a batch must be.
  Takes the flat record, {:answers :facts :locks}, each a map from key to
  row; the other gate's facts; the things in the depot's order; the clock;
  and the batch's base. Gives the flat record after."
  [flat other-facts things clock base]
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
                                            (:facts flat))
                           (decision/seals :microbatch act (:locks flat)))]
           (reduce (fn [flat [which k row]]
                     (case which
                       :fact (assoc-in flat [:facts k] row)
                       :lock (assoc-in flat [:locks k] row)
                       :new-lock (assoc-in flat [:locks k] (record/lock-row (seal/new-lock)))
                       :answer (assoc-in flat [:answers k] row)
                       flat))
                   flat
                   (decision/writes act-id act refused (batch/stamp-of-turn base turn) (digest/digest thing) (:locks flat)))))))
   flat
   (sort-by first (turns-of things))))

(defn- differences
  "Compares a record after `run-batch` with the flat record after
  `one-at-a-time`. Gives a list of what differs, empty when nothing does.
  What must be the same: each answer, on one task; each fact row, on one
  task, or on every task for a row under :softland.store/depot or
  :softland.store/permission; each lock, as one row, or as two with one key
  when it seals a value, the bytes in one of them only and the fact in both.
  The bytes of a lock that the batch made are random in each run and are not
  compared."
  [record flat]
  (let [by-key (fn [table] (reduce (fn [m [task rows]] (reduce (fn [m [k row]] (update m k (fnil conj []) row)) m rows)) {} (get record table)))
        answers (by-key :answers)
        facts (by-key :facts)
        locks (by-key :locks)
        same? (fn [a b] (= (comparable a) (comparable b)))]
    (concat
     (when (not= (set (keys answers)) (set (keys (:answers flat)))) [[:answers-under-other-keys]])
     (for [[k row] (:answers flat) :when (not (and (= 1 (count (get answers k))) (same? row (first (get answers k)))))]
       [:answer k (:refused row) (mapv :refused (get answers k))])
     (when (not= (set (keys facts)) (set (keys (:facts flat)))) [[:facts-under-other-keys]])
     (for [[k row] (:facts flat)
           :let [rows (get facts k)
                 everywhere? (or (str/ends-with? k " :softland.store/depot") (str/ends-with? k " :softland.store/permission"))]
           :when (not (and (= (count rows) (if everywhere? sim-tasks 1)) (every? #(same? row %) rows)))]
       [:fact k (count rows)])
     (when (not= (set (keys locks)) (set (keys (:locks flat)))) [[:locks-under-other-keys]])
     (for [[k row] (:locks flat)
           :let [rows (get locks k)]
           :when (not (and (<= 1 (count rows) (if (record/seals-a-fact? row) 2 1))
                           (= 1 (count (filter #(contains? % :bytes) rows)))
                           (every? #(= (dissoc row :bytes) (dissoc % :bytes)) rows)))]
       [:lock k (count rows)]))))

(defn- flat-of
  "Takes rows [table route key row] of a record before a batch. Gives the
  flat record."
  [rows]
  (reduce (fn [flat [table _ k row]] (assoc-in flat [table k] row)) {:answers {} :facts {} :locks {}} rows))

(defn- record-of
  "Takes rows [table route key row] of a record before a batch. Gives the
  record by task: each row on the task of its route, or on every task for
  the route :every-task."
  [rows last-stamp]
  (reduce (fn [record [table route k row]]
            (reduce (fn [record task] (assoc-in record [table task k] row))
                    record
                    (if (= :every-task route) (range sim-tasks) [(task-of route)])))
          {:answers {} :facts {} :locks {} :last-stamp last-stamp}
          rows))

(def ^:private batch-fixture
  "A batch of things into a base that is made, and the record before it. The
  things are in the depot's order. Among them: two acts that replace one
  fact from two tasks; two that write one place and name nothing; a give and
  an act under it, in both orders; two acts under one lock; a request and a
  use of its lock; a repeat; one act-id under two things; an act for a layer
  of the other gate, made and not made; a thing that the declaration
  refuses; strays; acts with no standing; an act on three tasks accepted,
  and one refused; a value on the entity that is its lock's request's uuid;
  an act that writes a place and names nothing, beside one that replaces the
  fact that was there before the batch."
  (let [[p1 p2 p3 q1 q2 q3 r1 r2 r3] (for [n (range 3) task [1 2 3]] (entity-on task n))
        [z1 z2] [(entity-on 0 0) (entity-on 0 1)]
        before (stamp/pack (- now 5000) 0)
        f0 (base-sealed p1 :title 11 "the fact before")
        answered (base-thing 801 :system system-own {:e p1 :k :repeat})
        sealed-on (fn [e index] (base-sealed e :text index (str "text " index)))]
    {:rows (concat
            [[:facts :every-task ":base :base :softland.store/depot" (record/fact-row {:e :base :k :softland.store/depot :v :microbatch} (uuid 800) 0 before)]
             [:facts :every-task ":base :system :softland.store/permission" (record/fact-row {:e :system :k :softland.store/permission :v system-own} (uuid 800) 1 before)]
             [:facts p1 (record/place-key :base p1 :title) (record/fact-row (assoc f0 :v nil) (uuid 802) 0 before)]
             [:facts p2 (record/place-key :base p2 :old) (record/fact-row {:e p2 :k :old :v nil :sealed nil :lock-id nil} (uuid 803) 0 before)]
             [:answers (uuid 801) (record/answer-key :base (uuid 801)) (record/answer-row nil before (digest/digest answered))]]
            (map (fn [i] [:locks (uuid 900) (record/lock-key :base (uuid 900) i) (record/lock-row (nth base-locks i))]) (range 11)))
     :other-facts {":alice :alice :softland.store/depot" (record/fact-row {:e :alice :k :softland.store/depot :v :streaming} (uuid 90) 0 before)}
     :last-stamp before
     :things
     [;; two acts that replace one fact, arriving on two tasks
      (base-thing 1 :system system-own (assoc (base-sealed p1 :title 0 "a") :replaces [(base-id 802) 0]))
      (base-thing 2 :system system-own {:e p2 :k :note} (assoc (base-sealed p1 :title 1 "b") :replaces [(base-id 802) 0]))
      ;; two acts that write one place and name nothing
      (base-thing 3 :system system-own {:e p3 :k :mark})
      (base-thing 4 :system system-own {:e z1 :k :note} {:e p3 :k :mark})
      ;; a give and an act under it; an act and then the give
      (base-thing 5 :system system-own {:e :carol :k :softland.store/permission :v carols})
      (base-thing 6 :carol carols {:e :carol :k :note})
      (base-thing 7 :dave daves-in-base {:e :dave :k :note})
      (base-thing 8 :system system-own {:e :dave :k :softland.store/permission :v daves-in-base})
      ;; an act alone on its entity
      (base-thing 9 :system system-own (sealed-on z2 2))
      ;; two acts under one lock, on two tasks
      (base-thing 10 :system system-own (sealed-on q1 3))
      (base-thing 11 :system system-own (sealed-on q2 3))
      ;; a request for locks, and a use of one of its locks
      (base-thing 12 :system system-own {:e (uuid 12) :k :softland.store/lock-request :v 2})
      (base-thing 13 :system system-own {:e q3 :k :text :sealed (bytes-of 40) :lock-id [(base-id 12) 0]})
      ;; a repeat, and one act-id under two things
      answered
      (base-thing 14 :system system-own {:e p1 :k :twice})
      (base-thing 14 :system system-own {:e p2 :k :twice})
      ;; an act for a layer of the other gate, made and not made
      {:act-id [:alice :microbatch (uuid 15)] :depot :microbatch :version 1 :who :alice :layer :alice
       :permission alice-own :session :s1 :facts [{:e p1 :k :stranger}]}
      {:act-id [:nobody :microbatch (uuid 16)] :depot :microbatch :version 1 :who :alice :layer :nobody
       :permission [:alice :nobody :nobody] :session :s1 :facts [{:e p2 :k :stranger}]}
      ;; a thing that the declaration refuses, and two strays
      (base-thing 17 :system system-own {:e p1 :k :plain :v "not sealed"})
      5
      {:act-id [:base :streaming (uuid 18)] :depot :streaming :layer :base :facts [{:e p1 :k :stray}]}
      ;; no standing: with a stamp too far ahead, with a permission that no act gives, by a who that is not the holder
      (assoc (base-thing 19 :mallory carols {:e p3 :k :m}) :based-on {[(base-id 802) 0] (stamp/pack (+ now 60000) 0)})
      (base-thing 20 :mallory mallorys {:e p3 :k :m})
      (base-thing 21 :mallory system-own {:e p3 :k :m})
      ;; an act on three tasks, accepted; and one refused whole
      (base-thing 22 :system system-own (sealed-on r1 4) (sealed-on r2 5) (sealed-on r3 6))
      (base-thing 23 :system system-own (sealed-on z1 7) (sealed-on q2 8) (assoc (sealed-on q3 9) :replaces [(base-id 999) 0]))
      ;; a value on the entity that is its lock's request's uuid
      (base-thing 24 :system system-own (base-sealed (uuid 900) :note 10 "the lock's home is the value's task"))
      ;; one act writes a place and names nothing; another replaces the fact that was there before the batch
      (base-thing 25 :system system-own {:e p2 :k :old})
      (base-thing 26 :system system-own {:e z2 :k :note} {:e p2 :k :old :replaces [(base-id 803) 0]})]}))

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
            (base-thing 36 :system system-own {:e (uuid 36) :k :softland.store/lock-request :v 3})]})

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

;; ----------------------------------------------------------- the door's words

(def ^:private door-words
  "The door's own fourteen words, written out a second time, each with its
  `:appended`: whether nothing was appended for the maker's act, or the door
  does not know."
  {:find-the-module            :nothing
   :make-an-act-id             :nothing
   :read-the-layers-depot      :nothing
   :read-the-act               :nothing
   :add-its-parts              :nothing
   :seal-a-value               :nothing
   :seal-that-many-values      :nothing
   :have-its-request-accepted  :nothing
   :append-its-request-cleanly :nothing
   :get-locks-within-its-wait  :nothing
   :read-its-requests-answer   :nothing
   :read-its-requests-locks    :nothing
   :append-cleanly             :not-known
   :read-the-answer            :nothing})

(def ^:private read-accepted
  "What the read by act-id gives for an accepted act whose digest is \"aa\"."
  {:verdict :accepted :reason nil :stamp 77 :digest "aa"})

(def ^:private read-refused
  "What the read by act-id gives for a refused act whose digest is \"aa\"."
  {:verdict :refused :reason :not-sealed :stamp 78 :digest "aa"})

(def ^:private door-words-cases
  "The cases of the door's words: the table of the fourteen; that none is a
  gate's word; a word as a maker gets it; the four answers from what a read
  gave; the two acks."
  [["the door's fourteen words, each with whether nothing was appended for the act or the door does not know"
    (fn [] (into {} (map (fn [[w entry]] [w (:appended entry)])) words/words))
    door-words]
   ["no word of the door's is a reason word of the act or of the declaration, or a word of a stray"
    (fn [] (filterv (set (concat act/reasons declaration/reasons declaration/stray-reasons)) (keys words/words)))
    []]
   ["a word as a maker gets it holds :could-not, its own :appended and what it carries, and no :answer and no :reason"
    (fn [] (words/word :append-cleanly {:act-id (act-id 1) :digest "aa" :answer :accepted :reason :not-sealed :appended :nothing}))
    {:could-not :append-cleanly :appended :not-known :act-id (act-id 1) :digest "aa"}]
   ["a word that the table does not hold is not made"
    (fn [] [(words/word :refused {:at :layer}) (words/word :layer-not-made nil) (words/word nil nil)])
    [nil nil nil]]
   ["the answer from a read of an accept with the digest given: accepted, with its stamp"
    (fn [] (words/answer read-accepted "aa"))
    {:answer :accepted :stamp 77}]
   ["the answer from a read of a refusal with the digest given: refused, with the gate's reason word and its stamp"
    (fn [] (words/answer read-refused "aa"))
    {:answer :refused :reason :not-sealed :stamp 78}]
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
  hold of it: the word, its `:appended`, and the place and the numbers that
  it carries."
  [word]
  (select-keys word [:could-not :appended :at :most :sealed-bytes]))

(defn- could-not
  "One of the door's words as `said` gives it, for what a case must give:
  the word with `:appended :nothing`, and its place when one is given."
  ([word] {:could-not word :appended :nothing})
  ([word at] (assoc (could-not word) :at at)))

(defn- locks-of-a-read
  "What the read of a request's locks gives for a request of n locks that a
  gate accepted: a map from each index to a new lock."
  [n]
  (into {} (map (fn [i] [i (seal/new-lock)])) (range n)))

(defn- locks-for
  "The locks of an act as a stock gives them, for a reading: one new lock
  for each value fact, each with the id of a lock of request 60."
  [reading]
  (mapv (fn [i] {:lock-id (lock-id 60 i) :lock (seal/new-lock)}) (range (:values reading))))

(defn- built
  "What the door's pure parts make of a maker's thing, with no cluster:
  {:thing the thing to append}, under the act-id given or `(act-id 70)`,
  each value sealed with a new lock; or the door's word."
  ([thing] (built thing nil))
  ([thing given]
   (let [reading (additions/reading thing given)]
     (if (:could-not reading)
       reading
       (additions/thing-to-append reading (or given (act-id 70)) (locks-for reading))))))

(defn- through
  "What comes of a maker's thing: the door's word, as `said` gives it; or
  [:goes-in word], where the word is what the declaration's check gives for
  the thing that the door would append, :ok or a gate's reason word."
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

(defn- text-of-bytes
  "A string whose one text has n bytes: n - 2 letters, and the two quotes."
  [n]
  (apply str (repeat (- n 2) "a")))

(def ^:private not-an-act-cases
  "The cases of things given to the door that are no act, one kind of thing
  after the other. A thing that ends in one of the door's words gives the
  word. A thing that goes in gives the word that the declaration's check has
  for the thing that the door would append."
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
   ["no act: :facts absent or nil: the thing goes in as given"
    (fn [] [(through (dissoc (maker) :facts)) (through (assoc (maker) :facts nil))
            (contains? (:thing (built (dissoc (maker) :facts))) :facts)])
    [[:goes-in :malformed] [:goes-in :malformed] false]]
   ["no act: :facts that is no sequence, one fact map, a set, a string: the door finds no fact, and a value inside goes plain"
    (fn [] [(mapv (fn [facts] (through (assoc (maker) :facts facts))) [value-fact #{value-fact} "facts"])
            (:facts (:thing (built (assoc (maker) :facts value-fact))))])
    [(vec (repeat 3 [:goes-in :malformed])) value-fact]]
   ["no act: facts with no end that each hold a value: the walk stops at the 257th; so it does for 257 facts; 256 are read"
    (fn [] [(through (assoc (maker) :facts (map numbered-fact (range))))
            (through (assoc (maker) :facts (map numbered-fact (range 257))))
            (:values (additions/reading (assoc (maker) :facts (map numbered-fact (range 256))) nil))])
    [{:could-not :seal-that-many-values :appended :nothing :most 256}
     {:could-not :seal-that-many-values :appended :nothing :most 256}
     256]]
   ["no act: a fact that is no map, a string, a number, a vector: as given"
    (fn [] [(through (maker "x" 7 [1])) (:facts (:thing (built (maker "x" 7 [1]))))])
    [[:goes-in :malformed] ["x" 7 [1]]]]
   ["no act: a fact that is a record, with a value"
    (fn [] (through (maker (assoc (->Thing :note-1 :text) :v "a text"))))
    (could-not :seal-a-value [:facts 0])]
   ["no act: a fact that is a record, with no value: as given"
    (fn [] (through (maker (->Thing :note-1 :text))))
    [:goes-in :malformed]]
   ["no act: a fact that already carries :sealed and :lock-id, and no :v: as given, no lock is taken, and a gate judges the maker's seal"
    (fn [] (let [fact (sealed-fact :note-1 :text)
                 act (maker fact)]
             [(through act) (:values (additions/reading act nil)) (identical? fact (first (:facts (:thing (built act)))))]))
    [[:goes-in :ok] 0 true]]
   ["no act: a fact with a value under a plain key that also holds :sealed, or :lock-id"
    (fn [] [(through (maker (assoc value-fact :sealed (bytes-of 3))))
            (through (maker (assoc value-fact :lock-id (lock-id 50 0))))
            (through (maker (assoc value-fact :sealed nil)))])
    [(could-not :add-its-parts [:facts 0 :sealed])
     (could-not :add-its-parts [:facts 0 :lock-id])
     (could-not :add-its-parts [:facts 0 :sealed])]]
   ["no act: a fact with a value under a store key that also holds :sealed: as given, since the door adds no part to it"
    (fn [] (through (maker {:e :alice :k :softland.store/depot :v :streaming :sealed (bytes-of 3)})))
    [:goes-in :store-key-sealed]]
   ["no act: an act that already carries :act-id, also a nil one"
    (fn [] [(through (assoc (maker value-fact) :act-id (act-id 1))) (through (assoc (maker value-fact) :act-id nil))])
    (vec (repeat 2 (could-not :add-its-parts :act-id)))]
   ["no act: an act that already carries :depot"
    (fn [] (through (assoc (maker value-fact) :depot :streaming)))
    (could-not :add-its-parts :depot)]
   ["no act: a value that is no value of the form: a ratio, a list, a record, a number that is not finite, a value inside 33 collections"
    (fn [] (mapv (fn [v] (through (maker (assoc value-fact :v v))))
                 [1/3 (list 1 2) (->Thing 1 2) Double/NaN (nested 33)]))
    (vec (repeat 5 (could-not :seal-a-value [:facts 0 :v])))]
   ["no act: a fact with a value whose :e is no id: sealed to the place as given, with one lock, and sent"
    (fn [] (let [act (maker {:e "n1" :k :text :v "a text"})]
             [(through act) (:values (additions/reading act nil))]))
    [[:goes-in :malformed] 1]]
   ["no act: a part that the form does not know, on the act and on a fact, and one named as the door's own words are: as given"
    (fn [] [(through (assoc (maker value-fact) :note "x"))
            (through (maker (assoc value-fact :note "x")))
            (:note (first (:facts (:thing (built (maker (assoc value-fact :note "x")))))))
            (through (assoc (maker value-fact) :could-not :read-the-act))
            (:could-not (:thing (built (assoc (maker value-fact) :could-not :read-the-act))))])
    [[:goes-in :unknown-part] [:goes-in :unknown-part] "x" [:goes-in :unknown-part] :read-the-act]]
   ["no act: 257 facts, two of them with a value: each value sealed, the whole thing sent"
    (fn [] (let [act (assoc (maker) :facts (concat [(numbered-fact 0) (numbered-fact 1)]
                                                     (map (fn [i] {:e (keyword (str "r" i)) :k :text}) (range 255))))]
             [(through act) (:values (additions/reading act nil))]))
    [[:goes-in :too-many-facts] 2]]
   ["no act: 1,025 entries of :based-on"
    (fn [] (through (assoc (maker value-fact) :based-on (into {} (map (fn [i] [[(act-id i) 0] 1])) (range 1025)))))
    [:goes-in :too-many-based-on]]
   ["no act: an id of 300 characters that is not the layer"
    (fn [] (through (maker {:e (long-id 300) :k :text})))
    [:goes-in :id-too-long]]])

(def ^:private additions-cases
  "The cases of the door's additions beside the table: the limit of sealed
  bytes; an act-id given back; the kinds of fact; the order of the steps;
  the thing that is built; the request for locks; things that throw; a
  lock's bytes; the count of a series."
  (let [lock (seal/new-lock)
        place [:alice :note-1 :text]
        given (fn [depot] [:alice depot (uuid 9)])
        permission-fact {:e :bob :k :softland.store/permission :v bobs}
        retract {:e :note-2 :k :text}
        mixed (fn [facts] (assoc makers-act :facts facts))
        three [value-fact retract permission-fact]
        hands-thing (fn [facts] (assoc makers-act :act-id (act-id 70) :depot :streaming :facts (vec facts)))
        no-value [retract permission-fact]]
    [;; the limit of sealed bytes
     ["a value whose sealed bytes are exactly the limit, 65,536, is read; one byte more is a value that the door could not seal, before any lock"
      (fn [] [(read-word (maker (assoc value-fact :v (text-of-bytes 65508))) nil)
              (read-word (maker (assoc value-fact :v (text-of-bytes 65509))) nil)])
      [:read {:could-not :seal-a-value :appended :nothing :at [:facts 0 :v] :sealed-bytes 65537 :most 65536}]]
     ["the length that seal.clj gives for a count of plain bytes is the length of what `seal` gives, for three sizes; nil for a way that is not known"
      (fn [] [(mapv (fn [n] [(seal/sealed-length seal/way-of-new-locks n)
                             (alength ^bytes (seal/seal lock place (byte-array n)))])
                    [0 1 1000])
              (seal/sealed-length 2 10)])
      [[[28 28] [29 29] [1028 1028]] nil]]
     ["the sealed bytes of the value at the limit, sealed with a real lock, are within the declaration's limit, and its check takes the thing"
      (fn [] (let [made (built (maker (assoc value-fact :v (text-of-bytes 65508))))]
               [(alength ^bytes (:sealed (first (:facts (:thing made))))) (verdict (declaration/check (:thing made)))]))
      [65536 :ok]]

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
     ["a fact under each of the three store keys keeps its plain value, and takes no lock"
      (fn [] (let [facts [{:e :alice :k :softland.store/depot :v :streaming}
                          permission-fact
                          {:e (uuid 70) :k :softland.store/lock-request :v 4}]
                   act (mixed facts)]
               [(:values (additions/reading act nil)) (= facts (:facts (:thing (built act))))
                (every? true? (map identical? facts (:facts (:thing (built act)))))]))
      [0 true true]]
     ["a fact with a value under a key in the store's namespace that is no store key: its value is sealed, and a gate refuses it either way"
      (fn [] (let [act (maker {:e :note-1 :k :softland.store/owner :v :alice})
                   fact (first (:facts (:thing (built act))))]
               [(:values (additions/reading act nil)) (contains? fact :v) (bytes? (:sealed fact)) (through act)]))
      [1 false true [:goes-in :store-key-not-known]]]
     ["a retract, a fact with no :v: as given, and it takes no lock"
      (fn [] (let [act (maker retract)]
               [(:values (additions/reading act nil)) (identical? retract (first (:facts (:thing (built act))))) (through act)]))
      [0 true [:goes-in :ok]]]
     ["a fact with a nil :v: as given, with its nil"
      (fn [] (let [fact {:e :note-2 :k :text :v nil}
                   act (maker fact)]
               [(:values (additions/reading act nil)) (first (:facts (:thing (built act))))]))
      [0 {:e :note-2 :k :text :v nil}]]
     ["one act with a store-key fact, a retract and a value: one lock, for the one value, and each fact at its position"
      (fn [] (let [act (mixed [permission-fact retract value-fact])
                   facts (:facts (:thing (built act)))]
               [(:values (additions/reading act nil))
                (mapv (fn [fact] (cond (:sealed fact) :sealed (contains? fact :v) :plain :else :retract)) facts)
                (:lock-id (nth facts 2))
                (through act)]))
      [1 [:plain :retract :sealed] (lock-id 60 0) [:goes-in :ok]]]

     ;; the order of the steps
     ["two faults, the thing is no plain map and it carries :act-id: the first step gives the word"
      (fn [] (through (assoc (->Thing :note-1 :text) :layer :alice :act-id (act-id 1))))
      (could-not :read-the-act)]
     ["two faults, it carries :act-id and has no :layer: the door's own parts come before the layer"
      (fn [] (through {:act-id (act-id 1) :facts [value-fact]}))
      (could-not :add-its-parts :act-id)]
     ["two faults, a :layer that is no id and a value that is no value: the layer comes before the walk of the facts"
      (fn [] (through (assoc (maker (assoc value-fact :v 1/3)) :layer "notes")))
      (could-not :make-an-act-id :layer)]

     ;; the thing that is built
     ["facts given as a vector: :facts is a vector, each fact at its position, the value sealed"
      (fn [] (let [facts (:facts (:thing (built (mixed (vec three)))))]
               [(vector? facts) (count facts) (bytes? (:sealed (nth facts 0))) (nth facts 1) (nth facts 2)]))
      [true 3 true retract permission-fact]]
     ["facts given as a list: :facts is a vector, each fact at its position"
      (fn [] (let [facts (:facts (:thing (built (mixed (apply list three)))))]
               [(vector? facts) (count facts) (bytes? (:sealed (nth facts 0))) (nth facts 1) (nth facts 2)]))
      [true 3 true retract permission-fact]]
     ["facts given as a lazy sequence: :facts is a vector, each fact at its position"
      (fn [] (let [facts (:facts (:thing (built (mixed (map identity three)))))]
               [(vector? facts) (count facts) (bytes? (:sealed (nth facts 0))) (nth facts 1) (nth facts 2)]))
      [true 3 true retract permission-fact]]
     ["a sealed fact is the maker's fact without :v and with :sealed and :lock-id; each other part of it stays"
      (fn [] (let [fact (assoc value-fact :replaces [(act-id 5) 0] :note "kept")
                   sealed (first (:facts (:thing (built (maker fact)))))]
               [(set (keys sealed)) (dissoc sealed :sealed)]))
      [#{:e :k :replaces :note :sealed :lock-id}
       {:e :note-1 :k :text :replaces [(act-id 5) 0] :note "kept" :lock-id (lock-id 60 0)}]]
     ["the lock opens the sealed bytes at the place [layer entity key] to the UTF-8 of the one text of the value in its normal form"
      (fn [] (let [v {:b 1.50M :a [1 2N]}
                   reading (additions/reading (maker (assoc value-fact :v v)) nil)
                   taken [{:lock-id (lock-id 60 0) :lock lock}]
                   sealed (:sealed (first (:facts (:thing (additions/thing-to-append reading (act-id 70) taken)))))]
               [(String. ^bytes (seal/open lock place sealed) StandardCharsets/UTF_8)
                (seal/opens? lock [:alice :note-2 :text] sealed)]))
      ["{:a [1 2] :b 1.5M}" false]]
     ["the declaration's check takes the thing that the door builds, and its one form holds the sealed bytes"
      (fn [] (let [thing (:thing (built (mixed three)))
                   checked (declaration/check thing)]
               [(verdict checked) (bytes? (:sealed (first (:facts (:ok checked)))))]))
      [:ok true]]
     ["a thing that the door reads is no stray on the depot that its act-id names; a record that carries a right act-id is one, and the door does not read it"
      (fn [] [(mapv (fn [[thing id]] (declaration/stray-reason (nth id 1) (:thing (built thing id))))
                    [[(maker value-fact) (given :streaming)]
                     [(maker value-fact) (given :microbatch)]
                     [(dissoc (maker) :facts) (given :streaming)]
                     [(assoc (maker value-fact) :note "x") (given :streaming)]])
              (declaration/stray-reason :streaming (assoc (->Thing :note-1 :text) :layer :alice :act-id (given :streaming)))
              (read-word (assoc (->Thing :note-1 :text) :layer :alice) (given :streaming))])
      [[nil nil nil nil] :no-act-id (could-not :read-the-act)]]
     ["each part that the maker gave is the maker's own object in the thing, and each fact that the door did not seal"
      (fn [] (let [act (assoc (mixed three) :subjects #{:bob} :based-on {[(act-id 5) 0] 7} :note {:a 1})
                   thing (:thing (built act))]
               [(every? (fn [part] (identical? (get act part) (get thing part)))
                        [:version :who :layer :permission :session :subjects :based-on :note])
                (identical? retract (nth (:facts thing) 1))
                (identical? permission-fact (nth (:facts thing) 2))
                (sort (remove (set (keys act)) (keys thing)))
                (:facts act)]))
      [true true true [:act-id :depot] three]]
     ["the digest of the thing that the door builds, for facts given as a list and as a lazy sequence, is the digest of the thing with the facts as a vector, as the hand sends it"
      (fn [] (let [wanted (digest/hex-of (hands-thing no-value))]
               [(= wanted (digest/hex-of (:thing (built (mixed (apply list no-value))))))
                (= wanted (digest/hex-of (:thing (built (mixed (map identity no-value))))))
                (= wanted (digest/hex-of (:thing (built (mixed (vec no-value))))))
                (= wanted (digest/hex-of (assoc (hands-thing no-value) :facts (apply list no-value))))]))
      [true true true false]]

     ;; the request for locks
     ["the door's request carries the version, who, permission and session of the maker's act as given; a part that the act does not hold is absent from it"
      (fn [] (let [request-id [:alice :streaming (uuid 61)]
                   whole (additions/request (additions/reading (maker value-fact) nil) request-id 4)
                   bare (additions/request (additions/reading (dissoc (maker value-fact) :session :version) nil) request-id 4)
                   odd (additions/request (additions/reading (assoc (maker value-fact) :who "nobody" :session nil) nil) request-id 4)]
               [(dissoc whole :facts) (sort (keys bare)) (select-keys odd [:who :session]) (contains? odd :session)]))
      [{:version 1 :who :alice :permission alice-own :session :alice-s1 :layer :alice
        :act-id [:alice :streaming (uuid 61)] :depot :streaming}
       [:act-id :depot :facts :layer :permission :who]
       {:who "nobody" :session nil}
       true]]
     ["the request's one fact: the count, under the store's key for a request, on the uuid of the request's own act-id; its depot is the depot slot of its act-id"
      (fn [] (let [request (additions/request (additions/reading (maker value-fact) nil) [:alice :microbatch (uuid 61)] 16)]
               [(:facts request) (:depot request)]))
      [[{:e (uuid 61) :k :softland.store/lock-request :v 16}] :microbatch]]
     ["the declaration's check takes the door's request at a count of 1 and of 256, and it is no stray; at 257 it would be refused"
      (fn [] (let [request (fn [n] (additions/request (additions/reading (maker value-fact) nil) [:alice :streaming (uuid 61)] n))]
               [(mapv (fn [n] (verdict (declaration/check (request n)))) [1 256 257])
                (declaration/stray-reason :streaming (request 1))]))
      [[:ok :ok :too-many-locks-requested] nil]]

     ;; things that throw
     ["nothing throws for facts that throw when they are read: the word names the place"
      (fn [] [(through (assoc (maker) :facts (throwing)))
              (through (assoc (maker) :facts (concat [value-fact retract] (throwing))))])
      [(could-not :read-the-act [:facts 0]) (could-not :read-the-act [:facts 2])]]
     ["nothing throws for a fact that throws when it is read, or for a thing that is anything at all"
      (fn [] [(through (maker value-fact (sorted-map "e" :note-1 "v" 1)))
              (mapv (fn [x] (:could-not (additions/reading x x))) [(throwing) (Object.) 'x {:layer (throwing)}])])
      [(could-not :read-the-act [:facts 1]) [:read-the-act :read-the-act :read-the-act :make-an-act-id]]]

     ;; a lock's bytes
     ["no lock's bytes are in the thing that the door builds, or in its request: the hex of each lock is in the text of neither"
      (fn [] (let [reading (additions/reading (mixed [value-fact (numbered-fact 1) (numbered-fact 2)]) nil)
                   taken (locks-for reading)
                   thing (:thing (additions/thing-to-append reading (act-id 70) taken))
                   request (additions/request reading [:alice :streaming (uuid 61)] 4)
                   texts (str (text/text thing) (text/text request))]
               [(count taken) (boolean (some (fn [{:keys [lock]}] (str/includes? texts (text/hex (:bytes lock)))) taken))]))
      [3 false]]

     ;; the count of a series
     ["the count of a series of act-ids: an integer of 1 or more"
      (fn [] (mapv additions/series-word [1 20 (int 3)]))
      [nil nil nil]]
     ["a count that is no integer of 1 or more: no act-id is made"
      (fn [] (mapv (fn [n] (said (additions/series-word n))) [0 -1 1.5 "3" nil]))
      (vec (repeat 5 (could-not :make-an-act-id :count)))]]))

;; ------------------------------------------------------------------ the stock

(def ^:private door-numbers
  "The door's five numbers, with the default of each, written out a second
  time."
  {:request-wait-ms 10000 :read-every-ms 200 :fewest-at-once 4
   :most-at-once-streaming 64 :most-at-once-microbatch 256})

(defn- request-id
  "The act-id of the n-th request for locks in Alice's layer, of a depot."
  ([n] (request-id :streaming n))
  ([depot n] [:alice depot (uuid (+ 1000 n))]))

(defn- with-locks-of
  "A stock after the door asked for n locks under the request `request` and
  read them, with a take of m for the act that came short. Gives
  [the stock after, the locks of the act or nil]."
  [stock request n m]
  (stock/with-locks-read (stock/with-request stock {:act-id request :digest "aa" :count n} 0)
                         request (locks-of-a-read n) m))

(defn- writing
  "A who that writes acts through one stock as the door does: each act takes
  its locks; when the stock is short, the door asks by the size rule, reads
  the locks and takes. Takes the five numbers, a depot word and the count of
  values of each act. Gives the counts of the requests in their order, the
  unused locks at the end, and whether after each request the stock held
  fewer unused locks than that request asked for."
  [numbers depot values-of-acts]
  (loop [stock stock/nothing
         acts values-of-acts
         requests []
         held? true]
    (if-let [m (first acts)]
      (if-let [[after _] (stock/take-locks stock m)]
        (recur after (rest acts) requests held?)
        (let [n (stock/ask-for numbers depot m (:given-out stock))
              [after taken] (with-locks-of stock (request-id depot (count requests)) n m)]
          (recur after (rest acts) (conj requests n)
                 (and held? (some? taken) (< (stock/unused after) n)))))
      {:requests requests :left (stock/unused stock) :held? held?})))

(defn- arrays-in
  "Each run of bytes that a thing holds, wherever it sits."
  [x]
  (cond
    (bytes? x) [x]
    (map? x) (mapcat arrays-in (concat (keys x) (vals x)))
    (coll? x) (mapcat arrays-in x)
    :else []))

(def ^:private stock-cases
  "The cases of the stock: an empty stock; taking; how many a request asks
  for; what goes into a run; the open request; the counts."
  (let [r1 (request-id 1)
        r2 (request-id 2)
        ids (fn [taken] (mapv :lock-id taken))
        four (first (with-locks-of stock/nothing r1 4 3))
        border (first (with-locks-of four r2 4 0))]
    [;; an empty stock
     ["a stock that holds nothing: no unused lock, nothing given out, no open request; the door lets such a stock go"
      (fn [] [(stock/unused stock/nothing) (stock/holds-nothing? stock/nothing) (stock/count-of [stock/nothing])
              (stock/holds-nothing? (stock/with-request stock/nothing {:act-id r1 :digest "aa" :count 4} 0))
              (stock/holds-nothing? (first (stock/take-locks (first (with-locks-of stock/nothing r1 1 0)) 1)))])
      [0 true {:unused-locks 0 :stocks 0 :open-requests 0} false false]]
     ["a take from a stock that holds nothing gives nothing"
      (fn [] (stock/take-locks stock/nothing 1))
      nil]

     ;; taking
     ["a take from one run: the next locks of the request, in the order of their indexes, each as the read gave it"
      (fn [] (let [read (locks-of-a-read 4)
                   filled (first (stock/with-locks-read (stock/with-request stock/nothing {:act-id r1 :digest "aa" :count 4} 0) r1 read 0))
                   [after taken] (stock/take-locks filled 2)]
               [(ids taken) (stock/unused after) (:given-out after)
                (mapv (fn [i {:keys [lock]}] (and (identical? (:bytes (get read i)) (:bytes lock)) (= 1 (:way lock)))) [0 1] taken)]))
      [[[r1 0] [r1 1]] 2 2 [true true]]]
     ["two takes share no lock, and a third that the stock cannot serve gives nothing"
      (fn [] (let [filled (first (with-locks-of stock/nothing r1 4 0))
                   [after-one one] (stock/take-locks filled 2)
                   [after-two two] (stock/take-locks after-one 2)]
               [(ids one) (ids two) (stock/take-locks after-two 1)]))
      [[[r1 0] [r1 1]] [[r1 2] [r1 3]] nil]]
     ["a take across the border of two runs: what is left of the old run, then the new one; nothing is left behind in the old run"
      (fn [] (let [[after taken] (stock/take-locks border 3)]
               [(ids taken) (mapv (fn [run] [(:request run) (count (:locks run))]) (:runs after)) (:given-out after)]))
      [[[r1 3] [r2 0] [r2 1]] [[r2 2]] 6]]
     ["a take that is short changes nothing: it gives nothing, and the stock is as it was"
      (fn [] [(stock/take-locks border 6) (stock/unused border) (ids (second (stock/take-locks border 5)))])
      [nil 5 [[r1 3] [r2 0] [r2 1] [r2 2] [r2 3]]]]

     ;; how many a request asks for
     ["the size rule, the first request of a who: the fewest at once, 4, in a streaming layer and in the base"
      (fn [] [(stock/ask-for door-numbers :streaming 1 0) (stock/ask-for door-numbers :microbatch 1 0)])
      [4 4]]
     ["the size rule in a streaming layer: as many as the stock has given out so far, 64 at most"
      (fn [] (mapv (fn [given-out] (stock/ask-for door-numbers :streaming 1 given-out)) [0 4 8 16 32 64 128 5000]))
      [4 4 8 16 32 64 64 64]]
     ["the size rule in a microbatch layer: as many as the stock has given out so far, 256 at most"
      (fn [] (mapv (fn [given-out] (stock/ask-for door-numbers :microbatch 1 given-out)) [8 100 256 5000]))
      [8 100 256 256]]
     ["the size rule never asks for fewer than the act needs, also past the most at once of a streaming layer"
      (fn [] (mapv (fn [[m given-out]] (stock/ask-for door-numbers :streaming m given-out)) [[10 0] [10 4] [100 8] [256 0] [3 16]]))
      [10 10 100 256 16]]
     ["the size rule never asks for more than the declaration's ceiling of one request, whatever the opener gives"
      (fn [] (let [given (stock/numbers {:most-at-once-streaming 9999 :most-at-once-microbatch 9999 :fewest-at-once 9999})]
               [(select-keys given [:fewest-at-once :most-at-once-streaming :most-at-once-microbatch])
                (stock/ask-for given :streaming 1 0) (stock/ask-for given :microbatch 1 100000)]))
      [{:fewest-at-once 256 :most-at-once-streaming 256 :most-at-once-microbatch 256} 256 256]]
     ["the door's five numbers: the defaults; a number that is missing or is no integer of 1 or more takes its default; a name that is none of the five is left out"
      (fn [] [(stock/numbers nil) stock/default-numbers
              (stock/numbers {:request-wait-ms 400 :read-every-ms 0 :fewest-at-once 2.5 :most-at-once-streaming "64" :other 1})
              (stock/numbers 7)
              (stock/numbers (sorted-map "a" 1))])
      [door-numbers door-numbers (assoc door-numbers :request-wait-ms 400) door-numbers door-numbers]]
     ["the size rule with the opener's numbers: the fewest 8 and the most 16"
      (fn [] (let [given (stock/numbers {:fewest-at-once 8 :most-at-once-streaming 16})]
               (mapv (fn [[m given-out]] (stock/ask-for given :streaming m given-out)) [[1 0] [1 12] [1 100] [20 0]])))
      [8 12 16 20]]
     ["a who that writes one value at a time in a streaming layer: 1 value is 1 request and 3 left; 20 values are requests of 4, 4, 8 and 16 and 12 left; 2,000 values are 36 requests and 48 left"
      (fn [] (mapv (fn [values] (let [{:keys [requests left]} (writing door-numbers :streaming (repeat values 1))]
                                  [(count requests) (vec (take 7 requests)) left]))
                   [1 20 2000]))
      [[1 [4] 3] [4 [4 4 8 16] 12] [36 [4 4 8 16 32 64 64] 48]]]

     ;; the bound of the stock
     ["over 200 acts of 1 to 5 values, drawn with a fixed seed, the stock holds after each request fewer unused locks than that request asked for"
      (fn [] (let [random (java.util.Random. 7)
                   acts (vec (repeatedly 200 (fn [] (inc (.nextInt random 5)))))
                   {:keys [requests left held?]} (writing door-numbers :streaming acts)]
               [held? (< left (peek requests)) (<= (reduce + acts) (reduce + requests)) (every? (fn [n] (<= 4 n 64)) requests)]))
      [true true true true]]

     ;; what goes into a run
     ["what goes into a run: a lock that the read gave with nil for its bytes, and a lock of a way that the code does not know, are left out"
      (fn [] (let [read (-> (locks-of-a-read 5)
                            (assoc-in [1 :bytes] nil)
                            (assoc-in [3 :way] 2))
                   [after taken] (stock/with-locks-read (stock/with-request stock/nothing {:act-id r1 :digest "aa" :count 5} 0) r1 read 3)]
               [(ids taken) (stock/unused after) (:open after)]))
      [[[r1 0] [r1 2] [r1 4]] 0 nil]]
     ["a read of locks that came back ends its request, whatever it gave: with no lock that can seal the stock holds no run, the request is forgotten, and the act's take gives nothing"
      (fn [] (let [read (into {} (map (fn [[i lock]] [i (assoc lock :way 2)])) (locks-of-a-read 4))
                   [after taken] (stock/with-locks-read (stock/with-request stock/nothing {:act-id r1 :digest "aa" :count 4} 0) r1 read 1)
                   [short none] (with-locks-of four r2 2 4)]
               [taken (:runs after) (:open after) (stock/holds-nothing? after)
                none (stock/unused short) (:open short)]))
      [nil [] nil true nil 3 nil]]
     ["a lock that was taken is in the stock no more: the stock after a take holds none of the taken bytes"
      (fn [] (let [filled (first (with-locks-of stock/nothing r1 4 0))
                   [after taken] (stock/take-locks filled 3)
                   left (arrays-in after)]
               [(count left)
                (boolean (some (fn [{:keys [lock]}] (some (fn [b] (identical? b (:bytes lock))) left)) taken))
                (count (arrays-in filled))]))
      [1 false 4]]

     ;; the open request
     ["a request is written into its stock before its append, in doubt, with its moment; a request in doubt is kept for the door's wait from that moment and no longer"
      (fn [] (let [written (stock/with-request stock/nothing {:act-id r1 :digest "aa" :count 4 :other "left out"} 1000)
                   open (:open written)]
               [open (:unused-locks (stock/count-of [written])) (:open-requests (stock/count-of [written]))
                (mapv (fn [read-began] (stock/kept-long-enough? open read-began 500)) [1000 1499 1500 9000])]))
      [{:act-id r1 :digest "aa" :count 4 :state :in-doubt :since 1000} 0 1 [false false true true]]]
     ["an append that returned makes the request sent, and a request that is sent is never given up; an append that threw leaves it in doubt from the moment of the throw"
      (fn [] (let [written (stock/with-request stock/nothing {:act-id r1 :digest "aa" :count 4} 1000)
                   sent (stock/request-sent written)
                   threw (stock/request-in-doubt-since written 3000)]
               [(select-keys (:open sent) [:state :act-id]) (stock/kept-long-enough? (:open sent) 999999 500)
                (select-keys (:open threw) [:state :since]) (stock/kept-long-enough? (:open threw) 3499 500)
                (stock/kept-long-enough? (:open threw) 3500 500)]))
      [{:state :sent :act-id r1} false {:state :in-doubt :since 3000} false true]]
     ["a request is forgotten: by word, and by the read of its locks; the locks that the stock held stay"
      (fn [] (let [written (stock/with-request four {:act-id r2 :digest "aa" :count 4} 1000)
                   [after taken] (stock/with-locks-read written r2 (locks-of-a-read 4) 2)]
               [(:open (stock/without-request written)) (stock/unused (stock/without-request written))
                (:open after) (ids taken) (stock/unused after)
                (stock/request-sent four) (stock/request-in-doubt-since four 5)]))
      [nil 1 nil [[r1 3] [r2 0]] 3 four four]]

     ;; the counts
     ["how many locks a stock has given out is the sum of its takes, across requests"
      (fn [] (let [[one _] (with-locks-of stock/nothing r1 4 3)
                   [two _] (stock/take-locks one 1)
                   [three _] (with-locks-of two r2 8 5)]
               (mapv :given-out [stock/nothing one two three])))
      [0 3 4 9]]
     ["the count of the door's stocks is three numbers, and prints no byte of a lock"
      (fn [] (let [open (stock/with-request stock/nothing {:act-id r2 :digest "aa" :count 4} 0)
                   counted (stock/count-of [four border open stock/nothing])
                   printed (pr-str counted)]
               [counted
                (some? (re-matches #"\{(:[a-z-]+ \d+(, )?)+\}" printed))
                (boolean (some (fn [b] (str/includes? printed (text/hex b))) (arrays-in [four border])))]))
      [{:unused-locks 6 :stocks 2 :open-requests 1} true false]]]))

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
  (shown "the store's namespace is" declaration/store-namespace
         "| a sealed fact under a key in it that the store does not know:"
         (pr-str (mapv (juxt identity sealed-under) [:softland.store/owner :softland.store/class]))
         "| a tool's own sealed fact under a bare word or under another namespace is a fact under a plain key:"
         (pr-str (mapv (juxt identity sealed-under) tools-own-keys)))
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
                                    (dissoc :act-id :depot)
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
         (str/join " | " (map (fn [[what gate-depot _ expected]] (str what " [" (name gate-depot) " gate] " expected)) first-act-rows))))

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

(deftest the-batch
  (let [n (+ (holds (map (fn [[what x expected]] [(str "what the depot derives: " what) (fn [] (batch/first-entity x)) expected])
                         batch-first-rows))
             (holds batch-turn-cases)
             (holds arrival-cases)
             (holds reply-cases)
             (holds meeting-cases)
             (holds deciding-cases)
             (holds routing-cases))]
    (shown "the pure code of a batch," n "cases: what the depot derives from a thing, never throwing; the turn is position x tasks + partition; the base is later than the gate's last stamp and than each carried stamp, and 10 seconds ahead at most; an act's stamp is the base plus its turn")
    (shown "what the depot derives:" (str/join " | " (map (fn [[what _ e]] (str what " " (pr-str e))) batch-first-rows)))
    (shown "the standing on arrival:"
           (str/join " | " (map (fn [[what _ _ _ expected]] (str what " " (name (:standing expected)) (when-let [w (:word expected)] (str " " w))))
                                arrival-rows)))
    (shown "who goes to the meeting task:" (str/join " | " (map first (take 9 meeting-cases))))
    (shown "one copy of a lock's bytes: a used lock is two rows, the home's row with no bytes first and the whole row beside the value; the read of a request's locks gives a used lock with no bytes; a second value under it is refused :lock-already-seals-a-value")
    (shown "the routing id of an act-id:" (pr-str (record/routing-id [:alice :streaming (uuid 5)])) "for a streaming one,"
           (str (record/routing-id [:base :microbatch (uuid 5)])) "for a microbatch one; a row is of a closed batch when its stamp is not later than closed-through")))

(deftest a-batch-is-decided-in-one-order
  (let [orders 200
        made (orders-compared batch-fixture orders)
        making (orders-compared making-fixture orders)
        words (:words made)
        record (:record made)
        rows-under (fn [table k] (keep (fn [[task rows]] (when-let [row (get rows k)] [task row])) (get record table)))
        lock-key (fn [i] (record/lock-key :base (uuid 900) i))
        words-of (fn [& ns] (set (map words ns)))
        fact-22 (fn [i] (nth (:facts (first (filter #(= (base-id 22) (:act-id %)) (:things batch-fixture)))) i))
        fact-23 (fn [i] (nth (:facts (first (filter #(= (base-id 23) (:act-id %)) (:things batch-fixture)))) i))]
    (is (= [0 nil] [(:differ made) (:first made)])
        "a batch into a base that is made: in each order of arrival the record is the record of deciding one at a time")
    (is (= [0 nil] [(:differ making) (:first making)])
        "a batch into a base that is not made: in each order of arrival the record is the record of deciding one at a time")
    (is (= #{:accepted :replaced-fact-not-current} (words-of 1 2)) "two acts of one batch that replace the same fact: one winner")
    (is (= #{:accepted} (words-of 3 4)) "two acts that write one place and name nothing are both accepted")
    (is (= [:accepted :accepted :permission-does-not-stand :accepted] (mapv words [5 6 7 8]))
        "a give and then an act under it: accepted; an act and then the give: the act is refused")
    (is (= #{:accepted :lock-already-seals-a-value} (words-of 10 11)) "two acts of one batch under one lock: one seals with it")
    (is (= :accepted (words 12)) "a request for locks is accepted")
    (is (contains? #{:lock-does-not-open-the-value :lock-not-made-for-this-layer} (words 13))
        "a use of a lock in the batch that asks for it is refused")
    (is (= [:accepted (stamp/pack (- now 5000) 0)]
           [(words 14) (:stamp (second (first (rows-under :answers (record/answer-key :base (uuid 801))))))])
        "one act-id under two things has one answer; a repeat leaves the answer of the first decision")
    (is (= [:layer-belongs-to-the-other-gate :layer-not-made :not-sealed nil] (mapv words [15 16 17 18]))
        "an act for a layer of the other gate, made and not made; a thing that the declaration refuses; a stray has no answer")
    (is (= [:carried-stamp-too-far-ahead :permission-does-not-stand :who-not-the-permissions-holder] (mapv words [19 20 21]))
        "no standing: the 10-second rule first, a permission that no act gave, a who that is not the holder")
    (is (= [:accepted :replaced-fact-not-current :accepted] (mapv words [22 23 24])) "an act on three tasks accepted, one refused, a value on its lock's home")
    (is (= [true true true]
           (mapv (fn [i]
                   (let [fact (fact-22 i)
                         index (second (:lock-id fact))
                         rows (rows-under :locks (lock-key index))
                         with-bytes (filter (fn [[_ row]] (contains? row :bytes)) rows)
                         [task row] (first with-bytes)]
                     (and (= 1 (count with-bytes))
                          (= task (task-of (:e fact)))
                          (every? (fn [[_ r]] (= (uuid 22) (:seals-act r))) rows)
                          (some? (seal/open (record/lock-of row) [:base (:e fact) (:k fact)] (:sealed fact))))))
                 (range 3)))
        "accepted whole: each lock has one row with its bytes, on the task of its value, and the bytes open the value")
    (is (= [1 true (uuid 24)]
           (let [rows (rows-under :locks (lock-key 10))]
             [(count rows) (contains? (second (first rows)) :bytes) (:seals-act (second (first rows)))]))
        "a value on the entity that is its lock's request's uuid: the one row holds the bytes and the fact")
    (is (= [[] [false false false]]
           [(vec (mapcat (fn [i] (filter (fn [[_ row]] (= (uuid 23) (:by-act row)))
                                         (rows-under :facts (record/place-key :base (:e (fact-23 i)) (:k (fact-23 i))))))
                         (range 3)))
            (mapv (fn [i] (boolean (some (fn [[_ row]] (record/seals-a-fact? row)) (rows-under :locks (lock-key i))))) [7 8 9])])
        "refused whole: no row of the act on any task, and its three locks are free")
    (is (= [:accepted true] [(words 25) (contains? #{:accepted :replaced-fact-not-current} (words 26))])
        "one act writes a place and names nothing, another replaces the fact that was there before: the order of the turns says whether the second is stale")
    (is (= #{:accepted :permission-fact-not-beneath-the-acts-own} (set (map (:words making) [31 32])))
        "two making acts of the base in one batch: one is accepted")
    (shown "the decisions of a batch of" (count (:things batch-fixture)) "things, run in memory in" orders
           "orders of arrival, are each time those of deciding its acts one at a time in the order of their turns:" (pr-str words))
    (shown "two acts of one batch that replace the same fact, arriving on two tasks: the same winner in every order of arrival:"
           (pr-str (select-keys words [1 2])) "| two under one lock:" (pr-str (select-keys words [10 11])))
    (shown "a batch into a base that is not made, in" orders "orders: an act before the making, two making acts, acts after:" (pr-str (:words making)))))

(deftest the-doors-words
  (let [n (holds door-words-cases)]
    (shown "the door's words," n "cases. Its own fourteen, each read after \"the door could not\":"
           (str/join " " (map (fn [[word appended]] (str word (when (= :not-known appended) " (the door does not know whether the depot holds the thing)")))
                              (sort-by key door-words)))
           "| none is a gate's reason word or a word of a stray")
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
                                (filter (fn [[what _ _]] (re-find #"^no act: (a map with no :layer|a thing whose :layer cannot|a record with a :layer|a fact that is a record, with a value|a fact with a value under a store|an act that already carries :depot|a fact with a value whose :e|1,025)" what))
                                        not-an-act-cases))))
    (shown "the door's two bounds, before any lock is taken: one act holds" additions/most-values-in-one-act
           "values to seal at most, and a sealed value holds" declaration/max-sealed-bytes "bytes at most, by the length that seal.clj gives:"
           (pr-str (said (additions/reading (maker (assoc value-fact :v (text-of-bytes 65509))) nil))))
    (shown "the thing that the door builds: the maker's act with :act-id and :depot, :facts as a vector, each value fact sealed; the declaration's check takes it and it is no stray:"
           (pr-str (comparable (update (:thing (built (maker value-fact {:e :note-2 :k :text})))
                                       :facts (fn [facts] (mapv (fn [fact] (if (:sealed fact) (assoc fact :sealed (symbol (str (alength ^bytes (:sealed fact)) "-bytes"))) fact)) facts))))))
    (shown "the door's request for 4 locks under that act's who and permission:"
           (pr-str (additions/request (additions/reading (maker value-fact) nil) [:alice :streaming (uuid 61)] 4)))))

(deftest the-doors-stock
  (let [n (holds stock-cases)]
    (shown "the door's stock," n "cases: a take gives the next locks of the oldest run, across the border of two runs, or nothing; a lock that was taken is in the stock no more; a read of locks that came back ends its request")
    (shown "the size rule: a who that writes one value at a time in a streaming layer makes requests of"
           (pr-str (:requests (writing door-numbers :streaming (repeat 200 1))))
           "for 200 values; in the base" (pr-str (:requests (writing door-numbers :microbatch (repeat 600 1)))) "for 600")
    (shown "the door's five numbers:" (pr-str (into (sorted-map) stock/default-numbers)))))

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
  (the-decision)
  (the-batch)
  (a-batch-is-decided-in-one-order)
  (the-doors-words)
  (the-doors-additions)
  (the-doors-stock))

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
