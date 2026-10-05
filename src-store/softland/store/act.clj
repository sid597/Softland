(ns softland.store.act
  "The act: its one form, its names, and the check of the form. Pure code. It
  needs no cluster, and it requires nothing of the rig and nothing of the door.

  An act is one fact or more under one name. A gate accepts the whole act or
  refuses the whole act. Whatever made an act, it has one form: a plain map of
  twelve parts that the store owns. Each fact is a plain map of a small core
  and a few pieces. Code checks the form: `parse` takes anything, and gives
  the act in its one form or the reason why it is not an act.

  Inputs and results, by call:

    parse        anything -> {:ok act} or {:refuse reason}
    make-name    a layer and a class -> a new name
    name-for     an act as its maker has it -> a new name for it
    sealed-view  an act with plain values -> the act as `parse` takes it after sealing
    digest       the :ok of a parse -> hex text for what the act says, its values aside

  State and effects. The namespace owns one constant, the secret of the
  digest. `make-name` with two arguments, and `name-for`, take a random id
  from the JVM's secure source. Each other function is pure. Nothing here
  reads a clock, appends, reads a record, or opens a sealed value.

  Callers. Only the proof of this namespace requires it. The door and the two
  gates use the rig's envelope, `rig.store.envelope`, and this file follows
  that one. Where a function follows one of the rig's, its docstring names
  that function and says how it differs. The form here is not the form that
  the rig's gates read; `parse` says where. So the door cannot change to this
  file by its require line alone. The eight names that the door takes from
  the rig's envelope have the same spelling and call shape here: `version`,
  `readable-keyword?`, `name-for`, `name-tag`, `valid-name?`, `sealed-view`,
  `parse` and `value-fact?`.

  Approach. The file has seven sections, in this order: the parts and the words
  that the form allows; the limits; ids and values; the name; the check; the
  stand-in sealed form, with which a maker examines an act before its values
  are sealed; the digest.

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

;; ---------------------------------------------------------------- the parts

(def version
  "The version marker of the form. This code knows this one version only."
  1)

(def act-parts
  "The twelve parts of an act. The store owns them, and `parse` refuses a
  thirteenth. A tool adds facts, never a part."
  #{:version :name :who :layer :class :permission :session :based-on
    :because-of :claimed-when :subjects :facts})

