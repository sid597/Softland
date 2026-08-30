(ns app.client.text.painter
  "The text painter: Slug. Glyph outlines are evaluated per pixel in the
   fragment shader from curve and band textures; no atlas, exact at any zoom.
   Takes: a device and a font's curve, band, and meta data to build the system;
   a text op with its layout to position and pack glyphs; a render pass to draw
   into.
   Gives: a text system with its pipeline, instance buffer, and font textures;
   one instance per glyph in a GPU buffer; draw calls.
   Holds: a cache from a font's glyph list to its unicode lookup, and a counter
   of layout fallbacks."
  (:require [clojure.string :as str]
            [app.client.engine.color :as scene-color]
            [app.client.engine.compositor :as compositor-gpu]
            [app.client.engine.device :as device]
            [app.client.text.glyph-pack :as glyph-pack]
            [app.client.text.layout :as tl]))

(def slug-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct ContainerTransform {
    axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
    flags: u32, padding: u32,
  };
  @group(0) @binding(4) var<storage, read> containers: array<ContainerTransform>;

  struct InstanceInput {
    @location(0) rect: vec4<f32>,
    @location(1) sample_bounds: vec4<f32>,
    @location(2) inv_jac: vec4<f32>,
    @location(3) banding: vec4<f32>,
    @location(4) glyph: vec4<u32>,
    @location(5) color: vec4<f32>,
    @location(6) container_idx: u32,
  };

  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) texcoord: vec2<f32>,
    @location(1) color: vec4<f32>,
    @location(2) banding: vec4<f32>,
    @location(3) @interpolate(flat) glyph: vec4<u32>,
  };

  @vertex
  fn main(@builtin(vertex_index) v_index: u32, instance: InstanceInput) -> VertexOutput {
    var output: VertexOutput;
    var pos = vec2<f32>(0.0, 0.0);
    var sign = vec2<f32>(-1.0, -1.0);

    switch(v_index) {
      case 0u: { pos = vec2<f32>(0.0, 0.0); sign = vec2<f32>(-1.0, -1.0); }
      case 1u: { pos = vec2<f32>(1.0, 0.0); sign = vec2<f32>(1.0, -1.0); }
      case 2u: { pos = vec2<f32>(0.0, 1.0); sign = vec2<f32>(-1.0, 1.0); }
      case 3u: { pos = vec2<f32>(1.0, 0.0); sign = vec2<f32>(1.0, -1.0); }
      case 4u: { pos = vec2<f32>(1.0, 1.0); sign = vec2<f32>(1.0, 1.0); }
      default: { pos = vec2<f32>(0.0, 1.0); sign = vec2<f32>(-1.0, 1.0); }
    }

    let world_pos = vec2<f32>(instance.rect.x + (pos.x * instance.rect.z),
                              instance.rect.y + (pos.y * instance.rect.w));
    let c = containers[instance.container_idx];
    let is_screen = (c.flags & 1u) != 0u;
    let zm = select(camera.zoom, 1.0, is_screen);
    let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
    // Half-pixel conservative dilation follows both affine axes. inv_jac then
    // carries the same local delta into Slug sample space.
    let axis_scale = max(vec2<f32>(length(c.axis_x), length(c.axis_y)) * zm,
                         vec2<f32>(0.0001, 0.0001));
    let delta = sign * vec2<f32>(0.5, 0.5) / axis_scale;
    let local_pos = world_pos + delta;
    let transformed = c.translation + c.axis_x * local_pos.x + c.axis_y * local_pos.y;
    let panned = transformed * zm + pn;
    let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);

    let sample_x = mix(instance.sample_bounds.x, instance.sample_bounds.z, pos.x);
    let sample_y = mix(instance.sample_bounds.y, instance.sample_bounds.w, pos.y);
    let sample_delta = vec2<f32>(dot(delta, instance.inv_jac.xy),
                                 dot(delta, instance.inv_jac.zw));

    output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
    output.texcoord = vec2<f32>(sample_x, sample_y) + sample_delta;
    output.color = instance.color;
    output.banding = instance.banding;
    output.glyph = instance.glyph;
    return output;
  }")

