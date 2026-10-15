(ns softland.store.act
  "The act: its one form, declared as data, and the check of that form. Pure
  code. It needs no cluster, and it requires nothing outside Clojure and the
  JVM.

  An act is one fact or more that one maker sends under one act-id; the
  door makes the act-id, and it is not a part of the form. A gate accepts
  the whole act or refuses the whole act. Whatever made an act, it has one
  form. The form here is the maker's act: what a maker gives, with plain
  values, before the door touches it. That is Sid's decision of 6 October
  2026. The twelve decisions are listed in src-store/README.md, which marks
  the two that wait for his word.

  Read `shapes` and `rules` first. `shapes` declares the form as plain data:
  the parts of an act, what each part holds, which parts must be there, and
  the reason word when a part is wrong; and the shape of a fact, an act-id,
  a fact id, a permission id and a carried stamp. `rules` declares what must
  hold between two places of an act, which a shape cannot say. `parse` reads
  the two declarations, takes anything, and gives the act in its one form or
  the reason why it is not an act.

  A permission id has three forms. `[who layer granted-in]` is a first
  permission, with nothing above it. `[who layer granted-in parent]` is a
  given permission, with the permission above it. And
  `[who layer granted-in parent allows]` is a given permission that says
  what it allows: `:see`; `:see-and-act`; or a set of key patterns, the
  keys that its holder may write (`shapes`, `:allows`). A permission without
  that slot allows seeing and acting, under every key; `allows` is the one
  function that reads the slot, and `allows-key?` and `allows-within?` read
  what it gives. That a permission says what it allows is Sid's decision of
  11 and 12 October 2026 (src-store/README.md, entry 1); that it says so in
  a slot of its own id is the builder's. That a right lists the keys it
  allows, and that a give allows no more than the giver holds, are defaults
  that a session told Sid on 15 October 2026 and that he did not strike
  (src/proposal/sign-in-2026-10-15/BUILD.md, default 12): the kind of an act
  is the keys it writes, and giving a right and changing a layer's setting
  are keys of the store like any other. The form of a pattern is a
  session's, Sid's to change.

  A key pattern is one of three. A key: that key and no other. `:ns/*`:
  every key of the namespace `ns`, `:softland.store/*` every key of the
  store's own namespace among them. `:*`: every plain key, and never a key
  of the store's namespace (`store-namespace`). So `:see-and-act` allows
  what `#{:* :softland.store/*}` allows, and `:see` what `#{}` allows: no
  key. Every right that stands allows seeing.

  Why the shape is in code and not a fact in the store. Each shape that a
  tool gives enters the store as a fact, inside an act. So the shape of the
  act itself is the floor: it must hold when each fact is wrong. It is
  compiled with the store. It is declared as data all the same, so that it
  can be read in one place, printed and shown.

  Inputs and results, by call:

    parse         anything -> {:ok act} or {:refuse reason}
    has-shape?    the name of a declared shape, and anything -> true or false
    as-shape      a map of shapes, the name of a shape in it, and anything
                  -> [:ok the thing in its one form] or [:no reason]
    value?        anything -> true for a value that a fact can hold
    normal-value  a value -> its one normal form
    allows        a permission id in its one form -> :see, :see-and-act,
                  or a set of key patterns
    allows-key?   what `allows` gave, and a key -> true or false
    allows-within?
                  what `allows` gave of a given permission, and what it
                  gave of the giver's -> true when the first allows no
                  more than the second

  State and effects. The namespace owns no state, and each function is pure.
  Nothing here reads a clock, takes a random id, appends, or reads a
  record.

  Callers. The proof of this namespace. `softland.store.door.declaration`: the
  door's declaration is a map of shapes in the language of `shapes`, with
  the entries of `shapes` in it, and `as-shape` gives it the engine that
  reads one; it reads `rules` too, and `store-namespace`.
  `softland.store.door.text` asks
  `readable-keyword?`. The door's additions
  (`softland.store.door.additions`) ask nothing of it but the shape of an
  act-id, through the declaration. The checks of a gate
  (`softland.store.gate.decision`, which the walk calls for both gates) ask
  `allows` of the permission that the act names, in its standing;
  `allows-key?` of the key of each fact, the first check of its places; and
  `allows-within?` of each permission that the act gives, beside check (g).
  The runtime under `src-runtime/` asks `allows-key?` of the keys of a block.
  The record's rows
  (`softland.store.record.rows`) ask `value?`, `normal-value`,
  `has-shape?` and `readable-keyword?` when they read a row back. The read
  path's check of a query (`softland.store.read-path.query`) declares its
  own map of shapes over `shapes`, reads it with `as-shape`, and asks
  `readable-keyword?`.

  Approach. The file has these sections, in this order: the words and the
  limits; the declared shape and the rules; what the declaration says, as
  sets; the leaves, which are the few shapes that code checks; the engine
  that reads a shape; the check.

  Not here. Act-ids: the door makes them, on request ahead of sending. The
  depot: the door adds it, from the layer. The time on the act: the door
  writes it, from the clock of its server. The standing of a permission, and
  whether an act claims to be the store: a gate decides. Which gate an act
  is for: the gate's. A replaced fact against the record, the rules of a
  session and the digest of an act: the gates'. The partition of an act in
  its depot: the depot takes it from the act's layer
  (`softland.store.module`). The door is `softland.store.door`; the gate
  is in its own namespaces beside this one. A stamp, an answer, a read and
  the settings of a layer are not here either.

  Words. A fact is one statement: entity, key, value. An act is one or more
  facts that one maker sends under one act-id. An act-id is
  `[layer depot uuid]`: the layer, the layer's depot, a random UUID. A
  layer's depot is `:streaming` or `:microbatch`: the depot that takes its
  acts. The layer and the depot are there so that the act can be routed and
  the door can find its answer in the right record. \"Depot\" is Sid's word,
  of 9 October 2026; his decisions before that day say \"class\" for it.
  From Sid's \"act-uuid\" of 7 October
  2026: the session wrote act-id, since the id holds more than the UUID, and
  the word is his to change. The ruling of 24 September said \"name\". A maker is a converter, the screen,
  an agent. The door is the code that takes an act from a maker into the
  store. A gate decides each act inside the store. \"Key\" is a fact's key,
  and \"lock\" is an encryption key: never one word for both."
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]))

