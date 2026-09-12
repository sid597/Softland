The Path Kind's Waist

Softland · 2D engine · working paper · session of 2026-09-05 · revised after the eight-session round

# The Path Kind's Waist

What must exist as code for ink and shapes, what goes in, what comes out, and where the PostScript imaging model holds, is only a default, or falls short. The picture comes first; the story reads off it. Positions are marked and are Sid's to rule on. Nothing here has landed in the repo.

  CHECKED — a hunter read the lines
  DERIVED — inferred from checked lines
  FIELD — recalled from the literature, not verified this session
  POSITION — mine, awaiting Sid's word

## The picture: the path kind as it should be

One grain: pieces, what each takes and gives, and the arrows the code would actually make. Above the waist is data anyone can author. Below is the small fixed set of code the data feeds.

[FIGURE: see the .html for the drawing]

Caption: Four sources derive one camera-free input. Below the waist, four pieces of code turn it into coverage draws and geometric answers. The filler is the program the text kind already runs per pixel; text becomes one more source above it.

## The story, read off the picture

- Everything on the canvas ends as one question the GPU answers per pixel: how much of me is inside this region, and what colour goes there.
- A region is a closed outline made of curves, plus a rule for what "inside" means where the outline crosses itself.
- Text already works this way. A letter is a closed outline of quadratic curves, and the text shader answers that question with anti-aliasing built in. [CHECKED]
- A shape, whether rectangle, star or blob, is a closed outline of curves too.
- A pen stroke is not an outline. It is a centreline with a width that changes along it.
- To draw a stroke you first make its skin: the outline of the region the pen tip swept. Then it is drawn like anything else.
- Skin-making is the hard geometry. It is pure, camera-free, CPU-side. Today's tapered quads and round fans are a first version of it, delivered in the wrong output. [CHECKED]
- What gets stored for a stroke is the samples (what the pen did) and a smooth curve fitted to them at a declared looseness (what we draw and edit). Both are camera-free.
- Zoom becomes a camera move because nothing stored or cached knows the zoom. Today the fan resolution does. [CHECKED]
- The code that must exist for this kind is four pieces: the path type, the geometry, the packer, and the one filler. Text's filler is that filler, packed for glyphs.
- Everything else is data feeding those: tools, brushes, tips, cap styles, shape libraries, fill rules.
- The imaging model is the right language for the output. It is not the right language for storing ink, because its stroke has one width and a pen does not.
- Where the model is only a default: brushes that darken where they overlap (a raster world), crisp one-pixel UI borders (a snapping policy), and very long thin strokes (a cover policy).
- The knitted sweater is not a drawing problem. It is a split-and-order edit, and the kind's only job there is keeping segment identity so a split costs nothing.

## Input and output, spelled out

The value the kind takes and the two things it gives. This is the contract every session on the path kind should be talking about.

### Input: path + paint + identity

```
path
  subpaths*          each: segments*, closed?
  segment            line | quad | cubic   (cubic in; quads at the packer)
  knot attributes?   width, opacity, pressure, time   (per knot, optional)
  knot ids           stable, so split/join keep identity

paint                two independent declarations
  fill?              { rule nonzero | even-odd, colour }
  stroke?            { tip  { shape, size | per-knot width },
                       cap, join, miter-limit,
                       overlap union,          (accumulate = raster route, later)
                       colour }
  neither            draws nothing; both = bordered shape

identity             material id, revision, container (group id)
tolerance τ          NOT in the value; a packer parameter
```

Open path + fill: the fill closes it implicitly, as PostScript does. Open/closed matters only to the stroke: whether a closing segment is drawn and whether the start gets caps or a join.

### Output: two kinds of answer

