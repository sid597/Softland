(ns softland.store.door-proof
  "The proof of the door on Rama's in-process cluster: acts go into both
  depots through `softland.store.door`, and their answers come back. One
  command, from the repository root:

    clojure -M:store-module -m softland.store.door-proof

  It prints each thing that it showed on a line that starts with SHOWN,
  prints those lines again together at the end, and exits with a status that
  is not zero when a check fails.

  The proof is the door's one maker. Each act, each request for locks and
  each answer of this proof goes through the door's six calls. The proof
  makes no act-id, asks for no lock, seals nothing and appends nothing
  itself, with one exception: for the check that the door sends what the
  hand sends, it builds one request and one act with the hand's steps,
  written out here a second time, and appends those two things itself.

  What it reads round the door. The door gives a maker no thing, no lock id
  and no lock. So, as the module's proof does, this proof holds handles of
  its own: it reads each record of the two depots' logs, to see what the
  door appended; the locks of a request, by the query `locks-of-request`, to
  open a sealed value; the answer under a request's act-id, by the query
  `answer-by-act-id`; each lock's row, by a select of the gates' PStates on
  each task, to count the rows that seal nothing and to hold every lock's
  bytes against every record; and the strays of both gates. And it reads
  one thing of the door's own memory that no call of the door shows: the
  door's map of stocks, from the field of the door's type, to see that a
  stock has left it, that each call was counted out of its stock, and that
  a stock's seat is free. It reads no lock there.

  Where the proof stands in for a call of Rama's client. Clojure lets a
  proof put a function of its own in the place of `foreign-append!` or
  `foreign-invoke-query` for a while. The proof does so for six things,
  and each line that rests on it says so: to count the round trips of a
  call of the door, or to note the moment of each; to make a read throw,
  since no real throw of a query was found that the in-process cluster
  gives without a fault of the cluster; to give the locks of one read back
  with a way of sealing that the code does not know; to set the interrupt
  mark of its own thread at the append of one request; to make one call
  slow, the append of a request or a read of an answer, so that it outlasts
  the door's wait; and to give the answer of one request back with another
  digest. The other throws are real throws of Rama's client: a module name
  under which no module runs, a thing that holds a plain Java object, and a
  call made on a thread whose interrupt mark the proof set. The proof never
  destroys the module to make a call throw: the close of the cluster then
  throws. Two things of a maker's act are the proof's own objects: a
  `:session` whose read blocks until the proof lets it end, and a `:who`
  that throws when it is hashed.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It opens five doors on the
  module: the door of most checks; a second door whose wait for locks is
  400 ms, for the checks of that wait; one with four of its numbers given;
  one for whos that cannot be the key of a stock, which ends with one
  request in doubt and no lock; and one near the end, which stands for the
  door of a process that started again. It owns four atoms:
  the lines that it showed; each result that a door gave it; what later
  checks come back to; and the locks that the proof itself left outside
  every stock. It reads the clock to time a wait.

  Approach. The checks run in one order on one cluster, and later checks
  build on the layers that earlier ones made. Each who of the proof has a
  layer of its own, made by the system through the door, so that a check
  knows what its who's stock holds. Near the end the proof starts the module
  again in the cluster, by Rama's module update with the unchanged module;
  Rama then prints some lines of its own log, marked ERROR, that are no
  failed checks. So does a thing that holds a plain Java object.

  What it does not reach is in the folder's README."
  (:require [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.test :refer [deftest is run-tests]]
            [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [softland.store.act :as act]
            [softland.store.door :as door]
            [softland.store.door.declaration :as declaration]
            [softland.store.door.digest :as digest]
            [softland.store.door.seal :as seal]
            [softland.store.door.text :as text]
            [softland.store.door.words :as words]
            [softland.store.gate.first-act :as first-act]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.nio.charset StandardCharsets]
           [java.util UUID]
           [java.util.concurrent Semaphore]
           [softland.store.door Door]))

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

;; ------------------------------------------------------- the proof's own state

(def ^:private tasks "The number of tasks of the proof's cluster." 4)

(def ^:dynamic ^:private *round*
  "What the proof holds of the cluster, round the door: the cluster itself,
  the name of the module, the two depots, three queries, and four PStates.
  And the two doors that are opened at the start: `:door`, and
  `:short-door`, whose wait for locks is 400 ms. `-main` binds it for the
  run."
  nil)

(defrecord Thing [e k])

(defonce ^:private results (atom []))
(defonce ^:private kept (atom {}))
(defonce ^:private outside-the-stocks (atom []))

(defn- noted
  "Keeps a result that a door gave the proof, for the check that no result
  holds a run of bytes and the check that each of the door's words was
  given. Gives the result."
  [result]
  (swap! results conj result)
  result)

(defn- open-round
  "Takes the cluster. Gives the handles that the proof reads round the door
  with, and the two doors."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc             ipc
     :module          m
     :streaming-depot (rama/foreign-depot ipc m "*streaming-depot")
     :microbatch-depot (rama/foreign-depot ipc m "*microbatch-depot")
     :answer          (rama/foreign-query ipc m "answer-by-act-id")
     :locks           (rama/foreign-query ipc m "locks-of-request")
     :depot-of-layer  (rama/foreign-query ipc m "depot-of-layer")
     :streaming-locks (rama/foreign-pstate ipc m "$$streaming-locks")
     :microbatch-locks (rama/foreign-pstate ipc m "$$microbatch-locks")
     :streaming-task  (rama/foreign-pstate ipc m "$$streaming-task")
     :microbatch-task (rama/foreign-pstate ipc m "$$microbatch-task")
     :door            (noted (door/open ipc m))
     :short-door      (noted (door/open ipc m {:request-wait-ms 400}))}))

;; ------------------------------------------------------------ through the door

(defn- the-door "The door of most checks." [] (:door *round*))

(defn- the-short-door "The door whose wait for locks is 400 ms." [] (:short-door *round*))

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

(defn- request?
  "True for a record whose first fact is under the store's key for a request
  for locks: a request as the door, or the hand, sends one."
  [r]
  (and (map? r)
       (vector? (get r :facts))
       (= :softland.store/lock-request (get (first (get r :facts)) :k))))

(defn- requests-of
  "The requests for locks in the log of a depot that carry a who and a
  layer, in the order of the log."
  ([who] (requests-of who who :streaming))
  ([who layer depot-word]
   (filterv (fn [r] (and (request? r) (= who (:who r)) (= layer (:layer r)))) (log-of depot-word))))

(defn- asked
  "How many locks a request in the log asks for."
  [request]
  (:v (first (:facts request))))

(defn- answer-round
  "The answer under an act-id as the module's query gives it, read with the
  proof's own handle, or nil."
  [act-id]
  (rama/foreign-invoke-query (:answer *round*) (record/routing-id act-id) act-id))

(defn- locks-round
  "The locks of a request as the module's query gives them, read with the
  proof's own handle: a map from index to {:bytes :way}."
  [request-act-id]
  (rama/foreign-invoke-query (:locks *round*) (record/routing-id request-act-id) request-act-id 0 255))

(defn- await-round
  "Reads the answer under an act-id round the door until one is there or 30
  seconds have passed. Gives the answer, or nil."
  [act-id]
  (let [deadline (+ (System/currentTimeMillis) 30000)]
    (loop []
      (or (answer-round act-id)
          (when (< (System/currentTimeMillis) deadline)
            (Thread/sleep 50)
            (recur))))))

(defn- strays
  "How many different strays the two gates hold, over every word and every
  task."
  []
  (reduce + (for [pstate [:streaming-task :microbatch-task]
                  word declaration/stray-reasons
                  k (rtest/gen-hashing-index-keys tasks)]
              (rama/foreign-select-one [(path/keypath :strays word) (path/view count)]
                                       (get *round* pstate) {:pkey k}))))

