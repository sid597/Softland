(ns app.client.webgpu.compute)


(defn compute-pipeline-descriptor [shader-module]
  {:label "square compute descriptor"
   :layout "GPUPipelineLayout"
   :compute (clj->js {:module shader-module
                      :entryPoint "modifySquare"})})



