(ns softland.store.door.declaration
  "The door's declaration: what arrives on a depot, declared as data, and the
  check of it. Pure code. It needs no cluster.

  What arrives on a depot is the maker's act (`softland.store.act`) plus what
  the door adds: an act-id, the layer's depot, and for each value fact its
  `:v` replaced by `:sealed` bytes and a `:lock-id`. A fact under a store
  key keeps a plain value. Anything can append to a depot without the door,
  so a gate checks each thing that arrives against this declaration, and the
  gate is the authority.

  It is made as `act.clj` is made, of the same three things in the same
  order. Read `shapes` and `rules` first. `shapes` is `act.clj`'s own map of
  shapes with the door's entries added; the maker's shapes are in it as
  `act.clj` declares them and are not written a second time. `rules` is a list of
  pairs, the name of a rule and its reason word, in the order in which they
  are examined; `rule-checks` holds the code of each. `check` reads the two
  and holds no knowledge of the form itself, so the order of the
  declarations is the order of the reasons. The engine that reads a shape is
  `act.clj`'s own, given this namespace's map by
  `softland.store.act/as-shape`; no other way into `act.clj`'s check is
  used.

  Inputs and results, by call:

    stray-reason  a gate's depot, and anything -> nil, or the word of a stray
    check         a thing that is no stray -> {:ok act} or {:refuse reason}
    store-key?    a fact's key -> true for one of the store keys
    id-text       an id or a fact's key -> its printed text
    id-within-the-limit?  an id or a fact's key -> true or false
    usable-id?    anything -> true for an id within the limit
    act-id-in-layer?  a layer id, and anything -> true for an act-id of it

  The store's namespace, the store keys, the limits and the words of a stray
  are declared here too, each in one place. The store's keys are in the
  namespace `softland.store`, and the store owns the whole of it: a fact
  whose key is in that namespace and is no store key is refused. Any other
  key is a plain key, a tool's own. `store-keys` is the table of the store
  keys: it says which is a setting, and which takes a second write
  (`second-write-keys`).

  The one part that the engine does not read is a fact's `:sealed`. The
  engine takes the map of shapes; the leaves, the few shapes that code
  checks, stay `act.clj`'s own, and none of them fits a run of bytes. So
  `check` sets each fact's `:sealed` aside before the engine reads the
  thing, puts it back on the one form, and two rules in code then hold it.

  State and effects. None. Each function is pure, and `stray-reason` and
  `check` never throw: a throw in a gate would stop its partition (a finding
  of the rig on Rama).

  Callers. `softland.store.gate.decision`, `softland.store.record.rows` and the two
  gates (`softland.store.module.streaming-gate`, `softland.store.module.microbatch-gate`).
  And the door's additions and its stock (`softland.store.door.additions`,
  `softland.store.door.stock`), for the tests of an id and of an act-id,
  the store keys and the limits.
  The door seals and sends; it is the gate that checks.

  Not here. Whether a thing's permission stands, whether a fact that it
  replaces is current, whether a lock opens a sealed value: those need the
  record, and are `softland.store.gate.decision`'s. A grammar check is not built."
  (:require [softland.store.act :as act]))

;; ------------------------------------------------------------- the limits

(def max-facts
  "How many facts one act can hold. A number of the build, kept and not
  decided; it bounds the rows that one event writes."
  256)

(def max-based-on
  "How many entries the `:based-on` of one act can hold. A number of the
  build, kept and not decided."
  1024)

(def max-sealed-bytes
  "How many bytes one sealed value can hold. A number of the build, kept and
  not decided; it bounds the bytes of one row."
  65536)

(def max-locks-in-request
  "How many locks one request can ask for, and so the count of the indexes of
  a lock: 0 to 255. A number of the build, kept and not decided."
  256)

(def max-id-text
  "How many characters the printed text of an id, or of a fact's key, can
  have. A readable keyword has no greatest length in the form, and a row's
  key is made of two or three of these texts; so without this limit a key
  had none. A number of the build, kept and not decided."
  256)

(def max-builds-on
  "How many layers the fact under `:softland.store/builds-on` can name. A
  number of the build, kept and not decided. Sid, 9 October 2026: \"if
  someone is building like 50 layers\". With each id at its limit the row of
  that fact is about 16.5 KB."
  64)

