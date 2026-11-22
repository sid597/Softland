(ns softland.store.read-path.answer
  "What a read gives: the layers put together, and what a change tells a
  live asker. Pure code. It needs no cluster.

  For each place the nearest layer that holds a fact wins, also when that
  fact says \"nothing here\" (BUILD-reads.md, B3). Every function here
  works that out from two things that it is handed at the moment of the
  call: what `softland.store.read-path.sources/reading` gave for the read,
  and `known`, the values that selects gave or that Rama's pushes hold now.
  None keeps anything. The steps of a live read are handed one thing more,
  the read's mark: whether the whole answer is owed, or what the last whole
  answer said of each layer, seen or left out and why, and which layers wait
  (`mark-of`), and nothing of any fact. So what a live read gives is worked
  out anew at each change, from the pushes' values, for the places that the
  change names; and what it says of a layer that the asker may not see is
  held against what she was told of it.

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
    {:holds :not-a-value :layer l :fact f :stamp s}    the nearest row's
                                                       String is the text of
                                                       no value of the form

  No write of a gate gives a row that earns the fourth
  (`softland.store.record.rows/read-value`); it is there so that a read
  never throws on what a row holds.

  `:fact` is the fact id `[[layer depot uuid] index]`, which a writer names
  in `:replaces`, and which names a fact that its act holds at that index;
  `:stamp` is the stamp of that fact's decision, which a writer carries in
  `:based-on`. One row is written by an act and by no fact of it: the use
  of a yes for one act, at the yes's place, which a read asks for only with
  the store's own facts. Its place-answer is `:nothing-here` with its
  layer and the stamp of the act that used the yes, and no `:fact`. With `:writer` in the query's `:with` a
  place-answer also holds `:writer`, with `:permission` it holds
  `:permission`, and with `:time` it holds `:time`, the time on the act
  that wrote the fact, a label; a read that asked for none is handed
  none.

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
    of-word             a map from each word to how its rows are put together
    of-places           a query, a reading, `known`, places -> their
                        place-answers, without the places that still wait
    whole               a query, a reading, `known` -> the answer
    told-by?            whether a change of a source at a place is to be told
    meanings            what a rights source meant before and after a change
    mark-of             a whole answer -> the mark of a read that was just
                        given it
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
  parts of the row that the query asked for: who wrote the fact, the
  permission that its act named, and the time on that act. The one place
  where any of them is put in. Rama sends the runtime the whole row either
  way; a caller is handed a part only when she asked."
  [query row place-answer]
  (cond-> place-answer
    (contains? (:with query) :writer)     (assoc :writer (rows/who-of row))
    (contains? (:with query) :permission) (assoc :permission (rows/permission-of row))
    (contains? (:with query) :time)       (assoc :time (:time row))))

(defn- fact-id
  "Takes a layer and a row. Gives the id of the fact that wrote the row:
  [[layer depot uuid] index], with the depot that the layer starts with; or
  nil for a row that no fact wrote, which names no index: the use of a yes
  for one act (`softland.store.record.rows/use-row`)."
  [layer row]
  (when (some? (:by-index row))
    [[layer (first-act/depot-at-making layer) (:by-act row)] (:by-index row)]))

