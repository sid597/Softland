(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-ui5 :as ui5]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [missionary.core :as m]))



(e/defn background []
  (svg/defs
    (svg/pattern
      (dom/props {:id "dotted-pattern"
                  :width "20"
                  :height "20"
                  :x 0
                  :y 0
                  :patternUnits "userSpaceOnUse"})
      (svg/circle
        (dom/props {:cx "10"
                    :cy "10"
                    :r "1"
                    :fill "black"
                    ;:stroke "grey"
                    #_#_:stroke-width "1"}))))
  (svg/rect
    (dom/props
      {:width "100%"
       :height "100%"
       :fill "url(#dotted-pattern)"})))

(defn round-to-2-decimals [n]
  (float (/ (Math/round (* n 100.0)) 100.0)))



#?(:cljs (def wheel-count (atom 0)))
#?(:cljs (def !h (atom 0)))
#?(:cljs (def !w (atom 0)))
#?(:cljs (def base-matrix '(1 0 0 1 0 0)))
#?(:cljs (def !transform-m (atom base-matrix)))
#?(:cljs (def mouse-x (atom nil)))
#?(:cljs (def mouse-y (atom nil)))

#?(:cljs (def !cx (atom 500)))
#?(:cljs (def !cy (atom 500)))


#?(:cljs (def start-x (atom 0)))
#?(:cljs (def start-y (atom 0)))
#?(:cljs (def is-dragging? (atom false)))
#?(:cljs (def !translate-x (atom 0)))
#?(:cljs (def !translate-y (atom 0)))
#?(:cljs (def !svg-pan (atom {:x 0 :y 0 :sx 1 :sy 1})))
#?(:cljs (def !last-position (atom {:x 0 :y 0})))

(e/def translate-x (e/watch !translate-x))
(e/def translate-y (e/watch !translate-y))
(e/def is-dragging (e/watch is-dragging?))
(e/def svg-pan (e/watch !svg-pan))
(e/def mx  (e/watch mouse-x))
(e/def my (e/watch mouse-y))
(e/def cx (round-to-2-decimals (e/watch !cx)))
(e/def cy (round-to-2-decimals (e/watch !cy)))

(e/def transform-m (e/watch !transform-m))


(defn scale-by [factor]
  (let [x (-> (:sx @!svg-pan)
             (* factor)
             (round-to-2-decimals))
        y (-> (:sy @!svg-pan)
            (* factor)
            (round-to-2-decimals))]
    (swap! !svg-pan assoc :sx x :sy y)))


(defn translate-to [x y]
  (let [x-scale (:sx @!svg-pan)
        y-scale (:sy @!svg-pan)
        cx (:x @!svg-pan)
        cy (:y @!svg-pan)
        dx  (round-to-2-decimals (* x (- 1 x-scale)))
        dy  (round-to-2-decimals (* y (- 1 y-scale)))]
    (println "dx" dx "dy" dy "pan " @!svg-pan)
    #_(swap! !svg-pan assoc :x dx :y dy)
    (swap! !svg-pan update :x + dx)
    (swap! !svg-pan update :y + dy)))

(defn circle-coordinates
  []
  (let [tx (nth @!transform-m 4)
        ty (nth @!transform-m 5)
        xs  (first @!transform-m)
        xy  (nth @!transform-m 3)
        x' (round-to-2-decimals (+ (* xs 500) tx))
        y' (round-to-2-decimals (+ (* xy 500) ty))]
    (println "--> current matrix" @!transform-m "---x---" x' "---y---" y')
    (reset! !cx x')
    (reset! !cy y')))

(defn get-mouse-position [e]
  (let [ex (.-clientX e)
        ey (.-clientY e)
        ctm (.getScreenCTM (.getElementById js/document "sv"))
        x  (/ (- ex (.-e ctm))
              (.-a ctm))
        y  (/ (- ey (.-f ctm))
              (.-d ctm))]
    [x y]))

(defn apply-transform [wheel]
  (let [factor  (round-to-2-decimals (js/Math.exp (* wheel 0.01)))
        svg     (.getElementById js/document "sv")
        x       (- (.-clientX e) (.-left (.getBoundingClientRect svg)))
        y       (- (.-clientY e) (.-top (.getBoundingClientRect svg)))]
    (println "factor" factor)
    (scale-by factor)
    (if (< wheel 0)
      (translate-to x y)
      (translate-to (- x)  (- y)))))

#?(:cljs (def !view-box (atom [0 0 1400 1400])))

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


(e/defn view []
  (dom/div
    (svg/svg
     (dom/props {:id    "sv"
                 ;:width 1400
                 ;:height 400
                 :viewBox (clojure.string/join " " view-box)})
     (dom/on "mousemove" (e/fn [e]
                           (when @is-dragging?
                             (let [dx (- (.-clientX e) (:x @!last-position))
                                   dy (- (.-clientY e) (:y @!last-position))]
                               (swap! !svg-pan update :x + dx)
                               (swap! !svg-pan update :y + dy)
                               (reset! !last-position {:x (.-clientX e) :y (.-clientY e)})))))
     (dom/on "mousedown" (e/fn [e]
                              (reset! !last-position {:x (.-clientX e) :y (.-clientY e)})
                              (reset! is-dragging? true)))
     (dom/on "mouseup" (e/fn [e]
                         (println "svg pan end ===================>" @!svg-pan)
                         (reset! is-dragging? false)))
     (dom/on "wheel" (e/fn [e]
                       (.preventDefault e)
                       (let [coords (browser-to-svg-coords e (.getElementById js/document "sv"))
                             wheel   (if (> (.-deltaY e) 0) 1  -1)
                             zoom-factor  (round-to-2-decimals (js/Math.exp (* wheel 0.01)))
                             ;zoom-factor (if (> (.-deltaY e) 0) 1.1 0.9)  ; assuming zoom-in for positive deltaY
                             new-view-box (direct-calculation @!view-box zoom-factor coords)]
                         (reset! !view-box new-view-box))))
     (svg/g
       (background.)
       (svg/circle
         (dom/props {:id "sc"
                     :cx 700
                     :cy 200
                     :r  8
                     :fill "red"}))))))





(e/defn main []
  (e/client
    (dom/div
      (dom/props {:style {:display "flex"
                          :flex-direction "column"}})
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "red"
                            :padding "10px"
                            :width "auto"}})
        (dom/on "click"
          (scale-by (round-to-2-decimals (js/Math.exp (* 1 0.01)))))

        (dom/text "translate x"))
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "grey"
                            :margin-top "60px"
                            :padding "10px"
                            :width "auto"}})
        (dom/on "click"
            (scale-by 0.5))
        (dom/text "translate -x")))

   (view.)))


