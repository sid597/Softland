# Ceiling and waist — exploration record (2026-09-03 → 2026-09-04)

Exploration record, direction register. Nothing here is settled ground; Sid's word settles. Two sessions wrote it. Session ask-max-2 (2026-09-03) asked the question, worked the five ceilings back, and sorted the existing client code by form. The 2026-09-04 session built the below-the-waist ladder, took one falsification round against it, and received Sid's build method. The code receipts behind §10 are in `ceiling-and-waist-factbase.md` beside this file. The 09-03 version of this file is the commit before this one; git is the time axis.

## 1. The question, in Sid's words

2026-09-03, from the starter:

> "what is the smallest, right-formed set below the line such that everything above the line is reachable as data?"

> "The problem is not we need to make some set of things in code to use later … the problem is what needs to be coded with the assumption that we are saying anything that is not here can be done on ecs layer and the layer can go to infinite compositions."

> "So this is the ceiling. When you see the ceilings, do not try to hack through them. We want to build through different layers."

> "What makes these hard is the stacking, not any one primitive: derivation → derivation → flat marks, re-run on every edit. Waist question: does the waist accept only the bottom of that chain, and does the ECS run the chain?"

> Dead = "wrong form (wrong input shape, wrong output shape, wrong owner deciding the shape), regardless of call count."

Register correction mid-session (Sid, 2026-09-03): "this is not a session to where you try to figure out what the waste is and read all the code and try to determine from that. It's more higher level than that. It's about from the product or develop what to do next kind of thing … don't try to overoptimise for it." And: "what about these [the ceilings] you did not say anything about these the 10 were like very naive."

2026-09-04:

> "Is it enough to do the hardest 2d things"

> "are these current below the waist or on "the waist" the only below the waist layer we need that needs to be hardcoded or there are more primitives that are missing for this What are they and why?"

> "Anything above the waist is stored as data, is collaborative editable, createable, agents and humans can build freely over it without getting into git merge deadlocks, anything in ecs layer should be creatable and then saved for reuse or build higher order things from it"

> "So this is the ceiling. When you see the ceilings, do not try to hack through them. We want to build through different layers. The question we are asking is: what is the below-the-waist layer that should exist for everything?"

> "my 2 cents on how to approach this, and I would leave the method to you: work backwards, maybe for each thing."

After the falsification round, the method: "i would like to build bottom up the thinking process should be greater unlocks on both what can be done on the layer above this and how parallely they can be implemented i am least worried about above the waist getting it done ... defining how a scene would compose and that parts"

"did you fix the docs as well?" — the word that landed this revision.

## 2. The answer in one screen (as of 2026-09-04)

1. The waist is a promise to users and agents: this, you can make without a programmer.
2. Today's below-the-waist is the picture only: draw items in, pixels out. None of the five ceilings is structurally hard at the picture. Each is hard in the chain that makes the draw items, derivation feeding derivation, re-run on every edit. Today that chain has no home; the harness builds draw items by hand.
3. The one move that survived the round: delicate algorithms and tight loops go below; which-applies goes above as data. Three questions sort any thing: is it numerically delicate, is it a tight loop, is it what makes data runnable or mergeable. Any yes is below. All no is data.
4. The narrow thing is not a list of algorithms; lists grow with every ceiling. The waist is a value vocabulary every rung speaks plus an admission rule for an algorithm: pure, typed over the vocabulary, its algorithm version in its cache key, delicate or loop by the three questions. The vocabulary: scalar, vector, color, affine, path geometry with arcs as primitives, polygon set with fill rule, glyph run, mesh, table reference, entity id, causal stamp.
5. Below the waist reads bottom up: device · marks · tree · index · ops · language · runtime · store. The picture rungs need holes closed, not new kinds. Index, ops, language, runtime are the missing rungs; store is reshaped.
6. Rules are data in one small, pure, total grammar with a named GPU-eligible subset; a rule the GPU cannot take is rejected with a reason, never silently run elsewhere. Algorithms are ops, added by receipt. Code as material remains the escape hatch for new algorithms (the 09-03 fork on whether agents can ship algorithms is still open).
7. Tables come in three classes and only one merges: authored rows (identity survives, resolved and validated), derived bulk (dense cache, never merged, re-run), session signals (camera, pointer, clock, size, pixel ratio; never persisted). This is the 09-03 source-or-derived law with the session class named.
8. The collaboration target is no blocking and no silent corruption: a server-ordered log (Rama has it), per-kind resolution for optimistic client edits, validation at apply, atomic batches, a propose lane for agents. Partition-tolerant merge was the wrong frame.
9. The picture has quality ceilings of its own: precision at zoom, exact curves, glyph rasterization, temporal effects. They are picture-layer work, not chain work, and the 09-04 ladder wrongly waved them off.
10. Build bottom up. Each rung is justified by the unlock it hands the rung above and by how much of it can be built in parallel. Ops and the store's server half are lanes that start the day the vocabulary lands; the runtime waits until hand-wired chains beg for it. Above the waist waits for nothing.
11. Every placement below is a prediction until its atom runs. The "ceiling met" stamps of the 09-04 ladder are withdrawn as verdicts and kept as predictions (§6).

