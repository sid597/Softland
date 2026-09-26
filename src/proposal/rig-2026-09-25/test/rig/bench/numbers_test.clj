(ns rig.bench.numbers-test
  "Phase 7's harness tests (PLAN-numbers.md 9.3): each harness measures what
  it claims, on the finished store. Run under the cluster lock:

    flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.bench.numbers-test

  Three tests: the pure ones (T5's pure half, T10, T11), one cluster at
  {:tasks 4 :threads 4 :workers 1} (T1 to T6, T8, T9), and one at {:tasks 1
  :threads 1 :workers 1} (T7). Each cluster is opened and closed inside its
  own test and clojure.test runs a namespace's tests one after another, so
  the two are never open at once (F6). The driver runs no measurement
  unless this namespace passes."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [rig.bench.agent-rate :as ar]
            [rig.bench.lock-bench :as lb]
            [rig.bench.lock-growth :as lg]
            [rig.bench.numbers :as nb]
            ;; not used here: loading it makes the set's first step fail at once
            ;; when any harness does not compile
            [rig.bench.one-thread]
            [rig.store.client :as c]
            [rig.store.micro-client :as mc]
            [rig.store.read-exit :as rx])
  (:import [java.util Random]
           [java.util.concurrent Executors]
           [org.rocksdb RocksDB]))

(def pkey "The primary act's key (F1)." nb/primary-key)

;; ================================================================= helpers

(defn- growth-matches
  "Every field of the layer grew by what the write list claims for
  `counts` (T1: a field the store has and the list does not name fails, and
  so does a named write that did not happen); :settings unchanged."
  [label g counts opts]
  (let [expected (nb/expected-growth counts opts)]
    (doseq [[f n] (:fields g)]
      (if (number? n)
        (is (= (get expected f 0) n) (str label ": field " f " grew by " n ", the list says " (get expected f 0)))
        (is (= :same n) (str label ": field " f " changed"))))
    (doseq [[f n] expected]
      (is (contains? (:fields g) f) (str label ": the list names " f ", which the store does not have")))))

(defn- offer-all!
  "`n` value acts of writer `w` into `lay` through `st`, one at a time;
  their answers."
  [st lay w n]
  (vec (for [i (range n)] (c/offer! st (second (ar/act-for lay pkey w i))))))

(defn- door-leases [st lay] (nb/known-leases [[st (:layer lay) (:session lay)]]))

;; ============================================================ pure: T5, T10, T11

