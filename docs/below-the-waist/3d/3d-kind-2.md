Markdown twin of 3d-kind-2.html, generated from the page; the three drawings live in the html. Landed 2026-09-06 (session 2).

 Softland · client · 3D kind · 2026-09-06 · session 2 · read against the path picture 

# The 3D Kind

What a 3D thing is and what it is made of, read at the level of inputs and outputs: what the kind takes today, what comes out, and what must exist as code below the waist so that CAD, film tools, open worlds, BIM, planets and scans can be built above it as data, in one answer with the 2D picture. Exploration phase: framing and the whole picture first; details are a fix list.

 **Fence** src/app/client/ only · docs: the path-kind directory and the ceilings starter · vision/LOG.md for what Softland is for **CHECKED** this session read the lines (fact-base-2.md) · **DERIVED** from checked lines · **FIELD** from the literature, unverified here · **POSITION** ours, awaiting Sid 

## What is decided and what is proposed

Sid's words with dates, kept apart from positions so nothing here promotes or demotes them. Everything else on this page is a position.

**Ruled · 2026-07-26 · LOG**

"I think we should be able to talk about the space as the entity .... it should be softland code -> space type -> space -> component. any block component does live in space and space has its own meaning of click drag select etc ... so now this is more world within a world" and "i do want to keep the zoom level will make something happen".

**Ruled · 2026-08-02 · LOG**

Ink truth is the gesture; the outline is a derivation ("Decision 2: yes A"). The envelope's exclusions are "for now, never never": "i will soon ask for sculpting and node authoring".

**Wanted · 2026-08-10 · LOG**

"imagine a house inspector with their notebook ... the driver in this workflow is biderictional like you can control the space and talk about it both from 2d or from the 3d pov". "this will all pass through softland so that its all tagged and get all the properties of versioning, permissions, relations etc. etc. the data part not each individual frame I mean". "OH NO WHERE???? I DON"T LIKE OPTIMISIC UPDATES".

**Asked · 2026-08-26 · recall bc83c63d, b912e83f**

"is this how it should be? ... what is the 3d facet what all is it made up of". "hover is one thing that should also be because from a user hover is saying this is where my focus on context is ... click in, "you hit this" out ... a model that is elegant and plays with differential reactive data and mouse actions over data so we can have fine grained reactivity".

**Held open · 2026-08-28 · LOG**

"entities wear 3D facets, or the land contains 3D windows?"

**Law · 2026-09-02 · LOG**

"we have build engine and some primitives that get hardcoded and we leave other heigher level layers for ecs and call that above the waist. Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks".

**The starter · 2026-09-06**

"The store that merges is above this kind; what the kind owes it is stable identity and fine-grained diffs." "the hit answer comes at pointer rate for the whole page, nested regions included, and says only what changed." The working basis, adopted to be attacked, is the field's scene (objects, transforms, geometry, materials, lights, a camera; rendering as projection, visibility, shading) and one composition primitive (render a scene of either kind into a surface; a surface is paint in the other).

**Said · 2026-09-06 about 02:00 · path-kind/HANDOFF-9 §6, after this session's bench was built**

On tools as data: "Yeah i know thats why softland exists those tools are dinasarus". On one executor over code nodes, the Houdini precedent: "ooooooo nice but one executor idk seems like the constraints and hard part would be getting them in line throughput seems small....". On clips, blends and layer stacks in one pipeline: "We should be ahead of everyone or at the very least adopt the best in field."

**Not decided**

Positions 1 to 9 at the end. Position 9, whether a construction is an input the kind takes and in what language, is the fork Sid's word closes. The exact open question, the identity of a derived part, is named once at the end.

## [POSITION] The 3D kind as it should be

Four nouns, one seam, one waist with two floors. A **space** is the entity; it comes in two kinds, planar and spatial. **Things** live in spaces. A **view** is a camera onto a space and belongs to a person. A **portal** is a thing in one space that shows a view of another. Every seam between spaces is a pair of maps: the picture goes up, the pointer goes down. Today's Region3D is a portal on the page with the whole world stored inside it.

[FIGURE: see 3d-kind-2.html for the drawing]

Caption: **Should be.** Above the line, data: two kinds of space, things in them, views owned by people, portals as the one nesting primitive in both directions. The seam is a pair of maps that must agree. Below the line, four services every kind shares, then two floors. The spatial floor's first box is not one type but one answer set over several stored representations; its packer runs per view with a budget; its filler renders a view, not a region.

## The story, read off the picture

- A 3D thing lives in a space, and the space is the entity: it has an id, a unit, things, and its own meaning of click and drag. The window that shows it is not the space.

- Spaces come in two kinds. In a planar space things have a paint order; the page is one. In a spatial space things have positions and depth, and their relations to each other (occlusion, shadow, contact) live there.

- A thing in a spatial space is a placement, a shape and a material. The shape is stored as its source made it: a mesh from an artist, an exact solid from a feature history, a formula, a volume from a scanner, points or splats from a capture.

- There is no single 3D "path". There is one set of questions every shape must answer: where does this ray hit you, is this point inside, what are your bounds, give me your surface at this tolerance, cut yourself with this plane. Converting between representations is work below the waist; which representation a thing uses is data above it.

- Triangles are the GPU's format. Storing a sphere as 32 by 16 quads is the 3D version of storing a curve as the pen's samples read as corners. Store what the source made; make triangles at the camera's tolerance, per view.

