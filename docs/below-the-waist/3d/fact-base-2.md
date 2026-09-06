# 3D kind — fact base, session 2 (Claude lane, 2026-09-06)

The raw facts under `3d-kind-2.html`. Every line is CHECKED by this session's own read of `src/app/client/` at HEAD `4b8e986` (working tree under `src/app/client/` clean at read time: `git status --short src/app/client/` empty), unless marked DERIVED (reasoning from checked lines) or FIELD (the literature, unverified here). Fence: `src/app/client/` only; never `src/app/server/env.clj`. Docs read: `docs/below-the-waist/path-kind/` (path-kind.md, HANDOFF-8.md, HANDOFF-9.md, fact-base-8.md Parts 4 and 7, bench-9/HANDOVER.md head), `docs/below-the-waist/3d-ceilings-starter.md`, `docs/below-the-waist/3d/STARTER-0.md`. For what Softland is for: `vision/LOG.md` entries of 2026-07-11, 07-23, 07-26, 08-02, 08-03, 08-10, 08-28, 09-01, 09-02, and three of Sid's prompts from recall (uuids bc83c63d, b912e83f, ec411449), prompts only, replies unread.

Contamination disclosure (session 2): the first 30 lines of the sibling's uncommitted `3d-kind-working-model.md` and `fact-base-1.md` were read to identify provenance (a parallel lane's answer to the same starter, and a Claude-lane session 1 fact base); nothing past the headers was read, and nothing from them is used. Perspectives stayed separate while the page was written.

The fold (the successor, the same night, about 02:35 to 03:20): every sibling document and every review in the directory was then read in full, and the page, the bench and this file were amended. Parts 11 and 12 below hold the fold's receipts and its record. The working tree at fold time had a sibling (session 1's successor) active on its own files (`HANDOFF-1.md`, `bench-0/HANDOVER.md`, `3d-kind.html`); none of them was edited by this chair, and none of this chair's facts derive from a file the sibling had uncommitted, except the review files Sid placed in the directory (`feedback-*.md`, `review-6f62711.md`), which are read as what they are: reviews, untracked at read time.

---

## Part 1 — the region row and the object (component.cljc, 773 lines)

- `:15` `schema-version 2`; `:18` `extent-max 1.0e4`; `:22-23` indexed-mesh limits 65 536 vertices, 131 072 triangles.
- `:24` object kinds `#{:mesh :light :empty :text :ink}`; `:25` primitive kinds `#{:box :sphere :cylinder :plane :cone :torus}`; `:26` light kinds `#{:directional :point :spot}`.
- `:53-60` `primitive-defaults`: `:sphere {:radius 0.5 :width-segments 32 :height-segments 16}`, `:cylinder {... :radial-segments 32}`, `:cone` likewise, `:torus {... :radial-segments 32 :tubular-segments 16}`. **The tessellation resolution of a primitive is a field of the stored value**, validated at `:267-306` (segments 3..4096).
- `:40-51` default lenses (perspective fov 50°, near 0.1, far 1e4; ortho scale 10) and `default-view {:pivot [0 0 0] :distance 10 :yaw 0 :pitch 0 :lens ...}` — the view is an orbit (pivot, distance, yaw, pitch) plus a lens.
- `:62-70` `default-component`: base-color, metallic, roughness, emissive — a metallic-roughness material.
- `:72-75` transform = translation, unit quaternion, scale; `:157-170` quaternions within 1e-3 of unit are normalised, others left for rejection.
- `:77-84` shadow constants: map size 2048, PCF 3×3, bias 2 / slope 2.0, comparison offset 0.0015.
- `:373-384` `indexed-triangles` requires `:positions`, `:normals`, `:indices`; normals must match positions in count (`:358-363`). **Normals, a GPU attribute, are part of the stored value** (`transformed-triangles` at scene.cljc:466-487 derives geometric normals for the BVH and ignores them).
- `:436-442` shadows only from directional lights; `:444-458` light schema.
- `:460-478` placed text and ink are references: `{:ref {:address ...}}` (+ text params colour, max-inline-size). Content lives outside the region.
- `:480-484` provenance `{:asserted-by ... :act? ...}` is required on every object (`:502`).
- `:486-516` an object is `:object/id`, `:object/kind`, `:transform`, `:provenance`, optional `:parent`, and exactly the body its kind demands.
- `:593-615` the region row: `:region/id :region/revision :region3d/version :extent :scene :view :background :ambient :region/rect`, all required, no optionals (extra keys are rejected by the shared checker). `:601` **the scene is inline in the region row**: `[:map-of some? object]`. Form validators: ids match keys, parents exist, no cycles.
- `:222-233` `:extent {:width :height :depth}`, each positive and ≤ 1e4. **No file under `region3d/` or `engine/` reads `:extent`** (grep over both directories: only the schema and the harness fixture at harness/region.cljs:105 mention it). It is required, validated, and part of the region's identity (harness `region3d-remint` on extent change, harness/region.cljs:139-141), and read by nothing.
- `:728-745` migration v1→v2 folds `:view-default` into `:view`. `:767-773` `validate-region!`: "Downstream APIs do not all call this automatically."

