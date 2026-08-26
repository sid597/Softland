(ns app.client.substrate.webgpu.region3d-gpu
  "Atom A's WebGPU region family.

   `prepare-region3d-frame!` is the only CPU->GPU upload door and never mints
   an encoder. `encode-region-passes!` is the compositor-owned plan producer;
   `execute-region3d-batch!` is the ordinary scene-tape composite executor.
   Region attachment bytes remain compositor target-pool leases throughout."
  (:require [app.client.substrate.frame-inputs :as frame-inputs]
            [app.client.substrate.region3d-evaluation :as evaluation]
            [app.client.substrate.region3d-material :as material]
            [app.client.substrate.region3d-placement :as placement]
            [app.client.substrate.region3d-scene :as scene]
            [app.client.substrate.webgpu.compositor-gpu :as compositor]
            [app.client.substrate.webgpu.gpu-budget :as gpu-budget]
            [app.client.substrate.webgpu.region-bindings :as region-bindings]
            [app.client.substrate.webgpu.region3d-placement-gpu
             :as placement-gpu]))

(def region3d-gpu-version 1)
(def max-lights 8)
(def mesh-vertex-stride 24)
(def mesh-instance-stride 112)
(def light-instance-stride 80)
(def composite-instance-stride 20)
(def region-uniform-bytes 128)
(def shadow-uniform-bytes 64)

(def mesh-vertex-shader
  "struct Region {
     view_proj: mat4x4<f32>, eye: vec4<f32>, ambient: vec4<f32>,
     settings: vec4<f32>, camera_info: vec4<f32>,
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
     settings: vec4<f32>, camera_info: vec4<f32>,
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
   struct ContainerTransform { axis_x: vec2<f32>, axis_y: vec2<f32>,
     translation: vec2<f32>, flags: u32, padding: u32, };
   @group(0) @binding(2) var<uniform> camera: Camera;
   @group(0) @binding(3) var<storage, read> containers: array<ContainerTransform>;
   struct In { @location(0) rect: vec4<f32>, @location(1) container_idx: u32, };
   struct Out { @builtin(position) position: vec4<f32>, @location(0) uv: vec2<f32>, };
   @vertex fn main(@builtin(vertex_index) index: u32, input: In) -> Out {
     var corners = array<vec2<f32>, 6>(vec2<f32>(0.0,0.0), vec2<f32>(1.0,0.0),
       vec2<f32>(0.0,1.0), vec2<f32>(1.0,0.0), vec2<f32>(1.0,1.0), vec2<f32>(0.0,1.0));
     let uv = corners[index]; let local = input.rect.xy + uv * input.rect.zw;
     let c = containers[input.container_idx]; let screen = (c.flags & 1u) != 0u;
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

(def refusal-fragment-shader
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
        refusal-fragment (shader-module device refusal-fragment-shader)
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
        refusal-layout
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
        refusal-pipeline
        (.createRenderPipeline
         ^js device
         (clj->js {:layout (pipeline-layout [refusal-layout])
                   :vertex {:module composite-vertex :entryPoint "main"
                            :buffers [{:arrayStride composite-instance-stride
                                       :stepMode "instance"
                                       :attributes [{:shaderLocation 0 :offset 0
                                                     :format "float32x4"}
                                                    {:shaderLocation 1 :offset 16
                                                     :format "uint32"}]}]}
                   :fragment {:module refusal-fragment :entryPoint "main"
                              :targets [{:format "rgba16float" :blend (blend-state)}]}
                   :primitive {:topology "triangle-list"}}))]
    {:interior-layout interior-layout :shadow-layout shadow-layout
     :composite-layout composite-layout :refusal-layout refusal-layout
     :opaque (mesh-pipeline false) :transparent (mesh-pipeline true)
     :shadow shadow-pipeline :composite composite-pipeline
     :worn worn-pipeline
     :refusal refusal-pipeline}))

