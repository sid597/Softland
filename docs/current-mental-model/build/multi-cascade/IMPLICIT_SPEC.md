# Implicit Spec

<!-- Phase 0. Fill in before starting Phase 1 (Plan). After completing this, fill in PLAN.md. -->

Before designing any Rama-specific implementation, write out the implicit assumptions and expectations that a senior engineer would bring to this system based on the requirements and the domain.

This artifact derives requirements from `CONTRACT_R2.md`. The contract and
`decisions.md` govern if this derivation drifts. It deliberately specifies
observable behavior, state, ordering, scale, and failure semantics only. It
does not choose storage structures, event-processing structure, partitioning
machinery, implementation functions, or tests.

## Domain boundaries and terms

- A **declaration** is in-process code data. R2 adds a runner classification;
  it does not make declarations durable or editable.
- An **emission** is the occurrence of one named trigger with a bounded data
  payload and a globally scoped deterministic `:emission/id`.
- A **run** is the one durable obligation/outcome identity for one
  `(cascade-id, emission-id)` pair. There is exactly one durable run identity
  forever for that pair.
- **Durable obligation/run truth** is authoritative across process death. An
  acknowledged obligation exists before its handler is allowed to start.
  Terminal outcome truth converges under duplicate and reordered delivery.
- **Handler effects are not part of the atomic run-truth transition.** For
  episode-retry they are (a) an acknowledged durable request that establishes
  the defunct marker and (b) a compare-and-remove against one JVM's runtime
  lane entry. Either can have happened even if the outcome observation has not
  yet become durable. Re-execution must therefore be safe.
- A **terminal run** is `:completed` or `:failed`. Terminality is permanent.
  A handler return, including a deliberate `:skipped` receipt, is completed;
  a handler throw is failed.
- A **defunct marker** is episode truth in the affected conversation
  container. It invalidates adoption of the marked episode only for turn cells
  at or before the marker's recorded turn time.
- The **runtime lane entry** is per-JVM, non-durable convenience state. It is
  neither the authoritative record of a run nor the authoritative record of a
  defunct episode.

The following global requirements apply to every operation:

- At-least-once observation and duplicate execution are normal conditions,
  including two server JVMs sharing one durable cluster.
- All identities that name durable facts are deterministic and byte-stable:
  - episode-turn-closed emission:
    `"casc-em:episode-turn-closed:" + sha256(turn-id NUL terminal-status)`;
  - run: `"casc-run:" + sha256(cascade-id NUL emission-id)`;
  - defunct import: `sha("episode-defunct " episode-id)`;
  - defunct order key: `ep-chain:<sha8(episode-id)>`.
- The same `(turn-id, terminal-status)` is the same emission. A different
  terminal status for the same turn is a different emission and therefore a
  different run. No later transition of one run mints another run id.
- Durable payloads and receipts are bounded EDN scalar/map data. They contain
  no functions, runtime handles, open resources, or canned-stream `:lines`.
- Namespace loading performs no work. Recovery is invoked explicitly after
  runtimes are bound; there is no timer, scheduler, load-time sweep, or
  self-firing retry loop.
- Best-effort autotag behavior remains exactly the R1 behavior: absent runner
  means best-effort, its declaration bytes do not change, its receipt remains
  optimistic, and it does not enter durable run truth.
- No cascade failure may fail the episode turn lane. A successful durable
  dispatch necessarily includes the obligation acknowledgement barrier before
  its receipt and before handler start; the handler itself is never on that
  calling path. Failure to establish the obligation is logged and produces no
  durable success receipt.
- A failed run is terminal, queryable, and never selected by recovery.
- No operation in R2 auto-respawns an agent or automatically retries a failed
  run. The next user act starts the next episode attempt.
- Existing durable vocabularies and cell shapes remain unchanged. The only new
  episode projection kind is `:episode-chain-defunct`; it is invisible to
  readers that did not ask for it.

## Operations

### 1. Enumerate and validate cascade declarations

- **Latency**: This is an in-process metadata read and should be effectively
  immediate (single-digit milliseconds). It must not depend on durable runtime
  availability.
- **Throughput**: Calls scale with console inspection, dispatch, and boot-time
  validation, not with durable history. The table has two rows in R2 and is
  expected to remain small.
- **Consistency/correctness invariants**:
  - Both rows are returned in declaration order.
  - Row #1 remains byte-identical and is interpreted as best-effort when
    `:cascade/runner` is absent.
  - Row #2 has the exact trigger, durable runner, effect class, system actor,
    handler identity, and full idempotency story required by contract §3f.
  - Any durable declaration with an absent or blank idempotency story is
    refused. The story must name scope, transition law, and duplicate-execution
    tolerance; a label such as “idempotent” is insufficient.
- **Data growth and scale**: No runtime data grows here. Adding declarations is
  a code change and remains outside R2's runtime operations.
- **Concurrency behavior**: All callers observe the same immutable declaration
  set for a loaded code version. There is no runtime table mutation race.
