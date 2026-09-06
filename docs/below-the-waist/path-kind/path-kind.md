Markdown twin of path-kind.html, generated from the page; the three drawings live in the html. Landed 2026-09-05, regenerated 2026-09-06 (session 10).

Softland · client · path kind · 2026-09-06 · sessions 12, 11, 10, 9, 8 and 7 folded, the definer's attacks 1, 2, 3 and 4 folded, Sid's two late questions worked

# The Path Kind
Ink strokes and shapes, read at the level of inputs and outputs: what the kind takes today, what comes out, and what the stored form of a stroke should be so that every 2D tool above the waist can be data. Exploration phase: framing and the whole picture first; details are a fix list.

**Fence** src/app/client/ only **CHECKED** a hunter read the lines · **DERIVED** from checked lines · **FIELD** from the literature, unverified here · **POSITION** ours, awaiting Sid

## What is decided and what is proposed
Sid's words with times, kept apart from positions so neither session promotes or demotes them. Everything else on this page is a position.

**Decided · 2026-09-05 08:38 · recall caca9242**

"Previously we did it in the format the GPU can consume, and now we need a format that a curve can consume. That's sorted."

**Adopted as a working basis, open to attack · 09:54 · the session starter**

"Working basis I have adopted and want attacked rather than assumed: the standard imaging model that has held since PostScript … Boundary and fill are independent declarations; the code must not presume fill." Also the fence: "Do not read any docs or code files outside client/."

**Ruled · about 16:50**

"yeah fuck the correctness and penalising it … framing and how to think is much more important in exploration phases."

**Ruled · about 17:10**

"its going to be 8 and 6" — the team. 8 holds the picture; 6 holds the definitions.

**Said · 2026-09-06 about 02:00 · on session 9's recap (his words; not rulings, the picture's next work)**

On clips, blend modes and layer stacks, told the page says "on top" while Vello and Skia carry them in one pipeline: "so then why we not doing it we should do this too imo" and "We should be ahead of everyone or at the very least adopt the best in field." On tools as data, told no 2D tool team does it: "Yeah i know thats why softland exists those tools are dinasarus". On the executor, told the precedent is Houdini and geometry nodes, one executor over a small set of code nodes: "ooooooo nice but one executor idk seems like the constraints and hard part would be getting them in line throughput seems small...." Session 10 worked the first and the third: Positions 9 and 8 are the answers, his to rule on.

**Not decided**

Positions 1 to 9 at the end. Position 7, what a varying width means, is the open fork and is asked once, verbatim and timestamped. Positions 8 (the executor) and 9 (the compositor) are asked now, together, because his two late questions are theirs. This morning's "three tiers" and "stroke is a fill of an offset region" were Claude's replies (uuids 6b009826, 28d31058, 418cb10c), not Sid's words.

## [position] The path kind as it should be
Two lines, not one. The waist is where the records stop and the executing starts: above it, data anyone can make; below it, a source stage of code whose general member is the executor. One step lower is the kind's input, a path with a paint declaration and an identity, and under it four pieces of code that exist once: the geometry, the packer, the filler and the compositor. Six pieces in all, each named by what comes out of it, and the list closes there: a path, a region, an answer, coverage, a surface, a record.

[FIGURE: see path-kind.html for the drawing]
Caption: **Should be.** Five kinds of record make one camera-free value: path plus paint plus identity. The waist is one step above the value: records are data, the source stage is code, and the executor is that stage's general member, running a saved construction and binding what the pieces return. Under the value, four pieces of code: the geometry that makes regions and answers, the packer that lowers them at the camera's tolerance, the filler the text kind already runs, and the compositor that owns surfaces, where a clip, a blend, a layer and a brush's read are one contract. Nothing sits "on top"; nothing sits "beside".

## The story, read off the picture
- Everything on the canvas ends as one question the GPU answers per pixel: how much of me is inside this region, and what colour goes there.

- At that boundary a pen stroke, a rectangle, a designer's outline and a letter are one kind of thing: a path, a chain of curve pieces, open or closed. A pen, a formula, a designer and a font file are sources, not kinds.

- A path says where the boundary goes, not what to do with it. Fill, stroke, clip and hit-test are separate declarations against one path.

- Stroke is an operation, not a second geometry: sweep a round nib along the path, its width allowed to vary; the region is the union of every disc it covered. That region is filled.

- Its skin folds over itself on the inside of every bend and at every join, so it is always filled nonzero. That is what turns sloppy overlapping pieces into one region.

- So the filler only ever fills regions. Skin-making is the hard geometry, pure and camera-free. Today's tapered quads and fans are a first version of it, delivered in the wrong output.

- A region is resolution-independent. Triangles, pixels and how many pieces a curve becomes depend on the camera and live on the frame side, never in the stored value.

- Today the code stores the pen's samples and reads each as a corner, bakes cap roundness at three zoom bands into the cache key, and paints the pieces separately, so translucent ink darkens where it crosses itself.

- Zoom already is a camera move in the shader; the only recompute is the band. Nothing stored or cached should know the zoom.

- Text already lives in the model: outlines stored once as curves, coverage per pixel. Its shader knows nothing about glyphs. What it lacks is a packer in the client and the stroke operation.

- The imaging model is the right language for the output and the wrong language for storing ink: its stroke has one width and a pen has one per sample. PostScript's stroke is the swept tip that never changes.

- Where the model is only a default: brushes that darken where they overlap, which are ordered dabs through the same filler, with surface state beside only for brushes that read the surface; crisp one-pixel UI borders, a snapping policy; very long thin strokes, a cover policy.

- Six pieces of code exist once, and the count is closed because each is named by its output. Above the value, the source stage: builder, generator, arrangement, reader, and the executor that runs a saved construction. Under it: the geometry (regions and answers), the packer, the filler text already runs, and the compositor (surfaces). Everything else is data: tools, brushes, tips, shape sets, fill rules, clips, layers, recipes.

- Clips, blend modes and layers are not on top of the picture; they are the compositor's contract. A clip is a region whose coverage multiplies in the same draw; a blend reads the backdrop under the region; a layer is a surface a group paints into and then paints as one region into its parent, allocated only when the group needs isolation. The screen is the root surface. A brush that reads what it painted calls the same surface. That is what the best pipelines carry in one pass, and the tree already has half of it: a target pool, a present pass, region3d's quad composite.

- The executor is a vocabulary and an order, not one interpreter. It moves values between capabilities and keeps the sequence a construction declares; every heavy thing happens inside a capability, per pixel on the GPU or per point in native code, and instances are many. The order between steps comes from the dependencies the construction states; a surface read before it is written is the one that forces an order between dabs, and that is the compositor's, bounded to brushes that read. The same stated dependencies say when a result's storage may be reused: after its last read, never before; and a result inside a returned record is read wherever the record is, so liveness follows how values compose, not how they are packaged. A record may also name another record's result by output, and resolving that name is the executor's, once per edit of the producer (attack 4).

