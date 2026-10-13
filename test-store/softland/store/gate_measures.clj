(ns softland.store.gate-measures
  "The measures of the gates: a run that is no proof. It prints what it
  measured and holds nothing to a number. One command, from the repository
  root, while no proof and no other measure runs on the machine:

    clojure -M:store-module -m softland.store.gate-measures

  It takes about seven minutes, and more when a gate holds less of D3's
  rates: every send of a rate is waited for before the next rate begins.
  With the one argument `small` it runs each shape with a small load, in
  about a minute, to see that the run works:

    clojure -M:store-module -m softland.store.gate-measures small

  Each line of it starts with MEASURED. The first lines say which tree is
  measured: what `git rev-parse --short HEAD` gives, and how many paths
  under `src-store` differ from that commit. A set of times is printed as
  its count, its least, its middle, the time that 9 of 10 are not above,
  and its most, in milliseconds. It exits 0 unless it cannot run.

  No live read is open in any measure: the run opens doors and no read path
  (`softland.store.read-path`), and each line of a number says so.

  Where an answer is, and when. Each gate keeps what it decided in its own
  tables. No gate appends an answer to a depot: the module's two depots
  hold what clients appended and nothing else (`softland.store.module`).
  The run sends acts through the door and reads answers back through the
  door, and times that. The append of a streaming act returns when the
  streaming gate has run, so a time of the streaming gate is to the return
  of `send-act`. The append of an act of the base returns when the depot
  holds the thing, and its answer is there when the batch that decided it
  is committed on the base's task; nothing waits for a later batch. So a
  time of the base is to the read of the door's wait that gave the answer:
  it can be one sleep of that wait and one read later than the commit.

  What is run, on Rama's in-process cluster with 4 tasks, 2 threads and 2
  workers. Every act goes in through the door.

    D1  an act through the door and a gate: one maker sends acts of one
        fact, a string of 100 bytes under a plain key, each to another place
        of one layer, 100 a second. The streaming gate, timed to the return
        of `send-act`, which is the streaming depot's ack: once with the key
        not marked and once with it marked. And the base, timed to the
        moment at which the door's wait gives \"accepted\": once through a
        door whose wait reads every 200 ms, the door's own number, and once
        through a door whose wait reads every 5 ms. The two streaming
        shapes and the first shape of the base are those of M1 of
        `softland.store.read-measures`. The lines of the streaming gate
        stand beside the M1 lines of the streaming gate in
        `src/proposal/backend-on-rama-2026-10-02/reads/runs/measures-final.txt`;
        the lines of the base stand beside no line of that file, whose
        base gave an answer only for a closed batch
    D2  the same act sent a second time under its act-id, into a streaming
        layer: the gate finds the answer that it gave the first time and
        decides nothing. One maker asks for the act-ids of a series ahead,
        sends each act under its act-id, and then sends each again; both
        sends are timed to the return of `send-act`
    D3  Rama's setting `topology.stream.max.executing.per.task`, at Rama's
        default and at 1. D1's first shape at 1. And the most acts a second
        that one maker holds, in both readings: acts sent at fixed rates
        with no wait for the one before, each rate for some seconds, with
        whether the last was answered within a second of its due moment;
        and one maker that sends each act when the one before is answered

  The setting at 1. The module `Store` does not name the setting and keeps
  Rama's default. This file holds a second module, `StoreAtOne`: it calls
  the definition of `Store` and sets the option for the topology
  `streaming-gate` at the launch. The in-process cluster has no call that
  changes an option of a launched module, so the half of D3 at 1 runs in a
  second cluster, started after the first is closed.

  What a number here is. The cluster runs in the process of the measure, on
  one machine, with every row in memory and no network. So a time is a floor
  for a round trip and says little of a disk, of a network or of a second
  node. One run is an order of size. D3's half at Rama's default runs in the
  first cluster after D1 and D2; its half at 1 runs in a cluster that held
  nothing before it.

  State. The run starts one in-process cluster, launches the module `Store`
  in it and closes it; then it starts a second, launches `StoreAtOne` in it
  and closes it. In the first it opens two doors, the second with a wait
  that reads every 5 ms; in the second, one door. It opens no depot and no
  table itself. It starts `git` twice to say which tree it is on. It reads
  the clock."
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [com.rpl.rama :as rama]
            [com.rpl.rama.test :as rtest]
            [softland.store.door :as door]
            [softland.store.module :as module])
  (:import [com.rpl.rama RamaModule]
           [java.util.concurrent ExecutorService Executors LinkedBlockingQueue ThreadFactory
            ThreadPoolExecutor TimeUnit]
           [java.util.concurrent.atomic AtomicLong]))

