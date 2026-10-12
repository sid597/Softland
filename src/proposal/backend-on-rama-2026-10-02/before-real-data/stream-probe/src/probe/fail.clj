(ns probe.fail
  "The run for question 1: an event writes its rows and appends a record to
  the second depot, and then its source record fails. What the second depot
  holds after the failure and after the retry.

  The failure is forced by a throw in three places, each for all twelve
  ways of the append (`probe.gate/variants`):

    F1  the event throws at its end, after the write and the append.
    F2  a later source record on the same task throws. Three things are
        sent: H, which holds the task's thread for 250 ms; then, while H
        holds it, A (the way under test, no failure asked) and B (write,
        then append with :append-ack, then a throw at its end). So A and B
        wait together for the thread, and B throws after A's event began.
    F3  the event's tree throws after the event's writes were committed:
        behind the write and the append stands a commit boundary, and the
        next event of the tree throws.

  Each thing fails on its first attempt only. All things go to one task,
  task 1. The client appends with `:ack` and says what it was given.

  Nothing is read from outside while a scenario runs: a throw makes Rama
  start the worker again, and a client's read that is on its way then is
  answered by nothing (see `probe.order`). What a scenario left is told
  from inside, by the marks of the events, and from the record of each
  attempt, which carries `:row-before`: the attempt of the row that the
  event found under its id. At the end the run reads the partition of the
  second depot once, whole.

  Run:  clojure -M:fail   (from this folder; the log is logs/fail.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.look :as look]
            [probe.util :as u])
  (:import [java.util.concurrent CompletableFuture TimeUnit]))

(defonce ^{:doc "One map for each scenario, in order, for the table at the end."}
  scenarios (atom []))

(defn- others-again
  "Takes the map of attempts as it was before a scenario and the scenario's
  own ids. Gives how many other ids began since, and how many times in all."
  [before own]
  (let [now @g/attempts
        ds (for [[id n] now :when (not (contains? own id))
                 :let [d (- n (get before id 0))] :when (pos? d)] d)]
    [(count ds) (reduce + 0 ds)]))

(defn- settle!
  "Ends one scenario. Takes its word, the way of the append, the id whose
  records count, the id that threw, the attempts and restarts before, the
  scenario's own ids, and what the client was given. Says the result from
  inside and keeps it for the table."
  [way v id thrower before r0 own client]
  (let [t-throw (g/mark thrower 1 :threw)
        t-again (g/mark thrower 2 :begin)
        [others runs] (others-again before own)
        result {:way way :variant (g/variant-name v) :id id :thrower thrower
                :throw-to-retry (when (and t-throw t-again) (- t-again t-throw))
                :restarts (- (g/restarts) r0)
                :others others :client client}]
    (u/say "RESULT  from the throw to the next begin of" thrower ":" (look/fmt (:throw-to-retry result)) "ms"
           "· task globals made ready again:" (:restarts result)
           "· things of earlier scenarios whose event began again:" others (str "(" runs " begins)"))
    (swap! scenarios conj result)))