;; ------------------------------------------------- the words and the limits

(def version
  "The version marker of the form: the edition of the list of parts. This code
  knows this one version only."
  1)

(def max-subjects
  "How many subjects an act can name."
  256)

(def max-depth
  "How many collections a value can be inside. `value?` stops at this depth,
  so no walk over a value goes deep. `nesting` reads the same number for how
  deep a permission can be."
  32)

(def max-key-patterns
  "How many key patterns the set in the fifth slot of a given permission can
  hold (`shapes`, `:allows`). A number of a session, kept and not decided.
  Each pattern is a key, and the door's declaration holds it to the limit
  of an id's text, as it holds each key that an act names."
  64)

(def store-namespace
  "The namespace of the store's own keys: `softland.store`. The word is
  Sid's, of 9 October 2026 (src-store/README.md, K4 B). It is made here, the
  one place, because what a permission allows reads it: the pattern `:*`
  allows every plain key and never a key of this namespace (`allows-key?`).
  The door's declaration reads it from here
  (`softland.store.door.declaration/store-namespace`), and says what the
  store does with its namespace."
  "softland.store")

;; ------------------------------------------------------- the declared shape

(def shapes
  "The declared shape of an act, and of each thing that an act holds. Plain
  data, with no code in it. Each entry is a name and a shape. A shape is one
  of these:

    :id :key :uuid :int :value   a leaf, which code checks (`leaves`)
    another name of this map     that shape
    [:enum v ...]                one of these values
    [:int {:min a :below b}]     a fixed-width integer in that range
    [:tuple s ...]               a vector of exactly these, in order
    [:set-of s {:max n}]         a set of s, of n at most
    [:map-of ks vs]              a plain map from ks to vs
    [:seq-of s]                  a vector or a list of s
    [:or s ...]                  the first of these that fits
    [:parts [part s opts] ...]   a plain map with these parts and no other,
                                 examined in this order

  The options of a part:

    :optional true   the part can be absent or nil; the one form then holds
                     nil for it, or its :default
    :left-out true   an absent part is absent from the one form too
    :reason word     the refusal when the part does not fit; :malformed when
                     no word is given
    :absent word     the refusal when a part that must be there is absent
    :empty word      the refusal when the part holds nothing

  In place of a shape, :never says that the thing does not carry the part at
  all, not even as nil. The engine keeps :never and :left-out as part of the
  language; no shape of this declaration uses them.

  The order of the parts is the order of the checks. So when a thing has two
  faults, the part that is declared first gives the reason."
  {:act
   [:parts
    [:version      [:enum version]                    {:reason :unknown-version}]
    [:who          :id]
    [:layer        :id]
    [:permission   :permission-id                     {:absent :no-permission}]
    [:session      :id                                {:absent :no-session}]
    [:based-on     [:map-of :fact-id :carried-stamp]  {:optional true :default {}}]
    [:because-of   :act-id                            {:optional true}]
    [:claimed-when :int                               {:optional true}]
    [:subjects     [:set-of :id {:max max-subjects}]  {:optional true :default #{}}]
    [:facts        [:seq-of :fact]                    {:empty :empty-act}]]

   ;; A fact: what it is about, what it says, and what it replaces.
   ;; A fact with no value is a retract.
   :fact
   [:parts
    [:e        :id]
    [:k        :key]
    [:v        :value   {:optional true}]
    [:replaces :fact-id {:optional true}]]

   ;; [layer depot uuid]. The door makes it; an act refers to act-ids.
   :act-id
   [:tuple :id [:enum :streaming :microbatch] :uuid]

   ;; [act-id index]: one fact of one act.
   :fact-id
   [:tuple :act-id [:int {:min 0}]]

   ;; [who layer granted-in]: a first permission. [who layer granted-in
   ;; parent]: a given permission. [who layer granted-in parent allows]: a
   ;; given permission that says what it allows. A tuple fits only its own
   ;; count, so each thing fits one alternative at most; the slot is no
   ;; permission, so it does not count toward the nesting limit.
   :permission-id
   [:or [:tuple :id :id :id]
        [:tuple :id :id :id :permission-id]
        [:tuple :id :id :id :permission-id :allows]]

   ;; What a given permission allows: see; see and act, under every key; or
   ;; a set of key patterns, each a key, :ns/* or :*, which allows seeing
   ;; and acting under the keys that its patterns name (`allows-key?`).
   :allows
   [:or [:enum :see :see-and-act]
        [:set-of :key {:max max-key-patterns}]]

   ;; The stamp of a fact that the act is based on, as its maker saw it. A
   ;; label: no gate reads it, and the act's own stamp is not raised above
   ;; it (`softland.store.gate.stamp`). So it has no bound but its form.
   :carried-stamp
   [:int {:min 0}]})

(def ^:private nesting
  "For a named shape, how many times it can be around one place, the place's
  own counted. A name that is not here has the number 1: it can be inside
  itself no times, so no shape goes round.

  The one entry is the depth of a permission: how many permission ids one
  permission id can hold, itself counted. A first permission has depth 1.
  The engine adds one each time it enters the shape and refuses a thing
  whose count is over the number, so the number is the greatest depth: 32. A
  deeper permission is `:malformed`.

  The number is the value's own, `max-depth`, on purpose. A permission is
  held on two roads: as an act's own `:permission`, which this number
  bounds, and as the value of a fact that gives a permission, which
  `value?` bounds first. `value?` takes a collection with fewer than
  `max-depth` collections around it, and the innermost vector of a
  permission of depth d has d - 1 around it; so a given permission can have
  depth 32 too, and the two roads agree because they read one number.

  Not decided. How deep a permission can be is Sid's to say. Kept: as deep
  as a value can be. This entry is the one place where another answer is
  made. A smaller number ends both roads there. A greater one needs a
  greater `max-depth` too, and must stay under 62: the one text of a thing
  (`softland.store.door.text`) writes a given permission whole to depth 61
  and no deeper."
  {:permission-id max-depth})

(def rules
  "What an act that has the shape must also keep, with the reason for each, in
  the order in which they are examined. A shape says what each place holds. A
  rule relates two places of the act. `rule-checks` holds the code of each
  rule.

  The one rule: the layer of the act's permission is the act's own layer. A
  permission cited for another layer cannot cover the act, whatever its
  standing; whether it stands is a gate's question."
  [[:permission-is-in-the-acts-layer :permission-not-in-layer]])

;; ------------------------------------------ what the declaration says, as sets

(defn- parts-of
  "Takes the name of a declared shape that is a :parts shape. Gives the set
  of its part names."
  [shape-name]
  (into #{} (map first) (rest (get shapes shape-name))))

(def act-parts
  "The parts of an act, read from the declared shape. `parse` refuses a part
  that is not one of them. A tool adds facts, never a part."
  (parts-of :act))

(def fact-parts
  "The parts of a fact, read from the declared shape."
  (parts-of :fact))

(def reasons
  "The words with which `parse` refuses, read from the two declarations: each
  word that a part or a rule names, and the two words that no part names,
  :malformed and :unknown-part."
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
  "Takes anything. Gives true for a keyword whose printed form reads back as
  the same keyword. A key is such a keyword, and so is one of the two kinds
  of id."
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
  boolean, a string, a readable keyword, a UUID, an integer of any width, a
  finite double, a big integer, a decimal, or a plain map, a vector or a set
  of values, inside `max-depth` collections at most. A ratio, a Java float, a
  list and a record are not values. The second argument is the number of
  collections already around x; a caller gives none."
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
  its maker gave it, however the maker built it. Limit: it does not examine
  x, and it goes as deep as x goes."
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

(def ^:private leaves
  "The shapes that code checks, by name. Each takes a thing and gives [:ok,
  its normal form], or nil when the thing does not fit. These are the only
  places where the check of a shape is code and not data:

    :id     an id: a keyword that reads back as itself, or a UUID
    :key    a key: a keyword that reads back as itself, and nothing else
    :uuid   a UUID of any version
    :int    a fixed-width integer; its normal form is a long
    :value  a value that the store can hold; its normal form is its one
            normal form"
  {:id    (fn [x] (when (or (readable-keyword? x) (uuid? x)) [:ok x]))
   :key   (fn [x] (when (readable-keyword? x) [:ok x]))
   :uuid  (fn [x] (when (uuid? x) [:ok x]))
   :int   (fn [x] (when (int? x) [:ok (long x)]))
   :value (fn [x] (when (value? x) [:ok (normal-value x)]))})

;; ---------------------------------------------------------------- the engine

(declare conform)

(defn- conform-all
  "Takes the map of shapes that the names are read from, and pairs of a
  shape and a thing. Gives [:ok, the normal forms in order, as a plain
  vector], or the first refusal."
  [declared pairs seen]
  (reduce (fn [[_ out] [shape x]]
            (let [[verdict normal :as got] (conform declared shape x seen)]
              (if (= :ok verdict) [:ok (conj out normal)] (reduced got))))
          [:ok []]
          pairs))

(defn- conform-parts
  "Takes the map of shapes that the names are read from, the entries of a
  :parts shape, and a thing. Gives [:ok, the thing as
  a plain map in its one form], or [:no reason]. A thing that is no plain map
  is :malformed. A part that the entries do not name is :unknown-part. Then
  the parts are examined in the order of the entries, and the first part
  that is wrong gives its reason; the options of a part say which reason
  (`shapes`)."
  [declared entries x seen]
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
           (let [[verdict normal] (conform declared shape given seen)]
             (cond
               (= :no verdict) (reduced [:no (or normal reason)])
               (and (:empty opts) (empty? normal)) (reduced [:no (:empty opts)])
               :else [:ok (assoc out part normal)])))))
     [:ok {}]
     entries)))

(defn- conform
  "The engine. Takes a map of shapes, a shape, a thing, and the count of
  each named shape that is around this place. A shape that is a name is
  read from the map given: `shapes` for the maker's act, a larger map for a
  caller of `as-shape`. The leaves and the nesting limits are this
  namespace's own, whatever map is given. Gives [:ok, the thing in its
  normal form], or [:no reason]. The reason is nil when the shape names
  none; the part that holds the place then gives its own. Each vector, map
  and set of a normal form is built new and plain. A shape that the engine
  does not know fits nothing.

  It can throw, for example for a thing that throws when it is read. `parse`
  and `has-shape?` turn each throw into a refusal."
  [declared shape x seen]
  (if (keyword? shape)
    (if-let [leaf (get leaves shape)]
      (or (leaf x) [:no nil])
      (let [times (inc (get seen shape 0))]
        (if (and (contains? declared shape) (<= times (get nesting shape 1)))
          (conform declared (get declared shape) x (assoc seen shape times))
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
          (conform-all declared (map vector args x) seen)
          [:no nil])

        :seq-of
        (if (sequential? x)
          (conform-all declared (map (fn [v] [(first args) v]) x) seen)
          [:no nil])

        :set-of
        (let [[of opts] args
              most (:max opts)]
          (if (and (set? x) (or (nil? most) (<= (count x) most)))
            (let [[verdict normal :as got] (conform-all declared (map (fn [v] [of v]) x) seen)]
              (if (= :ok verdict) [:ok (into #{} normal)] got))
            [:no nil]))

        :map-of
        (let [[ks vs] args]
          (if (and (map? x) (not (record? x)))
            (let [[verdict normal :as got] (conform-all declared (mapcat (fn [[k v]] [[ks k] [vs v]]) x) seen)]
              (if (= :ok verdict) [:ok (into {} (map vec) (partition 2 normal))] got))
            [:no nil]))

        :or
        (or (some (fn [alternative]
                    (let [got (conform declared alternative x seen)]
                      (when (= :ok (first got)) got)))
                  args)
            [:no nil])

        :parts
        (conform-parts declared args x seen)

        [:no nil]))))

(defn has-shape?
  "Takes the name of a declared shape, and anything. Gives true when the
  thing fits the shape. It never throws. Limit: for :act it examines the
  shape only, not the rules; `parse` examines both."
  [shape-name x]
  (try (= :ok (first (conform shapes shape-name x {})))
       (catch Throwable _ false)))

(defn as-shape
  "Gives the engine to another namespace. Takes a map of shapes in the
  language of `shapes`, the name of a shape in that map, and anything.
  Gives what the engine gives: [:ok, the thing in its one form], or
  [:no reason], where the reason is the word of the first part that is
  wrong, or nil when that part names none. The map may hold the entries of
  `shapes` and add its own; the door's declaration does so.

  Limits. The five leaves and the nesting limits stay this namespace's: a
  caller gives the map of shapes and nothing else, so a named shape of the
  caller's can be inside itself no times. It can throw, as the engine can,
  for a thing that throws when it is read; the caller turns a throw into a
  refusal, as `parse` does."
  [declared shape-name x]
  (conform declared shape-name x {}))

;; ---------------------------------------------------------------- the check

(defn allows
  "Takes a permission id in its one form. Gives what the permission allows:
  its fifth slot when it has one, `:see`, `:see-and-act` or a set of key
  patterns; `:see-and-act` for a permission with three or four slots. So a
  first permission, and one given without the slot, allow every key, and
  this is the one place where that is code. A gate refuses an act under a
  permission that allows `:see`, and a fact whose key what this gives does
  not allow (`allows-key?`). The read path does not ask it: any right that
  stands, with its whole chain (`softland.store.gate.chain/stands?`), allows
  seeing.

  What must hold: the thing has the shape `:permission-id`. It never throws:
  anything that is no vector of five slots gives `:see-and-act`, so a caller
  that holds a thing of no shape tests the shape first."
  [permission]
  (if (and (vector? permission) (= 5 (count permission)))
    (nth permission 4)
    :see-and-act))

(defn- every-key-of
  "Takes a key pattern. Gives the namespace, as a string, whose every key
  the pattern names when it is `:ns/*`; nil for `:*`, for a key, and for
  anything that is no keyword."
  [pattern]
  (when (and (keyword? pattern) (namespace pattern) (= "*" (name pattern)))
    (namespace pattern)))

(defn allows-key?
  "Takes what `allows` gave, and a fact's key. Gives true when it allows a
  fact under that key: `:see-and-act` allows every key, `:see` none, and a
  set of key patterns a key that one of its patterns names. The key itself
  names it; `:ns/*` names each key whose namespace is exactly `ns`; `:*`
  names each key whose namespace is not `store-namespace`, a key with none
  among them. So `:*` never allows a store key, and only `:softland.store/*`
  or the store key itself does. Gives false for anything else, a key that
  is no keyword among them. It never throws.

  A gate asks it of each fact of an act, with what the act's permission
  allows; a fact whose key it does not allow is refused `:key-not-allowed`
  (`softland.store.gate.decision/places`). A retract is a fact under its
  key like any other, so the end of a right needs the key
  `:softland.store/permission`."
  [allowed k]
  (boolean
   (and (keyword? k)
        (cond
          (= :see-and-act allowed) true
          (set? allowed) (or (contains? allowed k)
                             (and (some? (namespace k)) (contains? allowed (keyword (namespace k) "*")))
                             (and (contains? allowed :*) (not= store-namespace (namespace k))))
          :else false))))

(defn- patterns-of
  "Takes what `allows` gave. Gives it as a set of key patterns that allows
  the same keys: `:see` as `#{}`, `:see-and-act` as
  `#{:* :softland.store/*}`, a set as itself. Nil for anything else."
  [allowed]
  (cond
    (= :see allowed) #{}
    (= :see-and-act allowed) #{:* (keyword store-namespace "*")}
    (set? allowed) allowed
    :else nil))

(defn- pattern-within?
  "Takes one key pattern and a set of key patterns. Gives true when every
  key that the pattern names, the set allows. `:*` only under `:*`; `:ns/*`
  under itself, or under `:*` when `ns` is not the store's namespace; a key
  when the set allows it (`allows-key?`). A set of keys never holds a whole
  namespace, however many of its keys it names."
  [pattern patterns]
  (cond
    (= :* pattern) (contains? patterns :*)
    (every-key-of pattern) (or (contains? patterns pattern)
                               (and (contains? patterns :*) (not= store-namespace (every-key-of pattern))))
    :else (allows-key? patterns pattern)))

(defn allows-within?
  "Takes what `allows` gave of a given permission, and what it gave of the
  permission of its giver. Gives true when the first allows no more than
  the second: every key that the first allows, the second allows. `:see`
  and the empty set allow no key, so they are within anything;
  `:see-and-act` is within `:see-and-act`, and within a set only when the
  set holds both `:*` and `:softland.store/*`. A pattern of the given set
  is within when the giver's patterns name every key that it names
  (`pattern-within?`). Gives false for anything that `allows` cannot give.
  It never throws.

  A gate asks it of each permission that an act gives, against the act's
  own permission, which is the one straight above it; a give that allows
  more is refused `:permission-fact-allows-more-than-the-givers`
  (`softland.store.gate.decision/places`). Every permission allows seeing,
  so seeing is never more."
  [given givers]
  (let [wanted (patterns-of given)
        held (patterns-of givers)]
    (boolean (and wanted held (every? #(pattern-within? % held) wanted)))))

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act in its one form, which
  has the shape, and gives true when the act keeps the rule. A permission id
  in its one form is a vector whose second slot is its layer."
  {:permission-is-in-the-acts-layer
   (fn [act] (= (:layer act) (nth (:permission act) 1)))})

(defn- parse*
  "The body of `parse`, with no guard around it. Takes anything. Gives
  {:ok act} or {:refuse reason}. First the declared shape :act, then the
  rules in their order. It can throw; `parse` turns each throw into a
  refusal."
  [x]
  (let [[verdict got] (conform shapes :act x {})]
    (if (= :no verdict)
      {:refuse (or got :malformed)}
      (if-let [reason (some (fn [[rule reason]] (when-not ((get rule-checks rule) got) reason)) rules)]
        {:refuse reason}
        {:ok got}))))

(defn parse
  "The check of the form. Takes anything. Gives {:ok act}, with the act in
  its one form, or {:refuse reason}, with one of the `reasons`. It never
  throws, whatever x is, so that no caller has to guard it: a maker's tool,
  the door, or a gate, where a throw would stop the worker (a finding of the
  rig on Rama). So it turns each throw into :malformed.

  It reads the two declarations and holds no knowledge of the form itself.
  First the shape: the thing must have the declared shape :act (`shapes`).
  A thing that does not have it gets the reason of the first part that is
  wrong, in the order of the declaration. Then the rules: an act that has
  the shape must keep each rule (`rules`), and the first rule that it breaks
  gives the reason.

  The one form. Each part of the declaration is there. Each vector, map and
  set is plain, and each fixed-width integer is a long. A part that the act
  left out holds nil, or the default that the declaration gives: an empty
  map for `:based-on`, an empty set for `:subjects`. Each fact holds its
  four parts; a fact that gave no `:v`, or a nil one, holds `:v nil` and is
  a retract. To parse the :ok of a parse gives the same :ok.

  It is the check of the maker's act: what a maker gives, before the door
  adds an act-id, a depot and a time. An act that carries one of those
  three parts, and a fact that carries a part that the form does not name,
  are refused :unknown-part.

  Limits. It reads no clock and gives `:claimed-when` no default. It has no
  rule about a session beyond its shape. It examines the shape of a
  permission and its layer, not its standing. It examines a value with
  `value?` only. Facts that are a sequence with no end do not come back from
  it."
  [x]
  (try (parse* x)
       (catch Throwable _ {:refuse :malformed})))
