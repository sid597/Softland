(ns app.electric-flow
  (:require [clojure.string :as str]
            [hyperfiddle.electric3 :as e]
            [hyperfiddle.electric-dom3 :as dom]
            [missionary.core :as m]
            #?@(:cljs [[app.client.webgpu.editor :as editor]
                       [app.client.webgpu.loop :as loop]
                       [global-flow :refer [await-promise]]])))

(def source-code 
  #?(:clj (slurp "src/app/electric_flow.cljc") 
     :cljs nil)) 


(defn tokenize-line [line-text]
  (if (empty? line-text)
    []
    (let [pattern #";.*|:[^ \[\]\(\)\s]+|[\(\)\[\]\{\}]|\"[^\"]*\"|\s+|[^ ;:\[\]\(\)\{\}\"\s]+"]
      (mapv (fn [token-text]
              {:text token-text
               :type (cond
                       (str/starts-with? token-text ";") :comment
                       (str/starts-with? token-text ":") :keyword
                       (str/starts-with? token-text "\"") :string
                       (str/blank? token-text)            :whitespace 
                       (contains? #{"(" ")" "[" "]" "{" "}"} token-text) :delimiter
                       (contains? #{"defn" "def" "let" "fn" "if" "do" "ns"} token-text) :macro
                       :else :text)})
            (re-seq pattern line-text)))))

(def theme
  {:keyword   {:r 0.8 :g 0.4 :b 0.8 :a 1.0}
   :macro     {:r 0.3 :g 0.6 :b 1.0 :a 1.0}
   :string    {:r 0.6 :g 0.8 :b 0.4 :a 1.0}
   :comment   {:r 0.5 :g 0.5 :b 0.5 :a 1.0}
   :delimiter {:r 1.0 :g 1.0 :b 1.0 :a 0.6}
   :text      {:r 0.9 :g 0.9 :b 0.9 :a 1.0}})

(defn get-color [type] (get theme type (:text theme)))

(defn layout-tokens [lines-of-tokens start-x start-y font-size]
  (let [char-width (* font-size 0.6)] 
    (:render-ops
      (reduce (fn [acc tokens]
                (let [line-y (:current-y acc)
                      line-result (reduce (fn [l-acc token]
                                            (let [txt (:text token)
                                                  width (* (count txt) char-width)
                                                  color (get-color (:type token))
                                                  op (merge token color
                                                            {:x (:current-x l-acc)
                                                             :y line-y
                                                             :size font-size})]
                                              {:current-x (+ (:current-x l-acc) width)
                                               :ops (conj (:ops l-acc) op)}))
                                          {:current-x start-x :ops []}
                                          tokens)]
                  {:current-y (+ line-y (* font-size 1.2))
                   :render-ops (conj (:render-ops acc) (:ops line-result))}))
              {:current-y start-y :render-ops []}
              lines-of-tokens))))

#?(:cljs
   (defn load-resources-async []
     (js/Promise.all
       ;; We REMOVED the code fetch from here. We only fetch binary assets.
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

(e/defn Prepare-Geometry [device pipelines render-ops atlas]
  (e/client
    {:text (editor/update-text-data device (:text-sys pipelines) render-ops atlas 19)
     :rect (editor/update-rects device (:rect-sys pipelines) [])
     :pipelines pipelines}))

(e/defn main [ring-request]
  (e/server
    (let [file-content source-code] 
      
      (e/client
        (binding [dom/node js/document.body]
          (dom/style {:margin "0" :padding "0" 
                      :width "100vw" :height "100vh" 
                      :overflow "hidden" :background "#111" 
                      :user-select "none"})
          
          (let [resources (LoadWebGPU)]
            (when resources
              (let [device (get resources :device)
                    format (get resources :format)
                    atlas (get resources :atlas)
                    pipelines (editor/create-editor-state resources)]
                
                (let [lines (str/split-lines file-content)
                      tokenized-lines (mapv tokenize-line lines)
                      render-ops (layout-tokens tokenized-lines 50 100 19)]
                  
                  (let [geometry (Prepare-Geometry device pipelines render-ops atlas)]
                    (dom/canvas
                      (dom/props {:id "webgpu-canvas" 
                                  :style {:width "100vw" :height "100vh" :display "block"}})
                      (let [ctx (.getContext dom/node "webgpu" #?(:cljs #js {:alpha true}))]
                        (.configure ctx (clj->js {:device device :format format :alphaMode "premultiplied"}))
                        (let [loop-flow (e/Task (loop/start-loop! dom/node device ctx geometry))]
                          (e/input loop-flow))))))))))))))
