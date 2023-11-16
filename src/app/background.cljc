(ns app.background
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.flow-calc :as fc :refer [ view-box]]
            [hyperfiddle.electric-ui4 :as ui]))

(e/defn dot-background [color view-box]

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
