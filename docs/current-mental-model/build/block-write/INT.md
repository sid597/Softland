# block-write — INT record (G7 measure · S2 event · G8 setup)

Orchestrating session (Fable), 2026-07-12. Lanes A ∥ B green first-run
(records: `LANE_A.md`, `LANE_B.md`, `PHASE_0.md`); this file is the INT + G7
gate artifact. Suites at time of writing: block-write 49a · block-edit 16t/143a
(incl. INT pure-fn tests) · block-distiller + face-projection = 1003a — all
green, this session's runs.

## 1 · What INT built (the seam between the lanes)

- **Decision-shape reconcile** — Lane A returns
  `{:accepted? :replay? :reason :errors :status :request-id …}`; Lane B's seam
  expects `{:status :accepted|:rejected :reason}`. Adapted in ONE place
  (`block_edit_wiring.cljs` `atom-submit!` watch, routes by `:request-id`).
- **Click-to-focus** — `mouse.cljs` assembly-face branch: hit-test the CACHED
  `!face-scene` (trail-face precedent); deepest node whose `:id` path carries a
  block unit-id → `face-click!` focus; miss → blur. The old v0 click-swallow
  (G16 guard) still holds — no fall-through to editor handlers.
- **Keyboard routing** — `!focus = :face-edit` (set on focus), new
  `<face-edit-keys` router (R2 shape) + `face-edit-keys-consumer`; global
  Escape blurs FIRST (pending-input never outlives focus, BW-T4). Content keys
  mint ONE envelope each (BW-T3); caret keys move inside the one buffer value,
  no envelope (revision-noise guard).
- **Render overlay** — `overlay-face-context` (pure, `block_edit.cljc`):
  per-block `block-view` decides what shows — focused block renders
  pending-input + caret glyph (BW-T6/L8: text+caret from the ONE buffer
  value), refusal appends a visible notice line (G5), unfocused blocks render
  truth (single-unit overlay when newer, served text otherwise). Joined into
  the ONE `<face-assembly` `m/latest` (editor_compute.cljs) as two added
  watches; identity fast-path when idle so the scene cache still
  short-circuits. No side effects in the latest (R3/BW-T9).
