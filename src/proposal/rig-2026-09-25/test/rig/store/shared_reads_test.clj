(ns rig.store.shared-reads-test
  "Stage 5b's pure functions (PLAN-reads-rest.md, 'Namespaces and tests'),
  no cluster: the shared moment and visibility, addresses and entries, the
  era merge, index and purge writes and the purge-equals-rebuild property,
  deltas' cursors, the standing chain, and totality over garbage (RT13)."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [clojure.test.check.clojure-test :refer [defspec]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [rig.store.envelope :as env]
            [rig.store.reads :as reads]
            [rig.store.shared-reads :as sr]))

(def nm1 [:g :by-entity :offer #uuid "01926c00-0000-7000-8000-000000000001"])
(def nm2 [:g :by-entity :offer #uuid "01926c00-0000-7000-8000-000000000002"])

(defn- row [k v] {:layer :g :k k :v nil :sealed (byte-array [1 2 3]) :replaces nil :mark #{}
                  :lock-id [nm1 0] :lock {:required [] :any-of [:bob] :blob nil :any-blobs {:bob (byte-array [9])}}
                  :digest (byte-array [7])})

(deftest the-moment
  (is (= {:F 7} (sr/moment nil 7)) "absent: the task's frontier")
  (is (= {:F 5} (sr/moment {:frontier 5} 7)))
  (is (= {:F 7} (sr/moment {:frontier 1000000000} 7)) "the clamp")
  (is (= {:F -1} (sr/moment nil nil)) "before the first batch")
  (is (= {:refused :moment-kind} (sr/moment {:stamp 5} 7)) "RR1")
  (is (= {:refused :moment-kind} (sr/moment 5 7)))
  (is (= {:refused :moment-kind} (sr/moment "x" 7))))

(deftest visibility
  (let [group {:kind :group :class :by-entity}]
    (is (sr/visible? nil group 3 5 :alice) "a member named at or below F")
    (is (not (sr/visible? nil group 6 5 :alice)) "named above F: not yet a member at F")
    (is (not (sr/visible? nil group nil 5 :carol)) "not a member")
    (is (not (sr/visible? nil nil nil 5 :alice)) "no layer: the private answer (F4)")
    (is (sr/visible? {:kind :base :class :by-entity} nil nil 5 :anyone) "the re-classed base: any actor")
    (is (sr/visible? {:kind :personal :owner :alice :class :by-entity} nil nil 5 :alice))
    (is (not (sr/visible? {:kind :personal :owner :alice :class :by-entity} nil nil 5 :bob)))
    (is (not (sr/visible? nil group 1 5 "alice")) "a reader that is no keyword")))

(deftest addresses-and-entries
  (let [parts {:e :e1 :k :note :batch 42 :stamp 7 :fid [nm1 0] :digest (apply str (repeat 64 "a"))}
        ek (sr/address :ix-ek parts)
        s (sr/address :ix-s parts)]
    (is (str/starts-with? ek (str "e1" reads/sep "note" reads/sep (reads/hex16 42) reads/sep (reads/hex16 7))) "RR3: batch before stamp")
    (is (str/starts-with? s (reads/hex16 42)))
    (is (neg? (compare (sr/address :ix-ek (assoc parts :batch 9)) (str "e1" reads/sep "note" reads/sep (reads/hex16 10))))
        "as of F an end bound: batch 9 below hex(F + 1) for F = 9"))
  (let [e (sr/entry-of (row :note nil) :e1 [nm1 0] 7 42 nil)
        t (sr/tombstone e 99)]
    (is (= [:e1 [nm1 0] 7 42] ((juxt :e :fid :stamp :batch) e)))
    (is (= 99 (:erased-at t)))
    (is (every? nil? ((juxt :v :sealed :lock :digest :copy) t)) "a tombstone holds no value field")
    (is (= (:lock-id e) (:lock-id t)) "a lock id stays: an id"))
  (let [lock-row {:required [:alice] :any-of [] :blob (byte-array [5]) :any-blobs nil}
        e (sr/entry-of (assoc (row :note nil) :lock nil) :e1 [nm1 0] 7 42 lock-row)]
    (is (= lock-row (:lock e)) "an own-row value's entry carries a copy of its lock row")))

(deftest index-and-purge-writes
  (let [hints {:by-value #{:note} :opaque #{} :no-copy #{}}
        frows [[:e1 0 (row :note nil) nil] [:e2 1 (row :tag nil) nil]]
        w (sr/index-writes hints nm1 frows {0 "d0"} 7 42)]
    (is (= 7 (count (:index-put w))) "three id entries a fact, and one value entry")
    (is (= 1 (count (:index-of w))))
    (is (= 2 (count (:index-id w))))
    (is (every? #(and (= 42 (:batch (nth % 2))) (= 7 (:stamp (nth % 2)))) (:index-put w)))
    (let [kv (some (fn [[f a _]] (when (= :ix-kv f) a)) (:index-put w))]
      (is (str/includes? kv "d0") "the value index is keyed by the digest, never the text (F12)")
      (let [entry (nth (first (:index-put w)) 2)
            p (sr/purge-writes entry #{kv} 99)]
        (is (= 3 (count (:index-put p))))
        (is (every? #(= 99 (:erased-at (nth % 2))) (:index-put p)))
        (is (= #{[:ix-kv kv] [:ix-of [nm1 0]]} (set (:index-del p))))
        (is (empty? (:index-id p)) "ids are kept"))))
  (testing "RR6, F9: an index error is data"
    (reset! sr/fail-index #{nm2})
    (is (:index-error (sr/index-writes {} nm2 [[:e1 0 (row :note nil) nil]] {} 1 1)))
    (reset! sr/fail-index #{})))

(defn- rebuild-writes
  "What a rebuild writes for one fact, given how it opens."
  [hints entry o]
  (sr/implied-writes hints entry o))

(defspec purge-writes-what-a-rebuild-would 60
  (prop/for-all [by-value? gen/boolean
                 date (gen/large-integer* {:min 1 :max 1000000})
                 how (gen/elements [:value :erased])]
    (let [hints {:by-value (if by-value? #{:note} #{}) :opaque #{} :no-copy #{}}
          entry (sr/entry-of (row :note nil) :e1 [nm1 0] 7 42 nil)
          live (rebuild-writes hints entry {:value "v" :stamp 7})
          kvs (set (keep (fn [[f a _]] (when (= :ix-kv f) a)) (:index-put live)))
          purged (sr/purge-writes (nth (first (:index-put live)) 2) kvs date)
          rebuilt (rebuild-writes hints entry {:erased-at date})]
      ;; applying the live writes then the purge leaves exactly what a rebuild of the erased fact writes
      (let [apply-w (fn [m w] (as-> m m
                                (reduce (fn [m [f a e]] (assoc-in m [f a] e)) m (:index-put w))
                                (reduce (fn [m [fid s]] (assoc-in m [:ix-of fid] s)) m (:index-of w))
                                (reduce (fn [m [fid a]] (assoc-in m [:ix-id fid] a)) m (:index-id w))
                                (reduce (fn [m [f a]] (update m f dissoc a)) m (:index-del w))))
            after-purge (apply-w (apply-w {} live) purged)
            after-rebuild (apply-w {} rebuilt)
            clean (fn [m] (into {} (remove (comp empty? val)) m))]
        (= (clean after-purge) (clean after-rebuild))))))

(deftest the-era-merge
  (let [k (fn [e k s fid] {:entry {:e e :k k :stamp s :fid [nm1 fid]}})
        stream {:kept [(k :p7 :title 100 0) (k :p8 :title 110 1)] :more? false}
        micro {:kept [(k :p7 :title 200 2) (k :p9 :title 210 3)] :more? false}]
    (is (= [0 2 1 3] (mapv #(second (get-in % [:entry :fid])) (:kept (sr/merge-eras :k 10 stream micro))))
        "F1: by entity, then stamp, from the entries' fields")
    (is (= [2] (mapv #(second (get-in % [:entry :fid])) (:kept (sr/merge-eras :latest 10 {:kept [(k :p7 :title 100 0)]} {:kept [(k :p7 :title 200 2)]}))))
        "the head with the larger stamp")
    (is (= [0 1 2 3] (mapv #(second (get-in % [:entry :fid])) (:kept (sr/merge-eras :kv 10 stream micro))))
        "[:kv]: the stream era, then the micro era")
    (let [m (sr/merge-eras :k 3 stream micro)]
      (is (= 3 (count (:kept m))))
      (is (:more? m) "the union past the limit is partial"))))

(deftest deltas-and-the-chain
  (let [pp (reads/parse-pattern [:k :note] 2 nil)
        m 1000
        st {:kept [{:address (str "a" reads/sep "b")}] :cut :limit}]
    (is (= :ix-s (sr/delta-index pp)))
    (is (= {:from (reads/hex16 1001)} (sr/next-scan pp m {:kept []})) "a delta delivered whole: past its moment")
    (is (= {:from (str "a" reads/sep "b" reads/sep)} (sr/next-scan pp m st)) "RR10: after the last shown row")
    (let [last-scanned (str (reads/hex16 900) reads/sep (reads/fid-text [nm1 3]))
          nx (sr/next-scan pp m {:kept [] :cut :budget :last last-scanned})]
      (is (string? (:token nx)) "F2 as built: a budget cut resumes from a sealed cursor")
      (is (not (str/includes? (:token nx) (reads/hex16 900))) "the client cannot read it")
      (is (= (str last-scanned reads/sep) (reads/open-cursor (:token nx))) "the module can, exactly past what it scanned")
      (is (= (str last-scanned reads/sep) (sr/scan-from nx)))
      (is (nil? (reads/open-cursor "00ff")) "a token not of this module opens nothing")))
  (let [l1 (reads/standing-link nil {:stamp 5} "f1")
        l2 (reads/standing-link l1 {:stamp 9} "f2")]
    (is (= {:fp l1 :n 1 :partial? false} (sr/so-far nil {:stamp 5} "f1" :complete true)))
    (is (= {:fp l2 :n 2 :partial? true} (sr/so-far {:fp l1 :n 1 :partial? false} {:stamp 9} "f2" :partial true)))
    (is (= {:fp l1 :n 1} (sr/so-far {:fp l1 :n 1} {:stamp 9} "f2" :complete false)) "nothing new: unchanged")
    (is (not= l1 l2))
    (is (= (reads/standing-link nil nil nil) (:fingerprint (sr/closing nil {:layer :g :moment {:stamp 1}}))) "no line: the seed")
    (is (= {:fingerprint l2 :deliveries 2 :mark :partial :moment {:stamp 9} :layer :g}
           (sr/closing {:so-far {:fp l2 :n 2 :partial? true} :moment {:stamp 9} :layer :g} nil)))))

(deftest ops-and-arguments-are-total
  (is (= {:refuse :bad-op} (sr/micro-op {:op :rebuild-put :task 0 :entities 1})) "no request id")
  (is (= :rebuild-put (:op (sr/micro-op {:op :rebuild-put :task 0 :req "r" :entities 1}))))
  (is (= {:refuse :bad-op} (sr/micro-op {:op :rebuild-sweep :task 0 :req "r" :field :ix-nope :entries 1})))
  (is (= [:g "a"] (:after (sr/micro-op {:op :person-purge :task 0 :req "r" :person :bob :entries 5 :after [:g "a"]}))))
  (is (= {:refused :bad-scan} (sr/delta-args :alice :alice {:from 5 :kind :stamp})))
  (is (nil? (sr/delta-args :alice :alice {:from "x" :kind :frontier})))
  (is (= {:refused :bad-read} (sr/upkeep-args "x" nil nil nil nil 1)))
  (is (= {:refused :bad-read} (sr/reader-args :g 5))))

(defspec nothing-throws 200
  (prop/for-all [x (gen/one-of [gen/any-printable-equatable (gen/return nil) (gen/vector gen/any-printable-equatable)])]
    (and (map? (sr/micro-op x))
         (map? (sr/moment x 3))
         (boolean? (sr/visible? x x x 3 x))
         (map? (sr/index-writes {} x x x x x))
         (map? (sr/purge-writes x x x))
         (map? (sr/implied-writes {} x x))
         (map? (sr/sweep-writes :ix-ek x x x))
         (map? (sr/merge-eras :k 3 x x))
         (map? (sr/micro-pp {:kind :kv :pattern [:kv :note x] :v x}))
         (map? (sr/kv-digests {:by-value #{:note}} {:facts [{:k :note :v x}]} {}))
         (boolean? (reads/entry-matches? x x)))))
