# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

This is a fresh Phase 0 derivation from the amended
`CONTRACT_R2.md` (including the 2026-07-27 §3f/T7/G3/G5 ruling). The
contract and `decisions.md` are authoritative; this artifact only expands
their required behavior, state, ordering, scale, concurrency, and failure
semantics. The superseded prior-round `IMPLICIT_SPEC.md` was input for
falsification, not authority.

This phase does not choose or revise depots, PStates, topologies,
partitioners, runtime plumbing, implementation functions, or tests. Names
below such as “run truth” and “pending truth” identify contract-visible
facts, not a new storage design.

## Domain boundaries and terms

- A **declaration** is inert in-process code data. R2 classifies a row as
  best-effort or durable; it does not make the declaration table durable,
  editable, or material-selectable.
- An **emission** is one occurrence of a declared trigger with a bounded
  data payload. The episode-retry customer consumes
  `:episode/turn-closed`.
- An **obligation** is the durable, acknowledged instruction to execute one
  durable row for one emission. It must exist before that row's handler may
  start.
- A **run** is the one globally scoped durable identity for a
  `(cascade-id, emission-id)` pair. It is absent, obligated, completed, or
  failed. One run id is never re-minted for a status transition.
- An **observation** reports a handler attempt's terminal outcome back to
  run truth. A normal handler return, including `:skipped`, is completed; a
  throw is failed.
- **Terminality** means completed or failed. The first terminal observation
  permanently wins. A terminal run is never made pending again.
- A **defunct transition** is one eligible fresh-episode failure identified
  by `(episode-id, turn-id, terminal-status)`. Each distinct eligible
  transition has its own import identity.
- A **defunct marker** is the one stable projected
  `:episode-chain-defunct` cell for an episode id. Later eligible
  transitions update that same cell to a later `:marked-at-ms`; they do not
  create a second visible marker cell.
- A **defunct import journal entry** and the **projected marker cell** have
  different cardinalities: the journal grows by distinct eligible
  transitions, while the visible projection remains one stable
  `ep-chain:` cell per episode id.
- The **runtime lane entry** is per-JVM, non-durable convenience state. It
  is neither run truth nor the durable defunct fact.
- **Adoption** is `current-episode!` choosing a warm runtime episode or,
  when that hint is absent, a durable turn cell. R2 changes only the
  fallback read's filtering; `decide-episode` remains pure and semantically
  unchanged.
- A **recovery sweep** is an explicitly invoked enumeration and
  re-execution of old obligated runs. It is not a timer, scheduler, retry
  policy, or namespace-load effect.

## Global requirements and identities

- Run observations are at-least-once. Duplicate and reordered delivery are
  normal, including concurrent handler execution by two server JVMs sharing
  one durable cluster.
- Externally visible handler effects are not atomic with run truth. Safety
  therefore comes from recorded-before-call ordering plus the handler's own
  duplicate-execution law.
- Durable identities are globally scoped, deterministic, and byte-stable:
  - episode-turn-closed emission:
    `"casc-em:episode-turn-closed:" + sha256(turn-id NUL terminal-status)`;
  - run:
    `"casc-run:" + sha256(cascade-id NUL emission-id)`;
  - defunct transition discriminator:
    `sha("episode-defunct " episode-id " " turn-id " "
    (name terminal-status))`, riding the existing `imp:ep:` import lane;
  - projected defunct order key:
    `ep-chain:<sha8(episode-id)>`.
- Replaying the same `(turn-id, terminal-status)` re-derives the same
  emission and run. A different terminal status for the same turn is a new
  emission and run. A different eligible failure transition for the same
  episode id is also a new defunct import, but it updates the same projected
  marker cell.
- Every durable value byte needed for replay comes from the emission
  payload. In particular, `:marked-at-ms` is the turn's payload
  `:time-ms`, never handler wall clock or sweep wall clock.
- Durable payloads and receipts are bounded EDN scalar/map data. They
  contain no functions, runtime handles, open resources, transcript
  contents, or canned-stream `:lines`.
- A successful durable dispatch has a hard ordering barrier:
  acknowledged/readable obligation first, then handler start. The durable
  success receipt is minted only after that barrier.
- A failure to establish the obligation performs no handler effect, emits
  no durable success receipt, logs `[CASCADE][EMIT-FAILED]`, and cannot
  fail the caller. The contract does not pin an alternate failure-receipt
  shape; it pins the absence of the specified success claim.
- Once the obligation exists, handler duration and outcome-observation
  latency are off the caller's path.
- Best-effort autotag remains byte-behavior-identical to R1. An absent
  `:cascade/runner` means best-effort; row #1's map, guard truth semantics,
  optimistic receipt, logging, threading, and failure isolation do not
  enter the durable runner.
- Namespace loading starts no runtime and runs no sweep. Any lazy runtime
  used on the turn path is poisoned-but-total on failure, never a cached
  throw.
- No run, obligation context, failure record, or defunct import is deleted
  by R2. Terminalization removes only pending membership.
- Existing relation, OC/RK/LLM module, serve, and cell vocabularies remain
  unchanged. The only new episode projection kind is
  `:episode-chain-defunct`, and existing readers do not see it unless they
  ask for it.
- No operation in R2 auto-respawns an agent, retries a failed run, applies
  backoff, installs a clock, or crosses an episode boundary with
  `--resume`.

## Operations

### 1. Enumerate and validate cascade declarations

- **Latency**: An in-process metadata read should be effectively immediate,
  in the single-digit-millisecond class, and independent of cluster
  availability.
- **Throughput**: Calls scale with dispatch, boot validation, tests, and
  operator inspection. The R2 table has two rows and is expected to remain
  small.
- **Consistency/correctness invariants**:
  - Both rows are returned in declaration order with unique cascade ids.
  - Row #1 remains byte-identical and defaults to best-effort because its
    runner field is absent.
  - Row #2 has exactly the episode-turn-closed trigger, durable runner,
    durable-via-request effect class, system actor, repair handler, and
    full idempotency story pinned by contract §3f.
  - Every durable declaration has a nonblank idempotency string naming its
    scope, transition law, and duplicate-execution tolerance.
  - Enumeration is inert and printable as data.
- **Data growth and scale**: Runtime history does not accumulate in the
  table. Adding a row remains a code change outside R2's runtime protocol.
- **Concurrency behavior**: Every caller in one loaded code version sees
  the same immutable declaration set.
