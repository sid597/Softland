(ns judge-claude
  (:require [judge-common :as j]
            [app.client.engine.color :as color]
            [app.client.engine.device :as device]
            [app.client.path.component :as c]
            [app.client.path.records :as r]
            [app.client.path.renderer :as renderer]
            [app.client.harness.shared :as shared]))

(defn run! [gpu]
  (let [camera (device/create-camera-buffer gpu) groups (device/create-groups-buffer gpu)
        system (renderer/init-path-system gpu "rgba8unorm-srgb" camera groups :scene-color (color/scene-color true))
        cold (renderer/init-path-system gpu "rgba8unorm-srgb" camera groups :scene-color (color/scene-color true))
        worlds (j/transforms)
        recipe (-> r/harness-z (assoc :path/revision 2)
                   (assoc :path/construction (c/default-construction r/z-as-dabs)))
        nonlinear {:path/material-id :nonlinear :path/revision 1
                   :path/tool {:size 20 :fit :polyline :streamline 0}
                   :path/source {:kind :pen :samples [[20.5 30.5 0.0] [40.5 30.5 1.0]]}
                   :path/paint {:stroke {:overlap :union :tip :nib :width "size * p^2" :unit :local
                                         :cap :round :join :round :color [1 1 1 0.62]}}}
        border (-> r/border (assoc :path/material-id :screen-border :path/snap? false)
                   (update :path/paint dissoc :fill)
                   (assoc-in [:path/paint :stroke :color] [1 1 1 1]))
        captures [{:label "recipe-before" :record r/harness-z}
                  {:label "recipe-edited-retained" :record recipe}
                  {:label "recipe-edited-cold" :record recipe :system cold}
                  {:label "nonlinear-union" :record nonlinear :points [[30 35] [40 30]]}
                  {:label "screen-border-z1" :record border :group 17 :points [[24 60]]}
                  {:label "screen-border-z2" :record border :group 17 :zoom 2 :points [[24 60]]}]
        old-run (c/run r/harness-z {})]
    (-> (shared/promise-mapv
         (fn [{:keys [label record zoom group points] :as spec}]
           (j/capture! gpu (or (:system spec) system)
                       {:id :judge :path/material record :container (or group 0)} (or zoom 1) worlds
                       (fn [sys items zoom wt] (renderer/prepare-path-frame! sys items {:zoom zoom :pan [0 0]} wt))
                       (fn [pass sys _] (renderer/draw-path-frame! pass sys)) label (or points [[64 64]]))) captures)
        (.then (fn [rows] {:probes rows :screen-transform (get worlds 17)
                          :recipe {:fresh-region-count (count (:regions (c/run recipe {})))
                                   :rerun-decision (c/rerun? recipe {} (:reads old-run))}
                          :nonlinear-cpu (c/classify nonlinear [30.5 35.5])}))
        (.finally (fn [] (renderer/destroy-path-system! system) (renderer/destroy-path-system! cold)
                    (.destroy camera) (.destroy groups))))))

(defn init [] (j/start! run!))
