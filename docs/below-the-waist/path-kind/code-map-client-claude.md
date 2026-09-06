# The path folder, one notch down

Production note, 2026-09-07: the account below describes the Claude baseline
merged by `dd5d34b`. The current folder map is `src/app/client/path/README.md`.
The production geometry uses `path/nib.cljc` for the centered round union;
`nib_test.clj` pins nonlinear union and containing discs, `frame_test.clj`
pins value keys, and `component_test.clj` pins clipped classification.
The baseline's curve counts and its unqualified geometry claims below are
historical receipts, not claims about the production tree. Final production
handoff: [production/HANDOFF-1.md](production/HANDOFF-1.md).
The production renderer takes `:path/value`; recipes execute in
`path/construction.cljc`, and width/recipe expressions share `engine/executor.cljc`
(`construction_test.clj`, `executor_test.clj`, browser production pixels).


2026-09-06, the Claude builder session, branch `waist/path-claude`. The folder's own map is `src/app/client/path/README.md` (one box per file). This is the notch below it: the functions inside each file, what goes in, what comes out, why that piece exists. No verdicts here; the receipts are in `HANDOFF-client-claude.md`.

## The story in plain words

1. A tool hands over a record: who it is, its numbers, what it saw (pen samples, anchors or a rectangle), what to paint.
2. The record's declarations become a short list of steps, the same way a person could have written them by hand.
3. A runner walks the steps in order. Each step names an operation and where its inputs come from. The runner notes every value it looked up.
4. The first step turns what the tool saw into a path: curves with knots that remember which sample or anchor they came from.
5. If the record snaps, the next step moves the path's knots onto the device grid, which means looking at the view.
6. A fill step wraps the path as a region with a rule. A stroke step sweeps a round nib along it into one closed skin, or drops a row of round dabs along it.
7. A clip step wraps a given path as a region that multiplies everything else.
8. What comes back is the path, the regions in paint order, and the list of what was looked up.
9. The renderer keeps that list. Next frame it looks the same things up again; if nothing changed, the run stands.
10. Each region is lowered for the camera: cubics become quadratics at a tolerance chosen for the zoom's power-of-two bucket, and the curves are sorted into row and column bands.
11. The lowered region goes into a texture atlas on the GPU. One row per cover rectangle says where the region's bands live, its colour, its group and its clip.
12. One instanced draw paints every row. The fragment finds its own local position, walks the bands, counts crossings, and turns them into coverage.
13. The same crossing arithmetic runs on the CPU, so a test can ask a pixel and a point the same question.
14. Placed ink on a 3D plane runs the same steps and the same lowering; only the vertex stage differs, because a plane is a projection.

```
record ──▶ construction ──▶ run ──▶ path + regions + reads
                                        │
                                        ▼
                             pack per region per bucket
                                        │
                                        ▼
                                atlas + instance rows ──▶ one draw
```

```
    edit kind          what reruns
    ─────────          ───────────
    colour             the row
    geometry           the run, the pack, the row
    pan / zoom inside  nothing
    zoom across bucket the pack, the row
    group moved        nothing
    group rescaled     the pack, the row
```

## The pieces

Each row: the function, what goes in, what comes out, why it exists. Arrows say who calls whom.

### `engine/expression.cljc` (182 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `compile` | a string like `size * (1 - thinning * (1 - p))` | `{:ast :names :source}` | A width rule is data only if code can run it. Reports the names it reads so a caller knows exactly which fields it consumes. |
| `evaluate` | compiled expression, scope (name → number) | a double | Runs the AST. Division by zero is the platform's; the caller decides what a non-finite width means. |
| `scope-from` | compiled expression, a record, extra names | the scope with only the read names | Fields the expression never names are unread; callers can report them. |
| `functions` | | name → [arity fn] | The vocabulary: min, max, abs, sqrt, pow, sin, cos, exp, floor, clamp, step, smoothstep, mix. |

`source/width-function` ──▶ `compile`, `scope-from`, `evaluate`.

### `engine/executor.cljc` (135 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `run` | construction `{:steps :return}`, scope (root name → map), capabilities (op → fn) | `{:ok? :values :return :log :reads :missing :ms}` | Moves values between capabilities in the declared order. Every heavy thing happens inside a capability. Stops at the first missing op or thrown error and says which. |
| `resolve-binding` | scope, a binding | `{:value :read}` or `{:value :literal? true}` | A string is a dotted path into a root (`paint.stroke.tip`, `source.samples.3`); a string whose first segment is not a root is a literal word. |
| `reread` | scope, an earlier run's `:reads` | the same paths read now | The cache key: equal means the earlier result stands. |
| `missing-capabilities` | construction, capabilities | the ops the table lacks | Ask before running. |

`component/run` ──▶ `run`. `component/rerun?` ──▶ `reread`.

