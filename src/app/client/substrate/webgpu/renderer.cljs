(ns app.client.substrate.webgpu.renderer
  (:require [app.client.substrate.webgpu.gpu-budget :as gpu-budget]))

;; --- 1. SHADERS ---
;; Rich quads: 28 floats/rect, SDF-based rounded corners, borders, gradients
(def rect-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(0) var<uniform> camera: Camera;
  // scene-substrate P2: per-container transforms. data[i] = (offset.xy, scale, screen-flag).
  // Container 0 is the identity world container (trap T6).
  struct Containers { data: array<vec4<f32>, 1024>, };
  @group(0) @binding(1) var<uniform> containers: Containers;
  struct InstanceInput {
    @location(0) rect_geometry: vec4<f32>,
    @location(1) color: vec4<f32>,
    @location(2) corner_radii: vec4<f32>,
    @location(3) border_widths: vec4<f32>,
    @location(4) border_color: vec4<f32>,
    @location(5) gradient: vec4<f32>,
    @location(6) gradient_color2: vec4<f32>,
    @location(7) container_idx: u32,
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
      let c = containers.data[instance.container_idx];
      let is_screen = c.w != 0.0;
      let zm = select(camera.zoom, 1.0, is_screen);
      let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
      let panned = ((c.xy + world_pos * c.z) * zm) + pn;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.color = instance.color;
      // Pass local UV (0..size in pixels) and rect size for SDF evaluation.
      // Effective on-screen scale = container scale * camera zoom.
      let eff = c.z * zm;
      output.local_pos = pos * instance.rect_geometry.zw * eff;
      output.rect_size = instance.rect_geometry.zw * eff;
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
  // scene-substrate P2: per-container transforms (trap T6: container 0 = identity).
  struct Containers { data: array<vec4<f32>, 1024>, };
  @group(0) @binding(1) var<uniform> containers: Containers;
  struct InstanceInput {
    @location(0) expanded_rect: vec4<f32>,
    @location(1) shadow_color: vec4<f32>,
    @location(2) corner_radii: vec4<f32>,
    @location(3) blur_params: vec4<f32>,
    @location(4) inner_rect: vec4<f32>,
    @location(5) container_idx: u32,
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
      let c = containers.data[instance.container_idx];
      let is_screen = c.w != 0.0;
      let zm = select(camera.zoom, 1.0, is_screen);
      let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
      let eff = c.z * zm;
      let panned = ((c.xy + world_pos * c.z) * zm) + pn;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.shadow_color = instance.shadow_color;
      output.local_pos = pos * instance.expanded_rect.zw * eff;
      output.rect_size = instance.expanded_rect.zw * eff;
      output.corner_radii = instance.corner_radii;
      output.blur_params = instance.blur_params;
      // Scale inner_rect to match the effective-scaled local_pos
      output.inner_rect = vec4<f32>(
          instance.inner_rect.xy * eff,
          instance.inner_rect.zw * eff);
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
  // scene-substrate P2: per-container transforms (trap T6: container 0 = identity).
  struct Containers { data: array<vec4<f32>, 1024>, };
  @group(0) @binding(4) var<uniform> containers: Containers;
  // Per-instance: rect (vec4), uv_bounds (vec4), color (vec4), container_idx (u32) = 13 words
  struct InstanceInput { @location(0) rect: vec4<f32>, @location(1) uv_bounds: vec4<f32>, @location(2) color: vec4<f32>, @location(3) container_idx: u32, };
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
      let c = containers.data[instance.container_idx];
      let is_screen = c.w != 0.0;
      let zm = select(camera.zoom, 1.0, is_screen);
      let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
      let panned = ((c.xy + world_pos * c.z) * zm) + pn;
      let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
      output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
      output.uv = vec2<f32>(u, v);
      output.v_visual_size = max(instance.rect.z, instance.rect.w) * c.z * zm;
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

(def slug-vertex-shader "
  struct Camera { pan: vec2<f32>, zoom: f32, padding: f32, screen_dimensions: vec2<f32>, };
  @group(0) @binding(2) var<uniform> camera: Camera;
  // scene-substrate P2: per-container transforms (trap T6: container 0 = identity).
  struct Containers { data: array<vec4<f32>, 1024>, };
  @group(0) @binding(4) var<uniform> containers: Containers;

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
    let c = containers.data[instance.container_idx];
    let is_screen = c.w != 0.0;
    let zm = select(camera.zoom, 1.0, is_screen);
    let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
    // Half-pixel dilation in SCREEN space: divide by the effective scale so
    // delta stays half a screen pixel in glyph-local units.
    let dilation = 0.5 / max(c.z * zm, 0.0001);
    let delta = sign * dilation;
    let panned = ((c.xy + (world_pos + delta) * c.z) * zm) + pn;
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

(def slug-fragment-shader "
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
    return vec4<f32>(color.rgb, saturate(coverage + params.sharpness) * color.a);
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

(def rect-stride 116)  ;; 28 floats + container u32, × 4 bytes (scene-substrate P2)
(def msdf-text-instance-stride 52)  ;; 12 floats + container u32
(def slug-text-instance-stride 100) ;; 24 words + container u32

;; --- scene-substrate P2: shared per-container transform buffer -------------
;; One uniform array of vec4 per container: (offset.x, offset.y, scale, flags)
;; where flags != 0.0 means screen-camera (chrome) — the world camera's
;; pan/zoom become identity for that container. Container 0 is RESERVED as
;; the identity world container (trap T6): every packer defaults instances
;; to it, which keeps the pre-P2 render byte-identical until a producer
;; assigns real containers.
(def max-containers 1024)

(defn create-containers-buffer
  "Create the shared container-transform uniform buffer and write the
   identity container 0. Shared across all four pipelines like the camera."
  [^js/GPUDevice device tracker]
  (let [size (* max-containers 16)
        buffer (.createBuffer device (clj->js {:size size
                                               :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                              js/GPUBufferUsage.COPY_DST)}))
        identity0 (js/Float32Array. #js [0.0 0.0 1.0 0.0])]
    (gpu-budget/register-buffer! tracker buffer "containers/shared" size :active-bytes size)
    (.writeBuffer (.-queue device) buffer 0 identity0)
    buffer))

(defn write-containers!
  "Upload effective container transforms. `effective` is {cid {:x :y :scale
   :flags (0|1) or :screen? bool}} — containers/effective output is accepted
   as-is. cid 0 (the reserved identity, trap T6) is skipped: it was written at
   buffer creation and the registry refuses to mutate it. Writes one
   contiguous range [1 .. max cid]; unlisted cids in the range default to
   identity, not zero-scale."
  [^js/GPUDevice device ^js containers-buffer effective]
  (let [effective (dissoc effective 0)]
    (when (seq effective)
      (let [max-cid (apply max (keys effective))
            _ (when (>= max-cid max-containers)
                (throw (ex-info "Container id out of range" {:cid max-cid :max max-containers})))
            floats (js/Float32Array. (* 4 max-cid))] ;; cids 1..max-cid
        (loop [cid 1]
          (when (<= cid max-cid)
            (let [base (* 4 (dec cid))
                  {:keys [x y scale flags screen?] :or {x 0.0 y 0.0 scale 1.0}} (get effective cid)]
              (aset floats (+ base 0) x)
              (aset floats (+ base 1) y)
              (aset floats (+ base 2) scale)
              (aset floats (+ base 3) (cond (number? flags) flags screen? 1.0 :else 0.0)))
            (recur (inc cid))))
        (.writeBuffer (.-queue device) containers-buffer 16 floats)))))

(defn init-rect-system
  [^js/GPUDevice device fformat camera-buffer
   & {:keys [initial-capacity tracker label containers-buffer]
      :or {initial-capacity 1000
           label "rect/shared-system"}}]
  (assert containers-buffer "init-rect-system requires :containers-buffer (scene-substrate P2)")
  (let [v-module (.createShaderModule device (clj->js {:code rect-vertex-shader}))
        f-module (.createShaderModule device (clj->js {:code rect-fragment-shader}))
        buf-size (* initial-capacity rect-stride)
        instance-buffer (.createBuffer device (clj->js {:size buf-size :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        _ (gpu-budget/register-buffer! tracker instance-buffer label buf-size :active-bytes 0)
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility (bit-or js/GPUShaderStage.VERTEX js/GPUShaderStage.FRAGMENT) :buffer {:type "uniform"}}
                                                                     {:binding 1 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}]}))
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
                                                               {:shaderLocation 6 :offset 96 :format "float32x4"}   ;; gradient_color2
                                                               {:shaderLocation 7 :offset 112 :format "uint32"}]}]}  ;; container_idx
                              :fragment {:module f-module :entryPoint "main"
                                         :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                            :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                              :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout :entries [{:binding 0 :resource {:buffer camera-buffer}}
                                                                                  {:binding 1 :resource {:buffer containers-buffer}}]}))]
    {:pipeline pipeline
     :bind-group bind-group
     :instance-buffer instance-buffer
     :capacity initial-capacity
     :num-instances 0
     :gpu-tracker tracker
     :gpu-label label}))

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