(def max-marked-keys
  "How many keys the fact under `:softland.store/marked-keys` can hold. A
  number of the build, kept and not decided. A gate reads the row of that
  fact for each act with a fact under a plain key, so the number bounds a
  row that is read often: about 8.3 KB of keys and 9 KB of stamps at the
  limits."
  32)

;; --------------------------------------------------------- the store keys

(def store-namespace
  "The namespace of the store's own keys: `softland.store`. The word is
  Sid's, of 9 October 2026 (src-store/README.md, K4 B). The store owns the
  whole of it: a fact whose key has this namespace is under one of
  `store-keys`, or a gate refuses it (the rule `:store-key-is-known`). A key
  with another namespace, and a key with none, is a plain key and a tool's
  own."
  "softland.store")

(def store-keys
  "The keys that the store itself reads, each with: why it is a store key;
  the shape that its value must have, as a name of `shapes`; the entity that
  its fact sits on; whether it is a setting; and whether its fact can be
  written again. A fact under one of them keeps a plain value on arrival. A
  setting is a key whose fact a gate takes only on the layer's own entity.
  A key that is `:written-again` takes a second write on its place, and a
  third; any other store key takes one write for a place
  (`softland.store.gate.decision`, what a store key does not take). A
  retract under any of them is refused.

  The last three are of the read build (src-store/README.md, decisions of
  11 and 12 October 2026, entries 1, 2 and 6). Who may see a layer:
  `:members`, which is whoever holds a permission in it, or `:everyone`.
  What a layer builds on: the layers that a read looks through after it, in
  the writer's order; an empty vector says \"nothing\". The keys that a
  layer marks: for each of them a gate keeps every fact in one more order,
  by value; the empty set says \"none\", and a mark is taken off by writing
  the set again without it. What each reads as when no fact says it is
  `softland.store.gate.first-act/starts-with`. The three words of the keys
  are a session's, and Sid's to change.

  Each is in the namespace `softland.store` (`store-namespace`). Sid decided
  on 9 October 2026 that the store's keys carry a namespace
  (src-store/README.md, K4 B), and gave the words: `softland.store` before
  the slash, and \"depot\" for the kind of a layer, streaming or microbatch.
  So a bare word is no store key. A fact of a tool under `:depot`,
  `:permission` or `:lock-request` is a fact under a plain key, and the
  store reads nothing from it.

  `:sits-on` is a word for the reader of this table; the rules
  `:permission-fact-sits-on-its-holder` and `:lock-request-sits-on-its-act`
  and the gate's check of a setting hold what it says.

  A permission fact sits on its holder, so the gate finds the permission of
  an act's `:who` with one read, and a holder has one permission in a layer:
  she cannot hold \"see\" from one giver and \"see and act\" from another.
  That is not decided, and is kept as built; the rule
  `:permission-fact-sits-on-its-holder` and
  `softland.store.gate.decision/permission-place` are where the other answer
  would be made.
  A request for locks sits on the uuid of its own act, so its place is new
  by its form and no read is needed to hold one write for a place."
  {:softland.store/depot
   {:why           "a gate must read which gate a layer belongs to"
    :value         :depot-word
    :sits-on       :the-layer
    :setting       true
    :written-again false}
   :softland.store/permission
   {:why           "a gate must read whether an act's permission stands"
    :value         :permission-id
    :sits-on       :the-holder-that-the-permission-names
    :setting       false
    :written-again false}
   :softland.store/lock-request
   {:why           "in Rama a query cannot write, so the door asks for locks by an act"
    :value         :lock-count
    :sits-on       :the-uuid-of-its-own-act
    :setting       false
    :written-again false}
   :softland.store/who-may-see
   {:why           "a read must find who may see a layer"
    :value         :who-may-see-word
    :sits-on       :the-layer
    :setting       true
    :written-again true}
   :softland.store/builds-on
   {:why           "a read must find the layers that a layer builds on, in order"
    :value         :layer-list
    :sits-on       :the-layer
    :setting       true
    :written-again true}
   :softland.store/marked-keys
   {:why           "a gate must read for which keys it keeps the facts by value"
    :value         :key-set
    :sits-on       :the-layer
    :setting       true
    :written-again true}})

