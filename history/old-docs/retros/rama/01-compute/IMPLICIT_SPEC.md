# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

**Module:** The Compute Kernel — requested workspace commands become observable run state.

**Mode:** RETROSPECTIVE Phase 0. This spec was derived from the user-facing docs
(`docs/current-mental-model/architecture/dogfood-runtime/compute-track.md`,
`docs/current-mental-model/architecture/dogfood-runtime/slice-a-compute-run-command.md`),
the module's identity header, and the public contract as exercised by
`test/app/server/rama/dogfood_compute_test.clj`. The source docs mix requirements with
design decisions; per retro rules, storage-level design (state-store names/schemas,
log/queue names, partitioning, execution primitives) is deliberately excluded here. Where
a doc states a design choice, the requirement that motivated it is recorded instead.

## System summary (requirements language)

An actor (UI, test harness, later an agent) submits a request to run a workspace command
(an argv such as `["echo" "hello"]`, with working directory / env constraints). The kernel
validates the request and records an accept/reject **decision**. An accepted request
creates a **run** that must progress through an observable lifecycle:

```
(absent) → :pending → :launching → :running → :succeeded | :failed
```

Physical execution is performed by a kernel-owned **executor** that runs out-of-band from
request processing (request handling must never block on process execution). The executor
must not perform any physical work it was not durably granted: it discovers pending runs,
**claims** them, waits until the kernel has durably granted the claim to *it specifically*
(identity + secret claim token), and only then spawns the OS process. Everything the
process does comes back into the kernel as **observations** (`:started`, `:stdout`,
`:stderr`, `:heartbeat`, `:exit`), each attributable to the claim holder via the claim
token and ordered by a per-run monotonic sequence number. The kernel folds authorized,
sequence-ordered observations into run truth and into a UI-readable projection. The
back-arrow rule, stated as a requirement: actors (UI, executor) only *request* and
*report*; only the kernel writes truth; all actors read truth back from kernel state.