(def slug-fragment-shader (str device/scene-color-wgsl "
  const kLogBandTextureWidth: u32 = 12u;
  const kMinDerivative: f32 = 1.0 / 65536.0;

  @group(0) @binding(0) var curveTexture: texture_2d<f32>;
  @group(0) @binding(1) var bandTexture: texture_2d<u32>;
  struct SlugParams { sharpness: f32, padding1: f32, padding2: f32, padding3: f32, };
  @group(0) @binding(3) var<uniform> params: SlugParams;

  fn saturate(x: f32) -> f32 {
    return clamp(x, 0.0, 1.0);
  }

  fn calc_root_code(y1: f32, y2: f32, y3: f32) -> u32 {
    let i1 = bitcast<u32>(y1) >> 31u;
    let i2 = bitcast<u32>(y2) >> 30u;
    let i3 = bitcast<u32>(y3) >> 29u;
    var shift = (i2 & 2u) | (i1 & ~2u);
    shift = (i3 & 4u) | (shift & ~4u);
    return (0x2E74u >> shift) & 0x0101u;
  }

  fn solve_horiz_poly(p12: vec4<f32>, p3: vec2<f32>) -> vec2<f32> {
    let a = p12.xy - p12.zw * 2.0 + p3;
    let b = p12.xy - p12.zw;
    let ra = 1.0 / a.y;
    let rb = 0.5 / b.y;
    let d = sqrt(max(b.y * b.y - a.y * p12.y, 0.0));
    var t1 = (b.y - d) * ra;
    var t2 = (b.y + d) * ra;
    if (abs(a.y) < kMinDerivative) {
      t1 = p12.y * rb;
      t2 = t1;
    }
    return vec2<f32>((a.x * t1 - b.x * 2.0) * t1 + p12.x,
                     (a.x * t2 - b.x * 2.0) * t2 + p12.x);
  }

  fn solve_vert_poly(p12: vec4<f32>, p3: vec2<f32>) -> vec2<f32> {
    let a = p12.xy - p12.zw * 2.0 + p3;
    let b = p12.xy - p12.zw;
    let ra = 1.0 / a.x;
    let rb = 0.5 / b.x;
    let d = sqrt(max(b.x * b.x - a.x * p12.x, 0.0));
    var t1 = (b.x - d) * ra;
    var t2 = (b.x + d) * ra;
    if (abs(a.x) < kMinDerivative) {
      t1 = p12.x * rb;
      t2 = t1;
    }
    return vec2<f32>((a.y * t1 - b.y * 2.0) * t1 + p12.y,
                     (a.y * t2 - b.y * 2.0) * t2 + p12.y);
  }

  fn calc_band_loc(glyph_loc: vec2<i32>, offset: u32) -> vec2<i32> {
    let width = 1i << kLogBandTextureWidth;
    var x = glyph_loc.x + i32(offset);
    var y = glyph_loc.y + (x >> kLogBandTextureWidth);
    x = x & (width - 1);
    return vec2<i32>(x, y);
  }

  fn calc_coverage(xcov: f32, ycov: f32, xwgt: f32, ywgt: f32) -> f32 {
    let weighted = abs(xcov * xwgt + ycov * ywgt) / max(xwgt + ywgt, kMinDerivative);
    let coverage = max(weighted, min(abs(xcov), abs(ycov)));
    return saturate(coverage);
  }

  fn slug_render(render_coord: vec2<f32>, band_transform: vec4<f32>, glyph: vec4<u32>) -> f32 {
    let ems_per_pixel = max(fwidth(render_coord), vec2<f32>(kMinDerivative, kMinDerivative));
    let pixels_per_em = 1.0 / ems_per_pixel;
    let glyph_loc = vec2<i32>(i32(glyph.x), i32(glyph.y));
    let band_max = vec2<i32>(i32(glyph.z), i32(glyph.w & 0xFFFFu));
    let band_index = clamp(vec2<i32>(floor(render_coord * band_transform.xy + band_transform.zw)),
                           vec2<i32>(0, 0),
                           band_max);

    var xcov = 0.0;
    var xwgt = 0.0;
    let hband_data = textureLoad(bandTexture, vec2<i32>(glyph_loc.x + band_index.y, glyph_loc.y), 0).xy;
    let hband_loc = calc_band_loc(glyph_loc, hband_data.y);
    for (var curve_index = 0i; curve_index < i32(hband_data.x); curve_index = curve_index + 1i) {
      let curve_loc_data = textureLoad(bandTexture, vec2<i32>(hband_loc.x + curve_index, hband_loc.y), 0).xy;
      let curve_loc = vec2<i32>(i32(curve_loc_data.x), i32(curve_loc_data.y));
      let p12 = textureLoad(curveTexture, curve_loc, 0) - vec4<f32>(render_coord, render_coord);
      let p3 = textureLoad(curveTexture, vec2<i32>(curve_loc.x + 1, curve_loc.y), 0).xy - render_coord;
      if (max(max(p12.x, p12.z), p3.x) * pixels_per_em.x < -0.5) {
        break;
      }
      let code = calc_root_code(p12.y, p12.w, p3.y);
      if (code != 0u) {
        let roots = solve_horiz_poly(p12, p3) * pixels_per_em.x;
        if ((code & 1u) != 0u) {
          xcov = xcov + saturate(roots.x + 0.5);
          xwgt = max(xwgt, saturate(1.0 - abs(roots.x) * 2.0));
        }
        if (code > 1u) {
          xcov = xcov - saturate(roots.y + 0.5);
          xwgt = max(xwgt, saturate(1.0 - abs(roots.y) * 2.0));
        }
      }
    }

    var ycov = 0.0;
    var ywgt = 0.0;
    let vband_data = textureLoad(bandTexture, vec2<i32>(glyph_loc.x + band_max.y + 1 + band_index.x, glyph_loc.y), 0).xy;
    let vband_loc = calc_band_loc(glyph_loc, vband_data.y);
    for (var curve_index = 0i; curve_index < i32(vband_data.x); curve_index = curve_index + 1i) {
      let curve_loc_data = textureLoad(bandTexture, vec2<i32>(vband_loc.x + curve_index, vband_loc.y), 0).xy;
      let curve_loc = vec2<i32>(i32(curve_loc_data.x), i32(curve_loc_data.y));
      let p12 = textureLoad(curveTexture, curve_loc, 0) - vec4<f32>(render_coord, render_coord);
      let p3 = textureLoad(curveTexture, vec2<i32>(curve_loc.x + 1, curve_loc.y), 0).xy - render_coord;
      if (max(max(p12.y, p12.w), p3.y) * pixels_per_em.y < -0.5) {
        break;
      }
      let code = calc_root_code(p12.x, p12.z, p3.x);
      if (code != 0u) {
        let roots = solve_vert_poly(p12, p3) * pixels_per_em.y;
        if ((code & 1u) != 0u) {
          ycov = ycov - saturate(roots.x + 0.5);
          ywgt = max(ywgt, saturate(1.0 - abs(roots.x) * 2.0));
        }
        if (code > 1u) {
          ycov = ycov + saturate(roots.y + 0.5);
          ywgt = max(ywgt, saturate(1.0 - abs(roots.y) * 2.0));
        }
      }
    }

    return calc_coverage(xcov, ycov, xwgt, ywgt);
  }

  @fragment
  fn main(@location(0) texcoord: vec2<f32>,
          @location(1) color: vec4<f32>,
          @location(2) banding: vec4<f32>,
          @location(3) @interpolate(flat) glyph: vec4<u32>) -> @location(0) vec4<f32> {
    let coverage = slug_render(texcoord, banding, glyph);
    return scene_color(color, saturate(coverage + params.sharpness));
  }"))

(def slug-text-instance-stride 100) ;; 24 words + container u32

(defn- create-instance-buffer [^js/GPUDevice device initial-capacity stride]
  (let [size (* initial-capacity stride)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.VERTEX
                                                              js/GPUBufferUsage.COPY_DST)}))]
    buffer))

