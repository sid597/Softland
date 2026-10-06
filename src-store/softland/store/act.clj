(ns softland.store.act
  "The act: its declared shape, its names, and the check of the shape. Pure
  code. It needs no cluster, and it requires nothing outside Clojure and the
  JVM.

  An act is one fact or more under one name. A gate accepts the whole act or
  refuses the whole act. Whatever made an act, it has one form.

  Read `shapes` and `rules` first. `shapes` declares the form as plain data:
  the parts of an act, what each part holds, which parts must be there, and
  the reason word when a part is wrong. `rules` declares what must hold
  between two places of an act, which a shape cannot say. Code checks the
  form: `parse` reads the two declarations, takes anything, and gives the act
  in its one form or the reason why it is not an act.

  Why the shape is in code and not a fact in the store. Each shape that a
  tool gives enters the store as a fact, inside an act. So the shape of the
  act itself is the floor: it must hold when each fact is wrong. It is
  compiled with the store. It is declared as data all the same, so that it
  can be read in one place, printed and shown.

  Inputs and results, by call:

    parse        anything -> {:ok act} or {:refuse reason}
    has-shape?   the name of a declared shape, and anything -> true or false
    make-name    a layer and a class -> a new name
    name-for     an act as its maker has it -> a new name for it
    sealed-view  an act with plain values -> the act as `parse` takes it after sealing
    digest       the :ok of a parse -> hex text for what the act says, its values aside

  State and effects. The namespace owns one constant, the secret of the
  digest. `make-name` with two arguments, and `name-for`, take a random id
  from the JVM's secure source. Each other function is pure. Nothing here
  reads a clock, appends, reads a record, or opens a sealed value.

  Callers. Only the proof of this namespace requires it. The door does not
  use it yet; the map of this folder says why.

  Approach. The file has these sections, in this order: the words and the
  limits; the declared shape and the rules; the leaves, which are the few
  shapes that code checks; the engine that reads a shape; the name; the
  check; the stand-in sealed form, with which a maker examines an act before
  its values are sealed; the digest.

  Not here: a stamp, an answer, locks and sealing, leases, a depot, a read,
  the class or the settings of a layer, each rule about a session, and each
  thing that a gate decides: the standing of a permission, a replaced fact
  against the record, and which gate a name is for.

  Words. \"Key\" is the key of a fact. \"Lock\" is an encryption key. A
  \"store key\" is a key that the store itself acts on."
  (:require [clojure.string :as str])
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]
           [java.util HexFormat]
           [javax.crypto Mac]
           [javax.crypto.spec SecretKeySpec]))

;; ------------------------------------------------- the words and the limits

(def version
  "The version marker of the form. This code knows this one version only."
  1)

(def made-scheme
  "The scheme word in a name that a maker makes. This constant is the one
  place that holds the word."
  :act)

