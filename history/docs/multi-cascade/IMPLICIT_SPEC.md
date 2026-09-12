# Multi-cascade R2 — Phase 0 implicit specification

Status: complete Phase 0 requirements analysis, derived fresh on 2026-07-27
from `CONTRACT_R2.md` and the current source tree. This artifact does not
design depots, PStates, partitioning, or topologies. `CONTRACT_R2.md` remains
authoritative if any wording here is weaker.

## Phase boundary and verified ground

- The product contract is: record a durable cascade obligation before calling
  its handler; observe the handler outcome durably; recover old unobserved
  obligations explicitly after JVM death; and use that mechanism to heal a
  fresh episode whose CLI process died before its jsonl file existed.
- The R1 best-effort row and dispatch behavior remain byte-behavior-identical.
  R2 adds one durable row; it does not migrate row #1.
- A successful durable dispatch receipt means the obligation is already
  queryable, not that the handler succeeded.
- First terminal outcome wins. Later contradictory outcomes are diagnostic
  evidence only and never regress the run or re-pend it.
- The episode repair is a durable, transition-scoped upsert followed by a
  JVM-local compare-and-remove. It does not respawn an agent.
- `decide-episode` remains pure and unchanged. Defunct filtering belongs only
  to `current-episode!`'s durable fallback read.
- The sweep is explicit boot work, not a namespace-load effect and not a
  scheduler, clock, backoff loop, or failed-run retry policy.
- Manifest substance matched disk. One navigation hint drifted:
  `test/app/test_runner.clj`'s fail-closed inventory is now at lines 217–250,
  rather than the §8 hint 214–247. Its discovery, exact-one-tier
  classification, duplicate rejection, and stale-classification rejection
  semantics are unchanged. No other locator-only drift was found.

## Domain entities and logical states

1. **Cascade declaration row** — inert, enumerable code data. A row is
   best-effort (explicitly or by absence of `:cascade/runner`) or durable.
2. **Emission** — one occurrence of a trigger, identified globally. For
   `:episode/turn-closed`, identity is a deterministic function of turn id and
   terminal status.
3. **Cascade run** — one row × emission obligation, globally identified.
   Logical states are missing, obligated, completed, or failed. Completed and
   failed are terminal. A terminal row may additionally carry one
   `:late-conflict` evidence value.
4. **Pending membership** — the set of currently obligated, nonterminal runs,
   with the obligation time used by the grace test.
5. **Episode turn cell** — the existing durable per-turn cell, transitioning
   from absent to open to a terminal status.
6. **Episode-chain defunct marker** — one stable cell per episode id. Its value
   marks the latest eligible failed transition whose earlier-or-equal turn
   cells must not be adopted.
7. **Lane runtime entry** — JVM-local `{episode-id, last-turn-ms}` state.
8. **Cascade module deployment** — absent, running, unavailable, or restarted
   while durable state remains.

Logical growth:

- The declaration table is tiny and bounded.
- Run history and failed-run history grow with durable emissions and are not
  deleted by R2.
- Pending membership contains only nonterminal obligations and should remain
  small under normal handler completion and boot sweeps.
- There is at most one visible defunct marker cell per episode id, although
  each eligible failure has its own durable import identity/history.
- Turn cells grow with turns and remain readable history.

## Global correctness invariants

- `emission-id =
  "casc-em:episode-turn-closed:" + sha256(turn-id NUL terminal-status)` for
  episode close emissions.
- `run-id = "casc-run:" + sha256(cascade-id NUL emission-id)`. No lifecycle
  transition mints a replacement run id.
- Identity bytes are durable protocol. Refactors may not change emission,
  run, or defunct-import identity derivations.
- A replay of the same obligation cannot create a second logical run.
- An obligation delivered after a terminal observation cannot resurrect or
  re-pend that run.
