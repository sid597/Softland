(ns app.client.webgpu.core
  (:require
    [global-flow :refer [!visible-rects !old-visible-rects !font-bitmap !atlas-data]]
    [app.client.webgpu.shader :refer [text-vertex-shader text-fragment-shader add-new-rects-shader-descriptor]]
    [hyperfiddle.incseq :as i]))


(defn string->ints [s]
  (map #(.charCodeAt % 0) s))



(defn shape-text [texts fsize msdf-atlas]
  (let [atlas          (:atlas msdf-atlas)
        atlas-width    (:width atlas)
        atlas-height   (:height atlas)
        metrics        (:metrics msdf-atlas)
        line-height    (:lineHeight metrics)
        glyphs         (reduce (fn [acc glyph]
                                 (assoc acc (:unicode glyph)
                                            glyph))
                         {}
                         (:glyphs msdf-atlas))
        font-size      (* (/ 1 (:size atlas)) fsize)
        res            (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y]} txt
            !x (atom x)
            !y (atom y)]
        ;(println "shape text" x y txt)
        (doseq [ch (seq text)]
          (let [codepoint (.charCodeAt ch 0)]
            (cond
              (= ch \newline)   (reset! !y (- @!y (* font-size line-height)))
              (= ch \space)     (reset! !x (+ @!x (* font-size 0.5)))
              :else             (let [glyph (get glyphs codepoint)]
                                  (when glyph
                                    (let [advance        (* font-size (:advance glyph))
                                          plane-bounds   (:planeBounds glyph)
                                          atlas-bounds   (:atlasBounds glyph)
                                          fw             (/ (* font-size (- (get plane-bounds :right) (get plane-bounds :left))) 2)
                                          fh             (/ (* font-size (- (get plane-bounds :top) (get plane-bounds :bottom))) 2)
                                          ;; Scale plane bounds by font size
                                          pl            @!x
                                          pb            (- @!y fh) 
                                          pr            (+ @!x fw) 
                                          pt            @!y 
                                          positions     [[pl pb] [pr pb] [pr pt] [pl pt]]
                                          ;; Calculate texture coordinates
                                          al             (/ (get atlas-bounds :left) atlas-width)
                                          ab             (/ (get atlas-bounds :bottom) atlas-height)
                                          ar             (/ (get atlas-bounds :right) atlas-width)
                                          at             (/ (get atlas-bounds :top) atlas-height)
                                          uvs [[al (- 1.0 ab)] [ar (- 1.0 ab)] [ar (- 1.0 at)] [al (- 1.0 at)]]]
                                      (do
                                       (reset! !x (+ @!x (/ advance 2)))
                                       (swap! res conj {:codepoint codepoint
                                                        :positions positions
                                                        :uvs uvs}))))))))))
    @res))



