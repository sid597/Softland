# space-as-entity — Fable gate verdict (rungs 1+2, contract gates G1–G12)

2026-07-26 · Fable gate session · **PASS on all eleven reviewable gates;
G7 OPEN** — the package does not CLOSE until G7's felt half lands (one
glance at the echo bar during Sid's first headed zoom; T6's pick-per-burst
fallback is pre-approved if it reddens; still red → stop clause).

Inputs used as prior-pass records, never authority: `P1.md`, `P2.md`, the
NOW log. The code was read in full (both new files, every product diff
hunk, the dispatch/pick/wear regions of `ground.cljs`, the whole of
`binding_material.cljc` and `facet_material.cljc`).

## Independently re-run this session (never the implementer's word)

- **Fail-closed fast lane**: 25 namespaces / 189 tests / 1,777 assertions,
  0 failures 0 errors; one shared cluster; flake registry unchanged.
- **Five isolated affected namespaces** (not in the fast lane; run
  sequentially, one JVM each): material-truth 14/216 · provenance-material
  5/165 · material-circulation 8/61 · material-portal 16/284 ·
  face-projection 11/103 — all green.
- **G2 receipt, live-cluster server read**: `:green` — 7 masters / 33
  revisions / 28 binding rows scanned; zero active wheel rows at block
  sites; zero flagged residue; history complete. Counts moved 6/29/22 →
  7/33/28 from P1's read, exactly the fm:space master + its G6 revisions.
- **Live drive, headless Chrome 150 on the running dev app** (dev JVM
  booted fresh from the P2 working tree):
  - Drill: 12 probes, all `:claimed`, suite and live agreeing; tap-empty +
    shift-marquee `:master`; pan + both wheel probes `:floor`; probe 12 =
    `:camera/zoom-at-pointer` / `:floor` / facet `:space` at depth 1.
  - **G9**: client `table()` vs server `served()` — identical fm:space
    rows (6 = 6, field-wise deep equal); both label the floor
    `code-floor:fm:space:v0`.
  - **G4/G11 live**: hostile naked-drag candidate and garbage source both
    `refused / candidate-invalid` with error cards; `previewing? false`
    both times; drill still `pass`.
  - **G3 live**: real block at claim-chain depth 2 under the pointer;
    trusted wheel zoomed 1 → 4.22 → saturated at exactly 8.0; wheel-out
    clamped at exactly 0.1 (both served bounds felt live).
  - **G6 full cycle re-driven under FRESH gate-labeled request ids**
    (`fable-gate-*` — activations are new journal decisions, not replays):
    activate `zoom-max 2.0` → server wear [0.1 2.0] → trusted wheel
    saturates at exactly 2.0 → rollback to base → after reload wheel
    saturates at exactly 8.0. Malformed `min 5.0 / max 4.0` activation
    refused with exactly `[:space/zoom-clamp-invalid]` under a fresh
    request id; base stayed active; candidate retained inactive.
    Re-import minted byte-identical revision ids to Codex's banked ones —
    deterministic content-addressed identity confirmed.
- **Grep gates**: T7 (`space-claim` has exactly one product consumer, the
  chain builder) · G5 (one predicate defn; exactly three product call
  sites: fm:space grammar, console seam, served-instance lane) ·
  `verb_registry.cljc` zero diff · `instance-legal-sites` unchanged ·
  frozen v1/v2 validators unchanged · no new state atom · escape-detector
  membership present and the paths set is genuinely consumed ·
  `git diff --check` clean.

## Falsification pass — what was attacked, what held

- **Fence completeness**: grammar-legal modifier values are exactly
  `:any`/`#{}`/`#{:shift}` (`valid-row?` refuses `nil` and anything else),
  and the fence's 4-tuple set covers both naked spellings of both camera
  gestures; `matches?` needs exact set equality, so a `#{:shift}` row
  cannot capture a naked gesture. Cross-facet capture at the space rung is
  structurally impossible: `candidates-at` only consults rows filed under
  the claim's own facet, and the space claim names `[:space]`.
