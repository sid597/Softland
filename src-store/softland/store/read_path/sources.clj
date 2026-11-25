(ns softland.store.read-path.sources
  "The sources of a read: each thing that the read path asks of Rama, as
  data, and the one function that says which of them a read wants next.
  Pure code. It needs no cluster: it makes no path of Rama's and calls
  nothing.

  A source is one value that Rama gives and can push: one row, or one run
  of neighbouring rows, of one table on one task. As data it is a map:

    :gate        `:streaming` or `:microbatch`: the gate whose table it is,
                 which `softland.store.gate.first-act/depot-at-making` gives
                 for the lane
    :table       `:facts` or `:facts-by-value`
    :lane       the lane whose rows it reads. It adds nothing to what
                 makes two sources equal: every key of the source begins
                 with that lane's text
    :routing-id  what Rama's client is given as its partitioning key: the
                 lane in a streaming lane; in a microbatch lane the home
                 of the read. It is called a routing id, as in
                 `softland.store.record.rows/routing-id`, so that \"key\"
                 stays a fact's key
    :path        `[:row key]`, one row; `[:own-rows from to]`, the lane's
                 own rows; or `[:run from to]`, a run of places, `to` not
                 in it
    :read-after  only in the source of a row above a permission of the
                 asker's (`chain-sources`): the stamp of her row that names
                 it. The source is wanted only once that row is known, so
                 it is read, or its push opened, after the row was seen: its
                 value holds the place as it was at that stamp or later

  Where a lane is read. A streaming lane has every row on its own task,
  so each source of it has the lane as its routing id. A microbatch lane
  has every row of its facts, and of its facts by value, on every task, the
  same rows everywhere; so a read of it needs one task, any one. It is read
  on the task of the read's home, the lane that the read starts from
  (`softland.store.read-path.lanes/home`): each source of a microbatch
  lane has the home as its routing id, whatever it reads: the lane's own
  rows, a permission, a place, a check, a run of one entity or across
  entities, or a run of the facts by value. So an asker reads the base where
  her own lane lives, and a read that starts from a microbatch lane reads
  it on that lane's own task. Every source is one source: no run has a
  part on each task.

  Two askers look at the same thing when their sources are equal maps; that
  map is the key by which a push is shared. In a streaming lane those are
  all the askers of one row or run. In a microbatch lane they are the
  askers of that row or run whose reads have the same home: two reads with
  two homes read the same rows on the tasks of their homes, and each holds
  a push of its own. Which parts of a row an asker asked for, and whether
  she asked for the store's own facts, are no part of a source. A check of
  a nearer lane at a place and a read of that place are one source.

  `known` is a map from a source to the value that a select gave or that a
  push holds now: a row or nil for a source of one row, a map from key to
  row for the two kinds of run. A source that `known` does not hold is not
  read yet; a source that it holds with nil is read, and there is no row.

  Inputs and results, by call:

    own-rows-source, permission-source, run-permission-source,
    place-source, run-source,
    by-value-source, check-source      -> a source
    permission-state                    whether the asker holds a permission
                                        that stands in a lane, from `known`
    chain-sources                       her permission row -> the sources
                                        of the rows above it
    chain-rows                          those rows, from `known`
    of-word                             a map from each word of a query to
                                        what it reads in one lane
    first-read-holds?                   whether the first read of a run can
                                        be made
    before-standing?                    whether a source is read before the
                                        asker's standing is known
    rights-kind, check?                 what kind a source is for a query
    most-checks                         how many checks one read may need
    nearer-checks                       the checks of nearer lanes
    reading                             what a read wants now, and what of
                                        it is missing
    moves                               what a live read joins and leaves
    read-of                             a source -> what a read asked once
                                        reads for it
    calls-for-once                      the missing sources, one group for
                                        each call of Rama's client
    run-is-whole?, over-the-limit?      the size rules of a run
    keys-of-run, row-in                 the keys of a run's value, and the
                                        row under one of them
    named, places-of                    which keys, and which places, a
                                        change names

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
  No list is read further (`softland.store.read-path.lanes/in-order`).

  So, counted as rounds that fetch something, at most: asked once, the
  rights of a rank take 2 rounds, the orders 1 and the checks 1; a given
  list settles in 4 rounds, a start lane and its list in 6. Kept live, the
  rights of a rank take 3 rounds, the facts 1 and the checks 1; a given
  list opens in 5 rounds, a start lane and its list in 8. A read asked
  once fetches each source once and keeps its value, so nothing that is
  written while it reads makes its chain longer: it settles within 6
  rounds whatever is written. A live read takes the values that its pushes
  hold now, so a write while it opens can make a part of its chain wanted
  anew: a list that gains a lane, 5 rounds more for that lane. The bound
  of each loop is set from these numbers (`softland.store.read-path`,
  `max-rounds`).

  State and effects. None. Each function is pure.

  Callers. `softland.store.read-path`, which fetches what `reading` says is
  missing; `softland.store.read-path.answer`.

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
  "Takes a lane. Gives the gate whose tables hold the lane's record: the
  depot that the lane starts with. The read path looks in that record and
  no other, as `depot-of-lane` does for a lane that is not made."
  [lane]
  (first-act/depot-at-making lane))

