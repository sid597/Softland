Markdown twin of 3d-kind.html, generated from the page; the three drawings live in the html. Landed 2026-09-06 (session 1, the Claude lane).

Softland · client · 3D kind · 2026-09-06 · session 1, the Claude lane · the other lane's working model sits beside this page in the same directory, unfolded

# The 3D Kind
What a 3D thing is and what it is made of, how a page holds a 3D space and a 3D space holds a page, and what must exist as code below the waist so that parametric CAD, film tools, open worlds, BIM, the planet and scans can be built above it as data. Read against the path kind's picture: one waist for 2D and 3D, or a reason for two. Exploration phase: framing and the whole picture first; details are a fix list.

**Fence** src/app/client/ only
 **CHECKED** a hunter read the lines (fact-base-1.md) · **DERIVED** from checked lines · **FIELD** from the literature, unverified here · **POSITION** ours, awaiting Sid

## What is decided and what is proposed
Sid's words, kept apart from positions so neither session promotes or demotes them. Everything else on this page is a position.

**Ruled · Sid's framing, verbatim in the ceilings starter**
"we have build engine and some primitives that get hardcoded and we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans without getting into git merge deadlocks, anything in ecs layer should be creatable and then saved for reuse or build higher order things from it"
"When you see the ceilings, do not try to hack through them. We want to build through them. the question we are asking is: what is the below-the-waist layer that should exist for everything?"
**Adopted as a working basis, open to attack · 2026-09-06 · the session starter**
"In the field, a 3D scene is objects with transforms, geometry, materials, lights and a camera, and rendering is projection, visibility and shading; the interchange forms are settled and the execution is contested … One composition primitive joins the two models: render a scene of either into a surface, and a surface is paint in the other, in both directions and recursively. The 2D contract ends at a region drawn with per-pixel coverage; whether 3D begins there is open." And the method: "every session that started from rendering made a definitional error, and every session that started from what the object must mean made none."
**Carried from the path round · ruled 2026-09-05 about 16:50**
"yeah fuck the correctness and penalising it … framing and how to think is much more important in exploration phases."
**Carried from the path round, as positions, not decisions**
The path contract (path + paint + identity in; regions and answers out; four pieces of code, an evaluator beside the sources); Position 7 (what a varying width means) open, Sid's to close; the executor named by both chairs on 2026-09-06 (the composer's Position 8, the definer's Position A).
**Not decided**
Everything on this page. The three things that remain are named once at the end: a decision about the kind's output, a definition the definer's chair should work, and one question only Sid's feel can close.

### Sid's words and this page's words
- **region** today's fused row: a space, its scene, its camera and a rect on the page in one thing. On this page the word keeps meaning today's thing.
 
- **space, spaces** his "nested planar and spatial spaces": a frame with a rule for who is in front. A page is one; a 3D world is one.
 
- **portal** his word, kept: a thing in one space whose look is another space seen through a camera. A region on a page is one; a window on a wall is one.
 
- **surface** his word, kept: what rendering a space through a camera produces; paint in the space that holds the portal; also what a light or a probe reads.
 
- **land** "another land": another space, seen through a portal.
 
- **seam** his word, kept: the crossing between a space of one kind and a space of the other, in either direction.
 
- **chart** minted here, marked: a flat space with declared order that sits on a surface of a 3D thing, at that surface's depth. Ink on a face lives in one.

## [position] The 3D kind as it should be
Spaces hold things; a portal is a thing whose look is another space; a chart is a flat space on a face. Below the waist, the pieces the path picture already had grow a 3D shelf each, two pieces are new, and one is promoted to the centre.

[FIGURE: see 3d-kind.html for the drawing]
Caption: **Should be.** A page is a space whose order is declared. A region on it is a portal into a space whose order is derived from the view. A stroke on a face lives in a chart, a flat declared-order space at the face's depth, drawn as a layer of the face's look. A portal on the wall shows another land the same way the page shows this one. Below the waist, the path picture's pieces each grow a 3D shelf; the visibility resolve and the material stage are new; the executor moves from beside the sources to the centre, because every 3D ceiling is a graph re-run on edit.

## The story, read off the picture
1. Everything a 3D space shows ends as one question per pixel: which surface is nearest along this pixel's line of sight, and what colour does that surface give under the lights.
2. The first half is visibility, the second is shading, and shading's inputs include other renders of the same space, from the lights, so the question is recursive where 2D's never was.
3. A 3D thing is a shape in a space with a look and a name: a shape in one of several languages, a placement, a material, and identity for the thing, its faces and its copies.
4. A modelled mesh, a formula solid, a scan, a 2D shape given depth and a whole scene are sources of that thing; the object is general and the sources are above it, as pen, formula, designer and font are above the path.
5. A space is a frame plus a rule for who is in front: on a page the order is declared, in a 3D space it is derived from the geometry and the camera.
6. Today's region is three of these fused: a space, its scene, and a portal that shows the space on the page. Split them and the region stops being the thing; it becomes one way of seeing a space.
7. A portal is a thing in one space whose look is another space seen through a camera. A page holding a region and a wall holding a window are the same object, and ink on a face is the flat case: a chart, a declared-order space at the face's depth.
8. Cameras compose as a chain of matrices. Page zoom is the region camera's focal length and never its position; orbit and dolly are its position; nothing stored knows either.
9. Rendering a space through a camera into a surface is one primitive, whether the surface is a shadow map, a portal, a region on a page, or the second eye. It is also an answer data can ask for, the fifth answer 2D never had.
10. Sort every value by when it changes: shape, look and placement per edit; level of detail per scale, keyed on projected error; the camera and the resolve per frame; derived surfaces on their own inputs, so a shadow map does not care that you orbited.
11. Click and hover are one function: a ray into a space returns the nearest thing, and through a portal or into a chart the ray continues, so the answer is a chain ending in the innermost value, each link changing at its own rate.
12. Two per-pixel resolves nest in one waist: 2D coverage runs inside a face's look as paint, and a 3D space's resolve runs inside the page as one ordered item. Neither replaces the other.
13. The ceilings are graphs, feature trees and relationships re-run on every edit. An executor that runs saved constructions and tracks what they read is the centre of the waist, not a fifth piece beside it.
14. What must be code: the transform tree with cameras and rebasing, the visibility resolve with a transparency policy, render to surface from any camera, the material stage with the 2D filler as a layer, a geometry shelf, the per-scale mechanism, the pick chain, and the executor. Everything else is data.
15. One waist. The 3D code is shelves on the pieces the path picture already has, plus two new pieces and one promotion. Two waists only if 2D inside 3D could not be a chart, and it can.

