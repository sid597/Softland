(ns app.client.style-components.buttons
  (:require contrib.str
            #?(:cljs [clojure.string :as str])
            [hyperfiddle.electric-svg :as svg]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [hyperfiddle.electric-ui4 :as ui]
            [clojure.pprint :as pprint]
            [app.client.mode :refer [theme]]))



(e/defn icon-button [icon-name]
  (e/client
    (let [!hovered (atom false)
          hovered  (e/watch !hovered)]
      (dom/button
        (dom/style (merge {:background "none"
                           :border "none" ;"1px solid black"
                           :padding "3px"
                           :align-items "center"
                           :display "flex"
                           :border-radius "3px"}
                     (when hovered {:background "#efefef"})))
        (dom/on "mouseenter" (e/fn [e] (do (reset! !hovered true)
                                           (println "hovered" @!hovered)
                                           (reset! !hovered true))))
        (dom/on "mouseleave" (e/fn [e] (do (reset! !hovered false)
                                           (println "mouse leave hovered" @!hovered))))
        (new icon-name)))))
