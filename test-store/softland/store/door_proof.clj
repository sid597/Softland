(ns softland.store.door-proof
  "The proof of the door on Rama's in-process cluster: acts go into both
  depots through `softland.store.door`, and their answers come back. One
  command, from the repository root:

    clojure -M:store-module -m softland.store.door-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The proof is the door's one maker. Each act and each answer of this proof
  goes through the door's five calls. The proof makes no act-id and appends
  nothing itself, with one exception: for the check that the door sends what
  the hand sends, it builds one act with the hand's steps, written out here
  a second time, and appends that thing itself. The hand writes the time on
  that thing as the door does: a reading of the clock, under the name that
  `softland.store.door.declaration/time-part` holds.

  What it reads round the door. The door gives a maker no thing and no row.
  So, as the module's proof does, this proof holds handles of its own: it
  reads each record of the two depots' logs, to see what the door appended,
  with the time that the door wrote on it; the answer under an act-id, by
  the query `answer-by-act-id`; the row of a place, by a select of the
  gates' tables of facts, to see what a gate kept of a value and that a
  refused act left no row; and the strays of both gates. A place of a
  streaming layer has its row on the layer's task. A place of the base has
  the same row on every task, and the proof reads each task.

  The base. An act of the base goes to the microbatch depot, whose ack says
  that the depot holds the thing and nothing of the decision. The proof
  holds that with Rama's test tool, which pauses the microbatch gate: an
  act that the gate will accept and one that it will refuse come back with
  the same ack and read \"nothing yet\"; when the gate goes on, each reads
  its answer, and each later read gives that answer again, also after the
  module is started again. It holds the order of two sends: a give and then
  the first act under it, and an act and then an act that replaces its
  fact, each pair sent with no read between the two, are all accepted. It
  sends 8 acts while the gate is paused: a give, the first act under it,
  and six that each replace the fact of the act before, so that each needs
  the rows of the one before it. Rama's test docs say that the next batch
  then holds them all; the proof cannot see where a batch is cut, and
  nothing of a batch is in an answer. An answer of the base shows when its batch is
  committed on the base's task, and the copy of a fact on another task can
  show a moment later. So before it reads a row of the base round the door,
  the proof waits, with Rama's test tool, until the microbatch gate has
  processed each record of its depot. It prints how long an act of the base
  took from the return of `send-act` to its answer, as an order of size,
  and holds that time to nothing but the bound of a wait.

  The depot of a layer, and the other depot word. The door reads a layer's
  depot when it makes an act-id. The proof notes what that read was asked
  and what it gave, for the base, for a streaming layer and for a layer
  that is not made. An act under an act-id whose depot word is not the
  layer's goes to the depot that the act-id names. The proof sends one to
  each depot, for a layer that is made and for one that is not, and holds
  the word of the refusal that comes back.

  The time on the act. The door adds three parts to a maker's act: the
  act-id, the depot, and the time of the clock of the server that runs the
  door. The first two are known before the send. The time is not, so the
  proof holds it in two steps: a record of the log without its time is the
  maker's act with the act-id and the depot; and the time is a fixed-width
  integer between two readings of the clock that the proof makes, one just
  before the call of the door and one just after. The proof and its doors
  run in one process, so the proof reads the clock that the door reads. It
  also holds the time in the stamp of an accepted act against the time on
  the act, in a streaming layer and in the base, with the stamp's own
  functions (`softland.store.gate.stamp`). Where both are equal, the check
  says why: the act is the first of its layer, or it was sent after each
  earlier act of its layer. Of acts that 16 threads send into one layer at
  once it holds only what holds in every order: no stamp is of a time
  before the time on its act.

  Where the proof stands in for a call of Rama's client. Clojure lets a
  proof put a function of its own in the place of `foreign-append!`,
  `foreign-invoke-query`, `foreign-depot` or `foreign-query` for a while.
  The proof does so for four things, and each line that rests on it says
  so: to count the handles that the opening of a door fetches; to count the
  round trips of a call of the door; to note the calls of the door in their
  order, and of a read what it was asked and what Rama gave; and to make a
  read throw, since no real throw of a query was found that the in-process
  cluster gives without a fault of the cluster. The
  other throws are real throws of Rama's client: a module name under which
  no module runs, a thing that holds a plain Java object, a thing whose
  `:who` is an object of the proof's own, and a call made on a thread whose
  interrupt mark the proof set. The proof never destroys the module to make
  a call throw: the close of the cluster then throws. Two things of a
  maker's act are the proof's own objects: a `:session` that throws when it
  is read, and a `:who` that throws when it is hashed.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It opens seven doors on the
  module: the door of most checks; a second door, opened with a read each
  100 ms; one more for the count of the handles that an opening fetches;
  two in the check of the opener's number; one that reads each 5 ms, to
  time an act of the base; and one near the end, which stands for the door
  of another process. It pauses the microbatch gate with Rama's test tool,
  and lets it go on each time. It owns three atoms: the lines
  that it showed; each result that a door gave it; and what later checks
  come back to. It reads two clocks of its JVM and sets neither: `nanoTime`
  to time a wait, and `currentTimeMillis`, the clock that the door reads
  for the time on an act, round a call of the door.

  Approach. The checks run in one order on one cluster, and later checks
  build on the layers that earlier ones made. Near the end the proof starts
  the module again in the cluster, by Rama's module update with the
  unchanged module; Rama then prints some lines of its own log, marked
  ERROR, that are no failed checks. So does a thing that holds a plain Java
  object.

  What it does not reach is in the folder's README."
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.words :as words]
            [softland.store.gate.first-act :as first-act]
            [softland.store.gate.stamp :as stamp]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.nio.charset StandardCharsets]
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

;; ------------------------------------------------------- the proof's own state

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(def ^:dynamic ^:private *round*
  "What the proof holds of the cluster, round the door: the cluster itself,
  the name of the module, the two depots, two queries, and four PStates,
  the table of facts and the task's own table of each gate. And the two
  doors that are opened at the start: `:door`, and `:paced-door`, which is
  opened with a read each 100 ms. `-main` binds it for the run."
  nil)

(defrecord Thing [e k])

(defonce ^{:private true
           :doc "Each result that a call of the door gave the proof, in order: what
  the check over the whole run goes through."}
  results (atom []))

(defonce ^{:private true
           :doc "What one group of checks hands to a later one, by name."}
  kept (atom {}))

(defn- noted
  "Keeps a result that a door gave the proof, for the check that each of
  the door's words was given. Gives the result."
  [result]
  (swap! results conj result)
  result)

(defn- open-round
  "Takes the cluster. Gives the handles that the proof reads round the door
  with, and the two doors."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc              ipc
     :module           m
     :streaming-depot  (rama/foreign-depot ipc m "*streaming-depot")
     :microbatch-depot (rama/foreign-depot ipc m "*microbatch-depot")
     :answer           (rama/foreign-query ipc m "answer-by-act-id")
     :depot-of-layer   (rama/foreign-query ipc m "depot-of-layer")
     :streaming-facts  (rama/foreign-pstate ipc m "$$streaming-facts")
     :microbatch-facts (rama/foreign-pstate ipc m "$$microbatch-facts")
     :streaming-task   (rama/foreign-pstate ipc m "$$streaming-task")
     :microbatch-task  (rama/foreign-pstate ipc m "$$microbatch-task")
     :door             (noted (door/open ipc m))
     :paced-door       (noted (door/open ipc m {:read-every-ms 100}))}))

;; ------------------------------------------------------------ through the door

(defn- the-door "The door of most checks." [] (:door *round*))

(defn- the-paced-door "The door that was opened with a read each 100 ms." [] (:paced-door *round*))

(defn- act-id-for!
  "An act-id for a layer, through a door."
  [d layer]
  (noted (door/act-id-for d layer)))

(defn- send!
  "Sends a maker's act through a door, with the act-id that the maker asked
  for ahead when one is given."
  ([d act] (noted (door/send-act d act)))
  ([d act act-id] (noted (door/send-act d act act-id))))

(defn- answer!
  "Reads the answer of an act through a door, with a bound when one is
  given."
  ([d act-id digest] (noted (door/answer-of d act-id digest)))
  ([d act-id digest bound-ms] (noted (door/answer-of d act-id digest bound-ms))))

(def ^:private wait-ms
  "The bound that the proof gives a wait for the answer of an act of the
  base, in milliseconds."
  30000)

(defn- decide!
  "Sends an act through a door and reads its answer: at once for a streaming
  act, whose append waited for the gate, and with a bounded wait for an act
  of the base. Gives {:sent what `send-act` gave, :answer what `answer-of`
  gave}; with a word from the send there is no answer to read."
  ([d act] (decide! d act nil))
  ([d act act-id]
   (let [sent (send! d act act-id)]
     {:sent sent
      :answer (when-not (:could-not sent)
                (if (= :depot-holds (:ack sent))
                  (answer! d (:act-id sent) (:digest sent) wait-ms)
                  (answer! d (:act-id sent) (:digest sent))))})))

(defn- accepted?
  "True for a result of `decide!` whose act went in and was accepted."
  [decided]
  (= :accepted (:answer (:answer decided))))

;; -------------------------------------------------------------- round the door

(defn- log-of
  "Each record of a depot's log, read back, in the order of its partitions
  and of each partition's offsets. Takes :streaming or :microbatch."
  [depot-word]
  (let [depot (get *round* (if (= :microbatch depot-word) :microbatch-depot :streaming-depot))]
    (vec (for [p (range (:num-partitions (rama/foreign-object-info depot)))
               :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]
               r (rama/foreign-depot-read depot p start-offset end-offset)]
           r))))

(defn- log-count
  "How many records a depot's log holds, from the offsets of its partitions."
  [depot-word]
  (let [depot (get *round* (if (= :microbatch depot-word) :microbatch-depot :streaming-depot))]
    (reduce + (for [p (range (:num-partitions (rama/foreign-object-info depot)))
                    :let [{:keys [start-offset end-offset]} (rama/foreign-depot-partition-info depot p)]]
                (- end-offset start-offset)))))

(defn- log-counts
  "How many records the two logs hold: [streaming microbatch]."
  []
  [(log-count :streaming) (log-count :microbatch)])

(defn- records-of
  "The records of the log under an act-id: of the depot that the act-id
  names."
  [act-id]
  (filterv (fn [r] (and (map? r) (= act-id (get r :act-id)))) (log-of (nth act-id 1))))

(defn- record-of
  "The one record of the log under an act-id, or nil when the log holds none
  or more than one."
  [act-id]
  (let [found (records-of act-id)]
    (when (= 1 (count found)) (first found))))

(defn- clock
  "Reads the clock that the door reads for the time on an act: the JVM's
  `currentTimeMillis`, milliseconds since 1970. The proof and its doors run
  in one process, so this is the clock of the server that runs the door.
  The proof reads it just before and just after a call of the door, to hold
  the time that the door wrote between the two readings."
  []
  (System/currentTimeMillis))

(defn- time-on
  "The time on a record of the log: what it holds under the declaration's
  name of that part, or nil for a record that does not hold the part."
  [r]
  (get r declaration/time-part))

(defn- without-the-time
  "Takes a record of the log. Gives it without the part that holds the time
  on the act. Of a thing that the door appended, what is left is the maker's
  act with the door's two other additions, `:act-id` and `:depot`."
  [r]
  (dissoc r declaration/time-part))

(defn- time-between?
  "True for a time on an act as the door writes one: a fixed-width integer
  that is not before the reading `from` and not after the reading `to`.
  False for nil, which is what a record with no time gives. Limit: it rests
  on a clock that does not go back between the two readings."
  [t from to]
  (boolean (and (int? t) (<= from t to))))

(defn- seen-in-log
  "Takes an act-id and two readings of the clock, one from before the send
  under it and one from after. Gives, for each record of the log under the
  act-id, [the record without its time, whether its time is a time that the
  door wrote between the two readings]. So one thing that the door appended
  for a maker's act gives [[the act with `:act-id` and `:depot` added,
  true]]: the door's three additions, and nothing else changed."
  [act-id from to]
  (mapv (fn [r] [(without-the-time r) (time-between? (time-on r) from to)]) (records-of act-id)))

(defn- time-in
  "The time that a stamp holds, in milliseconds
  (`softland.store.gate.stamp/time-of`), or nil for a thing that is no
  stamp."
  [s]
  (when (int? s) (stamp/time-of s)))

(defn- counter-in
  "The counter that a stamp holds (`softland.store.gate.stamp/counter-of`),
  or nil for a thing that is no stamp."
  [s]
  (when (int? s) (stamp/counter-of s)))

(defn- answer-round
  "The answer under an act-id as the module's query gives it, read with the
  proof's own handle, or nil."
  [act-id]
  (rama/foreign-invoke-query (:answer *round*) (record/routing-id act-id) act-id))

(defn- row-of
  "The row of a place of a streaming layer, by a select of the streaming
  gate's table of facts on the task of the layer, or nil for a place with
  no row. What must hold: the layer is a streaming layer, and each of the
  three is an id within the limit."
  [layer e k]
  (rama/foreign-select-one (path/keypath (record/place-key layer e k)) (:streaming-facts *round*) {:pkey layer}))

(defn- rows-in
  "The row at one place in one gate's table of facts, as each task holds
  it: a vector with one entry for each task, nil where a task holds no row.
  Takes the proof's name of the table, :streaming-facts or
  :microbatch-facts, and the place. The microbatch gate keeps a fact of its
  layers as the same row on every task. What must hold: each of the three
  of the place is an id within the limit."
  [pstate layer e k]
  (mapv (fn [pkey]
          (rama/foreign-select-one (path/keypath (record/place-key layer e k)) (get *round* pstate) {:pkey pkey}))
        (rtest/gen-hashing-index-keys tasks)))

(defn- rows-at
  "Each row that the two gates hold at one place, by a select of both
  gates' tables of facts on each task: a vector, empty when no gate holds a
  row there. It asks every task, so it does not rest on where a gate keeps
  a place. A place of a streaming layer has one row at most; a place of a
  microbatch layer has its row once for each task. What must hold: each of
  the three is an id within the limit."
  [layer e k]
  (vec (remove nil? (concat (rows-in :streaming-facts layer e k) (rows-in :microbatch-facts layer e k)))))

(defn- strays
  "How many different strays the two gates hold, over every word and every
  task."
  []
  (reduce + (for [pstate [:streaming-task :microbatch-task]
                  word declaration/stray-reasons
                  k (rtest/gen-hashing-index-keys tasks)]
              (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                       (get *round* pstate) {:pkey k}))))