- The contract, then. In: path, paint (with a clip, a blend, the group's opacity), identity; beside it a construction's bindings and, for a tool that reads paint, a surface with a resolution. Out: regions to the filler, surfaces from the compositor, and answers from the same path: hit, distance, bounds, outline, flatten, evaluate, intersections, the arrangement.

- If the stored form changes, these change with it: the grammar, the answers, the envelope builder, the cache keys, both lanes that draw ink, the harness, the group record and the engine's compositor, and a builder, a band packer and an executor that do not exist yet.

## Input and output, spelled out
The value the kind takes and the two things it gives. This is what every session on the path kind should be talking about.

### In: path + paint + identity
```
path
  subpaths*          each: segments*, closed?
  segment            line | quad | cubic   (cubic in; quads at the packer)
  knot attributes?   width, opacity, pressure, time   (per knot, optional)
  knot ids           stable, so split and join keep identity

paint                two independent declarations, and what the compositor reads
  fill?              { rule nonzero | even-odd, colour, blend? }
  stroke?            { tip { construction nib | ribbon, shape,
                             size | per-knot width | width fn(pressure, s),
                             unit local | device },
                       cap, join, miter-limit,
                       align center | inside | outside,   (closed paths)
                       dash? [on, off, phase],
                       overlap union | accumulate { spacing },
                       colour, blend? }
  clip?              { path, rule }   a region with no paint; its coverage multiplies;
                     the path is an authored source built here, a derived path bound as a
                     value (frozen: nothing reconstructs it, nothing follows it), or a
                     reference { result, output } to a construction's returned path, which
                     the executor resolves once per edit of the producer and the packer
                     packs per scale (attack 4)
  neither            draws nothing; both = bordered shape

identity             material id, revision, container (group id)
group                { transform, opacity?, blend?, clip?, isolate? }
                     a layer is a group that needs isolation; nothing else costs a target
                     the group's clip masks the completed group where it is painted into
                     its parent; a child's own clip stays on the child (attack 3)

beside the value, never inside it (the definer, attack 2)
  construction?      { bindings, steps, state, each, return }  run by the executor
                     a binding may name another record's returned value by result and
                     output; the executor resolves it, and the dependent work follows the
                     producer's revision; the record's identity is what the name reaches
                     (attack 4)
  surface?           { id, revision, width, height, local→texel map, initial }
                     only for a tool that reads paint; the record names the revision it reads
                     a result is a value: key = the step that made it, parent = the surface
                     painted on, which keeps its contents; depth alone is not an identity
  checkpoint?        { at, load? }   a construction continuation: the state after `at`
                     items, every surface it reaches at any depth as a reference to content
                     saved once with its map and colour, the carried values, the next item,
                     and what it depends on; resumed, never re-run. As bytes (load): an
                     explicit format carrying its dependencies, its continuation and its
                     resources under a named encoding, checked on load and refused with a
                     reason when any no longer holds; a changed interpretation is a
                     conversion said out loud or a failed load, never the destination's
                     declaration supplied silently (attack 4)
tolerances           three, none in the value: the source's fit (in the
                     record), a consumer's request (a parameter of the
                     answers), the device's (the packer's)
```

Open path plus fill: the fill closes it implicitly, as PostScript does. Open or closed matters only to the stroke: whether a closing segment is drawn and whether the start gets caps or a join. A varying width means the swept round nib: the region is the union of discs of radius w(t)/2. Today's rule, perpendicular distance to the nearest centerline point against the width there, is a ribbon with end discs, an approximation that diverges at every taper. Width has a unit: a width in device pixels keeps the value camera-free but makes the envelope a per-scale quantity (session 9, the border walk). Alignment and dash are declarations the pen tools of the field carry; both are operations on the same path (bench 9). A nonlinear response is not the linear interpolation of endpoint widths (the definer, attack 1: interpolate the pressure, then square it); so the stroke may carry the width as a function of the interpolated source attribute, evaluated where the geometry needs it, and per-knot widths are the constant-between-knots case. Accumulate names ordered dabs, each a region through the same filler, painted in order; union names one region painted once. A clip is a path with a rule and no paint (session 10); the group's opacity, blend and clip are the layer stack's data, and a layer exists only when a group needs isolation: a translucency over overlapping children, a blend, a mask on the composite rather than on each child. A surface is a value beside the path, never inside it: an id, a revision, a resolution and a map; a construction that reads paint names which revision it reads (attack 2). No GPU handle enters any value.

### Out: two kinds of answer
```
to the GPU (per painted region)
  cover geometry     bbox rect, or the cells the region covers
                     (boundary cells and interior cells)
  curves             quadratics in local units, consistently wound
  bands              per row and per column: which curves cross it
  instance row       paint, group index, rule, blend, the group's clip chain
  ⇒ one coverage draw, resolved once, valid at any zoom

to and from the compositor (surfaces)
  paint(surface, region, paint, clips, blend) → surface   a new value with a key and a parent;
                                                          the input keeps its contents; storage
                                                          is reused only after a last read, and a
                                                          value reached through a returned record,
                                                          an array, the carried state or a return
                                                          is still read: liveness follows how
                                                          values compose, not how they are packaged
                                                          (attack 4)
  sample(surface, point, filter) → colour                 the backdrop; a brush's read
  a layer = a surface painted as a region into its parent; the screen is the root
  filter(surface, kernel) → surface                       effects, later

to the CPU / other kinds
  classify(point, slop) → :inside | :boundary | :outside
                          (winding with curves, same rule as the GPU)
  bbox (local)
  outline            the stroke's skin, as curves
  offset(path, d)    one side's edge; the ring between path and offset
                     is the inside or outside stroke (session 9)
  evaluate(t), tangent(t), length ↔ t    dashes, dabs, text on a path,
                     arrow caps (session 9; the definer, attack 1)
  intersections(a, b) → contacts* (seg, t, seg, t, crossing | touch)
                       + spans* ([seg, t0, t1] ↔ [seg, t1', t0'])   (attack 2)
  arrange(curves, coincidence) → the arrangement: pieces with source
                     intervals, half-edges ordered around vertices, faces
                     with boundary loops and holes, the unbounded face
  locate(arrangement, point, on-boundary) → a face | the boundary location
  face-boundaries(arrangement, face) → a path with provenance
  split(seg, t) with an id map             so a split keeps identity
  flatten(τ)         polyline or triangles, for mesh consumers; keeps
                     which segment and parameter each point came from
  dab packets        x, y, r, arc length, s, source segment + parameter,
                     pressure: what a recipe reads per dab (attack 2)
```

The GPU output is the shape of what the text renderer already uploads per glyph: a rect, curve and band data, a 100-byte instance row (CHECKED). The kind never emits anything that depends on the camera; the packer derives tolerance from camera, group and DPR itself. Membership (winding under the rule) is one definition shared by every implementation; boundary distance and pixel coverage are approximations of it, each compared against it at a declared error (the definer, attack 1). Answers return to constructions, not only to the renderer: a saved construction measures, splits, intersects and reuses before deciding to draw. The compositor's two operations are one contract for a clip, a blend, a layer and a brush that reads: paint and sample, the definer's surface interface (Position 9; bench 9 runs all four through them). Intersections return contacts and overlapping spans; the arrangement is an answer the geometry returns to a construction, which chooses the face by its own policy, a seed, an area, a neighbour (attack 2; bench 9's two network fixtures).

### When does a value change? That is the axis, not CPU versus GPU
| Changes… | Values | Where it may live | Today |
|---|---|---|---|
| per edit | path, paint, source record, identity; the arrangement, by its dependency graph (a moved vertex re-splits its partners' edges; a face choice reuses unchanged topology) | CPU; stored; uploaded once | stored as samples; cached triangles per zoom band |
| per brush step | a painting surface's content: each paint is a new value that keeps its parent readable; a committed dab is never re-applied by a frame; an edit before a checkpoint invalidates it and an edit after it does not, because the checkpoint is the construction's state after k items (the surface's content with its map and colour, the carried colour, the next item) plus what it depends on: the program, the record bindings it reads, the surface declaration, the item fields it binds, and the capabilities' behaviour (attack 3; bench 9 resumes the pickup brush from dab 12 to the same 65,536 components on each host). The same continuation leaves as bytes and comes back into fresh hosts: an explicit format with its dependencies, its continuation and each surface's content once, under a named encoding; a load checks the dependencies, the encoding, the length and the interpretation and fails with a reason (attack 4; bench 9 reloads the pickup brush's twelve dabs from 364,786 bytes to the same 65,536 components on the CPU twin and on the GPU) | the compositor's retained surface, at its declared resolution; the CPU twin composites the same way | nothing: no brush, no surface, no read of a target anywhere |
| per camera scale | flattening tolerance, cubic→quad lowering, bands, cover cells; the envelope of a stroke whose width is in device pixels | the packer's cache, keyed on projected error from the full transform | three hand-set bands; only cap roundness changes; whole frame repacked on a crossing |
| per frame | transform, coverage; snapping to the device grid when declared (it depends on the pan); a layer's surface when a group needs isolation (transient, the group's cover); a backdrop copy under each blended region | GPU, in practice; snapping is a repack of a small path at frame rate; layers from the target pool | zoom and pan already a 24-byte camera write; a target pool with a 512 MiB budget and region3d's per-lease MSAA targets (CHECKED) |

## The waist test: three real tools as data, one pipeline of code
Session 8 owed this. Three tools from the field, each written down as the record its own tool holds, walked through the picture from the code side: what executes, in which piece, and where a step needs code the picture had no name for. The definer walks the same three from the data side; the two chairs meet at the value. Everything below runs on bench 9, where each record is editable and every piece reports what it did and at which rate.

[FIGURE: see path-kind.html for the drawing]
Caption: **The test.** Three records above, one code pipeline below. The value in the middle is what the definer and the composer meet at. The source stage is code and sits above the kind: the waist is not the kind's input, it is one step higher, where the records stop and the executing starts.

### What the walk found, in plain words
- All three tools compile to the same value: a path, a paint, an identity. Nothing on any of the three records refused to become that value.

- Below the value, the same three pieces drew all three. The filler is the tree's text shader with two lines changed: the band texture width is a uniform instead of a compile-time 4096, and even-odd is the parity of the same crossings.

- The pen tool needed no code above the value. Its anchors already are a path. For a designer's tool the waist sits right at the tool.

- The draw tool needed the most: streamline, a pen model, a fit, a spline, an envelope. All of it is code executing the record's numbers, and one step is a function written as data, width from pressure. A function is data only if something below can run it. That runner is a fifth piece, and the picture did not have it.

- The border needed two things the contract did not say. The width has a unit, and a device-pixel width makes the envelope a per-scale quantity: the value stays camera-free, the geometry stage does not. And snapping is a policy that runs at frame rate, because it depends on the pan as well as the zoom.

- The pen tool's inside and outside strokes need one answer the list lacked: the offset of a path by a distance. The ring between a path and its offset is the aligned stroke, wound opposite so nonzero leaves the middle empty. No clip is needed for that.

- tldraw's own outline construction is the ribbon with round joins (FIELD: perfect-freehand offsets each sample perpendicular by its radius). To reproduce it as data the ribbon must be a construction the declaration can name. On the bench the two constructions are one line apart in the tracer, and the record picks. Position 7 stands as the default, not as the only meaning.

- No tool needed a new piece under the value except the evaluator. Everything else landed as a capability inside a piece that already had a name: offset and dash in the geometry, the unit and snap at the packer's rates.

### Every step that executes, and where it lives
| Step that executes | Piece | Needed by | On the page before | On bench 9 |
|---|---|---|---|---|
| samples → low-pass (tldraw's streamline) | source · builder | draw tool | named: the builder | yes |
| pressure, or speed when the device has none → width; taper along the length | source · evaluator | draw tool | not named | yes: an expression over p, s, v and the record's numbers |
| samples → knots at τ → spline through them | source · builder | draw tool | named | yes: decimate at τ, Catmull-Rom cubics; τ 0 keeps every sample; "polyline" is also a path |
| formula → path (a rect: 4 lines, 4 arcs as cubics) | source · generator | border | sources "not kinds"; the generator's code unnamed | yes; the generator is code. A parametric shape as data needs the evaluator |
| anchors → path | none | pen tool | — | a copy |
| envelope: tip nib or ribbon, caps, joins | geometry | every stroke | named; caps and joins "on top" | yes: one outline tracer, the two tips one line apart, joins fold through the pivot |
| stroke alignment: the ring between the path and its offset | geometry · offset(path, d) | pen tool, border | not in the answers | yes, on the flattened centerline |
| dash by arc length | geometry · length | draw tool's dashed style, pen tool | not named | yes |
| cubic → quad at τ; bands; cover | packer | all | named | yes; bands sorted by far edge so the shader's early exit holds; cells the outline touches plus interior cells |
| width in device pixels → envelope per scale | geometry, at the packer's rate | border | not named; "camera-free" stated without the unit | yes; the rate badge moves from per edit to per scale |
| snap knots to the device grid | packer · per frame | border | named as a policy on the paint or group | yes; the repack runs on every camera move and says so |
| coverage under a rule, one resolve per region | filler | all | named | the tree's shader |
| clip by a path (a mask) | compositor · in the filler's own draw | pen tool's "use as mask" | named as "on top" | yes (session 10): the clip's curves and bands ride in the same draw; coverage × clip coverage, per pixel; no mask texture |
| blend against the backdrop (multiply, screen …) | compositor · sample under the region | painting apps; Figma's blend modes | named as "on top" | yes (session 10): a copy of the target under the region, read by the program; over transparent nothing every mode is over |
| a group with an opacity or a blend: a layer | compositor · a surface painted as a region | every layer stack | named as "on top" | yes (session 10): a second surface, then one region into the root; 24 dabs at full alpha in a layer at 0.62 read 0.62 at the crossing |
| a brush that reads the paint under each dab, mixes, deposits | executor · compositor.sample, compositor.paint | the definer's pickup brush; smudge | "surface state beside", unnamed | yes (session 10): attack 2's record verbatim, on the GPU and on a CPU twin; dab 19's sample and carry equal on both |
| arrow caps: a shape placed at the end along the tangent | geometry · tangent(t) | pen tool | not named | no |
| vector-network faces | geometry · arrangement, locate; the executor chooses the face | pen tool's fill by face | "above the kind" | yes (session 10): attack 2's record verbatim; lines exact, curve crossings refined on the true curves; the face is an answer, the choice is the record's seed |
| a result bound as another record's input: a returned face as a mask | executor · a binding across records; packer · the same pack | the definer's face mask; Figma's use-as-mask on a computed path | "not yet general result reuse" | yes (session 12): a derived path bound as a value, or a live reference the executor resolves once per edit and the packer packs per scale; the crossing through the bottom face reads .8556 as dabs and .62 as a union, 0 through the top face; the definer's rect through his frozen face .6 inside and 0 outside |
| a surface returned inside a labelled record | executor · liveness follows composition; compositor · storage | a tool that returns its proofs in records; any value-shaped vocabulary | the compile pass saw the array, not the surface in it | yes (session 12): both hosts read the proof after the run; three targets, none released |
| a checkpoint reloaded from bytes into fresh hosts | executor · capture and load over the same reachability; compositor · content and resolve | a session resumed on another machine; a saved document | the in-memory checkpoint only | yes (session 12): 364,786 bytes leave the CPU twin and come back into fresh hosts, 0 of 65,536 components differ on each host against the saved bytes and against its own straight run; the same from the GPU; the definer's four edits refused or accepted with the same reasons |

### Where the two walks met
The definer walked the same three tools from the data side the same day (`attack-1.md`), with a fourth case, the harness's self-crossing stroke painted as a vector stroke, as an accumulating brush and as a smudge. Neither chair had read the other's. What both found is the picture's; what one found is folded at full weight.

| Finding | Composer (code side, bench 9) | Definer (data side, attack 1) | On the page now |
|---|---|---|---|
| A fifth piece runs the record's behaviour | an expression evaluator: width from pressure, taper, per sample | a construction executor: arithmetic, conditions, iteration, ordered accumulation, record updates, calls to geometry and surface; a return arrow from geometry to constructions | Position 8, both sizes; the return arrow in the drawing |
| The answers the tools consume | offset(path, d), length, tangent(t) | evaluate(t), length ↔ t, source correspondence, intersections as (seg, t, seg, t), splits with id maps | the contract's answers |
| Width between knots | per-knot widths, interpolated | interpolate the pressure, then apply the response: 16p² at the crossing's two visits is 9.65 and 11.61, not the interpolation of squared widths | width fn(pressure, s) in the stroke; bench 9 evaluates it per flattened point |
| Accumulation | a raster lane beside the kind | ordered dabs through the same filler: 24 dabs on the harness Z, alpha 0.8556 at the crossing; only a surface-reading brush needs state | the compositing row, story 12, Position 2, figure 1; bench 9's "the crossing, as dabs" |
| Tolerance | none in the value; the packer's | three: the source's fit, a consumer's request, the device's | the contract's tolerance line |
| Cover cells | boundary cells plus interior cells by winding | "cells the outline touches" drops a filled region's middle | renamed: cells the region covers |
| CPU and GPU | the CPU twin at the cursor against the GPU pixel | membership is the shared definition; distance and coverage are compared against it at a declared error | the contract's note, the cascade's classify row |
| The border | a stroke aligned inside, one device pixel, snapped | unequal side widths make the border a ring built from the box with CSS's inner radii, no centerline; per-side colours need the corner divided; a device width under shear needs the full transform | the table below, fix list |
| The pen tool | anchors are already a path | the network is the authored source: vertices, edges, faces as loops; moving a vertex needs incidence; discovered faces need intersections and splits; junction joins need a construction | the answers; "compile faces" stays a source's construction over them |
| Wet and dry | one region per pointer event | wet and dry must resolve as one region where they overlap or the crossing darkens during the handoff | both benches keep one region; the fix list |
| Over and under | stable knot ids | record the pair (AB, 38/75), (CD, 37/75) and the relation; split the display intervals, keep their map to the source | intersections and split in the answers |
| The envelope | the tracer folds inner joins through the pivot; nonzero absorbs them | a winding rule does not turn arbitrary folded contours into a sweep; correctly wound pieces are the builder's responsibility | the boundary row of the attack: "sloppy" sharpened to "consistently oriented" |

### The definer's attack 2, folded
Two constructions the first walk could not run: a brush that reads the paint under each dab, and a network whose faces are discovered, not stored (`attack-2.md`). The definer executed both in Node against the bench's own functions and wrote the numbers. Session 10 made both run on the bench itself, verbatim, and folded every finding at full weight.

| Finding | Definer (attack 2) | On the page and the bench now |
|---|---|---|
| The fifth piece executes a construction with named inputs, outputs and state, and calls into the host | Position A: the brush's three-step loop cannot run in a scalar evaluator; `execute(recipe, inputs, state) → results + next state` | Position 8 rewritten as the executor; bench 9 runs a sixty-line one over a capability table, the definer's records unchanged, the brush on two hosts |
| A surface interface beside geometry: sample and paint, with resolution, coordinates, content and an order | Position B: the same dab boundaries give different colours solely because the read version differs | Position 9: that interface is the compositor, and it is the same piece a clip, a blend and a layer need; the "brushes beside" box is gone from figure 1 |
| The arrangement as a reusable geometry result; face selection as an editable caller | Position C: intersections and splits must become incidence, order, cycles and faces before a caller can choose | the answers: arrange, locate, face-boundaries; the seed is the record's policy; bench 9: 4 faces of 2400, then 6 faces of 3800, 900, 3800, 900, 100, 100 with the cubic edge, the boundary alternating L and C |
| Authored sources, logical results and execution resources kept distinct; materialize what the next operation needs | Position D: pixels, or a replayable history; the framebuffer handle is replaceable | "surfaces are values" in figure 1; session 11: both hosts keep every result, the GPU painting in place only at an input's last use (the executor's compile pass says where), attack 3's two proofs agree on both |
| "Pen anchors already are a path" is scoped to a contour | a network has an authored graph and an executing construction before a boundary exists | the source row of figure 1: designer · network; the waist-test table's network row |
| Intersections must return contacts and overlapping spans; splits carry provenance; tangencies and ties in the tangent order are geometric work | the CA-copy edge (one geometric edge, two owners) and the touching quadratic | the answers; bench 9 merges exactly coincident edges and keeps partial spans, tangencies and ties on the fix list, said so on its panel |
| Dab packets carry the source location and attributes; the width function is evaluated at the dab, not interpolated between evaluated widths | dab 4 with size·p²: 4.677200212096 at the dab, 4.801338939016 interpolated | fixed on the bench (4.677200212096); the dab-packet line in the answers |
| An unconsumed field or an unknown operation is a finding; an empty drawing is not the tool having run | the Node check: the surface and program fields changed nothing; `read` was an unknown function | bench 9 lists the fields nobody read under the record and stops at an unknown operation, painting nothing |
| Rates: a surface advances with brush steps or an explicit replay; presentation with the camera; a committed operation is never re-applied by a frame | the rates paragraph | the axis table's per-brush-step row; the bench counts presentations since the run |
| The GPU route must keep an ordered read-before-write and never bind the target as its own input | the WebGL feedback rule; a copy or separate resources per pass | the bench copies the backdrop under the region on its own texture unit; the carry-on-GPU route is on the fix list |

### The definer's attack 3, folded
A result that survives its next use (`attack-3.md`): two paint proofs from one saved start where the GPU host turned the chosen red into blue; a checkpoint after twelve dabs that resumed the pickup brush to a byte-identical result on the CPU twin; a mask on a group whose scope the bench had nowhere to put. The definer ran the first through the bench's own record route and the third through its draw operations, and wrote the numbers. Session 11 made all three run on the bench and folded each at full weight.

| Finding | Definer (attack 3) | On the page and the bench now |
|---|---|---|
| A returned surface keeps its content meaning across later operations; physical storage is reused only after the last needed read | the GPU host returned one live target for both proofs: `red` sampled after `blue` was made read (0, 0, 0.5, 0.5); the CPU twin kept (0.5, 0, 0, 0.5) | the contract's paint entry; the executor's compile pass records where every binding is last read and tells paint when its input is consumable; the GPU host paints in place only then and otherwise copies into a pooled target; a read of a superseded value is refused, never served; both hosts now read red after blue as (0.5, 0, 0, 0.5) |
| A revision counter measures depth in a chain and does not identify branches | both proofs were `{ id: proof, revision: 1 }` with different pixels | every result carries a key (the step that made it: `proof:0/red`, `proof:0/blue`) and its parent (`proof:initial`); the revision stays as depth; the contract says so |
| A checkpoint is a construction continuation: the surface and the carried colour and the next position, valid while the prefix's inputs are unchanged | dabs 0–11 retained, dabs 12–23 continued: zero differing components; the reusable object needs the content, its map and colour, the carry, the next ordinal, and the versions of source, recipe and capabilities | the axis table's per-brush-step row; bench 9: `checkpoint.at`, a dependency key over the program, the record bindings the construction reads, the surface declaration, the item fields it binds and a capabilities string; taken on each host from its own prefix, resumed through a content resolver, compared with the straight run (0 of 65,536 differ on each host); moving the last pen sample keeps it, moving the first invalidates it at dab 0, renaming the tool keeps it |
| A group mask applies when the completed group is painted into its parent; clipping each child is a different operation | .62 unmasked, .465 clipping each child, .31 masking the group at a half-covered edge; `group.clip` had nowhere to go | the contract's group entry; bench 9 compiles `group.clip` as a region and multiplies it into the layer's one composite draw, drawn in local units under the camera so the mask's curves are read in local units; the children keep the record's clip; .31 on both hosts at the crossing, .465 with the clip at the root |
| Replay across hosts comes from defined capability behaviour, captured inputs and numeric rules; a restricted syntax alone does not give it | the same five-step record had different result semantics on two hosts | Position 8 qualified; the checkpoint's dependency key carries the capabilities' behaviour; on the bench the two hosts now agree across the whole pickup surface to 1.2e-7, after the residual below was attributed |
| A document's layer stack and the group's compositing fields coexist: the stack compiles to groups | Position 9's stop condition made them exclusive | Position 9's condition rewritten |
| "Only surface reads serialize" is too strong: dependencies are stated by the construction and the compiler schedules around them; a carry that crosses tiles still owes an order | the pickup's carry at dab k+1 depends on dab k even in another tile | Position 8 and the best-team bullet reworded |
| The child-clip route's residual (.461 against .465), unattributed | "a pixel-agreement fix" | attributed on the bench: the region shader took its local position from the interpolated vertex varying, which inherits the rasterizer's sub-pixel snapping of the cover's corners (a clip edge read at .48 instead of .5); fixed by deriving the position from the fragment's own pixel through the inverse map; the cross-host difference on the pickup surface fell from 5.8e-3 to 1.2e-7. The tree's filler interpolates the same varying |

### The definer's attack 4, folded
A result used somewhere else (`attack-4.md`): a network face bound as another tool's mask, which the bench's clip had no input for; a paint proof returned inside a labelled record, which the executor's liveness could not see and so released, and the GPU then refused; a checkpoint saved to bytes and reloaded into fresh hosts, which the bench's loader did not read. The definer proved each with a probe beside the bench and wrote the numbers. Session 12 made all three run on the bench, code first, and folded each at full weight; nothing was rejected.

| Finding | Definer (attack 4) | On the page and the bench now |
|---|---|---|
| A consumer that accepts a path accepts a derived path directly; the authored source stays an upstream way to obtain the same input; the packer applies the consumer's placement and tolerance as for any region | `clip.path` and `clip.rule` unread and no clip produced (the rectangle .6 at both probes); the same face bound by a probe into the draw's clip argument: .6 inside, 0 outside | the contract's clip entry (an authored source, a frozen derived value, or a live reference); bench 9's clip and group mask take a path value through one input; the definer's literal record runs verbatim as a fixture: alpha .6 at (60.5, 10.5) and 0 at (60.5, 70.5) on both hosts, every field read |
| A mask that follows edits needs a reference to a producer and an output, resolved by the executor, keeping the producing revision and the source correspondence, invalidated when the output changes; a camera change repacks without rerunning the arrangement; the syntax is illustrative, not a ruling on Position 8 | the bench had neither binding; three consumers of one packed face (a union stroke, 24 dabs, the presented pickup surface) at the crossing: 0 through the top face; .62, .8556 and (0.0134, 0, 0.9866, 1) through the bottom | the contract's construction entry; on the bench a record holds constructions by name under `constructions` (its stand-in for a document's other records), `clip.path = { result, output }` is resolved by the executor once per edit and packed per scale, and the panel names the face, its loop, its area and the producing revision; the crossing through the bottom face reads .8556 as dabs and .62 as a union on both hosts, 0 through the top face; the pickup surface presented through the same face on a scratch copy: the presented pixel at the crossing (64.5, 64.5) reads (0.013, 0, 0.987, 1) on both hosts, and 0 at (64.5, 20) outside the face, where the surface's own texel is still the opaque blue (0, 0, 1, 1): deposits untouched, presentation masked; the source correspondence rides on the path itself (each segment's edge and interval, the face's id); the network's pieces draw faintly under the masked stroke |
| Keeping a returned record keeps the logical resources reachable through it; the executor accounts for contained references, through `next` and `return` too; the compositor still owns allocation; a conservative traversal first, a compiler over the definition later; retaining an array is not merely reading its outer object | `compileUses` put red's last read at the `keep` call; `aliveAfter` compared identity and did not find red inside the array; the executor released it and the GPU refused the later read (zero paints in place, two copies, one release); the CPU twin hid it because its release does nothing; two controls kept it readable (a top-level return; an adapter declining release) | the contract's paint entry, story item 15, Position 8; the executor's liveness follows values into arrays and plain objects at the positions the compile pass gives, at each step's release and at `next`; a value the state or a returned record reaches, at any depth, is alive; the record runs verbatim as a fixture and both hosts read the red proof (0.5, 0, 0, 0.5) and the blue result (0, 0, 0.5, 0.5) after the run; three targets, two copies, none released; the panel reads every surface the returned values reach on both hosts and prints a refusal where a host refuses; a value is given back once (the executor had been releasing the same value at its last read and again at `next`, which the GPU host tolerated silently) |
| Saving a continuation saves its ordinary state and the content of its reachable resources, with an explicit representation and a compatibility check; size, map, colour and capability compatibility belong at load; a changed interpretation is an explicit conversion or a failed load, never supplied by the destination's declaration | `checkpoint.load` unread, the bench took a new prefix; the probe's wire (`softland/path-checkpoint` v1: dependencies, continuation, resources as base64 rgba32f-le) restored and resumed to 0 differing components on each backend; a naive JSON of a Float32Array decodes to length zero | the contract's checkpoint entry and the per-brush-step row; bench 9 writes that wire from the in-memory checkpoint of either host into a content store keyed by hash, `checkpoint.load` names it inline or by hash, and the loader checks the format, the dependencies, the continuation's index, each resource's encoding, length and interpretation, and every reference, refusing with the reason; the pickup's twelve dabs leave the CPU twin as 364,786 bytes and come back into fresh hosts: 0 of 65,536 components differ against the saved bytes and against the host's own straight run, on the CPU twin and on the GPU, and the saved content hashes to the definer's `9757e826…` exactly; from the GPU, 364,780 bytes, the same; the definer's four edits: the first pressure refused with "dab 0 differs", the last sample's x loads, `pickup` and a changed capability refused as a changed fixed dependency; a payload four bytes short refused by its count |
| The same resource reachability serves lifetime and saving; capture must find surfaces inside labelled records and resolve repeated references to one saved result consistently | the capture and bring code checked only top-level state fields (a source observation, not a receipt) | one walk serves the executor's liveness, the checkpoint's capture and the wire; a surface reached twice is one resource and comes back as one value; measured on a scratch copy (the nested record with three dabs, a checkpoint after one): the saved state carries `proofs = [{ label, surface: a reference to proof:0/red }]` and three resources (the start, the red proof, the blue result; 12 KB); resumed on each host to 0 of 1,024 differing components; as bytes, 19,366 bytes with a content hash per resource, restored and resumed to 0 of 1,024 on the CPU twin and on the GPU; across the three dabs the executor gave four targets back as the new state stopped reaching the old proofs |
| An unread-field report and `ok: true` are observations; later use of a returned result is part of the construction's receipt | the nested record ran `ok: true` with an empty unread list and its returned proof was unreadable | HOLDS; the panel samples every surface the returned values reach after the run, on both hosts, at the first dab's centre |
| Presenting a completed surface through a mask is not clipping its deposits; the two stay separately sayable | the third consumer row: the pickup surface masked at presentation, its deposits untouched | HOLDS; the bench presents the surface through the record's clip and says so on the panel; a tool that wants clipped deposits clips its paint steps |

### What this changes on the page, and what it only costs
- **A sixth piece, the compositor (session 10, Position 9).** Under the filler in figure 1. Its two operations, paint and sample, are the contract for a clip, a blend, a layer and a brush that reads; "on top" and "beside" both leave the page. The attack row, the fork table's new column, the cascade's new row and the axis table's new row carry it.

- **Position 8 rewritten as the executor.** A vocabulary, an order and compiled expressions; instances many; Sid's two worries placed. The construction is a fifth kind of record above the waist in figure 1, and the waist itself is drawn where session 9 put it, above the source stage.

- **The contract gains clip, blend and the group's fields; the surface and the construction sit beside it; the answers gain the arrangement, contacts and spans, and the dab packet.** Edited in the contract block above.

- **Results, checkpoints and masks (session 11, attack 3).** The contract's paint entry names value lifetime and result identity; the group's clip has a scope; a checkpoint sits beside the value as a continuation with its dependency key. None of it adds a piece: liveness is a compile pass in the executor, the pool and the resolver are the compositor's, the mask is the composite draw's one extra input.

- **From attack 2's fix list, carried.** Partial overlapping spans, tangent contacts that do not cross, ties in the tangent order, holes in general (the bench assigns them by containment), state and replay dependencies (a paint on an old revision differs between the bench's two hosts), the carry-on-GPU route, layers allocated at the group's cover rather than the target's size, a group mask on a soft edge versus clipping each child. Renderer cost claims stay apart from these constructions.