(defn- create-msdf-bind-group [^js/GPUDevice device layout sampler texture-view camera-buffer sizes-buffer containers-buffer]
  (.createBindGroup device
    (clj->js {:layout layout
              :entries [{:binding 0 :resource sampler}
                        {:binding 1 :resource texture-view}
                        {:binding 2 :resource {:buffer camera-buffer}}
                        {:binding 3 :resource {:buffer sizes-buffer}}
                        {:binding 4 :resource {:buffer containers-buffer}}]})))

(defn- create-slug-bind-group [^js/GPUDevice device layout curve-view band-view camera-buffer sizes-buffer containers-buffer]
  (.createBindGroup device
    (clj->js {:layout layout
              :entries [{:binding 0 :resource curve-view}
                        {:binding 1 :resource band-view}
                        {:binding 2 :resource {:buffer camera-buffer}}
                        {:binding 3 :resource {:buffer sizes-buffer}}
                        {:binding 4 :resource {:buffer containers-buffer}}]})))

(defn- create-msdf-font-resources [^js/GPUDevice device tracker font-bitmap texture-label]
  (let [texture (.createTexture device (clj->js {:size {:width (.-width font-bitmap)
                                                        :height (.-height font-bitmap)
                                                        :depthOrArrayLayers 1}
                                                 :format "rgba8unorm"
                                                 :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                                                js/GPUTextureUsage.TEXTURE_BINDING
                                                                js/GPUTextureUsage.COPY_DST)}))
        sampler (.createSampler device (clj->js {:minFilter "linear"
                                                 :magFilter "linear"
                                                 :mipmapFilter "linear"}))
        _ (.copyExternalImageToTexture (.-queue device)
                                       (clj->js {:source font-bitmap})
                                       (clj->js {:texture texture})
                                       (clj->js {:width (.-width font-bitmap)
                                                 :height (.-height font-bitmap)}))]
    (gpu-budget/register-texture! tracker texture texture-label
                                  :format "rgba8unorm"
                                  :width (.-width font-bitmap)
                                  :height (.-height font-bitmap))
    {:font-texture texture
     :font-texture-view (.createView texture)
     :font-sampler sampler
     :font-texture-label texture-label
     :font-resource-kind :msdf}))

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

(defn- destroy-msdf-font-resources! [text-sys]
  (when-let [tracker (:gpu-tracker text-sys)]
    (when-let [texture (:font-texture text-sys)]
      (gpu-budget/destroy-resource! tracker texture :reason :text-font-destroy)))
  (when-let [^js texture (:font-texture text-sys)]
    (.destroy texture)))

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
    (case (:backend text-sys)
      :msdf (destroy-msdf-font-resources! text-sys)
      :slug (destroy-slug-font-resources! text-sys)
      nil)))

