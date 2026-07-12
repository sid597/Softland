# machine-cut INT — integration + receipt + wearing (gates G13–G16 + serial suite)

2026-07-12 · Fable orchestrating session (`machine-cut-contract` — same session
as contract + countersign + dispatch; lanes A/B ran as parallel Opus 4.8
subagents, both GREEN first-run: `LANE_A.md` / `LANE_B.md`). Binding:
CONTRACT v1 (countersigned 2026-07-12).

## INT edits (the orchestrating session's own, per LANES §INT)

1. **`:pairs-with` enum line** — landed BEFORE lane dispatch (isolated code
   commit `f864c74`; ns-load + membership verified live). Both lanes used the
   kind directly; no indirection was ever built.
2. **`file_viewer.cljc` boot attach** (CONTRACT §6): `rk-rt` =
   `rk/start-relation-runtime!` inside the face-projection-runtime delay —
   its OWN in-process cluster (an ipc-accepting rk arity would be a second
   kernel-file edit = §11 stop-clause; separate cluster is the trail-runtime
   precedent). TOTAL per MC-T12: failure → nil → honest `:structure :none`.
   Machine-cut **WAL replay in a future** (arsenal-replay precedent) +
   epoch bump when it lands edges (MC-T6 at replay). `face-ctx` gains
   `:rk-rt`.
3. **Constant re-home** (LANES §INT item 3): `face_projection.clj`'s
   `machine-cut-actor-v0` now aliases `machine-cut/machine-cut-actor-id` —
   one value, two call sites, drift impossible (T18 shape).
4. **INT-3 observed-model plumb** (lane A flag): the model rides the
   `:claude/system-init` observation's `:raw/json` (llm.clj:1929-1949);
   token-usage read kept as fallback; both absent → "unknown" (honest
   recording, not unplumbed control).

## Serial suite (wave-end, ONE JVM, post-INT)

All 12 namespaces (7 `face_*` + `machine-cut-test` + `machine-cut-serve-test`
+ `relation-kernel-test` + `dogfood-llm-test` + `clojure-adapter-test`),
serial, one JVM:

**`Ran 90 tests containing 1431 assertions. 0 failures, 0 errors.`**

(Teardown-time `LeaderNotFoundException` ERROR log lines are cluster-close
noise, not failures — the summary line is the authority.)

## G13 — the receipt (real corpus, real Claude, run ONCE and kept)

Artifact: `RECEIPT_G13.edn` (harness: fresh JVM, full harvest + distill of
the real `7c80e2a`-family corpus → river 247; rk + llm runtimes; ONE live
`claude -p` run, subscription).

**VERDICT: PASS — all 10 assertions green, asserted against durable state:**
run completed · input-hash stable across independent rebuild · WAL completed
line present · disposition totality (every shown event exactly once) · every
edge endpoint ∈ the shown set · rk edge count == WAL response memberships ·
WAL replay-equivalence (relation-ids reconstruct identically) · projection
serves `:machine-cut` structure · served pairs == WAL pairs · serve totality
(pairs+unpaired cover all served turns exactly once).

The numbers, honestly framed (coverage-verb law — window, never
conversation): **window = 64 blocks / 35 events of 247 river events
(truncated? true)**. The LLM classified all 35 events: **1 pair (1 human
prompt + 34 machine responses), 0 unpaired, 0 rejections of any class**.
Edges: 34 desired = 34 asserted = 34 in rk. Run: `claude-fable-5` (the CLI's
configured default, observed not controlled), 67.9s, $0.37, run-id in every
edge note. Structure-line served: `"machine-cut v1 · 1 pairs · 0 unpaired"`.

**Mechanical-baseline comparison (gate-side comparator only, MC-T13):
agreement 100.0%, 0 disagreements, mechanical-pairs 1.** Honest reading for
the kinds round + D-006: this window is STRUCTURALLY TRIVIAL for pairing —
one human prompt opens it and everything after is machine material, so
adjacency and the LLM coincide; the window exercised the loop's correctness,
not the annotator's judgment. The discriminating material (multi-prompt
windows, interruptions, the "sidetrackkkk" class) sits beyond page 1 —
reaching it rides the paging extension (block-kernel §10, D-001-gated).
Recorded, not argued from.

