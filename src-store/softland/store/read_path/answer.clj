(ns softland.store.read-path.answer
  "What a read gives: the layers put together, and what a change tells a
  live asker. Pure code. It needs no cluster.

  For each place the nearest layer that holds a fact wins, also when that
  fact says \"nothing here\" (BUILD-reads.md, B3). Every function here
  works that out from two things that it is handed at the moment of the
  call: what `softland.store.read-path.sources/reading` gave for the read,
  and `known`, the values that selects gave or that Rama's pushes hold now.
  None keeps anything, and none is handed anything that was put together
  before. So what a live read gives is worked out anew at each change, from
  the pushes' values, for the places that the change names.

  An answer is a plain map. For every word of a query it holds

    :layers    the layers that the read took, nearest first
    :left-out  each of those layers that gave nothing, with why:
               {:layer layer :why :may-not-see} or {:layer layer :why
               :not-made} (`softland.store.read-path.layers/left-out`)

  and then, by the word:

    :place          `:place`, one place-answer
    :entity, :run   `:places`, a map from `[entity key]` to a place-answer,
                    for each place of the run at which a layer that she may
                    see holds a fact
    :points-at      `:places`, the same, for each place that points at the
                    thing; and `:orders`, for each layer that she may see,
                    {:layer layer :level-since stamp} or {:layer layer
                    :not-marked true}

  A place-answer tells four things apart, and says which layer won:

    {:holds :no-fact}                                  no layer that she may
                                                       see holds a fact there
    {:holds :nothing-here :layer l :fact f :stamp s}   the nearest fact says
                                                       \"nothing here\"
    {:holds :value :value v :layer l :fact f :stamp s} the nearest fact, with
                                                       its value as its
                                                       maker gave it
    {:holds :not-a-value :layer l :fact f :stamp s}    the nearest fact's
                                                       bytes are no value of
                                                       the form

  `:fact` is the fact id `[[layer depot uuid] index]`, which a writer names
  in `:replaces`; `:stamp` is the stamp of that fact's decision, which a
  writer carries in `:based-on`. With `:writer` in the query's `:with` a
  place-answer also holds `:writer`, and with `:permission` it holds
  `:permission`; a read that asked for neither is handed neither.

  What points at a thing, through layers. A place is found through an
  order: the read looks at a place when the order by value of some layer
  that she may see holds it. At such a place the nearest layer that she may
  see and that holds a row wins: by its own order, or by the check of its
  facts table. The place is given when the winner's value is the thing:
  from the order's row, or from the check's row when that row holds the
  thing. So a nearer fact that moves the thing, or takes it away, hides the
  place; a nearer fact that says the same thing does not, and the place
  comes from the nearer layer. What an asker must know of it: a place can
  come from a layer whose entry in `:orders` says `:not-marked`, or with a
  stamp that is not later than that layer's `:level-since`; and a place at
  which a nearer layer says the thing, and which no order of a layer that
  she may see holds, is not in the answer.

  Inputs and results, by call:

    counts?             a query, a key -> whether a row under that key counts
    place-answer        a query, a layer, a row -> a place-answer
    order-place-answer  a query, a layer, the thing, a row -> a place-answer
    one-of-each         the parts of a run, a key -> the one row under it
    of-word             a map from each word to how its rows are put together
    of-places           a query, a reading, `known`, places -> their
                        place-answers, without the places that still wait
    whole               a query, a reading, `known` -> the answer
    told-by?            whether a change of a source at a place is to be told
    meanings            what a rights source meant before and after a change
    after-change        what a live read does at a change of a push
    after-arrival       what a live read does when a push that it came to
                        need is given

  State and effects. None. Each function is pure.

  Callers. `softland.store.read-path`. The read path's pure proof plays the
  last two in every order of the callbacks."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.layers :as layers]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

;; ------------------------------------------------------------ one place

(defn counts?
  "Takes a query in its one form and a fact's key. Gives false for a row
  under one of the store's own keys
  (`softland.store.door.declaration/store-key?`) when the query did not ask
  for the store's own facts, and true for any other. A row that does not
  count is no row for the read: it wins no place and is in no answer. So
  the run of a holder's entity does not give her permission, and a run
  across entities gives no fact of the store's own.

  Not decided. That a tool's read leaves the store's own facts out unless
  it asks, and that any asker who may see the layer may ask, is a working
  answer, for Sid. This function is the one place where the other answer
  would be made."
  [query k]
  (or (boolean (:stores-own-facts query))
      (not (declaration/store-key? k))))

(defn- with-asked-parts
  "Takes a query, a row and a place-answer. Gives the place-answer with the
  parts of the row that the query asked for: who wrote the fact, and the
  permission that its act named. The one place where either is put in. Rama
  sends the runtime the whole row either way; a caller is handed a part
  only when she asked."
  [query row place-answer]
  (cond-> place-answer
    (contains? (:with query) :writer)     (assoc :writer (rows/who-of row))
    (contains? (:with query) :permission) (assoc :permission (rows/permission-of row))))

(defn- fact-id
  "Takes a layer and a row. Gives the id of the fact that wrote the row:
  [[layer depot uuid] index], with the depot that the layer starts with."
  [layer row]
  [[layer (first-act/depot-at-making layer) (:by-act row)] (:by-index row)])

(defn place-answer
  "Takes a query in its one form, the layer that won a place, and the row
  that won it, or nil. Gives the place-answer: one of the four that the
  namespace docstring lists, by `softland.store.record.rows/read-value`,
  with the layer, the fact id and the stamp for a row that is there, and
  the parts of the row that the query asked for. It never throws on a row."
  [query layer row]
  (let [[kind v] (rows/read-value row)]
    (if (= :no-fact kind)
      {:holds :no-fact}
      (with-asked-parts
        query row
        (cond-> {:holds kind :layer layer :fact (fact-id layer row) :stamp (:stamp row)}
          (= :value kind) (assoc :value v))))))

(defn order-place-answer
  "Takes a query in its one form, the layer that won a place, the thing that
  the query asks about, and the row that won the place: a row of the
  layer's order by value, which holds no value, or a check's row that holds
  the thing. Gives the place-answer, whose `:value` is the thing as the
  asker gave it, in its one normal form."
  [query layer thing row]
  (with-asked-parts
    query row
    {:holds :value :value thing :layer layer :fact (fact-id layer row) :stamp (:stamp row)}))

(defn one-of-each
  "Takes the parts of one run in a microbatch layer, each a map from key to
  row, and a key. Gives the one row under that key: nil when no part holds
  it; else, among the parts that hold it, the row with the latest stamp. A
  place of a plain key is in one part. A row that every task holds is in
  each part, and the tasks do not show a write of it at one instant, so two
  parts can differ for a moment; the later stamp is the newer row."
  [parts k]
  (reduce (fn [best part]
            (let [row (sources/row-in part k)]
              (if (and row (or (nil? best) (> (long (:stamp row)) (long (:stamp best)))))
                row
                best)))
          nil
          parts))

;; ------------------------------------------- how the rows of a word are put together

(defn- known-facts
  "Takes a reading, `known` and a layer. Gives the values of the layer's
  fact sources, in their order; or nil when `known` does not hold one of
  them, or the layer has none."
  [reading known layer]
  (let [fact-sources (get-in reading [:by-layer layer :facts])]
    (when (and (seq fact-sources) (every? #(contains? known %) fact-sources))
      (mapv (fn [source] [source (get known source)]) fact-sources))))

(defn- fact-row-at
  "Takes the known values of a layer's fact sources (`known-facts`) and the
  key of a place in that layer. Gives the row there, or nil: the value of a
  source of one row, or the row that the parts of a run hold under the
  key."
  [facts key-text]
  (let [[source value] (first facts)]
    (if (= :row (first (:path source)))
      value
      (one-of-each (map second facts) key-text))))

(defn- walk-facts
  "The walk of a place for a place, an entity or a run. Takes the query, a
  reading, `known` and a place. Goes through the layers from the nearest:
  the first layer of standing `:yes` whose fact sources hold a row at the
  place wins. Gives [:given place-answer]; [:none] when no layer holds a
  row there, or the place's key does not count; or [:waits] when the walk
  meets, before it stops, a layer whose standing is not known yet or a fact
  source that `known` does not hold. A source behind the layer that wins is
  not waited for."
  [query reading known [entity k]]
  (if-not (counts? query k)
    [:none]
    (loop [todo (:layers reading)]
      (if-let [layer (first todo)]
        (case (get-in reading [:standing layer])
          :not-known-yet [:waits]
          :yes (if-let [facts (known-facts reading known layer)]
                 (if-let [row (fact-row-at facts (rows/place-key layer entity k))]
                   [:given (place-answer query layer row)]
                   (recur (rest todo)))
                 [:waits])
          (recur (rest todo)))
        [:none]))))

(defn- order-row-at
  "Takes the query, a reading, `known`, a layer and an entity. Gives the row
  that the layer's order by value holds for that entity, or nil: nil too
  when the layer marks no order or `known` does not hold it."
  [query reading known layer entity]
  (let [{:keys [facts since]} (get-in reading [:by-layer layer])
        order-source (first facts)
        [_ k] (:asks query)]
    (when (and order-source since)
      (sources/row-in (get known order-source)
                      (rows/by-value-key layer k since (:digest reading) entity)))))

(defn- walk-orders
  "The walk of a place for what points at a thing. Takes the query, a
  reading, `known` and a place. Goes through the layers from the nearest.
  The first layer of standing `:yes` that holds a row at the place wins: by
  its own order, or by its check. A winner by its order gives the place
  from the order's row. A winner by its check gives the place from the
  check's row when that row holds the thing
  (`softland.store.record.rows/holds-this-value?`, the test by which a gate
  puts a fact in that thing's order), and gives [:none] when it says
  \"nothing here\" or holds another thing. Gives [:given place-answer],
  [:none], or [:waits] as `walk-facts` does: an order or a check that the
  read wants and `known` does not hold, met before the walk stops."
  [query reading known [entity :as place]]
  (let [[_ _ thing] (:asks query)]
    (loop [todo (:layers reading)]
      (if-let [layer (first todo)]
        (case (get-in reading [:standing layer])
          :not-known-yet [:waits]
          :yes (let [{:keys [facts checks]} (get-in reading [:by-layer layer])
                     order-source (first facts)
                     check (get checks place)]
                 (cond
                   (and order-source (not (contains? known order-source)))
                   [:waits]

                   (order-row-at query reading known layer entity)
                   [:given (order-place-answer query layer thing (order-row-at query reading known layer entity))]

                   (nil? check)
                   (recur (rest todo))

                   (not (contains? known check))
                   [:waits]

                   (nil? (get known check))
                   (recur (rest todo))

                   (rows/holds-this-value? (get known check) thing)
                   [:given (order-place-answer query layer thing (get known check))]

                   :else
                   [:none]))
          (recur (rest todo)))
        [:none]))))

(defn- places-of-runs
  "Takes the query, a reading and `known`. Gives the set of the places,
  `[entity key]`, at which a fact source of a layer of standing `:yes`
  holds a row that counts."
  [query reading known]
  (into #{}
        (for [layer (:layers reading)
              :when (= :yes (get-in reading [:standing layer]))
              source (get-in reading [:by-layer layer :facts])
              :when (= :run (first (:path source)))
              key-text (sources/keys-of-run source (get known source))
              :let [[_ entity k] (rows/place-of-key key-text)]
              :when (and entity (counts? query k))]
          [entity k])))

(defn- places-of-orders
  "Takes the query, a reading and `known`. Gives the set of the places,
  `[entity key]`, that the order by value of some layer of standing `:yes`
  holds. A place is found through an order, and through nothing else."
  [query reading known]
  (let [[_ k] (:asks query)]
    (into #{}
          (for [layer (:layers reading)
                :when (= :yes (get-in reading [:standing layer]))
                source (get-in reading [:by-layer layer :facts])
                key-text (sources/keys-of-run source (get known source))
                :let [entity (rows/by-value-entity key-text)]
                :when (declaration/usable-id? entity)]
            [entity k]))))

(defn- holds-fact-row?
  "Takes the query, a reading, `known`, a layer and a place. Gives true when
  a fact source of the layer, in `known`, holds a row at the place."
  [_query reading known layer [entity k]]
  (if-let [facts (known-facts reading known layer)]
    (some? (fact-row-at facts (rows/place-key layer entity k)))
    false))

(defn- holds-order-or-check-row?
  "Takes the query, a reading, `known`, a layer and a place. Gives true when
  the layer holds a row at the place for what points at a thing: in its
  order by value, or by a check that the read wants and `known` holds. A
  source that the read no longer wants is not counted."
  [query reading known layer [entity :as place]]
  (let [check (get-in reading [:by-layer layer :checks place])]
    (boolean (or (order-row-at query reading known layer entity)
                 (and check (some? (get known check)))))))

(def of-word
  "How the rows of each word of a query are put together. A map from the
  word to three functions and a name:

    :walk    a query, a reading, `known`, a place -> [:given place-answer],
             [:none] or [:waits]: the walk of one place from the nearest
             layer
    :places  a query, a reading, `known` -> the places that the answer
             looks at
    :holds?  a query, a reading, `known`, a layer, a place -> whether that
             layer holds a row at the place now
    :gives   `:place` when the answer holds one place-answer, `:places`
             when it holds a map of them

  A place, an entity and a run are put together from rows of the facts
  table; what points at a thing from the orders by value and the checks."
  {:place     {:walk walk-facts
               :places (fn [query _reading _known] (let [[_ entity k] (:asks query)] #{[entity k]}))
               :holds? holds-fact-row?
               :gives :place}
   :entity    {:walk walk-facts :places places-of-runs :holds? holds-fact-row? :gives :places}
   :run       {:walk walk-facts :places places-of-runs :holds? holds-fact-row? :gives :places}
   :points-at {:walk walk-orders :places places-of-orders :holds? holds-order-or-check-row? :gives :places}})

(defn- word-of
  "Takes a query in its one form. Gives the entry of `of-word` for its
  word."
  [query]
  (get of-word (first (:asks query))))

;; ------------------------------------------------------------ what is given

(defn of-places
  "Takes a query in its one form, what `reading` gave, `known`, and places,
  each `[entity key]`. Gives a map from each place to what the read gives
  there now: a place-answer, with {:holds :no-fact} for a place that the
  read does not give. A place that still waits is not in the map: its walk
  met, before it stopped, a layer whose standing is not known yet or a
  wanted source that `known` does not hold."
  [query reading known places]
  (let [walk (:walk (word-of query))]
    (into {}
          (keep (fn [place]
                  (let [[outcome place-answer] (walk query reading known place)]
                    (case outcome
                      :given [place place-answer]
                      :none  [place {:holds :no-fact}]
                      nil))))
          places)))

(defn whole
  "Takes a query in its one form, what `reading` gave, and `known`. Gives
  the answer, as the namespace docstring has it. A layer whose standing is
  `:may-not-see` or `:not-made` is in `:left-out`, and nothing of it is in
  the answer, whatever `known` holds of it. What must hold: nothing is
  missing (`reading`'s `:missing` is empty), so no place waits."
  [query reading known]
  (let [{:keys [walk places gives]} (word-of query)
        {:keys [layers standing by-layer]} reading
        given (fn [place] (let [[outcome place-answer] (walk query reading known place)]
                            (when (= :given outcome) place-answer)))
        answer {:layers layers
                :left-out (into []
                                (keep (fn [layer]
                                        (let [why (get standing layer)]
                                          (when (contains? #{:may-not-see :not-made} why)
                                            (layers/left-out layer why)))))
                                layers)}]
    (cond-> (if (= :place gives)
              (assoc answer :place (or (given (first (places query reading known))) {:holds :no-fact}))
              (assoc answer :places (into {}
                                          (keep (fn [place] (when-let [place-answer (given place)]
                                                              [place place-answer])))
                                          (places query reading known))))
      (= :points-at (first (:asks query)))
      (assoc :orders (into []
                           (keep (fn [layer]
                                   (when (= :yes (get standing layer))
                                     (if-let [since (get-in by-layer [layer :since])]
                                       {:layer layer :level-since since}
                                       {:layer layer :not-marked true}))))
                           layers)))))

;; ----------------------------------------------- what a change tells a live asker

(defn told-by?
  "Takes a query in its one form, what `reading` gave, `known`, the source
  whose push changed, and a place that the change names. Gives true when
  the change is to be told to this asker: the source's layer has the
  standing `:yes` for her, the place's key counts, and no nearer layer of
  standing `:yes` holds a row at the place now.

  It counts a nearer layer's row only in a source that `reading` wants and
  `known` holds. So a step that has just left a check does not count it:
  a change that such a check's push has taken and not yet called back
  would never reach the read, and the place would stay with an answer that
  the tables no longer give. A nearer row that is counted is safe to rest
  on: the read still holds its source, so that source's own change is told
  by its own step."
  [query reading known source place]
  (let [layer (:layer source)
        {:keys [layers standing]} reading
        holds? (:holds? (word-of query))]
    (boolean (and (= :yes (get standing layer))
                  (counts? query (nth place 1))
                  (not-any? (fn [nearer]
                              (and (= :yes (get standing nearer))
                                   (holds? query reading known nearer place)))
                            (take-while #(not= layer %) layers))))))

(defn meanings
  "Takes a query in its one form, `known`, the source whose push changed,
  and the push's value before and after. For the source of a layer's own
  rows, or of the asker's permission row, gives [before after]: what the
  rows meant for this query before the change and after it
  (`softland.store.read-path.layers/meaning`), each beside what `known`
  holds of the other source. Gives nil for any other source."
  [query known source old new]
  (let [layer (:layer source)
        asker (:asker query)
        signed-in? (some? asker)]
    (case (sources/rights-kind query source)
      :own-rows
      (let [permission (when signed-in? (sources/permission-source layer asker))
            permission-row (if (and permission (contains? known permission))
                             (get known permission)
                             :not-known-yet)]
        [(layers/meaning query layer old permission-row signed-in?)
         (layers/meaning query layer new permission-row signed-in?)])

      :permission
      (let [own-rows (get known (sources/own-rows-source layer))]
        [(layers/meaning query layer own-rows old signed-in?)
         (layers/meaning query layer own-rows new signed-in?)])

      nil)))

(defn- owes-for-a-move?
  "Takes a query, and the sources that a step joined and left. Gives true
  when one of them is no check: such a source is joined or left only when
  what shapes the read has changed in the pushes' values."
  [query joined left]
  (boolean (some #(not (sources/check? query %)) (concat joined left))))

(defn- whole-if-nothing-is-missing
  "Takes a query, a reading and `known`, for a read that owes its asker the
  whole answer. Gives {:tell {:anew answer} :owed? false} when nothing is
  missing; else {:tell nil :owed? true}: the mark stays, and the step in
  which the last missing source is given gives the whole answer."
  [query reading known]
  (if (empty? (:missing reading))
    {:tell {:anew (whole query reading known)} :owed? false}
    {:tell nil :owed? true}))

(defn after-change
  "What a live read does at a change of one of its pushes, once the joining
  and the leaving of the step are done and `known` is made again of the
  sources that the read still holds and that are given. Takes the query in
  its one form; what `reading` gave on that `known`; `known`; the source
  whose push changed; the places that the change names
  (`softland.store.read-path.sources/places-of`); what the source meant
  before and after
  (`meanings`; nil and nil for a source that is no rights source); the
  sources that the step joined and left; and whether the whole answer was
  owed already. Gives {:tell :owed?}: what to hand the asker's function,
  nil for nothing, {:changed {place place-answer}} or {:anew answer}; and
  whether the whole answer is owed after the step.

  The whole answer is owed from this step on in two cases: the source is a
  layer's own rows or her permission, and what it means for this query
  differs; or the step joined or left a source that is no check, whatever
  source called. While it is owed, no single
  place is told: the asker gets {:anew answer} when nothing is missing.

  Else each place that the change names is told when `told-by?` says so,
  with what the read gives there now; a place that waits is left out, and
  is told when the source that it waits for is given.

  The read path keeps no value between two changes: this function does not
  compare with what the asker was told before. A place can so be told a
  second time with the answer that it already had."
  [query reading known source places meant-before meant-after joined left owed?]
  (if (or owed?
          (not= meant-before meant-after)
          (owes-for-a-move? query joined left))
    (whole-if-nothing-is-missing query reading known)
    (let [told (of-places query reading known
                          (filter #(told-by? query reading known source %) places))]
      {:tell (when (seq told) {:changed told}) :owed? false})))

(defn after-arrival
  "What a live read does when a push that it came to need is given, once the
  joining and the leaving of the step are done and `known` is made again.
  Takes the query in its one form; what `reading` gave on that `known`;
  `known`; the source that was given; the sources that the step joined and
  left; and whether the whole answer was owed already. Gives {:tell :owed?}
  as `after-change` does.

  A step that joins or leaves a source that is no check owes the whole
  answer. When it is owed, the asker gets {:anew answer} if nothing is
  missing any more, whatever kind of source arrived. Else a check that
  arrives tells its place, with no test, unless the place still waits. Any
  other source that arrives with nothing owed tells nothing."
  [query reading known source joined left owed?]
  (cond
    (or owed? (owes-for-a-move? query joined left))
    (whole-if-nothing-is-missing query reading known)

    (sources/check? query source)
    (let [told (of-places query reading known
                          (sources/places-of query source [(second (:path source))]))]
      {:tell (when (seq told) {:changed told}) :owed? false})

    :else
    {:tell nil :owed? false}))
