(ns softland.store.door.declaration
  "The door's declaration: what arrives on a depot, declared as data in
  `shapes` and `rules`, and `check`, the check of it. Pure code.

  Anything can append to a depot without the door, so a gate checks each
  thing that arrives against this declaration, and the gate is the
  authority. `stray-reason` and `check` never throw: a throw in a gate would
  stop its partition (a finding of the rig on Rama).

  The maker's shapes are `act.clj`'s own, read from its map and not written
  a second time, and the engine is `softland.store.act/as-shape`. The order
  of the declarations is the order of the reasons.

  The check has three steps: the shape, the rules, and the values. The last
  makes the one text of each value, once, and holds the two bounds on the
  bytes of those texts. It is the last because it is the most costly: a
  thing that the shape or a rule refuses has no text made.

  It reads no record: whether a permission stands, and whether a replaced
  fact is current, are `softland.store.gate.decision`'s. A grammar check is
  not built."
  (:require [softland.store.act :as act]
            [softland.store.door.text :as text]))

;; ------------------------------------------------------------- the limits

(def max-facts
  "It bounds the rows that one event writes."
  256)

(def max-based-on
  1024)

(def max-value-bytes
  "For one fact under a plain key: the UTF-8 bytes of the one text of the
  value in its normal form, which are the bytes that the fact's row holds
  for the value. A value under a store key keeps the limits of its own key."
  65536)

(def max-act-bytes
  "How many bytes of text an accept of one act can write into the rows of
  its facts. Counted for each fact: the UTF-8 bytes of the one text of its
  value, under a plain key or a store key; and the UTF-8 bytes of the one
  text of the act's `:who` and of its permission, which each fact's row
  holds. So a deep permission with long ids is counted once for each fact
  that is written under it.

  The number is 18 times 2^20, which is `max-facts` times
  (`max-value-bytes` + 8,192): 256 values at the bound of one value, and
  8,192 bytes of who and permission for each of 256 facts. A permission of
  depth 32 whose ids are like UUIDs is about 4,400 bytes with its who, so
  an act that is full under such a permission passes.

  Not counted: the rows of the facts by value hold who and permission once
  more for each fact under a marked key, and which keys are marked is read
  from the record, after this check. Nor the use of a yes for one act, the
  one row more that an accept under a yes writes, which holds who and the
  yes once more (`softland.store.gate.decision`, `use-of-its-yes`)."
  18874368)

(def max-id-text
  "How many characters the printed text of an id, or of a fact's key, can
  have. A readable keyword has no greatest length in the form, and a row's
  key is made of two or three of these texts; so without this limit a key
  had none."
  256)

(def max-builds-on
  "With each id at its limit the row of the fact under
  `:softland.store/builds-on` is about 16.5 KB."
  64)

(def max-marked-keys
  "A gate reads the row of the fact under `:softland.store/marked-keys` for
  each act with a fact under a plain key, so the number bounds a row that
  is read often: about 8.3 KB of keys and 9 KB of stamps at the limits."
  32)

;; ------------------------------------------------------- the time on the act

(def time-part
  "The part of an arrived thing that holds the time on the act. This def is
  the one place where the name is made; the reason word `:no-door-time` is
  a word of its own and is not read from here.

  The door writes the clock of its server there, in milliseconds since
  1970. Anything else that appends a thing writes there what it will: a
  gate knows that the part holds a whole number not below zero, and not who
  wrote it. The time is a label: no rule reads it, no stamp holds it, and
  the digest of a thing leaves it out."
  :door-time)

(defn usable-time
  "The time on a thing as it arrived, when it is one that the shape
  `:arrived-act` takes; nil for anything else. It gives the time also for a
  thing that `check` refuses for another fault: the time is the thing's own
  whatever else is wrong with it. It never throws.

  No bound above. The time is a label that no rule reads and no sum takes,
  so a time far ahead of every clock is a wrong label and harms nothing
  else. Order never uses the time."
  [thing]
  (let [t (try (when (and (map? thing) (not (record? thing))) (get thing time-part))
               (catch Throwable _ nil))]
    (when (and (int? t) (<= 0 (long t)))
      (long t))))

