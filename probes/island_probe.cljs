(ns app.client.substrate.webgpu.island-probe
  "ISLANDS PROBE (2026-07-11) — UNCOMMITTED EVIDENCE, NOT PRODUCT.

   Rung-1 pipeline truth for the 3D-islands direction: prove a 3D face can be
   rendered in OUR renderer's idiom (own offscreen pass with depth), composited
   into the 2D land as a textured quad, and PRICE it — to falsify three claims
   from the direction session:
     (a) a sleeping island (unchanged scene) costs ~zero GPU per frame;
     (b) island interior frame rate decouples from land frame rate;
     (c) our MSDF atlas text works inside a perspective pass.  [P6 stretch]

   Integration seam (see render.cljs mount, ≤15 lines): the land's render loop
   only draws on world-change. When the probe is DRIVING it forces a redraw so
   we get continuous frames; `step!` runs right AFTER draw-frame! within the
   same RAF, so it composites onto the SAME swapchain texture the land just drew
   (loadOp \"load\", a second submit). No renderer.cljs edits.

   Control surface installed at window.__island (drive from console / harness):
     __island.enable()/.disable()
     __island.set({cubes, tex, render3d, compose, halfRate, sleep, autorotate,
                   yaw, pitch, dist})
     __island.stats()        ;; rolling phase timings + fps
     __island.resetStats()
     __island.snapshotDone() ;; resolves the pending onSubmittedWorkDone probe

   Scene-as-data, picking, faces integration are CONTRACT work — NOT built here."
  (:require [app.client.engine.budget :as gpu-budget]))

;; ─────────────────────────────────────────────────────────────────────────
;; Config + stats (all console/harness-settable)
;; ─────────────────────────────────────────────────────────────────────────

(defonce !cfg
  (atom {:cubes 1000      ;; instance count
         :tex 768         ;; offscreen square edge (px) — set 512 / 1024 for sweep
         :render3d true   ;; run the 3D pass this frame?
         :compose true    ;; composite the island quad onto the land?
         :half-rate false ;; re-render the 3D pass only every other frame
         :sleep false     ;; force-skip the 3D pass (scene frozen → cached texture)
         :autorotate true ;; spin the camera automatically (for numbers/screens)
         :auto-frame true ;; fit camera distance to the cluster span (ignores :dist)
         :show-text true  ;; P6: draw the MSDF label in the 3D pass (needs loadAtlas)
         :text "SOFTLAND"
         :yaw 0.7 :pitch 0.5 :dist 6.0}))

(defonce !enabled (atom false))

;; rolling stats
(defonce !stats (atom nil))
(defonce !frame-counter (atom 0))
(defonce !last-raf-ts (atom nil))
;; onSubmittedWorkDone probe: set a target frame, capture latency when it lands
(defonce !swd (atom {:pending false :samples []}))

(defn- fresh-stats []
  {:n 0
   :encode3d-ms 0.0 :composite-ms 0.0 :total-ms 0.0 :raf-dt-ms 0.0
   :encode3d-max 0.0 :composite-max 0.0 :raf-dt-max 0.0
   :frames-3d 0})

(defn reset-stats! [] (reset! !stats (fresh-stats)))

(defn- accum! [k v]
  (swap! !stats update k + v))

(defn stats-summary []
  (let [{:keys [n encode3d-ms composite-ms total-ms raf-dt-ms
                encode3d-max composite-max raf-dt-max frames-3d]} @!stats
        d (max 1 n)
        avg (fn [x] (/ (Math/round (* 1000 (/ x d))) 1000.0))]
    (clj->js
      {:frames n
       :frames_with_3d frames-3d
       :cfg @!cfg
       :cpu_encode3d_ms_avg (avg encode3d-ms)
       :cpu_encode3d_ms_max (/ (Math/round (* 1000 encode3d-max)) 1000.0)
       :cpu_composite_ms_avg (avg composite-ms)
       :cpu_composite_ms_max (/ (Math/round (* 1000 composite-max)) 1000.0)
       :cpu_island_total_ms_avg (avg total-ms)
       :raf_interval_ms_avg (avg raf-dt-ms)
       :raf_interval_ms_max (/ (Math/round (* 1000 raf-dt-max)) 1000.0)
       :fps_avg (/ (Math/round (* 10 (/ 1000.0 (max 0.001 (avg raf-dt-ms))))) 10.0)
       :swd_samples (clj->js (:samples @!swd))})))

;; ─────────────────────────────────────────────────────────────────────────
;; Column-major 4x4 matrix math (WGSL mat4x4 is column-major)
;; ─────────────────────────────────────────────────────────────────────────

(defn- mat4-identity []
  (js/Float32Array. #js [1 0 0 0, 0 1 0 0, 0 0 1 0, 0 0 0 1]))

(defn- mat4-mul
  "Column-major a*b."
  [^js a ^js b]
  (let [o (js/Float32Array. 16)]
    (dotimes [c 4]
      (dotimes [r 4]
        (aset o (+ (* c 4) r)
              (+ (* (aget a (+ 0 r)) (aget b (+ (* c 4) 0)))
                 (* (aget a (+ 4 r)) (aget b (+ (* c 4) 1)))
                 (* (aget a (+ 8 r)) (aget b (+ (* c 4) 2)))
                 (* (aget a (+ 12 r)) (aget b (+ (* c 4) 3)))))))
    o))

(defn- mat4-perspective [fovy aspect near far]
  (let [f (/ 1.0 (Math/tan (/ fovy 2.0)))
        nf (/ 1.0 (- near far))
        o (js/Float32Array. 16)]
    (aset o 0 (/ f aspect))
    (aset o 5 f)
    (aset o 10 (* (+ far near) nf))
    (aset o 11 -1.0)
    (aset o 14 (* 2.0 far near nf))
    o))

(defn- v3-sub [a b] #js [(- (aget a 0) (aget b 0)) (- (aget a 1) (aget b 1)) (- (aget a 2) (aget b 2))])
(defn- v3-cross [a b]
  #js [(- (* (aget a 1) (aget b 2)) (* (aget a 2) (aget b 1)))
       (- (* (aget a 2) (aget b 0)) (* (aget a 0) (aget b 2)))
       (- (* (aget a 0) (aget b 1)) (* (aget a 1) (aget b 0)))])
(defn- v3-norm [a]
  (let [l (Math/sqrt (+ (* (aget a 0) (aget a 0)) (* (aget a 1) (aget a 1)) (* (aget a 2) (aget a 2))))
        l (if (< l 1e-6) 1e-6 l)]
    #js [(/ (aget a 0) l) (/ (aget a 1) l) (/ (aget a 2) l)]))
(defn- v3-dot [a b] (+ (* (aget a 0) (aget b 0)) (* (aget a 1) (aget b 1)) (* (aget a 2) (aget b 2))))

(defn- mat4-look-at [eye center up]
  (let [z (v3-norm (v3-sub eye center))     ;; forward (points away from center)
        x (v3-norm (v3-cross up z))         ;; right
        y (v3-cross z x)                    ;; true up
        o (js/Float32Array. 16)]
    (aset o 0 (aget x 0)) (aset o 1 (aget y 0)) (aset o 2 (aget z 0)) (aset o 3 0)
    (aset o 4 (aget x 1)) (aset o 5 (aget y 1)) (aset o 6 (aget z 1)) (aset o 7 0)
    (aset o 8 (aget x 2)) (aset o 9 (aget y 2)) (aset o 10 (aget z 2)) (aset o 11 0)
    (aset o 12 (- (v3-dot x eye))) (aset o 13 (- (v3-dot y eye))) (aset o 14 (- (v3-dot z eye))) (aset o 15 1)
    o))

;; ─────────────────────────────────────────────────────────────────────────
;; WGSL — the two shaders (P2 3D pass, P3 composite)
;; ─────────────────────────────────────────────────────────────────────────

(def cube-3d-shader "
struct U { viewProj: mat4x4<f32>, lightDir: vec4<f32> };
struct Instance { model: mat4x4<f32>, color: vec4<f32> };
@group(0) @binding(0) var<uniform> u: U;
@group(0) @binding(1) var<storage, read> instances: array<Instance>;

struct VOut {
  @builtin(position) pos: vec4<f32>,
  @location(0) normal: vec3<f32>,
  @location(1) color: vec4<f32>,
};

@vertex
fn vs(@location(0) position: vec3<f32>,
      @location(1) normal: vec3<f32>,
      @builtin(instance_index) ii: u32) -> VOut {
  let inst = instances[ii];
  let world = inst.model * vec4<f32>(position, 1.0);
  var out: VOut;
  out.pos = u.viewProj * world;
  out.normal = normalize((inst.model * vec4<f32>(normal, 0.0)).xyz);
  out.color = inst.color;
  return out;
}

@fragment
fn fs(in: VOut) -> @location(0) vec4<f32> {
  let L = normalize(u.lightDir.xyz);
  let ndl = max(dot(normalize(in.normal), L), 0.0);
  let ambient = 0.28;
  let shade = ambient + (1.0 - ambient) * ndl;
  return vec4<f32>(in.color.rgb * shade, 1.0);
}")

(def composite-shader "
struct R { rect: vec4<f32> };   // clip-space: cx, cy, half-w, half-h
@group(0) @binding(0) var samp: sampler;
@group(0) @binding(1) var tex: texture_2d<f32>;
@group(0) @binding(2) var<uniform> r: R;

struct VOut { @builtin(position) pos: vec4<f32>, @location(0) uv: vec2<f32> };

@vertex
fn vs(@builtin(vertex_index) vi: u32) -> VOut {
  var corners = array<vec2<f32>, 6>(
    vec2<f32>(-1.0,-1.0), vec2<f32>( 1.0,-1.0), vec2<f32>(-1.0, 1.0),
    vec2<f32>(-1.0, 1.0), vec2<f32>( 1.0,-1.0), vec2<f32>( 1.0, 1.0));
  let c = corners[vi];
  var out: VOut;
  out.pos = vec4<f32>(r.rect.x + c.x * r.rect.z, r.rect.y + c.y * r.rect.w, 0.0, 1.0);
  out.uv = vec2<f32>((c.x + 1.0) * 0.5, (1.0 - c.y) * 0.5);  // flip Y → upright
  return out;
}

@fragment
fn fs(in: VOut) -> @location(0) vec4<f32> {
  return textureSample(tex, samp, in.uv);
}")

;; ─────────────────────────────────────────────────────────────────────────
;; Cube geometry (unit cube [-0.5,0.5]^3): 36 verts × (pos3, normal3)
;; ─────────────────────────────────────────────────────────────────────────

(defn- build-cube-verts []
  (let [faces [;; [origin, u-edge, v-edge, normal]
               [[-0.5 -0.5  0.5] [1 0 0] [0 1 0] [0 0 1]]    ;; +Z
               [[ 0.5 -0.5 -0.5] [-1 0 0] [0 1 0] [0 0 -1]]  ;; -Z
               [[ 0.5 -0.5  0.5] [0 0 -1] [0 1 0] [1 0 0]]   ;; +X
               [[-0.5 -0.5 -0.5] [0 0 1] [0 1 0] [-1 0 0]]   ;; -X
               [[-0.5  0.5  0.5] [1 0 0] [0 0 -1] [0 1 0]]   ;; +Y
               [[-0.5 -0.5 -0.5] [1 0 0] [0 0 1] [0 -1 0]]]  ;; -Y
        out (js/Float32Array. (* 36 6))
        idx (atom 0)
        push! (fn [p n]
                (let [i @idx]
                  (aset out (+ i 0) (nth p 0)) (aset out (+ i 1) (nth p 1)) (aset out (+ i 2) (nth p 2))
                  (aset out (+ i 3) (nth n 0)) (aset out (+ i 4) (nth n 1)) (aset out (+ i 5) (nth n 2))
                  (reset! idx (+ i 6))))
        add (fn [a b] (mapv + a b))]
    (doseq [[o u v n] faces]
      (let [p00 o
            p10 (add o u)
            p01 (add o v)
            p11 (add o (add u v))]
        ;; two ccw triangles
        (push! p00 n) (push! p10 n) (push! p11 n)
        (push! p00 n) (push! p11 n) (push! p01 n)))
    out))

;; ─────────────────────────────────────────────────────────────────────────
;; Scene: N instances (model mat4 + color) packed for the storage buffer
;; ─────────────────────────────────────────────────────────────────────────

(def ^:private inst-floats 20)  ;; mat4 (16) + color vec4 (4)

(defn- hsl->rgb [h]
  ;; simple hue → rgb, s=0.65 l=0.55
  (let [s 0.65 l 0.55
        c (* (- 1.0 (Math/abs (- (* 2 l) 1.0))) s)
        h' (/ (mod h 360) 60.0)
        x (* c (- 1.0 (Math/abs (- (mod h' 2) 1.0))))
        m (- l (/ c 2.0))
        [r g b] (cond
                  (< h' 1) [c x 0] (< h' 2) [x c 0] (< h' 3) [0 c x]
                  (< h' 4) [0 x c] (< h' 5) [x 0 c] :else [c 0 x])]
    [(+ r m) (+ g m) (+ b m)]))

(defn- build-scene-bytes [n]
  (let [data (js/Float32Array. (* n inst-floats))
        ;; lay cubes on a rough cube-root grid
        side (max 1 (Math/ceil (Math/cbrt n)))
        spacing 1.6
        half (* 0.5 (dec side) spacing)
        cube-scale 0.6]
    (dotimes [i n]
      (let [gx (mod i side)
            gy (mod (Math/floor (/ i side)) side)
            gz (Math/floor (/ i (* side side)))
            tx (- (* gx spacing) half)
            ty (- (* gy spacing) half)
            tz (- (* gz spacing) half)
            base (* i inst-floats)
            [r g b] (hsl->rgb (* i 37))]
        ;; column-major model = translate * scale (diagonal scale, translation col 3)
        (aset data (+ base 0) cube-scale) (aset data (+ base 5) cube-scale) (aset data (+ base 10) cube-scale)
        (aset data (+ base 15) 1.0)
        (aset data (+ base 12) tx) (aset data (+ base 13) ty) (aset data (+ base 14) tz)
        (aset data (+ base 16) r) (aset data (+ base 17) g) (aset data (+ base 18) b) (aset data (+ base 19) 1.0)))
    data))

;; ─────────────────────────────────────────────────────────────────────────
;; GPU resources (lazy, memoized in !gpu). Rebuilt when tex/cubes change.
;; ─────────────────────────────────────────────────────────────────────────

(defonce !gpu (atom nil))

(defn- swap-format []
  (.getPreferredCanvasFormat ^js js/navigator.gpu))

(defn- make-offscreen [^js device tex-size fformat tracker]
  (let [color (.createTexture device
                (clj->js {:size {:width tex-size :height tex-size}
                          :format fformat
                          :usage (bit-or js/GPUTextureUsage.RENDER_ATTACHMENT
                                         js/GPUTextureUsage.TEXTURE_BINDING)}))
        depth (.createTexture device
                (clj->js {:size {:width tex-size :height tex-size}
                          :format "depth24plus"
                          :usage js/GPUTextureUsage.RENDER_ATTACHMENT}))]
    (gpu-budget/register-texture! tracker color "island-probe/offscreen-color"
                                  :format fformat :width tex-size :height tex-size)
    (gpu-budget/register-texture! tracker depth "island-probe/offscreen-depth"
                                  :format "depth24plus" :width tex-size :height tex-size)
    {:color color :color-view (.createView color)
     :depth depth :depth-view (.createView depth)
     :size tex-size}))

(defn- with-error-scope [^js device label thunk]
  (.pushErrorScope device "validation")
  (let [r (thunk)]
    (.then (.popErrorScope device)
           (fn [err] (when err (js/console.error (str "[ISLAND] validation @" label ": " (.-message err))))))
    r))

(defn- make-3d-pipeline [^js device fformat]
  (let [module (.createShaderModule device (clj->js {:code cube-3d-shader}))
        bg-layout (.createBindGroupLayout device
                    (clj->js {:entries [{:binding 0 :visibility (bit-or js/GPUShaderStage.VERTEX js/GPUShaderStage.FRAGMENT) :buffer {:type "uniform"}}
                                        {:binding 1 :visibility js/GPUShaderStage.VERTEX :buffer {:type "read-only-storage"}}]}))
        layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (with-error-scope device "3d-pipeline"
                   (fn [] (.createRenderPipeline device
                   (clj->js {:layout layout
                             :vertex {:module module :entryPoint "vs"
                                      :buffers [{:arrayStride 24 :stepMode "vertex"
                                                 :attributes [{:shaderLocation 0 :offset 0 :format "float32x3"}
                                                              {:shaderLocation 1 :offset 12 :format "float32x3"}]}]}
                             :fragment {:module module :entryPoint "fs"
                                        :targets [{:format fformat}]}
                             :primitive {:topology "triangle-list" :cullMode "back" :frontFace "ccw"}
                             :depthStencil {:format "depth24plus" :depthWriteEnabled true :depthCompare "less"}}))))]
    {:pipeline pipeline :bg-layout bg-layout}))

(defn- make-composite-pipeline [^js device fformat]
  (let [module (.createShaderModule device (clj->js {:code composite-shader}))
        bg-layout (.createBindGroupLayout device
                    (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.FRAGMENT :sampler {:type "filtering"}}
                                        {:binding 1 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "float"}}
                                        {:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}]}))
        layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (with-error-scope device "composite-pipeline"
                   (fn [] (.createRenderPipeline device
                   (clj->js {:layout layout
                             :vertex {:module module :entryPoint "vs"}
                             :fragment {:module module :entryPoint "fs"
                                        :targets [{:format fformat}]}
                             :primitive {:topology "triangle-list"}}))))]
    {:pipeline pipeline :bg-layout bg-layout}))

(defn- ensure-gpu!
  "Build/rebuild memoized GPU resources when device/tex/cubes change."
  [^js device tracker]
  (let [{:keys [cubes tex]} @!cfg
        cur @!gpu
        fformat (swap-format)
        need-rebuild? (or (nil? cur)
                          (not (identical? device (:device cur)))
                          (not= tex (:tex cur))
                          (not= cubes (:cubes cur)))]
    (if-not need-rebuild?
      cur
      (let [;; static per-device pipelines (reuse if device unchanged)
            reuse? (and cur (identical? device (:device cur)))
            cube-buf (or (:cube-buf cur)
                         (let [verts (build-cube-verts)
                               b (.createBuffer device (clj->js {:size (.-byteLength verts)
                                                                 :usage (bit-or js/GPUBufferUsage.VERTEX js/GPUBufferUsage.COPY_DST)}))]
                           (.writeBuffer (.-queue device) b 0 verts)
                           (gpu-budget/register-buffer! tracker b "island-probe/cube-verts" (.-byteLength verts) :active-bytes (.-byteLength verts))
                           b))
            p3d (or (:p3d cur) (make-3d-pipeline device fformat))
            pcomp (or (:pcomp cur) (make-composite-pipeline device fformat))
            u-buf (or (:u-buf cur)
                      (let [b (.createBuffer device (clj->js {:size 80 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))]
                        (gpu-budget/register-buffer! tracker b "island-probe/uniform" 80 :active-bytes 80)
                        b))
            rect-buf (or (:rect-buf cur)
                         (let [b (.createBuffer device (clj->js {:size 16 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))]
                           (gpu-budget/register-buffer! tracker b "island-probe/rect" 16 :active-bytes 16)
                           b))
            sampler (or (:sampler cur) (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear"})))
            ;; scratch swap-sized target so bench! can measure composite fill cost off-swapchain
            scratch (or (:scratch cur)
                        (let [t (.createTexture device (clj->js {:size {:width 1600 :height 1000}
                                                                 :format fformat
                                                                 :usage js/GPUTextureUsage.RENDER_ATTACHMENT}))]
                          (gpu-budget/register-texture! tracker t "island-probe/scratch" :format fformat :width 1600 :height 1000)
                          {:tex t :view (.createView t)}))
            ;; offscreen: rebuild if tex changed
            off (if (and cur (= tex (:tex cur)) (:off cur)) (:off cur)
                    (make-offscreen device tex fformat tracker))
            ;; instance storage buffer: rebuild if cubes changed
            scene (build-scene-bytes cubes)
            span (let [side (max 1 (Math/ceil (Math/cbrt cubes)))] (+ (* (dec side) 1.6) 1.0))
            inst-buf (.createBuffer device (clj->js {:size (.-byteLength scene)
                                                     :usage (bit-or js/GPUBufferUsage.STORAGE js/GPUBufferUsage.COPY_DST)}))
            _ (.writeBuffer (.-queue device) inst-buf 0 scene)
            _ (gpu-budget/register-buffer! tracker inst-buf "island-probe/instances" (.-byteLength scene) :active-bytes (.-byteLength scene))
            bg-3d (.createBindGroup device
                    (clj->js {:layout (:bg-layout p3d)
                              :entries [{:binding 0 :resource {:buffer u-buf}}
                                        {:binding 1 :resource {:buffer inst-buf}}]}))
            bg-comp (.createBindGroup device
                      (clj->js {:layout (:bg-layout pcomp)
                                :entries [{:binding 0 :resource sampler}
                                          {:binding 1 :resource (:color-view off)}
                                          {:binding 2 :resource {:buffer rect-buf}}]}))]
        (js/console.log "[ISLAND] (re)built GPU resources" (clj->js {:cubes cubes :tex tex :format fformat}))
        (reset! !gpu
                {:device device :tex tex :cubes cubes :format fformat :span span
                 :cube-buf cube-buf :p3d p3d :pcomp pcomp
                 :u-buf u-buf :rect-buf rect-buf :sampler sampler :scratch scratch
                 :off off :inst-buf inst-buf :bg-3d bg-3d :bg-comp bg-comp
                 :cube-verts-count 36})))))

;; ─────────────────────────────────────────────────────────────────────────
;; P6 (stretch): MSDF text billboard inside the 3D pass (claim c).
;; A FIXED-ORIENTATION label embedded in the scene — it foreshortens under
;; perspective, which is the real test of MSDF screen-space AA (fwidth-based
;; screenPxRange, the perspective-correct Chlumsky formula).
;; ─────────────────────────────────────────────────────────────────────────

(def text-3d-shader "
struct U { viewProj: mat4x4<f32>, lightDir: vec4<f32> };
struct T { origin: vec4<f32>, params: vec4<f32> };  // origin.xyz ; params: scale, distRange, atlasSize, _
struct Glyph { rect: vec4<f32>, uv: vec4<f32> };    // rect x,y,w,h (em) ; uv u0,v0,u1,v1
@group(0) @binding(0) var<uniform> u: U;
@group(0) @binding(1) var<uniform> t: T;
@group(0) @binding(2) var<storage, read> glyphs: array<Glyph>;
@group(0) @binding(3) var samp: sampler;
@group(0) @binding(4) var atlas: texture_2d<f32>;

struct VOut { @builtin(position) pos: vec4<f32>, @location(0) uv: vec2<f32> };

@vertex
fn vs(@builtin(vertex_index) vi: u32, @builtin(instance_index) ii: u32) -> VOut {
  var corners = array<vec2<f32>,6>(vec2<f32>(0.,0.),vec2<f32>(1.,0.),vec2<f32>(0.,1.),
                                   vec2<f32>(1.,0.),vec2<f32>(1.,1.),vec2<f32>(0.,1.));
  let c = corners[vi];
  let g = glyphs[ii];
  let lx = (g.rect.x + c.x * g.rect.z) * t.params.x;
  let ly = (g.rect.y + c.y * g.rect.w) * t.params.x;
  let world = t.origin.xyz + vec3<f32>(lx, ly, 0.0);
  var out: VOut;
  out.pos = u.viewProj * vec4<f32>(world, 1.0);
  out.uv = vec2<f32>(mix(g.uv.x, g.uv.z, c.x), mix(g.uv.y, g.uv.w, c.y));
  return out;
}

fn median(a: f32, b: f32, c: f32) -> f32 { return max(min(a,b), min(max(a,b), c)); }

@fragment
fn fs(in: VOut) -> @location(0) vec4<f32> {
  let msd = textureSample(atlas, samp, in.uv).rgb;
  let sd = median(msd.r, msd.g, msd.b);
  let d = sd - 0.5;
  let unitRange = vec2<f32>(t.params.y / t.params.z);       // distanceRange / atlasSize (UV units)
  let screenTexSize = vec2<f32>(1.0) / fwidth(in.uv);        // screen px per UV
  let screenPxRange = max(0.5 * dot(unitRange, screenTexSize), 1.0);
  let alpha = clamp(d * screenPxRange + 0.5, 0.0, 1.0);
  if (alpha < 0.03) { discard; }
  return vec4<f32>(1.0, 0.93, 0.55, alpha);                  // warm label color
}")

(defonce !atlas (atom nil))
(defonce !runtime (atom nil)) ;; {:device :ctx :tracker} — stashed every land frame for bench!

(defn- build-text-instances [glyph-map s]
  ;; → {:data Float32Array(8/glyph) :count n :total-adv em}
  (let [chars (vec s)
        atlas-size 2048.0
        total-adv (reduce (fn [a ch] (+ a (:advance (get glyph-map (.charCodeAt ch 0)) 0.56))) 0.0 chars)
        drawable (filter (fn [ch] (let [g (get glyph-map (.charCodeAt ch 0))]
                                    (and g (:planeBounds g)))) chars)
        n (count drawable)
        data (js/Float32Array. (* n 8))
        pen (atom (- (/ total-adv 2.0)))
        i (atom 0)]
    (doseq [ch chars]
      (let [g (get glyph-map (.charCodeAt ch 0))]
        (when (and g (:planeBounds g))
          (let [{:keys [left bottom right top]} (:planeBounds g)
                a (:atlasBounds g)
                base (* @i 8)
                px @pen]
            (aset data (+ base 0) (+ px left))
            (aset data (+ base 1) bottom)
            (aset data (+ base 2) (- right left))
            (aset data (+ base 3) (- top bottom))
            ;; uv: atlas yOrigin bottom → v = 1 - y/size
            (aset data (+ base 4) (/ (:left a) atlas-size))
            (aset data (+ base 5) (- 1.0 (/ (:bottom a) atlas-size)))
            (aset data (+ base 6) (/ (:right a) atlas-size))
            (aset data (+ base 7) (- 1.0 (/ (:top a) atlas-size)))
            (swap! i inc)))
        (when g (swap! pen + (or (:advance g) 0.56)))))
    {:data data :count @i :total-adv total-adv}))

(defn- make-text-pipeline [^js device fformat]
  (let [module (.createShaderModule device (clj->js {:code text-3d-shader}))
        bg-layout (.createBindGroupLayout device
                    (clj->js {:entries [{:binding 0 :visibility js/GPUShaderStage.VERTEX :buffer {:type "uniform"}}
                                        {:binding 1 :visibility (bit-or js/GPUShaderStage.VERTEX js/GPUShaderStage.FRAGMENT) :buffer {:type "uniform"}}
                                        {:binding 2 :visibility js/GPUShaderStage.VERTEX :buffer {:type "read-only-storage"}}
                                        {:binding 3 :visibility js/GPUShaderStage.FRAGMENT :sampler {:type "filtering"}}
                                        {:binding 4 :visibility js/GPUShaderStage.FRAGMENT :texture {:sampleType "float"}}]}))
        layout (.createPipelineLayout device (clj->js {:bindGroupLayouts [bg-layout]}))
        pipeline (with-error-scope device "text-pipeline"
                   (fn [] (.createRenderPipeline device
                     (clj->js {:layout layout
                               :vertex {:module module :entryPoint "vs"}
                               :fragment {:module module :entryPoint "fs"
                                          :targets [{:format fformat
                                                     :blend {:color {:srcFactor "src-alpha" :dstFactor "one-minus-src-alpha"}
                                                             :alpha {:srcFactor "one" :dstFactor "one-minus-src-alpha"}}}]}
                               :primitive {:topology "triangle-list"}
                               :depthStencil {:format "depth24plus" :depthWriteEnabled false :depthCompare "less-equal"}}))))]
    {:pipeline pipeline :bg-layout bg-layout}))

(defn load-atlas!
  "Fetch the MSDF atlas (PNG+JSON), build the text pipeline + label instances.
   Returns a Promise. Idempotent."
  []
  (if @!atlas
    (js/Promise.resolve "already loaded")
    (let [{:keys [^js device tracker]} @!runtime
          fformat (swap-format)]
      (-> (js/Promise.all
            #js [(.then (js/fetch "/font_atlas.json") (fn [r] (.json r)))
                 (.then (js/fetch "/font_atlas.png") (fn [r] (.blob r)))])
          (.then (fn [[json blob]]
                   (.then (js/createImageBitmap blob)
                     (fn [bitmap]
                       (let [j (js->clj json :keywordize-keys true)
                             glyph-map (reduce (fn [m g] (assoc m (:unicode g) g)) {} (:glyphs j))
                             size (get-in j [:atlas :size])
                             w (get-in j [:atlas :width]) h (get-in j [:atlas :height])
                             dist (get-in j [:atlas :distanceRange])
                             ^js tex (.createTexture device (clj->js {:size {:width w :height h}
                                                                  :format "rgba8unorm"
                                                                  :usage (bit-or js/GPUTextureUsage.TEXTURE_BINDING
                                                                                 js/GPUTextureUsage.RENDER_ATTACHMENT
                                                                                 js/GPUTextureUsage.COPY_DST)}))
                             _ (.copyExternalImageToTexture (.-queue device)
                                 (clj->js {:source bitmap}) (clj->js {:texture tex})
                                 (clj->js {:width w :height h}))
                             _ (gpu-budget/register-texture! tracker tex "island-probe/msdf-atlas" :format "rgba8unorm" :width w :height h)
                             sampler (.createSampler device (clj->js {:minFilter "linear" :magFilter "linear" :mipmapFilter "linear"}))
                             pipe (make-text-pipeline device fformat)
                             {:keys [data total-adv] glyph-count :count} (build-text-instances glyph-map (:text @!cfg))
                             inst (.createBuffer device (clj->js {:size (max 32 (.-byteLength data)) :usage (bit-or js/GPUBufferUsage.STORAGE js/GPUBufferUsage.COPY_DST)}))
                             _ (.writeBuffer (.-queue device) inst 0 data)
                             vp-buf (.createBuffer device (clj->js {:size 80 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
                             t-buf (.createBuffer device (clj->js {:size 32 :usage (bit-or js/GPUBufferUsage.UNIFORM js/GPUBufferUsage.COPY_DST)}))
                             bg (.createBindGroup device (clj->js {:layout (:bg-layout pipe)
                                                                   :entries [{:binding 0 :resource {:buffer vp-buf}}
                                                                             {:binding 1 :resource {:buffer t-buf}}
                                                                             {:binding 2 :resource {:buffer inst}}
                                                                             {:binding 3 :resource sampler}
                                                                             {:binding 4 :resource (.createView tex)}]}))]
                         (reset! !atlas {:device device :glyph-map glyph-map :size size :dist dist
                                         :tex tex :sampler sampler :pipe pipe :inst inst :count glyph-count
                                         :total-adv total-adv :vp-buf vp-buf :t-buf t-buf :bg bg})
                         (js/console.log "[ISLAND] MSDF atlas loaded" (clj->js {:glyphs (count glyph-map) :size size :dist dist :label-glyphs glyph-count}))
                         "atlas loaded"))))))
      )))

;; ─────────────────────────────────────────────────────────────────────────
;; Per-frame camera → uniform
;; ─────────────────────────────────────────────────────────────────────────

(defn- update-uniform! [^js device u-buf]
  (let [{:keys [yaw pitch dist auto-frame]} @!cfg
        span (or (:span @!gpu) 6.0)
        dist (if auto-frame (* span 1.35) dist)
        aspect 1.0
        proj (mat4-perspective (* 0.9) aspect 0.1 (max 100.0 (* span 4)))
        ex (* dist (Math/cos pitch) (Math/sin yaw))
        ey (* dist (Math/sin pitch))
        ez (* dist (Math/cos pitch) (Math/cos yaw))
        view (mat4-look-at #js [ex ey ez] #js [0 0 0] #js [0 1 0])
        vp (mat4-mul proj view)
        buf (js/Float32Array. 20)]
    (.set buf vp 0)
    ;; light dir (world space), pointing from upper-right-front
    (aset buf 16 0.5) (aset buf 17 0.8) (aset buf 18 0.6) (aset buf 19 0.0)
    (.writeBuffer (.-queue device) u-buf 0 buf)
    ;; P6: feed the text pipeline the same view-projection + label transform
    (when-let [a @!atlas]
      (.writeBuffer (.-queue device) (:vp-buf a) 0 buf)
      (let [total-adv (max 0.001 (:total-adv a))
            scale (/ (* 0.5 span) total-adv)
            front-z (+ (* 0.5 span) (* 0.15 span))
            t (js/Float32Array. #js [0.0 (* -0.02 span) front-z 0.0
                                     scale (:dist a) (:size a) 0.0])]
        (.writeBuffer (.-queue device) (:t-buf a) 0 t)))))

(defn- update-rect! [^js device rect-buf vp-w vp-h]
  ;; Island quad occupies a fixed square in the top-right, ~40% of viewport height.
  (let [px (min vp-w vp-h)
        edge (* 0.42 px)                       ;; quad edge in CSS px
        margin (* 0.03 px)
        ;; center in pixel coords (top-right), then → clip space
        cx-px (- vp-w margin (/ edge 2))
        cy-px (+ margin (/ edge 2))
        cx (- (* 2.0 (/ cx-px vp-w)) 1.0)
        cy (- 1.0 (* 2.0 (/ cy-px vp-h)))       ;; clip Y up
        hw (/ edge vp-w)
        hh (/ edge vp-h)
        buf (js/Float32Array. #js [cx cy hw hh])]
    (.writeBuffer (.-queue device) rect-buf 0 buf)))

;; ─────────────────────────────────────────────────────────────────────────
;; The per-frame step (called right after draw-frame! in render.cljs)
;; ─────────────────────────────────────────────────────────────────────────

(defn driving?
  "When true, render.cljs forces a redraw so the probe gets continuous frames."
  []
  @!enabled)

(defn- should-render-3d? [frame]
  (let [{:keys [render3d sleep half-rate]} @!cfg]
    (and render3d (not sleep)
         (or (not half-rate) (zero? (mod frame 2))))))

(defn step!
  "P1+P2+P3: (maybe) render the island 3D pass to offscreen, then composite the
   offscreen texture onto the swapchain texture the land just drew.
   NEVER throws — disables the probe on error so the land is unaffected."
  [^js device ^js ctx vp-w vp-h _dpr tracker]
  (reset! !runtime {:device device :ctx ctx :tracker tracker})
  (when @!enabled
    (try
      (when @!stats
        (let [now (js/performance.now)
              last @!last-raf-ts]
          (when last (let [dt (- now last)]
                       (accum! :raf-dt-ms dt)
                       (swap! !stats update :raf-dt-max max dt)))
          (reset! !last-raf-ts now)))
      (let [g (ensure-gpu! device tracker)
            frame (swap! !frame-counter inc)
            {:keys [autorotate]} @!cfg]
        (when autorotate
          (swap! !cfg update :yaw + 0.01))
        (update-uniform! device (:u-buf g))
        (update-rect! device (:rect-buf g) vp-w vp-h)
        (let [do3d? (should-render-3d? frame)
              t0 (js/performance.now)
              enc (.createCommandEncoder device)]
          ;; ── P2: 3D pass → offscreen (skipped when sleeping/half-rate-off) ──
          (when do3d?
            (let [off (:off g)
                  pass (.beginRenderPass enc
                         (clj->js {:colorAttachments [{:view (:color-view off)
                                                       :clearValue {:r 0.06 :g 0.07 :b 0.11 :a 1.0}
                                                       :loadOp "clear" :storeOp "store"}]
                                   :depthStencilAttachment {:view (:depth-view off)
                                                            :depthClearValue 1.0
                                                            :depthLoadOp "clear" :depthStoreOp "store"}}))]
              (.setPipeline pass (:pipeline (:p3d g)))
              (.setBindGroup pass 0 (:bg-3d g))
              (.setVertexBuffer pass 0 (:cube-buf g))
              (.draw pass 36 (:cubes g))
              ;; P6: MSDF label in the same pass (respects the depth buffer)
              (when (and (:show-text @!cfg) @!atlas (pos? (:count @!atlas)))
                (let [a @!atlas]
                  (.setPipeline pass (:pipeline (:pipe a)))
                  (.setBindGroup pass 0 (:bg a))
                  (.draw pass 6 (:count a))))
              (.end pass)))
          (let [t1 (js/performance.now)]
            ;; ── P3: composite offscreen → swapchain (loadOp "load") ──
            (when (:compose @!cfg)
              (let [swap-tex (.getCurrentTexture ctx)
                    pass (.beginRenderPass enc
                           (clj->js {:colorAttachments [{:view (.createView swap-tex)
                                                         :loadOp "load" :storeOp "store"}]}))]
                (.setPipeline pass (:pipeline (:pcomp g)))
                (.setBindGroup pass 0 (:bg-comp g))
                (.draw pass 6)
                (.end pass)))
            (let [t2 (js/performance.now)]
              (.submit (.-queue device) #js [(.finish enc)])
              ;; onSubmittedWorkDone probe (GPU-completion proxy)
              (when (:pending @!swd)
                (swap! !swd assoc :pending false)
                (let [submit-t (js/performance.now)]
                  (.then (.onSubmittedWorkDone (.-queue device))
                         (fn []
                           (let [lat (- (js/performance.now) submit-t)]
                             (swap! !swd update :samples conj (/ (Math/round (* 100 lat)) 100.0)))))))
              (when @!stats
                (accum! :n 1)
                (when do3d? (accum! :frames-3d 1) (accum! :encode3d-ms (- t1 t0))
                      (swap! !stats update :encode3d-max max (- t1 t0)))
                (accum! :composite-ms (- t2 t1))
                (swap! !stats update :composite-max max (- t2 t1))
                (accum! :total-ms (- t2 t0)))))))
      (catch :default err
        (js/console.error "[ISLAND] step! failed — disabling probe" err)
        (reset! !enabled false)))))

;; ─────────────────────────────────────────────────────────────────────────
;; Serialized GPU timing (bench!) — no timestamp-query on this device, so we
;; submit ONE island frame with the land idle, await onSubmittedWorkDone, and
;; measure wall-clock. Median over N, minus an empty-submit baseline, isolates
;; each pass. Modes: :empty (submit overhead floor), :composite (quad → scratch,
;; = the sleeping-island cost), :3d (cubes → offscreen), :both.
;; ─────────────────────────────────────────────────────────────────────────

(defn- bench-encode! [^js device g mode]
  (let [enc (.createCommandEncoder device)]
    (when (or (= mode :3d) (= mode :both))
      (let [off (:off g)
            pass (.beginRenderPass enc
                   (clj->js {:colorAttachments [{:view (:color-view off)
                                                 :clearValue {:r 0.06 :g 0.07 :b 0.11 :a 1.0}
                                                 :loadOp "clear" :storeOp "store"}]
                             :depthStencilAttachment {:view (:depth-view off)
                                                      :depthClearValue 1.0
                                                      :depthLoadOp "clear" :depthStoreOp "store"}}))]
        (.setPipeline pass (:pipeline (:p3d g)))
        (.setBindGroup pass 0 (:bg-3d g))
        (.setVertexBuffer pass 0 (:cube-buf g))
        (.draw pass 36 (:cubes g))
        (.end pass)))
    (when (or (= mode :composite) (= mode :both))
      (let [pass (.beginRenderPass enc
                   (clj->js {:colorAttachments [{:view (:view (:scratch g))
                                                 :clearValue {:r 0 :g 0 :b 0 :a 1}
                                                 :loadOp "clear" :storeOp "store"}]}))]
        (.setPipeline pass (:pipeline (:pcomp g)))
        (.setBindGroup pass 0 (:bg-comp g))
        (.draw pass 6)
        (.end pass)))
    (.finish enc)))

(defn- bench-one [^js device g mode]
  (js/Promise.
    (fn [resolve _]
      (let [t0 (js/performance.now)]
        (.submit (.-queue device) #js [(bench-encode! device g mode)])
        (.then (.onSubmittedWorkDone (.-queue device))
               (fn [] (resolve (- (js/performance.now) t0))))))))

(defn- bench-samples [^js device g mode n]
  (js/Promise.
    (fn [resolve _]
      (letfn [(go [k acc]
                (if (zero? k)
                  (resolve acc)
                  (.then (bench-one device g mode)
                         (fn [ms] (go (dec k) (conj acc ms))))))]
        (go n [])))))

(defn- ms-stats [samples]
  (let [s (vec (sort samples)) n (max 1 (count s))
        r2 (fn [x] (/ (Math/round (* 1000 x)) 1000.0))]
    {:min (r2 (first s))
     :median (r2 (nth s (quot n 2)))
     :mean (r2 (/ (reduce + s) n))
     :p95 (r2 (nth s (min (dec n) (int (* 0.95 n)))))}))

(defn- bench-modes [^js device g modes iterations]
  (reduce (fn [p mode]
            (.then p (fn [acc]
                       (.then (bench-samples device g mode iterations)
                              (fn [s] (assoc acc mode (ms-stats s)))))))
          (js/Promise.resolve {})
          modes))

(defn bench!
  "Serialized GPU timing for one (cubes,tex). Disable driving first so the land
   is idle. Returns a Promise<js-obj> of isolated per-pass GPU ms."
  [cubes tex iterations]
  (let [{:keys [device tracker]} @!runtime]
    (swap! !cfg assoc :cubes cubes :tex tex :auto-frame true)
    (let [g (ensure-gpu! device tracker)]
      (update-uniform! device (:u-buf g))
      (update-rect! device (:rect-buf g) 1600 1000)
      (-> (bench-samples device g :both 20) ;; warmup (discarded)
          (.then (fn [_] (bench-modes device g [:empty :composite :3d :both] iterations)))
          (.then (fn [m]
                   (clj->js
                     {:cubes cubes :tex tex :iterations iterations
                      :empty_ms (:empty m)
                      :composite_ms (:composite m)
                      :render3d_ms (:3d m)
                      :both_ms (:both m)
                      :render3d_isolated_median_ms (max 0.0 (- (:median (:3d m)) (:median (:empty m))))
                      :composite_isolated_median_ms (max 0.0 (- (:median (:composite m)) (:median (:empty m))))})))))))

;; ─────────────────────────────────────────────────────────────────────────
;; P4: mouse orbit + wheel dolly (only over the quad, only when enabled)
;; ─────────────────────────────────────────────────────────────────────────

(defonce !drag (atom nil))

(defn- over-quad? [^js canvas ev]
  (let [rect (.getBoundingClientRect canvas)
        w (.-width rect) h (.-height rect)
        px (min w h)
        edge (* 0.42 px)
        margin (* 0.03 px)
        x (- (.-clientX ev) (.-left rect))
        y (- (.-clientY ev) (.-top rect))
        qx0 (- w margin edge) qx1 (- w margin)
        qy0 margin qy1 (+ margin edge)]
    (and (>= x qx0) (<= x qx1) (>= y qy0) (<= y qy1))))

(defn install-input! [^js canvas]
  ;; capture-phase so we can pre-empt the land's drag-select when over the quad
  (.addEventListener canvas "mousedown"
    (fn [ev] (when (and @!enabled (over-quad? canvas ev))
               (reset! !drag {:x (.-clientX ev) :y (.-clientY ev)})
               (swap! !cfg assoc :autorotate false)
               (.stopPropagation ev) (.preventDefault ev)))
    true)
  (.addEventListener js/window "mousemove"
    (fn [ev] (when-let [d @!drag]
               (let [dx (- (.-clientX ev) (:x d)) dy (- (.-clientY ev) (:y d))]
                 (swap! !cfg (fn [c] (-> c (update :yaw + (* dx 0.01))
                                         (update :pitch #(-> (+ % (* dy -0.01))
                                                             (max -1.4) (min 1.4))))))
                 (reset! !drag {:x (.-clientX ev) :y (.-clientY ev)})
                 (.preventDefault ev))))
    true)
  (.addEventListener js/window "mouseup" (fn [_] (reset! !drag nil)) true)
  ;; wheel on window (capture) — the app routes wheel through a non-canvas element,
  ;; so a canvas-only listener never sees it; filter by over-quad? instead.
  (.addEventListener js/window "wheel"
    (fn [ev] (when (and @!enabled (over-quad? canvas ev))
               (swap! !cfg (fn [c] (-> c (assoc :auto-frame false)
                                       (update :dist #(-> (+ % (* (.-deltaY ev) 0.01)) (max 2.0) (min 60.0))))))
               (.stopPropagation ev) (.preventDefault ev)))
    #js {:capture true :passive false}))

;; ─────────────────────────────────────────────────────────────────────────
;; Control surface — window.__island
;; ─────────────────────────────────────────────────────────────────────────

(defonce !installed (atom false))

(defn install-window-api! [^js canvas]
  (when-not @!installed
    (reset! !installed true)
    (install-input! canvas)
    (set! (.-__island js/window)
          (clj->js
            {:enable (fn [] (reset! !enabled true) (reset-stats!) (reset! !last-raf-ts nil) "island: ON")
             :disable (fn [] (reset! !enabled false) "island: OFF")
             :set (fn [o]
                    (let [m (js->clj o :keywordize-keys true)]
                      (swap! !cfg merge m)
                      (clj->js @!cfg)))
             :cfg (fn [] (clj->js @!cfg))
             :stats (fn [] (stats-summary))
             :resetStats (fn [] (reset-stats!) (reset! !last-raf-ts nil) "stats reset")
             :snapshotDone (fn [] (swap! !swd assoc :pending true :samples []) "swd probe armed")
             :swd (fn [] (clj->js (:samples @!swd)))
             :bench (fn [cubes tex iters] (bench! cubes tex iters))
             :loadAtlas (fn [] (load-atlas!))}))
    (js/console.log "[ISLAND] probe control surface installed at window.__island")))