(defn- init-msdf-text-system
  [^js/GPUDevice device fformat camera-buffer font-assets
   & {:keys [initial-capacity tracker label containers-buffer]
      :or {initial-capacity 10000
           label "text/content"}}]
  (assert containers-buffer "init-msdf-text-system requires :containers-buffer (scene-substrate P2)")
  (let [font-bitmap (:bitmap font-assets)
        vertex-module (.createShaderModule device (clj->js {:code text-vertex-shader}))
        fragment-module (.createShaderModule device (clj->js {:code text-fragment-shader}))
        font-resources (create-msdf-font-resources device tracker font-bitmap "text/atlas")
        instance-buffer (create-instance-buffer device tracker label initial-capacity msdf-text-instance-stride)
        sizes-buffer (.createBuffer device (clj->js {:size 16
                                                     :usage (bit-or js/GPUBufferUsage.UNIFORM
                                                                    js/GPUBufferUsage.COPY_DST)}))
        _ (gpu-budget/register-buffer! tracker sizes-buffer "text/shared-sizing" 16 :active-bytes 16)
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT :sampler {:type "filtering"}}
                                                                     {:binding 1 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "float"}}
                                                                     {:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                                                     {:binding 3 :visibility js/GPUShaderStage.FRAGMENT :buffer {:type "uniform"}}
                                                                     {:binding 4 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout pipeline-layout
                             :vertex {:module vertex-module
                                      :entryPoint "main"
                                      :buffers [{:arrayStride msdf-text-instance-stride
                                                 :stepMode "instance"
                                                 :attributes [{:shaderLocation 0 :offset 0 :format "float32x4"}
                                                              {:shaderLocation 1 :offset 16 :format "float32x4"}
                                                              {:shaderLocation 2 :offset 32 :format "float32x4"}
                                                              {:shaderLocation 3 :offset 48 :format "uint32"}]}]}
                             :fragment {:module fragment-module
                                        :entryPoint "main"
                                        :targets [{:format fformat
                                                   :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                           :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                             :primitive {:topology "triangle-list"}}))
        bind-group (create-msdf-bind-group device bg-layout (:font-sampler font-resources) (:font-texture-view font-resources) camera-buffer sizes-buffer containers-buffer)]
    (js/console.log "[RENDERER] Init text system"
                    {:backend :msdf
                     :label label
                     :initial-capacity initial-capacity
                     :instance-stride msdf-text-instance-stride
                     :atlas-size [(.-width font-bitmap) (.-height font-bitmap)]})
    (merge font-resources
           {:backend :msdf
            :pipeline pipeline
            :bind-group bind-group
            :bind-group-layout bg-layout
            :camera-uniform-buffer camera-buffer
            :containers-uniform-buffer containers-buffer
            :sizes-uniform-buffer sizes-buffer
            :instance-buffer instance-buffer
            :instance-stride msdf-text-instance-stride
            :num-instances 0
            :gpu-tracker tracker
            :gpu-label label
            :owns-font-resources? true
            :owns-sizing-buffer? true})))

(defn- init-slug-text-system
  [^js/GPUDevice device fformat camera-buffer font-assets
   & {:keys [initial-capacity tracker label containers-buffer]
      :or {initial-capacity 10000
           label "text/content"}}]
  (assert containers-buffer "init-slug-text-system requires :containers-buffer (scene-substrate P2)")
  (let [vertex-module (.createShaderModule device (clj->js {:code slug-vertex-shader}))
        fragment-module (.createShaderModule device (clj->js {:code slug-fragment-shader}))
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
                                                                     {:binding 4 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}]}))
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
                                                   :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                           :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                             :primitive {:topology "triangle-list"}}))
        bind-group (create-slug-bind-group device bg-layout (:curve-texture-view font-resources) (:band-texture-view font-resources) camera-buffer sizes-buffer containers-buffer)]
    (js/console.log "[RENDERER] Init text system"
                    {:backend :slug
                     :label label
                     :initial-capacity initial-capacity
                     :instance-stride slug-text-instance-stride})
    (merge font-resources
           {:backend :slug
            :pipeline pipeline
            :bind-group bind-group
            :bind-group-layout bg-layout
            :camera-uniform-buffer camera-buffer
            :containers-uniform-buffer containers-buffer
            :sizes-uniform-buffer sizes-buffer
            :instance-buffer instance-buffer
            :instance-stride slug-text-instance-stride
            :num-instances 0
            :gpu-tracker tracker
            :gpu-label label
            :owns-font-resources? true
            :owns-sizing-buffer? true})))

(defn init-text-system
  [^js/GPUDevice device fformat camera-buffer font-assets & {:as opts}]
  (if (= :slug (:backend font-assets))
    (apply init-slug-text-system device fformat camera-buffer font-assets (mapcat identity opts))
    (apply init-msdf-text-system device fformat camera-buffer font-assets (mapcat identity opts))))

(defn update-font-assets [^js/GPUDevice device text-sys font-assets]
  (js/console.log "[RENDERER] Update font assets"
                  {:current-backend (:backend text-sys)
                   :requested-backend (:backend font-assets)
                   :font-id (:id font-assets)})
  (if (not= (:backend text-sys) (:backend font-assets))
    (throw (ex-info "Text backend mismatch during font update."
                    {:current (:backend text-sys)
                     :requested (:backend font-assets)}))
    (case (:backend text-sys)
      :msdf
      (let [tracker (:gpu-tracker text-sys)
            old-texture (:font-texture text-sys)
            font-resources (create-msdf-font-resources device tracker (:bitmap font-assets) (or (:font-texture-label text-sys) "text/atlas"))]
        (when (and tracker old-texture (:owns-font-resources? text-sys))
          (gpu-budget/destroy-resource! tracker old-texture :reason :font-update))
        (when (and old-texture (:owns-font-resources? text-sys))
          (.destroy ^js old-texture))
        (merge text-sys
               font-resources
               {:bind-group (create-msdf-bind-group device
                                                    (:bind-group-layout text-sys)
                                                    (:font-sampler font-resources)
                                                    (:font-texture-view font-resources)
                                                    (:camera-uniform-buffer text-sys)
                                                    (:sizes-uniform-buffer text-sys)
                                                    (:containers-uniform-buffer text-sys))
                :owns-font-resources? true}))

      :slug
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
                :owns-font-resources? true})))))

(defn share-font-resources
  "Point a secondary text system at a primary text system's shared font resources."
  [target-state source-state]
  (when (and (:owns-font-resources? target-state)
             (not= (:backend target-state) (:backend source-state)))
    (throw (ex-info "Cannot share font resources across different text backends."
                    {:source (:backend source-state)
                     :target (:backend target-state)})))
  (when (:owns-font-resources? target-state)
    (case (:backend target-state)
      :msdf (destroy-msdf-font-resources! target-state)
      :slug (destroy-slug-font-resources! target-state)
      nil))
  (-> target-state
      (assoc :bind-group (:bind-group source-state)
             :owns-font-resources? false)
      (merge
        (select-keys source-state
                     [:font-texture :font-texture-view :font-sampler :font-texture-label
                      :curve-texture :curve-texture-view :curve-texture-label
                      :band-texture :band-texture-view :band-texture-label
                      :font-resource-kind]))))

