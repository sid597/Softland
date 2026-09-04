# Ceiling and waist — session ask-max-2, 2026-09-03

Exploration record, direction register. Nothing here is settled ground; Sid's word settles. The code receipts behind §7 are in `ceiling-and-waist-factbase.md` beside this file.

## 1. The question, in Sid's words (from the starter)

> "what is the smallest, right-formed set below the line such that everything above the line is reachable as data?"

> "The problem is not we need to make some set of things in code to use later … the problem is what needs to be coded with the assumption that we are saying anything that is not here can be done on ecs layer and the layer can go to infinite compositions."

> "So this is the ceiling. When you see the ceilings, do not try to hack through them. We want to build through different layers."

> "What makes these hard is the stacking, not any one primitive: derivation → derivation → flat marks, re-run on every edit. Waist question: does the waist accept only the bottom of that chain, and does the ECS run the chain?"

> Dead = "wrong form (wrong input shape, wrong output shape, wrong owner deciding the shape), regardless of call count."

Register correction mid-session (Sid, 2026-09-03): "this is not a session to where you try to figure out what the waste is and read all the code and try to determine from that. It's more higher level than that. It's about from the product or develop what to do next kind of thing … don't try to overoptimise for it." And: "what about these [the ceilings] you did not say anything about these the 10 were like very naive."

## 2. The answer in one screen

1. The waist is a promise to users and agents: this, you can make without a programmer.
2. The first promise, any mark you can draw you can save, share, and compose, has a settled shape: the imaging model print and the web converged on. Paths with curves, filled and stroked; glyphs from fonts; images; groups that transform and clip; color. Our four kinds are four of those in rougher clothes.
3. The second promise, anything you can compute from marks you can express, is the ceiling's question. The five ceilings answer it the same way: every derivation splits into data that configures and an algorithm that is built in. Constraints are data, the solver is code. Style expressions are data, collision is code. Layout properties are data, the layout engine is code. Keyframes are data, skinning is code.
4. So the ECS does not run arbitrary code. It holds configurations and the dependency graph as data, and a runner runs built-in algorithms over them. Infinite composition is infinite the way the web is: unbounded documents over a finite set of algorithms.
5. The waist accepts the bottom of the chain, plus two things that are not flat marks: groups with compositing attributes, and the camera as a two-way channel. Plus one speed exception where a derivation goes below: skinning.
6. "Run the chain" is four regimes, and the ceilings are those regimes at their limits: dataflow re-run on edit; tree layout with two passes; camera-driven, windowed, throttled; frame tick with state.
7. Softland now needs the first three and the collaboration law (only sources merge, derivations re-run). The fourth comes later and is the cheapest to add.
8. The open fork is not code versus formula. It is which algorithms ship, and whether agents can ship new ones. Softland already treats code as material, which is the escape hatch every long-lived tool grows.
9. What to do next at this altitude: settle the mark as data; hold the dependency graph as data with a runner that re-runs dirty nodes; build the connector between two shapes as the first derivation; then the box layout, which forces the tree regime and the compositing groups. The fork closes with a lived thing in hand, not on paper.

```
  edit
   → configurations + dependency graph        DATA (merges)
   → built-in algorithms, run by the runner    CODE, callable, four regimes
   → marks + compositing groups                DATA, exact, zoom-independent   ← the waist
   → flatten / shape / composite / present     ENGINE, zoom-dependent
   → pixels
   ↑ camera flows back up as data the chain may read
```

## 2b. The general things (Sid, 2026-09-03: "what general things to exist below the waist and everything else is more like composition")

Three nouns the ECS writes and the engine reads:
- **mark** — one row: header (id, revision, order, visible, pickable), transform and parent, compositing (clip, opacity, blend, filter), paint, and one payload. A mark with a children payload is a group; a frame with a fill is a mark and a group at once.
- **view** — the observer as data: camera, viewport, pixel ratio, time. The chain may read it; the engine reports through it what is visible.
- **event** — input as a stream: pointer, key, gesture.

