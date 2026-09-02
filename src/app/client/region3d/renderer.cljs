(ns app.client.region3d.renderer
  "The 3D renderer: uploads a region's evaluated scene and session, encodes its
   shadow and interior passes into a lease, and composites the result into the
   2D frame. Also attaches the compositor per device.
   Takes: a device and format to build the system; regions with zoom, dpr, and
   font assets to prepare; an encoder, a region id, a role, and a lease to
   encode a pass; a render pass to composite.
   Gives: a region system; a per-frame result; encoded passes; the composited
   region.
   Holds: one system per device and the compositor per device (WeakMaps), plus
   per-system atoms for prepared scenes and composite rows."
  (:require [app.client.region3d.frame :as frame]
            [app.client.region3d.component :as component]
            [app.client.region3d.on-plane :as on-plane]
            [app.client.region3d.scene :as scene]
            [app.client.engine.color :as color]
            [app.client.engine.compositor :as compositor]
            [app.client.engine.leases :as region-bindings]
            [app.client.engine.transform :as transform]
            [app.client.region3d.on-plane-renderer
             :as on-plane-renderer]))

(def region3d-gpu-version 1)
(def max-lights 8)
(def mesh-vertex-stride 24)
(def mesh-instance-stride 112)
(def light-instance-stride 80)
(def composite-instance-stride 20)
(def region-uniform-bytes 112)
(def shadow-uniform-bytes 64)

(def mesh-vertex-shader
  "struct Region {
     view_proj: mat4x4<f32>, eye: vec4<f32>, ambient: vec4<f32>,
     settings: vec3<f32>,
   };
   @group(0) @binding(0) var<uniform> region: Region;
   struct VertexIn {
     @location(0) position: vec3<f32>, @location(1) normal: vec3<f32>,
     @location(2) m0: vec4<f32>, @location(3) m1: vec4<f32>,
     @location(4) m2: vec4<f32>, @location(5) m3: vec4<f32>,
     @location(6) base: vec4<f32>, @location(7) pbr: vec4<f32>,
     @location(8) emissive: vec4<f32>,
   };
   struct VertexOut {
     @builtin(position) position: vec4<f32>,
     @location(0) world: vec3<f32>, @location(1) normal: vec3<f32>,
     @location(2) base: vec4<f32>, @location(3) pbr: vec4<f32>,
     @location(4) emissive: vec3<f32>,
   };
   @vertex fn main(input: VertexIn) -> VertexOut {
     let model = mat4x4<f32>(input.m0, input.m1, input.m2, input.m3);
     let world4 = model * vec4<f32>(input.position, 1.0);
     let basis_x = input.m0.xyz; let basis_y = input.m1.xyz;
     let basis_z = input.m2.xyz;
     let inv_det = 1.0 / dot(basis_x, cross(basis_y, basis_z));
     let normal_matrix = mat3x3<f32>(cross(basis_y, basis_z) * inv_det,
                                      cross(basis_z, basis_x) * inv_det,
                                      cross(basis_x, basis_y) * inv_det);
     var out: VertexOut;
     out.position = region.view_proj * world4;
     out.world = world4.xyz;
     out.normal = normalize(normal_matrix * input.normal);
     out.base = input.base; out.pbr = input.pbr;
     out.emissive = input.emissive.xyz;
     return out;
   }")

(def mesh-fragment-common
  "struct Region {
     view_proj: mat4x4<f32>, eye: vec4<f32>, ambient: vec4<f32>,
     settings: vec3<f32>,
   };
   struct Light {
     kind: f32, intensity: f32, range: f32, casts_shadow: f32,
     position: vec4<f32>, direction: vec4<f32>, color: vec4<f32>,
     cone: vec4<f32>,
   };
   @group(0) @binding(0) var<uniform> region: Region;
   @group(0) @binding(1) var<storage, read> lights: array<Light>;
   @group(0) @binding(2) var shadow_map: texture_depth_2d;
   @group(0) @binding(3) var shadow_sampler: sampler_comparison;
   @group(0) @binding(4) var<uniform> light_view_proj: mat4x4<f32>;
   const PI: f32 = 3.141592653589793;
   const SHADOW_OFFSET: f32 = 0.0015;
   fn fresnel_schlick(f0: vec3<f32>, vdh: f32) -> vec3<f32> {
     return f0 + (vec3<f32>(1.0) - f0) * pow(1.0 - vdh, 5.0);
   }
   fn pbr(base: vec3<f32>, metallic: f32, roughness: f32,
          n: vec3<f32>, v: vec3<f32>, l: vec3<f32>, radiance: vec3<f32>)
       -> vec3<f32> {
     let h = normalize(v + l);
     let ndl = max(dot(n, l), 0.0);
     let ndv = max(dot(n, v), 0.0000001);
     let ndh = max(dot(n, h), 0.0);
     let vdh = max(dot(v, h), 0.0);
     let alpha = max(0.0025, roughness * roughness);
     let alpha2 = alpha * alpha;
     let denom = ndh * ndh * (alpha2 - 1.0) + 1.0;
     let distribution = alpha2 / (PI * denom * denom);
     let visibility_denom = ndl * sqrt(ndv * ndv * (1.0 - alpha2) + alpha2)
                          + ndv * sqrt(ndl * ndl * (1.0 - alpha2) + alpha2);
     let visibility = select(0.0, 0.5 / visibility_denom,
                             visibility_denom > 0.0);
     let f0 = mix(vec3<f32>(0.04), base, metallic);
     let f = fresnel_schlick(f0, vdh);
     let specular = distribution * visibility * f;
     let diffuse = base / PI * (1.0 - metallic) * (vec3<f32>(1.0) - f);
     return (diffuse + specular) * radiance * ndl;
   }
   fn neutral_tone_map(input_color: vec3<f32>) -> vec3<f32> {
     let x = min(input_color, vec3<f32>(0.08));
     let offset = x - 6.25 * x * x;
     var color = input_color - offset;
     let peak = max(color.r, max(color.g, color.b));
     if (peak < 0.76) { return color; }
     let distance = 0.24;
     let new_peak = 1.0 - distance * distance / (peak + distance - 0.76);
     color = color * (new_peak / peak);
     let amount = 1.0 - 1.0 / (0.15 * (peak - new_peak) + 1.0);
     return mix(color, vec3<f32>(new_peak), amount);
   }
   fn shadow_factor(world: vec3<f32>, normal: vec3<f32>) -> f32 {
     if (region.settings.z < 0.5) { return 1.0; }
     let clip = light_view_proj * vec4<f32>(world, 1.0);
     let ndc = clip.xyz / clip.w;
     let uv = vec2<f32>(ndc.x * 0.5 + 0.5, 0.5 - ndc.y * 0.5);
     if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0 ||
         ndc.z < 0.0 || ndc.z > 1.0) { return 1.0; }
     let dims = vec2<f32>(textureDimensions(shadow_map));
     var sum = 0.0;
     for (var y = -1; y <= 1; y = y + 1) {
       for (var x = -1; x <= 1; x = x + 1) {
         let delta = vec2<f32>(f32(x), f32(y)) / dims;
         sum += textureSampleCompareLevel(shadow_map, shadow_sampler, uv + delta,
                                          ndc.z - SHADOW_OFFSET);
       }
     }
     return sum / 9.0;
   }
   fn light_radiance(light: Light, point: vec3<f32>) -> vec4<f32> {
     if (light.kind < 0.5) {
       return vec4<f32>(normalize(-light.direction.xyz),
                        light.intensity);
     }
     let delta = light.position.xyz - point;
     let distance = length(delta);
     let inv_sq = 1.0 / max(distance * distance, 0.0000001);
     let cutoff = pow(clamp(1.0 - pow(distance / light.range, 4.0), 0.0, 1.0), 2.0);
     var attenuation = inv_sq * cutoff;
     if (light.kind > 1.5) {
       let cosine = dot(normalize(point - light.position.xyz),
                        normalize(light.direction.xyz));
       attenuation *= clamp((cosine - light.cone.y) /
                            max(light.cone.x - light.cone.y, 0.0000001), 0.0, 1.0);
     }
     return vec4<f32>(normalize(delta), light.intensity * attenuation);
   }")

