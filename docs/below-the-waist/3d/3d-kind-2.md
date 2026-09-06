Markdown twin of 3d-kind-2.html, generated from the page by twin-2.py; the drawings live in the html. session 2, session 1's line folded in and closed

Softland · client · 3D kind · 2026-09-06 · session 2, folded by its successor the same night · read against the path picture, the siblings and the reviews

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

Positions 1 to 10 at the end. Position 9, whether a construction is an input the kind takes and in what language, is the fork Sid's word closes. The exact open question, the identity of a derived part, was worked on the bench in the fold and is Position 10; what it did not reach is named once at the end.

## [position] The 3D kind as it should be

Four nouns, one seam law crossed three ways, one waist with two floors of libraries. A **space** is the entity; it comes in two kinds, planar and spatial. **Things** live in spaces, each a placement of a definition. A **view** is a camera onto a space and belongs to a person. A **portal** is a thing in one space that shows a view of another. Every seam between spaces is a pair of maps: the picture goes up, the pointer goes down. The seam is crossed three ways, and each crossing carries both maps: a portal shows another space through a view; an **attachment** puts a planar space on a face by a saved rule; a derivation cuts new geometry out of a space with its provenance. Today's Region3D is a portal on the page with the whole world stored inside it.

[FIGURE: see the html for the drawing]

Caption: **Should be.** Above the line, data: two kinds of space, things in them, views owned by people, and the three crossings of the seam (portals, attachments, derivations), each a pair of maps that must agree. Below the line, four services every kind shares, then two floors of libraries. The spatial floor's first box is not one type but one question protocol over several stored representations, each declaring which answers it gives; its packer runs per view with a budget; its filler renders a view, not a region.

## The story, read off the picture

1. A 3D thing lives in a space, and the space is the entity: it has an id, a unit, things, and its own meaning of click and drag. The window that shows it is not the space.

2. Spaces come in two kinds. In a planar space things have a paint order; the page is one. In a spatial space things have positions and depth, and their relations to each other (occlusion, shadow, contact) live there. What makes a space one or the other is whether front-to-back is declared or derived from the view (session 1's order rule, kept as the criterion; the kinds stay the nouns because the walker and the packer branch on depth, units and a metric as well). And a picture of a spatial space asks a question that calls itself: what a face looks like depends on other pictures of the same space, a shadow map first (session 1's frame).

3. A thing in a spatial space is a placement of a definition, with a shape and a material; many things can place one definition, and the definition owns the edits while each placement owns its place (Sid's copies). The shape is stored as its source made it: a mesh from an artist, an exact solid from a feature history, a formula, a volume from a scanner, points or splats from a capture.

4. There is no single 3D "path". There is one way to ask every shape the questions: where does this ray hit you, is this point inside, what are your bounds, give me your surface at this tolerance, cut yourself with this plane. Not every shape has every answer: a point cloud has no inside until a rule reconstructs one, a density field has no surface until a threshold is named, so an answer can also be candidates, unsupported, or needs a policy. Converting between representations is work below the waist; which representation a thing uses is data above it.

5. Triangles are the GPU's format. Storing a sphere as 32 by 16 quads when the source was a sphere is the 3D version of storing a curve as the pen's samples read as corners. A mesh an artist faceted on purpose is the source and stays. Store what the source made; make triangles at the camera's tolerance, per view.

6. A view is a camera onto a space, and it belongs to a person. Orbiting is not an edit. Today's session row already keeps the view and the moved transforms outside the region row, which is the split made half-way.

7. A portal is a thing in one space that shows a view of another space, of either kind. Softland already has this noun. Today's Region3D is a portal on the page with the whole world stored inside it, so two windows onto one world are impossible.

8. Every seam has two maps. The picture goes up: render the view into a surface, paint the surface where the portal sits. The pointer goes down: map the point through the portal into a ray, ask the space. The two must agree: what you see is what you hit. Sharing one camera makes them agree about *where*, not about *what*; the bench found both sides wrong together, twice, so the what is checked apart.

9. A face in a spatial space can carry a planar space. Ink on a face is the path kind drawing in that planar space, in real units, millimetres on the face. That is 2D inside 3D, and it is the same path kind with no second contract. The attachment is a saved rule: which face, the frame the ink was drawn in, and what the ink keeps when the face changes.

10. The hit answer is an address, not a hit: page, portal, space, thing, part with its lineage, planar space, stroke, with coordinates at every level and the mode it was asked in. It is what "point and say" hands an agent.

11. Hover is a value computed from the pointer, the data and the views, not from pointer events. When an object moves in front of the stroke, the hover changes while the pointer holds still. The bench shows it. The hit is defined by the question asked (nearest visible surface, at this tolerance), not by the processor that answers it.

12. A screenshot looks the same and is not the same. It holds pixels and no reference; the portal holds a reference and no pixels. A picture can be data: a saved render, a scan, a painted texture. What a picture can never do is stand in for the editable meaning of the thing it pictures, the way triangles at a zoom band never stood in for the stroke.

13. Sort every value by when it changes: per edit (things, constructions, rules), per view (the camera, which things at what detail), per frame (the picture), per time (animation, simulation), and derived pictures on their own inputs, so a shadow map does not care that you orbited. Then place it. Per scale, the 2D axis's middle row, is the planar case of per view.

14. One waist. Identity and diffs, the seam walker, the compositor and the evaluator are shared by both kinds, and so are the three requests anyone can make of it: evaluate a construction, render an evaluated space through a view, query it with a point or a ray. Under them two floors of libraries: the planar floor the path round drew, and a spatial floor whose shapes, packer and renderer are a different language.

15. The evaluator the path round found as a fifth piece is, in 3D, the piece that decides the ceilings. A feature history, an operator graph, a shader graph and a mate are constructions; they are data only if code below can run them. That is the fork on this page. The bench now runs one, a rectangle extruded and cut, so what a runner must hand back has a first receipt: parts named by the construction, each with a lineage and a frame.

## Input and output, spelled out

The value the kind takes and the things it gives. This is what every session on the 3D kind should be talking about. It is written to compose with the path contract: the path is the planar shape, and a planar surface in a spatial space is where the two meet.

### In: spaces, things, views, portals, constructions

```
space
 id, revision, kind planar | spatial, unit (px | mm | m), up-axis, frame kind
 things* {id → thing}, relations (parent, prototype, hosts …)
 physics scope: which things occlude, light and collide with each other, declared on the space apart from grouping
   (a portal boundary isolates, a group does not; session 1)

thing   a placement of a definition (Sid's copies: many things,
 one definition; the definition owns the edits, the thing its place)
 id, kind, provenance, parent?, definition ref?, transform (TRS; a
   frame kind for non-affine cases such as geodetic)
 shape one of:
   mesh (polygons | subdivision cage)          the artist's truth
   brep (faces, edges, vertices)                from a construction
   implicit (f(p) ≤ 0, CSG)                     a formula
   volume (a grid)  ·  points  ·  splats        captures
   planar-surface (face ref, attachment rule,   where 2D lives in 3D
                   unit scale, planar space ref,
                   reads as coating | annotation | geometry)
   portal (view ref, extent, recursion bound,   a window either way;
           attachment rule when on a face)      picture or window
   solid (construction ref)                     what the runner made
 material (metallic-roughness | a shader construction), light?
 part ids stable: faces, edges, vertices are addressable; a derived
   part is named by the construction step that made it

attachment   a saved rule, not a face id: face ref, the domain the ink
 was authored on, anchor (origin corner | proportions | an edge),
 on split: clip | split | detach, on delete: detach; whose: this
 thing or the definition

view
 space ref, camera {pose: pivot distance yaw pitch | free; lens}, time
 policy: tolerance, budget, display mode
 owner: a session, or shared by declaration
 (the viewport comes from the portal that shows the view)

portal   a thing in a planar OR spatial space: view ref, extent

construction   a saved program over values: sketch → extrude → fillet;
 an operator graph; a shader graph; mates and constraints; a section
 at height h. Data only if the evaluator can run it (Position 9).
 Its modelling tolerance is in it: it changes the result.

identity   every thing and every addressable part has a stable id;
 a change is (thing id, component, value); nothing per view or per
 frame is ever in the value: no segment counts, no tessellations,
 no lease, no device tolerance. A source's fit, a construction's
 tolerance and a saved render with its provenance are values.
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

evaluate(construction, inputs, state)   no camera anywhere
 → values; parts named by the step that made them, each with a
   lineage against the previous run (unchanged | modified | split |
   merged | generated | deleted | ambiguous) and a frame; dependencies;
   status. The attachment rule reads the lineage and the frame.

to the CPU / other kinds   one question protocol; each representation
 declares which answers it gives, and an answer may be a result,
 candidates (glass, volumes, splats), unsupported (the inside of a
 point cloud), or needs a policy (a threshold, an opacity rule)
 hit(ray, mode visible | geometric | nearest | sample, τ)
   → {thing, part + lineage, point, normal, t, correspondence, status}
 inside(p, τ) · bounds · distance(p) where it is one
 surface(τ) → a mesh with correspondence to source parts
 section(plane) → paths · silhouette(view) → paths   (2D values,
   with provenance: which face made each)
 boolean · offset · extrude(path) · fillet …  kernel answers, bought

render(space, view, request)   the request names the quantity
 → a surface value: colour | depth | identity | radiance, its view,
   time, size and achieved quality. The page's portal asks for
   colour; a shadow asks for depth; a saved render is data.

the seam walk (shared with 2D)
 in: a page point or a ray; the tree of spaces, portals and views;
     the mode
 out: the address [space, thing, part + lineage, planar space +
      attachment status, stroke …] with coordinates at every level,
      t, the surface's status, and the diff against the last answer:
      entered, left, still

to the store
 ids and diffs only. A lease, a frame cache, a tessellation are
 never handed up; a render someone chose to keep is, with provenance.
```

Three things in this block are not in the field's scene description and are where this model speaks: the planar surface (a 2D space attached to a face, with a unit), the attachment as a saved rule, and the portal as a thing of either kind of space. Four tolerances, one more than the path kind: a source's fit and a construction's modelling tolerance are in the record, because they change meaning (a boolean's topology depends on them, FIELD; both reviews of this page and the other lane caught the earlier "none in the value"); a consumer's is a parameter of every answer; the device's is the packer's, per view, and is never in the value.

