(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [hyperfiddle.electric-svg3]
            #?@(:cljs [[app.client.webgpu.editor :as editor]
                       [global-flow :refer [await-promise
                                            mouse-down?>
                                            !canvas
                                            !font-bitmap
                                            !text-renderer
                                            !global-atom
                                            !device
                                            !context
                                            !atlas-data
                                            !width
                                            !height
                                            !canvas-y
                                            !dpr
                                            !canvas-x]]])))
(hyperfiddle.rcf/enable!)

(e/declare ^:dynamic *canvas*)
(e/declare ^:dynamic *device*)
(e/declare ^:dynamic *context*)
(e/declare ^:dynamic *width*)
(e/declare ^:dynamic *height*)

;; These do not have earmuffs, so they don't need ^:dynamic 
;; unless you intend to rename them to *canvas-y*, etc.
(e/declare canvas-y)
(e/declare canvas-x)
(e/declare global-atom)
(e/declare font-bitmap)
(e/declare atlas-data)

(e/declare ^:dynamic *dpr*)
(e/declare text-renderer)
(e/declare ^:dynamic *editor-state*)
(e/declare gpu-format)
(e/declare ^:dynamic *scroll-y-atom*)


(defn layout-lines [raw-content start-y]
  "Pure function. 
  Input: Vector of maps {:text str :size int}
  Output: Vector of maps with calculated {:x :y} coordinates."
  (let [initial-state {:current-y start-y :lines []}]
    (:lines 
      (reduce (fn [acc line]
                (let [fsize (:size line)
                      ;; Logic: Move Y down by 1.2x font size per line
                      next-y (+ (:current-y acc) (* fsize 1.2))] 
                  {:current-y next-y
                   :lines (conj (:lines acc) 
                                (assoc line :x 50 :y (:current-y acc)))}))
              initial-state
              raw-content))))


#?(:cljs
   (defn load-resources-async []
     (js/Promise.all
       #js [(-> (js/fetch "/font_atlas.png")
                (.then #(.blob %))
                (.then #(js/createImageBitmap %)))
            (-> (js/fetch "/font_atlas.json")
                (.then #(.json %))
                (.then #(js->clj % :keywordize-keys true)))])))


(e/defn LoadWebGPU []
  (e/client
    (let [raw-resources (e/Task (await-promise (load-resources-async)))]
      (when raw-resources
        (let [bitmap (aget raw-resources 0)
              atlas  (aget raw-resources 1)
              gpu    js/navigator.gpu
              adapter (e/Task (await-promise (.requestAdapter gpu)))
              device  (e/Task (await-promise (.requestDevice adapter)))
              format  (.getPreferredCanvasFormat gpu)]

          (println "📦 Atlas Loaded. Keys:" (keys atlas))
          (when (and device atlas bitmap)
            (println "✅ Resources Loaded:" (.-width bitmap) "x" (.-height bitmap)) ;; Debug Log
            {:bitmap bitmap 
             :atlas  atlas 
             :device device 
             :format format
             :gpu    gpu}))))))


(e/defn WebGPU-Layer [device ctx pipelines atlas camera-state render-data]
  (e/client
    (let [text-sys     (:text-sys pipelines)
          rect-sys     (:rect-sys pipelines)
          upd-text-sys (editor/update-text-data device text-sys (:text-lines render-data) atlas 19)
          upd-rect-sys (editor/update-rects device rect-sys (:rects render-data))]

      (js/requestAnimationFrame
        (fn []
          (when (< (rand) 0.5) ;; Log once per second
            (println "📷 Camera State:" camera-state))
          (editor/draw-frame! device 
                              ctx 
                              upd-text-sys   ;; The updated system from step 1
                              upd-rect-sys 
                              camera-state))))))

(defn text-to-rects [lines]
  (mapv (fn [line]
          {:x (:x line)
           ;; Shift Rect Y up by the font size to match Baseline->Top-Left
           :y (- (:y line) (:size line)) 
           :w 500 ;; Or calculate based on text width if available
           :h (:size line)
           :r 0.0 :g 0.0 :b 1.0 :a 0.3})
        lines))

(e/defn Prepare-Geometry [device pipelines render-data atlas]
  (e/client
    (println "STATIC: Uploading 30k vertices to GPU...")
    ;; This calls your update-text-data-fast ONE time.
    {:text (editor/update-text-data device 
                                         (:text-sys pipelines) 
                                         (:text-lines render-data) 
                                         atlas 
                                         19)}))

;; 2. FAST MOVEMENT (Uniform Buffer)
;; This function takes the PRE-CALCULATED geometry and the CHANGING camera.
(e/defn Fast-Render-Loop [device ctx geometry camera-state]
  (e/client
    (js/requestAnimationFrame
      (fn []
        ;; This function ONLY updates the tiny Camera Buffer (24 bytes)
        ;; It does NOT touch the Geometry Buffer (Megabytes)
        (editor/draw-frame! device
                            ctx
                            (:text geometry)
                            nil ;; rects
                            camera-state)))))


(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (dom/style {:margin "0"
                  :padding "0"
                  :width "100vw"
                  :height "100vh"
                  :overflow "hidden"
                  :background "black"})
      (let [resources (LoadWebGPU)]
        (when resources
          (let [ win       js/window
                raw-w      (dom/On win "resize" (fn [_] (.-innerWidth win)) (.-innerWidth win) nil)
                raw-h      (dom/On win "resize" (fn [_] (.-innerHeight win)) (.-innerHeight win) nil)
                dpr        (dom/On win "resize" (fn [_] (or (.-devicePixelRatio win) 1.0)) (or (.-devicePixelRatio win) 1.0) nil)                ;; Ensure we never have NaNs even if DOM is not ready
                calc-w    (max 1.0 (* (or raw-w 100) dpr))
                calc-h    (max 1.0 (* (or raw-h 100) dpr))

                !scroll-y (atom 0)
                pipelines (editor/create-editor-state resources) ;; Assumes you renamed init logic to this or kept separate calls
                ;; ... inside the let bindings ...
                raw-content (mapv (fn [i] 
                                    {:text (str "Line " i ": Stress test verify GPU batching. " 
                                                "The quick brown fox jumps over the lazy dog. (19px)") 
                                     :size 19})
                                  (range 2100))
                ;; --- Data Model --
                #_#_raw-content [{:text "Title (64px)" :size 64}
                                 {:text "Subtitle (32px)" :size 32}
                                 {:text "Body text (19px)" :size 19}]]
            (let [layout-lines (layout-lines raw-content 100)
                  render-data  {:text-lines layout-lines
                                :rects      (text-to-rects layout-lines)}]

              (dom/canvas
                (dom/props {:id "webgpu-canvas" :width calc-w :height calc-h 
                            :style {:width "100vw" :height "100vh" :display "block"}})
                (let [ctx (.getContext dom/node "webgpu" #?(:cljs #js {:alpha true}))]
                  (.configure ctx (clj->js {:device (:device resources) :format (:format resources) 
                                            :width (js/Math.ceil calc-w) :height (js/Math.ceil calc-h)}))
                  (dom/On js/window "wheel" (fn [e] (.preventDefault e) (swap! !scroll-y + (.-deltaY e))) nil {:passive false})
                  (let [geometry (Prepare-Geometry (:device resources) 
                                                   pipelines 
                                                   render-data 
                                                   (:atlas resources))]
                        (let [
                              scroll-y (e/watch !scroll-y)
                              camera-state {:pan-x 0 :pan-y scroll-y :zoom 1.0 :width calc-w :height calc-h}]
                          (Fast-Render-Loop (:device resources) ctx geometry camera-state)      )))))))))))