```
to the GPU (per painted region)
  cover geometry     bbox rect, or the cells the outline touches
  curves             quadratics in local units, consistently wound
  bands              per row and per column: which curves cross it
  instance row       paint, group index, rule
  ⇒ one coverage draw, resolved once, valid at any zoom

to the CPU / other kinds
  classify(point, slop) → :inside | :boundary | :outside
                          (winding with curves, same rule as the GPU)
  bbox (local)
  outline            the stroke's skin, as curves
  flatten(τ)         polyline or triangles, for mesh consumers
```

The GPU output is the shape of what the text renderer already uploads per glyph: a rect, curve and band data, a 100-byte instance row. [CHECKED]

## Today, at the same grain

What the four path files and their consumers actually do, from the hunters' reads. Red marks are where the GPU's format, or the camera, has leaked into what a stroke is.

[FIGURE: see the .html for the drawing]

Caption: Two pipelines that never meet. The path lane bakes camera resolution into a triangle cache and paints without coverage; the text lane already owns the general per-pixel filler, wrapped in glyph packing. Every claim on this figure is from the hunters' checked reads.

## The attack on the imaging model

Sid's working basis, claim by claim. HOLDS means it is the answer for this kind. DEFAULT means it works until a named condition, and the condition is stated. SHORT means it cannot express something Sid has already said he wants.

| Claim | Verdict | Why, and the condition |
|---|---|---|
| A path is line, quadratic and cubic segments, open or closed, in subpaths. | HOLDS | As the output language it has no rival: Skia, browsers, PDF, Vello, Rive, Figma's and Flutter's renderers all compile to it [FIELD]. In this tree, text's curve data is already exactly this, quadratic-only [CHECKED]. Cubic in the stored value, split to quads at the packer, is the standard bridge. |
| Fill covers the inside under nonzero or even-odd. | HOLDS | The text shader accumulates signed crossings, which is nonzero by construction; even-odd is the parity of the same count, one line in the shader [DERIVED] from the accumulation at text/renderer.cljs:215-251. Today's path fill has no rule at all and cannot take a self-crossing ring [CHECKED]. |
| Stroke is the fill of the region within half the width of the centreline, with cap and join. | SHORT | That stroke has one width. A pen stroke has a width per knot. The general object is the envelope of a tip swept along the path, attributes varying along it; PostScript's stroke is the special case of a round tip at constant size. Every ink system does this: tldraw's perfect-freehand builds the envelope polygon, Google's Ink API sweeps a brush tip into a mesh, PencilKit interpolates per-point size along a B-spline [FIELD]. Today's tessellator is already a variable-width envelope builder: tapered quads plus round fans at each knot's own radius [CHECKED] tessellation.cljc:211-268. So the reduction "stroke is a fill" survives, but the stroke declaration has to be designed from ink, not from PostScript. And "curve plus varying width" still names two different regions: a ribbon offset perpendicular to the centreline, or the union of round discs of radius w(t)/2 swept along it. They differ wherever width changes fast. Today's CPU classify projects the point onto the segment and reads the width at that foot [CHECKED] component.cljc:176-190, which is the ribbon, and its docstring says it is not an exact solver for tapered outlines. The two must be told apart before CPU and GPU can agree by construction. Sessions 5, 6 and 8 caught this; the first version of this page did not. |
| Boundary and fill are independent declarations; the code must not presume fill. | HOLDS | With one precision: the envelope is always filled nonzero, because the skin of a curved or turning stroke overlaps itself on the inside of every bend and at every join, and nonzero is what turns those overlapping pieces into one region. This is what lets the envelope builder be sloppy: it emits pieces with consistent orientation and the rule does the union. Even-odd on a stroke skin would punch holes. The interior takes whatever rule the fill declares. |
| Paint and compositing (clips, blends) sit on top. | DEFAULT | Holds for the union semantic: one region, painted once. It cannot express the painting-app semantic where a translucent stroke darkens where it crosses itself and each dab lands over the last. That is a raster model (Procreate, Krita) [FIELD]. The overlap semantic is therefore a declaration on the stroke; only union goes through the filler. Condition: if painterly brushes become first-class, a second operation joins, "stamp along a path into a raster target", sharing the compositor's offscreen targets but not the filler. |
| Resolution independent: exact at every zoom. | DEFAULT | Right for an infinite canvas. UI component libraries want a one-pixel border to be one crisp pixel, and analytic coverage at a fractional position gives two grey pixels instead. Every UI renderer carries a pixel-snapping policy the imaging model is silent on [FIELD]. Condition: the day a UI library is built above this kind, snapping becomes a declared option on the group or the paint, not a change to the filler. |
| The model is exact. | DEFAULT | Circles and arcs are not Béziers; a rounded rectangle's corners are approximations at a declared error. The envelope of a cubic is not a Bézier either (its offset is degree ten) [FIELD]. So a tolerance exists somewhere in every route. Position: it lives in the packer and the flattener as a parameter, never in the stored value. |
| The model says how to draw. | SHORT | It says nothing about cost. Text's filler draws each glyph as one dilated rectangle and evaluates every pixel inside it [CHECKED]. A glyph is compact. A stroke across the canvas is a thin diagonal in a huge box, and per-pixel cost is box area times curves per band. The cover policy (bbox, or only the cells the outline touches) is outside the model and decides whether the route is feasible for ink. Vello and Pathfinder tile for exactly this reason [FIELD]. |
| Samples become a curve; store the curve. | DEFAULT | The pen reported positions eight milliseconds apart; the curve between them is a prior, not data. Fitting is lossy and stylistic: the tolerance is what a user feels as "streamline". tldraw and Google's Ink API keep the samples and derive geometry; PencilKit keeps the spline [FIELD]. Position: samples are the event truth, the path is derived at a declared tolerance, and a polyline (tolerance zero) is a legitimate path. What leaked from the GPU was never the polyline; it was the triangles at zoom bands and the overdraw. |

