(ns softland.store.door.declaration
  "The door's declaration: what arrives on a depot, declared as data, and the
  check of it. Pure code. It needs no cluster.

  What arrives on a depot is the maker's act (`softland.store.act`) with the
  three parts that the door adds: an act-id, the layer's depot, and the time
  of the door's clock (`time-part`). Each value is plain, as its maker gave
  it. Anything can append to a depot without the door, so a gate checks each
  thing that arrives against this declaration, and the gate is the
  authority.

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
  used. The engine reads every part of what arrived.

  The check has three steps: the shape, the rules, and the values. The last
  step makes the one text of each value (`softland.store.door.text`), once,
  and holds two bounds on the bytes of those texts: `max-value-bytes` for
  one value under a plain key, and `max-act-bytes` for the act. It is the
  last because it is the most costly: a thing that the shape or a rule
  refuses has no text made. The text rides in the act's one form, under a
  fact's `:text`, and a fact's row takes it as it is.

  Inputs and results, by call:

    stray-reason  a gate's depot, and anything -> nil, or the word of a stray
    check         a thing that is no stray -> {:ok act} or {:refuse reason}
    store-key?    a fact's key -> true for one of the store keys
    id-text       an id or a fact's key -> its printed text
    id-within-the-limit?  an id or a fact's key -> true or false
    usable-id?    anything -> true for an id within the limit
    act-id-in-layer?  a layer id, and anything -> true for an act-id of it

  The store keys, the limits, the name and the bound of the time on the
  act, and the words of a stray are declared here too, each in one place.
  The store's namespace is read from `softland.store.act`, where what a
  permission allows needs it. The store's keys are in the
  namespace `softland.store`, and the store owns the whole of it: a fact
  whose key is in that namespace and is no store key is refused. Any other
  key is a plain key, a tool's own. `store-keys` is the table of the store
  keys: it says which is a setting, and which takes a second write
  (`second-write-keys`).

  State and effects. None. Each function is pure, and `stray-reason` and
  `check` never throw: a throw in a gate would stop its partition (a finding
  of the rig on Rama).

  Callers. `softland.store.gate.decision`, `softland.store.record.rows` and
  the walk of one thing, which both gates call
  (`softland.store.module.walk`).
  The door's additions (`softland.store.door.additions`), for the tests of
  an id and of an act-id, and for the name of the time's part. The digest
  (`softland.store.door.digest`), for that name: it is the part that a
  digest leaves out. The read path
  (`softland.store.read-path.query` and the namespaces beside it) asks the
  tests and the text of an id, `store-key?` and `max-builds-on`.
  The door sends; it is the gate that checks.

  Not here. Whether a thing's permission stands, and whether a fact that it
  replaces is current: those need the record, and are
  `softland.store.gate.decision`'s. A grammar check is not built."
  (:require [softland.store.act :as act]
            [softland.store.door.text :as text]))

;; ------------------------------------------------------------- the limits

(def max-facts
  "How many facts one act can hold. A number of the build, kept and not
  decided; it bounds the rows that one event writes."
  256)

(def max-based-on
  "How many entries the `:based-on` of one act can hold. A number of the
  build, kept and not decided."
  1024)

(def max-permission-bytes
  "How many bytes the one text of a permission id can have
  (`softland.store.door.text`), counted as UTF-8: of the act's own
  `:permission`, and of the value of each fact under
  `:softland.store/permission`. A number of a session, kept and not
  decided. It stands under Sid's Z5 of 12 October 2026 (src-store/README.md,
  entry 6 of that day): the limit of four on the depth of a permission is
  lifted, \"and a bound on the size of an act takes its place\".

  Why a bound of its own. A permission of depth 32 whose levels each list
  64 key patterns of 256 characters has a text of about 510 KB, and the
  row of each level holds the text of its own permission and of its
  giver's. A check of the standing of an act under such a right reads the
  row of each level, about 16 MB, and every act under it pays that, a
  refused one too. With each text at 8,192 bytes at most, a row of a level
  holds about 16.6 KB, and the 32 rows of a standing about 530 KB. A
  permission of depth 32 whose ids are like UUIDs is about 4,400 bytes, so
  the bound leaves room for a deep chain or a long list of patterns, not
  both at their limits."
  8192)

