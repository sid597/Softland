(ns probe.stream
  "Run S and run O of the probe of the plan `answers-in-a-log`.

  S asks what the plan's keeping does in a stream topology that has the
  shape of the store's streaming gate, on Rama 1.6.0 in-process. The stream
  probe of 12 October (`../stream-probe/`) ran the append in twelve ways on
  an event that always writes. The plan turns on one way, write, commit
  boundary, append with :append-ack, on an event that first reads whether
  its key has a row (the gate's check (a)) and, when it has, writes nothing
  and appends a copy of that row. That shape was not run. S runs it, with a
  failure forced at each point of the event.

  O asks whether Rama's setting `topology.stream.max.executing.per.task` can
  be set to 1 for one run from outside a module's own code, by a second
  module that calls the first one's definition, and what the setting then
  does to the events of one task.

  A probe, not a design: the module stands in for the gate. It requires
  nothing of the store's tree. No table, name or form here is a proposal
  for the store. The event's functions read atoms of this JVM, which works
  only because the in-process cluster shares the JVM with the run."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.ops :as ops]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [probe.util :as u])
  (:import [com.rpl.rama RamaModule]))

;; ---- what the event notes, in the memory of this JVM ----------------------

(defonce ^{:doc "record id → how many times its event began."} attempts (atom {}))
(defonce ^{:doc "task → the record ids whose event began and has not ended."} pending (atom {}))
(defonce ^{:doc "The most records of one task that were pending at once."} most-pending (atom 0))
(defonce ^{:doc "Each forced throw: {:rid :attempt :point}."} throws (atom []))

(defn begin!
  "The event calls it first. Takes the thing and the task. Counts the
  attempt of the thing's record and notes the record as pending on the task.
  Gives the note of the attempt, {:attempt n}."
  [thing task]
  (let [rid (:rid thing)
        n (get (swap! attempts update rid (fnil inc 0)) rid)
        now (get (swap! pending update task (fnil conj #{}) rid) task)]
    (swap! most-pending max (count now))
    {:attempt n}))

(defn point!
  "The event calls it at each point. Takes the point, the thing and the note
  of the attempt. Throws when the thing asks for a failure at that point on
  this attempt, and says so first."
  [point thing note]
  (when (and (= point (:fail-at thing)) (<= (:attempt note) (:fail-first thing 0)))
    (swap! throws conj {:rid (:rid thing) :attempt (:attempt note) :point point})
    (u/say "INSIDE " (:rid thing) "attempt" (:attempt note) "THROWS on purpose at" (name point))
    (throw (ex-info "probe: forced failure" {:rid (:rid thing) :point point})))
  nil)

(defn end!
  "The event calls it last. Takes the thing and the task. The record is no
  longer pending."
  [thing task]
  (swap! pending update task disj (:rid thing))
  nil)

(defn decided-row
  "Takes the task's last number or nil, the note of the attempt and the
  thing. Gives the row that the event writes under the thing's id: the next
  number, and which record and which attempt decided it."
  [last-number note thing]
  {:stamp (inc (or last-number 0)) :by-rid (:rid thing) :by-attempt (:attempt note)})

(defn entry-of
  "Takes the thing, the row to keep, the note of the attempt and where the
  row came from, :decided or :copy. Gives the record for the second depot:
  the id, the row, and which record and attempt appended it."
  [thing row note from]
  {:id (:id thing) :row row :from from :appended-by (:rid thing) :appended-in (:attempt note)})

;; ---- the module -----------------------------------------------------------

(defmodule KeepStream [setup topologies]
  (declare-depot setup *things (hash-by :k))
  (declare-depot setup *kept :disallow)
  (let [s (stream-topology topologies "gate")]
    (declare-pstate s $$rows {String Object})
    (declare-pstate s $$task {String Object})
    (<<sources s
      (source> *things {:start-from :beginning :retry-mode :all-after} :> *thing)
      (ops/current-task-id :> *task)
      (begin! *thing *task :> *note)
      (get *thing :id :> *id)
      ;; check (a): a key that has a row is not decided again
      (local-select> (keypath *id) $$rows :> *held)
      (<<if (nil? *held)
        (point! :before-writes *thing *note)
        (local-select> (keypath "last") $$task :> *last)
        (decided-row *last *note *thing :> *row)
        (get *row :stamp :> *stamp)
        (local-transform> [(keypath *id) (termval *row)] $$rows)
        (local-transform> [(keypath "last") (termval *stamp)] $$task)
        (point! :after-writes *thing *note)
        (identity *row :> *to-keep)
        (identity :decided :> *from)
       (else>)
        (identity *held :> *to-keep)
        (identity :copy :> *from))
      ;; the keeping: a commit boundary, then the append
      (|direct (ops/current-task-id))
      (point! :after-boundary *thing *note)
      (entry-of *thing *to-keep *note *from :> *entry)
      (depot-partition-append! *kept *entry :append-ack)
      (point! :after-append *thing *note)
      (end! *thing *task))))

(defmodule KeepStreamAtOne [setup topologies]
  ;; The same module, defined by the first one's own definition, with one
  ;; option of Rama set at the launch. Nothing of the first module's code
  ;; names the option.
  (.define ^RamaModule KeepStream setup topologies)
  (set-launch-topology-dynamic-option! setup "gate" "topology.stream.max.executing.per.task" 1))

;; ---- the hand -------------------------------------------------------------

(def ^:dynamic *round* nil)

(defn- open-round
  "Takes the cluster and the module. Gives the clients of the run."
  [ipc module]
  (let [m (get-module-name module)]
    {:ipc ipc :module m
     :things (foreign-depot ipc m "*things")
     :kept (foreign-depot ipc m "*kept")
     :rows (foreign-pstate ipc m "$$rows")}))

(defn- row-of
  "Takes a thing. Gives the row under its id, read from outside, or nil."
  [thing]
  (foreign-select-one (keypath (:id thing)) (:rows *round*) {:pkey (:k thing)}))

(defn- all-entries
  "Gives every record of the second depot, each with :partition and :offset, in the order of each partition."
  []
  (let [kept (:kept *round*)]
    (vec (for [p (range (:num-partitions (foreign-object-info kept)))
               :let [{:keys [start-offset end-offset]} (foreign-depot-partition-info kept p)]
               [i r] (map-indexed vector
                                  (loop [at start-offset out []]
                                    (if (>= at end-offset)
                                      out
                                      (let [upto (min end-offset (+ at 500))]
                                        (recur upto (into out (foreign-depot-read kept p at upto)))))))]
           (assoc r :partition p :offset (+ start-offset i))))))

(defn- entries-of
  "Takes an id. Gives its records in the second depot, in order."
  [id]
  (filterv #(= id (:id %)) (all-entries)))

(defn- send!
  "Appends a thing with :ack on another thread, so that a failure of the event does not stop the hand. Gives the future of what the append gave."
  [thing]
  (future (try (foreign-append! (:things *round*) thing :ack)
               (catch Throwable t [:threw (u/root-message t)]))))

(defn- steady
  "Takes a function of no arguments that reads from outside. Calls it until
  it does not throw, for 10 seconds at most: for a time after a worker is
  started again a read can throw. Gives what it gave, or [:read-threw message]."
  [f]
  (loop [n 0]
    (let [r (try [(f)] (catch Throwable t (when (>= n 200) [[:read-threw (u/root-message t)]])))]
      (if r (first r) (do (Thread/sleep 50) (recur (inc n)))))))

(defn- short
  "Takes a record of the second depot. Gives it in few words."
  [e]
  [(:from e) (str "row by " (:by-rid (:row e)) "#" (:by-attempt (:row e)) " stamp " (:stamp (:row e)))
   (str "appended by " (:appended-by e) "#" (:appended-in e)) (str "p" (:partition e) "@" (:offset e))])

(defn- scenario
  "Sends the things, waits until each has a row and until `done?` of the
  records of the ids holds, and says what the table and the second depot
  hold of each id. Takes a name, the things, and a function of the records
  of all the scenario's ids that says when to look."
  [label things done?]
  (u/say "RUN     ----" label "----")
  (let [ids (distinct (map :id things))
        acks (mapv send! things)
        of-ids (fn [] (filterv #(contains? (set ids) (:id %)) (all-entries)))
        ready (u/wait-until (fn [] (and (every? row-of things) (done? (of-ids)))) 90000)]
    (Thread/sleep 300)
    (doseq [a acks]
      (let [got (deref a 30000 :no-answer)]
        (when (vector? got) (u/say "CLIENT  an :ack append gave" (pr-str got)))))
    (doseq [id ids]
      (let [thing (first (filter #(= id (:id %)) things))
            row (steady #(row-of thing))
            es (steady #(entries-of id))]
        (u/say "RESULT " id "· ready:" (boolean ready) "· the table holds:" (pr-str row)
               "· records:" (count es) (pr-str (mapv short es))
               "· every record holds the table's row:" (every? #(= row (:row %)) es))))))

(defn- the-whole
  "Says, of everything that the run sent: how many ids have a row, how many
  records the second depot holds, how many records hold a row that is not
  the table's row of their id, how many rows have no record, and how the
  count of records is spread over the ids."
  [things]
  (let [by-id (group-by :id (steady all-entries))
        rows (into {} (map (fn [t] [(:id t) (steady #(row-of t))])) things)
        differ (for [[id es] by-id e es :when (not= (get rows id) (:row e))] [id (short e)])
        no-record (remove #(contains? by-id %) (keys rows))]
    (u/say "WHOLE   ids with a row:" (count (remove nil? (vals rows))) "of" (count rows)
           "· records in the second depot:" (reduce + (map count (vals by-id)))
           "· records that hold another row than the table's:" (count differ) (pr-str (take 5 differ))
           "· rows with no record:" (count no-record) (pr-str (take 5 no-record)))
    (u/say "WHOLE   how many ids have 1, 2, 3 ... records:" (pr-str (into (sorted-map) (frequencies (map count (vals by-id))))))
    (u/say "WHOLE   events begun in all:" (reduce + (vals @attempts)) "for" (count @attempts) "source records · forced throws:" (count @throws))))

(defn- run-fail
  "Run S."
  []
  (u/open-log! "logs/stream.log")
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc KeepStream {:tasks 4 :threads 2 :workers 2})
    (binding [*round* (open-round ipc KeepStream)]
      (let [quiet (vec (for [i (range 20)] {:k (str "q" (mod i 4)) :id (str "Q" i) :rid (str "Q" i)}))
            one (fn [id m] (merge {:k "f" :id id :rid id} m))
            f1 [(one "F1" {:fail-at :after-writes :fail-first 1})]
            f2 [(one "F2" {:fail-at :after-boundary :fail-first 1})]
            f3 [(one "F3" {:fail-at :after-append :fail-first 1})]
            two [{:k "f" :id "E" :rid "E-a"} {:k "f" :id "E" :rid "E-b"}]
            behind [(one "HX" {}) (one "HY" {:fail-at :before-writes :fail-first 1})]
            again [(one "F3" {:rid "F3-sent-again"})]
            n-entries (fn [n] (fn [es] (<= n (count es))))]
        (scenario "S0 · 20 things, no failure: each is decided once" quiet (n-entries 20))
        (scenario "S1 · a throw after the writes, before the boundary" f1 (n-entries 1))
        (scenario "S2 · a throw after the boundary, before the append: the row is committed and no record is appended" f2 (n-entries 1))
        (scenario "S3 · a throw after the append" f3 (n-entries 2))
        (scenario "S4 · two records under one id, sent together" two (n-entries 2))
        (scenario "S5 · X and Y sent together; Y throws before its writes" behind (n-entries 2))
        (scenario "S6 · the thing of S3 sent again, as a second record under its id" again (n-entries 3))
        (Thread/sleep 1500)
        (the-whole (concat quiet f1 f2 f3 two behind))
        (u/say "WHOLE   each forced throw:" (pr-str @throws))))))

;; ---- run O ----------------------------------------------------------------

(defn- flood
  "Launches a module, sends 400 things for one task with :ack, none waiting
  for another, and says how long it took and how many records of one task
  were pending at once."
  [label module]
  (reset! attempts {}) (reset! pending {}) (reset! most-pending 0)
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc module {:tasks 4 :threads 2 :workers 2})
    (binding [*round* (open-round ipc module)]
      (deref (send! {:k "o" :id "warm" :rid "warm"}) 30000 nil)
      (reset! most-pending 0)
      (let [t0 (System/nanoTime)
            fs (mapv (fn [i] (foreign-append-async! (:things *round*) {:k "o" :id (str "O" i) :rid (str "O" i)} :ack))
                     (range 400))
            failed (count (filter (fn [f] (try (.get ^java.util.concurrent.CompletableFuture f 60 java.util.concurrent.TimeUnit/SECONDS) false
                                               (catch Throwable _ true)))
                                  fs))
            took (/ (- (System/nanoTime) t0) 1e6)]
        (u/say "ONE    " label "· module" (:module *round*) "· 400 things for one task, sent at once with :ack: all answered after"
               (format "%.1f ms" took) "· appends that threw:" failed
               "· the most records of one task pending at once:" @most-pending
               "· records in the second depot:" (count (all-entries)))))))

(defn- sizes
  "Says how many bytes Nippy makes of a record of each kind that the plan
  puts on a depot of answers, when Nippy is on the classpath. Rama's own
  writing of a record was not measured; this is an order of size."
  []
  (if-let [freeze (try (requiring-resolve 'taoensso.nippy/freeze) (catch Throwable _ nil))]
    (let [n (fn [x] (count (freeze x)))
          digest (byte-array 32)
          hex (apply str (repeat 64 "a"))
          accepted (fn [layer] {:act-id [layer :streaming (random-uuid)] :stamp 117423958537994240 :digest digest})
          refused (fn [layer] (assoc (accepted layer) :refused :permission-does-not-stand))]
      (u/say "SIZE    one record of an accepted act, the layer a keyword of 20 letters:" (n {:answers [(accepted :softland.layer/notes)]})
             "bytes · the layer a UUID:" (n {:answers [(accepted (random-uuid))]})
             "· of a refused act, the word of 25 letters:" (n {:answers [(refused :softland.layer/notes)]})
             "· of a stray:" (n {:strays [{:stray :no-act-id :digest hex}]})
             "· the bare answer with no wrapper:" (n (accepted :softland.layer/notes)))
      (u/say "SIZE    one record of 256 accepted acts of a batch:" (n {:batch 48211 :answers (vec (repeatedly 256 #(accepted :base)))})
             "bytes · of 1 accepted act of a batch:" (n {:batch 48211 :answers [(accepted :base)]})
             "· what a batch keeps of itself:" (n {:batch 48211 :clock 1791747415687 :last-stamp 117423958537994240})))
    (u/say "SIZE    Nippy is not on the classpath; no size was taken")))

(defn- run-one
  "Run O."
  []
  (u/open-log! "logs/one.log")
  (sizes)
  (flood "the module as it is defined" KeepStream)
  (flood "the same definition, with topology.stream.max.executing.per.task set to 1 at the launch" KeepStreamAtOne))

(defn -main
  "Takes one word: `fail` for run S, `one` for run O."
  [& [which]]
  (try
    (if (= "one" which) (run-one) (run-fail))
    (catch Throwable t
      (u/say "STOPPED" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