(declare usable-id?)

(defn usable-who
  "The who that a thing names, when it is an id that a row's key can be made
  of; nil for anything else. It gives the who also for a thing that `check`
  refuses for another fault. No gate holds it against a sender: it is what
  the sending program wrote. A gate keeps it in the lane's line
  (`softland.store.gate.decision/line-writes`), so that a read gives a
  thing whole to the who that it names. It never throws."
  [thing]
  (let [w (try (when (and (map? thing) (not (record? thing))) (get thing :who))
               (catch Throwable _ nil))]
    (when (usable-id? w) w)))

;; --------------------------------------------------------- the store keys

(def store-namespace
  "The store owns the whole of it: a fact whose key has this namespace is
  under one of `store-keys`, or a gate refuses it (the rule
  `:store-key-is-known`). A key with another namespace, and a key with
  none, is a plain key and a tool's own."
  "softland.store")

(def store-keys
  "The keys that the store itself reads. `:value` is a name of `shapes`.
  `:sits-on` is a word for the reader of this table; the rules of a
  permission fact and the gate's check of a setting hold what it says.

  A fact under a store key keeps its value in the one form on arrival,
  beside the value's text, for the rules and a gate's checks to read. A
  setting is a key whose fact a gate takes only on the lane's own entity.
  A key that is `:written-again` takes a second write on its place, and a
  third, by the lane's first holder alone
  (`softland.store.gate.decision/who-may-write-fault`). A lane's depot
  takes one write. A permission's place takes a give when it is empty, when
  it holds an end, and when the permission that it holds no longer stands;
  a fact with no value there is the end of the permission that the place
  holds (`softland.store.gate.decision`, `end-fault` and
  `give-over-fault`). A retract under any of the other five is refused.

  What the settings say. Who may see a lane: `:members`, which is whoever
  holds a permission in it, or `:everyone`. What a lane builds on: the
  lanes that a read looks through after it, in the writer's order; an empty
  vector says \"nothing\". The keys that a lane marks: for each of them a
  gate keeps every fact in one more order, by value; the empty set says
  \"none\", and a mark is taken off by writing the set again without it.
  Who may read the lane's acts whole: `:members`, its holders and the who
  that a thing names, or `:everyone`, whoever may see the lane
  (`softland.store.read-path.acts`). What each reads as when no fact says
  it is `softland.store.gate.first-act/starts-with`.

  A bare word is no store key. A fact of a tool under `:depot` or
  `:permission` is a fact under a plain key, and the store reads nothing
  from it.

  A permission sits on what it covers, under a key of its own for each kind
  (`softland.store.act/covers`): one that covers the lane on its holder,
  one for a run on the run's session, a yes for one act on the yes's own
  id, the uuid that its giver made. Three keys and not one: a holder, a
  session and a yes are each named by an id of one kind, so under one key a
  give for a run whose session has the id of a holder would land on that
  holder's place. So a holder can have, beside her permission for the
  lane, one for each of any number of runs, and any number of yeses for
  acts. The gate finds the permission that an act names with one read, on
  the place that the permission's own form names
  (`softland.store.gate.chain/place`). A yes's place takes one give
  (`softland.store.gate.decision`, `store-key-fault`).

  A holder has one permission for the lane at a time: she cannot hold
  \"see\" from one giver and \"see and act\" from another."
  {:softland.store/depot
   {:why           "a gate must read which gate a lane belongs to"
    :value         :depot-word
    :sits-on       :the-lane
    :setting       true
    :written-again false}
   :softland.store/permission
   {:why           "a gate must read whether an act's permission stands"
    :value         :permission-id
    :sits-on       :the-holder-that-the-permission-names
    :setting       false
    :written-again false}
   :softland.store/run-permission
   {:why           "a gate must read whether the permission of an act's run stands"
    :value         :permission-id
    :sits-on       :the-run-that-the-permission-covers
    :setting       false
    :written-again false}
   :softland.store/act-permission
   {:why           "a gate must read whether a yes for one act stands and is not used up"
    :value         :permission-id
    :sits-on       :the-yes-own-id
    :setting       false
    :written-again false}
   :softland.store/who-may-see
   {:why           "a read must find who may see a lane"
    :value         :who-may-see-word
    :sits-on       :the-lane
    :setting       true
    :written-again true}
   :softland.store/who-may-read-acts
   {:why           "a read must find who may read a lane's acts whole"
    :value         :who-may-read-acts-word
    :sits-on       :the-lane
    :setting       true
    :written-again true}
   :softland.store/builds-on
   {:why           "a read must find the lanes that a lane builds on, in order"
    :value         :lane-list
    :sits-on       :the-lane
    :setting       true
    :written-again true}
   :softland.store/marked-keys
   {:why           "a gate must read for which keys it keeps the facts by value"
    :value         :key-set
    :sits-on       :the-lane
    :setting       true
    :written-again true}})

