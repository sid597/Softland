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

#?(:cljs (def !transform-m (atom '(1 0 0 1 0 0))))

(e/def transform-m (e/watch !transform-m))

(defn calc [factor matrix]
  (reduce
    (fn [l [idx v]]
      (let [cx 125
            cy 75
            new-val (cond
                      (= idx 4) (+ v
                                  (* (- 1 factor)
                                    cx))
                      (= idx 5) (+ v
                                  (* (- 1 factor)
                                    cy))
                      :else      (* v factor))]
        (conj l new-val)))
    []
    (map-indexed (fn [idx v] [idx v]) matrix)))

(calc 1.25 '(1 0 0 1 0 0))

(defn round-to-2-decimals [n]
  (float (/ (Math/round (* n 100.0)) 100.0)))

(defn set-on-zoom [factor]
  (println "factor" factor)
  (let [;delta (.-deltaY e)
        ;factor 1.25 ;(if (< delta 0) 1.1 0.9)
        sv (.getElementById  js/document "sv")
        vbox (-> sv
               (.getAttribute  "viewBox")
               (str/split #" ")
               (js->clj :keywordize-keys true))
        cx (/ (nth vbox 2) 2)
        cy (/ (nth vbox 3) 2)
        indxed-matrix (map-indexed
                        (fn [idx val]
                          [idx val])
                        @!transform-m)
        new-matrix (reverse (reduce
                              (fn [l [idx v]]
                                (let [new-mag (round-to-2-decimals (* v factor))
                                      in-range? (if (and (>= new-mag 0.25)
                                                      (<= new-mag 11.11))
                                                  true
                                                  false)
                                      new-val (if in-range?
                                                new-mag
                                                v)]
                                  (conj l new-val)))
                              ()
                              indxed-matrix))]

    (reset! !transform-m  new-matrix)))


(e/defn view [size]
  (dom/div
      (svg/svg
          (dom/props {:id      "sv"
                      :viewBox (str "0 " "0 " size " " size)
                      :style {:top "0"
                              ;:background-color "black"
                              :left "0"
                              ;:stroke "blue"
                              :stroke-width "5px"
                              :width (str size "px")
                              :height (str size "px")
                              :fill "red"}})
          (dom/on "wheel" (e/fn [e]
                            (js/console.log e)
                            (let [delta (.-deltaY e)
                                  factor (if (< delta 0) 1.01 0.99)]
                              (println "delta" delta)
                              (println "factor" factor)
                              (set-on-zoom factor))))

        (let [res (new (pinch-state< dom/node))] ; What's the significance of passing empty map as `rf` here?
          (println "pinch state res" res)
          (svg/g
              (dom/props {:id "matrix-group"
                          :transform (str "matrix" transform-m)})
              (println "transform-m" (str "matrix"  transform-m))
              (background.))))))

(e/defn main []
  (e/client
    (dom/div
      (dom/props {:style {:display "flex"
                          :flex-direction "column"}})
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "90px"
                            :position "absolute"
                            :background-color "red"
                            :width "90px"}})
        (dom/on "click" (set-on-zoom 1.01))
        (dom/text "click me"))
      (dom/div
        (dom/props {:class "hover"
                    :style {:height "30px"
                            :position "absolute"
                            :background-color "black"
                            :width "90px"}})
        (dom/on "click" (set-on-zoom 0.99))
        (dom/text "de click me")))
    (view. 1000)))

