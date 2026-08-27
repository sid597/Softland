(ns app.client.substrate.webgpu.renderer
  (:require [clojure.string :as str]
            [app.client.substrate.image-material :as image-material]
            [app.client.substrate.scene-color :as scene-color]
            [app.client.substrate.webgpu.buffer-pool :as buffer-pool]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.substrate.webgpu.compositor-gpu :as compositor-gpu]
            [app.client.substrate.webgpu.region3d-gpu :as region3d-gpu]
            [app.client.workspace.text-layout :as tl]))

(def ^:private scene-color-mode-declaration
  "const kSceneColorLinearPremultiplied: bool = false;")

(def ^:private scene-color-wgsl
  (str scene-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
       "}\n"
       "fn scene_color(straight: vec4<f32>, coverage: f32) -> vec4<f32> {\n"
       "  if (!kSceneColorLinearPremultiplied) {\n"
       "    return vec4<f32>(straight.rgb, straight.a * coverage);\n"
       "  }\n"
       "  let alpha = clamp(straight.a * coverage, 0.0, 1.0);\n"
       "  let linear = vec3<f32>(srgb_channel_to_linear(straight.r),\n"
       "                         srgb_channel_to_linear(straight.g),\n"
       "                         srgb_channel_to_linear(straight.b));\n"
       "  return vec4<f32>(linear * alpha, alpha);\n"
       "}\n"))

(defn- configure-scene-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader scene-color-mode-declaration
                 "const kSceneColorLinearPremultiplied: bool = true;")
    shader))

(defn- scene-color-blend [color]
  (let [{[color-src color-dst] :color
         [alpha-src alpha-dst] :alpha} (:blend color)]
    {:color {:srcFactor (name color-src) :dstFactor (name color-dst)}
     :alpha {:srcFactor (name alpha-src) :dstFactor (name alpha-dst)}}))

;; IMAGE-ATOM T3: the candidate samples through an sRGB texture view (the one
;; ingress decode) and writes a linear-premultiplied value to an *-srgb target
;; (the one presentation encode).  The legacy seam samples an unorm view and
;; returns encoded straight RGB, so it never half-converts.
(def ^:private image-color-mode-declaration
  "const kImageLinearPremultiplied: bool = false;")

(defn- configure-image-color-shader [shader color]
  (if (:enabled? color)
    (str/replace shader image-color-mode-declaration
                 "const kImageLinearPremultiplied: bool = true;")
    shader))