(defn- create-slug-bind-group [^js/GPUDevice device layout curve-view band-view camera-buffer sizes-buffer containers-buffer]
  (.createBindGroup device
    (clj->js {:layout layout
              :entries [{:binding 0 :resource curve-view}
                        {:binding 1 :resource band-view}
                        {:binding 2 :resource {:buffer camera-buffer}}
                        {:binding 3 :resource {:buffer sizes-buffer}}
                        {:binding 4 :resource {:buffer containers-buffer}}]})))

(defn- create-slug-texture [^js/GPUDevice device format width height bytes bytes-per-row]
  (let [texture (.createTexture device (clj->js {:size {:width width
                                                        :height height
                                                        :depthOrArrayLayers 1}
                                                 :format format
                                                 :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                                                                js/GPUTextureUsage.COPY_DST)}))
        data (js/Uint8Array. bytes)]
    (.writeTexture (.-queue device)
                   (clj->js {:texture texture})
                   data
                   (clj->js {:bytesPerRow bytes-per-row
                             :rowsPerImage height})
                   (clj->js {:width width :height height :depthOrArrayLayers 1}))
    texture))

(defn- create-slug-font-resources [^js/GPUDevice device slug-assets]
  (let [curve-width (get-in slug-assets [:meta :curveTexture :width])
        curve-height (get-in slug-assets [:meta :curveTexture :height])
        band-width (get-in slug-assets [:meta :bandTexture :width])
        band-height (get-in slug-assets [:meta :bandTexture :height])
        curve-texture (create-slug-texture device "rgba16float"
                                           curve-width curve-height
                                           (:curve-bytes slug-assets)
                                           (* curve-width 8))
        band-texture (create-slug-texture device "rg16uint"
                                          band-width band-height
                                          (:band-bytes slug-assets)
                                          (* band-width 4))]
    (js/console.log "[RENDERER] Created Slug font resources"
                    {:curve-texture [curve-width curve-height]
                     :band-texture [band-width band-height]})
    {:curve-texture curve-texture
     :curve-texture-view (.createView curve-texture)
     :curve-texture-label "text/slug-curve"
     :band-texture band-texture
     :band-texture-view (.createView band-texture)
     :band-texture-label "text/slug-band"
     :font-resource-kind :slug}))

(defn- destroy-slug-font-resources! [text-sys]
  (when-let [^js curve-texture (:curve-texture text-sys)]
    (.destroy curve-texture))
  (when-let [^js band-texture (:band-texture text-sys)]
    (.destroy band-texture)))

