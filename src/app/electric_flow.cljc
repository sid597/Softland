(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]
            [hyperfiddle.electric-svg3]
            [hyperfiddle.incseq.mount-impl :refer [mount]]
            [hyperfiddle.kvs :as kvs]
            [hyperfiddle.domlike :as dl]
            [hyperfiddle.incseq :as i]
            #?@(:cljs [[app.client.webgpu.core :as wcore :refer [render-rect render-text]]
                       [global-flow :refer [await-promise
                                            mouse-down?>
                                            debounce
                                            !canvas
                                            !font-bitmap
                                            global-client-flow
                                            !adapter
                                            !global-atom
                                            !device
                                            !context
                                            !atlas-data
                                            !command-encoder
                                            !format
                                            !all-rects
                                            !width
                                            !height
                                            !canvas-y
                                            !visible-rects
                                            !old-visible-rects
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
(declare rect-ids)
(declare visible-rects)
(declare old-visible-rects)
(declare data-spine)
(declare global-atom)
(declare font-bitmap)
(declare atlas-data)


(defn create-random-rects [rects ch cw]
 (let [res (atom {})]
   ;(println "RAND" @res rc ch cw)
   (doseq [i rects]
    (let [height (+ 20.0 (rand-int  60))
          width (+ 145.0 (rand-int 60))
          y (+ 0.1 (rand-int ch))
          x (+ 0.1 (rand-int cw))]
        ;(js/console.log "xx" x y)
        ;(println i 'Create-random-rects (keyword (str i)) [x y height width])
        (swap! res assoc (keyword (str i)) [x y height width])))
   (println "all RECTS" @res)
   res))


#?(:cljs (defn clip-x [x w] (- (* 2 (/ x w)) 1)))
#?(:cljs (defn clip-y [y h] (- 1 (* 2 (/ y h)))))

(e/defn Setup-webgpu []
  (e/client
    (when (some? canvas)
      (js/console.log canvas)
      (let [context  (.getContext canvas "webgpu" (clj->js {:alpha true}))
            gpu      js/navigator.gpu
            adapter  (e/Task (await-promise (.requestAdapter gpu (clj->js {:requiredFeatures ["validation"]}))))
            device   (e/Task (await-promise (.requestDevice adapter)))
            cformat  (.getPreferredCanvasFormat gpu)
            config   (clj->js {:format cformat
                               :device device})]
        (.configure context config)
        (reset! !adapter adapter)
        (reset! !device device)
        (reset! !context context)
        (reset! !format cformat)))))
       


(e/defn Mouse-down-cords [node] (e/input (mouse-down?> node)))


(e/defn Add-panning []
  (when-some [[start-x start-y] (Mouse-down-cords canvas)]
    (let [[off-x off-y] (e/snapshot offset)]
      (dom/On "mousemove" 
              (fn [e]
                (.preventDefault e)
                (let [
                      end-x (.-clientX e)
                      end-y (.-clientY e)
                      start-clip-x  (clip-x start-x width)
                      start-clip-y  (clip-y start-y height)
                      end-clip-x  (clip-x end-x width)
                      end-clip-y  (clip-y end-y height)
                      new-pan-x   (+ off-x (- end-clip-x start-clip-x))
                      new-pan-y   (+ off-y (- end-clip-y start-clip-y))]
                  (reset! !offset [new-pan-x new-pan-y])
                  [new-pan-x new-pan-y]))))))



(e/defn Add-wheel []
  (dom/On "wheel"
          (fn [e] (.preventDefault e)
            (let [delta         (.-deltaY e)
                  rect          (.getBoundingClientRect (.-target e))
                  cursor-x      (- (.-clientX e) (.-left rect))
                  cursor-y      (- (.-clientY e) (.-top rect))
                  scale         (if (< delta 0) 1.06 0.95)
                  new-zoom      (* zoom-factor scale)
                  [off-x off-y] offset
                  clip-cursor-x (clip-x cursor-x width)
                  clip-cursor-y (clip-y cursor-y height)
                  pan-zoom      (- 1 scale)
                  current-pan-x (* clip-cursor-x pan-zoom)
                  current-pan-y (* clip-cursor-y pan-zoom)
                  prev-pan-x    (* off-x scale)
                  prev-pan-y    (* off-y scale)
                  total-pan-x   (+ prev-pan-x current-pan-x)
                  total-pan-y   (+ prev-pan-y current-pan-y)]
              (reset! !offset [total-pan-x total-pan-y])
              (reset! !zoom-factor new-zoom)
              [new-zoom  total-pan-x total-pan-y]))
          nil {:passive false}))


(e/defn Render-with-webgpu []
  (let [nu    (e/watch !offset)
        spend (e/Token nu)
        dv (e/snapshot device)
        con (e/snapshot context)
        fmat (e/snapshot format)]
    (when (and (some? atlas-data)
               (some? device)
               (some? font-bitmap)
               (some? context)
               (some? format))
      (when (some? spend)
        (let [rects-data    (flatten (into [] (vals all-rects)))
              rects-ids     (into [] (keys all-rects))
              [cx cy]       nu
              [off-x off-y] (spend (e/Task (m/sleep 25 nu)))
              rx            (e/amb cx off-x)
              ry            (e/amb cy off-y)
              texts         (reduce
                             (fn [acc [id data]]
                               (let [[x y dh dw] data
                                     left (+ (* (clip-x (+ 7 x) width)  zoom-factor) off-x)
                                     top  (+ (* (clip-y (+ 7 y) height) zoom-factor) off-y)]
                                 (conj acc {:x  left
                                            :y  top
                                            :text (str (name id))})))
                             []
                             all-rects)
              zof           (max 17 (* (/ 1 zoom-factor) 14))]
          (render-rect
            "zoom"
            rects-data
            dv
            fmat
            con
            [width height rx ry zoom-factor]
            rects-ids)
          (render-text
           dv
           fmat
           con
           16
           zof
           atlas-data
           font-bitmap
           texts))))))
  
    
(e/defn Tap-diffs
  ([f! x] 
   (f! (e/input (e/pure x)))
   x)
  ([x] (Tap-diffs prn x)))

(e/defn Canvas-view []
 (e/client
    (dom/canvas
      (dom/props {:id "top-canvas"
                  :height height
                  :width width})
      (reset! !canvas dom/node)
      (Render-with-webgpu)
            
      (when-some [down (Mouse-down-cords dom/node)]
        (println "DOWN")
        (reset! !global-atom {:cords down}))
     #_(e/for-by identity [node (e/as-vec (e/input (e/join (i/items data-spine))))]

                (println node global-atom) 
                #_(On-node-add node))
      (println "NEW SPINE"
                (count visible-rects)
                (e/input (i/count data-spine))
                visible-rects 
                (e/as-vec (e/input (e/join (i/items data-spine)))))
      (let [mount-items (mount
                           (fn [element child]          (do 
                                                          (data-spine 
                                                           child 
                                                           (fn [_ new]
                                                             new)
                                                           child)
                                                          (.push element child)
                                                         element))
                           (fn [element child previous] (do 
                                                          (let [idx (.indexOf element previous)]
                                                            (when (>= idx 0)
                                                              (aset element idx child)))
                                                          element))
                           (fn [element child sibling]  (do
                                                          (let [idx (.indexOf element sibling)]
                                                            (if (>= idx 0)
                                                              (.splice element idx 0 child)
                                                              (.push element child)))
                                                          element))
                           (fn [element child]          (do 
                                                          (data-spine
                                                               child 
                                                               (fn [_ new]
                                                                 new)
                                                               nil)
                                                          (let [idx (.indexOf element child)]
                                                            (when (>= idx 0)
                                                              (.splice element idx  1)))
                                                          element))
                           (fn [element i]              (do 
                                                          (aget element i))))

             diff       (e/input (e/pure (e/diff-by identity visible-rects)))]

         ((fn [] (when (some? diff) 
                   (mount-items (object-array @!old-visible-rects) diff))))))))
        


#?(:cljs (defn load-bitmap-file []
           (println "Load bitmap file")
           (-> (js/fetch   "/font_atlas.png")
               (.then #(.blob %))
               (.then #(js/createImageBitmap %))
               (.then (fn [img]
                         (reset! !font-bitmap img))))))
                            
#?(:cljs
   (defn read-json-file []
     (-> (js/fetch "/font_atlas.json")
         (.then (fn [response]
                  (.json response)))
         (.then (fn [data]
                  (reset! !atlas-data (js->clj data :keywordize-keys true)))))))


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
              zoom-factor (e/watch !zoom-factor)
              visible-rects (e/watch !visible-rects)
              old-visible-rects (e/watch !old-visible-rects)
              data-spine   (i/spine)
              rect-ids (vec (range 200))
              global-atom (e/watch !global-atom)
              font-bitmap (e/watch !font-bitmap)
              atlas-data (e/watch !atlas-data)]

      (let [dpr (.-devicePixelRatio js/window)]
        (reset! !width (.-clientWidth dom/node))
        (reset! !height (.-clientHeight dom/node))
        (reset! !canvas-x 0)
        (reset! !canvas-y 0)
        (reset! !offset [0 0])
        (reset! !zoom-factor 1)
        (load-bitmap-file)
        (read-json-file)
        (Canvas-view)
        (when-not (some nil? [canvas height width])
          (let [rnd     (create-random-rects rect-ids height width)]
            ;(println "RND" @rnd)
            (reset! !all-rects @rnd)
            (println "all-rects" all-rects)
            (when (and (some? font-bitmap) (some? all-rects))
              (do
               (println "total rncts" all-rects)
               (println "success canvas" canvas all-rects)
               (Setup-webgpu)
               (Add-panning)
               (Add-wheel)))))))))