(defn- one-fails!
  "F1 and F3: one thing that fails on its first attempt. Takes the clients,
  the scenario's word, the way of the append, and what to add to the thing."
  [ctx way v extra]
  (let [{:keys [things k]} ctx
        id (str way "-" (g/variant-tag v))
        thing (merge v {:k k :id id :loud true} extra)
        r0 (g/restarts) before @g/attempts]
    (u/say "RUN     ----" way "·" (g/variant-name v) "----")
    (let [t (u/ms)
          ret (u/within 40000 #(foreign-append! things thing :ack))
          client (str (if (vector? ret) (second ret) (pr-str ret)) " after " (look/fmt (- (u/ms) t)) " ms")]
      (u/say "CLIENT  the :ack append of" id "gave" client
             "· from the call to the event's first begin:" (look/fmt (some-> (g/mark id 1 :begin) (- t))) "ms")
      (when-not (u/wait-until #(g/mark id 2 :done) 40000)
        (u/say "RUN     the second attempt of" id "did not come to its end within 40 s"))
      (Thread/sleep 400)
      (settle! way v id id before r0 #{id} client))))

(defn- outcome
  "Takes a future of an append and a limit in ms. Gives what it gave, as text."
  [^CompletableFuture f limit-ms]
  (try (pr-str (.get f limit-ms TimeUnit/MILLISECONDS))
       (catch Throwable t (u/root-message t))))

(defn- later-fails!
  "F2: H holds the thread; A and B wait behind it; B throws at its end.
  Takes the clients and the way of A's append."
  [ctx v]
  (let [{:keys [things k]} ctx
        tag (g/variant-tag v)
        h {:k k :id (str "F2h-" tag) :place :none :pause {:start 250} :pause-first 1}
        a (merge v {:k k :id (str "F2a-" tag) :loud true})
        b {:k k :id (str "F2b-" tag) :place :after :boundary false :ack :append-ack
           :loud true :fail-at :end :fail-first 1}
        r0 (g/restarts) before @g/attempts]
    (u/say "RUN     ---- F2 ·" (g/variant-name v) "---- (A is" (:id a) ", the thrower is" (str (:id b) ")"))
    (foreign-append-async! things h nil)
    (u/wait-until #(g/mark (:id h) 1 :begin) 20000)
    (let [fa (foreign-append-async! things a :ack)
          fb (foreign-append-async! things b :ack)
          ra (outcome fa 40000)
          rb (outcome fb 40000)
          client (str "A: " ra " · B: " rb)]
      (u/say "CLIENT  the :ack append of" (:id a) "gave" ra "· that of" (:id b) "gave" rb)
      (when-not (u/wait-until #(and (g/mark (:id b) 2 :done)
                                    (g/mark (:id a) (get @g/attempts (:id a)) :done))
                              40000)
        (u/say "RUN     A and B did not both come to their end within 40 s"))
      (Thread/sleep 400)
      (u/say "RESULT  A's first attempt came behind its append at" (look/fmt (g/mark (:id a) 1 :end))
             "· B threw at" (look/fmt (g/mark (:id b) 1 :threw))
             "· A's first attempt came to the end of its code:" (boolean (g/mark (:id a) 1 :done)))
      (settle! "F2" v (:id a) (:id b) before r0 #{(:id h) (:id a) (:id b)} client))))

(defn- say-table!
  "Reads the partition of the second depot whole and says, for each
  scenario, what the first two attempts of its thing left there."
  [ctx]
  (let [{:keys [log p]} ctx
        end (g/log-end log p)
        recs (g/read-range log p 0 end)
        by-id (group-by :id recs)]
    (u/say "TABLE   the second depot's partition" p "holds" end "records. For each scenario, of its thing:")
    (u/say "TABLE   way · the append · records of attempt 1 · records of attempt 2 · the row that attempt 2 found (1: the failed attempt's row had been committed; nil: it was gone) · count carried by attempt 1 and by attempt 2 · throw to retry ms · restarts · the client's :ack append")
    (doseq [{:keys [way variant id throw-to-retry restarts client]} @scenarios]
      (let [mine (get by-id id)
            of (fn [n] (filter #(= n (:attempt %)) mine))
            r1 (of 1) r2 (of 2)]
        (u/say "TABLE  " way "·" variant "·" (count r1) "·" (count r2)
               "·" (pr-str (:row-before (first r2)))
               "·" (pr-str (:count (first r1))) (pr-str (:count (first r2)))
               "·" (look/fmt throw-to-retry) "·" restarts
               "·" (subs client 0 (min 110 (count client))))))
    (let [n-attempts (fn [id] (get @g/attempts id))]
      (u/say "TABLE   what the replays added, for four things: [id · times its event began · its records in the second depot]"
             (pr-str (for [id ["warm" "F1-wa-p" "F2a-wa-p" "F3-wa-p"]]
                       [id (n-attempts id) (count (get by-id id))]))))
    (u/say "TABLE   events begun in all:" (reduce + (vals @g/attempts)) "for" (count @g/attempts) "source records"
           "· records in the second depot:" end
           "· the row \"count\", which each event that commits raises by one:" (pr-str (look/row ctx (:k ctx) "count")))))

(defn -main [& _]
  (u/open-log! "logs/fail.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc g/GateProbe {:tasks 4 :threads 4})
      (let [ctx0 (look/open ipc)
            ctx (assoc ctx0 :p 1 :k ((:ks ctx0) 1))]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads. Every thing goes to task 1, key" (pr-str (:k ctx))
               "· source {:start-from :beginning :retry-mode :all-after} · the second depot is :disallow and nothing consumes it")
        ;; one thing that does not fail, so that the first event of the JVM is not a measured one
        (foreign-append! (:things ctx) {:k (:k ctx) :id "warm" :place :after :boundary false :ack :append-ack} :ack)
        (doseq [v g/variants]
          (one-fails! ctx "F1" v {:fail-at :end :fail-first 1}))
        (doseq [v g/variants]
          (later-fails! ctx v))
        (doseq [v g/variants]
          (one-fails! ctx "F3" v {:commit-after true :fail-at :after-commit :fail-first 1}))
        (Thread/sleep 6000)
        (say-table! ctx)
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