(defn destroy-text-system! [text-sys]
  (when-let [^js instance-buffer (:instance-buffer text-sys)]
    (.destroy instance-buffer))
  (when (and (:owns-sizing-buffer? text-sys) (:sizes-uniform-buffer text-sys))
    (.destroy ^js (:sizes-uniform-buffer text-sys)))
  (when (:owns-font-resources? text-sys)
    (destroy-slug-font-resources! text-sys)))

(defn- init-slug-text-system
  [^js/GPUDevice device fformat camera-buffer font-assets
   & {:keys [initial-capacity label containers-buffer scene-color]
      :or {initial-capacity 10000
           label "text/content"
           scene-color scene-color/legacy-direct-color}}]
  (assert containers-buffer "init-slug-text-system requires :containers-buffer (scene-substrate P2)")
  (let [vertex-module (.createShaderModule device (clj->js {:code slug-vertex-shader}))
        fragment-module (.createShaderModule device
                                             (clj->js {:code (device/configure-scene-color-shader
                                                              slug-fragment-shader scene-color)}))
        font-resources (create-slug-font-resources device (:slug font-assets))
        instance-buffer (create-instance-buffer device initial-capacity slug-text-instance-stride)
        sizes-buffer (.createBuffer device (clj->js {:size 16
                                                     :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                    js/GPUBufferUsage.COPY_DST)}))
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "unfilterable-float"}}
                                                                     {:binding 1 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "uint"}}
                                                                     {:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                                                     {:binding 3 :visibility js/GPUShaderStage.FRAGMENT :buffer {:type "uniform"}}
                                                                     {:binding 4 :visibility js/GPUShaderStage.VERTEX :buffer {:type "read-only-storage"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout pipeline-layout
                             :vertex {:module vertex-module
                                      :entryPoint "main"
                                      :buffers [{:arrayStride slug-text-instance-stride
                                                 :stepMode "instance"
                                                 :attributes [{:shaderLocation 0 :offset 0 :format "float32x4"}
                                                              {:shaderLocation 1 :offset 16 :format "float32x4"}
                                                              {:shaderLocation 2 :offset 32 :format "float32x4"}
                                                              {:shaderLocation 3 :offset 48 :format "float32x4"}
                                                              {:shaderLocation 4 :offset 64 :format "uint32x4"}
                                                              {:shaderLocation 5 :offset 80 :format "float32x4"}
                                                              {:shaderLocation 6 :offset 96 :format "uint32"}]}]}
                             :fragment {:module fragment-module
                                        :entryPoint "main"
                                        :targets [{:format fformat
                                                   :blend (device/scene-color-blend scene-color)}]}
                             :primitive {:topology "triangle-list"}}))
        bind-group (create-slug-bind-group device bg-layout (:curve-texture-view font-resources) (:band-texture-view font-resources) camera-buffer sizes-buffer containers-buffer)]
    (js/console.log "[RENDERER] Init text system"
                    {:backend :slug
                     :label label
                     :initial-capacity initial-capacity
                     :instance-stride slug-text-instance-stride})
    (merge font-resources
           {:backend :slug
            :scene-color scene-color
            :pipeline pipeline
            :bind-group bind-group
            :bind-group-layout bg-layout
            :camera-uniform-buffer camera-buffer
            :containers-uniform-buffer containers-buffer
            :sizes-uniform-buffer sizes-buffer
            :instance-buffer instance-buffer
            :instance-stride slug-text-instance-stride
            :num-instances 0
            :frame-input/identity (js-obj)
            :!shape-rev (atom 0)
            :gpu-label label
            :owns-font-resources? true
            :owns-sizing-buffer? true})))

(defn init-text-system
  [^js/GPUDevice device fformat camera-buffer font-assets & {:as opts}]
  (apply init-slug-text-system device fformat camera-buffer font-assets
         (mapcat identity opts)))

(defn update-font-assets [^js/GPUDevice device text-sys font-assets]
  (js/console.log "[RENDERER] Update font assets"
                  {:font-id (:id font-assets)})
  (let [old-curve (:curve-texture text-sys)
         old-band (:band-texture text-sys)
         font-resources (create-slug-font-resources device (:slug font-assets))]
     (when (and old-curve (:owns-font-resources? text-sys))
       (.destroy ^js old-curve))
     (when (and old-band (:owns-font-resources? text-sys))
       (.destroy ^js old-band))
     (merge text-sys
            font-resources
            {:bind-group (create-slug-bind-group device
                                                 (:bind-group-layout text-sys)
                                                 (:curve-texture-view font-resources)
                                                 (:band-texture-view font-resources)
                                                 (:camera-uniform-buffer text-sys)
                                                 (:sizes-uniform-buffer text-sys)
                                                 (:containers-uniform-buffer text-sys))
             :owns-font-resources? true})))

