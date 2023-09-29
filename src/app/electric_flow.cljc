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

(defn client->coordinate [svg-id]
  (let [sv   (.getElementById js/document svg-id)
        ch   (.-clientHeight (.-documentElement js/document))
        cw   (.-clientWidth (.-documentElement js/document))
        vbox (-> sv
               (.getAttribute  "viewBox")
               (str/split #" ")
               (js->clj :keywordize-keys true))
        vw (nth vbox 2)
        vh (nth vbox 3)
        scale-x (round-to-2-decimals (/ cw vw))
        scale-y (round-to-2-decimals (/ ch vh))]
    (println "===> " "ch" ch "cw" cw "vbox" vbox "vh" vh "vw" vw "scale-x" scale-x "scale-y" scale-y)
    [scale-x scale-y]))


#?(:cljs (def !transform-m (atom '(1 0 0 1 0 0))))
#?(:cljs (def mouse-x (atom nil)))
#?(:cljs (def mouse-y (atom nil)))


(e/def mx (round-to-2-decimals (e/watch mouse-x)))
(e/def my (round-to-2-decimals (e/watch mouse-y)))
(e/def transform-m (e/watch !transform-m))


(defn scale-by [factor]
  (let [;delta (.-deltaY e)
        ;factor (if (< delta 0) 1.1 0.9)
        current-z (first @!transform-m)
        scale (round-to-2-decimals (* current-z  factor))
        new-matrix (reverse (into () (assoc (vec @!transform-m) 0 scale 3 scale)))]
    (println "new-matrix" new-matrix)
   (reset! !transform-m new-matrix)))


(defn translate-to [x y]
  (let [current-scale (first @!transform-m)
        ;; This works on zoom in, but not on zoom out
        dx (round-to-2-decimals (* x (- 1 current-scale)))
        dy (round-to-2-decimals (* y (- 1 current-scale)))
        new-matrix (reverse (conj (take 4 @!transform-m) dx dy))]
    (reset! !transform-m new-matrix)))


(e/defn view [size]
  (dom/div
      (svg/svg
          (dom/props {:id      "sv"
                      :viewBox (str "0 " "0 " size " " size)})
          (dom/on "mousemove" (e/fn [e]
                                (.preventDefault e)
                                (let [svg (.getElementById js/document "sv")
                                      x   (- (.-clientX e) (.-left (.getBoundingClientRect svg)))
                                      y   (- (.-clientY e) (.-top (.getBoundingClientRect svg)))
                                      [xf xy] (client->coordinate "sv")
                                      nx (/ x xf)
                                      ny (/ y xy)]
                                 (reset! mouse-x nx)
                                 (reset! mouse-y ny))))

          (dom/on "wheel" (e/fn [e]
                            (.preventDefault e)
                            (let [delta        (.-deltaY e)
                                  factor        (if (< delta 0) 1.03 0.97)]
                              (scale-by factor)
                              (translate-to mx my))))

          (svg/g
              (dom/props {:id "matrix-group"
                          :transform (str "matrix" transform-m)})
              (println "transform-m" (str "matrix"  transform-m))
              (background.)
              (svg/circle
                (dom/props {:cx 500
                            :cy 500
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
        (dom/on "click" (scale-by 2.01))
        (dom/text "zoom in"))
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "grey"
                            :margin-top "60px"
                            :padding "10px"
                            :width "auto"}})
        (dom/on "click" (scale-by 0.59))
        (dom/text "zoom out"))
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "green"
                            :margin-top "120px"
                            :padding "10px"
                            :width "auto"}})
        (dom/on "click" (translate-to  500 500))
        (dom/text "translate"))
      (dom/div
        (dom/props {
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "lightblue"
                            :margin-top "220px"
                            :padding "10px"
                            :width "auto"}})
        (dom/text "-- x -- " mx)
        (dom/text " -- y -- " my)))
    (view. 5000)))