(defn- create-buffer! [device tracker label size usage]
  (let [size (max 4 (int size))
        buffer (.createBuffer ^js device (clj->js {:size size :usage usage}))]
    (gpu-budget/register-buffer! tracker buffer label size :active-bytes 0)
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
                     (gpu-budget/replace-buffer! (:tracker system)
                                                 (:buffer current) buffer
                                                 label capacity
                                                 :active-bytes required
                                                 :reason :region3d-grow)
                     (.destroy ^js (:buffer current))
                     {:buffer buffer :capacity capacity :label label})
                   (create-buffer! (:device system) (:tracker system)
                                   label capacity usage))]
        next))))

(defn- write-buffer! [system buffer data active-bytes]
  (when (pos? active-bytes)
    (.writeBuffer (.-queue ^js (:device system)) (:buffer buffer) 0 data))
  (gpu-budget/set-active-bytes! (:tracker system) (:buffer buffer) active-bytes)
  buffer)

(defn- depth-fallback! [device tracker]
  (let [texture (.createTexture ^js device
                                (clj->js {:label "region3d/shadow-fallback"
                                          :size {:width 1 :height 1
                                                 :depthOrArrayLayers 1}
                                          :format "depth32float"
                                          :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                                                         js/GPUTextureUsage.RENDER_ATTACHMENT)}))]
    (gpu-budget/register-texture! tracker texture "region3d/shadow-fallback"
                                  :format "depth32float" :width 1 :height 1)
    {:texture texture :view (.createView texture)}))

(defn init-region3d-system!
  [device tracker camera-buffer containers-buffer]
  (let [fallback (depth-fallback! device tracker)
        composite-buffer (create-buffer!
                          device tracker "region3d/composite-instances" 256
                          (bit-or js/GPUBufferUsage.VERTEX
                                  js/GPUBufferUsage.COPY_DST))]
    {:region3d-gpu/version region3d-gpu-version
     :device device :tracker tracker :camera-buffer camera-buffer
     :containers-buffer containers-buffer :pipelines (create-pipelines! device)
     :placement-system (placement-gpu/init-placement-system! device tracker)
     :sampler (.createSampler ^js device (clj->js {:minFilter "linear"
                                                   :magFilter "linear"}))
     :shadow-sampler (.createSampler ^js device
                                     (clj->js {:compare "less-equal"
                                               :minFilter "linear"
                                               :magFilter "linear"}))
     :shadow-fallback fallback
     :frame-input/identity (js-obj) :!shape-rev (atom 0)
     :binding-owner (region-bindings/create-owner device)
     :!compositor (atom nil)
     :!prepared (atom {}) :!composite-buffer (atom composite-buffer)
     :!composite-rows (atom {})
     :!last-composite-key (atom nil)
     :!last-regions (atom ::never)
     :!entry-shape-key (atom ::never)
     :!receipt (atom {:version 1 :prepare-calls 0 :scene-derives 0
                      :scene-transform-updates 0
                      :region-encodes 0
                      :held-passes 0 :object-instance-uploads 0
                      :mesh-vertex-uploads 0
                      :light-uploads 0 :uniform-uploads 0
                      :composite-uploads 0 :regions {}})}))

(defonce ^:private !systems-by-device (js/WeakMap.))

(defn binding-owner [system] (:binding-owner system))

(defn drain-binding-deltas! [system]
  (region-bindings/drain-deltas! (:binding-owner system)))

(defn region-topology-rows [system]
  (region-bindings/topology-rows (:binding-owner system)))

(defn region3d-system-for-device
  "Return the already-created system without allocating one. This lets an
   empty frame retire buffers belonging to regions that just closed."
  [device]
  (.get !systems-by-device device))

