(ns render-probe.mixes
  "Synthetic trail-view-shaped collections + mutation mixes for the
   Electric-at-10^4 probe (docs/current-mental-model/build/render-north/
   NORTH.md §9, pre-registered).

   Pure and deterministic: a Park-Miller LCG keyed off the frame number, so
   the JVM producer bench and the browser consumer bench see byte-identical
   streams. Rows are rect-grade drawables in the buffer-pool's 28-float pack
   vocabulary, laid out lane-wise like trail cards. Every entity carries a
   stable string address in :id — the differ keys on it (N3)."
  (:refer-clojure :exclude [rand]))

;; Park-Miller: multiplier small enough that (* s 48271) stays exact in
;; JS doubles (< 2^53), so CLJ and CLJS produce identical sequences.
(defn- lcg [s] (mod (* s 48271) 2147483647))

(defn- lcg-picks
  "k distinct indices in [0, n), deterministic in seed. k must be <= n."
  [seed n k]
  (loop [s (lcg (+ 1 (mod seed 2147483646)))
         acc (transient #{})]
    (if (= (count acc) k)
      (persistent! acc)
      (recur (lcg s) (conj! acc (mod s n))))))

(def lanes 40)
(def card-w 240)
(def card-h 72)

(defn row
  "One drawable entity. :id is the stable address; :i the numeric index it
   was minted from; :gen bumps on every change so value inequality is real.
   Geometry keys match app.client.substrate.webgpu.buffer-pool/pack-rect."
  [i gen]
  (let [lane (mod i lanes)
        line (quot i lanes)]
    {:id (str "addr-" i)
     :i i
     :gen gen
     :x (* lane (+ card-w 20))
     :y (+ (* line (+ card-h 18)) (mod gen 7))
     :w card-w
     :h card-h
     :r 0.12
     :g (/ (mod (+ i gen) 255) 255.0)
     :b 0.35
     :a 1.0
     :radius 6.0}))

(defn init-rows [n] (mapv #(row % 0) (range n)))

(defn- change-step
  "Mutate k rows in place (same ids, new :gen -> new :y jitter + :g color)."
  [k]
  (fn [coll frame]
    (let [n (count coll)
          k (min k n)
          picks (lcg-picks (* 7919 (inc frame)) n k)]
      (persistent!
        (reduce (fn [c j] (assoc! c j (row (:i (nth coll j)) (inc frame))))
                (transient coll) picks)))))

(defn- rotate-step
  "Rotate the collection left by n/4: same ids, same values, big permutation.
   The differ emits a pure :permutation diff — pre-registered trap 1 food."
  [coll _frame]
  (let [n (count coll)
        rot (max 1 (quot n 4))]
    (into (subvec coll rot) (subvec coll 0 rot))))

(defn- churn-step
  "Feed-window slide: drop the k oldest rows, append k rows with FRESH ids.
   k = n is full ID churn (pre-registered trap 3: everything collapses to
   grow+shrink)."
  [pct]
  (fn [coll frame]
    (let [n (count coll)
          k (max 1 (quot (* n pct) 100))
          base (+ (:i (peek coll)) 1)]
      (into (subvec coll (min k n))
            (map #(row (+ base %) (inc frame)) (range k))))))

(defn- append-step
  "Steady ingest: append `k` fresh rows per frame."
  [k]
  (fn [coll frame]
    (let [base (if (seq coll) (inc (:i (peek coll))) 0)]
      (into coll (map #(row (+ base %) (inc frame)) (range k))))))

(defn make-mix
  "Returns {:init coll0 :step (fn [coll frame] coll')}.
   frame counts from 0 = first measured application."
  [mix n]
  (case mix
    ;; first frame loads the full n from empty; rest are idle (empty diffs).
    :cold-load   {:init [] :step (fn [coll frame]
                                   (if (zero? frame) (init-rows n) coll))}
    :change-1    {:init (init-rows n) :step (change-step (max 1 (quot n 100)))}
    :change-10   {:init (init-rows n) :step (change-step (max 1 (quot n 10)))}
    :permute-rot {:init (init-rows n) :step rotate-step}
    :churn-10    {:init (init-rows n) :step (churn-step 10)}
    :churn-100   {:init (init-rows n) :step (churn-step 100)}
    :grow-append {:init (init-rows n) :step (append-step 10)}))

(def all-mixes [:cold-load :change-1 :change-10 :permute-rot
                :churn-10 :churn-100 :grow-append])