- **The contract gains a unit and two fields.** Stroke width carries `unit local | device`; the stroke declaration carries `align` and `dash`. The answers gain `offset(path, d)`, `length` and `tangent(t)`. Edited in the contract block above.

- **A fifth piece, beside the sources.** The evaluator. Position 8 below names it and the three doors it could be.

- **Rates, made honest.** A device-unit stroke's envelope is per scale; snapping is per frame. Both rows of the axis table gain the entry. The value stays camera-free either way; that claim was about the wrong stage.

- **A cost, not a change (fix list).** Bench 9's envelope flattens the centerline and traces the outline; a 70-sample stroke becomes 26 knots, 25 cubics, 98 pieces and 491 outline curves, about 27 KB of curve and band texels at τ 0.1. The page's memory line, near 3 KB per stroke, assumed a stroker that offsets the curves themselves, a few quads per cubic per side. The claim survives with that stroker; the number does not survive the shortcut. The shortcut is what perfect-freehand ships; the offset stroker is what Skia ships (FIELD).

- **A detail, not a change (fix list).** On a tapered piece the nib's butt cap runs between the two tangent points, which are not perpendicular to the centerline; PostScript's butt is the constant-width case. Even-odd on the skin is a demonstration of the folds, never a declaration.

- **From the definer's fix list, carried.** Per-side border colours need the corner region divided and its shared boundary composited; calling that "paint on top" does not define it. A device width under shear or non-uniform scale needs the full transform, not one scalar. A stroked loop can have a hole. The current tapered projection rule is not the swept nib, and a round sweep gives no butt or miter without construction (both on bench 9 as outline-level constructions). Vector-network faces after an edit that makes curves cross need intersections, splits, ordered outgoing directions and a face-selection policy; ids alone cannot choose the user's intended face.