### Two corrections to the earlier turns today

- "Straight segments between samples is the GPU's format leaking." Half right. A polyline is a valid path in the imaging model. What leaked is the baked triangle cache keyed by zoom band, and the per-piece painting that double-blends. Those go; the samples stay as truth.
- "Stroke is the fill of the region within w/2 of the centreline." True for PostScript, too narrow for ink. The general statement is: stroke is the nonzero fill of the swept tip's envelope. Constant width is the case where the tip does not change. The current tessellator already lives in the general case and should keep doing so.

## If the stored form changes, what changes with it

Sid's last question. Each row is one file or one missing piece, with what it does today from the hunters' reads and what it becomes.

| Where | Today [CHECKED] | Becomes |
|---|---|---|
| path/component.cljc | Two grammars: :ink samples with a width each, cap/join round only; :shape rings tagged outer/hole. No curves, no rule, no closed flag; pressure and time accepted and never read. | One grammar: the path value above. :ink and :shape stop being kinds; they are sources. Outer/hole roles die: winding direction or the rule carries that. |
| content hash, mesh cache key | Key = canonical geometry + algorithm version + zoom band. Frame key carries the band too. | Zoom leaves both. Tolerance enters the packer's cache key if flattening is cached. |
| path/tessellation.cljc | Envelope builder for polylines (tapered quads, round fans, resolution by band) and a polygon triangulator (bridge holes, ear clip, O(n³) worst case, throws on no visible bridge). | Splits in two. Geometry: the envelope builder generalised to curves and tips, emitting a consistently wound outline; the triangulator dies, nonzero handles holes and overlaps. Packer: new. Cubic to quad, band building, cover cells. Fonts get banding offline today; user paths need it at pen-up, in cljc, fast. |
| path/renderer.cljs | Flat-colour triangle program, 28-byte vertices, no coverage, whole-buffer repack. | Dies as a program. Path becomes a producer of instance rows for the shared filler. |
| text/renderer.cljs, text/glyph_pack.cljs | The filler and the glyph packer in one namespace; em-box and glyph-id assumptions in the packer; 100-byte instance rows. | The filler moves out to be shared, gaining a rule parameter and a cover-cell mode. Glyph packing stays as the glyph source's packer. Text's own contract does not change. |
| path/frame.cljc | Dependency key = material, revision, container, plus zoom band. | Same key without the band. |
| CPU classify | Ink: distance to segments minus interpolated half-width. Shape: odd-even ray per ring, then outer-and-not-hole. Parity with the GPU tested only on the holed shape, never on ink. | Winding number with curves, the CPU twin of the shader's root counting, so CPU and GPU agree by construction. Parity runs on ink too. |
| region3d/on_plane* | Reuses the same tessellator and cache at zoom 1, uploads 88-byte vertices, draws on a plane with 4× MSAA. | Fork. Either a placed-coverage pipeline (the filler uses screen derivatives, so it works under any projection [DERIVED]), or keep flatten(τ) → triangles as the kind's second output so this lane survives untouched. Position: the second, until the first is wanted. |
| harness/path.cljs | Literal bursts and rings; byte-identical goldens; a translucent self-crossing case that declares the double-blend. | Fixtures gain the fit step; goldens change; the self-crossing case becomes the proof that overlap resolves once. |
| New, no home today | Nothing. No pen input anywhere in client/. | The fitter (samples → curves at τ), the band builder, the cover policy, and a live-stroke preview route while the pen is still down. |