**In scope (Slice A.0):** `:compute/run-command` end to end, automatic module-owned
execution, plus a manual single-step execution path for protocol tests.
**Explicitly deferred (not in this spec's required behavior, but must not be precluded):**
cancel, restart-reconcile after kernel restart, stall/lost-run detection, retry policy,
exceptions-as-observations policy (deferred to Slice A2 per the slice doc).

## Operations

### Visibility contract (applies to every operation)

Writes are acknowledged; after acknowledgment the effect must become visible to the
corresponding reads within bounded time. The test contract polls (`await-decision`,
`await-run`, `await-view`, `await-materialized`) with an end-to-end upper bound of
**5000 ms** from submit to terminal view for a trivial command (`echo`). Reads must be
callable at any time, in any entity state, and must never observe a state that violates
the invariants below (e.g. a view must never show output from an unauthorized writer, and
must never show a token).

---

### O1. Submit run-command request (write; actor → kernel)

The actor builds a request envelope and submits it. Evidence of shape (from docs + test
helper `run-command-request`): `:run/id` (minted by the requester *before* submission),
`:request/id`, `:request/type :compute/run-command`, `:request/time-ms`, actor identity,
target (e.g. `:workspace`), action params (`argv`, `cwd`, env allow-list), and an optional
executor-assignment hint (test field `:executor-task-id`; when omitted the kernel assigns
an executor inbox itself — this is the normal path).

- **Latency:** Interactive UI path. Decision and `:pending` run visibility should be
  sub-second; hundreds of ms is acceptable; single-digit ms is not required. Hard test
  bound: submit→terminal ≤ 5 s for `echo`.
- **Throughput:** Human/agent-paced dogfood usage — at most a few requests per second in
  bursts, typically far less. Volume scales with how often the inhabitant runs
  build/test/shell commands, and later with agent-initiated compute.
- **Consistency/correctness invariants:**
  - Every submitted request gets exactly one recorded decision (`:accepted` or
    `:rejected`) that is readable afterward, echoing `:request/type`.
  - An accepted request creates exactly one run in `:pending` and exactly one entry in
    exactly one executor's pending inbox.
  - A rejected request must never become spawnable: no pending-inbox entry, no run that
    an executor could claim.
  - Request validation must include target-kind checking (workspace targets are valid;
    the compute track doc requires "checks target world refs").
  - Request acceptance must not wait for, or depend on, process execution ("the topology
    never waits for the command to finish" → requirement: decision latency is independent
    of command duration).
- **Data growth and scale:** Run/decision records accumulate forever (every command ever
  run). Access pattern is point lookup by `:run/id`; results must remain readable after
  completion indefinitely (no deletion implied anywhere in the contract).
- **Concurrency behavior:** Requester mints `:run/id` before submitting, so duplicate
  delivery/retry of the same submission is possible. A duplicate submit of an existing
  `:run/id` must not create a second run, must not duplicate the inbox entry, and must
  never regress an in-flight or terminal run back to `:pending` (that would re-arm the
  spawn path → double spawn). Concurrent submissions with distinct run-ids are fully
  independent.
- **Edge cases:** empty argv; argv referencing a nonexistent binary (acceptance is still
  valid — failure surfaces later as a failed run, see O8 edge cases); missing/blank
  `:run/id` (must be rejected or refused — a run without identity cannot be tracked);
  reuse of a `:run/id` from a finished run (must not reopen it); request with an
  executor-assignment hint pointing at a nonexistent inbox (run would sit pending forever
  — acceptable for the manual/test path, which is exactly what the tests exploit, but the
  normal path must always assign a live executor inbox).

### O2. Claim a run (write; executor → kernel)

An executor announces intent to execute a specific run. Evidence of shape (test helper
`claim-record`): `:run/id`, executor id, a fresh secret `claim-token` (uuid-like, minted
by the claimant), claimed-at time, and the claimant's assignment id.

- **Latency:** Claim-grant visibility bounds time-to-spawn; sub-second expected. The
  executor polls for the grant on its own loop, so the kernel's only obligation is
  bounded visibility, not push.
- **Throughput:** One claim per run in the normal case, plus retries and (rarely)
  competing claims. Negligible volume.
- **Consistency/correctness invariants:**
  - **Single-grant invariant (the core safety property):** for any run, at most one claim
    is ever granted. The first claim processed against a `:pending` run wins: the run
    moves `:pending → :launching` and durably records the winner's identity
    (`:claimed-by`) and `claim-token`, and the run is removed from the pending inbox.
  - A claim against a run in any state other than `:pending` is a strict no-op — the
    recorded winner's identity and token are never overwritten.
  - A claim against an unknown `:run/id` must not create run state.
  - Claim processing must be retry-safe: redelivery of the winning claim must not change
    the outcome; redelivery of a losing claim must remain a no-op.
- **Data growth and scale:** Bounded by run count; claim outcome lives on the run record.
- **Concurrency behavior:** Multiple executors may claim the same run concurrently
  (duplicate inbox reads, races at scale-out). Outcome must be deterministic: exactly one
  winner, chosen by processing order, and the loser must be able to *detect* it lost by
  reading the run record (sees another's identity/token → interprets conflict). Test
  evidence: `double-claim-and-wrong-token-observation-test` — second claim leaves
  `claimed-by`/`claim-token` at the first claimant's values.
- **Edge cases:** claim for a run already terminal (no-op); claim redelivered after grant
  (no-op, idempotent); claim with the same token as the winner but different executor id
  (must not be treated as granted — grant check is identity AND token); claim arriving
  before the run is visible (claimant treats as not-yet-processed and retries; kernel
  must not error).

### O3. Report observation (write; executor → kernel)

The executor reports process events. Evidence of shape (test helper `observation`):
`:run/id`, `claim-token`, `:observation/type`, monotonic per-run `:sequence` (starting
at 0), payload. Types in scope from tests: `:started` (carries pid), `:stdout`/`:stderr`
(carry a line), `:exit` (carries exit code). Types named by the docs and required not to
be precluded: `:heartbeat`, `:artifact-produced`, `:server-started`, `:port-opened`,
`:failed`, `:succeeded`, `:cancelled`.

- **Latency:** Drives the "live logs" feel — observation-to-readable-view should be
  sub-second; hundreds of ms acceptable.
- **Throughput:** **This is the dominant write volume of the kernel.** Build/test output
  can emit thousands of lines in bursts (10³–10⁵ observations per run). Per-observation
  processing cost must be small and constant; readable surfaces must stay bounded no
  matter how much a process prints.
- **Consistency/correctness invariants:**
  - **Attribution/authorization:** an observation is applied only if its `claim-token`
    matches the token durably granted for that run. Unauthorized observations (wrong
    token, or any token while no grant exists) must NOT mutate run output/status, and
    MUST be recorded as observation errors with a reason (test evidence: reason
    `:observation/not-authorized`), visible in both the run truth record and the UI
    view — unauthorized attempts are auditable, not silently dropped.
  - **Ordering:** observations are applied in per-run `:sequence` order regardless of
    arrival order. Out-of-order observations are buffered, **never dropped**, and applied
    when the gap fills. `:exit` closes the run only after draining buffered observations
    in sequence order.
  - **Lifecycle effects:** authorized `:started` → status `:running` + pid recorded;
    `:stdout`/`:stderr` → folded into bounded recent-output tails in sequence order;
    `:heartbeat` → updates a liveness timestamp, no status change; `:exit` → terminal
    status derived from exit code (0 → `:succeeded`, non-zero → `:failed`) with the exit
    code recorded.
  - **Terminal immutability:** once a run is `:succeeded`/`:failed`, late or redelivered
    observations must not reopen the run or mutate its terminal result.
  - **Retry safety:** redelivery of an already-applied sequence number must not
    double-apply (e.g. the same stdout line must not appear twice in the tail).
- **Data growth and scale:** Per-run output is unbounded in principle; the readable run
  record and view keep bounded tails (`:stdout-tail`, `:stderr-tail`) plus the exit code.
  The out-of-order buffer must drain on gap fill / exit and must not grow without bound.
  The observation-errors list is attacker/bug-influenced (a rogue or stale writer can spam
  wrong-token observations) and must be bounded.
- **Concurrency behavior:** Exactly one authorized writer per run (the grant holder), so
  per-run total order comes from the sequence number, not arrival time. Observations for
  different runs are independent and must not contend.
- **Edge cases:** sequence gap that never fills before `:exit` (flagged below — the slice
  doc says "drain in seq order" but not whether a permanent gap blocks closure); duplicate
  sequence numbers with different payloads (must keep the first, never corrupt);
  observation for an unknown `:run/id` (must not create run state); huge single line
  (must not break the bounded tail); `:exit` as the only observation (process died before
  `:started` was reported — run must still close terminally); unauthorized observation
  while run is `:pending` (no token granted yet → not-authorized error, status unchanged —
  exact test scenario).

### O4. Read decision (read; any actor)

Returns the accept/reject decision for a run/request id. Test evidence: contains
`:decision/status` (`:accepted`) and `:request/type` (`:compute/run-command`).

- **Latency:** Point lookup, ms-scale; polled by `await-decision` after submit.
- **Throughput:** Low — once per submit plus UI checks.
- **Invariants:** Readable forever once recorded; never flips after being recorded;
  reflects the *request* outcome, not the process outcome (the `false` run's decision is
  `:accepted` even though the run ends `:failed`).
- **Edge cases:** unknown run-id → absent/nil; read before the submit is processed →
  absent, then appears (poll contract).

### O5. Read run record (read; executor-facing truth)

Returns the full lifecycle truth for a run. Test evidence of contents: `:status`,
`:claimed-by`, `:claim-token`, executor assignment id (`:executor/task-id`, a string),
`:stdout-tail`, `:observation-errors`; by implication also pid, `:exit-code`,
`:stderr-tail`, heartbeat timestamp.

- **Latency:** ms-scale point lookup; the executor polls this for its 3-state grant check,
  so it sits on the spawn-latency path.
- **Throughput:** Executor reconcile polling + tests; modest.
- **Invariants:** This is the *only* surface that exposes `claim-token` — it is what the
  grant check reads. Must always reflect every applied write (it is the truth record).
  Given a run record and a claim, the claimant must be able to classify deterministically:
  **granted-to-us** (`:launching` ∧ claimed-by is me ∧ token is mine), **not-yet-processed**
  (record absent ∨ `:pending`), **conflict-or-past** (anything else).
- **Edge cases:** unknown run-id → absent (claimant must treat as not-yet-processed, not
  error); read in every lifecycle state must be well-formed (see matrix).

### O6. Read view (read; UI-facing projection)

Returns the UI-readable projection of a run: `:status`, pid, `:stdout-tail` (collection of
recent lines), `:stderr-tail`, `:exit-code`, `:observation-errors`.

- **Latency:** Backs a live UI subscription/watch; sub-second freshness after the
  underlying write.
- **Throughput:** Polled/watched per visible run for the run's lifetime; must be a cheap
  point lookup.
- **Invariants:**
  - **Token secrecy:** the view must NEVER contain `:claim-token` (asserted in every
    test that reads a view: `(not (contains? view :claim-token))`). The secret that
    authorizes physical work must not leak to the UI-readable surface.
  - Bounded size regardless of process output volume.
  - Must show unauthorized-observation errors (auditability reaches the UI).
  - Terminal views (`:succeeded`/`:failed`) remain readable indefinitely.
- **Edge cases:** unknown run-id → absent; view of a `:pending`/`:launching` run has no
  output yet but is well-formed; view after rejected request must never look like a
  pending run.

### O7. Read pending work (read; executor discovery)

Returns the set/collection of pending run-ids assigned to a given executor inbox key
(test evidence: `read-pending runtime executor-task-id` → contains `run-id`; membership
checked with `contains?`).

- **Latency:** Polled by the executor's reconcile loop; ms-scale.
- **Throughput:** One poll per executor per reconcile tick.
- **Invariants:** A run-id appears here iff the run is `:pending` and assigned to this
  inbox; it is added exactly once on acceptance and removed when a claim is granted.
  Nothing else removes it (a pending run with no live executor waits forever in A.0 —
  stall handling is deferred).
- **Edge cases:** empty inbox → empty collection (not an error); inbox key that has never
  existed → empty; the same run must never appear in two inboxes.

### O8. Automatic execution (behavior; kernel-owned executor)

Requirement from the identity header and slice doc: the kernel *owns* its executor — an
accepted `:compute/run-command` must reach a terminal state with NO further actor
involvement (test evidence: `task-global-executor-runs-command-automatically-test`
submits a request and only waits). The executor's required protocol, expressed as
observable behavior:

1. Discover pending runs assigned to it (O7).
2. For each undiscovered run, mint a fresh claim token and claim it (O2).
3. Await the durable grant via the 3-state check on the run record (O5):
   granted-to-us → proceed; not-yet-processed → keep waiting; conflict-or-past → drop it.
4. **Spawn the OS process only after observing the durable grant** ("spawned after
   grant" is asserted by tests — `:spawned-after-grant? true`). Never spawn on hope.
5. Stream `:started`/`:stdout`/`:stderr`/`:exit` observations with monotonic sequence
   numbers (O3) until process exit.

- **Latency:** Submit→terminal ≤ 5 s for `echo` (test bound); the reconcile poll interval
  plus grant round-trip dominate; the process's own runtime is unbounded and must not
  block any kernel processing or other runs.
- **Throughput:** Concurrent runs limited by worker capacity; multiple in-flight runs
  must execute independently.
- **Invariants:**
  - Physical work happens out-of-band from request/claim/observation processing.
  - The executor never writes truth directly — its only write channels are claim and
    observation submission (back-arrow rule).
  - At most one spawn per run, ever (consequence of single-grant + spawn-after-grant +
    the executor's local registry deduplicating discovery).
  - Process stdout/stderr/exit are captured and reported faithfully (test: `echo hello`
    → `"hello"` in stdout tail; `false` → positive exit code, `:failed`).
- **Edge cases:** spawn failure (binary missing, cwd invalid) — the run must still reach
  a terminal `:failed`-shaped outcome, not hang in `:launching` (mechanism flagged below);
  process outliving any timeout (manual path takes `:timeout-ms`; behavior at expiry
  flagged below); kernel restart with a live child process (reconcile deferred to A2, but
  recorded truth must never claim success for work that didn't finish).

**Manual/test execution path:** the contract also exposes a single-step executor
(`run-one-pending-local!` with `{:executor-id, :timeout-ms}`) that performs exactly one
discover→claim→await-grant→spawn→stream cycle against an explicit test inbox and returns
`{:claim-state :granted-to-us, :spawned? true, :spawned-after-grant? true}` on success or
`nil` when there is nothing it can run (e.g. it lost the claim — test evidence:
executor-b gets `nil` after executor-a's claim won). This path must obey the identical
claim/grant/observation protocol as the automatic path — it is a protocol probe, not a
bypass.

### O9. Runtime lifecycle (operational)

The contract exposes start and close of the whole runtime
(`start-compute-runtime!` / `close-compute-runtime!`). Close must cleanly stop executor
loops and worker processes so test runs don't leak threads/processes; repeated
start/close cycles (one per test) must work. Durable truth written before a close must
not be required to survive *test* harness teardown, but in production worker restart must
not lose accepted runs or recorded results.

## Entity State × Write Matrix

Read operations referenced below: `read-decision` (O4), `read-run` (O5), `read-view`
(O6), `read-pending` (O7). All four can be called in every state.

### Entity: Decision record

States: **absent**, **accepted**, **rejected**.

```
absent × submit (valid request)
  - read-decision: accepted record with :decision/status :accepted and :request/type — the request was admitted
  - read-run: :pending run row — acceptance creates the run atomically with the decision (no window where decision exists but run is unclaimed-able forever)
  - read-view: :pending projection — UI can show the run immediately
  - read-pending: contains run-id — the run is discoverable by its assigned executor

absent × submit (invalid request, e.g. bad target kind)
  - read-decision: rejected record — the actor can see why nothing will run
  - read-run: no spawnable run (absent, or a rejected marker — FLAGGED below); must never be :pending
  - read-view: must never look like a runnable/pending run
  - read-pending: must NOT contain run-id — a rejected request is never claimable

accepted × duplicate submit (same run-id redelivered)
  - read-decision: unchanged original decision — decisions never flip
  - read-run: unchanged (whatever lifecycle state the run is in; no regression to :pending)
  - read-view: unchanged
  - read-pending: run-id appears at most once — no duplicate inbox entry

rejected × resubmit (same run-id, corrected request)
  - read-decision: FLAGGED — unspecified whether a run-id is single-use; safe requirement: run-id is single-use, decision unchanged, actor must mint a new run-id
  - read-run: absent/unchanged
  - read-view: absent/unchanged
  - read-pending: still does not contain run-id
```

### Entity: Run record (central entity)

States: **does-not-exist**, **:pending**, **:launching**, **:running**, **:succeeded**,
**:failed**.

```
does-not-exist × submit (valid)
  - read-decision: accepted
  - read-run: {:status :pending}, no claim fields yet
  - read-view: {:status :pending}, no output, no token
  - read-pending: contains run-id under the assigned inbox

does-not-exist × claim
  - read-decision: absent — a claim cannot create a request
  - read-run: still absent — claims must not create run state; claimant classifies "not-yet-processed" and retries
  - read-view: absent
  - read-pending: unchanged

does-not-exist × observation (any type, any token)
  - read-decision: absent
  - read-run: still absent — observations must not create run state (FLAGGED: whether the rejected attempt is recorded anywhere when no run row exists)
  - read-view: absent
  - read-pending: unchanged

:pending × duplicate submit
  - read-decision: unchanged
  - read-run: {:status :pending} unchanged — no reset of any field
  - read-view: :pending unchanged
  - read-pending: run-id present exactly once

:pending × claim (first claim to be processed)
  - read-decision: unchanged accepted
  - read-run: {:status :launching, :claimed-by <executor>, :claim-token <token>, :executor/task-id <assignment>} — the durable grant the claimant's 3-state check needs
  - read-view: {:status :launching}, NO :claim-token — token secrecy
  - read-pending: run-id REMOVED — no other executor should discover it anymore

:pending × observation (any token)
  - read-decision: unchanged
  - read-run: :pending unchanged; :observation-errors gains {:reason :observation/not-authorized} — no token has been granted, so nothing is authorized
  - read-view: same error visible; no output shown
  - read-pending: still contains run-id — the run remains claimable

:launching × claim (second/competing claim)
  - read-decision: unchanged
  - read-run: claimed-by/claim-token remain the FIRST claimant's values (test-asserted); loser classifies "conflict-or-past" and walks away
  - read-view: :launching unchanged
  - read-pending: unchanged (already removed at grant)

:launching × duplicate submit (late redelivery)
  - read-decision: unchanged
  - read-run: must NOT regress to :pending — regression would re-arm the spawn path and risk double spawn
  - read-view: unchanged
  - read-pending: must NOT re-add run-id

:launching × observation :started (authorized token)
  - read-decision: unchanged
  - read-run: {:status :running, :pid <pid>}
  - read-view: {:status :running, :pid <pid>}, no token
  - read-pending: unchanged

:launching × observation :stdout/:stderr (authorized, out-of-sequence — e.g. seq 1 before seq 0)
  - read-decision: unchanged
  - read-run: status unchanged; payload buffered (never dropped), NOT yet visible in tails; applied when the gap fills, in sequence order
  - read-view: tails unchanged until in-order application
  - read-pending: unchanged

:launching × observation (wrong token)        [exact test scenario]
  - read-decision: unchanged
  - read-run: {:status :launching}, :stdout-tail stays EMPTY, :observation-errors gains {:reason :observation/not-authorized}
  - read-view: same status, same empty output, same error entry, no token
  - read-pending: unchanged (already removed)

:launching × observation :exit (authorized, with earlier seqs buffered)
  - read-decision: unchanged
  - read-run: buffered observations drained in sequence order FIRST (so :started/output land), then terminal status from exit code, :exit-code recorded
  - read-view: terminal status + exit code + final tails
  - read-pending: unchanged

:running × observation :stdout/:stderr (authorized, in-order)
  - read-decision: unchanged
  - read-run: line appended to the bounded tail in sequence order, exactly once even under redelivery
  - read-view: same line visible in tail (live log)
  - read-pending: unchanged

:running × observation :heartbeat (authorized)
  - read-decision: unchanged
  - read-run: liveness timestamp updated; status still :running
  - read-view: unchanged status/output (heartbeat is liveness metadata)
  - read-pending: unchanged

:running × observation :exit (authorized)
  - read-decision: unchanged — still :accepted even when exit ≠ 0 (the `false` test: request acceptance ≠ process success)
  - read-run: exit 0 → {:status :succeeded, :exit-code 0}; non-zero → {:status :failed, :exit-code <pos>}; buffered obs drained first
  - read-view: terminal status + exit code + tails; remains readable forever
  - read-pending: unchanged

:running × claim (late competing claim)
  - read-decision: unchanged
  - read-run: no-op — grant fields untouched; claimant sees conflict-or-past
  - read-view: unchanged
  - read-pending: unchanged

:running × observation (wrong token)
  - read-decision: unchanged
  - read-run: output/status untouched; :observation-errors gains a not-authorized entry
  - read-view: error visible; output untouched
  - read-pending: unchanged

:succeeded / :failed × claim
  - read-decision: unchanged
  - read-run: terminal record untouched; claimant sees conflict-or-past
  - read-view: terminal record untouched
  - read-pending: unchanged (absent)

:succeeded / :failed × observation (authorized late/duplicate, e.g. redelivered :stdout or second :exit)
  - read-decision: unchanged
  - read-run: terminal state immutable — no reopened run, no double-applied output, exit-code unchanged (FLAGGED: ignore vs record-as-error is unspecified; either is acceptable, mutation is not)
  - read-view: unchanged terminal view
  - read-pending: unchanged

:succeeded / :failed × duplicate submit (same run-id)
  - read-decision: unchanged
  - read-run: terminal record untouched — run-ids are not reusable
  - read-view: unchanged
  - read-pending: must NOT re-add run-id
```

### Entity: Pending-work inbox (per executor assignment key)

States: **empty/absent**, **contains run-id(s)**.

```
empty × submit accepted (run assigned to this inbox)
  - read-pending: contains run-id — executor will discover it on next reconcile
  - read-run: :pending
  - read-view: :pending
  - read-decision: accepted

contains run-id × claim granted for that run
  - read-pending: run-id removed — exactly the grant removes it, nothing else in A.0
  - read-run: :launching with grant fields
  - read-view: :launching
  - read-decision: unchanged

contains run-id × losing/duplicate claim
  - read-pending: unchanged if the run was already removed by the winning grant; a losing claim never removes someone else's pending entry
  - read-run: winner's grant fields (or still :pending if no claim has been processed yet)
  - read-view: matches run status
  - read-decision: unchanged

contains run-id × duplicate submit
  - read-pending: run-id present exactly once — no duplicates
  - read-run/read-view/read-decision: unchanged
```

## Open Ambiguities (flagged for Phase 1)

1. **Rejected-request representation:** the docs require a recorded rejected decision, but
   do not say whether a run record exists for a rejected request (absent vs a
   `:rejected`-status row). Hard requirement either way: never `:pending`, never claimable.
2. **Run-id reuse / duplicate-submit semantics:** requester mints run-id, so redelivery is
   possible; exact dedup behavior (first-wins vs explicit idempotency) is unspecified.
   Hard requirement: never two runs, never lifecycle regression, never double spawn.
3. **Sequence gap at exit:** "drain buffer in seq order on :exit" — behavior when a gap
   never fills (lost observation) is unspecified: block closure, close with a recorded
   gap, or close after draining what is contiguous. A.0 has a single in-process writer so
   gaps shouldn't occur; A2 retry/stall work will force this decision.
4. **Post-terminal observations:** ignore silently vs record as observation errors —
   unspecified; immutability of the terminal result is the only hard requirement.
5. **Spawn-failure surfacing:** binary-not-found/cwd-invalid must terminally fail the run,
   but the observation type/path (`:exit` with synthetic code vs `:failed` observation) is
   unspecified; the compute-track doc lists a `:failed` observation type.
6. **Manual-path timeout (`:timeout-ms`):** behavior when the process exceeds it is
   unspecified (kill + failed observation vs abandon). A run must not be left in
   `:running` forever with no recorded outcome — though full stall detection is
   explicitly deferred to A2.
7. **Observation-errors boundedness:** a stale/rogue writer can spam unauthorized
   observations; errors must be visible/auditable but the list must not grow unboundedly.
8. **Observations for unknown run-ids:** must not create run state; whether/where the
   attempt is auditable is unspecified (there is no run record to attach an error to).
9. **Out-of-scope-but-named operations:** cancel (`:cancel-request`/`:cancelled`),
   restart-reconcile, stall/lost-run detection, heartbeat-based liveness policy, and the
   broader compute-track request types (build/test/serve/deploy) and richer views ("builds
   by world snapshot", "artifacts by build", "active servers") are named by the track doc
   but deferred; the design must not preclude them (e.g. terminal-status set must be
   extensible to `:cancelled`).
