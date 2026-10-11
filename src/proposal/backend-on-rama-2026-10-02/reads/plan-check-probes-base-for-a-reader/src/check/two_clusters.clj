(ns check.two-clusters
  "Run T, for the check of the plan of `base-for-a-reader`: the store's own
  module `Store` in three in-process clusters, one after the other, in one
  JVM. It is a probe for the check and no code of the store: it changes
  nothing under `src-store/`, and nothing here is a form for the store's
  proof.

  Ruling 49 of the build's page gives the check of the second attempt a
  cluster of its own; every other check of the module's proof runs on the
  functions as the store defines them, and the three replaced functions are
  in place only for that cluster and are put back after it. The papers ran
  one cluster in one JVM each time (the batch probe's B5, the plan's S). So
  this run asks:

    T1  do two clusters one after the other in one JVM work at all: the
        first is closed, the second is created, the same module object is
        launched again, and a batch is decided in each? The first cluster
        also starts the module again by a module update, as the module's
        proof does near its end.
    T2  a function of `softland.store.gate.batch` that the gate's dataflow
        calls is replaced in its var AFTER the module object was defined and
        AFTER the module was launched once in this JVM, and BEFORE the second
        launch: does the second cluster run the replacement?
    T3  the three functions are put back before a third launch: does the
        third cluster run the functions as the store defines them?
    T4  the plan has `rows-to-send` throw `on the first task that comes
        there`. `rows-to-send` runs on every task in step 4, and only the
        meeting task's call gives rows. Does the note of the first attempt
        always hold the rows of the acts that meet, or can the throw come
        before the meeting task's call? Each batch here holds one give and
        one act whose permission no act gave; both meet. Two ways to throw
        are run, several batches each: on the first call (as the plan has
        it), and on the first call that gives rows.
    T5  can a check tell two attempts of one batch from two batches without
        the clock? Each note keeps the last stamp that `base` was given.

  What the JVM keeps: what each attempt made, the run's wish to throw, and
  each throw. Memory of this process.

  Run, from the repository root:
    clojure -Sdeps '{:aliases {:check-probe {:extra-paths [\"src/proposal/backend-on-rama-2026-10-02/reads/plan-check-probes-base-for-a-reader/src\"]}}}' -M:store-module:check-probe -m check.two-clusters
  The log is logs/two-clusters.log in this probe's folder."
  (:require [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [check.say :as s]
            [softland.store.gate.batch :as batch]
            [softland.store.module :as module]
            [softland.store.record.rows :as record])
  (:import [java.util UUID]))

;; ---- the first functions, taken before any var is changed ----------------

(def first-base "`batch/base` as the store defines it." batch/base)
(def first-decide-or-send "`batch/decide-or-send` as the store defines it." batch/decide-or-send)
(def first-rows-to-send "`batch/rows-to-send` as the store defines it." batch/rows-to-send)

;; ---- what the JVM keeps ----------------------------------------------------

(defonce ^{:doc "Each attempt of a batch with things, in order: {:t :clock :last-stamp :base :numbers :rows}."}
  attempts (atom []))

(defonce ^{:doc "The run's wish: {:throw nil, :first-call or :first-call-with-rows}."}
  wishes (atom {}))

(defonce ^{:doc "Each throw that a replacement made: {:t :thread :rows-of-this-call :attempt}."}
  throws (atom []))

(defn- note-rows!
  "Adds rows, each [which key row], to the newest attempt."
  [rows]
  (when (seq rows)
    (swap! attempts (fn [as] (if (seq as) (update-in as [(dec (count as)) :rows] into rows) as)))))

;; ---- the replacements: each is a function that a var holds ----------------

(defn noting-base
  "Calls the first `base`. For a batch with numbers it starts the note of a
  new attempt: the clock and the last stamp that the gate gave it, and the
  base."
  [now-millis last-stamp numbers]
  (let [base (first-base now-millis last-stamp numbers)]
    (when numbers
      (swap! attempts conj {:t (s/ms) :clock now-millis :last-stamp last-stamp :base base
                            :numbers numbers :rows []}))
    base))

(defn noting-decide-or-send
  "Calls the first `decide-or-send` and notes each row that it gave: the
  facts written at once, and the rows of each parcel of the kind :rows."
  [item own-rows]
  (let [out (first-decide-or-send item own-rows)]
    (note-rows! (concat (map (fn [[k row]] [:fact k row]) (:now out))
                        (mapcat (fn [[_ kind payload]] (when (= :rows kind) payload)) (:parcels out))))
    out))

(defn- wish-after
  "Takes the wishes and whether this call of `rows-to-send` gave rows. Gives
  the wishes with the throw taken, when this call is the one to throw."
  [w gave-rows?]
  (let [mode (:throw w)]
    (if (or (= :first-call mode)
            (and (= :first-call-with-rows mode) gave-rows?))
      (assoc w :throw nil)
      w)))

(defn noting-rows-to-send
  "Calls the first `rows-to-send` and notes each row. Then, when the run
  asked for it and this call is the one that the wish names, throws, this
  once."
  [pass]
  (let [out (first-rows-to-send pass)
        gave-rows? (boolean (seq out))]
    (note-rows! (map (juxt :which :key :row) out))
    (let [[old new] (swap-vals! wishes wish-after gave-rows?)]
      (when (and (:throw old) (nil? (:throw new)))
        (swap! throws conj {:t (s/ms) :thread (.getName (Thread/currentThread))
                            :rows-of-this-call (count out) :attempt (count @attempts)})
        (throw (ex-info "check probe: forced failure of this attempt" {}))))
    out))

;; ---- the hand ---------------------------------------------------------------

(def system-own "The system's permission in the base." [:system :base :base])

(defn- thing
  "An act as a door sends it, into the base, by `who` under `permission`."
  [who permission facts]
  (let [act-id [:base :microbatch (UUID/randomUUID)]]
    {:act-id act-id :depot :microbatch :version 1 :who who :layer :base
     :permission permission :session :probe-session :facts (vec facts)}))

(defn- open-hand
  "Takes a cluster. Gives the handles that the hand works with."
  [ipc]
  (let [m (rama/get-module-name module/Store)]
    {:ipc ipc :m m
     :depot (rama/foreign-depot ipc m "*microbatch-depot")
     :answers (rama/foreign-query ipc m "answer-by-act-id")}))

(defn- answer-of [hand x]
  (let [act-id (:act-id x)]
    (rama/foreign-invoke-query (:answers hand) (record/routing-id act-id) act-id)))

(defn- word-of [hand x]
  (let [a (answer-of hand x)]
    (cond (nil? a) :nothing-yet
          (= :accepted (:verdict a)) :accepted
          :else (:reason a))))

(defn- answered? [hand xs limit]
  (boolean (s/wait-until #(every? (fn [x] (some? (answer-of hand x))) xs) limit)))

(defn- make-base!
  "Sends the making act of the base and waits for its answer. Gives its word."
  [hand]
  (let [making (thing :system system-own
                      [{:e :base :k :softland.store/depot :v :microbatch}
                       {:e :system :k :softland.store/permission :v system-own}])]
    (rama/foreign-append! (:depot hand) making :append-ack)
    (answered? hand [making] 60000)
    (word-of hand making)))

(defn- things-of-a-batch
  "Gives eight things for one batch: six acts of the system, each on an
  entity of its own; a give to a holder that `n` names, which meets; and an
  act whose permission no act gave, which meets and is refused."
  [n]
  (let [holder (keyword (str "holder-" n))]
    (-> (mapv (fn [_] (thing :system system-own [{:e (UUID/randomUUID) :k :note}])) (range 6))
        (conj (thing :system system-own
                     [{:e holder :k :softland.store/permission :v [holder :base :base system-own]}]))
        (conj (thing :mallory [:mallory :base :base system-own] [{:e (UUID/randomUUID) :k :note}])))))

(defn- send-batch!
  "Sends things so that one batch of the gate takes them all, with the wish
  to throw set to `mode` (nil for none). Gives [whether each was answered,
  the milliseconds from the topology's going on to the last answer]."
  [hand xs mode]
  (rtest/pause-microbatch-topology! (:ipc hand) (:m hand) "microbatch-gate")
  (doseq [x xs] (rama/foreign-append! (:depot hand) x :append-ack))
  (swap! wishes assoc :throw mode)
  (rtest/resume-microbatch-topology! (:ipc hand) (:m hand) "microbatch-gate")
  (let [t0 (s/ms)
        ok (answered? hand xs 120000)]
    (swap! wishes assoc :throw nil)
    [ok (- (s/ms) t0)]))

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

(defn- with-cluster
  "Creates a cluster, launches the store's module in it as the store's
  proofs do, calls `f` with a hand, and closes the cluster. Says how long
  the launch and the whole took."
  [label f]
  (s/say "RUN     ---- cluster" label "----")
  (let [t0 (s/ms)]
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc module/Store {:tasks 4 :threads 2 :workers 2})
      (s/say "OUTSIDE cluster" label ": created and launched in" (format "%.0f" (- (s/ms) t0)) "ms")
      (f (open-hand ipc)))
    (s/say "OUTSIDE cluster" label ": closed," (format "%.0f" (- (s/ms) t0)) "ms after its creation began")))

(defn- report-twice!
  "Says what the notes hold of one batch that was sent with a wish to throw."
  [label notes-before throws-before ok ms]
  (let [mine (subvec @attempts notes-before)
        thrown (subvec @throws throws-before)
        [a1 a2] mine
        r1 (by-key (:rows a1))
        r2 (by-key (:rows a2))
        only-2 (remove (set (keys r1)) (keys r2))
        only-1 (remove (set (keys r2)) (keys r1))]
    (s/say "RESULT " label ": answered" ok "in" (format "%.0f" ms) "ms · attempts noted" (count mine)
           "· the last stamps that `base` was given" (pr-str (mapv :last-stamp mine))
           "· the same last stamp:" (apply = (map :last-stamp mine))
           "· the clocks differ by" (when (and a1 a2) (- (:clock a2) (:clock a1))) "ms")
    (s/say "RESULT " label ": keys noted in attempt 1:" (count r1) "· in attempt 2:" (count r2)
           "· the same keys:" (= (set (keys r1)) (set (keys r2)))
           "· in attempt 2 only, by kind:" (pr-str (frequencies (map first only-2)))
           "· in attempt 1 only, by kind:" (pr-str (frequencies (map first only-1))))
    (doseq [t thrown]
      (s/say "INSIDE " label ": the throw came" (format "at %.1f" (:t t)) "· in the call that gave" (:rows-of-this-call t)
             "rows · on the thread" (pr-str (:thread t))))))

(defn -main [& _]
  (s/open-log! "src/proposal/backend-on-rama-2026-10-02/reads/plan-check-probes-base-for-a-reader/logs/two-clusters.log")
  (try
    (s/say "RUN     Rama 1.6.0, in-process, the store's module `Store`, each cluster 4 tasks, 2 threads, 2 workers")

    ;; T1: the first cluster, the functions as the store defines them
    (with-cluster "1 (the functions as the store defines them)"
      (fn [hand]
        (s/say "OUTSIDE the making act of the base:" (make-base! hand))
        (let [xs (things-of-a-batch 1)
              [ok ms] (send-batch! hand xs nil)]
          (s/say "OUTSIDE a batch of" (count xs) "things: answered" ok "in" (format "%.0f" ms) "ms ·"
                 (pr-str (frequencies (map #(word-of hand %) xs)))))
        (s/say "OUTSIDE update-module! with the unchanged module gave"
               (pr-str (s/within 120000 #(do (rtest/update-module! (:ipc hand) module/Store) :updated))))
        (let [xs (things-of-a-batch 2)
              [ok ms] (send-batch! hand xs nil)]
          (s/say "OUTSIDE a batch after the update: answered" ok "in" (format "%.0f" ms) "ms"))
        (s/say "RESULT  T1: attempts noted while cluster 1 ran (nothing is replaced yet):" (count @attempts))))

    ;; T2: replaced after the module object was defined and launched once
    (alter-var-root #'batch/base (constantly noting-base))
    (alter-var-root #'batch/decide-or-send (constantly noting-decide-or-send))
    (alter-var-root #'batch/rows-to-send (constantly noting-rows-to-send))
    (s/say "OUTSIDE the three functions are replaced in their vars, after cluster 1 was closed")
    (with-cluster "2 (three functions replaced before its launch)"
      (fn [hand]
        (s/say "OUTSIDE the making act of the base:" (make-base! hand))
        (s/say "RESULT  T2: attempts noted after the making act (the replacements run in the second launch):"
               (count @attempts))
        (let [before (count @attempts)
              xs (things-of-a-batch 10)
              [ok ms] (send-batch! hand xs nil)]
          (s/say "OUTSIDE a batch with no throw: answered" ok "in" (format "%.0f" ms) "ms · attempts noted"
                 (- (count @attempts) before) "· rows noted" (count (:rows (peek @attempts)))))
        ;; T4, T5
        (doseq [[mode n] (concat (map vector (repeat :first-call) (range 20 26))
                                 (map vector (repeat :first-call-with-rows) (range 30 36)))]
          (let [notes-before (count @attempts)
                throws-before (count @throws)
                [ok ms] (send-batch! hand (things-of-a-batch n) mode)]
            (Thread/sleep 300)
            (report-twice! (str "T4 " (name mode) " " n) notes-before throws-before ok ms)))))

    ;; T3: put back, then a third launch
    (alter-var-root #'batch/base (constantly first-base))
    (alter-var-root #'batch/decide-or-send (constantly first-decide-or-send))
    (alter-var-root #'batch/rows-to-send (constantly first-rows-to-send))
    (s/say "OUTSIDE the three functions are put back, after cluster 2 was closed")
    (let [notes-before (count @attempts)]
      (with-cluster "3 (the three functions put back before its launch)"
        (fn [hand]
          (s/say "OUTSIDE the making act of the base:" (make-base! hand))
          (let [xs (things-of-a-batch 40)
                [ok ms] (send-batch! hand xs nil)]
            (s/say "OUTSIDE a batch of" (count xs) "things: answered" ok "in" (format "%.0f" ms) "ms"))
          (s/say "RESULT  T3: attempts noted while cluster 3 ran (the first functions are back):"
                 (- (count @attempts) notes-before)))))
    (s/say "RUN     END")
    (catch Throwable t
      (s/say "RUN     THREW" (s/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