## How the best team in the field would define it, and what they would name

Sid asked to be told the trade-offs he does not know to ask about. The problem statement first, then the list. Everything in this section is [FIELD] unless marked.

**THE PROBLEM AS THEY WOULD STATE IT** Define a resolution-independent scene language as the output: paths, paint, compositing. Keep every source above it with its own editable truth (samples, radii, glyph ids). Pick the rasteriser by cost profile and keep it swappable behind that language. Decide up front, because they cannot be retrofitted: the anti-aliasing policy, the overdraw policy, the stroke model, the overlap semantic, where tolerance lives, and whether raster brushes are in scope.

- Tolerance is a screen-space quantity. Every route has "how smooth at this zoom" somewhere. Camera-free data does not remove it; it moves it into per-frame work. Vello flattens on the GPU every frame at the current scale; Slug pays per pixel instead. Name where it lives (packer or flattener) and keep it out of the value.
- The envelope has cusps and inner loops. Offsetting a curve inward past its radius of curvature folds the outline over itself. That is not a bug to remove; it is why the envelope must be filled nonzero, and why the builder needs consistent orientation, not a clean boundary. Levien's stroke-expansion work is the reference.
- Overdraw and conflation. Painting a region as many pieces double-blends translucent paint where pieces overlap, and anti-aliasing pieces separately leaves seams where they meet. The fix is structural: one resolve per region. Today's path lane has the first defect and declares it [CHECKED] harness/path.cljs:558-559.
- The cover policy. Bbox covers are right for glyphs and wrong for a stroke across the canvas. Cells touched by the outline, computed in local units at pack time, keep the cover tight and camera-free. This is the one thing the text lane never needed and ink needs from day one.
- Curve degree. Design tools and fitters produce cubics; the shader wants quadratics. Splitting multiplies the curve count roughly two to four times. Arcs and circles approximate either way. Decide the canonical stored degree (cubic) and convert once at the packer.
- Bands at edit time. Fonts are banded offline by a tool outside client/ [CHECKED]. A user path must be banded on pen-up, in the same pure language as the rest, fast enough that it is not felt. This is real work and it does not exist yet.
- The wet stroke. While the pen is down the burst is unfinished. Fitting and banding the whole stroke on every sample is the wrong cost. Every ink system has a cheap preview route for the live tail (polyline envelope at tolerance zero, or only the last few segments) and commits the fitted form at pen-up. The imaging model says nothing about this; latency does.
- CPU and GPU must use the same rule. Hit-testing on the CPU and coverage on the GPU disagree at self-crossings if one is odd-even and the other nonzero. The harness already tests parity for shapes [CHECKED]; the twin implementation keeps that honest.
- Pixel snapping for UI borders, as above. A policy, not a filler change.
- Raster brushes are a second model. Procreate-style dabs, textures and wet edges live on a pixel canvas. In scope or not is a product decision; the path value should carry enough (samples, pressure, time) that a brush engine can re-render a stroke later.
- Booleans and vector networks. Figma's fill-by-face and union/subtract need a planar-arrangement library, the classic pit. It sits above the kind. The path value must not preclude it: multiple subpaths, both rules, stable ids.
- Precision on an infinite canvas. Coordinates narrow from float64 to float32 once at upload and are never rebased around the camera, so at the legal zoom of 1000 vertex jitter is real [CHECKED by session 8's hunter, not re-read here]. Group affines keep locals small, but curves in local units inherit the same narrowing. Rebase relative to the camera before upload. The first version of this page said "nothing new" here; that was wrong.
- Memory goes down, not up. A hundred-sample stroke today, default band: about 310 triangles, 930 vertices at 28 bytes, near 26 KB per band [DERIVED] from the layouts. As curves: roughly seventy quadratics at 16 bytes plus band references and one 100-byte instance, near 3 KB, valid at every zoom. The GPU-memory worry that comes with local agents is real but it lives in render targets, not in path data.

## Sid's questions, one by one

**Q: Is the sampled pen stroke the general case of formula shapes and designer outlines, or is something else the general object?**
Is the sampled pen stroke the general case of formula shapes and designer outlines, or is something else the general object?
Something else. The general geometric object is the path; the pen burst is the least-structured source of one. But the general stroke object has to be designed from ink, because ink is the first thing Sid wants to draw and PostScript's stroke cannot express it. Least structured is not most general: a circle stored as a radius stays a circle when dragged; the same circle as two hundred samples has lost even the curve.

**Q: CPU versus GPU: how is it decided, and is it the right axis?**
CPU versus GPU: how is it decided, and is it the right axis?
Not the right axis. The axis is whether the camera is baked into what you store or cache. Today it is: fan resolution by zoom band, in the mesh key and the frame key [CHECKED]. Text bakes nothing and the same bytes draw at any zoom [CHECKED]. Once nothing cached knows the zoom, where the per-frame work runs is a performance choice that can change later without touching the value. The local-agents memory question is real and parked; the arithmetic above says path data is not where it bites.

**Q: Why does Slug exist for text, is it glyph-only, and is it what ink and shapes need?**
Why does Slug exist for text, is it glyph-only, and is it what ink and shapes need?
It exists so a letter is exact at every zoom without an atlas: curves and bands in textures, a per-pixel root count. The per-pixel core reads no font size and no glyph id [CHECKED]; the glyph-ness sits in the packer (em-normalised bounds, one-over-size jacobian, glyph lookup) and in the offline band bake. It is exactly what shapes need, after cubic-to-quad. It is what ink needs after the envelope step, never directly: it fills, it does not stroke, and it has no distance function. Two things it lacks for user paths: a live band builder and a cover policy for long thin outlines.

**Q: A stroke that crosses itself, and the knitted sweater.**
A stroke that crosses itself, and the knitted sweater.
Three different things. A closed shape that crosses itself is the fill rule, a declaration: nonzero fills the pentagram's centre, even-odd leaves it empty. A stroke that crosses itself is the overlap semantic: union paints the crossing once, which is what nonzero coverage gives and what vector tools do; accumulate darkens it, which is the raster route. Neither gives "under". Under means an earlier part of the same stroke sits on top of a later part, and one region has one paint order. So the sweater is a split-and-order edit the user makes, and the kind's job is stable knot ids so a split does not lose the stroke's identity.

**Q: Pre-baked and user-defined shapes: same drawing, and can tldraw's and Figma's sets be built above this?**
Pre-baked and user-defined shapes: same drawing, and can tldraw's and Figma's sets be built above this?
Same filler, different sources. tldraw's set fits entirely: formula shapes, outline shapes, text, and a freehand tool whose envelope polygon is this kind's envelope step [FIELD]. Figma's set needs three more things above the kind: vector networks as a source that compiles faces to paths, a boolean library, and raster effects (shadow, blur) that belong to the compositor's offscreen targets. UI component libraries need pixel snapping. None of those change the input or the output of the kind.

**Q: Do the current per-kind renderers match what "the renderer" should be?**
Do the current per-kind renderers match what "the renderer" should be?
Half of "the renderer" already exists as a frame contract all four kinds obey: device and format in, the shared camera and group buffers bound, an open pass at draw time, group index baked at prepare [CHECKED]. What differs is the program each kind runs. Under the position there is a small fixed set of programs (coverage filler, textured quad, 3D mesh plus composite, present) and kinds are sources that feed them. The path program is the one that should not exist; the text program is the one that should be shared.

**Q: Am I reasoning from how the code does it today, or from how it should be done?**
Am I reasoning from how the code does it today, or from how it should be done?
This page was derived field-first and then checked against the tree. The field-first derivation lands on the same shape as this morning's turns (path in, coverage out, text's filler shared) and corrects three things: the polyline is not the leak; the stroke must be the envelope of a swept tip, not the PostScript stroke; and the cover policy is a real design item the glyph case never had. "That is what text does today" is evidence of feasibility, not the reason for the design.

