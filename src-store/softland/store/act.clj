(ns softland.store.act
  "The act: its one form, declared as data in `shapes` and `rules`, and
  `parse`, the check of that form. Pure code.

  The form is the maker's act: what a maker gives, with plain values, before
  the door adds an act-id and a time. A gate accepts the whole act or
  refuses the whole act.

  The shape is code and not a fact in the store. Each shape that a tool
  gives enters the store as a fact, inside an act, so the shape of the act
  is the floor: it must hold when each fact is wrong. It is declared as data
  so that it can be read in one place, printed and shown.

  Who acts is a party, and a party's id says what it is: `:person.<id>`,
  `:agent.<id>`, `:run.<id>`, or `:system`. A permission is held by a party
  or by everyone: `:anyone`, which is anyone at all, and
  `:anyone-signed-in`. Those two hold and never act: an act needs an
  author.

  An act names the permission that it acts under by that permission's id, a
  UUID that its giver made. It names none when it makes its lane, and when
  all it does is end permissions that its author holds. What a permission
  says is a fact's value, and the door's declaration holds its shape
  (`softland.store.door.declaration`, `:permission-value`)."
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]))

;; ------------------------------------------------- the words and the limits

(def version
  "The edition of the list of parts. This code knows this one version only."
  1)

(def max-depth
  "How many collections a value can be inside."
  32)

;; ------------------------------------------------------- the declared shape

(def shapes
  "The shape of an act, and of each thing that an act holds. Plain data. A
  shape is a leaf that code checks (`leaves`), another name of this map, or
  a form that `conform` reads.

  The order of the parts is the order of the checks: when a thing has two
  faults, the part that is declared first gives the reason. Before any
  part is read, a thing that holds a part that the list does not name is
  refused `:unknown-part`; but the part marked `:edition`, which says which
  list of parts the thing was made by, is read before that word is said. A
  thing of another edition is then refused with the edition's reason: the
  list of another edition can name the part.

  An `:optional` part can be absent or nil; the one form then holds nil for
  it, or its `:default`. A part with no `:reason` is refused `:malformed`.
  The engine also knows `:never` in place of a shape, for a part that the
  thing does not carry at all, not even as nil, and `:left-out`, for an
  absent part that is absent from the one form too. No shape of this
  declaration uses those two."
  {:act
   [:parts
    [:version      [:enum version]                    {:reason :unknown-version :edition true}]
    [:who          :author-party                      {:reason :who-not-a-party}]
    [:lane        :id]
    [:permission   :uuid                              {:optional true :reason :permission-not-an-id}]
    [:session      :id                                {:absent :no-session}]
    [:based-on     [:map-of :fact-id :carried-stamp]  {:optional true :default {}}]
    [:because-of   :act-id                            {:optional true}]
    [:facts        [:seq-of :fact]                    {:empty :empty-act}]]

   ;; A fact with no value is a retract. Its key is an id, as its entity
   ;; is: a readable keyword, or a UUID, for a key that is a thing of its
   ;; maker's with a name of its own.
   :fact
   [:parts
    [:e        :id]
    [:k        :id]
    [:v        :value   {:optional true}]
    [:replaces :fact-id {:optional true}]]

   ;; [lane uuid]. The door makes it. The lane is there so that the act can
   ;; be routed and the door can find its answer on the lane's task. Which
   ;; depot a lane is on is the lane's to say, in a fact of its own
   ;; (`:softland.store/depot`), and no id holds it.
   :act-id
   [:tuple :id :uuid]

   ;; [act-id index]: one fact of one act.
   :fact-id
   [:tuple :act-id [:int {:min 0}]]

   ;; The stamp of a fact that the act is based on, as its maker saw it. A
   ;; label: no gate reads it, and the act's own stamp is not raised above
   ;; it (`softland.store.gate.stamp`). So it has no bound but its form.
   :carried-stamp
   [:int {:min 0}]})

(def ^:private nesting
  "For a named shape, how many times it can be around one place, the place's
  own counted. A name that is not here has the number 1, so no shape goes
  round. No shape of this declaration is inside itself."
  {})

(def rules
  "What a shape cannot say: a rule relates two places of the act. Each entry
  is a rule and its reason, in the order of the checks; `rule-checks` holds
  the code. The maker's act has none: the permission that it names is an id,
  and what that id holds is a gate's question."
  [])

;; ------------------------------------------ what the declaration says, as sets

