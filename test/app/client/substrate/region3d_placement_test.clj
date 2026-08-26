(ns app.client.substrate.region3d-placement-test
  (:require [app.client.substrate.region3d-material :as region-material]
            [app.client.substrate.region3d-placement :as placement]
            [app.client.substrate.region3d-scene :as region-scene]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.text-layout :as text-layout]
            [app.client.workspace.text-layout-planes :as text-layout-planes]
            [clojure.test :refer [deftest is]]))

(defn- placed-object [id kind address]
  (merge {:object/id id :object/kind kind :parent nil
          :transform region-material/default-transform
          :provenance {:asserted-by :sid}}
         (case kind
           :text {:text {:ref {:address address}
                         :params {:color nil :max-inline-size nil}}}
           :ink {:ink {:ref {:address address}}})))

(defn- region [objects]
  {:region3d/version 2
   :extent {:width 400.0 :height 240.0 :depth 100.0}
   :background region-material/default-background
   :ambient region-material/default-ambient
   :view-default region-material/default-view
   :scene objects})

(deftest s2-glyph-packing-uses-positioned-layout-and-atlas-metadata
  (let [layout (text-layout/layout
                {:text "A" :source-id :a :source-revision 1
                 :font-size 10.0 :char-advance 6.0 :line-height 12.0
                 :baseline-offset 9.0 :origin [0.0 0.0]})
        placement {:object-id :text/a :address :text/shared
                   :style {:font-size 10.0
                           :color (placement/adapt-legacy-color [1 1 1 1])}}
        font-assets {:atlas {:atlas {:width 64 :height 64}
                             :glyphs [{:unicode 65
                                       :planeBounds {:left 0.0 :right 0.6
                                                     :top 0.8 :bottom -0.2}
                                       :atlasBounds {:left 0.0 :right 16.0
                                                     :top 0.0 :bottom 16.0}}]}}
        [quad] (placement/pack-glyph-quads placement layout font-assets)]
    (is (= (:layout/id layout) (:layout/id quad)))
    (is (= [0.0 1.0 6.0 10.0] (:rect quad)))
    (is (= [0.0 1.0 0.25 0.75] (:uv quad)))))

(deftest s5-placed-layout-keeps-the-one-glyph-accessor-seam
  (let [placement {:address :text/shared
                   :content-revision [:content 1]
                   :text "ABC"
                   :style {:font-size 10.0 :max-inline-size nil}
                   :layout (text-layout/layout
                            {:text "ABC" :font-size 10.0 :char-advance 6.0
                             :line-height 12.0 :origin [0.0 0.0]})}
        font-assets {:layout-provider nil}
        layout (placement/layout-placed-text placement font-assets)]
    (is (= "ABC" (get-in layout [:source :text])))
    ;; Contract-T §7.5: glyph data reads exclusively through the accessor
    ;; seam — the raw :glyphs path is the named crime, not a missing field.
    (is (== 6.0 (-> (get-in layout [:lines 0])
                    text-layout-planes/line-glyphs
                    first :advance first)))))

(deftest s3-region-object-projection-carries-local-and-clamps-off-rect
  (let [object (assoc (placed-object :mesh/a :ink :unused)
                      :object/kind :empty :ink nil)
        scene (region {:mesh/a object})
        maintained (region-scene/derive-scene scene)
        camera (region-scene/camera-matrices (:view-default scene) [400.0 240.0])
        effective {0 {:affine containers/identity-affine :flags 0}}
        region-op {:address :region/shared :region-id :region/a
                   :container 0 :x 20.0 :y 30.0 :w 400.0 :h 240.0
                   :region3d/scene scene}
        result (placement/project-region-anchor
                {:binding {:bind :region-object :region :region/shared
                           :object :mesh/a :local [0.4 0.25 0.3]}
                 :region-op region-op :maintained maintained :camera camera
                 :effective-transforms effective :anchor-container 0})]
    (is (= :resolved (:status result)))
    (is (= [0.4 0.25 0.3] (:point3 result))
        "the required nonzero local point participates in projection")
    (is (false? (:anchor-clamped result)))
    (is (every? number? (:center result)))))
