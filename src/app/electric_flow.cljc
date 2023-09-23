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


(defn set-on-zoom [e]

  (let [delta (.-deltaY e)
        factor (if (< delta 0) 1.1 0.9)
        sv (.getElementById  js/document "sv")
        vbox (-> sv
               (.getAttribute  "viewBox")
               (str/split #" ")
               (js->clj :keywordize-keys true))
        [x y width height] (into-array
                             (map #(js/parseInt %) vbox))

        new-width  (min (max (* width factor)
                          (* 0.1 width))
                        width)
        new-height (min (max (* height factor)
                          (* 0.1 height))
                     height)
        xf         (- (.-clientX e)
                     (/ (.-left (.getBoundingClientRect sv))
                       (.-width (.getBoundingClientRect sv))))

        yf         (- (.-clientY e)
                     (/ (.-top (.getBoundingClientRect sv))
                       (.-height (.getBoundingClientRect sv))))
        dx         (* xf
                     (- new-width width))
        dy         (* yf
                     (- new-height height))
        new-x      (- dx x)
        new-y      (- dy y)
        new-viewbox (str
                      new-x " "
                      new-y " "
                      new-width " "
                      new-height)]
    (println "vbox" vbox)
    (println "new viewbox" new-viewbox)
    (.setAttribute sv "viewBox" new-viewbox)
    #_(.setAttribute (.getElementById js/document "dotted-pattern") "width" new-width)
   #_ (.setAttribute (.getElementById js/document "dotted-pattern") "height" new-height)))

(defn on-zoom [e]
 (js/console.log "wheel" (.-deltaY e) e))


(e/defn view [size]
  (let [>zoom (m/observe (fn mount [emit!]
                           (let [f (fn [e]
                                     (let [delta (.-deltaY e)]
                                        (println "calling zoom" delta)
                                        (emit! e)))]
                             (.addEventListener dom/node "wheel" f)
                             (fn unmount []
                               (.removeEventListener dom/node "wheel" f)))))
        zoom  (new (m/reductions {} nil >zoom))]
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
      (when zoom
        (js/console.log "zoom" zoom)
        (set-on-zoom zoom))
      (background.))))

(e/defn main []
  (view. 1000)
  #_(dom/div
      (dom/props {:style {:height "100%"
                          :width "100%"
                          :display "flex"
                          :flex-direction "row"}})

      #_(dom/div
          (dom/props {:style {:display "flex"
                              :height "100%"
                              :width "80%"}})
          (view. 40000))
      (dom/div
        (dom/text "Hello world"))))

