(ns app.client.substrate.region3d-evaluation-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.region3d-evaluation :as evaluation]
            [app.client.substrate.region3d-material-test :as fixture]
            [app.client.substrate.region3d-scene :as scene]))

(deftest transient-transform-is-a-retained-component-update
  (let [moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])
        child (fixture/mesh-object :child :moving [1.0 0.0 0.0])
        region (fixture/region {:moving moving :child child})
        initial (evaluation/evaluate-scene nil nil region {})
        preview-1 (assoc (:transform moving) :translation [2.0 0.0 0.0])
        preview-2 (assoc (:transform moving) :translation [3.0 0.0 0.0])
        first-preview
        (evaluation/evaluate-scene
         (:maintained initial) (:evaluation-key initial) region
         {:preview-transform {:object-id :moving :transform preview-1}})
        next-preview
        (evaluation/evaluate-scene
         (:maintained first-preview) (:evaluation-key first-preview) region
         {:preview-transform {:object-id :moving :transform preview-2}})
        settled
        (evaluation/evaluate-scene
         (:maintained next-preview) (:evaluation-key next-preview) region
         {:settled-transforms {:moving preview-2}})
        canceled
        (evaluation/evaluate-scene
         (:maintained next-preview) (:evaluation-key next-preview) region {})]
    (testing "preview evaluates only the affected hierarchy"
      (is (= :full (:update-kind initial)))
      (is (= :transform (:update-kind first-preview)))
      (is (= :transform (:update-kind next-preview)))
      (is (= #{:moving :child} (:affected-object-ids next-preview)))
      (is (zero? (get-in next-preview [:maintained :receipt :full-rebuilds])))
      (is (zero? (get-in next-preview
                         [:maintained :receipt :bvh-build-triangles])))
      (is (= 2 (get-in next-preview
                       [:maintained :receipt :instance-uploads])))
      (is (scene/scene-equivalent? (:maintained next-preview))))
    (testing "settle is identity and cancel is an incremental inverse"
      (is (= :none (:update-kind settled)))
      (is (identical? (:maintained next-preview) (:maintained settled)))
      (is (= :transform (:update-kind canceled)))
      (is (= (get-in initial [:maintained :region])
             (get-in canceled [:maintained :region])))
      (is (scene/scene-equivalent? (:maintained canceled))))))

(deftest transform-cost-is-affected-set-not-scene-population
  (let [moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])
        extras (into {}
                     (for [index (range 48)
                           :let [id (keyword (str "extra-" index))]]
                       [id (fixture/mesh-object id nil
                                                [100.0 index 0.0])]))
        region (fixture/region (assoc extras :moving moving))
        initial (evaluation/evaluate-scene nil nil region {})
        after (assoc (:transform moving) :translation [5.0 0.0 0.0])
        preview
        (evaluation/evaluate-scene
         (:maintained initial) (:evaluation-key initial) region
         {:preview-transform {:object-id :moving :transform after}})]
    (is (= 49 (count (get-in preview [:maintained :instances]))))
    (is (= #{:moving} (:affected-object-ids preview)))
    (is (= 1 (get-in preview [:maintained :receipt :instance-uploads])))
    (is (zero? (get-in preview [:maintained :receipt :full-rebuilds])))
    (is (scene/scene-equivalent? (:maintained preview)))))

(deftest material-change-keeps-the-full-oracle-door
  (let [moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])
        region (fixture/region {:moving moving})
        initial (evaluation/evaluate-scene nil nil region {})
        changed (assoc-in region [:scene :moving :material :roughness] 0.17)
        result (evaluation/evaluate-scene
                (:maintained initial) (:evaluation-key initial) changed {})]
    (is (= :full (:update-kind result)))
    (is (= 1 (get-in result [:maintained :receipt :full-rebuilds])))
    (is (scene/scene-equivalent? (:maintained result)))))

(deftest session-transform-cannot-mint-an-object
  (let [region (fixture/region
                {:moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])})]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"session transform target is missing"
         (evaluation/session-transform-map
          region {:settled-transforms
                  {:missing {:translation [0.0 0.0 0.0]
                             :rotation [0.0 0.0 0.0 1.0]
                             :scale [1.0 1.0 1.0]}}})))))
