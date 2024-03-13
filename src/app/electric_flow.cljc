(ns app.electric-flow
  (:import [hyperfiddle.electric Failure Pending])
  (:require contrib.str
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.background :as bg]
            [app.client.flow-calc :as fc]
            [app.client.electric-codemirror :as cm]
            [app.client.shapes.circle :refer [circle]]
            [app.client.shapes.rect :refer [rect]]
            [app.client.mode :refer [theme]]
            [app.client.editor.core :refer [canvas]]
            [app.client.shapes.util :as sutil :refer [new-line-el]]
            [app.client.playground.actions :refer [context-menu theme-toggle]]
            [app.client.utils :refer [ ui-mode edges nodes
                                      is-dragging?  zoom-level last-position subscribe
                                       viewbox  context-menu? reset-global-vals]]
            #?@(:cljs
                [[clojure.string :as str]
                 [app.client.utils :refer [!border-drag? !is-dragging? !zoom-level !last-position !viewbox !context-menu?]]
                 [missionary.core :as m]]
                :clj
                [[missionary.core :as m]
                 [com.rpl.rama :as r]
                 [app.server.llm :as llm :refer [chat-complete]]
                 [app.client.utils :refer [!ui-mode !edges !nodes]]
                 [com.rpl.rama.path :as path :refer [subselect ALL FIRST keypath select]]
                 [app.server.rama :as rama :refer [!subscribe nodes-pstate get-event-id add-new-node]]
                 [clojure.core.async :as a :refer [<! >! go]]])))



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
                                    (reset! current-selection {:selection (.-id (.-target e))
                                                               :movable? true})
                                    (println "pointerdown svg" @current-selection)
                                    (println "pointerdown svg" (fc/browser-to-svg-coords e viewbox (.getElementById js/document "sv")))
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
      (canvas. "canvas-id")
      #_(view.))))