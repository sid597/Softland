(ns rig.bench.agent-rate
  "Number 1 (PLAN-numbers.md 4): index writes a second, an agent session
  layer writing small acts continuously, on the finished store, through
  its own road: offers built and sealed by the door, locks leased from the
  gate, the gate's one decision event, and the read exit where reads are
  part of the workload.

  From the rig folder, each call its own JVM and its own cluster, under
  the cluster lock (8.1):

    clojure -M:bench rig.bench.agent-rate run <n>        A, A', D2, and D1 when its condition holds
    clojure -M:bench rig.bench.agent-rate sessions <n>   B: S sessions on one task, each at an assumed 100 acts a second
    clojure -M:bench rig.bench.agent-rate reads <n>      C: the agent reads before it writes, and the entry bytes

  The act (2.2): one fact `{:e <one of 1,000 entities in rotation> :k :note
  :v <38 characters>}`, 40 bytes of canonical EDN, the slices' act, written
  by `:ada` in the session `:bench-s1` opened beneath her root permission,
  citing the session's permission (B7). `:note` has no grammar in the layer,
  so the act writes 9 (2.3; F1's first case, BUILD_NOTES-numbers.md B9).

  Every RESULT line goes to `runs/phase7-final-agent-rate.edn` (8.5)."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.bench.numbers :as nb]
            [rig.store.client :as c]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as rr]
            [rpl.rama.api.durable.rocksdb.key-encoding :as rkey]
            [rpl.rama.util.nippy-serialization :as rser])
  (:import [java.nio ByteBuffer]
           [java.util Random]))

(def number :agent-rate)
(def tasks (:tasks nb/timing-launch))

(defn- t
  "A window's length: the plan's, or a short one in a small run."
  [full small]
  (if nb/smoke? small full))

;; ===================================================================== acts