- **Edge cases**:
  - Unknown runner values are invalid rather than silently treated as
    best-effort.
  - A non-durable row may omit the runner field and retains R1 behavior.
  - A durable row with whitespace-only idempotency text is invalid.
  - Empty trigger matches return no dispatch receipts and create no run truth.

### 2. Dispatch matching best-effort rows

- **Latency**: The call returns R1's immediate optimistic receipts; handler
  execution remains asynchronous and must not add durable-runtime latency.
- **Throughput**: Work is one asynchronous dispatch per matching best-effort
  row per emission. R2 does not increase autotag volume.
- **Consistency/correctness invariants**:
  - Matching, declaration order, receipt shape, log facts, guard truth
    semantics, and failure isolation are R1-identical.
  - No durable obligation, pending entry, or run record is created.
  - A handler failure is logged and cannot fail the emitter.
- **Data growth and scale**: R2 adds no durable growth to this path.
- **Concurrency behavior**: Concurrent best-effort emissions retain R1's
  behavior. R2 must not serialize them behind durable work.
- **Edge cases**: Literal false/nil guard behavior, handler resolution failure,
  and handler throw retain their R1 results. R2 must not “unify” these cases
  through the durable path.

### 3. Dispatch matching durable rows

- **Latency**: The obligation acknowledgement is on the durable dispatch path
  and must complete before a durable receipt can be returned. Low stream-style
  latency is expected; hundreds of milliseconds may be survivable at turn end,
  but handler duration is not part of dispatch latency. An unavailable or
  poisoned runtime must fail total rather than block the lane indefinitely.
- **Throughput**: Volume is
  `matching durable rows × emissions`. R2's honest customer contributes at
  most one run identity per `(turn-id, terminal-status)`, although duplicate
  dispatch attempts may repeat the same obligation.
- **Consistency/correctness invariants**:
  - Expansion is one obligation per matching `(row, emission)`.
  - Identity is computed before the write and is stable across JVMs and
    replays.
  - The obligation envelope contains exactly the run id, cascade id, trigger,
    emission id, bounded payload, and obligation time required by the contract.
  - The payload is bounded, serializable data only.
  - Obligation acknowledgement and readable `:obligated` truth precede handler
    start (“converge before call”).
  - The durable receipt is minted only after that acknowledgement and contains
    cascade id, dispatched true, durable true, emission id, and run id.
  - Successful row dispatch is logged as `[CASCADE]` with its run identity.
  - Handler execution is asynchronous. A normal return leads to a completed
    observation carrying a bounded receipt; a throw leads to a failed
    observation carrying bounded failure information.
  - Failure before obligation acknowledgement creates neither a durable
    success receipt nor an unrecorded handler effect. It logs
    `[CASCADE][EMIT-FAILED]` and the caller continues.
  - Failure after obligation acknowledgement but before a terminal observation
    leaves an obligated run recoverable by an explicit later sweep.
- **Data growth and scale**: Unique runs grow without deletion, one per unique
  `(cascade-id, emission-id)`. A duplicate does not add another run identity.
  Payload and receipt size are bounded independently of transcript length.
- **Concurrency behavior**:
  - Concurrent duplicate dispatches may execute the handler more than once.
  - They converge on one run identity and one durable episode marker.
  - The system does not rely on one JVM or on in-memory mutual exclusion.
  - Concurrent dispatch and recovery of the same run are legal.
- **Edge cases**:
  - No matching row creates no obligation.
  - Missing trigger discriminators are invalid because deterministic identity
    cannot be derived.
  - A different terminal status is not a duplicate; it creates another
    emission/run and relies on the handler effect's separate idempotency.
  - Failure to start the handler after acknowledgement leaves the run
    obligated; it does not erase the obligation.
  - Failure to record the terminal observation after a handler effect leaves
    the run obligated; recovery may repeat that effect.

### 4. Record or replay a durable obligation

- **Latency**: Materialized obligated truth is required before acknowledgement
  to the dispatching caller. It is a low-latency write/read-after-ack contract.
- **Throughput**: One attempted write per durable row/emission dispatch,
  including at-least-once repeats.
- **Consistency/correctness invariants**:
  - An absent run becomes obligated and recoverable.
  - Replaying an obligation while the run is still obligated keeps exactly one
    run and exactly one pending identity.
  - Replaying an obligation after either terminal outcome cannot change status,
    replace the winning receipt, or make the run pending again.
  - The run retains the bounded obligation data required to re-execute the
    handler after process death.
  - Pending membership and obligated run truth agree after the acknowledged
    operation.
- **Data growth and scale**: Unique-run history is unbounded; duplicate
  obligation writes are constant-space updates. Pending size is proportional
  to acknowledged obligations without a durable terminal outcome, not total
  historical runs.
- **Concurrency behavior**: Concurrent identical obligations converge.
  Obligation/terminal reordering is resolved in favor of terminality.
