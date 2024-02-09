(ns app.electric-flow
  (:import [hyperfiddle.electric Failure Pending])
  (:require contrib.str
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.background :as bg]
            [app.flow-calc :as fc]
            [app.electric-codemirror :as cm]
            [app.client.shapes.circle :refer [circle]]
            [app.client.shapes.rect :refer [rect]]
            [app.mode :refer [theme]]
            [app.client.shapes.util :as sutil :refer [new-line-el]]
            #?@(:cljs
                [[clojure.string :as str]
                 [missionary.core :as m]]

                :clj
                [[missionary.core :as m]
                 [com.rpl.rama :as r]
                 [app.server.llm :as llm :refer [chat-complete]]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]
                 [clojure.core.async :as a :refer [<! >! go]]])))




#_(defn log [message & args]
    (js/console.log message args))


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


(e/defn reset-global-vals []
  (e/client
    (cond
      (some? @!context-menu?) (reset! !context-menu? nil))))


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


(e/defn view []
  (let [current-selection (atom nil)]
    (e/client
      (theme-toggle.)
      (when (some? context-menu?)
        (println "context menu is " @!context-menu? (some? @!context-menu?))
        (context-menu.))
      (svg/svg
        (dom/props {:viewBox (clojure.string/join " " viewbox)
                    :id "sv"
                    :style {:min-width "100%"
                            :min-height "100%"
                            :z-index 1
                            :background-color (:svg-background (theme. ui-mode))
                            :top 0
                            :left 0}})
        (dom/on "contextmenu" (e/fn [e]
                                (e/client
                                  (println "contextmenu" @!context-menu?)
                                  (reset! !context-menu? {:x (str (.-clientX e) "px")
                                                           :y (str (.-clientY e) "px")})
                                  (.preventDefault e)
                                  (println "contextmenu" @!context-menu?))))

        (dom/on "mousemove" (e/fn [e]
                              (when (= 0
                                      (.-button e))
                                (when @!border-drag?
                                  (let [el (.getElementById js/document "sv")
                                        [x y] (fc/element-new-coordinates1 e el)]
                                    (println "border draging" x y)
                                    (e/server
                                     (swap! !edges assoc-in [:raw :x2] x)
                                     (swap! !edges assoc-in [:raw :y2] y))))

                                (cond
                                  (and is-dragging?
                                    (= "background"
                                      (:selection
                                        @current-selection))
                                    (:movable?
                                      @current-selection))    (let [svg-el (.getElementById js/document "sv")
                                                                    [nx ny] (fc/find-new-coordinates e last-position viewbox svg-el)
                                                                    ex      (.-clientX e)
                                                                    ey      (.-clientY e)]
                                                                (println "gg")
                                                                (swap! !viewbox assoc 0 nx)
                                                                (swap! !viewbox assoc 1 ny)
                                                                (reset! !last-position {:x ex
                                                                                        :y ey}))
                                  #_#_@!border-drag? (println "border draging")))))
        (dom/on "mousedown" (e/fn [e]
                                (.preventDefault e)
                                (when (= 0
                                        (.-button e))
                                  (let [ex (e/client (.-clientX e))
                                        ey (e/client (.-clientY e))]
                                    (println "pointerdown svg")
                                    (reset! current-selection {:selection (.-id (.-target e))
                                                               :movable? true})
                                    (reset! !last-position {:x ex
                                                            :y ey})
                                    (reset! !is-dragging? true)
                                    (reset-global-vals.)))))
        (dom/on "wheel" (e/fn [e]
                          (when (= (.-id (.-target e))
                                  "background")
                            (.preventDefault e)
                            (println "wheeled" viewbox (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv")))
                            (let [coords (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv"))
                                  wheel   (if (< (.-deltaY e) 0)
                                            1.01
                                            0.99)
                                  new-view-box  (fc/direct-calculation viewbox wheel coords)
                                  new-zoom-level (* zoom-level wheel)]
                                (reset! !zoom-level new-zoom-level)
                                (reset! !viewbox new-view-box)))))

        (dom/on "mouseup" (e/fn [e]
                               (.preventDefault e)
                               (when (= 0
                                       (.-button e))
                                 (println "pointerup svg")
                                 (reset! !is-dragging? false)
                                 (reset! !border-drag? false))
                              (when @!border-drag?
                                (println "border draging up >>>")
                                (reset! !border-drag? false))))

        (bg/dot-background. (:svg-dots (theme. ui-mode)) viewbox)

        (e/server
          (e/for-by identity [node-id (new (!subscribe (keypath :main) nodes-pstate))]
            (let [type (subscribe. [:main (first node-id) :type])]
             (e/client
               (println "type" type (= "circle" type))
               (rect. (first node-id))
               #_(cond
                       (= "circle" type)  (circle. node-id)
                       (= "rect" type)    (rect. node-id nodes-pstate)))))
          #_(e/for-by identity [edge edges]
              (let [[_ {:keys [type x2 y2]}] edge]
                (e/client
                  (println "edge type" type edge)
                  (cond
                    (and
                      (= "raw" type)
                      (not-every? nil? [x2 y2])) (new-line-el. edge)
                    (= "line" type)              (line. edge))))))))))


(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (view.))))