(def mesh-fragment-shader
  (str mesh-fragment-common
       "struct FragmentIn {
          @location(0) world: vec3<f32>, @location(1) normal: vec3<f32>,
          @location(2) base: vec4<f32>, @location(3) pbr_params: vec4<f32>,
          @location(4) emissive: vec3<f32>,
        };
        @fragment fn main(input: FragmentIn) -> @location(0) vec4<f32> {
          let n = normalize(input.normal);
          if (region.settings.y > 0.5 && region.settings.y < 1.5) {
            return vec4<f32>(input.base.rgb * input.base.a, input.base.a);
          }
          if (region.settings.y >= 1.5) {
            let normal_color = n * 0.5 + vec3<f32>(0.5);
            return vec4<f32>(normal_color * input.base.a, input.base.a);
          }
          let view = normalize(region.eye.xyz - input.world);
          var color = input.base.rgb * region.ambient.rgb * region.ambient.a
                    + input.emissive;
          let light_count = min(u32(region.settings.x), " max-lights "u);
          for (var i = 0u; i < light_count; i = i + 1u) {
            let lr = light_radiance(lights[i], input.world);
            var visibility = 1.0;
            if (lights[i].casts_shadow > 0.5) {
              visibility = shadow_factor(input.world, n);
            }
            color += pbr(input.base.rgb, input.pbr_params.x,
                         input.pbr_params.y, n, view, lr.xyz,
                         lights[i].color.rgb * lr.w * visibility);
          }
          let mapped = neutral_tone_map(color);
          return vec4<f32>(mapped * input.base.a, input.base.a);
        }"))

(def shadow-depth-shader
  "struct Input {
     @location(0) position: vec3<f32>, @location(1) normal: vec3<f32>,
     @location(2) m0: vec4<f32>, @location(3) m1: vec4<f32>,
     @location(4) m2: vec4<f32>, @location(5) m3: vec4<f32>,
     @location(6) base: vec4<f32>, @location(7) pbr: vec4<f32>,
     @location(8) emissive: vec4<f32>,
   };
   @group(0) @binding(0) var<uniform> light_view_proj: mat4x4<f32>;
   @vertex fn main(input: Input) -> @builtin(position) vec4<f32> {
     let model = mat4x4<f32>(input.m0, input.m1, input.m2, input.m3);
     return light_view_proj * model * vec4<f32>(input.position, 1.0);
   }")

(def composite-vertex-shader
  "struct Camera { pan: vec2<f32>, zoom: f32, padding: f32,
                   screen_dimensions: vec2<f32>, };
   struct GroupTransform { axis_x: vec2<f32>, axis_y: vec2<f32>,
     translation: vec2<f32>, flags: u32, padding: u32, };
   @group(0) @binding(2) var<uniform> camera: Camera;
   @group(0) @binding(3) var<storage, read> groups: array<GroupTransform>;
   struct In { @location(0) rect: vec4<f32>, @location(1) group_buffer_index: u32, };
   struct Out { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32>, };
   @vertex fn main(@builtin(vertex_index) index: u32, input: In) -> Out {
     var corners = array<vec2<f32>, 6>(vec2<f32>(0.0,0.0), vec2<f32>(1.0,0.0),
       vec2<f32>(0.0,1.0), vec2<f32>(1.0,0.0), vec2<f32>(1.0,1.0), vec2<f32>(0.0,1.0));
     let uv = corners[index]; let local = input.rect.xy + uv * input.rect.zw;
     let c = groups[input.group_buffer_index]; let screen = (c.flags & 1u) != 0u;
     let zoom = select(camera.zoom, 1.0, screen);
     let pan = select(camera.pan, vec2<f32>(0.0), screen);
     let world = c.translation + c.axis_x * local.x + c.axis_y * local.y;
     let pixel = world * zoom + pan;
     let ndc = pixel / camera.screen_dimensions * 2.0 - vec2<f32>(1.0);
     var out: Out; out.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
     out.uv = uv; return out;
   }")

(def composite-fragment-shader
  "@group(0) @binding(0) var region_sampler: sampler;
   @group(0) @binding(1) var region_resolve: texture_2d<f32>;
   @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
     return textureSample(region_resolve, region_sampler, uv);
   }")

(def worn-fragment-shader
  "@group(0) @binding(0) var region_sampler: sampler;
   @group(0) @binding(1) var region_resolve: texture_2d<f32>;
   @fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
     let resolved = textureSample(region_resolve, region_sampler, uv);
     let corner = uv.x > 0.90 && uv.y < 0.10;
     let stripe = fract((uv.x + uv.y) * 72.0) > 0.42;
     let worn = corner && stripe;
     return select(resolved, vec4<f32>(0.16,0.78,0.92,1.0), worn);
   }")

(def rejection-fragment-shader
  "@fragment fn main(@location(0) uv: vec2<f32>) -> @location(0) vec4<f32> {
     let checker = f32((u32(floor(uv.x * 12.0)) + u32(floor(uv.y * 12.0))) & 1u);
     var color = mix(vec3<f32>(0.18,0.055,0.065), vec3<f32>(0.32,0.09,0.10), checker);
     let corner = uv.x > 0.88 && uv.y < 0.12;
     color = select(color, vec3<f32>(1.0,0.55,0.16), corner);
     return vec4<f32>(color, 1.0);
   }")

(defn- shader-module [device code]
  (.createShaderModule ^js device (clj->js {:code code})))

(defn- blend-state []
  {:color {:srcFactor "one" :dstFactor "one-minus-src-alpha"}
   :alpha {:srcFactor "one" :dstFactor "one-minus-src-alpha"}})

(def mesh-vertex-layouts
  [{:arrayStride mesh-vertex-stride :stepMode "vertex"
    :attributes [{:shaderLocation 0 :offset 0 :format "float32x3"}
                 {:shaderLocation 1 :offset 12 :format "float32x3"}]}
   {:arrayStride mesh-instance-stride :stepMode "instance"
    :attributes [{:shaderLocation 2 :offset 0 :format "float32x4"}
                 {:shaderLocation 3 :offset 16 :format "float32x4"}
                 {:shaderLocation 4 :offset 32 :format "float32x4"}
                 {:shaderLocation 5 :offset 48 :format "float32x4"}
                 {:shaderLocation 6 :offset 64 :format "float32x4"}
                 {:shaderLocation 7 :offset 80 :format "float32x4"}
                 {:shaderLocation 8 :offset 96 :format "float32x4"}]}])

(defn- create-pipelines! [device]
  (let [mesh-vertex (shader-module device mesh-vertex-shader)
        mesh-fragment (shader-module device mesh-fragment-shader)
        shadow-module (shader-module device shadow-depth-shader)
        composite-vertex (shader-module device composite-vertex-shader)
        composite-fragment (shader-module device composite-fragment-shader)
        worn-fragment (shader-module device worn-fragment-shader)
        rejection-fragment (shader-module device rejection-fragment-shader)
        interior-layout
        (.createBindGroupLayout
         ^js device
         (clj->js {:entries [{:binding 0 :visibility (bit-or js/GPUShaderStage.VERTEX
                                                             js/GPUShaderStage.FRAGMENT)
                              :buffer {:type "uniform"}}
                             {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                              :buffer {:type "read-only-storage"}}
                             {:binding 2 :visibility js/GPUShaderStage.FRAGMENT
                              :texture {:sampleType "depth"}}
                             {:binding 3 :visibility js/GPUShaderStage.FRAGMENT
                              :sampler {:type "comparison"}}
                             {:binding 4 :visibility js/GPUShaderStage.FRAGMENT
                              :buffer {:type "uniform"}}]}))
        shadow-layout
        (.createBindGroupLayout
         ^js device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.VERTEX
                                         :buffer {:type "uniform"}}]}))
        composite-layout
        (.createBindGroupLayout
         ^js device (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT
                                         :sampler {:type "filtering"}}
                                        {:binding 1 :visibility js/GPUShaderStage.FRAGMENT
                                         :texture {:sampleType "float"}}
                                        {:binding 2 :visibility js/GPUShaderStage.VERTEX
                                         :buffer {:type "uniform"}}
                                        {:binding 3 :visibility js/GPUShaderStage.VERTEX
                                         :buffer {:type "read-only-storage"}}]}))
        rejection-layout
        (.createBindGroupLayout
         ^js device (clj->js {:entries [{:binding 2 :visibility js/GPUShaderStage.VERTEX
                                         :buffer {:type "uniform"}}
                                        {:binding 3 :visibility js/GPUShaderStage.VERTEX
                                         :buffer {:type "read-only-storage"}}]}))
        pipeline-layout (fn [layouts]
                          (.createPipelineLayout ^js device
                                                 (clj->js {:bindGroupLayouts layouts})))
        mesh-pipeline
        (fn [transparent?]
          (.createRenderPipeline
           ^js device
           (clj->js {:layout (pipeline-layout [interior-layout])
                     :vertex {:module mesh-vertex :entryPoint "main"
                              :buffers mesh-vertex-layouts}
                     :fragment {:module mesh-fragment :entryPoint "main"
                                :targets [(cond-> {:format "rgba16float"}
                                            transparent? (assoc :blend (blend-state)))]}
                     :primitive {:topology "triangle-list" :frontFace "ccw"
                                 :cullMode "back"}
                     :depthStencil {:format "depth24plus" :depthWriteEnabled (not transparent?)
                                    :depthCompare "less"}
                     :multisample {:count 4}})))
        shadow-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (pipeline-layout [shadow-layout])
                   :vertex {:module shadow-module :entryPoint "main"
                            :buffers mesh-vertex-layouts}
                   :primitive {:topology "triangle-list" :frontFace "ccw"
                               :cullMode "back"}
                   :depthStencil {:format "depth32float" :depthWriteEnabled true
                                  :depthCompare "less" :depthBias 2
                                  :depthBiasSlopeScale 2.0}}))
        composite-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (pipeline-layout [composite-layout])
                   :vertex {:module composite-vertex :entryPoint "main"
                            :buffers [{:arrayStride composite-instance-stride
                                       :stepMode "instance"
                                       :attributes [{:shaderLocation 0 :offset 0
                                                     :format "float32x4"}
                                                    {:shaderLocation 1 :offset 16
                                                     :format "uint32"}]}]}
                   :fragment {:module composite-fragment :entryPoint "main"
                              :targets [{:format "rgba16float" :blend (blend-state)}]}
                   :primitive {:topology "triangle-list"}}))
        worn-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (pipeline-layout [composite-layout])
                   :vertex {:module composite-vertex :entryPoint "main"
                            :buffers [{:arrayStride composite-instance-stride
                                       :stepMode "instance"
                                       :attributes [{:shaderLocation 0 :offset 0
                                                     :format "float32x4"}
                                                    {:shaderLocation 1 :offset 16
                                                     :format "uint32"}]}]}
                   :fragment {:module worn-fragment :entryPoint "main"
                              :targets [{:format "rgba16float" :blend (blend-state)}]}
                   :primitive {:topology "triangle-list"}}))
        rejection-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (pipeline-layout [rejection-layout])
                   :vertex {:module composite-vertex :entryPoint "main"
                            :buffers [{:arrayStride composite-instance-stride
                                       :stepMode "instance"
                                       :attributes [{:shaderLocation 0 :offset 0
                                                     :format "float32x4"}
                                                    {:shaderLocation 1 :offset 16
                                                     :format "uint32"}]}]}
                   :fragment {:module rejection-fragment :entryPoint "main"
                              :targets [{:format "rgba16float" :blend (blend-state)}]}
                   :primitive {:topology "triangle-list"}}))]
    {:interior-layout interior-layout :shadow-layout shadow-layout
     :composite-layout composite-layout :rejection-layout rejection-layout
     :opaque (mesh-pipeline false) :transparent (mesh-pipeline true)
     :shadow shadow-pipeline :composite composite-pipeline
     :worn worn-pipeline
     :rejection rejection-pipeline}))

