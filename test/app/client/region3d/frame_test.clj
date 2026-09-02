(ns app.client.region3d.frame-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.region3d.frame :as frame]))

(def draw-item
  {:region/material {:region/id :region/a
                     :region/revision "r1"
                     :region/rect {:x 10.0 :y 20.0 :w 100.0 :h 80.0}}
   :container 17})

(deftest region-key-is-the-row-revision-plus-projection-stamps
  (let [key (frame/region-key draw-item 1.0 2.0 "session-4")]
    (is (= [:region/a "r1" 17 1.0 2.0 "session-4"] key))
    (testing "deep content is not the gate"
      (is (= key
             (frame/region-key
              (assoc-in draw-item [:region/material :region/rect :w] 999.0)
              1.0 2.0 "session-4"))))
    (testing "every owned revision or projection stamp changes the key"
      (doseq [changed [(assoc-in draw-item [:region/material :region/revision] "r2")
                       (assoc draw-item :container 18)]]
        (is (not= key (frame/region-key changed 1.0 2.0 "session-4"))))
      (is (not= key (frame/region-key draw-item 1.1 2.0 "session-4")))
      (is (not= key (frame/region-key draw-item 1.0 1.0 "session-4")))
      (is (not= key (frame/region-key draw-item 1.0 2.0 "session-5"))))))

(deftest frame-key-preserves-draw-item-order
  (let [second-draw-item (-> draw-item
                      (assoc-in [:region/material :region/id] :region/b)
                      (assoc-in [:region/material :region/revision] "b1"))]
    (is (= [(frame/region-key draw-item 1.0 1.0 9)
            (frame/region-key second-draw-item 1.0 1.0 9)]
           (frame/frame-key [draw-item second-draw-item] 1.0 1.0 9)))))
