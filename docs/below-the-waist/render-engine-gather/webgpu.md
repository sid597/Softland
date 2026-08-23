# WebGPU core — gathered digest
BASE = /mnt/data/projects/Softland/src/app/client/substrate/webgpu/
(anchors written BASE/renderer.cljs:167 etc. Sizes: renderer 4011 lines, compositor_gpu 1488, buffer_pool 493, gpu_budget 358.)

## INDEX
- A. Six shader families in renderer.cljs (rect, shadow, MSDF text, Slug text, image, image-mip) + a clear-quad; every one draws the SAME unit quad from `vertex_index`, instanced, no vertex/index geometry buffers.
- B. Two text roads coexist: MSDF (pre-baked bitmap atlas, uv per glyph) and Slug (Bezier outlines read from two data textures, no atlas). Shaper positions are shared; only coverage differs. No runtime atlas growth or eviction exists.
- C. Camera = one 6-float uniform; container transforms = one read-only storage array of 32-byte affine rows, indexed per-instance by a compact `container_idx`. Composition happens in the vertex shader.
- D. draw-frame! = prepare all uploads → derive tape entries by changed family → maintain a sorted arrangement → project scissors → ONE road of two: linear multipass compositor, or legacy single pass. Retained across frames: 5 module-level atoms + the compositor per device.
- E. Interleaved in tape order, not one pass per family. Each entry names its family; the executor looks up `frame-family-registry` and calls that family's `:execute!`. A family supplies exactly `{:contract :inputs :produce :execute!}`.
- F. Compositor: scene target → nested group targets → optional mask, layer blur, backdrop blur → composite → ONE present pass that encodes linear→sRGB. All intermediates are rgba16float.
- G. buffer_pool = CPU-side slot allocator over one growing GPU buffer (keyed / ordered / batch diff strategies). gpu_budget = a bookkeeping WeakMap of every buffer/texture, reserved vs active bytes, warns at 80% of adapter limits. The compositor's own target pool is the thing that actually REFUSES.
- H. Picking is CPU-side: entries carry `:pick {:geometry :rect-tree-bounds :owner vi}` and `scene-tape/pick-reverse` walks the tape backwards. No id buffer, no readback.
- I. Only ONE file outside this directory touches the WebGPU substrate. `draw-frame!` and `draw-comparison-frame!` have ZERO callers anywhere in src/ or test/ — including verifier.cljs.
- J. Surprises: one storage buffer of affine rows composed in-shader; Slug glyphs solved analytically per pixel from a curve texture; the compositor renders masked sub-draws to their own throwaway textures.
- K. Yes — at least four visible generations: Phase 5/6x editor pools, W0-A/W2-A geometry, W2-B tape families, W4 frame-plan + compositor. The legacy branch still lives inside draw-frame!.

---