(def image-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  struct ContainerTransform {
    axis_x: vec2<f32>, axis_y: vec2<f32>, translation: vec2<f32>,
    flags: u32, padding: u32,
  };
  @group(0) @binding(3) var<storage, read> containers: array<ContainerTransform>;
  struct InstanceInput {
    @location(0) rect: vec4<f32>,
    @location(1) uv_bounds: vec4<f32>,
    @location(2) tint: vec4<f32>,
    @location(3) container_idx: u32,
  };
  struct VertexOutput {
    @builtin(position) position: vec4<f32>,
    @location(0) uv: vec2<f32>,
    @location(1) tint: vec4<f32>,
    @location(2) edge_pos: vec2<f32>,
    @location(3) edge_size: vec2<f32>,
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
    let c = containers[instance.container_idx];
    let is_screen = (c.flags & 1u) != 0u;
    let zm = select(camera.zoom, 1.0, is_screen);
    let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
    let axis_scale = max(vec2<f32>(length(c.axis_x), length(c.axis_y)) * zm,
                         vec2<f32>(0.0001, 0.0001));
    // Ramped Cg support: expand the raster hull by half a screen pixel while
    // keeping the mathematical quad at edge_pos 0/edge_size (IMAGE-ATOM G4).
    let sign = pos * 2.0 - vec2<f32>(1.0, 1.0);
    let local_delta = sign * vec2<f32>(0.5, 0.5) / axis_scale;
    let local_pos = instance.rect.xy + pos * instance.rect.zw + local_delta;
    let world_pos = c.translation + c.axis_x * local_pos.x + c.axis_y * local_pos.y;
    let panned = world_pos * zm + pn;
    let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
    output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
    output.uv = mix(instance.uv_bounds.xy, instance.uv_bounds.zw, pos);
    output.tint = instance.tint;
    output.edge_pos = (pos * instance.rect.zw + local_delta) * axis_scale;
    output.edge_size = instance.rect.zw * axis_scale;
    return output;
  }")

(def image-fragment-shader
  (str image-color-mode-declaration "\n"
       "fn srgb_channel_to_linear(v: f32) -> f32 {\n"
       "  if (v <= 0.04045) { return v / 12.92; }\n"
       "  return pow((v + 0.055) / 1.055, 2.4);\n"
       "}\n"
       "@group(0) @binding(0) var image_sampler: sampler;\n"
       "@group(0) @binding(1) var image_texture: texture_2d<f32>;\n"
       "@fragment\n"
       "fn main(@location(0) uv: vec2<f32>,\n"
       "        @location(1) tint: vec4<f32>,\n"
       "        @location(2) edge_pos: vec2<f32>,\n"
       "        @location(3) edge_size: vec2<f32>) -> @location(0) vec4<f32> {\n"
       "  let edge_distance = min(min(edge_pos.x, edge_size.x - edge_pos.x),\n"
       "                          min(edge_pos.y, edge_size.y - edge_pos.y));\n"
       "  let cg = clamp(edge_distance + 0.5, 0.0, 1.0);\n"
       "  let sampled = textureSample(image_texture, image_sampler, uv);\n"
       "  if (!kImageLinearPremultiplied) {\n"
       "    return vec4<f32>(sampled.rgb * tint.rgb, sampled.a * tint.a * cg);\n"
       "  }\n"
       "  let tint_linear = vec3<f32>(srgb_channel_to_linear(tint.r),\n"
       "                              srgb_channel_to_linear(tint.g),\n"
       "                              srgb_channel_to_linear(tint.b));\n"
       "  let alpha = clamp(sampled.a * tint.a * cg, 0.0, 1.0);\n"
       "  return vec4<f32>(sampled.rgb * tint_linear * alpha, alpha);\n"
       "}\n"))

(def ^:private image-mip-vertex-shader "
  struct Output { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32>, };
  @vertex fn main(@builtin(vertex_index) index: u32) -> Output {
    var positions = array<vec2<f32>, 3>(vec2<f32>(-1.0, -1.0),
                                        vec2<f32>(3.0, -1.0),
                                        vec2<f32>(-1.0, 3.0));
    var out: Output;
    out.position = vec4<f32>(positions[index], 0.0, 1.0);
    out.uv = positions[index] * vec2<f32>(0.5, -0.5) + vec2<f32>(0.5, 0.5);
    return out;
  }")

(def ^:private image-mip-fragment-shader "
  @group(0) @binding(0) var mip_sampler: sampler;
  @group(0) @binding(1) var mip_source: texture_2d<f32>;
  @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
    return textureSample(mip_source, mip_sampler, uv);
  }")

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

(def slug-fragment-shader (str scene-color-wgsl "
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

;; --- 2. INITIALIZATION ---

(def image-instance-stride image-material/image-instance-stride)
(def slug-text-instance-stride 100) ;; 24 words + container u32

;; --- W2-A/Q8: shared compact affine transport ------------------------------
;; One 32-byte storage entry per LIVE transform slot:
;; [axis-x.xy, axis-y.xy, translation.xy, flags:u32, pad:u32]. Semantic cids do
;; not index this table; containers/effective assigns compact stable slots.
;; Q8 priced 1,024 / 4,096 / 16,384 entries, and the largest measured tier is
;; the production allocation. Growing later rebinds the same storage scheme; it
;; is not another representation migration after atom multiplication.
(def affine-entry-bytes 32)
(def max-transform-nodes 16384)

(defn create-containers-buffer
  "Create the shared Q8 affine storage buffer and write identity slot 0.
   Shared across all four transform-consuming pipelines like the camera."
  [^js/GPUDevice device tracker]
  (let [size (* max-transform-nodes affine-entry-bytes)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.STORAGE
                                                              js/GPUBufferUsage.COPY_DST)}))
        identity0 (js/Float32Array. #js [1.0 0.0 0.0 1.0 0.0 0.0 0.0 0.0])]
    (gpu-budget/register-buffer! tracker buffer "containers/affine-storage" size
                                 :active-bytes affine-entry-bytes)
    (.writeBuffer (.-queue device) buffer 0 identity0)
    buffer))

(defn write-containers!
  "Upload containers/effective through compact :transport-slot values. Sparse
   semantic cids never allocate holes. Returns a machine receipt used by the
   1,024/4,096/16,384 Q8 verifier."
  [^js/GPUDevice device ^js containers-buffer effective]
  (let [entries (vals effective)
        slots (map :transport-slot entries)
        _ (when (some nil? slots)
            (throw (ex-info "Affine transport entry lacks :transport-slot"
                            {:missing (count (filter nil? slots))})))
        _ (when-not (= (count slots) (count (set slots)))
            (throw (ex-info "Affine transport slots must be unique"
                            {:slots slots})))
        max-slot (if (seq slots) (apply max slots) 0)
        entry-count (inc max-slot)
        _ (when (> entry-count max-transform-nodes)
            (throw (ex-info "Live affine transport exceeds the Q8 capacity"
                            {:entries entry-count :max max-transform-nodes})))
        raw (js/ArrayBuffer. (* entry-count affine-entry-bytes))
        floats (js/Float32Array. raw)
        uints (js/Uint32Array. raw)]
    ;; Holes can only occur in a hand-built effective map; make them identity,
    ;; never a singular zero matrix. Normal registries allocate densely.
    (dotimes [slot entry-count]
      (let [base (* slot 8)]
        (aset floats (+ base 0) 1.0)
        (aset floats (+ base 3) 1.0)))
    (doseq [{:keys [affine flags transport-slot]} entries]
      (let [[a b c d tx ty] affine
            base (* transport-slot 8)]
        (when-not (= 6 (count affine))
          (throw (ex-info "Affine transport requires [a b c d tx ty]"
                          {:affine affine :transport-slot transport-slot})))
        (aset floats (+ base 0) a)
        (aset floats (+ base 1) b)
        (aset floats (+ base 2) c)
        (aset floats (+ base 3) d)
        (aset floats (+ base 4) tx)
        (aset floats (+ base 5) ty)
        (aset uints (+ base 6) (or flags 0))
        (aset uints (+ base 7) 0)))
    (.writeBuffer (.-queue device) containers-buffer 0 (js/Uint8Array. raw))
    {:entries entry-count
     :bytes (* entry-count affine-entry-bytes)
     :max-slot max-slot
     :capacity max-transform-nodes}))

;; --- Image atom resource system --------------------------------------------

(defn- pack-image-instance [image-op]
  (let [words (image-material/instance-words
               {:x (:x image-op) :y (:y image-op)
                :w (:w image-op) :h (:h image-op)
                :uv (:image/resolved-uv image-op)
                :tint (:image/tint image-op)
                :opacity (:image/opacity image-op)
                :container-idx (:container-idx image-op)})
        data (js/Float32Array. image-material/image-instance-words)
        uints (js/Uint32Array. (.-buffer data))]
    (dotimes [index 12]
      (aset data index (nth words index)))
    (aset uints 12 (nth words 12))
    data))

(defn- create-image-bind-group
  [^js device layout sampler texture-view camera-buffer containers-buffer]
  (.createBindGroup
   device
   (clj->js {:layout layout
             :entries [{:binding 0 :resource sampler}
                       {:binding 1 :resource texture-view}
                       {:binding 2 :resource {:buffer camera-buffer}}
                       {:binding 3 :resource {:buffer containers-buffer}}]})))

(defn- create-image-pipeline
  [^js device fformat bind-layout scene-color]
  (let [vertex-module (.createShaderModule device
                                          (clj->js {:code image-vertex-shader}))
        fragment-module (.createShaderModule
                         device
                         (clj->js {:code (configure-image-color-shader
                                         image-fragment-shader scene-color)}))
        layout (.createPipelineLayout
                device (clj->js {:bindGroupLayouts [bind-layout]}))]
    (.createRenderPipeline
     device
     (clj->js
      {:layout layout
       :vertex {:module vertex-module
                :entryPoint "main"
                :buffers [{:arrayStride image-instance-stride
                           :stepMode "instance"
                           :attributes [{:shaderLocation 0 :offset 0
                                         :format "float32x4"}
                                        {:shaderLocation 1 :offset 16
                                         :format "float32x4"}
                                        {:shaderLocation 2 :offset 32
                                         :format "float32x4"}
                                        {:shaderLocation 3 :offset 48
                                         :format "uint32"}]}]}
       :fragment {:module fragment-module
                  :entryPoint "main"
                  :targets [{:format fformat
                             :blend (scene-color-blend scene-color)}]}
       :primitive {:topology "triangle-list"}}))))

(defn- create-image-mip-system [^js device scene-color]
  (let [mip-format (if (:enabled? scene-color)
                     "rgba8unorm-srgb"
                     "rgba8unorm")
        bind-layout (.createBindGroupLayout
                     device
                     (clj->js
                      {:entries [{:binding 0
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :sampler {:type "filtering"}}
                                 {:binding 1
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :texture {:sampleType "float"}}]}))
        layout (.createPipelineLayout
                device (clj->js {:bindGroupLayouts [bind-layout]}))
        vertex-module (.createShaderModule
                       device (clj->js {:code image-mip-vertex-shader}))
        fragment-module (.createShaderModule
                         device (clj->js {:code image-mip-fragment-shader}))
        ;; IMAGE-ATOM T4: candidate mip views are sRGB on both sides, so
        ;; filtering occurs between hardware decode and encode.  Seam-off uses
        ;; unorm on both sides so its declared zero-transfer leg cannot
        ;; accidentally half-convert while generating mips.
        pipeline (.createRenderPipeline
                  device
                  (clj->js
                   {:layout layout
                    :vertex {:module vertex-module :entryPoint "main"}
                    :fragment {:module fragment-module :entryPoint "main"
                               :targets [{:format mip-format}]}
                    :primitive {:topology "triangle-list"}}))
        sampler (.createSampler device
                                (clj->js {:minFilter "linear"
                                          :magFilter "linear"
                                          :mipmapFilter "linear"
                                          :addressModeU "clamp-to-edge"
                                          :addressModeV "clamp-to-edge"}))]
    {:bind-layout bind-layout :pipeline pipeline :sampler sampler
     :format mip-format}))

(defn- generate-image-mips!
  [^js device mip-system ^js texture mip-level-count]
  (when (> mip-level-count 1)
    (let [encoder (.createCommandEncoder device)]
      (doseq [level (range 1 mip-level-count)]
        (let [source-view (.createView
                           texture
                           (clj->js {:format (:format mip-system)
                                     :baseMipLevel (dec level)
                                     :mipLevelCount 1}))
              target-view (.createView
                           texture
                           (clj->js {:format (:format mip-system)
                                     :baseMipLevel level
                                     :mipLevelCount 1}))
              bind-group (.createBindGroup
                          device
                          (clj->js
                           {:layout (:bind-layout mip-system)
                            :entries [{:binding 0
                                       :resource (:sampler mip-system)}
                                      {:binding 1 :resource source-view}]}))
              pass (.beginRenderPass
                    encoder
                    (clj->js {:colorAttachments
                              [{:view target-view
                                :clearValue {:r 0 :g 0 :b 0 :a 0}
                                :loadOp "clear" :storeOp "store"}]}))]
          (.setPipeline pass (:pipeline mip-system))
          (.setBindGroup pass 0 bind-group)
          (.draw pass 3 1 0 0)
          (.end pass)))
      (.submit (.-queue device) #js [(.finish encoder)]))))

(defn- image-texture
  [^js device width height mip-level-count]
  (.createTexture
   device
   (clj->js {:size {:width width :height height :depthOrArrayLayers 1}
             :mipLevelCount mip-level-count
             :format "rgba8unorm"
             :viewFormats ["rgba8unorm-srgb"]
             :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                            js/GPUTextureUsage.COPY_DST
                            js/GPUTextureUsage.RENDER_ATTACHMENT)})))

(defn- image-view [^js texture scene-color]
  (.createView texture
               (clj->js {:format (if (:enabled? scene-color)
                                   "rgba8unorm-srgb"
                                   "rgba8unorm")})))

(defn- receipt-row-counts [rows]
  (reduce (fn [counts [_ {:keys [status]}]]
            (update counts status (fnil inc 0)))
          {:ok 0 :refused 0 :unavailable 0 :device-lost 0}
          rows))

(defn- publish-image-receipt! [image-system]
  (let [!receipt (:!receipt image-system)
        receipt (assoc @!receipt :counts (receipt-row-counts (:rows @!receipt)))]
    (reset! !receipt receipt)
    (aset js/globalThis "__softland_image_ingress_receipt" (clj->js receipt))
    receipt))

(defn- record-image-receipt!
  [image-system digest status reason details]
  (swap! (:!receipt image-system)
         (fn [receipt]
           (let [event (merge {:status status :reason reason} details)
                 prior (get-in receipt [:rows digest])
                 history (conj (vec (:history prior)) event)]
             (-> receipt
                 (assoc-in [:rows digest] (assoc event :history history))
                 (assoc :updated-at-ms (.now js/performance))))))
  (publish-image-receipt! image-system))

(defn- padded-image-canvas [^js bitmap padding]
  (let [width (.-width bitmap)
        height (.-height bitmap)
        canvas (js/OffscreenCanvas. (+ width (* 2 padding))
                                    (+ height (* 2 padding)))
        context (.getContext canvas "2d")]
    (.clearRect context 0 0 (.-width canvas) (.-height canvas))
    (.drawImage context bitmap padding padding)
    ;; IMAGE-ATOM T5: extrude edge texels through the declared gutter before
    ;; the atlas mip chain is generated; UVs still address only the interior.
    (.drawImage context bitmap 0 0 1 height 0 padding padding height)
    (.drawImage context bitmap (dec width) 0 1 height (+ padding width) padding
                padding height)
    (.drawImage context bitmap 0 0 width 1 padding 0 width padding)
    (.drawImage context bitmap 0 (dec height) width 1 padding (+ padding height)
                width padding)
    (.drawImage context bitmap 0 0 1 1 0 0 padding padding)
    (.drawImage context bitmap (dec width) 0 1 1 (+ padding width) 0
                padding padding)
    (.drawImage context bitmap 0 (dec height) 1 1 0 (+ padding height)
                padding padding)
    (.drawImage context bitmap (dec width) (dec height) 1 1
                (+ padding width) (+ padding height) padding padding)
    canvas))

(defn- bytes->sha256 [bytes]
  (-> (.digest (.-subtle js/crypto) "SHA-256" bytes)
      (.then (fn [digest]
               (apply str
                      (map (fn [byte]
                             (.padStart (.toString byte 16) 2 "0"))
                           (array-seq (js/Uint8Array. digest))))))))

(defn- normalize-image-alpha!
  "Return a bitmap whose RGB is straight.  PNG decode already yields straight
   bytes for :straight/:opaque sources.  A source explicitly tagged
   :premultiplied is unassociated exactly once before texture upload (T3/T9)."
  [source ^js bitmap]
  (if-not (= :premultiplied (:image/alpha-association source))
    (js/Promise.resolve bitmap)
    (let [width (.-width bitmap)
          height (.-height bitmap)
          canvas (js/OffscreenCanvas. width height)
          context (.getContext canvas "2d"
                               #js {:willReadFrequently true})
          _ (.drawImage context bitmap 0 0)
          image-data (.getImageData context 0 0 width height)
          pixels (.-data image-data)]
      (loop [index 0]
        (when (< index (.-length pixels))
          (let [alpha (aget pixels (+ index 3))]
            (when (pos? alpha)
              (aset pixels index
                    (min 255 (Math/round (/ (* (aget pixels index) 255.0)
                                              alpha))))
              (aset pixels (+ index 1)
                    (min 255 (Math/round (/ (* (aget pixels (+ index 1)) 255.0)
                                              alpha))))
              (aset pixels (+ index 2)
                    (min 255 (Math/round (/ (* (aget pixels (+ index 2)) 255.0)
                                              alpha))))))
          (recur (+ index 4))))
      (.putImageData context image-data 0 0)
      (.close bitmap)
      (js/createImageBitmap canvas #js {:colorSpaceConversion "none"
                                        :premultiplyAlpha "none"}))))

(defn init-image-system
  "Own the image pipeline, digest registry, atlas/dedicated resources, and the
   one shared 13-word instance pool.  Product activation remains staged; the
   verifier creates this system directly."
  [^js device fformat camera-buffer containers-buffer
   & {:keys [initial-capacity tracker scene-color budget-cap-bytes]
      :or {initial-capacity 256
           scene-color scene-color/legacy-direct-color}}]
  (assert containers-buffer "init-image-system requires :containers-buffer")
  (let [bind-layout (.createBindGroupLayout
                     device
                     (clj->js
                      {:entries [{:binding 0
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :sampler {:type "filtering"}}
                                 {:binding 1
                                  :visibility js/GPUShaderStage.FRAGMENT
                                  :texture {:sampleType "float"}}
                                 {:binding 2
                                  :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "uniform"}}
                                 {:binding 3
                                  :visibility js/GPUShaderStage.VERTEX
                                  :buffer {:type "read-only-storage"}}]}))
        pipeline (create-image-pipeline device fformat bind-layout scene-color)
        sampler (.createSampler device
                                (clj->js {:minFilter "linear"
                                          :magFilter "linear"
                                          :mipmapFilter "linear"
                                          :addressModeU "clamp-to-edge"
                                          :addressModeV "clamp-to-edge"}))
        mip-system (create-image-mip-system device scene-color)
        placeholder-texture (image-texture device 2 2 1)
        placeholder-bytes (js/Uint8Array.
                           #js [255 0 255 255, 24 24 24 255,
                                24 24 24 255, 255 0 255 255])
        _ (.writeTexture (.-queue device)
                         (clj->js {:texture placeholder-texture})
                         placeholder-bytes
                         (clj->js {:bytesPerRow 8 :rowsPerImage 2})
                         (clj->js {:width 2 :height 2}))
        _ (gpu-budget/register-texture! tracker placeholder-texture
                                        "image/placeholder"
                                        :format "rgba8unorm" :width 2 :height 2
                                        :mip-level-count 1)
        placeholder-view (image-view placeholder-texture scene-color)
        placeholder-bind-group (create-image-bind-group
                                device bind-layout sampler placeholder-view
                                camera-buffer containers-buffer)
        {:keys [width height mip-level-count]} image-material/atlas-config
        atlas-texture (image-texture device width height mip-level-count)
        _ (gpu-budget/register-texture! tracker atlas-texture "image/atlas"
                                        :format "rgba8unorm"
                                        :width width :height height
                                        :mip-level-count mip-level-count)
        atlas-view (image-view atlas-texture scene-color)
        atlas-bind-group (create-image-bind-group
                          device bind-layout sampler atlas-view
                          camera-buffer containers-buffer)
        pool (buffer-pool/create-pool
              device initial-capacity pipeline nil
              :floats-per-item image-material/image-instance-words
              :pack-fn pack-image-instance
              :tracker tracker :label "image/instances")
        image-system
        {:device device :pipeline pipeline :bind-layout bind-layout
         :sampler sampler :mip-system mip-system :pool pool
         :camera-buffer camera-buffer :containers-buffer containers-buffer
         :scene-color scene-color :gpu-tracker tracker
         :budget-cap-bytes budget-cap-bytes
         :placeholder {:texture placeholder-texture
                       :bind-group placeholder-bind-group
                       :binding-key :image/placeholder
                       :uv [0.0 0.0 1.0 1.0]}
         :!atlas-resource (atom {:texture atlas-texture
                                 :bind-group atlas-bind-group})
         :!atlas (atom (image-material/empty-atlas))
         :!resources (atom {})
         :!source-registry (atom (image-material/empty-source-registry))
         :!source-bytes (atom {})
         :frame-input/identity (js-obj) :!shape-rev (atom 0)
         :!last-images (atom ::never) :!prepared-images (atom [])
         :!last-prepare-key (atom ::never)
         :!receipt (atom {:version 1 :rows {}
                          :ingress image-material/ingress-receipt})}]
    (publish-image-receipt! image-system)
    image-system))

(defn- copy-image-to-atlas!
  [image-system ^js bitmap {:keys [x y padding width height]}]
  (let [^js device (:device image-system)
        ^js texture (:texture @(:!atlas-resource image-system))
        padded (padded-image-canvas bitmap padding)]
    (.copyExternalImageToTexture
     (.-queue device)
     (clj->js {:source padded})
     (clj->js {:texture texture
               :origin {:x (- x padding) :y (- y padding)}})
     (clj->js {:width (+ width (* 2 padding))
               :height (+ height (* 2 padding))}))
    (generate-image-mips! device (:mip-system image-system) texture
                          (:mip-level-count image-material/atlas-config))))

(defn- create-dedicated-image-resource!
  [image-system digest ^js bitmap]
  (let [^js device (:device image-system)
        width (.-width bitmap)
        height (.-height bitmap)
        mip-level-count (image-material/mip-level-count width height)
        texture (image-texture device width height mip-level-count)
        _ (.copyExternalImageToTexture
           (.-queue device)
           (clj->js {:source bitmap})
           (clj->js {:texture texture})
           (clj->js {:width width :height height}))
        _ (generate-image-mips! device (:mip-system image-system) texture
                                mip-level-count)
        _ (gpu-budget/register-texture! (:gpu-tracker image-system) texture
                                        (str "image/dedicated/" digest)
                                        :format "rgba8unorm"
                                        :width width :height height
                                        :mip-level-count mip-level-count)
        view (image-view texture (:scene-color image-system))
        bind-group (create-image-bind-group
                    device (:bind-layout image-system) (:sampler image-system)
                    view (:camera-buffer image-system)
                    (:containers-buffer image-system))]
    {:texture texture :bind-group bind-group
     :binding-key [:image/dedicated digest]
     :tier :dedicated :uv [0.0 0.0 1.0 1.0]
     :width width :height height :mip-level-count mip-level-count}))

(defn register-image-source!
  "Verify, decode, upload, and register one digest-addressed image source.
   The returned Promise resolves to a GPU resource or nil on a receipted
   refusal.  No decode or Promise work is reachable from the frame producer
   (IMAGE-ATOM T2/T9/T10)."
  [image-system source bytes]
  (let [digest (:image/digest source)
        started-at (.now js/performance)]
    (-> (js/Promise.resolve nil)
        (.then
         (fn [_]
           (image-material/validate-source! source)
           (bytes->sha256 bytes)))
        (.then
         (fn [computed-digest]
           (when-not (= digest computed-digest)
             (throw (ex-info "Image source digest mismatch"
                             {:reason :digest-mismatch
                              :declared digest :computed computed-digest})))
           (let [registry (image-material/register-verified-source
                           @(:!source-registry image-system)
                           source computed-digest)
                 width (:image/width source)
                 height (:image/height source)
                 planned-bytes (image-material/texture-bytes width height)
                 cap (:budget-cap-bytes image-system)]
             (reset! (:!source-registry image-system) registry)
             (swap! (:!source-bytes image-system) assoc digest
                    {:source source :bytes bytes})
             (if (and cap (> planned-bytes cap))
               (do
                 ;; IMAGE-ATOM T11: refusal uses full-chain bytes; level zero
                 ;; can never sneak through an injected budget cap.
                 (record-image-receipt!
                  image-system digest :refused :over-budget
                  {:planned-bytes planned-bytes :budget-cap-bytes cap})
                 nil)
               (let [blob (js/Blob. #js [bytes] #js {:type "image/png"})]
                 (js/createImageBitmap
                  blob
                  #js {:colorSpaceConversion
                       (if (get-in image-system [:scene-color :enabled?])
                         "default"
                         "none")
                       :premultiplyAlpha "none"}))))))
        (.then (fn [bitmap]
                 (if bitmap
                   (normalize-image-alpha! source bitmap)
                   nil)))
        (.then
         (fn [bitmap]
           (when bitmap
             (let [width (.-width bitmap)
                   height (.-height bitmap)
                   declared [(:image/width source) (:image/height source)]]
               (when-not (= declared [width height])
                 (.close bitmap)
                 (throw (ex-info "Decoded image dimensions differ from source"
                                 {:reason :dimension-mismatch
                                  :declared declared :decoded [width height]})))
               (let [decoded-at (.now js/performance)
                     plan (image-material/placement-plan
                           @(:!atlas image-system) digest
                           {:width width :height height})
                     resource
                     (if (= :atlas (:tier plan))
                       (let [placement (:placement plan)
                             _ (reset! (:!atlas image-system) (:atlas plan))
                             _ (copy-image-to-atlas! image-system bitmap placement)
                             atlas-resource @(:!atlas-resource image-system)]
                         {:bind-group (:bind-group atlas-resource)
                          :binding-key :image/atlas
                          :tier :atlas :uv (:uv placement)
                          :placement placement
                          :width width :height height
                          :mip-level-count
                          (:mip-level-count image-material/atlas-config)})
                       (create-dedicated-image-resource!
                        image-system digest bitmap))
                     completed-at (.now js/performance)]
                 (.close bitmap)
                 (swap! (:!resources image-system) assoc digest resource)
                 (record-image-receipt!
                  image-system digest :ok (:tier resource)
                  {:tier (:tier resource)
                   :mip-level-count (:mip-level-count resource)
                   :ingress-transfers (if (get-in image-system
                                                  [:scene-color :enabled?])
                                        1 0)
                   :presentation-encodes (if (get-in image-system
                                                     [:scene-color :enabled?])
                                           1 0)
                   :decode-ms (- decoded-at started-at)
                   :upload-enqueue-ms (- completed-at decoded-at)
                   :total-enqueue-ms (- completed-at started-at)})
                 resource)))))
        (.catch
         (fn [error]
           (let [data (ex-data error)
                 reason (or (:reason data) :invalid-source)]
             (record-image-receipt!
              image-system (or digest :image/unknown) :refused reason
              {:message (or (.-message error) (str error))})
             nil))))))

(defn image-ingress-receipt [image-system]
  (publish-image-receipt! image-system))

(defn- destroy-dedicated-resources! [image-system]
  (doseq [[_ resource] @(:!resources image-system)
          :when (= :dedicated (:tier resource))]
    (when-let [texture (:texture resource)]
      (gpu-budget/destroy-resource! (:gpu-tracker image-system) texture
                                    :reason :image-resource-destroy)
      (.destroy ^js texture))))

(defn rebuild-image-resources!
  "Reconstruct every device-owned image resource on a freshly initialized
   replacement system.  No pipeline, atlas, placeholder, pool buffer, or
   bind-group from the lost device is reused.  The digest registry is the only
   bridge across devices (IMAGE-ATOM T2/T11)."
  [lost-system replacement-system]
  (let [sources (sort-by #(get-in % [:source :image/digest])
                         (vals @(:!source-bytes lost-system)))
        digests (mapv #(get-in % [:source :image/digest]) sources)
        resources-fresh?
        (and (not (identical? (:device lost-system)
                              (:device replacement-system)))
             (not (identical? (:pipeline lost-system)
                              (:pipeline replacement-system)))
             (not (identical? (get-in lost-system [:placeholder :texture])
                              (get-in replacement-system
                                      [:placeholder :texture])))
             (not (identical? (:texture @(:!atlas-resource lost-system))
                              (:texture @(:!atlas-resource
                                          replacement-system))))
             (not (identical? (:buffer @(:pool lost-system))
                              (:buffer @(:pool replacement-system)))))]
    (doseq [digest digests]
      (record-image-receipt! lost-system digest :device-lost :device-lost
                             {:rebuild-count 0})
      (record-image-receipt! replacement-system digest
                             :device-lost :replacement-rebuild-start
                             {:rebuild-count 0}))
    (-> (js/Promise.all
         (clj->js (mapv (fn [{:keys [source bytes]}]
                          (register-image-source! replacement-system
                                                  source bytes))
                        sources)))
        (.then
         (fn [rebuilt]
           (swap! (:!receipt replacement-system) assoc
                  :device-loss {:status :device-lost
                                :rebuilt-count
                                (count (filter some? (array-seq rebuilt)))
                                :replacement-device? true
                                :resource-identities-fresh?
                                resources-fresh?})
           {:image-system replacement-system
            :receipt (publish-image-receipt! replacement-system)})))))

(defn destroy-image-system! [image-system]
  (destroy-dedicated-resources! image-system)
  (doseq [texture [(get-in image-system [:placeholder :texture])
                   (:texture @(:!atlas-resource image-system))]]
    (when texture
      (gpu-budget/destroy-resource! (:gpu-tracker image-system) texture
                                    :reason :image-system-destroy)
      (.destroy ^js texture)))
  (when-let [pool (:pool image-system)]
    (let [buffer (:buffer @pool)]
      (gpu-budget/destroy-resource! (:gpu-tracker image-system) buffer
                                    :reason :image-system-destroy)
      (.destroy ^js buffer)))
  (reset! (:!resources image-system) {})
  (reset! (:!prepared-images image-system) [])
  true)

(defn create-camera-buffer
  [^js/GPUDevice device tracker]
  (let [camera-buffer (.createBuffer device (clj->js {:size 24
                                                      :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                     js/GPUBufferUsage.COPY_DST)}))]
    (gpu-budget/register-buffer! tracker camera-buffer "text/shared-camera" 24 :active-bytes 24)
    camera-buffer))

(defn- create-instance-buffer [^js/GPUDevice device tracker label initial-capacity stride]
  (let [size (* initial-capacity stride)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.VERTEX
                                                              js/GPUBufferUsage.COPY_DST)}))]
    (gpu-budget/register-buffer! tracker buffer label size :active-bytes 0)
    buffer))

