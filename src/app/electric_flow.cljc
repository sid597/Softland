(ns app.electric-flow
  (:require [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            #?@(:cljs [[app.client.webgpu.editor :as editor]
                       [app.client.webgpu.loop :as loop]
                       [global-flow :refer [await-promise]]])))

(defn layout-lines [raw-content start-y]
  (let [initial-state {:current-y start-y :lines []}]
    (:lines 
      (reduce (fn [acc line]
                (let [fsize (:size line)
                      next-y (+ (:current-y acc) (* fsize 1.2))] 
                  {:current-y next-y
                   :lines (conj (:lines acc) (assoc line :x 50 :y (:current-y acc)))}))
              initial-state raw-content))))

(defn text-to-rects [lines]
  (mapv (fn [line] {:x (:x line) :y (- (:y line) (:size line)) 
                    :w 500 :h (:size line) 
                    :r 0.0 :g 0.0 :b 1.0 :a 0.3}) lines))

#?(:cljs
   (defn load-resources-async []
     (js/Promise.all
       #js [(-> (js/fetch "/font_atlas.png") (.then #(.blob %)) (.then #(js/createImageBitmap %)))
            (-> (js/fetch "/font_atlas.json") (.then #(.json %)) (.then #(js->clj % :keywordize-keys true)))])))

(e/defn LoadWebGPU []
  (e/client
    (let [raw (e/Task (await-promise (load-resources-async)))]
      (when raw
        (let [bitmap (aget raw 0) 
              atlas (aget raw 1) 
              gpu js/navigator.gpu
              adapter (e/Task (await-promise (.requestAdapter gpu)))
              device (e/Task (await-promise (.requestDevice adapter)))
              format (.getPreferredCanvasFormat gpu)]
          (when (and device atlas bitmap)
            {:bitmap bitmap :atlas atlas :device device :format format}))))))

(e/defn Prepare-Geometry [device pipelines render-data atlas]
  (e/client
    {:text (editor/update-text-data device (:text-sys pipelines) (:text-lines render-data) atlas 19)
     :rect (editor/update-rects device (:rect-sys pipelines) (:rects render-data))
     :pipelines pipelines}))

(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (dom/style {:margin "0" :padding "0" 
                  :width "100vw" :height "100vh" 
                  :overflow "hidden" :background "black"
                  :user-select "none"})
      (let [resources (LoadWebGPU)]
        (when resources
          (let [device (get resources :device)
                format (get resources :format)
                atlas (get resources :atlas)]
            (let [pipelines (editor/create-editor-state resources)]
              (let [raw-content (mapv (fn [i] {:text (str "Line " i "...") :size 19}) (range 10000))
                    layout (layout-lines raw-content 100)
                    render-data {:text-lines layout :rects (text-to-rects layout)}]
                (let [geometry (Prepare-Geometry device pipelines render-data atlas)]
                  (dom/canvas
                    (dom/props {:id "webgpu-canvas" 
                                :style {:width "100vw" :height "100vh" :display "block"}})
                    (let [ctx (.getContext dom/node "webgpu" #?(:cljs #js {:alpha true}))]
                      (.configure ctx (clj->js {:device device :format format :alphaMode "premultiplied"}))
                      (let [loop-flow (loop/start-loop! dom/node device ctx geometry)]
                        (let [loop-state (e/input loop-flow)]
                          loop-state)))))))))))))
