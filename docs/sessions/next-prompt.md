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

**Binding:** `docs/current-mental-model/decisions.md` · open build contracts: none (framework CLOSED 2026-07-11; machine-cut prompt ready, awaiting Sid's dispatch).
**Register:** direction sessions boot ONLY from `sense-line-model.md` + BETS
North + `vision/LOG.md` tail (CLAUDE.md rule) — never from this board.

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
  Both waves gate-passed; RETRO + adversarial recheck DONE (suite **54/880**
  green at committed HEAD — the gate's propagated 46/804 was a miscount,
  corrected). Retro + residue (D-001): `build/framework/RETRO.md` (§5 residue,
  incl. pair-structure). **Next-up = Sid's call: open the machine-cut package**
  — the G25 pair-structure evidence ORDERS it (D-005); prompt ready
  (`docs/sessions/machine-cut-contract-opening-prompt-2026-07-11.md`).

- **9 · editor-feel — READY (prompt written 2026-07-11, awaiting Sid's
  dispatch).** Typing-lag hot-path repair; causes pinned at file:line in the
  prompt. Paste `docs/sessions/editor-feel-opening-prompt-2026-07-11.md`,
  model Opus 4.8. Thread file: `build/editor-feel/NOW.md` (session creates).
- **10 · write-echo — DONE (2026-07-11). Direct-write FAILS the pre-registered
  criterion** (content 12/s echo p95 **307ms** vs ≤50, 720/720 stalls; 3/s p95
  394ms/max 1007ms; status 12/s p95 313ms). Cause: text-kernel microbatch
  materialization floor ~210ms p50 (leg2), payload/load-independent = iteration
  cadence (leg1 append-ack ~5ms; leg3 additive → verdict robust even at leg3=0).
  COMPOSED (leg3 characterized not E2E); IPC substrate = today's runtime; probe
  UNCOMMITTED. Numbers+method+deviations: `build/write-echo/NOW.md` — the
  empirical input to `build/editor-loop/ROAD.md`'s echo-budget/caret-law.
  **Hand-off (NOT ruled here):** Fable drafts decisions.md PROPOSED (direct vs
  stream topology vs pre-registered caret-affordance) → Sid countersign.
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
- **13 · islands-probe — READY (prompt written 2026-07-11, awaiting Sid's
  dispatch).** Rung-1 render-pipeline probe (offscreen color+depth → minimal
  WGSL 3D pass → composite quad → orbit → numbers; falsifies the
  sleeping-island / rate-decoupling / text-in-3D claims). Probe class: code
  UNCOMMITTED, findings feed the islands CONTRACT (framework Step-0
  precedent). NOT blocked by container-transforms (absolute-position
  composite; re-homed later). Paste
  `docs/sessions/islands-probe-opening-prompt-2026-07-11.md`, model Opus 4.8.

Kinds-round evidence pile (carried verbatim for the marks/kinds round):
"sidetrackkkk" branch receipt · serves/invokes family question · cross-scheme
bridges (panproto lenses).

## Active package blocks

None. When a package opens: STANDING (frozen at open) + NOW (≤15-line
entries) live in `build/<package>/NOW.md`; add a one-line board entry under
Threads; prune it at close (/work-package skill governs the mechanics).
Precedence unchanged: thread files and this board NEVER outrank CONTRACT.md
or decisions.md — flag discrepancies inline, don't pause.
