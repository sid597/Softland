(ns app.electric-flow
  (:require [hyperfiddle.electric-de :as e :refer [$]]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]
            #?@(:cljs [[app.client.webgpu.core :as wcore :refer [upload-vertices]]
                       [global-flow :refer [await-promise
                                            mouse-down?>
                                            !canvas
                                            !adapter
                                            !device
                                            !context
                                            !command-encoder
                                            !format
                                            !all-rects
                                            !width
                                            !height
                                            !canvas-y
                                            !canvas-x
                                            !offset]]

                       [app.client.webgpu.data :refer [!rects]]])))

(hyperfiddle.rcf/enable!)


(e/defn create-random-rects [rc]
  (let [res (atom [])]
    (doseq [i (range rc)]
      (let [height (+ 20.0 (rand-int  40))
            width (+ 25.0 (rand-int 40))
            y (+ 0.1 (rand-int (e/watch !height)))
            x (+ 0.1 (rand-int (e/watch !width)))]
        (println "xx" x y)
        (swap! res concat [x y height width])))
    ;(println "DONE" (e/watch res) rc)
    res))


(e/defn setup-webgpu []
  (e/client
    (let [canvas (e/watch !canvas)
          canvas-x (e/watch !canvas-x)
          canvas-y (e/watch !canvas-y)
          height (e/watch !height)
          width (e/watch !width)
          [off-x off-y] (e/watch !offset)]
      (when canvas
        (let [context (.getContext canvas "webgpu")
              gpu      js/navigator.gpu
              adapter  ($ e/Task (await-promise (.requestAdapter gpu (clj->js {:requiredFeatures ["validation"]}))))
              device   ($ e/Task (await-promise (.requestDevice adapter)))
              cformat  (.getPreferredCanvasFormat gpu)
              config   (clj->js {:format cformat
                                 :device device})
              encoder (.createCommandEncoder device)
              nos 16
              rnd  ($ create-random-rects nos)
              all-rects  (e/watch !all-rects)]
          ;(println "rnd" (e/watch rnd))
          (reset! !all-rects @rnd)
          (.configure context config)
          (when (some? all-rects)
            (println "All rects" all-rects)
            ;(println "RND" rnd)
            (upload-vertices all-rects device cformat context
              [canvas-x canvas-y width height 0 0])
            (reset! !adapter adapter)
            (reset! !device device)
            (reset! !context context)
            (reset! !format cformat)
            (reset! !command-encoder encoder)))))))


(e/defn mouse-down-cords [node] (e/input (mouse-down?> node)))


(e/defn canvas-view []
  (let [canvas (e/watch !canvas)
        canvas-x (e/watch !canvas-x)
        canvas-y (e/watch !canvas-y)
        height (e/watch !height)
        width (e/watch !width)
        device (e/watch !device)
        context (e/watch !context)
        cformat (e/watch !format)
        all-rects  (into [] (e/watch !all-rects))
        [off-x off-y] (e/watch !offset)]
    (e/client
      (dom/canvas
        (dom/props {:id "top-canvas"
                    :height (e/watch !height)
                    :width (e/watch !width)})
        (reset! !canvas dom/node)
        (when-some [[start-x start-y] ($ mouse-down-cords dom/node)]
          (when-some [[end-x end-y] ($ dom/On "mousemove" (fn [e] [(.-clientX e) (.-clientY e)]))]
             (let [dx (* 2 (/ (- end-x start-x) width))
                   dy (* 2 (/ (- end-y start-y) height))]
               (println 'start-x start-x start-y end-x end-y dx dy)
               (println 'start-y start-x start-y end-x end-y dx dy)
               #_(upload-vertices
                    all-rects
                    device
                    cformat
                    context
                    [canvas-x canvas-y width height dx dy]))))))))





(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (let [dpr (.-devicePixelRatio js/window)]
         (reset! !width (.-clientWidth dom/node))
         (reset! !height (.-clientHeight dom/node))
         (reset! !canvas-x 0)
         (reset! !canvas-y 0)
         ($ canvas-view)
         ($ setup-webgpu)))))
