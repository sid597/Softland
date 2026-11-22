(ns softland.store.read-path.sources
  "The sources of a read: each thing that the read path asks of Rama, as
  data, and the one function that says which of them a read wants next.
  Pure code. It needs no cluster: it makes no path of Rama's and calls
  nothing.

  A source is one value that Rama gives and can push: one row, or one run
  of neighbouring rows, of one table on one task. As data it is a map:

    :gate        `:streaming` or `:microbatch`: the gate whose table it is,
                 which `softland.store.gate.first-act/depot-at-making` gives
                 for the layer
    :table       `:facts` or `:facts-by-value`
    :layer       the layer whose rows it reads. It adds nothing to what
                 makes two sources equal: every key of the source begins
                 with that layer's text
    :routing-id  what Rama's client is given as its partitioning key: the
                 layer in a streaming layer; in a microbatch layer the home
                 of the read. It is called a routing id, as in
                 `softland.store.record.rows/routing-id`, so that \"key\"
                 stays a fact's key
    :path        `[:row key]`, one row; `[:own-rows from to]`, the layer's
                 own rows; or `[:run from to]`, a run of places, `to` not
                 in it
    :read-after  only in the source of a row above a permission of the
                 asker's (`chain-sources`): the stamp of her row that names
                 it. The source is wanted only once that row is known, so
                 it is read, or its push opened, after the row was seen: its
                 value holds the place as it was at that stamp or later

  Where a layer is read. A streaming layer has every row on its own task,
  so each source of it has the layer as its routing id. A microbatch layer
  has every row of its facts, and of its facts by value, on every task, the
  same rows everywhere; so a read of it needs one task, any one. It is read
  on the task of the read's home, the layer that the read starts from
  (`softland.store.read-path.layers/home`): each source of a microbatch
  layer has the home as its routing id, whatever it reads: the layer's own
  rows, a permission, a place, a check, a run of one entity or across
  entities, or a run of the facts by value. So an asker reads the base where
  her own layer lives, and a read that starts from a microbatch layer reads
  it on that layer's own task. Every source is one source: no run has a
  part on each task.

  Two askers look at the same thing when their sources are equal maps; that
  map is the key by which a push is shared. In a streaming layer those are
  all the askers of one row or run. In a microbatch layer they are the
  askers of that row or run whose reads have the same home: two reads with
  two homes read the same rows on the tasks of their homes, and each holds
  a push of its own. Which parts of a row an asker asked for, and whether
  she asked for the store's own facts, are no part of a source. A check of
  a nearer layer at a place and a read of that place are one source.

  `known` is a map from a source to the value that a select gave or that a
  push holds now: a row or nil for a source of one row, a map from key to
  row for the two kinds of run. A source that `known` does not hold is not
  read yet; a source that it holds with nil is read, and there is no row.

  Inputs and results, by call:

    own-rows-source, permission-source, run-permission-source,
    place-source, run-source,
    by-value-source, check-source      -> a source
    permission-state                    whether the asker holds a permission
                                        that stands in a layer, from `known`
    chain-sources                       her permission row -> the sources
                                        of the rows above it
    chain-rows                          those rows, from `known`
    of-word                             a map from each word of a query to
                                        what it reads in one layer
    first-read-holds?                   whether the first read of a run can
                                        be made
    before-standing?                    whether a source is read before the
                                        asker's standing is known
    rights-kind, check?                 what kind a source is for a query
    most-checks                         how many checks one read may need
    nearer-checks                       the checks of nearer layers
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
  before. In one layer of a read the chain is:

    rights   the layer's own rows; her permission and her run's, once the
             own rows say that the layer is made and open to its members,
             or for a read asked once in the round of the own rows; and
             the rows above whichever of the two holds a given permission
             of hers, one round after that row, all at once, since the row
             names each (`chain-sources`)
    facts    once she may see the layer; for a read asked once, a place and
             the first read of a run in the round of the own rows
             (`before-standing?`). The order by value waits for the own
             rows, which say whether the key is marked
    checks   one round after the orders of the farther layers
             (`nearer-checks`)

  A read has two ranks of layers at most: the layers of `:layers`, or the
  start layer of `:from-layer`; and then the layers of the start layer's
  list, which is known only once she may see the start layer, so that the
  rights of those layers begin one round after the start layer's last.
  No list is read further (`softland.store.read-path.layers/in-order`).

  So, counted as rounds that fetch something, at most: asked once, the
  rights of a rank take 2 rounds, the orders 1 and the checks 1; a given
  list settles in 4 rounds, a start layer and its list in 6. Kept live, the
  rights of a rank take 3 rounds, the facts 1 and the checks 1; a given
  list opens in 5 rounds, a start layer and its list in 8. A read asked
  once fetches each source once and keeps its value, so nothing that is
  written while it reads makes its chain longer: it settles within 6
  rounds whatever is written. A live read takes the values that its pushes
  hold now, so a write while it opens can make a part of its chain wanted
  anew: a list that gains a layer, 5 rounds more for that layer. The bound
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
            [softland.store.read-path.layers :as layers]
            [softland.store.record.rows :as rows]))

