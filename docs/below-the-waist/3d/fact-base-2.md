# 3D kind — fact base, session 2 (Claude lane, 2026-09-06)

The raw facts under `3d-kind-2.html`. Every line is CHECKED by this session's own read of `src/app/client/` at HEAD `4b8e986` (working tree under `src/app/client/` clean at read time: `git status --short src/app/client/` empty), unless marked DERIVED (reasoning from checked lines) or FIELD (the literature, unverified here). Fence: `src/app/client/` only; never `src/app/server/env.clj`. Docs read: `docs/below-the-waist/path-kind/` (path-kind.md, HANDOFF-8.md, HANDOFF-9.md, fact-base-8.md Parts 4 and 7, bench-9/HANDOVER.md head), `docs/below-the-waist/3d-ceilings-starter.md`, `docs/below-the-waist/3d/STARTER-0.md`. For what Softland is for: `vision/LOG.md` entries of 2026-07-11, 07-23, 07-26, 08-02, 08-03, 08-10, 08-28, 09-01, 09-02, and three of Sid's prompts from recall (uuids bc83c63d, b912e83f, ec411449), prompts only, replies unread.

Contamination disclosure: the first 30 lines of the sibling's uncommitted `3d-kind-working-model.md` and `fact-base-1.md` were read to identify provenance (a parallel lane's answer to the same starter, and a Claude-lane session 1 fact base); nothing past the headers was read, and nothing from them is used. Perspectives stay separate.

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