(defn share-font-resources
  "Point a secondary text system at a primary text system's shared font resources."
  [target-state source-state]
  (when (:owns-font-resources? target-state)
    (destroy-slug-font-resources! target-state))
  (-> target-state
      (assoc :bind-group (:bind-group source-state)
             :owns-font-resources? false)
      (merge
       (select-keys source-state
                    [:curve-texture :curve-texture-view :curve-texture-label
                     :band-texture :band-texture-view :band-texture-label
                     :font-resource-kind]))))

(defn recreate-text-system
  [^js/GPUDevice device fformat old-text-sys font-assets]
  (let [capacity (max 1 (quot (.-size ^js (:instance-buffer old-text-sys))
                              (:instance-stride old-text-sys)))
        label (:gpu-label old-text-sys)
        camera-buffer (:camera-uniform-buffer old-text-sys)
        containers-buffer (:containers-uniform-buffer old-text-sys)
        scene-color (:scene-color old-text-sys scene-color/legacy-direct-color)]
    (js/console.log "[RENDERER] Recreate text system"
                    {:label label
                     :capacity capacity
                     :font-id (:id font-assets)})
    (destroy-text-system! old-text-sys)
    (init-text-system device fformat camera-buffer font-assets
                      :initial-capacity capacity
                      :label label
                      :containers-buffer containers-buffer
                      :scene-color scene-color)))

(defn clone-text-system
  "Create a lightweight text system clone sharing pipeline, bind-group, camera,
   and font resources with the parent. Only the instance buffer is new."
  [^js/GPUDevice device parent-text-sys initial-capacity]
  (let [stride (:instance-stride parent-text-sys)
        ib (create-instance-buffer device initial-capacity stride)]
    (assoc parent-text-sys
           :instance-buffer ib
           :instance-stride stride
           :num-instances 0
           :line-offsets nil
           :frame-input/identity (js-obj)
           :!shape-rev (atom 0)
           :gpu-label "text/chrome"
           :owns-font-resources? false
           :owns-sizing-buffer? false)))

;; --- 3. UPDATES (CPU -> GPU) ---
(defn- make-snapper [snap-step]
  (when (and snap-step (pos? snap-step))
    (fn [v] (* (Math/round (/ v snap-step)) snap-step))))

(defn- token-color [{:keys [r g b a]}]
  [(or r 1.0) (or g 1.0) (or b 1.0) (or a 1.0)])