- **§5 narrowing (engaged — see §3)** — `:block-truth` projection
  (`face_projection.clj`, read-only; serves `read-unit`'s OVERLAY field) +
  `!block-truth-request/!block-truth-data` FacePull pair (artery, additive) +
  client trigger: an ACCEPTED, non-replay decision arms ONE single-unit pull;
  arriving truth merges into `!truth-overlay`; entries prune when the full
  face pull catches up.
- **Outbox lifecycle note (positive finding):** the edit outbox is never
  cleared — Electric re-runs only on value change, and a reconnect replaying
  the last envelope is a journaled no-op by the op-id law (BW-T5 working as
  designed).

## 2 · S2 stop-clause — fired, ruled, fixed, gated

**Verbatim finding:** `render-river-source` served
`:text (:derived-content-text unit)` — the RAW import row field. The import
row is never mutated by design, so every edit was invisible to river-page;
the G7 probe's full-pull channel echoed 0/120 (smoke), and edited content
could not have survived a reboot (G8 fail by construction). The CONTRACT §1
"discovered fact" was half-true: river-page CALLED `read-unit` and discarded
its overlay.

**Ruling (S2: stop → re-verify → contract amends):** `:text` now reads
`(:content-text read-result)` — `UnitReadResult`'s total overlay field
(graduation row's current content when edited, refreshed per revision at
`object_container.clj:1491-1499`; raw derived text otherwise). CONTRACT §1
amended in place. Gated in `block_write_test.clj`: edited block serves the
revised content; every never-edited block byte-identical to the physical
derived row. Live-verified: post-fix, the full-pull channel echoed 720/720.

## 3 · G7 — the measure (headed browser E2E, real WebGPU reader face)

**Method.** Headed system Chrome (`--enable-unsafe-webgpu`, DISPLAY=:0 —
headless has no WebGPU adapter), minimap-reader-face over the first-light
conversation (`10c22f9b…`), real `window` KeyboardEvents at 12/s open-loop for
60s through the full product path (keyboard router → buffer → outbox →
Electric → `submit-block-edit!` `:ack` → decision → `:block-truth` pull →
`!truth-overlay` → scene rebuild → RAF). Echo = keydown → first RAF after the
truth for that keystroke reaches the client render model; ±1 frame (~16.7ms)
quantization. BOTH truth channels timed simultaneously. Probe UNCOMMITTED
(`block_edit_probe.cljs` + tagged require in `runtime.cljs`); driver in the
session scratchpad (`g7_driver.js`).

**Narrow channel (the §5 single-unit pull) — 4 × 60s @ 12/s (720 events each):**

| run | block (state) | p50 | **p95** | p99 | max | stalls >100ms | input drops |
|---|---|---|---|---|---|---|---|
| R1 | 0 (probe-grown ~1.1KB) | 27.5 | **39.5** | 64.8 | 132.7 | **2** | 0 |
| R2 | 0 (~1.8KB) | 23.7 | **34.0** | 52.8 | 144.3 | **4** | **14** |
| R3 | 1 (fresh) | 23.4 | **31.7** | 69.9 | 142.1 | **7** | 0 |
| R4 | 2 (fresh) | 23.2 | **32.0** | 42.1 | 131.6 | **2** | 0 |

(10s smoke, pre-S2-fix server: p95 40.7, 1 stall.) R4 stall forensics: k=9 at
t=0.75s (cold-start region) and k=192 at t=16.0s — no periodic cluster.

**Full channel (as-built: epoch bump → INV-19 1s-debounced full FacePull):**
p50 ≈ **31s**, 720/720 stalls, every run. Under sustained typing each accept
re-arms the 1s debounce, so NO full pull fires until typing stops — the
as-built path misses the criterion categorically (Lane A's BW-T10 finding,
now measured). This is the recorded first-measure miss that engages the §5
narrowing. Post-run the full pull DOES deliver edited content (S2 fix
live-verified: 720/720 echoed at drain).

**Verdict vs the criterion (p95 ≤ 50ms AND ≤1 stall >100ms/min, unchanged):**
- p95: PASS every run, margin ~1.6× (31.7–39.5 vs 50).
- Stall clause: **FAIL every run** (2, 4, 7, 2 vs ≤1) — a thin 0.3–1% tail,
  max ~145ms, not load- or length-proportional.

**→ Stop clause S3 fires: G7 failed after the §5 narrowing. Returns to Sid
with these numbers.** Nothing further engaged without his ruling
(pre-registered next in the contract: local caret affordance — see analysis).

**Analysis for the ruling (Fable):**
1. The failure is a tail, not a floor: every percentile through p99 is inside
   100ms in R4 (42.1); the stall events are isolated ~130ms spikes, shaped
   like GC/scheduler pauses, present even on fresh blocks.
2. Under this package's design the typist never waits on the echo: the
   focused block paints pending-input immediately from the buffer; the echo
   bounds only how fast a REFUSAL can revert (worst observed ~145ms). The
   criterion was authored (write-echo) for a design where the caret blocked
   on the echo — the pre-registered S3 fallback ("local caret affordance") is
   effectively ALREADY BUILT as the pending-input law, which is why the tail
   is invisible at the fingers.
3. One real perf boundary surfaced: R2's 14 input drops on the ~1.8KB block —
   per-keystroke whole-face scene rebuild starts lagging the keyboard relieve
   at long block texts. Named LATER: focused-block partial rebuild
   (client-side; transport untouched).
4. Options: (a) rule the stall clause satisfied for block-write's design and
   re-express G7's tail bound as p99 ≤ 100ms (all runs pass; R4 42.1) —
   recommendation, grounds above; (b) chase the ~130ms tail before wearing
   (machine time, likely GC tuning); (c) the pre-registered S3 next — moot
   per (2). Sid rules; the criterion is not mine to reinterpret.

## 4 · Deviations / honest disclosures

- Probe typed append-only into blocks 0–2 of the first-light conversation —
  those blocks now carry probe text (durable; revision history holds the
  trail; imports untouched underneath). Visible in the reader face until
  edited back. This also produced a REAL G8 observation: the smoke run's
  edits survived the full server restart (WAL replay + graduation overlay)
  and seeded run R1's buffer.
- Paint timestamps are RAF-quantized (±1 frame) and the probe's rAF is a
  sibling of the render loop's — echo values carry ~±16.7ms resolution;
  the criterion spans ~3 frames.
- Emit cadence is setTimeout-based (nominal 83.3ms); emitted counts exact
  (720/720), open-loop (emission never waits on echo — BW-T3).
- R2's 14 unechoed = keystrokes dropped by the keyboard `m/relieve` (lossy by
  design, latest-wins) before reaching the buffer — honest input loss under
  client lag, no phantom text (the buffer never contained them).

## 5 · G8 wearing setup (Sid, ~5 min, dev app already running)

1. `http://localhost:8080` → `/face minimap-reader-face` (address defaults to
   the first-light conversation).
2. Click any reader-pane block → it focuses (edit mode); type — text +
   caret bar render as you type; Escape blurs (block falls back to truth).
3. Restart check: stop/start the dev server, re-enter the face — your edit
   must still be there (the machine half already observed this; your eyes
   close it).
4. Forced refusal (mid-word): console → `window.__blockwrite.forceStale()` —
   the focused block must visibly revert to truth + show
   "⟂ edit refused: edit/stale". No silent drop.
5. Lineage: the import rows underneath are bit-unchanged (gated in G1's
   provenance assertion; graduation rows carry the lineage).
