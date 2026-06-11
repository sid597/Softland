# Direct Review — `dogfood/compute.clj` (benchmark artifact)

**Provenance:** Claude direct falsification review, 2026-06-11, performed BEFORE the retro pipeline existed. Method: official `/rama` skill loaded (SKILL.md + core-concepts retry semantics verified), CLAUDE.md falsification protocol. NOT produced by the phased pipeline — this file is the calibration benchmark for it. Pipeline agents R0–R6 must not read this file; R7 merges it with provenance tags.

**Code reviewed at HEAD** (= last pre-skill state; file untouched since 2026-05-19): `src/app/server/rama/dogfood/compute.clj` (896 lines), `test/app/server/rama/dogfood_compute_test.clj` (147 lines).

## Verdict

The claim/grant/token protocol is correctly implemented for the happy path, and the v1 design lesson (never spawn inside a topology event) was absorbed — spawn happens executor-side after a grant read-back. But the module violates Rama's retry contract at the PState-write level: the request branch is non-idempotent under at-least-once replay, and two cross-partitioner write pairs can partially apply. The bug class caught at design time (retry × side effects) was reintroduced one layer down.

Key semantics both HIGH findings hinge on (verified in skill reference `core-concepts.md`): stream topologies are at-least-once; transaction scope = **between partitioners** (per-task atomic, not per-event atomic).

## Architecture (as-built)

- Depots: `*compute-depot`, `*compute-claim-depot`, `*compute-obs-depot` — all `(hash-by :run/id)`.
- One stream topology, four PStates: `$$compute-runs`, `$$compute-decisions-by-run-id`, `$$compute-pending-by-task`, `$$compute-views` (all `{String (map-schema Keyword Object)}`-style).
- `ComputeExecutorTaskGlobal` (declare-object) per task: scheduler thread polls `$$compute-pending-by-task` every 50ms via clusterPState handles, appends claims, awaits grant read-back, spawns process on worker pool, streams started/stdout/stderr/exit observations back through the obs depot.
- Back-arrow rule honored (workers → depots → PStates → readers). Partition alignment correct throughout (every local-select>/local-transform> traced against its preceding partitioner; foreign reads route by the same keys).

## Findings

**F1 — HIGH: Request replay clobbers live run state; can re-execute the command.**
`compute.clj:635` writes `initial-run-row` unconditionally (keypath + termval full overwrite). The request event spans two transaction scopes (`|hash *run-id` :626, `|hash *executor-task-id` :637). Scenario: event fully processed, claim granted, process runs/finishes; worker crashes before the stream offset checkpoints; record replays → :635 resets the row to `:pending` (claim token wiped), :638 re-publishes the pending entry. Consequences: late observations fail `authorized-observation?` and land as errors; executor sees a pending run absent from its rebuilt registry, claims, gets granted — **spawns the command a second time**. Non-idempotent commands (deploy, git push, rm) make this a correctness bug by the skill's own rule ("duplicate side effects from retried processing are bugs"). Same hole via duplicate client append with same run-id. Fix direction: guard with existence check (`<<if (nil? existing)`), make the write monotonic; derive pending entry under the same guard.

**F2 — HIGH: Granted-but-dead runs are stuck forever; no reconciliation path.**
Triggers: (a) executor JVM restart — registry (:569) is memory-only; a `:launching`/`:running` run's pending entry was deleted at grant (:651), so it is never re-discovered; (b) lost claim ack — `append-claim!` (:529) throws after the append landed → grant goes to a token nobody holds; replacement claim resolves `:conflict-or-past` (:364) and is dropped; (c) grandchild holding the pipe — `.join` on stdout/stderr threads (:854-855) has no timeout; after `destroyForcibly` a grandchild keeping the pipe open blocks the join forever (worker thread leak, exit observation never appended). No lease, no claim timeout, no `:launching`-age sweep. Stuck non-terminal state, permanent and silent.

**F3 — MEDIUM-HIGH: Partial claim event leaks a pending entry → infinite claim loop.**
Grant write (:648, scope hash(run-id)) and pending delete (:651, scope hash(executor-task-id)) are separate transactions. Failure between them → replay finds status ≠ `:pending`, `grantable-claim?` false, the delete branch never re-entered. Orphaned pending entry drives the executor into a permanent cycle: claim → not grantable → `:conflict-or-past` → registry remove → re-claim next tick (~20 depot appends/sec/orphan, forever, unlogged). Fix: unconditional pending delete keyed by the claim, or reconcile pending entries against run status.