(defn ensure-region3d-system!
  [device tracker camera-buffer containers-buffer]
  (or (.get !systems-by-device device)
      (let [system (init-region3d-system! device tracker camera-buffer
                                          containers-buffer)]
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
  (let [[r g b a] rgba
        decode (fn [value]
                 (if (<= value 0.04045)
                   (/ value 12.92)
                   (js/Math.pow (/ (+ value 0.055) 1.055) 2.4)))]
    [(decode r) (decode g) (decode b) a]))

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

(defn- instance-row-values [{:keys [matrix material]}]
  (let [material (or material material/default-material)
        base (tagged-linear (:base-color material))
        emissive (tagged-linear (:emissive material))]
    (vec (concat (column-major matrix)
                 base
                 [(:metallic material) (:roughness material) 0.0 0.0]
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
        matrix (get-in maintained [:effective-transforms object-id])
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

(defn- camera-info [camera]
  (let [[width height] (:viewport camera)
        lens (:lens camera)]
    [(if (= :perspective (:kind lens))
       (* (:fov-y-deg lens) (/ js/Math.PI 180.0)) 0.0)
     width height (or (:ortho-scale lens) 0.0)]))

(defn- region-uniform-values [maintained camera display-mode shadow-space]
  (let [[ar ag ab _] (tagged-linear (get-in maintained [:region :ambient :color]))
        intensity (get-in maintained [:region :ambient :intensity])
        light-count (:count (light-values maintained))
        ortho? (= :ortho (get-in camera [:lens :kind]))]
    (concat (column-major (:view-projection camera))
            (:eye camera) [1.0]
            [ar ag ab intensity]
            [light-count (display-mode-number display-mode)
             (if shadow-space 1.0 0.0) (if ortho? 1.0 0.0)]
            (camera-info camera))))

(defn- session-region [session region-id]
  (get-in session [:regions region-id] {}))

(defn- create-region-gpu [system region-id]
  (let [usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.VERTEX)
        uniform-usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.UNIFORM)
        storage-usage (bit-or js/GPUBufferUsage.COPY_DST js/GPUBufferUsage.STORAGE)]
    {:vertex (create-buffer! (:device system) (:tracker system)
                             (str "region3d/" region-id "/vertices") 256 usage)
     :instances (create-buffer! (:device system) (:tracker system)
                                (str "region3d/" region-id "/instances") 256 usage)
     :lights (create-buffer! (:device system) (:tracker system)
                             (str "region3d/" region-id "/lights") 640 storage-usage)
     :uniform (create-buffer! (:device system) (:tracker system)
                              (str "region3d/" region-id "/uniform")
                              region-uniform-bytes uniform-usage)
     :shadow-uniform (create-buffer! (:device system) (:tracker system)
                                     (str "region3d/" region-id "/shadow-uniform")
                                     shadow-uniform-bytes uniform-usage)
     :placement (placement-gpu/create-region-gpu!
                 (:placement-system system) region-id)}))

(defn- write-material-gpu! [system region-id gpu maintained]
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
   (fn [receipt object-id]
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
       (cond-> receipt
         (some? instance-index) (update :instance-uploads inc)
         (some? light-index) (update :light-uploads inc))))
   {:instance-uploads 0 :light-uploads 0}
   (sort-by pr-str affected-object-ids)))

(defn- object-depth [camera maintained object-id]
  (scene/length
   (scene/v- (scene/transform-point
              (get-in maintained [:effective-transforms object-id])
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

(defn- destroy-buffer! [system row reason]
  (when-let [buffer (:buffer row)]
    (gpu-budget/destroy-resource! (:tracker system) buffer :reason reason)
    (.destroy ^js buffer)))

(defn- destroy-region-gpu! [system gpu]
  (doseq [key [:vertex :instances :lights :uniform :shadow-uniform]]
    (destroy-buffer! system (get gpu key) :region3d-region-close))
  (placement-gpu/destroy-region-gpu! (:placement-system system)
                                     (:placement gpu)))

(defn- composite-row-bytes [{:keys [x y w h container-idx]}]
  (let [raw (js/ArrayBuffer. composite-instance-stride)
        floats (js/Float32Array. raw)
        uints (js/Uint32Array. raw)]
    (aset floats 0 x) (aset floats 1 y)
    (aset floats 2 w) (aset floats 3 h)
    (aset uints 4 (or container-idx 0))
    (js/Uint8Array. raw)))

(defn- upload-composites! [system desired]
  (let [rows (into {}
                   (map (fn [{:keys [slot composite]}]
                          [slot composite]))
                   desired)
        max-slot (reduce max -1 (keys rows))
        active-bytes (* (inc max-slot) composite-instance-stride)
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
                        (filter (fn [[slot row]]
                                  (not= row (get prior slot))))
                        rows))]
    (doseq [[slot row] changed]
      (.writeBuffer (.-queue ^js (:device system)) (:buffer buffer)
                    (* slot composite-instance-stride)
                    (composite-row-bytes row)))
    (gpu-budget/set-active-bytes! (:tracker system) (:buffer buffer)
                                  active-bytes)
    (reset! (:!composite-buffer system) buffer)
    (reset! (:!composite-rows system) rows)
    (count changed)))

(defn- font-input-token [font-assets]
  [(placement/provider-identity font-assets)
   (select-keys (get-in font-assets [:atlas :atlas])
                [:distanceRange :width :height])])

(defn- region-entry-shape-key [store-frame regions prepared]
  (mapv (fn [{:keys [region-id]}]
          (let [op (:op (get prepared region-id))]
            [region-id
             (get-in store-frame [:order-by-vi (:owner-vi op)])]))
        regions))

(defn prepare-region3d-frame!
  "Upload region material/session projections before any pass opens. Returns a
   receipt; it never creates a command encoder or requests a target lease."
  [system store-frame session
   {:keys [zoom dpr font-assets session-layout-snapshot atlas-view
           atlas-sampler path-system max-lease-size]
    :or {zoom 1.0 dpr 1.0}}]
  (let [regions (vec (or (:regions store-frame) []))
        prior @(:!prepared system)
        live-ids (set (map :region-id regions))
        computed
        (into {}
              (map
               (fn [op]
                 (frame-inputs/increment-ledger! :region-prepared)
                 (let [region-id (:region-id op)
                       session-row (session-region session region-id)
                       pixel-size [(max 1 (js/Math.ceil (* (:w op) zoom dpr)))
                                   (max 1 (js/Math.ceil (* (:h op) zoom dpr)))]
                       ;; The composite maps the FULL lease onto the region's
                       ;; screen rect, so texels past the attachment size can
                       ;; never reach the screen; capping here also keeps one
                       ;; lease (56 bytes/px across its four targets) inside
                       ;; the frame-target budget at any zoom — unclamped, the
                       ;; 4096-quant MSAA color target alone equals the whole
                       ;; 512MB pool. Camera aspect and picking stay on the
                       ;; unclamped pixel-size.
                       lease-size (mapv compositor/quantize-region-size
                                        (if max-lease-size
                                          (mapv min pixel-size max-lease-size)
                                          pixel-size))
                       encode-scale
                       (frame-inputs/quantize-region-encode-scale (* zoom dpr))
                       encode-rung (frame-inputs/region-encode-rung (* zoom dpr))
                       encode-pixel-size
                       [(max 1 (js/Math.ceil (* (:w op) encode-scale)))
                        (max 1 (js/Math.ceil (* (:h op) encode-scale)))]
                       old (get prior region-id)
                       raw-region (:region3d/scene op)
                       background-key (:background raw-region)
                       background-changed?
                       (or (nil? old)
                           (not= background-key (:background-key old)))
                       evaluation-result
                       (evaluation/evaluate-scene
                        (:maintained old) (:evaluation-key old)
                        raw-region session-row)
                       update-kind (:update-kind evaluation-result)
                       material-changed? (= :full update-kind)
                       transform-changed? (= :transform update-kind)
                       scene-changed? (not= :none update-kind)
                       maintained0
                       (assoc (:maintained evaluation-result)
                              :region-id region-id)
                       maintained
                       (if (and background-changed? (not material-changed?))
                         (assoc-in maintained0 [:region :background]
                                   (:background
                                    (material/validate-region! raw-region)))
                         maintained0)
                       shadow-space (if scene-changed?
                                      (scene/shadow-light-space maintained)
                                      (:shadow-space old))
                       view (or (:view session-row)
                                (get-in maintained [:region :view-default]))
                       view-key [view (:display-mode session-row) encode-rung
                                 shadow-space]
                       view-changed? (or scene-changed? (nil? old)
                                         (not= view-key (:view-key old)))
                       camera (if view-changed?
                                (scene/camera-matrices view encode-pixel-size)
                                (:camera old))
                       prepare-key
                       {:material (:evaluation-key evaluation-result)
                        :background background-key
                        :view view-key :dpr dpr
                        :placements (:region3d/resolved-placements op)
                        :font (font-input-token font-assets)
                        :session-layout
                        (placement/session-layout-key session-layout-snapshot)
                        :atlas-view atlas-view :atlas-sampler atlas-sampler
                        :path-system (frame-inputs/system-token path-system)
                        :max-lease-size max-lease-size}]
                   (let [gpu0 (or (:gpu old)
                                    (create-region-gpu system region-id))
                           upload-receipt
                           (case update-kind
                             :full
                             {:gpu (write-material-gpu! system region-id gpu0
                                                        maintained)
                              :instance-uploads (count (:instances maintained))
                              :mesh-vertex-uploads 1
                              :light-uploads (count (light-rows maintained))}

                             :transform
                             (assoc (write-transform-gpu!
                                     system gpu0 maintained
                                     (:affected-object-ids evaluation-result))
                                    :gpu gpu0 :mesh-vertex-uploads 0)

                             {:gpu gpu0 :instance-uploads 0
                              :mesh-vertex-uploads 0
                              :light-uploads 0})
                           gpu1 (:gpu upload-receipt)
                           gpu2 (if view-changed?
                                  (write-view-gpu! system gpu1 maintained camera
                                                   session-row shadow-space)
                                  gpu1)
                           placement-result
                           (placement-gpu/prepare-placements!
                            (:placement-system system) (:placement gpu2)
                            (:region3d/resolved-placements op) maintained camera
                            (if path-system @(:!mesh-cache path-system) {})
                            {:font-assets font-assets
                             :session-layout-snapshot session-layout-snapshot
                             :atlas-view atlas-view :atlas-sampler atlas-sampler})
                           _ (when path-system
                               (reset! (:!mesh-cache path-system)
                                       (:path-cache placement-result)))
                           gpu3 (assoc gpu2 :placement (:gpu placement-result))
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
                                          (:changed? placement-result)
                                          (nil? old))}]
                       [region-id
                        {:region-id region-id :op op
                         :prepare-key prepare-key
                         :material-key (:evaluation-key evaluation-result)
                         :evaluation-key (:evaluation-key evaluation-result)
                         :background-key background-key :view-key view-key
                         :maintained maintained :camera camera
                         :lease-size lease-size :encode-rung encode-rung
                         :shadow-space shadow-space
                         :shadow? (boolean shadow-space)
                         :gpu gpu3 :draw-order mesh-draw-order
                         :placements (:placements placement-result)
                         :placement-census (:census placement-result)
                         :dirty-by-role dirty-by-role
                         :scene-update-kind update-kind
                         :affected-object-ids
                         (:affected-object-ids evaluation-result)
                         :upload-receipt upload-receipt
                         :material-changed? material-changed?
                         :transform-changed? transform-changed?
                         :background-changed? background-changed?
                         :view-changed? view-changed?
                         :last-lease-keys (:last-lease-keys old)
                         :session session-row}])))
               regions))
        closed (vec (remove live-ids (keys prior)))
        desired
        (region-bindings/reconcile-desired!
         (:binding-owner system)
         (mapv (fn [[region-id row]]
                 (let [op (:op row)]
                   {:region/id region-id
                    :lease-size (:lease-size row)
                    :shadow? (:shadow? row)
                    :background (get-in row [:maintained :region :background])
                    :encode-rung (:encode-rung row)
                    :composite {:x (:x op) :y (:y op)
                                :w (:w op) :h (:h op)
                                :container-idx (:container-idx op)}}))
               computed))
        computed
        (into {}
              (map (fn [[region-id row]]
                     (let [row (assoc row :composite-slot
                                      (get-in desired [region-id :slot]))
                           old (get prior region-id)]
                       [region-id (if (= row old) old row)])))
              computed)
        composite-uploads
        (upload-composites! system (vals desired))
        next (if (and (empty? closed)
                      (= (keys prior) (keys computed))
                      (every? (fn [[id row]] (identical? row (get prior id)))
                              computed))
               prior computed)
        changed-rows (keep (fn [[region-id row]]
                             (when-not (identical? row (get prior region-id))
                               row))
                           computed)
        prepared-changed? (not (identical? prior next))
        entry-shape-key (region-entry-shape-key store-frame regions next)
        entry-shape-changed?
        (not= entry-shape-key @(:!entry-shape-key system))]
    (doseq [region-id closed]
      (destroy-region-gpu! system (:gpu (get prior region-id))))
    (when prepared-changed?
      (reset! (:!prepared system) next))
    (when entry-shape-changed?
      (reset! (:!entry-shape-key system) entry-shape-key)
      (frame-inputs/bump-shape-rev! system))
    (when (or prepared-changed? (seq closed) (pos? composite-uploads))
      (swap! (:!receipt system)
             (fn [receipt]
               (-> receipt
                   (update :prepare-calls inc)
                   (update :scene-derives +
                           (count (filter #(= :full (:scene-update-kind %))
                                          changed-rows)))
                   (update :scene-transform-updates +
                           (count (filter #(= :transform
                                                (:scene-update-kind %))
                                          changed-rows)))
                   (update :object-instance-uploads +
                           (reduce + 0
                                   (map #(get-in % [:upload-receipt
                                                    :instance-uploads] 0)
                                        changed-rows)))
                   (update :mesh-vertex-uploads +
                           (reduce + 0
                                   (map #(get-in % [:upload-receipt
                                                    :mesh-vertex-uploads] 0)
                                        changed-rows)))
                   (update :light-uploads +
                           (reduce + 0
                                   (map #(get-in % [:upload-receipt
                                                    :light-uploads] 0)
                                        changed-rows)))
                   (update :uniform-uploads +
                           (count (filter :view-changed? changed-rows)))
                   (update :composite-uploads + composite-uploads)
                   (assoc :last-prepare
                          {:regions (count regions) :closed closed
                           :dirty (into {}
                                        (map (fn [[id row]]
                                               [id (:dirty-by-role row)]))
                                        next)
                           :scene-updates
                           (into {}
                                 (map (fn [[id row]]
                                        [id {:kind (:scene-update-kind row)
                                             :affected
                                             (:affected-object-ids row)}]))
                                 next)
                           :composite-uploads composite-uploads})))))
    @(:!receipt system)))