(defn place-answer
  "Takes a query in its one form, the layer that won a place, and the row
  that won it, or nil. Gives the place-answer: one of the four that the
  namespace docstring lists, by `softland.store.record.rows/read-value`,
  with the layer, the fact id and the stamp for a row that is there, and
  the parts of the row that the query asked for. A row that no fact wrote
  gives no `:fact`: it names no fact that a writer could replace or a
  reader find in its act. It never throws on a row."
  [query layer row]
  (let [[kind v] (rows/read-value row)]
    (if (= :no-fact kind)
      {:holds :no-fact}
      (with-asked-parts
        query row
        (cond-> {:holds kind :layer layer :stamp (:stamp row)}
          (fact-id layer row) (assoc :fact (fact-id layer row))
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

;; ------------------------------------------- how the rows of a word are put together

(defn- known-fact
  "Takes a reading, `known` and a layer. Gives the layer's fact source with
  its value, as [source value]; or nil when the read wants no fact source
  in the layer, or `known` does not hold it. A word of a query reads one
  fact source in a layer (`softland.store.read-path.sources/of-word`)."
  [reading known layer]
  (let [source (first (get-in reading [:by-layer layer :facts]))]
    (when (and source (contains? known source))
      [source (get known source)])))

(defn- fact-row-at
  "Takes a layer's fact source with its value (`known-fact`) and the key of
  a place in that layer. Gives the row there, or nil: the value of a source
  of one row, or the row that a run holds under the key."
  [[source value] key-text]
  (if (= :row (first (:path source)))
    value
    (sources/row-in value key-text)))

(defn- walk-facts
  "The walk of a place for a place, an entity or a run. Takes the query, a
  reading, `known` and a place. Goes through the layers from the nearest:
  the first layer of standing `:yes` whose fact source holds a row at the
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
          :yes (if-let [fact (known-fact reading known layer)]
                 (if-let [row (fact-row-at fact (rows/place-key layer entity k))]
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
  the layer's fact source, in `known`, holds a row at the place."
  [_query reading known layer [entity k]]
  (if-let [fact (known-fact reading known layer)]
    (some? (fact-row-at fact (rows/place-key layer entity k)))
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
  rows, of the asker's permission row, of the permission of the run that
  she asks in, or of another row under a key of a permission in the layer,
  which can be a row above one of hers, gives [before after]: what the rows
  meant for this query before the change and after it
  (`softland.store.read-path.layers/meaning`), each beside what `known`
  holds of the other sources. Gives nil for any other source.

  Whether she holds a permission that stands is read from `known`
  (`softland.store.read-path.sources/permission-state`), with the changed
  source's value before, and then after, in its place. So the end of a
  permission above hers, or of her run's, means another thing before and
  after, and a change of a permission row that is neither hers nor above
  hers means the same."
  [query known source old new]
  (let [layer (:layer source)
        home (layers/home query)
        signed-in? (some? (:asker query))
        own-source (sources/own-rows-source home layer)
        meant (fn [known]
                (layers/meaning query layer (get known own-source)
                                (sources/permission-state query home layer known)
                                signed-in?))]
    (when (sources/rights-kind query source)
      [(meant (assoc known source old))
       (meant (assoc known source new))])))

(defn- owes-for-a-move?
  "Takes a query, and the sources that a step joined and left. Gives true
  when one of them is no check: such a source is joined or left only when
  what shapes the read has changed in the pushes' values."
  [query joined left]
  (boolean (some #(not (sources/check? query %)) (concat joined left))))

(defn mark-of
  "Takes the whole answer that a live read was just given. Gives the read's
  mark after it: {:told {layer standing}}, what the answer said of each of
  its layers, `:yes` for one that it gives, or the word of `:left-out`
  (`:may-not-see`, `:not-made`). It is the read's one memory of what its
  asker was told, a layer's standing and nothing of its facts, and it is
  made again at each whole answer. `softland.store.read-path/read-live`
  marks a read with it when it opens."
  [answer]
  {:told (into (zipmap (:layers answer) (repeat :yes))
               (map (fn [{:keys [layer why]}] [layer why]))
               (:left-out answer))})

(defn- told-of
  "Takes a read's mark. Gives what it holds of what the asker was told of
  each layer, a map, empty for a mark that holds nothing of it."
  [mark]
  (or (when (map? mark) (:told mark)) {}))

(defn- waiting-of
  "Takes a read's mark. Gives the layers that wait in it, a set."
  [mark]
  (or (when (map? mark) (:waiting mark)) #{}))

(defn- marked
  "Takes what the asker was told of each layer, a map, and the layers that
  wait, a set. Gives the read's mark: false when both are empty, as a read
  that was never told a whole answer is marked."
  [told waiting]
  (if (and (empty? told) (empty? waiting))
    false
    (cond-> {:told told} (seq waiting) (assoc :waiting waiting))))

(defn- only-rights-of?
  "Takes a query, a set of layers, and the sources that a step joined and
  left. Gives true when each of them that is no check is a row of her
  rights in one of those layers other than its own rows: her permission,
  her run's, or a row under a key of a permission
  (`softland.store.read-path.sources/rights-kind`). A move of a layer's own
  rows is a move of the read's list, and is never one of these."
  [query layers joined left]
  (every? (fn [source]
            (or (sources/check? query source)
                (and (contains? layers (:layer source))
                     (contains? #{:permission :run-permission :above} (sources/rights-kind query source)))))
          (concat joined left)))

(defn- unseen-layers
  "Takes what the asker was told of each layer (`told-of`) and what
  `reading` gave after a step. Gives the layers that she was told she may
  not see and that she may not see now, or of which that is not known yet.
  A change of her rights in such a layer, and the joining and the leaving
  of its rights, tell her nothing: while she may not see a layer, the
  answer says of it that it is left out, and nothing more, and an answer
  that says what it said would tell her that something of the layer was
  written (`softland.store.read-path.layers/meaning`). One whose standing
  is not known yet waits, in the mark, until it is: then she is owed the
  whole answer if she may see it, and nothing if she may not
  (`told-or-waiting`)."
  [told reading]
  (into #{}
        (keep (fn [[layer standing]]
                (when (and (= :may-not-see standing)
                           (contains? #{:may-not-see :not-known-yet} (get-in reading [:standing layer])))
                  layer)))
        told))

(defn- with-the-meaning-before
  "Takes a query, the source whose push changed, what it meant before the
  change (`meanings`), and what the asker was told of each layer. Gives the
  same map, with the source's layer as she was told it when the map holds
  nothing of it: the standing that the layer's meaning had before the
  change, for a source of her rights there. A read that the read path
  opened holds what it told of each layer; another holds nothing of it, and
  then the meaning before a change is what she was told only where each
  change is handled before the next is taken, as in one order."
  [query source meant-before told]
  (let [layer (:layer source)]
    (if (and (sources/rights-kind query source) (not (contains? told layer)))
      (if-let [standing (layers/meaning-standing meant-before)]
        (assoc told layer standing)
        told)
      told)))

(defn- whole-if-nothing-is-missing
  "Takes a query, a reading and `known`, for a read that owes its asker the
  whole answer. Gives {:tell {:anew answer} :owed? the mark of that answer}
  when nothing is missing (`mark-of`); else {:tell nil :owed? true}: the
  mark stays, and the step in which the last missing source is given gives
  the whole answer."
  [query reading known]
  (if (empty? (:missing reading))
    (let [answer (whole query reading known)]
      {:tell {:anew answer} :owed? (mark-of answer)})
    {:tell nil :owed? true}))

(defn- told-or-waiting
  "Takes a query, a reading, `known`, what the asker was told of each layer,
  the layers that wait (a set, empty for none), and the places to tell, a
  map of place-answers. Gives {:tell :owed?}: the places, and the mark.
  While something is missing the layers still wait, and the places are told
  now, since none of them waits. When nothing is missing, each layer that
  waited and is one that she may not see stops waiting, and she is told the
  places alone; a layer that waited and that she may now see, or that left
  the read, gives her the whole answer."
  [query reading known told waiting given]
  (let [tell (when (seq given) {:changed given})]
    (cond
      (empty? waiting) {:tell tell :owed? (marked told #{})}
      (seq (:missing reading)) {:tell tell :owed? (marked told waiting)}
      (every? #(= :may-not-see (get-in reading [:standing %])) waiting) {:tell tell :owed? (marked told #{})}
      :else (whole-if-nothing-is-missing query reading known))))

(defn after-change
  "What a live read does at a change of one of its pushes, once the joining
  and the leaving of the step are done and `known` is made again of the
  sources that the read still holds and that are given. Takes the query in
  its one form; what `reading` gave on that `known`; `known`; the source
  whose push changed; the places that the change names
  (`softland.store.read-path.sources/places-of`); what the source meant
  before and after
  (`meanings`; nil and nil for a source that is no rights source); the
  sources that the step joined and left; and the read's mark: true when the
  whole answer is owed; else what the asker was told of each layer and the
  layers that wait (`mark-of`), or false for a read that holds nothing of
  it. Gives {:tell :owed?}: what to hand the asker's function, nil for
  nothing, {:changed {place place-answer}} or {:anew answer}; and the mark
  after the step.

  The whole answer is owed from this step on in two cases: the source is one
  of her rights in a layer, its own rows, her permission, her run's, or a
  row above one of those (`softland.store.read-path.sources/rights-kind`),
  and what it means for this query differs; or the step joined or left a
  source that is no check, whatever source called. Neither counts when it
  is of her rights in a layer that she was told she may not see and that she
  may not see now, or of which that is not known yet (`unseen-layers`):
  nothing of such a layer is told, and one whose standing is not known yet
  waits, in the mark, until its rows are given (`after-arrival`). While the
  whole answer is owed, no single place is told: the asker gets {:anew
  answer} when nothing is missing. While a layer waits, a place that the
  change names is told when it does not wait; one that waits makes the
  whole answer owed.

  Else each place that the change names is told when `told-by?` says so,
  with what the read gives there now; a place that waits is left out, and
  is told when the source that it waits for is given.

  What the read keeps between two changes is its mark: what its asker was
  told of each layer, and nothing of the facts. This function compares no
  place with what the asker was told before: a place can so be told a
  second time with the answer that it already had."
  [query reading known source places meant-before meant-after joined left owed?]
  (if (true? owed?)
    (whole-if-nothing-is-missing query reading known)
    (let [told (with-the-meaning-before query source meant-before (told-of owed?))
          unseen (unseen-layers told reading)
          own-change? (and (not= meant-before meant-after)
                           (not (and (sources/rights-kind query source) (contains? unseen (:layer source)))))]
      (if (or own-change? (not (only-rights-of? query unseen joined left)))
        (whole-if-nothing-is-missing query reading known)
        (let [waiting (into (waiting-of owed?) (filter #(= :not-known-yet (get-in reading [:standing %]))) unseen)
              named (filter #(told-by? query reading known source %) places)
              given (of-places query reading known named)]
          (if (and (seq waiting) (< (count given) (count named)))
            (whole-if-nothing-is-missing query reading known)
            (told-or-waiting query reading known told waiting given)))))))

(defn after-arrival
  "What a live read does when a push that it came to need is given, once the
  joining and the leaving of the step are done and `known` is made again.
  Takes the query in its one form; what `reading` gave on that `known`;
  `known`; the source that was given; the sources that the step joined and
  left; and the read's mark, as `after-change` takes it. Gives {:tell
  :owed?} as `after-change` does.

  A step that joins or leaves a source that is no check owes the whole
  answer, unless each such source is of her rights in a layer that she was
  told she may not see and that she may not see now, or of which that is not
  known yet (`unseen-layers`). When it is owed, the asker gets {:anew
  answer} if nothing is missing any more, whatever kind of source arrived.
  While layers wait, the step says, once nothing is missing, whether each
  is still one that she may not see: then it stops waiting and she is told
  nothing of it; else she is given the whole answer (`told-or-waiting`).
  Else a check that arrives tells its place, with no test, unless the place
  still waits. Any other source that arrives with nothing owed tells
  nothing."
  [query reading known source joined left owed?]
  (if (true? owed?)
    (whole-if-nothing-is-missing query reading known)
    (let [told (told-of owed?)
          unseen (unseen-layers told reading)
          waiting (into (waiting-of owed?) (filter #(= :not-known-yet (get-in reading [:standing %]))) unseen)
          named (when (sources/check? query source)
                  (sources/places-of query source [(second (:path source))]))
          given (of-places query reading known named)]
      (cond
        (not (only-rights-of? query unseen joined left))
        (whole-if-nothing-is-missing query reading known)

        (seq waiting)
        (if (< (count given) (count named))
          (whole-if-nothing-is-missing query reading known)
          (told-or-waiting query reading known told waiting given))

        :else
        {:tell (when (seq given) {:changed given}) :owed? (marked told #{})}))))
