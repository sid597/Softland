(ns softland.store.read-path.sources
  "The sources of a read: each thing that the read path asks of Rama, as
  data, and the one function that says which of them a read wants next.
  Pure code: it makes no path of Rama's and calls nothing.

  A source is one value that Rama gives and can push: one row, or one run
  of neighbouring rows, of one table on one task. Its `:path` is
  `[:row key]`, `[:own-rows from to]`, `[:run from to]` or
  `[:holders from to]`, `to` not in the run. Its `:lane` adds nothing to
  what makes two sources equal: every key of the source begins with that
  lane's text. Its `:routing-id` is what Rama's client is given as its
  partitioning key; it is called a routing id so that \"key\" stays a
  fact's key.

  Where a lane is read. A streaming lane has every row on its own task,
  so each source of it has the lane as its routing id. A microbatch lane
  has every row of its facts, of its facts by value, and of its permissions
  by holder on every task, the same rows everywhere; so a read of it needs
  one task, any one. It is read on the task of the read's home, the lane
  that the read starts from (`softland.store.read-path.lanes/home`): each
  source of a microbatch lane has the home as its routing id, whatever it
  reads. So an asker reads the base where her own lane lives, and a read
  that starts from a microbatch lane reads it on that lane's own task.

  Rights and live-read validity (`CONTRACT.md` §2.4, `loop-0d.md`). Facts
  stay on direct selects and pushes; an asker's rights in a made lane come
  from one single-turn `lane-rights` query on the task, which returns
  `{:standing ... :read-set ...}`. For a read asked once, `lane-rights` is
  asked after the lane's last fact select returns. For a live read, the
  pushes to watch are derived directly from `(keys (:read-set snapshot))`
  (no hand-kept watch list), and `(:standing snapshot)` holds if and only
  if the current values and stamps in `known` match `:read-set`
  entry-for-entry (`snapshot-matches?`); otherwise standing is
  `:not-known-yet`.

  What must hold. The query passed
  `softland.store.read-path.query/check`: every key text is made by
  `softland.store.record.rows` of ids that were tested first."
  (:require [softland.store.door.declaration :as declaration]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.record.rows :as rows]))

;; -------------------------------------------------------------- the sources

(defn- gate-of
  "The gate whose tables hold the lane's record: the depot that the lane
  starts with."
  [lane]
  (first-act/depot-at-making lane))

(defn routing-id-of
  "The routing id under which a fact read reads `lane`, given the read's
  `home`."
  [home lane]
  (if (= :microbatch (gate-of lane)) home lane))

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

(defn holders-source
  "A holder-sitting prefix run in `permissions-by-holder`."
  [home lane from to]
  (source-of home lane :permissions-by-holder [:holders from to]))

(defn permission-row-source
  "One permission row at `place-key` in `facts`."
  [home lane place-key]
  (source-of home lane :facts [:row place-key]))

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

   :range
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
  "True for a text that holds a UTF-16 unit from U+D800 on."
  [^String k]
  (lanes/outside-the-agreed-range? k))

(defn first-read-holds?
  "The first read of a run walks a window from the run's start on the task
  and then cuts that window at the run's end. For two ends that hold no
  unit from U+D800 on, code-point order and Java's order of Strings agree
  on every key."
  [source]
  (let [[kind from to] (:path source)]
    (and (= :run kind)
         (not (outside-the-agreed-range? from))
         (not (outside-the-agreed-range? to)))))

