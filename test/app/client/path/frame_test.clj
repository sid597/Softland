(ns app.client.path.frame-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.frame :as frame]))

(deftest revision-container-regime-key-tripwire
  (let [left fixtures/example-ink-material
        right fixtures/example-shape-material
        ops [{:path/material left :container 0}
             {:path/material right :container 17}]
        key (frame/frame-key ops :floor-default)]
    (is (= [[[:path/ink-fixture :ink/rev-1 0]
             [:path/holed-concave :shape/rev-1 17]]
            :floor-default]
           key))
    (is (not= key
              (frame/frame-key
               [(update-in (first ops) [:path/material :path/revision]
                           (constantly :ink/rev-2))
                (second ops)]
               :floor-default)))
    (is (not= key (frame/frame-key ops :legal-max)))))
