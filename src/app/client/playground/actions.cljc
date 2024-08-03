(ns app.client.playground.actions
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [app.client.mode :refer [theme]]
            [app.client.utils :refer [ ui-mode edges nodes
                                      is-dragging?  zoom-level last-position subscribe
                                      viewbox  context-menu? reset-global-vals new-uuid]]
            [app.client.shapes.util :as sutil :refer [create-new-child-node]]

            #?@(:cljs [[app.client.utils :refer [!border-drag? !is-dragging? !zoom-level !last-position !viewbox !context-menu?]]]
                :clj
                [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.client.utils :refer [!ui-mode !edges !nodes]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]])))




(e/defn context-menu []
  (e/client
    (println "called context menu" (type (:x context-menu?)) (:y context-menu?))
    (let [x (:x context-menu?)
          y (:y context-menu?)]
      (println "context menu" x y)
      (dom/div
        (dom/props {:style {:position "absolute"
                            :z-index "1000"
                            :top  y
                            :left x
                            :background-color (:context-menu (theme. ui-mode))
                            :height "100px"
                            :padding "5px"
                            :width "100px"}
                    :id "context-menu"})
        (dom/div
          (dom/button
            (dom/props {:style {:background-color (:button-background (theme. ui-mode))
                                :color (:button-text (theme. ui-mode))
                                :border "none"
                                :border-width "5px"
                                :font-size "1em"
                                :height "40px"
                                :width "100%"}})
            (dom/text
              "New node")
            (dom/on "click" (e/fn [e]
                              (e/client
                                 (println "gg clicked")
                                 (let [id (new-uuid)
                                       [cx cy] (fc/browser-to-svg-coords e viewbox (.getElementById js/document "svg-parent-div"))
                                        node-data {id {:id id
                                                       :x cx
                                                       :y cy
                                                       :type-specific-data {:text "GM Hello"
                                                                            :width 400
                                                                            :height 800}
                                                          :type "rect"
                                                          :fill  "lightblue"}}]
                                     (println "id" id)
                                     (println "node data" cx cy)
                                     (e/server
                                       (let [event-data {:graph-name  :main
                                                         :event-id    (get-event-id)
                                                         :create-time (System/currentTimeMillis)}]
                                         (add-new-node node-data
                                                       event-data
                                                       true
                                                       true)))
                                  (reset! !context-menu? nil)))))))))))



(e/defn theme-toggle []
  (e/client
    (dom/button
      (dom/props
        {:top "100px"
         :left "1000px"
         :style {:background-color (:button-background (theme. ui-mode))
                 :color (:button-text (theme. ui-mode))
                 :border "none"
                 :margin "0px"
                 :padding "0px"
                 :font-size "10px"
                 :height "20px"
                 :width "100%"}})
      (dom/text "Theme")
      (dom/on "click" (e/fn [e]
                        (e/server
                          (reset! !ui-mode (if (= :dark @!ui-mode)
                                             :light
                                             :dark))))))))