(defn recreate-text-system
  [^js/GPUDevice device fformat old-text-sys font-assets]
  (let [capacity (max 1 (quot (.-size ^js (:instance-buffer old-text-sys))
                              (:instance-stride old-text-sys)))
        label (:gpu-label old-text-sys)
        tracker (:gpu-tracker old-text-sys)
        camera-buffer (:camera-uniform-buffer old-text-sys)
        containers-buffer (:containers-uniform-buffer old-text-sys)]
    (js/console.log "[RENDERER] Recreate text system"
                    {:old-backend (:backend old-text-sys)
                     :new-backend (:backend font-assets)
                     :label label
                     :capacity capacity
                     :font-id (:id font-assets)})
    (destroy-text-system! old-text-sys)
    (init-text-system device fformat camera-buffer font-assets
                      :initial-capacity capacity
                      :tracker tracker
                      :label label
                      :containers-buffer containers-buffer)))

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
           :gpu-label "text/chrome"
           :owns-font-resources? false
           :owns-sizing-buffer? false)))

;; --- Shadow system ---
(def shadow-stride 84)  ;; 20 floats + container u32, × 4 bytes (scene-substrate P2)

(defn init-shadow-system
  [^js/GPUDevice device fformat camera-buffer
   & {:keys [initial-capacity tracker label containers-buffer]
      :or {initial-capacity 256
           label "shadow/shared-system"}}]
  (assert containers-buffer "init-shadow-system requires :containers-buffer (scene-substrate P2)")
  (let [v-module (.createShaderModule device (clj->js {:code shadow-vertex-shader}))
        f-module (.createShaderModule device (clj->js {:code shadow-fragment-shader}))
        buf-size (* initial-capacity shadow-stride)
        instance-buffer (.createBuffer device (clj->js {:size buf-size :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))
        _ (gpu-budget/register-buffer! tracker instance-buffer label buf-size :active-bytes 0)
        bg-layout (.createBindGroupLayout device (clj->js {:entries [{:binding 0 :visibility (bit-or js/GPUShaderStage.VERTEX js/GPUShaderStage.FRAGMENT) :buffer {:type "uniform"}}
                                                                     {:binding 1 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}]}))
        pipeline-layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout pipeline-layout
                              :vertex {:module v-module :entryPoint "main"
                                       :buffers [{:arrayStride shadow-stride :stepMode "instance"
                                                  :attributes [{:shaderLocation 0 :offset 0  :format "float32x4"}   ;; expanded_rect
                                                               {:shaderLocation 1 :offset 16 :format "float32x4"}   ;; shadow_color
                                                               {:shaderLocation 2 :offset 32 :format "float32x4"}   ;; corner_radii
                                                               {:shaderLocation 3 :offset 48 :format "float32x4"}   ;; blur_params
                                                               {:shaderLocation 4 :offset 64 :format "float32x4"}   ;; inner_rect
                                                               {:shaderLocation 5 :offset 80 :format "uint32"}]}]}  ;; container_idx
                              :fragment {:module f-module :entryPoint "main"
                                         :targets [{:format fformat :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                                            :alpha {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}}}]}
                              :primitive {:topology "triangle-list"}}))
        bind-group (.createBindGroup device (clj->js {:layout bg-layout :entries [{:binding 0 :resource {:buffer camera-buffer}}
                                                                                  {:binding 1 :resource {:buffer containers-buffer}}]}))]
    {:pipeline pipeline
     :bind-group bind-group
     :instance-buffer instance-buffer
     :capacity initial-capacity
     :num-instances 0
     :gpu-tracker tracker
     :gpu-label label}))

(defn update-shadows [^js device shadow-system shadows]
  (let [n (count shadows)
        floats-per-shadow 21 ;; 20 + container u32 (scene-substrate P2)
        data (js/Float32Array. (* n floats-per-shadow))
        u32-view (js/Uint32Array. (.-buffer data))
        required-bytes (.-byteLength data)
        current-buffer (:instance-buffer shadow-system)
        current-size (.-size ^js current-buffer)
        needs-resize? (> required-bytes current-size)
        new-size (max required-bytes (* n shadow-stride))
        new-buffer (if needs-resize?
                     (.createBuffer device (clj->js {:size new-size
                                                     :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                    js/GPUBufferUsage.COPY_DST)}))
                     current-buffer)]
    (when needs-resize?
      (gpu-budget/replace-buffer! (:gpu-tracker shadow-system) current-buffer new-buffer (:gpu-label shadow-system)
                                  new-size
                                  :active-bytes (* n shadow-stride)
                                  :reason :shadow-resize)
      (.destroy ^js current-buffer))
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
          ;; Slot 5: container_idx (u32 view over the same buffer; trap T6 default 0)
          (aset u32-view (+ base 20) (or (:container-idx s) 0))
          (recur (inc i) (next ss)))))
    (when (pos? n)
      (.writeBuffer (.-queue device) new-buffer 0 data))
    (gpu-budget/set-active-bytes! (:gpu-tracker shadow-system) new-buffer (* n shadow-stride))
    (assoc shadow-system :instance-buffer new-buffer :num-instances n)))

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

(defn init-clear-quad [^js/GPUDevice device fformat]
  (let [module (.createShaderModule device (clj->js {:code clear-quad-shader}))
        layout (.createPipelineLayout device (clj->js {:bindGroupLayouts []}))
        pipeline (.createRenderPipeline device
                   (clj->js {:layout layout
                             :vertex {:module module :entryPoint "vs_main"}
                             :fragment {:module module :entryPoint "fs_main"
                                        :targets [{:format fformat
                                                   :writeMask 0xF}]}
                             :primitive {:topology "triangle-list"}}))]
    {:pipeline pipeline}))

;; --- Persistent render target (Phase 6E: survives swap chain double-buffering) ---

(defn create-render-target
  [^js device width height fformat & {:keys [tracker label previous]
                                      :or {label "render-target/persistent"}}]
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
     :gpu-tracker tracker
     :gpu-label label}))

(defn destroy-render-target! [{:keys [^js texture gpu-tracker]}]
  (when texture
    (gpu-budget/destroy-resource! gpu-tracker texture :reason :render-target-destroy)
    (.destroy texture)))

