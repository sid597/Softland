Markdown twin of path-kind.html, generated from the page; the three drawings live in the html. Landed 2026-09-05, regenerated 2026-09-06 (session 9).

Softland · client · path kind · 2026-09-06 · sessions 9, 8 and 7 folded, the definer's attack 1 folded

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

**Not decided**

Positions 1 to 7 at the end. Position 7, what a varying width means, is the open fork and is asked once, verbatim and timestamped. This morning's "three tiers" and "stroke is a fill of an offset region" were Claude's replies (uuids 6b009826, 28d31058, 418cb10c), not Sid's words.

## positionThe path kind as it should be

Two boundaries, not one. The kind's input is a path with a paint declaration. The kind's output is regions and answers. The waist is the line between data anyone can make and the four pieces of code that must exist once.

[FIGURE: see path-kind.html for the drawing]

Caption: **Should be.** Four sources make one camera-free value: path plus paint plus identity. Below the waist, four pieces of code: the path type, the geometry that makes regions, the packer that lowers them at the camera's tolerance, and the filler the text kind already runs. Answers come from the same path with the same nib definition, so CPU and GPU agree on membership by construction; boundary distance and coverage are compared at a declared error. Brushes sit beside, not inside: ordered dabs through the same filler, surface state only for brushes that read the surface.

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

- Four pieces of code must exist under the value: the path type, the geometry, the packer, the filler; text's filler is that filler. Beside the sources sits a fifth, an evaluator, so that a pen's response or a shape's formula can be data instead of a new generator. Everything else is data: tools, brushes, tips, shape sets, fill rules.

- The contract, then. In: path, paint, identity. Out: regions to the filler, and answers from the same path: hit, distance, bounds, outline, flatten.

- If the stored form changes, these change with it: the grammar, the answers, the envelope builder, the cache keys, both lanes that draw ink, the harness, and a builder and a band packer that do not exist yet.

## Input and output, spelled out

The value the kind takes and the two things it gives. This is what every session on the path kind should be talking about.

### In: path + paint + identity

```
path
 subpaths* each: segments*, closed?
 segment line | quad | cubic (cubic in; quads at the packer)
 knot attributes? width, opacity, pressure, time (per knot, optional)
 knot ids stable, so split and join keep identity

paint two independent declarations
 fill? { rule nonzero | even-odd, colour }
 stroke? { tip { construction nib | ribbon, shape,
 size | per-knot width | width fn(pressure, s),
 unit local | device },
 cap, join, miter-limit,
 align center | inside | outside, (closed paths)
 dash? [on, off, phase],
 overlap union | accumulate { spacing },
 colour }
 neither draws nothing; both = bordered shape

identity material id, revision, container (group id)
tolerances three, none in the value: the source's fit (in the
 record), a consumer's request (a parameter of the
 answers), the device's (the packer's)
```

Open path plus fill: the fill closes it implicitly, as PostScript does. Open or closed matters only to the stroke: whether a closing segment is drawn and whether the start gets caps or a join. A varying width means the swept round nib: the region is the union of discs of radius w(t)/2. Today's rule, perpendicular distance to the nearest centerline point against the width there, is a ribbon with end discs, an approximation that diverges at every taper. Width has a unit: a width in device pixels keeps the value camera-free but makes the envelope a per-scale quantity (session 9, the border walk). Alignment and dash are declarations the pen tools of the field carry; both are operations on the same path (bench 9). A nonlinear response is not the linear interpolation of endpoint widths (the definer, attack 1: interpolate the pressure, then square it); so the stroke may carry the width as a function of the interpolated source attribute, evaluated where the geometry needs it, and per-knot widths are the constant-between-knots case. Accumulate names ordered dabs, each a region through the same filler, painted in order; union names one region painted once.

### Out: two kinds of answer

```
to the GPU (per painted region)
 cover geometry bbox rect, or the cells the region covers
 (boundary cells and interior cells)
 curves quadratics in local units, consistently wound
 bands per row and per column: which curves cross it
 instance row paint, group index, rule
 ⇒ one coverage draw, resolved once, valid at any zoom

to the CPU / other kinds
 classify(point, slop) → :inside | :boundary | :outside
 (winding with curves, same rule as the GPU)
 bbox (local)
 outline the stroke's skin, as curves
 offset(path, d) one side's edge; the ring between path and offset
 is the inside or outside stroke (session 9)
 evaluate(t), tangent(t), length ↔ t dashes, dabs, text on a path,
 arrow caps (session 9; the definer, attack 1)
 intersections(a, b) → (seg, t, seg, t) the crossing edit, faces
 split(seg, t) with an id map so a split keeps identity
 flatten(τ) polyline or triangles, for mesh consumers; keeps
 which segment and parameter each point came from
```

