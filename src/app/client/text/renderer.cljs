(ns app.client.text.renderer
  "Upload glyph instances and evaluate outlines per pixel.

   Input: device/font outline assets/shared buffers, text items or retained
   layouts, and an open render pass. Output: a resource-owning text system,
   packed instance uploads and quad draws. Text system maps carry ownership
   flags for shared font and sizing resources; functions return replacement
   maps when handles change. There is no automatic per-frame equality cache
   in update-text-data: each call packs and uploads.

   Folder map: README.md."
  (:require [clojure.string :as str]
            [app.client.engine.color :as scene-color]
            [app.client.engine.compositor :as compositor-gpu]
            [app.client.engine.device :as device]
            [app.client.text.glyph-pack :as glyph-pack]
            [app.client.text.layout :as tl]))

;; Slug textures encode curve/band outlines rather than raster glyph images.
;; Finite texture formats, float arithmetic, derivative floors and affine
;; hull approximations bound the numerical result.
;;
;; Vertex main: Vertex index and 25-word instance, transforms/camera → clip
;; position, sample coordinate, color/banding/glyph metadata.. Dilates hull
;; along affine axes and adjusts sample coordinates through inverse Jacobian.
;; Arbitrary-shear coverage needs visual evidence.
(def slug-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct GroupTransform {
    axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
    flags: u32, padding: u32,
  };
  @group(0) @binding(4) var<storage, read> groups: array<GroupTransform>;

  struct InstanceInput {
    @location(0) rect: vec4<f32>,
    @location(1) sample_bounds: vec4<f32>,
    @location(2) inv_jac: vec4<f32>,
    @location(3) banding: vec4<f32>,
    @location(4) glyph: vec4<u32>,
    @location(5) color: vec4<f32>,
    @location(6) group_buffer_index: u32,
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
    let c = groups[instance.group_buffer_index];
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

;; saturate: Scalar → [0,1] clamp..
;;
;; calc_root_code: Three curve coordinate signs → encoded root crossing
;; classes.. Float sign bits and lookup constant. Use only with the
;; corresponding polynomial algorithm.
;;
;; solve_horiz_poly: Relative quadratic control points → horizontal crossing
;; coordinates.. Quadratic roots with near-linear fallback. Derivative
;; epsilon defines numerical behavior.
;;
;; solve_vert_poly: Same control points → vertical crossing coordinates..
;; Axis-swapped solver.
;;
;; calc_band_loc: Glyph texture origin and offset → wrapped texture
;; coordinate.. Bit arithmetic assuming band texture width 4096. Shader width
;; is fixed, while texture dimensions come from metadata.
;;
;; calc_coverage: Horizontal/vertical coverage and weights → bounded combined
;; coverage.. Weighted estimate plus minimum-axis safeguard. Describes
;; implemented estimator; no exactness verdict from source.
;;
;; slug_render: Sample coordinate, band transform, glyph metadata →
;; coverage.. Derivative-based pixel scale, band lookup, curve-root
;; accumulation on two axes. Per-pixel loops depend on selected curve bands.
;;
;; Fragment main: Interpolated sample/color/banding/glyph → scene-mode RGBA..
;; Evaluates coverage, adds sizing-uniform sharpness, clamps and applies
;; shared color helper. Update path sets sharpness to zero.
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

(def slug-text-instance-stride 100) ;; 24 words + group u32

(defn- create-instance-buffer
  "Device/capacity/stride → GPU instance buffer.

   Direct vertex/copy allocation. Caller must supply suitable capacity."
  [^js/GPUDevice device initial-capacity stride]
  (let [size (* initial-capacity stride)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.VERTEX
                                                              js/GPUBufferUsage.COPY_DST)}))]
    buffer))

(defn- create-slug-bind-group
  "Device/layout/outline views/camera/sizing/groups → bind group.

   Five explicit resources."
  [^js/GPUDevice device layout curve-view band-view camera-buffer sizes-buffer groups-buffer]
  (.createBindGroup device
    (clj->js {:layout layout
              :entries [{:binding 0 :resource curve-view}
                        {:binding 1 :resource band-view}
                        {:binding 2 :resource {:buffer camera-buffer}}
                        {:binding 3 :resource {:buffer sizes-buffer}}
                        {:binding 4 :resource {:buffer groups-buffer}}]})))

