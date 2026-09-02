(ns app.client.region3d.component-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [app.client.region3d.component :as component]
            [app.client.region3d.scene :as scene]))

(defn tagged
  ([r g b] (tagged r g b 1.0))
  ([r g b a]
   {:rgba [r g b a] :color-space :srgb :alpha-association :straight}))

(defn mesh-object
  ([object-id] (mesh-object object-id nil [0.0 0.0 0.0]))
  ([object-id parent translation]
   {:object/id object-id :object/kind :mesh :parent parent
    :transform {:translation translation
                :rotation [0.0 0.0 0.0 1.0]
                :scale [1.0 1.0 1.0]}
    :provenance {:asserted-by :sid :act :fixture}
    :mesh {:kind :box :params {:size [1.0 1.0 1.0]}}
    :component {:base-color (tagged 0.5 0.6 0.7)
               :metallic 0.2 :roughness 0.55
               :emissive (tagged 0.0 0.0 0.0)}}))

(defn light-object [object-id kind light]
  {:object/id object-id :object/kind :light :parent nil
   :transform component/default-transform
   :provenance {:asserted-by :sid}
   :light (assoc light :kind kind)})

(defn text-object [object-id]
  {:object/id object-id :object/kind :text :parent nil
   :transform component/default-transform
   :provenance {:asserted-by :sid}
   :text {:ref {:address :text/shared}
          :params {}}})

(defn region
  ([] (region {:parent (mesh-object :parent)}))
  ([objects]
   {:region/id :region/seam
    :region/revision "region-seam-r1"
    :region3d/version 1
    :extent {:width 640.0 :height 360.0 :depth 100.0}
    :background {:kind :opaque :color (tagged 0.1 0.12 0.15)}
    :ambient {:color (tagged 1.0 1.0 1.0) :intensity 0.1}
    :view-default component/default-view
    :scene objects
    :region/rect {:x 0.0 :y 0.0 :w 640.0 :h 360.0}}))

(defn rejection-data [thunk]
  (try
    (thunk)
    nil
    (catch clojure.lang.ExceptionInfo error
      (ex-data error))))

(deftest region-grammar-is-closed-data-after-pure-canonicalization
  (let [input (region)
        canonical (component/validate-region! input)
        round-tripped (edn/read-string (pr-str canonical))
        before-derived (scene/derive-scene canonical)
        after-derived (scene/derive-scene
                       (component/validate-region! round-tripped))]
    (is (= 2 (:region3d/version canonical)))
    (is (= 2 component/schema-version))
    (is (= component/default-view (:view canonical)))
    (is (not (contains? canonical :view-default)))
    (is (= [0.0 0.0 0.0 1.0]
           (get-in canonical [:scene :parent :transform :rotation])))
    (is (nil? (meta round-tripped)))
    (is (= canonical (component/validate-region! round-tripped)))
    (is (= (:instances before-derived) (:instances after-derived)))
    (is (= (:bvh before-derived) (:bvh after-derived)))
    (testing "a near-unit quaternion normalizes before the grammar checks it"
      (let [accepted (assoc-in input [:scene :parent :transform :rotation]
                               [0.0 0.0 0.0 1.0005])]
        (is (= [0.0 0.0 0.0 1.0]
               (get-in (component/validate-region! accepted)
                       [:scene :parent :transform :rotation])))))))

