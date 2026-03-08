(ns app.client.style-components.bottom-bar
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [clojure.pprint :as pprint]
            [app.client.style-components.svg-icons :refer [get-icon]]
            [app.client.mode :refer [theme]]))



(e/defn settings-rect []
  (e/client
    (let [!x      (atom -1550)
          x (e/watch !x)
          !y      (atom 250)
          y (e/watch !y)
          !height (atom 100)
          height (e/watch !height)
          !width  (atom 200)
          width (e/watch !width)
          !fill   (atom "lightblue")
          fill (e/watch !fill)]
      (svg/rect
        (dom/props {:x x
                    :y y
                    :height height
                    :width width
                    :fill fill})))))