;; first-light P1 (G1 drill finding): glyph-map is rebuilt PER LINE by
;; paint-slug-line — a whole-conversation reshape rebuilt the
;; full unicode→glyph map hundreds of times per keystroke (~23ms/keystroke,
;; CPU-profiled). The map is a pure derivation of the font's glyphs vector,
;; which only changes identity on a font swap — cache per vector
;; identity (WeakMap: no leak, old fonts' entries die with their vectors).
(defonce ^:private glyph-map-cache (js/WeakMap.))

(defn- glyph-map [glyphs]
  (or (.get glyph-map-cache glyphs)
      (let [m (reduce (fn [acc glyph]
                        (cond-> acc
                          (some? (:unicode glyph))
                          (assoc [:unicode (:unicode glyph)] glyph)

                          (some? (:index glyph))
                          (assoc [:index (:index glyph)] glyph)

                          (and (:fontId glyph) (some? (:unicode glyph)))
                          (assoc [(:fontId glyph) :unicode (:unicode glyph)] glyph)

                          (and (:fontId glyph) (some? (:index glyph)))
                          (assoc [(:fontId glyph) :index (:index glyph)] glyph)))
                      {} glyphs)]
        (when glyphs (.set glyph-map-cache glyphs m))
        m)))

(defn- painted-glyph [glyphs {:keys [glyph-id glyph-id-kind font-id]}]
  (let [kind (if (= glyph-id-kind :font-glyph-index) :index :unicode)]
    (or (get glyphs [font-id kind glyph-id])
        (get glyphs [kind glyph-id])
        (get glyphs [font-id :unicode 0xFFFD])
        (get glyphs [:unicode 0xFFFD])
        (get glyphs [font-id :index 0])
        (get glyphs [:index 0]))))

(defn- font-line-height [font-assets]
  (or (get-in font-assets [:slug :meta :metrics :lineHeight])
      1.2))

(defonce ^:private !text-layout-fallbacks (atom {}))

(defn text-layout-fallback-report []
  (merge {:ground 0 :combined-text-ops 0 :settings-panel-text 0}
         @!text-layout-fallbacks))

(defn reset-text-layout-fallbacks! [] (reset! !text-layout-fallbacks {}))

(defn- line-index-for-layout [layout-result]
  ;; Contract-T retains this index at construction. Consumers must never rebuild
  ;; it by scanning the line vector per op.
  (or (:line-index layout-result) {}))

(defn- position-text-op
  "Resolve one text op to positioned Contract-T glyphs before a paint backend
   is selected. Existing layout results survive clipping and tree translations;
   otherwise the active provider creates exactly one result here."
  [txt global-fsize font-assets char-width snap-step surface]
  (let [{:keys [text x y]} txt
        fsize (or (:size txt) global-fsize)
        snap (make-snapper snap-step)
        start-x (if snap (snap x) x)
        start-y (if snap (snap y) y)
        line-h (font-line-height font-assets)
        existing (:layout-result txt)
        fallback-surface (or (:layout/surface txt) surface :combined-text-ops)
        _ (when-not existing
            (swap! !text-layout-fallbacks update fallback-surface (fnil inc 0)))
        layout-result
        (or existing
            (tl/layout {:text text
                        :source-lines [text]
                        :provider (:layout-provider font-assets)
                        :font-size fsize
                        :char-advance (tl/legacy-char-advance-step
                                        fsize char-width snap-step)
                        :line-height (* fsize line-h)
                        :origin [start-x start-y]}))
        line (or (tl/line-by-id layout-result (:layout-line-id txt) start-y)
                 (get (line-index-for-layout layout-result)
                      (:layout-line-id txt))
                 (first (:lines layout-result)))
        [range-start range-end]
        (tl/line-source-bounds
         (or (:paint-source-range txt) (:paint-source-range line)
             (:source-range line)))
        [anchor-x anchor-y] (or (:layout-anchor txt) (:baseline line))
        dx (if existing (- (:x txt anchor-x) anchor-x) 0)
        dy (if existing (- (:y txt anchor-y) anchor-y) 0)
        range-result (tl/glyph-indexes-in-source-range
                      line
                      (if (= :header (first (:source-range line)))
                        [:header (second (:source-range line))
                         [range-start range-end]]
                        [(tl/tagged-index range-start) (tl/tagged-index range-end)]))]
    ;; The flat view: the line, its selected glyph indexes, and the op's
    ;; translation. Glyph maps are derived only by the oracle road
    ;; (`paint-slug-line` via `tl/glyph-views`); the pack door reads planes.
    {:layout/id (:layout/id layout-result)
     :style txt
     :font-size fsize
     :span-receipt (select-keys range-result [:glyph-span :visited-glyphs])
     :line line
     :indexes (:indexes range-result)
     :dx dx
     :dy dy}))

(defn- position-text
  [texts global-fsize font-assets char-width snap-step surface]
  (mapv #(position-text-op % global-fsize font-assets char-width snap-step surface)
        texts))

(defn- paint-slug-line
  [positioned font-assets]
  (let [paint-map (glyph-map (get-in font-assets [:slug :meta :glyphs]))
        res (atom [])]
    (doseq [{:keys [style font-size] :as positioned-op} positioned]
      (let [txt style
            [cr cg cb ca] (token-color txt)
            fsize font-size
            inv-size (if (pos? fsize) (/ 1.0 fsize) 0.0)
            positioned-glyphs (tl/glyph-views (:line positioned-op)
                                              (:indexes positioned-op)
                                              (:dx positioned-op)
                                              (:dy positioned-op))]
        (doseq [{:keys [character position glyph-id-kind] :as positioned-glyph}
                positioned-glyphs]
          (when-not (or (= character " ") (= glyph-id-kind :virtual/tab))
            ;; Slug consumes the positioned glyphs without owning layout.
            (let [g (painted-glyph paint-map positioned-glyph)
                    [x0 baseline-y] position]
                (when g
                  (let [sample-bounds (or (:sampleBounds g) (:planeBounds g))
                      slug (:slug g)
                      left (or (:left sample-bounds) 0.0)
                      right (or (:right sample-bounds) 0.0)
                      top (or (:top sample-bounds) 0.0)
                      bottom (or (:bottom sample-bounds) 0.0)
                      world-left (+ x0 (* fsize left))
                      world-right (+ x0 (* fsize right))
                      world-top (- baseline-y (* fsize top))
                      world-bottom (- baseline-y (* fsize bottom))]
                  (swap! res conj {:rect [world-left world-top (- world-right world-left) (- world-bottom world-top)]
                                   :sample-bounds [left top right bottom]
                                   :inv-jac [inv-size 0.0 0.0 (- inv-size)]
                                   :banding [(or (get-in slug [:banding :scaleX]) 0.0)
                                             (or (get-in slug [:banding :scaleY]) 0.0)
                                             (or (get-in slug [:banding :offsetX]) 0.0)
                                             (or (get-in slug [:banding :offsetY]) 0.0)]
                                   :glyph [(or (get-in slug [:glyphLoc :x]) 0)
                                           (or (get-in slug [:glyphLoc :y]) 0)
                                           (or (get-in slug [:bandMax :x]) 0)
                                           (or (:packedBandMeta slug) 0)]
                                   :color [cr cg cb ca]
                                   :layout/id (:layout/id positioned-op)
                                   :container (or (:container-idx txt) 0)}))))))))
    @res))

(defn shape-text [texts global-fsize font-assets & {:as opts}]
  (let [char-width (or (:char-width opts) 0.56)
        snap-step (:snap-step opts)
        positioned (position-text texts global-fsize font-assets char-width snap-step
                                  (:surface opts))]
    (paint-slug-line positioned font-assets)))

(defn- line-offsets-for [lines]
  (loop [remaining lines
         current-idx 0
         offsets []]
    (if (seq remaining)
      (let [cnt (:count (first remaining))]
        (recur (next remaining) (+ current-idx cnt) (conj offsets current-idx)))
      (vec offsets))))

(defn- ensure-text-instance-buffer
  [^js/GPUDevice device renderer-state required-size]
  (let [current-buffer (:instance-buffer renderer-state)
        current-size (.-size ^js current-buffer)
        needs-resize? (> required-size current-size)
        ;; first-light P1 (G1 drill finding): grow with 1.5× slack, never to the
        ;; exact required size. An exact-size buffer re-reallocs on EVERY
        ;; append (typing adds one instance per keystroke → GPUBuffer
        ;; create+destroy+full-upload per key), and that churn measurably
        ;; delayed websocket message delivery ~30ms/keystroke on the per-slot
        ;; text geos (cloned small, grown to exact). The content geo never hit
        ;; this only because its initial 10k-instance capacity was slack.
        alloc-size (js/Math.ceil (* 1.5 required-size))
        new-buffer (if needs-resize?
                     (.createBuffer device (clj->js {:size alloc-size
                                                     :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                    js/GPUBufferUsage.COPY_DST)}))
                     current-buffer)]
    (when needs-resize?
      (.destroy ^js current-buffer))
    new-buffer))

