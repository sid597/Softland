(ns app.client.editor.events.keydown
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.utils :refer [pos new-line-pos]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn add-text-at-pos [x y ctx text]
  (e/client
      (println "add-text-at-pos" x y)
      (set! (.-font ctx) "16px roboto")
      (set! (.-textAlign ctx) "end")
      (set! (.-textBaseline ctx) "top")
      (.fillText ctx text x y)))


(e/defn on-keydown [e ctx]
  (e/client
    (let [key (.-key e)
          code (.-code e)
          pos (e/client (e/watch !pos))
          key-width  (+ 1 (Math/round (.-width (.measureText ctx key))))
          x (first pos)
          y (second pos)
          res (atom {})]
     (println "keydown the canvas." key "--" key-width)
     (js/console.log (.measureText ctx key))

     (println "res" @res)

     (cond
       (= "Enter" code)  (reset! !pos [x (+ y 20)])
       (= "Space" code)  (reset! !pos [(+ x key-width) y])
       :else             (do
                           (println "1---- x y" x y pos @!pos)
                           (add-text-at-pos. (+  x key-width) y ctx key)
                           (reset! !pos [(+  x key-width)  y])
                           (println "1---- x y" x y pos @!pos))))))