## What the path kind is today
One notch below the folder map. Each box is a piece with what goes in, what comes out, and why it has that shape. Arrows are real calls. The text lane is drawn as an island because nothing in the path family calls it or is called by it.

[FIGURE: see path-kind.html for the drawing]
Caption: **Today.** The stored form is the pen's samples with a width each, read as straight pieces. Triangles are a cache keyed on the geometry and one of three zoom bands. Zoom is a camera move in the shader; the only recompute is how many triangles a round cap gets. Two pipelines that never meet: the path lane bakes camera resolution into a triangle cache and paints without coverage; the text lane already owns the general per-pixel filler, wrapped in glyph packing.

## The attack on the imaging model, claim by claim
Sid's working basis, one claim at a time. HOLDS means it is the answer for this kind. DEFAULT means it works until a named condition, and the condition is stated. SHORT means it cannot express something Sid has already said he wants. This is the form Sid can rule on.

| Claim | Verdict | Why, and the condition |
|---|---|---|
| A path is line, quadratic and cubic segments, open or closed, in subpaths. | HOLDS | As the output language it has no rival: Skia, browsers, PDF, Vello, Rive, Figma's and Flutter's renderers all compile to it (FIELD). In this tree, text's curve data is already exactly this, quadratic-only (CHECKED). Cubic in the stored value, split to quads at the packer, is the standard bridge. |
| Fill covers the inside under nonzero or even-odd. | HOLDS | The text shader accumulates signed crossings, which is nonzero by construction; even-odd is the parity of the same count, one line in the shader (DERIVED from text/renderer.cljs:215-251). Today's path fill has no rule at all and cannot take a self-crossing ring (CHECKED). |
| Stroke is the fill of the region within half the width of the centerline, with cap and join. | SHORT | That stroke has one width; a pen stroke has a width per knot. The general object is the envelope of a tip swept along the path, attributes varying along it; PostScript's stroke is the round tip at constant size. Every ink system does this: tldraw's perfect-freehand builds the envelope polygon, Google's Ink API sweeps a brush tip into a mesh, PencilKit interpolates per-point size along a B-spline (FIELD). Today's tessellator is already a variable-width envelope builder (CHECKED tessellation.cljc:211-268). And "curve plus varying width" still names two regions: a ribbon offset perpendicular to the centerline, or the union of discs of radius w(t)/2 swept along it. Today's CPU classify projects the point onto the segment and reads the width at that foot (CHECKED component.cljc:176-190), which is the ribbon; its docstring says it is not an exact solver for tapered outlines. Counterexample: centerline (0,0) to (10,0), radius 1 to 4, point (5, 2.6) is outside by the projection rule and inside the disc at (6,0). The two must be told apart before CPU and GPU can agree by construction. Position 7 picks the nib. |
| Boundary and fill are independent declarations; the code must not presume fill. | HOLDS | With one precision: the envelope is always filled nonzero, because the skin of a curved or turning stroke overlaps itself on the inside of every bend and at every join, and nonzero is what turns those pieces into one region. This lets the envelope builder skip the boolean, not the orientation: it must emit consistently wound pieces, and then the rule does the union; obtaining such pieces is the builder's job (the definer, attack 1), and bench 9's tracer does it by folding inner joins through the pivot. Even-odd on a stroke skin would punch holes. The interior takes whatever rule the fill declares. |
| Paint and compositing (clips, blends) sit on top. | HOLDS as declarations; SHORT as a placement | As declarations, yes: a clip, a blend mode and a group's opacity are separate from the boundary and the fill, and PDF 1.4's transparency model already carries all three (FIELD). As a placement on this page, "on top" was wrong, and Sid said so (2026-09-06, about 02:00): nothing composites after the pieces. The compositor is a piece, the last three lines of the filler's program plus surfaces (bench 9, session 10): a clip is a region whose coverage multiplies in the same draw, from its own curves and bands; a blend reads a copy of the backdrop under the region; a layer is a surface painted as a region into its parent, allocated only when a group needs isolation. The painting-app semantic where a translucent stroke darkens where it crosses itself is ordered dabs through the same filler (attack 1; alpha 0.8556 at the crossing), and the same dabs at full alpha in a layer at 0.62 read 0.62 again, which is how painting apps make a translucent stroke flat; three numbers at one pixel on the bench. A brush that reads the surface (smudge, the definer's pickup) is not a second lane: it is a construction calling the compositor's sample before each paint (attack 2; bench 9 runs it on the GPU and on a CPU twin). Condition: on the compute road all of this becomes a per-tile clip stack and blend stack inside one dispatch (Vello; FIELD); the contract does not change. |
| Resolution independent: exact at every zoom. | DEFAULT | Right for an infinite canvas. UI libraries want a one-pixel border to be one crisp pixel, and analytic coverage at a fractional position gives two grey pixels instead. Every UI renderer carries a pixel-snapping policy the model is silent on (FIELD). Condition: the day a UI library is built above this kind, snapping becomes a declared option on the group or the paint, not a change to the filler. |
| The model is exact. | DEFAULT | Circles and arcs are not Béziers; a rounded rectangle's corners are approximations at a declared error. The envelope of a cubic is not a Bézier either, its offset is degree ten (FIELD). So a tolerance exists somewhere in every route. Position: it lives in the packer and the flattener as a parameter, never in the stored value. |
| The model says how to draw. | SHORT | It says nothing about cost. Text's filler draws each glyph as one dilated rectangle and evaluates every pixel inside it (CHECKED). A glyph is compact. A stroke across the canvas is a thin diagonal in a huge box, and per-pixel cost is box area times curves per band. The cover policy, bbox or only the cells the region covers, is outside the model and decides whether the route is feasible for ink. Vello and Pathfinder tile for exactly this reason (FIELD). |
| Samples become a curve; store the curve. | DEFAULT | The pen reported positions eight milliseconds apart; the curve between them is a prior, not data. Fitting is lossy and stylistic: the tolerance is what a user feels as "streamline". tldraw and Google's Ink API keep the samples and derive geometry; PencilKit keeps the spline (FIELD). Position 1: samples are the event truth, the path is derived at a declared tolerance, and a polyline (tolerance zero, the interpolating spline) is a legitimate path. What leaked from the GPU was never the polyline; it was the triangles at zoom bands and the per-piece painting. |