(defn- pack-slug-instances! [^js float-view ^js uint-view shaped-lines]
  (loop [lines shaped-lines
         global-i 0]
    (when (seq lines)
      (let [instances (:instances (first lines))]
        (loop [remaining instances
               sub-i 0]
          (when (seq remaining)
            (let [{:keys [rect sample-bounds inv-jac banding glyph color container]} (first remaining)
                  [x y w h] rect
                  [sl st sr sb] sample-bounds
                  [jx jy kx ky] inv-jac
                  [sx sy ox oy] banding
                  [gx gy gzx gwy] glyph
                  [cr cg cb ca] color
                  base (* (+ global-i sub-i) 25)]
              (aset float-view (+ base 0) x)
              (aset float-view (+ base 1) y)
              (aset float-view (+ base 2) w)
              (aset float-view (+ base 3) h)
              (aset float-view (+ base 4) sl)
              (aset float-view (+ base 5) st)
              (aset float-view (+ base 6) sr)
              (aset float-view (+ base 7) sb)
              (aset float-view (+ base 8) jx)
              (aset float-view (+ base 9) jy)
              (aset float-view (+ base 10) kx)
              (aset float-view (+ base 11) ky)
              (aset float-view (+ base 12) sx)
              (aset float-view (+ base 13) sy)
              (aset float-view (+ base 14) ox)
              (aset float-view (+ base 15) oy)
              (aset uint-view (+ base 16) gx)
              (aset uint-view (+ base 17) gy)
              (aset uint-view (+ base 18) gzx)
              (aset uint-view (+ base 19) gwy)
              (aset float-view (+ base 20) cr)
              (aset float-view (+ base 21) cg)
              (aset float-view (+ base 22) cb)
              (aset float-view (+ base 23) ca)
              (aset uint-view (+ base 24) (or container 0))
              (recur (next remaining) (inc sub-i)))))
        (recur (next lines) (+ global-i (:count (first lines))))))))

(defn pack-instances-oracle
  "The frozen map road: glyph maps → instance maps → words. Returns the
   packed instance bytes with their line offsets and count (no GPU)."
  [texts font-assets font-size stride & {:keys [char-width snap-step surface]
                                          :or {char-width 0.56}}]
  (let [shaped-lines (mapv (fn [tokens-in-line]
                             (let [instances (shape-text tokens-in-line font-size font-assets
                                                         :char-width char-width
                                                         :snap-step snap-step
                                                         :surface surface)]
                               {:instances instances
                                :count (count instances)}))
                           texts)
        actual-instances (reduce + (map :count shaped-lines))
        buffer-instance-count (max actual-instances 1)
        raw-buffer (js/ArrayBuffer. (* buffer-instance-count stride))]
    (pack-slug-instances! (js/Float32Array. raw-buffer) (js/Uint32Array. raw-buffer)
                          shaped-lines)
    {:raw-buffer raw-buffer
     :line-offsets (line-offsets-for shaped-lines)
     :num-instances actual-instances}))