(defn- source-of
  "Takes the home of a read, a lane, the name of a table and a path. Gives
  the source of that path in that table of the lane. The one place where a
  source gets its routing id: the lane in a streaming lane, whose rows are
  on the lane's own task; the home in a microbatch lane, whose rows every
  task holds, so that they are read on the home's task. What must hold: the
  home is the home of the read that the source is for
  (`softland.store.read-path.lanes/home`)."
  [home lane table path]
  (let [gate (gate-of lane)]
    {:gate gate :table table :lane lane
     :routing-id (if (= :microbatch gate) home lane)
     :path path}))

(defn own-rows-source
  "Takes the home of a read and a lane. Gives the source of the lane's own
  rows: its depot, who may see it, what it builds on and its marked keys, as
  one run of the facts table. Every asker of every read through a streaming
  lane shares one push on it; in a microbatch lane, the askers of one
  home do."
  [home lane]
  (let [[from to] (rows/own-rows-run lane)]
    (source-of home lane :facts [:own-rows from to])))

(defn permission-source
  "Takes the home of a read, a lane and an asker's id. Gives the source of
  her permission row in the lane: one row, on its holder."
  [home lane asker]
  (source-of home lane :facts [:row (rows/place-key lane asker :softland.store/permission)]))

(defn run-permission-source
  "Takes the home of a read, a lane and a session's id. Gives the source of
  the row of that run's permission in the lane: one row, on the session,
  under the key of a permission for a run
  (`softland.store.gate.chain/place`)."
  [home lane session]
  (source-of home lane :facts [:row (rows/place-key lane session :softland.store/run-permission)]))

(defn chain-sources
  "Takes the home of a read, a lane, and the row of a permission of the
  asker's in the lane: her permission row, or the row of her run's
  permission; a row, nil, or `:not-known-yet`. Gives the sources of the
  rows above it, nearest first: one row for each permission above the one
  that the row holds, each on its own place
  (`softland.store.gate.chain/places-above`), and each naming the stamp of
  her row as `:read-after`. An empty vector when the row is not known, when
  it is empty, when it is an end, and when it holds a first permission. A
  permission stands only while each of those rows holds the permission
  above, with an older stamp, so a read keeps them as it keeps the row
  itself: the end of one above her is a change of one of them.

  Why a source above names her row's stamp. Rama tells each push by itself,
  and nothing lines two pushes up: a live read can hold her row as one
  decision wrote it and a row above as it was before that decision. One act
  can give her a permission beneath its own and end its own, and such a
  pair of rows then says that a permission stands which stood at no stamp.
  A source that names her row's stamp is a source of its own: it is wanted
  only once her row is known, so it is read, or its push is opened, after
  that row was seen, and its value holds its place as it was at her row's
  stamp or later. Then a row above that holds its permission, with a stamp
  older than the row beneath it, held it at her row's stamp too, since no
  decision wrote that place between the two; so a permission that these
  rows say stands, stood at her row's stamp (`softland.store.gate.chain`,
  the rule of two rows). A row that changes gives a stamp of its own, and
  the read joins the rows above it again, afresh. What it costs: askers
  share the push on a row above only when their own rows were written by
  one decision, so a live read holds a push of its own on each row above
  hers; a read asked once reads them in the round after her row, which it
  needs in any case, since her row names them.

  What must hold: the row's permission is hers; the caller tests a run's
  row first (`askers-run-row`)."
  [home lane permission-row]
  (if-let [held (when-not (= :not-known-yet permission-row) (rows/held-permission permission-row))]
    (let [after (:stamp permission-row)]
      (mapv (fn [k] (assoc (source-of home lane :facts [:row k]) :read-after after))
            (chain/places-above lane held)))
    []))

