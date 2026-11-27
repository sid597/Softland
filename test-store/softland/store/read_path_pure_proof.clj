(ns softland.store.read-path-pure-proof
  "The proof of the read path's pure namespaces: the form of a query and its
  check, the lanes, the sources, the answer, the steps of a live read, and
  the words. It needs no cluster and no Rama. One command, from the
  repository root:

    clojure -M:store -m softland.store.read-path-pure-proof

  It runs on the alias that holds Clojure alone, so a pass also holds that
  the namespaces under `softland/store/read_path/` load with no Rama.
  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status
  that is not zero when a check fails.

  Approach. The tables are a value here: a map from a gate and a table to a
  sorted map from a row's key to the row, in the order of
  `softland.store.record.rows/before?`. A small function of the proof's own,
  `write`, stands for a gate: it puts the rows of one accepted act in those
  maps, with the rows of `softland.store.record.rows`, keeps the order by
  holder of permissions and the order by value of a marked key as a gate
  keeps them. A PState source is read from the maps by `table-value`, and a
  single-turn `lane-rights` query on a task by `run-rights`. No task is
  played: a source's routing id is not looked at when it is read, so a
  source of the base gives the same rows under every home, as the store's
  tables do when every task holds the base's rows. That every task holds
  them is the proof on the cluster's.

  The player. For a live read the proof holds a world: the tables; the
  writes that are still to come; for each push that the read holds, the
  value that the push holds, the changes that it has not taken, and the
  callbacks that are not handled; for each lane, its held `lane-rights`
  snapshot and any in-flight `lane-rights` query; the read's sources and its
  mark; and the asker's screen, which is the first answer, then each
  `{:changed}` put in and each `{:anew}` in the place of all. One move
  changes one thing: a write; a push takes its next value; a callback is
  handled; a push that the read asked for is served with the table as it is
  then; its arrival is handled; an in-flight `lane-rights` query is served
  from the tables as they are then; its answer arrives. Callbacks and
  arrivals are handled by the functions that `softland.store.read-path`
  calls for them, in its order: `softland.store.read-path.sources/named`,
  `reading`, `moves` and `source-matches-snapshot?`, and
  `softland.store.read-path.answer/meanings`, `after-change`,
  `after-arrival` and `after-rights`. The player plays EVERY order of the
  moves, and holds, at each end, the screen against what a read asked once
  gives from the tables as they are then.

  State. The proof owns one atom, the lines that it showed. Its row makers
  remember each row that they made, so that one write makes one row object
  in every order of the moves and two worlds that hold the same rows are
  equal.

  What it does not reach is in the folder's README: anything that needs
  Rama."
  (:refer-clojure :exclude [agent])
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.door.words :as door-words]
            [softland.store.gate.chain :as chain]
            [softland.store.gate.first-act :as first-act]
            [softland.store.permissions :as perm :refer [person agent run]]
            [softland.store.read-path.acts :as acts]
            [softland.store.read-path.answer :as answer]
            [softland.store.read-path.lanes :as lanes]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.read-path.words :as words]
            [softland.store.record.rows :as rows])
  (:import [clojure.lang PersistentQueue]
           [java.nio ByteBuffer]
           [java.nio.charset StandardCharsets]
           [java.security MessageDigest]
           [java.util UUID]))

