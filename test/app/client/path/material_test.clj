(ns app.client.path.material-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.path.material :as path-material]))

(defn ink-material
  ([] (ink-material :ink/rev-1 [[0.0 0.0 0.2] [20.0 0.0 0.6]
                               [30.0 10.0 1.0]]))
  ([revision samples]
   {:path/material-id :path/ink-fixture
    :path/revision revision
    :path/kind :ink
    :path/geometry
    {:knots (mapv (fn [index [x y pressure]]
                    {:knot/id [:knot index]
                     :position [x y]
                     :pressure pressure})
                  (range) samples)
     :base-width 10.0 :cap :round :join :round}
    :path/paint {:color [0.2 0.6 0.9 0.8]
                 :opacity 0.75
                 :color-space :srgb
                 :alpha-association :straight}
    :path/provenance {:actor :fixture :act :create}}))

(defn holed-shape []
  {:path/material-id :path/holed-concave
   :path/revision :shape/rev-1
   :path/kind :shape
   :path/geometry
   {:open-width 3.0
    :contours
    [{:contour/id :outer :role :outer
      :points [[0.0 0.0] [40.0 0.0] [40.0 40.0]
               [24.0 40.0] [24.0 16.0] [16.0 16.0]
               [16.0 40.0] [0.0 40.0]]}
     {:contour/id :hole :role :hole
      :points [[4.0 4.0] [13.0 4.0] [13.0 13.0] [4.0 13.0]]}
     {:contour/id :open :role :open
      :points [[28.0 22.0] [34.0 28.0] [30.0 34.0]]}]}
   :path/paint {:color [0.9 0.3 0.2 1.0]
                :opacity 1.0
                :color-space :srgb
                :alpha-association :straight}
   :path/provenance {:actor :fixture :act :create}})

(deftest fail-closed-grammar-cache-and-packing-tripwire
  (let [material (ink-material)
        key (path-material/material-cache-key material 1.0)
        bumped (path-material/material-cache-key material
                                                :path-tessellation-v2 1.0)]
    (is (= material (path-material/validate-material! material)))
    (is (not= key bumped) "algorithm versions are part of cache identity")
    (is (= [0.2 0.6 0.9 0.6000000000000001]
           (path-material/paint-color material)))
    (is (= 7 (count (path-material/vertex-values material [1.0 2.0] 9))))
    (testing "malformed material refuses by field name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"missing required fields"
                            (path-material/validate-material!
                             (dissoc material :path/provenance))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"unknown fields"
                            (path-material/validate-material!
                             (assoc material :path/curve :quadratic))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"round caps only"
                            (path-material/validate-material!
                             (assoc-in material [:path/geometry :cap] :square)))))
    (is (= [:legal-min :floor-default :floor-default :legal-max :legal-max]
           (mapv (comp :regime/id path-material/zoom-regime)
                 [0.01 0.1 8.0 10.0 1000.0])))
    (is (path-material/assert-corpus-coverage!
         {:pressure-width [:jvm]
          :round-caps-joins [:jvm]
          :open-polyline [:jvm]
          :concave-outer [:jvm :gpu]
          :explicit-hole [:jvm :gpu]
          :translucent-self-crossing [:gpu]
          :legal-zoom-extremes [:gpu]}))))

(deftest pressure-width-and-cpu-interior-truth-tripwire
  (let [ink (ink-material)
        shape (holed-shape)]
    (is (= [2.0 6.0 10.0]
           (mapv #(path-material/pressure-width 10.0 %)
                 [0.2 0.6 1.0]))
        "base-width times clamped pressure is monotone")
    (is (= :inside (path-material/classify ink [10.0 1.0])))
    (is (= :boundary (path-material/classify ink [10.0 2.0])))
    (is (= :outside (path-material/classify ink [10.0 3.0])))
    (is (= :inside (path-material/classify shape [2.0 20.0])))
    (is (= :outside (path-material/classify shape [8.0 8.0]))
        "explicit hole subtracts from the outer interior")
    (is (= :boundary (path-material/classify shape [4.0 8.0]))
        "mathematical boundary is a named pick hit")
    (is (path-material/hit? shape [4.0 8.0]))))