### What each representation must say about its answers

One protocol of questions; the applicability travels with the representation, not with the type name. The table is the review Sid pasted, adopted; the OpenVDB precedent (samples, a physical mapping and an interpretation kept apart; some operations only on a level-set grid) is FIELD.

| Representation | What must be specified before the answers mean anything |
|---|---|
| open mesh | Ray intersections are meaningful; an enclosed inside may be undefined. inside(p) → unsupported unless a closure rule is declared. |
| solid (brep, CSG) | Inside, boundary and section have defined meanings, subject to the model's validity and its modelling tolerance. |
| density field | A surface needs a chosen interpretation, an isovalue; a pick may concern accumulated contribution along the ray. surface(τ) → needs a policy. |
| point capture, splats | A reconstructed surface is a new result requiring reconstruction choices, with a declared loss; a hit is candidates or a contribution rule, not one winner. |
| curve in space | Edges, hair, wires, a path whose centreline is 3D: a shape language of its own, drawn with a view-facing nib. hit and distance are meaningful; inside → unsupported; surface(τ) needs a nib (session 1). |

So an answer may be a result, candidates, an unsupported operation, or a request for a policy, and the common interface carries that information. Otherwise a nominally universal interface quietly forces every ceiling through a surface model, and the scan and field ceilings are hacked through, not built through.

### When does a value change? The axis, with a fourth row

