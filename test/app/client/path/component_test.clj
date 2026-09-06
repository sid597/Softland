(ns app.client.path.component-test
  (:require [app.client.path.component :as component]
            [app.client.path.fixtures :as fixtures]
            [clojure.test :refer [deftest is testing]]))

(defn- error-type [f]
  (try (f) nil (catch Exception e (:error-type (ex-data e)))))

(deftest records-validate-and-reject-by-name
  (doseq [record [fixtures/harness-z fixtures/z-as-dabs fixtures/pressure-ink fixtures/holed-concave
                  fixtures/border fixtures/draw-tool fixtures/pen-tool]]
    (is (= record (component/validate-component! record))))
  (is (= :path/fill-rule (error-type #(component/validate-component! (assoc-in fixtures/holed-concave [:path/paint :fill :rule] :winding)))))
  (is (= :path/stroke-tip (error-type #(component/validate-component! (assoc-in fixtures/harness-z [:path/paint :stroke :tip] :brush)))))
  (is (= :path/source-kind (error-type #(component/validate-component! (assoc fixtures/harness-z :path/source {:kind :circle})))))
  (is (= :schema/vector-too-short (error-type #(component/validate-component! (assoc-in fixtures/pen-tool [:path/source :contours 0 :anchors] [{:p [0.0 0.0]}])))))
  (is (= :path/paint-color (error-type #(component/validate-component! (assoc-in fixtures/harness-z [:path/paint :stroke :color] [1 1 1 2]))))))

(deftest the-default-construction-is-data-without-a-tool-name
  (let [c (component/default-construction fixtures/z-as-dabs)]
    (is (= [:path/source :path/dabs] (mapv :op (:steps c))))
    (is (= {:path "path" :regions ["stroke"]} (:return c)))
    (is (not-any? #(re-find #"name" (pr-str %)) (:steps c)) "nothing binds the tool's name"))
  (is (= [:path/source :path/snap :path/fill-region :path/envelope]
         (mapv :op (:steps (component/default-construction fixtures/border)))))
  (testing "a record may carry its own construction"
    (let [own {:steps [{:out "path" :op :path/source :source "source" :tool "tool"}
                       {:out "fill" :op :path/fill-region :path "path" :rule :nonzero}]
               :return {:path "path" :regions ["fill"]}}
          result (component/run (assoc fixtures/harness-z :path/construction own) {})]
      (is (:ok? result))
      (is (= [:fill] (mapv :kind (:regions result)))))))

(deftest a-run-reports-what-it-read-and-colour-is-never-read
  (let [result (component/run fixtures/harness-z {:scale 10.0 :pan [3.0 4.0]})]
    (is (:ok? result))
    (is (= [:stroke] (mapv :kind (:regions result))))
    (is (= #{"source" "tool" "paint.stroke.geometry"} (set (keys (:reads result)))))
    (is (not (component/rerun? (assoc-in fixtures/harness-z [:path/paint :stroke :color] [0 0 0 1]) {:scale 10.0} (:reads result)))
        "a colour edit changes nothing the construction read")
    (is (component/rerun? (assoc-in fixtures/harness-z [:path/paint :stroke :cap] :butt) {:scale 10.0} (:reads result)))
    (is (component/rerun? (assoc-in fixtures/harness-z [:path/source :samples 0 0] 30.0) {:scale 10.0} (:reads result)))
    (is (not (component/rerun? fixtures/harness-z {:scale 3.0 :pan [9.0 9.0]} (:reads result)))
        "a local-unit stroke never reads the view: a camera move is not a rerun"))
  (testing "a device-unit width and snapping read the view"
    (let [result (component/run fixtures/border {:scale 3.0 :pan [0.0 0.0]})]
      (is (contains? (:reads result) "view.scale"))
      (is (contains? (:reads result) "view.pan"))
      (is (component/rerun? fixtures/border {:scale 4.0 :pan [0.0 0.0]} (:reads result)))
      (is (component/rerun? fixtures/border {:scale 3.0 :pan [1.0 0.0]} (:reads result))))))

(deftest a-missing-capability-is-a-failed-run-not-an-empty-drawing
  (let [result (component/run (assoc fixtures/harness-z :path/construction
                                     {:steps [{:out "path" :op :path/source :source "source" :tool "tool"}
                                              {:out "face" :op :geometry/arrange :curves "path"}]
                                      :return {:path "path" :regions []}})
                              {})]
    (is (not (:ok? result)))
    (is (= [:geometry/arrange] (:missing result)))
    (is (= [] (:regions result)))))

(deftest classification-is-membership-with-a-boundary-band
  (testing "the holed shape under even-odd"
    (is (= :inside (component/classify fixtures/holed-concave [90.0 90.0])))
    (is (= :outside (component/classify fixtures/holed-concave [42.0 42.0])) "inside the hole")
    (is (= :outside (component/classify fixtures/holed-concave [64.0 90.0])) "the notch")
    (is (= :boundary (component/classify fixtures/holed-concave [24.0 60.0])))
    (is (= :boundary (component/classify fixtures/holed-concave [22.5 60.0] 2.0)) "slop widens the band")
    (is (= :outside (component/classify fixtures/holed-concave [20.0 60.0] 2.0)))
    (is (= :path/hit-slop (error-type #(component/classify fixtures/holed-concave [0.0 0.0] -1.0)))))
  (testing "the Z stroke: the crossing is inside once, the same answer the filler gives"
    (is (= :inside (component/classify fixtures/harness-z [65.0 64.0])))
    (is (= :outside (component/classify fixtures/harness-z [5.0 5.0])))
    (is (component/hit? fixtures/harness-z [26.0 28.0]))
    (is (< (component/boundary-distance fixtures/harness-z [5.0 5.0]) 30.0))))

(deftest colours-come-from-the-paint-by-region-kind
  (let [result (component/run fixtures/pen-tool {})
        [fill stroke] (:regions result)]
    (is (= :fill (:kind fill)))
    (is (= :stroke (:kind stroke)))
    (is (= [0.25 0.55 0.9 0.5] (component/region-color fixtures/pen-tool fill)))
    (is (= [0.05 0.1 0.3 1.0] (component/region-color fixtures/pen-tool stroke)))))

(deftest a-clip-is-a-region-with-no-paint
  (let [z (component/run fixtures/harness-z {})
        clipped (assoc-in fixtures/holed-concave [:path/paint :clip] {:path (:path (first (:regions z))) :rule :nonzero})
        result (component/run clipped {})]
    (is (= clipped (component/validate-component! clipped)) "a returned path bound as a value validates")
    (is (= :clip (:kind (:clip result))))
    (is (nil? (:paint (:clip result))))
    (is (= [:fill] (mapv :kind (:regions result))) "the clip is not painted")))

(deftest content-hash-ignores-identity
  (is (= (component/component-content-hash fixtures/harness-z)
         (component/component-content-hash (assoc fixtures/harness-z :path/revision 99 :path/material-id :other))))
  (is (not= (component/component-content-hash fixtures/harness-z)
            (component/component-content-hash fixtures/z-as-dabs))))
