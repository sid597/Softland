(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-ui5 :as ui5]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [missionary.core :as m]))





(defn round-to-2-decimals [n]
  (float (/ (Math/round (* n 100.0)) 100.0)))

#?(:cljs (def is-dragging? (atom false)))
#?(:cljs (def !last-position (atom {:x 0 :y 0})))
#?(:cljs (def !zoom-level (atom 1)))

(e/def zoom-level (e/watch !zoom-level))

#?(:cljs (def initial-viewbox [0 0 3400 3400]))
#?(:cljs (def !view-box (atom initial-viewbox)))



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
      {:x (first view-box)
       :y (second view-box)
       :width (nth view-box 2)
       :height (nth view-box 3)
       :fill "url(#dotted-pattern)"})))




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
                             (when @is-dragging?
                               (let [svg (.getElementById js/document "sv")
                                     dx (/ (- (.-clientX e) (:x @!last-position))
                                           (.-clientWidth svg))
                                     dy (/ (- (.-clientY e) (:y @!last-position))
                                          (.-clientHeight svg))
                                     ny  (- (second @!view-box) dy)
                                     nx  (- (first @!view-box) dx)]
                                 (swap! !view-box assoc 0 nx)
                                 (swap! !view-box assoc 1 ny)
                                 (reset! !last-position {:x (.-clientX e) :y (.-clientY e)})))))
     (dom/on "pointerdown" (e/fn [e]
                                (reset! !last-position {:x (.-clientX e) :y (.-clientY e)})
                                (reset! is-dragging? true)))
     (dom/on "pointerup" (e/fn [e]
                           (reset! is-dragging? false)))
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
     (svg/circle
        (dom/props {:id "sc"
                    :cx 700
                    :cy 200
                    :r  8
                    :fill "red"}))
     (svg/circle
        (dom/props {:id "sc"
                    :cx 900
                    :cy 200
                    :r  8
                    :fill "green"})))))



(e/defn main []
  (e/client
   (view.)))


