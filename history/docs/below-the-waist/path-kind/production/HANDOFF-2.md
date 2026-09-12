# Path kind — production handoff 2

2026-09-07, main checkout, `main`. **The broader client pure suite is red:**
115 tests, 950 assertions, 0 failures, 1 error, in
`app.client.region3d.on-plane-test/s5-placed-layout-keeps-the-one-glyph-accessor-seam`,
`:text/layout-provider-required`. It is the same named error in the immutable
starting snapshot, which ran 106 tests / 859 assertions. This slice does not
repair placed text. [Current log](receipts-2/client-pure.log),
[baseline log](receipts-2/baseline-client-pure.log).

Slice A is implemented in `91bd3c7` (executor, CPU compositor, pickup record)
and `e4908d1` (named placement pushes, browser and cold replay receipts).
The starting HEAD was `ad36ce3b448da3eac4b36bec3544c67192d696a3`.
[Changed paths](receipts-2/changed-paths.txt) are derived from Git;
[provenance](receipts-2/provenance.json) identifies the measured source.
Commits were made by exact path. Push remains Sid's. This is an implementation
and verification receipt; acceptance remains Sid's word.

The focused suite passes **56 tests / 490 assertions**. The repository's
browser build has **0 warnings**; its verifier passes all six guards, all
seven DejaVu Slug stations, the representative image goldens and the new
CPU pickup text golden. GPU image goldens were not re-recorded. The full
browser dump also passes both path and Region3D lanes.
[Focused log](receipts-2/path-pure.log), [build](receipts-2/build.log),
[repository verifier](receipts-2/verifier.json), [dump log](receipts-2/dump.log),
[source checks](receipts-2/source-checks.json).
The baseline collector did not capture a completed browser verifier receipt;
no baseline browser pass is claimed here. Server and IPC suites were not run.

The construction is one stateless executor over a caller-supplied vocabulary,
and the compositor's CPU value operations beside its existing physical owner.
The pickup is a record in that vocabulary. It produces a CPU surface; scene
presentation of that returned surface is **not implemented**. The push edge
takes placements already constructed at L0 and does not execute their recipes.
[Executor tests](../../../../../test/app/client/engine/executor_test.clj),
[surface tests](../../../../../test/app/client/engine/surface_test.clj),
[pickup tests](../../../../../test/app/client/path/pickup_test.clj),
[construction boundary tests](../../../../../test/app/client/path/construction_test.clj)
are the evidence for those separations.

The interfaces left for slice B are these; the linked tests establish the
landed cases, not every possible record.

| Piece | Inputs and outputs now | Evidence / extension left |
|---|---|---|
| L0, `construction/construct` | Authored path record → component with `:path/value`; the default and custom constructions use the new named-step grammar. | `construction_test.clj`; the existing Region3D path-placement boundary still consumes the result. |
| L2, `executor/run` | `{:program :roots}`, caller roots, capability table, options → complete results/subjects/state/history, refusal/error, or `:until` continuation. `ctx` carries `:budget :at :step :record`. | `executor_test.clj`; no pending, answer routing, `:from`, provisional interpretation or before-loop suspension yet. |
| `recipe` and `:subjects` | Resume dependencies start at the loop and resolve pre-loop producers; output subjects start at `:return`. Reached roots are whole values, including caller roots. Items are projections declared by `:fields`. Actual reads are diagnostics. | `executor_test.clj` checks caller-root refusal, future items, unread fields, no-loop subjects and reached pre-loop reads; `pickup_test.clj` checks the actual brush prefix. |
| `encode`, `decode`, `resume` | UTF-8 EDN; float arrays use little-endian float32 under base64. Load checks schema, vocabulary and surface lengths; resume checks recipe and consumed projections and reruns pre-loop work. | Executor/surface/pickup tests and [cold JVM runner](../../../../../test/app/client/path/pickup_wire.clj). Separate JVMs are tested; browser round trip stays in one page. Cross-host continuation exchange is **untested**. |
| L3, `engine/surface` | `new`, color/source-over `paint`, nearest `sample` over bottom-to-top surface layers, `mix`, encoding and PNG. Paint copies the surface data. Sampling returns a resolved read with full stack/point/filter snapshot. | `surface_test.clj` checks independent branches, clipping, order, outside transparency, lengths and readable PNG. Non-surface layers and chart domains remain slice B. |
| Kind binding, `path/surface` and `construction/capabilities` | Path regions become coverage in texel coordinates; the table binds dabs and the shared surface operations under named `:args` / `:needs`. Surface keys come from step position. | `pickup_test.clj`, including a rotated domain. Shear-specific coverage is **untested**. |
| L1/L4/L5, `placements/change` → `push/apply!` | Explicit upserts/removals/order/groups and view → current geometry/packs/ranges plus affected sets; those sets drive atlas operations and direct row writes. | [Frame tests](../../../../../test/app/client/path/frame_test.clj), [physical push probe](../../../../../src/app/client/harness/path_push.cljs), repository pixels. |

