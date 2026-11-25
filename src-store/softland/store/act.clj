(ns softland.store.act
  "The act: its one form, declared as data in `shapes` and `rules`, and
  `parse`, the check of that form. Pure code.

  The form is the maker's act: what a maker gives, with plain values, before
  the door adds an act-id, a depot and a time. A gate accepts the whole act
  or refuses the whole act.

  The shape is code and not a fact in the store. Each shape that a tool
  gives enters the store as a fact, inside an act, so the shape of the act
  is the floor: it must hold when each fact is wrong. It is declared as data
  so that it can be read in one place, printed and shown.

  A permission whose sixth slot is `[:run session]` covers one run: the
  acts that carry that session, and the reads that are asked in it. One
  whose sixth slot is `[:act yes digest]` is a yes for one act. `yes` is a
  UUID that its giver makes, and the yes sits on it. `digest` is the digest
  of the act as its maker made it, with its who and its session, and without
  its act-id, its depot, its time and its permission
  (`softland.store.door.digest/hex-for-a-yes`). So a yes is made before the
  act has an act-id, and the act goes under any act-id of its maker's. A yes
  covers its act once: a gate uses it up when it takes the act, and a try
  that is refused uses nothing. A yes that said `:see` would allow nothing."
  {:softland/whose-call
   [{:state :decided
     :what "The form of an act is the maker's act: what a maker gives, with
            plain values, before the door touches it. Whatever made an act,
            it has one form."
     :source "Sid's decision of 6 October 2026. The twelve decisions are
              listed in src-store/README.md, which marks the two that wait
              for his word"}
    {:state :decided
     :what "A permission says what it allows: `:see`, or `:see-and-act`."
     :source "Sid's decision of 11 and 12 October 2026 (src-store/README.md,
              entry 1)"}
    {:state :not-decided
     :what "That a permission says what it allows in a slot of its own id is
            the builder's."}
    {:state :decided
     :what "A permission covers an act, or a run, and a holder can have more
            than one in a lane."
     :source "Sid's decisions of 21 November 2026 (src-store/README.md,
              entries 4 to 6 of 20 and 21 November)"}
    {:state :not-decided
     :what "The form of a permission for a run, `[who lane granted-in parent
            allows [:run session]]`, is a session's."
     :source "src/proposal/runtime-2026-11-21/store-piece-2/SPEC.md"}
    {:state :not-decided
     :what "The form of a yes, `[who lane granted-in parent :see-and-act
            [:act yes digest]]`, with an id of its own and used once, is a
            session's, and Sid's to turn."
     :source "the prompt that Sid pasted on 22 November 2026 as his go for
              the build"}
    {:state :decided
     :what "\"Depot\" is Sid's word for the depot that takes a lane's acts,
            `:streaming` or `:microbatch`. His decisions before that day say
            \"class\" for it."
     :source "Sid, 9 October 2026"}
    {:state :not-decided
     :what "The word act-id is a session's, from Sid's \"act-uuid\" of 7
            October 2026: the session wrote act-id, since the id holds more
            than the UUID, and the word is his to change. The ruling of 24
            September said \"name\"."}]}
  (:import [clojure.lang BigInt]
           [java.math BigDecimal BigInteger]))

;; ------------------------------------------------- the words and the limits

(def version
  "The edition of the list of parts. This code knows this one version only."
  1)

(def max-subjects
  256)

(def max-depth
  "How many collections a value can be inside. `nesting` reads the same
  number for how deep a permission can be."
  32)

;; ------------------------------------------------------- the declared shape

(def shapes
  "The shape of an act, and of each thing that an act holds. Plain data. A
  shape is a leaf that code checks (`leaves`), another name of this map, or
  a form that `conform` reads.

  The order of the parts is the order of the checks: when a thing has two
  faults, the part that is declared first gives the reason.

  An `:optional` part can be absent or nil; the one form then holds nil for
  it, or its `:default`. A part with no `:reason` is refused `:malformed`.
  The engine also knows `:never` in place of a shape, for a part that the
  thing does not carry at all, not even as nil, and `:left-out`, for an
  absent part that is absent from the one form too. No shape of this
  declaration uses those two."
  {:act
   [:parts
    [:version      [:enum version]                    {:reason :unknown-version}]
    [:who          :id]
    [:lane        :id]
    [:permission   :permission-id                     {:absent :no-permission}]
    [:session      :id                                {:absent :no-session}]
    [:based-on     [:map-of :fact-id :carried-stamp]  {:optional true :default {}}]
    [:because-of   :act-id                            {:optional true}]
    [:claimed-when :int                               {:optional true}]
    [:subjects     [:set-of :id {:max max-subjects}]  {:optional true :default #{}}]
    [:facts        [:seq-of :fact]                    {:empty :empty-act}]]

   ;; A fact with no value is a retract.
   :fact
   [:parts
    [:e        :id]
    [:k        :key]
    [:v        :value   {:optional true}]
    [:replaces :fact-id {:optional true}]]

   ;; [lane depot uuid]. The door makes it. The lane and the depot are there
   ;; so that the act can be routed and the door can find its answer in the
   ;; right record.
   :act-id
   [:tuple :id [:enum :streaming :microbatch] :uuid]

   ;; [act-id index]: one fact of one act.
   :fact-id
   [:tuple :act-id [:int {:min 0}]]

   ;; [who lane granted-in] is a first permission, with nothing above it.
   ;; [who lane granted-in parent] is a given one. A fifth slot says what it
   ;; allows, and a sixth what it covers: one run, or one act. A tuple fits
   ;; only its own count, and the two forms of six slots differ in the word
   ;; of the sixth, so each thing fits one alternative at most. The fifth
   ;; and the sixth slot are no permission, so they do not count toward the
   ;; nesting limit.
   :permission-id
   [:or [:tuple :id :id :id]
        [:tuple :id :id :id :permission-id]
        [:tuple :id :id :id :permission-id [:enum :see :see-and-act]]
        [:tuple :id :id :id :permission-id [:enum :see :see-and-act] [:tuple [:enum :run] :id]]
        [:tuple :id :id :id :permission-id [:enum :see-and-act] [:tuple [:enum :act] :uuid :digest-hex]]]

   ;; The stamp of a fact that the act is based on, as its maker saw it. A
   ;; label: no gate reads it, and the act's own stamp is not raised above
   ;; it (`softland.store.gate.stamp`). So it has no bound but its form.
   :carried-stamp
   [:int {:min 0}]})

(def ^:private
  ^{:softland/whose-call
    [{:state :not-decided
      :what "How deep a permission can be is Sid's to say. Kept: as deep as a
             value can be, `max-depth`, 32. The entry of `:permission-id` in
             `nesting` is the one place where another answer is made. A
             smaller number ends both roads there, the act's own
             `:permission` and the value of a fact that gives a permission. A
             greater one needs a greater `max-depth` too, and must stay under
             62: the one text of a thing (`softland.store.door.text`) writes
             a given permission whole to depth 61 and no deeper."}]}
  nesting
  "For a named shape, how many times it can be around one place, the place's
  own counted. A name that is not here has the number 1, so no shape goes
  round. A permission whose count is over its number is `:malformed`.

  The depth of a permission is `max-depth` on purpose. A permission is held
  on two roads: as an act's own `:permission`, which this number bounds, and
  as the value of a fact that gives a permission, which `value?` bounds
  first. The innermost vector of a permission of depth d has d - 1
  collections around it, so a given permission can have depth 32 too, and
  the two roads agree because they read one number.

  A greater number needs a greater `max-depth` too, and must stay under 62:
  the one text of a thing (`softland.store.door.text`) writes a given
  permission whole to depth 61 and no deeper."
  {:permission-id max-depth})

(def rules
  "What a shape cannot say: a rule relates two places of the act. Each entry
  is a rule and its reason, in the order of the checks; `rule-checks` holds
  the code.

  A permission cited for another lane cannot cover the act, whatever its
  standing. Whether it stands is a gate's question."
  [[:permission-is-in-the-acts-lane :permission-not-in-lane]])

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

(def ^:private leaves
  "The shapes that code checks, by name: the only places where the check of
  a shape is code and not data. Each takes a thing and gives [:ok, its
  normal form], or nil when the thing does not fit."
  {:id    (fn [x] (when (or (readable-keyword? x) (uuid? x)) [:ok x]))
   :key   (fn [x] (when (readable-keyword? x) [:ok x]))
   :uuid  (fn [x] (when (uuid? x) [:ok x]))
   :int   (fn [x] (when (int? x) [:ok (long x)]))
   :value (fn [x] (when (value? x) [:ok (normal-value x)]))
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

(defn allows
  "What a permission allows: the word of its fifth slot, and `:see-and-act`
  for a permission with three or four slots. So a permission that was given
  before the slot existed reads as \"see and act\", and this is the one
  place where that is code. A gate refuses an act under a permission that
  allows `:see`. The read path does not ask: any permission that is there
  allows seeing (`softland.store.read-path.lanes/standing`).

  What must hold: the thing has the shape `:permission-id`. It never throws:
  anything that is no vector of five slots or six gives `:see-and-act`, so a
  caller that holds a thing of no shape tests the shape first."
  [permission]
  (if (and (vector? permission) (<= 5 (count permission) 6))
    (nth permission 4)
    :see-and-act))

(defn covers
  "What a permission covers, its sixth slot: `[:run session]`,
  `[:act yes digest]`, or nil for a permission with no sixth slot, which
  covers its lane until it is ended. The one place where that slot is read.

  What must hold: the thing has the shape `:permission-id`. It never throws:
  anything that is no vector of six slots gives nil."
  [permission]
  (when (and (vector? permission) (= 6 (count permission)))
    (nth permission 5)))

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act in its one form, which
  has the shape. The second slot of a permission id is its lane."
  {:permission-is-in-the-acts-lane
   (fn [act] (= (:lane act) (nth (:permission act) 1)))})

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
  the declared order, gives the reason.

  The one form. Each part of the declaration is there. Each vector, map and
  set is plain, and each fixed-width integer is a long. A part that the act
  left out holds nil, or the default that the declaration gives. A fact
  that gave no `:v`, or a nil one, holds `:v nil` and is a retract. To parse
  the :ok of a parse gives the same :ok.

  An act that carries an act-id, a depot or a time is refused
  `:unknown-part`: those three are the door's.

  Limits. It reads no clock and gives `:claimed-when` no default. It has no
  rule about a session beyond its shape. It examines the shape of a
  permission and its lane, not its standing. Facts that are a sequence with
  no end do not come back from it."
  [x]
  (try (parse* x)
       (catch Throwable _ {:refuse :malformed})))
