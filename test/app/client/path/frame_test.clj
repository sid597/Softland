(ns app.client.path.frame-test
  (:require [app.client.path.fixtures :as fixtures]
            [app.client.path.frame :as frame]
            [clojure.test :refer [deftest is testing]]))

(deftest the-frame-key-carries-only-the-view-facets-a-record-declares
  (let [items [{:path/material fixtures/harness-z :container 0}]
        k (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]})]
    (is (= [[[:fixture/z 1 0]] 1 nil nil] k))
    (is (= k (frame/frame-key items {:zoom 3.9 :pan [7.0 7.0]})) "a pan or a zoom inside the bucket changes nothing")
    (is (not= k (frame/frame-key items {:zoom 4.0 :pan [1.0 2.0]})) "crossing a bucket repacks")
    (is (not= k (frame/frame-key [{:path/material (assoc fixtures/harness-z :path/revision 2) :container 0}] {:zoom 3.0 :pan [1.0 2.0]}))))
  (testing "a device-unit width carries the exact scale, snapping carries the pan"
    (let [items [{:path/material fixtures/border :container 17}]]
      (is (= [[[:fixture/border 1 17]] 1 3.0 [1.0 2.0]] (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]})))
      (is (not= (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]}) (frame/frame-key items {:zoom 3.0 :pan [1.5 2.0]}))))))

(deftest pack-keys-see-the-outline-and-the-bucket
  (let [r {:path {:subpaths []} :rule :nonzero :kind :fill :paint :fill}]
    (is (= (frame/pack-key r 2) (frame/pack-key (assoc r :paint :stroke) 2)) "paint kind is not geometry")
    (is (not= (frame/pack-key r 2) (frame/pack-key r 3)))
    (is (not= (frame/pack-key r 2) (frame/pack-key (assoc r :rule :even-odd) 2)))))

(deftest item-view-composes-zoom-and-group-scale
  (is (= {:scale 6.0 :pan [1.0 2.0]} (frame/item-view {:zoom 3.0 :pan [1.0 2.0]} fixtures/border 2.0)))
  (is (= {:scale 3.0 :pan [0.0 0.0]} (frame/item-view {:zoom 3.0} fixtures/border nil))))
