# islands-probe — REPORT (2026-07-11 → 2026-07-12)

**Class:** probe (framework Step-0 precedent). Code stays **UNCOMMITTED**;
this report + the screenshots in `img/` are the artifacts. Findings feed the
future `islands` CONTRACT the way `framework/PROBE.md` fed CONTRACT v1.

**What ran:** the real Softland app (`clj -A:dev -X dev/-main`, Jetty :8080)
in headed Chrome driven by playwright-core, on real hardware —
**AMD Radeon RX 7900 XTX, RADV/Vulkan 1.4, RDNA-3**, swapchain format
`rgba8unorm`. Headless Chrome yields **no WebGPU adapter**; headed on `DISPLAY=:0`
gets the discrete GPU. (Harness reusable at
`/tmp/.../scratchpad/drive.mjs` + evals; not committed.)

**Probe surface:** one new file `src/app/client/substrate/webgpu/island_probe.cljs`
(~690 lines) + a **5-line mount** in `workspace/runtime/render.cljs` behind a
runtime flag (`window.__island`, default off). No `renderer.cljs` edits. The
mount: (1) `:require` the probe; (2) force a redraw while the probe is driving
so it gets continuous frames; (3) call `island/step!` right after `draw-frame!`;
(4) install the control surface once at init.

---

## What was proven, by P-step

| Step | Claim proven | Evidence |
|---|---|---|
| **P1** offscreen target | color (swapchain fmt) + `depth24plus`, own pass with depth test, budget-registered | cubes occlude correctly (`img/cubes-angle1.png`) |
| **P2** 3D pipeline | one WGSL shader; uniform view-proj (mat4) + per-instance model+color in a **storage buffer**; instanced cubes; single-dir Lambert; depth24plus | `img/cubes-angle1.png`, `img/cubes-angle3.png` |
| **P3** composite | textured-quad pipeline samples the island texture; drawn as one quad at an absolute top-right position via a **second `loadOp:"load"` submit** after `draw-frame!` | `img/composite.png` (island over the live editor) |
| **P4** interaction | cursor-over-quad drag orbits (yaw/pitch), wheel dollies; client atom → RAF-sampled; keyboard untouched | drag verified (yaw 0.7→1.9, pitch 0.5→0.1 for +120/+40px); wheel verified (dist 10→13) |
| **P5** numbers | the deliverable — see below | — |
| **P6** (stretch) | MSDF atlas text **inside the perspective pass**, fwidth-based AA, depth-tested, fixed-orientation (foreshortens) | `img/text-front.png`, `img/text-angle.png` |

The real editor (the "land") renders normally underneath in every shot; the
island is a **face composited on top**, not a window.

---

## P5 — the numbers

**Measurement caveat (important, and a contract input):** the app calls
`requestDevice()` with **no descriptor**, so the device has **no
`timestamp-query`** feature. Per-pass GPU ms therefore can't be read directly.
Two methodologies were used instead:

1. **Serialized GPU timing** (`bench!`): land idle, submit ONE island frame,
   `await queue.onSubmittedWorkDone()`, measure wall-clock, median over N,
   minus an empty-submit baseline. **`onSubmittedWorkDone` has a ~2 ms
   resolution floor** (it resolves on a GPU-poll/event-loop boundary), so any
   GPU cost **below ~2 ms is unmeasurable** and reads as "empty."
2. **Real-world fps** with **vsync ON**: island composited into the live land,
   forced-redraw every frame; `raf_interval` = the true end-to-end frame budget.

> Note: uncapped-vsync RAF timing was tried and **rejected** — with vsync off,
> RAF fires when the *CPU* finishes (~0.1 ms → ~10 000 "fps") while the GPU
> queue backs up thousands of frames; it measures CPU encode, not GPU cost.

### Serialized GPU (median ms; ~2 ms fence floor)

| cubes | tex | 3D isolated | composite isolated | both (median) | empty (median) |
|---|---|---|---|---|---|
| 100 | 768 | < floor | < floor | 2.4 | 2.1 |
| 1 000 | 768 | < floor | < floor | 2.4 | 2.1 |
| 10 000 | 768 | < floor | < floor | 2.4 | 2.1 |
| 100 000 | 768 | < floor | < floor | 2.4 | 2.1 |
| 500 000 | 768 | < floor | < floor | 2.2 | 2.1 |
| **1 000 000** | 768 | **1.1** | < floor | 3.2 | 2.1 |
| **1 500 000** | 768 | **2.3** | < floor | 3.2* | 2.1 |
| 1 000 | 512 | < floor | < floor | 2.4 | 2.1 |
| 1 000 | 1024 | < floor | < floor | 2.4 | 2.1 |