## What a 3D thing is made of, spelled out
The value the kind takes and the things it gives. This is what every session on the 3D kind should be talking about. The path's value appears inside it twice: as the content of a chart, and as the output of section and silhouette.

### In: spaces, things, looks, portals
```
space      frame            place in its parent (2D affine or 3D transform)
           order rule       declared | derived-from-view
           environment      background, ambient: a space-level look

object     shape            ONE representation —
             mesh           triangles + normals, uvs, face ids
             exact          B-rep patches, subdivision cage   → tessellate(τ)
             formula        SDF, CSG tree                     → march or mesh at τ
             sampled        volume, points, splats, heightfield → own program
             curve in space edges, hair, wires   → the path's sweep, nib facing the view
           placement        one transform | many (copies of one shape)
           look             material { base, metallic, roughness, emissive,
                                       textures, response (a graph, later),
                              layers: chart* { kind frame|face|uv|projection,
                                               content: paths, text, images
                                               in declared order } }
           identity         object id · stable face ids · copy ids · revision
           construction?    what it derives from: a saved construction

light      a thing whose look emits: kind, colour, intensity, range, cone, casts?
camera     a view of a space: projection perspective|ortho, pose, exposure
portal     a thing whose look is another space through a camera mapping:
             fixed (a picture) | window (derived from the viewer)
             | declared (a mapping the executor runs, e.g. zoom → dolly)
           events pass through?  recursion limit

tolerances three, none in the value: the source's fit, a consumer's
           request, the device's (per scale: projected error)
```

### Out: a resolve, and answers
```
to the GPU   per space per view: a draw into the space's
             visibility resolve — opaque by depth, translucent
             by order, edges by the resolve's antialiasing —
             consuming the view's inputs: camera, lights,
             derived surfaces (shadow, probe, portal)
             ⇒ a surface: display-referred, premultiplied,
               which is paint in the space that holds the portal

to the CPU   hit(ray, space)  → chain [space, thing, face,
                                 chart point, …, innermost value]
             bounds · closest(p) · distance(p) for formula shapes
             section(plane) → paths       silhouette(view) → paths
                                           (the path kind's input,
                                            with provenance)
             tessellate(τ) · convert(rep → rep, τ)

as a value   render(space, camera, size) → surface
             the fifth answer: shadows, probes, portals,
             thumbnails, bakes, a plan rendered to a page,
             all data above the waist once this exists
```

The GPU output is what the region renderer already does for one case: a depth-tested pass into a lease, resolved and composited (CHECKED, fact base Part 2 §3). The chain answer's first link is what `pick-region` already returns, minus the face and the chart (CHECKED, Part 1 §5). The surface answer is what the shadow pass already is, from the light's camera (CHECKED, Part 2 §8), never yet callable.

### When does a value change? The axis, not CPU versus GPU
| Changes… | Values | Where it may live | Today |
|---|---|---|---|
| per edit | shape, look, placement, chart contents, identity, construction | CPU; stored; uploaded once; BVH refit | the whole scene re-derives on any non-transform edit; one revision per region (CHECKED Part 1 §4, §8) |
| per scale | tessellation of exact shapes, level of detail, mip residency, tile admission, copy culling; the chart's packer tolerance from the full chain | the per-scale mechanism's caches, keyed on projected error and budget | none for meshes; placed ink tessellated at zoom 1.0 forever; the 256-pixel lease quantum and the 1.12-power bucket are resolution ladders for the surface, not detail (CHECKED Part 2 §2, Part 3 §2) |
| per frame | camera, rebase, the visibility resolve, the composite; window portals with derived cameras | GPU | interior re-encoded when dirty; composite every frame |
| on their own inputs | shadow maps (light + scene), probes (place + scene), fixed portals (scene + camera), a region's surface while nothing moves | derived surfaces with their own keys | the shadow re-encodes with the interior, keyed on the region's revision, not on the light (CHECKED Part 2 §3, §8) |

## The hardest case, lived
A pen stroke drawn on the front face of a box, inside a 3D region that sits on a page; I zoom the page, orbit the region, hover and click the stroke, and inside the region a portal shows another land. Moment by moment, what I see, what the model says is happening, and what today's code would do (CHECKED, from the fact base). The bench beside this page makes the same moments feelable.

1. **I look.** The page shows the region as one item in its declared order: over the big shape, under the translucent bar. Inside, the box's front face carries my stroke as paint on the face: lit by the sun, in shadow where the sphere shades it, hidden where the sphere stands in front. On the back wall a portal shows the other land: its own sun, its own depth, and the box in front of it hides part of the window.
 — The region is a portal into S1; the stroke is a chart layer in the face's look; the portal Q is a thing in S1 whose look is S2 through a window mapping. Three spaces, two seams, one screen.
 — today: the region composites as a textured quad; the ink is a separate transparent mesh with a negative depth bias, unlit, unshadowed; there is no portal and no second space.
2. **I zoom the page, wheel over the region.** The region grows on the page and everything inside grows with it; it is the region camera's focal length changing, an optical zoom, not a step into the room. The stroke's edges stay crisp. The box's silhouette refines if its shape is exact. The window shows a magnified crop of the other land. Nothing stored changed.
 — Only the page's projection changed in the chain P_page · T_rect · P_C1 · V_C1 · M_box. The chart's filler evaluates coverage at the true pixel footprint (screen derivatives); the per-scale mechanism refines by projected error. Zoom is a camera move all the way down.
 — today: the lease re-renders at the next 256-pixel quantum up to 4096, resamples between 1.12-power buckets, blurs past 4096; the interior's projection never sees the page zoom; the ink stays at its zoom-1.0 tessellation.
