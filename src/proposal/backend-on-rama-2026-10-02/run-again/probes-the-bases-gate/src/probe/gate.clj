(ns probe.gate
  "The stand-in gate that every probe in this folder runs, on Rama 1.6.0
  in-process, and the hand that feeds it and reads it.

  The question behind the probes: a gate decides every act of a layer on ONE
  task, one at a time in the order of the layer's one depot partition, each
  against the rows that the earlier acts wrote, and then copies each written
  row to EVERY task. Which of the Rama claims under that hold when run?

  The gate. A microbatch topology `gate` over a depot `*acts` declared
  `(hash-by :layer)`. A record is {:layer :id :key :val :replaces}. The walk
  for one record, on the task where the source emits it:

    1. read `$$answers` at the id: if it is there, do nothing more.
    2. read `$$rows` at [layer key]: accept when the record's :replaces equals
       the :id that the row holds (nil for no row), else refuse.
    3. n = the layer's last number + 1; write `$$last`, the answer
       {:verdict :n} and, for an accept, the row {:val :id :n}, all on this
       task.
    4. for an accept: `|all`, and write the same row on every task except the
       deciding one.

  What a probe can change, and how:
    - the layer's NAME turns two things: a name with \"noskip\" in it makes
      the deciding task write its own copies too (step 4 without the skip); a
      name with \"yield\" in it makes the walk call `(yield-if-overtime)`
      before step 1; a name with \"slow\" in it makes the walk keep the
      thread busy for 200 microseconds on every record, before that call.
    - a module built with `hop?` has `(|hash *layer)` at the head of the walk.
    - `throw-at` holds, for a module's tag, the id and the place at which the
      gate throws once: at the head of the walk before step 1, or where a
      copy arrives, on the deciding task or on another.

  State. The gate's three tables: `$$rows`, `$$answers`, `$$last`. Two tables
  that only the probe reads: `$$trace` (id -> the microbatch, the task at the
  source, the deciding task) and `$$attempts` ([attempt id] -> the deciding
  task, written wherever the row is written; the attempt is 1 until the throw
  and 2 after it, and is carried from the deciding task to the copies). One
  more table, `$$here`, written by a stream topology `ping`, holds each task's
  own number, so that a client can find a partitioning key that lands on a
  given task. Outside Rama the probe keeps a journal in an atom: one entry for
  each decision and each copy, in the order in which each task ran them.

  A probe, not a design: no table, name or form here is a proposal for the
  store. It requires nothing outside this folder."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]))

;; ---- the rule and the switches, pure ----------------------------------------

(defn verdict-of
  "The rule. Takes the row that the table holds for a key, or nil, and the id
  that a record says it replaces, or nil. Gives :accept or :refuse."
  [held replaces]
  (if (= replaces (:id held)) :accept :refuse))

(defn skips?
  "Takes a layer's name. Says whether the deciding task skips its own copy."
  [layer]
  (not (str/includes? layer "noskip")))

(defn yields?
  "Takes a layer's name. Says whether the walk calls `(yield-if-overtime)`."
  [layer]
  (str/includes? layer "yield"))

(defn slow?
  "Takes a layer's name. Says whether the walk burns time on every record."
  [layer]
  (str/includes? layer "slow"))

(defn burn!
  "Takes a layer's name. For a slow layer, keeps the task's thread busy for
  200 microseconds, so that a batch's walk is far over the 5 ms after which
  `(yield-if-overtime)` yields. Gives nil."
  [layer]
  (when (slow? layer)
    (let [until (+ (System/nanoTime) 200000)]
      (while (< (System/nanoTime) until))))
  nil)

(defn writes-copy?
  "Takes a layer's name, the task that a copy arrived on and the deciding
  task. Says whether the row is written here."
  [layer here at]
  (or (not (skips? layer)) (not= here at)))

;; ---- what the probe keeps outside Rama ---------------------------------------

(defonce ^{:doc "One entry [tag kind task id attempt thread nanos] for each decision and each copy."}
  journal (atom []))

(defonce ^{:doc "tag of a module -> {:id :where :task}: the record, the place and (when given) the task at which its gate throws once."}
  throw-at (atom {}))

(defonce ^{:doc "The tags of the modules whose one throw has been taken."} claimed (atom #{}))

(defonce ^{:doc "tag of a module -> the nanoTime at which its walk threw."} thrown (atom {}))

(defn note!
  "Takes a module's tag, what happened, the task, the record's id and the
  attempt. Adds one entry to the journal. Gives nil."
  [tag kind task id attempt]
  (swap! journal conj [tag kind task id attempt (.getName (Thread/currentThread)) (System/nanoTime)])
  nil)

(defn attempt-of
  "Takes a module's tag. Gives 1 until that module's walk has thrown, 2 after."
  [tag]
  (if (contains? @thrown tag) 2 1))

(defn leg
  "Takes the task that a copy arrived on and the deciding task. Gives :back
  when the copy came back to the deciding task, :copy on any other task."
  [here at]
  (if (= here at) :back :copy))

(defn throw-once!
  "Takes a module's tag, the place in the gate that calls (:walk at the head
  of the walk, :back or :copy where a copy arrives), the task and a record's
  id. When `throw-at` names this id and this place for the tag (and this
  task, when it names one) and no throw was taken yet: notes it, sleeps 300 ms so that the copies already sent can
  be written on the other tasks, and throws. Else gives nil."
  [tag where task id]
  (let [at (get @throw-at tag)]
    (when (and (= id (:id at)) (= where (:where at))
               (or (nil? (:task at)) (= task (:task at))))
      (let [[before _] (swap-vals! claimed conj tag)]
        (when-not (contains? before tag)
          (note! tag :throw task id 1)
          (Thread/sleep 300)
          (swap! thrown assoc tag (System/nanoTime))
          (throw (ex-info "PROBE: thrown on purpose, once, by the probe" {:tag tag :id id :where where})))))))

;; ---- the gate ------------------------------------------------------------------

(def ^:private gate-head
  "The source of the walk."
  '[(source> *acts {:start-from :beginning} :> %batch)
    (%batch :> {:keys [*layer *id *key *val *replaces]})
    (ops/current-microbatch-id :> *batch)
    (ops/current-task-id :> *from)])

(def ^:private gate-hop
  "The hop that only a module built with `hop?` has."
  '[(|hash *layer)])

(def ^:private gate-walk
  "Steps 1 to 4."
  '[(ops/current-task-id :> *at)
    (burn! *layer)
    (<<if (yields? *layer)
      (yield-if-overtime))
    (throw-once! tag :walk *at *id)
    (local-select> (keypath *id) $$answers :> *seen)
    (<<if (some? *seen)
      (note! tag :repeat *at *id 0)
     (else>)
      (vector *layer *key :> *rk)
      (local-select> (keypath *rk) $$rows :> *held)
      (verdict-of *held *replaces :> *verdict)
      (local-select> [(keypath *layer) (nil->val 0)] $$last :> *was)
      (inc *was :> *n)
      (attempt-of tag :> *attempt)
      (local-transform> [(keypath *layer) (termval *n)] $$last)
      (hash-map :verdict *verdict :n *n :> *answer)
      (local-transform> [(keypath *id) (termval *answer)] $$answers)
      (hash-map :batch *batch :from *from :at *at :> *where)
      (local-transform> [(keypath *id) (termval *where)] $$trace)
      (note! tag *verdict *at *id *attempt)
      (<<if (= *verdict :accept)
        (hash-map :val *val :id *id :n *n :> *row)
        (vector *attempt *id :> *mark)
        (local-transform> [(keypath *rk) (termval *row)] $$rows)
        (local-transform> [(keypath *mark) (termval *at)] $$attempts)
        (|all)
        (ops/current-task-id :> *here)
        (throw-once! tag (leg *here *at) *here *id)
        (<<if (writes-copy? *layer *here *at)
          (local-transform> [(keypath *rk) (termval *row)] $$rows)
          (local-transform> [(keypath *mark) (termval *at)] $$attempts)
          (note! tag :copy *here *id *attempt)
         (else>)
          (note! tag :skip *here *id *attempt))))])

(defmacro ^:private gate-sources
  "Takes the symbol of a microbatch topology and whether the walk has the hop.
  Gives the `<<sources` form of the gate."
  [mb hop?]
  `(<<sources ~mb ~@gate-head ~@(when hop? gate-hop) ~@gate-walk))

(defn add-gate
  "Adds the microbatch topology `gate` and its tables to a module. Takes the
  module's `topologies`, the tag under which the module writes the journal,
  and whether the walk starts with `(|hash *layer)`. What must hold: the
  module has a depot or a mirror of one under the name `*acts`. The gate
  starts from the depot's beginning."
  [topologies tag hop?]
  (let [mb (microbatch-topology topologies "gate")]
    (declare-pstate mb $$rows {Object Object})
    (declare-pstate mb $$answers {String Object})
    (declare-pstate mb $$last {Object Long})
    (declare-pstate mb $$trace {String Object})
    (declare-pstate mb $$attempts {Object Object})
    (if hop?
      (gate-sources mb true)
      (gate-sources mb false))))

(defn add-ping
  "Adds a depot `*ping`, a stream topology `ping` and the table `$$here` to a
  module. One record appended to `*ping` makes every task write its own number
  under the key \"task\". Nothing of the gate reads or writes them."
  [setup topologies]
  (declare-depot setup *ping :random)
  (let [s (stream-topology topologies "ping")]
    (declare-pstate s $$here {String Object})
    (<<sources s
      (source> *ping :> *p)
      (|all)
      (ops/current-task-id :> *t)
      (local-transform> [(keypath "task") (termval *t)] $$here))))

(defmodule Gate [setup topologies]
  (declare-depot setup *acts (hash-by :layer))
  (add-ping setup topologies)
  (add-gate topologies "gate" false))

(defmodule Twin [setup topologies]
  (declare-depot setup *acts (hash-by :layer))
  (add-ping setup topologies)
  (add-gate topologies "twin" false))

(defmodule Fresh [setup topologies]
  (declare-depot setup *acts (hash-by :layer))
  (add-ping setup topologies)
  (add-gate topologies "fresh" false))

(defmodule Again [setup topologies]
  (mirror-depot setup *acts "probe.gate/Gate" "*acts")
  (add-ping setup topologies)
  (add-gate topologies "again" false))

(defmodule AgainHop [setup topologies]
  (mirror-depot setup *acts "probe.gate/Gate" "*acts")
  (add-ping setup topologies)
  (add-gate topologies "again-hop" true))

;; ---- the log -----------------------------------------------------------------

(defonce ^{:doc "The open writer of the run's log file, or nil."} log-writer (atom nil))

(defn say
  "Takes parts of a line. Writes them as one line to the run's log and to
  stdout behind the word PROBE."
  [& parts]
  (let [line (str/join " " parts)]
    (println (str "PROBE " line))
    (when-let [^java.io.Writer w @log-writer]
      (.write w line) (.write w "\n") (.flush w))))

(defn yes-no
  "Takes a truth. Gives the word for the log."
  [x]
  (if x "yes" "NO"))

(defn run-probe!
  "Takes the name of the log file under logs/ (or a whole path that starts
  with a slash, for a trial run whose log is not kept) and a function of no
  arguments that is the probe. Runs it and ends the JVM: status 0 when the
  probe reached its end, 1 when something threw."
  [log-name probe]
  (let [path (if (str/starts-with? log-name "/") log-name (str "logs/" log-name ".log"))]
    (io/make-parents path)
    (reset! log-writer (io/writer path))
    (let [status (try
                   (probe)
                   0
                   (catch Throwable t
                     (say "THE RUN DID NOT REACH ITS END:" (str t))
                     (.printStackTrace t)
                     1))]
      (when-let [^java.io.Writer w @log-writer] (.close w))
      (shutdown-agents)
      (System/exit status))))

;; ---- the hand: launch, find the tasks, send, wait, read -------------------------

(defn launch!
  "Takes the cluster, a module and a number of tasks. Launches the module on 2
  task threads in 2 workers, as the probes of 13 October do."
  [ipc module tasks]
  (rtest/launch-module! ipc module {:tasks tasks :threads 2 :workers 2}))

(defn- pkeys-by-task
  "Takes the cluster and a launched module's name. Appends one record to
  `*ping`, then asks `$$here` under one candidate key after another on which
  task the read landed. Gives task -> a partitioning key that lands on it, for
  every task of the module."
  [ipc module-name]
  (let [here (foreign-pstate ipc module-name "$$here")
        tasks (:num-partitions (foreign-object-info here))]
    (foreign-append! (foreign-depot ipc module-name "*ping") "ping" :ack)
    (loop [i 0 found (sorted-map)]
      (cond
        (= (count found) tasks) found
        (> i 5000) (throw (ex-info "no partitioning key found for every task" {:found found :tasks tasks}))
        :else (let [k (str "pk-" i)
                    t (foreign-select-one (keypath "task") here {:pkey k})]
                (recur (inc i) (if (or (nil? t) (contains? found (long t))) found (assoc found (long t) k))))))))

(defn open-module
  "Takes the cluster, a launched module and whether it owns `*acts`. Gives
  what the hand needs: the cluster, the module's name, a partitioning key for
  every task, a count of the records sent, and the depot when it is owned."
  [ipc module owner?]
  (let [module-name (get-module-name module)]
    {:ipc ipc
     :name module-name
     :pkeys (pkeys-by-task ipc module-name)
     :sent (atom 0)
     :depot (when owner? (foreign-depot ipc module-name "*acts"))}))

(defn tasks-of
  "Takes an opened module. Gives its tasks' numbers."
  [m]
  (keys (:pkeys m)))

(defn table
  "Takes an opened module, a table's name and a task. Gives everything that
  the table holds on that task, read with one select."
  [m table-name task]
  (into {} (map (fn [[k v]] [k v]))
        (foreign-select ALL (foreign-pstate (:ipc m) (:name m) table-name)
                        {:pkey (get (:pkeys m) task)})))

(def gate-tables
  "The gate's own three tables."
  ["$$rows" "$$answers" "$$last"])

(defn read-tables
  "Takes an opened module and tables' names. Gives task -> table's name ->
  what it holds on that task."
  [m table-names]
  (into (sorted-map)
        (for [t (tasks-of m)]
          [t (into {} (for [n table-names] [n (table m n t)]))])))

(defn key-task
  "Takes an opened module and any key. Gives the task on which a read with
  that partitioning key lands."
  [m k]
  (long (foreign-select-one (keypath "task") (foreign-pstate (:ipc m) (:name m) "$$here") {:pkey k})))

(defn send!
  "Takes an opened module that owns the depot and records. Appends them one
  after the other from this thread, each waiting only for the depot."
  [m records]
  (doseq [r records]
    (foreign-append! (:depot m) r :append-ack))
  (swap! (:sent m) + (count records)))

(defn send-from-threads!
  "Takes an opened module that owns the depot and groups of chains. One thread
  for each group: it sends one record of each of its chains in turn, so every
  chain enters the depot in its own order while the threads interleave."
  [m groups]
  (let [senders (doall (for [chains groups]
                         (future (doseq [r (apply interleave chains)]
                                   (foreign-append! (:depot m) r :append-ack)))))]
    (run! deref senders)
    (swap! (:sent m) + (reduce + (map count (apply concat groups))))))

(defn settle!
  "Takes an opened module and, for a module that mirrors, the number of
  records to wait for. Waits until its gate has processed that many."
  ([m] (settle! m @(:sent m)))
  ([m n] (rtest/wait-for-microbatch-processed-count (:ipc m) (:name m) "gate" n 900000)))

(defn pause!
  "Takes an opened module. Pauses its gate."
  [m]
  (rtest/pause-microbatch-topology! (:ipc m) (:name m) "gate"))

(defn resume!
  "Takes an opened module. Lets its gate run."
  [m]
  (rtest/resume-microbatch-topology! (:ipc m) (:name m) "gate"))

;; ---- the records ---------------------------------------------------------------

(defn id-of
  "Takes a layer and a record's place in the layer. Gives the record's id."
  [layer i]
  (format "%s-%05d" layer i))

(defn index-of
  "Takes a record's id. Gives its place in its layer."
  [id]
  (Long/parseLong (subs id (inc (str/last-index-of id "-")))))

(defn layer-of
  "Takes a record's id. Gives its layer."
  [id]
  (subs id 0 (str/last-index-of id "-")))

(defn key-name
  "Takes a key's number. Gives the key."
  [j]
  (format "k-%02d" j))

(defn chain
  "Takes a layer, a number of records and a number of keys. Gives the records
  in their order: record i is on key (i mod keys) and says that it replaces
  the record before it on that key, so the keys' chains are interleaved."
  [layer n-records n-keys]
  (vec (for [i (range n-records)]
         {:layer layer
          :id (id-of layer i)
          :key (key-name (mod i n-keys))
          :val i
          :replaces (when (>= i n-keys) (id-of layer (- i n-keys)))})))

(defn last-rows
  "Takes a layer, a number of records and a number of keys. Gives [layer key]
  -> the row that the last record of that key's chain writes when every
  record is accepted in order."
  [layer n-records n-keys]
  (into {} (for [j (range (min n-keys n-records))
                 :let [i (+ j (* n-keys (quot (- n-records 1 j) n-keys)))]]
             [[layer (key-name j)] {:val i :id (id-of layer i) :n (inc i)}])))

(defn six-layers
  "Takes an opened module with 4 tasks. Gives six layer names, found by asking
  where each candidate lands: two on partition 0, one on 1, one on 2, two on 3."
  [m]
  (let [want {0 2, 1 1, 2 1, 3 2}]
    (loop [i 0 got {0 [] 1 [] 2 [] 3 []}]
      (cond
        (= want (into {} (for [[t names] got] [t (count names)]))) (vec (mapcat got [0 1 2 3]))
        (> i 5000) (throw (ex-info "no six layers found" {:got got}))
        :else (let [layer (format "layer%02d" i)
                    t (key-task m layer)]
                (recur (inc i) (if (< (count (got t)) (want t)) (update got t conj layer) got)))))))

(defn send-six!
  "Takes an opened module that owns the depot, six layers as `six-layers`
  gives them, and the numbers of records and keys of each. Sends them from 3
  threads, two layers to a thread. The two layers of partition 0 go from two
  threads, and so do the two of partition 3."
  [m [a0 b0 c1 d2 e3 f3] n-records n-keys]
  (let [c #(chain % n-records n-keys)]
    (send-from-threads! m [[(c a0) (c c1)] [(c b0) (c e3)] [(c d2) (c f3)]])))

;; ---- what the tables hold of one layer, pure ------------------------------------

(defn- of-layer
  "Takes a layer and a table keyed by record id. Gives its entries of that layer."
  [layer by-id]
  (into {} (filter (fn [[id _]] (= layer (layer-of id)))) by-id))

(defn layer-facts
  "Takes what `read-tables` gave (with `$$trace`), a layer and the numbers of
  its records and keys. Gives what the tables hold of it. Under :rows, for
  every task, how many keys hold: the row of the last record of the key's
  chain, the same as the deciding task (:last); the deciding task's row, which
  is not that last row (:not-last); a row other than the deciding task's
  (:other); no row (:none)."
  [tables layer n-records n-keys]
  (let [answers-by-task (into (sorted-map) (for [[t tb] tables] [t (of-layer layer (tb "$$answers"))]))
        answers (apply merge (vals answers-by-task))
        trace (apply merge (for [[_ tb] tables] (of-layer layer (tb "$$trace"))))
        expected (last-rows layer n-records n-keys)
        deciders (vec (for [[t a] answers-by-task :when (seq a)] t))
        decider (first deciders)
        decider-rows (get-in tables [decider "$$rows"])
        batches (frequencies (map :batch (vals trace)))]
    {:deciders deciders
     :from (vec (sort (distinct (map :from (vals trace)))))
     :answers answers
     :answered (reduce + (map count (vals answers-by-task)))
     :accepted (count (filter #(= :accept (:verdict %)) (vals answers)))
     :refused (count (filter #(= :refuse (:verdict %)) (vals answers)))
     :in-order? (every? (fn [i] (= (inc i) (:n (get answers (id-of layer i))))) (range n-records))
     :last (vec (for [[t tb] tables :let [n (get (tb "$$last") layer)] :when n] [t n]))
     :microbatches (count batches)
     :most-in-one (if (seq batches) (apply max (vals batches)) 0)
     :rows (into (sorted-map)
                 (for [[t tb] tables]
                   [t (frequencies
                       (for [[rk want] expected
                             :let [held (get (tb "$$rows") rk)]]
                         (cond
                           (nil? held) :none
                           (not= held (get decider-rows rk)) :other
                           (= held want) :last
                           :else :not-last)))]))}))

(defn all-accepted?
  "Takes what `layer-facts` gave and the layer's number of records. Says
  whether every record was answered once, accepted, and numbered in order."
  [facts n-records]
  (and (= n-records (:answered facts) (:accepted facts))
       (:in-order? facts)
       (= 1 (count (:deciders facts)))))

(defn every-task-as-decider?
  "Takes what `layer-facts` gave. Says whether every task holds, for every
  key, the row that the deciding task holds."
  [facts]
  (every? (fn [[_ counts]] (and (zero? (get counts :other 0)) (zero? (get counts :none 0)))) (:rows facts)))

(defn every-task-last?
  "Takes what `layer-facts` gave and the layer's number of keys. Says whether
  every task holds the last row of every key."
  [facts n-keys]
  (every? (fn [[_ counts]] (= counts {:last n-keys})) (:rows facts)))

;; ---- what the journal holds of one layer, pure ----------------------------------

(defn journal-of
  "Takes a module's tag and a layer. Gives the journal's entries of them, in
  the journal's order."
  [tag layer]
  (filterv (fn [[t _ _ id]] (and (= t tag) (= layer (layer-of id)))) @journal))

(defn copy-order
  "Takes a layer's journal entries. Gives task -> how many copies were written
  there and how many of them came out of the order in which the deciding task
  accepted their records."
  [entries]
  (let [place (zipmap (for [[_ kind _ id] entries :when (= kind :accept)] id) (range))]
    (into (sorted-map)
          (for [[t es] (group-by #(nth % 2) (filter #(= :copy (second %)) entries))
                :let [places (map #(place (nth % 3)) es)]]
            [t {:written (count es)
                :out-of-order (count (filter (fn [[a b]] (< b a)) (partition 2 1 places)))}]))))

(defn decider-walk
  "Takes a layer's journal entries, the deciding task and the layer's number
  of keys. Follows the deciding task's own entries in the order in which it
  ran them. Gives how many decisions it made on a record that stands in the
  depot before the record it decided just before, in how many stretches it
  decided (a stretch ends when a copy of its own comes back to it), how many
  of its own copies set a key back to an older row, and how many decisions it
  made while the key did not stand on the record before."
  [entries decider n-keys]
  (-> (reduce
       (fn [st [_ kind task id]]
         (if (not= task decider)
           st
           (let [i (index-of id)
                 k (mod i n-keys)
                 held (get-in st [:row k])]
             (case kind
               (:accept :refuse)
               (cond-> (-> st
                           (update :out-of-depot-order + (if (< i (:before st)) 1 0))
                           (assoc :before i)
                           (update :stretches + (if (:deciding? st) 0 1))
                           (assoc :deciding? true)
                           (update :decided-on-older + (if (= held (when (>= i n-keys) (- i n-keys))) 0 1)))
                 (= kind :accept) (assoc-in [:row k] i))
               :copy (-> st
                         (assoc :deciding? false)
                         (update :set-back + (if (and held (> held i)) 1 0))
                         (assoc-in [:row k] i))
               :skip (assoc st :deciding? false)
               st))))
       {:row {} :before -1 :out-of-depot-order 0 :stretches 0 :set-back 0 :decided-on-older 0 :deciding? false}
       entries)
      (select-keys [:out-of-depot-order :stretches :set-back :decided-on-older])))

(defn decision-runs
  "Takes a layer's journal entries and the deciding task. Gives the order in
  which that task decided the layer's records, as runs: each [from to] is a
  stretch of records decided one after the other in the depot's order."
  [entries decider]
  (reduce (fn [runs i]
            (let [[from to] (peek runs)]
              (if (and to (= i (inc to)))
                (conj (pop runs) [from i])
                (conj runs [i i]))))
          []
          (for [[_ kind task id] entries
                :when (and (= task decider) (#{:accept :refuse} kind))]
            (index-of id))))

(defn threads-by-task
  "Takes journal entries. Gives task -> the names of the threads that ran it."
  [entries]
  (into (sorted-map) (for [[t es] (group-by #(nth % 2) entries)] [t (vec (sort (distinct (map #(nth % 5) es))))])))

;; ---- one chain of one layer, sent, waited for, read and written to the log -------

(defn- watch-reads
  "Takes an opened module, a task and an atom that turns true when to stop.
  Reads `$$here` on that task from this thread, again and again, 1 ms apart.
  Gives how many reads it made and how long the slowest waited, in ms. A read
  is an event on the task's thread, so it waits while a gate event runs there."
  [m task stop?]
  (let [here (foreign-pstate (:ipc m) (:name m) "$$here")
        pk (get (:pkeys m) task)]
    (loop [n 0 worst 0]
      (if @stop?
        {:reads n :slowest-ms (/ worst 1e6)}
        (let [t0 (System/nanoTime)]
          (foreign-select-one (keypath "task") here {:pkey pk})
          (let [d (- (System/nanoTime) t0)]
            (Thread/sleep 1)
            (recur (inc n) (max worst d))))))))

(defn chain-scenario!
  "Takes an opened module that owns the depot, its tag, a layer, the numbers
  of records and keys, and :live or :paused. Sends the layer's chain (with the
  gate running, or paused until all of it is in the depot), waits for the
  gate, reads every table on every task and writes what they hold to the log.
  Empties the journal at its end. Gives the facts."
  [m tag layer n-records n-keys how]
  (reset! journal [])
  (let [records (chain layer n-records n-keys)
        stop? (atom false)
        _ (when (= how :paused) (pause! m) (send! m records))
        watcher (future (watch-reads m (key-task m layer) stop?))
        t0 (System/nanoTime)
        _ (if (= how :paused) (resume! m) (send! m records))
        _ (settle! m)
        took-ms (/ (- (System/nanoTime) t0) 1e6)
        _ (reset! stop? true)
        watched @watcher]
    (let [tables (read-tables m ["$$rows" "$$answers" "$$last" "$$trace"])
          facts (layer-facts tables layer n-records n-keys)
          entries (journal-of tag layer)
          decider (first (:deciders facts))
          copies (copy-order entries)
          walked (decider-walk entries decider n-keys)]
      (say "")
      (say (str (str/upper-case layer) ":") n-records "records of one layer, a chain on each of" n-keys
           "keys, appended" (if (= how :paused) "while the gate was paused." "while the gate ran.")
           "The deciding task skips its own copy:" (str (if (skips? layer) "yes" "no") ".")
           "The walk calls (yield-if-overtime):" (str (if (yields? layer) "yes" "no") ".")
           "The walk burns 200 microseconds a record:" (str (if (slow? layer) "yes" "no") "."))
      (say "  Depot partition:" (:from facts) ". Deciding task:" (:deciders facts)
           ". Answers:" (:answered facts) ". Accepted:" (:accepted facts) "of" n-records
           ". Refused:" (:refused facts) ". Numbered 1 to" n-records "in the depot's order:" (yes-no (:in-order? facts))
           ". `$$last`:" (:last facts) ".")
      (say "  Microbatches:" (:microbatches facts) ". The most records in one:" (:most-in-one facts) ".")
      (doseq [[t counts] (:rows facts)]
        (say (format "  Task %d%s: keys that hold the last row of their chain, with the deciding task's number: %d of %d. The deciding task's row, which is not the last of the chain: %d. A row other than the deciding task's: %d. No row: %d. Copies written here: %d, out of the order in which they were sent: %d."
                     t (if (= t decider) " (deciding)" "")
                     (get counts :last 0) n-keys (get counts :not-last 0) (get counts :other 0) (get counts :none 0)
                     (get-in copies [t :written] 0) (get-in copies [t :out-of-order] 0))))
      (say "  The deciding task, by the probe's journal: decisions made out of the depot's order:" (:out-of-depot-order walked)
           ". It decided in" (:stretches walked) "stretches (a stretch ends when a copy of its own comes back)."
           "Its own copies that set a key back to an older row:" (:set-back walked)
           ". Decisions made while the key did not stand on the record before:" (:decided-on-older walked) ".")
      (let [runs (decision-runs entries decider)]
        (say "  The order in which the deciding task decided, as runs of records in the depot's order:"
             (str/join ", " (for [[from to] (take 14 runs)] (str from ".." to)))
             (if (> (count runs) 14) (str "... (" (count runs) " runs)") (str "(" (count runs) " runs)"))))
      (say (format "  From %s to the gate's end: %.0f ms. A client's read of the deciding task, repeated 1 ms apart in that time: %d reads, the slowest waited %.1f ms."
                   (if (= how :paused) "the gate's release" "the first append") took-ms (:reads watched) (:slowest-ms watched)))
      (say "  Threads, by task:" (pr-str (threads-by-task entries)))
      (reset! journal [])
      (assoc facts :copies copies :walk walked :n-records n-records :n-keys n-keys))))

(defn copies-in-order?
  "Takes the facts that `chain-scenario!` gave. Says whether every task other
  than the deciding one wrote every accepted record's copy, in the order sent."
  [facts]
  (let [decider (first (:deciders facts))
        others (remove #{decider} (keys (:rows facts)))]
    (every? (fn [t] (and (= (:accepted facts) (get-in facts [:copies t :written] 0))
                         (zero? (get-in facts [:copies t :out-of-order] 0))))
            others)))

;; ---- two modules' tables held against each other, pure ---------------------------

(defn differences
  "Takes two tables. Gives how many keys hold another value in one than in the
  other, a key that only one of them has counted too."
  [a b]
  (count (filter (fn [k] (not= (get a k ::none) (get b k ::none)))
                 (distinct (concat (keys a) (keys b))))))

(defn union
  "Takes what `read-tables` gave and a table's name. Gives that table's
  entries of all tasks in one map."
  [tables table-name]
  (apply merge (for [[_ tb] tables] (tb table-name))))

(defn entries
  "Takes what `read-tables` gave and a table's name. Gives how many entries
  the table holds on all tasks together, an entry on two tasks counted twice."
  [tables table-name]
  (reduce + (for [[_ tb] tables] (count (tb table-name)))))

(defn microbatches
  "Takes what `read-tables` gave (with `$$trace`). Gives in how many
  microbatches the module decided, and the most records in one."
  [tables]
  (let [sizes (frequencies (map :batch (vals (union tables "$$trace"))))]
    {:microbatches (count sizes) :most-in-one (if (seq sizes) (apply max (vals sizes)) 0)}))

(defn source-tasks
  "Takes the tables of the module that owns the depot and of another module
  that read the same records (both with `$$trace`). In the owner a record is
  emitted on the task of its depot partition. Gives depot partition -> the
  tasks on which the other module's source emitted that partition's records."
  [owner-tables other-tables]
  (let [owner-trace (union owner-tables "$$trace")
        other-trace (union other-tables "$$trace")]
    (into (sorted-map)
          (for [[p ids] (group-by #(:from (owner-trace %)) (keys owner-trace))]
            [p (vec (sort (distinct (map #(:from (other-trace %)) ids))))]))))

(defn error-text
  "Takes what was thrown. Gives the class and message of it and of every
  cause under it, in one line."
  [^Throwable t]
  (str/join " | CAUSED BY: "
            (for [^Throwable c (take-while some? (iterate #(.getCause ^Throwable %) t))]
              (str (.getName (class c)) ": " (.getMessage c)
                   (when-let [d (ex-data c)] (str " " (pr-str d)))))))
