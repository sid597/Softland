(ns app.data)

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

#?(:clj (def !edges (atom [{:id "sv-line"
                            :x1 900
                            :y1 300
                            :x2 700
                            :y2 100
                            :type "line"
                            :to   "sv-circle"
                            :from "sv-circle1"
                            :color "black"}])))
#_(e/defn view []
    (dom/div
      (svg/svg
        (dom/props {:id    "sv"
                    :viewBox (clojure.string/join " " view-box)
                    :style {:min-width "100%"
                            :min-height "100%"
                            :top 0
                            :left 0}})
        (dom/on "pointermove" (e/fn [e]
                                (cond
                                  (and @is-dragging?
                                    (= "background"
                                      (:selection
                                        @current-selection))
                                    (:movable?
                                      @current-selection))    (let [[nx ny] (find-new-coordinates e)]
                                                                (println "gg")
                                                                (swap! !view-box assoc 0 nx)
                                                                (swap! !view-box assoc 1 ny)
                                                                (reset! !last-position {:x (.-clientX e) :y (.-clientY e)}))
                                  @!border-drag? (println "border draging"))))
        (dom/on "pointerdown" (e/fn [e]
                                (.preventDefault e)
                                (println "pointerdown svg")
                                (reset! current-selection {:selection (.-id (.-target e))
                                                           :movable? true})
                                (reset! !last-position {:x (.-clientX e) :y (.-clientY e)})
                                (reset! is-dragging? true)))
        (dom/on "pointerup" (e/fn [e]
                              (.preventDefault e)
                              (println "pointerup svg")
                              (reset! is-dragging? false)
                              (when @!border-drag?
                                (println "border draging up >>>")
                                (reset! !border-drag? false))))
        (dom/on "wheel" (e/fn [e]
                          (.preventDefault e)
                          (let [coords (browser-to-svg-coords e (.getElementById js/document "sv"))
                                wheel   (if (< (.-deltaY e) 0)
                                          1.01
                                          0.99)
                                new-view-box (direct-calculation view-box wheel coords)]
                            (reset! !zoom-level (* zoom-level wheel))
                            (reset! !view-box new-view-box))))
        (dot-background. "black")
        ;; Render nodes before the edges because edges depend on nodes
        (e/for [n nodes]
          (js/console.log "n" n identity)
          (circle. n))
        (e/for [ed edges]
          (js/console.log "ed" ed identity)
          (line. ed)))))