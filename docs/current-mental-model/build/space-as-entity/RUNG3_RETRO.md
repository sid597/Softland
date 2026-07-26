# space-as-entity — rung 3 retro (the G10 lift, closed 2026-07-26)

One-day rung riding a closed base: staged and contracted in one Fable
session (three traps found by disk verification before any phase existed),
implemented in ONE phase (Codex, all eight gates green in-context), one
fresh-context falsification finder (three real counterexamples, all
repaired pre-gate), Fable slim gate PASS, zero stop clauses. Code at
`ecbd572`; docs staged `b87f5a7`, gate `c598f86`. HEAD-dynamic suites
green at committed HEAD — banked by the adversarial recheck's independent
re-run below (code-atoms 9/198 · git-spine 7/167 · git-spine-gate 1/10 ·
focused 24/400); the close session's own prior run of the same four left
no artifact trail, so the recheck's run is the receipt.

## QC-layer scorecard

- **Contract authoring (Fable staging)** — caught 3, missed 1 (shared).
  T-R1 (the keyword-vs-string subject schism — a naive lift serves a
  deviation that silently never fires), T-R2 (the clamp reads the shared
  tier — a space deviation worn but never FELT), and T-R3 (a wholesale
  site-lift mints dead durable rows) were all found by reading the code at
  contract time; each names a SILENT-wrong failure, the class no suite
  catches after the fact. The fence's write-lane inheritance (T-R4) was
  verified pre-phase, converting a would-be fourth call site into a proof
  obligation (R3-G5a). First application of the gate-partition sum-check
  rule (minted at the rungs-1+2 close): P1 = all eight gates, zero
  remainder, no actor-bound gate staged — and the review needed none.
  Shared miss: R3-G1's precondition ("NO space instance anywhere") is
  destroyed forever by the package's own G3 — the durable instance master
  is append-only; release = holds/inherit — and nothing in the contract
  marked the precondition one-shot, so P1 encoded it literally into the
  committed harness (the g1 staleness below; rule 2).

- **Phase P1 (Codex)** — strong; one miss. The manifest was re-verified
  against disk with drift banked before code (two items, both benign) and
  pre-code SHA-256s taken for three of the six read-only files — new this
  rung; the other three rested on `git diff --numstat` vs HEAD, equally
  byte-grade. Five failure clusters retained in the artifact (the fixture starting from the
  bindings-less v0 grammar; the helper's nested-map bug; the import
  asymmetry; the headless GPU shape; the finder's three rounds). The
  judgment-call handling on the G3 repeat-import conflict was the model
  path: report both facts, interpret neither, flag for Fable — the
  package's one near-fork routed as neither an escalation nor a silent
  pick. Miss: the `src-dev/dev.cljc` LAND_PINNED guard rode outside the
  contract allowlist, unflagged in the artifact's file list — caught only
  by the gate reading the full diff (rule 1 below).

- **Falsification finder (one, fresh-context, aimed per the machine-cut
  sizing rule)** — three REAL counterexamples, all in the genuinely-new
  machinery it was aimed at: day-one wheel cache misses (a per-event work
  class the read plan forbids), a facet-only durable bridge letting a
  foreign space subject hijack THE space, and a first-round cache repair
  that could mask a non-space-facet instance on subject `"space"`. All
  repaired and counter-pinned before the gate, which found the repairs
  sound and nothing behind them. The batch-of-one sizing is now 2-for-2
  on the skill's own terms: zero findings at rungs 1+2 (that retro banked
  no explanation beyond "nothing suggests it was aimed wrong"), three
  here. A thin-vs-thick-machinery reading of the difference is a
  hypothesis this retro proposes, not a finding either package banked.

- **Gate review (Fable, slim per the sizing ruling)** — every
  deterministic number reproduced (suite counts, censuses, clamp values;
  revision-id equality proven within the gate's own re-drive — the gate
  banked no id string, so cross-session equality with P1's ids is
  inferred from the frozen revision count, not shown); the two that
  moved, moved for explained reasons (shadow compile 3→0: the served
  client was already byte-current at gate; the instance-history pointer
  census 9→13: +4 is exactly one full G3+G4 cycle — deviate/release/pin/
  unpin each append one instance pointer, the shared activation and
  rollback do not, revisions frozen at 4 by content-addressing — but
  WHICH session minted them is not determinable from the artifacts: the
  gate records no request ids or run order for its re-drives, and P1
  re-drove G3/G4 after the finder repairs; structurally explained,
  attribution unaudited). All four findings are non-blocking and none
  faults the delivered code — one confirmed judgment call over a
  pre-existing OC asymmetry (falsifier named), one process (allowlist
  drift), one ops (dead handoff dev app), one harness (g1 staleness) —
  consistent with the layers below having done their jobs. The G1 re-run
  judgment (re-prove every behavioral check
  under the strictly HARDER precondition — released instance rather than
  no instance) is the right template for world-moved preconditions.

- **Stop clause** — never fired, correctly. The one candidate (the
  import-fingerprint conflict) is a pre-existing asymmetry in read-only OC
  code, not a contract conflict; the flag-for-confirmation channel handled
  it at a fraction of an escalation's cost.

- **The wearing** — machine half only, by contract design:
  trusted-gesture browser drives (CDP wheel; attestation FIRST per the
  scene-substrate rule — adapter non-fallback, named in the gate) felt the
  deviation at exactly 3.0, the pin at 8.0-over-2.0, the unpin at 2.0,
  restoration at 8.0/0.1. No Sid-bound gate was staged — the contract's
  owner ruling (no new per-event work class) held at review. The space's
  first REAL deviation is consumer 2's payoff and lands with Sid's return
  wear; nothing blocks on it.