(defn- parts-of
  [shape-name]
  (into #{} (map first) (rest (get shapes shape-name))))

(def act-parts
  "`parse` refuses a part that is not one of them: a tool adds facts, never
  a part."
  (parts-of :act))

(def fact-parts
  (parts-of :fact))

(def reasons
  "Every word with which `parse` refuses."
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
  "True for a keyword whose printed form reads back as the same keyword."
  [x]
  (boolean (and (keyword? x)
                (re-matches token-pattern (name x))
                (let [ns (namespace x)] (or (nil? ns) (re-matches token-pattern ns))))))

(defn- fits-long?
  [x]
  (<= Long/MIN_VALUE x Long/MAX_VALUE))

(defn value?
  "True for a value that the store can hold. A ratio, a Java float, a list
  and a record are not values. `depth` is the number of collections already
  around x; a caller gives none."
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
  "The one normal form of a value. It is = to the value given, so a value
  reads back as its maker gave it, however the maker built it. A vector
  comes back plain, never a subvector. What must hold: x is a value, as
  `value?` knows one. It does not examine x, and it goes as deep as x
  goes."
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

(def everyone
  "The two holders that are everyone: anyone at all, and anyone signed in.
  They hold a permission and never act or ask."
  #{:anyone :anyone-signed-in})

(def ^:private party-pattern
  "The name of a party's id: its kind, a dot, and the id that the kind's
  namer gave it."
  #"(person|agent|run)\..+")

(defn party-kind
  "What a party is, read from its id: `:person`, `:agent`, `:run`,
  `:system`, or `:everyone` for one of `everyone`; nil for anything else.
  The kind is in the id so that a gate tells a person from an agent or a
  run with no read. It never throws."
  [x]
  (cond
    (= :system x) :system
    (contains? everyone x) :everyone
    (and (readable-keyword? x) (nil? (namespace x)))
    (when-let [[_ kind] (re-matches party-pattern (name x))]
      (keyword kind))
    :else nil))

(defn author-party?
  "True for a party that can act, give and ask: a person, an agent, a run or
  the system."
  [x]
  (contains? #{:person :agent :run :system} (party-kind x)))

(defn holder-party?
  "True for what can hold a permission: a party that can act, or everyone."
  [x]
  (some? (party-kind x)))

(defn borrower?
  "True for an agent or a run: a party that holds only what is lent."
  [x]
  (contains? #{:agent :run} (party-kind x)))

(def ^:private leaves
  "The shapes that code checks, by name: the only places where the check of
  a shape is code and not data. Each takes a thing and gives [:ok, its
  normal form], or nil when the thing does not fit."
  {:id    (fn [x] (when (or (readable-keyword? x) (uuid? x)) [:ok x]))
   :uuid  (fn [x] (when (uuid? x) [:ok x]))
   :int   (fn [x] (when (int? x) [:ok (long x)]))
   :value (fn [x] (when (value? x) [:ok (normal-value x)]))
   :author-party (fn [x] (when (author-party? x) [:ok x]))
   :holder-party (fn [x] (when (holder-party? x) [:ok x]))
   :digest-hex (fn [x] (when (and (string? x) (re-matches #"[0-9a-f]{64}" x)) [:ok x]))})

;; ---------------------------------------------------------------- the engine

(declare conform)

(defn- conform-all
  [declared pairs seen]
  (reduce (fn [[_ out] [shape x]]
            (let [[verdict normal :as got] (conform declared shape x seen)]
              (if (= :ok verdict) [:ok (conj out normal)] (reduced got))))
          [:ok []]
          pairs))

(defn- conform-parts
  [declared entries x seen]
  (let [in-order
        (fn [entries]
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
           entries))]
    (cond
      (not (and (map? x) (not (record? x)))) [:no :malformed]

      (not-every? (into #{} (map first) entries) (keys x))
      (let [edition (in-order (filter (fn [[_ _ opts]] (:edition opts)) entries))]
        (if (= :no (first edition)) edition [:no :unknown-part]))

      :else (in-order entries))))

(defn- conform
  "The engine. `seen` is the count of each named shape that is around this
  place. The leaves and the nesting limits are this namespace's own,
  whatever map is given. The reason of a [:no reason] is nil when the shape
  names none; the part that holds the place then gives its own. A shape
  that the engine does not know fits nothing.

  It can throw, for a thing that throws when it is read. `parse` and
  `has-shape?` turn each throw into a refusal."
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
  "It never throws. For :act it examines the shape only, not the rules;
  `parse` examines both."
  [shape-name x]
  (try (= :ok (first (conform shapes shape-name x {})))
       (catch Throwable _ false)))

(defn as-shape
  "Gives the engine to another namespace, for a map of shapes in the
  language of `shapes`. The map may hold the entries of `shapes` and add its
  own. The reason is the word of the first part that is wrong, or nil when
  that part names none.

  The leaves and the nesting limits stay this namespace's, so a named shape
  of the caller's can be inside itself no times. It can throw, for a thing
  that throws when it is read; the caller turns a throw into a refusal, as
  `parse` does."
  [declared shape-name x]
  (conform declared shape-name x {}))

;; ---------------------------------------------------------------- the check

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act in its one form, which
  has the shape."
  {})

(defn- parse*
  [x]
  (let [[verdict got] (conform shapes :act x {})]
    (if (= :no verdict)
      {:refuse (or got :malformed)}
      (if-let [reason (some (fn [[rule reason]] (when-not ((get rule-checks rule) got) reason)) rules)]
        {:refuse reason}
        {:ok got}))))

(defn parse
  "The check of the form: {:ok act}, with the act in its one form, or
  {:refuse reason}, with one of `reasons`. It never throws, whatever x is:
  a throw in a gate would stop the worker (a finding of the rig on Rama).
  Each throw is `:malformed`.

  The shape first, then the rules. The first part or rule that is wrong, in
  the declared order, gives the reason. An act with a part that the form
  does not name is refused `:unknown-part` before any part is read, unless
  its version is another: then `:unknown-version`.

  The one form. Each part of the declaration is there. Each vector, map and
  set is plain, and each fixed-width integer is a long. A part that the act
  left out holds nil, or the default that the declaration gives. A fact
  that gave no `:v`, or a nil one, holds `:v nil` and is a retract. To parse
  the :ok of a parse gives the same :ok.

  An act that carries an act-id or a time is refused `:unknown-part`: those
  two are the door's.

  Limits. It reads no clock. It has no rule about a session beyond its
  shape. Of the permission that the act
  names it examines that it is an id, and nothing of what that id holds.
  Facts that are a sequence with no end do not come back from it."
  [x]
  (try (parse* x)
       (catch Throwable _ {:refuse :malformed})))
