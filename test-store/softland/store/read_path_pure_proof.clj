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
  source is read from the maps by `table-value`. No task is played: every
  part of a run across the tasks of a microbatch layer holds the whole run.

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
  "Checks that what was observed is what must be. Gives what was observed."
  [what expected observed]
  (is (= expected observed) what)
  observed)

(def ^:private numbers "The read path's numbers, at their defaults." (query/numbers nil))

(defrecord Thing [asks asker layers])

(def ^:private task-ids
  "Four texts that stand for the routing ids of four tasks of the microbatch
  tables. The proof plays no task, so they only tell the parts of a run
  apart."
  ["t0" "t1" "t2" "t3"])

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

(def ^:private fact-row*
  "Makes the row of a fact as a gate would keep it, and remembers it. Takes
  the layer, who wrote it, the entity, the key, the value (nil for a
  retract) and the stamp. The fact is given to
  `softland.store.record.rows/fact-row` in its one form on arrival: with the
  one text of its value in its normal form, which the row keeps, and with
  the value itself under a store key alone. The row holds the texts of who
  wrote it and of a first permission of that who in the layer."
  (memoize
   (fn [layer who e k v stamp]
     (let [fact {:e e :k k :replaces nil
                 :v (when (declaration/store-key? k) v)
                 :text (when (some? v) (text/text (act/normal-value v)))}]
       (rows/fact-row fact (UUID. 0 (long stamp)) 0 stamp (text/text who) (text/text [who layer layer]))))))

(def ^:private marks-row*
  "Makes the row of a layer's marked keys, with the stamp since which each
  key's order is level, and remembers it."
  (memoize
   (fn [layer since-by-key stamp]
     (rows/with-level-since
      (fact-row* layer :maker layer :softland.store/marked-keys (set (keys since-by-key)) stamp)
      since-by-key))))

(def ^:private by-value-row*
  "Makes the row of a fact in the order by value, and remembers it."
  (memoize
   (fn [layer who stamp]
     (rows/by-value-row (text/text who) (text/text [who layer layer]) (UUID. 0 (long stamp)) 0 stamp))))

(defn- put
  "Puts a row under a key of one table."
  [tables gate table-name k row]
  (assoc tables [gate table-name] (assoc (table tables gate table-name) k row)))

(defn- write
  "Stands for a gate: puts the rows of one accepted act in the tables. Takes
  the tables and a write, one of:

    [:make layer stamp]                     the layer's depot fact
    [:setting layer word stamp]             who may see the layer
    [:permission layer holder stamp]        a permission for a holder. Its
                                            place takes one write: a second
                                            one changes nothing
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
      :permission (let [[holder stamp] more
                        k (rows/place-key layer holder :softland.store/permission)]
                    (if (get facts k)
                      tables
                      (put tables gate :facts k
                           (fact-row* layer :maker holder :softland.store/permission [holder layer layer] stamp))))
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
    (let [reading (sources/reading query known task-ids :once numbers)
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
          reading (sources/reading (:query world) known task-ids :live numbers (:held world))
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
          reading (sources/reading query known task-ids :live numbers (:held world))
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
  no permission in it."
  [k marks facts seen writes]
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
               (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers (mapv layer-n (range 1 (inc k)))}))))

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
  nearer layer at the place) or `[:far]`."
  [setting permission? writes asks]
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
    (open-live tables writes (q {:asks asks :asker :ann :layers [:near :far]}))))