## Positions, with the condition under which each stops being the answer

**POSITION 1 · the stored form** Store the burst raw (samples with width, pressure, time) as the event that happened. Derive the path at a declared tolerance and store that too; it is what gets drawn, edited and hit-tested. The kind takes the path, never the burst. Stops being the answer: never; if fitting turns out to be a user-facing style, this is already right, and if a brush engine later needs the raw samples, they are there.

**POSITION 2 · the imaging model as the output language** Paths, fill under a rule, stroke as the nonzero fill of a swept tip's envelope, paint and compositing on top. Stops being the answer: if painterly raster brushes become first-class, a second operation joins (stamp into a raster target); the path value already carries what it needs.

**POSITION 3 · one filler, shared with text** Coverage under a rule, per pixel, analytic anti-aliasing, one resolve per region. Strokes reach it through the envelope. Stops being the answer: if long thin strokes dominate and cover cells do not cut the cost enough, a distance-based stroke program is the fallback, fed by the same input value.

**POSITION 4 · zoom leaves the stored value and its identity** The path value and its content hash carry no zoom. Per-scale caches are legitimate, but they belong to the renderer and are keyed on projected error from the full transform, never on hand-set bands in the kind's own key. The first version said "every key, unconditionally"; sessions 1, 3, 5 and 6 were right that a derived approximation may depend on the view.