(def fact-parts
  "The parts of a fact. The core is the entity, the key and the value slot.
  The slot is a plain `:v`, or `:sealed` bytes with a `:lock-id`. The pieces
  are `:replaces`, `:mark`, and a `:layer` that must be the act's own."
  #{:e :k :v :sealed :lock-id :replaces :mark :layer})

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

(def marks
  "The words that the mark of a fact can hold."
  #{:die-with-any :own-row})

(def classes
  "The two classes of a layer: placed by layer, and placed by entity."
  #{:by-layer :by-entity})

(def made-scheme
  "The scheme word in a name that a maker makes. This constant is the one
  place that holds the word."
  :act)

(def store-schemes
  "The scheme words that only the store uses, for the two acts of a
  promotion."
  #{:crossing :landing})

(def schemes
  "Each scheme word that a name can carry."
  (conj store-schemes made-scheme))

(def reserved-who
  "Who no act that comes from outside the store can claim to be."
  #{:store})

(def reasons
  "The eleven words with which `parse` refuses."
  #{:malformed :unknown-part :unknown-version :bad-name :reserved-who
    :no-permission :reserved-scheme :empty-act :not-sealed
    :fact-outside-the-acts-layer :mis-tagged})

;; --------------------------------------------------------------- the limits

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

;; ----------------------------------------------------------- ids and values

(def ^:private token-pattern
  "The name, and the namespace, of a keyword whose printed form reads back as
  the same keyword."
  #"[\p{L}*+!_?$%&=<>.\-][\p{L}\p{N}*+!_?$%&=<>.\-'#]*")

(defn readable-keyword?
  "Takes anything. Gives true for an id: a keyword whose printed form reads
  back as the same keyword. The canonical text of a thing that holds only
  such keywords has one reading. The rig's `readable-keyword?`."
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
  already around x; a caller gives none. The rig's `edn-value?`."
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
  Limit: it does not examine x, and it goes as deep as x goes. The rig's
  `normalize-value`."
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
  text, and two different values never give one text. The rig's `canonical`,
  without its cases for a list and for a narrow integer, which a normal
  value never holds."
  [x]
  (binding [*print-readably* true *print-length* nil *print-level* nil
            *print-meta* false *print-dup* false]
    (canonical* x)))

;; ----------------------------------------------------------------- the name

(defn make-name
  "Gives a name: the vector [layer class scheme id]. With a layer and a class
  it makes a new name, with the scheme `made-scheme` and a random UUID. With
  all four slots it gives them back as a name and examines nothing;
  `valid-name?` examines. A maker makes the name of an act before a gate
  reads the act, so a maker can name each act of a series before the first
  one goes. Effect, with two arguments: it takes a random id. The rig's
  `make-name`, with one difference: the rig's id is a UUID of version 7,
  which holds the maker's clock. This one is random and holds no time."
  ([layer class] (make-name layer class made-scheme (random-uuid)))
  ([layer class scheme id] [layer class scheme id]))

(defn- placed-facts?
  "Takes the facts of an act. Gives true when the store places the act
  itself: there is a fact, and the key of each fact is a placed key. Facts
  that are no sequence give false. The rig's `store-placed-facts?`, which
  throws for facts that are no collection."
  [facts]
  (boolean (and (sequential? facts) (seq facts)
                (every? #(contains? placed-keys (:k %)) facts))))

(defn act-tag
  "Takes an act. Gives its tag: its layer, and its class, or nil in place of
  the class when the store places the act itself. A name made for the act
  carries this tag. The rig's `tag-of`."
  [act]
  [(:layer act) (when-not (placed-facts? (:facts act)) (:class act))])

(defn name-for
  "Takes an act as its maker has it; only its layer, class and facts are
  read. Gives a new name for it, with the scheme `made-scheme`: its tag and a
  random id. Effect: it takes a random id. The rig's `name-for`, in its shape
  of one argument."
  [act]
  (let [[layer class] (act-tag act)]
    (make-name layer class)))

(defn name-tag
  "Takes a valid name. Gives its tag: the layer and the class that the name
  was made for. Limit: it throws for a thing that has fewer than two slots.
  The rig's `name-tag`."
  [nm]
  [(nth nm 0) (nth nm 1)])

(defn valid-name?
  "Takes anything. Gives true for a name: a vector of four, with a layer that
  is an id, a class or nil, a scheme word, and a UUID of any version. The
  rig's `valid-name?`."
  [x]
  (and (vector? x) (= 4 (count x))
       (readable-keyword? (nth x 0))
       (or (nil? (nth x 1)) (contains? classes (nth x 1)))
       (contains? schemes (nth x 2))
       (uuid? (nth x 3))))

(defn crossing-name
  "Takes the name of a promotion's request. Gives the name of its crossing:
  the request's layer and id, no class, and the scheme :crossing. The rig's
  `crossing-name`."
  [request-name]
  [(nth request-name 0) nil :crossing (nth request-name 3)])

(defn landing-name
  "Takes the name of a promotion's request, the target layer, and the class
  of the target; :by-entity when no class is given. Gives the name of the
  landing: the target, that class, the scheme :landing and the request's id.
  The rig's `landing-name`."
  ([request-name target] (landing-name request-name target :by-entity))
  ([request-name target class] [target class :landing (nth request-name 3)]))

;; ---------------------------------------------------------------- the check

(defn fact-id?
  "Takes anything. Gives true for a fact id: a vector of a valid name and an
  index, where the index is a fixed-width integer of 0 or more. A lock id has
  the same shape. The rig's `fid?`."
  [x]
  (and (vector? x) (= 2 (count x)) (valid-name? (nth x 0))
       (int? (nth x 1)) (<= 0 (nth x 1))))

(defn permission-id?
  "Takes anything. Gives true for a permission id: a vector [who layer in] of
  three ids, or [who layer in parent] where the parent is a permission id.
  One id holds `max-permission-depth` ids at most, itself counted. The second
  argument is the count so far; a caller gives none. The rig's `pid?`, with
  its depth as a named limit."
  ([x] (permission-id? x 1))
  ([x depth]
   (and (vector? x) (<= depth max-permission-depth)
        (case (count x)
          3 (every? readable-keyword? x)
          4 (and (every? readable-keyword? (take 3 x))
                 (permission-id? (nth x 3) (inc depth)))
          false))))

(defn- carried-stamp?
  "Takes anything. Gives true for a stamp that an act can carry: a
  fixed-width integer below `max-carried-stamp`. A negative one passes."
  [x]
  (and (int? x) (< x max-carried-stamp)))

;; Each vector of the one form is built again with (into [] ...). `vec` gives
;; a subvector back as it is, and a subvector is not the plain vector that a
;; record of the store can hold as a key.

(defn- plain-vector
  "Takes a vector of any kind. Gives a plain vector of the same things."
  [v]
  (into [] v))

(defn- plain-fact-id
  "Takes a fact id. Gives it with a plain name and its index as a long."
  [x]
  [(plain-vector (nth x 0)) (long (nth x 1))])

(defn- plain-permission
  "Takes a permission id. Gives it as plain vectors, its parents too."
  [p]
  (cond-> (into [] (take 3 p))
    (= 4 (count p)) (conj (plain-permission (nth p 3)))))

(defn- value-slot
  "Takes a fact that is a plain map. Gives the value slot of its one form, or
  the reason why `parse` refuses the fact. The key of the fact tells which of
  three forms the slot has:

  - a store key: a plain `:v` that is a value, nil too, and no seal;
  - another key, with a value: `:sealed` bytes and a `:lock-id` that is a
    fact id, and no key `:v` at all;
  - another key, with no value: `:v` nil or absent, and no seal. This is a
    retract.

  :not-sealed is the reason for a seal on a store key, for a seal that is
  not whole, and for a plain value under another key. :malformed is the
  reason for a plain value that is not a value. It never reads sealed bytes.
  The rig's `value-slot`."
  [fact]
  (cond
    (contains? store-keys (:k fact))
    (cond
      (or (contains? fact :sealed) (contains? fact :lock-id)) :not-sealed
      (not (value? (:v fact))) :malformed
      :else {:v (normal-value (:v fact))})

    (contains? fact :sealed)
    (if (and (not (contains? fact :v)) (bytes? (:sealed fact)) (fact-id? (:lock-id fact)))
      {:sealed (:sealed fact) :lock-id (plain-fact-id (:lock-id fact))}
      :not-sealed)

    (or (contains? fact :lock-id) (some? (:v fact))) :not-sealed
    :else {:v nil}))

(defn- parse-fact
  "Takes one fact as given, and the layer of its act. Gives the fact in its
  one form, or the reason why `parse` refuses it. The first fault in this
  order gives the reason: not a plain map (:malformed); a part that a fact
  does not have (:unknown-part); an entity or a key that is no id
  (:malformed); the value slot (`value-slot`); a :replaces, a :mark or a
  :layer that is not as the form asks (:malformed); a layer that is not the
  layer of the act (:fact-outside-the-acts-layer).

  The one form has `:e`, `:k`, the slot, `:replaces` as a fact id or nil,
  `:mark` as a set, and `:layer` only when the fact gave one.

  The rig's `parse-fact`, with one difference. A fact that names a layer
  must name the layer of its act. The rig's parse asks only that the fact's
  layer is an id; the rig's gate refuses such an act later, in its decision,
  with this same word, and records that refusal. Here the refusal is on the
  face of the act, and so a gate that uses this check records nothing for
  it."
  [fact layer]
  (let [slot (when (and (map? fact) (not (record? fact))) (value-slot fact))]
    (cond
      (not (map? fact)) :malformed
      (record? fact) :malformed
      (not-every? fact-parts (keys fact)) :unknown-part
      (not (readable-keyword? (:e fact))) :malformed
      (not (readable-keyword? (:k fact))) :malformed
      (keyword? slot) slot
      (not (or (nil? (:replaces fact)) (fact-id? (:replaces fact)))) :malformed
      (not (or (nil? (:mark fact)) (and (set? (:mark fact)) (every? marks (:mark fact))))) :malformed
      (not (or (nil? (:layer fact)) (readable-keyword? (:layer fact)))) :malformed
      (not (or (nil? (:layer fact)) (= layer (:layer fact)))) :fact-outside-the-acts-layer
      :else (cond-> (merge {:e (:e fact) :k (:k fact)}
                           slot
                           {:replaces (some-> (:replaces fact) plain-fact-id)
                            :mark (into #{} (:mark fact))})
              (some? (:layer fact)) (assoc :layer (:layer fact))))))

(defn- parse*
  "The body of `parse`, with no guard around it. Takes anything. Gives
  {:ok act} or {:refuse reason}. It can throw, for example for a sorted map
  whose keys do not compare with a keyword, or for facts that throw when
  they are read; `parse` turns each throw into a refusal."
  [x]
  (let [facts (when (map? x) (:facts x))
        based-on (when (map? x) (:based-on x))
        subjects (when (map? x) (:subjects x))]
    (cond
      (not (map? x)) {:refuse :malformed}
      (record? x) {:refuse :malformed}
      (not-every? act-parts (keys x)) {:refuse :unknown-part}
      (not= version (:version x)) {:refuse :unknown-version}
      (not (valid-name? (:name x))) {:refuse :bad-name}
      (not (readable-keyword? (:who x))) {:refuse :malformed}
      (contains? reserved-who (:who x)) {:refuse :reserved-who}
      (not (readable-keyword? (:layer x))) {:refuse :malformed}
      (not (contains? classes (:class x))) {:refuse :malformed}
      (nil? (:permission x)) {:refuse :no-permission}
      (not (permission-id? (:permission x))) {:refuse :malformed}
      (not (or (nil? (:session x)) (readable-keyword? (:session x)))) {:refuse :malformed}
      (not (or (nil? based-on)
               (and (map? based-on) (not (record? based-on))
                    (every? (fn [[k v]] (and (fact-id? k) (carried-stamp? v))) based-on))))
      {:refuse :malformed}
      (not (or (nil? (:because-of x)) (valid-name? (:because-of x)))) {:refuse :malformed}
      (not (or (nil? (:claimed-when x)) (int? (:claimed-when x)))) {:refuse :malformed}
      (not (or (nil? subjects) (and (set? subjects) (every? readable-keyword? subjects))))
      {:refuse :malformed}
      (< max-subjects (count subjects)) {:refuse :malformed}
      (not (sequential? facts)) {:refuse :malformed}
      (contains? store-schemes (nth (:name x) 2)) {:refuse :reserved-scheme}
      (empty? facts) {:refuse :empty-act}
      :else
      (let [parsed (mapv #(parse-fact % (:layer x)) facts)]
        (if-let [fault (first (filter keyword? parsed))]
          {:refuse fault}
          (let [act {:version version
                     :name (plain-vector (:name x))
                     :who (:who x)
                     :layer (:layer x)
                     :class (:class x)
                     :permission (plain-permission (:permission x))
                     :session (:session x)
                     :based-on (into {} (map (fn [[k v]] [(plain-fact-id k) (long v)])) based-on)
                     :because-of (some-> (:because-of x) plain-vector)
                     :claimed-when (some-> (:claimed-when x) long)
                     :subjects (into #{} subjects)
                     :facts parsed}]
            (if (not= (act-tag act) (name-tag (:name act)))
              {:refuse :mis-tagged}
              {:ok act})))))))

(defn parse
  "The check of the form. Takes anything. Gives {:ok act}, with the act in
  its one form, or {:refuse reason}, with one of the eleven `reasons`. It
  never throws, whatever x is: an exception in the code of a gate stops the
  worker. So it turns each throw into :malformed.

  It is the check for an act that comes from outside the store. It does not
  know which gate reads the act.

  The one form. All twelve parts are there. The name, each fact id and the
  permission are plain vectors. `:based-on` is a map from a fact id to a
  long, and `:subjects` is a set; both are empty when the act gave none.
  `:claimed-when` is a long or nil. `:session` and `:because-of` are nil
  when the act gave none. `:facts` is a vector of facts in their one form
  (`parse-fact`). To parse the :ok of a parse gives the same :ok.

  The order of the checks decides the reason when an act has two faults:

   1. not a plain map: :malformed;
   2. a part that an act does not have: :unknown-part;
   3. a version that is not 1: :unknown-version;
   4. a name that is not valid: :bad-name;
   5. a :who that is no id: :malformed; a :who of the store: :reserved-who;
   6. a layer that is no id, or a class that is not one: :malformed;
   7. no permission: :no-permission; a permission that is no permission id:
      :malformed;
   8. each :malformed: a session, a :based-on, a :because-of, a
      :claimed-when or subjects that are not as the form asks; more than
      `max-subjects`; facts that are no sequence;
   9. a name with a scheme of the store: :reserved-scheme;
  10. no fact: :empty-act;
  11. the facts in order: the first fact with a fault gives its reason;
  12. a tag of the act that is not the tag of its name: :mis-tagged.

  Limits. It reads no clock and gives `:claimed-when` no default. It has no
  rule about a session. It examines the shape of a permission and not its
  standing. It does not examine the value of a store key beyond `value?`.
  It refuses the store's own acts, a landing too: a gate that takes a
  landing cannot use this check for it. Facts that are a sequence with no
  end do not come back from it.

  The rig's `parse`, as its stream gate calls it, with five differences.
  The part for what the act stood on is `:based-on`; the rig's key is
  another. The scheme word of a made name is `made-scheme`; the rig's word
  is another. An act must name a permission; the rig's parse takes an act
  with none, and the rig's gate lets the operator name none. A fact that
  names another layer is refused here (`parse-fact`). And this check has no
  gate: the rig's parse takes the gate as a second argument, refuses a name
  whose class is for the other gate, and takes a landing at its microbatch
  gate."
  [x]
  (try (parse* x)
       (catch Throwable _ {:refuse :malformed})))

;; ------------------------------------------------- the stand-in sealed form

(defn value-fact?
  "Takes anything. Gives true for a fact whose value a door must seal: a map,
  under a key that is not a store key, with a plain value that is not nil. A
  retract has no value, and a fact that is sealed has no plain value. The
  rig's `value-fact?`."
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
  as it is, and so does a thing that throws when it is read. The rig's
  `sealed-view`, which throws for such a thing."
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
  its secret in the module, not in source. It is not the rig's secret."
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
  it only what `parse` gave. The rig's `digest`, under another secret and
  without the case of a landing's box. The two give different text for one
  act, because the secret, one part's key and the scheme word differ."
  [act]
  (keyed-hex (canonical-text (update (dissoc act :name) :facts #(mapv digest-fact %)))))
