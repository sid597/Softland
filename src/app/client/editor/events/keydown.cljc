(ns app.client.editor.events.keydown
  (:require [clojure.string :as str]
            [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.utils :refer [letter-width rc cursor-width cursor-height pos new-line-pos s-x s-y ctx calc-new-position c-x c-y]]
            #?(:cljs [app.client.editor.events.utils :refer [!editor-text !pos !curr-pos settings]])
            [app.client.utils :refer [viewbox ui-mode subscribe]]))


(e/defn on-keydown [e]
  (e/client
    (let [key                (.-key e)
          code               (.-code e)
          pos                (e/client (e/watch !pos))
          key-width          (Math/round #_(max (.-actualBoundingBoxRight (.measureText ctx key)))
                                         (.-width (.measureText ctx key)))
          ;_  (js/console.log (.measureText ctx key))
          ;_                  (println "5. keydown the canvas." key "--" key-width "c-x" c-x "c-y" c-y "--" (.-width (.measureText ctx key)))
          #_#_[nx ny]  (calc-new-position. key-width)]
      (println "code" code "key" key)
      (cond
        (= "Space" code)           (swap! !editor-text str " ")
        (= "Enter" code)           (swap! !editor-text str "\n\n")
        (= "Backspace" code)       (swap! !editor-text #(subs % 0 (dec (count %))))
        (some? (@letter-width key)) (swap! !editor-text str key))
      (println "editor-text" @!editor-text)
     #_(cond
         (= "Enter" code)  (add-char-action. c-x c-y 20 (+ c-y 20) "")
         (= "Space" code)  (add-char-action. c-x c-y nx ny " ")
         :else             (add-char-action. c-x c-y nx ny key)))))


