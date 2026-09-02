(ns app.client.path.material-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [app.client.path.fixtures :as fixtures]
            [app.client.path.material :as material]
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

(deftest rows-travel-and-grammar-refuses-by-name-tripwire
  (doseq [[row query]
          [[fixtures/example-ink-material [10.0 1.0]]
           [fixtures/example-shape-material [2.0 20.0]]]]
    (let [validated (material/validate-material! row)
          travelled (edn/read-string (pr-str validated))
          validated-travelled (material/validate-material! travelled)]
      (is (nil? (meta travelled)))
      (is (= (material/material-content-key validated)
             (material/material-content-key validated-travelled)))
      (is (= (material/classify validated query)
             (material/classify validated-travelled query)))
      (is (= (mesh-bytes (tessellation/tessellate validated 10.0))
             (mesh-bytes (tessellation/tessellate validated-travelled 10.0))))))

  (let [ink fixtures/example-ink-material
        shape fixtures/example-shape-material
        refusals
        [[:schema/unknown-key #(material/validate-material!
                                (assoc ink :path/curve :quadratic))]
         [:schema/unknown-key #(material/validate-material!
                                (assoc-in ink
                                          [:path/geometry :knots 0 :tilt]
                                          0.5))]
         [:path/contour-role #(material/validate-material!
                              (assoc-in shape
                                        [:path/geometry :contours 0 :role]
                                        :rim))]
         [:path/paint-opacity #(material/validate-material!
                               (update ink :path/paint dissoc :opacity))]
         [:path/cap #(material/validate-material!
                      (assoc-in ink [:path/geometry :cap] :square))]
         [:path/hole-without-outer
          #(material/validate-material!
            (assoc-in shape [:path/geometry :contours]
                      [(second (get-in shape [:path/geometry :contours]))]))]
         [:path/knot-width #(material/validate-material!
                            (assoc-in ink
                                      [:path/geometry :knots 0 :width]
                                      0.0))]]]
    (doseq [[expected call] refusals]
      (let [data (error-data call)]
        (is (= expected (:error-type data)) (str expected))
        (is (vector? (:path data)) (str expected))
        (is (contains? data :value) (str expected)))))

  (is (= [0.2 0.6 0.9 0.6000000000000001]
         (material/paint-color
          (material/validate-material! fixtures/example-ink-material)))))

(deftest width-on-knot-one-formula-tripwire
  (let [tapered (material/validate-material! fixtures/tapered-segment)]
    (is (= :inside (material/classify tapered [8.0 3.0])))
    (is (< (Math/abs (- 2.2 (material/boundary-distance tapered [8.0 3.0])))
           1.0e-9))
    (testing "pressure provenance is not a geometry reader"
      (is (= (material/classify tapered [8.0 3.0])
             (material/classify
              (assoc-in tapered [:path/geometry :knots 0 :pressure] 0.0)
              [8.0 3.0]))))))

(deftest shape-cpu-truth-tripwire
  (let [shape (material/validate-material! fixtures/example-shape-material)]
    (is (= :inside (material/classify shape [2.0 20.0])))
    (is (= :outside (material/classify shape [8.0 8.0])))
    (is (= :boundary (material/classify shape [4.0 8.0])))
    (is (material/hit? shape [4.0 8.0]))))