## 3. The ladder, and the translation to the 09-03 nouns

```
 ABOVE      authored rows · systems · templates + overrides (an undesigned runtime law)
 ══ THE WAIST = the value vocabulary + the op admission rule ══════════════════════
 STORE      ordered log · per-kind resolution for optimistic edits · validate at apply ·
            atomic batches · propose lane for agents
 RUNTIME    three table classes: authored | derived bulk | session signals
            systems · protocols with bounded iteration · eager | lazy | explicit
            residency per resource kind, drivers: visibility · dependency · camera
 LANGUAGE   one grammar · named GPU subset (fixed-arity maps) · reject, never fall back
            fixed GPU primitives as ops · async compile
 OPS        open list under the admission rule · sketch solver with diagnostics
 INDEX      world index (cull pick nearest pairs) · per-frame screen grid (place)
 TREE       transform world | screen | billboard, camera-relative precision
            draw order per mark (layer, then tree) · clip · composite + temporal buffers
 MARKS      path with arcs · image · text (positions, per-glyph, raster quality) ·
            mesh + skin · instances (several marks share one placement table) · foreign
            region → 3D viewport over a subtree: Sid's call
 DEVICE     as is
```

Translation, Sid's words first, then each session's names:

- Sid: "hardcoded primitives", "below the waist" → 09-03: the four verbs draw · query · derive · run, plus the engine-owned mark header → 09-04: the rungs device · marks · tree · index · ops · language · runtime · store.
- Sid: "ECS", "above the waist", "how a scene would compose" → 09-03: configurations plus the dependency graph as data → 09-04: authored rows, systems as rows, templates with overrides. Rows referencing rows; not this record's concern.
- 09-03 draw → marks + tree + device. 09-03 query → index. 09-03 derive → ops, which the language calls by name. 09-03 mark header → the value vocabulary.
- 09-03 run and its four regimes → runtime: dataflow re-run on edit = incremental systems; tree layout with two passes = protocols with bounded iteration; camera-driven, windowed, throttled = session signals + lazy or explicit systems + residency; frame tick with state = per-element maps that take the prior frame's row + fixed GPU primitives for spawn and compaction.
- 09-03 view and event → session signals. 09-03 source-or-derived → authored versus derived table classes.

## 4. The five ceilings, each worked back

**CAD / EDA (KiCad, AutoCAD).** Chain: constraint graph → solver → exact geometry → booleans, offsets, fillets → snapping targets → marks, at millions of curves. Beyond the ten: exactness must survive the whole chain, so the geometry vocabulary must be closed under its own operations or declare a tolerance where it is not; the solver is iterative, so a node in the chain may converge rather than map; one edit re-runs one sub-chain and the engine takes a diff, never a scene. Says about the waist: marks at the bottom, a dependency graph as data, built-in exact geometry at the nodes, a runner that re-runs what is dirty.
09-04 round: exactness at zoom is a picture problem the ladder had declared fine. Single-precision affines in the groups buffer jitter on a nanometer board at high zoom and WGSL has no double, so transforms go camera-relative in high precision before upload. A copper pour is a whole-layer boolean, batch not incremental, so systems declare eager, lazy, or explicit and a derived table may be shown stale with a marker. Layers cut across the tree (one footprint has marks on copper, silk, and courtyard), so draw order is a per-mark key of layer then tree position, not a per-group key. Rigid footprints need no group instancing: several marks share one placement table. Hierarchical instancing is a named gap. The sketch solver is its own op that reports degrees of freedom and conflicting constraints; inverse kinematics is not the same op.