- The first terminal observation fixes the run's status and primary receipt.
  A later observation with the same status is a no-op. A later observation
  with the other status changes only `:late-conflict` to that observation's
  bounded `{status, receipt, observed-at-ms}` value. Reprocessing the same
  conflicting record converges to the same value; no counter is permitted.
- Completed and failed are terminal. Failed runs are enumerable but never
  selected by the sweep.
- A durable handler may execute concurrently or more than once. Its externally
  meaningful effect must therefore converge independently of run status.
- The obligation acknowledgment completes before handler execution begins.
- Durable receipts are minted only after that acknowledgment and carry exactly
  `:cascade/id`, `:dispatched? true`, `:durable? true`, `:emission/id`, and
  `:run/id`.
- Handler completion or throw produces a bounded terminal observation. A throw
  never escapes to the turn caller.
- Production durable payloads are bounded EDN scalars/maps. They contain no
  functions, runtime handles, streams, or `:lines`.
- A durable-emission failure logs `[CASCADE][EMIT-FAILED]`, does not mint a
  successful durable receipt, and does not fail the user's turn. Successful
  dispatch waits only for obligation materialization, never handler completion.
- Namespace loading starts no runtime and runs no sweep.
- Existing projection readers do not see `:episode-chain-defunct` unless they
  explicitly ask for that entry kind.

## Operations

### O1. Enumerate and validate cascade rows

- **Input/output:** `(cascade/rows)` returns declaration-order printable EDN.
  R2 returns row #1 unchanged followed by `:cascade/episode-retry`.
- **Latency/throughput:** in-memory, single-digit milliseconds; called by every
  emission and by diagnostics/tests. Table size is bounded.
- **Correctness:** ids are unique; effect classes are in the closed vocabulary;
  an absent runner means best-effort; a durable row must have a nonblank
  idempotency story naming scope, transition law, and duplicate tolerance.
- **Edges:** an invalid durable declaration is refused by the grammar scan; a
  row on an unmatched trigger remains inert.

### O2. Dispatch a best-effort row

- **Input/output:** `(cascade/react! ctx trigger payload)` preserves R1's
  declaration-order immediate `{:cascade/id ..., :dispatched? true}` receipt.
- **Latency/throughput:** returns without waiting for handler resolution or
  completion; volume is matching rows × emissions.
- **Correctness:** one future per row; sibling failure isolation; resolution
  occurs inside the future; `[CASCADE]` and `[CASCADE][FAILED]` facts retain
  their R1 meaning.
- **Edges:** row #1's literal guard remains in `run-ambient-autotag!`:
  `(and (nil? gold-receipt) rk-rt)`. False and nil retain their distinct old
  truth behavior. The existing canned `:lines` seam remains test-only.

### O3. Mint episode-close emission and run identities

- **Input/output:** the emitter derives one emission id from `(turn-id,
  terminal-status)` and one run id from `(cascade-id, emission-id)`.
- **Latency/throughput:** pure CPU work on every terminal episode turn.
- **Correctness:** exact replay is byte-identical; a different terminal status
  for the same turn is a different emission and run; no wall clock participates
  in identity.
- **Edges:** missing/invalid discriminators must not be allowed to fabricate a
  repair identity. Production always supplies a string turn id and a named
  terminal status.

### O4. Record and acknowledge a durable obligation

- **Input/output:** one bounded obligation for each matching durable row; after
  acknowledgment, `cascade/run` reads `:obligated` and
  `cascade/pending-runs` includes it.
- **Latency/throughput:** interactive; the acknowledgment may add normal Rama
  processing latency, but no handler latency. Volume is durable rows ×
  emissions.
- **Correctness:** record first, call second; same run id remains one run;
  terminal rows are never overwritten by obligation replay.
- **Edges:** duplicate delivery while obligated stays obligated; replay after
  terminal is ignored; failure is total/logged and cannot block or fail the
  episode lane.

### O5. Execute a durable handler and record its observation