- **Edge cases**:
  - A replay with the same run id but identity-bearing fields inconsistent with
    that id is malformed and must fail closed; it must not silently retarget a
    run.
  - A replay may not refresh a terminal run's recovery age.
  - An empty or missing run id is invalid.

### 5. Record a terminal observation

- **Latency**: Outcome visibility should be low-latency after handler return or
  throw. The handler future may wait for this write without delaying the
  original turn caller.
- **Throughput**: Normally one observation per execution attempt. Duplicate
  execution and retry can produce multiple observations for one run.
- **Consistency/correctness invariants**:
  - Only `:completed` and `:failed` are accepted terminal statuses.
  - The observation envelope contains the run id, terminal status, bounded
    receipt, and observation time required by the contract.
  - The first terminal observation fixes the run's terminal status and winning
    receipt and removes pending membership as one observable transition.
  - Later observations never regress a terminal run or restore pending.
  - A later conflicting terminal increments `:late-observations` while
    preserving the first terminal outcome.
  - An exact same-outcome duplicate is convergent: it does not replace the
    winning outcome or increment the conflicting-late count.
  - An observation cannot be used to invent an obligation. Under the exposed
    protocol this is guaranteed by acknowledge-before-call; an orphan
    observation is malformed and fails closed without fabricating pending work.
- **Data growth and scale**: Outcome updates remain bounded in the one run row.
  Late observation detail is represented by a count, not an unbounded list.
- **Concurrency behavior**: Simultaneous completed/failed observations race
  legitimately; whichever terminal transition becomes durable first wins, and
  the loser is counted as late. No caller may assume local execution order
  determines the winner across JVMs.
- **Edge cases**:
  - Missing receipt fields may be represented only within the bounded receipt
    contract; runtime values and unbounded exception data are forbidden.
  - Unknown status, missing run id, or structurally invalid receipt is refused.
  - Removing an already-absent pending membership is harmless.

### 6. Read one run by globally scoped run id

- **Latency**: This is a point read and should be single-digit milliseconds in
  normal cluster conditions, plus one client/server round trip.
- **Throughput**: Driven by dispatch confirmation, console inspection, live
  receipts, and diagnosis. It scales with user/operator queries, not history
  scans.
- **Consistency/correctness invariants**:
  - Missing id returns not-found rather than a fabricated state.
  - An acknowledged obligation is immediately readable as obligated.
  - A terminal read exposes the first terminal outcome and any conflicting-late
    count.
  - Run identity and retained obligation data remain available after
    terminality; terminalization is not deletion.
- **Data growth and scale**: Point-read cost must not grow with total run count.
- **Concurrency behavior**: A read concurrent with a transition may see the
  complete state before or after that transition, never a terminal row still
  advertised as pending.
- **Edge cases**: Empty/unknown ids return not-found or input error according to
  the existing console convention; they never trigger a scan.

### 7. Enumerate pending runs

- **Latency**: Intended for recovery and operator inspection, not a render-loop
  interaction. Cost may scale with the current pending set and cluster width,
  but not with all historical terminal runs.
- **Throughput**: Normally invoked once per server boot plus occasional
  explicit operator calls.
- **Consistency/correctness invariants**:
  - Returns every and only currently obligated run, at most once per run id.
  - Each result includes the obligation time needed for grace filtering and
    enough retained obligation data to resolve the handler.
  - Completed and failed runs are absent.
- **Data growth and scale**: Pending is expected to stay operationally small
  because handlers normally terminate, but it is not assumed to have a fixed
  hard maximum. Enumeration must remain correct for an empty or unusually
  large backlog.
- **Concurrency behavior**: A concurrent terminal transition may cause a sweep
  candidate to disappear. Recovery must re-check/run safely rather than rely on
  enumeration as a lock.
- **Edge cases**: Empty pending returns an empty collection. Ordering is not a
  correctness guarantee. Duplicate underlying delivery must not yield duplicate
  run identities.

### 8. Enumerate failed runs

- **Latency**: This is a console/diagnostic history query; interactive
  single-digit latency is not promised. Seconds may become acceptable as
  history grows, but the operation must remain finite for the current data set.
- **Throughput**: Low operator-driven volume.
- **Consistency/correctness invariants**:
  - Returns every and only terminal failed run visible at the query boundary.
  - A run whose first terminal was completed is not failed even if a later
    conflicting failed observation arrived.
  - Failed results retain identity, failure receipt, and obligation context
    needed for diagnosis.
  - Enumeration does not retry or mutate failures.
- **Data growth and scale**: Failed history is unbounded because runs are never
  deleted. The no-argument R2 surface implies full enumeration; consumers must
  not assume a stable ordering.
- **Concurrency behavior**: A run failing concurrently may appear or not in
  that invocation; subsequent reads must include it. Enumeration never sees
  half-terminal state.
- **Edge cases**: No failures returns empty. Late conflicting observations do
  not cause one run to appear in both completed and failed interpretations.

### 9. Resume old obligated runs explicitly

- **Latency**: This is boot/recovery work, not a user-interaction operation.
  Enumeration and dispatch time may scale with pending backlog. Handler
  completion remains asynchronous through the same runner behavior as original
  execution.
