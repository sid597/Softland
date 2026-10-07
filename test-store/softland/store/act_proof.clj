(ns softland.store.act-proof
  "The proof of the act. It needs no cluster. One command, from the repository
  root:

    clojure -M:store -m softland.store.act-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The form that it checks is the maker's act, with plain values, as the
  decisions of 6 October 2026 give it (src-store/README.md). The proof seals
  nothing, makes no act-id and opens nothing.

  Approach. Each check is a case: what it is about, a function of no
  argument that calls the act's code, and what that function must give.
  Where a case reads a declaration, it reads it from `softland.store.act`.
  Where the proof writes the form out a second time, as literals, it does so
  on purpose: a change of the form is then made in two places.

  State. The proof owns one atom: the lines that it showed, for the recap.

  What it shows, in order: the declared shape and the rule, and that the
  check reads them; acts that pass, from the least act to one with each
  optional part, each in its one form; one refused act for each fault, and
  each reason word earned; the order of the checks, which is the order of
  the declarations; that nothing throws; the limits at their edges; ids and
  values."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [softland.store.act :as act])
  (:import [java.util UUID]))

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

(defn- outcome
  "Takes a case. Gives what its function gave, or [:threw the class] when it
  threw."
  [[_ f _]]
  (try (f)
       (catch Throwable t [:threw (.getSimpleName (class t))])))

(defn- holds
  "Checks each case of a group: its function must give what the case says.
  Gives the number of cases."
  [cases]
  (doseq [[what _ expected :as c] cases]
    (is (= expected (outcome c)) what))
  (count cases))

(defn- verdict
  "What a parse gave, as one word: :ok, or the reason of its refusal."
  [parsed]
  (if (and (map? parsed) (contains? parsed :ok)) :ok (:refuse parsed)))

(defn- verdicts
  "Takes rows [what x word]. Gives them as cases of the check: the parse of x
  must give that word, :ok or a reason."
  [rows]
  (mapv (fn [[what x word]] [what (fn [] (verdict (act/parse x))) word]) rows))

(defn- words
  "Rows [what x word] as text: each what with its word."
  [rows]
  (str/join " | " (map (fn [[what _ word]] (str what " " word)) rows)))

;; ---------------------------------------------------------------- fixtures

(defn- uuid
  "A fixed UUID, so that an act-id of the proof is the same in each run."
  [n]
  (UUID. 0 n))

(defn- an-act-id
  "An act-id for a layer and a class, with a fixed UUID."
  [layer class n]
  [layer class (uuid n)])

(def ^:private earlier-act-id "The act-id of an earlier act in Alice's layer." (an-act-id :alice :streaming 2))
(def ^:private other-act-id "The act-id of another earlier act in Alice's layer." (an-act-id :alice :streaming 3))
(def ^:private alice-own "Alice's permission in her own layer." [:alice :alice :alice])
(def ^:private a-fact "One fact with a plain value." {:e :note-1 :k :note :v "a text"})

;; A record, for the checks that a record is no act, no fact and no value.
(defrecord Thing [e k])

(defn- act-of
  "Alice's act into her own layer, in her session, with these facts and no
  optional part."
  [& facts]
  {:version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :facts (vec facts)})

