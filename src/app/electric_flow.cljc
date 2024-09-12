(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
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
                                            !zoom-factor
                                            !offset]]

                       [app.client.webgpu.data :refer [!rects]]])))

(hyperfiddle.rcf/enable!)


(declare canvas)
(declare squares)
(declare adapter)
(declare device)
(declare context)
(declare format)
(declare command-encoder)
(declare all-rects)
(declare width)
(declare height)
(declare canvas-y)
(declare canvas-x)
(declare offset)
(declare zoom-factor)



(e/defn Create-random-rects [rc ch cw]
  (let [res (atom [])]
    (doseq [i (range rc)]
      (let [height (+ 20.0 (rand-int  40))
            width (+ 25.0 (rand-int 40))
            y (+ 0.1 (rand-int ch))
            x (+ 0.1 (rand-int cw))]
        (js/console.log "xx" x y)
        (swap! res concat [x y height width])))
    ;(println "DONE" (e/watch res) rc)
    (println "total rects" (count @res))
    res))


(e/defn Setup-webgpu []
  (e/client
    (when (some? canvas)
      (js/console.log canvas)
      (let [context (.getContext canvas "webgpu")
            gpu      js/navigator.gpu
            adapter  (e/Task (await-promise (.requestAdapter gpu (clj->js {:requiredFeatures ["validation"]}))))
            device   (e/Task (await-promise (.requestDevice adapter)))
            cformat  (.getPreferredCanvasFormat gpu)
            config   (clj->js {:format cformat
                               :device device})]

        ;(println "rnd" (e/watch rnd))
        (.configure context config)
        (reset! !adapter adapter)
        (reset! !device device)
        (reset! !context context)
        (reset! !format cformat)
        (when (some? all-rects)
          (let [ronce (e/snapshot all-rects)
                rheight (e/snapshot height)
                rwidth (e/snapshot width)
                rzoom  (e/snapshot zoom-factor)
                [off-x off-y] (e/snapshot offset)]
            (println "ronce" (count ronce) rheight rwidth rzoom)
            (upload-vertices "initial" ronce device cformat context
              [rwidth rheight off-x off-y rzoom])))))))



(e/defn Mouse-down-cords [node] (e/input (mouse-down?> node)))

(e/defn Add-panning []
  (when-some [[start-x start-y] (Mouse-down-cords canvas)]
    (let [[off-x off-y] (e/snapshot offset)]
      (when-some [[end-x end-y] (dom/On "mousemove" (fn [e]
                                                      (.preventDefault e)
                                                      (let [clip-x (fn [x] (- (* 2 (/ x width)) 1))
                                                            clip-y (fn [y] (- 1 (* 2 (/ y height))))
                                                            end-x (.-clientX e)
                                                            end-y (.-clientY e)
                                                            start-clip-x  (clip-x start-x)
                                                            start-clip-y  (clip-y start-y)
                                                            end-clip-x  (clip-x end-x)
                                                            end-clip-y  (clip-y end-y)
                                                            new-pan-x   (+ off-x (- end-clip-x start-clip-x))
                                                            new-pan-y   (+ off-y (- end-clip-y start-clip-y))]
                                                        (reset! !offset [new-pan-x new-pan-y])
                                                        [new-pan-x new-pan-y])))]
          (upload-vertices
            "panning"
            all-rects
            device
            format
            context
            [width height end-x end-y zoom-factor])))))

(e/defn Add-wheel []
  (when-some [[zf cx cy ] (dom/On "wheel" (fn [e] (.preventDefault e)
                                              (let [delta         (.-deltaY e)
                                                    rect          (.getBoundingClientRect (.-target e))
                                                    cursor-x      (- (.-clientX e) (.-left rect))
                                                    cursor-y      (- (.-clientY e) (.-top rect))
                                                    scale         (if (< delta 0) 1.06 0.95)
                                                    new-zoom      (* zoom-factor scale)
                                                    [off-x off-y] offset
                                                    clip-cursor-x (- (* 2 (/ cursor-x width)) 1)
                                                    clip-cursor-y (- 1 (* 2 (/ cursor-y height)))
                                                    pan-zoom      (- 1 scale)
                                                    current-pan-x (* clip-cursor-x pan-zoom)
                                                    current-pan-y (* clip-cursor-y pan-zoom)
                                                    prev-pan-x    (* off-x scale)
                                                    prev-pan-y    (* off-y scale)
                                                    total-pan-x   (+ prev-pan-x current-pan-x)
                                                    total-pan-y   (+ prev-pan-y current-pan-y)]
                                                (reset! !offset [total-pan-x total-pan-y])
                                                (reset! !zoom-factor new-zoom)
                                                [new-zoom  total-pan-x total-pan-y]

                                                #_[new-zoom clip-pan-x clip-pan-y cursor-x cursor-y]))
                              nil {:passive false})]
        (upload-vertices "zoom" all-rects device format context
          [width height cx cy zf])))


(e/defn Canvas-view []
  (e/client
    (dom/canvas
      (dom/props {:id "top-canvas"
                  :height height
                  :width width})
      (reset! !canvas dom/node))))



(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body
              canvas (e/watch !canvas)
              canvas-x (e/watch !canvas-x)
              canvas-y (e/watch !canvas-y)
              height (e/watch !height)
              width (e/watch !width)
              device (e/watch !device)
              format (e/watch !format)
              context (e/watch !context)
              all-rects (e/watch !all-rects)
              offset    (e/watch !offset)
              zoom-factor (e/watch !zoom-factor)]
      (let [dpr (.-devicePixelRatio js/window)]
        (reset! !width (.-clientWidth dom/node))
        (reset! !height (.-clientHeight dom/node))
        (reset! !canvas-x 0)
        (reset! !canvas-y 0)
        (reset! !offset [0 0])
        (reset! !zoom-factor 1.0)
        (Canvas-view)
       (when-not (some nil? [canvas height width])
         (let [nos     200
               rnd     (Create-random-rects nos height width)]
           (reset! !all-rects @rnd)
           (when (some? all-rects)
             (println "total rncts" (count all-rects))
             (do
               (js/console.log "success canvas" canvas all-rects)
               (Setup-webgpu)
               (Add-panning)
               (Add-wheel)))))))))