(def max-value-bytes
  "How many bytes the value of one fact under a plain key can have: the
  UTF-8 bytes of the one text of the value in its normal form, which are
  the bytes that the fact's row holds for the value. A value under a store
  key keeps the limits of its own key. A number of the build, kept and not
  decided; it bounds the bytes of one row."
  65536)

(def max-act-bytes
  "How many bytes of text an accept of one act can write into the rows of
  its facts. Counted for each fact: the UTF-8 bytes of the one text of its
  value, under a plain key or a store key, and none for a fact with no
  value; and the UTF-8 bytes of the one text of the act's `:who` and of its
  permission, which each fact's row holds. So a deep permission with long
  ids is counted once for each fact that is written under it.

  The number is 18 times 2^20, which is `max-facts` times
  (`max-value-bytes` + 8,192): 256 values at the bound of one value, and
  8,192 bytes of who and permission for each of 256 facts. A permission of
  depth 32 whose ids are like UUIDs is about 4,400 bytes with its who, so
  an act that is full under such a permission passes. A permission whose
  levels list key patterns is longer, by the text of each pattern, up to
  `max-permission-bytes`, and an act under it holds fewer facts within the
  bound. A number of the build, kept and not decided.

  Limit of the count: the rows of the facts by value hold who and
  permission once more for each fact under a marked key, and the bound
  does not count those. Which keys are marked is read from the record,
  after this check."
  18874368)

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

(def max-one-write-keys
  "How many keys the fact under `:softland.store/one-write-keys` can hold:
  as many as the marked keys, and for the same reason. A gate reads the row
  of that fact for each act that claims a place under a plain key, so the
  number bounds a row that is read often. A number of a session, kept and
  not decided."
  32)

;; ------------------------------------------------------- the time on the act

(def time-part
  "The name of the part of an arrived thing that holds the time on the act:
  `:door-time`. The door writes the part (`softland.store.door/send-act`):
  the clock of the server that runs the door, in milliseconds since 1970.
  Anything else that appends a thing writes there what it will: a gate
  knows that the part holds a whole number not below zero (`usable-time`),
  and not who wrote it. The time is a label. Each gate puts it beside the
  stamp, in the answer and in each row that an accepted act writes
  (`softland.store.gate.decision/writes`); no rule reads it, and no stamp
  holds it. The digest of a thing leaves it out
  (`softland.store.door.digest`).

  Not decided: kept for now. What the time on the act is called is Sid's to
  say; `:door-time` is a working name, a session's. This def is the one
  place where the name is made: the shape `:arrived-act`, the door's
  additions, the digest and `time-on` read it from here. The reason word
  `:no-door-time` is a word of its own and is not read from here."
  :door-time)

(defn usable-time
  "Takes a thing as it arrived, which can be anything. Gives the time on it,
  what it holds under `time-part`, when that is a time that the shape
  `:arrived-act` takes: a fixed-width integer that is not below zero. Gives
  nil for anything else: a thing that is no plain map, a thing with no
  time, and a time that the shape refuses with `:no-door-time`. A gate
  puts what this gives in the thing's answer
  (`softland.store.gate.decision/time-on`). It gives the time also for a
  thing that `check` refuses for another fault: the time is the thing's
  own whatever else is wrong with it. It never throws: a thing that throws
  when the part is read has no usable time.

  No bound above. The time is a label that no rule reads and no sum takes,
  so a time far ahead of every clock is a wrong label and harms nothing
  else. Order and time are apart, and order never uses the time (Sid's
  decision of 14 October 2026)."
  [thing]
  (let [t (try (when (and (map? thing) (not (record? thing))) (get thing time-part))
               (catch Throwable _ nil))]
    (when (and (int? t) (<= 0 (long t)))
      (long t))))