**Page layout / a browser engine (InDesign, the web).** Chain: document tree → style resolution → box tree → layout in two passes, children sizing parents that size children, with text flowing across linked frames and around shapes → line breaking → positioned boxes and runs → stacking, clip, blend → marks plus compositing groups. Beyond the ten: layout is tree-recursive and bidirectional, which a flat dataflow graph cannot express, so layout is a built-in algorithm over a tree of data, the way CSS is data configuring an engine nobody writes as formulas; flow around a shape means one kind's derivation reads another kind's geometry; the output is a compositing tree, not only marks. Says about the waist: groups carry clip, opacity, blend, filter; the runner invalidates by subtree.
09-04 round: the ladder's "one linear solver plus rule templates" was wrong here and the 09-03 reading stands. Text height is a function of width, width comes out of the pass that needs height: a fixpoint with a pass order, not a linear program with priorities (Grid Style Sheets tried Cassowary for CSS and died). Below is a layout protocol, constraints down and sizes up with bounded iteration, plus break with a height budget and width as a function of y, plus measure. The solver serves the constraint-shaped parts, anchors and alignment. Rules are bounded functions in the grammar over children's measures, the Flutter and Yoga shape with the rule made pluggable. Footnotes iterate. Dependency granularity (per row versus dirty bits) is a receipt question, not a position.

**Collaborative infinite whiteboard with embedded documents (FigJam, tldraw).** Chain: every genre, plus merge, plus foreign content, plus presence. Beyond the ten: only sources merge; derived marks are a cache recomputed per peer, so the data model must mark source versus derived and forbid editing derived directly; foreign content needs a surface mark the engine composites but does not draw; presence is ephemeral data through the same chain. Says about the waist: a foreign-surface mark, and the collaboration law on the chain.
09-04 round: byte convergence is not meaning. Two agents each add a constraint and the union is over-constrained; two edit one rule and the result does not type. The target is no blocking and no silent corruption (§2.8). "Someone's edit may lose, visibly and undoably" is the wrong contract when the loser is a human and the winner is an agent's thousand-row batch. Foreign has two modes: video, canvas, and bitmaps import as textures and composite like images; a DOM element such as an iframe can only be an overlay positioned by the tree, never clipped or filtered, a browser ceiling. Mounting another document by global id needs revision pinning, permissions, and cross-document residency, none designed.

**A map engine (Mapbox GL).** Chain: tiles fetched by view → style expressions, many of them functions of zoom → per-tile geometry → collision across everything visible → placement → marks, under continuous zoom and rotation. Beyond the ten: derivations legitimately read the camera, so the camera is data the chain depends on and the runner coalesces high-frequency input; inputs arrive by view, so the engine tells the chain what is visible and the chain streams marks in; collision is global over bounds. Says about the waist: the camera crosses the waist both ways, and the chain may be zoom-dependent even though the mark is not. Mapbox's style spec is a real declarative derivation language over hardcoded algorithms: the strongest evidence for "data configures built-in algorithms".
09-04 round: tile features are derived bulk, not authored rows: dense, unmerged, thrown away on pan. Residency has several drivers, and one is dependency: the font must arrive before shaping can say whether a label is visible. Label placement is a per-frame screen-space grid that changes under rotation, distinct from the persistent world index. Mapbox's true shape is CPU evaluation at tile load with a whitelisted set interpolated on the GPU; that is the precedent for the grammar's GPU subset.

