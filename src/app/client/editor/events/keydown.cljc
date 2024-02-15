(ns app.client.editor.events.keydown
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.utils :refer [pos new-line-pos s-x s-y ctx calc-new-position]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos !curr-pos]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn add-text-at-pos [x y  text]
  (e/client
      (println "add-text-at-pos" x y)
      (set! (.-font ctx) "16px roboto")
      (set! (.-textAlign ctx) "end")
      (set! (.-textBaseline ctx) "hanging")
      (.fillText ctx text x y)))


(e/defn on-keydown [e]
  (e/client
    (let [key        (.-key e)
          code       (.-code e)
          pos        (e/client (e/watch !pos))
          key-width  (Math/round (.-width (.measureText ctx key)))
          [nx ny]    (calc-new-position. key-width)]
     (println "keydown the canvas." key "--" key-width)
     (js/console.log (.measureText ctx key))
     (cond
       (= "Enter" code)  (reset! !pos [nx (+ ny 20)])
       (= "Space" code)  (reset! !pos [nx ny])
       :else             (do
                           (println "1---- x y" nx ny pos @!pos)
                           (add-text-at-pos. (+  nx key-width) ny  key)
                           (reset! !pos [(+  nx key-width)  ny])
                           (println "2---- x y" nx ny pos @!pos)
                           (reset! !curr-pos [(+ key-width nx)  ny]))))))