;; --------------------------------------------------------- the store keys

(def store-namespace
  "The namespace of the store's own keys: `softland.store`. The word is
  Sid's, of 9 October 2026 (src-store/README.md, K4 B). It is made in
  `softland.store.act/store-namespace`, since what a permission allows reads
  it, and is read from there. The store owns the whole of it: a fact whose
  key has this namespace is under one of `store-keys`, or a gate refuses it
  (the rule `:store-key-is-known`). A key with another namespace, and a key
  with none, is a plain key and a tool's own."
  act/store-namespace)

(def store-keys
  "The keys that the store itself reads, each with: why it is a store key;
  the shape that its value must have, as a name of `shapes`; the entity that
  its fact sits on; whether it is a setting; and whether its fact can be
  written again. A fact under one of them keeps its value in the one form on
  arrival, beside the value's text, for the rules and a gate's checks to
  read. A setting is a key whose fact a gate takes only on the layer's own
  entity. A key that is `:written-again` takes a second write on its place,
  and a third; any other store key takes one write for a place while the
  place holds a value (`softland.store.gate.decision`, what a store key does
  not take). A retract under any of them is refused, but under
  `:softland.store/permission`: that retract is the end of a right, which a
  gate takes from an act whose permission is above the right, and a place
  whose right has ended takes a give again.

  Three are of the read build (src-store/README.md, decisions of
  11 and 12 October 2026, entries 1, 2 and 6). Who may see a layer:
  `:members`, whoever holds a right in it that stands; `:signed-in`, anyone
  who is signed in, a session's word of sign-in (15 October 2026) and Sid's
  to change; or `:everyone`, a person who is not signed in too. The read
  path reads it (`softland.store.read-path.layers/standing`); no gate does.
  What a layer builds on: the layers that a read looks through after it, in
  the writer's order; an empty vector says \"nothing\". The keys that a
  layer marks: for each of them a gate keeps every fact in one more order,
  by value; the empty set says \"none\", and a mark is taken off by writing
  the set again without it. What each reads as when no fact says it is
  `softland.store.gate.first-act/starts-with`. The three words of the keys
  are a session's, and Sid's to change.

  The last, `:softland.store/one-write-keys`, is of the sign-in build: the
  plain keys under which the layer takes one write for a place
  (src/proposal/sign-in-2026-10-15/BUILD.md, the store's change d). It
  stops a blind second claim: a fact under such a key that names nothing to
  replace is refused when its place holds a value. It does not stop a fact
  that names the current fact, which replaces it, nor a retract, which
  frees the place, by any writer whose right allows the key: the store
  knows no holder of a claim. So a claim holds once only where one writer,
  such as the system, writes the key. The empty set says \"none\". Its
  word is a session's, and Sid's to change.

  Each is in the namespace `softland.store` (`store-namespace`). Sid decided
  on 9 October 2026 that the store's keys carry a namespace
  (src-store/README.md, K4 B), and gave the words: `softland.store` before
  the slash, and \"depot\" for the kind of a layer, streaming or microbatch.
  So a bare word is no store key. A fact of a tool under `:depot` or
  `:permission` is a fact under a plain key, and the store reads nothing
  from it.

  `:sits-on` is a word for the reader of this table; the rule
  `:permission-fact-sits-on-its-holder` and the gate's check of a setting
  hold what it says.

  A permission fact sits on its holder, so the gate finds the permission of
  an act's `:who` with one read, and a holder has one permission in a layer:
  she cannot hold \"see\" from one giver and \"see and act\" from another.
  That is not decided, and is kept as built; the rule
  `:permission-fact-sits-on-its-holder` and
  `softland.store.gate.decision/permission-place` are where the other answer
  would be made."
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
    :written-again true}
   :softland.store/one-write-keys
   {:why           "a gate must read under which keys a place takes one write"
    :value         :key-set
    :sits-on       :the-layer
    :setting       true
    :written-again true}})

