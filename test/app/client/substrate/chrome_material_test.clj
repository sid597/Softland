(ns app.client.substrate.chrome-material-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.chrome-material :as chrome]))

(def point-quad
  {:anchors [[10.0 20.0] [10.0 20.0] [10.0 20.0] [10.0 20.0]]
   :offsets-px [[-5.0 -5.0] [5.0 -5.0] [5.0 5.0] [-5.0 5.0]]
   :color [1.0 1.0 1.0 1.0]})

(def line-quad
  {:anchors [[10.0 20.0] [30.0 20.0] [30.0 20.0] [10.0 20.0]]
   :offsets-px [[0.0 -1.5] [0.0 -1.5] [0.0 1.5] [0.0 1.5]]
   :color [0.2 0.7 1.0 0.8]})

(deftest neutral-quad-grammar-and-packing-tripwire
  (let [material [point-quad line-quad]
        vertices (chrome/material-vertices material)]
    (is (= 12 (count vertices)))
    (is (= chrome/vertex-words
           (count (chrome/vertex-values (first vertices) 4))))
    (is (= 9 chrome/vertex-words))
    (is (= (* 4 chrome/vertex-words) chrome/vertex-stride))
    (testing "only exact finite quad values are admitted"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"unknown or missing"
                            (chrome/material-vertices
                             [(assoc point-quad :opaque-identity :forbidden)])))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo #"four finite"
                            (chrome/material-vertices
                             [(assoc point-quad :anchors [[10.0 20.0]])]))))))

(deftest anchored-and-screen-metric-tripwire
  (let [effective {:affine [2.0 0.0 0.0 2.0 30.0 40.0] :flags 0}
        anchor {:x 10.0 :y 20.0 :w 40.0 :h 30.0}
        stations [0.01 0.1 1.0 8.0 100.0 1000.0]
        point-rects
        (mapv #(chrome/chrome-screen-rect
                {:x 10.0 :y 20.0 :w 0.0 :h 0.0}
                effective {:x 7.0 :y 11.0 :zoom %}
                {:x -5.0 :y -5.0 :w 10.0 :h 10.0})
              stations)
        anchored-rects
        (mapv #(chrome/chrome-screen-rect
                anchor effective {:x 7.0 :y 11.0 :zoom %}
                {:x -0.5 :y -0.5 :w 1.0 :h 1.0})
              stations)]
    (is (every? #(= [10.0 10.0] [(:w %) (:h %)]) point-rects))
    (is (= [1.8000000000000007 81.0 8001.0 80001.0]
           (mapv :w [(first anchored-rects) (nth anchored-rects 2)
                     (nth anchored-rects 4) (last anchored-rects)])))
    (is (= {:x 2.5 :y 6.800000000000001 :w 10.0 :h 10.0}
           (first point-rects)))))