(defn- create-slug-bind-group [^js/GPUDevice device layout curve-view band-view camera-buffer sizes-buffer containers-buffer]
  (.createBindGroup device
    (clj->js {:layout layout
              :entries [{:binding 0 :resource curve-view}
                        {:binding 1 :resource band-view}
                        {:binding 2 :resource {:buffer camera-buffer}}
                        {:binding 3 :resource {:buffer sizes-buffer}}
                        {:binding 4 :resource {:buffer containers-buffer}}]})))

(defn- create-slug-texture [^js/GPUDevice device tracker label format width height bytes bytes-per-row]
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
    (gpu-budget/register-texture! tracker texture label
                                  :format format
                                  :width width
                                  :height height)
    texture))

(defn- create-slug-font-resources [^js/GPUDevice device tracker slug-assets]
  (let [curve-width (get-in slug-assets [:meta :curveTexture :width])
        curve-height (get-in slug-assets [:meta :curveTexture :height])
        band-width (get-in slug-assets [:meta :bandTexture :width])
        band-height (get-in slug-assets [:meta :bandTexture :height])
        curve-texture (create-slug-texture device tracker "text/slug-curve"
                                           "rgba16float"
                                           curve-width curve-height
                                           (:curve-bytes slug-assets)
                                           (* curve-width 8))
        band-texture (create-slug-texture device tracker "text/slug-band"
                                          "rg16uint"
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
  (when-let [tracker (:gpu-tracker text-sys)]
    (when-let [curve-texture (:curve-texture text-sys)]
      (gpu-budget/destroy-resource! tracker curve-texture :reason :slug-curve-destroy))
    (when-let [band-texture (:band-texture text-sys)]
      (gpu-budget/destroy-resource! tracker band-texture :reason :slug-band-destroy)))
  (when-let [^js curve-texture (:curve-texture text-sys)]
    (.destroy curve-texture))
  (when-let [^js band-texture (:band-texture text-sys)]
    (.destroy band-texture)))

(defn destroy-text-system! [text-sys]
  (when-let [tracker (:gpu-tracker text-sys)]
    (when-let [instance-buffer (:instance-buffer text-sys)]
      (gpu-budget/destroy-resource! tracker instance-buffer :reason :text-instance-destroy))
    (when (and (:owns-sizing-buffer? text-sys) (:sizes-uniform-buffer text-sys))
      (gpu-budget/destroy-resource! tracker (:sizes-uniform-buffer text-sys) :reason :text-sizing-destroy)))
  (when-let [^js instance-buffer (:instance-buffer text-sys)]
    (.destroy instance-buffer))
  (when (and (:owns-sizing-buffer? text-sys) (:sizes-uniform-buffer text-sys))
    (.destroy ^js (:sizes-uniform-buffer text-sys)))
  (when (:owns-font-resources? text-sys)
    (destroy-slug-font-resources! text-sys)))

(defn- init-slug-text-system
  [^js/GPUDevice device fformat camera-buffer font-assets
   & {:keys [initial-capacity tracker label containers-buffer scene-color]
      :or {initial-capacity 10000
           label "text/content"
           scene-color scene-color/legacy-direct-color}}]
  (assert containers-buffer "init-slug-text-system requires :containers-buffer (scene-substrate P2)")
  (let [vertex-module (.createShaderModule device (clj->js {:code slug-vertex-shader}))
        fragment-module (.createShaderModule device
                                             (clj->js {:code (configure-scene-color-shader
                                                              slug-fragment-shader scene-color)}))
        font-resources (create-slug-font-resources device tracker (:slug font-assets))
        instance-buffer (create-instance-buffer device tracker label initial-capacity slug-text-instance-stride)
        sizes-buffer (.createBuffer device (clj->js {:size 16
                                                     :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                    js/GPUBufferUsage.COPY_DST)}))
        _ (gpu-budget/register-buffer! tracker sizes-buffer "text/slug-params" 16 :active-bytes 16)
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
                                                   :blend (scene-color-blend scene-color)}]}
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
            :gpu-tracker tracker
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
  (let [tracker (:gpu-tracker text-sys)
         old-curve (:curve-texture text-sys)
         old-band (:band-texture text-sys)
         font-resources (create-slug-font-resources device tracker (:slug font-assets))]
     (when (and tracker old-curve (:owns-font-resources? text-sys))
       (gpu-budget/destroy-resource! tracker old-curve :reason :slug-curve-update))
     (when (and tracker old-band (:owns-font-resources? text-sys))
       (gpu-budget/destroy-resource! tracker old-band :reason :slug-band-update))
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
        tracker (:gpu-tracker old-text-sys)
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
                      :tracker tracker
                      :label label
                      :containers-buffer containers-buffer
                      :scene-color scene-color)))