(defn prepare-vertex-data [shaped-text]
  (let [vertices (atom [])
        indices  (atom [])
        index    (atom 0)]
    ;(doseq [shaped-text shaped-texts])
    (doseq [glyph shaped-text]
      ;(println 'shaped-text-1 glyph)
      (let [[[x0 y0] [x1 y1] [x2 y2] [x3 y3]] (:positions glyph)
            [[u0 v0] [u1 v1] [u2 v2] [u3 v3]] (:uvs glyph)
            idx @index]
        ;(println 'positions (:positions glyph))
        ;; Add vertices (position and UVs)
        (swap! vertices conj
               x0 y0 u0 v0
               x1 y1 u1 v1
               x2 y2 u2 v2
               x3 y3 u3 v3)
        ;; Add indices for two triangles (assuming CCW order)
        (swap! indices conj
               idx
               (+ 1 idx)
               (+ 2 idx)
               idx
               (+ 2 idx)
               (+ 3 idx))
        ;; Increment index
        (swap! index + 4)))
    {:vertex-data (js/Float32Array. (clj->js @vertices))
     :index-data  (js/Uint16Array. (clj->js @indices))}))


(defn render-text [device format context px-range font-size atlas font-bitmap texts]
  ;(println "render text ")
  (let [sizes                    (js/Float32Array. (clj->js [px-range (:size (:atlas atlas)) font-size]))
        shaped-texts              (shape-text texts font-size atlas)
       ; _ (println "shaped texts" shaped-texts)
        {:keys [vertex-data
                index-data]}     (prepare-vertex-data shaped-texts)
        ;_ (println 'vertex-data vertex-data)
        ;_ (println 'index-data index-data)
        num-indices              (.-length index-data)
        shader-module-vertex     (.createShaderModule 
                                   device 
                                   text-vertex-shader)
        shader-module-fragment   (.createShaderModule
                                   device 
                                   text-fragment-shader)
        vertex-buffer            (.createBuffer device
                                  (clj->js {:size (.-byteLength vertex-data)
                                            :usage (bit-or js/GPUBufferUsage.VERTEX
                                                           js/GPUBufferUsage.COPY_DST)}))
        index-buffer             (.createBuffer device
                                   (clj->js {:size (.-byteLength index-data)
                                             :usage (bit-or js/GPUBufferUsage.INDEX
                                                            js/GPUBufferUsage.COPY_DST)}))
        size-buffer              (.createBuffer 
                                   device 
                                   (clj->js {:size (.-byteLength sizes)
                                             :usage (bit-or js/GPUBufferUsage.UNIFORM 
                                                             js/GPUBufferUsage.COPY_DST)}))
        bitmap-height            (.-height font-bitmap)
        bitmap-width             (.-width font-bitmap)
        texture                  (.createTexture device
                                  (clj->js {:size {:width bitmap-width
                                                   :height bitmap-height
                                                   :depthOrArrayLayers 1}
                                            :format "rgba8unorm"
                                            :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                                           js/GPUTextureUsage.TEXTURE_BINDING
                                                           js/GPUTextureUsage.COPY_DST)}))
        sampler                  (.createSampler device (clj->js {:minFilter "linear"
                                                                  :magFilter "linear"
                                                                  :mipmapFilter "linear"}))
        texture-view             (.createView texture)
        ;_                        (println "texture view")
        bind-group-layout        (.createBindGroupLayout
                                   device
                                   (clj->js {:label "bind group layout"
                                             :entries [{:binding 0
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :sampler {:type "filtering"}}
                                                       {:binding 1
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :texture {:sampleType "float"}}
                                                       {:binding 2 
                                                        :visibility js/GPUShaderStage.FRAGMENT
                                                        :buffer {:type "uniform"}}]}))
                                                         
        bind-group               (.createBindGroup device
                                  (clj->js {:layout bind-group-layout
                                            :entries (clj->js
                                                       [{:binding 0
                                                         :resource sampler}
                                                        {:binding 1
                                                         :resource texture-view}
                                                        {:binding 2 
                                                         :resource {:buffer size-buffer}}])}))
        ;_ (println "bind group done")
        pipeline-layout          (.createPipelineLayout
                                   device 
                                   (clj->js {:label "pipeline layout"
                                             :bindGroupLayouts [bind-group-layout]}))
        pipeline                 (.createRenderPipeline device
                                  (clj->js {:layout pipeline-layout
                                            :vertex {:module shader-module-vertex
                                                     :entryPoint "main"
                                                     :buffers (clj->js [{:arrayStride (* 4 4)
                                                                         :attributes (clj->js
                                                                                      [{:shaderLocation 0 :offset 0 :format "float32x2"}
                                                                                       {:shaderLocation 1 :offset 8 :format "float32x2"}])}])}
                                            :fragment (clj->js 
                                                        {:module shader-module-fragment
                                                         :entryPoint "main"
                                                         :targets (clj->js 
                                                                    [{:format format
                                                                      :blend (clj->js {:color (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})
                                                                                       :alpha (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})})}])})}))
                                                                                             
        encoder                  (.createCommandEncoder device)

        bbg {:r 0.0 :g 0.0 :b 0.0 :a 1.0}
        wbg {:r 1.0 :g 1.0 :b 1.0 :a 1.0}
        render-pass              (.beginRenderPass
                                   encoder
                                   (clj->js {:colorAttachments 
                                             (clj->js [{:view (.createView (.getCurrentTexture context))
                                                        :loadOp "load"
                                                        :storeOp "store"}])
                                             :label "render parss"}))]
        ;_ (println "num indexes" num-indices)]
    ;(println "COMPILING")
    #_(-> (.getCompilationInfo shader-module-vertex)
          (.then (fn [info] (js/console.log "compute shader info:" info))))
    #_(-> (.getCompilationInfo shader-module-fragment)
          (.then (fn [info] (js/console.log "compute shader info:" info))))
    (.copyExternalImageToTexture
      (.-queue device)
      (clj->js {:source font-bitmap})
      (clj->js {:texture texture
                :origin {:x 0 :y 0 :z 0}})
      (clj->js {:width bitmap-width
                :height bitmap-height
                :depthOrArrayLayers 1}))
    ;(println "COPIED")
    (.writeBuffer (.-queue device) vertex-buffer 0 vertex-data)
    (.writeBuffer (.-queue device) index-buffer 0 index-data)
    (.writeBuffer (.-queue device) size-buffer 0 sizes)
    (.setPipeline render-pass pipeline)
    (.setBindGroup render-pass 0 bind-group)
    (.setVertexBuffer render-pass 0 vertex-buffer)
    (.setIndexBuffer render-pass index-buffer "uint16")
    ;(println "pipeline and buffers set")
    (.drawIndexed render-pass num-indices)
    (.end render-pass)
    (.submit (.-queue device) [(.finish encoder)])))
  

 