(def setting-keys
  "The store keys that are settings, read from `store-keys`."
  (into #{} (keep (fn [[k entry]] (when (:setting entry) k))) store-keys))

(def second-write-keys
  "The store keys whose fact can be written again on its place, read from
  `store-keys`: who may see a layer, what it builds on, its marked keys,
  and its one-write keys. A gate asks who may write each of them at one
  place (`softland.store.gate.decision/who-may-write-fault`)."
  (into #{} (keep (fn [[k entry]] (when (:written-again entry) k))) store-keys))

(defn store-key?
  "Takes a fact's key. Gives true for one of the store keys. A key that is
  none, and is not in the store's namespace, is a plain key: a tool's own.
  A gate keeps the text of a value under it and reads nothing of the
  value."
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
  `act.clj` declares it, and five more, in the same language.

    :arrived-act   an act as it arrives on a depot: the act-id, the layer's
                   depot under `:depot`, the time on the act under
                   `time-part`, then the ten parts of the maker's `:act`,
                   read from `act.clj`'s map in its order and with its
                   options. Its `:facts` are the maker's own: what arrives
                   as a fact is `act.clj`'s `:fact`
    :depot-word    one of the two depot words, `:streaming` and
                   `:microbatch`
    :who-may-see-word  one of the three words for who may see a layer,
                   `:members`, `:signed-in` and `:everyone`
    :layer-list    layer ids in an order. A value is never a list, so what
                   fits is a vector
    :key-set       a set of keys

  The last two say no limit: a rule holds each, with a word of its own.

  The order of the parts is the order of the checks, as in `act.clj`. A
  `:depot` that is absent or no depot word gives
  `:depot-not-the-act-ids-depot`. A time that is absent, nil, no
  fixed-width integer, or below zero gives `:no-door-time`: one word for
  each of them. Each of the maker's parts
  gives the word that `act.clj` declares for it. A fact that carries a part
  that `:fact` does not name gives `:unknown-part`, by the engine's rule for
  such a part. `:act-id` names no word: a thing whose act-id does not fit is
  a stray and never reaches `check`.

  A record that arrives with no time is refused (Sid's decision of 14
  October 2026), and the refusal is recorded under its act-id: such a
  thing has a usable act-id, so it is no stray and a gate can answer it.
  The entry of `time-part` in `:arrived-act` is the one place where another
  answer would be made."
  (assoc act/shapes
         :arrived-act  (into [:parts
                              [:act-id :act-id]
                              [:depot :depot-word {:reason :depot-not-the-act-ids-depot}]
                              [time-part [:int {:min 0}] {:reason :no-door-time}]]
                             (rest (get act/shapes :act)))
         :depot-word   [:enum :streaming :microbatch]
         :who-may-see-word [:enum :members :signed-in :everyone]
         :layer-list   [:seq-of :id]
         :key-set      [:set-of :key]))

(def rules
  "What a thing that has the shape `:arrived-act` must also keep, with the
  reason for each, in the order in which they are examined. A rule relates
  two places of what arrived, or holds a limit that no shape can say. The
  first is the maker's own, read from `softland.store.act/rules`. Then: the
  act as a whole, the text of its permission and of each permission that it
  gives among it; a key in the store's namespace; a fact under a store key;
  what holds between the facts; the text of an id. `rule-checks` holds the
  code of each. A rule about a fact is kept when each fact of the act keeps
  it. No rule reads the value of a fact under a plain key: the bytes of
  such a value are held by the last step of the check (`with-texts`).

  The nine rules of the four keys that take a second write stand after the
  rules of a permission fact: the value under each has its shape; a
  builds-on fact names `max-builds-on` layers at most, a marked-keys fact
  holds `max-marked-keys` keys at most, and a one-write-keys fact
  `max-one-write-keys`; no marked key is in the store's namespace, so the
  store's own facts are never kept by value; and no one-write key is in it,
  since the store's own keys say for themselves how often a place takes a
  write. Each keeps a fact with no value, as the rules beside them do; a
  gate refuses the retract.

  `:store-key-is-known` holds the store's namespace. A fact whose key is in
  `softland.store` and is not one of `store-keys` is refused
  `:store-key-not-known`, whether it has a value or none. Sid
  decided on 9 October 2026 that a gate refuses such a key
  (src-store/README.md). The rule comes before the rules of a store key, so
  such a fact gets this word and none of theirs. The word is a session's."
  (into (vec act/rules)
        [[:depot-is-the-act-ids-depot          :depot-not-the-act-ids-depot]
         [:facts-within-the-limit              :too-many-facts]
         [:based-on-within-the-limit           :too-many-based-on]
         [:permission-within-the-limit         :permission-too-long]
         [:store-key-is-known                  :store-key-not-known]
         [:depot-fact-holds-a-depot-word       :depot-fact-not-a-depot-word]
         [:permission-fact-holds-a-permission  :permission-fact-not-a-permission]
         [:permission-fact-sits-on-its-holder  :permission-fact-not-on-its-holder]
         [:who-may-see-fact-holds-its-word     :who-may-see-fact-not-a-who-may-see-word]
         [:builds-on-fact-holds-layers         :builds-on-fact-not-a-list-of-layers]
         [:builds-on-within-the-limit          :builds-on-too-many-layers]
         [:marked-keys-fact-holds-keys         :marked-keys-fact-not-a-set-of-keys]
         [:marked-keys-within-the-limit        :too-many-marked-keys]
         [:marked-keys-are-plain-keys          :marked-key-in-the-stores-namespace]
         [:one-write-keys-fact-holds-keys      :one-write-keys-fact-not-a-set-of-keys]
         [:one-write-keys-within-the-limit     :too-many-one-write-keys]
         [:one-write-keys-are-plain-keys       :one-write-key-in-the-stores-namespace]
         [:place-named-once                    :place-named-twice]
         [:no-fact-replaces-a-fact-of-its-act  :replaces-own-fact]
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
  "The words with which `check` refuses. Read from the two declarations:
  each word that a part or a rule names, `act.clj`'s own among them, and
  `:no-door-time`, the word of the time's part. And
  four by name: the two that no part names, :malformed and :unknown-part,
  and the two of the last step of the check, :value-too-large and
  :act-too-large (`with-texts`)."
  (into #{:malformed :unknown-part :value-too-large :act-too-large}
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
  "Takes a test of one fact, and an act in the one form that the engine
  gave. Gives true when each fact of the act passes the test."
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

(defn- permission-within-the-limit?
  "Takes a permission id, or any value under `:softland.store/permission`.
  Gives true when the UTF-8 bytes of its one text are `max-permission-bytes`
  at most. Cost: the making of that text once."
  [permission]
  (<= (long (text/utf8-count (text/text permission))) max-permission-bytes))

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
  layer and granted-in, of itself and of each permission above it; and each
  key pattern of a level whose fifth slot is a set of them, so that a
  pattern is held to the limit of an id's text as a fact's key is. The
  permission above is the fourth slot of both given forms, with or without
  the slot that says what the permission allows."
  [permission]
  (when (vector? permission)
    (concat (take 3 permission)
            (let [allows (nth permission 4 nil)]
              (when (set? allows) allows))
            (when (<= 4 (count permission)) (permission-ids (nth permission 3))))))

(defn- ids-of
  "Takes an act in the one form that the engine gave. Gives each id that it
  holds, wherever it sits, and each fact's key: who, the layer, the
  session, each subject, each fact's entity, each id inside an act-id, a
  fact id or a permission id, each key pattern that a permission lists,
  each layer that a builds-on fact names, and each key that a marked-keys
  or a one-write-keys fact holds. A key of a row is built from a layer of a
  list and from a marked key, so they are held to the limit with the rest;
  a pattern and a one-write key are held to it as the keys that they name
  are. The uuid of an act-id is not among them: it prints to 36 characters.
  What must hold: the rules of the store keys' values passed."
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
                              (when (= :softland.store/permission (:k fact))
                                (permission-ids (:v fact)))
                              (when (contains? #{:softland.store/builds-on :softland.store/marked-keys
                                                 :softland.store/one-write-keys}
                                               (:k fact))
                                (:v fact))))
                    (:facts act)))))

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act that has the shape
  `:arrived-act`, in the one form that the engine gave, and gives true when
  the act keeps the rule. Each may rest on the rules before
  it, since `check` stops at the first that is broken.

  The maker's rule is written here a second time, one line: its code is
  private to `act.clj`. Its name and its word are read from
  `softland.store.act/rules`."
  {:permission-is-in-the-acts-layer
   (fn [act] (= (:layer act) (nth (:permission act) 1)))

   :depot-is-the-act-ids-depot
   (fn [act] (= (:depot act) (nth (:act-id act) 1)))

   :facts-within-the-limit
   (fn [act] (<= (count (:facts act)) max-facts))

   :based-on-within-the-limit
   (fn [act] (<= (count (:based-on act)) max-based-on))

   ;; The one text of the act's own permission, and of each permission that
   ;; a fact gives, holds `max-permission-bytes` at most. The text is made
   ;; here, and again by the last step of the check for an act that passes.
   :permission-within-the-limit
   (fn [act] (and (permission-within-the-limit? (:permission act))
                  (each-fact? (fn [fact] (or (not= :softland.store/permission (:k fact))
                                             (nil? (:v fact))
                                             (permission-within-the-limit? (:v fact))))
                              act)))

   ;; The store owns its whole namespace: a key in it that is no store key is
   ;; refused, whatever the fact holds.
   :store-key-is-known
   (fn [act] (each-fact? (fn [fact] (or (not (in-store-namespace? (:k fact)))
                                        (store-key? (:k fact))))
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

   ;; A mark of one of the store's own keys is refused: a permission and a
   ;; setting are never kept in the order by value.
   :marked-keys-are-plain-keys
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/marked-keys (:k fact))
                                        (nil? (:v fact))
                                        (not-any? in-store-namespace? (:v fact))))
                         act))

   :one-write-keys-fact-holds-keys
   (fn [act] (value-fits-its-key? :softland.store/one-write-keys act))

   :one-write-keys-within-the-limit
   (fn [act] (count-within? :softland.store/one-write-keys max-one-write-keys act))

   ;; A store key is never one of a layer's one-write keys: how often the
   ;; place of a store key takes a write is the store's own rule.
   :one-write-keys-are-plain-keys
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/one-write-keys (:k fact))
                                        (nil? (:v fact))
                                        (not-any? in-store-namespace? (:v fact))))
                         act))

   :place-named-once
   (fn [act] (apply distinct? (map (juxt :e :k) (:facts act))))

   :no-fact-replaces-a-fact-of-its-act
   (fn [act] (each-fact? (fn [fact] (not= (:act-id act) (first (:replaces fact))))
                         act))

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
  `max-id-text` characters at most. On either depot, when a client
  appended it, it is on the task of that layer, which is where its gate
  writes its answer and where a read by its act-id looks.

  It reads no time. A thing with a usable act-id and no usable time is no
  stray: `check` refuses it, and the refusal is recorded under its act-id.

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