`< floor` = GPU cost below the ~2 ms `onSubmittedWorkDone` resolution. Texture
size (512/768/1024) made **no measurable difference** — composite is one quad;
fill is trivial. Cost only rises above the floor past **~1 M cubes**
(≈1.1 ms/M, 36 M triangles/M). *1.5 M `both` is noisier near the buffer ceiling.

### CPU encode (live, vsync-on, per frame)

| phase | ms |
|---|---|
| 3D pass encode | 0.01 – 0.02 |
| composite encode | 0.004 – 0.012 |
| **island total (CPU)** | **~0.02 – 0.03** |

### Real-world fps (vsync ON, island composited into the live land)

| config | fps | frame interval |
|---|---|---|
| land-only (island off, forced redraw) | 60.7 | 16.47 ms |
| island 2 k cubes (realistic) | 60.7 | 16.48 ms |
| island 50 k cubes | 60.7 | 16.48 ms |
| island 200 k cubes | 60.7 | 16.48 ms |
| island 1 M cubes | 60.7 | 16.48 ms |
| island 1.5 M cubes | 60.7 | 16.48 ms |

**The land never drops below 60 fps up to 1.5 M cubes** (the single-buffer
ceiling). At realistic island sizes (hundreds–thousands of objects) the island
is, to measurement precision, **free**.

### Buffer ceiling (a real finding)

**2 M cubes → `GPUValidationError`.** The instance storage buffer is 80 bytes/cube;
2 M = 160 MB > the **default** `maxStorageBufferBindingSize` of **128 MB**. The
adapter advertises **2 GB**, but the app requested the device with no
`requiredLimits`, so it got WebGPU defaults. Practical single-buffer cap ≈
**1.6 M cubes** until boot raises limits or the scene is chunked. (Far beyond
any realistic island scene.)

---

## The three claims

- **(a) A sleeping island (unchanged scene) costs ~zero GPU/frame — SUPPORTED.**
  Mechanism implemented: when scene+camera are unchanged, **skip the 3D pass**
  and composite the *cached* offscreen texture. `composite-only` (= sleeping)
  is at the ~2 ms floor and indistinguishable from an empty submit — i.e. a
  sleeping island costs one textured quad, below measurement precision. You pay
  the 3D cost **only on frames the scene/camera actually change.** This is the
  direct GPU-cost basis for scene-as-data: when the land data feeding an island
  doesn't change, the island sleeps for free.

- **(b) Island interior frame rate decouples from land frame rate — SUPPORTED
  (mechanism proven).** The `half-rate` mode re-renders the 3D pass every other
  frame while the composite (cheap) runs every frame; the land holds 60 fps
  regardless. The island's *expensive* update cadence is independent of the
  land's present rate — "pay 3D only when you choose." **Caveat:** in this probe
  the composite still runs inside the land's frame (same RAF); a truly
  independent island loop (separate thread/worker) was **not** tested — the
  swapchain-ordering constraint (composite must follow the land's clear in the
  same frame) is why. The *decoupling of cost from cadence* is proven; *parallel
  presentation* is contract-grade and open.

- **(c) MSDF atlas text works inside a perspective pass — VERIFIED.** "SOFTLAND"
  renders crisp and legible on a fixed-orientation plane embedded in the 3D
  scene, foreshortening under perspective and depth-tested against the cubes
  (`img/text-front.png`, `img/text-angle.png`). **Key:** it works only with a
  **`fwidth`-derived `screenPxRange`** (the Chlumsky formula,
  `0.5·dot(distanceRange/atlasSize, 1/fwidth(uv))`), **not** the 2D text
  pipeline's precomputed per-vertex `visual_size` — that assumes a constant
  texel-to-screen ratio, which perspective breaks.

---

## WGSL + integration gotchas (for the contract)

