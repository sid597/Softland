(ns run-again.smoke.part-b
  "A smoke run of part B of the build \"run again\", on the store itself:
  the base's gate. Both depots place a thing by its layer, one task decides
  a layer in the depot's order with the walk that both gates call, the rows
  of a microbatch layer's facts are copied to every task, the module has
  two depots, the door's two reads read one row on the layer's task, and a
  second module over the same two depots makes the same tables.

  It is no proof: the store's proofs are in test-store/. It starts `Store`
  in Rama's in-process cluster with 4 tasks, 2 task threads and 2 workers,
  opens the door, and plays a hand beside it: it appends things to the two
  depots with an act-id, a depot word and a time that it writes itself, and
  it selects the tables straight from the PStates, one task at a time.

  What it prints. One line for each thing that it shows, behind the word
  SMOKE and the letter of the point, a to i, with `| check: held` or
  `| check: NOT HELD` at its end. A check that did not hold also prints a
  line `SMOKE FAILED <what>`. A line that only tells a number has no check
  and says so. The last line is `SMOKE done` when the run reached its end.
  The JVM ends with status 0 only when it did and every check held.

  The points:

    a  through the door: the making act of the base, a give to another
       holder, and her first act under the given permission, sent one after
       the other; and how long the last answer took
    b  by hand: a chain of 200 acts into the base in one batch, each
       replacing the fact of the act before it at one of 10 places
    c  the copies: the facts and the facts by value on every task, the
       answers and the last stamps on their layer's task
    d  a mark and the facts after it, a replace and a retract, in one batch
    e  an act for a layer of the other gate, on each depot, before and
       after the layer is made
    f  a streaming layer through the door
    g  the module's depots, by name
    h  the door's two reads
    i  the run again: a second module over mirrors of the two depots

  Run it behind the shared lock, from the repository root, with the output
  in a file, and read the file only by grep for ^SMOKE and for Exception:

    flock src/proposal/backend-on-rama-2026-10-02/before-real-data/runs/cluster.lock \\
      clojure -M:store-module -e '(load-file \"src/proposal/backend-on-rama-2026-10-02/run-again/smoke/part_b.clj\")' \\
      > src/proposal/backend-on-rama-2026-10-02/run-again/smoke/raw-part-b.txt 2>&1

  State that it changes: none outside its own in-process cluster, which it
  closes. It writes no file."
  (:require [clojure.string :as str]
            [clojure.walk :as cwalk]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.text :as text]
            [softland.store.gate.stamp :as stamp]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

;; ---------------------------------------------------------- the second module

;; The gates and the reads of `Store` over mirrors of its two depots. Its
;; docstring is set below: `defmodule` takes none.
(rama/defmodule StoreAgain [setup topologies]
  (rama/mirror-depot setup *streaming-depot (rama/get-module-name module/Store) "*streaming-depot")
  (rama/mirror-depot setup *microbatch-depot (rama/get-module-name module/Store) "*microbatch-depot")
  (module/add-gates-and-reads topologies))

(alter-meta! #'StoreAgain assoc
             :doc "The second module of the smoke run: a mirror of each of the two depots of
  `Store`, under the depot's own name, and behind them everything else of
  `Store` by the store's one definition
  (`softland.store.module/add-gates-and-reads`). It stores no act. What must
  hold at its launch: `Store` runs in the same cluster, and both have the
  same number of tasks.")

;; ---------------------------------------------------------------- the lines

(defonce ^{:private true :doc "What each check that did not hold was about, in the order printed."}
  failed (atom []))

(defn- shown
  "Takes the things of a line. Gives them as one text, each behind a space:
  a string as it is, anything else printed readably."
  [things]
  (apply str (map #(str " " (if (string? %) % (pr-str %))) things)))

(defn- line!
  "Prints one line with a check: SMOKE, the letter of the point, what the
  line shows, the things given, and whether the check held. When it did
  not, prints a line SMOKE FAILED too and keeps it. Gives whether it held."
  [point what held? & things]
  (println (str "SMOKE " point " " what (shown things) " | check: " (if held? "held" "NOT HELD")))
  (when-not held?
    (println (str "SMOKE FAILED " point " " what))
    (swap! failed conj [point what]))
  (flush)
  (boolean held?))

(defn- tell!
  "Prints one line that has no check: SMOKE, the letter of the point, what
  the line tells, and the things given."
  [point what & things]
  (println (str "SMOKE " point " " what (shown things) " | no check"))
  (flush))

;; ------------------------------------------------------------------ the hand

(def ^:private tasks "The number of tasks of each of the two modules." 4)

(def ^:private index-keys
  "For each task, a key that Rama's hash partitioner sends to it: the i-th
  to task i. A select with one of them as `:pkey` reads that task's part of
  a table."
  (vec (rtest/gen-hashing-index-keys tasks)))

(def ^:private wait-ms "How long the run waits for one thing, in milliseconds." 300000)

(def ^:private gate-name "The name of the microbatch gate's topology." "microbatch-gate")

(def ^:private table-names
  "The names of the four tables of each gate."
  {:streaming {:answers "$$streaming-answers" :facts "$$streaming-facts"
               :facts-by-value "$$streaming-facts-by-value" :task "$$streaming-task"}
   :microbatch {:answers "$$microbatch-answers" :facts "$$microbatch-facts"
                :facts-by-value "$$microbatch-facts-by-value" :task "$$microbatch-task"}})

(defn- open-tables
  "Takes the cluster and a module's name. Gives the handles of its eight
  tables: gate -> table -> handle."
  [ipc module-name]
  (into {} (for [[gate names] table-names]
             [gate (into {} (for [[table table-name] names]
                              [table (rama/foreign-pstate ipc module-name table-name)]))])))

(defn- open-hand
  "Takes the cluster and the name of the first module, which is launched.
  Gives what the run works with: the cluster, the module's name, the door,
  the module's two depots and its tables. The door is opened with a short
  sleep between two reads of a wait, so that the time of point a is not
  the door's own sleep. Throws when the door gives a word."
  [ipc module-name]
  (let [d (door/open ipc module-name {:read-every-ms 5})]
    (when (:could-not d)
      (throw (ex-info "the door could not be opened" {:word d})))
    {:ipc ipc
     :module module-name
     :door d
     :streaming-depot (rama/foreign-depot ipc module-name "*streaming-depot")
     :microbatch-depot (rama/foreign-depot ipc module-name "*microbatch-depot")
     :tables (open-tables ipc module-name)}))

(def ^:private t-past
  "A time before the run, in milliseconds, for the hand's things of point e."
  1760000000000)

(def ^:private t-ahead
  "A time one hour after the start of the run, in milliseconds. The hand's
  acts into the base carry times from here on, so that each is later than
  the time in the base's last stamp, which point a made of the door's
  clock, and the stamp of each holds its own time."
  (+ (System/currentTimeMillis) 3600000))

(def ^:private system-own "The system's permission in the base, the base's first." [:system :base :base])

(def ^:private alice-in-base "Alice's permission in the base, beneath the system's." [:alice :base :base system-own])

(defn- thing
  "A thing as the hand appends it, with no door: a maker's act with an
  act-id that the hand makes for the layer under the depot word given, that
  word as `:depot`, and the time given as the time on the act."
  [depot layer who permission facts door-time]
  (assoc {:act-id [layer depot (UUID/randomUUID)] :depot depot
          :version 1 :who who :layer layer :permission permission :session :smoke
          :facts (vec facts)}
         declaration/time-part door-time))

(defn- system-act
  "An act of the system into the base, for the hand to append to the
  microbatch depot."
  [facts door-time]
  (thing :microbatch :base :system system-own facts door-time))

(defn- making-of
  "The making act of a layer by its holder, for the hand to append to the
  depot given: the layer's depot, and the holder's first permission."
  [depot layer holder door-time]
  (let [first-permission [holder layer layer]]
    (thing depot layer holder first-permission
           [{:e layer :k :softland.store/depot :v depot}
            {:e holder :k :softland.store/permission :v first-permission}]
           door-time)))

(defn- log-of
  "Each record of a depot's log, as [partition record], in the order of the
  partitions and of each partition's offsets."
  [depot]
  (vec (for [p (range (:num-partitions (rama/foreign-object-info depot)))
             :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
             r (rama/foreign-depot-read depot p start-offset end-offset)]
         [p r])))

(defn- records-in
  "How many records a depot holds, over its partitions."
  [depot]
  (reduce + (for [p (range (:num-partitions (rama/foreign-object-info depot)))
                  :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]]
              (- end-offset start-offset))))

(defn- to-microbatch!
  "Appends things to the microbatch depot, one after the other from this
  thread, each waiting only until the depot holds it."
  [hand things]
  (doseq [x things]
    (rama/foreign-append! (:microbatch-depot hand) x :append-ack)))

(defn- settle!
  "Waits until the first module's microbatch gate has processed each record
  that its depot holds, the door's and the hand's. Rama's test tool, which
  throws when `wait-ms` passes first."
  [hand]
  (rtest/wait-for-microbatch-processed-count (:ipc hand) (:module hand) gate-name
                                             (records-in (:microbatch-depot hand)) wait-ms))

(defn- in-one-batch!
  "Appends things to the microbatch depot while the first module's gate is
  paused, lets the gate go on, and waits until it has processed them.
  Rama's test docs say that the next batch then holds every record that
  was appended meanwhile, up to 1000 for a partition."
  [hand things]
  (rtest/pause-microbatch-topology! (:ipc hand) (:module hand) gate-name)
  (to-microbatch! hand things)
  (rtest/resume-microbatch-topology! (:ipc hand) (:module hand) gate-name)
  (settle! hand))

(defn- to-streaming!
  "Appends a thing to the streaming depot with the ack level :ack, which
  waits for the streaming gate's event."
  [hand x]
  (rama/foreign-append! (:streaming-depot hand) x :ack))

(defn- answer-row
  "Takes the hand, a gate's word and an act-id. Gives the row under the
  act-id in that gate's answers, read on the layer's task, or nil."
  [hand gate act-id]
  (let [[layer _ uuid] act-id]
    (rama/foreign-select-one (path/keypath (record/answer-key layer uuid))
                             (get-in hand [:tables gate :answers]) {:pkey layer})))

(defn- word
  "Takes an answer's row or nil. Gives one word: :accepted, the reason of a
  refusal, or :nothing-yet."
  [row]
  (cond (nil? row) :nothing-yet
        (:refused row) (:refused row)
        :else :accepted))

(defn- stamp-parts
  "The two halves of a stamp, for a line: [time counter]; nil for nil."
  [s]
  (when s [(stamp/time-of s) (stamp/counter-of s)]))

(defn- door-time-of
  "Takes a depot and an act-id. Gives the time on the first thing that the
  depot's log holds under the act-id, or nil."
  [depot act-id]
  (some (fn [[_ r]] (when (and (map? r) (= act-id (:act-id r))) (get r declaration/time-part)))
        (log-of depot)))

(defn- task-of-each-layer
  "Takes a depot. Gives, for each layer that a record of the depot names, the
  text of the layer id and the partition that holds its records: what a
  client can see of where the depot's partitioner put a layer."
  [depot]
  (into {} (keep (fn [[p r]] (when (and (map? r) (some? (:layer r)))
                               [(declaration/id-text (:layer r)) p])))
        (log-of depot)))

;; ------------------------------------------------------- what the tables hold

(defn- bytes-as-hex
  "Takes plain data. Gives it with each run of bytes as its hex text, so
  that `=` compares two of them: an answer's row holds its digest as bytes."
  [x]
  (cwalk/postwalk (fn [part] (if (bytes? part) (text/hex part) part)) x))

(defn- task-table-held
  "What a gate keeps for one task, as rows: under [:last-stamp layer-text]
  the last stamp of each layer, and under [:strays word] the set of the
  digests of the strays of that word."
  [handle k]
  (let [stamps (into {} (map (fn [[layer n]] [[:last-stamp layer] n]))
                     (rama/foreign-select [(path/keypath :last-stamp) path/ALL] handle {:pkey k}))
        words (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] handle {:pkey k})]
    (into stamps
          (keep (fn [w]
                  (let [digests (set (rama/foreign-select [(path/keypath :strays w) path/ALL] handle {:pkey k}))]
                    (when (seq digests) [[:strays w] digests]))))
          words)))

(defn- tables-held
  "Takes a module's handles. Gives every row of its eight tables on every
  task, selected straight from the PStates, with each run of bytes as hex
  text: gate -> table -> task -> key -> row. One select of a task's whole
  part for each of the three tables of rows; the tables of this run fit in
  memory."
  [handles]
  (bytes-as-hex
   (into {}
         (for [[gate tables] handles]
           [gate (into {}
                       (for [[table handle] tables]
                         [table (into (sorted-map)
                                      (map-indexed
                                       (fn [task k]
                                         [task (if (= :task table)
                                                 (task-table-held handle k)
                                                 (into {} (rama/foreign-select [path/ALL] handle {:pkey k})))])
                                       index-keys))]))]))))

(defn- rows-by-task
  "Takes one table as `tables-held` gives it, task -> key -> row. Gives how
  many rows each task holds, in the order of the tasks."
  [by-task]
  (mapv count (vals by-task)))

(defn- layer-text-of
  "Takes a key of a table as `tables-held` gives it: the text of a row's
  key, or [:last-stamp layer-text], or [:strays word]. Gives the text of
  the layer that the key begins with, or nil for a stray's."
  [k]
  (cond (string? k) (first (str/split k #" " 2))
        (= :last-stamp (first k)) (second k)
        :else nil))

(defn- the-copies
  "Reads the four tables of the first module's microbatch gate on every
  task and says what holds of them. Takes the hand. Gives:

    :facts-same?     every task holds the same map of facts, and it has rows
    :by-value-same?  every task holds the same map of the facts by value
    :on-its-layers-task?
                     each answer, and each layer's last stamp, is on the
                     task of the depot partition that holds the layer
    :held            the four tables, table -> task -> key -> row"
  [hand]
  (let [held (:microbatch (tables-held {:microbatch (get-in hand [:tables :microbatch])}))
        task-of (task-of-each-layer (:microbatch-depot hand))
        facts (vals (:facts held))]
    {:facts-same? (and (apply = facts) (pos? (count (first facts))))
     :by-value-same? (apply = (vals (:facts-by-value held)))
     :on-its-layers-task? (every? (fn [table]
                                    (every? (fn [[task rows]]
                                              (every? (fn [k] (let [layer (layer-text-of k)]
                                                                (or (nil? layer) (= task (get task-of layer)))))
                                                      (keys rows)))
                                            (get held table)))
                                  [:answers :task])
     :held held}))

;; -------------------------------------------------------------------- a to i

(defn- point-e-before!
  "The first half of e, before any layer is made. Appends by hand an act
  for the base to the streaming depot, and an act for another layer, which
  starts with the streaming depot, to the microbatch depot. Gives the word
  of each answer: {:base-on-streaming word, :erin-on-microbatch word}."
  [hand]
  (let [base-before (making-of :streaming :base :system t-past)
        erin-before (making-of :microbatch :erin :erin t-past)]
    (to-streaming! hand base-before)
    (to-microbatch! hand [erin-before])
    (settle! hand)
    {:base-on-streaming (word (answer-row hand :streaming (:act-id base-before)))
     :erin-on-microbatch (word (answer-row hand :microbatch (:act-id erin-before)))}))

(defn- point-a!
  "a. The base through the door. Sends the making act of the base, a give
  to Alice and Alice's first act under the given permission, one after the
  other with no read between them, then reads each answer with `answer-of`.
  The check: each send was acked `:depot-holds`; all three accepted; the
  stamps strictly increasing in the order sent; and the time in each stamp
  is the time that the door wrote on its thing. It tells how long the
  answer of the last took, from the return of its `send-act`."
  [hand]
  (let [d (:door hand)
        making {:version 1 :who :system :layer :base :permission system-own :session :smoke
                :facts [{:e :base :k :softland.store/depot :v :microbatch}
                        {:e :system :k :softland.store/permission :v system-own}]}
        give {:version 1 :who :system :layer :base :permission system-own :session :smoke
              :facts [{:e :alice :k :softland.store/permission :v alice-in-base}]}
        alices {:version 1 :who :alice :layer :base :permission alice-in-base :session :smoke
                :facts [{:e :alices-doc :k :text :v "alice's first act in the base"}]}
        sent (mapv #(door/send-act d %) [making give alices])
        returned (System/nanoTime)
        last-answer (door/answer-of d (:act-id (peek sent)) (:digest (peek sent)) wait-ms)
        took-ms (quot (- (System/nanoTime) returned) 1000000)
        answers (conj (mapv #(door/answer-of d (:act-id %) (:digest %) wait-ms) (pop sent)) last-answer)
        door-times (mapv #(door-time-of (:microbatch-depot hand) (:act-id %)) sent)
        stamps (mapv :stamp answers)]
    (line! "a" "through the door: the making act of the base, a give to another holder, and her first act under the given permission, sent one after the other with no read between them:"
           (and (= [:depot-holds :depot-holds :depot-holds] (mapv :ack sent))
                (= [:accepted :accepted :accepted] (mapv :answer answers))
                (every? some? stamps)
                (apply < stamps)
                (= door-times (mapv stamp/time-of stamps)))
           :acks (mapv :ack sent) :answers (mapv :answer answers)
           :stamps-as-time-and-counter (mapv stamp-parts stamps)
           :door-times-on-the-depot door-times)
    (tell! "a" "from the return of the last send-act to its answer, ms:" took-ms
           "(the door read every 5 ms; an order of size, one run)")))

(defn- chain
  "Takes a number of acts, a number of places, a key, a function from an
  act's index to its value and one to its time. Gives the acts of the
  system into the base in their order: act i writes one fact at place
  (i mod places) under the key, and from the second act of a place on it
  names the fact of the act before it at that place as the one to replace."
  [n places k value-of time-of]
  (reduce (fn [acts i]
            (let [before (when (>= i places) (nth acts (- i places)))]
              (conj acts (system-act [(cond-> {:e (keyword (str (name k) "-place-" (mod i places))) :k k :v (value-of i)}
                                        before (assoc :replaces [(:act-id before) 0]))]
                                     (time-of i)))))
          []
          (range n)))

(defn- in-the-depots-order
  "Takes the hand and acts that were appended to the microbatch depot.
  Gives their act-ids in the order in which the depot's log holds them."
  [hand acts]
  (let [ids (set (map :act-id acts))]
    (vec (keep (fn [[_ r]] (when (and (map? r) (contains? ids (:act-id r))) (:act-id r)))
               (log-of (:microbatch-depot hand))))))

(defn- point-b!
  "b. A chain by hand. Appends 200 acts of the system into the base with no
  wait between them, while the gate is paused: act i writes place
  (i mod 10) and replaces the fact of act i - 10. Three acts in a row carry
  one time. The check: every one accepted; the stamps strictly increasing
  in the depot's order; the depot's order the order sent; and each stamp
  its act's time, with a counter that counts the acts of one time. Gives
  the acts."
  [hand]
  (let [n 200
        tb (+ t-ahead 1000)
        acts (chain n 10 :text #(str "value " %) #(+ tb (quot % 3)))
        _ (in-one-batch! hand acts)
        by-key (into {} (rama/foreign-select [path/ALL] (get-in hand [:tables :microbatch :answers]) {:pkey :base}))
        row-of (fn [x] (get by-key (record/answer-key :base (nth (:act-id x) 2))))
        stamps (mapv (comp :stamp row-of) acts)
        stamp-of (zipmap (map :act-id acts) stamps)
        order (in-the-depots-order hand acts)
        in-order (mapv stamp-of order)
        expected (mapv #(stamp/pack (+ tb (quot % 3)) (rem % 3)) (range n))
        increasing? (and (= n (count in-order)) (every? some? in-order) (apply < in-order))]
    (line! "b" "by hand, with chosen times and no wait: a chain of 200 acts of the system into the base, each replacing the fact of the act before it at one of 10 places, appended while the gate was paused:"
           (and (= {:accepted n} (frequencies (map (comp word row-of) acts)))
                increasing?
                (= (mapv :act-id acts) order)
                (= expected stamps))
           :answers (frequencies (map (comp word row-of) acts))
           :stamps-strictly-increasing-in-the-depots-order increasing?
           :the-depots-order-is-the-order-sent (= (mapv :act-id acts) order)
           :each-stamp-is-its-acts-time-with-a-counter (= expected stamps)
           :first-and-last (mapv stamp-parts [(first stamps) (peek stamps)]))
    acts))

(defn- point-c!
  "c. The copies. Reads the microbatch gate's four tables on every task.
  The checks: the facts are the same map on all four tasks; the facts by
  value too; each answer and each layer's last stamp is on the task of its
  layer's depot partition, and the base's task holds answers; and each
  place of the chain of b holds, on every task, the row of the last act of
  that place."
  [hand chain-acts]
  (let [{:keys [facts-same? by-value-same? on-its-layers-task? held]} (the-copies hand)
        task-of (task-of-each-layer (:microbatch-depot hand))
        base-task (get task-of (declaration/id-text :base))
        last-ten (take-last 10 chain-acts)
        last-rows? (every? (fn [rows]
                             (every? (fn [x]
                                       (let [fact (first (:facts x))
                                             row (get rows (record/place-key :base (:e fact) (:k fact)))]
                                         (and (= (nth (:act-id x) 2) (:by-act row))
                                              (= [:value (:v fact)] (record/read-value row)))))
                                     last-ten))
                           (vals (:facts held)))]
    (line! "c" "$$microbatch-facts holds the same map on all four tasks:" facts-same?
           :rows-on-each-task (rows-by-task (:facts held)))
    (line! "c" "$$microbatch-facts-by-value holds the same map on all four tasks (no key is marked yet, so it holds no row; d reads it with rows):"
           by-value-same? :rows-on-each-task (rows-by-task (:facts-by-value held)))
    (line! "c" "$$microbatch-answers and $$microbatch-task hold rows only on the task of their layer:"
           (and (some? base-task) on-its-layers-task?
                (pos? (count (get-in held [:answers base-task]))))
           :task-of-each-layer task-of
           :answers-on-each-task (rows-by-task (:answers held))
           :rows-of-the-tasks-table-on-each-task (mapv (comp vec sort keys) (vals (:task held))))
    (line! "c" "each of the 10 places of the chain holds, on every task, the row of the last act of that place, with its value:"
           last-rows?)))

(defn- point-d!
  "d. A mark and the facts after it, with no wait, while the gate is paused.
  Appends: an act that marks the key `:on`; the next act, which writes that
  key on two entities, `:d1` and `:d3`; an act that writes it on `:d2`; an
  act that replaces the fact of `:d1` with another value; an act that
  retracts the fact of `:d2`. `:d3` is not touched again: its entry,
  written by the act straight after the mark, shows that the mark held from
  the next act. The checks: all five accepted; the marked key has the stamp
  of the marking act on every task; and on every task the order by value
  holds exactly two entries, `:d3` under the first value and `:d1` under
  the second, each written by its act."
  [hand]
  (let [td (+ t-ahead 2000)
        mark (system-act [{:e :base :k :softland.store/marked-keys :v #{:on}}] td)
        after (system-act [{:e :d1 :k :on :v :target-1} {:e :d3 :k :on :v :target-1}] (+ td 1))
        second-fact (system-act [{:e :d2 :k :on :v :target-1}] (+ td 2))
        replace (system-act [{:e :d1 :k :on :v :target-2 :replaces [(:act-id after) 0]}] (+ td 3))
        retract (system-act [{:e :d2 :k :on :replaces [(:act-id second-fact) 0]}] (+ td 4))
        acts [mark after second-fact replace retract]
        _ (in-one-batch! hand acts)
        rows (mapv #(answer-row hand :microbatch (:act-id %)) acts)
        since (:stamp (first rows))
        {:keys [facts-same? by-value-same? on-its-layers-task? held]} (the-copies hand)
        entry (fn [x index value entity]
                [(record/by-value-key :base :on since (record/value-digest-of value) entity)
                 {:by-act (nth (:act-id x) 2) :by-index index}])
        expected (into {} [(entry after 1 :target-1 :d3) (entry replace 0 :target-2 :d1)])
        entries-on (fn [rows] (into {} (map (fn [[k row]] [k (select-keys row [:by-act :by-index])])) rows))
        marks-on (fn [rows] (record/marked-keys-of (get rows (record/place-key :base :base :softland.store/marked-keys))))
        d2-on (fn [rows] (record/read-value (get rows (record/place-key :base :d2 :on))))]
    (line! "d" "a mark and the facts after it, with no wait, in one batch: an act marks a key, the next act writes it on two entities, an act writes it on a third, one fact is replaced with another value, one is retracted:"
           (= (repeat 5 :accepted) (map word rows))
           :answers (mapv word rows))
    (line! "d" "the marked key has the stamp of the act that marked it, on every task:"
           (and (some? since) (every? #(= {:on since} (marks-on %)) (vals (:facts held))))
           :stamp-of-the-marking-act (stamp-parts since)
           :marked-keys-on-each-task (mapv marks-on (vals (:facts held))))
    (line! "d" "on every task the order by value holds exactly the entries of the last state: the entry that the act straight after the mark wrote, and the entry of the new value; the replaced value's entry and the retracted one are out:"
           (and (every? #(= expected (entries-on %)) (vals (:facts-by-value held)))
                (every? #(= [:nothing-here] (d2-on %)) (vals (:facts held))))
           :entries-on-each-task (rows-by-task (:facts-by-value held))
           :expected-entries (count expected)
           :the-retracted-place-reads (mapv d2-on (vals (:facts held))))
    (line! "d" "the facts and the facts by value are the same rows on all four tasks, and each answer and last stamp is on its layer's task:"
           (and facts-same? by-value-same? on-its-layers-task?)
           :facts-on-each-task (rows-by-task (:facts held)))))

(defn- point-e!
  "e. A layer of the other gate. Takes the hand and the two words of
  `point-e-before!`. Makes the other layer by the streaming gate; appends
  an act for it to the microbatch depot again; and appends an act for the
  base to the streaming depot again, the base being made by then. The
  check: each of the four is refused `:layer-belongs-to-the-other-gate`,
  and both layers are made, each by its own gate."
  [hand before]
  (let [erin-made (making-of :streaming :erin :erin (+ t-past 1))
        _ (to-streaming! hand erin-made)
        erin-after (making-of :microbatch :erin :erin (+ t-past 2))
        _ (to-microbatch! hand [erin-after])
        _ (settle! hand)
        base-after (making-of :streaming :base :system (+ t-past 3))
        _ (to-streaming! hand base-after)
        base-made? (some? (rama/foreign-select-one
                           (path/keypath (record/place-key :base :base :softland.store/depot))
                           (get-in hand [:tables :microbatch :facts]) {:pkey :base}))
        words {:for-the-base-on-the-streaming-depot
               [(:base-on-streaming before) (word (answer-row hand :streaming (:act-id base-after)))]
               :for-a-streaming-layer-on-the-microbatch-depot
               [(:erin-on-microbatch before) (word (answer-row hand :microbatch (:act-id erin-after)))]}]
    (line! "e" "an act for a layer of the other gate, on each depot, before and after the layer is made by its own gate: one word, four times:"
           (and (= (repeat 4 :layer-belongs-to-the-other-gate) (mapcat val words))
                base-made?
                (= :accepted (word (answer-row hand :streaming (:act-id erin-made)))))
           :words-before-and-after words
           :the-base-is-made-by-the-microbatch-gate base-made?
           :the-streaming-layer-is-made-by-the-streaming-gate (word (answer-row hand :streaming (:act-id erin-made))))))

(defn- point-f!
  "f. A streaming layer through the door: its making act, a fact, a replace
  of that fact, one act sent twice under one act-id, and an act under a
  permission that does not stand. The checks: the answers; the two sends
  under one act-id have one digest and read one answer; and the time in the
  stamp of each is the time that the door wrote on the thing, for the act
  sent twice the time of the first send."
  [hand]
  (let [d (:door hand)
        own [:dana :dana :dana]
        of-dana (fn [facts] {:version 1 :who :dana :layer :dana :permission own :session :smoke :facts facts})
        decide (fn [act]
                 (let [sent (door/send-act d act)]
                   {:sent sent :answer (door/answer-of d (:act-id sent) (:digest sent) wait-ms)}))
        making (decide {:version 1 :who :system :layer :dana :permission own :session :smoke
                        :facts [{:e :dana :k :softland.store/depot :v :streaming}
                                {:e :dana :k :softland.store/permission :v own}]})
        fact (decide (of-dana [{:e :note-1 :k :text :v "one"}]))
        replaced (decide (of-dana [{:e :note-1 :k :text :v "one, replaced"
                                    :replaces [(:act-id (:sent fact)) 0]}]))
        twice-act (of-dana [{:e :note-2 :k :text :v "sent twice"}])
        twice-id (door/act-id-for d :dana)
        first-send (door/send-act d twice-act twice-id)
        _ (Thread/sleep 25)
        second-send (door/send-act d twice-act twice-id)
        first-answer (door/answer-of d twice-id (:digest first-send) wait-ms)
        second-answer (door/answer-of d twice-id (:digest second-send) wait-ms)
        refused (decide {:version 1 :who :mallory :layer :dana :permission [:mallory :dana :dana] :session :smoke
                         :facts [{:e :note-3 :k :text :v "x"}]})
        times-of-twice (mapv (fn [[_ r]] (get r declaration/time-part))
                             (filter (fn [[_ r]] (and (map? r) (= twice-id (:act-id r)))) (log-of (:streaming-depot hand))))
        each [[(:act-id (:sent making)) (:answer making)]
              [(:act-id (:sent fact)) (:answer fact)]
              [(:act-id (:sent replaced)) (:answer replaced)]
              [twice-id first-answer]
              [(:act-id (:sent refused)) (:answer refused)]]
        door-times (mapv (fn [[act-id _]] (door-time-of (:streaming-depot hand) act-id)) each)
        stamps (mapv (comp :stamp second) each)
        note-1 (rama/foreign-select-one (path/keypath (record/place-key :dana :note-1 :text))
                                        (get-in hand [:tables :streaming :facts]) {:pkey :dana})]
    (line! "f" "a streaming layer through the door: a making act, a fact, a replace, one act sent twice under one act-id, an act under a permission that does not stand:"
           (and (= [:accepted :accepted :accepted :accepted :refused] (mapv (comp :answer second) each))
                (= :permission-does-not-stand (:reason (:answer refused)))
                (= [:value "one, replaced"] (record/read-value note-1)))
           :answers (mapv (comp :answer second) each)
           :reason-of-the-refusal (:reason (:answer refused))
           :the-replaced-place-reads (record/read-value note-1))
    (line! "f" "the second send under one act-id: one digest, one answer, and two times on the depot:"
           (and (= (:digest first-send) (:digest second-send))
                (= first-answer second-answer)
                (= :accepted (:answer first-answer))
                (= 2 (count times-of-twice)))
           :same-digest (= (:digest first-send) (:digest second-send))
           :one-answer (= first-answer second-answer)
           :times-on-the-depot times-of-twice)
    (line! "f" "the time in the stamp of each is the door-time on its thing, for the act sent twice that of the first send:"
           (and (every? some? stamps) (every? some? door-times)
                (= door-times (mapv stamp/time-of stamps)))
           :door-times door-times
           :stamps-as-time-and-counter (mapv stamp-parts stamps))))

(defn- point-g!
  "g. The module's depots, by name. Asks the cluster for each of five depot
  names and for one PState name of the first module. The check: the two
  depots of acts are found, each with 4 partitions; `*batch-clock-depot`,
  `*microbatch-answer-depot` and `*streaming-answer-depot` are not; and
  `$$batch-clock` is not."
  [hand]
  (let [{:keys [ipc module]} hand
        depot (fn [depot-name]
                (try [:found (:num-partitions (rama/foreign-object-info (rama/foreign-depot ipc module depot-name)))]
                     (catch Throwable t [:not-found (.getName (class t))])))
        pstate (fn [pstate-name]
                 (try (let [p (rama/foreign-pstate ipc module pstate-name)]
                        [:found (pr-str (rama/foreign-select-one (path/keypath :microbatch) p {:pkey :base}))])
                      (catch Throwable t [:not-found (.getName (class t))])))
        found {"*streaming-depot" (depot "*streaming-depot")
               "*microbatch-depot" (depot "*microbatch-depot")
               "*batch-clock-depot" (depot "*batch-clock-depot")
               "*microbatch-answer-depot" (depot "*microbatch-answer-depot")
               "*streaming-answer-depot" (depot "*streaming-answer-depot")
               "$$batch-clock" (pstate "$$batch-clock")}]
    (line! "g" "the module has its two depots of acts and no other of the names asked:"
           (and (= [:found tasks] (found "*streaming-depot"))
                (= [:found tasks] (found "*microbatch-depot"))
                (every? #(= :not-found (first (found %)))
                        ["*batch-clock-depot" "*microbatch-answer-depot" "*streaming-answer-depot" "$$batch-clock"]))
           found)))

(defn- point-h!
  "h. The door's two reads. Calls the query `depot-of-layer` for the base,
  for the two streaming layers of the run and for a layer that is not made.
  Then sends one more act of the system into the base through the door,
  waits until the microbatch gate has processed every record of its depot,
  and calls `answer-of` once with no bound, which is one read. The checks:
  the depots; and that one read gives the act's answer, the same as the
  row on the base's task."
  [hand]
  (let [{:keys [ipc module]} hand
        d (:door hand)
        depot-of (let [q (rama/foreign-query ipc module "depot-of-layer")]
                   (fn [layer] (rama/foreign-invoke-query q layer)))
        depots (into {} (map (fn [layer] [layer (depot-of layer)])) [:base :dana :erin :nobodys-layer])
        sent (door/send-act d {:version 1 :who :system :layer :base :permission system-own :session :smoke
                               :facts [{:e :h-doc :k :text :v "read with one call"}]})
        _ (settle! hand)
        one-read (door/answer-of d (:act-id sent) (:digest sent))
        row (answer-row hand :microbatch (:act-id sent))]
    (line! "h" "depot-of-layer: the base, two streaming layers, and a layer that is not made:"
           (= {:base :microbatch :dana :streaming :erin :streaming :nobodys-layer nil} depots)
           depots)
    (line! "h" "answer-of for an act-id of the base, called once with no bound when the gate had processed its batch: one read gives the answer, with the stamp of the row on the base's task:"
           (and (= :accepted (:answer one-read))
                (some? row)
                (= (:stamp row) (:stamp one-read)))
           :ack (:ack sent) :the-one-read one-read :stamp-of-the-row (:stamp row))))

(defn- until-true
  "Calls a function of no arguments every 100 ms until it gives something
  true or `wait-ms` has passed. Gives what it gave last."
  [f]
  (let [end (+ (System/currentTimeMillis) wait-ms)]
    (loop []
      (let [got (try (f) (catch Throwable _ nil))]
        (if (or got (< end (System/currentTimeMillis)))
          got
          (do (Thread/sleep 100) (recur)))))))

(defn- streaming-decided
  "Takes a module's handles. Gives how far its streaming gate has come: how
  many answers its table holds over the tasks, and how many digests of
  strays."
  [handles]
  (let [answers (get-in handles [:streaming :answers])
        task (get-in handles [:streaming :task])]
    {:answers (reduce + (map (fn [k] (count (rama/foreign-select [path/MAP-KEYS] answers {:pkey k}))) index-keys))
     :strays (reduce + (for [k index-keys
                             w (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] task {:pkey k})]
                         (count (rama/foreign-select [(path/keypath :strays w) path/ALL] task {:pkey k}))))}))

(defn- rows-apart
  "Takes what each of two modules holds, gate -> table -> task -> key ->
  row. Gives each key whose row is not the same in both, as {:gate :table
  :task :key :first :again}, in the order of the gates, the tables, the
  tasks and the keys. A key that one side lacks has nil there. A row is
  compared on the task where it is."
  [of-first of-again]
  (vec (for [gate (sort (keys of-first))
             table (sort (keys (get of-first gate)))
             task (sort (distinct (concat (keys (get-in of-first [gate table])) (keys (get-in of-again [gate table])))))
             :let [first-rows (get-in of-first [gate table task])
                   again-rows (get-in of-again [gate table task])]
             k (sort-by pr-str (distinct (concat (keys first-rows) (keys again-rows))))
             :when (not= (get first-rows k) (get again-rows k))]
         {:gate gate :table table :task task :key k :first (get first-rows k) :again (get again-rows k)})))

(defn- point-i!
  "i. The run again. Takes the hand. Reads the first module's eight tables,
  launches `StoreAgain` in the same cluster, and waits until it has decided
  everything: Rama's test tool for its microbatch gate, with the number of
  records on the microbatch depot; for its streaming gate, until its
  answers and strays are as many as the first module's. Then it waits
  until nothing of the second module changed for 2 s, and compares every
  row of every table on every task. The checks: the second module decided
  everything; the first module did not change meanwhile; and no row
  differs. For each table it tells the rows of each module and how many
  differ, and the first ten that differ, with both rows."
  [hand]
  (let [ipc (:ipc hand)
        again-name (rama/get-module-name StoreAgain)
        _ (settle! hand)
        first-before (tables-held (:tables hand))
        in-microbatch (records-in (:microbatch-depot hand))
        in-streaming (records-in (:streaming-depot hand))
        began (System/currentTimeMillis)
        _ (rtest/launch-module! ipc StoreAgain {:tasks tasks :threads 2 :workers 2})
        again (open-tables ipc again-name)
        base-decided? (try (rtest/wait-for-microbatch-processed-count ipc again-name gate-name in-microbatch wait-ms)
                           true
                           (catch Throwable t
                             (println "SMOKE i NOT REACHED the second module's microbatch gate:" (subs (str t) 0 (min 300 (count (str t)))))
                             false))
        of-first (streaming-decided (:tables hand))
        streaming-decided? (boolean (until-true #(= of-first (streaming-decided again))))
        quiet? (boolean (until-true #(let [look (tables-held again)]
                                       (Thread/sleep 2000)
                                       (= look (tables-held again)))))
        took (- (System/currentTimeMillis) began)
        first-held (tables-held (:tables hand))
        again-held (tables-held again)
        apart (rows-apart first-held again-held)
        rows-in (fn [held gate table] (reduce + (map count (vals (get-in held [gate table])))))]
    (line! "i" "the run again: a second module that mirrors the two depots and calls add-gates-and-reads decided every record and came to rest:"
           (and base-decided? streaming-decided? quiet?)
           :records-on-the-microbatch-depot in-microbatch :records-on-the-streaming-depot in-streaming
           :microbatch-gate-processed-them base-decided?
           :streaming-answers-and-strays-of-the-first of-first :of-the-run-again (streaming-decided again)
           :from-its-launch-ms took)
    (line! "i" "the first module's tables did not change while the second ran:" (= first-before first-held))
    (doseq [gate [:microbatch :streaming]
            table [:answers :facts :facts-by-value :task]]
      (tell! "i" (str (get-in table-names [gate table]) ":")
             :rows-of-the-first (rows-in first-held gate table)
             :rows-of-the-run-again (rows-in again-held gate table)
             :rows-that-differ (count (filter #(and (= gate (:gate %)) (= table (:table %))) apart))))
    (line! "i" "every row of all eight tables on every task is the same in both modules:"
           (and (empty? apart)
                (pos? (rows-in again-held :microbatch :facts))
                (pos? (rows-in again-held :streaming :facts)))
           :rows-that-differ (count apart))
    (doseq [d (take 10 apart)]
      (println (str "SMOKE i differs " (pr-str d))))))

(defn- the-run!
  "The run, a to i, on a cluster in which the first module is launched.
  Takes the cluster."
  [ipc]
  (let [hand (open-hand ipc (rama/get-module-name module/Store))
        before (point-e-before! hand)
        _ (point-a! hand)
        chain-acts (point-b! hand)]
    (point-c! hand chain-acts)
    (point-d! hand)
    (point-e! hand before)
    (point-f! hand)
    (point-g! hand)
    (point-h! hand)
    (point-i! hand)))

(let [status (try
               (with-open [ipc (rtest/create-ipc)]
                 (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
                 (the-run! ipc))
               (println "SMOKE done")
               (if (empty? @failed) 0 1)
               (catch Throwable t
                 (println "SMOKE FAILED the run did not reach its end:" (str t))
                 (.printStackTrace t)
                 1))]
  (flush)
  (shutdown-agents)
  (System/exit status))
