(ns app.client.path.frame-test
  (:require [app.client.engine.transform :as transform]
            [app.client.path.construction :as construction]
            [app.client.path.records :as fixtures]
            [app.client.path.frame :as frame]
            [clojure.test :refer [deftest is testing]]))

(def transforms
  (transform/world-transforms
   (-> (transform/empty-registry)
       (transform/add-group 17 {:parent 0 :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
       (transform/add-group 18 {:parent 0 :camera :screen}))))

(deftest the-frame-key-carries-the-complete-level-inputs
  (let [z (construction/construct fixtures/harness-z)
        items [{:path/material z :container 0}]
        key (frame/frame-key items {:zoom 3.0 :pan [1.0 2.0]} transforms)]
    (is (= key (frame/frame-key items {:zoom 3.9 :pan [7.0 7.0]} transforms)))
    (is (not= key (frame/frame-key items {:zoom 4.0} transforms)))
    (is (not= key (frame/frame-key [(assoc (first items) :path/material (assoc-in z [:path/value :subpaths 0 :start 0] 30.0))]
                                  {:zoom 3.0} transforms)) "content changes even without a revision bump")
    (is (not= key (frame/frame-key [(assoc (first items) :path/material (assoc-in z [:path/paint :stroke :color] [0 0 0 1]))]
                                  {:zoom 3.0} transforms)))))

(deftest snap-fraction-and-screen-camera-are-real-inputs
  (let [border (construction/construct fixtures/border)
        items [{:path/material border :container 17}]]
    (is (= (frame/frame-key items {:zoom 3.0 :pan [0.25 -0.5]} transforms)
           (frame/frame-key items {:zoom 3.0 :pan [10.25 -20.5]} transforms)))
    (is (not= (frame/frame-key items {:zoom 3.0 :pan [0.25 0.0]} transforms)
              (frame/frame-key items {:zoom 3.0 :pan [0.5 0.0]} transforms)))
    (is (not= (frame/frame-key items {:zoom 3.0} transforms)
              (frame/frame-key items {:zoom 3.5} transforms)))
    (let [screen [(assoc (first items) :container 18)]]
      (is (= (frame/frame-key screen {:zoom 1.0} transforms)
             (frame/frame-key screen {:zoom 2.0 :pan [1.25 20.5]} transforms)) "judge C4 is not imported"))))

(deftest item-view-composes-zoom-and-group-scale
  (is (= {:scale 1.5 :pan-fraction [0.25 0.5]} (frame/item-view {:zoom 3.0 :pan [1.25 2.5]} (get transforms 17))))
  (is (= {:scale 3.0 :pan-fraction [0.0 0.0]} (frame/item-view {:zoom 3.0} nil)))
  (is (= {:scale 1.0 :pan-fraction [0.0 0.0]} (frame/item-view {:zoom 3.0 :pan [1.25 2.5]} (get transforms 18)))))

(deftest different-regions-never-alias-through-a-hash
  (let [a {:path {:subpaths [{:closed? true :start [0.0 0.0] :segments []}]} :rule :nonzero}
        b (assoc-in a [:path :subpaths 0 :start] [100.0 100.0])]
    (with-redefs [clojure.core/hash (constantly 1096847375)]
      (is (not= (frame/region-key a) (frame/region-key b)) "judge A3")
      (is (= (:path a) (first (frame/region-key a)))))))
