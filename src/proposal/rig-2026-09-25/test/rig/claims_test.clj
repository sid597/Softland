(ns rig.claims-test
  "Rig phase 0: the two Rama claims the formal model assumes, run on an
  in-process cluster, and the cross-module read question. Every test prints
  what it observed, for RIG.md."
  (:require [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.claims :as c]))

(defn- wait-until
  "Poll f until it is truthy or timeout-ms passes; return its last value."
  ([f] (wait-until f 30000))
  ([f timeout-ms]
   (let [deadline (+ (System/currentTimeMillis) timeout-ms)]
     (loop []
       (let [v (f)]
         (if (or v (> (System/currentTimeMillis) deadline))
           v
           (do (Thread/sleep 20) (recur))))))))

(defn- steps [tag what k]
  (count (filter #(= [what k] %) (get @c/executed tag))))

(defn- say [& xs] (apply println "OBSERVED" xs))

(deftest stream-claims
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc c/StreamClaims {:tasks 4 :threads 4})
    (let [mn (get-module-name c/StreamClaims)
          depot (foreign-depot ipc mn "*s-depot")
          left (foreign-pstate ipc mn "$$left")
          right (foreign-pstate ipc mn "$$right")
          [k0 k1 k2 k3] (rtest/gen-hashing-index-keys 4)
          append! (fn [rec]
                    (try (foreign-append! depot rec :ack) :acked
                         (catch Exception e (str "threw " (.getName (class e)) ": " (.getMessage e)))))
          ;; a worker that died on the injected failure is restarting; a read then fails
          sel (fn [p k] (try (foreign-select-one (keypath k) p) (catch Exception _ :unavailable)))
          t-start (System/currentTimeMillis)]

      (testing "one partition: a failure between two writes in one event discards both; the retry writes each once"
        (swap! c/fail-once conj :one)
        (let [ack (append! {:ka k0 :kb k0 :tag :one :mode :one-task})]
          (wait-until #(= 1 (sel right k0)) 120000)
          (say "stream one-task: append" ack "| recovered after" (- (System/currentTimeMillis) t-start) "ms"
               "| left" (sel left k0) "right" (sel right k0)
               "| first write ran" (steps :one :left k0) "times")
          (is (= 2 (steps :one :left k0)) "the first write ran in both attempts")
          (is (= 1 (sel left k0)) "the failed attempt's write on this task was discarded")
          (is (= 1 (sel right k0)))))

      (testing "two partitions: a failure on the second task replays the record from its start; the first task's commit stays"
        (swap! c/fail-once conj :two)
        (let [ack (append! {:ka k1 :kb k2 :tag :two :mode :two-tasks})]
          (wait-until #(= 1 (sel right k2)) 120000)
          (say "stream two-tasks: append" ack
               "| left" (sel left k1) "right" (sel right k2)
               "| first write ran" (steps :two :left k1) "times")
          (is (<= 2 (steps :two :left k1)) "replayed from the start, at least once")
          (is (= (steps :two :left k1) (sel left k1)) "every replay applied the first task's write again")
          (is (= 1 (sel right k2)))))

      (testing "a worker restart while a record is held on its second task"
        (swap! c/gates assoc :restart (promise))
        (let [appended (future (append! {:ka k3 :kb k0 :tag :restart :mode :two-tasks}))
              held? (wait-until #(some #{[:held nil]} (get @c/executed :restart)) 10000)
              t0 (System/currentTimeMillis)
              updated (future (try (rtest/update-module! ipc c/StreamClaims) :updated
                                   (catch Exception e (str "update threw: " (.getMessage e)))))
              _ (Thread/sleep 3000)
              update-done-while-held (realized? updated)]
          (deliver (get @c/gates :restart) :go)
          (let [u (deref updated 90000 :update-timeout)
                a (deref appended 90000 :append-timeout)
                _ (wait-until #(number? (sel right k0)) 60000)
                _ (Thread/sleep 2000)]
            (say "stream restart: held" (boolean held?)
                 "| update finished while held:" update-done-while-held
                 "| update" u "after" (- (System/currentTimeMillis) t0) "ms"
                 "| append" a
                 "| left" (sel left k3) "right" (sel right k0)
                 "| first write ran" (steps :restart :left k3) "times"
                 "| held" (steps :restart :held nil) "times"
                 "| second write ran" (steps :restart :right k0) "times")
            (is held?)
            (is (<= 1 (sel left k3)))
            (is (number? (sel right k0)) "the record completed"))))

      (testing "after the crashes: were records that had completed replayed too?"
        (say "stream replays of the one-task record, which completed before the later crashes:"
             "first write ran" (steps :one :left k0) "times, second write" (steps :one :right k0)
             "| left" (sel left k0) "| right k0 (one-task 1 + restart record's write)" (sel right k0)
             "| restart record's second write ran" (steps :restart :right k0) "times")
        (is (number? (sel left k0)))))))

(deftest micro-claims
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc c/MicroClaims {:tasks 4 :threads 4})
    (let [mn (get-module-name c/MicroClaims)
          depot (foreign-depot ipc mn "*m-depot")
          mcount (foreign-pstate ipc mn "$$mcount")
          mlast (foreign-pstate ipc mn "$$mlast")
          ks (vec (rtest/gen-hashing-index-keys 4))
          n (atom 0)
          append! (fn [rec] (foreign-append! depot rec) (swap! n inc))
          wait! (fn [] (rtest/wait-for-microbatch-processed-count ipc mn "mb" @n 60000))
          sel (fn [p k] (foreign-select-one (keypath k) p))]

      (testing "decided before visible: a write that ran on task 0 is not visible while the batch is held on task 3"
        (swap! c/gates assoc :hold (promise))
        (append! {:keys ks :tag :hold :hold-key (ks 3) :after-key (ks 0)})
        (let [held? (wait-until #(some #{[:held nil]} (get @c/executed :hold)) 20000)
              ran-on-0 (steps :hold :wrote (ks 0))
              during (mapv #(sel mcount %) (subvec ks 0 3))]
          (deliver (get @c/gates :hold) :go)
          (wait!)
          (let [after (mapv #(sel mcount %) ks)]
            (say "micro hold: held" (boolean held?) "| task-0 write ran" ran-on-0 "times"
                 "| counts during hold" during "| after commit" after)
            (is held?)
            (is (= 1 ran-on-0))
            (is (= [nil nil nil] during) "tasks 0-2, read while task 3 holds the batch")
            (is (= [1 1 1 1] after)))))

      (testing "atomic across partitions: a failure on task 1 after task 0 wrote leaves no partial state; the retry applies once"
        (swap! c/fail-once conj :fail)
        (append! {:keys ks :tag :fail :fail-key (ks 1) :after-key (ks 0)})
        (wait!)
        (let [after (mapv #(sel mcount %) ks)]
          (say "micro fail: task-0 write ran" (steps :fail :wrote (ks 0)) "times | counts" after)
          (is (= 2 (steps :fail :wrote (ks 0))) "the task-0 write ran in both attempts")
          (is (= [2 2 2 2] after) "and was applied once")))

      (testing "visibility during commit: does a later read ever show an older batch than an earlier read did?"
        (let [stop (atom false)
              anomalies (atom [])
              pairs (atom 0)
              seqn (atom 0)
              mseq (foreign-pstate ipc mn "$$mseq")
              appender (future
                         (loop [] (when-not @stop (append! {:keys ks :seq (swap! seqn inc)}) (Thread/sleep 1) (recur))))
              reader (fn []
                       (future
                         (loop []
                           (when-not @stop
                             (let [i (rand-int 4) j (rand-int 4)]
                               (when (not= i j)
                                 (let [a (sel mseq (ks i))
                                       b (sel mseq (ks j))]
                                   (swap! pairs inc)
                                   (when (and a b (< b a))
                                     (swap! anomalies conj [i a j b])))))
                             (recur)))))
              readers (doall (repeatedly 6 reader))
              t0 (System/currentTimeMillis)]
          (Thread/sleep 20000)
          (reset! stop true)
          @appender (run! deref readers)
          (wait!)
          (let [batches (sel mlast (ks 0))
                finals (mapv #(sel mseq %) ks)]
            (say "micro visibility: ran" (- (System/currentTimeMillis) t0) "ms,"
                 "records" @n ", last microbatch id" batches ", final max-seq per task" finals ","
                 "read pairs" @pairs ", pairs where the later read showed an older batch" (count @anomalies)
                 (when (seq @anomalies) (str "e.g. " (vec (take 5 @anomalies)))))
            ;; recorded, not asserted: the docs say a reader may see this
            (is (apply = finals) "once settled, every task holds the same batch")))))))

(deftest cross-module-read
  (with-open [ipc (rtest/create-ipc)]
    (rtest/launch-module! ipc c/StampSource {:tasks 4 :threads 2})
    (rtest/launch-module! ipc c/StampReader {:tasks 4 :threads 2})
    (let [sn (get-module-name c/StampSource)
          rn (get-module-name c/StampReader)
          src (foreign-depot ipc sn "*stamp-depot")
          own (foreign-depot ipc rn "*own-stamp-depot")
          rd (foreign-depot ipc rn "*read-depot")
          seen (foreign-pstate ipc rn "$$seen")]
      (testing "a microbatch topology reads another module's PState through a mirror, and its own module's stream PState"
        (foreign-append! src {:k "x" :stamp 42} :ack)
        (foreign-append! own {:k "x" :stamp 7} :ack)
        (foreign-append! rd {:k "x"})
        (rtest/wait-for-microbatch-processed-count ipc rn "mb" 1 60000)
        (let [v (foreign-select-one (keypath "x") seen)]
          (say "cross-module: source module name" sn "| microbatch read [other-module own-module] =" v)
          (is (= [42 7] v))))
      (testing "the read is a read at a moment: a later stamp in the other module is seen by a later batch"
        (foreign-append! src {:k "x" :stamp 43} :ack)
        (foreign-append! rd {:k "x"})
        (rtest/wait-for-microbatch-processed-count ipc rn "mb" 2 60000)
        (is (= [43 7] (foreign-select-one (keypath "x") seen)))))))
