(ns softland.store.read-path.answer
  "What a read gives: the lanes put together, and what a change tells a live
  asker. Pure code.

  For each place the nearest lane that holds a fact wins, also when that
  fact says \"nothing here\" (BUILD-reads.md, B3). Every function works that
  out from what it is handed at the call: what
  `softland.store.read-path.sources/reading` gave for the read, and `known`,
  the values that selects gave or that Rama's pushes hold now. None keeps
  anything. So what a live read gives is worked out anew at each change,
  from the pushes' values, for the places that the change names.

  The steps of a live read are handed the read's mark too: true while the
  whole answer is owed, or what the last whole answer said of each lane,
  seen or left out and why, and which lanes wait (`mark-of`); nothing of any
  fact. What a live read says of a lane that the asker may not see is held
  against what she was told of it.

  In a place-answer, `:fact` is the fact id `[[lane depot uuid] index]`,
  which a writer names in `:replaces`; `:stamp` is the stamp of that fact's
  decision, which a writer carries in `:based-on`; `:time` is the time on
  the act that wrote the fact, a label. `:holds :not-a-value` says that the
  nearest row's String is the text of no value of the form. No write of a
  gate gives such a row (`softland.store.record.rows/read-value`); it is
  there so that a read never throws on what a row holds.

  One row is written by an act and by no fact of it: the use of a yes for
  one act, at the yes's place, which a read asks for only with the store's
  own facts. Its place-answer is `:nothing-here` with its lane and the stamp
  of the act that used the yes, and no `:fact`.

  What points at a thing, through lanes. A place is found through an order:
  the read looks at a place when the order by value of some lane that she
  may see holds it. At such a place the nearest lane that she may see and
  that holds a row wins, by its own order or by the check of its facts
  table, and the place is given when the winner's value is the thing. So a
  nearer fact that moves the thing, or takes it away, hides the place; a
  nearer fact that says the same thing does not, and the place comes from
  the nearer lane. What an asker must know of it: a place can come from a
  lane whose entry in `:orders` says `:not-marked`, or with a stamp that is
  not later than that lane's `:level-since`; and a place at which a nearer
  lane says the thing, and which no order of a lane that she may see holds,
  is not in the answer."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

;; ------------------------------------------------------------ one place

(defn counts?
  "False for a row under one of the store's own keys when the query did not
  ask for the store's own facts. A row that does not count is no row for
  the read: it wins no place and is in no answer. So the run of a holder's
  entity does not give her permission, and a run across entities gives no
  fact of the store's own."
  {:softland/whose-call
   [{:state :not-decided
     :what "That a tool's read leaves the store's own facts out unless it
            asks, and that any asker who may see the lane may ask, is a
            working answer, for Sid. `counts?` is the one place where the
            other answer would be made."}]}
  [query k]
  (or (boolean (:stores-own-facts query))
      (not (declaration/store-key? k))))

(defn- with-asked-parts
  "The one place where a part of a row is put in a place-answer. Rama sends
  the runtime the whole row either way; a caller is handed a part only when
  she asked."
  [query row place-answer]
  (cond-> place-answer
    (contains? (:with query) :writer)     (assoc :writer (rows/who-of row))
    (contains? (:with query) :permission) (assoc :permission (rows/permission-of row))
    (contains? (:with query) :time)       (assoc :time (:time row))))

(defn- fact-id
  "Nil for a row that no fact wrote, which names no index: the use of a yes
  for one act (`softland.store.record.rows/use-row`)."
  [lane row]
  (when (some? (:by-index row))
    [[lane (first-act/depot-at-making lane) (:by-act row)] (:by-index row)]))

(defn place-answer
  "A row that no fact wrote gives no `:fact`: it names no fact that a writer
  could replace or a reader find in its act. It never throws on a row."
  [query lane row]
  (let [[kind v] (rows/read-value row)]
    (if (= :no-fact kind)
      {:holds :no-fact}
      (with-asked-parts
        query row
        (cond-> {:holds kind :lane lane :stamp (:stamp row)}
          (fact-id lane row) (assoc :fact (fact-id lane row))
          (= :value kind) (assoc :value v))))))

(defn order-place-answer
  "The row is one of the lane's order by value, which holds no value, or a
  check's row that holds the thing. So `:value` is the thing as the asker
  gave it, in its one normal form."
  [query lane thing row]
  (with-asked-parts
    query row
    {:holds :value :value thing :lane lane :fact (fact-id lane row) :stamp (:stamp row)}))

;; ------------------------------------------- how the rows of a word are put together

(defn- known-fact
  "The lane's fact source with its value, as [source value]. It rests on a
  word of a query reading one fact source in a lane
  (`softland.store.read-path.sources/of-word`)."
  [reading known lane]
  (let [source (first (get-in reading [:by-lane lane :facts]))]
    (when (and source (contains? known source))
      [source (get known source)])))

(defn- fact-row-at
  [[source value] key-text]
  (if (= :row (first (:path source)))
    value
    (sources/row-in value key-text)))

(defn- walk-facts
  "[:waits] when the walk meets, before it stops, a lane whose standing is
  not known yet or a fact source that `known` does not hold. A source
  behind the lane that wins is not waited for."
  [query reading known [entity k]]
  (if-not (counts? query k)
    [:none]
    (loop [todo (:lanes reading)]
      (if-let [lane (first todo)]
        (case (get-in reading [:standing lane])
          :not-known-yet [:waits]
          :yes (if-let [fact (known-fact reading known lane)]
                 (if-let [row (fact-row-at fact (rows/place-key lane entity k))]
                   [:given (place-answer query lane row)]
                   (recur (rest todo)))
                 [:waits])
          (recur (rest todo)))
        [:none]))))

(defn- order-row-at
  [query reading known lane entity]
  (let [{:keys [facts since]} (get-in reading [:by-lane lane])
        order-source (first facts)
        [_ k] (:asks query)]
    (when (and order-source since)
      (sources/row-in (get known order-source)
                      (rows/by-value-key lane k since (:digest reading) entity)))))

(defn- walk-orders
  "A winner by its check gives the place only when the check's row holds the
  thing (`softland.store.record.rows/holds-this-value?`, the test by which
  a gate puts a fact in that thing's order). When that row says \"nothing
  here\" or holds another thing, the place is not given, whatever a farther
  lane holds. [:waits] as in `walk-facts`: an order or a check that the
  read wants and `known` does not hold, met before the walk stops."
  [query reading known [entity :as place]]
  (let [[_ _ thing] (:asks query)]
    (loop [todo (:lanes reading)]
      (if-let [lane (first todo)]
        (case (get-in reading [:standing lane])
          :not-known-yet [:waits]
          :yes (let [{:keys [facts checks]} (get-in reading [:by-lane lane])
                     order-source (first facts)
                     check (get checks place)]
                 (cond
                   (and order-source (not (contains? known order-source)))
                   [:waits]

                   (order-row-at query reading known lane entity)
                   [:given (order-place-answer query lane thing (order-row-at query reading known lane entity))]

                   (nil? check)
                   (recur (rest todo))

                   (not (contains? known check))
                   [:waits]

                   (nil? (get known check))
                   (recur (rest todo))

                   (rows/holds-this-value? (get known check) thing)
                   [:given (order-place-answer query lane thing (get known check))]

                   :else
                   [:none]))
          (recur (rest todo)))
        [:none]))))

(defn- places-of-runs
  [query reading known]
  (into #{}
        (for [lane (:lanes reading)
              :when (= :yes (get-in reading [:standing lane]))
              source (get-in reading [:by-lane lane :facts])
              :when (= :run (first (:path source)))
              key-text (sources/keys-of-run source (get known source))
              :let [[_ entity k] (rows/place-of-key key-text)]
              :when (and entity (counts? query k))]
          [entity k])))

(defn- places-of-orders
  [query reading known]
  (let [[_ k] (:asks query)]
    (into #{}
          (for [lane (:lanes reading)
                :when (= :yes (get-in reading [:standing lane]))
                source (get-in reading [:by-lane lane :facts])
                key-text (sources/keys-of-run source (get known source))
                :let [entity (rows/by-value-entity key-text)]
                :when (declaration/usable-id? entity)]
            [entity k]))))

(defn- holds-fact-row?
  [_query reading known lane [entity k]]
  (if-let [fact (known-fact reading known lane)]
    (some? (fact-row-at fact (rows/place-key lane entity k)))
    false))

(defn- holds-order-or-check-row?
  "A check counts only when the read wants it and `known` holds it
  (`told-by?` says why)."
  [query reading known lane [entity :as place]]
  (let [check (get-in reading [:by-lane lane :checks place])]
    (boolean (or (order-row-at query reading known lane entity)
                 (and check (some? (get known check)))))))

(def of-word
  "How the rows of each word of a query are put together. `:walk` is the
  walk of one place from the nearest lane, and gives [:given place-answer],
  [:none] or [:waits]. `:places` gives the places that the answer looks at.
  `:holds?` says whether a lane holds a row at a place now."
  {:place     {:walk walk-facts
               :places (fn [query _reading _known] (let [[_ entity k] (:asks query)] #{[entity k]}))
               :holds? holds-fact-row?
               :gives :place}
   :entity    {:walk walk-facts :places places-of-runs :holds? holds-fact-row? :gives :places}
   :run       {:walk walk-facts :places places-of-runs :holds? holds-fact-row? :gives :places}
   :points-at {:walk walk-orders :places places-of-orders :holds? holds-order-or-check-row? :gives :places}})

(defn- word-of
  [query]
  (get of-word (first (:asks query))))

;; ------------------------------------------------------------ what is given

(defn of-places
  "A place that the read does not give holds {:holds :no-fact}. A place that
  still waits is not in the map."
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
  "What must hold: nothing is missing (`reading`'s `:missing` is empty), so
  no place waits. Nothing of a lane whose standing is `:may-not-see` or
  `:not-made` is in the answer, whatever `known` holds of it."
  [query reading known]
  (let [{:keys [walk places gives]} (word-of query)
        {:keys [lanes standing by-lane]} reading
        given (fn [place] (let [[outcome place-answer] (walk query reading known place)]
                            (when (= :given outcome) place-answer)))
        answer {:lanes lanes
                :left-out (into []
                                (keep (fn [lane]
                                        (let [why (get standing lane)]
                                          (when (contains? #{:may-not-see :not-made} why)
                                            (lanes/left-out lane why)))))
                                lanes)}]
    (cond-> (if (= :place gives)
              (assoc answer :place (or (given (first (places query reading known))) {:holds :no-fact}))
              (assoc answer :places (into {}
                                          (keep (fn [place] (when-let [place-answer (given place)]
                                                              [place place-answer])))
                                          (places query reading known))))
      (= :points-at (first (:asks query)))
      (assoc :orders (into []
                           (keep (fn [lane]
                                   (when (= :yes (get standing lane))
                                     (if-let [since (get-in by-lane [lane :since])]
                                       {:lane lane :level-since since}
                                       {:lane lane :not-marked true}))))
                           lanes)))))

;; ----------------------------------------------- what a change tells a live asker

(defn told-by?
  "Whether a change of a source at a place is to be told to this asker.

  It counts a nearer lane's row only in a source that `reading` wants and
  `known` holds. So a step that has just left a check does not count it: a
  change that such a check's push has taken and not yet called back would
  never reach the read, and the place would stay with an answer that the
  tables no longer give. A nearer row that is counted is safe to rest on:
  the read still holds its source, so that source's own change is told by
  its own step."
  [query reading known source place]
  (let [lane (:lane source)
        {:keys [lanes standing]} reading
        holds? (:holds? (word-of query))]
    (boolean (and (= :yes (get standing lane))
                  (counts? query (nth place 1))
                  (not-any? (fn [nearer]
                              (and (= :yes (get standing nearer))
                                   (holds? query reading known nearer place)))
                            (take-while #(not= lane %) lanes))))))

(defn meanings
  "What a rights source meant for this query before a change and after it,
  as [before after]; nil for any other source.

  Whether she holds a permission that stands is read from `known`
  (`softland.store.read-path.sources/permission-state`), with the changed
  source's value before, and then after, in its place. So the end of a
  permission above hers, or of her run's, means another thing before and
  after, and a change of a permission row that is neither hers nor above
  hers means the same."
  [query known source old new]
  (let [lane (:lane source)
        home (lanes/home query)
        signed-in? (some? (:asker query))
        own-source (sources/own-rows-source home lane)
        meant (fn [known]
                (lanes/meaning query lane (get known own-source)
                                (sources/permission-state query home lane known)
                                signed-in?))]
    (when (sources/rights-kind query source)
      [(meant (assoc known source old))
       (meant (assoc known source new))])))

(defn- owes-for-a-move?
  "A source that is no check is joined or left only when what shapes the
  read has changed in the pushes' values."
  [query joined left]
  (boolean (some #(not (sources/check? query %)) (concat joined left))))

(defn mark-of
  "The mark of a read that was just given a whole answer: the read's one
  memory of what its asker was told, a lane's standing (`:yes`, or the word
  of `:left-out`) and nothing of its facts. It is made again at each whole
  answer."
  [answer]
  {:told (into (zipmap (:lanes answer) (repeat :yes))
               (map (fn [{:keys [lane why]}] [lane why]))
               (:left-out answer))})

(defn- told-of
  [mark]
  (or (when (map? mark) (:told mark)) {}))

(defn- waiting-of
  [mark]
  (or (when (map? mark) (:waiting mark)) #{}))

(defn- marked
  "False when both are empty, as a read that was never told a whole answer
  is marked."
  [told waiting]
  (if (and (empty? told) (empty? waiting))
    false
    (cond-> {:told told} (seq waiting) (assoc :waiting waiting))))

(defn- only-rights-of?
  "True when each source that the step joined or left, and that is no check,
  is a row of her rights in one of those lanes other than its own rows. A
  move of a lane's own rows is a move of the read's list, and is never one
  of these."
  [query lanes joined left]
  (every? (fn [source]
            (or (sources/check? query source)
                (and (contains? lanes (:lane source))
                     (contains? #{:permission :run-permission :above} (sources/rights-kind query source)))))
          (concat joined left)))

(defn- unseen-lanes
  "The lanes that she was told she may not see and that she may not see now,
  or of which that is not known yet.

  A change of her rights in such a lane, and the joining and the leaving of
  its rights, tell her nothing: while she may not see a lane, the answer
  says of it that it is left out, and nothing more, and an answer that says
  what it said would tell her that something of the lane was written
  (`softland.store.read-path.lanes/meaning`). One whose standing is not
  known yet waits, in the mark, until it is: then she is owed the whole
  answer if she may see it, and nothing if she may not (`told-or-waiting`)."
  [told reading]
  (into #{}
        (keep (fn [[lane standing]]
                (when (and (= :may-not-see standing)
                           (contains? #{:may-not-see :not-known-yet} (get-in reading [:standing lane])))
                  lane)))
        told))

(defn- with-the-meaning-before
  "For a mark that holds nothing of the source's lane, what she was told of
  it is taken from the meaning that the lane had before the change. A read
  that the read path opened holds what it told of each lane. Another holds
  nothing of it, and then the meaning before a change is what she was told
  only where each change is handled before the next is taken, as in one
  order."
  [query source meant-before told]
  (let [lane (:lane source)]
    (if (and (sources/rights-kind query source) (not (contains? told lane)))
      (if-let [standing (lanes/meaning-standing meant-before)]
        (assoc told lane standing)
        told)
      told)))

(defn- whole-if-nothing-is-missing
  "While something is missing the mark stays true, and the step in which the
  last missing source is given gives the whole answer."
  [query reading known]
  (if (empty? (:missing reading))
    (let [answer (whole query reading known)]
      {:tell {:anew answer} :owed? (mark-of answer)})
    {:tell nil :owed? true}))

(defn- told-or-waiting
  "While something is missing the lanes still wait, and the places are told
  now, since none of them waits. When nothing is missing, each lane that
  waited and is one that she may not see stops waiting, and she is told the
  places alone; a lane that waited and that she may now see, or that left
  the read, gives her the whole answer."
  [query reading known told waiting given]
  (let [tell (when (seq given) {:changed given})]
    (cond
      (empty? waiting) {:tell tell :owed? (marked told #{})}
      (seq (:missing reading)) {:tell tell :owed? (marked told waiting)}
      (every? #(= :may-not-see (get-in reading [:standing %])) waiting) {:tell tell :owed? (marked told #{})}
      :else (whole-if-nothing-is-missing query reading known))))

(defn after-change
  "What a live read does at a change of one of its pushes. What must hold:
  the joining and the leaving of the step are done, and `known` is made
  again of the sources that the read still holds and that are given;
  `reading` is what was given on that `known`. `places` are what the change
  names (`softland.store.read-path.sources/places-of`). `meant-before` and
  `meant-after` are what `meanings` gave. `owed?` is the read's mark: true
  when the whole answer is owed; else what the asker was told of each lane
  and the lanes that wait (`mark-of`), or false for a read that holds
  nothing of it. Gives {:tell :owed?}: what to hand the asker's function,
  nil for nothing, {:changed {place place-answer}} or {:anew answer}; and
  the mark after the step.

  The whole answer is owed from this step on in two cases: the source is one
  of her rights in a lane (`softland.store.read-path.sources/rights-kind`)
  and what it means for this query differs; or the step joined or left a
  source that is no check, whatever source called. Neither counts when it is
  of her rights in one of `unseen-lanes`: nothing of such a lane is told.
  While the whole answer is owed, no single place is told. While a lane
  waits, a place that the change names and that waits makes the whole answer
  owed.

  Else a place that waits is left out, and is told when the source that it
  waits for is given.

  No place is compared with what the asker was told before: a place can so
  be told a second time with the answer that it already had."
  [query reading known source places meant-before meant-after joined left owed?]
  (if (true? owed?)
    (whole-if-nothing-is-missing query reading known)
    (let [told (with-the-meaning-before query source meant-before (told-of owed?))
          unseen (unseen-lanes told reading)
          own-change? (and (not= meant-before meant-after)
                           (not (and (sources/rights-kind query source) (contains? unseen (:lane source)))))]
      (if (or own-change? (not (only-rights-of? query unseen joined left)))
        (whole-if-nothing-is-missing query reading known)
        (let [waiting (into (waiting-of owed?) (filter #(= :not-known-yet (get-in reading [:standing %]))) unseen)
              named (filter #(told-by? query reading known source %) places)
              given (of-places query reading known named)]
          (if (and (seq waiting) (< (count given) (count named)))
            (whole-if-nothing-is-missing query reading known)
            (told-or-waiting query reading known told waiting given)))))))

(defn after-arrival
  "What a live read does when a push that it came to need is given. It takes
  the read's mark and gives {:tell :owed?} as `after-change` does, and the
  same must hold.

  When the whole answer is owed, the asker gets it if nothing is missing any
  more, whatever kind of source arrived. Else a check that arrives tells its
  place, with no test, unless the place still waits. Any other source that
  arrives with nothing owed tells nothing."
  [query reading known source joined left owed?]
  (if (true? owed?)
    (whole-if-nothing-is-missing query reading known)
    (let [told (told-of owed?)
          unseen (unseen-lanes told reading)
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
