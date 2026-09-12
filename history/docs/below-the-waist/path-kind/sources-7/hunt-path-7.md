# Hunt: path kind + input constructor
Root: /mnt/data/projects/Softland/src/app/client/. Files: path/{README.md,component.cljc (324 l), tessellation.cljc (607), frame.cljc (22), renderer.cljs (264)}, harness/path.cljs (562). Tags: CHECKED = lines read; DERIVED = inferred (from what); ASSUMED = not verified.

README (path/README.md) CHECKED: "The component remains the geometric input for picking; tessellation derives the render representation." "Components and transforms belong to callers. Tessellation returns derived values; a renderer system retains its cache and buffer."

## Five-question cards

### path/component.cljc
TAKE (validate-component! L105-112, classify L254-292, paint-color L317-324): a component map, keys L95-96: `:path/material-id` (some?), `:path/revision` (some?), `:path/kind` in `#{:ink :shape}` L15, `:path/geometry`, `:path/paint`. classify also takes `point` ([x y]) and optional `slop-local` (non-negative, L263-267).
GIVE: validate → same map or throw `{:error-type ...}` L27; classify → `:inside|:boundary|:outside`; hit? → boolean L294-297; boundary-distance → number L299-315; component-content-hash → `[:path/content-v2 (pr-str canonical-minus-id/revision)]` L137-139 (paint included, L134); paint-color → `[r g b (* a opacity)]` L322-324.
CALLERS (grep CHECKED): paint-color ← renderer.cljs:157, region3d/on_plane.cljc:117. component-content-hash ← harness/path.cljs:46, harness/region.cljs:184. validate-component! ← harness/path.cljs:44 only. classify, boundary-distance ← harness/path.cljs:354,357,375 only. contour-classify ← tessellation.cljc:353,354,559. hit? ← none in src/. canonical-component ← tessellation.cljc:52.
HOLD: nothing ("No retained state" L6). Constants: schema-version 2 L14, boundary-epsilon 1.0e-9 L18, legal-cap-join #{:round} L17.
MAKER: harness/path.cljs path-ink-component L74-92 / path-shape-component L94-115 / path-polygon-component L117-129 — literal vectors in code; no other constructor in src/app/client (grep for pointer/addEventListener: none).

### path/tessellation.cljc
TAKE: derive-mesh-set L590-607 `[cache components zoom]` — cache = map key→mesh (or nil), components = ordered vector of component maps, zoom = number in [0.01,1000] (zoom-lod throws otherwise L31-33). tessellate L569-588 `[component zoom]` / `[component algorithm zoom]`.
GIVE: derive-mesh-set → `{:cache :meshes :derived-keys}` L601-605. tessellate → `{:path.mesh/version 2 :algorithm-version :lod :cache-key :coverage :aliased-v1 :vertices [[x y] ...] :triangle-count :vertex-count}` L581-588. component-cache-key → `[sorted-map{:path/kind :path/geometry} algorithm lod-id]` L52-55.
CALLERS (CHECKED): derive-mesh-set ← renderer.cljs:209, on_plane.cljc:108, harness/region.cljs:220. component-cache-key ← harness/region.cljs:185. tessellate ← harness/path.cljs:285,343. zoom-lod ← renderer.cljs:202, harness/path.cljs:68,288,325. Public stroke-triangles/shape-triangles/bridge-holes/ear-clip ← only tessellate (no outside ns names them).
HOLD: nothing; "The caller owns cache lifetime" L5; "Cache grows with all encountered keys and has no eviction here" L594-595.
MAKER of input: same component makers; cache made by renderer `:!mesh-cache` atom (renderer L141) and on_plane caller.

### path/frame.cljc
TAKE: frame-key `[draw-items lod]` L15; draw-item read for `:path/material` (component) and `:container` L17-20.
GIVE: `[[[material-id revision container] ...] lod]` L16-22.
CALLERS: renderer.cljs:203 only. HOLD: none L4-5. MAKER of draw-item: harness path-draw-item L142-145 `{:id :path/material component :container group}`.