(defn- create-buffer! [device label size usage]
  (let [size (max 4 (int size))
        buffer (.createBuffer ^js device (clj->js {:size size :usage usage}))]
    {:buffer buffer :capacity size :label label}))

(defn- ensure-buffer! [system current label required usage]
  (let [required (max 4 (int required))]
    (if (and current (>= (:capacity current) required))
      current
      (let [capacity (max required
                          (if current (int (js/Math.ceil (* 1.5 (:capacity current))))
                              256))
            next (if current
                   (let [buffer (.createBuffer
                                 ^js (:device system)
                                 (clj->js {:size capacity :usage usage}))]
                     (.destroy ^js (:buffer current))
                     {:buffer buffer :capacity capacity :label label})
                   (create-buffer! (:device system) label capacity usage))]
        next))))

(defn- write-buffer! [system buffer data active-bytes]
  (when (pos? active-bytes)
    (.writeBuffer (.-queue ^js (:device system)) (:buffer buffer) 0 data))
  buffer)

(defn- depth-fallback! [device]
  (let [texture (.createTexture ^js device
                                (clj->js {:label "region3d/shadow-fallback"
                                          :size {:width 1 :height 1
                                                 :depthOrArrayLayers 1}
                                          :format "depth32float"
                                          :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                                                         js/GPUTextureUsage.RENDER_ATTACHMENT)}))]
    {:texture texture :view (.createView texture)}))

