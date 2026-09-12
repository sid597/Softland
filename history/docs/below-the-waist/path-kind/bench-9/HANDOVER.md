# The Waist Bench (bench 9) — handover, session 9, 2026-09-06; session 10's compositor and executor, and session 11's results, checkpoint and group mask, and session 12's result reuse (a clip bound to a returned path, liveness through records, a checkpoint as bytes), added the same day (sections at the end)

## Where it is

- Source: `docs/below-the-waist/path-kind/bench-9/waist-bench.html` (one file, ~95 KB, WebGL2, no build; only a Google Fonts link, with fallbacks).
- Live: https://claude.ai/code/artifact/da84fdb6-604a-488e-b797-e77c6f257cbc . To republish from another session: `Artifact` action `read` with that URL first, then publish with `url` set and this file as `file_path`.
- Run locally: open the file in any WebGL2 browser (`file://` is fine). Headless check: `google-chrome --headless=new --use-angle=swiftshader --use-gl=angle --enable-unsafe-swiftshader --window-size=1500,1000 --virtual-time-budget=4000 --screenshot=out.png "file://…/waist-bench.html#tool=pen"`; `--dump-dom` instead of `--screenshot` prints the panel's readouts.
- Deep links: the URL hash carries the whole state, e.g. `#tool=pressure&tip=ribbon&outline=1&zoom=6`. Keys: `tool`, `tip`, `cap`, `join`, `align`, `unit`, `dash`, `snap`, `fill`, `stroke`, `overlap`, `clip`, `blend`, `group` (`none` | `layer` | `mask`, session 11), `checkpoint` (`none` | `12`, session 11), `save` (`cpu` | `gpu`, session 12: presses a save-to-bytes button once the in-memory checkpoint exists), `paper`, `cover`, `bands`, `skin`, `outline`, `zoom`, `cx`, `cy`, and `at=x,y` (session 10: a deep-linked cursor, so a readout can be quoted from `--dump-dom`; the panel's text is nested, read it with an HTML parser rather than a regex).

## What it is

The waist test as a thing to feel. The right column is laid out as the waist: the tool's record above (data, editable JSON), the kind's value on the line (derived, read-only), the pieces of code below, each reporting what it did and at which rate (per edit / per scale / per frame). The canvas draws the result through one pipeline: source → path → envelope → packer → filler. The filler is the text lane's per-pixel program (`text/renderer.cljs:139-264`, the Slug algorithm) in GLSL, with two changes: the band texture width is a uniform rather than a compile-time 4096, and even-odd is the parity of the same crossings.

Local units: 1 unit = 1 CSS pixel at zoom 1, Y down, as in the client. Presets sit in a 0–128 box; the view opens at zoom 3 centred on (64, 64).

## The record (what a tool supplies; the definer's side of the waist)

```
{
  "tool":   { "name": "...", ...numbers the width expression may read (size, thinning, ...),
              "streamline": 0..0.95, "fit": τ | 0 | "polyline", "taperStart": n, "taperEnd": n,
              "simulatePressure": bool, "width": "expression over p, s, v and the tool's numbers" },
  "source": { "kind": "pen",     "samples": [[x, y, pressure?, t?], ...] }
          | { "kind": "anchors", "contours": [ { "closed": bool, "anchors": [ { "id"?, "p": [x, y], "in"?: [x, y], "out"?: [x, y] } ] } ] }
          | { "kind": "rect",    "x", "y", "w", "h", "r" },
  "paint":  { "fill":   null | { "rule": "nonzero" | "evenodd", "color": [r, g, b, a] },
              "stroke": null | { "on"?: bool, "tip": "nib" | "ribbon", "width": number | "knot",
                                 "unit": "local" | "device", "cap": "round" | "butt" | "square",
                                 "join": "round" | "miter" | "bevel", "miterLimit"?: n,
                                 "align": "center" | "inside" | "outside", "dash"?: [on, off],
                                 "overlap"?: "union" | "accumulate", "spacing"?: n,
                                 "color": [r, g, b, a] } },
  "snap": bool,
  "identity": { "id": "...", "revision": n }
}
```

`width: "knot"` means the per-knot widths the source produced (for a pen: the width expression per sample). `width: "<expression>"` on the stroke is the definer's fix from attack 1: the expression is evaluated per flattened point on the *interpolated pressure* (and `s`), so a nonlinear response is not the interpolation of squared knot widths. `overlap: "accumulate"` is the definer's construction: round dabs at arc lengths 0, spacing, 2·spacing … each its own region, painted in drawing order through the same filler; `union` is one region painted once. The expression evaluator accepts `+ - * / ^`, parentheses, `min max abs sqrt pow clamp step smoothstep mix sin cos exp floor`, `pi`, the names `p` (pressure), `s` (arc-length fraction 0..1), `v` (speed, units per ms) and every numeric field of `tool`. A parse error is shown under the record and the width falls back to `size * p`.

## The pipeline (what executes; the composer's side)

1. **source → path** (per edit). Pen: streamline (a low-pass, tldraw's name), simulated pressure from speed when asked, the width expression per sample, taper, decimation at τ (Ramer–Douglas–Peucker), Catmull-Rom cubics through the knots (τ 0 keeps every sample; `"polyline"` emits lines). Rect: 4 lines + 4 arcs as cubics (κ = 0.5523). Anchors: a copy. Knots keep ids (sample indices or anchor ids) and widths.
2. **the value**: `{ subpaths: [ { closed, start, segs: [L | Q | C], knots: [ { id, w } ] } ] }` plus the paint and identity from the record. No tolerance, no zoom.
3. **geometry** (per edit; per scale when the width is in device pixels; per frame when snapping). The envelope tracer: the centerline is flattened at τ 0.1 local (or 0.25 device px for device-unit widths), then one closed outline is traced per open piece (side A forward, end cap, side B backward, start cap) or two loops per closed piece (alignment picks which two: the path itself and its inward offset, the two half-width offsets, or the outward offset and the path reversed, wound opposite). Tangent points per piece: **ribbon** = knot + r·(perpendicular of the piece); **nib** = knot + r·(external tangent normal of the two end discs), which leans with the taper. That is the one line where the two constructions differ. Outer joins: round arc (≤45° per quadratic), miter with a limit, or bevel. Inner joins: a fold through the pivot; it keeps the body's orientation so nonzero absorbs it and even-odd punches it out. Caps: round (an arc through the end direction), butt (the chord between the tangent points), square. Dash: a walk by arc length over the flattened centerline; each dash is an open piece with caps.
4. **packer** (per scale; per frame when snapping). Cubics → quads at τ = 0.25 device px (split count from the third-difference bound). Row and column bands, `ceil(sqrt(n/2))` each, curves sorted by far edge descending so the shader's early exit holds. Cover: one box, or cells the outline's boxes touch plus interior cells (a CPU winding test at the cell centre). Textures: RGBA32F curves, two texels per quad; RG32UI bands, header (count, offset) per band then the index lists; width 1024, wrapping. Snap: knots moved to the device grid, handles carried along, then everything re-runs.
5. **filler** (per frame). The shader above. One coverage draw per painted region; fill first, then the stroke's skin; premultiplied over. The skin's rule is nonzero by definition; the bench lets you switch it to even-odd to see the folds.

## Readouts

- Each piece: what it did, counts (samples → knots → cubics; pieces → outline lines + arc quads; cubics → quads; bands and curves per band; cover rects and device pixels; texels), milliseconds, and its rate badge.
- The filler: ≈ cover pixels × (curves per row band + curves per column band) per region, a count not a timing.
- The cursor: local coordinates; per region the integer winding and the fractional coverage from the CPU twin of the shader, evaluated at the centre of the pixel under the cursor; the composited alpha against the GPU pixel read back; "agree" within 0.05. For strokes, the signed distance to the drawn centerline under both constructions (session 4's `segRibbon` and Quilez's uneven capsule for the nib) with a flag where their signs differ.
- Show outline curves: the packed quads of every region as thin lines (fill in the data colour, skin in the code colour) and the path's knots as points.

## Fixtures

| Preset | Record | What to look at |
|---|---|---|
| tldraw's draw tool | 70 pen samples along a limaçon with synthetic pressure and times; tool size 10, thinning 0.6, streamline 0.35, fit 0.9, taperEnd 22 | union at the self-crossing at 85 % alpha; switch tip; edit `fit`, `taperEnd`, `width` |
| Figma's pen tool | 12 anchors with Hermite handles on the trefoil from bench 4, closed; fill nonzero; stroke 3, miter, butt | fill rule toggle on the lobes; align inside / outside |
| a UI border | rect 24, 36, 80×52, r 6; fill; stroke 1 device px, miter, inside; snap on | zoom: one pixel stays one pixel; snap off greys the edge; rate badges go per scale / per frame |
| star (textbook) | pentagram as anchors; fill nonzero; stroke 2.5 miter | fill rule at the centre |
| self-crossing ink (harness) | the golden `translucent-self-crossing`, width 16 × pressure, polyline | the crossing painted once at 62 % |
| the crossing, as dabs (definer) | the golden `translucent-self-crossing` with `overlap: accumulate`, spacing 12, radius 8 × pressure | attack 1's hard case: 24 dabs, two on the crossing, alpha 0.62 + 0.38 × 0.62 = 0.8556; switch overlap to union for 0.62 |
| pressure ink (harness) | the golden `pressure-ink` | Position 7: switch tip; the cursor shows both distances |
| shape with a hole (harness) | the golden `holed-concave`, both rings the same direction; fill even-odd | switch to nonzero; reverse the hole's anchors in the record |
| draw your own | empty pen source; simulatePressure on | the wet route: the tail rides raw, the rest is fit as you draw; pen-up fits once; Shift-drag pans |

## Measured on 2026-09-06 (headless SwiftShader, zoom 3, DPR 1)

| Preset | value | envelope | packer |
|---|---|---|---|
| draw tool | 25 cubic, 26 knots (70 samples) | 98 pieces → 391 lines + 101 arc quads, 1.1 ms | stroke 491 curves, bands 16×16 (≈42 per band), cover 35,444 px, texels 982 + 1378 |
| pen tool | 12 cubic, 13 knots | 84 pieces → 2 loops, 504 lines | fill 24 quads; stroke 504 curves, bands 16×16 (≈39 per band) |
| border | 4 cubic + 4 line | 24 pieces → 2 loops, 76 lines (per scale) | fill 12 curves; stroke 84 curves; snapped: repack per camera move |
| harness Z | 3 lines, 4 knots | 3 pieces → 10 lines + 16 arc quads | 26 curves, bands 4×4 |
| harness pressure | 3 lines, 4 knots | 3 pieces → 10 lines + 15 arc quads | 25 curves |
| the crossing, as dabs | 3 lines, 4 knots | centerline length 281.94 → 24 dabs of 8 arc quads | 24 small packs, 46,032 device px covered in all |

## Unfinished, as notes

- The envelope flattens the centerline before tracing, so a curved stroke's skin is many short lines rather than a few offset curves: about five to eight times the curves an offset stroker would emit, and the memory line on the page (near 3 KB per stroke) assumes the stroker. Facets show at extreme zoom. Arcs (caps, round joins) are real quadratics.
- The nib's butt cap on a taper is the chord between the tangent points, not perpendicular to the centerline. PostScript's butt is the constant-width case.
- Where one end disc contains the other (|r_a − r_b| ≥ piece length) the nib's tangent points collapse; the outline is approximate there.
- The wet route re-runs the envelope and the pack for the whole stroke on every pointer move (the readout shows the milliseconds); only the fit is incremental. Two regions (dry + wet) would double-blend at their seam, which is why the bench keeps one region.
- No clip by a path (the mask), no arrow caps (needs `tangent(t)`), no sweater depth (bench 4 has the halo), no gradients, no surface-reading brush (the definer's smudge recurrence needs surface state the bench does not have).
- Dabs are separate regions and separate draws (24 packs for the fixture); a real lane would batch them into one instanced draw with painter's order preserved.
- Cover cells mode tests every cell against every curve's box on the CPU at pack time; fine here, not a design.
- Zoom range 0.05–100, not the client's 0.01–1000; the CPU twin runs in doubles while the GPU runs f32, so agreement can drift at extreme zoom.
- Even-odd with antialiasing uses a triangle wave over the sum of absolute crossing contributions; exact away from edges, an estimate on them.


## Session 10 — the compositor, the executor, the definer's two records (2026-09-06)

The pipeline column gained a fifth piece below the filler, **the compositor**, and the source stage gained **the executor** for a record's construction. The coverage code is untouched; the filler's last three lines and the surfaces around it are what was added. Verified headless in SwiftShader (the same command as above; `EXT_color_buffer_float` is available there, so the painting surface is RGBA32F).

### What a record may now declare

```
"clip":    { "source": <any source record: rect | anchors | pen | network>, "rule": "nonzero" | "evenodd" }
"group":   { "opacity": 0..1, "blend": <mode>, "isolate": bool }        // a layer when it needs isolation (opacity < 1, a blend, or isolate)
"surface": { "initial": { "clear": [r,g,b,a] } }                          // the root surface's starting content (paper)
         | { "id", "revision", "width", "height", "localToTexel": [a,b,c,d,e,f], "color": "linear-premultiplied-rgba", "initial": { "clear": [...] } }   // a painting surface at its own resolution (attack 2's declaration, verbatim)
"program": { "each": "dabs", "state": {...}, "steps": [ { "out", "op", ...bindings } ], "next": {...}, "return": [...] }     // attack 2's construction format, verbatim
         | { "definitions": {...}, "steps": [...], "return": [...] }       // without `each`: runs once and may return the value itself (the network)
paint.fill.blend, paint.stroke.blend: "over" | "multiply" | "screen" | "darken" | "lighten" | "difference"
source.kind: "network" → { vertices: { id: [x,y] }, edges: [ { id, from, to, kind: "L" | "C", c1?, c2? } ], fillSeed, onBoundary, coincidentEdges }   // no native branch: the program returns the path, or nothing does
```

**Fields nobody read are listed under the record.** The record is wrapped so every read by code below the waist is noted (a Proxy; `in` counts as a read); what was never read is printed: "fields with nowhere to go". An expression's scope is built only from the names it references, so a number the width expression does not use is unconsumed. A step whose operation the host lacks is flagged and the construction stops there; an empty drawing is not counted as the tool having run (attack 2's ask).

### The executor (`runProgram`, pure; before the GL setup)

Bindings are dotted paths resolved against the record roots (`tool`, `paint`, `source`, `surface`, `identity`), the loop item (`dab` for `each: dabs`), `state`, and earlier step outputs; a string that is not a binding is a literal word (`"nearest"`, `"source-over"`). Operations are a table of host capabilities: `sample(surface, point, filter)`, `mix(a, b, amount)`, `paint(surface, region, rgba, opacity, blend)`, `arrange(curves, coincident)`, `locate(arrangement, point, onBoundary)`, `face-boundaries(arrangement, selection)`, plus the record's own `definitions` (`map` / `as` / `emit` with `ref`, `lookup`, `key`, `default`). `each` iterates; `next` advances the state; `return` collects. It logs every step's value, unknown operations, unconsumed step fields, and the milliseconds. It moves values and keeps the order; nothing heavy happens inside it.

Two hosts run the same construction: the **GPU host** (`gpuHost`: a painting surface is two RGBA32F targets, the frozen start and the live one; `sample` is a one-texel `readPixels`, a GPU sync per dab; `paint` packs the dab in texel space and draws it through the filler into the live target) and the **CPU twin** (`cpuHost`, pure: a Float32Array, `coverageAt` per texel, the definer's reference route). The cursor compares the two texels under it. Values: a surface is a value; the CPU host copies on paint; the GPU host paints the live target in place and blits the start in first, so a program that painted twice from one old revision would differ between hosts (a values-versus-resources limit, attack 2's Position D).

### The compositor (GL section)

- **Root surface**: an RGBA8 target the size of the canvas; regions paint into it; presented to the screen by one overwrite. Cleared to `surface.initial.clear` when declared without a resolution (paper).
- **Clip**: a second region packed like any other; the filler's program takes its curves and bands on units 2–3 and multiplies its coverage in, per pixel, in the same draw. No mask texture, no extra target. Applies to every region of the record and to a presented painting surface. A group mask on a soft edge is not the same as clipping each child (not on the bench).
- **Blend**: before a region with a non-over blend is drawn, the compositor copies the target under the region's cover into a scratch texture (`copyTexSubImage2D`, unit 4) and the program mixes the source toward B(backdrop, source) by the backdrop's alpha (W3C separable modes), then paints over. Over transparent nothing, every mode is over. The panel counts the copies and pixels per frame.
- **Layer**: when the group needs isolation, its regions are drawn into a second target of the same size, which is then drawn into the root as one region (`FS_TEX`, mode 0) with the group's opacity and blend. Transient, per frame; the panel reports its bytes.
- **Painting surface**: allocated at the declared resolution; the program paints into it once per record revision; presented into the root by its `localToTexel` map, nearest (`FS_TEX`, mode 1), under the camera. Not re-run on a camera move: the panel counts presentations since the run.
- Every sampler is bound to a unit holding a texture of its own type on every draw (a dummy when unused): two sampler types on one unit is an `INVALID_OPERATION` and the draw silently does nothing. The backdrop copy is bound on its own unit for the same reason. Both were found the hard way.

### The arrangement (`arrange`, `locate`, `faceBoundaries`, pure)

Coincident edges of identical geometry merge into one piece with every source owner (the record's declared policy). Every pair of edges is intersected: lines exactly, curve pairs by flattening both at 0.02 and refining the seeds on the true curves by Newton. Each edge is cut at its parameters into pieces carrying the source interval (`AC:[.25,.5]`); vertices are named by position (an edge's own ends `V1…`, relabelled with the record's ids; discovered ones `X1…`); half-edges are sorted around each vertex by outgoing tangent; faces to the left, next = the neighbour before the twin; cycles walked; areas by 4-point Gauss–Legendre (exact for a cubic); positive cycles are bounded faces, negative ones are outer boundaries, assigned as holes to the smallest containing face when disconnected. `locate` returns the piece when the point is on an edge (the record's `onBoundary` policy) else the smallest face containing it. `face-boundaries` returns the loop (and holes) as the value's grammar with provenance on every segment. Not general: partial overlapping spans, tangencies that do not cross, tangent-order ties.

### Fixtures added

| Preset | Record | What to look at |
|---|---|---|
| the crossing, as a layer | the 24 dabs at alpha 1 in a group at opacity 0.62 | the crossing reads 0.62: three numbers at one pixel, union 0.62 / accumulate 0.8556 / layer 0.62 |
| a stroke through a mask | the draw stroke with `clip` by the trefoil | the cursor shows both coverages; toggle the clip to a rounded rect |
| dabs that multiply, on paper | the dabs with `blend: multiply` over an opaque paper root | RGB at the crossing; the copy count per frame |
| pickup brush (definer) | attack 2's record verbatim | dab 19's sample and carry on both hosts; the texel under the cursor; zoom in for texels; edit `pickup`, or read `surface.initial` |
| network faces (definer) | attack 2's record verbatim | the arrangement readout; move C to [100, 100] in the record |
| network, one curved edge (definer) | BD as the cubic | three crossings, six faces, 9600 in all; the boundary alternates L and C |

### Measured on 2026-09-06 (headless SwiftShader, zoom 3, DPR 1, canvas 1022 × 761)

| State | Readout |
|---|---|
| dabs, at (64, 64) | 2 of 24 dabs; alpha CPU 0.86 · GPU 0.85 |
| layer, at (64, 64) | alpha 0.62 both; layer target 3.0 MB, per frame |
| mask, at (92, 64) | clip coverage 1.00, stroke coverage 1.00; 24 clip curves in the same draw |
| multiply, at (64, 64) | rgba CPU (0.795, 0.342, 0.797, 1) · GPU (0.796, 0.341, 0.796, 1); 24 backdrop copies, 52,703 px per frame |
| pickup, at (64.5, 64.5) | dab 19 sampled (0.019, 0, 0.981, 1) → carries (0.010, 0, 0.990, 1) on both hosts; texel (64, 64) CPU (0.013, 0, 0.987, 1) · GPU the same; 24 texel reads = 4–26 ms of GPU round-trips in software, paints 2–6 ms; CPU twin 5–9 ms; the Node run of the same functions gives (0.013369522, 0, 0.986630440, 1), attack 2's (0.013369522403, 0, 0.986630477597, 1) to float precision |
| pickup, with `width: "size*p*p"` | dab 4 radius 4.677200212096 (attack 2's number; the emitter evaluates the width function at the dab's own interpolated pressure) |
| network, at (60, 10) | 5 vertices (1 discovered), 8 pieces, 16 half-edges, 4 faces of 2400; the value A → B → X1 → A; winding −1, alpha 0.60; arrangement 0.1 ms |
| network with C at [100, 100] | crossing (48, 48) at .48 on AC and .6 on BD; faces 2880, 3120, 2080, 1920; the seed's face still selected |
| network, curved BD, at (60, 10) | 7 vertices (3 discovered), 12 pieces, 24 half-edges, 6 faces 3800, 900, 3800, 900, 100, 100, total 9600; crossings at AC .75/.5/.25 × BD .25/.5/.75 at (90, 60), (60, 40), (30, 20); the value A → B → X3 → X2 → X1 → A, 2 cubic + 3 line, 13 quadratics at τ .01 |
| network plus a `CA-copy` edge | one geometric edge with two owners, span AC:[0,1] ↔ CA-copy:[1,0]; still 4 faces |
| a step with `op: "read"` | "unknown operation", the construction stops, nothing is painted |

### Unfinished, added

- The GPU host's `sample` is a CPU round-trip per dab; a GPU route keeps the carry in a small resource and reads the backdrop copy under the dab in the paint itself. Plain accumulate (no read) needs neither: one instanced draw, painter's order preserved by the API.
- Layers are allocated at the target's size, not the group's cover.
- A group mask (clip on the composite) versus clipping each child differ on soft edges; only the latter is on the bench. *(Session 11: both are, `group.clip` on the composite.)*
- Blend modes on the painting surface's presentation and on the layer read the whole target as backdrop, not the cover.
- The arrangement's limits above; and the `each` item's name is the plural minus its `s` (`dabs` → `dab`), a convention of the bench, not the format.

## Session 11 — a result that survives its next use (the definer's attack 3), 2026-09-06

Attack 3 brought two records and a receipt; each became a fix at full weight. The coverage code is still untouched; the executor gained a compile pass, the GPU host a pool, the record two fields (`group.clip`, `checkpoint.at`), and the filler one line about where a fragment's local position comes from. Verified headless in SwiftShader as before; the Node route (the pure declarations before the GL plumbing) still works and now reaches `runProgram`, `cpuHost`, `compileUses`, `checkpointKey` and `checkpointWhy`.

### What a record may now declare, in addition

```
"group":      { ..., "clip": { "source": <any source record>, "rule": "nonzero" | "evenodd" } }   // a mask on the completed group, applied when the group is painted into its parent; the record's root `clip` stays on each child
"checkpoint": { "at": <item index> }                                                            // after that many items of an `each` construction: save the state, resume the rest from it on both hosts, compare with the straight run
```

### Results are values with an identity (attack 3's first record)

- **The executor's compile pass** (`compileUses`): one walk over the program's text records where every binding is last read (the steps in order, `next` after them, `return` after that; a state field `next` never rebinds is alive throughout). At a `paint` step whose input surface has no later read, and no other still-read binding holds the same object, the executor passes `consume: true` in the step's context; after any step, a surface value whose last read that was is handed back to the host (`release`). The hint is the host's to take: the CPU twin copies on every paint regardless.
- **The GPU host** (`gpuHost`): a paint goes in place only when the input is consumable and not a saved result; otherwise the input is copied into another target from a pool and painted there. A saved result (the start, a checkpoint, a value still to be read) is never painted in place. A released target returns to the pool and its value can never be read again: `sample`, `paint` and `content` refuse a superseded value with an error that stops the construction, never a wrong pixel.
- **Identity**: every result carries `key` = `<surface id>:<iteration>/<step>` (the step that made it), `parent` = the key of the surface it was painted on, and `revision` = depth in the chain. Two branches one paint deep have different keys and the same depth. The panel lists every returned surface with its key and parent, and every returned colour on both hosts.
- The definer's record runs as a fixture (*two paint proofs, one chosen*): red sampled after blue is made is `(0.5, 0, 0, 0.5)` on both hosts; three targets (the start, red, blue), two copies, no paint in place. The pickup brush: one copy, 23 paints in place, two targets, 512 KB, unchanged from session 10's footprint.

### A checkpoint is a construction continuation (attack 3's receipt)

- `runProgram(program, scope, host, { until: k })` runs a prefix; `{ from: { state, at } }` continues from a state at an item. A checkpoint is taken on **each host from its own prefix**: for every surface in the state, its content (`host.content`, the bytes), map, colour, key and parent; every other state field as is (the pickup's carried colour); the next item's index.
- **What it depends on** (`checkpointKey`, pure): the program's text, the record bindings the construction reads (`runProgram` returns `reads`: `tool.pickup = 0.5`, `paint.stroke.color.3 = 0.62` for the pickup; a step output that shadows a root is not a read), the surface declaration, the item fields the program binds (`dab.xy`, `dab.path`) for the items before the checkpoint, and a capabilities string naming the bench's operation behaviour. `checkpointWhy(prior, key)` says why an earlier checkpoint cannot be reused, or null. Node receipt on the pickup: the same record → reusable; the last pen sample moved → reusable (dabs 0–11 unchanged); the first sample moved → "dab 0 differs"; `pickup` 0.6 → a binding changed; the tool renamed → reusable (nothing reads the name).
- **Resume**: `host.resolve(saved)` brings a saved surface into a host as a frozen value (the GPU uploads the bytes into its own target), so painting on it copies and the saved content stays. The bench resumes the rest on each host from its own checkpoint and compares the final surface with the straight run component by component; it also resumes the GPU from the CPU twin's checkpoint and reports that difference separately, as what determinism across clients would have to settle.
- The checkpoint lives in memory for the surface's id across record revisions; the panel says whether it was reused or why it was retaken.

### A mask on the completed group (attack 3's second record)

- `group.clip` compiles like the root clip (any source, packed as a region). It forces isolation: the children are painted into the layer under the record's own `clip`, and the layer is painted into the root as one region with the group's opacity, blend, and the mask's coverage multiplied in that draw. The composite draw's quad is the view in local units under the camera map, so the mask's curves are read in local units; the layer's texels are still read at the fragment's own pixel.
- Fixture *the crossing masked as one stroke*: at `(64.5, 64.5)` the alpha is `.31` on both hosts (`.62 × .5`); the same clip at the record's root gives `.465` on the CPU twin (`.62 × (.5 + .5 × .5)`) and `.46` on the GPU. The outline view draws the mask's curves too; the cursor shows the mask's coverage beside the clip's.

### The pixel-agreement residual, attributed and fixed

The definer measured the child-clip route at `.461` against the CPU's `.465` and did not attribute it. On this bench, before the fix: a single clipped dab on the mask's edge read `.30` on the GPU against `.31`, so the clip's coverage inside the region shader came out near `.48` where the same function in the layer composite gave exactly `.5`. Not the 8-bit targets (float targets gave the same number). The cause: the region shader took its local position from the interpolated vertex varying, and the cover triangles' corners sit at fractional device positions that the rasterizer snaps to its sub-pixel grid; the interpolated position at a fragment inherits that snap (about 0.01 local at zoom 3), which reads as a clip edge at `.48`. The layer's quad has integer corners and no such error. **Fix**: both fragment programs now derive the local position from `gl_FragCoord` through the inverse of the draw's map (`localP()`, `u_Minv`), exact per fragment. After it, the single clipped dab reads `.31` on both hosts, and the pickup surface's GPU-against-CPU difference across the whole 128 × 128 (the cross-host checkpoint number) fell from at most `5.8e-3` to at most `1.2e-7`. The tree's filler interpolates the same varying (`text/renderer.cljs`); the same fix applies there when the time comes.

### Measured on 2026-09-06 (headless SwiftShader, zoom 3, DPR 1, canvas 1022 × 761)

| State | Readout |
|---|---|
| proofs, at (8.5, 8.5) | `before` (0.5, 0, 0, 0.5), `after` (0.5, 0, 0, 0.5), `other` (0, 0, 0.5, 0.5) on both hosts; keys `proof:0/red` and `proof:0/blue`, both ← `proof:initial`, depth 1; GPU targets 3, 2 copies, 0 in place |
| pickup, at (64.5, 64.5) | texel (64, 64) CPU (0.013, 0, 0.987, 1) · GPU the same; `paint:23/surface` ← `paint:22/surface`, depth 24; GPU targets 2 (512 KB), 1 copy, 23 in place, 0 released |
| pickup with `checkpoint=12` | prefix 36 steps; saved `paint:11/surface` + carry (0.000319, 0, 0.99968, 1) + next dab 12, 256 KB; resumed over dabs 12–23, 36 steps: 0 of 65,536 components differ on the CPU twin, 0 on the GPU (each from its own checkpoint); the GPU from the CPU's checkpoint: 2,105 components differ, at most 1.19e-7 |
| groupmask, at (64.5, 64.5) | group mask coverage 0.50, 2 of 24 dabs; alpha CPU 0.31 · GPU 0.31 |
| groupmask with the clip at the root (a scratch copy) | clip coverage 0.50; alpha CPU 0.47 · GPU 0.46; at (64.5, 58), one dab: 0.31 · 0.31 (before the fix 0.30) |
| groupmask, `group=layer` (no mask) | alpha 0.62 both |
| dabs, layer, mask, multiply, network, border | unchanged from session 10's table |

### Unfinished, added

- A returned path (a network face's boundary) as a clip's source: `clip.source` is built by `buildPath`, which has no network branch; a construction's returned value needs a direct binding into the clip input (the definer's "not yet general result reuse").
- The checkpoint is in memory; a saved-and-reloaded one needs the same fields serialised (`content` as bytes) and the capabilities string checked on load.
- Release is by the executor's static last-use analysis plus an identity check against still-read bindings; a surface aliased through a definition's `emit` is not seen by it (the host's stale-read check would refuse the read rather than serve a wrong pixel).
- The carry-on-GPU route, layers at the group's cover, and blends on a layer or presented surface reading the whole target as backdrop: as before.

## Session 12 — a result used somewhere else (the definer's attack 4), 2026-09-06

Attack 4 brought three constructions, each a binding or an obligation the bench lacked; each became a fix at full weight, code before page. The coverage code is untouched again. The executor's liveness follows values into the records and arrays that hold them; a clip takes a path value or a live result; a checkpoint leaves as bytes and comes back into fresh hosts. Verified headless in SwiftShader; the Node route (the pure declarations before the GL plumbing) still works and now reaches `surfacesIn`, `captureState`, `restoreState`, `encodeCheckpoint`, `decodeCheckpoint`, `rgba32fLE`, `b64enc`/`b64dec` and `sha256`. The bench is 217 KB.

### What a record may now declare, in addition

```
"clip":          { "path": <a path value: { subpaths, meta }>, "rule": ... }            // a derived path bound as a value: frozen; nothing reconstructs it, nothing follows it
"clip":          { "path": { "result": <name>, "output": "path" }, "rule": ... }        // the live result of a construction the record holds; resolved once per edit, packed per scale
"group.clip":    the same two forms                                                        // the group's mask shares the clip's input
"constructions": { <name>: { "tool"?, "source", "program", "identity"? } }              // constructions the record holds by name: the bench's stand-in for a document's other records
"checkpoint":    { "at": <index>, "load": <the wire object> | { "store": <sha-256> } } // a saved continuation loaded from bytes into fresh hosts; no prefix is taken in its place
```

### A clip bound to a returned path (attack 4's first construction)

- `buildClip` takes one input for the record's clip and the group's mask: `source` (authored, built here), `path` with `subpaths` (a value), or `path` with `result` and `output` (a reference). A reference resolves against `state.held`, which `applyRecord` fills by running every held construction through the same `construct` as the root's own network (no host; the arrangement's vertices get the record's ids back, the face's loop is written into the path's meta). The clip carries its provenance (`from`: frozen or live; face, loop, area; the producing revision and the revision it ran at) and the panel prints it. A held network's pieces are drawn faintly like the root's.
- Rates: the producer runs once per edit (the executor, in `applyRecord`); the consumer packs per scale (`rebuild`); a camera move repacks without rerunning the arrangement. A reference to a construction the record does not hold, or to an output that is not a path, is an error printed under the source, not a silent no-clip.
- Fixture *a rect masked by a returned face*, the definer's literal record verbatim: alpha `.6` at (60.5, 10.5), `0` at (60.5, 70.5), both hosts, every field read. Fixture *the crossing through a face it holds*: 24 dabs at 62 % through the bottom face (seed (60, 70)) by reference: `.8556` at (64.5, 64.5) (the cursor's 8-bit root alpha reads .86 CPU / .85 GPU), `.62` as a union, `0` through the top face (seed (60, 10)). On a scratch copy, not landed, the pickup brush with the same held face: the presented pixel `(0.013, 0, 0.987, 1)` at the crossing and `0` at (64.5, 20), where the surface's own texel is still `(0, 0, 1, 1)`: presentation is masked, deposition is not, and the panel says which one the record asked for.

### A surface reachable only inside a returned record (attack 4's second construction)

- `surfacesIn(value)` walks arrays and plain objects (depth ≤ 8; a surface is a leaf; typed arrays are bytes). `aliveAfter` and the per-step release use it for the bindings the executor made (a step's output, the state, the item); a record root holds a surface only directly (`surface.initial`), so the record's proxies are never walked and the unread-field list stays honest. At `next`, every surface the previous state reached and the new one does not is released. A value is given back once (`free`): the executor had been releasing a value at its last read and again at `next`, which the GPU host's `newest` check tolerated silently.
- Fixture *proofs returned in a record*, verbatim: `state.proofs = [{ label: "red proof", surface: ⟨proof:0/red⟩ }]`; after the run both hosts read `proof:0/red` as `(0.5, 0, 0, 0.5)` and `proof:0/blue` as `(0, 0, 0.5, 0.5)`; 3 targets, 2 copies, 0 in place, 0 released. Before the fix (Node, a strict adapter on the CPU twin whose release makes a value unreadable): 1 released, the red read refused with the value's key, the definer's finding.
- The panel now reads every surface the returned values reach, at the first dab's centre, on both hosts, and prints a refusal where a host refuses: a later use of a returned value is part of the receipt.

### A checkpoint as bytes (attack 4's third construction)

- `captureState(state, content)` replaces every reachable surface with `{ surfaceRef: key }` and takes each key's content once; `restoreState(saved, bring)` is its inverse, one value per key. The in-memory checkpoint uses them too (`takeOn`, `bring`), so a nested state is captured and brought back whole.
- The wire (`encodeCheckpoint`): `{ format: "softland/path-checkpoint", version: 1, origin, dependencies: { at, fixedDeps, itemDeps }, continuation: { at, state }, resources: { <key>: { key, parent, revision, width, height, localToTexel, color, encoding: "rgba32f-le", byteLength, data } } }`, `data` base64. Floats are written and read one component at a time through a DataView, little-endian, so the platform's byte order never leaks into the bytes.
- The load (`decodeCheckpoint`): the format and version; `checkpointWhy(wire.dependencies, key)` against this record's own key; the continuation's index against `checkpoint.at`; each resource's encoding, its size against the declaration, its map and colour, its payload length; every reference the state makes present among the resources. Any mismatch throws its reason; the bench prints it in red, resumes nothing and takes no prefix in its place.
- `checkpoint.load` is the wire inline or `{ store: <sha-256> }` naming this page's content store (`STORE`: the sha-256 of the wire text → the text, in memory only; a reload empties it and a load then fails with that reason). The two buttons in the checkpoint row serialise the in-memory checkpoint of the CPU twin or of the GPU, store it, set `checkpoint.load` and re-apply the record: the new revision frees the old hosts and creates fresh ones, which load. `#…&save=cpu|gpu` presses a button headless.
- Measured, the pickup after 12 dabs from the CPU twin: 364,786 bytes; the saved content's sha-256 is `9757e826…`, the definer's, byte for byte; restored against the saved bytes 0 of 65,536 on both hosts; resumed 0 of 65,536 on the CPU twin and 2,105 at ≤ 1.19e-7 on the GPU against each host's own straight run (the cross-host number, as before). From the GPU: 364,780 bytes, content `502938d1…`; restored 0 on both (the float upload and read-back is exact); resumed 0 on the GPU, 2,146 at ≤ 1.19e-7 on the CPU twin. The definer's four edits, on the Node route against a stored wire: the first pressure `.65 → .4` refused, "dab 0 differs"; the last sample's x `102 → 110` loads; `pickup .5 → .25` refused, a fixed dependency changed; a changed capability in the key refused; a payload four bytes short refused by its count; the naive JSON of a Float32Array has 65,536 keys and resolves to length 0.
- On a scratch copy, the nested record with three dabs (two samples, spacing 2) and a checkpoint after one: three resources (the start, the red proof, the blue result, 12 KB) and the carried `proofs` with its reference inside; resumed 0 of 1,024 on both hosts; as bytes 19,366 bytes with a content hash per resource, restored and resumed 0 of 1,024 on both; across the three dabs the executor gave four targets back as the new state stopped reaching the old proofs.

### Measured on 2026-09-06 (headless SwiftShader, zoom 3, DPR 1, canvas 1022 × 761)

| State | Readout |
|---|---|
| facemask, at (60.5, 10.5) | clip coverage 1.00; alpha CPU 0.60 · GPU 0.60; no unread field |
| facemask, at (60.5, 70.5) | clip coverage 0.00; alpha 0.00 · 0.00 |
| facecrossing, at (64.5, 64.5) | clip coverage 1.00, 2 of 24 dabs; alpha CPU 0.86 · GPU 0.85 (the 8-bit root's .8556); held construction `face`: 4 steps, face f3, loop C → D → X1 → C, area 2400; no unread field |
| facecrossing, `overlap=union` | alpha 0.62 · 0.62 |
| pickupface (scratch), at (64.5, 64.5) | presented rgba CPU (0.013, 0, 0.987, 1) · GPU (0.012, 0, 0.988, 1); texel (64, 64) (0.013, 0, 0.987, 1) both |
| pickupface (scratch), at (64.5, 20) | clip coverage 0.00; presented rgba 0 both; texel (64, 20) (0, 0, 1, 1) both |
| nested | `state.proofs` = [{ label "red proof", surface ⟨proof:0/red⟩ }], `state.surface` = proof:0/blue ← proof:initial; read after the run: proof:0/red (0.5, 0, 0, 0.5), proof:0/blue (0, 0, 0.5, 0.5), both hosts; GPU targets 3, 2 copies, 0 in place, 0 released |
| pickup with `checkpoint=12` | unchanged from session 11 (0 of 65,536 on each host; the GPU from the CPU twin's: 2,105 at 1.19e-7) |
| pickup, `checkpoint=12&save=cpu` | 364,786 bytes, sha-256 cea99269…, content 9757e826…; restored 0 of 65,536 CPU · 0 GPU; resumed 0 of 65,536 CPU · 2,105 at 1.19e-7 GPU |
| pickup, `checkpoint=12&save=gpu` | 364,780 bytes, sha-256 30d081a1…, content 502938d1…; restored 0 · 0; resumed 2,146 at 1.19e-7 CPU · 0 GPU |
| nestedck (scratch), `checkpoint=1` | 3 surfaces saved incl. proof:0/red inside `proofs`; resumed 0 of 1,024 CPU · 0 GPU · cross 0; GPU targets 5, 6 copies, 0 in place, 4 released |
| nestedck (scratch), `checkpoint=1&save=cpu` | 19,366 bytes; 3 resources restored 0 of 1,024 each on both hosts; resumed 0 of 1,024 CPU · 0 GPU |
| proofs, groupmask, dabs, layer, mask, multiply, network, border | unchanged from session 11's table |

### Unfinished, added

- A reference across a document: the bench holds constructions inside the record; a document would name another record by identity and the executor would resolve it the same way.
- A compiler that reads reachability off a definition's parameter, map and emit; the bench walks values (conservative, depth ≤ 8).
- A content store beyond one page's memory; a reload empties the bench's, and the load then fails with that reason.
- The cursor's pixel alpha reads the 8-bit root to two decimals (.86/.85 for .8556); the texel readout is the float.
- As before: the offset stroker, the carry on the GPU, layers at the group's cover, blends on a layer reading the whole target, tangencies and partial overlapping spans.

The Node harness behind this section's numbers is landed under `node-route/` (five files and a README): extract the declarations, run the nested record on a strict host, the pickup and the proofs, and the checkpoint through bytes.