(defn clone-text-system
  "Create a lightweight text system clone sharing pipeline, bind-group, camera,
   and font resources with the parent. Only the instance buffer is new."
  [^js/GPUDevice device parent-text-sys initial-capacity]
  (let [stride (:instance-stride parent-text-sys)
        ib (create-instance-buffer device (:gpu-tracker parent-text-sys) "text/chrome" initial-capacity stride)]
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

;; --- Clear-quad system (Phase 6E: dirty-present) ---
(def clear-quad-shader "
  @vertex
  fn vs_main(@builtin(vertex_index) v: u32) -> @builtin(position) vec4<f32> {
      // Fullscreen triangle from vertex index — no vertex buffer needed
      let x = f32(i32(v & 1u)) * 4.0 - 1.0;
      let y = f32(i32(v >> 1u)) * 4.0 - 1.0;
      return vec4<f32>(x, y, 0.0, 1.0);
  }
  @fragment
  fn fs_main() -> @location(0) vec4<f32> {
      return vec4<f32>(0.0, 0.0, 0.0, 1.0);
  }")

(defn- configure-clear-quad-shader [color]
  (let [[r g b a] (:clear color)]
    (str/replace clear-quad-shader
                 "return vec4<f32>(0.0, 0.0, 0.0, 1.0);"
                 (str "return vec4<f32>(" r ", " g ", " b ", " a ");"))))

(defn- clear-value [color]
  (let [[r g b a] (:clear color)]
    {:r r :g g :b b :a a}))

(defn init-clear-quad
  [^js/GPUDevice device fformat & {:keys [scene-color]
                                   :or {scene-color scene-color/legacy-direct-color}}]
  (let [module (.createShaderModule device
                                   (clj->js {:code (configure-clear-quad-shader
                                                    scene-color)}))
        layout (.createPipelineLayout device (clj->js {:bindGroupLayouts []}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout layout
                             :vertex {:module module :entryPoint "vs_main"}
                             :fragment {:module module :entryPoint "fs_main"
                                        :targets [{:format fformat
                                                   :writeMask 0xF}]}
                             :primitive {:topology "triangle-list"}}))]
    {:pipeline pipeline
     :scene-color scene-color}))

;; --- Persistent render target (Phase 6E: survives swap chain double-buffering) ---

(defn create-render-target
  [^js device width height fformat & {:keys [tracker label previous scene-color]
                                      :or {label "render-target/persistent"
                                           scene-color scene-color/legacy-direct-color}}]
  (let [safe-width (max 1 width)
        safe-height (max 1 height)
        tex (.createTexture device
              (clj->js {:size {:width safe-width :height safe-height}
                        :format fformat
                        :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                       js/GPUTextureUsage.COPY_SRC)}))
        old-texture (:texture previous)]
    (js/console.log "[RENDERER] Create render target"
                    {:label label
                     :width safe-width
                     :height safe-height
                     :format fformat
                     :replacing? (boolean old-texture)})
    (if old-texture
      (gpu-budget/replace-texture! tracker old-texture tex label
                                   :format fformat
                                   :width safe-width
                                   :height safe-height
                                   :reason :render-target-resize)
      (gpu-budget/register-texture! tracker tex label
                                    :format fformat
                                    :width safe-width
                                    :height safe-height))
    (when old-texture
      (.destroy ^js old-texture))
    {:texture tex
     :view (.createView tex)
     :width safe-width
     :height safe-height
     :resource/id :scene-color/main
     :scene-color scene-color
     :gpu-tracker tracker
     :gpu-label label}))

(defn destroy-render-target! [{:keys [^js texture gpu-tracker]}]
  (when texture
    (gpu-budget/destroy-resource! gpu-tracker texture :reason :render-target-destroy)
    (.destroy texture)))

(defn- scene-color-resource
  "Resolve the one frame scene-color resource.  With no persistent target this
   is the direct-present swap view; when the existing default-off target is
   enabled it becomes the intermediate view.  Future group targets extend this
   resource shape instead of creating another frame path."
  [swap-view render-target color]
  {:resource/id :scene-color/main
   :resource/mode (if render-target :intermediate :direct-present)
   :view (if render-target (:view render-target) swap-view)
   :format (:format render-target)
   :color color})

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
        range-result (tl/glyphs-in-source-range
                      line
                      (if (= :header (first (:source-range line)))
                        [:header (second (:source-range line))
                         [range-start range-end]]
                        [(tl/tagged-index range-start) (tl/tagged-index range-end)])
                      dx dy)]
    {:layout/id (:layout/id layout-result)
     :style txt
     :font-size fsize
     :span-receipt (select-keys range-result [:glyph-span :visited-glyphs])
     :glyphs (:glyphs range-result)}))

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
            positioned-glyphs (:glyphs positioned-op)]
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
  [^js/GPUDevice device renderer-state required-size active-bytes]
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
      (gpu-budget/replace-buffer! (:gpu-tracker renderer-state) current-buffer new-buffer (:gpu-label renderer-state)
                                  alloc-size
                                  :active-bytes active-bytes
                                  :reason :text-resize)
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

