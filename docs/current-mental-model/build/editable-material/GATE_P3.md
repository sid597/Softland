# P3 gate — the facet spread (worn five) — PASS

2026-07-25 · Fable orchestration session (the same context that gated P1/P2
and adjudicated the facet-2 halt; Codex authored all implementation
elsewhere — this context judged, never wrote). Package = facets 2–6 + the
generic facet-master layer, gated in two stages: the facet-2 halt
(adjudicated 07-24, NOW.md) and this P3-END gate. Code commit `13f1dac`
(21 files, +2,415/−748; the provenance adapter recorded as a 59% rename to
`facet_master.clj`).

## Independent receipts (this session)

- Full suite: 387 tests / 5,329 assertions; red ONLY in the two known
  environment/flake classes, both resolved per the registry protocol:
  `ingest-watchers-test` errored on `System map startup failed` (the
  registered IPC port-conflict class; standalone green 2t/15a) and
  `dogfood-llm-probe-test` (registered flake; failed once standalone under
  sustained load, then TWO consecutive standalone greens 67/67 — protocol:
  two consecutive standalone failures = real; observed one). Codex's own
  fresh-context final run was fully green (387t/5,339a/0/0).
- CLJS `:dev` compile: 268 files, **0 warnings** (matches Codex's fresh
  standalone receipt).
- HEAD-reading suites re-run AFTER the commit moved HEAD: 16t/365a green.
- Render receipt (Codex, accepted): the controlled same-process whole-PNG
  byte-proof the halt demanded — baseline and final byte-identical
  (SHA-256 `c0517233…`) at pinned camera/viewport with all seven blocks
  geometry-exact, GPU attestation Radeon RX 7900 XTX / Vulkan (the
  environment-attestation rule). Cross-process AA variance remains
  disclosed as a capture-process fact, not laundered.
- Echo (Codex, accepted): post-repair samples 27.2ms / 17.8ms vs the
  unchanged 52ms bar — honestly presented as two samples, not a
  distribution. Non-blocking residue: a broader latency characterization
  rides any future echo-gate complaint.

## Falsification pass — held

- **Four resumed facets are textbook wearers**: thin specs (52–90 lines)
  over the generic layer; exact worn values pinned (fold defaults + the
  byte-pinned header vocabulary; reply-gap 34.0 + fallback {60,60};
  reach 3.0; wrap floor 32 / fallback 80); validators total and
  closed-key-set. The hard-coded anchor branch became declarative
  `:positioned/anchor-order` data over a closed rule set — the P5
  bindings law's spirit, arrived early and legally (data, not code refs).
- **Old literals provably deleted**: `reply-gap`, `fold-default`, the
  header strings, wrap floor/fallback — grep-verified; the only surviving
  occurrences are material consumption at the render boundary.
- **The activation-boundary repair (the package's real find)**: verified
  at all three sites. Material-derived positions are LABELED at write time
  (`:placement-derived?`) and re-derive under current wear on the next
  reconcile; settled cells and live human placements always win; the
  cached wrap input is the source-width MEASUREMENT
  (`:wrap-source-columns`), never the material-derived result; the epoch
  watch runs `reconcile!` BEFORE stamping, so served results and stamped
  revisions move together. The write → render → truth-reconciliation →
  clear lifecycle is closed. The live receipts witness it: gap 134 moved
  an unsettled reply by exactly +100 while a settled block stayed
  byte-exact; rollback exact.
- **Boundary consumption verified**: `fold-header-line` builds from wear's
  header-copy (per-appearance toggles stay ephemeral code);
  `default-position` walks the anchor-order data; `adoptive-thread-for`
  reads current reach at real send time; `reply-wrap-col` is ONE rule
  shared by settled render and provisional stream.
- **Composition vocabulary unchanged** — the four new boundaries occupy
  distinct slots (`:block/fold-header-text`, placement, thread-adoption,
  content-flow), so no new collision demanded vocabulary; the registry
  remains a serve registry, not a recipe (masters ≠ kinds held).
- **Settle-cell sovereignty (the DIRECTION tripwire)**: positioned wear
  claims only pre-cell defaults; settle cells remain per-instance durable
  truth with no master claim — the `!world` bundle unfuses at the touched
  seam only, never in bulk.

## Adjudications

- **The two red namespaces**: both the known environment classes, both
  standalone-green; tests-green SATISFIED (Codex's fresh run fully green;
  my reds are load artifacts of a session running cluster + suites + gates
  concurrently for many hours).
- **Echo two-sample receipt**: accepted for the registered per-echo stop
  (neither sample near the bar); explicitly NOT accepted as a latency
  distribution — recorded, non-blocking.
- **Fresh-review layer inside Codex's run**: the adversarial package review
  that BLOCKED on the stale-derived-state defect and re-reviewed the
  repair earned its keep — this is the QC model working inside the
  implementer's own lane. The gate independently re-verified the repair
  rather than trusting the reviewer's PASS.

## Open doubts (non-blocking, falsifiers named)

- **Reconcile-storm cost**: every material change now reconciles the full
  context (correct for truth; unmeasured for cost at large N). Falsifier:
  the echo bar + the standing narrow-invalidation board item — if the
  52ms gate ever complains after an activation, this is the first suspect.
- **Foldable header vocabulary as strings**: header-copy is free-form
  strings compiled total — a hostile-but-valid revision can make headers
  unreadable (policy freedom, not a defect; the error card only guards
  malformed). The immune answer is Gate 4's record + rollback, already in
  the ladder.
- Carried from earlier gates: cross-JVM activation epoch staleness (P6
  review checks the write path) · trail classification switches to event
  truth at P6 · arsenal-roster name shadowing (avoid reserved names until
  P7's collision assertion).

## Campaign state after this gate

P1, P2, P3 CLOSED (commits `83aa5dd`+`b6f3d75`, `c6bb5d8`, `13f1dac`).
P4 running in its worktree (`Softland-p4`, branched at `5c0eaa2` — its
gate session rebases/reconciles onto `13f1dac`). P5 is sendable (needs
P3 ✓). P6 still requires the Fable CONTRACT_P6.md before its prompt goes
out. The P1 wound trace + P3's retained activation/rollback/malformed
candidates are Gate 4's seed evidence.