- **Throughput**: Invoked once at each server boot after runtimes bind and may
  also be invoked explicitly. Work scales with pending entries older than the
  grace threshold.
- **Consistency/correctness invariants**:
  - Only obligated runs strictly older than the grace window are resumed.
  - Young obligated runs remain pending and are not executed.
  - Completed and failed runs are never re-executed.
  - Re-execution uses the same handler/outcome path as original execution.
  - The sweep does not create a new emission id or run id.
  - One summary log line reports resumed, pending, and failed-terminal counts.
  - No timer/backoff loop is installed.
- **Data growth and scale**: Read volume is proportional to current pending
  entries. Repeated sweeps do not add one run per attempt.
- **Concurrency behavior**:
  - Multiple server JVMs may sweep the same run concurrently.
  - A normal handler and a sweep may overlap.
  - Duplicate execution is tolerated by the handler; the run outcome still
    obeys first-terminal-wins.
- **Edge cases**:
  - Empty pending produces zero resumed and a valid summary.
  - At exactly `age == grace-ms`, “older than” means not yet eligible.
  - `grace-ms = 0` admits only entries whose obligation time is strictly
    earlier than the sweep's comparison instant.
  - Negative, non-numeric, or overflowed grace values are invalid input rather
    than a request to sweep everything.
  - Missing retained obligation data is corrupt run truth: log/refuse that
    candidate without inventing a handler call.

### 10. Emit episode-turn-closed before the terminal turn-cell write

- **Latency**: This occurs in the turn-end waiter, before the existing terminal
  status write. It may pay the durable obligation acknowledgement latency but
  not handler latency. Cascade unavailability is total and cannot strand the
  turn-end path.
- **Throughput**: One emission attempt per terminal close observation. Volume
  scales with episode turns, not transcript lines.
- **Consistency/correctness invariants**:
  - Emission occurs before the `:open` to terminal cell write.
  - The payload contains exactly the bounded close-time facts:
    `turn-id`, `terminal-status`, `exit-code`, `duration-ms`, `episode-id`,
    `fresh?`, `seed?`, `thread-id`, `conversation-id`, `cwd`, `time-ms`, and
    close-time `jsonl-exists?`.
  - It never contains `:lines`.
  - Emission is unconditional with respect to episode-retry eligibility. The
    handler, not the emitter, declines.
  - If the process dies after obligation acknowledgement but before the turn
    cell write, recovery remains correct using the emission payload.
  - Failure to emit degrades to today's turn-closing behavior and is logged; it
    does not suppress the terminal cell write.
- **Data growth and scale**: One bounded emission/run identity per distinct
  `(turn-id, terminal-status)` and matching durable row.
- **Concurrency behavior**: Replayed close handling for the same terminal
  result re-derives the same identity. A conflicting terminal result is a
  separate emission and may execute concurrently.
- **Edge cases**:
  - The file may appear after the close-time observation; the handler must
    re-check.
  - The terminal cell write may never happen after a successful emission; the
    repair is still valid.
  - The terminal cell may already be visible because of replay; terminal run
    convergence is unaffected.

### 11. Execute the episode-retry handler

- **Latency**: Asynchronous relative to turn closure. The durable request it
  issues must be acknowledged before runtime cleanup; seconds are tolerable,
  but no unbounded payload work or transcript scan belongs here.
- **Throughput**: Executions scale with failed/timeout fresh turns without a
  jsonl file, plus duplicate/recovery attempts. Healthy turn closures still
  invoke the handler but decline cheaply.
- **Consistency/correctness invariants**:
  - The handler declines with a bounded `:skipped` receipt unless status is
    failed or timeout, the episode was fresh, and no jsonl existed at close.
  - Immediately before repair it resolves the file from execution-time
    `(cwd, episode-id)` and declines if it now exists.
  - Durable defunct truth is requested and acknowledged before runtime lane
    cleanup.
  - All durable value bytes derive from the emission payload; `:marked-at-ms`
    is the turn's `:time-ms`, never replay-time wall clock.
  - The actor is the system actor, never the user actor.
  - The marker has its own import identity and never overwrites or reuses the
    turn cell's identity.
  - Only after durable repair acceptance does the handler compare-and-remove
    this JVM's lane entry, and only if it still names the dead episode id.
  - A normal `:skipped` return is a completed run with a skipped receipt; a
    thrown/failed repair is a failed run.
- **Data growth and scale**: At most one durable defunct fact per episode id;
  replays are journaled no-ops. No transcript or jsonl contents are stored in
  the run or marker.
- **Concurrency behavior**:
  - Duplicate handlers in one or several JVMs may issue the same durable
    request; identical bytes converge.
  - Each JVM may clean only its own runtime atom.
  - A JVM whose atom already moved to a newer episode does not remove it.