- **Edge cases**:
  - An unknown runner value is invalid, not silently best-effort.
  - A durable row with absent, empty, or whitespace-only idempotency text is
    refused.
  - A best-effort row may omit the runner key.
  - A trigger with no matching rows returns no dispatch receipts and
    creates no work.

### 2. Dispatch matching best-effort rows

- **Latency**: Returns R1's immediate optimistic receipts. No cluster write
  or durable-runtime latency is introduced.
- **Throughput**: One asynchronous handler attempt per matching
  best-effort row and emission. R2 does not increase autotag volume.
- **Consistency/correctness invariants**:
  - Matching, declaration order, receipt shape, log facts, handler
    resolution, and future-per-row isolation are R1-identical.
  - The handler continues to own the literal old head guard:
    `(and (nil? gold-receipt) rk-rt)`.
  - No durable run, obligation, pending fact, or observation is created.
  - A resolution failure or handler throw is logged
    `[CASCADE][FAILED]` and cannot fail a sibling row or the emitter.
- **Data growth and scale**: R2 creates no new durable data on this path.
- **Concurrency behavior**: Concurrent best-effort emissions retain R1's
  independent futures and are not serialized behind episode-retry work.
- **Edge cases**:
  - `gold-receipt=false`, `rk-rt=false`, and nil retain their exact R1
    truth behavior.
  - A missing handler var or thrown handler still leaves the already-minted
    optimistic R1 receipt unchanged.
  - R2 must not route this operation through durable machinery in pursuit
    of code reuse.

### 3. Dispatch a matching durable row

- **Latency**: The obligation acknowledgement is on the dispatch path.
  Stream-style low latency is expected; hundreds of milliseconds may be
  survivable at turn close, but handler duration is never part of dispatch
  latency. Runtime failure must return total rather than block indefinitely.
- **Throughput**: Work scales as matching durable rows times emissions.
  Episode-retry produces one run identity per distinct
  `(turn-id, terminal-status)` for its one row, even when dispatch is
  attempted repeatedly.
- **Consistency/correctness invariants**:
  - JVM-side declaration matching expands exactly one obligation for each
    matching `(row, emission)`.
  - The emitter derives emission and run identities before the durable
    write.
  - The obligation contains run id, cascade id, trigger, emission id,
    bounded payload, and obligation time.
  - Acknowledged/readable obligated truth precedes both handler start and
    the durable success receipt.
  - The durable success receipt is exactly
    `{:cascade/id … :dispatched? true :durable? true
    :emission/id … :run/id …}`.
  - The durable row's `[CASCADE]` log identifies the row and run.
  - Handler return produces a completed observation with a bounded receipt;
    handler throw produces a failed observation with bounded failure data.
  - Failure before acknowledgement produces no handler call and no durable
    success receipt.
  - Failure after acknowledgement but before a terminal observation leaves
    an obligated run for recovery.
- **Data growth and scale**: Unique run history grows once per unique
  `(cascade-id, emission-id)`. Duplicate dispatch does not create a second
  run identity. Payload and receipt size never scale with jsonl or
  transcript length.
- **Concurrency behavior**:
  - Two JVMs may append the same obligation and run the same handler.
  - Normal execution and a recovery execution may overlap.
  - Correctness cannot depend on an in-memory lock, one server process, or
    one handler attempt.
- **Edge cases**:
  - No match creates no obligation.
  - Missing identity discriminators make the emission malformed; no
    nondeterministic substitute may be minted.
  - Different terminal statuses for one turn are separate emissions/runs.
  - A start failure after acknowledgement leaves the run obligated.
  - A handler effect followed by observation-write failure leaves the run
    obligated and permits duplicate effect execution on recovery.
  - A durable-append/runtime exception is contained by the total wrapper;
    the existing turn-close path continues.

### 4. Record or replay a durable obligation

- **Latency**: Materialized obligated truth is required before the
  acknowledgement consumed by durable dispatch. This is a low-latency
  write/read-after-ack requirement.
- **Throughput**: One attempted write per durable row dispatch, including
  at-least-once and HTTP replays.
- **Consistency/correctness invariants**:
  - An absent run becomes obligated, retains all data needed for later
    handler resolution, and becomes pending.
  - Replaying an identical obligation while still obligated preserves one
    run and one pending identity.
  - The first acknowledged obligation time remains the recovery-age anchor;
    duplicate delivery cannot postpone recovery indefinitely by refreshing
    its age.
  - Replaying an obligation after either terminal outcome cannot change the
    winning status/receipt, restore pending membership, or refresh a sweep
    age.
  - Obligated run truth and pending membership change as one observable
    transition.
  - No handler or external request is executed by this truth-write
    operation.
- **Data growth and scale**: Unique-run history is unbounded. Replays are
  constant-cardinality convergence. Pending cardinality is unresolved
  obligations, not all historical runs.
- **Concurrency behavior**: Concurrent identical obligations converge.
  Obligation replay reordered after a terminal observation loses to
  terminality.
- **Edge cases**:
  - Missing/blank run id is invalid.
  - A record whose cascade/emission identity-bearing fields disagree with
    its run id is malformed and cannot retarget existing truth.
  - A duplicate run id with materially different retained payload is not a
    license for last-write-wins mutation; it must fail closed or preserve
    the already-authoritative obligation.
  - Replaying an old obligation after completion/failed is a state no-op.

### 5. Record a terminal observation

- **Latency**: Outcome visibility should be low-latency after handler
  return/throw, but it does not delay the original turn caller.
- **Throughput**: Normally one observation per handler attempt. Duplicate
  execution and retry may create many attempted observations for one run.
- **Consistency/correctness invariants**:
  - Only `:completed` and `:failed` are accepted statuses.
  - The observation carries run id, terminal status, bounded receipt, and
    observed-at time.
  - The first terminal observation fixes status, receipt, and first
    observation time and removes pending membership as one observable
    transition.
  - No later observation changes the winning status/receipt or restores
    pending.
  - An exact duplicate observation is convergent (ruled 2026-07-27: the
    conflict record is a VALUE, never a counter, so record replay cannot
    inflate anything).
  - A later conflicting terminal observation overwrites the run row's
    `:late-conflict` value with its `{:status :receipt :observed-at-ms}`;
    history is not accumulated, and re-processing the same observation
    records converges to the same value.
  - An observation does not invent an obligation. The public protocol makes
    an orphan impossible by recorded-before-call ordering; a malformed
    orphan cannot fabricate runnable work.