## G14 — the wearing: NOT COMPLETED (honest close, Sid's word 2026-07-12)

The FIRST wearing attempt earned its keep before being cut short: the live
boot caught the **concurrent-cluster worker-registry collision** (a fresh rk
cluster racing the face OC's in-flight module launch → chronic
transcript-ops worker failure) — fixed by reusing the TRAIL cluster's
relation kernel (one edge store beside the git-spine edges) + the
`machine-cut-ctx` accessor. The re-run against the fixed boot was in flight
when Sid closed the session on cost ("just do the commit close the handoff")
— **G14 remains the one open gate**: pair frames + silver structure-line +
epoch re-render live, driver at scratchpad `wearing_g14.js`, method = the
W1/W2 precedent. The WAL (`data/machine-cut-log.ednl`, 1 completed line, 34
edges) replays at any dev boot, so the wearing needs only: boot the dev app,
`/face boxes-paired-face`, wait for distill+replay epochs.

## Final suite state (honest)

Post-fix serial suite: **93 tests / 1458 assertions / 6 fail / 0 error** —
all 6 fails in `dogfood-llm-test/stale-approval-on-executor-death-test`,
a timing-sensitive llm-module test that ran while the dev server booted on
the same box; **green in isolation immediately after (12/114/0/0)** and
green in the pre-fix serial run (90/1431/0/0). Machine-cut's own namespaces
including all `gate-fix-*` regressions: green. The quiet-box authoritative
re-run + the committed-HEAD re-run are the CLOSE session's step-2 duty.

## Gate verdict (Fable, orchestrating session)

**Wave PASS with two open items** — G1–G13 + G15/G16 green (G13 asserted on
the real corpus with a real run); the falsification batch's HIGH cluster
fixed at gate with biting regressions; open: **G14 wearing** (above) and the
**quiet-box serial re-run**. Both route to the close session. Open doubts
carried with named falsifiers in §falsification above. D-006 notes: the
traps ledger held (MC-T2's own contract sentence was the one that needed
amending — the ≥1 contract-text-error ledger confirmed again); cost signal
recorded verbatim: Sid flagged the wave's ~990k subagent spend; proposed
retro rule — the falsification batch defaults to ONE finder on the
genuinely-new machinery (falsifiers B/C found 0 HIGH; A found all 4).

## Falsification-by-class batch (3 fresh Opus subagents, default-fail)

Artifacts: `FALSIFY_A.md` (driver lifecycle) · `FALSIFY_B.md` (serve +
fixture drift) · `FALSIFY_C.md` (boot seams + fence). Headline: **A found a
real HIGH cluster (one root, three surfaces); B and C found no HIGH** — the
serve core and the fence held. All fixes were applied by the orchestrating
session at the gate (the W2 (e2) pattern), each with a biting regression in
`machine_cut_test.clj` (`gate-fix-*` deftests), suite re-green after.

**Fixed at gate (with the regression that pins each):**
- **A-F2/F3 (HIGH, CONFIRMED — the root):** the flat idempotency key
  `"mc:"+relation-id` was stable per-edge-FOREVER; the relation journal
  replays a duplicate key's prior decision, so assert→retract→re-assert
  left the edge `:retracted` (and WAL replay could not converge A→B→A).
  Fix: **transition-keyed idempotency** (`transition-request-id` — first
  assert keeps the plain key; transitions on an incumbent key on its
  `status-changed-at-ms`; deterministic from read state, no run-id, no wall
  clock). CONTRACT §4.3 amended in place (dated) — the pre-registered ≥1
  contract-text-error instance for this contract (§14 criterion-3 ding,
  honest ledger). Regressions: `gate-fix-transition-keys-test` +
  the A→B→A IPC flip in `gate-fix-ipc-regressions-test`.
