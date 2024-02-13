(ns app.client.editor.core
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn blinker-cursor [pos ctx]
  (println "pos is " pos)
  (e/client
    (let [x (first pos)
          y (second pos)]
      (println "x" x "y" y)
      (when (= 0 (int (mod e/system-time-secs 2)))
        (.fillRect ctx x y 2 20)
        (e/on-unmount #(do (println 'component-will-unmount)
                           (.clearRect ctx x y 2 20)))))))


(e/defn canvas [id]
  (e/client
    (let [id (str "canvas-"id)
          pos (atom [100 100])]
      (dom/canvas
        (dom/props {:id id
                    :height 800
                    :width 400
                    :style {:border "1px solid black"}})
        (blinker-cursor. (e/client (e/watch pos)) (.getContext (.getElementById js/document id) "2d"))
        (dom/on "click" (e/fn [e]
                          (e/client
                            (println "clicked the canvas.")
                            (js/console.log e)
                            (let [rect (.getBoundingClientRect (.-currentTarget e))
                                  rl (.-left rect)
                                  rt (.-top rect)
                                  dx  (- (.-clientX e) rl)
                                  dy  (- (.-clientY e) rt)
                                  el (.getElementById js/document id)
                                  ctx (.getContext el "2d")]
                              (reset! pos [dx dy])
                              (println "pos" pos)))))))))




#_(e/defn canvas [id]
    (e/client
      (dom/canvas
        (dom/props {:id id :height 800 :width 400 :style {:border "1px solid black" :z-index 3}})
        (if (= 0 (int (mod e/system-time-secs 2)))
          (BlinkerComponent. id (.getContext (.getElementById js/document id) "2d"))))))