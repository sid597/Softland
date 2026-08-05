# box3d-spike — REPORT (2026-07-11)

> **VERDICT: BUILDS + DETERMINISTIC.** Box3D v0.1.0 builds native and to
> single-threaded wasm out of the box (Emscripten is a first-class upstream
> target). A 20-body / 1000-step toppling world produces a **byte-identical
> position trace across native x86-64 and wasm** — and even across the
> SIMD/scalar boundary — so a Box3D sim can serve as a *replayable trail*
> (same seed → same history). Caveat on scope: determinism was proven
> empirically on **one host** (x86-64 Linux, V8/Node) across two toolchains;
> true cross-*hardware* determinism (ARM, Apple Silicon, other browser wasm
> engines) rests on upstream's in-repo `CrossPlatformTest`, not on my probe.
> A clean gate: this lane is **unblocked** for islands rung-2 planning.

Everything below happened outside the repo, in scratch. No Softland code touched.

---

## 1. Provenance (what exactly was tested)

| Item | Value |
|---|---|
| Repo | `https://github.com/erincatto/box3d` (confirmed via box2d.org/documentation3d) |
| License | **MIT** (© 2026 Erin Catto) — vendoring a pinned commit is clean |
| Language | portable C17; public C API (`include/box3d/`, 417 `B3_API` functions) |
| v0.1.0 tag | commit `8441b4a` (2026-06-30, PR #21) |
| HEAD tested | commit `e9f6f1d` (2026-07-10, PR #67 "Add benchmark") — **main used for the build** |
| Churn | ~46 PRs in ~10 days between tag and HEAD — actively moving |
| Toolchain | gcc 13.3.0 · cmake 3.28.3 · ninja · emcc **6.0.2** · node v20.20.2 · x86-64 Linux |

## 2. Native build — PASS

`cmake -G Ninja -DCMAKE_BUILD_TYPE=Release -DBOX3D_SAMPLES=OFF -DBOX3D_BENCHMARKS=ON -DBOX3D_UNIT_TESTS=ON`
→ configured + built lib + tests + benchmark in **~2 s**. **Zero FetchContent
downloads** — the library, unit tests, and benchmark are self-contained
(`parallel_for.c`/`scheduler.c` are a built-in task system, no external threading dep).

- **Samples deliberately NOT built.** They pull sokol + imgui + implot + nfd +
  OpenGL + X11/Xi/Xcursor — a full GUI/display stack, irrelevant to a headless
  de-risk and to the rung-2 writer role. The real signal is lib + unit tests +
  a custom stepping harness, all of which ran.
- **Unit tests: 21/22 pass.** The lone failure is `CompoundTest ›
  CompoundMaterialDedup` (`c->materialCount == 1`) — a compound-shape material
  dedup assertion, **off the create/step/read-transform path entirely**.
  It fails **identically on the v0.1.0 tag AND on HEAD**, so it is a
  pre-existing, shipped-in-the-release failure, not a regression on main.
- **The two tests that matter most for Softland both pass:**
  - `DeterminismTest` → `MultithreadingTest` + `CrossPlatformTest`. The
    multithreaded solver is engineered to be *task-order independent*
    (deterministic with threads on). Upstream ships a cross-platform
    determinism assertion.
  - `RecordingTest` → 18 subtests incl. `ScrubBackward`, `SeekWithHull`,
    `QueryReplay`, `KeyframeHandleReuse`, `AllOps`. Box3D core ships a
    **record → scrub → replay** subsystem (`src/recording.c`,
    `recording_replay.c`, `world_snapshot.c`). Directly relevant to
    "history is terrain / scrub the trail."

## 3. Wasm build — PASS

Built with `emcc` directly over `src/*.c` + harness (bypassing the CMake path
on purpose — see §6 on threading), **single-threaded**, SSE2→wasm SIMD128
(`-msimd128 -msse2`), `-ffp-contract=off`. Compiled **first try**, no source
edits. Emscripten is a first-class upstream target (the CMakeLists has explicit
`if(EMSCRIPTEN)` branches mapping SSE2 and wiring pthreads).

| Artifact | Size |
|---|---|
| `step_wasm.wasm` (full engine core + harness, `-O2`) | 393 KB raw / **163 KB gzip** |
| JS glue (`.js`) | 69 KB |

## 4. Throughput (20 bodies, single-thread, 1000 steps)

| Build | steps/sec | µs/step | % of a 16.7 ms frame |
|---|---|---|---|
| native (SSE2) | ~67,000 | ~15 | ~0.09% |
| wasm (SIMD128) | ~33,000 | ~30 | ~0.18% |

wasm is ~2× slower than native (normal). Either way the writer role is **nowhere
near compute-bound** at this scale — 20 bodies costs a fraction of a frame; you
could step thousands of bodies per RAF tick before this matters.

## 5. Determinism — the headline result

Harness dumps every body's position every step (single-precision floats promoted
to double), writes a 480 KB binary trace, and folds a rolling FNV-1a-64 hash.
Scene is a 20-box **leaning column that topples** — RNG-free but chaotic, so any
floating-point divergence amplifies into a visible one. Comparison = `sha256` /
`cmp` byte-for-byte.

| Comparison | Result |
|---|---|
| same binary, run twice (replay) | **byte-identical ✓** |
| **native x86-64 ↔ wasm** | **byte-identical ✓** (sha256 `0b9cd6e8…`) |
| SIMD ↔ scalar (`-DBOX3D_DISABLE_SIMD`) | **byte-identical ✓** |
| all four builds (native/wasm × SIMD/scalar) | **one identical trace** |

Trace hash `0xd66228accc63afc8` across all builds. This is stronger than the
minimum bar: the SSE2 and scalar code paths compute the *same rounding in the
same accumulation order*, so replayability doesn't even require pinning the SIMD
mode — only the seed/config and the engine version. Credit the upstream
`-ffp-contract=off` default (disables fused-multiply-add, the usual native-vs-wasm
divergence source; ref box2d.org/posts/2024/08/determinism).

**Scope boundary (honest):** all four builds ran on the *same physical machine*
(x86-64 Linux) under one V8/Node. The native↔wasm result is a genuine
cross-*compiler* / cross-*ISA-semantics* test (gcc-SSE2 vs LLVM-wasm-SIMD128),
which is meaningful. It is **not** a cross-*hardware* test — ARM/NEON, Apple
Silicon, and different browsers' wasm engines are untested here. Upstream's
passing `CrossPlatformTest` is the evidence for those; a real rung-2 plan should
re-run this trace probe on ≥2 distinct architectures / browser engines.

**Precision note:** default build is **single precision** — `sizeof(b3Pos)=12`
(float x/y/z); `b3WorldTransform ≡ b3Transform` (float). Double-precision world
positions (the Jolt-style `double p` + `float q` split, for accuracy far from
origin) are **opt-in** via `-DBOX3D_DOUBLE_PRECISION=ON`, at the cost of a bigger
wasm and a heavier per-frame transform read-back.

## 6. Writer seam (the API question the prompt flagged as most important) — PASS

Proven end-to-end from JavaScript, not just read from headers. A thin exported
C shim (`seam.c`) + a node driver (`seam_test.js`):

- **Read-out primitive:** `b3World_GetBodyEvents(world) → b3BodyEvents` returns a
  **moved-only delta** (`{ b3BodyMoveEvent* moveEvents; int moveCount; }`), each
  event carrying `{ b3WorldTransform transform; void* userData; b3BodyId; bool fellAsleep }`.
  You consume the moved list, not poll all bodies — the cheap per-frame path.
- **JS reads it zero-copy:** the shim writes `[index, px,py,pz, qx,qy,qz,qw]` per
  moved body into a wasm buffer; JS wraps it as a `Float32Array` view over
  `HEAPF32` — one bulk read per frame, no per-field JS↔wasm calls. Verified
  values match the native harness (body 0 → `(-0.0013, 1.4986, 0.0009)`).
- **Settle → commit signal is real:** `moveCount` stayed 20 while the stack was
  live, then dropped to **0 at step 957** when all bodies slept. That zero
  transition is a natural **"settle reached → commit KernelEvent to Rama"**
  trigger — exactly the settle-state semantics the opening prompt described.
- Maps directly onto the rung-2 design: RAF `b3World_Step` → read moved deltas →
  write into per-container transform buffers (the pan/zoom seam); on `moveCount==0`,
  commit the settled pose as a Rama event.

**Integration caveat:** the public API passes small structs by value
(`b3WorldId`, `b3Vec3`, `b3WorldTransform`), which `ccall`/`cwrap` can't marshal.
The real binding is a flat C shim exposing primitive/array signatures compiled
with `-sEXPORTED_FUNCTIONS` (as done here), or embind — not raw `ccall` over the
417-function surface.

## 7. Maintenance posture

- **MIT** → vendor a pinned commit freely.
- **v0.1.0 is genuinely early**: a unit test fails in the release tag itself, and
  main moved ~46 PRs in 10 days. Recommendation: **vendor a pinned commit**
  (don't track a moving main). The pin choice (v0.1.0 tag vs a chosen HEAD) is
  low-stakes for our path — the compound-dedup failure is identical either way
  and orthogonal to the writer role.

## 8. Recommendations for islands rung-2 planning (not decisions — inputs)

1. **Vendor a pinned commit**, single-threaded wasm build.
2. **Single-threaded by default.** The multithreaded path is deterministic
   (upstream test) but its Emscripten build needs `-pthread`/`USE_PTHREADS` →
   `SharedArrayBuffer` + COOP/COEP serving headers + worker init. Avoid that
   complexity until profiling forces it; §4 shows compute is not the bottleneck.
3. **Writer seam = `b3World_GetBodyEvents` → flat float buffer → `HEAPF32` view →
   per-container transform buffers**; `moveCount==0` → commit settled pose.
4. **Precision:** default float is fine for a bounded scene. If 3D islands sit far
   from the land's world origin, evaluate `-DBOX3D_DOUBLE_PRECISION` (bigger wasm
   + read-back) before committing to a coordinate convention.
5. **Trail scrubbing** may not need us to build snapshotting from scratch —
   `world_snapshot.c` + the recording/replay subsystem already exist and pass
   tests; evaluate reusing them vs. re-deriving from Rama events.
6. **Open follow-ups (rung-2, not spike):** cross-*hardware* determinism re-run
   (ARM / Apple Silicon / Chrome-Firefox-Safari wasm); binding ergonomics
   (embind vs hand-written shim); whether settle-events or continuous deltas are
   the right Rama write granularity.

## 9. Reproduction

Scratch (out-of-repo), commit-pinned:

```
scratchpad/box3d-spike/
  box3d/            # clone @ e9f6f1d (main); worktree box3d-v010 @ 8441b4a (v0.1.0)
  step_harness.c    # native+wasm stepping + trace harness (same source both targets)
  seam.c seam_test.js   # JS-driven writer-seam probe
  build_wasm.sh     # emcc build: `simd` | `scalar`
```

- Native: `cmake -G Ninja -DBOX3D_SAMPLES=OFF …` then `gcc -O2 -ffp-contract=off -I include step_harness.c libbox3d.a -lm`
- Wasm: `bash build_wasm.sh simd` → `node step_wasm_simd.js trace.bin`
- Determinism: `cmp trace_native.bin trace_wasm_simd.bin` (identical) ; hash `0xd66228accc63afc8`, trace sha256 `0b9cd6e815e56022…`
- Seam: `node seam_test.js` → settle at step 957.