(defn render-rect [from data device fformat context config ids]
  ;(println 'uplaod-vertices data ":::::::" ids)
  ;(println 'config config)
          
  (let [varray                (js/Float32Array. (clj->js data))
        ids-array             (js/Uint32Array.  (clj->js ids))
        ;_ (println "IDS ARRAY" ids-array)
        ids-array-length      (.-byteLength ids-array)
        settings-array        (js/Float32Array. (clj->js config))
        num-rectangles        (count data)
        output-size           (* num-rectangles 12)
        shader-module         (.createShaderModule device add-new-rects-shader-descriptor)
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
        settings-uniform-buffer (.createBuffer
                                  device
                                  (clj->js {:label "settings buffer"
                                            :size (.-byteLength settings-array)
                                            :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                     js/GPUBufferUsage.COPY_DST)}))
        id-buffer             (.createBuffer
                                device
                                (clj->js {:label "id buffer"
                                          :size ids-array-length
                                          :usage (bit-or js/GPUBufferUsage.STORAGE
                                                   js/GPUBufferUsage.COPY_DST)}))
        rendered-ids-buffer   (.createBuffer
                                device
                                (clj->js {:label "rendered ids buffer"
                                          :size ids-array-length
                                          :usage (bit-or js/GPUBufferUsage.STORAGE
                                                   js/GPUBufferUsage.COPY_SRC)}))
        binding-group-layout  (.createBindGroupLayout
                               device
                               (clj->js {:label "compute bind group layout"
                                         :entries (clj->js [{:binding 0
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "read-only-storage"}}
                                                            {:binding 1
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "storage"}}
                                                            {:binding 2
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "uniform"}}
                                                            {:binding 3
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "read-only-storage"}}
                                                            {:binding 4
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "storage"}}])}))
        bind-group            (.createBindGroup
                                device
                                (clj->js {:layout binding-group-layout
                                          :entries (clj->js [{:binding 0
                                                              :resource {:buffer input-buffer}}
                                                             {:binding 1
                                                              :resource {:buffer output-buffer}}
                                                             {:binding 2
                                                              :resource {:buffer settings-uniform-buffer}}
                                                             {:binding 3
                                                              :resource {:buffer id-buffer}}
                                                             {:binding 4
                                                              :resource {:buffer rendered-ids-buffer}}])}))
        pipeline-layout       (.createPipelineLayout
                                device
                                (clj->js {:label "compute pipeline layout"
                                          :bindGroupLayouts [binding-group-layout]}))
        compute-pipeline      (.createComputePipeline
                                device
                                (clj->js {:layout pipeline-layout
                                          :label "compute pipeline"
                                          :compute (clj->js {:module shader-module
                                                             :entryPoint "main"})}))
        render-binding-group-layout  (.createBindGroupLayout
                                       device
                                       (clj->js {:label "render bind group layout"
                                                 :entries (clj->js [{:binding 1
                                                                      :visibility js/GPUShaderStage.VERTEX
                                                                      :buffer {:type "read-only-storage"}}
                                                                    {:binding 2
                                                                     :visibility js/GPUShaderStage.VERTEX
                                                                     :buffer {:type "uniform"}}])}))

        render-bind-group            (.createBindGroup
                                       device
                                       (clj->js {:layout render-binding-group-layout
                                                 :entries (clj->js [{:binding 1
                                                                     :resource {:buffer output-buffer}}
                                                                    {:binding 2
                                                                     :resource {:buffer settings-uniform-buffer}}])}))

        render-pipeline-layout (.createPipelineLayout
                                 device
                                 (clj->js {:label "compute pipeline layout"
                                           :bindGroupLayouts [render-binding-group-layout]}))

        render-pipeline       (.createRenderPipeline
                                device
                                (clj->js {:label "vertices render pipeline"
                                          :layout render-pipeline-layout
                                          :vertex (clj->js
                                                    {:module shader-module
                                                     :entryPoint "renderVertices"
                                                     :layout "auto"
                                                     :buffers (clj->js [])})
                                          :fragment (clj->js
                                                      {:module shader-module
                                                       :entryPoint "renderVerticesFragment"
                                                       :targets (clj->js 
                                                                    [{:format fformat
                                                                      :blend (clj->js {:color (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})
                                                                                       :alpha (clj->js {:srcFactor "src-alpha"
                                                                                                        :dstFactor "one-minus-src-alpha"})})}])})}))]

    #_(-> (.getCompilationInfo shader-module)
        (.then (fn [info] (js/console.log "compute shader info:" info))))
    (.writeBuffer (.-queue device) input-buffer 0 varray)
    (.writeBuffer (.-queue device) settings-uniform-buffer 0 settings-array)
    (.writeBuffer (.-queue device) id-buffer 0 ids-array)


    (let [encoder (.createCommandEncoder device)
          compute-pass  (.beginComputePass encoder)]
       ;; Compute pipeline
       (.setPipeline        compute-pass compute-pipeline)
       (.setBindGroup       compute-pass 0 bind-group)
       (.dispatchWorkgroups compute-pass (max 1 (/ num-rectangles  64)))
       (.end                compute-pass)
       ;(.submit (.-queue device) [(.finish encoder)])
       (let [staging-buffer (.createBuffer
                              device
                              (clj->js {:label "staging buffer"
                                        :size  ids-array-length
                                        :usage (bit-or js/GPUBufferUsage.MAP_READ
                                                 js/GPUBufferUsage.COPY_DST)}))]
         (.copyBufferToBuffer encoder rendered-ids-buffer 0 staging-buffer 0 ids-array-length)

         (.submit (.-queue device) [(.finish encoder)])

         ; Read the staging buffer
         (-> (.mapAsync staging-buffer js/GPUMapMode.READ)
            (.then (fn []
                     (let [mapped-range (.getMappedRange staging-buffer)
                           num-rendered (js/Uint32Array. mapped-range)
                           rendered-ids (sort (into-array num-rendered))
                           new-rects    (js->clj (into-array (filter (complement zero?) rendered-ids)))]
                       ;(println 'rendered-ids num-rendered new-rects)
                       (if (= "initial" from)
                         (swap! !visible-rects (constantly new-rects))
                         (do 
                           (when-not (= new-rects @!visible-rects)
                             (swap! !old-visible-rects (constantly @!visible-rects)))
                           (swap! !visible-rects (constantly new-rects))))
                       (.unmap staging-buffer)))))))


    ;; Render pipeline
    (let [encoder (.createCommandEncoder device)
          render-pass  (.beginRenderPass
                         encoder
                         (clj->js {:colorAttachments (clj->js [{:view (.createView (.getCurrentTexture context))
                                                                :clearValue (clj->js {:r 0.0 :g 0.0 :b 0.0 :a 1})
                                                                :loadOp "clear"
                                                                :storeOp "store"}])
                                   :label "render parss"}))]


      (.setPipeline  render-pass render-pipeline)
      (.setBindGroup render-pass 0 render-bind-group)
      (.draw         render-pass (* num-rectangles 2))
      (.end          render-pass)
      (.submit (.-queue device) [(.finish encoder)]))))