(defn init-region3d-system!
  [device camera-buffer groups-buffer]
  (let [fallback (depth-fallback! device)
        composite-buffer (create-buffer!
                          device "region3d/composite-instances" 256
                          (bit-or js/GPUBufferUsage.VERTEX
                                  js/GPUBufferUsage.COPY_DST))]
    {:region3d-gpu/version region3d-gpu-version
     :device device :camera-buffer camera-buffer
     :groups-buffer groups-buffer :pipelines (create-pipelines! device)
     :placement-system (on-plane-renderer/init-placement-system! device)
     :sampler (.createSampler ^js device (clj->js {:minFilter "linear"
                                                   :magFilter "linear"}))
     :shadow-sampler (.createSampler ^js device
                                     (clj->js {:compare "less-equal"
                                               :minFilter "linear"
                                               :magFilter "linear"}))
     :shadow-fallback fallback
     :binding-owner (region-bindings/create-owner device)
     :!compositor (atom nil)
     :!prepared (atom {}) :!composite-buffer (atom composite-buffer)
     :!composite-rows (atom {})}))

(defonce ^:private !systems-by-device (js/WeakMap.))

(defn binding-owner [system] (:binding-owner system))

(defn region-topology-rows [system]
  (region-bindings/topology-rows (:binding-owner system)))

(defn region3d-system-for-device
  "Return the already-created system without allocating one. This lets an
   empty frame retire buffers belonging to regions that just closed."
  [device]
  (.get !systems-by-device device))

(defn ensure-region3d-system!
  [device camera-buffer groups-buffer]
  (or (.get !systems-by-device device)
      (let [system (init-region3d-system! device camera-buffer groups-buffer)]
        (.set !systems-by-device device system)
        system)))

(defn attach-compositor! [system frame-compositor]
  (when-not (identical? @(:!compositor system) frame-compositor)
    (reset! (:!compositor system) frame-compositor)
    (region-bindings/attach-compositor! (:binding-owner system)
                                        frame-compositor)
    (swap! (:!prepared system)
           (fn [prepared]
             (into {}
                   (map (fn [[region-id row]]
                          [region-id
                           (assoc row
                                  :last-lease-keys nil
                                  :dirty-by-role
                                  {:shadow (boolean (:shadow? row))
                                   :interior true})]))
                   prepared))))
  system)

(defn- tagged-linear [{:keys [rgba]}]
  (let [[r g b a] rgba]
    [(color/srgb-channel->linear r)
     (color/srgb-channel->linear g)
     (color/srgb-channel->linear b)
     a]))

