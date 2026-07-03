# Implementation Validation — POST-FIX (Session 2)

> **Phase 4 re-run** against `src/app/server/rama/dogfood/llm.clj` AFTER fix session 2
> (2026-06-11). Scope of the session (per `fix-prompts/SESSION-2-llm.md`): (a) request
> dedup + sticky terminals (L-01, L-03); (b) claim-proof on every observation +
> never-drop (prior-retro F1, L-04); (c) approval lifecycle (L-07, prior-retro F3);
> (d) claim/inbox partial-commit repair + executor recovery index (L-02, L-05).
> Deferred by the session prompt: L-11 (live streaming + stderr redaction), L-09
> (bounds/subindexing), L-13 partially (hygiene — the keypath + foreign-select-one
> items WERE fixed in passing), plus L-06 (two-phase cancel), L-08 (write
> amplification), L-10 (single-append stale marking), L-12 (wall-clock in fold),
> approval timeout scheduler.
>
> Verification evidence: pre-fix failing baseline (3 probes reproduced the documented
> failures verbatim), post-fix adversary probe matrix
> `test/app/server/rama/dogfood_llm_probe_test.clj` (67 assertions, 0 failures),
> existing `dogfood_llm_test.clj` (114 assertions, 0 failures), space + compute +
> foundations suites green (see Verdict section for counts).

## What changed (structural)

