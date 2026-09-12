# islands-probe — opening prompt (2026-07-11)

**Session name:** `islands-probe` · **Model: Opus 4.8** · one session, probe class
(framework Step-0 precedent: probe code stays UNCOMMITTED — evidence, not product;
findings feed the future `islands` CONTRACT the way PROBE.md fed CONTRACT v1).

## Background — where this fits (grounding, not verbatim; vision/LOG.md 2026-07-11)

Sid's direction: **3D islands** — 3D faces composited into the 2D land. The
citizenship law from the direction session: *the island is a face, not a
window* — eventually its scene is LAND DATA (addresses of Softland objects),
its camera is wearable state, and "liveable and controllable from inside" is
the acceptance criterion for the whole capability. The ladder: rung 1 =
rendered island (own pipeline, scene-as-data), rung 2 = Box3D as transform
writer (parallel spike running), rung 3 = land objects as scene nodes.

Full direction analysis (ladder, gets/loses, laws): `build/spatial/ROAD.md`.

THIS probe is rung-1 PIPELINE truth only: prove the render path in our
renderer's idiom and price it. Scene-as-data, picking, faces integration are
contract-grade work that comes after — do NOT build them. Absolute-position
compositing is fine; the container-transforms package re-homes it later.

The probe exists to FALSIFY the direction session's claims, not decorate
them. Three specific claims to test hard: (a) a sleeping island (unchanged
scene) costs ~zero GPU per frame; (b) island interior frame rate decouples
from land frame rate; (c) our MSDF atlas text works inside a perspective pass.

## Prove, in order (stop where budget runs out; record how far you got)

- **P1 — offscreen target:** color (swapchain format) + `depth24plus`
  textures, `RENDER_ATTACHMENT|TEXTURE_BINDING`; own render pass with depth
  test. NOTE: `editor/create-render-target` exists (the dormant
  `use-persistent-render-target?` path in `render.cljs:16`/`renderer.cljs`) —
  extend/mirror it (it likely lacks depth); register with `gpu_budget`.
- **P2 — minimal 3D pipeline:** one WGSL shader — uniform view-projection
  (4x4), per-instance model matrix + color in a storage buffer, instanced
  cubes, single-directional Lambert. Follow the existing pipeline-creation
  idiom (`renderer.cljs:517` area) — struct-packed instances, explicit bind
  groups, budget-tracked buffers.
- **P3 — composite:** a textured-quad pipeline (the MSDF pipeline minus SDF
  math) sampling the island texture, drawn as one quad at an absolute
  position inside the existing `draw-frame!` sequence (painter's order;
  full-frame dirty rect during the probe is fine).
- **P4 — interaction (mouse only):** cursor over the quad → drag orbits the
  island camera, wheel dollies. Client atom → RAF-sampled, like all state
  here. Do NOT touch keyboard consumers.
- **P5 — numbers (the deliverable):** island-pass CPU+GPU ms at 100 / 1k /
  10k cubes; texture 512² vs 1024²; composite overhead; **idle cost with the
  island unchanged (claim (a))**; land frame time with island stepping at
  half-rate (claim (b)).
- **P6 — stretch:** one MSDF text billboard inside the 3D pass reusing the
  existing `font_atlas` (claim (c) — text-in-island legibility, screenshot
  at three angles).

## Fences & coordination

- New files only (e.g. `src/app/client/substrate/webgpu/island_probe.cljs`)
  PLUS a minimal mount: ≤15 lines total across `render.cljs`/`runtime.cljs`
  behind a dev flag `island-probe?` default-false, clearly marked. (The
  framework probe's dev-hook lesson: a render probe MUST mount — a JVM
  substitute can't answer GPU questions. The small fence carve-out is
  deliberate and ends with the session.)
- All probe code UNCOMMITTED at close; artifacts = numbers + screenshots.
- The dev app may be RUNNING for Sid's face-wearing (left up at W2 close).
  Check before restarting anything; if it's live, flag on the board (T11)
  and coordinate rather than silently killing it. Dev flow:
  `clj -A:dev -X dev/-main` (never standalone shadow-cljs).
- READ-ONLY on everything else; never read `src/app/server/env.clj`.
- Boot reads: `renderer.cljs` (pipelines/camera/draw-frame), `render.cljs`
  (consumer + RT path), `gpu_budget.cljs`, `buffer_pool.cljs`.

## Output

- `build/islands-probe/REPORT.md`: what was proven through which P-step,
  numbers table, screenshots, the three claims verified/falsified, WGSL +
  integration gotchas for the future contract, and a short "what the
  islands CONTRACT must pin" list.
- Board: flip the `islands-probe` line (pointer + status only). Docs commit
  on `docs/current-mental-model-local`; code stays uncommitted.
