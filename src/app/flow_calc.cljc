(ns app.flow-calc
  (:require contrib.str
           #?(:cljs [clojure.string :as str])
           [hyperfiddle.electric-svg :as svg]
           [hyperfiddle.electric :as e]
           [hyperfiddle.electric-dom2 :as dom]
           [hyperfiddle.electric-ui4 :as ui]))

#?(:cljs (def initial-viewbox [446 -115 761 761]))
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

(defn element-new-coordinates1 [e id]
  (let [el (.getElementById js/document id)
        ctm (.getScreenCTM el)
        dx (/ (- (.-clientX e) (.-e ctm))
             (.-a ctm))
        dy (/ (- (.-clientY e) (.-f ctm))
             (.-d ctm))]
    ;(println "dx" dx "dy" dy)
    [dx dy]))


(defn find-new-coordinates [e last-position viewbox]
  (let [svg (.getElementById js/document "sv")
        cw  (.-clientWidth svg)
        ch  (.-clientHeight svg)
        xf  (/ cw (nth viewbox 2))
        yf  (/ ch (nth viewbox 3))
        dx  (/ (- (.-clientX e) (:x last-position))
              xf)
        dy  (/ (- (.-clientY e) (:y last-position))
              yf)
        nx  (- (first viewbox) dx)
        ny  (- (second viewbox) dy)]
    [nx ny]))
