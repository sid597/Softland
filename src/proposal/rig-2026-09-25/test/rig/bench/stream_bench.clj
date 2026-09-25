(ns rig.bench.stream-bench
  "Phase 7's two stream-store numbers, on the stream store as it stands and
  the in-process cluster, whose timings mean orders of magnitude only
  (the rama skill's testing.md, axiom 1).

  From the rig folder, each call its own JVM and its own cluster:

    clojure -M:bench rig.bench.stream-bench agent-rate <run-id>
    clojure -M:bench rig.bench.stream-bench one-thread <run-id>
    clojure -M:bench rig.bench.stream-bench summarize <log> ...
    clojure -M:bench rig.bench.stream-bench probe
    clojure -M:bench rig.bench.stream-bench diag

  `agent-rate` (M1): an agent layer made with the client and its owner
  granted; K concurrent offerers (K = 1, 4, 16) stream one-fact acts with a
  40-byte value into it without pause. Admitted acts per second, index
  writes per act and per second, ack latency.

  `one-thread` (M3): a personal layer, placed by layer, so all its offers
  are decided on one task. (a) One offerer, 2,000 sequential acked offers
  after warmup. (b) K concurrent offerers, K = 1, 2, 4, 8, 16, 32, then 64
  and 128 past the asked range.

  Every measured window checks placement: which depot partition grew and
  which task's `$$clock` moved (each decision stamps at least clock + 1).
  Each result is one line `RESULT <edn>` on stdout; `summarize` reads those
  lines from run logs and prints the median and spread per level.

  An offerer builds an act with `c/build` and sends it with `c/offer!` (an
  acked append; it returns once the gate's event is complete and its writes
  visible). Latency is the time around `c/offer!` only."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.envelope :as env]
            [rig.store.module :as m])
  (:import [java.lang.management GarbageCollectorMXBean ManagementFactory ThreadMXBean]
           [java.util ArrayList Arrays Collections]
           [java.util.concurrent Callable CountDownLatch ExecutorService Executors Future]))

;; ------------------------------------------------------------ configuration

(def tasks "Tasks, and threads: one thread per task." 4)
(def launch-config {:tasks tasks :threads tasks :workers 1})

(def value-chars
  "The value is a string of this many characters, so its canonical text
  (quoted, as the log row stores it) is 40 bytes."
  38)

