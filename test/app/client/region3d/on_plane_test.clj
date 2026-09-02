(ns app.client.region3d.on-plane-test
  (:require [app.client.region3d.component :as region-component]
            [app.client.region3d.on-plane :as on-plane]
            [app.client.region3d.scene :as region-scene]
            [app.client.engine.transform :as transform]
            [app.client.text.layout :as text-layout]
            [app.client.text.layout-planes :as text-layout-planes]
            [clojure.test :refer [deftest is]]))

(defn- placed-object [id kind address]
  (merge {:object/id id :object/kind kind :parent nil
          :transform region-component/default-transform
          :provenance {:asserted-by :sid}}
         (case kind
           :text {:text {:ref {:address address}
                         :params {:color nil :max-inline-size nil}}}
           :ink {:ink {:ref {:address address}}})))

(defn- region [objects]
  {:region3d/version 2
   :extent {:width 400.0 :height 240.0 :depth 100.0}
   :background region-component/default-background
   :ambient region-component/default-ambient
   :view-default region-component/default-view
   :scene objects})

(deftest s5-placed-layout-keeps-the-one-glyph-accessor-seam
  (let [placement {:address :text/shared
                   :content-revision [:content 1]
                   :text "ABC"
                   :style {:font-size 10.0 :max-inline-size nil}
                   :layout (text-layout/layout
                            {:text "ABC" :font-size 10.0 :char-advance 6.0
                             :line-height 12.0 :origin [0.0 0.0]})}
        font-assets {:layout-provider nil}
        layout (on-plane/layout-placed-text placement font-assets)]
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
        world-transforms {0 {:affine transform/identity-affine :flags 0}}
        region-draw-item {:address :region/shared :region-id :region/a
                   :container 0 :x 20.0 :y 30.0 :w 400.0 :h 240.0
                   :region3d/scene scene}
        result (on-plane/project-region-anchor
                {:binding {:bind :region-object :region :region/shared
                           :object :mesh/a :local [0.4 0.25 0.3]}
                 :region-draw-item region-draw-item :maintained maintained :camera camera
                 :world-transforms world-transforms :anchor-container 0})]
    (is (= :resolved (:status result)))
    (is (= [0.4 0.25 0.3] (:point3 result))
        "the required nonzero local point participates in projection")
    (is (false? (:anchor-clamped result)))
    (is (every? number? (:center result)))))
