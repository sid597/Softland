(ns app.shapes
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [app.background :as bg :refer [dot-background]]
            [app.data :as data :refer [!nodes]]
            [app.flow-calc :as fc :refer [!last-position find-new-coordinates direct-calculation browser-to-svg-coords element-new-coordinates1 ]]))


#?(:cljs (def !border-drag? (atom false)))

(e/defn circle [{:keys [id cx cy r color draggable?]}]
  (let [el-is-dragging? (atom false)]
    (svg/circle
      (dom/props {:id id
                  :cx cx
                  :cy cy
                  :r  r
                  :fill color})
      (when draggable?
        (let [node (.getElementById js/document id)]
          (dom/on!  node "pointermove" (fn [e]
                                         (.preventDefault e)
                                         (when @el-is-dragging?
                                           (println "dragging element")
                                           (let [[x y]   (element-new-coordinates1 e id)]
                                             (e/server (swap! !nodes assoc :cx x))
                                             (e/server (swap! !nodes assoc :cy y))))))
          (dom/on! node "pointerdown" (fn [e]
                                        (println "pointerdown element")
                                        (reset! el-is-dragging? true)))
          (dom/on! node "pointerup" (fn [e]
                                      (println "pointerup element")
                                      (reset! el-is-dragging? false))))))
    (svg/circle
      (dom/props {:id (str id "-border")
                  :cx cx
                  :cy cy
                  :r   r
                  :stroke-width 18
                  :stroke "lightblue"
                  :fill "none"
                  :pointer-events "stroke"})
      (when draggable?
        (let [node (.getElementById js/document (str id "-border"))]
          (dom/on!  node "pointermove" (fn [e]
                                         (.preventDefault e)
                                         (when @!border-drag?
                                           (println "dragging border"))))
          (dom/on! node "pointerdown" (fn [e]
                                        (let [point (js/DOMPoint. (.-clientX e) (.-clientY e))
                                              ctm (.matrixTransform point (.inverse (.getScreenCTM node)))]
                                          (js/console.log "pointerdown border" ctm)
                                          (reset! !border-drag? true))))
          (dom/on! node "pointerup" (fn [e]
                                      (println "pointerup border")
                                      (reset! !border-drag? false))))))))


(e/defn line [{:keys [id color to from]}]
  (let [!to (.getElementById js/document to)
        !tx (atom (.getAttribute !to "cx"))
        !ty (atom (.getAttribute !to "cy"))
        !from  (.getElementById js/document from)
        !fx (atom (.getAttribute !from "cx"))
        !fy (atom (.getAttribute !from "cy"))]
    (svg/line
      (dom/props {:id id
                  :x1 (e/watch !fx)
                  :y1 (e/watch !fy)
                  :x2 (e/watch !tx)
                  :y2 (e/watch !ty)
                  :stroke color
                  :stroke-width 4}))))