- **Data growth and scale**: The terminal update and the single
  `:late-conflict` value remain bounded in one run record.
- **Concurrency behavior**: Simultaneous completed and failed observations
  legitimately race. The first durable terminal transition wins regardless
  of JVM-local completion order.
- **Edge cases**:
  - Unknown status, missing run id, runtime objects, or unbounded exception
    data are refused.
  - Removing already-absent pending membership is harmless.
  - A completed `:skipped` receipt is not a failed run.
  - A late failed observation does not make a first-completed run appear in
    failed enumeration.

### 6. Read one run

- **Latency**: A globally keyed point read should be single-digit
  milliseconds under normal cluster conditions plus one network round
  trip; cost must not scale with run history.
- **Throughput**: Driven by post-dispatch confirmation, console inspection,
  live proof, and diagnosis.
- **Consistency/correctness invariants**:
  - Missing id returns not-found, not fabricated state.
  - Acknowledged obligations are readable as obligated before their handler
    starts.
  - Terminal reads expose the first terminal outcome, retained obligation
    context, and the `:late-conflict` value (nil when no conflicting
    terminal was ever observed).
  - Terminalization never deletes the run.
- **Data growth and scale**: Point lookup remains independent of total
  historical cardinality.
- **Concurrency behavior**: A read racing a state transition may see the
  complete before-state or after-state, never a terminal run still
  advertised as pending in the same observation.
- **Edge cases**: Blank/unknown run ids do not trigger a scan. Corrupt run
  truth is surfaced rather than silently normalized into a valid status.

### 7. Enumerate pending runs

- **Latency**: This is a recovery/operator operation, not a render-loop
  query. It may scale with current pending cardinality and cluster width,
  but not with all terminal history.
- **Throughput**: Normally once per server boot plus occasional explicit
  operator calls.
- **Consistency/correctness invariants**:
  - Returns every and only currently obligated run, at most once per run id.
  - Each result exposes obligation time and retained obligation context
    sufficient to resolve the row's handler.
  - Completed and failed runs are absent.
- **Data growth and scale**: Pending should be operationally small because
  handlers normally terminate, but there is no hard maximum. Empty and
  backlog cases must both be correct.
- **Concurrency behavior**: A candidate can terminalize after enumeration.
  Enumeration is not a lock; duplicate execution and terminal convergence
  carry correctness.
- **Edge cases**:
  - Empty pending returns an empty collection.
  - Ordering is not a correctness promise.
  - A missing run for a pending identity is corrupt truth, not an excuse to
    invent payload or call a handler.

### 8. Enumerate failed runs

- **Latency**: This is a console/diagnostic history query. Single-digit
  milliseconds are not promised; seconds may be acceptable as history
  grows, but each invocation must terminate for the current finite data.
- **Throughput**: Low, operator-driven volume.
- **Consistency/correctness invariants**:
  - Returns every and only run whose first terminal outcome is failed.
  - A first-completed run remains absent even if it has a late failed
    observation.
  - Results retain failure receipt and obligation context for diagnosis.
  - Enumeration performs no retry, state transition, or cleanup.
- **Data growth and scale**: Failed history is unbounded and R2 defines no
  pagination or stable ordering. Consumers cannot treat this as a
  high-volume render surface.
- **Concurrency behavior**: A concurrently failing run may appear in this
  invocation or the next, but a query never returns a half-terminal row.
- **Edge cases**: No failed runs returns empty. Duplicate failed
  observations do not duplicate a run in the result.

### 9. Explicitly resume old obligated runs

- **Latency**: Boot/recovery latency may scale with the current pending
  backlog. Handler completion remains asynchronous through the same runner
  path.
- **Throughput**: Once per boot after runtime binding, plus explicit
  operator invocation. Work scales with pending entries strictly older than
  the grace threshold.
- **Consistency/correctness invariants**:
  - Default grace is 60,000 ms.
  - Only obligated runs strictly older than the grace window are resumed.
  - Young obligated, completed, and failed runs are not executed.
  - Recovery reuses the original run id, emission id, retained payload,
    handler, and observation path.
  - Starting recovery does not mint a run or change obligated status by
    itself.
  - One `[CASCADE][SWEEP]` line reports resumed count, pending count, and
    failed-terminal count.
  - No timer, repeated loop, or backoff policy is installed.
- **Data growth and scale**: Enumeration cost tracks pending, not total run
  history. Repeated sweeps create no attempt-history entity in R2.
- **Concurrency behavior**:
  - Two JVM boots may resume the same run.
  - A sweep may overlap the original handler or another sweep.
  - First-terminal-wins and handler idempotency, not claiming/locking,
    preserve correctness.
- **Edge cases**:
  - Empty pending yields zero resumed and a valid summary.
  - At `age == grace-ms`, the run is not yet eligible.
  - With zero grace, only obligations strictly earlier than the comparison
    instant are eligible.
  - Negative, nonnumeric, or overflowed grace is invalid input, not “resume
    everything.”
  - Missing retained data or an unresolvable handler cannot be replaced with
    guessed work.
  - A poisoned failed fixture remains unexecuted because failed is terminal.

### 10. Emit `:episode/turn-closed`

- **Latency**: Runs on the turn-end waiter before the existing terminal
  turn-cell write. It may pay bounded obligation-ack latency, never handler
  latency. Cascade unavailability remains total so terminal turn handling
  can continue.
- **Throughput**: One emission attempt per terminal close observation,
  scaling with turns rather than stream lines.
- **Consistency/correctness invariants**:
  - The emission occurs before the `:open -> terminal` turn-cell overwrite.
  - Its bounded payload is exactly:
    `{:turn-id :terminal-status :exit-code :duration-ms :episode-id
    :fresh? :seed? :thread-id :conversation-id :cwd :time-ms
    :jsonl-exists?}`.
  - `:jsonl-exists?` is observed at close time through
    `episode-jsonl-file`.
  - The payload contains no `:lines`.
  - Emission is unconditional with respect to episode-retry eligibility.
    Eligibility belongs to the handler head.
  - A replay of the same close derives the same emission/run.
  - If the JVM dies after obligation acknowledgement and before the turn
    status write, recovery remains valid from the payload.
  - If emission fails, the failure is logged and the existing terminal
    cell write still proceeds.
- **Data growth and scale**: One bounded run identity per matching durable
  row and distinct `(turn-id, terminal-status)`.
