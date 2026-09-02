(ns app.client.engine.color-test
  (:require [app.client.engine.color :as color]
            [clojure.test :refer [deftest is]]))

(deftest srgb-linear-transfer-tripwire
  (is (= 0.0 (color/srgb-channel->linear 0.0)))
  (is (= (/ 0.04045 12.92)
         (color/srgb-channel->linear 0.04045)))
  (is (= (* 0.0031308 12.92)
         (color/linear->srgb-channel 0.0031308)))
  (is (= 1.0 (color/srgb-channel->linear 1.0)))
  (is (= (- 1.055 0.055) (color/linear->srgb-channel 1.0)))
  (is (< (Math/abs
          (- 0.21404114048223255
             (color/srgb-channel->linear 0.5)))
         1.0e-15))
  (doseq [value [0.0 0.25 0.5 0.75 1.0]]
    (is (< (Math/abs
            (- value
               (color/linear->srgb-channel
                (color/srgb-channel->linear value))))
           1.0e-12))))