(def ^:private loads
  "The loads of the measures, by their names: the plan's own, and a small
  one for a first try. `:acts` and `:warm` are the acts of a shape at 100 a
  second and those before them that are not counted; `:again` is how many
  acts D2 sends a second time; `:rates` are D3's rates in acts a second,
  each held for `:rate-seconds`; `:one-by-one-seconds` is how long D3's
  maker sends one act after the other."
  {:full  {:acts 3000 :warm 500 :again 1000 :rates [200 400 800 1600] :rate-seconds 10 :one-by-one-seconds 10}
   :small {:acts 200 :warm 50 :again 100 :rates [200 400 800 1600] :rate-seconds 2 :one-by-one-seconds 2}})

(def ^:private most-senders
  "The most sends that D3's sender holds in `send-act` at once: each is a
  thread that waits there. A send that is due while that many wait is held
  until a thread is free, and its time from its due moment counts that. So
  a gate is never offered more than this many acts at once, and a rate that
  the gate does not hold shows as times that grow, and not as more load."
  2000)

(def ^:private setting
  "The name of Rama's setting that D3 measures."
  "topology.stream.max.executing.per.task")

;; The module `Store` with Rama's setting at 1 for the streaming gate, for
;; D3 only. Its docstring is set below: `defmodule` takes none.
(rama/defmodule StoreAtOne [setup topologies]
  (.define ^RamaModule module/Store setup topologies)
  (rama/set-launch-topology-dynamic-option! setup "streaming-gate" setting 1))

