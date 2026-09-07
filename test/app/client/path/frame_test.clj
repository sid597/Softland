(ns app.client.path.frame-test
  (:require [app.client.engine.transform :as transform]
            [app.client.path.construction :as construction]
            [app.client.path.records :as fixtures]
            [app.client.path.frame :as frame]
            [app.client.path.placements :as placements]
            [clojure.test :refer [deftest is testing]]))

(def transforms
  (transform/world-transforms
   (-> (transform/empty-registry)
       (transform/add-group 17 {:parent 0 :affine [0.5 0.0 0.0 0.5 40.0 20.0]})
       (transform/add-group 18 {:parent 0 :camera :screen}))))

(deftest the-item-key-carries-the-complete-level-inputs
  (let [z (construction/construct fixtures/harness-z)
        items {:path/material z :container 0}
        key (frame/item-key items {:zoom 3.0 :pan [1.0 2.0]} transforms)]
    (is (= key (frame/item-key items {:zoom 3.9 :pan [7.0 7.0]} transforms)))
    (is (not= key (frame/item-key items {:zoom 4.0} transforms)))
    (is (not= key (frame/item-key (assoc items :path/material (assoc-in z [:path/value :subpaths 0 :start 0] 30.0))
                                  {:zoom 3.0} transforms)) "content changes even without a revision bump")
    (is (not= key (frame/item-key (assoc items :path/material (assoc-in z [:path/paint :stroke :color] [0 0 0 1]))
                                  {:zoom 3.0} transforms)))))

(deftest snap-fraction-and-screen-camera-are-real-inputs
  (let [border (construction/construct fixtures/border)
        items {:path/material border :container 17}]
    (is (= (frame/item-key items {:zoom 3.0 :pan [0.25 -0.5]} transforms)
           (frame/item-key items {:zoom 3.0 :pan [10.25 -20.5]} transforms)))
    (is (not= (frame/item-key items {:zoom 3.0 :pan [0.25 0.0]} transforms)
              (frame/item-key items {:zoom 3.0 :pan [0.5 0.0]} transforms)))
    (is (not= (frame/item-key items {:zoom 3.0} transforms)
              (frame/item-key items {:zoom 3.5} transforms)))
    (let [screen (assoc items :container 18)]
      (is (= (frame/item-key screen {:zoom 1.0} transforms)
             (frame/item-key screen {:zoom 2.0 :pan [1.25 20.5]} transforms)) "judge C4 is not imported"))))

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

(def view {:zoom 1.0 :pan [0.0 0.0]})
(defn push [state diff] (placements/change state diff nil))

(deftest named-changes-reach-exactly-their-levels
  (let [z (construction/construct fixtures/harness-z)
        border (construction/construct fixtures/border)
        items {:a {:path/material z :container 0} :b {:path/material z :container 17}
               :snap {:path/material border :container 0} :group-snap {:path/material border :container 17}
               :screen {:path/material border :container 18}}
        initial (:state (push (placements/empty-state view) {:upsert items :groups transforms :order [:a :b :snap :group-snap :screen]}))
        color (push initial {:upsert {:a (assoc-in (:a items) [:path/material :path/paint :stroke :color] [1 0 0 1])}})
        knot (push initial {:upsert {:a (assoc-in (:a items) [:path/material :path/value :subpaths 0 :start 0] 30.0)}})
        pan (placements/change initial {} {:zoom 1.0 :pan [0.25 0.5]})
        integer-pan (placements/change (:state pan) {} {:zoom 1.0 :pan [5.25 -2.5]})
        zoom (placements/change initial {} {:zoom 1.5 :pan [0.0 0.0]})
        group (push initial {:groups (update-in transforms [17 :affine 4] + 0.25)})]
    (is (= {:geometry #{} :pack #{} :rows #{:a}} (:reran color)))
    (is (= #{:a} (get-in knot [:reran :geometry])))
    (is (seq (get-in knot [:reran :pack])))
    (is (= #{:a} (get-in knot [:reran :rows])))
    (is (= #{:snap :group-snap} (get-in pan [:reran :geometry])))
    (is (= #{} (get-in integer-pan [:reran :geometry])))
    (is (= #{} (get-in integer-pan [:reran :rows])))
    (is (= #{:snap :group-snap} (get-in zoom [:reran :geometry])))
    (is (= #{:group-snap} (get-in group [:reran :geometry])))
    (is (= #{:b :group-snap} (:affected group)))))

(deftest bucket-cover-order-and-multiple-placements
  (let [z (construction/construct fixtures/harness-z) item {:path/material z :container 0}
        initial (:state (push (placements/empty-state view) {:upsert {:a item :b item} :groups transforms :order [:a :b]}))
        crossing (placements/change initial {} {:zoom 2.5 :pan [0.0 0.0]})
        reorder (push initial {:order [:b :a]})
        drop (push initial {:remove #{:a}})
        empty (push (:state drop) {:remove #{:b}})]
    (is (empty? (get-in crossing [:reran :pack])) "quadratic skin keeps its existing pack")
    (is (= #{:a :b} (get-in crossing [:reran :rows])) "bucket cover changes rows")
    (is (= #{:a :b} (get-in reorder [:reran :rows])))
    (is (empty? (get-in reorder [:reran :geometry])))
    (is (= #{:b} (get-in drop [:reran :rows])) "later range shifted")
    (is (empty? (:drop-packs drop)) "the other placement still uses the material's pack")
    (is (seq (:drop-packs empty)))
    (is (zero? (get-in empty [:state :row-count])))))

(deftest one-upsert-does-not-prepare-its-neighbours
  (let [item {:path/material (construction/construct fixtures/harness-z) :container 0}
        state (:state (push (placements/empty-state view) {:upsert (zipmap (range 1600) (repeat item))
                                                          :order (vec (range 1600)) :groups transforms}))
        r (push state {:upsert {20 (assoc-in item [:path/material :path/paint :stroke :color] [0 1 0 1])}})]
    (is (= #{20} (:affected r)))
    (is (= #{20} (get-in r [:reran :rows])))
    (is (empty? (:affected (placements/change state {} {:zoom 1.0 :pan [0.25 0.5]}))))))