### path/renderer.cljs
TAKE: init-path-system `[device fformat camera-buffer groups-buffer & {:initial-capacity 2048 :scene-color legacy-direct-color}]` L81-84. prepare-path-frame! `[path-system draw-items zoom world-transforms]` L200; world-transforms is the map `transform/buffer-index` reads `[group-id :buffer-index]` from (transform.cljc:276-284, throws `:transform/unknown-group`). draw-path-range! `[pass path-system first-vertex vertex-count]` L247.
GIVE: init → `{:device :pipeline :bind-group :camera-buffer :groups-buffer :scene-color :!buffer :!capacity :!mesh-cache :!prepared :!last-frame-key}` L137-142. prepare → `{:changed? :writes :vertices :derived}` L205-208/L237-240. draw → encodes one `.draw vertex-count 1 first-vertex 0` L251, returns that. destroy → nil L264.
CALLERS (CHECKED): harness/path.cljs:176,185,316,436,446,514,561; harness/region.cljs:346,1146,1165,1170,1278,1279. None outside harness/ in src/.
HOLD (L140-142): `:!buffer` GPUBuffer, `:!capacity` vertex count, `:!mesh-cache` {cache-key→mesh}, `:!prepared` [{:draw-item :mesh :first-vertex :vertex-count}] L219-222, `:!last-frame-key` (init `::never`, destroy `::destroyed` L263). "It does not own the camera or group buffers" L7.
MAKER: harness run-path-step! L506-516 (device/create-camera-buffer, create-groups-buffer, transform registry with group 17).

### harness/path.cljs
TAKE: run-path-step! `[device]` L505. GIVE: promise of `{:cases :parity :color :upload-dirty-check :system :coverage :aliased-v1 :product-pick :cpu-path-authority :self-overlap-alpha :direct-triangle-double-blend-declared :pass?}` L554-560.
CALLERS: harness/core.cljs:91; harness/region.cljs:29 refers path-draw-item, path-ink-component, path-polygon-component (used L175,1151-1160). HOLD: none (creates and destroys its system L514,561).

## Q1 Component data shape (component.cljc, all CHECKED)
- `:path/paint` L30-39: `:color` 4-vector finite in [0,1] (schema.cljc:37-43); `:opacity` finite [0,1]; `:color-space` only `:srgb`; `:alpha-association` only `:straight`.
- `:path/kind :ink` → geometry L52-57: `:stroke-points` vector, min 2, unique by `:stroke-point/id`; `:cap` and `:join` each must be `:round` (L17). Stroke-point L41-50: required `:stroke-point/id` (some?), `:position` = 2-vector finite (schema.cljc:47-53), `:width` positive finite; optional `:pressure` finite, `:gesture-time` finite.
- `:path/kind :shape` → geometry L76-81: `:contours` vector min 1 unique by `:contour/id`; contour L59-64: `:contour/id`, `:role` in `#{:outer :hole}`, `:points` vector of points min 3. Form check holes-have-an-outer? L66-74: docstring "Presence check ... does not prove containment".
- Ink vs fill distinguished only by `:path/kind` + geometry dispatch L89-91. Points stored as literal `[x y]` vectors. No curve segment key of any kind — only samples/vertices; straight segments between consecutive samples (tessellation L212-229, component L240). Open/closed: not a flag; DERIVED from code — ink is walked as `(partition 2 1 stroke-points)` (open), contours are closed by `(concat (rest points) [(first points)])` L206. Fill rule: no key; see Q3. Width: per stroke-point `:width`, local units (DERIVED: normalized-ink divides width by the same scale as positions, tessellation L198; shader multiplies by camera.zoom, renderer L51). No per-component width; no width on contours.
- Docstring limit L108-110: "Structural acceptance does not establish simple polygons, contained holes or nonzero segments; tessellation can reject a structurally valid component."

## Q2 Ink stroke tessellation (tessellation.cljc, CHECKED)
1. Normalize: bbox origin, scale = max(1, w, h) L67-80; positions and widths divided by scale L187-199; triangles denormalized after L281-283.
2. Per consecutive pair L211-229: `direction = unit(b-a)` (throws "Path contains a zero-length line segment" if |v| ≤ 1e-10, L128-130); `normal = left-normal`; radii = width/2 at each end; corners `a±normal*ra`, `b±normal*rb`; two triangles → one tapered quad per segment.
3. Caps L234-245: semicircle fan at first point (radius = first width/2) and last point, `fan-triangles` L169-185: steps = max(1, ceil(resolution·|Δangle|/π)).
4. Joins L246-268: per interior point, `turn = cross(dir_in, dir_out)`; turn > 1e-10 → ccw fan from -normal_in to -normal_out; turn < -1e-10 → cw fan from normal_in to normal_out; else none. Radius = that point's width/2. Round only; no miter/bevel exists.
5. Concatenate segment quads + caps + joins L269-271. Degenerate triangles dropped by cross-area filter L141-147.
- Zoom bands L14-23: `:legal-min` [0.01,0.1) fan-resolution 4; `:engine-default` [0.1,8.0] fan-resolution 8; `:legal-max` (8,1000] fan-resolution 16. zoom-lod L34-37. Only fan-resolution changes per band (L209).
- Offset/expansion is on CPU (all of the above). Self-intersection/sharp turn: no handling; docstring L204-205 "overlapping stroke pieces remain separate triangles"; harness tags result `:self-overlap-alpha :direct-triangle-double-blend-declared` L558-559. No simplification or smoothing (grep smooth/simplif: none). Width in local units (Q1).

