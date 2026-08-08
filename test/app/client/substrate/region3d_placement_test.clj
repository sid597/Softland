(ns app.client.substrate.region3d-placement-test
  (:require [app.client.substrate.path-material-test :as path-fixture]
            [app.client.substrate.region3d-material :as region-material]
            [app.client.substrate.region3d-placement :as placement]
            [app.client.substrate.region3d-scene :as region-scene]
            [app.client.workspace.containers :as containers]
            [app.client.workspace.rect-tree :as rt]
            [app.client.workspace.scene-store :as scene-store]
            [app.client.workspace.text-layout :as text-layout]
            [app.client.workspace.text-layout-planes :as text-layout-planes]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]))

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

(defn- fixture-store []
  (let [layout (text-layout/layout
                {:text "ABC" :source-id :text/shared :source-revision 7
                 :font-size 10.0 :char-advance 6.0 :line-height 12.0
                 :baseline-offset 9.0 :origin [0.0 0.0]})
        text-op {:text "ABC" :x 0.0 :y 9.0
                 :size 10.0 :r 0.2 :g 0.4 :b 0.8 :a 0.75
                 :layout-result layout
                 :layout-line-id (get-in layout [:lines 0 :line/id])}
        text-tree
        (rt/rt-node :root :group {:x 0.0 :y 0.0 :w 100.0 :h 40.0}
                    :data {:address :text/shared}
                    :children
                    [(rt/rt-node :background :rect
                                 {:x 0.0 :y 0.0 :w 100.0 :h 40.0}
                                 :data {:address :text/shared})
                     (rt/rt-node :text :text
                                 {:x 0.0 :y 0.0 :w 18.0 :h 12.0}
                                 ;; Contract T's line-paint output is flat.
                                 ;; This is the exact T2 fixture spelling.
                                 :text [text-op]
                                 :data {:address :text/shared
                                        :layout/id (:layout/id layout)})])
        ink (path-fixture/ink-material)
        ink-tree (rt/rt-node :ink :path {:x 0.0 :y 0.0 :w 40.0 :h 20.0}
                              :data {:address :ink/shared
                                     :path/material ink})
        region-tree
        (rt/rt-node :region :region3d
                    {:x 20.0 :y 30.0 :w 400.0 :h 240.0}
                    :data {:address :region/shared
                           :region3d/id :region/a
                           :region3d/scene
                           (region {:text/a (placed-object :text/a :text
                                                          :text/shared)
                                    :ink/a (placed-object :ink/a :ink
                                                         :ink/shared)})})]
    (-> (scene-store/empty-store)
        (scene-store/upsert-slot :text/vi
                                 {:tree text-tree :container 0 :container-slot 0})
        (scene-store/upsert-slot :ink/vi
                                 {:tree ink-tree :container 0 :container-slot 0})
        (scene-store/upsert-slot :region/vi
                                 {:tree region-tree :container 0 :container-slot 0}))))

(deftest s1-one-identity-two-spaces-and-declared-absence
  (let [store (fixture-store)
        frame (scene-store/derive-store-frame store)
        regions (:regions frame)
        [region-op] regions
        by-id (into {} (map (juxt :object-id identity))
                    (:region3d/resolved-placements region-op))]
    (is (= {:placements 2 :resolved 2 :ref-absent 0 :ref-ambiguous 0}
           (:region3d/placement-census region-op)))
    (is (= :text (get-in by-id [:text/a :kind])))
    (is (= :text (get-in by-id [:text/a :owner :node-id]))
        "the root/background occurrences do not become text owners")
    (is (= :region3d/placed-color-v1
           (get-in by-id [:text/a :style :color-adapter])))
    (is (= :ink (get-in by-id [:ink/a :kind])))
    (is (= {:object/id :text/a :object/kind :text :parent nil
            :transform region-material/default-transform
            :provenance {:asserted-by :sid}
            :text {:ref {:address :text/shared}
                   :params {:color nil :max-inline-size nil}}}
           (get-in by-id [:text/a :object]))
        "the stored placement is the exact written ref shape, never copied text")
    (is (= [:session :text/shared :layout/preedit]
           (placement/session-layout-key
            {:address :text/shared :revision :layout/preedit})))
    (testing "dead and multiply-owned refs fail closed"
      (let [dead (placement/resolve-placed-object
                  (placed-object :dead :text :dead) {} {})
            text-index {:text/shared [{:address :text/shared :layout {}}
                                      {:address :text/shared :layout {}}]}
            ambiguous (placement/resolve-placed-object
                       (placed-object :many :text :text/shared) text-index {})]
        (is (= :ref-absent (:status dead)))
        (is (= :ref-ambiguous (:status ambiguous)))))))