(defn- pause-base! "Pauses the microbatch gate, with Rama's test tool." []
  (rtest/pause-microbatch-topology! (:ipc *round*) (:module *round*) "microbatch-gate"))

(defn- resume-base! "Lets the microbatch gate go on." []
  (rtest/resume-microbatch-topology! (:ipc *round*) (:module *round*) "microbatch-gate"))

(defn- settle-base!
  "Waits until the microbatch gate has processed each record that its depot
  holds, with Rama's test tool, which throws when `wait-ms` passes first.
  The proof calls it before it reads a row of the base round the door: an
  answer shows when its batch is committed on the base's task, and the copy
  of a fact on another task can show a moment later. What must hold: the
  gate is not paused, and the module was not started again since the
  cluster started, since the proof does not know what the tool counts
  after that."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *round*) (:module *round*) "microbatch-gate"
                                             (log-count :microbatch) wait-ms))

;; ----------------------------------------- standing in for a call of Rama's client

(defn- counting
  "Calls f while the proof stands in for Rama's two client calls with
  functions that count and then call Rama. Gives {:result what f gave,
  :appends how many appends were made, :queries how many queries}. What must
  hold: f makes calls of the door only, and nothing else of the proof calls
  Rama meanwhile."
  [f]
  (let [appends (atom 0)
        queries (atom 0)
        real-append rama/foreign-append!
        real-query rama/foreign-invoke-query]
    (with-redefs [rama/foreign-append! (fn [& args] (swap! appends inc) (apply real-append args))
                  rama/foreign-invoke-query (fn [& args] (swap! queries inc) (apply real-query args))]
      (let [result (f)]
        {:result result :appends @appends :queries @queries}))))

(defn- noting
  "Calls f while the proof stands in for Rama's two client calls with
  functions that note each call and then call Rama. Gives {:result what f
  gave, :calls the calls in their order}, each call as [:append or :query,
  how many arguments it had after the handle]: 1 is the read of a layer's
  depot, 2 the read of an answer, and an append has 2. What must hold is
  what `counting` asks."
  [f]
  (let [calls (atom [])
        real-append rama/foreign-append!
        real-query rama/foreign-invoke-query]
    (with-redefs [rama/foreign-append! (fn [depot & args]
                                         (swap! calls conj [:append (count args)])
                                         (apply real-append depot args))
                  rama/foreign-invoke-query (fn [query & args]
                                              (swap! calls conj [:query (count args)])
                                              (apply real-query query args))]
      (let [result (f)]
        {:result result :calls @calls}))))

(defn- asking
  "Calls f while the proof stands in for Rama's client call
  `foreign-invoke-query` with a function that calls Rama and then notes
  what the query was asked and what it gave. Gives {:result what f gave,
  :asked the queries in their order}, each query as [the arguments after
  the handle, what Rama gave]: the read of a layer's depot has one
  argument, the layer; the read of an answer has two, the routing id and
  the act-id. What must hold is what `counting` asks."
  [f]
  (let [asked (atom [])
        real-query rama/foreign-invoke-query]
    (with-redefs [rama/foreign-invoke-query (fn [query & args]
                                              (let [got (apply real-query query args)]
                                                (swap! asked conj [(vec args) got])
                                                got))]
      (let [result (f)]
        {:result result :asked @asked}))))

(defn- fetching
  "Calls f while the proof stands in for the two calls of Rama's client
  that fetch a handle, `foreign-depot` and `foreign-query`, with functions
  that note the name asked for and then call Rama. Gives {:result what f
  gave, :depots the names of the depots asked for, :queries the names of
  the queries}, each in the order of the calls. What must hold is what
  `counting` asks."
  [f]
  (let [depots (atom [])
        queries (atom [])
        real-depot rama/foreign-depot
        real-query rama/foreign-query]
    (with-redefs [rama/foreign-depot (fn [& args] (swap! depots conj (last args)) (apply real-depot args))
                  rama/foreign-query (fn [& args] (swap! queries conj (last args)) (apply real-query args))]
      (let [result (f)]
        {:result result :depots @depots :queries @queries}))))

(defn- with-read-thrown
  "Calls f while the proof stands in for Rama's client call
  `foreign-invoke-query` with a function that throws for a call with n
  arguments after the handle, and calls Rama for each other: 1 is the read
  of a layer's depot, 2 the read of an answer."
  [n f]
  (let [real-query rama/foreign-invoke-query]
    (with-redefs [rama/foreign-invoke-query
                  (fn [query & args]
                    (if (= n (count args))
                      (throw (ex-info "the proof made this read throw" {:arguments n}))
                      (apply real-query query args)))]
      (f))))

(defn- on-a-thread
  "Runs f on a thread of its own and gives what it gave, or [:threw text]
  when it threw, or [:no-return] after 60 seconds. For a check that sets the
  interrupt mark of the thread that calls the door."
  [f]
  (let [out (promise)
        ^Runnable run (bound-fn [] (deliver out (try (f) (catch Throwable t [:threw (str t)]))))
        thread (Thread. run)]
    (.start thread)
    (deref out 60000 [:no-return])))

;; ----------------------------------------------------------------- the makers

(defn- own
  "The first permission of a personal layer: its person holds it, with
  nothing above it."
  [who]
  [who who who])

(defn- making-of
  "The making act of a personal layer, as a maker gives it: by the system,
  for the person, with the depot that the layer starts with."
  [who]
  {:version 1 :who :system :layer who :permission (own who) :session :door-proof
   :facts [{:e who :k :softland.store/depot :v :streaming}
           {:e who :k :softland.store/permission :v (own who)}]})

(defn- act-of
  "An act of a person into her own layer, as a maker gives it: plain values,
  no act-id, no depot. `more` is merged in last, for other parts and for
  faults."
  ([who facts] (act-of who facts {}))
  ([who facts more]
   (merge {:version 1 :who who :layer who :permission (own who) :session :door-proof :facts facts}
          more)))

(defn- value
  "A value fact as a maker gives it: a plain value under the plain key
  :text."
  [e v]
  {:e e :k :text :v v})

(defn- make-layer!
  "Makes the personal layer of a who, through the door. Gives true when the
  making act was accepted."
  [d who]
  (accepted? (decide! d (making-of who))))

(def ^:private system-own "The system's permission in the base, the base's first." [:system :base :base])
(def ^:private alice-in-base "Alice's permission in the base, beneath the system's." [:alice :base :base system-own])

(defn- system-act
  "The system's act into the base, as a maker gives it."
  [facts]
  {:version 1 :who :system :layer :base :permission system-own :session :door-proof :facts facts})

(defn- base-act-of
  "An act into the base of a who under a permission, as a maker gives it."
  [who permission facts]
  {:version 1 :who who :layer :base :permission permission :session :door-proof :facts facts})

(defn- time-sent
  "The time that the door wrote on the thing that it sent for a result of
  `send-act`, read back from the log of the depot that the act-id names; or
  nil when that log holds no record, or more than one, under the act-id."
  [sent]
  (when (:act-id sent)
    (time-on (record-of (:act-id sent)))))

(defn- rising?
  "True for stamps that are all there and of which each is above the one
  before it."
  [stamps]
  (boolean (and (seq stamps) (every? int? stamps) (apply < stamps))))