- **Input/output:** execute from the durable obligation with execution-time
  context; append `:completed` on return or `:failed` on throw, with a bounded
  receipt.
- **Latency/throughput:** asynchronous relative to `react!`; handler duration
  may be seconds. Concurrency includes two JVMs executing the same run.
- **Correctness:** first terminal wins; pending disappears atomically with
  terminalization; duplicate same-status observations converge; contradictory
  terminal observations update only the last conflict evidence.
- **Edges:** an observation that is processed before its obligation still
  establishes terminal truth; a later obligation cannot resurrect it.

### O6. Read one run

- **Input/output:** `(cascade/run ctx run-id)` returns the one run row or nil.
- **Latency/throughput:** interactive point read; used by receipts, console,
  gates, and recovery diagnosis.
- **Correctness:** it exposes obligated/terminal status, primary receipt, and
  optional late-conflict without deriving a new identity.
- **Edges:** missing id returns nil; conflict evidence never replaces the first
  terminal status.

### O7. Enumerate pending runs

- **Input/output:** `(cascade/pending-runs ctx)` returns every and only
  nonterminal obligated run with enough obligation data to resume it.
- **Latency/throughput:** administrative range read; one invocation may inspect
  every partition. Pending is expected to stay small.
- **Correctness:** completed and failed runs are absent; a run is visible after
  obligation acknowledgment.
- **Edges:** empty state returns an empty collection; ordering is not semantic.

### O8. Enumerate failed runs

- **Input/output:** `(cascade/failed-runs ctx)` returns every run whose first
  terminal status is failed.
- **Latency/throughput:** administrative enumeration; history can grow without
  bound, so callers must not assume a tiny lifetime set.
- **Correctness:** a completed run with a later failed conflict is not a failed
  run; a failed run with a later completed conflict remains failed.
- **Edges:** empty state returns empty; failed runs remain queryable and are
  never deleted or retried by R2.

### O9. Resume old obligated runs

- **Input/output:** `(cascade/resume-obligated! ctx {:grace-ms 60000})` reads
  pending state, re-executes only entries older than the nonnegative grace
  window through the same handler/observation path, and logs one
  `[CASCADE][SWEEP]` receipt containing resumed, pending, and failed-terminal
  counts.
- **Latency/throughput:** boot/admin work; seconds are acceptable. It must not
  run on a request thread or namespace load.
- **Correctness:** young obligations stay pending; failed/completed runs are
  never executed; exact-boundary entries are not “older than” the window yet.
- **Edges:** empty pending is valid; duplicate boot sweeps across JVMs are safe;
  handler throws terminalize failed; there is no self-firing retry/backoff loop.

### O10. Emit `:episode/turn-closed`

- **Input/output:** the waiter emits unconditionally with scalar payload
  `{turn-id, terminal-status, exit-code, duration-ms, episode-id, fresh?,
  seed?, thread-id, conversation-id, cwd, time-ms, jsonl-exists?}`.
- **Latency/throughput:** once per terminal waiter callback.
- **Correctness:** emission occurs before the existing open→terminal turn-cell
  overwrite. `jsonl-exists?` is observed at close through
  `episode-jsonl-file`.
- **Edges:** JVM death after emission but before the turn-cell overwrite still
  leaves a repairable obligation; JVM death before the obligation ack may lose
  this best-effort emission but may never kill the turn lane.

### O11. Repair a stranded episode lane

- **Input/output:** `repair-stranded-lane!` returns `:skipped` unless status is
  failed/timeout, the turn was fresh, and no jsonl existed at close. Before
  writing, it rechecks the file is still absent. An eligible repair awaits one
  system-authored object-container import, then compare-removes local lane
  runtime state.
- **Latency/throughput:** rare failure path; the durable request may take
  hundreds of milliseconds. It is safe under duplicate/concurrent execution.