- A view is a camera onto a space, and it belongs to a person. Orbiting is not an edit. Today's session row already keeps the view and the moved transforms outside the region row, which is the split made half-way.

- A portal is a thing in one space that shows a view of another space, of either kind. Softland already has this noun. Today's Region3D is a portal on the page with the whole world stored inside it, so two windows onto one world are impossible.

- Every seam has two maps. The picture goes up: render the view into a surface, paint the surface where the portal sits. The pointer goes down: map the point through the portal into a ray, ask the space. The two must agree: what you see is what you hit.

- A face in a spatial space can carry a planar space. Ink on a face is the path kind drawing in that planar space, in real units, millimetres on the face. That is 2D inside 3D, and it is the same path kind with no second contract.

- The hit answer is an address, not a hit: page, portal, space, thing, part, planar space, stroke, with coordinates at every level. It is what "point and say" hands an agent.

- Hover is a value computed from the pointer and the data, not from pointer events. When an object moves in front of the stroke, the hover changes while the pointer holds still. The bench shows it.

- A screenshot looks the same and is not the same. It holds pixels and no reference; the portal holds a reference and no pixels. The picture is never data, the way triangles at a zoom band were never the stroke.

- Sort every value by when it changes: per edit (things), per view (the camera, which things at what detail), per frame (the picture), and per time (animation, simulation). Then place it. Per scale, the 2D axis's middle row, is the planar case of per view.

- One waist. Identity and diffs, the seam walker, the compositor and the evaluator are shared by both kinds. Under them two floors: the planar floor the path round drew, and a spatial floor whose shapes, packer and renderer are a different language.

- The evaluator the path round found as a fifth piece is, in 3D, the piece that decides the ceilings. A feature history, an operator graph, a shader graph and a mate are constructions; they are data only if code below can run them. That is the fork on this page.

## Input and output, spelled out

The value the kind takes and the things it gives. This is what every session on the 3D kind should be talking about. It is written to compose with the path contract: the path is the planar shape, and a planar surface in a spatial space is where the two meet.

### In: spaces, things, views, portals, constructions

```
space
 id, revision, kind planar | spatial, unit (px | mm | m), up-axis, frame kind
 things* {id → thing}, relations (parent, prototype, hosts …)

thing
 id, kind, provenance, parent?, transform (TRS; a frame kind for
   non-affine cases such as geodetic)
 shape one of:
   mesh (polygons | subdivision cage)          the artist's truth
   brep (faces, edges, vertices)                from a construction
   implicit (f(p) ≤ 0, CSG)                     a formula
   volume (a grid)  ·  points  ·  splats        captures
   planar-surface (frame, size, unit scale,
                   planar space ref)            where 2D lives in 3D
   portal (view ref, extent, recursion bound)   a window either way
   prototype ref                                an instance
 material (metallic-roughness | a shader construction), light?
 part ids stable: faces, edges, vertices are addressable

view
 space ref, camera {pose: pivot distance yaw pitch | free; lens}, time
 policy: tolerance, budget, display mode
 owner: a session, or shared by declaration
 (the viewport comes from the portal that shows the view)

portal   a thing in a planar OR spatial space: view ref, extent

construction   a saved program over values: sketch → extrude → fillet;
 an operator graph; a shader graph; mates and constraints; a section
 at height h. Data only if the evaluator can run it (Position 9).

identity   every thing and every addressable part has a stable id;
 a change is (thing id, component, value); nothing per view or per
 frame is ever in the value: no segment counts, no pictures, no
 tessellations, no tolerance
```

### Out: a surface per view, answers, and the walk

```
to the GPU (per view)
 the packed visible set at τ(view, budget):
   mesh → triangles + instance rows
   brep → tessellated at τ, edges and silhouettes exact
   implicit / volume → a raymarch program, or a derived mesh
   points / splats → view-sorted lists
 lights, shadow spaces, a sort order or an OIT policy
 planar surfaces as materials: the path filler runs on the face
 ⇒ one view render → a surface (colour, premultiplied; depth stays
   inside), leased by the compositor, worn visibly when degraded

to the CPU / other kinds (same representation, same tolerance)
 hit(ray, τ) → {thing, part, point, normal, t, correspondence}
 classify(p, τ) · bounds · distance(p)
 surface(τ) → a mesh with correspondence to source parts
 section(plane) → paths · silhouette(view) → paths   (2D values)
 boolean · offset · extrude(path) · fillet …  kernel answers, bought

the seam walk (shared with 2D)
 in: a page point or a ray; the tree of spaces, portals and views
 out: the address [space, thing, part, planar space, stroke …]
      with coordinates at every level, t, the surface's status,
      and the diff against the last answer: entered, left, still

to the store
 ids and diffs only. The picture is never handed up.
```

