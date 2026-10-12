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
  a second time, and appends that thing itself.

  What it reads round the door. The door gives a maker no thing and no row.
  So, as the module's proof does, this proof holds handles of its own: it
  reads each record of the two depots' logs, to see what the door appended;
  the answer under an act-id, by the query `answer-by-act-id`; the row of a
  place, by a select of the gates' tables of facts, to see what a gate kept
  of a value and that a refused act left no row; and the strays of both
  gates.

  Where the proof stands in for a call of Rama's client. Clojure lets a
  proof put a function of its own in the place of `foreign-append!`,
  `foreign-invoke-query`, `foreign-depot` or `foreign-query` for a while.
  The proof does so for four things, and each line that rests on it says
  so: to count the handles that the opening of a door fetches; to count the
  round trips of a call of the door; to note the calls of one send in their
  order; and to make a read throw, since no real throw of a query was found
  that the in-process cluster gives without a fault of the cluster. The
  other throws are real throws of Rama's client: a module name under which
  no module runs, a thing that holds a plain Java object, a thing whose
  `:who` is an object of the proof's own, and a call made on a thread whose
  interrupt mark the proof set. The proof never destroys the module to make
  a call throw: the close of the cluster then throws. Two things of a
  maker's act are the proof's own objects: a `:session` that throws when it
  is read, and a `:who` that throws when it is hashed.

  State. The proof starts one in-process cluster with four tasks and two
  workers, launches the module `Store` in it, and closes it at the end; the
  cluster's files are temporary and are Rama's. It opens six doors on the
  module: the door of most checks; a second door, opened with a read each
  100 ms; one more for the count of the handles that an opening fetches;
  two in the check of the opener's number; and one near the end, which
  stands for the door of another process. It owns three atoms: the lines
  that it showed; each result that a door gave it; and what later checks
  come back to. It reads the clock to time a wait.

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