The GPU output is the shape of what the text renderer already uploads per glyph: a rect, curve and band data, a 100-byte instance row (CHECKED). The kind never emits anything that depends on the camera; the packer derives tolerance from camera, group and DPR itself. Membership (winding under the rule) is one definition shared by every implementation; boundary distance and pixel coverage are approximations of it, each compared against it at a declared error (the definer, attack 1). Answers return to constructions, not only to the renderer: a saved construction measures, splits, intersects and reuses before deciding to draw.

### When does a value change? That is the axis, not CPU versus GPU

| Changes… | Values | Where it may live | Today |
|---|---|---|---|
| per edit | path, paint, source record, identity | CPU; stored; uploaded once | stored as samples; cached triangles per zoom band |
| per camera scale | flattening tolerance, cubic→quad lowering, bands, cover cells; the envelope of a stroke whose width is in device pixels | the packer's cache, keyed on projected error from the full transform | three hand-set bands; only cap roundness changes; whole frame repacked on a crossing |
| per frame | transform, coverage; snapping to the device grid when declared (it depends on the pan) | GPU, in practice; snapping is a repack of a small path at frame rate | zoom and pan already a 24-byte camera write |

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
| pressure, or speed when the device has none → width; taper along the length | source · evaluator | draw tool | **not named** | yes: an expression over p, s, v and the record's numbers |
| samples → knots at τ → spline through them | source · builder | draw tool | named | yes: decimate at τ, Catmull-Rom cubics; τ 0 keeps every sample; "polyline" is also a path |
| formula → path (a rect: 4 lines, 4 arcs as cubics) | source · generator | border | sources "not kinds"; the generator's code unnamed | yes; the generator is code. A parametric shape as data needs the evaluator |
| anchors → path | none | pen tool | — | a copy |
| envelope: tip nib or ribbon, caps, joins | geometry | every stroke | named; caps and joins "on top" | yes: one outline tracer, the two tips one line apart, joins fold through the pivot |
| stroke alignment: the ring between the path and its offset | geometry · offset(path, d) | pen tool, border | **not in the answers** | yes, on the flattened centerline |
| dash by arc length | geometry · length | draw tool's dashed style, pen tool | **not named** | yes |
| cubic → quad at τ; bands; cover | packer | all | named | yes; bands sorted by far edge so the shader's early exit holds; cells the outline touches plus interior cells |
| width in device pixels → envelope per scale | geometry, at the packer's rate | border | **not named**; "camera-free" stated without the unit | yes; the rate badge moves from per edit to per scale |
| snap knots to the device grid | packer · per frame | border | named as a policy on the paint or group | yes; the repack runs on every camera move and says so |
| coverage under a rule, one resolve per region | filler | all | named | the tree's shader |
| clip by a path (a mask) | compositor, or the filler with a second region | pen tool's "use as mask" | named as "on top" | no |
| arrow caps: a shape placed at the end along the tangent | geometry · tangent(t) | pen tool | **not named** | no |
| vector-network faces | a source compiler | pen tool's fill by face | "above the kind" | no; code if ever wanted, and out of the kind |

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