(defn update-text-data
  [^js/GPUDevice device renderer-state texts font-assets font-size
   & {:keys [line-height-factor line-height char-width snap-step surface]
      :or {line-height-factor 1.0 char-width 0.56}}]
  (let [line-h (or line-height (* font-size line-height-factor))
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [instances (shape-text tokens-in-line font-size font-assets
                                                         :char-width char-width
                                                         :snap-step snap-step
                                                         :surface surface)]
                               {:instances instances
                                :count (count instances)}))
                           texts)
        actual-instances (reduce + (map :count shaped-lines))
        buffer-instance-count (max actual-instances 1)
        stride (:instance-stride renderer-state)
        line-offsets (line-offsets-for shaped-lines)
        active-bytes (* actual-instances stride)]
    (let [raw-buffer (js/ArrayBuffer. (* buffer-instance-count stride))
          float-view (js/Float32Array. raw-buffer)
          uint-view (js/Uint32Array. raw-buffer)
          upload-view (js/Uint8Array. raw-buffer)
          required-size (.-byteLength upload-view)
          new-buffer (ensure-text-instance-buffer device renderer-state required-size active-bytes)]
      (pack-slug-instances! float-view uint-view shaped-lines)
      (.writeBuffer (.-queue device) new-buffer 0 upload-view)
      ;; Slug renders raw mathematical coverage — no sharpness bias.
      ;; The uniform exists (pipeline expects binding 3) but stays at 0.
      (when-let [sizes-buffer (:sizes-uniform-buffer renderer-state)]
        (let [sizes (js/Float32Array. #js [0.0 0.0 0.0 0.0])]
          (.writeBuffer (.-queue device) sizes-buffer 0 sizes)))
      (gpu-budget/set-active-bytes! (:gpu-tracker renderer-state) new-buffer active-bytes)
      (when (or (not= line-offsets (:line-offsets renderer-state))
                (not= (pos? actual-instances)
                      (pos? (:num-instances renderer-state 0))))
        (swap! (:!shape-rev renderer-state) inc))
      (assoc renderer-state
             :instance-buffer new-buffer
             :num-instances actual-instances
             :line-offsets line-offsets
             :line-height line-h))))

