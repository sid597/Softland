(ns softland.store.module-proof
  "The proof of the store's module on Rama's in-process cluster: the two
  gates, their records, and the three reads. One command, from the
  repository root:

    clojure -M:store-module -m softland.store.module-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The door is not built. The proof plays it by hand, with a few functions of
  this namespace, in the order of one act: it reads the class of the layer;
  makes the act-id, with a random UUID; asks for locks by an act and reads
  them with one query; seals each value, the UTF-8 of the one text of the
  value, to its place under a lock that it has not used; appends the thing;
  reads the answer by act-id; and compares the answer's digest with the
  digest of what it sent. An error from the append is not an answer. It
  keeps each thing that it sent, so that a repeat is the same thing byte for
  byte.

  The two depots. For a streaming layer the hand appends with the ack level
  :ack and reads the answer once: the ack waits for the gate. For the base,
  the one microbatch layer, no ack carries an answer: the hand appends with
  :append-ack and reads the answer by act-id in a loop with a time limit, as
  a door would. To put several things into one batch it pauses the
  microbatch topology with Rama's test tool, appends, and lets it go on.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It owns eight atoms: the
  lines that it showed; the things that it sent to each depot; the first
  answers of each gate, in the order sent; the locks that it holds and has
  not used; the bytes of each lock that it read; and what later checks come
  back to. It reads the clock to make a carried stamp and to time a wait.

  Approach. The checks run in one order on one cluster, and later checks
  build on the layers, the facts and the permissions that earlier ones
  made. The checks of the streaming gate run first. The base is made after
  the check that sends its making act to the streaming gate, because that
  check holds the word of a layer that is not made. Each check says what
  must be read back, and reads it back through the module's own queries.
  Where it shows a row, it selects the row from a gate's PState: for a
  streaming layer with the layer as the partitioning key, for the base with
  the row's own route, and for \"on which tasks is this row\" once on each
  task. Such a select goes round the three reads, so it sees a row of a
  batch that is not closed; one check uses that to catch an answer between
  its batch and the closing. Near the end the proof starts the module again
  in the cluster, by Rama's module update with the unchanged module, and
  reads both records once more; Rama then prints some lines of its own log,
  marked ERROR, that are no failed checks.

  What it does not reach is in the folder's README."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act]
            [softland.store.declaration :as declaration]
            [softland.store.digest :as digest]
            [softland.store.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.record :as record]
            [softland.store.seal :as seal]
            [softland.store.stamp :as stamp]
            [softland.store.text :as text])
  (:import [java.util UUID]))

(defonce ^:private shown-lines (atom []))

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

;; ------------------------------------------------------------------ the hand

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(def ^:dynamic ^:private *hand*
  "What the proof's hand holds of the cluster: the cluster itself, the name
  of the module, the two depots, the three queries, and the four PStates of
  each gate. `-main` binds it for the run."
  nil)

(defonce ^:private things-sent (atom []))
(defonce ^:private first-answers (atom []))
(defonce ^:private locks-in-hand (atom {}))
(defonce ^:private kept (atom {}))
(defonce ^:private base-things-sent (atom []))
(defonce ^:private base-answers (atom []))
(defonce ^:private lock-bytes-read (atom #{}))

(defn- open-hand
  "Takes the cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc        ipc
     :depot      (rama/foreign-depot ipc m "*streaming-depot")
     :answer     (rama/foreign-query ipc m "answer-by-act-id")
     :locks      (rama/foreign-query ipc m "locks-of-request")
     :class      (rama/foreign-query ipc m "class-of-layer")
     :answers-ps (rama/foreign-pstate ipc m "$$streaming-answers")
     :facts-ps   (rama/foreign-pstate ipc m "$$streaming-facts")
     :locks-ps   (rama/foreign-pstate ipc m "$$streaming-locks")
     :task-ps    (rama/foreign-pstate ipc m "$$streaming-task")
     :module     m
     :mb-depot      (rama/foreign-depot ipc m "*microbatch-depot")
     :mb-answers-ps (rama/foreign-pstate ipc m "$$microbatch-answers")
     :mb-facts-ps   (rama/foreign-pstate ipc m "$$microbatch-facts")
     :mb-locks-ps   (rama/foreign-pstate ipc m "$$microbatch-locks")
     :mb-task-ps    (rama/foreign-pstate ipc m "$$microbatch-task")}))

(defn- class-of-layer
  "Reads the class of a layer, or nil for a layer that is not made. The hand
  tests what it passes before it calls, and gives nil itself for a thing
  that is no id."
  [layer]
  (when (record/class-read layer)
    (rama/foreign-invoke-query (:class *hand*) layer)))

(defn- answer-of
  "Reads the answer under an act-id, or nil for nothing yet. The hand passes
  the act-id's routing id as the first argument, the layer of a streaming
  act-id and the uuid of a microbatch one, and tests the act-id before it
  calls."
  [act-id]
  (let [routing-id (record/routing-id act-id)]
    (when (record/answer-read-by-routing-id routing-id act-id)
      (rama/foreign-invoke-query (:answer *hand*) routing-id act-id))))