(defonce ^{:private true
           :doc "The lines that the proof has printed with SHOWN, in order, for the
  recap that `-main` prints at the end."}
  shown-lines (atom []))

(defn- shown
  "Prints one line of what the proof observed, and keeps it for the recap
  that `-main` prints at the end."
  [& xs]
  (let [line (apply print-str "SHOWN" xs)]
    (swap! shown-lines conj line)
    (println line)
    (flush)))

(defn- check
  "Checks that what was observed is what must be, and prints the check's
  sentence after PASSED or FAILED, so that a run lists what it checked. Gives
  what was observed."
  [what expected observed]
  (println (if (is (= expected observed) what) "PASSED" "FAILED") what)
  observed)

(def ^:private numbers "The read path's numbers, at their defaults." (query/numbers nil))

(defrecord Thing [asks asker lanes])

(defn- q
  "Takes a query as a caller gives it. Gives it in its one form; nil for a
  thing that the check turns away."
  [thing]
  (:ok (query/check thing)))

;; ------------------------------------------------------- the tables, as a value

(def ^:private by-code-point
  "The order of a table's keys, as a comparator: the order of
  `softland.store.record.rows/before?`."
  (comparator rows/before?))

(defn- table
  "Takes the tables, a gate and a table's name. Gives that table: a sorted
  map from a row's key to the row."
  [tables gate table-name]
  (get tables [gate table-name] (sorted-map-by by-code-point)))

(defn- time-of
  "The time on the act of a write in this proof, in milliseconds since
  1970, which a gate puts in each row of the act's facts. Takes the act's
  stamp. A time of the proof's own, made of the stamp so that two writes
  have two times. It runs back as the stamp runs on: order and time are
  apart, and the proof does not let the two agree by chance."
  [stamp]
  (- 1760000000000 (* 1000 (long stamp))))

(def ^:private uuid-of
  "A deterministic version-8 UUID made of any Clojure value, so every order of
  moves mints the same permission ids."
  (memoize
   (fn [x]
     (let [bytes (.getBytes (str "softland/read-path-pure-proof/" (pr-str x)) StandardCharsets/UTF_8)
           hash (.digest (MessageDigest/getInstance "SHA-256") bytes)
           buf (ByteBuffer/wrap hash 0 16)
           hi (.getLong buf)
           lo (.getLong buf)]
       (UUID. (bit-or (bit-and hi (bit-not 0xF000)) 0x8000)
              (bit-or (bit-and lo 0x3FFFFFFFFFFFFFFF) Long/MIN_VALUE))))))

(defn- standing-perm-id
  "The deterministic permission UUID of `holder`'s initial standing permission
  in `lane`."
  [lane holder]
  (uuid-of [:standing lane holder]))

(def ^:private fact-row*
  "Makes the row of a fact as a gate would keep it, and remembers it."
  (memoize
   (fn
     ([lane who e k v stamp]
      (fact-row* lane who (standing-perm-id lane who) e k v stamp))
     ([lane who perm-id e k v stamp]
      (let [v-norm (if (and (= declaration/permission-key k) (some? v))
                     (declaration/permission-value v)
                     v)
            fact {:e e :k k :replaces nil
                  :v (when (declaration/store-key? k) v-norm)
                  :text (when (some? v-norm) (text/text (act/normal-value v-norm)))}]
        (rows/fact-row fact (UUID. 0 (long stamp)) 0 stamp (time-of stamp) (declaration/id-text who)
                       (when perm-id (declaration/id-text perm-id))))))))

(def ^:private marks-row*
  "Makes the row of a lane's marked keys, with the stamp since which each
  key's order is level, and remembers it."
  (memoize
   (fn [lane since-by-key stamp]
     (rows/with-level-since
      (fact-row* lane (person :maker) lane :softland.store/marked-keys (set (keys since-by-key)) stamp)
      since-by-key))))

(def ^:private by-value-row*
  "Makes the row of a fact in the order by value, with the time on its act
  (`time-of`), and remembers it."
  (memoize
   (fn [lane who stamp]
     (rows/by-value-row (declaration/id-text who) (declaration/id-text (standing-perm-id lane who))
                        (UUID. 0 (long stamp)) 0 stamp (time-of stamp)))))

(defn- put
  "Puts a row under a key of one table."
  [tables gate table-name k row]
  (assoc tables [gate table-name] (assoc (table tables gate table-name) k row)))

(defn- drop-key
  "Removes a key from one table."
  [tables gate table-name k]
  (assoc tables [gate table-name] (dissoc (table tables gate table-name) k)))

(defn- climb-in-facts
  [lane id facts]
  (loop [known-rows {}]
    (let [{:keys [wants] :as res} (chain/climb lane id known-rows)]
      (if wants
        (recur (assoc known-rows wants (get facts wants)))
        res))))

(defn- holder-perm-ids
  [tables gate lane holder session]
  (let [[from to] (rows/by-holder-run lane holder session)]
    (vec (keep rows/permission-of-by-holder-key
               (keys (subseq (table tables gate :permissions-by-holder) >= from < to))))))

(defn- standing-holder-perm
  [tables gate lane holder session]
  (let [facts (table tables gate :facts)]
    (some (fn [id]
            (let [{:keys [values whole?]} (climb-in-facts lane id facts)]
              (when whole?
                [id (first values) (chain/allows-now values)])))
          (holder-perm-ids tables gate lane holder session))))

(defn- grant-perm
  [tables gate lane giver under-id perm-id perm-val stamp]
  (let [v (declaration/permission-value perm-val)
        place (chain/place lane perm-id)
        tables (put tables gate :facts place
                    (fact-row* lane giver under-id perm-id declaration/permission-key v stamp))]
    (if (= :default-group (first (:covers v)))
      (put tables gate :permissions-by-holder
           (rows/by-holder-key lane (:holder v) (:session v) perm-id)
           (long stamp))
      tables)))

(defn- end-perm
  [tables gate lane ender under-id perm-id stamp]
  (let [facts (table tables gate :facts)
        place (chain/place lane perm-id)
        was (rows/held-permission (get facts place))]
    (if (nil? was)
      tables
      (let [tables (put tables gate :facts place
                        (fact-row* lane ender under-id perm-id declaration/permission-key nil stamp))]
        (if (= :default-group (first (:covers was)))
          (drop-key tables gate :permissions-by-holder
                    (rows/by-holder-key lane (:holder was) (:session was) perm-id))
          tables)))))

(defn- write
  "Stands for a gate: puts the rows of one accepted act in the tables (`:facts`,
  `:permissions-by-holder`, and `:facts-by-value`)."
  [tables [kind lane & more]]
  (let [gate (first-act/depot-at-making lane)
        facts (table tables gate :facts)
        own (fn [k v stamp]
              (put tables gate :facts (rows/place-key lane lane k)
                   (fact-row* lane (person :maker) (when (not= :softland.store/depot k)
                                                     (standing-perm-id lane (person :maker)))
                              lane k v stamp)))]
    (case kind
      :make (let [[stamp] more] (own :softland.store/depot gate stamp))
      :open (let [[stamp] more]
              (if (standing-holder-perm tables gate lane :anyone nil)
                tables
                (let [id0 (first-act/first-anyone-id lane)
                      id (if (nil? (get facts (chain/place lane id0))) id0 (uuid-of [:open lane stamp]))
                      giver (person :maker)]
                  (grant-perm tables gate lane giver (standing-perm-id lane giver) id
                              {:holder :anyone
                               :allows {:see true}
                               :covers [:default-group lane]
                               :given-by giver
                               :under (standing-perm-id lane giver)}
                              stamp))))
      :close (let [[stamp] more
                   ender (person :maker)]
               (reduce (fn [t id]
                         (end-perm t gate lane ender (standing-perm-id lane ender) id stamp))
                       tables
                       (holder-perm-ids tables gate lane :anyone nil)))
      :list (let [[listed stamp] more] (own :softland.store/builds-on listed stamp))
      :permission (let [[holder stamp] more]
                    (if (standing-holder-perm tables gate lane holder nil)
                      tables
                      (let [id0 (standing-perm-id lane holder)
                            id (if (nil? (get facts (chain/place lane id0)))
                                 id0
                                 (uuid-of [:standing lane holder stamp]))]
                        (grant-perm tables gate lane holder nil id
                                    {:holder holder
                                     :allows declaration/owner-allows
                                     :covers [:default-group lane]
                                     :given-by holder}
                                    stamp))))
      :give (let [[holder giver stamp opts] more]
              (if-let [[giver-id _ giver-allows] (standing-holder-perm tables gate lane giver nil)]
                (if (or (:see giver-allows) (:write giver-allows))
                  (let [id (or (:id opts) (uuid-of [:give lane holder giver stamp (dissoc opts :id)]))
                        lent-id (if (= false (:lent? opts)) nil giver-id)
                        allows (or (:allows opts)
                                   (if (act/borrower? holder)
                                     {:see true :write :all}
                                     perm/member-allows))
                        val (cond-> {:holder holder
                                     :allows allows
                                     :covers (or (:covers opts) [:default-group lane])
                                     :given-by giver
                                     :under giver-id}
                              (:session opts) (assoc :session (:session opts))
                              lent-id (assoc :lent-from lent-id))]
                    (if (get facts (chain/place lane id))
                      tables
                      (grant-perm tables gate lane giver giver-id id val stamp)))
                  tables)
                tables))
      :give-run (let [[session holder giver stamp opts] more]
                  (if-let [[giver-id _ giver-allows] (standing-holder-perm tables gate lane giver nil)]
                    (if (or (:see giver-allows) (:write giver-allows))
                      (let [id (or (:id opts) (uuid-of [:give-run lane session holder giver stamp (dissoc opts :id)]))
                            lent-id (if (= false (:lent? opts)) nil giver-id)
                            allows (or (:allows opts) {:see true})
                            val (cond-> {:holder holder
                                         :allows allows
                                         :covers [:default-group lane]
                                         :session session
                                         :given-by giver
                                         :under giver-id}
                                  lent-id (assoc :lent-from lent-id))]
                        (if (get facts (chain/place lane id))
                          tables
                          (grant-perm tables gate lane giver giver-id id val stamp)))
                      tables)
                    tables))
      :change (let [[perm-id allows stamp] more
                    was (rows/held-permission (get facts (chain/place lane perm-id)))]
                (if (nil? was)
                  tables
                  (grant-perm tables gate lane (:given-by was) (:under was) perm-id
                              (assoc was :allows allows) stamp)))
      :swap (let [[holder old-id new-id allows stamp] more
                  maker (person :maker)
                  under-id (standing-perm-id lane maker)]
              (-> tables
                  (end-perm gate lane maker under-id old-id stamp)
                  (grant-perm gate lane maker under-id new-id
                              {:holder holder
                               :allows allows
                               :covers [:default-group lane]
                               :given-by maker
                               :under under-id}
                              stamp)))
      :give-act (let [[yes holder giver stamp] more]
                  (if-let [[giver-id _ _] (standing-holder-perm tables gate lane giver nil)]
                    (let [p (perm/act-permission holder lane {:id giver-id :value {:holder giver}}
                                                 yes (apply str (repeat 64 "a")))
                          {:keys [e v]} (perm/permission-fact p)]
                      (if (nil? (get facts (chain/place lane e)))
                        (grant-perm tables gate lane giver giver-id e v stamp)
                        tables))
                    tables))
      :use-act (let [[yes who stamp] more
                     place (chain/place lane yes)]
                 (put tables gate :facts place
                      (rows/use-row (UUID. 0 (long stamp)) stamp (time-of stamp)
                                    (declaration/id-text who) (declaration/id-text yes))))
      :end-id (let [[perm-id stamp] more
                    ender (person :maker)]
                (end-perm tables gate lane ender (standing-perm-id lane ender) perm-id stamp))
      :end-run (let [[session a b] more
                     [holder stamp] (if (some? b) [a b] [nil a])
                     ender (person :maker)
                     ids (if holder
                           (holder-perm-ids tables gate lane holder session)
                           (let [l-prefix (str (declaration/id-text lane) " ")
                                 l-end (str (declaration/id-text lane) "!")
                                 s-text (declaration/id-text session)]
                             (keep (fn [[k _]]
                                     (let [parts (str/split k #" ")]
                                       (when (and (= 4 (count parts)) (= s-text (nth parts 2)))
                                         (rows/permission-of-by-holder-key k))))
                                   (subseq (table tables gate :permissions-by-holder) >= l-prefix < l-end))))]
                 (reduce (fn [t id]
                           (end-perm t gate lane ender (standing-perm-id lane ender) id stamp))
                         tables
                         ids))
      :end (let [[holder stamp] more
                 ender (person :maker)]
             (reduce (fn [t id]
                       (end-perm t gate lane ender (standing-perm-id lane ender) id stamp))
                     tables
                     (holder-perm-ids tables gate lane holder nil)))
      :marks (let [[marked stamp] more
                   before (rows/marked-keys-of (get facts (rows/place-key lane lane :softland.store/marked-keys)))
                   since-by-key (into {} (map (fn [k] [k (get before k stamp)])) marked)]
               (put tables gate :facts (rows/place-key lane lane :softland.store/marked-keys)
                    (marks-row* lane since-by-key stamp)))
      :fact (let [[who e k v stamp] more
                  place (rows/place-key lane e k)
                  before (get facts place)
                  since (get (rows/marked-keys-of (get facts (rows/place-key lane lane :softland.store/marked-keys))) k)
                  tables (put tables gate :facts place (fact-row* lane who e k v stamp))
                  order (table tables gate :facts-by-value)
                  order (if (and since before (some? (:value before)))
                          (dissoc order (rows/by-value-key lane k since (rows/value-digest (rows/plain-bytes-of before)) e))
                          order)
                  order (if (and since (some? v) (< since stamp))
                          (assoc order (rows/by-value-key lane k since (rows/value-digest-of v) e)
                                 (by-value-row* lane who stamp))
                          order)]
              (assoc tables [gate :facts-by-value] order)))))

(defn- table-value
  "Takes the tables and a source. Gives what Rama would give for the source
  now: the row or nil for a source of one row, the rows of the run for the
  runs (`:own-rows`, `:run`, `:holders`)."
  [tables source]
  (let [t (table tables (:gate source) (:table source))
        [kind a b] (:path source)]
    (if (= :row kind)
      (get t a)
      (into (sorted-map-by by-code-point) (subseq t >= a < b)))))

(defn- run-rights
  "Runs one single-turn `lane-rights` query on `tables`, bounded at
  `max-holder-permissions + 1` entries per holder scan just as
  `softland.store.module.door-reads/read-lane-rights` runs on the task."
  ([tables lane asker session]
   (run-rights tables lane :see asker session))
  ([tables lane question asker session]
   (let [gate (first-act/depot-at-making lane)
         limit (inc lanes/max-holder-permissions)]
     (loop [state (lanes/rights-begin lane question asker session)]
       (if-let [reads (seq (lanes/rights-reads state))]
         (recur (lanes/rights-resume
                 state
                 (into {}
                       (map (fn [[which a b :as r]]
                              [r (case which
                                   :fact (get (table tables gate :facts) a)
                                   :holders (let [h (table tables gate :permissions-by-holder)
                                                  entries (if (lanes/holder-range-agreed? a b)
                                                            (take limit (subseq h >= a < b))
                                                            (take limit (subseq h >= a)))]
                                              (lanes/clean-holder-run a b (into {} entries))))]))
                       reads)))
         (lanes/rights-snapshot state))))))

(defn- rights-state
  "Runs `rights-begin`/`rights-resume` to completion on `tables` and returns
  `(lanes/rights state)` (`{:made? :sees? :standing}`)."
  [tables lane asker session]
  (let [gate (first-act/depot-at-making lane)
        limit (inc lanes/max-holder-permissions)]
    (loop [state (lanes/rights-begin lane :see asker session)]
      (if-let [reads (seq (lanes/rights-reads state))]
        (recur (lanes/rights-resume
                state
                (into {}
                      (map (fn [[which a b :as r]]
                             [r (case which
                                  :fact (get (table tables gate :facts) a)
                                  :holders (lanes/clean-holder-run
                                            a b (into {} (take limit (subseq (table tables gate :permissions-by-holder) >= a < b)))))]))
                      reads)))
        (lanes/rights state)))))

(defn- play-once
  "Plays a read asked once on the tables, as `softland.store.read-path/read-once*`
  does: fetch missing PState sources in batched calls per table and routing id,
  and ask `lane-rights` after each made lane's last fact select returns."
  [tables query]
  (let [bump (fn [epochs sources]
               (reduce (fn [m lane] (update m lane (fnil inc 0)))
                       epochs
                       (distinct (map :lane sources))))]
    (loop [sel-round 0
           ask-round 0
           known {}
           fact-epochs {}
           rights-at {}
           calls []
           rights-calls []
           first-missing nil]
      (let [snapshots (into {}
                            (keep (fn [[lane {:keys [epoch snapshot]}]]
                                    (when (or (= :too-many-permissions (:standing snapshot))
                                              (= epoch (get fact-epochs lane 0)))
                                      [lane snapshot])))
                            rights-at)
            reading (sources/reading query known :once numbers nil snapshots)
            missing (:missing reading)
            needs-rights (:needs-rights reading)]
        (cond
          (:too-many-permissions reading)
          {:rounds sel-round :ask-rounds ask-round :calls calls :rights-calls rights-calls
           :first-missing first-missing :reading reading :known known :snapshots snapshots
           :answer (words/word :too-many-permissions (:too-many-permissions reading))}

          (:too-large reading)
          {:rounds sel-round :ask-rounds ask-round :calls calls :rights-calls rights-calls
           :first-missing first-missing :reading reading :known known :snapshots snapshots
           :answer (words/word :serve-a-read-that-large (:too-large reading))}

          (seq missing)
          (if (<= 6 sel-round)
            {:rounds sel-round :ask-rounds ask-round :calls calls :rights-calls rights-calls
             :first-missing first-missing :reading reading :known known :snapshots snapshots
             :answer :did-not-settle}
            (recur (inc sel-round)
                   ask-round
                   (into known (map (fn [source] [source (table-value tables source)])) missing)
                   (bump fact-epochs missing)
                   rights-at
                   (conj calls (count (sources/calls-for-once missing numbers)))
                   rights-calls
                   (or first-missing missing)))

          (seq needs-rights)
          (if (<= 12 ask-round)
            {:rounds sel-round :ask-rounds ask-round :calls calls :rights-calls rights-calls
             :first-missing first-missing :reading reading :known known :snapshots snapshots
             :answer :did-not-settle}
            (let [snaps (into {}
                              (map (fn [[_routing-id lane]]
                                     [lane (run-rights tables lane :see (:asker query) (:session query))]))
                              needs-rights)]
              (recur sel-round
                     (inc ask-round)
                     known
                     fact-epochs
                     (reduce (fn [m [lane snap]]
                               (assoc m lane {:epoch (get fact-epochs lane 0) :snapshot snap}))
                             rights-at
                             snaps)
                     calls
                     (conj rights-calls (count needs-rights))
                     first-missing)))

          (sources/settled? reading)
          {:rounds sel-round :ask-rounds ask-round :calls calls :rights-calls rights-calls
           :first-missing first-missing :reading reading :known known :snapshots snapshots
           :answer (answer/whole query reading known)}

          :else
          {:rounds sel-round :ask-rounds ask-round :calls calls :rights-calls rights-calls
           :first-missing first-missing :reading reading :known known :snapshots snapshots
           :answer :did-not-settle})))))

;; -------------------------------------------------------------- the player

(def ^:dynamic ^:private *known-before-the-leaving*
  "Bound by the player, for one fault that it can be told to make: `known`
  as it was before a step left the sources that are no longer wanted."
  nil)

(defn- known-of
  "Takes a world. Gives `known` for its read: each source that the read
  holds and whose push is given, with the value that the push holds."
  [world]
  (into {}
        (keep (fn [source]
                (let [push (get-in world [:pushes source])]
                  (when (contains? #{:served :given} (:phase push))
                    [source (:value push)]))))
        (:held world)))

(defn- snapshots-of
  [world]
  (into {}
        (keep (fn [[lane st]]
                (when-let [s (:snapshot st)]
                  [lane s])))
        (:rights world)))

(defn- ensure-rights
  "Single-flight `lane-rights` launcher per `(read, lane)`, mirroring
  `softland.store.read-path/ensure-rights-queries!`."
  [world reading]
  (reduce (fn [w [_routing-id lane]]
            (let [st (get-in w [:rights lane])]
              (if (and (nil? (:in-flight st)) (get st :dirty? true))
                (assoc-in w [:rights lane]
                          (assoc (or st {}) :dirty? false :in-flight {:phase :asked}))
                w)))
          world
          (:needs-rights reading)))

(defn- settle
  "The first line of a step, as `softland.store.read-path/settle!` makes it:
  recomputes `reading` (validating `:read-set` stamps entry-for-entry), joins
  newly wanted pushes, and leaves unwanted pushes until moves stabilize."
  [world]
  (loop [world world joined #{} left #{} known-before nil round 0]
    (let [known (known-of world)
          snapshots (snapshots-of world)
          reading (sources/reading (:query world) known :live numbers (:held world) snapshots)
          {:keys [join leave]} (sources/moves reading (:held world))]
      (if (or (and (empty? join) (empty? leave)) (< 24 round))
        [(reduce (fn [w [lane st]]
                   (if (and (contains? #{:yes :may-not-see} st)
                            (nil? (get-in w [:rights lane :in-flight])))
                     (assoc-in w [:rights lane :dirty?] false)
                     w))
                 world
                 (:standing reading))
         {:reading reading :known known :snapshots snapshots
          :joined joined :left left :known-before (or known-before known)}]
        (recur (-> world
                   (update :held #(into (reduce disj % leave) join))
                   (update :pushes #(into (reduce dissoc % leave)
                                          (map (fn [source] [source {:phase :asked :queue [] :pend []}]))
                                          join)))
               (into joined join)
               (into left leave)
               (or known-before known)
               (inc round))))))

(defn- told
  "Puts what a step tells in the asker's screen: nothing; a whole answer in
  the place of all; or each changed place, where a place that the read no
  longer gives leaves a map of places."
  [world tell]
  (cond
    (nil? tell) world
    (:anew tell) (assoc world :screen (:anew tell))
    :else (update world :screen
                  (fn [screen]
                    (reduce (fn [screen [place place-answer]]
                              (cond
                                (contains? screen :place) (assoc screen :place place-answer)
                                (= :no-fact (:holds place-answer)) (update screen :places dissoc place)
                                :else (assoc-in screen [:places place] place-answer)))
                            screen
                            (:changed tell))))))

(defn- after-a-step
  "Takes a world, what a pure function said the step does, and the fault
  that the player was told to make, if any. Gives the world with the mark
  and the screen as the step leaves them. With the fault `:no-mark` the
  mark is dropped, so no step carries an owed answer to a later one."
  [world {:keys [tell owed?]} fault]
  (-> world
      (assoc :owed? (if (= :no-mark fault) false owed?))
      (told tell)))

(defn- handle
  "One callback of a push is handled, as `softland.store.read-path/on-change`
  handles it."
  [world source fault]
  (let [[old new] (first (get-in world [:pushes source :pend]))
        world (update-in world [:pushes source :pend] subvec 1)
        changed (sources/named source :all old new)]
    (if (or (empty? changed) (not (contains? (:held world) source)))
      world
      (let [query (:query world)
            world (if (sources/rights-kind query source)
                    (assoc-in world [:rights (:lane source) :dirty?] true)
                    world)
            [world {:keys [reading known snapshots joined left known-before]}] (settle world)
            [meant-before meant-after] (answer/meanings query snapshots known source old new)]
        (-> (after-a-step world
                          (binding [*known-before-the-leaving* known-before]
                            (answer/after-change query reading known source
                                                 (sources/places-of query source changed)
                                                 meant-before meant-after joined left (:owed? world)))
                          fault)
            (ensure-rights reading))))))

(defn- arrive
  "The arrival of a push that the read asked for is handled, as
  `softland.store.read-path/on-arrival` handles it."
  [world source fault]
  (let [world (assoc-in world [:pushes source :phase] :given)]
    (if-not (contains? (:held world) source)
      world
      (let [query (:query world)
            lane (:lane source)
            home (lanes/home query)
            snap (get-in world [:rights lane :snapshot])
            known-now (known-of world)
            world (if (and (sources/rights-kind query source)
                           snap
                           (not (sources/source-matches-snapshot? home lane snap source known-now)))
                    (assoc-in world [:rights lane :dirty?] true)
                    world)
            [world {:keys [reading known joined left]}] (settle world)]
        (-> (after-a-step world
                          (answer/after-arrival query reading known source joined left (:owed? world))
                          fault)
            (ensure-rights reading))))))

(defn- arrive-rights
  "The arrival of an async `lane-rights` snapshot is handled, as
  `softland.store.read-path/on-rights` handles it."
  [world lane fault]
  (let [snap (get-in world [:rights lane :in-flight :snapshot])
        world (update-in world [:rights lane] #(-> (or % {}) (assoc :snapshot snap) (dissoc :in-flight)))
        [world {:keys [reading known joined left]}] (settle world)]
    (-> (after-a-step world
                      (answer/after-rights (:query world) reading known lane joined left (:owed? world))
                      fault)
        (ensure-rights reading))))

(defn- moves-of
  "Takes a world. Gives every move that can be made now: the next write; for
  each push, by its state, to be served, to have its arrival handled, to take
  its next value, to have its next callback handled; and for each in-flight
  `lane-rights` query, to be served on the tables or to have its answer arrive."
  [world]
  (concat
   (when (seq (:writes world)) [[:write]])
   (mapcat (fn [[source push]]
             (let [open? (contains? #{:served :given} (:phase push))]
               (concat (when (= :asked (:phase push)) [[:serve source]])
                       (when (= :served (:phase push)) [[:arrive source]])
                       (when (and open? (seq (:queue push))) [[:take source]])
                       (when (and open? (seq (:pend push))) [[:handle source]]))))
           (:pushes world))
   (mapcat (fn [[lane st]]
             (case (get-in st [:in-flight :phase])
               :asked  [[:serve-rights lane]]
               :served [[:arrive-rights lane]]
               nil))
           (:rights world))))

(defn- move
  "Makes one move in a world."
  [world [kind source] fault]
  (case kind
    :write (let [before (:tables world)
                 after (write before (first (:writes world)))]
             (-> world
                 (assoc :tables after)
                 (update :writes subvec 1)
                 (update :pushes
                         (fn [pushes]
                           (into {}
                                 (map (fn [[source push]]
                                        [source (if (and (contains? #{:served :given} (:phase push))
                                                         (not= (table-value before source) (table-value after source)))
                                                  (update push :queue conj (table-value after source))
                                                  push)]))
                                 pushes)))))
    :serve (assoc-in world [:pushes source]
                     {:phase :served :value (table-value (:tables world) source) :queue [] :pend []})
    :take (let [push (get-in world [:pushes source])
                new (first (:queue push))]
            (assoc-in world [:pushes source]
                      (-> push
                          (assoc :value new)
                          (update :queue subvec 1)
                          (update :pend conj [(:value push) new]))))
    :handle (handle world source fault)
    :arrive (arrive world source fault)
    :serve-rights (let [query (:query world)
                        snap (run-rights (:tables world) source :see (:asker query) (:session query))]
                    (assoc-in world [:rights source :in-flight] {:phase :served :snapshot snap}))
    :arrive-rights (arrive-rights world source fault)))

(defn- open-live
  "Opens a live read in a world, as `softland.store.read-path/read-live`
  does: join missing pushes and serve them from the tables, ask `lane-rights`
  for made lanes that need rights, join their `:read-set` watches, and repeat
  until settled; then the first answer is the asker's screen and `mark-of` its
  mark."
  [tables writes query]
  (loop [world {:tables tables :writes (vec writes) :query query
                :held #{} :pushes {} :rights {} :owed? false}
         round 0
         first-joined nil]
    (let [known (known-of world)
          snapshots (snapshots-of world)
          reading (sources/reading query known :live numbers (:held world) snapshots)
          {:keys [join leave]} (sources/moves reading (:held world))
          needs-rights (:needs-rights reading)]
      (cond
        (or (< 24 round) (:too-large reading) (:too-many-permissions reading))
        (assoc world :screen :did-not-open :rounds round :first-joined first-joined)

        (or (seq join) (seq leave))
        (recur (-> world
                   (update :held #(into (reduce disj % leave) join))
                   (update :pushes #(into (reduce dissoc % leave)
                                          (map (fn [source]
                                                 [source {:phase :given
                                                          :value (table-value tables source)
                                                          :queue [] :pend []}]))
                                          join)))
               (inc round)
               (or first-joined join))

        (seq needs-rights)
        (let [snaps (into {}
                          (map (fn [[_routing-id lane]]
                                 [lane (run-rights tables lane :see (:asker query) (:session query))]))
                          needs-rights)]
          (recur (reduce (fn [w [lane snap]]
                           (assoc-in w [:rights lane] {:snapshot snap :dirty? false}))
                         world
                         snaps)
                 (inc round)
                 first-joined))

        :else
        (let [screen (answer/whole query reading known)]
          (assoc world :screen screen :owed? (answer/mark-of screen)
                       :rounds round :first-joined first-joined))))))

(defn- every-order
  "Plays every order of the moves from a world. Takes the world and the
  fault that the player is to make, or nil. Gives {:states how many worlds
  were met, :ends how many of them have no move left, :wrong the first end
  whose screen is not what a read asked once gives from its tables, as
  {:order :screen :tables-give}, or nil}."
  [start fault]
  (let [start (dissoc start :rounds :first-joined)]
    (loop [todo (conj PersistentQueue/EMPTY [start []])
           seen #{start}
           ends 0
           wrong nil]
      (if-let [[world order] (peek todo)]
        (let [moves (moves-of world)]
          (if (empty? moves)
            (let [truth (:answer (play-once (:tables world) (:query world)))]
              (recur (pop todo) seen (inc ends)
                     (or wrong
                         (when (not= truth (:screen world))
                           {:order order :screen (:screen world) :tables-give truth}))))
            (let [[todo seen] (reduce (fn [[todo seen] a-move]
                                        (let [next-world (move world a-move fault)]
                                          (if (contains? seen next-world)
                                            [todo seen]
                                            [(conj todo [next-world (conj order (first a-move))])
                                             (conj seen next-world)])))
                                      [(pop todo) seen]
                                      moves)]
              (recur todo seen ends wrong))))
        {:states (count seen) :ends ends :wrong wrong}))))

;; ------------------------------------------------ the worlds that are played

(defn- lane-n
  "The id of the n-th lane of a played list, 1 the nearest."
  [n]
  (keyword (str "l" n)))

(def ^:private things
  "The things that a played fact points at: `:v` is the thing that the
  asker asks about, each other word another thing."
  {:v :canvas-1 :u :canvas-u :x :canvas-x :w :canvas-w})

(defn- points-at-world
  "A world for what points at a thing. Lanes in `seen` start open to `:anyone`;
  the others start closed, with no permission for the asker until `[:see lane]`."
  ([k marks facts seen writes]
   (points-at-world lane-n k marks facts seen writes))
  ([lane-n k marks facts seen writes]
   (let [tables (reduce write {}
                        (concat
                         (for [n (range 1 (inc k))] [:make (lane-n n) 1])
                         (for [n (range 1 (inc k)) :when (contains? seen n)] [:open (lane-n n) 2])
                         (for [n (range 1 (inc k)) :when (get marks n)] [:marks (lane-n n) #{:parent} 3])
                         (for [[[n place] [thing version]] (sort-by (comp second val) facts)]
                           [:fact (lane-n n) (person :maker) place :parent (get things thing) (+ 10 version)])))
         writes (map-indexed (fn [i [kind n place thing]]
                               (if (= :see kind)
                                 [:permission (lane-n n) (person :ann) (+ 100 i)]
                                 [:fact (lane-n n) (person :maker) place :parent (get things thing) (+ 100 i)]))
                             writes)]
     (open-live tables writes
                (q {:asks [:points-at :parent :canvas-1] :asker (person :ann) :lanes (mapv lane-n (range 1 (inc k)))})))))

(defn- base-at
  [i]
  (fn [n] (if (= n i) :base (lane-n n))))

(def ^:private points-at-scenarios
  [["1. the farther order loses p while the nearer lane gets another row at p"
    2 {1 true 2 true} {[2 :p] [:v 1]} #{1 2}
    [[:w 1 :p :u] [:w 2 :p :x]]]
   ["2. a nearer lane that does not mark the key writes the same thing; then the farther order loses p"
    2 {1 false 2 true} {[2 :p] [:v 1]} #{1 2}
    [[:w 1 :p :v] [:w 2 :p :x]]]
   ["3. the nearer writes the same thing, the farther moves away and moves back"
    2 {1 false 2 true} {[2 :p] [:v 1]} #{1 2}
    [[:w 1 :p :v] [:w 2 :p :x] [:w 2 :p :v]]]
   ["4. the nearer hides, then says the same thing, while the farther is written again"
    2 {1 false 2 true} {[2 :p] [:v 1]} #{1 2}
    [[:w 1 :p :u] [:w 2 :p :v] [:w 1 :p :v]]]
   ["5. both mark: the nearer moves p away and back while the farther moves it away"
    2 {1 true 2 true} {[2 :p] [:v 1]} #{1 2}
    [[:w 1 :p :u] [:w 1 :p :v] [:w 2 :p :x]]]
   ["6. three lanes, all mark: the middle loses p, the nearest gets a row, the farthest is written again"
    3 {1 true 2 true 3 true} {[2 :p] [:v 1] [3 :p] [:v 2]} #{1 2 3}
    [[:w 2 :p :u] [:w 1 :p :w] [:w 3 :p :v]]]
   ["7. three lanes: the middle loses p and the farthest loses p, then the middle gets it back"
    3 {1 false 2 true 3 true} {[2 :p] [:v 1] [3 :p] [:v 2] [1 :p] [:v 3]} #{1 2 3}
    [[:w 2 :p :u] [:w 3 :p :u] [:w 2 :p :v]]]
   ["8. three lanes, four writes on one place"
    3 {1 false 2 true 3 true} {[3 :p] [:v 1]} #{1 2 3}
    [[:w 2 :p :v] [:w 1 :p :v] [:w 2 :p :u] [:w 3 :p :u]]]
   ["9. a permission is given: a nearer lane joins, with a place q of its own, over a farther place p"
    2 {1 true 2 true} {[1 :q] [:v 1] [2 :p] [:v 2]} #{2}
    [[:see 1]]]
   ["10. the same, while the farther lane is written"
    2 {1 true 2 true} {[1 :q] [:v 1] [2 :p] [:v 2] [1 :p] [:u 3]} #{2}
    [[:see 1] [:w 2 :p :v] [:w 1 :q :u]]]
   ["11. a new place in the farther order, with a nearer row there already"
    2 {1 false 2 true} {[1 :p] [:v 1]} #{1 2}
    [[:w 2 :p :v] [:w 1 :p :u] [:w 2 :p :x]]]
   ["12. four lanes: the second loses p, so checks are asked in the second and the third; then the third gets p"
    4 {1 true 2 true 3 true 4 true} {[2 :p] [:v 1] [4 :p] [:v 2]} #{1 2 3 4}
    [[:w 2 :p :u] [:w 3 :p :v]]]
   ["13. a nearer lane joins while a check of a new place is on its way"
    3 {1 true 2 false 3 true} {[1 :q] [:v 1] [2 :p] [:u 2]} #{2 3}
    [[:w 3 :p :v] [:see 1] [:w 2 :p :v]]]])

(defn- lists-up-to
  [n options]
  (mapcat (fn [length]
            (reduce (fn [lists _] (for [l lists o options] (conj l o))) [[]] (range length)))
          (range 1 (inc n))))

(defn- every-small-points-at
  [fault]
  (let [options (for [n [1 2] thing [:v :u]] [:w n :p thing])
        starts [{} {[2 :p] [:v 1]} {[1 :p] [:u 1] [2 :p] [:v 2]} {[1 :p] [:v 1] [2 :p] [:v 2]}]
        played (for [nearer-marks? [true false]
                     start starts
                     writes (lists-up-to 3 options)]
                 (assoc (every-order (points-at-world 2 {1 nearer-marks? 2 true} start #{1 2} writes) fault)
                        :is [nearer-marks? start writes]))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

(defn- standing-world
  "A world for one place, or one entity, through two lanes: `:near` starts open
  to `:anyone` or closed, with a permission for `:ann` or none; `:far` is open
  to `:anyone` and holds a fact at the place. Writes: `[:open]`, `[:close]`,
  `[:permission]`, `[:near]`, `[:far]`."
  [open? permission? writes asks]
  (let [tables (reduce write {}
                       (concat [[:make :near 1] [:make :far 1] [:open :far 2]]
                               (when open? [[:open :near 2]])
                               (when permission? [[:permission :near (person :ann) 3]])
                               [[:fact :far (person :maker) :p :text "f0" 11]]))
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :open       [:open :near (+ 100 i)]
                                :close      [:close :near (+ 100 i)]
                                :permission [:permission :near (person :ann) (+ 100 i)]
                                :near       [:fact :near (person :maker) :p :text (str "n" i) (+ 100 i)]
                                :far        [:fact :far (person :maker) :p :text (str "f" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks asks :asker (person :ann) :lanes [:near :far]}))))

(defn- base-world
  "A world for one place, or one entity, through `:mine` (the asker's home,
  open to `:anyone`) and `:base` (microbatch, starting open to `:anyone` or
  closed, with a permission for `:ann` or none, read under `:mine`'s routing id)."
  [open? permission? writes asks]
  (let [tables (reduce write {}
                       (concat [[:make :mine 1] [:make :base 1] [:open :mine 2]]
                               (when open? [[:open :base 2]])
                               (when permission? [[:permission :base (person :ann) 3]])
                               [[:fact :base (person :maker) :p :text "b0" 11]]))
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :open       [:open :base (+ 100 i)]
                                :close      [:close :base (+ 100 i)]
                                :permission [:permission :base (person :ann) (+ 100 i)]
                                :mine       [:fact :mine (person :maker) :p :text (str "m" i) (+ 100 i)]
                                :base       [:fact :base (person :maker) :p :text (str "b" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks asks :asker (person :ann) :lanes [:mine :base]}))))

(defn- chain-world
  "A world for one place through two lanes, for an asker whose lent permission
  in `:near` is two beneath the lane's first (`:root` -> `:bea` -> `:ann`)."
  [writes asks]
  (let [tables (reduce write {}
                       [[:make :near 1] [:make :far 1] [:open :far 2]
                        [:permission :near (person :root) 2] [:give :near (person :bea) (person :root) 3] [:give :near (person :ann) (person :bea) 4]
                        [:fact :near (person :maker) :p :text "n0" 10] [:fact :far (person :maker) :p :text "f0" 11]])
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :end-above    [:end :near (person :bea) (+ 100 i)]
                                :give-above   [:give :near (person :bea) (person :root) (+ 100 i)]
                                :end-own      [:end :near (person :ann) (+ 100 i)]
                                :give-own     [:give :near (person :ann) (person :bea) (+ 100 i)]
                                :give-by-root [:give :near (person :ann) (person :root) (+ 100 i)]
                                :near         [:fact :near (person :maker) :p :text (str "n" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks asks :asker (person :ann) :lanes [:near :far]}))))

(defn- run-world
  "A world for one place through two lanes, for an asker who asks in sitting
  `:run-7`, holding a sitting-scoped permission lent from `:bea`."
  [writes asks]
  (let [tables (reduce write {}
                       [[:make :near 1] [:make :far 1] [:open :far 2]
                        [:permission :near (person :root) 2] [:give :near (person :bea) (person :root) 3]
                        [:give-run :near :run-7 (person :ann) (person :bea) 4]
                        [:fact :near (person :maker) :p :text "n0" 10] [:fact :far (person :maker) :p :text "f0" 11]])
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :end-run    [:end-run :near :run-7 (person :ann) (+ 100 i)]
                                :give-run   [:give-run :near :run-7 (person :ann) (person :bea) (+ 100 i)]
                                :end-above  [:end :near (person :bea) (+ 100 i)]
                                :give-above [:give :near (person :bea) (person :root) (+ 100 i)]
                                :give-own   [:give :near (person :ann) (person :root) (+ 100 i)]
                                :end-own    [:end :near (person :ann) (+ 100 i)]
                                :give-run-to-another [:give-run :near :run-7 (person :dov) (person :bea) (+ 100 i)]
                                :near       [:fact :near (person :maker) :p :text (str "n" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks asks :asker (person :ann) :session :run-7 :lanes [:near :far]}))))

(defn- unseen-world
  "A world for one place through two lanes, for an asker who may not see
  `:near` while `:dov` is given and loses permissions in sitting `:run-7`."
  [writes]
  (let [tables (reduce write {}
                       [[:make :near 1] [:make :far 1] [:open :far 2]
                        [:permission :near (person :root) 2] [:give :near (person :bea) (person :root) 3]
                        [:fact :near (person :maker) :p :text "n0" 10] [:fact :far (person :maker) :p :text "f0" 11]])
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :give-run-to-another [:give-run :near :run-7 (person :dov) (person :bea) (+ 100 i)]
                                :end-run    [:end-run :near :run-7 (person :dov) (+ 100 i)]
                                :end-above  [:end :near (person :bea) (+ 100 i)]
                                :give-above [:give :near (person :bea) (person :root) (+ 100 i)]
                                :near       [:fact :near (person :maker) :p :text (str "n" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks [:place :p :text] :asker (person :ann) :session :run-7 :lanes [:near :far]}))))

(defn- own-unseen-world
  "A world for one place through two lanes, for an asker who may not see
  `:near`, and whose own holder runs there receive write-only permissions
  (`{:write :all}`) and ends that never allow seeing."
  [writes]
  (let [tables (reduce write {}
                       [[:make :near 1] [:make :far 1] [:open :far 2]
                        [:permission :near (person :root) 2] [:give :near (person :bea) (person :root) 3] [:give :near (person :cal) (person :root) 4]
                        [:fact :near (person :maker) :p :text "n0" 10] [:fact :far (person :maker) :p :text "f0" 11]])
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :bea-write-only         [:give :near (person :ann) (person :bea) (+ 100 i) {:allows {:write :all}}]
                                :cal-run-write-only     [:give-run :near :run-7 (person :ann) (person :cal) (+ 100 i) {:allows {:write :all}}]
                                :root-ends-hers         [:end :near (person :ann) (+ 100 i)]
                                :root-ends-the-run      [:end-run :near :run-7 (person :ann) (+ 100 i)]
                                :near                   [:fact :near (person :maker) :p :text (str "n" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks [:place :p :text] :asker (person :ann) :session :run-7 :lanes [:near :far]}))))

(defn- every-small-own-unseen
  []
  (let [tells (atom 0)
        options [[:bea-write-only] [:cal-run-write-only] [:root-ends-hers] [:root-ends-the-run] [:near]]
        played (with-redefs [told (let [told* told]
                                    (fn [world tell]
                                      (when tell (swap! tells inc))
                                      (told* world tell)))]
                 (vec (for [writes (lists-up-to 3 options)]
                        (assoc (every-order (own-unseen-world writes) nil) :is writes))))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :tells @tells
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

(defn- stale-chain-world
  "A world for one place through two lanes, where `:ann`'s earlier lent
  permissions fell when `:dave`'s ended before `\"the secret\"` was written,
  and `:bea` then receives a new permission from `:root`."
  [writes]
  (let [tables (reduce write {}
                       [[:make :near 1] [:make :far 1] [:open :far 2]
                        [:permission :near (person :root) 2] [:give :near (person :dave) (person :root) 3] [:give :near (person :bea) (person :dave) 4]
                        [:give :near (person :ann) (person :bea) 5] [:give-run :near :run-7 (person :ann) (person :bea) 6] [:end :near (person :dave) 7]
                        [:fact :near (person :maker) :p :text "the secret" 8] [:fact :far (person :maker) :p :text "f0" 9]
                        [:give :near (person :bea) (person :root) 10]])
        writes (map-indexed (fn [i [kind]]
                              (case kind
                                :bea-write-only     [:give :near (person :ann) (person :bea) (+ 100 i) {:allows {:write :all}}]
                                :bea-run-write-only [:give-run :near :run-7 (person :ann) (person :bea) (+ 100 i) {:allows {:write :all}}]
                                :bea-again          [:give :near (person :bea) (person :root) (+ 100 i)]
                                :near               [:fact :near (person :maker) :p :text (str "n" i) (+ 100 i)]
                                :bea-gives          [:give :near (person :ann) (person :bea) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks [:place :p :text] :asker (person :ann) :session :run-7 :lanes [:near :far]}))))

(defn- every-small-stale-chain
  [options]
  (let [tells (atom 0)
        given (atom #{})
        played (with-redefs [told (let [told* told]
                                    (fn [world tell]
                                      (when tell
                                        (swap! tells inc)
                                        (let [answer (or (:anew tell) {:place (val (first (:changed tell)))})]
                                          (when (= :near (get-in answer [:place :lane]))
                                            (swap! given conj (get-in answer [:place :value])))))
                                      (told* world tell)))]
                 (vec (for [writes (lists-up-to 3 options)]
                        (assoc (every-order (stale-chain-world writes) nil) :is writes))))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :tells @tells
     :given @given
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

(defn- every-small-unseen
  []
  (let [tells (atom 0)
        options [[:give-run-to-another] [:end-run] [:end-above] [:give-above] [:near]]
        played (with-redefs [told (let [told* told]
                                    (fn [world tell]
                                      (when tell (swap! tells inc))
                                      (told* world tell)))]
                 (vec (for [writes (lists-up-to 3 options)]
                        (assoc (every-order (unseen-world writes) nil) :is writes))))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :tells @tells
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

(defn- every-small-run
  [n asks fault]
  (let [options [[:end-run] [:give-run] [:end-above] [:give-above] [:give-own] [:end-own] [:give-run-to-another] [:near]]
        played (for [writes (lists-up-to n options)]
                 (assoc (every-order (run-world writes asks) fault) :is writes))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

(defn- every-small-chain
  [n asks fault]
  (let [options [[:end-above] [:give-above] [:end-own] [:give-own] [:give-by-root] [:near]]
        played (for [writes (lists-up-to n options)]
                 (assoc (every-order (chain-world writes asks) fault) :is writes))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

(defn- every-small-standing
  "Every list of up to n writes of `:close`, `:open`, `:permission` and the two
  facts, from four starts."
  [world [nearer farther] n asks fault]
  (let [options [[:close] [:open] [:permission] [nearer] [farther]]
        played (for [open? [true false]
                     permission? [false true]
                     writes (lists-up-to n options)]
                 (assoc (every-order (world open? permission? writes asks) fault)
                        :is [open? permission? writes]))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

;; ---------------------------------------------- the lanes of the plan's traces

(def ^:private alice "Alice's query parts: who asks." {:asker (person :alice)})

(def ^:private traces
  "The tables of the plan's traces. `:alice-notes` and `:team` are streaming
  and closed; `:base` is microbatch and open to `:anyone`. Alice holds a
  permission in the first two, Bob in `:team` only. `:alice-notes` builds on
  `:team` and `:base`. `:team` marks `:parent` since the stamp 700, the base
  since 300; `:alice-notes` marks nothing."
  (reduce write {}
          [[:make :alice-notes 1] [:make :team 1] [:make :base 1] [:open :base 1]
           [:permission :alice-notes (person :alice) 2] [:permission :team (person :alice) 2] [:permission :team (person :bob) 3]
           [:list :alice-notes [:team :base] 4]
           [:marks :team #{:parent} 700] [:marks :base #{:parent} 300]
           [:fact :base :system :note-41 :text "from the base" 310]
           [:fact :base :system :note-41 :title "a title" 311]
           [:fact :base :system :block-1 :parent :canvas-1 320]
           [:fact :base :system :block-2 :parent :canvas-1 321]
           [:fact :team (person :alice) :block-3 :parent :canvas-1 710]
           [:fact :team (person :alice) :block-1 :parent :canvas-2 711]
           [:fact :team (person :bob) :note-41 :text "from the team" 712]]))

(def ^:private chains
  "Tables in which each permission of the asker's is lent two deep beneath
  `:root`, and her sitting `:run-7` holds one lent beneath `:cal`."
  (reduce write {}
          (concat
           (for [l [:start :mid :far :extra]
                 w [[:make l 1] [:permission l (person :root) 3] [:give l (person :bea) (person :root) 4]
                    [:give l (person :cal) (person :root) 5] [:give l (person :ann) (person :bea) 6]
                    [:give-run l :run-7 (person :ann) (person :cal) 7]
                    [:marks l #{:parent} 8]]]
             w)
           [[:list :start [:mid :far] 9]
            [:fact :mid (person :maker) :b1 :parent :canvas-1 20]
            [:fact :far (person :maker) :b2 :parent :canvas-1 21]
            [:fact :extra (person :maker) :b3 :parent :canvas-1 22]])))

(defn- play-once-while-written
  "Plays a read asked once as `play-once` does, with one write before the
  select round that `at-round` names. Gives how many select rounds fetched
  something."
  [tables query at-round a-write]
  (let [bump (fn [epochs sources]
               (reduce (fn [m lane] (update m lane (fnil inc 0)))
                       epochs
                       (distinct (map :lane sources))))]
    (loop [tables tables sel-round 0 ask-round 0 known {} fact-epochs {} rights-at {}]
      (let [tables (if (= sel-round at-round) (write tables a-write) tables)
            at-round (if (= sel-round at-round) -1 at-round)
            snapshots (into {}
                            (keep (fn [[lane {:keys [epoch snapshot]}]]
                                    (when (= epoch (get fact-epochs lane 0))
                                      [lane snapshot])))
                            rights-at)
            reading (sources/reading query known :once numbers nil snapshots)
            missing (:missing reading)
            needs-rights (:needs-rights reading)]
        (cond
          (or (> sel-round 20) (> ask-round 40)) sel-round
          (seq missing)
          (recur tables (inc sel-round) ask-round
                 (into known (map (fn [source] [source (table-value tables source)])) missing)
                 (bump fact-epochs missing)
                 rights-at)
          (seq needs-rights)
          (let [snaps (into {}
                            (map (fn [[_ lane]]
                                   [lane (run-rights tables lane :see (:asker query) (:session query))]))
                            needs-rights)]
            (recur tables sel-round (inc ask-round) known fact-epochs
                   (reduce (fn [m [lane snap]]
                             (assoc m lane {:epoch (get fact-epochs lane 0) :snapshot snap}))
                           rights-at snaps)))
          :else sel-round)))))

(defn- open-live-while-written
  "Opens a live read as `open-live` does, with one write before the round that
  `at-round` names. Gives how many rounds ran."
  [tables query at-round a-write]
  (loop [tables tables held {} snapshots {} round 0 at-round at-round]
    (let [[tables held at-round]
          (if (= round at-round)
            (let [t (write tables a-write)]
              [t (into {} (map (fn [[source _]] [source (table-value t source)])) held) -1])
            [tables held at-round])
          reading (sources/reading query held :live numbers (set (keys held)) snapshots)
          {:keys [join leave]} (sources/moves reading (set (keys held)))
          needs-rights (:needs-rights reading)]
      (cond
        (> round 40) round
        (or (seq join) (seq leave))
        (recur tables
               (into (reduce dissoc held leave) (map (fn [source] [source (table-value tables source)])) join)
               snapshots
               (inc round)
               at-round)
        (seq needs-rights)
        (let [snaps (into {}
                          (map (fn [[_ lane]]
                                 [lane (run-rights tables lane :see (:asker query) (:session query))]))
                          needs-rights)]
          (recur tables held (merge snapshots snaps) (inc round) at-round))
        :else round))))

(defn- live-rounds
  "Opens a live read of a query on the traces' tables. Gives how many rounds
  ran, and how many pushes the read holds."
  [query]
  (let [world (open-live traces [] query)]
    [(:rounds world) (count (:held world))]))

;; ---------------------------------------------------------------- the checks

(deftest the-form-of-a-query
  (let [refuses (fn [thing] (let [{:keys [refuse at]} (query/check thing)] [refuse at]))
        good {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:team]}
        forms {"a place"                 {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:team]}
               "an entity"               {:asks [:entity :note-41] :asker (agent :ada) :from-lane :team}
               "a run of keys"           {:asks [:run [:e :a] [:e :m]] :asker (run :r1) :lanes [:team]}
               "a run of entities"       {:asks [:run [:e1] [:e2]] :asker :system :lanes [:team]}
               "a run from an entity on" {:asks [:run [:e] :last] :asker (person :alice) :lanes [:team]}
               "a whole lane"           {:asks [:run :first :last] :not-signed-in true :lanes [:base]}
               "a run to a place"        {:asks [:run :first [:e :k]] :asker (person (UUID. 1 2)) :lanes '(:team)}
               "what points at a thing"  {:asks [:points-at :parent (UUID. 0 7)] :asker (person :alice) :lanes [:team]
                                          :with #{:writer :permission} :stores-own-facts true}
               "a place, with each part of a row" {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:team]
                                                   :with #{:writer :permission :time}}
               "a lane's acts by stamp"  {:asks [:acts 0 100] :asker (person :alice) :from-lane :team}
               "a lane's acts, named as a list of one" {:asks [:acts 7 1] :not-signed-in true :lanes [:base]}}
        faults [["no map"                          5                                                [:not-a-query nil]]
                ["a record"                        (->Thing [:place :note-41 :text] (person :alice) [:team]) [:not-a-query nil]]
                ["a part that the form does not hold" (assoc good :live true)                     [:part-not-known :live]]
                ["no :asks"                        (dissoc good :asks)                              [:asks-no-word-known :asks]]
                ["a word that is not known"        (assoc good :asks [:find "text"])                [:asks-no-word-known :asks]]
                ["an :asks that is no vector"      (assoc good :asks :place)                        [:asks-no-word-known :asks]]
                ["a place with no key"             (assoc good :asks [:place :note-41])             [:place-not-named :asks]]
                ["an entity that is no id"         (assoc good :asks [:entity "note"])              [:entity-not-named :asks]]
                ["a run with one end"              (assoc good :asks [:run [:e]])                   [:run-not-named :asks]]
                ["a run whose end is no end"       (assoc good :asks [:run [:e] :middle])           [:run-not-named :asks]]
                ["what points at, with no thing"   (assoc good :asks [:points-at :parent])          [:points-at-not-named :asks]]
                ["what points at nil"              (assoc good :asks [:points-at :parent nil])      [:points-at-not-named :asks]]
                ["a read of acts from below 0"     (assoc good :asks [:acts -1 5])                  [:acts-not-named :asks]]
                ["a read of acts of no count"      (assoc good :asks [:acts 0 0])                   [:acts-not-named :asks]]
                ["a read of acts from a start that is no whole number" (assoc good :asks [:acts 1.5 5]) [:acts-not-named :asks]]
                ["a read of acts of two lanes"    (assoc good :asks [:acts 0 5] :lanes [:team :base]) [:acts-not-of-one-lane :lanes]]
                ["an asker that is no party"       (assoc good :asker "alice")                      [:asker-not-a-party :asker]]
                ["an asker that is a bare keyword" (assoc good :asker :asha)                        [:asker-not-a-party :asker]]
                ["anyone as the asker"             (assoc good :asker :anyone)                      [:asker-not-a-party :asker]]
                ["anyone signed in as the asker"   (assoc good :asker :anyone-signed-in)            [:asker-not-a-party :asker]]
                ["an asker that is a UUID"         (assoc good :asker (UUID. 1 2))                  [:asker-not-a-party :asker]]
                ["no asker"                        (dissoc good :asker)                             [:no-one-asker :asker]]
                ["an asker, and not signed in"     (assoc good :not-signed-in true)                 [:no-one-asker :asker]]
                ["not signed in, said with false"  (assoc (dissoc good :asker) :not-signed-in false) [:no-one-asker :not-signed-in]]
                ["a session that is no id"         (assoc good :session "run-7")                    [:session-is-no-id :session]]
                ["a session, and not signed in"    (assoc (dissoc good :asker) :not-signed-in true :session :run-7) [:run-with-no-asker :session]]
                ["lanes that are no list"         (assoc good :lanes :team)                       [:lanes-not-a-list-of-lanes :lanes]]
                ["a lane that is no id"           (assoc good :lanes [:team "base"])              [:lanes-not-a-list-of-lanes :lanes]]
                ["a start lane that is no id"     (assoc (dissoc good :lanes) :from-lane 7)      [:from-lane-is-no-lane :from-lane]]
                ["no lanes"                       (dissoc good :lanes)                            [:lanes-not-named-one-way :lanes]]
                ["a list and a start lane"        (assoc good :from-lane :team)                   [:lanes-not-named-one-way :lanes]]
                ["an empty list"                   (assoc good :lanes [])                          [:lanes-not-named-one-way :lanes]]
                ["66 lanes"                       (assoc good :lanes (mapv #(keyword (str "l" %)) (range 66))) [:too-many-lanes :lanes]]
                ["a part of a row that is not known" (assoc good :with #{:value})                 [:part-of-a-row-not-known :with]]
                ["a :with that is no set"          (assoc good :with [:writer])                     [:part-of-a-row-not-known :with]]
                ["an asker of 300 characters"      (assoc good :asker (perm/long-person 300)) [:id-over-the-limit :asker]]
                ["a lane of 300 characters"       (assoc good :lanes [(keyword (apply str (repeat 300 "l")))]) [:id-over-the-limit :lanes]]
                ["an entity of 300 characters"     (assoc good :asks [:entity (keyword (apply str (repeat 300 "e")))]) [:id-over-the-limit :asks]]
                ["a key of 300 characters"         (assoc good :asks [:place :e (keyword (apply str (repeat 300 "k")))]) [:id-over-the-limit :asks]]
                ["a run whose ends are out of order" (assoc good :asks [:run [:e2] [:e1]])         [:run-ends-out-of-order :asks]]
                ["a run that starts at :last"      (assoc good :asks [:run :last [:e]])             [:run-ends-out-of-order :asks]]
                ["a run that ends at :first"       (assoc good :asks [:run [:e] :first])            [:run-ends-out-of-order :asks]]
                ["a run whose end holds a letter outside the first plane" (assoc good :asks [:run [:e] [(keyword (str "x" (String. (Character/toChars 0x1D49C))))]]) [:run-end-not-ordered :asks]]
                ["a run whose end holds a letter from U+E000 on" (assoc good :asks [:run [(keyword "x\uFB00") :k] :last]) [:run-end-not-ordered :asks]]
                ["a place under a store key, not asked for" (assoc good :asks [:place :team :softland.store/depot]) [:store-key-not-asked-for :asks]]
                ["what points at, under a store key" (assoc good :asks [:points-at :softland.store/permission :x] :stores-own-facts true) [:store-key-not-asked-for :asks]]
                ["a :stores-own-facts that is no true or false" (assoc good :stores-own-facts "yes") [:not-a-query :stores-own-facts]]]
        given (set (map (comp first #(nth % 2)) faults))
        throws (reify clojure.lang.ILookup
                 (valAt [_ _] (throw (ex-info "the proof made this read throw" {})))
                 (valAt [_ _ _] (throw (ex-info "the proof made this read throw" {}))))
        thrower (proxy [clojure.lang.APersistentMap] []
                  (seq [] (throw (ex-info "the proof made this read throw" {})))
                  (count [] 1)
                  (valAt ([_] (throw (ex-info "the proof made this read throw" {})))
                    ([_ _] (throw (ex-info "the proof made this read throw" {}))))
                  (containsKey [_] true)
                  (iterator [] (throw (ex-info "the proof made this read throw" {}))))]
    (doseq [[what thing] forms]
      (check (str "a query is taken: " what) true (some? (q thing))))
    (check "the one form of a query: each part there, an asker of nil for a person who is not signed in, one of the two ways of naming lanes nil, a set, true or false"
           {:asks [:run :first :last] :asker nil :not-signed-in true :session nil :lanes [:base] :from-lane nil :with #{} :stores-own-facts false}
           (q (get forms "a whole lane")))
    (check "a :with that asks for each of the three parts of a row, the writer, the permission and the time, is a known query, and its one form keeps the three"
           #{:writer :permission :time} (:with (q (get forms "a place, with each part of a row"))))
    (check "a list of lanes that is no vector, and a thing that is a UUID, come out in the one form"
           [[:team] true]
           (let [one (q (get forms "a run to a place"))] [(:lanes one) (vector? (:lanes one))]))
    (doseq [[what thing expected] faults]
      (check (str "a query is turned away: " what) expected (refuses thing)))
    (check "each of the check's words was given, and each is declared" [query/reasons true]
           [given (every? query/reasons given)])
    (check "the check never throws: a thing that throws when it is read, of two kinds, and things that are no map"
           [:not-a-query :not-a-query :not-a-query :not-a-query :not-a-query]
           (mapv (comp :refuse query/check) [thrower throws nil "a query" [:place :e :k]]))
    (check "a list of lanes with no end is turned away after 66, and its lanes are not read"
           [:too-many-lanes :lanes]
           (refuses (assoc good :lanes (map (fn [i] (if (< i 70) (keyword (str "l" i)) (throw (ex-info "read past the bound" {}))))
                                             (iterate inc 0)))))
    (check "the numbers: a default for each that is left out or is none; a number that is given is taken"
           [query/default-numbers
            (assoc query/default-numbers :wait-ms 300 :run-start-places 4 :max-pushes-under-one-routing-id 5)
            query/default-numbers]
           [(query/numbers nil)
            (query/numbers {:wait-ms 300 :run-start-places 4 :max-places-of-one-run 0 :max-checks-of-one-read "many"
                            :max-pushes-under-one-routing-id 5 :max-checks-of-one-live-read -1 :more 1})
            (query/numbers 7)])
    (check "the read path's nine numbers, written out a second time: a read kept live may need 250 checks of nearer lanes where one asked once may need 40,000, and the read path holds 900 pushes on one table under one routing id, under the 1,000 that Rama opens on one table on one task; a read of acts gives 1,000 decisions at most, and things that weigh 32 MiB in all, the heaviest thing that a gate keeps"
           {:run-start-places 256 :max-places-of-one-run 20000 :max-checks-of-one-read 40000 :max-checks-of-one-live-read 250
            :max-pushes-under-one-routing-id 900 :max-paths-in-one-call 2000 :wait-ms 10000
            :max-acts-of-one-read 1000 :max-thing-weight-of-one-read 33554432}
           query/default-numbers)
    (shown "the form of a query:" (count forms) "queries are taken, one for each word, party kind and kind of end;"
           (count faults) "faults are each turned away with a word and the part,"
           (count given) "different words, every word that the declaration holds:" (pr-str (sort given)))
    (shown "the check never throws: a thing that throws when it is read gives :not-a-query, and a list of lanes with no end gives :too-many-lanes after 66 are counted")))

(deftest a-word-is-declared-in-three-places
  (check "the words of a query that read facts, what each reads in one lane, and how its rows are put together have the same keys; the word of a lane's acts is in the first map alone, since `softland.store.read-path.acts` reads it and puts it together"
         [#{:place :entity :run :points-at :acts} true true false false]
         (let [of-facts (disj (set (keys query/words)) :acts)]
           [(set (keys query/words)) (= of-facts (set (keys sources/of-word))) (= of-facts (set (keys answer/of-word)))
            (contains? sources/of-word :acts) (contains? answer/of-word :acts)]))
  (shown "a word of a query is declared in three maps or in none:" (pr-str (sort (keys query/words)))))

(deftest the-lanes
  (let [own (fn [lane] (table-value traces (sources/own-rows-source lane lane)))
        standing-in (fn [tables lane asker session] (lanes/standing (rights-state tables lane asker session)))
        from (q {:asks [:place :note-41 :text] :asker (person :alice) :from-lane :alice-notes})
        points (q {:asks [:points-at :parent :canvas-1] :asker (person :bob) :lanes [:team]})
        with-list-on (fn [lane listed stamp]
                       (table-value (write traces [:list lane listed stamp]) (sources/own-rows-source lane lane)))
        with-list (fn [listed] (with-list-on :alice-notes listed 900))
        with-mark (fn [marked] (table-value (write traces [:marks :team marked 900]) (sources/own-rows-source :team :team)))]
    (check "the lanes of a list: each once, at its first place"
           [:alice-notes :team :nowhere :base]
           (lanes/in-order (q {:asks [:place :e :k] :asker (person :bob) :lanes [:alice-notes :team :nowhere :team :base]}) nil nil))
    (check "from a lane: the lane, then the lanes of its fact, in the fact's order, and no further"
           [:alice-notes :team :base] (lanes/in-order from (own :alice-notes) :yes))
    (check "a list that names its own lane and a lane twice: each once, the start lane first"
           [:alice-notes :base :team] (lanes/in-order from (with-list [:base :alice-notes :team :base]) :yes))
    (check "a start lane with no fact: the lane alone" [:team] (lanes/in-order (assoc from :from-lane :team) (own :team) :yes))
    (check "a start lane whose rows, or her standing in it, are not known yet: nothing can be said"
           [nil nil] [(lanes/in-order from nil :not-known-yet) (lanes/in-order from (own :alice-notes) :not-known-yet)])
    (check "a start lane that she may not see, or that is not made: the lane alone, and its list is not used"
           [[:alice-notes] [:alice-notes]]
           [(lanes/in-order from (own :alice-notes) :may-not-see) (lanes/in-order from {} :not-made)])
    (check "the home of a read is the lane that it starts from: the first lane of a list, also of a list that names it a second time, and of a list of one; the start lane of a query that takes its list from a lane"
           [:alice-notes :team :base :alice-notes]
           (mapv #(lanes/home (q (merge {:asks [:place :e :k] :asker (person :bob)} %)))
                 [{:lanes [:alice-notes :team :base]} {:lanes [:team :base :team]} {:lanes [:base]}
                  {:from-lane :alice-notes}]))
    (check "the home is read from the query alone: it is the same for each word of a query, for a person who is not signed in, and for a start lane that is not made or that she may not see, where the list is the lane alone"
           [[:team :team :team :team] :team [:nowhere [:nowhere]] [:alice-notes [:alice-notes]]]
           [(mapv #(lanes/home (q {:asks % :asker (person :bob) :lanes [:team :base]}))
                  [[:place :e :k] [:entity :e] [:run :first :last] [:points-at :parent :canvas-1]])
            (lanes/home (q {:asks [:place :e :k] :not-signed-in true :lanes [:team :base]}))
            (let [nowhere (q {:asks [:place :e :k] :asker (person :bob) :from-lane :nowhere})]
              [(lanes/home nowhere) (lanes/in-order nowhere {} :not-made)])
            (let [bob-from (assoc from :asker (person :bob))]
              [(lanes/home bob-from) (lanes/in-order bob-from (own :alice-notes) :may-not-see)])])
    (check "lent and standing permissions in a lane: a lent permission lets her see while each permission it is lent from stands and allows seeing; when the lender ends, or is lowered to write-only, she may not; a new permission to the lender on a new id does not bring hers back; a standing give to a person stays when her inviter's ends; an unfinished rights walk is not known yet"
           [:yes :may-not-see :may-not-see :yes :may-not-see :yes :yes :not-known-yet]
           (let [bea-id (uuid-of [:bea-1])
                 lab [[:make :lab 1]
                      [:permission :lab (person :root) 2]
                      [:give :lab (person :bea) (person :root) 3 {:id bea-id :lent? false}]
                      [:give :lab (person :cal) (person :bea) 4]]
                 of (fn [more] (standing-in (reduce write {} (concat lab more)) :lab (person :cal) nil))]
             [(of [])
              (of [[:end :lab (person :bea) 5]])
              (of [[:end :lab (person :bea) 5] [:give :lab (person :bea) (person :root) 6]])
              (of [[:end :lab (person :bea) 5] [:give :lab (person :bea) (person :root) 6] [:give :lab (person :cal) (person :bea) 7]])
              (of [[:change :lab bea-id {:write :all} 5]])
              (of [[:change :lab bea-id {:write :all} 5] [:change :lab bea-id perm/member-allows 6]])
              (standing-in (reduce write {} (concat lab [[:end :lab (person :root) 5]])) :lab (person :bea) nil)
              (lanes/standing (lanes/rights (lanes/rights-begin :lab (person :cal) nil)))]))
    (check "lane-rights unions across multiple permissions of a holder, reads :anyone first on :see, lets a person and no agent see through :anyone-signed-in, lets an agent see when lent from :anyone, and returns :too-many-permissions past the 32 holder-scan or lent-chain bound"
           [:yes true :yes :may-not-see :yes :yes :too-many-permissions :too-many-permissions [true false false]]
           (let [multi (reduce write {}
                               [[:make :lab 1]
                                [:permission :lab (person :root) 2]
                                [:give :lab (person :cal) (person :root) 3 {:allows {:write :all}}]
                                [:give :lab (person :cal) (person :root) 4 {:allows {:see true}}]])
                 open-lab (reduce write {} [[:make :lab 1] [:permission :lab (person :root) 2] [:open :lab 3]])
                 signed-in-id (uuid-of [:signed-in])
                 signed-in-lab (reduce write {}
                                       [[:make :lab 1]
                                        [:permission :lab (person :root) 2]
                                        [:give :lab :anyone-signed-in (person :root) 3 {:id signed-in-id :allows {:see true :write :all} :lent? false}]])
                 agent-lent (let [open-id (first-act/first-anyone-id :lab)
                                  agent-id (uuid-of [:agent-borrow])]
                              (grant-perm open-lab :streaming :lab (person :cal) open-id agent-id
                                          {:holder (agent :ada) :allows {:see true} :covers [:default-group :lab]
                                           :given-by (person :cal) :under open-id :lent-from open-id}
                                          4))
                 deep-chain (fn [depth]
                              (loop [t (reduce write {} [[:make :lab 1] [:permission :lab (person :root) 2]])
                                     parent-id (standing-perm-id :lab (person :root))
                                     d 2]
                                (if (> d depth)
                                  t
                                  (let [id (uuid-of [:chain d])]
                                    (recur (grant-perm t :streaming :lab (person :root) parent-id id
                                                       {:holder (if (= d depth) (agent :ada) (person :bea))
                                                        :allows {:see true}
                                                        :covers [:default-group :lab]
                                                        :given-by (person :root)
                                                        :under parent-id
                                                        :lent-from parent-id}
                                                       d)
                                           id
                                           (inc d))))))
                 flood (reduce (fn [t i]
                                 (write t [:give :lab (agent :ada) (person :root) (+ 10 i) {:allows {:write :all}}]))
                               (reduce write {} [[:make :lab 1] [:permission :lab (person :root) 2]])
                               (range 33))]
             [(standing-in multi :lab (person :cal) nil)
              (= (run-rights open-lab :lab :see (person :cal) :sit-1)
                 (run-rights open-lab :lab :see nil nil))
              (standing-in signed-in-lab :lab (person :cal) nil)
              (standing-in signed-in-lab :lab (agent :ada) nil)
              (standing-in agent-lent :lab (agent :ada) nil)
              (standing-in (deep-chain 32) :lab (agent :ada) nil)
              (standing-in (deep-chain 33) :lab (agent :ada) nil)
              (standing-in flood :lab (agent :ada) nil)
              [(lanes/valid-rights-query? :team :base :see (person :alice) :s1)
               (lanes/valid-rights-query? :alice-notes :team :see (person :alice) :s1)
               (lanes/valid-rights-query? :team :team :unknown (person :alice) nil)]]))
    (check "an asker who asks in a sitting: she sees the lane under a permission for that sitting when it is hers and stands; not in no sitting or another sitting; not under another holder's sitting permission; not when ended or when its lender ended; and under her any-sitting permission whatever the sitting holds"
           [:yes :may-not-see :may-not-see :may-not-see :may-not-see :may-not-see :yes :not-known-yet]
           (let [lab [[:make :lab 1] [:permission :lab (person :root) 2] [:give :lab (person :bea) (person :root) 3]
                      [:give-run :lab :run-7 (person :cal) (person :bea) 4]]
                 of (fn [more asker session]
                      (standing-in (reduce write {} (concat lab more)) :lab asker session))]
             [(of [] (person :cal) :run-7)
              (of [] (person :cal) nil)
              (of [] (person :cal) :run-8)
              (of [] (person :dov) :run-7)
              (of [[:end-run :lab :run-7 (person :cal) 5]] (person :cal) :run-7)
              (of [[:end :lab (person :bea) 5]] (person :cal) :run-7)
              (of [[:end-run :lab :run-7 (person :cal) 5] [:give :lab (person :cal) (person :root) 6]] (person :cal) :run-7)
              (sources/lane-standing :lab :lab :live {:lab (run-rights (reduce write {} lab) :lab :see (person :cal) :run-7)} {})]))
    (check "the standing and live read-set validation: the five outcomes of standing, and live standing holds only while open pushes match :read-set stamps entry for entry"
           [[:not-known-yet :not-made :too-many-permissions :yes :may-not-see]
            [:yes :not-known-yet :not-known-yet]]
           (let [snap (run-rights traces :base :see (person :alice) nil)
                 own-src (sources/own-rows-source :base :base)
                 anyone-holders (sources/holders-source :base :base ":base :anyone * " ":base :anyone *!")
                 anyone-perm (sources/permission-row-source :base :base (chain/place :base (first-act/first-anyone-id :base)))
                 known-ok {own-src (table-value traces own-src)
                           anyone-holders (table-value traces anyone-holders)
                           anyone-perm (table-value traces anyone-perm)}
                 known-stale (assoc-in known-ok [anyone-perm :stamp] 999)
                 known-missing (dissoc known-ok anyone-holders)]
             [[(lanes/standing nil)
               (lanes/standing {:made? false :sees? false :standing :may-not-see})
               (lanes/standing {:made? true :sees? false :standing :too-many-permissions})
               (lanes/standing {:made? true :sees? true :standing :yes})
               (lanes/standing {:made? true :sees? false :standing :may-not-see})]
              [(sources/lane-standing :base :base :live {:base snap} known-ok)
               (sources/lane-standing :base :base :live {:base snap} known-stale)
               (sources/lane-standing :base :base :live {:base snap} known-missing)]]))
    (check "what no fact reads as: no lane is open by code, who may read acts starts at members, a list is nothing, no key is marked"
           [nil :members [] nil]
           [(first-act/starts-with :base :softland.store/who-may-see)
            (lanes/who-may-read-acts (own :team) :team)
            (lanes/builds-on (own :team) :team)
            (lanes/marked-since (own :alice-notes) :alice-notes :parent)])
    (check "a person who is not signed in sees the base and no members lane; the system is a who like any other"
           [:yes :may-not-see :may-not-see]
           [(standing-in traces :base nil nil)
            (standing-in traces :team nil nil)
            (standing-in traces :team :system nil)])
    (check "a list that is written again with the same lanes: the row differs, and its meaning does not"
           [false true]
           (let [again (with-list-on :alice-notes [:team :base] 900)]
             [(rows/same-row? (get (own :alice-notes) (rows/place-key :alice-notes :alice-notes :softland.store/builds-on))
                              (get again (rows/place-key :alice-notes :alice-notes :softland.store/builds-on)))
              (= (lanes/meaning from :alice-notes (own :alice-notes) :yes)
                 (lanes/meaning from :alice-notes again :yes))]))
    (check "a list, and a mark of the asked key, that are written in a lane that she may not see mean the same before and after"
           [true true]
           (let [carol-from (assoc from :asker (person :carol))
                 carol-points (assoc points :asker (person :carol))]
             [(= (lanes/meaning carol-from :alice-notes (own :alice-notes) :may-not-see)
                 (lanes/meaning carol-from :alice-notes (with-list [:base]) :may-not-see))
              (= (lanes/meaning carol-points :team (own :team) :may-not-see)
                 (lanes/meaning carol-points :team (with-mark #{}) :may-not-see))]))
    (check "for an asker who may see the lane, a list that is written again and a mark that is taken off mean another thing"
           [false false]
           [(= (lanes/meaning from :alice-notes (own :alice-notes) :yes)
               (lanes/meaning from :alice-notes (with-list [:base]) :yes))
            (= (lanes/meaning points :team (own :team) :yes)
               (lanes/meaning points :team (with-mark #{}) :yes))])
    (check "closing an open lane invalidates the snapshot that rested on everyone, turning live meaning to not-known-yet, while a fresh snapshot for a member keeps her standing yes"
           [false :not-known-yet :yes]
           (let [open-team (write traces [:open :team 800])
                 snap-open (run-rights open-team :team :see (person :bob) nil)
                 closed-team (write open-team [:close :team 900])
                 known-closed (into {}
                                    (map (fn [s] [s (table-value closed-team s)]))
                                    (conj (sources/read-set-sources :team :team (:read-set snap-open))
                                          (sources/own-rows-source :team :team)))
                 st-stale (sources/lane-standing :team :team :live {:team snap-open} known-closed)
                 st-fresh (standing-in closed-team :team (person :bob) nil)]
             [(= (lanes/meaning points :team (table-value open-team (sources/own-rows-source :team :team)) :yes)
                 (lanes/meaning points :team (table-value closed-team (sources/own-rows-source :team :team)) st-stale))
              st-stale
              st-fresh]))
    (check "what an answer says of a lane that it left out: the lane and why, and nothing else"
           {:lane :alice-notes :why :may-not-see} (lanes/left-out :alice-notes :may-not-see))
    (shown "the lanes: a list gives each lane once at its first place; from a lane the read looks in the lane and then in the lanes of its own fact, and no further; a start lane that she may not see gives itself alone")
    (shown "the home of a read is the lane that it starts from, read from the query alone: the first lane of :lanes, or :from-lane, whatever is asked, whoever asks and whatever the tables hold of that lane")
    (shown "lane-rights: reads :anyone first on :see, unions across multiple permissions, walks lent chains up to 32, caps rights by lenders, returns :too-many-permissions past 32, and holds live standing to entry-for-entry :read-set stamp equality")))

(deftest a-step-that-joins-or-leaves-a-source-owes-the-whole-answer
  (let [;; `:near` starts open to `:anyone` while `:ann` also holds her own permission;
        ;; closing `:near` while `:near`'s fact changes invalidates the `:anyone` snapshot,
        ;; joins her own permission watches, tells nothing yet, and owes the whole answer.
        world (standing-world true true [[:close] [:near]] [:place :p :text])
        after-writes (-> world (move [:write] nil) (move [:write] nil))
        anyone-holders (sources/holders-source :near :near ":near :anyone * " ":near :anyone *!")
        anyone-perm (sources/permission-row-source :near :near (chain/place :near (first-act/first-anyone-id :near)))
        fact (sources/place-source :near :near :p :text)
        ann-perm (sources/permission-row-source :near :near (chain/place :near (standing-perm-id :near (person :ann))))
        re-queried (-> after-writes
                       (move [:take anyone-holders] nil)
                       (move [:take anyone-perm] nil)
                       (move [:take fact] nil)
                       (move [:handle fact] nil)
                       (move [:handle anyone-holders] nil)
                       (move [:handle anyone-perm] nil)
                       (move [:serve-rights :near] nil)
                       (move [:arrive-rights :near] nil))
        newly-asked (vec (for [[src push] (:pushes re-queried) :when (= :asked (:phase push))] src))
        given-her (reduce (fn [w src] (-> w (move [:serve src] nil) (move [:arrive src] nil)))
                          re-queried
                          newly-asked)
        ;; a step that leaves an order whose mark was taken off
        marked (reduce write {} [[:make :l1 1] [:open :l1 2] [:marks :l1 #{:parent} 3]
                                 [:fact :l1 (person :maker) :p :parent :canvas-1 11]])
        open (open-live marked [[:marks :l1 #{} 100]] (q {:asks [:points-at :parent :canvas-1] :asker (person :ann) :lanes [:l1]}))
        own (sources/own-rows-source :l1 :l1)
        unmarked (-> open (move [:write] nil) (move [:take own] nil) (move [:handle own] nil))]
    (check "when the give to everyone ends while a fact changes, the read joins her own permission watches, tells nothing of its place yet, and owes"
           [true true (:screen world)]
           [(contains? (:held re-queried) ann-perm) (true? (:owed? re-queried)) (:screen re-queried)])
    (check "when her permission is given, the whole answer comes, with the fact in it, and the mark is taken off"
           [false "n1" :near]
           [(true? (:owed? given-her)) (get-in given-her [:screen :place :value]) (get-in given-her [:screen :place :lane])])
    (check "a step that leaves an order whose mark was taken off gives the whole answer anew, with not-marked and no place"
           [false {} [{:lane :l1 :not-marked true}] 3]
           [(true? (:owed? unmarked)) (get-in unmarked [:screen :places]) (get-in unmarked [:screen :orders]) (count (:held unmarked))])
    (shown "a step that joins or leaves a source that is no check owes the whole answer: closing a public lane joins her own permission watches and owes until they arrive; taking off a mark leaves the order push and gives the whole answer anew")))

(deftest the-sources
  (let [digest (rows/value-digest-of :canvas-1)
        own-rows (fn [lane] (table-value traces (sources/own-rows-source lane lane)))
        alice-team-id (standing-perm-id :team (person :alice))
        alice-base-id (standing-perm-id :base (person :alice))
        of (fn [asks home lane]
             ((get sources/of-word (first asks)) (q (merge alice {:asks asks :lanes (vec (distinct [home lane]))}))
              lane (own-rows lane)))
        read-in (fn [query lane] (set (filter #(= lane (:lane %)) (keys (:known (play-once traces query))))))
        held-in (fn [query lane] (set (filter #(= lane (:lane %)) (:held (open-live traces [] query)))))
        asks-of-each-word [[:place :note-41 :text] [:entity :note-41] [:run [:note-41 :a] [:note-41 :z]]
                           [:run [:block-1] [:note-9]] [:run :first :last] [:points-at :parent :canvas-1]]]
    (check "the lane's own rows: one run of the lane's own entity under the store's namespace. A streaming lane's routing id is the lane, whatever the home; the base's is the home"
           [{:gate :streaming :table :facts :lane :team :routing-id :team
             :path [:own-rows ":team :team :softland.store/" ":team :team :softland.store0"]}
            {:gate :microbatch :table :facts :lane :base :routing-id :team
             :path [:own-rows ":base :base :softland.store/" ":base :base :softland.store0"]}
            {:gate :microbatch :table :facts :lane :base :routing-id :base
             :path [:own-rows ":base :base :softland.store/" ":base :base :softland.store0"]}]
           [(sources/own-rows-source :alice-notes :team) (sources/own-rows-source :team :base)
            (sources/own-rows-source :base :base)])
    (check "a holder run in permissions-by-holder and a permission row on its own UUID: in a streaming lane under the lane, and in the base under the home"
           [{:gate :streaming :table :permissions-by-holder :lane :team :routing-id :team
             :path [:holders ":team :person.alice * " ":team :person.alice *!"]}
            {:gate :microbatch :table :permissions-by-holder :lane :base :routing-id :team
             :path [:holders ":base :anyone * " ":base :anyone *!"]}
            {:gate :streaming :table :facts :lane :team :routing-id :team
             :path [:row (chain/place :team alice-team-id)]}
            {:gate :microbatch :table :facts :lane :base :routing-id :team
             :path [:row (chain/place :base alice-base-id)]}]
           [(apply sources/holders-source :alice-notes :team (rows/by-holder-run :team (person :alice) nil))
            (apply sources/holders-source :team :base (rows/by-holder-run :base :anyone nil))
            (sources/permission-row-source :alice-notes :team (chain/place :team alice-team-id))
            (sources/permission-row-source :team :base (chain/place :base alice-base-id))])
    (check "a place: the lane routes a streaming lane, the home a microbatch lane, and never the entity"
           [[{:gate :streaming :table :facts :lane :team :routing-id :team :path [:row ":team :note-41 :text"]}]
            [{:gate :microbatch :table :facts :lane :base :routing-id :team :path [:row ":base :note-41 :text"]}]
            [{:gate :microbatch :table :facts :lane :base :routing-id :base :path [:row ":base :note-41 :text"]}]]
           [(of [:place :note-41 :text] :team :team) (of [:place :note-41 :text] :team :base)
            (of [:place :note-41 :text] :base :base)])
    (check "an entity: one run, under the home in the base"
           [[{:gate :streaming :table :facts :lane :team :routing-id :team :path [:run ":team :note-41 " ":team :note-41!"]}]
            [{:gate :microbatch :table :facts :lane :base :routing-id :team :path [:run ":base :note-41 " ":base :note-41!"]}]
            [{:gate :microbatch :table :facts :lane :base :routing-id :base :path [:run ":base :note-41 " ":base :note-41!"]}]]
           [(of [:entity :note-41] :team :team) (of [:entity :note-41] :team :base) (of [:entity :note-41] :base :base)])
    (check "a run of keys of one entity, a run across entities and a whole lane are each one source in the base, under the home; in a streaming lane each is one source under the lane"
           [[[:team] [[:run ":base :note-41 :a" ":base :note-41 :z "]]]
            [[:team] [[:run ":base :a " ":base :b!"]]]
            [[:team] [[:run ":base " ":base!"]]]
            [[:base] [[:run ":base :a " ":base :b!"]]]
            [[:team] [[:run ":team :a " ":team :b!"]]]]
           (mapv (fn [[asks home lane]] ((juxt #(mapv :routing-id %) #(mapv :path %)) (of asks home lane)))
                 [[[:run [:note-41 :a] [:note-41 :z]] :team :base]
                  [[:run [:a] [:b]] :team :base]
                  [[:run :first :last] :team :base]
                  [[:run [:a] [:b]] :base :base]
                  [[:run [:a] [:b]] :alice-notes :team]]))
    (check "the ends of a run: a place is in the run that ends at it; a whole lane is a run and no table"
           [[:run ":team :e :a" ":team :e :m "] [:run ":team " ":team!"] [:run ":team :e " ":team!"]]
           (mapv #(:path (first (of % :team :team))) [[:run [:e :a] [:e :m]] [:run :first :last] [:run [:e] :last]]))
    (check "what points at a thing: the thing's run in the facts by value, by the stamp of the mark, one source; in the base under the home, and never under the value's digest; a lane that does not mark the key reads nothing"
           [[{:gate :streaming :table :facts-by-value :lane :team :routing-id :team
              :path [:run (str ":team :parent 700 " digest " ") (str ":team :parent 700 " digest "!")]}]
            [{:gate :microbatch :table :facts-by-value :lane :base :routing-id :team
              :path [:run (str ":base :parent 300 " digest " ") (str ":base :parent 300 " digest "!")]}]
            [{:gate :microbatch :table :facts-by-value :lane :base :routing-id :base
              :path [:run (str ":base :parent 300 " digest " ") (str ":base :parent 300 " digest "!")]}]
            []]
           [(of [:points-at :parent :canvas-1] :team :team) (of [:points-at :parent :canvas-1] :team :base)
            (of [:points-at :parent :canvas-1] :base :base) (of [:points-at :parent :canvas-1] :alice-notes :alice-notes)])
    (check "a check of a nearer lane at a place is the source of that place, in a streaming lane and in the base; a check of the base has the home as its routing id"
           [true true {:gate :microbatch :table :facts :lane :base :routing-id :alice-notes :path [:row ":base :block-3 :parent"]}]
           [(= (sources/place-source :team :team :block-1 :parent) (sources/check-source :team :team :block-1 :parent))
            (= (sources/place-source :team :base :block-1 :parent) (sources/check-source :team :base :block-1 :parent))
            (get-in (:reading (play-once traces (q (merge alice {:asks [:points-at :parent :canvas-1] :lanes [:alice-notes :base :team]}))))
                    [:by-lane :base :checks [:block-3 :parent]])])
    (check "a source of a streaming lane is the same under every home: the lane's own rows, a holder run, a permission row, a place, a check, a run and the order by value"
           [true true true true true true true]
           (mapv (fn [make] (apply = (map make [:team :alice-notes :base :nowhere])))
                 [#(sources/own-rows-source % :team)
                  #(apply sources/holders-source % :team (rows/by-holder-run :team (person :alice) nil))
                  #(sources/permission-row-source % :team (chain/place :team alice-team-id))
                  #(sources/place-source % :team :note-41 :text)
                  #(sources/check-source % :team :block-1 :parent)
                  #(sources/run-source % :team ":team :a " ":team :b!")
                  #(sources/by-value-source % :team :parent 700 :canvas-1)]))
    (check "every source of the base that a read asked once comes to read has the read's home as its routing id, for each word and each kind of run: through her notes and the team, through her notes with the team behind the base, through the team alone, and from the base itself"
           [#{:alice-notes} #{:alice-notes} #{:team} #{:base}]
           (mapv (fn [lanes]
                   (into #{}
                         (mapcat (fn [asks] (map :routing-id (read-in (q (merge alice {:asks asks :lanes lanes})) :base))))
                         asks-of-each-word))
                 [[:alice-notes :team :base] [:alice-notes :base :team] [:team :base] [:base]]))
    (check "each word reads the base as one fact source; asked once it reads the base's own rows and asks lane-rights, and kept live it adds the shared :anyone watches"
           [[1 1 1 1 1 1]
            [[:own-rows] [:own-rows] [:own-rows] [:own-rows] [:own-rows] [:own-rows]]
            [:holders :own-rows :row]]
           (let [read (fn [asks]
                        (let [query (q (merge alice {:asks asks :lanes [:team :base]}))
                              {:keys [reading known]} (play-once traces query)]
                          [(count (get-in reading [:by-lane :base :facts]))
                           (vec (sort (map (comp first :path)
                                           (filter #(and (= :base (:lane %)) (sources/rights-kind query %)) (keys known)))))]))
                 live-q (q (merge alice {:asks [:place :note-41 :text] :lanes [:team :base]}))]
             [(mapv (comp first read) asks-of-each-word)
              (mapv (comp second read) asks-of-each-word)
              (vec (sort (map (comp first :path)
                              (filter #(sources/rights-kind live-q %) (held-in live-q :base)))))]))
    (check "two reads with one home have equal sources for the base, and two askers with one home share every push on the open base while keeping their own holder and permission watches in a closed lane"
           [true true 4 true 4]
           (let [place {:asks [:place :note-41 :text]}
                 short (held-in (q (merge alice place {:lanes [:alice-notes :base]})) :base)
                 long (held-in (q (merge alice place {:lanes [:alice-notes :team :base]})) :base)
                 from (held-in (q (merge alice place {:from-lane :alice-notes})) :base)
                 hers-base (held-in (q (merge alice place {:lanes [:team :base]})) :base)
                 his-base (held-in (q (merge place {:asker (person :bob) :lanes [:team :base]})) :base)
                 hers-team (held-in (q (merge alice place {:lanes [:team :base]})) :team)
                 his-team (held-in (q (merge place {:asker (person :bob) :lanes [:team :base]})) :team)]
             [(= short long) (= long from) (count long) (= hers-base his-base)
              (count (filter his-team hers-team))]))
    (check "two reads with two homes share no source of the base, though each reads the same rows of it; the sources of the team, a streaming lane, are shared"
           [#{} true true]
           (let [place {:asks [:place :note-41 :text]}
                 through-notes (q (merge alice place {:lanes [:alice-notes :team :base]}))
                 through-team (q (merge alice place {:lanes [:team :base]}))]
             [(set (filter (read-in through-notes :base) (read-in through-team :base)))
              (= (set (map :path (read-in through-notes :base))) (set (map :path (read-in through-team :base))))
              (= (read-in through-notes :team) (read-in through-team :team))]))
    (check "two askers of one thing who ask for different parts have equal sources"
           true
           (let [one (q {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:team :base] :with #{:writer}})
                 two (q {:asks [:place :note-41 :text] :asker (person :alice) :lanes [:team :base] :stores-own-facts true})]
             (= (:wanted (sources/reading one {} :once numbers)) (:wanted (sources/reading two {} :once numbers)))))
    (check "which sources are a lane's rights, and which a check, in a streaming lane and in the base under a home"
           [:own-rows :holders :permission nil true false false :own-rows :holders :permission nil true false false]
           (let [points (q (merge alice {:asks [:points-at :parent :canvas-1] :lanes [:team :base]}))
                 team-holders (apply sources/holders-source :team :team (rows/by-holder-run :team (person :alice) nil))
                 team-perm (sources/permission-row-source :team :team (chain/place :team alice-team-id))
                 base-holders (apply sources/holders-source :team :base (rows/by-holder-run :base :anyone nil))
                 base-perm (sources/permission-row-source :team :base (chain/place :base alice-base-id))]
             [(sources/rights-kind points (sources/own-rows-source :team :team))
              (sources/rights-kind points team-holders)
              (sources/rights-kind points team-perm)
              (sources/rights-kind points (sources/place-source :team :team :block-1 :parent))
              (sources/check? points (sources/check-source :team :team :block-1 :parent))
              (sources/check? points team-holders)
              (sources/check? points team-perm)
              (sources/rights-kind points (sources/own-rows-source :team :base))
              (sources/rights-kind points base-holders)
              (sources/rights-kind points base-perm)
              (sources/rights-kind points (sources/place-source :team :base :block-1 :parent))
              (sources/check? points (sources/check-source :team :base :block-1 :parent))
              (sources/check? points base-holders)
              (sources/check? points base-perm)]))
    (check "a source is a map of five parts and no other, in a streaming lane and in the base"
           [#{:gate :table :lane :routing-id :path}]
           (vec (distinct (map (comp set keys)
                               (mapcat #(keys (:known (play-once traces (q (merge alice {:asks % :lanes [:alice-notes :team :base]})))))
                                       asks-of-each-word)))))
    (shown "the sources of each word in a streaming lane and in the base hold the table, the routing id and the path; open lanes share :anyone watches across every viewer with the same home")
    (shown "two reads with one home have equal sources for the base, so they share each push on it; two reads with two homes share none of them, and read the same rows; a streaming lane's sources are shared whatever the homes")
    (shown "two askers of one thing who ask for different parts of a row have equal sources, so they share each push; a check of a nearer lane at a place is the source of that place")))

(deftest the-loop-ends
  (let [place-3 (q (merge alice {:asks [:place :note-41 :text] :lanes [:alice-notes :team :base]}))
        place-1 (q (merge alice {:asks [:place :note-41 :text] :lanes [:team]}))
        place-from (q (merge alice {:asks [:place :note-41 :text] :from-lane :alice-notes}))
        entity-base (q (merge alice {:asks [:entity :note-41] :lanes [:base]}))
        run-base (q (merge alice {:asks [:run [:block-1] [:block-9]] :lanes [:base]}))
        points-3 (q (merge alice {:asks [:points-at :parent :canvas-1] :lanes [:alice-notes :team :base]}))
        points-from (q (merge alice {:asks [:points-at :parent :canvas-1] :from-lane :alice-notes}))
        once (fn [query] (let [{:keys [rounds calls rights-calls]} (play-once traces query)] [rounds calls rights-calls]))
        fact-source? (fn [query source] (and (nil? (sources/rights-kind query source)) (= :facts (:table source))))]
    (check "asked once: a place in one lane is one select call and then one rights query; through three lanes three of each; from a lane one and then two"
           [[1 [1] [1]] [1 [3] [3]] [2 [1 2] [1 2]]]
           (mapv once [place-1 place-3 place-from]))
    (check "asked once: an entity of the base is one select call, for its own rows and the run, followed by one rights query; a run across entities and a whole lane are the same"
           [[1 [1] [1]] [1 [1] [1]] [1 [1] [1]]]
           (mapv once [entity-base run-base (q (merge alice {:asks [:run :first :last] :lanes [:base]}))]))
    (check "asked once through her own lane: the base is one select call and one rights query more, both under her lane's routing id, for a place, an entity, a run across entities and a whole lane"
           [[1 [2] [2]] [1 [2] [2]] [1 [2] [2]] [1 [2] [2]]]
           (mapv #(once (q (merge alice {:asks % :lanes [:team :base]})))
                 [[:place :note-41 :text] [:entity :note-41] [:run [:block-1] [:block-9]] [:run :first :last]]))
    (check "asked once: what points at a thing through three lanes is three select rounds, own rows, orders, checks, with lane-rights asked after each lane's last select; from a lane four select rounds"
           [[3 [3 2 2] [3 2 2]] 4]
           [(once points-3) (first (once points-from))])
    (check "kept live: a place opens in four rounds, own rows, lane-rights, read-set watches, fact; what points at a thing through a given list in five, and from a start lane in eight; none exceeds eight"
           [4 4 5 8 true]
           [(first (live-rounds (q (merge alice {:asks [:place :note-41 :text] :lanes [:base]}))))
            (first (live-rounds place-3))
            (first (live-rounds points-3))
            (first (live-rounds points-from))
            (every? #(<= (first (live-rounds %)) 8) [place-1 place-3 place-from entity-base run-base points-3 points-from])])
    (check "the worked case of the plan: a place kept live through three lanes, two closed and the open base, holds sixteen pushes, four of them shared on the base"
           16 (second (live-rounds place-3)))
    (check "a read asked once wants the place in its first select round, before lane-rights; a live read does not"
           [3 0]
           [(count (filter #(fact-source? place-3 %) (:first-missing (play-once traces place-3))))
            (count (filter #(fact-source? place-3 %) (:first-joined (open-live traces [] place-3))))])
    (check "with the one predicate of that choice giving false, a read asked once reads own rows and rights before the place, and gives the same answer"
           [true [2 [3 3] [3 3]]]
           (let [as-built (:answer (play-once traces place-3))]
             (with-redefs [sources/before-standing? (constantly false)]
               [(= as-built (:answer (play-once traces place-3)))
                (once place-3)])))
    (check "the calls of one round: a lane's own rows and its fact are one select call, in the base under the home's routing id"
           [[:microbatch :facts :base 2]]
           (mapv (juxt :gate :table :routing-id (comp count :sources))
                 (sources/calls-for-once (:missing (sources/reading entity-base {} :once numbers)) numbers)))
    (check "the calls of one round through her own lane: one select call for each of the two gates' facts tables, both under her lane's routing id, each with the lane's own rows and the fact"
           #{[:streaming :facts :team 2] [:microbatch :facts :team 2]}
           (set (map (juxt :gate :table :routing-id (comp count :sources))
                     (sources/calls-for-once
                      (:missing (sources/reading (q (merge alice {:asks [:entity :note-41] :lanes [:team :base]})) {} :once numbers))
                      numbers))))
    (check "each call holds sources of one table under one routing id, and each source is in one call: every source of what points at a thing through three lanes, the base among them"
           [true true]
           (let [all (:wanted (:reading (play-once traces points-3)))
                 calls (sources/calls-for-once all numbers)]
             [(every? (fn [{:keys [gate table routing-id sources]}]
                        (every? #(= [gate table routing-id] ((juxt :gate :table :routing-id) %)) sources))
                      calls)
              (= (sort-by pr-str all) (sort-by pr-str (mapcat :sources calls)))]))
    (check "the calls of one round are cut at the greatest count of paths of one call"
           [2 2 1]
           (mapv (comp count :sources)
                 (sources/calls-for-once (map #(sources/place-source :team :team (keyword (str "e" %)) :k) (range 5))
                                         (assoc numbers :max-paths-in-one-call 2))))
    (shown "asked once, each lane's own rows and fact go in one select call per gate under the home's routing id, and lane-rights runs after the lane's last fact select returns")
    (shown "the loop ends: asked once, a place is 1 select round, from a lane 2, what points at a thing 3, from a lane 4; kept live, 4, 5 and 8 rounds")
    (shown "a read asked once wants the place before lane-rights, a live read only once standing is yes; with before-standing? giving false the read asked once gives the same answer")))

(deftest the-rounds-of-a-read-are-the-chains-of-its-sources
  (let [rounds-once (fn [tables query] (:rounds (play-once tables query)))
        rounds-live (fn [tables query] (:rounds (open-live tables [] query)))
        asking (fn [parts] (q (merge {:asker (person :ann) :session :run-7} parts)))
        place-given (asking {:asks [:place :b1 :parent] :lanes [:start :mid :far]})
        place-from (asking {:asks [:place :b1 :parent] :from-lane :start})
        points-given (asking {:asks [:points-at :parent :canvas-1] :lanes [:start :mid :far]})
        points-from (asking {:asks [:points-at :parent :canvas-1] :from-lane :start})
        writes [[:list :start [:mid :far :extra] 30] [:end :start (person :bea) 30] [:end-run :mid :run-7 (person :ann) 30]
                [:open :far 30] [:end :far (person :ann) 30] [:fact :far (person :maker) :b4 :parent :canvas-1 30]]
        runs (reduce write {}
                     (concat
                      (for [l [:solo :given]
                            w [[:make l 1] [:permission l (person :root) 3]
                               [:give l (person :bea) (person :root) 4] [:give l (person :cal) (person :root) 5]
                               [:fact l (person :maker) :p :text "p0" 10]]]
                        w)
                      [[:give-run :solo :run-7 (person :ann) (person :bea) 11]
                       [:give-run :solo :run-8 (person :dov) (person :bea) 12]
                       [:give :given (person :ann) (person :bea) 11]
                       [:give-run :given :run-7 (person :ann) (person :cal) 12]]))
        in (fn [lane & session] (q (cond-> {:asks [:place :p :text] :asker (person :ann) :lanes [lane]}
                                     (seq session) (assoc :session (first session)))))]
    (check "asked once, where every permission of the asker's is lent: lane-rights climbs the chain in one turn on the task, so a place settles in 1 select round (2 from a lane) and what points at a thing in 3 (4 from a lane)"
           [1 2 3 4]
           (mapv #(rounds-once chains %) [place-given place-from points-given points-from]))
    (check "kept live, the same reads open in 4, 7, 5 and 8 rounds: lane-rights returns every climbed row in :read-set at once, so all watches of a lane open in one round whatever the chain's depth"
           [4 7 5 8]
           (mapv #(rounds-live chains %) [place-given place-from points-given points-from]))
    (check "asked once, a write before any round of the longest read lengthens nothing: each source is fetched once, so the read settles within 6 select rounds whatever is written"
           true
           (every? (fn [[w r]] (<= (play-once-while-written chains points-from r w) 6))
                   (for [w writes r (range 5)] [w r])))
    (check "kept live, a list that gains a lane at the round at which the longest read would open asks 5 rounds more, own rows, lane-rights, watches, order and checks of that lane: 13, and ending the start lane's lender there asks 15, within the 24 that a live read is given; none of the writes asks more"
           [13 15 true]
           [(open-live-while-written chains points-from 8 [:list :start [:mid :far :extra] 30])
            (open-live-while-written chains points-from 8 [:end :start (person :bea) 30])
            (every? (fn [[w r]] (<= (open-live-while-written chains points-from r w) 15))
                    (for [w writes r (range 9)] [w r]))])
    (check "in a sitting where another holder holds a permission, lane-rights never reads that holder's run or chain; in her own sitting it climbs her sitting permission in the same single turn"
           [[1 [1] [1] :may-not-see]
            [1 [1] [1] :yes]
            [1 [1] [1] :may-not-see]
            false]
           (let [r-none (play-once runs (in :solo))
                 r-7 (play-once runs (in :solo :run-7))
                 r-8 (play-once runs (in :solo :run-8))
                 dov-run [:holders ":solo :person.dov :run-8 " ":solo :person.dov :run-8!"]]
             [[(:rounds r-none) (:calls r-none) (:rights-calls r-none) (get-in r-none [:reading :standing :solo])]
              [(:rounds r-7) (:calls r-7) (:rights-calls r-7) (get-in r-7 [:reading :standing :solo])]
              [(:rounds r-8) (:calls r-8) (:rights-calls r-8) (get-in r-8 [:reading :standing :solo])]
              (contains? (get-in r-8 [:snapshots :solo :read-set]) dov-run)]))
    (check "when her any-sitting permission already stands and allows seeing, lane-rights stops there without scanning her sitting run; when it does not, her sitting run is scanned in the same turn"
           [[false :yes] [true :yes]]
           (let [snap-standing (run-rights runs :given :see (person :ann) :run-7)
                 ended-any (write runs [:end :given (person :ann) 20])
                 snap-sitting (run-rights ended-any :given :see (person :ann) :run-7)
                 sit-run (into [:holders] (rows/by-holder-run :given (person :ann) :run-7))]
             [[(contains? (:read-set snap-standing) sit-run) (:standing snap-standing)]
              [(contains? (:read-set snap-sitting) sit-run) (:standing snap-sitting)]]))
    (shown "the rounds of a read do not grow with permission chain depth: asked once, at most 4 select rounds; kept live, at most 8 on unchanging tables, and at most 15 of 24 under any single write while opening")))

(deftest the-checks-of-nearer-lanes
  (let [query (q (merge alice {:asks [:points-at :parent :canvas-1] :lanes [:alice-notes :team :base]}))
        once (play-once traces query)
        r (:reading once)
        once-known (:known once)
        once-snaps (:snapshots once)
        live-w (open-live traces [] query)
        live-known (known-of live-w)
        live-snaps (snapshots-of live-w)
        checks (fn [reading lanes]
                 (into {} (map (fn [lane] [lane (set (keys (get-in reading [:by-lane lane :checks])))])) lanes))
        between (reduce write {} [[:make :l1 1] [:make :l2 1] [:make :l3 1]
                                  [:open :l1 2] [:open :l2 2] [:open :l3 2]
                                  [:marks :l1 #{:parent} 3] [:marks :l3 #{:parent} 3]
                                  [:fact :l1 (person :maker) :p :parent :canvas-1 11] [:fact :l3 (person :maker) :p :parent :canvas-1 12]
                                  [:fact :l3 (person :maker) :q :parent :canvas-1 13]])
        between-r (:reading (play-once between (q {:asks [:points-at :parent :canvas-1] :asker (person :ann) :lanes [:l1 :l2 :l3]})))
        nearer (fn [v] (let [tables (reduce write {} [[:make :l1 1] [:make :l2 1]
                                                       [:open :l1 2] [:open :l2 2]
                                                       [:marks :l2 #{:parent} 3]
                                                       [:fact :l2 (person :maker) :p :parent :canvas-1 11]
                                                       [:fact :l1 (person :ann) :p :parent v 12]])]
                         (:answer (play-once tables (q {:asks [:points-at :parent :canvas-1] :asker (person :ann) :lanes [:l1 :l2]})))))
        row (fn [v] (fact-row* :l (person :maker) :e :k v 5))
        uuid (UUID. 3 4)]
    (check "a check is wanted in each lane nearer than the nearest order that holds the place, and in no other: the base's two places in both nearer lanes; the place of the team's own order in the nearest lane alone"
           {:alice-notes #{[:block-1 :parent] [:block-2 :parent] [:block-3 :parent]}
            :team #{[:block-1 :parent] [:block-2 :parent]}
            :base #{}}
           (checks r [:alice-notes :team :base]))
    (check "the base's block-1 is checked in the team too, where another thing was written: it is hidden; block-2 comes from the base, block-3 from the team's own order"
           [[[:block-2 :parent] :base] [[:block-3 :parent] :team]]
           (sort (map (fn [[place a]] [place (:lane a)]) (:places (:answer once)))))
    (check "a place that two orders hold, with a lane between them: no check in the lane between, since the nearer order wins before it is reached; the farther order's own place is checked in both nearer lanes"
           {:l1 #{[:q :parent]} :l2 #{[:q :parent]} :l3 #{}}
           (checks between-r [:l1 :l2 :l3]))
    (check "with the one function of the checks put aside, the runs are joined: no check is wanted, and the place that a nearer lane moved is shown from the farther order"
           [{} #{[:block-1 :parent] [:block-2 :parent] [:block-3 :parent]}]
           (with-redefs [sources/nearer-checks (constantly {})]
             (let [joined (play-once traces query)]
               [(into {} (mapcat :checks) (vals (:by-lane (:reading joined))))
                (set (keys (:places (:answer joined))))])))
    (check "the checks of one read are bounded, by one number when it is asked once and by another when it is kept live: past its own number the read wants none of them and says that it is too large, with that number; the other number does not bound it"
           [[{:what :checks-of-one-read :most 2} true] [{:what :checks-of-one-read :most 2} true] [nil false] [nil false]]
           (let [bounded (fn [mode nums]
                           (let [[kn sn] (if (= :once mode) [once-known once-snaps] [live-known live-snaps])
                                 r (sources/reading query kn mode nums nil sn)]
                             [(:too-large r) (every? nil? (map :checks (vals (:by-lane r))))]))]
             [(bounded :once (assoc numbers :max-checks-of-one-read 2))
              (bounded :live (assoc numbers :max-checks-of-one-live-read 2))
              (bounded :once (assoc numbers :max-checks-of-one-live-read 2))
              (bounded :live (assoc numbers :max-checks-of-one-read 2))]))
    (check "how many checks one read may need, by how it is asked, at the defaults"
           [40000 250] [(sources/most-checks :once numbers) (sources/most-checks :live numbers)])
    (check "a check's row that holds another thing hides the place; one that says nothing here hides it; one that holds the asked thing gives the place from the nearer lane's row"
           [{} {} [:l1 :canvas-1 12]]
           [(:places (nearer :canvas-2)) (:places (nearer nil))
            ((juxt :lane :value :stamp) (get (:places (nearer :canvas-1)) [:p :parent]))])
    (check "a row holds a thing exactly when its String is the thing's one text: a UUID, a string with spaces, a decimal with trailing zeros; not another thing, not a retract, not no row, not a String that is no value of the form, in a row that the proof writes by hand"
           [true true true false false false false]
           [(rows/holds-this-value? (row uuid) uuid)
            (rows/holds-this-value? (row "a thing with spaces") "a thing with spaces")
            (rows/holds-this-value? (row 1.50M) 1.5M)
            (rows/holds-this-value? (row uuid) (str uuid))
            (rows/holds-this-value? (row nil) :canvas-1)
            (rows/holds-this-value? nil :canvas-1)
            (rows/holds-this-value? {:value "(not a value" :stamp 1} :canvas-1)])
    (shown "the checks of nearer lanes: one in each lane nearer than the nearest order that holds a place, and in no other; none in a lane between two orders that both hold it; with the one function put aside the runs are joined; past the build's number none is wanted and the read is too large")
    (shown "the nearest row wins, and the place is given from it exactly when it holds the thing: a nearer row with another thing, or a retract, hides the place; a nearer row with the asked thing gives it, from the nearer lane")))

(deftest what-a-read-gives
  (let [yes-used (UUID. 0 88)
        tables (reduce write traces
                       [[:fact :alice-notes (person :alice) :note-41 :text nil 20]
                        [:fact :team (person :bob) :note-9 :text "only the team" 713]
                        [:give-act :team yes-used (person :bob) (person :alice) 714]
                        [:use-act :team yes-used (person :bob) 715]
                        [:fact :base :system :note-9 :size 9 330]])
        tables (put tables :microbatch :facts (rows/place-key :base :note-9 :raw)
                    {:value "(not a value" :by-act (UUID. 0 9) :by-index 0 :stamp 331 :time (time-of 331)
                     :who ":system" :permission (str (standing-perm-id :base :system))})
        bob-perm-id (standing-perm-id :team (person :bob))
        ask (fn [who asks lanes & more]
              (:answer (play-once tables (q (merge {:asks asks :lanes lanes} (if who {:asker who} {:not-signed-in true})
                                                   (apply hash-map more))))))
        place (fn [& args] (:place (apply ask args)))
        uuid (fn [stamp] (UUID. 0 (long stamp)))]
    (check "the four things that a place can hold, told apart: no fact; nothing here; a value as its maker gave it; a String that is no value of the form, in a row that the proof puts in the table by hand, since no write of a gate gives one"
           [{:holds :no-fact}
            {:holds :nothing-here :lane :alice-notes :fact [[:alice-notes :streaming (uuid 20)] 0] :stamp 20}
            {:holds :value :value "from the base" :lane :base :fact [[:base :microbatch (uuid 310)] 0] :stamp 310}
            {:holds :not-a-value :lane :base :fact [[:base :microbatch (uuid 9)] 0] :stamp 331}]
           [(place (person :alice) [:place :nobody :text] [:alice-notes :team :base])
            (place (person :alice) [:place :note-41 :text] [:alice-notes :team :base])
            (place (person :alice) [:place :note-41 :text] [:base])
            (place (person :alice) [:place :note-9 :raw] [:base])])
    (check "the nearest lane that holds a fact wins, also when that fact says nothing here; a place that only a farther lane holds comes from there"
           [[:nothing-here :alice-notes] [:value :team] [:value :base]]
           [((juxt :holds :lane) (place (person :alice) [:place :note-41 :text] [:alice-notes :team :base]))
            ((juxt :holds :lane) (place (person :alice) [:place :note-41 :text] [:team :base]))
            ((juxt :holds :lane) (place (person :alice) [:place :note-41 :title] [:alice-notes :team :base]))])
    (check "a run through lanes: each place from the nearest lane that holds it"
           {[:note-41 :text] :alice-notes [:note-41 :title] :base [:note-9 :text] :team [:note-9 :size] :base [:note-9 :raw] :base}
           (into {} (map (fn [[p a]] [p (:lane a)])) (:places (ask (person :alice) [:run [:note-41] [:note-9]] [:alice-notes :team :base]))))
    (check "a lane that she may not see, and one that is not made, are named with why, and nothing of them is given: the answer is that of the same query without them"
           [[{:lane :alice-notes :why :may-not-see} {:lane :nowhere :why :not-made}]
            [:alice-notes :team :nowhere :base]
            true]
           (let [with (ask (person :bob) [:place :note-41 :text] [:alice-notes :team :nowhere :team :base])
                 without (ask (person :bob) [:place :note-41 :text] [:team :base])]
             [(:left-out with) (:lanes with) (= (:place with) (:place without))]))
    (check "a start lane that she may not see: that one lane is left out, and its list is not used"
           {:lanes [:alice-notes] :left-out [{:lane :alice-notes :why :may-not-see}] :place {:holds :no-fact}}
           (:answer (play-once tables (q {:asks [:place :note-41 :text] :asker (person :bob) :from-lane :alice-notes}))))
    (check "a person who is not signed in sees the base, and no members lane"
           [[:value :base] [{:lane :team :why :may-not-see}]]
           (let [a (ask nil [:place :note-41 :text] [:team :base])] [((juxt :holds :lane) (:place a)) (:left-out a)]))
    (check "a place-answer holds no writer and no permission unless the query asked: neither, the writer, both"
           [#{:holds :value :lane :fact :stamp}
            [(person :bob) nil]
            [(person :bob) bob-perm-id]]
           [(set (keys (place (person :alice) [:place :note-9 :text] [:team])))
            ((juxt :writer :permission) (place (person :alice) [:place :note-9 :text] [:team] :with #{:writer}))
            ((juxt :writer :permission) (place (person :alice) [:place :note-9 :text] [:team] :with #{:writer :permission}))])
    (check "a place-answer holds the time on the act that wrote its fact only when the query asks for :time: not with the writer and the permission; with :time alone, the time and neither of the two; with all three, all three"
           [false
            [nil nil (time-of 713)]
            [(person :bob) bob-perm-id (time-of 713)]]
           [(contains? (place (person :alice) [:place :note-9 :text] [:team] :with #{:writer :permission}) :time)
            ((juxt :writer :permission :time) (place (person :alice) [:place :note-9 :text] [:team] :with #{:time}))
            ((juxt :writer :permission :time) (place (person :alice) [:place :note-9 :text] [:team] :with #{:writer :permission :time}))])
    (check "a run through three lanes that asks for :time: each place-answer holds the :time of the row that it came from, in the nearest lane that holds the place; the same run with no :with holds no time, and is the first with the time taken out"
           [{[:note-41 :text] (time-of 20) [:note-41 :title] (time-of 311) [:note-9 :text] (time-of 713)
             [:note-9 :size] (time-of 330) [:note-9 :raw] (time-of 331)}
            true true true]
           (let [with-time (:places (ask (person :alice) [:run [:note-41] [:note-9]] [:alice-notes :team :base] :with #{:time}))
                 without (:places (ask (person :alice) [:run [:note-41] [:note-9]] [:alice-notes :team :base]))
                 row-of (fn [[e k] {:keys [lane]}]
                          (get (table tables (first-act/depot-at-making lane) :facts) (rows/place-key lane e k)))]
             [(into {} (map (fn [[p a]] [p (:time a)])) with-time)
              (every? (fn [[p a]] (= (:time a) (:time (row-of p a)))) with-time)
              (not-any? (fn [[_ a]] (contains? a :time)) without)
              (= without (into {} (map (fn [[p a]] [p (dissoc a :time)])) with-time))]))
    (check "the store's own facts are left out of a run unless the query asks: a permission's own entity, the lane itself, and the use of a yes for one act which carries no fact id"
           [{} #{:softland.store/permission}
            {} #{:softland.store/depot :softland.store/marked-keys}
            {:holds :nothing-here :lane :team :stamp 715}]
           [(:places (ask (person :alice) [:entity bob-perm-id] [:team]))
            (set (map second (keys (:places (ask (person :alice) [:entity bob-perm-id] [:team] :stores-own-facts true)))))
            (:places (ask (person :alice) [:entity :team] [:team]))
            (set (map second (keys (:places (ask (person :alice) [:entity :team] [:team] :stores-own-facts true)))))
            (place (person :alice) [:place yes-used :softland.store/permission] [:team] :stores-own-facts true)])
    (check "a row under a store key does not count for a read that did not ask, and counts for one that did"
           [false true true]
           [(answer/counts? (q {:asks [:entity :e] :asker (person :a) :lanes [:l]}) :softland.store/permission)
            (answer/counts? (q {:asks [:entity :e] :asker (person :a) :lanes [:l] :stores-own-facts true}) :softland.store/permission)
            (answer/counts? (q {:asks [:entity :e] :asker (person :a) :lanes [:l]}) :text)])
    (check "a run across entities of the base, read through her own lane, gives each place of the base once, from its row, and the nearer lane's place where it holds one"
           [{[:block-1 :parent] [:team :canvas-2 711] [:block-2 :parent] [:base :canvas-1 321] [:block-3 :parent] [:team :canvas-1 710]
             [:note-41 :text] [:team "from the team" 712] [:note-41 :title] [:base "a title" 311]
             [:note-9 :text] [:team "only the team" 713] [:note-9 :size] [:base 9 330] [:note-9 :raw] [:base nil 331]}
            {[:block-1 :parent] [:base :canvas-1 320] [:block-2 :parent] [:base :canvas-1 321]
             [:note-41 :text] [:base "from the base" 310] [:note-41 :title] [:base "a title" 311]
             [:note-9 :size] [:base 9 330] [:note-9 :raw] [:base nil 331]}]
           (let [of-run (fn [lanes] (into {} (map (fn [[p a]] [p ((juxt :lane :value :stamp) a)]))
                                           (:places (ask (person :alice) [:run [:block-1] [:note-9 :text]] lanes))))]
             [(of-run [:team :base]) (of-run [:base])]))
    (check "the answer of a read of the base does not differ by its home: the same place, entity, run and what points at a thing, from the base itself and through a lane that is not made"
           [true true true true]
           (mapv (fn [asks]
                   (= (dissoc (ask (person :alice) asks [:base]) :lanes :left-out)
                      (dissoc (ask (person :alice) asks [:nowhere :base]) :lanes :left-out)))
                 [[:place :note-41 :text] [:entity :note-41] [:run :first :last] [:points-at :parent :canvas-1]]))
    (check "what points at a thing says, for each lane that she may see, since which stamp its order is level, or that it marks nothing"
           [{:lane :alice-notes :not-marked true} {:lane :team :level-since 700} {:lane :base :level-since 300}]
           (:orders (ask (person :alice) [:points-at :parent :canvas-1] [:alice-notes :team :base])))
    (shown "what a read gives: the four things that a place can hold are told apart; the nearest lane that holds a fact wins, also with nothing here; each place of a run comes from the nearest lane that holds it; a lane that is left out is named with why and nothing of it is given")
    (shown "a place-answer holds no writer, no permission and no time unless asked; the store's own facts are in no answer that did not ask for them; a run across entities of the base gives each place once, and a read of the base gives the same from the base itself and through another home")))

(deftest which-changes-reach-an-asker
  (let [tables (reduce write traces [[:fact :alice-notes (person :alice) :note-41 :text "from her notes" 20]])
        alice-team-id (standing-perm-id :team (person :alice))
        bob-team-id (standing-perm-id :team (person :bob))
        known-for (fn [query] ((juxt :reading :known) (play-once tables query)))
        alice-3 (q (merge alice {:asks [:place :note-41 :text] :lanes [:alice-notes :team :base]}))
        [r known] (known-for alice-3)
        bob-3 (q {:asks [:place :note-41 :text] :asker (person :bob) :lanes [:alice-notes :team :base]})
        [bob-r bob-known] (known-for bob-3)
        holder (q (merge alice {:asks [:entity bob-team-id] :lanes [:team]}))
        [holder-r holder-known] (known-for holder)
        asked (assoc holder :stores-own-facts true)
        [asked-r asked-known] (known-for asked)
        place [:note-41 :text]]
    (check "a change in the lane that wins the place is told; a change under a nearer row is not"
           [true false false]
           [(answer/told-by? alice-3 r known (sources/place-source :alice-notes :alice-notes :note-41 :text) place)
            (answer/told-by? alice-3 r known (sources/place-source :alice-notes :team :note-41 :text) place)
            (answer/told-by? alice-3 r known (sources/place-source :alice-notes :base :note-41 :text) place)])
    (check "a change of the base's row is told to an asker who reads it through her own lane, when no nearer lane holds the place; the source is one that her read holds"
           [true true false]
           (let [title [:note-41 :title]
                 query (q (merge alice {:asks [:place :note-41 :title] :lanes [:alice-notes :team :base]}))
                 [title-r title-known] (known-for query)
                 source (sources/place-source :alice-notes :base :note-41 :title)]
             [(answer/told-by? query title-r title-known source title)
              (contains? title-known source)
              (contains? title-known (sources/place-source :base :base :note-41 :title))]))
    (check "a change in a lane that she may not see is not told; for her the next lane wins, and its change is told"
           [false true]
           [(answer/told-by? bob-3 bob-r bob-known (sources/place-source :alice-notes :alice-notes :note-41 :text) place)
            (answer/told-by? bob-3 bob-r bob-known (sources/place-source :alice-notes :team :note-41 :text) place)])
    (check "a change of a store key's row is told only to an asker who asked for the store's own facts"
           [false true]
           [(answer/told-by? holder holder-r holder-known (first (get-in holder-r [:by-lane :team :facts])) [bob-team-id :softland.store/permission])
            (answer/told-by? asked asked-r asked-known (first (get-in asked-r [:by-lane :team :facts])) [bob-team-id :softland.store/permission])])
    (check "the places that a change names for a query: the asked place from its row and from no other; each key of a run; no place from a lane's own rows"
           [#{[:note-41 :text]} #{} #{[bob-team-id :softland.store/permission]} #{}]
           [(sources/places-of alice-3 (sources/place-source :alice-notes :team :note-41 :text) [":team :note-41 :text"])
            (sources/places-of alice-3 (sources/permission-row-source :alice-notes :team (chain/place :team alice-team-id))
                               [(chain/place :team alice-team-id)])
            (sources/places-of holder (first (get-in holder-r [:by-lane :team :facts]))
                               [(chain/place :team bob-team-id) "no key"])
            (sources/places-of holder (sources/own-rows-source :team :team) [":team :team :softland.store/depot"])])
    (check "what the base's rights watches mean at a change, for a live read through her own lane: matching :read-set keeps her standing, and a stamp mismatch turns the meaning to :not-known-yet"
           [[{:made? true :sees? true} {:made? true :sees? true}]
            [:not-known-yet {:made? true :sees? true}]]
           (let [t (reduce write {} [[:make :mine 1] [:make :base 1] [:open :mine 2]
                                     [:permission :base (person :ann) 3] [:fact :base (person :maker) :p :text "b0" 11]])
                 query (q {:asks [:place :p :text] :asker (person :ann) :lanes [:mine :base]})
                 w (open-live t [] query)
                 known (known-of w)
                 snapshots (snapshots-of w)
                 own (sources/own-rows-source :mine :base)
                 perm-src (sources/permission-row-source :mine :base (chain/place :base (standing-perm-id :base (person :ann))))]
             [(answer/meanings query snapshots known own (get known own) (get known own))
              (answer/meanings query snapshots known perm-src nil (get known perm-src))]))
    (check "what a climbed lender's row means at a change: ending the lender invalidates :read-set to :not-known-yet, while another holder's permission row outside :read-set leaves meaning unchanged"
           [[{:made? true :sees? true} :not-known-yet] true]
           (let [bea-id (uuid-of [:bea-lab])
                 dov-id (uuid-of [:dov-lab])
                 t (reduce write {} [[:make :lab 1] [:permission :lab (person :root) 2]
                                     [:give :lab (person :bea) (person :root) 3 {:id bea-id}]
                                     [:give :lab (person :cal) (person :bea) 4]
                                     [:give :lab (person :dov) (person :root) 5 {:id dov-id}]
                                     [:fact :lab (person :maker) :p :text "l0" 11]])
                 query (q {:asks [:place :p :text] :asker (person :cal) :lanes [:lab]})
                 w (open-live t [] query)
                 known (known-of w)
                 snapshots (snapshots-of w)
                 above (sources/permission-row-source :lab :lab (chain/place :lab bea-id))
                 beside (sources/permission-row-source :lab :lab (chain/place :lab dov-id))
                 ended (table-value (write t [:end :lab (person :bea) 20]) above)
                 beside-ended (table-value (write t [:end :lab (person :dov) 20]) beside)]
             [(answer/meanings query snapshots known above (get known above) ended)
              (apply = (answer/meanings query snapshots known beside (table-value t beside) beside-ended))]))
    (check "the places that a change of the base names, for a read through her own lane: the asked place from its row; each key of a run across entities; the entity of each key of the order by value"
           [#{[:note-41 :text]} #{[:block-1 :parent] [:note-41 :title]} #{[:block-2 :parent]}]
           (let [digest (rows/value-digest-of :canvas-1)
                 run (q (merge alice {:asks [:run :first :last] :lanes [:team :base]}))
                 points (q (merge alice {:asks [:points-at :parent :canvas-1] :lanes [:team :base]}))]
             [(sources/places-of alice-3 (sources/place-source :alice-notes :base :note-41 :text) [":base :note-41 :text"])
              (sources/places-of run (sources/run-source :team :base ":base " ":base!") [":base :block-1 :parent" ":base :note-41 :title"])
              (sources/places-of points (sources/by-value-source :team :base :parent 300 :canvas-1)
                                 [(rows/by-value-key :base :parent 300 digest :block-2)])]))
    (shown "which changes reach an asker: a change in the lane that wins the place; not one under a nearer row, not one in a lane that she may not see, and one of a store key's row only when she asked for the store's own facts")))

(deftest an-asker-who-may-not-see-a-lane-is-told-nothing-of-it
  (let [now (every-small-unseen)
        alone (fn [writes] (every-order (unseen-world writes) nil))
        joined (fn [world] (set (map :path (filter #(= :near (:lane %)) (:held world)))))
        dov-id (uuid-of [:dov-sit])
        bea-id (uuid-of [:bea-sit])
        given (open-live (reduce write {}
                                 [[:make :near 1] [:make :far 1] [:open :far 2]
                                  [:permission :near (person :root) 2]
                                  [:give :near (person :bea) (person :root) 3 {:id bea-id}]
                                  [:fact :near (person :maker) :p :text "n0" 10]
                                  [:fact :far (person :maker) :p :text "f0" 11]])
                         [[:give-run :near :run-7 (person :dov) (person :bea) 100 {:id dov-id}]]
                         (q {:asks [:place :p :text] :asker (person :ann) :session :run-7 :lanes [:near :far]}))
        after-give (-> given (move [:write] nil))]
    (check "a live read in a sitting, of a lane that she may not see, is handed nothing in any order of up to three writes there: another holder's sitting permission, its end, the end of the one above it and its new give, a fact; and each order ends on the screen that a read asked once gives"
           [155 0 []]
           [(:lists now) (:tells now) (:wrong now)])
    (check "her read holds the lane's own rows and the six holder runs of everyone and herself, and never another holder's sitting run or permission rows"
           [#{[:own-rows ":near :near :softland.store/" ":near :near :softland.store0"]
              [:holders ":near :anyone * " ":near :anyone *!"]
              [:holders ":near :anyone :run-7 " ":near :anyone :run-7!"]
              [:holders ":near :anyone-signed-in * " ":near :anyone-signed-in *!"]
              [:holders ":near :anyone-signed-in :run-7 " ":near :anyone-signed-in :run-7!"]
              [:holders ":near :person.ann * " ":near :person.ann *!"]
              [:holders ":near :person.ann :run-7 " ":near :person.ann :run-7!"]}
            []
            false
            false]
           [(joined after-give)
            (vec (moves-of after-give))
            (contains? (joined after-give) [:row (chain/place :near dov-id)])
            (contains? (joined after-give) [:row (chain/place :near bea-id)])])
    (check "another holder's sitting permission in permissions-by-holder produces zero push callbacks on her read, even without unseen-lanes"
           0
           (:tells (with-redefs [softland.store.read-path.answer/unseen-lanes (constantly #{})]
                     (every-small-unseen))))
    (check "one order of the review's history alone: the sitting give to another holder and its end hand her nothing"
           nil
           (:wrong (alone [[:give-run-to-another] [:end-run]])))
    (shown "an asker who may not see a lane is told nothing of it while she asks in a sitting: in" (:lists now) "lists of up to three writes," (:states now) "worlds, no step handed her anything, and another holder's sitting run is never watched")))

(deftest an-asker-whose-own-permission-does-not-stand-is-told-nothing
  (let [now (every-small-own-unseen)
        without-the-rule (with-redefs [softland.store.read-path.answer/unseen-lanes (constantly #{})]
                           (every-small-own-unseen))
        ;; Without `mark-of` remembering what she was told of each lane, an in-place lowering
        ;; whose two rights pushes are both taken before either is handled leaves `:near`
        ;; recorded as `:not-known-yet`, and a second write-only change then leaks an `:anew` tell.
        leak-without-memory
        (let [ann-id (standing-perm-id :near (person :ann))
              tables (reduce write {}
                             [[:make :near 1] [:make :far 1] [:open :far 2]
                              [:permission :near (person :ann) 2]
                              [:change :near ann-id {:write :all} 3]
                              [:fact :far (person :maker) :p :text "f0" 10]])
              tells (atom 0)]
          (with-redefs [answer/mark-of (constantly false)
                        told (let [told* told]
                               (fn [world tell] (when tell (swap! tells inc)) (told* world tell)))]
            (every-order (open-live tables
                                    [[:change :near ann-id {:write :all :give #{:write}} 100]]
                                    (q {:asks [:place :p :text] :asker (person :ann) :lanes [:near :far]}))
                         nil))
          @tells)
        one (fn [writes] (let [tells (atom 0)
                               played (with-redefs [told (let [told* told]
                                                           (fn [world tell] (when tell (swap! tells inc)) (told* world tell)))]
                                        (every-order (own-unseen-world writes) nil))]
                           [@tells (:wrong played)]))]
    (check "a live read in a sitting, of a lane that she may not see, where write-only permissions are given to her and her sitting and ended: in every order of every list of up to three such writes and a fact, no step hands her anything, and each order ends on what a read asked once gives"
           [155 0 []] [(:lists now) (:tells now) (:wrong now)])
    (check "a write-only give to her and its end by the first holder, alone, hand her nothing"
           [[0 nil] [0 nil]] [(one [[:bea-write-only]]) (one [[:bea-write-only] [:root-ends-hers]])])
    (check "the player can fail: without unseen-lanes, joining and leaving write-only permission watches in a lane she may not see hands her answers; and without mark-of remembering her standing, two rights pushes taken before either is handled leak an answer on an unseen lane"
           [true true] [(pos? (:tells without-the-rule)) (pos? leak-without-memory)])
    (shown "an asker whose permissions in a lane never allow seeing is told nothing of the lane: in" (:lists now) "lists of up to three writes," (:states now) "worlds, no step handed her anything; without unseen-lanes or mark-of, steps did")))

(deftest a-row-above-hers-is-read-after-her-own
  (let [unseen [[:bea-write-only] [:bea-run-write-only] [:bea-again] [:near]]
        now (every-small-stale-chain unseen)
        with-a-give (every-small-stale-chain [[:bea-gives] [:bea-write-only] [:near]])
        ;; One-act transfer (`BUILD.md` Batch 4 line 4): `old-id` ended and `new-id` granted in one act.
        old-id (standing-perm-id :near (person :ann))
        new-id (uuid-of [:ann-swapped])
        swap-tables (reduce write {}
                            [[:make :near 1] [:make :far 1] [:open :far 2]
                             [:permission :near (person :ann) 2]
                             [:fact :near (person :maker) :p :text "n0" 10]
                             [:fact :far (person :maker) :p :text "f0" 11]])
         run-swap (fn []
                    (let [between (atom #{})]
                      (with-redefs [told (let [told* told]
                                           (fn [world tell]
                                             (let [w (told* world tell)]
                                               (when tell
                                                 (swap! between conj
                                                        [(get-in w [:screen :place :lane])
                                                         (get-in w [:screen :left-out])]))
                                               w)))]
                        (let [res (every-order
                                   (open-live swap-tables
                                              [[:swap :near (person :ann) old-id new-id {:see true} 100]]
                                              (q {:asks [:place :p :text] :asker (person :ann) :lanes [:near :far]}))
                                   nil)]
                          [@between (:wrong res)]))))
        [swap-seen swap-wrong] (run-swap)
        ;; Without `:read-set` stamp validation (falling back to `:may-not-see` instead of `:not-known-yet`
        ;; when open pushes disagree with `:read-set`), a 1-act swap exposes `:far` in between!
        [unvalidated-seen _] (with-redefs [sources/lane-standing
                                           (fn [home lane mode snapshots known]
                                             (let [own (sources/own-rows-source home lane)
                                                   snap (get snapshots lane)]
                                               (cond
                                                 (not (contains? known own)) :not-known-yet
                                                 (not (lanes/made? (get known own) lane)) :not-made
                                                 (nil? snap) :not-known-yet
                                                 (= :once mode) (:standing snap)
                                                 (sources/snapshot-matches? home lane snap known) (:standing snap)
                                                 :else :may-not-see)))]
                               (run-swap))
        root-id (standing-perm-id :lab (person :root))
        bea-id (uuid-of [:bea-dedup])
        ann-id (uuid-of [:ann-dedup])
        ann-sit-id (uuid-of [:ann-sit-dedup])
        tables (reduce write {} [[:make :lab 1] [:permission :lab (person :root) 2]
                                 [:give :lab (person :bea) (person :root) 3 {:id bea-id}]
                                 [:give :lab (person :ann) (person :bea) 4 {:id ann-id :allows {:write :all}}]
                                 [:give-run :lab :run-7 (person :ann) (person :bea) 5 {:id ann-sit-id :allows {:see true}}]])
        snap (run-rights tables :lab :see (person :ann) :run-7)]
    (check "an asker who holds a live read open on a lane after an ancestor's end, while new write-only gives, giver grants and facts arrive, is handed nothing in every list of up to three writes, and each order ends on what a read asked once gives"
           [84 0 #{} []] [(:lists now) (:tells now) (:given now) (:wrong now)])
    (check "a permission transfer in one act never shows a live viewer a state in between: every callback order keeps the nearer lane throughout, whereas treating a mismatched :read-set as :may-not-see instead of :not-known-yet exposes the farther lane in between"
           [[#{[:near []]} nil] true]
           [[swap-seen swap-wrong] (contains? unvalidated-seen [:far [{:lane :near :why :may-not-see}]])])
    (check "a give that stands is told: with Bea's give to her beneath Bea's permission among the writes, every order ends on what a read asked once gives"
           [39 []] [(:lists with-a-give) (:wrong with-a-give)])
    (check "lane-rights records the exact stamp of every climbed permission row and holder run in :read-set, climbing shared ancestors in one turn"
           [:yes 1 2 3 4 5]
           [(:standing snap)
            (get (:read-set snap) [:fact (rows/place-key :lab :lab :softland.store/depot)])
            (get (:read-set snap) [:fact (chain/place :lab root-id)])
            (get (:read-set snap) [:fact (chain/place :lab bea-id)])
            (get (:read-set snap) [:fact (chain/place :lab ann-id)])
            (get (:read-set snap) [:fact (chain/place :lab ann-sit-id)])])
    (shown "read-set stamp validation: in" (:lists now) "unseen lists no step handed her anything, and across a 1-act permission swap zero callback orders showed an intermediate state")))

(deftest a-yes-for-one-act-lets-nobody-see
  (let [yes (UUID. 0 77)
        tables (reduce write {} [[:make :near 1] [:permission :near (person :root) 2] [:give :near (person :bea) (person :root) 3]
                                 [:give-act :near yes (person :ann) (person :bea) 4] [:fact :near (person :maker) :p :text "n0" 10]])
        asks [{:asks [:place :p :text] :asker (person :ann) :lanes [:near]}
              {:asks [:place :p :text] :asker (person :ann) :session yes :lanes [:near]}]
        left-out {:lanes [:near] :left-out [{:lane :near :why :may-not-see}] :place {:holds :no-fact}}
        yes-row (get (table tables :streaming :facts) (chain/place :near yes))]
    (check "the yes is where the gate keeps it: on its own id under the permission key, out of permissions-by-holder, covering one exact act for the asker"
           [(person :ann) :exact-act {:write :all} {} false]
           (let [held (rows/held-permission yes-row)
                 by-holder (table tables :streaming :permissions-by-holder)]
             [(:holder held) (first (:covers held)) (:allows held)
              (into {} (subseq by-holder >= ":near :person.ann " < ":near :person.ann!"))
              (boolean (some #(= yes (rows/permission-of-by-holder-key %)) (keys by-holder)))]))
    (check "a holder whose one permission in a lane that is open to its members is a yes for one act may not see it: asked once and kept live, in no run and in a run whose id is the yes's own"
           [left-out left-out left-out left-out]
           (into (mapv #(:answer (play-once tables (q %))) asks)
                 (mapv #(:screen (open-live tables [] (q %))) asks)))
    (check "neither a read asked once nor a live read watches the row of a permission for one exact act"
           [false false]
           (let [yes-src (sources/permission-row-source :near :near (chain/place :near yes))]
             [(contains? (:wanted (:reading (play-once tables (q (second asks))))) yes-src)
              (contains? (:held (open-live tables [] (q (second asks)))) yes-src)]))
    (shown "a yes for one act lets nobody see (R8): its holder, asking once or live, in no run or in a run whose id is the yes's, is left out with :may-not-see")))

(deftest the-reviews-probe-of-the-read-path-written-again
  (let [rounds-live (fn [tables query] (:rounds (open-live tables [] query)))
        once (fn [tables query] (let [{:keys [rounds calls rights-calls]} (play-once tables query)] [rounds calls rights-calls]))
        probe-3 (reduce write {}
                        [[:make :start 1] [:permission :start (person :root) 2] [:list :start [:mid :far] 3]
                         [:give-run :start :run-7 (person :cal) (person :root) 4]
                         [:make :mid 1] [:permission :mid (person :root) 2] [:give-run :mid :run-7 (person :cal) (person :root) 3]
                         [:make :far 1] [:open :far 2] [:marks :far #{:parent} 3]
                         [:fact :far (person :root) :item-1 :parent :target 4]])
        probe-6b (reduce write {}
                         [[:make :start 1] [:permission :start (person :root) 2] [:list :start [:mid :far] 3]
                          [:give-run :start :run-7 (person :cal) (person :root) 4]
                          [:make :mid 1] [:permission :mid (person :root) 2] [:give-run :mid :run-7 (person :cal) (person :root) 3]
                          [:make :far 1] [:permission :far (person :root) 2] [:marks :far #{:parent} 3]
                          [:give-run :far :run-7 (person :cal) (person :root) 4]
                          [:fact :far (person :root) :item-1 :parent :canvas-1 10]])
        in-run-7 (fn [thing] (q {:asks [:points-at :parent thing] :asker (person :cal) :session :run-7 :from-lane :start}))
        probe-4 (reduce write {}
                        [[:make :near 1] [:permission :near (person :root) 2] [:give :near (person :bea) (person :root) 3]
                         [:fact :near (person :root) :p :text "secret" 4]
                         [:make :far 1] [:open :far 2] [:fact :far (person :root) :p :text "public" 3]])
        tells (atom 0)
        probe-4-played (with-redefs [told (let [told* told]
                                            (fn [world tell] (when tell (swap! tells inc)) (told* world tell)))]
                         (every-order (open-live probe-4 [[:give-run :near :run-7 (person :dov) (person :bea) 100]
                                                          [:end-run :near :run-7 (person :dov) 101]]
                                                 (q {:asks [:place :p :text] :asker (person :ann) :session :run-7 :lanes [:near :far]}))
                                      nil))
        cal-id (uuid-of [:cal-probe-5])
        base-tables (reduce write {}
                            [[:make :near 1] [:make :far 1] [:open :far 2]
                             [:marks :near #{:parent} 3] [:marks :far #{:parent} 3]
                             [:permission :near (person :root) 4] [:give :near (person :bea) (person :root) 5]
                             [:give :near (person :cal) (person :bea) 6 {:id cal-id}]
                             [:give :near (person :ann) (person :root) 7]
                             [:fact :near (person :root) :p1 :parent :canvas-x 10] [:fact :far (person :root) :p1 :parent :canvas-1 11]
                             [:fact :near (person :root) :p2 :parent :canvas-1 12]])
        scenarios [[base-tables
                    [[:give-run :near :run-7 (person :ann) (person :cal) 100] [:end :near (person :ann) 101] [:fact :near (person :root) :p1 :parent :canvas-1 102]]
                    {:asks [:points-at :parent :canvas-1] :asker (person :ann) :session :run-7 :lanes [:near :far]}]
                   [base-tables
                    [[:give-run :near :run-7 (person :ann) (person :cal) 100] [:end :near (person :ann) 101] [:end :near (person :bea) 102]
                     [:fact :far (person :root) :p3 :parent :canvas-1 103]]
                    {:asks [:points-at :parent :canvas-1] :asker (person :ann) :session :run-7 :lanes [:near :far]}]
                   [(write base-tables [:list :near [:far] 8])
                    [[:end :near (person :ann) 100] [:give-run :near :run-7 (person :ann) (person :cal) 101] [:fact :near (person :root) :p1 :parent :canvas-1 102]]
                    {:asks [:points-at :parent :canvas-1] :asker (person :ann) :session :run-7 :from-lane :near}]
                   [base-tables
                    [[:give-run :near :run-7 (person :ann) (person :cal) 100] [:end :near (person :ann) 101] [:end :near (person :cal) 102]]
                    {:asks [:place cal-id :softland.store/permission] :asker (person :ann) :session :run-7
                     :lanes [:near :far] :stores-own-facts true}]
                   [(reduce write {} [[:make :near 1] [:make :far 1] [:open :far 2]
                                      [:permission :near (person :root) 3] [:give :near (person :bea) (person :root) 4]
                                      [:fact :far (person :root) :near :note "far-note" 10]])
                    [[:give-run :near :near (person :ann) (person :bea) 100] [:end-run :near :near (person :ann) 101] [:give-run :near :near (person :ann) (person :bea) 102]]
                    {:asks [:entity :near] :asker (person :ann) :session :near :lanes [:near :far] :stores-own-facts true}]]
        probe-5 (mapv (fn [[tables writes query]] (every-order (open-live tables writes (q query)) nil)) scenarios)
        m-run (reduce write {} [[:make :m-run 1] [:permission :m-run (person :ada) 2] [:give-run :m-run :run-9 (person :cal) (person :ada) 3]
                                [:fact :m-run (person :ada) :note :text "from m-run" 4]])
        m-run-bea (reduce write {} [[:make :m-run 1] [:permission :m-run (person :ada) 2] [:give :m-run (person :bea) (person :ada) 3]
                                    [:give-run :m-run :run-9 (person :dov) (person :bea) 4] [:fact :m-run (person :ada) :note :text "from m-run" 5]])
        note (fn [who & session] (q (cond-> {:asks [:place :note :text] :asker who :lanes [:m-run]}
                                       (seq session) (assoc :session (first session)))))]
    (check "the review's probe 3, a live read from a start lane in a sitting with the farthest lane open to everyone: 8 rounds, within the 24 that a live read is given"
           8 (rounds-live probe-3 (in-run-7 :target)))
    (check "the review's probe 6B, the same with the farthest lane closed to its members: 8 rounds, since open and closed lanes take the same rounds through lane-rights"
           8 (rounds-live probe-6b (in-run-7 :canvas-1)))
    (check "the review's probe 4: an asker who may not see the nearer lane, asking in her sitting, while another holder's permission for that sitting is given and ended there: no step hands her anything, in any order, and each order ends on what a read asked once gives"
           [0 nil] [@tells (:wrong probe-4-played)])
    (check "the review's probe 5: its five worlds, every order of the callbacks, and none ends wrong"
           [nil nil nil nil nil] (mapv :wrong probe-5))
    (check "the review's probe 6A, asked once: Cal in no sitting and in her sitting, and Ada in a sitting where Dov holds a permission, each settle in one select call and one lane-rights query, and Dov's chain is never read"
           [[1 [1] [1]] [1 [1] [1]] [1 [1] [1]] [1 [1] [1]]]
           [(once m-run (note (person :cal))) (once m-run (note (person :cal) :run-9))
            (once m-run-bea (note (person :ada))) (once m-run-bea (note (person :ada) :run-9))])
    (shown "the review's probe of the read path: probe 3 opens in" (rounds-live probe-3 (in-run-7 :target))
           "rounds and 6B in" (rounds-live probe-6b (in-run-7 :canvas-1))
           "| probe 4 hands her nothing over" (:states probe-4-played) "worlds | probe 5's five worlds:" (pr-str (mapv :states probe-5)) "worlds, none wrong")))

(deftest every-order-of-the-callbacks
  (let [played (mapv (fn [[what k marks facts seen writes]]
                       (assoc (every-order (points-at-world k marks facts seen writes) nil) :what what))
                     points-at-scenarios)
        small (every-small-points-at nil)
        place (every-small-standing standing-world [:near :far] 3 [:place :p :text] nil)
        entity (every-small-standing standing-world [:near :far] 2 [:entity :p] nil)
        base-played (vec (for [[where at] [["the nearest" (constantly 1)] ["the second" (constantly 2)] ["the farthest" identity]]
                               [what k marks facts seen writes] points-at-scenarios]
                           (assoc (every-order (points-at-world (base-at (at k)) k marks facts seen writes) nil)
                                  :what (str "the base is " where " lane; " what))))
        base-place (every-small-standing base-world [:mine :base] 3 [:place :p :text] nil)
        base-entity (every-small-standing base-world [:mine :base] 2 [:entity :p] nil)
        base-run (every-small-standing base-world [:mine :base] 2 [:run :first :last] nil)
        list-world (fn [asks]
                     (let [tables (reduce write {} [[:make :home 1] [:make :near 1] [:make :far 1]
                                                    [:open :home 2] [:open :near 2] [:open :far 2]
                                                    [:list :home [:near :far] 3]
                                                    [:fact :far (person :maker) :p :text "f0" 11]])]
                       (open-live tables
                                  [[:list :home [:far] 100] [:fact :near (person :maker) :p :text "n1" 101]
                                   [:list :home [:near :far] 102] [:fact :far (person :maker) :p :text "f1" 103]]
                                  (q {:asks asks :asker (person :ann) :from-lane :home}))))
        lists (mapv #(every-order (list-world %) nil) [[:place :p :text] [:entity :p] [:run :first :last]])
        chain-place (every-small-chain 3 [:place :p :text] nil)
        chain-entity (every-small-chain 2 [:entity :p] nil)
        chain-points (every-small-chain 2 [:points-at :parent :canvas-1] nil)
        chain-points-unseen (with-redefs [sources/rights-kind
                                          (let [rights-kind sources/rights-kind]
                                            (fn [query source]
                                              (let [kind (rights-kind query source)]
                                                (when-not (= :permission kind) kind))))]
                              (every-small-chain 2 [:points-at :parent :canvas-1] nil))
        run-place (every-small-run 2 [:place :p :text] nil)
        run-points (every-small-run 2 [:points-at :parent :canvas-1] nil)
        run-open (run-world [[:end-run]] [:place :p :text])
        run-ended (loop [world run-open n 0]
                    (let [moves (moves-of world)]
                      (if (or (empty? moves) (< 200 n)) world (recur (move world (first moves) nil) (inc n)))))
        chain-open (chain-world [[:end-above]] [:place :p :text])
        chain-ended (loop [world chain-open n 0]
                      (let [moves (moves-of world)]
                        (if (or (empty? moves) (< 200 n)) world (recur (move world (first moves) nil) (inc n)))))
        no-mark (mapv (fn [[what k marks facts seen writes]]
                        (when (:wrong (every-order (points-at-world k marks facts seen writes) :no-mark)) (first (str/split what #"\."))))
                      points-at-scenarios)
        counting-left (with-redefs [answer/told-by?
                                    (let [told-by? answer/told-by?]
                                      (fn [query reading known source place]
                                        (and (told-by? query reading known source place)
                                             (not-any? (fn [nearer]
                                                         (some? (get *known-before-the-leaving*
                                                                     (sources/check-source (lanes/home query) nearer
                                                                                           (first place) (second place)))))
                                                       (take-while #(not= (:lane source) %) (:lanes reading))))))]
                        {:scenarios (mapv (fn [[what k marks facts seen writes]]
                                            (when (:wrong (every-order (points-at-world k marks facts seen writes) nil))
                                              (first (str/split what #"\."))))
                                          points-at-scenarios)
                         :small (count (:wrong (every-small-points-at nil)))})]
    (doseq [{:keys [what wrong]} played]
      (check (str "what points at a thing, in every order of the callbacks, the screen at the end is what a read asked once gives: " what)
             nil wrong))
    (check "every list of up to three writes on one place in two lanes, the nearer marking the key or not, from four starts: 672 lists, none with an order that ends wrong"
           [672 []] [(:lists small) (:wrong small)])
    (check "a place through two lanes, the nearer open to everyone or closed and with a permission or none: every list of up to three writes of closing, opening, her permission and the two facts, 620 lists, none with an order that ends wrong"
           [620 []] [(:lists place) (:wrong place)])
    (check "the same for an entity, every list of up to two writes: 120 lists, none with an order that ends wrong"
           [120 []] [(:lists entity) (:wrong entity)])
    (check "a lane that leaves and joins by the start lane's list while its fact and a farther one change: a place, an entity and a whole lane, none with an order that ends wrong"
           [nil nil nil] (mapv :wrong lists))
    (check "a place through a lane in which the asker's lent permission is two beneath the first: every list of up to three writes of the end of the one above hers, a new give to her lender, the end of her own, a new give to her by either holder, and a fact, 258 lists, none with an order that ends wrong; the same for an entity and for what points at a thing under a key that no lane marks, every list of up to two, 42 lists each"
           [[258 []] [42 []] [42 []]]
           [[(:lists chain-place) (:wrong chain-place)] [(:lists chain-entity) (:wrong chain-entity)]
            [(:lists chain-points) (:wrong chain-points)]])
    (check "the player can fail. With permission rows taken as rows that mean nothing for her rights, lists that end the lender above hers end wrong for what points at a thing: no fact of the lane is held there, so nothing else re-queries her rights or tells her that the lane is left out"
           [true true]
           [(pos? (count (:wrong chain-points-unseen))) (contains? (set (:wrong chain-points-unseen)) [[:end-above]])])
    (check "such a read watches each climbed permission row and holder run in :read-set; when the lender above hers ends, the read leaves the lane's fact and the root row above the end, keeps the holder runs that let it in again, and gives the farther lane's fact with the nearer lane left out"
           [[3 2] ["n0" :near] ["f0" :far] true false 3]
           (let [perm-rows (fn [world] (count (filter #(and (= :near (:lane %))
                                                            (= :permission (sources/rights-kind (:query world) %)))
                                                      (:held world))))
                 holder-runs (fn [world] (count (filter #(and (= :near (:lane %))
                                                              (= :holders (sources/rights-kind (:query world) %)))
                                                        (:held world))))
                 fact (sources/place-source :near :near :p :text)]
             [[(perm-rows chain-open) (perm-rows chain-ended)]
              ((juxt :value :lane) (get-in chain-open [:screen :place]))
              ((juxt :value :lane) (get-in chain-ended [:screen :place]))
              (boolean (some #(= :near (:lane %)) (get-in chain-ended [:screen :left-out])))
              (contains? (:held chain-ended) fact)
              (holder-runs chain-ended)]))
    (check "a place through a lane that the asker sees under the permission of the sitting she asks in: every list of up to two writes of the end of the sitting permission, a new sitting give, the end of its lender and a new give to the lender, a lane permission given to her and ended, a sitting give to another holder, and a fact, 72 lists, none with an order that ends wrong; the same for what points at a thing under a key that no lane marks"
           [[72 []] [72 []]] [[(:lists run-place) (:wrong run-place)] [(:lists run-points) (:wrong run-points)]])
    (check "such a read watches the six holder runs and the three climbed permission rows of her sitting permission; when the sitting permission ends, the read leaves the lane's fact and the permission rows while keeping the six holder runs, and her screen gives the farther lane's fact"
           [[3 0 6 6] ["n0" :near] ["f0" :far] true]
           (let [perm-rows (fn [world] (count (filter #(and (= :near (:lane %))
                                                            (= :permission (sources/rights-kind (:query world) %)))
                                                      (:held world))))
                 holder-runs (fn [world] (count (filter #(and (= :near (:lane %))
                                                              (= :holders (sources/rights-kind (:query world) %)))
                                                        (:held world))))]
             [[(perm-rows run-open) (perm-rows run-ended) (holder-runs run-open) (holder-runs run-ended)]
              ((juxt :value :lane) (get-in run-open [:screen :place]))
              ((juxt :value :lane) (get-in run-ended [:screen :place]))
              (boolean (some #(= :near (:lane %)) (get-in run-ended [:screen :left-out])))]))
    (doseq [{:keys [what wrong]} base-played]
      (check (str "what points at a thing with one lane of the list being the base, each source of the base read under the routing id of the nearest lane: in every order of the callbacks the screen at the end is what a read asked once gives: " what)
             nil wrong))
    (check "the base is read in those worlds, under the routing id of the nearest lane: with the base second, each read that may see it and whose base marks the key holds the base's order by value under the nearest lane's routing id, and the scenario of four lanes comes to hold a check of the base under it; with the base nearest, under the base's own"
           [[true] true [true]]
           (let [holds? (fn [world table routing-id]
                          (boolean (some #(= [:microbatch table routing-id] ((juxt :gate :table :routing-id) %)) (:held world))))
                 opened (fn [i [_ k marks facts seen writes]] (points-at-world (base-at i) k marks facts seen writes))]
             [(vec (distinct (map (fn [[_ _ marks _ seen :as scenario]]
                                    (or (not (get marks 2)) (not (contains? seen 2))
                                        (holds? (opened 2 scenario) :facts-by-value :l1)))
                                  points-at-scenarios)))
              (let [twelve (nth points-at-scenarios 11)
                    world (loop [world (opened 2 twelve) n 0]
                            (let [moves (moves-of world)]
                              (if (or (empty? moves) (< 200 n)) world (recur (move world (first moves) nil) (inc n)))))]
                (boolean (some #(and (= [:microbatch :facts :l1] ((juxt :gate :table :routing-id) %))
                                     (= :row (first (:path %)))
                                     (sources/check? (:query world) %))
                               (:held world))))
              (vec (distinct (map (fn [[_ _ marks _ seen :as scenario]]
                                    (or (not (get marks 1)) (not (contains? seen 1))
                                        (holds? (opened 1 scenario) :facts-by-value :base)))
                                  points-at-scenarios)))]))
    (check "a place through her own lane and the base, the base open to everyone or closed and with a permission or none, each source of the base under her lane's routing id: every list of up to three writes of closing, opening, her permission and the two facts, 620 lists, none with an order that ends wrong"
           [620 []] [(:lists base-place) (:wrong base-place)])
    (check "the same for an entity of the base and for the whole of the two lanes, every list of up to two writes: 120 lists each, none with an order that ends wrong"
           [[120 []] [120 []]] [[(:lists base-entity) (:wrong base-entity)] [(:lists base-run) (:wrong base-run)]])
    (check "in those worlds the read holds the base's own rows, its holder runs, her permission row and the fact as sources of the microbatch gate under her own lane's routing id, and none under the base's"
           [#{[:microbatch :facts :mine] [:microbatch :permissions-by-holder :mine]}
            #{[:own-rows] [:holders] [:row]}]
           (let [world (base-world false true [] [:place :p :text])
                 of-base (filter #(= :base (:lane %)) (:held world))]
             [(set (map (juxt :gate :table :routing-id) of-base))
              (set (map (comp vector first :path) of-base))]))
    (check "the player can fail. With the mark dropped, so that no step carries an owed answer to a later one, the scenarios in which a lane joins over a farther order end wrong"
           [true true] [(contains? (set no-mark) "9") (contains? (set no-mark) "13")])
    (check "the player can fail. With told-by? counting a check that the same step has just left, the plan's own crossing ends wrong, and so do lists of the small ones"
           [true true]
           [(contains? (set (:scenarios counting-left)) "1") (pos? (:small counting-left))])
    (shown "every order of the callbacks, played on the functions that the read path calls: the thirteen scenarios of what points at a thing,"
           (reduce + (map :states played)) "states and" (reduce + (map :ends played)) "ends; every list of up to three writes on one place in two lanes,"
           (:lists small) "lists and" (:states small) "states; in each end the asker's screen is what a read asked once gives from the tables")
    (shown "the same for a place through a lane opened and closed to everyone,"
           (:lists place) "lists and" (:states place) "states; for an entity," (:lists entity) "lists and" (:states entity)
           "states; and while the start lane's list drops a lane and takes it back,"
           (reduce + (map :states lists)) "states")
    (shown "the base read under the routing id of the asker's own lane, in every order of the callbacks:"
           (reduce + (map :states base-played)) "states across the thirteen scenarios,"
           (:lists base-place) "place lists and" (:states base-place) "states, none wrong")
    (shown "the player can fail: with the mark dropped the scenarios" (pr-str (filterv some? no-mark))
           "end wrong; with told-by? counting a check that the step has just left, the scenarios"
           (pr-str (filterv some? (:scenarios counting-left))) "and" (:small counting-left) "of the 672 small lists end wrong")
    (shown "a lent permission two beneath the lane's first, in every order of the callbacks: a place,"
           (:lists chain-place) "lists and" (:states chain-place) "states; an entity," (:lists chain-entity) "lists; what points at a thing,"
           (:lists chain-points) "lists, none wrong")
    (shown "an asker who asks in a sitting, while her sitting permission, its lender and her lane permission change, in every order of the callbacks: a place,"
           (:lists run-place) "lists and" (:states run-place) "states; what points at a thing," (:lists run-points) "lists and" (:states run-points) "states, none wrong")
    (shown "that Rama's client delivers in these ways and no other is not shown here: the probes of this build ran it, in-process")))

(deftest the-size-of-a-run-and-the-order-of-its-keys
  (let [small (assoc numbers :max-places-of-one-run 20000)
        row (fn [held stamp] {:value held :by-act (UUID. 0 1) :by-index 0 :stamp stamp :who ":a" :permission "[:a :l :l]"})
        a (String. "\"one\"")
        b (String. "\"one\"")
        run {:gate :streaming :table :facts :lane :l :routing-id :l :path [:run ":l :e " ":l :e!"]}
        one-row {:gate :streaming :table :facts :lane :l :routing-id :l :path [:row ":l :e :k"]}]
    (check "the size of a run, from a bounded walk: a full walk whose last key is before the run's end is too large; a full walk that ran on past the run's end is not; a walk that is not full is not; a walk that holds nothing is not"
           [true false false false]
           [(sources/over-the-limit? [20001 ":l :e k020000"] ":l :e!" small)
            (sources/over-the-limit? [20001 ":m :big k019996"] ":l :e!" small)
            (sources/over-the-limit? [1 ":l :e :x"] ":l :e!" small)
            (sources/over-the-limit? [0] ":l :e!" small)])
    (check "the first read of a run is the whole run when it gave no more places than it takes first"
           [true true false]
           [(sources/run-is-whole? 256 numbers) (sources/run-is-whole? {} numbers) (sources/run-is-whole? 257 numbers)])
    (check "the order of a table's keys is by code point: a space, !, z, a letter outside ASCII, U+E000, U+FFFF, a letter outside the first plane"
           ["L x" "L x a" "L x!" "L xz" "L xä" "L x" "L x￿" (str "L x" (String. (Character/toChars 0x1F600)))]
           (sort by-code-point
                 (shuffle ["L x!" "L x" "L xz" (str "L x" (String. (Character/toChars 0x1F600))) "L x￿" "L xä" "L x a" "L x"])))
    (check "a text that is the start of another is before it; a text is not before itself; Java's own order puts the letter outside the first plane before U+E000, and this order does not"
           [true false false true false]
           [(rows/before? ":l :a" ":l :a!") (rows/before? ":l :a" ":l :a") (rows/before? ":l :ab" ":l :a!")
            (neg? (compare (String. (Character/toChars 0x1F600)) ""))
            (rows/before? (String. (Character/toChars 0x1F600)) "")])
    (check "the ends of a run in the facts table: an entity's run holds no place of an entity whose text begins with its text; the lane's own rows are the store's namespace on its own entity"
           [[":l :a " ":l :a!"] true false [":l :l :softland.store/" ":l :l :softland.store0"]]
           [(rows/entity-run :l :a)
            (rows/before? ":l :a :k" ":l :a!")
            (rows/before? ":l :a! :k" ":l :a!")
            (rows/own-rows-run :l)])
    (check "a run of two ends: from a place to a place with the last place in it; ends in the wrong order give nothing"
           [[":l :e :a" ":l :e :m "] [":l " ":l!"] [":l :e " ":l :f!"] nil nil]
           [(rows/run-of-ends :l [:e :a] [:e :m]) (rows/run-of-ends :l :first :last) (rows/run-of-ends :l [:e] [:f])
            (rows/run-of-ends :l [:f] [:e]) (rows/run-of-ends :l [:e :m] [:e :a])])
    (check "two rows are the same when each field is equal: a value that is an equal String and not one object; nil and nil; a row and nil; rows that differ in one field; a field that one row has not; no field of a row is a byte array"
           [true false true false false false false true]
           [(rows/same-row? (row a 5) (row b 5)) (identical? a b) (rows/same-row? nil nil) (rows/same-row? (row a 5) nil)
            (rows/same-row? (row a 5) (row a 6)) (rows/same-row? (row a 5) (row "\"two\"" 5))
            (rows/same-row? (row a 5) (dissoc (row a 5) :value))
            (not-any? bytes? (vals (row a 5)))])
    (check "which keys a change names: a row that is the same before and after is dropped, also the row that a second attempt wrote again; the removal of a place that the run never held is no change"
           [#{} #{":l :e :k"} #{":l :e :b"} #{} #{":l :e :b"}]
           [(sources/named one-row :all (row a 5) (row b 5))
            (sources/named one-row :all (row a 5) (row a 6))
            (sources/named run [[:put ":l :e :a"] [:put ":l :e :b"]]
                           {":l :e :a" (row a 5) ":l :e :b" (row a 5)} {":l :e :a" (row b 5) ":l :e :b" (row a 6)})
            (sources/named run [[:out ":l :e :never"]] {":l :e :a" (row a 5)} {":l :e :a" (row a 5)})
            (sources/named run :all {":l :e :a" (row a 5) ":l :e :b" (row a 5)} {":l :e :a" (row b 5)})])
    (check "the row under a key of a run, where the map that holds the run finds keys by another order than the table's: a key with a letter outside the range in which the two orders agree is found by going through the run; a key inside that range is looked up and the run is not gone through"
           [:the-row nil :found]
           (let [wide (str ":l :x" (String. (Character/toChars 0x1D49C)) " :n")
                 misses (fn [rows] (reify
                                     clojure.lang.ILookup
                                     (valAt [_ _] nil)
                                     (valAt [_ _ d] d)
                                     clojure.lang.Seqable
                                     (seq [_] (rows))))]
             [(sources/row-in (misses (fn [] (seq [[":l :xz :n" :another] [wide :the-row]]))) wide)
              (sources/row-in (misses (fn [] (throw (ex-info "the run was gone through" {})))) ":l :xz :n")
              (sources/row-in {":l :xz :n" :found} ":l :xz :n")]))
    (check "an entity whose id holds such a letter is taken as an entity, and so is a run whose ends hold letters outside ASCII and below U+D800"
           [true true]
           [(some? (q {:asks [:entity (keyword (str "x" (String. (Character/toChars 0x1D49C))))] :asker (person :alice) :lanes [:team]}))
            (some? (q {:asks [:run [(keyword "x\u00E4")] [(keyword "x\u6C34")]] :asker (person :alice) :lanes [:team]}))])
    (check "the keys of a run whose end holds such a letter are held to the run's two ends, so a key that a range gave from outside them is dropped; the keys of any other run are taken as they came"
           [[":l :x\uFB00 :n"] 2]
           (let [wide (String. (Character/toChars 0x1D49C))
                 inside ":l :x\uFB00 :n"
                 outside (str ":l :x" wide " :n")
                 odd {:gate :streaming :table :facts :lane :l :routing-id :l :path [:run ":l :x\uFB00 " ":l :x\uFB00!"]}
                 plain {:gate :streaming :table :facts :lane :l :routing-id :l :path [:run ":l :e " ":l :e!"]}]
             [(vec (sources/keys-of-run odd {inside :a-row outside :a-neighbours-row}))
              (count (sources/keys-of-run plain {":l :e :a" :a-row ":l :zz :a" :not-tested}))]))
    (check "the first read of a run is made only when neither of its ends holds a letter outside the range in which the two orders agree; a run with such an end is read by its size and whole, and not before the asker's standing is known"
           [true false false true false true]
           (let [wide (String. (Character/toChars 0x1D49C))
                 a-run (fn [from to] {:gate :streaming :table :facts :lane :l :routing-id :l :path [:run from to]})
                 plain (a-run ":l :e " ":l :e!")
                 wide-end (a-run ":l :e " (str ":l :x" wide "!"))
                 wide-start (a-run ":l :xﬀ " ":l :y!")]
             [(sources/first-read-holds? plain) (sources/first-read-holds? wide-end) (sources/first-read-holds? wide-start)
              (sources/before-standing? :once plain) (sources/before-standing? :once wide-end)
              (sources/before-standing? :once one-row)]))
    (shown "the size rule of a run: a full bounded walk whose last key is still before the run's end is too large, and no other; the order that a run's two ends are held to is by code point, which is Rama's order of String keys and not Java's")
    (shown "a row that is written again with the same fields is no change, and neither is the removal of a place that a run never held")))

(deftest the-words
  (let [each (keys words/words)
        made (words/word :serve-a-read-that-large {:what :places-of-one-run :most 20000 :places {:a 1} :lanes [:l]})]
    (check "the read path's own eleven words"
           #{:find-the-module :read-the-query :reach-the-store :get-an-answer-within-its-wait
             :read-inside-an-askers-function :serve-a-read-that-large :keep-more-reads-live :keep-the-read-live
             :find-the-line-whole :keep-a-read-of-acts-live :too-many-permissions}
           (set each))
    (check "the word of the read path's own bound on its pushes is given by a read that is kept live, and by no read asked once"
           #{:read-live} (:from (get words/words :keep-more-reads-live)))
    (check "the two words of a read of a lane's acts: a line that is not whole, given by a read asked once; and the read of acts kept live, which is turned away"
           [#{:read-once} #{:read-live}]
           [(:from (get words/words :find-the-line-whole)) (:from (get words/words :keep-a-read-of-acts-live))])
    (check "the word of a lane that passed the bound on permissions is given by both calls, carries the lane and the bound, and drops parts of an answer"
           [#{:read-once :read-live} {:could-not :too-many-permissions :lane :lab :most 32}]
           [(:from (get words/words :too-many-permissions))
            (words/word :too-many-permissions {:lane :lab :most 32 :acts [] :last-stamp 5})])
    (check "each word says what it is and which call gives it; a word that is not declared is not made"
           [true nil]
           [(every? (fn [w] (let [entry (get words/words w)] (and (string? (:says entry)) (seq (:from entry))))) each)
            (words/word :no-such-word {})])
    (check "a word holds :could-not and what it carries, and no part of an answer"
           {:could-not :serve-a-read-that-large :what :places-of-one-run :most 20000} made)
    (check "what a word can carry is declared, and so are the seven reasons for which a live read ends and the two words for a lane that is left out"
           [#{:module :why :at :threw :said :waited-ms :what :most :lane :because :stamp}
            #{:the-store-ended-a-push :a-step-did-not-settle :a-run-grew-too-large :a-read-it-came-to-need-failed
              :no-room-for-a-push-it-came-to-need :too-many-permissions :its-function-threw}
            #{:may-not-see :not-made}]
           [(set (keys words/carried)) (set (keys words/because)) (set (keys words/left-out-words))])
    (check "no word of the read path is a gate's reason, a word of a stray, or a word with which the check of a query turns a thing away"
           [#{} #{} #{} #{}]
           [(set (filter act/reasons each)) (set (filter declaration/reasons each))
            (set (filter (set declaration/stray-reasons) each)) (set (filter query/reasons each))])
    (check "one word is spelled as a word of the door: the opening of each cannot find the module; no other is"
           #{:find-the-module} (set (filter (set (keys door-words/words)) each)))
    (check "no word with which the check of a query turns a thing away is a gate's reason or a word of the door"
           [#{} #{} #{}]
           [(set (filter act/reasons query/reasons)) (set (filter declaration/reasons query/reasons))
            (set (filter (set (keys door-words/words)) query/reasons))])
    (shown "the read path's eleven words are declared, each with what it says:" (pr-str (sort each))
           "| none is a gate's reason or a word of a stray; one, :find-the-module, is spelled as the door's word for its own opening")))

;; ------------------------------------------------- a lane's acts by stamp

(defn- digest-of
  "The digest of a decision in this proof's line: 32 bytes made of its
  stamp, so that two decisions have two digests. The read path reads none:
  it hands the line row's hex text on."
  [stamp]
  (byte-array (repeat 32 (unchecked-byte (long stamp)))))

(defn- acts-setting
  "Stands for a gate that takes the lane's acts setting: puts the row of
  `:softland.store/who-may-read-acts` on the lane's entity, as a fact of
  its maker. Takes the tables, the lane, the word and the stamp."
  [tables lane word stamp]
  (put tables (first-act/depot-at-making lane) :facts
       (rows/place-key lane lane :softland.store/who-may-read-acts)
       (fact-row* lane (person :maker) lane :softland.store/who-may-read-acts word stamp)))

(defn- decide
  "Stands for a gate's decision in a lane's line: puts its line row, its
  thing when it is kept, the lane's last stamp, and its row by who when
  the thing names a who, in the tables, as `softland.store.gate.decision`
  has a gate write them."
  [tables lane stamp {:keys [who refused thing not-kept weight]}]
  (let [gate (first-act/depot-at-making lane)
        k (rows/line-key lane stamp)
        weight (when-not not-kept (or weight (count (pr-str thing))))
        row (rows/line-row (UUID. 3 (long stamp)) refused (time-of stamp) (digest-of stamp) who weight not-kept)]
    (cond-> (-> tables
                (put gate :line k row)
                (put gate :last-stamp (declaration/id-text lane) stamp))
      (not not-kept) (put gate :things k thing)
      who (put gate :by-who (rows/by-who-key lane who stamp) stamp))))

(defn- window-of
  [t from to n]
  (into (sorted-map-by by-code-point)
        (filter (fn [[k]] (rows/before? k to)))
        (take n (subseq t >= from))))

(defn- acts-value
  [tables source]
  (let [[kind a b n] (:path source)
        gate (:gate source)]
    (case [(:table source) kind]
      [:task :last-stamp] (get (table tables gate :last-stamp) a)
      [:task :by-who] (window-of (table tables gate :by-who) a b n)
      [:line :window] (window-of (table tables gate :line) a b n)
      [:things :window] (window-of (table tables gate :things) a b n)
      (table-value tables source))))

(defn- play-acts
  "Plays a read of acts asked once, as `softland.store.read-path/read-acts-once`
  plays it: last stamp first, then own rows and `lane-rights` for `:acts`
  together, then line/by-who, then things."
  ([tables query] (play-acts (constantly tables) query numbers))
  ([tables-at query nums]
   (let [lane (lanes/home query)]
     (loop [round 0 known {} snapshot nil read []]
       (let [{:keys [answer fault missing needs-rights too-many-permissions]}
             (acts/reading query known nums snapshot)]
         (cond
           answer {:answer answer :rounds round :read read}
           too-many-permissions {:too-many-permissions too-many-permissions :rounds round :read read}
           fault {:fault fault :rounds round :read read}
           (or (and (empty? missing) (not needs-rights)) (< 6 round))
           {:stuck true :rounds round :read read}
           :else
           (let [t (tables-at round)
                 fetched (into {} (map (fn [s] [s (acts-value t s)])) missing)
                 snap (when needs-rights
                        (run-rights t lane :acts (:asker query) (:session query)))
                 round-sources (cond-> (set missing)
                                 needs-rights
                                 (conj {:gate (first-act/depot-at-making lane)
                                        :table :lane-rights
                                        :lane lane
                                        :routing-id lane
                                        :path [:rights :acts]}))]
             (recur (inc round)
                    (merge known fetched)
                    (if needs-rights snap snapshot)
                    (conj read round-sources)))))))))

(defn- acts-of
  ([lane start most asker] (acts-of lane start most asker nil))
  ([lane start most asker session]
   (q (cond-> {:asks [:acts start most] :from-lane lane}
        asker (assoc :asker asker)
        (nil? asker) (assoc :not-signed-in true)
        session (assoc :session session)))))

(defn- given
  [answer]
  (mapv (fn [d] [(:stamp d) (cond (contains? d :thing) :whole
                                  (contains? d :no-thing) (:no-thing d)
                                  :else :withheld)])
        (:acts answer)))

(def ^:private lab-things
  [{:who (person :ann) :thing {:who (person :ann) :facts [:make]}}
   {:who (person :ann) :thing {:who (person :ann) :facts [:give :bo]}}
   {:who (person :cy) :refused :permission-not-found :thing {:who (person :cy) :facts [:try 1]}}
   {:who (person :bo) :thing {:who (person :bo) :facts [:note]}}
   {:refused :malformed :thing {:facts [:no-who]}}
   {:who (person :cy) :refused :permission-not-found :not-kept :too-heavy :thing {:who (person :cy) :facts [:heavy]}}
   {:who (person :ann) :thing {:who (person :ann) :facts [:last]}}])

(defn- lab-world
  "The tables of the proof's lane `:lab`, a streaming lane: made by Ann at
  stamp 0 with her first permission, Bo given one lent beneath hers at stamp 1,
  and the seven decisions of `lab-things` in its line."
  [& more]
  (let [facts (reduce (fn [t w]
                        (if (= :acts-setting (first w))
                          (apply acts-setting t (rest w))
                          (write t w)))
                      {}
                      (concat [[:make :lab 0] [:permission :lab (person :ann) 0] [:give :lab (person :bo) (person :ann) 1]] more))]
    (reduce (fn [t [stamp d]] (decide t :lab stamp d)) facts (map-indexed vector lab-things))))

(deftest who-reads-a-lanes-acts
  (let [holder (play-acts (lab-world) (acts-of :lab 0 10 (person :ann)))
        given-one (play-acts (lab-world) (acts-of :lab 0 10 (person :bo)))
        outside (play-acts (lab-world) (acts-of :lab 0 10 (person :cy)))
        open (lab-world [:open :lab 7])
        open-and-read (lab-world [:open :lab 7] [:acts-setting :lab :everyone 8])
        bo-ended (lab-world [:end :lab (person :bo) 7])
        all-whole [[0 :whole] [1 :whole] [2 :whole] [3 :whole] [4 :whole] [5 :too-heavy] [6 :whole]]]
    (check "a holder of a lane, its first or one given beneath her, reads each decision of its line whole, from the start: its answer and its thing, a thing that the gate kept not with its word, and the last stamp"
           [all-whole 6 7 [] all-whole]
           [(given (:answer holder)) (:last-stamp (:answer holder)) (:next (:answer holder)) (:left-out (:answer holder))
            (given (:answer given-one))])
    (check "a decision given whole holds its act-id, the answer of the door's read by act-id, and the thing as it arrived"
           [[:lab :streaming (UUID. 3 2)]
            {:verdict :refused :reason :permission-not-found :stamp 2 :time (time-of 2) :digest (text/hex (digest-of 2))}
            {:who (person :cy) :facts [:try 1]}]
           (let [d (nth (:acts (:answer holder)) 2)] [(:act-id d) (:answer d) (:thing d)]))
    (check "an asker who may not see a lane is given her own decisions there, taken or refused, and nothing else of it: not the stamps of others, not its last stamp; the answer says that the lane is left out"
           [[[2 :whole] [5 :too-heavy]] false [{:lane :lab :why :may-not-see}] 6]
           [(given (:answer outside)) (contains? (:answer outside) :last-stamp) (:left-out (:answer outside))
            (:next (:answer outside))])
    (check "a visitor of a lane that everyone may see, while its acts setting is members, reads her own decisions whole and every other one as its stamp alone; one who is not signed in, each as its stamp alone"
           [[[0 :withheld] [1 :withheld] [2 :whole] [3 :withheld] [4 :withheld] [5 :too-heavy] [6 :withheld]] 6
            #{#{:stamp}}
            [[0 :withheld] [1 :withheld] [2 :withheld] [3 :withheld] [4 :withheld] [5 :withheld] [6 :withheld]]]
           (let [cy (:answer (play-acts open (acts-of :lab 0 10 (person :cy))))
                 nobody (:answer (play-acts open (acts-of :lab 0 10 nil)))]
             [(given cy) (:last-stamp cy)
              (set (map (comp set keys) (remove #(contains? % :act-id) (:acts cy))))
              (given nobody)]))
    (check "with the acts setting at everyone, whoever may see the lane reads its acts whole, one who is not signed in too; a holder reads them as before"
           [all-whole all-whole all-whole]
           [(given (:answer (play-acts open-and-read (acts-of :lab 0 10 (person :cy)))))
            (given (:answer (play-acts open-and-read (acts-of :lab 0 10 nil))))
            (given (:answer (play-acts open-and-read (acts-of :lab 0 10 (person :ann)))))])
    (check "the acts setting turns nothing on in a lane that is open to its members alone: an asker who may not see it is given her own and nothing else"
           [[2 :whole] [5 :too-heavy]]
           (given (:answer (play-acts (lab-world [:acts-setting :lab :everyone 7]) (acts-of :lab 0 10 (person :cy))))))
    (check "a holder whose permission ended reads the lane's acts as an asker who may not see it: her own alone"
           [[[3 :whole]] [{:lane :lab :why :may-not-see}]]
           (let [a (:answer (play-acts bo-ended (acts-of :lab 0 10 (person :bo))))] [(given a) (:left-out a)]))
    (check "a lent holder beneath an ended lender reads as outside, whereas a standing give to a person stays a holder when her inviter's ends; on an open lane a member stays a holder, an un-lent agent is outside, and past 32 permissions the read returns :too-many-permissions"
           [[[9 :whole]] [{:lane :lab :why :may-not-see}]
            all-whole all-whole [] {:lane :lab :most 32}]
           (let [lent-ended (-> (lab-world [:give :lab (person :dee) (person :bo) 7] [:end :lab (person :bo) 8])
                                (decide :lab 7 {:who (person :bo) :thing {:who (person :bo) :facts [:give :dee]}})
                                (decide :lab 8 {:who (person :ann) :thing {:who (person :ann) :facts [:end :bo]}})
                                (decide :lab 9 {:who (person :dee) :thing {:who (person :dee) :facts [:mine]}}))
                 a-lent (:answer (play-acts lent-ended (acts-of :lab 0 10 (person :dee))))
                 standing-kept (lab-world [:give :lab (person :dee) (person :bo) 7 {:lent? false}] [:end :lab (person :bo) 8])
                 flood (reduce (fn [t i]
                                 (write t [:give :lab (agent :ada) (person :ann) (+ 20 i) {:allows {:write :all}}]))
                               open
                               (range 33))]
             [(given a-lent) (:left-out a-lent)
              (given (:answer (play-acts standing-kept (acts-of :lab 0 10 (person :dee)))))
              (given (:answer (play-acts open (acts-of :lab 0 10 (person :bo)))))
              (given (:answer (play-acts open (acts-of :lab 0 10 (agent :ada)))))
              (:too-many-permissions (play-acts flood (acts-of :lab 0 10 (agent :ada))))]))
    (check "the permission of the run that she asks in lets a holder of no permission of her own read the acts whole; a yes for one act lets nobody read them"
           [all-whole [[2 :whole] [5 :too-heavy]]]
           (let [run (lab-world [:give-run :lab :run-1 (person :cy) (person :ann) 7])
                 yes (lab-world [:give-act :lab (UUID. 9 9) (person :cy) (person :ann) 7])]
             [(given (:answer (play-acts run (acts-of :lab 0 10 (person :cy) :run-1))))
              (given (:answer (play-acts yes (acts-of :lab 0 10 (person :cy)))))]))
    (check "in a lane that is not made, the who that a refused thing names reads it whole, and nobody else is given anything of the lane"
           [[[0 :whole] [2 :whole]] [{:lane :void :why :not-made}] [] [{:lane :void :why :not-made}]]
           (let [w (-> {}
                       (decide :void 0 {:who (person :cy) :refused :lane-not-made :thing {:who (person :cy) :n 0}})
                       (decide :void 1 {:who (person :zed) :refused :lane-not-made :thing {:who (person :zed) :n 1}})
                       (decide :void 2 {:who (person :cy) :refused :lane-not-made :thing {:who (person :cy) :n 2}}))
                 cy (:answer (play-acts w (acts-of :void 0 10 (person :cy))))
                 nobody (:answer (play-acts w (acts-of :void 0 10 nil)))]
             [(given cy) (:left-out cy) (given nobody) (:left-out nobody)]))
    (check "the base's acts are read on the base's own task, where its gate decides: each source of the read has the base as its routing id, in the microbatch gate"
           [[[0 :whole] [1 :whole]] #{[:microbatch :base]}]
           (let [w (-> {}
                       (write [:make :base 0]) (write [:permission :base :system 0])
                       (decide :base 0 {:who :system :thing {:who :system :n 0}})
                       (decide :base 1 {:who (person :cy) :refused :permission-not-found :thing {:who (person :cy) :n 1}}))
                 played (play-acts w (acts-of :base 0 10 :system))]
             [(given (:answer played)) (set (map (juxt :gate :routing-id) (apply concat (:read played))))]))
    (shown "who reads a lane's acts, on the proof's lane of seven decisions:"
           "a holder" (pr-str (given (:answer holder)))
           "| an asker outside" (pr-str (given (:answer outside))))))

(deftest a-read-of-acts-is-bounded
  (let [w (lab-world)
        light (assoc numbers :max-thing-weight-of-one-read 60)
        made (-> {} (write [:make :lab 0]) (write [:permission :lab (person :ann) 0]))
        three (-> made
                  (decide :lab 0 {:who (person :ann) :thing {:n 0} :weight 30})
                  (decide :lab 1 {:who (person :ann) :thing {:n 1} :weight 30})
                  (decide :lab 2 {:who (person :ann) :thing {:n 2} :weight 30}))
        heavy (-> made
                  (decide :lab 0 {:who (person :ann) :thing {:n 0} :weight 6})
                  (decide :lab 1 {:who (person :ann) :thing {:n 1} :weight 61})
                  (decide :lab 2 {:who (person :ann) :thing {:n 2} :weight 6}))]
    (check "a read gives `most` decisions at most, and says where the next one begins; `most` is cut to the read path's number"
           [[[0 :whole] [1 :whole] [2 :whole]] 3 3]
           (let [a (:answer (play-acts (constantly w) (acts-of :lab 0 3 (person :ann)) numbers))
                 cut (:answer (play-acts (constantly w) (acts-of :lab 0 100 (person :ann)) (assoc numbers :max-acts-of-one-read 3)))]
             [(given a) (:next a) (count (:acts cut))]))
    (check "the things of one answer weigh no more than the read's bound: the answer ends before the first thing that does not fit, and the next read begins at its stamp"
           [[[0 :whole] [1 :whole]] 2]
           (let [a (:answer (play-acts (constantly three) (acts-of :lab 0 10 (person :ann)) light))]
             [(given a) (:next a)]))
    (check "a thing that alone weighs more than the read's bound does not stop the line: its stamp, act-id and answer are given, with the word that tells it from a withheld one, and the read goes on behind it"
           [[[0 :whole] [1 :heavier-than-the-read] [2 :whole]] 3 true]
           (let [a (:answer (play-acts (constantly heavy) (acts-of :lab 0 10 (person :ann)) light))]
             [(given a) (:next a) (every? #(contains? % :answer) (:acts a))]))
    (check "from a stamp past the last one, an asker who may see the lane is given nothing, the last stamp, and the same stamp to begin from"
           [[] 6 20]
           (let [a (:answer (play-acts w (acts-of :lab 20 10 (person :ann))))] [(:acts a) (:last-stamp a) (:next a)]))
    (shown "a read of acts within its bounds: three things of 30 against a bound of 60 give two;"
           "things of 6, 61 and 6 give" (pr-str (given (:answer (play-acts (constantly heavy) (acts-of :lab 0 10 (person :ann)) light)))))))

(deftest the-rounds-of-a-read-of-acts
  (let [w (lab-world)
        holder (play-acts w (acts-of :lab 0 10 (person :ann)))
        given-one (play-acts w (acts-of :lab 0 10 (person :bo)))
        outside (play-acts w (acts-of :lab 0 10 (person :cy)))
        kinds (fn [played] (mapv (fn [round] (into (sorted-set) (map (fn [s] [(:table s) (first (:path s))])) round)) (:read played)))
        line-reads (fn [played] (for [round (:read played) s round :when (#{:line :things} (:table s))] s))]
    (check "the last stamp is read in a round of its own, before the rights; the rights before the line; the line before its things"
           [[#{[:task :last-stamp]} #{[:facts :own-rows] [:lane-rights :rights]} #{[:line :window]} #{[:things :window]}]
            [#{[:task :last-stamp]} #{[:facts :own-rows] [:lane-rights :rights]} #{[:line :window]} #{[:things :window]}]]
           [(kinds holder) (kinds given-one)])
    (check "an asker who may not see the lane reads her rows by who and no window of the line: each row of the line and each thing that her read fetches is one of hers"
           [[#{[:task :last-stamp]} #{[:facts :own-rows] [:lane-rights :rights]} #{[:task :by-who]} #{[:line :window]} #{[:things :window]}]
            #{2 5}]
           [(kinds outside)
            (into #{} (mapcat (fn [s] (let [[_ from to] (:path s)]
                                        (keep rows/stamp-of-line-key [from (str/trim to)]))))
                  (line-reads outside))])
    (check "a holder's page of kept things is one window of the things table, across a decision whose thing the gate kept not; a visitor's own things are read alone, never a withheld one's"
           [[[(rows/line-key :lab 0) (str (rows/line-key :lab 6) " ") 6]]
            [[(rows/line-key :lab 2) (str (rows/line-key :lab 2) " ") 1]]]
           (let [visitor (play-acts (lab-world [:open :lab 7]) (acts-of :lab 0 10 (person :cy)))]
             [(for [s (last (:read holder))] (rest (:path s)))
              (for [s (last (:read visitor))] (rest (:path s)))]))
    (check "a read of acts takes five rounds at most: four for a holder, first or lent, five for an asker outside, also beneath an ended lender, and two for one who is not signed in and outside"
           [4 4 5 5 2]
           (let [dee (-> (lab-world [:give :lab (person :dee) (person :bo) 7] [:end :lab (person :bo) 8])
                         (decide :lab 7 {:who (person :bo) :thing {:n 7}})
                         (decide :lab 8 {:who (person :ann) :thing {:n 8}})
                         (decide :lab 9 {:who (person :dee) :thing {:n 9}}))]
             [(:rounds holder) (:rounds given-one) (:rounds outside)
              (:rounds (play-acts dee (acts-of :lab 0 10 (person :dee))))
              (:rounds (play-acts w (acts-of :lab 0 10 nil)))]))
    (check "a decision that lands after the last stamp was read is not given, though the window holds it: it is decided after the rights were read, and the next read gives it"
           [[0 1 2 3 4 5 6] 6 7]
           (let [later (decide w :lab 7 {:who (person :ann) :thing {:n 7}})
                 a (:answer (play-acts (fn [round] (if (zero? round) w later)) (acts-of :lab 0 10 (person :ann)) numbers))]
             [(mapv :stamp (:acts a)) (:last-stamp a) (:next a)]))
    (check "a line that is not whole gives the first stamp at which a row is not there, and no answer: a row of the line up to the last stamp, or a thing that the line says is kept"
           [{:stamp 3} {:stamp 2} {:stamp 3}]
           (let [no-row (update w [:streaming :line] dissoc (rows/line-key :lab 3))
                 no-thing (update w [:streaming :things] dissoc (rows/line-key :lab 2))]
             [(:fault (play-acts no-row (acts-of :lab 0 10 (person :ann))))
              (:fault (play-acts no-thing (acts-of :lab 0 10 (person :ann))))
              (:fault (play-acts no-row (acts-of :lab 0 10 (person :bo))))]))
    (check "the runs of neighbouring stamps: a gap or a stamp to break ends a run, a stamp to bridge goes on, and each run counts what it takes"
           [[[0 1 2]] [[0 0 1] [2 2 1]] [[0 2 2]] [[0 0 1] [3 3 1]] [] [[5 6 2] [9 9 1]]]
           [(acts/runs [[0 :take] [1 :take]])
            (acts/runs [[0 :take] [1 :break] [2 :take]])
            (acts/runs [[0 :take] [1 :bridge] [2 :take]])
            (acts/runs [[0 :take] [3 :take]])
            (acts/runs [[0 :bridge] [1 :break]])
            (acts/runs [[4 :bridge] [5 :take] [6 :take] [7 :bridge] [9 :take]])])
    (shown "the rounds of a read of acts: a holder" (pr-str (kinds holder)) "| an asker outside" (pr-str (kinds outside)))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-form-of-a-query)
  (a-word-is-declared-in-three-places)
  (the-lanes)
  (the-sources)
  (the-loop-ends)
  (the-rounds-of-a-read-are-the-chains-of-its-sources)
  (the-checks-of-nearer-lanes)
  (what-a-read-gives)
  (which-changes-reach-an-asker)
  (an-asker-who-may-not-see-a-lane-is-told-nothing-of-it)
  (an-asker-whose-own-permission-does-not-stand-is-told-nothing)
  (a-row-above-hers-is-read-after-her-own)
  (a-yes-for-one-act-lets-nobody-see)
  (the-reviews-probe-of-the-read-path-written-again)
  (a-step-that-joins-or-leaves-a-source-owes-the-whole-answer)
  (every-order-of-the-callbacks)
  (the-size-of-a-run-and-the-order-of-its-keys)
  (who-reads-a-lanes-acts)
  (a-read-of-acts-is-bounded)
  (the-rounds-of-a-read-of-acts)
  (the-words))

(defn -main
  "Runs the proof and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [{:keys [fail error] :as result} (run-tests 'softland.store.read-path-pure-proof)]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