(defn create-editor-state [{:keys [device format font-assets gpu-budget]}]
  (let [camera-buffer (create-camera-buffer device gpu-budget)
        containers-buffer (create-containers-buffer device gpu-budget)
        text-sys (init-text-system device format camera-buffer font-assets
                                   :initial-capacity 1000000
                                   :tracker gpu-budget
                                   :label "text/content"
                                   :containers-buffer containers-buffer)
        rect-sys (init-rect-system device format (:camera-uniform-buffer text-sys)
                                   :initial-capacity 50000
                                   :tracker gpu-budget
                                   :label "rect/shared-system"
                                   :containers-buffer containers-buffer)
        shadow-sys (init-shadow-system device format (:camera-uniform-buffer text-sys)
                                       :initial-capacity 256
                                       :tracker gpu-budget
                                       :label "shadow/shared-system"
                                       :containers-buffer containers-buffer)
        clear-quad (init-clear-quad device format)

        camera-floats (js/Float32Array. 6)

        pass-descriptor (clj->js {:colorAttachments [{:view nil
                                                      :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 0.0}
                                                      :loadOp "clear"
                                                      :storeOp "store"}]})]
    (js/console.log "[RENDERER] Create editor state"
                    {:format format
                     :font-id (:id font-assets)
                     :font-backend (:backend font-assets)
                     :camera-buffer-bytes 24
                     :containers-buffer-bytes (* max-containers 16)})

    {:text-sys text-sys
     :rect-sys rect-sys
     :shadow-sys shadow-sys
     :clear-quad clear-quad
     :format format
     :camera-floats camera-floats
     :containers-buffer containers-buffer
     :pass-descriptor pass-descriptor}))

;; --- 3. UPDATES (CPU -> GPU) ---
(defn- make-snapper [snap-step]
  (when (and snap-step (pos? snap-step))
    (fn [v] (* (Math/round (/ v snap-step)) snap-step))))

(defn- token-color [{:keys [r g b a]}]
  [(or r 1.0) (or g 1.0) (or b 1.0) (or a 1.0)])

(defn- advance-width [fsize char-width snap]
  (let [v (* fsize char-width)]
    (if snap (snap v) v)))

(defn- glyph-map [glyphs]
  (reduce (fn [acc glyph] (assoc acc (:unicode glyph) glyph)) {} glyphs))

(defn- font-line-height [font-assets]
  (or (get-in font-assets [:atlas :metrics :lineHeight])
      (get-in font-assets [:slug :meta :metrics :lineHeight])
      1.2))

(defn- shape-msdf-line
  [texts global-fsize font-assets & {:keys [char-width snap-step] :or {char-width 0.56}}]
  (let [atlas-w (or (get-in font-assets [:atlas :atlas :width]) 1)
        atlas-h (or (get-in font-assets [:atlas :atlas :height]) 1)
        line-h (font-line-height font-assets)
        glyphs (glyph-map (get-in font-assets [:atlas :glyphs]))
        res (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y]} txt
            [cr cg cb ca] (token-color txt)
            fsize (or (:size txt) global-fsize)
            snap (make-snapper snap-step)
            start-x (if snap (snap x) x)
            start-y (if snap (snap y) y)
            advance (advance-width fsize char-width snap)
            !x (atom start-x)
            !y (atom start-y)]
        (doseq [ch (seq text)]
          (let [code (.charCodeAt ch 0)]
            (cond
              (= ch \newline) (do (reset! !x start-x) (reset! !y (+ @!y (* fsize line-h))))
              (= ch \space) (swap! !x + advance)
              :else
              ;; V3-5: a missing glyph must still ADVANCE (never the old
              ;; zero-advance skip that desynced column math) and draws the
              ;; atlas fallback U+FFFD when present. Defense-in-depth behind
              ;; the cljc sanitizer, which substitutes upstream.
              (let [g (or (get glyphs code) (get glyphs 0xFFFD))
                    x0 @!x]
                (swap! !x + advance)
                (when g
                  (let [pb (:planeBounds g)
                        ab (:atlasBounds g)
                        sl (+ x0 (* fsize (or (:left pb) 0)))
                        sr (+ x0 (* fsize (or (:right pb) 0)))
                        st (- @!y (* fsize (or (:top pb) 0)))
                        sb (- @!y (* fsize (or (:bottom pb) 0)))
                        ul (/ (:left ab) atlas-w)
                        ur (/ (:right ab) atlas-w)
                        vt (- 1.0 (/ (:top ab) atlas-h))
                        vb (- 1.0 (/ (:bottom ab) atlas-h))]
                    (swap! res conj {:rect [sl st (- sr sl) (- sb st)]
                                     :uv [ul vt ur vb]
                                     :color [cr cg cb ca]
                                     :container (or (:container-idx txt) 0)})))))))))
    @res))

(defn- shape-slug-line
  [texts global-fsize font-assets & {:keys [char-width snap-step] :or {char-width 0.56}}]
  (let [line-h (font-line-height font-assets)
        glyphs (glyph-map (get-in font-assets [:slug :meta :glyphs]))
        res (atom [])]
    (doseq [txt texts]
      (let [{:keys [text x y]} txt
            [cr cg cb ca] (token-color txt)
            fsize (or (:size txt) global-fsize)
            snap (make-snapper snap-step)
            start-x (if snap (snap x) x)
            start-y (if snap (snap y) y)
            advance (advance-width fsize char-width snap)
            inv-size (if (pos? fsize) (/ 1.0 fsize) 0.0)
            !x (atom start-x)
            !y (atom start-y)]
        (doseq [ch (seq text)]
          (let [code (.charCodeAt ch 0)]
            (cond
              (= ch \newline) (do (reset! !x start-x) (reset! !y (+ @!y (* fsize line-h))))
              (= ch \space) (swap! !x + advance)
              :else
              ;; V3-5: always advance; draw the fallback glyph when missing
              ;; (slug meta today has no U+FFFD -> honest gap WITH advance).
              (let [g (or (get glyphs code) (get glyphs 0xFFFD))
                    x0 @!x
                    _ (swap! !x + advance)]
                (when g
                  (let [sample-bounds (or (:sampleBounds g) (:planeBounds g))
                      slug (:slug g)
                      left (or (:left sample-bounds) 0.0)
                      right (or (:right sample-bounds) 0.0)
                      top (or (:top sample-bounds) 0.0)
                      bottom (or (:bottom sample-bounds) 0.0)
                      world-left (+ x0 (* fsize left))
                      world-right (+ x0 (* fsize right))
                      world-top (- @!y (* fsize top))
                      world-bottom (- @!y (* fsize bottom))]
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
                                   :container (or (:container-idx txt) 0)})))))))))
    @res))

(defn shape-text [texts global-fsize font-assets & {:as opts}]
  (if (= :slug (:backend font-assets))
    (apply shape-slug-line texts global-fsize font-assets (mapcat identity opts))
    (apply shape-msdf-line texts global-fsize font-assets (mapcat identity opts))))

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
        new-buffer (if needs-resize?
                     (.createBuffer device (clj->js {:size required-size
                                                     :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                    js/GPUBufferUsage.COPY_DST)}))
                     current-buffer)]
    (when needs-resize?
      (gpu-budget/replace-buffer! (:gpu-tracker renderer-state) current-buffer new-buffer (:gpu-label renderer-state)
                                  required-size
                                  :active-bytes active-bytes
                                  :reason :text-resize)
      (.destroy ^js current-buffer))
    new-buffer))

