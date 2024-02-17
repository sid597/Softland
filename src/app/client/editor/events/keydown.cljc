(ns app.client.editor.events.keydown
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.utils :refer [rc cursor-width cursor-height pos new-line-pos s-x s-y ctx calc-new-position c-x c-y]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos !curr-pos settings]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn add-text-at-pos [x y  text]
  (e/client
      (println "add-text-at-pos" x y)
      (set! (.-font ctx) "14px arial")
      ;(set! (.-textAlign ctx) "left")
      (set! (.-textBaseline ctx) "hanging")
      (.fillText ctx text x y)))



(e/defn add-char-action [ox oy nx ny text]
  (e/client
   (do
     (.clearRect ctx ox  oy 1 14)
     (add-text-at-pos.  ox oy text)
     (.fillRect ctx nx ny 1 14)
     (reset! !curr-pos [nx ny]))))

(e/defn on-keydown [e]
  (e/client
    (let [key                (.-key e)
          code               (.-code e)
          pos                (e/client (e/watch !pos))
          key-width          (Math/round #_(max (.-actualBoundingBoxRight (.measureText ctx key)))
                                         (.-width (.measureText ctx key)))
          _  (js/console.log (.measureText ctx key))
          _                  (println "5. keydown the canvas." key "--" key-width "c-x" c-x "c-y" c-y "--" (.-width (.measureText ctx key)))
          [nx ny]  (calc-new-position. key-width)]
      (println "code" code)
     (cond
       (= "Enter" code)  (add-char-action. c-x c-y 20 (+ c-y 20) "")
       (= "Space" code)  (add-char-action. c-x c-y nx ny " ")
       :else             (add-char-action. c-x c-y nx ny key)))))



