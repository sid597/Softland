# Hunt: text kind, glyph renderer in focus

Root: /mnt/data/projects/Softland/src/app/client/text/ (all paths below relative to src/app/client/). Files read: README.md, fonts.cljs, shaper.cljs, glyph_pack.cljs, renderer.cljs, shaped_line.cljc in full; layout.cljc / layout_planes.cljc ns docstrings + public entry signatures + `pack-glyphs!`; harness/text.cljs lines 1-76, 219-330, 597-650; harness/core.cljs 48-75; engine/transform.cljc 276-284; engine/device.cljs 20-37, 149-161.

## Callers (grep across src/app/client) — CHECKED
- `text.renderer` required by: harness/text.cljs:15, harness/shared.cljs:17 (shared.cljs:249-250 only reads the two shader strings for digests). **No caller outside harness/.**
- `text.glyph-pack` required by: text/renderer.cljs:16 only.
- `text.fonts` required by: harness/core.cljs:16 only (calls at core.cljs:52,63,64,137,145).
- `text.shaper` required by: text/fonts.cljs:11 only.
- `text.shaped-line` required by: text/layout.cljc:18, text/shaper.cljs:19.
- `text.layout` required outside text/: harness/text.cljs:14, region3d/on_plane.cljc:17 (calls `text-layout/layout` at on_plane.cljc:52).
- `draw-text-system!` (renderer.cljs:862): **no caller anywhere in src/app/client**. `draw-instances!` (838): only from draw-text-system!. The harness draws by hand: harness/text.cljs:54-57 `.setPipeline / .setBindGroup / .setVertexBuffer / (.draw pass 6 num-instances 0 0)`.
- `clone-text-system`, `recreate-text-system`, `share-font-resources`, `update-font-assets`, `destroy-text-system!`: no callers in src/app/client (grep, CHECKED).

## Five-question cards

### fonts.cljs (`app.client.text.fonts`) — CHECKED
- Docstring why: "Output: promises of manifest/config/assets, including a synchronous layout provider and optional complete Slug metadata/curve/band bundle. No persistent state in this file." (fonts.cljs:4-7)
- TAKE: `load-font-assets [font-config]` (120-126). Fields read from config: `:slug {:meta <file> :curve <file> :band <file>}` (127-136), `:font` (100), `:faceRevision`, `:variations`, `:fallbacks [{:id :font :faceRevision :variations}]` (113-118), `:features`, `:language`, `:tabColumns` (141-144), `:id :name` (165-166). `load-font-manifest-async []` fetches `/fonts/manifest.json` (13, 22). `resolve-default-font-config [manifest]` (33-44).
- GIVE: promise of `{:id :name :backend :slug :layout-provider <shaper provider> :slug (when all three present) {:meta <keywordized JSON> :curve-bytes <ArrayBuffer> :band-bytes <ArrayBuffer>}}` (165-172). `:slug` is nil unless meta+curve+band all loaded (157, 169).
- HOLD: nothing; `base-path "/fonts/"` (13) only.
- WHO MAKES INPUT: the manifest JSON at `/fonts/manifest.json` (fetched, 22) — a person adds a config entry there; harness/core.cljs:56-59 picks entries by `:id "dejavu-sans-mono"` and `"ubuntu-sans-variable"`. **fonts.cljs does not parse TTF/OTF** — it fetches bytes only (87-93) and hands font bytes to the shaper (139), and fetches pre-built Slug meta/curve/band files (129-136). The tool that bakes those three files is not in src/app/client (grep for a writer: not found — CHECKED by absence in the slice; ASSUMED it lives outside).

### shaper.cljs (`app.client.text.shaper`) — CHECKED
- Docstring why: "The namespace caches the HarfBuzz wrapper, raw module and in-flight promise. Each provider closes over font/blob/face objects, bidi engine, metadata and a 16-byte WASM extents scratch allocation." (5-8)
- TAKE: `load-provider! [font-sources opts]` (424-430); font-sources = vector of `{:id :revision :url :variations}` (429); opts `{:features :language :tab-columns}` (388-390). Provider's `:shape-line (fn [text opts])` (420-422).
- GIVE: provider map `{:face-id :face-revision :shaper-id :harfbuzz/wasm :shaper-version :features :variations :axes :fallback-chain :metrics :upem :shape-line}` (410-422). `:shape-line` returns a shaped-line column record (below), with `:advance pen-x` and `:base-direction` set (300-304).
- HOLD: `!harfbuzz`, `!harfbuzz-module`, `!harfbuzz-promise` atoms (24-26); WASM at `/fonts/harfbuzz-0.10.3.wasm` (21). Per provider closure: faces (blob/face/font handles, `:unicode-set` JS Set) (127-138), 16-byte scratch malloc (405).
- Libraries: `harfbuzzjs/hb.js`, `harfbuzzjs/hbjs.js`, `bidi-js` (16-18). Shaping: `.shape hb font buffer features-str` (275); reads glyph infos/positions straight from WASM heap (328-344); extents via `hb_font_get_glyph_extents` (363-364).
- Fallback face: per codepoint, first face whose unicode-set has it, else face 0 (143-164). All faces rescaled to the primary UPEM (395-400).