## Part 2 — derivation, maintenance, camera, pick (scene.cljc, 1125 lines)

- `:18-23` algorithm versions (scene-v1, bvh-v1, camera-v1, tone map khronos-pbr-neutral-v1), `ray-epsilon 1e-7`, `bvh-leaf-size 8`.
- `:209-228` `compose-hierarchy`: world matrix = parent world × local TRS, memoised per call. `:230-243` `affected-descendants` scans the whole scene per parent (no child index; docstring says O(affected × scene)).
- `:245-447` primitive meshes generated from params (box, plane, sphere, cylinder/cone, torus) into positions/normals/indices; `:449-457` `object-mesh`: indexed pass-through or primitive derivation.
- `:466-487` `transformed-triangles`: every vertex transformed on the CPU into world space; triangles carry `:object-id :triangle-index :a :b :c :normal` (geometric normal). Docstring: "Regenerates primitive geometry when called during transform maintenance."
- `:526-558` `build-bvh`: median split on the largest axis, sorting at every level, leaves ≤ 8 triangles, `:object-ids` sets per node. `:560-600` `refit-bvh` (transform-only maintenance refits bounds, keeps topology — read by index only).
- `:626-655` `ray-triangle`: Möller–Trumbore, two-sided, tolerant edges, returns `t`, `point3`, `normal`, `barycentric`, `boundary?`. `:657-668` tie-break by printed object id. `:670-690` `query-bvh`: nearest hit, prunes by best `t`, does not order children by distance.
- `:692-737` look-at, orbit-eye, perspective and ortho matrices. `:739-762` `camera-matrices [view viewport]`: **the viewport (aspect) is an argument**; returns view, projection, view-projection, inverse.
- `:764-777` `ray-from-region-point [camera [x y]]`: region-local pixels → NDC → unproject near/far → ray. `:779-802` `project-point`: world → screen pixels + depth, nil behind the eye.
- `:804-823` `derive-instance-row`: text and ink objects are always classified transparent; meshes are transparent when base alpha < 1.
- `:825-859` `derive-scene`: sorted object ids, instances, world triangles for every mesh object, one BVH for the whole region. Stats count one full rebuild.
- `:926-963` `maintain-transforms`: transform-only changes recompose affected descendants and refit.
- `:965-982` `pick-region [{maintained camera region-point}]`: camera ray + BVH → `{:route :object :object-id :point3 :normal :t :triangle-index :boundary?}` or `{:route :region-background}`. Docstring: **"Intended for mesh picking; text/ink placements are not in this BVH."**
- `:984-1040` `shadow-light-space`: the first sorted directional shadow light; one shadow space per region.
- `:1041-1060` `session-transform-map`: per-session settled transforms and one preview transform overlay the region's transforms; `:1062-1069` `session-region-value` applies them. **Transforms are already split between the row and the session.**
- `:1071-1086` `evaluation-key`: the whole static component (region minus id/revision/rect/view/background, objects minus transform) versus the resolved transforms. Docstring: building the key visits all scene data.
- `:1088-1125` `evaluate-scene`: three outcomes — `:full` (any static change: full derive, rebuild everything), `:none`, `:transform` (maintenance of the changed objects and their descendants). **The unit of change today is "the whole static component" or "one object's transform"; a material edit or one mesh's edit is a full rebuild.**

## Part 3 — the outer key (frame.cljc, 26 lines)

- `:9-18` `region-key` = `[region/id region/revision container zoom dpr session-revision]`. Docstring: session contents and resolved placement contents must change the supplied revision to get past this key. **Raw page zoom and DPR are in the outer key; every continuous zoom change re-enters preparation** (fact-base-8 Part 4 agrees).

## Part 4 — preparation, passes, composite (renderer.cljs, 1457 lines)

