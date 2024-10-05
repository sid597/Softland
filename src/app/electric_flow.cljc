(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]
            [hyperfiddle.electric-svg3]
            [hyperfiddle.incseq.mount-impl :refer [mount]]
            [hyperfiddle.kvs :as kvs]
            [hyperfiddle.incseq :as i]
            #?@(:cljs [[app.client.webgpu.core :as wcore :refer [upload-vertices]]
                       [global-flow :refer [await-promise
                                            mouse-down?>
                                            !canvas
                                            global-client-flow
                                            !adapter
                                            !global-atom
                                            current-time-ms
                                            !device
                                            !context
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


(e/defn Create-random-rects [rc ch cw]
  (let [res (atom [])]
    (doseq [i (range rc)]
      (let [height (+ 20.0 (rand-int  40))
            width (+ 25.0 (rand-int 40))
            y (+ 0.1 (rand-int ch))
            x (+ 0.1 (rand-int cw))]
        ;(js/console.log "xx" x y)
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
              [rwidth rheight off-x off-y rzoom]
              rect-ids)))))))



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
        (do
          (swap! !old-visible-rects (constantly @!visible-rects))
          (upload-vertices
            "panning"
            all-rects
            device
            format
            context
            [width height end-x end-y zoom-factor]
            rect-ids))))))

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
                                                (swap! !old-visible-rects (constantly @!visible-rects))
                                                [new-zoom  total-pan-x total-pan-y]


                                                #_[new-zoom clip-pan-x clip-pan-y cursor-x cursor-y]))
                                  nil {:passive false})]
      (upload-vertices "zoom" all-rects device format context
        [width height cx cy zf]
        rect-ids)))

(e/defn On-node-add [id]
  ;; signature 
  ;; on add need to ad to spine then create the watchers etc. or do I even need to add to the spine? 
  ;; not sure but this seems the right place to do such thing. 
  ;; how?   
  ;; I think I am thinking ahead adding is easy but how do I remove? 
  ;; I think the task and flow pattern would come in handy like there is a on success and on cancel 
  ;; think. If its canceled then the thing is gone. 
  ;; I need to find how to do that
  (println id "nf" global-atom visible-rects))
    
(e/defn Tap-diffs
  ([f! x] 
   (f! (e/input (e/pure x)))
   x)
  ([x] (Tap-diffs prn x)))

(e/defn Get-diffs [x]
  (println "DIFFS FOR" x)
  [x (e/input (e/pure (e/diff-by identity x)))])

