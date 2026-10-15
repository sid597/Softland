(ns softland.store.read-path-pure-proof
  "The proof of the read path's pure namespaces: the form of a query and its
  check, the layers, the sources, the answer, the steps of a live read, and
  the words. It needs no cluster and no Rama. One command, from the
  repository root:

    clojure -M:store -m softland.store.read-path-pure-proof

  It runs on the alias that holds Clojure alone, so a pass also holds that
  the five namespaces under `softland/store/read_path/` load with no Rama.
  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status
  that is not zero when a check fails.

  Approach. The tables are a value here: a map from a gate and a table to a
  sorted map from a row's key to the row, in the order of
  `softland.store.record.rows/before?`. A small function of the proof's own,
  `write`, stands for a gate: it puts the rows of one accepted act in those
  maps, with the rows of `softland.store.record.rows`, and keeps the order
  by value of a marked key as the store's page says a gate keeps it. A
  source is read from the maps by `table-value`. No task is played: a
  source's routing id is not looked at when it is read, so a source of the
  base gives the same rows under every home, as the store's tables do when
  every task holds the base's rows. That every task holds them is the proof
  on the cluster's.

  The player. For a live read the proof holds a world: the tables; the
  writes that are still to come; for each push that the read holds, the
  value that the push holds, the changes that it has not taken, and the
  callbacks that are not handled; the read's sources and its mark; and the
  asker's screen, which is the first answer, then each `{:changed}` put in
  and each `{:anew}` in the place of all. One move changes one thing: a
  write; a push takes its next value; a callback is handled; a push that
  the read asked for is served with the table as it is then; its arrival is
  handled. A callback and an arrival are handled by the functions that
  `softland.store.read-path` calls for them, in its order:
  `softland.store.read-path.sources/named`, `reading` and `moves`, and
  `softland.store.read-path.answer/meanings`, `after-change` and
  `after-arrival`. The player plays EVERY order of the moves, and holds, at
  each end, the screen against what a read asked once gives from the tables
  as they are then. That Rama's client delivers in these ways and no other
  is not this proof's: it is what the probes of this build ran
  (reads/plan-probes-read-path, reads/plan-check-probes-read-path).

  State. The proof owns one atom, the lines that it showed. Its row makers
  remember each row that they made, so that one write makes one row object
  in every order of the moves and two worlds that hold the same rows are
  equal.

  What it does not reach is in the folder's README: anything that needs
  Rama."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [softland.store.act :as act]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.door.words :as door-words]
            [softland.store.gate.first-act :as first-act]
            [softland.store.read-path.answer :as answer]
            [softland.store.read-path.layers :as layers]
            [softland.store.read-path.query :as query]
            [softland.store.read-path.sources :as sources]
            [softland.store.read-path.words :as words]
            [softland.store.record.rows :as rows])
  (:import [clojure.lang PersistentQueue]
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

(defrecord Thing [asks asker layers])

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

(def ^:private fact-row*
  "Makes the row of a fact as a gate would keep it, and remembers it. Takes
  the layer, who wrote it, the entity, the key, the value (nil for a
  retract) and the stamp. The fact is given to
  `softland.store.record.rows/fact-row` in its one form on arrival: with the
  one text of its value in its normal form, which the row keeps, and with
  the value itself under a store key alone. The row holds the time on its
  act (`time-of`), and the texts of who wrote it and of a first permission
  of that who in the layer."
  (memoize
   (fn [layer who e k v stamp]
     (let [fact {:e e :k k :replaces nil
                 :v (when (declaration/store-key? k) v)
                 :text (when (some? v) (text/text (act/normal-value v)))}]
       (rows/fact-row fact (UUID. 0 (long stamp)) 0 stamp (time-of stamp) (text/text who) (text/text [who layer layer]))))))

(def ^:private marks-row*
  "Makes the row of a layer's marked keys, with the stamp since which each
  key's order is level, and remembers it."
  (memoize
   (fn [layer since-by-key stamp]
     (rows/with-level-since
      (fact-row* layer :maker layer :softland.store/marked-keys (set (keys since-by-key)) stamp)
      since-by-key))))

(def ^:private by-value-row*
  "Makes the row of a fact in the order by value, with the time on its act
  (`time-of`), and remembers it."
  (memoize
   (fn [layer who stamp]
     (rows/by-value-row (text/text who) (text/text [who layer layer]) (UUID. 0 (long stamp)) 0 stamp (time-of stamp)))))

(defn- put
  "Puts a row under a key of one table."
  [tables gate table-name k row]
  (assoc tables [gate table-name] (assoc (table tables gate table-name) k row)))

(defn- write
  "Stands for a gate: puts the rows of one accepted act in the tables. Takes
  the tables and a write, one of:

    [:make layer stamp]                     the layer's depot fact
    [:setting layer word stamp]             who may see the layer
    [:permission layer holder stamp]        a first permission for a holder,
                                            [holder layer layer]. A place
                                            that holds a value takes no
                                            other: the write changes nothing
    [:give layer holder parent stamp]       a given permission for a holder,
                                            [holder layer layer parent],
                                            beneath the permission id
                                            `parent`; taken as a first one is
    [:end layer holder stamp]               the end of the holder's right: a
                                            retract at her place
    [:list layer layers stamp]              what the layer builds on
    [:marks layer keys stamp]               the layer's marked keys. A key
                                            that was marked keeps its stamp;
                                            a new one gets this stamp
    [:fact layer who entity key value stamp] a fact; nil is a retract. For
                                            a key that the layer marks, the
                                            row of the value before leaves
                                            the order by value, and a value
                                            whose stamp is later than the
                                            key's stamp enters it

  What must hold: each stamp is later than the stamps before it in its
  layer."
  [tables [kind layer & more]]
  (let [gate (first-act/depot-at-making layer)
        facts (table tables gate :facts)
        own (fn [k v stamp] (put tables gate :facts (rows/place-key layer layer k) (fact-row* layer :maker layer k v stamp)))]
    (case kind
      :make (let [[stamp] more] (own :softland.store/depot gate stamp))
      :setting (let [[word stamp] more] (own :softland.store/who-may-see word stamp))
      :list (let [[listed stamp] more] (own :softland.store/builds-on listed stamp))
      (:permission :give)
      (let [[holder parent stamp] (if (= :give kind) more [(first more) nil (second more)])
            k (rows/place-key layer holder :softland.store/permission)
            given (if parent [holder layer layer parent] [holder layer layer])]
        (if (some? (:value (get facts k)))
          tables
          (put tables gate :facts k (fact-row* layer :maker holder :softland.store/permission given stamp))))
      :end (let [[holder stamp] more]
             (put tables gate :facts (rows/place-key layer holder :softland.store/permission)
                  (fact-row* layer :maker holder :softland.store/permission nil stamp)))
      :marks (let [[marked stamp] more
                   before (rows/marked-keys-of (get facts (rows/place-key layer layer :softland.store/marked-keys)))
                   since-by-key (into {} (map (fn [k] [k (get before k stamp)])) marked)]
               (put tables gate :facts (rows/place-key layer layer :softland.store/marked-keys)
                    (marks-row* layer since-by-key stamp)))
      :fact (let [[who e k v stamp] more
                  place (rows/place-key layer e k)
                  before (get facts place)
                  since (get (rows/marked-keys-of (get facts (rows/place-key layer layer :softland.store/marked-keys))) k)
                  tables (put tables gate :facts place (fact-row* layer who e k v stamp))
                  order (table tables gate :facts-by-value)
                  order (if (and since before (some? (:value before)))
                          (dissoc order (rows/by-value-key layer k since (rows/value-digest (rows/plain-bytes-of before)) e))
                          order)
                  order (if (and since (some? v) (< since stamp))
                          (assoc order (rows/by-value-key layer k since (rows/value-digest-of v) e)
                                 (by-value-row* layer who stamp))
                          order)]
              (assoc tables [gate :facts-by-value] order)))))

(defn- table-value
  "Takes the tables and a source. Gives what Rama would give for the source
  now: the row or nil for a source of one row, the rows of the run for the
  two kinds of run. The routing id is not looked at: the proof plays no
  task."
  [tables source]
  (let [t (table tables (:gate source) (:table source))
        [kind a b] (:path source)]
    (if (= :row kind)
      (get t a)
      (into (sorted-map-by by-code-point) (subseq t >= a < b)))))

(defn- play-once
  "Plays a read asked once on the tables: what is wanted, fetch what is
  missing from the tables, again. Takes the tables and a query in its one
  form. Gives {:rounds how many rounds fetched something, :calls how many
  calls each round would make, :first-missing the sources of the first
  round, :reading what `reading` gave at the end, :known what was fetched,
  :answer the answer}; `:answer` is `:did-not-settle` when nine rounds were
  not enough."
  [tables query]
  (loop [round 0 known {} calls [] first-missing nil]
    (let [reading (sources/reading query known :once numbers)
          missing (:missing reading)]
      (cond
        (empty? missing) {:rounds round :calls calls :first-missing first-missing
                          :reading reading :known known
                          :answer (answer/whole query reading known)}
        (< 8 round) {:rounds round :calls calls :first-missing first-missing
                     :reading reading :known known :answer :did-not-settle}
        :else (recur (inc round)
                     (into known (map (fn [source] [source (table-value tables source)])) missing)
                     (conj calls (count (sources/calls-for-once missing numbers)))
                     (or first-missing missing))))))

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

(defn- settle
  "The first line of a step, as `softland.store.read-path/settle!` makes it:
  what the read wants on the values that its pushes hold now; join what is
  wanted and not held, as a push that is asked for; leave what is held and
  no longer wanted, whose push is gone with what it had not delivered; and
  `known` again. Gives [the world, {:reading :known :joined :left
  :known-before}]."
  [world]
  (loop [world world joined #{} left #{} known-before nil round 0]
    (let [known (known-of world)
          reading (sources/reading (:query world) known :live numbers (:held world))
          {:keys [join leave]} (sources/moves reading (:held world))]
      (if (or (and (empty? join) (empty? leave)) (< 8 round))
        [world {:reading reading :known known :joined joined :left left :known-before (or known-before known)}]
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
  "One callback of a push is handled, as `softland.store.read-path` handles
  it: the keys that changed; nothing for a row that is the same or a source
  that the read no longer holds; else the step."
  [world source fault]
  (let [[old new] (first (get-in world [:pushes source :pend]))
        world (update-in world [:pushes source :pend] subvec 1)
        changed (sources/named source :all old new)]
    (if (or (empty? changed) (not (contains? (:held world) source)))
      world
      (let [[world {:keys [reading known joined left known-before]}] (settle world)
            query (:query world)
            [meant-before meant-after] (answer/meanings query known source old new)]
        (after-a-step world
                      (binding [*known-before-the-leaving* known-before]
                        (answer/after-change query reading known source
                                             (sources/places-of query source changed)
                                             meant-before meant-after joined left (:owed? world)))
                      fault)))))

(defn- arrive
  "The arrival of a push that the read asked for is handled, as
  `softland.store.read-path` handles it."
  [world source fault]
  (let [world (assoc-in world [:pushes source :phase] :given)]
    (if-not (contains? (:held world) source)
      world
      (let [[world {:keys [reading known joined left]}] (settle world)]
        (after-a-step world
                      (answer/after-arrival (:query world) reading known source joined left (:owed? world))
                      fault)))))

(defn- moves-of
  "Takes a world. Gives every move that can be made now: the next write; and
  for each push, by its state, to be served, to have its arrival handled,
  to take its next value, to have its next callback handled."
  [world]
  (concat
   (when (seq (:writes world)) [[:write]])
   (mapcat (fn [[source push]]
             (let [open? (contains? #{:served :given} (:phase push))]
               (concat (when (= :asked (:phase push)) [[:serve source]])
                       (when (= :served (:phase push)) [[:arrive source]])
                       (when (and open? (seq (:queue push))) [[:take source]])
                       (when (and open? (seq (:pend push))) [[:handle source]]))))
           (:pushes world))))

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
    :arrive (arrive world source fault)))

(defn- open-live
  "Opens a live read in a world, as `softland.store.read-path/read-live`
  does: what is wanted, join what is missing and serve it from the tables
  at once, again; then the first answer is the asker's screen. Takes the
  tables, the writes that are to come and a query in its one form. Gives
  the world, with `:rounds`, how many rounds joined something, and
  `:first-joined`, the sources of the first round."
  [tables writes query]
  (loop [world {:tables tables :writes (vec writes) :query query :held #{} :pushes {} :owed? false}
         round 0
         first-joined nil]
    (let [known (known-of world)
          reading (sources/reading query known :live numbers (:held world))
          {:keys [join leave]} (sources/moves reading (:held world))]
      (if (and (empty? join) (empty? leave))
        (assoc world :screen (answer/whole query reading known) :rounds round :first-joined first-joined)
        (recur (-> world
                   (update :held #(into (reduce disj % leave) join))
                   (update :pushes #(into (reduce dissoc % leave)
                                          (map (fn [source]
                                                 [source {:phase :given :value (table-value tables source)
                                                          :queue [] :pend []}]))
                                          join)))
               (inc round)
               (or first-joined join))))))

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

(defn- layer-n
  "The id of the n-th layer of a played list, 1 the nearest."
  [n]
  (keyword (str "l" n)))

(def ^:private things
  "The things that a played fact points at: `:v` is the thing that the
  asker asks about, each other word another thing."
  {:v :canvas-1 :u :canvas-u :x :canvas-x :w :canvas-w})

(defn- points-at-world
  "A world for what points at a thing, after the check's model of the plan
  (reads/plan-check-probes-read-path/model/crossing.py). Takes how many
  layers; which of them mark the key; the facts at the start, a map from
  [layer place] to [thing, version]; the layers that the asker may see at
  the start; and the writes: `[:w layer place thing]`, a fact, and
  `[:see layer]`, a permission that is given to her. A layer that she may
  see is \"everyone\"; one that she may not is \"members\", and she holds
  no permission in it. With a first argument, a function from a layer's
  number to its id, the layers have those ids; with none they are
  `layer-n`'s."
  ([k marks facts seen writes]
   (points-at-world layer-n k marks facts seen writes))
  ([layer-n k marks facts seen writes]
   (let [tables (reduce write {}
                        (concat
                         (for [n (range 1 (inc k))] [:make (layer-n n) 1])
                         (for [n (range 1 (inc k))] [:setting (layer-n n) (if (contains? seen n) :everyone :members) 2])
                         (for [n (range 1 (inc k)) :when (get marks n)] [:marks (layer-n n) #{:parent} 3])
                         (for [[[n place] [thing version]] (sort-by (comp second val) facts)]
                           [:fact (layer-n n) :maker place :parent (get things thing) (+ 10 version)])))
         writes (map-indexed (fn [i [kind n place thing]]
                               (if (= :see kind)
                                 [:permission (layer-n n) :ann (+ 100 i)]
                                 [:fact (layer-n n) :maker place :parent (get things thing) (+ 100 i)]))
                             writes)]
     (open-live tables writes
                (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers (mapv layer-n (range 1 (inc k)))})))))

(defn- base-at
  "Takes the number of a layer of a played list, 1 the nearest. Gives a
  function from a layer's number to its id, for `points-at-world`: that
  layer is the base, a microbatch layer, and each other one is `layer-n`'s.
  With the base nearest, the read's home is the base. With the base second
  or farther, the home is a streaming layer, and the base's order by value
  and the checks of the base are read under that layer's routing id."
  [i]
  (fn [n] (if (= n i) :base (layer-n n))))

(def ^:private points-at-scenarios
  "The thirteen scenarios of the check's model, each as: what it is, the
  count of layers, which mark the key, the facts at the start, the layers
  that she may see at the start, and the writes."
  [["1. the farther order loses p while the nearer layer gets another row at p"
    2 {1 true 2 true} {[2 :p] [:v 1]} #{1 2}
    [[:w 1 :p :u] [:w 2 :p :x]]]
   ["2. a nearer layer that does not mark the key writes the same thing; then the farther order loses p"
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
   ["6. three layers, all mark: the middle loses p, the nearest gets a row, the farthest is written again"
    3 {1 true 2 true 3 true} {[2 :p] [:v 1] [3 :p] [:v 2]} #{1 2 3}
    [[:w 2 :p :u] [:w 1 :p :w] [:w 3 :p :v]]]
   ["7. three layers: the middle loses p and the farthest loses p, then the middle gets it back"
    3 {1 false 2 true 3 true} {[2 :p] [:v 1] [3 :p] [:v 2] [1 :p] [:v 3]} #{1 2 3}
    [[:w 2 :p :u] [:w 3 :p :u] [:w 2 :p :v]]]
   ["8. three layers, four writes on one place"
    3 {1 false 2 true 3 true} {[3 :p] [:v 1]} #{1 2 3}
    [[:w 2 :p :v] [:w 1 :p :v] [:w 2 :p :u] [:w 3 :p :u]]]
   ["9. a permission is given: a nearer layer joins, with a place q of its own, over a farther place p"
    2 {1 true 2 true} {[1 :q] [:v 1] [2 :p] [:v 2]} #{2}
    [[:see 1]]]
   ["10. the same, while the farther layer is written"
    2 {1 true 2 true} {[1 :q] [:v 1] [2 :p] [:v 2] [1 :p] [:u 3]} #{2}
    [[:see 1] [:w 2 :p :v] [:w 1 :q :u]]]
   ["11. a new place in the farther order, with a nearer row there already"
    2 {1 false 2 true} {[1 :p] [:v 1]} #{1 2}
    [[:w 2 :p :v] [:w 1 :p :u] [:w 2 :p :x]]]
   ["12. four layers: the second loses p, so checks are asked in the second and the third; then the third gets p"
    4 {1 true 2 true 3 true 4 true} {[2 :p] [:v 1] [4 :p] [:v 2]} #{1 2 3 4}
    [[:w 2 :p :u] [:w 3 :p :v]]]
   ["13. a nearer layer joins while a check of a new place is on its way"
    3 {1 true 2 false 3 true} {[1 :q] [:v 1] [2 :p] [:u 2]} #{2 3}
    [[:w 3 :p :v] [:see 1] [:w 2 :p :v]]]])

(defn- lists-up-to
  "Every list of 1 to n of the things given, in every order, with
  repeats."
  [n options]
  (mapcat (fn [length]
            (reduce (fn [lists _] (for [l lists o options] (conj l o))) [[]] (range length)))
          (range 1 (inc n))))

(defn- every-small-points-at
  "Every list of up to three writes on one place in two layers, the nearer
  marking the key or not, from four starts: the check's model's small
  lists. Takes the fault that the player is to make, or nil. Gives {:lists
  :states :wrong the lists that have an order which ends wrong}."
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
  "A world for one place, or one entity, through two layers, after the
  check's second model (model/standing.py). The nearer layer has a setting
  and, for the asker, a permission or none; the farther is \"everyone\" and
  holds a fact at the place. Takes the nearer layer's setting at the start,
  whether she holds a permission there, the writes, and what is asked. A
  write is `[:setting word]`, `[:permission]`, `[:near]` (a fact of the
  nearer layer at the place) or `[:far]`. With a sixth argument, who asks:
  `{:asker :ann}` when none is given, or `{:not-signed-in true}`."
  ([setting permission? writes asks] (standing-world setting permission? writes asks {:asker :ann}))
  ([setting permission? writes asks who]
   (let [tables (reduce write {}
                        (concat [[:make :near 1] [:make :far 1]
                                 [:setting :near setting 2] [:setting :far :everyone 2]]
                                (when permission? [[:permission :near :ann 3]])
                                [[:fact :far :maker :p :text "f0" 11]]))
         writes (map-indexed (fn [i [kind word]]
                               (case kind
                                 :setting    [:setting :near word (+ 100 i)]
                                 :permission [:permission :near :ann (+ 100 i)]
                                 :near       [:fact :near :maker :p :text (str "n" i) (+ 100 i)]
                                 :far        [:fact :far :maker :p :text (str "f" i) (+ 100 i)]))
                             writes)]
     (open-live tables writes (q (merge {:asks asks :layers [:near :far]} who))))))

(defn- base-world
  "A world for one place, or one entity, through an asker's own layer and
  the base. Her own layer, `:mine`, is the read's home: a streaming layer
  that is \"everyone\" and holds nothing at the start. The base, a
  microbatch layer, has a setting and, for the asker, a permission or none,
  and holds a fact at the place. So each source of the base, its own rows,
  her permission and the fact, is read under the routing id of her own
  layer. Takes the base's setting at the start, whether she holds a
  permission there, the writes, and what is asked. A write is `[:setting
  word]` and `[:permission]`, of the base; `[:mine]`, a fact of her own
  layer at the place; or `[:base]`, a fact of the base there."
  [setting permission? writes asks]
  (let [tables (reduce write {}
                       (concat [[:make :mine 1] [:make :base 1]
                                [:setting :mine :everyone 2] [:setting :base setting 2]]
                               (when permission? [[:permission :base :ann 3]])
                               [[:fact :base :maker :p :text "b0" 11]]))
        writes (map-indexed (fn [i [kind word]]
                              (case kind
                                :setting    [:setting :base word (+ 100 i)]
                                :permission [:permission :base :ann (+ 100 i)]
                                :mine       [:fact :mine :maker :p :text (str "m" i) (+ 100 i)]
                                :base       [:fact :base :maker :p :text (str "b" i) (+ 100 i)]))
                            writes)]
    (open-live tables writes (q {:asks asks :asker :ann :layers [:mine :base]}))))

(defn- every-small-standing
  "Every list of up to n writes of the setting, the permission and the two
  facts, from four starts. Takes a world, `standing-world` or `base-world`,
  with the two words of its facts' writes; n; what is asked; and the fault
  that the player is to make, or nil. Gives {:lists :states :wrong}. With
  two arguments more, the words of the setting that the starts and the
  writes take (`[:everyone :members]` when none are given), and who asks,
  for `standing-world` alone: a start for each word and each permission."
  ([world names n asks fault]
   (every-small-standing world names n asks fault [:everyone :members] nil))
  ([world [nearer farther] n asks fault settings who]
   (let [options (concat (map (fn [word] [:setting word]) settings) [[:permission] [nearer] [farther]])
         played (for [setting settings
                      permission? [false true]
                      writes (lists-up-to n options)]
                  (assoc (every-order (if who
                                        (world setting permission? writes asks who)
                                        (world setting permission? writes asks))
                                      fault)
                         :is [setting permission? writes]))]
     {:lists (count played)
      :states (reduce + (map :states played))
      :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))})))

;; ---------------------------------------------- the layers of the plan's traces

(def ^:private alice "Alice's query parts: who asks." {:asker :alice})

(def ^:private traces
  "The tables of the plan's traces. `:alice-notes` and `:team` are streaming
  and \"members\"; `:base` is microbatch and \"everyone\". Alice holds a
  permission in the first two, Bob in `:team` only. `:alice-notes` builds
  on `:team` and `:base`. `:team` marks `:parent` since the stamp 700, the
  base since 300; `:alice-notes` marks nothing."
  (reduce write {}
          [[:make :alice-notes 1] [:make :team 1] [:make :base 1]
           [:permission :alice-notes :alice 2] [:permission :team :alice 2] [:permission :team :bob 3]
           [:list :alice-notes [:team :base] 4]
           [:marks :team #{:parent} 700] [:marks :base #{:parent} 300]
           [:fact :base :system :note-41 :text "from the base" 310]
           [:fact :base :system :note-41 :title "a title" 311]
           [:fact :base :system :block-1 :parent :canvas-1 320]
           [:fact :base :system :block-2 :parent :canvas-1 321]
           [:fact :team :alice :block-3 :parent :canvas-1 710]
           [:fact :team :alice :block-1 :parent :canvas-2 711]
           [:fact :team :bob :note-41 :text "from the team" 712]]))

(defn- live-rounds
  "Opens a live read of a query on the traces' tables. Gives how many rounds
  joined something, and how many pushes the read holds."
  [query]
  (let [world (open-live traces [] query)]
    [(:rounds world) (count (:held world))]))

;; ---------------------------------------------------------------- the checks

(deftest the-form-of-a-query
  (let [refuses (fn [thing] (let [{:keys [refuse at]} (query/check thing)] [refuse at]))
        good {:asks [:place :note-41 :text] :asker :alice :layers [:team]}
        forms {"a place"                 {:asks [:place :note-41 :text] :asker :alice :layers [:team]}
               "an entity"               {:asks [:entity :note-41] :asker :alice :from-layer :team}
               "a run of keys"           {:asks [:run [:e :a] [:e :m]] :asker :alice :layers [:team]}
               "a run of entities"       {:asks [:run [:e1] [:e2]] :asker :alice :layers [:team]}
               "a run from an entity on" {:asks [:run [:e] :last] :asker :alice :layers [:team]}
               "a whole layer"           {:asks [:run :first :last] :not-signed-in true :layers [:base]}
               "a run to a place"        {:asks [:run :first [:e :k]] :asker (UUID. 1 2) :layers '(:team)}
               "what points at a thing"  {:asks [:points-at :parent (UUID. 0 7)] :asker :alice :layers [:team]
                                          :with #{:writer :permission} :stores-own-facts true}
               "a place, with each part of a row" {:asks [:place :note-41 :text] :asker :alice :layers [:team]
                                                   :with #{:writer :permission :time}}}
        faults [["no map"                          5                                                [:not-a-query nil]]
                ["a record"                        (->Thing [:place :note-41 :text] :alice [:team]) [:not-a-query nil]]
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
                ["an asker that is no id"          (assoc good :asker "alice")                      [:asker-is-no-id :asker]]
                ["no asker"                        (dissoc good :asker)                             [:no-one-asker :asker]]
                ["an asker, and not signed in"     (assoc good :not-signed-in true)                 [:no-one-asker :asker]]
                ["not signed in, said with false"  (assoc (dissoc good :asker) :not-signed-in false) [:no-one-asker :not-signed-in]]
                ["layers that are no list"         (assoc good :layers :team)                       [:layers-not-a-list-of-layers :layers]]
                ["a layer that is no id"           (assoc good :layers [:team "base"])              [:layers-not-a-list-of-layers :layers]]
                ["a start layer that is no id"     (assoc (dissoc good :layers) :from-layer 7)      [:from-layer-is-no-layer :from-layer]]
                ["no layers"                       (dissoc good :layers)                            [:layers-not-named-one-way :layers]]
                ["a list and a start layer"        (assoc good :from-layer :team)                   [:layers-not-named-one-way :layers]]
                ["an empty list"                   (assoc good :layers [])                          [:layers-not-named-one-way :layers]]
                ["66 layers"                       (assoc good :layers (mapv #(keyword (str "l" %)) (range 66))) [:too-many-layers :layers]]
                ["a part of a row that is not known" (assoc good :with #{:value})                 [:part-of-a-row-not-known :with]]
                ["a :with that is no set"          (assoc good :with [:writer])                     [:part-of-a-row-not-known :with]]
                ["an asker of 300 characters"      (assoc good :asker (keyword (apply str (repeat 300 "a")))) [:id-over-the-limit :asker]]
                ["a layer of 300 characters"       (assoc good :layers [(keyword (apply str (repeat 300 "l")))]) [:id-over-the-limit :layers]]
                ["an entity of 300 characters"     (assoc good :asks [:entity (keyword (apply str (repeat 300 "e")))]) [:id-over-the-limit :asks]]
                ["a key of 300 characters"         (assoc good :asks [:place :e (keyword (apply str (repeat 300 "k")))]) [:id-over-the-limit :asks]]
                ["a run whose ends are out of order" (assoc good :asks [:run [:e2] [:e1]])         [:run-ends-out-of-order :asks]]
                ["a run that starts at :last"      (assoc good :asks [:run :last [:e]])             [:run-ends-out-of-order :asks]]
                ["a run that ends at :first"       (assoc good :asks [:run [:e] :first])            [:run-ends-out-of-order :asks]]
                ["a run whose end holds a letter outside the first plane" (assoc good :asks [:run [:e] [(keyword (str "x" (String. (Character/toChars 0x1D49C))))]]) [:run-end-not-ordered :asks]]
                ["a run whose end holds a letter from U+E000 on" (assoc good :asks [:run [(keyword "x\uFB00") :k] :last]) [:run-end-not-ordered :asks]]
                ["a place under a store key, not asked for" (assoc good :asks [:place :team :softland.store/who-may-see]) [:store-key-not-asked-for :asks]]
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
    (check "the one form of a query: each part there, an asker of nil for a person who is not signed in, one of the two ways of naming layers nil, a set, true or false"
           {:asks [:run :first :last] :asker nil :not-signed-in true :layers [:base] :from-layer nil :with #{} :stores-own-facts false}
           (q (get forms "a whole layer")))
    (check "a :with that asks for each of the three parts of a row, the writer, the permission and the time, is a known query, and its one form keeps the three"
           #{:writer :permission :time} (:with (q (get forms "a place, with each part of a row"))))
    (check "a list of layers that is no vector, and a thing that is a UUID, come out in the one form"
           [[:team] true]
           (let [one (q (get forms "a run to a place"))] [(:layers one) (vector? (:layers one))]))
    (doseq [[what thing expected] faults]
      (check (str "a query is turned away: " what) expected (refuses thing)))
    (check "each of the check's words was given, and each is declared" [query/reasons true]
           [given (every? query/reasons given)])
    (check "the check never throws: a thing that throws when it is read, of two kinds, and things that are no map"
           [:not-a-query :not-a-query :not-a-query :not-a-query :not-a-query]
           (mapv (comp :refuse query/check) [thrower throws nil "a query" [:place :e :k]]))
    (check "a list of layers with no end is turned away after 66, and its layers are not read"
           [:too-many-layers :layers]
           (refuses (assoc good :layers (map (fn [i] (if (< i 70) (keyword (str "l" i)) (throw (ex-info "read past the bound" {}))))
                                             (iterate inc 0)))))
    (check "the numbers: a default for each that is left out or is none; a number that is given is taken"
           [query/default-numbers
            (assoc query/default-numbers :wait-ms 300 :run-start-places 4 :max-pushes-under-one-routing-id 5)
            query/default-numbers]
           [(query/numbers nil)
            (query/numbers {:wait-ms 300 :run-start-places 4 :max-places-of-one-run 0 :max-checks-of-one-read "many"
                            :max-pushes-under-one-routing-id 5 :max-checks-of-one-live-read -1 :more 1})
            (query/numbers 7)])
    (check "the read path's seven numbers, written out a second time: a read kept live may need 250 checks of nearer layers where one asked once may need 40,000, and the read path holds 900 pushes on one table under one routing id, under the 1,000 that Rama opens on one table on one task"
           {:run-start-places 256 :max-places-of-one-run 20000 :max-checks-of-one-read 40000 :max-checks-of-one-live-read 250
            :max-pushes-under-one-routing-id 900 :max-paths-in-one-call 2000 :wait-ms 10000}
           query/default-numbers)
    (shown "the form of a query:" (count forms) "queries are taken, one for each word and each kind of end;"
           (count faults) "faults are each turned away with a word and the part,"
           (count given) "different words, every word that the declaration holds:" (pr-str (sort given)))
    (shown "the check never throws: a thing that throws when it is read gives :not-a-query, and a list of layers with no end gives :too-many-layers after 66 are counted")))

(deftest a-word-is-declared-in-three-places
  (check "the words of a query, what each reads in one layer, and how its rows are put together have the same keys"
         [#{:place :entity :run :points-at} true true]
         [(set (keys query/words)) (= (set (keys query/words)) (set (keys sources/of-word)))
          (= (set (keys query/words)) (set (keys answer/of-word)))])
  (shown "a word of a query is declared in three maps or in none:" (pr-str (sort (keys query/words)))))

(deftest the-layers
  (let [own (fn [layer] (table-value traces (sources/own-rows-source layer layer)))
        permission (fn [layer who] (table-value traces (sources/permission-source layer layer who)))
        ;; her right, from her row: each permission of the traces is a first one, with nothing above it
        right-of (fn [layer who] (layers/right layer who (permission layer who) {}))
        levels-of (fn [layer who] (layers/levels-above layer who (permission layer who) {}))
        with-setting-in (fn [layer word] (table-value (write traces [:setting layer word 900]) (sources/own-rows-source layer layer)))
        from (q {:asks [:place :note-41 :text] :asker :alice :from-layer :alice-notes})
        points (q {:asks [:points-at :parent :canvas-1] :asker :bob :layers [:team]})
        with-setting (fn [word stamp] (table-value (write traces [:setting :team word stamp]) (sources/own-rows-source :team :team)))
        with-list (fn [listed] (table-value (write traces [:list :alice-notes listed 900]) (sources/own-rows-source :alice-notes :alice-notes)))
        with-mark (fn [marked] (table-value (write traces [:marks :team marked 900]) (sources/own-rows-source :team :team)))]
    (check "the layers of a list: each once, at its first place"
           [:alice-notes :team :nowhere :base]
           (layers/in-order (q {:asks [:place :e :k] :asker :bob :layers [:alice-notes :team :nowhere :team :base]}) nil nil))
    (check "from a layer: the layer, then the layers of its fact, in the fact's order, and no further"
           [:alice-notes :team :base] (layers/in-order from (own :alice-notes) :yes))
    (check "a list that names its own layer and a layer twice: each once, the start layer first"
           [:alice-notes :base :team] (layers/in-order from (with-list [:base :alice-notes :team :base]) :yes))
    (check "a start layer with no fact: the layer alone" [:team] (layers/in-order (assoc from :from-layer :team) (own :team) :yes))
    (check "a start layer whose rows, or her standing in it, are not known yet: nothing can be said"
           [nil nil] [(layers/in-order from nil :not-known-yet) (layers/in-order from (own :alice-notes) :not-known-yet)])
    (check "a start layer that she may not see, or that is not made: the layer alone, and its list is not used"
           [[:alice-notes] [:alice-notes]]
           [(layers/in-order from (own :alice-notes) :may-not-see) (layers/in-order from {} :not-made)])
    (check "the home of a read is the layer that it starts from: the first layer of a list, also of a list that names it a second time, and of a list of one; the start layer of a query that takes its list from a layer"
           [:alice-notes :team :base :alice-notes]
           (mapv #(layers/home (q (merge {:asks [:place :e :k] :asker :bob} %)))
                 [{:layers [:alice-notes :team :base]} {:layers [:team :base :team]} {:layers [:base]}
                  {:from-layer :alice-notes}]))
    (check "the home is read from the query alone: it is the same for each word of a query, for a person who is not signed in, and for a start layer that is not made or that she may not see, where the list is the layer alone"
           [[:team :team :team :team] :team [:nowhere [:nowhere]] [:alice-notes [:alice-notes]]]
           [(mapv #(layers/home (q {:asks % :asker :bob :layers [:team :base]}))
                  [[:place :e :k] [:entity :e] [:run :first :last] [:points-at :parent :canvas-1]])
            (layers/home (q {:asks [:place :e :k] :not-signed-in true :layers [:team :base]}))
            (let [nowhere (q {:asks [:place :e :k] :asker :bob :from-layer :nowhere})]
              [(layers/home nowhere) (layers/in-order nowhere {} :not-made)])
            (let [bob-from (assoc from :asker :bob)]
              [(layers/home bob-from) (layers/in-order bob-from (own :alice-notes) :may-not-see)])])
    (check "the standing: the nine lines of its table"
           [:not-known-yet :not-made :yes :may-not-see :yes :may-not-see :not-known-yet :yes :may-not-see]
           [(layers/standing :team nil :not-known-yet true)
            (layers/standing :nowhere (own :nowhere) :none true)
            (layers/standing :base (own :base) :not-known-yet true)
            (layers/standing :team (with-setting-in :team :signed-in) :not-known-yet false)
            (layers/standing :team (with-setting-in :team :signed-in) :not-known-yet true)
            (layers/standing :team (own :team) :not-known-yet false)
            (layers/standing :team (own :team) :not-known-yet true)
            (layers/standing :team (own :team) (right-of :team :bob) true)
            (layers/standing :alice-notes (own :alice-notes) (right-of :alice-notes :bob) true)])
    (check "who may see a layer reads three words, and a word that it does not know as members"
           [:everyone :signed-in :members :members]
           (mapv #(layers/who-may-see (with-setting-in :team %) :team) [:everyone :signed-in :members :owner]))
    (check "what no fact reads as: a layer with no setting is members, the base is everyone, a list is nothing, no key is marked"
           [:members :everyone [] nil]
           [(layers/who-may-see (own :team) :team) (layers/who-may-see (own :base) :base)
            (layers/builds-on (own :team) :team) (layers/marked-since (own :alice-notes) :alice-notes :parent)])
    (check "a person who is not signed in sees the base and no members layer; the system is a who like any other"
           [:yes :may-not-see :may-not-see]
           [(layers/standing :base (own :base) :not-known-yet false)
            (layers/standing :team (own :team) :not-known-yet false)
            (layers/standing :team (own :team) (right-of :team :system) true)])
    (check "a setting that is written again with the same word: the row differs, and its meaning does not"
           [false true]
           (let [again (with-setting :members 900)]
             [(rows/same-row? (get (own :team) (rows/place-key :team :team :softland.store/who-may-see))
                              (get again (rows/place-key :team :team :softland.store/who-may-see)))
              (= (layers/meaning points :team (with-setting :members 800) (right-of :team :bob) (levels-of :team :bob) true)
                 (layers/meaning points :team again (right-of :team :bob) (levels-of :team :bob) true))]))
    (check "a list, and a mark of the asked key, that are written in a layer that she may not see mean the same before and after"
           [true true]
           (let [carol-from (assoc from :asker :carol)
                 carol-points (assoc points :asker :carol)]
             [(= (layers/meaning carol-from :alice-notes (own :alice-notes) :none nil true)
                 (layers/meaning carol-from :alice-notes (with-list [:base]) :none nil true))
              (= (layers/meaning carol-points :team (own :team) :none nil true)
                 (layers/meaning carol-points :team (with-mark #{}) :none nil true))]))
    (check "for an asker who may see the layer, a list that is written again and a mark that is taken off mean another thing"
           [false false]
           [(= (layers/meaning from :alice-notes (own :alice-notes) (right-of :alice-notes :alice) (levels-of :alice-notes :alice) true)
               (layers/meaning from :alice-notes (with-list [:base]) (right-of :alice-notes :alice) (levels-of :alice-notes :alice) true))
            (= (layers/meaning points :team (own :team) (right-of :team :bob) (levels-of :team :bob) true)
               (layers/meaning points :team (with-mark #{}) (right-of :team :bob) (levels-of :team :bob) true))])
    (check "a setting that goes to members for an asker who holds a permission means another thing, though her standing stays yes"
           [false :yes :yes]
           (let [everyone (with-setting :everyone 800)
                 members (with-setting :members 900)]
             [(= (layers/meaning points :team everyone (right-of :team :bob) (levels-of :team :bob) true)
                 (layers/meaning points :team members (right-of :team :bob) (levels-of :team :bob) true))
              (layers/standing :team everyone (right-of :team :bob) true)
              (layers/standing :team members (right-of :team :bob) true)]))
    (check "what an answer says of a layer that it left out: the layer and why, and nothing else"
           {:layer :alice-notes :why :may-not-see} (layers/left-out :alice-notes :may-not-see))
    (shown "the layers: a list gives each layer once at its first place; from a layer the read looks in the layer and then in the layers of its own fact, and no further; a start layer that she may not see gives itself alone")
    (shown "the home of a read is the layer that it starts from, read from the query alone: the first layer of :layers, or :from-layer, whatever is asked, whoever asks and whatever the tables hold of that layer")
    (shown "the standing: the nine lines of its table hold; who may see a layer is one of three words; a layer with no setting reads as members, the base as everyone; a row that is written again with the same word, and a list or a mark of a layer that she may not see, mean the same before and after")))

(deftest a-step-that-joins-or-leaves-a-source-owes-the-whole-answer
  (let [;; a fact's step that finds the new setting first and joins her permission
        world (standing-world :everyone true [[:setting :members] [:near]] [:place :p :text])
        after-writes (-> world (move [:write] nil) (move [:write] nil))
        setting (sources/own-rows-source :near :near)
        fact (sources/place-source :near :near :p :text)
        both-taken (-> after-writes (move [:take setting] nil) (move [:take fact] nil))
        fact-first (move both-taken [:handle fact] nil)
        permission (sources/permission-source :near :near :ann)
        given-her (-> fact-first (move [:serve permission] nil) (move [:arrive permission] nil))
        ;; a step that leaves an order whose mark was taken off
        marked (reduce write {} [[:make :l1 1] [:setting :l1 :everyone 2] [:marks :l1 #{:parent} 3]
                                 [:fact :l1 :maker :p :parent :canvas-1 11]])
        open (open-live marked [[:marks :l1 #{} 100]] (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers [:l1]}))
        own (sources/own-rows-source :l1 :l1)
        unmarked (-> open (move [:write] nil) (move [:take own] nil) (move [:handle own] nil))]
    (check "a fact's step that sees the new setting first joins her permission, tells nothing of its place, and owes"
           [true true (:screen world)]
           [(contains? (:held fact-first) permission) (:owed? fact-first) (:screen fact-first)])
    (check "when her permission is given, the whole answer comes, with the fact in it, and the mark is taken off"
           [false "n1" :near]
           [(:owed? given-her) (get-in given-her [:screen :place :value]) (get-in given-her [:screen :place :layer])])
    (check "a step that leaves an order whose mark was taken off gives the whole answer anew, with not-marked and no place"
           [false {} [{:layer :l1 :not-marked true}] 1]
           [(:owed? unmarked) (get-in unmarked [:screen :places]) (get-in unmarked [:screen :orders]) (count (:held unmarked))])
    (shown "a step that joins or leaves a source that is no check owes the whole answer: a fact's step that joins her permission tells nothing and owes, and the arrival of her permission gives the answer with the fact; a step that leaves an order gives the answer anew with :not-marked")))

(deftest the-sources
  (let [digest (rows/value-digest-of :canvas-1)
        own-rows (fn [layer] (table-value traces (sources/own-rows-source layer layer)))
        ;; what a word reads in one layer, for a read whose home is `home`
        of (fn [asks home layer]
             ((get sources/of-word (first asks)) (q (merge alice {:asks asks :layers (vec (distinct [home layer]))}))
              layer (own-rows layer)))
        ;; every source of a layer that a read asked once came to read
        read-in (fn [query layer] (set (filter #(= layer (:layer %)) (keys (:known (play-once traces query))))))
        asks-of-each-word [[:place :note-41 :text] [:entity :note-41] [:run [:note-41 :a] [:note-41 :z]]
                           [:run [:block-1] [:note-9]] [:run :first :last] [:points-at :parent :canvas-1]]]
    (check "the layer's own rows: one run of the layer's own entity under the store's namespace. A streaming layer's routing id is the layer, whatever the home; the base's is the home"
           [{:gate :streaming :table :facts :layer :team :routing-id :team
             :path [:own-rows ":team :team :softland.store/" ":team :team :softland.store0"]}
            {:gate :microbatch :table :facts :layer :base :routing-id :team
             :path [:own-rows ":base :base :softland.store/" ":base :base :softland.store0"]}
            {:gate :microbatch :table :facts :layer :base :routing-id :base
             :path [:own-rows ":base :base :softland.store/" ":base :base :softland.store0"]}]
           [(sources/own-rows-source :alice-notes :team) (sources/own-rows-source :team :base)
            (sources/own-rows-source :base :base)])
    (check "her permission: one row, on its holder; in the base under the home"
           [{:gate :streaming :table :facts :layer :team :routing-id :team :path [:row ":team :alice :softland.store/permission"]}
            {:gate :microbatch :table :facts :layer :base :routing-id :team
             :path [:row ":base :alice :softland.store/permission"]}
            {:gate :microbatch :table :facts :layer :base :routing-id :base
             :path [:row ":base :alice :softland.store/permission"]}]
           [(sources/permission-source :alice-notes :team :alice) (sources/permission-source :team :base :alice)
            (sources/permission-source :base :base :alice)])
    (check "a place: the layer routes a streaming layer, the home a microbatch layer, and never the entity"
           [[{:gate :streaming :table :facts :layer :team :routing-id :team :path [:row ":team :note-41 :text"]}]
            [{:gate :microbatch :table :facts :layer :base :routing-id :team :path [:row ":base :note-41 :text"]}]
            [{:gate :microbatch :table :facts :layer :base :routing-id :base :path [:row ":base :note-41 :text"]}]]
           [(of [:place :note-41 :text] :team :team) (of [:place :note-41 :text] :team :base)
            (of [:place :note-41 :text] :base :base)])
    (check "an entity: one run, under the home in the base"
           [[{:gate :streaming :table :facts :layer :team :routing-id :team :path [:run ":team :note-41 " ":team :note-41!"]}]
            [{:gate :microbatch :table :facts :layer :base :routing-id :team :path [:run ":base :note-41 " ":base :note-41!"]}]
            [{:gate :microbatch :table :facts :layer :base :routing-id :base :path [:run ":base :note-41 " ":base :note-41!"]}]]
           [(of [:entity :note-41] :team :team) (of [:entity :note-41] :team :base) (of [:entity :note-41] :base :base)])
    (check "a run of keys of one entity, a run across entities and a whole layer are each one source in the base, under the home; in a streaming layer each is one source under the layer"
           [[[:team] [[:run ":base :note-41 :a" ":base :note-41 :z "]]]
            [[:team] [[:run ":base :a " ":base :b!"]]]
            [[:team] [[:run ":base " ":base!"]]]
            [[:base] [[:run ":base :a " ":base :b!"]]]
            [[:team] [[:run ":team :a " ":team :b!"]]]]
           (mapv (fn [[asks home layer]] ((juxt #(mapv :routing-id %) #(mapv :path %)) (of asks home layer)))
                 [[[:run [:note-41 :a] [:note-41 :z]] :team :base]
                  [[:run [:a] [:b]] :team :base]
                  [[:run :first :last] :team :base]
                  [[:run [:a] [:b]] :base :base]
                  [[:run [:a] [:b]] :alice-notes :team]]))
    (check "the ends of a run: a place is in the run that ends at it; a whole layer is a run and no table"
           [[:run ":team :e :a" ":team :e :m "] [:run ":team " ":team!"] [:run ":team :e " ":team!"]]
           (mapv #(:path (first (of % :team :team))) [[:run [:e :a] [:e :m]] [:run :first :last] [:run [:e] :last]]))
    (check "what points at a thing: the thing's run in the facts by value, by the stamp of the mark, one source; in the base under the home, and never under the value's digest; a layer that does not mark the key reads nothing"
           [[{:gate :streaming :table :facts-by-value :layer :team :routing-id :team
              :path [:run (str ":team :parent 700 " digest " ") (str ":team :parent 700 " digest "!")]}]
            [{:gate :microbatch :table :facts-by-value :layer :base :routing-id :team
              :path [:run (str ":base :parent 300 " digest " ") (str ":base :parent 300 " digest "!")]}]
            [{:gate :microbatch :table :facts-by-value :layer :base :routing-id :base
              :path [:run (str ":base :parent 300 " digest " ") (str ":base :parent 300 " digest "!")]}]
            []]
           [(of [:points-at :parent :canvas-1] :team :team) (of [:points-at :parent :canvas-1] :team :base)
            (of [:points-at :parent :canvas-1] :base :base) (of [:points-at :parent :canvas-1] :alice-notes :alice-notes)])
    (check "a check of a nearer layer at a place is the source of that place, in a streaming layer and in the base; a check of the base has the home as its routing id"
           [true true {:gate :microbatch :table :facts :layer :base :routing-id :alice-notes :path [:row ":base :block-3 :parent"]}]
           [(= (sources/place-source :team :team :block-1 :parent) (sources/check-source :team :team :block-1 :parent))
            (= (sources/place-source :team :base :block-1 :parent) (sources/check-source :team :base :block-1 :parent))
            ;; the base as a nearer layer of a read that starts from her notes: the place of the team's own order is checked in it
            (get-in (:reading (play-once traces (q (merge alice {:asks [:points-at :parent :canvas-1] :layers [:alice-notes :base :team]}))))
                    [:by-layer :base :checks [:block-3 :parent]])])
    (check "a source of a streaming layer is the same under every home: the layer's own rows, a permission, a place, a check, a run and the order by value"
           [true true true true true true]
           (mapv (fn [make] (apply = (map make [:team :alice-notes :base :nowhere])))
                 [#(sources/own-rows-source % :team)
                  #(sources/permission-source % :team :alice)
                  #(sources/place-source % :team :note-41 :text)
                  #(sources/check-source % :team :block-1 :parent)
                  #(sources/run-source % :team ":team :a " ":team :b!")
                  #(sources/by-value-source % :team :parent 700 :canvas-1)]))
    (check "every source of the base that a read asked once comes to read has the read's home as its routing id, for each word and each kind of run: through her notes and the team, through her notes with the team behind the base, through the team alone, and from the base itself"
           [#{:alice-notes} #{:alice-notes} #{:team} #{:base}]
           (mapv (fn [layers]
                   (into #{}
                         (mapcat (fn [asks] (map :routing-id (read-in (q (merge alice {:asks asks :layers layers})) :base))))
                         asks-of-each-word))
                 [[:alice-notes :team :base] [:alice-notes :base :team] [:team :base] [:base]]))
    (check "each word reads the base as one fact source, and the base's rights as two: its own rows and her permission"
           [[1 1 1 1 1 1] [[:own-rows :row] [:own-rows :row] [:own-rows :row] [:own-rows :row] [:own-rows :row] [:own-rows :row]]]
           (let [read (fn [asks]
                        (let [query (q (merge alice {:asks asks :layers [:team :base]}))
                              {:keys [reading known]} (play-once traces query)]
                          [(count (get-in reading [:by-layer :base :facts]))
                           (vec (sort (map (comp first :path)
                                           (filter #(and (= :base (:layer %)) (sources/rights-kind query %)) (keys known)))))]))]
             [(mapv (comp first read) asks-of-each-word) (mapv (comp second read) asks-of-each-word)]))
    (check "two reads with one home have equal sources for the base: a list, a longer list and a start layer whose list is the longer one; two askers with one home share the base's own rows and the place, and each has her own permission row"
           [true true 3
            #{[:own-rows ":base :base :softland.store/" ":base :base :softland.store0"] [:row ":base :note-41 :text"]}]
           (let [place {:asks [:place :note-41 :text]}
                 short (read-in (q (merge alice place {:layers [:alice-notes :base]})) :base)
                 long (read-in (q (merge alice place {:layers [:alice-notes :team :base]})) :base)
                 from (read-in (q (merge alice place {:from-layer :alice-notes})) :base)
                 hers (read-in (q (merge alice place {:layers [:team :base]})) :base)
                 his (read-in (q (merge place {:asker :bob :layers [:team :base]})) :base)]
             [(= short long) (= long from) (count long)
              (set (map :path (filter his hers)))]))
    (check "two reads with two homes share no source of the base, though each reads the same rows of it; the sources of the team, a streaming layer, are shared"
           [#{} true true]
           (let [place {:asks [:place :note-41 :text]}
                 through-notes (q (merge alice place {:layers [:alice-notes :team :base]}))
                 through-team (q (merge alice place {:layers [:team :base]}))]
             [(set (filter (read-in through-notes :base) (read-in through-team :base)))
              (= (set (map :path (read-in through-notes :base))) (set (map :path (read-in through-team :base))))
              (= (read-in through-notes :team) (read-in through-team :team))]))
    (check "two askers of one thing who ask for different parts have equal sources"
           true
           (let [one (q {:asks [:place :note-41 :text] :asker :alice :layers [:team :base] :with #{:writer}})
                 two (q {:asks [:place :note-41 :text] :asker :alice :layers [:team :base] :stores-own-facts true})]
             (= (:wanted (sources/reading one {} :once numbers)) (:wanted (sources/reading two {} :once numbers)))))
    (check "which sources are a layer's rights, and which a check, in a streaming layer and in the base under a home"
           [:own-rows :permission nil true false :own-rows :permission nil true false]
           (let [points (q (merge alice {:asks [:points-at :parent :canvas-1] :layers [:team :base]}))]
             [(sources/rights-kind points (sources/own-rows-source :team :team))
              (sources/rights-kind points (sources/permission-source :team :team :alice))
              (sources/rights-kind points (sources/place-source :team :team :block-1 :parent))
              (sources/check? points (sources/check-source :team :team :block-1 :parent))
              (sources/check? points (sources/permission-source :team :team :alice))
              (sources/rights-kind points (sources/own-rows-source :team :base))
              (sources/rights-kind points (sources/permission-source :team :base :alice))
              (sources/rights-kind points (sources/place-source :team :base :block-1 :parent))
              (sources/check? points (sources/check-source :team :base :block-1 :parent))
              (sources/check? points (sources/permission-source :team :base :alice))]))
    (check "a source is a map of five parts and no other, in a streaming layer and in the base"
           [#{:gate :table :layer :routing-id :path}]
           (vec (distinct (map (comp set keys)
                               (mapcat #(keys (:known (play-once traces (q (merge alice {:asks % :layers [:alice-notes :team :base]})))))
                                       asks-of-each-word)))))
    (shown "the sources of each word in a streaming layer and in the base hold the table, the routing id and the path that the record's page gives. The routing id is the layer for a streaming layer, under every home; in the base it is the home of the read for every kind of source: the layer's own rows, a permission, a place, a check, an entity, a run of keys, a run across entities, a whole layer and the order by value. Each is one source")
    (shown "two reads with one home have equal sources for the base, so they share each push on it; two reads with two homes share none of them, and read the same rows; a streaming layer's sources are shared whatever the homes")
    (shown "two askers of one thing who ask for different parts of a row have equal sources, so they share each push; a check of a nearer layer at a place is the source of that place")))

(deftest the-loop-ends
  (let [place-3 (q (merge alice {:asks [:place :note-41 :text] :layers [:alice-notes :team :base]}))
        place-1 (q (merge alice {:asks [:place :note-41 :text] :layers [:team]}))
        place-from (q (merge alice {:asks [:place :note-41 :text] :from-layer :alice-notes}))
        entity-base (q (merge alice {:asks [:entity :note-41] :layers [:base]}))
        run-base (q (merge alice {:asks [:run [:block-1] [:block-9]] :layers [:base]}))
        points-3 (q (merge alice {:asks [:points-at :parent :canvas-1] :layers [:alice-notes :team :base]}))
        points-from (q (merge alice {:asks [:points-at :parent :canvas-1] :from-layer :alice-notes}))
        once (fn [query] (let [{:keys [rounds calls]} (play-once traces query)] [rounds calls]))
        fact-source? (fn [query source] (and (nil? (sources/rights-kind query source)) (= :facts (:table source))))]
    (check "asked once: a place in one layer is one call in one round; through three layers three calls in one round; from a layer one call and then two"
           [[1 [1]] [1 [3]] [2 [1 2]]]
           (mapv once [place-1 place-3 place-from]))
    (check "asked once: an entity of the base is one call, with the rights in it; a run across entities of the base is one call too, and so is a whole layer"
           [[1 [1]] [1 [1]] [1 [1]]]
           (mapv once [entity-base run-base (q (merge alice {:asks [:run :first :last] :layers [:base]}))]))
    (check "asked once through her own layer: the base is one call more, under her layer's routing id, for a place, an entity, a run across entities and a whole layer"
           [[1 [2]] [1 [2]] [1 [2]] [1 [2]]]
           (mapv #(once (q (merge alice {:asks % :layers [:team :base]})))
                 [[:place :note-41 :text] [:entity :note-41] [:run [:block-1] [:block-9]] [:run :first :last]]))
    (check "asked once: what points at a thing through three layers is three rounds, the rights, the orders, the checks; from a layer four"
           [[3 [3 2 2]] 4]
           [(once points-3) (first (once points-from))])
    (check "kept live: a place through an everyone layer opens in two rounds; through members layers in three; what points at a thing through a given list in four. From a start layer that is members it is six: the list of a start layer is read only when she may see the layer, and that needs her permission, one round after the layer's own rows. None takes more than six"
           [2 3 4 6 true]
           [(first (live-rounds (q (merge alice {:asks [:place :note-41 :text] :layers [:base]}))))
            (first (live-rounds place-3))
            (first (live-rounds points-3))
            (first (live-rounds points-from))
            (every? #(<= (first (live-rounds %)) 6) [place-1 place-3 place-from entity-base run-base points-3 points-from])])
    (check "the worked case of the plan: a place kept live through three layers, of which two are members, holds eight pushes"
           8 (second (live-rounds place-3)))
    (check "a read asked once wants the place with the rights, in its first round; a live read does not"
           [3 0]
           [(count (filter #(fact-source? place-3 %) (:first-missing (play-once traces place-3))))
            (count (filter #(fact-source? place-3 %) (:first-joined (open-live traces [] place-3))))])
    (check "with the one predicate of that choice giving false, a read asked once reads the rights first, and gives the same answer"
           [true [2 [3 3]]]
           (let [as-built (:answer (play-once traces place-3))]
             (with-redefs [sources/before-standing? (constantly false)]
               [(= as-built (:answer (play-once traces place-3)))
                (once place-3)])))
    (check "the calls of one round: a layer's rights and its fact are one call, in the base under the home's routing id"
           [[:microbatch :facts :base 3]]
           (mapv (juxt :gate :table :routing-id (comp count :sources))
                 (sources/calls-for-once (:missing (sources/reading entity-base {} :once numbers)) numbers)))
    (check "the calls of one round through her own layer: one call for each of the two tables, both under her layer's routing id, each with the rights and the fact"
           #{[:streaming :facts :team 3] [:microbatch :facts :team 3]}
           (set (map (juxt :gate :table :routing-id (comp count :sources))
                     (sources/calls-for-once
                      (:missing (sources/reading (q (merge alice {:asks [:entity :note-41] :layers [:team :base]})) {} :once numbers))
                      numbers))))
    (check "each call holds sources of one table under one routing id, and each source is in one call: every source of what points at a thing through three layers, the base among them"
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
    (shown "asked once, the base is one call of its table under the home's routing id, with the rights and the fact in it: 1 call from the base itself, also for a run across its entities and for the whole layer; 1 call more than her own layer's when it is read through her layer")
    (shown "the loop ends: asked once, a place is 1 round (1 call in one layer, 3 through three layers), from a layer 2 rounds (1 call, then 2), what points at a thing 3 rounds, from a layer 4; kept live, a place opens in 2 or 3 rounds, what points at a thing through a given list in 4, and from a start layer that is members in 6; none takes more than 6")
    (shown "a read asked once wants the place in the same round as the rights, a live read only when she may see the layer; with before-standing? giving false the read asked once takes the rights first, 2 rounds of 3 calls, and gives the same answer")))

(deftest the-checks-of-nearer-layers
  (let [query (q (merge alice {:asks [:points-at :parent :canvas-1] :layers [:alice-notes :team :base]}))
        once (play-once traces query)
        r (:reading once)
        known (:known once)
        checks (fn [reading layers]
                 (into {} (map (fn [layer] [layer (set (keys (get-in reading [:by-layer layer :checks])))])) layers))
        ;; a place that two orders hold, with a layer between them
        between (reduce write {} [[:make :l1 1] [:make :l2 1] [:make :l3 1]
                                  [:setting :l1 :everyone 2] [:setting :l2 :everyone 2] [:setting :l3 :everyone 2]
                                  [:marks :l1 #{:parent} 3] [:marks :l3 #{:parent} 3]
                                  [:fact :l1 :maker :p :parent :canvas-1 11] [:fact :l3 :maker :p :parent :canvas-1 12]
                                  [:fact :l3 :maker :q :parent :canvas-1 13]])
        between-r (:reading (play-once between (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers [:l1 :l2 :l3]})))
        ;; ruling 64: a check's row that holds another thing, nothing, the thing
        nearer (fn [v] (let [tables (reduce write {} [[:make :l1 1] [:make :l2 1]
                                                       [:setting :l1 :everyone 2] [:setting :l2 :everyone 2]
                                                       [:marks :l2 #{:parent} 3]
                                                       [:fact :l2 :maker :p :parent :canvas-1 11]
                                                       [:fact :l1 :ann :p :parent v 12]])]
                         (:answer (play-once tables (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers [:l1 :l2]})))))
        row (fn [v] (fact-row* :l :maker :e :k v 5))
        uuid (UUID. 3 4)]
    (check "a check is wanted in each layer nearer than the nearest order that holds the place, and in no other: the base's two places in both nearer layers; the place of the team's own order in the nearest layer alone"
           {:alice-notes #{[:block-1 :parent] [:block-2 :parent] [:block-3 :parent]}
            :team #{[:block-1 :parent] [:block-2 :parent]}
            :base #{}}
           (checks r [:alice-notes :team :base]))
    (check "the base's block-1 is checked in the team too, where another thing was written: it is hidden; block-2 comes from the base, block-3 from the team's own order"
           [[[:block-2 :parent] :base] [[:block-3 :parent] :team]]
           (sort (map (fn [[place a]] [place (:layer a)]) (:places (:answer once)))))
    (check "a place that two orders hold, with a layer between them: no check in the layer between, since the nearer order wins before it is reached; the farther order's own place is checked in both nearer layers"
           {:l1 #{[:q :parent]} :l2 #{[:q :parent]} :l3 #{}}
           (checks between-r [:l1 :l2 :l3]))
    (check "with the one function of the checks put aside, the runs are joined: no check is wanted, and the place that a nearer layer moved is shown from the farther order"
           [{} #{[:block-1 :parent] [:block-2 :parent] [:block-3 :parent]}]
           (with-redefs [sources/nearer-checks (constantly {})]
             (let [joined (play-once traces query)]
               [(into {} (mapcat :checks) (vals (:by-layer (:reading joined))))
                (set (keys (:places (:answer joined))))])))
    (check "the checks of one read are bounded, by one number when it is asked once and by another when it is kept live: past its own number the read wants none of them and says that it is too large, with that number; the other number does not bound it"
           [[{:what :checks-of-one-read :most 2} true] [{:what :checks-of-one-read :most 2} true] [nil false] [nil false]]
           (let [bounded (fn [mode numbers]
                           (let [r (sources/reading query known mode numbers)]
                             [(:too-large r) (every? nil? (map :checks (vals (:by-layer r))))]))]
             [(bounded :once (assoc numbers :max-checks-of-one-read 2))
              (bounded :live (assoc numbers :max-checks-of-one-live-read 2))
              (bounded :once (assoc numbers :max-checks-of-one-live-read 2))
              (bounded :live (assoc numbers :max-checks-of-one-read 2))]))
    (check "how many checks one read may need, by how it is asked, at the defaults"
           [40000 250] [(sources/most-checks :once numbers) (sources/most-checks :live numbers)])
    (check "a check's row that holds another thing hides the place; one that says nothing here hides it; one that holds the asked thing gives the place from the nearer layer's row"
           [{} {} [:l1 :canvas-1 12]]
           [(:places (nearer :canvas-2)) (:places (nearer nil))
            ((juxt :layer :value :stamp) (get (:places (nearer :canvas-1)) [:p :parent]))])
    (check "a row holds a thing exactly when its String is the thing's one text: a UUID, a string with spaces, a decimal with trailing zeros; not another thing, not a retract, not no row, not a String that is no value of the form, in a row that the proof writes by hand"
           [true true true false false false false]
           [(rows/holds-this-value? (row uuid) uuid)
            (rows/holds-this-value? (row "a thing with spaces") "a thing with spaces")
            (rows/holds-this-value? (row 1.50M) 1.5M)
            (rows/holds-this-value? (row uuid) (str uuid))
            (rows/holds-this-value? (row nil) :canvas-1)
            (rows/holds-this-value? nil :canvas-1)
            (rows/holds-this-value? {:value "(not a value" :stamp 1} :canvas-1)])
    (shown "the checks of nearer layers: one in each layer nearer than the nearest order that holds a place, and in no other; none in a layer between two orders that both hold it; with the one function put aside the runs are joined; past the build's number none is wanted and the read is too large")
    (shown "the nearest row wins, and the place is given from it exactly when it holds the thing: a nearer row with another thing, or a retract, hides the place; a nearer row with the asked thing gives it, from the nearer layer")))

(deftest what-a-read-gives
  (let [tables (reduce write traces
                       [[:fact :alice-notes :alice :note-41 :text nil 20]
                        [:fact :team :bob :note-9 :text "only the team" 713]
                        [:fact :base :system :note-9 :size 9 330]])
        tables (put tables :microbatch :facts (rows/place-key :base :note-9 :raw)
                    {:value "(not a value" :by-act (UUID. 0 9) :by-index 0 :stamp 331 :time (time-of 331)
                     :who ":system" :permission "[:system :base :base]"})
        ask (fn [who asks layers & more]
              (:answer (play-once tables (q (merge {:asks asks :layers layers} (if who {:asker who} {:not-signed-in true})
                                                   (apply hash-map more))))))
        place (fn [& args] (:place (apply ask args)))
        uuid (fn [stamp] (UUID. 0 (long stamp)))]
    (check "the four things that a place can hold, told apart: no fact; nothing here; a value as its maker gave it; a String that is no value of the form, in a row that the proof puts in the table by hand, since no write of a gate gives one"
           [{:holds :no-fact}
            {:holds :nothing-here :layer :alice-notes :fact [[:alice-notes :streaming (uuid 20)] 0] :stamp 20}
            {:holds :value :value "from the base" :layer :base :fact [[:base :microbatch (uuid 310)] 0] :stamp 310}
            {:holds :not-a-value :layer :base :fact [[:base :microbatch (uuid 9)] 0] :stamp 331}]
           [(place :alice [:place :nobody :text] [:alice-notes :team :base])
            (place :alice [:place :note-41 :text] [:alice-notes :team :base])
            (place :alice [:place :note-41 :text] [:base])
            (place :alice [:place :note-9 :raw] [:base])])
    (check "the nearest layer that holds a fact wins, also when that fact says nothing here; a place that only a farther layer holds comes from there"
           [[:nothing-here :alice-notes] [:value :team] [:value :base]]
           [((juxt :holds :layer) (place :alice [:place :note-41 :text] [:alice-notes :team :base]))
            ((juxt :holds :layer) (place :alice [:place :note-41 :text] [:team :base]))
            ((juxt :holds :layer) (place :alice [:place :note-41 :title] [:alice-notes :team :base]))])
    (check "a run through layers: each place from the nearest layer that holds it"
           {[:note-41 :text] :alice-notes [:note-41 :title] :base [:note-9 :text] :team [:note-9 :size] :base [:note-9 :raw] :base}
           (into {} (map (fn [[p a]] [p (:layer a)])) (:places (ask :alice [:run [:note-41] [:note-9]] [:alice-notes :team :base]))))
    (check "a layer that she may not see, and one that is not made, are named with why, and nothing of them is given: the answer is that of the same query without them"
           [[{:layer :alice-notes :why :may-not-see} {:layer :nowhere :why :not-made}]
            [:alice-notes :team :nowhere :base]
            true]
           (let [with (ask :bob [:place :note-41 :text] [:alice-notes :team :nowhere :team :base])
                 without (ask :bob [:place :note-41 :text] [:team :base])]
             [(:left-out with) (:layers with) (= (:place with) (:place without))]))
    (check "a start layer that she may not see: that one layer is left out, and its list is not used"
           {:layers [:alice-notes] :left-out [{:layer :alice-notes :why :may-not-see}] :place {:holds :no-fact}}
           (:answer (play-once tables (q {:asks [:place :note-41 :text] :asker :bob :from-layer :alice-notes}))))
    (check "a person who is not signed in sees the base, and no members layer"
           [[:value :base] [{:layer :team :why :may-not-see}]]
           (let [a (ask nil [:place :note-41 :text] [:team :base])] [((juxt :holds :layer) (:place a)) (:left-out a)]))
    (check "a place-answer holds no writer and no permission unless the query asked: neither, the writer, both"
           [#{:holds :value :layer :fact :stamp}
            [:bob nil]
            [:bob [:bob :team :team]]]
           [(set (keys (place :alice [:place :note-9 :text] [:team])))
            ((juxt :writer :permission) (place :alice [:place :note-9 :text] [:team] :with #{:writer}))
            ((juxt :writer :permission) (place :alice [:place :note-9 :text] [:team] :with #{:writer :permission}))])
    (check "a place-answer holds the time on the act that wrote its fact only when the query asks for :time: not with the writer and the permission; with :time alone, the time and neither of the two; with all three, all three"
           [false
            [nil nil (time-of 713)]
            [:bob [:bob :team :team] (time-of 713)]]
           [(contains? (place :alice [:place :note-9 :text] [:team] :with #{:writer :permission}) :time)
            ((juxt :writer :permission :time) (place :alice [:place :note-9 :text] [:team] :with #{:time}))
            ((juxt :writer :permission :time) (place :alice [:place :note-9 :text] [:team] :with #{:writer :permission :time}))])
    (check "a run through three layers that asks for :time: each place-answer holds the :time of the row that it came from, in the nearest layer that holds the place; the same run with no :with holds no time, and is the first with the time taken out"
           [{[:note-41 :text] (time-of 20) [:note-41 :title] (time-of 311) [:note-9 :text] (time-of 713)
             [:note-9 :size] (time-of 330) [:note-9 :raw] (time-of 331)}
            true true true]
           (let [with-time (:places (ask :alice [:run [:note-41] [:note-9]] [:alice-notes :team :base] :with #{:time}))
                 without (:places (ask :alice [:run [:note-41] [:note-9]] [:alice-notes :team :base]))
                 row-of (fn [[e k] {:keys [layer]}]
                          (get (table tables (first-act/depot-at-making layer) :facts) (rows/place-key layer e k)))]
             [(into {} (map (fn [[p a]] [p (:time a)])) with-time)
              (every? (fn [[p a]] (= (:time a) (:time (row-of p a)))) with-time)
              (not-any? (fn [[_ a]] (contains? a :time)) without)
              (= without (into {} (map (fn [[p a]] [p (dissoc a :time)])) with-time))]))
    (check "the store's own facts are left out of a run unless the query asks: the entity of a holder, and of the layer itself"
           [{} #{:softland.store/permission}
            {} #{:softland.store/depot :softland.store/marked-keys}]
           [(:places (ask :alice [:entity :bob] [:team]))
            (set (map second (keys (:places (ask :alice [:entity :bob] [:team] :stores-own-facts true)))))
            (:places (ask :alice [:entity :team] [:team]))
            (set (map second (keys (:places (ask :alice [:entity :team] [:team] :stores-own-facts true)))))])
    (check "a row under a store key does not count for a read that did not ask, and counts for one that did"
           [false true true]
           [(answer/counts? (q {:asks [:entity :e] :asker :a :layers [:l]}) :softland.store/permission)
            (answer/counts? (q {:asks [:entity :e] :asker :a :layers [:l] :stores-own-facts true}) :softland.store/permission)
            (answer/counts? (q {:asks [:entity :e] :asker :a :layers [:l]}) :text)])
    (check "a run across entities of the base, read through her own layer, gives each place of the base once, from its row, and the nearer layer's place where it holds one"
           [{[:block-1 :parent] [:team :canvas-2 711] [:block-2 :parent] [:base :canvas-1 321] [:block-3 :parent] [:team :canvas-1 710]
             [:note-41 :text] [:team "from the team" 712] [:note-41 :title] [:base "a title" 311]
             [:note-9 :text] [:team "only the team" 713] [:note-9 :size] [:base 9 330] [:note-9 :raw] [:base nil 331]}
            {[:block-1 :parent] [:base :canvas-1 320] [:block-2 :parent] [:base :canvas-1 321]
             [:note-41 :text] [:base "from the base" 310] [:note-41 :title] [:base "a title" 311]
             [:note-9 :size] [:base 9 330] [:note-9 :raw] [:base nil 331]}]
           (let [of-run (fn [layers] (into {} (map (fn [[p a]] [p ((juxt :layer :value :stamp) a)]))
                                           (:places (ask :alice [:run [:block-1] [:note-9 :text]] layers))))]
             [(of-run [:team :base]) (of-run [:base])]))
    (check "the answer of a read of the base does not differ by its home: the same place, entity, run and what points at a thing, from the base itself and through a layer that is not made"
           [true true true true]
           (mapv (fn [asks]
                   (= (dissoc (ask :alice asks [:base]) :layers :left-out)
                      (dissoc (ask :alice asks [:nowhere :base]) :layers :left-out)))
                 [[:place :note-41 :text] [:entity :note-41] [:run :first :last] [:points-at :parent :canvas-1]]))
    (check "what points at a thing says, for each layer that she may see, since which stamp its order is level, or that it marks nothing"
           [{:layer :alice-notes :not-marked true} {:layer :team :level-since 700} {:layer :base :level-since 300}]
           (:orders (ask :alice [:points-at :parent :canvas-1] [:alice-notes :team :base])))
    (shown "what a read gives: the four things that a place can hold are told apart; the nearest layer that holds a fact wins, also with nothing here; each place of a run comes from the nearest layer that holds it; a layer that is left out is named with why and nothing of it is given")
    (shown "a place-answer holds no writer, no permission and no time unless asked; the store's own facts are in no answer that did not ask for them; a run across entities of the base gives each place once, and a read of the base gives the same from the base itself and through another home")))

(deftest which-changes-reach-an-asker
  (let [tables (reduce write traces [[:fact :alice-notes :alice :note-41 :text "from her notes" 20]])
        known-for (fn [query] ((juxt :reading :known) (play-once tables query)))
        alice-3 (q (merge alice {:asks [:place :note-41 :text] :layers [:alice-notes :team :base]}))
        [r known] (known-for alice-3)
        bob-3 (q {:asks [:place :note-41 :text] :asker :bob :layers [:alice-notes :team :base]})
        [bob-r bob-known] (known-for bob-3)
        holder (q (merge alice {:asks [:entity :bob] :layers [:team]}))
        [holder-r holder-known] (known-for holder)
        asked (assoc holder :stores-own-facts true)
        [asked-r asked-known] (known-for asked)
        place [:note-41 :text]]
    (check "a change in the layer that wins the place is told; a change under a nearer row is not"
           [true false false]
           [(answer/told-by? alice-3 r known (sources/place-source :alice-notes :alice-notes :note-41 :text) place)
            (answer/told-by? alice-3 r known (sources/place-source :alice-notes :team :note-41 :text) place)
            (answer/told-by? alice-3 r known (sources/place-source :alice-notes :base :note-41 :text) place)])
    (check "a change of the base's row is told to an asker who reads it through her own layer, when no nearer layer holds the place; the source is one that her read holds"
           [true true false]
           (let [title [:note-41 :title]
                 query (q (merge alice {:asks [:place :note-41 :title] :layers [:alice-notes :team :base]}))
                 [title-r title-known] (known-for query)
                 source (sources/place-source :alice-notes :base :note-41 :title)]
             [(answer/told-by? query title-r title-known source title)
              (contains? title-known source)
              (contains? title-known (sources/place-source :base :base :note-41 :title))]))
    (check "a change in a layer that she may not see is not told; for her the next layer wins, and its change is told"
           [false true]
           [(answer/told-by? bob-3 bob-r bob-known (sources/place-source :alice-notes :alice-notes :note-41 :text) place)
            (answer/told-by? bob-3 bob-r bob-known (sources/place-source :alice-notes :team :note-41 :text) place)])
    (check "a change of a store key's row is told only to an asker who asked for the store's own facts"
           [false true]
           [(answer/told-by? holder holder-r holder-known (first (get-in holder-r [:by-layer :team :facts])) [:bob :softland.store/permission])
            (answer/told-by? asked asked-r asked-known (first (get-in asked-r [:by-layer :team :facts])) [:bob :softland.store/permission])])
    (check "the places that a change names for a query: the asked place from its row and from no other; each key of a run; no place from a layer's own rows"
           [#{[:note-41 :text]} #{} #{[:bob :softland.store/permission]} #{}]
           [(sources/places-of alice-3 (sources/place-source :alice-notes :team :note-41 :text) [":team :note-41 :text"])
            (sources/places-of alice-3 (sources/permission-source :alice-notes :team :alice) [":team :alice :softland.store/permission"])
            (sources/places-of holder (first (get-in holder-r [:by-layer :team :facts])) [":team :bob :softland.store/permission" "no key"])
            (sources/places-of holder (sources/own-rows-source :team :team) [":team :team :softland.store/depot"])])
    (check "what the base's own rows and her right mean at a change, for a read through her own layer: each is read beside the other, which the read holds under her layer's routing id"
           [[:stands :stands] [:none :stands] [:members :members]]
           (let [tables (reduce write {} [[:make :mine 1] [:make :base 1] [:setting :mine :everyone 2] [:setting :base :members 2]
                                          [:permission :base :ann 3] [:fact :base :maker :p :text "b0" 11]])
                 query (q {:asks [:place :p :text] :asker :ann :layers [:mine :base]})
                 known (:known (play-once tables query))
                 own (sources/own-rows-source :mine :base)
                 permission (sources/permission-source :mine :base :ann)
                 of-own (answer/meanings query known own (get known own) (get known own))
                 of-permission (answer/meanings query known permission nil (get known permission))]
             [(mapv :right of-own) (mapv :right of-permission) (mapv :who-may-see of-permission)]))
    (check "the places that a change of the base names, for a read through her own layer: the asked place from its row; each key of a run across entities; the entity of each key of the order by value"
           [#{[:note-41 :text]} #{[:block-1 :parent] [:note-41 :title]} #{[:block-2 :parent]}]
           (let [digest (rows/value-digest-of :canvas-1)
                 run (q (merge alice {:asks [:run :first :last] :layers [:team :base]}))
                 points (q (merge alice {:asks [:points-at :parent :canvas-1] :layers [:team :base]}))]
             [(sources/places-of alice-3 (sources/place-source :alice-notes :base :note-41 :text) [":base :note-41 :text"])
              (sources/places-of run (sources/run-source :team :base ":base " ":base!") [":base :block-1 :parent" ":base :note-41 :title"])
              (sources/places-of points (sources/by-value-source :team :base :parent 300 :canvas-1)
                                 [(rows/by-value-key :base :parent 300 digest :block-2)])]))
    (shown "which changes reach an asker: a change in the layer that wins the place; not one under a nearer row, not one in a layer that she may not see, and one of a store key's row only when she asked for the store's own facts")))

;; ------------------------------------------ a right's chain, and who may see a layer

(def ^:private olga-near "Olga's right in `:near`: the first permission of the layer." [:olga :near :near])
(def ^:private gus-near "Gus's right in `:near`, given beneath Olga's." [:gus :near :near olga-near])
(def ^:private ann-near "Ann's right in `:near`, given beneath Gus's: a chain of three." [:ann :near :near gus-near])

(def ^:private chain-tables
  "The tables of the checks of a right's chain. `:near` is a streaming layer
  that is members, with a fact at the place `[:p :text]`; in it Olga holds
  the first permission, Gus a right beneath hers, and Ann a right beneath
  his, each written after the one above it. `:far` is everyone and holds a
  fact at the same place."
  (reduce write {}
          [[:make :near 1] [:make :far 1] [:setting :near :members 2] [:setting :far :everyone 2]
           [:permission :near :olga 3] [:give :near :gus olga-near 4] [:give :near :ann gus-near 5]
           [:fact :near :maker :p :text "n0" 10] [:fact :far :maker :p :text "f0" 11]]))

(def ^:private ann-place
  "Ann's query: the place `[:p :text]` through `:near` and `:far`."
  (q {:asks [:place :p :text] :asker :ann :layers [:near :far]}))

(def ^:private chain-writes
  "The writes of the chain's played worlds, by name: an end, or a give, at
  a place of the chain; the setting of `:near`, in each of its three words;
  and a fact of `:near`. A give that meets a place that holds a value
  changes nothing, as a gate refuses it."
  (array-map
   :end-gus   [:end :near :gus]
   :give-gus  [:give :near :gus olga-near]
   :end-ann   [:end :near :ann]
   :give-ann  [:give :near :ann olga-near]
   :end-olga  [:end :near :olga]
   :everyone  [:setting :near :everyone]
   :signed-in [:setting :near :signed-in]
   :members   [:setting :near :members]
   :near      [:fact :near :maker :p :text]))

(def ^:private ann-points
  "Ann's query of what points at `:canvas-1` under `:parent`, through
  `:near` and `:far`, neither of which marks the key: no fact source is
  read in either, so her standing in `:near` shows only in the answer's
  `:left-out` and `:orders`."
  (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers [:near :far]}))

(defn- chain-world
  "A world for a query of Ann's on the chain's tables: her place through
  `:near` and `:far` when no query is given. Takes the names of the writes
  that are to come, each at a stamp of its own after the tables'; a fact of
  `:near` gets a text of its own."
  ([names] (chain-world names ann-place))
  ([names query]
   (open-live chain-tables
              (map-indexed (fn [i n]
                             (let [w (get chain-writes n)]
                               (if (= :near n) (conj w (str "n" (inc i)) (+ 100 i)) (conj w (+ 100 i)))))
                           names)
              query)))

(defn- quiet
  "Takes a world. Makes every move that is no write, the first that can be
  made each time, until none is left: each push takes its values, each
  callback is handled, each push that the read asked for is served and
  arrives. Gives the world."
  [world]
  (loop [world world n 0]
    (let [moves (remove #(= :write (first %)) (moves-of world))]
      (if (or (empty? moves) (< 500 n)) world (recur (move world (first moves) nil) (inc n))))))

(defn- every-small-chain
  "Every list of up to n of the chain's writes, each played in every order
  of the moves, for Ann's place or for the query given. Gives {:lists
  :states :wrong}."
  ([n] (every-small-chain n ann-place))
  ([n query]
   (let [played (for [names (lists-up-to n (keys chain-writes))]
                  (assoc (every-order (chain-world names query) nil) :is names))]
     {:lists (count played)
      :states (reduce + (map :states played))
      :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))})))

(deftest a-right-stands-while-its-chain-stands
  (let [after (fn [& ws] (reduce write chain-tables ws))
        once (fn [tables] (play-once tables ann-place))
        seen (fn [tables] (let [answer (:answer (once tables))]
                            [(:left-out answer) ((juxt :layer :value) (:place answer))]))
        sees-near [[] [:near "n0"]]
        out-of-near [[{:layer :near :why :may-not-see}] [:far "f0"]]
        ann-src (sources/permission-source :near :near :ann)
        gus-src (sources/permission-source :near :near :gus)
        olga-src (sources/permission-source :near :near :olga)
        ann-row (fn [tables] (table-value tables ann-src))
        above (fn [tables] (into {} (map (fn [k] [k (get (table tables :streaming :facts) k)]))
                                 (layers/above-places :near :ann (ann-row tables))))
        right-in (fn [tables] (layers/right :near :ann (ann-row tables) (above tables)))
        ends-gus (after [:end :near :gus 20])
        given-gus-again (after [:end :near :gus 20] [:give :near :gus olga-near 21])
        ends-olga (after [:end :near :olga 20])
        ends-ann (after [:end :near :ann 20])
        given-ann-again (after [:end :near :gus 20] [:end :near :ann 21] [:give :near :ann olga-near 22])
        ;; what a change of Gus's row means for Ann's read, from what a read asked once knew
        meant (fn [tables new-tables]
                (let [[before after] (answer/meanings ann-place (:known (once tables)) gus-src
                                                      (table-value tables gus-src) (table-value new-tables gus-src))]
                  (= before after)))
        ;; kept live: the end above her, the end of her own right, a new right beneath Olga's
        live-0 (chain-world [:end-gus :end-ann :give-ann])
        live-1 (quiet (move live-0 [:write] nil))
        live-2 (quiet (move live-1 [:write] nil))
        live-3 (quiet (move live-2 [:write] nil))
        on-screen (fn [world] [(:left-out (:screen world)) ((juxt :layer :value) (:place (:screen world)))])
        chain (every-small-chain 3)
        chain-points (every-small-chain 3 ann-points)
        ;; the longest read: from a start layer that is members, through a members layer, each right given beneath another's
        from-tables (reduce write {}
                            [[:make :home 1] [:make :near 1] [:setting :home :members 2] [:setting :near :members 2]
                             [:list :home [:near] 3] [:marks :near #{:parent} 3] [:marks :home #{:parent} 3]
                             [:permission :home :olga 4] [:give :home :ann [:olga :home :home] 5]
                             [:permission :near :olga 4] [:give :near :gus olga-near 5] [:give :near :ann gus-near 6]
                             [:fact :near :maker :b-1 :parent :canvas-1 10] [:fact :home :maker :b-2 :parent :canvas-1 11]])
        from-points (q {:asks [:points-at :parent :canvas-1] :asker :ann :from-layer :home})
        from-live (open-live from-tables [] from-points)
        from-once (play-once from-tables from-points)
        ;; a fault: a change of a level above is not taken as a change of her right
        blind (with-redefs [sources/rights-kind (let [rights-kind sources/rights-kind]
                                                  (fn [query source]
                                                    (let [kind (rights-kind query source)]
                                                      (when-not (= :level-above kind) kind))))]
                (every-small-chain 2 ann-points))
        ;; a fault: her right is in the meaning by its word alone, with no mark for each level above
        word-only (with-redefs [layers/levels-above (constantly nil)]
                    (every-small-chain 2 ann-points))]
    (check "a right given beneath another's is read with the row of each level above it, nearest first: asked once, one round more than a first permission, in one call; and she sees the members layer"
           [[gus-src olga-src] [2 [2 1]] sees-near [1 [2]]]
           [(get-in (:reading (once chain-tables)) [:by-layer :near :above])
            ((juxt :rounds :calls) (once chain-tables))
            (seen chain-tables)
            ((juxt :rounds :calls) (play-once chain-tables (q {:asks [:place :p :text] :asker :olga :layers [:near :far]})))])
    (check "kept live, a place through a members layer in which her right is the third of a chain opens in four rounds, one more than through a first permission"
           [4 3] [(:rounds (chain-world [])) (:rounds (open-live chain-tables [] (q {:asks [:place :p :text] :asker :olga :layers [:near :far]})))])
    (check "the longest read: from a start layer that is members, through a members layer that marks the key, each right given beneath another's, what points at a thing kept live opens in eight rounds and asked once in six, and the two give the same answer with both places"
           [8 6 true #{[:b-1 :parent] [:b-2 :parent]}]
           [(:rounds from-live) (:rounds from-once) (= (:screen from-live) (:answer from-once))
            (set (keys (:places (:answer from-once))))])
    (check "her right as it is known: it stands; with her row not read yet, or a place above it not read yet, it is not known yet"
           [:stands :not-known-yet :not-known-yet]
           [(right-in chain-tables)
            (layers/right :near :ann :not-known-yet {})
            (layers/right :near :ann (ann-row chain-tables) (dissoc (above chain-tables) (second (:path olga-src))))])
    (check "a row at her place holds a right of hers only when the value is a permission whose holder is she, in this layer"
           [ann-near nil nil nil nil]
           [(layers/held-permission :near :ann (ann-row chain-tables))
            (layers/held-permission :near :ann (fact-row* :near :maker :ann :softland.store/permission gus-near 30))
            (layers/held-permission :near :ann (fact-row* :near :maker :ann :softland.store/permission [:ann :far :far] 30))
            (layers/held-permission :near :ann (fact-row* :near :maker :ann :softland.store/permission :owner 30))
            (layers/held-permission :near :ann (ann-row ends-ann))])
    (check "a retract at her place is no right: she may not see the members layer, and the place comes from the farther one"
           [:none out-of-near] [(right-in ends-ann) (seen ends-ann)])
    (check "a right whose level above has ended does not let her see: Gus's right ended; Gus's right ended and given again, since the new row above is later than hers; the first permission of the layer ended"
           [[:none out-of-near] [:none out-of-near] [:none out-of-near]]
           (mapv (juxt right-in seen) [ends-gus given-gus-again ends-olga]))
    (check "a new right given to her beneath the first permission, after hers ended, lets her see again, and its one level above is read"
           [:stands sees-near [olga-src]]
           [(right-in given-ann-again) (seen given-ann-again)
            (get-in (:reading (once given-ann-again)) [:by-layer :near :above])])
    (check "a level above her right is a rights source of its own kind, and no permission row is one for a person who is not signed in"
           [:level-above :permission nil false]
           [(sources/rights-kind ann-place gus-src) (sources/rights-kind ann-place ann-src)
            (sources/rights-kind (q {:asks [:place :p :text] :not-signed-in true :layers [:near]}) gus-src)
            (sources/check? ann-place gus-src)])
    (check "what a change of a level above means: an end above a right that stands means another thing; the first change of a level whose mark was still true means another thing, also when an end farther up had taken her right already; a level above given again after its end means the same"
           [false false true]
           [(meant chain-tables ends-gus)
            (meant ends-olga (after [:end :near :olga 20] [:end :near :gus 21]))
            (meant ends-gus given-gus-again)])
    (check "kept live: the read holds a push on each level above her right; an end above her is told, and she sees the members layer no more; the end of her own right leaves her out; a new right beneath the first permission lets her see again, and the push on the level that is no longer above her is left"
           [sees-near out-of-near out-of-near sees-near [true true] [false true] false]
           [(on-screen live-0) (on-screen live-1) (on-screen live-2) (on-screen live-3)
            [(contains? (:held live-0) gus-src) (contains? (:held live-0) olga-src)]
            [(contains? (:held live-3) gus-src) (contains? (:held live-3) olga-src)]
            (:owed? live-3)])
    (check "in every order of the callbacks the screen at the end is what a read asked once gives: a place through a members layer in which her right is the third of a chain, and an everyone layer; every list of up to three writes of an end or a give at a place of the chain, the layer's setting and a fact of the layer, 819 lists, none with an order that ends wrong"
           [819 []] [(:lists chain) (:wrong chain)])
    (check "the same for what points at a thing through the two layers, neither marking the key, so that her standing shows only in what the answer says of the layers: 819 lists, none with an order that ends wrong"
           [819 []] [(:lists chain-points) (:wrong chain-points)])
    (check "the player can fail. With a level above not taken as a rights source, so that its change is not read as a change of her right, the end of Gus's right ends wrong for what points at a thing, where no source of the layer is left"
           true (boolean (some #{[:end-gus]} (:wrong blind))))
    (check "the player can fail. With her right in the meaning by its word alone, two levels above that end at once can each be handled after both pushes hold their new values, and neither step tells: the ends of Gus's right and of Olga's end wrong for what points at a thing"
           true (boolean (some #{[:end-gus :end-olga]} (:wrong word-only))))
    (shown "a right stands while its chain stands: asked once, the levels above her right are one round more, in one call; a retract at her place, an end above her, the same right given again above her after its end, and the end of the layer's first permission each leave her out; a new right beneath the first permission lets her in")
    (shown "kept live, every order of the callbacks for every list of up to three ends, gives, settings and facts:" (:lists chain) "lists and" (:states chain) "states for a place,"
           (:lists chain-points) "lists and" (:states chain-points) "states for what points at a thing; in each end the screen is what a read asked once gives."
           "With a level above not taken as a rights source," (count (:wrong blind)) "of the" (:lists blind) "lists of up to two writes end wrong;"
           "with her right by its word alone," (count (:wrong word-only)) (pr-str (:wrong word-only)))))

(deftest who-may-see-a-layer-of-three-settings
  (let [signed-in (reduce write chain-tables [[:setting :near :signed-in 30]])
        bob (q {:asks [:place :p :text] :asker :bob :layers [:near :far]})
        nobody (q {:asks [:place :p :text] :not-signed-in true :layers [:near :far]})
        seen (fn [query] (let [answer (:answer (play-once signed-in query))]
                           [(:left-out answer) ((juxt :layer :value) (:place answer))]))
        place (every-small-standing standing-world [:near :far] 3 [:place :p :text] nil
                                    [:members :signed-in :everyone] {:asker :ann})
        nobody-place (every-small-standing standing-world [:near :far] 2 [:place :p :text] nil
                                           [:members :signed-in :everyone] {:not-signed-in true})
        ;; what each step of a live read tells, every write played to its end in one order
        tells (fn [setting permission? writes who]
                (let [told (atom [])]
                  (with-redefs [answer/after-change (let [after-change answer/after-change]
                                                      (fn [& args]
                                                        (let [step (apply after-change args)]
                                                          (when (:tell step) (swap! told conj (:tell step)))
                                                          step)))
                                answer/after-arrival (let [after-arrival answer/after-arrival]
                                                       (fn [& args]
                                                         (let [step (apply after-arrival args)]
                                                           (when (:tell step) (swap! told conj (:tell step)))
                                                           step)))]
                    (reduce (fn [world _] (quiet (move world [:write] nil)))
                            (standing-world setting permission? writes [:place :p :text] who)
                            writes))
                  @told))
        nobody-between (tells :members false [[:setting :signed-in] [:setting :members]] {:not-signed-in true})
        nobody-to-everyone (tells :members false [[:setting :everyone]] {:not-signed-in true})
        holder-between (tells :members true [[:setting :signed-in] [:setting :members]] {:asker :ann})]
    (check "a layer set to anyone signed in: an asker whom the query names sees it, with no right of her own, and after its own rows the read wants no right of hers there; a person who is not signed in may not see it"
           [[[] [:near "n0"]] [nil []] [[{:layer :near :why :may-not-see}] [:far "f0"]]]
           [(seen bob)
            ((juxt :permission :above) (get-in (:reading (play-once signed-in bob)) [:by-layer :near]))
            (seen nobody)])
    (check "kept live, a place through a layer whose setting is one of three words and an everyone layer: every list of up to three writes of the setting, her permission and the two facts, 1548 lists, none with an order that ends wrong"
           [1548 []] [(:lists place) (:wrong place)])
    (check "the same for a person who is not signed in, every list of up to two writes: 252 lists, none with an order that ends wrong"
           [252 []] [(:lists nobody-place) (:wrong nobody-place)])
    (check "a person who is not signed in is told nothing when a layer that she may not see goes from members to anyone signed in and back: the setting is in what the rows mean for her only as whether it is everyone; the layer going to everyone is told, once, as a whole answer"
           [[] 1 true]
           [nobody-between (count nobody-to-everyone) (every? :anew nobody-to-everyone)])
    (check "an asker whose right stands is told each of those two changes, as a whole answer anew: for her the setting is in the meaning with its word"
           [2 true] [(count holder-between) (every? :anew holder-between)])
    (shown "who may see a layer, of three settings: anyone signed in sees a signed-in layer with no right read;"
           "a person who is not signed in does not; kept live," (+ (:lists place) (:lists nobody-place)) "lists and"
           (+ (:states place) (:states nobody-place)) "states of setting changes, in each end the screen is what a read asked once gives")))

(deftest every-order-of-the-callbacks
  (let [played (mapv (fn [[what k marks facts seen writes]]
                       (assoc (every-order (points-at-world k marks facts seen writes) nil) :what what))
                     points-at-scenarios)
        small (every-small-points-at nil)
        place (every-small-standing standing-world [:near :far] 3 [:place :p :text] nil)
        entity (every-small-standing standing-world [:near :far] 2 [:entity :p] nil)
        ;; the base, read under the routing id of the asker's own layer
        base-played (vec (for [[where at] [["the nearest" (constantly 1)] ["the second" (constantly 2)] ["the farthest" identity]]
                               [what k marks facts seen writes] points-at-scenarios]
                           (assoc (every-order (points-at-world (base-at (at k)) k marks facts seen writes) nil)
                                  :what (str "the base is " where " layer; " what))))
        base-place (every-small-standing base-world [:mine :base] 3 [:place :p :text] nil)
        base-entity (every-small-standing base-world [:mine :base] 2 [:entity :p] nil)
        base-run (every-small-standing base-world [:mine :base] 2 [:run :first :last] nil)
        ;; a layer that leaves and joins by the start layer's list, while its fact and a nearer one change
        list-world (fn [asks]
                     (let [tables (reduce write {} [[:make :home 1] [:make :near 1] [:make :far 1]
                                                    [:setting :home :everyone 2] [:setting :near :everyone 2] [:setting :far :everyone 2]
                                                    [:list :home [:near :far] 3]
                                                    [:fact :far :maker :p :text "f0" 11]])]
                       (open-live tables
                                  [[:list :home [:far] 100] [:fact :near :maker :p :text "n1" 101]
                                   [:list :home [:near :far] 102] [:fact :far :maker :p :text "f1" 103]]
                                  (q {:asks asks :asker :ann :from-layer :home}))))
        lists (mapv #(every-order (list-world %) nil) [[:place :p :text] [:entity :p] [:run :first :last]])
        ;; the faults that the player can be told to make
        no-mark (mapv (fn [[what k marks facts seen writes]]
                        (when (:wrong (every-order (points-at-world k marks facts seen writes) :no-mark)) (first (str/split what #"\."))))
                      points-at-scenarios)
        counting-left (with-redefs [answer/told-by?
                                    (let [told-by? answer/told-by?]
                                      (fn [query reading known source place]
                                        (and (told-by? query reading known source place)
                                             (not-any? (fn [nearer]
                                                         (some? (get *known-before-the-leaving*
                                                                     (sources/check-source (layers/home query) nearer
                                                                                           (first place) (second place)))))
                                                       (take-while #(not= (:layer source) %) (:layers reading))))))]
                        {:scenarios (mapv (fn [[what k marks facts seen writes]]
                                            (when (:wrong (every-order (points-at-world k marks facts seen writes) nil))
                                              (first (str/split what #"\."))))
                                          points-at-scenarios)
                         :small (count (:wrong (every-small-points-at nil)))})]
    (doseq [{:keys [what wrong]} played]
      (check (str "what points at a thing, in every order of the callbacks, the screen at the end is what a read asked once gives: " what)
             nil wrong))
    (check "every list of up to three writes on one place in two layers, the nearer marking the key or not, from four starts: 672 lists, none with an order that ends wrong"
           [672 []] [(:lists small) (:wrong small)])
    (check "a place through two layers, the nearer with a setting and a permission or none: every list of up to three writes of the setting, the permission and the two facts, 620 lists, none with an order that ends wrong"
           [620 []] [(:lists place) (:wrong place)])
    (check "the same for an entity, every list of up to two writes: 120 lists, none with an order that ends wrong"
           [120 []] [(:lists entity) (:wrong entity)])
    (check "a layer that leaves and joins by the start layer's list while its fact and a farther one change: a place, an entity and a whole layer, none with an order that ends wrong"
           [nil nil nil] (mapv :wrong lists))
    (doseq [{:keys [what wrong]} base-played]
      (check (str "what points at a thing with one layer of the list being the base, each source of the base read under the routing id of the nearest layer: in every order of the callbacks the screen at the end is what a read asked once gives: " what)
             nil wrong))
    (check "the base is read in those worlds, under the routing id of the nearest layer: with the base second, each read that may see it and whose base marks the key holds the base's order by value under the nearest layer's routing id, and the scenario of four layers comes to hold a check of the base under it; with the base nearest, under the base's own"
           [[true] true [true]]
           (let [holds? (fn [world table routing-id]
                          (boolean (some #(= [:microbatch table routing-id] ((juxt :gate :table :routing-id) %)) (:held world))))
                 opened (fn [i [_ k marks facts seen writes]] (points-at-world (base-at i) k marks facts seen writes))]
             [(vec (distinct (map (fn [[_ _ marks _ seen :as scenario]]
                                    (or (not (get marks 2)) (not (contains? seen 2))
                                        (holds? (opened 2 scenario) :facts-by-value :l1)))
                                  points-at-scenarios)))
              (let [twelve (nth points-at-scenarios 11)
                    ;; one order of its moves, played to the end: always the first move that can be made
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
    (check "a place through her own layer and the base, the base with a setting and a permission or none, each source of the base under her layer's routing id: every list of up to three writes of the setting, the permission and the two facts, 620 lists, none with an order that ends wrong"
           [620 []] [(:lists base-place) (:wrong base-place)])
    (check "the same for an entity of the base and for the whole of the two layers, every list of up to two writes: 120 lists each, none with an order that ends wrong"
           [[120 []] [120 []]] [[(:lists base-entity) (:wrong base-entity)] [(:lists base-run) (:wrong base-run)]])
    (check "in those worlds the read holds the base's own rows, her permission and the fact as sources of the microbatch gate under her own layer's routing id, and none under the base's"
           [#{[:microbatch :facts :mine]} #{[:own-rows] [:row]}]
           (let [world (base-world :members true [] [:place :p :text])
                 of-base (filter #(= :base (:layer %)) (:held world))]
             [(set (map (juxt :gate :table :routing-id) of-base))
              (set (map (comp vector first :path) of-base))]))
    (check "the player can fail. With the mark dropped, so that no step carries an owed answer to a later one, the scenarios in which a layer joins over a farther order end wrong"
           [true true] [(contains? (set no-mark) "9") (contains? (set no-mark) "13")])
    (check "the player can fail. With told-by? counting a check that the same step has just left, the plan's own crossing ends wrong, and so do lists of the small ones"
           [true true]
           [(contains? (set (:scenarios counting-left)) "1") (pos? (:small counting-left))])
    (shown "every order of the callbacks, played on the functions that the read path calls: the thirteen scenarios of what points at a thing, through two, three and four layers,"
           (reduce + (map :states played)) "states and" (reduce + (map :ends played)) "ends; every list of up to three writes on one place in two layers,"
           (:lists small) "lists and" (:states small) "states; in each end the asker's screen is what a read asked once gives from the tables")
    (shown "the same for a place through a layer whose setting and permission change,"
           (:lists place) "lists and" (:states place) "states; for an entity," (:lists entity) "lists and" (:states entity)
           "states; and for a place, an entity and a whole layer while the start layer's list drops a layer and takes it back,"
           (reduce + (map :states lists)) "states")
    (shown "the base read under the routing id of the asker's own layer, in every order of the callbacks: the thirteen scenarios of what points at a thing, each with the base as the nearest, the second and the farthest layer,"
           (reduce + (map :states base-played)) "states and" (reduce + (map :ends base-played)) "ends; a place through her own layer and the base while the base's setting and her permission in it change,"
           (:lists base-place) "lists and" (:states base-place) "states; an entity," (:lists base-entity) "lists and" (:states base-entity)
           "states; the whole of the two layers," (:lists base-run) "lists and" (:states base-run)
           "states; in each end the asker's screen is what a read asked once gives from the tables")
    (shown "the player can fail: with the mark dropped the scenarios" (pr-str (filterv some? no-mark))
           "end wrong; with told-by? counting a check that the step has just left, the scenarios"
           (pr-str (filterv some? (:scenarios counting-left))) "and" (:small counting-left) "of the 672 small lists end wrong")
    (shown "that Rama's client delivers in these ways and no other is not shown here: the probes of this build ran it, in-process")))

(deftest the-size-of-a-run-and-the-order-of-its-keys
  (let [small (assoc numbers :max-places-of-one-run 20000)
        row (fn [held stamp] {:value held :by-act (UUID. 0 1) :by-index 0 :stamp stamp :who ":a" :permission "[:a :l :l]"})
        a (String. "\"one\"")
        b (String. "\"one\"")
        run {:gate :streaming :table :facts :layer :l :routing-id :l :path [:run ":l :e " ":l :e!"]}
        one-row {:gate :streaming :table :facts :layer :l :routing-id :l :path [:row ":l :e :k"]}]
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
    (check "the ends of a run in the facts table: an entity's run holds no place of an entity whose text begins with its text; the layer's own rows are the store's namespace on its own entity"
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
           [(some? (q {:asks [:entity (keyword (str "x" (String. (Character/toChars 0x1D49C))))] :asker :alice :layers [:team]}))
            (some? (q {:asks [:run [(keyword "x\u00E4")] [(keyword "x\u6C34")]] :asker :alice :layers [:team]}))])
    (check "the keys of a run whose end holds such a letter are held to the run's two ends, so a key that a range gave from outside them is dropped; the keys of any other run are taken as they came"
           [[":l :x\uFB00 :n"] 2]
           (let [wide (String. (Character/toChars 0x1D49C))
                 inside ":l :x\uFB00 :n"
                 outside (str ":l :x" wide " :n")
                 odd {:gate :streaming :table :facts :layer :l :routing-id :l :path [:run ":l :x\uFB00 " ":l :x\uFB00!"]}
                 plain {:gate :streaming :table :facts :layer :l :routing-id :l :path [:run ":l :e " ":l :e!"]}]
             [(vec (sources/keys-of-run odd {inside :a-row outside :a-neighbours-row}))
              (count (sources/keys-of-run plain {":l :e :a" :a-row ":l :zz :a" :not-tested}))]))
    (check "the first read of a run is made only when neither of its ends holds a letter outside the range in which the two orders agree; a run with such an end is read by its size and whole, and not before the asker's standing is known"
           [true false false true false true]
           (let [wide (String. (Character/toChars 0x1D49C))
                 a-run (fn [from to] {:gate :streaming :table :facts :layer :l :routing-id :l :path [:run from to]})
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
        made (words/word :serve-a-read-that-large {:what :places-of-one-run :most 20000 :places {:a 1} :layers [:l]})]
    (check "the read path's own eight words"
           #{:find-the-module :read-the-query :reach-the-store :get-an-answer-within-its-wait
             :read-inside-an-askers-function :serve-a-read-that-large :keep-more-reads-live :keep-the-read-live}
           (set each))
    (check "the word of the read path's own bound on its pushes is given by a read that is kept live, and by no read asked once"
           #{:read-live} (:from (get words/words :keep-more-reads-live)))
    (check "each word says what it is and which call gives it; a word that is not declared is not made"
           [true nil]
           [(every? (fn [w] (let [entry (get words/words w)] (and (string? (:says entry)) (seq (:from entry))))) each)
            (words/word :no-such-word {})])
    (check "a word holds :could-not and what it carries, and no part of an answer"
           {:could-not :serve-a-read-that-large :what :places-of-one-run :most 20000} made)
    (check "what a word can carry is declared, and so are the five reasons for which a live read ends and the two words for a layer that is left out"
           [#{:module :why :at :threw :said :waited-ms :what :most :because}
            #{:the-store-ended-a-push :a-run-grew-too-large :a-read-it-came-to-need-failed :no-room-for-a-push-it-came-to-need
              :its-function-threw}
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
    (shown "the read path's eight words are declared, each with what it says:" (pr-str (sort each))
           "| none is a gate's reason or a word of a stray; one, :find-the-module, is spelled as the door's word for its own opening")))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them."
  []
  (the-form-of-a-query)
  (a-word-is-declared-in-three-places)
  (the-layers)
  (the-sources)
  (the-loop-ends)
  (the-checks-of-nearer-layers)
  (what-a-read-gives)
  (which-changes-reach-an-asker)
  (a-step-that-joins-or-leaves-a-source-owes-the-whole-answer)
  (a-right-stands-while-its-chain-stands)
  (who-may-see-a-layer-of-three-settings)
  (every-order-of-the-callbacks)
  (the-size-of-a-run-and-the-order-of-its-keys)
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