(defn before-standing?
  "True when the source is read before the read knows whether the asker may
  see its lane: for a read asked once, a place, or the first read of a run
  of the facts table. What is read early is bounded, one row or the first
  places of a run, and nothing of it is handed over: `read-once` asks
  `lane-rights` after the lane's last fact select returns, and an answer
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
  "Which kind of rights source `source` is: `:own-rows`, `:holders`, or
  `:permission` (a permission row in `facts`, which can also be the place a
  query asked for with `:stores-own-facts`); nil for any other source."
  [_query source]
  (let [[kind k] (:path source)]
    (cond
      (= :own-rows kind) :own-rows
      (and (= :holders kind) (= :permissions-by-holder (:table source))) :holders
      (and (= :row kind) (= :facts (:table source)) (rows/permission-place? k)) :permission
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
  (`softland.store.record.rows/before?`)."
  [source run]
  (let [[_ from to] (:path source)]
    (if (or (outside-the-agreed-range? from) (outside-the-agreed-range? to))
      (filter (fn [k] (and (not (rows/before? k from)) (rows/before? k to))) (keys run))
      (keys run))))

(defn row-in
  "Looks up `k` in `run`, falling back to linear scan when `k` holds a
  UTF-16 unit from U+D800 on."
  [run k]
  (when run
    (or (get run k)
        (when (outside-the-agreed-range? k)
          (some (fn [[key-text row]] (when (= k key-text) row)) run)))))

;; ------------------------------------------- read-set watch & stamp check

(defn read-set-sources
  "The PState watch sources named by `read-set` beyond `own-rows-source`
  (which already covers the lane's depot row)."
  [home lane read-set]
  (let [depot-key (rows/place-key lane lane :softland.store/depot)]
    (into []
          (comp (keep (fn [[which a b]]
                        (case which
                          :holders (holders-source home lane a b)
                          :fact (when (not= a depot-key)
                                  (permission-row-source home lane a))
                          nil)))
                (distinct))
          (keys read-set))))

(defn- entry-matches?
  [home lane [which a b] expected known]
  (let [depot-key (rows/place-key lane lane :softland.store/depot)]
    (case which
      :holders
      (let [src (holders-source home lane a b)]
        (and (contains? known src)
             (= expected (lanes/clean-holder-run a b (get known src)))))

      :fact
      (if (= a depot-key)
        (let [own (own-rows-source home lane)]
          (and (contains? known own)
               (= expected (:stamp (row-in (get known own) depot-key)))))
        (let [src (permission-row-source home lane a)]
          (and (contains? known src)
               (= expected (:stamp (get known src))))))

      false)))

(defn snapshot-matches?
  "True if and only if `snapshot` is a non-error `lane-rights` snapshot and
  every entry of its `:read-set` matches the current values and stamps in
  `known` entry-for-entry."
  [home lane snapshot known]
  (boolean
   (and (map? snapshot)
        (contains? #{:yes :may-not-see :holder :visitor :outside} (:standing snapshot))
        (map? (:read-set snapshot))
        (seq (:read-set snapshot))
        (every? (fn [[entry-key expected]]
                  (entry-matches? home lane entry-key expected known))
                (:read-set snapshot)))))

(defn source-matches-snapshot?
  "True when `source` is either not in `snapshot`'s `:read-set` or its
  current value in `known` matches what `snapshot`'s `:read-set` expects.
  Used when a newly opened watch push arrives (`[t_query, t_open_watch]`
  gap check)."
  [home lane snapshot source known]
  (if-not (and (map? snapshot) (map? (:read-set snapshot)) (contains? known source))
    true
    (let [depot-key (rows/place-key lane lane :softland.store/depot)
          own (own-rows-source home lane)]
      (every? (fn [[[which a b :as entry-key] expected]]
                (let [entry-src (case which
                                  :holders (holders-source home lane a b)
                                  :fact (if (= a depot-key) own (permission-row-source home lane a))
                                  nil)]
                  (or (not= source entry-src)
                      (entry-matches? home lane entry-key expected known))))
              (:read-set snapshot)))))

(defn lane-standing
  "The asker's standing in `lane` from `known` and `snapshots`. In `:live`
  mode `(:standing snapshot)` holds only while `(snapshot-matches? ...)`
  holds; otherwise `:not-known-yet`."
  [home lane mode snapshots known]
  (let [own (own-rows-source home lane)]
    (cond
      (not (contains? known own)) :not-known-yet
      (not (lanes/made? (get known own) lane)) :not-made
      :else
      (let [snapshot (get snapshots lane)]
        (cond
          (nil? snapshot) :not-known-yet
          (= :too-many-permissions (:standing snapshot)) :too-many-permissions
          (= :once mode) (if (contains? #{:yes :may-not-see} (:standing snapshot))
                           (:standing snapshot)
                           :not-known-yet)
          (snapshot-matches? home lane snapshot known) (:standing snapshot)
          :else :not-known-yet)))))

;; ---------------------------------------------- checks in nearer lanes

(defn most-checks
  [mode numbers]
  (if (= :live mode)
    (:max-checks-of-one-live-read numbers)
    (:max-checks-of-one-read numbers)))

(defn nearer-checks
  "The checks that a read of what points at a thing wants in nearer lanes: a
  map from a lane to a map from a place, `[entity key]`, to the check's
  source; `:too-many` when they are more than `most`."
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

(defn settled?
  "True when a `reading` needs no more PState sources, needs no `lane-rights`
  query, hit no bound, and has resolved every lane's standing to `:yes`,
  `:may-not-see`, or `:not-made`."
  [reading]
  (boolean
   (and reading
        (nil? (:too-large reading))
        (nil? (:too-many-permissions reading))
        (empty? (:missing reading))
        (empty? (:needs-rights reading))
        (every? #(contains? #{:yes :may-not-see :not-made} %) (vals (:standing reading))))))

(defn reading
  "What a read wants now. `mode` is `:once` or `:live`; `held`, for a live
  read, is the set of the sources that the read holds, given or not;
  `snapshots` is `{lane snapshot}` of `lane-rights` answers held by this
  read.

  In `:live` mode, the watch sources of a made lane are the sources in
  `(:read-set snapshot)` (`read-set-sources`), and `(:standing snapshot)`
  holds only while `known` matches `:read-set` entry-for-entry. While
  standing is `:not-known-yet`, a live read keeps any fact pushes of that
  lane that it already holds so they are not closed and reopened across a
  query turn, and tells nothing of the lane."
  ([query known mode numbers]
   (reading query known mode numbers nil nil))
  ([query known mode numbers held]
   (reading query known mode numbers held nil))
  ([query known mode numbers held snapshots]
   (let [home (lanes/home query)
         [word k thing] (:asks query)
         facts-of (get of-word word)
         rights (fn [lane]
                  (let [own (own-rows-source home lane)
                        own-known? (contains? known own)
                        own-rows (get known own)
                        made? (and own-known? (lanes/made? own-rows lane))
                        snapshot (get snapshots lane)
                        watch (if (and (= :live mode)
                                       made?
                                       snapshot
                                       (not= :too-many-permissions (:standing snapshot)))
                                (read-set-sources home lane (:read-set snapshot))
                                [])
                        st (lane-standing home lane mode snapshots known)
                        needs-rights? (and made?
                                           (= :not-known-yet st)
                                           (every? #(contains? known %) watch))]
                    {:own own
                     :own-rows (when own-known? own-rows)
                     :watches watch
                     :needs-rights? needs-rights?
                     :standing st}))
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
         too-many-lane (some (fn [lane]
                               (when (= :too-many-permissions (get standing lane)) lane))
                             lane-list)
         needs-rights (into []
                            (keep (fn [lane]
                                    (when (get-in by-lane [lane :needs-rights?])
                                      [(routing-id-of home lane) lane])))
                            lane-list)
         checks (if too-many-lane
                  {}
                  (nearer-checks query lane-list standing by-lane known (most-checks mode numbers)))
         too-many? (= :too-many checks)
         by-lane (if too-many?
                   by-lane
                   (reduce (fn [m [lane at]] (assoc-in m [lane :checks] at)) by-lane checks))
         wanted (into #{}
                      (mapcat (fn [{:keys [own watches facts checks]}]
                                (concat [own] watches facts (vals checks))))
                      (vals by-lane))]
     (cond-> {:lanes          lane-list
              :list-known?    (some? listed)
              :standing       standing
              :needs-rights   needs-rights
              :by-lane        (into {}
                                    (map (fn [[lane told]]
                                           [lane (dissoc told :own-rows :standing :needs-rights?)]))
                                    by-lane)
              :wanted         wanted
              :missing        (into #{} (remove #(contains? known %)) wanted)
              :too-large      (when too-many?
                                {:what :checks-of-one-read :most (most-checks mode numbers)})
              :too-many-permissions (when too-many-lane
                                      {:lane too-many-lane :most lanes/max-holder-permissions})}
       (= :points-at word) (assoc :digest (rows/value-digest-of thing))))))

(defn moves
  [reading held]
  (let [wanted (:wanted reading)]
    {:join  (into #{} (remove #(contains? held %)) wanted)
     :leave (into #{} (remove #(contains? wanted %)) held)}))

;; ------------------------------------------------- a read that is asked once

(defn read-of
  "What a read asked once reads for a source."
  [source]
  (dissoc source :read-after))

(defn calls-for-once
  "The sources of one read (`read-of`) are in one call and are read once."
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
  run's start gave."
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
  for a whole value that Rama sent anew."
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
  query. The lane's own rows are no run of `:entity` or `:range`: a place of
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

       (and (contains? #{:entity :range} word) facts-run?)
       (keep of-fact-key ks)

       (and (= :points-at word) order?)
       (map (fn [entity] [entity a]) (filter declaration/usable-id? (keep rows/by-value-entity ks)))

       (and (= :points-at word) (check? query source))
       (keep of-fact-key ks)

       :else nil))))
