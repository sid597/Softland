(ns app.client.substrate.region3d-scene-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.region3d-material :as material]
            [app.client.substrate.region3d-oracle :as oracle]
            [app.client.substrate.region3d-scene :as region]
            [app.client.substrate.scene-tape :as tape]
            [app.client.substrate.region3d-material-test :as fixture]))

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
                           :lens material/default-perspective-lens}))))

(defn tape-fixture-entry [id path]
  {:entry/id id :material/id id :material/revision 1 :instance/id id
   :family/id :render.family/path
   :order {:stratum :world :pass-class :direct :stack-path path
           :part-rank 0 :stable-tie id}
   :paint {:vertex-count 1}
   :visibility {:visible? true}})

(deftest s1-region-is-an-ordinary-world-direct-sandwich-with-earned-order
  (let [region-entry (region/tape-entry
                      {:region-id :region/a :revision 1
                       :source-order {:stack-path [[:root 2 2]]}
                       :rect [0 0 200 120]})
        path (tape-fixture-entry :path [[:root 1 1]])
        text (assoc (tape-fixture-entry :text [[:root 3 3]])
                    :family/id :render.family/msdf)
        compiled-a (tape/compile-tape :sandwich [text region-entry path])
        compiled-b (tape/compile-tape :sandwich [path text region-entry])
        moved (assoc-in region-entry [:order :stack-path] [[:root 4 4]])
        moved-tape (tape/compile-tape :moved [text moved path])]
    (is (= :world (get-in region-entry [:order :stratum])))
    (is (= :direct (get-in region-entry [:order :pass-class])))
    (is (= [:path [:frame/region3d :region/a] :text]
           (mapv :entry/id (:entries compiled-a))))
    (is (= (:order-hash compiled-a) (:order-hash compiled-b)))
    (is (= (mapv :entry/id (:entries compiled-a))
           (mapv :entry/id (:entries compiled-b))))
    (is (= [:path :text [:frame/region3d :region/a]]
           (mapv :entry/id (:entries moved-tape))))))

(deftest s2-depth-ray-pick-returns-nearest-identity-and-t-not-painter-order
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
        material-row (assoc material/default-material
                            :base-color (fixture/tagged 0.7 0.2 0.1 0.5)
                            :metallic 0.3 :roughness 0.45)
        shaded (oracle/shade-reference
                {:material material-row
                 :normal [0.0 0.0 1.0]
                 :point [0.0 0.0 0.51]
                 :eye [0.0 0.0 8.0]
                 :lights [{:light {:kind :directional
                                   :color (fixture/tagged 1.0 1.0 1.0)
                                   :intensity 2.0 :cast-shadow false}
                           :position [0.0 5.0 5.0]
                           :direction [0.0 -0.3 -1.0]}]
                 :ambient material/default-ambient
                 :bvh (:bvh derived) :object-id :near})]
    (is (= 4 (count shaded)))
    (is (= 0.5 (last shaded)) "material alpha is not forced to one")
    (is (every? #(<= 0.0 % 0.5) (butlast shaded))
        "linear RGB is premultiplied by material alpha")
    (is (= :khronos-pbr-neutral-v1 region/tone-map-algorithm-version))))

(deftest shadow-light-space-remains-derived-from-the-live-scene
  (let [derived (region/derive-scene (fixture-region))]
    (is (some? (region/shadow-light-space derived)))))