**POSITION 5 · cubic in, quadratic at the packer** The stored path allows cubics because fitters and design tools produce them; the packer splits to quadratics for the shader. Stops being the answer: if a cubic-capable filler is written, the packer step disappears and nothing above notices.

**POSITION 6 · placed ink takes the coverage lane on a plane** The filler uses screen derivatives for its pixel scale, so it works under the region's projection; placed ink becomes an instance row drawn on the object's z=0 plane, and the 88-byte triangle road is cut. The first version kept triangles "until a placed-coverage pipeline exists", which is keep-until-better, and that is not a form this project allows. flatten(τ) stays a geometric answer for consumers that need lines, not a render road.

**POSITION 7 · what a varying width means** The stroke region is the swept round nib: the union of discs of radius w(t)/2 along the curve. Cap and join constructions (butt, square, miter, bevel) are declared operations on the ends and corners on top of it, as PostScript adds them. The ribbon is named as the approximation the current CPU rule computes. Reason: only the nib gives a tip shape a meaning, and it is what a pen physically does. Stops being the answer: if a designer stroke tool wants the ribbon's crisper tapers, both constructions are named operations and the declaration picks one.

## Folded in from the other seven sessions on this prompt

Eight sessions answered this round; each was ranked by the others. These are the items the peers had that this page lacked, with who found them. All are adopted here.