| Changes… | Values | Where it may live | Today (CHECKED) |
|---|---|---|---|
| per edit | things: transform, shape, material, parent, lights; a space's membership; the stroke's samples; a construction's inputs (the evaluator re-runs and reports lineage); an attachment's rule | CPU; stored; uploaded per thing; the evaluator | any static change is a full rebuild: every world triangle, one BVH, every instance row (scene.cljc:1088-1105); only a transform is maintained per object (:1113-1125) |
| per view | camera pose and lens; the visible set; each thing's tolerance; sort order; a planar surface's footprint (the path filler's τ on that face) | the packer's per-view cache, keyed on the view and the budget | the camera is rebuilt on a 1.12 bucket crossing of page zoom (renderer.cljs:1089-1095); draw order by object-origin distance (:894-913); placed ink packed at a fixed zoom 1.0 (on_plane.cljc:20), so it never re-lowers with distance or page zoom |
| per scale (a page view) | the portal's surface size; the lease | the compositor: quantised, capped, admitted by rungs | ceil(w·zoom·dpr) per rect, 256-px quanta, 4096 cap, divisors [1 2 4 8] (renderer.cljs:1053-1059; compositor.cljs:25-27; rungs.cljc:11) |
| per frame | the picture: shading, composite; the hover, re-walked when data or a view moved under a still pointer | GPU; the surface lease; the walker on the CPU | the interior re-encodes when scene, view, background or placements are dirty (renderer.cljs:1134-1139); no hover exists; pick is meshes only (scene.cljc:965-982) |
| on its own inputs | derived pictures: a shadow map (the light and the scene, not the camera), a fixed portal's surface (its view and its space), a probe; a render someone saved | keyed on their inputs, never on the frame; a saved one is a value (session 1's row, adopted) | the shadow has its own dirty role, set by any scene change and by nothing else: a camera-only orbit holds it, and moving a thing that casts on nothing re-encodes it, because its key is the scene's update kind at the region's grain, not the light and its casters (CHECKED renderer.cljs:1076, :1084-1086, :1134-1139, re-read in the fold of session 1's line; this page had said orbit re-encodes the shadow, a derived claim wearing an anchor; session 1's page had made and corrected the same error, and bench 0's shadow counter shows both halves: it rises when the box moves, not on orbit) |
| per time | animated attributes; simulation state; sim caches as derived values; a deformation, which is a placement on this axis: per frame on the GPU or per edit on the CPU, the executor's GPU door deciding (session 1) | the evaluator's clock; caches beside, never in the value | nothing |

## The hardest case, moment by moment

A pen stroke on a face of a box inside a 3D portal on a page. Zoom the page, orbit, hover, click; a sphere passes in front; a face of the box is a portal to another land; a screenshot of the portal sits beside it. Then the box's construction is edited under the ink: the rectangle widened, a groove cut across the top. Every moment below runs on bench 2 (`bench-2/seam-bench.html`), whose panel prints the address and the diff; the numbers are its, re-read after the fold's two repairs.

[FIGURE: see the html for the drawing]

Caption: **Both directions of the seam.** Down: the pointer becomes a ray in S, a point in Π, a point in L. Up: L's surface is paint on a face; S's surface is paint at R. The screenshot has the upward picture frozen and no downward path. The numbers are bench 2's at its default view.

**you draw** — Pen down over R. Each sample walks down: page point → R-local → ray from V → B's top face at t → Π's (u, v) in millimetres. The sample is appended to k with its width and its arc length. k is a path value in Π; it does not know it is on a box. Π knows it is attached to B's face E.top, by the rule "distances from the origin corner", authored on a 1000×1000 mm face, with 1000 mm to the metre. per edit: k.samples +1, revision +1, one texel row uploaded.

**you zoom the page** — R is drawn 2.4× larger. Its viewport goes from 420×330 to 1008×792 pixels, inside a lease that grows from 448×384 to 1024×832. V is unchanged and so is the framing: the projection takes its aspect from the portal's rect, never from the lease, so the same portal-local point (222.4, 109.7) hits the same world point at both zooms. A window's content does not come closer when you zoom the page, it gets bigger. k is unchanged. What did change is the footprint of a Π millimetre on the screen, 0.09 to 0.22 pixels, so the filler on the face runs at a new tolerance. per scale: the lease. per view (of the face): τ. per edit: nothing. Before the fold the bench took the aspect from the lease and the same world point drifted between the two sizes (the review Sid pasted caught it; renderer and picker agreed with each other through the drift). Today: the page zoom crosses a 1.12 bucket, the camera is rebuilt from the encode pixel size, within a pixel of the rect's aspect, the whole interior re-encodes, and the placed ink stays at its fixed zoom 1.0 and shows its polygons (CHECKED).

**you orbit** — V's yaw and pitch change. V is the session's; the region row does not change. The visible set, each thing's tolerance, the sort order and Π's footprint change. The walker uses the new camera and the same data. per view. Today: the session row already carries the view (renderer.cljs:1087).

**you hover k** — The walk returns an address, not a hit: [page (282, 180) px · R (222, 110), a 420×330 viewport in a 448×384 lease, framing 1.273 · S: ray hits at t = 3.23 m at world (0.00, 1.00, −0.20) · B, construction CB revision 0, face E.top, lineage from E · Ftop attachment resolved, rule origin, authored on 1000×1000 mm · Π (500, 302) mm, footprint 0.09 px per Π mm · k inside: segment 34, s = 673 mm, width 30 mm, at least 15 mm from the edge]. Six levels. One ray, one slab test per block, one classify with the exact swept nib (the union of the nib's discs; the bench's first classifier projected onto the centreline and missed points inside a wider disc further along, the review Sid pasted caught it, the shader shared the error). The diff says entered k. Today: pick-region returns B and a triangle index; placed ink is not in the BVH; no map from a page point to a region point exists (CHECKED scene.cljc:965-982; harness/region.cljs:600).

**Q passes in front** — Q's transform changes while the pointer holds still. Hover is a derived value of (pointer, data, views), so it re-walks: the ray now hits Q at t = 2.26 before the face at 3.23. The diff says left k → entered Q (the pointer did not move; the data did). This is the fine-grained reactivity Sid named: a mouse action over data, recomputed when the data moves. It rules out any design where the hit is only read on pointer events. It does not make the processor the definition: the hit is defined by the question (the nearest visible surface, at this tolerance, in this mode) over the data and the views; the CPU walker is the reference implementation of that question, and a GPU id buffer would have to be refreshed whenever the data or a view changed and agree with the reference at a tolerance (both reviews of this page; the other lane).

**you click k** — The same address. What the click does is a reaction row above the waist: on click of a thing of kind path in a planar surface, select it, or say something about it. The address is the context "point and say" sends: S, B, face E.top, k, 673 mm along. A drag keeps the target it captured until it ends; hovering something else mid-drag transfers nothing (the Codex lane).

**the portal on the face** — Hover B's front face: the walk enters T, maps the face point (0.50, 0.50) to L's (128, 128) px, and L's floor answers: circle c. [page R S B E.front T L c]. Rendering went the other way first: L into its own surface, that surface as paint on B's face, S into V's surface, that surface as paint at R. If L held a portal back to S, the compositor bounds the recursion and wears the picture visibly past the bound; the data holds no bound. T is an attachment too, with the rule "proportions of the face": widen the face and the land stretches with it; cut the face and the notch cuts the picture. Depth on L's side is L's and cannot be compared with S's (the other lane).

**the screenshot beside it** — I looks the same at the moment it was taken. Hover it: [page · I pixel (210, 165) of 420×330 · no seam]. Orbit: I does not change. Q moves: I does not change. Zoom past its pixels: I blurs while R re-leases. Draw on I: the ink goes on the page. What must differ in the data: R is a reference and a view; I is bytes with a digest and, at best, a provenance line "screenshot of R at revision 1". I is data, bytes anyone may crop or paint on. What I cannot do is stand in for R's meaning: no edit of the box reaches it, no hover through it reaches the box. The 3D kind's triangles at a zoom band are the same kind of thing: a picture of the shape, never the shape.

**you widen the rectangle** — R0's width goes from 1.00 to 1.40 m. The construction re-runs and reports E.top modified (1.00 → 1.40 m wide), E.right moved, everything else unchanged. k's samples do not change. Where they land is the attachment rule's to say: with "distances from the origin corner", Π (500, 302) stays at world x 0.00 and the new strip of face is blank; with "proportions of the face", the same Π point is at world x 0.20 and a Π millimetre is 1.4 world millimetres along x (footprint 0.14 px per Π mm); with "distance from the +x edge", it is at world x 0.40. The face's identity was never in doubt; where the ink belongs was, and naming the face could not say (the review Sid pasted). per edit: R0.size.w, CB revision +1 → the evaluator, the attachment resolves, the solid re-uploads, the hover re-walks under a still pointer.

**you cut a groove** — C subtracts a slab across the top. The construction re-runs and reports E.top split into 2 by C · C.floor, C.wall−, C.wall+ generated · E.front, E.back modified (notched). The rule says clip: k stays one stroke in Π and is painted where a surviving part of E.top is under it; over the groove it is absent, and the groove floor answers hover with "a generated face inherits no ink". Hover k on the left part: [… B face E.top · part 1 of 2 · split into 2 by C · Ftop resolved on 2 parts (clip) · Π (500, 302) · k inside …]. Say detach instead and the address reads attachment detached: split into 2 by C and the rule says detach · k stays in the record, on no face. Undo the cut: E.top merged from 2, the generated faces deleted, the ink whole again. The other picture, splitting the centreline and capping both ends at the groove, is a different rule and a different stroke count; named, not built. The same question stands in 2D: attack 2's network discovers faces from edges, and which face inherits a selection after a vertex moves is lineage plus a rule there too.

### Where the names still hide unfinished work

- **Surface.** Three things hid under this word until the Codex lane split them: the face a stroke sits on, the image a view produces, the target the GPU allocates. This page says planar surface, surface, lease. For the image: colour is one request; depth, identity and radiance are others, each with its own resolve rule (identity cannot be averaged), and a result must name the view and time it came from so an asynchronous answer never attaches a current identity to old pixels (the other lane). It has a status (fresh, worn at a divisor, rejected, stale at a revision) that the page and the hit answer must be able to read; "worn visibly" is law. Who owns that status and how it reaches above the waist is not designed. Today the worn stripe is painted by the composite pipeline and known to nothing else (CHECKED renderer.cljs:280-289).

- **Region.** Dies as a noun. It splits into a space, a view, a portal and a lease. Its `:extent` (width, height, depth) is required, validated, part of identity, and read by nothing (CHECKED fact-base-2 Part 1). If it is the space's bounds it belongs to the space; if it is the portal's clip volume it belongs to the portal. Named, not decided.

- **Portal.** The framing comes from the portal, so a view is not fully independent of the portal that shows it: two portals of different aspect onto "one view" are two views, and the lease's rounding must never move the framing (the bench's repair; today's aspect comes from the encode pixel size, within a pixel of the rect's, CHECKED renderer.cljs:1093-1095). The recursion bound is compositor policy. A portal's mapping is one of three kinds: fixed (a picture), window (derived from the viewer), declared (a mapping the executor runs, zoom beyond the rect becoming a dolly); travel is the third kind seen from the page (session 1). A mirror is a portal into its own space, so cycles are a case for the bound, not only depth (session 1). A portal's picture can read as an emissive screen or as reflectance under the host's light, and the two look different; the declaration chooses (the other lane). Whether events pass through, and whether light or bodies cross, are declarations a picture never supplies (session 1). The map from a planar point into a spatial view is what camera-matrices takes as an argument today (CHECKED scene.cljc:744).

- **Scene.** Dies as a noun; it is the space's content. "A whole scene as a source of an object" is a reference: a thing whose shape is the content of another space placed here. That needs instancing as a relation (thing → prototype) and an address that names the instance. Neither exists.

- **Face.** A planar surface on a plane is the affine case, and it is now worked: the face is named by the construction step that made it and comes back with a lineage and a frame (Position 10). Ink on a curved face is a planar space attached through a (u, v) → point map with a metric: a round nib in uv is not round on the surface, so surface-distance ink, a projected decal and uv painting are three constructions, not one (the other lane, session 1); the map has seams and a valid domain with holes; the filler on it takes an anisotropic footprint from the full chain, not one scalar (session 1); or the ink is baked to a texture on the face, which is what games do (FIELD). A gesture that leaves the face is a declared behaviour, clip, split, cross to the next face, never a silent jump to whatever is behind the pointer (the Codex lane). Not built.

- **Attachment.** The new noun the fold forced. It holds: the content (a planar space), the face it is on, the domain the ink was authored on, the anchor rule, a clause per lineage (on split, on delete, on merge), what the content reads as (a coating in the material, an unlit annotation, raised geometry; the Codex lane), and whose it is (this thing or the definition, so ink on one copy does or does not appear on all). What resolves it is the construction's frames and lineage. The merge clause is not written: two faces that both carried ink become one, and whose Π wins is not decided.

- **Chart, and session 1's other words.** Session 1's page and this one describe the same thing under different names, and neither set of words is Sid's; the translation is here so a reader of both needs nothing else. Its *chart* (two-dimensional coordinates with declared order plus a mapping onto an identified part of a face, the depth the face's under that mapping) is this page's planar surface, and its *attachment* is this page's attachment. Its three verbs, Construct, Present and Query, are this page's three requests: evaluate a construction, render an evaluated space through a view, query it with a point or a ray. Its Position 7, "2D on 3D is a chart layer in the face's look, never a biased decal", is this page's planar surfaces as materials in the filler row: the ink is read by the face's material and inherits the face's visibility, and today's negative depth bias on a separate ink mesh is the leak. Its Position 6, "two per-pixel resolves nest in one waist" (coverage inside a face's look, a space's visibility resolve inside the page's paint order), is this page's portal in the page's order with planar surfaces on faces. Its "a space is a frame plus an order rule" is kept as the criterion in story 2 and held against as the noun.

## What the 3D kind is today

