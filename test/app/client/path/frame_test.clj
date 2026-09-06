(ns app.client.path.frame-test
  (:require [app.client.engine.transform :as transform]
            [app.client.path.records :as fixtures]
            [app.client.path.frame :as frame]
            [clojure.test :refer [deftest is testing]]))

(def transforms
  "Group 0 at the root, 17 scaled by half and moved, 18 in screen space."
  (transform/world-transforms
   (-> (transform/empty-registry)
       (transform/add-group 17 {:parent 0 :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
       (transform/add-group 18 {:parent 0 :camera :screen}))))

(deftest the-frame-key-carries-only-the-view-facets-a-record-declares
  (let [items [{:path/material fixtures/harness-z :container 0}]
        k (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]} transforms)]
    (is (= [[:fixture/z 1 0 0 1 nil nil]] k))
    (is (= k (frame/frame-key items {:zoom 3.9 :pan [7.0 7.0]} transforms)) "a pan or a zoom inside the bucket changes nothing")
    (is (not= k (frame/frame-key items {:zoom 4.0 :pan [1.0 2.0]} transforms)) "crossing a bucket repacks")
    (is (not= k (frame/frame-key [{:path/material (assoc fixtures/harness-z :path/revision 2) :container 0}] {:zoom 3.0 :pan [1.0 2.0]} transforms))))
  (testing "a device-unit width carries the exact scale, snapping carries the pan"
    (let [items [{:path/material fixtures/border :container 17}]]
      (is (= [[:fixture/border 1 17 1 0 1.5 [121.0 62.0]]] (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]} transforms))
          "the group's half scale and its origin in device pixels")
      (is (not= (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]} transforms) (frame/frame-key items {:zoom 3.0 :pan [1.5 2.0]} transforms)))))
  (testing "a group's translation is not in the key; its scale is"
    (let [items [{:path/material fixtures/harness-z :container 17}]
          view {:zoom 1.0 :pan [0.0 0.0]}
          moved (assoc-in transforms [17 :affine 4] 44.0)
          scaled (assoc-in transforms [17 :affine] [2.0 0.0 0.0 2.0 40.0 20.0])]
      (is (= (frame/frame-key items view transforms) (frame/frame-key items view moved)))
      (is (not= (frame/frame-key items view transforms) (frame/frame-key items view scaled)) "half to double crosses a bucket")))
  (testing "a screen-space group ignores the camera"
    (let [items [{:path/material fixtures/border :container 18}]]
      (is (= (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]} transforms)
             (frame/frame-key items {:zoom 0.5 :pan [9.0 9.0]} transforms))))))

(deftest pack-keys-see-the-outline-and-the-bucket
  (let [r {:path {:subpaths []} :rule :nonzero :kind :fill :paint :fill}]
    (is (= (frame/pack-key r 2) (frame/pack-key (assoc r :paint :stroke) 2)) "paint kind is not geometry")
    (is (not= (frame/pack-key r 2) (frame/pack-key r 3)))
    (is (not= (frame/pack-key r 2) (frame/pack-key (assoc r :rule :even-odd) 2)))))

(deftest item-view-composes-zoom-and-group-scale
  (is (= {:scale 1.5 :pan [121.0 62.0]} (frame/item-view {:zoom 3.0 :pan [1.0 2.0]} (get transforms 17))))
  (is (= {:scale 3.0 :pan [0.0 0.0]} (frame/item-view {:zoom 3.0} nil)) "no transform is the identity at the root")
  (is (= {:scale 1.0 :pan [0.0 0.0]} (frame/item-view {:zoom 3.0 :pan [1.0 2.0]} (get transforms 18)))))