(e/defn Canvas-view []
 (e/client
    (dom/canvas
      (dom/props {:id "top-canvas"
                  :height height
                  :width width})
      (reset! !canvas dom/node)
      #_(when-some [down (Mouse-down-cords dom/node)]
          (println "DOWN")
          (reset! !global-atom {:cords down}))
     #_ (e/for-by identity [node (e/as-vec (e/input (e/join (i/items data-spine))))]

                  (println node global-atom) 
                  #_(On-node-add node))
     (let [spine-count (e/input (i/count data-spine))
           spine-el 
                      (e/as-vec (e/input (e/join (i/items data-spine))))]
      ((fn [] (when (some? spine-el)
                (do (println (current-time-ms)
                             "NEW SPINE"
                             (count @!visible-rects)
                             (count spine-el)
                             (count @!old-visible-rects)

                             @!visible-rects 
                             spine-el
                              ;; Find all the elements in a data spine and show them in a vec representation.
                             @!old-visible-rects)))))
      (let [mount-items (mount
                          (fn [element child]          (do 
                                                          ;(println "append child" element "::" child)
                                                          (data-spine 
                                                           child 
                                                           (fn [old new]
                                                             #_(println "S: append OLD" old "new" new)
                                                             new)
                                                           ;(fn ll [] (println "I AM RUNNING" child))
                                                           child)))
                                                           
                          (fn [element child previous] (do (println "replace child" element "::" child "::" previous)))
                                                           
                          (fn [element child sibling]  (println "insert child" element "::" child "::" sibling))
                                                           
                          (fn [element child]          (do (println (current-time-ms)   "remove child" element "::" child)
                                                           (data-spine
                                                                child 
                                                                (fn [old new]
                                                                  ;(println "S: remove child" child "OLD" old "NEW" new)
                                                                  new)
                                                                nil)))
                          (fn [element i]            (do (println "NODES child" i "::" element "::" (nth element i nil)) 
                                                         (nth element i nil))))


            ;; My understanding of whats going on here is that 
            ;; we create diffs (not from the inseq ns) but from the electric ns. Theses diffs are returned as a vec 
            ;; but we want to know the actual diff that was produced not the reconsiled version of it. So we use other 
            ;; functison from electric ns and undo the work being done by e/diff-by. So we use e/pure to get the pure 
            ;; version of each output of e/diff-by then use e/input to read the value returned.


            ;; What is a table? 

            ;; its a width-1 diff for e.g any value type e.g "hello" or a vector

            ;; I think I can use fixed_impl/flow as an inspiration for my long lived flow functions?? 
            ;; and manage the termination with the help of diff-by and spine?? 

            diff (e/input (e/pure (e/diff-by identity visible-rects)))]

         ((fn [] (when (some? diff) 
                   (do
                    (println (current-time-ms) "DIFF" diff @!visible-rects)
                    (mount-items @!old-visible-rects diff)
                    (swap! !old-visible-rects (constantly @!visible-rects))))))
                                  
      

        ;(println "Diff" visible-rects "by" diff)
        ;(println "--VISIBLE--" visible-rects "::" old-visible-rects)


        ;; Learning: This is how to use a non-reactive value with a reactive one. 
        ;; xy problem in this case is: if there is a new diff run mount items function
        ;; with the old visible rects and the diff. Note that we need old version of the rects array
        ;; because diff is calculated based on old vs new value of the array. Since the array changes we 
        ;; get a diff on that array so we need to have access to the old array in the mount to let us know which item 
        ;; was deleted. 
        ;(Tap-diffs diff)
        ;(println "==" (e/input (e/pure "HELLO" ))"::"(e/input (e/pure zoom-factor)))
        ;(println "===" (e/input (m/watch (atom "hello"))))
        ;(println "====" (e/join (e/pure visible-rects)))
        ;;; create flow of diffs for the value vieible-rects
        ;(println "==0-0" (e/join (i/fixed (e/pure visible-rects))))
        ;(println "==s" (e/pure visible-rects)
        ;  (e/join (i/fixed (e/pure visible-rects)))
        ; "::" #_(e/input (e/pure visible-rects))
        #_(println "TT" (->> (e/pure visible-rects)
                             (m/reductions (fn [x d]
                                             (do
                                               (println "patch vec" x d)
                                               (i/patch-vec))))
                                         
                             (m/latest (fn [c]
                                        (do (println "CC" c)
                                          (eduction cat c))))
                             (i/diff-by identity)
                             (e/join)
                             (e/pure)
                             (e/input)) 
                   "::" visible-rects))))))
                           
        
        




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
              rect-ids     (vec (range 1000 1201))
              visible-rects (e/watch !visible-rects)
              old-visible-rects (e/watch !old-visible-rects)
              data-spine   (i/spine)
              global-atom (e/watch !global-atom)]

      (let [dpr (.-devicePixelRatio js/window)]
        (reset! !width (.-clientWidth dom/node))
        (reset! !height (.-clientHeight dom/node))
        (reset! !canvas-x 0)
        (reset! !canvas-y 0)
        (reset! !offset [0 0])
        (reset! !zoom-factor 3)
        (Canvas-view)
        (println "rects " rect-ids)
       (when-not (some nil? [canvas height width])
         (let [nos     20
               rnd     (Create-random-rects nos height width)]

           (reset! !all-rects @rnd)
           (when (some? all-rects)
             (println "total rncts" (count all-rects) @rnd)
             (do
               (js/console.log "success canvas" canvas all-rects)
               (Setup-webgpu)
               (Add-panning)
               (Add-wheel)))))))))