Two corrections to this morning's turns. "Straight segments between samples is the GPU's format leaking" is half right: a polyline is a valid path; what leaked is the triangle cache keyed by zoom band and the per-piece painting that double-blends. "Stroke is the fill of the region within w/2 of the centerline" is true for PostScript and too narrow for ink: stroke is the nonzero fill of the swept tip's envelope, and constant width is the case where the tip does not change.

## The fork below the waist
The contract above is stable across the field; the program under it is where teams differ, by workload and platform. Four shapes fit under this contract. The current flat triangle lane is not one of them: it has no coverage, it consumes geometry with a pixel quantity baked in, and it disagrees with the CPU definition at overlaps.

| Renderer shape | Antialiasing | GPU memory | Zoom | Strokes | Clips, blends, layers (session 10) | Cost to reach from here |
|---|---|---|---|---|---|---|
| the text filler, made online | analytic, per pixel | small: curves + bands | camera move; lower cubics→quads per scale bucket | via the envelope, filled nonzero | a clip as a second region in the same draw; a blend from a backdrop copy under the region; a layer as a cover-sized target only under isolation (bench 9 does all three) | a packer (cubic→quad, bands, cover cells) at pen-up; dynamic textures; unifies text and paths |
| stencil-then-cover + MSAA | MSAA 4× | large: multisampled target, 40 MB at 1080p, 170 MB at 4K | flatten per scale bucket | today's quads + fans into the stencil → union | a clip through the stencil; blends and layers as on the first road, on a multisampled target | smallest change from today; removes bridging and ear-clipping |
| compute coverage (Vello-shaped) | analytic | transient, scene-sized | re-flatten on GPU every frame | expansion on GPU | all three per tile in one dispatch: a clip stack and a blend stack in tile memory, no full-screen layer ever (FIELD); the same contract | largest lift; best known end state |
| capsule / distance lane | analytic from distance | small | camera move | today's CPU rule, a ribbon with end discs, per segment; the swept nib needs the disc union per pixel; round only | composites like the first road | strokes only; no fill story; ideal as the wet-ink lane under the pen |

Memory goes down, not up, on the first road. A hundred-sample stroke today at the default band is about 310 triangles, 930 vertices at 28 bytes, near 26 KB per band (DERIVED from the layouts). As curves it is roughly seventy quadratics at 16 bytes plus band references and one 100-byte instance, near 3 KB, valid at every zoom. The GPU-memory worry that comes with local agents is real, but it lives in render targets, the MSAA road, not in path data.

[position] Settle the contract first; it is the part that lives. Under it, the region lane to build is the text filler made online, because it is the shape already in the tree, it needs no multisampled target, and it gives glyphs, ink and shapes one lane. Keep a capsule lane for the stroke under the pen, committing to the filler on pen-up; wet and dry ink is how shipping ink systems handle latency. Conditions that flip this: if banding per pen event proves too slow and the wet lane is not enough, stencil-then-cover is the cheaper correct road; if long thin strokes dominate and cover cells do not cut the cost enough, a distance-based stroke program is the fallback, fed by the same value.

## If the stored form changes, what changes with it
Each row is one file or one missing piece: what it does today from the hunters' reads, and what it becomes.