**F4 — MEDIUM: One lost observation wedges the fold permanently.**
`append-process-observation!` consumes a sequence number before appending (:819). A throwing append kills that streaming thread with a hole in the sequence; later observations (incl. exit) buffer in `:obs-buffer` forever (:473); run never reaches terminal status. No gap timeout, no buffer bound. Side effect: `with-open` closes the child's pipe early → possible SIGPIPE kill of the child.

**F5 — MEDIUM: Rejected requests with blank/missing run/id write a decision under a nil key.**
Validation flags blank run/id (:154) but the rejected decision is still written at `(keypath *run-id)` (:627) with nil into a `{String ...}` PState. If Rama enforces key type → poison event retrying forever; if not → unreachable decision row. Untested either way.

**F6 — MEDIUM: `fold-observation` authorizes before duplicate-drop (:456-467).**
Obs depot uses `{:retry-mode :all-after}` (:653) → already-applied observations replay when a later record fails. Replays arriving after terminal status fail the terminal-status fence and are logged as `:observation/not-authorized` errors — false alarms for a healthy run. Reorder: seq-dedup first, then authorize.

**F7 — MEDIUM (perf): Per-line write amplification.**
Every stdout/stderr line rewrites the entire run row (two ≤200-line tails + obs buffer) into TWO PStates (:659-660): O(lines × row-size) serialization per line. Tails belong in a subindexed structure; the view should be a projection, not a second materialized full copy.

**F8 — LOW: Silent executor death.** Reconcile loop swallows every Throwable, no logging (:594). Persistent failure = executor does nothing, forever, invisibly.

**F9 — LOW: Unbounded concurrency.** Cached thread pool + claim-everything-pending (:526): no cap on simultaneous OS processes per task.

**F10 — LOW: JVM-global executor registry.** `defonce` atom keyed by identityHashCode (:538, :578); `close-compute-runtime!` → `close-all-compute-executor-states!` kills every executor in the JVM, not just this module's.

## Writers / readers / clearers

| State | Writers | Readers | Clearers | Verdict |
|---|---|---|---|---|
| `$$compute-runs` | request branch (reset! — F1), claim branch, obs branch | executors, clients, other branches | never (correct) | non-monotonic writer is the flaw |
| `$$compute-pending-by-task` | request branch (add), claim branch (delete on grant) | executors (poll) | grant path only | orphanable (F3); no status sweep |
| `$$compute-decisions-by-run-id` | request branch | clients | never | nil-key hazard (F5) |
| `$$compute-views` | all three branches | clients | never | claim-token stripped ✓ (tested) |
| executor registry (memory) | claim/submit/reconcile/worker-finally | reconcile loop | finally + conflict | lost on restart (F2) |

## Async ordering risks

Out-of-order observations: handled by the seq reorder buffer. Two claimants: first-wins via status fence (tested). The real risks are replay interleavings (F1/F3): "A finishes after B" where A is a replayed depot record and B is the live claim/grant flow.

## Error-path cleanup

Spawn failure → catch appends stderr + exit-127 with valid token → `:failed` ✓; registry entry removed in finally ✓. NOT cleaned: sequence holes (F4), orphaned pending entries (F3), stuck `:launching` rows (F2), join-blocked worker threads (F2c).

## Test coverage

Four tests: two happy paths, failed-exit, double-claim + wrong-token (good adversarial test — pins token fence and view token-stripping). Zero coverage of: validation rejections through the topology (F5 path never executed), replay/restart, observation gaps, claims against terminal runs, pending-entry consistency.

## What held up well

Token-fenced observations with terminal-status write-fence; reorder buffer; deterministic decision/event IDs; correct partition alignment everywhere; pending-index-driven discovery; spawn-after-grant discipline. The protocol design survived review; the write idempotency did not.

## Open doubts

1. Does Rama throw on a nil key into a `{String ...}` PState? (Decides F5: poison event vs unreachable row.) 5-line IPC test settles it.
2. Stream offset checkpoint cadence relative to processing — sets the real-world width of F1's replay window.
3. Can clusterPState reads from the TaskGlobal scheduler (initial delay 0) race module launch? Masked today by F8's catch-all.
