# Path production slice 1 — caller construction and explicit geometry

2026-09-07. Main checkout, `main`; push remains Sid's. The broader client pure
suite remains **red** on the pre-existing placed-text error
`:text/layout-provider-required`: 106 tests, 858 passing assertions, no failed
assertions, one error. [Current receipt](receipts/client-pure.log) and the
[untouched Claude source replay](receipts/baseline-placed-text.log) name the
same test and exception. No placed-text source or test was changed to conceal it.

The **repo browser verifier is green without recording enabled**: all six
runtime guards and every checked golden pass ([log](receipts/verifier-final.log),
[detailed receipt](receipts/verifier-final.json)). The
[pre-record verifier](receipts/verifier-before-record.log) preserves the two
red image comparisons before their intentional recording; the reason is below. The focused path/placement suite is green:
47 tests, 399 assertions ([receipt](receipts/path-pure.log)); the browser build
has zero warnings ([receipt](receipts/build.log)).

## Landed tree and ownership

| Commit | Coherent change |
|---|---|
| `dd5d34b` | Merge commit with parents `d7c8939` and `e8cb946`. Claude's judged `4d9e0d6` remains in history. Main's newer findings file won the conflict. |
| `f1a7200` | Round capsule union and radius subdivision; clipped CPU membership; region keys carry values; semantic tests replace tracer counts. |
| `2391644` | Caller construction and path-value renderer boundary; explicit scale/fractional pan; one expression language; caller and pixel tests; documentation hierarchy. |
| Final documentation/goldens commit | This handoff, durable receipts and intentional golden recording. Its hash is the commit containing this file. |

The Codex worktree at `07ea014` was read-only source. No worktree was created,
no push was made, and `src/app/server/env.clj` was never read. Concurrent
documentation commits and foreign dirty files were preserved. Git history
and the final status receipt establish source custody; these are workspace
actions, not product-test claims.

## The waist and the caller

The current [folder map](../../../../src/app/client/path/README.md) is the
code-facing entry. An authored record has source, tool numbers, paint and
optionally a recipe. `construction/construct` executes it into a validated
component with `:path/value`, paint, numeric parameters, identity/revision and
snap declaration. The caller keeps the authored record beside that value.
Source/recipe edits execute construction; independent paint edits can update
the returned component directly. If a recipe itself reads paint, its caller
must execute it when those inputs change. The harness schedules these calls
explicitly; the automatic push edge is not implemented.
Evidence: `construction_test`, browser `path_production/recipe-check!`, and
the separate source-edit timing in the browser trace.

The value vocabulary remains the bench's `:kind :line/:quad/:cubic`,
`:p/:c/:c1/:c2`, `:closed?` and `:knots`. There is no alternate Codex segment
adapter. `component/geometry-inputs` returns the complete ordinary input
value: path, geometric paint declarations, parameters, clip, snap flag and
declared view fields. `component/geometry` is pure. The renderer holds no
source program, observed-read report or construction-run cache.
Evidence: `value_test`, `component_test`, `frame_test`, and captures that
make `executor/execute` throw if renderer preparation enters it.

The shared EDN language supports arithmetic, required `:get`, quoted
`:literal`, lazy `:if`, structured return values, capability calls and a
bounded-by-input `:each` loop carrying bindings. Missing bindings/operators
throw before later calls. Width rules compile once per geometry/source
evaluation using that same compiler. There is no string-language fallback
or clock in executor results. `executor_test` exercises the language and
loop; the browser's returned-mask pixels exercise arithmetic through a
recipe into clipping. No pickup brush or compositor was implemented.

## Counterexamples and the hard view case

Named path tests are under [test/app/client/path](../../../../test/app/client/path/).
The browser checks are in [path_production.cljs](../../../../src/app/client/harness/path_production.cljs)
and [path.cljs](../../../../src/app/client/harness/path.cljs).
The final [path dump](receipts/path-step.json) and [plane dump](receipts/region3d-floor.json)
both pass; all seven parity stations report zero mismatches.