| Where | Today (CHECKED) | Becomes |
|---|---|---|
| path/component.cljc | Two grammars: :ink samples with a width each, cap/join round only; :shape rings tagged outer/hole. No curves, no rule, no closed flag; pressure and time accepted and never read. A stroked rectangle or a filled ink loop is inexpressible (:15, :89-92). | One grammar: the path value above. :ink and :shape stop being kinds; they are sources. Outer/hole roles die: winding direction or the rule carries that. Pressure and time move to the source record. |
| content hash, mesh key | Key = canonical geometry + algorithm version + zoom band; the frame key carries the band too (tessellation.cljc:42-55, frame.cljc:9-22). | Zoom leaves both. Tolerance enters the packer's own cache key, keyed on projected error. |
| path/tessellation.cljc | Envelope builder for polylines (tapered quads, round fans, resolution by band) and a polygon triangulator (bridge holes, ear clip, O(n³) worst case, throws on no visible bridge) (:201-271, :285-567). | Splits in two. Geometry: the envelope builder generalised to curves and tips, emitting a consistently wound outline; the triangulator dies, nonzero handles holes and overlaps. Packer: new. Cubic to quad, band building, cover cells. Fonts are banded offline today; user paths need it at pen-up, in cljc, fast. |
| path/renderer.cljs | Flat-colour triangle program, 28-byte vertices, paint replicated per vertex, no coverage, whole-frame repack on any change (:144-240). | Dies as a program. Path becomes a producer of instance rows for the shared filler; per-item update through the engine's buffer pool. |
| text/renderer.cljs, glyph_pack.cljs | The filler and the glyph packer in one namespace; em-box and glyph-id assumptions in the packer; 100-byte instance rows; band width 4096 a compile-time constant while the texture reads its width from metadata (:112-113, :127, :321). | The filler moves out to be shared, gaining a rule parameter and a cover-cell mode. Glyph packing stays as the glyph source's packer. Text's own contract does not change. |
| path/frame.cljc | Dependency key = material, revision, container, plus zoom band. | Same key without the band. |
| CPU classify | Ink: distance to segments minus interpolated half-width, the ribbon. Shape: odd-even ray per ring, then outer-and-not-hole. Parity with the GPU tested only on the holed shape, never on ink, skipping a boundary band (harness/path.cljs:341-393). | Winding number with curves and the nib definition, the CPU twin of the shader's root counting, so CPU and GPU agree on membership by construction; distance and coverage are compared against that membership at a declared error. Parity runs on ink too, against coverage. |
| region3d/on_plane* | Reuses the same tessellator and shares the path renderer's mesh cache object at zoom 1.0, uploads 88-byte vertices with a 4×4 matrix each, draws on a plane with 4× MSAA; only :ink placeable (on_plane.cljc:20, on_plane_renderer.cljs:19, 211-235). | Placed ink takes the coverage lane on the object's plane: the filler uses screen derivatives, so it works under the projection (DERIVED). The 88-byte mesh road is cut now, no keep-until-better. flatten(τ) stays a geometric answer for consumers that need lines, not a render road. |
| harness/path.cljs | Literal bursts and rings; byte-identical goldens; a translucent self-crossing case that declares the double-blend and names the CPU classifier the authority (:556-559); the variable-width fixture built and never run (:245, :517-518). | Fixtures gain the fit step, a formula shape, a stroked shape, a filled loop, a cubic; goldens change; the self-crossing case becomes the proof that overlap resolves once; parity moves from alpha over 128 to coverage. |
| engine/compositor.cljs, region3d's composite, the group record | A target pool keyed by format, size and sample count, a 512 MiB budget, epoch reclaim (compositor.cljs:157-260); a present pass, linear → sRGB, an overwrite (:362-374, :644-671); region3d's composite draws a region lease's resolved MSAA texture as an instanced quad, premultiplied over, into the caller's pass (region3d/renderer.cljs:1366-1412); a scissor rect per draw, supplied by text's caller, not by any group (:612-628; text/renderer.cljs:851-854); a group is a transform node with one flag, screen or world (transform.cljc:65-79, device.cljs:65-137); no blend mode, no opacity, no clip by a path, no stencil, no compute pass anywhere in client/. Painter's order is whatever the harness paints in. | The compositor of Position 9. The pool and the present stay; the quad composite becomes the general "a surface painted as a region"; the group record gains opacity, blend, clip; the filler's program gains the clip's second region and the backdrop read; a layer comes from the pool only when a group needs isolation; a painting surface is a retained target with a revision. The harness's painter's order becomes the scene's. |
| new, no home today | Nothing. No pen input anywhere in client/; no shape generator; no circle fixture; no executor; no arrangement. | The builder (samples → curves, appended live, at τ), the executor for constructions written as data (a step runner over a capability table; bench 9's is sixty lines and runs the definer's two records), the band packer, the cover policy, offset and arc length as answers, the arrangement (lines exact, curve crossings refined; bench 9), the compositor's sample and paint, and a wet route while the pen is still down. Bench 9 has a first cut of each but the wet route, in JS. |

## How the best team in the field would define it, and what they would name
**The problem as they would state it.** Define a resolution-independent scene language as the output: paths, paint, compositing. Keep every source above it with its own editable truth: samples, radii, glyph ids. Pick the rasteriser by cost profile and keep it swappable behind that language. Decide up front, because they cannot be retrofitted: the antialiasing policy, the overdraw policy, the stroke model, the overlap semantic, where tolerance lives, and whether raster brushes are in scope. Then the trade-offs Sid did not know to ask about. FIELD unless marked.

- **Tolerance is a screen-space quantity.** Every route has "how smooth at this zoom" somewhere. Camera-free data does not remove it; it moves it into per-scale work. Name where it lives, packer or flattener, and keep it out of the value.

- **The envelope has cusps and inner loops.** Offsetting a curve inward past its radius of curvature folds the outline over itself. Not a bug to remove; it is why the envelope is filled nonzero and why the builder needs consistent orientation, not a clean boundary.

- **Overdraw and conflation.** Painting a region as many pieces double-blends translucent paint where pieces overlap, and antialiasing pieces separately leaves seams where they meet. The fix is structural: one resolve per region. Today's path lane has the first defect and declares it (CHECKED harness/path.cljs:556-559).

- **The cover policy.** Bbox covers are right for glyphs and wrong for a stroke across the canvas. Cells the region covers, boundary cells plus interior cells, computed in local units at pack time, keep the cover tight and camera-free; boundary cells alone would drop a filled region's middle (the definer, attack 1; bench 9 tests interior cells by winding at the cell centre). The one thing the text lane never needed and ink needs from day one.

- **Curve degree.** Design tools and fitters produce cubics; the shader wants quadratics. Splitting multiplies the curve count two to four times. Decide the canonical stored degree, cubic, and convert once at the packer.

- **Bands at edit time.** Fonts are banded offline by a tool outside client/ (CHECKED). A user path must be banded on pen-up, fast enough not to be felt. Real work, and it does not exist yet.

- **The wet stroke.** While the pen is down the burst is unfinished; fitting and banding the whole stroke on every sample is the wrong cost. Every ink system has a cheap route for the live tail and commits the fitted form at pen-up. Today any change repacks the whole frame and fills the cache with one dead mesh per sample (DERIVED).

- **CPU and GPU must use the same rule and the same nib.** Hit-testing and coverage disagree at self-crossings if one is odd-even and the other nonzero, or one is the ribbon and the other the nib. Today three parts answer "inside" three ways: pick is odd-even, the text shader is nonzero, the ear clipper throws (CHECKED).

- **Editing geometry is not rendering geometry.** Hit tests, bounds and snapping are derived from the path separately from what the GPU gets. Never make the renderer serve the hit test; never let the hit test follow the GPU's approximation.

- **Paint order versus depth.** Alpha needs painter's order; depth-testing breaks it. A per-knot z for the sweater pays this and needs defined behaviour for depth, coverage and alpha together.

- **Pixel snapping for UI borders.** A policy on the group or the paint, not a filler change.

- **Raster brushes are a second model.** Dabs, textures and wet edges live on a pixel canvas. In scope or not is Sid's decision; the path value carries enough (samples, pressure, time) that a brush engine can re-render a stroke later.

- **Booleans and vector networks.** Figma's fill-by-face and union/subtract need a planar-arrangement library, the classic pit. It sits above the kind; the value must not preclude it: multiple subpaths, both rules, stable ids.