- **Concurrency behavior**: Replayed close processing converges. Different
  terminal statuses are separate emissions that may execute concurrently.
- **Edge cases**:
  - The jsonl file may appear after close-time observation; execution must
    re-check it.
  - The terminal cell write may never land after a successful obligation;
    the repair remains valid.
  - An already-visible terminal cell does not change emission identity.
  - `:complete` is a healthy close and is expected to be declined by the
    repair handler.

### 11. Execute the episode-retry handler

- **Latency**: Asynchronous relative to turn closure. The durable repair
  request is acknowledged before runtime cleanup. Seconds may be tolerable,
  but transcript/jsonl content scanning is not.
- **Throughput**: One invocation per run execution attempt, including cheap
  declines, duplicates, and recovery re-execution.
- **Consistency/correctness invariants**:
  - Head-decline unless terminal status is failed or timeout, `fresh?` is
    truthy, and close-time `jsonl-exists?` is false.
  - A head decline returns bounded `:skipped` and performs no marker or
    runtime write.
  - An initially eligible execution re-resolves the jsonl file from payload
    `(cwd, episode-id)` immediately before repair.
  - File-present at execution returns `:skipped`.
  - File still absent triggers the system-authored durable marker request.
  - Durable request acceptance/journaled convergence precedes runtime
    compare-and-remove.
  - The marker's identity and every value byte derive from the emission
    payload.
  - Runtime cleanup is limited to this JVM and conditional on the current
    entry naming the dead episode id.
  - Normal return, including skip or a harmless compare no-op, is completed.
    A thrown/rejected repair is failed.
- **Data growth and scale**: Eligible distinct failures append one import
  identity each; duplicate execution of the same transition is a journaled
  no-op. No file contents enter run or marker truth.
- **Concurrency behavior**:
  - Duplicate handlers may issue the same durable request.
  - Later eligible failure of the same episode id issues a different
    transition-scoped request and advances the one marker.
  - Each JVM can mutate only its own runtime atom.
  - A different current episode id is never removed by an old repair.
- **Edge cases**:
  - Healthy, non-fresh, present-at-close, or present-at-execution cases
    complete skipped.
  - File-check failure cannot be treated as proven absence; it must not
    establish a defunct marker from an unverified assumption.
  - Fingerprint conflict is not successful convergence; runtime cleanup
    cannot proceed as though durable repair landed.
  - Process death after marker acceptance but before cleanup is safe because
    durable fallback filtering survives JVM death.
  - Process death after both effects but before the completed observation is
    safe because re-execution converges and can observe completion again.

### 12. Upsert the durable defunct transition and marker

- **Latency**: An acknowledged durable request on the handler path, not the
  original turn-response path.
- **Throughput**: One attempted request per eligible execution. Unique
  accepted writes scale with eligible failure transitions, not duplicate
  attempts.
- **Consistency/correctness invariants**:
  - The actor is exactly the `:system` actor
    `"system:episode-retry/v1"`.
  - The request rides the existing hint-only `imp:ep:` import lane; no
    kernel vocabulary changes.
  - The import identity is transition-scoped from episode id, turn id, and
    terminal status.
  - The projected order key is stable per episode id.
  - The projected kind is `:episode-chain-defunct`.
  - The value is exactly
    `{:world-id :lane-id :episode-id :marked-at-ms}`, with marked time from
    the turn payload.
  - Replaying one transition is fingerprint-identical and journaled as a
    no-op.
  - A later eligible transition for the same episode id has a new import
    identity and advances the same projected cell.
  - A replay of an earlier, already-journaled transition after that advance
    is still a journal no-op and cannot regress the projected marker.
  - Different eligible terminal statuses for the same turn have distinct
    import identities but equal marker value bytes/time; either arrival
    order has the same visible marker.
  - Existing turn cells are byte-untouched.
  - Readers not asking for the new entry kind remain unchanged.
- **Data growth and scale**: One visible marker cell per episode id; one
  journal identity per distinct eligible failure transition. Journal growth
  is unbounded over time and no R2 cleanup exists.
- **Concurrency behavior**:
  - Same-transition requests converge exactly.
  - A causally later same-id transition advances the one marker.
  - The later transition is lawful only after the prior marker was visible
    enough for adoption to mint/revalidate a new fresh attempt; therefore
    its advancement is causally ordered, not last-write luck.
- **Edge cases**:
  - Missing world, lane, episode, turn, terminal status, or marked time makes
    the request invalid.
  - Same transition identity with different bytes is a durable fingerprint
    conflict and does not overwrite.
  - A later turn with time greater than the marker is deliberately not
    poisoned by the old marker.
  - A second eligible failure of the same episode id must not reuse the
    first failure's import identity.

### 13. Compare-and-remove the runtime lane entry

- **Latency**: An immediate in-memory operation after durable marker
  acknowledgement.
- **Throughput**: At most one attempt per eligible handler execution in that
  JVM.
- **Consistency/correctness invariants**:
  - Removal occurs only when the current lane entry's episode id equals the
    failed payload episode id.
  - Missing entry is a successful no-op.
  - An entry naming a different episode id is preserved.
  - No other JVM's atom is visible or mutated.
  - The compare and removal are one conditional transition.
- **Data growth and scale**: No durable growth. The per-JVM lane map remains
  bounded by active lanes.
- **Concurrency behavior**:
  - A concurrent local stamp with a different episode id wins preservation.
  - A newer re-minted entry with the same episode id is indistinguishable by
    the contract's compare key and may be removed; its already-durable later
    turn cell remains adoptable because its time is beyond the old marker.
  - Duplicate cleanup after a prior removal is harmless.
- **Edge cases**:
  - A fresh JVM sees an empty atom.
  - Process death makes the atom disappear without changing durable repair.
  - Runtime cleanup must not run after a rejected marker request.

### 14. Resolve the current episode through durable adoption

- **Latency**: This is on the next-turn decision path and must preserve
  interactive latency. Marker filtering cannot become an unbounded scan
  across unrelated conversations.
