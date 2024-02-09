(ns app.client.playground.actions
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [app.electric-codemirror :as cm]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.flow-calc :as fc]
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [app.mode :refer [theme]]
            [app.client.shapes.util :as sutil :refer [create-new-child-node]]

            #?@(:clj
                [[com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]])))



#?(:clj (def !ui-mode (atom :dark)))
(e/def ui-mode (e/server (e/watch !ui-mode)))

(defn new-uuid []
  (keyword (str (random-uuid))))



#?(:clj (def !edges (atom {#_#_:sv-line {:id :sv-line
                                         :type "line"
                                         :to   :sv-circle
                                         :from :sv-circle1
                                         :color "black"}})))

(e/def edges (e/server (e/watch !edges)))

#?(:clj (def !nodes (atom {#_#_:sv-circle {:id :sv-circle :dragging? false
                                           :x 700
                                           :y 100
                                           :r 100
                                           :type "circle"
                                           :color "red"}
                           #_#_:sv-circle1 {:id :sv-circle1
                                            :dragging? false
                                            :x 900
                                            :y 300
                                            :r 100
                                            :type "circle"
                                            :color "green"}
                           :rect       {:id :rect
                                        :x 500
                                        :y 600
                                        :text "GM Hello"
                                        :width 400
                                        :height 800
                                        :type "rect"
                                        :fill  "lightblue" #_"#111110" #_"#1E1523" #_"#1B1A17" #_"#111A27"}})))

(e/def nodes (e/server (e/watch {})))


#?(:cljs (def !new-line (atom {:start nil
                               :end   nil})))
#?(:cljs (def !border-drag? (atom false)))
#?(:cljs (def !is-dragging? (atom false)))
#?(:cljs (def !zoom-level (atom 1.5)))
#?(:cljs (def !last-position (atom {:x 0 :y 0})))
#?(:cljs (def !viewbox (atom [0 0 2000 2000])))

(e/def viewbox (e/client (e/watch !viewbox)))
(e/def last-position (e/client (e/watch !last-position)))
(e/def zoom-level (e/client (e/watch !zoom-level)))
(e/def is-dragging? (e/client (e/watch !is-dragging?)))
(e/def new-line (e/client (e/watch !new-line)))




(e/defn subscribe [path]
  (e/server (new (!subscribe (concat [(keypath :main)]
                               path)
                   nodes-pstate))))


#?(:cljs (def !context-menu? (atom nil)))
(e/def context-menu? (e/client (e/watch !context-menu?)))


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
                                       [cx cy] (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv"))
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
