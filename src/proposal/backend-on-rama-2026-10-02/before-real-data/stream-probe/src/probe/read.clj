(ns probe.read
  "The run for question 5: how a client reads a depot partition from its
  start, and what orders a record that a topology appended against the
  records of the source depot.

  Parts:

    R1  the calls: what a client can ask of a depot, and what an empty
        partition gives.
    R2  10,000 things are sent to task 1 (write, then append with
        :append-ack), 500 at once at a time. Then the second depot's
        partition is read from its start: in one read, and in reads of
        1000, 500 and 100; the source depot the same. The time of each.
    R3  the edges of a read: past the end, an empty range, a start before
        the partition's start.
    R4  40,000 more, then one read of 50,000, with a read of a row of the
        same task sent while it runs.
    R5  order. On three other tasks, 3000 things at once for each of three
        more ways of the append. For each task: the ids of the source
        partition in order against the ids of the second depot's partition
        in order, and the count that each record carries (the event reads
        the row \"count\", adds one, writes it, and puts it in the record).

  Run:  clojure -M:read   (from this folder; the log is logs/read.log)"
  (:use [com.rpl.rama]
        [com.rpl.rama.path])
  (:require [clojure.string :as str]
            [com.rpl.rama.test :as rtest]
            [probe.gate :as g]
            [probe.look :as look]
            [probe.util :as u])
  (:import [java.util.concurrent CompletableFuture TimeUnit]))

(defn- timed
  "Takes a function of no arguments. Gives [its value, the ms it took]; a throw gives [[:threw message] ms]."
  [f]
  (let [t (System/nanoTime)
        v (try (f) (catch Throwable e [:threw (u/root-message e)]))]
    [v (u/nanos->ms (- (System/nanoTime) t))]))

(defn- fill!
  "Sends n things of one way to the task of key k, `chunk` at once at a time, each with :ack. Takes the clients, the key, a word for the ids, the way, n and the chunk. Gives the ms it took."
  [ctx k word v n chunk]
  (let [t (System/nanoTime)]
    (doseq [part (partition-all chunk (range n))]
      (let [fs (mapv #(foreign-append-async! (:things ctx) (merge v {:k k :id (str word "-" %)}) :ack) part)]
        (doseq [^CompletableFuture f fs] (.get f 60 TimeUnit/SECONDS))))
    (u/nanos->ms (- (System/nanoTime) t))))

(defn- read-in
  "Reads a partition from `from` to `to` in reads of `step`. Gives the records as a vector."
  [depot p from to step]
  (loop [at from out []]
    (if (>= at to)
      out
      (let [upto (min to (+ at step))]
        (recur upto (into out (foreign-depot-read depot p at upto)))))))

(defn- say-reads!
  "Says the time of reading one partition from its start in reads of each step, three times each."
  [word depot p]
  (let [{:keys [start-offset end-offset]} (foreign-depot-partition-info depot p)]
    (doseq [step [end-offset 1000 500 100]]
      (let [runs (vec (repeatedly 3 #(timed (fn [] (count (read-in depot p start-offset end-offset step))))))]
        (u/say "READ   " word "partition" p "· offsets" start-offset "to" end-offset
               "· in reads of" step (str "(" (long (Math/ceil (/ (double (- end-offset start-offset)) step))) " calls)")
               "· records got:" (pr-str (mapv first runs))
               "· ms of three tries:" (pr-str (mapv #(Double/parseDouble (format "%.1f" (second %))) runs)))))))

(defn- order!
  "R5 for one task. Takes the clients, the task, the way, and how many things."
  [ctx task v n]
  (let [{:keys [things log ks]} ctx
        k (ks task)
        word (str "R5-" (g/variant-tag v))
        s0 (:end-offset (foreign-depot-partition-info things task))
        l0 (g/log-end log task)
        fs (mapv #(foreign-append-async! things (merge v {:k k :id (str word "-" %)}) :ack) (range n))
        failed (count (filter (fn [^CompletableFuture f] (try (.get f 60 TimeUnit/SECONDS) false (catch Throwable _ true))) fs))]
    (u/wait-until #(>= (- (g/log-end log task) l0) n) 10000)
    (let [src (mapv :id (read-in things task s0 (:end-offset (foreign-depot-partition-info things task)) 500))
          recs (read-in log task l0 (g/log-end log task) 500)
          logged (mapv :id recs)
          counts (mapv :count recs)
          same-place (count (filter true? (map = src logged)))
          pos (into {} (map-indexed (fn [i id] [id i]) src))
          moved (keep-indexed (fn [i id] (let [d (- i (long (pos id)))] (when-not (zero? d) d))) logged)
          count-of (into {} (map (juxt :id :count) recs))
          count-is-offset (count (filter (fn [[i id]] (= (count-of id) (+ s0 i 1))) (map-indexed vector src)))]
      (u/say "ORDER   task" task "·" (g/variant-name v) "·" n "things at once · appends that failed:" failed
             "· source records:" (count src) "· records in the second depot:" (count logged)
             "· every source record has one record:" (= (sort src) (sort logged)))
      (u/say "ORDER   task" task "· records at the same place in both partitions:" same-place "of" (count src)
             "· records at another place:" (count moved)
             (if (seq moved)
               (str "· moved by, in places: least " (apply min moved) " · most " (apply max moved)
                    " · the first twelve ids of the source: " (pr-str (mapv #(subs % (inc (count word))) (take 12 src)))
                    " · of the second depot: " (pr-str (mapv #(subs % (inc (count word))) (take 12 logged))))
               ""))
      (u/say "ORDER   task" task "· the counts carried by the records: distinct" (count (distinct counts)) "of" (count counts)
             "· rising along the second depot's partition:" (boolean (or (empty? counts) (apply < counts)))
             "· records whose count is the offset of their source record plus one:" count-is-offset "of" (count src)
             "· the row \"count\" now:" (pr-str (look/row ctx k "count"))))))

(defn -main [& _]
  (u/open-log! "logs/read.log")
  (try
    (with-open [ipc (rtest/create-ipc)]
      (rtest/launch-module! ipc g/GateProbe {:tasks 4 :threads 4})
      (let [ctx (look/open ipc)
            {:keys [things log ks]} ctx
            k (ks 1)
            plain {:place :after :boundary false :ack :append-ack}]
        (u/say "RUN     Rama 1.6.0, in-process, 4 tasks, 4 threads")

        (u/say "RUN     ---- R1: the calls ----")
        (u/say "CALLS   the class of a depot client:" (.getName (class log))
               "· the methods that the interface com.rpl.rama.Depot declares:"
               (pr-str (sort (distinct (map #(.getName ^java.lang.reflect.Method %) (.getMethods com.rpl.rama.Depot))))))
        (u/say "CALLS   (foreign-object-info log) =>" (pr-str (foreign-object-info log)))
        (u/say "CALLS   (foreign-depot-partition-info log 1), nothing appended yet =>" (pr-str (foreign-depot-partition-info log 1)))
        (u/say "CALLS   (foreign-depot-read log 1 0 0), nothing appended yet =>" (pr-str (first (timed #(foreign-depot-read log 1 0 0)))))
        (u/say "CALLS   (foreign-depot-partition-info log 4), a partition that is not there =>" (pr-str (first (timed #(foreign-depot-partition-info log 4)))))

        (u/say "RUN     ---- R2: 10,000 things, then the reads ----")
        (u/say "FILL    10,000 things sent to task 1, 500 at once at a time, each with :ack: done in" (look/fmt (fill! ctx k "R2" plain 10000 500)) "ms")
        (let [one (foreign-depot-read log 1 0 3)]
          (u/say "READ    (foreign-depot-read log 1 0 3) =>" (pr-str one) "· the class of the result:" (.getName (class one))
                 "· of one record:" (.getName (class (first one)))))
        (u/say "READ    (foreign-depot-read things 1 0 1) =>" (pr-str (foreign-depot-read things 1 0 1)))
        (let [ts (vec (repeatedly 200 #(second (timed (fn [] (foreign-depot-partition-info log 1))))))]
          (u/say "READ    (foreign-depot-partition-info log 1) =>" (pr-str (foreign-depot-partition-info log 1)) "· the call, 200 times:" (u/stats ts)))
        (say-reads! "the second depot," log 1)
        (say-reads! "the source depot," things 1)

        (u/say "RUN     ---- R3: the edges of a read ----")
        (let [end (g/log-end log 1)]
          (doseq [[what from to] [["five before the end to five past it" (- end 5) (+ end 5)]
                                  ["the end to the end" end end]
                                  ["the end to one past it" end (inc end)]
                                  ["an end before the start" 10 5]
                                  ["a start below zero" -1 2]]]
            (let [[v ms] (timed #(foreign-depot-read log 1 from to))]
              (u/say "EDGE    (foreign-depot-read log 1" from (str to ")") "," what "=>"
                     (if (and (vector? v) (= :threw (first v))) (str "threw " (second v)) (str (count v) " records"))
                     "after" (look/fmt ms) "ms"))))

        (u/say "RUN     ---- R4: one read of 50,000 ----")
        (u/say "FILL    40,000 more things: done in" (look/fmt (fill! ctx k "R4" plain 40000 500)) "ms")
        (let [end (g/log-end log 1)]
          (dotimes [i 2]
            (let [big (future (timed #(count (foreign-depot-read log 1 0 end))))
                  _ (Thread/sleep 5)
                  [row row-ms] (timed #(look/row ctx k "R2-0"))
                  [n big-ms] @big]
              (u/say "READ    one read of the second depot's partition 1 from 0 to" end "=>" (pr-str n) "records in" (look/fmt big-ms) "ms"
                     "· a read of a row of the same task, sent 5 ms after it, was answered after" (look/fmt row-ms) "ms")))
          (let [ts (vec (repeatedly 50 #(second (timed (fn [] (look/row ctx k "R2-0"))))))]
            (u/say "READ    the same read of a row with nothing else going on, 50 times:" (u/stats ts))))

        (u/say "RUN     ---- R5: order ----")
        (order! ctx 2 {:place :after :boundary true :ack :append-ack} 3000)
        (order! ctx 3 {:place :after :boundary false :ack nil} 3000)
        (order! ctx 0 {:place :before :boundary false :ack :append-ack} 3000)
        (let [src (mapv :id (read-in things 1 0 (:end-offset (foreign-depot-partition-info things 1)) 1000))
              recs (read-in log 1 0 (g/log-end log 1) 1000)]
          (u/say "ORDER   task 1 · write, then append · no boundary · :append-ack · the 50,000 of R2 and R4, 500 at once at a time"
                 "· the ids of the two partitions are in the same order:" (= src (mapv :id recs))
                 "· every count is the offset of its source record plus one:" (= (mapv :count recs) (vec (range 1 (inc (count recs)))))))
        (u/say "ORDER   which source partition the records of each partition of the second depot came from, by the id without its number:"
               (pr-str (into (sorted-map)
                             (for [p (range 4)]
                               [p (frequencies (map #(let [^String id (:id %)] (subs id 0 (.lastIndexOf id "-"))) (read-in log p 0 (g/log-end log p) 1000)))]))))
        (u/say "RUN     events begun in all:" (reduce + (vals @g/attempts)) "for" (count @g/attempts) "source records"
               "· task globals made ready again:" (g/restarts))
        (u/say "RUN     END")))
    (catch Throwable t
      (u/say "RUN     THREW" (u/root-message t))
      (.printStackTrace t)))
  (shutdown-agents)
  (System/exit 0))