- **Correctness:** stable order key
  `ep-chain:<sha8(episode-id)>`; transition-scoped import key
  `sha("episode-defunct " episode-id " " turn-id " "
  (name terminal-status))`; replay-stable value
  `{world-id, lane-id, episode-id, marked-at-ms}` with `marked-at-ms =
  payload.time-ms`; actor id `"system:episode-retry/v1"`, actor type `:system`.
- **Edges:** healthy close, nonfresh failure, file-at-close, or file-now-present
  skips. Missing identity fields cannot authorize a fabricated marker. Rejected
  durable import or any throw becomes a failed cascade observation. The handler
  never respawns the resident.

### O12. Adopt the current episode

- **Input/output:** `current-episode!` keeps its order: local runtime entry,
  then durable turn cells, then the existing file belt. On the durable fallback
  only, it excludes a turn cell when the same episode is marked defunct and
  `turn.time-ms <= marker.marked-at-ms`, then passes the newest survivor to
  unchanged `decide-episode`.
- **Latency/throughput:** interactive turn-start read. Normal warm-atom turns do
  no added durable read.
- **Correctness:** later cells for the same episode id
  (`time-ms > marked-at-ms`) revalidate it. Marker-read failure degrades to the
  exact pre-R2 behavior.
- **Edges:** no surviving turn produces the virgin/fresh decision; a different
  episode id is not filtered by another episode's marker; no cross-boundary
  resume is introduced.

### O13. Second eligible failure of the same episode id

1. Failure A at time A lands marker A with import identity A.
2. Adoption filters cells for that episode at or before A and may mint/reuse
   that same episode id fresh.
3. A later cell at time B>A revalidates the episode id.
4. Eligible failure B mints a different transition import identity B but writes
   the same stable marker cell, advancing `marked-at-ms` to B.
5. Adoption now filters the second dead interval through B.
6. A replay of A is an already-journaled no-op and cannot rewrite marker B.
   Replays of B are byte-identical no-ops.

This lifecycle is mandatory: revalidation is repairable repeatedly, never
single-use, and an older replay cannot regress the marker.

### O14. Boot and deploy

- The new module is the sixth deployed module. Environment identity and module
  list are attested before deployment.
- Deployment is additive and settles each module to server-read RUNNING before
  continuing. Existing five modules remain RUNNING.
- The explicit sweep is invoked once per server boot only after runtimes bind.
  Multiple server JVMs may each invoke it; duplicate execution remains safe.
- Worker restart preserves durable run/pending truth and consumed progress; it
  does not require depot-history replay.
- Module deploy failure or a license/node-count question is a package stop, not
  a Phase-0 policy decision.

## State transitions

```text
run missing
  -- obligation --> obligated
  -- completed observation --> completed
  -- failed observation --> failed

obligated
  -- obligation replay --> obligated
  -- completed observation --> completed, not pending
  -- failed observation --> failed, not pending

completed
  -- obligation replay --> completed, not pending
  -- completed observation --> unchanged
  -- failed observation --> completed + latest failed late-conflict

failed
  -- obligation replay --> failed, not pending
  -- failed observation --> unchanged
  -- completed observation --> failed + latest completed late-conflict
```

```text
fresh failed/timeout turn, no jsonl
  -> close emission
  -> acknowledged obligation
  -> eligible repair
  -> transition-scoped defunct import
  -> stable marker cell at turn time
  -> compare-remove matching JVM-local lane entry
  -> next adoption excludes cells through marker time
  -> fresh episode decision
```

## Entity state × write matrices

The matrices enumerate every related read for every row. For cascade runs:
`run` means `cascade/run`; `pending` means `cascade/pending-runs`; `failed`
means `cascade/failed-runs`; `sweep` means sweep selection.

### Cascade run and pending membership