(def value-chars
  "The value is a string of this many characters, so its canonical text is
  40 bytes (the slices' `value-for`)."
  38)

(def entities "The entities acts are about, in rotation." (mapv #(keyword (str "e" %)) (range 1000)))

(defn value-for
  "A 38-character string, distinct per writer and act (the slices')."
  [w i]
  (subs (format "offerer %02d act %012d note-xxxxxxxxxx" w i) 0 value-chars))

(defn act-for
  "Writer `w`'s act `i` into the layer spec `lay`: one fact under `key`,
  built by the door (`c/build`: a fresh name, the offerer's claimed time),
  citing the layer's permission or `permission` (D2), standing on
  `stood-on` (C). [fact offer]; the door seals the value as it sends."
  ([lay key w i] (act-for lay key w i nil))
  ([{:keys [layer who session pid]} key w i {:keys [permission stood-on]}]
   (let [f {:e (nth entities (mod (+ i (* 7 w)) (count entities))) :k key :v (value-for w i)}]
     [f (c/build {:who who :session session :permission (or permission pid) :layer layer :class :by-layer
                  :stood-on stood-on :facts [f]})])))

(defn value-step
  "A closed-loop writer's iteration (4.3): build an act, send it with
  `c/offer!` (the door seals it, then an acked append that returns once the
  gate's event is complete and its writes are visible), time the send only,
  and keep one admitted act in 50 for the read-back."
  ([st lay key act-kind] (value-step st lay key act-kind nil))
  ([st lay key act-kind opts]
   (fn [w i stats]
     (let [[f o] (act-for lay key w i opts)
           t0 (System/nanoTime)
           a (try (c/offer! st o) (catch Exception e e))
           t1 (System/nanoTime)]
       (when (and (= :yes (nb/record! stats :value a (- t1 t0)))
                  (zero? (mod (nb/yes-count stats :value) 50)))
         (nb/sample! stats {:layer (:layer lay) :session (:session lay) :name (:name o) :kind act-kind
                            :stamp (:stamp a) :e (:e f) :k (:k f) :v (:v f)}))))))

(defn index-writes
  "2.5: the sum over act kinds of (admitted acts of that kind) × (its writes
  in 2.3), over the window's seconds; the read indexes and the deletes
  beside. The counts come from the run, the per-act totals from the list T1
  holds to the code. `leases` defaults to the doors' count."
  [w value-kind & {:keys [lease-n leases secs] :or {lease-n c/lease-size}}]
  (let [secs (double (or secs (:secs w)))
        nv (get-in w [:admitted :value] 0)
        ne (get-in w [:admitted :entry] 0)
        nl (or leases (get-in w [:leases :door]) 0)
        pv (nb/per-act-writes value-kind)
        pl (nb/per-act-writes :lease lease-n)
        pe (nb/per-act-writes :entry)
        per-s (fn [k] (Math/round (/ (+ (* nv (k pv)) (* nl (k pl)) (* ne (k pe))) secs)))]
    {:per-act {:value pv :lease pl :entry pe}
     :acts {:value nv :lease nl :entry ne}
     :per-s {:all (per-s :total) :read-indexes (per-s :read-indexes) :deletes (per-s :deletes)}}))

(defn- emit-result!
  "A measured window as a RESULT line, with its read-back and its index
  writes."
  [ctx w extra]
  (let [r (merge (nb/result w) {:number number :run (:run ctx) :uid (:uid ctx)} extra)]
    (nb/emit! "RESULT" r number)
    r))

(defn- window!
  "A closed window of `k` writers stepping `step` on `lay` for `secs`."
  [ctx st lay variant k secs step]
  (nb/closed-window {:ctx ctx :st st :tasks tasks :home (:home lay)
                     :lease-doors [[st (:layer lay) (:session lay)]]
                     :variant variant :k k :secs secs :step step}))

(defn measure!
  "3 s unmeasured at K, then a measured window at K, recorded with its
  read-back (4.3) and its index writes (2.5)."
  [ctx st lay variant k step act-kind extra]
  (let [warm (window! ctx st lay variant k (t 3 1) step)]
    (nb/emit! "UNMEASURED" (select-keys warm [:variant :k :value-acts-per-s :errors]))
    (let [w (window! ctx st lay variant k (t 15 2) step)]
      (emit-result! ctx w (merge {:act-kind act-kind :layer (:layer lay)
                                  :check (nb/check-sample st (:sample w))
                                  :index-writes (index-writes w act-kind)}
                                 extra)))))

;; ======================================================== A, A', D2 and D1

(defn- p99 [r] (get-in r [:lat :value :p99]))

(defn- flattened?
  "D1's condition (4.7): A's rate gained under 10% from K = 64 to K = 128
  while the home task's thread stayed under 60% of a core."
  [by-k]
  (let [r64 (get by-k 64) r128 (get by-k 128)]
    (boolean (and r64 r128
                  (< (:value-acts-per-s r128) (* 1.1 (:value-acts-per-s r64)))
                  (< (or (second (get-in r128 [:cpu :store-busiest])) 100.0) 60.0)))))

(defn stock-ahead!
  "D1's stocking (F4): `c/lease!` of 256 until the session's leases hold
  `target` locks, then one `c/stock!`, whose single `lease-locks` call
  takes every unconsumed lock of the session into the pool. Never
  `c/stock!` each time: its refresh reads and unleases every standing lease
  row of the session on the home task, n² over n stockings. Reported apart:
  the lease acts, the locks, the pool, the time."
  [st lay target]
  (let [t0 (System/nanoTime)
        {:keys [who layer session pid]} lay]
    (loop [locks 0 acts 0]
      (if (< locks target)
        (do (nb/yes! "a stocking lease" (c/lease! st who layer session 256 pid))
            (recur (+ locks 256) (inc acts)))
        (do (nb/yes! "the stocking's last lease" (c/stock! st who layer session 256 pid))
            {:lease-acts (inc acts) :locks (+ locks 256)
             :pool (count (get-in @(:door st) [:pool [layer session]]))
             :secs (nb/secs-since t0)})))))

(defn run
  "One run of number 1's variants A, A', D2 and D1 (4.3, 4.4, 4.7):
  setup (4.2), a 10 s idle window, a 20 s warm-up at 4 writers, A at K = 1,
  4, 16, 32, 64, 128 (then 256 and 512 while the rate still gains 10%, p99
  stays at most 200 ms and nothing errs), A' at K = 1, 4, 16 on a layer
  whose `:note` grammar indexes by value, D2 at K = 16 citing the owner's
  root permission, and D1 when A's rate has flattened."
  [& [run-id]]
  (nb/with-run
    {:number number :run run-id :fn-name "run" :args [run-id] :launch nb/timing-launch}
    (fn [{:keys [ipc] :as ctx}]
      (let [st (c/connect ipc)
            mst (mc/connect ipc)
            _ (nb/people! st)
            _ (nb/person! st :ada)
            key nb/primary-key
            lay (nb/layer! st mst {:layer :bench-agent :kind :agent :owner :ada :session :bench-s1 :tasks tasks})
            lay-v (nb/layer! st mst {:layer :bench-agent-v :kind :agent :owner :ada :session :bench-s1 :tasks tasks
                                     :grammars {key nb/note-by-value}})
            step (value-step st lay key :agent-value)
            _ (nb/emit! "RESULT" (merge (nb/idle-window (t 10 2))
                                        {:number number :run run-id :uid (:uid ctx)
                                         :layers {:bench-agent (:home lay) :bench-agent-v (:home lay-v)}})
                        number)
            _ (nb/emit! "WARMUP" (nb/result (window! ctx st lay :warm-up 4 (t 20 3) step)))
            a-levels (loop [ks (if nb/smoke? [1 4 16] [1 4 16 32 64 128])
                            more (if nb/smoke? [] [256 512])
                            by-k (sorted-map)]
                       (cond
                         (seq ks)
                         (let [k (first ks)]
                           (recur (rest ks) more (assoc by-k k (measure! ctx st lay :A k step :agent-value {}))))

                         (seq more)
                         (let [prev (val (last by-k))
                               before (val (last (butlast by-k)))
                               go? (and (>= (:value-acts-per-s prev) (* 1.1 (:value-acts-per-s before)))
                                        (<= (or (p99 prev) Double/MAX_VALUE) 200.0)
                                        (zero? (:errors prev)))]
                           (if go?
                             (let [k (first more)]
                               (recur ks (rest more) (assoc by-k k (measure! ctx st lay :A k step :agent-value {}))))
                             by-k))

                         :else by-k))
            step-v (value-step st lay-v key :agent-value-by-value)]
        (doseq [k (if nb/smoke? [1 4] [1 4 16])]
          (measure! ctx st lay-v :A-prime k step-v :agent-value-by-value {}))
        ;; D2: the same act citing the owner's root permission, a chain of 1
        (measure! ctx st lay :D2 16 (value-step st lay key :agent-value {:permission (:root-pid lay)}) :agent-value
                  {:permission (:root-pid lay)})
        ;; D1: the gate's rate when no writer waits on a lease
        (when (or nb/smoke? (flattened? a-levels))
          (let [r128 (get a-levels 128 (val (last a-levels)))
                target (if nb/smoke? 1024 (long (* 2 (:value-acts-per-s r128) (+ 3 15))))
                stocking (stock-ahead! st lay target)
                _ (nb/emit! "STOCKED" stocking)
                k (if nb/smoke? 16 128)
                warm (window! ctx st lay :D1 k (t 3 1) step)
                w (window! ctx st lay :D1 k (t 15 2) step)]
            (nb/emit! "UNMEASURED" (select-keys warm [:variant :k :value-acts-per-s :errors :leases]))
            (emit-result! ctx w {:act-kind :agent-value :layer (:layer lay)
                                 :check (nb/check-sample st (:sample w))
                                 :index-writes (index-writes w :agent-value)
                                 :stocking stocking
                                 :d1-valid? (and (zero? (or (get-in warm [:leases :door]) 0))
                                                 (zero? (or (get-in w [:leases :door]) 0)))
                                 :d1-mark (when-not (zero? (or (get-in w [:leases :door]) 0)) :the-pool-ran-dry)})))
        :done))))

;; ====================================================== B: sessions, open loop

(def agent-period-ns
  "An agent at full speed writes about 100 acts a second (README.md,
  assumed, 4.5): a slot every 10 ms."
  10000000)

(defn- session-lanes
  "B's lanes, one per session: its one writer on its own schedule, a slot
  every 10 ms from a random phase, sending through the session's own door
  (its own `c/connect`), so no session waits on another's lease, as no real
  agent process would."
  [lays key ^Random rnd]
  (vec (for [[j lay] (map-indexed vector lays)]
         {:threads 1 :period-ns agent-period-ns :kind :value
          :phase-ns (long (* agent-period-ns (.nextDouble rnd)))
          :send (fn [n]
                  (let [[f o] (act-for lay key j n)
                        a (c/offer! (:st lay) o)]
                    (cond-> a
                      (= :yes (:answer a))
                      (assoc :sample-item {:layer (:layer lay) :session (:session lay) :name (:name o)
                                           :kind :agent-value :stamp (:stamp a) :e (:e f) :k (:k f) :v (:v f)}))))})))

(defn sessions
  "One run of variant B (4.5): the divisor, measured. S agent session layers
  all homed on one task (placement is by layer, so candidates are made one
  at a time and kept when their making grew the target partition), each
  with its own session and its own door, each writing on an open schedule
  at an assumed 100 acts a second, latency from the schedule. After a 10 s
  idle window and a 20 s warm-up at S = 10: S = 10, 20, 40, 80, 160, each 3 s
  unmeasured then 15 s measured, stopping after the first level where fewer
  than 90% of the offered acts were admitted, p99 passed 100 ms, or anything
  erred. The result: the largest S with at least 95% admitted and p99 at
  most 20 ms (number 3's line, reused, assumed)."
  [& [run-id]]
  (nb/with-run
    {:number number :run run-id :fn-name "sessions" :args [run-id] :launch nb/timing-launch}
    (fn [{:keys [ipc] :as ctx}]
      (let [st (c/connect ipc)
            mst (mc/connect ipc)
            _ (nb/people! st)
            _ (nb/person! st :ada)
            key nb/primary-key
            levels (if nb/smoke? [2 4] [10 20 40 80 160])
            target 0
            t0 (System/nanoTime)
            ids (nb/layers-on-task! st {:prefix "bench-a" :owner :ada :n (last levels) :target target :tasks tasks})
            lays (vec (for [[j L] (map-indexed vector ids)]
                        (let [root [:ada L L]
                              S (keyword (str "bench-s" j))]
                          (nb/yes! (str "granting " root) (c/offer-until-answered! st (c/grant-offer st root)))
                          (nb/yes! (str "opening " S " in " L) (get (mc/open-session! mst S :ada [L]) L))
                          {:layer L :kind :agent :owner :ada :who :ada :root-pid root
                           :session S :pid [S L L root] :home target :st (c/connect ipc)})))
            setup {:layers (count lays) :target-task target :secs (nb/secs-since t0)
                   :candidates (inc (Long/parseLong (subs (name (peek ids)) (count "bench-a"))))}
            rnd (Random.)
            window (fn [variant n warm secs]
                     (let [ls (subvec lays 0 n)]
                       (nb/open-window {:ctx ctx :st st :tasks tasks :home target
                                        :lease-doors (mapv (fn [l] [(:st l) (:layer l) (:session l)]) ls)
                                        :variant variant :k n :lanes (session-lanes ls key rnd)
                                        :warm warm :secs secs})))]
        (nb/emit! "SETUP" setup)
        (nb/emit! "RESULT" (merge (nb/idle-window (t 10 2)) {:number number :run run-id :uid (:uid ctx) :variant :idle-B})
                  number)
        (nb/emit! "WARMUP" (nb/result (window :warm-up (first levels) 0 (t 20 3))))
        (let [results (loop [ls levels acc []]
                        (if-let [n (first ls)]
                          (let [w (window :B n (t 3 1) (t 15 2))
                                whole (get-in w [:whole-window :admitted])
                                door (or (get-in w [:leases :door]) 0)
                                ;; the doors' leases cover the whole window; the measured part's share, derived
                                leases-measured (if (pos? whole)
                                                  (Math/round (* (double door) (/ (get-in w [:admitted :value]) whole)))
                                                  0)
                                r (emit-result! ctx w {:act-kind :agent-value :sessions n :setup setup
                                                       :check (nb/check-sample st (:sample w))
                                                       :lease-acts-per-s (nb/rate door (long (* 1e9 (:whole-secs w))))
                                                       :index-writes (assoc (index-writes w :agent-value :leases leases-measured
                                                                                          :secs (:secs w))
                                                                            :leases-in-the-measured-part :derived)})
                                p99 (get-in r [:lat :value :p99])
                                stop? (or (< (or (:admitted-share r) 0) 0.9)
                                          (> (or p99 Double/MAX_VALUE) 100.0)
                                          (pos? (:errors r)))]
                            (if stop? (conj acc r) (recur (rest ls) (conj acc r))))
                          acc))
              good (filter #(and (>= (or (:admitted-share %) 0) 0.95)
                                 (<= (or (get-in % [:lat :value :p99]) Double/MAX_VALUE) 20.0))
                           results)]
          (nb/emit! "RESULT" {:number number :run run-id :uid (:uid ctx) :variant :B-summary
                              :largest-s (when (seq good) (apply max (map :sessions good)))
                              :agent-acts-per-s 100 :agent-speed :assumed
                              :line "at least 95% admitted and p99 at most 20 ms (number 3's line, reused, assumed)"
                              :levels (mapv #(select-keys % [:sessions :offered-per-s :value-acts-per-s :admitted-share
                                                             :errors :lat])
                                            results)}
                    number))
        :done))))

;; ================================================ C: the agent reads, then writes

(defn read-spec
  "4.6's read with the landed exit (F3's second case; BUILD_NOTES-numbers.md
  B10): the agent reads as `:ada`, a model, for `:ada`, in its session,
  citing the session's permission; the layer read is the working layer,
  where the entry lands."
  [lay read role]
  {:reader (:who lay) :reader-kind :model :for (:who lay) :session (:session lay) :permission (:pid lay)
   :working (:layer lay) :layer (:layer lay) :read read :role role})

