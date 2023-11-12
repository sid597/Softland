(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-ui5 :as ui5]
            [hyperfiddle.electric-dom2 :as dom]
            [applied-science.js-interop :as j]
            [hyperfiddle.electric-ui4 :as ui]
            [missionary.core :as m]))





(defn round-to-2-decimals [n]
  (float (/ (Math/round (* n 100.0)) 100.0)))

#?(:cljs (def is-dragging? (atom false)))
#?(:cljs (def !last-position (atom {:x 0 :y 0})))
#?(:cljs (def !zoom-level (atom 1)))

(e/def zoom-level (e/watch !zoom-level))

#?(:cljs (def initial-viewbox [446 -115 761 761]))
#?(:cljs (def !view-box (atom initial-viewbox)))
#?(:cljs (def !border-drag? (atom false)))


(e/def view-box (e/watch !view-box))


(defn direct-calculation [viewBox zoom-factor focus-point]
  (let [new-width (* (nth viewBox 2) zoom-factor)
        new-height (* (nth viewBox 3) zoom-factor)
        dx (* (- (nth viewBox 2) new-width)
             (/ (- (first focus-point) (nth viewBox 0)) (nth viewBox 2)))
        dy (* (- (nth viewBox 3) new-height)
             (/ (- (second focus-point) (nth viewBox 1)) (nth viewBox 3)))]
    [(+ (nth viewBox 0) dx)
     (+ (nth viewBox 1) dy)
     new-width
     new-height]))


(defn browser-to-svg-coords [event svg-element]
  (let [bbox (.getBoundingClientRect svg-element)
        view-box-str (.getAttribute svg-element "viewBox")
        viewbox (mapv js/parseFloat (clojure.string/split view-box-str #" "))
        view-box-width (nth viewbox 2)
        view-box-height (nth viewbox 3)
        ratio-x (/ view-box-width (.-width bbox))
        ratio-y (/ view-box-height (.-height bbox))
        svg-x (+ (* (- (.-clientX event) (.-left bbox)) ratio-x) (nth viewbox 0))
        svg-y (+ (* (- (.-clientY event) (.-top bbox)) ratio-y) (nth viewbox 1))]
    [svg-x svg-y]))


(e/defn dot-background [color]
  (svg/defs
   (svg/pattern
     (dom/props {:id "dotted-pattern"
                 :width 20
                 :height 20
                 :patternUnits "userSpaceOnUse"})
     (svg/circle
       (dom/props {:cx 1
                   :cy 1
                   :r  1
                   :fill color}))))

  (svg/rect
    (dom/props
      {:id "background"
       :x (first view-box)
       :y (second view-box)
       :width (nth view-box 2)
       :height (nth view-box 3)
       :fill "url(#dotted-pattern)"})))

(defn element-new-coordinates1 [e id]
  (let [el (.getElementById js/document id)
        ctm (.getScreenCTM el)
        dx (/ (- (.-clientX e) (.-e ctm))
             (.-a ctm))
        dy (/ (- (.-clientY e) (.-f ctm))
             (.-d ctm))]
    ;(println "dx" dx "dy" dy)
    [dx dy]))

#?(:cljs (def border-origin (atom nil)))

(e/defn circle [{:keys [x y r id draggable? color]}]
  (let [!cx (atom x)
        !cy (atom y)
        cx (e/watch !cx)
        cy (e/watch !cy)
        el-is-dragging? (atom false)]
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
                                           (reset! !cx x)
                                           (reset! !cy y)))))
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

(e/defn line [{:keys [x1 y1 x2 y2 id color to from]}]
  (let [!to (.getElementById js/document to)
        !tx (atom (.getAttribute !to "cx"))
        !ty (atom (.getAttribute !to "cy"))
        !from  (.getElementById js/document from)
        !fx (atom (.getAttribute !from "cx"))
        !fy (atom (.getAttribute !from "cy"))]
    (js/console.log "to" to "from" from "tx" !tx "ty" !ty "fx" !fx "fy" !fy)
    (svg/line
      (dom/props {:id id
                  :x1 (e/watch !fx)
                  :y1 (e/watch !fy)
                  :x2 (e/watch !tx)
                  :y2 (e/watch !ty)
                  :stroke color
                  :stroke-width 4}))))