- Ribbon versus swept nib (sessions 5, 6; session 8 corrected itself on it). Now Position 7 and the stroke row above.
- The record is already cooked (session 2). Width is required per sample and pressure is optional [CHECKED] component.cljc:41-50, so a pen model ran before storage and a better pen can never reach old strokes. Store the gesture; derive width through the pen as data.
- Write the exact image as a definition first (session 2). One slow reference renderer that produces it; every fast lane is tested against it. The harness parity check is the seed of this and today it skips a band of pixels along every boundary [CHECKED] harness/path.cljs:341-393.
- List every reader of the stored record before shaping it (sessions 2, 4): screen, hit test, editor, 3D placement, file, other people. The record must serve all of them.
- Two intended pictures one input cannot tell apart (session 1). The test for whether a field is missing. The sweater passes it in one line; so does closed-versus-filled, and translucent union versus accumulate.
- Three parts of the client answer "inside" three ways (session 4): CPU pick is odd-even per ring, the text shader is nonzero, the ear clipper throws on a self-crossing ring [CHECKED] component.cljc:199-218, text/renderer.cljs:215-251, tessellation.cljc:480-525. One truth, one rule.
- A brush field in a record does not make brushes data (session 5). Unless the machinery below executes the arithmetic, sampling and surface reads, the waist has not been achieved. Same for "region": naming it does not discharge the geometry.
- Sort work per edit, per scale, per frame (session 8), then place it. Today a per-scale quantity, fan resolution, sits in the per-edit cache key.
- Five places decide how big a pixel is (session 8): the path bands, region3d's lease size and its 1.12 bucket ladder, the placement zoom of 1.0, text's snap step. Derive pixels-per-local-unit once from camera, group and DPR.
- The variable-width fixture is built and never run; the harness declares the CPU classifier the authority and stamps the double-blend [CHECKED] harness/path.cljs:245-246, 518, 556-559. The overlap semantics is already declared on one side and violated on the other.
- No shape generator exists (session 8): every rectangle in the tree is four typed corners; no circle fixture; no cos, sin, arc or bezier in any maker.
- The renderer fork, with memory (session 8): analytic coverage (small GPU memory, curves plus bands), stencil-then-cover with MSAA (a multisampled target, hundreds of MB at canvas size), compute tiles (transient, scene-sized, the largest lift), and a capsule lane for wet ink only. The parked local-agents question lands on the MSAA road, not on path data.
- Vector or raster ink is a decision (session 4), answered here by a raster lane beside the path kind, not a change to it.
- A bench (session 4 built one). Things you can feel come before documents about them; this page still owes one.

## Fact base

Three hunter reports, one per disjoint slice, every fact tagged and anchored to file:line. They are the raw material for every CHECKED mark above.

- scratchpad/hunt-path.md — path/component, tessellation, frame, renderer, harness/path: the grammar, the tessellation steps and bands, the vertex layout and shaders, the CPU classify, the fixtures.
- scratchpad/hunt-text.md — text/fonts, shaper, glyph_pack, renderer, shaped_line: the slug technique per pixel, the texture layouts, the 100-byte instance, what is glyph-specific and what is general.
- scratchpad/hunt-engine.md — engine/*, region3d placed ink, harness frame drivers: zoom and rungs, the camera and group buffers, the renderer contract across the four kinds, the placed-ink call chain.

Full path of the scratchpad: /tmp/claude-1000/-mnt-data-projects-Softland/344f9292-3a47-4b8c-bd37-0ec01e872808/scratchpad/. At Sid's word this page lands as .md and .html twins under docs/ so it can be contested on disk.
