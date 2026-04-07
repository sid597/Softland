(ns app.client.core
  (:require [contrib.str :refer [pprint-str]]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.rect.core :refer [rect]]
            [app.client.shapes.draw-rect :refer [draw-rect]]
            [app.client.quad-tree :refer [total-forces]]
            [app.client.shapes.line :refer [line]]
            #?@(:cljs
                [[clojure.string :as str]
                 [app.client.shapes.line :refer [edge-update]]
                 [global-flow :refer [!all-nodes-map debounce !quad-tree global-client-flow !node-pos-atom node-pos-flow !global-atom current-time-ms]]
                 [app.client.rect.core :refer [server-update]]
                 [app.client.quad-tree :refer [build-quad-tree approximate-force total-forces]]
                 [app.client.flow-calc :refer [browser-to-svg-coords]]
                 [contrib.electric-contrib :refer-macros [after]]
                 [missionary.core :as m]]
                :clj
                [[missionary.core :as m]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.server.rama.util-fns :refer[roam-query-request !subscribe get-path-data nodes-pstate update-node get-event-id node-ids-pstate]]])))

(e/defn view []
  (e/client
    (let [!viewbox (atom [-2028 61 1331 331])
          viewbox (e/watch !viewbox)
          !ui-mode (atom :dark)
          ui-mode (e/watch !ui-mode)]
        (dom/div
          (dom/props {:id "svg-parent-div"
                      :style {:display "flex"
                              :flex 1}})
          (reset! !viewbox [0 0 (.-clientWidth dom/node) (.-clientHeight dom/node)])
          (svg/svg
            (dom/props {:viewBox (clojure.string/join " " viewbox)
                        :id "sv"
                        :style {:z-index 1
                                :background-color (:svg-background (theme. ui-mode))
                                :top 0
                                :left 0}})

            (svg/defs
               (svg/pattern
                 (dom/props {:id "dotted-pattern"
                             :width 6
                             :height 6
                             :patternUnits "userSpaceOnUse"})
                 (svg/circle
                   (dom/props {:cx 1
                               :cy 1
                               :r 0.5
                               :fill "#91919a"}))))
            (svg/rect
               (dom/props
                 {:id "background"
                  :x (first viewbox)
                  :y (second viewbox)
                  :width (nth viewbox 2)
                  :height (nth viewbox 3)
                  :fill "url(#dotted-pattern)"})))))))