The executor's expression names and operators remain the one language used
by width rules. Admission checks declared arguments/needs, missing capabilities,
shadowing, the single loop and item fields before capability work. `:next`
replaces state rather than merging it. `executor_test.clj` exercises those
refusals and the retained lazy expression branches. There is no compatibility
entry for `execute`, `run-steps` or the old step grammar. The deleted frame
walk/key and the old run-cache names have no source/test callers in the
[recorded census](receipts-2/source-checks.json). The three unused compositor
read-back helpers were deleted in `91bd3c7`; PNG export is exercised by
`surface_test/png-is-a-readable-picture` and the browser pickup.

The hardest pickup case was run on the definer's record, not a toy substitute.
[The test](../../../../../test/app/client/path/pickup_test.clj) constructs
`records/pickup`, executes it straight, suspends before dab 12, encodes and
decodes that continuation, and resumes with the table supplied again.
The result has **24 dabs**, **24 paints**, revision **24**, key
`paint:23/painted`, and **65,536 float32 components**. Sampling the centre of
texel `(64,64)` gives `[0.01336952205747366 0 0.9866304397583008 1]`.
The resumed painting components, complete history and subjects equal the
straight run by value. A corrupted restored surface length refuses with
`:load`. The history includes each read's full snapshot, including the
initial blue surface at the first read.

Every edit below starts from the same decoded dab-12 checkpoint. These are
both JVM assertions and browser outcomes in [path-step.json](receipts-2/path-step.json),
under `pickup.edits`.

| Edit against that checkpoint | Result | What the receipt establishes |
|---|---|---|
| First pressure `.65 → .4` | `:refused :consumed-items-differ`, `{:at 0}` | The consumed dab's path changed. |
| Last x `102 → 110` | `:complete`; result equals a fresh edited run | Future dabs can change while the first twelve declared projections hold. The dab's whole-path `:s` is not a declared consumed field. |
| Pickup `.5 → .25` | `:refused :recipe-differs`, `:tool` | A reached tool root changed. |
| Surface `128×128 → 64×64` | `:refused :recipe-differs`, `:surface` | The initial painting declaration enters through the pre-loop producer. |

The [fresh-process runner](../../../../../test/app/client/path/pickup_wire.clj)
wrote the checkpoint, exited, then decoded/resumed it in another JVM with
only the files and table. Results, state, history and subjects equal the saved
straight run. [Save receipt](receipts-2/wire-save.log),
[resume receipt](receipts-2/wire-resume.log). The final painting's float32 SHA-256
is `b09d37d3e1e2e8b42b998546ef6ad89cda71be62ded42574618ba51b5fc13aaa` in both
JVM and browser; the repository verifier checks the intentionally added
[text golden](../../../../../test/app/fixtures/render_engine/path-pickup.json).
Hashes are printed evidence, never continuation or geometry identity.

The checkpoint is **8,773,182 bytes on the JVM**, **8,772,902 in the browser**.
It retains state-after surfaces and read snapshots in history; its encoding
does not deduplicate those values. The portable array payload is float32 LE;
the EDN envelope byte counts are not equal and cross-host replay is untested.
The re-run Node bench writes **364,753 bytes**, while its handover stated
364,786; [wire.log](receipts-2/bench9-wire.log) also reproduces its dab-12
content hash and zero-difference replay. That bench count was never an
implementation target. [Regress](receipts-2/bench9-regress.log) reproduces
the definer's texel. The full history is an explicit cost here, with no
compression, memo or content-address substitution introduced.

