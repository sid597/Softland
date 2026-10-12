(ns probe.order
  "The run for question 3, and for the failures that question 1 asks for
  beside a throw in the event: which source records run again when one
  fails, and what their appends leave in the second depot.

  Two launches, since the second needs an option of the topology set.

  `clojure -M:order` (log: logs/order.log), Rama's own options:

    O1  What a throw runs again. Six things on task 1 and three on task 2
        are appended with :ack and succeed. Then one thing on task 1
        throws at its end. Around the throw, fresh reads are sent from
        outside every 20 ms, each on a thread of its own, to tell when a
        client is answered again.
    O2  The same after 35 seconds with no failure, since Rama writes a
        stream topology's progress every 30 seconds.
    O3  N throws while N+1 and N+2 wait for the thread behind it.
    O4  An attempt to have N+1 and N+2 answered while N is still on its
        way: N writes, appends and then waits for a future, the thread
        free; N+1 and N+2 are appended with :ack meanwhile; then the run
        completes the future and N throws. The run shows that this state
        is not reached on one task: while N waits, no later source record
        of the task begins, and Rama fails the batch that N holds when
        `topology.stream.timeout.seconds` has passed (5 here, Rama's own
        value as its line says). So O4 shows that failure, which is no
        throw and starts no worker again, and what :all-after runs again.

  `clojure -M:order timeout` (log: logs/order-timeout.log), with
  `topology.stream.timeout.seconds` set to 3:

    T1  The same as O4 with an earlier thing E before N and no throw asked:
        N waits past the timeout. Which run again, and what a reader is
        given meanwhile. Then the future of N's first attempt is
        completed, to see whether the old attempt goes on."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.look :as look]
            [probe.util :as u])
  (:import [java.util.concurrent CompletableFuture TimeUnit]
           [java.util.concurrent.locks LockSupport]))

(def plain
  "The way of the append of every thing that is not under test: write, then append with :append-ack, no boundary."
  {:place :after :boundary false :ack :append-ack})

