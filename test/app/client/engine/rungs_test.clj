(ns app.client.engine.rungs-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.engine.rungs :as rungs]))

(def mib (* 1024 1024))

(defn- quantize [value]
  (-> (/ (max 1.0 (double value)) 256.0)
      Math/ceil
      (* 256)
      (min 4096)
      int))

(defn- lease-bytes [width height shadow?]
  (+ (* 56 width height)
     (if shadow? (* 16 mib) 0)))

(defn- request
  [& {:as overrides}]
  (merge {:region/id :region/a
          :desired-size [3840 2176]
          :shadow? true
          :held-lease nil
          :reserved-bytes 0
          :budget-cap-bytes (* 512 mib)
          :quantize quantize
          :lease-bytes lease-bytes}
         overrides))

(deftest l1-sharpest-admitted-rung-mints-achieved-receipt
  (let [grant (rungs/grant
               (request :reserved-bytes 104637808))]
    (is (= [1 2] (:evaluated-divisors grant)))
    (is (= 2 (:rung-divisor grant)))
    (is (= [:region/a 3840 2304] (:desired-key grant)))
    (is (= [:region/a 2048 1280] (:granted-key grant)))
    (is (= (lease-bytes 2048 1280 true) (:candidate-bytes grant)))
    (is (= {:region/id :region/a
            :desired-key [:region/a 3840 2304]
            :granted-key [:region/a 2048 1280]
            :rung-divisor 2
            :candidate-bytes (lease-bytes 2048 1280 true)
            :reserved-bytes 104637808
            :budget-cap-bytes (* 512 mib)}
           (:rung-receipt grant)))))

(deftest l3-held-rung-is-never-released-to-probe-a-sharper-one
  (let [held {:key [:region/a 768 768]
              :bytes (lease-bytes 768 768 false)
              :shadow nil :refused? false}
        grant (rungs/grant
               (request :desired-size [2560 2560]
                        :shadow? false
                        :held-lease held
                        :reserved-bytes (* 500 mib)
                        :budget-cap-bytes (* 512 mib)))]
    (is (= 4 (:rung-divisor grant)))
    (is (= (:key held) (:granted-key grant)))
    (is (zero? (:self-credit grant)))
    (is (= (* 500 mib) (:projected-reserved-bytes grant)))))

(deftest l3-key-crossing-credits-only-the-regions-synchronous-release
  (let [held {:key [:region/a 512 512] :bytes (* 20 mib)
              :shadow nil :refused? false}
        grant (rungs/grant
               (request :desired-size [768 768]
                        :shadow? false
                        :held-lease held
                        :reserved-bytes (* 500 mib)
                        :budget-cap-bytes (* 512 mib)))]
    (is (= 1 (:rung-divisor grant)))
    (is (= (* 20 mib) (:self-credit grant)))
    (is (= (+ (* 480 mib) (lease-bytes 768 768 false))
           (:projected-reserved-bytes grant)))))

(deftest same-key-shadow-transitions-price-only-physical-new-bytes
  (let [base-bytes (lease-bytes 512 512 false)
        without-shadow {:key [:region/a 512 512] :bytes base-bytes
                        :shadow nil :refused? false}
        with-shadow (assoc without-shadow
                           :bytes (lease-bytes 512 512 true)
                           :shadow :held)
        add (rungs/grant
             (request :desired-size [512 512]
                      :shadow? true :held-lease without-shadow
                      :reserved-bytes (* 100 mib)))
        remove (rungs/grant
                (request :desired-size [512 512]
                         :shadow? false :held-lease with-shadow
                         :reserved-bytes (* 100 mib)))]
    (testing "shadow-on prices only its new target"
      (is (= (+ (* 100 mib) (* 16 mib))
             (:projected-reserved-bytes add))))
    (testing "shadow-off never credits deferred destruction"
      (is (= (* 100 mib) (:projected-reserved-bytes remove)))
      (is (zero? (:self-credit remove))))))

(deftest l5-floor-is-frozen-and-terminates-after-four-candidates
  (let [no-shadow (rungs/grant
                   (request :desired-size [4096 4096]
                            :shadow? false
                            :budget-cap-bytes (* 3 mib)))
        shadowed (rungs/grant
                  (request :desired-size [640 360]
                           :shadow? true
                           :budget-cap-bytes (* 5 mib)))]
    (is (= [1 2 4 8] rungs/admission-divisors))
    (doseq [grant [no-shadow shadowed]]
      (is (false? (:admitted? grant)))
      (is (= [1 2 4 8] (:evaluated-divisors grant)))
      (is (nil? (:granted-key grant)))
      (is (nil? (:rung-divisor grant))))))

(deftest sequential-grants-consume-the-then-current-pool-snapshot
  (let [first-grant (rungs/grant
                     (request :region/id :region/a
                              :desired-size [1536 1024]
                              :shadow? false
                              :budget-cap-bytes (* 140 mib)))
        second-grant (rungs/grant
                      (request :region/id :region/b
                               :desired-size [1536 1024]
                               :shadow? false
                               :reserved-bytes
                               (:projected-reserved-bytes first-grant)
                               :budget-cap-bytes (* 140 mib)))]
    (is (= 1 (:rung-divisor first-grant)))
    (is (= 2 (:rung-divisor second-grant)))
    (is (= [:region/b 768 512] (:granted-key second-grant)))))