1. **Stream → microbatch** (`llm-track-topology`). Same move as Session 1's compute
   fix and for the same two root causes: at-least-once stream retry re-executing
   committed multi-hop writes (L-01's clobber vector), and partial commit across
   partitioner hops (L-02's grant/inbox-removal livelock). One microbatch attempt is
   a single cross-partition exactly-once transaction. **Documented deviation:** the
   spec's ~200 ms streaming feel degrades to microbatch cadence; PLAN.md preferred a
   stream topology with per-write idempotency. The minimal-correct-fix rule
   (session prompt) plus the proven compute shape won. Revisit if streaming latency
   becomes product-critical.
2. **Request dedup gate** — `core/decision-dedup-gate` + `core/with-request-fingerprint`
   keyed by run-id (the decision row is the dedup anchor; run ids are single-use).
   Identical replay → no writes; same-id-different-content → no writes, committed
   decision stands. Blank/nil run ids refused client-side AND filtered topology-side.
3. **Claim-proof on every observation** — envelope gains `:executor/id` +
   `:claim/token`; `fold-observation` rebuilt on `core/authorize-mutation`
   (auth → identity → terminal → sequence). A `:pending` row rejects ANY token.
   Rejections fold token-free bounded audit errors (`bounded-audit-field`).
4. **Never-drop** — `$$llm-dead-letters` (bounded, `core/bounded-dead-letter`
   values) for unknown-run observations AND controls, on independent dataflow
   branches (`anchor>`/`hook>` at the source). No run state invented.
5. **Sticky terminals** — terminal observation arms route through `close-run-row`;
   `cancel-run` and `resolve-approval`'s fail branch fence on `terminal-run-row?`;
   `drain-observation-buffer` discards buffered entries once terminal; authorized
   post-terminal redeliveries are ignored silently (no audit noise).
6. **Approval lifecycle** — `resolve-approval` requires an EXISTING `:pending`
   approval (unknown → `:approval/unknown` audit error; resolved/expired → recorded
   no-op, first resolution wins, no resurrection); native JSON-RPC id taken from the
   stored row, not caller payload; every terminal close (`run-finished`, failures,
   cancel, decline-cascade) expires leftover pending approvals (`:run-closed`).
7. **Diff-driven cross-key indexes** — `$$llm-approvals-pending` and
   `$$llm-item-by-id` maintained by diffing the run row across the fold
   (`pending-approval-additions`/`-removals`, `newly-materialized-items`): buffer-
   drained approvals/items now index (pre-fix S5 bug), replays diff to nothing
   (pre-fix resurrection bug).
8. **Executor recovery** — `$$llm-executor-active-runs` (grant adds, terminal fold
   removes, both atomic in the batch); `claim-state` decides ownership by token
   equality for any non-terminal status, so a restarted executor re-verifies its
   grant after `:running`/`:blocked` (pre-fix it read `:conflict-or-past`).
9. **Control replay guard** — duplicate control id → no-op fold (protects the
   unkeyed `:steers`/`:compactions` trail vectors against client re-appends).
10. **Hygiene fixed in passing** — 6 consecutive-keypath sites collapsed;
    `select-pstate-one` now wraps the built-in `foreign-select-one`; thread native
    ids are first-write-wins (no silent flip; divergence 12).
11. **Test-harness JVM** — `deps.edn` `:test` alias gains `-Xss16m`: Rama's
    topology-graph analysis is recursive and loading the llm + space modules in one
    JVM now exceeds the default thread stack. Compile-time only.

## Check matrix (re-run)

- **Redundant conditionals — PASS.** New `<<if`s are single-purpose guards.
- **Consecutive keypath — PASS** (was FAIL ×6; all collapsed to `(keypath a b)`).
- **Select-compute-transform — FAIL (deferred L-08).** Whole-row fold + full-value
  derived writes per observation unchanged; requires the L-09 schema restructure.
- **Unnecessary nil->val — PASS** (vacuous).
- **:allow-yield? — PASS** (vacuous; no subindexed reads yet).
- **Non-subindexed collections without size limits — FAIL (deferred L-09).**
  Newly bounded: `:obs-buffer` (1024, overflow auditable), `$$llm-dead-letters`
  (100/run), `:observation-errors` (50, was already). Everything else
  (items/raw/tool-calls/thread histories/rollups/pending lanes) still unbounded
  `{String Object}`.
- **Stream topology idempotency — PASS.** Microbatch is exactly-once for Rama
  retries; client-level duplicates covered by: dedup gate (requests), token-equality
  no-op grants (claims), seq watermark + store-if-absent buffer + identical-row
  write skip (observations), control-id first-delivery-wins (controls). Probes P3,
  P4, P13 + existing suite pin this.
- **Partial failure — PASS** (was FAIL-major). Grant + inbox removal + recovery
  index commit in one batch transaction; request fan-out likewise. The pre-fix
  lane-livelock trace is structurally impossible now.
- **Single depot append per client op — FAIL (deferred L-10).**
  `mark-stale-approvals!` still appends one control per approval. Severity reduced:
  the FIRST resolve's terminal-decision branch now expires all remaining pending
  approvals and fails the run, so a crash mid-doseq no longer leaves durable
  inconsistency — the remaining controls become recorded no-ops.
- **Application-state caches survive restart — PASS** (was FAIL). The recovery
  index + token-based `claim-state` give a restarted executor a durable discovery
  path (probe P12). The Claude executor's buffer-to-EOF streaming (L-11) is a
  latency defect, not a truth cache.
- **No reimplementation of built-ins — PASS** (was FAIL).

## Spec validation (delta against pre-fix S-findings)

- **S1 per-run total order + never-drop — PARTIAL.** Never-drop now holds: orphan
  records land in `$$llm-dead-letters`, queryable by run id (probes P6/P7); nothing
  is silently consumed; controls for known runs always reach `$$llm-control-by-id`.
  Mechanical cross-depot total order does NOT hold — still four depots, not
  PLAN.md's single records depot; executor/bridge ack-causality remains the ordering
  argument. Dead-letters are not auto-replayed (surfacing beats guessing). Flagged
  for a future session if the single-depot consolidation is wanted.
- **S2 sticky terminals — FIXED.** All four regression sites fenced + drain fence +
  silent authorized-redelivery policy. Probes P9a/P9b/P15.
- **S3 two-phase cancel — NOT FIXED (deferred L-06).** Cancel is still one-phase
  (`:cancelled` immediately); no `:cancel-requested` state; executor mid-turn
  control polling still absent. Sticky terminals now prevent the worst symptom
  (run-finished overwriting `:cancelled`).
- **S4 approval lifecycle — FIXED except timeout.** First-wins, no resurrection, no
  invention, run-close expiry all probed (P5/P10/P11). No wall-clock timeout
  scheduler exists (deferred — needs a tick depot).
- **S5 buffered approval loses the pending index — FIXED** (diff-driven index;
  probe P8 pins drain-materialized approval + native id 77).
- **S6 deterministic fold — PARTIAL (deferred L-12).** Audit errors now prefer
  record time (`audit-time-ms`); `apply-observation-effect`/`grant-claim` wall-clock
  fallbacks for records missing timestamps remain.
- **S7 bounded live view — NOT FIXED (deferred L-09/L-08).** `run-view` still
  embeds the full item vector.
- **S8 live streaming — NOT FIXED (deferred L-11).** Claude executor still appends
  nothing until process exit.
- **S9 restart policy decorative — unchanged** (behavior matches the only MVP
  policy; design debt).
- **S10 secret hygiene — NOT FIXED (deferred L-11).** `:stderr-tail` still stored
  unredacted; non-Claude adapter redaction still conventional.
- **Divergence 12 (native id silent flip) — FIXED** (thread row first-write-wins;
  `:native-conflicts` recording still absent).

## Probes (failing-first evidence)

Pre-fix baseline (same module, before any edit) reproduced the documented
failures verbatim:

```
:observation-without-claim {:status :running, :claimed-by nil, :last-seq 0,
                            :items 1, :pending-still? true}
:duplicate-request-after-terminal {:status :pending, :last-seq -1,
                                   :claim-token nil, :pending-again? true}
:approval-resolve-unknown {:approval {:approval/id approval-never-requested,
                                      :status :approved, ...},
                           :pending-approval nil, :run-status :pending}
```

Post-fix, `dogfood_llm_probe_test.clj` (P1–P15 over the Session-0 probe harness)
asserts the semantic payloads: claimless/wrong-token observations reject + audit +
run stays claimable and completes; duplicate request after `:succeeded` is a total
no-op in both same-payload and conflict variants; approval invention rejected with
the control retained on the trail; buffered approvals index on drain; terminal
stickiness both directions; first-resolution-wins; run-close expiry; recovery-index
lifecycle with `:granted-to-us` surviving `:running` and dying at terminal;
winner-claim replay; terminal buffer fence; unknown-run obs + control dead-letters;
blank-id refusal/drop with the topology alive after every hostile record.

## Open doubts

1. **Microbatch latency vs the streaming spec** — accepted deviation, not measured.
2. **Cross-source ordering inside one microbatch** — a request and its first
   observation in the same batch have no defined relative order across `<<sources`
   branches; an early observation dead-letters rather than folds (executor
   ack-causality makes this unreachable in the sanctioned flow, and never-drop now
   surfaces it if a client misbehaves). The single-records-depot design would
   eliminate the window mechanically.
3. **Dead-letter recovery** — ledgered records are not auto-replayed; an operator
   (or a future reconciliation) must decide. This matches PLAN.md's
   `$$orphaned-records` stance.
4. **`(get *gate :gate/status)` / helper-fn dataflow calls** rely on plain-fn
   invocation semantics — consistent with the compute kernel's identical usage.

## Verdict

**conditional-pass (in-scope), major-fail outstanding (deferred scope).**

Every finding in the session's correctness scope — L-01, L-02, L-03, L-04, L-05,
L-07 (minus timeout scheduler), prior-retro F1/F2/F3, S2, S5, divergence 12 — is
fixed and probe-pinned. The deferred families (L-06 two-phase cancel, L-08/L-09
schema + bounds + write-amplification, L-10 single-append stale marking, L-11 live
streaming + stderr redaction, L-12 fold determinism residue, S1 single-depot total
order, approval timeout) keep the module at major-fail against the FULL spec, by
the rubric's "any single failure requiring significant changes" rule — exactly the
posture Session 1 ended in for compute. Those belong to the follow-up batches.

Test evidence at close: probe matrix 67/67; `dogfood-llm-test` 114/114;
space/compute/foundations suites green (space tests updated to claim runs before
streaming — the claim-proof contract change; noted for Session 3).