;; --- Data Structures ---

(defn create-graph [nodes edges]
  (let [num-nodes (count nodes)
        num-edges (count edges)
        ;; Each node needs 6 floats: position(2) + velocity(2) + force(2)
        nodes-typed-array (js/Float32Array.
                            (flatten
                              (map (fn [n]
                                     [(:x n)    (:y n)     ; position (vec2<f32>)
                                      0.0       0.0        ; velocity (vec2<f32>)
                                      0.0       0.0])      ; force (vec2<f32>)
                                nodes)))
        ;; Each edge needs 2 uint32s: node1_index and node2_index
        edges-typed-array (js/Uint32Array.
                            (flatten
                              (map (fn [e]
                                     [(:node1-index e)
                                      (:node2-index e)])
                                edges)))]
    (println "Node struct size:" (* 6 4) "bytes") ; 6 floats * 4 bytes
    (println "Total nodes buffer size:" (* num-nodes 6 4) "bytes")
    (println "Total edges buffer size:" (* num-edges 2 4) "bytes")
    {:num-nodes num-nodes
     :num-edges num-edges
     :nodes nodes-typed-array
     :edges edges-typed-array}))
;; --- Shader Modules ---
;;
;;



(def repulsion-shader-code
  "struct Node {
    position: vec2<f32>,
    velocity: vec2<f32>,
    force: vec2<f32>
   };

  @group(0) @binding(0) var<storage, read_write> nodes: array<Node>;
  @group(0) @binding(1) var<uniform> num_nodes: u32;

  const repulsion_strength: f32 = 100.0;

  @compute @workgroup_size(64)
  fn main(@builtin(global_invocation_id) global_id: vec3<u32>) {
    let node_index = global_id.x;
    if (node_index >= num_nodes) {
      return;
    }

    var force = vec2<f32>(0.0, 0.0);
    for (var i: u32 = 0; i < num_nodes; i++) {
      if (i == node_index) {
        continue;
      }

      let other_node_pos = nodes[i].position;
      let delta = nodes[node_index].position - other_node_pos;
      let distance = length(delta);

      if (distance > 0.0) {
        let repulsion = repulsion_strength / (distance * distance);
        force += repulsion * delta / distance;
      }
  }

  nodes[node_index].force += force;
  }