- **Precision on an infinite canvas.** Coordinates narrow from float64 to float32 once at upload and are never rebased around the camera (CHECKED by 8's hunter), so at the legal zoom of 1000 vertex jitter is at pixel scale on a modest canvas (DERIVED). Rebase relative to the camera before upload.

- **Clips, blends and layers ride in one pipeline.** Skia's canvas carries a clip stack and saves a layer only when it must; Vello's fine rasteriser keeps a clip stack and a blend stack per tile and never allocates a full-screen layer; Rive reads the framebuffer in place (FIELD). The shared shape: a clip as coverage multiplied in the same program, a blend as a read of the backdrop, layers only under isolation. What none of them do is make the clip, the layer and the blend records a tool can be built from, with a CPU twin that composites the same way; that is the part that would be ahead.

- **Isolation is a policy, and it costs a target.** Group opacity over overlapping children, a non-over blend on a group, or a soft mask on the composite need the group rendered apart; a group with none of these paints straight through (browsers, Skia; FIELD). The number of live layers is the nesting depth, not the count of layers in the document.

- **Dependencies are stated by the construction; the compiler schedules around them.** Plain accumulation is one instanced draw in painter's order, which the API keeps. A brush that reads what it painted needs the previous dab resolved first: on a fragment road a copy under the dab per dab, on a compute road sequential dabs inside the tile (Krita: tiles and threads on the CPU; FIELD), and a carried colour that crosses tiles still owes an order between them (attack 3). That, not the executor, is where a painting brush's throughput lives; bench 9 counts 24 syncs for 24 dabs. The same stated dependencies tell the compositor when a result's storage may be reused: at the input's last read, never before.

- **Determinism when two clients paint one surface.** A construction that reads a surface makes pixels into inputs; two GPUs differ in the last bits, so a shared painting surface drifts unless one client is the authority, the arithmetic is integer, or the CPU twin is the truth (Drawpile paints on the CPU for this reason; FIELD). Open, and Sid's; the bench's CPU twin is the seed of an answer.

- **Antialiasing strategy decides the architecture and the memory.** Analytic coverage needs no extra target; MSAA needs a multisampled target of tens to hundreds of MB. The parked GPU-sharing question lands here.

## Sid's questions, one by one
**Is the sampled pen stroke the general case of formula shapes and designer outlines, or is something else the general object?**

Something else. The general geometric object is the path; the pen burst is the least-structured source of one. But the general stroke object has to be designed from ink, because ink is the first thing Sid wants to draw and PostScript's stroke cannot express it. Least structured is not most general: a circle stored as a radius stays a circle when dragged; the same circle as two hundred samples has lost even the curve. Today the code stores the source in the geometry slot: :ink is the stroke-point list with pressure and time riding on each point (CHECKED component.cljc:41-57).

**CPU versus GPU: how is it decided, and is it the right axis?**

Not the right axis. Sort every value by when it changes: per edit, per camera scale, per frame. Then CPU or GPU is a placement choice per stage. Today a per-scale quantity, fan resolution, sits in the per-edit cache key and the frame key (CHECKED); zoom from 1.0 to 7.9 is a pure camera move, crossing 0.1 or 8.0 re-derives every mesh and repacks the whole frame. Text bakes nothing and the same bytes draw at any zoom (CHECKED). Five places in the tree each decide how big a pixel is, by five rules; derive pixels per local unit once.

**Why does Slug exist for text, is it glyph-only, and is it what ink and shapes need?**

It exists so a letter is exact at every zoom without an atlas: curves and bands in textures, a per-pixel root count. The per-pixel core reads no font size and no glyph id (CHECKED text/renderer.cljs:194-256); the glyph-ness sits in the packer and the offline band bake. It is exactly what shapes need, after cubic-to-quad. It is what ink needs after the envelope step, never directly: it fills, it does not stroke, and it has no distance function. Three things it lacks for user paths: a packer in the client, a cover policy for long thin outlines, and a route for the wet stroke. One trap: the band texture width is a compile-time 4096 while the real texture reads its width from metadata (CHECKED).

**A stroke that crosses itself, and the knitted sweater.**

Three different things. A closed shape that crosses itself is the fill rule, a declaration: nonzero fills the pentagram's centre, even-odd leaves it empty. A stroke that crosses itself is the overlap semantic: union paints the crossing once, which is what nonzero coverage gives and what vector tools do; accumulate darkens it, which is the raster lane. Neither gives "under". Under means an earlier part of the same stroke sits on top of a later part, and one region has one paint order. So the sweater is a split-and-order edit the user makes, and the kind's job is stable knot ids so a split does not lose the stroke's identity. Today the GPU double-paints overlaps at alpha and the CPU says union; the harness declares both (CHECKED).

**Pre-baked and user-defined shapes: same drawing, and can tldraw's and Figma's sets be built above this?**

Same filler, different sources. tldraw's set fits entirely: formula shapes, outline shapes, text, and a freehand tool whose envelope polygon is this kind's envelope step (FIELD). Figma's set needs three more things: vector networks as an authored graph whose faces are an answer of the geometry, chosen by a record's policy (attack 2; bench 9 runs Figma-shaped networks with a curved edge); booleans over the same arrangement; effects as the compositor's filters. Its masks and blend modes are the compositor's declarations (session 10). UI libraries need pixel snapping. None change the path value. Today no generator exists: every rectangle in the tree is four typed corners (CHECKED).

**What are the current path files doing, and do the per-kind renderers match "the renderer"?**

Read off the today picture: the harness is the only maker and only caller, a library without a host. Half of "the renderer" already exists as a frame contract all four kinds obey: device and format in, the shared camera and group buffers bound, an open pass at draw time, group index baked at prepare (CHECKED). What differs is the program each kind runs. Under the position there is a small fixed set of programs, coverage filler, textured quad, 3D mesh plus composite, present, and kinds are sources that feed them. The path program is the one that should not exist; the text program is the one that should be shared.

**Am I reasoning from how the code does it today, or from how it should be done?**

This page was derived field-first and then checked against the tree. The field-first derivation lands on the same shape as this morning's turns, path in, coverage out, text's filler shared, and corrects three things: the polyline is not the leak; the stroke must be the envelope of a swept tip, not the PostScript stroke; and the cover policy is a real design item the glyph case never had. "That is what text does today" is evidence of feasibility, not the reason for the design.

## Positions, each with the condition under which it stops being the answer
**[position 1] the stored form**

Store the burst raw (samples with pressure, time) as the event that happened. Derive the path at a declared tolerance and store that too; it is what gets drawn, edited and hit-tested. Tolerance zero is the interpolating spline, one knot per sample, appended live; a fit above zero is a brush setting. The kind takes the path, never the burst. Stops being the answer: never; if fitting turns out to be a user-facing style this is already right, and if a brush engine later needs the raw samples, they are there.

**[position 2] the imaging model as the output language**

Paths, fill under a rule, stroke as the nonzero fill of a swept tip's envelope, paint and compositing in the compositor (Position 9), not on top. Accumulating brushes are ordered dabs through the same filler (attack 1); brushes that read the surface are constructions calling the compositor's sample before each paint (attack 2). Stops being the answer: on the output side, never; the condition it used to carry, first-class surface-reading brushes, is now inside the contract, and bench 9 runs one.

**[position 3] one filler, shared with text**

Coverage under a rule, per pixel, analytic antialiasing, one resolve per region. Strokes reach it through the envelope. A capsule lane serves the wet stroke under the pen. Stops being the answer: if long thin strokes dominate and cover cells do not cut the cost enough, a distance-based stroke program is the fallback, fed by the same value; if banding per pen event is too slow and the wet lane is not enough, stencil-then-cover is the cheaper correct road.

**[position 4] zoom leaves the stored value and its identity**

The path value and its content hash carry no zoom. Per-scale caches are legitimate, but they belong to the packer and are keyed on projected error from the full transform, never on hand-set bands in the kind's own key.

**[position 5] cubic in, quadratic at the packer**

The stored path allows cubics because fitters and design tools produce them; the packer splits to quadratics for the shader. Stops being the answer: if a cubic-capable filler is written, the packer step disappears and nothing above notices.

**[position 6] placed ink takes the coverage lane on a plane**

The filler uses screen derivatives for its pixel scale, so it works under the region's projection; placed ink becomes an instance row drawn on the object's z=0 plane, and the 88-byte triangle road is cut. flatten(τ) stays a geometric answer for consumers that need lines, not a render road.

**[position 7 · the open fork] what a varying width means**

The stroke region is the swept round nib: the union of discs of radius w(t)/2 along the curve. Cap and join constructions (butt, square, miter, bevel) are declared operations on the ends and corners on top of it, as PostScript adds them. The ribbon is named as the approximation the current CPU rule computes. Reason: only the nib gives a tip shape a meaning, and it is what a pen physically does. Stops being the answer: if a designer stroke tool wants the ribbon's crisper tapers, both constructions are named operations and the declaration picks one. This is Sid's to close, verbatim and timestamped; asked once. The sliver is visible on 4's bench and on bench 9: pick the pressure-ink fixture, switch the tip, and the cursor shows what each construction says and flags where they disagree. Sharpened by the waist test: tldraw's own construction is the ribbon (FIELD), so the ribbon must stay nameable whichever is the default; on bench 9 the two are one line apart in the same tracer.

**[position 8 · asked] the executor: behaviour as data needs a runner, and the runner is a vocabulary, not one interpreter**

A tool's settings are numbers until one of them is a function, and a function in a record is data only if code below the waist runs it. Both chairs found the piece on the same day (session 9's evaluator, the definer's construction executor) and attack 2 made its calls concrete: sample, mix, paint, arrange, locate, face-boundaries, over bindings, state and an order. Bench 9 runs a sixty-line executor over that table; the definer's two records run on it unchanged, the brush on two hosts with equal numbers. What the executor is: three things at three rates. First, a vocabulary of capabilities, each code that exists once (the geometry's answers, the compositor's two operations, the builder's steps); this is the real waist for behaviour, and it grows by attack, one missing operation at a time (the bench lists the record fields nobody read for exactly this). Second, a sequencing rule over records (bindings, steps, state, each, return) that runs per edit and per brush step, never per frame, and only moves values. Third, per-element expressions (width from pressure now; a paint from position later) compiled to the host's per-element form, per point on the CPU, per pixel in the shader, never interpreted per element (Houdini compiles VEX; Blender evaluates fields as arrays; FIELD). Instances are many, one per construction, per user, per agent; the definition is what exists once. Sid's two worries land in two places. "Getting them in line" is the first thing: every tool must be sayable in the vocabulary, and the attack method is how the vocabulary is found. "Throughput seems small" is not the second thing, which does no work; it is the dependencies the construction states, of which the surface read before it is written is the one that forces an order between dabs, and that belongs to the compositor and only to brushes that read (bench 9: 24 texel reads are 24 GPU syncs on the fragment road; plain accumulation is one draw; the compute road makes the reads tile-local, and a carry that crosses tiles still owes an order, attack 3). The same stated dependencies are what the executor compiles: where each value is last read, so a host may reuse its storage after that and never before (bench 9's pickup brush: one copy, twenty-three paints in place, two targets). A value reached through a returned record or the carried state is still read, so liveness follows how values compose and not how they are packaged; and a binding may name another record's result by output, which the executor resolves once per edit of the producer (attack 4: a face as a mask; the syntax of such a reference is an implementation choice, not a ruling here). The three doors stand: a fixed menu, each behaviour new code; this executor, records over a vocabulary, sandboxed by construction and replayable when the vocabulary's behaviour is defined, the inputs captured and the numeric rules fixed, which the syntax alone does not give (attack 3: one five-step record, two hosts, two results, until the host's contract was fixed); or code as data (sci), the general answer and a substrate decision that gives up the sandbox and the replay unless fenced, and that could be given the same restricted bindings. Stops being the answer: if a real tool needs control flow the record format cannot say (recursion, iteration beyond each over a collection), the third door opens for that class; if Sid rules that tools are code above a data-only contract, the executor dies and each response is a generator. Asked now, with Position 9.

**[position 9 · asked] the compositor: clips, blends and layers are one piece with the surface a brush reads**

The compositor is the sixth piece and it exists once: paint(surface, region, paint, clips, blend) → surface, and sample(surface, point) → colour. A clip is a region with no paint whose coverage multiplies the painted region's, in the same draw, from the same kind of curves and bands; a blend is a function of the source and the backdrop under the region; a layer is a surface a group paints into and then paints as one region into its parent with the group's opacity and blend, allocated only when the group needs isolation; the screen is the root surface; a painting surface is retained at its own resolution and advances per brush step. A brush that reads what it painted is a construction calling sample before paint (attack 2), so there is no raster lane beside the kind: the definer's surface interface and the compositor are one piece. Adopt the best in field as the contract: this is Vello's scene (clip and layer pushes with blends, resolved in one pass) and Skia's canvas (a clip stack, a layer only when needed) said as data. Implement it on the filler road first, which is what bench 9 does: the filler's last three lines plus surfaces, and every declaration agrees with a CPU twin at the cursor. Keep the compute road as the end state, where the same contract runs per tile and layers stop costing targets. Ahead of everyone is not a faster compositor; it is that a clip, a layer, a blend and a surface are records a human or an agent can make, edit and reuse, that a mask can be any path from any source including a computed one (attack 4: a face a network construction returned, bound as a value or by reference), and that the CPU can say what colour a pixel is. The tree has half of it, a target pool with a 512 MiB budget, a present pass, region3d's quad composite, and none of the declarations. A document's layer stack, if Sid wants one, is source structure above the kind that compiles to groups with these fields, as a network compiles to face paths; the two coexist and the group keeps its fields either way (attack 3). A mask on a group applies where the completed group is painted into its parent, and a child's clip stays on the child: two scopes, both kept. Stops being the answer: if a real document needs a compositing operation a group cannot say (a knockout, a mask read from another layer's channel), the group record grows or the piece changes; if two clients must show the same pixels of a surface-reading brush, the CPU twin or one authority becomes the truth of the surface (bench 9 after session 11: the two hosts agree to 1.2e-7 across the pickup surface, which is the seed of an answer and not one). Asked now, with Position 8.

## Folded in from the eight sessions
Eight sessions answered this round and ranked each other. Everything a peer had that this page lacked, with who found it. All adopted.

- **Ribbon versus swept nib** (5, 6; 8 corrected itself). Now Position 7 and the stroke row.

- **The imaging model is the output language and the wrong storage language** (7). Now the story and the attack.

- **The record is already cooked** (2). Width required, pressure optional (CHECKED component.cljc:41-50), so a pen model ran before storage and a better pen can never reach old strokes. Store the gesture; derive width through the pen as data.

- **Write the exact image as a definition first** (2). One slow reference renderer that produces it; every fast lane is tested against it. The harness parity check is the seed and today it skips a band along every boundary (CHECKED harness/path.cljs:341-393).

- **List every reader of the stored record before shaping it** (2, 4): screen, hit test, editor, 3D placement, file, other people.

- **Two intended pictures one input cannot tell apart** (1). The test for whether a field is missing: the sweater, closed-versus-filled, union-versus-accumulate, screen-pixel width under zoom. And 1's invariants: subdividing a curve must not darken it, a gap stays transparent over anything, zoom preserves within a declared error, spatial queries have a defined relation to the picture.

- **What must the thing still mean after the user does something else to it** (3): change a circle's radius, re-pen a gesture, flip a crossing. Shared computation does not determine placement.

- **Three parts answer "inside" three ways** (4): pick is odd-even per ring, the text shader is nonzero, the ear clipper throws (CHECKED component.cljc:199-218, text/renderer.cljs:215-251, tessellation.cljc:480-525). One truth, one rule. Also 4's standing rule: the renderer's convenience never leaks into the model. Every definitional error in the round was a renderer quantity promoted to a definition.

- **A brush field in a record does not make brushes data** (5). Unless the machinery below executes the arithmetic, sampling and surface reads, the waist has not been achieved. Same for "region": naming it does not discharge the geometry.

- **Change it and see if the pieces compose** (5): change the nib or pressure response; edit the centerline after drawing; use the same geometry as a mask and as a text guide; make a thread cross over here and under there; save the construction so someone else adapts it.

- **Sort work per edit, per scale, per frame** (8), then place it.

- **Five places decide how big a pixel is** (8): path bands, region3d's pixel size and its 1.12 bucket ladder, the placement zoom of 1.0, text's snap step.

- **The variable-width fixture is built and never run; the harness names the CPU classifier the authority and declares the double-blend** (8; CHECKED harness/path.cljs:245, 518, 556-559).

- **The renderer fork, with memory** (8), and the capsule wet lane.

- **Vector or raster ink is a decision** (4), answered here by a raster lane beside the path kind, not a change to it; Sid has not ruled.

- **Both ends are missing** (2): no live input anywhere in client/, no definition of the correct image; the code defines only the middle.

- **A bench** (4 built it: https://claude.ai/code/artifact/2f98a7e6-beb1-43df-9247-3974586256d6, source under `bench-4/`). One path, three roads, the pick formula at the cursor: today's triangles as a mirror of the tessellator, per pixel with one cover, per pixel with a cover per segment. The three harness goldens by their own coordinates, a pen loop, a pentagram, a trefoil with z per point for over and under. It now carries both stroke rules side by side, ribbon plus end discs and the swept nib, and the cursor shows both numbers; on the counterexample the ribbon says +0.10 outside and the nib says -0.02 inside. Not yet on it: curves, non-round caps, a cheap wet route.

- **Bench 9** (session 9 built it: https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc, source under `bench-9/`). The waist test as a thing to feel: seven records (the three tools, the three harness goldens, the star) plus draw your own, one pipeline. Curves throughout; cubics lowered at the camera's tolerance; bands; the tree's text shader as the filler; both tip constructions, caps, joins, alignment, dash, device-unit width, snapping; the CPU twin of the filler at the cursor against the GPU pixel. A wet route while the pen is down: the tail rides raw, the rest is fit as you go. The definer's hard case as a fixture: the harness Z as twenty-four dabs, 0.8556 at the crossing, one word away from the union stroke at 0.62. Not on it: a clip by a path, arrow caps, an offset stroker for curves (it flattens the centerline first), the sweater's depth, a surface-reading brush.

### Folded in from session 9
- **The waist is one step above the kind's input.** The records are data; the source stage (builder, generator, evaluator) is code; the value is what the kind takes. Figure 1 already drew the builder below the line; the waist test made the whole stage visible and gave it a third member.

- **Width has a unit**; alignment and dash are declarations; offset, length and tangent are answers. All in the contract block.

- **Rates**: a device-unit stroke's envelope is per scale; snapping is per frame. In the axis table.

- **Costs, on the fix list**: the flatten-then-trace envelope inflates curve counts about five to eight times over an offset stroker, and with it the memory line; the nib's butt cap on a taper is not perpendicular.

- **The two constructions are one line apart**, and tldraw ships the ribbon. Position 7 is a default with the other construction nameable.

- **From the definer's attack 1, at full weight**: the construction executor and the return arrow (Position 8); width as a function of the interpolated source attribute; accumulate as ordered dabs through the same filler, surface state only for brushes that read; three tolerances; membership as the shared definition; cover cells include the interior; the network as the authored source with intersections and splits as answers; the crossing edit as (segment, parameter) pairs; wet and dry as one region. Its Positions A to D are read as constructions and hold with the page's.

### Folded in from session 10
- **From the definer's attack 2, at full weight**: the executor's calls made concrete (sample, mix, paint, arrange, locate, face-boundaries) with bindings, state and an order; a surface interface beside geometry, which is the compositor; the arrangement as an answer with the face choice as data; sources, results and resources kept distinct; "anchors already are a path" scoped to a contour; intersections as contacts and spans; dab packets with the source location; the final-dab width evaluation (fixed on the bench); unconsumed fields as findings (the bench lists them); the rates row for a surface. Its Positions A to D hold with the page's; its Position B and Sid's compositing question met in Position 9.

- **Sid's two late questions, worked**: clips, blends and layers are the compositor's contract, not "on top" (Position 9, the attack row, the fork column, figure 1); the executor is a vocabulary, an order and compiled expressions, not one interpreter, with the serial cost placed in the surface (Position 8). Both asked now, together.

- **The tree's compositor, checked** (a hunter's read, 2026-09-06): engine/compositor.cljs owns a target pool and a present pass; region3d's composite draws a resolved MSAA texture as a quad; a scissor rect per draw; groups are transform nodes only; no blend mode, no opacity, no clip by a path, no compute pass anywhere in client/. In the cascade table.

- **Bench 9, session 10**: a compositor stage (a clip in the same draw, blends from a backdrop copy, a layer as a surface painted as a region, a painting surface per brush step), an executor that runs the definer's two records verbatim (the pickup brush on the GPU and a CPU twin, dab 19's sample and carry equal on both; the network through an arrangement: 4 faces of 2400, then 6 faces of 3800, 900, 3800, 900, 100, 100 with a curved edge), and the list of fields nobody read. Three numbers at one pixel: union 0.62, accumulate 0.8556, layer 0.62.

