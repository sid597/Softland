# space-as-entity — retro (rungs 1+2, closed 2026-07-26)

One-day package: staged, contracted, implemented in two phases (Codex),
one stop-clause cycle, gate-reviewed (Fable), G7 closed on Sid's headed
receipt — all twelve gates green. Code at `43a57a0` (rung 1) + `f7945fd`
(rung 2); docs trail committed alongside. Two independent falsification
passes found zero code defects; every deterministic implementer number
(suite counts, clamp values, revision ids) reproduced exactly under
independent re-run — the G2 cluster counts moved 6/29/22 → 7/33/28 for
the explained reason (the fm:space master + its G6 revisions now exist).

## QC-layer scorecard

- **Contract authoring (staging session)** — caught 2, missed 1 (shared).
  T1 (space-claim is a one-element vector; `conj` would nest and kill
  every space gesture) and T2 (verbatim floor bindings as master material
  are structurally impossible — the grammar refuses floor-reserved verbs)
  were both discovered by VERIFICATION at contract time, before any phase
  existed. Both would have been implementation bugs; T1's comment ships
  in the code. Miss: the Process section said "each phase runs its gates
  green" but never required the per-phase gate partition to sum to the
  contract's gate list — the G7 gap's root.
- **Phase P1 (Codex)** — clean. Manifest re-verified against disk before
  code; final suites green (no artifact records whether the first run was
  green — inference removed at recheck); the live G3 receipt captured a
  real decision off a real wheel event. Nothing in its work was faulted later. It chose
  gates G1/G2/G3/G10g — G7 (a rung-1 gate: the wheel-pick cost) was not
  claimed and nothing flagged the omission (miss shared with R1).
- **Stop clause (P2 pre-code)** — the package's best moment. Codex read
  the compiler before writing code, found that per-key validators
  physically cannot see a `min < max` pair, refused to improvise on a
  frozen allowlist, and escalated with citations + a recommendation.
  Zero implementation was wasted.
- **RULING R1 (Fable)** — caught 1, missed 1. The ruling's own
  verification found the SECOND validation site (`valid-material?` — the
  wear-time validator `resolved-wear` floors through): compile-only
  enforcement would have WORN a durable min≥max revision. That is a real
  would-be bug killed at ruling time. Miss: R1's P2 gate list
  (G4/G5/G6/G8/G9/G11/G12) was a derived enumeration that dropped G7 and
  was never sum-checked against the contract — exactly the
  ruling-execution-sweep class the skill already warns about for counts
  and gate lists.
- **Phase P2 (Codex)** — clean. Receipts server-read; hostile candidates
  left the cluster in the documented handoff state; revision ids
  content-addressed. (The only sourced first-run-green claim in the trail
  is the R1 seam specifically, per GATE.md.) Gate re-verification reproduced every
  number byte-for-byte (re-imports minted identical revision ids).
- **Falsification finder (one, fresh-context, per contract sizing)** —
  zero findings; agreed with the independent gate falsification pass.
  Nothing suggests it was aimed wrong; the batch-of-one sizing held.
- **Gate review (Fable, fresh session)** — one finding, process-class:
  G7 ran in NO phase. Everything else it did (fast lane + five isolated
  namespaces re-run, G2 receipt re-run, live drill/G3/G4/G6/G9/G11
  re-driven under fresh request ids) diverged from the build sessions'
  numbers exactly nowhere — consistent with the P7-gate ledger behind the
  slim-gate ruling; the wider run was justified here only because the
  package edits the dispatch kernel's floor tables and the material
  compiler.
- **The wearing (Sid, headed browser)** — G7's felt half doubled as the
  package's first real wear: typing through a sustained wheel burst over
  the dense area, echo p95 23.3 / max 40.3 vs the 52 bar, zero outliers;
  he drove zoom to the served 0.1 floor live (the 8.0 saturation receipt
  is the gate session's headless drive, not his). The receipt carries no
  adapter attestation — waived, with grounds, in GATE.md (the rule guards
  false FAILs; a degraded adapter cannot fake a green). The gate that no
  session on this box could run was closed by one console paste
  (`copy(__ground.report())`).

## What the next contract does differently (one rule, one failure)

- **Per-phase gate partitions sum-check against the contract's gate list
  at the moment the partition is made** (staging, a ruling, or a phase
  prompt), and any gate needing a specific environment or actor (G7:
  Sid's headed browser) is assigned an OWNER in the same breath. Grounds:
  G7 appeared in no phase list (P1's choice, then R1's enumeration),
  nothing summed the lists, and the gap surfaced only at gate review —
  after which the gate needed an act only Sid could perform, discovered
  at the worst time to discover it.

No other new rules — every other layer performed to spec, and inventing
rules from a clean run is the speculative hardening the skill forbids.

## Mechanisms that earned their keep

- **Stop clause + ruling verification**: the cheapest catch in the
  package (T9 gap before any code), and the ruling process itself caught
  the second enforcement site the recommendation missed.
- **Contract-time trap verification**: T1/T2 found by reading, not by
  debugging; trap numbers cited in shipped code comments.
- **Content-addressed revision ids**: the gate's independent G6 re-drive
  minted byte-identical revision ids under fresh request ids — two
  sessions provably drove the same durable objects.
- **Stable receipt commands** (`space-as-entity-receipt g2|g6-start|
  g6-finish`): any session can re-derive the served-truth receipts.
- **`__ground.report()` as a paste-able receipt**: closed a
  Sid-environment-only gate with one console command and one paste.

## Adversarial recheck (fresh context, 2026-07-26)

Ran before this retro fed any binding artifact. Verdict: scorecard
structure, the single lesson, the residue numbers, the git discipline
(no mixed commits, no Co-Authored-By anywhere in the package's seven
commits), and the suite state all held; three factual-precision
corrections were required and are applied in place above (the 8.0/0.1
wear misattribution; the overbroad "every number reproduced" claim; the
unevidenced "first-run green" claims). The recheck also re-ran the fast
lane fresh at committed HEAD `f7945fd`: 25 namespaces / 189 tests /
1,777 assertions, green — discharging the close protocol's
HEAD-dynamic-suite step alongside the code-atoms-test re-run.

## Residue (tracked, non-blocking)

- Two >100ms client stalls in the G7 receipt (144ms/283ms, 09:29:36/39Z),
  zero echo outliers coincident — first place to look if a hitch is ever
  felt; not attributed to this package.
- The per-event pick costs ~4.5ms at 189 blocks (dev build, JS-side) and
  is paid by hover moves and now wheels. Under the 52 bar with a wide
  margin today; at 5–10× block count it becomes the budget's biggest
  line. Named trigger: echo outliers clustering at move/zoom moments →
  T6 pick-per-burst (pre-approved for the wheel) and/or hover throttle.
- Adapter-attestation asymmetry (noted in GATE.md): the attestation rule
  guards false FAILs; a green echo receipt is robust to a degraded
  adapter. Single benign instance — a candidate skill note only if it
  recurs.
- **Carry into rung 3's contract**: the fence's three lanes (fm:space
  grammar, console seam, served-instance extraction) are EXISTING
  invariants to preserve — the console seam already reads the predicate
  pre-lift, so the G10 lift cannot open a console-only capture window.
  Rung 3 cites them as standing law, not deliverables. G5's
  three-call-site census updates to whatever the lift adds, in the same
  change.