## Q3 Filled contour (CHECKED)
- shape-triangles L540-567: outers and holes split by role; a hole is owned by an outer if hole's first point is not `:outside` that outer (L556-561); per outer: `ear-clip (bridge-holes outer owned-holes)`; results concatenated across outers.
- bridge-holes L393-407: outer oriented CCW, holes CW, holes sorted by rightmost point. bridge-hole L364-391: h = rightmost hole vertex; v = nearest outer vertex passing bridge-visible? (L338-355: no proper crossing with any outer/hole edge, midpoint inside outer and outside all holes); splices `outer[0..v] ++ hole-walk ++ [h v] ++ rest`; throws "No visible deterministic bridge exists for path hole" L384.
- ear-clip L480-525: ear-index L441-470 = convex (orientation > 1e-10), no other vertex inside triangle (point-in-triangle? L409-420), diagonal-clear? L422-439 (no proper crossing); fallback removes an exactly collinear vertex L509-522; fuel = 4·n L489; throws "Ear clipping could not consume" / "found no legal ear" L497,523. Docstring L483-485: "Worst-case repeated scans can be O(n³)".
- Fill rule: none named. DERIVED (from ear-clip of a simple bridged polygon): fills the polygon interior with holes subtracted; not odd-even/nonzero over self-intersecting input. proper-segment-intersection? L318-329 docstring: "Intentionally excludes collinear/touching cases; not a general intersection predicate."
- Limits, docstring L543-545: "Zoom is unused by shape triangulation; overlapping outers and malformed hole relationships are not resolved here." Multiple outers each triangulated separately and concatenated (L553-564).
- CPU side (component.cljc L199-218): contour-classify is odd/even ray crossing per contour; classify L277-280 = inside iff some outer inside and no hole inside.

## Q4 Renderer (renderer.cljs, CHECKED)
- Two-level invalidation. Frame key (frame.cljc L16-22): `[[material-id revision container]...] + lod-id`; equal → `{:changed? false :writes 0 ...}` L204-208. Mesh cache key (tessellation L42-55): canonical `{:path/kind :path/geometry}` + `:path-tessellation-v1` + lod-id; paint/material-id/revision excluded, ids nested in geometry included. Docstring L196-199: "Transform coefficients are intentionally excluded, but a reassigned compact index with unchanged container ID is also excluded; callers must preserve that index or invalidate the frame."
- Vertex layout L17-18, L113-121: 7 words / 28 bytes: `position float32x2 @0`, `color float32x4 @8`, `group_buffer_index uint32 @24`. pack-vertices L144-169 replicates paint-color and group index per vertex; index from `transform/buffer-index world-transforms (:container draw-item)` L158.
- Vertex shader L24-57 (verbatim body):
```
let c = groups[input.group_buffer_index];
let is_screen = (c.flags & 1u) != 0u;
let zm = select(camera.zoom, 1.0, is_screen);
let pn = select(camera.pan, vec2<f32>(0.0, 0.0), is_screen);
let world = c.translation + c.axis_x * input.position.x + c.axis_y * input.position.y;
let panned = world * zm + pn;
let ndc = (panned / camera.screen_dimensions * 2.0) - vec2<f32>(1.0, 1.0);
output.position = vec4<f32>(ndc.x, -ndc.y, 0.0, 1.0); output.color = input.color;
```
Bindings L29,L34: `@group(0) @binding(0) var<uniform> camera: Camera {pan vec2, zoom f32, padding f32, screen_dimensions vec2}`; `@binding(1) var<storage, read> groups: array<GroupTransform {axis_x, axis_y, translation vec2; flags, padding u32}>`. Both buffers passed in at init L81, asserted L85-86; renderer "does not own the camera or group buffers" L7.
- Fragment L59-62 verbatim: `@fragment fn main(@location(0) color: vec4<f32>) -> @location(0) vec4<f32> { return scene_color(color, 1.0); }` — prefixed by `device/scene-color-wgsl` and configured via `device/configure-scene-color-shader` L93-95; blend from `device/scene-color-blend` L125.
- Anti-aliasing: none. Mesh `:coverage :aliased-v1` (tessellation L585); pipeline has no `:multisample` key (grep: none); topology triangle-list, cullMode none L126-128.
- prepare-path-frame! L192-240 takes system, draw-items, zoom, world-transforms; derives all meshes, repacks ALL prepared rows ("repacks all prepared rows" L148), grows buffer by doubling (destroys old, L187), one `writeBuffer` at offset 0 L233; gives `{:changed? :writes (0|1) :vertices :derived}`.
- draw-path-range! L242-251: one draw = caller-chosen contiguous vertex range; harness draws `0..vertices` (all paths, harness L185); "Caller owns ordering, clipping and pass lifetime" L245-246.