;; -------------------------------------------------------------- the sources

(defn- gate-of
  "Takes a layer. Gives the gate whose tables hold the layer's record: the
  depot that the layer starts with. The read path looks in that record and
  no other, as `depot-of-layer` does for a layer that is not made."
  [layer]
  (first-act/depot-at-making layer))

(defn- source-of
  "Takes the home of a read, a layer, the name of a table and a path. Gives
  the source of that path in that table of the layer. The one place where a
  source gets its routing id: the layer in a streaming layer, whose rows are
  on the layer's own task; the home in a microbatch layer, whose rows every
  task holds, so that they are read on the home's task. What must hold: the
  home is the home of the read that the source is for
  (`softland.store.read-path.layers/home`)."
  [home layer table path]
  (let [gate (gate-of layer)]
    {:gate gate :table table :layer layer
     :routing-id (if (= :microbatch gate) home layer)
     :path path}))

(defn own-rows-source
  "Takes the home of a read and a layer. Gives the source of the layer's own
  rows: its depot, who may see it, what it builds on and its marked keys, as
  one run of the facts table. Every asker of every read through a streaming
  layer shares one push on it; in a microbatch layer, the askers of one
  home do."
  [home layer]
  (let [[from to] (rows/own-rows-run layer)]
    (source-of home layer :facts [:own-rows from to])))

(defn permission-source
  "Takes the home of a read, a layer and an asker's id. Gives the source of
  her permission row in the layer: one row, on its holder."
  [home layer asker]
  (source-of home layer :facts [:row (rows/place-key layer asker :softland.store/permission)]))

(defn run-permission-source
  "Takes the home of a read, a layer and a session's id. Gives the source of
  the row of that run's permission in the layer: one row, on the session,
  under the key of a permission for a run
  (`softland.store.gate.chain/place`)."
  [home layer session]
  (source-of home layer :facts [:row (rows/place-key layer session :softland.store/run-permission)]))

(defn chain-sources
  "Takes the home of a read, a layer, and the row of a permission of the
  asker's in the layer: her permission row, or the row of her run's
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
  [home layer permission-row]
  (if-let [held (when-not (= :not-known-yet permission-row) (rows/held-permission permission-row))]
    (let [after (:stamp permission-row)]
      (mapv (fn [k] (assoc (source-of home layer :facts [:row k]) :read-after after))
            (chain/places-above layer held)))
    []))

(defn askers-run-row
  "Takes the row at the place of the permission of the run that a query
  names, a row, nil or `:not-known-yet`, and the asker. Gives the row when it
  holds a permission whose holder is the asker, and nil for any other row: a
  run's permission is its holder's, and one that another holder holds for a
  run of the same id lets the asker see nothing
  (`softland.store.read-path.layers/run-permission-state`). So the rows above
  another holder's permission are never wanted: a read that held them would
  join and leave pushes, and owe its asker a whole answer, at a give or an
  end that tells her nothing (`softland.store.read-path.answer/after-change`)."
  [run-row asker]
  (when-let [held (when-not (= :not-known-yet run-row) (rows/held-permission run-row))]
    (when (= asker (nth held 0)) run-row)))