(defn- pack-msdf-instances! [^js float-view ^js uint-view shaped-lines]
  (loop [lines shaped-lines
         global-i 0]
    (when (seq lines)
      (let [instances (:instances (first lines))]
        (loop [remaining instances
               sub-i 0]
          (when (seq remaining)
            (let [{:keys [rect uv color container]} (first remaining)
                  [x y w h] rect
                  [u-min v-min u-max v-max] uv
                  [cr cg cb ca] color
                  base (* (+ global-i sub-i) 13)]
              (aset float-view (+ base 0) x)
              (aset float-view (+ base 1) y)
              (aset float-view (+ base 2) w)
              (aset float-view (+ base 3) h)
              (aset float-view (+ base 4) u-min)
              (aset float-view (+ base 5) v-min)
              (aset float-view (+ base 6) u-max)
              (aset float-view (+ base 7) v-max)
              (aset float-view (+ base 8) cr)
              (aset float-view (+ base 9) cg)
              (aset float-view (+ base 10) cb)
              (aset float-view (+ base 11) ca)
              (aset uint-view (+ base 12) (or container 0))
              (recur (next remaining) (inc sub-i)))))
        (recur (next lines) (+ global-i (:count (first lines))))))))

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
   & {:keys [px-range line-height-factor line-height sharpness char-width snap-step]
      :or {px-range 8.0 line-height-factor 1.0 sharpness 0.0 char-width 0.56}}]
  (when (not= (:backend renderer-state) (:backend font-assets))
    (throw (ex-info "Text backend mismatch during text upload."
                    {:renderer-backend (:backend renderer-state)
                     :font-backend (:backend font-assets)})))
  (let [line-h (or line-height (* font-size line-height-factor))
        shaped-lines (mapv (fn [tokens-in-line]
                             (let [instances (shape-text tokens-in-line font-size font-assets
                                                         :char-width char-width
                                                         :snap-step snap-step)]
                               {:instances instances
                                :count (count instances)}))
                           texts)
        actual-instances (reduce + (map :count shaped-lines))
        buffer-instance-count (max actual-instances 1)
        stride (:instance-stride renderer-state)
        line-offsets (line-offsets-for shaped-lines)
        active-bytes (* actual-instances stride)]
    (case (:backend renderer-state)
      :msdf
      (let [raw-buffer (js/ArrayBuffer. (* buffer-instance-count stride))
            float-view (js/Float32Array. raw-buffer)
            uint-view (js/Uint32Array. raw-buffer)
            upload-view (js/Uint8Array. raw-buffer)
            required-size (.-byteLength upload-view)
            new-buffer (ensure-text-instance-buffer device renderer-state required-size active-bytes)
            atlas-em (or (get-in font-assets [:atlas :atlas :size]) 64.0)]
        (pack-msdf-instances! float-view uint-view shaped-lines)
        (.writeBuffer (.-queue device) new-buffer 0 upload-view)
        (when-let [sizes-buffer (:sizes-uniform-buffer renderer-state)]
          (let [sizes (js/Float32Array. #js [(float px-range) (float atlas-em) (float sharpness) 0.0])]
            (.writeBuffer (.-queue device) sizes-buffer 0 sizes)))
        (gpu-budget/set-active-bytes! (:gpu-tracker renderer-state) new-buffer active-bytes)
        (assoc renderer-state
               :instance-buffer new-buffer
               :num-instances actual-instances
               :line-offsets line-offsets
               :line-height line-h))

      :slug
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
        (assoc renderer-state
               :instance-buffer new-buffer
               :num-instances actual-instances
               :line-offsets line-offsets
               :line-height line-h)))))


(defn update-rects [^js device rect-system rects]
  (let [n (count rects)
        floats-per-rect 29 ;; 28 + container u32 (scene-substrate P2)
        data (js/Float32Array. (* n floats-per-rect))
        u32-view (js/Uint32Array. (.-buffer data))
        required-bytes (.-byteLength data)
        current-buffer (:instance-buffer rect-system)
        current-size (.-size ^js current-buffer)
        needs-resize? (> required-bytes current-size)
        new-size (max required-bytes (* n rect-stride))
        new-buffer (if needs-resize?
                     (.createBuffer device (clj->js {:size new-size
                                                     :usage (bit-or js/GPUBufferUsage.VERTEX
                                                                    js/GPUBufferUsage.COPY_DST)}))
                     current-buffer)]
    (when needs-resize?
      (gpu-budget/replace-buffer! (:gpu-tracker rect-system) current-buffer new-buffer (:gpu-label rect-system)
                                  new-size
                                  :active-bytes (* n rect-stride)
                                  :reason :rect-resize)
      (.destroy ^js current-buffer))
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
          ;; Slot 7: container_idx (u32 view over the same buffer; trap T6 default 0)
          (aset u32-view (+ base 28) (or (:container-idx rect) 0))
          (recur (inc i) (next rs)))))
    (when (pos? n)
      (.writeBuffer (.-queue device) new-buffer 0 data))
    (gpu-budget/set-active-bytes! (:gpu-tracker rect-system) new-buffer (* n rect-stride))
    (assoc rect-system :instance-buffer new-buffer :num-instances n)))


(defn update-camera [^js device camera-buffer ^js floats pan-x pan-y zoom w h]
  (aset floats 0 pan-x)
  (aset floats 1 pan-y)
  (aset floats 2 zoom)
  (aset floats 3 0.0)
  (aset floats 4 w)
  (aset floats 5 h)
  (.writeBuffer (.-queue device) camera-buffer 0 floats))