(defn- locks-of
  "Reads the locks of a request, of the indexes from `from` to `to`: a map
  from index to {:bytes :way}. It keeps the hex of each lock's bytes that it
  read, for the check that no lock's bytes are on a depot."
  [request-act-id from to]
  (let [routing-id (record/routing-id request-act-id)
        locks (if (record/lock-range-read-by-routing-id routing-id request-act-id from to)
                (rama/foreign-invoke-query (:locks *hand*) routing-id request-act-id from to)
                {})]
    (swap! lock-bytes-read into (keep #(some-> (:bytes %) text/hex)) (vals locks))
    locks))

(defn- new-act-id
  "Makes an act-id for a layer: the layer, its class as the module gives it,
  or the class that the layer starts with when it is not made, and a random
  UUID."
  [layer]
  [layer (or (class-of-layer layer) (first-act/class-at-making layer)) (UUID/randomUUID)])

(defn- thing
  "An act as the door sends it: the maker's parts, and the door's act-id and
  class. `more` is merged in last, for the optional parts and for faults."
  ([layer who permission facts] (thing layer who permission facts {}))
  ([layer who permission facts more]
   (let [act-id (new-act-id layer)]
     (merge {:act-id act-id :class (nth act-id 1)
             :version 1 :who who :layer layer :permission permission :session :proof-session
             :facts (vec facts)}
            more))))

(defn- send!
  "Appends a thing to the streaming depot with the ack level :ack, and keeps
  it. Gives :acked, or what the append threw, which is not an answer."
  [x]
  (swap! things-sent conj x)
  (try (rama/foreign-append! (:depot *hand*) x :ack)
       :acked
       (catch Throwable t [:append-failed (.getSimpleName (class t))])))

(defn- decide!
  "Sends a thing and reads its answer by act-id. Gives the thing, the ack,
  the answer, whether the answer's digest is the digest of what was sent,
  and one word for the outcome: :accepted, the reason of a refusal, or
  :nothing-yet. The first answer of each act-id is kept, in the order sent,
  for the check of the stamps."
  [x]
  (let [ack (send! x)
        answer (answer-of (:act-id x))
        mine? (digest/is-digest-of? (:digest answer) x)]
    (when (and answer mine? (not-any? #(= (:act-id x) (:act-id %)) @first-answers))
      (swap! first-answers conj {:act-id (:act-id x) :layer (:layer x) :stamp (:stamp answer)}))
    {:thing x :ack ack :answer answer :mine? mine?
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

(defn- ask-for-locks!
  "Asks for n locks by an act of `who` under `permission`: one fact under
  :lock-request on the uuid of that act's own act-id. Reads the answer, then
  the locks with one query. Gives the result of the act with :locks, a map
  from index to lock, and :request, the request's act-id. The locks are put
  in the hand for the layer."
  [layer who permission n]
  (let [base (thing layer who permission [])
        request (:act-id base)
        result (decide! (assoc base :facts [{:e (nth request 2) :k :lock-request :v n}]))
        locks (locks-of request 0 255)]
    (swap! locks-in-hand update layer (fnil into [])
           (map (fn [[index lock]] {:lock-id [request index] :lock lock}) (sort-by key locks)))
    (assoc result :request request :locks locks)))

(defn- take-lock!
  "Takes one lock that the hand holds for a layer and has not used. Gives
  {:lock-id :lock}. Throws when the hand holds none: the proof then asked
  for too few."
  [layer]
  (let [taken (first (get @locks-in-hand layer))]
    (when-not taken (throw (ex-info "the hand holds no lock for the layer" {:layer layer})))
    (swap! locks-in-hand update layer (comp vec rest))
    taken))

(defn- plain-bytes
  "The plain bytes that the hand seals for a value: the UTF-8 of the one text
  of the value in its normal form."
  [v]
  (text/utf8 (act/normal-value v)))

(defn- seal-with
  "Seals a value to the place [layer e k] under a lock in hand,
  {:lock-id :lock}. Gives the fact as it arrives: {:e :k :sealed :lock-id}."
  [{:keys [lock-id lock]} layer e k v]
  {:e e :k k :sealed (seal/seal lock [layer e k] (plain-bytes v)) :lock-id lock-id})

(defn- sealed
  "A value fact as it arrives, sealed under a lock that the hand has not
  used."
  [layer e k v]
  (seal-with (take-lock! layer) layer e k v))

(defn- row
  "Selects one row of a PState of the gate, by its key, on the task of the
  layer."
  [pstate layer k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey layer}))

(defn- fact-row
  "The row of a place of a layer, or nil for an empty place."
  [layer e k]
  (row :facts-ps layer (record/place-key layer e k)))

(defn- lock-row
  "The row of a lock, by its id."
  [[[layer _ request-uuid] index]]
  (row :locks-ps layer (record/lock-key layer request-uuid index)))

(defn- last-stamp
  "The last stamp that the gate gave on the task of a layer."
  [layer]
  (rama/foreign-select-one (path/keypath :last-stamp) (:task-ps *hand*) {:pkey layer}))

(defn- strays
  "The number of different strays under a word, over every task: one select
  for each task, with a key that hashes to it."
  [word]
  (reduce + (map (fn [k] (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                                  (:task-ps *hand*) {:pkey k}))
                 (rtest/gen-hashing-index-keys tasks))))

(defn- stray-digests
  "The digests of the strays under a word, over every task."
  [word]
  (into #{} (mapcat (fn [k] (rama/foreign-select [(path/keypath :strays word) path/ALL]
                                                 (:task-ps *hand*) {:pkey k}))
                    (rtest/gen-hashing-index-keys tasks))))

(defn- stored
  "Each record of the streaming depot's log, read back: [partition record]."
  []
  (let [depot (:depot *hand*)]
    (for [p (range (:num-partitions (rama/foreign-object-info depot)))
          :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
          r (rama/foreign-depot-read depot p start-offset end-offset)]
      [p r])))

(defn- short-id
  "The first eight characters of the uuid of an act-id, for a line."
  [act-id]
  (subs (str (nth act-id 2)) 0 8))

(defn- told
  "A result as text for a line: the short act-id, the outcome, the stamp."
  [{:keys [thing answer word]}]
  (str (short-id (:act-id thing)) " " word " stamp " (:stamp answer)))

;; ------------------------------------------------------ the hand for the base

(def ^:private time-limit-ms
  "How long the hand reads for the answer of a microbatch act before it gives
  up, in milliseconds."
  30000)

(defn- append-to-base!
  "Appends a thing to the microbatch depot with the ack level :append-ack,
  and keeps it. Gives :appended, or what the append threw. The ack says that
  the depot holds the thing, and nothing about the decision."
  [x]
  (swap! base-things-sent conj x)
  (try (rama/foreign-append! (:mb-depot *hand*) x :append-ack)
       :appended
       (catch Throwable t [:append-failed (.getSimpleName (class t))])))

(defn- wait-until-processed!
  "Waits until the microbatch gate has processed each thing that the hand
  appended to its depot. Rama's test tool; a door has none, and reads."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *hand*) (:module *hand*) "microbatch-gate" (count @base-things-sent)))

(defn- await-answer
  "Reads the answer under a microbatch act-id in a loop, as a door would,
  until one is there or the time limit has passed. Gives the answer, or
  nil."
  [act-id]
  (let [deadline (+ (System/currentTimeMillis) time-limit-ms)]
    (loop []
      (let [answer (answer-of act-id)]
        (cond answer answer
              (< deadline (System/currentTimeMillis)) nil
              :else (do (Thread/sleep 20) (recur)))))))

(defn- note-base-answers!
  "Keeps the first answers of one batch as one group, for the check of the
  stamps: of each result whose answer is of its own thing, and whose act-id
  the proof has not seen answered before."
  [results]
  (let [seen (into #{} (comp cat (map :act-id)) @base-answers)
        group (into []
                    (comp (filter (fn [r] (and (:answer r) (:mine? r))))
                          (map (fn [r] {:act-id (:act-id (:thing r)) :stamp (:stamp (:answer r))}))
                          (remove (fn [a] (contains? seen (:act-id a))))
                          (distinct))
                    results)]
    (when (seq group) (swap! base-answers conj group))))

(defn- base-result
  "A result as `decide!` gives one, for a thing that was sent to the
  microbatch depot and processed: the thing, the ack, the answer read in a
  loop, whether the answer's digest is the digest of what was sent, and one
  word. A stray has no answer to wait for, so its act-id is read once."
  [x ack]
  (let [act-id (when (map? x) (get x :act-id))
        answer (if (declaration/stray-reason :microbatch x)
                 (answer-of act-id)
                 (await-answer act-id))]
    {:thing x :ack ack :answer answer :mine? (digest/is-digest-of? (:digest answer) x)
     :word (cond (nil? answer) :nothing-yet
                 (= :accepted (:verdict answer)) :accepted
                 :else (:reason answer))}))

(defn- decide-in-base!
  "Sends one thing to the microbatch depot, alone, and reads its answer by
  act-id until one is there. Gives the result, as `decide!` does."
  [x]
  (let [ack (append-to-base! x)
        _ (wait-until-processed!)
        result (base-result x ack)]
    (note-base-answers! [result])
    result))

(defn- decide-in-one-batch!
  "Sends things to the microbatch depot so that one batch of the gate takes
  them all: it pauses the microbatch topology, appends, and lets it go on.
  Gives the result of each, in the order given."
  [xs]
  (rtest/pause-microbatch-topology! (:ipc *hand*) (:module *hand*) "microbatch-gate")
  (let [acks (mapv append-to-base! xs)]
    (rtest/resume-microbatch-topology! (:ipc *hand*) (:module *hand*) "microbatch-gate")
    (wait-until-processed!)
    (let [results (mapv base-result xs acks)]
      (note-base-answers! results)
      results)))

(defn- ask-for-base-locks!
  "Asks for n locks in the base by an act of `who` under `permission`, as
  `ask-for-locks!` does in a streaming layer. Gives the result of the act
  with :locks and :request. The locks are put in the hand for the base."
  [who permission n]
  (let [request-thing (thing :base who permission [])
        request (:act-id request-thing)
        result (decide-in-base! (assoc request-thing :facts [{:e (nth request 2) :k :lock-request :v n}]))
        locks (locks-of request 0 255)]
    (swap! locks-in-hand update :base (fnil into [])
           (map (fn [[index lock]] {:lock-id [request index] :lock lock}) (sort-by key locks)))
    (assoc result :request request :locks locks)))

(defn- base-row
  "Selects one row of a PState of the microbatch gate, by its key, on the
  task of a route: the entity of a fact, the uuid of an answer, the uuid of
  a lock's request. The select goes round the three reads, so it also sees a
  row of a batch that is not closed."
  [pstate route k]
  (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey route}))

(defn- base-fact
  "The row of a place of the base, on the task of its entity, or nil."
  [e k]
  (base-row :mb-facts-ps e (record/place-key :base e k)))

(defn- base-lock-key
  "The key of a lock of the base, from its id."
  [[[_ _ request-uuid] index]]
  (record/lock-key :base request-uuid index))

(defn- rows-by-task
  "Selects the row under a key on each task of the cluster. Gives a sorted
  map from the number of each task that holds a row to that row."
  [pstate k]
  (into (sorted-map)
        (keep-indexed (fn [task pkey]
                        (when-let [row (rama/foreign-select-one (path/keypath k) (get *hand* pstate) {:pkey pkey})]
                          [task row])))
        (rtest/gen-hashing-index-keys tasks)))

(defn- task-of-row
  "The number of the task that holds the row under a key, or nil. For a row
  that is on one task."
  [pstate k]
  (first (keys (rows-by-task pstate k))))

(defn- closed-through
  "The `:closed-through` of the microbatch gate on the task of a route."
  [route]
  (rama/foreign-select-one (path/keypath :closed-through) (:mb-task-ps *hand*) {:pkey route}))

(defn- gates-last-stamp
  "The last stamp that the microbatch gate gave: one for the gate, on the
  gate's task, the task of the class word."
  []
  (rama/foreign-select-one (path/keypath :last-stamp) (:mb-task-ps *hand*) {:pkey :microbatch}))

(defn- base-strays
  "The number of different strays of the microbatch gate under each word,
  over every task."
  []
  (into {}
        (map (fn [word]
               [word (reduce + (map (fn [k] (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                                                     (:mb-task-ps *hand*) {:pkey k}))
                                    (rtest/gen-hashing-index-keys tasks)))]))
        declaration/stray-reasons))

(defn- stored-in-base
  "Each record of the microbatch depot's log, read back: [partition record]."
  []
  (let [depot (:mb-depot *hand*)]
    (for [p (range (:num-partitions (rama/foreign-object-info depot)))
          :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
          r (rama/foreign-depot-read depot p start-offset end-offset)]
      [p r])))

;; ------------------------------------------------------------- the people

(def ^:private alice-own "Alice's permission in her personal layer." [:alice :alice :alice])
(def ^:private alice-in-team "Alice's permission in the group's layer that she makes." [:alice :team :team])
(def ^:private bobs "Bob's permission in Alice's layer, beneath hers." [:bob :alice :alice alice-own])
(def ^:private carols "Carol's permission in Alice's layer, beneath Bob's." [:carol :alice :alice bobs])
(def ^:private daves "Dave's permission in Alice's layer, beneath Carol's: four deep." [:dave :alice :alice carols])

(def ^:private system-own "The system's permission in the base, the base's first." [:system :base :base])
(def ^:private alice-in-base "Alice's permission in the base, beneath the system's." [:alice :base :base system-own])
(def ^:private carol-in-base "Carol's permission in the base, beneath the system's." [:carol :base :base system-own])
(def ^:private dave-in-base "Dave's permission in the base, beneath the system's." [:dave :base :base system-own])
(def ^:private bob-in-base "Bob's permission in the base, beneath Alice's." [:bob :base :base alice-in-base])

(defn- making
  "The making act of a layer: by `who`, naming the first permission of
  `holder`, with the class that the layer starts with."
  [layer who holder]
  (thing layer who [holder layer layer]
         [{:e layer :k :class :v (first-act/class-at-making layer)}
          {:e holder :k :permission :v [holder layer layer]}]))

(defn- alice
  "Alice's act into her personal layer, with these facts."
  ([facts] (alice facts {}))
  ([facts more] (thing :alice :alice alice-own facts more)))

(defn- system-act
  "The system's act into the base, with these facts."
  ([facts] (system-act facts {}))
  ([facts more] (thing :base :system system-own facts more)))

;; ---------------------------------------------------------------- the checks

(deftest what-rama-carries
  (let [kinds {:vector [1 2 3] :list (list 1 2 3) :map {:a {:b 1}} :keyword :tool/class
               :uuid (UUID/randomUUID) :big-integer 100000000000000000000N :decimal 1.50M
               :bytes (byte-array [1 2 -3]) :set #{1 2} :narrow-integer (int 5) :minus-zero -0.0
               :string "a \"text\"" :nothing nil :subvector (subvec [0 1 2] 1)}
        x {:act-id (new-act-id :carrier) :layer :carrier :kinds kinds}
        sent-digest (digest/hex-of x)
        result (decide! x)
        [partition back] (first (filter (fn [[_ r]] (and (map? r) (= (:act-id x) (:act-id r)))) (stored)))
        back-kinds (:kinds back)]
    (check "the thing was acked" :acked (:ack result))
    (check "the gate's digest of the thing as it arrived is the digest that the door took before the append"
           sent-digest (:digest (:answer result)))
    (check "a thing that is no act, with a usable act-id, gets a recorded refusal" :unknown-part (:word result))
    (check "the record read back from the depot's log is another object than the one sent: it was serialised"
           false (identical? x back))
    (check "the digest of what came back from the depot's log is the digest of what was sent"
           sent-digest (digest/hex-of back))
    (check "a vector comes back a vector, a list a list in its order, a plain map a plain map, bytes the same bytes"
           [true true [1 2 3] true [1 2 -3]]
           [(vector? (:vector back-kinds))
            (and (sequential? (:list back-kinds)) (not (vector? (:list back-kinds))))
            (vec (:list back-kinds))
            (and (map? (:map back-kinds)) (not (record? (:map back-kinds))))
            (vec (:bytes back-kinds))])
    (check "a keyword with its namespace, a UUID, a big integer, a decimal and a set come back equal"
           (select-keys kinds [:keyword :uuid :big-integer :decimal :set])
           (select-keys back-kinds [:keyword :uuid :big-integer :decimal :set]))
    (shown "each kind of value comes back from the depot's log with the digest it was sent with:" sent-digest
           "| partition" partition "| the classes that came back:"
           (pr-str (into (sorted-map) (map (fn [[k v]] [k (some-> v class .getSimpleName)])) back-kinds)))
    (shown "the gate took the same digest of the thing as it arrived, and recorded it with its refusal:" (told result))))

(deftest layers-are-made
  (let [before (class-of-layer :alice)
        not-made (decide! (alice [{:e :note-1 :k :text}]))
        request-early (ask-for-locks! :alice :alice alice-own 4)
        by-stranger (decide! (assoc (making :alice :system :alice) :who :mallory))
        base-here (decide! (thing :base :system [:system :base :base]
                                  [{:e :base :k :class :v :microbatch}
                                   {:e :system :k :permission :v [:system :base :base]}]
                                  {:act-id [:base :streaming (UUID/randomUUID)] :class :streaming}))
        personal (decide! (making :alice :system :alice))
        group (decide! (making :team :alice :alice))
        own-uuid (UUID/randomUUID)
        own-layer (UUID/randomUUID)
        by-uuid (decide! (making own-layer own-uuid own-uuid))
        again-by-system (decide! (making :alice :system :alice))
        again-by-holder (decide! (making :team :alice :alice))
        system-again (decide! (thing :alice :system alice-own [{:e :note-1 :k :text}]))]
    (check "before its making, the class of a layer is nothing" nil before)
    (check "an act into a layer that is not made is refused" :layer-not-made (:word not-made))
    (check "a request for locks into a layer that is not made is refused, and makes no lock"
           [:layer-not-made {}] [(:word request-early) (:locks request-early)])
    (check "a making act by a who that is neither the holder nor the system is refused" :layer-not-made (:word by-stranger))
    (check "a making act of the base at the streaming gate is refused" :layer-not-made (:word base-here))
    (check "a person's layer made by the system for that person: accepted, and its class is streaming"
           [:accepted true :streaming] [(:word personal) (:mine? personal) (class-of-layer :alice)])
    (check "the record holds the class as a fact on the layer's own entity, and the person's permission on its holder"
           [:streaming alice-own]
           [(record/plain-value-of (fact-row :alice :alice :class)) (record/plain-value-of (fact-row :alice :alice :permission))])
    (check "the class, the permission and the answer hold one stamp"
           [(:stamp (:answer personal)) (:stamp (:answer personal))]
           [(:stamp (fact-row :alice :alice :class)) (:stamp (fact-row :alice :alice :permission))])
    (check "a group's layer made by the person who creates it: accepted, and its class is streaming"
           [:accepted :streaming] [(:word group) (class-of-layer :team)])
    (check "a layer whose id is a UUID, made by a person whose id is a UUID" [:accepted :streaming]
           [(:word by-uuid) (class-of-layer own-layer)])
    (check "a making act for a layer that is made, by the system under the person's permission, is refused"
           :who-not-the-permissions-holder (:word again-by-system))
    (check "a making act for a layer that is made, by its holder, is refused: its permission is not beneath its own"
           :permission-fact-not-beneath-the-acts-own (:word again-by-holder))
    (check "the system holds no permission in the personal layer that it made: its second act there is refused"
           :who-not-the-permissions-holder (:word system-again))
    (check "the first permission of the group's layer is the one that the first making act wrote"
           alice-in-team (record/plain-value-of (fact-row :team :alice :permission)))
    (shown "an act into a layer that is not made refused:" (told not-made))
    (shown "a person's layer made by the system for that person, with the class streaming:" (told personal)
           "| class-of-layer :alice =" (class-of-layer :alice))
    (shown "a group's layer made by the person who creates it:" (told group) "| class-of-layer :team =" (class-of-layer :team))
    (shown "a making act for a layer that is made is refused: by the system" (:word again-by-system)
           "| by its holder" (:word again-by-holder) "| a second act of the system in a personal layer" (:word system-again))))

(deftest an-act-lands
  (let [request (ask-for-locks! :alice :alice alice-own 16)
        in-hand (first (get @locks-in-hand :alice))
        fact (sealed :alice :note-1 :text "the first text")
        landed (decide! (alice [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (fact-row :alice :note-1 :text)
        the-lock (lock-row (:lock-id in-hand))
        listed (decide! (alice [] {:facts (list (sealed :alice :note-2 :text {:n 1}) (sealed :alice :note-3 :text #{:a}))}))
        list-uuid (nth (:act-id (:thing listed)) 2)
        never-sent (answer-of (new-act-id :alice))]
    (swap! kept assoc :landed landed :landed-lock in-hand)
    (check "a request for 16 locks is accepted" :accepted (:word request))
    (check "one query gives its 16 locks, each 32 bytes of way 1, no two the same"
           [16 #{32} #{1} 16]
           [(count (:locks request)) (set (map #(alength ^bytes (:bytes %)) (vals (:locks request))))
            (set (map :way (vals (:locks request)))) (count (set (map #(vec (:bytes %)) (vals (:locks request)))))])
    (check "the request's own fact is a row: its count, as a plain value on the uuid of its act"
           16 (record/plain-value-of (fact-row :alice (nth (:request request) 2) :lock-request)))
    (check "one act lands through the streaming gate and is read back by act-id: accepted, with the digest of what was sent"
           [:acked :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the sealed bytes as sent, the fact that wrote it, its lock, and the stamp of the answer"
           [(vec (:sealed fact)) act-uuid 0 (nth (first (:lock-id fact)) 2) (second (:lock-id fact)) (:stamp (:answer landed))]
           [(vec (:value the-row)) (:by-act the-row) (:by-index the-row) (:lock-act the-row) (:lock-index the-row) (:stamp the-row)])
    (check "the lock's row has the bytes that the door read, and now names the fact that it seals"
           [(vec (:bytes (:lock in-hand))) 1 act-uuid 0]
           [(vec (:bytes the-lock)) (:way the-lock) (:seals-act the-lock) (:seals-index the-lock)])
    (check "the lock opens the place's bytes at its place: the value as it was sealed"
           (vec (plain-bytes "the first text"))
           (vec (seal/open (record/lock-of the-lock) [:alice :note-1 :text] (:value the-row))))
    (check "an act whose facts came as a list is accepted, and each fact has its index in the order sent"
           [:accepted [list-uuid 0] [list-uuid 1]]
           [(:word listed)
            ((juxt :by-act :by-index) (fact-row :alice :note-2 :text))
            ((juxt :by-act :by-index) (fact-row :alice :note-3 :text))])
    (check "an act-id that was never sent has no answer: nothing yet" nil never-sent)
    (shown "a request for 16 locks, read with one query:" (told request) "| locks" (count (:locks request)))
    (shown "one act landing through the streaming gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "its place in the record: written by" (str (:by-act the-row)) "index" (:by-index the-row)
           "stamp" (:stamp the-row) "sealed bytes" (alength ^bytes (:value the-row))
           "| its lock sits beside it and names the fact:" (str (:seals-act the-lock)) (:seals-index the-lock))))

(deftest refusals-and-repeats
  (let [accepted-thing (:thing (:landed @kept))
        first-fact-id [(:act-id accepted-thing) 0]
        unsealed (decide! (alice [{:e :note-9 :k :text :v "a plain value"}]))
        answer-before (answer-of (:act-id accepted-thing))
        stamp-before (last-stamp :alice)
        repeat-ack (send! accepted-thing)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (do (send! (:thing unsealed)) (answer-of (:act-id (:thing unsealed))))
        stamp-after (last-stamp :alice)
        other-under-used (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id accepted-thing)))
        junk (decide! {:act-id (new-act-id :alice) :layer :alice :junk 1})
        good-under-junk (decide! (assoc (alice [{:e :note-8 :k :text}]) :act-id (:act-id (:thing junk))))
        stale (decide! (alice [(assoc (sealed :alice :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        empty-place (decide! (alice [(assoc (sealed :alice :note-7 :text "x") :replaces first-fact-id)]))
        replaced (decide! (alice [(assoc (sealed :alice :note-1 :text "the second text") :replaces first-fact-id)]))
        second-fact-id [(:act-id (:thing replaced)) 0]
        stale-now (decide! (alice [(assoc (sealed :alice :note-1 :text "late") :replaces first-fact-id)]))
        repeat-of-replace (do (send! (:thing replaced)) (answer-of (:act-id (:thing replaced))))
        over (decide! (alice [(sealed :alice :note-1 :text "the third text")]))
        over-row (fact-row :alice :note-1 :text)
        retract (decide! (alice [{:e :note-1 :k :text}]))
        retract-row (fact-row :alice :note-1 :text)
        over-retract (decide! (alice [(assoc (sealed :alice :note-1 :text "after the retract")
                                             :replaces [(:act-id (:thing retract)) 0])]))
        retract-of-retract (decide! (alice [{:e :note-6 :k :text}]))
        retract-again (decide! (alice [{:e :note-6 :k :text}]))
        onto-retract (decide! (alice [(sealed :alice :note-6 :text "onto a retract, naming nothing")]))
        on-the-layer (decide! (alice [(sealed :alice :alice :title "a fact of the layer's own entity, under a plain key")]))
        twice (decide! (alice [{:e :note-10 :k :text} {:e :note-10 :k :text}]))
        own-base (alice [])
        own-fact (decide! (assoc own-base :facts [{:e :note-10 :k :text}
                                                  {:e :note-11 :k :text :replaces [(:act-id own-base) 0]}]))
        one-lock (take-lock! :alice)
        lock-twice (decide! (alice [(seal-with one-lock :alice :note-10 :text "a") (seal-with one-lock :alice :note-11 :text "b")]))
        lock-then-used (decide! (alice [(seal-with one-lock :alice :note-10 :text "a")]))]
    (check "an unsealed value is refused, and the refusal is recorded with its reason, a stamp and the digest"
           [:refused :not-sealed true true]
           [(:verdict (:answer unsealed)) (:reason (:answer unsealed)) (int? (:stamp (:answer unsealed))) (:mine? unsealed)])
    (check "a refusal writes nothing at the place" nil (fact-row :alice :note-9 :text))
    (check "a repeat is acked and answered from the record: the same answer, with the stamp of the first decision"
           [:acked answer-before] [repeat-ack answer-after])
    (check "a repeat of a refused act reads the recorded refusal" (:answer unsealed) refused-again)
    (check "not decided twice: the task's last stamp is the same after two repeats" stamp-before stamp-after)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false]
           [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (fact-row :alice :note-8 :text))
    (check "a thing that is no act uses its act-id: a good act sent under it reads the first thing's refusal"
           [:unknown-part true :unknown-part false]
           [(:word junk) (:mine? junk) (:word good-under-junk) (:mine? good-under-junk)])
    (check "a stale replaces is refused: a fact that is not the current fact of the place" :replaced-fact-not-current (:word stale))
    (check "a fact that names something to replace on an empty place is stale" :replaced-fact-not-current (:word empty-place))
    (check "a fact that names the current fact of its place is accepted" :accepted (:word replaced))
    (check "a second act that names the fact that was replaced is stale" :replaced-fact-not-current (:word stale-now))
    (check "a repeat of the accepted replace is still accepted, although its replaces would now be stale"
           (:answer replaced) repeat-of-replace)
    (check "a fact that names nothing to replace is never stale: it becomes the current fact of a place that holds one"
           [:accepted (nth (:act-id (:thing over)) 2)] [(:word over) (:by-act over-row)])
    (check "a retract is recorded as the current fact of its place: a row with no value and no lock"
           [:accepted (nth (:act-id (:thing retract)) 2) false false]
           [(:word retract) (:by-act retract-row) (contains? retract-row :value) (contains? retract-row :lock-act)])
    (check "a later fact can replace the retract" :accepted (:word over-retract))
    (check "a retract on an empty place, and a retract of a retract, are accepted" [:accepted :accepted]
           [(:word retract-of-retract) (:word retract-again)])
    (check "a value that names nothing, on a place that holds a retract, is accepted" :accepted (:word onto-retract))
    (check "a value fact on the layer's own entity, under a plain key, is a fact like any other" :accepted (:word on-the-layer))
    (check "an act names a place once: two facts on one place, and a fact that replaces a fact of its own act, are refused"
           [:place-named-twice :replaces-own-fact nil] [(:word twice) (:word own-fact) (fact-row :alice :note-11 :text)])
    (check "two facts of one act under one lock are refused, and the lock is free for a later act"
           [:lock-named-twice :accepted] [(:word lock-twice) (:word lock-then-used)])
    (shown "a refusal recorded with its reason:" (pr-str (:answer unsealed)))
    (shown "an unsealed value refused:" (told unsealed))
    (shown "a repeat (same act-id sent twice) answered from the record, not decided twice: stamp before" (:stamp answer-before)
           "after" (:stamp answer-after) "| the task's last stamp before" stamp-before "after" stamp-after)
    (shown "a second, different act under a used act-id told so: the digest of the answer" (subs (:digest (:answer other-under-used)) 0 16)
           "is not the digest of what was sent" (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "a stale replaces refused:" (told stale) "| the current fact named:" (told replaced) "| the replaced fact named again:" (told stale-now))
    (shown "a place named twice:" (:word twice) "| a fact that replaces a fact of its own act:" (:word own-fact)
           "| two facts under one lock:" (:word lock-twice))
    (shown "a fact that names nothing is never stale:" (told over) "| a retract is the current fact of its place:" (told retract)
           "| a later fact replaces the retract:" (told over-retract) "(second fact id" (short-id (first second-fact-id)) ")")))

(deftest settings-and-permissions
  (let [setting (decide! (alice [{:e :team :k :class :v :streaming}]))
        other-layer (decide! (thing :alice :alice alice-in-team [{:e :note-5 :k :text}]))
        not-yet (decide! (thing :alice :bob bobs [{:e :note-5 :k :text}]))
        give (decide! (alice [{:e :bob :k :permission :v bobs}]))
        not-yet-again (do (send! (:thing not-yet)) (answer-of (:act-id (:thing not-yet))))
        bob-asks (ask-for-locks! :alice :bob bobs 2)
        bob-writes (decide! (thing :alice :bob bobs [(sealed :alice :bobs-note :text "bob's text")]))
        beside (decide! (thing :alice :bob bobs [{:e :carol :k :permission :v [:carol :alice :alice alice-own]}]))
        by-no-holder (decide! (thing :alice :carol bobs [{:e :dave :k :permission :v [:dave :alice :alice bobs]}]))
        carol-not-given (decide! (thing :alice :carol [:carol :alice :alice alice-own] [{:e :note-5 :k :text}]))
        give-carol (decide! (thing :alice :bob bobs [{:e :carol :k :permission :v carols}]))
        give-dave (decide! (thing :alice :carol carols [{:e :dave :k :permission :v daves}]))
        dave-acts (decide! (thing :alice :dave daves [{:e :daves-note :k :text}]))
        five-deep (decide! (thing :alice :dave daves [{:e :erin :k :permission :v [:erin :alice :alice daves]}]))
        granted-elsewhere (decide! (alice [{:e :erin :k :permission :v [:erin :alice :team alice-own]}]))
        for-elsewhere (decide! (alice [{:e :erin :k :permission :v [:erin :team :alice alice-own]}]))
        at-the-top (decide! (alice [{:e :erin :k :permission :v [:erin :alice :alice]}]))
        give-again (decide! (alice [{:e :bob :k :permission :v bobs}]))
        end-it (decide! (alice [{:e :bob :k :permission}]))
        class-again (decide! (alice [{:e :alice :k :class :v :streaming}]))
        class-other (decide! (alice [{:e :alice :k :class :v :microbatch}]))
        class-retract (decide! (alice [{:e :alice :k :class}]))
        bob-still (decide! (thing :alice :bob bobs [{:e :bobs-note-2 :k :text}]))
        own-give (decide! (thing :alice :frank [:frank :alice :alice alice-own]
                                 [{:e :frank :k :permission :v [:frank :alice :alice alice-own]}]))
        with-stale (decide! (alice [{:e :erin :k :permission :v [:erin :alice :alice alice-own]}
                                    (assoc (sealed :alice :note-1 :text "stale") :replaces [(new-act-id :alice) 0])]))
        erin-acts (decide! (thing :alice :erin [:erin :alice :alice alice-own] [{:e :note-5 :k :text}]))]
    (check "a setting key on another layer is refused, and that layer's class is as it was"
           [:setting-not-on-the-layer :streaming] [(:word setting) (class-of-layer :team)])
    (check "an act with a permission for another layer is refused by the form" :permission-not-in-layer (:word other-layer))
    (check "an act under a permission that does not stand is refused" :permission-does-not-stand (:word not-yet))
    (check "a permission given beneath another by its holder is accepted, and sits on its holder as a fact"
           [:accepted bobs] [(:word give) (record/plain-value-of (fact-row :alice :bob :permission))])
    (check "a repeat of the refused act reads its recorded refusal, although the act would now pass"
           (:answer not-yet) not-yet-again)
    (check "an act under it is accepted: a request for locks, and a value sealed under one of them"
           [:accepted :accepted] [(:word bob-asks) (:word bob-writes)])
    (check "a permission given by one who does not hold the one above is refused: it is not beneath the giver's own"
           :permission-fact-not-beneath-the-acts-own (:word beside))
    (check "an act under a permission by a who that is not its holder is refused" :who-not-the-permissions-holder (:word by-no-holder))
    (check "the refused give gave nothing: an act under that permission is refused" :permission-does-not-stand (:word carol-not-given))
    (check "a chain of four: each holder gives one beneath its own, and the last acts under it"
           [:accepted :accepted :accepted] [(:word give-carol) (:word give-dave) (:word dave-acts)])
    (check "a permission five deep is no permission: the holder of one four deep cannot give"
           :permission-fact-not-a-permission (:word five-deep))
    (check "a granted-in that is not the act's layer is refused" :permission-fact-granted-in-another-layer (:word granted-elsewhere))
    (check "a permission fact for another layer is refused" :permission-fact-for-another-layer (:word for-elsewhere))
    (check "a permission with nothing above it, in a layer that is made, is refused" :permission-fact-not-beneath-the-acts-own (:word at-the-top))
    (check "a permission that stands is not given again: a store key takes one write for a place" :store-key-takes-one-write (:word give-again))
    (check "a retract of a permission is refused with the word that says it is not decided" :ending-a-permission-not-decided (:word end-it))
    (check "a class fact after the making: the same class, the other class, a retract"
           [:store-key-takes-one-write :changing-a-class-not-decided :store-key-retract-not-built]
           [(:word class-again) (:word class-other) (:word class-retract)])
    (check "after each of those refusals the permission stands and the class is as it was"
           [:accepted :streaming] [(:word bob-still) (class-of-layer :alice)])
    (check "an act that gives a permission and names that permission as its own is refused: it does not stand before the act"
           :permission-does-not-stand (:word own-give))
    (check "whole or nothing: an act that gives a permission and carries a stale fact is refused, and the permission is not given"
           [:replaced-fact-not-current :permission-does-not-stand nil]
           [(:word with-stale) (:word erin-acts) (fact-row :alice :erin :permission)])
    (shown "a setting key on another layer refused:" (told setting))
    (shown "an act with a permission for another layer refused:" (told other-layer))
    (shown "an act under a permission that does not stand refused:" (told not-yet))
    (shown "a permission given beneath another by its holder:" (told give) "| and an act under it accepted:" (told bob-writes))
    (shown "a permission given by one who does not hold the one above refused:" (told beside)
           "| by a who that is not the holder of the permission it names:" (told by-no-holder))
    (shown "a chain four deep stands, and a fifth is refused:" (:word give-carol) (:word give-dave) (:word dave-acts) (:word five-deep))
    (shown "granted-in must be the act's layer:" (:word granted-elsewhere) "| a store key takes one write:" (:word give-again)
           "| ending a permission:" (:word end-it) "| a class after the making:" (:word class-again) (:word class-other) (:word class-retract))
    (shown "whole or nothing: a give with a stale fact" (:word with-stale) "| an act under the permission that was not given" (:word erin-acts))))

(deftest the-path-of-a-lock
  (let [used-lock (:landed-lock @kept)
        second-value (decide! (alice [(seal-with used-lock :alice :note-20 :text "a second value")]))
        free (take-lock! :alice)
        refused-first (decide! (alice [(assoc (seal-with free :alice :note-21 :text "v") :replaces [(new-act-id :alice) 0])]))
        free-after (lock-row (:lock-id free))
        copied-bytes (seal-with free :alice :note-21 :text "v")
        copied (decide! (alice [(assoc copied-bytes :e :note-22)]))
        any-bytes (decide! (alice [(assoc copied-bytes :sealed (byte-array 40))]))
        then-used (decide! (alice [copied-bytes]))
        unknown-lock (decide! (alice [(seal-with {:lock-id [(new-act-id :alice) 0] :lock (:lock free)} :alice :note-23 :text "v")]))
        team-request (ask-for-locks! :team :alice alice-in-team 2)
        team-lock (take-lock! :team)
        in-other-layer (decide! (alice [(seal-with team-lock :alice :note-24 :text "v")]))
        renamed (decide! (alice [(seal-with (assoc team-lock :lock-id [[:alice :streaming (nth (first (:lock-id team-lock)) 2)] 0])
                                            :alice :note-24 :text "v")]))
        team-uses (decide! (thing :team :alice alice-in-team [(seal-with team-lock :team :plan :text "the team's text")]))
        by-stranger (ask-for-locks! :alice :mallory alice-own 3)
        too-many (ask-for-locks! :alice :alice alice-own 257)
        request (ask-for-locks! :alice :alice alice-own 16)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (:locks request))
        repeat-ack (send! (:thing request))
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of (:request request) 0 255))
        bobs-lock (take-lock! :alice)
        by-another-who (decide! (thing :alice :bob bobs [(seal-with bobs-lock :alice :bobs-note-3 :text "sealed under a lock that Alice asked for")]))
        both-base (alice [])
        both (decide! (assoc both-base :facts [{:e (nth (:act-id both-base) 2) :k :lock-request :v 2}
                                               (sealed :alice :note-25 :text "with a request")]))]
    (check "a second value under a lock that already seals one is refused" :lock-already-seals-a-value (:word second-value))
    (check "a lock that a refused act named is free: its row names no fact"
           [:replaced-fact-not-current false] [(:word refused-first) (record/seals-a-fact? free-after)])
    (check "sealed bytes copied to another place do not open" :lock-does-not-open-the-value (:word copied))
    (check "any bytes under a real lock id do not open" :lock-does-not-open-the-value (:word any-bytes))
    (check "after those three refusals the lock seals a value of a later act" :accepted (:word then-used))
    (check "a sealed value under a lock id that the store did not make is refused" :lock-not-made-for-this-layer (:word unknown-lock))
    (check "a sealed value under a lock that the store made for another layer is refused, under its own id and under this layer's"
           [:accepted :lock-not-made-for-this-layer :lock-not-made-for-this-layer]
           [(:word team-request) (:word in-other-layer) (:word renamed)])
    (check "that lock is still free in its own layer" :accepted (:word team-uses))
    (check "a refused request makes no lock: by a who that is not the holder, and for 257 locks"
           [:who-not-the-permissions-holder {} :too-many-locks-requested {}]
           [(:word by-stranger) (:locks by-stranger) (:word too-many) (:locks too-many)])
    (check "a request sent twice makes no lock again: each lock has the bytes that the first run made"
           [:acked bytes-before] [repeat-ack bytes-after])
    (check "a read of a part of a request's locks: one index, the last two, a range cut to 0 to 255, an empty range"
           [[10] [14 15] 16 {}]
           [(vec (keys (locks-of (:request request) 10 10))) (vec (sort (keys (locks-of (:request request) 14 99))))
            (count (locks-of (:request request) -5 999)) (locks-of (:request request) 9 3)])
    (check "a lock in each state gives the same bytes: one that seals a value reads as the door first read it"
           (vec (:bytes (:lock free))) (vec (:bytes (get (locks-of (first (:lock-id free)) 0 255) (second (:lock-id free))))))
    (check "a lock is bound to its layer, not to who asked for it: another holder of the layer seals under it" :accepted (:word by-another-who))
    (check "one act asks for locks and carries a sealed value: accepted, and its two locks are made"
           [:accepted 2] [(:word both) (count (locks-of (:act-id (:thing both)) 0 255))])
    (shown "the path of a lock: asked for and read (" (count (:locks request)) "locks ), used, and then a second value under it refused:" (told second-value))
    (shown "the lock of a refused act is still free:" (told refused-first) "then" (told then-used))
    (shown "sealed bytes copied to another place do not open:" (told copied) "| any bytes under a real lock:" (:word any-bytes))
    (shown "a lock that the store did not make for this layer:" (:word unknown-lock) "| a lock of another layer:" (:word in-other-layer)
           "| a refused request makes no lock:" (:word by-stranger) (pr-str (:locks by-stranger)))
    (shown "a request sent twice gives the same" (count bytes-after) "locks, byte for byte")))

(deftest strays-and-limits
  (let [counts (fn [] (into {} (map (fn [w] [w (strays w)])) declaration/stray-reasons))
        before (counts)
        long (keyword (apply str (repeat 300 "a")))
        no-act-id 5
        malformed {:act-id [:alice :streaming] :layer :alice :marker (UUID/randomUUID)}
        other-gate (assoc (alice [{:e :note-30 :k :text}]) :act-id [:alice :microbatch (UUID/randomUUID)] :class :microbatch)
        other-layer (assoc (alice [{:e :note-30 :k :text}]) :layer :team)
        too-long {:act-id [long :streaming (UUID/randomUUID)] :layer long}
        acks (mapv send! [no-act-id malformed other-gate other-layer too-long other-gate other-gate])
        after (counts)
        second-of-a-word (do (send! (assoc malformed :marker (UUID/randomUUID))) (counts))
        many (fn [n] (alice (map (fn [i] {:e (keyword (str "many-" i)) :k :text}) (range n))))
        facts-256 (decide! (many 256))
        facts-257 (decide! (many 257))
        based-on (fn [n] (into {} (map (fn [i] [[[:alice :streaming (UUID. 0 1)] i] 1])) (range n)))
        based-1024 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1024)}))
        based-1025 (decide! (alice [{:e :note-31 :k :text}] {:based-on (based-on 1025)}))
        big (take-lock! :alice)
        sealed-over (decide! (alice [(assoc (seal-with big :alice :note-32 :text "v") :sealed (byte-array 65537))]))
        sealed-at (decide! (alice [{:e :note-32 :k :text :lock-id (:lock-id big)
                                    :sealed (seal/seal (:lock big) [:alice :note-32 :text] (byte-array (- 65536 28)))}]))
        long-entity (decide! (alice [{:e long :k :text}]))
        now (System/currentTimeMillis)
        fact-id [[:alice :streaming (UUID. 0 1)] 0]
        far-ahead (decide! (alice [{:e :note-33 :k :text}] {:based-on {fact-id (stamp/pack (+ now 60000) 0)}}))
        claim (decide! (alice [{:e :note-33 :k :text}] {:claimed-when Long/MAX_VALUE}))
        carried (stamp/pack (+ (System/currentTimeMillis) 5000) 7)
        near-ahead (decide! (alice [{:e :note-34 :k :text}] {:based-on {fact-id carried} :because-of (new-act-id :alice)}))
        next-one (decide! (alice [{:e :note-35 :k :text}]))]
    (check "each stray was acked" (vec (repeat 7 :acked)) acks)
    (check "a stray is kept under its word, once however often it comes: five different strays, one of them sent three times"
           {:no-act-id 1 :act-id-malformed 1 :id-too-long 1 :act-id-of-the-other-gate 1 :act-id-of-another-layer 1}
           (merge-with - after before))
    (check "what is kept is the digest of the stray thing"
           [true true] [(contains? (stray-digests :act-id-of-the-other-gate) (digest/hex-of other-gate))
                        (contains? (stray-digests :no-act-id) (digest/hex-of no-act-id))])
    (check "the count of strays under a word is the number of different ones: a second, different one makes it two more than before"
           2 (- (:act-id-malformed second-of-a-word) (:act-id-malformed before)))
    (check "a stray gets no answer: an act-id of the other gate, and an act-id whose layer is not the thing's layer"
           [nil nil] [(answer-of (:act-id other-gate)) (answer-of (:act-id other-layer))])
    (check "a stray writes no fact" nil (fact-row :alice :note-30 :text))
    (check "an act of 256 facts is accepted, and each fact is a row; one of 257 is refused"
           [:accepted 255 :too-many-facts] [(:word facts-256) (:by-index (fact-row :alice :many-255 :text)) (:word facts-257)])
    (check "1,024 entries of based-on are taken; 1,025 are refused" [:accepted :too-many-based-on] [(:word based-1024) (:word based-1025)])
    (check "a sealed value of 65,537 bytes is refused; one of 65,536 is accepted, under the same lock"
           [:sealed-value-too-large :accepted 65536] [(:word sealed-over) (:word sealed-at) (alength ^bytes (:value (fact-row :alice :note-32 :text)))])
    (check "an id that prints to more than 256 characters is refused, and the refusal is recorded under the act-id" :id-too-long (:word long-entity))
    (check "an act that carries a stamp more than 10 seconds ahead of the gate's clock is refused"
           :carried-stamp-too-far-ahead (:word far-ahead))
    (check "its answer holds an ordinary stamp, not one that follows the carried stamp"
           true (< (stamp/time-of (:stamp (:answer far-ahead))) (+ now 30000)))
    (check "claimed-when far ahead is a claim: the act is accepted, and its stamp does not follow it"
           [:accepted true] [(:word claim) (< (stamp/time-of (:stamp (:answer claim))) (+ now 30000))])
    (check "an act that carries a stamp 5 seconds ahead, based on a fact and because of an act that the record does not hold, is accepted, with a stamp later than the one it carries"
           [:accepted true] [(:word near-ahead) (> (:stamp (:answer near-ahead)) carried)])
    (check "and the next stamp of the task is later still" true (> (:stamp (:answer next-one)) (:stamp (:answer near-ahead))))
    (shown "a stray is kept under its word and gets no answer; strays seen, by word:" (pr-str (merge-with - after before))
           "| the same stray sent three times counts once | answer of a stray:" (pr-str (answer-of (:act-id other-gate))))
    (shown "a thing over each limit is refused:" (:word facts-257) (:word based-1025) (:word sealed-over) (:word long-entity)
           "| at each limit accepted:" (:word facts-256) (:word based-1024) (:word sealed-at))
    (shown "a carried stamp too far ahead:" (told far-ahead) "| 5 seconds ahead:" (told near-ahead) "carried" carried)))

(deftest the-reads-take-anything
  (let [u (UUID/randomUUID)
        long (keyword (apply str (repeat 300 "a")))
        accepted (:act-id (:thing (:landed @kept)))
        ask (fn [query & args] (apply rama/foreign-invoke-query (get *hand* query) args))]
    (check "the hand gives nothing itself for what is no id or no act-id, and calls no query"
           [nil nil nil nil {} {}]
           [(class-of-layer nil) (class-of-layer "alice") (answer-of nil) (answer-of [:alice :streaming]) (locks-of nil 0 1)
            (locks-of accepted "0" 1)])
    (check "the read by act-id, called with what is no act-id of the layer given, gives nothing"
           [nil nil nil nil nil true]
           [(ask :answer :alice [:team :streaming u]) (ask :answer :alice 5) (ask :answer :alice [:alice :streaming])
            (ask :answer "alice" ["alice" :streaming u]) (ask :answer long [long :streaming u])
            (some? (ask :answer :alice accepted))])
    (check "an act-id of the class microbatch, asked for with its layer and not with its uuid as the routing id, gets nothing" nil (ask :answer :alice [:alice :microbatch (nth accepted 2)]))
    (check "the read of a class, called with what is no id, or with a layer that is not made, gives nothing"
           [nil nil nil] [(ask :class "alice") (ask :class long) (ask :class :nobody)])
    (check "the read of locks, called with an act-id that made no lock, indexes that are no integers, or the other class, gives an empty map"
           [{} {} {} {}]
           [(ask :locks :alice accepted 0 255) (ask :locks :alice accepted "0" 3) (ask :locks :alice [:alice :microbatch u] 0 3)
            (ask :locks :alice [:team :streaming u] 0 3)])
    (check "the same uuid in two layers is two answers: an answer is found by its layer and its uuid"
           [nil nil] [(ask :answer :team [:team :streaming (nth accepted 2)]) (answer-of [:team :streaming (nth accepted 2)])])
    (shown "the three reads give nothing for what is no id, no act-id, another layer's act-id or the other class, and do not fail")))

;; ----------------------------------------------- the checks of the base's gate

(deftest the-base-is-made
  (let [before (class-of-layer :base)
        on-the-other-depot (decide! (thing :base :system system-own [{:e :early :k :note}]
                                           {:act-id [:base :streaming (UUID/randomUUID)] :class :streaming}))
        sent-at (System/currentTimeMillis)
        early (decide-in-base! (system-act [{:e :early :k :note}]))
        waited (- (System/currentTimeMillis) sent-at)
        [made-a made-b] (decide-in-one-batch! [(making :base :system :system)
                                               (update (making :base :system :system) :facts (comp vec reverse))])
        [winner loser] (if (= :accepted (:word made-a)) [made-a made-b] [made-b made-a])
        class-rows (rows-by-task :mb-facts-ps (record/place-key :base :base :class))
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :system :permission))
        after (decide-in-base! (system-act [{:e :early :k :note}]))]
    (check "before its making, the class of the base is nothing" nil before)
    (check "an act for the base on the streaming depot, before the base is made, is refused as an act into a layer that is not made"
           :layer-not-made (:word on-the-other-depot))
    (check "an act into the base before it is made is refused, and its answer is found from its act-id alone"
           [:appended :layer-not-made true] [(:ack early) (:word early) (:mine? early)])
    (check "the answer of an act that is alone in its batch, with nothing sent after it, is readable within the hand's time limit"
           true (< waited time-limit-ms))
    (check "two making acts of the base in one batch, their facts in the two orders: one is accepted, the other refused"
           [:accepted true :permission-fact-not-beneath-the-acts-own] [(:word winner) (:mine? winner) (:word loser)])
    (check "the one that is accepted holds the earlier stamp" true (< (:stamp (:answer winner)) (:stamp (:answer loser))))
    (check "the base made by the system: its class is microbatch" :microbatch (class-of-layer :base))
    (check "the class row and the system's permission row are on every task, each with the stamp of the making act's answer"
           [tasks tasks #{(:stamp (:answer winner))}]
           [(count class-rows) (count permission-rows) (set (map :stamp (concat (vals class-rows) (vals permission-rows))))])
    (check "the rows hold the class as a fact on the layer's own entity, and the system's permission on its holder"
           [:microbatch system-own]
           [(record/plain-value-of (first (vals class-rows))) (record/plain-value-of (first (vals permission-rows)))])
    (check "after the making, the same act of the system into the base is accepted" :accepted (:word after))
    (shown "an act into the base before it is made refused:" (told early) "| its answer was readable" waited
           "ms after the append, with nothing sent after it")
    (shown "the base made by the system, with the class microbatch:" (told winner) "| class-of-layer :base =" (class-of-layer :base)
           "| the class row is on" (count class-rows) "tasks, the system's permission on" (count permission-rows))
    (shown "two making acts of the base in one batch: one accepted, the other" (:word loser))))

(deftest an-act-lands-in-the-base
  (let [candidates (mapv #(keyword (str "doc-" %)) (range 1 9))
        seen (decide-in-one-batch! (mapv (fn [e] (system-act [{:e e :k :seen}])) candidates))
        task-of-entity (into {} (map (fn [e] [e (task-of-row :mb-facts-ps (record/place-key :base e :seen))])) candidates)
        firsts (mapv first (vals (group-by task-of-entity candidates)))
        three (vec (take 3 (concat firsts (remove (set firsts) candidates))))
        request (ask-for-base-locks! :system system-own 16)
        request-uuid (nth (:request request) 2)
        in-hand (first (get @locks-in-hand :base))
        e (first three)
        fact (sealed :base e :title "the first text of the base")
        landed (decide-in-base! (system-act [fact]))
        act-uuid (nth (:act-id (:thing landed)) 2)
        the-row (base-fact e :title)
        lock-key (base-lock-key (:lock-id in-hand))
        beside (base-row :mb-locks-ps e lock-key)
        at-home (base-row :mb-locks-ps request-uuid lock-key)
        by-task (rows-by-task :mb-locks-ps lock-key)
        home-task (task-of-row :mb-answers-ps (record/answer-key :base request-uuid))
        read-after (locks-of (:request request) 0 255)
        never-sent (answer-of (new-act-id :base))]
    (swap! kept assoc :base-landed landed :base-landed-lock in-hand :base-entities three :base-task-of-entity task-of-entity)
    (check "eight acts of one batch on eight entities are each accepted, and their facts are on more than one task"
           [(vec (repeat 8 :accepted)) true] [(mapv :word seen) (< 1 (count (set (vals task-of-entity))))])
    (check "a request for 16 locks in the base is accepted, and one query gives its 16 locks, each 32 bytes of way 1, no two the same"
           [:accepted 16 #{32} #{1} 16]
           [(:word request) (count (:locks request)) (set (map #(alength ^bytes (:bytes %)) (vals (:locks request))))
            (set (map :way (vals (:locks request)))) (count (set (map #(vec (:bytes %)) (vals (:locks request)))))])
    (check "the request's fact, its answer and its locks are on one task, the task of its uuid"
           [home-task home-task]
           [(task-of-row :mb-facts-ps (record/place-key :base request-uuid :lock-request))
            (task-of-row :mb-locks-ps (record/lock-key :base request-uuid 15))])
    (check "one act lands through the microbatch gate and is read back by act-id: accepted, with the digest of what was sent"
           [:appended :accepted nil true] [(:ack landed) (:verdict (:answer landed)) (:reason (:answer landed)) (:mine? landed)])
    (check "the place holds the sealed bytes as sent, the fact that wrote it, its lock, and the stamp of the answer"
           [(vec (:sealed fact)) act-uuid 0 request-uuid (second (:lock-id fact)) (:stamp (:answer landed))]
           [(vec (:value the-row)) (:by-act the-row) (:by-index the-row) (:lock-act the-row) (:lock-index the-row) (:stamp the-row)])
    (check "the lock's row beside the value, on the value's task, has the bytes that the door read and names the fact that it seals"
           [(vec (:bytes (:lock in-hand))) 1 act-uuid 0]
           [(vec (:bytes beside)) (:way beside) (:seals-act beside) (:seals-index beside)])
    (check "the lock's row at its home names the fact, and holds no bytes unless the home is the value's task"
           [act-uuid 0 (= home-task (task-of-entity e))]
           [(:seals-act at-home) (:seals-index at-home) (contains? at-home :bytes)])
    (check "one copy of the lock's bytes: over every task, one row under the lock's key holds bytes"
           1 (count (filter #(contains? % :bytes) (vals by-task))))
    (check "a sealed value in the base is opened by its lock: the bytes beside the value open the place's bytes"
           (vec (plain-bytes "the first text of the base"))
           (vec (seal/open (record/lock-of beside) [:base e :title] (:value the-row))))
    (check "the read of the request's locks then gives the used lock with its way and no bytes, and each other lock with its bytes"
           [{:bytes nil :way 1} 15]
           [(get read-after (second (:lock-id in-hand))) (count (filter :bytes (vals read-after)))])
    (check "an act-id of the base that was never sent has no answer: nothing yet" nil never-sent)
    (shown "a request for 16 locks in the base, read with one query:" (told request) "| locks" (count (:locks request)))
    (shown "one act landing through the microbatch gate and read back by act-id:" (pr-str (:answer landed)))
    (shown "a sealed value in the base opened by its lock, with one copy of the lock's bytes: the value on task" (task-of-entity e)
           "| rows under the lock's key, by task, with bytes?" (pr-str (into (sorted-map) (map (fn [[task row]] [task (contains? row :bytes)])) by-task))
           "| the lock's home is task" home-task "| opened:" (String. ^bytes (seal/open (record/lock-of beside) [:base e :title] (:value the-row)) "UTF-8"))))

(defn- seen-before-closing
  "Sends one request for a lock, alone, and watches the answers table
  itself, round the reads, for the row of its answer. At the first sight of
  the row it selects the row of the new lock and the `:closed-through` of
  the task, then reads the answer and the locks as a door does. Gives
  {:thing :row :lock-row :closed-through :read :locks-read}."
  []
  (let [request-thing (system-act [])
        act-id (:act-id request-thing)
        uuid (nth act-id 2)
        x (assoc request-thing :facts [{:e uuid :k :lock-request :v 1}])
        _ (append-to-base! x)
        deadline (+ (System/currentTimeMillis) time-limit-ms)
        row (loop []
              (or (base-row :mb-answers-ps uuid (record/answer-key :base uuid))
                  (when (< (System/currentTimeMillis) deadline)
                    (Thread/sleep 1)
                    (recur))))
        lock-row (base-row :mb-locks-ps uuid (record/lock-key :base uuid 0))
        closed (closed-through uuid)]
    {:thing x :row row :lock-row lock-row :closed-through closed
     :read (answer-of act-id) :locks-read (locks-of act-id 0 255)}))

(deftest an-answer-is-readable-when-its-batch-is-closed
  (let [tries (loop [n 1]
                (let [seen (seen-before-closing)
                      result (do (wait-until-processed!) (base-result (:thing seen) :appended))]
                  (note-base-answers! [result])
                  (if (or (and (:row seen) (nil? (:read seen))) (<= 5 n))
                    (assoc seen :after result :tries n)
                    (recur (inc n)))))
        {:keys [thing row lock-row read locks-read after]} tries
        act-id (:act-id thing)
        uuid (nth act-id 2)]
    (check "the row of the answer and the row of its new lock are in the tables, the answer's stamp later than its task's closed-through"
           [true true true]
           [(some? row) (some? (:bytes lock-row))
            (boolean (and row (or (nil? (:closed-through tries)) (< (:closed-through tries) (:stamp row)))))])
    (check "and the reads give nothing of them: no answer by act-id, and no lock: the batch is not closed" [nil {}] [read locks-read])
    (check "after the next run of the gate the read gives the answer, with the stamp of that row, and closed-through is not below it"
           [(:stamp row) :accepted true]
           [(:stamp (:answer after)) (:word after) (<= (:stamp row) (closed-through uuid))])
    (check "and the read of locks gives the lock, with the bytes that were in the table"
           (vec (:bytes lock-row)) (vec (:bytes (get (locks-of act-id 0 255) 0))))
    (shown "an answer is not readable before its batch is closed: the row is in the table with stamp" (:stamp row)
           "| closed-through on its task" (:closed-through tries) "| answer-by-act-id gives" (pr-str read)
           "| locks-of-request gives" (pr-str locks-read) "although the lock's row is in the table"
           "| and is readable after:" (told after) "| closed-through" (closed-through uuid) "| caught at try" (:tries tries))))

(deftest refusals-and-repeats-in-the-base
  (let [landed (:base-landed @kept)
        accepted-thing (:thing landed)
        e (:e (first (:facts accepted-thing)))
        task-of-entity (:base-task-of-entity @kept)
        [e1 e2] (:base-entities @kept)
        [unsealed stale] (decide-in-one-batch!
                          [(system-act [{:e :doc-r1 :k :text :v "a plain value"}])
                           (system-act [(assoc (sealed :base e :title "stale") :replaces [(new-act-id :base) 0])])])
        answer-before (answer-of (:act-id accepted-thing))
        row-before (base-fact e :title)
        last-stamp-before (gates-last-stamp)
        _ (append-to-base! accepted-thing)
        _ (append-to-base! (:thing unsealed))
        _ (wait-until-processed!)
        answer-after (answer-of (:act-id accepted-thing))
        refused-again (answer-of (:act-id (:thing unsealed)))
        row-after (base-fact e :title)
        other-under-used (decide-in-base! (assoc (system-act [{:e :doc-r2 :k :text}]) :act-id (:act-id accepted-thing)))
        twice-a (system-act [{:e e1 :k :twice}])
        twice-b (assoc (system-act [{:e e2 :k :twice}]) :act-id (:act-id twice-a))
        [two-a two-b] (decide-in-one-batch! [twice-a twice-b])
        written-a (base-fact e1 :twice)
        written-b (base-fact e2 :twice)]
    (check "an unsealed value is refused through the microbatch gate, and the refusal is recorded with its reason, a stamp and the digest"
           [:refused :not-sealed true true]
           [(:verdict (:answer unsealed)) (:reason (:answer unsealed)) (int? (:stamp (:answer unsealed))) (:mine? unsealed)])
    (check "a refusal writes nothing at the place" nil (base-fact :doc-r1 :text))
    (check "a stale replaces is refused through the microbatch gate, and the place is as it was"
           [:replaced-fact-not-current (:stamp (:answer landed))] [(:word stale) (:stamp row-before)])
    (check "a repeat is answered from the record: the same answer, with the stamp of the first decision"
           answer-before answer-after)
    (check "not decided twice: the row of its place is the row of the first decision"
           [(:stamp row-before) (:by-act row-before) (vec (:value row-before))]
           [(:stamp row-after) (:by-act row-after) (vec (:value row-after))])
    (check "a repeat of a refused act reads the recorded refusal" (:answer unsealed) refused-again)
    (check "a second, different act under a used act-id is told so: the answer it reads is not of its own digest"
           [answer-before false] [(:answer other-under-used) (:mine? other-under-used)])
    (check "and nothing of the second act is written" nil (base-fact :doc-r2 :text))
    (check "the two things of one act-id arrive on two tasks" true (not= (task-of-entity e1) (task-of-entity e2)))
    (check "one act-id under two different things in one batch: one answer, which is of one of the two"
           [true #{true false}] [(= (:answer two-a) (:answer two-b)) (set [(:mine? two-a) (:mine? two-b)])])
    (check "the thing that has the answer wrote its fact, and nothing of the other is written"
           [(:mine? two-a) (:mine? two-b)] [(some? written-a) (some? written-b)])
    (shown "a refusal through the microbatch gate, recorded with its reason:" (pr-str (:answer unsealed)))
    (shown "a stale replaces refused through the microbatch gate:" (told stale))
    (shown "a repeat (same act-id sent twice to the microbatch depot) answered from the record, not decided twice: stamp before"
           (:stamp answer-before) "after" (:stamp answer-after) "| the stamp of its place before" (:stamp row-before) "after" (:stamp row-after)
           "| the gate's last stamp moved from" last-stamp-before "to" (gates-last-stamp) "and gave the repeat no stamp")
    (shown "a second, different act under a used act-id of the base told so: the digest of the answer"
           (subs (:digest (:answer other-under-used)) 0 16) "is not the digest of what was sent"
           (subs (digest/hex-of (:thing other-under-used)) 0 16))
    (shown "one act-id under two things in one batch, on tasks" (task-of-entity e1) "and" (task-of-entity e2)
           ": one answer; written by the first thing?" (some? written-a) "by the second?" (some? written-b))))

(deftest one-batch-one-order
  (let [task-of-entity (:base-task-of-entity @kept)
        [ea eb] (:base-entities @kept)
        first-value (decide-in-base! (system-act [(sealed :base ea :order "the fact before")]))
        f0 [(:act-id (:thing first-value)) 0]
        [a b] (decide-in-one-batch!
               [(system-act [(assoc (sealed :base ea :order "from the first act") :replaces f0)])
                (system-act [{:e eb :k :order-other} (assoc (sealed :base ea :order "from the second act") :replaces f0)])])
        [winner loser] (if (= :accepted (:word a)) [a b] [b a])
        place (base-fact ea :order)
        other-fact (base-fact eb :order-other)
        [first-mark second-mark] (decide-in-one-batch! [(system-act [{:e ea :k :mark}])
                                                        (system-act [{:e eb :k :mark-other} {:e ea :k :mark}])])
        mark (base-fact ea :mark)
        [give-carol carol-acts dave-acts give-dave]
        (decide-in-one-batch! [(system-act [{:e :carol :k :permission :v carol-in-base}])
                               (thing :base :carol carol-in-base [{:e :carol :k :note}])
                               (thing :base :dave dave-in-base [{:e :dave :k :note}])
                               (system-act [{:e :dave :k :permission :v dave-in-base}])])
        dave-later (decide-in-base! (thing :base :dave dave-in-base [{:e :dave :k :note}]))
        one-lock (take-lock! :base)
        under-one-lock (decide-in-one-batch! [(system-act [(seal-with one-lock :base ea :under-one-lock "a")])
                                              (system-act [(seal-with one-lock :base eb :under-one-lock "b")])])]
    (check "the two entities are on two tasks" true (not= (task-of-entity ea) (task-of-entity eb)))
    (check "two acts of one batch that replace the same fact, arriving on two tasks: one is accepted, the other refused as stale"
           [:accepted :replaced-fact-not-current] [(:word winner) (:word loser)])
    (check "the place holds the winner's fact, with the winner's stamp"
           [(nth (:act-id (:thing winner)) 2) (:stamp (:answer winner))] [(:by-act place) (:stamp place)])
    (check "the winner holds the earlier stamp: it has the earlier turn" true (< (:stamp (:answer winner)) (:stamp (:answer loser))))
    (check "the loser is refused whole: its fact on the other task is there only when it is the winner's"
           (= b winner) (some? other-fact))
    (check "two acts of one batch that write one place and name nothing are both accepted, and the place holds the later stamp"
           [:accepted :accepted (max (:stamp (:answer first-mark)) (:stamp (:answer second-mark)))]
           [(:word first-mark) (:word second-mark) (:stamp mark)])
    (check "in one batch, on one entity: a give and then an act under it are both accepted; an act and then the give, the act is refused"
           [:accepted :accepted :permission-does-not-stand :accepted]
           [(:word give-carol) (:word carol-acts) (:word dave-acts) (:word give-dave)])
    (check "the same act under the permission, in a later batch, is accepted" :accepted (:word dave-later))
    (check "two acts of one batch under one lock, on two tasks: one seals with it, the other is refused"
           #{:accepted :lock-already-seals-a-value} (set (map :word under-one-lock)))
    (shown "two acts of one batch that replace the same fact, with one winner: on tasks" (task-of-entity ea) "and" (task-of-entity eb)
           "|" (told winner) "|" (told loser) "| the place is written by" (short-id [nil nil (:by-act place)]))
    (shown "two acts of one batch that write one place and name nothing:" (told first-mark) "|" (told second-mark) "| the place holds stamp" (:stamp mark))
    (shown "a give and an act under it in one batch, in both orders:" (:word give-carol) (:word carol-acts) "|" (:word dave-acts) (:word give-dave)
           "| two acts under one lock:" (pr-str (mapv :word under-one-lock)))))

(deftest a-person-acts-in-the-base
  (let [give (decide-in-base! (system-act [{:e :alice :k :permission :v alice-in-base}]))
        permission-rows (rows-by-task :mb-facts-ps (record/place-key :base :alice :permission))
        asks (ask-for-base-locks! :alice alice-in-base 2)
        writes (decide-in-base! (thing :base :alice alice-in-base [(sealed :base :alices-doc :text "alice's text in the base")]))
        [give-again end-it class-again class-other setting-elsewhere give-bob]
        (decide-in-one-batch!
         [(system-act [{:e :alice :k :permission :v alice-in-base}])
          (system-act [{:e :alice :k :permission}])
          (system-act [{:e :base :k :class :v :microbatch}])
          (system-act [{:e :base :k :class :v :streaming}])
          (system-act [{:e :team :k :class :v :microbatch}])
          (thing :base :alice alice-in-base [{:e :bob :k :permission :v bob-in-base}])])
        bob-acts (decide-in-base! (thing :base :bob bob-in-base [{:e :bobs-doc :k :note}]))
        permission-rows-after (rows-by-task :mb-facts-ps (record/place-key :base :alice :permission))
        clock (System/currentTimeMillis)
        far (stamp/pack (+ clock 60000) 0)
        fact-id [(:act-id (:thing give)) 0]
        [not-given not-holder beside far-ahead far-ahead-no-standing]
        (decide-in-one-batch!
         [(thing :base :erin [:erin :base :base system-own] [{:e :erins-doc :k :note}])
          (thing :base :mallory alice-in-base [{:e :mallorys-doc :k :note}])
          (thing :base :alice alice-in-base [{:e :frank :k :permission :v [:frank :base :base system-own]}])
          (system-act [{:e :far-doc :k :note}] {:based-on {fact-id far}})
          (thing :base :mallory alice-in-base [{:e :mallorys-doc :k :note}] {:based-on {fact-id far}})])
        next-one (decide-in-base! (system-act [{:e :far-doc :k :note}]))]
    (check "the system gives a person a permission in the base, beneath its own: accepted, and it sits on its holder as a fact"
           [:accepted alice-in-base] [(:word give) (record/plain-value-of (first (vals permission-rows)))])
    (check "the permission's row is on every task" tasks (count permission-rows))
    (check "a person acts in the base under the permission that the system gave: a request for locks, and a sealed value"
           [:accepted 2 :accepted true] [(:word asks) (count (:locks asks)) (:word writes) (:mine? writes)])
    (check "an act under a permission that does not stand is refused" :permission-does-not-stand (:word not-given))
    (check "an act under a permission by a who that is not its holder is refused" :who-not-the-permissions-holder (:word not-holder))
    (check "a permission given beside the giver's own, not beneath it, is refused, and gives nothing"
           [:permission-fact-not-beneath-the-acts-own {}]
           [(:word beside) (rows-by-task :mb-facts-ps (record/place-key :base :frank :permission))])
    (check "in the base a store key takes one write for a place: a permission given again, the end of one, the class again, another class"
           [:store-key-takes-one-write :ending-a-permission-not-decided :store-key-takes-one-write :changing-a-class-not-decided]
           [(:word give-again) (:word end-it) (:word class-again) (:word class-other)])
    (check "a setting key on an entity that is not the layer is refused in the base" :setting-not-on-the-layer (:word setting-elsewhere))
    (check "after those refusals the permission's rows and the class are as they were"
           [(into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows) :microbatch]
           [(into {} (map (fn [[task row]] [task (:stamp row)])) permission-rows-after) (class-of-layer :base)])
    (check "a person gives a permission beneath her own, in the same batch as those refusals, and an act under it is accepted"
           [:accepted :accepted] [(:word give-bob) (:word bob-acts)])
    (check "an act that carries a stamp more than 10 seconds ahead of the gate's clock is refused"
           :carried-stamp-too-far-ahead (:word far-ahead))
    (check "the same under a permission that does not stand is refused with the word of the 10-second rule, as in the streaming gate"
           :carried-stamp-too-far-ahead (:word far-ahead-no-standing))
    (check "the carried stamp puts the stamps of its batch 10 seconds ahead of the clock at most, and the next stamp is later still"
           [true true]
           [(< (stamp/time-of (:stamp (:answer far-ahead))) (+ (System/currentTimeMillis) 11000))
            (< (:stamp (:answer far-ahead)) (:stamp (:answer next-one)))])
    (shown "a person acting in the base under a permission that the system gave:" (told give) "| the permission's row is on"
           (count permission-rows) "tasks | her request for locks:" (told asks) "| her act:" (told writes))
    (shown "in the base, an act under a permission that does not stand:" (:word not-given) "| by a who that is not the holder:" (:word not-holder)
           "| a permission given beside the giver's own:" (:word beside))
    (shown "a store key takes one write in the base:" (:word give-again) "| ending a permission:" (:word end-it)
           "| the class again:" (:word class-again) (:word class-other) "| a setting key on another entity:" (:word setting-elsewhere)
           "| a permission beneath a person's, and an act under it:" (:word give-bob) (:word bob-acts))
    (shown "a carried stamp too far ahead, in the base:" (told far-ahead) "| the same with no standing:" (:word far-ahead-no-standing))))

(deftest a-long-batch
  (let [n 400
        entity (fn [i] (keyword (str "long-" (mod i 20))))
        k (fn [i] (keyword (str "k" i)))
        results (decide-in-one-batch! (mapv (fn [i] (system-act [{:e (entity i) :k (k i)}])) (range n)))
        stamps (mapv #(:stamp (:answer %)) results)
        sample (range 0 n 37)]
    (check "a batch of 400 acts on 20 entities: each is accepted, each with a stamp of its own"
           [#{:accepted} n] [(set (map :word results)) (count (set stamps))])
    (check "the acts on one entity keep the depot's order: their stamps go up in the order sent"
           true (every? (fn [[_ sent]] (apply < (map second sent))) (group-by #(entity (first %)) (map-indexed vector stamps))))
    (check "the fact of each act of a sample is on its place, with the stamp of its answer"
           (mapv #(nth stamps %) sample) (mapv #(:stamp (base-fact (entity %) (k %))) sample))
    (shown "a batch of" n "acts on 20 entities: each accepted, with" (count (set stamps))
           "stamps; on each entity the stamps go up in the order sent")))

(deftest an-act-on-several-tasks
  (let [task-of-entity (:base-task-of-entity @kept)
        entities (:base-entities @kept)
        request-thing (system-act [])
        request (:act-id request-thing)
        request-uuid (nth request 2)
        asked (decide-in-base! (assoc request-thing :facts [{:e request-uuid :k :lock-request :v 3}]))
        door-locks (locks-of request 0 255)
        locks (mapv (fn [i] {:lock-id [request i] :lock (get door-locks i)}) (range 3))
        places (mapv (fn [e] [e :body]) entities)
        facts (fn [] (mapv (fn [lock [e k]] (seal-with lock :base e k (str "the body of " (name e)))) locks places))
        place-rows (fn [] (mapv (fn [[e k]] (rows-by-task :mb-facts-ps (record/place-key :base e k))) places))
        lock-rows (fn [] (mapv (fn [lock] (rows-by-task :mb-locks-ps (base-lock-key (:lock-id lock)))) locks))
        home-task (task-of-row :mb-answers-ps (record/answer-key :base request-uuid))
        refused (decide-in-base! (system-act (update (facts) 2 assoc :replaces [(new-act-id :base) 0])))
        places-after-refusal (place-rows)
        locks-after-refusal (lock-rows)
        earlier-stamps (keep (fn [[e task]] (when (contains? (set (map task-of-entity entities)) task) (:stamp (base-fact e :seen))))
                             task-of-entity)
        last-stamp-before (gates-last-stamp)
        sent-facts (facts)
        accepted (decide-in-base! (system-act sent-facts))
        act-uuid (nth (:act-id (:thing accepted)) 2)
        stamp (:stamp (:answer accepted))
        places-after (place-rows)
        locks-after (lock-rows)
        beside (mapv (fn [rows [e _]] (get rows (task-of-entity e))) locks-after places)
        at-home (mapv (fn [rows] (get rows home-task)) locks-after)]
    (swap! kept assoc :base-values {:places places :locks locks :sent sent-facts :act-uuid act-uuid})
    (check "the three entities of the act are on more than one task" true (< 1 (count (set (map task-of-entity entities)))))
    (check "the request for its three locks is accepted" [:accepted 3] [(:word asked) (count door-locks)])
    (check "an act that touches several tasks, with its third fact stale, is refused whole" :replaced-fact-not-current (:word refused))
    (check "no row of it is on any task: none of its three places holds a fact" [{} {} {}] places-after-refusal)
    (check "its three locks are still free: each is one row, at its home, with its bytes, and names no fact"
           (mapv (fn [lock] [[home-task] (vec (:bytes (:lock lock))) false]) locks)
           (mapv (fn [rows] [(vec (keys rows)) (vec (:bytes (first (vals rows)))) (record/seals-a-fact? (first (vals rows)))])
                 locks-after-refusal))
    (check "the same act with each fact good is accepted whole" [:accepted true] [(:word accepted) (:mine? accepted)])
    (check "each fact's row is on the task of its entity and no other, written by the act, with the stamp of its answer"
           (mapv (fn [[e _] i] [[(task-of-entity e)] act-uuid i stamp]) places (range))
           (mapv (fn [rows] (let [row (first (vals rows))] [(vec (keys rows)) (:by-act row) (:by-index row) (:stamp row)])) places-after))
    (check "each lock's row with its bytes is beside its value, and names the fact"
           (mapv (fn [lock i] [(vec (:bytes (:lock lock))) act-uuid i]) locks (range))
           (mapv (fn [row] [(vec (:bytes row)) (:seals-act row) (:seals-index row)]) beside))
    (check "each lock's row at its home names the fact, and holds no bytes unless the home is the value's task"
           (mapv (fn [[e _] i] [act-uuid i (= home-task (task-of-entity e))]) places (range))
           (mapv (fn [row] [(:seals-act row) (:seals-index row) (contains? row :bytes)]) at-home))
    (check "one copy of each lock's bytes over every task, and at least one lock's home is another task than its value's"
           [[1 1 1] true]
           [(mapv (fn [rows] (count (filter #(contains? % :bytes) (vals rows)))) locks-after)
            (boolean (some (fn [rows] (< 1 (count rows))) locks-after))])
    (check "each sealed value is opened by the bytes beside it"
           (mapv (fn [[e _]] (vec (plain-bytes (str "the body of " (name e))))) places)
           (mapv (fn [row [e k] rows] (vec (seal/open (record/lock-of row) [:base e k] (:value (first (vals rows))))))
                 beside places places-after))
    (check "the one stamp of builder's rule 18: later than the stamp of each row written before on those tasks, and than the refusal's"
           [true true] [(every? #(< % stamp) earlier-stamps) (< (:stamp (:answer refused)) stamp)])
    (check "the gate has one last stamp: it was below the act's stamp before the batch, and is not below it after"
           [true true] [(< last-stamp-before stamp) (<= stamp (gates-last-stamp))])
    (shown "an act that touches several tasks accepted whole:" (told accepted) "| its facts on tasks" (pr-str (mapv #(vec (keys %)) places-after))
           "| each lock's bytes beside its value, rows under each lock's key by task with bytes?"
           (pr-str (mapv (fn [rows] (into (sorted-map) (map (fn [[task row]] [task (contains? row :bytes)])) rows)) locks-after))
           "| the locks' home is task" home-task)
    (shown "one refused whole with no row of it on any task:" (told refused) "| rows at its three places:" (pr-str places-after-refusal)
           "| its three locks still free at their home")
    (shown "the one stamp of builder's rule 18: the act's answer and its three facts, on" (count (set (map task-of-entity entities)))
           "tasks, hold the stamp" stamp "| later than each of" (count earlier-stamps) "rows written before on those tasks"
           "| the gate's last stamp before" last-stamp-before "after" (gates-last-stamp))))

(deftest each-gate-tells-the-other-gates-layers
  (let [base-on-streaming (decide! (thing :base :system system-own [{:e :wrong-depot :k :note}]
                                          {:act-id [:base :streaming (UUID/randomUUID)] :class :streaming}))
        [alice-on-microbatch nobody-on-microbatch]
        (decide-in-one-batch!
         [(thing :alice :alice alice-own [{:e :wrong-depot :k :note}] {:act-id [:alice :microbatch (UUID/randomUUID)] :class :microbatch})
          (thing :nobody :nobody [:nobody :nobody :nobody] [{:e :wrong-depot :k :note}]
                 {:act-id [:nobody :microbatch (UUID/randomUUID)] :class :microbatch})])
        strays-before (base-strays)
        both (alice [{:e :both-depots :k :text}])
        on-streaming (decide! both)
        malformed {:act-id [:base :microbatch] :layer :base :marker (UUID/randomUUID)}
        acks (mapv append-to-base! [both 5 5 malformed])
        _ (wait-until-processed!)
        strays-after (base-strays)]
    (check "an act for the base, a microbatch layer, sent to the streaming depot is refused as an act into a layer of the other gate, under its streaming act-id"
           [:acked :layer-belongs-to-the-other-gate true] [(:ack base-on-streaming) (:word base-on-streaming) (:mine? base-on-streaming)])
    (check "an act for a streaming layer sent to the microbatch depot is refused the same way, and read back with the uuid of its act-id"
           [:layer-belongs-to-the-other-gate true] [(:word alice-on-microbatch) (:mine? alice-on-microbatch)])
    (check "an act for a layer that no gate made, sent to the microbatch depot, is refused as an act into a layer that is not made"
           :layer-not-made (:word nobody-on-microbatch))
    (check "neither wrote a fact, in either record"
           [nil {}] [(fact-row :base :wrong-depot :note) (rows-by-task :mb-facts-ps (record/place-key :alice :wrong-depot :note))])
    (check "each thing was appended" (vec (repeat 4 :appended)) acks)
    (check "one act-id sent on both depots has one answer, the streaming gate's: the microbatch gate keeps the thing as a stray"
           [:accepted (:answer on-streaming)] [(:word on-streaming) (answer-of (:act-id both))])
    (check "a stray on the microbatch depot is kept under its word, once however often it comes, and gets no answer"
           {:no-act-id 1 :act-id-malformed 1 :id-too-long 0 :act-id-of-the-other-gate 1 :act-id-of-another-layer 0}
           (merge-with - strays-after strays-before))
    (shown "rule 7 in both directions: an act for a microbatch layer sent to the streaming depot:" (told base-on-streaming)
           "| an act for a streaming layer sent to the microbatch depot:" (told alice-on-microbatch)
           "| for a layer that is not made:" (:word nobody-on-microbatch))
    (shown "one act-id on both depots has one answer:" (told on-streaming) "| strays of the microbatch gate, by word:"
           (pr-str (merge-with - strays-after strays-before)) "| the same stray sent twice counts once")))

(defn- base-snapshot
  "What the proof reads of the base before and after the module is started
  again: the answer of the act that landed, the class, the gate's last
  stamp, the count of strays, a permission's row on each task, and the bytes
  of the locks of one request."
  [request]
  {:answer (answer-of (:act-id (:thing (:base-landed @kept))))
   :class (class-of-layer :base)
   :last-stamp (gates-last-stamp)
   :strays (base-strays)
   :permission (into (sorted-map) (map (fn [[task row]] [task (:stamp row)])) (rows-by-task :mb-facts-ps (record/place-key :base :alice :permission)))
   :locks (into (sorted-map) (map (fn [[i lock]] [i (some-> (:bytes lock) vec)])) (locks-of request 0 255))})

(deftest the-path-of-a-lock-in-the-base
  (let [used-lock (:base-landed-lock @kept)
        second-value (decide-in-base! (system-act [(seal-with used-lock :base :lock-doc :second "a second value")]))
        request-thing (system-act [])
        request (:act-id request-thing)
        request-uuid (nth request 2)
        asked (decide-in-base! (assoc request-thing :facts [{:e request-uuid :k :lock-request :v 2}]))
        door-locks (locks-of request 0 255)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) door-locks)
        _ (append-to-base! (:thing asked))
        _ (wait-until-processed!)
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of request 0 255))
        on-its-home (decide-in-base! (system-act [(seal-with {:lock-id [request 0] :lock (get door-locks 0)}
                                                             :base request-uuid :note "a value on its lock's home")]))
        rows (rows-by-task :mb-locks-ps (record/lock-key :base request-uuid 0))
        the-lock (first (vals rows))
        the-value (base-fact request-uuid :note)
        read-after (locks-of request 0 255)
        new-request (system-act [])
        forged-lock {:lock-id [(:act-id new-request) 0] :lock (seal/new-lock)}
        [requested forged] (decide-in-one-batch!
                            [(assoc new-request :facts [{:e (nth (:act-id new-request) 2) :k :lock-request :v 1}])
                             (system-act [(seal-with forged-lock :base :lock-doc :forged "sealed with bytes that the store did not make")])])
        kept-request (ask-for-base-locks! :system system-own 2)]
    (swap! kept assoc :base-kept-request (:request kept-request) :base-before (base-snapshot (:request kept-request)))
    (check "a second value under a lock that already seals one is refused, although the lock's row at its home holds no bytes"
           :lock-already-seals-a-value (:word second-value))
    (check "a request sent twice makes no lock again: each lock has the bytes that the first run made"
           [:accepted 2 bytes-before] [(:word asked) (count bytes-after) bytes-after])
    (check "a value on the entity that is its lock's request's uuid, so that the lock's home is the value's task, is accepted"
           :accepted (:word on-its-home))
    (check "one copy of a lock's bytes also then: the one row under the lock's key, on one task, holds the bytes and the fact"
           [1 (get bytes-before 0) (nth (:act-id (:thing on-its-home)) 2) 0]
           [(count rows) (vec (:bytes the-lock)) (:seals-act the-lock) (:seals-index the-lock)])
    (check "and its bytes open the value"
           (vec (plain-bytes "a value on its lock's home"))
           (vec (seal/open (record/lock-of the-lock) [:base request-uuid :note] (:value the-value))))
    (check "the read of locks gives that lock with no bytes all the same, and the lock that is not used with its bytes"
           [{:bytes nil :way 1} (get bytes-before 1)] [(get read-after 0) (vec (:bytes (get read-after 1)))])
    (check "a request for locks and a use of its lock in one batch: the request is accepted, and the use is refused"
           [:accepted true]
           [(:word requested) (contains? #{:lock-does-not-open-the-value :lock-not-made-for-this-layer} (:word forged))])
    (shown "the path of a lock in the base: asked for and read, used, its bytes beside its value and no longer at its home, and then a second value under it refused:"
           (told second-value))
    (shown "one copy of a lock's bytes when the lock's home is its value's task: rows under the lock's key" (count rows)
           "| holds bytes" (contains? the-lock :bytes) "| names the fact" (some? (:seals-act the-lock))
           "| locks-of-request gives it as" (pr-str (get read-after 0)))
    (shown "a request sent twice to the microbatch depot gives the same" (count bytes-after) "locks, byte for byte | a use of a lock in the batch that asks for it:" (:word forged))))

(deftest after-the-module-is-started-again
  (let [landed (:landed @kept)
        accepted (:act-id (:thing landed))
        request (ask-for-locks! :alice :alice alice-own 2)
        bytes-before (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (:locks request))
        stamp-before (last-stamp :alice)
        strays-before (strays :act-id-malformed)
        _ (rtest/update-module! (:ipc *hand*) module/Store)
        answer-after (answer-of accepted)
        repeat-ack (send! (:thing landed))
        stamp-after-repeat (last-stamp :alice)
        bytes-after (into {} (map (fn [[i lock]] [i (vec (:bytes lock))])) (locks-of (:request request) 0 255))
        with-old-lock (decide! (alice [(sealed :alice :note-40 :text "sealed under a lock from before")]))
        by-bob (decide! (thing :alice :bob bobs [{:e :bobs-note-4 :k :text}]))]
    (check "the answer that was read before is the answer after" (:answer landed) answer-after)
    (check "a repeat after it is answered from the record: the task's last stamp is the one from before"
           [:acked stamp-before] [repeat-ack stamp-after-repeat])
    (check "the class, the strays and the locks are as they were: each lock has the same bytes"
           [:streaming strays-before bytes-before] [(class-of-layer :alice) (strays :act-id-malformed) bytes-after])
    (check "a lock that was asked for before seals a value after" :accepted (:word with-old-lock))
    (check "the first stamp after is later than the last stamp before" true (> (:stamp (:answer with-old-lock)) stamp-before))
    (check "a permission that was given before stands after" :accepted (:word by-bob))
    (shown "after the module is started again in the cluster, the record is as it was: the same answer, the same locks;"
           "the last stamp before" stamp-before "the first stamp after" (:stamp (:answer with-old-lock)))))

(deftest the-stamps-and-the-depot
  (let [records (vec (stored))
        partition-of (into {} (keep (fn [[p r]] (when (and (map? r) (contains? r :layer)) [(:layer r) p]))) records)
        made [:alice :team :carrier]
        by-partition (group-by #(get partition-of (:layer %)) @first-answers)
        lock-texts (into #{} (comp (filter #(and (map? %) (vector? (:facts %))))
                                   (map :act-id)
                                   (mapcat (fn [act-id] (vals (locks-of act-id 0 255))))
                                   (map #(text/hex (:bytes %))))
                         @things-sent)
        depot-text (apply str (map (fn [[_ r]] (text/text r)) records))]
    (check "the stamps of the answers increase strictly on each task, in the order in which the acts were sent"
           true (every? (fn [[_ answers]] (apply < (map :stamp answers))) by-partition))
    (check "each stamp is below 2^62 and holds a clock time of today"
           true (every? #(and (< (:stamp %) act/max-carried-stamp)
                              (< (- (System/currentTimeMillis) 3600000) (stamp/time-of (:stamp %)) (+ (System/currentTimeMillis) 60000)))
                        @first-answers))
    (check "the depot holds each thing that was sent: accepted, refused, repeated and stray" (count @things-sent) (count records))
    (check "the layers of the proof are on more than one task" true (< 1 (count (set (vals partition-of)))))
    (check "no lock's bytes are on the depot: the hex of each lock that the door read is in no stored record"
           [true false] [(< 30 (count lock-texts)) (boolean (some #(str/includes? depot-text %) lock-texts))])
    (shown "the stamps of" (count @first-answers) "answers increase strictly on each task; answers by partition:"
           (pr-str (into (sorted-map) (map (fn [[p answers]] [p (count answers)])) by-partition)))
    (shown "layers by depot partition:" (pr-str (select-keys partition-of made)) "| the depot holds all" (count records)
           "things sent | none of" (count lock-texts) "locks' bytes is on the depot")))

(deftest the-base-after-the-module-is-started-again
  (let [before (:base-before @kept)
        request (:base-kept-request @kept)
        landed (:base-landed @kept)
        after (base-snapshot request)
        _ (append-to-base! (:thing landed))
        _ (wait-until-processed!)
        answer-after-repeat (answer-of (:act-id (:thing landed)))
        with-old-lock (decide-in-base! (system-act [(seal-with {:lock-id [request 0] :lock {:bytes (byte-array (get (:locks before) 0)) :way 1}}
                                                               :base :after-doc :text "sealed under a lock from before")]))
        by-alice (decide-in-base! (thing :base :alice alice-in-base [{:e :alices-doc :k :after}]))
        {:keys [places locks sent]} (:base-values @kept)
        opened (mapv (fn [[e k] lock fact]
                       (let [value (:value (base-fact e k))
                             beside (base-row :mb-locks-ps e (base-lock-key (:lock-id lock)))]
                         [(vec value) (some? (seal/open (record/lock-of beside) [:base e k] value))]))
                     places locks sent)]
    (check "the answer, the class of the base, the gate's last stamp, the strays, a permission's rows and the locks are as they were"
           before after)
    (check "a repeat after it is answered from the record" (:answer before) answer-after-repeat)
    (check "a lock of the base that was asked for before seals a value after" :accepted (:word with-old-lock))
    (check "the first stamp after is later than the gate's last stamp before" true (< (:last-stamp before) (:stamp (:answer with-old-lock))))
    (check "a permission that the system gave before stands after" :accepted (:word by-alice))
    (check "each sealed value of the act on several tasks is as it was sent, and the bytes beside it still open it"
           (mapv (fn [fact] [(vec (:sealed fact)) true]) sent) opened)
    (shown "after the module is started again in the cluster, the base's record is as it was: the same answer, class" (:class after)
           "| the same locks | the gate's last stamp before" (:last-stamp before) "the first stamp after" (:stamp (:answer with-old-lock)))))

(deftest the-stamps-and-the-depot-of-the-base
  (let [records (vec (stored-in-base))
        groups @base-answers
        answers (vec (apply concat groups))
        depot-text (apply str (map (fn [[_ r]] (text/text r)) records))
        lock-texts @lock-bytes-read
        not-closed (remove (fn [{:keys [act-id stamp]}] (<= stamp (or (closed-through (nth act-id 2)) -1))) answers)]
    (check "the stamps of the microbatch gate's answers go up in the order decided: each batch's stamps are later than those of the batch before"
           true (every? (fn [[earlier later]] (< (reduce max (map :stamp earlier)) (reduce min (map :stamp later)))) (partition 2 1 groups)))
    (check "no two answers of the gate share a stamp" true (apply distinct? (map :stamp answers)))
    (check "each stamp is below 2^62 and holds a clock time of today"
           true (every? #(and (< (:stamp %) act/max-carried-stamp)
                              (< (- (System/currentTimeMillis) 3600000) (stamp/time-of (:stamp %)) (+ (System/currentTimeMillis) 60000)))
                        answers))
    (check "each answer that a read gave is of a closed batch: its stamp is not above closed-through on its task" [] (vec not-closed))
    (check "the microbatch depot holds each thing that was sent to it: accepted, refused, repeated and stray"
           (count @base-things-sent) (count records))
    (check "the things of the base are on more than one partition of the depot" true (< 1 (count (set (map first records)))))
    (check "no lock's bytes are on the microbatch depot: the hex of each lock that the door read is in no stored record"
           [true false] [(< 30 (count lock-texts)) (boolean (some #(str/includes? depot-text %) lock-texts))])
    (shown "the stamps of" (count answers) "answers of the microbatch gate, in" (count groups) "batches, go up in the order decided;"
           "each answer read is of a closed batch")
    (shown "the microbatch depot holds all" (count records) "things sent, on" (count (set (map first records)))
           "partitions | none of" (count lock-texts) "locks' bytes that the door read is on it")))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them. Later checks
  build on what earlier ones made. The checks of the streaming gate come
  first, as they were; the base is made after them."
  []
  (what-rama-carries)
  (layers-are-made)
  (an-act-lands)
  (refusals-and-repeats)
  (settings-and-permissions)
  (the-path-of-a-lock)
  (strays-and-limits)
  (the-reads-take-anything)
  (the-base-is-made)
  (an-act-lands-in-the-base)
  (an-answer-is-readable-when-its-batch-is-closed)
  (refusals-and-repeats-in-the-base)
  (one-batch-one-order)
  (a-person-acts-in-the-base)
  (a-long-batch)
  (an-act-on-several-tasks)
  (each-gate-tells-the-other-gates-layers)
  (the-path-of-a-lock-in-the-base)
  (after-the-module-is-started-again)
  (the-base-after-the-module-is-started-again)
  (the-stamps-and-the-depot)
  (the-stamps-and-the-depot-of-the-base))

(defn -main
  "Starts the cluster, launches the module, runs the proof, closes the
  cluster and exits: status 0 when each check holds. It takes no argument."
  [& _]
  (let [result (with-open [ipc (rtest/create-ipc)]
                 (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
                 (binding [*hand* (open-hand ipc)]
                   (run-tests 'softland.store.module-proof)))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