(deftest region-grammar-rejects-every-r2-boundary-by-name
  (testing "an unknown top-level field"
    (let [data (rejection-data
                #(component/validate-region!
                  (assoc (region) :future/shape :closed)))]
      (is (= :schema/unknown-key (:error-type data)))
      (is (= [:future/shape] (:path data)))))

  (testing "a parent cycle names its cycle point"
    (let [data (rejection-data
                #(component/validate-region!
                  (region {:a (mesh-object :a :b [0.0 0.0 0.0])
                           :b (mesh-object :b :a [0.0 0.0 0.0])})))]
      (is (= :region/parent-cycle (:error-type data)))
      (is (contains? #{:a :b} (:cycle-at data)))))

  (testing "a missing parent"
    (let [data (rejection-data
                #(component/validate-region!
                  (region {:child (mesh-object :child :missing
                                               [0.0 0.0 0.0])})))]
      (is (= :region/parent-missing (:error-type data)))
      (is (= :missing (:parent data)))))

  (testing "a reversed spot cone"
    (let [spot (light-object
                :spot :spot
                {:color (tagged 1.0 1.0 1.0)
                 :intensity 1.0 :range 10.0 :cast-shadow false
                 :cone {:inner-deg 40.0 :outer-deg 20.0}})
          data (rejection-data
                #(component/validate-region! (region {:spot spot})))]
      (is (= :region/light-cone (:error-type data)))))

  (testing "a torus whose tube reaches its radius"
    (let [torus (assoc (mesh-object :torus)
                       :mesh {:kind :torus
                              :params {:radius 0.5 :tube 0.5
                                       :radial-segments 32
                                       :tubular-segments 16}})
          data (rejection-data
                #(component/validate-region! (region {:torus torus})))]
      (is (= :region/primitive-kind (:error-type data)))))

  (testing "an index outside the vertex population"
    (let [indexed (assoc
                   (mesh-object :indexed)
                   :mesh {:kind :indexed-triangles
                          :positions [0.0 0.0 0.0
                                      1.0 0.0 0.0
                                      0.0 1.0 0.0]
                          :normals [0.0 0.0 1.0
                                    0.0 0.0 1.0
                                    0.0 0.0 1.0]
                          :indices [0 1 3]})
          data (rejection-data
                #(component/validate-region! (region {:indexed indexed})))]
      (is (= :region/mesh-index (:error-type data)))))

  (testing "a quaternion outside the normalization tolerance"
    (let [data (rejection-data
                #(component/validate-region!
                  (assoc-in (region)
                            [:scene :parent :transform :rotation]
                            [0.0 0.0 0.0 1.01])))]
      (is (= :region/quaternion-unit (:error-type data)))))

  (testing "a point light casting a shadow"
    (let [point (light-object
                 :point :point
                 {:color (tagged 1.0 1.0 1.0)
                  :intensity 1.0 :range 10.0 :cast-shadow true})
          data (rejection-data
                #(component/validate-region! (region {:point point})))]
      (is (= :region/light-shadow (:error-type data)))))

  (testing "a placed text body with an extra key"
    (let [placed (assoc-in (text-object :placed-text)
                           [:text :future/shape] :closed)
          data (rejection-data
                #(component/validate-region!
                  (region {:placed-text placed})))]
      (is (= :schema/unknown-key (:error-type data)))))

  (testing "an unsupported region version"
    (let [data (rejection-data
                #(component/validate-region!
                  (assoc (region) :region3d/version 3)))]
      (is (= :schema/invalid-value (:error-type data)))
      (is (= [:region3d/version] (:path data))))))

(deftest primitive-generators-are-deterministic-general-triangle-projections
  (doseq [[kind params] component/primitive-defaults]
    (let [first (scene/primitive-mesh {:kind kind :params params})
          second (scene/primitive-mesh {:kind kind :params params})]
      (is (= first second) (str kind " is deterministic"))
      (is (pos? (count (:positions first))))
      (is (= (count (:positions first)) (count (:normals first))))
      (is (zero? (mod (count (:indices first)) 3)))
      (is (< (apply max (:indices first))
             (quot (count (:positions first)) 3)))))
  (let [general {:kind :indexed-triangles
                 :positions [0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0 0.0]
                 :normals [0.0 0.0 1.0 0.0 0.0 1.0 0.0 0.0 1.0]
                 :indices [0 1 2]}]
    (is (= [0 1 2]
           (get-in (component/canonical-object
                    (assoc (mesh-object :indexed) :mesh general))
                   [:mesh :indices])))
    (is (= :region/mesh-index
           (:error-type
            (rejection-data
             #(component/validate-region!
               (region {:indexed (assoc (mesh-object :indexed)
                                        :mesh (assoc general :indices [0 1 3]))}))))))))
