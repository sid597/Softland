(ns probe.seen
  "The run for question 2: for an event that does not fail, is its row given
  to a client before its record is readable in the second depot, after it,
  or never one without the other? And what does the client that appended
  the source record with :ack find when its append returns?

  Two parts, each for all twelve ways of the append.

    A  held open. The event writes and appends and then waits for a future,
       its task's thread free, so that a reader can be answered while the
       event has not ended. The run reads the row and the second depot,
       says what it was given, then completes the future and reads again.

    B  as it comes. 200 things, one after the other, each appended with
       :ack. One thread reads without a pause, in this order: the second
       depot's end offset, the thing's row, the end offset again. A record
       that the first read shows while the second shows no row was readable
       before the row was given. A row that the second read shows while the
       third shows no record was given before the record was readable. The
       client itself reads both at once when its append returns.

  All things go to one task, task 1.

  Run:  clojure -M:seen   (from this folder; the log is logs/seen.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.look :as look]
            [probe.util :as u])
  (:import [java.util.concurrent CompletableFuture TimeUnit]))

(defn- held-open!
  "Part A for one way of the append. Takes the clients and the way."
  [ctx v]
  (let [{:keys [things log k p]} ctx
        id (str "A-" (g/variant-tag v))
        e (g/log-end log p)
        f (foreign-append-async! things (merge v {:k k :id id :wait-first 1}) :ack)]
    (if-not (u/wait-until #(g/mark id 1 :waiting) 10000)
      (u/say "HELD   " (g/variant-name v) ": the event never came to its wait")
      (let [_ (Thread/sleep 50)
            row-during (look/row ctx k id)
            rec-during (mapv (juxt :id :attempt) (g/read-range log p e (g/log-end log p)))
            done-during (.isDone ^CompletableFuture f)]
        (.complete (g/the-future id) true)
        (let [ret (try (pr-str (.get ^CompletableFuture f 10 TimeUnit/SECONDS)) (catch Throwable t (u/root-message t)))
              row-after (look/row ctx k id)
              rec-after (mapv (juxt :id :attempt) (g/read-range log p e (g/log-end log p)))]
          (u/say "HELD   " (g/variant-name v) "· while the event waited, a reader was given: row" (pr-str (some-> row-during :attempt))
                 "· records" (pr-str rec-during) "· the client's append had returned:" done-during
                 "‖ after the event ended: row" (pr-str (some-> row-after :attempt)) "· records" (pr-str rec-after)
                 "· the client's append gave" ret))))))

(defn- as-it-comes!
  "Part B for one way of the append. Takes the clients, the way, and how many things."
  [ctx v n]
  (let [{:keys [things log rows k p]} ctx
        tag (g/variant-tag v)
        current (atom nil)          ; {:id :off} of the thing on its way, or nil
        counts (atom {})            ; id → {:record-first n :row-first n}
        stop (atom false)
        reader (doto (Thread.
                      (fn []
                        (while (not @stop)
                          (when-let [{:keys [id off]} @current]
                            (let [r1 (g/log-end log p)
                                  w (look/row ctx k id)
                                  r2 (g/log-end log p)]
                              (when (= id (:id @current))
                                (cond
                                  (and (> r1 off) (nil? w)) (swap! counts update-in [id :record-first] (fnil inc 0))
                                  (and (some? w) (<= r2 off)) (swap! counts update-in [id :row-first] (fnil inc 0)))))))))
                 (.setDaemon true)
                 (.start))
        at-return (atom {:row 0 :record 0 :both 0})
        late (atom [])
        took (atom [])]
    (dotimes [i n]
      (let [id (str "B-" tag "-" i)
            off (g/log-end log p)]
        (reset! current {:id id :off off})
        (let [t (System/nanoTime)
              _ (foreign-append! things (merge v {:k k :id id}) :ack)
              t1 (System/nanoTime)
              ;; the client's own two reads, the order turned on each thing
              [row? rec?] (if (even? i)
                            (let [r (some? (look/row ctx k id)) d (> (g/log-end log p) off)] [r d])
                            (let [d (> (g/log-end log p) off) r (some? (look/row ctx k id))] [r d]))]
          (swap! took conj (u/nanos->ms (- t1 t)))
          (swap! at-return (fn [m] (cond-> m row? (update :row inc) rec? (update :record inc) (and row? rec?) (update :both inc))))
          (when-not rec?
            (let [t2 (System/nanoTime)]
              (u/wait-until #(> (g/log-end log p) off) 5000)
              (swap! late conj (u/nanos->ms (- (System/nanoTime) t2)))))
          (reset! current nil))))
    (reset! stop true)
    (.join reader 2000)
    (let [cs (vals @counts)
          rec-first (count (filter :record-first cs))
          row-first (count (filter :row-first cs))
          both (count (filter #(and (:record-first %) (:row-first %)) cs))]
      (u/say "COMES  " (g/variant-name v) "· of" n "things: the record was readable before the row was given for" rec-first
             "· the row was given before the record was readable for" row-first "· both seen for one thing:" both
             "‖ at the return of the client's :ack append: row found" (:row @at-return) "· record found" (:record @at-return)
             "· both" (:both @at-return)
             (if (seq @late) (str "· a record that was missing came " (u/stats @late) " later") "")
             "‖ the append call:" (u/stats @took)))))

(defn -main [& _]
  (u/open-log! "logs/seen.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc g/GateProbe {:tasks 4 :threads 4})
      (let [ctx0 (look/open ipc)
            ctx (assoc ctx0 :p 1 :k ((:ks ctx0) 1))]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads. Every thing goes to task 1 · no thing fails in this run")
        (dotimes [i 50]
          (foreign-append! (:things ctx) {:k (:k ctx) :id (str "warm-" i) :place :after :boundary false :ack :append-ack} :ack))
        (u/say "RUN     ---- A: the event held open by a wait that leaves the thread free ----")
        (doseq [v g/variants] (held-open! ctx v))
        (u/say "RUN     ---- B: as it comes, 200 things for each way ----")
        (doseq [v g/variants] (as-it-comes! ctx v 200))
        (u/say "RUN     events begun in all:" (reduce + (vals @g/attempts)) "for" (count @g/attempts) "source records"
               "· task globals made ready again:" (g/restarts)
               "· records in the second depot's partition 1:" (g/log-end (:log ctx) 1))
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