![CPU pickup, 128 by 128](receipts-2/png/cpu-path-pickup.png)

For the push case, the caller installed **1,600 placements of one material**,
then pushed a color change for placement `20`. The physical probe observes
`GPUQueue.writeBuffer` and the row packer, instruments prior-row comparisons
and `frame/item-key`, and forbids the two former population roads
(`batch-update-pool!`, `coverage/retain!`) during the measured call.
[Probe source](../../../../../src/app/client/harness/path_push.cljs),
[executed values](receipts-2/path-step.json), `push`:

| Observed work | Color edit | Unsnapped pan |
|---|---:|---:|
| Per-placement input checks | 1 | 0 |
| Geometry / pack reruns | 0 / 0 | 0 / 0 |
| Packed rows / prior-row comparisons | 1 / 0 | 0 / 0 |
| Queue writes | 1, offset 2240, 112 bytes | 0 |
| `:reran :rows` | `#{20}` | `#{}` |

Removal leaves 1,599 instances and moves placement `21` to `[20 1]`.
A separate removal forces atlas compaction while the surviving small
placement keeps row range `[0 1]`: its curve base moves `114 → 0`, band base
`[178 0] → [0 0]`. That survivor appears in `:reran :rows`, gets one actual
112-byte write and packs identically to a fresh renderer. This catches work
that an affected-id set alone would conceal. The frame tests also pin color,
knot, pan, device width, group move, screen groups, bucket cover, order and
shared-material placements. They establish the named cases, not general
camera/projection support.

The re-run scene trace uses named edits too. On the dump's headless
SwiftShader adapter, its 1,600 authored placements contain 2,132 regions and
2,056,503 curves. The color edit takes **0.4 ms preparation**, writes one row,
and still waits **2,115.6 ms** for the draw. Geometry edit preparation is
**22.2 ms** for one geometry/pack/row; first preparation is **13,868.4 ms**.
Source-only editing is **588.1 ms**, geometry alone **5,422.8 ms**, packing
alone **4,727.3 ms**. [Trace and workload](receipts-2/path-step.json),
[adapter](receipts-2/shader-digests.json).
HANDOFF-1 recorded a **73.0 ms** color preparation for the same scene shape
([earlier trace](receipts/path-step.json)); this is a historical comparison,
not a calibrated speedup ratio. The write/input counters establish removal
of the population walk. These timings do not establish interactive capacity
or a need for another cache. The independent JVM
[timing re-run](receipts-2/timing.log) puts Z dabs at 583.9 µs, draw-tool
source+geometry at 3,778.5 µs and its packing at 971.8 µs per run.
The browser pickup's 1,108.4 ms is the whole receipt workload (straight,
checkpoint, encoding, replay and edits), not one brush-run timing.

Three design departures were necessary, plus one diagnostic representation
clarification; each is brought back here rather than hidden in a helper.

| Departure / hidden work | Support for this construction | When it stops holding; what changes with it |
|---|---|---|
| `:ms` is omitted from the pure executor log. Timing lives at its caller. | DESIGN §3.6 prohibits a clock; `executor_test/one-language-and-explicit-state` compares complete repeated results. Harness timing supplies measurements. | If step timing is needed, a caller wrapper measures capability calls and keeps that report outside the pure result. The run/continuation value stays unchanged. |
| Path coverage lowers the transformed path in texel coordinates at quarter-texel tolerance, instead of dividing tolerance by the affine map's `a`. | The design admits invertible affine maps; the rotated-domain test has `a=0` and paints/samples correctly. The pickup and existing GPU goldens still pass. | This construction applies to affine planes. A chart or nonlinear map needs its own domain/coverage adapter; the path wrapper changes, not shared source-over arithmetic. Shear-specific behavior is untested. |
| `coverage.cljs` gains named `remove!`, despite §10's untouched list. | The old retained-set API discovers removals by scanning the atlas. The push probe forbids that road and verifies real compaction relocation and writes. Existing thresholds and storage remain. | A different atlas reclamation policy changes how moved slots are reported. Push still needs named removed keys and every moved survivor; existing `retain!` callers keep their full-set interface. |
| `:read-order` carries first-read ordering beside the `:reads` map. | Maps do not promise insertion order across the two runtimes; actual observations stay diagnostics. `executor_test.clj` checks read values, but ordering itself is **untested**. | A consumer choosing a different ordered diagnostic representation changes only that report. Neither recipe nor subject may use observed reads as its root key. |

