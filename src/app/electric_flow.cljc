(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.data :as data]
            [app.background :as bg]
            [app.flow-calc :as fc]
            [hyperfiddle.electric-ui4 :as ui]))

#?(:clj (def !edges (atom [{:id :sv-line
                            :type "line"
                            :to   :sv-circle
                            :from :sv-circle1
                            :color "black"}])))

(e/def edges (e/server (e/watch !edges)))

#?(:clj (def !nodes (atom {:sv-circle {:id :sv-circle
                                       :dragging? false
                                       :x 700
                                       :y 100
                                       :r 80
                                       :type "circle"
                                       :color "red"}
                           :sv-circle1 {:id :sv-circle1
                                        :dragging? false
                                        :x 900
                                        :y 300
                                        :r 60
                                        :type "circle"
                                        :color "green"}})))

(e/def nodes (e/server (e/watch !nodes)))

#?(:clj (def !viewbox (atom [0 0 1000 1000])))
(e/def viewbox (e/server (e/watch !viewbox)))


#?(:clj (def !last-position (atom {:x 0 :y 0})))
(e/def last-position (e/server (e/watch !last-position)))

#?(:clj (def !zoom-level (atom 1)))
(e/def zoom-level (e/server (e/watch !zoom-level)))

#?(:clj (def !is-dragging? (atom false)))
(e/def is-dragging? (e/server (e/watch !is-dragging?)))


(e/defn circle [node]
  (let [[k {:keys [id x y r color dragging?]}] node]
    (svg/circle
      (dom/props {:id id
                  :cx x
                  :cy y
                  :r  r
                  :fill color})
      (dom/on  "mousemove" (e/fn [e]
                               (.preventDefault e)
                               (when dragging?
                                (println "dragging element")
                                (let [[x y]   (fc/element-new-coordinates1 e id)]
                                  (e/server (swap! !nodes assoc-in [k :x] x))
                                  (e/server (swap! !nodes assoc-in [k :y] y))))))
      (dom/on "mousedown"  (e/fn [e]
                             (.preventDefault e)
                             (.stopPropagation e)
                             (println "pointerdown element")
                             (e/server (swap! !nodes assoc-in [k :dragging?] true))))
      (dom/on "mouseup"    (e/fn [e]
                               (.preventDefault e)
                               (.stopPropagation e)
                               (println "pointerup element")
                               (e/server (swap! !nodes assoc-in [k :dragging?] false))))
      (dom/on "mouseleave"    (e/fn [e]
                                (.preventDefault e)
                                (.stopPropagation e)
                                (println "mouseleave element")
                                (e/server (swap! !nodes assoc-in [k :dragging?] false))))
      (dom/on "mouseout"    (e/fn [e]
                              (.preventDefault e)
                              (.stopPropagation e)
                              (println "mouseout element")
                              (e/server (swap! !nodes assoc-in [k :dragging?] false)))))))

(e/defn line [{:keys [id color to from]}]
  (let [r1 (-> nodes
               to
               :r)
        r2 (-> nodes
               from
               :r)
        tx (-> nodes
               to
               :x)
        ty (-> nodes
               to
               :y)
        fx (-> nodes
               from
               :x)
        fy (-> nodes
               from
               :y)]
    (svg/line
      (dom/props {:id id
                  :x1  tx
                  :y1  ty
                  :x2  fx
                  :y2  fy
                  :stroke color
                  :stroke-width 4}))))



(e/defn view []
  (let [current-selection (atom nil)]
    (e/client
      (svg/svg
        (dom/props {:viewBox (clojure.string/join " " viewbox)
                    :id "sv"
                    :style {:min-width "100%"
                            :min-height "100%"
                            :top 0
                            :left 0}})
        (dom/on "mousemove" (e/fn [e]
                              (cond
                                (and is-dragging?
                                  (= "background"
                                    (:selection
                                      @current-selection))
                                  (:movable?
                                    @current-selection))    (let [[nx ny] (fc/find-new-coordinates e last-position viewbox)
                                                                  ex      (.-clientX e)
                                                                  ey      (.-clientY e)]
                                                              (println "gg")
                                                              (e/server (swap! !viewbox assoc 0 nx))
                                                              (e/server (swap! !viewbox assoc 1 ny))
                                                              (e/server (reset! !last-position {:x ex
                                                                                                :y ey})))
                                #_#_@!border-drag? (println "border draging"))))
        (dom/on "mousedown" (e/fn [e]
                              (.preventDefault e)
                              (let [ex (e/client (.-clientX e))
                                    ey (e/client (.-clientY e))]
                                (println "pointerdown svg")
                                (reset! current-selection {:selection (.-id (.-target e))
                                                           :movable? true})
                                (e/server (reset! !last-position {:x ex
                                                                  :y ey}))
                                (e/server (reset! !is-dragging? true)))))
        (dom/on "wheel" (e/fn [e]
                          (.preventDefault e)
                          (let [coords (fc/browser-to-svg-coords e (.getElementById js/document "sv") viewbox)
                                wheel   (if (< (.-deltaY e) 0)
                                          1.01
                                          0.99)
                                new-view-box (fc/direct-calculation viewbox wheel coords)
                                new-zoom-level (* zoom-level wheel)]
                            (e/server (reset! !zoom-level new-zoom-level))
                            (e/server (reset! !viewbox new-view-box)))))
        (dom/on "mouseup" (e/fn [e]
                            (.preventDefault e)
                            (println "pointerup svg")
                            (e/server (reset! !is-dragging? false))
                            #_(when @!border-drag?
                                (println "border draging up >>>")
                                (reset! !border-drag? false))))
        (bg/dot-background. "black" viewbox)
        (e/for-by identity [node nodes]
          (circle. node))
        (e/for-by identity [edge edges]
          (line. edge))))))

(e/defn main []
    (view.))

