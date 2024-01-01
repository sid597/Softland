(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [clojure.edn :as edn]
            [clojure.pprint :as pprint]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [applied-science.js-interop :as j]
            [app.data :as data]
            [app.background :as bg]
            [app.flow-calc :as fc]
            [app.electric-codemirror :as cm]
            [hyperfiddle.electric-ui4 :as ui]))

#?(:clj (def !edges (atom {:sv-line {:id :sv-line
                                     :type "line"
                                     :to   :sv-circle
                                     :from :sv-circle1
                                     :color "black"}})))

(e/def edges (e/server (e/watch !edges)))

#?(:clj (def !nodes (atom {:sv-circle {:id :sv-circle :dragging? false
                                       :x 700
                                       :y 100
                                       :r 100
                                       :type "circle"
                                       :color "red"}
                           :sv-circle1 {:id :sv-circle1
                                        :dragging? false
                                        :x 900
                                        :y 300
                                        :r 100
                                        :type "circle"
                                        :color "green"}
                           :rect       {:id :rect
                                        :x 500
                                        :y 600
                                        :width 400
                                        :height 800
                                        :type "rect"
                                        :fill "lightblue"}})))


(e/def nodes (e/server (e/watch !nodes)))


(def !new-line (atom {:start nil
                      :end   nil}))
(def !border-drag? (atom false))
(def !is-dragging? (atom false))
(def !zoom-level (atom 1))
(def !last-position (atom {:x 0 :y 0}))
(def !viewbox (atom [0 0 2000 2000]))



(e/def viewbox (e/watch !viewbox))
(e/def last-position (e/watch !last-position))
(e/def zoom-level (e/watch !zoom-level))
(e/def is-dragging? (e/watch !is-dragging?))
(e/def new-line (e/watch !new-line))

(e/defn circle [[k {:keys [id x y r color dragging?]}]]
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
                                  (e/server (swap! !nodes assoc-in [k :x]  x))
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
                              (e/server (swap! !nodes assoc-in [k :dragging?] false))))))

(e/defn line [[k {:keys [id color to from]}]]
  (let [tx (->  (e/server nodes)
                to
                :x)
        ty (-> (e/server nodes)
               to
               :y)
        fx (-> (e/server nodes)
               from
               :x)
        fy (-> (e/server nodes)
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



(e/defn rect [[_ {:keys [x y width height fill]}]]
  (let [read (fn [edn-str]
               (println "Read string:" edn-str)
               (try (edn/read-string edn-str)
                    (catch #?(:clj Throwable :cljs :default) t
                      #?(:clj (clojure.tools.logging/error t)
                         :cljs (js/console.warn t)) nil)))
        write (fn [edn] (with-out-str (pprint/pprint edn)))]
   (svg/g
    (svg/rect
      (dom/props {:id "rect"
                  :x x
                  :y y
                  :width width
                  :height height
                  :fill "black"})
      (dom/on "click" (e/fn [e]
                        (println "clicked the rect.")))

      (dom/on "mousedown" (e/fn [e]
                            (println "mousedown the rect.")
                            (let [[x y] (fc/element-new-coordinates1 e "rect")]
                              (e/server
                                (swap! !edges assoc :raw {:id :raw
                                                          :type "raw"
                                                          :x1 x
                                                          :y1 y
                                                          :x2 nil
                                                          :y2 nil
                                                          :stroke "blue"
                                                          :stroke-width 4})))
                            (reset! !border-drag? true)))
      (dom/on "mouseup" (e/fn [e]
                          (println "mouseup the rect.")
                          (reset! !border-drag? false))))


    (svg/foreignObject
      (dom/props {:x (+  x 5)
                  :y (+  y 5)
                  :height (- height 10)
                  :width  (- width 10)
                  :fill "black"
                  :style {:display "flex"
                          :flex-direction "column"}})

      (dom/div
          (dom/props {:style {:background-color fill
                              :height "100%"}})
          (dom/div
            (new cm/CodeMirror {:parent dom/node} read write "GM"))
          #_(ui/button
              read
              (dom/text "save")))))))

(e/defn new-line-el [[k {:keys [id x1 y1 x2 y2 stroke stroke-width]}]]
  (println "dom props")
  (svg/line
    (dom/props {:id "draw"
                :x1  x1
                :y1  y1
                :x2  x2
                :y2  y2
                :stroke "blue"
                :stroke-width 4})))


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
                              (when @!border-drag?
                                (let [[x y] (fc/element-new-coordinates1 e "sv")]
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
                                    @current-selection))    (let [[nx ny] (fc/find-new-coordinates e last-position viewbox)
                                                                  ex      (.-clientX e)
                                                                  ey      (.-clientY e)]
                                                              (println "gg")
                                                              (swap! !viewbox assoc 0 nx)
                                                              (swap! !viewbox assoc 1 ny)
                                                              (reset! !last-position {:x ex
                                                                                      :y ey}))
                                #_#_@!border-drag? (println "border draging"))))
        (dom/on "mousedown" (e/fn [e]
                              (.preventDefault e)
                              (let [ex (e/client (.-clientX e))
                                    ey (e/client (.-clientY e))]
                                (println "pointerdown svg")
                                (reset! current-selection {:selection (.-id (.-target e))
                                                           :movable? true})
                                (reset! !last-position {:x ex
                                                        :y ey})
                                (reset! !is-dragging? true))))
        (dom/on "wheel" (e/fn [e]
                          (.preventDefault e)
                          (let [coords (fc/browser-to-svg-coords e (.getElementById js/document "sv") viewbox)
                                wheel   (if (< (.-deltaY e) 0)
                                          1.01
                                          0.99)
                                new-view-box (fc/direct-calculation viewbox wheel coords)
                                new-zoom-level (* zoom-level wheel)]
                            (reset! !zoom-level new-zoom-level)
                            (reset! !viewbox new-view-box))))
        (dom/on "mouseup" (e/fn [e]
                            (.preventDefault e)
                            (println "pointerup svg")
                            (reset! !is-dragging? false)
                            (reset! !border-drag? false)
                            #_(when @!border-drag?
                                (println "border draging up >>>")
                                (reset! !border-drag? false))))

        (bg/dot-background. "black" viewbox)
        (e/server
          (e/for-by identity [node nodes]
            (let [[_ {:keys [type]}] node]
              (e/client
                (println "type" type (= "circle" type))

                (cond
                      (= "circle" type)  (circle. node)
                      (= "rect" type)    (rect. node)))))
          (e/for-by identity [edge edges]
            (let [[_ {:keys [type x2 y2]}] edge]
              (e/client
                (println "edge type" type edge)
                (cond
                  (and
                    (= "raw" type)
                    (not-every? nil? [x2 y2])) (new-line-el. edge)
                  (= "line" type)              (line. edge))))))))))

(e/defn main []
  (println "server viewbox val" viewbox)
  (println "gg")
  (view.))

