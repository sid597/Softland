(ns softland.inland.store
  "The rig's store behind Inland: the source-flow boundary Electric reads
  through, and every write the screen makes. Takes Inland's read addresses,
  gestures and admission requests; gives tagged flows, gesture outcomes and
  admission decisions. Owns the process's handles on the rig's door and one
  read exit, and one session in the store per browser page; borrows the
  store's persons, layers and groups from `people`.

  Replaces Inland's own Rama module as this namespace's authority (the rig's
  module, rig.store.module/Store, is what `bin/inland` deploys). The names
  Inland's other namespaces call stay: `connect!`, `watch-path`,
  `submit-result!`, `ensure-workspace!`, `read-one`, `metrics-snapshot`.

  - Reads: every read is through the one exit and recorded. A view's reads
    go through `reader` (standing reads, one act per cause, pushed by the
    store); a gesture's through `gesture` (one act per gesture). Nothing a
    read gave is shown before its act is answered yes.
  - Writes: every act names `:who` the person, `:session` their browser
    session in the store and its permission in the layer, and stands on
    every fact the gesture that decided it read. No write is shown
    optimistically: the screen shows it once a push delivers it.
  - Records are facts (`facts`): one per attribute, the thing's id the
    keyword of the record's name; a stored cell is a fact on the session's
    thing in the person's hand layer.

  Vocabulary: \"key\" is a fact's key; \"lock\" is an encryption key."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [com.rpl.rama :as r]
            [missionary.core :as m]
            [rig.store.bell :as bell]
            [rig.store.client :as c]
            [rig.store.clock :as clock]
            [rig.store.micro-client :as mc]
            [rig.store.promote-client :as pc]
            [rig.store.read-exit :as rx]
            [softland.inland.facts :as f]
            [softland.inland.gesture :as gesture]
            [softland.inland.people :as people]
            [softland.inland.reader :as reader]
            [softland.inland.resident :as resident]
            [softland.inland.seed :as seed]
            [softland.inland.total :as total])
  (:import [java.time Instant ZoneId]
           [java.time.format DateTimeFormatter]))

