(ns render-probe.differ-bench
  "JVM half of the NORTH.md §9 probe: what does it cost to MINT the diffs?

   hyperfiddle.incseq/->seq-differ is the exact stateful differ Electric's
   e/for-by runs server-side per recompute; this measures it raw, without the
   Electric DAG on top (declared floor, not ceiling). Also records a
   payload-size proxy per diff (pr-str byte length — Electric's wire codec
   differs; sizes are indicative only).

   Run from the probe scratch dir (its deps.edn carries the classpath):
     clj -M -m render-probe.differ-bench > differ-results.edn"
  (:require [hyperfiddle.incseq :as i]
            [render-probe.mixes :as mixes]))

(def scales [100 1000 10000])
(def warmup-steps 5)
(def measure-steps 60)
(def cell-time-cap-ms 30000)

(defn- pct [sorted-v p]
  (nth sorted-v (min (dec (count sorted-v))
                     (long (Math/floor (* p (count sorted-v)))))))

(defn- summarize [xs]
  (if (empty? xs)
    {:n 0}
    (let [s (vec (sort xs))]
      {:p50 (pct s 0.5) :p95 (pct s 0.95) :max (peek s) :n (count s)})))

(defn- diff-ops [d]
  {:grow (:grow d) :shrink (:shrink d)
   :perm (count (:permutation d)) :change (count (:change d))})

(defn bench-cell [n mix]
  (let [{:keys [init step]} (mixes/make-mix mix n)
        differ (i/->seq-differ :id)
        cold-t0 (System/nanoTime)
        _cold (differ init)
        cold-ms (/ (- (System/nanoTime) cold-t0) 1e6)
        cell-t0 (System/currentTimeMillis)]
    ;; all-* collect every step (incl. warmup) so a time-capped cell still
    ;; reports honest numbers, flagged :warmup-included.
    (loop [frame 0, coll init
           mints [], bytes [], all-mints [], all-bytes [], ops nil]
      (if (or (= frame (+ warmup-steps measure-steps))
              (> (- (System/currentTimeMillis) cell-t0) cell-time-cap-ms))
        (let [warm? (empty? mints)]
          {:n n :mix mix
           :cold-mint-ms cold-ms
           :mint-ms (summarize (if warm? all-mints mints))
           :payload-bytes (summarize (if warm? all-bytes bytes))
           :ops ops
           :warmup-included? warm?
           :truncated? (< (count all-mints) (+ warmup-steps measure-steps))})
        (let [coll' (step coll frame)
              t0 (System/nanoTime)
              d (differ coll')
              ms (/ (- (System/nanoTime) t0) 1e6)
              measured? (>= frame warmup-steps)]
          (recur (inc frame) coll'
                 (if measured? (conj mints ms) mints)
                 (if measured? (conj bytes (count (pr-str d))) bytes)
                 (conj all-mints ms)
                 (conj all-bytes (count (pr-str d)))
                 (diff-ops d)))))))

(defn -main [& _]
  ;; one EDN line per cell, flushed immediately — a killed run keeps its data
  (prn {:probe :differ-bench
        :differ 'hyperfiddle.incseq/->seq-differ
        :electric-jar "electric-v3-alpha-20260325.114002-44"
        :warmup warmup-steps :steps measure-steps})
  (flush)
  (doseq [n scales
          mix mixes/all-mixes
          ;; full churn only makes sense above toy scale
          :when (not (and (= mix :churn-100) (= n 100)))]
    (let [r (bench-cell n mix)]
      (prn r)
      (flush)
      (binding [*out* *err*]
        (println "done" n mix (:mint-ms r))))))
