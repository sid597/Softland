(ns app.client.path.value-test
  (:require [app.client.path.value :as v]
            [clojure.test :refer [deftest is testing]]))

(def square
  {:closed? true :start [0.0 0.0]
   :segments [(v/line [10.0 0.0]) (v/line [10.0 10.0]) (v/line [0.0 10.0])]
   :knots [{:id 0 :width 2.0 :pressure 0.2} {:id 1 :width 4.0 :pressure 0.4}
           {:id 2 :width 6.0 :pressure 0.6} {:id 3 :width 8.0 :pressure 0.8}]})

(deftest close-and-reverse-keep-identity
  (let [closed (v/explicit-close square)]
    (is (= 4 (count (:segments closed))) "the closing line is spelled out once")
    (is (= [0.0 0.0] (:p (last (:segments closed)))))
    (is (= 5 (count (:knots closed))) "the first knot is repeated at the close")
    (is (= closed (v/explicit-close closed)) "closing twice adds nothing")
    (let [reversed (v/reverse-subpath square)]
      (is (= [0.0 0.0] (:start reversed)))
      (is (= [[0.0 10.0] [10.0 10.0] [10.0 0.0] [0.0 0.0]] (mapv :p (:segments reversed))))
      (is (= [0 3 2 1 0] (mapv :id (:knots reversed)))))))

(deftest cubic-reversal-swaps-handles
  (let [sp {:closed? false :start [0.0 0.0] :segments [(v/cubic [1.0 0.0] [2.0 1.0] [3.0 1.0])]}
        r (v/reverse-subpath sp)]
    (is (= [3.0 1.0] (:start r)))
    (is (= (v/cubic [2.0 1.0] [1.0 0.0] [0.0 0.0]) (first (:segments r))))))

(deftest bounds-cover-control-points
  (is (= [0.0 0.0 3.0 2.0]
         (v/bbox {:subpaths [{:closed? false :start [0.0 0.0]
                              :segments [(v/cubic [1.0 2.0] [3.0 -0.0] [2.0 1.0])]}]})))
  (is (= [0.0 0.0 0.0 0.0] (v/bbox v/empty-path))))

(deftest flattening-carries-source-correspondence-and-pressure
  (testing "a line flattens to its endpoints, widths interpolated from the knots"
    (let [{:keys [points closed?]} (v/flatten-subpath square 0.1 (fn [sp i] (v/knot-width sp i 1.0)) nil)]
      (is closed?)
      (is (= 4 (count points)) "the repeated close point is dropped")
      (is (= [2.0 4.0 6.0 8.0] (mapv :w points)))
      (is (= [0 0 1 2] (mapv :seg points))
          "the start sits on segment 0; each end knows its segment")))
  (testing "a width function reads the interpolated pressure, not interpolated widths"
    (let [sp {:closed? false :start [0.0 0.0]
              :segments [(v/quad [5.0 5.0] [10.0 0.0])]
              :knots [{:id 0 :pressure 0.0} {:id 1 :pressure 1.0}]}
          {:keys [points]} (v/flatten-subpath sp 0.05 (constantly 1.0) (fn [p _] (* 16.0 p p)))
          mid (nth points (quot (count points) 2))]
      (is (> (count points) 3))
      (is (< (Math/abs (- (:w mid) (* 16.0 (:p mid) (:p mid)))) 1e-9))
      (is (not= (:w mid) (* 16.0 (:p mid))) "16 p² is not 16 p at the midpoint"))))

(deftest curve-steps-follow-the-tolerance
  (let [a [0.0 0.0] c [50.0 100.0] b [100.0 0.0]]
    (is (> (v/quad-steps a c b 0.1) (v/quad-steps a c b 1.0)))
    (is (= 1 (v/quad-steps a [50.0 0.0] b 0.1)) "a flat control point needs one line")))

(deftest map-points-keeps-knots
  (let [moved (v/map-points {:subpaths [square]} (fn [[x y]] [(+ x 1.0) y]))]
    (is (= [1.0 0.0] (:start (first (:subpaths moved)))))
    (is (= (:knots square) (:knots (first (:subpaths moved)))))))