3. **I orbit the region, drag inside it.** The region camera moves; the page does not. The stroke is re-projected on its face, the same path in the same chart. The sun's shadow does not change, because the light and the scene did not. Through the window I see around the corner of the other land, because a window's camera moves with mine.
 — C1 changed; S1 re-resolves; the shadow surface's key (sun, S1's scene) did not change, so it is reused; the portal's derived camera C2 = f(C1, Q's placement) changed, so S2 re-renders. Orbit costs a resolve, not a derive.
 — today: the interior and the shadow are one dirty region: both re-encode. Ink draw order is only re-sorted when content changes, so a camera-only orbit keeps a stale order (docstring, Part 3 §3).
4. **I hover the stroke.** On every move the pointer becomes a chain: screen point → page point → the topmost page item under it, the region → region-local point → a ray in S1 → the nearest thing: the box, its front face, a chart coordinate → in the chart, the stroke, inside. A highlight watches the innermost link and lights the stroke; a status line watches the face link and names it.
 — hit(ray, S1) returns [box, face front, uv]; the face has a chart; classify(uv, slop) over the stroke's path answers inside. The box link changes rarely, the uv link on every move, the stroke link at its boundary. That is the fine grain a reactive system watches, and it falls out of the chain being explicit.
 — today: hover does not exist; pick returns a triangle index; the ink is not in the BVH; the ray-to-plane function has no caller; the path's classify has no caller outside the harness. Every link exists as a function; none are joined.
5. **I click the stroke.** The same chain, and the answer says: stroke S, on the front face of the box, in region R, at uv (0.31, 0.62), world point p, normal n. A tool that wants to place a note gets p and n; a tool that wants the page gets the first link.
 — "You hit this" is the chain, and each consumer reads the link it cares about. Click and hover differ only in what is done with the answer.
 — today: the harness asserts one hard-coded object and one ray parameter at the rect's centre; nothing else is asked.
6. **I hover the window.** The chain reaches Q and continues: the ray, transformed by Q's mapping into S2, hits a hill. The answer: [R → Q → S2: hill, face, uv]. Whether a drag through the window moves the hill is a declaration on Q: does it pass events through?
 — A portal is a link in the pick chain, not a wall. A picture portal maps the quad's uv through its fixed camera; a window portal transforms the ray itself. Either way the chain crosses the seam.
 — today: nothing to cross.
7. **I drag the box.** Its placement changes. The stroke goes with it, because it lives in the box's chart. The BVH refits. The shadow re-renders, because the scene changed. The window does not, because its land did not. The page item does not, because the region's surface re-resolves under it.
 — Per edit: one transform. Derived surfaces re-run by their keys: shadow yes, portal no. The chart's contents are unchanged data.
 — today: transform-only refit works exactly like this (CHECKED Part 1 §4); the shadow also re-encodes, correctly here.
8. **I share the page.** What travels: the page's items; the region's declaration, its space by reference and its camera; the space's things, the box's shape by reference, its look, its placement, the chart on its front face with the stroke's path value; the portal with its target space by reference and its mapping; the other land likewise. What stays: leases, shadow maps, tessellations, the BVH, packed bands. The other machine re-derives every surface.
 — The value is camera-free and surface-free. Sharing a portal shares a reference, not a copy, the same way an image shares a source digest.
 — today: the region row is already a value; the placed-ref address is an opaque thing nobody in client/ resolves (CHECKED Part 1 §7).

[FIGURE: see 3d-kind.html for the drawing]
Caption: **The seam, both ways.** Down the left, a path value in a chart becomes a layer of a face's look, a fragment in S1's resolve, a surface or a direct draw at the region's rect, one ordered item on the page, a pixel. Up the right, a screen point becomes a ray in S1, a hit on the box's front face, a chart point, and the path's own classify; or a hit on the portal and the same walk in S2. The seam is crossed at the chart going down and at the portal going up, and both crossings are values, not special cases.

### Where a name still hides work
| Name | What it hides | Smallest useful next step |
|---|---|---|
| surface | Its size policy under a changing outer scale (a projected size, not a fixed ladder); its staleness key (its own inputs, never the frame); its colour space (display-referred premultiplied, which the tone-mapped rgba16float lease already is, CHECKED Part 2 §3); its filtering when seen at a grazing angle (mips, which no lease has today, CHECKED Part 2 §4); its edge, which is the page's coverage problem again. | Write render(space, camera, size) → surface as a function with a key, and make the region's lease its first caller. |
| region | Three things fused: a space, a scene, a portal. Two sizes, extent in scene units (written, never read) and rect in page units (CHECKED Part 1 §1). One revision for everything. | Split the row: the space and its scene by reference, the portal with a mapping, a revision per thing. |
| portal | The camera mapping kinds (fixed, window, declared); recursion depth and cycles (a mirror is a portal into its own space); whether events pass through; the portal's own coverage at its boundary; what a window's camera is when the outer camera is orthographic (a page). | Declare mapping and pass-through on the portal; cap recursion; the region is the first portal, mapping fixed. |
| scene | Identity and revision per thing, not per region; things include lights, cameras, portals; copies of one shape as a first-class placement, not a million objects (the group table caps at 16384, CHECKED Part 2 §6). | A revision per object; a shape referenced by many objects; a copies placement. |
| chart | Where a flat frame on a 3D surface comes from: a plane in the object's frame (today's z = 0, CHECKED Part 3 §2), a named face's parameter space (CAD, BIM), the mesh's uv atlas (DCC, games), a projector (decals). Each needs point on surface ↔ chart coordinate both ways. Anchoring: a triangle index renumbers silently (CHECKED Part 1 §8); a chart must anchor to a stable face or frame. | The definer's chair: which chart kinds, and what identity each anchors to. Frame first, because it is stable and exists. |
| material | A fixed menu today (base, metallic, roughness, emissive; no textures, CHECKED Part 1 §2). Layers need the path filler callable from inside a shader with per-chart bindings. A response as data needs a graph runner: shader generation, permutations, compile cost. | Add textures and one chart layer to the fixed shelf; the runner when a look is wanted as data. |
| executor | Everything the ceilings call "the graph": what operations it may call, how it records dependencies, whether it has a GPU door (per-frame deformation), how a saved construction is versioned and shared without merge deadlocks. | The path round's three doors; pick one before the first ceiling is attempted. |