- `:28-35` strides: mesh vertex 24 B, mesh instance 112 B, light 80 B, composite instance 20 B, region uniform 112 B; `:29` `max-lights 8`.
- `:41-96` mesh vertex shader: instance matrix per row, inverse-transpose normal basis. `:97-191` fragment common: GGX/Schlick PBR, up to 8 lights, PCF shadow via `sampler_comparison`, neutral tone map. `:192-224` fragment: display modes lit / base / normals via `region.settings.y`. `:229-241` shadow depth vertex shader.
- `:245-266` composite vertex shader: a six-vertex quad at `rect` through the **shared 2D group transform and the 2D camera** (`pixel = world * zoom + pan`, screen groups skip zoom/pan) — the region is placed on the page exactly like any 2D draw item. `:270-275` composite fragment: **`textureSample` of the resolved colour, nothing else** — the surface is colour only. `:280-289` worn fragment: striped cyan corner when the rung divisor > 1. `:293-300` rejection fragment: checker with an orange corner.
- `:783-797` region uniform: view-projection, eye, ambient, light count, display mode, shadow flag.
- `:883-913` `object-depth` = eye-to-object-origin distance; `draw-order`: opaque near-first, transparent far-first by that proxy; transparent background routes all meshes transparent. Docstring: "Per-object sorting cannot resolve intersecting transparency exactly."
- `:995-1013` encode scale bucket = `floor(log(zoom·dpr)/log 1.12)`, hysteresis-free.
- `:1023-1212` `prepare-region3d-frame!`:
  - `:1047-1050` outer key equal ⇒ early return with the old row.
  - `:1053-1054` `pixel-size = ceil(w·zoom·dpr) × ceil(h·zoom·dpr)` from the region rect; `:1055-1059` lease size quantised (256 steps, cap 4096) and optionally clamped by `max-lease-size`; `:1060-1064` encode scale = bucketed zoom·dpr; encode pixel size from it.
  - `:1069-1076` `evaluate-scene` on the raw region + session row.
  - `:1087-1092` **`view = (or (:view session-row) (:view raw-region))`**; `view-key = [view display-mode encode-bucket shadow-space]`; the camera is rebuilt when the view key changes, so a page-zoom bucket crossing rebuilds the camera (aspect from encode pixel size, `:1093-1095`).
  - `:1116-1123` placements prepared with the path system's mesh cache handed in and written back.
  - `:1134-1139` dirty roles: shadow when the scene changed; interior when scene, view, background or placements changed. **Any view change re-encodes the whole interior.**
  - `:1175-1189` desired lease rows reconciled through the binding owner (leases.cljs); composite rows carry the rect and the group buffer index.
- `:1268-1293` shadow pass: both mesh lists into a depth-only pass (transparent meshes cast solid shadows). `:1295-1322` interior pass: MSAA colour (rgba16float) resolved to the lease's resolve target, depth cleared; **order: opaque meshes, transparent meshes, then placed ink** ("separate ordered groups rather than one combined depth ordering").
- `:1324-1364` `encode-region-pass!`: encodes only when the role is dirty or the lease key changed; dirty cleared at encoding.
- `:1390-1411` `composite-region!`: picks composite / worn / rejection by the lease state and draws one quad in the caller's open pass.

## Part 5 — placed content (on_plane.cljc 187 lines, on_plane_renderer.cljs 439 lines)

