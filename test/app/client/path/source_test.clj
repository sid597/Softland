(ns app.client.path.source-test
  (:require [app.client.path.records :as fixtures]
            [app.client.path.source :as source]
            [app.client.path.value :as v]
            [clojure.test :refer [deftest is testing]]))

(deftest the-pen-keeps-samples-as-knots-and-runs-the-width-rule
  (let [{:keys [path meta]} (source/build (:path/source fixtures/harness-z) (:path/tool fixtures/harness-z))
        sp (first (:subpaths path))]
    (is (= :pen (:kind meta)))
    (is (= :polyline (:spline meta)))
    (is (= 3 (count (:segments sp))))
    (is (every? #(= :line (:kind %)) (:segments sp)))
    (is (= ["s0" "s1" "s2" "s3"] (mapv :id (:knots sp))) "knots keep the sample index")
    (is (= [0.65 0.9 1.0 0.7] (mapv :pressure (:knots sp))))
    (is (= [0.0 90.0 180.0 270.0] (mapv :time (:knots sp))))
    (testing "the default width rule is size * (1 - thinning * (1 - p)) with thinning 0.5"
      (is (= [(* 16 (- 1 (* 0.5 (- 1 0.65)))) (* 16 (- 1 (* 0.5 (- 1 0.9)))) 16.0 (* 16 (- 1 (* 0.5 (- 1 0.7))))]
             (mapv :width (:knots sp)))))
    (testing "an explicit width expression reads only the tool numbers it names"
      (let [{:keys [path meta]} (source/build (:path/source fixtures/harness-z)
                                              {:size 16 :fit :polyline :thinning 0.9 :width [:* [:get :size] [:get :p]]})]
        (is (= #{:size :p} (:width-names meta)))
        (is (= [(* 16 0.65) (* 16 0.9) 16.0 (* 16 0.7)] (mapv :width (:knots (first (:subpaths path))))))))))

(deftest a-fit-decimates-and-a-spline-interpolates
  (let [{:keys [path meta]} (source/build (:path/source fixtures/draw-tool) (:path/tool fixtures/draw-tool))
        sp (first (:subpaths path))]
    (is (= 70 (:samples meta)))
    (is (< (:knots meta) 70) "fit 0.9 drops samples")
    (is (= :catmull-rom (:spline meta)))
    (is (every? #(= :cubic (:kind %)) (:segments sp)))
    (is (= (inc (count (:segments sp))) (count (:knots sp))))
    (testing "the spline passes through every knot"
      (doseq [[i s] (map-indexed vector (:segments sp))]
        (let [k (nth (:knots sp) (inc i))]
          (is (some? (:id k)))
          (is (= (:p s) (v/segment-end sp i))))))
    (testing "the end taper shrinks the last widths to zero"
      (is (zero? (:width (last (:knots sp)))))
      (is (pos? (:width (nth (:knots sp) (quot (count (:knots sp)) 2))))))
    (testing "fit 0 and :polyline keep every sample"
      (is (= 70 (:knots (:meta (source/build (:path/source fixtures/draw-tool) (assoc (:path/tool fixtures/draw-tool) :fit 0))))))
      (is (= :polyline (:spline (:meta (source/build (:path/source fixtures/draw-tool) (assoc (:path/tool fixtures/draw-tool) :fit :polyline)))))))))

(deftest a-malformed-width-is-a-named-error
  (is (thrown? clojure.lang.ExceptionInfo
               (source/build (:path/source fixtures/harness-z) {:size 16 :width [:unknown 1]}))))

(deftest a-rectangle-is-four-lines-and-four-arcs
  (let [{:keys [path meta]} (source/build (:path/source fixtures/border) nil)
        sp (first (:subpaths path))]
    (is (= {:kind :rect :lines 4 :arcs 4} meta))
    (is (:closed? sp))
    (is (= [:line :cubic :line :cubic :line :cubic :line :cubic] (mapv :kind (:segments sp))))
    (is (= 9 (count (:knots sp))))
    (is (= [30.0 36.0] (:start sp)) "the start sits after the first corner's radius")
    (testing "the arc's cubic ends on the circle"
      (let [arc (second (:segments sp))]
        (is (< (Math/abs (- (v/dist (:p arc) [98.0 42.0]) 6.0)) 1e-9))))
    (testing "no radius means four lines"
      (is (= {:kind :rect :lines 4 :arcs 0} (:meta (source/build {:kind :rect :x 0 :y 0 :w 4 :h 3} nil)))))))

(deftest anchors-copy-into-lines-and-cubics
  (let [{:keys [path meta]} (source/build (:path/source fixtures/pen-tool) nil)
        sp (first (:subpaths path))]
    (is (= 3 (:cubics meta)))
    (is (= 1 (:lines meta)) "c has no out handle and d no in handle: a line")
    (is (= ["a" "b" "c" "d" "a"] (mapv :id (:knots sp))))
    (is (= [110.0 30.0] (:c2 (first (:segments sp)))) "b's in handle ends the first cubic")
    (is (= [40.0 30.0] (:c2 (last (:segments sp)))) "a's missing in handle collapses to the anchor"))
  (let [{:keys [meta]} (source/build (:path/source fixtures/holed-concave) nil)]
    (is (= {:kind :anchors :cubics 0 :lines 12} meta))))