(defn- column-major [matrix]
  (mapv #(nth matrix %) [0 4 8 12 1 5 9 13 2 6 10 14 3 7 11 15]))

(defn- typed-f32 [values]
  (js/Float32Array. (clj->js (vec values))))

(defn- write-buffer-range! [system buffer byte-offset values]
  (let [data (typed-f32 values)]
    (when (pos? (.-byteLength data))
      (.writeBuffer (.-queue ^js (:device system))
                    (:buffer buffer) byte-offset data)))
  buffer)

(defn- mesh-vertex-values [object]
  (let [{:keys [positions normals indices]} (scene/object-mesh object)]
    (vec
     (mapcat (fn [index]
               (let [base (* 3 index)]
                 [(nth positions base) (nth positions (inc base))
                  (nth positions (+ base 2))
                  (nth normals base) (nth normals (inc base))
                  (nth normals (+ base 2))]))
             indices))))

(defn- mesh-upload [maintained]
  (reduce
   (fn [{:keys [vertices draws]} [object-id object]]
     (if-not (= :mesh (:object/kind object))
       {:vertices vertices :draws draws}
       (let [values (mesh-vertex-values object)
             first-vertex (quot (count vertices) 6)
             vertex-count (quot (count values) 6)]
         {:vertices (into vertices values)
          :draws (assoc draws object-id
                        {:first-vertex first-vertex
                         :vertex-count vertex-count})})))
   {:vertices [] :draws {}}
   (sort-by (comp pr-str key) (get-in maintained [:region :scene]))))

(defn- instance-row-values [{:keys [matrix component]}]
  (let [component (or component component/default-component)
        base (tagged-linear (:base-color component))
        emissive (tagged-linear (:emissive component))]
    (vec (concat (column-major matrix)
                 base
                 [(:metallic component) (:roughness component) 0.0 0.0]
                 [(nth emissive 0) (nth emissive 1) (nth emissive 2) 0.0]))))

(defn- instance-values [maintained]
  (vec (mapcat instance-row-values (:instances maintained))))

(defn- object-index [maintained]
  (into {} (map-indexed (fn [index row] [(:object-id row) index])
                        (:instances maintained))))

(defn- light-rows [maintained]
  (->> (get-in maintained [:region :scene])
       (filter (fn [[_ object]] (= :light (:object/kind object))))
       (sort-by (comp pr-str key))
       (take max-lights)
       vec))

(defn- light-index [maintained]
  (into {} (map-indexed (fn [index [object-id _]] [object-id index])
                        (light-rows maintained))))

(defn- light-row-values [maintained object-id object]
  (let [light (:light object)
        matrix (get-in maintained [:world-transforms object-id])
        position (scene/transform-point matrix [0.0 0.0 0.0])
        direction (scene/normalize
                   (scene/transform-direction matrix [0.0 0.0 -1.0]))
        [r g b _] (tagged-linear (:color light))
        kind (case (:kind light) :directional 0.0 :point 1.0 :spot 2.0)
        cone (:cone light)
        inner (js/Math.cos (* (or (:inner-deg cone) 0.0)
                              (/ js/Math.PI 180.0)))
        outer (js/Math.cos (* (or (:outer-deg cone) 0.0)
                              (/ js/Math.PI 180.0)))]
    (vec (concat [kind (:intensity light) (or (:range light) 100.0)
                  (if (:cast-shadow light) 1.0 0.0)]
                 position [1.0] direction [0.0] [r g b 1.0]
                 [inner outer 0.0 0.0]))))

(defn- light-values [maintained]
  (let [lights (light-rows maintained)]
    {:count (count lights)
     :values
     (vec (mapcat (fn [[object-id object]]
                    (light-row-values maintained object-id object))
                  lights))}))

(defn- shadow-projection [{:keys [min max]}]
  (let [[left bottom far] min
        [right top near] max
        dx (- right left) dy (- top bottom) dz (- far near)]
    [(/ 2.0 dx) 0.0 0.0 (/ (- (+ right left)) dx)
     0.0 (/ 2.0 dy) 0.0 (/ (- (+ top bottom)) dy)
     0.0 0.0 (/ 1.0 dz) (/ (- near) dz)
     0.0 0.0 0.0 1.0]))

(defn- shadow-matrix [shadow-space]
  (if shadow-space
    (scene/mat4-mul (shadow-projection (:bounds shadow-space))
                    (:view shadow-space))
    scene/identity-mat4))

(defn- display-mode-number [mode]
  (case mode :flat 1.0 :normal 2.0 0.0))

(defn- region-uniform-values [maintained camera display-mode shadow-space]
  (let [[ar ag ab _] (tagged-linear (get-in maintained [:region :ambient :color]))
        intensity (get-in maintained [:region :ambient :intensity])
        light-count (:count (light-values maintained))]
    (concat (column-major (:view-projection camera))
            (:eye camera) [1.0]
            [ar ag ab intensity]
            [light-count (display-mode-number display-mode)
             (if shadow-space 1.0 0.0)])))

(defn- session-region [session region-id]
  (get-in session [:regions region-id] {}))

(defn- create-region-gpu [system region-id]
  (let [usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.VERTEX)
        uniform-usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.UNIFORM)
        storage-usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.STORAGE)]
    {:vertex (create-buffer! (:device system)
                             (str "region3d/" region-id "/vertices") 256 usage)
     :instances (create-buffer! (:device system)
                                (str "region3d/" region-id "/instances") 256 usage)
     :lights (create-buffer! (:device system)
                             (str "region3d/" region-id "/lights") 640 storage-usage)
     :uniform (create-buffer! (:device system)
                              (str "region3d/" region-id "/uniform")
                              region-uniform-bytes uniform-usage)
     :shadow-uniform (create-buffer! (:device system)
                                     (str "region3d/" region-id "/shadow-uniform")
                                     shadow-uniform-bytes uniform-usage)
     :placement (on-plane-renderer/create-region-gpu!
                 (:placement-system system) region-id)}))

(defn- write-component-gpu! [system region-id gpu maintained]
  (let [{:keys [vertices draws]} (mesh-upload maintained)
        vertex-data (typed-f32 vertices)
        instance-data (typed-f32 (instance-values maintained))
        {:keys [count values]} (light-values maintained)
        light-data (typed-f32 values)
        vertex-buffer (ensure-buffer! system (:vertex gpu)
                                      (str "region3d/" region-id "/vertices")
                                      (.-byteLength vertex-data)
                                      (bit-or js/GPUBufferUsage.COPY_DST
                                              js/GPUBufferUsage.VERTEX))
        instance-buffer (ensure-buffer! system (:instances gpu)
                                        (str "region3d/" region-id "/instances")
                                        (.-byteLength instance-data)
                                        (bit-or js/GPUBufferUsage.COPY_DST
                                                js/GPUBufferUsage.VERTEX))]
    (write-buffer! system vertex-buffer vertex-data (.-byteLength vertex-data))
    (write-buffer! system instance-buffer instance-data (.-byteLength instance-data))
    (write-buffer! system (:lights gpu) light-data (.-byteLength light-data))
    (assoc gpu :vertex vertex-buffer :instances instance-buffer
           :draws draws :object-index (object-index maintained)
           :light-index (light-index maintained) :light-count count)))

(defn- write-transform-gpu!
  "Write only evaluated transform dependents. Mesh vertices and draw topology
  stay retained; instance and light rows use their stable offsets."
  [system gpu maintained affected-object-ids]
  (reduce
   (fn [result object-id]
     (let [object (get-in maintained [:region :scene object-id])
           instance-index (get-in gpu [:object-index object-id])
           light-index (get-in gpu [:light-index object-id])]
       (when (some? instance-index)
         (write-buffer-range!
          system (:instances gpu) (* instance-index mesh-instance-stride)
          (instance-row-values
           (get-in maintained [:instances-by-object object-id]))))
       (when (some? light-index)
         (write-buffer-range!
          system (:lights gpu) (* light-index light-instance-stride)
          (light-row-values maintained object-id object)))
       (cond-> result
         (some? instance-index) (update :instance-uploads inc)
         (some? light-index) (update :light-uploads inc))))
   {:instance-uploads 0 :light-uploads 0}
   (sort-by pr-str affected-object-ids)))

(defn- object-depth [camera maintained object-id]
  (scene/length
   (scene/v- (scene/transform-point
              (get-in maintained [:world-transforms object-id])
              [0.0 0.0 0.0])
             (:eye camera))))