| Entity state | Write | Result | `run` read | `pending` read | `failed` read | `sweep` read |
|---|---|---|---|---|---|---|
| Missing | obligation | Obligated | Obligated row | Includes run | Excludes run | Selects only after grace |
| Missing | completed observation | Completed first-terminal row | Completed | Excludes run | Excludes run | Never selects |
| Missing | failed observation | Failed first-terminal row | Failed | Excludes run | Includes run | Never selects |
| Obligated, within grace | obligation replay | Still obligated; replayed obligation data is current | Obligated | Includes run | Excludes run | Not yet selected |
| Obligated, within grace | completed observation | Completed; pending removed | Completed | Excludes run | Excludes run | Never selects |
| Obligated, within grace | failed observation | Failed; pending removed | Failed | Excludes run | Includes run | Never selects |
| Obligated, older than grace | obligation replay | Still obligated; grace is evaluated from the resulting obligation time | Obligated | Includes run | Excludes run | Selects iff resulting age is over grace |
| Obligated, older than grace | completed observation | Completed; pending removed | Completed | Excludes run | Excludes run | Never selects |
| Obligated, older than grace | failed observation | Failed; pending removed | Failed | Excludes run | Includes run | Never selects |
| Completed, no conflict | obligation replay | No change; no resurrection | Completed | Excludes run | Excludes run | Never selects |
| Completed, no conflict | completed observation | No change | Completed, no conflict | Excludes run | Excludes run | Never selects |
| Completed, no conflict | failed observation | Base remains completed; failed evidence becomes `:late-conflict` | Completed + failed conflict | Excludes run | Excludes run | Never selects |
| Completed, conflict present | obligation replay | No change | Completed + existing conflict | Excludes run | Excludes run | Never selects |
| Completed, conflict present | completed observation | No change to first terminal or conflict | Completed + existing conflict | Excludes run | Excludes run | Never selects |
| Completed, conflict present | failed observation | Base stays completed; conflict value becomes this last failed observation | Completed + latest failed conflict | Excludes run | Excludes run | Never selects |
| Failed, no conflict | obligation replay | No change; no resurrection | Failed | Excludes run | Includes run | Never selects |
| Failed, no conflict | failed observation | No change | Failed, no conflict | Excludes run | Includes run | Never selects |
| Failed, no conflict | completed observation | Base remains failed; completed evidence becomes `:late-conflict` | Failed + completed conflict | Excludes run | Includes run | Never selects |
| Failed, conflict present | obligation replay | No change | Failed + existing conflict | Excludes run | Includes run | Never selects |
| Failed, conflict present | failed observation | No change to first terminal or conflict | Failed + existing conflict | Excludes run | Includes run | Never selects |
| Failed, conflict present | completed observation | Base stays failed; conflict value becomes this last completed observation | Failed + latest completed conflict | Excludes run | Includes run | Never selects |

### Episode-close emission identity

Related reads are the run, pending, and failed APIs for the derived run id.

| Prior emission state | Write/emission | Identity/result | Run read | Pending read | Failed read |
|---|---|---|---|---|---|
| Never emitted | Close with status S | New deterministic emission/run | Obligated after ack, then terminal | Includes only while obligated | Includes only if first terminal fails |
| Same `(turn,S)` emitted | Exact replay | Same emission/run; no second logical run | Existing state | Mirrors existing state | Mirrors existing first terminal |
| `(turn,S1)` emitted | Close with different S2 | New emission and new run | Two independently readable runs | Each pending independently | Each classified by its own first terminal |
| Invalid/missing discriminator | Attempted emission | No fabricated identity or repair | No new valid run | No new entry | No new entry |

### Episode-chain defunct marker

Related reads are the explicit marker/projection read, `current-episode!`, and
all pre-existing projection readers.