- **Throughput**: One resolution per turn attempt.
- **Consistency/correctness invariants**:
  - Existing warm runtime entry precedence remains unchanged.
  - When warm state is absent, durable turn candidates are restricted to
    the current lane/thread before choosing the newest.
  - A candidate turn cell is excluded when its episode id matches a defunct
    marker and `cell.time-ms <= marker.marked-at-ms`.
  - A later cell for that episode id with
    `cell.time-ms > marker.marked-at-ms` is eligible again.
  - Filtering occurs before taking `last`.
  - If all candidates are filtered, the pure decision receives no adoptable
    turn and mints fresh under existing rules.
  - The read layer changes; `decide-episode` does not.
  - Marker-read failure degrades to today's exact adoption behavior rather
    than failing the turn.
- **Data growth and scale**: The dominant scope is one conversation/lane's
  turn and marker rows. Unrelated containers are never scanned.
- **Concurrency behavior**:
  - A read racing first marker visibility may observe old behavior once.
  - After visibility, old/equal-time cells are excluded.
  - A marker advancing from `t1` to `t2` widens filtering only through
    `t2`; it does not poison cells after `t2`.
  - Runtime cleanup racing fallback is harmless because fallback uses
    durable truth.
- **Edge cases**:
  - No marker means exact existing behavior.
  - No remaining turn means fresh creation.
  - A later same-id cell revalidates that id without deleting the marker.
  - A marker-read failure is an explicit total fallback, not evidence that
    no marker exists.

### 15. Stamp the runtime lane at spawn

- **Latency**: Immediate and in-memory; it occurs before invoking the CLI
  process.
- **Throughput**: Once per spawned turn.
- **Consistency/correctness invariants**:
  - The lane key maps to the selected episode id and current turn time.
  - The stamp occurs before spawn, preserving the existing strand mechanism
    that R2 repairs rather than rewriting.
  - It creates no durable episode or run truth.
- **Data growth and scale**: One bounded runtime cell per active lane.
- **Concurrency behavior**: A later local stamp may replace an older cell.
  Repair cleanup may remove it only under the episode-id comparison
  described above.
- **Edge cases**:
  - Spawn may fail after the stamp, leaving the dead warm hint until repair.
  - The same episode id may be re-minted after durable filtering; the new
    stamp uses the later turn time.

### 16. Invoke recovery during server boot

- **Latency**: Invocation occurs after required runtimes bind. It must not be
  a load-time effect or a permanent boot poison.
- **Throughput**: Once explicitly per server boot; multiple server JVMs each
  legitimately invoke against shared truth.
- **Consistency/correctness invariants**:
  - Fresh classload starts no runtime and performs no sweep.
  - Boot uses the same explicit recovery operation and grace semantics.
  - Runtime construction is total-with-retry; a failed attempt is not cached
    as a throwing delay.
  - Runtime unavailability leaves obligations pending for a later boot or
    explicit invocation.
- **Data growth and scale**: Boot enumeration alone creates no run or
  attempt-history entity.
- **Concurrency behavior**: Simultaneous boots may execute the same
  obligation, which is already part of the durable row contract.
- **Edge cases**:
  - Sweep never runs before handler context is available.
  - Boot failure is logged/contained and cannot make every later turn
    rethrow a cached exception.

### 17. Deploy, redeploy, and restart the cascade-log organ

- **Latency**: Deployment is an operator/ops operation measured in settle
  time, not an interactive request. Status must be observed to terminal
  RUNNING rather than inferred from CLI process exit.
- **Throughput**: Rare package/deploy operations. This is the sixth module,
  not a per-turn action.
- **Consistency/correctness invariants**:
  - First deployment is additive: the five existing modules remain RUNNING.
  - The first-deploy “module absent” precondition is one-shot and is not a
    valid permanent harness invariant.
  - The post-package invariant is module present, RUNNING, and cleanly
    redeployable.
  - Worker restart preserves obligations, run state, pending state, and
    consumed offsets; it resumes native durable state rather than rebuilding
    it by replaying all depot history.
  - Deployment/classload itself runs no handler or sweep.
- **Data growth and scale**: Module lifecycle changes do not duplicate
  durable run identity or erase history.
- **Concurrency behavior**: Server JVMs may remain connected while a worker
  restarts; callers must observe total unavailability rather than corrupt
  fallback state.
- **Edge cases**:
  - Real-cluster deploy failure or license/node-count uncertainty is a
    package stop, not an alternate implementation path.
  - Existing-module status regression is a failed deploy gate.

## Required state transitions

### Durable run lifecycle

1. No run exists for `(cascade-id, emission-id)`.
2. A matching durable dispatch appends the bounded obligation.
3. Acknowledgement means the run is readable as obligated and pending.
4. Only after step 3 may the handler start and the durable dispatch receipt
   exist.
5. Until a terminal observation lands, the run remains obligated and may
   be recovered after grace.
6. Handler return records completed; handler throw records failed.
7. The first terminal observation permanently removes pending membership.
8. Later identical observations converge; later conflicting observations
   increment only the late count.
9. Replayed obligations after step 7 cannot resurrect the run.

### Episode-retry eligibility lifecycle

1. Every turn close emits regardless of eligibility.
2. Healthy status, non-fresh episode, or file-present-at-close returns
   completed/skipped.
3. Failed/timeout + fresh + file-absent-at-close reaches execution-time
   re-verification.
4. File-present-at-execution returns completed/skipped.
5. File still absent issues the transition-scoped durable defunct request.
6. Accepted/journaled durable repair precedes local runtime cleanup.
7. Normal cleanup/no-op return completes the run.
8. On later adoption, turn cells at or before the marker are filtered.

### Ruled-B second-eligible-failure lifecycle

1. A fresh episode id `E` fails on turn `T1` with terminal status `S1`, no
   jsonl file, and turn time `t1`.
2. Emission `EM1` and run `R1` are derived from `(T1, S1)`.
3. Repair imports transition identity
   `I1 = sha("episode-defunct " E " " T1 " " (name S1))` and writes the
   one marker cell `ep-chain:<sha8(E)>` with `marked-at-ms=t1`.
4. Replays of `R1/I1` are fingerprint-identical no-ops.
5. Adoption filters all `E` turn cells at or before `t1`. With no other
   adoptable turn, existing pure rules mint a fresh attempt; in the lane-id
   case this may intentionally re-mint the same episode id `E`.
6. The new turn `T2` has `t2 > t1`. Its durable turn cell revalidates `E`
   because the marker is time-scoped, not episode-id-forever.
7. If that fresh same-id attempt fails eligibly with terminal status `S2`
   and still has no jsonl, it derives a new emission `EM2`, new run `R2`,
   and new transition import
   `I2 = sha("episode-defunct " E " " T2 " " (name S2))`.
