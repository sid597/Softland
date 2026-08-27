(ns app.client.workspace.containers-test
  (:require [app.client.workspace.containers :as containers]
            [clojure.test :refer [deftest is]]))

(deftest anchored-and-screen-metric-tripwire
  (let [effective {:affine [2.0 0.0 0.0 2.0 30.0 40.0] :flags 0}
        anchor {:x 10.0 :y 20.0 :w 40.0 :h 30.0}
        stations [0.01 0.1 1.0 8.0 100.0 1000.0]
        point-rects
        (mapv #(containers/anchored-screen-rect
                {:x 10.0 :y 20.0 :w 0.0 :h 0.0}
                effective {:x 7.0 :y 11.0 :zoom %}
                {:x -5.0 :y -5.0 :w 10.0 :h 10.0})
              stations)
        anchored-rects
        (mapv #(containers/anchored-screen-rect
                anchor effective {:x 7.0 :y 11.0 :zoom %}
                {:x -0.5 :y -0.5 :w 1.0 :h 1.0})
              stations)]
    (is (every? #(= [10.0 10.0] [(:w %) (:h %)]) point-rects))
    (is (= [1.8000000000000007 81.0 8001.0 80001.0]
           (mapv :w [(first anchored-rects) (nth anchored-rects 2)
                     (nth anchored-rects 4) (last anchored-rects)])))
    (is (= {:x 2.5 :y 6.800000000000001 :w 10.0 :h 10.0}
           (first point-rects)))))