")



(def attraction-shader-code
  "
  struct Node {
    position: vec2<f32>,
    velocity: vec2<f32>,
    force: vec2<f32>
  };

  struct Edge {
    node1_index: u32,
    node2_index: u32
  };

  @group(0) @binding(0) var<storage, read_write> nodes: array<Node>;
  @group(0) @binding(1) var<storage, read> edges: array<Edge>;
  @group(0) @binding(2) var<uniform> num_edges: u32;

  const ideal_edge_length: f32 = 50.0;

  @compute @workgroup_size(64)
  fn main(@builtin(global_invocation_id) global_id: vec3<u32>) {
    let edge_index = global_id.x;
    if (edge_index >= num_edges) {
     return;}


    let edge = edges[edge_index];
    let node1_pos = nodes[edge.node1_index].position;
    let node2_pos = nodes[edge.node2_index].position;

    let delta = node2_pos - node1_pos;
    let distance = length(delta);

    if (distance > 0.0) {
      let attraction = (distance - ideal_edge_length) * delta / distance;
      nodes[edge.node1_index].force += attraction;
      nodes[edge.node2_index].force -= attraction;}}")


(def integration-shader-code
  "struct Node {
    position: vec2<f32>,
    velocity: vec2<f32>,
    force: vec2<f32>
  };

  @group(0) @binding(0) var<storage, read_write> nodes: array<Node>;
  @group(0) @binding(1) var<uniform> num_nodes: u32;

  const cooling_factor: f32 = 0.99;
  const dt: f32 = 0.1;

  @compute @workgroup_size(64)
  fn main(@builtin(global_invocation_id) global_id: vec3<u32>) {
     let node_index = global_id.x;
     if (node_index >= num_nodes) {
       return;}


     nodes[node_index].velocity = (nodes[node_index].velocity + nodes[node_index].force * dt) * cooling_factor;
     nodes[node_index].position += nodes[node_index].velocity * dt;
     nodes[node_index].force = vec2<f32>(0.0, 0.0);}")


(defn create-graph-buffers [device graph]
  (let [
        ;; Each node has position (2 floats), velocity (2 floats), and force (2 floats)
        node-struct-size (* 6 4)  ; 6 floats * 4 bytes per float
        node-buffer-size (* (:num-nodes graph) node-struct-size)

        ;; Each edge has two u32 indices
        edge-struct-size (* 2 4)  ; 2 u32s * 4 bytes per u32
        edge-buffer-size (* (:num-edges graph) edge-struct-size)

        node-count-buffer-size 4  ; Size of one u32 value
        edge-count-buffer-size 4] ; Size of one u32 value
    (println "bytes per element " (.-BYTES_PER_ELEMENT (:nodes graph)))
    (println "bytes per element " (.-BYTES_PER_ELEMENT (:edges graph)))
    (println "num nodes " (:num-nodes graph))
    (println "num edges " (:num-edges graph))

    {:node-buffer (-> device (.createBuffer (clj->js {:label "node buffer"
                                                      :size node-buffer-size
                                                      :usage (bit-or js/GPUBufferUsage.STORAGE
                                                               js/GPUBufferUsage.COPY_DST
                                                               js/GPUBufferUsage.COPY_SRC)})))
     :edge-buffer (-> device (.createBuffer (clj->js {:label "edge buffer"
                                                      :size edge-buffer-size
                                                      :usage (bit-or js/GPUBufferUsage.STORAGE
                                                               js/GPUBufferUsage.COPY_DST)})))
     :node-count-buffer (-> device (.createBuffer (clj->js {:label "node count buffer"
                                                            :size node-count-buffer-size
                                                            :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                     js/GPUBufferUsage.COPY_DST)})))
     :edge-count-buffer (-> device (.createBuffer (clj->js {:label "edge count buffer"
                                                            :size edge-count-buffer-size
                                                            :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                     js/GPUBufferUsage.COPY_DST)})))}))