(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))

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

(defn- inset-resource-uv [resource-uv crop-uv]
  (let [[resource-u0 resource-v0 resource-u1 resource-v1] resource-uv
        [crop-u0 crop-v0 crop-u1 crop-v1] crop-uv
        du (- resource-u1 resource-u0)
        dv (- resource-v1 resource-v0)]
    [(+ resource-u0 (* crop-u0 du))
     (+ resource-v0 (* crop-v0 dv))
     (+ resource-u0 (* crop-u1 du))
     (+ resource-v0 (* crop-v1 dv))]))

(defn- resolve-image-op [image-system image-op]
  (let [digest (:image/digest image-op)
        resource (get @(:!resources image-system) digest)
        prior-row (get-in @(:!receipt image-system) [:rows digest])
        resolved (or resource (:placeholder image-system))]
    (when-not resource
      ;; Missing resources are a deterministic material outcome.  The frame
      ;; never starts decode work and never silently drops an instance (T10).
      ;; A prior refusal or device-loss event keeps its causal status/history;
      ;; painting its placeholder must not relabel it as an unknown digest.
      (if prior-row
        (do
          (swap! (:!receipt image-system) assoc-in
                 [:rows digest :placeholder-rendered] true)
          (publish-image-receipt! image-system))
        (record-image-receipt! image-system digest :unavailable
                               :unresolvable-digest
                               {:placeholder true
                                :placeholder-rendered true})))
    (assoc image-op
           :image/binding-key (:binding-key resolved)
           :image/bind-group (:bind-group resolved)
           :image/resolved-uv
           (if resource
             (inset-resource-uv (:uv resolved) (:image/uv image-op))
             (:uv resolved)))))