## A. SYSTEMS
FACT. Six instanced "systems" plus two helpers. Each is a map holding {pipeline, bind-group, instance-buffer, capacity, num-instances, family/id, !paint-state}. Every pipeline uses `primitive triangle-list` and `.draw(pass, 6, instance-count)` — the quad's 6 corners come from a `switch(v_index)` in the vertex shader, so there is no vertex buffer at all; the only vertex buffer slot carries per-INSTANCE data (`stepMode "instance"`).
- rect (BASE/renderer.cljs:167,236,847) — rounded/bordered/gradient boxes. Stride 116 B = 28 floats + 1 u32 across 8 attributes: rect_geometry(0,16B) color(16) corner_radii(32) border_widths(48) border_color(64) gradient(80) gradient_color2(96) container_idx(112, uint32). Fragment: Inigo-Quilez per-corner rounded-box SDF, `aa = clamp(0.5 - dist, 0, 1)`, optional inner SDF for the border ring, optional linear gradient.
- shadow (BASE/renderer.cljs:342,399,1854) — stride 84 B = 20 floats + u32: expanded_rect, shadow_color, corner_radii, blur_params(blur, offset.xy, spread), inner_rect. Fragment: same rounded-box SDF, alpha from a Gaussian CDF via an Abramowitz-&-Stegun `erf_approx`.
- MSDF text (BASE/renderer.cljs:456,497) — stride 52 B = 13 words: rect(vec4) uv_bounds(vec4) color(vec4) container_idx(u32). Fragment: median of the 3 MSDF channels, screen-px-range scaling from the instance's on-screen size.
- Slug text (BASE/renderer.cljs:517,588) — stride 100 B = 25 words: rect, sample_bounds, inv_jac, banding, glyph(vec4<u32>, flat), color, container_idx. Fragment: per-pixel analytic coverage from Bezier curves (see B).
- image (BASE/renderer.cljs:67,119) — stride from image_material; instance = rect, uv_bounds, tint, container_idx. Fragment: sample + tint + a half-pixel edge coverage ramp (`cg`).
- image-mip (BASE/renderer.cljs:146,158) — a full-screen-triangle blit used to build mip chains after an upload.
- clear-quad (BASE/renderer.cljs:1978) — fullscreen triangle from vertex_index, constant color, used for dirty-rect partial clears.
- shared prelude `scene-color-wgsl` (BASE/renderer.cljs:25) is string-concatenated into every fragment shader; a `const kSceneColorLinearPremultiplied` is textually rewritten per pipeline to switch the whole engine between straight-sRGB and linear-premultiplied output.
DRAW CALLS. One `.draw` per tape entry (BASE/renderer.cljs:3057-3077), plus one per scissor/clip run when an entry carries `:sub-draws`, plus one per contiguous bind-group run for images (BASE/renderer.cljs:3102-3113). So the count is "number of tape entries", not "number of systems".
EXTRACTION (the shared quad trick, rect vertex, BASE/renderer.cljs:200-204):
```
switch(v_index) {
  case 0u: { pos = vec2<f32>(0.0, 0.0); } case 1u: { pos = vec2<f32>(1.0, 0.0); }
  case 2u: { pos = vec2<f32>(0.0, 1.0); } case 3u: { pos = vec2<f32>(1.0, 0.0); }
  case 4u: { pos = vec2<f32>(1.0, 1.0); } default: { pos = vec2<f32>(0.0, 1.0); }
}
```
UNCERTAINTY. Read directly. I did not open path_gpu/connector_gpu/chrome_gpu/region3d_gpu (out of scope) — those four families own their own shaders and executors elsewhere.
DECISION SERVED: explaining the WebGPU core.

## B. TEXT
FACT. Glyph placement is done once, upstream, and shared: `paint-msdf-line` and `paint-slug-line` both consume the same `positioned` glyph list from the shaper and only differ in what coverage metadata they attach. MSDF: the font ships a pre-baked bitmap; `create-msdf-font-resources` uploads it once with `copyExternalImageToTexture` into an rgba8unorm texture; each glyph's `atlasBounds` are divided by atlas width/height into uv_bounds, its `planeBounds` scaled by font-size into the quad rect. Slug: NO atlas — `create-slug-font-resources` uploads two data textures, `curve` (rgba16float, the Bezier control points) and `band` (rg16uint, per-glyph band index), and the fragment shader solves horizontal/vertical polynomial root codes per pixel to compute exact coverage. Growth/eviction: none found. The MSDF texture is replaced wholesale on a font swap (`update-font-assets`, `recreate-text-system`, BASE/renderer.cljs:1732,1810); there is no per-glyph residency, LRU, or atlas repacking anywhere in the file.
SOURCE. BASE/renderer.cljs:1483-1507 (msdf upload), :1529-1554 (slug curve+band), :2266-2299 (msdf placement→uv), :2302-2344 (slug placement→sample/banding/glyphLoc), :2389-2416 (13-word packer), :2418-2460 (25-word packer), :497-515 (msdf fragment), :588-728 (slug fragment).
EXTRACTION (rationale that placement is authoritative and coverage is secondary, BASE/renderer.cljs:2277-2279):
```
;; Placement/advance came from Contract T. MSDF selects coverage
;; metadata only; a missing glyph never changes placement.
```
EXTRACTION (the Slug root-code table — the classic Loop-Blinn/Slug constant, BASE/renderer.cljs:601-607 region):
```
fn calc_root_code(y1: f32, y2: f32, y3: f32) -> u32 { ... return (0x2E74u >> shift) & 0x0101u; }
```
EXTRACTION (a real perf receipt on the CPU side of text, BASE/renderer.cljs:2141-2147):
```
;; first-light P1 (G1 drill finding): glyph-map is rebuilt PER LINE by
;; shape-msdf-line/shape-slug-line — a whole-conversation reshape rebuilt the
;; full unicode→glyph map hundreds of times per keystroke (~23ms/keystroke,
;; CPU-profiled). ... cache per vector identity (WeakMap ...)
```
UNCERTAINTY. "No eviction" is a negative result from grepping `atlas` across renderer.cljs (all atlas hits after line 1080 are the IMAGE atlas, not glyphs) — inferred, not proven by reading a design doc.

