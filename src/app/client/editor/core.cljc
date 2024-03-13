(ns app.client.editor.core
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.keydown :refer [on-keydown]]
            [app.client.editor.parser :refer [parse-doc parse-text]]
            [app.client.editor.events.click :refer [blinker-cursor]]
            [app.client.utils :refer [viewbox ui-mode subscribe]]
            [app.client.editor.events.utils :refer [c-x c-y pos]]
            #?@(:cljs [[app.client.editor.events.utils :refer [!pos initialise-canvas]]

                       [app.client.editor.events.click :refer [on-click]]])))


#?(:cljs (defn oc []
           (println "clicked the canvas.")))
(e/defn canvas [id]
  (e/client
    (let [id  (str "canvas-"id)
          dpr (or (.-devicePixelRatio js/window) 1)]
      (println "pos" pos "dpr" dpr)

      (dom/canvas
        (dom/props {:id id
                    :tabIndex 0
                    :height 800
                    :width 800
                    :style {:border "1px solid red"
                            :margin "30px"
                            :background-color "beige"}})
        (let [!text  (atom "")
              text   (e/watch !text)
              el     (.getElementById js/document id)
              ctx    (.getContext el "2d")
              rect   (.getBoundingClientRect el)]
          (initialise-canvas el rect dpr ctx)
          #_(parse-doc.)
          #_(blinker-cursor.)
          (parse-text.)
          (dom/on! "click" (fn [e]
                             (println "4. clicked canvas")
                             (.fillRect ctx c-x c-y 1 14)
                             #_(on-click. e)))
          (dom/on "keydown" (e/fn [e]
                              (on-keydown. e))))))))