(def setting-keys
  "The store keys that are settings, read from `store-keys`."
  (into #{} (keep (fn [[k entry]] (when (:setting entry) k))) store-keys))

(def second-write-keys
  "The store keys whose fact can be written again on its place, read from
  `store-keys`: who may see a layer, what it builds on, and its marked
  keys. A gate asks who may write each of them at one place
  (`softland.store.gate.decision/who-may-write-fault`)."
  (into #{} (keep (fn [[k entry]] (when (:written-again entry) k))) store-keys))

(defn store-key?
  "Takes a fact's key. Gives true for one of the store keys. A key that is
  none, and is not in the store's namespace, is a plain key, and a value
  under it must arrive sealed."
  [k]
  (contains? store-keys k))

(defn- in-store-namespace?
  "Takes a fact's key, a keyword. Gives true when its namespace is
  `store-namespace`. The namespace is taken exactly: a key with no
  namespace is not in it, and `softland.store.x` is another namespace.
  Whether the store owns such longer namespaces too is not decided."
  [k]
  (= store-namespace (namespace k)))

;; ------------------------------------------------------- the declared shape

(def shapes
  "The door's map of shapes: each entry of `softland.store.act/shapes`, as
  `act.clj` declares it, and eight more, in the same language.

    :arrived-act   an act as it arrives on a depot: the act-id, the layer's
                   depot under `:depot`, then the ten parts of the maker's
                   `:act`, read from `act.clj`'s map in its order and with
                   its options, with one changed: `:facts` holds arrived
                   facts
    :arrived-fact  a fact as it arrives: the four parts of the maker's
                   `:fact`, read from `act.clj`'s map, and a lock id. Its
                   `:sealed` is not in the shape; `check` sets it aside
    :depot-word    one of the two depot words, `:streaming` and
                   `:microbatch`
    :lock-id       the id of a lock: the act-id of its request, and its
                   index, 0 to 255
    :lock-count    how many locks a request asks for: 1 or more
    :who-may-see-word  one of the two words for who may see a layer,
                   `:members` and `:everyone`
    :layer-list    layer ids in an order. A value is never a list, so what
                   fits is a vector
    :key-set       a set of keys

  The last two say no limit: a rule holds each, with a word of its own.

  The order of the parts is the order of the checks, as in `act.clj`. A
  `:depot` that is absent or no depot word gives
  `:depot-not-the-act-ids-depot`; a lock id that does not fit gives
  `:lock-id-malformed`; each of the maker's parts gives the word that
  `act.clj` declares for it. `:act-id` names no word: a thing whose act-id
  does not fit is a stray and never reaches `check`."
  (let [makers-parts (rest (get act/shapes :act))
        arrived-parts (map (fn [[part :as entry]]
                             (if (= :facts part) (assoc entry 1 [:seq-of :arrived-fact]) entry))
                           makers-parts)]
    (assoc act/shapes
           :arrived-act  (into [:parts
                                [:act-id :act-id]
                                [:depot :depot-word {:reason :depot-not-the-act-ids-depot}]]
                               arrived-parts)
           :arrived-fact (conj (get act/shapes :fact)
                               [:lock-id :lock-id {:optional true :reason :lock-id-malformed}])
           :depot-word   [:enum :streaming :microbatch]
           :lock-id      [:tuple :act-id [:int {:min 0 :below max-locks-in-request}]]
           :lock-count   [:int {:min 1}]
           :who-may-see-word [:enum :members :everyone]
           :layer-list   [:seq-of :id]
           :key-set      [:set-of :key])))

(def rules
  "What a thing that has the shape `:arrived-act` must also keep, with the
  reason for each, in the order in which they are examined. A rule relates
  two places of what arrived, or holds a limit that no shape can say. The
  first is the maker's own, read from `softland.store.act/rules`. Then: the
  act as a whole; a key in the store's namespace; a fact under a store key;
  a fact under a plain key; what holds between the facts; the text of an
  id. `rule-checks` holds the code of each. A rule about a fact is kept when
  each fact of the act keeps it.

  The six rules of the three keys that take a second write stand after the
  rules of a request for locks: the value under each has its shape; a
  builds-on fact names `max-builds-on` layers at most and a marked-keys
  fact holds `max-marked-keys` keys at most; and no marked key is in the
  store's namespace, so the store's own facts are never kept by value. Each
  keeps a fact with no value, as the rules beside them do; a gate refuses
  the retract.

  `:store-key-is-known` holds the store's namespace. A fact whose key is in
  `softland.store` and is not one of `store-keys` is refused
  `:store-key-not-known`, whether its value is sealed, plain or absent. Sid
  decided on 9 October 2026 that a gate refuses such a key
  (src-store/README.md). The rule comes before the rules of a store key and
  of a plain key, so such a fact gets this word and none of theirs. The
  word is a session's."
  (into (vec act/rules)
        [[:depot-is-the-act-ids-depot          :depot-not-the-act-ids-depot]
         [:facts-within-the-limit              :too-many-facts]
         [:based-on-within-the-limit           :too-many-based-on]
         [:store-key-is-known                  :store-key-not-known]
         [:store-key-fact-has-no-seal          :store-key-sealed]
         [:depot-fact-holds-a-depot-word       :depot-fact-not-a-depot-word]
         [:permission-fact-holds-a-permission  :permission-fact-not-a-permission]
         [:permission-fact-sits-on-its-holder  :permission-fact-not-on-its-holder]
         [:lock-request-holds-a-count          :lock-request-not-a-count]
         [:lock-request-within-the-limit       :too-many-locks-requested]
         [:lock-request-sits-on-its-act        :lock-request-not-on-its-act]
         [:who-may-see-fact-holds-its-word     :who-may-see-fact-not-members-or-everyone]
         [:builds-on-fact-holds-layers         :builds-on-fact-not-a-list-of-layers]
         [:builds-on-within-the-limit          :builds-on-too-many-layers]
         [:marked-keys-fact-holds-keys         :marked-keys-fact-not-a-set-of-keys]
         [:marked-keys-within-the-limit        :too-many-marked-keys]
         [:marked-keys-are-plain-keys          :marked-key-in-the-stores-namespace]
         [:plain-value-is-sealed               :not-sealed]
         [:sealed-fact-has-no-plain-value      :sealed-with-a-plain-value]
         [:seal-is-whole                       :seal-incomplete]
         [:sealed-is-bytes                     :sealed-not-bytes]
         [:sealed-within-the-limit             :sealed-value-too-large]
         [:place-named-once                    :place-named-twice]
         [:no-fact-replaces-a-fact-of-its-act  :replaces-own-fact]
         [:lock-named-once                     :lock-named-twice]
         [:ids-within-the-limit                :id-too-long]]))

(def stray-reasons
  "The words of a stray, in the order in which `stray-reason` tests them. A
  stray is a thing that gets no answer; a gate keeps its digest under its
  word. The set is closed: a gate's map of strays has these keys and no
  other."
  [:no-act-id
   :act-id-malformed
   :id-too-long
   :act-id-of-the-other-gate
   :act-id-of-another-layer])

(def reasons
  "The words with which `check` refuses, read from the two declarations: each
  word that a part or a rule names, `act.clj`'s own among them, and the two
  that no part names, :malformed and :unknown-part."
  (into #{:malformed :unknown-part}
        (concat (map second rules)
                (for [shape (vals shapes)
                      :when (= :parts (first shape))
                      [_ _ opts] (rest shape)
                      word (keep #(get opts %) [:reason :absent :empty])]
                  word))))

;; ------------------------------------------------------------ the text of an id

(defn id-text
  "Takes an id or a fact's key: a readable keyword or a UUID. Gives its
  printed text: a keyword with its colon and its namespace, a UUID as its 36
  characters. It holds no space, and a keyword and a UUID never print alike.
  A row's key is made of these texts, and `max-id-text` counts them."
  [id]
  (str id))

(defn id-within-the-limit?
  "Takes an id or a fact's key. Gives true when its printed text has
  `max-id-text` characters at most. A UUID always keeps the limit."
  [id]
  (<= (count (id-text id)) max-id-text))

(defn usable-id?
  "Takes anything. Gives true for an id that a row's key can be made of: a
  readable keyword or a UUID, within the limit. A read tests what a caller
  gave it with this before it builds a key. It never throws."
  [x]
  (boolean (and (or (act/readable-keyword? x) (uuid? x))
                (id-within-the-limit? x))))

(defn act-id-in-layer?
  "Takes a layer id and anything. Gives true when the thing has the shape of
  an act-id, [layer depot uuid], and its layer slot is the layer given. A
  read tests what a caller gave it with this. It never throws."
  [layer x]
  (try (and (= :ok (first (act/as-shape shapes :act-id x)))
            (= layer (nth x 0)))
       (catch Throwable _ false)))

;; ------------------------------------------------------ the code of the rules

(defn- plain-map?
  "True for a map that is no record."
  [x]
  (and (map? x) (not (record? x))))

(defn- each-fact?
  "Takes a test of one fact, and an act in its one form on arrival. Gives
  true when each fact of the act passes the test."
  [fact-ok? act]
  (every? fact-ok? (:facts act)))

(defn- value-fits-its-key?
  "Takes a store key and an act. Gives true when the value of each fact
  under that key, when the fact has one, has the shape that `store-keys`
  names for the key. The engine tests the value. A fact with no value keeps
  the rule; a gate refuses it by its own step."
  [k act]
  (let [shape (get-in store-keys [k :value])]
    (each-fact? (fn [fact]
                  (or (not= k (:k fact))
                      (nil? (:v fact))
                      (= :ok (first (act/as-shape shapes shape (:v fact))))))
                act)))

(defn- count-within?
  "Takes a store key, a limit and an act. Gives true when the value of each
  fact under that key, when the fact has one, holds that many parts at
  most. What must hold: the rule of the value's shape passed, so the value
  is a vector or a set."
  [k most act]
  (each-fact? (fn [fact]
                (or (not= k (:k fact))
                    (nil? (:v fact))
                    (<= (count (:v fact)) most)))
              act))

(defn- permission-ids
  "Takes a permission id in its one form. Gives each id that it holds: who,
  layer and granted-in, of itself and of each permission above it. The
  permission above is the fourth slot of both given forms, with or without
  the slot that says what the permission allows."
  [permission]
  (when (vector? permission)
    (concat (take 3 permission)
            (when (<= 4 (count permission)) (permission-ids (nth permission 3))))))

(defn- ids-of
  "Takes an act in its one form on arrival. Gives each id that it holds,
  wherever it sits, and each fact's key: who, the layer, the session, each
  subject, each fact's entity, each id inside an act-id, a fact id, a
  permission id or a lock id, each layer that a builds-on fact names, and
  each key that a marked-keys fact holds. A key of a row is built from each
  of the last two kinds, so they are held to the limit with the rest. The
  uuid of an act-id is not among them: it prints to 36 characters. What
  must hold: the rules of the store keys' values passed."
  [act]
  (let [layer-of (fn [act-id] (when act-id [(nth act-id 0)]))]
    (concat [(:who act) (:layer act) (:session act)]
            (:subjects act)
            (permission-ids (:permission act))
            (layer-of (:act-id act))
            (layer-of (:because-of act))
            (mapcat (fn [[act-id _]] (layer-of act-id)) (keys (:based-on act)))
            (mapcat (fn [fact]
                      (concat [(:e fact) (:k fact)]
                              (layer-of (first (:replaces fact)))
                              (layer-of (first (:lock-id fact)))
                              (when (= :softland.store/permission (:k fact))
                                (permission-ids (:v fact)))
                              (when (contains? #{:softland.store/builds-on :softland.store/marked-keys}
                                               (:k fact))
                                (:v fact))))
                    (:facts act)))))

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act that has the shape
  `:arrived-act`, in its one form with `:sealed` back on its facts, and
  gives true when the act keeps the rule. Each may rest on the rules before
  it, since `check` stops at the first that is broken.

  The maker's rule is written here a second time, one line: its code is
  private to `act.clj`, which this build changes in one place only. Its name
  and its word are read from `softland.store.act/rules`."
  {:permission-is-in-the-acts-layer
   (fn [act] (= (:layer act) (nth (:permission act) 1)))

   :depot-is-the-act-ids-depot
   (fn [act] (= (:depot act) (nth (:act-id act) 1)))

   :facts-within-the-limit
   (fn [act] (<= (count (:facts act)) max-facts))

   :based-on-within-the-limit
   (fn [act] (<= (count (:based-on act)) max-based-on))

   ;; The store owns its whole namespace: a key in it that is no store key is
   ;; refused, whatever the fact holds.
   :store-key-is-known
   (fn [act] (each-fact? (fn [fact] (or (not (in-store-namespace? (:k fact)))
                                        (store-key? (:k fact))))
                         act))

   :store-key-fact-has-no-seal
   (fn [act] (each-fact? (fn [fact] (or (not (store-key? (:k fact)))
                                        (and (nil? (:sealed fact)) (nil? (:lock-id fact)))))
                         act))

   :depot-fact-holds-a-depot-word
   (fn [act] (value-fits-its-key? :softland.store/depot act))

   :permission-fact-holds-a-permission
   (fn [act] (value-fits-its-key? :softland.store/permission act))

   ;; A permission fact with no value names no holder; a gate refuses it.
   :permission-fact-sits-on-its-holder
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/permission (:k fact))
                                        (nil? (:v fact))
                                        (= (:e fact) (nth (:v fact) 0))))
                         act))

   :lock-request-holds-a-count
   (fn [act] (value-fits-its-key? :softland.store/lock-request act))

   :lock-request-within-the-limit
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/lock-request (:k fact))
                                        (nil? (:v fact))
                                        (<= (:v fact) max-locks-in-request)))
                         act))

   ;; Held for a fact with no value too, so that the place of each fact under
   ;; :softland.store/lock-request is new by its form.
   :lock-request-sits-on-its-act
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/lock-request (:k fact))
                                        (= (:e fact) (nth (:act-id act) 2))))
                         act))

   :who-may-see-fact-holds-its-word
   (fn [act] (value-fits-its-key? :softland.store/who-may-see act))

   :builds-on-fact-holds-layers
   (fn [act] (value-fits-its-key? :softland.store/builds-on act))

   :builds-on-within-the-limit
   (fn [act] (count-within? :softland.store/builds-on max-builds-on act))

   :marked-keys-fact-holds-keys
   (fn [act] (value-fits-its-key? :softland.store/marked-keys act))

   :marked-keys-within-the-limit
   (fn [act] (count-within? :softland.store/marked-keys max-marked-keys act))

   ;; A mark of one of the store's own keys is refused: a permission, a
   ;; request for locks and a setting are never kept in the order by value.
   :marked-keys-are-plain-keys
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/marked-keys (:k fact))
                                        (nil? (:v fact))
                                        (not-any? in-store-namespace? (:v fact))))
                         act))

   :plain-value-is-sealed
   (fn [act] (each-fact? (fn [fact] (or (store-key? (:k fact))
                                        (nil? (:v fact))
                                        (some? (:sealed fact))))
                         act))

   :sealed-fact-has-no-plain-value
   (fn [act] (each-fact? (fn [fact] (not (and (some? (:sealed fact)) (some? (:v fact)))))
                         act))

   :seal-is-whole
   (fn [act] (each-fact? (fn [fact] (= (some? (:sealed fact)) (some? (:lock-id fact))))
                         act))

   :sealed-is-bytes
   (fn [act] (each-fact? (fn [fact] (let [sealed (:sealed fact)]
                                      (or (nil? sealed)
                                          (and (bytes? sealed) (pos? (alength ^bytes sealed))))))
                         act))

   :sealed-within-the-limit
   (fn [act] (each-fact? (fn [fact] (let [sealed (:sealed fact)]
                                      (or (nil? sealed)
                                          (<= (alength ^bytes sealed) max-sealed-bytes))))
                         act))

   :place-named-once
   (fn [act] (apply distinct? (map (juxt :e :k) (:facts act))))

   :no-fact-replaces-a-fact-of-its-act
   (fn [act] (each-fact? (fn [fact] (not= (:act-id act) (first (:replaces fact))))
                         act))

   :lock-named-once
   (fn [act] (let [named (keep :lock-id (:facts act))]
               (or (empty? named) (apply distinct? named))))

   :ids-within-the-limit
   (fn [act] (every? id-within-the-limit? (ids-of act)))})