(defn prepare-image-frame!
  "Identity-gated image pool write: the prepared vector is rewritten only when
   the image ops or the resolved resources changed (IMAGE-ATOM T10)."
  [image-system images]
  (let [images (or images [])
        !last-images (:!last-images image-system)
        prepare-key {:images images :resources @(:!resources image-system)}]
    (if (= prepare-key @(:!last-prepare-key image-system))
      {:identity-changed? false :writes 0
       :instances (count @(:!prepared-images image-system))}
      (let [prepared (mapv #(resolve-image-op image-system %) images)
            writes (buffer-pool/batch-update-pool! (:pool image-system)
                                                   prepared)]
        (reset! !last-images images)
        (reset! (:!last-prepare-key image-system) prepare-key)
        (reset! (:!prepared-images image-system) prepared)
        (swap! (:!receipt image-system) assoc
               :frame-write {:identity-changed? true
                             :writes writes :instances (count prepared)})
        (publish-image-receipt! image-system)
        {:identity-changed? true :writes writes
         :instances (count prepared)}))))

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

(defn image-draw-runs
  "Binding runs over one contiguous slice of the prepared image ops, in op
   order; no texture grouping may reorder the stamped op stream."
  [image-system offset instance-count]
  (let [prepared @(:!prepared-images image-system)
        slot-ops (subvec prepared offset (+ offset instance-count))
        buffer (:buffer @(:pool image-system))]
    (mapv (fn [{:keys [first-instance instance-count ops]}]
            {:bind-group (:image/bind-group (first ops))
             :buffer buffer
             :instance-count instance-count
             :first-instance (+ offset first-instance)})
          (image-material/contiguous-binding-runs slot-ops))))

(defn draw-image-runs!
  "Family-owned sub-draw walker over one prepared image slice."
  [^js pass image-system offset instance-count]
  (.setPipeline pass (:pipeline image-system))
  (doseq [{:keys [bind-group buffer instance-count first-instance]}
          (image-draw-runs image-system offset instance-count)]
    (.setBindGroup pass 0 bind-group)
    (.setVertexBuffer pass 0 buffer)
    (.draw pass 6 instance-count 0 first-instance)))

(defn- create-linear-image-variant [^js device image-system]
  (when image-system
    (let [bind-layout
          (.createBindGroupLayout
           device
           (clj->js
            {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                        :sampler {:type "filtering"}}
                       {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                        :texture {:sampleType "float"}}
                       {:binding 2 :visibility js/GPUShaderStage.VERTEX
                        :buffer {:type "uniform"}}
                       {:binding 3 :visibility js/GPUShaderStage.VERTEX
                        :buffer {:type "read-only-storage"}}]}))
          pipeline (create-image-pipeline device "rgba16float" bind-layout
                                          scene-color/linear-premultiplied-color)
          binding-map (js/WeakMap.)
          install!
          (fn [{:keys [texture bind-group]}]
            (when (and texture bind-group (not (.has binding-map bind-group)))
              (let [view (.createView ^js texture
                                      (clj->js {:format "rgba8unorm-srgb"}))
                    linear-bind-group
                    (create-image-bind-group
                     device bind-layout (:sampler image-system) view
                     (:camera-buffer image-system)
                     (:containers-buffer image-system))]
                (.set binding-map bind-group linear-bind-group))))
          sync!
          (fn []
            (install! (:placeholder image-system))
            (install! @(:!atlas-resource image-system))
            (doseq [resource (vals @(:!resources image-system))]
              (install! resource))
            true)]
      (sync!)
      {:pipeline pipeline :binding-map binding-map :sync! sync!})))