(defn chain-rows
  "Takes the home of a read, a layer, the asker's permission row in the
  layer, and `known`. Gives the rows above her permission, as a map from
  the key of each place of `chain-sources` to the row that `known` holds
  for it, nil for an empty place; or `:not-known-yet` when `known` does not
  hold one of those sources. An empty map when there is none to read. What
  `softland.store.read-path.layers/standing` and `meaning` take."
  [home layer permission-row known]
  (let [above (chain-sources home layer permission-row)]
    (if (every? #(contains? known %) above)
      (into {} (map (fn [source] [(second (:path source)) (get known source)])) above)
      :not-known-yet)))

(defn permission-state
  "Whether the asker of a query holds a permission that stands in a layer,
  as `known` has it. Takes the query in its one form, the home of the read,
  the layer and `known`. Gives `:stands`, `:none` or `:not-known-yet`
  (`softland.store.read-path.layers/either`): of her permission for the
  layer, and, when the query names the run that it is asked in, of that
  run's permission, each with the rows above it. `:none` for an asker who
  is not signed in. The one place where the rows of a read's rights are
  taken out of `known`: `reading` and
  `softland.store.read-path.answer/meanings` both ask it."
  [query home layer known]
  (let [asker (:asker query)
        session (:session query)
        row-of (fn [source] (if (contains? known source) (get known source) :not-known-yet))]
    (if (nil? asker)
      :none
      (let [own-row (row-of (permission-source home layer asker))
            for-the-layer (layers/permission-state layer own-row (chain-rows home layer own-row known))
            for-the-run (if session
                          (let [run-row (row-of (run-permission-source home layer session))]
                            (layers/run-permission-state layer asker run-row
                                                         (chain-rows home layer (askers-run-row run-row asker) known)))
                          :none)]
        (layers/either for-the-layer for-the-run)))))

(defn place-source
  "Takes the home of a read, a layer, an entity and a key. Gives the source
  of that place in the layer: one row."
  [home layer entity k]
  (source-of home layer :facts [:row (rows/place-key layer entity k)]))

(defn check-source
  "Takes the home of a read, a nearer layer, an entity and a key. Gives the
  source of the check of that layer at that place: which row, if any, the
  layer holds there. It is the source of the place itself, so a check and a
  read of the place share one push."
  [home layer entity k]
  (place-source home layer entity k))

(defn run-source
  "Takes the home of a read, a layer and the two ends of a run of its facts,
  as texts. Gives the source of the run: one source, whether the two ends
  name one entity or the run goes across entities, in a streaming layer and
  in a microbatch layer alike. The task that serves it holds the whole
  run."
  [home layer from to]
  (source-of home layer :facts [:run from to]))

(defn by-value-source
  "Takes the home of a read, a layer, a key that the layer marks, the stamp
  of the mark and a thing. Gives the source of that thing's run in the
  layer's facts by value: every place under the key whose current fact has
  that value and was written since that stamp. One source: the task that
  serves it holds the whole run."
  [home layer k since thing]
  (let [[from to] (rows/by-value-run layer k since thing)]
    (source-of home layer :facts-by-value [:run from to])))

(def of-word
  "What each word of a query reads in one layer. A map from the word to a
  function of the query in its one form, a layer and the layer's own rows
  (nil when not known); it gives the fact sources of that word in that
  layer, as a vector. Each word of this build reads one source in a layer,
  or none, and each source has the routing id that the query's home gives
  it (`softland.store.read-path.layers/home`).

    :place      the place: one row
    :entity     the entity's run
    :run        the run between the two ends; none when the ends give no
                run
    :points-at  the thing's run in the layer's facts by value, when the
                layer's own rows are known and mark the key; else none

  Whether a source of it is wanted now is `reading`'s."
  {:place
   (fn [query layer _own-rows]
     (let [[_ entity k] (:asks query)]
       [(place-source (layers/home query) layer entity k)]))

   :entity
   (fn [query layer _own-rows]
     (let [[_ entity] (:asks query)
           [from to] (rows/entity-run layer entity)]
       [(run-source (layers/home query) layer from to)]))

   :run
   (fn [query layer _own-rows]
     (let [[_ first-end last-end] (:asks query)]
       (if-let [[from to] (rows/run-of-ends layer first-end last-end)]
         [(run-source (layers/home query) layer from to)]
         [])))

   :points-at
   (fn [query layer own-rows]
     (let [[_ k thing] (:asks query)]
       (if-let [since (layers/marked-since own-rows layer k)]
         [(by-value-source (layers/home query) layer k since thing)]
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
  see its layer: for a read asked once, a place, or the first read of a run
  of the facts table. A run that has no first read (`first-read-holds?`) is
  read only when she may see the layer, since its size would be read. The rights and the fact then go in one call for a
  layer, so a place through k layers is k round trips in one round, where
  rights first would be 2k in two. What is read early is bounded, one row
  or the first places of a run, and nothing of it is handed over: an answer
  gives nothing of a layer that the asker may not see. A live read opens no
  push early, because an open push costs the layer's gate at each write.

  The one predicate of that choice, which is the builder's. The other
  answer is this function giving false: then nothing of a layer is read for
  an asker who may not see it."
  [mode source]
  (and (= :once mode)
       (= :facts (:table source))
       (or (= :row (first (:path source)))
           (first-read-holds? source))))

(defn rights-kind
  "Takes a query in its one form and a source. Gives `:own-rows` for the
  source of a layer's own rows, `:permission` for the source of the asker's
  permission row in the source's layer, `:run-permission` for the source of
  the permission of the run that the query names, `:above` for the source
  of any other row under a key of a permission in that layer when the asker
  is signed in, and nil for any other. A source can be her permission row
  and the place that she asks for at once. `:above` says only that the row
  could be one of the rows above a permission of hers: whether it is, that
  permission's own row says (`chain-sources`)."
  [query source]
  (let [[kind k] (:path source)
        layer (:layer source)]
    (cond
      (= :own-rows kind) :own-rows
      (not (and (= :row kind) (= :facts (:table source)) (some? (:asker query)))) nil
      (= k (rows/place-key layer (:asker query) :softland.store/permission)) :permission
      (and (some? (:session query))
           (= k (rows/place-key layer (:session query) :softland.store/run-permission))) :run-permission
      (contains? declaration/permission-keys (nth (rows/place-of-key k) 2 nil)) :above
      :else nil)))

(defn check?
  "Takes a query in its one form and a source. Gives true when the source is
  a check of a nearer layer for that query: the query asks what points at a
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
  "Takes the source of a run, or of a layer's own rows, and its value, a map
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
  end is the end of one entity, of one layer or of one value's order, whose
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
  many checks of nearer layers one such read may need:
  `:max-checks-of-one-read` for a read asked once, where a check is one row
  of one call; `:max-checks-of-one-live-read` for a read that is kept live,
  where a check is one push and Rama bounds the pushes on one table on one
  task."
  [mode numbers]
  (if (= :live mode)
    (:max-checks-of-one-live-read numbers)
    (:max-checks-of-one-read numbers)))

(defn nearer-checks
  "The checks that a read of what points at a thing wants in nearer layers.
  Takes the query in its one form; the layers of the read, nearest first;
  each layer's standing; what each layer reads (`reading`'s `:by-layer`,
  for each layer's order by value); `known`; and how many checks the read
  may need at most (`most-checks`). Gives a map
  from a layer to a map from a place, `[entity key]`, to the check's source:
  for each place that the order of some layer of standing `:yes` holds, one
  check in each layer of standing `:yes` that is nearer than the nearest
  layer whose order holds the place. A layer behind that one cannot win the
  place, so nothing is read there. Gives `:too-many` when the checks are
  more than that number, and an empty map for any other word.
  An entity comes from a row's key and not from a caller, so it is tested
  here and one that is no id within the limit is left out.

  It is the only place where a source depends on the rows of a run. The
  check reads the facts table of the nearer layer, marked or not: the row
  that it finds wins the place, and the place is given when that row holds
  the thing (`softland.store.read-path.answer`).

  Not decided. What \"what points at a thing\" means through a list of
  layers is a working answer, for Sid: at a place that a farther layer's
  order gives, the nearest layer that holds a row wins. This function is
  the one place where the other answer would be made: left out (an empty
  map), a read finds no nearer row and the runs are joined."
  [query layers standing by-layer known most]
  (let [[word k] (:asks query)
        home (layers/home query)]
    (if (not= :points-at word)
      {}
      (loop [todo (filter #(= :yes (get standing %)) layers)
             nearer []
             won #{}
             checks {}
             n 0]
        (if-let [layer (first todo)]
          (let [order-source (first (get-in by-layer [layer :facts]))
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
                     (conj nearer layer)
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

    :layers       the layers of the read, nearest first. While the list of
                  a start layer cannot be said yet, the start layer alone
    :list-known?  false while the list cannot be said yet
    :standing     a map from each layer to the asker's standing in it
    :by-layer     a map from each layer to what the read reads in it:
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

    The own rows of the start layer, and of each layer of the list.
    Her permission in a layer, when she is signed in: while the layer's own
    rows are not known, only for a read asked once, so that it rides the
    call of the own rows; when they are known, only when the layer is made
    and its setting is `:members`. Under \"everyone\" her permission shapes
    nothing, and a layer that is not made holds none.
    The permission of the run that she asks in, by the same rule, when the
    query names a run.
    The rows above her permission, and above her run's (`chain-sources`),
    when the permission is wanted and its row is known: the row names them.
    Above her run's only when its row holds a permission of hers
    (`askers-run-row`): another holder's permission for a run of the same id
    shapes nothing of her read.
    A place or a run in a layer, when her standing is `:yes`. While her
    standing is `:not-known-yet`: for a read asked once, by
    `before-standing?`; for a live read, when the read holds the source
    already, so that a push is not closed and opened again while her
    permission is read.
    The order by value in a layer, when the layer's own rows mark the key
    and her standing is `:yes`; kept as a place or a run is while her
    standing is not known.
    A check, by `nearer-checks`.

  A layer whose standing is `:may-not-see` wants nothing but its own rows,
  her permission, her run's, and the rows above whichever of the two holds
  a permission of hers: those are what could let it in later. A layer that
  is `:not-made` wants its own rows alone. The start layer of `:from-layer` is
  in the list from the first round, before its own rows are known, so its
  permission and its fact source go with its own rows.

  Limit. It reads no size of a run: a run that is too large is found where
  it is fetched (`softland.store.read-path`)."
  ([query known mode numbers]
   (reading query known mode numbers nil))
  ([query known mode numbers held]
   (let [asker (:asker query)
         home (layers/home query)
         signed-in? (some? asker)
         [word k thing] (:asks query)
         facts-of (get of-word word)
         session (:session query)
         rights (fn [layer]
                  (let [own (own-rows-source home layer)
                        own-rows (get known own)
                        row-of (fn [source] (if (and source (contains? known source))
                                              (get known source)
                                              :not-known-yet))
                        permission (when signed-in? (permission-source home layer asker))
                        run-permission (when (and signed-in? session) (run-permission-source home layer session))
                        wanted? (and signed-in?
                                     (if (nil? own-rows)
                                       (= :once mode)
                                       (and (layers/made? own-rows layer)
                                            (= :members (layers/who-may-see own-rows layer)))))
                        standing (layers/standing layer own-rows
                                                  (permission-state query home layer known)
                                                  signed-in?)]
                    {:own own
                     :own-rows own-rows
                     :permission (when wanted? permission)
                     :run-permission (when wanted? run-permission)
                     :chain (if wanted?
                              (into (chain-sources home layer (row-of permission))
                                    (when run-permission
                                      (chain-sources home layer (askers-run-row (row-of run-permission) asker))))
                              [])
                     :standing standing}))
         start (:from-layer query)
         start-rights (when start (rights start))
         listed (layers/in-order query (:own-rows start-rights) (:standing start-rights))
         layer-list (or listed [start])
         with-facts (fn [layer {:keys [own-rows standing] :as told}]
                      (let [wanted? (fn [source]
                                      (case standing
                                        :yes true
                                        :not-known-yet (if (= :once mode)
                                                         (before-standing? mode source)
                                                         (contains? held source))
                                        false))]
                        (cond-> (assoc told :facts (filterv wanted? (facts-of query layer own-rows)))
                          (= :points-at word) (assoc :since (layers/marked-since own-rows layer k)))))
         by-layer (into {}
                        (map (fn [layer]
                               [layer (with-facts layer (if (= layer start) start-rights (rights layer)))]))
                        layer-list)
         standing (into {} (map (fn [[layer told]] [layer (:standing told)])) by-layer)
         checks (nearer-checks query layer-list standing by-layer known (most-checks mode numbers))
         too-many? (= :too-many checks)
         by-layer (if too-many?
                    by-layer
                    (reduce (fn [m [layer at]] (assoc-in m [layer :checks] at)) by-layer checks))
         wanted (into #{}
                      (mapcat (fn [{:keys [own permission run-permission chain facts checks]}]
                                (concat [own] (when permission [permission])
                                        (when run-permission [run-permission]) chain facts (vals checks))))
                      (vals by-layer))]
     (cond-> {:layers      layer-list
              :list-known? (some? listed)
              :standing    standing
              :by-layer    (into {} (map (fn [[layer told]] [layer (dissoc told :own-rows :standing)])) by-layer)
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
  (`read-of`) are in one call and are read once. So a layer's rights and
  its fact are one round trip. Every source of a microbatch layer in one
  read has the read's home as its routing id, so what one read reads of
  such layers is one call for their facts and one for their facts by
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
    :entity,    each key of a run of the facts table. The layer's own rows
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
