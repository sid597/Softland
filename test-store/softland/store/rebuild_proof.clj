(ns softland.store.rebuild-proof
  "The proof of the rebuild on Rama's in-process cluster: what
  `softland.store.rebuild/from-logs` makes from the logs of a running
  module is held, row by row, against what the gates wrote into their
  tables. One command, from the repository root:

    clojure -M:store-module -m softland.store.rebuild-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status
  that is not zero when a check fails. Each row that differs is printed on
  a line that starts with DIFFERENT, with its gate, table, task and key and
  both rows; each thing that the rebuild named on a line that starts with
  NAMED.

  What it does, on each of two clusters. It sends things to both depots of
  acts, through the door (`softland.store.door`) and through no door, by
  the hand: an append of a thing that the proof built, act-id and all. It
  waits until the store is at rest. It reads the logs with
  `softland.store.rebuild.reader`, asks `softland.store.rebuild.placing` for
  the task of each route that the rebuild needs, and runs the rebuild. It
  selects every row of every table of each gate on every task, and the row
  of the kept clock, and compares.

  How it reaches one task's part of a table. As the module's proof does: a
  select with `{:pkey k}`, where `k` is the key that Rama's test tool gives
  for that task (`com.rpl.rama.test/gen-hashing-index-keys`). The select
  goes round the door's reads, so it sees every row that is there.

  How it knows that the store is at rest. Three things, in this order.
  Every appended thing is decided: an append to the streaming depot is made
  with the ack level :ack, which waits for the gate's event, the append of
  its entry in it; for the microbatch depot Rama's test tool waits until
  the gate has processed as many things as the proof appended, and on the
  second cluster, where a worker is started again, the proof reads the
  answer of each act that it sent until each is there. Every batch is
  closed and the run after the last batch is done: on every task the
  microbatch gate's row holds no `:to-close` and no key under `:to-keep`,
  and its `:closed-through` is the gate's last stamp. And nothing is still
  being appended: the end of every partition of every depot is the same
  before and after a quiet time.

  The two clusters. Each has four tasks and two workers, and holds the
  module `Store` and the module `Placing`. The first runs the store's
  functions as the store defines them. For the second, `-main` puts two
  functions of this namespace into the vars of two of the store's pure
  functions before the launch, and puts the store's own back when the
  cluster is closed, in a `finally`: each gives what the store's gives, and
  throws once when the proof asked. One makes an event of the streaming
  gate fail, the other an attempt of a batch. Rama takes the function that
  a var holds at the launch. Rama then prints lines of its own log, marked
  ERROR, that are no failed checks.

  What is left out of the comparison, by name: `:turns`, `:to-keep` and
  `:to-close` of what the microbatch gate keeps for a task
  (`softland.store.rebuild/left-out`). The proof reads the last two to know
  that the store is at rest, and never `:turns`.

  State. The proof owns six atoms: the lines that it showed; how many
  things it appended to the microbatch depot on the cluster that runs; the
  acts whose answers the second cluster waits for; the two wishes for a
  throw; and the throws that were made. `-main` empties all but the shown
  lines between the clusters. A cluster's files are temporary and are
  Rama's.

  What a pass does not show. A real cluster, a process that is killed, a
  leader that moves. A failure of the streaming gate behind its commit and
  before its append: in this build no function of the store runs there, so
  the throw that the proof makes is before the commit. A store that is not
  at rest. A number of tasks other than the one that decided the acts. A
  store larger than memory. That `Placing` gives Rama's task for a route in
  a real cluster: here it is held against the test tool's keys and against
  every row."
  (:require [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.gate.batch :as batch]
            [softland.store.module :as module]
            [softland.store.rebuild :as rebuild]
            [softland.store.rebuild.placing :as placing]
            [softland.store.rebuild.reader :as reader]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

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

(defn- cut
  "Takes anything. Gives its printed text, cut to 400 characters, for a
  line that a person reads."
  [x]
  (let [text (pr-str x)]
    (if (< 400 (count text)) (str (subs text 0 400) " ...") text)))

;; ------------------------------------------------------------------ the hand

(def ^:private tasks "The number of tasks of the proof's clusters." 4)

(def ^:private index-keys
  "For each task, a key that Rama's hash partitioner sends to it: the i-th
  to task i. A select with one of them as `:pkey` reads that task's part of
  a table."
  (vec (rtest/gen-hashing-index-keys tasks)))

(def ^:private gates
  "What the proof knows of each gate of the module: its depot of acts and
  the ack level of an append by the hand; the names of its four tables; the
  numbers that its row for a task holds; and, for a gate that decides in
  batches, the name of its topology."
  {:streaming {:depot "*streaming-depot" :ack :ack
               :tables {:answers "$$streaming-answers" :facts "$$streaming-facts"
                        :facts-by-value "$$streaming-facts-by-value" :task "$$streaming-task"}
               :numbers [:last-stamp]}
   :microbatch {:depot "*microbatch-depot" :ack :append-ack
                :tables {:answers "$$microbatch-answers" :facts "$$microbatch-facts"
                         :facts-by-value "$$microbatch-facts-by-value" :task "$$microbatch-task"}
                :numbers [:last-stamp :closed-through]
                :topology "microbatch-gate"}})

(def ^:dynamic ^:private *hand*
  "What the proof's hand holds of a cluster: the cluster, the module's name,
  the door, the query of an answer, for each gate its depot and its tables,
  the table of the kept clock, and `:checks`, which says which of the
  proof's two clusters it is. `-main` binds it for the run on each cluster."
  nil)

(defonce ^{:private true
           :doc "How many things the proof appended to the microbatch depot on the
  cluster that runs: what the wait for the gate waits for."}
  appended-to-base (atom 0))

(defonce ^{:private true
           :doc "The act-ids whose answers the proof reads for before it takes the
  second cluster to be at rest."}
  to-be-answered (atom []))

(defonce ^{:private true
           :doc "The wish for a throw in the streaming gate: the UUID of the act-id
  whose event is to fail once, or nil."}
  uuid-to-fail (atom nil))

(defonce ^{:private true
           :doc "The wish for a throw in a batch: true while the next call of
  `failing-rows-to-send` that gives rows is to throw."}
  batch-to-fail (atom false))

(defonce ^{:private true
           :doc "Each throw that the proof's two functions made: {:gate :wall}."}
  throws-made (atom []))

(defn- open-hand
  "Takes the cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc ipc
     :module m
     :door (door/open ipc m)
     :answer (rama/foreign-query ipc m "answer-by-act-id")
     :depots (into {} (map (fn [[gate {:keys [depot]}]] [gate (rama/foreign-depot ipc m depot)])) gates)
     :tables (into {}
                   (map (fn [[gate {:keys [tables]}]]
                          [gate (into {} (map (fn [[table table-name]] [table (rama/foreign-pstate ipc m table-name)])) tables)]))
                   gates)
     :clock (rama/foreign-pstate ipc m "$$batch-clock")}))

(defn- act
  "A maker's act, as she hands it to the door: no act-id and no depot."
  [layer who permission facts]
  {:version 1 :who who :layer layer :permission permission :session :proof-session :facts (vec facts)})

(defn- arrived
  "A thing as it arrives with no door: a maker's act with an act-id that
  the proof makes, under the depot word given, and that word as `:depot`."
  [depot layer who permission facts]
  (assoc (act layer who permission facts) :act-id [layer depot (UUID/randomUUID)] :depot depot))

(defn- by-hand!
  "Appends a thing to a gate's depot of acts with no door, with the ack
  level of `gates`. Gives :appended, or what the append threw."
  [gate thing]
  (when (= :append-ack (get-in gates [gate :ack]))
    (swap! appended-to-base inc))
  (try (rama/foreign-append! (get-in *hand* [:depots gate]) thing (get-in gates [gate :ack]))
       :appended
       (catch Throwable t [:append-failed (.getSimpleName (class t))])))

(defn- by-door!
  "Sends a maker's act through the door. Gives what the door gave:
  {:act-id :digest :ack}, or one of the door's words. An act that the door
  appended to the microbatch depot is counted."
  [act]
  (let [got (door/send-act (:door *hand*) act)]
    (when (= :microbatch (get-in got [:act-id 1]))
      (swap! appended-to-base inc))
    got))

(defn- settle!
  "Waits until the microbatch gate has processed each thing that the proof
  appended to its depot. Rama's test tool."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module *hand*)
                                             (get-in gates [:microbatch :topology]) @appended-to-base))

(defn- alone!
  "Appends one thing to the microbatch depot by the hand and waits until the
  gate has processed it, so that the next thing is of a later batch."
  [thing]
  (let [ack (by-hand! :microbatch thing)]
    (settle!)
    ack))

(defn- in-one-batch!
  "Appends things to the microbatch depot by the hand so that one batch of
  the gate takes them all: it pauses the topology, appends, and lets it go
  on. Waits until the gate has processed them."
  [things]
  (let [{:keys [ipc module]} *hand*
        topology (get-in gates [:microbatch :topology])]
    (rtest/pause-microbatch-topology! ipc module topology)
    (let [acks (mapv (fn [thing] (by-hand! :microbatch thing)) things)]
      (rtest/resume-microbatch-topology! ipc module topology)
      (settle!)
      acks)))

(defn- answer-of
  "Reads the answer under an act-id through the module's query, as the door
  does. Gives the answer, or nil for nothing yet and for a read that threw:
  for a short time after a worker is started again a read can throw."
  [act-id]
  (try
    (let [routing-id (record/routing-id act-id)]
      (when (record/answer-read-by-routing-id routing-id act-id)
        (rama/foreign-invoke-query (:answer *hand*) routing-id act-id)))
    (catch Throwable _ nil)))

;; ---------------------------------------------------------------- the people

(def ^:private alice-own "Alice's permission in her own layer, its first." [:alice :alice :alice])
(def ^:private bob-sees "Bob's permission in Alice's layer: he may see and not act." [:bob :alice :alice alice-own :see])
(def ^:private system-own "The system's permission in the base, the base's first." [:system :base :base])
(def ^:private alice-in-base "Alice's permission in the base, beneath the system's." [:alice :base :base system-own])
(def ^:private bob-sees-base "Bob's permission in the base: he may see and not act." [:bob :base :base system-own :see])

(def ^:private depot-key "The store key of a layer's depot." :softland.store/depot)
(def ^:private permission-key "The store key of a permission." :softland.store/permission)
(def ^:private marks-key "The store key of the keys that a layer marks." :softland.store/marked-keys)

(defn- of-kinds
  "Facts on one entity that hold a value of several kinds: an id that is a
  keyword, one that is a UUID, a string with spaces, a number, a map, and a
  map that is not in its normal form."
  [entity]
  [{:e entity :k :kw :v :canvas-1}
   {:e entity :k :uuid :v #uuid "0b0f1c6e-5a52-4c8e-9a43-6f1d2e7c8a90"}
   {:e entity :k :string :v "a string with spaces"}
   {:e entity :k :number :v 42}
   {:e entity :k :map :v {:a [1 #{2}] "b c" 1.5}}
   {:e entity :k :as-given :v {:b 1.50M :a [1 2N]}}])

;; ------------------------------------------------------------- what is sent

(defn- sends-to-the-streaming-gate!
  "Sends the things of the first cluster that go to the streaming depot,
  into Alice's layer, some through the door and some by the hand. Gives
  what the door gave for each act that went through it."
  []
  (let [door! (fn [who permission & facts] (by-door! (act :alice who permission facts)))
        hand! (fn [who permission & facts] (by-hand! :streaming (arrived :streaming :alice who permission facts)))
        marks (fn [ks] {:e :alice :k marks-key :v ks})
        twice (arrived :streaming :alice :alice alice-own [{:e :note-6 :k :text :v "sent twice"}])
        of-the-other-gate (arrived :microbatch :alice :alice alice-own [{:e :note-7 :k :text :v "a stray"}])]
    ;; an act into a layer of the other gate, before that layer is made
    (by-hand! :streaming (arrived :streaming :base :system system-own [{:e :thing-8 :k :name :v "before the base is made"}]))
    (let [through-the-door
          [;; the first act of the layer
           (door! :alice alice-own {:e :alice :k depot-key :v :streaming} {:e :alice :k permission-key :v alice-own})
           ;; a value of several kinds
           (apply door! :alice alice-own (of-kinds :kinds))
           ;; a mark, a fact under it, replaced
           (door! :alice alice-own (marks #{:text}))
           (door! :alice alice-own {:e :note-1 :k :text :v "first"})
           (door! :alice alice-own {:e :note-1 :k :text :v "second"})
           ;; a retract
           (door! :alice alice-own {:e :note-2 :k :text :v "to go"})
           (door! :alice alice-own {:e :note-2 :k :text})
           ;; the mark taken off and put on again, with a fact under each marking
           (door! :alice alice-own (marks #{}))
           (door! :alice alice-own {:e :note-3 :k :text :v "while the mark is off"})
           (door! :alice alice-own (marks #{:text}))
           (door! :alice alice-own {:e :note-1 :k :text :v "third"})
           ;; a maker who may not act
           (door! :alice alice-own {:e :bob :k permission-key :v bob-sees})
           (door! :bob bob-sees {:e :note-5 :k :text :v "bob may see"})]]
      ;; a refused act: a permission that nobody gave
      (hand! :zed [:zed :alice :alice alice-own] {:e :note-4 :k :text :v "no permission"})
      ;; an act sent twice, and a second and different act under its act-id
      (by-hand! :streaming twice)
      (by-hand! :streaming twice)
      (by-hand! :streaming (assoc twice :facts [{:e :note-8 :k :text :v "another thing under a used act-id"}]))
      ;; strays: no act-id; and an act-id of the other gate, twice
      (by-hand! :streaming 5)
      (by-hand! :streaming of-the-other-gate)
      (by-hand! :streaming of-the-other-gate)
      ;; a thing that the declaration refuses
      (by-hand! :streaming (dissoc (arrived :streaming :alice :alice alice-own [{:e :note-9 :k :text :v "no session"}]) :session))
      through-the-door)))

(defn- sends-to-the-microbatch-gate!
  "Sends the things of the first cluster that go to the microbatch depot,
  into the base, some through the door and some by the hand, and waits for
  the gate after each batch. Gives what the door gave for each act that
  went through it."
  []
  (let [system (fn [& facts] (arrived :microbatch :base :system system-own facts))
        alice (fn [& facts] (arrived :microbatch :base :alice alice-in-base facts))
        door! (fn [who permission & facts]
                (let [got (by-door! (act :base who permission facts))]
                  (settle!)
                  got))
        marks (fn [ks] {:e :base :k marks-key :v ks})
        twice (alice {:e :thing-6 :k :name :v "sent twice"})
        twice-in-a-batch (alice {:e :thing-7 :k :name :v "twice in one batch"})
        of-the-other-gate (arrived :streaming :alice :alice alice-own [{:e :note-7 :k :text :v "a stray of the base"}])]
    ;; the first act of the layer
    (alone! (system {:e :base :k depot-key :v :microbatch} {:e :system :k permission-key :v system-own}))
    (let [through-the-door
          [;; a fact under a key that is on every task
           (door! :system system-own {:e :alice :k permission-key :v alice-in-base})]
          ;; a mark and a fact under its key in one batch
          _ (in-one-batch! [(system (marks #{:kind})) (alice {:e :thing-1 :k :kind :v "x"})])
          through-the-door
          (into through-the-door
                [;; a fact under the marked key, replaced
                 (door! :alice alice-in-base {:e :thing-1 :k :kind :v "y"})
                 (door! :alice alice-in-base {:e :thing-1 :k :kind :v "z"})
                 ;; a value of several kinds
                 (apply door! :alice alice-in-base (of-kinds :kinds))])]
      ;; two acts of one batch on one place, under a plain key and under the marked key
      (in-one-batch! [(alice {:e :thing-2 :k :name :v "first"}) (alice {:e :thing-2 :k :name :v "second"})
                      (alice {:e :thing-3 :k :kind :v "p"}) (alice {:e :thing-3 :k :kind :v "q"})])
      ;; a retract, under a plain key and under the marked key
      (alone! (alice {:e :thing-2 :k :name}))
      (alone! (alice {:e :thing-3 :k :kind}))
      ;; the mark taken off, with a fact under the key in the same batch; a
      ;; fact while it is off; the mark put on again, and a fact under it
      (in-one-batch! [(system (marks #{})) (alice {:e :thing-5 :k :kind :v "in the batch that takes the mark off"})])
      (alone! (alice {:e :thing-4 :k :kind :v "while the mark is off"}))
      (alone! (system (marks #{:kind})))
      (alone! (alice {:e :thing-1 :k :kind :v "under the second marking"}))
      ;; a refused act, and a maker who may not act
      (alone! (arrived :microbatch :base :zed [:zed :base :base system-own] [{:e :thing-9 :k :name :v "no permission"}]))
      (alone! (system {:e :bob :k permission-key :v bob-sees-base}))
      (let [through-the-door (conj through-the-door (door! :bob bob-sees-base {:e :thing-10 :k :name :v "bob may see"}))]
        ;; an act sent twice, in two batches and in one; a second and different act under a used act-id
        (alone! twice)
        (alone! twice)
        (in-one-batch! [twice-in-a-batch twice-in-a-batch
                        (assoc twice-in-a-batch :facts [{:e :thing-7 :k :name :v "another thing under a used act-id"}])])
        ;; strays: no act-id; and an act-id of the other gate, twice
        (alone! 7)
        (alone! of-the-other-gate)
        (alone! of-the-other-gate)
        ;; an act into a layer of the other gate, and a thing that the declaration refuses
        (alone! (arrived :microbatch :alice :alice alice-own [{:e :note-10 :k :text :v "into the other gate's layer"}]))
        (alone! (dissoc (alice {:e :thing-11 :k :name :v "no session"}) :session))
        through-the-door))))

;; ------------------------------------------------------------------ at rest

(def ^:private rest-limit-ms
  "How long the proof waits for the store to come to rest, in milliseconds."
  120000)

(def ^:private quiet-ms
  "The quiet time across which no depot may grow, in milliseconds."
  1500)

(defn- batches-closed?
  "True when, on every task, the microbatch gate's row holds no `:to-close`
  and no key under `:to-keep`, and its `:closed-through` is the gate's last
  stamp, which one task holds; or no task holds either number. A read that
  throws gives false."
  []
  (try
    (let [table (get-in *hand* [:tables :microbatch :task])
          part (fn [k]
                 {:last-stamp (rama/foreign-select-one (path/keypath :last-stamp) table {:pkey k})
                  :closed-through (rama/foreign-select-one (path/keypath :closed-through) table {:pkey k})
                  :to-close (rama/foreign-select-one (path/keypath :to-close) table {:pkey k})
                  :to-keep (rama/foreign-select [(path/keypath :to-keep) path/MAP-KEYS] table {:pkey k})})
          parts (mapv part index-keys)
          last-stamps (keep :last-stamp parts)]
      (and (every? (comp nil? :to-close) parts)
           (every? (comp empty? :to-keep) parts)
           (<= (count last-stamps) 1)
           (every? (fn [p] (= (first last-stamps) (:closed-through p))) parts)))
    (catch Throwable _ false)))

(defn- ends-of-the-depots
  "The end offset of every partition of every depot that the reader reads,
  in one order. nil when a call threw."
  []
  (try
    (let [{:keys [ipc module]} *hand*
          {:keys [acts answers batch-clocks]} reader/depots]
      (vec (for [{:keys [depot]} (concat acts answers [batch-clocks])
                 :let [handle (rama/foreign-depot ipc module depot)]
                 partition (range tasks)]
             (:end-offset (rama/foreign-depot-partition-info handle partition)))))
    (catch Throwable _ nil)))

(defn- until-true
  "Calls a function of no argument every 50 ms until it gives a true thing
  or `rest-limit-ms` has passed. Gives what it gave last."
  [f]
  (let [deadline (+ (System/currentTimeMillis) rest-limit-ms)]
    (loop []
      (let [got (f)]
        (cond got got
              (< deadline (System/currentTimeMillis)) got
              :else (do (Thread/sleep 50) (recur)))))))

(defn- at-rest!
  "Waits until the store is at rest, as the namespace docstring says. Gives
  {:answered? :closed? :quiet? :waited-ms}: whether each act that the proof
  reads for was answered, whether every batch was closed and the run after
  the last was done, whether a quiet time passed in which no depot grew,
  and how long it waited."
  []
  (let [began (System/currentTimeMillis)
        answered? (boolean (until-true (fn [] (every? answer-of @to-be-answered))))
        closed? (boolean (until-true batches-closed?))
        quiet? (boolean (until-true (fn []
                                      (let [before (ends-of-the-depots)]
                                        (Thread/sleep quiet-ms)
                                        (and before (batches-closed?) (= before (ends-of-the-depots)))))))]
    {:answered? answered? :closed? closed? :quiet? quiet?
     :waited-ms (- (System/currentTimeMillis) began)}))

;; ----------------------------------------------------------- the comparison

(defn- task-row-held
  "What a gate keeps for one task, as the tables hold it and as the rebuild
  gives it: the numbers that `gates` names for the gate, each when it is
  there, and `:strays`, a map from each word to its set of digests, when
  the task holds a stray. `:turns`, `:to-keep` and `:to-close` are not
  read: they are left out of the comparison."
  [gate table k]
  (let [numbers (into {}
                      (keep (fn [number]
                              (when-let [n (rama/foreign-select-one (path/keypath number) table {:pkey k})]
                                [number n])))
                      (get-in gates [gate :numbers]))
        words (rama/foreign-select [(path/keypath :strays) path/MAP-KEYS] table {:pkey k})
        strays (into {}
                     (keep (fn [word]
                             (let [digests (set (rama/foreign-select [(path/keypath :strays word) path/ALL] table {:pkey k}))]
                               (when (seq digests) [word digests]))))
                     words)]
    (cond-> numbers
      (seq strays) (assoc :strays strays))))

(defn- tables-held
  "Every row of every table of each gate on every task, selected straight
  from the PStates: gate -> table -> task -> rows, in the form of
  `softland.store.rebuild/from-logs`. One select of a whole part for each
  table and task; the tables of a proof are small."
  []
  (into {}
        (map (fn [[gate tables]]
               [gate (into {}
                           (map (fn [[table handle]]
                                  [table (into {}
                                               (map-indexed
                                                (fn [task k]
                                                  [task (if (= :task table)
                                                          (task-row-held gate handle k)
                                                          (into {} (rama/foreign-select [path/ALL] handle {:pkey k})))]))
                                               index-keys)]))
                           tables)]))
        (:tables *hand*)))

(defn- clock-held
  "The rows of the table of the kept clock on every task: task -> rows."
  []
  (into {}
        (map-indexed (fn [task k] [task (into {} (rama/foreign-select [path/ALL] (:clock *hand*) {:pkey k}))]))
        index-keys))

(defn- rows-apart
  "Takes two maps, table -> task -> key -> row, and a word for the gate.
  Gives each key whose row is not the same in both, as
  {:gate :table :task :key :made :held}, in the order of the tables, the
  tasks and the keys. A key that one side lacks has nil there."
  [gate made held]
  (vec (for [table (sort (distinct (concat (keys made) (keys held))))
             task (sort (distinct (concat (keys (get made table)) (keys (get held table)))))
             :let [made-rows (get-in made [table task])
                   held-rows (get-in held [table task])]
             k (sort-by str (distinct (concat (keys made-rows) (keys held-rows))))
             :when (not= (get made-rows k) (get held-rows k))]
         {:gate gate :table table :task task :key k :made (get made-rows k) :held (get held-rows k)})))

(defn- compared
  "Reads the logs, runs the rebuild, selects the tables, and compares. Gives
  {:logs :routes :made :held :apart :rows}: the logs as the reader gave
  them; how many routes the rebuild asked the task of; what the rebuild
  made and what the tables hold, each with a run of bytes as hex text; each
  row that differs; and how many rows the tables hold. Each difference is
  printed on a line that starts with DIFFERENT, and each thing that the
  rebuild named on a line that starts with NAMED."
  []
  (let [{:keys [ipc module]} *hand*
        logs (reader/logs ipc module)
        routes (rebuild/routes-asked logs)
        task-of-route (placing/tasks-of-routes ipc routes)
        made (rebuild/bytes-as-hex (rebuild/from-logs (assoc logs :task-of-route task-of-route)))
        held (rebuild/bytes-as-hex {:tables (tables-held) :batch-clock (clock-held)})
        apart (into (vec (mapcat (fn [gate] (rows-apart gate (get-in made [:tables gate]) (get-in held [:tables gate])))
                                 (sort (distinct (concat (keys (:tables made)) (keys (:tables held)))))))
                    (rows-apart :the-kept-clock {:batch-clock (:batch-clock made)} {:batch-clock (:batch-clock held)}))]
    (doseq [{:keys [gate table task key made held]} apart]
      (println "DIFFERENT" gate table "task" task "key" (pr-str key) "| made:" (cut made) "| held:" (cut held)))
    (doseq [named (:named made)]
      (println "NAMED" (cut named)))
    (flush)
    {:logs logs :routes (count routes) :made made :held held :apart apart
     :rows (into {}
                 (map (fn [[gate tables]]
                        [gate (into {} (map (fn [[table by-task]]
                                              [table (reduce + (map count (vals by-task)))]))
                                    tables)]))
                 (:tables held))}))

(defn- words-of-answers
  "Takes what the tables hold of one gate. Gives how many answers hold each
  word, with :accepted for an answer with none."
  [held-of-gate]
  (frequencies (map (fn [row] (get row :refused :accepted)) (mapcat vals (vals (:answers held-of-gate))))))

(defn- records-in
  "How many records a log holds, over its partitions."
  [log]
  (reduce + (map count log)))

(defn- entries-in
  "How many answers' entries a log of answers holds, over its records."
  [log]
  (reduce + (map (fn [record] (count (:answers record))) (apply concat log))))

;; ------------------------------------------------------------------ the checks

(deftest the-placing-is-ramas
  (let [asked (placing/tasks-of-routes (:ipc *hand*) index-keys)]
    (is (= (zipmap index-keys (range tasks)) asked))
    (shown "the query of the module Placing gives each of Rama's own keys for a task the task that Rama made it for:"
           (pr-str (mapv asked index-keys)))))

(deftest the-store-is-made-again-from-its-logs
  (let [by-door-streaming (sends-to-the-streaming-gate!)
        by-door-base (sends-to-the-microbatch-gate!)
        ;; an act into a layer of the other gate, now that the layer is made
        _ (by-hand! :streaming (arrived :streaming :base :system system-own [{:e :thing-8 :k :name :v "after the base is made"}]))
        not-appended (remove :act-id (concat by-door-streaming by-door-base))
        rested (at-rest!)
        {:keys [logs routes made held apart rows]} (compared)
        streaming (get-in held [:tables :streaming])
        base (get-in held [:tables :microbatch])
        on-tasks (fn [gate-tables table k] (count (filter (fn [by-key] (contains? by-key k)) (vals (get gate-tables table)))))]
    (is (empty? not-appended))
    (shown "sent through the door:" (count by-door-streaming) "acts to the streaming depot and" (count by-door-base)
           "to the microbatch depot, each appended; the rest by the hand, with no door | things in the logs of acts:"
           (pr-str (mapv (fn [{:keys [gate log]}] [gate (records-in log)]) (:acts logs))))
    (is (= {:answered? true :closed? true :quiet? true} (dissoc rested :waited-ms)))
    (shown "the store is at rest: the gate processed each of the" @appended-to-base
           "things of the microbatch depot; no task holds a :to-close or a key under :to-keep; :closed-through is the gate's last stamp on every task; no depot grew in"
           quiet-ms "ms | waited" (:waited-ms rested) "ms")
    (is (and (pos? (get (words-of-answers streaming) :accepted 0)) (< 1 (count (words-of-answers streaming)))))
    (is (and (pos? (get (words-of-answers base) :accepted 0)) (< 1 (count (words-of-answers base)))))
    (shown "what the gates decided, by word | streaming:" (pr-str (words-of-answers streaming))
           "| microbatch:" (pr-str (words-of-answers base)))
    (is (= 1 (on-tasks streaming :facts ":alice :kinds :map")))
    (is (= 1 (on-tasks base :facts ":base :kinds :map")))
    (is (= tasks (on-tasks base :facts ":base :alice :softland.store/permission")))
    (is (pos? (:facts-by-value (:streaming rows))))
    (is (pos? (:facts-by-value (:microbatch rows))))
    (let [by-value-of-base (sort (map (fn [k] (let [parts (vec (.split ^String k " "))] [(nth parts 2) (nth parts 4)]))
                                      (mapcat keys (vals (:facts-by-value base)))))]
      (is (some (fn [[_ entity]] (= ":thing-5" entity)) by-value-of-base))
      (is (= 2 (count (distinct (map first by-value-of-base)))))
      (shown "the facts by value of the base, each as the key's stamp and the entity: two markings; the fact of the mark's own batch (the first value of :thing-1) has no row, and the fact of the batch that takes the mark off (:thing-5) has one, under the earlier stamp:"
             (pr-str by-value-of-base)))
    (is (some (fn [row] (seq (:strays row))) (vals (:task streaming))))
    (is (some (fn [row] (seq (:strays row))) (vals (:task base))))
    (shown "the rows that the tables hold, over the" tasks "tasks:" (pr-str rows)
           "| a fact of the base under a key that every task holds is on" (on-tasks base :facts ":base :alice :softland.store/permission") "tasks"
           "| the kept clock:" (pr-str (:batch-clock held)))
    (shown "the logs of answers:" (pr-str (into {} (map (fn [[gate log]] [gate {:records (records-in log) :entries (entries-in log)}])) (:answers logs)))
           "| the log of clocks:" (records-in (:batch-clocks logs)) "records | routes that Rama placed for the rebuild:" routes)
    (is (empty? (:named made)))
    (shown "the rebuild named nothing: every entry has its act, no two entries under one act-id differ, and the check of today gives each accepted thing its form:"
           (pr-str (:named made)))
    (is (empty? apart))
    (shown "made again from the logs alone and held against the tables, row by row, each table of each gate on each task, and the row of the kept clock: rows that differ:"
           (count apart))
    (shown "left out of the comparison, by name, as the rebuild's ruling says:" (pr-str (mapv :part rebuild/left-out)))))

;; ----------------------------------------------- a failed attempt of each gate

(def ^:private stores-answer-record
  "`softland.store.record.rows/answer-record` as the store defines it, taken
  when this namespace was loaded, before any var is changed."
  record/answer-record)

(def ^:private stores-rows-to-send
  "`softland.store.gate.batch/rows-to-send` as the store defines it."
  batch/rows-to-send)

(defn failing-answer-record
  "Stands in for `softland.store.record.rows/answer-record` on the proof's
  second cluster. Takes what it takes and gives what it gives. Once, for
  the act-id whose UUID the proof marked, it throws in its place: the
  streaming gate calls it in the event of an act, behind the event's writes
  and before its commit, so that attempt of the event fails and Rama runs
  the record again. It is public and held by a var, and it closes over
  nothing: Rama takes the function that the store's var holds at the
  launch."
  [gate-depot layer act-uuid row]
  (let [marked @uuid-to-fail]
    (when (and marked (= marked act-uuid) (compare-and-set! uuid-to-fail marked nil))
      (swap! throws-made conj {:gate gate-depot :wall (System/currentTimeMillis)})
      (throw (ex-info "the rebuild's proof: a forced failure of this event of the streaming gate" {}))))
  (stores-answer-record gate-depot layer act-uuid row))

(defn failing-rows-to-send
  "Stands in for `softland.store.gate.batch/rows-to-send` on the proof's
  second cluster. Takes what it takes and gives what it gives. Once, when
  the proof asked and this call gave rows, it throws in its place: the
  attempt of that batch fails behind the decisions of its acts, and Rama
  runs the batch again. Only the meeting task of a layer gets rows from it,
  so the batch that is to fail holds acts that meet."
  [pass]
  (let [out (stores-rows-to-send pass)]
    (when (and (seq out) (compare-and-set! batch-to-fail true false))
      (swap! throws-made conj {:gate :microbatch :wall (System/currentTimeMillis)})
      (throw (ex-info "the rebuild's proof: a forced failure of this attempt of a batch" {})))
    out))

(defn- send-and-read-for!
  "Appends a thing by the hand on a thread of its own and does not wait for
  the ack longer than a minute: an append whose event fails can throw, or
  come back late. Keeps the thing's act-id among those that the proof reads
  for. Gives the ack, or :no-word-in-time."
  [gate thing]
  (swap! to-be-answered conj (:act-id thing))
  (deref (future (by-hand! gate thing)) 60000 :no-word-in-time))

(deftest after-a-failed-attempt-of-each-gate
  (let [alice! (fn [& facts] (send-and-read-for! :streaming (arrived :streaming :alice :alice alice-own facts)))
        system! (fn [& facts] (send-and-read-for! :microbatch (arrived :microbatch :base :system system-own facts)))
        base-topology (get-in gates [:microbatch :topology])
        {:keys [ipc module]} *hand*
        ;; the two layers, a marked key in each, and a fact under it
        _ (alice! {:e :alice :k depot-key :v :streaming} {:e :alice :k permission-key :v alice-own})
        _ (alice! {:e :alice :k marks-key :v #{:text}})
        _ (alice! {:e :note-1 :k :text :v "before the failure"})
        _ (system! {:e :base :k depot-key :v :microbatch} {:e :system :k permission-key :v system-own})
        _ (until-true (fn [] (every? answer-of @to-be-answered)))
        _ (system! {:e :base :k marks-key :v #{:kind}})
        _ (until-true (fn [] (every? answer-of @to-be-answered)))
        _ (system! {:e :thing-1 :k :kind :v "before the failure"})
        _ (until-true (fn [] (every? answer-of @to-be-answered)))
        ;; an event of the streaming gate fails once
        to-fail (arrived :streaming :alice :alice alice-own [{:e :note-1 :k :text :v "its first event fails"}])
        _ (reset! uuid-to-fail (nth (:act-id to-fail) 2))
        ack-of-failed (send-and-read-for! :streaming to-fail)
        streaming-answered? (boolean (until-true (fn [] (every? answer-of @to-be-answered))))
        throws-in-streaming (count @throws-made)
        _ (alice! {:e :note-2 :k :text :v "after the failure"})
        ;; an attempt of a batch fails once: two acts on one place under the marked key meet
        _ (rtest/pause-microbatch-topology! ipc module base-topology)
        _ (reset! batch-to-fail true)
        _ (system! {:e :thing-2 :k :kind :v "first of the batch that runs twice"})
        _ (system! {:e :thing-2 :k :kind :v "second of the batch that runs twice"})
        _ (rtest/resume-microbatch-topology! ipc module base-topology)
        base-answered? (boolean (until-true (fn [] (every? answer-of @to-be-answered))))
        _ (reset! batch-to-fail false)
        _ (system! {:e :thing-3 :k :kind :v "after the failure"})
        _ (alice! {:e :note-3 :k :text :v "after both failures"})
        rested (at-rest!)
        {:keys [logs made held apart rows]} (compared)
        entries (fn [gate] (entries-in (get-in logs [:answers gate])))
        answers (fn [gate] (reduce + (map count (vals (get-in held [:tables gate :answers])))))]
    (is (= [:streaming :microbatch] (mapv :gate @throws-made)))
    (shown "a failed attempt of each gate was made: an event of the streaming gate threw behind its writes and before its commit, and an attempt of a batch threw behind its decisions:"
           (pr-str (mapv :gate @throws-made)) "| the ack of the act whose event failed:" (pr-str ack-of-failed)
           "| throws when the streaming act was answered:" throws-in-streaming)
    (is (and streaming-answered? base-answered?))
    (is (= {:answered? true :closed? true :quiet? true} (dissoc rested :waited-ms)))
    (shown "each act that was sent has its answer, the acts of the failed attempts too; then the store came to rest: every batch closed, no depot growing | waited"
           (:waited-ms rested) "ms")
    (shown "entries in the logs of answers against answers in the tables: a second run leaves copies, and no entry of an attempt that did not count | streaming:"
           (entries :streaming) "entries for" (answers :streaming) "answers | microbatch:" (entries :microbatch) "entries for" (answers :microbatch) "answers")
    (is (empty? (:named made)))
    (is (empty? apart))
    (shown "after the failed attempts, made again from the logs and held against the tables, row by row:" (pr-str rows)
           "| rows that differ:" (count apart) "| named:" (pr-str (:named made)))))

;; ---------------------------------------------------------------- the run

(defn test-ns-hook
  "Runs the checks of the cluster that the hand is on, in the order in which
  the proof tells them."
  []
  (the-placing-is-ramas)
  (if (= :a-failed-attempt-of-each-gate (:checks *hand*))
    (after-a-failed-attempt-of-each-gate)
    (the-store-is-made-again-from-its-logs)))

(defn- run-on-a-cluster
  "Creates an in-process cluster, launches the modules `Store` and `Placing`
  in it, each with four tasks, two threads and two workers, runs the checks
  that `checks` names with the hand on that cluster, and closes the
  cluster. Gives what `run-tests` gives. The launch takes the functions
  that the vars of the store's code hold at that moment."
  [checks]
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
    (rtest/launch-module! ipc placing/Placing {:tasks tasks :threads 2 :workers 2})
    (binding [*hand* (assoc (open-hand ipc) :checks checks)]
      (run-tests 'softland.store.rebuild-proof))))

(defn- empty-the-hand!
  "Empties each atom of the proof but the shown lines: what the hand keeps
  of one cluster means nothing on the next."
  []
  (reset! appended-to-base 0)
  (reset! to-be-answered [])
  (reset! uuid-to-fail nil)
  (reset! batch-to-fail false)
  (reset! throws-made []))

(defn -main
  "Runs the proof on two clusters, one after the other, and exits: status 0
  when each check of both runs holds. With the one argument `first` it runs
  the first cluster alone, for a quicker look while the proof is worked on.

  The first cluster runs on the functions as the store defines them. When
  it is closed, `-main` empties the hand's atoms, puts the two failing
  functions into the vars of the store's two, starts the second cluster,
  and puts the store's functions back when that cluster is closed, in a
  `finally`. The count line is one: the counts of the runs, added."
  [& args]
  (let [counts #(select-keys % [:test :pass :fail :error])
        first-run (run-on-a-cluster :the-store-as-defined)
        _ (empty-the-hand!)
        second-run (when-not (= ["first"] (vec args))
                     (try
                       (alter-var-root #'record/answer-record (constantly failing-answer-record))
                       (alter-var-root #'batch/rows-to-send (constantly failing-rows-to-send))
                       (run-on-a-cluster :a-failed-attempt-of-each-gate)
                       (finally
                         (alter-var-root #'record/answer-record (constantly stores-answer-record))
                         (alter-var-root #'batch/rows-to-send (constantly stores-rows-to-send)))))
        result (merge-with + (counts first-run) (counts (or second-run {})))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println result)
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
