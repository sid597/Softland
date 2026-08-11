(ns app.client.substrate.region3d-scene-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.region3d-material :as material]
            [app.client.substrate.region3d-scene :as region]
            [app.client.substrate.scene-tape :as tape]
            [app.client.substrate.region3d-material-test :as fixture]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-store :as store]))

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
   :pick {:geometry :fixture :owner id}
   :visibility {:visible? true}})

(deftest s1-region-is-an-ordinary-world-direct-sandwich-with-earned-order
  (let [region-entry (region/tape-entry
                      {:region-id :region/a :revision 1
                       :source-order {:stack-path [[:root 2 2]]}
                       :resolve-view :held :rect [0 0 200 120]})
        path (tape-fixture-entry :path [[:root 1 1]])
        text (assoc (tape-fixture-entry :text [[:root 3 3]])
                    :family/id :render.family/slug)
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

(deftest region-store-lane-and-pick-terminal-stay-session-free
  (let [region-value (fixture-region)
        tree (rt/rt-node
              :root :group {:x 10.0 :y 20.0 :w 500.0 :h 300.0}
              :children
              [(rt/rt-node :viewport :region3d
                           {:x 30.0 :y 40.0 :w 320.0 :h 180.0}
                           :data {:address :region/address
                                  :region3d/id :region/a
                                  :region3d/scene region-value})])
        registry (-> (containers/empty-registry)
                     (containers/add-container
                      :world {:x 100.0 :y 50.0 :scale 1.0
                              :layer 1 :sibling-rank 1}))
        effective (containers/effective registry)
        slot-index (containers/transport-slot registry :world)
        scene-store (store/upsert-slot
                     (store/empty-store) :region/vi
                     {:tree tree :container :world
                      :container-slot slot-index
                      :stack-path (:stack-path (get effective :world))})
        frame (store/derive-store-frame scene-store)
        hit (store/pick scene-store effective [145.0 115.0])]
    (is (= 1 (count (:regions frame))))
    (is (= 1 (get-in frame [:ops-count-by-vi :region/vi :regions])))
    (is (= :region/a (get-in frame [:regions 0 :region-id])))
    (is (= :region3d (:route hit)))
    (is (= :region/a (:region-id hit)))
    (is (= [5.0 5.0] (:region-local hit)))
    (is (not (contains? hit :camera))
        "the scene store never sees session camera state")))

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
                       :direction [0.0 0.0 -1.0]})
        gizmo-pick (region/pick-region
                    {:maintained maintained :camera camera
                     :region-point [320.0 180.0]
                     :gizmo-handles [{:handle/id :translate/x
                                      :object-id :near
                                      :position [0.0 0.0 0.5]}]})
        sun-screen (:screen (region/project-point camera [4.0 8.0 6.0]))
        glyph-pick (region/pick-region
                    {:maintained maintained :camera camera
                     :region-point (update sun-screen 0 +
                                           (dec region/glyph-hit-radius-px))})]
    (is (= :near (:object-id center)))
    (is (= :near (:object-id hit)))
    (is (< 7.0 (:t center) 8.0))
    (is (= (:t hit) (:t center)))
    (is (= :near (:object-id boundary-hit)))
    (is (true? (:boundary? boundary-hit)))
    (is (= {:route :gizmo :object-id :near
            :handle-id :translate/x :point3 [0.0 0.0 0.5]}
           gizmo-pick))
    (is (= :object-glyph (:route glyph-pick)))
    (is (= :sun (:object-id glyph-pick)))
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