(defn- every-small-standing
  "Every list of up to n writes of the setting, the permission and the two
  facts, from four starts. Takes n, what is asked, and the fault that the
  player is to make, or nil. Gives {:lists :states :wrong}."
  [n asks fault]
  (let [options [[:setting :members] [:setting :everyone] [:permission] [:near] [:far]]
        played (for [setting [:everyone :members]
                     permission? [false true]
                     writes (lists-up-to n options)]
                 (assoc (every-order (standing-world setting permission? writes asks) fault)
                        :is [setting permission? writes]))]
    {:lists (count played)
     :states (reduce + (map :states played))
     :wrong (vec (keep (fn [p] (when (:wrong p) (:is p))) played))}))

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
                                          :with #{:writer :permission} :stores-own-facts true}}
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
  (let [own (fn [layer] (table-value traces (sources/own-rows-source layer)))
        permission (fn [layer who] (table-value traces (sources/permission-source layer who)))
        from (q {:asks [:place :note-41 :text] :asker :alice :from-layer :alice-notes})
        points (q {:asks [:points-at :parent :canvas-1] :asker :bob :layers [:team]})
        with-setting (fn [word stamp] (table-value (write traces [:setting :team word stamp]) (sources/own-rows-source :team)))
        with-list (fn [listed] (table-value (write traces [:list :alice-notes listed 900]) (sources/own-rows-source :alice-notes)))
        with-mark (fn [marked] (table-value (write traces [:marks :team marked 900]) (sources/own-rows-source :team)))]
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
    (check "the standing: the seven lines of its table"
           [:not-known-yet :not-made :yes :may-not-see :not-known-yet :yes :may-not-see]
           [(layers/standing :team nil :not-known-yet true)
            (layers/standing :nowhere (own :nowhere) nil true)
            (layers/standing :base (own :base) :not-known-yet true)
            (layers/standing :team (own :team) :not-known-yet false)
            (layers/standing :team (own :team) :not-known-yet true)
            (layers/standing :team (own :team) (permission :team :bob) true)
            (layers/standing :alice-notes (own :alice-notes) (permission :alice-notes :bob) true)])
    (check "what no fact reads as: a layer with no setting is members, the base is everyone, a list is nothing, no key is marked"
           [:members :everyone [] nil]
           [(layers/who-may-see (own :team) :team) (layers/who-may-see (own :base) :base)
            (layers/builds-on (own :team) :team) (layers/marked-since (own :alice-notes) :alice-notes :parent)])
    (check "a person who is not signed in sees the base and no members layer; the system is a who like any other"
           [:yes :may-not-see :may-not-see]
           [(layers/standing :base (own :base) :not-known-yet false)
            (layers/standing :team (own :team) :not-known-yet false)
            (layers/standing :team (own :team) (permission :team :system) true)])
    (check "a setting that is written again with the same word: the row differs, and its meaning does not"
           [false true]
           (let [again (with-setting :members 900)]
             [(rows/same-row? (get (own :team) (rows/place-key :team :team :softland.store/who-may-see))
                              (get again (rows/place-key :team :team :softland.store/who-may-see)))
              (= (layers/meaning points :team (with-setting :members 800) (permission :team :bob) true)
                 (layers/meaning points :team again (permission :team :bob) true))]))
    (check "a list, and a mark of the asked key, that are written in a layer that she may not see mean the same before and after"
           [true true]
           (let [carol-from (assoc from :asker :carol)
                 carol-points (assoc points :asker :carol)]
             [(= (layers/meaning carol-from :alice-notes (own :alice-notes) nil true)
                 (layers/meaning carol-from :alice-notes (with-list [:base]) nil true))
              (= (layers/meaning carol-points :team (own :team) nil true)
                 (layers/meaning carol-points :team (with-mark #{}) nil true))]))
    (check "for an asker who may see the layer, a list that is written again and a mark that is taken off mean another thing"
           [false false]
           [(= (layers/meaning from :alice-notes (own :alice-notes) (permission :alice-notes :alice) true)
               (layers/meaning from :alice-notes (with-list [:base]) (permission :alice-notes :alice) true))
            (= (layers/meaning points :team (own :team) (permission :team :bob) true)
               (layers/meaning points :team (with-mark #{}) (permission :team :bob) true))])
    (check "a setting that goes to members for an asker who holds a permission means another thing, though her standing stays yes"
           [false :yes :yes]
           (let [everyone (with-setting :everyone 800)
                 members (with-setting :members 900)]
             [(= (layers/meaning points :team everyone (permission :team :bob) true)
                 (layers/meaning points :team members (permission :team :bob) true))
              (layers/standing :team everyone (permission :team :bob) true)
              (layers/standing :team members (permission :team :bob) true)]))
    (check "what an answer says of a layer that it left out: the layer and why, and nothing else"
           {:layer :alice-notes :why :may-not-see} (layers/left-out :alice-notes :may-not-see))
    (shown "the layers: a list gives each layer once at its first place; from a layer the read looks in the layer and then in the layers of its own fact, and no further; a start layer that she may not see gives itself alone")
    (shown "the standing: the seven lines of its table hold; a layer with no setting reads as members, the base as everyone; a row that is written again with the same word, and a list or a mark of a layer that she may not see, mean the same before and after")))

(deftest a-step-that-joins-or-leaves-a-source-owes-the-whole-answer
  (let [;; a fact's step that finds the new setting first and joins her permission
        world (standing-world :everyone true [[:setting :members] [:near]] [:place :p :text])
        after-writes (-> world (move [:write] nil) (move [:write] nil))
        setting (sources/own-rows-source :near)
        fact (sources/place-source :near :p :text)
        both-taken (-> after-writes (move [:take setting] nil) (move [:take fact] nil))
        fact-first (move both-taken [:handle fact] nil)
        permission (sources/permission-source :near :ann)
        given-her (-> fact-first (move [:serve permission] nil) (move [:arrive permission] nil))
        ;; a step that leaves an order whose mark was taken off
        marked (reduce write {} [[:make :l1 1] [:setting :l1 :everyone 2] [:marks :l1 #{:parent} 3]
                                 [:fact :l1 :maker :p :parent :canvas-1 11]])
        open (open-live marked [[:marks :l1 #{} 100]] (q {:asks [:points-at :parent :canvas-1] :asker :ann :layers [:l1]}))
        own (sources/own-rows-source :l1)
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
        own-rows (fn [layer] (table-value traces (sources/own-rows-source layer)))
        of (fn [asks layer] ((get sources/of-word (first asks)) (q (merge alice {:asks asks :layers [layer]})) layer (own-rows layer) task-ids))]
    (check "the layer's own rows: one run of the layer's own entity under the store's namespace; in the base every task holds it"
           [{:gate :streaming :table :facts :layer :team :routing-id :team
             :path [:own-rows ":team :team :softland.store/" ":team :team :softland.store0"]}
            {:gate :microbatch :table :facts :layer :base :routing-id :base :every-task true
             :path [:own-rows ":base :base :softland.store/" ":base :base :softland.store0"]}]
           [(sources/own-rows-source :team) (sources/own-rows-source :base)])
    (check "her permission: one row, on its holder"
           [{:gate :streaming :table :facts :layer :team :routing-id :team :path [:row ":team :alice :softland.store/permission"]}
            {:gate :microbatch :table :facts :layer :base :routing-id :base :every-task true
             :path [:row ":base :alice :softland.store/permission"]}]
           [(sources/permission-source :team :alice) (sources/permission-source :base :alice)])
    (check "a place: the layer routes a streaming layer, the entity a microbatch layer"
           [[{:gate :streaming :table :facts :layer :team :routing-id :team :path [:row ":team :note-41 :text"]}]
            [{:gate :microbatch :table :facts :layer :base :routing-id :note-41 :path [:row ":base :note-41 :text"]}]]
           [(of [:place :note-41 :text] :team) (of [:place :note-41 :text] :base)])
    (check "an entity: one run, on the entity's task in the base"
           [[{:gate :streaming :table :facts :layer :team :routing-id :team :path [:run ":team :note-41 " ":team :note-41!"]}]
            [{:gate :microbatch :table :facts :layer :base :routing-id :note-41 :path [:run ":base :note-41 " ":base :note-41!"]}]]
           [(of [:entity :note-41] :team) (of [:entity :note-41] :base)])
    (check "a run of keys of one entity is on that entity's task; a run across entities of the base is one source for each task"
           [[:note-41] ["t0" "t1" "t2" "t3"] [[:run ":base :a " ":base :b!"]] [:team]]
           [(mapv :routing-id (of [:run [:note-41 :a] [:note-41 :z]] :base))
            (mapv :routing-id (of [:run [:a] [:b]] :base))
            (vec (distinct (map :path (of [:run [:a] [:b]] :base))))
            (mapv :routing-id (of [:run [:a] [:b]] :team))])
    (check "the ends of a run: a place is in the run that ends at it; a whole layer is a run and no table"
           [[:run ":team :e :a" ":team :e :m "] [:run ":team " ":team!"] [:run ":team :e " ":team!"]]
           (mapv #(:path (first (of % :team))) [[:run [:e :a] [:e :m]] [:run :first :last] [:run [:e] :last]]))
    (check "what points at a thing: the thing's run in the facts by value, by the stamp of the mark; the value's digest routes the base; a layer that does not mark the key reads nothing"
           [[{:gate :streaming :table :facts-by-value :layer :team :routing-id :team
              :path [:run (str ":team :parent 700 " digest " ") (str ":team :parent 700 " digest "!")]}]
            [{:gate :microbatch :table :facts-by-value :layer :base :routing-id digest
              :path [:run (str ":base :parent 300 " digest " ") (str ":base :parent 300 " digest "!")]}]
            []]
           [(of [:points-at :parent :canvas-1] :team) (of [:points-at :parent :canvas-1] :base)
            (of [:points-at :parent :canvas-1] :alice-notes)])
    (check "a check of a nearer layer at a place is the source of that place"
           (sources/place-source :team :block-1 :parent) (sources/check-source :team :block-1 :parent))
    (check "two askers of one thing who ask for different parts have equal sources"
           true
           (let [one (q {:asks [:place :note-41 :text] :asker :alice :layers [:team :base] :with #{:writer}})
                 two (q {:asks [:place :note-41 :text] :asker :alice :layers [:team :base] :stores-own-facts true})]
             (= (:wanted (sources/reading one {} nil :once numbers)) (:wanted (sources/reading two {} nil :once numbers)))))
    (check "which sources are a layer's rights, and which a check"
           [:own-rows :permission nil true false]
           (let [points (q (merge alice {:asks [:points-at :parent :canvas-1] :layers [:team :base]}))]
             [(sources/rights-kind points (sources/own-rows-source :team))
              (sources/rights-kind points (sources/permission-source :team :alice))
              (sources/rights-kind points (sources/place-source :team :block-1 :parent))
              (sources/check? points (sources/check-source :team :block-1 :parent))
              (sources/check? points (sources/permission-source :team :alice))]))
    (check "only a run whose two ends do not name one entity needs the routing ids of the tasks"
           [true false false false]
           (mapv #(sources/needs-task-routing-ids? (q (merge alice {:asks % :layers [:base]})))
                 [[:run [:a] [:b]] [:run [:a :k] [:a :m]] [:entity :a] [:place :a :k]]))
    (shown "the sources of each word in a streaming layer and in the base hold the table, the routing id and the path that the record's page gives: the layer for a streaming layer; in the base the entity for a place and an entity, one source for each task for a run across entities, the value's digest for what points at a thing")
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
    (check "asked once: an entity of the base is one call, with the rights riding it; a run across entities of the base is one call for each task"
           [[1 [1]] [1 [4]]]
           (mapv once [entity-base run-base]))
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
    (check "the calls of one round: a source that every task holds rides a call of its layer"
           [[:note-41 3]]
           (mapv (juxt :routing-id (comp count :sources))
                 (sources/calls-for-once (:missing (sources/reading entity-base {} nil :once numbers)) numbers)))
    (check "the calls of one round are cut at the greatest count of paths of one call"
           [2 2 1]
           (mapv (comp count :sources)
                 (sources/calls-for-once (map #(sources/place-source :team (keyword (str "e" %)) :k) (range 5))
                                         (assoc numbers :max-paths-in-one-call 2))))
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
                           (let [r (sources/reading query known task-ids mode numbers)]
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
                    {:value "(not a value" :by-act (UUID. 0 9) :by-index 0 :stamp 331
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
    (check "a row that several parts of a run of the base hold is given once: the one with the latest stamp; a place of one part comes from that part"
           [7 3 nil]
           (let [parts [{"k" {:stamp 5} "a" {:stamp 3}} {"k" {:stamp 7}} {} {"k" {:stamp 6}}]]
             [(:stamp (answer/one-of-each parts "k")) (:stamp (answer/one-of-each parts "a")) (answer/one-of-each parts "none")]))
    (check "what points at a thing says, for each layer that she may see, since which stamp its order is level, or that it marks nothing"
           [{:layer :alice-notes :not-marked true} {:layer :team :level-since 700} {:layer :base :level-since 300}]
           (:orders (ask :alice [:points-at :parent :canvas-1] [:alice-notes :team :base])))
    (shown "what a read gives: the four things that a place can hold are told apart; the nearest layer that holds a fact wins, also with nothing here; each place of a run comes from the nearest layer that holds it; a layer that is left out is named with why and nothing of it is given")
    (shown "a place-answer holds no writer and no permission unless asked; the store's own facts are in no answer that did not ask for them; a row that every task of the base holds is given once, the latest")))

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
           [(answer/told-by? alice-3 r known (sources/place-source :alice-notes :note-41 :text) place)
            (answer/told-by? alice-3 r known (sources/place-source :team :note-41 :text) place)
            (answer/told-by? alice-3 r known (sources/place-source :base :note-41 :text) place)])
    (check "a change in a layer that she may not see is not told; for her the next layer wins, and its change is told"
           [false true]
           [(answer/told-by? bob-3 bob-r bob-known (sources/place-source :alice-notes :note-41 :text) place)
            (answer/told-by? bob-3 bob-r bob-known (sources/place-source :team :note-41 :text) place)])
    (check "a change of a store key's row is told only to an asker who asked for the store's own facts"
           [false true]
           [(answer/told-by? holder holder-r holder-known (first (get-in holder-r [:by-layer :team :facts])) [:bob :softland.store/permission])
            (answer/told-by? asked asked-r asked-known (first (get-in asked-r [:by-layer :team :facts])) [:bob :softland.store/permission])])
    (check "the places that a change names for a query: the asked place from its row and from no other; each key of a run; no place from a layer's own rows"
           [#{[:note-41 :text]} #{} #{[:bob :softland.store/permission]} #{}]
           [(sources/places-of alice-3 (sources/place-source :team :note-41 :text) [":team :note-41 :text"])
            (sources/places-of alice-3 (sources/permission-source :team :alice) [":team :alice :softland.store/permission"])
            (sources/places-of holder (first (get-in holder-r [:by-layer :team :facts])) [":team :bob :softland.store/permission" "no key"])
            (sources/places-of holder (sources/own-rows-source :team) [":team :team :softland.store/depot"])])
    (shown "which changes reach an asker: a change in the layer that wins the place; not one under a nearer row, not one in a layer that she may not see, and one of a store key's row only when she asked for the store's own facts")))

(deftest every-order-of-the-callbacks
  (let [played (mapv (fn [[what k marks facts seen writes]]
                       (assoc (every-order (points-at-world k marks facts seen writes) nil) :what what))
                     points-at-scenarios)
        small (every-small-points-at nil)
        place (every-small-standing 3 [:place :p :text] nil)
        entity (every-small-standing 2 [:entity :p] nil)
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
                                                                     (sources/check-source nearer (first place) (second place)))))
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
