(ns app.client.editor.core
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]
            [app.client.editor.events.keydown :refer [on-keydown]]
            [app.client.editor.events.click :refer [on-click blinker-cursor]]
            [app.client.utils :refer [viewbox ui-mode subscribe]]
            [app.client.editor.events.utils :refer [pos]]
            #?(:cljs [app.client.editor.events.utils :refer [!pos]])))

(e/defn canvas [id]
  (e/client
    (let [id  (str "canvas-"id)
          dpr (or (.-devicePixelRatio js/window) 1)]
      (println "pos" pos "dpr" dpr)

      (dom/canvas
        (dom/props {:id id
                    :height 800
                    :width 400
                    :tabIndex 0
                    :style {:border "1px solid black"
                            :background-color "beige"}})
        (let [!text (atom "")
              text  (e/watch !text)
              el    (.getElementById js/document id)
              ctx   (.getContext el "2d")
              rect  (.getBoundingClientRect el)
              width  (* dpr  (.-width rect))
              height (* dpr  (.-height rect))
              sx     (/ (.-width el) (.-width rect))
              sy    (/ (.-height el) (.-height rect))]
          (set! (.-webkitFontSmoothing (.-style el)) "antialiased")
          (set! (.-height el) height)
          (set! (.-width el) width)
          (.scale ctx dpr dpr)
          (set! (.-width (.-style el)) (str (.-width rect) "px"))
          (set! (.-height (.-style el)) (str (.-height rect) "px"))
          (println "canvas width" (.-width el))
          (blinker-cursor. (.getContext (.getElementById js/document id) "2d") sx sy)
          (dom/on "click" (e/fn [e]
                            (on-click. e sx sy)))
          (dom/on "keydown" (e/fn [e]
                              (on-keydown. e ctx sx sy))))))))