(defn read-write-step
  "Variant C's iteration (4.6): a point read, through the exit, of the fact
  this writer admitted last (its first reads one written at setup),
  recorded as an entry in the session layer; then a value act standing on
  that fact at its stamp, as an agent's act stands on what it read. `:read`
  times `read!` (its query and its entry's acked offer), `:value` the
  write; `:entry` counts the entry acts."
  [st lay key seeds]
  (let [lasts (vec (repeatedly (count seeds) #(volatile! nil)))]
    (fn [w i stats]
      (let [{:keys [fid stamp]} (or @(nth lasts w) (nth seeds w))
            t0 (System/nanoTime)
            r (try (rx/read! st (read-spec lay [:point [fid]] :stood-on)) (catch Exception e e))
            t1 (System/nanoTime)
            ok? (and (map? r) (some? (:entry r)) (not (contains? r :refused)))
            outcome (cond (instance? Throwable r) r
                          ok? {:answer :yes}
                          :else {:answer :no :reason (:refused r)})]
        (nb/record! stats :read outcome (- t1 t0))
        (when (and (map? r) (:entry r))
          (nb/record! stats :entry outcome (- t1 t0)))
        (when (and ok? (zero? (mod (nb/yes-count stats :entry) 50)))
          (nb/sample! stats {:layer (:layer lay) :session (:session lay) :name (:entry r) :kind :entry
                             :stamp (:entry-stamp r)}))
        (when ok?
          (let [so {fid (or (:stamp (first (:rows r))) stamp)}
                [f o] (act-for lay key w i {:stood-on so})
                t2 (System/nanoTime)
                a (try (c/offer! st o) (catch Exception e e))
                t3 (System/nanoTime)]
            (when (= :yes (nb/record! stats :value a (- t3 t2)))
              (vreset! (nth lasts w) {:fid [(:name o) 0] :stamp (:stamp a)})
              (when (zero? (mod (nb/yes-count stats :value) 50))
                (nb/sample! stats {:layer (:layer lay) :session (:session lay) :name (:name o)
                                   :kind :agent-value-stood-on :stamp (:stamp a) :e (:e f) :k (:k f) :v (:v f)
                                   :stood-on so})))))))))

(defn- lock-row-count [st L]
  (or (foreign-select-one [(keypath L :locks) (view count)] (:layers st)) 0))

(defn- measure-c!
  "C at K: 3 s unmeasured, then 15 s measured; around the measured window
  (never inside it) the layer's lock rows counted, one per entry from the
  `:own-row` mark."
  [ctx st lay k step]
  (let [warm (window! ctx st lay :C k (t 3 1) step)
        _ (nb/emit! "UNMEASURED" (select-keys warm [:variant :k :value-acts-per-s :errors]))
        l0 (lock-row-count st (:layer lay))
        w (window! ctx st lay :C k (t 15 2) step)
        l1 (lock-row-count st (:layer lay))
        el (long (* 1e9 (:secs w)))]
    (emit-result! ctx w {:act-kind :agent-value-stood-on :layer (:layer lay)
                         :check (nb/check-sample st (:sample w))
                         :index-writes (index-writes w :agent-value-stood-on)
                         :acts-per-s {:value (:value-acts-per-s w)
                                      :entry (nb/rate (get-in w [:admitted :entry] 0) el)
                                      :lease (nb/rate (or (get-in w [:leases :door]) 0) el)}
                         :entry-lock-rows (- l1 l0)})))

(def ^:private big-buf (ThreadLocal/withInitial (reify java.util.function.Supplier
                                                   (get [_] (ByteBuffer/allocate (* 16 1024 1024))))))

(defn- freeze-bytes
  "Bytes of `x` as Rama 1.6.0 serializes a PState value (B12)."
  [x]
  (let [^ByteBuffer bb (.get ^ThreadLocal big-buf)]
    (.clear bb) (rser/freeze bb x) (.position bb)))

(defn- key-bytes
  "Bytes of `x` as Rama 1.6.0 encodes a RocksDB key (B12)."
  [x]
  (let [^ByteBuffer bb (.get ^ThreadLocal big-buf)]
    (.clear bb) (rkey/k-ser bb x) (.position bb)))

(defn entry-bytes!
  "4.6's entry bytes (optional): in agent layers holding exactly 10, 100
  and 1,000 `:note` facts, a model's pattern read `[:k :note]`, whose line
  carries the exact list of every matched fact; the logical bytes (Rama's
  serializers, as 6.4) of each entry act's row, its three id-index entries
  (no copy) and its lock row, keys and values apart."
  [st mst key]
  (vec (for [n (if nb/smoke? [10 100] [10 100 1000])]
         (let [L (keyword (str "bench-e" n))
               lay (nb/layer! st mst {:layer L :kind :agent :owner :ada :session :bench-s1 :tasks tasks})
               _ (dotimes [i n] (nb/yes! "a fact for the entry bytes" (c/offer! st (second (act-for lay key 0 i)))))
               r (rx/read! st (assoc (read-spec lay [:pattern [:k key]] :shown) :limit 1000))
               _ (when (or (contains? r :refused) (nil? (:entry r)))
                   (throw (ex-info "the pattern read was refused" {:read (dissoc r :rows)})))
               nm (:entry r)
               row (first (c/raw-rows st L nm))
               stamp (:entry-stamp r)
               fid [nm 0]
               ies (into (sorted-map)
                         (for [f [:ix-ek :ix-ke :ix-s]
                               :let [a (rr/address f {:e (:e row) :k (:k row) :stamp stamp :fid fid})
                                     e (foreign-select-one [(keypath L f a)] (:layers st))]]
                           [f {:key (key-bytes a) :value (freeze-bytes e) :copy (:copy e)}]))
               lock (c/lock-row st L (:lock-id row))
               lr {:key (key-bytes (:lock-id row)) :value (freeze-bytes lock)}
               rb {:key (key-bytes nm) :value (freeze-bytes row)}]
           {:facts-in-layer n :matched (count (:matched r)) :sealed-bytes (alength ^bytes (:sealed row))
            :row rb :id-index-entries ies :lock-row lr
            :total (+ (:key rb) (:value rb) (reduce + (map #(+ (:key %) (:value %)) (vals ies))) (:key lr) (:value lr))}))))

(defn reads
  "One run of variant C (4.6), then the entry bytes: setup, a fact per
  writer written at setup for its first read, a 10 s idle window, a 20 s
  warm-up at 4 writers, then K = 1, 4, 16, each 3 s unmeasured and 15 s
  measured."
  [& [run-id]]
  (nb/with-run
    {:number number :run run-id :fn-name "reads" :args [run-id] :launch nb/timing-launch}
    (fn [{:keys [ipc] :as ctx}]
      (let [st (rx/connect ipc)
            mst (mc/connect ipc)
            _ (nb/people! st)
            _ (nb/person! st :ada)
            key nb/primary-key
            lay (nb/layer! st mst {:layer :bench-agent-r :kind :agent :owner :ada :session :bench-s1 :tasks tasks})
            ks (if nb/smoke? [1 4] [1 4 16])
            seeds (vec (for [w (range (apply max 4 ks))]
                         (let [[_ o] (act-for lay key (+ 100 w) 0)
                               a (nb/yes! "a fact for a first read" (c/offer! st o))]
                           {:fid [(:name o) 0] :stamp (:stamp a)})))
            step (read-write-step st lay key seeds)]
        (nb/emit! "RESULT" (merge (nb/idle-window (t 10 2)) {:number number :run run-id :uid (:uid ctx) :variant :idle-C})
                  number)
        (nb/emit! "WARMUP" (nb/result (window! ctx st lay :warm-up 4 (t 20 3) step)))
        (doseq [k ks] (measure-c! ctx st lay k step))
        (nb/emit! "RESULT" {:number number :run run-id :uid (:uid ctx) :variant :entry-bytes
                            :entries (entry-bytes! st mst key)}
                  number)
        :done))))