| Finding | Regression evidence and observed answer |
|---|---|
| A1, nonlinear width on a line | `nib_test/nonlinear-union-resolves-the-radius-between-line-endpoints`; browser nonlinear union: alpha 0 at `(30,35)`, 0.6196 at `(40,30)` for radius `10p²`. |
| A2, containing discs | `nib_test/a-containing-disc-is-kept-whole` covers coincident centers in either radius order. Browser reads 0.6196 at `(19,50)` inside the larger disc. The original judge declaration needs the correction below. |
| A3, hash collision | `frame_test/different-regions-never-alias-through-a-hash` forces the same hash and requires distinct full region keys. The renderer and placed-ink atlas receive values as keys. |
| A4, ignored CPU clip | `component_test/classification-intersects-the-returned-clip` checks three original inside points rejected by the clip. Browser returned/empty clip pixels and `region3d/path_placement_test` exercise both callers. |
| A5, stale recipe | `construction_test/a-recipe-edit-changes-the-value-the-retained-renderer-will-see`; browser original/retained-edited/fresh-edited: 1/24/24 regions, crossing alpha 0.6196/0.8549/0.8549. |

A2 is a correction to the receipt, not a changed default. The literal judge
record produces widths `[4.4 8.0]`; its claimed radii require explicitly
declaring `size=16, width=size*p`, which produces `[1.6 16.0]`. The baseline
already contains `(19,50)` under that declaration. It loses the later larger
disc at coincident centers; production retains it. The same
[`probe_a2.clj`](../../../../test/app/client/path/probe_a2.clj) produced
[baseline](receipts/a2-baseline.edn) and [production](receipts/a2-production.edn)
values. This is the counterexample the A2 regression actually repairs.

The border's one device pixel is an input declaration: width divides by
projected scale, and scale is in the geometry input. Snapping adds the
fractional device pan, including group translation. Writing `p=n+f` gives
`round(x*s+p)-p = round(x*s+f)-f`; integer pans move the GPU picture without
changing geometry. Screen groups ignore the world camera in both geometry
inputs and shader placement. `frame_test` pins group scale and fractions.

The browser isolates the stroke by removing the fill and reads adjacent
pixels. At zooms 0.01, 0.1, 1, 8, 10, 100 and 1000 the alpha triplet is
`[0,1,0]`. These are screen-constant fixtures: path coordinates divide by
projected scale, while the declared device width stays 1. Fractional pan
produces a new geometry value; an added integer shift reuses it with zero
geometry evaluations, packs or row writes. A screen-group world zoom/pan
also produces zero work and the same stroke pixels. These statements are
proved for positive uniform scale and translation. Correct snapping under
rotation/shear, one device pixel under nonuniform scale, and perspective
plane device widths are untested; the plane caller retains its unit-scale
convention. Pixel evidence: `path_production/border-check!` in the dump.

The taper is a picture and a pixel comparison: `(119,36)` reads 0 with the
draw record's taper and 0.851 without it; 97 pixels lose more than 0.5 alpha.
`path_production/taper-check!` asserts that fixed point and exports both PNGs.

## Departures, their support, and where they stop

