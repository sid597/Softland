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

(def add-new-rect-shader-descriptor
  (clj->js
    {:label "add rect shader"
     :code "
     struct Rect {x: f32; y: f32;width: f32;height: f32;};
     @group(1) @binding(0) var<storage, read> rect: array<Rect>
     @vertex
     fn drawRect() -> @builtin(position): vec4f "}))