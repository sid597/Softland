(ns app.client.editor.events.utils
  (:require [hyperfiddle.electric :as e]
            [hyperfiddle.electric-dom2 :as dom]
            [app.client.flow-calc :as fc]))


#?(:cljs (def !pos  (atom [0 0])))
(e/def pos (e/client (e/watch !pos)))

#?(:cljs (def !curr-pos (atom [20 8])))
#?(:cljs (def settings
           {:c-width   (atom nil)
            :c-height  (atom nil)
            :d-width   (atom nil)
            :d-height  (atom nil)
            :s-x       (atom nil)
            :s-y       (atom nil)
            :ctx       (atom nil)
            ;; release cursor
            :rc (atom nil)
            :cursor-width  (atom 1)
            :cursor-height (atom 14)}))

(e/def c-width  (e/client (e/watch (:c-width settings))))
(e/def c-height (e/client (e/watch (:c-height settings))))
(e/def d-width (e/client (e/watch (:d-width settings))))
(e/def d-height (e/client (e/watch (:d-height settings))))
(e/def s-x      (e/client (e/watch (:s-x settings))))
(e/def s-y      (e/client (e/watch (:s-y settings))))
(e/def ctx      (e/client (e/watch (:ctx settings))))
(e/def c-x      (e/client (first (e/watch !curr-pos))))
(e/def c-y      (e/client (second (e/watch !curr-pos))))
(e/def rc       (e/client (e/watch (:rc settings))))
(e/def cursor-height (e/client (e/watch (:cursor-height settings))))
(e/def cursor-width (e/client (e/watch (:cursor-width settings))))

#?(:cljs (defn initialise-canvas [el rect dpr ctx]
           (let [dw     (.-width rect)
                 dh     (.-height rect)
                 width  (Math/round (* dpr  dw))
                 height (Math/round (* dpr dh))
                 sx     (Math/round (/ (.-width el) dw))
                 sy     (Math/round (/ (.-height el) dh))]
             (reset! (:c-width settings)  width)
             (reset! (:c-height settings) height)
             (reset! (:s-x settings) sx)
             (reset! (:s-y settings) sy)
             (reset! (:ctx settings) ctx)
             (reset! (:d-width settings) dw)
             (reset! (:d-height settings) dh)
             (reset! (:rc settings) (atom true))
             (reset! (:cursor-height settings) 14)
             (reset! (:cursor-width settings) 1)
             (println "1. Initialise canvas: ""sx" sx "sy" sy "width" width "height" height "dpr" dpr "dw" dw "dh" dh)
             (set! (.-webkitFontSmoothing (.-style el)) "antialiased")
             (set! (.-height el) width)
             (set! (.-width el) height)
             (.scale ctx dpr dpr)
             (set! (.-width (.-style el)) (str (.-width rect) "px"))
             (set! (.-height (.-style el)) (str (.-height rect) "px")))))




#?(:cljs (defn get-click-position [e]
           (let [rect (.getBoundingClientRect (.-currentTarget e))
                 rl   (.-left rect)
                 rt   (.-top rect)
                 dx   (* s-x (- (.-clientX e) rl))
                 dy   (* s-y (- (.-clientY e) rt))]
             (println "onclick --> dx" dx "dy" dy)
             (reset! !pos [dx dy])
             (println "pos" !pos))))


(e/defn new-line-pos [action]
  (e/client
     (cond
       (= (:enter action)) (reset! !pos [(first pos) (+ (second pos) 20)]))))

(e/defn calc-new-position [key-width]
  ;; calculate new position for the cursor to place on
  ;; if the current position is end of line then move to next line
  (e/client
    (let [new-line? (> (+ 20 c-x key-width) d-width)
          nx        (if new-line? 20 (+ c-x key-width))
          ny        (if new-line? (+ c-y 20) c-y)]
      (println "6. calc-new-position" c-x c-y new-line? nx ny)
      [nx ny])))