## C. TRANSFORMS & CAMERA
FACT. Two bindings, shared by every transform-consuming pipeline. (1) Camera: a 6-float uniform `{pan: vec2, zoom: f32, padding, screen_dimensions: vec2}` rewritten every frame by `update-camera` — one `writeBuffer` of 24 bytes. (2) Containers: ONE read-only storage buffer of 32-byte affine rows, `[axis_x.xy, axis_y.xy, translation.xy, flags:u32, pad:u32]`, capacity 16384 rows (512 KB), allocated once. Each instance carries a `container_idx: u32`; the vertex shader reads `containers[container_idx]`, applies the affine, then applies camera pan/zoom — unless `flags & 1` marks the container "screen space", in which case zoom is forced to 1 and pan to 0. Slot 0 is written at creation as identity. Semantic container ids are NOT the index: `write-containers!` writes by a compact `:transport-slot`, so sparse ids never leave holes.
SOURCE. BASE/renderer.cljs:768-776 (declaration), :786-798 (create), :800-846 (write), :1449 (camera buffer), :2628-2635 (update-camera), :169-176 + :206-221 (WGSL).
EXTRACTION (composition in-shader, rect vertex, BASE/renderer.cljs:206-221):
```
let c = containers[instance.container_idx];
let is_screen = (c.flags & 1u) != 0u;
let zm = select(camera.zoom, 1.0, is_screen);
let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
let world_pos = c.translation + c.axis_x * local_pos.x + c.axis_y * local_pos.y;
let panned = world_pos * zm + pn;
let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0);
```
EXTRACTION (why 16384, BASE/renderer.cljs:772-774):
```
;; Q8 priced 1,024 / 4,096 / 16,384 entries, and the largest measured tier is
;; the production allocation. Growing later rebinds the same storage scheme; it
;; is not another representation migration after atom multiplication.
```
UNCERTAINTY. Read directly. Where `:transport-slot` is assigned (containers/effective) is outside these four files.