**A 2D game / animation engine (Spine, After Effects).** Chain: time → keyframes → bone hierarchy → skinning → mesh vertices → particles with state → effects → marks, every frame. Beyond the ten: the root input is time and the chain runs on a tick with a budget; skinning goes below the waist for speed, the mark carries bones and weights and the engine deforms; particles are stateful systems. Says about the waist: a mesh mark with a bone palette, a frame-tick regime with stateful systems, effects as compositor passes.
09-04 round: prior-frame state is a per-element map input (ping-pong buffers), so that part of the grammar holds. Spawn and compaction are not maps; they are fixed GPU primitives (compaction, prefix sum, sort) that data composes but does not author. Pipeline compilation on every rule edit is a real authoring cost in WebGPU and needs async compile-and-swap. Inverse kinematics is closed-form two-bone or a bounded iteration in the grammar, not the sketch solver. Composite and skinning stand.

## 5. What must be built below

Seven shape decisions (the mark), 09-03, standing:

1. One geometry vocabulary: contours of line, quadratic, cubic, arc segments; a fill rule; open or closed. The mark carries it, booleans consume and produce it, hit and bounds query it. Curves below the waist, because CAD's "exact at every zoom" means the flattener runs per zoom below, not above.
2. One geometry, two paints: fill and stroke on the same mark; a stroke spec of width or width profile, cap, join, miter limit, dash, align, scaling. Ink is a stroke with a width profile and no fill.
3. One paint vocabulary owned by the engine: solid, linear gradient, radial gradient, image; one tagged color form.
4. One mark header owned by the engine: id, revision, container, order, visible, pickable; the engine diffs rows by id and revision.
5. Text is a glyph-run mark plus services: shaping, line breaking, Slug rendering; the renderer never runs layout.
6. The compositor takes groups as its unit: clip, opacity, blend, filter per group, targets from the pool; anti-aliasing for paths at the target.
7. Engine services: loop and clock; input events as data; one pick query and one bounds query over all marks; a mesh mark as the universal bottom; an instancing modifier on any mark.

09-04 additions:

8. The admission rule for an op: pure; typed over the value vocabulary; algorithm version in its cache key; delicate or loop by the three questions. The op list is open under this rule; the vocabulary is what stays narrow.
9. The grammar: numbers, vectors, booleans, ids; arithmetic and vector math; comparisons and conditionals; read a component by id; local names; named parametrized functions; map, filter, reduce over a finite table. No recursion, no mutation, no I/O. A named GPU-eligible subset (fixed-arity per-element and per-pixel maps); the compiler rejects a rule from the GPU lane with a reason. Fixed GPU primitives (compaction, prefix sum, sort, convolve) are ops. Async pipeline compile.
10. Three table classes (§2.7). Systems declare eager, lazy, or explicit; a derived table may be shown stale with a marker. Protocols with bounded iteration for layout-shaped fixpoints.
11. Store: ordered log · per-kind resolution for optimistic edits (register, set, sequence for text, tree with acyclicity) · validate at apply (schema, rule typing, acyclicity, solvability where a solver is involved) · atomic batches · a propose lane for agents.
12. Templates with overrides, a runtime law, undesigned: an instance is a template reference plus an override map keyed by stable child ids; template edits propagate except where overridden; a structural edit that removes an overridden child orphans the override visibly.
13. Picture quality: camera-relative transforms computed in high precision before upload; draw order per mark (layer, then tree); a per-frame screen-space grid for placement; glyph rasterization quality; temporal buffers in the compositor; stroke width in world or screen units; per-glyph transform.
14. Instances: any mark times a placement table; several marks share one table. Hierarchical instancing is a named gap.

Built-in algorithms, chosen by ceiling, 09-03 with 09-04 amendments:
- exact geometry: flatten by tolerance, stroke offset, boolean, intersect, nearest, tangent, bounds, arc-length, band intervals (polygon against a horizontal band, for wrap)
- layout as a protocol: constraints down, sizes up, bounded iteration; the linear-priority solver for anchors and alignment; break with a height budget and width-at-y; measure (min- and max-content)
- shaping and line breaking (exist in part)
- collision and placement over bounds, in screen space per frame
- later: the sketch solver with diagnostics (CAD); skinning, GPU compaction, and closed-form inverse kinematics (game)