Four verbs the engine performs:
- **draw** — marks and view to pixels, zoom-dependent, incremental by id and revision.
- **query** — bounds, hit, nearest, tangent over any mark in any space.
- **derive** — a pure, content-addressed function from data to data; the built-in algorithms are its first members.
- **run** — a dependency graph plus a dirty set to re-derived data, in the regimes a genre needs.

One law: **source or derived**. Sources merge; derived is a cache that re-runs; nothing edits derived directly.

Where generality bottoms out, and so where the product decisions live:
- the payloads of a mark: geometry, glyphs, image, mesh, external surface, 3D scene, children — seven, and the claim is the list is closed;
- the first derivations shipped: exact geometry ops, box layout with text inside, shaping and breaking, collision over bounds; later constraints and skinning.

Composition, four examples from the ceilings: a Figma component is marks under a group mark plus a layout derivation configured by properties; a connector is a derivation from two marks' queries to one mark; a chart is a derivation from a table to many instanced marks; a map is view to fetch to derive to marks. All the same few moves.

## 3. The five ceilings, each worked back

**CAD / EDA (KiCad, AutoCAD).** Chain: constraint graph → solver → exact geometry → booleans, offsets, fillets → snapping targets → marks, at millions of curves. Beyond the ten: exactness must survive the whole chain, so the geometry vocabulary must be closed under its own operations or declare a tolerance where it is not; the solver is iterative, so a node in the chain may converge rather than map; one edit re-runs one sub-chain and the engine takes a diff, never a scene. Says about the waist: marks at the bottom, a dependency graph as data, built-in exact geometry at the nodes, a runner that re-runs what is dirty.

**Page layout / a browser engine (InDesign, the web).** Chain: document tree → style resolution → box tree → layout in two passes, children sizing parents that size children, with text flowing across linked frames and around shapes → line breaking → positioned boxes and runs → stacking, clip, blend → marks plus compositing groups. Beyond the ten: layout is tree-recursive and bidirectional, which a flat dataflow graph cannot express, so layout is a built-in algorithm over a tree of data, the way CSS is data configuring an engine nobody writes as formulas; flow around a shape means one kind's derivation reads another kind's geometry; the output is a compositing tree, not only marks. Says about the waist: groups carry clip, opacity, blend, filter; the runner invalidates by subtree.

**Collaborative infinite whiteboard with embedded documents (FigJam, tldraw).** Chain: every genre, plus merge, plus foreign content, plus presence. Beyond the ten: only sources merge; derived marks are a cache recomputed per peer, so the data model must mark source versus derived and forbid editing derived directly; foreign content needs a surface mark the engine composites but does not draw; presence is ephemeral data through the same chain. Says about the waist: a foreign-surface mark, and the collaboration law on the chain.

**A map engine (Mapbox GL).** Chain: tiles fetched by view → style expressions, many of them functions of zoom → per-tile geometry → collision across everything visible → placement → marks, under continuous zoom and rotation. Beyond the ten: derivations legitimately read the camera, so the camera is data the chain depends on and the runner coalesces high-frequency input; inputs arrive by view, so the engine tells the chain what is visible and the chain streams marks in; collision is global over bounds. Says about the waist: the camera crosses the waist both ways, and the chain may be zoom-dependent even though the mark is not. Mapbox's style spec is a real declarative derivation language over hardcoded algorithms: the strongest evidence for "data configures built-in algorithms".

**A 2D game / animation engine (Spine, After Effects).** Chain: time → keyframes → bone hierarchy → skinning → mesh vertices → particles with state → effects → marks, every frame. Beyond the ten: the root input is time and the chain runs on a tick with a budget; skinning goes below the waist for speed, the mark carries bones and weights and the engine deforms; particles are stateful systems. Says about the waist: a mesh mark with a bone palette, a frame-tick regime with stateful systems, effects as compositor passes.