- **A-F4 (HIGH, CONFIRMED):** every barrier result was discarded
  (`await-relation` returns the last value without throwing on deadline) —
  counts/epoch came from the PLAN. Fix: `materialized-to?` checks every
  transition; counts and the epoch decision use MATERIALIZED transitions
  only; a shortfall returns `:incomplete-writes`, never silent success.
- **A-F1 (HIGH, CONFIRMED):** `:noop-complete` trusted run-state; a crash
  between WAL write and edge appends froze wrong edges forever. Fix: the
  noop path **verifies-and-converges from the run's own WAL line** (zero
  adapter calls); succeeded-run-with-no-WAL returns honest `:stale-no-wal`.
  Regression: the manual-divergence noop-converge IPC test.
- **A-F6 (MED):** replay reconciled address-wide vs the live path's window
  scope → replay could retract out-of-window edges. Fix:
  `reconcile-from-wal-line!` scopes to the LINE's own window (§5.5 amended).
  Regression: the out-of-window-edge-survives IPC test.
- **A-F7 (MED):** `run-one-pending-*` executes the lowest-sorted pending (a
  stale run under concurrency), and a bundle-hash mismatch THREW. Fix:
  **targeted `claim-run!` of our run-id**; mismatch → honest `:failed
  :bundle-hash-mismatch` (short-circuit, no terminal await). Regression:
  the shifting-loader IPC test.
- **A-F9 (MED):** the terminal await used the 2s default → a slow fold
  recorded an honest run as `:failed`. Fix: bounded await
  `max(10s, run timeout)`.
- **A-F5 + A-F11 (MED/LOW):** empty-`:responses` pairs counted as pairs
  (zero edges — counts lied); non-sequential `:responses` char-seq'd into
  N unknown-ids. Fix: shape checks + `:empty-pairs`/`:malformed-pairs`
  counts. Regression: `gate-fix-validation-shapes-test`.
- **C-MC-C1 (MED, CONFIRMED):** LANE_A.md carried a raw NUL byte (file(1) =
  data). Fixed (escape text); file(1) now text.
- **C-MC-C2 + B-F5 (MED/LOW — and the wearing's live catch):** no lawful
  accessor for the live rk handle → a live annotate could mint a SECOND rk
  cluster nothing serves; the first G14 boot also hit the concurrent-
  cluster worker-registry collision live (chronic transcript-ops launch
  failure). Fix: the face boot now REUSES the trail cluster's relation
  kernel (one edge store beside the git-spine edges — one land, one edge
  truth; deref-first also sequences the boots) + `machine-cut-ctx` exported
  as the lawful live-annotate handle. C-MC-C4/C6 dissolved by the same fix.

**Accepted with falsifiers named (open doubts, non-blocking):**
- **A-F8:** a transiently-failed run permanently noops its identity —
  honest `:retry-with-salt` hint returned; escape = explicit salt (one
  deliberate paid re-run). Falsifier: a timeout during a real annotate.
- **A-F10:** boot WAL-replay racing a concurrent live REPL annotate on the
  same relation-id is order-dependent. v0 exposure ≈ nil (replay is
  seconds; annotation is human-triggered). Falsifier: scripted concurrent
  replay+annotate.
- **C-MC-C3:** replay's per-line catch is `Exception`-scoped; a mid-replay
  `Error` after the distill epoch fired strands landed edges unsignaled
  until the next epoch. Falsifier: fault-injected Error mid-replay.
- **C-MC-C5:** the read-only projection compile-depends on the llm module
  for one constant (the T18 one-def-two-sites trade, recorded); G15's form
  scan would not catch a future WRITE call through that require — the scan
  is complete only while the require stays constant-only.
- **B-F1/F2 (inherited W1 class):** fixture block-`:kind` / `:speaker`
  vocabularies differ from live river vocabulary — cosmetic (colors/labels),
  OUT of the pair path, inherited from the W1 fixtures. Routed to the
  harmonization/marks-round residue, not this package.
- **B-F3:** structure-count scopes documented in-code (window vs
  conversation diagnostics; they coincide in v0). **B-F4:** chain/self-loop
  edges from a hypothetical non-driver asserter drop silently — driver
  validation prevents its own; falsifier: hand-seeded chain edge.