(defn region3d-entries
  [{:keys [store-frame region3d-system]}]
  (if-not (and region3d-system store-frame)
    []
    (mapv
     (fn [op]
       (let [region-id (:region-id op)
             source-order (get-in store-frame [:order-by-vi (:owner-vi op)])
             entry (scene/tape-entry
                    {:region-id region-id
                     :revision (get-in op [:region3d/scene :region3d/version])
                     :source-order source-order
                     :rect [(:x op) (:y op) (:w op) (:h op)]})]
         (assoc-in entry [:paint :region-system] region3d-system)))
     (:regions store-frame))))

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
    (placement-gpu/draw-placements! pass (:placement-system system)
                                    (:placement gpu) (:uniform gpu))
    (.end pass)))

(defn encode-region-passes!
  "Compositor pass producer. Clean regions retain the declared pass but encode
   nothing; a new lease key forces one encode before it can be sampled."
  [system encoder pass lease]
  (let [region-id (:region/id pass)
        role (:region/role pass)
        prepared (get @(:!prepared system) region-id)
        encode? (and prepared lease (not (:refused? lease))
                     (or (get-in prepared [:dirty-by-role role])
                         (not= (:key lease)
                               (get-in prepared [:last-lease-keys role]))))]
    (cond
      (or (nil? prepared) (nil? lease))
      {:region-id region-id :role role :encoded? false
       :held? false :reason :missing-region-state}

      (:refused? lease)
      {:region-id region-id :role role :encoded? false
       :held? false :refusal (:refusal lease)}

      (not encode?)
      (do (frame-inputs/increment-ledger! :region-held)
          (swap! (:!receipt system) update :held-passes inc)
          {:region-id region-id :role role
           :encoded? false :held? true :lease-key (:key lease)})

      :else
      (do
        (case role
          :shadow (when (and (:shadow-space prepared) (:shadow lease))
                    (encode-shadow! system encoder prepared lease))
          :interior (encode-interior! system encoder prepared lease)
          nil)
        (frame-inputs/increment-ledger! :region-encoded)
        (swap! (:!receipt system) update :region-encodes inc)
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
                       {:binding 3 :resource {:buffer (:containers-buffer system)}}]})))

