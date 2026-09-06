(ns app.client.path.component-test
  (:require [app.client.path.component :as component]
            [app.client.path.construction :as construction]
            [app.client.path.records :as fixtures]
            [clojure.test :refer [deftest is testing]]))

(defn- error-type [f] (try (f) nil (catch Exception e (:error-type (ex-data e)))))
(def z (construction/construct fixtures/harness-z))
(def holed (construction/construct fixtures/holed-concave))
(def border (construction/construct fixtures/border))

(deftest path-values-validate-without-a-source-kind
  (doseq [record [fixtures/harness-z fixtures/z-as-dabs fixtures/pressure-ink fixtures/holed-concave
                  fixtures/border fixtures/draw-tool fixtures/pen-tool]
          :let [value (construction/construct record)]]
    (is (= value (component/validate-component! value))))
  (is (= :path/fill-rule (error-type #(component/validate-component! (assoc-in holed [:path/paint :fill :rule] :winding)))))
  (is (= :path/stroke-tip (error-type #(component/validate-component! (assoc-in z [:path/paint :stroke :tip] :brush)))))
  (is (= :path/paint-color (error-type #(component/validate-component! (assoc-in z [:path/paint :stroke :color] [1 1 1 2])))))
  (is (some? (error-type #(component/validate-component! fixtures/harness-z))) "the renderer cannot accept an unevaluated source record"))

(deftest geometry-inputs-are-complete-values-with-explicit-view-declarations
  (let [inputs (component/geometry-inputs z {:scale 1.0})]
    (is (= inputs (component/geometry-inputs (assoc-in z [:path/paint :stroke :color] [0 0 0 1]) {:scale 99.0})))
    (is (= inputs (component/geometry-inputs (assoc z :path/material-id :copy :path/revision 7) {:pan [5.0 9.0]})))
    (is (not= inputs (component/geometry-inputs (assoc-in z [:path/paint :stroke :cap] :butt) {})))
    (is (not= inputs (component/geometry-inputs (assoc-in z [:path/value :subpaths 0 :start 0] 30.0) {}))))
  (let [input (component/geometry-inputs border {:scale 3.0 :pan [0.25 -0.5]})]
    (is (= {:scale 3.0 :pan-fraction [0.25 0.5]} (:view input)))
    (is (= input (component/geometry-inputs border {:scale 3.0 :pan [10.25 -20.5]}))))
  (testing "a snapped local-width record also depends on scale"
    (let [local (assoc-in border [:path/paint :stroke :unit] :local)]
      (is (not= (component/geometry-inputs local {:scale 1.0}) (component/geometry-inputs local {:scale 1.5}))))))

(deftest classification-is-membership-with-a-boundary-band
  (is (= :inside (component/classify holed [90.0 90.0])))
  (is (= :outside (component/classify holed [42.0 42.0])))
  (is (= :outside (component/classify holed [64.0 90.0])))
  (is (= :boundary (component/classify holed [24.0 60.0])))
  (is (= :boundary (component/classify holed [22.5 60.0] 2.0)))
  (is (= :outside (component/classify holed [20.0 60.0] 2.0)))
  (is (= :path/hit-slop (error-type #(component/classify holed [0.0 0.0] -1.0))))
  (is (= :inside (component/classify z [65.0 64.0])))
  (is (= :outside (component/classify z [5.0 5.0])))
  (is (component/hit? z [26.0 28.0]))
  (is (< (component/boundary-distance z [5.0 5.0]) 30.0)))

(deftest colours-come-from-the-paint-by-region-kind
  (let [value (construction/construct fixtures/pen-tool)
        [fill stroke] (:regions (component/regions value {}))]
    (is (= [:fill :stroke] (mapv :kind [fill stroke])))
    (is (= [0.25 0.55 0.9 0.5] (component/region-color value fill)))
    (is (= [0.05 0.1 0.3 1.0] (component/region-color value stroke)))))

(deftest classification-intersects-the-returned-clip
  (let [skin (:path (first (:regions (component/regions z {}))))
        clipped (assoc-in holed [:path/paint :clip] {:path skin :rule :nonzero})]
    (is (= clipped (component/validate-component! clipped)))
    (doseq [point [[30.0 60.0] [60.0 40.0] [100.0 70.0]]]
      (is (= :inside (component/classify holed point)))
      (is (= :outside (component/classify clipped point)) (str "judge A4 " point)))
    (is (= :inside (component/classify clipped [90.0 90.0])))
    (is (= :outside (component/classify (assoc-in clipped [:path/paint :clip :path] {:subpaths []}) [90.0 90.0])))))

(deftest content-key-ignores-identity
  (is (= (component/component-content-key z) (component/component-content-key (assoc z :path/revision 99 :path/material-id :other))))
  (is (not= (component/component-content-key z) (component/component-content-key (construction/construct fixtures/z-as-dabs)))))
