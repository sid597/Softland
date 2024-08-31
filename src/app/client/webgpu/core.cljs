(ns app.client.webgpu.core
  (:require [app.client.webgpu.bind :refer [bind-group-descriptor
                                            storage-bind-group-descriptor]]
            [app.client.webgpu.buffer :refer [base-square-buffer-descriptor
                                              uniform-buffer-descriptor
                                              storage-buffer-descriptor]]
            [app.client.webgpu.pipeline :refer [base-square-pipeline-descriptor]]
            [app.client.webgpu.data :refer [uniform-array vertices grid-size]]
            [app.client.webgpu.shader :refer [shader-descriptor]]))




(defn run-webgpu [context device canvas cformat]
  (let [vertex-buffer      (.createBuffer device base-square-buffer-descriptor)
        uniform-buffer     (.createBuffer device uniform-buffer-descriptor)
        cell-shader-module (.createShaderModule device shader-descriptor)
        pipeline-data      (base-square-pipeline-descriptor cell-shader-module cformat)
        cell-pipeline      (.createRenderPipeline device pipeline-data)
        bind-group         (.createBindGroup device
                             (bind-group-descriptor cell-pipeline uniform-buffer))
        encoder            (.createCommandEncoder device)
        pass               (.beginRenderPass
                             encoder
                             (clj->js {:colorAttachments
                                       (clj->js [{:view (.createView (.getCurrentTexture context))
                                                  :clearValue (clj->js {:r 0.2 :g 0.2 :b 0.2 :a 1})
                                                  :loadOp "clear"
                                                  :storeOp "store"}])}))]
    ;(reset! !vertex-buffer vertex-buffer)
    (.writeBuffer (.-queue device) uniform-buffer 0 uniform-array)
    (.writeBuffer (.-queue device) vertex-buffer 0 vertices)
    (.setPipeline pass cell-pipeline)
    (.setVertexBuffer pass 0 vertex-buffer)
    (.setBindGroup pass 0 bind-group)
    (.draw pass (/ (.-length vertices) 2) (* grid-size grid-size))
    (.end pass)
    (.submit (.-queue device) [(.finish encoder)])))