Runner regimes, 09-03: dataflow re-run on edit; tree layout invalidation; camera-driven, windowed, throttled; frame tick with state. The first three now. In 09-04 terms these are runtime features, not separate runners (§3 translation).

The collaboration law, 09-03: only sources merge; derivations re-run; a derived mark is never edited directly. 09-04: plus §2.8.

## 6. The falsification round (2026-09-04) and the rulings

Source: a review Sid pasted on 2026-09-04, written from general knowledge of the outside engines plus the two 09-04 texts, nothing read from disk. Rulings by the 09-04 session. The review's verdict words do not transfer; each finding was split into what it says about the want and what it says about the proposal as written.

- Three things called table. Concede. Authored, derived bulk, session signals; only the first merges; the join is a materialization. Kept: the edit-time versus frame-time partition still falls out of what a system reads.
- The merge law. Partly. Concede: byte convergence is not meaning; the target is no blocking plus no silent corruption; add validation at apply, atomic batches, a propose lane; retract the loser contract. Hold: per-kind resolution rules survive under a server order (last-writer-wins per property and server-side cycle rejection are a register rule and a tree rule); concurrent text still needs a sequence discipline. The partition-tolerance framing goes.
- Two backends for one language. Partly. Hold: prior-frame state is a map input. Concede: spawn and compaction are fixed GPU primitives; variable-arity derivation is CPU work; pipeline compile on rule edit is a real cost; one grammar with a named GPU subset and rejection with a reason.
- Platform, not waist. Concede, and it strengthens the design: the op list is open, so the waist is the value vocabulary plus the admission rule. Arcs are in the geometry. Some listed ops are compositions (fillet, justification, bidi), some new (straight skeleton, Delaunay, simplification), one policy-laden (push-and-shove); the three questions still sort them. Differential dataflow is the precedent name for auto-tracked incremental tables.
- Page layout. Concede (§4).
- CAD. Concede precision, batch pours, layer order, solver diagnostics; partly footprints (§4).
- Map. Concede: derived bulk, residency drivers, two indexes (§4).
- Game. Partly: prior state holds; compaction and inverse kinematics conceded (§4).
- Whiteboard. Concede: mounting undesigned; loser contract (§4).
- Region. Concede the test result: a scene of objects, placements, and materials inside one mark is a second scene graph, which the third question says is data. The unifying form is a 3D viewport group with camera and projection over 3D marks in a subtree. Region is accepted, built work; whether and when is Sid's call.
- Skipped. Concede: picture-layer quality ceilings; templates with overrides as a runtime law; receipts. Hand-building in the harness is the hack as a permanent form and the instrument as a probe.
- Stamps. Every "ceiling met" in the 09-04 ladder is withdrawn as a verdict and kept as a prediction.
- Held against the round: magnitude claims on both sides (dirty bits versus fine tracking, "tracking a million rows costs more than recompute") are unprobed and stay hypotheses.

## 7. The build map (Sid's method, 2026-09-04)

Bottom up. A rung is built only when its unlock on the rung above is named in advance and its parallel cut is known. Each bracket is one atom; lanes run in parallel; time runs left to right.

```
 lane          now ───────────────────────────────────────────────────▶ later
 V vocabulary  [value types + op admission rule: one document, first]
 P picture         [instances][arcs in path][per-glyph xform][stroke unit][foreign][mesh+skin]
                   [order per mark][clip][billboard][camera-relative precision][rotation]
                                                            [composite: offscreen, blend, filters]
 I index                                          [world index: cull pick nearest pairs]
                                                                        [screen grid: place]
 O ops             [geometry: boolean · offset · outline · intersect · arc-length · band intervals]
                   [text: break with height budget · measure]
                   [solve: linear-priority]  [sketch solver with diagnostics]
 L language                        [grammar · typecheck · CPU compiler]──────[GPU subset compiler]
 R runtime                                                        [tables · systems · protocols · laziness]
 S store       [server on Rama: validate at apply · atomic batch · propose lane]───[client resolution]
```