(defn- draw-order [gpu maintained camera]
  (let [transparent-background?
        (= :transparent (get-in maintained [:region :background :kind]))
        rows (for [[object-id draw] (:draws gpu)
                   :let [instance (get-in maintained [:instances-by-object object-id])]]
               (merge draw {:object-id object-id
                            :instance-index (get-in gpu [:object-index object-id])
                            :transparent? (or transparent-background?
                                              (:transparent? instance))
                            :depth (object-depth camera maintained object-id)}))]
    {:opaque (vec (sort-by (juxt :depth (comp pr-str :object-id))
                           (remove :transparent? rows)))
     :transparent (vec (sort-by (juxt (comp - :depth) (comp pr-str :object-id))
                                (filter :transparent? rows)))}))

(defn- write-view-gpu! [system gpu maintained camera session-row shadow-space]
  (let [display-mode (or (:display-mode session-row) :lit)
        uniform (typed-f32 (region-uniform-values maintained camera display-mode
                                                   shadow-space))
        shadow (typed-f32 (column-major (shadow-matrix shadow-space)))]
    (write-buffer! system (:uniform gpu) uniform (.-byteLength uniform))
    (write-buffer! system (:shadow-uniform gpu) shadow (.-byteLength shadow))
    gpu))

(defn- destroy-buffer! [row]
  (when-let [buffer (:buffer row)]
    (.destroy ^js buffer)))

(defn- destroy-region-gpu! [gpu]
  (doseq [key [:vertex :instances :lights :uniform :shadow-uniform]]
    (destroy-buffer! (get gpu key)))
  (on-plane-renderer/destroy-region-gpu! (:placement gpu)))

(defn- composite-row-bytes [{:keys [x y w h buffer-index]}]
  (let [raw (js/ArrayBuffer. composite-instance-stride)
        floats (js/Float32Array. raw)
        uints (js/Uint32Array. raw)]
    (aset floats 0 x) (aset floats 1 y)
    (aset floats 2 w) (aset floats 3 h)
    (aset uints 4 buffer-index)
    (js/Uint8Array. raw)))

(defn- upload-composites! [system desired]
  (let [rows (into {}
                   (map (fn [{:keys [buffer-index composite]}]
                          [buffer-index composite]))
                   desired)
        max-buffer-index (reduce max -1 (keys rows))
        active-bytes (* (inc max-buffer-index) composite-instance-stride)
        old-buffer @(:!composite-buffer system)
        buffer (ensure-buffer! system old-buffer
                               "region3d/composite-instances"
                               (max 4 active-bytes)
                               (bit-or js/GPUBufferUsage.COPY_DST
                                       js/GPUBufferUsage.VERTEX))
        grew? (not (identical? (:buffer old-buffer) (:buffer buffer)))
        prior @(:!composite-rows system)
        changed (if grew?
                  rows
                  (into {}
                        (filter (fn [[buffer-index row]]
                                  (not= row (get prior buffer-index))))
                        rows))]
    (doseq [[buffer-index row] changed]
      (.writeBuffer (.-queue ^js (:device system)) (:buffer buffer)
                    (* buffer-index composite-instance-stride)
                    (composite-row-bytes row)))
    (reset! (:!composite-buffer system) buffer)
    (reset! (:!composite-rows system) rows)
    (count changed)))

(def ^:private encode-scale-step 1.12)

(defn- encode-scale-bucket
  "Versioned geometric camera door. The bucket, not raw scale, is semantic."
  [scale]
  (let [scale (max 1.0e-9 (double (or scale 1.0)))]
    (long (js/Math.floor (/ (js/Math.log scale)
                            (js/Math.log encode-scale-step))))))

(defn- quantize-encode-scale [scale]
  (js/Math.pow encode-scale-step (encode-scale-bucket scale)))

(defn- empty-region-return [changed?]
  {:changed? changed? :full-rebuilds 0 :instance-uploads 0
   :bvh-refits 0 :region-encodes 0})

