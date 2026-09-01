(ns app.client.path.material-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.path.material :as path-material]))

(def claimed-corpus-pressures
  #{:pressure-width :round-caps-joins :open-polyline :concave-outer
    :explicit-hole :translucent-self-crossing :legal-zoom-extremes})

(defn assert-corpus-coverage! [fixture-pressure->gates]
  (let [actual (set (keys fixture-pressure->gates))
        missing (seq (sort (remove actual claimed-corpus-pressures)))
        unconsumed (seq (sort (for [[pressure gates] fixture-pressure->gates
                                   :when (empty? gates)]
                               pressure)))]
    (when (or missing unconsumed)
      (throw (ex-info "Path corpus is incomplete or unconsumed"
                      {:missing (vec missing) :unconsumed (vec unconsumed)})))
    true))

(defn ink-material
  ([] path-material/example-ink-material)
  ([revision samples]
   (-> path-material/example-ink-material
       (assoc :path/revision revision)
       (assoc-in [:path/geometry :knots]
                 (mapv (fn [index [x y pressure]]
                         {:knot/id [:knot index]
                          :position [x y]
                          :pressure pressure})
                       (range) samples)))))

(defn holed-shape []
  path-material/example-shape-material)

(deftest fail-closed-grammar-and-content-tripwire
  (let [material (ink-material)]
    (is (= material (path-material/validate-material! material)))
    (is (= path-material/example-shape-material
           (path-material/validate-material! path-material/example-shape-material)))
    (is (= (path-material/material-content-key material)
           (path-material/material-content-key
            (assoc material :path/material-id :path/other
                            :path/revision :ink/rev-2)))
        "material identity and revision are not content")
    (is (= [0.2 0.6 0.9 0.6000000000000001]
           (path-material/paint-color material)))
    (testing "malformed material refuses by field name"
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"missing required fields"
                            (path-material/validate-material!
                             (dissoc material :path/paint))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"unknown fields"
                            (path-material/validate-material!
                             (assoc material :path/curve :quadratic))))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"round caps only"
                            (path-material/validate-material!
                             (assoc-in material [:path/geometry :cap] :square)))))
    (is (assert-corpus-coverage!
         {:pressure-width [:jvm]
          :round-caps-joins [:jvm]
          :open-polyline [:jvm]
          :concave-outer [:jvm :gpu]
          :explicit-hole [:jvm :gpu]
          :translucent-self-crossing [:gpu]
          :legal-zoom-extremes [:gpu]}))))

(deftest gesture-time-grammar-tripwire
  (let [material (assoc-in (ink-material)
                           [:path/geometry :knots 0 :gesture-time]
                           12.5)]
    (is (= material (path-material/validate-material! material)))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"gesture time must be finite"
                          (path-material/validate-material!
                           (assoc-in material
                                     [:path/geometry :knots 0 :gesture-time]
                                     Double/POSITIVE_INFINITY))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"unknown fields"
                          (path-material/validate-material!
                           (assoc-in material
                                     [:path/geometry :knots 0 :source-event-ids]
                                     [:event/e1]))))))

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
