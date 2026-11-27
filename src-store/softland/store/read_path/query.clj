(ns softland.store.read-path.query
  "A query written as data: its one form, declared, and the check of that
  form; and the read path's numbers. Pure code.

  Once or live is no part of the data: it is the call that the caller makes,
  so one query can be asked both ways and two askers of one thing hold equal
  data.

  `:lanes` is nearest first; `:from-lane` takes the list from the lane.
  `:session` is the login sitting that the read is asked in, as an act names
  its `:session`. `:asker` is a party that can act (`:person.<id>`,
  `:agent.<id>`, `:run.<id>` or `:system`), when the asker is signed in: who
  asks is what the caller says.

  A run is every place of one lane from one end to the other, both ends in
  the run, in the order in which the table keeps its keys: the printed text
  of the entity, then the printed text of the key. So two entities are
  neighbours only if their ids print as neighbours. An end is `[entity]`,
  `[entity key]`, `:first` (from the lane's first place) or `:last` (to the
  lane's last place). The same two ends are read in each lane of the list.

  The thing of a `:points-at` is a value as a maker gives it: for a thing
  whose id is a UUID, the UUID itself.

  A query of `:acts` names one lane and is read once. Its `:with` and its
  `:stores-own-facts` say what of a fact's row a read gives, and it gives
  none.

  `check` never throws."
  (:require [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

;; ------------------------------------------------------------- the numbers

(def max-lanes-of-one-read
  "The start lane and the lanes that a builds-on fact can name."
  (inc declaration/max-builds-on))

(def default-numbers
  "The read path's numbers. An opener can give another at
  `softland.store.read-path/open`.

    :run-start-places        the places that the first read of a run takes.
                             One act holds 256 facts at most, so an entity
                             that one act wrote is whole in one call
    :max-places-of-one-run   in one lane, on one task. The largest run that
                             the push probe opened as one push
    :max-checks-of-one-read  the checks of nearer lanes that one read of
                             what points at a thing may need when it is
                             asked once: places times nearer lanes. Each
                             check is one row of one call
    :max-checks-of-one-live-read
                             the same kept live, where each check is one
                             push. Far fewer, since Rama bounds the pushes
                             on one table on one task: three such reads
                             through one lane are open under the next
                             number
    :max-pushes-under-one-routing-id
                             the pushes that the read path holds at one time
                             on one table under one routing id
                             (`softland.store.read-path`, \"How many
                             pushes\"). It stands under Rama's own bound of
                             1,000 pushes on one table on one task, the
                             default of Rama's option `pstate.proxy.limit`.
                             A read asked once opens no push and is not
                             held to it
    :max-paths-in-one-call   of one call of Rama's client
    :wait-ms                 how long a call of the read path waits for
                             Rama, in all
    :max-acts-of-one-read    the `most` of a query is cut to it. A page of
                             1,000 line rows is about 250 KB
    :max-thing-weight-of-one-read
                             the weight of the things that one read of a
                             lane's acts fetches and gives, in all: 32 MiB,
                             the heaviest thing that a gate keeps, so that
                             each kept thing can be read. A thing that
                             alone weighs more is given with no thing, and
                             one that does not fit in what is left ends the
                             read before it

  None of the first seven bounds the bytes of a run: no table gives the size
  of a run's values before it is read. Each row of the line holds its
  thing's weight, so a read of acts is bounded in bytes by the last."
  {:run-start-places                256
   :max-places-of-one-run           20000
   :max-checks-of-one-read          40000
   :max-checks-of-one-live-read     250
   :max-pushes-under-one-routing-id 900
   :max-paths-in-one-call           2000
   :wait-ms                         10000
   :max-acts-of-one-read            1000
   :max-thing-weight-of-one-read    33554432})

(defn numbers
  "It never throws: a thing that is no map gives the defaults."
  [given]
  (let [given (if (map? given) given {})]
    (into {}
          (map (fn [[k default]]
                 (let [x (get given k)]
                   [k (if (and (int? x) (pos? x)) (long x) default)])))
          default-numbers)))

;; ------------------------------------------------------ the declared form

(def words
  "What can be asked, each word with the shape of the whole of `:asks`, in
  the language of `softland.store.act/shapes`, and its reason word.
  `:points-at` gives the places under one key whose value is the thing.
  `:acts` reads a lane's line and no facts.

  A new word of facts, such as finding by text or looking back, is one entry
  here and one in each of `softland.store.read-path.sources/of-word` and
  `softland.store.read-path.answer/of-word`. The read path's pure proof
  holds that the three have the same keys, but for `:acts`."
  {:place     {:shape [:tuple [:enum :place] :id :key]        :reason :place-not-named}
   :entity    {:shape [:tuple [:enum :entity] :id]            :reason :entity-not-named}
   :run       {:shape [:tuple [:enum :run] :run-end :run-end] :reason :run-not-named}
   :points-at {:shape [:tuple [:enum :points-at] :key :value] :reason :points-at-not-named}
   :acts      {:shape [:tuple [:enum :acts] [:int {:min 0}] [:int {:min 1}]] :reason :acts-not-named}})

(def shapes
  "`softland.store.act/shapes` with the query's two. The shape holds `:asks`
  only to being there and to being a value: `check` then holds the whole of
  it to the shape of its word, so that a word that is not known and a known
  word with a wrong part get two different reasons."
  (assoc act/shapes
         :query   [:parts
                   [:asks             :value          {:absent :asks-no-word-known :reason :asks-no-word-known}]
                   [:asker            :author-party   {:optional true :reason :asker-not-a-party}]
                   [:not-signed-in    [:enum true]    {:optional true :reason :no-one-asker}]
                   [:session          :id             {:optional true :reason :session-is-no-id}]
                   [:lanes           [:seq-of :id]   {:optional true :reason :lanes-not-a-list-of-lanes}]
                   [:from-lane       :id             {:optional true :reason :from-lane-is-no-lane}]
                   [:with             [:set-of [:enum :writer :permission :time] {:max 3}]
                    {:optional true :default #{} :reason :part-of-a-row-not-known}]
                   [:stores-own-facts [:enum true false] {:optional true :default false}]]
         :run-end [:or [:enum :first :last] [:tuple :id] [:tuple :id :key]]))

(def rules
  "What a query must also keep, with the reason word for each, in the order
  in which they are examined.

  `:a-run-has-an-asker`: a run is somebody's. `:acts-of-one-lane`: two lines
  have no order between them. `:store-key-is-asked-for`: a `:points-at`
  under one of the store's own keys is turned away either way, since no lane
  can mark it.

  `:run-ends-are-ordered`: no id and no key of an end of a run holds a
  letter from U+E000 on, or one outside the first plane. A range of Rama's
  between two such ends was seen, on the store's own table, to give fewer
  places than the table holds, so the read path reads none. An entity whose
  id holds such a letter is read as `[:entity entity]`, whose run cannot
  lose a place."
  [[:lanes-within-the-limit  :too-many-lanes]
   [:names-one-asker          :no-one-asker]
   [:a-run-has-an-asker       :run-with-no-asker]
   [:names-its-lanes-one-way :lanes-not-named-one-way]
   [:acts-of-one-lane        :acts-not-of-one-lane]
   [:ids-within-the-limit     :id-over-the-limit]
   [:run-ends-in-order        :run-ends-out-of-order]
   [:run-ends-are-ordered     :run-end-not-ordered]
   [:points-at-names-a-thing  :points-at-not-named]
   [:store-key-is-asked-for   :store-key-not-asked-for]])

(def rules-before-the-shape
  "The engine walks a whole list, so a list of ten thousand lanes is counted
  with a bound, and turned away, before any lane of it is read."
  #{:lanes-within-the-limit})

(def reasons
  "The words with which `check` turns a thing away. Two are named by no
  part: `:not-a-query` (the thing is no plain map, it threw while it was
  read, or a part that names no word of its own does not fit) and
  `:part-not-known` (a part that the form does not hold)."
  (into #{:not-a-query :part-not-known}
        (concat (map second rules)
                (map :reason (vals words))
                (for [[_ _ opts] (rest (get shapes :query))
                      word (keep #(get opts %) [:reason :absent :empty])]
                  word))))

;; ------------------------------------------------------ the code of the rules

(defn- usable-key?
  [x]
  (boolean (and (act/readable-keyword? x) (declaration/id-within-the-limit? x))))

(defn- end-usable?
  [end]
  (or (keyword? end)
      (and (declaration/usable-id? (nth end 0))
           (or (= 1 (count end)) (usable-key? (nth end 1))))))

(defn- asks-usable?
  "The thing of a `:points-at` is held to no size: a thing that is larger
  than any value that a gate keeps passes, and the read finds no row."
  [[word a b]]
  (case word
    :place     (and (declaration/usable-id? a) (usable-key? b))
    :entity    (declaration/usable-id? a)
    :run       (and (end-usable? a) (end-usable? b))
    :points-at (usable-key? a)
    :acts      true
    false))

(defn- end-ordered?
  [end]
  (or (keyword? end)
      (not-any? (fn [id] (sources/outside-the-agreed-range? (declaration/id-text id))) end)))

(def ^:private rule-checks
  "Each gives nil, or the part of the query at which its rule is broken.
  Each takes the query in its one form, with `:asks` in the shape of its
  word, and may rest on the rules before it; but `:lanes-within-the-limit`
  takes the thing as it was given and reads no lane of it."
  {:lanes-within-the-limit
   (fn [thing]
     (let [lanes (get thing :lanes)]
       (when (and (sequential? lanes)
                  (< max-lanes-of-one-read (bounded-count (inc max-lanes-of-one-read) lanes)))
         :lanes)))

   :names-one-asker
   (fn [q] (when (= (some? (:asker q)) (some? (:not-signed-in q))) :asker))

   :a-run-has-an-asker
   (fn [q] (when (and (some? (:session q)) (nil? (:asker q))) :session))

   :names-its-lanes-one-way
   (fn [q] (when (or (= (some? (:lanes q)) (some? (:from-lane q)))
                     (and (some? (:lanes q)) (empty? (:lanes q))))
             :lanes))

   :acts-of-one-lane
   (fn [q] (when (and (= :acts (first (:asks q))) (some? (:lanes q)) (not= 1 (count (:lanes q))))
             :lanes))

   :ids-within-the-limit
   (fn [q]
     (cond
       (and (some? (:asker q)) (not (declaration/usable-id? (:asker q)))) :asker
       (and (some? (:session q)) (not (declaration/usable-id? (:session q)))) :session
       (not-every? declaration/usable-id? (:lanes q)) :lanes
       (and (some? (:from-lane q)) (not (declaration/usable-id? (:from-lane q)))) :from-lane
       (not (asks-usable? (:asks q))) :asks
       :else nil))

   ;; The order of the two ends is the same in every lane, since each text
   ;; begins with the lane's own; so it is asked with one lane id, which
   ;; names no lane of the store.
   :run-ends-in-order
   (fn [q]
     (let [[word first-end last-end] (:asks q)]
       (when (and (= :run word)
                  (or (= :last first-end)
                      (= :first last-end)
                      (nil? (rows/run-of-ends :any-lane first-end last-end))))
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
  [x]
  (and (map? x) (not (record? x))))

(defn- part-at-fault
  [thing]
  (let [entries (rest (get shapes :query))
        declared (into #{} (map first) entries)]
    (or (first (remove declared (keys thing)))
        (some (fn [[part :as entry]]
                (when (= :no (first (act/as-shape shapes [:parts entry] (select-keys thing [part]))))
                  part))
              entries))))

(defn- broken-rule
  [rule-names thing]
  (some (fn [[rule word]]
          (when (contains? rule-names rule)
            (when-let [at ((get rule-checks rule) thing)]
              {:refuse word :at at})))
        rules))

(defn- check*
  "It can throw; `check` turns each throw into `:not-a-query`."
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
  "Gives {:ok query}, with the query in its one form, or {:refuse word :at
  part}, with one of `reasons`; the part is nil for a thing that is no map.
  It never throws.

  The one form. Each part is there: one that was left out holds nil, or the
  default that its shape gives. `:asks` is a plain vector in the shape of
  its word, its thing in its one normal form. `:asker` is nil for a person
  who is not signed in. `:session` is nil for a read that is asked in no
  run. One of `:lanes`, a plain vector, and `:from-lane` is nil.

  It is the test that must come before any key is made
  (`softland.store.record.rows`): after it, each id that a caller gave is an
  id within the limit, each key a readable keyword within it, and the thing
  of a `:points-at` is a value.

  Limits. A part that has no end, other than `:lanes`, does not come back. A
  thing of a `:points-at` that lies inside as many collections as a value
  can is turned away as `:asks-no-word-known`, since `:asks` as a whole is
  then no value. It reads no table: whether a lane is made, and whether the
  asker may see it, are `softland.store.read-path.lanes`'s."
  [thing]
  (try (check* thing)
       (catch Throwable _ {:refuse :not-a-query :at nil})))