One notch below the folder map. Each box is a piece with what goes in, what comes out, and why it has that shape. Arrows are real calls. Read against the picture above: the region row is a portal with the world stored inside it; the view and the moved transforms already leak out of the row into the session; the pointer map down exists for meshes only; the picture map up is one quad through the 2D group transform.

[FIGURE: see the html for the drawing]

Caption: **Today.** The region row goes in; a colour quad in the page's paint order and a mesh hit come out. Three pieces are already the right shape at the wrong scope: the CPU reference for hits, the view in the session, the leased surface admitted by budget. Two are the leak: the world inside the window, and render resolution inside the value.

At one notch below the drawing, where a renderer's convenience became a definition (session 1's third column, adopted as the how-to-think of the whole path round made visible on the tree): the rect, a viewport, stood for the portal's place; the lease, an allocation, for the surface's size; the composite, a draw, for the page's paint order; the depth bias, a pipeline constant, for the attachment; the triangle index, a buffer position, for the face. Two more instances from the tree: the CPU pick is two-sided while the GPU culls back faces (CHECKED scene.cljc:629-630, renderer.cljs:391), which is "agree about where, not about what" in the tree itself; and depth is forward [0,1] depth24plus, not reversed-Z (CHECKED scene.cljc:717-727), the depth twin of the rebase the planet needs. One thing already sits where it belongs: the space renders linear HDR into rgba16float, the region's shader tone-maps, the surface is premultiplied and the present converts to sRGB (CHECKED renderer.cljs:139, :222, compositor.cljs:30-31), which is exposure placed on the camera, like a real one (session 1).

## The attack on the working basis, claim by claim

Sid's working basis, one claim at a time. HOLDS means it is the answer for this kind. DEFAULT means it works until a named condition, and the condition is stated. SHORT means it cannot express something Sid has already said he wants.