(defn- rows-at
  "Each row that the two gates hold at one place, by a select of both
  gates' tables of facts on each task: a vector, empty when no gate holds a
  row there. It asks every task, so it does not rest on where a gate keeps
  a place. A place under a plain key has one row at most. What must hold:
  each of the three is an id within the limit."
  [layer e k]
  (vec (for [pstate [:streaming-facts :microbatch-facts]
             pkey (rtest/gen-hashing-index-keys tasks)
             :let [row (rama/foreign-select-one (path/keypath (record/place-key layer e k)) (get *round* pstate) {:pkey pkey})]
             :when row]
         row)))

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
        landed (decide! d first-act)
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
    (check "the record in the log is the maker's act with :act-id and :depot, and its fact holds :e, :k and :v and no other part"
           [(assoc first-act :act-id act-id :depot :streaming) #{:e :k :v}]
           [record (set (keys fact))])
    (check "the value in the log is as the maker gave it and not in its normal form: the decimal keeps its trailing zero, and the big integer is big"
           ["1.50M" "[1 2N]" #{:a :b}]
           [(pr-str (:b in-log)) (pr-str (:a in-log)) (when (map? in-log) (set (keys in-log)))])
    (check "one gate holds one row at the place, and it holds the one text of the value in its normal form"
           ["{:a [1 2] :b 1.5M}"] (mapv :value rows))
    (check "a reader of that row is given the value" [:value {:a [1 2] :b 1.5M}] (record/read-value (first rows)))
    (check "the digest that the maker got is the digest of the record in the log, and the record's answer under the act-id is of that digest"
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

(defn- without-the-uuid
  "Takes a record of the log. Gives it with the uuid of its act-id set
  aside: the one part of a thing that is made at random."
  [r]
  (assoc r :act-id (assoc (:act-id r) 2 :the-acts-uuid)))

(deftest the-door-and-the-hand-send-the-same-thing
  (let [d (the-door)
        made? (make-layer! d :erin)
        facts [(value :note-1 {:n 1 :tags #{:a}})
               {:e :note-2 :k :text}
               {:e :erin :k :softland.store/who-may-see :v :members}]
        ;; through the door
        by-door (decide! d (act-of :erin facts))
        door-record (record-of (:act-id (:sent by-door)))
        ;; by the hand: the hand's steps, written out a second time
        depot-word (or (rama/foreign-invoke-query (:depot-of-layer *round*) :erin) (first-act/depot-at-making :erin))
        hand-id [:erin depot-word (UUID/randomUUID)]
        hand-thing (merge {:act-id hand-id :depot depot-word} (act-of :erin facts))
        _ (rama/foreign-append! (:streaming-depot *round*) hand-thing :ack)
        hand-answer (answer-round hand-id)
        hand-record (record-of hand-id)]
    (check "the layer of this check is made" true made?)
    (check "one act with a value, a retract and a store-key fact goes in through the door and is accepted" true (accepted? by-door))
    (check "the same act by the hand is accepted, with the digest of what the hand sent"
           [:accepted (digest/hex-of hand-thing)] [(:verdict hand-answer) (:digest hand-answer)])
    (check "the door's record in the log, with the uuid of its act-id set aside, is the maker's act with the door's two additions, written out here"
           {:version 1 :who :erin :layer :erin :permission [:erin :erin :erin] :session :door-proof
            :facts [{:e :note-1 :k :text :v {:n 1 :tags #{:a}}}
                    {:e :note-2 :k :text}
                    {:e :erin :k :softland.store/who-may-see :v :members}]
            :act-id [:erin :streaming :the-acts-uuid]
            :depot :streaming}
           (without-the-uuid door-record))
    (check "the hand's record is equal to it but for that uuid"
           (without-the-uuid door-record) (without-the-uuid hand-record))
    (check "the two act-ids differ, and each holds a uuid: two records were compared, and the uuid is the one part in which they differ"
           [false true true]
           [(= (:act-id door-record) (:act-id hand-record))
            (uuid? (get (:act-id door-record) 2))
            (uuid? (get (:act-id hand-record) 2))])
    (check "the place that both acts wrote holds one row, with the one text of the value in its normal form"
           ["{:n 1 :tags #{:a}}"] (mapv :value (rows-at :erin :note-1 :text)))
    (shown "the door and the hand send the same thing for one act with a value, a retract and a store-key fact, but for the uuid of the act-id:"
           (pr-str (without-the-uuid door-record))
           "| both accepted. The proof appended the hand's act itself.")))

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
    (check "for facts given as a vector, a list and a lazy sequence: the answer under the act-id is of the digest that send-act gave, and the digest is that of the record in the log; in the log :facts is a vector only where the maker gave one, a sequence in each case, with each fact at its position"
           [[:vector :accepted true true true true]
            [:list :accepted true false true true]
            [:lazy-sequence :accepted true false true true]]
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
           (pr-str (mapv first (filter (fn [row] (nth row 3)) seen))) "alone")
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
        send-1 (send! d twice id-2)
        send-2 (send! d twice id-2)
        answer-1 (answer! d id-2 (:digest send-1))
        answer-2 (answer! d id-2 (:digest send-2))
        twice-records (records-of id-2)
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
    (check "a maker's own second send of one act with a value is the same thing: one digest of 64 hex characters for both sends; both went to the depot, which holds two equal records under the act-id"
           [true true [:gate-ran :gate-ran] 2 true]
           [(hex64? (:digest send-1)) (= (:digest send-1) (:digest send-2)) [(:ack send-1) (:ack send-2)]
            (count twice-records) (= (first twice-records) (second twice-records))])
    (check "a gate answered that act-id once, and both sends read the one answer as their own: accepted, with one stamp"
           [{:answer :accepted} true true]
           [(dissoc answer-1 :stamp) (int? (:stamp answer-1)) (= answer-1 answer-2)])
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
    (shown "a maker's own second send of one act with a value is the same thing: the two digests" (subs (:digest send-1) 0 12) (subs (:digest send-2) 0 12)
           "read" (:answer answer-1) "and" (:answer answer-2) "| records in the log under that act-id:" (count twice-records))))

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
        sent (counting #(send! d mallorys-act id))
        logs-after (log-counts)
        answer (answer! d id (:digest (:result sent)))
        round-the-door (answer-round id)
        ;; a value act into a layer that is not made
        nowheres-act (act-of :nowhere [(value :n-1 "a value into a layer that is not made")])
        logs-before-2 (log-counts)
        not-made (decide! d nowheres-act)
        logs-after-2 (log-counts)
        not-made-id (:act-id (:sent not-made))]
    (check "a value of the layer's holder is accepted, and the select round the door finds its one row"
           [true ["\"a value of the layer's holder\""]] [(accepted? alices) (mapv :value alices-rows)])
    (check "a who under a permission that was never given sends a value act into that layer: the door checks nothing of her and stops nothing; one append and no query, the streaming gate's ack, and one record more in the streaming depot's log"
           [nil :gate-ran [1 0] [1 0]]
           [(:could-not (:result sent)) (:ack (:result sent)) [(:appends sent) (:queries sent)] (mapv - logs-after logs-before)])
    (check "the log holds her act under its own act-id, as she gave it, with its value plain"
           [(assoc mallorys-act :act-id id :depot :streaming)] (records-of id))
    (check "a gate refuses it, and the refusal is recorded under the act's own act-id with the gate's word: answer-of gives it for the digest that the send gave, and the record's answer round the door is of that digest"
           [{:answer :refused :reason :permission-does-not-stand} [:refused :permission-does-not-stand (:digest (:result sent))]]
           [(dissoc answer :stamp) [(:verdict round-the-door) (:reason round-the-door) (:digest round-the-door)]])
    (check "no fact of it is in the tables: no gate holds a row at its place, on any task" [] (rows-at :alice :m-1 :text))
    (check "a value act into a layer that is not made: the thing goes to the depot that the layer would start with, one record more in its log, and a gate refuses it under its own act-id with its word"
           [nil :gate-ran [1 0] {:answer :refused :reason :layer-not-made}]
           [(:could-not (:sent not-made)) (:ack (:sent not-made)) (mapv - logs-after-2 logs-before-2) (dissoc (:answer not-made) :stamp)])
    (check "the log holds that act too, as its maker gave it, with its value plain; and no gate holds a row at its place"
           [[(assoc nowheres-act :act-id not-made-id :depot :streaming)] []]
           [(when not-made-id (records-of not-made-id)) (rows-at :nowhere :n-1 :text)])
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
        sent (send! d veras-act id)
        logs-after (log-counts)
        answer (answer! d id (:digest sent))
        round-the-door (answer-round id)
        no-value (decide! d {:version 1 :who :vera :layer :alice :permission veras :session :door-proof
                             :facts [{:e :v-1 :k :text}]})]
    (check "Alice gives, through the door, a permission in her layer that says see: a fact under a store key, and it is accepted"
           true (accepted? gave))
    (check "the holder of see sends a value: the thing goes in under the act-id that she gave back, one record more in the streaming depot's log, and the log holds her act as she gave it, with its value plain"
           [nil :gate-ran [1 0] [(assoc veras-act :act-id id :depot :streaming)]]
           [(:could-not sent) (:ack sent) (mapv - logs-after logs-before) (records-of id)])
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
    (check "8 whos at once, each on 2 threads, 10 acts of one value each: every layer is made and every act is accepted"
           [(vec (repeat 8 true)) 160] [whos-made (count (filter accepted? decided-2))])
    (check "for each of the 8 whos: 20 records under her who in the log, and each of her 20 values reads back from the row of its own place"
           (vec (repeat 8 [20 20])) by-who)
    (shown "16 threads through one door for one who:" (count (filter accepted? decided)) "acts accepted with" values
           "values, of which" in-their-rows "read back from their rows | records more in the two logs:" (pr-str (mapv - logs-after logs-before)))
    (shown "8 whos at once, each on 2 threads:" (count (filter accepted? decided-2)) "acts accepted; records and rows of each who:" (pr-str by-who))))

(deftest the-base-through-the-door
  (let [d (the-door)
        before (act-id-for! d :base)
        making {:version 1 :who :system :layer :base :permission system-own :session :door-proof
                :facts [{:e :base :k :softland.store/depot :v :microbatch}
                        {:e :system :k :softland.store/permission :v system-own}]}
        made-sent (send! d making)
        made-answer (answer! d (:act-id made-sent) (:digest made-sent) wait-ms)
        ;; a value act: one read of the layer's depot and one append
        first-act (system-act [(value :doc-1 "a title")])
        logs-before (log-counts)
        noted-calls (noting #(send! d first-act))
        first-value (:result noted-calls)
        logs-after (log-counts)
        first-record (when-not (:could-not first-value) (record-of (:act-id first-value)))
        first-answer (when-not (:could-not first-value) (answer! d (:act-id first-value) (:digest first-value) wait-ms))
        first-rows (rows-at :base :doc-1 :text)
        ;; several acts of one who from several threads while the gate is paused
        gave (decide! d (system-act [{:e :alice :k :softland.store/permission :v alice-in-base}]))
        alices-act (fn [i] {:version 1 :who :alice :layer :base :permission alice-in-base :session :door-proof
                            :facts [(value (keyword (str "alices-doc-" i)) i)]})
        _ (pause-base!)
        logs-before-paused (log-counts)
        work (mapv (fn [i] (future (send! d (alices-act i)))) (range 4))
        paused-sent (mapv (fn [f] (deref f 60000 {:could-not :no-return})) work)
        logs-while-paused (log-counts)
        while-paused (mapv (fn [sent] (when-not (:could-not sent) (answer! d (:act-id sent) (:digest sent)))) paused-sent)
        _ (resume-base!)
        paused-answers (mapv (fn [sent] (when-not (:could-not sent) (answer! d (:act-id sent) (:digest sent) wait-ms))) paused-sent)
        ;; the base's batches: 8 acts on 8 entities in one batch
        _ (pause-base!)
        eight (mapv (fn [i] (send! d (system-act [{:e (keyword (str "seen-" i)) :k :seen}]))) (range 8))
        _ (resume-base!)
        eight-answers (mapv (fn [sent] (answer! d (:act-id sent) (:digest sent) wait-ms)) eight)
        stamps (map :stamp eight-answers)]
    (check "act-id-for the base before it is made: the base starts as a microbatch layer" [:base :microbatch] (vec (take 2 before)))
    (check "the base's making act by the system: the microbatch depot's ack, which says only that the depot holds the thing; and a wait gives accepted"
           [:depot-holds [:base :microbatch] :accepted] [(:ack made-sent) (vec (take 2 (:act-id made-sent))) (:answer made-answer)])
    (check "a value act into the base: the microbatch depot's ack, and one record more in its log, the act itself"
           [nil :depot-holds [0 1]]
           [(:could-not first-value) (:ack first-value) (mapv - logs-after logs-before)])
    (check "the calls of Rama's client inside that send, in their order: the read of the layer's depot, and the act's append"
           [[:query 1] [:append 2]] (:calls noted-calls))
    (check "the record in the log is the system's act with :act-id and :depot, its value plain"
           (assoc first-act :act-id (:act-id first-value) :depot :microbatch) first-record)
    (check "the act's answer by a wait is accepted, and one gate holds one row at its place, with the text of its value"
           [:accepted ["\"a title\""]] [(:answer first-answer) (mapv :value first-rows)])
    (check "the system gives Alice a permission in the base" true (accepted? gave))
    (check "4 acts of one who sent from 4 threads while the gate is paused: each send comes back with the depot's ack while the gate is still paused, the microbatch log holds 4 records more, and each reads nothing yet"
           [[nil nil nil nil] (vec (repeat 4 :depot-holds)) [0 4] (vec (repeat 4 {:answer :nothing-yet}))]
           [(mapv :could-not paused-sent) (mapv :ack paused-sent) (mapv - logs-while-paused logs-before-paused) while-paused])
    (check "the gate goes on, and each of the 4 answers by a wait is accepted"
           [:accepted :accepted :accepted :accepted] (mapv :answer paused-answers))
    (check "8 acts on 8 entities sent while the gate is paused: each is acked by the depot, and each answer by a bounded wait is accepted"
           [(vec (repeat 8 :depot-holds)) (vec (repeat 8 :accepted))] [(mapv :ack eight) (mapv :answer eight-answers)])
    (check "the 8 were decided in one batch: their 8 stamps are different and lie within one tick of the gate's clock"
           [8 true] [(count (set stamps)) (< (- (reduce max stamps) (reduce min stamps)) 65536)])
    (shown "the base through the door: its making act" (pr-str (dissoc made-sent :digest)) "and a wait gave" (pr-str made-answer))
    (shown "a value act into the base made the calls" (pr-str (:calls noted-calls))
           "of Rama's client, which the proof noted by standing in for them with functions that note and then call Rama; a wait gave" (pr-str first-answer)
           "| 4 acts of one who from 4 threads while the gate was paused each came back with the depot's ack and read" (pr-str (first while-paused))
           "until the gate went on | 8 acts on 8 entities in one batch, each accepted by a bounded wait")))

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
        seen (fn [layer i answer]
               (let [rows (rows-at layer (entity i) :text)]
                 [(:answer answer) (mapv :value rows) (record/read-value (first rows))]))
        all (vec (map-indexed (fn [i [what v text]]
                                [what v text (seen :kinds i (:answer (nth streaming i))) (seen :base i (nth base-answers i))])
                              kinds-of-value))]
    (check "the layer of this check is made, and the proof holds a value of 45 kinds" [true 45] [made? (count kinds-of-value)])
    (doseq [[what v text through-streaming through-base] all]
      (check (str what ", through the streaming depot: accepted under the door's digest; one gate holds one row at its place, with the one text of the value in its normal form; a reader of the row is given the value")
             [:accepted [text] [:value v]] through-streaming)
      (check (str what ", through the microbatch depot: the same")
             [:accepted [text] [:value v]] through-base))
    (shown "a value of each of" (count kinds-of-value) "kinds that the form takes, through the door and each depot: accepted under the door's digest through the streaming depot"
           (count (filter (fn [[_ _ _ [answer]]] (= :accepted answer)) all)) "| through the microbatch depot"
           (count (filter (fn [[_ _ _ _ [answer]]] (= :accepted answer)) all))
           "| rows that hold the text written out in the proof:"
           (count (filter (fn [[_ _ text [_ texts]]] (= [text] texts)) all)) "and"
           (count (filter (fn [[_ _ text _ [_ texts]]] (= [text] texts)) all)))
    (shown "for example" (pr-str {:b 1.50M :a [1 2N]}) "is kept as" (pr-str "{:a [1 2] :b 1.5M}")
           "| the texts of a few kinds:" (str/join " | " (map (fn [[what _ text]] (str what " " (pr-str text))) (take 12 (drop 12 kinds-of-value)))))))