(deftest pure-test
  (testing "T5, pure: rate and percentiles on known values"
    (let [a (long-array (map #(* % 1000000) (range 1 101)))]
      (java.util.Arrays/sort a)
      (is (= {:n 100 :p50 50.0 :p95 95.0 :p99 99.0 :max 100.0 :mean 50.5} (nb/percentiles a))))
    (is (= 500.0 (nb/rate 1000 2000000000)))
    (is (nil? (nb/percentiles (long-array 0)))))

  (testing "T5, the open loop measures a stall from each slot scheduled during it (F5)"
    (let [period 10000000
          w (nb/open-window {:ctx {:number :test :run "T5"} :variant :stall :k 1 :warm 0 :secs 0.4 :trace? true
                             :lanes [{:threads 1 :period-ns period :phase-ns 0 :kind :value
                                      :send (fn [n] (when (= n 10) (Thread/sleep 50)) {:answer :yes})}]})
          by-slot (into {} (map (fn [[_ n ts started ended]] [n {:ts ts :started started :ended ended}]) (:trace w)))
          stall-end (:ended (get by-slot 10))
          during (for [n (range 11 15)] (get by-slot n))]
      (is (= 4 (count (filter some? during))) "four slots were scheduled during the 50 ms stall")
      (doseq [{:keys [ts started ended]} during]
        (is (pos? (- started ts)) "each was sent late, at once")
        (is (>= (- ended ts) (- stall-end ts 1000000)) "its latency runs from its slot and holds the stall"))
      (is (>= (get-in w [:lat :value :max]) 40.0) "the window's slowest latency holds the stall")))

  (testing "T10, the verdict rule"
    (is (= :far-passes (:call (nb/verdict 10000 1000 :higher))))
    (is (= :near-passes (:call (nb/verdict 2249 1000 :higher))))
    (is (= :near-passes (:call (nb/verdict 4.7 20 :lower))))
    (is (= :far-fails (:call (nb/verdict 50 1000 :higher))))
    (is (= :over-four-times-change-the-default (:call (nb/bytes-verdict 4.7))))
    (is (= :fine (:call (nb/bytes-verdict 0.85))))
    (is (= :between-two-and-four (:call (nb/bytes-verdict 2.17)))))

  (testing "T11, the values: the slice's first 100 for h40, h200, h40-p2; every variant at its size"
    (doseq [[v slice-cfg] [["h40" {:size 40 :people 1}] ["h200" {:size 200 :people 1}] ["h40-p2" {:size 40 :people 2}]]]
      (let [r1 (Random. 20260925) r2 (Random. 20260925) cfg (get lg/variants v)]
        (is (= (vec (for [i (range 100)] (lb/value-spec r1 i slice-cfg)))
               (vec (for [i (range 100)] (lg/value-spec r2 i cfg))))
            (str v ": the slice's first 100 values"))))
    (doseq [[v cfg] lg/variants]
      (let [r (Random. 20260925)]
        (is (every? #(= (:size cfg) (lg/canonical-size (:v (lg/value-spec r % cfg)))) (range 100))
            (str v ": every value " (:size cfg) " bytes"))))))

;; ====================================================== four tasks: T1-T6, T8, T9

(deftest four-task-test
  (with-open [ipc (nb/launch! nb/timing-launch)]
    (let [tasks 4
          st (c/connect ipc)
          mst (mc/connect ipc)]
      (nb/people! st)
      (doseq [p [:ada :bob]] (nb/person! st p))

      (testing "T9, the machine: every field present and not empty"
        (let [m (nb/machine {:dir (nb/ipc-root ipc) :ran {:ns "rig.bench.numbers-test" :fn "T9" :args []}})]
          (doseq [path [[:cpu] [:cores] [:hardware-threads] [:ram] [:os] [:kernel] [:jdk :vm] [:jdk :runtime]
                        [:jdk :vendor] [:jvm-flags] [:gc] [:rama] [:rama-helpers] [:clojure] [:cluster-dir]
                        [:disk :filesystem] [:disk :device] [:disk :disk-model] [:load] [:git :head] [:host] [:at]]]
            (let [x (get-in m path)]
              (is (and (some? x) (not (and (or (string? x) (coll? x)) (empty? x)))) (str "machine " path " is " (pr-str x)))))
          (println "T9 machine" (nb/edn-str (select-keys m [:cpu :cores :hardware-threads :ram :os :kernel :jdk :rama :disk :load])))))

      (testing "T1, the write list is the store's own: an agent layer with no grammar, 100 acts, two leases"
        (let [lay (nb/layer! st mst {:layer :t1-agent :kind :agent :owner :ada :session :t1-s :tasks tasks})
              door (c/connect ipc)
              before (nb/field-counts door :t1-agent tasks)
              answers (offer-all! door lay 0 100)
              after (nb/field-counts door :t1-agent tasks)
              g (nb/growth before after)]
          (is (every? #(= :yes (:answer %)) answers))
          (is (= 2 (door-leases door lay)) "exactly two lease acts: 64 locks, then 64 more at the 65th act")
          (growth-matches "T1" g {:agent-value 100 :lease 2} {:first-uses 1})
          (is (= 102 (get-in g [:fields :answers])))
          (is (= 28 (get-in g [:fields :leases])) "2 x 64 - 100")
          (is (= 1 (get-in g [:fields :key-rows])) "the first :note act's key row (F2)")
          (is (= 102 (:log-rows g)))
          (is (= [(:home lay)] (:clocks-moved g)) "the home task's clock moved, no other")
          (if (set? (:present after))
            (is (every? (nb/store-fields) (:present after)) "every field the store reports is in the harness's list")
            (println "T1: the store's own field keys:" (:present after)))
          (println "T1 growth" (nb/edn-str g))))

      (testing "T2, a by-value grammar: :ix-kv and :ix-of by 100, the key row used"
        (let [lay (nb/layer! st mst {:layer :t2-agent :kind :agent :owner :ada :session :t2-s :tasks tasks
                                     :grammars {pkey nb/note-by-value}})
              door (c/connect ipc)
              before (nb/field-counts door :t2-agent tasks)
              answers (offer-all! door lay 0 100)
              g (nb/growth before (nb/field-counts door :t2-agent tasks))]
          (is (every? #(= :yes (:answer %)) answers))
          (growth-matches "T2" g {:agent-value-by-value 100 :lease 2} {:first-uses 0})
          (is (= 100 (get-in g [:fields :ix-kv])))
          (is (= 100 (get-in g [:fields :ix-of])))
          (is (true? (:used (foreign-select-one [(keypath :t2-agent :key-rows pkey)] (:layers st))))
              "the first :note act set the grammar's row :used")))

      (testing "T3, a personal and a hand layer: a lock row per value"
        (doseq [[L kind] [[:t3-person :personal] [:t3-hand :hand]]]
          (let [lay (nb/layer! st mst {:layer L :kind kind :owner :ada :tasks tasks})
                door (c/connect ipc)
                before (nb/field-counts door L tasks)
                answers (offer-all! door lay 0 100)
                g (nb/growth before (nb/field-counts door L tasks))]
            (is (every? #(= :yes (:answer %)) answers))
            (growth-matches (str "T3 " kind) g {:owner-value 100 :lease 2} {:first-uses 1})
            (is (= 100 (get-in g [:fields :locks])) (str kind ": 100 lock rows")))))

      (testing "T4, the read entry: 20 point reads through the exit with variant C's spec"
        (let [lay (nb/layer! st mst {:layer :t4-agent :kind :agent :owner :ada :session :t4-s :tasks tasks})
              ;; the facts read are written in the owner's default session, so the
              ;; reader's fresh door leases in :t4-s itself (a door takes a session's
              ;; unconsumed locks from any earlier lease, which would hide the lease)
              fids (mapv (fn [a] [(:name a) 0])
                         (offer-all! st (assoc lay :session (c/default-session :ada) :pid (:root-pid lay)) 0 20))
              reader (rx/connect ipc)
              before (nb/field-counts reader :t4-agent tasks)
              leases0 (door-leases reader lay)
              reads (vec (for [fid fids] (rx/read! reader (ar/read-spec lay [:point [fid]] :stood-on))))
              lease-acts (- (door-leases reader lay) leases0)
              g (nb/growth before (nb/field-counts reader :t4-agent tasks))]
          (is (every? #(and (:entry %) (not (contains? % :refused))) reads) (str "every read shown: " (first (filter :refused reads))))
          (is (= 1 lease-acts) "a fresh door leases once for 20 entries")
          (is (= 20 (get-in g [:fields :locks])) "one lock row per entry, the :own-row mark")
          (growth-matches "T4" g {:entry 20 :lease lease-acts} {:first-uses 0})
          (let [checks (for [r reads] (nb/check-act reader {:layer :t4-agent :session :t4-s :name (:entry r)
                                                            :kind :entry :stamp (:entry-stamp r)}))]
            (is (every? #(every? true? (vals %)) checks) (str "each entry read back: " (first (remove #(every? true? (vals %)) checks))))
            (is (every? :no-copy checks) "the entries' id-index entries carry no value fields"))))

      (testing "T5 and T6, the counts and the placement, in a short window of four writers"
        (let [lay (nb/layer! st mst {:layer :t5-agent :kind :agent :owner :ada :session :t5-s :tasks tasks})
              door (c/connect ipc)
              _ (offer-all! door lay 9 5)
              before (nb/field-counts door :t5-agent tasks)
              w (nb/closed-window {:ctx {:number :test :run "T5"} :st door :tasks tasks :home (:home lay)
                                   :lease-doors [[door :t5-agent :t5-s]] :variant :T5 :k 4 :secs 2
                                   :step (ar/value-step door lay pkey :agent-value)})
              g (nb/growth before (nb/field-counts door :t5-agent tasks))
              answered (reduce + (vals (get-in w [:outcomes :value])))]
          (is (= (:iterations w) answered) "every answer the writers received is counted")
          (is (zero? (:errors w)))
          (is (= (get-in w [:admitted :value]) (get-in w [:outcomes :value :yes])))
          (is (true? (get-in w [:leases :agree?])) (str "the two lease counts agree: " (:leases w)))
          (is (= (get-in w [:leases :door]) (- (get-in g [:fields :answers]) (get-in w [:admitted :value])))
              "and agree with the growth of :answers less the value acts")
          (is (true? (get-in w [:placement :one-task?])) (str "T6: every offer decided on the home task: " (:placement w)))
          (let [check (nb/check-sample door (:sample w))]
            (is (pos? (:checked check)))
            (is (= (:checked check) (:ok check)) (str "the read-back: " check)))))

      (testing "T6, layers-on-task! and one more act into each (F7)"
        (let [target 1
              ids (nb/layers-on-task! st {:prefix "t6-a" :owner :ada :n 3 :target target :tasks tasks})]
          (is (= 3 (count ids)))
          (doseq [L ids]
            (let [p0 (nb/partition-ends st tasks)
                  c0 (nb/task-clocks st tasks)
                  a (c/offer-until-answered! st (c/grant-offer st [:ada L L]))]
              (is (= :yes (:answer a)))
              (is (= [target] (nb/grown p0 (nb/partition-ends st tasks))) (str L ": only the target partition grew"))
              (is (= [target] (nb/grown c0 (nb/task-clocks st tasks))) (str L ": only the target task's clock moved"))))))

      (testing "T8, the wrap: an unmarked :mention of Bob is wrapped under the owner alone; a marked one under both"
        (let [L :t8-hand
              lay (nb/layer! st mst {:layer L :kind :hand :owner :ada :tasks tasks :grammars {:mention nb/mention-grammar}})
              door (c/connect ipc)
              act (fn [f] (let [o (c/build {:who :ada :layer L :class :by-layer :permission (:root-pid lay) :facts [f]})
                                a (c/offer! door o)]
                            (is (= :yes (:answer a)) (str "admitted: " a))
                            (c/lock-row door L (:lock-id (first (c/raw-rows door L (:name o)))))))
              note (act {:e :t8-a :k pkey :v {:text "hi"} :mark #{}})
              plain (act {:e :t8-b :k :mention :v {:persons #{:bob} :text "hi"} :mark #{}})
              marked (act {:e :t8-c :k :mention :v {:persons #{:bob} :text "hi"} :mark #{:die-with-any}})]
          (is (= [:ada] (:required plain)))
          (is (= (lg/value-bytes note) (lg/value-bytes plain)) "the size of an unmarked :note's record")
          (is (= [:ada :bob] (:required marked)))
          (is (= 28 (- (alength ^bytes (:blob marked)) (alength ^bytes (:blob plain)))) "one more seal: 28 bytes"))))))

;; ============================================================== one task: T7

(deftest one-task-test
  (RocksDB/loadLibrary)
  (with-open [ipc (nb/launch! nb/bytes-launch)]
    (let [st (c/connect ipc)
          ps (:layers st)
          root (nb/ipc-root ipc)
          scratch (doto (java.io.File. (System/getProperty "java.io.tmpdir") (str "phase7-t7-" (System/currentTimeMillis))) (.mkdirs))
          ex (Executors/newFixedThreadPool 16)
          write! (fn [L owner n]
                   (nb/yes! "making" (c/offer-until-answered! st (c/make-layer-offer L {:kind :hand :owner owner})))
                   (nb/yes! "granting" (c/offer-until-answered! st (c/grant-offer st [owner L L])))
                   (let [r (Random. 20260925)
                         pairs (vec (for [i (range n)] [i (lg/value-spec r i (get lg/variants "h40"))]))
                         ;; write-phase! writes as :alice into :alice-hand; the second layer's acts are built here
                         answers (if (= L lg/layer)
                                   (:answers (lg/write-phase! ex st pairs 1 1000 (fn [& _])))
                                   (frequencies (for [[_ f] pairs]
                                                  (nb/outcome-key
                                                   (c/offer! st (c/build {:who owner :layer L :class :by-layer
                                                                          :permission [owner L L] :facts [f]}))))))]
                     answers))]
      (try
        (nb/people! st)
        (doseq [p [:alice :bob]] (nb/person! st p))
        (testing "T7, the lock-store measure, on 2,000 values of 40 bytes in one hand layer"
          (is (= {:yes 2000} (write! lg/layer :alice 2000)))
          (let [logical (lg/lock-rows-logical ps lg/layer 10000 100)
                dirs (lb/rocks-dirs root "$$layers")]
            (is (= 2000 (:rows logical)))
            (doseq [[lid rec kb vb] (:sampled logical)]
              (let [again (c/lock-row st lg/layer lid)]
                (is (= vb (lg/value-bytes again)) "a row sized in the pages is the row read by keypath")
                (is (= (:required rec) (:required again)))
                (is (= kb (lg/key-bytes lid)))))
            (is (= 1 (count dirs)) "one task, one $$layers directory")
            (let [d (lg/lock-store-on-disk (first dirs) scratch (dissoc logical :sampled))
                  pk (get-in d [:lock-store :pick])]
              (println "T7 one layer" (nb/edn-str (update d :lock-store dissoc :repack)))
              (is (nil? (:error d)))
              (is (true? (get-in d [:lock-store :checks :one-structure?])) "the pick is one structure")
              (is (= 2000 (:picked pk)) "exactly the logical rows")
              (is (true? (get-in d [:lock-store :checks :bytes?])) "raw bytes within 4 a row of the logical ones")
              (is (= :re-pack (get-in d [:lock-store :method])))
              (is (true? (get-in d [:lock-store :repack-exact?])) "the re-packed store holds exactly the picked entries (F8)")
              (is (pos? (get-in d [:lock-store :sst])))
              (is (<= (get-in d [:lock-store :sst]) (get-in d [:compacted :sst])) "no larger than the whole store")
              (is (>= (get-in d [:lock-store :sst]) (* 60 2000)) "at least 60 bytes a row: a sealed lock does not compress (F8)")))
          (testing "then a second hand layer's values in the same store: the one-structure check fails"
            (is (= {:yes 500} (write! :bob-hand :bob 500)))
            (let [logical (dissoc (lg/lock-rows-logical ps lg/layer 10000) :sampled)
                  d (lg/lock-store-on-disk (first (lb/rocks-dirs root "$$layers")) scratch logical)
                  pk (get-in d [:lock-store :pick])]
              (println "T7 two layers" (nb/edn-str (update d :lock-store dissoc :repack)))
              (is (= 2500 (:picked pk)) "both layers' lock rows are picked by value")
              (is (false? (get-in d [:lock-store :checks :one-structure?])) "the check fails: it is not empty")
              (is (= :estimate (get-in d [:lock-store :method])) "and the named estimate is taken, with its bounds")
              (println "T7 two layers: the picked rows make" (:runs pk) "stretches of key order"))))
        (finally
          (.shutdown ex))))))