;; -------------------------------------------------------- the test for a stray

(defn stray-reason
  "The test for a stray. Takes the depot of the gate where a thing arrived,
  :streaming or :microbatch, and the thing, which can be anything. Gives nil
  for a thing that the gate can answer under its act-id, or the word of a
  stray, one of `stray-reasons`. In order:

    :no-act-id                 the thing is no plain map, or holds no act-id
    :act-id-malformed          its act-id is not [layer depot uuid]
    :id-too-long               the layer slot of its act-id prints to more
                               than `max-id-text` characters. An answer's
                               key holds that layer, so such a thing can
                               have no answer
    :act-id-of-the-other-gate  the depot slot is not the gate's depot
    :act-id-of-another-layer   the layer slot is not the thing's `:layer`.
                               This covers a thing with no `:layer`

  A thing that passes names one layer in its act-id and in its `:layer`, of
  `max-id-text` characters at most. On the streaming depot, when a client
  appended it, it is on the task of that layer, which is where a read by
  its act-id looks. On the microbatch depot it is on the task of the entity
  of its first fact, and its answer is written on the task of its act-id's
  uuid, which is where that read looks.

  It never throws: a thing that throws while it is tested gives :no-act-id."
  [gate-depot thing]
  (try
    (let [act-id (when (plain-map? thing) (get thing :act-id))]
      (cond
        (nil? act-id) :no-act-id
        (not= :ok (first (act/as-shape shapes :act-id act-id))) :act-id-malformed
        (not (id-within-the-limit? (nth act-id 0))) :id-too-long
        (not= gate-depot (nth act-id 1)) :act-id-of-the-other-gate
        (not= (nth act-id 0) (get thing :layer)) :act-id-of-another-layer
        :else nil))
    (catch Throwable _ :no-act-id)))

