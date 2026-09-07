(ns app.client.region3d.surface-region-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.region3d.support :as support]
            [app.client.region3d.records :as records]
            [app.client.region3d.surface-region :as region]
            [app.client.engine.value-bytes :as vb]))

(defn cap [radius] (region/construct records/host {:id "G" :revision 0}
                                    {:seed [Math/PI (/ Math/PI 3)] :radius radius}))
(defn near [a b] (< (Math/abs (- a b)) 1.0e-8))

(deftest reach-is-intrinsic-and-retained-as-data
  (let [r (cap 150) q (support/unit [(* 1.5 Math/PI) (/ Math/PI 3)])
        loaded (vb/decode (vb/encode r))]
    (is (near 144.54684956268315 (region/distance r q)))
    (is (near 67433.94227378976 (:area r)))
    (is (region/member r q))
    (is (not (region/member (cap 140) q)))
    (is (= 150 (:radius r)))
    (is (= r loaded))
    (is (region/member loaded q))
    (is (= #{:north :west :east} (set (:pieces r))))
    (is (not (region/member r (support/unit [Math/PI (- (/ Math/PI 3) 0.76)]))))))

(deftest radius-saturates-before-trigonometry-can-cycle
  (let [antipode (support/scale (support/unit [Math/PI (/ Math/PI 3)]) -1)
        R (:R records/host)]
    (is (not (region/member (cap 628) antipode)))
    (is (near 502654.50582273136 (:area (cap 628))))
    (doseq [radius [(* Math/PI R) 629 (* 2 Math/PI R)]]
      (let [r (cap radius)]
        (is (:saturated? r))
        (is (region/member r antipode))
        (is (near (* 4 Math/PI R R) (:area r)))
        (is (= [[(- R) R] [(- R) R] [(- R) R]] (:bounds r)))
        (is (every? (fn [[a b]] (<= a b)) (:bounds r))))))
  (is (region/member (cap 0) (support/unit [Math/PI (/ Math/PI 3)])))
  (is (not (region/member (cap 0) (support/unit [Math/PI 1]))))
  (is (= :radius (try (cap -1) (catch Exception e (:reason (ex-data e)))))))

(deftest charts-describe-the-same-points-with-one-piece-owner
  (doseq [t [0 0.41 0.65 0.7 1] chart [:S0 :S1 :S2]]
    (let [p (support/curve-point records/host "kA/arc-AB" t)
          uv (support/point->chart records/host chart p)
          q (support/chart->point records/host chart uv)]
      (is (< (support/distance records/host p q) 1.0e-10))))
  (is (= :north (support/piece [0 1 0])))
  (is (= :east (support/piece (support/unit [Math/PI 0]))))
  (is (= :west (support/piece (support/unit [(- Math/PI) 0]))))
  (let [g (support/metric records/host :S2 (/ Math/PI 3))]
    (is (every? true? (map near (mapcat identity g) [1 -0.25 -0.25 1.0625])))))

(deftest chart-piece-bounds-are-conservative-at-a-cut
  (let [r (region/construct records/host {:id "G" :revision 0} {:seed [0 0] :radius 0})]
    (is (= #{:west :east} (set (:pieces r))))
    (is (= :east (support/piece (:point r))))
    (is (region/member r (:point r)))
    (is (not (region/member r (support/unit [0.001 0]))))))
