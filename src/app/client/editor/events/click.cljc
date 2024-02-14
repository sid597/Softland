(ns app.client.editor.events.click
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.utils :refer [pos]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn blinker-cursor [ctx sx sy]
  (e/client
    (let [x  (Math/round (first pos))
          y (Math/round (second pos))
          rect-width (*  1 sx)]
      (println "x" x "y" y "d" (- x 3))
      (when (= 0 (int (mod e/system-time-secs 2)))
        (.fillRect ctx x y rect-width 20)
        (e/on-unmount #(do
                         (println "unmount" x y)
                         (.clearRect ctx  x  y rect-width 20)))))))


(e/defn on-click [e sx sy]
  (e/client
    (println "clicked the canvas.")
    (js/console.log e)
    (let [rect (.getBoundingClientRect (.-currentTarget e))
          rl (.-left rect)
          rt (.-top rect)
          dx  (* sx (- (.-clientX e) rl))
          dy  (* sy (- (.-clientY e) rt))]

      (reset! !pos [dx dy])
      (println "pos" !pos))))