(defonce connection (atom nil))
(defonce sessions (atom {}))
(defonce metrics (atom {}))
(defonce test-hold (atom nil))
(defonce workspaces (atom #{"workbench"}))

(def group-layers
  "The group layers this screen offers (placeholder: one, the pair; a group's
  id is a layer id the host names)."
  [:pair])

(def stored-cells
  "The session cells the store holds (placeholder): what a gesture chose
  and what is pinned, so what stood on a changed fact can be found. Every
  other cell stays a draft or attention in the browser."
  #{"selection" "context"})

(defn count!
  "Owner and metric key → updated diagnostic map, incrementing that counter."
  [owner key]
  (swap! metrics update-in [owner key] (fnil inc 0)))

(defn metrics-snapshot
  "Retained counters → per-owner totals and printable path details."
  []
  (into {} (for [[owner rows] @metrics]
             [owner (reduce (fn [out [[kind path] count]]
                              (-> out (update kind (fnil + 0) count)
                                  (update :paths conj {:kind kind :path (pr-str path) :count count})))
                            {:opened 0 :closed 0 :changes 0 :paths []} rows)])))

(defn- the-store [] (:store @connection))

;; ================================================================ the connection

(defn connect-to!
  "A cluster holding the rig's module (Inland's local cluster, or an
  in-process one in a test) → the rig's door, read exit and promotion
  handles, the store's first facts and the workbench's genesis records when
  they are not in, and one key per task for the bells."
  [cluster]
  (let [store (merge (rx/connect cluster) (pc/connect cluster))]
    ;; the first launch: the store's first facts, then the workbench's genesis records in the base
    (when (people/ensure-world! store)
      (seed/put-genesis! store))
    (reset! connection {:manager cluster :store store :task-keys (bell/task-keys store)})))

(defn connect!
  "Inland's local isolated cluster (bin/inland) → `connect-to!` it. Called
  once at process start."
  []
  (connect-to! (r/open-cluster-manager
                {"conductor.host" "localhost" "conductor.port" 1997
                 "zookeeper.port" 2217 "zookeeper.root" "inland-electric"})))

;; ================================================================ dates

(def ^:private day (.withZone (DateTimeFormatter/ofPattern "yyyy-MM-dd") (ZoneId/systemDefault)))

(defn date-of
  "A hybrid stamp's day, as the screen says an erasure's date."
  [stamp]
  (when (int? stamp) (.format ^DateTimeFormatter day (Instant/ofEpochMilli (clock/ms-of stamp)))))

;; ================================================================ sessions

(defn- groups-of [p]
  (people/groups-of (the-store) p (filter #(some? (mc/settings-of (the-store) %)) group-layers)))

(defn default-context
  "A person's context: their own layer, their groups, then the base
  (Inland's layer names, nearest first)."
  [p groups]
  {:layers (vec (concat [(f/layer-name (people/own-of p))] (map f/layer-name groups) ["base"])) :pins {}})

(declare cell-write!)

(defn open-session!
  "Electric's page owner and the logged-in person → their session in the
  store and its reader, once per page. Writes the stored context cell."
  [owner person]
  (or (get @sessions owner)
      (let [{:keys [store task-keys]} @connection
            groups (groups-of person)
            s (people/open-session! store person groups)
            rd (reader/create store {:who (:who s) :own (:own s) :people people/people :task-keys task-keys})
            ctx (default-context person groups)
            sess (assoc s :owner owner :reader rd :groups (atom (set groups)) :writes (atom (:writes s)))]
        (swap! sessions assoc owner sess)
        (reader/set-context! rd (:layers ctx))
        (cell-write! sess {"context" ctx} {} nil)
        sess)))

(defn session-of [owner] (get @sessions owner))

(defn page-open!
  "A page's session opened for the person the host logged in, and what the
  page needs of it: the address of its stored cells (the session's thing in
  its hand layer), which keys are stored, and the person's name. Throws for
  a page with no person: the host refuses its connection first."
  [owner person]
  (when-not (keyword? person) (throw (ex-info "No person is logged in for this page." {})))
  (let [sess (open-session! owner person)]
    {:thing (f/record-name (:S sess)) :hand (f/layer-name (:hand sess)) :stored stored-cells
     :person (name person)}))

(defn close-session!
  "The page is gone: close its reader (its standing reads close in one act)."
  [owner]
  (when-let [sess (get @sessions owner)]
    (swap! sessions dissoc owner)
    (reader/close! (:reader sess))))

(defn- permission-in
  "The session's permission in layer `L`, granted now when it has none there
  (a group joined after the page opened). Nil when the store refuses it."
  [sess L]
  (or (get @(:writes sess) L)
      (let [a (people/grant-session! (the-store) (:person sess) (:S sess) L)]
        (when (= :yes (:answer a))
          (let [pid (people/session-permission (:person sess) (:S sess) L (people/layer-kind (the-store) L))]
            (swap! (:writes sess) assoc L pid)
            pid)))))

;; ================================================================ reads for views

(defn- split-key
  "Inland's row key `\"layer/name\"` → [layer name]; a name may hold slashes."
  [key]
  (let [i (str/index-of key "/")] [(subs key 0 i) (subs key (inc i))]))

(defn- attr-value
  "An attribute path of a record; a forgotten attribute is `{:erased-at day}`."
  [record path]
  (when record
    (if-let [date (get (:erased record) (first path))]
      {:erased-at (date-of date)}
      (get-in record path))))

(defn address
  "An Inland read address → the read units it needs (`[layer pattern]`, or
  `[layer pattern revision]` for a pinned version) and `:value`, from their
  rows (unit → fid → row) to what Inland reads there."
  [kind path]
  (let [[_ key & attr] path]
    (case kind
      :rows (let [[L name] (split-key key)
                  u [(f/layer-id L) [:e (f/entity name)]]]
              {:units [u]
               :value (fn [rows-of] (let [rec (f/record-in name L (vals (get rows-of u)))]
                                      (if (seq attr) (attr-value rec (vec attr)) rec)))})
      :versions (let [[L name rev] (edn/read-string key)
                      u [(f/layer-id L) [:e (f/entity name)] rev]]
                  {:units [u]
                   :value (fn [rows-of] (let [rec (f/record-in name L (vals (get rows-of u)))]
                                          (if (seq attr) (attr-value rec (vec attr)) rec)))})
      :index (let [[L bucket] (split-key key)
                   ps (f/bucket-patterns bucket)
                   us (mapv #(vector (f/layer-id L) %) ps)]
               {:units us
                :value (fn [rows-of] (f/bucket-names bucket (into {} (for [[u p] (map vector us ps)] [p (vals (get rows-of u))]))))})
      {:units [] :value (constantly nil)})))

(defn- unit-status
  "Every watched unit's state → the tagged value Inland reads."
  [value units-now]
  (let [states (vals units-now)]
    (cond
      (some #(= :failed (:status %)) states)
      {:status :failed :reason "The read's record was refused; nothing was shown."}
      (some #(= :refused (:status %)) states)
      {:status :failed :reason (str "The store refused the read: "
                                    (some #(when (= :refused (:status %)) (some-> (:reason %) name)) states))}
      :else
      (let [v (value (into {} (for [[k u] units-now] [k (:rows u)])))]
        (if (nil? v) {:status :absent :complete? true} {:status :value :value v :complete? true})))))

(defn watch-path
  "Owner and read address → continuous tagged value, complete absence or
  failure. Each Electric demand watches the reader's units under it; the
  reader gives rows only once their read is recorded and delivers only on
  the store's push. Errors are failed status values, not accepted data."
  [owner kind path]
  (m/eduction
    (dedupe)
    (m/relieve
      (fn [_ next] next)
      (m/observe
        (fn [emit]
          (let [sess (session-of owner)
                {:keys [units value]} (address kind path)
                key [kind path]]
            (count! owner [:opened key])
            (if (or (nil? sess) (empty? units))
              (do (emit {:status :failed :reason (if sess "This address is not read here." "No session for this page.")})
                  #(count! owner [:closed key]))
              (let [lock (Object.) !units (atom {})
                    stops (mapv (fn [u]
                                  (reader/watch! (:reader sess) u
                                                 (fn [unit]
                                                   (locking lock
                                                     (swap! !units assoc u unit)
                                                     (count! owner [:changes key])
                                                     (when (= (count units) (count @!units))
                                                       (emit (unit-status value @!units)))))))
                                units)]
                (fn [] (run! #(%) stops) (count! owner [:closed key]))))))))))

(defn read-one
  "A synchronous read for the host's own diagnostics (test controls): the
  store's own view (rig.store.client `read-as-of`), recorded nowhere and
  never shown to a person."
  [kind path]
  (let [{:keys [units value]} (address kind path)
        store (the-store)]
    (value (into {} (for [[L pattern] units]
                      [[L pattern] (into {} (for [fact (:facts (c/read-as-of store L (c/clock store L)))
                                                  :when (= (second pattern) (:e fact))]
                                              [(:id fact) (assoc fact :fid (:id fact))]))])))))

(defn registered-workspaces [] @workspaces)

(defn ensure-workspace!
  "Workspace name → accepted: the store's first facts are made at connect."
  [workspace]
  (swap! workspaces conj workspace)
  {:status :accepted :request-id "world" :name "world"})

;; ================================================================ writes

(defn- act!
  "One act by the session's person into layer `L`, under the session's
  permission there, standing on `stood-on`, through the door of the gate
  that orders `L`. The answer, with `:name` the act's."
  [sess L facts stood-on]
  (if-let [pid (permission-in sess L)]
    (let [o (c/build {:who (:person sess) :layer L :class (mc/layer-class (the-store) L)
                      :permission pid :session (:S sess) :stood-on (or stood-on {}) :facts facts})]
      (assoc (mc/offer-into! (the-store) o) :name (:name o)))
    {:answer :no :reason :no-permission-here}))

(defn- operator-act!
  "An operator act into layer `L` (the resident's observations: the executor is
  operator code), standing on `stood-on`. The answer, with `:name`."
  [L facts stood-on]
  (let [o (c/build {:who :operator :layer L :class (mc/layer-class (the-store) L)
                    :stood-on (or stood-on {}) :facts facts})]
    (assoc (mc/offer-into! (the-store) o) :name (:name o))))

(defn- heads-of
  "The heads of the session's thing in its hand layer, as a gesture reads
  them (recorded with it): `{key {:fid :stamp :layer}}`."
  [g sess]
  (let [rec (gesture/resolve-record g [(f/layer-name (:hand sess))] (f/record-name (:S sess)) nil)]
    (when-not (total/blocked? rec) (:basis rec))))

(defn cell-write!
  "Stored cells written as one act on the session's thing in its hand
  layer, each replacing the cell's head there; `g` the gesture that read
  them, or nil at the session's start, which reads (and records) the cells
  itself."
  [sess writes stood-on g]
  (let [store (the-store)
        basis (if g
                (heads-of g sess)
                (let [spec {:layer (:hand sess) :read [:pattern [:e (:S sess)]]}
                      [s answer] (rx/checked-query store (:who sess) spec)]
                  (rx/record! store (:who sess) [[s answer]])
                  (into {} (for [[k row] (f/heads (:rows answer))] [k {:fid (:fid row)}]))))
        facts (vec (for [[key v] writes
                         :let [k (f/cell-key key) from (get basis k)]]
                     (cond-> {:e (:S sess) :k k}
                       (some? v) (assoc :v v)
                       from (assoc :replaces (:fid from)))))]
    (when (seq facts) (act! sess (:hand sess) facts stood-on))))

(defn- first-layer [context] (first (:layers context ["base"])))

(defn- put-record!
  "A record put into layer `L` (Inland's `:put`): the facts that turn the
  record there into `after`, when the revision the person saw is still the
  one `expected-revision` names. What they saw is the record as their
  context resolves it when `L` is their nearest layer, else the record in
  `L` (a candidate layer, a group)."
  [sess g context {:keys [name layer expected-revision source row basis]} stood-on]
  (let [L (or layer (first-layer context))
        parsed (if source (f/parse-source source) {:value row})
        after (some-> (:value parsed) (assoc :name name) (cond-> basis (assoc :candidate-basis basis)))
        before (gesture/resolve-record g [L] name nil)
        seen (if (= L (first-layer context)) (gesture/resolve-record g (:layers context ["base"]) name nil) before)]
    (cond
      (:error parsed) {:status :rejected :reason (:error parsed)}
      (total/blocked? before) {:status :rejected :reason (:reason before)}
      (total/blocked? seen) {:status :rejected :reason (:reason seen)}
      (total/row-error after) {:status :rejected :reason (total/row-error after)}
      (and (some? expected-revision) (not= expected-revision (:revision seen 0)))
      {:status :rejected :reason "The accepted revision changed. Load it before applying this draft."}
      :else
      (let [{:keys [facts]} (f/changes (f/layer-id L) before (f/authored after) (:basis before))]
        (if (empty? facts)
          {:status :accepted :reason "Nothing changed." :name name :layer L}
          (let [a (act! sess (f/layer-id L) facts stood-on)]
            (if (= :yes (:answer a))
              {:status :accepted :revision (:stamp a) :name name :layer L}
              {:status :rejected :reason (str "The store refused the act: " (some-> (:reason a) clojure.core/name))})))))))

(defn- retract-record!
  "Every fact of record `name` in layer `layer` retracted (Inland's
  `:delete-override`): a fact with no value replacing each head there."
  [sess g {:keys [name layer]} stood-on]
  (let [before (gesture/resolve-record g [layer] name nil)]
    (if (or (nil? before) (total/blocked? before))
      {:status :rejected :reason "There is no such record in that layer."}
      (let [heads (for [[k b] (:basis before) :when (= layer (:layer b))] [k b])
            a (act! sess (f/layer-id layer) (vec (for [[k b] heads] {:e (f/entity name) :k k :replaces (:fid b)})) stood-on)]
        (if (= :yes (:answer a)) {:status :accepted} {:status :rejected :reason (str (:reason a))})))))

(defn- promote-record!
  "Record `name` shared from layer `from-layer` into layer `layer`
  (Inland's `:promote`): each of its facts in the source promoted by the
  store's promotion (the request in the owner's layer, the crossing, the
  landing in the target), each landing replacing the target's head for its
  key. The requests are answered at once; each landing shows when it lands,
  and the screen says it is pending until then."
  [sess g {:keys [name from-layer layer]}]
  (let [src (gesture/resolve-record g [from-layer] name nil)
        dst (gesture/resolve-record g [layer] name nil)
        from (f/layer-id from-layer) to (f/layer-id layer)
        landing-permission (permission-in sess to)
        permission (permission-in sess from)]
    (cond
      (or (nil? src) (total/blocked? src)) {:status :rejected :reason "There is nothing of that record to share in your layer."}
      (total/blocked? dst) {:status :rejected :reason (:reason dst)}
      (nil? landing-permission) {:status :rejected :reason "You may not write that layer."}
      :else
      (let [done (vec (for [[k b] (:basis src) :when (= from-layer (:layer b))]
                        (pc/promote! (the-store)
                                     {:who (:person sess) :layer from :permission permission :session (:S sess)
                                      :source (:fid b) :source-stamp (:stamp b) :source-e (f/entity name)
                                      :target to :landing-permission landing-permission
                                      :replaces (get-in dst [:basis k :fid])})))]
        {:status (if (every? #(#{:pending :crossed :done} (:status %)) done) :accepted :rejected)
         :reason (some :statement done)
         :promotion (mapv #(select-keys % [:name :status :statement]) done)}))))

(defn- note!
  "A note on the material (Inland's `:note` admission): a record whose
  `:note` value says what it is on, its text and whom it names, in layer
  `layer` (default the nearest). The note's grammar there makes the people
  it names its subjects."
  [sess context {:keys [on text names layer]} stood-on]
  (let [L (f/layer-id (or layer (first-layer context)))
        id (str "note-" (subs (str (random-uuid)) 0 8))
        v {:on on :text (str text) :names (set (map keyword names))}
        a (act! sess L [{:e (f/entity id) :k :name :v id} {:e (f/entity id) :k :note :v v}] stood-on)]
    (if (= :yes (:answer a))
      {:status :accepted :name id :layer (f/layer-name L)}
      {:status :rejected :reason (str "The store refused the note: " (some-> (:reason a) name))})))

(defn- group!
  "A group made on the person's asking, with them its first member."
  [sess {:keys [group]}]
  (let [L (keyword group)]
    (if-not (some #{L} group-layers)
      {:status :rejected :reason "This screen offers no such group."}
      (let [a (people/make-group! (the-store) L (:person sess))]
        (if (= :yes (:answer a))
          (do (swap! (:groups sess) conj L) {:status :accepted :group group})
          {:status :rejected :reason (str "The store refused the group: " (some-> (:reason a) name))})))))

(defn- member!
  "A person accepted into a group on a member's asking."
  [sess {:keys [group person]}]
  (let [a (people/add-member! (the-store) (keyword group) (:person sess) (keyword person))]
    (if (= :yes (:answer a))
      {:status :accepted}
      {:status :rejected :reason (str "Not accepted: " (some-> (:reason a) name))})))

(defn- start-activity!
  "An asked question (Inland's `:start`): the activity as a record in the
  person's nearest layer, then handed to the resident, whose running and
  outcome the operator writes, each replacing the status before it. The
  reply is a stand-in until Sid says yes (resident `live?`)."
  [sess g context {:keys [name intent]} stood-on]
  (let [layer (first-layer context)
        L (f/layer-id layer)
        e (f/entity name)
        existing (gesture/resolve-record g [layer] name nil)
        input (:input intent)]
    (cond
      (total/blocked? existing) {:status :rejected :reason (:reason existing)}
      existing {:status :rejected :reason "An activity with this identity already exists."}
      (not (and (= :claude (:runner intent)) (string? input) (<= 1 (count input) 2000)
                (int? (:max-output intent)) (<= 1 (:max-output intent) 400)))
      {:status :rejected :reason "The resident needs a 1–2000 character request and a maximum of 400 output tokens."}
      :else
      (let [a (act! sess L [{:e e :k :name :v name} {:e e :k :activity :v intent}
                            {:e e :k :status :v :pending} {:e e :k :catalog :v "activities"}]
                    stood-on)]
        (if-not (= :yes (:answer a))
          {:status :rejected :reason (str "The store refused the activity: " (some-> (:reason a) clojure.core/name))}
          (let [!head (atom [[(:name a) 2] (:stamp a)])]
            (resident/execute!
             intent
             (fn [status fields]
               (let [[fid stamp] @!head
                     facts (cond-> (into [{:e e :k :status :v status :replaces fid}]
                                         (for [[k v] fields :when (#{:reply :reason :provider-result} k)] {:e e :k k :v v}))
                             (:reply fields) (conj {:e e :k :provenance :v :machine}))
                     o (operator-act! L facts {fid stamp})]
                 (when (= :yes (:answer o)) (reset! !head [[(:name o) 0] (:stamp o)])))))
            {:status :accepted :name name}))))))

(defn- cancel-activity!
  "A cancel of the person's activity (Inland's `:cancel`): a pending one is
  cancelled; a running one becomes unconfirmed, since its call may have been
  made; a terminal one stays."
  [sess g context {:keys [name]} stood-on]
  (let [layer (first-layer context)
        rec (gesture/resolve-record g [layer] name nil)]
    (if (and rec (not (total/blocked? rec)) (#{:pending :running} (:status rec)))
      (let [e (f/entity name)
            a (act! sess (f/layer-id layer)
                    [{:e e :k :status :v (if (= :pending (:status rec)) :cancelled :unconfirmed)
                      :replaces (get-in rec [:basis :status :fid])}
                     {:e e :k :reason :v "Cancellation requested; a possibly performed external call is not retried."}]
                    stood-on)]
        (if (= :yes (:answer a)) {:status :accepted} {:status :rejected :reason (str (:reason a))}))
      {:status :rejected :reason "This activity is already terminal."})))

(defn admit!
  "One admission request a gesture decided, after the gesture's reads are
  recorded: the act or acts it makes, standing on everything the gesture
  read."
  [sess g context request stood-on]
  (case (:kind request)
    :start (start-activity! sess g context request stood-on)
    :cancel (cancel-activity! sess g context request stood-on)
    :put (put-record! sess g context request stood-on)
    :remove (put-record! sess g context (-> request (assoc :row {:removed true}) (dissoc :source)) stood-on)
    :delete-override (retract-record! sess g request stood-on)
    :promote (promote-record! sess g request)
    :note (note! sess context request stood-on)
    :group (group! sess request)
    :member (member! sess request)
    {:status :rejected :reason "No admission capability answers this request."}))

;; ================================================================ gestures

(defn- context-of
  "The session's context as the gesture reads it (the stored cell); its
  default only when the cell holds none. A refused read stays a refusal."
  [g sess]
  (let [v (gesture/cell g "context")]
    (cond (total/blocked? v) v
          (and (map? v) (seq (:layers v))) v
          :else (default-context (:person sess) @(:groups sess)))))

(defn- read-for-writes!
  "The reads the effects' writes will need, made in the gesture so its one
  record holds them: the cells' heads for stored cells, the record in the
  layer an admission writes and as the context resolves it."
  [g sess context effects]
  (doseq [e effects]
    (case (:effect e)
      :session (when (some stored-cells (keys (:writes e))) (heads-of g sess))
      :admit (let [rq (:request e)]
               (case (:kind rq)
                 (:put :remove :delete-override)
                 (do (gesture/resolve-record g [(or (:layer rq) (first-layer context))] (:name rq) nil)
                     (gesture/resolve-record g (:layers context ["base"]) (:name rq) nil))
                 (:start :cancel)
                 (gesture/resolve-record g [(first-layer context)] (:name rq) nil)
                 :promote
                 (do (gesture/resolve-record g [(:from-layer rq)] (:name rq) nil)
                     (gesture/resolve-record g [(:layer rq)] (:name rq) nil))
                 nil))
      nil)))

(defn gesture!
  "One gesture, once: the rules its kind indexes run with their reads
  through the exit; ONE act records every read; only on its yes do its
  effects run. Stored cells and admissions are written here, standing on
  what the gesture read; every other effect returns to the page to apply.
  `cells` are the page's local cells. Returns `{:effects [...] :recorded
  name :decisions [...] :supports [...]}`, or `{:failure {:reason ..}}`."
  [owner event cells]
  (let [sess (session-of owner)]
    (if-not sess
      {:failure {:reason "No session for this page."}}
      (let [g (gesture/begin (the-store) {:who (:who sess) :S (:S sess) :hand (f/layer-name (:hand sess))
                                          :stored-cells stored-cells :cells cells})
            ctx (context-of g sess)
            context (when-not (total/blocked? ctx) (merge ctx {:session owner :who (name (:person sess))}))
            results (if context (gesture/dispatch g context event) ctx)
            failure (or (when (total/blocked? results) results)
                        (total/first-failure results)
                        (some #(when-let [reason (total/effect-error (:value %))] {:runtime/status :failed :reason reason}) results))
            effects (when-not failure (vec (mapcat :value results)))
            _ (read-for-writes! g sess context effects)
            rec (gesture/record! g)]
        (cond
          (not (#{:yes :none} (:answer rec)))
          {:failure {:reason (str "The gesture's reads were not recorded: " (some-> (:reason rec) name))}}

          failure {:failure {:reason (:reason failure)} :recorded (:entry rec)}

          (empty? results)
          {:effects [] :recorded (:entry rec) :supports []
           :decisions [(admit! sess g context {:kind :put :name (str "unanswered-" (:id event)) :expected-revision 0
                                               :row {:catalog "unanswered" :event (dissoc event :run)}}
                               (gesture/stood-on g))]}

          :else
          (let [stood (gesture/stood-on g)
                stored-writes (into {} (for [e effects :when (= :session (:effect e))
                                             [k v] (:writes e) :when (contains? stored-cells k)] [k v]))
                local (vec (keep (fn [e]
                                   (case (:effect e)
                                     :session (let [w (into {} (remove #(contains? stored-cells (key %)) (:writes e)))]
                                                (when (seq w) (assoc e :writes w)))
                                     :admit nil
                                     e))
                                 effects))
                ;; admissions first: a cell may name what an admission makes
                decisions (vec (for [e effects :when (= :admit (:effect e))]
                                 (admit! sess g context (:request e) stood)))
                cell-answer (when (seq stored-writes) (cell-write! sess stored-writes stood g))]
            (when (contains? stored-writes "context")
              (reader/set-context! (:reader sess) (:layers (get stored-writes "context"))))
            {:effects local :recorded (:entry rec) :supports (mapv :support results)
             :decisions (cond-> decisions
                          (and cell-answer (not= :yes (:answer cell-answer)))
                          (conj {:status :rejected :reason (str "The store refused the cells: " (some-> (:reason cell-answer) name))}))}))))))

;; ================================================================ admissions from the page

(defn submit-result!
  "An admission request from the page's request slot (an activity's step):
  its reads recorded, then written standing on them. Decision, or
  unconfirmed on exception."
  [owner op]
  (try
    (if-let [sess (session-of owner)]
      (let [g (gesture/begin (the-store) {:who (:who sess) :S (:S sess) :hand (f/layer-name (:hand sess))
                                          :stored-cells stored-cells})
            ctx (context-of g sess)
            context (when-not (total/blocked? ctx) (merge ctx {:session owner}))
            _ (when context (read-for-writes! g sess context [{:effect :admit :request op}]))
            rec (gesture/record! g)]
        (if (and context (#{:yes :none} (:answer rec)))
          (assoc (admit! sess g context op (gesture/stood-on g)) :request-id (:request-id op) :name (:name op))
          {:request-id (:request-id op) :name (:name op) :status :rejected :reason "The request's reads were not recorded."}))
      {:request-id (:request-id op) :name (:name op) :status :rejected :reason "No session for this page."})
    (catch Throwable _
      {:request-id (:request-id op) :name (:name op) :status :unconfirmed
       :reason "Acceptance is unconfirmed. Retrieve this request's decision before retrying."})))

(defn marks-flow
  "The session's current marks as a flow: what stood on a fact the screen
  saw change, found by the store's lookup and recorded as read."
  [owner]
  (m/eduction
    (dedupe)
    (m/observe (fn [emit]
                 (if-let [sess (session-of owner)]
                   (do (reader/on-marks! (:reader sess) emit) #(reader/on-marks! (:reader sess) (fn [_])))
                   (do (emit {}) (fn [])))))))

(defn marks-view
  "Marks → what views read, in names and strings, so a view looks a mark up
  rather than searching: `{:cells {cell mark} :things {record mark}}`, the
  marks on this page's own stored cells by cell name (\"selection\",
  \"context\"), and the marks on records by record name. A mark says the
  thing and key that stood on a changed fact, the layer it is in, the thing,
  key and layer of the fact it stood on, the layer whose new fact changed
  it, and the day the old fact was written."
  [owner marks]
  (let [S (:S (session-of owner))]
    (reduce (fn [out [_ mk]]
              (let [m {:thing (some-> (:e mk) f/record-name) :key (some-> (:k mk) str (subs 1))
                       :layer (some-> (:layer mk) f/layer-name)
                       :on-thing (some-> (:on-e mk) f/record-name) :on-key (some-> (:on-k mk) str (subs 1))
                       :on-layer (some-> (:on-layer mk) f/layer-name) :by (some-> (:by mk) f/layer-name)
                       :on-date (date-of (:on-stamp mk))}]
                (if (= S (:e mk))
                  (assoc-in out [:cells (name (:k mk))] m)
                  (assoc-in out [:things (:thing m)] m))))
            {:cells {} :things {}}
            (sort-by (comp str key) marks))))

(defn forget-person!
  "The operator forgets person `p` (the host's command line). The answer."
  [p]
  (people/forget! (the-store) p))