8. `I2` is accepted as a new import and overwrites the same `ep-chain:`
   projection cell with `marked-at-ms=t2`.
9. A late replay of `I1` is already journaled and therefore cannot regress
   the cell to `t1`.
10. Adoption now filters `E` cells through `t2`; the lane can mint fresh
    again. The repair is therefore reusable across eligible lifecycle
    transitions, never single-use.

### Required crash/failure outcomes

- **Before obligation acknowledgement**: no handler effect and no durable
  success receipt; caller continues after a logged total failure.
- **After obligation acknowledgement, before handler start**: run remains
  obligated/pending and is recoverable after grace.
- **After handler start, before marker acceptance**: no durable repair is
  assumed; throw may terminalize failed.
- **After marker acceptance, before local cleanup**: durable fallback is
  healed; duplicate execution journals a no-op and retries harmless cleanup.
- **After cleanup, before completed observation**: run is still obligated;
  recovery repeats only idempotent/conditional effects.
- **After close emission acknowledgement, before terminal turn-cell
  overwrite**: run/repair may complete while the turn cell remains open.
- **After first terminal, before obligation replay**: terminal status and
  empty pending membership remain unchanged.
- **File appears between close and handler execution**: execution-time
  re-verification completes skipped and writes no marker.
- **Two JVMs execute one run**: durable requests converge, local cleanup is
  JVM-scoped, and first terminal wins.

## Required refusals

- No clock pulse, scheduler, timer, automatic sweep loop, or retry backoff.
- No automatic resident respawn.
- No retry of terminal failed runs.
- No migration of autotag to durable execution.
- No durable/editable declaration table and no rows-as-material.
- No cross-boundary `--resume`.
- No changes to relation kinds, existing module/depot/PState/serve
  contracts, or existing durable cell shapes.
- No handler side effect before acknowledged obligation truth.
- No topology/truth-write path that calls a handler.
- No runtime handle or `:lines` in durable payloads.
- No semantic change to `decide-episode`.

## Entity State × Write Matrix

Every related read is listed for every row. “Run read” means the one-run
query; “pending read” means pending enumeration; “failed read” means failed
enumeration; “sweep read” means recovery eligibility.

### Entity A: durable cascade run aggregate

The aggregate is the one run's retained obligation/outcome plus its pending
membership, because those facts must change as one observable transition.

States:

- **R0 absent**: no acknowledged obligation.
- **R1 obligated-young**: acknowledged/nonterminal and not strictly older
  than grace.
- **R2 obligated-old**: acknowledged/nonterminal and strictly older than
  grace.
- **R3 completed**: completed won first terminal.
- **R4 failed**: failed won first terminal.

Write operations:

- **W1 obligation**: record/replay the deterministic obligation.
- **W2 completed observation**: record completed with bounded receipt.
- **W3 failed observation**: record failed with bounded receipt.

#### R0 absent × W1 obligation

- **Run read**: one obligated run with retained obligation data and the
  first obligation time.
- **Pending read**: contains the run exactly once.
- **Failed read**: absent.
- **Sweep read**: ineligible until strictly older than grace.

#### R0 absent × W2 completed observation

- **Run read**: remains not-found; orphan observation is malformed and does
  not invent an obligation.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent.

#### R0 absent × W3 failed observation

- **Run read**: remains not-found under the same orphan rule.
- **Pending read**: absent.
- **Failed read**: absent; failure history is not fabricated without its
  obligation context.
- **Sweep read**: absent.

#### R1 obligated-young × W1 obligation

- **Run read**: remains one obligated run with its first authoritative
  obligation context/time.
- **Pending read**: contains the run exactly once.
- **Failed read**: absent.
- **Sweep read**: remains based on the first obligation time; replay does
  not postpone age.

#### R1 obligated-young × W2 completed observation

- **Run read**: completed with this first terminal receipt/time.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent permanently.

#### R1 obligated-young × W3 failed observation

- **Run read**: failed with this first terminal receipt/time.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent permanently.

#### R2 obligated-old × W1 obligation

- **Run read**: remains the one obligated run; identity/context do not
  duplicate.
- **Pending read**: contains the run exactly once.
- **Failed read**: absent.
- **Sweep read**: remains eligible from the original age; replay does not
  make it young.

#### R2 obligated-old × W2 completed observation

- **Run read**: completed with this first terminal receipt/time.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent; an already-started duplicate may finish, but its
  observation cannot change the winner.

#### R2 obligated-old × W3 failed observation

- **Run read**: failed with this first terminal receipt/time.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent; no later sweep retries failed.

#### R3 completed × W1 obligation

- **Run read**: unchanged first completed outcome and retained obligation.
- **Pending read**: absent; terminal is not resurrected.
- **Failed read**: absent.
- **Sweep read**: absent.

#### R3 completed × W2 completed observation

- **Run read**: exact duplicate leaves the row/count unchanged; a
  non-identical later completed observation preserves the first receipt and
  is late.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent.

#### R3 completed × W3 failed observation

- **Run read**: remains completed with first receipt; late count increases.
- **Pending read**: absent.
- **Failed read**: absent because failure did not win.
- **Sweep read**: absent.

#### R4 failed × W1 obligation

- **Run read**: unchanged first failed outcome and retained obligation.
- **Pending read**: absent; terminal is not resurrected.
- **Failed read**: contains the run once.
- **Sweep read**: absent permanently.

#### R4 failed × W2 completed observation

- **Run read**: remains failed with first receipt; late count increases.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent.

#### R4 failed × W3 failed observation

- **Run read**: exact duplicate leaves the row/count unchanged; a
  non-identical later failed observation preserves the first receipt and is
  late.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent.

### Entity B: defunct transition journal plus stable marker

This aggregate includes transition-scoped import decisions and the one
projected marker cell. They deliberately have different identity scopes.

States relative to the candidate **D1 defunct upsert**:

- **M0 absent**: no marker and candidate transition not journaled.
- **M1 same-current**: the candidate transition is already journaled with
  identical bytes and is the current marker.
- **M2 same-older**: the candidate transition is already journaled, but a
  causally later transition has since advanced the current marker.
- **M3 predecessor-current/new-later**: an earlier marker is current and the
  candidate is a new, later eligible failure transition.
- **M4 same-turn-new-status**: a marker for this turn time exists, but the
  candidate has a different eligible terminal status and therefore a new
  import identity with equal visible marker bytes.