(defn- with-texts
  "The last step of the check: the values. Takes an act in the one form
  that the engine gave, which kept each rule. Gives {:ok act}, the act in
  its one form on arrival, or {:refuse :value-too-large}, or {:refuse
  :act-too-large}.

  For each fact that has a value, in the act's order, it makes the one text
  of the value, `softland.store.door.text/text` of the normal form that the
  engine gave, and counts the UTF-8 bytes of that text. A fact under a
  plain key whose count is over `max-value-bytes` gives `:value-too-large`,
  and the step ends there: no text of a later value is made. The text goes
  on the fact under `:text`, and the fact's row takes that String as it is.
  Under a plain key the fact's `:v` becomes nil: a gate reads nothing of a
  tool's value. Under a store key `:v` stays beside the text, for a gate's
  checks. A fact with no value holds nil for both.

  Then the bound on the act. The sum is the bytes of each value's text,
  and, for each fact, the bytes of the one text of the act's `:who` and of
  its permission (`max-act-bytes` says why those). A sum over
  `max-act-bytes` gives `:act-too-large`. So of an act with both faults the
  word is `:value-too-large`.

  Cost: the text of each value is made once, and of the act's who and of
  its permission once. The count of a text makes no array
  (`softland.store.door.text/utf8-count`). What must hold: the act is the
  engine's one form, so each value is a value of the form in its normal
  form."
  [act]
  (let [texted (reduce (fn [[facts total] fact]
                         (let [v (:v fact)]
                           (if (nil? v)
                             [(conj facts (assoc fact :text nil)) total]
                             (let [^String value-text (text/text v)
                                   size (long (text/utf8-count value-text))
                                   plain? (not (store-key? (:k fact)))]
                               (if (and plain? (< max-value-bytes size))
                                 (reduced :value-too-large)
                                 [(conj facts (cond-> (assoc fact :text value-text)
                                                plain? (assoc :v nil)))
                                  (+ total size)])))))
                       [[] 0]
                       (:facts act))]
    (if (= :value-too-large texted)
      {:refuse :value-too-large}
      (let [[facts values-total] texted
            for-each-fact (+ (long (text/utf8-count (text/text (:who act))))
                             (long (text/utf8-count (text/text (:permission act)))))
            total (+ (long values-total) (* (count facts) for-each-fact))]
        (if (< max-act-bytes total)
          {:refuse :act-too-large}
          {:ok (assoc act :facts facts)})))))