Rung by rung, the unlock above and the parallel cut:

- Vocabulary. Unlock: every atom in every lane is typed against the same dozen values. Cut: one document, not an atom; it carries the arcs decision because the path renderer and the intersect op both need it on day one. This is the contract in Sid's process.
- Picture. Unlock for the index: bounds per instance and a transform per group. Unlock felt: a hundred thousand pads, a road name on a bend, a frame that clips, a layer that blends. Cut: eleven atoms against the device rung alone; composite is the largest; region as a 3D viewport is a twelfth if Sid calls it.
- Index. Unlock: the first click that lands on anything; cull makes the million draw; nearest and pairs feed snapping and rule checks; place feeds labels. Cut: two atoms, world index and screen grid; waits on bounds only.
- Ops. Unlock for the language: the functions a rule can call. Unlock felt, hand-wired in the harness: text wrapping a shape, a copper pour, snapping candidates, a name along a curve. Cut: the widest lane; every op is pure, CPU, and by the project's law a JVM-tested `.cljc` with no browser; geometry, text, solve are independent sub-lanes and each op is its own atom. Starts the day the vocabulary lands.
- Language. Unlock: rules stop being Clojure. Cut: grammar, typechecker, CPU compiler as one atom; the GPU subset compiler waits for the picture lane's shaders.
- Runtime. Unlock: the hand wiring dies; one edit recomputes locally; templates with overrides become possible. Cut: the least parallel rung; starts only when the ops lane's hand-wired demos have made the wiring visibly painful. That pain is its receipt.
- Store. Unlock: many hands. Cut: the server half runs on Rama from day one, three atoms; client-side resolution waits for the runtime's authored table class.

Cross-lane pairings are the felt demos and the real receipts: instances plus cull is the million; arcs plus intersect is exact snapping; per-glyph transform plus arc-length is text on a curve; band intervals plus break-with-budget is wrap around a shape; pick plus nearest is the first working tool. Each is one afternoon in the harness with hand wiring.

First move under this method: the vocabulary document, and the same week the picture lane and the ops lane in parallel, with the store's server atoms as a third lane if wanted. The language waits for the vocabulary. The runtime waits for pain. Scene composition above the waist waits for nothing.

## 8. Sid's forest and planet questions (09-03), with 09-04 amendments