(deftest s2-perspective-picks-and-one-path-cache-share-the-paint-truth
  (let [store (fixture-store)
        region-op (first (placement/resolve-placed-refs
                          (scene-store/ordered-slots store)))
        resolved (into {} (map (juxt :object-id identity))
                       (:region3d/resolved-placements region-op))
        matrix region-scene/identity-mat4
        ray {:origin [6.0 -1.0 5.0] :direction [0.0 0.0 -1.0]}
        text (assoc (get resolved :text/a) :matrix matrix)
        ink (assoc (get resolved :ink/a) :matrix matrix)
        text-hit (placement/pick-placed-text text ray)
        ink-hit (placement/pick-placed-ink ink ray)
        first-pack (placement/pack-placed-ink {} ink)
        second-pack (placement/pack-placed-ink (:cache first-pack) ink)]
    (is (= :placed-text (:route text-hit)))
    (is (= :text/shared (:address text-hit)))
    (is (= [6.0 1.0] (:material-local text-hit)))
    (is (= :placed-ink (:route ink-hit)))
    (is (= :placed-ink
           (:route (region-scene/pick-region
                    {:maintained (region-scene/derive-scene
                                  (region {:text/a (placed-object :text/a :text
                                                                 :text/shared)
                                           :ink/a (placed-object :ink/a :ink
                                                                :ink/shared)}))
                     :camera (region-scene/camera-matrices
                              region-material/default-view [400.0 240.0])
                     :region-point [320.0 144.0]
                     :placements [ink]
                     :placement-picker (fn [_ _] ink-hit)}))))
    (is (seq (get-in first-pack [:pack :triangles])))
    (is (seq (:derived-keys first-pack)))
    (is (empty? (:derived-keys second-pack))
        "the caller-owned cache is the one tessellation cell")))

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

(deftest s5-settled-layout-key-is-a-value-door-through-the-one-seam
  (let [placement {:address :text/shared
                   :content-revision [:content 1]
                   :text "ABC"
                   :style {:font-size 10.0 :max-inline-size nil}
                   :layout (text-layout/layout
                            {:text "ABC" :font-size 10.0 :char-advance 6.0
                             :line-height 12.0 :origin [0.0 0.0]})}
        font-assets {:layout-provider nil}
        layout (placement/layout-placed-text placement font-assets)]
    (is (= [:text/shared [:content 1] {}]
           (placement/settled-layout-key placement font-assets)))
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

(deftest s4-record-replay-and-shuffled-registration-preserve-one-model
  (let [store (fixture-store)
        entries (mapv (fn [[vi slot]]
                        [vi (select-keys slot
                                         [:tree :container :container-slot
                                          :stack-path :meta :stratum])])
                      (:slots store))
        shuffled (reduce (fn [s [vi row]]
                           (scene-store/upsert-slot s vi row))
                         (scene-store/empty-store)
                         (reverse entries))
        replayed (-> store pr-str edn/read-string scene-store/rebuild-ordered)
        frame (scene-store/derive-store-frame store)
        shuffled-frame (scene-store/derive-store-frame shuffled)
        replayed-frame (scene-store/derive-store-frame replayed)]
    (is (= (:regions frame) (:regions shuffled-frame)))
    (is (= (:regions frame) (:regions replayed-frame)))
    (is (= (mapv :vi (scene-store/ordered-slots store))
           (mapv :vi (scene-store/ordered-slots shuffled))
           (mapv :vi (scene-store/ordered-slots replayed))))
    (is (= {:placements 2 :resolved 2 :ref-absent 0 :ref-ambiguous 0}
           (get-in frame [:regions 0 :region3d/placement-census])))))

(deftest s5-unrelated-store-change-leaves-region-placement-projection-asleep
  (let [store (fixture-store)
        before (:regions (scene-store/derive-store-frame store))
        unrelated (rt/rt-node :unrelated :rect
                              {:x 0.0 :y 0.0 :w 8.0 :h 8.0}
                              :style {:bg [1.0 0.0 0.0 1.0]}
                              :data {:address :unrelated/material})
        after (:regions
               (scene-store/derive-store-frame
                (scene-store/upsert-slot
                 store :unrelated/vi
                 {:tree unrelated :container 0 :container-slot 0})))]
    (is (= before after)
        "a non-placed material does not change any region placement value")))
