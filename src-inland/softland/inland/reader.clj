(ns softland.inland.reader
  "The screen's reads, through the rig's one read exit, pushed by the store.
  Takes a person's session in the store and read units (a layer and a
  pattern); gives each unit's rows to its watchers once the act recording
  the read is answered yes. Owns, per session, one serial worker, the open
  standing reads and the marks; shares the store's bells across sessions.

  - A unit is one standing read (rig.store.standing) on `[layer pattern]`,
    shared by every narrow subscription Inland opens onto it. Opening it,
    each delivery and its closing are lines in the reader's working layer
    (the person's hand layer), under the session's permission there.
  - One record per cause: every line and every read one cause produces (the
    screen opening reads, a push delivering, a forget re-read, a lookup of
    what stood on a changed fact) goes into ONE act; nothing it read reaches
    a watcher unless that act is answered yes.
  - Push, not poll: a delivery runs only when a bell rings (rig.store.bell):
    a one-owner layer's last admitted stamp, the micro store's settled
    frontier for shared layers, a person's entry on each task for a forget.
    A ring carries no fact; what follows it is read through the exit.
  - A forget does not reach a standing read's deltas (RIG.md For Sid 53), so
    a person's ring re-reads by id every value the session shows, and the
    rows come back erased with their date.
  - When new rows show a fact replaced in its layer, or shadowed by a
    nearer layer of the session's context, the reader reads what stood on
    the old fact (`[:dependents F]`, the citation session's pattern) in the
    person's hand layer and own layer, and keeps the dependents as marks.
    A page's own stored cells are left out: only this page's gestures
    write them, and every gesture reads the context cell, so a pin would
    otherwise mark every selection made before it.
  - What the page stood on is held live (`stand-on!`): once a gesture's act
    stands on what it read, the reader holds a unit on each thing read, in
    every context layer, so the change is seen whatever the screen shows.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [com.rpl.rama :as rama]
            [rig.store.bell :as bell]
            [rig.store.envelope :as env]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]
            [rig.store.standing :as sg]
            [softland.inland.facts :as f])
  (:import [java.util.concurrent Executors ScheduledExecutorService TimeUnit]))

(def flush-delay-ms "How long a cause gathers its reads before its one act." 15)
(def close-grace-ms "How long an unwatched unit stays open, so a re-render re-watching it opens nothing new." 4000)
(def unit-limit "Rows a standing read delivers at a time." 1000)
(def point-page "Fact ids one re-read by id takes." 1000)

(declare rung! flush!)

;; ================================================================ the bells, shared