## 4. What must be built below

Seven shape decisions (the mark):

1. One geometry vocabulary: contours of line, quadratic, cubic, arc segments; a fill rule; open or closed. The mark carries it, booleans consume and produce it, hit and bounds query it. Curves below the waist, because CAD's "exact at every zoom" means the flattener runs per zoom below, not above.
2. One geometry, two paints: fill and stroke on the same mark; a stroke spec of width or width profile, cap, join, miter limit, dash, align, scaling. Ink is a stroke with a width profile and no fill.
3. One paint vocabulary owned by the engine: solid, linear gradient, radial gradient, image; one tagged color form.
4. One mark header owned by the engine: id, revision, container, order, visible, pickable; the engine diffs rows by id and revision.
5. Text is a glyph-run mark plus services: shaping, line breaking, Slug rendering; the renderer never runs layout.
6. The compositor takes groups as its unit: clip, opacity, blend, filter per group, targets from the pool; anti-aliasing for paths at the target.
7. Engine services: loop and clock; input events as data; one pick query and one bounds query over all marks; a mesh mark as the universal bottom; an instancing modifier on any mark.

Built-in algorithms, chosen by ceiling (the second half of the waist):
- exact geometry: flatten by tolerance, stroke offset, boolean, intersect, nearest, tangent, bounds
- a box layout with hug, fill, fixed, gap, padding, constraints, and text flowing inside
- shaping and line breaking (exist in part)
- collision and placement over bounds
- later: geometric constraint solving (CAD), skinning and particles (game)

Runner regimes: dataflow re-run on edit; tree layout invalidation; camera-driven, windowed, throttled; frame tick with state. The first three now.

The collaboration law: only sources merge; derivations re-run; a derived mark is never edited directly.

## 5. Sid's forest and planet questions, in order

- Set of kinds or one mark protocol: one protocol, engine-owned header, paint, group, modifiers; kind payloads under it. The tell today is the color adapter between path and region3d.
- Who owns the draw-item's shape: the engine owns the header and cross-cutting fields, each kind its payload sub-schema, the ECS conforms. Today nobody declares any draw-item; the harness is the de facto author.
- Curves: below.
- Stroke versus fill: one geometry, two paints.
- Bounds and hit: a contract every kind answers and one engine query. The engine half already exists uncalled in transform.
- Group tree: the registry with parent resolution is the render tree and is right; the ECS's richer tree materializes into groups and flat marks; add clip, opacity, blend at the group.
- Compositor: below; frame clipping is the first thing a Figma frame does.
- Time and input: engine services; events are data.
- Text source keys: wrong in the mark lane, right as the layout service's cache key.
- Reuse below: a cache, and the right one.
- The invariant: a thing is below the waist if it changes with zoom or if its correctness lives in code.
- Can the ECS run code: it runs built-in algorithms over configurations; new algorithms are code as material.
- Does collaboration constrain the waist's input: rows keyed by id with a revision, content-addressed derivations, no whole-frame keys, and the source/derived split.
- 3D: a portal kind, 2D primary, as today.
- The pinnacle: the mark contract is the same for all five ceilings; what differs is which algorithms ship first.

## 6. The ten genres (naive depth), pressure-tested