### `path/value.cljc` (314 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| vector helpers (`add sub scale dot cross len dist perp lerp angle dir unit clamp sign norm-angle`) | 2D vectors | 2D vectors, numbers | Shared by the whole family; one definition of a perpendicular (left-hand, Y down). |
| `schema`, `validate!` | a path value | the value or a named exception | Subpaths of lines, quads, cubics; knots optional, one more than segments when present. |
| `line quad cubic` | points | a segment | Constructors. |
| `segment-end knot-at knot-width knot-pressure` | subpath, index | a point, a knot, a width, a pressure (0.5 when absent) | Reading a subpath without caring whether it has knots. |
| `explicit-close` | subpath | the subpath with its closing line spelled out | So reversal and joins see the closing edge. |
| `reverse-subpath` | subpath | the same curve walked backwards, knots reversed | Alignment `:outside` winds the exact path the other way. |
| `bbox stats` | path | bounds; counts of subpaths, knots, lines, quads, cubics, `:varying?` | Answers and test receipts. |
| `evaluate quad-at cubic-at` | subpath, segment, t | a point | Points on curves. |
| `quad-steps cubic-steps` | control points, tolerance | a line count | Wang's bound: how many lines keep a curve within the tolerance. |
| `flatten-subpath` | subpath, tolerance, width-of, optional width-fn | `{:closed? :points}`, each point with x y w t p seg u | The centerline as lines with source correspondence. Width between knots is the interpolation of widths, or the response applied to the interpolated pressure (the definer's fix from attack 2). |
| `flatten-path` | path, tolerance | one polyline per subpath | The answer `flatten(τ)` for consumers that need lines. |
| `polyline-length` | points | arc length | Dabs walk by arc length. |
| `map-points` | path, f | the path with every point mapped, knots kept | Snapping and projection. |

`source` ──▶ constructors. `stroke` ──▶ `flatten-subpath`, `explicit-close`, `reverse-subpath`. `pack` ──▶ `explicit-close`, `quad-at`, `quad-steps`, `lerp`.

### `path/source.cljc` (227 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `build` | source `{:kind ...}`, tool | `{:path :meta}` | Dispatch on the source's kind, never on a tool's name. Unknown kind: an empty path, `:kind :none`. |
| `build-pen` | `{:samples [[x y p? t?]]}`, tool | a path with one open subpath, knots carrying sample id, width, pressure, time | Streamline, speed, simulated pressure (tldraw's rule), the width expression per sample, tapers, fit by decimation, then Catmull-Rom cubics or a polyline. Samples are the event truth; the path is derived at a declared tolerance. |
| `build-anchors` | `{:contours [{:closed? :anchors [{:id :p :in :out}]}]}` | cubics between anchors, a line where both handles are missing | A designer's outline already is a path; this copies it. |
| `build-rect` | `{:x :y :w :h :r?}` | four lines and, with a radius, four κ arcs as cubics | A formula shape; the generator is code. |
| `arc-cubic` | centre, radius, two angles ≤ 90° apart | one cubic | The standard κ bridge. |
| `decimate` | points, ε | the subset within ε (Ramer–Douglas–Peucker) | Fit. |
| `width-function` | tool | `{:width-fn (p, s, v → width) :names :error}` | Compiles the tool's `:width` string; a malformed one falls back to `size * p` and says so. |

`component` capability `:path/source` ──▶ `build`. `component/stroke-options` ──▶ `width-function`.

### `path/stroke.cljc` (421 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `envelope` | path, stroke declaration, options (tip, tolerance, width rule, dash) | `{:path :arcs :open :closed :pieces :polylines}` | The skin: one closed outline per open piece, a ring per closed piece, consistently wound so nonzero turns the folded skin into one region. |
| `dabs` | path, options plus spacing | `{:dabs :polylines :length}`, each dab `{:x :y :r :at :s :p :seg :u :path}` | The ordered round footprints of an accumulating brush, each at its own interpolated pressure. |
| `trace-pieces` | points with radii, closed?, tip | one piece per segment with the two tangent directions | The one line where the tips differ: the nib leans by the external tangent of the two end discs, the ribbon uses the normal. |
| `trace-open` | outline, points, stroke, tip | the outline around an open centerline | Side A forward, end cap, side B backward, start cap. |
| `trace-loop`, `stroke-closed` | pieces of a closed centerline, side, direction; the exact subpath | one loop; the ring between two loops | `:center` two offsets, `:inside` the path itself and its inward offset, `:outside` the outward offset and the path reversed. |
| `join` | outline, two pieces, side, direction, stroke | the outline with the join | Inner joins fold through the pivot; outer joins are round, miter within the limit, or bevel. |
| `cap` | outline, piece, end, sides, stroke | the outline with the cap | Round through the end direction, square, or butt (the chord between tangent points, not perpendicular on a taper). |
| `arc` | outline, centre, radius, angles, optional via | the arc as ≤ 45° quadratics | Caps go the long way round through `via`. |
| `dash-polyline` | flattened polyline, on, off, phase | open pieces by arc length | Dash. |
| `disc` | centre, radius | a closed outline of four arc quads | One dab. |
| `move-to line-to quad-to add-subpath outline-path` | an outline under construction | the outline | The tracer's pen. |
| `signed-area dedupe-points with-radius tangent-point` | points | numbers, points | Helpers the tracer reads. |

`component` capabilities `:path/envelope`, `:path/dabs` ──▶ `envelope`, `dabs`.

### `path/pack.cljc` (380 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `scale-bucket bucket-scale bucket-tolerance bucket-margin` | device pixels per local unit; a bucket | the power-of-two bucket; its top scale; a quarter device pixel there in local units; one device pixel at its coarsest | The camera enters here and nowhere above. Quality is set at the bucket's top so it never drops inside it. |
| `cubic->quads`, `split-cubic` | cubic control points, tolerance | quads as `[x1 y1 cx cy x3 y3]` | The midpoint quad errs by at most (√3/36)·‖b − 3c₂ + 3c₁ − a‖, shrinking by n³. |
| `lower` | path, tolerance | `{:quads :cubics :from-cubics}` | Every segment as quadratics; lines get their control at the midpoint. |
| `pack-quads` | quads, options | the pack: quads, boxes, bbox, band counts and lists, `:band-xf`, `:band-texels` | ceil(√(n/2)) rows and columns; each curve in every band its box touches; row lists by max x descending, column lists by max y descending. That order is what lets the shader stop early. |
| `pack-region` | region outline, tolerance, options | `{:pack :cubics :from-cubics}` | `lower` then `pack-quads`. |
| `root-code solve-axis` | three relative coordinates; relative control points | crossing classes; the two crossing positions | The Slug lookup and solver, the same arithmetic as the WGSL. |
| `winding-at inside?` | pack, point (and rule) | integer winding; membership | The one definition every reader shares. |
| `coverage-at` | pack, point, pixels per unit on each axis, rule | the filler's estimate in [0, 1] | The CPU twin: the same root code, solver and combine, so a GPU pixel can be compared to it. |
| `cover` | pack, `{:mode :box|:cells :margin :cell :rule}` | `{:rects :tested :area}` | What the filler draws. Cells keep the ones a curve passes through plus interior ones by winding at the centre, so a thin diagonal does not pay for its box. |
| `snap-path` | path, local→device, device→local | the path with knots on the device grid, handles carried | A policy at frame rate. |
| `outline-distance` | pack, point | distance to the nearest curve | Boundary slop; not per pixel. |

`component` ──▶ `pack-region`, `inside?`, `outline-distance`, `snap-path`. `renderer` ──▶ `scale-bucket`, `bucket-tolerance`, `bucket-margin`, `pack-region`, `cover`. `region3d/on-plane` ──▶ the same. `harness` ──▶ `coverage-at`, `inside?`.

### `path/component.cljc` (353 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `schema`, `validate-component!` | a record | the record or a named exception | Structural acceptance; whether a construction runs is the executor's report. |
| `capabilities` | | op → fn | `:path/source :path/snap :path/fill-region :path/envelope :path/dabs :path/clip-region`. Every op a pure derivation. |
| `default-construction` | record | `{:steps :return}` | The construction the declarations imply, as data: source; snap when declared (reads `view.scale`, `view.pan`); a fill region; the envelope as a union or the dabs as an accumulation (reads `view.scale` only for a device-unit width); a clip region. Colours are bound nowhere. |
| `construction` | record | its own construction or the default | A record may carry its own. |
| `scope` | record, view | the executor's roots | `tool` without its name, `source`, `paint` with the stroke's geometry fields under `paint.stroke.geometry`, `identity`, `view`. |
| `run` | record, view | the executor's result with `:path :meta :regions :clip` lifted out | Regions flat, in paint order. |
| `rerun?` | record, view, an earlier `:reads` | true when a read value changed | The cache test. |
| `stroke-defaults stroke-options` | stroke declaration, tool, scale | filled defaults; the tracer's options (tip, tolerance, width rule in local units, dash, spacing) | Device-unit widths divide by the scale here. |
| `classify classify-regions hit? boundary-distance painted-regions region-pack` | record, point, slop, view | `:inside | :boundary | :outside`; a boolean; a distance; regions with packs | CPU answers from the same regions the filler paints. |
| `region-color` | record, region | straight RGBA | The fill's colour for a fill region, the stroke's for a stroke or dab. |
| `canonical-component component-content-hash` | record | a stable content key without identity and revision | The harness's revision. |

`renderer` ──▶ `run`, `rerun?`, `region-color`. `region3d/on-plane` ──▶ `run`, `region-color`. `harness` ──▶ `validate-component!`, `run`, `painted-regions`, `classify-regions`, `component-content-hash`.

### `path/frame.cljc` (79 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `item-view` | view, a group's world transform | `{:scale :pan}` in device pixels | The view a construction sees; a screen-space group ignores the camera. |
| `item-key` | draw item, view, world transforms | `[id revision container buffer-index bucket scale-or-nil pan-or-nil]` | Per item: the scale only for a device-unit width, the pan only for a snapped record; a group's translation is not here (the group buffer moves the picture), its scale is (the bucket). |
| `frame-key` | draw items, view, world transforms | one item key per item | The frame's early-out. |
| `region-key pack-key` | region; region and bucket | `[hash-of-outline rule]`; that plus the bucket | What a pack is cached under. |
| `device-width? group-scale` | record; world transform | booleans, numbers | Helpers. |

`renderer` ──▶ all of them.

### `path/records.cljc` (96 lines)

The definer's records as data, read by the tests and the harness alike so a record means one thing in both: `harness-z`, `z-as-dabs`, `z-nonlinear`, `pressure-ink`, `holed-concave`, `border`, `draw-tool`, `pen-tool`.

### `path/renderer.cljs` (328 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `region-vertex-shader` | | WGSL | Six vertices per instance span the cover rectangle through the group affine and the camera; hands the fragment the inverse placement, flat. |
| `region-fragment-shader` | | WGSL | The pixel centre through the inverse placement gives the local position; its derivative gives pixels per unit; the shared program gives coverage times the clip's. |
| `init-path-system` | device, format, camera and group buffers, options | the system | Pipeline, atlas, instance pool, bind group, four caches. |
| `prepare-path-frame!` | system, draw items, view, world transforms | `{:changed? :runs :packs :instances :instance-writes :atlas :cell-covers :items}` | Pass 1 runs and packs through the caches; the atlas keeps this frame's packs and takes the new ones; rows are written through the pool; the bind group is rebuilt only after a regrow. |
| `cover-options` | pack, rule, bucket, scale | `{:mode :margin :cell :rule}` | Box, or cells when the box is large in device pixels and sparse in curves. |
| `draw-path-frame! draw-path-instances!` | open pass, system (and a range) | one instanced draw | Painter's order is the row order. The caller owns the pass. |
| `item-range prepared-rows` | system, index | `[first count]`; the rows | A caller can draw one item between other passes (the region harness's surround). |
| `destroy-path-system!` | system | nil | Atlas, instance buffer, caches. |

`harness/path`, `harness/region` ──▶ `init-path-system`, `prepare-path-frame!`, `draw-*`, `item-range`, `destroy-path-system!`.

### `engine/coverage.cljs` (453 lines)

| Piece | In | Out | Why |
|---|---|---|---|
| `coverage-wgsl` | | WGSL | The program text and paths share: root codes, the two axis solvers, band lookup wrapping at a power-of-two width the caller names, the combine; even-odd as parity of the same crossings; `region_alpha` with the clip. Bindings 0 and 1 are the curve and band textures. |
| `instance-words instance-stride instance-attributes instance-input-wgsl pack-instance` | an instance description `{:rect :slot :color :rule :index :clip}` | 112 bytes; the vertex layout | The region instance row. |
| `create-atlas` | device, options | an atlas with two planes (rgba32float curves, rg32uint bands, 1024 wide), CPU mirrors, slots | Paths pack into it at edit time. |
| `insert!` | atlas, key, pack | the slot | Writes the pack into both mirrors at the fill points when new. |
| `retain!` | atlas, keys still in use | count dropped | Dropped slots become garbage; past half the fill, the live packs are rewritten compactly. |
| `flush!` | atlas | `{:curve-rows :band-rows :regrown?}` | Uploads the dirty rows once per plane. A regrow replaces the views. |
| `views stats destroy-atlas! bind-group-layout-entries` | atlas | views; counts; nil; the two texture entries | Owners rebuild bind groups after a regrow. |

`text/renderer` ──▶ `coverage-wgsl`. `path/renderer`, `region3d/on-plane-renderer` ──▶ everything.

### Where the state is

| Owner | State | Lifetime |
|---|---|---|
| `path/renderer` system | `!runs`, `!packs`, `!prepared`, `!last-frame-key`, `!bind-group`, the atlas's `!state`, the pool's buffer | init to destroy |
| `region3d/on-plane-renderer` region row | `pack-cache`, `pack-key`, `placements`, an atlas, a pool, a matrix buffer | the region's GPU lifetime |
| everything in cljc | none | |