- **Edge cases**:
  - `:completed`, cancellation-like statuses outside failed/timeout,
    non-fresh, or file-present-at-close cases decline.
  - File absent at close but present at execution declines.
  - File existence check failure must be total and conservative: it cannot
    establish a defunct marker on an unverified assumption of absence.
  - A fingerprint conflict is not a successful repair; runtime cleanup cannot
    proceed as though the durable marker converged.
  - Process death after durable marker acceptance but before runtime cleanup is
    safe because adoption consults durable truth.
  - Process death after both handler effects but before outcome observation is
    safe because a later execution converges and re-observes an outcome.

### 12. Upsert the durable episode-chain-defunct marker

- **Latency**: This is an acknowledged durable request in the handler path.
  It is not on the original turn's response path.
- **Throughput**: One attempted request per eligible execution, with duplicates
  converging on the same import identity.
- **Consistency/correctness invariants**:
  - The marker value is exactly `world-id`, `lane-id`, `episode-id`, and
    payload-derived `marked-at-ms`.
  - Identical identity and bytes are accepted as a journaled no-op.
  - Same identity with different bytes is a fingerprint conflict, never an
    overwrite.
  - Existing turn cells are byte-untouched.
  - Unasked projection readers do not surface the new entry kind.
- **Data growth and scale**: One marker identity per episode id. The number of
  markers grows with distinct defunct episodes and is not deleted by R2.
- **Concurrency behavior**: Concurrent identical requests converge. A
  conflicting writer loses via existing fingerprint rules rather than
  last-write-wins.
- **Edge cases**: Missing world/lane/episode/time values make the request
  invalid. Reusing a lane id later does not erase the historical marker; the
  time-scoped adoption rule prevents that marker from poisoning newer turns.
  A later eligible failure of a revalidated same-id episode is the unresolved
  contract fork recorded below.

### 13. Compare-and-remove the runtime lane entry

- **Latency**: In-memory and immediate after durable marker acknowledgement.
- **Throughput**: At most one compare-and-remove attempt per eligible handler
  execution in that JVM.
- **Consistency/correctness invariants**:
  - Remove only when the current lane entry still names the payload's dead
    episode id.
  - Missing entry is a successful no-op.
  - A different/newer episode entry is preserved.
  - The operation cannot affect another JVM's atom.
- **Data growth and scale**: No durable growth; removal reduces or preserves
  bounded runtime state.
- **Concurrency behavior**: The compare and removal are one conditional
  runtime transition. Competing local updates cannot be erased merely because
  the lane key matches.
- **Edge cases**: A swept duplicate in a fresh JVM sees an empty atom and does
  nothing. A local lane that advanced between durable repair and cleanup keeps
  the advanced entry.

### 14. Resolve the current episode through the adoption read

- **Latency**: This is on the next-turn decision path. It should preserve the
  existing interactive behavior; additional marker filtering must not turn a
  bounded lane read into an unbounded history scan.
- **Throughput**: One resolution per episode turn attempt.
- **Consistency/correctness invariants**:
  - Runtime warm state remains a hint, not a replacement for durable adoption
    truth.
  - Durable fallback excludes a turn cell when its episode id has a defunct
    marker and `cell.time-ms <= marked-at-ms`.
  - A later cell for the same episode id with
    `cell.time-ms > marked-at-ms` is adoptable again.
  - Filtering happens only at the read layer; pure episode-decision semantics
    do not change.
  - Marker-read failure degrades to today's exact adoption behavior rather
    than failing the turn path.
- **Data growth and scale**: The dominant access remains the lane's relevant
  turn/marker history. The read must not scan unrelated conversations.
- **Concurrency behavior**: A read racing marker creation may see pre-marker
  behavior once; after marker visibility, old cells are excluded. Runtime
  cleanup racing this read is safe because durable marker truth is decisive on
  the fallback path.
- **Edge cases**:
  - With no marker, behavior is unchanged.
  - With all candidate cells filtered, resolution behaves as no adoptable
    episode and the next turn mints fresh.
  - A later valid turn re-establishes adoption without deleting the marker.
  - Marker-read failure must not be misreported as proof that no marker exists;
    it is a deliberate total fallback.

### 15. Invoke recovery during server boot

- **Latency**: Invocation occurs after all required runtimes bind. It must not
  be a namespace-load side effect or a permanent boot poison.
- **Throughput**: Exactly one explicit invocation per server boot. Multiple
  server JVMs each legitimately invoke their own sweep against shared truth.
- **Consistency/correctness invariants**:
  - Classload alone starts no runtime and performs no sweep.
  - A lazily booted runtime handle is total even after boot failure; dereference
    cannot cache and rethrow a permanent exception on every turn.
  - Boot invocation uses the same grace and recovery semantics as an explicit
    sweep.
- **Data growth and scale**: Boot does not create new run identities merely by
  enumerating them.
- **Concurrency behavior**: Simultaneous boots may duplicate handler
  execution, which the durable row contract already tolerates.
- **Edge cases**: Runtime-unavailable boot logs failure and leaves obligations
  pending for a future explicit/boot sweep; it does not run handlers before
  their context exists.