(defn askers-run-row
  "Takes the row at the place of the permission of the run that a query
  names, a row, nil or `:not-known-yet`, and the asker. Gives the row when it
  holds a permission whose holder is the asker, and nil for any other row: a
  run's permission is its holder's, and one that another holder holds for a
  run of the same id lets the asker see nothing
  (`softland.store.read-path.lanes/run-permission-state`). So the rows above
  another holder's permission are never wanted: a read that held them would
  join and leave pushes, and owe its asker a whole answer, at a give or an
  end that tells her nothing (`softland.store.read-path.answer/after-change`)."
  [run-row asker]
  (when-let [held (when-not (= :not-known-yet run-row) (rows/held-permission run-row))]
    (when (= asker (nth held 0)) run-row)))

(defn chain-rows
  "Takes the home of a read, a lane, the asker's permission row in the
  lane, and `known`. Gives the rows above her permission, as a map from
  the key of each place of `chain-sources` to the row that `known` holds
  for it, nil for an empty place; or `:not-known-yet` when `known` does not
  hold one of those sources. An empty map when there is none to read. What
  `softland.store.read-path.lanes/standing` and `meaning` take."
  [home lane permission-row known]
  (let [above (chain-sources home lane permission-row)]
    (if (every? #(contains? known %) above)
      (into {} (map (fn [source] [(second (:path source)) (get known source)])) above)
      :not-known-yet)))

(defn permission-state
  "Whether the asker of a query holds a permission that stands in a lane,
  as `known` has it. Takes the query in its one form, the home of the read,
  the lane and `known`. Gives `:stands`, `:none` or `:not-known-yet`
  (`softland.store.read-path.lanes/either`): of her permission for the
  lane, and, when the query names the run that it is asked in, of that
  run's permission, each with the rows above it. `:none` for an asker who
  is not signed in. The one place where the rows of a read's rights are
  taken out of `known`: `reading` and
  `softland.store.read-path.answer/meanings` both ask it."
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
  "Takes the home of a read, a lane, an entity and a key. Gives the source
  of that place in the lane: one row."
  [home lane entity k]
  (source-of home lane :facts [:row (rows/place-key lane entity k)]))

(defn check-source
  "Takes the home of a read, a nearer lane, an entity and a key. Gives the
  source of the check of that lane at that place: which row, if any, the
  lane holds there. It is the source of the place itself, so a check and a
  read of the place share one push."
  [home lane entity k]
  (place-source home lane entity k))

(defn run-source
  "Takes the home of a read, a lane and the two ends of a run of its facts,
  as texts. Gives the source of the run: one source, whether the two ends
  name one entity or the run goes across entities, in a streaming lane and
  in a microbatch lane alike. The task that serves it holds the whole
  run."
  [home lane from to]
  (source-of home lane :facts [:run from to]))

(defn by-value-source
  "Takes the home of a read, a lane, a key that the lane marks, the stamp
  of the mark and a thing. Gives the source of that thing's run in the
  lane's facts by value: every place under the key whose current fact has
  that value and was written since that stamp. One source: the task that
  serves it holds the whole run."
  [home lane k since thing]
  (let [[from to] (rows/by-value-run lane k since thing)]
    (source-of home lane :facts-by-value [:run from to])))

(def of-word
  "What each word of a query reads in one lane. A map from the word to a
  function of the query in its one form, a lane and the lane's own rows
  (nil when not known); it gives the fact sources of that word in that
  lane, as a vector. Each word of this build reads one source in a lane,
  or none, and each source has the routing id that the query's home gives
  it (`softland.store.read-path.lanes/home`).

    :place      the place: one row
    :entity     the entity's run
    :run        the run between the two ends; none when the ends give no
                run
    :points-at  the thing's run in the lane's facts by value, when the
                lane's own rows are known and mark the key; else none

  Whether a source of it is wanted now is `reading`'s."
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
  "Takes the text of a key, or of an end of a run. Gives true when it holds
  a UTF-16 unit from U+D800 on: a letter outside the first plane, or one
  from U+E000 on. Only for such a text can Java's order of Strings and the
  order by code point give two answers. The table keeps its keys by code
  point. The map that Rama's client makes of a run was seen to look a key
  up by the other order (`row-in`); and runs between two ends that hold
  such a unit were seen to give fewer places than the table holds, for a
  cause that this build did not find (`first-read-holds?`, `keys-of-run`)."
  [^String k]
  (let [n (.length k)]
    (loop [i 0]
      (cond (>= i n) false
            (>= (int (.charAt k i)) 0xD800) true
            :else (recur (inc i))))))

(defn first-read-holds?
  "Takes the source of a run. Gives true when the first read of the run can
  be made: neither of the run's two ends holds a UTF-16 unit from U+D800 on.

  The first read of a run walks a window from the run's start on the task
  and then cuts that window at the run's end. The table keeps its keys by
  code point. For two ends that hold no unit from U+D800 on, that order and
  Java's order of Strings agree on every key, so the cut is right whichever
  of the two Rama uses, whatever the keys of the window are. For an end
  that holds one, the two orders can differ, and this build did not find
  out which of them Rama uses for the cut. So such a run has no first read:
  it is read by its size and then whole.

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
  "Takes how a read is asked, `:once` or `:live`, and a fact source. Gives
  true when the source is read before the read knows whether the asker may
  see its lane: for a read asked once, a place, or the first read of a run
  of the facts table. A run that has no first read (`first-read-holds?`) is
  read only when she may see the lane, since its size would be read. The rights and the fact then go in one call for a
  lane, so a place through k lanes is k round trips in one round, where
  rights first would be 2k in two. What is read early is bounded, one row
  or the first places of a run, and nothing of it is handed over: an answer
  gives nothing of a lane that the asker may not see. A live read opens no
  push early, because an open push costs the lane's gate at each write.

  The one predicate of that choice, which is the builder's. The other
  answer is this function giving false: then nothing of a lane is read for
  an asker who may not see it."
  [mode source]
  (and (= :once mode)
       (= :facts (:table source))
       (or (= :row (first (:path source)))
           (first-read-holds? source))))

(defn rights-kind
  "Takes a query in its one form and a source. Gives `:own-rows` for the
  source of a lane's own rows, `:permission` for the source of the asker's
  permission row in the source's lane, `:run-permission` for the source of
  the permission of the run that the query names, `:above` for the source
  of any other row under a key of a permission in that lane when the asker
  is signed in, and nil for any other. A source can be her permission row
  and the place that she asks for at once. `:above` says only that the row
  could be one of the rows above a permission of hers: whether it is, that
  permission's own row says (`chain-sources`)."
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
  "Takes a query in its one form and a source. Gives true when the source is
  a check of a nearer lane for that query: the query asks what points at a
  thing, and the source is one row of the facts table under the asked key.
  No other word has checks, and the asked key is never one of the store's
  own, so a permission's row is never a check."
  [query source]
  (let [[word k] (:asks query)
        [kind key-text] (:path source)]
    (boolean (and (= :points-at word)
                  (= :row kind)
                  (= :facts (:table source))
                  (= k (nth (rows/place-of-key key-text) 2 nil))))))

(defn keys-of-run
  "Takes the source of a run, or of a lane's own rows, and its value, a map
  from key to row. Gives the keys of the value that are keys of the run:
  from the run's first end up to its last, the last not in it, in the
  table's order (`softland.store.record.rows/before?`).

  For a run whose two ends hold no UTF-16 unit from U+D800 on, those are
  all the keys, and none is tested: Rama's range and the table's order
  agree there. For an end that holds one, a probe of this build asked the
  store's own table for a range and was given a key that the order by code
  point puts after the range's end. So each key of such a value is held to
  the two ends here, and a key outside them is dropped: the read path gives no row whose
  key is not in the run that it asked for, whatever a range gave. Such an
  end is the end of one entity, of one lane or of one value's order, whose
  own keys all begin with the text that the two ends share, so no key of
  the run itself is lost; a run between two entities that a caller names
  is turned away before it is read, when an end of it holds such a unit
  (`softland.store.read-path.query`)."
  [source run]
  (let [[_ from to] (:path source)]
    (if (or (outside-the-agreed-range? from) (outside-the-agreed-range? to))
      (filter (fn [k] (and (not (rows/before? k from)) (rows/before? k to))) (keys run))
      (keys run))))

(defn row-in
  "Takes the value of a run, a map from key to row as a select or a push
  gave it, or nil; and a key. Gives the row under the key, or nil.

  A plain lookup is not enough for every key. The table keeps its keys by
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
  "Takes how a read is asked, `:once` or `:live`, and the numbers. Gives how
  many checks of nearer lanes one such read may need:
  `:max-checks-of-one-read` for a read asked once, where a check is one row
  of one call; `:max-checks-of-one-live-read` for a read that is kept live,
  where a check is one push and Rama bounds the pushes on one table on one
  task."
  [mode numbers]
  (if (= :live mode)
    (:max-checks-of-one-live-read numbers)
    (:max-checks-of-one-read numbers)))

(defn nearer-checks
  "The checks that a read of what points at a thing wants in nearer lanes.
  Takes the query in its one form; the lanes of the read, nearest first;
  each lane's standing; what each lane reads (`reading`'s `:by-lane`,
  for each lane's order by value); `known`; and how many checks the read
  may need at most (`most-checks`). Gives a map
  from a lane to a map from a place, `[entity key]`, to the check's source:
  for each place that the order of some lane of standing `:yes` holds, one
  check in each lane of standing `:yes` that is nearer than the nearest
  lane whose order holds the place. A lane behind that one cannot win the
  place, so nothing is read there. Gives `:too-many` when the checks are
  more than that number, and an empty map for any other word.
  An entity comes from a row's key and not from a caller, so it is tested
  here and one that is no id within the limit is left out.

  It is the only place where a source depends on the rows of a run. The
  check reads the facts table of the nearer lane, marked or not: the row
  that it finds wins the place, and the place is given when that row holds
  the thing (`softland.store.read-path.answer`).

  Not decided. What \"what points at a thing\" means through a list of
  lanes is a working answer, for Sid: at a place that a farther lane's
  order gives, the nearest lane that holds a row wins. This function is
  the one place where the other answer would be made: left out (an empty
  map), a read finds no nearer row and the runs are joined."
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
  "What a read wants now. Takes the query in its one form; `known`; how the
  read is asked, `:once` or `:live`; the numbers; and, for a live read, the
  set of the sources that the read holds, given or not. Gives a map:

    :lanes       the lanes of the read, nearest first. While the list of
                  a start lane cannot be said yet, the start lane alone
    :list-known?  false while the list cannot be said yet
    :standing     a map from each lane to the asker's standing in it
    :by-lane     a map from each lane to what the read reads in it:
                  `:own`, the source of its own rows; `:permission`, the
                  source of her permission, when wanted; `:run-permission`,
                  the source of the permission of the run that she asks in,
                  when wanted; `:chain`, the sources of the rows above
                  those two; `:facts`, the fact
                  sources that are wanted, by `of-word`; `:since`, the
                  stamp of the asked key's mark; `:checks`, a map from a
                  place to the check wanted there
    :digest       for what points at a thing, the digest of the thing
    :wanted       the set of every source that is wanted
    :missing      the set of those that `known` does not hold
    :too-large    nil, or {:what :checks-of-one-read :most n} when the
                  read needs more checks than one read may, asked as it is
                  (`most-checks`); no check is then wanted

  A source is wanted by these rules and no other.

    The own rows of the start lane, and of each lane of the list.
    Her permission in a lane, when she is signed in: while the lane's own
    rows are not known, only for a read asked once, so that it rides the
    call of the own rows; when they are known, only when the lane is made
    and its setting is `:members`. Under \"everyone\" her permission shapes
    nothing, and a lane that is not made holds none.
    The permission of the run that she asks in, by the same rule, when the
    query names a run.
    The rows above her permission, and above her run's (`chain-sources`),
    when the permission is wanted and its row is known: the row names them.
    Above her run's only when its row holds a permission of hers
    (`askers-run-row`): another holder's permission for a run of the same id
    shapes nothing of her read.
    A place or a run in a lane, when her standing is `:yes`. While her
    standing is `:not-known-yet`: for a read asked once, by
    `before-standing?`; for a live read, when the read holds the source
    already, so that a push is not closed and opened again while her
    permission is read.
    The order by value in a lane, when the lane's own rows mark the key
    and her standing is `:yes`; kept as a place or a run is while her
    standing is not known.
    A check, by `nearer-checks`.

  A lane whose standing is `:may-not-see` wants nothing but its own rows,
  her permission, her run's, and the rows above whichever of the two holds
  a permission of hers: those are what could let it in later. A lane that
  is `:not-made` wants its own rows alone. The start lane of `:from-lane` is
  in the list from the first round, before its own rows are known, so its
  permission and its fact source go with its own rows.

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
  "Takes what `reading` gave for a live read, and the set of the sources
  that the read holds. Gives {:join the sources that are wanted and not
  held, :leave those that are held and no longer wanted}, each a set."
  [reading held]
  (let [wanted (:wanted reading)]
    {:join  (into #{} (remove #(contains? held %)) wanted)
     :leave (into #{} (remove #(contains? wanted %)) held)}))

;; ------------------------------------------------- a read that is asked once

(defn read-of
  "Takes a source. Gives what a read asked once reads for it: the source
  without `:read-after`. Such a read fetches a source in a round after the
  rows that make it wanted, so a row above her permission is read after her
  own row whatever stamp it names, and two sources that differ in that
  stamp alone are one read: one path of one call. A live read keeps them
  apart, each a push of its own, opened after the row beneath it was seen
  (`chain-sources`)."
  [source]
  (dissoc source :read-after))

(defn calls-for-once
  "Takes the missing sources of a read asked once, and the numbers. Gives
  the calls to make, as a vector of {:gate :table :routing-id :sources}:
  the sources in groups of one table and one routing id, each group cut at
  `:max-paths-in-one-call` reads, where the sources of one read
  (`read-of`) are in one call and are read once. So a lane's rights and
  its fact are one round trip. Every source of a microbatch lane in one
  read has the read's home as its routing id, so what one read reads of
  such lanes is one call for their facts and one for their facts by
  value."
  [missing numbers]
  (let [most (:max-paths-in-one-call numbers)]
    (vec (for [[[gate table routing-id] sources] (group-by (juxt :gate :table :routing-id) missing)
               part (partition-all most (vals (group-by read-of sources)))]
           {:gate gate :table table :routing-id routing-id :sources (vec (apply concat part))}))))

(defn run-is-whole?
  "Takes what the first read of a run gave, its rows or their count, and the
  numbers. Gives true when the first read holds the whole run: it gave
  `:run-start-places` rows at most, where it asked for one more."
  [rows-or-count numbers]
  (<= (if (number? rows-or-count) rows-or-count (count rows-or-count))
      (:run-start-places numbers)))

(defn over-the-limit?
  "Takes what the size of a run gave, the run's end and the numbers. The
  size is the values of a walk of `:max-places-of-one-run` and one rows
  from the run's start: the count of the rows walked and the key of the
  last of them, or the count alone when the walk holds nothing. Gives true
  when the run holds more places than one read may: the walk is full, and
  its last key is still before the run's end. A walk has no end of its own
  and runs on into the keys after the run, so a full walk alone does not
  say it."
  [size to numbers]
  (let [[n last-key] size]
    (boolean (and (number? n)
                  (> n (:max-places-of-one-run numbers))
                  (string? last-key)
                  (rows/before? last-key to)))))

;; --------------------------------------------------- what a change names

(defn named
  "Which keys a change of a push names. Takes the source; what the diff
  named, a sequence of `[:put key]` and `[:out key]`, or `:all` when the
  diff names no key, as for a whole value that Rama sent anew; and the
  value of the push before and after. Gives the
  set of the keys whose row is not the same before and after
  (`softland.store.record.rows/same-row?`): for a source of one row, its
  key; for a run, among the keys that the diff named, or among every key
  of the two values for `:all`. So three things give the empty set, and
  the asker nothing: a push that calls back with a value that is the same,
  the row that a second attempt of a batch writes again, and the removal of
  a place that the run never held."
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
  "Takes a query in its one form, a source, and keys of that source's table.
  Gives the set of the places, `[entity key]`, that those keys are for this
  query, and no place for a source that cannot give this query a fact:

    :place      the asked place, from the one row at its key
    :entity,    each key of a run of the facts table. The lane's own rows
    :run        are no such run: a place of them is told by the run that
                holds it
    :points-at  the entity of each key of a run of the facts by value, and
                the place of a check

  A key that is no key of its table gives no place."
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