(defn- create-slug-texture
  "Device/format/dimensions/bytes/row stride → uploaded texture.

   Allocates then writes bytes. Metadata/bytes consistency is trusted."
  [^js/GPUDevice device format width height bytes bytes-per-row]
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

(defn- create-slug-font-resources
  "Device/Slug assets → curve/band textures, views and labels.

   RGBA16F curves and RG16UINT bands from metadata dimensions. Intended for
   the declared asset format; partial failure cleanup is absent here."
  [^js/GPUDevice device slug-assets]
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

(defn- destroy-slug-font-resources!
  "Text state → destroys present curve/band textures.

   Nil-safe explicit release."
  [text-sys]
  (when-let [^js curve-texture (:curve-texture text-sys)]
    (.destroy curve-texture))
  (when-let [^js band-texture (:band-texture text-sys)]
    (.destroy band-texture)))

(defn destroy-text-system!
  "Text state → release results; destroys instance buffer and only owned
   shared resources.

   Ownership flags prevent clone double-destruction. Caller must manage
   parent/clone lifetime."
  [text-sys]
  (when-let [^js instance-buffer (:instance-buffer text-sys)]
    (.destroy instance-buffer))
  (when (and (:owns-sizing-buffer? text-sys) (:sizes-uniform-buffer text-sys))
    (.destroy ^js (:sizes-uniform-buffer text-sys)))
  (when (:owns-font-resources? text-sys)
    (destroy-slug-font-resources! text-sys)))

(defn- init-slug-text-system
  "Device/format/camera/assets/options → complete Slug system.

   Creates textures, instance/sizing buffers, pipeline/layout/binding; marks
   ownership. Requires groups buffer, trusts complete Slug assets."
  [^js/GPUDevice device fformat camera-buffer font-assets
   & {:keys [initial-capacity label groups-buffer scene-color]
      :or {initial-capacity 10000
           label "text/content"
           scene-color scene-color/legacy-direct-color}}]
  (assert groups-buffer "init-slug-text-system requires :groups-buffer (scene-substrate P2)")
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
        bind-group (create-slug-bind-group device bg-layout (:curve-texture-view font-resources) (:band-texture-view font-resources) camera-buffer sizes-buffer groups-buffer)]
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
            :groups-uniform-buffer groups-buffer
            :sizes-uniform-buffer sizes-buffer
            :instance-buffer instance-buffer
            :instance-stride slug-text-instance-stride
            :num-instances 0
            :gpu-label label
            :owns-font-resources? true
            :owns-sizing-buffer? true})))

(defn init-text-system
  "Device, output format, camera buffer, font assets and keyword options →
   Slug system.

   Single backend wrapper forwarding keyword options. Intended for current
   one-backend tree."
  [^js/GPUDevice device fformat camera-buffer font-assets & {:as opts}]
  (apply init-slug-text-system device fformat camera-buffer font-assets
         (mapcat identity opts)))

(defn update-font-assets
  "Device/state/new assets → replacement state with new textures/bindings;
   destroys previously owned textures.

   Rebinds existing layout/shared buffers. Callers holding clones must
   update their bindings when old font textures are destroyed."
  [^js/GPUDevice device text-sys font-assets]
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
                                                 (:groups-uniform-buffer text-sys))
             :owns-font-resources? true})))

