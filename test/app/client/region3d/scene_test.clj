(ns app.client.region3d.scene-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.region3d.component :as component]
            [app.client.region3d.oracle :as oracle]
            [app.client.region3d.scene :as region]
            [app.client.region3d.component-test :as fixture]))

(defn fixture-region
  ([] (fixture-region {}))
  ([extra]
   (let [base {:near (fixture/mesh-object :near nil [0.0 0.0 0.0])
               :far (fixture/mesh-object :far nil [0.35 0.0 -1.0])
               :sun {:object/id :sun :object/kind :light :parent nil
                     :transform {:translation [4.0 8.0 6.0]
                                 :rotation [0.0 0.0 0.0 1.0]
                                 :scale [1.0 1.0 1.0]}
                     :provenance {:asserted-by :sid}
                     :light {:kind :directional
                             :color (fixture/tagged 1.0 0.96 0.9)
                             :intensity 2.0 :cast-shadow true}}}]
     (assoc (fixture/region (merge base extra))
            :view-default {:pivot [0.0 0.0 0.0] :distance 8.0
                           :yaw 0.0 :pitch 0.0
                           :lens component/default-perspective-lens}))))

(deftest s2-depth-ray-pick-returns-nearest-identity-and-t-not-renderer-order
  (let [maintained (assoc (region/derive-scene (fixture-region))
                          :region-id :region/a)
        camera (region/camera-matrices
                (get-in maintained [:region :view-default]) [640.0 360.0])
        center (region/pick-region {:maintained maintained :camera camera
                                    :region-point [320.0 180.0]})
        ray (region/ray-from-region-point camera [320.0 180.0])
        hit (region/query-bvh (:bvh maintained) ray)
        boundary-hit (region/query-bvh
                      (:bvh maintained)
                      {:origin [0.5 0.5 8.0]
                       :direction [0.0 0.0 -1.0]})]
    (is (= :near (:object-id center)))
    (is (= :near (:object-id hit)))
    (is (< 7.0 (:t center) 8.0))
    (is (= (:t hit) (:t center)))
    (is (= :near (:object-id boundary-hit)))
    (is (true? (:boundary? boundary-hit)))
    (is (= :region-background
           (:route (region/pick-region
                    {:maintained maintained :camera camera
                     :region-point [5.0 5.0]}))))
    (testing "coincident surfaces use stable object-id order"
      (let [same (fixture-region
                  {:a (fixture/mesh-object :a nil [2.0 0.0 0.0])
                   :b (fixture/mesh-object :b nil [2.0 0.0 0.0])})
            derived (region/derive-scene same)
            ray {:origin [2.0 0.0 8.0] :direction [0.0 0.0 -1.0]}]
        (is (= :a (:object-id (region/query-bvh (:bvh derived) ray))))))))

(deftest l2-worn-representation-does-not-enter-camera-or-pick-geometry
  (let [maintained (assoc (region/derive-scene (fixture-region))
                          :region-id :region/a)
        desired-size [640.0 360.0]
        sharp-lease {:desired-size desired-size :size [768 512]
                     :rung-divisor 1}
        worn-lease {:desired-size desired-size :size [384 256]
                    :rung-divisor 2}
        camera-for (fn [lease]
                     (region/camera-matrices
                      (get-in maintained [:region :view-default])
                      (:desired-size lease)))
        sharp-camera (camera-for sharp-lease)
        worn-camera (camera-for worn-lease)
        pick-for (fn [camera]
                   (region/pick-region
                    {:maintained maintained :camera camera
                     :region-point [320.0 180.0]}))]
    (is (not= (:size sharp-lease) (:size worn-lease)))
    (is (= sharp-camera worn-camera))
    (is (= (pick-for sharp-camera) (pick-for worn-camera)))
    (is (= :near (:object-id (pick-for worn-camera))))))

