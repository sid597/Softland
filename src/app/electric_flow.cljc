(ns app.electric-flow
  (:require [hyperfiddle.electric-de :as e :refer [$]]
            [missionary.core :as m]
            [hyperfiddle.electric-dom3 :as dom]
            #?@(:cljs [[app.client.webgpu.core :as wcore :refer [run-webgpu render-new-vertices]]
                       [app.client.webgpu.data :refer [!rects]]])))

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
#?(:cljs (defonce !width (atom nil)))
#?(:cljs (defonce !height (atom nil)))
#?(:cljs (defonce !squares (atom nil)))
#?(:cljs (defonce !adapter (atom nil)))
#?(:cljs (defonce !device (atom nil)))
#?(:cljs (defonce !context (atom nil)))
#?(:cljs (defonce !command-encoder (atom nil)))
#?(:cljs (defonce !cd (atom nil)))




#?(:cljs (def vertices-compute-shader
           (clj->js {:label "vertices compute shader descriptor"
                     :code"
                     // Constants for screen dimensions
                     @group(0) @binding(0) var<storage, read> rectangles: array<f32>;     // Flattened input array
                     @group(0) @binding(1) var<storage, read_write> vertices: array<f32>; // Output vertices

                     @compute @workgroup_size(64)
                     fn main(@builtin(global_invocation_id) global_id: vec3<u32>){
                      let index = global_id.x;
                      if (index >= arrayLength(&rectangles) / 4) {
                                            return;}
                      let base_index = index * 4 ;
                      let x = rectangles[base_index];
                      let y = rectangles[base_index + 1];
                      let height = rectangles[base_index + 2];
                      let width = rectangles[base_index + 3];

                      // Calculate the four corners of the rectangle in clip space
                      let left = (x / 1920) * 2 - 1;
                      let right = ((x + width) / 1920) * 2 - 1;
                      let top = 1 - (y / 1080) * 2 ;
                      let bottom = 1 - ((y + height) / 1080) * 2 ;


                      // Create 6 vertices for two triangles (12 float values)
                      let vertex_index = index * 12;  // 6 vertices * 2 components each


                      // Triangle 1
                      vertices[vertex_index + 0] = left;
                      vertices[vertex_index + 1] = top;
                      vertices[vertex_index + 2] = right;
                      vertices[vertex_index + 3] = top;
                      vertices[vertex_index + 4] = left;
                      vertices[vertex_index + 5] = bottom;

                      // Triangle 2
                      vertices[vertex_index + 6] = right;
                      vertices[vertex_index + 7] = top;
                      vertices[vertex_index + 8] = right;
                      vertices[vertex_index + 9] = bottom;
                      vertices[vertex_index + 10] = left;
                      vertices[vertex_index + 11] = bottom;
                     }
                     "})))



#?(:cljs
   (defn run-compute-pipeline [data device encoder]
     (let [varray                (js/Float32Array. (clj->js data))
           num-rectangles        (/ (count data) 4)
           output-size           (* num-rectangles 12 4)
           compute-shader-module (.createShaderModule device vertices-compute-shader)
           input-buffer          (.createBuffer
                                   device
                                   (clj->js {:label "input buffer"
                                             :size (.-byteLength varray)
                                             :usage (bit-or js/GPUBufferUsage.STORAGE
                                                      js/GPUBufferUsage.COPY_DST)}))
           output-buffer         (.createBuffer
                                   device
                                   (clj->js {:label "output buffer"
                                             :size output-size
                                             :usage (bit-or js/GPUBufferUsage.STORAGE
                                                      js/GPUBufferUsage.VERTEX
                                                      js/GPUBufferUsage.COPY_DST
                                                      js/GPUBufferUsage.COPY_SRC)}))
           output-read-buffer    (.createBuffer
                                   device
                                   (clj->js {:label "output read buffer"
                                             :size output-size
                                             :usage (bit-or js/GPUBufferUsage.MAP_READ
                                                      js/GPUBufferUsage.COPY_DST)}))
           binding-group-layout (.createBindGroupLayout
                                 device
                                 (clj->js {:label "compute bind group layout"
                                           :entries (clj->js [{:binding 0
                                                               :visibility js/GPUShaderStage.COMPUTE
                                                               :buffer {:type "read-only-storage"}}
                                                              {:binding 1
                                                               :visibility js/GPUShaderStage.COMPUTE
                                                               :buffer {:type "storage"}}])}))
           bind-group            (.createBindGroup
                                   device
                                   (clj->js {:layout binding-group-layout
                                             :entries (clj->js [{:binding 0
                                                                 :resource {:buffer input-buffer}}
                                                                {:binding 1
                                                                 :resource {:buffer output-buffer}}])}))
           pipeline-layout       (.createPipelineLayout
                                   device
                                   (clj->js {:label "compute pipeline layout"
                                             :bindGroupLayouts [binding-group-layout]}))
           compute-pipeline      (.createComputePipeline
                                   device
                                   (clj->js {:layout pipeline-layout
                                             :label "compute pipeline"
                                             :compute (clj->js {:module compute-shader-module
                                                                :entryPoint "main"})}))
           compute-pass          (.beginComputePass encoder)]
       (-> (.getCompilationInfo compute-shader-module)
         (.then (fn [info] (js/console.log "compute shader info:" info))))
       (.writeBuffer        (.-queue device) input-buffer 0 varray)
       (.setPipeline        compute-pass compute-pipeline)
       (.setBindGroup       compute-pass 0 bind-group)
       (.dispatchWorkgroups compute-pass   (/ (count data)  64))
       (.end                compute-pass)
      [output-buffer output-read-buffer])))

(e/defn rnd [rc]
  (let [res (atom [])]
    (doseq [i (range rc)]
      (let [height (* (rand) 10)
            width (* (rand) 10)
            y (* (rand) 1920)
            x (* (rand) 1080)]
        ;(println "xx" x y @res)
        (swap! res concat [x y height width])))
    (println "DONE" (e/watch res) rc)
    res))


(e/defn setup-webgpu []
  (e/client
    (let [canvas (e/watch !canvas)]
      (when canvas
        (let [context (.getContext canvas "webgpu")
              gpu      js/navigator.gpu
              adapter  ($ e/Task (await-promise (.requestAdapter gpu (clj->js {:requiredFeatures ["validation"]}))))
              device   ($ e/Task (await-promise (.requestDevice adapter)))
              cformat  (.getPreferredCanvasFormat gpu)
              config   (clj->js {:format cformat
                                 :device device})
              encoder (.createCommandEncoder device)
              nos 201
              !rnd ($ rnd nos)
              rnd  (into [] (e/watch !rnd))]
          ;(println "rnd" (e/watch rnd))
          (.configure context config)
          (when (some? rnd)
            (println "RND" rnd)
            (let [[ob orb] (run-compute-pipeline
                              rnd
                              #_[100.0 100.0 100.0 200.0
                                 600.0 300.0 100.0 200.0]
                              device
                              encoder)
                  !oaray (atom nil)
                  oraay (e/watch !oaray)]
              (when (some? ob)
                (case
                  ($ e/Task (m/sleep 1))  (.copyBufferToBuffer encoder ob 0 orb 0 (* 12 4 nos)))
                (case
                  ($ e/Task (m/sleep 25)) (let [command-buffer (.finish encoder)]
                                            (.submit (.-queue device) (clj->js [command-buffer]))))
                (case
                  ($ e/Task (m/sleep 36)) (let [maped ($ e/Task (await-promise (.mapAsync orb 1)))
                                                output-buffer-map ($ e/Task (await-promise (.onSubmittedWorkDone (.-queue device))))]
                                            (when (and (nil? maped) (nil? output-buffer-map))
                                              (println "--" maped output-buffer-map)
                                              (let [array-buffer (.getMappedRange orb)
                                                    rray (js/Float32Array. array-buffer)
                                                    js-array (into-array rray)]
                                                (.unmap orb)
                                                (reset! !oaray js-array)
                                                (js/console.log "array buffer" oraay)
                                                (case ($ e/Task (m/sleep 40)) (render-new-vertices
                                                                                context
                                                                                oraay
                                                                                device
                                                                                cformat
                                                                                nos
                                                                                ob)))))))))





          #_(run-compute-pipeline
              [3 4 1920 1080]
              device
              encoder))))))
        ;(reset! !adapter adapter)
        ;(reset! !device device)
        ;(reset! !context context)
        ;(reset! !command-encoder encoder)
        ;(.configure context (clj->js config))
        ;(render-new-vertices context [20 20 390 390] device encoder cformat)))))


(e/defn canvas-view []
  (e/client
    (dom/canvas
      (dom/props {:id "top-canvas"
                  :width 1920 ;(e/watch !width)
                  :height 1080}) ;(e/watch !height)})
      (reset! !canvas dom/node)
      #_(let [md? ($ dom/MouseDown?)]
          (let [x (rand-int (e/watch !width))
                y (rand-int (e/watch !width))
                h (rand-int 100)
                w (rand-int 100)
                rects (e/watch !rects)]
            (js/console.log "rects ->" (js/Float32Array. rects))
           (when md?
             (println "MOUSEDOWN"  x y h w)
             (swap! !rects concat [x y h w])))))))


(e/defn main [ring-request]
  (e/client
    (binding [dom/node js/document.body]
      (let [dpr (.-devicePixelRatio js/window)]
         (reset! !width (.-clientWidth dom/node))
         (reset! !height (.-clientHeight dom/node))
         ($ canvas-view)
         ($ setup-webgpu)))))