## State transitions and crash windows

### Durable run lifecycle

1. No run exists.
2. A matching durable dispatch writes an obligation.
3. Only after the obligation is acknowledged and readable does the run become
   externally dispatchable and the handler start.
4. While no terminal observation is durable, the run remains obligated and
   appears pending.
5. Handler return records completed; handler throw records failed.
6. The first terminal outcome permanently removes pending membership.
7. Replayed obligations cannot leave terminality. Later conflicting outcomes
   only increase the late-observation count.

Crash/failure placement has the following required outcomes:

- **Before obligation acknowledgement**: no handler effect, no durable receipt;
  caller continues after a total logged failure.
- **After obligation acknowledgement, before handler start**: obligated and
  pending; eligible for recovery after grace.
- **During handler before durable marker acceptance**: obligated until an
  outcome is observed; a throw may make it terminal failed.
- **After marker acceptance, before runtime cleanup**: durable adoption is
  already healed; re-execution converges the marker and retries harmless local
  cleanup.
- **After runtime cleanup, before completed observation**: run is still
  obligated; re-execution converges the marker and no-ops the local cleanup.
- **After first terminal, before an obligation replay**: replay is ignored for
  state purposes; terminal status and empty pending membership remain.

### Episode-retry lifecycle

1. Every turn closure emits unconditionally.
2. Healthy/non-fresh/file-present cases complete with a skipped receipt and
   perform no repair.
3. Failed/timeout + fresh + absent-at-close proceeds to execution-time file
   verification.
4. File now present completes skipped.
5. File still absent requests the deterministic defunct marker.
6. Marker acceptance precedes compare-and-remove of the matching local runtime
   lane entry.
7. Successful handler return completes the run.
8. On the next episode resolution, old/equal-time cells for that episode are
   filtered; no adoptable cell means fresh episode creation.
9. A later successful turn cell with a greater time makes that episode id
   adoptable again.

### Required refusals preserved

- No clock pulse, timer, scheduler, retry backoff, or automatic resweep of
  failed runs.
- No automatic resident respawn.
- No migration of autotag to durable execution.
- No durable/editable declaration table and no rows-as-material.
- No cross-boundary resume.
- No new relation kind and no change to existing organ-owned durable
  vocabulary, serve contracts, or cell shapes.
- No external handler effect in the durable run-truth write path.
- No change to pure episode-decision behavior.

## Entity State × Write Matrix

All related reads are listed in every matrix row. “Run read” means the single
run query; “pending read” means pending enumeration; “failed read” means failed
enumeration; “sweep read” means eligibility in explicit recovery.

### Entity: durable cascade run aggregate

This entity includes the one run row and its pending-membership truth because
the contract requires them to change as one observable transition. The matrix
therefore covers writes to both, not merely the status field.

States:

- **R0 absent**: no acknowledged obligation exists.
- **R1 obligated-young**: acknowledged and non-terminal, but not older than
  the grace window.
- **R2 obligated-old**: acknowledged, non-terminal, and strictly older than
  the grace window.
- **R3 completed**: completed was the first terminal outcome.
- **R4 failed**: failed was the first terminal outcome.

Write operations:

- **W1 obligation**: record/replay the deterministic obligation.
- **W2 completed observation**: record completed with bounded receipt.
- **W3 failed observation**: record failed with bounded receipt.

#### R0 absent × W1 obligation

- **Run read**: one obligated run with retained obligation data.
- **Pending read**: contains the run once.
- **Failed read**: does not contain the run.
- **Sweep read**: ineligible until its stored obligation time becomes older
  than grace.

#### R0 absent × W2 completed observation

- **Run read**: remains not-found; an orphan observation is malformed because
  the public protocol requires acknowledged obligation before handler call.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent.

#### R0 absent × W3 failed observation

- **Run read**: remains not-found for the same fail-closed orphan rule.
- **Pending read**: absent.
- **Failed read**: absent; failure history cannot be fabricated without the
  obligation identity/context.
- **Sweep read**: absent.

#### R1 obligated-young × W1 obligation

- **Run read**: still one obligated run; duplicate delivery cannot create a
  second identity or terminalize it.
- **Pending read**: still contains the run exactly once.
- **Failed read**: absent.
- **Sweep read**: eligibility is computed from the one stored obligation time;
  no duplicate pending entry exists.

#### R1 obligated-young × W2 completed observation

- **Run read**: completed with this first terminal receipt.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent permanently.

#### R1 obligated-young × W3 failed observation

- **Run read**: failed with this first terminal receipt.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent permanently; failed is never retried.

#### R2 obligated-old × W1 obligation

- **Run read**: still one obligated run; replay cannot duplicate it.
- **Pending read**: still contains the run exactly once.
- **Failed read**: absent.
- **Sweep read**: remains governed by the single current stored obligation
  time; concurrent handler execution is allowed, but duplicate pending
  identities are not.

#### R2 obligated-old × W2 completed observation

