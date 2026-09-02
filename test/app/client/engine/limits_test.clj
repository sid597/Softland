(ns app.client.engine.limits-test
  (:require [app.client.engine.limits :as limits]
            [clojure.test :refer [deftest is testing]]))

(deftest texture-byte-pricing-tripwire
  (testing "the shared format table prices representative color, curve, band, and depth formats"
    (is (= 16 (limits/texture-bytes "rgba8unorm" 2 2 1 1)))
    (is (= 32 (limits/texture-bytes "rgba16float" 2 2 1 1)))
    (is (= 16 (limits/texture-bytes "rg16uint" 2 2 1 1)))
    (is (= 16 (limits/texture-bytes "depth24plus" 2 2 1 1))))
  (testing "all mip levels and samples are included"
    (is (= 1398100
           (limits/texture-bytes "rgba8unorm" 512 512 10 1)))
    (is (= (* 4 1398100)
           (limits/texture-bytes "rgba8unorm" 512 512 10 4))))
  (testing "an unknown format has no guessed price"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Texture format lacks byte pricing"
                          (limits/texture-bytes "unknown" 1 1 1 1)))))
