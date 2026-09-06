(ns app.client.path.nib-test
  (:require [app.client.path.component :as component]
            [app.client.path.nib :as nib]
            [app.client.path.pack :as pack]
            [app.client.path.stroke :as stroke]
            [clojure.test :refer [deftest is testing]]))

(defn inside? [path [x y]]
  (pack/inside? (:pack (pack/pack-region path 0.001 {})) :nonzero x y))

(deftest nonlinear-union-resolves-the-radius-between-line-endpoints
  ;; Judge F2's independent oracle leaves (10,5) at least 1.18 units
  ;; outside every disc of the continuous sweep (20t,0), radius 10t².
  (let [path {:subpaths [{:start [0.0 0.0] :closed? false
                          :segments [{:kind :line :p [20.0 0.0]}]
                          :knots [{:id 0 :pressure 0.0} {:id 1 :pressure 1.0}]}]}
        result (stroke/envelope path (component/stroke-defaults {})
                                {:tip :nib :tolerance 0.01 :width-fn (fn [p _] (* 20.0 p p))})]
    (is (not (inside? (:path result) [10.0 5.0])))
    (is (inside? (:path result) [20.0 0.0]))
    (is (> (count (first (:polylines result))) 2))
    (doseq [point (first (:polylines result))]
      (is (< (Math/abs (- (:r point) (* 10.0 (:p point) (:p point)))) 1e-12)))))

(deftest a-containing-disc-is-kept-whole
  (let [path {:subpaths [{:start [10.0 50.0] :closed? false
                          :segments [{:kind :line :p [12.0 50.0]}]
                          :knots [{:id 0 :width 1.6} {:id 1 :width 16.0}]}]}
        skin (:path (stroke/envelope path (component/stroke-defaults {}) {:tip :nib :tolerance 0.01}))]
    (doseq [p [[19.0 50.0] [18.5 50.0] [12.0 57.0] [12.0 43.5] [5.0 50.0] [4.5 50.0]]]
      (is (inside? skin p) (str "judge A2 " p)))
    (is (not (inside? skin [20.1 50.0]))))
  (testing "equal centers retain the larger radius, in either order"
    (doseq [[a b] [[1.0 8.0] [8.0 1.0]]]
      (let [path {:subpaths [{:start [12.0 50.0] :closed? false
                              :segments [{:kind :line :p [12.0 50.0]}]
                              :knots [{:id 0 :width (* 2.0 a)} {:id 1 :width (* 2.0 b)}]}]}
            skin (:path (stroke/envelope path (component/stroke-defaults {}) {:tip :nib :tolerance 0.001}))]
        (is (inside? skin [19.0 50.0]))
        (is (not (inside? skin [20.1 50.0])))))))