### shaped_line.cljc (`app.client.text.shaped-line`) — CHECKED
- Docstring why: "Fourteen glyph columns and four run columns carry font-unit integers and source/run identities; :ink-w = -1 means no ink." (6-8)
- TAKE: `make-line [glyph-count run-count faces]` (105-111); `from-maps [m]` legacy map → columns (280).
- GIVE: map `{:glyph-count :run-count :advance :base-direction :faces` + glyph columns `:glyph-id :cluster-start :cluster-end (u32) :advance-x :advance-y :offset-x :offset-y :glyph-x :glyph-y :ink-x :ink-y :ink-w :ink-h (i32) :run-index (u32)` + run columns `:run-source-start :run-source-end :run-flags :run-face` (u32)}` (115-137, 91-96). Flags: rtl=1, tab=2 (88-89). `->maps` rebuilds legacy `{:runs :glyphs :clusters :advance :base-direction}` (241-271).
- HOLD: none ("No global retained state", 6).
- WHO MAKES IT: shaper.cljs:280 `sl/make-line`, filled at 308-377.

### glyph_pack.cljs (`app.client.text.glyph-pack`) — CHECKED
- Docstring why: "A WeakMap holds one derived lookup table per glyph-list object. It does not shape text or create GPU buffers." (6-8)
- TAKE: `slug-table [glyphs]` — the `:glyphs` list from Slug meta JSON (renderer.cljs:739). Per glyph row it reads: `:sampleBounds` or `:planeBounds {:left :top :right :bottom}`, `:slug {:banding {:scaleX :scaleY :offsetX :offsetY} :glyphLoc {:x :y} :bandMax {:x} :packedBandMeta}`, `:unicode`, `:index`, `:fontId` (47-65). `pack-draw-item! [float-view uint-view instance-index draw-item table world-transforms]` (166); draw-item = `{:style {:r :g :b :a} :font-size :container :line :indexes :dx :dy}` (167-170, 137; built at renderer.cljs:668-675). `pack-lines! [float-view uint-view lines table world-transforms]` (224).
- GIVE: `slug-table` → `{:floats Float64Array(8n) :uints Uint32Array(4n) :by-font-index :by-index :by-font-unicode :by-unicode}` (72-74). `pack-draw-item!` → next instance index; writes 25 words per glyph (14, 179-215). `count-instances` → int (149).
- HOLD: `slug-table-cache` WeakMap keyed by the glyphs list object identity (16, 38, 75). Invalidation: none explicit; "mutating the list in place would leave a stale cache" (34-35).
- WHO MAKES INPUT: `tl/pack-glyphs!` (layout.cljc:540 → layout_planes.cljc:822) supplies per-glyph primitives `(f index glyph-id shaped? tab? font-id x y cluster-start cluster-end)` (layout.cljc:544-546); renderer.cljs:626-676 builds the draw-item.

### renderer.cljs (`app.client.text.renderer`) — CHECKED
- Docstring why: "Text system maps carry ownership flags for shared font and sizing resources; functions return replacement maps when handles change. There is no automatic per-frame equality cache in update-text-data: each call packs and uploads." (6-9)
- TAKE: `init-text-system [device fformat camera-buffer font-assets & {:initial-capacity :label :groups-buffer :scene-color}]` (432-440); asserts `:groups-buffer` (376); reads `(:slug font-assets)` → `[:meta :curveTexture :width/:height]`, `[:meta :bandTexture :width/:height]`, `:curve-bytes`, `:band-bytes` (319-330). `update-text-data [device renderer-state texts font-assets font-size & {:line-height-factor :line-height :snap-step :world-transforms}]` (770-772); `texts` = vector of lines, each a vector of text items `{:text :x :y :size :r :g :b :a :container ?:layout-result ?:layout-line-id ?:paint-source-range ?:layout-anchor}` (634-657; harness fixture at harness/text.cljs:308-314). `draw-text-system! [pass text-sys attachment-size]` (867).
- GIVE: text-system map `{:backend :slug :scene-color :pipeline :bind-group :bind-group-layout :camera-uniform-buffer :groups-uniform-buffer :sizes-uniform-buffer :instance-buffer :instance-stride 100 :num-instances :gpu-label :owns-font-resources? :owns-sizing-buffer?` + `:curve-texture :curve-texture-view :band-texture :band-texture-view :font-resource-kind :slug}` (334-340, 416-430). `update-text-data` returns state with `:instance-buffer :num-instances :line-offsets :fallbacks :unresolved-glyphs :line-height` (788-794). `pack-instances-flat` → `{:raw-buffer :line-offsets :num-instances :fallbacks :unresolved-glyphs}` (757-761).
- HOLD: no defonce/atoms; all state in the returned map. GPU: curve texture `rgba16float` (323), band texture `rg16uint` (327), instance buffer VERTEX|COPY_DST (276), 16-byte sizes uniform (383-385). Instance buffer grows 1.5× on demand, old destroyed (720-727).
- WHO MAKES INPUT: fonts.cljs:165-172 makes font-assets; harness/text.cljs:308-314 makes text items; harness/text.cljs:322-327 makes world-transforms via `engine/transform`; camera buffer written by `device/update-camera` (engine/device.cljs:149-161).

### layout.cljc / layout_planes.cljc (entry signatures only) — CHECKED
- layout ns: "Output: plane-backed layout, cache transitions, draw-item adapters, source copies, carets, selection rectangles and hits. Caller owns results/cache." (layout.cljc:6-8). `layout [{:keys [provider] :as input}]` throws `:text/layout-provider-required` without a provider (960-967); input fields used by renderer: `:text :source-lines :provider :font-size :line-height :origin` (renderer.cljs:643-648). `legal-zoom?` = `[0.01, 1000]` (118-124). Other entries: `layout-key` 126, `line-by-id` 465, `glyph-indexes-in-source-range` 528, `pack-glyphs!` 540, `measure/wrap/copy/paint/caret/selection/clip/hit-test-result` 1118-1487, `layout-cache-*` 998-1071.
- planes ns: "Positioned geometry uses float32 columns. Glyph IDs occupy 30 bits with tab/direction flags in the remaining bits." (layout_planes.cljc:14-16); masks at 17-19. Entries: `plane-builder` 715, `put-glyph!` 742, `finish-planes!` 786, `pack-glyphs!` 822, `compact-result` 394.

### harness/text.cljs — fixtures — CHECKED
- Fixture text: `"oo"`, position/size divided by zoom so screen size stays constant (305-314); world transforms = root + group 17 `{:parent 0 :affine [0.5 0 0 0.5 40 20]}` (318-327). Fonts: dejavu-sans-mono (slug-assets) and ubuntu-sans-variable (t1-assets) (core.cljs:56-59). Three systems, capacities 1/2/2 (607-626). Harness decodes glyph 111 ("o") curves on CPU from the same bytes (274-303, 628).

## Numbered facts

### 1. Technique — CHECKED
- The name "Slug" appears throughout: `slug-vertex-shader` (27), `slug-fragment-shader` (126), `create-slug-*` (280-340), `:backend :slug` (417), `slug-table` (glyph_pack:30), manifest `:slug` config (fonts:127). **"Lengyel" not found** anywhere in text/ (grep). "winding", "signed distance", "SDF", "MSDF", "atlas", "bezier", "even-odd", "nonzero": **not found** in text/ (grep). "band", "curve", "root", "coverage", "quadratic" appear only in renderer.cljs 19-125 comments and the WGSL.
- Comment: "Slug textures encode curve/band outlines rather than raster glyph images. Finite texture formats, float arithmetic, derivative floors and affine hull approximations bound the numerical result." (renderer.cljs:19-21)
- Comment: "solve_horiz_poly: Relative quadratic control points → horizontal crossing coordinates.. Quadratic roots with near-linear fallback." (104-106); "calc_root_code: Three curve coordinate signs → encoded root crossing classes.. Float sign bits and lookup constant." (100-102); "calc_band_loc: ... Bit arithmetic assuming band texture width 4096." (111-113); "calc_coverage: ... Weighted estimate plus minimum-axis safeguard. Describes implemented estimator; no exactness verdict from source." (115-117); "slug_render: ... Derivative-based pixel scale, band lookup, curve-root accumulation on two axes." (119-121).
- Shaders are inlined WGSL strings, not loaded from files: vertex 27-96, fragment 126-265 (prefixed by `device/scene-color-wgsl`, 126). Full text is in renderer.cljs at those lines; per-pixel algorithm (DERIVED from 194-256): (a) `fwidth(render_coord)` → pixels-per-em (195-196); (b) pick horizontal band `band_index.y` and vertical band `band_index.x` via `floor(render_coord * band_transform.xy + band_transform.zw)` clamped to `band_max` (199-201); (c) band header texel = (curve count, offset) at `bandTexture[glyph_loc.x + band_index.y, glyph_loc.y]` (205), vertical headers start at `glyph_loc.x + band_max.y + 1` (231); (d) per curve: two texels of `curveTexture` → p1,p2 (xy,zw) and p3 (.xy), translated to be relative to the pixel (210-211); early `break` once all three x (resp. y) are more than half a pixel left/below (212-214, 238-240); (e) `calc_root_code` from the sign bits of the three y's (215) — Slug's 0x2E74 lookup (145); (f) solve quadratic for roots, scale to pixels, accumulate `xcov += saturate(root+0.5)` / `-= saturate(root+0.5)` (217-225), symmetric for y with opposite signs (243-251); (g) `calc_coverage` combines both axes weighted by `xwgt/ywgt`, floors with `min(|xcov|,|ycov|)`, clamps (188-192); (h) `scene_color(color, saturate(coverage + params.sharpness))` (264), sharpness written as 0 (786-787, comment 783-784).

### 2. Geometry TAKEN — CHECKED
- Per-glyph metadata (JSON): `sampleBounds|planeBounds {left top right bottom}`, `slug.banding {scaleX scaleY offsetX offsetY}`, `slug.glyphLoc {x y}`, `slug.bandMax {x}`, `slug.packedBandMeta`, `unicode`, `index`, `fontId` (glyph_pack:47-65). Shader reads `glyph.z` = bandMax.x and `glyph.w & 0xFFFF` = bandMax.y (198) — DERIVED: packedBandMeta low 16 bits carry bandMax.y.
- Curve texture: `rgba16float`, bytesPerRow = width*8 (323-326): one texel = 4 halfs = two points; a curve = 2 consecutive texels = p1(xy) p2(zw) | p3(xy) (210-211; harness decoder 263-303 confirms "quadratic control-point vectors", 274-276). **Quadratic only** — the solver is `a = p1 - 2p2 + p3` (149, 165); no cubic path exists (CHECKED by reading the whole fragment shader).
- Band texture: `rg16uint`, bytesPerRow = width*4 (327-330): each texel = (u16, u16). Glyph block at `glyphLoc`: `bandMax.y+1` horizontal headers then `bandMax.x+1` vertical headers, each (count, offset); offsets index a curve-reference list of (x,y) texel coords into the curve texture, wrapping at width 4096 (180-186, harness 289-296). Bytes per curve: 8 (curve texture) ×2 texels = 16 bytes + 4 bytes per band reference. Bytes per glyph in textures: not fixed (depends on band/curve counts).
- Per-instance upload (100 bytes = 25 words, renderer:267, glyph_pack:14): words 0-3 world rect (x y w h), 4-7 sample bounds (left top right bottom in em), 8-11 inv_jac `[1/size 0 0 -1/size]`, 12-15 banding scale/offset, 16-19 u32 glyphLoc.x glyphLoc.y bandMax.x packedBandMeta, 20-23 rgba, 24 u32 group index (glyph_pack:190-214; attribute layout renderer:398-404).
- Where geometry comes from: pre-baked files named in the manifest `:slug {:meta :curve :band}`, fetched as JSON + two ArrayBuffers (fonts:127-136, 169-172). **No TTF/OTF outline parsing in src/app/client** (CHECKED by grep across text/; the shaper hands font bytes to HarfBuzz for metrics only, shaper:116-126).

### 3. Glyph-specific vs general — CHECKED / DERIVED
- Font-unit em assumptions: sample bounds are multiplied by `fsize` to get world rect (glyph_pack:186-189) and inv_jac is `±1/fsize` (198-201) — DERIVED: sample space is em-normalized (1.0 = font size); shader names it `ems_per_pixel` (195). `font-line-height` default 1.2 from `[:slug :meta :metrics :lineHeight]` (renderer:539-543).
- Glyph-id assumptions: lookup by `(font-id, index)` then global index, then U+FFFD, then glyph 0 (glyph_pack:96-120); table keyed by `:unicode`/`:index`/`:fontId` (66-71). `pack-glyphs!` callback carries `glyph-id shaped? tab? font-id` (layout.cljc:544-546).
- Skips: tabs and single-space clusters are never painted (glyph_pack:143, 122-129).
- General parts: `slug_render` itself takes only `(render_coord, band_transform, glyph loc/bandMax)` (194) — any block in the two textures laid out that way would render (DERIVED from 194-256; nothing in the shader reads font size or glyph id). Band width 4096 is hard-coded (127, 181) while texture dims come from meta (319-322) — comment 112-113.

### 4. Fill rule and AA — CHECKED / DERIVED
- Accumulation: signed `saturate(root + 0.5)` added or subtracted per root by crossing class (218-225, 244-251) — DERIVED: this is a nonzero-winding-style signed count (crossings in opposite directions cancel), fractional near the pixel. Overlapping same-direction contours would sum >1 and be clamped by `saturate` (191); opposite-direction overlaps cancel. No even-odd path. No explicit self-intersection handling (CHECKED by absence).
- AA: analytic — each root's distance to the pixel center, scaled by `pixels_per_em`, gives a 0..1 contribution (`saturate(roots.x + 0.5)`, 219); weights `saturate(1 - 2|root|)` (220) favor the axis with a crossing near the pixel; final `max(weighted, min(|xcov|,|ycov|))` (190). No supersampling (CHECKED). Extra conservative half-pixel hull dilation in the vertex shader (75-80).

### 5. Fills only — CHECKED
- The fragment shader emits one coverage → alpha (263-264). No stroke width, no outline offset, no second pass anywhere in renderer.cljs; "stroke" not found in text/ (grep). Comment 19: "curve/band outlines" refers to the curve data, not a stroke draw.

### 6. Zoom — CHECKED
- Same GPU data at any zoom: `update-text-data` re-packs instances on every call (763-794) but curve/band textures are uploaded once at init (313-340) and only replaced by `update-font-assets` (442-467). Vertex shader applies `camera.zoom`/`camera.pan` (73-74, 82) and the group affine `axis_x/axis_y/translation` from `groups[instance.group_buffer_index]` (71, 81); `flags & 1` = screen-space (no zoom/pan) (72-74). Fragment uses `fwidth` for pixel scale (195), so no zoom uniform reaches it.
- Camera comes from `device/update-camera` writing `[pan-x pan-y zoom 0 w h]` (engine/device.cljs:155-161) into the buffer bound at binding 2 (29, 289). Groups come from `engine/transform` world-transforms (`transform/buffer-index`, glyph_pack:170; engine/transform.cljc:276-284) written by `device/write-groups!` (harness:605). No `engine/rungs` reference in text/ (grep: not found).
- `tl/legal-zoom?` bounds `[0.01,1000]` (layout.cljc:118-124) — layout-side only.

### 7. HOLD and invalidation — CHECKED
- renderer: per-system map only (no atoms). Font textures owned when `:owns-font-resources?` (429); `share-font-resources` moves the whole bind group incl. camera/sizing/groups ("Shares more than fonts", 473-474). `update-font-assets` destroys old textures only if owned (454-457). Instance buffer: grow-only 1.5×, destroy old (709-728). No per-glyph cache or atlas; no equality short-circuit ("Packs every call", 767).
- glyph_pack: WeakMap `slug-table-cache` by glyph-list identity (16, 38); invalidates only by GC of the list object.
- shaper: three atoms (24-26); failed load resets the promise atom (85-87); provider face handles never disposed ("lacks rollback/disposal", 113-114; "no ... provider teardown", 385-387).
- fonts: none (7).

### 8. Shaper output — CHECKED
- HarfBuzz WASM (`harfbuzzjs`, shaper:16-17, 21) + `bidi-js` (18). Output is positioned glyph ids + advances + offsets + cluster ranges + ink extents in integer font units, in visual order (columns, shaped_line:91-96, 115-137); `:glyph-x/:glyph-y` are pen positions after the pen walk (shaper:371-377); `:advance` = total pen-x (301). Tabs: one virtual glyph per tab run with computed advance (313-321), `tab-width = tab-columns * upem * 0.5` (296).

### 9. text ↔ path cross-requires — CHECKED
- `app.client.path.*` required inside text/: **none** (grep). `app.client.text.*` required inside path/ (component.cljc, frame.cljc, renderer.cljs, tessellation.cljc): **none** (grep). Shared upstream only: both text/renderer and text/glyph-pack require `engine.transform` (glyph_pack:11), `engine.device`, `engine.color`, `engine.compositor` (renderer:13-15); path/ requires not read (outside slice).
