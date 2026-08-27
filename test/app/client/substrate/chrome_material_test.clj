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
