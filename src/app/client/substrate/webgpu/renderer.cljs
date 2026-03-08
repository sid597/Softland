(ns app.client.substrate.webgpu.renderer)

;; --- 1. SHADERS ---
;; Rich quads: 28 floats/rect, SDF-based rounded corners, borders, gradients
(def rect-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(0) var<uniform> camera: Camera;
  struct InstanceInput {
    @location(0) rect_geometry: vec4<f32>,
    @location(1) color: vec4<f32>,
    @location(2) corner_radii: vec4<f32>,
    @location(3) border_widths: vec4<f32>,
    @location(4) border_color: vec4<f32>,
    @location(5) gradient: vec4<f32>,
    @location(6) gradient_color2: vec4<f32>,
  };
  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) color: vec4<f32>,
    @location(1) local_pos: vec2<f32>,
    @location(2) rect_size: vec2<f32>,
    @location(3) corner_radii: vec4<f32>,
    @location(4) border_widths: vec4<f32>,
    @location(5) border_color: vec4<f32>,
    @location(6) gradient: vec4<f32>,
    @location(7) gradient_color2: vec4<f32>,
  };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;
      var pos = vec2<f32>(0.0, 0.0);
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
          case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
          case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
      }
      let world_pos = vec2<f32>(instance.rect_geometry.x + (pos.x * instance.rect_geometry.z),
                                instance.rect_geometry.y + (pos.y * instance.rect_geometry.w));
      let panned = (world_pos * camera.zoom) + camera.pan;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.color = instance.color;
      // Pass local UV (0..size in pixels) and rect size for SDF evaluation
      output.local_pos = pos * instance.rect_geometry.zw * camera.zoom;
      output.rect_size = instance.rect_geometry.zw * camera.zoom;
      output.corner_radii = instance.corner_radii;
      output.border_widths = instance.border_widths;
      output.border_color = instance.border_color;
      output.gradient = instance.gradient;
      output.gradient_color2 = instance.gradient_color2;
      return output;
  }")

(def rect-fragment-shader "
  // Inigo Quilez SDF rounded box with per-corner radii
  fn sd_rounded_box(p: vec2<f32>, half_size: vec2<f32>, radii: vec4<f32>) -> f32 {
      // radii: tl, tr, br, bl → select based on quadrant
      var r: vec2<f32>;
      if (p.x > 0.0) {
          r = vec2<f32>(radii.y, radii.z);  // tr, br
      } else {
          r = vec2<f32>(radii.x, radii.w);  // tl, bl
      }
      if (p.y > 0.0) {
          r.x = r.y;  // bottom row
      }
      let q = abs(p) - half_size + vec2<f32>(r.x, r.x);
      return min(max(q.x, q.y), 0.0) + length(max(q, vec2<f32>(0.0, 0.0))) - r.x;
  }

  @fragment
  fn main(
    @location(0) color: vec4<f32>,
    @location(1) local_pos: vec2<f32>,
    @location(2) rect_size: vec2<f32>,
    @location(3) corner_radii: vec4<f32>,
    @location(4) border_widths: vec4<f32>,
    @location(5) border_color: vec4<f32>,
    @location(6) gradient: vec4<f32>,
    @location(7) gradient_color2: vec4<f32>,
  ) -> @location(0) vec4<f32> {
      let half_size = rect_size * 0.5;
      // p in centered coordinates: (0,0) = center of rect
      let p = local_pos - half_size;

      // Clamp radii so they don't exceed half the smallest dimension
      let max_r = min(half_size.x, half_size.y);
      let radii = min(corner_radii, vec4<f32>(max_r, max_r, max_r, max_r));

      let dist = sd_rounded_box(p, half_size, radii);

      // Anti-aliased edge (1px smoothstep)
      let aa = clamp(0.5 - dist, 0.0, 1.0);

      // --- Fill color (with optional gradient) ---
      var fill = color;
      let t_stop = gradient.y;
      if (t_stop > 0.0) {
          // Linear gradient: angle in radians, t_stop = blend position
          let angle = gradient.x;
          let cs = cos(angle);
          let sn = sin(angle);
          // Project centered UV onto gradient axis
          let uv_norm = local_pos / rect_size;
          let t = clamp(uv_norm.x * cs + uv_norm.y * sn, 0.0, 1.0);
          fill = mix(color, gradient_color2, smoothstep(0.0, t_stop, t));
      }

      // --- Border ---
      let has_border = (border_widths.x + border_widths.y + border_widths.z + border_widths.w) > 0.0;
      if (has_border) {
          // Use max border width for SDF shrink (uniform-ish approach)
          let bw = max(max(border_widths.x, border_widths.y), max(border_widths.z, border_widths.w));
          let inner_half = half_size - vec2<f32>(bw, bw);
          let inner_radii = max(radii - vec4<f32>(bw, bw, bw, bw), vec4<f32>(0.0, 0.0, 0.0, 0.0));
          let inner_dist = sd_rounded_box(p, inner_half, inner_radii);
          let inner_aa = clamp(0.5 - inner_dist, 0.0, 1.0);
          // Composite: border color in the ring, fill inside
          let result = mix(border_color, fill, inner_aa);
          return vec4<f32>(result.rgb, result.a * aa);
      }

      return vec4<f32>(fill.rgb, fill.a * aa);
  }")

;; --- Shadow shaders ---
;; 20 floats/shadow (80 bytes): expanded_rect, shadow_color, corner_radii, blur_params, inner_rect
(def shadow-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(0) var<uniform> camera: Camera;
  struct InstanceInput {
    @location(0) expanded_rect: vec4<f32>,
    @location(1) shadow_color: vec4<f32>,
    @location(2) corner_radii: vec4<f32>,
    @location(3) blur_params: vec4<f32>,
    @location(4) inner_rect: vec4<f32>,
  };
  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) shadow_color: vec4<f32>,
    @location(1) local_pos: vec2<f32>,
    @location(2) rect_size: vec2<f32>,
    @location(3) corner_radii: vec4<f32>,
    @location(4) blur_params: vec4<f32>,
    @location(5) inner_rect: vec4<f32>,
  };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;
      var pos = vec2<f32>(0.0, 0.0);
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
          case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
          case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
      }
      let world_pos = vec2<f32>(instance.expanded_rect.x + (pos.x * instance.expanded_rect.z),
                                instance.expanded_rect.y + (pos.y * instance.expanded_rect.w));
      let panned = (world_pos * camera.zoom) + camera.pan;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.shadow_color = instance.shadow_color;
      output.local_pos = pos * instance.expanded_rect.zw * camera.zoom;
      output.rect_size = instance.expanded_rect.zw * camera.zoom;
      output.corner_radii = instance.corner_radii;
      output.blur_params = instance.blur_params;
      // Scale inner_rect to match zoom-scaled local_pos
      output.inner_rect = vec4<f32>(
          instance.inner_rect.xy * camera.zoom,
          instance.inner_rect.zw * camera.zoom);
      return output;
  }")