(defn- lock-rows
  "Each lock that the two records hold, by a select of the two gates' lock
  PStates on each task: a map from [depot word, the lock's key] to the rows
  under that key. A lock of a streaming layer has one row. A lock of the
  base that seals a value can have two, on two tasks: the home's row with no
  bytes, and the whole row beside the value."
  []
  (reduce (fn [found [depot-word pstate]]
            (reduce (fn [found k]
                      (reduce (fn [found [lock-key row]]
                                (update found [depot-word lock-key] (fnil conj []) row))
                              found
                              (rama/foreign-select path/ALL (get *round* pstate) {:pkey k})))
                    found
                    (rtest/gen-hashing-index-keys tasks)))
          {}
          [[:streaming :streaming-locks] [:microbatch :microbatch-locks]]))

(defn- key-of-lock
  "The [depot word, key] of a lock, as `lock-rows` has it, from a lock id."
  [[[layer depot-word request-uuid] index]]
  [depot-word (record/lock-key layer request-uuid index)])

(defn- lock-ids-of
  "The lock ids that the facts of a record name."
  [r]
  (when (and (map? r) (sequential? (get r :facts)))
    (keep (fn [fact] (when (map? fact) (get fact :lock-id))) (get r :facts))))

(defn- locks-asked-in
  "The counts of the request facts that a record holds, at any position: of
  a request as the door sends one, and of an act that carries a request fact
  as a store-key fact of its own."
  [r]
  (when (and (map? r) (sequential? (get r :facts)))
    (keep (fn [fact] (when (and (map? fact) (= :softland.store/lock-request (get fact :k)) (int? (get fact :v)))
                       (get fact :v)))
          (get r :facts))))

(defn- wait-until-base-processed!
  "Waits until the microbatch gate has processed each record of its depot's
  log. Rama's test tool; a door has none, and reads."
  []
  (rtest/wait-for-microbatch-processed-count (:ipc *round*) (:module *round*) "microbatch-gate" (log-count :microbatch)))

(defn- pause-base! "Pauses the microbatch gate, with Rama's test tool." []
  (rtest/pause-microbatch-topology! (:ipc *round*) (:module *round*) "microbatch-gate"))

(defn- resume-base! "Lets the microbatch gate go on." []
  (rtest/resume-microbatch-topology! (:ipc *round*) (:module *round*) "microbatch-gate"))

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

(defn- timing
  "Calls f while the proof stands in for Rama's two client calls with
  functions that note the moment of each call and then call Rama. Gives
  {:result what f gave, :calls the calls in their order}, each call as
  [:append or :query, the moment of the JVM's clock at which it began, how
  many arguments it had after the handle]. What must hold is what `counting`
  asks."
  [f]
  (let [calls (atom [])
        real-append rama/foreign-append!
        real-query rama/foreign-invoke-query]
    (with-redefs [rama/foreign-append! (fn [depot & args]
                                         (swap! calls conj [:append (System/nanoTime) (count args)])
                                         (apply real-append depot args))
                  rama/foreign-invoke-query (fn [query & args]
                                              (swap! calls conj [:query (System/nanoTime) (count args)])
                                              (apply real-query query args))]
      (let [result (f)]
        {:result result :calls @calls}))))

(defn- with-read-thrown
  "Calls f while the proof stands in for Rama's client call
  `foreign-invoke-query` with a function that throws for a call with n
  arguments after the handle, and calls Rama for each other: 1 is the read
  of a layer's depot, 2 the read of an answer, 4 the read of a request's
  locks."
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

;; ------------------------------------------- the door's own memory, read round it

(defn- stocks-of
  "The door's own map of stocks, read round the door from the field of the
  door's type: a map from [layer depot who] to what the door holds for that
  stock. No call of the door shows it."
  [d]
  @(.-stocks ^Door d))

(defn- stock-told
  "What a door's map holds for the stock of one layer, depot and who, or nil
  when the map holds none: how many calls are counted in, whether the stock
  holds an open request, and whether its seat is free. No lock is read."
  [d triple]
  (when-let [held (get (stocks-of d) triple)]
    {:calls      (:calls held)
     :open?      (some? (:open @(:cell held)))
     :seat-free? (= 1 (.availablePermits ^Semaphore (:seat held)))}))

(defn- delaying
  "Calls f while the proof stands in for one of Rama's two client calls with
  a function that sleeps for `ms` and then calls Rama: for the append of a
  request for locks when `which` is :request-append, and for a read of an
  answer when it is :answer-read. Each other call goes to Rama at once."
  [which ms f]
  (let [real-append rama/foreign-append!
        real-query rama/foreign-invoke-query]
    (with-redefs [rama/foreign-append! (fn [depot data & more]
                                         (when (and (= :request-append which) (request? data)) (Thread/sleep (long ms)))
                                         (apply real-append depot data more))
                  rama/foreign-invoke-query (fn [query & args]
                                              (when (and (= :answer-read which) (= 2 (count args))) (Thread/sleep (long ms)))
                                              (apply real-query query args))]
      (f))))

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
(def ^:private bobs "Bob's permission in Alice's layer, beneath hers." [:bob :alice :alice (own :alice)])

(defn- system-act
  "The system's act into the base, as a maker gives it."
  [facts]
  {:version 1 :who :system :layer :base :permission system-own :session :door-proof :facts facts})

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

(defn- plain-bytes
  "The plain bytes that stand for a value: the UTF-8 of the one text of the
  value in its normal form."
  [v]
  (text/utf8 (act/normal-value v)))

(defn- opened
  "Opens the sealed value of a fact of a record with its lock, which the
  proof reads round the door. Gives the plain bytes as text, or nil."
  [layer fact]
  (let [[request index] (:lock-id fact)
        lock (get (locks-round request) index)]
    (when-let [^bytes plain (seal/open lock [layer (:e fact) (:k fact)] (:sealed fact))]
      (String. plain StandardCharsets/UTF_8))))

;; ---------------------------------------------------------------- the checks

(deftest the-door-is-opened
  (let [wrong (noted (door/open (:ipc *round*) "no.such/Module"))
        d (the-door)
        text (str "#<door " (:module *round*) ">")
        printed (fn [x] [(pr-str x) (str x) (str/trim (with-out-str (println x))) (str/trim (with-out-str (pprint/pprint x)))])]
    (swap! kept assoc :door-text text :printed printed)
    (check "a door opened on a name under which no module runs: the word, by a real throw of Rama's client, and no door"
           [:find-the-module :nothing "no.such/Module" true]
           [(:could-not wrong) (:appended wrong) (:module wrong) (string? (:threw wrong))])
    (check "the right name gives a door and no word; so does the same name with a number of the door's given"
           [nil nil] [(:could-not d) (:could-not (the-short-door))])
    (check "a door prints as the word door and the module's name, whatever prints it: pr, str, println, a pretty-printer"
           (vec (repeat 4 text)) (printed d))
    (check "a door that was just opened holds no lock" {:unused-locks 0 :stocks 0 :open-requests 0} (door/stock-count d))
    (shown "a door opened on a wrong module name:" (pr-str (select-keys wrong [:could-not :appended :module :threw]))
           "| a real throw of Rama's client")
    (shown "an opened door prints as" (pr-str d) "and holds" (pr-str (door/stock-count d)))))

(deftest an-act-with-no-value
  (let [d (the-door)
        before (act-id-for! d :alice)
        making (making-of :alice)
        logs-before (log-counts)
        sent (send! d making before)
        logs-after (log-counts)
        answer (answer! d (:act-id sent) (:digest sent))
        in-log (record-of before)
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
    (check "the record in the log is the maker's act with :act-id and :depot, and nothing else changed"
           (assoc making :act-id before :depot :streaming) in-log)
    (check "the digest that the maker got is the digest of the record in the log" (:digest sent) (digest/hex-of in-log))
    (check "after the making, act-id-for gives the same layer and depot, and another uuid"
           [[:alice :streaming] true] [(vec (take 2 after)) (not= (nth before 2) (nth after 2))])
    (check "act-ids-for 20: 20 different act-ids of the layer, made with one query and no append"
           [20 20 #{[:alice :streaming]} 1 0]
           [(count ids) (count (set ids)) (set (map (fn [id] (vec (take 2 id))) ids)) (:queries series) (:appends series)])
    (shown "the making act of a streaming layer through the door:" (pr-str (dissoc sent :digest)) "| answer-of:" (pr-str answer))
    (shown "act-ids-for 20 in one layer: 20 different act-ids with" (:queries series)
           "query. The proof counted the query by standing in for Rama's client call with a function that counts and then calls Rama.")))

(deftest round-trips-are-counted
  (let [d (the-door)
        made? (make-layer! d :carol)
        meets (counting #(send! d (act-of :carol [(value :c-1 "one")])))
        given-id (act-id-for! d :carol)
        held (counting #(send! d (act-of :carol [(value :c-2 "two")]) given-id))
        makes (counting #(send! d (act-of :carol [(value :c-3 "three")])))
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
    (check "each of the three sends went in" [nil nil nil] (mapv (comp :could-not :result) [meets held makes]))
    (check "a send that makes its act-id and meets a request for locks in a streaming layer: 2 appends and 3 queries" [2 3] (trips meets))
    (check "a send with a given act-id and its locks in the stock: 1 append and no query" [1 0] (trips held))
    (check "a send that makes its act-id, with its locks in the stock: 1 query and 1 append" [1 1] (trips makes))
    (check "an answer-of with no wait: 1 query, and the answer" [[0 1] :accepted] [(trips one-read) (:answer (:result one-read))])
    (check "a wait of 600 ms on an act-id that was never sent: 4 queries, nothing yet, and it comes back after the bound"
           [[0 4] {:answer :nothing-yet} true] [(trips waited) (:result waited) (<= 600 waited-ms 5000)])
    (check "a bound of zero, a bound below zero, and a bound that is no number or nil: one read each, and nothing yet"
           (vec (repeat 4 [[0 1] {:answer :nothing-yet}])) (mapv (fn [counted] [(trips counted) (:result counted)]) no-wait))
    (check "a bound greater than any wait that the door counts, as a long, a big integer, a double and infinity, and a bound that is a ratio or a fraction: no call throws, and each reads the answer that is there with one query"
           (vec (repeat 6 [[0 1] :accepted])) (mapv (fn [counted] [(trips counted) (:answer (:result counted))]) great))
    (check "answer-of a thing that is no act-id, with a bound: nothing yet, with no query, at once"
           [[0 0] {:answer :nothing-yet} true] [(trips no-id) (:result no-id) (< no-id-ms 300)])
    (shown "round trips of one call of the door, [appends queries]: a send that meets a request" (pr-str (trips meets))
           "| a send with its act-id given and its locks in the stock" (pr-str (trips held))
           "| a send that makes its act-id" (pr-str (trips makes)) "| an answer-of" (pr-str (trips one-read))
           "| a wait of 600 ms that finds nothing" (pr-str (trips waited)) "in" waited-ms
           "ms. The proof counted them by standing in for Rama's two client calls with functions that count and then call Rama.")))

(deftest locks-and-the-seal
  (let [d (the-door)
        made? (make-layer! d :dora)
        v {:b 1.50M :a [1 2N]}
        first-act (act-of :dora [(value :note-1 v)] {:session :doras-session})
        landed (decide! d first-act)
        record (record-of (:act-id (:sent landed)))
        requests (requests-of :dora)
        request (first requests)
        fact (first (:facts record))
        more (mapv (fn [i] (decide! d (act-of :dora [(value (keyword (str "n-" i)) i)]))) (range 128))
        sizes (mapv asked (requests-of :dora))
        count-before (door/stock-count d)
        mixed-facts [{:e :dan :k :softland.store/permission :v [:dan :dora :dora (own :dora)]}
                     {:e :note-1 :k :text}
                     (value :note-2 "one value")]
        mixed (decide! d (act-of :dora mixed-facts))
        count-after (door/stock-count d)
        mixed-record (record-of (:act-id (:sent mixed)))]
    (swap! kept assoc :landed {:act-id (:act-id (:sent landed)) :digest (:digest (:sent landed)) :answer (:answer landed)})
    (check "the layer of this check is made" true made?)
    (check "a person's first act with a value: one request in the log, for 4 locks, under her who, her permission, her session and her version"
           [1 {:who :dora :permission (own :dora) :session :doras-session :version 1 :layer :dora :depot :streaming}
            [{:e (nth (:act-id request) 2) :k :softland.store/lock-request :v 4}]]
           [(count requests) (select-keys request [:who :permission :session :version :layer :depot]) (:facts request)])
    (check "the act's fact in the log holds :sealed and :lock-id and no :v, and names the first lock of that request"
           [#{:e :k :sealed :lock-id} [(:act-id request) 0] true]
           [(set (keys fact)) (:lock-id fact) (bytes? (:sealed fact))])
    (check "the lock, read round the door, opens the sealed bytes at [layer entity key] to the UTF-8 of the one text of the value in its normal form"
           "{:a [1 2] :b 1.5M}" (opened :dora fact))
    (check "the first act and each of 128 more of one value are accepted" [true 128] [(accepted? landed) (count (filter accepted? more))])
    (check "as she writes 129 values, the requests in the log ask for 4, 4, 8, 16, 32, 64 and then 64" [4 4 8 16 32 64 64] sizes)
    (check "an act with a store-key fact, a retract and a value is accepted and takes one lock"
           [true 1 0] [(accepted? mixed) (- (:unused-locks count-before) (:unused-locks count-after))
                       (- (count (requests-of :dora)) (count sizes))])
    (check "in the log its store-key fact is as given, with its plain value; its retract is as given; its value is sealed"
           [(nth mixed-facts 0) (nth mixed-facts 1) #{:e :k :sealed :lock-id} "\"one value\""]
           [(nth (:facts mixed-record) 0) (nth (:facts mixed-record) 1) (set (keys (nth (:facts mixed-record) 2)))
            (opened :dora (nth (:facts mixed-record) 2))])
    (shown "a person's first act with a value, through the door:" (told landed) "| the door's request in the log asks for"
           (asked request) "locks under" (pr-str (select-keys request [:who :permission :session :version])))
    (shown "the requests in the log for 129 values of one who, one value at a time:" (pr-str sizes)
           "| the door then holds" (pr-str count-before))
    (shown "an act with a store-key fact, a retract and a value took" (- (:unused-locks count-before) (:unused-locks count-after)) "lock:" (told mixed))))

(defn- without-the-random-parts
  "Takes a record of the log. Gives it with its random parts set aside: the
  uuid of its act-id, wherever the record holds it; the uuid of the request
  in each lock id; and the sealed bytes."
  [r]
  (let [act-uuid (nth (:act-id r) 2)]
    (-> r
        (assoc :act-id (assoc (:act-id r) 2 :the-acts-uuid))
        (update :facts (fn [facts]
                         (mapv (fn [fact]
                                 (cond-> fact
                                   (= act-uuid (:e fact)) (assoc :e :the-acts-uuid)
                                   (:sealed fact) (assoc :sealed :sealed-bytes)
                                   (:lock-id fact) (update :lock-id (fn [[request index]] [(assoc request 2 :the-requests-uuid) index]))))
                               facts))))))

(deftest the-door-and-the-hand-send-the-same-thing
  (let [d (the-door)
        made? (make-layer! d :erin)
        v {:n 1 :tags #{:a}}
        facts-under (fn [act-id] [(value :note-1 v)
                                  {:e :note-2 :k :text}
                                  {:e (nth act-id 2) :k :softland.store/lock-request :v 1}])
        ;; through the door
        door-id (act-id-for! d :erin)
        by-door (decide! d (act-of :erin (facts-under door-id)) door-id)
        door-record (record-of door-id)
        ;; by the hand: the hand's steps, written out a second time
        depot-word (or (rama/foreign-invoke-query (:depot-of-layer *round*) :erin) (first-act/depot-at-making :erin))
        hand-request-id [:erin depot-word (UUID/randomUUID)]
        hand-request (merge {:act-id hand-request-id :depot depot-word}
                            (act-of :erin [{:e (nth hand-request-id 2) :k :softland.store/lock-request :v 4}]))
        _ (rama/foreign-append! (:streaming-depot *round*) hand-request :ack)
        hand-locks (locks-round hand-request-id)
        hand-id [:erin depot-word (UUID/randomUUID)]
        hand-facts (assoc (facts-under hand-id) 0
                          {:e :note-1 :k :text
                           :sealed (seal/seal (get hand-locks 0) [:erin :note-1 :text] (plain-bytes v))
                           :lock-id [hand-request-id 0]})
        hand-thing (merge {:act-id hand-id :depot depot-word} (act-of :erin hand-facts))
        _ (rama/foreign-append! (:streaming-depot *round*) hand-thing :ack)
        hand-answer (answer-round hand-id)
        hand-record (record-of hand-id)]
    (swap! outside-the-stocks conj
           {:why "the hand's request for 4 locks, of which the hand used 1" :locks 3}
           {:why "the request fact that the act through the door carried as a store-key fact" :locks 1}
           {:why "the same request fact in the hand's act" :locks 1})
    (check "the layer of this check is made" true made?)
    (check "one act with a value, a retract and a store-key fact goes in through the door and is accepted" true (accepted? by-door))
    (check "the same act by the hand is accepted, with the digest of what the hand sent"
           [:accepted (digest/hex-of hand-thing)] [(:verdict hand-answer) (:digest hand-answer)])
    (check "the two records in the log are equal when the uuid of each act-id, the request's uuid in each lock id and the sealed bytes are set aside"
           (without-the-random-parts hand-record) (without-the-random-parts door-record))
    (check "what is set aside differs: the two act-ids, the two requests, the two runs of sealed bytes"
           [false false false]
           [(= (:act-id door-record) (:act-id hand-record))
            (= (first (:lock-id (first (:facts door-record)))) (first (:lock-id (first (:facts hand-record)))))
            (= (vec (:sealed (first (:facts door-record)))) (vec (:sealed (first (:facts hand-record)))))])
    (check "each sealed value opens to the same plain bytes: the UTF-8 of the one text of the value in its normal form"
           [(String. ^bytes (plain-bytes v) StandardCharsets/UTF_8) (String. ^bytes (plain-bytes v) StandardCharsets/UTF_8)]
           [(opened :erin (first (:facts door-record))) (opened :erin (first (:facts hand-record)))])
    (shown "the door and the hand send the same thing for one act with a value, a retract and a store-key fact, but for the random parts:"
           (pr-str (without-the-random-parts door-record))
           "| both accepted. The proof appended the hand's request and the hand's act itself.")))

(deftest the-digest-is-the-gates
  (let [d (the-door)
        facts [(value :list-1 "a value") {:e :list-2 :k :text} (value :list-3 [1 2])]
        kinds [[:vector (vec facts)] [:list (apply list facts)] [:lazy-sequence (map identity facts)]]
        sent (mapv (fn [[kind given]] [kind (decide! d (act-of :alice given))]) kinds)
        seen (mapv (fn [[kind decided]]
                     (let [r (record-of (:act-id (:sent decided)))]
                       [kind
                        (:answer (:answer decided))
                        (= (:digest (:sent decided)) (digest/hex-of r))
                        (vector? (:facts r))
                        (mapv (fn [fact] (if (:sealed fact) :sealed fact)) (:facts r))]))
                   sent)]
    (check "for facts given as a vector, a list and a lazy sequence: the answer under the act-id is of the digest that send-act gave, the digest is that of the record in the log, and :facts in the log is a vector with each fact at its position"
           (mapv (fn [[kind _]] [kind :accepted true true [:sealed {:e :list-2 :k :text} :sealed]]) kinds)
           seen)
    (shown "the digest that the door gives is the gate's, for facts given as" (pr-str (mapv first kinds))
           ": each act reads accepted under its own digest, and :facts arrives as a vector")))

(deftest things-that-are-no-act
  (let [d (the-door)
        alice (fn [facts] (act-of :alice facts))
        logs-before (log-counts)
        strays-before (strays)
        said (fn [word] (select-keys word [:could-not :appended :at :most]))
        could-not (fn ([w] {:could-not w :appended :nothing}) ([w at] {:could-not w :appended :nothing :at at}))
        words-given
        [["no map" (mapv (fn [x] (said (send! d x))) [nil 7 "x" [1 2]]) (vec (repeat 4 (could-not :read-the-act)))]
         ["no :layer" (said (send! d (dissoc (alice [(value :n 1)]) :layer))) (could-not :make-an-act-id :layer)]
         ["a sorted map with string keys" (said (send! d (sorted-map "layer" :alice "facts" []))) (could-not :read-the-act :layer)]
         ["a record with a :layer" (said (send! d (assoc (->Thing :n :text) :layer :alice :facts []))) (could-not :read-the-act)]
         ["a :layer that is no id" (mapv (fn [layer] (said (send! d (assoc (alice [(value :n 1)]) :layer layer)))) ["notes" 7])
          (vec (repeat 2 (could-not :make-an-act-id :layer)))]
         ["a :layer of 257 characters" (said (send! d (assoc (alice []) :layer (keyword (apply str (repeat 256 "a")))))) (could-not :make-an-act-id :layer)]
         ["facts with no end that each hold a value" (said (send! d (alice (map (fn [i] (value (keyword (str "e" i)) i)) (range)))))
          {:could-not :seal-that-many-values :appended :nothing :most 256}]
         ["a fact that is a record, with a value" (said (send! d (alice [(assoc (->Thing :n :text) :v "x")]))) (could-not :seal-a-value [:facts 0])]
         ["a fact with a value that also holds :sealed" (said (send! d (alice [(assoc (value :n 1) :sealed (byte-array 3))]))) (could-not :add-its-parts [:facts 0 :sealed])]
         ["an act that already carries :act-id, or :depot"
          [(said (send! d (assoc (alice [(value :n 1)]) :act-id [:alice :streaming (UUID/randomUUID)])))
           (said (send! d (assoc (alice [(value :n 1)]) :depot :streaming)))]
          [(could-not :add-its-parts :act-id) (could-not :add-its-parts :depot)]]
         ["a value that is no value of the form" (mapv (fn [v] (said (send! d (alice [(value :n v)])))) [1/3 (list 1 2) Double/NaN])
          (vec (repeat 3 (could-not :seal-a-value [:facts 0 :v])))]
         ["300 facts that each hold a value" (said (send! d (alice (mapv (fn [i] (value (keyword (str "e" i)) i)) (range 300)))))
          {:could-not :seal-that-many-values :appended :nothing :most 256}]
         ["a value whose sealed bytes would be over the limit" (select-keys (send! d (alice [(value :n (apply str (repeat 65507 "a")))])) [:could-not :appended :at :most :sealed-bytes])
          {:could-not :seal-a-value :appended :nothing :at [:facts 0 :v] :most 65536 :sealed-bytes 65537}]
         ["an act-id given back that is of another layer" (said (send! d (alice [(value :n 1)]) [:carol :streaming (UUID/randomUUID)])) (could-not :make-an-act-id :act-id)]]
        logs-after-words (log-counts)
        strays-after-words (strays)
        forged-lock [[:alice :streaming (UUID/randomUUID)] 0]
        plain-in-facts {:e :plain-1 :k :text :v "a value that the door does not find"}
        goes-in
        [[":facts absent" (dissoc (alice []) :facts) :malformed]
         [":facts that is one fact map" (alice plain-in-facts) :malformed]
         [":facts that is a set" (alice #{{:e :plain-2 :k :text}}) :malformed]
         ["a fact that is a string" (alice ["a fact"]) :malformed]
         ["a fact that already carries :sealed and :lock-id, and no :v" (alice [{:e :forged :k :text :sealed (byte-array 40) :lock-id forged-lock}]) :lock-not-made-for-this-layer]
         ["a fact with a value under a store key that also holds :sealed" (alice [{:e :alice :k :softland.store/depot :v :streaming :sealed (byte-array 3)}]) :store-key-sealed]
         ["a value fact whose :e is no id" (alice [{:e "n1" :k :text :v "sealed all the same"}]) :malformed]
         ["a part that the form does not know" (assoc (alice [{:e :n :k :text}]) :note "kept plain") :unknown-part]
         ["a part that is named as the door's own words are, :could-not" (assoc (alice [{:e :n :k :text}]) :could-not :read-the-act) :unknown-part]
         ["257 facts, two with a value" (alice (into [(value :m-1 1) (value :m-2 2)] (map (fn [i] {:e (keyword (str "r" i)) :k :text}) (range 255)))) :too-many-facts]
         ["1,025 entries of :based-on" (assoc (alice [{:e :n :k :text}]) :based-on (into {} (map (fn [_] [[[:alice :streaming (UUID/randomUUID)] 0] 1])) (range 1025))) :too-many-based-on]
         ["an entity of 300 characters" (alice [{:e (keyword (apply str (repeat 299 "a"))) :k :text}]) :id-too-long]]
        alices-requests-before (count (requests-of :alice))
        gone-in (mapv (fn [[what thing reason]] [what (decide! d thing) reason]) goes-in)
        logs-after (log-counts)
        requests-more (- (count (requests-of :alice)) alices-requests-before)
        one-fact-map (record-of (:act-id (:sent (second (nth gone-in 1)))))
        makers-own-seal (:act-id (:sent (second (nth gone-in 4))))
        no-id-entity (record-of (:act-id (:sent (second (nth gone-in 6)))))]
    (swap! kept assoc :a-makers-own-seal makers-own-seal)
    (doseq [[what observed expected] words-given]
      (check (str what ": the door's word, and nothing is appended") expected observed))
    (check "after each of those words: no record more in either log, and no stray more"
           [logs-before strays-before] [logs-after-words strays-after-words])
    (doseq [[what decided reason] gone-in]
      (check (str what ": the thing goes in, and answer-of gives the gate's recorded refusal")
             [nil :refused reason] [(:could-not (:sent decided)) (:answer (:answer decided)) (:reason (:answer decided))]))
    (check "each of those things is one record more in the streaming depot's log, beside the door's own requests for locks, and none is a stray"
           [[(count goes-in) 0] strays-before]
           [(mapv - logs-after logs-after-words [requests-more 0]) (strays)])
    (check "where :facts is one fact map the door found no fact, so the value inside went plain into the depot" plain-in-facts (:facts one-fact-map))
    (check "the value of a fact whose :e is no id was sealed to the place as given, with one lock" [#{:e :k :sealed :lock-id} "n1"]
           [(set (keys (first (:facts no-id-entity)))) (:e (first (:facts no-id-entity)))])
    (shown "things that are no act, each ending in one of the door's words with nothing appended:"
           (str/join " | " (map (fn [[what observed _]] (str what " " (pr-str (if (vector? observed) (first observed) observed)))) words-given)))
    (shown "things that are no act and go in, each with the gate's recorded refusal read by answer-of:"
           (str/join " | " (map (fn [[what decided _]] (str what " " (:reason (:answer decided)))) gone-in)))))

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
        ;; not mine: a maker's own second send of one act with a value
        id-2 (act-id-for! d :alice)
        twice (act-of :alice [(value :twice "the same act")])
        send-1 (send! d twice id-2)
        send-2 (send! d twice id-2)
        answer-1 (answer! d id-2 (:digest send-1))
        answer-2 (answer! d id-2 (:digest send-2))
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
    (check "a maker's own second send of one act with a value is another thing: another digest; one of the two reads its own answer and the other reads not mine"
           [false {:answer :accepted} {:answer :not-mine}]
           [(= (:digest send-1) (:digest send-2)) (dissoc answer-1 :stamp) answer-2])
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
    (shown "a maker's own second send of one act with a value reads not mine: the two digests" (subs (:digest send-1) 0 12) (subs (:digest send-2) 0 12)
           "read" (:answer answer-1) "and" (:answer answer-2))))

(deftest a-refused-request
  (let [d (the-door)
        id (act-id-for! d :alice)
        logs-before (log-counts)
        mallorys [:mallory :alice :alice (own :alice)]
        word (send! d {:version 1 :who :mallory :layer :alice :permission mallorys :session :door-proof
                       :facts [(value :m-1 "a value of a who with no permission")]}
                    id)
        logs-after (log-counts)
        request (:request word)
        request-record (when request (record-of request))
        request-answer (when request (answer-round request))
        own-answer (answer! d id "no digest was given")
        ;; a value act into a layer that is not made
        not-made (send! d (act-of :nowhere [(value :n-1 "a value")]))
        ;; the other outcome of one faulty act: a who with locks in her stock
        doras-requests (count (requests-of :dora))
        unused-before (:unused-locks (door/stock-count d))
        faulty (decide! d (act-of :dora [(value :faulty "a value")] {:permission [:dora :dora :dora [:dan :dora :dora (own :dora)]]}))
        unused-after (:unused-locks (door/stock-count d))
        left-the-map [(stock-told d [:alice :streaming :mallory]) (stock-told d [:nowhere :streaming :nowhere])]
        stays (stock-told d [:dora :streaming :dora])]
    (check "a who with no permission in a made layer sends a value act: the word, with the request's act-id and the request's answer as answer-of gives one"
           [:have-its-request-accepted :nothing [:alice :streaming] {:answer :refused :reason :permission-does-not-stand}]
           [(:could-not word) (:appended word) (vec (take 2 request)) (dissoc (:request-answer word) :stamp)])
    (check "round the door: the request is in the log under that who and her permission, and its refusal is in the record"
           [[1 0] {:who :mallory :permission mallorys :layer :alice} [:refused :permission-does-not-stand]]
           [(mapv - logs-after logs-before) (select-keys request-record [:who :permission :layer])
            [(:verdict request-answer) (:reason request-answer)]])
    (check "nothing is under the act's own act-id: no record in the log, and nothing yet" [[] {:answer :nothing-yet}] [(records-of id) own-answer])
    (check "a value act into a layer that is not made: the door asks first, and the request is refused as an act into a layer that is not made"
           [:have-its-request-accepted :nothing [:nowhere :streaming] {:answer :refused :reason :layer-not-made}]
           [(:could-not not-made) (:appended not-made) (vec (take 2 (:request not-made))) (dissoc (:request-answer not-made) :stamp)])
    (check "the other outcome, by the state of the stock: a who with locks in her stock sends a value act under a permission that does not stand; that act goes in, and its refusal is under its own act-id"
           [nil {:answer :refused :reason :permission-does-not-stand} 1 0]
           [(:could-not (:sent faulty)) (dissoc (:answer faulty) :stamp) (- unused-before unused-after)
            (- (count (requests-of :dora)) doras-requests)])
    (check "the stock of a who whose one request was refused has left the door's memory, for both; the stock of a who that was given locks stays, with no call counted in and its seat free"
           [[nil nil] {:calls 0 :open? false :seat-free? true}] [left-the-map stays])
    (shown "a value act of a who with no permission, with an empty stock:" (pr-str (dissoc word :request)) "| nothing is recorded under the act's own act-id"
           "| the door's memory holds no stock for that who after the call; the proof read the door's map of stocks from the door's own field")
    (shown "the same fault with locks in the stock: the act goes in and is refused under its own act-id:" (told faulty)
           "| the lock that it took does not come back")))

(deftest a-request-in-doubt
  (let [;; a request that never left the process, on the door whose wait is 400 ms
        d2 (the-short-door)
        made-frank? (make-layer! d2 :frank)
        id (act-id-for! d2 :frank)
        logs-before (log-counts)
        word (send! d2 (act-of :frank [(value :f-1 "a value")] {:session (Object.)}) id)
        logs-after (log-counts)
        count-in-doubt (door/stock-count d2)
        _ (Thread/sleep 500)
        id-2 (act-id-for! d2 :frank)
        began (System/nanoTime)
        after-the-bound (counting #(send! d2 (act-of :frank [(value :f-1 "a value")]) id-2))
        took-ms (quot (- (System/nanoTime) began) 1000000)
        franks-answer (answer! d2 id-2 (:digest (:result after-the-bound)))
        franks-requests (requests-of :frank)
        franks-record (record-of id-2)
        ;; a request whose append threw and went through, on the door of most checks
        d (the-door)
        made-gina? (make-layer! d :gina)
        id-3 (act-id-for! d :gina)
        real-append rama/foreign-append!
        [interrupted mark-set?]
        (on-a-thread
         (fn []
           (let [got (with-redefs [rama/foreign-append!
                                   (fn [depot data & more]
                                     (when (request? data) (.interrupt (Thread/currentThread)))
                                     (apply real-append depot data more))]
                       (send! d (act-of :gina [(value :g-1 "a value")]) id-3))]
             [got (Thread/interrupted)])))
        count-gina (door/stock-count d)
        id-4 (act-id-for! d :gina)
        next-send (counting #(send! d (act-of :gina [(value :g-1 "a value")]) id-4))
        ginas-answer (answer! d id-4 (:digest (:result next-send)))
        ginas-requests (requests-of :gina)
        ginas-record (record-of id-4)]
    (check "the layers of this check are made" [true true] [made-frank? made-gina?])
    (check "a value act whose :session is a plain Java object, with an empty stock: the request carries that session, and Rama's client throws. The word says so of the request, and nothing is appended for the act"
           [:append-its-request-cleanly :nothing [:frank :streaming] true [0 0]]
           [(:could-not word) (:appended word) (vec (take 2 (:request word))) (string? (:threw word)) (mapv - logs-after logs-before)])
    (check "the door remembers that request as open" 1 (:open-requests count-in-doubt))
    (check "after longer than the door's wait, the next send of that who makes one read of the request in doubt, forgets it, asks anew and goes in: 2 appends and 3 queries, in less than the wait"
           [nil 2 3 true {:answer :accepted}]
           [(:could-not (:result after-the-bound)) (:appends after-the-bound) (:queries after-the-bound) (< took-ms 400)
            (dissoc franks-answer :stamp)])
    (check "the log holds one request of that who, the new one, and the act names it; the door holds no open request"
           [1 true false 0]
           [(count franks-requests)
            (= (:act-id (first franks-requests)) (first (:lock-id (first (:facts franks-record)))))
            (= (:request word) (:act-id (first franks-requests)))
            (:open-requests (door/stock-count d2))])
    (check "a request whose append threw and went through: the word, with the class of the throw, and the thread's interrupt mark is set again"
           [:append-its-request-cleanly :nothing "java.lang.InterruptedException" true]
           [(:could-not interrupted) (:appended interrupted) (:threw interrupted) mark-set?])
    (check "the door remembers that request too" 1 (:open-requests count-gina))
    (check "the next send of that who reads the request's answer and uses its locks: one append, for the act alone, and no second request"
           [nil 1 true {:answer :accepted}]
           [(:could-not (:result next-send)) (:appends next-send) (<= 2 (:queries next-send)) (dissoc ginas-answer :stamp)])
    (check "the log holds one request of that who, the one of the word; the act names its first lock; no lock of it is left unread"
           [1 (:request interrupted) [(:request interrupted) 0] 0]
           [(count ginas-requests) (:act-id (first ginas-requests)) (:lock-id (first (:facts ginas-record)))
            (:open-requests (door/stock-count d))])
    (shown "a request for locks whose append threw before it left the process (a real throw of Rama's client, for a :session that is a plain Java object):"
           (pr-str (select-keys word [:could-not :appended :threw])) "| after" 500 "ms, longer than that door's wait of 400 ms, the next send made"
           (:queries after-the-bound) "queries and" (:appends after-the-bound) "appends and went in, in" took-ms
           "ms. The proof counted them by standing in for Rama's two client calls with functions that count and then call Rama.")
    (shown "a request for locks whose append threw and went through:" (pr-str (select-keys interrupted [:could-not :appended :threw]))
           "| the next send read its answer and used its locks, with" (:appends next-send) "append and" (:queries next-send)
           "queries. The proof interrupted its own thread at the request's append, by standing in for Rama's client call with a function that sets the thread's mark and then calls Rama.")))

(deftest reads-that-throw
  (let [d (the-door)
        logs-before (log-counts)
        ;; the read of a layer's depot
        depot-word (with-read-thrown 1 #(act-id-for! d :alice))
        depot-word-in-send (with-read-thrown 1 #(send! d (act-of :alice [{:e :n :k :text}])))
        series-word (with-read-thrown 1 #(noted (door/act-ids-for d :alice 3)))
        ;; the read of an answer
        making (:making @kept)
        answer-word (with-read-thrown 2 #(answer! d (:act-id making) (:digest making) 600))
        logs-after-two (log-counts)
        ;; the read of a request's answer, and then the short read of an earlier call's request
        made-hana? (make-layer! d :hana)
        id-1 (act-id-for! d :hana)
        hana-word (with-read-thrown 2 #(send! d (act-of :hana [(value :h-0 0)]) id-1))
        count-hana (door/stock-count d)
        hana-stays (stock-told d [:hana :streaming :hana])
        hanas-requests-1 (requests-of :hana)
        id-2 (act-id-for! d :hana)
        ten (decide! d (act-of :hana (mapv (fn [i] (value (keyword (str "h-" i)) i)) (range 1 11))) id-2)
        hanas-requests-2 (requests-of :hana)
        ten-record (record-of id-2)
        ;; the read of a request's locks
        made-ivan? (make-layer! d :ivan)
        id-3 (act-id-for! d :ivan)
        ivan-word (with-read-thrown 4 #(send! d (act-of :ivan [(value :i-1 "a value")]) id-3))
        count-ivan (door/stock-count d)
        id-4 (act-id-for! d :ivan)
        ivan-next (counting #(send! d (act-of :ivan [(value :i-1 "a value")]) id-4))
        ivans-answer (answer! d id-4 (:digest (:result ivan-next)))
        ivans-requests (requests-of :ivan)
        ivans-record (record-of id-4)
        ;; a read of the call's own request that gives locks that cannot seal
        made-judy? (make-layer! d :judy)
        id-5 (act-id-for! d :judy)
        unused-before (:unused-locks (door/stock-count d))
        real-query rama/foreign-invoke-query
        judy-word (with-redefs [rama/foreign-invoke-query
                                (fn [query & args]
                                  (let [got (apply real-query query args)]
                                    (if (= 4 (count args))
                                      (into {} (map (fn [[index lock]] [index (assoc lock :way 99)])) got)
                                      got)))]
                    (send! d (act-of :judy [(value :j-1 "a value")]) id-5))
        count-judy (door/stock-count d)
        judy-next (decide! d (act-of :judy [(value :j-1 "a value")]))
        judys-requests (requests-of :judy)
        judys-record (record-of (:act-id (:sent judy-next)))
        ;; the refusal of a request that an earlier call left is handed to nobody
        strangers [:stranger :alice :alice (own :alice)]
        strangers-act {:version 1 :who :stranger :layer :alice :permission strangers :session :door-proof
                       :facts [(value :s-1 "a value of a who with no permission")]}
        stranger-first (with-read-thrown 2 #(send! d strangers-act))
        stranger-next (send! d strangers-act)
        strangers-requests (requests-of :stranger :alice :streaming)
        count-stranger (door/stock-count d)]
    (swap! outside-the-stocks conj
           {:why "a request whose locks came back with a way of sealing that the code does not know, so the door gave them up" :locks (asked (first judys-requests))})
    (check "the layers of this check are made" [true true true] [made-hana? made-ivan? made-judy?])
    (check "the read of a layer's depot is made to throw: the word, from act-id-for, from a send that makes its act-id, and from act-ids-for"
           (vec (repeat 3 [:read-the-layers-depot :nothing :alice "clojure.lang.ExceptionInfo"]))
           (mapv (juxt :could-not :appended :layer :threw) [depot-word depot-word-in-send series-word]))
    (check "the read of an answer is made to throw: the word, and none of the four answers"
           [:read-the-answer :nothing (:act-id making) nil] [(:could-not answer-word) (:appended answer-word) (:act-id answer-word) (:answer answer-word)])
    (check "nothing was appended by those four calls" logs-before logs-after-two)
    (check "the read of a request's answer is made to throw: the word, nothing appended for the act, and the request stays open"
           [:read-its-requests-answer :nothing [:hana :streaming] 1 [4] []]
           [(:could-not hana-word) (:appended hana-word) (vec (take 2 (:request hana-word))) (:open-requests count-hana)
            (mapv asked hanas-requests-1) (records-of id-1)])
    (check "the stock that holds that open request stays in the door's memory, with no call counted in and its seat free"
           {:calls 0 :open? true :seat-free? true} hana-stays)
    (check "a send of 10 values of that who then reads that request, of 4 locks, asks for its own in the same call and goes in: two requests in the log, for 4 and for 10"
           [true [4 10] 0] [(accepted? ten) (mapv asked hanas-requests-2) (:open-requests (door/stock-count d))])
    (check "and the act names the locks of both requests, as neighbours: the 4 of the first, then the first 6 of the second"
           (into (mapv (fn [i] [(:act-id (nth hanas-requests-2 0)) i]) (range 4))
                 (mapv (fn [i] [(:act-id (nth hanas-requests-2 1)) i]) (range 6)))
           (mapv :lock-id (:facts ten-record)))
    (check "the read of a request's locks is made to throw: the word carries the class of the throw and no message, and the request stays open"
           [:read-its-requests-locks :nothing "clojure.lang.ExceptionInfo" false 1]
           [(:could-not ivan-word) (:appended ivan-word) (:threw ivan-word) (contains? ivan-word :said) (:open-requests count-ivan)])
    (check "the next send of that who reads the request's answer again and then its locks, 2 queries, and sends no second request: 1 append"
           [nil 1 2 {:answer :accepted} 1 [(:request ivan-word) 0]]
           [(:could-not (:result ivan-next)) (:appends ivan-next) (:queries ivan-next) (dissoc ivans-answer :stamp)
            (count ivans-requests) (:lock-id (first (:facts ivans-record)))])
    (check "a read of the call's own request that gives locks that cannot seal: the word, with no throw; the door's count holds none of them, and the request is ended"
           [:read-its-requests-locks :nothing false unused-before 0]
           [(:could-not judy-word) (:appended judy-word) (contains? judy-word :threw) (:unused-locks count-judy) (:open-requests count-judy)])
    (check "the send after it asks anew and goes in: two requests of that who in the log, and the act names the second"
           [true 2 (:act-id (second judys-requests))]
           [(accepted? judy-next) (count judys-requests) (first (:lock-id (first (:facts judys-record))))])
    (check "a request of a who with no permission is left open by a read of its answer that the proof made throw. The next send of that who reads that request, which was refused, hands that refusal to nobody, sends its own request, and is handed the refusal of its own"
           [:read-its-requests-answer :have-its-request-accepted 2 true true {:answer :refused :reason :permission-does-not-stand} 0]
           [(:could-not stranger-first) (:could-not stranger-next) (count strangers-requests)
            (= (:request stranger-first) (:act-id (first strangers-requests)))
            (= (:request stranger-next) (:act-id (second strangers-requests)))
            (dissoc (:request-answer stranger-next) :stamp)
            (:open-requests count-stranger)])
    (shown "four reads that the proof made throw itself, by standing in for Rama's client call foreign-invoke-query with a function that throws for a call with that query's count of arguments:"
           (pr-str (mapv :could-not [depot-word answer-word hana-word ivan-word])) "| each says" (pr-str (:appended depot-word)) "of the act")
    (shown "after a read of a request's answer that threw, a send of 10 values read that request of 4 locks and asked for its own in the same call: requests"
           (pr-str (mapv asked hanas-requests-2)) "| after a read of locks that threw, the next send made" (:queries ivan-next) "queries and" (:appends ivan-next)
           "append, which the proof counted by standing in for Rama's two client calls with functions that count and then call Rama"
           "| a refused request that an earlier call left open was handed to nobody: the next send of that who was handed the refusal of its own request,"
           (pr-str (dissoc (:request-answer stranger-next) :stamp)))
    (shown "locks that cannot seal:" (pr-str (dissoc judy-word :request))
           "| the door holds none of them. The proof gave each lock of that read back with a way of sealing that the code does not know, by standing in for Rama's client call with a function that calls Rama and changes what it gave.")))

(deftest the-seat-and-the-wait
  (let [d2 (the-short-door)
        ;; a part of the act that does not come back when it is read
        made-olga? (make-layer! d2 :olga)
        blocker (promise)
        stuck (future (send! d2 (act-of :olga [(value :o-1 "a value")] {:session (lazy-seq @blocker)})))
        _ (Thread/sleep 150)
        while-stuck (stock-told d2 [:olga :streaming :olga])
        other (decide! d2 (act-of :olga [(value :o-2 "a value")]))
        olgas-requests (count (requests-of :olga))
        _ (deliver blocker nil)
        came-back (deref stuck 30000 {:could-not :no-return})
        after-olga (stock-told d2 [:olga :streaming :olga])
        ;; the append of a request that outlasts the door's wait
        made-pia? (make-layer! d2 :pia)
        id-1 (act-id-for! d2 :pia)
        id-w (act-id-for! d2 :pia)
        slow-maker (future (timing #(delaying :request-append 600 (fn [] (send! d2 (act-of :pia [(value :p-1 "a value")]) id-1)))))
        _ (Thread/sleep 50)
        began-w (System/nanoTime)
        waiter (send! d2 (act-of :pia [(value :p-w "a value")]) id-w)
        waiter-ms (quot (- (System/nanoTime) began-w) 1000000)
        slow (deref slow-maker 30000 {:result {:could-not :no-return} :calls []})
        slow-word (:result slow)
        pia-open (stock-told d2 [:pia :streaming :pia])
        id-2 (act-id-for! d2 :pia)
        pia-next (counting #(send! d2 (act-of :pia [(value :p-1 "a value")]) id-2))
        pias-answer (answer! d2 id-2 (:digest (:result pia-next)))
        pias-requests (requests-of :pia)
        pias-record (record-of id-2)
        ;; a wait that ends after the call read an earlier call's request to its end
        strangers [:stranger-2 :pia :pia (own :pia)]
        strangers-act (fn [e] {:version 1 :who :stranger-2 :layer :pia :permission strangers :session :door-proof
                               :facts [(value e "a value of a who with no permission")]})
        left-open (with-read-thrown 2 #(send! d2 (strangers-act :s-1)))
        open-before (stock-told d2 [:pia :streaming :stranger-2])
        late (counting #(delaying :answer-read 600 (fn [] (send! d2 (strangers-act :s-2)))))
        late-word (:result late)
        open-after (stock-told d2 [:pia :streaming :stranger-2])
        own-refusal (send! d2 (strangers-act :s-3))
        strangers-requests (requests-of :stranger-2 :pia :streaming)
        ;; a request in doubt whose append truly failed, met before the door's wait has passed since the throw
        made-sam? (make-layer! d2 :sam)
        sams-word (send! d2 (act-of :sam [(value :s-1 "a value")] {:session (Object.)}))
        _ (Thread/sleep 200)
        began (System/nanoTime)
        sams-next (counting #(send! d2 (act-of :sam [(value :s-1 "a value")])))
        sams-ms (quot (- (System/nanoTime) began) 1000000)
        sams-answer (when-not (:could-not (:result sams-next))
                      (answer! d2 (:act-id (:result sams-next)) (:digest (:result sams-next))))]
    (check "the layers of this check are made" [true true true] [made-olga? made-pia? made-sam?])
    (check "a value act whose :session does not come back when it is read, with an empty stock: its call is counted into the stock and holds no seat"
           {:calls 1 :open? false :seat-free? true} while-stuck)
    (check "another maker of that who and layer meanwhile is not held by it: the door asks for locks for it, and its act is accepted"
           [true 1] [(accepted? other) olgas-requests])
    (check "when the proof lets that read end, the first call comes back; no call is counted into the stock and its seat is free"
           [true {:calls 0 :open? false :seat-free? true}] [(not= :no-return (:could-not came-back)) after-olga])
    (check "the append of a request that the proof made take 600 ms, on the door whose wait is 400 ms: when it returns the wait has ended, and the door starts no step more: the one call of Rama's client in that send is the append, and no read follows it"
           [[:append 2]] (mapv (fn [[kind _ arguments]] [kind arguments]) (:calls slow)))
    (check "a second maker of that who, 50 ms later, waits for the stock's seat while the first is inside that append: its own wait ends first, at 400 ms and before the first maker's append returns. The word names no request, and that call made no call of Rama's client"
           [:get-locks-within-its-wait :nothing false true true []]
           [(:could-not waiter) (:appended waiter) (contains? waiter :request) (<= 395 (:waited-ms waiter 0) 540) (< waiter-ms 540)
            (records-of id-w)])
    (check "the word names the request, which its depot holds, and the request stays open with its stock's seat free"
           [:get-locks-within-its-wait :nothing [:pia :streaming] true {:calls 0 :open? true :seat-free? true}]
           [(:could-not slow-word) (:appended slow-word) (vec (take 2 (:request slow-word))) (<= 600 (:waited-ms slow-word 0)) pia-open])
    (check "the next send of that who reads that request's answer and then its locks and sends no second request: 1 append and 2 queries, and its act names the request of the word"
           [nil 1 2 {:answer :accepted} 1 [(:request slow-word) 0]]
           [(:could-not (:result pia-next)) (:appends pia-next) (:queries pia-next) (dissoc pias-answer :stamp)
            (count pias-requests) (:lock-id (first (:facts pias-record)))])
    (check "a refused request that an earlier call left open, and a next send whose read of its answer the proof made take 600 ms: the call reads the request to its end, and its wait has then ended before it could send its own. The word names no request, since none of this call stays open, and the stock has left the door's memory"
           [:read-its-requests-answer {:calls 0 :open? true :seat-free? true}
            :get-locks-within-its-wait :nothing false true [0 2] nil]
           [(:could-not left-open) open-before
            (:could-not late-word) (:appended late-word) (contains? late-word :request) (<= 600 (:waited-ms late-word 0))
            [(:appends late) (:queries late)] open-after])
    (check "the send after it asks for itself and is handed the refusal of its own request: two requests of that who in the log"
           [:have-its-request-accepted {:answer :refused :reason :permission-does-not-stand} 2 true]
           [(:could-not own-refusal) (dissoc (:request-answer own-refusal) :stamp) (count strangers-requests)
            (= (:request own-refusal) (:act-id (second strangers-requests)))])
    (check "a request whose append threw before it left the process, met by the next send 200 ms later, on the door whose wait is 400 ms: that send reads the request in doubt until 400 ms have passed since the throw, forgets it, asks anew inside its own wait and goes in"
           [:append-its-request-cleanly nil 2 true true {:answer :accepted} 1 {:calls 0 :open? false :seat-free? true}]
           [(:could-not sams-word) (:could-not (:result sams-next)) (:appends sams-next) (<= 5 (:queries sams-next))
            (<= 150 sams-ms 3000) (dissoc sams-answer :stamp) (count (requests-of :sam)) (stock-told d2 [:sam :streaming :sam])])
    (shown "a value act whose :session does not come back when it is read held no seat:" (pr-str while-stuck)
           "| another maker of that who went in meanwhile:" (told other)
           "| the proof made that read block and then let it end, and read the door's map of stocks from the door's own field")
    (shown "an append of a request that outlasted the door's wait of 400 ms:" (pr-str (select-keys slow-word [:could-not :appended :waited-ms]))
           "| calls of Rama's client in that send:" (pr-str (mapv first (:calls slow))) "| the next send used that request with" (:appends pia-next)
           "append and" (:queries pia-next) "queries. The proof made the append slow by standing in for Rama's client call with a function that sleeps and then calls Rama.")
    (shown "a wait that ended after the call had read an earlier call's refused request to its end:" (pr-str (select-keys late-word [:could-not :appended :waited-ms :request]))
           "| the send after it:" (pr-str (dissoc (:request-answer own-refusal) :stamp))
           "| the proof made the read of the answer slow by standing in for Rama's client call with a function that sleeps and then calls Rama.")
    (shown "a request in doubt whose append truly failed, met 200 ms after the throw: the next send went in after" sams-ms "ms with" (:appends sams-next)
           "appends and" (:queries sams-next) "queries, which the proof counted by standing in for Rama's two client calls with functions that count and then call Rama")))

(deftest a-requests-answer-with-another-digest
  (let [d (the-door)
        made? (make-layer! d :quinn)
        id (act-id-for! d :quinn)
        real-query rama/foreign-invoke-query
        reads-of-locks (atom 0)
        word (with-redefs [rama/foreign-invoke-query
                           (fn [query & args]
                             (when (= 4 (count args)) (swap! reads-of-locks inc))
                             (let [got (apply real-query query args)]
                               (if (and (= 2 (count args)) (map? got))
                                 (assoc got :digest (apply str (repeat 64 "0")))
                                 got)))]
               (send! d (act-of :quinn [(value :q-1 "a value")]) id))
        told-after (stock-told d [:quinn :streaming :quinn])
        request-round (when (:request word) (answer-round (:request word)))
        locks-made (when (:request word) (count (locks-round (:request word))))
        next-send (decide! d (act-of :quinn [(value :q-1 "a value")]))
        quinns-requests (requests-of :quinn)
        record (record-of (:act-id (:sent next-send)))]
    (swap! outside-the-stocks conj
           {:why "a request whose answer the proof gave back with another digest, so the door read none of its locks" :locks (or locks-made 0)})
    (check "the layer of this check is made" true made?)
    (check "the answer under the door's own request comes back with a digest that is not the digest of the request as sent: the door does not take it for its own. The word, with the request's answer as not mine; no read of locks is made, and nothing is appended for the act"
           [:have-its-request-accepted :nothing {:answer :not-mine} 0 [] nil]
           [(:could-not word) (:appended word) (:request-answer word) @reads-of-locks (records-of id) told-after])
    (check "round the door, the record's own answer to that request is an accept, and its 4 locks were made: the door held to the digest and not to the verdict"
           [:accepted 4] [(:verdict request-round) locks-made])
    (check "the send after it asks anew and goes in: two requests of that who in the log, and the act names the second"
           [true 2 (:act-id (second quinns-requests))]
           [(accepted? next-send) (count quinns-requests) (first (:lock-id (first (:facts record))))])
    (shown "a request's answer with another digest:" (pr-str (select-keys word [:could-not :appended :request-answer]))
           "| reads of locks made:" @reads-of-locks
           "| the proof changed the digest of the answer that the read gave, by standing in for Rama's client call with a function that calls Rama and changes what it gave")))

(deftest an-act-over-three-runs
  (let [d (the-door)
        made? (make-layer! d :rosa)
        one (decide! d (act-of :rosa [(value :r-0 0)]))
        two (decide! d (act-of :rosa [(value :r-1 1) (value :r-2 2)]))
        word (with-read-thrown 2 #(send! d (act-of :rosa (mapv (fn [i] (value (keyword (str "r3-" i)) i)) (range 3)))))
        open-told (stock-told d [:rosa :streaming :rosa])
        id (act-id-for! d :rosa)
        ten (decide! d (act-of :rosa (mapv (fn [i] (value (keyword (str "r10-" i)) i)) (range 10))) id)
        requests (requests-of :rosa)
        named (mapv :lock-id (:facts (record-of id)))
        request-id (fn [i] (:act-id (nth requests i nil)))]
    (check "the layer of this check is made, and two acts of 1 and of 2 values go in from one request for 4: the stock then holds 1 lock"
           [true true true [4]] [made? (accepted? one) (accepted? two) (mapv asked (take 1 requests))])
    (check "a send of 3 values asks for 4 more, and the read of that request's answer is made to throw: the request stays open"
           [:read-its-requests-answer {:calls 0 :open? true :seat-free? true}]
           [(:could-not word) open-told])
    (check "a send of 10 values then holds 1 lock, reads the 4 of the open request, is still short and asks for 10 of its own: accepted, and the requests in the log ask for 4, 4 and 10"
           [true [4 4 10]] [(accepted? ten) (mapv asked requests)])
    (check "that act names the locks of three requests, one for each run that it crosses, each a run of neighbours: the last of the first, the 4 of the second, the first 5 of the third"
           (vec (concat [[(request-id 0) 3]]
                        (map (fn [i] [(request-id 1) i]) (range 4))
                        (map (fn [i] [(request-id 2) i]) (range 5))))
           named)
    (shown "an act that names the locks of three requests: the stock held 1 lock, the door read 4 of a request that an earlier call left open and asked for 10 more; indexes by request:"
           (pr-str (mapv (fn [ids] (mapv second ids)) (partition-by first named))) "|" (told ten))))

(deftest the-openers-numbers
  (let [d4 (noted (door/open (:ipc *round*) (:module *round*)
                             {:fewest-at-once 2 :most-at-once-streaming 3 :read-every-ms 100 :request-wait-ms Long/MAX_VALUE}))
        made? (make-layer! d4 :nora)
        sent (mapv (fn [i] (decide! d4 (act-of :nora [(value (keyword (str "n-" i)) i)]))) (range 6))
        sizes (mapv asked (requests-of :nora))
        never (act-id-for! d4 :nora)
        waited (counting #(answer! d4 never "no digest" 250))]
    (swap! kept assoc :numbers-door d4)
    (check "a door opened with four of its numbers given, one of them a wait greater than any that the door counts: a door, and its layer is made"
           [nil true] [(:could-not d4) made?])
    (check "with the fewest at once 2 and the most at once 3, six acts of one value go in and the requests in the log ask for 2, 2 and 3; the door holds the one lock that is left"
           [6 [2 2 3] {:unused-locks 1 :stocks 1 :open-requests 0}]
           [(count (filter accepted? sent)) sizes (door/stock-count d4)])
    (check "with a read each 100 ms, a wait of 250 ms that finds nothing makes 4 queries"
           [[0 4] {:answer :nothing-yet}] [[(:appends waited) (:queries waited)] (:result waited)])
    (shown "a door opened with the numbers {:fewest-at-once 2 :most-at-once-streaming 3 :read-every-ms 100} and a wait for locks of Long/MAX_VALUE ms: requests of one who for 6 values"
           (pr-str sizes) "| a wait of 250 ms that finds nothing made" (:queries waited)
           "queries, which the proof counted by standing in for Rama's client call with a function that counts and then calls Rama")))

(deftest a-who-that-cannot-be-a-key
  (let [d5 (noted (door/open (:ipc *round*) (:module *round*)))
        made? (make-layer! d5 :tess)
        logs-before (log-counts)
        no-hash (reify Object (hashCode [_] (throw (ex-info "the proof made this who throw when it is hashed" {}))))
        said (fn [word] (select-keys word [:could-not :appended :at]))
        unhashed (send! d5 (act-of :tess [(value :t-1 "a value")] {:who no-hash}))
        sorted-who (send! d5 (act-of :tess [(value :t-2 "a value")] {:who (sorted-map :a 1) :session (Object.)}))
        kept-stock (stock-told d5 [:tess :streaming (sorted-map :a 1)])
        other-map (send! d5 (act-of :tess [(value :t-3 "a value")] {:who {"a" 1}}))
        logs-after (log-counts)]
    (check "the layer of this check is made" true made?)
    (check "a value act whose :who throws when it is hashed: the door's word, at :who"
           {:could-not :read-the-act :appended :nothing :at :who} (said unhashed))
    (check "a value act whose :who is a sorted map with keyword keys, and whose request's append throws: the door keeps a stock for that who, with its request in doubt"
           [:append-its-request-cleanly {:calls 0 :open? true :seat-free? true}] [(:could-not sorted-who) kept-stock])
    (check "then a value act of that layer whose :who is a map with string keys, which throws when it is compared with the who of that stock: the door's word, at :who, and no throw out of the call"
           {:could-not :read-the-act :appended :nothing :at :who} (said other-map))
    (check "nothing was appended for the three, and that door holds no lock and the one request in doubt"
           [logs-before {:unused-locks 0 :stocks 0 :open-requests 1}] [logs-after (door/stock-count d5)])
    (shown "a :who that cannot be hashed:" (pr-str (said unhashed)) "| a :who that cannot be compared with a who that the door holds a stock for:"
           (pr-str (said other-map)) "| no call threw")))

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

(deftest a-lock-never-crosses-a-who
  (let [d (the-door)
        ;; Alice sends until one of her acts met a request: her stock then holds locks
        alices-before (count (requests-of :alice))
        filled (loop [sent 0]
                 (decide! d (act-of :alice [(value (keyword (str "fill-" sent)) sent)]))
                 (if (or (< alices-before (count (requests-of :alice))) (<= 300 sent))
                   (inc sent)
                   (recur (inc sent))))
        alices-requests (count (requests-of :alice))
        gave (decide! d (act-of :alice [{:e :bob :k :softland.store/permission :v bobs}]))
        ;; a second holder of the layer
        bobs-act {:version 1 :who :bob :layer :alice :permission bobs :session :door-proof :facts [(value :bobs-note "Bob's value")]}
        by-bob (decide! d bobs-act)
        bobs-requests (requests-of :bob :alice :streaming)
        bobs-record (record-of (:act-id (:sent by-bob)))
        by-alice (decide! d (act-of :alice [(value :alices-note "Alice's value")]))
        alices-record (record-of (:act-id (:sent by-alice)))
        alices-requests-after (requests-of :alice)
        ;; the same who in a second layer
        in-team [:alice :team :team]
        team-made (decide! d {:version 1 :who :alice :layer :team :permission in-team :session :door-proof
                              :facts [{:e :team :k :softland.store/depot :v :streaming}
                                      {:e :alice :k :softland.store/permission :v in-team}]})
        in-the-team (decide! d {:version 1 :who :alice :layer :team :permission in-team :session :door-proof
                                :facts [(value :team-note "Alice's value in the team's layer")]})
        team-requests (requests-of :alice :team :streaming)
        team-record (record-of (:act-id (:sent in-the-team)))]
    (check "Alice's stock holds locks: one of her acts has just met a request" [true true] [(< alices-before alices-requests) (< filled 300)])
    (check "Alice gives Bob a permission in her layer, and Bob's act with a value is accepted" [true true] [(accepted? gave) (accepted? by-bob)])
    (check "Bob's act is not given a lock of Alice's stock: the door asks for Bob, under his who and his permission, and his act names that request"
           [1 {:who :bob :permission bobs :layer :alice} true]
           [(count bobs-requests) (select-keys (first bobs-requests) [:who :permission :layer])
            (= (:act-id (first bobs-requests)) (first (:lock-id (first (:facts bobs-record)))))])
    (check "Alice's next act takes from her own stock: no request more of hers, and her act names a request of hers"
           [true alices-requests true]
           [(accepted? by-alice) (count alices-requests-after)
            (contains? (set (map :act-id alices-requests-after)) (first (:lock-id (first (:facts alices-record)))))])
    (check "the same who in a second layer: the door asks for locks in that layer, and the act there names that request"
           [true true 1 true]
           [(accepted? team-made) (accepted? in-the-team) (count team-requests)
            (= (:act-id (first team-requests)) (first (:lock-id (first (:facts team-record)))))])
    (shown "two holders of one layer: Bob's first value act made the door ask for locks under Bob's who while Alice's stock held locks; one who in two layers: a request in each layer."
           "Requests in the log: Alice in her layer" (count alices-requests-after) "| Bob in Alice's layer" (count bobs-requests) "| Alice in the team's layer" (count team-requests))))

(deftest many-makers-at-once
  (let [d (the-door)
        made? (make-layer! d :crowd)
        threads 16
        acts-each 25
        values-of (fn [t i] (inc (mod (+ t i) 3)))
        work (mapv (fn [t]
                     (future
                       (mapv (fn [i]
                               (decide! d (act-of :crowd (mapv (fn [j] (value (keyword (str "t" t "-a" i "-v" j)) [t i j]))
                                                               (range (values-of t i))))))
                             (range acts-each))))
                   (range threads))
        decided (vec (mapcat (fn [f] (deref f 120000 [{:sent {:could-not :no-return}}])) work))
        records (filterv (fn [r] (and (map? r) (= :crowd (:who r)) (not (request? r)) (seq (lock-ids-of r)))) (log-of :streaming))
        lock-ids (vec (mapcat lock-ids-of records))
        values (reduce + (for [t (range threads) i (range acts-each)] (values-of t i)))
        requests (mapv asked (requests-of :crowd))
        held-to-the-rule (map (fn [n before] (and (<= 4 n 64) (<= n (max 4 (reduce + before)))))
                              requests
                              (map (fn [i] (take i requests)) (range (count requests))))
        ;; eight whos at once, each on two threads
        whos (mapv (fn [i] (keyword (str "who-" i))) (range 8))
        whos-made (mapv (fn [who] (make-layer! d who)) whos)
        work-2 (mapv (fn [t]
                       (let [who (nth whos (mod t 8))]
                         (future (mapv (fn [i] (decide! d (act-of who [(value (keyword (str "t" t "-a" i)) i)]))) (range 10)))))
                     (range 16))
        decided-2 (vec (mapcat (fn [f] (deref f 120000 [{:sent {:could-not :no-return}}])) work-2))
        by-who (mapv (fn [who]
                       (let [named (mapcat lock-ids-of (filter (fn [r] (and (map? r) (= who (:who r)) (not (request? r)))) (log-of :streaming)))
                             asked-by (set (map :act-id (requests-of who)))]
                         [(count named) (count (set named)) (every? (fn [[request _]] (contains? asked-by request)) named)
                          (<= (count asked-by) 6)]))
                     whos)]
    (check "the layer of this check is made" true made?)
    (check "16 threads through the one door, for one who in one layer, 25 acts each of 1 to 3 values: every act is accepted"
           [(* threads acts-each) (* threads acts-each)] [(count decided) (count (filter accepted? decided))])
    (check "no lock id is named twice over all of them, and there is one for each value" [values values] [(count lock-ids) (count (set lock-ids))])
    (check "each act names the locks of one request, or of two" #{1 2}
           (into #{1 2} (map (fn [r] (count (set (map first (lock-ids-of r))))) records)))
    (check "each request in the log for that who is held to the size rule, by its own count: 4 at least, 64 at most, and no more than the requests before it asked for together, or 4"
           true (every? true? held-to-the-rule))
    (check "the requests cover the values, and are fewer than the acts by far: not one for each thread's act"
           [true true] [(<= values (reduce + requests)) (< (count requests) 50)])
    (check "8 whos at once, each on 2 threads, 10 acts of one value each: every layer is made and every act is accepted"
           [(vec (repeat 8 true)) 160] [whos-made (count (filter accepted? decided-2))])
    (check "for each of the 8 whos: 20 lock ids, no one twice, each of a request of that who, in 6 requests at most"
           (vec (repeat 8 [20 20 true true])) by-who)
    (shown "16 threads through one door for one who:" (count (filter accepted? decided)) "acts accepted with" values
           "values, each under a lock of its own | the requests in the log ask for" (pr-str requests)
           "| acts that name two requests:" (count (filter (fn [r] (= 2 (count (set (map first (lock-ids-of r)))))) records)))
    (shown "8 whos at once, each on 2 threads:" (count (filter accepted? decided-2)) "acts accepted; requests of each who:"
           (pr-str (mapv (fn [who] (mapv asked (requests-of who))) whos)))))

(deftest the-base-through-the-door
  (let [d (the-door)
        before (act-id-for! d :base)
        making {:version 1 :who :system :layer :base :permission system-own :session :door-proof
                :facts [{:e :base :k :softland.store/depot :v :microbatch}
                        {:e :system :k :softland.store/permission :v system-own}]}
        made-sent (send! d making)
        made-answer (answer! d (:act-id made-sent) (:digest made-sent) wait-ms)
        ;; a value act: the request is sent inside the call
        logs-before (log-counts)
        began (System/nanoTime)
        timed (timing #(send! d (system-act [(value :doc-1 "a title")])))
        first-value (:result timed)
        took-ms (quot (- (System/nanoTime) began) 1000000)
        logs-after (log-counts)
        calls (:calls timed)
        requests-append (second (first (filter (fn [[kind]] (= :append kind)) calls)))
        answer-reads (filter (fn [[kind _ arguments]] (and (= :query kind) (= 2 arguments))) calls)
        first-read-after-ms (quot (- (second (first answer-reads)) requests-append) 1000000)
        systems-requests (requests-of :system :base :microbatch)
        request-answer-at-return (answer-round (:act-id (first systems-requests)))
        first-record (record-of (:act-id first-value))
        first-answer (answer! d (:act-id first-value) (:digest first-value) wait-ms)
        ;; several acts of one who from several threads while the gate is paused
        gave (decide! d (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}]))
        alices-act (fn [i] {:version 1 :who :alice :layer :base :permission alice-in-base :session :door-proof
                            :facts [(value (keyword (str "alices-doc-" i)) i)]})
        _ (pause-base!)
        work (mapv (fn [i] (future (send! d (alices-act i)))) (range 4))
        _ (Thread/sleep 600)
        _ (resume-base!)
        paused-sent (mapv (fn [f] (deref f 60000 {:could-not :no-return})) work)
        paused-answers (mapv (fn [sent] (when-not (:could-not sent) (answer! d (:act-id sent) (:digest sent) wait-ms))) paused-sent)
        alices-requests (requests-of :alice :base :microbatch)
        alices-lock-ids (mapcat (fn [sent] (when-not (:could-not sent) (lock-ids-of (record-of (:act-id sent))))) paused-sent)
        ;; the base's batches: 8 acts on 8 entities in one batch
        _ (pause-base!)
        eight (mapv (fn [i] (send! d (system-act [{:e (keyword (str "seen-" i)) :k :seen}]))) (range 8))
        _ (resume-base!)
        eight-answers (mapv (fn [sent] (answer! d (:act-id sent) (:digest sent) wait-ms)) eight)
        stamps (map :stamp eight-answers)]
    (check "act-id-for the base before it is made: the base starts as a microbatch layer" [:base :microbatch] (vec (take 2 before)))
    (check "the base's making act by the system: the microbatch depot's ack, which says only that the depot holds the thing; and a wait gives accepted"
           [:depot-holds [:base :microbatch] :accepted] [(:ack made-sent) (vec (take 2 (:act-id made-sent))) (:answer made-answer)])
    (check "a value act into the base: the request is sent inside the call, to the microbatch depot, for 4 locks; the log holds the request and the act"
           [nil :depot-holds [0 2] [4]]
           [(:could-not first-value) (:ack first-value) (mapv - logs-after logs-before) (mapv asked systems-requests)])
    (check "the call came back after the request's batch was closed: the request's answer was readable when the call returned, and the act names the request's first lock"
           [:accepted [(:act-id (first systems-requests)) 0]]
           [(:verdict request-answer-at-return) (:lock-id (first (:facts first-record)))])
    (check "the calls of Rama's client inside that send, in their order: the read of the layer's depot, the request's append, reads of its answer, one read of its locks, the act's append"
           [[:query 1] [:append 2] [:query 2] [:query 4] [:append 2]]
           (mapv (fn [[kind _ arguments]] [kind arguments]) (dedupe (map (fn [[kind _ arguments]] [kind nil arguments]) calls))))
    (check "in a microbatch layer the first read of the door's own request comes after one interval of the wait, 200 ms, and not at once"
           true (<= 195 first-read-after-ms 5000))
    (check "the act's answer by a wait: accepted" :accepted (:answer first-answer))
    (check "the system gives Alice a permission in the base" true (accepted? gave))
    (check "4 acts of one who sent from 4 threads while the gate is paused, and then it goes on: each send goes in, and each answer by a wait is accepted"
           [[nil nil nil nil] [:accepted :accepted :accepted :accepted]]
           [(mapv :could-not paused-sent) (mapv :answer paused-answers)])
    (check "the log holds one request of that who, for 4 locks, and the 4 acts name its 4 locks, each once"
           [[4] 4 4 #{(:act-id (first alices-requests))}]
           [(mapv asked alices-requests) (count alices-lock-ids) (count (set alices-lock-ids)) (set (map first alices-lock-ids))])
    (check "8 acts on 8 entities sent while the gate is paused: each is acked by the depot, and each answer by a bounded wait is accepted"
           [(vec (repeat 8 :depot-holds)) (vec (repeat 8 :accepted))] [(mapv :ack eight) (mapv :answer eight-answers)])
    (check "the 8 were decided in one batch: their 8 stamps are different and lie within one tick of the gate's clock"
           [8 true] [(count (set stamps)) (< (- (reduce max stamps) (reduce min stamps)) 65536)])
    (shown "the base through the door: its making act" (pr-str (dissoc made-sent :digest)) "and a wait gave" (pr-str made-answer))
    (shown "a value act into the base came back after" took-ms "ms, with the door's request for" (asked (first systems-requests))
           "locks sent and answered inside the call; the door read the request's answer" (count answer-reads) "times, the first" first-read-after-ms
           "ms after the request's append (the proof noted the calls by standing in for Rama's client calls) | 4 acts of one who from 4 threads while the gate was paused: requests in the log for that who"
           (pr-str (mapv asked alices-requests)) "| 8 acts on 8 entities in one batch, each accepted by a bounded wait")))

(deftest a-wait-for-locks-that-passes
  (let [d2 (the-short-door)
        id (act-id-for! d2 :base)
        base-requests-before (count (filter request? (log-of :microbatch)))
        id-b (act-id-for! d2 :base)
        _ (pause-base!)
        began (System/nanoTime)
        first-maker (future (send! d2 (system-act [(value :paused-doc "a value")]) id))
        second-maker (future (Thread/sleep 50) (send! d2 (system-act [(value :paused-doc-b "a value")]) id-b))
        word (deref first-maker 30000 {:could-not :no-return})
        word-b (deref second-maker 30000 {:could-not :no-return})
        took-ms (quot (- (System/nanoTime) began) 1000000)
        count-open (door/stock-count d2)
        request-in-log (when (:request word) (record-of (:request word)))
        _ (resume-base!)
        _ (wait-until-base-processed!)
        request-answer (when (:request word) (await-round (:request word)))
        id-2 (act-id-for! d2 :base)
        next-send (send! d2 (system-act [(value :paused-doc "a value")]) id-2)
        answer (when-not (:could-not next-send) (answer! d2 id-2 (:digest next-send) wait-ms))
        base-requests-after (count (filter request? (log-of :microbatch)))
        record (record-of id-2)]
    (check "the microbatch gate is paused, and a value act goes into the base through the door whose wait is 400 ms: the request is acked by its depot and not answered. The word, after the wait"
           [:get-locks-within-its-wait :nothing [:base :microbatch] true true]
           [(:could-not word) (:appended word) (vec (take 2 (:request word))) (<= 400 (:waited-ms word 0)) (<= 400 took-ms 5000)])
    (check "a second maker of that who, 50 ms later, needs locks of the same stock: it waits for the first maker's request and no longer than its own wait, sends no request of its own, and gets the same word"
           [:get-locks-within-its-wait :nothing true true]
           [(:could-not word-b) (:appended word-b)
            (contains? #{nil (:request word)} (:request word-b))
            (<= 395 (:waited-ms word-b 0) 3000)])
    (check "the one request is in the log and stays open in the door; nothing is in the log for either act"
           [true 1 [] [] 1]
           [(request? request-in-log) (:open-requests count-open) (records-of id) (records-of id-b)
            (- (count (filter request? (log-of :microbatch))) base-requests-before)])
    (check "the gate goes on, and the request is accepted" :accepted (:verdict request-answer))
    (check "the next send of that who uses the same request: it goes in, its act names that request, and the log holds one request more, not two"
           [nil :accepted (:request word) 1 0]
           [(:could-not next-send) (:answer answer) (first (:lock-id (first (:facts record))))
            (- base-requests-after base-requests-before) (:open-requests (door/stock-count d2))])
    (shown "a wait for locks that passed, for a request that its depot acked while the gate was paused:"
           (pr-str (select-keys word [:could-not :appended :waited-ms])) "| a second maker that needed the same stock meanwhile:"
           (pr-str (select-keys word-b [:could-not :appended :waited-ms])) (if (:request word-b) "after it read the first maker's request" "after it waited for the stock's seat")
           "| both back after" took-ms
           "ms | when the gate went on, the next send used the same request: requests more in the log" (- base-requests-after base-requests-before))))

(deftest a-request-in-doubt-in-the-base
  (let [d (the-door)
        kims [:kim :base :base system-own]
        gave (decide! d (system-act [{:e :kim :k :softland.store/permission :v kims}]))
        kims-act (fn [e] {:version 1 :who :kim :layer :base :permission kims :session :door-proof :facts [(value e "a value")]})
        id (act-id-for! d :base)
        real-append rama/foreign-append!
        [word mark-set?]
        (on-a-thread
         (fn []
           (let [got (with-redefs [rama/foreign-append!
                                   (fn [depot data & more]
                                     (when (request? data) (.interrupt (Thread/currentThread)))
                                     (apply real-append depot data more))]
                       (send! d (kims-act :kims-doc) id))]
             [got (Thread/interrupted)])))
        count-open (door/stock-count d)
        id-2 (act-id-for! d :base)
        began (System/nanoTime)
        next-send (counting #(send! d (kims-act :kims-doc) id-2))
        took-ms (quot (- (System/nanoTime) began) 1000000)
        answer (when-not (:could-not (:result next-send)) (answer! d id-2 (:digest (:result next-send)) wait-ms))
        kims-requests (requests-of :kim :base :microbatch)
        record (record-of id-2)]
    (check "the system gives Kim a permission in the base" true (accepted? gave))
    (check "a value act into the base whose request's append threw: the word, the thread's mark set again, and the door remembers the request"
           [:append-its-request-cleanly :nothing "java.lang.InterruptedException" true 1]
           [(:could-not word) (:appended word) (:threw word) mark-set? (:open-requests count-open)])
    (check "the next send of that who comes at once, long before the door's wait has passed since the throw. Its first read of the request in doubt finds nothing, since the request's batch is not closed; it does not forget the request, reads again each interval, and uses its locks: 1 append, for the act alone, and 3 queries or more"
           [nil 1 true true :accepted]
           [(:could-not (:result next-send)) (:appends next-send) (<= 3 (:queries next-send)) (< took-ms 9000) (:answer answer)])
    (check "the log holds one request of that who, the one of the word, and the act names its first lock"
           [1 (:request word) [(:request word) 0] 0]
           [(count kims-requests) (:act-id (first kims-requests)) (:lock-id (first (:facts record))) (:open-requests (door/stock-count d))])
    (shown "a request in doubt in the base, met at once by the next send: the door kept it, read its answer" (dec (:queries next-send))
           "times in" took-ms "ms until its batch was closed, and used its locks with" (:appends next-send)
           "append. The proof interrupted its own thread at the request's append, by standing in for Rama's client call with a function that sets the thread's mark and then calls Rama, and it counted the calls of that send by standing in with functions that count and then call Rama.")))

(deftest an-act-id-with-the-other-depot-word
  (let [d (the-door)
        count-before (door/stock-count d)
        alices-requests (count (requests-of :alice))
        id-1 (assoc (act-id-for! d :alice) 1 :microbatch)
        no-value (decide! d (act-of :alice [{:e :wrong-depot :k :text}]) id-1)
        id-2 (assoc (act-id-for! d :alice) 1 :microbatch)
        logs-before (log-counts)
        with-value (send! d (act-of :alice [(value :wrong-depot-2 "a value")]) id-2)
        logs-after (log-counts)
        count-after (door/stock-count d)]
    (check "Alice's stock of her layer's own depot holds locks" true (pos? (:unused-locks count-before)))
    (check "an act of a streaming layer under an act-id that a maker gives back with the other depot word: the thing goes to the depot that the act-id names, and that gate's recorded refusal comes back"
           [:depot-holds {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:ack (:sent no-value)) (dissoc (:answer no-value) :stamp)])
    (check "the same with a value: the stock of that layer, that depot and that who is another stock, and it is empty; the door's request goes to the same depot and is refused there"
           [:have-its-request-accepted :nothing [:alice :microbatch] {:answer :refused :reason :layer-belongs-to-the-other-gate} [0 1]]
           [(:could-not with-value) (:appended with-value) (vec (take 2 (:request with-value)))
            (dissoc (:request-answer with-value) :stamp) (mapv - logs-after logs-before)])
    (check "no lock of the layer's own depot was taken for it, and no request was sent to that depot"
           [count-before alices-requests] [count-after (count (requests-of :alice))])
    (shown "an act-id given back with the other depot word: an act with no value comes back" (pr-str (dissoc (:answer no-value) :stamp))
           "| an act with a value stops at the door's request:" (pr-str (dissoc (:request-answer with-value) :stamp))
           "| the locks that the door holds for the layer's own depot are untouched:" (pr-str count-after))))

(deftest the-module-is-started-again
  (let [d (the-door)
        landed (:landed @kept)
        doras-requests (requests-of :dora)
        count-before (door/stock-count d)
        _ (rtest/update-module! (:ipc *round*) module/Store)
        answer-after (answer! d (:act-id landed) (:digest landed))
        with-old-lock (decide! d (act-of :dora [(value :after-the-update "sealed under a lock that the door held before")]))
        count-after (door/stock-count d)
        record (record-of (:act-id (:sent with-old-lock)))
        base-after (decide! d (system-act [(value :doc-after-the-update "a value")]))]
    (check "the door held unused locks of that who before the module was started again" true (pos? (:unused-locks count-before)))
    (check "answer-of gives the same answer for an act-id and a digest from before" (:answer landed) answer-after)
    (check "a lock that the door held before seals a value after: the act is accepted, it names a request from before, and the door sent no request"
           [true true (count doras-requests) 1]
           [(accepted? with-old-lock)
            (contains? (set (map :act-id doras-requests)) (first (:lock-id (first (:facts record)))))
            (count (requests-of :dora))
            (- (:unused-locks count-before) (:unused-locks count-after))])
    (check "an act into the base through the same door is accepted after it too" true (accepted? base-after))
    (shown "the module was started again under the opened door, by Rama's module update with the unchanged module: answer-of gives"
           (pr-str answer-after) "for an act from before, and a lock that the door held before sealed a value after:" (told with-old-lock))))

(deftest the-doors-memory-starts-again
  (let [d (the-door)
        d2 (the-short-door)
        first-count (door/stock-count d)
        alices-requests (requests-of :alice)
        d3 (noted (door/open (:ipc *round*) (:module *round*)))
        third-before (door/stock-count d3)
        after-restart (decide! d3 (act-of :alice [(value :after-the-restart "a value")]))
        alices-requests-after (requests-of :alice)
        record (record-of (:act-id (:sent after-restart)))
        new-request (first (remove (set (map :act-id alices-requests)) (map :act-id alices-requests-after)))
        ;; the rows that seal nothing, counted from the record round the door
        d4 (:numbers-door @kept)
        counts (mapv door/stock-count [d d2 d3 d4])
        rows (lock-rows)
        sealing? (fn [found] (boolean (some record/seals-a-fact? found)))
        seals-nothing (set (keep (fn [[k found]] (when-not (sealing? found) k)) rows))
        named (set (map key-of-lock (mapcat lock-ids-of (concat (log-of :streaming) (log-of :microbatch)))))
        named-and-not-sealing (count (filter named seals-nothing))
        never-named (count (remove named seals-nothing))
        in-the-stocks (reduce + (map :unused-locks counts))
        outside (reduce + (map :locks @outside-the-stocks))
        made (reduce + (for [r (concat (log-of :streaming) (log-of :microbatch))
                             :let [counts (locks-asked-in r)]
                             :when (seq counts)
                             :let [answer (answer-round (:act-id r))]
                             :when (and (= :accepted (:verdict answer)) (= (:digest answer) (digest/hex-of r)))
                             n counts]
                         n))]
    (swap! kept assoc :rows rows :doors [d d2 d3 d4])
    (check "the first door holds unused locks, of Alice among others" [true true] [(pos? (:unused-locks first-count)) (pos? (:stocks first-count))])
    (check "a door that is opened holds nothing, whatever the record holds" {:unused-locks 0 :stocks 0 :open-requests 0} third-before)
    (check "its first value act for a who of the first door sends a new request, and names no lock of an old one"
           [true (inc (count alices-requests)) [new-request 0]]
           [(accepted? after-restart) (count alices-requests-after) (:lock-id (first (:facts record)))])
    (check "none of the four doors that hold locks holds an open request at the end" [0 0 0 0] (mapv :open-requests counts))
    (check "each lock that an accepted request made is a row of the record" made (count rows))
    (check "the rows that seal nothing and that no record of a log names are exactly: the unused locks that the four doors hold, and the locks that the proof left outside every stock"
           (+ in-the-stocks outside) never-named)
    (shown "THE ROWS THAT SEAL NOTHING, counted in both records round the door:" (count seals-nothing) "of" (count rows) "lock rows."
           "Their sources:" in-the-stocks "unused in the stocks of the four doors" (pr-str (mapv :unused-locks counts))
           "(the first door's would be lost at a restart of its process) |" named-and-not-sealing
           "taken for an act that was not accepted: refused, second under its act-id, or one whose value was sealed and the gate refused the thing |"
           outside "outside every stock:" (str/join "; " (map (fn [{:keys [why locks]}] (str locks " " why)) @outside-the-stocks)))
    (shown "a new door holds nothing:" (pr-str third-before) "| its first value act for Alice sent a new request, where the first door held"
           (pr-str first-count))))

(defn- bytes-in?
  "True when a thing holds a run of bytes, wherever it sits."
  [x]
  (cond
    (bytes? x) true
    (map? x) (boolean (some bytes-in? (concat (keys x) (vals x))))
    (coll? x) (boolean (some bytes-in? x))
    :else false))

(deftest no-locks-bytes-anywhere
  (let [d (the-door)
        streaming (log-of :streaming)
        microbatch (log-of :microbatch)
        records (concat streaming microbatch)
        rows (:rows @kept)
        ;; every lock's bytes: from the query for each request in the logs, and from the rows
        request-records (filter (fn [r] (seq (locks-asked-in r))) records)
        read-round (vec (mapcat (fn [r] (vals (locks-round (:act-id r)))) request-records))
        from-the-query (into #{} (comp (keep :bytes) (map text/hex)) read-round)
        nil-from-the-query (count (filter (fn [lock] (nil? (:bytes lock))) read-round))
        from-the-rows (into #{} (comp (mapcat val) (keep :bytes) (map text/hex)) rows)
        every-lock (into from-the-query from-the-rows)
        depot-text (apply str (map text/text records))
        ;; a lock never crosses a who, over every record of both logs
        requests-by-id (into {} (map (fn [r] [(:act-id r) r])) (filter request? records))
        makers-own-seal (:a-makers-own-seal @kept)
        crossing (for [r records
                       :when (and (map? r) (not= makers-own-seal (:act-id r)))
                       [request _] (lock-ids-of r)
                       :let [asked-by (get requests-by-id request)]
                       :when (not (and asked-by
                                       (= (:who asked-by) (:who r))
                                       (= (:layer asked-by) (:layer r))
                                       (= (:depot asked-by) (:depot r))
                                       (= (nth request 1) (nth (:act-id r) 1))))]
                   [(:act-id r) request])
        acts-with-locks (count (filter (fn [r] (and (map? r) (seq (lock-ids-of r)))) records))
        given (set (keep :could-not @results))
        every-stock (for [door (:doors @kept) triple (keys (stocks-of door))] (stock-told door triple))]
    (check "every lock of every request in both logs was read round the door: as many different runs of bytes as lock rows"
           (count rows) (count every-lock))
    (check "the bytes of a lock of the base that seals a value come from its whole row, since the query gives nil for them: the query gave bytes for every lock but those"
           [true (count rows)] [(pos? nil-from-the-query) (+ (count from-the-query) nil-from-the-query)])
    (check "no record of either log holds a run of bytes that is one of them"
           [true false] [(< 500 (count every-lock)) (boolean (some (fn [hex] (str/includes? depot-text hex)) every-lock))])
    (check "no result that a door gave the proof in the whole run holds a run of bytes at all"
           [true false] [(< 1000 (count @results)) (boolean (some bytes-in? @results))])
    (check "the door that holds locks still prints as the word door and the module's name, and its count is numbers"
           [(vec (repeat 4 (:door-text @kept))) true]
           [((:printed @kept) d) (every? int? (vals (door/stock-count d)))])
    (check "a lock never crosses a who: over every record of both logs, each lock id that an act names is of a request whose who and layer are that act's, and whose depot is the act's depot"
           [true []] [(< 600 acts-with-locks) (vec crossing)])
    (check "at the end, over every stock in the memory of the four doors that hold locks: no call is counted in, no seat is taken, and no request is open"
           [true #{{:calls 0 :open? false :seat-free? true}}] [(< 20 (count every-stock)) (set every-stock)])
    (check "the door made no stray in the whole run" 0 (strays))
    (check "each of the door's fourteen words was given in this run" (set (keys words/words)) given)
    (shown "no lock's bytes are on a depot:" (count every-lock) "locks, each read round the door, of which" nil-from-the-query
           "came from the query with no bytes and were read from their rows; none is in the" (count records)
           "records of the two logs | no result of" (count @results) "that the doors gave holds a run of bytes")
    (shown "a lock never crosses a who: over" acts-with-locks "records with sealed values in both logs, each lock is of a request with the act's who, layer and depot."
           "One record is left out: the act whose fact carried a seal and a lock id of the maker's own making, not the door's.")
    (shown "each of the door's fourteen words was given:" (pr-str (sort given)) "| strays of both gates:" (strays))))

(defn test-ns-hook
  "Runs the checks in the order in which the proof tells them. Later checks
  build on the layers that earlier ones made. The base is made late, the
  module is started again after every check of the first door's stocks, and
  the counts over the whole run come last."
  []
  (the-door-is-opened)
  (an-act-with-no-value)
  (round-trips-are-counted)
  (locks-and-the-seal)
  (the-door-and-the-hand-send-the-same-thing)
  (the-digest-is-the-gates)
  (things-that-are-no-act)
  (the-answer-back)
  (a-refused-request)
  (a-request-in-doubt)
  (reads-that-throw)
  (the-seat-and-the-wait)
  (a-requests-answer-with-another-digest)
  (an-act-over-three-runs)
  (the-openers-numbers)
  (a-who-that-cannot-be-a-key)
  (an-append-that-threw)
  (a-lock-never-crosses-a-who)
  (many-makers-at-once)
  (the-base-through-the-door)
  (a-wait-for-locks-that-passes)
  (a-request-in-doubt-in-the-base)
  (an-act-id-with-the-other-depot-word)
  (the-module-is-started-again)
  (the-doors-memory-starts-again)
  (no-locks-bytes-anywhere))

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