(def store-schemes
  "The scheme words that only the store uses, for the two acts of a
  promotion."
  #{:crossing :landing})

(def reserved-who
  "Who no act that comes from outside the store can claim to be."
  #{:store})

(def store-keys
  "The fifteen keys that the store itself acts on. A fact under one of them
  carries a plain value and no lock. A fact with a value under each other key
  is a value fact, and its value is sealed."
  #{:forget :lock-grain :class :promote-request :crossed :permission :revoke
    :kind :owner :person :forget-person :lease :session-closed :members
    :grammar})

(def placed-keys
  "The keys of the acts that the store places itself. A name for an act that
  has only such keys carries no class."
  #{:class :lock-grain :forget :crossed :person :forget-person})

(def max-subjects
  "How many subjects an act can name."
  256)

(def max-depth
  "How many collections a value can be inside. `value?` stops at this depth,
  so no walk over a value goes deep."
  32)

(def max-carried-stamp
  "A carried stamp must be below this number, 2^62. A gate's stamp is later
  than each stamp that the act carries, so a carried stamp near the end of
  the long range would stop the clock of a task."
  (bit-shift-left 1 62))

(def max-permission-depth
  "How many permission ids one permission id can hold, itself counted."
  4)

;; ------------------------------------------------------- the declared shape

(def ^:private fact-pieces
  "The pieces that each kind of fact can carry beside its entity, its key and
  its value slot. A fact's own layer is left out of the one form when the
  fact gave none."
  [[:replaces :fact-id {:optional true}]
   [:mark [:set-of [:enum :die-with-any :own-row]] {:optional true :default #{}}]
   [:layer :id {:optional true :left-out true}]])

(def shapes
  "The declared shape of an act, and of each thing that an act holds. Plain
  data, with no code in it. Each entry is a name and a shape. A shape is one
  of these:

    :id :uuid :int :bytes :value :nil   a leaf, which code checks (`leaves`)
    another name of this map            that shape
    [:enum v ...]                       one of these values
    [:int {:min a :below b}]            a fixed-width integer in that range
    [:tuple s ...]                      a vector of exactly these, in order
    [:set-of s {:max n}]                a set of s, of n at most
    [:map-of ks vs]                     a plain map from ks to vs
    [:seq-of s]                         a vector or a list of s
    [:or s ...]                         the first of these that fits
    [:parts [part s opts] ...]          a plain map with these parts and no
                                        other, examined in this order
    [:by-kind decider {kind s}]         the shape for the kind that the
                                        decider names (`deciders`)

  The options of a part:

    :optional true   the part can be absent or nil; the one form then holds
                     nil for it, or its :default
    :left-out true   an absent part is absent from the one form too
    :reason word     the refusal when the part does not fit; :malformed when
                     no word is given
    :absent word     the refusal when a part that must be there is absent
    :empty word      the refusal when the part holds nothing

  In place of a shape, :never says that this kind of thing does not carry the
  part at all, not even as nil.

  The order of the parts is the order of the checks. So when a thing has two
  faults, the part that is declared first gives the reason."
  {:act
   [:parts
    [:version      [:enum version]                    {:reason :unknown-version}]
    [:name         :name                              {:reason :bad-name}]
    [:who          :id]
    [:layer        :id]
    [:class        [:enum :by-layer :by-entity]]
    [:permission   :permission-id                     {:absent :no-permission}]
    [:session      :id                                {:optional true}]
    [:based-on     [:map-of :fact-id :carried-stamp]  {:optional true :default {}}]
    [:because-of   :name                              {:optional true}]
    [:claimed-when :int                               {:optional true}]
    [:subjects     [:set-of :id {:max max-subjects}]  {:optional true :default #{}}]
    [:facts        [:seq-of :fact]                    {:empty :empty-act}]]

   ;; A fact has an entity, a key, a value slot and a few pieces. The slot has
   ;; three forms. The key of the fact, and sealed bytes, tell which.
   :fact
   [:by-kind :fact-kind {:store-key :store-key-fact
                         :sealed    :sealed-fact
                         :retract   :retract}]

   ;; Under a store key: a plain value, nil too, and no seal.
   :store-key-fact
   (into [:parts
          [:e :id]
          [:k :id]
          [:sealed  :never {:reason :not-sealed}]
          [:lock-id :never {:reason :not-sealed}]
          [:v :value {:optional true}]]
         fact-pieces)

   ;; Under another key, with a value: sealed bytes and the id of their lock,
   ;; and no plain value at all.
   :sealed-fact
   (into [:parts
          [:e :id]
          [:k :id]
          [:v       :never   {:reason :not-sealed}]
          [:sealed  :bytes   {:reason :not-sealed}]
          [:lock-id :fact-id {:reason :not-sealed}]]
         fact-pieces)

   ;; Under another key, with no value: a retract. A plain value here is a
   ;; value that nobody sealed.
   :retract
   (into [:parts
          [:e :id]
          [:k :id]
          [:lock-id :never {:reason :not-sealed}]
          [:v :nil {:optional true :reason :not-sealed}]]
         fact-pieces)

   ;; [layer class scheme id]. The class is nil when the store places the act
   ;; itself. A maker makes the id at random.
   :name
   [:tuple :id
           [:or [:enum :by-layer :by-entity] :nil]
           [:enum made-scheme :crossing :landing]
           :uuid]

   ;; [name index]: one fact of one act. A lock id has the same shape.
   :fact-id
   [:tuple :name [:int {:min 0}]]

   ;; [who layer in], or [who layer in parent].
   :permission-id
   [:or [:tuple :id :id :id]
        [:tuple :id :id :id :permission-id]]

   :carried-stamp
   [:int {:below max-carried-stamp}]})

(def ^:private nesting
  "How many times a named shape can be inside itself. A name that is not here
  can be inside itself no times, so no shape goes round."
  {:permission-id max-permission-depth})

(def rules
  "What an act that has the shape must also keep, with the reason for each, in
  the order in which they are examined. A shape says what each place holds. A
  rule bars a word that only the store uses, or relates two places of the
  act. `rule-checks` holds the code of each rule."
  [[:who-is-not-the-store          :reserved-who]
   [:scheme-is-a-makers            :reserved-scheme]
   [:facts-are-in-the-acts-layer   :fact-outside-the-acts-layer]
   [:name-was-made-for-this-act    :mis-tagged]])

;; ------------------------------------------ what the declaration says, as sets

(defn- parts-of
  "Takes the name of a declared shape that is a :parts shape. Gives the set
  of its part names."
  [shape-name]
  (into #{} (map first) (rest (get shapes shape-name))))

(def act-parts
  "The twelve parts of an act, read from the declared shape. The store owns
  them, and `parse` refuses a thirteenth. A tool adds facts, never a part."
  (parts-of :act))

(def fact-parts
  "The eight parts of a fact, read from the three kinds of the declared
  shape."
  (into #{} (mapcat parts-of) [:store-key-fact :sealed-fact :retract]))

(def reasons
  "The eleven words with which `parse` refuses, read from the two
  declarations: each word that a part or a rule names, and the two words
  that no part names, :malformed and :unknown-part."
  (into #{:malformed :unknown-part}
        (concat (map second rules)
                (for [shape (vals shapes)
                      :when (= :parts (first shape))
                      [_ _ opts] (rest shape)
                      word (keep #(get opts %) [:reason :absent :empty])]
                  word))))

;; ---------------------------------------------------------------- the leaves

(def ^:private token-pattern
  "The name, and the namespace, of a keyword whose printed form reads back as
  the same keyword."
  #"[\p{L}*+!_?$%&=<>.\-][\p{L}\p{N}*+!_?$%&=<>.\-'#]*")

(defn readable-keyword?
  "Takes anything. Gives true for an id: a keyword whose printed form reads
  back as the same keyword. The canonical text of a thing that holds only
  such keywords has one reading."
  [x]
  (boolean (and (keyword? x)
                (re-matches token-pattern (name x))
                (let [ns (namespace x)] (or (nil? ns) (re-matches token-pattern ns))))))

(defn- fits-long?
  "Takes an integer of any width. Gives true when it is in the long range."
  [x]
  (<= Long/MIN_VALUE x Long/MAX_VALUE))

(defn value?
  "Takes anything. Gives true for a value that the store can hold: nil, a
  boolean, a string, an id, a UUID, an integer of any width, a finite double,
  a big integer, a decimal, or a plain map, a vector or a set of values,
  inside `max-depth` collections at most. A ratio, a Java float, a list and a
  record are not values. The second argument is the number of collections
  already around x; a caller gives none."
  ([x] (value? x 0))
  ([x depth]
   (cond
     (nil? x) true
     (boolean? x) true
     (string? x) true
     (keyword? x) (readable-keyword? x)
     (uuid? x) true
     (int? x) true
     (instance? BigInt x) true
     (instance? BigInteger x) true
     (instance? Double x) (not (or (Double/isNaN x) (Double/isInfinite x)))
     (decimal? x) true
     (>= depth max-depth) false
     (map? x) (and (not (record? x))
                   (every? (fn [[k v]] (and (value? k (inc depth)) (value? v (inc depth)))) x))
     (vector? x) (every? #(value? % (inc depth)) x)
     (set? x) (every? #(value? % (inc depth)) x)
     :else false)))

(defn normal-value
  "Takes a value, as `value?` knows one. Gives its one normal form, which is
  = to the value given: an integer as a long, and big only past the long
  range; -0.0 as 0.0; a decimal with no trailing zeros; each vector a plain
  vector, never a subvector; plain maps and sets. So a value reads back as
  its maker gave it, and its text does not depend on how the maker built it.
  Limit: it does not examine x, and it goes as deep as x goes."
  [x]
  (cond
    (int? x) (long x)
    (instance? BigInt x) (if (fits-long? x) (long x) x)
    (instance? BigInteger x) (let [b (bigint x)] (if (fits-long? b) (long b) b))
    (instance? Double x) (if (zero? (double x)) 0.0 x)
    (decimal? x) (.stripTrailingZeros ^BigDecimal x)
    (map? x) (into {} (map (fn [[k v]] [(normal-value k) (normal-value v)])) x)
    (vector? x) (into [] (map normal-value) x)
    (set? x) (into #{} (map normal-value) x)
    :else x))

(defn- canonical*
  "Takes a normal value. Gives its canonical text. `canonical-text` calls it
  with the printer's settings fixed."
  [x]
  (cond
    (nil? x) "nil"
    (map? x) (str "{" (str/join ", " (sort (map (fn [[k v]] (str (canonical* k) " " (canonical* v))) x))) "}")
    (set? x) (str "#{" (str/join " " (sort (map canonical* x))) "}")
    (vector? x) (str "[" (str/join " " (map canonical* x)) "]")
    :else (pr-str x)))

(defn canonical-text
  "Takes a normal value, or data made of normal values. Gives its canonical
  text: EDN text in which the entries of a map and the elements of a set are
  sorted by their own canonical text, at each depth. Two = values give one
  text, and two different values never give one text."
  [x]
  (binding [*print-readably* true *print-length* nil *print-level* nil
            *print-meta* false *print-dup* false]
    (canonical* x)))

(def ^:private leaves
  "The shapes that code checks, by name. Each takes a thing and gives [:ok,
  its normal form], or nil when the thing does not fit. These are the only
  places where the check of a shape is code and not data:

    :id     a keyword that reads back as itself
    :uuid   a UUID of any version
    :int    a fixed-width integer; its normal form is a long
    :bytes  a byte array, which nobody here reads
    :value  a value that the store can hold; its normal form is its one
            normal form
    :nil    nil"
  {:id    (fn [x] (when (readable-keyword? x) [:ok x]))
   :uuid  (fn [x] (when (uuid? x) [:ok x]))
   :int   (fn [x] (when (int? x) [:ok (long x)]))
   :bytes (fn [x] (when (bytes? x) [:ok x]))
   :value (fn [x] (when (value? x) [:ok (normal-value x)]))
   :nil   (fn [x] (when (nil? x) [:ok nil]))})

(defn- fact-kind
  "Takes a fact as given. Gives the kind of its value slot: :store-key when
  its key is a store key; :sealed when it carries sealed bytes; else
  :retract. A thing that is no map counts as a :retract, and the shape of a
  retract then refuses it."
  [fact]
  (cond
    (not (map? fact)) :retract
    (contains? store-keys (:k fact)) :store-key
    (contains? fact :sealed) :sealed
    :else :retract))

(def ^:private deciders
  "The functions that name the kind of a thing for a :by-kind shape."
  {:fact-kind fact-kind})

;; ---------------------------------------------------------------- the engine

(declare conform)

(defn- conform-all
  "Takes pairs of a shape and a thing. Gives [:ok, the normal forms in
  order, as a plain vector], or the first refusal."
  [pairs seen]
  (reduce (fn [[_ out] [shape x]]
            (let [[verdict normal :as got] (conform shape x seen)]
              (if (= :ok verdict) [:ok (conj out normal)] (reduced got))))
          [:ok []]
          pairs))

(defn- conform-parts
  "Takes the entries of a :parts shape, and a thing. Gives [:ok, the thing as
  a plain map in its one form], or [:no reason]. A thing that is no plain map
  is :malformed. A part that the entries do not name is :unknown-part. Then
  the parts are examined in the order of the entries, and the first part
  that is wrong gives its reason; the options of a part say which reason
  (`shapes`)."
  [entries x seen]
  (cond
    (not (and (map? x) (not (record? x)))) [:no :malformed]
    (not-every? (into #{} (map first) entries) (keys x)) [:no :unknown-part]
    :else
    (reduce
     (fn [[_ out] [part shape opts]]
       (let [reason (:reason opts :malformed)
             given (get x part)]
         (cond
           (= :never shape)
           (if (contains? x part) (reduced [:no reason]) [:ok out])

           (nil? given)
           (cond
             (not (:optional opts)) (reduced [:no (:absent opts reason)])
             (:left-out opts) [:ok out]
             :else [:ok (assoc out part (:default opts))])

           :else
           (let [[verdict normal] (conform shape given seen)]
             (cond
               (= :no verdict) (reduced [:no (or normal reason)])
               (and (:empty opts) (empty? normal)) (reduced [:no (:empty opts)])
               :else [:ok (assoc out part normal)])))))
     [:ok {}]
     entries)))

(defn- conform
  "The engine. Takes a shape, a thing, and the count of each named shape that
  is around this place. Gives [:ok, the thing in its normal form], or
  [:no reason]. The reason is nil when the shape names none; the part that
  holds the place then gives its own. Each vector, map and set of a normal
  form is built new and plain. A shape that the engine does not know fits
  nothing.

  It can throw, for example for a thing that throws when it is read. `parse`
  and `has-shape?` turn each throw into a refusal."
  [shape x seen]
  (if (keyword? shape)
    (if-let [leaf (get leaves shape)]
      (or (leaf x) [:no nil])
      (let [times (inc (get seen shape 0))]
        (if (and (contains? shapes shape) (<= times (get nesting shape 1)))
          (conform (get shapes shape) x (assoc seen shape times))
          [:no nil])))
    (let [[form & args] shape]
      (case form
        :enum
        (let [hits (filter #(= % x) args)]
          (if (seq hits) [:ok (first hits)] [:no nil]))

        :int
        (let [{lowest :min below :below} (first args)]
          (if (and (int? x)
                   (or (nil? lowest) (<= lowest x))
                   (or (nil? below) (< x below)))
            [:ok (long x)]
            [:no nil]))

        :tuple
        (if (and (vector? x) (= (count args) (count x)))
          (conform-all (map vector args x) seen)
          [:no nil])

        :seq-of
        (if (sequential? x)
          (conform-all (map (fn [v] [(first args) v]) x) seen)
          [:no nil])

        :set-of
        (let [[of opts] args
              most (:max opts)]
          (if (and (set? x) (or (nil? most) (<= (count x) most)))
            (let [[verdict normal :as got] (conform-all (map (fn [v] [of v]) x) seen)]
              (if (= :ok verdict) [:ok (into #{} normal)] got))
            [:no nil]))

        :map-of
        (let [[ks vs] args]
          (if (and (map? x) (not (record? x)))
            (let [[verdict normal :as got] (conform-all (mapcat (fn [[k v]] [[ks k] [vs v]]) x) seen)]
              (if (= :ok verdict) [:ok (into {} (map vec) (partition 2 normal))] got))
            [:no nil]))

        :or
        (or (some (fn [alternative]
                    (let [got (conform alternative x seen)]
                      (when (= :ok (first got)) got)))
                  args)
            [:no nil])

        :parts
        (conform-parts args x seen)

        :by-kind
        (let [[decider kinds] args]
          (conform (get kinds ((get deciders decider) x)) x seen))

        [:no nil]))))

(defn has-shape?
  "Takes the name of a declared shape, and anything. Gives true when the
  thing fits the shape. It never throws. Limit: for :act it examines the
  shape only, not the rules; `parse` examines both."
  [shape-name x]
  (try (= :ok (first (conform shape-name x {})))
       (catch Throwable _ false)))

;; ----------------------------------------------------------------- the name

(defn make-name
  "Gives a name: the vector [layer class scheme id]. With a layer and a class
  it makes a new name, with the scheme `made-scheme` and a random UUID that
  holds no time. With all four slots it gives them back as a name and
  examines nothing; `valid-name?` examines. A maker makes the name of an act
  before a gate reads the act, so a maker can name each act of a series
  before the first one goes. Effect, with two arguments: it takes a random
  id."
  ([layer class] (make-name layer class made-scheme (random-uuid)))
  ([layer class scheme id] [layer class scheme id]))

(defn- placed-facts?
  "Takes the facts of an act. Gives true when the store places the act
  itself: there is a fact, and the key of each fact is a placed key. Facts
  that are no sequence give false."
  [facts]
  (boolean (and (sequential? facts) (seq facts)
                (every? #(contains? placed-keys (:k %)) facts))))

(defn act-tag
  "Takes an act. Gives its tag: its layer, and its class, or nil in place of
  the class when the store places the act itself. A name made for the act
  carries this tag."
  [act]
  [(:layer act) (when-not (placed-facts? (:facts act)) (:class act))])

(defn name-for
  "Takes an act as its maker has it; only its layer, class and facts are
  read. Gives a new name for it, with the scheme `made-scheme`: its tag and a
  random id. Effect: it takes a random id."
  [act]
  (let [[layer class] (act-tag act)]
    (make-name layer class)))

(defn name-tag
  "Takes a valid name. Gives its tag: the layer and the class that the name
  was made for. Limit: it throws for a thing that has fewer than two slots."
  [nm]
  [(nth nm 0) (nth nm 1)])

(defn valid-name?
  "Takes anything. Gives true for a name: a thing that has the declared shape
  :name."
  [x]
  (has-shape? :name x))

(defn crossing-name
  "Takes the name of a promotion's request. Gives the name of its crossing:
  the request's layer and id, no class, and the scheme :crossing."
  [request-name]
  [(nth request-name 0) nil :crossing (nth request-name 3)])

(defn landing-name
  "Takes the name of a promotion's request, the target layer, and the class
  of the target; :by-entity when no class is given. Gives the name of the
  landing: the target, that class, the scheme :landing and the request's id."
  ([request-name target] (landing-name request-name target :by-entity))
  ([request-name target class] [target class :landing (nth request-name 3)]))

;; ---------------------------------------------------------------- the check

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act in its one form and
  gives true when the act keeps the rule."
  {:who-is-not-the-store
   (fn [act] (not (contains? reserved-who (:who act))))

   :scheme-is-a-makers
   (fn [act] (not (contains? store-schemes (nth (:name act) 2))))

   :facts-are-in-the-acts-layer
   (fn [act] (every? #(or (nil? (:layer %)) (= (:layer act) (:layer %))) (:facts act)))

   :name-was-made-for-this-act
   (fn [act] (= (act-tag act) (name-tag (:name act))))})

(defn- parse*
  "The body of `parse`, with no guard around it. Takes anything. Gives
  {:ok act} or {:refuse reason}. First the declared shape :act, then the
  rules in their order. It can throw; `parse` turns each throw into a
  refusal."
  [x]
  (let [[verdict got] (conform :act x {})]
    (if (= :no verdict)
      {:refuse (or got :malformed)}
      (if-let [reason (some (fn [[rule reason]] (when-not ((get rule-checks rule) got) reason)) rules)]
        {:refuse reason}
        {:ok got}))))

(defn parse
  "The check of the form. Takes anything. Gives {:ok act}, with the act in
  its one form, or {:refuse reason}, with one of the eleven `reasons`. It
  never throws, whatever x is: an exception in the code of a gate stops the
  worker. So it turns each throw into :malformed.

  It reads the two declarations and holds no knowledge of the form itself.
  First the shape: the thing must have the declared shape :act (`shapes`).
  A thing that does not have it gets the reason of the first part that is
  wrong, in the order of the declaration. Then the rules: an act that has
  the shape must keep each rule (`rules`), and the first rule that it breaks
  gives the reason.

  The one form. All twelve parts are there. Each vector, map and set is
  plain, and each fixed-width integer is a long. A part that the act left
  out holds nil, or the default that the declaration gives: an empty map for
  `:based-on`, an empty set for `:subjects` and for the mark of a fact. To
  parse the :ok of a parse gives the same :ok.

  It is the check for an act that comes from outside the store. It does not
  know which gate reads the act.

  Limits. It reads no clock and gives `:claimed-when` no default. It has no
  rule about a session. It examines the shape of a permission and not its
  standing. It does not examine the value of a store key beyond `value?`.
  It refuses the store's own acts, a landing too: a gate that takes a
  landing cannot use this check for it. Facts that are a sequence with no
  end do not come back from it."
  [x]
  (try (parse* x)
       (catch Throwable _ {:refuse :malformed})))

;; ------------------------------------------------- the stand-in sealed form

(defn value-fact?
  "Takes anything. Gives true for a fact whose value a door must seal: a map,
  under a key that is not a store key, with a plain value that is not nil. A
  retract has no value, and a fact that is sealed has no plain value."
  [fact]
  (and (map? fact) (not (contains? store-keys (:k fact))) (some? (:v fact))))

(defn sealed-view
  "Takes an act as its maker has it, with plain values. Gives the act as
  `parse` will take it after sealing: each value fact loses its `:v` and
  gets stand-in sealed bytes, which are empty, and a stand-in lock id, [the
  act's name 0]. Each other fact and each other part is as it was. So a maker
  can examine an act before a lock is leased or a value is sealed. The digest
  leaves the bytes and the lock id out, so this view has the digest of the
  sealed act. A thing that is not a map with a sequence of facts comes back
  as it is, and so does a thing that throws when it is read."
  [act]
  (try
    (if (and (map? act) (sequential? (:facts act)))
      (let [stand-in [(:name act) 0]]
        (assoc act :facts (mapv (fn [fact]
                                  (if (value-fact? fact)
                                    (-> fact (dissoc :v) (assoc :sealed (byte-array 0) :lock-id stand-in))
                                    fact))
                                (:facts act))))
      act)
    (catch Throwable _ act)))

;; --------------------------------------------------------------- the digest

(def ^:private digest-secret
  "The secret that keys the digest. A throwaway: a store that is kept keeps
  its secret in the module, not in source."
  (.getBytes "softland.store.act: the secret of the digest, a throwaway" "UTF-8"))

(defn- keyed-hex
  "Takes a text. Gives its HMAC-SHA256 under `digest-secret`, as lower-case
  hex."
  [^String text]
  (let [mac (Mac/getInstance "HmacSHA256")]
    (.init mac (SecretKeySpec. ^bytes digest-secret "HmacSHA256"))
    (.formatHex (HexFormat/of) (.doFinal mac (.getBytes text "UTF-8")))))

(defn- digest-fact
  "Takes a fact in its one form. Gives it as the digest sees it: `true` in
  place of sealed bytes, and no lock id. So one act that is sealed two times,
  or under other locks, has one digest. A value fact, a retract and a store
  key's fact under one entity and key still differ."
  [fact]
  (cond-> fact
    (contains? fact :sealed) (-> (dissoc :lock-id) (assoc :sealed true))))

(defn digest
  "Takes the :ok of a parse. Gives the digest of what the act says, as
  lower-case hex: the keyed HMAC-SHA256 of the canonical text of the act
  with no name, with `true` in place of each fact's sealed bytes and with no
  lock id. The digest is not a part of the act.

  It covers the version, who, layer, class, permission, session, based-on,
  because-of, claimed-when and subjects; and for each fact the entity, the
  key, replaces, the mark, the layer when the fact gave one, and the plain
  value of a store key. It does not see a sealed value: two acts that differ
  only in a sealed value have one digest. Limit: it examines nothing, so give
  it only what `parse` gave."
  [act]
  (keyed-hex (canonical-text (update (dissoc act :name) :facts #(mapv digest-fact %)))))