- Set of kinds or one mark protocol: one protocol, engine-owned header, paint, group, modifiers; kind payloads under it. The tell today is the color adapter between path and region3d. (09-04: the header is the value vocabulary, §2.4.)
- Who owns the draw-item's shape: the engine owns the header and cross-cutting fields, each kind its payload sub-schema, the ECS conforms. Today nobody declares any draw-item; the harness is the de facto author.
- Curves: below. (09-04: arcs as primitives, confirmed by the round.)
- Stroke versus fill: one geometry, two paints.
- Bounds and hit: a contract every kind answers and one engine query. The engine half already exists uncalled in transform. (09-04: two indexes, world and per-frame screen.)
- Group tree: the registry with parent resolution is the render tree and is right; the ECS's richer tree materializes into groups and flat marks; add clip, opacity, blend at the group. (09-04: draw order is per mark, layer then tree; camera-relative precision.)
- Compositor: below; frame clipping is the first thing a Figma frame does.
- Time and input: engine services; events are data. (09-04: session signals, never persisted.)
- Text source keys: wrong in the mark lane, right as the layout service's cache key.
- Reuse below: a cache, and the right one. (09-04: templates with overrides is the reuse law above, undesigned, §5.12.)
- The invariant: a thing is below the waist if it changes with zoom or if its correctness lives in code. (09-04: the three questions, §2.3.)
- Can the ECS run code: it runs built-in algorithms over configurations; new algorithms are code as material. (09-04: rules in the grammar, validated at apply; algorithms as ops by receipt; code as material stays the hatch.)
- Does collaboration constrain the waist's input: rows keyed by id with a revision, content-addressed derivations, no whole-frame keys, and the source/derived split. (09-04: causal stamps, validation at apply, atomic batches, propose lane.)
- 3D: a portal kind, 2D primary, as today. (09-04 position: a 3D viewport group over a subtree; Sid's call.)
- The pinnacle: the mark contract is the same for all five ceilings; what differs is which algorithms ship first. (09-04: and the hardest part of all five lands on the same three rungs, ops, language, runtime.)

## 9. The ten genres (naive depth), pressure-tested (09-03)

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

## 10. The existing code, by form (09-03; build register; anchors in the factbase)

Verdicts are on shapes and owners. An algorithm with the right input and output is re-homed under the right shape; nothing is kept in a wrong shape waiting.

path/* — all four files go. component: the ink/shape split, polyline geometry, required per-point and per-contour ids that nothing below reads and that enter the content hash, a kind-owned header and color form. frame: a whole-frame key. tessellation: wrong input (polylines, lod buckets), right output contract (geometry-only local mesh, caller-owned content cache) in the wrong representation; ink joins overlap; ear clip needs simple polygons. renderer: lifecycle right, packing wrong (color per vertex, whole-frame repack, non-instanced). Re-homed: validate at ingress; canonical and hash as engine code; the tri-state query contract; the mesh contract and cache as typed arrays; ear clip as the terminal step; init, prepare, draw, destroy.

text/* — shaped_line, shaper, fonts stay (right form). layout_planes: right representation, wrong owner; becomes the glyph-run mark's payload. layout goes: string in, one face, one size, document keys and zoom in a zoom-independent system and in its identity; the mark is a layout line by reference. Re-homed as a service: line breaking over shaped lines; caret, selection, hit as queries over positioned glyphs; a content-keyed cache. glyph_pack: technique right, input contract wrong; per-glyph 2x2 is inverse size only. renderer: Slug right; the inline layout call, the layout knobs, and font-texture sharing between systems go; destroy and the draw function are uncalled and right-form, alive.

engine/* — schema, limits, buffer_pool stay. transform stays; the test-fixture index shim goes; the uncalled bounds functions are alive. device: right, with the camera and group structs to be declared once here instead of in three kinds, and the string-replaced color constant to go. color: the tagged spec and the linear premultiplied resource stay; the legacy resource, the boolean, and default-off go. rungs, compositor, leases: mechanism right, vocabulary speaks region3d; leases' id-keyed reconcile is the row diff every lane needs.

image: right as a texture-rect mark; a third header; a whole-frame key. region3d: right as a portal; fixed placement zoom; its own color adapter; a second vertex format for the same ink; a fourth header.

## 11. Questions not yet asked

Answered since 09-03, by pointer:
- Systems are code as material, do they merge like values or like git → rules in the grammar are rows that resolve as registers and are validated at apply (§2.8); new algorithms are ops added by receipt, code as material stays the hatch (§2.6).
- Is the editor chrome marks → yes; tools are systems over pointer and pick (§7).
- Precision on an infinite canvas → a picture-layer item (§5.13), still a structure claim until the picture lane's precision atom runs.

Still open from 09-03:
- Scene per frame or a change stream: today three kinds hand the engine a scene and key the whole frame.
- Are the built-in kinds literally ECS component types: if yes, "below the waist" means shipped components and shipped algorithms.
- Determinism as a collaboration requirement: same geometry or same pixels across machines.
- Mutable sources: a raster brush needs a texture that changes; every image source today is a digest.
- Baking versus expression: a frankenstein saved as geometry or as the union of its parts.

New from 09-04:
- Hierarchical instancing: a footprint of footprints; the transform pass instancing whole groups is a change to its tight loop.
- Override identity when a template's structure changes: which override survives which edit.
- Pipeline compile latency while authoring GPU rules: async compile-and-swap, or a uniforms-driven interpreter in the shader.
- Dependency granularity, per row versus dirty bits: a receipt question for the runtime's first atom.
- Mounting another document: revision pinning, permissions, cross-document residency.
- Region as a 3D viewport group over a subtree: Sid's call.
- Which chain is the first felt demo: wrap around an exclusion (ops lane) or a hundred thousand pads (picture lane); under the build map both run, order is Sid's.
