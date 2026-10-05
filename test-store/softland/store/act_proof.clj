(ns softland.store.act-proof
  "The proof of the act. It needs no cluster. One command, from the repository
  root:

    clojure -M:store -m softland.store.act-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  A second command runs the same cases through the rig's envelope, and prints
  each case where the two give different things:

    clojure -M:store -m softland.store.act-proof beside-the-rig

  That run asserts nothing and exits with status 0: a difference is a
  finding, not a failure. It loads `rig.store.envelope` when it runs; the
  proof itself requires nothing of the rig.

  Approach. Each check is a case: what it is about, a function of an
  implementation, and what that function must give. An implementation is a
  map from a role, such as :parse, to a function or a definition. The proof
  gives each case the act's own functions (`here`). The comparison gives the
  same cases the rig's functions under the same roles (`rig-roles`). So the
  two runs use the same cases, and nobody keeps a second list by hand.

  The proof seals nothing and opens nothing. Where an act needs sealed bytes,
  any bytes do, because the act's code never reads them.

  State. The proof owns one atom: the lines that it showed, for the recap.

  What it shows, in order: the parts; acts that pass, each in its one form;
  one refused act for each fault, with its reason; the order of the checks;
  that nothing throws; the limits at their edges; names; the stand-in sealed
  form; the digest, and what it does not see; ids and values."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [clojure.walk :as walk]
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

;; ------------------------------------------------------- an implementation

(def ^:private here
  "The act's own functions and definitions, each under its role."
  {:parse act/parse :digest act/digest :sealed-view act/sealed-view
   :value-fact? act/value-fact? :readable-keyword? act/readable-keyword?
   :value? act/value? :normal-value act/normal-value :canonical-text act/canonical-text
   :make-name act/make-name :name-for act/name-for :name-tag act/name-tag
   :act-tag act/act-tag :valid-name? act/valid-name?
   :crossing-name act/crossing-name :landing-name act/landing-name
   :fact-id? act/fact-id? :permission-id? act/permission-id?
   :act-parts act/act-parts :fact-parts act/fact-parts :store-keys act/store-keys
   :placed-keys act/placed-keys :marks act/marks :classes act/classes
   :schemes act/schemes :reasons act/reasons
   :limits [act/max-subjects act/max-depth act/max-carried-stamp act/max-permission-depth]})

(defn- comparable
  "Takes a result. Gives it as a value that = can compare and a line can
  print: each byte array as [:bytes its bytes]."
  [x]
  (walk/postwalk #(if (bytes? %) [:bytes (vec %)] %) x))

(defn- outcome
  "Takes an implementation and a case. Gives what the function of the case
  gave, made comparable; or [:threw the class] when it threw."
  [impl [_ f _]]
  (try (comparable (f impl))
       (catch Throwable t [:threw (.getSimpleName (class t))])))

(defn- holds
  "Checks each case of a group against the act's own functions. Gives the
  number of cases."
  [cases]
  (doseq [[what _ expected :as c] cases]
    (is (= (comparable expected) (outcome here c)) what))
  (count cases))

(defn- verdict
  "What a parse gave, as one word: :ok, or the reason of its refusal."
  [parsed]
  (if (and (map? parsed) (contains? parsed :ok)) :ok (:refuse parsed)))

(defn- verdicts
  "Takes rows [what x word]. Gives them as cases of `parse`: the parse of x
  must give that word, :ok or a reason."
  [rows]
  (mapv (fn [[what x word]] [what (fn [impl] (verdict ((:parse impl) x))) word]) rows))

;; ---------------------------------------------------------------- fixtures

(def ^:private made
  "The scheme word of a made name, from the act's one constant."
  act/made-scheme)

(defn- uuid
  "A fixed UUID, so that a name of the proof is the same in each run."
  [n]
  (UUID. 0 n))

(defn- a-name
  "A name for a layer and a class, with a fixed id."
  [layer class n]
  [layer class made (uuid n)])

(def ^:private alice-name "The name of Alice's act in her own layer." (a-name :alice :by-layer 1))
(def ^:private earlier-name "The name of an earlier act in that layer." (a-name :alice :by-layer 2))
(def ^:private lease-name "The name of the lease whose locks seal Alice's values." (a-name :alice :by-layer 3))
(def ^:private alice-own "Alice's permission in her own layer." [:alice :alice :alice])
(def ^:private bytes-a "Sealed bytes of one sealing." (byte-array [1 2 3]))
(def ^:private bytes-b "Sealed bytes of another sealing, or of another value." (byte-array [9 9 9 9]))

;; A record, for the checks that a record is no act, no fact and no value.
(defrecord Thing [e k])

(defn- sealed-fact
  "A value fact as a door sends it: sealed bytes, and the id of a lock of the
  lease."
  ([e] (sealed-fact e bytes-a 0))
  ([e sealed i] {:e e :k :note :sealed sealed :lock-id [lease-name i]}))

(defn- act-of
  "Alice's act into her own layer, with these facts and no optional part."
  [& facts]
  {:version 1 :name alice-name :who :alice :layer :alice :class :by-layer
   :permission alice-own :facts (vec facts)})

(defn- one-form
  "The one form of `act-of` for facts that are in their one form."
  [& facts]
  {:version 1 :name alice-name :who :alice :layer :alice :class :by-layer
   :permission alice-own :session nil :based-on {} :because-of nil
   :claimed-when nil :subjects #{} :facts (vec facts)})

(def ^:private good
  "The act that each refused act differs from in one thing."
  (act-of (sealed-fact :note-1)))

(defn- nested
  "The number 1 inside n vectors."
  [n]
  (loop [v 1 i 0] (if (< i n) (recur [v] (inc i)) v)))

(defn- throwing
  "A sequence that throws when it is read."
  []
  (lazy-seq (throw (ex-info "this sequence throws when it is read" {}))))

;; --------------------------------------------------------------- the parts

(def ^:private parts-cases
  "The words that the form allows. Each set is written out here a second
  time, so a change of the form is made in two places on purpose."
  [["the twelve parts of an act" :act-parts
    #{:version :name :who :layer :class :permission :session :based-on
      :because-of :claimed-when :subjects :facts}]
   ["the eight parts of a fact" :fact-parts #{:e :k :v :sealed :lock-id :replaces :mark :layer}]
   ["the fifteen store keys" :store-keys
    #{:forget :lock-grain :class :promote-request :crossed :permission :revoke :kind :owner
      :person :forget-person :lease :session-closed :members :grammar}]
   ["the six placed keys" :placed-keys #{:class :lock-grain :forget :crossed :person :forget-person}]
   ["the two marks" :marks #{:die-with-any :own-row}]
   ["the two classes" :classes #{:by-layer :by-entity}]
   ["the three scheme words" :schemes #{made :crossing :landing}]
   ["the eleven reason words" :reasons
    #{:malformed :unknown-part :unknown-version :bad-name :reserved-who :no-permission
      :reserved-scheme :empty-act :not-sealed :fact-outside-the-acts-layer :mis-tagged}]
   ["the four limits: subjects, collections, a carried stamp, the depth of a permission" :limits
    [256 32 (bit-shift-left 1 62) 4]]])

;; ---------------------------------------------------------- acts that pass

(def ^:private whole
  "An act with each optional part. It is built with subvectors, narrow
  integers and a list of facts, so its one form differs in class from what
  was given."
  {:version (int 1)
   :name (subvec (into [:x] alice-name) 1)
   :who :alice :layer :alice :class :by-layer
   :permission (subvec [:x :alice :alice :alice [:operator :alice :alice]] 1)
   :session :alice-s1
   :based-on {[(subvec (into [:x] earlier-name) 1) (int 0)] (int 41)}
   :because-of (subvec (into [:x] earlier-name) 1)
   :claimed-when (int 1700000)
   :subjects #{:alice :bob}
   :facts (list {:e :alice-s1 :k :lease :v {:count (int 2)}}
                {:e :note-1 :k :note :sealed bytes-a :lock-id [lease-name (int 0)]
                 :mark #{:own-row} :replaces [earlier-name (short 0)] :layer :alice}
                {:e :note-2 :k :note :v nil})})

(def ^:private whole-form
  "The one form of `whole`."
  {:version 1 :name alice-name :who :alice :layer :alice :class :by-layer
   :permission [:alice :alice :alice [:operator :alice :alice]]
   :session :alice-s1
   :based-on {[earlier-name 0] 41}
   :because-of earlier-name
   :claimed-when 1700000
   :subjects #{:alice :bob}
   :facts [{:e :alice-s1 :k :lease :v {:count 2} :replaces nil :mark #{}}
           {:e :note-1 :k :note :sealed bytes-a :lock-id [lease-name 0]
            :replaces [earlier-name 0] :mark #{:own-row} :layer :alice}
           {:e :note-2 :k :note :v nil :replaces nil :mark #{}}]})

(defn- classes-of
  "The class names of the vectors and the numbers in the one form of `whole`."
  [ok]
  (let [[fact-id stamp] (first (:based-on ok))
        sealed (second (:facts ok))]
    (mapv #(.getSimpleName (class %))
          [(:version ok) (:name ok) (:permission ok) (:because-of ok)
           fact-id (first fact-id) (second fact-id) stamp (:claimed-when ok)
           (:lock-id sealed) (second (:lock-id sealed))
           (:replaces sealed) (second (:replaces sealed))
           (get-in ok [:facts 0 :v :count]) (:facts ok)])))

(def ^:private layer-making
  "An act of store-key facts only: the operator makes Bob's layer."
  {:version 1 :name (a-name :bob :by-layer 5) :who :operator :layer :bob :class :by-layer
   :permission [:operator :bob :bob]
   :facts [{:e :bob :k :kind :v :personal} {:e :bob :k :owner :v :bob}]})

(def ^:private placed
  "An act that the store places itself: its one key is a placed key, and its
  name has no class."
  {:version 1 :name [:alice nil made (uuid 6)] :who :operator :layer :alice :class :by-layer
   :permission [:operator :alice :alice]
   :facts [{:e :alice :k :lock-grain :v :per-act}]})

(def ^:private into-a-group
  "Alice's act into a layer that is placed by entity."
  {:version 1 :name (a-name :group :by-entity 4) :who :alice :layer :group :class :by-entity
   :permission [:alice :group :group]
   :facts [{:e :note-1 :k :note :sealed bytes-a :lock-id [(a-name :group :by-entity 8) 0]}]})

(def ^:private passing-cases
  "Acts that the check takes, each with what its one form holds."
  [["one fact with a sealed value, by a person, into her own layer"
    (fn [impl] ((:parse impl) good))
    {:ok (one-form {:e :note-1 :k :note :sealed bytes-a :lock-id [lease-name 0] :replaces nil :mark #{}})}]
   ["the same act as said, with a plain value, through its sealed view"
    (fn [impl] ((:parse impl) ((:sealed-view impl) (act-of {:e :note-1 :k :note :v "a text"}))))
    {:ok (one-form {:e :note-1 :k :note :sealed (byte-array 0) :lock-id [alice-name 0] :replaces nil :mark #{}})}]
   ["an act of store-key facts only"
    (fn [impl] (:facts (:ok ((:parse impl) layer-making))))
    [{:e :bob :k :kind :v :personal :replaces nil :mark #{}} {:e :bob :k :owner :v :bob :replaces nil :mark #{}}]]
   ["a retract"
    (fn [impl] (:facts (:ok ((:parse impl) (act-of {:e :note-1 :k :note :replaces [earlier-name 0]})))))
    [{:e :note-1 :k :note :v nil :replaces [earlier-name 0] :mark #{}}]]
   ["an act of three facts, with a mark on one"
    (fn [impl] (mapv :mark (:facts (:ok ((:parse impl) (act-of (sealed-fact :note-1 bytes-a 0)
                                                                 (assoc (sealed-fact :note-2 bytes-b 1) :mark #{:die-with-any})
                                                                 (sealed-fact :note-3 bytes-a 2)))))))
    [#{} #{:die-with-any} #{}]]
   ["an act that the store places itself, under a name with no class"
    (fn [impl] (verdict ((:parse impl) placed)))
    :ok]
   ["an act into a layer that is placed by entity"
    (fn [impl] (verdict ((:parse impl) into-a-group)))
    :ok]
   ["an act with each optional part, in its one form"
    (fn [impl] ((:parse impl) whole))
    {:ok whole-form}]
   ["in that one form each vector is plain and each number is a long"
    (fn [impl] (classes-of (:ok ((:parse impl) whole))))
    ["Long" "PersistentVector" "PersistentVector" "PersistentVector"
     "PersistentVector" "PersistentVector" "Long" "Long" "Long"
     "PersistentVector" "Long" "PersistentVector" "Long" "Long" "PersistentVector"]]
   ["to parse that one form again gives the same"
    (fn [impl] (let [ok (:ok ((:parse impl) whole))] (= {:ok ok} ((:parse impl) ok))))
    true]
   ["the least act: the parts that it left out come back empty"
    (fn [impl] ((:parse impl) (act-of {:e :note-1 :k :note})))
    {:ok (one-form {:e :note-1 :k :note :v nil :replaces nil :mark #{}})}]])

;; --------------------------------------------------------------- refusals

(def ^:private landing
  "A landing in a layer that is placed by entity. The store makes a landing;
  no maker does."
  (assoc into-a-group :name [:group :by-entity :landing (uuid 7)]))

(def ^:private refusal-rows
  "One refused act for each fault. Each differs from an act that passes in
  one thing only."
  [;; the act
   ["another part" (assoc good :signature "x") :unknown-part]
   ["a part for expected versions" (assoc good :expected-versions nil) :unknown-part]
   ["the rig's key for what the act stood on" (assoc good :stood-on {}) :unknown-part]
   ["version 2" (assoc good :version 2) :unknown-version]
   ["no version" (dissoc good :version) :unknown-version]
   [":who the store" (assoc good :who :store) :reserved-who]
   [":who that is no id" (assoc good :who "alice") :malformed]
   ["no :who" (dissoc good :who) :malformed]
   ["a layer that is no id" (assoc good :layer "alice") :malformed]
   ["no class" (dissoc good :class) :malformed]
   ["a class that is not one of the two" (assoc good :class :sideways) :malformed]
   ["no permission" (dissoc good :permission) :no-permission]
   ["a permission of two items" (assoc good :permission [:alice :alice]) :malformed]
   ["a permission with an item that is no id" (assoc good :permission [:alice "alice" :alice]) :malformed]
   ["a permission that is a list" (assoc good :permission '(:alice :alice :alice)) :malformed]
   ["a session that is no id" (assoc good :session "s1") :malformed]
   ["a :because-of that is no name" (assoc good :because-of [:alice 0]) :malformed]
   ["a :claimed-when that is a double" (assoc good :claimed-when 1.5) :malformed]
   ["a :claimed-when that is a big integer" (assoc good :claimed-when 1N) :malformed]
   ["subjects that are no set" (assoc good :subjects [:bob]) :malformed]
   ["a subject that is no id" (assoc good :subjects #{"bob"}) :malformed]
   ["no facts" (dissoc good :facts) :malformed]
   ["facts that are a map" (assoc good :facts {:e :note-1}) :malformed]
   ["facts with none in it" (assoc good :facts []) :empty-act]
   ;; based on
   [":based-on that is no map" (assoc good :based-on [[earlier-name 0] 1]) :malformed]
   ["a key of :based-on that is no fact id" (assoc good :based-on {[:alice 0] 1}) :malformed]
   ["a carried stamp that is a double" (assoc good :based-on {[earlier-name 0] 1.5}) :malformed]
   ;; the name
   ["a name of three items" (assoc good :name (subvec alice-name 0 3)) :bad-name]
   ["a name that is a string" (assoc good :name "alice") :bad-name]
   ["a name whose layer is no id" (assoc-in good [:name 0] "alice") :bad-name]
   ["a name with an unknown class" (assoc-in good [:name 1] :sideways) :bad-name]
   ["a name with an unknown scheme" (assoc-in good [:name 2] :mine) :bad-name]
   ["a name whose id is no UUID" (assoc-in good [:name 3] "id") :bad-name]
   ["a name with the scheme :crossing" (assoc-in good [:name 2] :crossing) :reserved-scheme]
   ["a name with the scheme :landing" (assoc-in good [:name 2] :landing) :reserved-scheme]
   ["a landing in a layer that is placed by entity" landing :reserved-scheme]
   ["a name made for another layer" (assoc good :layer :alice-hand) :mis-tagged]
   ["a name made for another class" (assoc good :class :by-entity) :mis-tagged]
   ["a name with no class, for ordinary facts" (assoc-in good [:name 1] nil) :mis-tagged]
   ["a name with a class, for an act that the store places itself"
    (act-of {:e :alice :k :lock-grain :v :per-act}) :mis-tagged]
   ;; a fact
   ["a fact that is a number" (act-of 42) :malformed]
   ["a fact that is a vector" (act-of [:note-1 :note "x"]) :malformed]
   ["a fact that is a record" (act-of (->Thing :note-1 :note)) :malformed]
   ["another part on a fact" (act-of (assoc (sealed-fact :note-1) :colour :red)) :unknown-part]
   ["an entity that is no id" (act-of (assoc (sealed-fact :note-1) :e "note-1")) :malformed]
   ["a key that is no id" (act-of (assoc (sealed-fact :note-1) :k "note")) :malformed]
   ["a plain value on a value fact" (act-of {:e :note-1 :k :note :v "x"}) :not-sealed]
   ["a plain value beside a sealed fact" (act-of (sealed-fact :note-1) {:e :note-2 :k :note :v "y"}) :not-sealed]
   ["a :v, though nil, beside sealed bytes" (act-of (assoc (sealed-fact :note-1) :v nil)) :not-sealed]
   ["sealed that is not bytes" (act-of (assoc (sealed-fact :note-1) :sealed "abc")) :not-sealed]
   ["sealed bytes and no lock id" (act-of (dissoc (sealed-fact :note-1) :lock-id)) :not-sealed]
   ["sealed bytes and a lock id that is not well formed" (act-of (assoc (sealed-fact :note-1) :lock-id [:x 0])) :not-sealed]
   ["a lock id and no sealed bytes" (act-of {:e :note-1 :k :note :lock-id [lease-name 0]}) :not-sealed]
   ["sealed bytes on a store key" (act-of (assoc (sealed-fact :alice-s1) :k :lease)) :not-sealed]
   ["a lock id on a store key" (act-of {:e :alice-s1 :k :lease :v {:count 1} :lock-id [lease-name 0]}) :not-sealed]
   ["a store key's value that is a list" (act-of {:e :alice-s1 :k :lease :v '(1 2)}) :malformed]
   ["a store key's value that is a ratio" (act-of {:e :alice-s1 :k :lease :v 1/3}) :malformed]
   ["a :replaces that is no fact id" (act-of (assoc (sealed-fact :note-1) :replaces [:alice 0])) :malformed]
   ["a mark that is no set" (act-of (assoc (sealed-fact :note-1) :mark [:own-row])) :malformed]
   ["a mark that is not one of the two" (act-of (assoc (sealed-fact :note-1) :mark #{:shiny})) :malformed]
   ["a fact's layer that is no id" (act-of (assoc (sealed-fact :note-1) :layer "alice")) :malformed]
   ["a fact that names another layer" (act-of (assoc (sealed-fact :note-1) :layer :alice-hand))
    :fact-outside-the-acts-layer]])

(def ^:private refusal-cases "The refusal rows as cases." (verdicts refusal-rows))

;; ----------------------------------------------------------------- order

(def ^:private order-rows
  "Acts with two faults. The earlier check gives the reason."
  [["another part, and version 2" (assoc good :signature "x" :version 2) :unknown-part]
   ["version 2, and a name that is not valid" (assoc good :version 2 :name "alice") :unknown-version]
   ["a name that is not valid, and :who the store" (assoc good :name "alice" :who :store) :bad-name]
   [":who the store, and no permission" (-> good (assoc :who :store) (dissoc :permission)) :reserved-who]
   ["no permission, and a session that is no id" (-> good (dissoc :permission) (assoc :session "s1")) :no-permission]
   ["a scheme of the store, and no fact" (-> good (assoc-in [:name 2] :crossing) (assoc :facts [])) :reserved-scheme]
   ["no fact, and a name made for another layer" (assoc good :facts [] :layer :alice-hand) :empty-act]
   ["two facts with a fault: the first one's"
    (act-of (assoc (sealed-fact :note-1) :colour :red) {:e :note-2 :k :note :v "y"}) :unknown-part]
   ["the same two, the other way round"
    (act-of {:e :note-2 :k :note :v "y"} (assoc (sealed-fact :note-1) :colour :red)) :not-sealed]
   ["in one fact: a plain value, and another layer"
    (act-of {:e :note-1 :k :note :v "x" :layer :alice-hand}) :not-sealed]
   ["a fact with a fault, and a name made for another class"
    (assoc (act-of {:e :note-1 :k :note :v "x"}) :class :by-entity) :not-sealed]])

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
   ["a map whose keys are strings" {"version" 1} :unknown-part]
   ["a sorted map whose keys are strings" (sorted-map "version" 1) :malformed]])

(def ^:private total-cases
  "Nothing throws: the check, the sealed view and the test of a name are each
  given things that are not acts, and each gives back data."
  (into (verdicts not-acts)
        [["the check, for a store key's value inside 2000 collections"
          (fn [impl] (verdict ((:parse impl) (act-of {:e :alice-s1 :k :lease :v (nested 2000)}))))
          :malformed]
         ["the check, for facts that throw when they are read"
          (fn [impl] (verdict ((:parse impl) (assoc good :facts (throwing)))))
          :malformed]
         ["the sealed view gives each of these back as it is"
          (fn [impl] (mapv (fn [[_ x _]] (identical? x ((:sealed-view impl) x))) not-acts))
          (vec (repeat (count not-acts) true))]
         ["the sealed view, for facts that throw when they are read: the act as it is"
          (fn [impl] (let [x (assoc good :facts (throwing))] (identical? x ((:sealed-view impl) x))))
          true]
         ["the test of a name says no to each thing that is no name"
          (fn [impl] (mapv (:valid-name? impl) [nil 42 "alice" {:a 1} (apply list alice-name) (throwing) (nested 2000)]))
          [false false false false false false false]]]))

;; -------------------------------------------------------------- the limits

(defn- subjects
  "A set of n subjects."
  [n]
  (set (map #(keyword (str "p" %)) (range n))))

(defn- deep-permission
  "A permission id that holds n permission ids, itself counted."
  [n]
  (reduce (fn [parent _] [:alice :alice :alice parent]) [:alice :alice :alice] (range (dec n))))

(def ^:private limit-rows
  "Each limit at its edge."
  [["256 subjects" (assoc good :subjects (subjects 256)) :ok]
   ["257 subjects" (assoc good :subjects (subjects 257)) :malformed]
   ["a store key's value inside 32 collections" (act-of {:e :alice-s1 :k :lease :v (nested 32)}) :ok]
   ["a store key's value inside 33 collections" (act-of {:e :alice-s1 :k :lease :v (nested 33)}) :malformed]
   ["a carried stamp of 2^62 - 1" (assoc good :based-on {[earlier-name 0] (dec act/max-carried-stamp)}) :ok]
   ["a carried stamp of 2^62" (assoc good :based-on {[earlier-name 0] act/max-carried-stamp}) :malformed]
   ["a carried stamp below zero" (assoc good :based-on {[earlier-name 0] -5}) :ok]
   ["a permission four deep" (assoc good :permission (deep-permission 4)) :ok]
   ["a permission five deep" (assoc good :permission (deep-permission 5)) :malformed]])

(def ^:private limit-cases "The limit rows as cases." (verdicts limit-rows))

;; ------------------------------------------------------------------ names

(defn- name-shape
  "A made name with its random id left out: its layer, class and scheme."
  [nm]
  (subvec nm 0 3))

(def ^:private name-cases
  "Names: making one, its tag, the names that the store makes from a request,
  and what is not a name."
  [["a made name: four slots, the scheme of a made name, a random id of version 4, valid, and its tag"
    (fn [impl] (let [nm ((:make-name impl) :alice :by-layer)]
                 [(count nm) (nth nm 2) (.version ^UUID (nth nm 3)) ((:valid-name? impl) nm) ((:name-tag impl) nm)]))
    [4 made 4 true [:alice :by-layer]]]
   ["two made names differ"
    (fn [impl] (not= ((:make-name impl) :alice :by-layer) ((:make-name impl) :alice :by-layer)))
    true]
   ["a name from all four slots"
    (fn [impl] ((:make-name impl) :alice nil :crossing (uuid 1)))
    [:alice nil :crossing (uuid 1)]]
   ["a name for an act of placed keys only has no class"
    (fn [impl] (name-shape ((:name-for impl) {:layer :alice :class :by-layer
                                              :facts [{:e :alice :k :lock-grain :v :per-act}
                                                      {:e :alice :k :class :v :by-entity}]})))
    [:alice nil made]]
   ["a name for an ordinary act has the class"
    (fn [impl] (name-shape ((:name-for impl) good)))
    [:alice :by-layer made]]
   ["one ordinary fact among placed ones: the class"
    (fn [impl] (name-shape ((:name-for impl) {:layer :alice :class :by-layer
                                              :facts [{:e :alice :k :lock-grain :v :per-act}
                                                      {:e :note-1 :k :note :v "x"}]})))
    [:alice :by-layer made]]
   ["no fact: the class"
    (fn [impl] (name-shape ((:name-for impl) {:layer :alice :class :by-layer :facts []})))
    [:alice :by-layer made]]
   ["facts that are no sequence: the class, and no throw"
    (fn [impl] (name-shape ((:name-for impl) {:layer :alice :class :by-layer :facts 5})))
    [:alice :by-layer made]]
   ["the tag of an ordinary act, and of an act that the store places itself"
    (fn [impl] [((:act-tag impl) good) ((:act-tag impl) placed)])
    [[:alice :by-layer] [:alice nil]]]
   ["a crossing's name: the request's layer and id, and no class"
    (fn [impl] ((:crossing-name impl) alice-name))
    [:alice nil :crossing (uuid 1)]]
   ["a landing's name: the target, the request's id, and by entity when no class is given"
    (fn [impl] [((:landing-name impl) alice-name :group) ((:landing-name impl) alice-name :base :by-layer)])
    [[:group :by-entity :landing (uuid 1)] [:base :by-layer :landing (uuid 1)]]]
   ["names: with a class, with none, and with each scheme of the store"
    (fn [impl] (mapv (:valid-name? impl) [alice-name [:alice nil made (uuid 1)]
                                          [:alice nil :crossing (uuid 1)] [:group :by-entity :landing (uuid 1)]]))
    [true true true true]]
   ["not names: an unknown scheme, an unknown class, a list, three slots, an id that is no UUID, a layer that is no id"
    (fn [impl] (mapv (:valid-name? impl) [(assoc alice-name 2 :mine) (assoc alice-name 1 :sideways)
                                          (apply list alice-name) (subvec alice-name 0 3)
                                          (assoc alice-name 3 "id") (assoc alice-name 0 "alice")]))
    [false false false false false false]]
   ["fact ids: a name and an index from 0; not a negative index, not a big one, not a list"
    (fn [impl] (mapv (:fact-id? impl) [[alice-name 0] [alice-name (int 3)] [alice-name -1] [alice-name 1N]
                                       (list alice-name 0) [:alice 0]]))
    [true true false false false false]]])

;; ------------------------------------------------ the stand-in sealed form

(def ^:private said
  "Alice's act as she says it: a plain value with a mark, a store key's
  fact, a retract, and a fact that is sealed already."
  (act-of {:e :note-1 :k :note :v "a text" :mark #{:own-row}}
          {:e :alice-s1 :k :lease :v {:count 1}}
          {:e :note-2 :k :note :replaces [earlier-name 0]}
          (sealed-fact :note-3)))

(def ^:private view-cases
  "The stand-in sealed form: how a maker examines an act before its values
  are sealed."
  [["the act as said is refused as it is" (fn [impl] (verdict ((:parse impl) said))) :not-sealed]
   ["through its sealed view it is taken" (fn [impl] (verdict ((:parse impl) ((:sealed-view impl) said)))) :ok]
   ["the view: the value fact gets empty bytes and the lock id [name 0]; each other fact is as it was"
    (fn [impl] (:facts ((:sealed-view impl) said)))
    [{:e :note-1 :k :note :mark #{:own-row} :sealed (byte-array 0) :lock-id [alice-name 0]}
     {:e :alice-s1 :k :lease :v {:count 1}}
     {:e :note-2 :k :note :replaces [earlier-name 0]}
     (sealed-fact :note-3)]]
   ["each other part of the act is as it was"
    (fn [impl] (= (dissoc said :facts) (dissoc ((:sealed-view impl) said) :facts)))
    true]
   ["which facts a door must seal: the plain value only; not the store key, the retract, the sealed fact, a number or nil"
    (fn [impl] (mapv (:value-fact? impl) (conj (:facts said) 42 nil)))
    [true false false false false false]]])

;; -------------------------------------------------------------- the digest

(def ^:private told
  "An act with each part that the digest covers."
  {:version 1 :name alice-name :who :alice :layer :alice :class :by-layer
   :permission alice-own :session :alice-s1 :based-on {[earlier-name 0] 41}
   :because-of earlier-name :claimed-when 1700000 :subjects #{:bob}
   :facts [(assoc (sealed-fact :note-1) :replaces [earlier-name 0] :mark #{:own-row})
           {:e :alice-s1 :k :lease :v {:count 1}}]})

(def ^:private told-changes
  "`told` with one part changed, for each part that the digest covers. Each
  is still an act that the check takes."
  {:who (assoc told :who :bob)
   :layer (assoc told :layer :alice-hand :name (a-name :alice-hand :by-layer 1))
   :class (assoc told :class :by-entity :name (a-name :alice :by-entity 1))
   :permission (assoc told :permission [:alice :alice :alice [:operator :alice :alice]])
   :session (assoc told :session :alice-s2)
   :based-on (assoc told :based-on {[earlier-name 0] 42})
   :because-of (assoc told :because-of lease-name)
   :claimed-when (assoc told :claimed-when 1700001)
   :subjects (assoc told :subjects #{:bob :carol})
   :fact-entity (assoc-in told [:facts 0 :e] :note-9)
   :fact-key (assoc-in told [:facts 0 :k] :text)
   :fact-replaces (assoc-in told [:facts 0 :replaces] [earlier-name 1])
   :fact-mark (assoc-in told [:facts 0 :mark] #{:die-with-any})
   :fact-layer-when-given (assoc-in told [:facts 0 :layer] :alice)
   :store-key-value (assoc-in told [:facts 1 :v] {:count 2})
   :one-fact-more (update told :facts conj {:e :note-2 :k :note})})

(defn- digest-of
  "The digest that an implementation gives for a thing that its check takes;
  nil when its check refuses the thing."
  [impl x]
  (some-> ((:parse impl) x) :ok ((:digest impl))))

(def ^:private built-one-way
  "An act whose store-key value is in its normal form."
  (assoc (act-of {:e :alice-s1 :k :lease :v {:count 2 :rate 0.0 :price 1.5M :path [1 2]}})
         :claimed-when 5 :subjects #{:bob :carol}))

(def ^:private built-another-way
  "The same act, built with a narrow integer, -0.0, a decimal with zeros at
  its end, a subvector, a sorted map and a list of facts."
  (-> (act-of)
      (assoc :facts (list {:e :alice-s1 :k :lease
                           :v (sorted-map :rate -0.0 :price 1.500M :path (subvec [0 1 2] 1) :count (int 2))})
             :claimed-when (int 5)
             :subjects (sorted-set :carol :bob))))

(def ^:private digest-cases
  "The digest: what it leaves out, and what it covers."
  [["lower-case hex, 64 long"
    (fn [impl] (boolean (re-matches #"[0-9a-f]{64}" (digest-of impl told))))
    true]
   ["one act sealed two times, under other bytes and another lock id: one digest"
    (fn [impl] (= (digest-of impl told)
                  (digest-of impl (update-in told [:facts 0] assoc :sealed bytes-b :lock-id [lease-name 7]))))
    true]
   ["the maker's plain act, through its sealed view: the same digest"
    (fn [impl] (= (digest-of impl told)
                  (digest-of impl ((:sealed-view impl)
                                   (update-in told [:facts 0] #(-> % (dissoc :sealed :lock-id) (assoc :v "a text")))))))
    true]
   ["another name alone: the same digest"
    (fn [impl] (= (digest-of impl told) (digest-of impl (assoc told :name (a-name :alice :by-layer 99)))))
    true]
   ["a change of each part that it covers: another digest"
    (fn [impl] (let [base (digest-of impl told)]
                 (into (sorted-map)
                       (map (fn [[part changed]]
                              (let [d (digest-of impl changed)] [part (and (some? d) (not= base d))])))
                       told-changes)))
    (into (sorted-map) (map (fn [part] [part true])) (keys told-changes))]
   ["the version too, on a map that no parse gives: there is one version"
    (fn [impl] (let [ok (:ok ((:parse impl) told))]
                 (not= ((:digest impl) ok) ((:digest impl) (assoc ok :version 2)))))
    true]
   ["two acts that mean the same and were built in other ways: one form, and one digest"
    (fn [impl] [(= (:ok ((:parse impl) built-one-way)) (:ok ((:parse impl) built-another-way)))
                (= (digest-of impl built-one-way) (digest-of impl built-another-way))
                (some? (digest-of impl built-one-way))])
    [true true true]]])

(def ^:private unseen-cases
  "What the digest does not see. A check of its own: if the digest must
  cover a value, this is the check that breaks."
  [["two acts that differ only in a sealed value have one digest"
    (fn [impl] (= (digest-of impl told)
                  (digest-of impl (assoc-in told [:facts 0 :sealed] bytes-b))))
    true]])

;; ---------------------------------------------------------- ids and values

(def ^:private normal-inputs
  "Values that are not in their normal form, and one that is big past the
  long range."
  [(int 7) (short 7) (byte 7) 7N (biginteger 7) 100000000000000000000N -0.0 1.500M 100M
   (subvec [0 1 2] 1) (sorted-map :b 2 :a 1) (sorted-set 3 1)])

(def ^:private value-cases
  "Ids, values, the one normal form, and the canonical text."
  [["ids: a keyword that reads back as itself; not one with a space, an empty one, one with a slash in its namespace, a string, nil or a symbol"
    (fn [impl] (mapv (:readable-keyword? impl)
                     [:a :a/b :a.b/c-d? (keyword "a b") (keyword "") (keyword "x/y" "z") "a" nil 'a]))
    [true true true false false false false false false]]
   ["values"
    (fn [impl] (mapv (:value? impl) [nil true "s" :a/b (uuid 1) 7 (int 7) 7N (biginteger 7) 1.5 1.5M {:a [1 #{2}]}]))
    (vec (repeat 12 true))]
   ["not values: a list, a symbol, a character, a ratio, a Java float, not-a-number, infinity, a keyword that does not read back, a record, a list inside a map, a date"
    (fn [impl] (mapv (:value? impl) ['(1 2) 'sym \c 1/3 (float 1.5) Double/NaN Double/POSITIVE_INFINITY
                                     (keyword "a b") (->Thing 1 2) {:a '(1)} (java.util.Date. 0)]))
    (vec (repeat 11 false))]
   ["a value inside 32 collections, and inside 33"
    (fn [impl] [((:value? impl) (nested 32)) ((:value? impl) (nested 33))])
    [true false]]
   ["each value in its one normal form: = to what was given, its class, its canonical text"
    (fn [impl] (mapv (fn [v] (let [n ((:normal-value impl) v)]
                               [(= v n) (.getSimpleName (class n)) ((:canonical-text impl) n)]))
                     normal-inputs))
    [[true "Long" "7"] [true "Long" "7"] [true "Long" "7"] [true "Long" "7"] [true "Long" "7"]
     [true "BigInt" "100000000000000000000N"] [true "Double" "0.0"]
     [true "BigDecimal" "1.5M"] [true "BigDecimal" "1E+2M"]
     [true "PersistentVector" "[1 2]"] [true "PersistentArrayMap" "{:a 1, :b 2}"]
     [true "PersistentHashSet" "#{1 3}"]]]
   ["two = maps that were built in other orders have one canonical text"
    (fn [impl] (= ((:canonical-text impl) (array-map :a 1 :b #{2 1}))
                  ((:canonical-text impl) (array-map :b #{1 2} :a 1))))
    true]
   ["a canonical text, sorted at each depth"
    (fn [impl] ((:canonical-text impl) {:b #{2 1} :a [3 {:d nil :c "x"}]}))
    "{:a [3 {:c \"x\", :d nil}], :b #{1 2}}"]
   ["permission ids: three ids; with a parent; not two items, not a list, not five deep"
    (fn [impl] (mapv (:permission-id? impl) [alice-own (deep-permission 2) (deep-permission 4)
                                             [:alice :alice] '(:alice :alice :alice) (deep-permission 5)]))
    [true true true false false false]]])

;; ------------------------------------------------------------- the checks

(defn- words
  "Rows [what x word] as text: each what with its word."
  [rows]
  (str/join " | " (map (fn [[what _ word]] (str what " " word)) rows)))

(deftest the-parts
  (holds parts-cases)
  (shown "the parts:" (count act/act-parts) "on an act," (count act/fact-parts) "on a fact |"
         (count act/store-keys) "store keys," (count act/placed-keys) "placed keys |"
         (count act/reasons) "reason words | the scheme of a made name:" made
         "| limits:" (pr-str (:limits here))))

(deftest acts-that-pass
  (let [n (holds passing-cases)
        ok (:ok (act/parse whole))]
    (shown n "checks of acts that pass.")
    (shown "an act with each optional part, given with subvectors, narrow integers and a list of facts, in its one form:"
           (pr-str (comparable ok)))
    (shown "in that form each vector is plain and each number is a long:" (= #{"Long" "PersistentVector"} (set (classes-of ok)))
           "| parsed again it is the same:" (= {:ok ok} (act/parse ok)))
    (shown "the least act, with the parts that it left out:"
           (pr-str (:ok (act/parse (act-of {:e :note-1 :k :note})))))
    (shown "taken with no gate named: an act into a layer placed by entity" (verdict (act/parse into-a-group))
           "| an act that the store places itself, under a name with no class" (verdict (act/parse placed)))))

(deftest refusals-by-reason
  (let [n (holds refusal-cases)
        earned (frequencies (map #(nth % 2) refusal-rows))]
    (is (= act/reasons (set (keys earned))) "each reason word comes from an act that earns it")
    (shown n "acts refused, each for one fault. By reason:" (pr-str (into (sorted-map) earned)))
    (shown "each of the" (count act/reasons) "reason words came from an act that earns it:" (= act/reasons (set (keys earned))))
    (shown "no permission:" (verdict (act/parse (dissoc good :permission)))
           "| a fact that names another layer:"
           (verdict (act/parse (act-of (assoc (sealed-fact :note-1) :layer :alice-hand))))
           "| a part for expected versions:" (verdict (act/parse (assoc good :expected-versions nil)))
           "| a landing:" (verdict (act/parse landing)))))

(deftest the-order-of-the-checks
  (let [n (holds order-cases)]
    (shown "the earlier check gives the reason," n "acts with two faults:" (words order-rows))))

(deftest nothing-throws
  (holds total-cases)
  (shown "nothing throws. The check refuses" (count not-acts) "things that are not acts:" (words not-acts))
  (shown "facts that throw when they are read: the check gives"
         (verdict (act/parse (assoc good :facts (throwing))))
         "| the sealed view gives the act back as it is:"
         (let [x (assoc good :facts (throwing))] (identical? x (act/sealed-view x)))
         "| the test of a name says no to each thing that is no name"))

(deftest the-limits
  (holds limit-cases)
  (shown "the limits at their edges:" (words limit-rows)))

(deftest names
  (holds name-cases)
  (let [nm (act/make-name :alice :by-layer)]
    (shown "a made name:" (pr-str nm) "| the version of its id:" (.version ^UUID (nth nm 3))
           "| valid:" (act/valid-name? nm) "| its tag:" (pr-str (act/name-tag nm)))
    (shown "a name for an act that the store places itself:" (pr-str (name-shape (act/name-for placed)))
           "| for an ordinary act:" (pr-str (name-shape (act/name-for good))))
    (shown "from the request" (pr-str alice-name) "the crossing's name:" (pr-str (act/crossing-name alice-name))
           "| the landing's name:" (pr-str (act/landing-name alice-name :group)))))

(deftest the-stand-in-sealed-form
  (holds view-cases)
  (shown "an act as said, with a plain value:" (verdict (act/parse said))
         "| through its sealed view:" (verdict (act/parse (act/sealed-view said))))
  (shown "the sealed view of its facts:" (pr-str (comparable (:facts (act/sealed-view said))))))

(deftest the-digest
  (holds digest-cases)
  (shown "the digest of one act:" (digest-of here told))
  (shown "sealed two times under other bytes and another lock id, as said through the sealed view, and under another name: the same digest")
  (shown "a change of each of these gives another digest:" (pr-str (vec (sort (keys told-changes)))) "| and the version")
  (shown "two acts that mean the same and were built in other ways have one form and one digest:"
         (= (digest-of here built-one-way) (digest-of here built-another-way))))

(deftest what-the-digest-does-not-see
  (holds unseen-cases)
  (shown "WHAT THIS DIGEST DOES NOT SEE: two acts that differ only in a sealed value have one digest:"
         (= (digest-of here told) (digest-of here (assoc-in told [:facts 0 :sealed] bytes-b)))))

(deftest ids-and-values
  (holds value-cases)
  (shown "each value in its one normal form, with its class and its canonical text:"
         (pr-str (mapv (fn [v] (let [n (act/normal-value v)] [(.getSimpleName (class n)) (act/canonical-text n)]))
                       normal-inputs)))
  (shown "a canonical text, sorted at each depth:" (act/canonical-text {:b #{2 1} :a [3 {:d nil :c "x"}]})))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-parts)
  (acts-that-pass)
  (refusals-by-reason)
  (the-order-of-the-checks)
  (nothing-throws)
  (the-limits)
  (names)
  (the-stand-in-sealed-form)
  (the-digest)
  (what-the-digest-does-not-see)
  (ids-and-values))

;; ------------------------------------------------------ beside the rig

(def ^:private groups
  "Each group of cases with its title, for the comparison."
  [["the parts" (mapv (fn [[what role expected]] [what (fn [impl] (get impl role)) expected]) parts-cases)]
   ["acts that pass" passing-cases]
   ["refusals" refusal-cases]
   ["the order" order-cases]
   ["nothing throws" total-cases]
   ["the limits" limit-cases]
   ["names" name-cases]
   ["the sealed view" view-cases]
   ["the digest" digest-cases]
   ["what the digest does not see" unseen-cases]
   ["ids and values" value-cases]])

(defn- name-with
  "Takes a thing. If it is a vector of four whose scheme word is `from`,
  gives it with the scheme word `to`; else gives it as it is."
  [x from to]
  (if (and (vector? x) (= 4 (count x)) (= from (nth x 2))) (assoc x 2 to) x))

(defn- fact-id-with
  "Takes a thing. If it is a vector of two, gives it with the scheme word of
  its name changed as `name-with` does; else gives it as it is."
  [x from to]
  (if (and (vector? x) (= 2 (count x))) (update x 0 name-with from to) x))

(defn- act-with
  "Takes an act, or a thing that is no act. Gives it in other words: the
  scheme word `from` as `to` in each place where the form holds a name, and
  the part `from-part` as `to-part`. It reads only those places, so a thing
  that is no act, a sorted map, and facts that throw when they are read all
  come through as they are."
  [x from to from-part to-part]
  (let [fact (fn [f]
               (if (and (map? f) (not (record? f)))
                 (cond-> f
                   (contains? f :replaces) (update :replaces fact-id-with from to)
                   (contains? f :lock-id) (update :lock-id fact-id-with from to))
                 f))]
    (if (and (map? x) (not (record? x)) (not (sorted? x)))
      (cond-> x
        (contains? x :name) (update :name name-with from to)
        (contains? x :because-of) (update :because-of name-with from to)
        (contains? x from-part)
        (-> (assoc to-part (let [b (get x from-part)]
                             (if (and (map? b) (not (record? b)))
                               (into {} (map (fn [[k v]] [(fact-id-with k from to) v])) b)
                               b)))
            (dissoc from-part))
        (or (vector? (:facts x)) (list? (:facts x))) (update :facts #(mapv fact %)))
      x)))

(defn- rig-roles
  "Loads the rig's envelope and gives its functions and definitions under the
  roles of the proof, as `here` gives the act's.

  Two words are put back for the rig, on the way in and on the way out: the
  part :based-on is the rig's :stood-on, and the scheme word of a made name
  is the rig's own. The rig's parse takes a gate; a case goes to the gate
  that the class of its name is for. The rig has no definition of its reason
  words and no named depth of a permission, so those two give nil."
  []
  (require 'rig.store.envelope)
  (let [r (fn [sym] @(ns-resolve 'rig.store.envelope sym))
        rig-word (first (remove (r 'store-schemes) (r 'schemes)))
        there #(act-with % made rig-word :based-on :stood-on)
        back #(act-with % rig-word made :stood-on :based-on)
        name-there #(name-with % made rig-word)
        name-back #(name-with % rig-word made)]
    {:rig-word rig-word
     :there there
     :parse (fn [x]
              (let [nm (when (and (map? x) (not (sorted? x))) (:name x))
                    gate (if (and (vector? nm) (= :by-entity (nth nm 1 nil))) :micro :stream)
                    got ((r 'parse) (there x) gate)]
                (cond-> got (contains? got :ok) (update :ok back))))
     :digest (fn [a] ((r 'digest) (there a)))
     :sealed-view (fn [x] (back ((r 'sealed-view) (there x))))
     :value-fact? (r 'value-fact?)
     :readable-keyword? (r 'readable-keyword?)
     :value? (r 'edn-value?)
     :normal-value (r 'normalize-value)
     :canonical-text (r 'canonical)
     :make-name (fn ([layer class] (name-back ((r 'make-name) layer class)))
                  ([layer class scheme id] ((r 'make-name) layer class scheme id)))
     :name-for (fn [a] (name-back ((r 'name-for) a)))
     :name-tag (r 'name-tag)
     :act-tag (r 'tag-of)
     :valid-name? (fn [x] ((r 'valid-name?) (name-there x)))
     :crossing-name (fn [nm] ((r 'crossing-name) (name-there nm)))
     :landing-name (fn [nm & more] (apply (r 'landing-name) (name-there nm) more))
     :fact-id? (fn [x] ((r 'fid?) (fact-id-with x made rig-word)))
     :permission-id? (r 'pid?)
     :act-parts (set (replace {:stood-on :based-on} (r 'act-parts)))
     :fact-parts (r 'fact-parts)
     :store-keys (r 'control-keys)
     :placed-keys (r 'placed-keys)
     :marks (r 'marks)
     :classes (r 'classes)
     :schemes (set (replace {rig-word made} (r 'schemes)))
     :reasons nil
     :limits [(r 'max-subjects) (r 'max-depth) (r 'max-carried-stamp) nil]}))

(defn- brief
  "A result as text for one line, cut at 260 characters."
  [x]
  (let [s (pr-str x)]
    (if (< 260 (count s)) (str (subs s 0 260) " ...") s)))

(defn- beside-the-rig
  "Runs each case of the proof with the rig's envelope in place of the act,
  and prints each case where the two give different things. It asserts
  nothing. Effect: it loads `rig.store.envelope`, prints, and for one line
  puts the rig's secret in the place of this file's for the length of one
  call.

  The last lines are about the digest of one act. The two files give
  different text, because the secret, one part's key and the scheme word
  differ. To show that nothing else differs, the act's own function digests
  the act in the rig's words under the rig's secret, and the line says if
  that is the rig's digest."
  []
  (let [rig (rig-roles)
        rows (vec (for [[title cases] groups
                        [what :as c] cases]
                    (let [ours (outcome here c) theirs (outcome rig c)]
                      {:group title :what what :ours ours :theirs theirs :same? (= ours theirs)})))
        differ (remove :same? rows)]
    (println "BESIDE THE RIG'S ENVELOPE: the same cases, through rig.store.envelope")
    (println "PUT BACK for the rig, in and out: the part :based-on as its :stood-on | the scheme word"
             made "as its" (:rig-word rig))
    (println "GATE: the rig's parse takes a gate. Each case goes to the gate that the class of its name is for.")
    (doseq [{:keys [group what ours theirs]} differ]
      (println "DIFFERS" (str group ":") what "| here:" (brief ours) "| the rig:" (brief theirs)))
    (println "CASES" (count rows) "| the same:" (- (count rows) (count differ)) "| differ:" (count differ))
    (let [ok-here (:ok (act/parse told))
          ok-there ((:there rig) ok-here)
          rig-digest @(ns-resolve 'rig.store.envelope 'digest)
          rig-secret @(ns-resolve 'rig.store.envelope 'fingerprint-secret)
          d-here (act/digest ok-here)
          d-rig (rig-digest ok-there)
          d-here-as-rig (with-redefs-fn {#'act/digest-secret rig-secret} #(act/digest ok-there))]
      (println "DIGEST of one act | here:" d-here "| the rig:" d-rig "| equal:" (= d-here d-rig))
      (println "DIGEST by this file's function, of the act in the rig's words, under the rig's secret:"
               d-here-as-rig "| equal to the rig's:" (= d-here-as-rig d-rig)))))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. With the one
  argument beside-the-rig it runs the comparison in place of the proof, and
  exits with status 0."
  [& args]
  (if (= "beside-the-rig" (first args))
    (do (beside-the-rig)
        (shutdown-agents)
        (System/exit 0))
    (let [{:keys [fail error] :as result} (run-tests 'softland.store.act-proof)]
      (println)
      (println "THE PROOF, IN ONE PLACE")
      (run! println @shown-lines)
      (println (select-keys result [:test :pass :fail :error]))
      (shutdown-agents)
      (System/exit (if (zero? (+ fail error)) 0 1)))))
