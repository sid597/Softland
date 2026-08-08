(ns app.client.substrate.region3d-material-test
  (:require [clojure.test :refer [deftest is testing]]
            [app.client.substrate.region3d-material :as material]
            [app.client.substrate.region3d-scene :as scene]))

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
    :material {:base-color (tagged 0.5 0.6 0.7)
               :metallic 0.2 :roughness 0.55
               :emissive (tagged 0.0 0.0 0.0)}}))

(defn region
  ([] (region {:parent (mesh-object :parent)}))
  ([objects]
   {:region3d/version 1
    :extent {:width 640.0 :height 360.0 :depth 100.0}
    :background {:kind :opaque :color (tagged 0.1 0.12 0.15)}
    :ambient {:color (tagged 1.0 1.0 1.0) :intensity 0.1}
    :view-default material/default-view
    :scene objects
    :future/sculpt-node {:preserved true}}))

(deftest material-grammar-migrates-v1-and-admits-v2-placements
  (let [input (region)
        canonical (material/validate-region! input)]
    (is (= 2 (:region3d/version canonical)))
    (is (= 2 material/schema-version))
    (is (= {:preserved true} (:future/sculpt-node canonical))
        "unknown fields are preserved, never silently dropped")
    (is (= [0.0 0.0 0.0 1.0]
           (get-in canonical [:scene :parent :transform :rotation])))
    (is (= :preserve
           (get-in material/region-family-registration
                   [:versioning :unknown-field-policy])))
    (is (= :via-router
           (get-in material/object-family-citizenship [:render :order])))
    (testing "quaternion normalization is exact inside the pinned tolerance"
      (let [accepted (assoc-in input [:scene :parent :transform :rotation]
                               [0.0 0.0 0.0 1.0005])]
        (is (= [0.0 0.0 0.0 1.0]
               (get-in (material/validate-region! accepted)
                       [:scene :parent :transform :rotation]))))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"outside unit tolerance"
           (material/validate-region!
            (assoc-in input [:scene :parent :transform :rotation]
                      [0.0 0.0 0.0 1.01])))))
    (testing "cycles, untagged colors, non-directional shadows, and extents refuse"
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"cycle"
           (material/validate-region!
            (region {:a (assoc (mesh-object :a :b [0.0 0.0 0.0]) :parent :b)
                     :b (assoc (mesh-object :b :a [0.0 0.0 0.0]) :parent :a)}))))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"tagged map"
           (material/validate-region!
            (assoc-in input [:background :color] [0.0 0.0 0.0 1.0]))))
      (is (thrown-with-msg?
           clojure.lang.ExceptionInfo #"extent component"
           (material/validate-region!
            (assoc-in input [:extent :width] 10001.0))))))

  (let [text-object {:object/id :placed-text :object/kind :text :parent nil
                     :transform material/default-transform
                     :provenance {:asserted-by :sid}
                     :text {:ref {:address :text/shared}
                            :params {:color nil :max-inline-size nil
                                     :future/shape :preserved}}}
        ink-object {:object/id :placed-ink :object/kind :ink :parent nil
                    :transform material/default-transform
                    :provenance {:asserted-by :sid}
                    :ink {:ref {:address :ink/shared}}}
        v2 (assoc (region {:placed-text text-object :placed-ink ink-object})
                  :region3d/version 2)
        canonical (material/validate-region! v2)]
    (is (= :preserved
           (get-in canonical [:scene :placed-text :text :params :future/shape])))
    (is (= {:address :ink/shared}
           (get-in canonical [:scene :placed-ink :ink :ref])))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"requires :ref and :params"
         (material/validate-region!
          (assoc-in v2 [:scene :placed-text :text]
                    {:ref {:address :text/shared}}))))
    (is (= [:region3d/v1-base :region3d/v2-placements]
           (get-in material/object-family-citizenship
                   [:versioning :migration])))))

(deftest primitive-generators-are-deterministic-general-triangle-projections
  (doseq [[kind params] material/primitive-defaults]
    (let [first (scene/primitive-mesh {:kind kind :params params})
          second (scene/primitive-mesh {:kind kind :params params})]
      (is (= first second) (str kind " is deterministic"))
      (is (pos? (count (:positions first))))
      (is (= (count (:positions first)) (count (:normals first))))
      (is (zero? (mod (count (:indices first)) 3)))
      (is (< (apply max (:indices first))
             (quot (count (:positions first)) 3)))))
  (let [general {:object/id :indexed :object/kind :mesh :parent nil
                 :transform material/default-transform
                 :provenance {:asserted-by :importer}
                 :mesh {:kind :indexed-triangles
                        :positions [0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0 0.0]
                        :normals [0.0 0.0 1.0 0.0 0.0 1.0 0.0 0.0 1.0]
                        :indices [0 1 2]}
                 :material material/default-material}]
    (is (= [0 1 2]
           (get-in (material/canonical-object general) [:mesh :indices])))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo #"index exceeds"
         (material/canonical-object
          (assoc-in general [:mesh :indices] [0 1 3]))))))

(deftest semantic-edit-values-are-per-object-invertible-and-replayable
  (let [before material/default-transform
        after (assoc before :translation [4.0 0.0 0.0])
        diff (material/edit-diff
              {:op :region3d/set-transform
               :region-id :region/a :object-id :parent
               :before before :after after})
        edited (material/apply-edit (region) diff)
        inverse (material/edit-diff
                 {:op :region3d/set-transform
                  :region-id :region/a :object-id :parent
                  :before after :after before})
        reparent (material/edit-diff
                  {:op :region3d/set-parent
                   :region-id :region/a :object-id :child
                   :before :parent :after nil})
        parented (region {:parent (mesh-object :parent)
                          :child (mesh-object :child :parent [1.0 0.0 0.0])})]
    (is (= [:region/a :parent] (:key diff)))
    (is (= #{:region-id :object-id :before :after}
           (set (keys (:payload diff)))))
    (is (not (contains? (:payload diff) :scene)))
    (is (= [4.0 0.0 0.0]
           (get-in edited [:scene :parent :transform :translation])))
    (is (nil? (get-in (material/apply-edit parented reparent)
                      [:scene :child :parent])))
    (is (= (material/canonical-region (region))
           (material/canonical-region (material/apply-edit edited inverse))))))

(deftest placed-edit-carries-only-the-kind-submap
  (let [placed {:object/id :placed :object/kind :text :parent nil
                :transform material/default-transform
                :provenance {:asserted-by :sid}
                :text {:ref {:address :text/a}
                       :params {:color nil :max-inline-size nil}}}
        input (assoc (region {:placed placed}) :region3d/version 2)
        after {:ref {:address :text/b}
               :params {:color nil :max-inline-size 320.0}}
        diff (material/edit-diff
              {:op :region3d/set-placed :region-id :region/a
               :object-id :placed :before (:text placed) :after after})]
    (is (= after (get-in (material/apply-edit input diff)
                         [:scene :placed :text])))
    (is (= #{:region-id :object-id :before :after}
           (set (keys (:payload diff)))))))
