(ns app.client.path.component-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.component :as component]
            [app.client.path.tessellation :as tessellation]))

(defn- mesh-bytes [mesh]
  (mapv (fn [[x y]]
          [(Float/floatToIntBits (float x))
           (Float/floatToIntBits (float y))])
        (:vertices mesh)))

(defn- error-data [f]
  (try
    (f)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(deftest rows-travel-and-schema-rejects-by-name-tripwire
  (doseq [[row query]
          [[fixtures/example-ink-component [10.0 1.0]]
           [fixtures/example-shape-component [2.0 20.0]]]]
    (let [validated (component/validate-component! row)
          travelled (edn/read-string (pr-str validated))
          validated-travelled (component/validate-component! travelled)]
      (is (nil? (meta travelled)))
      (is (= (component/component-content-hash validated)
             (component/component-content-hash validated-travelled)))
      (is (= (component/classify validated query)
             (component/classify validated-travelled query)))
      (is (= (mesh-bytes (tessellation/tessellate validated 10.0))
             (mesh-bytes (tessellation/tessellate validated-travelled 10.0))))))

  (let [ink fixtures/example-ink-component
        shape fixtures/example-shape-component
        refusals
        [[:schema/unknown-key #(component/validate-component!
                                (assoc ink :path/curve :quadratic))]
         [:schema/unknown-key #(component/validate-component!
                                (assoc-in ink
                                          [:path/geometry :stroke-points 0 :tilt]
                                          0.5))]
         [:path/contour-role #(component/validate-component!
                              (assoc-in shape
                                        [:path/geometry :contours 0 :role]
                                        :rim))]
         [:path/paint-opacity #(component/validate-component!
                               (update ink :path/paint dissoc :opacity))]
         [:path/cap #(component/validate-component!
                      (assoc-in ink [:path/geometry :cap] :square))]
         [:path/hole-without-outer
          #(component/validate-component!
            (assoc-in shape [:path/geometry :contours]
                      [(second (get-in shape [:path/geometry :contours]))]))]
         [:path/stroke-point-width #(component/validate-component!
                            (assoc-in ink
                                      [:path/geometry :stroke-points 0 :width]
                                      0.0))]]]
    (doseq [[expected call] refusals]
      (let [data (error-data call)]
        (is (= expected (:error-type data)) (str expected))
        (is (vector? (:path data)) (str expected))
        (is (contains? data :value) (str expected)))))

  (is (= [0.2 0.6 0.9 0.6000000000000001]
         (component/paint-color
          (component/validate-component! fixtures/example-ink-component)))))

(deftest width-on-stroke-point-one-formula-tripwire
  (let [tapered (component/validate-component! fixtures/tapered-segment)]
    (is (= :inside (component/classify tapered [8.0 3.0])))
    (is (< (Math/abs (- 2.2 (component/boundary-distance tapered [8.0 3.0])))
           1.0e-9))
    (testing "pressure provenance is not a geometry reader"
      (is (= (component/classify tapered [8.0 3.0])
             (component/classify
              (assoc-in tapered [:path/geometry :stroke-points 0 :pressure] 0.0)
              [8.0 3.0]))))))

(deftest shape-cpu-truth-tripwire
  (let [shape (component/validate-component! fixtures/example-shape-component)]
    (is (= :inside (component/classify shape [2.0 20.0])))
    (is (= :outside (component/classify shape [8.0 8.0])))
    (is (= :boundary (component/classify shape [4.0 8.0])))
    (is (component/hit? shape [4.0 8.0]))))
