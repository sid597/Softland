(ns gate-histories.live
  "A live read through the store's own read path, in memory: what an asker
  who keeps a read open is told while a history's decisions are written.
  Pure code: no cluster.

  It plays the read path's own pure steps (`softland.store.read-path.sources`,
  `softland.store.read-path.answer`) in one order: after a decision, each
  push whose value changed calls back, one after the other in the order of
  their sources; the read settles what it holds (`sources/reading`,
  `sources/moves`) and tells what `answer/after-change` gives; each push
  that the step joined is served from the record at once and arrives
  (`answer/after-arrival`). That is one of the orders that the read path's
  pure proof plays (`softland.store.read-path-pure-proof`, `every-order`),
  the one in which each push is served and each callback handled as soon as
  it can be. What it does not play: any other order, and Rama's pushes.

  A read is a map:

    :query   the query in its one form
    :held    the sources that the read holds
    :known   each held source whose push is given, with its value
    :asked   the held sources whose push is asked for and not given yet
    :owed?   whether the whole answer is owed
    :screen  what the asker's function was shown last

  `tables` makes the read path's tables of a record of `gate-histories.walk`.
  `open` opens a read on tables. `after-writes` takes a read and the tables
  after a decision, and gives the read after and what it told the asker.
  `once` gives what a read asked once gives from tables."
  (:require [softland.store.read-path.answer :as answer]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.record.rows :as rows]))

(def ^:private numbers "The read path's numbers, at their defaults." (query/numbers nil))

(defn- by-code-point
  "The order of the keys of a table, as Rama keeps String keys
  (`softland.store.record.rows/before?`)."
  [a b]
  (cond (= a b) 0 (rows/before? a b) -1 :else 1))

(defn tables
  "The tables that the read path reads, made of a record of the walk: its
  facts and its facts by value, each in the order of its keys, under the
  gate's depot word. Takes the record and the gate's depot word."
  [rec gate]
  {[gate :facts] (into (sorted-map-by by-code-point) (:facts rec))
   [gate :facts-by-value] (into (sorted-map-by by-code-point) (:by-value rec))})

(defn- value-of
  "What a push of a source holds on these tables: the row, or nil, for a
  source of one row; the rows of the run for a run."
  [tables source]
  (let [t (get tables [(:gate source) (:table source)] (sorted-map-by by-code-point))
        [kind a b] (:path source)]
    (if (= :row kind)
      (get t a)
      (into (sorted-map-by by-code-point) (subseq t >= a < b)))))

(defn check
  "The query in its one form (`softland.store.read-path.query/check`).
  Throws for a query that the read path refuses: a history asks only what a
  read may ask."
  [query-map]
  (let [checked (query/check query-map)]
    (or (:ok checked)
        (throw (ex-info "the read path refuses the query" {:query query-map :got checked})))))

(defn once
  "What a read asked once gives on these tables, as the read path asks it:
  what is wanted, the missing sources fetched, again, nine rounds at most.
  Takes the tables and a query in its one form. Gives the answer, or
  `:did-not-settle`."
  [tables q]
  (loop [round 0 known {}]
    (let [reading (sources/reading q known :once numbers)
          missing (:missing reading)]
      (cond (empty? missing) (answer/whole q reading known)
            (< 8 round) :did-not-settle
            :else (recur (inc round) (into known (map (fn [s] [s (value-of tables s)])) missing))))))

(defn open
  "Opens a live read on tables, as `softland.store.read-path/read-live`
  does: what is wanted is joined and served at once from the tables, again,
  until nothing more is wanted; the first answer is the screen. Takes the
  tables and a query in its one form. Gives the read."
  [tables q]
  (loop [held #{} known {} round 0]
    (let [reading (sources/reading q known :live numbers held)
          {:keys [join leave]} (sources/moves reading held)]
      (if (or (and (empty? join) (empty? leave)) (< 8 round))
        {:query q :held held :known known :asked #{} :owed? false
         :screen (answer/whole q reading known)}
        (recur (into (reduce disj held leave) join)
               (into (reduce dissoc known leave) (map (fn [s] [s (value-of tables s)])) join)
               (inc round))))))

(defn- settle
  "The first line of a step, as `softland.store.read-path/settle!` makes it:
  what the read wants on what it knows now; join what is wanted and not
  held, as a push that is asked for and not given; leave what is held and
  no longer wanted; again. Gives [the read, {:reading :joined :left}]."
  [read]
  (loop [read read joined #{} left #{} round 0]
    (let [reading (sources/reading (:query read) (:known read) :live numbers (:held read))
          {:keys [join leave]} (sources/moves reading (:held read))]
      (if (or (and (empty? join) (empty? leave)) (< 8 round))
        [read {:reading reading :joined joined :left left}]
        (recur (-> read
                   (update :held #(into (reduce disj % leave) join))
                   (update :known #(reduce dissoc % leave))
                   (update :asked #(into (reduce disj % leave) join)))
               (into joined join)
               (into left leave)
               (inc round))))))

(defn- shown
  "The screen after a tell, as an asker's function keeps it: a whole answer
  in the place of all; or each changed place, where a place that the read
  no longer gives leaves a map of places."
  [screen tell]
  (cond
    (nil? tell) screen
    (:anew tell) (:anew tell)
    :else (reduce (fn [screen [place place-answer]]
                    (cond
                      (contains? screen :place) (assoc screen :place place-answer)
                      (= :no-fact (:holds place-answer)) (update screen :places dissoc place)
                      :else (assoc-in screen [:places place] place-answer)))
                  screen
                  (:changed tell))))

(defn- with-step
  "The read and its tells after one step of the read path, which gave
  {:tell :owed?}."
  [[read tells] {t :tell owed? :owed?}]
  [(-> read (assoc :owed? owed?) (update :screen shown t))
   (cond-> tells t (conj t))])

(defn- arrive-all
  "Each push that the read asked for is served from the tables and arrives,
  one after the other in the order of their sources, as
  `softland.store.read-path` handles an arrival."
  [[read tells] tables]
  (if-let [source (first (sort-by pr-str (:asked read)))]
    (let [read (update read :asked disj source)]
      (if-not (contains? (:held read) source)
        (recur [read tells] tables)
        (let [read (assoc-in read [:known source] (value-of tables source))
              [read {:keys [reading joined left]}] (settle read)
              step (answer/after-arrival (:query read) reading (:known read) source joined left (:owed? read))]
          (recur (with-step [read tells] step) tables))))
    [read tells]))

(defn after-writes
  "What a live read does when a decision is written: each push whose value
  changed calls back, one after the other in the order of their sources,
  with its value before and after, as `softland.store.read-path` handles a
  callback; and each push that a step joined is served and arrives. Takes
  the read and the tables after the decision. Gives [the read after, the
  tells that the asker's function got, in order]: each {:anew answer} or
  {:changed places}."
  [read tables]
  (reduce
   (fn [[read tells] source]
     (if-not (contains? (:known read) source)
       [read tells]
       (let [old (get (:known read) source)
             new (value-of tables source)
             names (sources/named source :all old new)
             read (assoc-in read [:known source] new)]
         (if (empty? names)
           [read tells]
           (let [q (:query read)
                 [read {:keys [reading joined left]}] (settle read)
                 [meant-before meant-after] (answer/meanings q (:known read) source old new)
                 step (answer/after-change q reading (:known read) source
                                           (sources/places-of q source names)
                                           meant-before meant-after joined left (:owed? read))]
             (arrive-all (with-step [read tells] step) tables))))))
   [read []]
   (sort-by pr-str (filter #(not= (get (:known read) %) (value-of tables %)) (keys (:known read))))))
