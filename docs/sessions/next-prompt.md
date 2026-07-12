# Baton — the state board

**Charter (2026-07-10; replaces the monolith baton — Sid's ruling in-session).**
This file answers "where are we — done / doing / next" per open thread, at two
altitudes: the direction line, then implementation threads. Every line is a
POINTER + STATUS, never content — detail lives in the linked doc, history
lives in git (`git log -- docs/sessions/next-prompt.md` holds every prior
baton verbatim, including the 1,568-line 2026-07-10 predecessor). Parallel
sessions each write their OWN thread file (`build/<package>/NOW.md`) and touch
only their board line; this board is the single shared surface, so
cross-thread coordination flags (the T11 class) live here. Prune lines at
thread close. Pre-registered form-break (decisions.md 2026-07-10 note): if a
fresh session mis-orients — needs more than board + thread file to boot — or
content creeps back in because there's no room, revert or amend; don't suffer.

**Binding:** `docs/current-mental-model/decisions.md` · open build contracts: **machine-cut** (v1 COUNTERSIGNED-BINDING 2026-07-12 — `build/machine-cut/CONTRACT.md`).
**Register:** direction sessions boot ONLY from `sense-line-model.md` + BETS
North + `vision/LOG.md` tail (CLAUDE.md rule) — never from this board.

**Vision (D-015):** LOG routed through **2026-07-11**. Open pulls (prune as absorbed):
- LOG 07-11 **designer edit-mode** (figma-like direct edit over live faces, 80→95%) → filed as BETS Candidate C2; sharpening at next intake sitting
- LOG 07-10 **design-loop-in-land beyond D-012** (accepted faces → arsenal → Softland-as-design-orchestrator → deploy + share) → BETS Candidate C1; sharpening at next intake sitting
- LOG 07-11 **"optimized on every dimension while maintaining explorable explanations + lineage, live and composable"** → North-grade sentence, Sid's hand only — next vision sitting
- LOG 07-11 **branch-from-base** (two worlds built out of one base, code-diff-like) → input to the spatial cluster contracts (thread 12)
- **BETS H1 KILL evaluation due ~2026-07-19** (verdict log): do new thinking panels still start on paper / wall photos still get pasted into chats?

## Direction line (countersigned 2026-07-09: "all agree on the specs")

spec v0 + CONTRACT v1 ✅ → substrate ✅ (block-kernel + code-atom families both
CLOSED 07-09/10; retros signed into /work-package + /atomize) → **dual-read
falsifier** (Sid ∥ Fable independent reads of the example chat — the SPEC's
falsifier, NOT the benchmark room; SPEC ≠ BENCHMARK stands) → dogfood
reconciliation UI → marks/kinds round → benchmark room (question bank waits
on kinds).

## Threads

- **1 · Sense-line main — dual-read round OPEN (2026-07-10).** Material
  picked by Sid: the dissolution chain; read window W1 = conversation
  `18d63935` (51 river events; chain ingested, receipt + braid notes in
  `build/sense-line-mvp/dual-read/NOW.md`). PROTOCOL.md drafted (Sid
  redlines); read material + design fixture generated. Next: Sid's read
  (his pace) ∥ Fable's read (fresh session, PROTOCOL §anti-anchoring) ∥
  design session (thread 2).
- **2 · Reconciliation UI — brief ready, fork travels with it.** Sid opens a
  fresh design session with `build/sense-line-mvp/dual-read/DESIGN_BRIEF.md`
  (one paste); the task-UI vs cards-wall fork is its EXPLORE item 2 —
  rendered both ways, picked by feel there. Design pass precedes build.
- **3 · block-kernel residue — PARKED (D-001).** HEAD re-green confirmed by
  fresh run 2026-07-10 (28/1250/0/0). Parked at `block-kernel/RETRO.md` §5:
  §10 cursor/denormalized query (form-break gated) · multi-stratum under-fill
  (waits on a 2nd distiller) · content-preview asymmetry (marks round).
- **4 · code-atom residue — PARKED (D-001).** `code-atom/RETRO.md` §5:
  F4 fault-injection falsifier · N5 history-sort nit. (Durability fork
  RULED 2026-07-11 blanket: in-memory stands until form-break.)
- **5 · D-006 evaluation — ACCUMULATING, no open ruling.** Consolidated
  two-package tally recorded 2026-07-10; criterion-2 probe WAIVED for these
  packages (2026-07-11 blanket; pre-registered reopen: runs iff the final
  evaluation is close).
- **6 · Queued by Sid — UNBLOCKED.** Code-size/verbosity audit of `src/`
  (was gated post-package; both packages now closed).
- **7 · Design queue — PARKED.** Question-unit design (form-break evidence
  acquired 07-04; decisions.md Open Questions + trail-view INPUTS.md item 5)
  · model-UXR question bank (post-kinds; apparatus warm, DIRECTION.md §4.3)
  · block/code two-arm braid (noted 07-09, not engaged).

- **8 · framework (faces-as-assemblies) — CLOSED + retro'd (2026-07-11).**
  Retro + residue: `build/framework/RETRO.md` (suite 54/880 green at
  committed HEAD). Its pair-structure residue spawned thread 14.

- **14 · machine-cut — COUNTERSIGNED + WAVE RUNNING (2026-07-12).**
  Contract v1 BINDING (Sid, in-session); `:pairs-with` enum landed
  (`f864c74`, verified). Lanes A (driver) ∥ B (serve+face) dispatched as
  parallel Opus 4.8 subagents per `build/machine-cut/LANES.md`. Then: INT
  (boot attach, WAL wiring, serial suite) → G13 receipt → G14 wearing →
  falsification batch → gate — this orchestrating session; close/retro =
  a separate fresh session. Thread file: `build/machine-cut/NOW.md`.

- **9 · editor-feel — CODE DONE + verified, awaiting Sid's wear (2026-07-11).**
  All 5 hot-path causes removed/gated: Phase-4B mirror OFF by default
  (`editor-rama-mirror?`, gated in save-editor-doc! → covers mouse.cljs too),
  per-key `(mapv count)` → `:lengths` cached on the doc (reused by
  `<bracket-match`), fold rescan throttled ~150ms (leading, initially-ready —
  JVM-probed). Compiles clean in the live dev server (3 files, 0 warn);
  face+missionary suite 85t/1625a green. Code + docs committed separately.
  Left: `[RAF]`/typing-burst capture + Sid's fingers (dev app is live, serving
  it). Thread file: `build/editor-feel/NOW.md`.
