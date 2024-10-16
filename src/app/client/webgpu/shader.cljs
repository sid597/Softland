(ns app.client.webgpu.shader)




(def text-vertex-shader 
  (clj->js {:label "text vertex shader"
            :code "// vertex.wgsl
            // VertexInput example: [x1 y1 u1 v1] 
            // - [x1,y1] represent the coords in clipspace for triangle.
            // - [u1,v1] represent the coords in font atlas  

            struct VertexInput { 
               @location(0) position: vec2<f32>,
               @location(1) uv: vec2<f32>,
            };

            // Outputting: the position of the vertices and uv coords but why?

            struct VertexOutput {
               @builtin(position) position: vec4<f32>,
               @location(0) uv: vec2<f32>,
            };

            @vertex
            fn main(input: VertexInput) -> VertexOutput {
               var output: VertexOutput;
               output.position = vec4<f32>(input.position, 0.0, 1.0);
               output.uv = input.uv;
               return output;
               }
            "}))

(def text-fragment-shader
  (clj->js {:label "text fragment shader"
            :code "// fragment.wgsl
            @group(0) @binding(0)
            var sampler0: sampler;

            @group(0) @binding(1)
            var texture0: texture_2d<f32>;

            @group(0) @binding(2) var<uniform> sizes:sizing;

            struct sizing {
              pxRange: f32,
              atlasSize: f32,
              renderSize: f32,
            }


            fn median(a: f32, b: f32, c: f32) -> f32 {
                return max(min(a, b), min(max(a, b), c));
            }

            @fragment
            fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> 
            {
             let msd = textureSample(texture0, sampler0, uv).rgb;
             let sd = median(msd.r, msd.g, msd.b);

             let pxRange = sizes.pxRange; 
             let atlasSize = sizes.atlasSize; 
             let renderSize = sizes.renderSize; 
             
             let screenPxRange = max(pxRange * (renderSize / atlasSize), 1.0);
             
             let screenPxDistance = screenPxRange * (sd - 0.5);
             let opacity = clamp(screenPxDistance + 0.5, 0.0, 1.0);
             
             let text = vec4<f32>(0.0, 0.0, 0.0, opacity); // Transparent background how??

             return text;
            }
            "}))


(def add-new-rects-shader-descriptor
  (clj->js {:label "vertices compute shader descriptor"
            :code  "
                     struct CanvasSettings {
                            width: f32,
                            height: f32,
                            panX: f32,
                            panY: f32,
                            zoomFactor: f32,
                     }
                     // Constants for screen dimensions
                     @group(0) @binding(0) var<storage, read>       rectangles: array<f32>;     // Flattened input array
                     @group(0) @binding(1) var<storage, read_write> vertices: array<f32>; // Output vertices
                     @group(0) @binding(2) var<uniform>             canvas_settings: CanvasSettings;  // Canvas settings uniform\n
                     @group(0) @binding(3) var<storage, read>       id_buffer: array<u32>;
                     @group(0) @binding(4) var<storage, read_write> rendered_ids: array<atomic<u32>>;


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

                      let left = ((x / canvas_settings.width ) * 2 - 1)               * canvas_settings.zoomFactor + canvas_settings.panX;
                      let right = (((x + width) / canvas_settings.width  ) * 2 - 1)   * canvas_settings.zoomFactor + canvas_settings.panX;
                      let top = (1 - (y / canvas_settings.height ) * 2)               * canvas_settings.zoomFactor + canvas_settings.panY ;
                      let bottom = (1 - ((y + height) / canvas_settings.height ) * 2) * canvas_settings.zoomFactor + canvas_settings.panY ;



                      if (max(left, right) >= -1.0 && min(left, right) <= 1.0 && max(top, bottom) >= -1.0 && min(top, bottom) <= 1.0) {
                         let rect_id = id_buffer[index];
                         let pos = atomicAdd(&rendered_ids[0], 1u);
                         atomicStore(&rendered_ids[pos], rect_id);


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
                      }}

                      struct VertexOutput {
                      @builtin(position) position: vec4f,
                      @location(0) fragPos: vec2f
                      }

                      @group(0) @binding(1) var<storage, read>       vertex_buffer: array<f32>; // Changed to read-only


                      @vertex
                      fn renderVertices(@builtin(vertex_index) vertexIndex: u32) -> VertexOutput {
                         let index = vertexIndex * 2;
                         let px = vertex_buffer[index] ;
                         let py = vertex_buffer[index + 1];

                         var output: VertexOutput;
                         output.position = vec4f(px, py, 0.0, 1.0);
                         output.fragPos = vec2f(px, py);
                         return output;
                      }

                      @fragment
                      fn renderVerticesFragment(@location(0) fragPos: vec2<f32>) -> @location(0) vec4<f32> {
                          // Use both x and y coordinates for more varied colors
                          let r = sin(fragPos.x * 3.14159) * 0.3 + 0.9;
                          let g = cos(fragPos.y * 3.14159) * 0.3 + 0.5;
                          let b = sin((fragPos.x + fragPos.y) * 3.14159) * 0.3 + 0.5;


                          // Add some randomness to the color
                          let random = fract(sin(dot(fragPos, vec2(12.898, 78.233))) * 43758.5453);

                          // Mix the gradient colors with the random value
                          let color = mix(
                          vec4<f32>(r, g, b, 1.0),
                          vec4<f32>(random, random, random, 1.0),
                          0.1  // Adjust this value to control the amount of randomness (0.0 to 1.0)
                          );

                          return color;
                      }
                     "}))
