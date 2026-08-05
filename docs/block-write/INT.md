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

**Verdict vs the inherited criterion (p95 ≤ 50ms AND ≤1 stall >100ms/min):**
- p95: PASS every run, margin ~1.6× (31.7–39.5 vs 50).
- Stall clause: **FAIL every run** (2, 4, 7, 2 vs ≤1) — a thin 0.3–1% tail,
  max ~145ms, not load- or length-proportional.

**→ Stop clause S3 FIRED and was RULED by Sid on 2026-07-13.** The inherited
stall-count clause is replaced for this pending-input design by **p95 ≤ 50ms
AND p99 ≤ 100ms**; all four runs pass. The pending-input/caret path already
is the pre-registered local affordance. Full optimistic echo remains forbidden,
truth stays streamed, and F4 long-block input loss stays a named LATER.

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
   per (2). Sid ruled (a) on 2026-07-13.

## 4 · Deviations / honest disclosures

- Probe typed append-only into blocks 0–2 of the first-light conversation.
  **Correction 2026-07-13:** the recorded "restart survival" was a client
  reconnect, not a JVM replacement. Before the block-edit WAL existed, the
  development object-container runtime rebuilt from imports on JVM restart,
  so survival was architecturally impossible. The observation is retracted;
  G8 now requires an exact conversation+unit receipt across process replacement.
- Paint timestamps are RAF-quantized (±1 frame) and the probe's rAF is a
  sibling of the render loop's — echo values carry ~±16.7ms resolution;
  the criterion spans ~3 frames.
- Emit cadence is setTimeout-based (nominal 83.3ms); emitted counts exact
  (720/720), open-loop (emission never waits on echo — BW-T3).
- R2's 14 unechoed = keystrokes dropped by the keyboard `m/relieve` (lossy by
  design, latest-wins) before reaching the buffer — honest input loss under
  client lag, no phantom text (the buffer never contained them).

## 5 · G8 wearing setup (Sid, ~4 min)

**Machine pre-drill, 2026-07-13 — green on non-precious material.** Isolated
synthetic conversation `chat:7b0aa3…`, designated fixture block **"just one
line, no structure."**, unit `…:000010:00:000000`: real keydown accepted
`just one line, no structure.!?`; forced stale reverted the transient `x` to
that exact truth and painted `edit refused: stale` at scene y=134 / scroll 0;
a full JVM replacement boot replayed 4 request intents / 0 failed and served
the same text from the same unit. The test WAL was then deleted. Normal boot
reported 0 replayed / 0 failed; the previously contaminated real unit again
ends byte-for-byte at `sell it to me.`

**Pixel diagnosis:** nothing in scene→pixels dropped the notice. `setup(4)`
had focused an offscreen sense-block copy at y=3184 while the screenshot showed
identical prose from the whole-message block at y=809. Bringing the actual
focused block into the camera painted caret + notice. No kernel or renderer
change was made; the probe now uses a unique stale request id per invocation.

1. The normal dev server is live at `http://localhost:8080`. Enter
   `/face minimap-reader-face` (default = first-light conversation).
2. Pick a real block whose edit you deliberately want to keep. **Click that
   visibly rendered block; do not call `setup(n)`** (it can focus an offscreen
   duplicate). Type a short suffix of your choosing and see it settle as truth.
3. Without reloading the page, run `window.__blockwrite.forceStale()` once.
   A transient `x` may flash; it must disappear, leaving exactly your accepted
   suffix, and the same visible block must show `edit refused: stale`.
4. Stop the server with Ctrl-C, start it again with
   `clj -A:dev -X dev/-main`, wait for `[FACE] block-edit WAL replay: N
   replayed, 0 failed`, re-enter the face, and inspect the same block. Your
   accepted suffix must still be present after the full JVM replacement.
5. Lineage is the machine half: G1's physical provenance assertion and the
   two-runtime WAL regression prove import rows remain bit-unchanged while
   graduation/revision rows carry the edit. Sid's wear closes surface + restart.

**Pass receipt to return:** accepted suffix · visible stale revert + reason ·
full-JVM boot replay line · same block text after restart.

**Sid's wearing receipt, 2026-07-13 — PASS.** On the deliberately chosen real
block, accepted suffix `hello` remained served truth; `forceStale()` produced a
transient `x`, reverted to the accepted text, and Sid saw `edit refused: stale`
on that same visible block. Codex then stopped the JVM and started a fresh one;
boot reported `[FACE] block-edit WAL replay: 10 replayed, 0 failed`. Sid
re-entered the same block and confirmed the accepted text was still present.
This is the corrected four-part G8 receipt: accepted edit, visible refusal +
revert, full process replacement, same-unit persistence.

## 6 · Gate review (Fable, this session — CLAUDE.md falsification protocol)

**Inputs:** the ONE falsification finder's record (`FALSIFY.md`: 1 HIGH,
5 MED, 4 LOW; traps BW-T1–T10 all HELD) + the full diff + fresh suite re-runs
THIS session. Finder cost: ~137k tokens (the machine-cut cost rule held:
one finder).

