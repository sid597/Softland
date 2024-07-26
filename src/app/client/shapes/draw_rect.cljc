(ns app.client.shapes.draw-rect
  (:require contrib.str
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [clojure.pprint :as pprint]
            [app.client.style-components.svg-icons :refer [get-icon]]
            [app.client.mode :refer [theme]]
            #?@(:cljs [[clojure.string :as str]
                       [global-flow :refer [!global-atom current-time-ms]]])))


(e/defn draw-rect []
  (e/client
    (dom/button
      (dom/props {:style {:display "flex"
                          :background-color "red"}})
      (dom/text "GM")
      (dom/on "click" (e/fn [e]
                        (e/client
                            (do
                              (println "BEF" @!global-atom)
                             (reset! !global-atom {:type :draw-rect
                                                   :action :start-drawing
                                                   :time (current-time-ms)})
                             (println "clicked seddings"@!global-atom))))))))