## The four things 2D never had: which force code, which are data
| Thing 2D never had | Below the waist, code | Above it, data | Hidden work |
|---|---|---|---|
| **A mark's pixels depend on other marks** — shadows, occlusion, transparency order | The visibility resolve: depth for opaque, a transparency policy for translucent. Render to surface from any camera, which is how shading gets its other-viewpoint inputs (a shadow map is S1 seen from the sun, CHECKED Part 2 §8). | Which lights cast, resolutions, cascades, bounce counts; the look's response, once a runner exists. | The transparency policy is contested (sorted per object today with object-origin depth, CHECKED; peeled, per-pixel lists, weighted, FIELD). Derived surfaces need their own keys. A graph runner is a shader generator. |
| **Many shape representations at once** — converting is the work | One program per family that cannot lower to triangles (triangles, splats, volumes, points, SDF march), each writing into one resolve or declaring how it composes. Converters as native operations: tessellate, mesh ↔ SDF, isosurface, voxelise, boolean. | Which representation a thing uses, when to convert, at what tolerance: a construction. | Each program's depth rule (splats do not write hard depth). Converters carry a tolerance and lose information one way. The exact shelf, a B-rep kernel, is a library that is bought, never data. |
| **2D and 3D nest both ways** — including 2D derived from 3D | The portal (space + camera mapping + a surface or a direct draw). The chart (a flat space on a surface; point ↔ chart coordinate per kind). The path filler callable as a look layer. Section and silhouette as answers that emit path values with provenance. | What each portal shows and how its camera maps; what sits on which face; which plane cuts the model; which view makes the drawing. | Chart kinds; event pass-through; recursion and cycles; the surface's size policy; hidden-line removal for drawings, which needs visibility as a CPU answer, not a draw. |
| **Ranges and budgets** — past float32, past frame time, two eyes, hands | f64 frames with camera-relative upload (no stage rebases today, CHECKED Part 1 §6, Part 2 §6). Reversed-Z float depth (forward [0,1] depth24plus today, CHECKED Part 2 §9). The per-scale mechanism with admission by projected error under a byte and time budget (bytes only today, CHECKED Part 2 §2). The resolve from N views per frame. | Budget numbers, detail policies, tile schemes, which hand is the pointer. | Rebasing must be total: one stage that forgets jitters (copies, streaming, picking, the chart's uv). The budget mechanism decides architecture (GPU-driven culling and detail selection) and the rungs and leases are its seed. |

## The ceilings on this waist
The 2D round stamped every ceiling "met" and the adversarial pass broke each at its hardest step. So here each ceiling gets three columns: what is data above, what code it needs below that the list above does not already name, and the crack, the step where it will break if the crack is not named now. FIELD throughout; the product mechanics are general knowledge of those tools.

| Ceiling | As data above the waist | Code it needs below, beyond the list | The crack |
|---|---|---|---|
| **Parametric CAD** | Sketches as path values with constraints. The feature tree as a construction the executor re-runs from the changed feature down. Assemblies as things referencing parts, mates as constraints. Drawings as section and silhouette → paths on a page. | The exact shelf: a B-rep kernel (surfaces, booleans, fillets, shells) and a constraint solver for sketches and mates. Tessellation at τ per scale. Exact edges as curves in space through the path's sweep with a view-facing nib. | Exact silhouettes at every zoom: the silhouette of a curved face is view-dependent and must come from the exact surface per scale, not from a tessellation. And the kernel is a million lines bought or written; the day it is wanted is a scope cliff. |
| **Procedural DCC and film** | Operator graphs. Rigs as deformer constructions producing meshes per frame. Looks as graphs. Simulation caches as sampled shapes indexed by time. Two renderers as two executions of one contract. | Subdivision and solvers as native operations. A graph → shader runner. A path-traced execution under the same contract. | Per-frame deformation of a hundred characters is the executor running on the GPU; without a GPU door it is a CPU bottleneck. And nothing in the value may be raster-only (a screen-space trick) or the second renderer cannot draw it. |
| **Real-time open world** | The level as things referencing shared shapes; scatter as constructions; detail and streaming policies as numbers; lighting setup as declarations. An editor many people use at once on one level. | GPU-driven culling, detail selection and streaming: the per-scale mechanism at scale. Copies as a first-class placement. Shadow cascades, probes and reflections as render-to-surface scheduled by data. Physics and animation solvers. Post-process as surface → surface passes. | The budget: the frame is decided by what to drop, and dropping is a mechanism, not a policy. Two views at 90 Hz is the primitive twice with shared preparation. Editing one level together is identity plus merge, outside client/. |
| **BIM** | Element types and instances with typed components. Hosting and joining as constructions with dependencies: move a wall, the roof re-derives. Plans and sections as section(plane) → paths on pages. Schedules as queries over components. | Section of solids. Hidden-line removal as a CPU visibility answer. Dependency tracking in the executor. | Live 2D derived from 3D at drawing quality: hidden-line needs visibility as an answer, section of a mesh is a polyline soup to stitch while section of a B-rep is exact, and every derived line must carry which face made it or the plan cannot be edited back. |
| **Planet-scale geospatial** | Tile schemes, layers, time-dynamic properties. | f64 frames and camera-relative rebase in the transform tree. Detail trees keyed on projected error: the same mechanism. A non-affine frame (geographic → Cartesian) as a node the executor evaluates per tile. Heightfield as a sampled shape. | Precision everywhere at once: every stage that touches a world coordinate must be rebased, including copies, streaming, picking and the chart's uv. One stage that forgets, jitters. |
| **Fields and scans** | Transfer functions, thresholds, layer choices. | Sampled-shape programs (volume ray-march, splat, points), each declaring how it composes with opaque depth. Isosurface extraction as a converter. Octree streaming: the same mechanism. | Composing a splat cloud or a volume with meshes in one resolve: splats do not write hard depth; the practical rule is opaque by depth first, then sorted blending against that depth, declared per shape kind, not free. |

Across all six, the same nine things recur as code: the executor with dependency tracking; render to surface from any camera; the visibility resolve with its transparency policy and the sampled-shape programs' composition rule; the per-scale mechanism, GPU-driven at the top; the geometry shelf, with the exact kernel as its bought member; f64 and rebase in the transform tree; the material stage, fixed shelf now, graph runner later, with the path filler as a layer; curves in space; stable identity for things, faces and copies. Everything else in the six is data. That is the answer to "is something missing that has to be hardcoded": yes, these, and nothing else so far.

## What the 3D kind is today
One notch below the folder map. Each box is a piece with what goes in and what comes out; arrows are real calls. The harness is the only caller of any of it (CHECKED, Part 2 §1).

[FIGURE: see 3d-kind.html for the drawing]
Caption: **Today.** The region is a picture portal executed offscreen, with its scene, its camera and its rect in one row under one revision. The page camera reaches the rect and nothing inside. Placed ink is a decal in a second lane at one zoom. The pick reaches a triangle and is called by nobody but the harness. The shadow pass is the composition primitive, already in the tree, unnamed.

### Today, read against the model
| Today (CHECKED) | An instance of | Where the renderer's convenience became the definition |
|---|---|---|
| The region row: id, revision, version, extent, scene, view, background, ambient, rect (Part 1 §1) | a space with an environment, a scene, a camera and a portal, fused | the row is the thing; extent is written and never read; rect is in page units and extent in scene units, and neither says what the portal shows |
| Composite: a textured quad at the rect through the container affine and the page camera; nothing of the page camera reaches the interior; resampled between 1.12-power buckets (Part 2 §4) | the fixed mapping of a portal, executed offscreen | "3D is a picture on the page": two cameras that never meet, which is the only reason the zoom-crossing question exists |
| Lease sizes: ceil(w·zoom·dpr) → 256-quantum ≤ 4096; rungs by bytes; rejection and worn fills (Part 2 §2) | a surface size policy and a budget mechanism, for one surface kind | a resolution ladder is not level of detail: nothing inside refines with scale |
| Objects: TRS, six primitives or indexed triangles, four material numbers, ≤ 8 lights, one shadow map (Part 1 §2-3, Part 2 §8) | the object: shape (mesh) + placement (one) + look (a fixed menu) | no uvs, face ids, textures, copies or object revision: nothing a chart, a constraint or a fine-grained edit can anchor to |
| Shadow: all triangles from the first directional caster into a depth surface; every casting light reads that one map (Part 2 §8) | render to surface from another camera, read by shading | keyed with the interior on the region's revision, not on the light and the scene; orbit re-encodes what did not change |
| Placed ink: a separate transparent mesh, 88-byte vertices, tessellated at zoom 1.0, negative depth bias, unlit, not in the BVH (Part 3 §2-4) | a chart of kind frame (object-local z = 0), drawn as a decal | paint on a surface is part of the surface's look; it should be lit, shadowed and occluded with its face, never biased, and its tolerance should come from the chain |
| Pick: viewport point → ray → BVH → {object, triangle index, point, normal, t}; harness only; no hover; ink and text absent (Part 1 §5, Part 3 §4) | the first link of the chain | the answer stops at a triangle because triangles are what the BVH holds; a face and a chart point are what a person hit |
| One revision per region; full re-derive on any non-transform edit; refit on move (Part 1 §4, §8) | the per-edit rate, at the coarsest grain | fine-grained reactivity needs a revision per thing and derived surfaces keyed on their own inputs |
| No nesting: no region in a region, no scene reference, placed-ref unresolved (Part 1 §7, Part 3 §1) | nothing | the portal does not exist; containment is one level deep by construction |
| f64 CPU → f32 upload, absolute world coordinates, no rebasing; forward [0,1] depth24plus (Part 1 §6, Part 2 §9) | the transform stage without its two precision moves | the path round's finding again: rebase around the camera before upload; reversed-Z is its depth twin |

## The attack on the working basis, claim by claim
Sid's working basis, one claim at a time. HOLDS means it is the answer. DEFAULT means it works until a named condition. SHORT means it cannot express something already wanted. ANSWERED means the open part closes here, as a position.

| Claim | Verdict | Why, and the condition |
|---|---|---|
| A 3D scene is objects with transforms, geometry, materials, lights and a camera. | HOLDS, two additions | Lights and cameras are things (glTF and USD: nodes with components, FIELD); today's tree already makes a light an object kind (CHECKED). Missing and needed by the seam: the portal, a thing whose look is another space, and identity below the object: faces and copies. |
| Rendering is projection, visibility and shading. | HOLDS, one precision | Shading's inputs are renders of the same space from other cameras: shadow maps today (CHECKED), probes and reflections in the field. So render to surface is part of rendering, not only of composition, and the per-pixel question is recursive; every renderer cuts the recursion somewhere, and where is the contested execution. |
| The interchange forms are settled and the execution is contested. | HOLDS meshes · DEFAULT exact · SHORT sampled | glTF and USD settle meshes, PBR looks, lights, cameras, skins, animation (FIELD). STEP settles exact solids, but kernels are not interchangeable and tessellate differently. Splats, volumes and SDFs have no settled interchange; each is a frontier. Condition: a value that must round-trip those forms carries its representation and a converter tolerance, never a promise of exactness. |
| One composition primitive: render a scene of either into a surface, and a surface is paint in the other, both ways, recursively. | HOLDS as primitive · corrected as model | The model is the matrix chain; render to surface is one execution of it and a direct draw with the composed matrices and a clip is the other. Same image, different cost and crispness. "A surface is paint" leaves three things unsaid: colour space (display-referred premultiplied, which today's tone-mapped lease is, CHECKED), size policy (projected size, not a ladder), staleness key (its own inputs). And the same primitive is what shading uses for shadows, so it is not a seam primitive but the space primitive. |
| The 2D contract ends at a region drawn with per-pixel coverage; whether 3D begins there is open. | ANSWERED: no | 3D begins at the visibility resolve. Coverage re-enters inside 3D as a layer of a face's look, in chart units, with the footprint from screen derivatives (the path page's Position 6, sharpened from a plane to a chart). The seam is not "coverage, then 3D"; it is 3D's resolve with coverage inside it going down, and the page's order with a 3D surface inside it going up. |
| Sessions that started from rendering erred; sessions that started from meaning did not. | HOLDS as method | Today's region is defined by its rendering: a rect, a lease, a composite, a decal, a triangle index. Each row of the today table is a renderer quantity promoted to a definition. This page was derived from what the things must mean and then checked against the tree; the tree turned out to hold one instance of each piece and none of the joins. |

## The fork below the waist
The contract above is stable across the field; the program under it is where teams differ, by workload and platform. Four shapes fit under it. The current region lane is the first, for one space, one camera, one surface.

| Renderer shape | Antialiasing | Translucency | Looks as data | Portals and charts | Memory | Cost to reach from here |
|---|---|---|---|---|---|---|
| forward, MSAA (today's shape, made general) | MSAA 4×; a page with text and ink cannot take temporal smear | sorted per thing, per-pixel lists when needed | an ubershader with a fixed layer stack first; compile per look later | natural: any face may run any layer; portals are surfaces or direct draws | 4× colour + depth per resolve target; the parked GPU-sharing worry lives here | smallest: generalise the region's passes to render(space, camera) → surface; add the chart layer to the mesh shader; add faces, uvs, copies, revision to the object |
| deferred + temporal (the game engines' default) | temporal | hard; a separate forward pass anyway | a fixed G-buffer layout fights per-face layers | charts need the forward pass anyway | a G-buffer per view | largest rewrite; wrong for a page |
| ray query over a BVH (hybrid; hardware when WebGPU exposes it) | any | exact order per ray | any | portals as ray redirection: the model's own shape | a BVH per space | a compute road; the CPU pick's BVH is its seed; visibility becomes one function for draw and pick |
| path traced (film's second renderer) | converged | exact | any | any | scene + BVH | a second execution under the same contract; forbids raster-only values |

[position] Forward with MSAA, generalised from today's region lane, because it is the shape in the tree, it keeps the page crisp, and every face can carry a chart layer. Keep the ray-query road open by never letting a raster-only quantity into the value; the CPU BVH the pick needs is the first half of that road. Conditions that flip this: if open-world counts make forward lighting the bottleneck, clustered forward (still forward) before deferred; if hardware ray queries land in WebGPU, visibility and pick become one function and the depth resolve becomes a cache.

## How the best team in the field would define it, and what they would name
**The problem as they would state it.** Define a scene description as the output language: typed things with stable identity, composition by reference, looks as declarations, cameras and lights as things; USD is the existing answer to that sentence (FIELD). Keep every source above it with its own editable truth: sketches, cages, captures, constructions. Decide up front, because they cannot be retrofitted: the coordinate model (f64 world, camera-relative upload, reversed-Z), the identity model (what a face is, what a copy is), the antialiasing policy, the transparency policy, the material model and whether looks are graphs, where tolerance lives, the seam with 2D, and whether a second renderer is ever wanted. Then the trade-offs Sid did not know to ask about. FIELD unless marked.

- **Forward or deferred.** Deferred wants a fixed G-buffer and hates translucency, MSAA and per-face layers; it is the wrong shape for a page with charts on faces. Forward, clustered when lights are many, keeps every face free to run any layer.
- **MSAA or temporal.** Temporal antialiasing is everywhere in games and smears text and ink on a page. MSAA costs memory per resolve target, which is exactly the parked GPU-sharing worry; decide it with the budget mechanism, not by default.
- **Looks as graphs means shader generation.** Permutations, compile cost (WebGPU pipelines are created asynchronously for a reason), an ubershader versus compile-per-look. The fixed shelf buys time; the runner is the evaluator's GPU form.
- **CPU and GPU must agree on nearest.** Pick over the same lowered geometry the GPU drew, or over the exact shape with a declared error at silhouettes; today the CPU is two-sided and the GPU culls back faces (CHECKED). The id-buffer read at the cursor is the parity check, one frame late, never the primary.
- **Exact shapes are a bought kernel.** OpenCascade in WASM is tens of megabytes; writing one is a decade. SDF and CSG give formula solids cheaply (marched, meshable) and cover much of "parametric" outside manufacturing; B-rep only when manufacturing precision is wanted. A scope cliff to name before the CAD ceiling is attempted.
- **Splats and volumes do not write hard depth.** Sort per frame (a GPU radix sort at scale), blend after opaque against the opaque depth; composing them with meshes is a declared rule per shape kind, and the frontier of the field.
- **Colour across the seam.** The space renders linear HDR, the camera tone-maps (exposure is the camera's, like a real one), the surface is display-referred premultiplied, the page composites it like any item. Today's lease already is that (CHECKED, tone map in shader). Trade: no HDR page; wide gamut later.
- **Precision is camera-relative or nothing.** Double on the GPU is slow; the answer is to upload every matrix relative to the view's origin, and it must be total: copies, streaming, picking, the chart's uv. The same fix the path round found for the infinite canvas.
- **Invalidation grain decides whether reactivity is fine.** A revision per thing, derived surfaces keyed on their own inputs (a shadow map on the light and the scene, never the camera), a BVH that refits per edit and per frame for deforming shapes. One revision per region is the trap (CHECKED).
- **Hover has a cost.** A ray per move needs a BVH kept current; deforming shapes need a refit per frame; a budget applies here too. And hover through a portal is a second ray.
- **Two renderers is a discipline, not a feature.** Nothing raster-only may enter the value (no screen-space trick in a look) or the path tracer cannot draw it. Decide whether film's second renderer is ever wanted, because it constrains the value from day one.
- **Copies are two different things.** A forest is one thing with many placements and no identity per tree (pick returns an index); walls are many things sharing one shape, each with identity (BIM). The renderer batches by shape either way; the value must say which it is.
- **Deformation is a placement choice on the rate axis.** Skinning and blendshapes per frame on the GPU, or per edit on the CPU: the executor's GPU door decides, and the rate axis tells you which.
- **Face identity does not come free.** glTF has no faces; a chart, a constraint, a mate, a hosted door all need something stable to anchor to. Face groups, uv charts or exact faces: decide the anchor model before the first placement, because every placement references it.
- **Event routing at the seam.** Which space owns the wheel when the region fills the screen: page zoom or region dolly. A policy above the waist, but the pick chain must be able to route input to the space it hit.
- **Portals recurse and cycle.** A mirror is a portal into its own space. Depth limit, cycle detection, and a budget for portal surfaces (each is a full render) are policies the primitive must expose.

## Sid's questions, one by one
**What is the general 3D object, and what are its sources? Is the region the thing, or one container of things?**
The object: a shape in one of several languages, a placement (one or many), a look with chart layers, identity for the thing, its faces and its copies, and optionally the construction it derives from. The sources sit above it, as on the path page: a modeller, a formula run by the executor, a capture streamed like an image, extrude over a path value, a reference to another space. The region is not the thing. It is a portal on a page into a space, and the space is the container. Today the region holds all three fused under one revision (CHECKED).
**Containment: 3D inside 2D, 2D inside 3D, or nested spaces of both kinds? What must a 3D thing still mean after I orbit it, move an object, place ink on a face, zoom the page, or share it?**
Nested spaces through portals, both ways, recursively; the root is the window's space, a page on a desktop, a room in a headset; nothing in the model changes with the root. Orbit changes a camera and no value. Move changes one placement, and the ink on the face moves with it because it lives in the face's chart. Placing ink adds a path value to a chart. Page zoom changes the page's projection and refines detail by projected error; no value knows it. Sharing sends values and references; every surface is re-derived on the other side.
**Two cameras: what composes them, and what happens when the page's zoom crosses into a region?**
A matrix chain: P_page · T_rect · P_C1 · V_C1 · M. Page zoom multiplies the projection, which for a perspective region is a change of focal length around the cursor: an optical zoom, a magnified crop, never a step into the room. Nothing crosses; the image is the same whether the region re-projects or resamples, only crispness and cost differ. To enter the space you move its camera, a different gesture, or you declare a mapping on the portal that turns zoom beyond the rect into a dolly, which the executor runs. Today the interior never sees the page zoom and the lease resamples between buckets (CHECKED).
**Picking and hover through the seam: click in, "you hit this" out, when the thing hit is ink on a face of an object inside a region on a page.**
One function, hit(ray, space), applied down the containment: the page's order gives the region; the region's inverse projection gives a ray; the space's nearest gives the box, its front face and a chart point; the chart's classify gives the stroke and inside. Through a portal the ray continues into the other land. The answer is the chain, each link a value at its own rate, which is what a fine-grained reactive system watches. Hover is the same call on every move. Today every link exists as a function and none are joined; there is no hover anywhere (CHECKED).
**The four things 2D never had: which force code below the waist, which are data above it?**
The table above. All four force code: the resolve and render-to-surface for the first; the shape programs and converters for the second; the portal, the chart and section/silhouette for the third; rebasing, reversed-Z and the budget mechanism for the fourth. Above each sits data: policies, declarations, constructions. None forces a second waist.
**What must exist as code below the waist for 3D, and what is its input and output, such that the ceilings can be built above it as data?**
The nine things named under the ceilings table, and the contract block: spaces, things, looks, portals in; a resolve per space per view, a chain answer, geometric answers, and a surface answer out. The executor is the one whose absence makes every ceiling native code; the others are shelves on pieces the path picture already has, plus the resolve and the material stage.
**Am I reasoning from what region3d does today or from how it should be? How would the best team define the problem, and what trade-offs would they name?**
This page was derived from what the things must mean, then checked against the tree; the tree holds one instance of each piece (a shadow surface, a BVH, a lease, a decal, a triangle pick) and none of the joins, and its definitions are all renderer quantities: rect, lease, composite, bias, index. The best team's definition and their sixteen trade-offs are above; the ones Sid did not know to ask about are the bought kernel, the two kinds of copies, the face anchor, and that MSAA versus temporal is decided by the page, not by the scene.

## Positions, each with the condition under which it stops being the answer
**[position 1] the general 3D object**
Shape in one representation, placement (one or many), look with chart layers, identity for thing, faces and copies, construction if derived; in a space; sources above. Stops being the answer: never for the shape of it; the representation list grows, and a representation that cannot write depth declares how it composes and is still an object.
**[position 2] a space is a frame plus an order rule; the region splits**
Declared order on a page, derived-from-view order in a 3D space; environment (background, ambient) is a space-level look. The region row becomes a space by reference, a portal with a mapping, and a revision per thing. Stops being the answer: if a page ever wants real depth for 2D items with lighting, it becomes a derived-order space, and nothing else changes.
**[position 3] containment is nested spaces through portals, both ways, recursively**
The root is the window's space. 3D in 2D is a portal on a page; 2D in 3D is a chart on a face or a page on a wall; 3D in 3D is a window. Stops being the answer: never as a model; as execution, offscreen versus direct is the fork, decided per portal by cost.
**[position 4] render(space, camera, size) → surface is the primitive and the fifth answer**
One function under shadow maps, probes, portals, a region on a page, the second eye, thumbnails and bakes; callable by data, keyed on its own inputs, budgeted like the leases are. Stops being the answer: never; if surfaces blow the budget, the answer is rate-limited by the mechanism, not removed.
**[position 5] cameras compose as a chain; page zoom is focal length**
Page zoom changes the page's projection only; at a region that is an optical zoom, and nothing enters the space. Orbit and dolly are the region camera's; a zoom-to-dolly mapping is a declaration on the portal the executor runs. Stops being the answer: at Sid's feel, if zooming into a region should walk into it by default; then the default mapping changes and the model does not.
**[position 6] two per-pixel resolves nest in one waist**
Ordered coverage (the path filler) and depth plus shading (the 3D resolve); each contains the other, as a chart layer and as a portal surface. Stops being the answer: a path-traced execution replaces the depth resolve and keeps the contract; nothing above notices.
**[position 7] 2D on 3D is a chart layer in the face's look, never a biased decal**
Chart kinds frame, face, uv, projection; frame first because it is stable and exists today; paint in a chart is part of the surface's albedo, so it is lit, shadowed and occluded with the face; tolerance from the full chain, anisotropic. The path page's Position 6 is this with "plane" replaced by "chart". Stops being the answer: for content that must float off the surface (a label on a stalk), which is a thing of its own, not a layer.
**[position 8] the pick is a chain; hover is the same call**
hit(ray, space) over the same lowered geometry the GPU drew, through portals and into charts, ending at the innermost value; the screen point is one source of a ray, the head and a hand are others. The id-buffer is the check. Stops being the answer: never; if the ray-query road lands, draw and pick become one function and this position gets cheaper.
**[position 9] sort by rate, then place; derived surfaces keyed on their own inputs**
Per edit, per scale (projected error), per frame, and a fourth row: on their own inputs, for shadows, probes, fixed portals and static regions. Stops being the answer: never; it is the path round's axis with one row added.
**[position 10] the executor is the centre of the waist**
Saved constructions (feature trees, operator graphs, relationships, scatter, section-to-plan, looks as graphs) are data it runs with dependency tracking; native operations are its shelf; the path round's three doors are its forms and door two is the minimum. Stops being the answer: at the path round's exit for Position 8, if Sid rules that tools are code above a data-only contract; then every ceiling's graph is native and the ceilings are hacked through, which his own framing forbids.
**[position 11] one waist**
One contract, one transform tree, one composition primitive, one rate axis, one executor, one pick, two resolves that nest. The 3D code is shelves on the path picture's pieces plus the resolve, the material stage and the promotion. Stops being the answer: only if 2D inside 3D could not be a chart with declared order at a derived depth, and it can; a second execution below (ray query, path tracing) is a second engine under one waist, which is the path round's own "the contract lives, the program is the fork".

## What changes in the path-kind picture
- **Position 6, sharpened.** "Placed ink takes the coverage lane on the object's z = 0 plane" becomes "placed ink is a chart layer in the face's look; chart kinds frame, face, uv, projection; the z = 0 plane is the first frame chart". The cut of the 88-byte mesh road stands.
- **The filler gains a caller and a constraint.** It must be callable as a function inside another program, coverage(uv, footprint) → alpha, with curve and band data bindable per chart, not only as its own draw. The packer's tolerance is a 2×2 footprint from the full chain, not a scalar: a face at a grazing angle has different tolerance along and across.
- **classify gains a caller.** The pick chain calls it with a uv from a ray hit; its slop is derived from the ray's footprint at the hit, in chart units.
- **The evaluator moves.** Position 8's fifth piece beside the sources becomes the executor at the centre; both chairs already named it on 2026-09-06, and 3D makes it unavoidable. The three doors stand; door two is the minimum.
- **Sweep generalises.** The envelope builder is sweep in a plane: a nib along a path is Minkowski along a curve, and extrude, revolve and pipe are the same operation one dimension up. A curve in space with a view-facing nib is a path whose centerline is 3D: edges, hair, wires. The geometry piece is one piece with a dimension.
- **The path value becomes an input to 3D constructions.** Sketches, extrude and section-to-plan reference path values by identity; the answers the definer added (intersections, splits with id maps, length ↔ t) serve sketch solvers directly.
- **The path kind gains a source.** Section and silhouette emit path values derived from 3D; they must carry provenance (which face, which edge) or a BIM plan cannot be edited back, the definer's "source correspondence" one level up.
- **The sweater stays order.** Over and under in 2D is declared order; in a chart it is chart order; never depth. Depth is the 3D space's derived order and the two are not the same axis.
- **Precision is one fix.** Rebase around the camera before upload, found by the path round for the infinite canvas, is the same move the planet needs; one stage in the transform tree, both dimensions.

## The decision that remains, and the exact question still open
**[decision · 3D's output] Is render(space, camera, size) → surface an answer, or only the compositor's internal act?**
If it is an answer, shadows, probes, portals, thumbnails, bakes and a plan rendered to a page are all data above the waist, scheduled by declarations, and the region is its first caller. If it is internal, each of those is a native feature added one at a time. This is the one input-and-output decision that changes what the ceilings can be built from. Position 4 says answer; the cost is a surface budget, which the leases and rungs already are for one surface kind.
**[definition · the definer's chair] What is a chart?**
Where a flat frame on a 3D surface comes from and what it anchors to: a plane in the object's frame, a named face's parameter space, the mesh's uv atlas, a projector. This is the 3D twin of the path round's Position 7: a definitional fork at the seam, with a counterexample waiting (a stroke that crosses from one triangle to another on a curved mesh has no frame chart). Position 7 above takes frame first; the definer works the kinds and their identity.
**[open · Sid's feel] What does page zoom mean at a region: focal length, or a step into the room?**
Position 5 says focal length: an optical zoom, a magnified crop, nothing stored changes, and entering is the region camera's own gesture. The other reading, zoom beyond the rect becomes a dolly, is a declared mapping on the portal. Which is the default is what a person feels the first time they wheel over a region, and only Sid can close it. Asked once, here. The bench beside this page lets him wheel over one and feel both.
Landed under `docs/below-the-waist/3d/`: this page and its `.md` twin, `fact-base-1.md` with the three hunter reports and this session's derivations, and `bench-0/`, the seam bench. This page is published at claude.ai/code/artifact/ced3dcf5-d276-46ab-8456-3a1be0141bc5 and the bench at claude.ai/code/artifact/5fd52994-becf-4840-871e-0453bcd7bc3e; to republish either from a fresh session, read it with that url first, then publish with `url` set and the file as `file_path`, or a second artifact is made. Beside them, unfolded and not read past its first paragraph: the other lane's `3d-kind-working-model.md`. The path kind's directory was being edited by its own chairs while this was written (attack 1 folded, `d202c90`, `4b8e986`); nothing here changes it, and the "what changes in the path-kind picture" list above is for its composer to fold or refuse. Positions are marked and the verdicts are Sid's.