- **Run read**: completed with this first terminal receipt.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent; a sweep that enumerated it earlier may finish a
  duplicate handler, whose later observation cannot change the winner.

#### R2 obligated-old × W3 failed observation

- **Run read**: failed with this first terminal receipt.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent; no subsequent sweep retries it.

#### R3 completed × W1 obligation

- **Run read**: unchanged completed outcome and receipt.
- **Pending read**: absent; the run is not resurrected.
- **Failed read**: absent.
- **Sweep read**: absent.

#### R3 completed × W2 completed observation

- **Run read**: unchanged first completed outcome; exact duplicate cannot
  replace its receipt.
- **Pending read**: absent.
- **Failed read**: absent.
- **Sweep read**: absent.

#### R3 completed × W3 failed observation

- **Run read**: remains completed; conflicting-late count increases and the
  first completed receipt remains authoritative.
- **Pending read**: absent.
- **Failed read**: absent because failed did not win terminality.
- **Sweep read**: absent.

#### R4 failed × W1 obligation

- **Run read**: unchanged failed outcome and receipt.
- **Pending read**: absent; the run is not resurrected.
- **Failed read**: still contains the run once.
- **Sweep read**: absent permanently.

#### R4 failed × W2 completed observation

- **Run read**: remains failed; conflicting-late count increases and the first
  failed receipt remains authoritative.
- **Pending read**: absent.
- **Failed read**: still contains the run once.
- **Sweep read**: absent.

#### R4 failed × W3 failed observation

- **Run read**: unchanged first failed outcome; exact duplicate cannot replace
  its receipt.
- **Pending read**: absent.
- **Failed read**: contains the run once.
- **Sweep read**: absent.

### Entity: durable episode-chain-defunct marker aggregate

This entity includes the deterministic import/journal identity and the
projected marker cell produced by the acknowledged request. The matrix covers
both the request result and all related reads.

States:

- **M0 absent**: no marker/import identity exists for the episode.
- **M1 identical-present**: the deterministic identity exists with identical
  marker bytes.
- **M2 conflicting-present**: the deterministic identity exists with different
  bytes, which is a fingerprint conflict.

Write operation:

- **D1 defunct upsert**: issue the system-authored durable request for the
  payload-derived marker.

Related reads:

- **Marker read**: the projection read that asks for defunct markers.
- **Adoption read**: current-episode durable fallback.
- **Unasked projection read**: any existing reader whose entry-kind filter does
  not request the new kind.
- **Run read**: the handler's durable run outcome after the request result is
  observed.

#### M0 absent × D1 defunct upsert

- **Marker read**: returns exactly one marker with system provenance and the
  payload-derived value.
- **Adoption read**: excludes matching episode turn cells at or before
  `marked-at-ms`; later cells remain adoptable.
- **Unasked projection read**: unchanged; the new marker is invisible.
- **Run read**: may complete only after the request is accepted and local
  compare-and-remove returns; if a later handler step throws, the durable marker
  can exist beside a failed run.

#### M1 identical-present × D1 defunct upsert

- **Marker read**: still returns the same one marker, byte-identical.
- **Adoption read**: same time-scoped filtering as before; no widening.
- **Unasked projection read**: unchanged.
- **Run read**: the replay may complete as a journaled no-op; no duplicate
  marker or new run identity is created.

#### M2 conflicting-present × D1 defunct upsert

- **Marker read**: returns the pre-existing bytes; they are not overwritten.
- **Adoption read**: follows the pre-existing durable marker, not the rejected
  candidate.
- **Unasked projection read**: unchanged.
- **Run read**: the repair cannot claim a successful completed effect; the
  conflict is surfaced as handler failure, and runtime cleanup does not proceed
  as if convergence occurred.

### Entity: per-JVM runtime lane entry

States:

- **L0 absent**: this JVM has no entry for the lane.
- **L1 dead-match**: this JVM's entry names the dead episode from the payload.
- **L2 different-current**: this JVM's entry names another/newer episode.

Write operation:

- **L-W1 compare-and-remove** with expected dead episode id.

Related reads:

- **Runtime entry read**: direct in-JVM lane lookup.
- **Current-episode read**: the next episode resolution path.
- **Durable marker read**: defunct projection truth.
- **Run read**: durable outcome after handler completion/failure.

#### L0 absent × L-W1 compare-and-remove

- **Runtime entry read**: remains absent.
- **Current-episode read**: falls through to durable adoption; the defunct
  marker filters the old episode and permits fresh creation.
- **Durable marker read**: unchanged and present before this operation.
- **Run read**: cleanup is a successful no-op; the handler may complete.

#### L1 dead-match × L-W1 compare-and-remove

- **Runtime entry read**: becomes absent.
- **Current-episode read**: cannot resume from the removed warm hint; durable
  fallback also filters the old episode, so the next attempt is fresh.
- **Durable marker read**: unchanged and present.
- **Run read**: handler may complete after successful conditional removal.

#### L2 different-current × L-W1 compare-and-remove

