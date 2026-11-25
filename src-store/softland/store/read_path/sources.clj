(ns softland.store.read-path.sources
  "The sources of a read: each thing that the read path asks of Rama, as
  data, and the one function that says which of them a read wants next.
  Pure code: it makes no path of Rama's and calls nothing.

  A source is one value that Rama gives and can push: one row, or one run
  of neighbouring rows, of one table on one task. Its `:path` is
  `[:row key]`, `[:own-rows from to]` or `[:run from to]`, `to` not in the
  run. Its `:lane` adds nothing to what makes two sources equal: every key
  of the source begins with that lane's text. Its `:routing-id` is what
  Rama's client is given as its partitioning key; it is called a routing
  id so that \"key\" stays a fact's key.

  Where a lane is read. A streaming lane has every row on its own task,
  so each source of it has the lane as its routing id. A microbatch lane
  has every row of its facts, and of its facts by value, on every task, the
  same rows everywhere; so a read of it needs one task, any one. It is read
  on the task of the read's home, the lane that the read starts from
  (`softland.store.read-path.lanes/home`): each source of a microbatch
  lane has the home as its routing id, whatever it reads. So an asker reads
  the base where her own lane lives, and a read that starts from a
  microbatch lane reads it on that lane's own task. Every source is one
  source: no run has a part on each task.

  Two askers look at the same thing when their sources are equal maps; that
  map is the key by which a push is shared. In a streaming lane those are
  all the askers of one row or run. In a microbatch lane they are the
  askers of that row or run whose reads have the same home: two reads with
  two homes read the same rows on the tasks of their homes, and each holds
  a push of its own. Which parts of a row an asker asked for, and whether
  she asked for the store's own facts, are no part of a source. A check of
  a nearer lane at a place and a read of that place are one source.

  `known` is a map from a source to the value that a select gave or that a
  push holds: a row or nil for a source of one row, a map from key to row
  for the two kinds of run. A source that `known` does not hold is not read
  yet; a source that it holds with nil is read, and there is no row.

  How many rounds a read takes. A source is wanted as a function of the
  query and of the values of other sources that the read holds
  (`reading`), never of its own value. So a loop that fetches what is
  missing and asks `reading` again settles in as many rounds as the
  longest chain of sources in which each is wanted on the value of the one
  before. In one lane of a read the chain is:

    rights   the lane's own rows; her permission and her run's, once the
             own rows say that the lane is made and open to its members,
             or for a read asked once in the round of the own rows; and
             the rows above whichever of the two holds a given permission
             of hers, one round after that row, all at once, since the row
             names each (`chain-sources`)
    facts    once she may see the lane; for a read asked once, a place and
             the first read of a run in the round of the own rows
             (`before-standing?`). The order by value waits for the own
             rows, which say whether the key is marked
    checks   one round after the orders of the farther lanes
             (`nearer-checks`)

  A read has two ranks of lanes at most: the lanes of `:lanes`, or the
  start lane of `:from-lane`; and then the lanes of the start lane's
  list, which is known only once she may see the start lane, so that the
  rights of those lanes begin one round after the start lane's last.

  So, counted as rounds that fetch something, at most: asked once, the
  rights of a rank take 2 rounds, the orders 1 and the checks 1; a given
  list settles in 4 rounds, a start lane and its list in 6. Kept live, the
  rights of a rank take 3 rounds, the facts 1 and the checks 1; a given
  list opens in 5 rounds, a start lane and its list in 8. A read asked
  once fetches each source once and keeps its value, so nothing that is
  written while it reads makes its chain longer: it settles within 6
  rounds whatever is written. A live read takes the values that its pushes
  hold, so a write while it opens can make a part of its chain wanted
  anew: a list that gains a lane, 5 rounds more for that lane. The bound
  of each loop is set from these numbers (`softland.store.read-path`,
  `max-rounds`).

  What must hold. The query passed
  `softland.store.read-path.query/check`: every key text is made by
  `softland.store.record.rows` of ids that were tested first."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.record.rows :as rows]))

;; -------------------------------------------------------------- the sources

(defn- gate-of
  "The gate whose tables hold the lane's record: the depot that the lane
  starts with. The read path looks in that record and no other, as
  `depot-of-lane` does for a lane that is not made."
  [lane]
  (first-act/depot-at-making lane))

(defn- source-of
  "The one place where a source gets its routing id. What must hold: the
  home is the home of the read that the source is for
  (`softland.store.read-path.lanes/home`)."
  [home lane table path]
  (let [gate (gate-of lane)]
    {:gate gate :table table :lane lane
     :routing-id (if (= :microbatch gate) home lane)
     :path path}))

(defn own-rows-source
  [home lane]
  (let [[from to] (rows/own-rows-run lane)]
    (source-of home lane :facts [:own-rows from to])))

(defn permission-source
  [home lane asker]
  (source-of home lane :facts [:row (rows/place-key lane asker :softland.store/permission)]))

(defn run-permission-source
  [home lane session]
  (source-of home lane :facts [:row (rows/place-key lane session :softland.store/run-permission)]))

(defn chain-sources
  "The sources of the rows above a permission of the asker's: her permission
  row, or the row of her run's permission. A permission stands only while
  each of those rows holds the permission above, with an older stamp, so a
  read keeps them as it keeps the row itself: the end of one above her is a
  change of one of them.

  Why a source above names her row's stamp, as `:read-after`. Rama tells
  each push by itself, and nothing lines two pushes up: a live read can
  hold her row as one decision wrote it and a row above as it stood before
  that decision. One act can give her a permission beneath its own and end
  its own, and such a pair of rows then says that a permission stands which
  stood at no stamp. A source that names her row's stamp is a source of its
  own: it is wanted only once her row is known, so it is read, or its push
  is opened, after that row was seen, and its value holds its place as of
  her row's stamp or later. Then a row above that holds its permission,
  with a stamp older than the row beneath it, held it at her row's stamp
  too, since no decision wrote that place between the two; so a permission
  that these rows say stands, stood at her row's stamp
  (`softland.store.gate.chain`, the rule of two rows). A row that changes
  gives a stamp of its own, and the read joins the rows above it again,
  afresh.

  What it costs: askers share the push on a row above only when their own
  rows were written by one decision, so a live read holds a push of its own
  on each row above hers; a read asked once reads them in the round after
  her row, which it needs in any case, since her row names them.

  What must hold: the row's permission is hers; the caller tests a run's
  row first (`askers-run-row`)."
  [home lane permission-row]
  (if-let [held (when-not (= :not-known-yet permission-row) (rows/held-permission permission-row))]
    (let [after (:stamp permission-row)]
      (mapv (fn [k] (assoc (source-of home lane :facts [:row k]) :read-after after))
            (chain/places-above lane held)))
    []))

(defn askers-run-row
  "The run's row when it holds a permission whose holder is the asker, and
  nil for any other row: a run's permission is its holder's, and one that
  another holder holds for a run of the same id lets the asker see nothing
  (`softland.store.read-path.lanes/run-permission-state`). So the rows above
  another holder's permission are never wanted: a read that held them would
  join and leave pushes, and owe its asker a whole answer, at a give or an
  end that tells her nothing (`softland.store.read-path.answer/after-change`)."
  [run-row asker]
  (when-let [held (when-not (= :not-known-yet run-row) (rows/held-permission run-row))]
    (when (= asker (nth held 0)) run-row)))

(defn chain-rows
  [home lane permission-row known]
  (let [above (chain-sources home lane permission-row)]
    (if (every? #(contains? known %) above)
      (into {} (map (fn [source] [(second (:path source)) (get known source)])) above)
      :not-known-yet)))

(defn permission-state
  "The one place where the rows of a read's rights are taken out of `known`."
  [query home lane known]
  (let [asker (:asker query)
        session (:session query)
        row-of (fn [source] (if (contains? known source) (get known source) :not-known-yet))]
    (if (nil? asker)
      :none
      (let [own-row (row-of (permission-source home lane asker))
            for-the-lane (lanes/permission-state lane own-row (chain-rows home lane own-row known))
            for-the-run (if session
                          (let [run-row (row-of (run-permission-source home lane session))]
                            (lanes/run-permission-state lane asker run-row
                                                         (chain-rows home lane (askers-run-row run-row asker) known)))
                          :none)]
        (lanes/either for-the-lane for-the-run)))))

(defn place-source
  [home lane entity k]
  (source-of home lane :facts [:row (rows/place-key lane entity k)]))

(defn check-source
  "The check of a nearer lane at a place is the source of the place itself,
  so a check and a read of the place share one push."
  [home lane entity k]
  (place-source home lane entity k))

(defn run-source
  "One source, whether the two ends name one entity or the run goes across
  entities, in a streaming lane and in a microbatch lane alike: the task
  that serves it holds the whole run."
  [home lane from to]
  (source-of home lane :facts [:run from to]))

(defn by-value-source
  [home lane k since thing]
  (let [[from to] (rows/by-value-run lane k since thing)]
    (source-of home lane :facts-by-value [:run from to])))

(def of-word
  "What each word of a query reads in one lane: a function of the query, a
  lane and the lane's own rows (nil when not known), which gives the fact
  sources. Each word of this build reads one source in a lane, or none.
  Whether a source of it is wanted is `reading`'s."
  {:place
   (fn [query lane _own-rows]
     (let [[_ entity k] (:asks query)]
       [(place-source (lanes/home query) lane entity k)]))

   :entity
   (fn [query lane _own-rows]
     (let [[_ entity] (:asks query)
           [from to] (rows/entity-run lane entity)]
       [(run-source (lanes/home query) lane from to)]))

   :run
   (fn [query lane _own-rows]
     (let [[_ first-end last-end] (:asks query)]
       (if-let [[from to] (rows/run-of-ends lane first-end last-end)]
         [(run-source (lanes/home query) lane from to)]
         [])))

   :points-at
   (fn [query lane own-rows]
     (let [[_ k thing] (:asks query)]
       (if-let [since (lanes/marked-since own-rows lane k)]
         [(by-value-source (lanes/home query) lane k since thing)]
         [])))})

(defn outside-the-agreed-range?
  "True for a text that holds a UTF-16 unit from U+D800 on: a letter outside
  the first plane, or one from U+E000 on. Only for such a text can Java's
  order of Strings and the order by code point give two answers. The table
  keeps its keys by code point. The map that Rama's client makes of a run
  was seen to look a key up by the other order (`row-in`); and runs between
  two ends that hold such a unit were seen to give fewer places than the
  table holds, for a cause that this build did not find
  (`first-read-holds?`, `keys-of-run`)."
  [^String k]
  (let [n (.length k)]
    (loop [i 0]
      (cond (>= i n) false
            (>= (int (.charAt k i)) 0xD800) true
            :else (recur (inc i))))))

(defn first-read-holds?
  "The first read of a run walks a window from the run's start on the task
  and then cuts that window at the run's end. The table keeps its keys by
  code point. For two ends that hold no unit from U+D800 on, that order and
  Java's order of Strings agree on every key, so the cut is right whichever
  of the two Rama uses. For an end that holds one, the two orders can
  differ, and this build did not find out which of them Rama uses for the
  cut. So such a run has no first read: it is read by its size and then
  whole.

  What was seen, on the store's own table, in runs of the read path's
  proof: with the first read, a run between two entities whose ids hold
  such letters gave no place where the table holds two; without it, the
  same run gave both. A probe of the build then asked the same two paths of
  Rama by hand and got the right keys from both. The two runs differ in
  more than this one thing, so that the cut was at fault is not shown; the
  choice here is the one that does not rest on it."
  [source]
  (let [[kind from to] (:path source)]
    (and (= :run kind)
         (not (outside-the-agreed-range? from))
         (not (outside-the-agreed-range? to)))))

(defn before-standing?
  "True when the source is read before the read knows whether the asker may
  see its lane: for a read asked once, a place, or the first read of a run
  of the facts table. The rights and the fact then go in one call for a
  lane, so a place through k lanes is k round trips in one round, where
  rights first would be 2k in two. What is read early is bounded, one row
  or the first places of a run, and nothing of it is handed over: an answer
  gives nothing of a lane that the asker may not see.

  A run that has no first read (`first-read-holds?`) is read only when she
  may see the lane, since its size would be read. A live read opens no push
  early, because an open push costs the lane's gate at each write."
  [mode source]
  (and (= :once mode)
       (= :facts (:table source))
       (or (= :row (first (:path source)))
           (first-read-holds? source))))

(defn rights-kind
  "A source can be her permission row and the place that she asks for at
  once. `:above` says only that the row could be one of the rows above a
  permission of hers: whether it is, that permission's own row says
  (`chain-sources`)."
  [query source]
  (let [[kind k] (:path source)
        lane (:lane source)]
    (cond
      (= :own-rows kind) :own-rows
      (not (and (= :row kind) (= :facts (:table source)) (some? (:asker query)))) nil
      (= k (rows/place-key lane (:asker query) :softland.store/permission)) :permission
      (and (some? (:session query))
           (= k (rows/place-key lane (:session query) :softland.store/run-permission))) :run-permission
      (contains? declaration/permission-keys (nth (rows/place-of-key k) 2 nil)) :above
      :else nil)))

(defn check?
  "Only a query of what points at a thing has checks, and the asked key is
  never one of the store's own, so a permission's row is never a check."
  [query source]
  (let [[word k] (:asks query)
        [kind key-text] (:path source)]
    (boolean (and (= :points-at word)
                  (= :row kind)
                  (= :facts (:table source))
                  (= k (nth (rows/place-of-key key-text) 2 nil))))))

(defn keys-of-run
  "The keys of a run's value that are keys of the run, in the table's order
  (`softland.store.record.rows/before?`).

  For a run whose two ends hold no UTF-16 unit from U+D800 on, those are
  all the keys, and none is tested: Rama's range and the table's order
  agree there. For an end that holds one, a probe of this build asked the
  store's own table for a range and was given a key that the order by code
  point puts after the range's end. So each key of such a value is held to
  the two ends here, and a key outside them is dropped. Such an end is the
  end of one entity, of one lane or of one value's order, whose own keys
  all begin with the text that the two ends share, so no key of the run
  itself is lost; a run between two entities that a caller names is turned
  away before it is read, when an end of it holds such a unit
  (`softland.store.read-path.query`)."
  [source run]
  (let [[_ from to] (:path source)]
    (if (or (outside-the-agreed-range? from) (outside-the-agreed-range? to))
      (filter (fn [k] (and (not (rows/before? k from)) (rows/before? k to))) (keys run))
      (keys run))))

(defn row-in
  "A plain lookup is not enough for every key. The table keeps its keys by
  code point, and so does the run that Rama sends; but the map that Rama's
  client makes of the run finds a key by Java's order of Strings, which is
  by UTF-16 unit. The two orders agree for every key that holds no unit
  from U+D800 on, and such a key is always found. For a key that holds one,
  a lookup can miss a row that is there; so when the lookup gives nothing
  for such a key, the run is gone through, row by row. Read from a throw of
  Rama's client in a run of this build, which named the function that
  searches; the read path's proof against the module reads such keys."
  [run k]
  (when run
    (or (get run k)
        (when (outside-the-agreed-range? k)
          (some (fn [[key-text row]] (when (= k key-text) row)) run)))))

(defn most-checks
  "For a read asked once a check is one row of one call. For a read that is
  kept live a check is one push, and Rama bounds the pushes on one table on
  one task."
  [mode numbers]
  (if (= :live mode)
    (:max-checks-of-one-live-read numbers)
    (:max-checks-of-one-read numbers)))

(defn nearer-checks
  "The checks that a read of what points at a thing wants in nearer lanes: a
  map from a lane to a map from a place, `[entity key]`, to the check's
  source; `:too-many` when they are more than `most`. For each place that
  the order of some lane of standing `:yes` holds, one check in each lane
  of standing `:yes` that is nearer than the nearest lane whose order holds
  the place. A lane behind that one cannot win the place, so nothing is
  read there.

  The check reads the facts table of the nearer lane, marked or not: the
  row that it finds wins the place, and the place is given when that row
  holds the thing (`softland.store.read-path.answer`). It is the only
  place where a source depends on the rows of a run.

  An entity comes from a row's key and not from a caller, so it is tested
  here and one that is no id within the limit is left out. `by-lane` is
  `reading`'s `:by-lane`; `most` is what `most-checks` gives."
  [query lanes standing by-lane known most]
  (let [[word k] (:asks query)
        home (lanes/home query)]
    (if (not= :points-at word)
      {}
      (loop [todo (filter #(= :yes (get standing %)) lanes)
             nearer []
             won #{}
             checks {}
             n 0]
        (if-let [lane (first todo)]
          (let [order-source (first (get-in by-lane [lane :facts]))
                order (when order-source (get known order-source))
                fresh (into []
                            (comp (keep rows/by-value-entity)
                                  (filter declaration/usable-id?)
                                  (remove won))
                            (when order (keys-of-run order-source order)))
                n (+ n (* (count fresh) (count nearer)))]
            (if (> n most)
              :too-many
              (recur (rest todo)
                     (conj nearer lane)
                     (into won fresh)
                     (reduce (fn [checks near]
                               (assoc checks near
                                      (into (get checks near {})
                                            (map (fn [entity] [[entity k] (check-source home near entity k)]))
                                            fresh)))
                             checks
                             (when (seq fresh) nearer))
                     n)))
          checks)))))

;; -------------------------------------------------------- what a read wants

(defn reading
  "What a read wants now. `mode` is `:once` or `:live`; `held`, for a live
  read, is the set of the sources that the read holds, given or not. While
  the list of a start lane cannot be said yet, `:lanes` is the start lane
  alone. With `:too-large` no check is wanted.

  Her permission in a lane, and her run's, are wanted when she is signed
  in. While the lane's own rows are not known: only for a read asked once,
  so that they ride the call of the own rows. When they are known: only
  when the lane is made and its setting is `:members`. Under \"everyone\"
  her permission shapes nothing, and a lane that is not made holds none.
  The rows above her run's are wanted only when its row holds a permission
  of hers (`askers-run-row`).

  A place or a run in a lane is wanted when her standing is `:yes`; an
  order by value, when the lane's own rows mark the key too. A lane that is
  `:not-made` wants its own rows alone. While her standing is
  `:not-known-yet`: for a read asked once, by `before-standing?`; for a
  live read, when the read holds the source already, so that a push is not
  closed and opened again while her permission is read.

  A lane whose standing is `:may-not-see` wants nothing but its own rows,
  her permission, her run's, and the rows above whichever of the two holds
  a permission of hers: those are what could let it in later. The start
  lane of `:from-lane` is in the list from the first round, before its own
  rows are known, so its permission and its fact source go with its own
  rows.

  Limit. It reads no size of a run: a run that is too large is found where
  it is fetched (`softland.store.read-path`)."
  ([query known mode numbers]
   (reading query known mode numbers nil))
  ([query known mode numbers held]
   (let [asker (:asker query)
         home (lanes/home query)
         signed-in? (some? asker)
         [word k thing] (:asks query)
         facts-of (get of-word word)
         session (:session query)
         rights (fn [lane]
                  (let [own (own-rows-source home lane)
                        own-rows (get known own)
                        row-of (fn [source] (if (and source (contains? known source))
                                              (get known source)
                                              :not-known-yet))
                        permission (when signed-in? (permission-source home lane asker))
                        run-permission (when (and signed-in? session) (run-permission-source home lane session))
                        wanted? (and signed-in?
                                     (if (nil? own-rows)
                                       (= :once mode)
                                       (and (lanes/made? own-rows lane)
                                            (= :members (lanes/who-may-see own-rows lane)))))
                        standing (lanes/standing lane own-rows
                                                  (permission-state query home lane known)
                                                  signed-in?)]
                    {:own own
                     :own-rows own-rows
                     :permission (when wanted? permission)
                     :run-permission (when wanted? run-permission)
                     :chain (if wanted?
                              (into (chain-sources home lane (row-of permission))
                                    (when run-permission
                                      (chain-sources home lane (askers-run-row (row-of run-permission) asker))))
                              [])
                     :standing standing}))
         start (:from-lane query)
         start-rights (when start (rights start))
         listed (lanes/in-order query (:own-rows start-rights) (:standing start-rights))
         lane-list (or listed [start])
         with-facts (fn [lane {:keys [own-rows standing] :as told}]
                      (let [wanted? (fn [source]
                                      (case standing
                                        :yes true
                                        :not-known-yet (if (= :once mode)
                                                         (before-standing? mode source)
                                                         (contains? held source))
                                        false))]
                        (cond-> (assoc told :facts (filterv wanted? (facts-of query lane own-rows)))
                          (= :points-at word) (assoc :since (lanes/marked-since own-rows lane k)))))
         by-lane (into {}
                        (map (fn [lane]
                               [lane (with-facts lane (if (= lane start) start-rights (rights lane)))]))
                        lane-list)
         standing (into {} (map (fn [[lane told]] [lane (:standing told)])) by-lane)
         checks (nearer-checks query lane-list standing by-lane known (most-checks mode numbers))
         too-many? (= :too-many checks)
         by-lane (if too-many?
                    by-lane
                    (reduce (fn [m [lane at]] (assoc-in m [lane :checks] at)) by-lane checks))
         wanted (into #{}
                      (mapcat (fn [{:keys [own permission run-permission chain facts checks]}]
                                (concat [own] (when permission [permission])
                                        (when run-permission [run-permission]) chain facts (vals checks))))
                      (vals by-lane))]
     (cond-> {:lanes      lane-list
              :list-known? (some? listed)
              :standing    standing
              :by-lane    (into {} (map (fn [[lane told]] [lane (dissoc told :own-rows :standing)])) by-lane)
              :wanted      wanted
              :missing     (into #{} (remove #(contains? known %)) wanted)
              :too-large   (when too-many?
                             {:what :checks-of-one-read :most (most-checks mode numbers)})}
       (= :points-at word) (assoc :digest (rows/value-digest-of thing))))))

(defn moves
  [reading held]
  (let [wanted (:wanted reading)]
    {:join  (into #{} (remove #(contains? held %)) wanted)
     :leave (into #{} (remove #(contains? wanted %)) held)}))

;; ------------------------------------------------- a read that is asked once

(defn read-of
  "What a read asked once reads for a source. Such a read fetches a source
  in a round after the rows that make it wanted, so a row above her
  permission is read after her own row whatever stamp it names, and two
  sources that differ in that stamp alone are one read: one path of one
  call. A live read keeps them apart, each a push of its own, opened after
  the row beneath it was seen (`chain-sources`)."
  [source]
  (dissoc source :read-after))

(defn calls-for-once
  "The sources of one read (`read-of`) are in one call and are read once. So
  a lane's rights and its fact are one round trip. Every source of a
  microbatch lane in one read has the read's home as its routing id, so
  what one read reads of such lanes is one call for their facts and one
  for their facts by value."
  [missing numbers]
  (let [most (:max-paths-in-one-call numbers)]
    (vec (for [[[gate table routing-id] sources] (group-by (juxt :gate :table :routing-id) missing)
               part (partition-all most (vals (group-by read-of sources)))]
           {:gate gate :table table :routing-id routing-id :sources (vec (apply concat part))}))))

(defn run-is-whole?
  "What must hold: the first read asked for `:run-start-places` rows and
  one more."
  [rows-or-count numbers]
  (<= (if (number? rows-or-count) rows-or-count (count rows-or-count))
      (:run-start-places numbers)))

(defn over-the-limit?
  "`size` is what a walk of `:max-places-of-one-run` and one rows from the
  run's start gave: the count of the rows walked and the key of the last
  of them, or the count alone when the walk holds nothing. A walk has no
  end of its own and runs on into the keys after the run, so a full walk
  alone does not say that the run is over the limit: its last key must
  also be before the run's end."
  [size to numbers]
  (let [[n last-key] size]
    (boolean (and (number? n)
                  (> n (:max-places-of-one-run numbers))
                  (string? last-key)
                  (rows/before? last-key to)))))

;; --------------------------------------------------- what a change names

(defn named
  "Which keys a change of a push names. `names` is a sequence of
  `[:put key]` and `[:out key]`, or `:all` when the diff names no key, as
  for a whole value that Rama sent anew. Three things give the empty set,
  and the asker nothing: a push that calls back with a value that is the
  same, the row that a second attempt of a batch writes again, and the
  removal of a place that the run never held."
  [source names old new]
  (let [[kind k] (:path source)]
    (if (= :row kind)
      (if (rows/same-row? old new) #{} #{k})
      (into #{}
            (remove (fn [k] (rows/same-row? (row-in old k) (row-in new k))))
            (if (= :all names)
              (concat (keys old) (keys new))
              (map second names))))))

(defn places-of
  "The places, `[entity key]`, that keys of a source's table are for this
  query. The lane's own rows are no run of `:entity` or `:run`: a place of
  them is told by the run that holds it."
  [query source ks]
  (let [[word a b] (:asks query)
        [kind] (:path source)
        facts-row? (and (= :row kind) (= :facts (:table source)))
        facts-run? (and (= :run kind) (= :facts (:table source)))
        order? (and (= :run kind) (= :facts-by-value (:table source)))
        of-fact-key (fn [key-text] (when-let [[_ entity k] (rows/place-of-key key-text)] [entity k]))]
    (set
     (cond
       (and (= :place word) facts-row?)
       (filter #(= [a b] %) (keep of-fact-key ks))

       (and (contains? #{:entity :run} word) facts-run?)
       (keep of-fact-key ks)

       (and (= :points-at word) order?)
       (map (fn [entity] [entity a]) (filter declaration/usable-id? (keep rows/by-value-entity ks)))

       (and (= :points-at word) (check? query source))
       (keep of-fact-key ks)

       :else nil))))