(def permission-keys
  "One for each thing that a permission covers: the lane, one run, one act."
  #{:softland.store/permission :softland.store/run-permission :softland.store/act-permission})

(def setting-keys
  (into #{} (keep (fn [[k entry]] (when (:setting entry) k))) store-keys))

(def second-write-keys
  (into #{} (keep (fn [[k entry]] (when (:written-again entry) k))) store-keys))

(defn store-key?
  [k]
  (contains? store-keys k))

(defn- in-store-namespace?
  "The namespace is taken exactly: a key with no namespace is not in it, and
  `softland.store.x` is another namespace."
  [k]
  (= store-namespace (namespace k)))

;; ------------------------------------------------------- the declared shape

(def shapes
  "The door's map of shapes: each entry of `softland.store.act/shapes`, and
  six more in the same language. `:arrived-act` is the three parts that the
  door adds and then the ten parts of the maker's `:act`, read from
  `act.clj`'s map in its order and with its options. `:lane-list` and
  `:key-set` say no limit: a rule holds each, with a word of its own. A
  value is never a list, so what fits `:lane-list` is a vector.

  The order of the parts is the order of the checks. A time that is absent,
  nil, no fixed-width integer, or below zero gives `:no-door-time`: one word
  for each of them. `:act-id` names no word: a thing whose act-id does not
  fit is a stray and never reaches `check`.

  A record that arrives with no time is refused, and the refusal is
  recorded under its act-id: such a thing has a usable act-id, so it is no
  stray and a gate can answer it."
  (assoc act/shapes
         :arrived-act  (into [:parts
                              [:act-id :act-id]
                              [:depot :depot-word {:reason :depot-not-the-act-ids-depot}]
                              [time-part [:int {:min 0}] {:reason :no-door-time}]]
                             (rest (get act/shapes :act)))
         :depot-word   [:enum :streaming :microbatch]
         :who-may-see-word [:enum :members :everyone]
         :who-may-read-acts-word [:enum :members :everyone]
         :lane-list   [:seq-of :id]
         :key-set      [:set-of :key]))

(def rules
  "What a thing that has the shape `:arrived-act` must also keep, each with
  its reason, in the order of the checks. The first is the maker's own,
  read from `softland.store.act/rules`. `rule-checks` holds the code. No
  rule reads the value of a fact under a plain key: the last step of the
  check holds the bytes of such a value (`with-texts`).

  No marked key is in the store's namespace, so the store's own facts are
  never kept by value. Each rule of a store key keeps a fact with no value;
  a gate refuses the retract.

  The rule of who may read a lane's acts stands right behind the rule of
  who may see it. A thing that a gate refused `:store-key-not-known` for a
  fact under that key, while the key was no store key, and that has another
  fault that a later rule names, gets the later rule's word here
  (src/proposal/runtime-2026-11-21/store-piece-3/PLAN_VALIDATION-gates-keep-the-line.md,
  item d, traces it). A thing that an earlier rule or the shape refuses
  keeps its word.

  `:store-key-is-known` comes before the rules of a store key. So a fact
  whose key is in `softland.store` and is not one of `store-keys` gets
  `:store-key-not-known` and none of their words, whether it has a value or
  none."
  (into (vec act/rules)
        [[:depot-is-the-act-ids-depot          :depot-not-the-act-ids-depot]
         [:facts-within-the-limit              :too-many-facts]
         [:based-on-within-the-limit           :too-many-based-on]
         [:store-key-is-known                  :store-key-not-known]
         [:depot-fact-holds-a-depot-word       :depot-fact-not-a-depot-word]
         [:permission-fact-holds-a-permission  :permission-fact-not-a-permission]
         [:permission-fact-is-of-its-key       :permission-fact-under-another-key]
         [:permission-fact-sits-on-its-holder  :permission-fact-not-on-its-holder]
         [:permission-fact-sits-on-what-it-covers :permission-fact-not-on-what-it-covers]
         [:who-may-see-fact-holds-its-word     :who-may-see-fact-not-members-or-everyone]
         [:who-may-read-acts-fact-holds-its-word :who-may-read-acts-fact-not-members-or-everyone]
         [:builds-on-fact-holds-lanes         :builds-on-fact-not-a-list-of-lanes]
         [:builds-on-within-the-limit          :builds-on-too-many-lanes]
         [:marked-keys-fact-holds-keys         :marked-keys-fact-not-a-set-of-keys]
         [:marked-keys-within-the-limit        :too-many-marked-keys]
         [:marked-keys-are-plain-keys          :marked-key-in-the-stores-namespace]
         [:place-named-once                    :place-named-twice]
         [:no-fact-replaces-a-fact-of-its-act  :replaces-own-fact]
         [:ids-within-the-limit                :id-too-long]]))

(def stray-reasons
  "In the order in which `stray-reason` tests them. A stray is a thing that
  gets no answer; a gate keeps its digest under its word. The set is
  closed: a gate's map of strays has these keys and no other."
  [:no-act-id
   :act-id-malformed
   :id-too-long
   :act-id-of-the-other-gate
   :act-id-of-another-lane])

(def reasons
  "Every word with which `check` refuses."
  (into #{:malformed :unknown-part :value-too-large :act-too-large}
        (concat (map second rules)
                (for [shape (vals shapes)
                      :when (= :parts (first shape))
                      [_ _ opts] (rest shape)
                      word (keep #(get opts %) [:reason :absent :empty])]
                  word))))

;; ------------------------------------------------------------ the text of an id

(defn id-text
  "The printed text of an id or of a fact's key. It holds no space, and a
  keyword and a UUID never print alike. A row's key is made of these texts,
  and `max-id-text` counts them."
  [id]
  (str id))

(defn id-within-the-limit?
  [id]
  (<= (count (id-text id)) max-id-text))

(defn usable-id?
  "True for an id that a row's key can be made of. It never throws."
  [x]
  (boolean (and (or (act/readable-keyword? x) (uuid? x))
                (id-within-the-limit? x))))

(defn act-id-in-lane?
  "It never throws, whatever x is."
  [lane x]
  (try (and (= :ok (first (act/as-shape shapes :act-id x)))
            (= lane (nth x 0)))
       (catch Throwable _ false)))

;; ------------------------------------------------------ the code of the rules

(defn- plain-map?
  [x]
  (and (map? x) (not (record? x))))

(defn- each-fact?
  [fact-ok? act]
  (every? fact-ok? (:facts act)))

(defn- value-fits-its-key?
  "A fact with no value keeps the rule; a gate refuses it by its own step."
  [k act]
  (let [shape (get-in store-keys [k :value])]
    (each-fact? (fn [fact]
                  (or (not= k (:k fact))
                      (nil? (:v fact))
                      (= :ok (first (act/as-shape shapes shape (:v fact))))))
                act)))

(defn- count-within?
  "What must hold: the rule of the value's shape passed, so the value is a
  vector or a set."
  [k most act]
  (each-fact? (fn [fact]
                (or (not= k (:k fact))
                    (nil? (:v fact))
                    (<= (count (:v fact)) most)))
              act))

(defn- permission-ids
  "Each id that a permission id holds: who, lane and granted-in, and the
  session of a permission for a run, of itself and of each permission above
  it."
  [permission]
  (when (vector? permission)
    (concat (take 3 permission)
            (when (= :run (first (act/covers permission))) [(second (act/covers permission))])
            (when (<= 4 (count permission)) (permission-ids (nth permission 3))))))

(defn- ids-of
  "Each id that an act holds, wherever it sits, and each fact's key. A key
  of a row is built from each lane that a builds-on fact names and each key
  that a marked-keys fact holds, so they are held to the limit with the
  rest. The uuid of an act-id is not among them: it prints to 36
  characters. What must hold: the rules of the store keys' values passed."
  [act]
  (let [lane-of (fn [act-id] (when act-id [(nth act-id 0)]))]
    (concat [(:who act) (:lane act) (:session act)]
            (:subjects act)
            (permission-ids (:permission act))
            (lane-of (:act-id act))
            (lane-of (:because-of act))
            (mapcat (fn [[act-id _]] (lane-of act-id)) (keys (:based-on act)))
            (mapcat (fn [fact]
                      (concat [(:e fact) (:k fact)]
                              (lane-of (first (:replaces fact)))
                              (when (contains? permission-keys (:k fact))
                                (permission-ids (:v fact)))
                              (when (contains? #{:softland.store/builds-on :softland.store/marked-keys}
                                               (:k fact))
                                (:v fact))))
                    (:facts act)))))

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes an act that has the shape
  `:arrived-act`, in the one form that the engine gave, and may rest on the
  rules before it: `check` stops at the first that is broken.

  The maker's rule is written here a second time, one line: its code is
  private to `act.clj`."
  {:permission-is-in-the-acts-lane
   (fn [act] (= (:lane act) (nth (:permission act) 1)))

   :depot-is-the-act-ids-depot
   (fn [act] (= (:depot act) (nth (:act-id act) 1)))

   :facts-within-the-limit
   (fn [act] (<= (count (:facts act)) max-facts))

   :based-on-within-the-limit
   (fn [act] (<= (count (:based-on act)) max-based-on))

   :store-key-is-known
   (fn [act] (each-fact? (fn [fact] (or (not (in-store-namespace? (:k fact)))
                                        (store-key? (:k fact))))
                         act))

   :depot-fact-holds-a-depot-word
   (fn [act] (value-fits-its-key? :softland.store/depot act))

   :permission-fact-holds-a-permission
   (fn [act] (every? (fn [k] (value-fits-its-key? k act)) permission-keys))

   :permission-fact-is-of-its-key
   (fn [act] (each-fact? (fn [fact]
                           (or (not (contains? permission-keys (:k fact)))
                               (nil? (:v fact))
                               (= (:k fact)
                                  (case (first (act/covers (:v fact)))
                                    :run :softland.store/run-permission
                                    :act :softland.store/act-permission
                                    :softland.store/permission))))
                         act))

   ;; A permission fact with no value names no holder: it is an end, and a
   ;; gate holds it to the permission at its place.
   :permission-fact-sits-on-its-holder
   (fn [act] (each-fact? (fn [fact] (or (not= :softland.store/permission (:k fact))
                                        (nil? (:v fact))
                                        (= (:e fact) (nth (:v fact) 0))))
                         act))

   ;; A permission for a run sits on the run's session, and a yes for one
   ;; act on its own id.
   :permission-fact-sits-on-what-it-covers
   (fn [act] (each-fact? (fn [fact]
                           (or (not (contains? #{:softland.store/run-permission :softland.store/act-permission}
                                               (:k fact)))
                               (nil? (:v fact))
                               (= (:e fact) (second (act/covers (:v fact))))))
                         act))

   :who-may-see-fact-holds-its-word
   (fn [act] (value-fits-its-key? :softland.store/who-may-see act))

   :who-may-read-acts-fact-holds-its-word
   (fn [act] (value-fits-its-key? :softland.store/who-may-read-acts act))

   :builds-on-fact-holds-lanes
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

   :place-named-once
   (fn [act] (apply distinct? (map (juxt :e :k) (:facts act))))

   :no-fact-replaces-a-fact-of-its-act
   (fn [act] (each-fact? (fn [fact] (not= (:act-id act) (first (:replaces fact))))
                         act))

   :ids-within-the-limit
   (fn [act] (every? id-within-the-limit? (ids-of act)))})

;; -------------------------------------------------------- the test for a stray

(defn stray-reason
  "nil for a thing that the gate can answer under its act-id, or the word of
  a stray. `:id-too-long`: an answer's key holds the lane of the act-id, so
  such a thing can have no answer. `:act-id-of-another-lane` covers a thing
  with no `:lane`.

  A thing that passes names one lane in its act-id and in its `:lane`. On
  either depot, when a client appended it, it is on the task of that lane,
  which is where its gate writes its answer and where a read by its act-id
  looks.

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
        (not= (nth act-id 0) (get thing :lane)) :act-id-of-another-lane
        :else nil))
    (catch Throwable _ :no-act-id)))

;; ------------------------------------------------------------------ the check

(defn- with-texts
  "The last step of the check: the values. What must hold: the act is the
  engine's one form and kept each rule, so each value is in its normal
  form.

  Under a plain key the fact's `:v` becomes nil: a gate reads nothing of a
  tool's value. Under a store key `:v` stays beside the text, for a gate's
  checks.

  The step ends at the first value over `max-value-bytes`: no text of a
  later value is made. So of an act with both faults the word is
  `:value-too-large`. `max-act-bytes` says what the sum counts.

  The text of each value is made once, and of the act's who and of its
  permission once. The count of a text makes no array."
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
  [thing]
  (let [[verdict got] (act/as-shape shapes :arrived-act thing)]
    (if (= :no verdict)
      {:refuse (or got :malformed)}
      (if-let [reason (some (fn [[rule reason]] (when-not ((get rule-checks rule) got) reason)) rules)]
        {:refuse reason}
        (with-texts got)))))

(defn check
  "The check of what arrived: {:ok act}, with the act in its one form on
  arrival, or {:refuse reason}, with one of `reasons`. What must hold: the
  thing is no stray (`stray-reason` gave nil for it). It never throws: each
  throw becomes :malformed.

  The shape, then the rules, then the values. So a fault of the maker's
  form comes out with `act.clj`'s own word, and of a thing with a fault that
  a rule names and a value over the bound, the rule's word is the one
  given.

  The one form on arrival. Each fact holds `:e`, `:k`, `:v`, `:replaces`
  and `:text`. `:text` is the one text of the fact's value in its normal
  form, a String: the fact's row takes it as it is. `:v` is the value for a
  fact under a store key, and nil for a fact under a plain key. A fact with
  no value holds nil for both. So a fact has a value exactly when its
  `:text` is not nil.

  Limits. It reads no record. It reads a thing over a limit once before its
  rule refuses it, and it makes the text of a value over the bound before
  it refuses the value."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :malformed})))