- **M5 fingerprint-conflict**: the candidate import identity exists with
  different bytes.

Related reads:

- **Import decision read**: accepted, identical journaled no-op, or rejected
  conflict.
- **Marker read**: projection read asking for the defunct kind.
- **Adoption read**: durable fallback used by `current-episode!`.
- **Unasked projection read**: existing reader not requesting the new kind.
- **Run read**: the handler's run after the import result and remaining
  local step are observed.

#### M0 absent × D1 defunct upsert

- **Import decision read**: accepted under the candidate's
  transition-scoped identity.
- **Marker read**: one system-authored marker at the candidate time.
- **Adoption read**: excludes matching episode cells at or before that time
  and permits later cells.
- **Unasked projection read**: unchanged; marker remains invisible.
- **Run read**: may complete after local cleanup/no-op; can remain obligated
  across a crash or fail if a later handler step throws.

#### M1 same-current × D1 defunct upsert

- **Import decision read**: fingerprint-identical journaled no-op.
- **Marker read**: same one marker, byte-identical.
- **Adoption read**: same filtering boundary; no widening or regression.
- **Unasked projection read**: unchanged.
- **Run read**: replay may complete after harmless cleanup; no new run or
  import identity is created.

#### M2 same-older × D1 defunct upsert

- **Import decision read**: fingerprint-identical journaled no-op for the
  older transition.
- **Marker read**: remains at the later transition's time; replay cannot
  overwrite it backward.
- **Adoption read**: retains the later filtering boundary.
- **Unasked projection read**: unchanged.
- **Run read**: the older run remains whatever terminal outcome first won;
  the replay cannot create a new run or undo the later repair.

#### M3 predecessor-current/new-later × D1 defunct upsert

- **Import decision read**: accepted as a new transition identity.
- **Marker read**: still one cell, advanced to the later payload time.
- **Adoption read**: now excludes matching cells through the later time and
  permits cells after it.
- **Unasked projection read**: unchanged.
- **Run read**: the new run may complete after cleanup/no-op; the predecessor
  run remains independently terminal.

#### M4 same-turn-new-status × D1 defunct upsert

- **Import decision read**: accepted under the distinct terminal-status
  transition identity.
- **Marker read**: one cell with the same visible value/time bytes.
- **Adoption read**: unchanged boundary because both eligible outcomes name
  the same turn time.
- **Unasked projection read**: unchanged.
- **Run read**: the distinct run may complete independently; neither run
  changes the other's first-terminal outcome.

#### M5 fingerprint-conflict × D1 defunct upsert

- **Import decision read**: rejected as fingerprint conflict.
- **Marker read**: pre-existing marker bytes remain authoritative.
- **Adoption read**: follows the pre-existing marker, not rejected bytes.
- **Unasked projection read**: unchanged.
- **Run read**: handler cannot report successful repair; runtime cleanup
  does not proceed and a throw/failure observation can terminalize failed.

### Entity C: per-JVM runtime lane entry

States relative to the failed payload episode id:

- **L0 absent**: no entry in this JVM.
- **L1 dead-match**: entry names the failed episode and is the stranded
  warm hint.
- **L2 same-id-newer**: a later fresh turn has re-minted/stamped the same
  episode id with a later turn time.
- **L3 different-current**: entry names a different episode id.

Write operations:

- **L-W1 stamp**: existing spawn-time write of selected episode id/time.
- **L-W2 compare-remove**: repair cleanup using the failed episode id.

Related reads:

- **Runtime read**: direct per-JVM lane lookup.
- **Current-episode read**: warm-first episode resolution.
- **Marker read**: durable defunct marker projection.
- **Run read**: durable outcome of the repair handler.

#### L0 absent × L-W1 stamp

- **Runtime read**: returns the newly selected episode/time.
- **Current-episode read**: may use this warm hint under existing rules.
- **Marker read**: unchanged.
- **Run read**: unchanged; stamping is not a cascade outcome.

#### L1 dead-match × L-W1 stamp

- **Runtime read**: returns the newly stamped episode/time.
- **Current-episode read**: uses the new warm value; if the id is re-minted
  same-id, its later durable turn cell remains the restart fallback.
- **Marker read**: unchanged and time-scoped.
- **Run read**: unchanged.

#### L2 same-id-newer × L-W1 stamp

- **Runtime read**: returns the latest local stamp.
- **Current-episode read**: follows that warm value.
- **Marker read**: unchanged; later turn time remains beyond the older
  marker.
- **Run read**: unchanged.

#### L3 different-current × L-W1 stamp

- **Runtime read**: returns the newly selected stamp according to the
  existing last-spawn runtime behavior.
- **Current-episode read**: follows that current warm entry.
- **Marker read**: unchanged and scoped to the failed episode.
- **Run read**: unchanged.

#### L0 absent × L-W2 compare-remove

- **Runtime read**: remains absent.
- **Current-episode read**: falls through to durable adoption, where the
  marker filters the dead cells.
- **Marker read**: unchanged and already durable.
- **Run read**: cleanup is a successful no-op; handler may complete.

#### L1 dead-match × L-W2 compare-remove

- **Runtime read**: becomes absent.
- **Current-episode read**: cannot resume the dead warm hint; fallback
  filters old/equal-time cells and can mint fresh.
- **Marker read**: unchanged.
- **Run read**: handler may complete after conditional removal.

#### L2 same-id-newer × L-W2 compare-remove

- **Runtime read**: becomes absent because the contract compares episode id,
  not transition/time.
- **Current-episode read**: falls back to durable cells; the newer cell's
  time is greater than the old marker, so the same episode id remains
  adoptable.
- **Marker read**: unchanged; a later eligible failure advances it through a
  new durable transition, not this runtime operation.
- **Run read**: old repair cleanup is still a harmless local effect; it does
  not erase the newer durable turn.

#### L3 different-current × L-W2 compare-remove

- **Runtime read**: preserves the different episode entry.
- **Current-episode read**: may use/validate that different current episode
  under existing rules.
- **Marker read**: unchanged and scoped to the failed episode/time.
- **Run read**: compare mismatch is a harmless no-op, not failed durable
  repair.

### Entity D: episode turn cell at close

R2 does not alter the existing cell shape or import-key law. It adds a
required ordering dependency to the terminal write.

States:

- **T0 open**: the durable pre-agent turn cell exists.
- **T1 same-terminal-present**: the same terminal result is already
  represented.