(def vertices-shader
  (clj->js {:label "vertices shader descriptor"
            :code "
            // Constants for screen dimensions
            @group(0) @binding(0) var<storage, read> rectangles: array<f32>;            // Flattened input array
            @group(0) @binding(1) var<storage, write> vertices: array<vec4<f32>>; // Output vertices
            @group(0) @binding(2) var<storage, write> debugOutput: array<f32>;


            @compute @workspace_size(64)
            fn main(@builtin(global_invocation_id) global_id: vec3<u32>) {

              let index = global_id.x; // Check if this thread should process a rectangle
              if (index >= arrayLength(&rectangles) / 4) {
                      return;
              }

              // Each rectangle is represented by 4 consecutive float values
              let base_index = index * 4;
              let x = rectangles[base_index];
              let y = rectangles[base_index + 1];
              let height = rectangles[base_index + 2];
              let width = rectangles[base_index + 3];

              // Calculate the four corners of the rectangle in clip space
              let left = (x / 1920.0) * 2.0 - 1.0;
              let right = ((x + width) / 1920.0) * 2.0 - 1.0;
              let top = 1.0 - (y / 1080.0) * 2.0 ;
              let bottom = 1.0 -((y + height) / 1080.0) * 2.0 ;


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

              debugOutput[0] = vertices;

            }

            @vertex
            fn renderVertices(@location(0) pos: vec2<f32>) -> @builtin(position) vec4<f32> {
             return vec4f(pos, 0.0, 1.0);
            }

            @fragment
            fn renderVerticesFragment(@location(0) frag_color: vec4<f32>) -> @location(0) vec4<f32> {
             return vec4f(0.1, 1, 1, 1);
            }
            "}))





(defn render-new-vertices [context new-vertices device encoder fformat]
  (let [varray               (js/Float32Array. new-vertices)
        vertices-buffer      (.createBuffer
                               device
                               (clj->js {:label            "vertices buffer"
                                         :size             (.-byteLength varray)
                                         :usage            (bit-or js/GPUBufferUsage.VERTEX
                                                             js/GPUBufferUsage.COPY_DST)
                                         :mappedAtCreation? false}))
        vertex-buffer-layout (clj->js {:arrayStride 16 ;; each vertex is of length 4 xyhw type f32 (4 bytes)
                                       :attributes (clj->js [{:format "float32x4"
                                                              :offset 0
                                                              ;; numeric location associated with this attribute which will corres
                                                              ;; -pond with a "@location" attribute
                                                              :shaderLocation 0}])})
        shader-module        (.createShaderModule device vertices-shader)
        compute-pipeline     (.createComputePipeline
                               device
                               (clj->js {:layout "auto"
                                         :compute {:module shader-module
                                                   :entryPoint "main"}}))
        render-pipeline      (.createRenderPipeline
                               device
                               (clj->js {:label "vertices render pipeline"
                                         :layout "auto"
                                         :vertex (clj->js
                                                   {:module shader-module
                                                    :entryPoint "renderVertices"
                                                    :layout "auto"
                                                    :buffers [vertex-buffer-layout]})
                                         :fragment (clj->js
                                                     {:module shader-module
                                                      :entryPoint "renderVerticesFragment "
                                                      :targets (clj->js [{:format fformat}])})}))

        num-rectangles       (/ (count new-vertices) 4)
        input-buffer         (.createBuffer
                               device
                               (clj->js {:size (.-byteLength varray)
                                         :usage (bit-or js/GPUBufferUsage.STORAGE
                                                  js/GPUBufferUsage.COPY_DST)
                                         :mappedAtCreation false}))
        output-buffer        (.createBuffer
                               device
                               (clj->js {:size (* num-rectangles 12 4)
                                         :usage (bit-or js/GPUBufferUsage.STORAGE
                                                  js/GPUBufferUsage.VERTEX
                                                  js/GPUBufferUsage.COPY_SRC)
                                         :mappedAtCreation false}))
        debugBuffer          (.createBuffer
                               device
                               (clj->js {:size (* num-rectangles 12 4)
                                         :usage (bit-or js/GPUBufferUsage.STORAGE
                                                  js/GPUBufferUsage.COPY_SRC)}))


        bind-group           (.createBindGroup
                               device
                               (clj->js {:layout (.getBindGroupLayout compute-pipeline 0)
                                         :entries (clj->js [{:binding 0
                                                             :resource {:buffer input-buffer}}
                                                            {:binding 1
                                                             :resource {:buffer output-buffer}}
                                                            {:binding 2
                                                             :resource {:buffer debugBuffer}}])}))]


    (.writeBuffer (.-queue device) input-buffer 0 varray)

    (let [compute-pass (.beginComputePass encoder)]
      (.setPipeline compute-pass compute-pipeline)
      (.setBindGroup compute-pass 0 bind-group)
      (.dispatchWorkgroups compute-pass (js/Math.ceil (/ num-rectangles 64)))
      (.end compute-pass))


    (let [render-pass (.beginRenderPass
                        encoder
                        (clj->js {:colorAttachments
                                  (clj->js [{:view (.createView (.getCurrentTexture context))
                                             :clearValue {:r 0.2 :g 0.9 :b 0.2 :a 1.0}
                                             :loadOp "clear"
                                             :storeOp "store"}])}))]
      (.setPipeline render-pass render-pipeline)
      (.setVertexBuffer render-pass 0 output-buffer)
      (.draw render-pass (* num-rectangles 6)) ; 6 vertices per rectangle
      (.end render-pass)
      (.submit (.-queue device) [(.finish encoder)]))))


