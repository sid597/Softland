(ns rig.bench.put-page-bench
  "Wave 1: the read exit's put page cap (`reads/max-put-rows`, built at
  4,096 rows, 2,048 since this measurement) re-measured now that every
  row's open reads its lock (phase 2's
  `open-row>`: the ledger entry, the lock row or the record lock, the
  wrap's person entries, one unwrap and one open of the value). A put page
  is one event on the layer's home task and never yields
  (BUILD_NOTES-read-exit.md, D3), so its length is how long it holds that
  task; the read exit's plan estimated 0.1 to 0.3 s a page before sealing.

  Run from the rig folder, under the cluster lock:

    clojure -M:bench rig.bench.put-page-bench run [rows-per-act] [acts] [repeats]

  One in-process cluster (4 tasks, 2 threads, 1 worker), seeded with the
  model's one-owner world. Into each of three layers of Alice's it writes
  `acts` acts (default 10) of `rows-per-act` facts (default 512), through
  the door: `:alice` (personal: every value's lock in a lock row), `:alice-agent`
  (agent: every lock in the record) with sealed `:note` values of about 40
  bytes, and a personal layer `:bench-retracts` of retracts (`:v nil`: no
  seal, so its opens read nothing, the pre-sealing cost's stand-in). Then,
  per layer, it sends the first put page (`{:op :rebuild-put :after nil
  :acts 256}`, which stops once the cap's rows are gathered) `repeats` times
  (default 7; a page is idempotent) and reports the client's wall time per
  page (median and max), the rows the page covered, and microseconds per
  row. The in-process cluster is not a proxy for production latency: only
  the order of magnitude and the ratio between layers mean anything."
  (:require [com.rpl.rama :refer :all]
            [com.rpl.rama.path :refer :all]
            [com.rpl.rama.test :as rtest]
            [rig.store.client :as c]
            [rig.store.module :as m]
            [rig.store.read-exit :as rx]
            [rig.store.reads :as reads]))

(defn- ms [t0] (/ (- (System/nanoTime) t0) 1e6))

(defn- median [xs] (let [v (vec (sort xs))] (nth v (quot (count v) 2))))

(defn- fill!
  "Write `acts` acts of `n` facts into `layer` as Alice, through the door."
  [st layer n acts retracts?]
  (dotimes [a acts]
    (let [o (c/build {:who :alice :layer layer :class :by-layer :permission [:alice layer layer]
                      :facts (vec (for [i (range n)]
                                    {:e (keyword (str "e" (mod i 64))) :k :note
                                     :v (when-not retracts? (format "bench value %06d-%04d, forty bytes" a i))}))})
          r (c/offer-until-answered! st o)]
      (when-not (= :yes (:answer r)) (throw (ex-info "a bench act was refused" {:answer r}))))))

(defn run
  ([] (run "512" "10" "7"))
  ([rows-per-act acts repeats]
   (let [n (Long/parseLong rows-per-act)
         acts (Long/parseLong acts)
         repeats (Long/parseLong repeats)]
     (with-open [ipc (rtest/create-ipc)]
       (rtest/launch-module! ipc m/Store {:tasks 4 :threads 2 :workers 1})
       (let [st (rx/connect ipc)]
         (c/seed! st)
         (c/offer-until-answered! st (c/make-layer-offer :bench-retracts {:kind :personal :owner :alice}))
         (c/offer-until-answered! st (c/grant-offer st [:alice :bench-retracts :bench-retracts]))
         (println "RUN put-page-bench rows-per-act" n "acts" acts "repeats" repeats "cap" reads/max-put-rows
                  "machine" (.availableProcessors (Runtime/getRuntime)) "cores," (System/getProperty "os.name")
                  (System/getProperty "java.version"))
         (doseq [[layer retracts? what] [[:bench-retracts true "personal, retracts (no seal: the open reads nothing)"]
                                         [:alice false "personal, sealed notes (lock rows)"]
                                         [:alice-agent false "agent, sealed notes (locks in the record)"]]]
           (let [t0 (System/nanoTime)]
             (fill! st layer n acts retracts?)
             (println "FILLED" layer (* n acts) "facts in" (format "%.0f ms" (ms t0))))
           (let [pages (vec (for [_ (range repeats)]
                              (let [t0 (System/nanoTime)
                                    r (rx/index-op! st {:layer layer :op :rebuild-put :after nil :acts 256})]
                                {:ms (ms t0) :acts (:acts r)})))
                 page-acts (:acts (first pages))
                 ;; the seed's making and grant acts sort among the bench's, a handful of rows
                 rows (* n (min page-acts (quot (+ reads/max-put-rows (dec n)) n)))
                 med (median (map :ms pages))]
             (println "PAGE" layer "|" what "| acts in the page" page-acts "| rows about" rows
                      "| ms median" (format "%.1f" med) "max" (format "%.1f" (apply max (map :ms pages)))
                      "| us per row" (format "%.1f" (/ (* 1000 med) (max 1 rows)))
                      "| all" (mapv #(format "%.0f" (:ms %)) pages)))))))))