| Marker/import state | Repair write | Marker read | `current-episode!` | Existing projection readers |
|---|---|---|---|---|
| No marker, transition A unseen | Eligible A | One cell at A; journal A recorded | Filters same-episode turns at/before A | Ignore new entry kind |
| No marker, ineligible transition | Handler skips | No marker | Pre-R2 adoption | Unchanged |
| Marker A, exact A replay | Same byte-identical import | Still A; journaled no-op | Same filter through A | Unchanged |
| Marker A, later same-id turn B>A exists | No repair yet | Still A | B survives and revalidates episode id | Unchanged |
| Marker A, eligible later transition B | New transition-scoped import B | Same cell advanced to B | Filters same-episode turns through B | Ignore new entry kind |
| Marker B, old A replay | Already-seen import A | Still B; no projection rewrite | Filter cannot regress from B | Unchanged |
| Marker B, exact B replay | Same byte-identical import | Still B | Same filter through B | Unchanged |
| Any marker, reused import identity with different bytes | Fingerprint-conflicting write | Existing marker unchanged; durable rejection | Existing filter unchanged | Unchanged |
| Any marker, file now exists at execution | Handler skips before write | Existing marker unchanged | Existing marker semantics only | Unchanged |
| Marker read fails | No marker write involved | Read unavailable | Degrades to exact pre-R2 adoption | Unchanged |

### Lane runtime entry

Related reads are `current-episode!` and the resulting `summon-argv` choice.

| Runtime state | Write | Result | `current-episode!` | `summon-argv` |
|---|---|---|---|---|
| Missing | `note-episode-turn!` for E | Entry E installed | Uses warm E next turn | `--resume E` while within boundary |
| Missing | Repair compare-remove E | No-op | Falls back to filtered durable turns | Fresh/resume follows fallback decision |
| Names dead E | `note-episode-turn!` for E/new time | Entry refreshed | Uses refreshed E until repair | Follows refreshed decision |
| Names dead E | Repair compare-remove E | Entry removed | Falls back; marker excludes dead interval | Fresh if no later survivor |
| Names different E2 | `note-episode-turn!` for E3 | Replaced by normal spawn stamp E3 | Uses E3 | Follows E3 decision |
| Names different E2 | Repair compare-remove E | No-op | Preserves E2 | Does not disturb E2 |
| Names E with later revalidating turn time | Repair compare-remove E | Removed because id matches; durable fallback still sees later turn > marker | Re-adopts valid later E | May safely resume E |
| Other JVM has no matching entry | Duplicate repair E | No-op in that JVM | Uses its own fallback | Independent and safe |

### Episode turn cell

Related reads are `read-turn-records`, the marker-aware fallback in
`current-episode!`, and existing face/serve readers.

| Turn state | Write | `read-turn-records` | `current-episode!` fallback | Existing face/serve read |
|---|---|---|---|---|
| Missing | Open status | One open cell | Candidate unless defunct marker excludes its time/episode | Shows existing turn-cell shape |
| Missing | Terminal status | One terminal cell | Candidate unless marker excludes it | Shows terminal existing shape |
| Missing | Exact terminal replay | Same terminal cell | Same candidate/filter result | Same row |
| Open | Open replay | Same open cell | Same candidate/filter result | Same row |
| Open | Terminal S | Same order-key cell overwritten to S | Candidate unless marker excludes it | Shows S |
| Open | Terminal S2 after close emission S1 | Existing transition overwrite semantics; S2 has its own import | Candidate/filter depends on time/episode, not status | Shows last accepted turn status |
| Terminal S | Exact S replay | Journaled identical result | Same candidate/filter result | Same row |
| Terminal S | Different terminal S2 | Same cell overwritten through existing transition-scoped import | Same candidate/filter result | Shows last accepted status |
| Terminal S | Open replay | Existing protocol does not use this transition; R2 must not introduce it | Existing terminal remains the meaningful durable turn | Existing reader behavior unchanged |

For every terminal write, the close emission occurs first. A different terminal
status derives a different cascade emission/run even though the existing turn
cell keeps one stable order key.

### Cascade declaration row

Related reads are `cascade/rows` and `cascade/react!`.

