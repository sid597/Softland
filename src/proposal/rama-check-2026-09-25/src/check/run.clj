(ns check.run
  "Runs the checks against an in-process cluster and prints, for each claim,
   what was run and what was seen. Throwaway.

   Second version: every read retries through a lost worker connection and
   counts the loss, each experiment is fenced so one crash does not end the
   run, and the failure experiments run on the retrying source first."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [check.module :as m]
            [check.faults :as faults]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

;; ---------------------------------------------------------------- helpers

(def results (atom []))
(def conn-losses (atom 0))

(defn record! [claim status detail]
  (swap! results conj {:claim claim :status status :detail detail})
  (println)
  (println (str "  [" (name status) "] " claim))
  (doseq [line (str/split-lines (with-out-str (pp/pprint detail)))]
    (println (str "      " line))))

(defn section [title]
  (println)
  (println (str "==== " title " ====")))

(defmacro experiment
  "Fence: an exception inside records :error instead of ending the run."
  [claim & body]
  `(try ~@body
        (catch Throwable e#
          (record! ~claim :error {:exception (str (.getName (class e#)) ": " (.getMessage e#))}))))

(defn read!
  "A foreign read that retries through a lost worker connection for up to 30 s,
   counting each loss."
  [f]
  (let [deadline (+ (System/currentTimeMillis) 30000)]
    (loop []
      (let [r (try {:ok (f)}
                   (catch Throwable e
                     (if (re-find #"CallbackException|connection|closed" (str e))
                       (do (swap! conn-losses inc) {:retry (str e)})
                       (throw e))))]
        (cond (contains? r :ok) (:ok r)
              (> (System/currentTimeMillis) deadline) (throw (ex-info "read never recovered" {:last (:retry r)}))
              :else (do (Thread/sleep 100) (recur)))))))

(defn try-append
  "foreign-append! in a future, so a hanging ack cannot stall the run."
  [depot rec ack timeout-ms]
  (let [f (future (try {:ok (foreign-append! depot rec ack)}
                       (catch Throwable e
                         {:error (str (.getName (class e)) ": " (.getMessage e))})))
        r (deref f timeout-ms ::timeout)]
    (if (= r ::timeout) {:timeout timeout-ms} r)))

(defn wait-until [pred ms desc]
  (let [deadline (+ (System/currentTimeMillis) ms)]
    (loop []
      (cond (pred) true
            (> (System/currentTimeMillis) deadline)
            (do (println "  !! timed out waiting for" desc) false)
            :else (do (Thread/sleep 20) (recur))))))

(defn settle!
  "Wait until the fault state has not changed for quiet-ms (replays finished)
   and reads work again."
  [quiet-ms]
  (loop [last @faults/state t (System/currentTimeMillis) n 0]
    (Thread/sleep 100)
    (let [now @faults/state]
      (cond (not= now last) (recur now (System/currentTimeMillis) (inc n))
            (> (- (System/currentTimeMillis) t) quiet-ms) n
            :else (recur now t n)))))

(defn ids-in [entries] (mapv :id entries))
(defn entries-for [entries id] (filterv #(= id (:id %)) entries))
(defn dup-ids [entries] (->> (frequencies (ids-in entries)) (filter #(> (val %) 1)) (into {})))

;; ---------------------------------------------------------------- main

(defn run [ipc]
  (rtest/launch-module! ipc m/StampModule {:tasks 4 :threads 4 :workers 1})
  (rtest/launch-module! ipc m/CheckModule {:tasks 4 :threads 4 :workers 1})
  (let [mn (get-module-name m/CheckModule)
        sn m/stamp-module-name
        [_k0 kA kB _k3] (rtest/gen-hashing-index-keys 4)
        s-retry (foreign-depot ipc mn "*s-retry")
        s-none (foreign-depot ipc mn "*s-none")
        mb-depot (foreign-depot ipc mn "*mb-depot")
        vis-depot (foreign-depot ipc mn "*vis-depot")
        stamp-depot (foreign-depot ipc sn "*stamp-depot")
        $s-a (foreign-pstate ipc mn "$$s-a")
        $s-a-count (foreign-pstate ipc mn "$$s-a-count")
        $s-clock (foreign-pstate ipc mn "$$s-clock")
        $s-b (foreign-pstate ipc mn "$$s-b")
        $mb-a (foreign-pstate ipc mn "$$mb-a")
        $mb-clock (foreign-pstate ipc mn "$$mb-clock")
        $mb-b (foreign-pstate ipc mn "$$mb-b")
        $mb-vis (foreign-pstate ipc mn "$$mb-vis")
        $stamps (foreign-pstate ipc sn "$$stamps")
        mb-count (atom 0)
        mb-append! (fn [rec] (swap! mb-count inc) (try-append mb-depot rec :append-ack 30000))
        mb-wait! (fn [] (read! #(rtest/wait-for-microbatch-processed-count ipc mn "mb" @mb-count 60000)))
        s-a (fn [] (read! #(foreign-select-one (keypath kA) $s-a)))
        s-a-count (fn [] (read! #(foreign-select-one (keypath kA) $s-a-count)))
        s-clock-a (fn [] (read! #(foreign-select-one (keypath 1) $s-clock {:pkey kA})))
        s-b (fn [] (read! #(foreign-select-one (keypath kB) $s-b)))
        mb-a (fn [] (read! #(foreign-select-one (keypath kA) $mb-a)))
        mb-clock-a (fn [] (read! #(foreign-select-one (keypath 1) $mb-clock {:pkey kA})))
        mb-b (fn [] (read! #(foreign-select-one (keypath kB) $mb-b)))
        stream-state (fn [] {:a-ids (ids-in (s-a)) :a-dups (dup-ids (s-a)) :a-count (s-a-count) :clock-a (s-clock-a)
                             :b-ids (ids-in (s-b)) :b-dups (dup-ids (s-b))})
        rec (fn [id] {:id id :ka kA :kb kB})
        losses-since (fn [n0] (- @conn-losses n0))]
    (println "kA =" kA "(hashes to task 1), kB =" kB "(hashes to task 2)")

    ;; ============================================================ 1. STREAM
    (section "1. Stream: one event, two partitions")

    (experiment "stream baseline"
      (let [r (try-append s-retry (rec "s0") :ack 30000)]
        (record! "stream baseline: two-hop event lands on A then B, :ack returns"
                 (if (and (:ok r) (= ["s0"] (ids-in (s-a))) (= ["s0"] (ids-in (s-b)))) :held :failed)
                 {:append r :a (s-a) :a-count (s-a-count) :clock-a (s-clock-a) :b (s-b)})))

    ;; 1a. retry :individual, throw between the two writes on A, :append-ack
    (experiment "s3"
      (let [n0 @conn-losses]
        (faults/arm-fail! "s3" :a-mid 1)
        (let [r (try-append s-retry (rec "s3") :append-ack 30000)
              done (wait-until #(>= (faults/attempts "s3" :b) 1) 30000 "s3 to reach B")
              _ (settle! 1500)
              st (stream-state)]
          (record! "stream, retry :individual, throw between two writes on A: A's writes discarded together, event retried, A gets exactly one entry"
                   (if (and done (= 1 (count (entries-for (s-a) "s3"))) (= 1 (count (entries-for (s-b) "s3")))
                            (= 2 (faults/attempts "s3" :a)))
                     :held :failed)
                   {:append r :attempts (faults/snapshot "s3") :worker-connection-losses (losses-since n0)
                    :a-entries-for-s3 (entries-for (s-a) "s3") :b-entries-for-s3 (entries-for (s-b) "s3")
                    :whole-state st}))))

    ;; 1b. retry :individual, throw on B after A committed, :append-ack
    (experiment "s4"
      (let [n0 @conn-losses]
        (faults/arm-fail! "s4" :b 1)
        (let [r (try-append s-retry (rec "s4") :append-ack 30000)
              done (wait-until #(>= (faults/attempts "s4" :b) 2) 30000 "s4 to reach B twice")
              _ (settle! 1500)
              st (stream-state)]
          (record! "stream, retry :individual, throw on B after A committed: event retried whole from the depot, A gets the write TWICE with two stamps"
                   (if (and done (= 2 (count (entries-for (s-a) "s4"))) (= 1 (count (entries-for (s-b) "s4")))) :held :failed)
                   {:append r :attempts (faults/snapshot "s4") :worker-connection-losses (losses-since n0)
                    :a-entries-for-s4 (entries-for (s-a) "s4") :b-entries-for-s4 (entries-for (s-b) "s4")
                    :whole-state st}))))

    ;; 1c. same as 1b but the client waits with :ack — what does the client see?
    (experiment "s4b"
      (let [n0 @conn-losses]
        (faults/arm-fail! "s4b" :b 1)
        (let [r (try-append s-retry (rec "s4b") :ack 30000)
              done (wait-until #(>= (faults/attempts "s4b" :b) 2) 30000 "s4b to reach B twice")
              _ (settle! 1500)
              st (stream-state)]
          (record! "stream, retry :individual, throw on B, client appended with :ack: what the ack returns after the retry"
                   (if (and done (= 1 (count (entries-for (s-b) "s4b")))) :held :failed)
                   {:append r :attempts (faults/snapshot "s4b") :worker-connection-losses (losses-since n0)
                    :a-entries (entries-for (s-a) "s4b") :b-entries (entries-for (s-b) "s4b")
                    :whole-state st}))))

    ;; 1d. retry :none, throw between two writes on A, :append-ack
    (experiment "s1"
      (let [n0 @conn-losses]
        (faults/arm-fail! "s1" :a-mid 1)
        (let [r (try-append s-none (rec "s1") :append-ack 30000)
              _ (wait-until #(>= (faults/attempts "s1" :a) 1) 30000 "s1 to reach A")
              _ (settle! 2500)
              st (stream-state)]
          (record! "stream, retry :none, throw between two writes on A: nothing of the event is visible on A, and it is not retried"
                   (if (and (empty? (entries-for (s-a) "s1")) (empty? (entries-for (s-b) "s1"))) :held :failed)
                   {:append r :attempts (faults/snapshot "s1") :worker-connection-losses (losses-since n0)
                    :whole-state st}))))

    ;; 1e. retry :none, throw on B, :ack
    (experiment "s2"
      (let [n0 @conn-losses]
        (faults/arm-fail! "s2" :b 1)
        (let [r (try-append s-none (rec "s2") :ack 30000)
              _ (wait-until #(>= (faults/attempts "s2" :b) 1) 30000 "s2 to reach B")
              _ (settle! 2500)
              st (stream-state)]
          (record! "stream, retry :none, throw on B after A committed: A shows the write, B never gets it"
                   (if (and (= 1 (count (entries-for (s-a) "s2"))) (empty? (entries-for (s-b) "s2"))) :held :failed)
                   {:append r :attempts (faults/snapshot "s2") :worker-connection-losses (losses-since n0)
                    :a-entries (entries-for (s-a) "s2") :whole-state st}))))

    ;; 1f. event held on B: is A's write already visible while the event is in flight?
    (experiment "s5"
      (faults/arm-block! "s5" :b)
      (let [r (try-append s-retry (rec "s5") :append-ack 30000)
            reached (wait-until #(= 1 (faults/attempts "s5" :b)) 10000 "s5 to reach B")
            a-while (s-a) c-while (s-a-count) b-while (s-b)]
        (faults/release! "s5" :b)
        (let [landed (wait-until #(seq (entries-for (s-b) "s5")) 10000 "s5 to land on B")]
          (record! "stream, event held on B: A's write is visible to a client before B's write exists"
                   (if (and reached (= 1 (count (entries-for a-while "s5"))) (empty? (entries-for b-while "s5")) landed)
                     :held :failed)
                   {:append r :a-while-held (entries-for a-while "s5") :a-count-while-held c-while
                    :b-while-held (entries-for b-while "s5") :b-after (entries-for (s-b) "s5")}))))

    ;; ======================================================== 2. MICROBATCH
    (section "2. Microbatch: one batch, two partitions")

    (experiment "microbatch baseline and stamps"
      (try-append stamp-depot {:k kA :stamp 100} :ack 30000)
      (mb-append! (rec "m0"))
      (mb-wait!)
      (let [a (mb-a) b (mb-b)]
        (record! "microbatch baseline: two-hop event lands on A then B"
                 (if (and (= ["m0"] (ids-in a)) (= ["m0"] (ids-in b))) :held :failed)
                 {:a a :clock-a (mb-clock-a) :b b}))
      (record! "stamps: a microbatch event reads a PState owned by the stream topology in the SAME module (committed view)"
               (if (= (s-clock-a) (:s-clock (first (mb-b)))) :held :failed)
               {:stream-clock-a-now (s-clock-a) :seen-by-micro-event (:s-clock (first (mb-b)))})
      (record! "stamps: a microbatch event reads a stamp written by a stream topology in ANOTHER module (mirror PState)"
               (if (= 100 (:stood-on (first (mb-b)))) :held :failed)
               {:stamp-in-other-module (read! #(foreign-select-one (keypath kA) $stamps))
                :seen-by-micro-event (:stood-on (first (mb-b)))}))

    ;; 2a. throw on B: whole batch retried, A's non-idempotent writes appear once.
    (experiment "m1"
      (let [n0 @conn-losses]
        (faults/arm-fail! "m1" :mb-b 1)
        (mb-append! (rec "m1"))
        (mb-wait!)
        (settle! 1500)
        (let [a (mb-a) clk (mb-clock-a) b (mb-b)]
          (record! "microbatch, throw on B after A's writes: batch retried whole, A's append and clock tick appear exactly ONCE"
                   (if (and (= 1 (count (entries-for a "m1"))) (= 2 clk) (= 1 (count (entries-for b "m1")))
                            (>= (faults/attempts "m1" :mb-a) 2) (>= (faults/attempts "m1" :mb-b) 2))
                     :held :failed)
                   {:attempts (faults/snapshot "m1") :worker-connection-losses (losses-since n0)
                    :a-entries-for-m1 (entries-for a "m1") :a-dups (dup-ids a) :clock-a clk
                    :b-entries-for-m1 (entries-for b "m1") :b-dups (dup-ids b)}))))

    ;; 2b. batch held on B: is A's write visible before the batch commits?
    (experiment "m2"
      (faults/arm-block! "m2" :mb-b)
      (rtest/pause-microbatch-topology! ipc mn "mb")
      (mb-append! (rec "m2"))
      (rtest/resume-microbatch-topology! ipc mn "mb")
      (let [reached (wait-until #(= 1 (faults/attempts "m2" :mb-b)) 15000 "m2 to reach B")
            a-while (mb-a) clk-while (mb-clock-a)]
        (faults/release! "m2" :mb-b)
        (mb-wait!)
        (record! "microbatch, batch held on B after A's writes: NOTHING of the batch is visible on A until the batch commits"
                 (if (and reached (empty? (entries-for a-while "m2")) (= 2 clk-while)
                          (= 1 (count (entries-for (mb-a) "m2"))) (= 1 (count (entries-for (mb-b) "m2"))))
                   :held :failed)
                 {:a-while-held (ids-in a-while) :clock-a-while-held clk-while
                  :a-after (ids-in (mb-a)) :b-after (ids-in (mb-b))})))

    ;; 2c. what the micro event "stood on" is a read at attempt time; a retry reads again.
    (experiment "m3"
      (faults/arm-block! "m3" :mb-b)
      (faults/arm-fail! "m3" :mb-b 1)
      (rtest/pause-microbatch-topology! ipc mn "mb")
      (mb-append! (rec "m3"))
      (rtest/resume-microbatch-topology! ipc mn "mb")
      (let [reached (wait-until #(= 1 (faults/attempts "m3" :mb-b)) 15000 "m3 to reach B")]
        (try-append stamp-depot {:k kA :stamp 200} :ack 30000)
        (faults/release! "m3" :mb-b)
        (mb-wait!)
        (settle! 1500)
        (let [b (mb-b) e (first (entries-for b "m3"))]
          (record! "stamps across a retry: the mirror read is redone on the retry, so the value stood on can change between attempts"
                   (if (and reached (= 100 (first (faults/notes "m3" :stood-on-read))) (= 200 (:stood-on e))) :held :failed)
                   {:reads-per-attempt (faults/notes "m3" :stood-on-read) :attempts (faults/snapshot "m3")
                    :landed-entry e :a-entries (entries-for (mb-a) "m3") :clock-a (mb-clock-a)}))))

    ;; 2d. mid-commit visibility across two tasks
    (section "2d. Microbatch: can a reader see one partition's commit before the other's?")
    (experiment "vis"
      (let [rounds 400
            filler 500
            producer (future
                       (dotimes [r rounds]
                         (swap! mb-count inc)
                         (foreign-append! vis-depot {:round (inc r) :ka kA :kb kB :filler filler} :append-ack)
                         (Thread/sleep 5))
                       :done)
            obs (atom [])
            poller (future
                     (loop [i 0]
                       (when-not (realized? producer)
                         (let [ab? (even? i)
                               x (foreign-select-one (keypath (if ab? kA kB)) $mb-vis)
                               y (foreign-select-one (keypath (if ab? kB kA)) $mb-vis)]
                           (swap! obs conj [(if ab? :A-then-B :B-then-A) x y])
                           (recur (inc i)))))
                     :done)]
        @producer @poller
        (mb-wait!)
        (let [o @obs
              pairs (filter (fn [[_ x y]] (and x y)) o)
              first-ahead (filter (fn [[_ x y]] (> x y)) pairs)
              second-ahead (filter (fn [[_ x y]] (< x y)) pairs)
              same (filter (fn [[_ x y]] (= x y)) pairs)
              max-gap (reduce max 0 (map (fn [[_ x y]] (Math/abs (- x y))) pairs))]
          (record! "microbatch commit across two tasks: a client can read partition A already on batch N while B still shows N-1 (first read AHEAD of second)"
                   (if (seq first-ahead) :observed :not-observed)
                   {:reads (count pairs) :same (count same)
                    :first-read-ahead (count first-ahead) :second-read-ahead (count second-ahead)
                    :max-round-gap max-gap
                    :sample-first-ahead (take 5 first-ahead)
                    :final-a (foreign-select-one (keypath kA) $mb-vis) :final-b (foreign-select-one (keypath kB) $mb-vis)}))))

    ;; ========================================================== 3. FAILOVER
    (section "3. Failover: what an in-process cluster can do")

    (experiment "sf1"
      (faults/arm-block! "sf1" :b)
      (let [r (try-append s-retry (rec "sf1") :append-ack 30000)
            reached (wait-until #(= 1 (faults/attempts "sf1" :b)) 10000 "sf1 to reach B")
            t0 (System/currentTimeMillis)
            upd (future (try {:ok (rtest/update-module! ipc m/CheckModule)}
                             (catch Throwable e {:error (str (.getName (class e)) ": " (.getMessage e))})))
            during (deref upd 10000 ::still-running)
            t1 (System/currentTimeMillis)]
        (faults/release! "sf1" :b)
        (let [after (deref upd 60000 ::still-running)
              t2 (System/currentTimeMillis)
              landed (wait-until #(seq (entries-for (s-b) "sf1")) 30000 "sf1 to land on B after update")]
          (settle! 1500)
          (record! "failover, stream: update-module! (same module = simulated worker restart) while an event is held mid-flight"
                   :ran
                   {:append r :reached-b reached
                    :update-result-while-held during :ms-waited-while-held (- t1 t0)
                    :update-result-after-release after :ms-until-update-returned (- t2 t0)
                    :attempts (faults/snapshot "sf1")
                    :a-entries (entries-for (s-a) "sf1") :b-entries (entries-for (s-b) "sf1") :landed landed
                    :whole-state (stream-state)}))))

    (experiment "mf1"
      (faults/arm-block! "mf1" :mb-b)
      (mb-append! (rec "mf1"))
      (let [reached (wait-until #(= 1 (faults/attempts "mf1" :mb-b)) 15000 "mf1 to reach B")
            t0 (System/currentTimeMillis)
            upd (future (try {:ok (rtest/update-module! ipc m/CheckModule)}
                             (catch Throwable e {:error (str (.getName (class e)) ": " (.getMessage e))})))
            during (deref upd 10000 ::still-running)
            t1 (System/currentTimeMillis)]
        (faults/release! "mf1" :mb-b)
        (let [after (deref upd 60000 ::still-running)
              t2 (System/currentTimeMillis)
              waited (try (mb-wait!) true (catch Throwable e (str e)))]
          (settle! 1500)
          (record! "failover, microbatch: update-module! while a batch is held mid-flight"
                   :ran
                   {:reached-b reached
                    :update-result-while-held during :ms-waited-while-held (- t1 t0)
                    :update-result-after-release after :ms-until-update-returned (- t2 t0)
                    :attempts (faults/snapshot "mf1") :mb-wait waited
                    :a-entries (entries-for (mb-a) "mf1") :a-dups (dup-ids (mb-a)) :clock-a (mb-clock-a)
                    :b-entries (entries-for (mb-b) "mf1") :b-dups (dup-ids (mb-b))}))))))

(defn -main [& _]
  (println "Rama check, in-process cluster, 4 tasks / 4 threads / 1 worker")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (println "IPC public methods:"
               (sort (distinct (map #(.getName %) (.getMethods (class ipc))))))
      (run ipc))
    (catch Throwable e
      (println "RUN ABORTED:" (str e)))
    (finally
      (section "Summary")
      (doseq [{:keys [claim status]} @results]
        (println (format "  %-13s %s" (name status) claim)))
      (println "  worker connection losses seen by the client:" @conn-losses)
      (spit "results.edn" (with-out-str (pp/pprint {:results @results :conn-losses @conn-losses})))
      (println "wrote results.edn")))
  (shutdown-agents)
  (System/exit 0))