(defn- hex64?
  "True for a digest as a maker gets one: 64 hex characters."
  [x]
  (boolean (and (string? x) (re-matches #"[0-9a-f]{64}" x))))

(defn- told
  "A result of `decide!` as text for a line."
  [{:keys [sent answer]}]
  (if (:could-not sent)
    (pr-str (select-keys sent [:could-not :appended :at]))
    (str (subs (str (nth (:act-id sent) 2)) 0 8) " " (:ack sent) " " (pr-str answer))))

(defn- utf8-bytes
  "How many bytes the UTF-8 of a String has, by the JVM's own encoder and
  by no function of the store. nil for nil."
  [s]
  (when (string? s)
    (alength (.getBytes ^String s StandardCharsets/UTF_8))))

;; ---------------------------------------------------------------- the checks

(deftest the-door-is-opened
  (let [wrong (noted (door/open (:ipc *round*) "no.such/Module"))
        d (the-door)
        fetched (fetching #(noted (door/open (:ipc *round*) (:module *round*))))
        text (str "#<door " (:module *round*) ">")
        printed (fn [x] [(pr-str x) (str x) (str/trim (with-out-str (println x))) (str/trim (with-out-str (pprint/pprint x)))])]
    (check "a door opened on a name under which no module runs: the word, by a real throw of Rama's client, and no door"
           [:find-the-module :nothing "no.such/Module" true]
           [(:could-not wrong) (:appended wrong) (:module wrong) (string? (:threw wrong))])
    (check "the right name gives a door and no word; so does the same name with the door's one number given"
           [nil nil] [(:could-not d) (:could-not (the-paced-door))])
    (check "the opening of a door fetches four handles of Rama's client and no more: the two depots of acts, and the two queries, of an answer and of a layer's depot"
           [nil #{"*streaming-depot" "*microbatch-depot"} 2 #{"answer-by-act-id" "depot-of-layer"} 2]
           [(:could-not (:result fetched)) (set (:depots fetched)) (count (:depots fetched))
            (set (:queries fetched)) (count (:queries fetched))])
    (check "a door prints as the word door and the module's name, whatever prints it: pr, str, println, a pretty-printer"
           (vec (repeat 4 text)) (printed d))
    (shown "a door opened on a wrong module name:" (pr-str (select-keys wrong [:could-not :appended :module :threw]))
           "| a real throw of Rama's client")
    (shown "an opened door prints as" (pr-str d) "| its opening fetched the depots" (pr-str (:depots fetched)) "and the queries" (pr-str (:queries fetched))
           "and no other handle. The proof noted the fetches by standing in for Rama's two client calls that fetch a handle with functions that note the name and then call Rama.")))

(deftest an-act-with-no-value
  (let [d (the-door)
        before (act-id-for! d :alice)
        making (making-of :alice)
        logs-before (log-counts)
        clock-before (clock)
        sent (send! d making before)
        clock-after (clock)
        logs-after (log-counts)
        answer (answer! d (:act-id sent) (:digest sent))
        in-log (record-of before)
        door-time (time-on in-log)
        round-the-door (answer-round before)
        after (act-id-for! d :alice)
        series (counting #(door/act-ids-for d :alice 20))
        ids (noted (:result series))]
    (swap! kept assoc :making {:act-id before :digest (:digest sent) :answer answer})
    (check "act-id-for a layer that is not made: the layer, the depot that it starts with, and a uuid"
           [:alice :streaming true 3] [(nth before 0) (nth before 1) (uuid? (nth before 2)) (count before)])
    (check "the making act through the door: the act-id that the maker gave back, a digest of 64 hex characters, and the streaming gate's ack"
           [before true :gate-ran #{:act-id :digest :ack}] [(:act-id sent) (hex64? (:digest sent)) (:ack sent) (set (keys sent))])
    (check "one record more in the streaming depot's log, and none in the other" [1 0] (mapv - logs-after logs-before))
    (check "answer-of gives accepted, with a stamp" [:accepted true #{:answer :stamp}]
           [(:answer answer) (int? (:stamp answer)) (set (keys answer))])
    (check "the record in the log is the maker's act with the door's three additions, and nothing else changed: without its time it is the act with :act-id and :depot, and its parts are the maker's parts and those three"
           [(assoc making :act-id before :depot :streaming) (conj (set (keys making)) :act-id :depot declaration/time-part)]
           [(without-the-time in-log) (set (keys in-log))])
    (check "the time that the door wrote on it, read back from the depot, is a fixed-width integer, not before the proof's reading of the clock just before the send and not after its reading just after"
           [true true] [(int? door-time) (time-between? door-time clock-before clock-after)])
    (check "the digest that the maker got is the digest of the record in the log, which holds the time; it is the digest of that record with its time taken out, and with another time; and the gate's digest in the answer under the act-id is the same: the door and the gate both leave the time out"
           (vec (repeat 4 (:digest sent)))
           [(digest/hex-of in-log) (digest/hex-of (without-the-time in-log))
            (digest/hex-of (assoc in-log declaration/time-part 0)) (:digest round-the-door)])
    (check "the stamp of the answer holds the time that the door wrote, and no other clock: this making act is the first decision of its layer, so its stamp is that time with a counter of zero"
           [door-time 0] [(time-in (:stamp answer)) (counter-in (:stamp answer))])
    (check "after the making, act-id-for gives the same layer and depot, and another uuid"
           [[:alice :streaming] true] [(vec (take 2 after)) (not= (nth before 2) (nth after 2))])
    (check "act-ids-for 20: 20 different act-ids of the layer, made with one query and no append"
           [20 20 #{[:alice :streaming]} 1 0]
           [(count ids) (count (set ids)) (set (map (fn [id] (vec (take 2 id))) ids)) (:queries series) (:appends series)])
    (shown "the making act of a streaming layer through the door:" (pr-str (dissoc sent :digest)) "| answer-of:" (pr-str answer))
    (shown "the time that the door wrote on that act, read back from the depot:" door-time
           "| the proof's readings of the clock just before and just after the send:" clock-before clock-after
           "| the time and the counter in the stamp of its answer:" (time-in (:stamp answer)) (counter-in (:stamp answer))
           "| the parts of the record that the maker did not give:" (pr-str (vec (sort (remove (set (keys making)) (keys in-log))))))
    (shown "act-ids-for 20 in one layer: 20 different act-ids with" (:queries series)
           "query. The proof counted the query by standing in for Rama's client call with a function that counts and then calls Rama.")))

(deftest round-trips-are-counted
  (let [d (the-door)
        made? (make-layer! d :carol)
        logs-before (log-counts)
        first-value (counting #(send! d (act-of :carol [(value :c-1 "one")])))
        given-id (act-id-for! d :carol)
        held (counting #(send! d (act-of :carol [(value :c-2 "two")]) given-id))
        five (counting #(send! d (act-of :carol (mapv (fn [i] (value (keyword (str "c-5-" i)) i)) (range 5)))))
        logs-after (log-counts)
        answers (mapv (fn [counted] (answer! d (:act-id (:result counted)) (:digest (:result counted)))) [first-value five])
        one-read (counting #(answer! d given-id (:digest (:result held))))
        never (act-id-for! d :carol)
        began (System/nanoTime)
        waited (counting #(answer! d never (:digest (:result held)) 600))
        waited-ms (quot (- (System/nanoTime) began) 1000000)
        no-wait (mapv (fn [bound] (counting #(answer! d never (:digest (:result held)) bound))) [0 -5 "soon" nil])
        great (mapv (fn [bound] (counting #(answer! d given-id (:digest (:result held)) bound)))
                    [Long/MAX_VALUE 100000000000000000000N 1e300 Double/POSITIVE_INFINITY 1/2 0.4])
        began-no-id (System/nanoTime)
        no-id (counting #(answer! d :no-act-id (:digest (:result held)) 600))
        no-id-ms (quot (- (System/nanoTime) began-no-id) 1000000)
        trips (fn [counted] [(:appends counted) (:queries counted)])]
    (check "the layer of this check is made" true made?)
    (check "each of the three sends went in" [nil nil nil] (mapv (comp :could-not :result) [first-value held five]))
    (check "a person's first send of a value, which makes its act-id: 1 append, of the act, and 1 query, the read of the layer's depot" [1 1] (trips first-value))
    (check "a send of a value with a given act-id: 1 append and no query" [1 0] (trips held))
    (check "a send of five values that makes its act-id: 1 append and 1 query, as for one value" [1 1] (trips five))
    (check "the three sends are three records more in the streaming depot's log and none in the other, and the two that made their act-id are accepted"
           [[3 0] [:accepted :accepted]] [(mapv - logs-after logs-before) (mapv :answer answers)])
    (check "an answer-of with no wait: 1 query, and the answer" [[0 1] :accepted] [(trips one-read) (:answer (:result one-read))])
    (check "a wait of 600 ms on an act-id that was never sent: 4 queries, nothing yet, and it comes back after the bound"
           [[0 4] {:answer :nothing-yet} true] [(trips waited) (:result waited) (<= 600 waited-ms 5000)])
    (check "a bound of zero, a bound below zero, and a bound that is no number or nil: one read each, and nothing yet"
           (vec (repeat 4 [[0 1] {:answer :nothing-yet}])) (mapv (fn [counted] [(trips counted) (:result counted)]) no-wait))
    (check "a bound greater than any wait that the door counts, as a long, a big integer, a double and infinity, and a bound that is a ratio or a fraction: no call throws, and each reads the answer that is there with one query"
           (vec (repeat 6 [[0 1] :accepted])) (mapv (fn [counted] [(trips counted) (:answer (:result counted))]) great))
    (check "answer-of a thing that is no act-id, with a bound: nothing yet, with no query, at once"
           [[0 0] {:answer :nothing-yet} true] [(trips no-id) (:result no-id) (< no-id-ms 300)])
    (shown "round trips of one call of the door, [appends queries]: a person's first send of a value, which makes its act-id" (pr-str (trips first-value))
           "| a send with its act-id given" (pr-str (trips held))
           "| a send of five values that makes its act-id" (pr-str (trips five)) "| an answer-of" (pr-str (trips one-read))
           "| a wait of 600 ms that finds nothing" (pr-str (trips waited)) "in" waited-ms
           "ms. The proof counted them by standing in for Rama's two client calls with functions that count and then call Rama.")))

(deftest a-value-is-in-the-log-as-given
  (let [d (the-door)
        made? (make-layer! d :dora)
        v {:b 1.50M :a [1 2N]}
        first-act (act-of :dora [(value :note-1 v)] {:session :doras-session})
        logs-before (log-counts)
        clock-before (clock)
        landed (decide! d first-act)
        clock-after (clock)
        logs-after (log-counts)
        act-id (:act-id (:sent landed))
        record (record-of act-id)
        fact (first (:facts record))
        in-log (:v fact)
        rows (rows-at :dora :note-1 :text)
        round-the-door (answer-round act-id)
        mixed-facts [{:e :dan :k :softland.store/permission :v [:dan :dora :dora (own :dora)]}
                     {:e :note-1 :k :text}
                     (value :note-2 "one value")]
        mixed (decide! d (act-of :dora mixed-facts))
        mixed-record (record-of (:act-id (:sent mixed)))]
    (swap! kept assoc :landed {:act-id act-id :digest (:digest (:sent landed)) :answer (:answer landed)})
    (check "the layer of this check is made" true made?)
    (check "a person's first act with a value is one record more in the streaming depot's log, the act itself, and it is accepted"
           [[1 0] true] [(mapv - logs-after logs-before) (accepted? landed)])
    (check "the record in the log is the maker's act with the door's three additions: without its time it is the act with :act-id and :depot, its time is a time of the door's clock between the proof's two readings round the call, and its fact holds :e, :k and :v and no other part"
           [(assoc first-act :act-id act-id :depot :streaming) true #{:e :k :v}]
           [(without-the-time record) (time-between? (time-on record) clock-before clock-after) (set (keys fact))])
    (check "the time in the stamp of its answer is the time that the door wrote on it: the act was sent after the layer's making act, so its time is not before the time in the layer's last stamp"
           (time-on record) (time-in (:stamp (:answer landed))))
    (check "the value in the log is as the maker gave it and not in its normal form: the decimal keeps its trailing zero, and the big integer is big"
           ["1.50M" "[1 2N]" #{:a :b}]
           [(pr-str (:b in-log)) (pr-str (:a in-log)) (when (map? in-log) (set (keys in-log)))])
    (check "one gate holds one row at the place, and it holds the one text of the value in its normal form"
           ["{:a [1 2] :b 1.5M}"] (mapv :value rows))
    (check "a reader of that row is given the value" [:value {:a [1 2] :b 1.5M}] (record/read-value (first rows)))
    (check "the digest that the maker got is the digest of the record in the log, which holds the time, and the record's answer under the act-id is of that digest: the door's digest is the gate's, of the thing with the time in it"
           [true (:digest (:sent landed)) (:digest (:sent landed))]
           [(hex64? (:digest (:sent landed))) (digest/hex-of record) (:digest round-the-door)])
    (check "an act with a store-key fact, a retract and a value is accepted, and each of its facts is in the log as given"
           [true mixed-facts] [(accepted? mixed) (:facts mixed-record)])
    (check "after it the place of the retract holds nothing, and the place of the value holds the text of its value"
           [[:nothing-here] "\"one value\""]
           [(record/read-value (row-of :dora :note-1 :text)) (:value (row-of :dora :note-2 :text))])
    (shown "a person's first act with a value, through the door:" (told landed) "| the value in the log:" (pr-str in-log)
           "| the value in the fact's row:" (pr-str (:value (first rows)))
           "| the proof read the log and the row round the door, with handles of its own")
    (shown "an act with a store-key fact, a retract and a value:" (told mixed) "| its facts in the log:" (pr-str (:facts mixed-record)))))

(defn- without-the-uuid-and-the-time
  "Takes a record of the log. Gives it with two parts set aside, each under
  a word of the proof's: the uuid of its act-id, which is made at random,
  and its time, which is read from a clock. They are the two parts of a
  thing that are made of nothing of the maker's act. The time is set aside
  only where the record holds a fixed-width integer there, so a record with
  no time, or with a time of another kind, stays as it is and equals no
  record whose time was set aside."
  [r]
  (cond-> (assoc r :act-id (assoc (:act-id r) 2 :the-acts-uuid))
    (int? (time-on r)) (assoc declaration/time-part :the-time-of-a-clock)))

(deftest the-door-and-the-hand-send-the-same-thing
  (let [d (the-door)
        made? (make-layer! d :erin)
        facts [(value :note-1 {:n 1 :tags #{:a}})
               {:e :note-2 :k :text}
               {:e :erin :k :softland.store/who-may-see :v :members}]
        ;; through the door
        clock-before (clock)
        by-door (decide! d (act-of :erin facts))
        clock-after (clock)
        door-record (record-of (:act-id (:sent by-door)))
        ;; by the hand: the hand's steps, written out a second time
        depot-word (or (rama/foreign-invoke-query (:depot-of-layer *round*) :erin) (first-act/depot-at-making :erin))
        hand-id [:erin depot-word (UUID/randomUUID)]
        hand-time (clock)
        hand-thing (merge {:act-id hand-id :depot depot-word declaration/time-part hand-time} (act-of :erin facts))
        _ (rama/foreign-append! (:streaming-depot *round*) hand-thing :ack)
        hand-answer (answer-round hand-id)
        hand-record (record-of hand-id)]
    (check "the layer of this check is made" true made?)
    (check "one act with a value, a retract and a store-key fact goes in through the door and is accepted" true (accepted? by-door))
    (check "the same act by the hand is accepted, with the digest of what the hand sent, and the time in its stamp is the time that the hand wrote on it, a reading of the clock after the door's send"
           [:accepted (digest/hex-of hand-thing) hand-time]
           [(:verdict hand-answer) (:digest hand-answer) (time-in (:stamp hand-answer))])
    (check "the door's record in the log, with the uuid of its act-id and its time set aside, is the maker's act with the door's three additions, written out here"
           {:version 1 :who :erin :layer :erin :permission [:erin :erin :erin] :session :door-proof
            :facts [{:e :note-1 :k :text :v {:n 1 :tags #{:a}}}
                    {:e :note-2 :k :text}
                    {:e :erin :k :softland.store/who-may-see :v :members}]
            :act-id [:erin :streaming :the-acts-uuid]
            :depot :streaming
            declaration/time-part :the-time-of-a-clock}
           (without-the-uuid-and-the-time door-record))
    (check "the time that was set aside in the door's record is a time of the door's clock between the proof's two readings round the call, and the hand's record holds the time that the hand wrote"
           [true hand-time] [(time-between? (time-on door-record) clock-before clock-after) (time-on hand-record)])
    (check "the hand's record is equal to the door's but for that uuid and the time"
           (without-the-uuid-and-the-time door-record) (without-the-uuid-and-the-time hand-record))
    (check "the two act-ids differ, and each holds a uuid: two records were compared, and the uuid and the time are the two parts in which they can differ"
           [false true true]
           [(= (:act-id door-record) (:act-id hand-record))
            (uuid? (get (:act-id door-record) 2))
            (uuid? (get (:act-id hand-record) 2))])
    (check "the place that both acts wrote holds one row, with the one text of the value in its normal form"
           ["{:n 1 :tags #{:a}}"] (mapv :value (rows-at :erin :note-1 :text)))
    (shown "the door and the hand send the same thing for one act with a value, a retract and a store-key fact, but for the uuid of the act-id and the time:"
           (pr-str (without-the-uuid-and-the-time door-record))
           "| both accepted. The proof appended the hand's act itself, with a time that it read from the clock.")))

(deftest the-digest-is-the-gates
  (let [d (the-door)
        facts [(value :list-1 "a value") {:e :list-2 :k :text} (value :list-3 [1 2])]
        kinds [[:vector (vec facts)] [:list (apply list facts)] [:lazy-sequence (map identity facts)]]
        sent (mapv (fn [[kind given]] [kind (decide! d (act-of :alice given))]) kinds)
        seen (mapv (fn [[kind decided]]
                     (let [r (record-of (:act-id (:sent decided)))]
                       [kind
                        (:answer (:answer decided))
                        (int? (time-on r))
                        (= (:digest (:sent decided)) (digest/hex-of r))
                        (vector? (:facts r))
                        (sequential? (:facts r))
                        (= facts (:facts r))]))
                   sent)
        ;; one act under one act-id, with its facts in two forms
        one-fact [(value :form-1 "one act, its facts in three forms")]
        id-v (act-id-for! d :alice)
        as-vector (send! d (act-of :alice (vec one-fact)) id-v)
        as-list-after (send! d (act-of :alice (apply list one-fact)) id-v)
        vectors-answer (answer! d id-v (:digest as-vector))
        lists-answer-after (answer! d id-v (:digest as-list-after))
        id-l (act-id-for! d :alice)
        as-list (send! d (act-of :alice (apply list one-fact)) id-l)
        as-lazy (send! d (act-of :alice (map identity one-fact)) id-l)
        lists-answer (answer! d id-l (:digest as-list))
        lazys-answer (answer! d id-l (:digest as-lazy))]
    (check "for facts given as a vector, a list and a lazy sequence: the answer under the act-id is of the digest that send-act gave, the record in the log holds the time, and the digest is that of this record, with the time in it; in the log :facts is a vector only where the maker gave one, a sequence in each case, with each fact at its position"
           [[:vector :accepted true true true true true]
            [:list :accepted true true false true true]
            [:lazy-sequence :accepted true true false true true]]
           seen)
    (check "one act under one act-id, sent with its facts as a vector and then as a list: two digests, since the one text writes a vector and a list apart; the first send reads its answer, and the second reads not mine"
           [true true false :accepted {:answer :not-mine}]
           [(hex64? (:digest as-vector)) (hex64? (:digest as-list-after)) (= (:digest as-vector) (:digest as-list-after))
            (:answer vectors-answer) lists-answer-after])
    (check "the same act under another act-id, sent with its facts as a list and then as a lazy sequence: one digest, and both sends read the one answer as their own"
           [true :accepted true]
           [(= (:digest as-list) (:digest as-lazy)) (:answer lists-answer) (= lists-answer lazys-answer)])
    (shown "the digest that the door gives is the gate's, for facts given as" (pr-str (mapv first kinds))
           ": each act reads accepted under its own digest, and :facts is in the log as a vector for"
           (pr-str (mapv first (filter (fn [row] (nth row 4)) seen))) "alone")
    (shown "one act under one act-id with its facts as a vector and then as a list reads" (:answer vectors-answer) "and" (:answer lists-answer-after)
           "| with its facts as a list and then as a lazy sequence it reads" (:answer lists-answer) "and" (:answer lazys-answer))))

(deftest things-that-are-no-act
  (let [d (the-door)
        alice (fn ([facts] (act-of :alice facts)) ([facts more] (act-of :alice facts more)))
        logs-before (log-counts)
        strays-before (strays)
        said (fn [word] (select-keys word [:could-not :appended :at]))
        could-not (fn ([w] {:could-not w :appended :nothing}) ([w at] {:could-not w :appended :nothing :at at}))
        throws-when-read (lazy-seq (throw (ex-info "the proof made this part throw when it is read" {})))
        ;; a thing that already holds the part of the door's time, with a number and with nil
        logs-before-time-given (log-counts)
        time-given (mapv (fn [t] (said (send! d (assoc (alice [(value :n 1)]) declaration/time-part t))))
                         [1760000000000 nil])
        logs-after-time-given (log-counts)
        words-given
        [["no map" (mapv (fn [x] (said (send! d x))) [nil 7 "x" [1 2]]) (vec (repeat 4 (could-not :read-the-act)))]
         ["no :layer" (said (send! d (dissoc (alice [(value :n 1)]) :layer))) (could-not :make-an-act-id :layer)]
         ["a sorted map with string keys" (said (send! d (sorted-map "layer" :alice "facts" []))) (could-not :read-the-act :layer)]
         ["a record with a :layer" (said (send! d (assoc (->Thing :n :text) :layer :alice :facts []))) (could-not :read-the-act)]
         ["a :layer that is no id" (mapv (fn [layer] (said (send! d (assoc (alice [(value :n 1)]) :layer layer)))) ["notes" 7])
          (vec (repeat 2 (could-not :make-an-act-id :layer)))]
         ["a :layer of 257 characters" (said (send! d (assoc (alice []) :layer (keyword (apply str (repeat 256 "a")))))) (could-not :make-an-act-id :layer)]
         ["an act that already carries :act-id, or :depot"
          [(said (send! d (assoc (alice [(value :n 1)]) :act-id [:alice :streaming (UUID/randomUUID)])))
           (said (send! d (assoc (alice [(value :n 1)]) :depot :streaming)))]
          [(could-not :add-its-parts :act-id) (could-not :add-its-parts :depot)]]
         ["an act-id given back that is of another layer" (said (send! d (alice [(value :n 1)]) [:carol :streaming (UUID/randomUUID)])) (could-not :make-an-act-id :act-id)]
         ["a :session that throws when it is read, which the door meets in the walk for its digest"
          (said (send! d (alice [(value :n 1)] {:session throws-when-read}))) (could-not :read-the-act)]]
        ;; a who that the door does not read, and that Rama's client cannot write
        no-hash (reify Object (hashCode [_] (throw (ex-info "the proof made this who throw when it is hashed" {}))))
        unhashed-id (act-id-for! d :alice)
        unhashed (send! d (alice [(value :t-1 "a value")] {:who no-hash}) unhashed-id)
        logs-after-words (log-counts)
        strays-after-words (strays)
        plain-in-facts {:e :plain-1 :k :text :v "a value in a fact map that is no sequence of facts"}
        no-id-fact {:e "n1" :k :text :v "a value at an entity that is no id"}
        goes-in
        [[":facts absent" (dissoc (alice []) :facts) :malformed]
         [":facts that is one fact map" (alice plain-in-facts) :malformed]
         [":facts that is a set" (alice #{{:e :plain-2 :k :text}}) :malformed]
         ["a fact that is a string" (alice ["a fact"]) :malformed]
         ["a fact that is a record, with a value" (alice [(assoc (->Thing :n-record :text) :v "x")]) :malformed]
         ["a value that is a ratio" (alice [(value :n-ratio 1/3)]) :malformed]
         ["a value that is a list" (alice [(value :n-list (list 1 2))]) :malformed]
         ["a value that is a double and no number" (alice [(value :n-nan Double/NaN)]) :malformed]
         ["a value fact whose :e is no id" (alice [no-id-fact]) :malformed]
         ["a :who that is a sorted map" (alice [(value :by-a-sorted-map "a value")] {:who (sorted-map :a 1)}) :malformed]
         ["a :who that is a map with string keys" (alice [(value :by-a-map "a value")] {:who {"a" 1}}) :malformed]
         ["a fact that carries :sealed and :lock-id, and no :v"
          (alice [{:e :forged :k :text :sealed (byte-array 40) :lock-id [[:alice :streaming (UUID/randomUUID)] 0]}]) :unknown-part]
         ["a fact with a value that also holds :sealed" (alice [(assoc (value :n-sealed 1) :sealed (byte-array 3))]) :unknown-part]
         ["a fact with a value under a store key that also holds :sealed" (alice [{:e :alice :k :softland.store/depot :v :streaming :sealed (byte-array 3)}]) :unknown-part]
         ["a fact under :softland.store/lock-request, which is no key of the store" (alice [{:e :asked :k :softland.store/lock-request :v 4}]) :store-key-not-known]
         ["a part that the form does not know" (assoc (alice [{:e :n :k :text}]) :note "kept plain") :unknown-part]
         ["a part that is named as the door's own words are, :could-not" (assoc (alice [{:e :n :k :text}]) :could-not :read-the-act) :unknown-part]
         ["257 facts, two with a value" (alice (into [(value :m-1 1) (value :m-2 2)] (map (fn [i] {:e (keyword (str "r" i)) :k :text}) (range 255)))) :too-many-facts]
         ["300 facts that each hold a value" (alice (mapv (fn [i] (value (keyword (str "e" i)) i)) (range 300))) :too-many-facts]
         ["1,025 entries of :based-on" (assoc (alice [{:e :n :k :text}]) :based-on (into {} (map (fn [_] [[[:alice :streaming (UUID/randomUUID)] 0] 1])) (range 1025))) :too-many-based-on]
         ["an entity of 300 characters" (alice [{:e (keyword (apply str (repeat 299 "a"))) :k :text}]) :id-too-long]]
        gone-in (mapv (fn [[what thing reason]] [what (decide! d thing) reason]) goes-in)
        logs-after (log-counts)
        decided-of (fn [what] (some (fn [[w decided]] (when (= what w) decided)) gone-in))
        one-fact-map (record-of (:act-id (:sent (decided-of ":facts that is one fact map"))))
        no-id-entity (record-of (:act-id (:sent (decided-of "a value fact whose :e is no id"))))
        ;; each place that one of the refused things named, by its own key: one is under a key of the store's namespace
        places-named (conj (mapv (fn [e] [e :text])
                                 [:plain-1 :n-record :n-ratio :n-list :n-nan :by-a-sorted-map :by-a-map :forged :n-sealed :m-1 :m-2 :e0 :e299])
                           [:asked :softland.store/lock-request])
        rows-left (vec (mapcat (fn [[e k]] (rows-at :alice e k)) places-named))
        ;; the edge of one value is a gate's
        inside-the-bound (decide! d (alice [(value :edge-1 (apply str (repeat 65507 "a")))]))
        at-the-bound (decide! d (alice [(value :edge-2 (apply str (repeat 65534 "a")))]))
        over-the-bound (decide! d (alice [(value :edge-3 (apply str (repeat 65535 "a")))]))
        over-record (record-of (:act-id (:sent over-the-bound)))]
    (doseq [[what observed expected] words-given]
      (check (str what ": the door's word, and nothing is appended") expected observed))
    (check "a maker's thing that already holds the part of the door's time, with a number and with nil: each comes back with the door's word :add-its-parts at that part, since the door writes over nothing; and neither log holds a record more"
           [(vec (repeat 2 (could-not :add-its-parts declaration/time-part))) [0 0]]
           [time-given (mapv - logs-after-time-given logs-before-time-given)])
    (check "a value act whose :who throws when it is hashed: the door reads no who and stops nothing of it; Rama's client cannot write the object, a real throw; the word says that the door does not know, with the act-id and a digest, and the log holds nothing under that act-id"
           [:append-cleanly :not-known unhashed-id true []]
           [(:could-not unhashed) (:appended unhashed) (:act-id unhashed) (hex64? (:digest unhashed)) (records-of unhashed-id)])
    (check "after each of those words: no record more in either log, and no stray more"
           [logs-before strays-before] [logs-after-words strays-after-words])
    (doseq [[what decided reason] gone-in]
      (check (str what ": the thing goes in, and answer-of gives the gate's recorded refusal")
             [nil :refused reason] [(:could-not (:sent decided)) (:answer (:answer decided)) (:reason (:answer decided))]))
    (check "each of those things is one record more in the streaming depot's log, and none is a stray"
           [[(count goes-in) 0] strays-before]
           [(mapv - logs-after logs-after-words) (strays)])
    (check "where :facts is one fact map, the map is in the log as its maker gave it, with its value plain: the door reads no fact"
           plain-in-facts (:facts one-fact-map))
    (check "a value fact whose :e is no id is in the log as its maker gave it"
           no-id-fact (first (:facts no-id-entity)))
    (check "no fact of a refused thing is in the tables: no gate holds a row at a place that one of them named, on any task"
           [] rows-left)
    (check "a string of 65,507 characters under a plain key is accepted, and its row holds the 65,509 bytes of its text"
           [true 65509] [(accepted? inside-the-bound) (utf8-bytes (:value (row-of :alice :edge-1 :text)))])
    (check "the edge is a gate's, at 65,536 bytes of the value's text: a string of 65,534 characters, whose text with its two quotes is 65,536 bytes, is accepted, and its row holds those bytes"
           [true 65536] [(accepted? at-the-bound) (utf8-bytes (:value (row-of :alice :edge-2 :text)))])
    (check "one character more, 65,537 bytes of text: the thing goes in, the log holds the value whole, a gate refuses it with its word, and no gate holds a row at its place"
           [nil {:answer :refused :reason :value-too-large} 65535 []]
           [(:could-not (:sent over-the-bound)) (dissoc (:answer over-the-bound) :stamp)
            (count (:v (first (:facts over-record)))) (rows-at :alice :edge-3 :text)])
    (shown "things that are no act, each ending in one of the door's words with nothing appended:"
           (str/join " | " (map (fn [[what observed _]] (str what " " (pr-str (if (vector? observed) (first observed) observed)))) words-given)))
    (shown "a maker's thing that already holds" (pr-str declaration/time-part) "with a number, and with nil:" (pr-str time-given)
           "| records more in the two logs:" (pr-str (mapv - logs-after-time-given logs-before-time-given)))
    (shown "a value act whose :who throws when it is hashed:" (pr-str (select-keys unhashed [:could-not :appended :threw]))
           "| the door read no who; the throw is Rama's client's, which cannot write the object")
    (shown "things that are no act and go in, each with the gate's recorded refusal read by answer-of:"
           (str/join " | " (map (fn [[what decided _]] (str what " " (:reason (:answer decided)))) gone-in)))
    (shown "the edge of one value is a gate's: a value whose text is 65,536 bytes is" (:answer (:answer at-the-bound))
           "| one byte more is in the log whole and" (pr-str (dissoc (:answer over-the-bound) :stamp)))))

(deftest the-answer-back
  (let [d (the-door)
        ;; accepted and refused
        id-1 (act-id-for! d :alice)
        first-sent (decide! d (act-of :alice [(value :stamped "one")]) id-1)
        stale (decide! d (act-of :alice [{:e :stale :k :text :replaces [[:alice :streaming (UUID/randomUUID)] 0]}]))
        ;; nothing yet
        never (answer! d (act-id-for! d :alice) (:digest (:sent first-sent)))
        no-act-id (counting #(answer! d "no act-id" (:digest (:sent first-sent))))
        ;; not mine: a second, different act under a used act-id
        other (send! d (act-of :alice [{:e :other :k :text}]) id-1)
        others-answer (answer! d id-1 (:digest other))
        still (answer! d id-1 (:digest (:sent first-sent)))
        no-digest (answer! d id-1 nil)
        ;; a maker's own second send of one act with a value
        id-2 (act-id-for! d :alice)
        twice (act-of :alice [(value :twice "the same act")])
        clock-1 (clock)
        send-1 (send! d twice id-2)
        clock-2 (clock)
        send-2 (send! d twice id-2)
        clock-3 (clock)
        answer-1 (answer! d id-2 (:digest send-1))
        answer-2 (answer! d id-2 (:digest send-2))
        twice-records (records-of id-2)
        [time-1 time-2] (mapv time-on twice-records)
        twice-round (answer-round id-2)
        ;; a wait whose thread is interrupted
        never-2 (act-id-for! d :alice)
        out (promise)
        ^Runnable run (bound-fn []
                        (let [began (System/nanoTime)
                              got (answer! d never-2 (:digest (:sent first-sent)) 20000)]
                          (deliver out [got (Thread/interrupted) (quot (- (System/nanoTime) began) 1000000)])))
        waiting (Thread. run)
        _ (.start waiting)
        _ (Thread/sleep 300)
        _ (.interrupt waiting)
        [interrupted-answer mark-set? interrupted-ms] (deref out 30000 [:no-return false 30000])
        ;; the stamp is usable
        stamp (:stamp (:answer first-sent))
        replaced (decide! d (act-of :alice [(assoc (value :stamped "two") :replaces [id-1 0])]
                                    {:based-on {[id-1 0] stamp}}))]
    (check "accepted carries the stamp, and nothing more" [#{:answer :stamp} :accepted true]
           [(set (keys (:answer first-sent))) (:answer (:answer first-sent)) (int? stamp)])
    (check "refused carries the gate's reason word and the stamp"
           [#{:answer :reason :stamp} :refused :replaced-fact-not-current true]
           [(set (keys (:answer stale))) (:answer (:answer stale)) (:reason (:answer stale)) (int? (:stamp (:answer stale)))])
    (check "nothing yet, for an act-id that was never sent" {:answer :nothing-yet} never)
    (check "nothing yet, for a thing that is no act-id, with no query made" [{:answer :nothing-yet} 0] [(:result no-act-id) (:queries no-act-id)])
    (check "a second, different act through the door under a used act-id goes to the depot and reads not mine, and nothing more"
           [:gate-ran {:answer :not-mine}] [(:ack other) others-answer])
    (check "the first act's own digest still reads its own answer; no digest reads not mine"
           [(:answer first-sent) {:answer :not-mine}] [still no-digest])
    (check "a maker's own second send of one act with a value, under the act-id that she gives back: one digest of 64 hex characters for both sends; both went to the depot, which holds two records under the act-id; and each of the two, without its time, is the maker's act with :act-id and :depot"
           [true true [:gate-ran :gate-ran] 2 (vec (repeat 2 (assoc twice :act-id id-2 :depot :streaming)))]
           [(hex64? (:digest send-1)) (= (:digest send-1) (:digest send-2)) [(:ack send-1) (:ack send-2)]
            (count twice-records) (mapv without-the-time twice-records)])
    (check "each of the two records holds the time of its own send: the first a time between the proof's readings of the clock round the first send, the second a time between its readings round the second; so the second is not before the first, and the two have one digest, the one that the sends gave"
           [true true true [(:digest send-1) (:digest send-1)]]
           [(time-between? time-1 clock-1 clock-2) (time-between? time-2 clock-2 clock-3)
            (boolean (and (int? time-1) (int? time-2) (<= time-1 time-2)))
            (mapv digest/hex-of twice-records)])
    (check "a gate answered that act-id once, and both sends read the one answer as their own: accepted, with one stamp; the gate's digest in that answer is the digest of both sends, and the time in its stamp is the time on the first of the two records, the one that a gate decided: that send came after each earlier act of its layer"
           [{:answer :accepted} true true (:digest send-2) time-1]
           [(dissoc answer-1 :stamp) (int? (:stamp answer-1)) (= answer-1 answer-2)
            (:digest twice-round) (time-in (:stamp answer-1))])
    (check "a wait of 20 seconds whose thread is interrupted after 300 ms ends as if its bound had passed, with the thread's mark set again: nothing yet, or, when the interrupt met a read on its way, the word of that read"
           [true true true]
           [(or (= {:answer :nothing-yet} interrupted-answer)
                (= [:read-the-answer "java.lang.InterruptedException"] [(:could-not interrupted-answer) (:threw interrupted-answer)]))
            mark-set?
            (< interrupted-ms 5000)])
    (check "the stamp is usable: an act that replaces the fact and names the stamp in :based-on is accepted" true (accepted? replaced))
    (shown "the four answers through the door:" (pr-str (:answer first-sent)) (pr-str (:answer stale)) (pr-str never) (pr-str others-answer))
    (shown "a wait of 20 s whose thread was interrupted after 300 ms came back after" interrupted-ms "ms with" (pr-str (select-keys interrupted-answer [:answer :could-not :threw]))
           "and the thread's mark set | the proof interrupted its own thread")
    (shown "a maker's own second send of one act with a value under one act-id: the two digests" (subs (:digest send-1) 0 12) (subs (:digest send-2) 0 12)
           "read" (:answer answer-1) "and" (:answer answer-2) "| records in the log under that act-id:" (count twice-records)
           "| the times on the two:" time-1 time-2
           (cond (not (and (int? time-1) (int? time-2))) "| a record holds no time"
                 (= time-1 time-2) "| the second send was seen with the same time as the first"
                 :else (str "| the second send was seen with another time, " (- time-2 time-1) " ms after the first"))
           "| the time in the stamp of the one answer:" (time-in (:stamp answer-1)))))

(deftest a-maker-who-may-not-act
  (let [d (the-door)
        ;; a place of the same layer that its holder writes: the select that must find no row for a refused act finds this one
        alices (decide! d (act-of :alice [(value :a-1 "a value of the layer's holder")]))
        alices-rows (rows-at :alice :a-1 :text)
        ;; a who under a permission that was never given
        id (act-id-for! d :alice)
        mallorys [:mallory :alice :alice (own :alice)]
        mallorys-act {:version 1 :who :mallory :layer :alice :permission mallorys :session :door-proof
                      :facts [(value :m-1 "a value of a who with no permission")]}
        logs-before (log-counts)
        clock-before (clock)
        sent (counting #(send! d mallorys-act id))
        clock-after (clock)
        logs-after (log-counts)
        answer (answer! d id (:digest (:result sent)))
        round-the-door (answer-round id)
        ;; a value act into a layer that is not made
        nowheres-act (act-of :nowhere [(value :n-1 "a value into a layer that is not made")])
        logs-before-2 (log-counts)
        clock-before-2 (clock)
        not-made (decide! d nowheres-act)
        clock-after-2 (clock)
        logs-after-2 (log-counts)
        not-made-id (:act-id (:sent not-made))]
    (check "a value of the layer's holder is accepted, and the select round the door finds its one row"
           [true ["\"a value of the layer's holder\""]] [(accepted? alices) (mapv :value alices-rows)])
    (check "a who under a permission that was never given sends a value act into that layer: the door checks nothing of her and stops nothing; one append and no query, the streaming gate's ack, and one record more in the streaming depot's log"
           [nil :gate-ran [1 0] [1 0]]
           [(:could-not (:result sent)) (:ack (:result sent)) [(:appends sent) (:queries sent)] (mapv - logs-after logs-before)])
    (check "the log holds her act under its own act-id, as she gave it, with its value plain: one record, which without its time is her act with :act-id and :depot, and whose time is a time of the door's clock between the proof's two readings round the send"
           [[(assoc mallorys-act :act-id id :depot :streaming) true]] (seen-in-log id clock-before clock-after))
    (check "a gate refuses it, and the refusal is recorded under the act's own act-id with the gate's word: answer-of gives it for the digest that the send gave, and the record's answer round the door is of that digest"
           [{:answer :refused :reason :permission-does-not-stand} [:refused :permission-does-not-stand (:digest (:result sent))]]
           [(dissoc answer :stamp) [(:verdict round-the-door) (:reason round-the-door) (:digest round-the-door)]])
    (check "no fact of it is in the tables: no gate holds a row at its place, on any task" [] (rows-at :alice :m-1 :text))
    (check "a value act into a layer that is not made: the thing goes to the depot that the layer would start with, one record more in its log, and a gate refuses it under its own act-id with its word"
           [nil :gate-ran [1 0] {:answer :refused :reason :layer-not-made}]
           [(:could-not (:sent not-made)) (:ack (:sent not-made)) (mapv - logs-after-2 logs-before-2) (dissoc (:answer not-made) :stamp)])
    (check "the log holds that act too, as its maker gave it, with its value plain, and with the door's three additions: one record, which without its time is the act with :act-id and :depot, and whose time is between the proof's two readings round the call; and no gate holds a row at its place"
           [[[(assoc nowheres-act :act-id not-made-id :depot :streaming) true]] []]
           [(when not-made-id (seen-in-log not-made-id clock-before-2 clock-after-2)) (rows-at :nowhere :n-1 :text)])
    (shown "a value act of a who under a permission that was never given goes in with" (:appends sent) "append and" (:queries sent)
           "queries, and is refused under its own act-id:" (pr-str (dissoc answer :stamp))
           "| its value is plain in the log, and no row is at its place. The proof counted the calls by standing in for Rama's two client calls with functions that count and then call Rama.")
    (shown "a value act into a layer that is not made goes in and is refused under its own act-id:" (told not-made))))

(deftest a-holder-of-see
  (let [d (the-door)
        veras [:vera :alice :alice (own :alice) :see]
        gave (decide! d (act-of :alice [{:e :vera :k :softland.store/permission :v veras}]))
        id (act-id-for! d :alice)
        veras-act {:version 1 :who :vera :layer :alice :permission veras :session :door-proof
                   :facts [(value :v-1 "a value of a holder of see")]}
        logs-before (log-counts)
        clock-before (clock)
        sent (send! d veras-act id)
        clock-after (clock)
        logs-after (log-counts)
        answer (answer! d id (:digest sent))
        round-the-door (answer-round id)
        no-value (decide! d {:version 1 :who :vera :layer :alice :permission veras :session :door-proof
                             :facts [{:e :v-1 :k :text}]})]
    (check "Alice gives, through the door, a permission in her layer that says see: a fact under a store key, and it is accepted"
           true (accepted? gave))
    (check "the holder of see sends a value: the thing goes in under the act-id that she gave back, one record more in the streaming depot's log, and the log holds her act as she gave it, with its value plain: one record, which without its time is her act with :act-id and :depot, and whose time is a time of the door's clock between the proof's two readings round the send"
           [nil :gate-ran [1 0] [[(assoc veras-act :act-id id :depot :streaming) true]]]
           [(:could-not sent) (:ack sent) (mapv - logs-after logs-before) (seen-in-log id clock-before clock-after)])
    (check "a gate refuses it under its own act-id with its word, an act needs act: answer-of gives the refusal for the digest that the send gave, and the record's answer round the door is of that digest"
           [{:answer :refused :reason :permission-does-not-allow-acting} [:refused :permission-does-not-allow-acting (:digest sent)]]
           [(dissoc answer :stamp) [(:verdict round-the-door) (:reason round-the-door) (:digest round-the-door)]])
    (check "no fact of it is in the tables: no gate holds a row at its place, on any task" [] (rows-at :alice :v-1 :text))
    (check "an act of hers with no value is refused the same way, under its own act-id with the same word"
           [nil {:answer :refused :reason :permission-does-not-allow-acting}]
           [(:could-not (:sent no-value)) (dissoc (:answer no-value) :stamp)])
    (shown "a holder of a permission that says see sends a value through the door: the thing goes in and is refused under its own act-id:"
           (pr-str (dissoc answer :stamp)) "| its value is plain in the log, and no row is at its place")
    (shown "her act with no value goes in and is refused under its own act-id:" (told no-value))))

(deftest reads-that-throw
  (let [d (the-door)
        logs-before (log-counts)
        ;; the read of a layer's depot
        depot-word (with-read-thrown 1 #(act-id-for! d :alice))
        depot-word-in-send (with-read-thrown 1 #(send! d (act-of :alice [(value :read-thrown-1 "a value")])))
        series-word (with-read-thrown 1 #(noted (door/act-ids-for d :alice 3)))
        ;; the read of an answer
        making (:making @kept)
        answer-word (with-read-thrown 2 #(answer! d (:act-id making) (:digest making) 600))
        logs-after (log-counts)
        ;; a send whose act-id is given makes no read
        given (act-id-for! d :alice)
        no-read (with-read-thrown 1 #(send! d (act-of :alice [(value :read-thrown-2 "a value")]) given))
        no-read-answer (when-not (:could-not no-read) (answer! d given (:digest no-read)))]
    (check "the read of a layer's depot is made to throw: the word, from act-id-for, from a send of a value that makes its act-id, and from act-ids-for"
           (vec (repeat 3 [:read-the-layers-depot :nothing :alice "clojure.lang.ExceptionInfo"]))
           (mapv (juxt :could-not :appended :layer :threw) [depot-word depot-word-in-send series-word]))
    (check "the read of an answer is made to throw: the word, and none of the four answers"
           [:read-the-answer :nothing (:act-id making) nil] [(:could-not answer-word) (:appended answer-word) (:act-id answer-word) (:answer answer-word)])
    (check "nothing was appended by those four calls, and no row is at the place of the value whose send ended in the word"
           [logs-before []] [logs-after (rows-at :alice :read-thrown-1 :text)])
    (check "a send of a value whose act-id is given makes no read, so the read that is made to throw does not meet it: the thing goes in and is accepted"
           [nil :gate-ran :accepted] [(:could-not no-read) (:ack no-read) (:answer no-read-answer)])
    (shown "two reads that the proof made throw itself, by standing in for Rama's client call foreign-invoke-query with a function that throws for a call with that query's count of arguments:"
           (pr-str (mapv :could-not [depot-word answer-word])) "| each says" (pr-str (:appended depot-word)) "of the act"
           "| a send of a value with its act-id given made no read and went in")))

(deftest the-openers-numbers
  (let [paced (the-paced-door)
        made? (make-layer! paced :nora)
        never (act-id-for! paced :nora)
        waited (counting #(answer! paced never "no digest" 250))
        ;; what is no number of the door's
        unread (noted (door/open (:ipc *round*) (:module *round*) {:read-every-ms 0 :no-number-of-the-door 400}))
        waited-unread (counting #(answer! unread never "no digest" 600))
        ;; a number greater than any wait that the door counts
        great (noted (door/open (:ipc *round*) (:module *round*) {:read-every-ms Long/MAX_VALUE}))
        waited-great (counting #(answer! great never "no digest" 300))
        through-great (decide! great (act-of :nora [(value :n-1 "a value")]))
        trips (fn [counted] [(:appends counted) (:queries counted)])]
    (check "the door that was opened with a read each 100 ms makes its layer" true made?)
    (check "with a read each 100 ms, a wait of 250 ms that finds nothing makes 4 queries: at once, twice more, and at its bound"
           [[0 4] {:answer :nothing-yet}] [(trips waited) (:result waited)])
    (check "a door opened with a :read-every-ms that is no integer of 1 or more, and with a name that is none of the door's: a door, which reads each 200 ms, the door's own number: a wait of 600 ms that finds nothing makes 4 queries"
           [nil [0 4] {:answer :nothing-yet}] [(:could-not unread) (trips waited-unread) (:result waited-unread)])
    (check "a door opened with a read each Long/MAX_VALUE ms, more than any wait that the door counts: a door, and no call throws; a wait of 300 ms that finds nothing makes 2 queries, at once and at its bound; and an act through it is accepted"
           [nil [0 2] {:answer :nothing-yet} true]
           [(:could-not great) (trips waited-great) (:result waited-great) (accepted? through-great)])
    (shown "a door opened with the number {:read-every-ms 100}: a wait of 250 ms that finds nothing made" (:queries waited)
           "queries | opened with {:read-every-ms 0 :no-number-of-the-door 400}, of which the door reads nothing: a wait of 600 ms made" (:queries waited-unread)
           "queries | opened with a read each Long/MAX_VALUE ms: a wait of 300 ms made" (:queries waited-great)
           "queries. The proof counted them by standing in for Rama's client call with a function that counts and then calls Rama.")))

(deftest an-append-that-threw
  (let [d (the-door)
        ;; a real throw before the thing leaves the process
        id-1 (act-id-for! d :alice)
        word (send! d (act-of :alice [{:e :note-9 :k :text}] {:note (Object.)}) id-1)
        answer (answer! d id-1 (:digest word))
        in-log (count (records-of id-1))
        ;; a throw for an append that went through
        id-2 (act-id-for! d :alice)
        [interrupted mark-set?]
        (on-a-thread
         (fn []
           (.interrupt (Thread/currentThread))
           (let [got (send! d (act-of :alice [{:e :note-10 :k :text}]) id-2)]
             [got (Thread/interrupted)])))
        arrived (loop [tries 0]
                  (let [found (records-of id-2)]
                    (if (or (seq found) (<= 200 tries))
                      found
                      (do (Thread/sleep 50) (recur (inc tries))))))
        answer-2 (answer! d id-2 (:digest interrupted) 10000)]
    (check "an act with a part that the form does not know, which holds a plain Java object: a real throw of Rama's client. The word carries the act-id and the digest, and says that the door does not know"
           [:append-cleanly :not-known id-1 true true]
           [(:could-not word) (:appended word) (:act-id word) (hex64? (:digest word)) (string? (:threw word))])
    (check "answer-of with that pair gives nothing yet" {:answer :nothing-yet} answer)
    (check "an act with no value and a given act-id, sent on a thread whose interrupt mark the proof set: Rama's client throws. The word carries the act-id and a digest, and the thread's mark is set again"
           [:append-cleanly :not-known id-2 "java.lang.InterruptedException" true true]
           [(:could-not interrupted) (:appended interrupted) (:act-id interrupted) (:threw interrupted) (hex64? (:digest interrupted)) mark-set?])
    (check "the append went through all the same: the log holds one record under that act-id, and its digest is the word's"
           [1 (:digest interrupted)] [(count arrived) (some-> (first arrived) digest/hex-of)])
    (check "answer-of with the word's pair, on a thread with no mark, gives accepted" {:answer :accepted} (dissoc answer-2 :stamp))
    (shown "an append that threw before the thing left the process:" (pr-str (select-keys word [:could-not :appended :threw]))
           "| records in the log under its act-id:" in-log "| answer-of with the word's act-id and digest:" (pr-str answer)
           "| a real throw of Rama's client")
    (shown "an append that threw and went through:" (pr-str (select-keys interrupted [:could-not :appended :threw]))
           "| the log holds" (count arrived) "record under its act-id, with the word's digest | answer-of:" (pr-str (dissoc answer-2 :stamp))
           "| the proof set the interrupt mark of its own thread before the call; the throw is Rama's client's")))

(deftest many-makers-at-once
  (let [d (the-door)
        made? (make-layer! d :crowd)
        threads 16
        acts-each 25
        values-of (fn [t i] (inc (mod (+ t i) 3)))
        entity (fn [t i j] (keyword (str "t" t "-a" i "-v" j)))
        logs-before (log-counts)
        work (mapv (fn [t]
                     (future
                       (mapv (fn [i]
                               (decide! d (act-of :crowd (mapv (fn [j] (value (entity t i j) [t i j]))
                                                               (range (values-of t i))))))
                             (range acts-each))))
                   (range threads))
        decided (vec (mapcat (fn [f] (deref f 120000 [{:sent {:could-not :no-return}}])) work))
        logs-after (log-counts)
        act-ids (keep (comp :act-id :sent) decided)
        values (reduce + (for [t (range threads) i (range acts-each)] (values-of t i)))
        in-their-rows (count (for [t (range threads) i (range acts-each) j (range (values-of t i))
                                   :when (= [:value [t i j]] (record/read-value (row-of :crowd (entity t i j) :text)))]
                               [t i j]))
        ;; eight whos at once, each on two threads
        whos (mapv (fn [i] (keyword (str "who-" i))) (range 8))
        whos-made (mapv (fn [who] (make-layer! d who)) whos)
        work-2 (mapv (fn [t]
                       (let [who (nth whos (mod t 8))]
                         (future (mapv (fn [i] (decide! d (act-of who [(value (keyword (str "t" t "-a" i)) i)]))) (range 10)))))
                     (range 16))
        decided-2 (vec (mapcat (fn [f] (deref f 120000 [{:sent {:could-not :no-return}}])) work-2))
        streaming (log-of :streaming)
        ;; the time on each of the 400 acts of the one layer, against the time in its stamp
        crowds-times (into {} (keep (fn [r] (when (and (map? r) (= :crowd (:layer r))) [(:act-id r) (time-on r)]))) streaming)
        stamps-and-times (mapv (fn [one] [(:stamp (:answer one)) (get crowds-times (:act-id (:sent one)))]) decided)
        not-before (count (filter (fn [[s t]] (and (int? s) (int? t) (<= t (stamp/time-of s)))) stamps-and-times))
        of-the-same-time (count (filter (fn [[s t]] (and (int? s) (int? t) (= t (stamp/time-of s)))) stamps-and-times))
        by-who (mapv (fn [n who]
                       [(count (filter (fn [r] (and (map? r) (= who (:who r)))) streaming))
                        (count (for [t [n (+ n 8)] i (range 10)
                                     :when (= [:value i] (record/read-value (row-of who (keyword (str "t" t "-a" i)) :text)))]
                                 [t i]))])
                     (range 8) whos)]
    (check "the layer of this check is made" true made?)
    (check "16 threads through the one door, for one who in one layer, 25 acts each of 1 to 3 values: every act is accepted"
           [(* threads acts-each) (* threads acts-each)] [(count decided) (count (filter accepted? decided))])
    (check "the log holds one record more for each of the 400 acts and no other, and the door gave 400 different act-ids"
           [[400 0] 400] [(mapv - logs-after logs-before) (count (set act-ids))])
    (check "the acts hold 799 values, and each of them reads back from the row of its own place"
           [799 799] [values in-their-rows])
    (check "each of the 400 records holds a time, and the stamp of each of the 400 answers is of a time that is not before the time on its act; the 400 stamps of the one layer are different. Two threads can read the clock in one order and reach the depot in the other, so a stamp can be of a later time than the time on its act"
           [400 400 400]
           [(count (filter int? (map second stamps-and-times))) not-before (count (set (map first stamps-and-times)))])
    (check "8 whos at once, each on 2 threads, 10 acts of one value each: every layer is made and every act is accepted"
           [(vec (repeat 8 true)) 160] [whos-made (count (filter accepted? decided-2))])
    (check "for each of the 8 whos: 20 records under her who in the log, and each of her 20 values reads back from the row of its own place"
           (vec (repeat 8 [20 20])) by-who)
    (shown "16 threads through one door for one who:" (count (filter accepted? decided)) "acts accepted with" values
           "values, of which" in-their-rows "read back from their rows | records more in the two logs:" (pr-str (mapv - logs-after logs-before))
           "| stamps of a time that is not before the time on the act:" not-before "| of them, stamps of exactly that time:" of-the-same-time)
    (shown "8 whos at once, each on 2 threads:" (count (filter accepted? decided-2)) "acts accepted; records and rows of each who:" (pr-str by-who))))

(deftest the-base-through-the-door
  (let [d (the-door)
        asked-before (asking #(act-id-for! d :base))
        before (:result asked-before)
        ;; an act for the base under an act-id with the other depot word, before the base is made
        other-id (assoc (act-id-for! d :base) 1 :streaming)
        logs-before-other (log-counts)
        other (decide! d (system-act [(value :wrong-depot-base-1 "a value")]) other-id)
        logs-after-other (log-counts)
        making {:version 1 :who :system :layer :base :permission system-own :session :door-proof
                :facts [{:e :base :k :softland.store/depot :v :microbatch}
                        {:e :system :k :softland.store/permission :v system-own}]}
        made-sent (send! d making)
        made-answer (answer! d (:act-id made-sent) (:digest made-sent) wait-ms)
        made-time (time-sent made-sent)
        ;; a value act: one read of the layer's depot and one append
        first-act (system-act [(value :doc-1 "a title")])
        logs-before (log-counts)
        clock-before (clock)
        noted-calls (noting #(send! d first-act))
        clock-after (clock)
        first-value (:result noted-calls)
        logs-after (log-counts)
        first-record (when-not (:could-not first-value) (record-of (:act-id first-value)))
        first-answer (when-not (:could-not first-value) (answer! d (:act-id first-value) (:digest first-value) wait-ms))
        _ (settle-base!)
        first-rows (rows-in :microbatch-facts :base :doc-1 :text)
        asked-again (asking #(answer! d (:act-id first-value) (:digest first-value)))
        gave (decide! d (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}]))]
    (swap! kept assoc
           :base-under-the-streaming-word (:answer other)
           :base-answers [[(:act-id made-sent) (:digest made-sent) made-answer]
                          [(:act-id first-value) (:digest first-value) first-answer]])
    (check "act-id-for the base before it is made: the base starts as a microbatch layer" [:base :microbatch] (vec (take 2 before)))
    (check "that act-id came of one read of the layer's depot, asked with the layer alone, which gave nothing: the base was not made, and the door gave the depot that the base starts with"
           [[[[:base] nil]] (first-act/depot-at-making :base)] [(:asked asked-before) (get before 1)])
    (check "an act for the base under an act-id with the streaming depot word, sent before the base is made: the thing goes to the streaming depot, one record more in its log and none in the other; the streaming gate's ack; and that gate's recorded refusal comes back, with the word of a layer of the other gate and not the word of a layer that is not made"
           [nil :gate-ran [1 0] {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:could-not (:sent other)) (:ack (:sent other)) (mapv - logs-after-other logs-before-other) (dissoc (:answer other) :stamp)])
    (check "the base's making act by the system: the microbatch depot's ack, which says only that the depot holds the thing; and a wait gives accepted"
           [:depot-holds [:base :microbatch] :accepted] [(:ack made-sent) (vec (take 2 (:act-id made-sent))) (:answer made-answer)])
    (check "the stamp of that answer holds the time that the door wrote on the making act, with a counter of zero: it is the first decision of the base in the microbatch gate's record"
           [true made-time 0] [(int? made-time) (time-in (:stamp made-answer)) (counter-in (:stamp made-answer))])
    (check "a value act into the base: the microbatch depot's ack, and one record more in its log, the act itself"
           [nil :depot-holds [0 1]]
           [(:could-not first-value) (:ack first-value) (mapv - logs-after logs-before)])
    (check "the calls of Rama's client inside that send, in their order: the read of the layer's depot, and the act's append"
           [[:query 1] [:append 2]] (:calls noted-calls))
    (check "the record in the log is the system's act with the door's three additions, its value plain: without its time it is the act with :act-id and :depot, and its time is a time of the door's clock between the proof's two readings round the send. The door writes the time on a thing for the microbatch depot as it does for the other"
           [(assoc first-act :act-id (:act-id first-value) :depot :microbatch) true]
           [(without-the-time first-record) (time-between? (time-on first-record) clock-before clock-after)])
    (check "the act's answer by a wait is accepted; the microbatch gate holds the row of its place on each task, one row, with the text of its value; and the streaming gate holds no row there on any task"
           [:accepted (vec (repeat tasks "\"a title\"")) 1 (vec (repeat tasks nil))]
           [(:answer first-answer) (mapv :value first-rows) (count (set first-rows)) (rows-in :streaming-facts :base :doc-1 :text)])
    (check "a read of that answer with no wait is one query, asked with the act-id's layer and the act-id: the routing id of an act-id of the base is its layer. The read gives the answer that the wait gave, its stamp too"
           [[[:base (:act-id first-value)]] :base first-answer]
           [(mapv first (:asked asked-again)) (record/routing-id (:act-id first-value)) (:result asked-again)])
    (check "the system gives Alice a permission in the base" true (accepted? gave))
    (shown "the base through the door: its making act" (pr-str (dissoc made-sent :digest)) "and a wait gave" (pr-str made-answer)
           "| the time that the door wrote on it:" made-time "| the time and the counter in the stamp of its answer:"
           (time-in (:stamp made-answer)) (counter-in (:stamp made-answer)))
    (shown "a value act into the base made the calls" (pr-str (:calls noted-calls))
           "of Rama's client, which the proof noted by standing in for them with functions that note and then call Rama; a wait gave" (pr-str first-answer)
           "| the value at its place in the microbatch gate's table, task by task:" (pr-str (mapv :value first-rows))
           "| a read of its answer with no wait asked the query with its routing id and its act-id, and the routing id was" (pr-str (first (ffirst (:asked asked-again)))))
    (shown "an act for the base under an act-id with the streaming depot word, sent before the base was made:" (told other))))

(deftest the-depot-of-a-layer
  (let [d (the-door)
        asked (fn [layer] (asking #(act-id-for! d layer)))
        base (asked :base)
        streaming (asked :alice)
        not-made (asked :no-such-layer)
        seen (fn [got] [(:asked got) (vec (take 2 (:result got)))])
        series (asking #(noted (door/act-ids-for d :base 3)))
        sent (asking #(send! d (system-act [(value :depot-read-doc "a value")])))
        sent-answer (answer! d (:act-id (:result sent)) (:digest (:result sent)) wait-ms)
        by-hand (mapv (fn [layer] (rama/foreign-invoke-query (:depot-of-layer *round*) layer)) [:base :alice :no-such-layer])]
    (swap! kept update :base-answers conj [(:act-id (:result sent)) (:digest (:result sent)) sent-answer])
    (check "act-id-for the base, which is made: one read of the layer's depot, asked with the layer alone; the read gave the microbatch depot, and the act-id names it"
           [[[[:base] :microbatch]] [:base :microbatch]] (seen base))
    (check "act-id-for a streaming layer that is made: one read, which gave the streaming depot, and the act-id names it"
           [[[[:alice] :streaming]] [:alice :streaming]] (seen streaming))
    (check "act-id-for a layer that is not made: one read, which gave nothing; the act-id names the depot that the layer starts with, which for a layer that is not the base is the streaming depot"
           [[[[:no-such-layer] nil]] [:no-such-layer :streaming] :streaming]
           (conj (seen not-made) (first-act/depot-at-making :no-such-layer)))
    (check "act-ids-for 3 in the base: one read, which gave the microbatch depot, and 3 act-ids that name it"
           [[[[:base] :microbatch]] 3 #{[:base :microbatch]}]
           [(:asked series) (count (:result series)) (set (map (fn [id] (vec (take 2 id))) (:result series)))])
    (check "a send into the base that makes its act-id: one read, of the base's depot, which gave the microbatch depot; the thing went to that depot, and its answer by a wait is accepted"
           [[[[:base] :microbatch]] [:base :microbatch] :depot-holds :accepted]
           [(:asked sent) (vec (take 2 (:act-id (:result sent)))) (:ack (:result sent)) (:answer sent-answer)])
    (check "the query of a layer's depot, called with the proof's own handle for the same three layers, gives the microbatch depot, the streaming depot, and nothing"
           [:microbatch :streaming nil] by-hand)
    (shown "the depot of a layer, as the door's read of it gave it, each read asked with the layer alone: the base" (pr-str (:asked base))
           "| a streaming layer that is made" (pr-str (:asked streaming)) "| a layer that is not made" (pr-str (:asked not-made))
           "for which the door gave" (pr-str (vec (take 2 (:result not-made))))
           "| the proof noted each read by standing in for Rama's client call with a function that calls Rama and notes what it gave")))

(deftest the-ack-of-the-base-is-no-answer
  (let [d (the-door)
        alices-act (fn [i] (base-act-of :alice alice-in-base [(value (keyword (str "alices-doc-" i)) i)]))
        ;; several acts of one who from several threads while the gate is paused
        _ (pause-base!)
        logs-before-paused (log-counts)
        work (mapv (fn [i] (future (send! d (alices-act i)))) (range 4))
        paused-sent (mapv (fn [f] (deref f 60000 {:could-not :no-return})) work)
        logs-while-paused (log-counts)
        while-paused (mapv (fn [sent] (when-not (:could-not sent) (answer! d (:act-id sent) (:digest sent)))) paused-sent)
        _ (resume-base!)
        paused-answers (mapv (fn [sent] (when-not (:could-not sent) (answer! d (:act-id sent) (:digest sent) wait-ms))) paused-sent)
        ;; one act that the gate will accept and one that it will refuse, while the gate is paused
        mallorys [:mallory :base :base system-own]
        to-accept (system-act [(value :ack-doc "a value that the gate will accept")])
        to-refuse (base-act-of :mallory mallorys [(value :mallorys-doc "a value of a who with no permission")])
        _ (pause-base!)
        two-sent (mapv (fn [act] (send! d act)) [to-accept to-refuse])
        two-paused (mapv (fn [sent] (answer! d (:act-id sent) (:digest sent))) two-sent)
        began (System/nanoTime)
        waited-paused (answer! d (:act-id (first two-sent)) (:digest (first two-sent)) 600)
        waited-ms (quot (- (System/nanoTime) began) 1000000)
        _ (resume-base!)
        two-answers (mapv (fn [sent] (answer! d (:act-id sent) (:digest sent) wait-ms)) two-sent)
        read-again (mapv (fn [sent] (vec (repeatedly 5 #(answer! d (:act-id sent) (:digest sent))))) two-sent)
        _ (settle-base!)
        accepted-rows (rows-in :microbatch-facts :base :ack-doc :text)]
    (swap! kept update :base-answers into (mapv (fn [sent answer] [(:act-id sent) (:digest sent) answer]) two-sent two-answers))
    (check "4 acts of one who sent from 4 threads while the gate is paused: each send comes back with the depot's ack while the gate is still paused, the microbatch log holds 4 records more, and each reads nothing yet"
           [[nil nil nil nil] (vec (repeat 4 :depot-holds)) [0 4] (vec (repeat 4 {:answer :nothing-yet}))]
           [(mapv :could-not paused-sent) (mapv :ack paused-sent) (mapv - logs-while-paused logs-before-paused) while-paused])
    (check "the gate goes on, and each of the 4 answers by a wait is accepted"
           [:accepted :accepted :accepted :accepted] (mapv :answer paused-answers))
    (check "an act that the gate will accept, and an act of a who under a permission that was never given, which it will refuse, sent while the gate is paused: both sends come back with the one ack of the microbatch depot, which says nothing of the decision, and each reads nothing yet"
           [[nil nil] [:depot-holds :depot-holds] [{:answer :nothing-yet} {:answer :nothing-yet}]]
           [(mapv :could-not two-sent) (mapv :ack two-sent) two-paused])
    (check "a wait of 600 ms for the first of the two, while the gate is still paused, comes back after its bound with nothing yet"
           [{:answer :nothing-yet} true] [waited-paused (<= 600 waited-ms)])
    (check "the gate goes on: a wait gives accepted for the first, and for the second the gate's recorded refusal with its word"
           [{:answer :accepted} {:answer :refused :reason :permission-does-not-stand}]
           (mapv (fn [answer] (dissoc answer :stamp)) two-answers))
    (check "a read that gave an answer gives the same answer on each later read: 5 reads more of each of the two, with no wait, give the answer that the wait gave, its stamp too"
           [(vec (repeat 5 (nth two-answers 0))) (vec (repeat 5 (nth two-answers 1)))] read-again)
    (check "the accepted value is one row on each task of the microbatch gate's table, and no gate holds a row at the place of the refused value, on any task"
           [(vec (repeat tasks "\"a value that the gate will accept\"")) 1 []]
           [(mapv :value accepted-rows) (count (set accepted-rows)) (rows-at :base :mallorys-doc :text)])
    (shown "4 acts of one who from 4 threads while the microbatch gate was paused each came back with the depot's ack and read" (pr-str (first while-paused))
           "until the gate went on, and then" (pr-str (mapv :answer paused-answers)))
    (shown "the ack of the microbatch depot is no answer: an act that the gate accepts and one that it refuses, sent while the gate was paused, came back with the acks" (pr-str (mapv :ack two-sent))
           "and read" (pr-str two-paused) "| a wait of 600 ms came back after" waited-ms "ms with" (pr-str waited-paused)
           "| when the gate went on they read" (pr-str (mapv (fn [answer] (dissoc answer :stamp)) two-answers))
           "| 5 later reads of each gave that answer again:" (pr-str (mapv (fn [answer reads] (every? (fn [r] (= answer r)) reads)) two-answers read-again))
           "| the proof paused the gate with Rama's test tool")))

(deftest two-sends-into-the-base-are-decided-in-their-order
  (let [d (the-door)
        answers-of (fn [sent] (mapv (fn [one] (answer! d (:act-id one) (:digest one) wait-ms)) sent))
        times-in (fn [answers] (mapv (comp time-in :stamp) answers))
        ;; a give, and then the first act under it
        orens [:oren :base :base system-own]
        give-ids (noted (door/act-ids-for d :base 2))
        give (system-act [{:e :oren :k :softland.store/permission :v orens}])
        orens-first (base-act-of :oren orens [(value :orens-doc "the first act under a permission that was just given")])
        give-calls (noting (fn [] [(send! d give (get give-ids 0)) (send! d orens-first (get give-ids 1))]))
        give-sent (:result give-calls)
        give-answers (answers-of give-sent)
        give-times (mapv time-sent give-sent)
        ;; an act, and then an act that names its fact to replace
        replace-ids (noted (door/act-ids-for d :base 2))
        written (system-act [(value :two-sends-doc "as first sent")])
        replacing (system-act [(assoc (value :two-sends-doc "as replaced") :replaces [(get replace-ids 0) 0])])
        replace-calls (noting (fn [] [(send! d written (get replace-ids 0)) (send! d replacing (get replace-ids 1))]))
        replace-sent (:result replace-calls)
        replace-answers (answers-of replace-sent)
        replace-times (mapv time-sent replace-sent)
        ;; 8 acts in one batch, each of which needs the rows of the one before it
        petras [:petra :base :base system-own]
        chain-ids (noted (door/act-ids-for d :base 8))
        chain-acts (into [(system-act [{:e :petra :k :softland.store/permission :v petras}])
                          (base-act-of :petra petras [(value :chain-doc 1)])]
                         (map (fn [i] (base-act-of :petra petras [(assoc (value :chain-doc i) :replaces [(get chain-ids (dec i)) 0])])))
                         (range 2 8))
        _ (pause-base!)
        chain-calls (noting (fn [] (vec (map-indexed (fn [i act] (send! d act (get chain-ids i))) chain-acts))))
        chain-sent (:result chain-calls)
        chain-paused (mapv (fn [one] (answer! d (:act-id one) (:digest one))) chain-sent)
        _ (resume-base!)
        chain-answers (answers-of chain-sent)
        chain-times (mapv time-sent chain-sent)
        _ (settle-base!)
        replaced-rows (rows-in :microbatch-facts :base :two-sends-doc :text)
        chain-rows (rows-in :microbatch-facts :base :chain-doc :text)]
    (check "a give in the base and then the first act under the given permission, sent one after the other under act-ids asked for ahead: the calls of Rama's client from the first send to the return of the second are two appends, and no read"
           [[:append 2] [:append 2]] (:calls give-calls))
    (check "both are accepted: the gate decided the give before the act that needs it, which is the order of the two sends. The two stamps rise in that order, and the time in each is the time that the door wrote on its thing: one maker sent the two one after the other, after each earlier act of the base"
           [[:accepted :accepted] true give-times]
           [(mapv :answer give-answers) (rising? (mapv :stamp give-answers)) (times-in give-answers)])
    (check "an act that writes a fact and then an act that names that fact to replace, sent the same way: two appends, and no read"
           [[:append 2] [:append 2]] (:calls replace-calls))
    (check "both are accepted: the gate decided the first before the act that replaces its fact. The two stamps rise in the order of the sends, the time in each is the time that the door wrote on its thing, and the place holds the value of the second, one row on each task"
           [[:accepted :accepted] true replace-times (vec (repeat tasks "\"as replaced\"")) 1]
           [(mapv :answer replace-answers) (rising? (mapv :stamp replace-answers)) (times-in replace-answers)
            (mapv :value replaced-rows) (count (set replaced-rows))])
    (check "8 acts of the base sent one after the other while the gate is paused, under act-ids asked for ahead: 8 appends and no read; each send comes back with the depot's ack, and each reads nothing yet"
           [(vec (repeat 8 [:append 2])) (vec (repeat 8 :depot-holds)) (vec (repeat 8 {:answer :nothing-yet}))]
           [(:calls chain-calls) (mapv :ack chain-sent) chain-paused])
    (check "the gate goes on, and each of the 8 is accepted by a bounded wait. The first is a give, the second is the first act under it, and each of the next six replaces the fact of the act before it: so each was decided with the rows that the act before it wrote"
           (vec (repeat 8 :accepted)) (mapv :answer chain-answers))
    (check "the 8 stamps rise in the order of the sends, and the time in each is the time that the door wrote on its thing"
           [true chain-times] [(rising? (mapv :stamp chain-answers)) (times-in chain-answers)])
    (check "the place that the last seven wrote holds the value of the last of them, one row on each task"
           [(vec (repeat tasks "7")) 1] [(mapv :value chain-rows) (count (set chain-rows))])
    (shown "two sends into the base with no read between them are decided in their order: a give and then the first act under it read" (pr-str (mapv :answer give-answers))
           "| an act and then an act that replaces its fact read" (pr-str (mapv :answer replace-answers))
           "| the calls of Rama's client in each pair:" (pr-str (:calls give-calls)) (pr-str (:calls replace-calls))
           "| the proof noted the calls by standing in for them with functions that note and then call Rama")
    (shown "8 acts of the base sent while the gate was paused, a give, the first act under it, and six that each replace the fact of the act before: read while the gate was paused" (pr-str (frequencies (map :answer chain-paused)))
           "| read when it went on" (pr-str (frequencies (map :answer chain-answers)))
           "| the stamps rise in the order of the sends:" (rising? (mapv :stamp chain-answers))
           "| the counters in the 8 stamps:" (pr-str (mapv (comp counter-in :stamp) chain-answers))
           "| the value at the place, task by task:" (pr-str (mapv :value chain-rows))
           "| Rama's test docs say that the batch after a pause holds each record that was appended meanwhile; the proof did not see where a batch was cut")))

(deftest how-long-an-act-of-the-base-takes
  (let [fine (noted (door/open (:ipc *round*) (:module *round*) {:read-every-ms 5}))
        timed (mapv (fn [i]
                      (let [sent (send! fine (system-act [(value (keyword (str "timed-doc-" i)) i)]))
                            returned (System/nanoTime)
                            answer (answer! fine (:act-id sent) (:digest sent) wait-ms)]
                        {:sent sent :answer answer :took-ms (quot (- (System/nanoTime) returned) 1000000)}))
                    (range 5))]
    (check "a door opened with a read each 5 ms is a door" nil (:could-not fine))
    (check "5 acts of the base through it, each sent when the one before is answered: each send comes back with the depot's ack, and a wait with the proof's bound gives accepted"
           [(vec (repeat 5 :depot-holds)) (vec (repeat 5 :accepted))]
           [(mapv (comp :ack :sent) timed) (mapv (comp :answer :answer) timed)])
    (shown "from the return of send-act to the answer of an act of the base, for 5 acts sent one by one through a door that reads each 5 ms:" (pr-str (mapv :took-ms timed))
           "ms | an order of size: one run on Rama's in-process cluster, on one machine, with every row in memory and no network; a time can be one sleep of 5 ms and one read later than the commit of the act's batch on the base's task"
           "| the proof holds these times to nothing but the bound of a wait," wait-ms "ms")))

(def ^:private kinds-of-value
  "A value of each kind that the form takes, each as [what it is, the value,
  the one text of its normal form]. Each text is written out here, or is
  put together from digits by the proof's own code; none is made by a
  function of the store. A string with half of a surrogate pair is not
  among them: such a string is not kept as its maker gave it."
  [["true" true "true"]
   ["false, which is a value and no retract" false "false"]
   ["an empty string, which is a value and no retract" "" "\"\""]
   ["a string" "a text" "\"a text\""]
   ["a string with double quotes and a backslash" "say \"hi\" \\ there" "\"say \\\"hi\\\" \\\\ there\""]
   ["a string with a newline and a tab" "one\ntwo\tthree" "\"one\\ntwo\\tthree\""]
   ["a string with U+0000" "a\u0000b" "\"a\u0000b\""]
   ["a string with a character outside the first plane" "𝄞" "\"𝄞\""]
   ["a string with both" "\u0000𝄞" "\"\u0000𝄞\""]
   ["a keyword" :plain ":plain"]
   ["a keyword with a namespace" :tool/class ":tool/class"]
   ["a UUID" #uuid "00000000-0000-4000-8000-000000000001" "#uuid \"00000000-0000-4000-8000-000000000001\""]
   ["zero" 0 "0"]
   ["the greatest long" Long/MAX_VALUE "9223372036854775807"]
   ["the least long" Long/MIN_VALUE "-9223372036854775808"]
   ["an Integer" (int 7) "7"]
   ["a Short" (short -7) "-7"]
   ["a Byte" (byte 7) "7"]
   ["a big integer inside the long range, a BigInt" 2N "2"]
   ["a big integer inside the long range, a BigInteger" (biginteger 3) "3"]
   ["a big integer above the long range, a BigInt" 9223372036854775808N "9223372036854775808N"]
   ["a big integer above the long range, a BigInteger" (biginteger 9223372036854775809N) "9223372036854775809N"]
   ["a big integer below the long range" -9223372036854775809N "-9223372036854775809N"]
   ["a double" 1.5 "1.5"]
   ["the double -0.0" -0.0 "0.0"]
   ["the least double above zero" Double/MIN_VALUE "4.9E-324"]
   ["the greatest double" Double/MAX_VALUE "1.7976931348623157E308"]
   ["a decimal with a trailing zero" 1.50M "1.5M"]
   ["a decimal that is zero with a scale" 0.00M "0M"]
   ["a decimal with an exponent of 400" 1E+400M "1E+400M"]
   ["a decimal with an exponent of -400" 1E-400M "1E-400M"]
   ["an empty vector" [] "[]"]
   ["an empty map, which is a value and no retract" {} "{}"]
   ["an empty set" #{} "#{}"]
   ["a vector with nil inside" [nil] "[nil]"]
   ["a map with nil inside" {:a nil} "{:a nil}"]
   ["a set with nil inside" #{nil} "#{nil}"]
   ["a sorted map" (sorted-map :b 2 :a 1) "{:a 1 :b 2}"]
   ["a sorted set" (sorted-set 3 1 2) "#{1 2 3}"]
   ["a subvector" (subvec [0 1 2 3] 1 3) "[1 2]"]
   ["a map whose keys are of two kinds" {1 :number "1" :string} "{\"1\" :string 1 :number}"]
   ["a nested value" {:a [1 {:b #{2.5 :c}}]} "{:a [1 {:b #{2.5 :c}}]}"]
   ["a vector of 100 entries" (vec (range 100)) (str "[" (str/join " " (range 100)) "]")]
   ["a map of 20 entries" (zipmap (map (fn [i] (keyword (str "k" i))) (range 20)) (range 20))
    (str "{" (str/join " " (map (fn [i] (str ":k" i " " i)) (sort-by str (range 20)))) "}")]
   ["a value inside 32 collections" (nth (iterate vector 7) 32)
    (str (apply str (repeat 32 "[")) 7 (apply str (repeat 32 "]")))]])

(deftest a-value-of-each-kind-through-each-depot
  (let [d (the-door)
        made? (make-layer! d :kinds)
        entity (fn [i] (keyword (str "kind-" i)))
        streaming (vec (map-indexed (fn [i [_ v _]] (decide! d (act-of :kinds [(value (entity i) v)]))) kinds-of-value))
        base-sent (vec (map-indexed (fn [i [_ v _]] (send! d (system-act [(value (entity i) v)]))) kinds-of-value))
        base-answers (mapv (fn [sent] (when-not (:could-not sent) (answer! d (:act-id sent) (:digest sent) wait-ms))) base-sent)
        _ (settle-base!)
        seen (fn [layer i answer]
               (let [rows (rows-at layer (entity i) :text)]
                 [(:answer answer) (mapv :value rows) (record/read-value (first rows))]))
        seen-in-base (fn [i answer]
                       (let [rows (rows-in :microbatch-facts :base (entity i) :text)]
                         [(:answer answer) (mapv :value rows) (count (set rows)) (record/read-value (first rows))]))
        in-the-other-table (vec (mapcat (fn [i] (remove nil? (rows-in :streaming-facts :base (entity i) :text)))
                                        (range (count kinds-of-value))))
        all (vec (map-indexed (fn [i [what v text]]
                                [what v text (seen :kinds i (:answer (nth streaming i))) (seen-in-base i (nth base-answers i))])
                              kinds-of-value))]
    (check "the layer of this check is made, and the proof holds a value of 45 kinds" [true 45] [made? (count kinds-of-value)])
    (doseq [[what v text through-streaming through-base] all]
      (check (str what ", through the streaming depot: accepted under the door's digest; one gate holds one row at its place, with the one text of the value in its normal form; a reader of the row is given the value")
             [:accepted [text] [:value v]] through-streaming)
      (check (str what ", through the microbatch depot: accepted under the door's digest; the microbatch gate holds the row of its place on each task, one row, with the one text of the value in its normal form; a reader of the row is given the value")
             [:accepted (vec (repeat tasks text)) 1 [:value v]] through-base))
    (check "no place of those values of the base has a row in the streaming gate's table, on any task" [] in-the-other-table)
    (shown "a value of each of" (count kinds-of-value) "kinds that the form takes, through the door and each depot: accepted under the door's digest through the streaming depot"
           (count (filter (fn [[_ _ _ [answer]]] (= :accepted answer)) all)) "| through the microbatch depot"
           (count (filter (fn [[_ _ _ _ [answer]]] (= :accepted answer)) all))
           "| places of the streaming layer whose one row holds the text written out in the proof:"
           (count (filter (fn [[_ _ text [_ texts]]] (= [text] texts)) all))
           "| places of the base whose row holds that text on each of the" tasks "tasks:"
           (count (filter (fn [[_ _ text _ [_ texts]]] (= (vec (repeat tasks text)) texts)) all)))
    (shown "for example" (pr-str {:b 1.50M :a [1 2N]}) "is kept as" (pr-str "{:a [1 2] :b 1.5M}")
           "| the texts of a few kinds:" (str/join " | " (map (fn [[what _ text]] (str what " " (pr-str text))) (take 12 (drop 12 kinds-of-value)))))))

(deftest an-act-id-with-the-other-depot-word
  (let [d (the-door)
        id-1 (assoc (act-id-for! d :alice) 1 :microbatch)
        no-value (decide! d (act-of :alice [{:e :wrong-depot :k :text}]) id-1)
        id-2 (assoc (act-id-for! d :alice) 1 :microbatch)
        with-value-act (act-of :alice [(value :wrong-depot-2 "a value")])
        logs-before (log-counts)
        clock-before (clock)
        with-value (decide! d with-value-act id-2)
        clock-after (clock)
        logs-after (log-counts)
        ;; a layer that is not made and that starts with the streaming depot, under the microbatch depot word
        id-3 (assoc (act-id-for! d :made-by-nobody) 1 :microbatch)
        not-made (decide! d (act-of :made-by-nobody [(value :wrong-depot-3 "a value")]) id-3)
        ;; the base, which is made, under the streaming depot word
        id-4 (assoc (act-id-for! d :base) 1 :streaming)
        base-act (system-act [(value :wrong-depot-base-2 "a value")])
        logs-before-base (log-counts)
        clock-before-base (clock)
        base-other (decide! d base-act id-4)
        clock-after-base (clock)
        logs-after-base (log-counts)
        before-the-making (:base-under-the-streaming-word @kept)]
    (check "an act of a streaming layer under an act-id that a maker gives back with the other depot word: the thing goes to the depot that the act-id names, and that gate's recorded refusal comes back"
           [:depot-holds {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:ack (:sent no-value)) (dissoc (:answer no-value) :stamp)])
    (check "the same with a value: the thing goes to the depot that the act-id names, one record more in that log and none in the layer's own, and that gate's recorded refusal comes back under the act's own act-id"
           [nil :depot-holds [0 1] {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:could-not (:sent with-value)) (:ack (:sent with-value)) (mapv - logs-after logs-before) (dissoc (:answer with-value) :stamp)])
    (check "the log of that depot holds the act as its maker gave it, with its value plain: one record, which without its time is the act with :act-id and :depot, the depot that the act-id names, and whose time is a time of the door's clock between the proof's two readings round the call; and no fact of it is in the tables: no gate holds a row at its place, on any task"
           [[[(assoc with-value-act :act-id id-2 :depot :microbatch) true]] []]
           [(seen-in-log id-2 clock-before clock-after) (rows-at :alice :wrong-depot-2 :text)])
    (check "an act into a layer that is not made and that starts with the streaming depot, under an act-id with the microbatch depot word: the microbatch gate refuses it with the same word, the word of a layer of the other gate, and not with the word of a layer that is not made; no gate holds a row at its place, on any task"
           [:depot-holds {:answer :refused :reason :layer-belongs-to-the-other-gate} []]
           [(:ack (:sent not-made)) (dissoc (:answer not-made) :stamp) (rows-at :made-by-nobody :wrong-depot-3 :text)])
    (check "an act of the base, which is made, under an act-id with the streaming depot word: the thing goes to the streaming depot, one record more in that log and none in the base's own; the streaming gate's ack; and that gate's recorded refusal comes back under the act's own act-id"
           [nil :gate-ran [1 0] {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:could-not (:sent base-other)) (:ack (:sent base-other)) (mapv - logs-after-base logs-before-base) (dissoc (:answer base-other) :stamp)])
    (check "the log of the streaming depot holds that act as its maker gave it: one record, which without its time is the act with :act-id and :depot, the depot that the act-id names, and whose time is a time of the door's clock between the proof's two readings round the call; and no gate holds a row at its place, on any task"
           [[[(assoc base-act :act-id id-4 :depot :streaming) true]] []]
           [(seen-in-log id-4 clock-before-base clock-after-base) (rows-at :base :wrong-depot-base-2 :text)])
    (check "the streaming gate gave the same word for an act of the base before the base was made: the word of an act for a layer of the other gate does not follow what the other gate has made"
           [{:answer :refused :reason :layer-belongs-to-the-other-gate} true]
           [(dissoc before-the-making :stamp) (= (dissoc before-the-making :stamp) (dissoc (:answer base-other) :stamp))])
    (shown "an act-id given back with the other depot word: an act with no value comes back" (pr-str (dissoc (:answer no-value) :stamp))
           "| an act with a value goes to the depot that the act-id names and comes back" (pr-str (dissoc (:answer with-value) :stamp))
           "under its own act-id, with no row at its place")
    (shown "the other depot word on each depot: an act of a streaming layer that is made, on the microbatch depot," (pr-str (:reason (:answer with-value)))
           "| an act into a streaming layer that is not made, on the microbatch depot," (pr-str (:reason (:answer not-made)))
           "| an act of the base on the streaming depot, before the base was made" (pr-str (:reason before-the-making))
           "and after" (pr-str (:reason (:answer base-other))))))

(deftest the-module-is-started-again
  (let [d (the-door)
        landed (:landed @kept)
        row-before (row-of :dora :note-2 :text)
        _ (rtest/update-module! (:ipc *round*) module/Store)
        answer-after (answer! d (:act-id landed) (:digest landed))
        after (decide! d (act-of :dora [(value :after-the-update "a value that is sent after the module was started again")]))
        row-after (row-of :dora :note-2 :text)
        base-before (:base-answers @kept)
        base-read-again (mapv (fn [[act-id digest _]] (answer! d act-id digest)) base-before)
        base-after (decide! d (system-act [(value :doc-after-the-update "a value")]))]
    (check "answer-of gives the same answer for an act-id and a digest from before" (:answer landed) answer-after)
    (check "the same for the base: of 5 acts of the base whose answers were read before, 4 accepted and 1 refused, one read each with no wait gives the answer that was read before, its stamp too"
           [5 {:accepted 4 :refused 1} (mapv (fn [[_ _ answer]] answer) base-before)]
           [(count base-before) (frequencies (map (fn [[_ _ answer]] (:answer answer)) base-before)) base-read-again])
    (check "a value act through the same door after it is accepted, and its row holds the text of its value"
           [true "\"a value that is sent after the module was started again\""]
           [(accepted? after) (:value (row-of :dora :after-the-update :text))])
    (check "a row that a gate wrote before is the same row after, with the text of its value"
           [true "\"one value\""] [(= row-before row-after) (:value row-after)])
    (check "an act with a value into the base through the same door is accepted after it too" true (accepted? base-after))
    (shown "the module was started again under the opened door, by Rama's module update with the unchanged module: answer-of gives"
           (pr-str answer-after) "for an act from before, and a value act after it:" (told after)
           "| answers of acts of the base from before that one read gave again, the stamp too:"
           (count (filter true? (map (fn [[_ _ answer] read] (= answer read)) base-before base-read-again))) "of" (count base-before))))

(deftest the-door-holds-nothing
  (let [d (the-door)
        act (act-of :alice [(value :through-two-doors "a value")])
        id (act-id-for! d :alice)
        sent (send! d act id)
        by-the-first (answer! d id (:digest sent))
        base-sent (send! d (system-act [(value :doc-through-two-doors "a value")]))
        ;; a second door on the module, which stands for the door of another process
        d2 (noted (door/open (:ipc *round*) (:module *round*)))
        by-the-second (counting #(answer! d2 id (:digest sent)))
        other-digest (answer! d2 id (apply str (repeat 64 "0")))
        again (send! d2 act id)
        again-answer (answer! d2 id (:digest again))
        base-by-the-second (when-not (:could-not base-sent) (answer! d2 (:act-id base-sent) (:digest base-sent) wait-ms))
        through-the-second (decide! d2 (act-of :alice [(value :after-the-second-door "a value")]))]
    (check "an act with a value that the first door sent is accepted" [true :accepted] [(hex64? (:digest sent)) (:answer by-the-first)])
    (check "a second door, opened on the module after that send, reads the answer by the act-id and the digest alone, with one query: the answer that the first door gave, its stamp too"
           [nil by-the-first [0 1]]
           [(:could-not d2) (:result by-the-second) [(:appends by-the-second) (:queries by-the-second)]])
    (check "the second door gives not mine for the same act-id with another digest: an answer is a maker's by her digest, and by nothing that a door keeps"
           {:answer :not-mine} other-digest)
    (check "the same act sent again through the second door under the same act-id has the digest that the first door gave, and reads the same answer"
           [(:digest sent) by-the-first] [(:digest again) again-answer])
    (check "an act of the base that the first door sent is read through the second, by a wait: accepted" :accepted (:answer base-by-the-second))
    (check "a value act through the second door is accepted" true (accepted? through-the-second))
    (shown "a second door, opened on the module, read the answer of an act that the first door sent, by the act-id and the digest alone:"
           (pr-str (:result by-the-second)) "with" (:queries by-the-second)
           "query | the same act sent again through it had the same digest | the proof counted the query by standing in for Rama's client call with a function that counts and then calls Rama")))

(deftest each-word-and-no-stray
  (let [given (set (keep :could-not @results))
        seven #{:find-the-module :make-an-act-id :read-the-layers-depot :read-the-act :add-its-parts :append-cleanly :read-the-answer}]
    (check "the door made no stray in the whole run: over each word of a stray, of which there is one at least, and each task of both gates"
           [true 0] [(pos? (count declaration/stray-reasons)) (strays)])
    (check "the door's words are seven, written out here, and each of them was given in this run, and no other"
           [seven seven] [(set (keys words/words)) given])
    (shown "each of the door's seven words was given:" (pr-str (sort given)) "| in" (count @results)
           "results that the doors gave the proof | strays of both gates:" (strays))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them. Later checks
  build on the layers that earlier ones made. The base is made late, and
  the checks of the base follow its making; the module is started again
  near the end, and the check over the whole run comes last."
  []
  (the-door-is-opened)
  (an-act-with-no-value)
  (round-trips-are-counted)
  (a-value-is-in-the-log-as-given)
  (the-door-and-the-hand-send-the-same-thing)
  (the-digest-is-the-gates)
  (things-that-are-no-act)
  (the-answer-back)
  (a-maker-who-may-not-act)
  (a-holder-of-see)
  (reads-that-throw)
  (the-openers-numbers)
  (an-append-that-threw)
  (many-makers-at-once)
  (the-base-through-the-door)
  (the-depot-of-a-layer)
  (the-ack-of-the-base-is-no-answer)
  (two-sends-into-the-base-are-decided-in-their-order)
  (how-long-an-act-of-the-base-takes)
  (a-value-of-each-kind-through-each-depot)
  (an-act-id-with-the-other-depot-word)
  (the-module-is-started-again)
  (the-door-holds-nothing)
  (each-word-and-no-stray))

(defn -main
  "Starts the cluster, launches the module, opens the doors, runs the proof,
  closes the cluster and exits: status 0 when each check holds. It takes no
  argument."
  [& _]
  (let [result (with-open [ipc (rtest/create-ipc)]
                 (rtest/launch-module! ipc module/Store {:tasks tasks :threads 2 :workers 2})
                 (binding [*round* (open-round ipc)]
                   (run-tests 'softland.store.door-proof)))
        {:keys [fail error]} result]
    (println)
    (println "THE PROOF, IN ONE PLACE")
    (run! println @shown-lines)
    (println (select-keys result [:test :pass :fail :error]))
    (shutdown-agents)
    (System/exit (if (zero? (+ fail error)) 0 1))))
