(ns app.client.engine.rungs
  "Choose an offscreen resolution within a supplied budget.

   Input: desired region size, held lease, reserved/cap bytes, and injected
   quantization/pricing functions. Output: admission result with selected
   size, divisor, and accounting. No GPU work or retained state. It tries
   divisors [1 2 4 8] in order. This file chooses; the compositor allocates.

   Folder map: README.md.")

(def admission-divisors [1 2 4 8])

(defn- lease-shadow?
  "Lease → boolean read from :shadow?.

   Current limitation: physical compositor leases carry :shadow instead. A
   same-size already-shadowed lease can still have zero additional byte
   cost; this shape mismatch alone does not imply a wrong admission."
  [lease]
  (boolean (:shadow? lease)))

(defn- candidate
  "Request and divisor → priced candidate including admission boolean.

   Quantizes size, credits a replaced lease, prices shadow addition. Correct
   shadow accounting depends on the expected held-lease shape."
  [{region-id :region/id
    [desired-width desired-height] :desired-size
    :keys [shadow? held-lease reserved-bytes budget-cap-bytes
           quantize lease-bytes]}
   divisor]
  (let [width (quantize (/ (double desired-width) divisor))
        height (quantize (/ (double desired-height) divisor))
        key [region-id width height]
        candidate-bytes (lease-bytes width height shadow?)
        held? (and held-lease (not (:rejected? held-lease)))
        held-key (when held? (:key held-lease))
        same-key? (= key held-key)
        key-crossing? (and held? (not same-key?))
        self-credit (if key-crossing? (:bytes held-lease 0) 0)
        ;; Reusing the held key needs only newly-added physical storage. A
        ;; shadow removal allocates nothing and its deferred bytes remain in
        ;; reserved-bytes; a shadow addition prices only the new shadow target.
        same-key-extra (if (and same-key?
                                shadow?
                                (not (lease-shadow? held-lease)))
                         (max 0 (- candidate-bytes (:bytes held-lease 0)))
                         0)
        projected-reserved-bytes
        (if same-key?
          (+ reserved-bytes same-key-extra)
          (+ (- reserved-bytes self-credit) candidate-bytes))]
    {:region/id region-id
     :key key
     :size [width height]
     :divisor divisor
     :candidate-bytes candidate-bytes
     :reserved-bytes reserved-bytes
     :projected-reserved-bytes projected-reserved-bytes
     :budget-cap-bytes budget-cap-bytes
     :self-credit self-credit
     :key-crossing? key-crossing?
     :admitted? (<= projected-reserved-bytes budget-cap-bytes)}))

(defn grant
  "Request → first fitting rung or explicit rejection, including evaluated
   divisors.

   Bounded linear search over four choices. Deterministic and independently
   executable without a GPU."
  [{region-id :region/id :keys [desired-size] :as request}]
  (let [desired (candidate request 1)]
    (loop [[divisor & remaining] admission-divisors
           evaluated []]
      (if divisor
        (let [row (if (= 1 divisor) desired (candidate request divisor))
              evaluated (conj evaluated divisor)]
          (if (:admitted? row)
            (let [stats {:region/id region-id
                           :desired-key (:key desired)
                           :granted-key (:key row)
                           :rung-divisor divisor
                           :candidate-bytes (:candidate-bytes row)
                           :reserved-bytes (:reserved-bytes row)
                           :budget-cap-bytes (:budget-cap-bytes row)}]
              {:admitted? true
               :region/id region-id
               :desired-size desired-size
               :desired-key (:key desired)
               :granted-key (:key row)
               :granted-size (:size row)
               :rung-divisor divisor
               :candidate-bytes (:candidate-bytes row)
               :reserved-bytes (:reserved-bytes row)
               :projected-reserved-bytes (:projected-reserved-bytes row)
               :budget-cap-bytes (:budget-cap-bytes row)
               :self-credit (:self-credit row)
               :key-crossing? (:key-crossing? row)
               :evaluated-divisors evaluated
               :rung-stats (when (> divisor 1) stats)})
            (recur remaining evaluated)))
        {:admitted? false
         :region/id region-id
         :desired-size desired-size
         :desired-key (:key desired)
         :granted-key nil
         :rung-divisor nil
         :reserved-bytes (:reserved-bytes desired)
         :budget-cap-bytes (:budget-cap-bytes desired)
         :evaluated-divisors evaluated}))))