(def entities "The entities acts are about, in rotation." (mapv #(keyword (str "e" %)) (range 1000)))

(def writes-per-act
  "The PState writes the gate makes for an admitted one-fact act with no
  replace, nothing stood on and no control fact (module.clj, gate.clj
  `decide*`): the answer record, the act's row vector (one row), the new
  fact's heads row, and the task's clock."
  [[:layers :answers] [:layers :log] [:layers :heads] [:clock]])

;; ------------------------------------------------------------------ output

(defn- emit! [tag m]
  (locking *out*
    (binding [*print-length* nil *print-level* nil]
      (println tag (pr-str m)))
    (flush)))

(defn- ms [ns] (when ns (/ (Math/round (/ (double ns) 1e3)) 1e3)))

;; ------------------------------------------------------------------- acts

(defn- value-for
  "A 38-character string, distinct per offerer and act."
  [offerer i]
  (subs (format "offerer %02d act %012d note-xxxxxxxxxx" offerer i) 0 value-chars))

(defn- fact-for [offerer i]
  {:e (nth entities (mod (+ i (* 7 offerer)) (count entities))) :k :note :v (value-for offerer i)})

(defn- act-for [{:keys [layer who pid]} offerer i]
  (let [f (fact-for offerer i)]
    [f (c/build {:who who :layer layer :class :by-layer :permission pid :facts [f]})]))

(defn- make-layer!
  "Make a one-owner layer and grant its owner, through the client; both acts
  must be admitted. The layer spec an offerer needs."
  [st layer kind owner]
  (let [made (c/offer-until-answered! st (c/make-layer-offer layer {:kind kind :owner owner}))
        pid [owner layer layer]
        granted (c/offer-until-answered! st (c/grant-offer st pid))]
    (when-not (and (= :yes (:answer made)) (= :yes (:answer granted)))
      (throw (ex-info "layer not made" {:made made :granted granted})))
    {:layer layer :who owner :pid pid :kind kind}))

;; -------------------------------------------------------------- snapshots

(defn- partition-ends
  "Each depot partition's end offset: how many records it holds."
  [st]
  (mapv #(:end-offset (foreign-depot-partition-info (:depot st) %)) (range tasks)))

(def ^:private task-keys
  "A key hashing to each task, so each task's `$$clock` can be read."
  (delay (vec (rtest/gen-hashing-index-keys tasks))))

(defn- task-clocks [st]
  (mapv #(foreign-select-one STAY (:clock st) {:pkey %}) @task-keys))

(defn- gc-totals []
  (reduce (fn [[n t] ^GarbageCollectorMXBean b]
            [(+ n (max 0 (.getCollectionCount b))) (+ t (max 0 (.getCollectionTime b)))])
          [0 0] (ManagementFactory/getGarbageCollectorMXBeans)))

(defn- thread-cpu
  "Thread id -> [name cpu-ns], for every live thread."
  []
  (let [^ThreadMXBean tb (ManagementFactory/getThreadMXBean)
        ids (.getAllThreadIds tb)]
    (into {} (for [id ids
                   :let [info (.getThreadInfo tb (long id))
                         cpu (.getThreadCpuTime tb (long id))]
                   :when (and info (pos? cpu))]
               [id [(.getThreadName info) cpu]]))))

(defn- busiest
  "The threads that used the most CPU between two `thread-cpu` snapshots, as
  [name percent-of-one-core]."
  [cpu0 cpu1 elapsed-ns n]
  (->> cpu1
       (map (fn [[id [nm c1]]] [nm (- c1 (second (get cpu0 id [nm 0])))]))
       (sort-by second >)
       (take n)
       (mapv (fn [[nm d]] [nm (/ (Math/round (* 1000.0 (/ (double d) elapsed-ns))) 10.0)]))))

;; ---------------------------------------------------------------- offerers

(defn- offer-loop
  "One offerer: build an act, send it acked, time the send; again until
  `stop?` says so. Latencies in ns, answers counted, and every 50th admitted
  act kept as [name entity stamp] for the post-check."
  [st lay offerer stop?]
  (let [lat (ArrayList. 65536)
        sample (ArrayList.)]
    (loop [i 0 yes 0 no 0 errs 0 reasons {} first-err nil]
      (if (stop? i)
        {:lat lat :n i :yes yes :no no :errors errs :reasons reasons
         :first-error first-err :sample (vec sample)}
        (let [[f o] (act-for lay offerer i)
              t0 (System/nanoTime)
              a (try (c/offer! st o) (catch Exception e e))
              t1 (System/nanoTime)]
          (.add lat (- t1 t0))
          (cond
            (instance? Exception a)
            (recur (inc i) yes no (inc errs) reasons (or first-err (str a)))

            (= :yes (:answer a))
            (do (when (zero? (mod i 50)) (.add sample [(:name o) (:e f) (:stamp a)]))
                (recur (inc i) (inc yes) no errs reasons first-err))

            :else
            (recur (inc i) yes (inc no) errs (update reasons (:reason a) (fnil inc 0)) first-err)))))))

(defn- percentiles [^longs sorted]
  (let [n (alength sorted)
        at (fn [q] (aget sorted (max 0 (dec (long (Math/ceil (* q n)))))))]
    (when (pos? n)
      {:p50 (ms (at 0.50)) :p95 (ms (at 0.95)) :p99 (ms (at 0.99)) :max (ms (aget sorted (dec n)))
       :mean (ms (/ (double (areduce sorted i s 0.0 (+ s (aget sorted i)))) n))})))

(defn- check-sample
  "For up to 200 sampled admitted acts: the answer record says yes with the
  act's stamp, the log holds its one row, and its fact heads its chain at
  that stamp. Every index the gate writes, read back."
  [st layer sample]
  (let [picked (take 200 (shuffle sample))
        ok? (fn [[nm e stamp]]
              (let [r (c/record st nm)
                    rows (c/facts st layer nm)]
                (and (= :yes (:answer r)) (= stamp (:stamp r))
                     (= 1 (count rows)) (= e (:e (first rows)))
                     (= stamp (c/head st layer e :note [nm 0])))))]
    {:checked (count picked) :ok (count (filter ok? picked))}))

(defn- window
  "K offerers at once on the layer, each until `secs` pass or it has sent
  `n` acts. Measured: the acts, answers, latencies, rate, where they were
  decided, GC and the busiest threads."
  [st lay k {:keys [secs n]}]
  (let [^ExecutorService pool (Executors/newFixedThreadPool k)
        go (CountDownLatch. 1)
        deadline (volatile! 0)
        stop? (if n (fn [i] (>= i n)) (fn [_] (>= (System/nanoTime) @deadline)))
        parts0 (partition-ends st)
        clocks0 (task-clocks st)
        gc0 (gc-totals)
        cpu0 (thread-cpu)
        futs (mapv (fn [id]
                     (.submit pool ^Callable (fn [] (.await go) (offer-loop st lay id stop?))))
                   (range k))
        wall-start (System/currentTimeMillis)
        t0 (System/nanoTime)
        _ (vreset! deadline (+ t0 (long (* 1e9 (or secs 0)))))
        _ (.countDown go)
        results (mapv #(.get ^Future %) futs)
        t1 (System/nanoTime)
        cpu1 (thread-cpu)
        gc1 (gc-totals)
        wall-end (System/currentTimeMillis)
        parts1 (partition-ends st)
        clocks1 (task-clocks st)
        _ (.shutdown pool)
        elapsed (- t1 t0)
        all (let [l (ArrayList.)] (doseq [r results] (.addAll l ^ArrayList (:lat r))) l)
        sorted (let [a (long-array (count all))]
                 (dotimes [i (count all)] (aset a i (long (.get all i))))
                 (Arrays/sort a)
                 a)
        offers (reduce + (map :n results))
        yes (reduce + (map :yes results))
        no (reduce + (map :no results))
        errors (reduce + (map :errors results))
        pd (mapv - parts1 parts0)
        cd (mapv - clocks1 clocks0)
        grew (vec (keep-indexed (fn [i d] (when (pos? d) i)) pd))
        moved (vec (keep-indexed (fn [i d] (when (pos? d) i)) cd))
        home (first grew)
        secs-elapsed (/ elapsed 1e9)]
    {:k k
     :secs (/ (Math/round (* 1000 secs-elapsed)) 1000.0)
     :offers offers :admitted yes :refused no :errors errors
     :reasons (apply merge-with + (map :reasons results))
     :first-error (some :first-error results)
     :offers-per-offerer [(reduce min (map :n results)) (reduce max (map :n results))]
     :acts-per-s (Math/round (/ yes secs-elapsed))
     :lat-ms (percentiles sorted)
     :placement {:partition-deltas pd :clock-deltas cd :home-task home
                 :one-task? (boolean (and home (= [home] grew) (= [home] moved)
                                          (= offers (get pd home))
                                          (>= (get cd home) (+ yes no))))}
     :stamp-ahead-of-wall-ms (when home (- (get clocks1 home) wall-end))
     :wall-ms [wall-start wall-end]
     :gc {:count (- (first gc1) (first gc0)) :ms (- (second gc1) (second gc0))}
     :busiest-threads (busiest cpu0 cpu1 elapsed 5)
     :sample (vec (mapcat :sample results))}))

(defn- report [w & {:as extra}]
  (-> w (dissoc :sample) (merge extra)))

;; ------------------------------------------------------------ measurements

(defmacro ^:private with-store
  "A fresh in-process cluster with the store launched, `st` its handles."
  [[st] & body]
  `(with-open [ipc# (rtest/create-ipc)]
     (rtest/launch-module! ipc# m/Store launch-config)
     (let [~st (c/connect ipc#)]
       ~@body)))

(defn- run-meta [measure run-id]
  {:measure measure :run run-id :launch launch-config
   :value-bytes (count (env/encode-value (value-for 0 0)))
   :jvm (str (System/getProperty "java.vm.name") " " (System/getProperty "java.runtime.version"))
   :max-heap-mb (quot (.maxMemory (Runtime/getRuntime)) (* 1024 1024))
   :processors (.availableProcessors (Runtime/getRuntime))})

(defn agent-rate
  "M1, one run: an agent layer, warmed with 4 offerers for 20 s, then for
  K = 1, 4, 16 a 3 s warm and a 15 s measured window."
  [& [run-id]]
  (let [info (run-meta :agent-rate run-id)]
    (emit! "META" info)
    (with-store [st]
      (let [lay (make-layer! st :bench-agent :agent :ada)]
        (emit! "WARMUP" (report (window st lay 4 {:secs 20})))
        (doseq [k [1 4 16]]
          (window st lay k {:secs 3})
          (let [w (window st lay k {:secs 15})]
            (emit! "RESULT" (report w :measure :agent-rate :part :stream :run run-id
                                    :writes-per-act (count writes-per-act)
                                    :writes writes-per-act
                                    :index-writes-per-s (* (count writes-per-act) (:acts-per-s w))
                                    :indexes-written (check-sample st (:layer lay) (:sample w))))))))))

(defn one-thread
  "M3, one run: a personal layer, warmed with 4 offerers for 20 s. (a) One
  offerer: 1,000 sequential acts to warm, then 2,000 measured. (b) K = 1,
  2, 4, 8, 16, 32 offerers, then 64 and 128 past the asked range: a 3 s warm
  and a 10 s measured window each."
  [& [run-id]]
  (let [info (run-meta :one-thread run-id)]
    (emit! "META" info)
    (with-store [st]
      (let [lay (make-layer! st :bench-person :personal :pat)]
        (emit! "WARMUP" (report (window st lay 4 {:secs 20})))
        (window st lay 1 {:n 1000})
        (let [w (window st lay 1 {:n 2000})]
          (emit! "RESULT" (report w :measure :one-thread :part :sequential :run run-id
                                  :indexes-written (check-sample st (:layer lay) (:sample w)))))
        ;; 64 and 128 go past the asked range, to find where the rate stops rising
        (doseq [k [1 2 4 8 16 32 64 128]]
          (window st lay k {:secs 3})
          (let [w (window st lay k {:secs 10})]
            (emit! "RESULT" (report w :measure :one-thread :part :concurrent :run run-id
                                    :indexes-written (check-sample st (:layer lay) (:sample w))))))))))

;; ------------------------------------------------------------------ diag

(defn diag
  "Not a measurement: a K = 1 and a K = 16 window of 5 s each after a 10 s
  warmup, with their wall-clock bounds, for a run under strace that counts
  the syncs to disk inside each window."
  [& _]
  (with-store [st]
    (let [lay (make-layer! st :bench-agent :agent :ada)]
      (window st lay 4 {:secs 10})
      (doseq [k [1 16]]
        (window st lay k {:secs 2})
        (emit! "DIAG" (report (window st lay k {:secs 5})))))))

;; ----------------------------------------------------------------- probe

(defn probe
  "Launch, make a layer, send a few acts, and print what the harness relies
  on: the value's size, the task keys, partition info, clocks, thread names
  and the busiest threads under a short load."
  [& _]
  (with-store [st]
    (let [lay (make-layer! st :bench-agent :agent :ada)
          [f o] (act-for lay 0 0)]
      (println "PROBE value" (pr-str (:v f)) "canonical bytes" (count (env/encode-value (:v f))))
      (println "PROBE answer" (c/offer! st o))
      (println "PROBE record" (c/record st (:name o)))
      (println "PROBE rows" (c/facts st :bench-agent (:name o)))
      (println "PROBE task-keys" (pr-str @task-keys))
      (println "PROBE object-info" (foreign-object-info (:depot st)))
      (println "PROBE partitions" (partition-ends st))
      (println "PROBE clocks" (task-clocks st) "home clock" (c/clock st :bench-agent)
               "wall" (System/currentTimeMillis))
      (println "PROBE thread names"
               (->> (vals (thread-cpu)) (map first) (map #(str/replace % #"\d+" "N")) frequencies (sort-by key)))
      (let [w (window st lay 4 {:secs 3})]
        (println "PROBE window" (pr-str (report w)))
        (println "PROBE sample check" (check-sample st :bench-agent (:sample w)))))))

;; -------------------------------------------------------------- summarize

(defn- median [xs] (let [s (vec (sort xs)) n (count s)] (when (pos? n) (s (quot n 2)))))

(defn- stat [xs] {:median (median xs) :min (reduce min xs) :max (reduce max xs) :runs (count xs)})

(defn summarize
  "Median and spread (min, max) across runs, per measurement, part and K,
  from the RESULT lines of the given logs."
  [& files]
  (let [rs (for [f files
                 line (str/split-lines (slurp f))
                 :when (str/starts-with? line "RESULT ")]
             (edn/read-string (subs line 7)))]
    (doseq [[[measure part k] g] (sort-by (fn [[[a b k] _]] [(str a) (str b) k]) (group-by (juxt :measure :part :k) rs))]
      (println (pr-str {:measure measure :part part :k k
                        :acts-per-s (stat (map :acts-per-s g))
                        :index-writes-per-s (when (:index-writes-per-s (first g)) (stat (map :index-writes-per-s g)))
                        :p50-ms (stat (map (comp :p50 :lat-ms) g))
                        :p95-ms (stat (map (comp :p95 :lat-ms) g))
                        :p99-ms (stat (map (comp :p99 :lat-ms) g))
                        :max-ms (stat (map (comp :max :lat-ms) g))
                        :offers (map :offers g)
                        :errors (reduce + (map :errors g))
                        :refused (reduce + (map :refused g))
                        :one-task (map (comp :one-task? :placement) g)
                        :home-task (map (comp :home-task :placement) g)
                        :indexes-written (map :indexes-written g)
                        :gc (map :gc g)
                        :busiest (map (comp first :busiest-threads) g)})))))