(deftest an-act-id-with-the-other-depot-word
  (let [d (the-door)
        id-1 (assoc (act-id-for! d :alice) 1 :microbatch)
        no-value (decide! d (act-of :alice [{:e :wrong-depot :k :text}]) id-1)
        id-2 (assoc (act-id-for! d :alice) 1 :microbatch)
        with-value-act (act-of :alice [(value :wrong-depot-2 "a value")])
        logs-before (log-counts)
        with-value (decide! d with-value-act id-2)
        logs-after (log-counts)]
    (check "an act of a streaming layer under an act-id that a maker gives back with the other depot word: the thing goes to the depot that the act-id names, and that gate's recorded refusal comes back"
           [:depot-holds {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:ack (:sent no-value)) (dissoc (:answer no-value) :stamp)])
    (check "the same with a value: the thing goes to the depot that the act-id names, one record more in that log and none in the layer's own, and that gate's recorded refusal comes back under the act's own act-id"
           [nil :depot-holds [0 1] {:answer :refused :reason :layer-belongs-to-the-other-gate}]
           [(:could-not (:sent with-value)) (:ack (:sent with-value)) (mapv - logs-after logs-before) (dissoc (:answer with-value) :stamp)])
    (check "the log of that depot holds the act as its maker gave it, with its value plain; and no fact of it is in the tables: no gate holds a row at its place, on any task"
           [[(assoc with-value-act :act-id id-2 :depot :microbatch)] []]
           [(records-of id-2) (rows-at :alice :wrong-depot-2 :text)])
    (shown "an act-id given back with the other depot word: an act with no value comes back" (pr-str (dissoc (:answer no-value) :stamp))
           "| an act with a value goes to the depot that the act-id names and comes back" (pr-str (dissoc (:answer with-value) :stamp))
           "under its own act-id, with no row at its place")))

