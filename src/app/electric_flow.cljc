(ns app.electric-flow
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric-dom2 :as dom]
            [app.data :as data]
            [app.background :as bg]
            [hyperfiddle.electric-ui4 :as ui]))



(e/def edges (e/server (e/watch data/!edges)))

#?(:clj (def !nodes (atom {:sv-circle {:id "sv-circle"
                                       :draggable? true
                                       :x 700
                                       :y 100
                                       :r 80
                                       :type "circle"
                                       :color "red"}
                           :sv-circle1 {:id "sv-circle1"
                                        :draggable? true
                                        :x 900
                                        :y 300
                                        :r 60
                                        :type "circle"
                                        :color "green"}})))

(e/def nodes (e/server (e/watch !nodes)))
#?(:clj (def !viewbox (atom [0 0 3000 2000])))
(e/def viewbox (e/server (e/watch !viewbox)))

(e/defn view []
  (e/client
    (svg/svg
      (dom/props {:viewBox (clojure.string/join " " viewbox)
                  :style {:min-width "100%"
                          :min-height "100%"
                          :top 0
                          :left 0}})
      (bg/dot-background. "black" viewbox)
      #_(svg/circle
          (dom/props {:id id
                      :cx x
                      :cy y
                      :r  r
                      :fill color})
          (dom/on  "click" (e/fn [e]
                             (e/server (swap! !nodes assoc-in  [k :r] (+ r 4)))))))))

(e/defn main []
    (view.))