## D. THE FRAME
FACT (walk of BASE/renderer.cljs:3678-4011).
1. :3678-3703 — a 9-positional + ~30-keyword signature: device, context, text-sys, editor pools, camera floats, pan/zoom/w/h, plus every family system (image/path/connector/chrome/region3d), `store-frame` (the scene data), `effective-transforms`, container deltas, font assets, capabilities.
2. :3706-3707 — begin the per-frame ledger; `reset-frame-retention!` wipes all retained atoms if the GPU device changed identity.
3. :3717-3762 — ALL uploads first: `prepare-image-frame!`, `prepare-path-frame!`, `prepare-chrome-frame!`, `prepare-region3d-frame!`, then `prepare-connector-frame!`. Comment at :3717-3718: "W4: every upload/prepare happens before the first pass opens. No queue write is relied on while a pass encoder is live."
4. :3785-3792 — build `inputs` = `frame-input-map`; diff against `!prev-frame-inputs` to get `changed-families`; `produce-frame-entries` runs ONLY the changed families' `:produce` fns.
5. :3800-3812 — `update-frame-arrangement` upserts/removes into a sorted map keyed by tape order; unchanged families are never revisited.
6. :3813-3860 — semantic-state deltas (containers, region topology, globals) folded into a retained `!frame-semantic-state`; ledger counters written.
7. :3866-3882 — derive `effect-spans` (which containers form groups with opacity/blur/mask) and `plan` (the pass list + color mode); a plan change publishes `__softlandFramePlanReceipt` to globalThis.
8. :3885-3894 — `update-camera` (one writeBuffer, per frame, unconditionally).
9. :3899-3906 — project every entry's clip into screen-space scissors (`project-entry-scissors`).
10. :3908-3915 — `frame-tape-twin-check!` (a compare-only shadow implementation), then the road split on `(:color-mode plan)`.
11a. LINEAR road :3921-3972 — get/create the per-device compositor, build the "variant layer" (linearized pipelines) lazily, call `compositor-gpu/draw-multipass!`, publish receipts to globalThis.
11b. LEGACY road :3973-4008 — one `createCommandEncoder`, one `beginRenderPass` onto the swap-chain view (loadOp "load" when a dirty rect is present), `execute-scene-tape!` walks entries forward, `.end`, optional `copy-present!`, `.submit`.
RETAINED between frames (module-level, BASE/renderer.cljs:3392-3397):
```
(defonce ^:private !frame-semantic-state (atom nil))   ; effects + plan + container/region state
(defonce ^:private !prev-frame-inputs (atom nil))      ; last frame's input map, for family diffing
(defonce ^:private !frame-device (atom nil))           ; device identity guard
(defonce ^:private !prev-attachment-size (atom nil))   ; viewport binding delta
(defonce ^:private !compositors-by-device (js/WeakMap.))
```
The arrangement itself lives inside `!frame-semantic-state` as `{:ordered <sorted-map keyed by tape-order comparator> :keys-by-family {family-id #{keys}}}` (BASE/renderer.cljs:3383-3390). Also retained: every system's GPU buffers/textures/pipelines (created in `create-editor-state`, BASE/renderer.cljs:2077), the containers storage buffer, the camera buffer, the compositor's target pool. Rebuilt per frame: the entries of CHANGED families only, the scissor projection vector, the uniform writes, and every compositor intermediate texture (leased and returned).
EXTRACTION (a measured reason the arrangement is not rebuilt, BASE/renderer.cljs:3896-3899):
```
;; arrangement-raw already iterates in tape order (sorted map); every
;; consumer flattens to entries, so project straight into a vector —
;; rebuilding a second sorted map re-ran the Contract-O comparator
;; ~n·log n times per frame for nothing (perf receipt 2026-08-08).
```
UNCERTAINTY. Read directly.

