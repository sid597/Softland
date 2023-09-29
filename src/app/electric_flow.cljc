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

(e/def mx  (e/watch mouse-x))
(e/def my (e/watch mouse-y))
(e/def cx (round-to-2-decimals (e/watch !cx)))
(e/def cy (round-to-2-decimals (e/watch !cy)))

(e/def transform-m (e/watch !transform-m))

(defn client->coordinate [svg-id]
  (let [sv   (.getElementById js/document svg-id)
        ch   (.-clientHeight sv)
        cw   (.-clientWidth sv)
        vbox (-> sv
               (.getAttribute  "viewBox")
               (str/split #" ")
               (js->clj :keywordize-keys true))
        vw  (nth vbox 2)
        vh  (nth vbox 3)
        scale-x  (round-to-2-decimals (/ cw vw))
        scale-y (round-to-2-decimals (/ ch vh))]
    [scale-x scale-y]))

(defn scale-by [factor]
  (let [;delta (.-deltaY e)
        ;factor (if (< delta 0) 1.1 0.9)
        x (-> (first @!transform-m)
            (* factor)
            (round-to-2-decimals))
        y (-> (nth @!transform-m 3)
            (* factor)
            (round-to-2-decimals))
        new-matrix (reverse (into () (assoc (vec @!transform-m) 0 x 3 y)))]
    (println "x" x "y" y)
   (reset! !transform-m new-matrix)))


(defn translate-to [x y]
  (let [[xf yf] (client->coordinate "sv")
        nx (/ x xf)
        ny (/ y yf)
        x-scale (first @!transform-m)
        y-scale (nth @!transform-m 3)
        ;; This works on zoom in, but not on zoom out
        ox (nth @!transform-m 4)
        oy (nth @!transform-m 5)
        dx (round-to-2-decimals (* nx (- 1 x-scale)))
        dy (round-to-2-decimals (* ny (- 1 y-scale)))
        new-matrix (reverse (conj (take 4 @!transform-m) dx dy))]
    (println "translate" ox oy "dx+" (+ ox dx) (+ oy dy))
    (reset! !transform-m new-matrix)))

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


(e/defn view [h w]
  (dom/div
      (svg/svg
          (dom/props {:id      "sv"
                      :viewBox (str "0 " "0 " h " " w)})
          (dom/on "mousemove" (e/fn [e]
                                (.preventDefault e)
                                (let [svg (.getElementById js/document "sv")
                                      x   (- (.-clientX e) (.-left (.getBoundingClientRect svg)))
                                      y   (- (.-clientY e) (.-top (.getBoundingClientRect svg)))
                                      [xf xy] (client->coordinate "sv")
                                      nx (/ x xf)
                                      ny (/ y xy)]
                                 (reset! mouse-x (str (round-to-2-decimals (.-clientX e)) "-->" (round-to-2-decimals x) "-->" (round-to-2-decimals nx)))
                                 (reset! mouse-y (str (round-to-2-decimals (.-clientY e)) "-->" (round-to-2-decimals y) "-->" (round-to-2-decimals ny))))))
          (dom/on "wheel" (e/fn [e]
                            (.preventDefault e)
                            (reset! wheel-count (inc @wheel-count))
                            (when (= 0 (mod @wheel-count 1))
                              (println "-------- count" @wheel-count)
                              (let [delta        (.-deltaY e)
                                    wheel        (if (< delta 0) 1  -1)
                                    factor (js/Math.exp (* wheel 0.02))
                                    svg (.getElementById js/document "sv")
                                    x   (- (.-clientX e) (.-left (.getBoundingClientRect svg)))
                                    y   (- (.-clientY e) (.-top (.getBoundingClientRect svg)))]
                                (translate-to x y)
                                (scale-by factor)
                                (translate-to (- x) (- y))
                                (circle-coordinates)))))
          (svg/g
              (dom/props {:id "matrix-group"
                          :transform (str "matrix" transform-m)})
              (background.)
              (svg/circle
                (dom/props {:cx 500
                            :cy 500
                            :r  8
                            :fill "red"}))))))
;; We find the point at which the cursor is currently.
;; get its location in the base coordinate system
;; translate the cursor location from base system to current system
;; to do so we need to find how much the base system has been scaled by default

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
          (do
           (scale-by 2)
           (circle-coordinates)))
        (dom/text "zoom in"))
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "grey"
                            :margin-top "60px"
                            :padding "10px"
                            :width "auto"}})
        (dom/on "click"
          (do
           (scale-by 0.5)
           (circle-coordinates)))
        (dom/text "zoom out"))
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "60px"
                            :position "absolute"
                            :background-color "green"
                            :margin-top "120px"
                            :padding "10px"
                            :width "auto"}})
        (dom/on "click"
          (do (translate-to  500 500)
              (circle-coordinates)))

        (dom/text "translate"))
      (dom/div
        (dom/props {:style {:height "120px"
                             :position "absolute"
                             :background-color "lightblue"
                             :margin-top "220px"
                             :padding "10px"
                             :flex-direction "column"
                             :display "flex"
                             :width "auto"}})
        (dom/div (dom/text "cx -- " cx "\n"))
        (dom/div (dom/text "cy -- " cy))
        (dom/div (dom/text "mx -- " (str mx "\n")))
        (dom/div (dom/text "my -- " my))
        (dom/div (dom/text "tm --" transform-m))))
    (let [dh (.-clientHeight (.-documentElement js/document))
          dw (.-clientWidth (.-documentElement js/document))]
       (view. dh dw))))