(defn pack-instances-flat
  "The flat road: planes → words through the pack door, two passes (count,
   then write). Same return shape as `pack-instances-oracle`."
  [texts font-assets font-size stride & {:keys [char-width snap-step surface]
                                          :or {char-width 0.56}}]
  (let [table (glyph-pack/slug-table (get-in font-assets [:slug :meta :glyphs]))
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [ops (position-text tokens-in-line font-size font-assets
                                                      char-width snap-step surface)]
                               {:ops ops
                                :count (reduce + 0 (map #(glyph-pack/count-instances % table)
                                                        ops))}))
                           texts)
        actual-instances (reduce + (map :count shaped-lines))
        buffer-instance-count (max actual-instances 1)
        raw-buffer (js/ArrayBuffer. (* buffer-instance-count stride))]
    (glyph-pack/pack-lines! (js/Float32Array. raw-buffer) (js/Uint32Array. raw-buffer)
                            shaped-lines table)
    {:raw-buffer raw-buffer
     :line-offsets (line-offsets-for shaped-lines)
     :num-instances actual-instances}))

(defn update-text-data
  [^js/GPUDevice device renderer-state texts font-assets font-size
   & {:keys [line-height-factor line-height char-width snap-step surface]
      :or {line-height-factor 1.0 char-width 0.56}}]
  (let [line-h (or line-height (* font-size line-height-factor))
        stride (:instance-stride renderer-state)
        {:keys [raw-buffer line-offsets num-instances]}
        (pack-instances-flat texts font-assets font-size stride
                             :char-width char-width :snap-step snap-step
                             :surface surface)
        actual-instances num-instances]
    (let [upload-view (js/Uint8Array. raw-buffer)
          required-size (.-byteLength upload-view)
          new-buffer (ensure-text-instance-buffer device renderer-state required-size)]
      (.writeBuffer (.-queue device) new-buffer 0 upload-view)
      ;; Slug renders raw mathematical coverage — no sharpness bias.
      ;; The uniform exists (pipeline expects binding 3) but stays at 0.
      (when-let [sizes-buffer (:sizes-uniform-buffer renderer-state)]
        (let [sizes (js/Float32Array. #js [0.0 0.0 0.0 0.0])]
          (.writeBuffer (.-queue device) sizes-buffer 0 sizes)))
      (when (or (not= line-offsets (:line-offsets renderer-state))
                (not= (pos? actual-instances)
                      (pos? (:num-instances renderer-state 0))))
        (swap! (:!shape-rev renderer-state) inc))
      (assoc renderer-state
             :instance-buffer new-buffer
             :num-instances actual-instances
             :line-offsets line-offsets
             :line-height line-h))))

(defn- contiguous-state-runs [rows]
  (loop [remaining rows offset 0 result []]
    (if-let [row (first remaining)]
      (let [same (take-while #(= row %) remaining)
            n (count same)]
        (recur (drop n remaining) (+ offset n)
               (conj result {:clip (:clip row) :container (:container row)
                             :offset offset :count n})))
      result)))

(defn- text-clip-runs [geo line-clips]
  (let [offsets (:line-offsets geo)
        line-count (count offsets)
        rows (mapv (fn [line-index]
                     (let [start (nth offsets line-index)
                           end (if (< (inc line-index) line-count)
                                 (nth offsets (inc line-index))
                                 (:num-instances geo))
                           clip-row (first (filter :clip
                                                   (get line-clips line-index)))]
                       {:clip (:clip clip-row) :container (:container clip-row)
                        :offset start :count (- end start)}))
                   (range line-count))]
    (->> rows
         (partition-by #(select-keys % [:clip :container]))
         (mapv (fn [group]
                 (let [first-row (first group)]
                   {:clip (:clip first-row) :container (:container first-row)
                    :offset (:offset first-row)
                    :count (reduce + (map :count group))}))))))

(defn draw-instances!
  "Issue one instanced quad draw, or its per-clip sub-draws, on an open pass.
   A projected clip that scissors to nothing suppresses its draw."
  [^js pass {:keys [pipeline bind-group buffer vertex-count instance-count
                    first-vertex first-instance scissor sub-draws]}
   attachment-size]
  (.setPipeline pass pipeline)
  (when bind-group (.setBindGroup pass 0 bind-group))
  (if (seq sub-draws)
    (doseq [{:keys [clip buffer vertex-count instance-count first-vertex
                    first-instance]} sub-draws]
      (when (compositor-gpu/apply-scissor! pass clip attachment-size)
        (when buffer (.setVertexBuffer pass 0 buffer))
        (.draw pass vertex-count instance-count first-vertex first-instance)))
    (when (compositor-gpu/apply-scissor!
           pass
           (when scissor {:x (nth scissor 0) :y (nth scissor 1)
                          :w (nth scissor 2) :h (nth scissor 3)})
           attachment-size)
      (when buffer (.setVertexBuffer pass 0 buffer))
      (.draw pass vertex-count instance-count first-vertex first-instance))))

(defn draw-text-system!
  "Paint every shaped instance a text system currently holds."
  [^js pass text-sys attachment-size]
  (draw-instances! pass {:pipeline (:pipeline text-sys)
                         :bind-group (:bind-group text-sys)
                         :buffer (:instance-buffer text-sys)
                         :vertex-count 6
                         :instance-count (:num-instances text-sys 0)
                         :first-vertex 0 :first-instance 0}
                   attachment-size))
