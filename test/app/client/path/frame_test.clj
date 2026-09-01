(ns app.client.path.frame-test
  (:require [clojure.test :refer [deftest is]]
            [app.client.engine.placement :as placement]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.frame :as frame]))

(defn- error-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

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

(deftest effective-container-slot-tripwire
  (let [registry (-> (placement/empty-registry)
                     (placement/add-container
                      17 {:parent 0 :affine [0.5 0.0 0.0 0.5 40.0 20.0]}))
        effective (placement/effective registry)]
    (is (= 0 (frame/slot effective 0)))
    (is (= 1 (frame/slot effective 17)))
    (is (= :path/unknown-container
           (:error-type (error-data #(frame/slot effective 99)))))))