(defn draw-comparison-frame!
  "Draw two text systems side-by-side: left = primary backend, right = comparison backend.
   Each half gets screen_dimensions = (w/2, h) so text renders at full scale.
   Rects, shadows, chrome are skipped — this is a pure text rendering comparison."
  [^js device ^js context primary-sys comparison-sys ^js camera-floats pan-x pan-y w h]
  (let [half-w (/ w 2.0)
        dpr (or (.-devicePixelRatio js/window) 1)
        phys-w (Math/floor (* w dpr))
        phys-h (Math/floor (* h dpr))
        phys-half-w (Math/floor (* half-w dpr))
        ;; Scratch buffer for comparison camera (avoid mutating camera-floats twice for same buffer)
        comp-floats (js/Float32Array. 6)]
    ;; Update both cameras — each sees half-width viewport
    (update-camera device (:camera-uniform-buffer primary-sys) camera-floats pan-x pan-y 1.0 half-w h)
    (update-camera device (:camera-uniform-buffer comparison-sys) comp-floats pan-x pan-y 1.0 half-w h)

    (let [encoder (.createCommandEncoder device)
          swap-texture (.getCurrentTexture context)
          swap-view (.createView swap-texture)
          pass (.beginRenderPass encoder
                 (clj->js {:colorAttachments [{:view swap-view
                                               :clearValue {:r 0.06 :g 0.06 :b 0.06 :a 1.0}
                                               :loadOp "clear"
                                               :storeOp "store"}]}))]

      ;; Left half — primary backend
      (.setViewport pass 0 0 phys-half-w phys-h 0 1)
      (when (and primary-sys (> (:num-instances primary-sys) 0))
        (.setPipeline pass (:pipeline primary-sys))
        (.setBindGroup pass 0 (:bind-group primary-sys))
        (.setVertexBuffer pass 0 (:instance-buffer primary-sys))
        (.draw pass 6 (:num-instances primary-sys) 0 0))

      ;; Right half — comparison backend
      (.setViewport pass phys-half-w 0 (- phys-w phys-half-w) phys-h 0 1)
      (when (and comparison-sys (> (:num-instances comparison-sys) 0))
        (.setPipeline pass (:pipeline comparison-sys))
        (.setBindGroup pass 0 (:bind-group comparison-sys))
        (.setVertexBuffer pass 0 (:instance-buffer comparison-sys))
        (.draw pass 6 (:num-instances comparison-sys) 0 0))

      (.end pass)
      (.submit (.-queue device) #js [(.finish encoder)]))))


(defn draw-frame! [^js device ^js context text-sys editor-pool-info cmd-rect-sys camera-floats _ignored_pass_descriptor pan-x pan-y w h
                   & {:keys [cmd-panel-visible cmd-panel-h chrome-text-sys chrome-base-line-count
                             settings-line-count settings-visible settings-rect-sys
                             diagnostics-visible diagnostics-line-index agent-visible
                             editor-shadow-pool-info sidebar-shadow-pool-info sidebar-pool-info
                             dirty-rect render-target clear-quad frame-idx zoom
                             extra-text-geos]
                      :or {cmd-panel-visible false cmd-panel-h 40 chrome-text-sys nil chrome-base-line-count 0
                           settings-line-count 0 settings-visible false
                           settings-rect-sys nil agent-visible false
                           editor-shadow-pool-info nil sidebar-shadow-pool-info nil sidebar-pool-info nil
                           dirty-rect nil render-target nil clear-quad nil frame-idx 0
                           zoom 1.0 extra-text-geos nil}}]
  ;; scene-substrate P2: the world camera zoom wakes — callers may drive it;
  ;; default 1.0 keeps every existing call byte-identical.
  (update-camera device (:camera-uniform-buffer text-sys) camera-floats pan-x pan-y zoom w h)
  (when (and chrome-text-sys
             (not= (:camera-uniform-buffer chrome-text-sys) (:camera-uniform-buffer text-sys)))
    (update-camera device (:camera-uniform-buffer chrome-text-sys) camera-floats pan-x pan-y zoom w h))

  (let [encoder (.createCommandEncoder device)
          swap-texture (.getCurrentTexture context)
          swap-view (.createView swap-texture)
          ;; Phase 6E: always render to persistent target (survives swap chain double-buffering)
          ;; dirty-rect non-nil → loadOp "load" + scissor + clear-quad (partial redraw)
          ;; dirty-rect nil → loadOp "clear" (first frame, resize, text/font change)
          use-rt? (some? render-target)
          target-view (if use-rt? (:view render-target) swap-view)
          partial? (and use-rt? dirty-rect)
          load-op (if partial? "load" "clear")

          pass-descriptor (clj->js
                            {:colorAttachments [{:view target-view
                                                 :clearValue {:r 0.0 :g 0.0 :b 0.0 :a 1.0}
                                                 :loadOp load-op
                                                 :storeOp "store"}]})

          pass (.beginRenderPass encoder pass-descriptor)]

      (when (<= frame-idx 5)
        (let [canvas (.-canvas context)
              rect (when canvas (.getBoundingClientRect canvas))]
          (js/console.log "[RENDER/PRESENT]"
                          (str "{\"frame\":" frame-idx
                               ",\"canvasWidth\":" (or (some-> canvas .-width) -1)
                               ",\"canvasHeight\":" (or (some-> canvas .-height) -1)
                               ",\"clientWidth\":" (or (some-> canvas .-clientWidth) -1)
                               ",\"clientHeight\":" (or (some-> canvas .-clientHeight) -1)
                               ",\"rectWidth\":" (or (some-> rect .-width) -1)
                               ",\"rectHeight\":" (or (some-> rect .-height) -1)
                               ",\"swapWidth\":" (or (some-> swap-texture .-width) -1)
                               ",\"swapHeight\":" (or (some-> swap-texture .-height) -1)
                               ",\"useRenderTarget\":" (if use-rt? "true" "false")
                               ",\"contentInstances\":" (:num-instances text-sys)
                               ",\"chromeInstances\":" (or (:num-instances chrome-text-sys) 0)
                               "}"))))

      ;; Phase 6E: scissor + clear-quad for partial redraw
      (when (and partial? clear-quad)
        (let [{:keys [x y]} dirty-rect
              dw (:w dirty-rect)
              dh (:h dirty-rect)]
          (.setScissorRect pass (int x) (int y) (int (max 1 dw)) (int (max 1 dh)))
          (.setPipeline pass (:pipeline clear-quad))
          (.draw pass 3)))

      ;; Draw shadows FIRST (behind everything) — per-source pools (Phase 6C)
      (when (and editor-shadow-pool-info (> (:draw-count editor-shadow-pool-info) 0))
        (.setPipeline pass (:pipeline editor-shadow-pool-info))
        (.setBindGroup pass 0 (:bind-group editor-shadow-pool-info))
        (.setVertexBuffer pass 0 (:buffer editor-shadow-pool-info))
        (.draw pass 6 (:draw-count editor-shadow-pool-info)))
      (when (and sidebar-shadow-pool-info (> (:draw-count sidebar-shadow-pool-info) 0))
        (.setPipeline pass (:pipeline sidebar-shadow-pool-info))
        (.setBindGroup pass 0 (:bind-group sidebar-shadow-pool-info))
        (.setVertexBuffer pass 0 (:buffer sidebar-shadow-pool-info))
        (.draw pass 6 (:draw-count sidebar-shadow-pool-info)))

      ;; Draw sidebar pool (behind editor content, uses differential buffer)
      (when (and sidebar-pool-info (> (:draw-count sidebar-pool-info) 0))
        (.setPipeline pass (:pipeline sidebar-pool-info))
        (.setBindGroup pass 0 (:bind-group sidebar-pool-info))
        (.setVertexBuffer pass 0 (:buffer sidebar-pool-info))
        (.draw pass 6 (:draw-count sidebar-pool-info)))

      ;; Draw editor rects (differential pool — selection, brackets, fold indicators, caret, eval)
      (when (and editor-pool-info (> (:draw-count editor-pool-info) 0))
        (.setPipeline pass (:pipeline editor-pool-info))
        (.setBindGroup pass 0 (:bind-group editor-pool-info))
        (.setVertexBuffer pass 0 (:buffer editor-pool-info))
        (.draw pass 6 (:draw-count editor-pool-info)))

      ;; Draw content text (editor + sidebar — all instances, viewport culled at data level)
      (when (and text-sys (> (:num-instances text-sys) 0))
        (.setPipeline pass (:pipeline text-sys))
        (.setBindGroup pass 0 (:bind-group text-sys))
        (.setVertexBuffer pass 0 (:instance-buffer text-sys))
        (.draw pass 6 (:num-instances text-sys) 0 0))

      ;; scene-substrate P3b Rung 2: per-slot isolated text geos (drawn AFTER
      ;; content, before chrome). Each is a clone of the content text system
      ;; (shared pipeline/bind-group/camera/containers-buffer), so its OWN
      ;; instance buffer draws at its container's transform — msdf AND slug both
      ;; work because a clone inherits the parent's backend pipeline (T12: no
      ;; second text path). G8 isolation: a slot edit reshapes only its own geo.
      (doseq [geo extra-text-geos]
        (when (and geo (> (:num-instances geo) 0))
          (.setPipeline pass (:pipeline geo))
          (.setBindGroup pass 0 (:bind-group geo))
          (.setVertexBuffer pass 0 (:instance-buffer geo))
          (.draw pass 6 (:num-instances geo) 0 0)))

      (let [chrome-ready? (and chrome-text-sys (> (:num-instances chrome-text-sys) 0))
            chrome-offsets (:line-offsets chrome-text-sys)
            chrome-lines (when chrome-offsets (count chrome-offsets))
            chrome-base chrome-base-line-count]

          ;; Agent output background (instance 0) — draw behind chrome text
          (when (and agent-visible cmd-rect-sys (>= (:num-instances cmd-rect-sys) 1))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 0))

          ;; Command panel background (instance 1)
          (when (and cmd-panel-visible chrome-ready?
                     cmd-rect-sys (>= (:num-instances cmd-rect-sys) 2))
            (.setPipeline pass (:pipeline cmd-rect-sys))
            (.setBindGroup pass 0 (:bind-group cmd-rect-sys))
            (.setVertexBuffer pass 0 (:instance-buffer cmd-rect-sys))
            (.draw pass 6 1 0 1))

          ;; Chrome base text (cmd + agent + status — before settings bg)
          (when chrome-ready?
            (let [base-end (if (and chrome-offsets (< chrome-base chrome-lines))
                             (nth chrome-offsets chrome-base)
                             (:num-instances chrome-text-sys))]
              (when (> base-end 0)
                (.setPipeline pass (:pipeline chrome-text-sys))
                (.setBindGroup pass 0 (:bind-group chrome-text-sys))
                (.setVertexBuffer pass 0 (:instance-buffer chrome-text-sys))
                (.draw pass 6 base-end 0 0))))

          ;; Caret (instance 2)
          (when (and cmd-panel-visible chrome-ready?
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
            (when (and settings-rect-sys (> (:num-instances settings-rect-sys) 0))
              (.setPipeline pass (:pipeline settings-rect-sys))
              (.setBindGroup pass 0 (:bind-group settings-rect-sys))
              (.setVertexBuffer pass 0 (:instance-buffer settings-rect-sys))
              (.draw pass 6 (:num-instances settings-rect-sys)))

            ;; Settings text (from chrome buffer, after base chrome lines)
            (when (and chrome-ready? (pos? settings-line-count) chrome-offsets)
              (let [settings-start-line chrome-base
                    settings-end-line (+ settings-start-line settings-line-count)
                    settings-start-inst (if (< settings-start-line chrome-lines)
                                          (nth chrome-offsets settings-start-line)
                                          (:num-instances chrome-text-sys))
                    settings-end-inst (if (< settings-end-line chrome-lines)
                                        (nth chrome-offsets settings-end-line)
                                        (:num-instances chrome-text-sys))
                    settings-draw-count (- settings-end-inst settings-start-inst)]
                (when (> settings-draw-count 0)
                  (.setPipeline pass (:pipeline chrome-text-sys))
                  (.setBindGroup pass 0 (:bind-group chrome-text-sys))
                  (.setVertexBuffer pass 0 (:instance-buffer chrome-text-sys))
                  (.draw pass 6 settings-draw-count 0 settings-start-inst)))))

          ;; Diagnostics overlay
          (when (and diagnostics-visible (not cmd-panel-visible) (not settings-visible)
                     diagnostics-line-index chrome-ready? chrome-offsets)
            (when (< diagnostics-line-index chrome-lines)
              (let [start-inst (nth chrome-offsets diagnostics-line-index)
                    next-line (inc diagnostics-line-index)
                    end-inst (if (< next-line chrome-lines)
                               (nth chrome-offsets next-line)
                               (:num-instances chrome-text-sys))
                    draw-count (- end-inst start-inst)]
                (when (> draw-count 0)
                  (.setPipeline pass (:pipeline chrome-text-sys))
                  (.setBindGroup pass 0 (:bind-group chrome-text-sys))
                  (.setVertexBuffer pass 0 (:instance-buffer chrome-text-sys))
                  (.draw pass 6 draw-count 0 start-inst))))))

    (.end pass)

    ;; Phase 6E: copy persistent render target → swap chain for presentation
    (when use-rt?
      (let [rt-tex (:texture render-target)]
        (.copyTextureToTexture encoder
          (clj->js {:texture rt-tex})
          (clj->js {:texture swap-texture})
          (clj->js {:width (:width render-target)
                    :height (:height render-target)}))))

    (.submit (.-queue device) #js [(.finish encoder)])))