| genre | reachable as data once | today's shape that blocks it |
|---|---|---|
| pen | curves in the mark; stroke to outline; AA at the target; booleans for a pixel eraser | polyline stroke points; join fans overlapping the quads; aliased coverage; round only |
| vector tool | one geometry vocabulary with a fill rule; fill and stroke; a paint vocabulary; booleans callable | the ink/shape split; explicit hole roles; ear clip on simple polygons; flat color only |
| Figma component | the group tree (exists); clip on groups; bounds on every mark; the shared header; layout as a built-in | no clip; no bounds in path; text takes one string, one face, one size; four headers |
| document canvas | glyph-run mark; shaping (exists); line breaking over runs; the line map as its side table | layout's fused input; the mark is a layout line by reference; the renderer runs layout |
| diagram | bounds and tangent queries; curves; markers as marks; the runner | no bounds, no tangent, no curves, no runner |
| charts | instancing; per-mark color; id-wise update | color in every vertex; whole-frame repack; non-instanced draw; nested-vector meshes |
| map | screen-space flag (exists); continuous tolerance from zoom; per-glyph transform; collision over bounds | three lod buckets; glyph 2x2 is inverse size only; no bounds |
| raster in vector | a paintable texture source; clip and blend per group; filters as passes | image sources immutable by digest; no blend, clip, or effect pass |
| animation | loop and clock; keyframes as a system; per-row update | no loop, no time, no input anywhere in the client |
| 3D | the portal as today; shared header and paint; tolerance from projected scale | placement zoom fixed at 1.0; the color adapter; two vertex formats for one mesh |

## 7. The existing code, by form (build register; anchors in the factbase)

Verdicts are on shapes and owners. An algorithm with the right input and output is re-homed under the right shape; nothing is kept in a wrong shape waiting.

path/* — all four files go. component: the ink/shape split, polyline geometry, required per-point and per-contour ids that nothing below reads and that enter the content hash, a kind-owned header and color form. frame: a whole-frame key. tessellation: wrong input (polylines, lod buckets), right output contract (geometry-only local mesh, caller-owned content cache) in the wrong representation; ink joins overlap; ear clip needs simple polygons. renderer: lifecycle right, packing wrong (color per vertex, whole-frame repack, non-instanced). Re-homed: validate at ingress; canonical and hash as engine code; the tri-state query contract; the mesh contract and cache as typed arrays; ear clip as the terminal step; init, prepare, draw, destroy.

text/* — shaped_line, shaper, fonts stay (right form). layout_planes: right representation, wrong owner; becomes the glyph-run mark's payload. layout goes: string in, one face, one size, document keys and zoom in a zoom-independent system and in its identity; the mark is a layout line by reference. Re-homed as a service: line breaking over shaped lines; caret, selection, hit as queries over positioned glyphs; a content-keyed cache. glyph_pack: technique right, input contract wrong; per-glyph 2x2 is inverse size only. renderer: Slug right; the inline layout call, the layout knobs, and font-texture sharing between systems go; destroy and the draw function are uncalled and right-form, alive.

engine/* — schema, limits, buffer_pool stay. transform stays; the test-fixture index shim goes; the uncalled bounds functions are alive. device: right, with the camera and group structs to be declared once here instead of in three kinds, and the string-replaced color constant to go. color: the tagged spec and the linear premultiplied resource stay; the legacy resource, the boolean, and default-off go. rungs, compositor, leases: mechanism right, vocabulary speaks region3d; leases' id-keyed reconcile is the row diff every lane needs.

image: right as a texture-rect mark; a third header; a whole-frame key. region3d: right as a portal; fixed placement zoom; its own color adapter; a second vertex format for the same ink; a fourth header.

## 8. Questions not yet asked

- Systems are code as material: where do they live, who versions them, can an agent add one at runtime, and do they merge like values or like git.
- Scene per frame or a change stream: today three kinds hand the engine a scene and key the whole frame.
- Are the built-in kinds literally ECS component types: if yes, "below the waist" means shipped components and shipped algorithms.
- Precision on an infinite canvas: pan and zoom are applied in f32 after the group affine; drift far from the origin is a structure claim needing a probe.
- Determinism as a collaboration requirement: same geometry or same pixels across machines.
- Is the editor chrome marks: handles, guides, carets as screen-space marks means tools are systems.
- Mutable sources: a raster brush needs a texture that changes; every image source today is a digest.
- Baking versus expression: a frankenstein saved as geometry or as the union of its parts.