| Departure | Support | Limit and what changes with it |
|---|---|---|
| Capsule loops replace the base's round outline. Centerline control-hull error and sampled radius error drive subdivision. | A1/A2 tests and browser pixels; one nonzero union still paints the crossing once. | Radius checks estimate general formulas; arbitrary oscillation between samples is untested. More capsule curves increase geometry/packing cost. Local tolerance does not promise extreme-zoom silhouette accuracy. |
| Base nonround caps/joins, aligned rings, ribbon and dashes remain policy constructions. The Codex capsule piece did not supply these. | `stroke_test` tests butt/square caps, open miter limits, ribbon, dash and alignment; source tests retain fit, pressure and tapers. | The policy tracer remains for those policies. It shares radius subdivision, but arbitrary self-intersecting offset behavior is untested. The offset-stroker question is still open. |
| Recipes moved to a new caller namespace; results may include paint overrides. | A5 requires the returned overlap declaration to affect subsequent rendering; custom source and arithmetic-mask tests demonstrate reusable returned values. | A source edit must refresh the caller's value. A recipe that reads paint must be run on that paint edit. The renderer cannot infer this responsibility. |
| Geometry receives explicit view declarations and parameter values; no observed reads choose reuse. | Border/snapping pixels, full-input key tests, and the returned-width-parameter test. | Scalar scale/translation limits above apply. Geometry `s` remains normalized segment parameter and `v=0`; source `s` is sample arc fraction and `v` sample speed. Their equivalence is untested. |
| EDN expressions replace both base languages; errors are explicit, and executor values have no clock. | `executor_test`, `source_test`, custom recipe and nonlinear pixels. | Existing stored string formulas/old recipes require migration; only repository fixtures/callers were migrated. No persisted-record migration is claimed. |
| Existing geometry reuse, pack buckets and atlas remain; full values replace hash/revision/read proxies. Bucket cover selection uses the bucket's lower scale. | Hash and frame tests, browser rates, and scene trace; ordinary geometry/packing are measured separately. | No new memo, bucket or atlas. The trace measures this retained implementation, not an uncached-vs-cached speedup or interactive capacity. The frame still walks unchanged items on an edit; undo may recompute. |

The nearest-outline distance remains a contributor-outline distance; exact
distance to the external boundary of an overlapping union or clipped result
is untested. The tri-state query retains its outline boundary band. The
A4 tests establish clip membership at the named counterexample points, not
a general Boolean boundary-distance solver.

The names of the three ports hid work: adapting the capsule math to the
bench value without an adapter; preserving policy constructions absent from
that capsule piece; designing the recipe result/paint exchange; carrying
returned width parameters; composing screen-group scale and fractional pan;
removing run status/read-report assumptions from 2D and plane callers; and
making the harness retain authored records separately so its timing did not
hide source work. The tests above are the evidence for those additions.

## Pictures, costs and reproduction

Only two golden PNGs intentionally change. The round-nib crossing differs at
68 of 16,384 pixels (maximum channel delta 38); the placed-ink tree differs
at four pixels (maximum delta 1). [Pixel comparison](receipts/golden-delta.json)
records coordinates and values against the merged baseline. Both use the
new capsule geometry, including its circle quadratics. The unchanged text
and other golden comparisons are in the verifier receipt. Golden hashes
establish change custody; the semantic and pixel tests above establish the
selected drawing claims.

The [final trace](receipts/path-step.json) contains source-edit,
geometry-alone, packing-alone and per-frame times:

| Records | Source edits | Geometry alone | Packing alone | First prepare | One color edit prepare |
|---|---:|---:|---:|---:|---:|
| 50 | 22.3 ms | 189.6 ms | 130.6 ms | 458.4 ms | 2.3 ms |
| 400 | 153.8 ms | 1404.7 ms | 969.9 ms | 4384.7 ms | 20.2 ms |
| 1600 | 498.1 ms | 5540.6 ms | 4088.0 ms | 19806.9 ms | 73.0 ms |

These are this machine's headless SwiftShader observations, one dump run. No hardware run or product interaction was tested.
Use the receipt's numbers when considering the push edge and curve count;
do not carry the old tracer's timing forward as capsule performance.

Commands and artifact meanings are in [receipts/README.md](receipts/README.md).
The [client](../../../../src/app/client/README.md),
[engine](../../../../src/app/client/engine/README.md),
[path](../../../../src/app/client/path/README.md),
[harness](../../../../src/app/client/harness/README.md) and
[Region3D](../../../../src/app/client/region3d/README.md) maps and the changed
docstrings describe this tree. The original Claude handoff and code map are
marked as baseline history with a pointer here.

The next implementation starts on this value boundary. The concurrent
companion design must reconcile its caller and executor assumptions with
this landed tree; no later design code was included here. The caller push edge, offset
stroker, pickup brush and compositor remain outside this slice. The exact
open question is: **What does the caller send, and who owns its retained
values, so a named source, recipe, paint or view change reaches only the
geometry, packing and rows whose complete declared inputs changed?**