## What the next contract does differently (two rules, one failure each)

- **The phase artifact's changed-file list is diff-derived and sum-checked
  against the contract allowlist at artifact-writing time** —
  `git diff --name-only` (plus status for new files) at phase end, every
  path classified allowlist / new-per-contract / DRIFT-flagged; never
  reconstructed from memory. Grounds: P1's artifact said "seven
  deliverables, exactly" while the tree carried an eighth item outside
  them (`src-dev/dev.cljc`'s LAND_PINNED guard — a legitimate harness
  need, default path behaviorally the old code, re-indented only); no
  phase layer flagged it; the gate
  found it by reading the full diff, and it reached Sid's commit decision
  as a gate finding instead of an implementer disclosure.
- **A gate precondition the package itself destroys is marked ONE-SHOT in
  the contract, and its committed harness asserts the post-package
  invariant instead.** Grounds: R3-G1's "no space instance anywhere" ended
  forever at G3 (the durable master is append-only; release =
  holds/inherit); the committed harness's `:no-space-instance?` check is
  now permanently unsatisfiable, and the gate had to re-derive the
  strictly-harder released-instance claim by hand.

No other new rules — every other layer performed to spec; more would be
the speculative hardening the skill forbids.

## Mechanisms that earned their keep

- **Pre-code SHA-256 banking of read-only files** (P1, new this rung) +
  identical hashes re-derived at G6: byte-grade proof for the three files
  it covered (`git diff --numstat` vs HEAD carried the other three,
  equally byte-grade). Keep for every contract with a READ-ONLY list.
- **The traps ledger**, again: all six T-R comments at their sites;
  T-R1/T-R2 name silent-wrong failures the naive lift ships with no error
  card anywhere.
- **The gate-partition sum-check rule** (first application since minting):
  eight gates, one phase, zero remainder — the failure class it was minted
  for did not recur.
- **Content-addressed revision ids**: each session's re-import minted the
  byte-identical id under a fresh request id (proven inside P1's receipt
  and inside the gate's drive; cross-session id equality is inferred from
  the frozen revision count, not shown) — third package running.
- **The committed receipt harness**
  (`test/app/space_as_entity_rung3_receipt.clj`, seven entry points): the
  four server-read gates (G1/G3/G4/G5) re-runnable by any session — there
  the gate's re-drives were invocations, not re-derivations. The
  console/browser half (G2, G7, every felt receipt) rode the gate's own
  uncommitted harness: re-derived, not re-invoked.
- **The judgment-flag channel**: the implementer reports both facts +
  flags, Fable confirms at gate — the correct middle band between
  stop-clause escalation and silent interpretation.
- **LAND_PINNED itself** (the drift, kept on merit): a pinned-serve boot
  so concurrent sessions editing the tree cannot hot-swap a live
  gate/wear session's client — the process failure was the missing flag,
  not the guard.

## Adversarial recheck (fresh context, 2026-07-26)

Ran before this retro fed any binding artifact. Verdict: CORRECTIONS
REQUIRED — two substantive and six precision corrections, all applied in
place above. Substantive: (1) the committed harness covers only the four
server-read gates — G2/G7 and every felt receipt rode the gate's own
uncommitted browser harness (re-derived, not re-invoked); (2) the 9→13
pointer delta is structurally exactly one G3+G4 cycle, but which session
minted it is unauditable — the gate banked no request ids or run order.
Precision: the thin-vs-thick finder story marked as this retro's
hypothesis; the gate findings' class label corrected; "eighth item", not
"eighth edit"; SHA-256s covered three of six read-only files; "verbatim"
overclaim dropped; cross-session revision-id equality marked inferred.
Everything else held under citation: the three contract traps, the two
drift items, the finder's three counterexamples, the four gate findings,
the sum-check-rule application (first use confirmed by commit order), the
felt values, both censuses re-derived at HEAD (one defn + exactly three
reads each, no fourth fence site), all six T-R comments at their sites
with the subject bridge at exactly two client sites, the residue wording,
and git discipline (three commits, code/docs never mixed, no
Co-Authored-By anywhere). The recheck re-ran all four HEAD-dynamic suites
fresh at committed HEAD `ecbd572` — code-atoms 9/198 · git-spine 7/167 ·
git-spine-gate 1/10 · focused 24/400, all 0 failures / 0 errors —
independently discharging the close protocol's HEAD-dynamic step.

## Residue (tracked, non-blocking)

- **OC bootstrap-vs-normal import asymmetry** (pre-existing; surfaced at
  G3, confirmed at gate): a bootstrap import carries its active pointer; a
  later NORMAL import of the same source under the same import key
  computes a different material fingerprint →
  `:idempotency/material-fingerprint-conflict` — while the
  content-addressed revision id stays byte-identical and fresh activations
  land. Cheap falsifier, staged for whenever an OC package next opens
  (carry into that contract per the skill's residue rule): one OC test
  asserting a normal re-import of a bootstrap-imported source REPLAYS
  instead of conflicting.
- **g1 harness one-line fix**:
  `test/app/space_as_entity_rung3_receipt.clj:139`
  `:no-space-instance? (nil? (:instance s))` — permanently unsatisfiable
  (rule 2 above). Fix with the next code commit that touches the harness:
  assert released/inherit (instance master present, nothing worn) instead.
  Not landed at close — the close carries no code commit; Sid's commit
  word was spent on `ecbd572`.
- **Consumer 2's felt payoff** (the space's first real deviation/pin,
  Sid's hand) rides the return wear; proven headless, nothing staged on
  it.