“One push” hid a group/view membership index, pack-user tracking, row ranges,
range-shift propagation and slot relocation. Those current-state relations
are explicit inputs/outputs of `placements/change`; GPU mutation is in
`push.cljs`. The frame tests and physical probe above are the receipts for
the separation. The old multi-value geometry map is replaced by each
placement's current geometry and complete input. Existing pack buckets,
covers and the atlas remain; nothing about their existence is credited as
proof that they are needed. No new memo, bucket or atlas was added
([changed paths](receipts-2/changed-paths.txt) and the two implementation commits).

“Continuation bytes” hid recursive value encoding, full float-array equality,
array/declaration validation and the cost of retaining snapshots. Those live
in `value_bytes.cljc` and the surface validation boundary, with the cold
runner and corrupted-length tests above. “PNG” hid an actual portable encoder;
`surface_png.cljc` uses deterministic stored-deflate PNG output, read by JVM
ImageIO in `surface_test` and exported by the browser harness. “Subject” hid
the second dependency reading starting from each return expression; the
no-loop and pre-loop-read tests protect it separately from resume's recipe.

The caller migration includes `harness/path.cljs`, its explicit fixture
setup in `harness/path_push.cljs`, and `harness/region.cljs` for the surround
and interleaved draws. `init-path-system` now takes the initial view after
the group buffer, and `item-range` takes a placement id. A whole-picture
fixture replacement names all its ids; the scale trace names only each edit.
`prepared-rows` remains an explicit inspection operation and is not used by
push/frame preparation. The repository path, Region3D and text-tree guards
exercise the migrated callers. Existing `region3d/` source files did not
change, as the Git path receipt shows. No Missionary or store wiring was
added; it remains outside both slices.

Reproduce from the repository root:

```sh
clj -M:test -i test/app/client/path/run_pure.clj
clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier
node test/render_engine/run_verifier.mjs
node test/render_engine/dump_result.mjs /tmp/softland-pickup-dump
clj -M:test -m app.client.path.pickup-wire save /tmp/softland-pickup-wire
clj -M:test -m app.client.path.pickup-wire resume /tmp/softland-pickup-wire
clj -M:test -i test/app/client/path/timing.clj
node docs/below-the-waist/path-kind/bench-9/node-route/extract.js docs/below-the-waist/path-kind/bench-9/waist-bench.html docs/below-the-waist/path-kind/bench-9/node-route/bench.js
node docs/below-the-waist/path-kind/bench-9/node-route/regress.js
node docs/below-the-waist/path-kind/bench-9/node-route/wire.js
```

The broader client command reads the repo runner's namespace list as data,
selects its client entries, then runs those namespaces. This is the command
body used for both baseline and final logs, under `clj -M:test -i`:

```clojure
(require '[clojure.test :as t] '[clojure.java.io :as io] '[clojure.string :as str])
(let [form (with-open [r (java.io.PushbackReader. (io/reader "test/app/test_runner.clj"))]
             (first (filter #(and (seq? %) (= 'def (first %)) (= 'pure-namespaces (second %)))
                            (doall (take-while #(not= ::eof %) (repeatedly #(read {:eof ::eof} r)))))))
      names (filter #(str/starts-with? (str %) "app.client.") (second (nth form 2)))]
  (doseq [n names] (require n))
  (let [result (apply t/run-tests names)]
    (println result)
    (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1))))
```

Slice B starts with DESIGN §3's pending/request/belonging branch and §4's
non-surface layers/chart domain, keeping this pickup and push evidence passing.
Those branches are **not implemented or tested here**. The design's proposed
owner remains unimplemented: a frame caller grants work, a session holds a
continuation, and a long correspondence could instead need a revisioned row
and a worker. That choice changes the caller and byte custody, not the value
passed to `resume`. The exact open question remains:

**Who grants work, and where does a suspended continuation live between a refusal and a grant?**
