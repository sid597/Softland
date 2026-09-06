# Handoff from the Claude builder session — the path kind's first client change

Written 2026-09-06 on branch `waist/path-claude` in the worktree `../Softland-claude`, builder chair, from `STARTER-client.md`. Read the starter first, then the definer's `from-12-to-client.md` (the change this session made and the five scenarios it is measured by), then this file, then the code: `src/app/client/path/README.md` is the map, and every namespace's docstring says what it takes and returns. The receipts are in `receipts-client-claude.json` beside this file, the full result of the browser harness for the path and region3d lanes, dumped from the verifier's page on this tree. The fence is unchanged: code only under `src/app/client/`, never `src/app/server/env.clj`; commits with exact paths on this branch; push is Sid's.

## 1. What landed

Three commits. The old route (`path/tessellation.cljc`: ink fans, hole bridges, ear clipping, the mesh cache, the zoom table; the flat triangle shaders and their seven-word vertex; the `:ink`/`:shape` grammar) is gone from every client caller. In its place, the path kind as the page drew it: a record runs through a construction into a path value and regions, the regions are packed for the camera, and one shared filler paints them.

**a0c8102, the pure layer** (cljc, runs on the JVM and in the browser, no camera above the packer):

| Namespace | What it is |
|---|---|
| `engine/expression.cljc` | A tool's arithmetic as a string (`size * p^2`) compiled to an AST that reports the names it reads. `+ - * / ^`, a fixed function set, `pi`. |
| `engine/executor.cljc` | Runs a construction (`{:steps [{:out :op ...bindings}] :return ...}`) over a capability table in step order. Bindings are dotted paths into named roots. Returns the values, the resolved return, a log, every root path read with its value, and the ops the table lacked. Holds nothing. |
| `path/value.cljc` | The value: subpaths of lines, quads and cubics; knots with id, width, pressure, time. Closing, reversal, bounds, evaluation, flattening with source correspondence (`:seg :u`), and the width between knots as either the interpolation of widths or the response applied to the interpolated pressure. |
| `path/source.cljc` | Pen (streamline, speed, simulated pressure, the width expression per sample, tapers, fit by decimation, Catmull-Rom), anchors (copying), rect (four lines and four κ arcs). Each gives `{:path :meta}`. |
| `path/stroke.cljc` | The swept round nib or the ribbon along the flattened centerline into one consistently wound skin per open piece (or a ring per closed piece under `:align`), with caps, joins, miter limit and dash; and the ordered dabs of an accumulating brush, each a disc of four arc quads at its own interpolated pressure. |
| `path/pack.cljc` | Cubics to quads at the bucket's tolerance (a quarter device pixel at the bucket's top scale), row and column bands sorted for the shader's early exit, the box or cells cover, snapping, distance to an outline, and the CPU twin: winding and coverage with the filler's own root code, solver and combine. |
| `path/component.cljc` | The record's schema, the capability table (`:path/source :path/snap :path/fill-region :path/envelope :path/dabs :path/clip-region`), the default construction as data derived from what the record declares, the scope (`tool` without its name, `source`, `paint` with the stroke's geometry fields under `paint.stroke.geometry`, `identity`, `view`), `run`, `rerun?`, and the CPU answers (classify, hit, boundary distance). |
| `path/frame.cljc` | The keys per level: the item's view, the item key, the frame key, the pack key. |
| `path/records.cljc` | The definer's records as data, read by the tests and the harness alike: the Z, the Z as dabs, the Z with 16 p², the pressure ink, the holed concave, the border, the draw tool, the pen tool. |

Tests carry the definer's numbers: the Z as 3 pieces → 10 lines + 16 arc quads in one closed outline; 26 curves in 4 × 4 bands with the lists sorted the way the shader expects; 24 dabs of 8 arc quads on 281.94 of centerline, two of them covering the crossing; the dab's width 16 p² at its own pressure, more than 0.1 from the interpolation of widths; the holed shape a hole by parity (winding 2 there); the border's one device pixel snapped by reading the view; the nib's side point the external tangent of the two end discs, the ribbon's the normal; a miter past its limit a bevel; five dashes over 100 at 10 on, 10 off. 43 tests, 360 assertions, green on this tree.

**b7146c7, the browser lanes**:

| Namespace | What it is |
|---|---|
| `engine/coverage.cljs` | The per-pixel program extracted from text as one WGSL string (root codes, the two axis solvers, band lookup wrapping at a power-of-two width the caller names, the combine; even-odd as the parity of the same crossings), `region_alpha` (a region's coverage times its clip's), the 112-byte region instance row, and a dynamic atlas: an rgba32float curve plane and an rg32uint band plane, each 1024 wide, with CPU mirrors, dirty-row uploads, doubling regrow, `retain!` with compaction when garbage passes half the fill. |
| `text/renderer.cljs` | Composes its fragment from the shared string, passing em units, its offline font atlas (bands 4096 wide, so log2 width 12) and the nonzero rule. All nine text goldens byte-identical: the receipt that the extraction is exact. Layout and glyph packing untouched. |
| `path/renderer.cljs` | The system: run cache (the construction's result with what it read), pack cache per region and bucket, the atlas, the instance pool, the pipeline. A frame reruns a record only when a read value changed, packs a region only when its content or the bucket changed, writes a row only when it changed, draws once instanced with six vertices per cover rectangle. The fragment takes its local position from its own pixel centre through the inverse placement handed flat from the vertex stage. |
| `region3d/on_plane.cljc`, `on_plane_renderer.cljs` | Placed ink runs the same construction at the unit view and packs for bucket 0 with a box cover; the region renderer keeps an atlas, a pool and a storage buffer of placement matrices per region, orders resolved ink back to front, draws once instanced under the plane's view projection with the old depth contract (tested, not written, the surface bias). The fragment reads its local position as a perspective-correct varying, not through an affine inverse, because the placement is a projection. |
| `harness/path.cljs`, `harness/region.cljs` | The five scenarios, the goldens, colour, parity; the region harness's ink and polygon records through the same route. |

**This session's commit**: the frame key carries the group transforms. Reading the renderer against `frame.cljc` showed the early-out key held the view but not the world transforms, so a group's scale change (a translation is the group buffer's) would have passed the early-out untouched: a stale bucket across a boundary, a stale run for a device-unit width or a snapped record in that group. Now `frame/item-key` carries, per item, the group's buffer index, the item's own scale bucket, and its exact scale or pan only when the record's declarations read them; `frame/item-view` moved out of the renderer into the pure file. A test covers moved versus rescaled and a screen-space group; the harness gained the rescaled-group rate row.

## 2. The receipts (from `receipts-client-claude.json`, SwiftShader in headless Chrome, both this tree and b7146c7)

The verifier passes all six guards (base renderer, Ubuntu slug, image, path, region3d, text tree); every representative golden matches; the path and region3d goldens and the shader digests were re-recorded at b7146c7 with `RENDER_VERIFIER_RECORD_PATH_ROUTE=1` and are unchanged since.

**Scenario 1, three records, one route.** Draw (ops source, envelope), pen (source, fill-region, envelope), border (source, snap, fill-region, envelope). For each: a geometry edit changes the picture, a behaviour edit changes the picture, renaming the tool changes nothing and reruns nothing (`rename-runs 0`). The reads name only `source`, `tool`, `paint.fill.rule`, `paint.stroke.geometry`, and for the border `view.scale` and `view.pan`; no read contains `name`.

**Scenario 2, pressure and crossing.** At the crossing pixel (64, 64):

| Reading | Value | Expected |
|---|---|---|
| Union alpha | 0.6196 | 0.62 (0.62 × 255 = 158.1, the byte is 158) |
| 24 dabs alpha | 0.8549 | 0.8556 (218.2 → 218) |
| Dab count, instances | 24, 24 | 24 |
| Tip | `:nib`, named in the record | |
| GPU vs CPU twin along the union's edge, 200 pixels | max delta 0.0020 | < 0.05 |
| 16 p²: every dab's radius 8 p² at its own pressure | holds | |
| Widest gap on the first segment between 8 p² and the interpolation | 0.124 local | sub-pixel on the Z |

The nonlinear receipt is the rule checked on every dab and the dab painted at its own radius (centre alpha 0.8549); on this Z the difference from interpolating widths is a fraction of a pixel, so it is not a pixel that turns.

**Scenario 3, one region meaning.** Holed concave under even-odd: interior 0.9608 (the fill's 0.96), hole 0, notch 0; under nonzero the hole fills (0.9608). Edge pixel (24, 60) on the shape shifted half a pixel: CPU twin 0.48, GPU 0.4784. The Z's returned skin as the shape's clip: a probe inside the Z reads the shape's alpha, three probes outside read 0. Text runs the extracted filler.

**Scenario 4, rates.** What `prepare-path-frame!` reports:

| Frame | changed? | runs | packs | row writes |
|---|---|---|---|---|
| first | yes | 1 | 1 | 1 |
| repeat | no | 0 | 0 | 0 |
| colour edit | yes | 0 | 0 | 1 |
| geometry edit | yes | 1 | 1 | 1 |
| restore the original | yes | 1 | 1 | 1 |
| pan | no | 0 | 0 | 0 |
| zoom 1.9 (inside bucket 0) | no | 0 | 0 | 0 |
| zoom 2.5 (bucket 1) | yes | 0 | 0 | 1 |
| pen tool first | yes | 1 | 2 | 2 |
| pen tool zoom 2.5 (bucket 1) | yes | 0 | 1 | 2 |
| border first | yes | 1 | 2 | 2 |
| border pan | yes | 1 | 2 | 2 |
| border zoom 1.5 | yes | 1 | 2 | 2 |
| Z in group 17, first | yes | 1 | 1 | 2 |
| group 17 moved | no | 0 | 0 | 0 |
| group 17 rescaled half → double | yes | 0 | 0 | 1 |

A fractional placement (group 18 at 0.5, 0.25): 300 edge pixels within 0.0020 of the CPU twin, the old varying-position error absent. The Z's rows across a bucket without a repack, and the pen tool's one repack, are the pack fix of the measurements round: a region without cubics packs once for every bucket (`measurements-client-claude.md`).

**Parity.** Seven zooms (0.01, 0.1, 1, 8, 10, 100, 1000), 15,456 decisive pixels each (5,040 inside, 10,416 outside), zero mismatches.

**Colour.** Legacy direct and linear premultiplied within one byte of the expected blend.

**Scenario 5, the projected caller.** The region3d floor cases (sandwich, lit-depth-shadow, tree, worn) byte-identical across two captures; the tree's placed ink resolves as one placement of 26 curves (the harness key still says `ink-vertices`; it counts the packed curves now); the placement system reports 1 pack, 1 upload, 17 draws, 0 over limit; the depth oracle at (64, 64) reads the near object at t 6.9 within one byte. Leases retire to zero after close.

## 3. Judgment calls

- **Behaviour is data twice.** A construction is data (steps over capabilities), and a width rule is data (a string over named numbers). A tool's name is not in the scope at all; the source's kind names the capability that builds the path. A record may carry its own construction; the default one is derived from its declarations and could have been written by hand.
- **Caches by observation, not by declaration.** `rerun?` compares the values the executor reported reading against a fresh read of the same paths. A construction that never reads `view` is camera-free because it did not read it. Colour is never bound into a construction (rows carry it), so a colour edit reruns nothing; the revision only decides whether the frame's early-out is skipped.
- **The run cache holds one result per material id.** Restoring an edited record reruns (the "restore" row). Bounded memory was the reason; an LRU keyed by revision is the change if undo thrash matters.
- **The camera enters at the packer and nowhere above.** Tolerance and cover margin come from the power-of-two bucket of device pixels per local unit; a zoom inside the bucket is a camera move. A device-unit width and snapping are the two declarations that make a run read the view, and they rerun per scale or per pan by design (the border rows).
- **The fragment's position.** In the 2D lane the vertex stage hands the inverse placement flat and the fragment maps its own pixel centre back to local units, so a fractional placement does not inherit the rasterizer's snap of an interpolated corner (session 11's finding). Under a projection that inverse is not affine, so the on-plane fragment uses a perspective-correct varying instead.
- **One filler, three atlases.** Text keeps its offline font atlas; the path system owns one dynamic atlas; each region3d region owns its own. The program is one string with the band width as a parameter. Sharing the dynamic atlas across systems was not needed for the receipts and would couple their lifetimes.
- **Dabs are one capability, not an executor loop.** `:path/dabs` returns the ordered footprint regions; the compositor paints them in order. The bench's per-brush-step row (the executor iterating with carried state, a brush reading the surface) is not here.
- **Reactive: none inside the kind.** The data flow is pull. The frame caller decides when a frame runs and calls `prepare-path-frame!`; the four levels of cache decide how much of it runs. Missionary belongs, if anywhere, at the edge that decides when a frame runs and where records arrive, not below it. The state a system holds is five atoms with one owner each (runs, packs, prepared, last frame key, the atlas) plus the pool and the bind group; the pure files hold nothing.
- **No compositor change.** The renderer draws into the caller's open pass with the engine's scene-colour blend. Clips are a region multiplied in the fragment. Layers, blends and a surface a brush reads are Position 9's compositor, a separate step.
- **The expression evaluator is a parser.** A human types `size * (1 - thinning * (1 - p))`; the string is the record's form. An AST-as-data alternative would be a second form for the same thing.
- **Commits on this branch.** The two earlier commits carry a `Co-Authored-By` trailer the tool's template suggests; this session's does not, following Sid's rule. Sid decides whether to strip them.

## 4. What stays on the list

| Work | Where it lands | Note |
|---|---|---|
| The offset stroker | `path/stroke.cljc` | The envelope flattens the centerline and traces the hull with arc joins, so a curved stroke is many short lines plus arcs. A true offset of the cubics keeps the curve count near the source's. The nib's meaning (the external tangent of the end discs) must survive the change; the tests pin it. |
| Rows cached per item, rebuilt only for changed items | `path/renderer.cljs` | The trace: a one-record colour edit costs 35 ms at 1,600 items with no run and no pack, about 22 µs per unchanged item. The pull model's price; a push edge removes it. `measurements-client-claude.md`. |
| View-reading records rerun per pan | `path/component.cljc`, `path/renderer.cljs` | 266 snapped borders cost 79 ms per pan. Visible only, or snapping and the device-unit width in the placement. |
| Cubic regions packed for a bucket range, repacks spread over frames | `path/renderer.cljs`, `path/pack.cljc` | The remaining bucket-crossing hitch, 184 ms at 1,600 items, after cubic-free regions stopped repacking. |
| Cell cover thresholds | `path/renderer.cljs` `cover-options` | Cells when the box exceeds 256² device pixels and 2048 px per curve, twelve cells across the longer side. Guesses; measure on a long thin stroke at high zoom. |
| Layers, blends, the surface a brush reads | `engine/compositor.cljs` | Paint and sample as the two operations; the path renderer must not grow a private surface owner. |
| The executor's each-loop | `engine/executor.cljc` | A step that iterates with carried state (the bench's per-brush-step row, the pickup). The executor stops at the first missing op; a partial run is not counted as the tool having run. |
| Precision rebase | `path/pack.cljc`, `engine/coverage.cljs` | Quads sit in the atlas in local units as f32. A path far from its group's origin loses precision; the fix is quads relative to the pack's box origin with the origin in the row. |
| The run cache per revision | `path/renderer.cljs` | See the judgment call above. |
| Naming | `harness/region.cljs` | `ink-vertices` counts curves. `path/component` keeps the old name and the `:path/material-id` keys so callers did not move. |
| Foreign red, untouched | `test/app/client/region3d/on_plane_test` | The placed-text case errors on a missing shaped provider before and after this change. |
| Tangencies, partial overlaps, arrangement | geometry | Not started; the definer's list. |
| References beyond the record, checkpoints, multiplayer truth | callers, codec | Not started; the definer's list. |

## 5. How to run it

```
# JVM: the eight pure namespaces, the definer's records as tests, about a minute
clj -M:test -i test/app/client/path/run_pure.clj

# browser, from this worktree (the :dev alias needs env.clj, absent here; node_modules is a symlink to the main checkout's)
clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier
node test/render_engine/run_verifier.mjs                 # pass/fail, six guards, golden hashes
node test/render_engine/dump_result.mjs <out-dir>        # every scenario number as JSON and every picture as PNG
```

The verifier's `receipt.json` keeps pass/fail and golden hashes only; `dump_result.mjs` writes the page's full result for the path and region3d lanes, which is what `receipts-client-claude.json` is. `STARTER-judge.md` beside this file carries a REPL session over the pure layer for change-it moves by hand.

## 6. Measurements

`measurements-client-claude.md`: the JVM per-record costs, the browser trace at three scene sizes on the real adapter (AMD RDNA 3) and on SwiftShader, and the reading: the GPU has room, geometry per edit is a millisecond, the CPU frame loop is the cost in three named places. The trace is a harness step with no pass; `test/app/client/path/timing.clj` and `RENDER_VERIFIER_HARDWARE=1 node test/render_engine/dump_result.mjs <out>` reproduce it.

## 7. Next

The definer's constructions against the landed code as they arrive, in this directory. A construction that needs an input, result or state these interfaces cannot carry reopens the picture; a tangent, a curve count or a missing namespace is work inside it.
