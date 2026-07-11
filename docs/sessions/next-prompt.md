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

**Binding:** `docs/current-mental-model/decisions.md` · open contracts: `build/framework/CONTRACT.md` (v1, 2026-07-11).
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

- **8 · framework (faces-as-assemblies) — WAVE 2 OPEN (dispatched by Sid
  2026-07-11; W1 closed same day, gate passed, commit `c84ebfa`).** CONTRACT
  v2 §§16–21 added (arsenal placement + gates G17–G26); D-011/D-012
  COUNTERSIGNED and entered in decisions.md (the §15 ⚠ is resolved). Lanes:
  D arsenal kernel ∥ E plurality (Sid's picks: Boxes + Minimap/Reader);
  W2-INT + G26 ride the orchestrating session. Thread file:
  `build/framework/NOW.md`.

Kinds-round evidence pile (carried verbatim for the marks/kinds round):
"sidetrackkkk" branch receipt · serves/invokes family question · cross-scheme
bridges (panproto lenses).

## Active package blocks

None. When a package opens: STANDING (frozen at open) + NOW (≤15-line
entries) live in `build/<package>/NOW.md`; add a one-line board entry under
Threads; prune it at close (/work-package skill governs the mechanics).
Precedence unchanged: thread files and this board NEVER outrank CONTRACT.md
or decisions.md — flag discrepancies inline, don't pause.