(defn prepare-region3d-frame!
  "Upload changed region rows before any pass opens. The region revision and
   projection stamps are the dirty check; the returned counts belong to this call."
  [system {:keys [regions]} session
   {:keys [zoom dpr world-transforms font-assets session-layout-snapshot path-system
           max-lease-size]
    :or {zoom 1.0 dpr 1.0}}]
  (let [regions (vec (or regions []))
        prior @(:!prepared system)
        session-revision (:revision session-layout-snapshot)
        live-ids (set (map #(get-in % [:region/material :region/id]) regions))
        group-buffer-indexes (mapv #(transform/buffer-index world-transforms (:container %))
                              regions)
        results
        (mapv
         (fn [[draw-item group-buffer-index]]
           (let [raw-region (:region/material draw-item)
                 region-id (:region/id raw-region)
                 key (frame/region-key draw-item zoom dpr session-revision)
                 old (get prior region-id)]
             (if (= key (:key old))
               [region-id old (empty-region-return false)]
               (let [{:keys [w h]} (:region/rect raw-region)
                     session-row (session-region session region-id)
                     pixel-size [(max 1 (js/Math.ceil (* w zoom dpr)))
                                 (max 1 (js/Math.ceil (* h zoom dpr)))]
                     lease-size
                     (mapv compositor/quantize-region-size
                           (if max-lease-size
                             (mapv #(min % max-lease-size) pixel-size)
                             pixel-size))
                     encode-scale (quantize-encode-scale (* zoom dpr))
                     encode-bucket (encode-scale-bucket (* zoom dpr))
                     encode-pixel-size
                     [(max 1 (js/Math.ceil (* w encode-scale)))
                      (max 1 (js/Math.ceil (* h encode-scale)))]
                     background-key (:background raw-region)
                     background-changed?
                     (or (nil? old)
                         (not= background-key (:background-key old)))
                     evaluation-result
                     (scene/evaluate-scene
                      (:maintained old) (:evaluation-key old)
                      raw-region session-row)
                     update-kind (:update-kind evaluation-result)
                     component-changed? (= :full update-kind)
                     transform-changed? (= :transform update-kind)
                     scene-changed? (not= :none update-kind)
                     maintained0 (assoc (:maintained evaluation-result)
                                        :region-id region-id)
                     maintained
                     (if (and background-changed? (not component-changed?))
                       (assoc-in maintained0 [:region :background]
                                 background-key)
                       maintained0)
                     shadow-space (if scene-changed?
                                    (scene/shadow-light-space maintained)
                                    (:shadow-space old))
                     view (or (:view session-row)
                              (:view raw-region))
                     view-key [view (:display-mode session-row) encode-bucket
                               shadow-space]
                     view-changed? (or scene-changed? (nil? old)
                                       (not= view-key (:view-key old)))
                     camera (if view-changed?
                              (scene/camera-matrices view encode-pixel-size)
                              (:camera old))
                     gpu0 (or (:gpu old)
                              (create-region-gpu system region-id))
                     upload-return
                     (case update-kind
                       :full
                       {:gpu (write-component-gpu! system region-id gpu0 maintained)
                        :instance-uploads (count (:instances maintained))}

                       :transform
                       (assoc (write-transform-gpu!
                               system gpu0 maintained
                               (:affected-object-ids evaluation-result))
                              :gpu gpu0)

                       {:gpu gpu0 :instance-uploads 0})
                     gpu1 (:gpu upload-return)
                     gpu2 (if view-changed?
                            (write-view-gpu! system gpu1 maintained camera
                                             session-row shadow-space)
                            gpu1)
                     placement-return
                     (on-plane-renderer/prepare-placements!
                      (:placement-system system) (:placement gpu2)
                      (:region3d/resolved-placements draw-item) maintained camera
                      (if path-system @(:!mesh-cache path-system) {}))
                     _ (when path-system
                         (reset! (:!mesh-cache path-system)
                                 (:path-cache placement-return)))
                     gpu3 (assoc gpu2 :placement (:gpu placement-return))
                     mesh-draw-order
                     (if (or scene-changed? view-changed?
                             (not= (get-in old [:maintained :region
                                               :background :kind])
                                   (get-in maintained [:region :background
                                                       :kind]))
                             (nil? old))
                       (draw-order gpu3 maintained camera)
                       (:draw-order old))
                     dirty-by-role
                     {:shadow (or scene-changed? (nil? old))
                      :interior (or scene-changed? view-changed?
                                    background-changed?
                                    (:changed? placement-return)
                                    (nil? old))}
                     row
                     {:key key :region-id region-id :draw-item draw-item
                      :group-buffer-index group-buffer-index
                      :evaluation-key (:evaluation-key evaluation-result)
                      :background-key background-key :view-key view-key
                      :maintained maintained :camera camera
                      :lease-size lease-size :encode-rung encode-bucket
                      :shadow-space shadow-space
                      :shadow? (boolean shadow-space)
                      :gpu gpu3 :draw-order mesh-draw-order
                      :placements (:placements placement-return)
                      :dirty-by-role dirty-by-role
                      :scene-update-kind update-kind
                      :affected-object-ids
                      (:affected-object-ids evaluation-result)
                      :last-lease-keys (:last-lease-keys old)
                      :session session-row}
                     call-return
                     {:changed? true
                      :full-rebuilds (if component-changed? 1 0)
                      :instance-uploads (:instance-uploads upload-return 0)
                      :bvh-refits (if transform-changed?
                                    (count
                                     (filter (:triangles-by-object maintained)
                                             (:affected-object-ids
                                              evaluation-result)))
                                    0)
                      :region-encodes 0}]
                 [region-id row call-return]))))
         (map vector regions group-buffer-indexes))
        computed (into {} (map (fn [[region-id row _]] [region-id row])) results)
        region-returns
        (into {} (map (fn [[region-id _ call-return]]
                        [region-id call-return])) results)
        closed (vec (remove live-ids (keys prior)))
        desired
        (region-bindings/reconcile-desired!
         (:binding-owner system)
         (mapv (fn [[region-id row]]
                 (let [draw-item (:draw-item row)
                       {:keys [x y w h]} (get-in draw-item [:region/material
                                                    :region/rect])]
                   {:region/id region-id
                    :lease-size (:lease-size row)
                    :shadow? (:shadow? row)
                    :background (get-in row [:maintained :region :background])
                    :encode-rung (:encode-rung row)
                    :composite {:x x :y y :w w :h h
                                :buffer-index (:group-buffer-index row)}}))
               computed))
        computed
        (into {}
              (map (fn [[region-id row]]
                     (let [buffer-index (get-in desired [region-id :buffer-index])]
                       [region-id
                        (if (= buffer-index (:composite-buffer-index row))
                          row
                          (assoc row :composite-buffer-index buffer-index))])))
              computed)
        composite-uploads (upload-composites! system (vals desired))
        unchanged? (and (empty? closed)
                        (= (keys prior) (keys computed))
                        (every? (fn [[id row]]
                                  (identical? row (get prior id)))
                                computed))
        next (if unchanged? prior computed)]
    (doseq [region-id closed]
      (destroy-region-gpu! (:gpu (get prior region-id))))
    (when-not unchanged?
      (reset! (:!prepared system) next))
    {:regions region-returns
     :composite-uploads composite-uploads
     :held-passes 0}))

(defn- region-bind-group [system prepared lease]
  (let [gpu (:gpu prepared)
        pipelines (:pipelines system)
        shadow-view (or (get-in lease [:shadow :view])
                        (get-in system [:shadow-fallback :view]))]
    (.createBindGroup
     ^js (:device system)
     (clj->js {:layout (:interior-layout pipelines)
               :entries [{:binding 0 :resource {:buffer (:buffer (:uniform gpu))}}
                         {:binding 1 :resource {:buffer (:buffer (:lights gpu))}}
                         {:binding 2 :resource shadow-view}
                         {:binding 3 :resource (:shadow-sampler system)}
                         {:binding 4 :resource {:buffer (:buffer (:shadow-uniform gpu))}}]}))))

(defn- one-buffer-bind-group [system layout buffer]
  (.createBindGroup ^js (:device system)
                    (clj->js {:layout layout
                              :entries [{:binding 0
                                         :resource {:buffer (:buffer buffer)}}]})))

(defn- clear-color [prepared]
  (let [[r g b a] (tagged-linear
                   (get-in prepared [:maintained :region :background :color]))
        transparent? (= :transparent
                        (get-in prepared [:maintained :region :background :kind]))
        alpha (if transparent? 0.0 a)]
    {:r (* r alpha) :g (* g alpha) :b (* b alpha) :a alpha}))

(defn- draw-mesh-rows! [pass system prepared rows pipeline bind-group]
  (let [gpu (:gpu prepared)]
    (.setPipeline ^js pass pipeline)
    (.setBindGroup ^js pass 0 bind-group)
    (.setVertexBuffer ^js pass 0 (:buffer (:vertex gpu)))
    (.setVertexBuffer ^js pass 1 (:buffer (:instances gpu)))
    (doseq [{:keys [first-vertex vertex-count instance-index]} rows]
      (.draw ^js pass vertex-count 1 first-vertex instance-index))))

(defn- encode-shadow! [system encoder prepared lease]
  (let [gpu (:gpu prepared)
        pass (.beginRenderPass
              ^js encoder
              (clj->js {:colorAttachments []
                        :depthStencilAttachment
                        {:view (get-in lease [:shadow :view])
                         :depthClearValue 1.0 :depthLoadOp "clear"
                         :depthStoreOp "store"}}))
        bind (one-buffer-bind-group system
                                    (get-in system [:pipelines :shadow-layout])
                                    (:shadow-uniform gpu))
        rows (concat (get-in prepared [:draw-order :opaque])
                     (get-in prepared [:draw-order :transparent]))]
    (.setPipeline pass (get-in system [:pipelines :shadow]))
    (.setBindGroup pass 0 bind)
    (.setVertexBuffer pass 0 (:buffer (:vertex gpu)))
    (.setVertexBuffer pass 1 (:buffer (:instances gpu)))
    (doseq [{:keys [first-vertex vertex-count instance-index]} rows]
      (.draw pass vertex-count 1 first-vertex instance-index))
    (.end pass)))

(defn- encode-interior! [system encoder prepared lease]
  (let [gpu (:gpu prepared)
        pass (.beginRenderPass
              ^js encoder
              (clj->js {:colorAttachments
                        [{:view (get-in lease [:color-msaa :view])
                          :resolveTarget (get-in lease [:resolve :view])
                          :clearValue (clear-color prepared)
                          :loadOp "clear" :storeOp "store"}]
                        :depthStencilAttachment
                        {:view (get-in lease [:depth :view])
                         :depthClearValue 1.0 :depthLoadOp "clear"
                         :depthStoreOp "store"}}))
        mesh-bind (region-bind-group system prepared lease)]
    (draw-mesh-rows! pass system prepared (get-in prepared [:draw-order :opaque])
                     (get-in system [:pipelines :opaque]) mesh-bind)
    (draw-mesh-rows! pass system prepared
                     (get-in prepared [:draw-order :transparent])
                     (get-in system [:pipelines :transparent]) mesh-bind)
    (on-plane-renderer/draw-placements! pass (:placement-system system)
                                    (:placement gpu) (:uniform gpu))
    (.end pass)))

(defn encode-region-pass!
  "Encode one role (:shadow or :interior) of one region into its lease. Clean
   regions hold their encode; a new lease key forces one encode before it can
   be sampled."
  [system encoder region-id role lease]
  (let [prepared (get @(:!prepared system) region-id)
        encode? (and prepared lease (not (:rejected? lease))
                     (or (get-in prepared [:dirty-by-role role])
                         (not= (:key lease)
                               (get-in prepared [:last-lease-keys role]))))]
    (cond
      (or (nil? prepared) (nil? lease))
      {:region-id region-id :role role :encoded? false
       :held? false :reason :missing-region-state}

      (:rejected? lease)
      {:region-id region-id :role role :encoded? false
       :held? false :rejection (:rejection lease)}

      (not encode?)
      {:region-id region-id :role role
       :encoded? false :held? true :lease-key (:key lease)}

      :else
      (do
        (case role
          :shadow (when (and (:shadow-space prepared) (:shadow lease))
                    (encode-shadow! system encoder prepared lease))
          :interior (encode-interior! system encoder prepared lease)
          nil)
        (swap! (:!prepared system) update region-id
               (fn [row]
                 (-> row
                     (assoc-in [:dirty-by-role role] false)
                     (assoc-in [:last-lease-keys role] (:key lease)))))
        {:region-id region-id :role role :encoded? true
         :held? false :lease-key (:key lease)}))))

(defn- composite-bind-group [system lease]
  (.createBindGroup
   ^js (:device system)
   (clj->js {:layout (get-in system [:pipelines :composite-layout])
             :entries [{:binding 0 :resource (:sampler system)}
                       {:binding 1 :resource (get-in lease [:resolve :view])}
                       {:binding 2 :resource {:buffer (:camera-buffer system)}}
                       {:binding 3 :resource {:buffer (:groups-buffer system)}}]})))

(defn- rejection-bind-group [system]
  (.createBindGroup
   ^js (:device system)
   (clj->js {:layout (get-in system [:pipelines :rejection-layout])
             :entries [{:binding 2 :resource {:buffer (:camera-buffer system)}}
                       {:binding 3 :resource {:buffer (:groups-buffer system)}}]})))

(defn composite-region!
  "Composite one region's held lease onto an open pass at its stable buffer-index, or
   its rejection placeholder when no lease is held."
  [pass region-system region-id]
  (let [owner (:binding-owner region-system)
        lease (region-bindings/lease owner region-id)
        composite-buffer-index (region-bindings/buffer-index owner region-id)
        rejected? (or (nil? lease) (:rejected? lease))
        pipeline-key (cond
                       rejected? :rejection
                       (> (:rung-divisor lease 1) 1) :worn
                       :else :composite)]
    (.setPipeline ^js pass (get-in region-system
                                   [:pipelines pipeline-key]))
    (.setBindGroup ^js pass 0 (if rejected?
                                (rejection-bind-group region-system)
                                (composite-bind-group region-system lease)))
    (.setVertexBuffer ^js pass 0 (:buffer @(:!composite-buffer region-system)))
    (.draw ^js pass 6 1 0 composite-buffer-index)))

(defn destroy-region3d-system! [system]
  (doseq [[_ row] @(:!prepared system)]
    (destroy-region-gpu! (:gpu row)))
  (destroy-buffer! @(:!composite-buffer system))
  (when-let [texture (get-in system [:shadow-fallback :texture])]
    (.destroy ^js texture))
  (on-plane-renderer/destroy-placement-system! (:placement-system system))
  (reset! (:!prepared system) {})
    (.delete !systems-by-device (:device system))
  true)

(defonce ^:private !compositors-by-device (js/WeakMap.))

(defn- ensure-frame-compositor! [device format]
  (or (.get !compositors-by-device device)
      (let [compositor (compositor/create-compositor!
                        device format)]
        (.set !compositors-by-device device compositor)
        compositor)))

(defn replace-frame-compositor!
  "The sole same-device compositor epoch producer.  Semantic frame state is
   deliberately retained; Region3D reattaches to the new binding epoch."
  [device format]
  (when-let [old (.get !compositors-by-device device)]
    (compositor/destroy-compositor! old))
  (let [compositor (compositor/create-compositor! device format)]
    (.set !compositors-by-device device compositor)
    (when-let [region-system (region3d-system-for-device device)]
      (attach-compositor! region-system compositor))
    compositor))