(defn create-compute-pipeline [device graph-buffers]
  (println "create compute pipeline")
  (let [repulsion-bind-group-layout (-> device
                                      (.createBindGroupLayout
                                        (clj->js {:label "repulsion bind group layout"
                                                  :entries [{:binding 0
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "storage"}}
                                                            {:binding 1
                                                             :visibility js/GPUShaderStage.COMPUTE
                                                             :buffer {:type "uniform"}}]})))


        attraction-bind-group-layout (-> device
                                       (.createBindGroupLayout
                                         (clj->js {:label "attraction bind group layout"
                                                   :entries [{:binding 0
                                                              :visibility js/GPUShaderStage.COMPUTE
                                                              :buffer {:type "storage"}}
                                                             {:binding 1
                                                              :visibility js/GPUShaderStage.COMPUTE
                                                              :buffer {:type "read-only-storage"}}
                                                             {:binding 2
                                                              :visibility js/GPUShaderStage.COMPUTE
                                                              :buffer {:type "uniform"}}]})))

        integration-bind-group-layout (-> device
                                        (.createBindGroupLayout
                                          (clj->js {:label "integration bind group layout"
                                                    :entries [{:binding 0
                                                               :visibility js/GPUShaderStage.COMPUTE
                                                               :buffer {:type "storage"}}
                                                              {:binding 1
                                                               :visibility js/GPUShaderStage.COMPUTE
                                                               :buffer {:type "uniform"}}]})))

        repulsion-pipeline-layout (-> device
                                    (.createPipelineLayout
                                      (clj->js {:label "repulsion pipeline layout"
                                                :bindGroupLayouts [repulsion-bind-group-layout]})))

        attraction-pipeline-layout (-> device
                                     (.createPipelineLayout
                                       (clj->js {:label "attraction pipeline layout"
                                                 :bindGroupLayouts [attraction-bind-group-layout]})))

        integration-pipeline-layout (-> device
                                      (.createPipelineLayout
                                        (clj->js {:label "integration pipeline layout"
                                                  :bindGroupLayouts [integration-bind-group-layout]})))

        repulsion-shader-module (-> device
                                  (.createShaderModule
                                    (clj->js {:code repulsion-shader-code})))

        attraction-shader-module (-> device
                                   (.createShaderModule
                                     (clj->js {:code attraction-shader-code})))

        integration-shader-module (-> device
                                    (.createShaderModule
                                      (clj->js {:code integration-shader-code})))

        repulsion-pipeline (-> device
                             (.createComputePipeline
                               (clj->js {:layout repulsion-pipeline-layout
                                         :compute {:module repulsion-shader-module
                                                   :entryPoint "main"}})))

        attraction-pipeline (-> device
                              (.createComputePipeline
                                (clj->js {:layout attraction-pipeline-layout
                                          :compute {:module attraction-shader-module
                                                    :entryPoint "main"}})))

        integration-pipeline (-> device
                               (.createComputePipeline
                                 (clj->js {:layout integration-pipeline-layout
                                           :compute {:module integration-shader-module
                                                     :entryPoint "main"}})))

        repulsion-bind-group (-> device
                               (.createBindGroup
                                 (clj->js {:layout repulsion-bind-group-layout
                                           :entries [{:binding 0
                                                      :resource {:buffer (:node-buffer graph-buffers)}}
                                                     {:binding 1
                                                      :resource {:buffer (:node-count-buffer graph-buffers)}}]})))

        attraction-bind-group (-> device
                                (.createBindGroup
                                  (clj->js {:layout attraction-bind-group-layout
                                            :entries [{:binding 0
                                                       :resource {:buffer (:node-buffer graph-buffers)}}
                                                      {:binding 1
                                                       :resource {:buffer (:edge-buffer graph-buffers)}}
                                                      {:binding 2
                                                       :resource {:buffer (:edge-count-buffer graph-buffers)}}]})))

        integration-bind-group (-> device
                                 (.createBindGroup
                                   (clj->js {:layout integration-bind-group-layout
                                             :entries [{:binding 0
                                                        :resource {:buffer (:node-buffer graph-buffers)}}
                                                       {:binding 1
                                                        :resource {:buffer (:node-count-buffer graph-buffers)}}]})))]

    {:repulsion-pipeline repulsion-pipeline
     :attraction-pipeline attraction-pipeline
     :integration-pipeline integration-pipeline
     :repulsion-bind-group repulsion-bind-group
     :attraction-bind-group attraction-bind-group
     :integration-bind-group integration-bind-group}))

