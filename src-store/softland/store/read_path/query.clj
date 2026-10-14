(ns softland.store.read-path.query
  "A query written as data: its one form, declared, and the check of that
  form; and the read path's numbers. Pure code. It needs no cluster.

  A tool hands the read path a query: a plain map of a few words. Nobody
  decides what a tool asks; what is decided is the few words that a query is
  made of (src-store/README.md, decisions of 11 and 12 October 2026, entry
  6). The form of the map, and each word of it, is the builder's and is
  Sid's to change.

    {:asks             [:place entity key]      a place
                     | [:entity entity]         an entity: every place of it
                     | [:run first-end last-end] any run of neighbouring places
                     | [:points-at key thing]   what points at a thing
     :asker            id                       who asks: exactly one of
     :not-signed-in    true                       these two
     :layers           [layer ...]              through which layers, nearest
     :from-layer       layer                      first: exactly one of these
                                                  two. The second takes the
                                                  list from the layer
     :with             #{:writer :permission    which parts of a row she asks
                         :time}                   for; left out: none
     :stores-own-facts true}                    left out: the store's own
                                                  facts are not given

  Once or live is no part of the data: it is the call that the caller makes
  (`softland.store.read-path/read-once`, `read-live`), so one query can be
  asked both ways and two askers of one thing hold equal data.

  A run is every place of one layer from one end to the other, both ends in
  the run, in the order in which the table keeps its keys: the printed text
  of the entity, then the printed text of the key. An end is `[entity]`,
  `[entity key]`, `:first` (only as the first end: from the layer's first
  place) or `:last` (only as the last end: to the layer's last place). The
  same two ends are read in each layer of the list. A tool chose none of
  these orders: two entities are neighbours only if their ids print as
  neighbours.

  The thing of a `:points-at` is a value as a maker gives it: for a thing
  whose id is a UUID, the UUID itself. It can be any value of the form but
  nil.

  It is made as `softland.store.act` and the door's declaration are made, of
  the same things in the same order. Read `words`, `shapes` and `rules`
  first. `words` declares what can be asked: for each word its own shape and
  the reason when a thing under that word does not fit. `shapes` is
  `act.clj`'s map of shapes with the query's entries added. `rules` is a
  list of pairs, the name of a rule and its reason word, in the order in
  which they are examined; `rule-checks` holds the code of each. `check`
  reads the three and holds no knowledge of the form itself.

  A new word is added once, in code, in three maps that are keyed by the
  word: `words` here, `softland.store.read-path.sources/of-word` (what it
  reads in one layer) and `softland.store.read-path.answer/of-word` (how its
  rows are put together). The read path's pure proof holds that the three
  have the same keys.

  Inputs and results, by call:

    check    anything -> {:ok query} in the one form, or
             {:refuse word :at part}
    numbers  what an opener gave, or nil -> the read path's seven numbers

  State and effects. None. Each function is pure, and `check` never throws.

  Callers. `softland.store.read-path`, which checks each thing that a caller
  hands it before it builds a key or calls Rama.

  What it requires beside the act's engine and the door's declaration: the
  record's rows, for the order of a run's two ends; and
  `softland.store.read-path.sources`, for the one test of a letter that a
  range of Rama's does not order as the table does.

  Not decided. Who asks is what the caller says (BUILD-reads.md, B2): the
  check takes `:asker` as given. Sign-in would stand in front of the read
  path and change none of this."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

;; ------------------------------------------------------------- the numbers

(def max-layers-of-one-read
  "How many layers one read can go through: the start layer and the layers
  that a builds-on fact can name
  (`softland.store.door.declaration/max-builds-on`). A number of the build,
  kept and not decided."
  (inc declaration/max-builds-on))

(def default-numbers
  "The read path's seven numbers, each with its default. Each is the
  builder's, kept and not decided, and an opener can give another at
  `softland.store.read-path/open`.

    :run-start-places        256     how many places the first read of a run
                                     takes. One act holds 256 facts at most,
                                     so an entity that one act wrote is
                                     whole in one call
    :max-places-of-one-run   20,000  the places of one run in one layer, on
                                     one task, that a read holds. The
                                     largest run that the push probe opened
                                     as one push
    :max-checks-of-one-read  40,000  the checks of nearer layers that one
                                     read of what points at a thing may
                                     need when it is asked once: places
                                     times nearer layers. Each check is
                                     one row of one call
    :max-checks-of-one-live-read
                             250     the same for a read that is kept
                                     live, where each check is one push.
                                     Far fewer, since Rama bounds the
                                     pushes on one table on one task (the
                                     next number): three such reads through
                                     one layer are open under that number
    :max-pushes-under-one-routing-id
                             900     the pushes that the read path holds at
                                     one time on one table under one routing
                                     id. A read kept live that would take it
                                     past this is turned away before it
                                     opens one of those pushes. In a
                                     streaming layer the routing id is the
                                     layer, so it bounds every push of every
                                     asker through one layer. In a
                                     microbatch layer, the base, the routing
                                     id is the home of the read, so it
                                     bounds the pushes on the base of the
                                     askers of one home. It stands
                                     under Rama's own bound of 1,000 pushes
                                     on one table on one task, the default
                                     of Rama's option `pstate.proxy.limit`
                                     (`softland.store.read-path`, \"How many
                                     pushes\"). A read asked once opens no
                                     push and is not held to it
    :max-paths-in-one-call   2,000   the paths of one call of Rama's client
    :wait-ms                 10,000  how long a call of the read path waits
                                     for Rama, in all, before it gives a
                                     word

  None bounds the bytes of a run: no table gives the size of a run's values
  before it is read."
  {:run-start-places                256
   :max-places-of-one-run           20000
   :max-checks-of-one-read          40000
   :max-checks-of-one-live-read     250
   :max-pushes-under-one-routing-id 900
   :max-paths-in-one-call           2000
   :wait-ms                         10000})

(defn numbers
  "Takes what an opener gave for the read path's numbers: a map, or nil.
  Gives the seven numbers: for each name of `default-numbers` the number
  given when it is a fixed-width integer above zero, as a long, and the
  default when it is left out or is none. A name that `default-numbers` does
  not hold is dropped. It never throws: a thing that is no map gives the
  defaults."
  [given]
  (let [given (if (map? given) given {})]
    (into {}
          (map (fn [[k default]]
                 (let [x (get given k)]
                   [k (if (and (int? x) (pos? x)) (long x) default)])))
          default-numbers)))

;; ------------------------------------------------------ the declared form

(def words
  "What can be asked: the four words of a query. Each has the shape that the
  whole of `:asks` must have under that word, in the language of
  `softland.store.act/shapes`, and the reason word when it does not fit.

    :place      one place: an entity and a key
    :entity     one entity: every place of it
    :run        a run of neighbouring places, from one end to the other
    :points-at  what points at a thing: the places under one key whose
                value is the thing

  A word that does not exist yet, such as finding by text or looking back,
  would be one entry here and one in each of the two maps `of-word`."
  {:place     {:shape [:tuple [:enum :place] :id :key]        :reason :place-not-named}
   :entity    {:shape [:tuple [:enum :entity] :id]            :reason :entity-not-named}
   :run       {:shape [:tuple [:enum :run] :run-end :run-end] :reason :run-not-named}
   :points-at {:shape [:tuple [:enum :points-at] :key :value] :reason :points-at-not-named}})

(def shapes
  "The query's map of shapes: each entry of `softland.store.act/shapes`, as
  `act.clj` declares it, and two more, in the same language.

    :query    a query: its seven parts, in the order of the checks
    :run-end  one end of a run: `:first`, `:last`, `[entity]` or
              `[entity key]`

  The shape holds `:asks` only to being there and to being a value: `check`
  then holds the whole of it to the shape of its word (`words`), so that a
  word that is not known and a known word with a wrong part get two
  different reasons. `:with` is a set of the three parts of a row that can
  be asked for. A part that is left out holds nil in the one form, or the
  default that the shape gives."
  (assoc act/shapes
         :query   [:parts
                   [:asks             :value          {:absent :asks-no-word-known :reason :asks-no-word-known}]
                   [:asker            :id             {:optional true :reason :asker-is-no-id}]
                   [:not-signed-in    [:enum true]    {:optional true :reason :no-one-asker}]
                   [:layers           [:seq-of :id]   {:optional true :reason :layers-not-a-list-of-layers}]
                   [:from-layer       :id             {:optional true :reason :from-layer-is-no-layer}]
                   [:with             [:set-of [:enum :writer :permission :time] {:max 3}]
                    {:optional true :default #{} :reason :part-of-a-row-not-known}]
                   [:stores-own-facts [:enum true false] {:optional true :default false}]]
         :run-end [:or [:enum :first :last] [:tuple :id] [:tuple :id :key]]))

(def rules
  "What a thing that has the shape `:query`, and whose `:asks` has the shape
  of its word, must also keep, with the reason for each, in the order in
  which they are examined. `rule-checks` holds the code of each.

    :layers-within-the-limit    a list holds `max-layers-of-one-read` layers
                                at most
    :names-one-asker            exactly one of `:asker` and `:not-signed-in`
    :names-its-layers-one-way   exactly one of `:layers` and `:from-layer`;
                                a list holds one layer at least
    :ids-within-the-limit       the asker, each layer and each entity are
                                ids within the limit of a key's text; each
                                key is a readable keyword within it
    :run-ends-in-order          `:last` is no first end and `:first` no last
                                end; the first end is not after the last
    :run-ends-are-ordered       no id and no key of an end of a run holds a
                                letter from U+E000 on, or one outside the
                                first plane. A range of Rama's between two
                                such ends was seen, on the store's own
                                table, to give fewer places than the table
                                holds, so the read path reads none. An
                                entity whose id holds such a letter is read
                                as `[:entity entity]`, whose run cannot
                                lose a place
    :points-at-names-a-thing    the thing is not nil
    :store-key-is-asked-for     a `:place` under one of the store's own keys
                                needs `:stores-own-facts true`; a
                                `:points-at` under one is turned away either
                                way, since no layer can mark it

  The first is examined before the shape, on the thing as it was given
  (`rules-before-the-shape`)."
  [[:layers-within-the-limit  :too-many-layers]
   [:names-one-asker          :no-one-asker]
   [:names-its-layers-one-way :layers-not-named-one-way]
   [:ids-within-the-limit     :id-over-the-limit]
   [:run-ends-in-order        :run-ends-out-of-order]
   [:run-ends-are-ordered     :run-end-not-ordered]
   [:points-at-names-a-thing  :points-at-not-named]
   [:store-key-is-asked-for   :store-key-not-asked-for]])

(def rules-before-the-shape
  "The rules that `check` examines before the engine reads the thing: the
  engine walks a whole list, so a list of ten thousand layers is counted
  with a bound, and turned away, before any layer of it is read."
  #{:layers-within-the-limit})

(def reasons
  "The words with which `check` turns a thing away, read from the three
  declarations: each word that a part, a word of a query or a rule names,
  and the two that no part names: `:not-a-query` (the thing is no plain map,
  it threw while it was read, or a part that names no word of its own does
  not fit) and `:part-not-known` (a part that the form does not hold)."
  (into #{:not-a-query :part-not-known}
        (concat (map second rules)
                (map :reason (vals words))
                (for [[_ _ opts] (rest (get shapes :query))
                      word (keep #(get opts %) [:reason :absent :empty])]
                  word))))

;; ------------------------------------------------------ the code of the rules

(defn- usable-key?
  "Takes anything. Gives true for a fact's key that a row's key can be made
  of: a readable keyword within the limit of a key's text."
  [x]
  (boolean (and (act/readable-keyword? x) (declaration/id-within-the-limit? x))))

(defn- end-usable?
  "Takes one end of a run in its one form. Gives true when each id and key
  that it holds can be a part of a row's key."
  [end]
  (or (keyword? end)
      (and (declaration/usable-id? (nth end 0))
           (or (= 1 (count end)) (usable-key? (nth end 1))))))

(defn- asks-usable?
  "Takes `:asks` in its one form. Gives true when each entity that it names
  is an id within the limit and each key a readable keyword within it. The
  thing of a `:points-at` is held to no size: a thing that is larger than
  any value that a gate keeps passes, and the read finds no row."
  [[word a b]]
  (case word
    :place     (and (declaration/usable-id? a) (usable-key? b))
    :entity    (declaration/usable-id? a)
    :run       (and (end-usable? a) (end-usable? b))
    :points-at (usable-key? a)
    false))

(defn- end-ordered?
  "Takes one end of a run in its one form. Gives true when no id and no key
  of it holds a UTF-16 unit from U+D800 on
  (`softland.store.read-path.sources/outside-the-agreed-range?`)."
  [end]
  (or (keyword? end)
      (not-any? (fn [id] (sources/outside-the-agreed-range? (declaration/id-text id))) end)))

(def ^:private rule-checks
  "The code of each rule of `rules`. Each takes a query and gives nil when
  the query keeps the rule, or the part of the query at which it is broken.
  Each takes the query in its one form, with `:asks` in the shape of its
  word, and may rest on the rules before it; but `:layers-within-the-limit`,
  which takes the thing as it was given and reads no layer of it."
  {:layers-within-the-limit
   (fn [thing]
     (let [layers (get thing :layers)]
       (when (and (sequential? layers)
                  (< max-layers-of-one-read (bounded-count (inc max-layers-of-one-read) layers)))
         :layers)))

   :names-one-asker
   (fn [q] (when (= (some? (:asker q)) (some? (:not-signed-in q))) :asker))

   :names-its-layers-one-way
   (fn [q] (when (or (= (some? (:layers q)) (some? (:from-layer q)))
                     (and (some? (:layers q)) (empty? (:layers q))))
             :layers))

   :ids-within-the-limit
   (fn [q]
     (cond
       (and (some? (:asker q)) (not (declaration/usable-id? (:asker q)))) :asker
       (not-every? declaration/usable-id? (:layers q)) :layers
       (and (some? (:from-layer q)) (not (declaration/usable-id? (:from-layer q)))) :from-layer
       (not (asks-usable? (:asks q))) :asks
       :else nil))

   ;; The order of the two ends is the same in every layer, since each text
   ;; begins with the layer's own; so it is asked with one layer id, which
   ;; names no layer of the store.
   :run-ends-in-order
   (fn [q]
     (let [[word first-end last-end] (:asks q)]
       (when (and (= :run word)
                  (or (= :last first-end)
                      (= :first last-end)
                      (nil? (rows/run-of-ends :any-layer first-end last-end))))
         :asks)))

   :run-ends-are-ordered
   (fn [q]
     (let [[word first-end last-end] (:asks q)]
       (when (and (= :run word) (not (and (end-ordered? first-end) (end-ordered? last-end))))
         :asks)))

   :points-at-names-a-thing
   (fn [q] (let [[word _ thing] (:asks q)]
             (when (and (= :points-at word) (nil? thing)) :asks)))

   :store-key-is-asked-for
   (fn [q]
     (let [[word a b] (:asks q)]
       (when (or (and (= :place word) (declaration/store-key? b) (not (:stores-own-facts q)))
                 (and (= :points-at word) (declaration/store-key? a)))
         :asks)))})

;; ------------------------------------------------------------------ the check

(defn- plain-map?
  "True for a map that is no record."
  [x]
  (and (map? x) (not (record? x))))

(defn- part-at-fault
  "Takes a thing that the engine refused under the shape `:query`. Gives the
  part at which it stopped: the first part that the declaration does not
  hold, or else the first declared part, in the declared order, that does
  not fit by itself. It reads the declaration and knows no part by name."
  [thing]
  (let [entries (rest (get shapes :query))
        declared (into #{} (map first) entries)]
    (or (first (remove declared (keys thing)))
        (some (fn [[part :as entry]]
                (when (= :no (first (act/as-shape shapes [:parts entry] (select-keys thing [part]))))
                  part))
              entries))))

(defn- broken-rule
  "Takes the names of the rules to examine, in order, and a thing. Gives
  {:refuse word :at part} for the first of them that the thing breaks, or
  nil."
  [rule-names thing]
  (some (fn [[rule word]]
          (when (contains? rule-names rule)
            (when-let [at ((get rule-checks rule) thing)]
              {:refuse word :at at})))
        rules))

(defn- check*
  "The body of `check`, with no guard around it. It can throw; `check` turns
  each throw into `:not-a-query`."
  [thing]
  (or (when-not (plain-map? thing) {:refuse :not-a-query :at nil})
      (broken-rule rules-before-the-shape thing)
      (let [[verdict got] (act/as-shape shapes :query thing)]
        (if (= :no verdict)
          {:refuse (case got
                     :unknown-part :part-not-known
                     (nil :malformed) :not-a-query
                     got)
           :at (part-at-fault thing)}
          (let [asks (:asks got)
                entry (when (vector? asks) (get words (first asks)))]
            (if-not entry
              {:refuse :asks-no-word-known :at :asks}
              (let [[fits normal] (act/as-shape shapes (:shape entry) asks)]
                (if (= :no fits)
                  {:refuse (:reason entry) :at :asks}
                  (let [query (assoc got :asks normal)]
                    (or (broken-rule (into #{} (comp (map first) (remove rules-before-the-shape)) rules)
                                     query)
                        {:ok query}))))))))))

(defn check
  "The check of a query. Takes anything. Gives {:ok query}, with the query
  in its one form, or {:refuse word :at part}, with one of `reasons` and
  the part of the thing at which the check stopped (nil for a thing that is
  no map). It never throws: each throw becomes `:not-a-query`.

  In order. The thing must be a plain map. A list of layers is counted with
  a bound before any layer of it is read. Then the shape `:query`: the
  first part that is wrong, in the declared order, gives its reason. Then
  the word of `:asks`: a first slot that `words` does not hold gives
  `:asks-no-word-known`, and a thing that does not fit its word's shape
  gives that word's reason. Then the rules, in their order.

  The one form. Each part is there. `:asks` is a plain vector in the shape
  of its word, its thing in its one normal form. `:asker` is nil for a
  person who is not signed in, and `:not-signed-in` is then true. One of
  `:layers`, a plain vector, and `:from-layer` is nil. `:with` is a set.
  `:stores-own-facts` is true or false.

  It is the test that must come before any key is made
  (`softland.store.record.rows`, \"What must hold before a key is made\"):
  after it, each id that a caller gave is an id within the limit, each key a
  readable keyword within it, and the thing of a `:points-at` is a value.

  Limits. A part that has no end, other than `:layers`, does not come back.
  A thing of a `:points-at` that lies inside as many collections as a value
  can is turned away as `:asks-no-word-known`, since `:asks` as a whole is
  then no value. It reads no table: whether a layer is made, and whether the
  asker may see it, are `softland.store.read-path.layers`'s."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :not-a-query :at nil})))
