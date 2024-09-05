(ns app.client.webgpu.shader)


(def shader-descriptor
  (clj->js
    {:label "cell shader"
     :code "@group(0) @binding(0) var<uniform> grid: vec2f;
             @vertex
             fn vertexMain(@location(0) pos: vec2f,
                           @builtin(instance_index) instance: u32) ->
               @builtin(position) vec4f {
               let i = f32(instance);
               let cell = vec2f(i % grid.x, floor(i / grid.x));\n
               let cellOffset = cell / grid * 2;
               let gridPos = (pos / 16 + 1) / grid - 1 + cellOffset;
               //return vec4f(gridPos, 0, 1);
               return vec4f(pos, 0, 1);
             }

             @fragment
             fn fragmentMain() -> @location(0) vec4f {
               return vec4f(0.1, 0.1, 0.1, 1);
             }"}))

(def add-new-rects-shader-descriptor
  (clj->js {:label "vertices compute shader descriptor"
            :code  "
                     struct CanvasSettings {
                            width: f32,
                            height: f32,
                            panX: f32,
                            panY: f32
                     }
                     // Constants for screen dimensions
                     @group(0) @binding(0) var<storage, read> rectangles: array<f32>;     // Flattened input array
                     @group(0) @binding(1) var<storage, read_write> vertices: array<f32>; // Output vertices
                     @group(0) @binding(2) var<uniform> canvas_settings: CanvasSettings;  // Canvas settings uniform\n


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
                      let left = (x / canvas_settings.width ) * 2 - 1 ;
                      let right = ((x + width) / canvas_settings.width  ) * 2 - 1;
                      let top = 1 - (y / canvas_settings.height ) * 2 ;
                      let bottom = 1 - ((y + height) / canvas_settings.height ) * 2  ;


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

                      struct VertexOutput {
                      @builtin(position) position: vec4f,
                      @location(0) fragPos: vec2f
                      }

                      @group(0) @binding(1) var<storage, read> vertex_buffer: array<f32>; // Changed to read-only\n
                      @group(0) @binding(2) var<uniform> can_settings: CanvasSettings;  // Canvas settings uniform\\n\n

                      @vertex
                      fn renderVertices(@builtin(vertex_index) vertexIndex: u32) -> VertexOutput {
                         let index = vertexIndex * 2;
                         let px = vertex_buffer[index] + can_settings.panX;
                         let py = vertex_buffer[index + 1] - can_settings.panY;

                         // Pass position to the fragment shader
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


(defn transform-vertices []
  "
  struct Uniforms
  {
   color: vec3<f32>,
   viewbox: vec4<f32>, // x, y, width, height}


  @group(0) @binding(0) var<uniform> uniforms: Uniforms;
  @group(0) @binding(1) var<storage, read> vertex_buffer: array<f32>;


  struct VertexOutput
  {
   @builtin(position) position: vec4f,
   @location(0) fragPos: vec2f}


  @vertex
  fn renderVertices(@builtin(vertex_index) vertexIndex: u32) -> VertexOutput
  {
   let index = vertexIndex * 2;
   let px = vertex_buffer[index];
   let py = vertex_buffer[index + 1];

   // Apply viewbox transformation
   let x = (px - uniforms.viewbox.x) / uniforms.viewbox.z * 2.0 - 1.0;
   let y = 1.0 - (py - uniforms.viewbox.y) / uniforms.viewbox.w * 2.0;

   var output: VertexOutput;
   output.position = vec4f(x, y, 0.0, 1.0);
   output.fragPos = vec2f(px, py);  // Keep original position for color calculation
   return output};


  @fragment
  fn renderVerticesFragment(@location(0) fragPos: vec2<f32>) -> @location(0) vec4<f32>
  {
   let baseColor = uniforms.color;

   let r = baseColor.r * (sin(fragPos.x * 3.14159) * 0.2 + 0.8);
   let g = baseColor.g * (cos(fragPos.y * 3.14159) * 0.2 + 0.8);
   let b = baseColor.b * (sin((fragPos.x + fragPos.y) * 3.14159) * 0.2 + 0.8);

   return vec4<f32>(r, g, b, 1.0)});
 ")