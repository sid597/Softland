# RETRO — git-spine WP2 (+ /assert write shim) · light, at close 2026-07-05

Package: CONTRACT v1.2 (v1 → R1 FAIL → v1.1 → R2 FAIL → v1.2; round 3 waived
under the time-box). Phases P1+P2 (spine), P3 (names), PW (route) built by
fresh-context subagents inside the 07-05 HQ marathon; batched
diff-falsification (`DIFF_FALSIFICATION_R1.md`); Fable gate with fixes applied
in-review (`GATE_REVIEW.md`). Closed under the delivery-mode ruling
(decisions.md D-006 notes 2026-07-05).

## What the package delivered
- Commits as objects (md-path reuse, byte-stable §3.A canonical text),
  parent `:based-on` edges, transcript→commit/doc `:produced` edges
  (repo-verified shas only), display names (feed basename/sha7 +
  View-3 `<sha7> · <subject>`), and the land's first write affordance
  (`POST /api/relation/assert` + write-ahead log + boot replay).
- Reviewer gates beyond the contract: G8 pair (route line → fresh cluster,
  identical relation-id — killed the §3.C serialization-drift risk
  empirically) and G11 (commit+session in one View-3 context), which caught
  a REAL bug (edge-line dropped the far endpoint — map-lie class).

## What worked
- **Two independent implementations of one serialization seam + a pair test**
  (SF-R2.1): the drift risk was named at contract time, assigned a gate, and
  the gate was cheap to write. Keep this pattern for any cross-builder file
  format.
- **Contract validation rounds earned their cost twice** (R1
  invisible-edges, R2 impossible-reader — both Fable-authored errors caught
  at text time, before any code). The delivery-mode ruling consolidates QC to
  the end of a wave; these two catches are the class the end-gate must be
  sized for (honest ledger, per the ruling).
- **Batched falsification found all real defects** (6 should-fixes, 0
  blockers) and the batched gate could fix them directly in one sitting —
  the delivery-mode cadence (code → one test batch → one falsification+gate)
  held for a 4-package wave.

## What to carry forward
1. **The route needed the import path's own trap ledger.** Traps 4 (stable
   idempotency keys) and B1 (bare-sha targets don't join) were both solved on
   the import path and both re-appeared on the manual path. When a contract
   hardens one path against a trap, every OTHER path writing the same
   substrate must cite that trap explicitly (contract checklist item).
2. **Validate at the outermost gate what the innermost gate will reject.**
   The depot rejects nil actors — but after `:append-ack`, so the route 200'd
   and the write-ahead log kept a poison line. Rule of thumb: a
   write-affordance route must pre-validate every depot shape-error it can
   compute (they all are computable — `request-shape-errors` is pure).
3. **Per-item isolation in replay/sweep loops is not optional.** A torn last
   line (crash mid-append) was guaranteed-by-construction eventually; the
   unwrapped reduce would have silently degraded durability forever.
4. **`clip?` replace-vs-intersect** (R-1's finding, fixed at this gate):
   walk-layer semantics that force builders into either/or paint bugs are
   root causes, not paint bugs. Fix the walk, not the caller.

## Deviations recorded
- Round-3 contract validation waived (time-box; residual risk landed in
  duty §8.7 + G8 — both held).
- `rect_tree.cljc` touched at gate, outside R-1's allowlist (ruled in
  GATE_REVIEW.md "Contract notes"; behavior-identical for all existing call
  sites).
- G9's subject-in-feed-title remains the recorded follow-up (bundle rendering
  shows it; feed rows carry sha7 only).

## Open doubts carried (GATE_REVIEW.md "Open doubts")
Doc-edge path-string join across the dual working dirs (count danglers after
first boot); unbounded assert-log growth; optimistic-200 residual;
`:transcript-file-updated` fixture parity never live-exercised.