- **The waist, drawn where session 9 put it**: figure 1 now has the waist above the source stage, the executor in that stage, the value one step lower, the compositor under the filler, and the loop.

- **The count of pieces is closed by outputs**: a path, a region, an answer, coverage, a surface, a record. Six pieces; a seventh would need a seventh kind of output.

### Folded in from session 11
- **From the definer's attack 3, at full weight**: a result is a value that keeps its contents, with a key and a parent (the contract's paint entry; the executor's compile pass; the GPU host's pool); a checkpoint is a construction continuation with a dependency key (the per-brush-step row; `checkpoint.at` on the bench); a group's mask applies at the composite and a child's clip on the child (the contract's group entry; `group.clip` on the bench); replay across hosts is the vocabulary's defined behaviour plus captured inputs plus numeric rules (Position 8); a document's layer stack compiles to groups rather than replacing them (Position 9); dependencies are stated by the construction and the compiler schedules around them (Position 8, the best-team bullet). The table above carries each finding to where it landed.

- **The residual attributed**: the interpolated vertex varying carries the rasterizer's sub-pixel snap of the cover's corners; the fragment's local position now comes from its own pixel through the inverse map, and the two hosts agree to 1.2e-7 across the pickup surface. The tree's filler interpolates the same varying; the same one-line fix applies there.

- **Bench 9, session 11**: two fixtures verbatim (two paint proofs, one chosen; the crossing masked as one stroke), a checkpoint control, the results' keys and the host's storage receipts on the panel, the group mask in the outline view and the cursor. The Node route now reaches the executor, the CPU twin and the checkpoint key. Numbers in `bench-9/HANDOVER.md` and `fact-base-11.md`.

- **Still the definer's**: a construction's returned path as a clip's source (not yet general result reuse); the in-memory checkpoint as a saved one; a surface aliased through a definition's emit, which the compile pass does not see and the host's stale-read check would refuse rather than serve.

### Folded in from session 12
- **From the definer's attack 4, at full weight**: a clip's path is an authored source, a derived path bound as a value, or a live reference to a construction's returned path that the executor resolves once per edit (the contract's clip and construction entries; `clip.path` and constructions held by name on the bench); a value reached through a returned record, an array or the carried state is still read, so liveness follows how values compose (the contract's paint entry, story item 15, Position 8; one reachability walk in the executor); a checkpoint leaves as bytes with an explicit format, its dependencies, its continuation and each surface's content once, and a load checks all of it and fails with a reason (the checkpoint entry, the per-brush-step row; the wire, the content store and `checkpoint.load` on the bench). Nothing was rejected. The table above carries each finding to where it landed; the three items session 11 left as the definer's are the three that landed.

- **Bench 9, session 12**: three fixtures verbatim (a rect masked by a returned face; the crossing through a face it holds; proofs returned in a record), two save-to-bytes controls and a deep link that presses them headless, the panel's read of every surface the returned values reach on both hosts, the held constructions with their run and their returned loop, the content store. The Node route reaches the codec, the capture and the hash. Numbers in `bench-9/HANDOVER.md` and `fact-base-12.md`.

- **Still the definer's**: a tangent contact and a partial overlap in the arrangement; a reference across a document rather than a record's held table; a compiler that reads reachability off a definition's parameter, map and emit instead of walking values; a content store outside one page's memory; the offset stroker, the carry on the GPU and layers at the group's cover, as before.

---
Landed under `docs/below-the-waist/path-kind/`: this page and its `.md` twin, session 8's fact base with the hunter reports and the six closing folds, session 7's brief, page and three hunt reports under `sources-7/`, session 4's bench and its handover under `bench-4/`, session 9's bench under `bench-9/` with session 10's additions and their handover, the fact bases `fact-base-9.md`, `fact-base-10.md`, `fact-base-11.md` and `fact-base-12.md`, and the definer's `attack-1.md`, `attack-2.md`, `attack-3.md` and `attack-4.md`. Positions are marked and the verdicts are Sid's.