(defn- one-form
  "The one form of `act-of` for facts that are in their one form."
  [& facts]
  {:version 1 :who :alice :layer :alice :permission alice-own :session :alice-s1
   :based-on {} :because-of nil :claimed-when nil :subjects #{} :facts (vec facts)})

(def ^:private good
  "The least act: version, who, layer, permission, session and one fact. Each
  refused act differs from it in one thing."
  (act-of a-fact))

(defn- nested
  "The number 1 inside n vectors."
  [n]
  (loop [v 1 i 0] (if (< i n) (recur [v] (inc i)) v)))

(defn- throwing
  "A sequence that throws when it is read."
  []
  (lazy-seq (throw (ex-info "this sequence throws when it is read" {}))))

(defn- subjects
  "A set of n subjects."
  [n]
  (set (map #(keyword (str "p" %)) (range n))))

(defn- deep-permission
  "A permission id in Alice's layer that holds n permission ids, itself
  counted."
  [n]
  (reduce (fn [parent _] [:alice :alice :alice parent]) alice-own (range (dec n))))

;; --------------------------------------------------------------- the parts

(def ^:private declared
  "The declared shape, written out here a second time, with each word and
  each number as a literal. So a change of the form is made in two places on
  purpose."
  {:act [:parts
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
   :fact [:parts
          [:e :id]
          [:k :key]
          [:v :value {:optional true}]
          [:replaces :fact-id {:optional true}]]
   :act-id [:tuple :id [:enum :streaming :microbatch] :uuid]
   :fact-id [:tuple :act-id [:int {:min 0}]]
   :permission-id [:or [:tuple :id :id :id] [:tuple :id :id :id :permission-id]]
   :carried-stamp [:int {:min 0 :below 4611686018427387904}]})

(def ^:private parts-cases
  "The two declarations, and what they say as sets. Each is written out here
  a second time, so a change of the form is made in two places on purpose."
  [["the declared shape of an act and of each thing that an act holds"
    (fn [] act/shapes) declared]
   ["the one rule, with its reason"
    (fn [] act/rules) [[:permission-is-in-the-acts-layer :permission-not-in-layer]]]
   ["the ten parts of an act"
    (fn [] act/act-parts)
    #{:version :who :layer :permission :session :based-on :because-of :claimed-when :subjects :facts}]
   ["the four parts of a fact"
    (fn [] act/fact-parts) #{:e :k :v :replaces}]
   ["the door's additions and the seal are no parts: no act-id or class on an act; no seal, lock id, layer or mark on a fact"
    (fn [] (mapv #(contains? %1 %2)
                 [act/act-parts act/act-parts act/fact-parts act/fact-parts act/fact-parts act/fact-parts]
                 [:act-id :class :sealed :lock-id :layer :mark]))
    [false false false false false false]]
   ["the seven reason words, read from the two declarations"
    (fn [] act/reasons)
    #{:malformed :unknown-part :unknown-version :no-permission :no-session :empty-act
      :permission-not-in-layer}]
   ["the version that this code knows, and the four limits: subjects, collections, a carried stamp, the depth of a permission"
    (fn [] [act/version act/max-subjects act/max-depth act/max-carried-stamp act/max-permission-depth])
    [1 256 32 (bit-shift-left 1 62) 4]]])

(def ^:private whole
  "An act with each optional part. It is built with subvectors, narrow
  integers and a list of facts, so its one form differs in class from what
  was given."
  {:version (int 1)
   :who :alice :layer :alice
   :permission (subvec [:x :alice :alice :alice [:operator :alice :alice]] 1)
   :session :alice-s1
   :based-on {[(subvec (into [:x] earlier-act-id) 1) (int 0)] (int 41)}
   :because-of (subvec (into [:x] earlier-act-id) 1)
   :claimed-when (int 1700000)
   :subjects #{:alice :bob}
   :facts (list {:e :note-1 :k :note :v {:count (int 2) :path (subvec [0 1 2] 1)}
                 :replaces [earlier-act-id (short 0)]}
                {:e :note-2 :k :note})})

(def ^:private whole-form
  "The one form of `whole`."
  {:version 1 :who :alice :layer :alice
   :permission [:alice :alice :alice [:operator :alice :alice]]
   :session :alice-s1
   :based-on {[earlier-act-id 0] 41}
   :because-of earlier-act-id
   :claimed-when 1700000
   :subjects #{:alice :bob}
   :facts [{:e :note-1 :k :note :v {:count 2 :path [1 2]} :replaces [earlier-act-id 0]}
           {:e :note-2 :k :note :v nil :replaces nil}]})

;; ---------------------------------------------------------- acts that pass

(def ^:private passing-rows
  "Acts that the check takes, from the least act to one with each optional
  part, each with its one form."
  (let [w (uuid 11) l (uuid 12) s (uuid 13) e (uuid 14)]
    [["the least act: version, who, layer, permission, session and one fact"
      good
      (one-form {:e :note-1 :k :note :v "a text" :replaces nil})]
     ["a retract: a fact with no :v"
      (act-of {:e :note-1 :k :note :replaces [earlier-act-id 0]})
      (one-form {:e :note-1 :k :note :v nil :replaces [earlier-act-id 0]})]
     ["three facts, each with a plain value under any key: a text, a map, a UUID"
      (act-of a-fact {:e :alice-s1 :k :lease :v {:count 1}} {:e :note-2 :k :link :v (uuid 5)})
      (one-form {:e :note-1 :k :note :v "a text" :replaces nil}
                {:e :alice-s1 :k :lease :v {:count 1} :replaces nil}
                {:e :note-2 :k :link :v (uuid 5) :replaces nil})]
     ["an act whose ids are UUIDs: who, the layer, the permission, the session, an entity and a subject"
      {:version 1 :who w :layer l :permission [w l l] :session s :subjects #{w}
       :facts [{:e e :k :note :v 1}]}
      {:version 1 :who w :layer l :permission [w l l] :session s :based-on {} :because-of nil
       :claimed-when nil :subjects #{w} :facts [{:e e :k :note :v 1 :replaces nil}]}]
     ["an act under a permission that a parent permission granted"
      (assoc good :who :bob :permission [:bob :alice :alice alice-own])
      (assoc (one-form {:e :note-1 :k :note :v "a text" :replaces nil})
             :who :bob :permission [:bob :alice :alice alice-own])]
     ["an act that replaces a fact, says what it was based on and what it came from"
      (assoc (act-of (assoc a-fact :replaces [earlier-act-id 0]))
             :based-on {[earlier-act-id 0] 41 [other-act-id 1] 7} :because-of other-act-id)
      (assoc (one-form {:e :note-1 :k :note :v "a text" :replaces [earlier-act-id 0]})
             :based-on {[earlier-act-id 0] 41 [other-act-id 1] 7} :because-of other-act-id)]
     ["an act with each optional part, given with subvectors, narrow integers and a list of facts"
      whole
      whole-form]]))

(defn- classes-of
  "The class names of the vectors and the numbers in the one form of `whole`."
  [ok]
  (let [[fact-id stamp] (first (:based-on ok))
        fact (first (:facts ok))]
    (mapv #(.getSimpleName (class %))
          [(:version ok) (:permission ok) (:because-of ok)
           fact-id (first fact-id) (second fact-id) stamp (:claimed-when ok)
           (:replaces fact) (second (:replaces fact))
           (get-in fact [:v :count]) (get-in fact [:v :path]) (:facts ok)])))

(def ^:private passing-cases
  "The passing rows as cases, and two things that hold of the one form."
  (into (mapv (fn [[what x form]] [what (fn [] (act/parse x)) {:ok form}]) passing-rows)
        [["in the one form of the act with each optional part, each vector is plain and each number is a long"
          (fn [] (classes-of (:ok (act/parse whole))))
          ["Long" "PersistentVector" "PersistentVector" "PersistentVector" "PersistentVector" "Long"
           "Long" "Long" "PersistentVector" "Long" "Long" "PersistentVector" "PersistentVector"]]
         ["to parse a one form again gives the same"
          (fn [] (mapv (fn [[_ x _]] (let [ok (:ok (act/parse x))] (= {:ok ok} (act/parse ok)))) passing-rows))
          (vec (repeat (count passing-rows) true))]]))

;; --------------------------------------------------------------- refusals

(def ^:private refusal-rows
  "One refused act for each fault. Each differs from the least act in one
  thing only."
  [;; parts that the form does not name
   ["another part" (assoc good :signature "x") :unknown-part]
   ["an act-id: the door makes it, and it is no part" (assoc good :act-id earlier-act-id) :unknown-part]
   ["a class: the door adds it, and it is no part" (assoc good :class :streaming) :unknown-part]
   ["a part for expected versions" (assoc good :expected-versions {}) :unknown-part]
   ;; the version
   ["version 2" (assoc good :version 2) :unknown-version]
   ["no version" (dissoc good :version) :unknown-version]
   ;; who and the layer
   ["no :who" (dissoc good :who) :malformed]
   ["a :who that is a string" (assoc good :who "alice") :malformed]
   ["no layer" (dissoc good :layer) :malformed]
   ["a layer that is a string" (assoc good :layer "alice") :malformed]
   ;; the permission
   ["no permission" (dissoc good :permission) :no-permission]
   ["a permission of two items" (assoc good :permission [:alice :alice]) :malformed]
   ["a permission that is a list" (assoc good :permission '(:alice :alice :alice)) :malformed]
   ["a permission with an item that is a string" (assoc good :permission [:alice "alice" :alice]) :malformed]
   ["a permission whose parent is no permission" (assoc good :permission [:alice :alice :alice [:alice]]) :malformed]
   ["a permission in another layer" (assoc good :permission [:alice :bob :bob]) :permission-not-in-layer]
   ["a permission whose parent is in the act's layer, but itself in another"
    (assoc good :permission [:alice :bob :bob alice-own]) :permission-not-in-layer]
   ;; the session
   ["no session" (dissoc good :session) :no-session]
   ["a session that is a string" (assoc good :session "s1") :malformed]
   ;; based on
   [":based-on that is no map" (assoc good :based-on [[earlier-act-id 0] 1]) :malformed]
   ["a key of :based-on that is no fact id" (assoc good :based-on {[:alice 0] 1}) :malformed]
   ["a carried stamp that is a double" (assoc good :based-on {[earlier-act-id 0] 1.5}) :malformed]
   ;; because of, claimed when, subjects
   ["a :because-of that is no act-id" (assoc good :because-of [:alice 0]) :malformed]
   ["a :because-of with a scheme word, four slots" (assoc good :because-of [:alice :streaming :act (uuid 2)]) :malformed]
   ["a :because-of with no class" (assoc good :because-of [:alice nil (uuid 2)]) :malformed]
   ["a :claimed-when that is a double" (assoc good :claimed-when 1.5) :malformed]
   ["a :claimed-when that is a big integer" (assoc good :claimed-when 1N) :malformed]
   ["subjects that are no set" (assoc good :subjects [:bob]) :malformed]
   ["a subject that is a string" (assoc good :subjects #{"bob"}) :malformed]
   ;; the facts
   ["no facts" (dissoc good :facts) :malformed]
   ["facts that are a map" (assoc good :facts a-fact) :malformed]
   ["facts with none in it" (assoc good :facts []) :empty-act]
   ;; a fact
   ["a fact that is a number" (act-of 42) :malformed]
   ["a fact that is a vector" (act-of [:note-1 :note "a text"]) :malformed]
   ["a fact that is a record" (act-of (->Thing :note-1 :note)) :malformed]
   ["another part on a fact" (act-of (assoc a-fact :colour :red)) :unknown-part]
   ["sealed bytes on a fact: the act holds plain values" (act-of (assoc a-fact :sealed (byte-array [1 2 3]))) :unknown-part]
   ["a lock id on a fact" (act-of (assoc a-fact :lock-id [earlier-act-id 0])) :unknown-part]
   ["a layer on a fact" (act-of (assoc a-fact :layer :alice)) :unknown-part]
   ["a mark on a fact" (act-of (assoc a-fact :mark #{:own-row})) :unknown-part]
   ["a fact with no entity" (act-of (dissoc a-fact :e)) :malformed]
   ["a fact with no key" (act-of (dissoc a-fact :k)) :malformed]
   ["an entity that is a string" (act-of (assoc a-fact :e "note-1")) :malformed]
   ["a key that is a string" (act-of (assoc a-fact :k "note")) :malformed]
   ["a key that is a UUID" (act-of (assoc a-fact :k (uuid 9))) :malformed]
   ["a value that is a list" (act-of (assoc a-fact :v '(1 2))) :malformed]
   ["a value that is a ratio" (act-of (assoc a-fact :v 1/3)) :malformed]
   ["a value that is a byte array" (act-of (assoc a-fact :v (byte-array [1 2 3]))) :malformed]
   ["a :replaces that is no fact id" (act-of (assoc a-fact :replaces [:alice 0])) :malformed]])

(def ^:private refusal-cases "The refusal rows as cases." (verdicts refusal-rows))

;; ----------------------------------------------------------------- order

(def ^:private order-rows
  "Acts with two faults. The order of the two declarations gives the reason:
  first the shape, part by part and fact by fact; then the rule."
  [["another part, and version 2: a part that the form does not name, first"
    (assoc good :signature "x" :version 2) :unknown-part]
   ["version 2, and no permission" (-> good (assoc :version 2) (dissoc :permission)) :unknown-version]
   ["a :who that is a string, and no permission" (-> good (assoc :who "alice") (dissoc :permission)) :malformed]
   ["no permission, and no session" (dissoc good :permission :session) :no-permission]
   ["no session, and no fact" (-> good (dissoc :session) (assoc :facts [])) :no-session]
   ["257 subjects, and no fact" (assoc good :subjects (subjects 257) :facts []) :malformed]
   ["a permission in another layer, and no session: the shape comes before the rule"
    (-> good (assoc :permission [:alice :bob :bob]) (dissoc :session)) :no-session]
   ["a permission in another layer, and no fact: the shape comes before the rule"
    (assoc good :permission [:alice :bob :bob] :facts []) :empty-act]
   ["a permission in another layer, and a mark on a fact: the shape comes before the rule"
    (assoc good :permission [:alice :bob :bob] :facts [(assoc a-fact :mark #{})]) :unknown-part]
   ["two facts with a fault: the first one's"
    (act-of (assoc a-fact :layer :alice) {:e :note-2 :k "note" :v 1}) :unknown-part]
   ["the same two, the other way round"
    (act-of {:e :note-2 :k "note" :v 1} (assoc a-fact :layer :alice)) :malformed]
   ["in one fact, another part and a key that is a string: the part that the form does not name, first"
    (act-of (assoc a-fact :k "note" :mark #{})) :unknown-part]])

(def ^:private order-cases "The order rows as cases." (verdicts order-rows))

;; --------------------------------------------------------- nothing throws

(def ^:private not-acts
  "Things that are not acts, with the reason that the check gives for each."
  [["nil" nil :malformed]
   ["a number" 42 :malformed]
   ["a string" "act" :malformed]
   ["a vector" [1 2] :malformed]
   ["a list" '(1 2) :malformed]
   ["a record" (->Thing :note-1 :note) :malformed]
   ["an object" (Object.) :malformed]
   ["a map whose keys are strings" {"version" 1} :unknown-part]
   ["a sorted map whose keys are strings" (sorted-map "version" 1) :unknown-part]
   ["a value inside 2000 collections" (act-of (assoc a-fact :v (nested 2000))) :malformed]
   ["facts that throw when they are read" (assoc good :facts (throwing)) :malformed]])

(def ^:private garbage
  "Things that fit no declared shape, a thing that throws when it is read
  among them."
  [nil "act" '(1 2) (->Thing :note-1 :note) (Object.) {"version" 1} (nested 2000)
   (throwing) (assoc good :facts (throwing))])

(def ^:private total-cases
  "Nothing throws: the check, and the test of each declared shape, are given
  things that are not acts, and each gives back data."
  (into (verdicts not-acts)
        [["the test of each declared shape says no to each thing of no shape"
          (fn [] (into (sorted-map) (for [s (keys act/shapes)] [s (mapv #(act/has-shape? s %) garbage)])))
          (into (sorted-map) (for [s (keys act/shapes)] [s (vec (repeat (count garbage) false))]))]
         ["the test of a shape that is not declared says no"
          (fn [] (act/has-shape? :nothing good))
          false]]))

;; -------------------------------------------------------------- the limits

(def ^:private limit-rows
  "Each limit at its edge."
  [["256 subjects" (assoc good :subjects (subjects 256)) :ok]
   ["257 subjects" (assoc good :subjects (subjects 257)) :malformed]
   ["a value inside 32 collections" (act-of (assoc a-fact :v (nested 32))) :ok]
   ["a value inside 33 collections" (act-of (assoc a-fact :v (nested 33))) :malformed]
   ["a carried stamp of 2^62 - 1" (assoc good :based-on {[earlier-act-id 0] (dec act/max-carried-stamp)}) :ok]
   ["a carried stamp of 2^62" (assoc good :based-on {[earlier-act-id 0] act/max-carried-stamp}) :malformed]
   ["a carried stamp below zero" (assoc good :based-on {[earlier-act-id 0] -5}) :malformed]
   ["a permission four deep" (assoc good :permission (deep-permission 4)) :ok]
   ["a permission five deep" (assoc good :permission (deep-permission 5)) :malformed]])

(def ^:private limit-cases "The limit rows as cases." (verdicts limit-rows))

;; ---------------------------------------------------------- ids and values

(def ^:private id-rows
  "Ids and keys in an act. An id is a readable keyword or a UUID; a key is a
  readable keyword only."
  [["who as a UUID" (assoc good :who (uuid 7) :permission [(uuid 7) :alice :alice]) :ok]
   ["the layer as a UUID, with the permission in it" (assoc good :layer (uuid 8) :permission [:alice (uuid 8) (uuid 8)]) :ok]
   ["the session as a UUID" (assoc good :session (uuid 9)) :ok]
   ["a subject as a UUID" (assoc good :subjects #{(uuid 10)}) :ok]
   ["an entity as a UUID" (act-of (assoc a-fact :e (uuid 9))) :ok]
   ["who as a string" (assoc good :who "alice") :malformed]
   ["an entity as a string" (act-of (assoc a-fact :e "note-1")) :malformed]
   ["an id as a keyword that does not read back" (assoc good :session (keyword "a b")) :malformed]
   ["a key as a keyword" (act-of (assoc a-fact :k :note)) :ok]
   ["a key as a UUID" (act-of (assoc a-fact :k (uuid 9))) :malformed]
   ["a key as a string" (act-of (assoc a-fact :k "note")) :malformed]
   ["a key as a keyword that does not read back" (act-of (assoc a-fact :k (keyword "a b"))) :malformed]])

(def ^:private normal-inputs
  "Values that are not in their normal form, and one that is big past the
  long range."
  [(int 7) (short 7) (byte 7) 7N (biginteger 7) 100000000000000000000N -0.0 1.500M 100M
   (subvec [0 1 2] 1) (sorted-map :b 2 :a 1) (sorted-set 3 1)])

(def ^:private value-cases
  "Ids, keys, values, the one normal form, and the shapes that an act refers
  to: an act-id, a fact id and a permission id."
  (into (verdicts id-rows)
        [["a retract is a fact with no :v, and a fact with a nil :v is the same retract"
          (fn [] [(-> (act/parse (act-of {:e :note-1 :k :note})) :ok :facts first)
                  (-> (act/parse (act-of {:e :note-1 :k :note :v nil})) :ok :facts first)])
          [{:e :note-1 :k :note :v nil :replaces nil} {:e :note-1 :k :note :v nil :replaces nil}]]
         ["readable keywords: not one with a space, an empty one, one with a slash in its namespace, a string, nil or a symbol"
          (fn [] (mapv act/readable-keyword? [:a :a/b :a.b/c-d? (keyword "a b") (keyword "") (keyword "x/y" "z") "a" nil 'a]))
          [true true true false false false false false false]]
         ["values"
          (fn [] (mapv act/value? [nil true "s" :a/b (uuid 1) 7 (int 7) 7N (biginteger 7) 1.5 1.5M {:a [1 #{2}]}]))
          (vec (repeat 12 true))]
         ["not values: a list, a symbol, a character, a ratio, a Java float, not-a-number, infinity, a keyword that does not read back, a record, a list inside a map, a date, bytes"
          (fn [] (mapv act/value? ['(1 2) 'sym \c 1/3 (float 1.5) Double/NaN Double/POSITIVE_INFINITY
                                   (keyword "a b") (->Thing 1 2) {:a '(1)} (java.util.Date. 0) (byte-array 1)]))
          (vec (repeat 12 false))]
         ["a value inside 32 collections, and inside 33"
          (fn [] [(act/value? (nested 32)) (act/value? (nested 33))])
          [true false]]
         ["each value in its one normal form: = to what was given, and its class"
          (fn [] (mapv (fn [v] (let [n (act/normal-value v)] [(= v n) (.getSimpleName (class n))])) normal-inputs))
          [[true "Long"] [true "Long"] [true "Long"] [true "Long"] [true "Long"] [true "BigInt"] [true "Double"]
           [true "BigDecimal"] [true "BigDecimal"] [true "PersistentVector"] [true "PersistentArrayMap"]
           [true "PersistentHashSet"]]]
         ["the shape of an act-id, as an act refers to one: [layer class uuid], the layer an id"
          (fn [] (mapv #(act/has-shape? :act-id %)
                       [[:alice :streaming (uuid 1)] [:group :microbatch (uuid 1)] [(uuid 3) :streaming (uuid 1)]
                        [:alice :streaming :act (uuid 1)] [:alice nil (uuid 1)] [:alice :sideways (uuid 1)]
                        [:alice :streaming "id"] ["alice" :streaming (uuid 1)] (list :alice :streaming (uuid 1))]))
          [true true true false false false false false false]]
         ["fact ids: an act-id and an index from 0; not a negative index, not a big one, not a list, not an id in place of an act-id"
          (fn [] (mapv #(act/has-shape? :fact-id %)
                       [[earlier-act-id 0] [earlier-act-id (int 3)] [earlier-act-id -1] [earlier-act-id 1N]
                        (list earlier-act-id 0) [:alice 0]]))
          [true true false false false false]]
         ["permission ids: three ids, UUIDs too; with a parent; four deep; not two items, not a list, not five deep"
          (fn [] (mapv #(act/has-shape? :permission-id %)
                       [alice-own [(uuid 1) (uuid 2) (uuid 2)] (deep-permission 2) (deep-permission 4)
                        [:alice :alice] '(:alice :alice :alice) (deep-permission 5)]))
          [true true true true false false false]]]))

;; ------------------------------------------------------------- the checks

(deftest the-parts
  (holds parts-cases)
  (shown "the declared shape of an act, part by part, in the order of the checks:")
  (doseq [[part shape opts] (rest (:act act/shapes))]
    (shown "  " part (pr-str shape) (if opts (pr-str opts) "")))
  (shown "a fact:" (pr-str (vec (rest (:fact act/shapes)))))
  (shown "an act-id:" (pr-str (:act-id act/shapes)) "| a fact id:" (pr-str (:fact-id act/shapes))
         "| a permission id:" (pr-str (:permission-id act/shapes))
         "| a carried stamp:" (pr-str (:carried-stamp act/shapes)))
  (shown "the rule:" (pr-str act/rules))
  (shown "read from the declarations:" (count act/act-parts) "parts on an act," (count act/fact-parts)
         "on a fact," (count act/reasons) "reason words | version" act/version
         "| limits:" (pr-str [act/max-subjects act/max-depth act/max-carried-stamp act/max-permission-depth])))

(deftest the-declaration-drives-the-check
  ;; These checks read the declaration and then ask the check, so they hold
  ;; for each part that the declaration names, a part that is added later too.
  ;; They start from the one form of `whole`, which has each part.
  (doseq [[part _ opts] (rest (:act act/shapes))]
    (let [reason (:reason opts :malformed)]
      (is (= (if (:optional opts) :ok (:absent opts reason)) (verdict (act/parse (dissoc whole-form part))))
          (str "an act without " part ": taken when the part is optional, else the declared reason"))
      (is (= reason (verdict (act/parse (assoc whole-form part (Object.)))))
          (str "an act with a thing of no shape in " part ": the declared reason"))))
  (doseq [[part _ opts] (rest (:fact act/shapes))]
    (let [reason (:reason opts :malformed)
          fact (first (:facts whole-form))]
      (is (= (if (:optional opts) :ok (:absent opts reason)) (verdict (act/parse (act-of (dissoc fact part)))))
          (str "a fact without " part ": taken when the part is optional, else the declared reason"))
      (is (= reason (verdict (act/parse (act-of (assoc fact part (Object.))))))
          (str "a fact with a thing of no shape in " part ": the declared reason"))))
  (is (= :ok (verdict (act/parse whole-form))) "the act that these are made from is taken")
  (shown "the check reads the declaration: for each of the" (count act/act-parts) "parts of an act and the"
         (count act/fact-parts) "parts of a fact, a thing without the part and a thing with a thing of no shape in it"
         "get what the declaration gives"))

(deftest acts-that-pass
  (let [n (holds passing-cases)]
    (shown n "checks of acts that pass. Each act, in its one form:")
    (doseq [[what x _] passing-rows]
      (shown "  " (str what ":") (pr-str (:ok (act/parse x)))))
    (shown "the one form of the act with each optional part has only plain vectors and longs:"
           (= #{"Long" "PersistentVector"} (set (classes-of (:ok (act/parse whole)))))
           "| each one form, parsed again, is the same:"
           (every? (fn [[_ x _]] (let [ok (:ok (act/parse x))] (= {:ok ok} (act/parse ok)))) passing-rows))))

(deftest refusals-by-reason
  (let [n (holds refusal-cases)
        earned (frequencies (map #(nth % 2) refusal-rows))
        first-for (fn [word] (some (fn [[what _ w]] (when (= w word) what)) refusal-rows))]
    (is (= act/reasons (set (keys earned))) "each reason word of the declarations comes from an act that earns it")
    (shown n "acts refused, each for one fault. By reason:" (pr-str (into (sorted-map) earned)))
    (shown "each of the" (count act/reasons) "reason words read from the declarations came from an act that earns it:"
           (= act/reasons (set (keys earned))))
    (doseq [word (sort act/reasons)]
      (shown "  " word "for example:" (first-for word)))))

(deftest the-order-of-the-checks
  (let [n (holds order-cases)]
    (shown "the order of the declarations gives the reason," n "acts with two faults:" (words order-rows))))

(deftest nothing-throws
  (holds total-cases)
  (shown "nothing throws. The check refuses" (count not-acts) "things that are not acts:" (words not-acts))
  (shown "the test of each of the" (count act/shapes) "declared shapes says no to" (count garbage)
         "things of no shape, facts that throw when they are read among them, and throws nothing"))

(deftest the-limits
  (holds limit-cases)
  (shown "the limits at their edges:" (words limit-rows)))

(deftest ids-and-values
  (holds value-cases)
  (shown "ids and keys:" (words id-rows))
  (shown "a retract, as given with no :v, in its one form:"
         (pr-str (-> (act/parse (act-of {:e :note-1 :k :note})) :ok :facts first)))
  (shown "each value in its one normal form, with its class:"
         (pr-str (mapv (fn [v] (let [n (act/normal-value v)] [n (.getSimpleName (class n))])) normal-inputs))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-parts)
  (the-declaration-drives-the-check)
  (acts-that-pass)
  (refusals-by-reason)
  (the-order-of-the-checks)
  (nothing-throws)
  (the-limits)
  (ids-and-values))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.act-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