| Claim | Verdict | Why, and the condition |
|---|---|---|
| A 3D scene is objects with transforms, geometry, materials, lights and a camera; rendering is projection, visibility and shading. | HOLDS, with two edits | As the output language of the raster model it has no rival; the path tracer takes the same description, which is why the description is the settled part (FIELD). Two edits. "Geometry" is several representations under one question protocol, each declaring its answers, not one type. "A camera" belongs to the view, not the scene: a camera can be placed in a space as a thing ("a good view", data), but the camera that renders is a person's. Today the camera comes from the session first and the row second (CHECKED renderer.cljs:1087). |
| Interchange settled, execution contested; the same split as the 2D imaging model. | HOLDS | USD, glTF, IFC, 3D Tiles, VDB are the settled forms; renderers differ by workload (FIELD). The consequence for Softland: shape the data above the waist like USD's composition, prims with attributes at stable paths, opinions in layers resolved by declared strength, references and instancing. That is the field's answer to many people editing one scene without merge deadlocks: a layer stack is a merge by strength, not by time. It does not make two incompatible edits compatible: two dimension edits can merge mechanically and still break a constraint, so an explicit conflict outcome stays above the kind (both lanes). USD names things by path and warns that topology changes defeat stronger layers; OpenExec adds computation to USD and still supplies no interaction (the other lane's correction, FIELD). Where USD is silent, the page, the portal, hover, the planar surface, is where this page speaks. |
| One composition primitive joins the two models: render a scene of either into a surface, and a surface is paint in the other, in both directions and recursively. | DEFAULT for the picture, SHORT for the hit and the diff | It states the upward map only. The seam is a pair, and the downward map (a point through the portal into a ray, a face point into a planar space) is what "click in, you hit this out" needs; the bench's screenshot shows a surface with the upward half and no downward half. And three crossings hide under "one primitive": a portal (a view and a route), an attachment (coordinates on a face by a saved rule), a derivation (new geometry with provenance); each is a pair of maps, and only the first is a picture (the Codex lane, the other lane, the review Sid pasted). Also "a surface is paint" holds only while the portal is a window: if a 3D thing must occlude page text by depth, the surface would need depth and the page a depth policy. Position 4 keeps it paint. |
| The 2D contract ends at a region drawn with per-pixel coverage; whether 3D begins there is open. | answered: it does not | 3D's *imaging* begins one level up, at the view render: (view, packed visible set, lights) → a surface. Coverage of a region is one filler; a view render is another; both end at a surface the compositor owns. The two meet at the compositor (surfaces) and at the walker (addresses), not at the coverage filler. The one place they touch below that: on a face, the path filler runs inside the view render as the face's material (the path page's Position 6). But the view render is the imaging boundary, not the entrance to the kind: a construction evaluates and a section is cut with no camera anywhere, so the kind has three entrances, evaluate, render, query, and the coverage filler is a library inside one of them (the review Sid pasted; the other lane). |
| Every session that started from rendering made a definitional error; start from what the object must mean. | HOLDS, with one exception named | Honoured here: the four nouns come from what a space, a view and a hit must mean under orbit, edit, share and travel. The exception: splats and radiance fields have no geometry beneath them; their meaning is what they look like from any view, and "you hit this" for them is a policy over opacity along the ray (FIELD). The rule bends there, and the question protocol absorbs it: hit takes a mode and a policy, and candidates are an answer. |
| The triangle mesh is the 3D leak, the GPU's format standing in for the object. | half right, as with the polyline | A mesh is the artist's truth and a scan's reconstruction; as a source it is legitimate, like the polyline was a legitimate path. The leak is three other things: tessellation resolution stored in the value (segment counts, CHECKED component.cljc:53-60), a derived mesh standing in for a source that could be re-tessellated at the camera's tolerance (a sphere, a solid, a cage), and normals *required* of every mesh (CHECKED :373-384): an artist's authored normals are intent and stay, the requirement is the leak (the review of this page). And the larger leak would be a picture standing in for the shape: a lease, a frame cache, a tessellation in the value. A render someone chose to keep is not that leak; it is a value with provenance. The region row holds no picture today; what it holds instead is the world. |

## Do the path round's pieces transfer?

Four pieces of code and one axis. Each transfers as a role and changes size; two break in a way that names the difference between the floors.

| Path kind | Spatial floor | Where it breaks |
|---|---|---|
| path type | Not one type: an interface. Shape representations (mesh, subdivision cage, brep, implicit, volume, points, splats), each stored as its source made it, each answering the questions it can under one protocol: hit, inside, bounds, surface(τ), section, silhouette, distance, correspondence; unsupported and needs-a-policy are answers too. The path is the planar member of the same interface. | The single stored representation. In 2D everything compiles to the path cheaply enough; in 3D conversion is lossy and is the work (brep → mesh at τ; mesh → brep is research; volume → isosurface; points → mesh). So the value is per representation and the answers are one. |
| geometry (answers) | The same answers lifted a dimension, plus kernel operations the field buys, not writes: boolean, offset (shell), fillet, extrude(path). Section and silhouette return path values. Correspondence maps say which source part made which derived part. Sweep generalises: a nib along a path is Minkowski along a curve, and extrude, revolve and pipe are the same operation one dimension up, so the geometry piece is one piece with a dimension (session 1). | The answers include libraries the size of the whole client (a brep kernel). Bought behind the interface; the choice is a dependency decision (OpenCascade, Manifold for mesh booleans; FIELD). |
| packer (per scale) | Per view, with a budget: tessellate at the view's tolerance per thing, cull, select detail, instance, stream, sort. The image kind's residency counter is the seed of this piece, grown up. | Budget and residency. 2D had no residency question except images; 3D's packer is a scheduler that must never make the frame wait (the render-seam law) and must report worn state. This is the ceilings' home: LOD trees, tile trees, octrees are all this piece. |
| filler (per region) | Per view: (view, packed set, lights) → a surface. Depth, shading, shadows, a transparency policy, several programs (raster, raymarch, splat), planar surfaces as materials; the render request names the quantity (colour, depth, identity, radiance); a CPU walker is the reference for the hit, not for the image. | "One resolve per region" becomes "one render per view", because a mark's pixels depend on other marks. The filler is no longer per item. Shading as data (shader graphs) needs a compiler below the waist: the evaluator at another size. And the question is recursive: shading's inputs are other renders of the same space (a shadow map is the space seen from the sun, in the tree today; probes and reflections in the field), so the per-pixel question calls itself and where a renderer cuts the recursion is the contested execution (session 1's frame). The road for that renderer is session 1's position, adopted as it stands and marked as about the road, not the model: of the four (forward with MSAA, deferred with temporal, ray query over a BVH, path traced), forward with MSAA, generalised from today's region lane, because it is the shape in the tree, it keeps a page with text and ink crisp (temporal antialiasing smears both), and every face stays free to run any layer, where a deferred G-buffer fights per-face layers and translucency; MSAA's memory per resolve target is the parked GPU-sharing worry, decided by the budget mechanism and not by default; keep the ray-query road open by never letting a raster-only quantity into the value. It flips to clustered forward, still forward, if open-world light counts make forward lighting the bottleneck; and if hardware ray queries land in WebGPU, visibility and pick become one function and the depth resolve becomes a cache. |
| evaluator (Position 8) | The piece that defines the ceilings: feature histories, operator graphs, shader graphs, rigs, constraint systems, simulations with a clock. Shared with 2D. What it hands back: values, parts named by the step that made them with a lineage and a frame, dependencies, status (the bench's evaluator does this for a box; the definer's attack 2 spells the call as execute(recipe, inputs, state) → named results + next state). | Scale. An interpreted graph over a million points per frame does not hit frame rate; the field compiles (FIELD). The evaluator needs a compiled tier, and its language is the fork (Position 9). |
| axis: per edit · per scale · per frame | per edit · per view · per frame · per time | Per scale is the planar case of per view (a page has one scale; a spatial view has distance and angle per thing). Per time is new: values change without an edit, so the evaluator needs a clock and caches are derived, never truth, except a capture that cannot be re-run. |

## What 2D never had: code below, or data above

| Thing | Forces code below the waist | Stays data above |
|---|---|---|
| A mark's pixels depend on other marks (shadows, occlusion, transparency order) | The view render: depth, shadow passes, a transparency policy (per-object sorting by origin cannot resolve intersections, CHECKED renderer.cljs:894-899; OIT is the field's fix, FIELD). The picture's dirtiness is separate from the data's diff: one move dirties the view's picture, the data diff is one row. | Which lights, which materials, shadow flags, the policy chosen per view. |
| Many shape representations; converting is the work | The question protocol; each representation's answers and refusals; conversions as operations the evaluator calls, each with a declared loss. | Which representation a thing uses; the construction that derived one from another (a mesh from a scan at tolerance τ). |
| 2D and 3D nest both ways; 2D derived from 3D (sections, silhouettes, plans) | The seam walker; section(plane) and silhouette(view) as answers that return paths; the path filler on faces; the planar surface as a shape kind. | Portals, planar surfaces, the construction "plan at height h" whose result is a planar space of paths; BIM's drawings are exactly this, re-run on edit. |
| Ranges and budgets past float32 and per-frame time | f64 rebased around the camera before upload (never done today, CHECKED fact-base-8 Part 4); non-affine frame kinds (geodetic); residency by budget with worn status; N views per frame (stereo: the head is the camera, the hands are rays into the same walker). | Units, frame kinds, the budget policy per view, view pairs. |
| Time and simulation | A clock in the evaluator; solver operations as a library; per-time caches as derived values. | The animation or simulation as a construction; a cache that cannot be reproduced is a source, like a scan. |
| A constraint history that must survive an edit | The executor; kernel operations; lineage and a frame for every derived part from every operation (worked on the bench; Position 10). | The history, the mates, the sketch constraints. |
| Units, tolerances and frames as meaning | The consumer's tolerance as a parameter of every answer; the device's in the packer; frame kinds as code. | A space's unit and up-axis; a planar surface's scale (today's fixed 1.0 is this, hard-coded, CHECKED on_plane.cljc:20); a source's fit and a construction's modelling tolerance, in the record; a view's tolerance policy. |
| From the ceilings: instancing, streaming, many lights, stereo | Instancing in the packer; tile and octree residency; clustered lighting; two views per frame. | Prototype relations, tile references, light lists, the view pair. |

## The ceilings through the model

For each ceiling: what is data above, what must be code below, and the honest step where the model would crack, the way the 2D round found each ceiling broke at its own hardest step. The product mechanics are general knowledge of those tools (FIELD).

| Ceiling | Data above | Code below | Where it cracks |
|---|---|---|---|
| Parametric CAD | Sketches as path values with constraints; a feature history as a construction; assemblies as references with mates; drawings as planar spaces derived by section and projection. | A brep kernel behind the question protocol (bought); a sketch constraint solver and a mate solver as operations; exact edges and silhouettes at every zoom (answers, not tessellation); the executor. | Derived-part identity: the fillet, the mate and the ink refer to a face the re-run extrude must produce again, with a frame and a lineage the attachment rule can read (worked on the bench for a box, Position 10; a kernel's history is what it must return, OCAF's generated/modified/deleted is the precedent, FIELD). Fillet robustness is the kernel's. A hundred thousand parts is the residency ceiling. |
| Procedural film tools | Operator graphs over geometry values; rigs as deformer constructions; shader graphs; sim caches per time; two views (real-time, path traced). | The executor with a compiled tier; subdivision, deformers, solvers as a library; a shader compiler; a path tracer as an out-of-loop rung (his home rig; the generated-solidity tag). | The evaluator's speed: a graph over a million points per frame, interpreted, does not hit frame rate. Sim determinism across collaborators is a fork (lockstep or cached). |
| Real-time open world | Instances (thing → prototype), detail declarations, lights, materials, physics and animation state machines with a clock. | Instancing, streaming residency with budgets, culling, clustered lighting, shadows, global illumination as a view-render policy, post passes in the compositor, two views per frame. | The frame planner. If everything must be scheduled by one budget, the page becomes content of that planner. One scheduler can coordinate several libraries, so this does not by itself force two engines (the review Sid pasted; Position 6, qualified). GI and reflections are "pixels depend on everything" at scale; nothing above the waist can add them. |
| BIM | Typed things with relations that propagate (a wall hosts a door); disciplines as layers; plans, sections and schedules as planar spaces and text derived live. | Section and projection answers; the propagation executor driven by the diff (the reactive graph is the executor's scheduler); layer composition by strength; hidden-line removal as a visibility answer on the CPU, not a draw, because a plan at drawing quality needs each line with the face that made it or the plan cannot be edited back (session 1). | Propagation cost: one wall move re-runs every dependent construction. The fine-grained diff must reach the executor as dependency invalidation, or every edit is a full re-run, today's :full. |
| Planet-scale geospatial | Tiles as references with per-tile frames; imagery as image things on terrain surfaces; time-dynamic attributes. | Geodetic frame kinds; f64 rebased around the camera; tile-tree residency at view-dependent detail; a depth policy (reversed or logarithmic depth). | Precision through the seam: a page portal into a planet at street level needs the whole chain in f64 down to the view; the camera and group buffers are f32 today (CHECKED). A hit at planet scale is a tile plus a feature id. |
| Fields and scans | Volumes, point sets and splats as representations; isosurface constructions with a threshold. | Raymarch and slice programs in the view render; octree residency; marching cubes as an operation; splat sorting per view. | "You hit this" has no surface: the hit for a volume or a splat set is a policy (first voxel above a threshold, accumulated opacity) over a ray-integration answer. The object-meaning-first rule bends here. |

**What must be code, said once** (session 1's list, adopted; the answer to Sid's "is something missing that has to be hardcoded"). Nine things recur as code across all six ceilings: the executor with dependency tracking; render to surface from any camera; the visibility resolve with its transparency policy and the sampled-shape programs' composition rule; the per-scale mechanism, GPU-driven at the top; the geometry shelf with the exact kernel as its bought member; f64 and rebase in the transform tree; the material stage with the path filler as a layer; curves in space; stable identity for things, faces and copies. Everything else in the six rows above is data that names which of the nine to run and with what.

## How the best team in the field would define it, and what they would name

**The problem as they would state it.** You are building a composed scene description with stable paths and layered opinions, a change tracker that turns edits into per-thing dirty bits, render delegates behind one interface (a real-time raster, an offline path tracer, a CPU reference), a query service for picking, and a pipeline of scene filters, pure functions over scene data, for procedural generation, instancing, detail and derivation (FIELD: USD and Hydra are this shape; Hydra 2.0's scene indices are the filters). Then they would add what the field does not have and this page needs: the planar space, the portal in both directions, the walker, and hover as a derived value. The trade-offs they would name, FIELD unless marked:

- **Buy the kernel.** Nobody writes a brep kernel; the choice is which one runs in the browser and at what size. Same for mesh booleans and sparse volumes.

- **Instancing versus uniqueness.** A million things share one shape and differ by placement; an instance has an id and no data of its own; moving the prototype is one edit that dirties a million pictures; the hit names an instance path.

- **The tolerance is per view and per thing.** A screen-space error metric with hysteresis so detail does not pop. The packer is asynchronous: the frame never waits for data; worn is the honest state while it streams.

- **Transparency ordering.** Per-object sorting fails at intersections (today's docstring says so, CHECKED). Choose an order-independent policy up front; it cannot be retrofitted.

- **Shading as data has a compiler.** Material graphs become programs; permutations explode; an uber-shader or a cached compiler is the choice.

- **The hit is defined by the question, not the processor.** Two collaborators must agree on what was hit; GPU id buffers differ by driver. So the definition is the declared semantics (mode, policy, tolerance) over the data and the views; the CPU walker is the reference implementation; any fast path is compared to it at a tolerance, the same rule the path round set for membership and coverage. A processor alone establishes nothing: the bench's CPU and GPU agreed with each other through a wrong nib and a wrong aspect (the review of this page).

- **Copies are two things.** A forest is one thing with many placements and no identity per tree; walls are many things sharing one shape, each with identity. The renderer batches by shape either way; the value must say which (session 1).

- **Face identity does not come free.** glTF has no faces; every attachment, mate and hosted door needs an anchor model decided before the first placement, because every placement references it (session 1). On this page the anchor is the construction step's name plus a frame, and the attachment's rule reads it.

- **Event routing and capture at the seam.** Which space owns the wheel when the region fills the screen is policy above the waist, but the walker must be able to route input to the space it hit; a drag keeps the target it captured until it ends (the Codex lane, session 1).

- **Precision.** Camera-relative rendering and per-tile frames; never a single f32 world.

- **Time and determinism.** Time samples on attributes; a view has a time; simulation for collaboration is lockstep or cached, a fork to name early.

- **Sessions are a layer.** The per-session view and transform overlays that exist today are the weakest layer of a layer stack; formalise them as one, and undo and preview fall out.

- **Conventions.** Up axis (Y here, Z in CAD and BIM), units, handedness: a space declares them and imports convert; a silent mismatch is the classic bug.

- **Stereo and hands.** Two views per frame of one space, and a ray as the pointer: the walker takes a ray, and the page point is the orthographic special case.

- **Clips, blends and layer stacks in one pipeline.** Vello and Skia carry them as compositor passes; the tree has a rectangle scissor (CHECKED compositor.cljs:612-629). Sid's word the same night: "adopt the best in field". They land in the shared compositor, once, for both floors.

- **Colour management.** Linear premultiplied inside, sRGB at present, a tone map per view; today's chain does this (CHECKED scene.cljc:21, compositor.cljs:644-671) and the page's colour space must be declared alongside.

## Sid's questions, one by one

**What is the general 3D object, and what are its sources? Is the region the thing?**

A placement of a definition with a shape and a material in a spatial space, where the shape is stored as its source made it and every representation answers one protocol of questions with the answers it has. Sources sort as: a modelled mesh is a mesh (an artist's polygons or a subdivision cage, the cage is the source); a formula solid splits into an implicit (f(p) ≤ 0, CSG) and a constructed brep (sketch, extrude); a scan is points, a splat cloud is splats; a 2D shape given depth is a construction over a path value (the seam with the path kind: the sketch is a path); a whole scene is not an object, it is a space, and "used as an object" it is a reference placed in another space. The region is neither the thing nor the container: it is a window, a portal with a view and a lease. The space is the container. The things are in it.

**Is the triangle mesh the 3D leak, and what is the analogue of storing the curve?**

The triangle is the leak only when it stands in for something else. Storing the representation the source made, and tessellating per view at the camera's tolerance, is the analogue of storing the curve; today's segment counts in the value and normals required of every mesh are the analogue of the zoom bands in the cache key. The larger analogue is a picture standing in for the shape: a lease, a tessellation, a frame cache in the value. A render someone kept, a scan, a painted texture are data with provenance; what they cannot be is the editable thing they picture.

**Do the four pieces and the axis transfer?**

As roles, yes, each at a larger size; the table above. Two break in a way that defines the spatial floor: there is no single stored representation, and the filler renders a view, not a region. The axis gains a row, per time, and renames per scale to per view.

**Containment: 3D in 2D, 2D in 3D, or nested spaces?**

Nested spaces of both kinds, crossed three ways, each a pair of maps: a portal shows a view of another space, an attachment puts a planar space on a face by a saved rule, a derivation cuts geometry out with provenance; the page root with windows is the default containment and travel re-roots. What a 3D thing must continue to mean: after orbit, the same thing, because the view is not the value; after moving an object, the same thing with one changed row, and every picture that depends on it dirty; after placing ink on a face, a planar space attached to that face with the ink in it, so the ink follows the face; after zooming the page, the same view at more pixels; after sharing, the same space with your view and their view separate, unless a view is shared on purpose.

**Two cameras: what composes them, and what happens when the page's zoom crosses into a region?**

Function composition: page affine ∘ portal rect ∘ viewport ∘ projection ∘ view ∘ model. The page's zoom sets the portal's pixel size, per scale; the region's camera sets the projection, per view, from the portal's framing; they never mix. Three operations, never one word: page zoom magnifies the placed view with its framing kept; a lens zoom changes the region camera's field inside a fixed aperture; a dolly moves the camera. Only the first is the page's (session 1's bench measured the first: aperture doubled, inner projection identical; the reviews of 6f62711; this is the account the review Sid pasted carried forward). Crossing is not a zoom: it is travel, a change of root, and it is a declared reaction on a band-crossing event, which is what "the zoom level will make something happen" asked for. Below the waist that needs one thing: either kind of space can be the root.

**Picking and hover through the seam**

The walk in the moments above: page point → portal → ray in S → nearest hit among shapes → if the hit part carries a planar surface, its point → the path kind's classify at the pointer's footprint → an address with coordinates at every level and the diff. At pointer rate: one ray, one BVH walk per space on the path, one classify. Says only what changed: entered, left, still. Recomputed when data or a view moves under a still pointer. The walk names the mode it answered in (visible nearest, here) and returns candidates where no single winner exists: glass, a volume, splats (the other lane; session 1).

**A surface is paint: colour only, or depth and identity too, and at whose resolution? What is the unit of change?**

For the page's portal, colour only, premultiplied, at the portal's projected size times DPR, quantised and admitted by the compositor's budget, worn visibly, framed by the portal and not by the lease. But render is a request, and the request names the quantity: colour for the page, depth for a shadow, identity for a parity check, radiance for a probe, each with its own resolve rule (session 1, the other lane). Exposure is the camera's, like a real one: the space renders linear HDR, the camera tone-maps, the surface is display-referred premultiplied; today's chain already is that (session 1; CHECKED renderer.cljs:139, :222, compositor.cljs:30-31). Depth and identity for the hit are answered from data by the walker, not read from the surface, and that is what lets a portal be a plain paint item in the page's order. The unit of change is (thing id, component, value); for derived parts, the construction step's name with a lineage. The picture's dirtiness is a separate, coarser thing.

**Am I reasoning from what region3d does today or from how it should be?**

This page was derived from what a space, a view, a portal and a hit must mean under orbit, edit, share and travel, in Sid's own cases (the house inspector, point and say, world within a world), then checked against the tree. Three things in the tree already have the right shape at the wrong scope: the CPU reference for hits, the view and transforms in the session, the leased surface admitted by budget. Two are the leak: the world inside the window, and render resolution inside the value. "That is what region3d does today" is evidence of feasibility, never the reason for the design.

## Positions, each with the condition under which it stops being the answer

**[position 1] the space is the entity; the region splits into four**

Space (id, unit, things, relations), view (a camera; a session's), portal (a thing on a page or a face: a view reference and an extent), surface (a lease; derived, worn visibly). Supported by Sid's words (space as entity, the bidirectional house inspector, hover and click, sharing) and by the code's own session overlays for view and transforms, which are this split made half-way. Stops being the answer: never for the data. If the product only ever shows one window per world, the four nouns are still right; the split costs one more row.

**[position 2] the general 3D object is a placed shape under one question protocol, with several stored representations, each declaring its answers**

Store what the source made (an artist's faceted mesh included); tessellate per view; the questions are one interface, the answers are each representation's own, and an answer may be a result, candidates, unsupported, or needs a policy; the interface names the questions, not a promise that every shape has every answer (both reviews of this page; the other lane; the Codex lane). Stops being the answer: if only meshes and subdivision surfaces ever matter (the film and game road), a mesh-only floor is the workable default and CAD, volumes and captures enter as lossy imports; then the CAD, BIM and scan ceilings are hacked through, not built through. Sid's "sculpting and node authoring" soon, and CAD on the ceilings list, say several.

**[position 3] the seam is a pair of maps, and it is crossed three ways: portal, attachment, derivation**

Picture up, pointer down, in both directions and recursively, agreeing at a declared tolerance: what you see is what you hit. Agreement by a shared camera covers where; what (which face, inside which stroke) has its own definition on each side and is checked apart (the tree's own instance today: the CPU pick is two-sided and the GPU culls back faces, CHECKED scene.cljc:629-630, renderer.cljs:391; session 1). Three crossings, each a pair of maps: a portal shows a view of another space and carries a route for the pointer; an attachment puts a planar space on a face by a saved rule (the picture up is the face's material, the pointer down is ray → face → frame → classify); a derivation returns new geometry with provenance (the picture up is the paths drawn, the pointer down is the face that made each). This page first said "one nesting primitive"; the Codex lane and the other lane had three crossings, and the review Sid pasted showed the one word hid the other two. Stops being the answer: no condition found. The unfinished work is named: the recursion bound and the surface's status.

**[position 4] the page stays planar with a paint order; 3D relations live inside one spatial space; the surface is colour only**

"Entities wear 3D facets" and "the land contains 3D windows" are one model: a facet is a portal framed on one thing in a shared space. Two 3D things on one page that should occlude each other are two things in one space with one portal, or two portals into it. Stops being the answer: if Sid wants a 3D thing to occlude page text by depth, sticking out of its window, the page needs a depth policy and the surface needs depth. Named, not taken.

**[position 5] the root is any space; a window is the default containment; travel re-roots on a declared reaction**

The house inspector walks in; the page becomes a window or a HUD. Stops being the answer: engine cost. A spatial root means text and ink draw on planes everywhere and UI lives in space; until that is built, the page root with windows is the workable default, and nothing in the data changes when it arrives.

**[position 6] one waist, two floors of libraries**

One data model for both kinds; four shared services (identity and diffs, the walker, the compositor, the evaluator) and three shared requests (evaluate, render, query); a planar floor and a spatial floor whose values and fillers differ, as libraries under the one waist, not engines. What they exchange is values, identities, dependencies and operations. Stops being the answer: not at the residency ceiling, as this page first said; one scheduler can coordinate several libraries and the page becoming content of the frame planner is a scheduling fact, not a second engine (the review Sid pasted). It stops at incompatible guarantees: if a spatial runtime must ship separately with its own semantics, the boundary between them is a bridge that still shares identity, versions, units and queries (the Codex lane). The compositor is the meeting point either way.

**[position 7] the hit is defined by the question; the CPU walker is its reference; hover is a derived value**

"You hit this" is defined by the declared semantics of the query (mode: visible, geometric, nearest, sample; policy for glass, volumes and splats; tolerance) over the data and the views. The walker on the CPU is the reference implementation of that definition, because two collaborators must agree and GPU id buffers differ by driver; a GPU answer may implement the same semantics and must agree with the reference at a tolerance, refreshed whenever its inputs change, not re-read unconditionally every frame. The processor is not the definition: CPU code can implement the wrong geometry, and the bench's did, twice, in step with its shader (both reviews of this page; the other lane; session 1). Hover is f(pointer, data, views), recomputed when any of the three changes, and reports only the diff. Visible hit, geometric intersection and the selection a person intends are three questions with declared relations, sharpest for thin ink, transparency and volumes (the other lane). Stops being the answer: if pointer-rate hover over millions of instances cannot be served by a BVH per space, the id buffer becomes the fast path, still compared to the reference.

**[position 8] views belong to people; cameras in a space are things**

A camera placed in a space is data ("a good view", a saved shot); the view that renders is a session's, shared only by declaration. Orbiting is never an edit. Stops being the answer: no condition found.

**[position 9 · the fork] constructions are an input the kind takes, and the evaluator's language is Sid's to choose**

A feature history, an operator graph, a shader graph, a mate, a section at height h: each is a saved program over values, data only if code below runs it. The path round's Position 8 named three doors and said the choice would close when brushes or shape formulas were wanted as data. 3D forces it now. The doors: (a) a fixed menu of operations per family, every new behaviour new code; (b) a node vocabulary as data with an executor, the shape of USD's scene filters, Houdini and BIM; (c) code as data, Clojure forms run at runtime. Position: (b) as the data shape with (c) as the escape hatch, because otherwise CAD histories, operator graphs, shader graphs and constraint systems are four separate languages. Sid's doubt of the same night, "one executor idk seems like the constraints and hard part would be getting them in line throughput seems small", sharpens it: one *language* for constructions, several *runners* behind it. Constraints are not the executor's job but operations it calls (a sketch solver, a mate solver, bought like the kernel); throughput is a compiled tier for what runs per frame or per time (Houdini compiles its expression language; FIELD), with the interpreted tier for editing. What stays one is the data shape the store merges and the address the walker returns. Two things the fold adds before the language is the only fork left: the decision that constructions are authorable is separate from which entrance consumes them, and an evaluator that produces typed shape values for geometry, rendering and queries keeps the renderer from interpreting a whole authoring history (the review of this page); and what the runners must exchange has a first receipt on the bench, parts named by the construction, each with a lineage and a frame, which the definer's attack 2 spells as execute(recipe, inputs, state) → named results + next state (the review Sid pasted asked for this before the language is chosen). This is a substrate decision, a future-binding noun, and Sid's alone; asked once, here.

**[position 10] a derived part's identity is what its construction returns, a name, a lineage and a frame; where the ink belongs is the attachment's rule, not the face's**

Naming the face is necessary and not enough. On the bench the box is a saved construction (a rectangle, extruded, optionally cut). Its runner returns parts named by the step that made them (E.top, E.front, C.floor …), each with a lineage against the previous run (unchanged, modified, split into n, merged from n, generated, deleted) and a frame (origin corner, extents, height). The attachment is data of its own: which face, the domain the ink was authored on, an anchor rule (distances from the origin corner, proportions of the face, distance from an edge), and a clause per lineage (on split: clip or detach; on delete: detach). Widen the face and the three rules put the same stroke in three places while the face's identity never wavered (the review Sid pasted named the three); cut the face and "clip" keeps one stroke painted on the surviving parts, the generated faces inherit nothing, and "detach" leaves the stroke in the record on no face, said out loud in the address. What the store sees per edit: one row of the construction and one revision; k's samples never change. What the walker says: the face, its part, its lineage, the attachment's status, then Π and k. Attaching to a face and attaching to a construction step are one thing here: the face is named by the step that made it and the rule reads the step's frame (the review of this page: not exclusive). The field's kernels return the same three (OCAF: generated, modified, deleted, with history; FIELD) and still admit ambiguity; ambiguity is then an honest lineage value the rule needs a clause for, not a heuristic the kernel guesses with. The same question stands in 2D: the faces of attack 2's network are derived parts, and which one inherits a selection after a vertex moves is lineage plus a rule; one answer for both floors. Stops being the answer: two cases the bench does not reach, a merge where both faces carried ink (whose Π wins) and a curved face (a frame is not a plane); and if a kernel cannot return a parameter map for a modified face, the rule falls back to nearest-by-geometry and must say so in the status.

## What changes in the path-kind picture

- **The path value gains a home.** A path lives in a planar space: the page, or a planar surface on a face. Nothing in the value changes. The planar surface carries the unit, so the stroke width's unit "local" means the surface's unit, which on a face is millimetres.

- **The filler runs under a projection.** Position 6 of the path page stands: the coverage filler on a face is the face's material inside the view render; it takes the face's frame and the per-view footprint as its tolerance. The bench draws the swept nib this way on the face; the placement zoom of 1.0 is the sixth place that decides how big a pixel is, and it dies.

- **One new consumer, one new producer.** The walker calls classify(point, slop) with a slop derived from the pointer's footprint on the face, not a constant. Section(plane) and silhouette(view) of spatial shapes return path values, and extrude(path) takes one: the path is the shared 2D value across both floors. Sections keep their conics: line, quadratic and cubic are the imaging language, and circles, conics and rational intersections stay analytic in the geometry shelf's result and lower to the path value at a declared error with provenance; the path filler never becomes a CAD kernel (session 1).

- **The evaluator is promoted.** Position 8 of the path page is forced to a decision by 3D, not by brushes, and the recommendation moves toward the general door. Everything else on the path page holds.

- **The axis is renamed, not changed.** Per scale is per view's planar case; the path page's rows stand.

- **The precision line hardens.** Rebase around the camera before upload was a fix-list item for ink; for a planet it is the design. Reversed-Z float depth is its depth twin; today is forward [0,1] depth24plus (CHECKED scene.cljc:717-727; session 1).

- **Coverage declares what it reads as.** On a face the same coverage can be a coating in the material, an unlit annotation, or the input to raised geometry; the planar surface says which (the Codex lane, session 1). The filler is callable inside another program, coverage(uv, footprint), with an anisotropic footprint from the full chain (session 1).

- **Derived parts are a 2D question too.** Attack 2's network discovers faces from edges; the selected face is a derived part, and which face inherits the selection after a vertex moves is lineage plus a rule, the same answer as Position 10. The arrangement is a reusable geometry result with links back to its source edges; the face-choice policy is data (the definer).

- **The nib is exact on both sides.** The swept nib's membership is the union of its discs; per segment that is the convex hull of the two end discs with a closed-form signed distance, on the CPU and in the shader alike. The centreline-projection shortcut is wrong wherever the width changes, and it was wrong in step on both sides of the bench (the review Sid pasted).

## What decision remains, and the exact question still open

**The decision that remains about 3D's input and output** is Position 9: whether a construction (a feature history, an operator graph, a shader graph, a mate) is a value the kind takes, and in which language the evaluator below runs it. Everything else on this page is a position with an exit that later sessions can test on a bench; this one binds the substrate and is Sid's.

**The open question this page carried**, the identity of a derived part, was worked on the bench in the fold and is Position 10: the face is named by the step that made it and comes back with a lineage and a frame; the attachment's rule says what the ink keeps; naming the face alone could not say where the ink belongs. It sits exactly at the seam between the store's identity contract and the executor, and it is the crack the 2D round would have found late. What the bench did not reach: a real kernel's history for a fillet or a boolean of curved faces, a merge where both faces carried ink, a curved face. And before the split, the precursor the reviews of 6f62711 put first and session 1 carried, which this page dropped when it built the split: one stroke across a parameterization seam on a curved mesh; reparameterize the face without changing the intended surface; the stroke must stay where its attachment says and the pick through the page must return its original identity. It isolates the coordinate mapping from the topology change, so the boolean then adds only the question of which support survives; bench 2 has the second half without the first, and it goes before the merge clause on the owed list, the definer's to construct. **The exact question that remains**, as the review Sid pasted put it and this page adopts: what must a construction return about part lineage and coordinate correspondence so a saved attachment rule can preserve, clip, split or explicitly detach the stroke when its support changes? For the box the answer is a lineage relation and a frame per face, and the rule reads them. For a kernel the shape is OCAF's history (generated, modified, deleted; FIELD), and the part it must add is the parameter map for a modified face, which is what the rule needs and what a heuristic cannot give; where the kernel has none, the lineage says ambiguous and the rule has a clause for it. **The next construction** after that is the one the other lane and the review of the working model asked for: an implicit or sampled object the engine does not know as a hardcoded kind, its saved definition, the operations it supplies, the kernels they call, and what its editable hit addresses when there is no face to name; it tests the limit of "a new tool needs no engine change", which is Position 9's other half.

## Folded, and from whom

Read in the fold, after the page was written: the review Sid pasted of a85fd1e (two bench probes, executed; the three-rule counterexample; the three requests), the review filed as `feedback-a85fd1e.md`, the review and receipts of 6f62711 (`feedback-6f62711.md`, `review-6f62711.md`), the Codex lane's working model and the feedback on it, the object-space contribution and its handoff, session 1's page and bench handover, and the definer's attack 2 on the path kind. Counterexamples at full weight; rankings none from this chair, because each model family ranks its own higher (Sid) and this chair is one of the families. Whoever ranks: framing and the whole picture first, details as a fix list (Sid, 2026-09-05).

- **Adopted from the review Sid pasted.** One question protocol, not one answer set (the representation table: open mesh, solid, density field, point capture). The picture rule rewritten: a rendered picture does not replace the editable meaning of the thing pictured. Source tolerances in the record. Three crossings, not one primitive. One waist with libraries beneath it; the scheduler does not force two engines. The hit defined by meaning and policy, the CPU as reference. The two bench repairs (nib, framing), reproduced before the change. The three requests of the waist. The three attachment rules and the clip behaviour for the split; the exact question that remains.

- **Adopted from the review of this page (feedback-a85fd1e).** Capabilities and unsupported as answers across the whole set; agreement claims bounded to the coordinate relation; authored normals as intent; a saved render as a reusable value; lineage beyond the surviving face (split, merge, delete, ambiguous, with parameter maps); attaching to a step and to a face as one thing; the evaluator producing typed values for geometry, rendering and queries.

- **Adopted from the Codex lane's working model** (through session 1's fold and read in full here): the level table (source, construction, representation, occurrence, scene, view); definition versus occurrence; "surface" as three words; present, embed, derive; a gesture leaving the face as a declared behaviour; capture during a drag; the merge caveat; coverage declaring what it reads as; scheduling as a capability; the extension question.

- **Adopted from the object-space contribution.** Query modes and candidates; visible hit, geometric intersection and intended selection as three questions; asynchronous answers never attaching a current identity to old pixels; depth incomparable across a portal; the portal's picture as emissive or reflectance by declaration; OpenExec; the topology-correspondence question with the boolean-split construction.

- **Adopted from session 1's page.** The rate row "on its own inputs" for derived pictures; render as a callable request that names its quantity; copies are two things; face identity does not come free; the three camera operations, measured on its bench (magnify: aperture doubled, projection identical); the filler callable with an anisotropic footprint; event routing at the seam. Folded later the same day, when Sid asked which line folds which and both chairs answered the same way: List A of session 1's own cross-read (`crossread-a0f20b1.md`), eighteen items its line held that this one lacked, at full weight where it is a definition or a counterexample. Frame-level: the order rule as the criterion for which kind a space is, and the picture question that calls itself (story 2); what must be code, said once (after the ceilings). The road: forward with MSAA with its flip conditions (the filler row); today at one notch, the renderer quantity that became the definition (after the today drawing). Rows: physics scope on the space (In); curves in space (the representation table); sweep generalises (the geometry row); sections keep their conics and reversed-Z as rebase's twin (the path-kind list); the portal's three mapping kinds and the mirror (the names list); hidden-line removal as an answer (the BIM row); the parity crack in the tree (Position 3, today); exposure on the camera (Sid's question on the surface); deformation on the axis (per time); the chart-seam precursor before the split (the closing section). The bench: bench 0 stays live as the receipt for the shadow's key and the three wheel operations. The fold found one correction on this page: the axis row had said an orbit re-encodes the shadow, and the lines say the opposite; session 1's page had made and corrected the same error under its reviews. Its words are translated in the names list. Its fact base, three hunter reads at file:line, stays the deeper anchor set for the region lane. Session 1's line closes here: its page and bench are folded in full and are not to be folded again.

- **Adopted from the definer's attack 2.** execute(recipe, inputs, state) → named results + next state as the evaluator's call; the surface interface beside geometry; the arrangement as a reusable geometry result with the face choice as data; authored sources, logical results and execution resources kept distinct; the 2D instance of the derived-part question.

- **Where this page holds, and why.** The space as the entity and the four nouns (Sid's words; every sibling agrees). The seam as a pair of maps, now over three crossings: no sibling states the downward half as a law and the bench shows what it costs to leave it out (the screenshot). Hover as a derived value with the bench's receipt (the sibling benches reached it after their reviews). Page zoom as magnification with the framing kept (the review Sid pasted carried this account forward). The CPU walker as the reference, no longer the definition. Session 1's "a space is a frame plus an order rule" is kept as the criterion for which kind a space is and held against as the noun: the walker and the packer branch on depth, units and a metric as well, so planar and spatial stay the two kinds.

- **Fix list, never rank penalties.** Bench 2: no shadow (bench 0 has one, with the counter that holds on orbit); no curved face and no chart-seam precursor; the exact nib but not the banded coverage lane; no recursion bound measured; no travel; no query modes; the split-and-cap rule and the merge clause not built; the record read-only. Not carried onto bench 2 from bench 0, which stays live for them: the surface execution with the lease ladder, the picture portal with a fixed camera, the GPU id read at the cursor as the parity check, the three wheel operations with their counters. Page: the today drawing still says "one BVH per region" for a solid that is now blocks on the bench (the tree's BVH is unchanged and CHECKED).

## Landed

Under `docs/below-the-waist/3d/`: this page and its `.md` twin (`3d-kind-2`), `fact-base-2.md` with every anchor, the fold's receipts (Part 11) and the fold record (Part 12), `bench-2/seam-bench.html` (live at the Seam Bench) with its handover, `HANDOFF-2.md` and `STARTER-2.md` for the next chair. The page was written with the siblings unread; the fold read them all and says what came from where. Later the same day session 1's line was folded here in full from its own List A and closed; the receipts and where each item landed are `fact-base-2.md` Part 13.

Softland · 3D kind · session 2, folded by its successor, session 1's line folded in and closed · 2026-09-06 · Positions are marked; the verdicts are Sid's. Every CHECKED line is anchored in `fact-base-2.md`.
