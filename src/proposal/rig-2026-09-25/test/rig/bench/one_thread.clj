(ns rig.bench.one-thread
  "Number 3 (PLAN-numbers.md 5): one person's layer on one thread, acts a
  second and latency, on the finished store. Every offer into a one-owner
  layer is decided on that layer's one task (the gate's `<<sources` block
  has no partitioner), and under {:tasks 4 :threads 4} that task has one
  thread; every window checks it (4.8's placement).

  From the rig folder, each call its own JVM and its own cluster, under the
  cluster lock (8.1):

    clojure -M:bench rig.bench.one-thread run <n>     (a), (b) and (b')
    clojure -M:bench rig.bench.one-thread reads <n>   (c), the person's own reads

  The layer: `:bench-person`, {:kind :personal :owner :pat}, per-value
  grain; `:pat` writes citing `[:pat L L]` in the door's default session
  `:door/pat` (5.2). The act is number 1's (2.2), and in a personal layer it
  writes 10 (2.3: its lock row). (a) and (b) are closed loops: their
  latencies are service times at a fixed number in flight, reported and
  labelled so; the latency threshold is judged on (b')'s open arrival (F5).

  Every RESULT line goes to `runs/phase7-final-one-thread.edn` (8.5)."
  (:require [rig.bench.agent-rate :as ar]
            [rig.bench.numbers :as nb]
            [rig.store.client :as c]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx])
  (:import [java.util Random]))

(def number :one-thread)
(def tasks (:tasks nb/timing-launch))

(defn- t [full small] (if nb/smoke? small full))

(defn- setup!
  "5.2: the store layer, the person `:pat`, the personal layer and its
  owner's root permission. [store layer-spec]."
  [ipc]
  (let [st (rx/connect ipc)
        mst (mc/connect ipc)]
    (nb/people! st)
    (nb/person! st :pat)
    [st (nb/layer! st mst {:layer :bench-person :kind :personal :owner :pat :tasks tasks})]))

(defn- emit-result! [ctx w extra]
  (let [r (merge (nb/result w) {:number number :run (:run ctx) :uid (:uid ctx)} extra)]
    (nb/emit! "RESULT" r number)
    r))

(defn- window! [ctx st lay variant k opts step]
  (nb/closed-window (merge {:ctx ctx :st st :tasks tasks :home (:home lay)
                            :lease-doors [[st (:layer lay) (:session lay)]]
                            :variant variant :k k :step step}
                           opts)))

(defn tagged-step
  "(a)'s iteration (5.3): number 1's, each offer also tagged leased when the
  door came to know a new lease during its call, which is exact with one
  writer, and its latency kept under its tag too."
  [st lay key]
  (let [ld [[st (:layer lay) (:session lay)]]]
    (fn [w i stats]
      (let [[f o] (ar/act-for lay key w i)
            k0 (nb/known-leases ld)
            t0 (System/nanoTime)
            a (try (c/offer! st o) (catch Exception e e))
            t1 (System/nanoTime)
            ok (nb/record! stats :value a (- t1 t0))]
        (nb/record-lat! stats (if (> (nb/known-leases ld) k0) :value-leased :value-unleased) (- t1 t0))
        (when (and (= :yes ok) (zero? (mod (nb/yes-count stats :value) 50)))
          (nb/sample! stats {:layer (:layer lay) :session (:session lay) :name (:name o) :kind :owner-value
                             :stamp (:stamp a) :e (:e f) :k (:k f) :v (:v f)}))))))

(defn- open-lane
  "(b')'s one lane: a slot every `period-ns` from a random phase, served by
  `threads` sender threads through the one door."
  [st lay key threads period-ns ^Random rnd]
  {:threads threads :period-ns period-ns :kind :value
   :phase-ns (long (* period-ns (.nextDouble rnd)))
   :send (fn [n]
           (let [[f o] (ar/act-for lay key 0 n)
                 a (c/offer! st o)]
             (cond-> a
               (= :yes (:answer a))
               (assoc :sample-item {:layer (:layer lay) :session (:session lay) :name (:name o) :kind :owner-value
                                    :stamp (:stamp a) :e (:e f) :k (:k f) :v (:v f)}))))})

(defn run
  "One run of number 3's (a), (b) and (b') (5.3 to 5.4b): setup, a 10 s
  idle window, a 20 s warm-up at 4 writers; (a) one writer, 1,000 acts
  unmeasured then 6,400 measured, each timed and tagged; (b) K = 1, 2, 4,
  8, 16, 32, 64, 128 closed-loop writers, each 3 s unmeasured and 10 s
  measured; (b') value acts on a fixed schedule at 100 a second (4 sender
  threads) and at 1,000 a second (32), each 3 s unmeasured and 15 s
  measured, latency from the schedule."
  [& [run-id]]
  (nb/with-run
    {:number number :run run-id :fn-name "run" :args [run-id] :launch nb/timing-launch}
    (fn [{:keys [ipc] :as ctx}]
      (let [[st lay] (setup! ipc)
            key nb/primary-key
            step (ar/value-step st lay key :owner-value)
            check (fn [w] (nb/check-sample st (:sample w)))]
        (nb/emit! "RESULT" (merge (nb/idle-window (t 10 2))
                                  {:number number :run run-id :uid (:uid ctx) :layers {:bench-person (:home lay)}})
                  number)
        (nb/emit! "WARMUP" (nb/result (window! ctx st lay :warm-up 4 {:secs (t 20 3)} step)))
        ;; (a): one writer, one act at a time
        (let [tagged (tagged-step st lay key)
              warm (window! ctx st lay :a 1 {:n (t 1000 100)} tagged)
              _ (nb/emit! "UNMEASURED" (select-keys warm [:variant :k :value-acts-per-s :errors]))
              w (window! ctx st lay :a 1 {:n (t 6400 300)} tagged)]
          (emit-result! ctx w {:act-kind :owner-value :layer (:layer lay) :check (check w)
                               :index-writes (ar/index-writes w :owner-value)}))
        ;; (b): K writers sharing one door
        (doseq [k (if nb/smoke? [1 4 16] [1 2 4 8 16 32 64 128])]
          (let [warm (window! ctx st lay :b k {:secs (t 3 1)} step)
                _ (nb/emit! "UNMEASURED" (select-keys warm [:variant :k :value-acts-per-s :errors]))
                w (window! ctx st lay :b k {:secs (t 10 2)} step)]
            (emit-result! ctx w {:act-kind :owner-value :layer (:layer lay) :check (check w)
                                 :index-writes (ar/index-writes w :owner-value)})))
        ;; (b'): open arrival at the threshold's rate and at ten times it (F5)
        (let [rnd (Random.)]
          (doseq [[per-s threads] [[100 4] [1000 32]]]
            (let [w (nb/open-window {:ctx ctx :st st :tasks tasks :home (:home lay)
                                     :lease-doors [[st (:layer lay) (:session lay)]]
                                     :variant :b-prime :k per-s
                                     :lanes [(open-lane st lay key threads (quot 1000000000 per-s) rnd)]
                                     :warm (t 3 1) :secs (t 15 2)})
                  whole (get-in w [:whole-window :admitted])
                  door (or (get-in w [:leases :door]) 0)
                  leases-measured (if (pos? whole)
                                    (Math/round (* (double door) (/ (get-in w [:admitted :value]) whole)))
                                    0)]
              (emit-result! ctx w {:act-kind :owner-value :layer (:layer lay) :check (check w)
                                   :arrival-per-s per-s :senders threads
                                   :index-writes (assoc (ar/index-writes w :owner-value :leases leases-measured :secs (:secs w))
                                                        :leases-in-the-measured-part :derived)}))))
        :done))))

(defn reads
  "(c), the person's own reads (5.5, optional; its own JVM in the full set):
  setup, a 20 s warm-up at 4 writers, 200 facts written and kept, then
  2,000 sequential point reads of them in rotation through the exit (reader
  kind `:person`, the personal layer as the layer read and the working
  layer, the entry in `:door/pat`), each `read!` timed: its query and its
  acked entry, the exit's two round trips (PLAN-read-exit.md,
  'Minimization')."
  [& [run-id]]
  (nb/with-run
    {:number number :run run-id :fn-name "reads" :args [run-id] :launch nb/timing-launch}
    (fn [{:keys [ipc] :as ctx}]
      (let [[st lay] (setup! ipc)
            key nb/primary-key
            L (:layer lay)
            step (ar/value-step st lay key :owner-value)
            _ (nb/emit! "WARMUP" (nb/result (window! ctx st lay :warm-up 4 {:secs (t 20 3)} step)))
            fids (vec (for [i (range 200)]
                        (let [[_ o] (ar/act-for lay key 900 i)]
                          (nb/yes! "a fact to read" (c/offer! st o))
                          [(:name o) 0])))
            read-step (fn [_ i stats]
                        (let [fid (nth fids (mod i (count fids)))
                              t0 (System/nanoTime)
                              r (try (rx/read! st {:reader :pat :reader-kind :person :working L :layer L
                                                   :permission (:pid lay) :read [:point [fid]]})
                                     (catch Exception e e))
                              t1 (System/nanoTime)
                              ok? (and (map? r) (some? (:entry r)) (not (contains? r :refused)))]
                          (nb/record! stats :read (cond (instance? Throwable r) r
                                                        ok? {:answer :yes}
                                                        :else {:answer :no :reason (:refused r)})
                                      (- t1 t0))
                          (when (and (map? r) (:entry r))
                            (nb/record! stats :entry (if ok? {:answer :yes} {:answer :no :reason (:refused r)}) (- t1 t0)))))
            w (window! ctx st lay :c 1 {:n (t 2000 200)} read-step)]
        (emit-result! ctx w {:layer L :reads (count fids)})
        :done))))