(defonce ^:private bells
  ;; key -> {:proxy ProxyState :readers #{reader}}; key [:layer L] | [:frontier] | [:person p pkey]
  (atom {}))

(defn- ring! [key]
  (doseq [r (get-in @bells [key :readers])]
    ((:on-ring r) key)))

(defn- open-bell
  "The proxy for a bell key, pushed by the store."
  [store [kind a b]]
  (case kind
    :layer (bell/layer-bell store a (fn [_] (ring! [:layer a])))
    :frontier (bell/frontier-bell store (fn [_] (ring! [:frontier])))
    :person (bell/person-bell store a b (fn [_] (ring! [:person a b])))))

(defn- hear!
  "Subscribe reader `r` to bell `key`, opening its proxy on first use."
  [store r key]
  (locking bells
    (if (get @bells key)
      (swap! bells update-in [key :readers] conj r)
      (let [p (open-bell store key)]
        (swap! bells assoc key {:proxy p :readers #{r}})))))

(defn- unhear!
  "Unsubscribe reader `r` from bell `key`, closing its proxy when no reader is left."
  [r key]
  (locking bells
    (when-let [{:keys [proxy readers]} (get @bells key)]
      (let [left (disj readers r)]
        (if (empty? left)
          (do (swap! bells dissoc key) (try (rama/close! proxy) (catch Throwable _ nil)))
          (swap! bells assoc-in [key :readers] left))))))

;; ================================================================ units

(defn unit-key "A read unit's key." [layer pattern] [layer pattern])

(defn- bell-for [class layer]
  (if (= :by-entity class) [:frontier] [:layer layer]))

(defn- value-row? [row] (contains? row :value))

(defn- rows-by-fid [rows] (into {} (map (juxt :fid identity)) rows))

;; ================================================================ the reader

(defn create
  "A reader for one browser session. `who` is the exit's reader part
  (`:reader` the person, `:reader-kind :person`, `:working` their hand
  layer, `:permission` the session's there, `:session`); `own` the
  person's own layer; `people` the persons whose forgets can erase what the
  screen shows; `task-keys` one key per task (rig.store.bell). The reader
  starts listening for the people's forgets at once."
  [store {:keys [who own people task-keys]}]
  (let [exec ^ScheduledExecutorService (Executors/newSingleThreadScheduledExecutor)
        ;; the ring handler reaches the finished reader: a map closed over before it
        ;; held :on-ring would be registered with the bells and fail at the next ring
        self (promise)
        r {:store store :who who :own own :exec exec
           :units (atom {})          ; key -> unit
           :dirty (atom #{})         ; layers rung and not yet delivered; :erasures when a forget rang
           :flush-at (atom nil)      ; a scheduled flush, or nil
           :marks (atom {})          ; dependent fid -> mark
           :looked (atom #{})        ; fids whose dependents were read
           :context (atom ["base"])  ; the session's context layers, nearest first (Inland names)
           :held (atom #{})          ; things this page's acts stood on, watched in every context layer
           :holding (atom {})        ; unit key -> stop, the units held open on them
           :on-marks (atom nil)      ; called with the marks when they change
           :closed? (atom false)
           :on-ring (fn [key] (.execute exec #(rung! @self key)))}]
    (deliver self r)
    (doseq [p people k task-keys] (hear! store r [:person p k]))
    r))

(defn- run-on [r f]
  (.execute ^ScheduledExecutorService (:exec r) ^Runnable f))

(defn- notify!
  "Hand a unit's current rows to every watcher."
  [unit]
  (doseq [[_ w] (:watchers unit)]
    (try (w unit) (catch Throwable _ nil))))

(defn- schedule-flush!
  "Flush once `flush-delay-ms` from the first request of a cause."
  [r]
  (when (compare-and-set! (:flush-at r) nil :scheduled)
    (.schedule ^ScheduledExecutorService (:exec r)
               ^Runnable (fn [] (reset! (:flush-at r) nil) (flush! r))
               (long flush-delay-ms) TimeUnit/MILLISECONDS)))

(defn rung!
  "A bell rang (on the reader's worker): mark what to deliver, and flush."
  [r [kind a]]
  (case kind
    :layer (swap! (:dirty r) conj a)
    :frontier (swap! (:dirty r) into (->> (vals @(:units r)) (filter :shared?) (map :layer)))
    :person (swap! (:dirty r) conj :erasures))
  (schedule-flush! r))

(defn watch!
  "Watch unit `key` with `f`, called with the unit (`:status` `:open`,
  `:refused` or `:failed`, and `:rows`, a map from fact id to row) whenever
  it changes, on the reader's worker. Opens the unit in the next flush when
  nobody holds it. Returns a function that stops watching; an unwatched
  unit closes after `close-grace-ms`."
  [r key f]
  (let [id (Object.)]
    (run-on r
      (fn []
        (if-let [u (get @(:units r) key)]
          (do (swap! (:units r) update key #(-> % (assoc-in [:watchers id] f) (dissoc :idle-since)))
              (when (#{:open :refused :failed} (:status u)) (f (get @(:units r) key))))
          (do (swap! (:units r) assoc key (cond-> {:key key :layer (first key) :pattern (second key)
                                                   :status :opening :watchers {id f} :rows {}}
                                            ;; a pinned version: one read as of its stamp, never delivered again
                                            (= 3 (count key)) (assoc :as-of (nth key 2))))
              (schedule-flush! r)))))
    (fn []
      (run-on r
        (fn []
          (when (get @(:units r) key)
            (swap! (:units r) update key
                   (fn [u] (let [u (update u :watchers dissoc id)]
                             (if (empty? (:watchers u)) (assoc u :idle-since (System/currentTimeMillis)) u))))
            (when (:idle-since (get @(:units r) key))
              ;; the grace timer flushes itself: a flush already scheduled may run before the grace passed
              (.schedule ^ScheduledExecutorService (:exec r)
                         ^Runnable (fn [] (flush! r))
                         (long (+ close-grace-ms 50)) TimeUnit/MILLISECONDS))))))))

(defn- hold!
  "Open, in every context layer, a unit on each held thing not yet held.
  Its watcher does nothing: the unit is held so its rows reach the flush."
  [r]
  (doseq [e @(:held r)
          layer (map f/layer-id @(:context r))
          :let [key (unit-key layer [:e e])]
          :when (not (contains? @(:holding r) key))]
    (swap! (:holding r) assoc key (watch! r key (fn [_])))))

(defn stand-on!
  "Things an act of this page stood on (each read whole by a gesture): the
  reader holds a live read on each, in every layer of the session's context,
  for the session's life. A change to a fact the page stood on then arrives
  by push whatever the screen shows (the editor may show a pinned version,
  or nothing), and what stood on it can be marked."
  [r things]
  (swap! (:held r) into things)
  (hold! r))

(defn set-context!
  "The session's context layers (Inland names, nearest first), for deciding
  which fact a nearer layer now shadows. A layer joined later gets its held
  units."
  [r layers]
  (reset! (:context r) (vec layers))
  (hold! r))

(defn current-marks
  "The marks whose dependent fact still heads its (thing, key) in the unit
  the session holds open on it: a selection made again, a pin let go, a
  variation kept again drop out. A mark on a thing no open unit holds stays,
  since nothing the screen shows says it moved."
  [r]
  (let [units (vals @(:units r))]
    (into {} (filter (fn [[fid mk]]
                       (let [u (some #(when (and (= (:layer mk) (:layer %)) (= [:e (:e mk)] (:pattern %))) %) units)]
                         (or (nil? u) (= fid (:fid (get (f/heads (vals (:rows u))) (:k mk)))))))
                     @(:marks r)))))

(defn on-marks!
  "Call `f` with the current marks after every flush that showed something,
  and once now (on the reader's worker)."
  [r f]
  (reset! (:on-marks r) f)
  (run-on r #(f (current-marks r))))

;; ------------------------------------------------------------ the flush

(defn- spec-of [r unit]
  (merge (:who r) {:layer (:layer unit) :pattern (:pattern unit) :limit unit-limit}))

(defn- cell-row?
  "A row of a page's stored cell, its key in the `cell` namespace."
  [row]
  (= "cell" (some-> (:k row) namespace)))

(defn- same-value?
  "Whether two rows say the same value: a fact replaced or shadowed by an
  equal one leaves nothing that stood on it stale. An erased row says none."
  [a b]
  (and (contains? a :value) (contains? b :value) (= (:value a) (:value b))))

(defn- changed-facts
  "The facts new rows change for this session, each with what it was and
  what changed it: a fact a new row replaces in its own layer (not a page's
  stored cell), and the head in a farther context layer of the (thing, key)
  a new row now says in a nearer one; in both, only when the new row's value
  differs. `{fid {:e :k :layer :now :by}}`. `units` are the reader's units
  with the new rows in; `fresh` the new rows by layer."
  [r units fresh]
  (let [ctx (mapv f/layer-id @(:context r))
        rank (zipmap ctx (range))
        known (into {} (mapcat (comp seq :rows)) (vals units))
        heads-in (fn [layer e]
                   (some (fn [u] (when (and (= layer (:layer u)) (= [:e e] (:pattern u)))
                                   (f/heads (vals (:rows u)))))
                         (vals units)))]
    (into {}
          (concat
           (for [[layer rows] fresh, row rows, :let [old (:replaces row)]
                 :when (and old (not (cell-row? row)) (not (same-value? row (get known old))))]
             [old {:e (:e row) :k (:k row) :layer layer :now (:fid row) :by layer
                   :stamp (get-in known [old :stamp])}])
           (for [[layer rows] fresh
                 :let [near (get rank layer)] :when near
                 row rows
                 :when (and (:k row) (not (f/namespace-of-read? (:k row))))
                 farther (subvec ctx (inc near))
                 :let [h (get (heads-in farther (:e row)) (:k row))]
                 :when (and h (:fid h) (not (same-value? row h)))]
             [(:fid h) {:e (:e row) :k (:k row) :layer farther :now (:fid row) :by layer :stamp (:stamp h)}])))))

(defn- mark-rows
  "Marks from a `[:dependents F]` answer: each dependent fact, the layer it
  is in, the fact `F` it stood on (a row's `:on` is the set of the facts the
  dependent act named), and what `F` was and what changed it (`changed`).
  The act that made the change is no mark: an edit stands on what it
  replaces."
  [answer layer changed F]
  (let [was (get changed F)
        changer (first (:now was))]
    (into {} (for [row (:rows answer) :when (and (:fid row) (not= changer (first (:fid row))))]
               [(:fid row) {:layer layer :fid (:fid row) :e (:e row) :k (:k row) :stamp (:stamp row)
                            :on F :on-stamp (:stamp was)
                            :on-e (:e was) :on-k (:k was) :on-layer (:layer was) :by (:by was)}]))))

(defn flush!
  "One cause's reads, on the reader's worker: open what is waiting, deliver
  what rang, re-read by id what a forget may have erased, read what stood on
  each fact the new rows change, close what nobody watches; then ONE act
  for every line and every read; then, only on its yes, the rows to the
  watchers. A refused act shows nothing: an opening unit is marked failed,
  a delivery stays undelivered and is delivered again at the next ring."
  [r]
  (when-not @(:closed? r)
    (let [{:keys [store who]} r
          units @(:units r)
          dirty @(:dirty r)
          _ (reset! (:dirty r) #{})
          now (System/currentTimeMillis)
          working (:working who)
          classes (memoize (fn [layer] (mc/layer-class store layer)))
          entry (fn [] (reads/entry-entity (env/make-name working (classes working))))
          ;; opens: a standing read, or for a pinned version one read as of its stamp
          opened (vec (for [u (vals units) :when (and (= :opening (:status u)) (nil? (:as-of u)))]
                        [u (sg/open-lines store (spec-of r u) (entry))]))
          fixed (vec (for [u (vals units) :when (and (= :opening (:status u)) (:as-of u))]
                       [u (rx/checked-query store who {:layer (:layer u) :read [:pattern (:pattern u)]
                                                       :as-of {:stamp (:as-of u)}})]))
          ;; deliveries, on the layers that rang, watched or not (a re-watched unit must be current)
          delivered (vec (for [u (vals units)
                               :when (and (= :open (:status u)) (nil? (:as-of u)) (contains? dirty (:layer u)))]
                           [u (sg/delivery-lines store (:state u))]))
          ;; a re-classed layer: close :reclass and open again, in this same act (F18)
          reclassed (vec (for [[u d] delivered :when (:reclassed d)]
                           [u (sg/close-lines store (:spec (:state u)) (:ent (:state u)) :reclass)
                            (sg/open-lines store (spec-of r u) (entry))]))
          ;; closes: unwatched past the grace, and not delivered in this act; a pinned
          ;; version, a refused or a failed unit has no standing entry to close
          idle? (fn [u] (and (empty? (:watchers u)) (:idle-since u) (<= close-grace-ms (- now (:idle-since u)))))
          closed (vec (for [u (vals units)
                            :when (and (= :open (:status u)) (nil? (:as-of u)) (idle? u)
                                       (not (contains? dirty (:layer u))))]
                        [u (sg/close-lines store (:spec (:state u)) (:ent (:state u)) :unsubscribe)]))
          dropped (vec (filter #(and (idle? %) (or (:as-of %) (#{:refused :failed} (:status %)))) (vals units)))
          ;; a forget rang: every value shown, by id
          erasure-reads (when (contains? dirty :erasures)
                          (vec (for [[layer pairs] (->> (vals units) (filter #(= :open (:status %)))
                                                        (mapcat (fn [u] (for [row (vals (:rows u)) :when (value-row? row)]
                                                                          [(:layer u) (:fid row)])))
                                                        (group-by first))
                                     page (partition-all point-page (distinct (map second pairs)))]
                                 (rx/checked-query store who {:layer layer :read [:point (vec page)]}))))
          ;; what the new rows change, and what stood on it
          fresh (merge-with into
                            (apply merge-with into {} (for [[u o] opened :when (:facts o)] {(:layer u) (vec (:rows o))}))
                            (apply merge-with into {} (for [[u d] delivered :when (:facts d)] {(:layer u) (vec (:rows d))}))
                            (apply merge-with into {} (for [[u _ o] reclassed :when (:facts o)] {(:layer u) (vec (:rows o))})))
          units-after (merge units
                             (into {} (for [[u o] opened :when (:facts o)]
                                        [(:key u) (assoc u :rows (rows-by-fid (:rows o)))]))
                             (into {} (for [[u d] delivered :when (:facts d)]
                                        [(:key u) (update u :rows merge (rows-by-fid (:rows d)))])))
          changed-map (changed-facts r units-after fresh)
          changed (vec (remove @(:looked r) (keys changed-map)))
          dependent-reads (vec (for [F changed layer (distinct [working (:own r)])]
                                 [layer F (rx/checked-query store who {:layer layer :read [:pattern [:dependents F]]})]))
          ;; the one act
          entry-facts (fn [[spec answer]]
                        (when-not (contains? answer :refused)
                          (reads/entry-facts answer (assoc spec :entry-name (env/make-name working (classes working))))))
          facts (vec (concat (mapcat (comp :facts second) opened)
                             (mapcat (fn [[_ q]] (entry-facts q)) fixed)
                             (mapcat (comp :facts second) delivered)
                             (mapcat (fn [[_ c o]] (concat c (:facts o))) reclassed)
                             (mapcat second closed)
                             (mapcat entry-facts erasure-reads)
                             (mapcat (fn [[_ _ q]] (entry-facts q)) dependent-reads)))
          answer (when (seq facts) (sg/offer-lines! store who facts))
          yes? (or (empty? facts) (= :yes (:answer answer)))]
      ;; nothing new moves only the cursor; it needs no act
      (doseq [[u d] delivered :when (:nothing-new d)]
        (swap! (:units r) assoc-in [(:key u) :state] (:state d)))
      (doseq [u dropped] (swap! (:units r) dissoc (:key u)))
      (if-not yes?
        (doseq [[u _] (concat opened fixed)]
          (swap! (:units r) assoc (:key u) (assoc u :status :failed :reason (or (:reason answer) :entry-refused)))
          (notify! (get @(:units r) (:key u))))
        (do
          (doseq [[u [_ answer]] fixed]
            (swap! (:units r) assoc (:key u)
                   (if (contains? answer :refused)
                     (assoc u :status :refused :reason (:refused answer))
                     (assoc u :status :open :rows (rows-by-fid (:rows answer)))))
            (notify! (get @(:units r) (:key u))))
          (doseq [[u o] opened]
            (if (:refused o)
              (swap! (:units r) assoc (:key u) (assoc u :status :refused :reason (:refused o)))
              (let [class (classes (:layer u))]
                (swap! (:units r) assoc (:key u) (assoc u :status :open :state (:state o)
                                                       :shared? (= :by-entity class)
                                                       :rows (rows-by-fid (:rows o))))
                (hear! store r (bell-for class (:layer u)))))
            (notify! (get @(:units r) (:key u))))
          (doseq [[u d] delivered :when (:facts d)]
            (swap! (:units r) update (:key u) #(-> % (assoc :state (:state d)) (update :rows merge (rows-by-fid (:rows d)))))
            (notify! (get @(:units r) (:key u))))
          (doseq [[u _ o] reclassed]
            (unhear! r [:layer (:layer u)])
            (if (:refused o)
              (swap! (:units r) assoc (:key u) (assoc u :status :refused :reason (:refused o)))
              (do (swap! (:units r) assoc (:key u) (assoc u :status :open :state (:state o) :shared? true
                                                         :rows (rows-by-fid (:rows o))))
                  (hear! store r [:frontier])))
            (notify! (get @(:units r) (:key u))))
          (doseq [[u _] closed]
            (swap! (:units r) dissoc (:key u))
            (when-not (some #(= (:layer u) (:layer %)) (vals @(:units r)))
              (unhear! r (bell-for (classes (:layer u)) (:layer u)))))
          (doseq [[spec answer] erasure-reads :when (not (contains? answer :refused))]
            (let [by-fid (rows-by-fid (:rows answer))]
              (doseq [u (vals @(:units r)) :when (= (:layer spec) (:layer u))]
                (let [hit (select-keys by-fid (keys (:rows u)))]
                  (when (some (fn [[fid row]] (not= row (get-in u [:rows fid]))) hit)
                    (swap! (:units r) update-in [(:key u) :rows] merge
                           (into {} (for [[fid row] hit]
                                      [fid (cond-> (merge (get-in u [:rows fid]) row)
                                             (contains? row :erased-at) (dissoc :value))])))
                    (notify! (get @(:units r) (:key u))))))))
          (when (seq changed)
            (swap! (:looked r) into changed)
            (swap! (:marks r) merge (apply merge {} (for [[layer F [_ a]] dependent-reads :when (not (contains? a :refused))]
                                                      (mark-rows a layer changed-map F)))))
          (when (seq facts)
            (when-let [cb @(:on-marks r)] (cb (current-marks r)))))))))

(defn close!
  "Stop the reader: close every standing read in one act, stop hearing the
  bells, stop the worker."
  [r]
  (run-on r
    (fn []
      (let [{:keys [store who]} r
            open (filter #(= :open (:status %)) (vals @(:units r)))
            facts (vec (mapcat #(sg/close-lines store (:spec (:state %)) (:ent (:state %)) :session-close) open))]
        (when (seq facts) (try (sg/offer-lines! store who facts) (catch Throwable _ nil)))
        (reset! (:closed? r) true)
        (doseq [[key {:keys [readers]}] @bells :when (contains? readers r)] (unhear! r key))
        (.shutdown ^ScheduledExecutorService (:exec r))))))