| Row state | Source declaration write | `rows` | `react!` |
|---|---|---|---|
| Row absent | Add valid durable row #2 | Lists it after row #1 | Matches only its trigger and takes durable path |
| Existing best-effort row #1 | R2 activation | Byte-unchanged row | Exact R1 path and receipt |
| Valid durable row | Enumeration/reload | Printable, unique, full idempotency story | Durable obligation-before-call semantics |
| Durable row with blank/missing idempotency | Attempted declaration | Grammar scan refuses package | Must not dispatch |
| Row on unmatched trigger | No runtime write | Still enumerable | No receipt, handler call, or durable obligation |

### Module deployment

Related reads are deployed-module listing, server-read module status,
`cascade/run`, and `cascade/pending-runs`.

| Module state | Operational write | Module/status reads | Run read | Pending read |
|---|---|---|---|---|
| Absent | First deploy | Sixth module appears and reaches RUNNING; five existing stay RUNNING | Available after deploy | Available after deploy |
| RUNNING | Clean redeploy | Returns to RUNNING | Existing durable rows preserved | Existing pending preserved |
| Worker restarted | Native resume | Module returns/stays RUNNING without history replay | Existing rows preserved | Existing pending preserved |
| Deploy failure or license/node question | Deploy attempt | Non-RUNNING/error; package stops | No success claim | No success claim |

## Edge-case inventory

- No matching row: empty receipt vector; no handler or obligation.
- Several matching rows: declaration-order receipts; each row isolated.
- Obligation duplicate before terminal: one obligated run.
- Obligation duplicate after terminal: terminal unchanged and not pending.
- Observation duplicate: first terminal unchanged.
- Conflicting terminal: base status unchanged; last conflicting evidence value,
  never a counter.
- Observation before obligation visibility: terminal truth wins; later
  obligation cannot resurrect it.
- Handler return, throw, JVM death before execution, and concurrent execution
  all have explicit outcomes.
- Empty pending/failed/run reads are valid.
- Grace boundary, young pending, old pending, and failed terminal are distinct
  sweep cases.
- Healthy close, complete, nonfresh failure, timeout, missing jsonl at close,
  jsonl appearing before execution, and malformed repair input are distinct
  handler cases.
- Same transition replay, new later transition, second same-episode failure,
  and old replay after advancement are distinct marker cases.
- Matching dead runtime entry, absent entry, different entry, revalidated
  same-id entry, and another JVM's empty atom are distinct runtime cases.
- Marker read outage preserves today's behavior rather than blocking a turn.
- Durable runner outage preserves the user's turn and emits an error fact.
- Production payload scan excludes `:lines` and non-EDN/runtime values.
- Classload, server boot, explicit sweep, deploy, and worker restart are
  separate lifecycle moments.

## Required proof obligations carried forward

These are requirements for later phases, not a Phase-1 plan:

- R1 focused behavior identity remains green, including false/nil guard cases.
- Identity bytes match across independent runtimes.
- Obligation acknowledgment is proven by an immediate authoritative read.
- Duplicate/reordered obligation and observation histories converge.
- Crash at the record-before-call boundary is recoverable.
- Both repair declines and the full stranded-lane product path are exercised.
- The first and second eligible failures of one episode id prove marker
  advancement and non-regression.
- Live proof reads authoritative run and episode projection truth, not a helper
  return or reconstructed id.
- Namespace loading is inert; boot sweep is explicit; failed runs are poisoned
  fixtures that prove no resweep.
- Deployment proof uses server-read RUNNING state and a restart-preservation
  receipt.
- Diff proof remains additive: no existing module/depot/PState/serve/cell
  shape, no OC/RK/llm module, and no read-only
  `material_circulation.clj`/`verb_registry.cljc` edit.

## Explicit non-goals

No scheduler or clock, retry/backoff policy, failed-run retry, auto-respawn,
autotag migration, rows-as-material, cross-boundary resume, new relation kind,
existing durable-shape edit, OC/RK/llm module edit, or durable declaration
table belongs to R2.