(defn share-font-resources
  "Target/source states → target using source textures and whole bind group;
   destroys target's owned font textures.

   Shares more than fonts because bind group also contains source
   camera/sizing/groups. Safe only when that shared binding context is
   intended."
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
  "Device/format/old state/assets → newly initialized state after old
   teardown.

   Retains capacity, old camera/groups references and color mode. This is
   not a complete device-loss migration because shared buffers are reused."
  [^js/GPUDevice device fformat old-text-sys font-assets]
  (let [capacity (max 1 (quot (.-size ^js (:instance-buffer old-text-sys))
                              (:instance-stride old-text-sys)))
        label (:gpu-label old-text-sys)
        camera-buffer (:camera-uniform-buffer old-text-sys)
        groups-buffer (:groups-uniform-buffer old-text-sys)
        scene-color (:scene-color old-text-sys scene-color/legacy-direct-color)]
    (js/console.log "[RENDERER] Recreate text system"
                    {:label label
                     :capacity capacity
                     :font-id (:id font-assets)})
    (destroy-text-system! old-text-sys)
    (init-text-system device fformat camera-buffer font-assets
                      :initial-capacity capacity
                      :label label
                      :groups-buffer groups-buffer
                      :scene-color scene-color)))

(defn clone-text-system
  "Device/parent/capacity → state sharing pipeline/bindings/font/sizing with
   new instance buffer.

   Marks shared-resource ownership false. Intended for same-device
   shared-resource clones; parent lifetime remains a dependency."
  [^js/GPUDevice device parent-text-sys initial-capacity]
  (let [stride (:instance-stride parent-text-sys)
        ib (create-instance-buffer device initial-capacity stride)]
    (assoc parent-text-sys
           :instance-buffer ib
           :instance-stride stride
           :num-instances 0
           :line-offsets nil
           :gpu-label "text/chrome"
           :owns-font-resources? false
           :owns-sizing-buffer? false)))

;; --- 3. UPDATES (CPU -> GPU) ---
(defn- make-snapper
  "Optional positive step → rounding function or nil.

   Local coordinate snapping."
  [snap-step]
  (when (and snap-step (pos? snap-step))
    (fn [v] (* (Math/round (/ v snap-step)) snap-step))))

(defn- font-line-height
  "Font assets → declared Slug line-height factor or 1.2."
  [font-assets]
  (or (get-in font-assets [:slug :meta :metrics :lineHeight])
      1.2))

(defn- sum-fallbacks
  "Rows → sum of layout fallback counts.

   Counts newly built layout results; font-fallback choices are not
   included."
  [rows]
  (reduce + 0 (map :fallbacks rows)))

(defn- sum-unresolved-glyphs
  "Rows → sum of direct glyph-resolution misses.

   Reduction."
  [rows]
  (reduce + 0 (map :unresolved-glyphs rows)))

(defn- map-has?
  "JS map/key → defined entry?

   Missing/zero distinction."
  [^js m key]
  (and m (not (undefined? (.get m key)))))

(defn- font-map-has?
  "Nested maps/font/key → font-specific entry present?

   Guarded lookup."
  [^js outer font-id key]
  (when (and outer font-id)
    (let [inner (.get outer font-id)]
      (and (not (undefined? inner)) (map-has? inner key)))))

(defn- directly-resolved-glyph?
  "Table/shaped flag/font/ID → direct font/global glyph available?

   Uses glyph indexes for shaped text, Unicode otherwise. Intended for
   diagnostics; replacement glyphs do not count as direct success."
  [{:keys [by-font-index by-index by-font-unicode by-unicode]}
   shaped? font-id glyph-id]
  (if shaped?
    (or (font-map-has? by-font-index font-id glyph-id)
        (map-has? by-index glyph-id))
    (or (font-map-has? by-font-unicode font-id glyph-id)
        (map-has? by-unicode glyph-id))))

(defn- single-space-cluster?
  "Text/source endpoints → exactly one space?

   Clamped code-unit check. Mirrors packer skip rule."
  [text start end]
  (let [length (.-length text)
        start (min length start)
        end (min length end)]
    (and (= 1 (- end start)) (= 32 (.charCodeAt text start)))))

(defn- count-unresolved-glyphs
  "Positioned item/table → count lacking direct glyph metadata, excluding
   tabs/spaces.

   Primitive plane walk. Separate diagnostic pass adds work alongside count
   and packing passes."
  [{:keys [line indexes dx dy]} table]
  (let [text (str (or (:text line) ""))
        !count (volatile! 0)]
    (tl/pack-glyphs!
     line indexes dx dy
     (fn [_ glyph-id shaped? tab? font-id _ _ cluster-start cluster-end]
       (when (and (not tab?)
                  (not (single-space-cluster? text cluster-start cluster-end))
                  (not (directly-resolved-glyph? table shaped? font-id glyph-id)))
         (vswap! !count inc))))
    @!count))