- **10 · write-echo → D-014 CLOSED (countersigned 2026-07-12); thread done.**
  Stream probe PASSED decisively (p95 7.66ms, 0/1620 stalls; numbers/method:
  `build/write-echo/NOW.md`). **decisions.md D-014** rules the transport:
  direct write over STREAM, no optimistic echo; op-id idempotency
  contract-binding; clustered re-measure pre-registered at substrate change;
  browser E2E at the block-write gate. **Next: Fable drafts the block-write
  CONTRACT** (editor-loop ROAD §6 — first WRITE face, editing blocks in the
  reader face). Probes stay UNCOMMITTED (`write_echo_probe.clj`,
  `stream_echo_probe.clj`).
- **11 · box3d-spike — DONE (2026-07-11): BUILDS + DETERMINISTIC.** Box3D
  (MIT) builds native + single-threaded wasm (163 KB gzip); native↔wasm trace
  **byte-identical** → sim = replayable trail. Writer seam proven from JS
  (`b3World_GetBodyEvents`→HEAPF32; settle@957). Caveat: determinism verified
  one host (x86-64), cross-hardware rests on upstream test. Findings:
  `build/box3d-spike/REPORT.md`. → islands rung-2 **unblocked**.
- **12 · Spatial cluster — NAMED, not dispatched (2026-07-11 direction
  session).** `container-transforms` (contract AFTER Sid wears Boxes/Minimap —
  dev app running) · `islands` CONTRACT (awaits islands-probe findings +
  Sid's Box3D reading answer: engine/docs/both + first-form pick) ·
  `point-and-say` + `scene-diff` (dimension-agnostic, can rehearse in 2D over
  live faces) · `G-perf` standing gate clause (one-paragraph template
  amendment, pending). Direction trail: vision/LOG.md 2026-07-11 entries;
  full analysis: `build/spatial/ROAD.md` (substrate facts · transforms design ·
  islands ladder+gets/loses · point-and-say · scene-diff · Box3D · napkins).
- **13 · islands-probe — DONE (2026-07-12).** All P1–P6 proven on real HW
  (Radeon 7900 XTX, headed Chrome). Three claims: **(a) sleeping island ~free**
  (skip 3D, composite cached texture) + **(b) cost decouples from cadence**
  SUPPORTED (parallel-presentation still open); **(c) MSDF text in a perspective
  pass** VERIFIED (needs fwidth-derived screenPxRange). Island holds the land at
  60 fps up to 1.5 M cubes; single-buffer ceiling ~1.6 M (default 128 MB storage
  limit — device requested with no requiredLimits). Report + screenshots:
  `build/islands-probe/REPORT.md`. Probe code UNCOMMITTED in tree
  (`island_probe.cljs` + 5 mount lines in `render.cljs`, all tagged
  `islands-probe 2026-07-11`; delete to remove). Feeds the `islands` CONTRACT
  (thread 12): boot device-limits/timestamp-query, scene-as-data dirty policy,
  compositing/z-order, fwidth-MSDF text path.

Kinds-round evidence pile (carried verbatim for the marks/kinds round):
"sidetrackkkk" branch receipt · serves/invokes family question · cross-scheme
bridges (panproto lenses).

## Active package blocks

**machine-cut** — STANDING + NOW at `build/machine-cut/NOW.md` (thread 14).
Charter: STANDING (frozen at open) + NOW (≤15-line entries) live in
`build/<package>/NOW.md`; one board line under Threads; prune at close
(/work-package skill governs the mechanics).
Precedence unchanged: thread files and this board NEVER outrank CONTRACT.md
or decisions.md — flag discrepancies inline, don't pause.