1. **Bind-group visibility must list every stage that touches a binding.**
   Two `GPUValidationError`s cost real time here: the 3D uniform is read in the
   *fragment* shader (Lambert light dir) and the text transform uniform is read
   in *both* stages — both had to be `VERTEX | FRAGMENT`, not `VERTEX`. The
   error text ("Entry point's stage (Fragment) is not in the binding visibility
   … (Vertex)") is precise; wrap pipeline creation in
   `pushErrorScope("validation")` to surface it (the app's global hook only logs
   the *type*).
2. **The composite seam is a second submit, `loadOp:"load"`, same frame.**
   `draw-frame!` does `getCurrentTexture → one pass (clear) → submit` and does
   **not** present (the browser presents when the RAF callback returns). A
   second `getCurrentTexture` in the same RAF returns the *same* texture;
   `loadOp:"load"` composites over the land; GPU runs the two command buffers in
   submission order. No `renderer.cljs` change needed.
3. **`loadOp:"load"` needs the land to have drawn this frame.** The land only
   draws on world-change; on idle frames the swapchain isn't updated, so an
   independent island loop compositing with `"load"` would read a fresh/garbage
   texture. Hooking after `draw-frame!` (and forcing redraw while driving)
   guarantees a valid background + correct order.
4. **Instances in a read-only storage buffer, indexed by `@builtin(instance_index)`**
   worked cleanly (storage buffer readable in the vertex stage). Model matrix +
   color, 80 B/instance, column-major mat4.
5. **`onSubmittedWorkDone` is a ~2 ms-resolution proxy, not a profiler.**
   Without `timestamp-query` there is no cheap sub-ms GPU number. See §P5.
6. **MSDF atlas is a plain sampled `rgba8unorm` texture** (`/font_atlas.{png,json}`,
   2048², distanceRange 8, planeBounds/atlasBounds per glyph, `yOrigin:"bottom"`
   → `v = 1 − y/size`). `copyExternalImageToTexture` from an `ImageBitmap`.

---

## What the islands CONTRACT must pin

1. **Device limits at boot.** Decide whether the app requests
   `maxStorageBufferBindingSize` (and `maxBufferSize`) above the 128 MB / 256 MB
   defaults, or mandates **scene chunking** per island. Also: request
   **`timestamp-query`** if islands are to be profiled/perf-gated (needed for a
   real `G-perf` clause).
2. **Scene-as-data → dirty policy.** Claim (a) hinges on "re-render only when the
   scene/camera changed." The contract must define *what marks an island dirty*
   (land-object edges changed? camera/wearable-state changed?) and the
   cached-texture lifecycle. This is where rung-1 meets rung-3.
3. **Compositing model.** This probe: absolute-position quad, full-frame dirty,
   painter's order after `draw-frame!`. The `container-transforms` package
   re-homes placement; the contract must say how island quads participate in the
   land's dirty-rect / scissor system and z-order among faces.
4. **Text-in-3D uses the fwidth MSDF path**, not the 2D pipeline's size-based
   range. If islands reuse the renderer's text system, that shader needs a
   perspective variant.
5. **Interaction routing.** The probe grabbed pointer events over the quad via
   window-capture listeners (canvas-target wheel didn't reach it). The contract
   must define how an island claims pointer/scroll focus without fighting the
   land's drag-select / scroll consumers (picking is rung-1+; deferred here).
6. **Claim (b) parallel presentation is open.** Cost-vs-cadence decoupling is
   proven; a genuinely independent island render loop (worker/OffscreenCanvas)
   vs. the same-frame composite is a contract decision, gated by the
   swapchain-ordering constraint in gotcha #3.

---

## How to reproduce (probe still in tree, uncommitted)

1. `clj -A:dev -X dev/-main`, open `http://localhost:8080` in **headed** Chrome
   with `--enable-unsafe-webgpu --enable-features=Vulkan` on a machine with a GPU.
2. Console: `window.__island.enable()` — a spinning cube island appears top-right.
   `window.__island.loadAtlas()` — the MSDF label appears.
   `window.__island.set({cubes:1000, tex:768, halfRate:true, sleep:false})`.
   `await window.__island.bench(1000000, 768, 80)` — serialized GPU numbers.
   `window.__island.stats()` — live per-frame timings. `window.__island.disable()`.
3. To remove: delete the file + revert the 5 mount lines in `render.cljs`
   (all tagged `islands-probe 2026-07-11`).