- **Runtime entry read**: retains the different/newer episode.
- **Current-episode read**: may use or validate that newer entry under existing
  rules; the repair cannot roll it back.
- **Durable marker read**: unchanged and scoped to the dead episode/time.
- **Run read**: compare failure is a harmless no-op, not evidence that repair
  failed.

### Entity: episode turn cell at close

R2 does not alter the cell schema or the existing terminal-write semantics, but
it adds a required write-order dependency, so the states are included.

States:

- **T0 open**: the turn has its durable pre-agent open cell.
- **T1 same-terminal-present**: replay observes the already-written same
  terminal result.
- **T2 different-terminal-present**: another terminal result is already
  represented for the turn under the existing turn-record identity law.

Write operation:

- **T-W1 terminal close write**, preceded by the unconditional close emission.

Related reads:

- **Turn-record read**: existing turn history.
- **Adoption read**: current-episode fallback over turn cells.
- **Run read**: the cascade run derived from this close result.
- **Defunct marker read**: episode-retry repair truth.

#### T0 open × T-W1 terminal close write

- **Turn-record read**: sees the existing terminal representation if the write
  lands; if the JVM dies after emission and before this write, it may still see
  open while the run/repair proceeds.
- **Adoption read**: uses terminal history subject to defunct-marker filtering.
- **Run read**: the obligation is already acknowledged before this cell write
  is attempted, or the total emission failure was logged with no durable
  receipt.
- **Defunct marker read**: may become present asynchronously for an eligible
  failed/timeout close.

#### T1 same-terminal-present × T-W1 terminal close write

- **Turn-record read**: remains semantically the same under the existing
  idempotency law.
- **Adoption read**: unchanged except for any separately durable defunct marker.
- **Run read**: same `(turn-id, terminal-status)` resolves to the same run and
  converges.
- **Defunct marker read**: identical repair execution resolves to the same
  marker identity.

#### T2 different-terminal-present × T-W1 terminal close write

- **Turn-record read**: preserves the existing turn-record identity semantics;
  R2 does not collapse different terminal statuses into one identity.
- **Adoption read**: evaluates the resulting turn history and marker time scope
  without changing pure decision rules.
- **Run read**: the different terminal status produces a different emission
  and run.
- **Defunct marker read**: both runs may target the same episode marker cell;
  each carries its own transition-scoped import identity (ruled 2026-07-27),
  and their value bytes for the same turn are identical, so either order
  converges on the same visible marker.

## Contract ambiguity — RULED 2026-07-27 (option 2, transition-scoped)

Phase 0 stopped here on a genuine fork: the contract pinned the defunct
import identity per episode-id while its value bytes carried the failing
turn's `:time-ms`, and the adoption law deliberately permits same-id
revalidation — so a second eligible failure of a revalidated episode was a
fingerprint-conflict rejection, leaving the lane stranded again.

**Ruling (Fable, on referral per §6 / the work-package escalation path):
option 2.** The import identity is transition-scoped —
`sha("episode-defunct " episode-id " " turn-id " " (name terminal-status))`
— over the unchanged stable order key `ep-chain:<sha8(episode-id)>` and
unchanged value bytes. Same transition replays byte-identically; a later
eligible failure mints a new import that advances the one visible marker.
Advancement is causally monotone (a same-id fresh re-mint requires the
prior marker already visible to adoption; T6 + fingerprint-identical
no-op close the replay-reordering routes). This is the existing turn-cell
pattern (`episode.clj:464-489`). Contract §3f, T7, G3, G5 carry the ruled
text; this artifact is superseded by the fresh Phase 0 re-run the ruling
mandates.

## Data-retention and scale summary

- Run history, failed-run history, and defunct markers are append/update-only
  for R2 and therefore unbounded over the life of the land. No implicit cleanup
  is permitted.
- Pending truth is bounded by unresolved obligations operationally, not by a
  contract maximum. All-empty and backlog cases remain correct.
- Point lookup by run id must remain independent of total history.
- Pending enumeration scales with pending, not all terminal history.
- Failed enumeration is explicitly a low-volume console surface and may scale
  with failed history; R2 defines neither pagination nor a stable sort order.
- Every write's size is bounded by a single row/emission and never by an
  unbounded external collection.

## Correctness summary

- **Recorded first**: no episode-retry handler effect before acknowledged
  obligation truth.
- **First terminal wins**: terminal status/receipt never regress; conflicting
  late results are counted.
- **Recovery-safe effects**: marker upsert is deterministic and runtime cleanup
  is conditional/local.
- **Time-scoped adoption**: defunct means “this episode as evidenced at or
  before this failed turn,” not “this episode id forever.”
- **Truth separation**: run truth records obligation/outcome; the defunct
  marker records episode repair; the runtime atom is disposable local state.
- **Totality**: cascade/runtime/marker-read failures cannot make the turn lane
  unavailable, though a failed durable repair remains truthfully enumerable.
- **No hidden activation**: load does nothing; boot invokes one explicit sweep;
  no clock or background retry policy is introduced.
