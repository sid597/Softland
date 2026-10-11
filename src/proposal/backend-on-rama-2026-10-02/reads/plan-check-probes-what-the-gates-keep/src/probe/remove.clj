(ns probe.remove
  "The run for one fact about Rama that the plan of `what-the-gates-keep`
  rests on: a row at a top-level key of a PState is taken out with `NONE>`,
  in a stream topology and in a microbatch topology.

  A probe, not a design. It requires nothing of the store's tree and nothing
  of the rig. No table, name or form here is a proposal for the store. Its
  one table has the shape that the plan gives the one more order: a
  top-level map from a String to a row of fixed keys.

  The module. Two depots of things, each partitioned by the thing's `:route`.
  A thing is {:id text :route text :writes [...]}.

    stream      one event for one thing. A write is [key row]. The event makes
                the writes in their order, with no partitioner: a row that is
                nil is taken out with `NONE>`, any other row is put with
                `termval`. Around each write the event reads the key, so the
                log says what the topology itself saw before and after.
    microbatch  two batch blocks. In the first a write is [home key row]: it
                goes to the task of `home` by `|hash`, which is not the task
                where the thing arrived, and is made there in the same way.
                The second block goes to every task after the first is done on
                all of them, and throws on one task when the run asked for a
                failure. So the rows of the first attempt, a removal among
                them, were written before the throw.

  What the JVM keeps, beside Rama: the run's wish to fail, and the batch ids
  that made a write. Memory of this process; it is how the run counts
  attempts.

  Run: see deps.edn. The one argument is the path of the log; with none it
  is logs/remove.log."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [com.rpl.rama.ops :as ops]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.util.concurrent.locks LockSupport]))

;; ---- one log, one clock ---------------------------------------------------

(defonce ^{:doc "The nanosecond at which this JVM loaded the probe."}
  start
  (System/nanoTime))

(defonce ^{:doc "The open writer of the run's log, or nil."}
  log-writer
  (atom nil))

(defn ms
  "Gives the milliseconds since `start`."
  []
  (/ (double (- (System/nanoTime) start)) 1e6))

(defn say
  "Takes parts of a line. Writes them as one line, with the time since
  `start` in front, to the log and to stdout behind the word PROBE. May be
  called from any thread."
  [& parts]
  (let [line (str (format "%9.1f  " (ms)) (str/join " " parts))]
    (locking log-writer
      (println (str "PROBE " line))
      (when-let [^java.io.Writer w @log-writer]
        (.write w line)
        (.write w "\n")
        (.flush w)))))

(defn root-message
  "Takes a throwable. Gives the class and the first line of the message of
  its innermost cause, cut to 200 characters."
  [^Throwable t]
  (let [^Throwable r (last (take-while some? (iterate #(.getCause ^Throwable %) t)))
        m (-> (or (.getMessage r) "") (str/split #"\n") first)]
    (str (.getSimpleName (class r)) ": " (subs m 0 (min 200 (count m))))))

(defn wait-until
  "Takes a function of no arguments and a limit in milliseconds. Calls it
  every 2 ms until it gives a true value or the limit has passed. Gives that
  value, or nil."
  [f limit-ms]
  (let [end (+ (System/nanoTime) (* (long limit-ms) 1000000))]
    (loop []
      (or (try (f) (catch Throwable _ nil))
          (when (< (System/nanoTime) end)
            (LockSupport/parkNanos 2000000)
            (recur))))))

;; ---- what the JVM keeps ---------------------------------------------------

(defonce ^{:doc "The run's wish: how many attempts must still fail."}
  wish
  (atom {:fail-times 0}))

(defonce ^{:doc "Batch id → how many writes its attempts made so far."}
  writes-of-batch
  (atom {}))

;; ---- the functions that the topologies call ------------------------------

(defn saw!
  "A topology calls it after each write. Takes a label, the batch id or nil,
  the thing's id, the task, the key, the row that was to be written (nil for
  a removal), and what the topology read at the key before and after the
  write. Says it."
  [label batch id task k row before after]
  (when batch (swap! writes-of-batch update batch (fnil inc 0)))
  (say "INSIDE " label (if batch (str "batch " batch) "") "task" task "thing" (pr-str id)
       (if (nil? row) "TAKES OUT" "PUTS") (pr-str k)
       "· read before:" (pr-str before) "· read after:" (pr-str after))
  nil)

(defn as-map
  "Takes the rows of a range, as the range navigator gives them. Gives a plain
  map. A named function, since a client's path cannot hold a lambda."
  [rows]
  (into {} rows))

(defn late-point!
  "The second batch block calls it on every task, after the first block is
  done on all of them. Takes the batch id and the task. On task 0, when the
  run asked for a failure and the batch made a write, it throws."
  [batch task]
  (when (and (zero? task) (pos? (get @writes-of-batch batch 0)))
    (let [[old _] (swap-vals! wish update :fail-times #(max 0 (dec %)))]
      (when (pos? (:fail-times old))
        (say "INSIDE  microbatch batch" batch "task" task
             ": the last step THROWS on purpose, after the writes of this attempt")
        (throw (ex-info "probe: forced failure of this attempt" {:batch batch})))))
  nil)

;; ---- the module -------------------------------------------------------------

(deframafn write-row
  "Makes one write on this task at a top-level key: a row that is nil is
  taken out with `NONE>`, any other row is put whole with `termval`. No
  read."
  [$$rows *key *row]
  (<<if (nil? *row)
    (local-transform> [(keypath *key) NONE>] $$rows)
   (else>)
    (local-transform> [(keypath *key) (termval *row)] $$rows))
  (:> nil))

(defmodule RemoveProbe [setup topologies]
  (declare-depot setup *s-things (hash-by :route))
  (declare-depot setup *b-things (hash-by :route))
  (let [s (stream-topology topologies "s")]
    (declare-pstate s $$s-rows {String (fixed-keys-schema {:stamp Long :who String})})
    (<<sources s
      (source> *s-things :> {:keys [*id *writes]})
      (ops/current-task-id :> *task)
      (ops/explode *writes :> [*key *row])
      (local-select> (keypath *key) $$s-rows :> *before)
      (write-row $$s-rows *key *row :> *done)
      (local-select> (keypath *key) $$s-rows :> *after)
      (saw! "stream" nil *id *task *key *row *before *after)))
  (let [mb (microbatch-topology topologies "mb")]
    (declare-pstate mb $$b-rows {String (fixed-keys-schema {:stamp Long :who String})})
    (<<sources mb
      (source> *b-things :> %things)

      ;; 1. each write goes to the task of its home, and is made there
      (<<batch
        (%things :> {:keys [*id *writes]})
        (ops/explode *writes :> [*home *key *row])
        (|hash *home)
        (ops/current-task-id :> *task)
        (ops/current-microbatch-id :> *batch)
        (local-select> (keypath *key) $$b-rows :> *before)
        (write-row $$b-rows *key *row :> *done)
        (local-select> (keypath *key) $$b-rows :> *after)
        (saw! "microbatch" *batch *id *task *key *row *before *after))

      ;; 2. on every task, after 1 is done everywhere: the forced failure
      (<<batch
        (|all)
        (ops/current-microbatch-id :> *batch-2)
        (ops/current-task-id :> *task-2)
        (late-point! *batch-2 *task-2)))))

;; ---- the run ---------------------------------------------------------------

(defn- run-text
  "Takes a value's stand-in text. Gives the first part of each key of that
  value's run, as the plan's key has it: layer, key, since, the value's
  text, and a space."
  [d]
  (str "L :on 100 " d " "))

(defn- k
  "Takes a value's stand-in text and an entity's text. Gives the key."
  [d e]
  (str (run-text d) e))

(defn- run-ends
  "Takes a value's stand-in text. Gives the two ends of its run: from the
  run's text, to the same text with ! in place of its last space."
  [d]
  (let [t (run-text d)]
    [t (str (subs t 0 (dec (count t))) "!")]))

(defn- cases
  "Runs the same cases against one topology. Takes a label, a function that
  appends a thing's writes and waits until the topology has processed them
  (it takes an id and a vector of [value-text entity row-or-nil]), the
  PState's client, the partitioning key of the run of value c1, and a
  function of no arguments that gives every row of the task as a map."
  [label write! rows pkey all-rows]
  (let [[from to] (run-ends "c1")
        run       #(into (sorted-map) (foreign-select-one [(sorted-map-range from to) (view as-map)]
                                                         rows {:pkey pkey}))
        one       #(foreign-select-one (keypath %) rows {:pkey pkey})
        row       (fn [stamp] {:stamp stamp :who ":alice"})
        pushed    (atom [])
        show      (fn [what]
                    (say "OUTSIDE" label what)
                    (say "OUTSIDE" label "  the run of c1 now:" (pr-str (run)))
                    (say "OUTSIDE" label "  every key of the task now:" (pr-str (vec (keys (all-rows)))))
                    (say "OUTSIDE" label "  pushes since the last line:" (pr-str @pushed))
                    (reset! pushed []))]
    (say "RUN    " label "---- the run of c1 is from" (pr-str from) "to" (pr-str to) ", the end not in it ----")
    (write! "1 put" [["c1" ":b1" (row 110)] ["c1" ":b2" (row 111)] ["c2" ":b1" (row 112)]])
    (say "OUTSIDE" label "1 three rows put: two of value c1, one of value c2")
    (say "OUTSIDE" label "  one key read:" (pr-str (one (k "c1" ":b1"))))
    (say "OUTSIDE" label "  the run of c1 now:" (pr-str (run)))
    (let [proxy (foreign-proxy [(sorted-map-range from to)] rows
                               {:pkey pkey
                                :callback-fn (fn [new-value diff old-value]
                                               (swap! pushed conj (str diff)))})]
      (Thread/sleep 300)
      (reset! pushed [])
      (write! "2 out" [["c1" ":b1" nil]])
      (Thread/sleep 300)
      (show "2 the row of c1 :b1 was taken out")
      (say "OUTSIDE" label "  that key read alone:" (pr-str (one (k "c1" ":b1"))))

      (write! "3 out of nothing" [["c1" ":never" nil]])
      (Thread/sleep 300)
      (show "3 a key that never held a row was taken out")

      (write! "4 out and in, one thing" [["c2" ":b1" nil] ["c1" ":b1" (row 140)] ["c3" ":b9" (row 141)]])
      (Thread/sleep 300)
      (show "4 one thing took c2 :b1 out, and put c1 :b1 and c3 :b9")

      (write! "5 out, then put back later" [["c1" ":b2" nil]])
      (write! "5b" [["c1" ":b2" (row 150)]])
      (Thread/sleep 300)
      (show "5 c1 :b2 was taken out by one thing and put again by the next")
      (try (.close ^java.io.Closeable proxy) (catch Throwable _ nil)))))

(defn -main [& args]
  (let [log (or (first args) "logs/remove.log")]
    (io/make-parents log)
    (reset! log-writer (io/writer log)))
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc RemoveProbe {:tasks 4 :threads 4})
      (let [m        (get-module-name RemoveProbe)
            s-depot  (foreign-depot ipc m "*s-things")
            b-depot  (foreign-depot ipc m "*b-things")
            s-rows   (foreign-pstate ipc m "$$s-rows")
            b-rows   (foreign-pstate ipc m "$$b-rows")
            homes    (vec (rtest/gen-hashing-index-keys "home-" 4))
            all-of   (fn [rows pkey]
                       (into (sorted-map)
                             (foreign-select-one [(sorted-map-range "" "~") (view as-map)]
                                                 rows {:pkey pkey})))
            processed (atom 0)
            ;; stream: the thing arrives on task 0 and is written there
            s-write! (fn [id writes]
                       (foreign-append! s-depot {:id id :route (homes 0)
                                                 :writes (mapv (fn [[d e r]] [(k d e) r]) writes)}
                                        :ack))
            ;; microbatch: the thing arrives on task 1; each write goes to task 2
            b-write! (fn [id writes]
                       (foreign-append! b-depot {:id id :route (homes 1)
                                                 :writes (mapv (fn [[d e r]] [(homes 2) (k d e) r]) writes)}
                                        :append-ack)
                       (rtest/wait-for-microbatch-processed-count ipc m "mb" (swap! processed inc)))]
        (say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads. One key for each task:" (pr-str homes))

        (cases "STREAM    " s-write! s-rows (homes 0) #(all-of s-rows (homes 0)))

        ;; stream only: two writes at ONE key in one event, in both orders
        (s-write! "6 out then in, one key, one event" [["c1" ":b7" nil] ["c1" ":b7" {:stamp 160 :who ":alice"}]])
        (say "OUTSIDE STREAM     6 one event took c1 :b7 out and then put it:"
             (pr-str (foreign-select-one (keypath (k "c1" ":b7")) s-rows {:pkey (homes 0)})))
        (s-write! "6b in then out, one key, one event" [["c1" ":b8" {:stamp 161 :who ":alice"}] ["c1" ":b8" nil]])
        (say "OUTSIDE STREAM     6b one event put c1 :b8 and then took it out:"
             (pr-str (foreign-select-one (keypath (k "c1" ":b8")) s-rows {:pkey (homes 0)})))

        (cases "MICROBATCH" b-write! b-rows (homes 2) #(all-of b-rows (homes 2)))

        ;; microbatch only: a removal in an attempt that fails
        (say "RUN     ---- microbatch: the first attempt of a batch that takes a row out is made to fail ----")
        (let [before (all-of b-rows (homes 2))
              seen   (atom [])
              stop   (atom false)
              watch  (doto (Thread.
                            (fn []
                              (loop [last-v ::none]
                                (when-not @stop
                                  (let [v (try (vec (keys (all-of b-rows (homes 2))))
                                               (catch Throwable t [:read-threw (root-message t)]))]
                                    (when (not= v last-v)
                                      (swap! seen conj v)
                                      (say "OUTSIDE MICROBATCH   a select of the task's keys now gives" (pr-str v)))
                                    (LockSupport/parkNanos 300000)
                                    (recur v))))))
                       (.setDaemon true)
                       (.start))]
          (say "OUTSIDE MICROBATCH 7 before the batch, the task holds:" (pr-str (vec (keys before))))
          (reset! wish {:fail-times 1})
          (foreign-append! b-depot {:id "7 out c1 :b1, in c4 :b1" :route (homes 1)
                                    :writes [[(homes 2) (k "c1" ":b1") nil]
                                             [(homes 2) (k "c4" ":b1") {:stamp 170 :who ":alice"}]]}
                           :append-ack)
          (say "OUTSIDE MICROBATCH 7 the task came to hold c4 :b1 and not c1 :b1:"
               (boolean (wait-until #(let [ks (set (keys (all-of b-rows (homes 2))))]
                                       (and (contains? ks (k "c4" ":b1")) (not (contains? ks (k "c1" ":b1")))))
                                    60000)))
          (Thread/sleep 800)
          (reset! stop true)
          (.join watch 2000)
          (say "OUTSIDE MICROBATCH 7 what a select of the task's keys was given, in order:" (pr-str @seen))
          (say "OUTSIDE MICROBATCH 7 writes that the attempts of each batch made, by batch id:"
               (pr-str (into (sorted-map) @writes-of-batch))))
        (say "RUN     END")))
    (catch Throwable t
      (say "RUN     THREW" (root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
