(ns app.electric-flow
  (:require [hyperfiddle.electric-de :as e :refer [$]]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]))

(hyperfiddle.rcf/enable!)

#?(:cljs
   (defn resize-observer [node]
     (do
       (println "resize observer")
       (m/relieve {}
         (m/observe (fn [!] (! [(.-clientHeight node)
                                (.-clientWidth node)])
                      (let [obs (new js/ResizeObserver
                                  (fn [entries]
                                    (let [content-box-size (-> entries (aget 0) .-contentBoxSize (aget 0))]
                                      (! [(.-blockSize content-box-size)
                                          (.-inlineSize content-box-size)]))))]
                        (.observe obs node) #(.unobserve obs))))))))

#?(:cljs
   (do
     (defonce !body (atom nil))
     (defonce !canvas (atom nil))
     (defonce !canvas-h (atom nil))
     (defonce !canvas-w (atom nil))
     (defonce !ctx (atom nil))))

(e/defn view []
  (e/client
    (dom/canvas
      (dom/props {:id "top-canvas"
                  :width (e/watch !canvas-w)
                  :height (e/watch !canvas-h)
                  :style {:height (e/watch !canvas-h)
                          :width (e/watch !canvas-w)
                          :background-color "red"}})
      (reset! !canvas dom/node))))

(e/defn init []
  (let [[h w] (e/input (resize-observer (e/watch !body)))
        canvas (e/watch !canvas)
        dpr (.-devicePixelRatio js/window)]
    (reset! !canvas-h (* h dpr))
    (reset! !canvas-w (* w dpr))
    (reset! !ctx (.getContext canvas "2d"))))




(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (reset! !body dom/node)
      (println "HELLO WORLD! ")
      ($ view)
      ($ init))))