(defn find-new-coordinates [e]
  (let [svg (.getElementById js/document "sv")
        cw  (.-clientWidth svg)
        ch  (.-clientHeight svg)
        xf  (/ cw (nth @!view-box 2))
        yf  (/ ch (nth @!view-box 3))
        dx  (/ (- (.-clientX e) (:x @!last-position))
              xf)
        dy  (/ (- (.-clientY e) (:y @!last-position))
              yf)
        nx  (- (first @!view-box) dx)
        ny  (- (second @!view-box) dy)]
    [nx ny]))

#?(:cljs (def current-selection (atom nil)))
#?(:cljs (def !nodes (atom [{:id "sv-circle"
                             :draggable? true
                             :x 700
                             :y 100
                             :r 80
                             :type "circle"
                             :color "red"}
                            {:id "sv-circle1"
                             :draggable? true
                             :x 900
                             :y 300
                             :r 60
                             :type "circle"
                             :color "green"}
                            ])))

#?(:cljs (def !edges (atom [{:id "sv-line"
                             :x1 900
                             :y1 300
                             :x2 700
                             :y2 100
                             :type "line"
                             :to   "sv-circle"
                             :from "sv-circle1"
                             :color "black"}])))

(e/def nodes (e/watch !nodes))
(e/def edges (e/watch !edges))

(e/defn view []
  (dom/div
    (svg/svg
     (dom/props {:id    "sv"
                 :viewBox (clojure.string/join " " view-box)
                 :style {:min-width "100%"
                             :min-height "100%"
                             :top 0
                             :left 0}})
     (dom/on "pointermove" (e/fn [e]
                             (cond
                               (and @is-dragging?
                                    (= "background"
                                      (:selection
                                        @current-selection))
                                    (:movable?
                                      @current-selection))    (let [[nx ny] (find-new-coordinates e)]
                                                                (println "gg")
                                                                (swap! !view-box assoc 0 nx)
                                                                (swap! !view-box assoc 1 ny)
                                                                (reset! !last-position {:x (.-clientX e) :y (.-clientY e)}))
                               @!border-drag? (println "border draging"))))
     (dom/on "pointerdown" (e/fn [e]
                             (.preventDefault e)
                             (println "pointerdown svg")
                             (reset! current-selection {:selection (.-id (.-target e))
                                                        :movable? true})
                             (reset! !last-position {:x (.-clientX e) :y (.-clientY e)})
                             (reset! is-dragging? true)))
     (dom/on "pointerup" (e/fn [e]
                           (.preventDefault e)
                           (println "pointerup svg")
                           (reset! is-dragging? false)
                           (when @!border-drag?
                             (println "border draging up >>>")
                             (reset! !border-drag? false))))
     (dom/on "wheel" (e/fn [e]
                       (.preventDefault e)
                       (let [coords (browser-to-svg-coords e (.getElementById js/document "sv"))
                             wheel   (if (< (.-deltaY e) 0)
                                       1.01
                                       0.99)
                             new-view-box (direct-calculation view-box wheel coords)]
                         (reset! !zoom-level (* zoom-level wheel))
                         (reset! !view-box new-view-box))))
     (dot-background. "black")
     ;; Render nodes before the edges because edges depend on nodes
     (e/for [n nodes]
       (js/console.log "n" n identity)
       (circle. n))
     (e/for [ed edges]
        (js/console.log "ed" ed identity)
        (line. ed)))))
     #_(circle. 700 100 80 "sv-circle" true "red")
     #_(circle. 900 300 60 "sv-circle1" true "green")


(e/defn main []
  (e/client
   (view.)))