(defn run-simulation [device graph pipelines buffers]
  (let [num-iterations 5]
    (println "run simulation")

    ;; Initial buffer writes (outside the simulation loop)
    (.writeBuffer
      (.-queue device)
      (:node-count-buffer buffers)
      0
      (js/Uint32Array. (clj->js [(:num-nodes graph)])))

    (.writeBuffer
      (.-queue device)
      (:edge-count-buffer buffers)
      0
      (js/Uint32Array. (clj->js [(:num-edges graph)])))

    (.writeBuffer
      (.-queue device)
      (:node-buffer buffers)
      0
      (:nodes graph))

    (.writeBuffer
      (.-queue device)
      (:edge-buffer buffers)
      0
      (:edges graph))

    ;; Run iterations
    (dotimes [i num-iterations]
      (let [command-encoder (.createCommandEncoder device)
            compute-pass (.beginComputePass command-encoder)]

        (println "iteration" i)

        ;; Repulsion pass
        (.setPipeline compute-pass (:repulsion-pipeline pipelines))
        (.setBindGroup compute-pass 0 (:repulsion-bind-group pipelines))
        (.dispatchWorkgroups compute-pass (js/Math.ceil (/ (:num-nodes graph) 64)))

        ;; Attraction pass
        (.setPipeline compute-pass (:attraction-pipeline pipelines))
        (.setBindGroup compute-pass 0 (:attraction-bind-group pipelines))
        (.dispatchWorkgroups compute-pass (js/Math.ceil (/ (:num-edges graph) 64)))

        ;; Integration pass
        (.setPipeline compute-pass (:integration-pipeline pipelines))
        (.setBindGroup compute-pass 0 (:integration-bind-group pipelines))
        (.dispatchWorkgroups compute-pass (js/Math.ceil (/ (:num-nodes graph) 64)))

        ;; End compute pass and submit commands
        (.end compute-pass)
        (.submit (.-queue device) [(.finish command-encoder)]))
      (let [staging-buffer (.createBuffer
                             device
                             (clj->js {:label "staging buffer"
                                       :size  (* (:num-nodes graph)
                                                (.-BYTES_PER_ELEMENT (:nodes graph)))
                                       :usage (bit-or js/GPUBufferUsage.MAP_READ
                                                js/GPUBufferUsage.COPY_DST)}))
            command-encoder (.createCommandEncoder device)]
        (.copyBufferToBuffer command-encoder
          (:node-buffer buffers)
          0
          staging-buffer
          0
          (* (:num-nodes graph)
            (.-BYTES_PER_ELEMENT (:nodes graph))))
        (.submit (.-queue device) [(.finish command-encoder)])
        (-> (.mapAsync staging-buffer js/GPUMapMode.READ)
          (.then (fn []
                   (let [result-buffer (.getMappedRange staging-buffer)
                         result-array (js/Float32Array. result-buffer)]
                     (println "Final node positions:" (vec result-array))
                     (.unmap staging-buffer)))))))))


;; Example usage (you'll need to adapt this to your specific application context)
(defn main-simulation [device]
  (println "main simulation")
  (let [nodes [{:x 10 :y 20}
               {:x 100 :y 100}
               {:x 200 :y 50}
               {:x 300 :y 300}
               {:x 350 :y 350}] ; Example node positions
        edges [{:node1-index 0 :node2-index 1}
               {:node1-index 1 :node2-index 2}
               {:node1-index 2 :node2-index 3}
               {:node1-index 3 :node2-index 4}
               {:node1-index 4 :node2-index 0}
               {:node1-index 0 :node2-index 2}
               {:node1-index 1 :node2-index 3}
               {:node1-index 2 :node2-index 4}] ; Example edge connections
        graph (create-graph nodes edges)
        _ (println "graph" graph)
        buffers (create-graph-buffers device graph)
        _ (println "buffers" buffers)
        pipelines (create-compute-pipeline device buffers)
        _ (println "pipelines" pipelines)]

    (run-simulation device graph pipelines buffers)))