- **T9 both sites**: `compile-form` and `valid-material?` read the same
  `:form-validators`; entry-less declarations are byte-identical no-ops
  (`every?` over nil); `instance-grammars` passes the entry through, so
  instance masters inherit the invariant; the wear-time projection under
  instance grammars includes the three extra keys and the space predicate
  ignores them (total over nil/garbage — `get` before `map?` is nil-safe).
  The pinned and deviation branches of `wear-for-subject` both re-validate
  through the parent spec, so no route wears a min≥max material.
- **T5**: the clamp read is one cache-hit lookup per wheel event through
  the existing `identical?`-keyed wears cache; it also respects the
  preview membrane (a previewed candidate's clamp is felt; a refused one
  never creates overlay state). No second atom, no parallel read path.
- **Label**: one source var (`binding-material/space-floor-master-id`
  = spec `:facet-master/code-floor-revision-id`; registry map now purely
  derived); convention matches the family (`code-floor:fm:<name>:v0`).
- **Floor tables**: the fm:space spec's v0 floor carries no bindings, so
  the kernel's `assoc` of the four camera-inclusive rows is the only
  answer for `:space` — verified in both product and test derivations.
- **`handle-wheel! 0` budget pin**: counts mechanism BRANCHES, not picks —
  still true post-rung-1; not a stale pin.
- No code defect found. Codex's independent fresh-context falsification
  also reported none.

## The finding — G7 was run by NO phase

P1 ran G1/G2/G3/G10g; RULING R1 routed P2 to G4/G5/G6/G8/G9/G11/G12. No
list contained G7; both phase artifacts are silent on it. This is the
ruling-execution-sweep class (a derived enumeration — the per-phase gate
partition — dropped a member and nothing sum-checked it against the
contract's gate list). Retro item: a phase gate partition must sum to the
contract's full gate list, checked at ruling time.

**Mechanism receipts banked this session** (environment attested FIRST:
headless Chrome, adapter architecture `swiftshader` — CPU raster, so the
52ms bar itself is unclassifiable here per the scene-substrate rule):
`pick-at` costs ~4.5ms/event JS-side at 189 live blocks (dev build) and is
~100% of `handle-wheel!`'s cost, identical over block vs empty ground.
Placement: `pointer-move!` already runs the same pick per idle move for
hover at the same event rate — the wheel pick joins an already-paid cost
class rather than minting a new one (this is the argument the felt bar
will hold; it is inference, not the gate's letter). The felt half needs
Sid's headed browser: zoom over a populated region with the echo bar
visible. Green → G7 closes and the package CLOSES. Red → T6
pick-per-burst (pre-approved), re-measure; still red → stop clause.

## D-006 evaluation notes

- Implementer (Codex) quality: high — zero code defects across two
  independent falsification passes; every claimed number and revision id
  reproduced exactly under independent re-run; receipts server-read;
  hostile candidates left the cluster in the documented handoff state.
- The stop clause worked as designed and cheaply: the P2 pre-code manifest
  caught the per-key/cross-field gap before any code, escalated with
  citations, and RULING R1 strengthened the recommendation (the
  `valid-material?` second site) — the seam then went green first run.
- The one process miss (G7 partition) is Fable-side (staging + R1 gate
  enumeration), not implementer-side.
- Gate sizing: this review ran wider than the SLIM default (suite re-runs
  + five isolated namespaces) because the package edits the dispatch
  kernel's floor tables and the material compiler — new-organ-adjacent.
  The live-drive half reproduced everything and diverged nowhere; the
  suite re-runs diverged nowhere. Consistent with the P7-gate ledger.

## Cluster/tree end state

Live cluster running, base `[0.1 8.0]` revision active, malformed
candidate inactive and queryable; my re-drive added two gate-labeled
activation events (`space-as-entity/FABLE-GATE-G6-*`) to the history by
design. Dev app and headless browser stopped. Code remains UNCOMMITTED on
the shared tree — the commit decision is Sid's; docs trail committed by
this session (docs-only, explicit paths).