(defonce ^:private !compositors-by-device (js/WeakMap.))

(defn- ensure-frame-compositor! [device format tracker]
  (or (.get !compositors-by-device device)
      (let [compositor (compositor-gpu/create-compositor!
                        device format tracker)]
        (.set !compositors-by-device device compositor)
        compositor)))

(defn replace-frame-compositor!
  "The sole same-device compositor epoch producer.  Semantic frame state is
   deliberately retained; Region3D reattaches to the new binding epoch."
  [device format tracker]
  (when-let [old (.get !compositors-by-device device)]
    (compositor-gpu/destroy-compositor! old))
  (let [compositor (compositor-gpu/create-compositor! device format tracker)]
    (.set !compositors-by-device device compositor)
    (when-let [region-system (region3d-gpu/region3d-system-for-device device)]
      (region3d-gpu/attach-compositor! region-system compositor))
    compositor))

(defn- clip-execution-mode
  "A screen-axis-aligned effective affine uses a scissor. Rotation/shear is a
   declared mask-road binding; callers must never approximate it as a scissor."
  [[a b c d _tx _ty]]
  (if (or (and (< (abs (double b)) 1.0e-9)
               (< (abs (double c)) 1.0e-9))
          (and (< (abs (double a)) 1.0e-9)
               (< (abs (double d)) 1.0e-9)))
    :scissor
    :mask))

(defn project-clip-rect
  "Project a container-local clip into WebGPU attachment pixels. Camera and
   container coordinates are CSS pixels; scissor rectangles are device pixels,
   so the viewport-to-attachment scale is part of the projection."
  [clip container effective-transforms pan-x pan-y zoom attachment-size
   viewport-size]
  (when clip
    (let [{:keys [affine flags]}
          (or (get effective-transforms container)
              {:affine [1.0 0.0 0.0 1.0 0.0 0.0] :flags 0})]
      (let [[a b c d tx ty] affine
            {:keys [x y w h]} clip
            points [[x y] [(+ x w) y] [x (+ y h)] [(+ x w) (+ y h)]]
            screen? (= 1 (bit-and (or flags 0) 1))
            zm (if screen? 1.0 zoom)
            px (if screen? 0.0 pan-x)
            py (if screen? 0.0 pan-y)
            [aw ah] attachment-size
            [vw vh] (or viewport-size attachment-size)
            device-x (/ aw (max 1.0 vw))
            device-y (/ ah (max 1.0 vh))
            projected (map (fn [[lx ly]]
                             [(* device-x
                                 (+ (* (+ (* a lx) (* c ly) tx) zm) px))
                              (* device-y
                                 (+ (* (+ (* b lx) (* d ly) ty) zm) py))])
                           points)
            xs (map first projected)
            ys (map second projected)
            x0 (int (js/Math.floor (apply min xs)))
            y0 (int (js/Math.floor (apply min ys)))
            x1 (int (js/Math.ceil (apply max xs)))
            y1 (int (js/Math.ceil (apply max ys)))
            cx (max 0 (min aw x0))
            cy (max 0 (min ah y0))]
        (if (= :scissor (clip-execution-mode affine))
          {:mode :scissor
           :x cx :y cy :w (max 0 (- (max cx (min aw x1)) cx))
           :h (max 0 (- (max cy (min ah y1)) cy))}
          ;; Perimeter order (tl, tr, br, bl) feeds the generic convex mask
          ;; pass. A rotated/sheared clip is never approximated by its AABB.
          {:mode :mask :points [(nth projected 0) (nth projected 1)
                                (nth projected 3) (nth projected 2)]
           :container container :local-clip clip})))))
