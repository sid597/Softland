(ns app.client.path.frame-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.frame :as frame]))

(deftest revision-group-lod-key-tripwire
  (let [left fixtures/example-ink-component
        right fixtures/example-shape-component
        draw-items [{:path/material left :container 0}
             {:path/material right :container 17}]
        key (frame/frame-key draw-items :engine-default)]
    (is (= [[[:path/ink-fixture :ink/rev-1 0]
             [:path/holed-concave :shape/rev-1 17]]
            :engine-default]
           key))
    (is (not= key
              (frame/frame-key
               [(update-in (first draw-items) [:path/material :path/revision]
                           (constantly :ink/rev-2))
                (second draw-items)]
               :engine-default)))
    (is (not= key (frame/frame-key draw-items :legal-max)))))
