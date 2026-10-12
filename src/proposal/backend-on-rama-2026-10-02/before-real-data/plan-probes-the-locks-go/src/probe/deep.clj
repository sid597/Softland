(ns probe.deep
  "What Rama's client writes and a topology reads of a thing that is very
  large or very deep, now that the door will test nothing of a value. Run
  last and alone: a thing that a worker cannot read may stop the probe's
  cluster, and then that is what the log says.

  The module is `probe.digests/DigestProbe`. Each case is appended to the
  depot placed by `:layer`, which a stream topology reads, and to the depot
  placed by the first entity, which a microbatch topology reads. For each:
  whether the append came back, in how many milliseconds, whether the
  topology's row came, and whether its digest is the client's. The run stops
  at the first case whose row does not come within 40 seconds.

  Run, from this folder:  flock ../runs/cluster.lock timeout 420 clojure -M:deep
  The log is logs/deep.log. The JVM has -Xss16m, on every thread; a worker
  of a real cluster has its own stack, and the depth that it reads was not
  run. Nothing here is a form for the store."
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [com.rpl.rama.test :as rtest]
            [probe.digests :as d]
            [probe.seen :as seen])
  (:import [java.util UUID]
           [java.util.concurrent CompletableFuture TimeUnit]))

(defonce ^:private out (atom nil))

(defn- say [& parts]
  (let [line (str/join " " parts)]
    (locking out
      (when-let [w @out] (.write ^java.io.Writer w (str line "\n")) (.flush ^java.io.Writer w))
      (println "PROBE" line))))

(defn- root-message [^Throwable t]
  (let [r (loop [t t] (if-let [c (.getCause t)] (recur c) t))
        m (str (.getName (class r)) ": " (.getMessage r))]
    (seen/shown (if (> (count m) 200) (subs m 0 200) m))))

(def ^:private who (UUID/fromString "00000000-0000-4000-8000-0000000000a1"))

(defn- thing
  "A thing as the door would append it, with the facts given."
  [depot-word facts]
  (let [layer :probe-deep]
    {:version 1 :who who :layer layer :permission [who layer layer] :session who :facts facts
     :act-id [layer depot-word (UUID/randomUUID)] :depot depot-word}))

(defn- fact [v] {:e (UUID/randomUUID) :k :probe/v :v v})

(defn- nested [x n] (loop [x x i 0] (if (= i n) x (recur [x] (inc i)))))

(defn- wait-for-row [pstate id ms]
  (let [end (+ (System/currentTimeMillis) ms)]
    (loop []
      (let [row (try (foreign-select-one (keypath id) pstate) (catch Throwable _ nil))]
        (cond row row
              (> (System/currentTimeMillis) end) nil
              :else (do (Thread/sleep 100) (recur)))))))

(defn- one!
  "One case through one depot. Gives true when the run may go on."
  [ctx label depot-word facts]
  (let [t (thing depot-word facts)
        id (seen/id-of t)
        door (try (seen/seen-of t) (catch Throwable e {:threw (root-message e)}))
        ack (if (= :microbatch depot-word) :append-ack :ack)
        t0 (System/nanoTime)
        sent (try (let [^CompletableFuture f (foreign-append-async! (get-in ctx [:depots depot-word]) t ack)]
                    (.get f 40 TimeUnit/SECONDS) :ok)
                  (catch Throwable e [:threw (root-message e)]))
        ms (/ (- (System/nanoTime) t0) 1e6)]
    (if (not= :ok sent)
      (do (say "CASE" label "|" depot-word "| NOT APPENDED after" (format "%.0f" ms) "ms:" (second sent)
               "| the client's own digest walk:" (if (:threw door) (str "threw " (:threw door)) "came back"))
          ;; an append that timed out may still be on its way: look once for a row
          (let [row (wait-for-row (get-in ctx [:rows depot-word]) id 3000)]
            (when row (say "     and yet a row came:" (pr-str (select-keys row [:digest :threw :text-length]))))
            true))
      (let [row (wait-for-row (get-in ctx [:rows depot-word]) id 40000)]
        (say "CASE" label "|" depot-word "| appended, the call took" (format "%.0f" ms) "ms"
             "| the topology's row:" (cond (nil? row) "DID NOT COME within 40 s"
                                           (:threw row) (str "the topology's walk threw: " (:threw row))
                                           :else (str "came · door digest = gate digest: " (= (:digest door) (:digest row))
                                                      " · text length " (:text-length row) " · the act's check: " (:parse row))))
        (some? row)))))

(defn -main [& _]
  (io/make-parents "logs/deep.log")
  (reset! out (io/writer "logs/deep.log"))
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc d/DigestProbe {:tasks 4 :threads 2})
      (let [m (get-module-name d/DigestProbe)
            ctx {:depots {:streaming (foreign-depot ipc m "*by-layer")
                          :microbatch (foreign-depot ipc m "*by-first-entity")}
                 :rows {:streaming (foreign-pstate ipc m "$$seen-by-stream")
                        :microbatch (foreign-pstate ipc m "$$seen-by-batch")}}
            big (fn [n] (apply str (repeat n "a")))
            value-65534 (big 65534)
            cases [["one value, a string of 1,000,000 characters" #(vector (fact (big 1000000)))]
                   ["256 facts, each a string whose text is 65,536 bytes (16.8 MB of values)"
                    #(mapv (fn [_] (fact value-65534)) (range 256))]
                   ["one value, a string of 20,000,000 characters" #(vector (fact (big 20000000)))]
                   ["100,000 facts with no value" #(mapv (fn [_] {:e (UUID/randomUUID) :k :probe/v}) (range 100000))]
                   ["one value, a 1 inside 1,000 vectors" #(vector (fact (nested 1 1000)))]
                   ["one value, a 1 inside 5,000 vectors" #(vector (fact (nested 1 5000)))]
                   ["one value, a 1 inside 20,000 vectors" #(vector (fact (nested 1 20000)))]
                   ["one value, a 1 inside 100,000 vectors" #(vector (fact (nested 1 100000)))]
                   ["one value, a 1 inside 1,000,000 vectors" #(vector (fact (nested 1 1000000)))]]]
        (say "RUN     Rama 1.6.0, in-process, 4 tasks, 2 threads · -Xss16m on every thread · Java" (System/getProperty "java.version"))
        (loop [cases cases]
          (when-let [[label facts] (first cases)]
            (let [fs (facts)
                  ok-s (one! ctx label :streaming fs)
                  ok-b (and ok-s (one! ctx label :microbatch fs))]
              (if (and ok-s ok-b)
                (recur (rest cases))
                (say "RUN     STOPPED: a row did not come. The cases after this one were not run.")))))
        (say "RUN     a last small thing, to see that both topologies still run:")
        (one! ctx "a small thing after all of the above" :streaming [(fact 1)])
        (one! ctx "a small thing after all of the above" :microbatch [(fact 1)])
        (say "RUN     END")))
    (catch Throwable t
      (say "RUN     THREW" (root-message t))))
  (shutdown-agents)
  (System/exit 0))