- **T2 different-terminal-present**: a different terminal result is already
  represented under the existing status-scoped import law.

Write operation:

- **T-W1 terminal close**: existing terminal cell write, now preceded by the
  unconditional close emission.

Related reads:

- **Turn read**: existing turn-record history.
- **Adoption read**: durable fallback over lane turn cells.
- **Run read**: run derived from this close's terminal status.
- **Marker read**: episode-retry repair projection.

#### T0 open × T-W1 terminal close

- **Turn read**: sees terminal if the write lands; may still see open if the
  JVM dies after emission and before this write.
- **Adoption read**: reads the resulting turn subject to marker filtering.
- **Run read**: obligation is already acknowledged before the terminal
  write attempt, or total emission failure was logged with no durable
  success receipt.
- **Marker read**: may appear asynchronously only for an eligible close.

#### T1 same-terminal-present × T-W1 terminal close

- **Turn read**: remains semantically the same under existing idempotency.
- **Adoption read**: unchanged except for separately durable marker truth.
- **Run read**: same `(turn-id, terminal-status)` converges on the same run.
- **Marker read**: same eligible repair transition journals a no-op; an
  ineligible status writes no marker.

#### T2 different-terminal-present × T-W1 terminal close

- **Turn read**: preserves the existing status-scoped turn-record semantics;
  R2 does not collapse terminal statuses.
- **Adoption read**: evaluates whatever existing turn projection is visible,
  then applies marker time filtering.
- **Run read**: different status produces a different emission and run.
- **Marker read**: if both statuses are eligible failed/timeout outcomes,
  they have distinct defunct transition identities but the same marker
  time/value; if one is healthy, its handler completes skipped and writes no
  marker.

### Entity E: cascade-log deployed organ and its durable truth

States:

- **D0 absent**: module not yet deployed (valid only before the one-shot
  first deployment).
- **D1 running**: module present/RUNNING with zero or more durable runs.
- **D2 worker-restarting**: module durable state exists but worker is
  temporarily unavailable.
- **D3 present-not-running**: deploy/redeploy failed to reach RUNNING.

Write/operational transitions:

- **D-W1 deploy/redeploy**: launch/update the module and wait for server-read
  RUNNING.
- **D-W2 worker restart**: restart processing while retaining native durable
  state and offsets.

Related reads:

- **Module-status read**: server-read module lifecycle.
- **Existing-module status read**: the five pre-existing modules.
- **Run read**: one run by id.
- **Pending read**: unresolved obligations.

#### D0 absent × D-W1 deploy/redeploy

- **Module-status read**: becomes present/RUNNING on success.
- **Existing-module status read**: all five remain RUNNING.
- **Run read**: no historical run exists before first use.
- **Pending read**: empty before first obligation.

#### D1 running × D-W1 deploy/redeploy

- **Module-status read**: returns RUNNING after the settle barrier.
- **Existing-module status read**: remains RUNNING.
- **Run read**: existing durable rows remain readable.
- **Pending read**: existing unresolved obligations remain present.

#### D2 worker-restarting × D-W1 deploy/redeploy

- **Module-status read**: must reach RUNNING; CLI exit alone is insufficient.
- **Existing-module status read**: remains RUNNING.
- **Run read**: may be temporarily unavailable but is not reconstructed or
  erased.
- **Pending read**: returns the same durable pending truth after recovery.

#### D3 present-not-running × D-W1 deploy/redeploy

- **Module-status read**: must become RUNNING to pass; otherwise the package
  stops.
- **Existing-module status read**: any regression is a failed gate.
- **Run read**: no claim of availability or loss is made from CLI exit.
- **Pending read**: preserved durable truth must be verified after recovery.

#### D0 absent × D-W2 worker restart

- **Module-status read**: remains absent; restart is invalid without a
  deployed module.
- **Existing-module status read**: unchanged.
- **Run read**: not available.
- **Pending read**: not available.

#### D1 running × D-W2 worker restart

- **Module-status read**: transitions through restart and returns RUNNING.
- **Existing-module status read**: remains RUNNING.
- **Run read**: pre-restart obligations/outcomes remain readable.
- **Pending read**: pre-restart unresolved obligations remain and processing
  resumes from durable offsets, not full-history replay.

#### D2 worker-restarting × D-W2 worker restart

- **Module-status read**: eventually RUNNING or the drill fails.
- **Existing-module status read**: remains RUNNING.
- **Run read**: temporarily unavailable is allowed; data loss is not.
- **Pending read**: same pending identities after recovery.

#### D3 present-not-running × D-W2 worker restart

- **Module-status read**: must reach RUNNING for recovery to count.
- **Existing-module status read**: remains RUNNING.
- **Run read**: existing truth must reappear unchanged.
- **Pending read**: existing unresolved work must reappear unchanged.

## Data-retention and scale summary

- Run history and failed-run history are unbounded and never deleted in R2.
- Defunct import-journal history grows once per distinct eligible failure
  transition. The visible projection remains one stable marker cell per
  episode id.
- Pending cardinality is unresolved obligations, not total historical runs.
- One-run reads remain independent of history size.
- Pending enumeration scales with pending and cluster width, not terminal
  history.
- Failed enumeration is a low-volume full diagnostic surface; R2 provides
  neither pagination nor ordering.
- Every individual write is bounded by one row/emission/transition and never
  by transcript or jsonl size.

## Correctness summary

- **Recorded before call**: no durable handler effect precedes acknowledged
  obligation truth.
- **First terminal wins**: status/receipt never regress, and terminal runs
  never re-enter pending.
- **Recovery-safe effects**: each defunct transition has deterministic import
  identity; the marker has stable projected identity; runtime cleanup is
  conditional and JVM-local.
- **Reusable same-id repair**: the second eligible failure mints a new import
  and advances the one marker; replay of the first transition cannot move it
  backward.
- **Time-scoped adoption**: defunct means “this episode as evidenced at or
  before this failure time,” not “this episode id forever.”
- **Truth separation**: run truth records obligation/outcome, journal truth
  records accepted repair transitions, the marker records current durable
  invalidation, and the runtime atom is disposable.
- **Totality**: cascade/runtime/marker-read failures cannot make the episode
  turn lane unavailable, while failed durable repairs remain truthfully
  terminal and enumerable.
- **No hidden activation**: classload does nothing; boot invokes one explicit
  sweep; no clock or automatic retry policy is introduced.