(alter-meta! #'StoreAtOne assoc
             :doc "The module `Store` by its own definition, with one option of Rama set at
  the launch: the streaming gate runs one event of a task at a time. It
  calls the definition of `softland.store.module/Store` with its own
  `setup` and `topologies`. So it declares the two depots of acts as
  `Store` does, each partitioned by the thing's `:layer`, and adds the two
  gates and the door's two reads by `Store`'s one call; this file writes
  none of that a second time. It has a name of its own, and a door opens on
  that name. Nothing of the store's code names the option.")

;; ---------------------------------------------------------------- what is printed

(defn- said
  "Prints one line that holds no number of a measure."
  [& xs]
  (println (apply print-str "MEASURED" xs))
  (flush))

(defn- measured
  "Prints one line of what was measured, with the words that no live read
  was open."
  [& xs]
  (println (apply print-str "MEASURED" (concat xs ["| no live read open"])))
  (flush))

(defn- ms
  "Takes a span in nanoseconds. Gives it in milliseconds, with three
  decimals."
  [nanos]
  (/ (Math/round (/ (double nanos) 1000.0)) 1000.0))

(defn- times
  "Takes times in nanoseconds. Gives their count, their least, their middle,
  the time that 9 of 10 are not above, and their most, as text."
  [nanos]
  (if (empty? nanos)
    "count 0"
    (let [sorted (vec (sort nanos))
          n (count sorted)
          at (fn [share] (nth sorted (max 0 (dec (long (Math/ceil (* share n)))))))]
      (str "count " n " | least " (ms (first sorted)) " ms | middle " (ms (at 0.5)) " ms | 9 of 10 "
           (ms (at 0.9)) " ms | most " (ms (peek sorted)) " ms"))))

(defn- git-says
  "Starts `git` with the arguments given, in the directory of the run. Gives
  what it printed, trimmed, or \"not told\" when it could not be started or
  did not end well."
  [& args]
  (try (let [{:keys [exit out]} (apply shell/sh "git" args)]
         (if (zero? exit) (str/trim out) "not told"))
       (catch Throwable _ "not told")))

;; ------------------------------------------------------------- through the door

(defn- send!
  "Sends one act of a who into a layer through a door, under a permission;
  under an act-id when one is given. Gives what `send-act` gave."
  ([door who layer permission facts] (send! door who layer permission facts nil))
  ([door who layer permission facts act-id]
   (door/send-act door
                  {:version 1 :who who :layer layer :permission permission
                   :session :gate-measures :facts facts}
                  act-id)))

(defn- accepted!
  "Sends one act and waits until the door's wait gives its answer. Gives
  the answer. It stops the run when the act is not accepted: the measures
  cannot run on a layer that is not as they made it."
  [door who layer permission facts]
  (let [sent (send! door who layer permission facts)
        answer (when-not (:could-not sent)
                 (door/answer-of door (:act-id sent) (:digest sent) 60000))]
    (when-not (= :accepted (:answer answer))
      (throw (ex-info "the measures: an act of the set-up was not accepted" {:sent sent :answer answer})))
    answer))

(def ^:private system-own "The system's permission in the base." [:system :base :base])

(defn- set-up!
  "Makes the base through a door and gives the maker a permission in it: the
  set-up of the read build's measures."
  [door]
  (accepted! door :system :base system-own [{:e :base :k :softland.store/depot :v :microbatch}
                                            {:e :system :k :softland.store/permission :v system-own}])
  (accepted! door :system :base system-own [{:e :maker :k :softland.store/permission :v [:maker :base :base system-own]}]))

(defn- make!
  "Makes a streaming layer whose holder is `:maker`. Gives the maker's
  permission in it."
  [door layer]
  (let [own [:maker layer layer]]
    (accepted! door :maker layer own [{:e layer :k :softland.store/depot :v :streaming}
                                      {:e :maker :k :softland.store/permission :v own}])
    own))

(defn- answer-word
  "Reads the answer of what a send gave, once, through a door. Gives the
  answer's word, `:accepted` for an accepted act; the door's word for a send
  that came back with one; nil for no send."
  [door sent]
  (cond
    (nil? sent) nil
    (:could-not sent) (:could-not sent)
    :else (let [answer (door/answer-of door (:act-id sent) (:digest sent))]
            (or (:answer answer) (:could-not answer)))))

(defn- text-of-100-bytes
  "A string of 100 bytes that says which act it is of."
  [i]
  (let [head (str "act " i " ")]
    (str head (apply str (repeat (- 100 (count head)) "x")))))

(defn- another-place
  "The fact of an act of a measure: a string of 100 bytes at a place of its
  own."
  [i]
  {:e (keyword (str "p" (+ 100000 i))) :k :text :v (text-of-100-bytes i)})

(defn- paced
  "Calls f once for each i below n, at start + i x every-nanos and no
  sooner, with i and the moment at which the call was due, in nanoseconds.
  One thread: a call that is late makes the next ones late."
  [n every-nanos f]
  (let [start (System/nanoTime)]
    (dotimes [i n]
      (let [due (+ start (* (long i) (long every-nanos)))
            wait (- due (System/nanoTime))]
        (when (pos? wait)
          (Thread/sleep (quot wait 1000000) (int (rem wait 1000000))))
        (f i due)))))

(def ^:private every-10-ms "The span between two acts at 100 a second, in nanoseconds." 10000000)

;; ------------------------------------------------------------------------ D1

(defn- streaming-gate-times
  "One maker sends acts of one fact into a streaming layer, 100 a second,
  after acts that are not counted. Takes a door, the layer, the maker's
  permission, how many acts, how many are not counted, and a function from
  the act's number to its fact. Gives {:from-due :from-call :last}: the
  times to the return of `send-act`, in nanoseconds, and the word of the
  last act's answer, read once after the last return."
  [door layer permission n warm fact-of]
  (dotimes [i warm]
    (send! door :maker layer permission [(fact-of (- i warm))]))
  (let [from-due (atom []) from-call (atom []) last-sent (atom nil)]
    (paced n every-10-ms
           (fn [i due]
             (let [called (System/nanoTime)
                   sent (send! door :maker layer permission [(fact-of i)])
                   returned (System/nanoTime)]
               (reset! last-sent sent)
               (when-not (:could-not sent)
                 (swap! from-due conj (- returned due))
                 (swap! from-call conj (- returned called))))))
    {:from-due @from-due :from-call @from-call :last (answer-word door @last-sent)}))

(defn- print-streaming-shape
  "Prints the two lines of one shape of `streaming-gate-times`: from the due
  moment, and from the call. Takes the name of the measure, what the shape
  is, and what `streaming-gate-times` gave."
  [measure what got]
  (measured measure "the streaming gate, 100 acts a second, one fact of 100 bytes each to another place," what
            "| from the act's due moment to the return of send-act:" (times (:from-due got)))
  (measured measure "the streaming gate, the same," what "| from the call of send-act to its return:" (times (:from-call got))
            "| the last act's answer, read once:" (pr-str (:last got))))

(defn- d1-streaming
  "D1, the streaming gate: once with the key not marked, once with it
  marked."
  [{:keys [door]} {:keys [acts warm]}]
  (let [layer :d1
        own (make! door layer)
        plain (streaming-gate-times door layer own acts warm another-place)
        _ (accepted! door :maker layer own [{:e layer :k :softland.store/marked-keys :v #{:text}}])
        marked (streaming-gate-times door layer own acts warm (fn [i] (another-place (+ 50000 i))))]
    (print-streaming-shape "D1" "the key not marked" plain)
    (print-streaming-shape "D1" "the key marked, so each act also writes a row of the facts by value" marked)))

(defn- d1-base
  "D1, the base: the same acts into the base, each timed to the moment at
  which the door's wait gives \"accepted\". Takes a door, how often its
  wait reads, in milliseconds, for the printed line, the number of the
  first act's place, and the load. The sender sends at its pace; each act's
  answer is waited for on a thread of its own, by the door's wait. The
  answer is there when the act's batch is committed on the base's task, and
  the wait reads at its own pace: a time can be one sleep of the wait and
  one read later than that commit. The reads are a load of their own on the
  base's task, the more the more often the wait reads."
  [door every-ms first-number {:keys [acts warm]}]
  (let [^ExecutorService waiters (Executors/newCachedThreadPool)
        from-due (atom []) from-call (atom []) not-accepted (atom 0)
        send-one (fn [i due count?]
                   (let [called (System/nanoTime)
                         sent (send! door :system :base system-own [(another-place (+ first-number i))])]
                     (if (:could-not sent)
                       (swap! not-accepted inc)
                       (.submit waiters
                                ^Runnable
                                (fn []
                                  (let [answer (door/answer-of door (:act-id sent) (:digest sent) 120000)
                                        now (System/nanoTime)]
                                    (if (= :accepted (:answer answer))
                                      (when count?
                                        (swap! from-due conj (- now due))
                                        (swap! from-call conj (- now called)))
                                      (swap! not-accepted inc))))))))]
    (paced warm every-10-ms (fn [i due] (send-one (- i warm) due false)))
    (paced acts every-10-ms (fn [i due] (send-one i due true)))
    (.shutdown waiters)
    (.awaitTermination waiters 180 TimeUnit/SECONDS)
    (measured "D1 the base, 100 acts a second, the same acts, the door's wait reads every" every-ms
              "ms | from the act's due moment to the moment at which the door's wait gives accepted, which is the commit of the act's batch on the base's task and then one sleep of the wait and one read at most:"
              (times @from-due))
    (measured "D1 the base, the same, the door's wait reads every" every-ms "ms | from the call of send-act to that moment:" (times @from-call)
              "| acts that got no accepted answer:" @not-accepted)))

;; ------------------------------------------------------------------------ D2

(defn- d2-again
  "D2, the streaming gate: the same act sent a second time. One maker asks
  for the act-ids of the series ahead, sends each act under its act-id,
  100 a second, and then sends each again under the same act-id, 100 a
  second. A second send of one act under one act-id is the
  same thing but for the time that the door writes on it, and has the
  digest of the first (`softland.store.door/send-act`); the door appends it
  with `:ack`, and the gate finds the answer that it gave the first time
  and decides nothing. Both sends are timed from the call of `send-act` to its
  return. Neither reads the layer's depot, since the act-id is given: so the
  first sends are not D1's shape, and they are printed to stand beside the
  second ones."
  [{:keys [door]} {:keys [again warm]}]
  (let [layer :d2-again
        own (make! door layer)
        fact-of (fn [i] (another-place (+ 200000 i)))
        ids (door/act-ids-for door layer again)]
    (when (:could-not ids)
      (throw (ex-info "the measures: the door gave no act-ids for D2" {:word ids})))
    (dotimes [i warm]
      (send! door :maker layer own [(another-place (- i warm))]))
    (let [round (fn []
                  (let [took (atom []) words (atom 0) last-sent (atom nil)]
                    (paced again every-10-ms
                           (fn [i _]
                             (let [called (System/nanoTime)
                                   sent (send! door :maker layer own [(fact-of i)] (nth ids i))
                                   returned (System/nanoTime)]
                               (reset! last-sent sent)
                               (if (:could-not sent)
                                 (swap! words inc)
                                 (swap! took conj (- returned called))))))
                    {:took @took :words @words :last (answer-word door @last-sent)}))
          first-time (round)
          second-time (round)]
      (measured "D2 the streaming gate, 100 acts a second, each act sent under an act-id asked for ahead, so the send reads no depot of a layer | the first send | from the call of send-act to its return:"
                (times (:took first-time)) "| sends that came back with a word of the door:" (:words first-time)
                "| the last act's answer, read once:" (pr-str (:last first-time)))
      (measured "D2 the streaming gate, the same acts sent a second time under the same act-ids, each with the door's time of its second send, appended with :ack, so the gate finds each answer there | from the call of send-act to its return:"
                (times (:took second-time)) "| sends that came back with a word of the door:" (:words second-time)
                "| the last act's answer, read once:" (pr-str (:last second-time))))))

;; ------------------------------------------------------------------------ D3

(defn- senders
  "Gives a pool of `most-senders` threads, all started, for sends that are
  not waited for. Each thread has a stack of 1 MB whatever the JVM's
  setting is, and is a daemon."
  ^ThreadPoolExecutor []
  (doto (ThreadPoolExecutor. (int most-senders) (int most-senders) 10 TimeUnit/SECONDS
                             (LinkedBlockingQueue.)
                             (reify ThreadFactory
                               (newThread [_ r]
                                 (doto (Thread. nil r "gate-measures-sender" (* 1024 1024))
                                   (.setDaemon true)))))
    (.prestartAllCoreThreads)))

(defn- at-a-rate
  "One maker sends acts of one fact into a streaming layer at a rate for
  some seconds, with no wait for the one before: each send is given to a
  thread of `senders` at its due moment. Then it waits until every send has
  returned, for 300 s at most. Takes a door, the layer, the maker's
  permission, the rate in acts a second, the seconds, and the number of the
  first act's place. Gives {:from-due :from-call :words :most-on-way
  :last-took :last :all-returned?}: the times to the return of `send-act`
  in nanoseconds; the sends that came back with a word of the door, as how
  many came back with each [word, class of what threw]; the most sends
  that were due and had not returned at one moment; the last act's time
  from its due moment, or nil; the word of its answer, read once after
  every send returned; and whether every send returned in the 300 s."
  [door layer permission rate seconds first-number]
  (let [n (* (long rate) (long seconds))
        pool (senders)
        from-due (atom []) from-call (atom []) words (atom [])
        on-way (AtomicLong.) most-on-way (AtomicLong.)
        last-took (atom nil) last-sent (atom nil)]
    (paced n (quot 1000000000 (long rate))
           (fn [i due]
             (let [now-on (.incrementAndGet on-way)]
               (when (< (.get most-on-way) now-on) (.set most-on-way now-on)))
             (.execute pool
                       (fn []
                         (let [called (System/nanoTime)
                               sent (send! door :maker layer permission [(another-place (+ first-number i))])
                               returned (System/nanoTime)]
                           (.decrementAndGet on-way)
                           (if (:could-not sent)
                             (swap! words conj [(:could-not sent) (:threw sent)])
                             (do (swap! from-due conj (- returned due))
                                 (swap! from-call conj (- returned called))))
                           (when (= i (dec n))
                             (reset! last-sent sent)
                             (when-not (:could-not sent) (reset! last-took (- returned due)))))))))
    (.shutdown pool)
    (let [all-returned? (.awaitTermination pool 300 TimeUnit/SECONDS)]
      (when-not all-returned? (.shutdownNow pool))
      {:from-due @from-due :from-call @from-call :words (frequencies @words) :most-on-way (.get most-on-way)
       :last-took @last-took :last (answer-word door @last-sent) :all-returned? all-returned?})))

(defn- one-by-one
  "One maker sends acts of one fact into a streaming layer, each when the
  one before is answered: it sends, reads the answer back through the door,
  and sends the next, for some seconds. Takes a door, the layer, the maker's
  permission, the seconds, and the number of the first act's place. Gives
  {:to-return :to-answer :other :seconds}: the times from the call of
  `send-act` to its return and to the moment at which the answer was read
  as accepted, in nanoseconds, of the accepted acts; how many acts were not
  read as accepted; and how long the sending took."
  [door layer permission seconds first-number]
  (let [began (System/nanoTime)
        ends (+ began (* 1000000000 (long seconds)))]
    (loop [i 0 to-return [] to-answer [] other 0]
      (if (<= ends (System/nanoTime))
        {:to-return to-return :to-answer to-answer :other other
         :seconds (/ (double (- (System/nanoTime) began)) 1.0e9)}
        (let [called (System/nanoTime)
              sent (send! door :maker layer permission [(another-place (+ first-number i))])
              returned (System/nanoTime)
              answer (when-not (:could-not sent)
                       (door/answer-of door (:act-id sent) (:digest sent) 60000))
              read (System/nanoTime)]
          (if (= :accepted (:answer answer))
            (recur (inc i) (conj to-return (- returned called)) (conj to-answer (- read called)) other)
            (recur (inc i) to-return to-answer (inc other))))))))

(defn- d3-most
  "D3, the most acts a second that one maker holds, in both readings, for
  one setting: the rates of `at-a-rate`, one after the other, each begun
  when every send of the one before has returned; and `one-by-one`. Takes a
  door, a streaming layer that is made and warm, the maker's permission,
  what the setting is, as text, and the load."
  [door layer permission at {:keys [rates rate-seconds one-by-one-seconds]}]
  (let [held (atom [])]
    (doseq [[r rate] (map-indexed vector rates)]
      (let [got (at-a-rate door layer permission rate rate-seconds (* 1000000 (inc r)))
            in-a-second? (and (:last-took got) (<= (:last-took got) 1000000000))]
        (when in-a-second? (swap! held conj rate))
        (measured "D3 the streaming gate, the setting" at "| one maker sends" rate "acts a second for" rate-seconds
                  "s with no wait for the one before, one fact of 100 bytes each to another place | from the act's due moment to the return of send-act:"
                  (times (:from-due got))
                  "| the last act answered within a second of its due moment:"
                  (if in-a-second? "yes" "no") (if (:last-took got) (str "(" (ms (:last-took got)) " ms)") "(it did not return with an act-id)")
                  "| its answer, read once:" (pr-str (:last got)))
        (measured "D3 the same," rate "acts a second, the setting" at "| from the call of send-act to its return:" (times (:from-call got))
                  "| the most sends due and not returned at one moment:" (:most-on-way got)
                  "| the sender holds" most-senders "in send-act at once, and a send that is due over that waits for a thread"
                  "| sends that came back with a word of the door:" (reduce + (vals (:words got)))
                  (if (seq (:words got))
                    (str "| how many with each word and class of what threw: " (pr-str (:words got)))
                    "")
                  "| every send returned within 300 s of the last due moment:" (if (:all-returned? got) "yes" "no"))))
    (measured "D3 the streaming gate, the setting" at "| the rates of this run at which the last act was answered within a second of its due moment:"
              (pr-str @held) "of" (pr-str rates) "acts a second")
    (let [got (one-by-one door layer permission one-by-one-seconds 9000000)
          n (count (:to-answer got))]
      (measured "D3 the streaming gate, the setting" at "| one maker sends each act when the one before is answered, for" one-by-one-seconds
                "s: it sends, reads the answer through the door, and sends the next |" n "acts in" (ms (* 1.0e9 (:seconds got)))
                "ms, which is" (/ (Math/round (* 10.0 (/ n (:seconds got)))) 10.0)
                "acts a second | from the call of send-act to the answer read as accepted:" (times (:to-answer got))
                "| acts not read as accepted:" (:other got))
      (measured "D3 the same, one by one, the setting" at "| from the call of send-act to its return:" (times (:to-return got))))))

(defn- d3-default
  "D3's half at Rama's default, in the first cluster: a streaming layer of
  its own, warmed with acts that are not counted, and then `d3-most`."
  [{:keys [door]} {:keys [warm] :as load}]
  (let [layer :d3
        own (make! door layer)]
    (dotimes [i warm]
      (send! door :maker layer own [(another-place (- i warm))]))
    (d3-most door layer own "at Rama's default" load)))

(defn- d3-at-one
  "D3's half at 1, in a cluster of its own with the module `StoreAtOne`:
  D1's first shape, and then `d3-most` in the same layer. Takes a door that
  is opened on `StoreAtOne`."
  [door {:keys [acts warm] :as load}]
  (set-up! door)
  (let [layer :d3
        own (make! door layer)
        plain (streaming-gate-times door layer own acts warm another-place)]
    (print-streaming-shape "D3 the setting at 1," "the key not marked" plain)
    (d3-most door layer own "at 1" load)))

;; ----------------------------------------------------------------------- the run

(defn- open-door
  "Opens a door on a module of a cluster, with the door's numbers when some
  are given. Gives the door. It stops the run when the door gives a word."
  [cluster module-name numbers]
  (let [door (door/open cluster module-name numbers)]
    (when (:could-not door)
      (throw (ex-info "the measures: the door did not open" {:word door})))
    door))

(defn -main
  "Runs the measures and exits. It starts a cluster, launches `Store`, says
  which tree it measures, runs D1, D2 and D3's half at Rama's default, and
  closes the cluster; then it starts a second cluster, launches `StoreAtOne`,
  runs D3's half at 1, and closes it. With the argument `small` each shape
  has a small load."
  [& args]
  (let [load-name (if (= "small" (first args)) :small :full)
        load (get loads load-name)
        launch {:tasks 4 :threads 2 :workers 2}]
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc module/Store launch)
      (let [m (rama/get-module-name module/Store)
            hold {:door (open-door ipc m nil)
                  :door-5ms (open-door ipc m {:read-every-ms 5})}]
        (said "the measures of the gates, with the load" (pr-str load-name) (pr-str load)
              "| no live read is open in any measure: the run opens doors and no read path"
              "| Rama's in-process cluster, 4 tasks, 2 threads, 2 workers, one machine, every row in memory, no network: a time is a floor for a round trip and says little of a disk or a network | JVM heap limit"
              (quot (.maxMemory (Runtime/getRuntime)) (* 1024 1024)) "MB")
        (said "the tree | git rev-parse --short HEAD:" (git-says "rev-parse" "--short" "HEAD")
              "| paths under src-store that differ from that commit:"
              (let [status (git-says "status" "--porcelain" "--" "src-store")]
                (cond (= "not told" status) status
                      (str/blank? status) 0
                      :else (count (str/split-lines status))))
              "| the module" m "| each gate keeps its answers in its own tables, and no gate appends an answer to a depot")
        (said "a time of the streaming gate is to the return of send-act, which is the streaming depot's ack | a time of the base is to the read of the door's wait that gave accepted: the answer is there when the act's batch is committed on the base's task, and nothing waits for a later batch")
        (said "D1's lines of the streaming gate stand beside the M1 lines of the streaming gate in reads/runs/measures-final.txt; those are of the tree of the read build, whose door took the locks of a send from a stock that it kept, so a request's round is not in their middle | D1's lines of the base stand beside no line of that file: its lines of the base are to an answer that the door's read gave only for a closed batch, and this store closes no batch")
        (set-up! (:door hold))
        (d1-streaming hold load)
        (d1-base (:door hold) 200 0 load)
        (d1-base (:door-5ms hold) 5 300000 load)
        (d2-again hold load)
        (d3-default hold load)))
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc StoreAtOne launch)
      (let [m (rama/get-module-name StoreAtOne)]
        (said "D3 at 1 | a second cluster of the same shape, with the module" m
              "| it is the module Store by its own definition, with" setting "set to 1 for the topology streaming-gate at the launch")
        (d3-at-one (open-door ipc m nil) load)))
    (said "not measured here: a real cluster, the base on one, a disk, a network, a second node, a gate with a live read open")
    (println (str/join " " ["the measures ran to their end;" "they prove nothing"]))
    (shutdown-agents)
    (System/exit 0)))