(deftest s4-lit-color-oracle-is-linear-premultiplied-and-alpha-survives
  (let [derived (region/derive-scene (fixture-region))
        component-row (assoc component/default-component
                            :base-color (fixture/tagged 0.7 0.2 0.1 0.5)
                            :metallic 0.3 :roughness 0.45)
        shaded (oracle/shade-reference
                {:component component-row
                 :normal [0.0 0.0 1.0]
                 :point [0.0 0.0 0.51]
                 :eye [0.0 0.0 8.0]
                 :lights [{:light {:kind :directional
                                   :color (fixture/tagged 1.0 1.0 1.0)
                                   :intensity 2.0 :cast-shadow false}
                           :position [0.0 5.0 5.0]
                           :direction [0.0 -0.3 -1.0]}]
                 :ambient component/default-ambient
                 :bvh (:bvh derived) :object-id :near})]
    (is (= 4 (count shaded)))
    (is (= 0.5 (last shaded)) "component alpha is not forced to one")
    (is (every? #(<= 0.0 % 0.5) (butlast shaded))
        "linear RGB is premultiplied by component alpha")
    (is (= :khronos-pbr-neutral-v1 region/tone-map-algorithm-version))))

(deftest shadow-light-space-remains-derived-from-the-live-scene
  (let [derived (region/derive-scene (fixture-region))]
    (is (some? (region/shadow-light-space derived)))))

(deftest transient-transform-is-a-retained-component-update
  (let [moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])
        child (fixture/mesh-object :child :moving [1.0 0.0 0.0])
        region (fixture/region {:moving moving :child child})
        initial (region/evaluate-scene nil nil region {})
        preview-1 (assoc (:transform moving) :translation [2.0 0.0 0.0])
        preview-2 (assoc (:transform moving) :translation [3.0 0.0 0.0])
        first-preview
        (region/evaluate-scene
         (:maintained initial) (:evaluation-key initial) region
         {:preview-transform {:object-id :moving :transform preview-1}})
        next-preview
        (region/evaluate-scene
         (:maintained first-preview) (:evaluation-key first-preview) region
         {:preview-transform {:object-id :moving :transform preview-2}})
        settled
        (region/evaluate-scene
         (:maintained next-preview) (:evaluation-key next-preview) region
         {:settled-transforms {:moving preview-2}})
        canceled
        (region/evaluate-scene
         (:maintained next-preview) (:evaluation-key next-preview) region {})]
    (testing "preview evaluates only the affected hierarchy"
      (is (= :full (:update-kind initial)))
      (is (= :transform (:update-kind first-preview)))
      (is (= :transform (:update-kind next-preview)))
      (is (= #{:moving :child} (:affected-object-ids next-preview)))
      (is (zero? (get-in next-preview [:maintained :stats :full-rebuilds])))
      (is (zero? (get-in next-preview
                         [:maintained :stats :bvh-build-triangles])))
      (is (= 2 (get-in next-preview
                       [:maintained :stats :instance-uploads])))
      (is (oracle/scene-equivalent? (:maintained next-preview))))
    (testing "settle is identity and cancel is an incremental inverse"
      (is (= :none (:update-kind settled)))
      (is (identical? (:maintained next-preview) (:maintained settled)))
      (is (= :transform (:update-kind canceled)))
      (is (= (get-in initial [:maintained :region])
             (get-in canceled [:maintained :region])))
      (is (oracle/scene-equivalent? (:maintained canceled))))))

(deftest transform-cost-is-affected-set-not-scene-population
  (let [moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])
        extras (into {}
                     (for [index (range 48)
                           :let [id (keyword (str "extra-" index))]]
                       [id (fixture/mesh-object id nil
                                                [100.0 index 0.0])]))
        region (fixture/region (assoc extras :moving moving))
        initial (region/evaluate-scene nil nil region {})
        after (assoc (:transform moving) :translation [5.0 0.0 0.0])
        preview
        (region/evaluate-scene
         (:maintained initial) (:evaluation-key initial) region
         {:preview-transform {:object-id :moving :transform after}})]
    (is (= 49 (count (get-in preview [:maintained :instances]))))
    (is (= #{:moving} (:affected-object-ids preview)))
    (is (= 1 (get-in preview [:maintained :stats :instance-uploads])))
    (is (zero? (get-in preview [:maintained :stats :full-rebuilds])))
    (is (oracle/scene-equivalent? (:maintained preview)))))

(deftest component-change-keeps-the-full-oracle-door
  (let [moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])
        region (fixture/region {:moving moving})
        initial (region/evaluate-scene nil nil region {})
        changed (assoc-in region [:scene :moving :component :roughness] 0.17)
        result (region/evaluate-scene
                (:maintained initial) (:evaluation-key initial) changed {})]
    (is (= :full (:update-kind result)))
    (is (= 1 (get-in result [:maintained :stats :full-rebuilds])))
    (is (oracle/scene-equivalent? (:maintained result)))))

(deftest session-transform-cannot-mint-an-object
  (let [region (fixture/region
                {:moving (fixture/mesh-object :moving nil [0.0 0.0 0.0])})]
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"session transform target is missing"
         (region/session-transform-map
          region {:settled-transforms
                  {:missing {:translation [0.0 0.0 0.0]
                             :rotation [0.0 0.0 0.0 1.0]
                             :scale [1.0 1.0 1.0]}}})))))