(def shadow-fragment-shader "
  // Approximate erf for Gaussian CDF shadow falloff
  fn erf_approx(x: f32) -> f32 {
      let a = abs(x);
      // Abramowitz & Stegun approximation (max error ~1.5e-7)
      let t = 1.0 / (1.0 + 0.3275911 * a);
      let poly = t * (0.254829592 + t * (-0.284496736 + t * (1.421413741 + t * (-1.453152027 + t * 1.061405429))));
      let result = 1.0 - poly * exp(-a * a);
      return select(-result, result, x >= 0.0);
  }

  // SDF rounded box (same as rect shader)
  fn sd_rounded_box(p: vec2<f32>, half_size: vec2<f32>, radii: vec4<f32>) -> f32 {
      var r: vec2<f32>;
      if (p.x > 0.0) {
          r = vec2<f32>(radii.y, radii.z);
      } else {
          r = vec2<f32>(radii.x, radii.w);
      }
      if (p.y > 0.0) {
          r.x = r.y;
      }
      let q = abs(p) - half_size + vec2<f32>(r.x, r.x);
      return min(max(q.x, q.y), 0.0) + length(max(q, vec2<f32>(0.0, 0.0))) - r.x;
  }

  @fragment
  fn main(
    @location(0) shadow_color: vec4<f32>,
    @location(1) local_pos: vec2<f32>,
    @location(2) rect_size: vec2<f32>,
    @location(3) corner_radii: vec4<f32>,
    @location(4) blur_params: vec4<f32>,
    @location(5) inner_rect: vec4<f32>,
  ) -> @location(0) vec4<f32> {
      let blur = blur_params.x;
      let offset = blur_params.yz;
      let spread = blur_params.w;

      // Inner rect center and half-size (in zoom-scaled pixels, relative to expanded quad)
      let inner_center = (inner_rect.xy + inner_rect.zw * 0.5) + offset;
      let inner_half = inner_rect.zw * 0.5 + vec2<f32>(spread, spread);

      let max_r = min(inner_half.x, inner_half.y);
      let radii = min(corner_radii, vec4<f32>(max_r, max_r, max_r, max_r));

      // Pixel position relative to inner rect center
      let p = local_pos - inner_center;
      let dist = sd_rounded_box(p, inner_half, radii);

      // Gaussian CDF falloff
      let sigma = max(blur * 0.5, 0.001);
      let alpha = 0.5 - 0.5 * erf_approx(dist / (sigma * 1.4142135));

      return vec4<f32>(shadow_color.rgb, shadow_color.a * alpha);
  }")

(def text-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  // Per-instance: rect (vec4), uv_bounds (vec4), color (vec4) = 12 floats
  struct InstanceInput { @location(0) rect: vec4<f32>, @location(1) uv_bounds: vec4<f32>, @location(2) color: vec4<f32>, };
  struct VertexOutput { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32>, @location(1) v_visual_size: f32, @location(2) color: vec4<f32>, };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
      var output: VertexOutput;
      var pos = vec2<f32>(0.0, 0.0);
      switch(v_index) {
          case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
          case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
          case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
      }
      let world_pos = vec2<f32>(instance.rect.x + (pos.x * instance.rect.z),
                                instance.rect.y + (pos.y * instance.rect.w));
      let u = mix(instance.uv_bounds.x, instance.uv_bounds.z, pos.x);
      let v = mix(instance.uv_bounds.y, instance.uv_bounds.w, pos.y);
      let panned = (world_pos * camera.zoom) + camera.pan;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.uv = vec2<f32>(u, v);
      output.v_visual_size = max(instance.rect.z, instance.rect.w) * camera.zoom;
      output.color = instance.color;
      return output;
  }")

(def text-fragment-shader "
  @group(0) @binding(0) var sampler0: sampler;
  @group(0) @binding(1) var texture0: texture_2d<f32>;
  // Sizing uniform: pxRange, atlasEmSize, sharpness (color now per-instance)
  struct Sizing { pxRange: f32, atlasEmSize: f32, sharpness: f32, padding: f32, };
  @group(0) @binding(3) var<uniform> params: Sizing;
  fn median(a: f32, b: f32, c: f32) -> f32 { return max(min(a, b), min(max(a, b), c)); }

  @fragment
  fn main(@location(0) uv: vec2<f32>, @location(1) visual_size: f32, @location(2) color: vec4<f32>) -> @location(0) vec4<f32> {
       let msd = textureSample(texture0, sampler0, uv).rgb;
       let sd = median(msd.r, msd.g, msd.b);
       let screenPxRange = max(params.pxRange * (visual_size / params.atlasEmSize), 1.0);
       // sharpness: negative = sharper edges, positive = softer edges, 0 = standard MSDF
       let dist = sd - 0.5 + params.sharpness;
       let opacity = clamp(dist * screenPxRange + 0.5, 0.0, 1.0);
       // Use per-instance color instead of uniform color
       return vec4<f32>(color.rgb, opacity * color.a);
  }")

;; Calculate bracket highlight rectangles
(defn calculate-bracket-rects [bracket-match font-size start-x start-y line-h]
  (when bracket-match
    (let [char-w (* font-size 0.56)
          {:keys [open close]} bracket-match
          make-rect (fn [{:keys [line col]}]
                      {:x (+ start-x (* col char-w))
                       :y (+ start-y (* line line-h))
                       :w char-w
                       :h line-h
                       ;; Golden/yellow highlight for matching brackets
                       :r 0.8 :g 0.6 :b 0.2 :a 0.4})]
      [(make-rect open) (make-rect close)])))

;; Updated hit-test: clamps column to actual line length
(defn hit-test [x y font-size start-x start-y line-h line-lengths]
  (let [char-w     (* font-size 0.56)
        rel-x      (- x start-x)
        rel-y      (- y start-y)
        line-idx   (max 0 (Math/floor (/ rel-y line-h)))
        ;; Clamp line index to valid range
        line-idx   (min line-idx (max 0 (dec (count line-lengths))))
        ;; Get actual line length, default to 0 for empty lines
        line-len   (get line-lengths line-idx 0)
        ;; Clamp column to [0, line-length]
        col-idx    (-> (/ rel-x char-w)
                       (Math/round)
                       (max 0)
                       (min line-len))]
    {:line line-idx :col col-idx}))

;; --- 2. INITIALIZATION ---

(def rect-stride 112)  ;; 28 floats × 4 bytes = 112 bytes per rect

(defn init-rect-system [^js/GPUDevice device fformat camera-buffer & {:keys [initial-capacity] :or {initial-capacity 1000}}]
  (let [v-module (.createShaderModule device (clj->js {:code rect-vertex-shader}))
        f-module (.createShaderModule device (clj->js {:code rect-fragment-shader}))
        buf-size (* initial-capacity rect-stride)
        instance-buffer (.createBuffer device (clj->js {:size buf-size :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility (bit-or js/GPUShaderStage.VERTEX js/GPUShaderStage.FRAGMENT) :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout pipeline-layout
                              :vertex {:module v-module :entryPoint "main"
                                       :buffers [{:arrayStride rect-stride :stepMode "instance"
                                                  :attributes [{:shaderLocation 0 :offset 0  :format "float32x4"}   ;; rect_geometry
                                                               {:shaderLocation 1 :offset 16 :format "float32x4"}   ;; color
                                                               {:shaderLocation 2 :offset 32 :format "float32x4"}   ;; corner_radii
                                                               {:shaderLocation 3 :offset 48 :format "float32x4"}   ;; border_widths
                                                               {:shaderLocation 4 :offset 64 :format "float32x4"}   ;; border_color
                                                               {:shaderLocation 5 :offset 80 :format "float32x4"}   ;; gradient
                                                               {:shaderLocation 6 :offset 96 :format "float32x4"}]}]}  ;; gradient_color2
                              :fragment {:module f-module :entryPoint "main"
                                         :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                            :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                              :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout :entries [{:binding 0 :resource {:buffer camera-buffer}}]}))]
    {:pipeline pipeline :bind-group bind-group :instance-buffer instance-buffer :capacity initial-capacity :num-instances 0}))

(defn init-text-system [^js/GPUDevice device fformat atlas font-bitmap & {:keys [initial-capacity] :or {initial-capacity 10000}}]
  (let [vertex-module (.createShaderModule device (clj->js {:code text-vertex-shader}))
        fragment-module (.createShaderModule device (clj->js {:code text-fragment-shader}))
        texture (.createTexture device (clj->js {:size {:width (.-width font-bitmap) :height (.-height font-bitmap) :depthOrArrayLayers 1}
                                                 :format "rgba8unorm" :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT js/GPUTextureUsage.TEXTURE_BINDING js/GPUTextureUsage.COPY_DST)}))
        _ (.copyExternalImageToTexture (.-queue device) (clj->js {:source font-bitmap}) (clj->js {:texture texture}) (clj->js {:width (.-width font-bitmap) :height (.-height font-bitmap)}))
        sampler (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear" :mipmapFilter "linear"}))
        ;; 12 floats per glyph: rect(4) + uv(4) + color(4) = 48 bytes
        instance-buffer (.createBuffer device (clj->js {:size (* initial-capacity 48) :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        camera-buffer (.createBuffer device (clj->js {:size 24 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
        sizes-buffer (.createBuffer device (clj->js {:size 16 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT :sampler {:type "filtering"}}
                                                                     {:binding 1 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "float"}}
                                                                     {:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                                                     {:binding 3 :visibility js/GPUShaderStage.FRAGMENT :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device (clj->js {:layout pipeline-layout
                                                         :vertex {:module vertex-module :entryPoint "main"
                                                                  ;; 12 floats: rect(4) + uv(4) + color(4) = 48 bytes stride
                                                                  :buffers [{:arrayStride 48 :stepMode "instance"
                                                                             :attributes [{:shaderLocation 0 :offset 0 :format "float32x4"}   ;; rect
                                                                                          {:shaderLocation 1 :offset 16 :format "float32x4"}  ;; uv_bounds
                                                                                          {:shaderLocation 2 :offset 32 :format "float32x4"}]}]}
                                                         :fragment {:module fragment-module :entryPoint "main"
                                                                    :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                                                       :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                                                         :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout
                                                      :entries [{:binding 0 :resource sampler} {:binding 1 :resource (.createView texture)}
                                                                {:binding 2 :resource {:buffer camera-buffer}} {:binding 3 :resource {:buffer sizes-buffer}}]}))]
    {:pipeline pipeline :bind-group bind-group :bind-group-layout bg-layout :camera-uniform-buffer camera-buffer :sizes-uniform-buffer sizes-buffer :instance-buffer instance-buffer :num-instances 0}))

(defn update-font-texture [^js/GPUDevice device renderer-state font-bitmap]
  (let [texture (.createTexture device (clj->js {:size {:width (.-width font-bitmap) :height (.-height font-bitmap) :depthOrArrayLayers 1}
                                                 :format "rgba8unorm" :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT js/GPUTextureUsage.TEXTURE_BINDING js/GPUTextureUsage.COPY_DST)}))
        _ (.copyExternalImageToTexture (.-queue device) (clj->js {:source font-bitmap}) (clj->js {:texture texture}) (clj->js {:width (.-width font-bitmap) :height (.-height font-bitmap)}))
        sampler (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear" :mipmapFilter "linear"}))
        
        new-bind-group (.createBindGroup device (clj->js {:layout (:bind-group-layout renderer-state)
                                                          :entries [{:binding 0 :resource sampler} 
                                                                    {:binding 1 :resource (.createView texture)}
                                                                    {:binding 2 :resource {:buffer (:camera-uniform-buffer renderer-state)}} 
                                                                    {:binding 3 :resource {:buffer (:sizes-uniform-buffer renderer-state)}}]}))]
    (assoc renderer-state :bind-group new-bind-group)))

;; --- Shadow system ---
(def shadow-stride 80)  ;; 20 floats × 4 bytes = 80 bytes per shadow

(defn init-shadow-system [^js/GPUDevice device fformat camera-buffer & {:keys [initial-capacity] :or {initial-capacity 256}}]
  (let [v-module (.createShaderModule device (clj->js {:code shadow-vertex-shader}))
        f-module (.createShaderModule device (clj->js {:code shadow-fragment-shader}))
        buf-size (* initial-capacity shadow-stride)
        instance-buffer (.createBuffer device (clj->js {:size buf-size :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility (bit-or js/GPUShaderStage.VERTEX js/GPUShaderStage.FRAGMENT) :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout pipeline-layout
                              :vertex {:module v-module :entryPoint "main"
                                       :buffers [{:arrayStride shadow-stride :stepMode "instance"
                                                  :attributes [{:shaderLocation 0 :offset 0  :format "float32x4"}   ;; expanded_rect
                                                               {:shaderLocation 1 :offset 16 :format "float32x4"}   ;; shadow_color
                                                               {:shaderLocation 2 :offset 32 :format "float32x4"}   ;; corner_radii
                                                               {:shaderLocation 3 :offset 48 :format "float32x4"}   ;; blur_params
                                                               {:shaderLocation 4 :offset 64 :format "float32x4"}]}]}  ;; inner_rect
                              :fragment {:module f-module :entryPoint "main"
                                         :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                            :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                              :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout :entries [{:binding 0 :resource {:buffer camera-buffer}}]}))]
    {:pipeline pipeline :bind-group bind-group :instance-buffer instance-buffer :capacity initial-capacity :num-instances 0}))

(defn update-shadows [^js device shadow-system shadows]
  (let [n (count shadows)
        floats-per-shadow 20
        data (js/Float32Array. (* n floats-per-shadow))
        required-bytes (.-byteLength data)
        current-buffer (:instance-buffer shadow-system)
        current-size (.-size ^js current-buffer)
        needs-resize? (> required-bytes current-size)
        new-buffer (if needs-resize?
                     (do (.destroy ^js current-buffer)
                         (.createBuffer device (clj->js {:size (max required-bytes (* n shadow-stride))
                                                         :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                        js/GPUBufferUsage.COPY_DST)})))
                     current-buffer)]
    (loop [i 0 ss shadows]
      (when (seq ss)
        (let [s (first ss)
              {:keys [x y w h blur offset-x offset-y spread color corner-radii radius]} s
              blur   (or blur 8.0)
              ox     (or offset-x 0.0)
              oy     (or offset-y 0.0)
              spread (or spread 0.0)
              sc     (or color [0 0 0 0.25])
              expand (* 3.0 blur)
              ;; Expanded quad (captures Gaussian tail)
              ex     (- x expand (max ox 0))
              ey     (- y expand (max oy 0))
              ew     (+ w (* 2 expand) (Math/abs ox))
              eh     (+ h (* 2 expand) (Math/abs oy))
              ;; Inner rect relative to expanded quad origin
              ix     (- x ex)
              iy     (- y ey)
              ;; Corner radii
              cr     corner-radii
              ur     (or radius 0.0)
              base   (* i floats-per-shadow)]
          ;; Slot 0: expanded_rect
          (aset data (+ base 0) ex)  (aset data (+ base 1) ey)
          (aset data (+ base 2) ew)  (aset data (+ base 3) eh)
          ;; Slot 1: shadow_color
          (aset data (+ base 4) (nth sc 0))  (aset data (+ base 5) (nth sc 1))
          (aset data (+ base 6) (nth sc 2))  (aset data (+ base 7) (nth sc 3))
          ;; Slot 2: corner_radii
          (if cr
            (do (aset data (+ base 8)  (nth cr 0))
                (aset data (+ base 9)  (nth cr 1))
                (aset data (+ base 10) (nth cr 2))
                (aset data (+ base 11) (nth cr 3)))
            (do (aset data (+ base 8)  ur) (aset data (+ base 9)  ur)
                (aset data (+ base 10) ur) (aset data (+ base 11) ur)))
          ;; Slot 3: blur_params [blur, offset_x, offset_y, spread]
          (aset data (+ base 12) blur)   (aset data (+ base 13) ox)
          (aset data (+ base 14) oy)     (aset data (+ base 15) spread)
          ;; Slot 4: inner_rect (relative to expanded quad, in zoom-scaled space)
          (aset data (+ base 16) ix)  (aset data (+ base 17) iy)
          (aset data (+ base 18) w)   (aset data (+ base 19) h)
          (recur (inc i) (next ss)))))
    (when (pos? n)
      (.writeBuffer (.-queue device) new-buffer 0 data))
    (assoc shadow-system :instance-buffer new-buffer :num-instances n)))

(defn create-editor-state [{:keys [device format atlas bitmap]}]
  (let [text-sys (init-text-system device format atlas bitmap :initial-capacity 1000000)
        rect-sys (init-rect-system device format (:camera-uniform-buffer text-sys) :initial-capacity 50000)
        shadow-sys (init-shadow-system device format (:camera-uniform-buffer text-sys) :initial-capacity 256)

        camera-floats (js/Float32Array. 6)

        pass-descriptor (clj->js {:colorAttachments [{:view nil
                                                      :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
                                                      :loadOp "clear"
                                                      :storeOp "store"}]})]

    {:text-sys text-sys
     :rect-sys rect-sys
     :shadow-sys shadow-sys
     :camera-floats camera-floats
     :pass-descriptor pass-descriptor}))

;; --- 3. UPDATES (CPU -> GPU) ---
(defn shape-text [texts global-fsize msdf-atlas & {:keys [char-width snap-step] :or {char-width 0.56}}]
  "Shape text tokens into GPU-ready quads with per-glyph colors.
   Each token should have: {:text :x :y :size :r :g :b :a}"
  (let [atlas (:atlas msdf-atlas) metrics (:metrics msdf-atlas)
        glyphs (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} (:glyphs msdf-atlas))
        atlas-w (or (:width atlas) 1) atlas-h (or (:height atlas) 1) line-h (or (:lineHeight metrics) 1.2)
        res (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y r g b a]} txt
            ;; Default to white if no color specified
            cr (or r 1.0) cg (or g 1.0) cb (or b 1.0) ca (or a 1.0)
            fsize (or (:size txt) global-fsize)
            snap (when (and snap-step (pos? snap-step))
                   (fn [v] (* (Math/round (/ v snap-step)) snap-step)))
            start-x (if snap (snap x) x)
            start-y (if snap (snap y) y)
            advance (let [v (* fsize char-width)] (if snap (snap v) v))
            !x (atom start-x)
            !y (atom start-y)]
        (doseq [ch (seq text)]
          (let [code (.charCodeAt ch 0)]
            (cond
              (= ch \newline) (do (reset! !x start-x) (reset! !y (+ @!y (* fsize line-h))))
              (= ch \space) (swap! !x + advance)
              :else (when-let [g (get glyphs code)]
                      (let [pb (:planeBounds g)
                            ab (:atlasBounds g)
                            sl (+ @!x (* fsize (or (:left pb) 0)))
                            sr (+ @!x (* fsize (or (:right pb) 0)))
                            st (- @!y (* fsize (or (:top pb) 0)))
                            sb (- @!y (* fsize (or (:bottom pb) 0)))
                            ul (/ (:left ab) atlas-w)
                            ur (/ (:right ab) atlas-w)
                            vt (- 1.0 (/ (:top ab) atlas-h))
                            vb (- 1.0 (/ (:bottom ab) atlas-h))]
                        (swap! !x + advance)
                        ;; Include color in output for per-glyph coloring
                        (swap! res conj {:vertices [[sl sb ul vb fsize] [sr sb ur vb fsize] [sr st ur vt fsize] [sl st ul vt fsize]]
                                         :color [cr cg cb ca]}))))))))
    @res))


(defn update-text-data [^js/GPUDevice device renderer-state texts atlas font-size & {:keys [px-range line-height-factor line-height sharpness char-width snap-step] :or {px-range 8.0 line-height-factor 1.0 sharpness 0.0 char-width 0.56}}]
  "Update GPU buffers with text data. Now includes per-glyph colors (12 floats per glyph)."
  (let [
        line-h (or line-height (* font-size line-height-factor))
        atlas-em (or (get-in atlas [:atlas :size]) 64.0)
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [quads (shape-text tokens-in-line font-size atlas
                                                     :char-width char-width
                                                     :snap-step snap-step)]
                               {:quads quads :count (count quads)}))
                           texts)

        total-instances (reduce + (map :count shaped-lines))
        total-instances (max total-instances 1)
        ;; 12 floats per glyph: rect(4) + uv(4) + color(4)
        data (js/Float32Array. (* total-instances 12))

        line-offsets (loop [lines shaped-lines
                            current-idx 0
                            offsets []]
                       (if (seq lines)
                         (let [cnt (:count (first lines))]
                           (recur (next lines) (+ current-idx cnt) (conj offsets current-idx)))
                         (vec offsets)))

        current-buffer (:instance-buffer renderer-state)
        required-size (.-byteLength data)
        current-size (.-size ^js current-buffer)

        needs-resize? (> required-size current-size)

        new-buffer (if needs-resize?
                     (do
                       (.destroy ^js current-buffer)
                       (.createBuffer device (clj->js {:size required-size
                                                       :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                      js/GPUBufferUsage.COPY_DST)})))
                     current-buffer)

        new-bind-group (if needs-resize?
                         (.createBindGroup device
                           (clj->js {:layout (:bind-group-layout renderer-state)
                                     :entries [{:binding 0
                                                :resource {:buffer new-buffer}}
                                               {:binding 1
                                                :resource {:buffer (:sizes-uniform-buffer renderer-state)}}]}))
                         (:bind-group renderer-state))]

    ;; Write 12 floats per glyph: [x, y, w, h, u_min, v_min, u_max, v_max, r, g, b, a]
    (loop [lines shaped-lines global-i 0]
      (when (seq lines)
        (let [quads (:quads (first lines))]
          (loop [q quads sub-i 0]
            (when (seq q)
              (let [quad (first q)
                    verts (:vertices quad)
                    [cr cg cb ca] (or (:color quad) [1.0 1.0 1.0 1.0])
                    v-tl (nth verts 3) v-br (nth verts 1)
                    [tl_x tl_y u_min v_min _] v-tl [br_x br_y u_max v_max _] v-br
                    base (* (+ global-i sub-i) 12)]
                ;; rect: x, y, w, h
                (aset data (+ base 0) tl_x) (aset data (+ base 1) tl_y)
                (aset data (+ base 2) (- br_x tl_x)) (aset data (+ base 3) (- br_y tl_y))
                ;; uv: u_min, v_min, u_max, v_max
                (aset data (+ base 4) u_min) (aset data (+ base 5) v_min)
                (aset data (+ base 6) u_max) (aset data (+ base 7) v_max)
                ;; color: r, g, b, a
                (aset data (+ base 8) cr) (aset data (+ base 9) cg)
                (aset data (+ base 10) cb) (aset data (+ base 11) ca)
                (recur (next q) (inc sub-i)))))
          (recur (next lines) (+ global-i (:count (first lines)))))))

    (.writeBuffer (.-queue device) new-buffer 0 data)

    ;; Sizes uniform: pxRange, atlasEmSize, sharpness, padding (color now per-instance)
    (let [sizes (js/Float32Array. #js [(float px-range) (float atlas-em) (float sharpness) 0.0])]
      (.writeBuffer (.-queue device) (:sizes-uniform-buffer renderer-state) 0 sizes))

    (assoc renderer-state 
           :instance-buffer new-buffer
           :bind-group new-bind-group 
           :num-instances total-instances
           :line-offsets line-offsets
           :line-height line-h)))


(defn update-rects [^js device rect-system rects]
  (let [n (count rects)
        floats-per-rect 28
        data (js/Float32Array. (* n floats-per-rect))
        required-bytes (.-byteLength data)
        current-buffer (:instance-buffer rect-system)
        current-size (.-size ^js current-buffer)
        needs-resize? (> required-bytes current-size)
        new-buffer (if needs-resize?
                     (do (.destroy ^js current-buffer)
                         (.createBuffer device (clj->js {:size (max required-bytes (* n rect-stride))
                                                         :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                        js/GPUBufferUsage.COPY_DST)})))
                     current-buffer)]
    (loop [i 0 rs rects]
      (when (seq rs)
        (let [rect (first rs)
              {:keys [x y w h r g b a]} rect
              base (* i floats-per-rect)
              ;; Corner radii: uniform :radius or per-corner :corner-radii [tl tr br bl]
              cr (:corner-radii rect)
              uniform-r (or (:radius rect) 0.0)
              ;; Border widths: uniform :border-width or per-side :border-widths [t r b l]
              bw (:border-widths rect)
              uniform-bw (or (:border-width rect) 0.0)
              ;; Border color
              bc (or (:border-color rect) [0 0 0 0])
              ;; Gradient: [angle t-stop 0 0]
              gr (or (:gradient rect) [0 0 0 0])
              ;; Gradient color 2
              gc2 (or (:gradient-color2 rect) [0 0 0 0])]
          ;; Slot 0: rect_geometry [x y w h]
          (aset data (+ base 0) x)  (aset data (+ base 1) y)
          (aset data (+ base 2) w)  (aset data (+ base 3) h)
          ;; Slot 1: color [r g b a]
          (aset data (+ base 4) r)  (aset data (+ base 5) g)
          (aset data (+ base 6) b)  (aset data (+ base 7) a)
          ;; Slot 2: corner_radii [tl tr br bl]
          (if cr
            (do (aset data (+ base 8)  (nth cr 0))
                (aset data (+ base 9)  (nth cr 1))
                (aset data (+ base 10) (nth cr 2))
                (aset data (+ base 11) (nth cr 3)))
            (do (aset data (+ base 8)  uniform-r)
                (aset data (+ base 9)  uniform-r)
                (aset data (+ base 10) uniform-r)
                (aset data (+ base 11) uniform-r)))
          ;; Slot 3: border_widths [top right bottom left]
          (if bw
            (do (aset data (+ base 12) (nth bw 0))
                (aset data (+ base 13) (nth bw 1))
                (aset data (+ base 14) (nth bw 2))
                (aset data (+ base 15) (nth bw 3)))
            (do (aset data (+ base 12) uniform-bw)
                (aset data (+ base 13) uniform-bw)
                (aset data (+ base 14) uniform-bw)
                (aset data (+ base 15) uniform-bw)))
          ;; Slot 4: border_color [r g b a]
          (aset data (+ base 16) (nth bc 0))
          (aset data (+ base 17) (nth bc 1))
          (aset data (+ base 18) (nth bc 2))
          (aset data (+ base 19) (nth bc 3))
          ;; Slot 5: gradient [angle t_stop 0 0]
          (aset data (+ base 20) (nth gr 0))
          (aset data (+ base 21) (nth gr 1))
          (aset data (+ base 22) (nth gr 2))
          (aset data (+ base 23) (nth gr 3))
          ;; Slot 6: gradient_color2 [r g b a]
          (aset data (+ base 24) (nth gc2 0))
          (aset data (+ base 25) (nth gc2 1))
          (aset data (+ base 26) (nth gc2 2))
          (aset data (+ base 27) (nth gc2 3))
          (recur (inc i) (next rs)))))
    (when (pos? n)
      (.writeBuffer (.-queue device) new-buffer 0 data))
    (assoc rect-system :instance-buffer new-buffer :num-instances n)))


(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))


(defn draw-frame! [^js device ^js context text-sys editor-rect-sys cmd-rect-sys camera-floats _ignored_pass_descriptor pan-x pan-y w h
                   & {:keys [cmd-panel-visible cmd-panel-h editor-line-count pre-settings-line-count settings-line-count settings-visible settings-rect-sys
                             diagnostics-visible diagnostics-line-index agent-visible shadow-sys]
                      :or {cmd-panel-visible false cmd-panel-h 40 editor-line-count nil pre-settings-line-count nil settings-line-count 0 settings-visible false
                           settings-rect-sys nil agent-visible false shadow-sys nil}}]
  (update-camera device (:camera-uniform-buffer text-sys) camera-floats pan-x pan-y 1.0 w h)

  (let [encoder (.createCommandEncoder device)
          texture (.getCurrentTexture context)
          view    (.createView texture)

          pass-descriptor (clj->js
                            {:colorAttachments [{:view view
                                                 :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 1.0}
                                                 :loadOp "clear"
                                                 :storeOp "store"}]})

          pass (.beginRenderPass encoder pass-descriptor)]

      ;; Draw shadows FIRST (behind everything)
      (when (and shadow-sys (> (:num-instances shadow-sys) 0))
        (.setPipeline pass (:pipeline shadow-sys))
        (.setBindGroup pass 0 (:bind-group shadow-sys))
        (.setVertexBuffer pass 0 (:instance-buffer shadow-sys))
        (.draw pass 6 (:num-instances shadow-sys)))

      ;; Draw editor rects (selection, brackets, fold indicators, caret, eval)
      (when (and editor-rect-sys (> (:num-instances editor-rect-sys) 0))
        (.setPipeline pass (:pipeline editor-rect-sys))
        (.setBindGroup pass 0 (:bind-group editor-rect-sys))
        (.setVertexBuffer pass 0 (:instance-buffer editor-rect-sys))
        (.draw pass 6 (:num-instances editor-rect-sys)))

      ;; Draw editor text (data-level viewport culling — buffer only contains visible lines)
      (when (and text-sys (> (:num-instances text-sys) 0))
        (.setPipeline pass (:pipeline text-sys))
        (.setBindGroup pass 0 (:bind-group text-sys))
        (.setVertexBuffer pass 0 (:instance-buffer text-sys))

        (let [line-offsets (:line-offsets text-sys)
              total-lines  (count line-offsets)
              editor-lines (or editor-line-count total-lines)

              ;; Draw all editor instances (viewport culling already done at data level)
              editor-end-inst (if (< editor-lines total-lines)
                                (nth line-offsets editor-lines)
                                (:num-instances text-sys))
              draw-count editor-end-inst]
          (when (> draw-count 0)
            (.draw pass 6 draw-count 0 0))

          ;; Agent output background (instance 0) — draw behind text
          (when (and agent-visible cmd-rect-sys (>= (:num-instances cmd-rect-sys) 1))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 0))

          ;; Command panel background (instance 1) — only draw when text data is ready
          ;; Gating on (< editor-lines total-lines) prevents a 1-frame blank box
          ;; from the race between cmd-visible (direct watch) and text-data (derived flow)
          (when (and cmd-panel-visible (< editor-lines total-lines)
                     cmd-rect-sys (>= (:num-instances cmd-rect-sys) 2))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 1))

          ;; Command + agent TEXT (on top of backgrounds)
          (when (< editor-lines total-lines)
            (let [cmd-start-inst (nth line-offsets editor-lines)
                  cmd-end-inst   (:num-instances text-sys)
                  cmd-draw-count (- cmd-end-inst cmd-start-inst)]
              (when (> cmd-draw-count 0)
                (.setPipeline pass (:pipeline text-sys))
                (.setBindGroup pass 0 (:bind-group text-sys))
                (.setVertexBuffer pass 0 (:instance-buffer text-sys))
                (.draw pass 6 cmd-draw-count 0 cmd-start-inst))))

          ;; Caret (instance 2) — draw on top of text, same guard
          (when (and cmd-panel-visible (< editor-lines total-lines)
                     cmd-rect-sys (>= (:num-instances cmd-rect-sys) 3))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 2))

          ;; Status bar background (instance 3) — always visible
          (when (and cmd-rect-sys (>= (:num-instances cmd-rect-sys) 4))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 3))

          ;; Settings panel: draw on top of everything when visible
          (when settings-visible
            ;; Draw settings panel BACKGROUND + UI rects
            (when (and settings-rect-sys (> (:num-instances settings-rect-sys) 0))
              (.setPipeline pass (:pipeline settings-rect-sys))
              (.setBindGroup pass 0 (:bind-group settings-rect-sys))
              (.setVertexBuffer pass 0 (:instance-buffer settings-rect-sys))
              (.draw pass 6 (:num-instances settings-rect-sys)))

            ;; Draw settings panel TEXT (font names, labels, values)
            ;; Settings text is appended after editor text and command/agent text.
            ;; Runtime passes exact line counts so we can slice this deterministically.
            (when (pos? settings-line-count)
              (let [settings-start-line (or pre-settings-line-count editor-line-count 0)
                    settings-end-line (+ settings-start-line settings-line-count)
                    settings-start-inst (if (< settings-start-line (count line-offsets))
                                          (nth line-offsets settings-start-line)
                                          (:num-instances text-sys))
                    settings-end-inst (if (< settings-end-line (count line-offsets))
                                        (nth line-offsets settings-end-line)
                                        (:num-instances text-sys))
                    settings-draw-count (- settings-end-inst settings-start-inst)]
                (when (> settings-draw-count 0)
                  (.setPipeline pass (:pipeline text-sys))
                  (.setBindGroup pass 0 (:bind-group text-sys))
                  (.setVertexBuffer pass 0 (:instance-buffer text-sys))
                  (.draw pass 6 settings-draw-count 0 settings-start-inst)))))

          ;; Diagnostics overlay: draw when enabled and not covered by panels
          (when (and diagnostics-visible (not cmd-panel-visible) (not settings-visible) diagnostics-line-index)
            (when (< diagnostics-line-index (count line-offsets))
              (let [start-inst (nth line-offsets diagnostics-line-index)
                    next-line (inc diagnostics-line-index)
                    end-inst (if (< next-line (count line-offsets))
                               (nth line-offsets next-line)
                               (:num-instances text-sys))
                    draw-count (- end-inst start-inst)]
                (when (> draw-count 0)
                  (.setPipeline pass (:pipeline text-sys))
                  (.setBindGroup pass 0 (:bind-group text-sys))
                  (.setVertexBuffer pass 0 (:instance-buffer text-sys))
                  (.draw pass 6 draw-count 0 start-inst)))))))

    (.end pass)
    (.submit (.-queue device) #js [(.finish encoder)])))