(deftest the-module-is-started-again
  (let [d (the-door)
        landed (:landed @kept)
        row-before (row-of :dora :note-2 :text)
        _ (rtest/update-module! (:ipc *round*) module/Store)
        answer-after (answer! d (:act-id landed) (:digest landed))
        after (decide! d (act-of :dora [(value :after-the-update "a value that is sent after the module was started again")]))
        row-after (row-of :dora :note-2 :text)
        base-after (decide! d (system-act [(value :doc-after-the-update "a value")]))]
    (check "answer-of gives the same answer for an act-id and a digest from before" (:answer landed) answer-after)
    (check "a value act through the same door after it is accepted, and its row holds the text of its value"
           [true "\"a value that is sent after the module was started again\""]
           [(accepted? after) (:value (row-of :dora :after-the-update :text))])
    (check "a row that a gate wrote before is the same row after, with the text of its value"
           [true "\"one value\""] [(= row-before row-after) (:value row-after)])
    (check "an act with a value into the base through the same door is accepted after it too" true (accepted? base-after))
    (shown "the module was started again under the opened door, by Rama's module update with the unchanged module: answer-of gives"
           (pr-str answer-after) "for an act from before, and a value act after it:" (told after))))

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
  build on the layers that earlier ones made. The base is made late, the
  module is started again near the end, and the check over the whole run
  comes last."
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
