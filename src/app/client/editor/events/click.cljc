(ns app.client.editor.events.click
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.keydown :refer [on-keydown]]
            [app.client.editor.events.utils :refer [pos cursor-height cursor-width rc s-x s-y ctx c-x c-y]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos settings]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn blinker-cursor []
  (e/client
    (let [x  (Math/round c-x)
          y (Math/round c-y)
          rect-width (*  1 s-x)]
      (println "rect width-->" rect-width "x" x "y" y "ctx" ctx)
      ;(println "x" x "y" y "d" (- x 3))
      (when (= 0 (int (mod e/system-time-secs 2)))
        (.fillRect ctx x y rect-width 20)
        (e/on-unmount #(do
                         (println "unmount" x y)
                         (.clearRect ctx x y rect-width 20)))))))


#?(:cljs (defn on-click [e sx sy]
             (println "clicked the canvas.")
             (js/console.log e)
             (let [rect (.getBoundingClientRect (.-currentTarget e))
                   rl (.-left rect)
                   rt (.-top rect)
                   dx  (* sx (- (.-clientX e) rl))
                   dy  (* sy (- (.-clientY e) rt))]

               (println "onclick --> dx" dx "dy" dy)
               (reset! !pos [dx dy])
               (println "pos" !pos))))