Two things in this block are not in the field's scene description and are where this model speaks: the planar surface (a 2D space attached to a face, with a unit) and the portal as a thing of either kind of space. Three tolerances, as in the path kind: the source's (in the record: a scan's resolution, a mesh's polygon count), a consumer's (a parameter of every answer), the device's (the packer's, per view). None in the value.

### When does a value change? The axis, with a fourth row

| Changes… | Values | Where it may live | Today (CHECKED) |
| --- | --- | --- | --- |
| per edit | things: transform, shape, material, parent, lights; a space's membership; the stroke's samples | CPU; stored; uploaded per thing | any static change is a full rebuild: every world triangle, one BVH, every instance row (scene.cljc:1088-1105); only a transform is maintained per object (:1113-1125) |
| per view | camera pose and lens; the visible set; each thing's tolerance; sort order; a planar surface's footprint (the path filler's τ on that face) | the packer's per-view cache, keyed on the view and the budget | the camera is rebuilt on a 1.12 bucket crossing of page zoom (renderer.cljs:1089-1095); draw order by object-origin distance (:894-913); placed ink packed at a fixed zoom 1.0 (on_plane.cljc:20), so it never re-lowers with distance or page zoom |
| per scale (a page view) | the portal's surface size; the lease | the compositor: quantised, capped, admitted by rungs | ceil(w·zoom·dpr) per rect, 256-px quanta, 4096 cap, divisors [1 2 4 8] (renderer.cljs:1053-1059; compositor.cljs:25-27; rungs.cljc:11) |
| per frame | the picture: shading, shadows, composite; the hover, re-walked when data or a view moved under a still pointer | GPU; the surface lease; the walker on the CPU | the interior re-encodes when scene, view, background or placements are dirty; the shadow pass on scene change (renderer.cljs:1134-1139); no hover exists; pick is meshes only (scene.cljc:965-982) |
| per time | animated attributes; simulation state; sim caches as derived values | the evaluator's clock; caches beside, never in the value | nothing |

## The hardest case, moment by moment

A pen stroke on a face of a box inside a 3D portal on a page. Zoom the page, orbit, hover, click; a sphere passes in front; a face of the box is a portal to another land; a screenshot of the portal sits beside it. Every moment below runs on bench 2 (https://claude.ai/code/artifact/32169816-ae1c-4bbb-ba07-a9171eea419a) (`bench-2/seam-bench.html`), whose panel prints the address and the diff; the numbers are its.

[FIGURE: see 3d-kind-2.html for the drawing]

Caption: **Both directions of the seam.** Down: the pointer becomes a ray in S, a point in Π, a point in L. Up: L's surface is paint on a face; S's surface is paint at R. The screenshot has the upward picture frozen and no downward path. The numbers are bench 2's at its default view.

**you draw** — Pen down over R. Each sample walks down: page point → R-local → ray from V → B's top face at t → Π's (u, v) in millimetres. The sample is appended to k with its width and its arc length. k is a path value in Π; it does not know it is on a box. Π knows it is on B's face +y with 1000 mm to the metre. per edit: k.samples +1, revision +1, one texel row uploaded.

**you zoom the page** — R is drawn 2.4× larger. Its surface is re-leased at 1024×832 instead of 448×384. V is unchanged: a window's content does not come closer when you zoom the page, it gets bigger. k is unchanged. What did change is the footprint of a millimetre of Π on the screen, 0.10 to 0.23 pixels, so the filler on the face runs at a new tolerance. per scale: the lease. per view (of the face): τ. per edit: nothing. Today: the page zoom crosses a 1.12 bucket, the camera is rebuilt, the whole interior re-encodes, and the placed ink stays at its fixed zoom 1.0 and shows its polygons (CHECKED).

**you orbit** — V's yaw and pitch change. V is the session's; the region row does not change. The visible set, each thing's tolerance, the sort order and Π's footprint change. The walker uses the new camera and the same data. per view. Today: the session row already carries the view (renderer.cljs:1087).

**you hover k** — The walk returns an address, not a hit: [page (283, 180) px · R (223, 110) in a 448×384 surface · S: ray hits at t = 3.23 m at world (0.00, 1.00, −0.20) · B face +y · Π (500, 302) mm, footprint 0.10 px/mm · k inside: segment 34, s = 673 mm, width 30 mm, 15 mm from the edge]. Six levels. One ray, one box test, one classify. The diff says entered k. Today: pick-region returns B and a triangle index; placed ink is not in the BVH; no map from a page point to a region point exists (CHECKED scene.cljc:965-982; harness/region.cljs:600).

**Q passes in front** — Q's transform changes while the pointer holds still. Hover is a derived value of (pointer, data, views), so it re-walks: the ray now hits Q at t = 2.26 before the face at 3.23. The diff says left k → entered Q (the pointer did not move; the data did). This is the fine-grained reactivity Sid named: a mouse action over data, recomputed when the data moves. It rules out any design where the hit is only read on pointer events, and it makes the CPU walker the definition; a GPU id buffer would have to be re-read every frame to match it.

**you click k** — The same address. What the click does is a reaction row above the waist: on click of a thing of kind path in a planar surface, select it, or say something about it. The address is the context "point and say" sends: S, B, face +y, k, 673 mm along.

**the portal on the face** — Hover B's front face: the walk enters T, maps the face point (0.50, 0.50) to L's (128, 128) px, and L's floor answers: circle c. [page R S B +z T L c]. Rendering went the other way first: L into its own surface, that surface as paint on B's face, S into V's surface, that surface as paint at R. If L held a portal back to S, the compositor bounds the recursion and wears the picture visibly past the bound; the data holds no bound.

**the screenshot beside it** — I looks the same at the moment it was taken. Hover it: [page · I pixel (224, 192) of 448×384 · no seam]. Orbit: I does not change. Q moves: I does not change. Zoom past its pixels: I blurs while R re-leases. Draw on I: the ink goes on the page. What must differ in the data: R is a reference and a view; I is bytes with a digest and, at best, a provenance line "screenshot of R at revision 1". The picture is never data. The 3D kind's triangles-at-a-zoom-band are the picture. 

### Where the names still hide unfinished work

- **Surface.** Colour only is enough for the picture and the seam does the rest, but the surface has a status (fresh, worn at a divisor, rejected, stale at a revision) that the page and the hit answer must be able to read; "worn visibly" is law. Who owns that status and how it reaches above the waist is not designed. Today the worn stripe is painted by the composite pipeline and known to nothing else (CHECKED renderer.cljs:280-289).

- **Region.** Dies as a noun. It splits into a space, a view, a portal and a lease. Its `:extent` (width, height, depth) is required, validated, part of identity, and read by nothing (CHECKED fact-base-2 Part 1). If it is the space's bounds it belongs to the space; if it is the portal's clip volume it belongs to the portal. Named, not decided.

- **Portal.** The viewport comes from the portal, so a view is not fully independent of the portal that shows it: two portals of different aspect onto "one view" are two views. The recursion bound is compositor policy. The map from a planar point into a spatial view (a rect on a page shows which pixels of the view) is the aspect rule; it is what camera-matrices takes as an argument today (CHECKED scene.cljc:744).

- **Scene.** Dies as a noun; it is the space's content. "A whole scene as a source of an object" is a reference: a thing whose shape is the content of another space placed here. That needs instancing as a relation (thing → prototype) and an address that names the instance. Neither exists.

- **Face.** A planar surface on a plane is the affine case. Ink on a curved face is a planar space attached through a (u, v) → point map, the surface's parameterisation. The filler still works, it uses screen derivatives; or the ink is baked to a texture on the face, which is what games do (FIELD). Two roads, not chosen. And a face of a constructed solid needs an identity that survives re-running the construction: the open question at the end.

## What the 3D kind is today

One notch below the folder map. Each box is a piece with what goes in, what comes out, and why it has that shape. Arrows are real calls. Read against the picture above: the region row is a portal with the world stored inside it; the view and the moved transforms already leak out of the row into the session; the pointer map down exists for meshes only; the picture map up is one quad through the 2D group transform.

[FIGURE: see 3d-kind-2.html for the drawing]

Caption: **Today.** The region row goes in; a colour quad in the page's paint order and a mesh hit come out. Three pieces are already the right shape at the wrong scope: the CPU oracle for hits, the view in the session, the leased surface admitted by budget. Two are the leak: the world inside the window, and render resolution inside the value.

## The attack on the working basis, claim by claim

Sid's working basis, one claim at a time. HOLDS means it is the answer for this kind. DEFAULT means it works until a named condition, and the condition is stated. SHORT means it cannot express something Sid has already said he wants.

| Claim | Verdict | Why, and the condition |
| --- | --- | --- |
| A 3D scene is objects with transforms, geometry, materials, lights and a camera; rendering is projection, visibility and shading. | HOLDS, with two edits | As the output language of the raster model it has no rival; the path tracer takes the same description, which is why the description is the settled part (FIELD). Two edits. "Geometry" is several representations behind one answer set, not one type. "A camera" belongs to the view, not the scene: a camera can be placed in a space as a thing ("a good view", data), but the camera that renders is a person's. Today the camera comes from the session first and the row second (CHECKED renderer.cljs:1087). |
| Interchange settled, execution contested; the same split as the 2D imaging model. | HOLDS | USD, glTF, IFC, 3D Tiles, VDB are the settled forms; renderers differ by workload (FIELD). The consequence for Softland: shape the data above the waist like USD's composition, prims with attributes at stable paths, opinions in layers resolved by declared strength, references and instancing. That is the field's answer to many people editing one scene without merge deadlocks: a layer stack is a merge by strength, not by time. Where USD is silent, the page, the portal, hover, the planar surface, is where this page speaks. |
| One composition primitive joins the two models: render a scene of either into a surface, and a surface is paint in the other, in both directions and recursively. | DEFAULT for the picture, SHORT for the hit and the diff | It states the upward map only. The seam is a pair, and the downward map (a point through the portal into a ray, a face point into a planar space) is what "click in, you hit this out" needs; the bench's screenshot shows a surface with the upward half and no downward half. Also "a surface is paint" holds only while the portal is a window: if a 3D thing must occlude page text by depth, the surface would need depth and the page a depth policy. Position 4 keeps it paint. |
| The 2D contract ends at a region drawn with per-pixel coverage; whether 3D begins there is open. | answered: it does not | 3D begins one level up, at the view render: (view, packed visible set, lights) → a surface. Coverage of a region is one filler; a view render is another; both end at a surface the compositor owns. The two meet at the compositor (surfaces) and at the walker (addresses), not at the coverage filler. The one place they touch below that: on a face, the path filler runs inside the view render as the face's material (the path page's Position 6). |
| Every session that started from rendering made a definitional error; start from what the object must mean. | HOLDS, with one exception named | Honoured here: the four nouns come from what a space, a view and a hit must mean under orbit, edit, share and travel. The exception: splats and radiance fields have no geometry beneath them; their meaning is what they look like from any view, and "you hit this" for them is a policy over opacity along the ray (FIELD). The rule bends there, and the answer set (hit with a policy parameter) absorbs it. |
| The triangle mesh is the 3D leak, the GPU's format standing in for the object. | half right, as with the polyline | A mesh is the artist's truth and a scan's reconstruction; as a source it is legitimate, like the polyline was a legitimate path. The leak is three other things: tessellation resolution stored in the value (segment counts, CHECKED component.cljc:53-60), a derived mesh standing in for a source that could be re-tessellated at the camera's tolerance (a sphere, a solid, a cage), and normals as a required attribute of the value (CHECKED :373-384). And the larger leak is the picture: the region row must never hold it, and today it does not; what it holds instead is the world. |

## Do the path round's pieces transfer?

Four pieces of code and one axis. Each transfers as a role and changes size; two break in a way that names the difference between the floors.

| Path kind | Spatial floor | Where it breaks |
| --- | --- | --- |
| path type | Not one type: an interface. Shape representations (mesh, subdivision cage, brep, implicit, volume, points, splats), each stored as its source made it, each answering one set: hit, classify, bounds, surface(τ), section, silhouette, distance, correspondence. The path is the planar member of the same interface. | The single stored representation. In 2D everything compiles to the path cheaply enough; in 3D conversion is lossy and is the work (brep → mesh at τ; mesh → brep is research; volume → isosurface; points → mesh). So the value is per representation and the answers are one. |
| geometry (answers) | The same answers lifted a dimension, plus kernel operations the field buys, not writes: boolean, offset (shell), fillet, extrude(path). Section and silhouette return path values. Correspondence maps say which source part made which derived part. | The answers include libraries the size of the whole client (a brep kernel). Bought behind the interface; the choice is a dependency decision (OpenCascade, Manifold for mesh booleans; FIELD). |
| packer (per scale) | Per view, with a budget: tessellate at the view's tolerance per thing, cull, select detail, instance, stream, sort. The image kind's residency counter is the seed of this piece, grown up. | Budget and residency. 2D had no residency question except images; 3D's packer is a scheduler that must never make the frame wait (the render-seam law) and must report worn state. This is the ceilings' home: LOD trees, tile trees, octrees are all this piece. |
| filler (per region) | Per view: (view, packed set, lights) → a surface. Depth, shading, shadows, a transparency policy, several programs (raster, raymarch, splat), planar surfaces as materials, a CPU oracle as the definition of the image. | "One resolve per region" becomes "one render per view", because a mark's pixels depend on other marks. The filler is no longer per item. Shading as data (shader graphs) needs a compiler below the waist: the evaluator at another size. |
| evaluator (Position 8) | The piece that defines the ceilings: feature histories, operator graphs, shader graphs, rigs, constraint systems, simulations with a clock. Shared with 2D. | Scale. An interpreted graph over a million points per frame does not hit frame rate; the field compiles (FIELD). The evaluator needs a compiled tier, and its language is the fork (Position 9). |
| axis: per edit · per scale · per frame | per edit · per view · per frame · per time | Per scale is the planar case of per view (a page has one scale; a spatial view has distance and angle per thing). Per time is new: values change without an edit, so the evaluator needs a clock and caches are derived, never truth, except a capture that cannot be re-run. |

## What 2D never had: code below, or data above

| Thing | Forces code below the waist | Stays data above |
| --- | --- | --- |
| A mark's pixels depend on other marks (shadows, occlusion, transparency order) | The view render: depth, shadow passes, a transparency policy (per-object sorting by origin cannot resolve intersections, CHECKED renderer.cljs:894-899; OIT is the field's fix, FIELD). The picture's dirtiness is separate from the data's diff: one move dirties the view's picture, the data diff is one row. | Which lights, which materials, shadow flags, the policy chosen per view. |
| Many shape representations; converting is the work | The answer set per representation; conversions as operations the evaluator calls. | Which representation a thing uses; the construction that derived one from another (a mesh from a scan at tolerance τ). |
| 2D and 3D nest both ways; 2D derived from 3D (sections, silhouettes, plans) | The seam walker; section(plane) and silhouette(view) as answers that return paths; the path filler on faces; the planar surface as a shape kind. | Portals, planar surfaces, the construction "plan at height h" whose result is a planar space of paths; BIM's drawings are exactly this, re-run on edit. |
| Ranges and budgets past float32 and per-frame time | f64 rebased around the camera before upload (never done today, CHECKED fact-base-8 Part 4); non-affine frame kinds (geodetic); residency by budget with worn status; N views per frame (stereo: the head is the camera, the hands are rays into the same walker). | Units, frame kinds, the budget policy per view, view pairs. |
| Time and simulation | A clock in the evaluator; solver operations as a library; per-time caches as derived values. | The animation or simulation as a construction; a cache that cannot be reproduced is a source, like a scan. |
| A constraint history that must survive an edit | The executor; kernel operations; an id scheme for derived parts with correspondence (the open question). | The history, the mates, the sketch constraints. |
| Units, tolerances and frames as meaning | Tolerance as a parameter of every answer; frame kinds as code. | A space's unit and up-axis; a planar surface's scale (today's fixed 1.0 is this, hard-coded, CHECKED on_plane.cljc:20); a view's tolerance policy. |
| From the ceilings: instancing, streaming, many lights, stereo | Instancing in the packer; tile and octree residency; clustered lighting; two views per frame. | Prototype relations, tile references, light lists, the view pair. |

## The ceilings through the model

For each ceiling: what is data above, what must be code below, and the honest step where the model would crack, the way the 2D round found each ceiling broke at its own hardest step. The product mechanics are general knowledge of those tools (FIELD).

| Ceiling | Data above | Code below | Where it cracks |
| --- | --- | --- | --- |
| Parametric CAD | Sketches as path values with constraints; a feature history as a construction; assemblies as references with mates; drawings as planar spaces derived by section and projection. | A brep kernel behind the answer set (bought); a sketch constraint solver and a mate solver as operations; exact edges and silhouettes at every zoom (answers, not tessellation); the executor. | Derived-part identity: the fillet refers to a face that the re-run extrude must produce again as the same face (the open question). Fillet robustness is the kernel's. A hundred thousand parts is the residency ceiling. |
| Procedural film tools | Operator graphs over geometry values; rigs as deformer constructions; shader graphs; sim caches per time; two views (real-time, path traced). | The executor with a compiled tier; subdivision, deformers, solvers as a library; a shader compiler; a path tracer as an out-of-loop rung (his home rig; the generated-solidity tag). | The evaluator's speed: a graph over a million points per frame, interpreted, does not hit frame rate. Sim determinism across collaborators is a fork (lockstep or cached). |
| Real-time open world | Instances (thing → prototype), detail declarations, lights, materials, physics and animation state machines with a clock. | Instancing, streaming residency with budgets, culling, clustered lighting, shadows, global illumination as a view-render policy, post passes in the compositor, two views per frame. | The frame planner. If everything must be scheduled by one budget, the page becomes content of that planner: this is where one execution waist may become two (Position 6's condition). GI and reflections are "pixels depend on everything" at scale; nothing above the waist can add them. |
| BIM | Typed things with relations that propagate (a wall hosts a door); disciplines as layers; plans, sections and schedules as planar spaces and text derived live. | Section and projection answers; the propagation executor driven by the diff (the reactive graph is the executor's scheduler); layer composition by strength. | Propagation cost: one wall move re-runs every dependent construction. The fine-grained diff must reach the executor as dependency invalidation, or every edit is a full re-run, today's :full. |
| Planet-scale geospatial | Tiles as references with per-tile frames; imagery as image things on terrain surfaces; time-dynamic attributes. | Geodetic frame kinds; f64 rebased around the camera; tile-tree residency at view-dependent detail; a depth policy (reversed or logarithmic depth). | Precision through the seam: a page portal into a planet at street level needs the whole chain in f64 down to the view; the camera and group buffers are f32 today (CHECKED). A hit at planet scale is a tile plus a feature id. |
| Fields and scans | Volumes, point sets and splats as representations; isosurface constructions with a threshold. | Raymarch and slice programs in the view render; octree residency; marching cubes as an operation; splat sorting per view. | "You hit this" has no surface: the hit for a volume or a splat set is a policy (first voxel above a threshold, accumulated opacity) over a ray-integration answer. The object-meaning-first rule bends here. |

## How the best team in the field would define it, and what they would name

**The problem as they would state it.** You are building a composed scene description with stable paths and layered opinions, a change tracker that turns edits into per-thing dirty bits, render delegates behind one interface (a real-time raster, an offline path tracer, a CPU oracle), a query service for picking, and a pipeline of scene filters, pure functions over scene data, for procedural generation, instancing, detail and derivation (FIELD: USD and Hydra are this shape; Hydra 2.0's scene indices are the filters). Then they would add what the field does not have and this page needs: the planar space, the portal in both directions, the walker, and hover as a derived value. The trade-offs they would name, FIELD unless marked:

- **Buy the kernel.** Nobody writes a brep kernel; the choice is which one runs in the browser and at what size. Same for mesh booleans and sparse volumes.

- **Instancing versus uniqueness.** A million things share one shape and differ by placement; an instance has an id and no data of its own; moving the prototype is one edit that dirties a million pictures; the hit names an instance path.

- **The tolerance is per view and per thing.** A screen-space error metric with hysteresis so detail does not pop. The packer is asynchronous: the frame never waits for data; worn is the honest state while it streams.

- **Transparency ordering.** Per-object sorting fails at intersections (today's docstring says so, CHECKED). Choose an order-independent policy up front; it cannot be retrofitted.

- **Shading as data has a compiler.** Material graphs become programs; permutations explode; an uber-shader or a cached compiler is the choice.

- **The hit must be CPU-defined.** Two collaborators must agree on what was hit; GPU id buffers differ by driver. Keep the oracle as the definition and compare the fast path to it at a tolerance, the same rule the path round set for membership and coverage.

- **Precision.** Camera-relative rendering and per-tile frames; never a single f32 world.

- **Time and determinism.** Time samples on attributes; a view has a time; simulation for collaboration is lockstep or cached, a fork to name early.

- **Sessions are a layer.** The per-session view and transform overlays that exist today are the weakest layer of a layer stack; formalise them as one, and undo and preview fall out.

- **Conventions.** Up axis (Y here, Z in CAD and BIM), units, handedness: a space declares them and imports convert; a silent mismatch is the classic bug.

- **Stereo and hands.** Two views per frame of one space, and a ray as the pointer: the walker takes a ray, and the page point is the orthographic special case.

- **Clips, blends and layer stacks in one pipeline.** Vello and Skia carry them as compositor passes; the tree has a rectangle scissor (CHECKED compositor.cljs:612-629). Sid's word the same night: "adopt the best in field". They land in the shared compositor, once, for both floors.

- **Colour management.** Linear premultiplied inside, sRGB at present, a tone map per view; today's chain does this (CHECKED scene.cljc:21, compositor.cljs:644-671) and the page's colour space must be declared alongside.

## Sid's questions, one by one

**What is the general 3D object, and what are its sources? Is the region the thing?**

A placed shape with a material in a spatial space, where the shape is stored as its source made it and every representation answers one set of questions. Sources sort as: a modelled mesh is a mesh (an artist's polygons or a subdivision cage, the cage is the source); a formula solid splits into an implicit (f(p) ≤ 0, CSG) and a constructed brep (sketch, extrude); a scan is points, a splat cloud is splats; a 2D shape given depth is a construction over a path value (the seam with the path kind: the sketch is a path); a whole scene is not an object, it is a space, and "used as an object" it is a reference placed in another space. The region is neither the thing nor the container: it is a window, a portal with a view and a lease. The space is the container. The things are in it.

**Is the triangle mesh the 3D leak, and what is the analogue of storing the curve?**

The triangle is the leak only when it stands in for something else. Storing the representation the source made, and tessellating per view at the camera's tolerance, is the analogue of storing the curve; today's segment counts in the value and normals as a required attribute are the analogue of the zoom bands in the cache key. The picture is the larger analogue: it is never data.

**Do the four pieces and the axis transfer?**

As roles, yes, each at a larger size; the table above. Two break in a way that defines the spatial floor: there is no single stored representation, and the filler renders a view, not a region. The axis gains a row, per time, and renames per scale to per view.

**Containment: 3D in 2D, 2D in 3D, or nested spaces?**

Nested spaces of both kinds, with one primitive, the portal, in both directions; the page root with windows is the default containment and travel re-roots. What a 3D thing must continue to mean: after orbit, the same thing, because the view is not the value; after moving an object, the same thing with one changed row, and every picture that depends on it dirty; after placing ink on a face, a planar space attached to that face with the ink in it, so the ink follows the face; after zooming the page, the same view at more pixels; after sharing, the same space with your view and their view separate, unless a view is shared on purpose.

**Two cameras: what composes them, and what happens when the page's zoom crosses into a region?**

Function composition: page affine ∘ portal rect ∘ viewport ∘ projection ∘ view ∘ model. The page's zoom sets the portal's pixel size, per scale; the region's camera sets the projection, per view; they never mix. Crossing is not a zoom: it is travel, a change of root, and it is a declared reaction on a band-crossing event, which is what "the zoom level will make something happen" asked for. Below the waist that needs one thing: either kind of space can be the root.

**Picking and hover through the seam**

The walk in the moments above: page point → portal → ray in S → nearest hit among shapes → if the hit part carries a planar surface, its point → the path kind's classify at the pointer's footprint → an address with coordinates at every level and the diff. At pointer rate: one ray, one BVH walk per space on the path, one classify. Says only what changed: entered, left, still. Recomputed when data or a view moves under a still pointer.

**A surface is paint: colour only, or depth and identity too, and at whose resolution? What is the unit of change?**

Colour only, premultiplied, at the portal's projected size times DPR, quantised and admitted by the compositor's budget, worn visibly. Depth and identity are answered from data by the walker, not read from the surface, and that is what lets a portal be a plain paint item in the page's order. The unit of change is (thing id, component, value); for derived parts, an id from the construction. The picture's dirtiness is a separate, coarser thing.

**Am I reasoning from what region3d does today or from how it should be?**

This page was derived from what a space, a view, a portal and a hit must mean under orbit, edit, share and travel, in Sid's own cases (the house inspector, point and say, world within a world), then checked against the tree. Three things in the tree already have the right shape at the wrong scope: the CPU oracle for hits, the view and transforms in the session, the leased surface admitted by budget. Two are the leak: the world inside the window, and render resolution inside the value. "That is what region3d does today" is evidence of feasibility, never the reason for the design.

## Positions, each with the condition under which it stops being the answer

**[position 1] the space is the entity; the region splits into four**

Space (id, unit, things, relations), view (a camera; a session's), portal (a thing on a page or a face: a view reference and an extent), surface (a lease; derived, worn visibly). Supported by Sid's words (space as entity, the bidirectional house inspector, hover and click, sharing) and by the code's own session overlays for view and transforms, which are this split made half-way. Stops being the answer: never for the data. If the product only ever shows one window per world, the four nouns are still right; the split costs one more row.

**[position 2] the general 3D object is a placed shape behind one answer set, with several stored representations**

Store what the source made; tessellate per view; the answers are one interface. Stops being the answer: if only meshes and subdivision surfaces ever matter (the film and game road), a mesh-only floor is the workable default and CAD, volumes and captures enter as lossy imports; then the CAD, BIM and scan ceilings are hacked through, not built through. Sid's "sculpting and node authoring" soon, and CAD on the ceilings list, say several.

**[position 3] the portal is Softland's one nesting primitive, and the seam is a pair of maps**

Picture up, pointer down, in both directions and recursively, agreeing at a declared tolerance: what you see is what you hit. Stops being the answer: no condition found. The unfinished work is named: the recursion bound and the surface's status.

**[position 4] the page stays planar with a paint order; 3D relations live inside one spatial space; the surface is colour only**

"Entities wear 3D facets" and "the land contains 3D windows" are one model: a facet is a portal framed on one thing in a shared space. Two 3D things on one page that should occlude each other are two things in one space with one portal, or two portals into it. Stops being the answer: if Sid wants a 3D thing to occlude page text by depth, sticking out of its window, the page needs a depth policy and the surface needs depth. Named, not taken.

**[position 5] the root is any space; a window is the default containment; travel re-roots on a declared reaction**

The house inspector walks in; the page becomes a window or a HUD. Stops being the answer: engine cost. A spatial root means text and ink draw on planes everywhere and UI lives in space; until that is built, the page root with windows is the workable default, and nothing in the data changes when it arrives.

**[position 6] one waist, two floors**

One data model for both kinds; four shared services (identity and diffs, the walker, the compositor, the evaluator); a planar floor and a spatial floor whose values and fillers differ. Stops being the answer: at the residency ceiling. If one frame planner must schedule every draw by budget, the page becomes content of the spatial engine at execution time, 2D lives in 3D below the waist, and there are two engines under one data model. The compositor is the meeting point either way, so the fork can wait.

**[position 7] the hit is CPU-defined and hover is a derived value**

The walker on the CPU is the definition of "you hit this"; a GPU id buffer, if ever needed, is an approximation compared at a tolerance. Hover is f(pointer, data, views), recomputed when any of the three changes, and reports only the diff. Stops being the answer: if pointer-rate hover over millions of instances cannot be served by a BVH per space, the id buffer becomes the fast path, re-read per frame, still compared to the definition.

**[position 8] views belong to people; cameras in a space are things**

A camera placed in a space is data ("a good view", a saved shot); the view that renders is a session's, shared only by declaration. Orbiting is never an edit. Stops being the answer: no condition found.

**[position 9 · the fork] constructions are an input the kind takes, and the evaluator's language is Sid's to choose**

A feature history, an operator graph, a shader graph, a mate, a section at height h: each is a saved program over values, data only if code below runs it. The path round's Position 8 named three doors and said the choice would close when brushes or shape formulas were wanted as data. 3D forces it now. The doors: (a) a fixed menu of operations per family, every new behaviour new code; (b) a node vocabulary as data with an executor, the shape of USD's scene filters, Houdini and BIM; (c) code as data, Clojure forms run at runtime. Position: (b) as the data shape with (c) as the escape hatch, because otherwise CAD histories, operator graphs, shader graphs and constraint systems are four separate languages. Sid's doubt of the same night, "one executor idk seems like the constraints and hard part would be getting them in line throughput seems small", sharpens it: one *language* for constructions, several *runners* behind it. Constraints are not the executor's job but operations it calls (a sketch solver, a mate solver, bought like the kernel); throughput is a compiled tier for what runs per frame or per time (Houdini compiles its expression language; FIELD), with the interpreted tier for editing. What stays one is the data shape the store merges and the address the walker returns. This is a substrate decision, a future-binding noun, and Sid's alone; asked once, here.

## What changes in the path-kind picture

- **The path value gains a home.** A path lives in a planar space: the page, or a planar surface on a face. Nothing in the value changes. The planar surface carries the unit, so the stroke width's unit "local" means the surface's unit, which on a face is millimetres.

- **The filler runs under a projection.** Position 6 of the path page stands: the coverage filler on a face is the face's material inside the view render; it takes the face's frame and the per-view footprint as its tolerance. The bench draws the swept nib this way on the face; the placement zoom of 1.0 is the sixth place that decides how big a pixel is, and it dies.

- **One new consumer, one new producer.** The walker calls classify(point, slop) with a slop derived from the pointer's footprint on the face, not a constant. Section(plane) and silhouette(view) of spatial shapes return path values, and extrude(path) takes one: the path is the shared 2D value across both floors.

- **The evaluator is promoted.** Position 8 of the path page is forced to a decision by 3D, not by brushes, and the recommendation moves toward the general door. Everything else on the path page holds.

- **The axis is renamed, not changed.** Per scale is per view's planar case; the path page's rows stand.

- **The precision line hardens.** Rebase around the camera before upload was a fix-list item for ink; for a planet it is the design.

## What decision remains, and the exact question still open

**The decision that remains about 3D's input and output** is Position 9: whether a construction (a feature history, an operator graph, a shader graph, a mate) is a value the kind takes, and in which language the evaluator below runs it. Everything else on this page is a position with an exit that later sessions can test on a bench; this one binds the substrate and is Sid's.

**The exact open question** is the identity of a derived part. Ink on face F of a solid made by a construction: edit the sketch, the extrude re-runs, and something must make the new F the same F, or the ink and every mate, fillet and portal that named F are orphaned. For a hand-authored mesh F is an index that lasts until the mesh is edited. For a brep from a construction F must be named by the construction (step id, source part, role) with a correspondence map; the field calls this topological naming and every kernel solves it by heuristic (FIELD). The alternative is to attach the ink to the construction step and a (u, v) frame rather than to a face id. Which it is decides whether ink on a face survives a parametric edit, which is the hardest step of the CAD and BIM ceilings, and it sits exactly at the seam between the store's identity contract and the executor. It is the crack the 2D round would have found late. No session has worked it, and the definer's chair should take it first.

## Landed

Under `docs/below-the-waist/3d/`: this page and its `.md` twin (`3d-kind-2`), `fact-base-2.md` with every anchor, `bench-2/seam-bench.html` (live at the Seam Bench (https://claude.ai/code/artifact/32169816-ae1c-4bbb-ba07-a9171eea419a)) with its handover, and `HANDOFF-2.md`. Perspectives stay separate: the sibling sessions' pages in the same directory were not read while this was written.

Softland · 3D kind · session 2 · 2026-09-06 · Positions are marked; the verdicts are Sid's. Every CHECKED line is anchored in `fact-base-2.md`. 