(deftest felt-camera-and-gizmo-controls-match-the-painted-affordance
  (let [maintained (assoc (region/derive-scene (fixture-region))
                          :region-id :region/a)
        view (assoc (get-in maintained [:region :view-default])
                    :yaw 0.52 :pitch 0.34)
        camera (region/camera-matrices view [720.0 480.0])
        effective (:effective-transforms maintained)
        translate (region/gizmo-handles effective camera :near :translate)
        rotate (region/gizmo-handles effective camera :near :rotate)
        scale (region/gizmo-handles effective camera :near :scale)
        z-handle (last (filter #(= [:translate :z] (:handle/id %)) translate))
        z-screen (:screen (region/project-point camera (:position z-handle)))
        z-pick (region/pick-region
                {:maintained maintained :camera camera
                 :region-point (update z-screen 0 + 8.0)
                 :gizmo-handles translate})
        orbit-step (region/orbit view 100.0 100.0)
        orbit-clamped (region/orbit view 0.0 100000.0)]
    (is (> (second (:eye camera)) (second (:pivot view)))
        "the felt camera opens above its pivot")
    (is (= 0.5 (- (:yaw orbit-step) (:yaw view)))
        "orbit does not jump a radian per 100px")
    (is (= region/orbit-pitch-limit (:pitch orbit-clamped))
        "the camera stays away from the disorienting pole")
    (is (= #{[:translate :x] [:translate :y] [:translate :z]
             [:translate :xy] [:translate :xz] [:translate :yz]}
           (set (map :handle/id translate))))
    (is (= #{[:rotate :x] [:rotate :y] [:rotate :z] [:rotate :view]}
           (set (map :handle/id rotate))))
    (is (= #{[:scale :x] [:scale :y] [:scale :z] [:scale :uniform]}
           (set (map :handle/id scale))))
    (is (> (count (filter #(= [:rotate :z] (:handle/id %)) rotate)) 32)
        "the painted ring, not one endpoint, is pickable")
    (is (= [:translate :z] (:handle-id z-pick))
        "the declared screen slop reaches the visible Z axis")))

(deftest s3-hierarchy-gizmo-settle-is-one-constant-grain-diff-and-fenced
  (let [parent (fixture/mesh-object :parent nil [0.0 0.0 0.0])
        child (fixture/mesh-object :child :parent [2.0 0.0 0.0])
        small (fixture/region {:parent parent :child child})
        large-objects
        (into {:parent parent :child child}
              (for [index (range 48)
                    :let [id (keyword (str "extra-" index))]]
                [id (fixture/mesh-object id nil [100.0 index 0.0])]))
        large (fixture/region large-objects)
        before (:transform parent)
        after (assoc before :translation [3.0 0.0 0.0])
        diff-small (material/edit-diff
                    {:op :region3d/set-transform :region-id :region/a
                     :object-id :parent :before before :after after})
        diff-large (material/edit-diff
                    {:op :region3d/set-transform :region-id :region/a
                     :object-id :parent :before before :after after})
        maintained (region/maintain-scene (region/derive-scene small)
                                          diff-small)
        replayed (material/apply-edit small diff-small)
        replayed-maintained (region/derive-scene replayed)
        camera (region/camera-matrices (:view-default replayed)
                                       [640.0 360.0])
        maintained-pick (region/pick-region
                         {:maintained maintained :camera camera
                          :region-point [320.0 180.0]})
        replayed-pick (region/pick-region
                       {:maintained replayed-maintained :camera camera
                        :region-point [320.0 180.0]})
        reparent-diff (material/edit-diff
                       {:op :region3d/set-parent :region-id :region/a
                        :object-id :child :before :parent :after nil})
        reparented (region/maintain-scene maintained reparent-diff)
        reparent-oracle (region/derive-scene
                         (material/apply-edit replayed reparent-diff))
        child-origin (region/transform-point
                      (get (:effective-transforms maintained) :child)
                      [0.0 0.0 0.0])]
    (is (= (:payload diff-small) (:payload diff-large))
        "scene population cannot enter the edit payload")
    (is (= #{:parent :child}
           (get-in maintained [:receipt :affected-object-ids])))
    (is (= 2 (get-in maintained [:receipt :instance-uploads])))
    (is (= [5.0 0.0 0.0] child-origin))
    (is (= maintained-pick replayed-pick))
    (is (= [2.0 0.0 0.0]
           (region/transform-point
            (get (:effective-transforms reparented) :child)
            [0.0 0.0 0.0])))
    (is (= (get-in reparented [:effective-transforms :child])
           (get-in reparent-oracle [:effective-transforms :child])))
    (is (region/scene-equivalent? maintained))
    (is (= (:payload diff-small)
           (:payload (material/edit-diff
                      {:op :region3d/set-transform :region-id :region/a
                       :object-id :parent :before before :after after}))))
    (is (= 50 (count (:scene (material/validate-region! large)))))))

(deftest s4-lit-color-oracle-is-linear-premultiplied-and-alpha-survives
  (let [derived (region/derive-scene (fixture-region))
        material-row (assoc material/default-material
                            :base-color (fixture/tagged 0.7 0.2 0.1 0.5)
                            :metallic 0.3 :roughness 0.45)
        shaded (region/shade-reference
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
    (is (= (/ 2.0 255.0) region/brdf-readback-error))
    (is (= :khronos-pbr-neutral-v1 region/tone-map-algorithm-version))))

(deftest s5-camera-wake-has-zero-instance-upload-and-shadow-edge-is-declared
  (let [derived (region/derive-scene (fixture-region))
        camera-wake (region/maintain-camera
                     derived (region/orbit (get-in derived [:region :view-default])
                                            4.0 -2.0))
        pass (region/region-pass-fragment
              {:region-id :region/a :size [768 512] :shadow? true})
        shadow (first (:passes pass))
        interior (second (:passes pass))]
    (is (= {:region-encodes 1 :instance-uploads 0
            :bvh-refits 0 :two-d-uploads 0}
           (:receipt camera-wake)))
    (is (= :shadow (:region/role shadow)))
    (is (= :interior (:region/role interior)))
    (is (= [(:pass/id shadow)] (:producer-edges interior)))
    (is (= :depth
           (get-in pass [:resources [:region3d/depth :region/a] :kind])))
    (is (some? (region/shadow-light-space derived)))))
