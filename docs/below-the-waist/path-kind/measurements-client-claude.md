# Where the time goes — the path kind's client on the CPU and the GPU (Claude lane, 2026-09-06)

The question, from Sid over the page's list of CPU answers: "Why do we need the CPU, is this not going to bottleneck? Can this not be done on the GPU?" The Codex lane's follow-up, taken as the method: keep the JVM measurements with the exact command and workload; show a browser edit-and-render trace at a representative scene size, separating CPU geometry and packing, GPU execution and waiting. This file is that, with the receipts beside it. Nothing here is a check; the harness's trace carries no pass.

## The commands

```
clj -M:test -i test/app/client/path/timing.clj                        # JVM, per record, microseconds
clj -Sdeps '{:deps {thheller/shadow-cljs {:mvn/version "2.28.23"}}}' -M -m shadow.cljs.devtools.cli release render-verifier
RENDER_VERIFIER_HARDWARE=1 node test/render_engine/dump_result.mjs <out>   # headful, the real adapter; omit the variable for SwiftShader
```

The trace is the `trace` block of `<out>/path-step.json`; the adapter that ran is in `<out>/shader-digests.json`. The receipts landed here: `receipts-trace-before-pack-fix.json` and `receipts-trace-client-claude.json`, both on the AMD RDNA 3 adapter (the desktop's Radeon), before and after the one fix this measurement produced.

## The workload

A scene of n records on a grid over a 1024 × 1024 target: two thirds draw-tool strokes (70 samples each, the limaçon traced from a different phase per record, streamline, fit, taper, the swept nib), a sixth pen tools (four cubics, fill and miter stroke), a sixth borders (a rounded rectangle, filled, stroked one device pixel inside, snapped, so they read the view). Nine frames per scene: first; repeat; a colour edit of one record and its restore; a geometry edit of one record and its restore; a pan; a zoom inside the bucket; a zoom across it. Three numbers per frame: the CPU in prepare (runs, packs, rows, atlas mirror writes, upload enqueues); the CPU in encode and submit; the wall time from submit to the queue reporting its work done (GPU execution plus the wait). And, for the scene, the geometry and the packing timed alone on the CPU.

Caveats. The target is one size; records overlap about two to three deep at the larger scenes, which is a document, not a stress. The GPU time includes an asynchronous wait whose floor is about a millisecond. The trace runs inside the verifier page after its other steps, on a warm JIT.

## JVM, per record (warm, 200 runs)

| Operation | µs |
|---|---|
| Run: the Z as one skin | 262 |
| Run: the Z as 24 dabs | 498 |
| Run: the draw tool, 70 samples to spline to skin | 1,410 |
| Run: the pen tool | 518 |
| Run: the border with snap | 121 |
| Run: the holed concave | 19 |
| Pack: the Z's skin at bucket 0 | 59 |
| Pack: the draw tool's skin at bucket 0 | 291 |
| Classify: the Z at the crossing (runs, packs at query tolerance, winds) | 183 |
| Classify: the draw tool at a point | 1,816 |

The draw tool's skin is 421 curves at bucket 0 and 421 at bucket 3: the envelope flattens the centerline at a fixed tolerance and its outline has no cubics, so the pack's tolerance changes nothing in it.

## The browser on the real adapter, after the fix (`receipts-trace-client-claude.json`)

Geometry alone and packing alone, the whole scene on the CPU:

| n | regions | curves | geometry | packing |
|---|---|---|---|---|
| 50 | 66 | 17,862 | 42 ms | 39 ms |
| 400 | 532 | 141,252 | 274 ms | 269 ms |
| 1,600 | 2,132 | 566,239 | 1,051 ms | 980 ms |

So V8 runs the geometry at about the JVM's speed: 0.7 ms per record, 0.5 ms per region to pack.

Per frame, milliseconds, n = 400:

| Frame | prepare (CPU) | encode | GPU + wait | runs | packs | rows written |
|---|---|---|---|---|---|---|
| first | 791 | 0.0 | 6.2 | 400 | 532 | 532 |
| repeat | 0.3 | 0.0 | 3.6 | 0 | 0 | 0 |
| colour edit | 8.3 | 0.1 | 2.3 | 0 | 0 | 1 |
| restore | 7.1 | 0.0 | 2.5 | 0 | 0 | 1 |
| geometry edit | 11.1 | 0.0 | 2.4 | 1 | 1 | 1 |
| restore | 9.6 | 0.0 | 2.5 | 1 | 1 | 1 |
| pan | 20.3 | 0.1 | 2.3 | 66 | 0 | 0 |
| zoom inside bucket | 42.2 | 0.0 | 3.7 | 66 | 132 | 132 |
| zoom across bucket | 45.9 | 0.1 | 2.8 | 66 | 198 | 532 |

Per frame, n = 1,600:

| Frame | prepare (CPU) | GPU + wait | runs | packs | rows written |
|---|---|---|---|---|---|
| first | 3,108 | 17.9 | 1,600 | 2,132 | 2,132 |
| repeat | 1.2 | 4.6 | 0 | 0 | 0 |
| colour edit | 35.0 | 5.8 | 0 | 0 | 1 |
| geometry edit | 34.3 | 6.9 | 1 | 1 | 1 |
| pan | 78.9 | 4.8 | 266 | 0 | 0 |
| zoom inside bucket | 158.8 | 4.8 | 266 | 532 | 532 |
| zoom across bucket | 184.1 | 7.9 | 266 | 798 | 2,132 |

The same trace on SwiftShader (headless, the verifier's own road) gives the same CPU columns within noise and a GPU column of 50 to 310 ms: software rendering, not a hardware number.

## The reading

1. **The GPU is not the bottleneck on this hardware.** Two to eight milliseconds a frame with half a million curves in the atlas, eighteen on the first frame that uploads them. The per-pixel filler has headroom even at the current curve counts.
2. **Geometry per edit is not the bottleneck either.** One record's construction and pack is about one millisecond in V8, the same as the JVM.
3. **The CPU frame loop is, in three places.**
   - *The walk over unchanged items.* A colour edit of one record costs 8 ms at 400 items and 35 ms at 1,600, with no run and no pack: about 22 µs per item spent re-deriving rows, comparing them, rereading what each construction read, and pruning caches. That is the pull model's price, and it is paid on every edit. The fix is rows cached per item key and rebuilt only for items whose key changed, which makes an edit cost what changed; a push edge, the caller naming the changed records, would remove the walk entirely. This is the concrete reason the reactive question matters at scale.
   - *Records that read the view.* A border with a device-unit width, snapped, reruns on every pan and zoom: 0.3 ms each, 79 ms for 266 of them on a pan. Fixes: rerun only the visible ones; or take snapping and the device-unit width out of the construction and into the placement, where the vertex stage already knows the device grid.
   - *The bucket crossing.* Before the fix, a zoom across a power-of-two bucket repacked every region: 350 ms at 400 records, 1,394 ms at 1,600. The pack key carried the bucket for every region, but a region without cubics (every stroke's skin: lines and arc quads) lowers identically at every tolerance. After the fix, only regions with cubics repack: 46 ms and 184 ms, most of which is the borders' reruns and the pens' cubic fills. What remains can go further by packing a cubic region for a range of buckets at once (a finer tolerance costs quads only by the cube root) and by repacking across frames rather than in one.
4. **The curve count is the CPU's cost, not the GPU's.** 421 curves per stroke skin is the flattening tolerance of the envelope. The offset stroker, keeping curves as curves, cuts packing time, atlas size and first-frame time in proportion. On this adapter the GPU absorbs the count; the CPU pays it at 0.5 ms per region.

## What this changes on the list

| Work | Home | Effect, from the trace |
|---|---|---|
| Rows cached per item, rebuilt only for changed items | `path/renderer.cljs` | A one-record edit at 1,600 items from 35 ms toward a few. The pull walk's floor is the frame key, 1.2 ms at 1,600. |
| View-reading records: visible only, or snap and device width in the placement | `path/component.cljc`, `path/renderer.cljs` | A pan with 266 borders from 79 ms toward zero. |
| Cubic regions packed for a bucket range; repacks spread over frames | `path/renderer.cljs`, `path/pack.cljc` | The remaining bucket-crossing hitch from 184 ms at 1,600 toward a frame. |
| The offset stroker | `path/stroke.cljc` | Curves per stroke from 421 toward tens; packing and first frame in proportion. |

Done in this round: cubic-free regions pack once for all buckets (commit after 8267376; the harness's rates check and the rescaled-group row now expect no repack for the Z and a repack of the pen tool's cubic fill).

## The answer to the question as asked

The split is the one the field makes: pixels on the GPU, geometry and the document's truth on the CPU. It is not inevitable, and the measurements do not make it so; they say where this build spends its time, which is in its own frame loop, in three places with named fixes, none of which moves work to the GPU. The GPU on this adapter has room for ten times the curves it is given. The CPU has one millisecond of real work per edit and tens of milliseconds of walking, and the walking is the thing to remove.