**Adjudication + fixes applied at gate (each with a regression):**
- **F1 (HIGH) — FIXED.** Root cause: a single-value `!block-truth-request`
  atom conflates under Electric (latest-wins), losing a cross-unit pull →
  a stale `!truth-overlay` entry could mask newer truth PERMANENTLY
  (equality-prune never fired) and seed a re-focused buffer with stale text
  (durable overwrite of good truth). Fix, two mechanisms: (1) the request is
  now a UNION map `{unit → nonce}` (conflation lossless by construction —
  the latest value contains every armed unit; capped at 8, oldest-nonce
  dropped), server serves all requested units in one read pass
  (`read-unit` reads at execution time — a late pull can never carry stale
  content); (2) the overlay prune is CLEAR-ALL on face-context arrival (the
  arriving context postdates every earlier merge; in-flight narrow results
  re-add current truth). Also fixes **F5** (paged-out units' entries).
  Regression: the IPC narrowing gate now asserts multi-unit + unknown-unit
  service.
- **F3 (MED) — BOUNDED.** `:pending` + `!continuations` trimmed to the newest
  64 by seq; a late decision for a trimmed entry no-ops (nil-block-id guard
  in `on-decision`). Regressions: `f3-bounds-and-unknown-decisions`.
- **F6 (MED) — FIXED.** Refusal dismisses on resume-typing and on blur
  (a notice never outlives the edit session). Single-slot refusal stays —
  accepted v0 residue (one human writer; revisit when agent writers appear).
  Regression: `f6-refusal-lifecycle`.
- **F4 (MED) — NAMED LATER** (already in §3): per-keystroke whole-face
  rebuild makes `m/relieve` drop keystrokes on very long blocks (measured
  14/720 at ~1.8KB). Fix-shape: focused-block partial rebuild. Falsifier
  named (headed type-burst asserting emitted == buffer delta).
- **F2 (MED) — HISTORICAL COMMIT-TIME FINDING, corrected:** at the 07-12 gate,
  `runtime.cljs:23` (block-edit probe require) and the island-probe lines in
  `render.cljs` pointed at then-untracked probe files, so that candidate would
  have broken a clean cljs build. Commit `a54bee1` subsequently checkpointed
  both probes and mounts as local-only evidence; the missing-namespace premise
  is therefore no longer current. The present WAL candidate excludes all probe
  and doc changes; probe cleanup remains a separate boundary.
- **F7/F8/F10 (LOW) — open doubts, non-blocking,** falsifiers named in
  FALSIFY.md (epoch fan-out audit · hit-test id-collision dump · sidebar-x
  convention). **F9** (probe edits real blocks) disclosed in §4.

**Suite re-runs at the 07-12 gate (post-fix):** client block-edit 11t/74a;
face-projection + server block-write + block-distiller 28t/1004a (cumulative
gate command 39t/1078a); machine-cut-serve + face-transcription 27t/349a — all
green. The 07-13 close adds G8's 1t/4a: server group 29t/1008a, cumulative
40t/1082a.

**Verdict: code PASS at gate** — gates G1–G6, G9 green; traps held; HIGH
finding fixed with regressions. **Close gates completed 2026-07-13:** Sid ruled
S3 (§3), then wore all four G8 parts (§5) across a full JVM replacement. One
honest open item at the 07-12 gate was that the F1 union-map seam was IPC-gated
but not re-driven headed after the fix (the measurement rig was shut down at
Sid's request); the corrected wearing exercised the live request/merge shape
and passed.

**Commit checklist (code commits are Sid's call; separate from docs):** prepare
only the WAL code + its test; strip probe requires from that release surface;
leave docs, `block_edit_probe.cljs`, `island_probe.cljs`, and `src/app/probe/*`
unstaged. Stop before commit.

### Post-G8 WAL mini-falsification (2026-07-13)

The live slice was inspected structurally without printing block content:
10/10 complete EDN lines, all `:object/edit`, 10 unique request ids, one
expected object/document, every payload content-hashed, newline-terminated.
Fresh-JVM replay consumed all 10 with zero failures. The user's WAL remains in
place because it now carries their deliberate edit; no machine verification
edit was added to it after wearing.

New surface routed, not fixed at close:
- **Multi-conversation replay readiness — LATER.** Boot currently reconstructs
  the default transcript, then replays the whole edit WAL. Targets from other
  conversations need per-target import readiness or deferred replay before
  this can claim arbitrary-conversation recovery.
- **No-fsync tail — LATER.** Append is WAL-first and closes/flushed the writer,
  but does not call filesystem `fsync`; sudden host/power loss may lose or tear
  the newest line. Replay isolates malformed lines, which bounds recovery but
  is not a durability claim.
- **WAL growth/compaction — LATER.** Every request intent is retained and replayed;
  compaction or checkpointing belongs with the already-named history-stratum
  policy, not this connection package.