### What this changes on the page, and what it only costs

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
| Paint and compositing (clips, blends) sit on top. | DEFAULT | Holds for the union semantic: one region, painted once. The painting-app semantic where a translucent stroke darkens where it crosses itself is not a second rasteriser: it is ordered dabs, each a region through the same filler, painted in order (the definer, attack 1; bench 9's "the crossing, as dabs": twenty-four dabs on the harness Z, two on the crossing, alpha 0.62 + 0.38 × 0.62 = 0.8556). What does need more is a brush that reads the surface it paints on (smudge, wet edges): an ordered surface operation with state that survives, beside the geometry (Procreate, Krita; FIELD). Condition: if such brushes become first-class, that surface lane joins, sharing the compositor's offscreen targets; the filler is unchanged either way. |
| Resolution independent: exact at every zoom. | DEFAULT | Right for an infinite canvas. UI libraries want a one-pixel border to be one crisp pixel, and analytic coverage at a fractional position gives two grey pixels instead. Every UI renderer carries a pixel-snapping policy the model is silent on (FIELD). Condition: the day a UI library is built above this kind, snapping becomes a declared option on the group or the paint, not a change to the filler. |
| The model is exact. | DEFAULT | Circles and arcs are not Béziers; a rounded rectangle's corners are approximations at a declared error. The envelope of a cubic is not a Bézier either, its offset is degree ten (FIELD). So a tolerance exists somewhere in every route. Position: it lives in the packer and the flattener as a parameter, never in the stored value. |
| The model says how to draw. | SHORT | It says nothing about cost. Text's filler draws each glyph as one dilated rectangle and evaluates every pixel inside it (CHECKED). A glyph is compact. A stroke across the canvas is a thin diagonal in a huge box, and per-pixel cost is box area times curves per band. The cover policy, bbox or only the cells the region covers, is outside the model and decides whether the route is feasible for ink. Vello and Pathfinder tile for exactly this reason (FIELD). |
| Samples become a curve; store the curve. | DEFAULT | The pen reported positions eight milliseconds apart; the curve between them is a prior, not data. Fitting is lossy and stylistic: the tolerance is what a user feels as "streamline". tldraw and Google's Ink API keep the samples and derive geometry; PencilKit keeps the spline (FIELD). Position 1: samples are the event truth, the path is derived at a declared tolerance, and a polyline (tolerance zero, the interpolating spline) is a legitimate path. What leaked from the GPU was never the polyline; it was the triangles at zoom bands and the per-piece painting. |

Two corrections to this morning's turns. "Straight segments between samples is the GPU's format leaking" is half right: a polyline is a valid path; what leaked is the triangle cache keyed by zoom band and the per-piece painting that double-blends. "Stroke is the fill of the region within w/2 of the centerline" is true for PostScript and too narrow for ink: stroke is the nonzero fill of the swept tip's envelope, and constant width is the case where the tip does not change.

## The fork below the waist

The contract above is stable across the field; the program under it is where teams differ, by workload and platform. Four shapes fit under this contract. The current flat triangle lane is not one of them: it has no coverage, it consumes geometry with a pixel quantity baked in, and it disagrees with the CPU definition at overlaps.

| Renderer shape | Antialiasing | GPU memory | Zoom | Strokes | Cost to reach from here |
|---|---|---|---|---|---|
| the text filler, made online | analytic, per pixel | small: curves + bands | camera move; lower cubics→quads per scale bucket | via the envelope, filled nonzero | a packer (cubic→quad, bands, cover cells) at pen-up; dynamic textures; unifies text and paths |
| stencil-then-cover + MSAA | MSAA 4× | large: multisampled target, 40 MB at 1080p, 170 MB at 4K | flatten per scale bucket | today's quads + fans into the stencil → union | smallest change from today; removes bridging and ear-clipping |
| compute coverage (Vello-shaped) | analytic | transient, scene-sized | re-flatten on GPU every frame | expansion on GPU | largest lift; best known end state |
| capsule / distance lane | analytic from distance | small | camera move | today's CPU rule, a ribbon with end discs, per segment; the swept nib needs the disc union per pixel; round only | strokes only; no fill story; ideal as the wet-ink lane under the pen |

Memory goes down, not up, on the first road. A hundred-sample stroke today at the default band is about 310 triangles, 930 vertices at 28 bytes, near 26 KB per band (DERIVED from the layouts). As curves it is roughly seventy quadratics at 16 bytes plus band references and one 100-byte instance, near 3 KB, valid at every zoom. The GPU-memory worry that comes with local agents is real, but it lives in render targets, the MSAA road, not in path data.

positionSettle the contract first; it is the part that lives. Under it, the region lane to build is the text filler made online, because it is the shape already in the tree, it needs no multisampled target, and it gives glyphs, ink and shapes one lane. Keep a capsule lane for the stroke under the pen, committing to the filler on pen-up; wet and dry ink is how shipping ink systems handle latency. Conditions that flip this: if banding per pen event proves too slow and the wet lane is not enough, stencil-then-cover is the cheaper correct road; if long thin strokes dominate and cover cells do not cut the cost enough, a distance-based stroke program is the fallback, fed by the same value.

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
| new, no home today | Nothing. No pen input anywhere in client/; no shape generator; no circle fixture. | The builder (samples → curves, appended live, at τ), the evaluator for responses and formulas written as data, the band packer, the cover policy, offset and arc length as answers, and a wet route while the pen is still down. Bench 9 has a first cut of each but the wet route, in JS. |

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

Same filler, different sources. tldraw's set fits entirely: formula shapes, outline shapes, text, and a freehand tool whose envelope polygon is this kind's envelope step (FIELD). Figma's set needs three more things above the kind: vector networks as a source that compiles faces to paths, a boolean library, and raster effects in the compositor's offscreen targets. UI libraries need pixel snapping. None change the input or output of the kind. Today no generator exists: every rectangle in the tree is four typed corners (CHECKED).

**What are the current path files doing, and do the per-kind renderers match "the renderer"?**

Read off the today picture: the harness is the only maker and only caller, a library without a host. Half of "the renderer" already exists as a frame contract all four kinds obey: device and format in, the shared camera and group buffers bound, an open pass at draw time, group index baked at prepare (CHECKED). What differs is the program each kind runs. Under the position there is a small fixed set of programs, coverage filler, textured quad, 3D mesh plus composite, present, and kinds are sources that feed them. The path program is the one that should not exist; the text program is the one that should be shared.

**Am I reasoning from how the code does it today, or from how it should be done?**

This page was derived field-first and then checked against the tree. The field-first derivation lands on the same shape as this morning's turns, path in, coverage out, text's filler shared, and corrects three things: the polyline is not the leak; the stroke must be the envelope of a swept tip, not the PostScript stroke; and the cover policy is a real design item the glyph case never had. "That is what text does today" is evidence of feasibility, not the reason for the design.

## Positions, each with the condition under which it stops being the answer

**position 1the stored form**

Store the burst raw (samples with pressure, time) as the event that happened. Derive the path at a declared tolerance and store that too; it is what gets drawn, edited and hit-tested. Tolerance zero is the interpolating spline, one knot per sample, appended live; a fit above zero is a brush setting. The kind takes the path, never the burst. Stops being the answer: never; if fitting turns out to be a user-facing style this is already right, and if a brush engine later needs the raw samples, they are there.

**position 2the imaging model as the output language**

Paths, fill under a rule, stroke as the nonzero fill of a swept tip's envelope, paint and compositing on top. Accumulating brushes are ordered dabs through the same filler (attack 1). Stops being the answer: if brushes that read the surface become first-class, a surface operation with ordered state joins beside the geometry; the path value already carries what it needs to replay a stroke through it.

**position 3one filler, shared with text**

Coverage under a rule, per pixel, analytic antialiasing, one resolve per region. Strokes reach it through the envelope. A capsule lane serves the wet stroke under the pen. Stops being the answer: if long thin strokes dominate and cover cells do not cut the cost enough, a distance-based stroke program is the fallback, fed by the same value; if banding per pen event is too slow and the wet lane is not enough, stencil-then-cover is the cheaper correct road.

**position 4zoom leaves the stored value and its identity**

The path value and its content hash carry no zoom. Per-scale caches are legitimate, but they belong to the packer and are keyed on projected error from the full transform, never on hand-set bands in the kind's own key.

**position 5cubic in, quadratic at the packer**

The stored path allows cubics because fitters and design tools produce them; the packer splits to quadratics for the shader. Stops being the answer: if a cubic-capable filler is written, the packer step disappears and nothing above notices.

**position 6placed ink takes the coverage lane on a plane**

The filler uses screen derivatives for its pixel scale, so it works under the region's projection; placed ink becomes an instance row drawn on the object's z=0 plane, and the 88-byte triangle road is cut. flatten(τ) stays a geometric answer for consumers that need lines, not a render road.

**position 7 · the open forkwhat a varying width means**

The stroke region is the swept round nib: the union of discs of radius w(t)/2 along the curve. Cap and join constructions (butt, square, miter, bevel) are declared operations on the ends and corners on top of it, as PostScript adds them. The ribbon is named as the approximation the current CPU rule computes. Reason: only the nib gives a tip shape a meaning, and it is what a pen physically does. Stops being the answer: if a designer stroke tool wants the ribbon's crisper tapers, both constructions are named operations and the declaration picks one. This is Sid's to close, verbatim and timestamped; asked once. The sliver is visible on 4's bench and on bench 9: pick the pressure-ink fixture, switch the tip, and the cursor shows what each construction says and flags where they disagree. Sharpened by the waist test: tldraw's own construction is the ribbon (FIELD), so the ribbon must stay nameable whichever is the default; on bench 9 the two are one line apart in the same tracer.

**position 8behaviour as data needs an evaluator**

A tool's settings are numbers until one of them is a function: width from pressure, a shape's corners from its parameters, a taper along the length. A function written in a record is data only if code below the waist can run it. Both chairs reached this from opposite sides on the same day: the composer's evaluator (bench 9 runs one: arithmetic over pressure, arc length, speed and the record's numbers) and the definer's construction executor (attack 1: arithmetic and vector arithmetic, conditions, iteration over collections, an ordered accumulation, record updates, calling another saved construction, and calls to the geometry's answers and to a surface). They are the same piece at two sizes; the evaluator is the smallest member. It sits beside the builder in the source stage, and the geometry's answers return to it: a construction measures, splits, intersects and reuses before it draws. The three doors: a fixed menu of responses and generators, each new behaviour new code; an evaluator of that scope; or code as data, Clojure forms interpreted at runtime (sci), which is the general answer and a substrate decision. Stops being the answer: if Sid rules that tools are code above a data-only contract, the executor dies and each response is a generator. Not asked yet; it closes when brushes or shape formulas are wanted as data, not before.

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

Landed under `docs/below-the-waist/path-kind/`: this page and its `.md` twin, session 8's fact base with the hunter reports and the six closing folds, session 7's brief, page and three hunt reports under `sources-7/`, session 4's bench and its handover under `bench-4/`, session 9's bench under `bench-9/`, its fact base `fact-base-9.md`, and the definer's `attack-1.md`. Positions are marked and the verdicts are Sid's.