(defn- refusal-bind-group [system]
  (.createBindGroup
   ^js (:device system)
   (clj->js {:layout (get-in system [:pipelines :refusal-layout])
             :entries [{:binding 2 :resource {:buffer (:camera-buffer system)}}
                       {:binding 3 :resource {:buffer (:containers-buffer system)}}]})))

(defn execute-region3d-batch! [pass entry]
  (let [{:keys [region-system region-id]}
        (:paint entry)
        owner (:binding-owner region-system)
        lease (region-bindings/lease owner region-id)
        composite-slot (region-bindings/slot owner region-id)
        refused? (or (nil? lease) (:refused? lease))
        pipeline-key (cond
                       refused? :refusal
                       (> (:rung-divisor lease 1) 1) :worn
                       :else :composite)]
    (.setPipeline ^js pass (get-in region-system
                                   [:pipelines pipeline-key]))
    (.setBindGroup ^js pass 0 (if refused?
                                (refusal-bind-group region-system)
                                (composite-bind-group region-system lease)))
    (.setVertexBuffer ^js pass 0 (:buffer @(:!composite-buffer region-system)))
    (.draw ^js pass 6 1 0 composite-slot)
    (:entry/id entry)))

(defn region3d-receipt [system]
  (assoc @(:!receipt system)
         :bindings (region-bindings/receipt (:binding-owner system))
         :placements (placement-gpu/placement-receipt
                      (:placement-system system))
         :prepared
         (into {} (map (fn [[id row]]
                         [id {:dirty-by-role (:dirty-by-role row)
                              :last-lease-keys (:last-lease-keys row)
                              :shadow? (:shadow? row)
                              :placement-layouts
                              (into {}
                                    (map (fn [placed]
                                           [(:object-id placed)
                                            {:address (:address placed)
                                             :status (:status placed)
                                             :layout-id (get-in placed
                                                                [:layout :layout/id])}]))
                                    (:placements row))
                              :objects (count (get-in row [:maintained :instances]))}]))
               @(:!prepared system))))

(defn destroy-region3d-system! [system]
  (doseq [[_ row] @(:!prepared system)]
    (destroy-region-gpu! system (:gpu row)))
  (destroy-buffer! system @(:!composite-buffer system)
                   :region3d-system-destroy)
  (when-let [texture (get-in system [:shadow-fallback :texture])]
    (gpu-budget/destroy-resource! (:tracker system) texture
                                  :reason :region3d-system-destroy)
    (.destroy ^js texture))
  (placement-gpu/destroy-placement-system! (:placement-system system))
  (reset! (:!prepared system) {})
    (.delete !systems-by-device (:device system))
  true)
