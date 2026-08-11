(ns app.client.substrate.region-rungs)

(def admission-divisors [1 2 4 8])

(defn- lease-shadow?
  [lease]
  (if (contains? lease :shadow?)
    (boolean (:shadow? lease))
    (boolean (:shadow lease))))

(defn- candidate
  [{region-id :region/id
    [desired-width desired-height] :desired-size
    :keys [shadow? held-lease reserved-bytes budget-cap-bytes
           quantize lease-bytes]}
   divisor]
  (let [width (quantize (/ (double desired-width) divisor))
        height (quantize (/ (double desired-height) divisor))
        key [region-id width height]
        candidate-bytes (lease-bytes width height shadow?)
        held? (and held-lease (not (:refused? held-lease)))
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
  "Choose the sharpest admission rung that fits the supplied physical pool
   snapshot. quantize and lease-bytes are injected so the compositor and JVM
   tests execute the same arithmetic."
  [{region-id :region/id :keys [desired-size] :as request}]
  (let [desired (candidate request 1)]
    (loop [[divisor & remaining] admission-divisors
           evaluated []]
      (if divisor
        (let [row (if (= 1 divisor) desired (candidate request divisor))
              evaluated (conj evaluated divisor)]
          (if (:admitted? row)
            (let [receipt {:region/id region-id
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
               :rung-receipt (when (> divisor 1) receipt)})
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
