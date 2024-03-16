(ns app.client.editor.core
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.keydown :refer [on-keydown]]
            [app.client.editor.parser :refer [parse-doc parse-text nested-bold parsed->canvas-text]]
            [app.client.editor.events.click :refer [blinker-cursor]]
            [app.client.utils :refer [viewbox ui-mode subscribe]]
            [app.client.editor.events.utils :refer [editor-text letter-width c-x c-y pos]]
            [clojure.string :as string]
            #?@(:cljs [[app.client.editor.events.utils :refer [settings !pos initialise-canvas]]
                       [app.client.editor.events.click :refer [on-click]]])))


#?(:cljs (defn oc []
           (println "clicked the canvas.")))

(e/defn canvas [id]
  (e/client
    (let [id  (str "canvas-"id)
          dpr (or (.-devicePixelRatio js/window) 1)]
      (println "pos" pos "dpr" dpr)

      (dom/div
        (dom/props
          {:style {:overflow "scroll"}})
        (dom/canvas
          (dom/props {:id id
                      :tabIndex 0
                      :height 800
                      :width 800
                      :style {:border "1px solid red"
                              :margin "30px"
                              :background-color "beige"}})
          (let [el           (.getElementById js/document id)
                ctx          (.getContext el "2d")
                rect         (.getBoundingClientRect el)
                !text        (atom "Hello **world** ")
                text         (e/client (e/watch !text))
                !parsed-text (atom (nested-bold text))
                !canvas-text (atom (parsed->canvas-text @!parsed-text ctx settings))
                canvas-text  (e/client (e/watch !canvas-text))
                x-pos        (atom 0)
                y-pos        (atom 20)]
            (initialise-canvas el rect dpr ctx)
            #_(parse-doc.)
            #_(blinker-cursor.)
            #_(parse-text.)
            (e/for-by (fn [x]
                        (let [ky (str (:x x) "--" (:y x) "--" (:text x)"--" (:type x))]
                         (println "for-by key: "  ky)
                         ky))
              [node canvas-text]
              (let [ch (:text node)
                    x (:x node)
                    y (:y node)
                    type (:type node)]
                (do
                  (println "---some change:--- " ch x y type @x-pos)
                  (println "c" ch x)
                  (println "canvas text")
                  (cond
                    (= :text  type) (set! (.-font ctx) "200  17px IA writer Quattro S")
                    (= :bold  type) (set! (.-font ctx) "bold 17px IA writer Quattro S"))
                  (if (> @x-pos x)
                    (do
                      (println "XPOS >>>>>>> X" @x-pos x (- @x-pos x))
                      (.clearRect ctx x @y-pos (- @x-pos x) 20)
                      (.fillText ctx ch x y)
                      (reset! x-pos x))
                    (do
                      (println "XPOS <<<<< X" @x-pos x)
                      (.fillText ctx ch x y)
                      (reset! x-pos x)
                      (reset! y-pos y)))))

              #_(println "-----------t---------" t "--" "--")
              #_(println "-----------render-text---------" @!render-text "--" t))
            #_(e/for-by identity [node render-text]
                (println "node _-->" node)
                (.clearRect ctx 0 0 800 #_(letter-width (last @!text)) 200)
                (reset! x 0)
                (reset! y 0)
                (e/for-by identity [c (reverse (:content node))]
                 (println "fby" c)
                 (println "c" c @x (letter-width c))
                 (.fillText ctx c @x @y)
                 (reset! x (+ @x (letter-width c)))))
            (dom/on! "click" (fn [e]
                               (println "4. clicked canvas")
                               #_(on-click. e)))
            (dom/on "keydown" (e/fn [e]
                                (let [key                (.-key e)
                                      code               (.-code e)]
                                  (println "code" code "key" key)
                                  (cond
                                    (= "Space" code)           (swap! !text str " ")
                                    (= "Enter" code)           (swap! !text str "\n\n")
                                    (= "Backspace" code)       (do
                                                                 (println "backspace" @!text @x-pos)
                                                                 (reset! x-pos (- @x-pos (@letter-width (last @!text))))
                                                                 (.clearRect ctx @x-pos @y-pos (@letter-width (last @!text)) 20
                                                                  (println "text before " @!text))
                                                                 (reset! !text (subs @!text 0 (dec (count @!text))))
                                                                 (println "text after " @!text))
                                    (some? (@letter-width key)) (swap! !text str key #_(fn [s]
                                                                                         (str (subs s 0 1 ) "--" key (subs s 1 (count s)))))))))))))))