;; ------------------------------------------------------------------ the check

(defn- without-sealed
  "Takes a thing that is a plain map. Gives it with each fact's `:sealed` set
  aside: only when `:facts` is a sequence, and only from a fact that is a
  plain map. Anything else stays as it arrived, so that the engine gives the
  reason, in the declared order, and this step gives none of its own. The
  facts are not read here: the sequence that it gives reads them when the
  engine comes to `:facts`, after the parts declared before it."
  [thing]
  (let [facts (get thing :facts)]
    (if (sequential? facts)
      (assoc thing :facts (map (fn [fact] (if (plain-map? fact) (dissoc fact :sealed) fact)) facts))
      thing)))

(defn- with-sealed
  "Takes the one form that the engine gave for a thing, and the thing. Gives
  the one form with each fact's `:sealed` put back as it arrived, nil for a
  fact that carried none. What must hold: the engine took the thing, so its
  facts are a sequence of plain maps, as many as the one form has."
  [form thing]
  (assoc form :facts (mapv (fn [fact arrived] (assoc fact :sealed (get arrived :sealed)))
                           (:facts form)
                           (get thing :facts))))

(defn- check*
  "The body of `check`, with no guard around it. It can throw; `check` turns
  each throw into a refusal."
  [thing]
  (let [[verdict got] (act/as-shape shapes :arrived-act (without-sealed thing))]
    (if (= :no verdict)
      {:refuse (or got :malformed)}
      (let [act (with-sealed got thing)]
        (if-let [reason (some (fn [[rule reason]] (when-not ((get rule-checks rule) act) reason)) rules)]
          {:refuse reason}
          {:ok act})))))

(defn check
  "The check of what arrived. Takes a thing that is no stray
  (`stray-reason` gave nil for it). Gives {:ok act}, with the act in its one
  form on arrival, or {:refuse reason}, with one of `reasons`. It never
  throws: as in `softland.store.act/parse`, each throw becomes :malformed.

  First the shape: the thing must have the declared shape `:arrived-act`,
  and the first part that is wrong, in the declared order, gives its reason.
  Then the rules, in their order, and the first that the act breaks gives
  its reason. So a fault of the maker's form comes out with `act.clj`'s own
  word.

  The one form on arrival. Each part of `:arrived-act` is there, as the
  engine builds a one form: plain vectors and maps, each fixed-width integer
  a long, nil or the declared default for a part that was left out. Each
  fact holds `:e`, `:k`, `:v`, `:replaces`, `:lock-id` and `:sealed`; the
  `:sealed` is the array that arrived, or nil.

  Limits. It reads no record: the standing of a permission, a replaced fact
  and a lock are the gate's. It reads a thing over a limit once before its
  rule refuses it."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :malformed})))