## E. THE TAPE
FACT. The tape is ten families (not nine): rect, shadow, msdf, slug, clip, image, path, connector, chrome, region-3d (`scene-tape/family-ids`, /mnt/data/projects/Softland/src/app/client/substrate/scene_tape.cljc:14-25). Execution is INTERLEAVED in tape order with a pipeline switch per entry — `scene-tape/paint-forward` maps the executor over `(:entries tape)` in order, and `execute-frame-entry!` looks the family up in `frame-family-registry` and calls its `:execute!`. There is no per-family pass and no family branch in the executor. A family supplies exactly four keys: `:contract` (from scene_tape's declarative registry), `:inputs` (from frame_inputs' input declarations), `:produce` (inputs → entries), `:execute!` (pass, entry → entry-id). Six families use the generic `execute-gpu-batch!`; image, path, connector, chrome, region-3d bring their own walker. A startup assertion throws unless registered executors EXACTLY cover admitted families and each has a non-nil contract.
SOURCE. BASE/renderer.cljs:3297-3360 (registry), :3363-3379 (the coverage assertion), :3647-3665 (executor + paint-forward), :3057-3077 (generic batch), scene_tape.cljc:14-25, :480-495, :750-767.
EXTRACTION (a plug-in family's whole registration, BASE/renderer.cljs:3335-3341):
```
:render.family/path
(family :render.family/path
        (store-producer path-gpu/path-entries
                        [:paths :ordered-vis :ops-count-by-vi
                         :order-by-vi])
        path-gpu/execute-path-batch!)
```
EXTRACTION (the fence, BASE/renderer.cljs:3366-3370):
```
(when-not (= admitted-families executor-families)
  (throw (ex-info "Frame executor registrations must exactly cover admitted families"
                  {:admitted admitted-families :executors executor-families})))
```
draw-comparison-frame! (BASE/renderer.cljs:2637-2679): a text-backend A/B harness. It splits the canvas with `setViewport`, gives each half `screen_dimensions = (w/2, h)`, and draws the primary text system on the left and a second text system on the right in ONE render pass. Its docstring: "Rects, shadows, chrome are skipped — this is a pure text rendering comparison." It exists to eyeball MSDF vs Slug side by side. Zero callers (see I).
UNCERTAINTY. Read directly. The prompt said nine families; the code says ten.

## F. COMPOSITOR
FACT. `compositor_gpu.cljs` owns the multi-target road. Five WGSL strings: a full-screen-triangle vertex shader; `composite` (group texture × opacity, optional mask alpha, optional backdrop mix of original vs blurred); `blur` (a separable 5-tap Gaussian, run twice — H then V); `present` (un-premultiply, linear→sRGB encode, re-premultiply); `clip-mask` (rasterize an arbitrary quad by four edge cross-products into a coverage mask). ORDER inside `draw-multipass!` (:1303-1345): (1) region pass producers encode first — Region3D interiors render into their own leased targets; (2) `encode-linear-scene!` acquires a viewport-sized rgba16float "scene" target and walks the arrangement; each effect group recurses into its own "group-content" target, optionally layer-blurs it, optionally renders a mask, composites into a "group-output" target, then composites that into the parent (with a backdrop snapshot + blur if declared); (3) `draw-present!` does exactly one full-screen pass onto the swap-chain view; (4) one `.submit`; (5) `release-after-submit!` returns leases and destroys transient uniform buffers on `onSubmittedWorkDone`. "Effects" = opacity, mask (arbitrary quad clip via a coverage texture), layer-blur (blur the group itself), backdrop-blur (blur what is behind the group). All intermediates are rgba16float; the swap chain is the only 8-bit surface.
SOURCE. BASE/compositor_gpu.cljs:28-116 (shaders), :1088-1155 (encode-linear-scene!), :1006-1046 (render-actions!), :967-987 (masked sub-draw), :1158-1180 (present), :1303-1345 (draw-multipass!).
EXTRACTION (why multipass costs what it costs — the budget rationale, BASE/compositor_gpu.cljs:20-26):
```
;; The W4 backdrop road at zoom peaks at six live viewport-sized rgba16
;; targets — scene, group content, group output, snapshot, and the separable
;; blur pair — plus the downsample chain and any mask target. Keep that road
;; inside the owned budget at a 3840x2160 physical canvas (~66MB per target);
;; the pool still refuses larger live sets by name, reclaims stale free cache
;; under pressure, and the blur road degrades on refusal instead of dying.
```
EXTRACTION (ns docstring, BASE/compositor_gpu.cljs:2-8): "The frame graph names resources and producer edges; this namespace owns the recycling target pool, linear group/mask/blur composition, exactly-one presentation transfer, per-draw scissor state, and asynchronous raster readback."
UNCERTAINTY. Read directly. `export-viewport!` (:1415) does a readback + PNG encode; I read only its neighbours (`strip-padded-rows`, `unpremultiply!`, `png-bytes!`) by name.

## G. POOLS & BUDGET
FACT — buffer_pool.cljs. One growing GPU vertex buffer plus a CPU-side slot allocator: `{buffer capacity free-list active-slots high-water-mark generations pack-fn bytes-per-item}`. Growth doubles capacity and `copyBufferToBuffer`s the live prefix (:146-164). Three update strategies over the same pool: KEYED (`keyed-diff-update-pool!`, :214) syncs by `:id` — new id allocates a slot, changed id rewrites it, removed id frees and zeroes it; slot order is arbitrary, so z-order is NOT preserved. ORDERED (`ordered-diff-update-pool!`, :275) forces slot i = input i so draw order is z-correct, and uses ids only to SKIP writes where identity and content both held. BATCH (`batch-update-pool!`, :328) is positional-only: compare index-wise against `prev-rects`, write the differences, zero the tail when the list shrinks. `free-slot!` zeroes the slot's floats explicitly — the docstring says "to prevent ghost rendering", i.e. a stale slot would otherwise still be drawn. `pool-draw-info` (:364) hands out the pool ATOM, not the buffer, so a retained tape entry resolves a grown buffer at encode time.
FACT — gpu_budget.cljs. Pure bookkeeping, no allocation authority. A WeakMap from GPU object → `{kind label reserved-bytes active-bytes details}` plus a ring of the last 200 events. RESERVED = what was allocated (textures price every mip level and MSAA sample, :152-169); ACTIVE = how much is actually in use (`set-active-bytes!` after each upload). It compares reserved bytes against `maxBufferSize` and texture dims against `maxTextureDimension2D`/`maxTextureArrayLayers` from the adapter and `console.warn`s at 80%. It never refuses. The thing that refuses is the COMPOSITOR's target pool: `ensure-target-capacity!` throws `:frame-target-budget-exceeded` past a 512 MB cap, after first trying `reclaim-free-targets!` / `reclaim-stale-free-targets!`; the blur road catches the refusal and degrades (`try-acquire-target!`, :809; the "no backdrop declared, or its snapshot refused under budget pressure" branch at :1041).
SOURCE. BASE/buffer_pool.cljs:111-208, :214-234, :275-300, :328-377; BASE/gpu_budget.cljs:31-45, :73-110, :145-193, :268-278; BASE/compositor_gpu.cljs:193-204, :239-284, :1038-1043.
EXTRACTION (BASE/buffer_pool.cljs:218-222):
```
This is the Missionary-side equivalent of what e/for-by would do:
- new ID → allocate-slot! + update-slot!
- same ID, changed rect → update-slot!
- removed ID → free-slot!
```
UNCERTAINTY. Read directly.

## H. PICKING
FACT. CPU-side, no GPU readback. Tape entries carry a `:pick` value that is either `:none` or `{:geometry :rect-tree-bounds :owner vi}` — i.e. a pointer back to the CPU rect tree, not a GPU id. `scene-tape/pick-reverse` walks `(rseq (:entries tape))` and returns the first visible, pickable entry whose `hit` fn resolves. Nothing in these four files allocates a pick/id attachment or calls `mapAsync` for hit-testing (the only readback is `export-viewport!`, for PNG export). renderer.cljs's own `hit-test` (:750) is a text caret/column helper delegating to `text-layout/hit-test-result`, unrelated to scene picking.
SOURCE. BASE/renderer.cljs:2693-2706 (`frame-entry` :pick), :2918-2922 (`:rect-tree-bounds`), :749-760 (text caret hit-test); scene_tape.cljc:757-767 (`pick-reverse`).
EXTRACTION (verifier.cljs states the product truth, /mnt/data/projects/Softland/src/app/client/substrate/webgpu/verifier.cljs:9-11):
```
The CPU point-in-path probe is NOT today's product picking path. Product
picking remains axis-aligned rect-tree bounds; the receipt carries an
explicit rounded-corner divergence sentinel so those truths cannot collapse.
```
UNCERTAINTY. Read directly.

## I. DEAD vs LIVE
FACT. Grep across src/ and test/ (excluding target/, node_modules/) for `<ns-alias>/<name>` per public name. THE BIG ONE: **`draw-frame!` has zero callers anywhere in src/ or test/** — `grep -rn "draw-frame"` outside renderer.cljs returns nothing. Same for `draw-comparison-frame!`. Neither is called by verifier.cljs. More broadly, only ONE file outside BASE requires anything from `app.client.substrate.webgpu.*`: `/mnt/data/projects/Softland/src/app/client/workspace/region3d_runtime.cljs:12` requires `region3d-gpu`. The renderer / compositor / buffer-pool namespaces are required ONLY by `verifier.cljs`, which uses ~38 renderer names (heaviest: create-containers-buffer, create-camera-buffer, update-camera, update-text-data, write-containers!, init-image-system, execute-frame-entry!, init-rect-system, init-text-system) but never draw-frame!.
Public names with zero external callers, by file:
- renderer.cljs (25): image-vertex-shader, image-fragment-shader, shadow-vertex-shader, shadow-fragment-shader, calculate-bracket-rects, rect-stride, image-instance-stride, msdf-text-instance-stride, slug-text-instance-stride, max-transform-nodes, update-font-assets, share-font-resources, recreate-text-system, shadow-stride, init-shadow-system, clear-quad-shader, init-clear-quad, create-render-target, destroy-render-target!, create-editor-state, **draw-comparison-frame!**, frame-family-registry, replace-frame-compositor!, project-clip-rect, **draw-frame!**.
- compositor_gpu.cljs (9): target-pool-version, compositor-version, default-pool-budget-bytes, the five shader strings, release-all-region-leases!.
- buffer_pool.cljs (13): floats-per-rect, bytes-per-rect, pack-shadow, allocate-slot!, free-slot!, update-slot!, keyed-diff-update-pool!, ordered-diff-update-pool!, pool-draw-info, allocate-handle!, update-handle!, free-handle!, gpu-mount. (i.e. the ENTIRE public API of buffer_pool has no external caller.)
- gpu_budget.cljs (4): format-bytes, texture-reserved-bytes, summary-line, log-startup-report!.
SOURCE. Scan script output saved at /tmp/claude-1000/-mnt-data-projects-Softland/6cfba0cb-8608-4e70-850d-9fd910894549/scratchpad/gather-webgpu/dead.txt; verifier requires at /mnt/data/projects/Softland/src/app/client/substrate/webgpu/verifier.cljs:12-42.
UNCERTAINTY. The scan matches the CLJS `alias/name` call form and would MISS a `:refer`-style import or a name reached only through a string/JS interop path. I spot-checked `draw-frame`, `draw-comparison-frame`, `pool-draw-info`, `gpu-mount`, `keyed-diff-update-pool`, `ordered-diff-update-pool` with a bare-substring grep across src+test — still zero. Note `create-editor-state` is a live INTERNAL entry point (it wires the whole engine) but nothing outside the file calls it either.

## J. THREE MOST SURPRISING DESIGN CHOICES
1. Every container transform is ONE row in ONE storage array, and composition happens in the vertex shader — no matrix stack, no CPU pre-transform, no per-object uniform. Semantic ids are remapped to dense slots so sparsity costs nothing. BASE/renderer.cljs:768-776:
```
;; --- W2-A/Q8: shared compact affine transport ------------------------------
;; One 32-byte storage entry per LIVE transform slot:
;; [axis-x.xy, axis-y.xy, translation.xy, flags:u32, pad:u32]. Semantic cids do
;; not index this table; containers/effective assigns compact stable slots.
```
2. Text has TWO complete backends running off one shaper, and one of them puts no glyph pixels on the GPU at all — Slug ships Bezier control points in an rgba16float "curve" texture plus an rg16uint "band" index texture and solves coverage analytically per fragment. BASE/renderer.cljs:1529-1543 and the fragment's declarations at :592-594:
```
@group(0) @binding(0) var curveTexture: texture_2d<f32>;
@group(0) @binding(1) var bandTexture: texture_2d<u32>;
```
3. A masked ("clip to an arbitrary quad") sub-draw is not scissored — it gets its OWN pair of viewport-sized textures, renders unclipped into one, rasterizes the clip quad into the other as coverage, and composites. Per sub-draw. BASE/compositor_gpu.cljs:967-987 + the ordering rationale at :994-996:
```
;; Preserve per-operation order whenever one sub-draw takes the general
;; mask road; neighboring scissor and unclipped operations stay distinct.
```
Runner-up worth naming: the whole engine's color space is a *textual* shader constant (`kSceneColorLinearPremultiplied`) rewritten per pipeline at build time (BASE/renderer.cljs:22-23, 42-47), so "legacy sRGB" and "linear premultiplied" are the same source compiled twice.
UNCERTAINTY. "Most surprising" is my judgement; the code and comments quoted are verbatim.

## K. GENERATIONS
FACT. At least four strata coexist in renderer.cljs, and the oldest is still executable.
- **Phase 5 / 6A / 6D / 6E (editor era)** — `buffer_pool.cljs`'s keyed/ordered/batch APIs (banners at :210, :271, :324, :379), `clear-quad`/dirty-present (BASE/renderer.cljs:1977), `create-render-target` "survives swap chain double-buffering" (:2019), `create-editor-state` (:2077), `init-shadow-system`/`update-shadows` (:1854-1975). This is a direct-draw editor renderer: pools of rects and shadows, a persistent render target, partial clears.
- **W0-A / W2-A / Q5 / Q8 (geometry + transport era)** — the shared affine storage (:768), the half-pixel conservative raster hull (:214-216 "Q5"), the SDF coverage law explicitly frozen ("Preserve the settled rich-rect raster law exactly. W2-A changes the transport and geometry, not the fragment coverage convention.", :286-287), `scene-substrate P2`'s container_idx appended to every stride (:763-766).
- **W2-B / SEAM-STEP1 (tape era)** — `scene-tape/family-ids` "The complete pre-W2-B family set… it never adds a branch to the frame executor"; `frame-family-registry`, `frame-entry`, `paint-forward`, the arrangement sorted-map and family-scoped deltas (:3381-3390 "SEAM-STEP1 T8", :3506-3538 "SEAM-STEP1 T12").
- **W4 (frame plan + compositor era)** — `frame-plan-view`/`frame-effect-view`/`frame-semantic-state`, the linear color road, `compositor_gpu` itself (ns docstring "W4's generic WebGPU compositor capability"), the upload-before-any-pass law (:3717-3718).
- **IMAGE-ATOM (T1-T10)** — the newest layer inside renderer.cljs: digest-addressed image ingress, two-tier atlas/dedicated residency, sRGB view vs unorm view (:54-57), family-owned sub-draw walker (:3102-3105 "the central tape executor remains family-blind (T1)").
COEXISTENCE IS LIVE, not vestigial: `draw-frame!` branches at :3920 on `linear?` — the W4 compositor road, or the legacy road that opens one render pass straight onto the swap chain and walks the same tape. Both are reachable from the same call.
EXTRACTION (the legacy fallback constant is still wired, BASE/renderer.cljs:3976-3978):
```
scene-color (or (:scene-color render-target)
                (:scene-color text-sys)
                scene-tape/legacy-direct-color)
```
UNCERTAINTY. The generation NAMES (W2-A, W4, SEAM-STEP1, IMAGE-ATOM, Phase 6E) are read verbatim from comments; their chronological order is inferred from git log on renderer.cljs (`143497a feat(render): add W4 frame runtime` precedes `8b1ab98 render-engine: land Atom A`, `43ae9d9`, `79c8839`, `ec8c430`, `ea63447`) — I did not read the contracts that define them.
