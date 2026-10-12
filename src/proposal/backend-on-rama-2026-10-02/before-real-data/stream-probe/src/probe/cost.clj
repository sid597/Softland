(ns probe.cost
  "The run for question 4: what the append costs an event.

  Thirteen ways: no append, and the twelve of `probe.gate/variants`. For
  each way, three loads, all on one task, task 1:

    steady    300 things at 100 a second. A thread sends each at its due
              time with an :ack append that does not wait, so the rate does
              not depend on the answers.
    one       500 things from one client, each sent when the one before
              was answered: flat out for one client, one event at a time.
    at once   2000 things sent as fast as the loop goes, none waiting for
              another: flat out for the task.

  Three times are kept:

    event     inside, from the event's first step to its last. It leaves
              out the commit of the rows, which comes after the code.
              With an append that waits, other events run on the thread
              during the wait, so under the load `at once` this is the time
              the event was on its way, not the work it cost.
    append    inside, around the `depot-partition-append!` step alone.
    client    from the client's :ack append call to its return.

  Times of one in-process cluster on one machine: an order of size, no more.

  Run:  clojure -M:cost   (from this folder; the log is logs/cost.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.look :as look]
            [probe.util :as u])
  (:import [java.util.concurrent CompletableFuture ConcurrentLinkedQueue]
           [java.util.concurrent.locks LockSupport]
           [java.util.function BiConsumer]))

(defn- drain!
  "Empties the queue of the events' times. Gives label → {:event [ms] :append [ms]}."
  []
  (loop [out {}]
    (if-let [[label ev ap] (.poll ^ConcurrentLinkedQueue g/times)]
      (recur (-> out
                 (update-in [label :event] (fnil conj []) (u/nanos->ms ev))
                 (update-in [label :append] (fnil conj []) (u/nanos->ms ap))))
      out)))

(defn- send-all!
  "Sends n things of one way without waiting for an answer. Takes the
  clients, the way, the label, n, and the things a second (nil: as fast as
  the loop goes). Waits until all were answered, at most 120 s. Gives
  {:client [ms] :failed n :wall ms :first-failure text}."
  [ctx v label n per-second]
  (let [{:keys [things k]} ctx
        done (atom [])
        failed (atom [])
        period (when per-second (/ 1e9 per-second))
        t0 (System/nanoTime)]
    (dotimes [i n]
      (when period
        (let [due (+ t0 (long (* i period)))]
          (while (< (System/nanoTime) due) (LockSupport/parkNanos 20000))))
      (let [t (System/nanoTime)
            ^CompletableFuture f (foreign-append-async! things (merge v {:k k :id (str label "-" i) :label label}) :ack)]
        (.whenComplete f (reify BiConsumer
                           (accept [_ r e]
                             (if e
                               (swap! failed conj (u/root-message e))
                               (swap! done conj (u/nanos->ms (- (System/nanoTime) t)))))))))
    (u/wait-until #(= n (+ (count @done) (count @failed))) 120000)
    {:client @done :failed (count @failed) :first-failure (first @failed)
     :wall (u/nanos->ms (- (System/nanoTime) t0))}))

(defn- one-by-one!
  "Sends n things of one way from one client, each when the one before was answered. Gives the same map as `send-all!`."
  [ctx v label n]
  (let [{:keys [things k]} ctx
        t0 (System/nanoTime)
        res (reduce (fn [acc i]
                      (let [t (System/nanoTime)]
                        (try
                          (foreign-append! things (merge v {:k k :id (str label "-" i) :label label}) :ack)
                          (update acc :client conj (u/nanos->ms (- (System/nanoTime) t)))
                          (catch Throwable e
                            (-> acc (update :failed inc) (update :first-failure #(or % (u/root-message e))))))))
                    {:client [] :failed 0 :first-failure nil}
                    (range n))]
    (assoc res :wall (u/nanos->ms (- (System/nanoTime) t0)))))

(defn- say-load!
  "Says the three times of one way under one load."
  [load-word v label {:keys [client failed first-failure wall]}]
  (Thread/sleep 200)
  (let [{:keys [event append]} (get (drain!) label)
        n (count client)]
    (u/say "COST   " load-word "·" (g/variant-name v)
           "· event:" (u/stats event)
           "· append:" (if (= :none (:place v)) "none" (u/stats append))
           "· client:" (u/stats client)
           "· all in" (look/fmt wall) "ms =" (look/fmt (when (pos? wall) (/ (* 1000.0 n) wall))) "a second"
           (if (pos? failed) (str "· FAILED " failed ", the first: " first-failure) ""))))

(defn -main [& _]
  (u/open-log! "logs/cost.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc g/GateProbe {:tasks 4 :threads 4})
      (let [ctx0 (look/open ipc)
            ctx (assoc ctx0 :p 1 :k ((:ks ctx0) 1))
            ways (into [{:place :none}] g/variants)]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads. Every thing goes to task 1. The event reads three rows and writes three. Times in ms; one in-process cluster on one machine, an order of size, no more.")
        ;; the JVM is made warm on every way first
        (doseq [v ways] (one-by-one! ctx v (str "warm/" (g/variant-tag v)) 150))
        (send-all! ctx {:place :none} "warm/atonce" 2000 nil)
        (drain!)
        (u/say "RUN     ---- steady: 300 things at 100 a second ----")
        (doseq [v ways]
          (let [label (str "steady/" (g/variant-tag v))]
            (say-load! "steady 100/s" v label (send-all! ctx v label 300 100))))
        (u/say "RUN     ---- one client: 500 things, each sent when the one before was answered ----")
        (doseq [v ways]
          (let [label (str "one/" (g/variant-tag v))]
            (say-load! "one client" v label (one-by-one! ctx v label 500))))
        (u/say "RUN     ---- at once: 2000 things, none waiting for another ----")
        (doseq [v ways]
          (let [label (str "atonce/" (g/variant-tag v))]
            (say-load! "at once" v label (send-all! ctx v label 2000 nil))))
        (let [sent (:end-offset (foreign-depot-partition-info (:things ctx) 1))
              begun (reduce + (vals @g/attempts))
              recs (g/read-range (:log ctx) 1 0 (g/log-end (:log ctx) 1))
              row-count (look/row ctx (:k ctx) "count")]
          (u/say "RUN     source records in partition 1:" sent "· ids sent:" (count @g/attempts)
                 "· events begun:" begun "· so events that were a second attempt:" (- begun sent)
                 "· task globals made ready again:" (g/restarts)
                 "· records in the second depot's partition 1:" (count recs))
          (u/say "RUN     the row \"count\": each event reads it, adds one and writes it. Events begun:" begun
                 "· the row holds:" (pr-str row-count) "· raises that were lost:" (- begun (long row-count)))
          ;; which ways lose a raise: the count that each record carries, by way and load
          (u/say "LOST    load · the append · records · distinct counts among them · records that carry a count another record of the same load carries too")
          (doseq [load ["steady" "one" "atonce"] v g/variants]
            (let [prefix (str load "/" (g/variant-tag v) "-")
                  cs (mapv :count (filter #(.startsWith ^String (:id %) prefix) recs))]
              (u/say "LOST   " load "·" (g/variant-name v) "·" (count cs) "·" (count (distinct cs))
                     "·" (- (count cs) (count (distinct cs)))))))
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