(defn- ack!
  "Takes the clients and a thing. Appends it with :ack, waiting at most 20 s. Says and gives what the client was given."
  [ctx thing]
  (let [t (u/ms)
        ret (u/within 20000 #(foreign-append! (:things ctx) thing :ack))
        out (if (vector? ret) (second ret) (pr-str ret))]
    (u/say "CLIENT  the :ack append of" (:id thing) "gave" out "after" (look/fmt (- (u/ms) t)) "ms")
    out))

(defn- outcome
  "Takes a future of an append and a limit in ms. Gives what it gave, as text."
  [^CompletableFuture f limit-ms]
  (try (pr-str (.get f limit-ms TimeUnit/MILLISECONDS))
       (catch Throwable t (u/root-message t))))

(defn- tail
  "Takes the clients, a partition and an offset. Gives the records of the second depot from there on as [id attempt]."
  [ctx p from]
  (mapv (juxt :id :attempt) (g/read-range (:log ctx) p from (g/log-end (:log ctx) p))))

(defn- given
  "Takes the clients, a key, an id, a partition and an offset. Reads from
  outside, each read waiting at most 3 s. Gives as text the row that a
  reader is given under the id and the records of the second depot from the
  offset on."
  [ctx k id p from]
  (let [r (u/within 3000 #(look/row ctx k id))
        l (u/within 3000 #(tail ctx p from))]
    (str "row of " id ": " (pr-str r) " · records: " (pr-str l))))

(defn- pings!
  "Starts a thread that, every 20 ms until the atom holds true, sends one
  read of a row and one read of the second depot's end offset, each on a
  thread of its own, and keeps for each {:kind :at :took :r}. Takes the
  clients, a key, an id, a partition, the atom, and the atom of results."
  [ctx k id p stop results]
  (doto (Thread.
         (fn []
           (while (not @stop)
             (doseq [[kind f] [[:row #(look/row ctx k id)] [:depot #(g/log-end (:log ctx) p)]]]
               (let [t (u/ms)]
                 (future
                   (let [r (try (f) :answered (catch Throwable _ :threw))]
                     (swap! results conj {:kind kind :at t :took (- (u/ms) t) :r r})))))
             (LockSupport/parkNanos 20000000))))
    (.setDaemon true)
    (.start)))

(defn- say-pings!
  "Says what the fresh reads around a throw were given. Takes the results and the ms of the throw."
  [results t-throw]
  (doseq [kind [:row :depot]]
    (let [rs (sort-by :at (filter #(= kind (:kind %)) results))
          rel (fn [r] (- (:at r) t-throw))
          before (filter #(neg? (rel %)) rs)
          after (filter #(not (neg? (rel %))) rs)
          slow (filter #(> (:took %) 100) rs)
          first-fast (first (filter #(and (= :answered (:r %)) (< (:took %) 100)) after))]
      (u/say "PINGS  " (name kind) "reads sent:" (count rs) "· answered within 100 ms:" (count (filter #(and (= :answered (:r %)) (<= (:took %) 100)) rs))
             "· answered later:" (count (filter #(= :answered (:r %)) slow))
             "· threw:" (count (filter #(= :threw (:r %)) rs)))
      (when (seq slow)
        (u/say "PINGS  " (name kind) "the reads that took more than 100 ms were sent from"
               (look/fmt (rel (first slow))) "to" (look/fmt (rel (last slow))) "ms around the throw, and took"
               (u/stats (map :took slow))
               "· by outcome:" (pr-str (frequencies (map :r slow)))))
      (u/say "PINGS  " (name kind) "the first read after the throw that was answered within 100 ms was sent"
             (look/fmt (some-> first-fast rel)) "ms after the throw and took" (look/fmt (:took first-fast)) "ms"))))

(defn- began
  "Takes ids. Gives each with how many times its event began, as text."
  [ids]
  (pr-str (mapv (fn [id] [id (get @g/attempts id 0)]) ids)))

;; ---- the launch with Rama's own options -------------------------------------

(defn- o1!
  "O1 and O2: things that succeeded, then one that throws. Takes the clients
  and a word for the ids."
  [ctx word]
  (let [{:keys [things ks]} ctx
        k1 (ks 1) k2 (ks 2)
        ps (mapv #(str word "-p" %) (range 1 7))
        qs (mapv #(str word "-q" %) (range 1 4))
        x (str word "-x")
        e1 (g/log-end (:log ctx) 1) e2 (g/log-end (:log ctx) 2)]
    (doseq [id ps] (ack! ctx (merge plain {:k k1 :id id})))
    (doseq [id qs] (ack! ctx (merge plain {:k k2 :id id})))
    (u/say "OUTSIDE before the throw. Task 1:" (given ctx k1 (last ps) 1 e1))
    (u/say "OUTSIDE before the throw. Task 2:" (given ctx k2 (last qs) 2 e2))
    (let [stop (atom false) results (atom []) r0 (g/restarts)]
      (pings! ctx k1 (first ps) 1 stop results)
      (Thread/sleep 300)
      (ack! ctx (merge plain {:k k1 :id x :loud true :fail-at :end :fail-first 1}))
      (u/wait-until #(g/mark x 2 :done) 30000)
      (u/say "INSIDE  events begun so far:" (began (concat ps qs [x]))
             "· task globals made ready again:" (- (g/restarts) r0)
             "· from the throw to the next begin of" x ":" (look/fmt (some-> (g/mark x 2 :begin) (- (g/mark x 1 :threw)))) "ms")
      ;; a thing appended right after the retry, with :append-ack, so that the append and the event can be told apart
      (let [t (u/ms) id (str word "-after")
            ret (u/within 30000 #(foreign-append! things (merge plain {:k k1 :id id}) :append-ack))
            t-ret (u/ms)]
        (u/wait-until #(g/mark id 1 :done) 30000)
        (u/say "CLIENT  a thing appended with :append-ack" (look/fmt (- t (g/mark x 1 :threw))) "ms after the throw:"
               "the append gave" (pr-str ret) "after" (look/fmt (- t-ret t)) "ms"
               "· its event began" (look/fmt (some-> (g/mark id 1 :begin) (- t))) "ms after the call"))
      (Thread/sleep 1500)
      (reset! stop true)
      (Thread/sleep 11000)
      (say-pings! @results (g/mark x 1 :threw))
      (u/say "RESULT  events begun:" (began (concat ps qs [x (str word "-after")])))
      (u/say "RESULT  the second depot, task 1, since the scenario began:" (pr-str (tail ctx 1 e1)))
      (u/say "RESULT  the second depot, task 2, since the scenario began:" (pr-str (tail ctx 2 e2))))))

(defn- o3!
  "O3: N throws while N+1 and N+2 wait behind it. Takes the clients and the way of the append."
  [ctx v]
  (let [{:keys [things ks]} ctx
        k (ks 1) tag (g/variant-tag v)
        h {:k k :id (str "O3h-" tag) :place :none :pause {:start 250} :pause-first 1}
        ids (mapv #(str "O3-" tag "-n" %) (range 3))
        e (g/log-end (:log ctx) 1) r0 (g/restarts) before @g/attempts]
    (u/say "RUN     ---- O3 ·" (g/variant-name v) "· N is" (first ids) "----")
    (foreign-append-async! things h nil)
    (u/wait-until #(g/mark (:id h) 1 :begin) 20000)
    (let [fs (mapv (fn [id i]
                     (foreign-append-async! things (merge v {:k k :id id :loud true}
                                                          (when (zero? i) {:fail-at :end :fail-first 1}))
                                            :ack))
                   ids (range))]
      (u/say "CLIENT  the three :ack appends gave:" (pr-str (mapv #(outcome % 30000) fs)))
      (u/wait-until #(every? (fn [id] (g/mark id 2 :done)) ids) 30000)
      (Thread/sleep 6000)
      (u/say "RESULT  events begun:" (began ids) "· task globals made ready again:" (- (g/restarts) r0)
             "· events of other things that began again:" (count (remove (set ids) (keys (read-string (look/began-since before))))))
      (u/say "RESULT  the second depot, the three things only, in order:"
             (pr-str (filterv #((set ids) (first %)) (tail ctx 1 e)))))))

(defn- o4!
  "O4: N+1 and N+2 are answered while N is on its way; then N throws. Takes the clients and the way of the append."
  [ctx v]
  (let [{:keys [things ks]} ctx
        k (ks 1) tag (g/variant-tag v)
        ids (mapv #(str "O4-" tag "-n" %) (range 3))
        n (first ids)
        e (g/log-end (:log ctx) 1) r0 (g/restarts) before @g/attempts]
    (u/say "RUN     ---- O4 ·" (g/variant-name v) "· N is" n "----")
    (let [fn0 (foreign-append-async! things (merge v {:k k :id n :loud true :wait-first 1
                                                      :fail-at :after-wait :fail-first 1})
                                     :ack)]
      (u/wait-until #(g/mark n 1 :waiting) 20000)
      (u/say "OUTSIDE while N waits:" (given ctx k n 1 e))
      (doseq [id (rest ids)]
        (ack! ctx (merge v {:k k :id id :loud true}))
        (u/say "OUTSIDE after that append:" (given ctx k id 1 e)))
      (u/say "OUTSIDE while N still waits:" (given ctx k n 1 e))
      (.complete (g/the-future n) true)
      (u/say "CLIENT  the :ack append of N gave" (outcome fn0 30000))
      (u/wait-until #(every? (fn [id] (g/mark id 2 :done)) ids) 30000)
      (Thread/sleep 6000)
      (u/say "RESULT  events begun:" (began ids) "· task globals made ready again:" (- (g/restarts) r0)
             "· events of other things that began again:" (count (remove (set ids) (keys (read-string (look/began-since before))))))
      (u/say "RESULT  the second depot, the three things only, in order:"
             (pr-str (filterv #((set ids) (first %)) (tail ctx 1 e))))
      (u/say "OUTSIDE at the end: row of N:" (pr-str (look/row ctx k n))
             "· row of N+1:" (pr-str (look/row ctx k (second ids)))
             "· row of N+2:" (pr-str (look/row ctx k (nth ids 2)))))))

(defn- run-default! [ipc]
  (let [ctx (look/open ipc)]
    (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads, Rama's own options · source {:start-from :beginning :retry-mode :all-after} · keys by task:" (pr-str (:ks ctx)))
    (u/say "RUN     ---- O1: nine things succeed, then one on task 1 throws ----")
    (o1! ctx "O1")
    (u/say "RUN     ---- O2: 35 seconds with no failure, then the same again ----")
    (Thread/sleep 35000)
    (o1! ctx "O2")
    (o3! ctx {:place :after :boundary false :ack :append-ack})
    (o3! ctx {:place :after :boundary true :ack :append-ack})
    (o4! ctx {:place :after :boundary false :ack :append-ack})
    (o4! ctx {:place :after :boundary true :ack :append-ack})
    (u/say "RUN     at the end, events begun of O1's things:" (began ["O1-p1" "O1-p6" "O1-q1" "O1-x" "O1-after"])
           "· of O2's things:" (began ["O2-p1" "O2-p6" "O2-q1" "O2-x" "O2-after"]))
    (u/say "RUN     events begun in all:" (reduce + (vals @g/attempts)) "for" (count @g/attempts) "source records"
           "· records in the second depot, by partition:" (pr-str (mapv #(g/log-end (:log ctx) %) (range 4))))))

;; ---- the launch with the timeout set ----------------------------------------

(defn- t1!
  "T1: N waits past the stream timeout. Takes the clients and the way of the append."
  [ctx v]
  (let [{:keys [things ks]} ctx
        k (ks 1) tag (g/variant-tag v)
        e-id (str "T1-" tag "-e")
        ids (mapv #(str "T1-" tag "-n" %) (range 3))
        n (first ids)
        e (g/log-end (:log ctx) 1) r0 (g/restarts)]
    (u/say "RUN     ---- T1 ·" (g/variant-name v) "· N is" n "----")
    (ack! ctx (merge plain {:k k :id e-id}))
    (let [t (u/ms)
          fn0 (foreign-append-async! things (merge v {:k k :id n :loud true :wait-first 1 :loud-first 5}) :ack)]
      (u/wait-until #(g/mark n 1 :waiting) 20000)
      (u/say "OUTSIDE while N waits:" (given ctx k n 1 e))
      (doseq [id (rest ids)]
        (ack! ctx (merge v {:k k :id id :loud true :loud-first 5}))
        (u/say "OUTSIDE after that append:" (given ctx k id 1 e)))
      (let [stop (atom false) seen (atom [])]
        (look/watch-row ctx k n stop seen)
        (u/say "CLIENT  the :ack append of N gave" (outcome fn0 60000) "after" (look/fmt (- (u/ms) t)) "ms")
        (u/wait-until #(g/mark n 2 :done) 60000)
        (Thread/sleep 1500)
        (reset! stop true))
      (u/say "RESULT  from N's first begin to its second:" (look/fmt (some-> (g/mark n 2 :begin) (- (g/mark n 1 :begin)))) "ms"
             "· events begun:" (began (cons e-id ids)) "· task globals made ready again:" (- (g/restarts) r0))
      (u/say "RESULT  the second depot since the scenario began:" (pr-str (tail ctx 1 e)))
      (u/say "RUN     now the future of N's first attempt is completed")
      (.complete (g/the-future n) true)
      (Thread/sleep 1500)
      (u/say "RESULT  N's first attempt went on behind its wait:" (boolean (g/mark n 1 :after-wait))
             "· came to the end of its code:" (boolean (g/mark n 1 :done))
             "·" (given ctx k n 1 e))
      (Thread/sleep 5000)
      (u/say "RESULT  five seconds later, events begun:" (began (cons e-id ids)) "·" (given ctx k n 1 e)))))

(defn- run-timeout! [ipc]
  (let [ctx (look/open ipc)]
    (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads, topology.stream.timeout.seconds set to" g/stream-timeout-seconds
           "· source {:start-from :beginning :retry-mode :all-after}")
    (t1! ctx {:place :after :boundary false :ack :append-ack})
    (t1! ctx {:place :after :boundary true :ack :append-ack})
    (u/say "RUN     events begun in all:" (reduce + (vals @g/attempts)) "for" (count @g/attempts) "source records"
           "· records in the second depot's partition 1:" (g/log-end (:log ctx) 1))))

(defn -main [& args]
  (let [timeout? (= "timeout" (first args))]
    (u/open-log! (if timeout? "logs/order-timeout.log" "logs/order.log"))
    (when timeout? (alter-var-root #'g/stream-timeout-seconds (constantly 3)))
    (try
      (with-open [ipc (rtest/create-ipc)]
        (rtest/launch-module! ipc g/GateProbe {:tasks 4 :threads 4})
        (if timeout? (run-timeout! ipc) (run-default! ipc))
        (u/say "RUN     END"))
      (catch Throwable t
        (u/say "RUN     THREW" (u/root-message t))
        (.printStackTrace t))))
  (shutdown-agents)
  (System/exit 0))
