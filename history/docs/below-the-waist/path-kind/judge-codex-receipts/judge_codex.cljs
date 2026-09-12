(ns judge-codex
  (:require [judge-common :as j]
            [app.client.engine.color :as color]
            [app.client.engine.device :as device]
            [app.client.path.component :as c]
            [app.client.path.records :as r]
            [app.client.path.pack :as pack]
            [app.client.path.renderer :as renderer]
            [app.client.harness.path-fixtures :as fixtures]
            [app.client.harness.shared :as shared]))

(defn run! [gpu]
  (let [camera (device/create-camera-buffer gpu) groups (device/create-groups-buffer gpu)
        system (renderer/init-path-system gpu "rgba8unorm-srgb" camera groups :scene-color (color/scene-color true))
        worlds (j/transforms)
        nonlinear (r/construct {:identity {:id :nonlinear :revision 1} :construction r/draw
                                :source {:samples [{:id 0 :position [20.5 30.5] :pressure 0.0}
                                                   {:id 1 :position [40.5 30.5] :pressure 1.0}]}
                                :settings {:interpolation :line}
                                :paint {:stroke {:tip :round-nib :overlap :union :width [:* 20 [:pow [:get :pressure] 2]]
                                                  :color [1 1 1 0.62]}}})
        border (r/construct {:identity {:id :screen-border :revision 1} :construction r/border
                             :source {:x 24 :y 36 :width 80 :height 52 :radius 6}
                             :paint {:stroke {:tip :round-nib :width 1 :unit :device :overlap :union
                                               :cap :butt :join :miter :align :inside :color [1 1 1 1]}}})
        mask-record {:identity {:id :mask :revision 1} :construction r/border
                     :source {:width 32.25 :height 128} :paint {:fill {:color [1 1 1 1]}}}
        mask-edited (assoc-in mask-record [:construction :steps 0 :args]
                              [{:width [:* 2 [:get :source :width]] :height [:get :source :height]}])
        union (r/construct (fixtures/z-record :union))
        use-mask (fn [record] (assoc-in union [:path/paint :clip] {:path (:path/value (r/construct record)) :rule :nonzero}))
        captures [{:label "nonlinear-union" :record nonlinear :points [[30 35] [40 30]]}
                  {:label "screen-border-z1" :record border :group 17 :points [[24 60]]}
                  {:label "screen-border-z2" :record border :group 17 :zoom 2 :points [[24 60]]}
                  {:label "recipe-mask-before" :record (use-mask mask-record) :points [[64 64] [65 64]]}
                  {:label "recipe-mask-after" :record (use-mask mask-edited) :points [[64 64] [65 64]]}]]
    (-> (shared/promise-mapv
         (fn [{:keys [label record zoom group points]}]
           (j/capture! gpu system {:id :judge :path/material record :container (or group 0)} (or zoom 1) worlds
                       renderer/prepare-path-frame!
                       (fn [pass sys stats] (renderer/draw-path-range! pass sys 0 (:vertices stats))) label points)) captures)
        (.then (fn [rows] {:probes rows :screen-transform (get worlds 17)
                          :screen-scale-z2 (pack/projected-scale (get worlds 17) 2 1)
                          :nonlinear-cpu (c/classify nonlinear [30.5 35.5])}))
        (.finally (fn [] (renderer/destroy-path-system! system) (.destroy camera) (.destroy groups))))))

(defn init [] (j/start! run!))
