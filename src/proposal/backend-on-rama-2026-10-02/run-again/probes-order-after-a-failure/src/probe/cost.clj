(ns probe.cost
  "What the stand-in gate costs a maker, with a setting on or off. Run on a
  fresh in-process cluster:

    `clojure -M:cost [key value ...]`
       :timeout     seconds for `topology.stream.timeout.seconds` (left out: Rama's own value)
       :retry-mode  :all-after (the default), :individual, :none
       :log         the name of the log file under logs/ (default: cost)

  One layer, so one task. Four makers send acts of that layer with :ack, by
  the client's call that does not wait, at a set rate for ten seconds: 100 a
  second, then 200, 400, 800 and 1,600. Every act has a key of its own and
  says `:replaces nil`, so every act is accepted and writes three rows. For
  each rate the run says how many appends were answered, what the others
  gave, and how long the :ack calls took.

  No failure is put in. The JVM keeps no notes of the attempts here
  (`probe.gate/noting?` is false).

  A probe, not a design."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.java.io :as io]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.order :as o])
  (:import [java.util.concurrent CompletableFuture ConcurrentLinkedQueue]
           [java.util.concurrent.atomic AtomicLong]
           [java.util.concurrent.locks LockSupport]))

(def rates
  "The rates of the run, acts a second."
  [100 200 400 800 1600])

(def seconds
  "How long the makers send at each rate."
  10)

(defn- at-rate!
  "Takes the depot client, a layer and a rate. Four makers send acts of the
  layer at that rate for `seconds`. Waits for every call, at most 60 s more.
  Gives {:sent :took (ms of each answered call) :gave (what the calls gave, counted)}."
  [depot layer rate]
  (let [took (ConcurrentLinkedQueue.)
        gave (atom {})
        sent (AtomicLong.)
        back (AtomicLong.)
        gap (long (/ 4e9 rate))
        t0 (System/nanoTime)
        makers (mapv (fn [m]
                       (future
                         (loop [i 0]
                           (when (< (- (System/nanoTime) t0) (* seconds 1e9))
                             (let [id (str "c-" rate "-" m "-" i)
                                   a {:layer layer :id id :key id :val "v" :replaces nil :time (System/currentTimeMillis)}
                                   called (System/nanoTime)]
                               (.incrementAndGet sent)
                               (try
                                 (.whenComplete ^CompletableFuture (foreign-append-async! depot a :ack)
                                                (reify java.util.function.BiConsumer
                                                  (accept [_ v t]
                                                    (if t
                                                      (swap! gave update (o/root-message t) (fnil inc 0))
                                                      (do (.add took (/ (double (- (System/nanoTime) called)) 1e6))
                                                          (swap! gave update "answered" (fnil inc 0))))
                                                    (.incrementAndGet back))))
                                 (catch Throwable t
                                   (swap! gave update (str "the call itself threw " (o/root-message t)) (fnil inc 0))
                                   (.incrementAndGet back))))
                             (LockSupport/parkNanos (max 0 (- (+ t0 (* (inc i) gap)) (System/nanoTime))))
                             (recur (inc i))))))
                     (range 4))]
    (run! deref makers)
    (let [sending-ms (/ (double (- (System/nanoTime) t0)) 1e6)]
      (o/wait-until #(= (.get back) (.get sent)) 60000)
      {:sent (.get sent) :back (.get back) :sending-ms sending-ms
       :took (vec (sort (.toArray took))) :gave @gave})))

(defn -main [& args]
  (let [opts (into {} (map (fn [[k v]] [(read-string k) (read-string v)]) (partition 2 args)))
        log (str "logs/" (:log opts "cost") ".log")]
    (io/make-parents log)
    (reset! o/log-writer (io/writer log))
    (swap! g/settings merge (select-keys opts [:timeout :retry-mode]))
    (g/reset-state!)
    (reset! g/noting? false)
    (o/say "COST    Rama 1.6.0, in-process, 4 tasks, 4 threads · one layer · every act accepted · settings of First:" (pr-str @g/settings))
    (try
      (with-open [ipc (rtest/create-ipc)]
        (rtest/launch-module! ipc g/First {:tasks 4 :threads 4})
        (let [depot (foreign-depot ipc (get-module-name g/First) "*acts")]
          ;; the JVM's first acts are slow; they are not counted
          (dotimes [i 300]
            (foreign-append! depot {:layer "L00" :id (str "warm-" i) :key (str "warm-" i) :val "v" :replaces nil
                                    :time (System/currentTimeMillis)} :ack))
          (doseq [rate rates]
            (let [{:keys [sent back sending-ms took gave]} (at-rate! depot "L00" rate)
                  n (count took)
                  at (fn [q] (if (zero? n) Double/NaN (double (took (min (dec n) (int (* q n)))))))]
              (o/say (format "COST    asked %4d a second · sent %5d in %.1f s (%.0f a second) · answered %5d · calls that came back %5d · middle %.2f ms · 9 of 10 under %.2f ms · 99 of 100 under %.2f ms · most %.1f ms · what the calls gave: %s"
                             rate sent (/ sending-ms 1000) (/ sent (/ sending-ms 1000)) n back
                             (at 0.5) (at 0.9) (at 0.99) (at 1.0) (pr-str gave)))
              (Thread/sleep 3000)))))
      (o/say "COST    END")
      (catch Throwable t
        (o/say "COST    DID NOT REACH ITS END:" (o/root-message t))
        (.printStackTrace t)))
    (when-let [^java.io.Writer w @o/log-writer] (.close w))
    (shutdown-agents)
    (System/exit 0)))