- on_plane.cljc `:20` **`placement-zoom 1.0`**, fixed; `:8-9` docstring says so.
- `:40-64` `layout-placed-text`: adapts a placement to `text.layout/layout` at zoom 1 ("does not prove a text GPU placement path exists").
- `:66-70` object-local XYZ → plane 2D is `[x, -y]`; `:72-99` `ray->placement-plane`: inverts the object matrix, intersects **local z = 0**, returns `t`, `point3`, object-local and component-local points. **The only 3D→2D pointer map in the tree; not called by `pick-region`.**
- `:101-117` `pack-placed-ink`: `path-tessellation/derive-mesh-set` at zoom 1 → vertices + a legacy colour. The path kind's triangles, at one fixed resolution, regardless of camera distance or page zoom.
- `:151-187` `project-region-anchor`: object-local point → world → `project-point` → clamped to the region rect edge → page group coordinates. **The only 3D→page upward map in the tree** (a point, not a picture).
- on_plane_renderer.cljs `:19` 88-byte vertex (xy + mat4 + rgba); `:26-50` flat shader: `view_proj * model * (x, -y, 0, 1)`, colour through, **no coverage**; `:20-21` depth bias −1 / −1.0, depth test without write (per the path round's Part 7 fact).
- `:201-209` placement key = `[object-id kind status content-revision matrix]`. `:211-235` `pack-one`: ink only; text and other kinds → `:unsupported-kind`. `:310-326` sort by object-origin distance. `:328-385` `prepare-placements!` docstring: "a camera-only change does not change pack key and retains old draw order."

## Part 6 — the engine's surface story (compositor.cljs 857, leases.cljs 242, rungs.cljc 110)

- compositor `:25-27` lease quantum 256 px, max 4096, default pool budget 512 MiB. `:398-407` `quantize-region-size`. `:439-449` a region lease bundle = rgba16float ×4 MSAA colour + depth24plus ×4 + rgba16float resolve (+ 2048² depth32float shadow when requested). `:451-` `acquire-region-lease!` reuses by `[region-id qw qh]`, adds/removes shadow separately, rolls back on failure. `:644-671` `draw-present!`: one full-screen triangle from the scene target to the output with the sRGB encode.
- leases `:15-31` one owner atom: logical identities, device epoch, buffer indexes, pending/free sets, physical lease refs. Logical identity survives physical replacement.
- rungs `:11` divisors `[1 2 4 8]`; `:65-110` `grant`: first divisor whose priced candidate fits the budget, else rejection. Rungs are offscreen resolution admission, not zoom (fact-base-8 Part 7 agrees).

## Part 7 — how a frame is driven (harness/region.cljs)

- `:138-148` a region draw item = `{:region/material <region row with :region/rect> :container <group id>}`.
- `:164-192` the boundary fixture: an `:ink` object whose `:ink {:ref {:address ...}}` names a path component; the resolved placement carried beside the draw item under `:region3d/resolved-placements` as `{:object-id :object :kind :address :status :content-revision :cache-key :component :owner}`.
- `:336-354` paint order: a path draw, the region composite, another path draw (the sandwich) — the region is one ordered entry of the 2D pass.
- `:356-400` the frame: reconcile leases → encode every prepared region's shadow/interior passes → acquire the frame's scene target → one 2D pass painting the closures in order → present → submit → release → read back.
- `:600-626`, `:1023-1030` picks are driven with literal region-local points (`[40.0 44.0]`, `[320.0 352.0]`, `[5.0 5.0]`); no page point → region point map exists in the tree.

## Part 8 — carried from the path round (fact-base-8 Parts 4 and 7, path-kind.md)

- The 2D camera is six f32: pan, zoom, pad, w, h (device.cljs:149-162); zoom is one scalar, no rotation. Groups are affine `[a b c d tx ty]` with a world|screen flag; f64 on the CPU, narrowed to f32 at upload, never rebased around the camera.
- Five places decide how big a pixel is; two are region3d's (`ceil(w·zoom·dpr)` and the 1.12 ladder) and one is on_plane's fixed 1.0.
- Region3d placed ink shares the path renderer's mesh cache object (renderer.cljs:1116-1123), 88-byte vertices, 4× MSAA, depth test no write, one/one-minus blend; only `:ink` placeable; placed text exists on the CPU with no caller.
- Compositor: no blend modes of its own, no stencil/mask, rect scissor only.
- The path page's Position 6: placed ink takes the coverage lane on the object's plane; the 88-byte road is cut.
- The path page's contract: in = path + paint + identity; out = regions to the filler and answers (classify, bbox, outline, offset, evaluate, tangent, length, intersections, split, flatten); four pieces of code (path type, geometry, packer, filler) plus the evaluator; the axis per edit / per scale / per frame.

## Part 9 — Sid's words used on the page (verbatim, with source)

- 2026-07-11, LOG: "we want the pan and zoom to both be applicabale to the whole frame but also be able to do so for only an individual container in the frame as well ... how ready is our system for 3d rendering???"
- 2026-07-11, LOG: "can i say zoom in turn pan get to some area of the 3d map and then say "make a new xyz here" is that possible?? to point and then say something about it will we be able to get the data or context we are talking about in a way that can be sent to an llm ??"
- 2026-07-23, LOG: "the portal is an instance of the softland space".
- 2026-07-26, LOG: "I think we should be able to talk about the space as the entity .... currently its like softland code -> space -> components .... but it should be softland code -> space type -> space -> component. any block component does live in space and space has its own meaning of click drag select etc ... you can think like we can make space -> 2d canvas -> canvas components ... so now this is more world within a world can we handle this in existing engine?" and "i do want to keep the zoom level will make something happen that is going to happen".
- 2026-08-02, LOG: "an engine in which i can do both tldraw stuff, figma stuff and blender type of stuff as well so that what remains is composition on top and never the missing render". Ink truth = the gesture ("Decision 2: yes A").
- 2026-08-03, LOG: "never never ever do a patch work never if smth is wrong systematically we solve it there not a patch work, never do caching NEVER." and "if we have the uids list or some sort of identifier for the diff: what is on the screen, new action, what new diff do i get".
- 2026-08-10, LOG: "imagine a house inspector with their notebook the go to different layers of house and interact with it take their notes and all first from outside the inside etc etc now if we emulate this in softland the driver in this workflow is biderictional like you can control the space and talk about it both from 2d or from the 3d pov"; "the 3d will represent what they are researching about manifested i. 3d to absolute details they can move around interact with it and then tall about it in their lab nitebook"; "OH NO WHERE???? I DON"T LIKE OPTIMISIC UPDATES"; "this will all pass through softland so that its all tagged and get all the properties of versioning, permissions, relations etc. etc. the data part not each individual frame I mean"; "why cant we have a library on top of our rendering engine which the ai can use".
- 2026-08-26, recall bc83c63d: the Region3D description ("a 3D region embedded inside the ordinary 2D world ... composited back into the 2D scene as one ordinary ordered entry") "and is this how it should be? that is why i am asking what is the 3d facet what all is it made up of so that i can understand what we have and what needs to be deleted".
- 2026-08-26, recall b912e83f: "i think hover is one thing that should also be because from a user hover is saying this is where my focus on context is ... And one job back: click in, "you hit this" out ... since we have all 2d, 3d that can be in one single window we need to have a model that is elegant and plays with differential reactive data and mouse actions over data so we can have fine grained reactivity".
- 2026-08-28, LOG: the 3D line "entities wear 3D facets, or the land contains 3D windows?" held open by him until after the fold.
- 2026-09-01, LOG: "this artifact that you created this si something that should should should be possible to do in softland ... build softland in softland".
- 2026-09-02, LOG: "we have build engine and some primitives that get hardcoded and we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks".
- 2026-09-06, the starter: "The store that merges is above this kind; what the kind owes it is stable identity and fine-grained diffs." "the hit answer comes at pointer rate for the whole page, nested regions included, and says only what changed."

## Part 10 — FIELD claims made on the page (unverified here)

- USD: prims with typed attributes at stable paths; layers composed non-destructively by strength order (opinions); references, payloads, variants, instancing; a stage; Hydra as the render-delegate seam with a change tracker that dirties per prim/attribute; scene indices as composable filters (Hydra 2.0). glTF as the runtime scene form. B-rep kernels (Parasolid, ACIS, OpenCascade) with feature histories and the topological-naming problem. IFC for BIM. 3D Tiles with per-tile transforms for geospatial. OpenVDB for sparse volumes. Gaussian splatting as a view-dependent radiance representation, sorted per view.
- Rasterisation vs path tracing take the same scene description. Order-independent transparency (weighted blended, per-pixel lists) versus per-object sorting. Floating origin / camera-relative rendering for precision at scale. Screen-space error metrics with hysteresis for LOD. Clustered lighting for many lights. Virtualised geometry (Nanite) as a GPU-driven frame planner. Decals versus projected 2D on faces. Houdini compiles VEX; interpreted graph evaluation does not hit frame rate at scale. Three.js, Unity, Blender viewports all build on meshes, placement, camera, depth, lights, uploads, passes, picking, lifecycle (Sid's own checklist of 2026-08-26 says the same).


---

## Part 11 — the fold's receipts (the successor, 2026-09-06)

Both bench repairs named by the review Sid pasted were reproduced against the committed bench (`a85fd1e`, `bench-2/seam-bench.html`) by extracting its functions verbatim into Node v20.20.2 before any change was made (the probe script: `patch_bench.py`'s sibling `probe.js`, scratch; the numbers below are the receipt).

- **Classifier.** `classifyStroke([0, 1.5])` on samples `[(0,0,w 2), (10,0,w 18)]` returned `{signed: 0.5, seg: 0, t: 0, w: 2}` (outside by 0.5). The disc of the sweep at t = 0.2 has centre (2, 0) and radius 2.6; the point is 2.500 from it: inside. Brute-force `min_t (|p − c(t)| − w(t)/2)` over 10 001 samples of t: −0.1000. CONFIRMED: the committed classifier projected onto the centreline and read the width there; it is not the declared union of discs. The shader's `segDist` did the same, so CPU/GPU agreement was agreement in one error. Repair: the uneven-capsule closed form (the convex hull of the two end discs; Quilez) as `capsuleSD` on the CPU and in GLSL; t by ternary search of the convex `f(t)` on the winning segment; the inside distance labelled a lower bound.
- **Framing.** `cameraOf(V, surface)` took its aspect from the allocated surface. Rect 420×330 (aspect 1.2727). At zoom 1: surface 448×384 (aspect 1.1667), world (0.00, 1.00, −0.20) → normalized portal x 0.532435. At zoom 2.4: surface 1024×832 (aspect 1.2308) → normalized x 0.530746. CONFIRMED: the same world point moved on the portal with the lease's rounding (the reviewer's figures, 0.532125 → 0.530451, differ in the third decimal from a slightly different probe point; the structure is the same). Repair: aspect from the portal's rect; the viewport is the used sub-rect `round(rect × zoom × DPR)` with the cap scaling both axes; the allocation is quantised around it; the page paints the used sub-rect; the screenshot copies it. After the repair, `aim=k` at zoom 1 and zoom 2.4 both report portal-local (222.4, 109.7) and the same world hit.
- **Today's tree on the framing question (from Part 4, re-read):** the camera's aspect comes from the encode pixel size, `ceil(w·s) / ceil(h·s)` with `s` the bucketed zoom·DPR (renderer.cljs:1053-1064, :1093-1095): within a pixel of the rect's aspect, so the drift is bounded to rounding, but the aspect is still derived from a pixel size rather than declared by the portal. DERIVED.

Bench dumps after the repairs and the derived-face case (headless SwiftShader, DPR 1, `--dump-dom`, 2026-09-06 about 02:50; every number on the page's moments comes from these):

| hash | address (abridged) |
|---|---|
| `aim=k` | page (282.4, 179.7) · R portal-local (222.4, 109.7), surface 420×330 used of a 448×384 lease, framing 1.273 · S t = 3.232 m at (0.00, 1.00, −0.20) · B construction CB rev 0, face E.top, lineage from E · Ftop resolved, rule origin, authored 1000×1000 mm · Π (500.0, 302.0) mm, footprint 0.09 px per Π mm · k inside, segment 34, t 1.00, s 673.3 mm, width 30.0 mm, at least 15.00 mm from the edge · entered k |
| `aim=k&zoom=2.4` | the same page point and portal-local; viewport 1008×792 in a 1024×832 lease; the same t, the same Π; footprint 0.22 |
| `aim=k&occ=0.36` | Q at t = 2.260 m, world (0.50, 1.35, 0.56) · entered Q |
| `aim=k&w=1.4&anchor=origin` | E.top modified (1.00 → 1.40 m wide); Π (500.0, 302.0) at world x 0.00 |
| `aim=k&w=1.4&anchor=normalized` | the same Π at world x 0.20; footprint 0.14 px per Π mm |
| `aim=k&w=1.4&anchor=edge` | the same Π at world x 0.40; footprint 0.10 |
| `aim=k&cut=1` | E.top part 1 of 2, split into 2 by C · Ftop resolved on 2 parts (clip) · k inside · derived: CB → 3 blocks, faces E.top×2 E.front E.bottom E.left E.right E.back C.floor C.wall- C.wall+ |
| `aim=k&cut=1&onsplit=detach` | attachment detached: split into 2 by C and the rule says detach · k stays in the record, on no face · leaf B:E.top (ink detached) |
| `aim=floor&cut=1&yaw=0.05&pitch=0.62` | t = 3.175 m at (0.21, 0.72, 0.00) · face C.floor, generated by C · no attachment here: a generated face inherits no ink |
| `aim=T&cut=1&w=1.3` | E.front modified (1.00 → 1.30 m wide) · T normalized · face (0.500, 0.500) → L (128.0, 128.0) → c, disc |

One screenshot was taken (`aim=k&cut=1&w=1.3&yaw=0.35&pitch=0.62`): the groove splits the top, the stroke is clipped to the surviving parts, the land on the front stretches with the widened face and the notch cuts it, the panel shows the lineage. The `aim=floor` deep link needs a front-on yaw: from the default yaw the right block's top edge hides the groove floor along the line of sight (checked by the dump: the ray grazed E.top part 2 at (0.50, 1.00, 0.46)).

## Part 12 — the fold record: what was read, what was adopted, from whom

Read in full in the fold: the review Sid pasted of `a85fd1e` (its author is not named in the paste; called "the review Sid pasted" throughout); `feedback-a85fd1e.md`; `feedback-6f62711.md` and `review-6f62711.md`; `feedback-working-model.md`; `3d-kind-working-model.md` (the Codex lane); `3d-object-space-and-query.md` and its handoff; `3d-kind.md` (session 1, which had already folded the two lanes) and `bench-0/HANDOVER.md`; `HANDOFF-1.md`; `path-kind/attack-2.md` (the definer). Rankings: none produced; each family ranks its own higher (Sid, 2026-09-05) and this chair is one of the families. The exploration criterion for anyone who ranks: framing and the whole picture, details as a fix list.

Every finding was split want-vs-is before it changed the page. All the definitional findings hit the want (a law of the page contradicted by a scenario: a point cloud with no inside under "one answer set"; a saved render forbidden by "the picture is never data"; the processor standing in for the definition; one word for three crossings; a boundary named as an entrance). The two bench findings hit the is (bugs) and were repaired and recorded. No finding was demoted because "the code does not do it yet".

| Source | Adopted (where on the page) | Held against it, and why |
|---|---|---|
| the review Sid pasted | one question protocol, answers per representation (story 4, the In/Out block, the representation table, Position 2); the picture rule rewritten (story 12, the screenshot moment, the attack table row 6, Sid's question 2); source tolerances in the record (the tolerance paragraph, the identity block); three crossings (the should-be paragraph, Position 3); libraries not engines, the scheduler does not force two (Position 6, the open-world crack); the hit defined by meaning and policy (Position 7, the Q moment, the field list); the two bench repairs (Part 11); the three requests (story 14, the Out block, the attack table row 4); the three attachment rules and the clip behaviour (the two new moments, Position 10); the exact question that remains (the closing section) | the page zoom account it carried forward is kept as is; "3D begins at the view render" is kept for imaging and demoted from entrance to boundary rather than struck |
| `feedback-a85fd1e.md` | capabilities and unsupported across the whole set; agreement bounded to the coordinate relation (story 8, Position 3, the bench handover); authored normals as intent (the attack table row 6); a saved render as a value; lineage beyond the surviving face with parameter maps (Position 10, the Out block); step and face as one attachment (Position 10); the evaluator producing typed values (Position 9) | none |
| the Codex lane (`3d-kind-working-model.md`) | definition versus occurrence (story 3, the In block); surface as three words (the names list); present / embed / derive (Position 3); a gesture leaving the face (the names list); capture during a drag (the click moment); the merge caveat (the attack table row 2); coverage declaring what it reads as (the In block, the path-kind list); the bridge as the two-engine condition (Position 6); the extension question (the closing section) | its "one computational waist with several libraries" is this page's Position 6 in other words; its level table is folded as vocabulary, not as a new section |
| the object-space contribution | query modes and candidates (the Out block, Sid's question 6); visible / geometric / intended as three questions (Position 7); asynchronous answers and identity (the names list); depth incomparable across a portal (the portal moment); emissive or reflectance by declaration (the names list); OpenExec (the attack table row 2); the boolean-split construction (Position 10) | its "no privileged root dimension" is this page's Position 5 |
| session 1 (`3d-kind.md`, `bench-0/HANDOVER.md`; amended by Part 13: List A folded in full, the order rule kept as the criterion and held as the noun) | the rate row "on its own inputs" (the axis table, story 13); render as a request naming its quantity (the Out block, Sid's question 7); copies are two things; face identity does not come free; event routing (the field list); the three camera operations with its bench's measurement (Sid's question 5); the filler callable with an anisotropic footprint (the path-kind list) | its "page zoom is focal length" was corrected by its own reviews to magnification, which is what this page said; its "a space is a frame plus an order rule" is not adopted: this page keeps planar and spatial as the two kinds, because the walker and the packer branch on more than the order rule (depth, units, a metric) |
| the definer's attack 2 | `execute(recipe, inputs, state) → named results + next state` (Position 9, the transfer table); the surface interface beside geometry, authored / logical / execution kept distinct (the Out block's store line); the arrangement as a reusable result with the face choice as data, the 2D instance of the derived-part question (Position 10, the path-kind list) | its records are the path composer's to fold; this page takes only what bears on the evaluator and on derived parts |

What the fold changed in this file besides Parts 11 and 12: nothing in Parts 1 to 10; the CHECKED lines stand at HEAD `4b8e986`, and `git status --short src/app/client/` was empty at fold time too.

## Part 13 — the fold of session 1's line (the chair after the successor, 2026-09-06, about 11:00)

**Why.** Sid asked which of the two lines folds which. Both chairs answered the same way without reading each other's answer: session 2's line is the trunk. This chair's reasons: Part 12 already had a row for session 1 with what was adopted and what was held; session 1's line had folded nothing of this one; two pages each folding the other is a loop that doubles every future fold and republish. Session 1's chair's reasons are in its own bridge, `crossread-a0f20b1.md` (commit `d0463ee`): the same containment argument, plus that this line worked the derived-part question on its bench and opens from Sid's words. Sid's word came in `STARTER-definer.md`: "I have asked them to fold Session 1 into one continuing picture." Session 1's chair wrote List A there, eighteen items its line held that this one lacked; this Part is the fold of List A and the one correction it forced.

**Read for this fold.** `HANDOFF-1.md` (§3, §5, §8), `STARTER-1.md`, `crossread-a0f20b1.md` in full, `bench-0/HANDOVER.md`, `STARTER-definer.md`; `fact-base-1.md` at the lines List A's code claims rest on (93, 138, 183-184, 187, 236, 238); session 1's twin and this page's twin through a Sonnet reader returning verbatim passages with line numbers (this chair never held either page whole); the code at the lines below, own read. `src/app/client/` is unchanged since `4b8e986` (`git log 4b8e986..HEAD -- src/app/client/` is empty), so every CHECKED line in Parts 1 to 10 stands.

**Receipts, own reads at the lines.**

- **The shadow's dirty role (the correction).** `renderer.cljs:1076` `scene-changed? (not= :none update-kind)`; `:1084-1086` the shadow space is recomputed only when the scene changed; `:1089-1092` `view-key [view display-mode encode-bucket shadow-space]` and `view-changed? (or scene-changed? (nil? old) (not= view-key (:view-key old)))`; `:1134-1139` `dirty-by-role {:shadow (or scene-changed? (nil? old)) :interior (or scene-changed? view-changed? background-changed? (:changed? placement-return) (nil? old))}`; `:1324-1348` `encode-region-pass!` encodes a role only when it is dirty or its lease key changed, and clears dirty at encoding. Consequence: an orbit is a session-row view change; it flips `view-changed?` and not `scene-changed?`, so the interior re-encodes and the shadow holds. This page's axis row had said "orbit re-encodes a shadow that did not change" with the same anchor; the derivation came from the harness looping both roles, not from the flags. Session 1's successor made the identical error from the identical lines and logged it as a guard (`HANDOFF-1.md` §7). What stays true: the shadow's key is the scene's update kind at the region's grain, so a `:transform` of a thing that casts on nothing re-encodes it. Bench 0's shadow counter shows both halves (`bench-0/HANDOVER.md`: "its counter rises when the box moves and not when you orbit"). Landed as E1 on the page; the handoff's §3 item 7 and §7 guards carry it.
- **The parity crack in the tree.** `scene.cljc:629-630`, the docstring of `ray-triangle`: "Two-sided CPU query differs from backface-culling GPU pipelines." `renderer.cljs:391` and `:402` `:cullMode "back"` on both mesh pipelines; `on_plane_renderer.cljs:112` `:cullMode "none"` for placed ink. So a ray entering a mesh from behind a face hits it on the CPU and sees through it on the GPU: the tree's own instance of "agree about where, not about what". Landed as E8 and E14.
- **Exposure on the camera.** `renderer.cljs:139` `fn neutral_tone_map(input_color: vec3<f32>)` and `:222` `let mapped = neutral_tone_map(color)` in the region's fragment, into an `rgba16float` target (Part 4, `:380-394`); `compositor.cljs:30-31` the present samples premultiplied scene colour, unpremultiplies, encodes sRGB; `:47-49` the sRGB constants. The space renders linear HDR, the region's shader tone-maps, the surface is premultiplied, the present converts: session 1's "exposure is the camera's, like a real one" describes the chain as it is. Landed as E8 and E13.
- **Depth is forward, not reversed.** `scene.cljc:717-727` `perspective-matrix`: z row `far/(near-far)`, `near·far/(near-far)`, w row `-1`, paired with `depth24plus`, clear 1.0 and `"less"` (Part 4). Landed as E8 and E16.

**Where List A landed (E-numbers are the patch's edits; the twin carries the same text).**

| List A item (session 1's words, abridged) | Where on the page | Weight, and what was held |
|---|---|---|
| The pixel question is recursive | story 2 (E2); the filler row (E10) | full: a frame this page had only the consequences of |
| What must be code, said once: nine things | a paragraph after the ceilings table (E12) | full: adopted as the answer to "is something missing that has to be hardcoded" |
| The order rule as what makes the two kinds two | story 2 (E2); the holds item (E18b); the translation (E7) | full as the criterion; held as the noun (the walker and the packer branch on depth, units and a metric as well) |
| The renderer fork and the position on it | the filler row (E10) | full, marked as about the road, not the model; the four roads named, the flip conditions kept |
| The today reading at one notch | a paragraph after the today drawing (E8) | full: the five renderer quantities that became definitions, plus the two tree instances found here |
| Physics scope declared on the space | the In block's space fields (E3) | full |
| Curves in space as a shape language | the representation table, new row (E4) | full |
| Sweep generalises | the geometry row (E9) | full |
| Sections keep their conics | the path-kind list (E15) | full |
| The portal's mapping kinds; the mirror | the Portal item in the names list (E6) | full; travel named as the third kind seen from the page |
| Hidden-line removal as a CPU visibility answer | the BIM row (E11) | full |
| CPU two-sided, GPU culls | Position 3 (E14); the today paragraph (E8) | full, with the anchors re-read |
| Exposure is the camera's | Sid's question on the surface (E13); the today paragraph (E8) | full, with the anchors re-read |
| Deformation as a placement on the axis | the per time row (E5) | full |
| Reversed-Z as rebase's depth twin | the path-kind list (E16); the today paragraph (E8) | full, with the anchor re-read |
| The chart precursor before the boolean | the closing section (E17); the fix list (E18c); the handoff's owed list, before the merge clause; `bench-2/HANDOVER.md` | full; the definer constructs it (`STARTER-definer.md`), this chair folds |
| Bench 0's shadow with its own key | the axis row (E1); the fix list (E18c) | full; bench 0 stays live as the receipt |
| Bench 0's three wheel operations | already on the page (Sid's question 5); the fix list (E18d) | already adopted; bench 0 stays live as the thing Sid can wheel over |

Also folded: the translation of session 1's words (chart, attachment, the three verbs, its Positions 6 and 7) as one item in the names list (E7); the "Adopted from session 1" item rewritten to list every landing (E18a); "Landed" and the foot amended (E19). Session 1's List B (what this page holds that its page lacks) needs no action: it is this page.

**Held against, with the reason.** One item only: "a space is a frame plus an order rule" as the noun. The criterion is adopted (story 2 now says what makes a space one kind or the other); the noun is held because the walker and the packer branch on depth, units and a metric as well as on the order, and "planar | spatial" is the field in the record.

**What this closes.** Session 1's line: its page and bench are folded in full and are not folded again; its artifacts are not republished; its chair puts the one closing line at its page's head at Sid's word (its own proposal in `crossread-a0f20b1.md`). Its fact base stays the deeper anchor set for the region lane (three hunter reads at file:line) and is consulted, never re-hunted. Rankings: none.

**What the fold changed in this file:** Part 12's session 1 row points here; this Part; nothing in Parts 1 to 11.
