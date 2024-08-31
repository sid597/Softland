(ns app.electric-flow
  (:require [hyperfiddle.electric-de :as e :refer [$]]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]
            #?@(:cljs [[app.client.webgpu.sync :as wsync :refer [run-webgpu]]])))

(hyperfiddle.rcf/enable!)

(defn await-promise
  "Returns a task completing with the result of given promise"
  [p]
  (let [v (m/dfv)]
    (.then p
      #(v (fn [] %))
      #(v (fn [] (throw %))))
    (m/absolve v)))


#?(:cljs (defonce !canvas (atom nil)))


(e/defn setup-webgpu []
  (let [canvas (e/watch !canvas)]
    (when canvas
      (let [context (.getContext canvas "webgpu")
            gpu      js/navigator.gpu
            adapter  ($ e/Task (await-promise (.requestAdapter gpu)))
            device   ($ e/Task (await-promise (.requestDevice adapter)))
            cformat  (.getPreferredCanvasFormat gpu)
            config   {:format cformat
                      :device device}]
        (.configure context (clj->js config))
        (run-webgpu context device canvas cformat)))))


(e/defn canvas-view []
  (dom/canvas
    (dom/props {:id "top-canvas"
                :width 2048
                :height 2048})
    (reset! !canvas dom/node)))

(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
         ($ canvas-view)
         ($ setup-webgpu))))