(defn- line-index-for-layout
  "Result → retained line index or empty map.

   No reconstruction."
  [layout-result]
  ;; The layout retains this index at construction. Consumers must never rebuild
  ;; it by scanning the line vector per draw-item.
  (or (:line-index layout-result) {}))

(defn- position-text-draw-item
  "Text item/global size/assets/snap step → positioned flat draw item plus
   fallback count.

   Reuses supplied layout or builds one, resolves line/source range, carries
   translation. Nearest-baseline fallback can choose a different line when
   ID is missing, and local x/y are required by snapping."
  [txt global-fsize font-assets snap-step]
  (let [{:keys [text x y]} txt
        fsize (or (:size txt) global-fsize)
        snap (make-snapper snap-step)
        start-x (if snap (snap x) x)
        start-y (if snap (snap y) y)
        line-h (font-line-height font-assets)
        existing (:layout-result txt)
        layout-result
        (or existing
            (tl/layout {:text text
                        :source-lines [text]
                        :provider (:layout-provider font-assets)
                        :font-size fsize
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
    ;; The line, its selected glyph indexes, and the draw-item's translation
    ;; are the packer's direct view of the layout planes.
    {:draw-item {:layout/id (:layout/id layout-result)
          :style txt
          :font-size fsize
          :container (:container txt)
          :line line
          :indexes (:indexes range-result)
          :dx dx
          :dy dy}
     :fallbacks (if existing 0 1)}))

(defn- position-text
  "Text items/size/assets/snap → positioned items and aggregate fallback
   count.

   Applies position-text-draw-item to each input and sums its layout
   fallback count."
  [texts global-fsize font-assets snap-step]
  (let [rows (mapv #(position-text-draw-item % global-fsize font-assets snap-step)
                   texts)]
    {:draw-items (mapv :draw-item rows)
     :fallbacks (sum-fallbacks rows)}))

(defn- line-offsets-for
  "Per-line instance counts → prefix offsets.

   Linear scan."
  [lines]
  (loop [remaining lines
         current-idx 0
         offsets []]
    (if (seq remaining)
      (let [cnt (:count (first remaining))]
        (recur (next remaining) (+ current-idx cnt) (conj offsets current-idx)))
      (vec offsets))))

(defn- ensure-text-instance-buffer
  "Device/state/required bytes → retained or larger buffer; destroys old if
   grown.

   Allocates 1.5× required size. Intended for append slack; Growth retains
   spare capacity to reduce repeated allocation."
  [^js/GPUDevice device renderer-state required-size]
  (let [current-buffer (:instance-buffer renderer-state)
        current-size (.-size ^js current-buffer)
        needs-resize? (> required-size current-size)
        ;; first-light P1 (G1 drill finding): grow with 1.5× slack, never to the
        ;; exact required size. An exact-size buffer re-reallocs on EVERY
        ;; append (typing adds one instance per keystroke → GPUBuffer
        ;; create+destroy+full-upload per key), and that churn measurably
        ;; delayed websocket message delivery ~30ms/keystroke on the per-entity
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

(defn pack-instances-flat
  "Grouped text items/assets/size/stride/options → packed ArrayBuffer, line
   offsets/counts and diagnostics.

   Positions items, counts drawable glyphs, counts direct misses, packs.
   Intended for direct column route; effectively three glyph traversals
   including diagnostics, the count/write pair excludes the diagnostic
   traversal."
  [texts font-assets font-size stride & {:keys [snap-step world-transforms]}]
  (let [table (glyph-pack/slug-table (get-in font-assets [:slug :meta :glyphs]))
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [{:keys [draw-items fallbacks]}
                                   (position-text tokens-in-line font-size font-assets snap-step)]
                               {:draw-items draw-items
                                :count (reduce + 0 (map #(glyph-pack/count-instances % table)
                                                        draw-items))
                                :fallbacks fallbacks
                                :unresolved-glyphs
                                (reduce + 0 (map #(count-unresolved-glyphs % table)
                                                 draw-items))}))
                           texts)
        actual-instances (reduce + (map :count shaped-lines))
        buffer-instance-count (max actual-instances 1)
        raw-buffer (js/ArrayBuffer. (* buffer-instance-count stride))]
    (glyph-pack/pack-lines! (js/Float32Array. raw-buffer)
                            (js/Uint32Array. raw-buffer)
                            shaped-lines table world-transforms)
    {:raw-buffer raw-buffer
     :line-offsets (line-offsets-for shaped-lines)
     :num-instances actual-instances
     :fallbacks (sum-fallbacks shaped-lines)
     :unresolved-glyphs (sum-unresolved-glyphs shaped-lines)}))

(defn update-text-data
  "Device/state/grouped text/assets/size/options → updated state; uploads
   packed buffer and zero sharpness uniform.

   Packs every call and grows if needed. No unchanged-data short circuit;
   line-height options affect returned bookkeeping, while fallback layout
   uses font metadata line height."
  [^js/GPUDevice device renderer-state texts font-assets font-size
   & {:keys [line-height-factor line-height snap-step world-transforms]
      :or {line-height-factor 1.0}}]
  (let [line-h (or line-height (* font-size line-height-factor))
        stride (:instance-stride renderer-state)
        {:keys [raw-buffer line-offsets num-instances fallbacks unresolved-glyphs]}
        (pack-instances-flat texts font-assets font-size stride
                             :snap-step snap-step :world-transforms world-transforms)
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
      (assoc renderer-state
             :instance-buffer new-buffer
             :num-instances actual-instances
             :line-offsets line-offsets
             :fallbacks fallbacks
             :unresolved-glyphs unresolved-glyphs
             :line-height line-h))))

(defn- contiguous-state-runs
  "Equal-state rows → clip/group runs with offsets/counts.

   Sequential equal-run scan. Serves as utility; no call in this file's
   current draw path."
  [rows]
  (loop [remaining rows offset 0 result []]
    (if-let [row (first remaining)]
      (let [same (take-while #(= row %) remaining)
            n (count same)]
        (recur (drop n remaining) (+ offset n)
               (conj result {:clip (:clip row) :group (:group row)
                             :offset offset :count n})))
      result)))

(defn- text-clip-runs
  "Geometry offsets and per-line clip rows → merged adjacent clip/group
   instance runs.

   Picks first clipped item per line, then merges states. Assumes one
   relevant clip/group per line; no call in this file's current draw path."
  [geo line-clips]
  (let [offsets (:line-offsets geo)
        line-count (count offsets)
        rows (mapv (fn [line-index]
                     (let [start (nth offsets line-index)
                           end (if (< (inc line-index) line-count)
                                 (nth offsets (inc line-index))
                                 (:num-instances geo))
                           clip-row (first (filter :clip
                                                   (get line-clips line-index)))]
                       {:clip (:clip clip-row) :group (:container clip-row)
                        :offset start :count (- end start)}))
                   (range line-count))]
    (->> rows
         (partition-by #(select-keys % [:clip :group]))
         (mapv (fn [group]
                 (let [first-row (first group)]
                   {:clip (:clip first-row) :group (:group first-row)
                    :offset (:offset first-row)
                    :count (reduce + (map :count group))}))))))

(defn draw-instances!
  "Open pass/draw descriptor/attachment size → encoded draw(s).

   Sets pipeline/binding, applies explicit scissors, skips empty clips,
   handles optional subdraws. Depends on compositor's scissor normalization."
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
  "Open pass/text system/attachment size → full-system glyph draw.

   Six vertices per instance through generic helper. Does not apply the
   private per-line clip-run helpers automatically."
  [^js pass text-sys attachment-size]
  (draw-instances! pass {:pipeline (:pipeline text-sys)
                         :bind-group (:bind-group text-sys)
                         :buffer (:instance-buffer text-sys)
                         :vertex-count 6
                         :instance-count (:num-instances text-sys 0)
                         :first-vertex 0 :first-instance 0}
                   attachment-size))
