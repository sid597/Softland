(ns probe.store-second-attempt
  "Run S: a real batch of the store's own microbatch gate made to run a
  second time, with no hook in the module's source, on the store as it
  stands today (no clock is kept yet). It is a probe for the plan of
  `base-for-a-reader`: it changes nothing under `src-store/`, and nothing
  here is a form for the store's proof.

  What it asks, for the proof of ruling 17 (d):

    S1  does a function of `softland.store.gate.batch` that the gate's
        dataflow calls, replaced in its var before the launch by a function
        that a var holds (the batch probe's B5.a), run inside the store's
        module, launched as the store's proofs launch it: 4 tasks, 2
        threads, 2 workers?
    S2  does a throw from it, late in step 4, make Rama run the batch again,
        and does the cluster of two workers come back so that the answers
        are readable?
    S3  can the proof learn what the first attempt made, without reading a
        table? Three functions are replaced by ones that call the first
        function and note what it gave: `base` (the clock and the base of
        each attempt with things), `decide-or-send` (each row of an act that
        is decided where it arrived) and `rows-to-send` (each row of an act
        that meets).
    S4  today, with the clock read anew by each attempt: in which fields do
        the rows of the two attempts differ? This is the stop test's other
        side: the check must be able to see a difference.

  What the JVM keeps: what each attempt made, and the run's wish to throw.
  Memory of this process; it lasts through the worker's restart only because
  the in-process cluster is one JVM.

  Run, from the repository root:
    clojure -Sdeps '{:aliases {:plan-probe {:extra-paths [\"src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-base-for-a-reader/src\"]}}}' -M:store-module:plan-probe -m probe.store-second-attempt
  The log is logs/store-second-attempt.log in this probe's folder."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.path :as path]
            [com.rpl.rama.test :as rtest]
            [probe.say :as s]
            [softland.store.gate.batch :as batch]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

;; ---- the first functions, taken before any var is changed ----------------

(def first-base "`batch/base` as the store defines it." batch/base)
(def first-decide-or-send "`batch/decide-or-send` as the store defines it." batch/decide-or-send)
(def first-rows-to-send "`batch/rows-to-send` as the store defines it." batch/rows-to-send)

;; ---- what the JVM keeps ----------------------------------------------------

(defonce ^{:doc "Each attempt of a batch with things, in order: {:t :clock :base :numbers :rows}."}
  attempts (atom []))

(defonce ^{:doc "The run's wish: {:throw? bool}."}
  wishes (atom {}))

(defn- note-rows!
  "Adds rows, each [which key row], to the newest attempt."
  [rows]
  (when (seq rows)
    (swap! attempts (fn [as] (if (seq as) (update-in as [(dec (count as)) :rows] into rows) as)))))

;; ---- the replacements: each is a function that a var holds ----------------

(defn noting-base
  "Calls the first `base`. For a batch with numbers it starts the note of a
  new attempt, with the clock that the gate read and the base."
  [now-millis last-stamp numbers]
  (let [base (first-base now-millis last-stamp numbers)]
    (when numbers
      (swap! attempts conj {:t (s/ms) :clock now-millis :base base :numbers numbers :rows []}))
    base))

(defn noting-decide-or-send
  "Calls the first `decide-or-send` and notes each row that it gave: the
  facts written at once, and the rows of each parcel of the kind :rows."
  [item own-rows]
  (let [out (first-decide-or-send item own-rows)]
    (note-rows! (concat (map (fn [[k row]] [:fact k row]) (:now out))
                        (mapcat (fn [[_ kind payload]] (when (= :rows kind) payload)) (:parcels out))))
    out))

(defn noting-rows-to-send
  "Calls the first `rows-to-send` and notes each row. Then, when the run
  asked for it, throws, this once."
  [pass]
  (let [out (first-rows-to-send pass)]
    (note-rows! (map (juxt :which :key :row) out))
    (let [[old _] (swap-vals! wishes assoc :throw? false)]
      (when (:throw? old)
        (s/say "INSIDE  rows-to-send THROWS on purpose, in step 4 of attempt" (count @attempts) "since the JVM began")
        (throw (ex-info "probe: forced failure of this attempt" {}))))
    out))

;; ---- the hand ---------------------------------------------------------------

(def system-own "The system's permission in the base." [:system :base :base])

(defn- thing
  "An act as a door sends it, into the base, by `who` under `permission`."
  [who permission facts]
  (let [act-id [:base :microbatch (UUID/randomUUID)]]
    {:act-id act-id :depot :microbatch :version 1 :who who :layer :base
     :permission permission :session :probe-session :facts (vec facts)}))

(defn- seen
  "Takes a value. Gives it with each run of bytes as a vector, so that two
  rows can be compared by their content."
  [x]
  (cond (bytes? x) (vec x)
        (map? x) (into {} (map (fn [[k v]] [k (seen v)])) x)
        (sequential? x) (mapv seen x)
        :else x))

(defn- by-key
  "Takes the rows that an attempt made. Gives a map from [which key] to the
  row, the last one noted for that key."
  [rows]
  (into {} (map (fn [[which k row]] [[which k] (seen row)])) rows))

(defn -main [& _]
  (s/open-log! "src/proposal/backend-on-rama-2026-10-02/reads/plan-probes-base-for-a-reader/logs/store-second-attempt.log")
  (try
    ;; B5.a: before the launch, a function that a var holds
    (alter-var-root #'batch/base (constantly noting-base))
    (alter-var-root #'batch/decide-or-send (constantly noting-decide-or-send))
    (alter-var-root #'batch/rows-to-send (constantly noting-rows-to-send))
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 2})
      (let [m       (rama/get-module-name module/Store)
            depot   (rama/foreign-depot ipc m "*microbatch-depot")
            answers (rama/foreign-query ipc m "answer-by-act-id")
            facts   (rama/foreign-pstate ipc m "$$microbatch-facts")
            answer-of (fn [x] (let [act-id (:act-id x)]
                                (rama/foreign-invoke-query answers (record/routing-id act-id) act-id)))
            word-of (fn [x] (let [a (answer-of x)]
                              (cond (nil? a) :nothing-yet
                                    (= :accepted (:verdict a)) :accepted
                                    :else (:reason a))))
            answered? (fn [xs limit] (boolean (s/wait-until #(every? (fn [x] (some? (answer-of x))) xs) limit)))
            making  (thing :system system-own
                           [{:e :base :k :softland.store/depot :v :microbatch}
                            {:e :system :k :softland.store/permission :v system-own}])
            entities (vec (repeatedly 6 #(UUID/randomUUID)))
            batch-things (-> (mapv (fn [e] (thing :system system-own [{:e e :k :note}])) entities)
                             ;; a give: its rows go to every task, by the meeting task
                             (conj (thing :system system-own
                                          [{:e :alice :k :softland.store/permission
                                            :v [:alice :base :base system-own]}]))
                             ;; an act whose permission no act gave: refused
                             (conj (thing :mallory [:mallory :base :base system-own] [{:e :m :k :note}])))]
        (s/say "RUN     Rama 1.6.0, in-process, the store's module `Store`, 4 tasks, 2 threads, 2 workers")
        (rama/foreign-append! depot making :append-ack)
        (s/say "OUTSIDE the making act of the base is answered:" (answered? [making] 30000) (word-of making))
        (s/say "INSIDE  attempts noted so far (S1: the replacements run inside the module):" (count @attempts))

        (let [before (count @attempts)]
          (rtest/pause-microbatch-topology! ipc m "microbatch-gate")
          (doseq [x batch-things] (rama/foreign-append! depot x :append-ack))
          (swap! wishes assoc :throw? true)
          (rtest/resume-microbatch-topology! ipc m "microbatch-gate")
          (let [t0 (s/ms)
                ok (answered? batch-things 90000)]
            (s/say "OUTSIDE each of the" (count batch-things) "things of the batch is answered:" ok
                   "·" (format "%.0f" (- (s/ms) t0)) "ms after the topology went on")
            (s/say "OUTSIDE the words:" (pr-str (frequencies (map word-of batch-things)))))
          (let [mine (subvec @attempts before)
                [a1 a2] mine
                r1 (by-key (:rows a1))
                r2 (by-key (:rows a2))
                differing (for [k (sort-by str (keys r1))
                                :let [x (get r1 k) y (get r2 k)]
                                :when (not= x y)]
                            [k (vec (for [f (distinct (concat (keys x) (keys y)))
                                          :when (not= (get x f) (get y f))]
                                      f))])]
            (s/say "RESULT  S2: attempts of batches with things since the topology went on:" (count mine))
            (doseq [[i a] (map-indexed vector mine)]
              (s/say "INSIDE  attempt" (inc i) (format "at %.1f" (:t a)) "· the clock" (:clock a) "· the base" (:base a)
                     "· numbers" (pr-str (:numbers a)) "· rows noted" (count (:rows a))
                     "· by kind" (pr-str (frequencies (map first (:rows a))))))
            (s/say "RESULT  S3: the two attempts made rows under the same keys:" (= (set (keys r1)) (set (keys r2)))
                   "·" (count r1) "keys in attempt 1," (count r2) "in attempt 2")
            (s/say "RESULT  S4: keys whose row differs between the two attempts:" (count differing) "of" (count r1)
                   "· the fields that differ, over all of them:" (pr-str (frequencies (mapcat second differing))))
            (s/say "RESULT  S4: the clocks differ by" (when (and a1 a2) (- (:clock a2) (:clock a1))) "ms · the bases by"
                   (when (and a1 a2) (- (:base a2) (:base a1))))
            ;; the tables against what the second attempt made, for the fact rows on their own tasks
            (let [in-table (into {}
                                 (for [e entities
                                       :let [k (record/place-key :base e :note)]]
                                   [[:fact k] (seen (rama/foreign-select-one (path/keypath k) facts {:pkey e}))]))
                  same-as-2 (count (filter (fn [[k row]] (= row (get r2 k))) in-table))
                  same-as-1 (count (filter (fn [[k row]] (= row (get r1 k))) in-table))]
              (s/say "RESULT  the" (count in-table) "fact rows in the table: the same as attempt 2 made:" same-as-2
                     "· the same as attempt 1 made:" same-as-1))))
        (s/say "RUN     END")))
    (catch Throwable t
      (s/say "RUN     THREW" (s/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