(defn- check*
  "The body of `check`, with no guard around it: the shape, then the rules,
  then the values (`with-texts`). It can throw; `check` turns each throw
  into a refusal."
  [thing]
  (let [[verdict got] (act/as-shape shapes :arrived-act thing)]
    (if (= :no verdict)
      {:refuse (or got :malformed)}
      (if-let [reason (some (fn [[rule reason]] (when-not ((get rule-checks rule) got) reason)) rules)]
        {:refuse reason}
        (with-texts got)))))

(defn check
  "The check of what arrived. Takes a thing that is no stray
  (`stray-reason` gave nil for it). Gives {:ok act}, with the act in its one
  form on arrival, or {:refuse reason}, with one of `reasons`. It never
  throws: as in `softland.store.act/parse`, each throw becomes :malformed.

  First the shape: the thing must have the declared shape `:arrived-act`,
  and the first part that is wrong, in the declared order, gives its reason.
  Then the rules, in their order, and the first that the act breaks gives
  its reason. So a fault of the maker's form comes out with `act.clj`'s own
  word. Then the values: the text of each value is made and counted, and a
  value or an act over its bound gives `:value-too-large` or
  `:act-too-large` (`with-texts`). So of a thing with a fault that a rule
  names and a value over the bound, the rule's word is the one given.

  The one form on arrival. Each part of `:arrived-act` is there, as the
  engine builds a one form: plain vectors and maps, each fixed-width integer
  a long, nil or the declared default for a part that was left out. The
  time on the act is there under `time-part`, as it arrived. Each
  fact holds `:e`, `:k`, `:v`, `:replaces` and `:text`. `:text` is the one
  text of the fact's value in its normal form, a String: what the fact's
  row holds. `:v` is the value in its normal form for a fact under a store
  key, and nil for a fact under a plain key. A fact with no value holds nil
  for both. So a fact has a value exactly when its `:text` is not nil.

  Limits. It reads no record: the standing of a permission and a replaced
  fact are the gate's. It reads a thing over a limit once before its rule
  refuses it, and it makes the text of a value over the bound before it
  refuses the value."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :malformed})))
