# Path kind — raw fact base (2026-09-05 session)

Fence: src/app/client/ only. Tags: CHECKED = read in source this session; DERIVED = follows from checked facts; ASSUMED = not verified.
Part 1 = session model's own read of the four path files. Parts 2–4 = hunter reports, appended verbatim.

## Part 1 — path family, read directly

### component.cljc (324 lines) — the grammar
- CHECKED :15  `legal-kinds #{:ink :shape}` — two kinds, and the kind fixes both the geometry shape AND the paint operation (ink is always stroked, shape is always filled). No stroked shape, no filled ink is expressible.
- CHECKED :17  `legal-cap-join #{:round}` — the only cap and join.
- CHECKED :41-50  stroke-point = {:stroke-point/id :position :width} + optional {:pressure :gesture-time}. Width is REQUIRED per point (variable-width stroke already in the grammar); pressure/time are raw pen record kept beside it.
- CHECKED :52-57  ink-geometry = {:stroke-points (≥2, unique ids) :cap :join}. Straight segments between points; no curve segment type anywhere in the file.
- CHECKED :59-81  shape-geometry = {:contours (≥1)}, contour = {:contour/id :role ∈ #{:outer :hole} :points (≥3)}. Polygons only. Holes are declared by ROLE, not by winding.
- CHECKED :30-39  paint = {:color rgba :opacity :color-space #{:srgb} :alpha-association #{:straight}}.
- CHECKED :176-190, 230-252  ink hit rule: per segment, project point, interpolate width linearly along the segment, delta = distance − half-width; classification = min delta over segments (union of tapered capsules). Round caps fall out of the clamped projection.
- CHECKED :199-218  contour-classify = boundary scan then even-odd ray crossing ("odd/even").
- CHECKED :268-292  shape classify = inside iff inside some :outer AND inside no :hole; boundary if on any contour edge; slop widens outside→boundary/inside.
- CHECKED :131-139  content hash includes paint; :134-135 says the mesh cache key does not.
- CHECKED :105-112  validation is structural only: "does not establish simple polygons, contained holes or nonzero segments; tessellation can reject".

### tessellation.cljc (607 lines) — polyline → triangles
- CHECKED :14-37  three zoom bands (LODs): zoom [0.01,0.1) → fan-resolution 4; [0.1,8] → 8; (8,1000] → 16. Outside [0.01,1000] throws.
- CHECKED :201-271  ink expansion: per segment a quad of two triangles offset by each endpoint's half-width (tapered quad); round caps = half-circle fans; round joins = fans on the outer side of the turn between incoming/outgoing normals. Docstring :205 "overlapping stroke pieces remain separate triangles" → overlaps are drawn twice (double blend when alpha<1).
- CHECKED :209  fan-resolution is the ONLY thing zoom changes for ink; :543-544 "Zoom is unused by shape triangulation".
- CHECKED :121-131  zero-length segment throws.
- CHECKED :67-96, 273-283  bbox normalisation to unit scale then denormalise (numeric conditioning).
- CHECKED :285-567  shape fill: orient outer CCW, holes CW, bridge each hole to the outer (rightmost hole vertex → nearest visible outer vertex), then ear-clip; O(n²) per ear, O(n³) worst (:484-485). Self-intersecting contours are not handled ("overlapping outers and malformed hole relationships are not resolved here", :544-545). ≈280 lines exist only because the renderer needs triangles.
- CHECKED :42-55  mesh cache key = [canonical kind+geometry, algorithm label, LOD id]. Paint excluded.
- CHECKED :569-588  mesh = {version 2, algorithm, lod, cache-key, :coverage :aliased-v1, flat vertices (local 2D), triangle-count, vertex-count}. No antialiasing of any kind.
- CHECKED :590-607  derive-mesh-set: cache grows with every key seen, no eviction.

### renderer.cljs (264 lines) — flat triangle lane
- CHECKED :17-18, 113-121  vertex = 28 bytes: position f32x2 (local), color f32x4 (straight RGBA, opacity folded in), group index u32. Paint is replicated per vertex.
- CHECKED :24-57  vertex shader: local → group affine (axis_x, axis_y, translation; flag bit 0 = screen-space) → world*zoom + pan → NDC. Camera = {pan vec2, zoom f32, screen_dimensions vec2}. Zoom is a scalar camera move in the shader.
- CHECKED :59-62  fragment = flat color through scene_color transfer. No coverage, no AA.
- CHECKED :126-128  triangle-list, cullMode none.
- CHECKED :192-240  prepare-path-frame!: frame key = ordered [material-id revision container] rows + LOD (frame.cljc :9-22). On any change: derive meshes (cache), REPACK EVERY ROW of the frame into one typed array, one writeBuffer. Docstring :196 "whole-frame packing and one upload".
- CHECKED :242-251  draw-path-range!: one contiguous draw per range; caller orders/clips.
- CHECKED :7  renderer does not own camera or group buffers (borrowed from engine).

### frame.cljc (22 lines)
- CHECKED :9-22  key = [[material-id revision container]…] + lod. Crossing a zoom band changes the key → whole frame re-derived and re-uploaded.

### Derived from Part 1
- DERIVED  Stored form is NOT triangles; it is the pen's sample polyline with per-point width. Triangles are derived+cached. The leak is subtler than "GPU format stored": the curve was flattened at pen time at the pen's sampling rate, so there is nothing for the display to re-flatten at its own tolerance; only cap roundness is re-derived per band.
- DERIVED  CPU classify (union of tapered capsules) and GPU draw (overlapping triangles) DISAGREE for translucent ink at self-overlaps and inner joins: CPU says one region, GPU paints twice.
- DERIVED  Sid's stroke definition ("region within half the width of the centerline") is exactly the round-cap round-join case, which is exactly what legal-cap-join permits. Butt/square caps and miter/bevel joins are extra rules outside that definition.
- DERIVED  The hole/outer ROLE system + bridging + ear-clipping is the cost of a triangle renderer that cannot evaluate a fill rule; a winding-evaluating renderer needs none of it.

## Part 2 — hunter report: makers and consumers of path components (verbatim)

Only three constructors exist anywhere in src/app/client/ outside the path family; all three live in harness/path.cljs and end at the same `revisioned-path` gate (path.cljs:38-46), which calls validate-component! then stamps :path/revision with component-content-hash. (checked — grep for :path/kind across client returns only path.cljs:83, 103, 125.)

| Constructor | file:line | kind | geometry filled |
|---|---|---|---|
| path-ink-component (public) | path.cljs:74-92 | :ink | {:stroke-points [...] :cap :round :join :round} |
| path-shape-component (private) | path.cljs:94-115 | :shape | {:contours [outer hole]} |
| path-polygon-component (public) | path.cljs:117-129 | :shape | {:contours [outer]} |
| path-quad-component (private) | path.cljs:131-140 | — | polygon with 4 listed corners |

Paint always path-paint (path.cljs:26-30): {:color :opacity :color-space :srgb :alpha-association :straight}. Per-stroke-point (path.cljs:85-90): {:stroke-point/id [id index] :position (screen-point zoom [x y]) :width (* (/ 16.0 zoom) pressure) :pressure pressure}. Contour points are bare [x y]. screen-point (path.cljs:32-36) divides by zoom: fixtures authored in screen pixels, inverse-scaled to local. Docstring path.cljs:7: "Fixtures inverse-scale geometry with zoom where constant screen extent is intended."

Fixtures: :pressure-ink (path.cljs:248-255, 4 samples, zoom 0.1) built but NOT run — run-path-step! passes only [:holed-concave :translucent-self-crossing] (path.cljs:517-518). :holed-concave (257-261) 8-pt concave outer + 4-pt hole, zoom 1.0. :translucent-self-crossing (263-270) ink X, alpha 0.62, zoom 10. Group-tree golden (308-313) one polygon in groups 0 and 17 plus group 99 (must throw). Parity fixture (341-342) holed-concave at zooms 0.01 0.1 1.0 8.0 10.0 100.0 1000.0 (shared.cljs:25-32). Color fixture (402-404) quad. Dirty-check (459-466) two polygons, zooms 1.0 then 10.0. Region3D boundary ink (region.cljs:175-180) 5 pressure samples zoom 1.0. Region3D surround quads (region.cljs:1151-1164).

No fixture is generated from a formula: no Math/cos, Math/sin, circle, ellipse, bezier, arc in path.cljs, on_plane.cljc, on_plane_renderer.cljs (grep empty). Every rectangle is four literal corners. No circle fixture exists.

Consumers: (1) harness/path.cljs — prepare-path-frame! (:176), draw-path-range! (:185); re-derives tessellate for evidence (:285, :343); calls classify/boundary-distance (:354, :357, :375). (2) region3d/on_plane.cljc:101-117 pack-placed-ink — derive-mesh-set at hardcoded placement-zoom 1.0 (:20, :108); keeps only :vertices and color; drops triangle counts, coverage, roles, widths. (3) region3d/on_plane_renderer.cljs — ink-pack (:189-199); pack-one (:211-235) supports only :ink, :shape → :unsupported-kind; build-uploads (:271-308) expands each vertex to 22 floats = [x y] + 16-float matrix + 4 color, stride 88 B (:19); comment :276-277 "Repeats matrix/color per vertex ... a bandwidth/storage choice"; draw-placements! (:398-418) draws only :ink. (4) harness/region.cljs placement-ink-vertices (:208-223) re-derives meshes to count vertices.

Harness checks (run-path-step! path.cljs:499-562): determinism via SHA-256 of two renders (:205-228, :544-545); readback 128×128 rgba8unorm (:147-203); CPU/GPU classification parity at 7 zooms, classify vs alpha>128, excluding a 1.25-screen-pixel boundary band (:389-393, docstring :338-339); float32 quantization evidence (:57-72); group transport (:546-549); color legacy vs linear-premultiplied (:395-426); dirty/upload counters (:451-497: frame 4 at zoom 10.0 derives 2 again). Result stamps :coverage :aliased-v1, :product-pick :cpu-path-authority, :self-overlap-alpha :direct-triangle-double-blend-declared (:556-559) — declared labels.

Hunter's boundary reading: same drawer, one branch point (pack-one); pre-baked vs user-defined is not a distinction this code makes (no generator, no parameter record); ink points and contour points are siblings, not general/special; both reduce to :vertices before the GPU.

## Part 3 — hunter report: text renderer (the Slug lane) (verbatim)

Outline data: fonts.cljs:120-136 fetches three prebuilt files under :slug — :meta (JSON), :curve (bytes), :band (bytes). Nothing in src/app/client produces those bytes (checked). renderer.cljs:313-330 uploads curveTexture rgba16float and bandTexture rg16uint, written once at create (:305-310); no later write path exists.

Per glyph: one quadratic = two adjacent texels (p1.x p1.y p2.x p2.y)(p3.x p3.y _ _) — renderer.cljs:210-211 and harness/text.cljs:299-302. Only quadratics; straight segments are degenerate quadratics (:156-159, :172-175). Bands: per glyph, bandMax.y+1 horizontal + bandMax.x+1 vertical band headers (curve_count, offset) → runs of curve locations (:205-209, :231-235). Per-glyph CPU metadata (glyph_pack.cljs:54-65): sampleBounds, banding scale/offset, glyphLoc, bandMax, packedBandMeta.

Instance = 25 words / 100 B (glyph_pack.cljs:14, renderer.cljs:267; written :190-214): world rect, sample_bounds, inv_jac = [1/fsize 0 0 -1/fsize], banding, glyph loc (4 uints), rgba, group index. 6 vertices per instance. Comment renderer.cljs:26 "Arbitrary-shear coverage needs visual evidence."

Vertex (:69-91): quad corner → world rect → group affine → *zoom + pan → NDC; conservative half-pixel dilation delta = sign*0.5/axis_scale pushed into sample space via inv_jac.

Fragment (:195-264): band pick via fwidth(render_coord) → pixels_per_em; per curve in band, control points made relative to the pixel; root code = (0x2E74u >> shift) & 0x0101u (:139-146); roots of the horizontal quadratic × pixels_per_em; xcov ± saturate(root + 0.5), xwgt = max(...); vertical mirror (:245-250); coverage = max(weighted, min(|xcov|,|ycov|)) (:188-192). Fill decision = signed ray-crossing sum along +x and +y from the pixel, in pixel units, band-restricted. No fill-rule flag; ± signs assume closed consistently wound contours (derived). Output scene_color(color, saturate(coverage + params.sharpness)) (:264).

Glyph-specific in the shader: nothing (checked) — the contract is closed quadratic contours + band grid + affine to pixels. Glyph-specific elsewhere: glyph_pack.cljs row-for fallbacks (:96-120), skip tabs/spaces (:122-147), rect from baseline+font size (:186-189), inv_jac = 1/font-size (:198-201); traversal from a shaped line (layout.cljc:540-548).

Stroke: no width/join/cap/distance term in the fragment shader (:126-265 full read); an open contour yields an unbalanced crossing (:219-224); a stroke would have to arrive already flattened to a closed quadratic outline in the same texel layout, in an atlas region addressed by glyphLoc/bandMax; no code writes the textures after creation.

Zoom: uniform only (device.cljs:149-162 writes [pan.x pan.y zoom 0 w h]); instance data in world units, zoom-independent; update-text-data takes no zoom (:770-772). In-shader changes with zoom: NDC, dilation, fwidth → AA ramp + early-break threshold. Same curve data at every zoom. Layout runs at default zoom 1 (layout.cljc:570).

Bounds on per-pixel work: bands are the only acceleration; loop counts come from the asset; early break (:212-214, :238-240) assumes curves sorted by descending max coordinate — asset's responsibility, unverified. kLogBandTextureWidth=12 (width 4096) compile-time vs meta-declared width (:321); comment :112-113 acknowledges.

State (renderer.cljs:416-430): pipeline, bind group, curve/band textures, camera/groups/sizes buffers, instance buffer, counts, labels; glyph_pack.cljs:16 WeakMap of packed rows. Growth 1.5× (:703-728); comment :713-719: exact-size buffers cost "~30ms/keystroke".

Why-comments: :19-21 "Slug textures encode curve/band outlines rather than raster glyph images..."; :783-784 "Slug renders raw mathematical coverage — no sharpness bias"; :2-9 "no automatic per-frame equality cache in update-text-data: each call packs and uploads."

Callers: harness only (harness/text.cljs, harness/shared.cljs:249-250, harness/core.cljs:16). draw-text-system!, clone-text-system, share-font-resources, recreate-text-system, update-font-assets, destroy-text-system! — no caller in src/ or test/.

## Part 4 — hunter report: engine camera, groups, zoom (verbatim)

Camera: device.cljs:149-162 update-camera writes six f32 into a 24 B uniform: pan-x pan-y zoom pad w h. WGSL struct identical in path/renderer.cljs:25-28 and region3d/renderer.cljs:246. Zoom is one scalar; no matrix, no rotation.

Groups: transform/empty-registry (transform.cljc:105-112) {:groups {0 root} :next-buffer-index 1 :free-buffer-indexes}; root affine [1 0 0 1 0 0], camera :world, buffer-index 0. Spec = :affine xor legacy x/y/scale/rotation (:56-63), plus :parent, :camera ∈ #{:world :screen}; spec->affine (:81-97) → [a b c d tx ty] doubles. Semantic id ≠ GPU row (:14-16); allocate-buffer-index smallest-free-first (:114-123). world-transforms (:242-261) recomposes every group every call ("no incremental cross-call cache"); :flags 1 iff :screen (:258).

Path borrows camera+groups buffers (path/renderer.cljs:81-84, :7). pack-vertices writes mesh x,y verbatim in component-local coordinates (tessellation denormalises :281-283, :565-567). Shader does affine → world*zoom + pan → NDC; :screen groups short-circuit zoom and pan (:46-48). region3d/renderer.cljs:262 same expression "let pixel = world * zoom + pan".

Zoom split: camera move = one 24 B writeBuffer. Recompute = prepare-path-frame! discards zoom magnitude into a 3-band lod; frame key = rows + lod; zoom 1.0→7.9 is pure camera move (no CPU work); crossing 0.1 or 8.0 re-derives all meshes and repacks all rows into one writeBuffer. Zoom is discrete-3 to the path kind, continuous to the shader.

Group move: set-transform (transform.cljc:157-169) — no callers in src/ or test/app/client (also remove-group, world-transform-scale, screen-bounds, transform-bounds, compose-affines, determinant). write-groups! (device.cljs:88-137) rebuilds (max-index+1)×32 B and uploads the whole prefix every call; capacity 16384 rows fixed (:71-73). Path frame NOT invalidated by a group move (key carries :container id, not affine) — declared gap at path/renderer.cljs:197-199 for reassigned buffer index with same container id; pack-vertices:167 bakes group_buffer_index per vertex; allocate-buffer-index reuses freed indexes.

Five discretisations of "how big is a pixel": path/renderer.cljs:200 zoom → 3 LOD bands; region3d/renderer.cljs:1032-1035 zoom and dpr → pixel-size = ceil(w*zoom*dpr) (:1053-54); region3d/renderer.cljs:995-1013 encode-scale = 1.12^floor(log s/log 1.12); on_plane.cljc:20,104-109 placement-zoom 1.0 hardcoded; text/renderer.cljs:535-537 snap-step; engine/rungs.cljc:11 divisors [1 2 4 8] budget-driven. region3d/frame.cljc:15-18 keys on raw zoom and dpr (re-keys every continuous zoom change); path does not.

Precision: CPU f64 throughout transform.cljc (:88-96); epsilons 1e-12 (affine), 1e-10 (tessellation), 1e-9 (boundary, plane). GPU f32 everywhere; f64→f32 narrowing once at upload; no camera-relative rebasing or split-double anywhere.

Docstring vs code: tessellation.cljc:544 "Zoom is unused by shape triangulation" yet component-cache-key:55 puts LOD in the key for both kinds → shapes re-derive an identical mesh on every band crossing. transform.cljc:2 "one coordinate system for CPU and GPU" yet screen-bounds:345 accepts :zoom or :scale while GPU reads camera.zoom only; screen-bounds has no callers. device.cljs:66-67 "per LIVE index" vs write-groups!:106 sizes per highest index.

Callers of prepare-path-frame!: harness only (harness/path.cljs:176,316,469,471,477,479; harness/region.cljs:1165). No driver in src/.

## Part 5 — folds from the six closing sessions (2026-09-05, banked before the rewind)

Team after the cut: session 8 (this one) holds the picture — page, contract, cascade, fork list, what to paint. Session 6 holds the definitions — does each named thing mean one thing, the hard cases, the hidden work. Sid: exploration phase; rank and work on framing, how to think, the whole picture; detail errors are a fix list, never penalties ("it's like starting a painting").

Artifacts that survive the closures:
- 8's page https://claude.ai/code/artifact/af4a8019-6383-4eea-a9b2-1ebb3f89f478 (file: this scratchpad/path-kind.html)
- 7's page https://claude.ai/code/artifact/38bcc965-844b-4a6e-8841-5fd8a25fbb6c, file /tmp/claude-1000/-mnt-data-projects-Softland/344f9292-3a47-4b8c-bd37-0ec01e872808/scratchpad/path-kind-waist.html, hunt reports hunt-path.md, hunt-text.md, hunt-engine.md beside it. Already folds the round with anchors named to sessions.
- 4's bench https://claude.ai/code/artifact/2f98a7e6-beb1-43df-9247-3974586256d6 — per-pixel evaluator over a polyline with per-point width, winding for closed fills, distance bands, over/under halo, mirror of today's fans; cursor readout = the CPU pick formula. Adding the swept-nib rule beside the ribbon rule is one function.
- Recall: this morning's session with Sid 07:48–08:38, prompt uuids 7caba2dd, 255b8f3e, caca9242 — where "the three sources", "stroke is a fill of an offset region", "the rectangle changes job" were first said in his words.
- Memory: feedback-exploration-rank-framing-over-correctness.md (written by a sibling today).

S1 (Codex): two reasoning tools — indistinguishable inputs (two intended outcomes, same input ⇒ find the missing distinction: value, operation, state or relationship) and the next operation (what someone does to the object next tells what must survive). Some marks are objects, some are results of processes (a brush reads and changes a surface). Do not force every object through one sequence; a circle, an imported outline and a brush trajectory keep different information and share particular computations. Keep asking: with this machinery, how would someone make a new tool without changing it?

S2 (Claude): CHECKED no live input path and no definition of the correct image — the code defines only the middle; both ends (hand, pixel) unwritten. CHECKED width required, pressure optional ⇒ a pen model ran before storage, old strokes can never be re-penned. DERIVED whole-geometry cache key + no eviction ⇒ one dead mesh per sample during a live stroke. POSITION write the exact image as math on no machine and keep one slow renderer that produces it; every fast lane is tested against that. POSITION list every reader of the record before shaping it (screen, hit, editor, 3D placement, file, another person). POSITION (repaired) a per-pixel evaluator serving later-on-top, union and ordered deposits returns every passage within reach, distance and parameter each, never the nearest only. GUARD: every definitional error in the round was a renderer quantity promoted to a definition (distance, cache key, projection rule) — when a page sentence starts from the renderer, stop. On 6: take its geometry, hold the frame.

S3 (Codex): phase lesson — judging readiness-for-contract when Sid needed help composing the proposal. Give a rough idea a constructive reading; work inside it; when something is missing, contribute a construction or an alternative picture. Carry: the next-operation question; "the consumer does not care where this came from" holds at a boundary, not for the whole system; shared computation does not determine placement — first what it does and who needs it, then who owns it. Keep one evolving picture with alternatives visible; roles are points of attention, not limits.

S4 (Claude): the page still owes two feelable things — the ribbon/nib sliver at a hard press and the wet stroke while the pen is down; both cheap via the bench. The shared error shape (signed distance for 4, projection rule for 8). State vector-versus-raster ink once on the page as a named decision Sid has not ruled, not as a lane that appeared. Fix list is 6's; do not let it slow the picture.

S5 (Codex): keep the waist larger than the renderer; show where invention happens, how it executes, what another construction reuses. After working a tool, change it: nib or pressure response; edit the centerline after drawing; same geometry as a mask or a text guide; thread over here and under there; save the construction so someone else adapts it — these reveal whether pieces compose and what must survive. Keep Sid's choices distinct from proposals; provisional commitments are fine, not laws.

S7 (Claude): four things on its page not on 8's — the imaging model is the output language and the wrong storage language (its stroke has one width, a pen has one per sample; the stroke declaration is designed from ink, PostScript's stroke is the tip that never changes); placed ink goes on the coverage lane, the 88-byte mesh road is cut now (no keep-until-better); the three things glyphs never needed and ink needs from day one (where screen tolerance lives, a cover policy for a long thin outline in a huge box, a cheap route for the wet stroke); the attack is claim by claim, holds / default / short, each with the condition that flips it — the form Sid can rule on. Caution: 6 pushes on correctness and is right about the detail — every counterexample is a fix, never a reframe; Codex rankings partitioned by family 16/16, so their counterexamples carry full weight and their rankings none. The nib fork is Sid's to close, verbatim and timestamped; ask once.

## Part 6 — session 8's own fold (what I carry into the team)

Hold: the whole canvas — sources (pen, formula, designer, font) → PATH (line/quad/cubic, open/closed, per-anchor attributes) + STYLE (fill rule | stroke width fn, cap, join, paint) → REGION (outline+rule | centerline+w(t) as swept nib) → coverage at the camera's tolerance → pixels; answers (classify, distance, bounds) from the same path; the waist between data above and code below. The axis: per edit / per camera scale / per frame, then place on CPU or GPU. The fork below the waist: Slug-online (small memory, analytic AA, unifies text) vs stencil+MSAA (cheap road, big target) vs compute coverage (best end state, largest lift) vs capsule lane (strokes only, wet ink). Position: Slug-online + wet capsule lane. Cascade per file (component, tessellation, frame, renderer, region3d, harness, new builder).

Corrections accepted as fixes: stroke region = swept round nib (union of discs radius w(t)/2); today's projection rule = ribbon + end discs, an approximation diverging at every taper (counterexample: centerline (0,0)-(10,0), r 1→4, point (5,2.6): outside by projection, inside disc at (6,0)). Spline through samples is a builder policy with alternatives (B-spline, least-squares). Banding bounds the average. Depth + coverage + alpha needs defined behaviour, not exclusion. "Zoom leaves every key" → zoom leaves the stored value; per-scale caches belong to the renderer. Cut the region3d mesh road now.

Folded from others: 7's output-vs-storage thesis; 2's two ends, wet/dry, exact image + slow reference renderer, readers list, cooked record, dead mesh per sample; 4's bench, three-way "inside" disagreement (pick even-odd, text nonzero, clipper throws), vector-vs-raster as a named decision; 1's indistinguishability test and invariants; 3's next-operation question; 5/6's four objects, brush-is-not-data, hidden work behind names.

Forks for Sid's word (positions in brackets): ribbon vs swept nib [nib]; vector vs raster ink [raster kind beside the path kind, strokes as sources to both]; default fill rule [nonzero]; cubics vs arcs [cubics, lower arcs at source]; store gesture + derived path [both]; renderer road [Slug-online + wet capsule lane]. Details stay blurry until boots hit the ground.

## Part 7 — checked facts from session 7's hunters not in Parts 1–4 (from from-7-to-8.md; anchors in sources-7/hunt-*.md)
- Harness parity skips pixel centres with boundary-distance·zoom > 1.25 (harness/path.cljs:341-393) and runs only on the holed shape; ink parity is never checked.
- Renderer contract across the four kinds: all take device + fformat + camera-buffer + groups-buffer; all draw into an open pass except region3d's encode-region-pass! (takes an encoder); group index baked at prepare in all four; zoom arg only path and region3d; text packs every call with no equality cache; text draw needs attachment-size for scissor.
- Text's draw-text-system! has NO caller; the harness draws text by hand.
- slug_render(render_coord, band_transform, glyph) reads no font size and no glyph id (text/renderer.cljs:194-256); em-box and glyph-id assumptions live in glyph_pack (186-201) and the offline bake.
- Region3d placed ink shares the path renderer's !mesh-cache object (region3d/renderer.cljs:1116-1123), fixed placement zoom 1.0 (on_plane.cljc:20), 88-byte vertices with mat4 per vertex, 4× MSAA, depth test no-write, one/one-minus blend; only :ink placeable; placed text exists on CPU with no caller.
- Compositor: no blend modes of its own, no stencil/mask, rect scissor only (compositor.cljs apply-scissor! 612-629).
- Rungs are NOT zoom: offscreen resolution admission (engine/rungs.cljc). The only zoom band in the tree is the path kind's fan resolution.
- Memory arithmetic (DERIVED from layouts): 100-sample stroke ≈ 310 triangles ≈ 930 vertices × 28 B ≈ 26 KB per zoom band today; as ≈70 quadratics × 16 B + bands + one 100-byte instance ≈ 3 KB, zoom-free.

## Part 8 — session 4's Ink Bench handover (2026-09-05; source and HANDOVER.md under bench-4/)
Live: https://claude.ai/code/artifact/2f98a7e6-beb1-43df-9247-3974586256d6. One file, WebGL2, file:// fine. World unit = 1 CSS px at zoom 1, Y down like the client.
Fixtures: the three harness goldens by their own coordinates and colours (translucent self-crossing Z, pressure ink, holed concave shape); a self-crossing pen loop with winding 2 inside; a pentagram; a trefoil with z = −sin 3t per point for over/under; draw your own with pen pressure.
Roads: (1) today's CPU triangles, a JS mirror of stroke-triangles-normalized incl. fan res 4/8/16 by band, aliased, straight alpha, blend over ⇒ double-blend as declared; hole bridging and ear clipping not mirrored. (2) per pixel, one cover: s = min over segments of the stroke rule, sign flipped by winding (nonzero or even-odd) when closed and filled, formula shapes by min/max, ordered intervals on s, coverage clamp((s − edge)/px + 0.5). (3) per pixel, a cover per segment: own segment only, no fills; blend over (sums) or max (union via offscreen layer); order as drawn, reversed, or by thread height; paper halo where the ±4 neighbouring segments are not inked = the crossing.
Stroke rules, both roads and the cursor: ribbon + end discs (segment-delta, today's CPU pick) and swept round nib (exact distance to the convex hull of the two end discs). Counterexample (0,0)–(10,0), r 1→4, point (5,2.6): ribbon +0.10 outside, nib −0.02 inside; both numbers shown at the cursor, sliver visible on the pressure-ink fixture. Union by min under both; round caps/joins free; no butt/square/miter/bevel.
Cost readout = cover area in device pixels × segments tested per pixel (a count, not a timing).
Unfinished: client CPU pick needs the nib formula before pick = draw holds; polylines only; no non-round caps; no fill on the per-segment road; halo cut not anti-aliased; wet case redraws everything per pointer move; zoom slider 0.04–63 not 0.01–1000.
Caution from 4: swept-nib addition verified by hand on two cases, not by screenshot; a "shader failed to build" message would be the new segNib block.