## Q5 CPU classification (CHECKED)
- classify L254-292 (tri-state, optional slop). Ink L242-252: for each segment, distance to clamped centerline projection minus interpolated half-width (`segment-delta` L176-190); min over segments, minus slop; `< -1e-9` inside, `|d| ≤ 1e-9` boundary, else outside. Docstring L180-182: "it is not an independent exact-distance solver for every tapered outline." Shape: per-contour odd/even (L199-218) then outer-and-not-hole; slop applied only when outside L282-291.
- hit? L294-297 = not outside. boundary-distance L299-315: min |segment delta| (ink) or min edge distance (shape); docstring L303-304 "nearest individual segment boundary need not be the boundary of their union."
- Callers: classify and boundary-distance ← harness/path.cljs:354,357,375 only; hit? ← none in src/; contour-classify ← tessellation L353,354,559.

## Q6 Harness (harness/path.cljs, CHECKED)
Fixtures: path-ink-component L74-92 — samples `[x y pressure]` → id `[id index]`, position `[x/zoom y/zoom]` (screen-point L32-36), width `(16/zoom)*pressure`, `:pressure` kept, cap/join round; path-shape-component L94-115 — 8-point notched outer (screen 24..104) + 4-point square hole (34..50); path-polygon-component L117-129 — one outer from given points; path-quad-component L131-140 — 24..104 square. Golden specs L241-270: `:pressure-ink` (zoom 0.1, 4 samples pressures 0.2/0.45/0.72/1.0), `:holed-concave` (zoom 1), `:translucent-self-crossing` (zoom 10, X-shaped 4 samples, alpha 0.62). run-path-step! runs only `[:holed-concave :translucent-self-crossing]` L518 — `:pressure-ink` defined, not run (docstring L245-246). Tree golden L297-331: 32×32 quad at group 0 and 17 (affine `[0.5 0 0 0.5 40 20]` L509-511), group 99 expected to throw `:transform/unknown-group`.
Ink samples: synthetic literal vectors only (L253-254, L268-269; harness/region.cljs:175-179). No pointer/pen event code anywhere in src/app/client (grep addEventListener/pointerdown/PointerEvent: none).
Checks: two identical renders → sha256 byte-identical L205-228; PNG data-url L236; mesh triangle-count/coverage + f32 quantization error L57-72,L291-294; parity L333-393: 7 zooms (shared.cljs:25-32) on the holed shape only (L341), 128×128 (shared.cljs:19), pixel centers with `boundary-distance·zoom > 1.25` skipped, CPU classify vs GPU `alpha>128`, pass iff no mismatch and both classes present; color L395-426 source-over quad ±3 bytes legacy vs linear; dirty check L451-497 expects `frame-1 derived 2 writes 1`, `frame-2 unchanged`, `frame-3 (revision reminted) writes 1 derived 0`, `frame-4 (zoom 10) derived 2`. Ink parity is not checked (only shape fixture at L341).

## Q7 Curves / pressure / time (CHECKED)
- `:pressure` and `:gesture-time` are optional stroke-point keys, validated finite only (component L43,48-50). Neither is read by tessellation.cljc or renderer.cljs (grep "pressure|gesture-time": hits only component.cljc and harness). Harness sets `:pressure` and uses it only to compute `:width` L88-89; never sets `:gesture-time`.
- bezier/curve/spline/tilt/velocity/timestamp/smooth/simplif: not found in any slice file (grep empty). No docstring explicitly rules them out; absence only.

## Q8 frame.cljc dependency key (CHECKED L16-22)
`[(mapv (fn [item] [(:path/material-id component) (:path/revision component) (:container item)]) draw-items) lod]`. Docstring L13-14: "Trusts revision to change for geometry/paint edits; excludes compact transform index changes."
