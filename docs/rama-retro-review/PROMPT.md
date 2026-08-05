# Prompt - Retroactive Rama Review

Use this prompt to review already committed Rama implementation work that
predates the current Rama-skill workflow.

This prompt is self-contained for the retro-review workflow. Do not use any
global Softland prompt. Do not use an implementation handoff. Do not turn this
review into a repair task unless the user explicitly asks after the review is
complete.

## Role

You are an adversarial Rama reviewer. Let the Rama lens run the review.

The code already exists. Treat it as a committed implementation artifact that
arrived without a clean phase trail.

Your job is to figure out whether the committed work was done the wrong way,
and if so, where the wrongness entered: requirements, plan, validation,
implementation, tests, or runtime integration.

Do not assume the code is merely a Phase 3 implementation needing Phase 4
validation. That is one possible outcome, not the starting conclusion. The
review is allowed to discover that the old implicit spec was wrong, the depot
boundaries were wrong, the PState shape was wrong, the implementation drifted,
the tests proved the wrong thing, or the slice is basically sound.

You are not implementing. You are not refactoring. You are not improving docs
outside this review folder.

## Inputs

Start from one selected block:

```text
docs/current-mental-model/build/rama-retro-review/<block>/
```

Read:

```text
docs/current-mental-model/build/rama-retro-review/PROMPT.md
docs/current-mental-model/build/rama-retro-review/PHASE_RECONSTRUCTION_TEMPLATE.md
docs/current-mental-model/build/rama-retro-review/REVIEW_TEMPLATE.md
docs/current-mental-model/build/rama-retro-review/<block>/BRIEF.md
docs/current-mental-model/build/rama-retro-review/<block>/PHASE_RECONSTRUCTION.md
docs/current-mental-model/build/rama-retro-review/<block>/RAMA_REVIEW.md
```

Then read only the committed code, tests, and architecture docs named in that
block's `BRIEF.md`.

Use commit anchors from `BRIEF.md` only for archaeology when the current code,
tests, or docs are ambiguous.

## Hard Scope

Only committed Rama work listed in the selected block is in scope.

Out of scope:

- In-process work by other agents.
- `docs/current-mental-model/build/chat-ingester/`.
- `docs/current-mental-model/build/transcript-object-ingest/`.
- New feature design.
- Code patches.
- Refactors.
- Global prompt edits.
- Index-doc updates.
- Reading `src/app/server/env.clj`.

Object-container Slice 1 is not a retro-review target because it was implemented
with the Rama skill already. It may be used only as a comparison point if the
selected block needs one.

## Retroactive Rama Phase Triage

The normal Rama skill phases are forward-building phases. In retro review, use
them as diagnostic labels.

```text
Phase 0 - Implicit spec:
  Did the old work define the right problem, entities, invariants, scale,
  failure cases, and user/world contract?

Phase 1 - Plan:
  Were depot boundaries, partition keys, topology choice, PState shapes,
  query paths, idempotency, side effects, and restart behavior designed the
  right way?

Phase 2 - Plan validation:
  Would an adversarial Rama review have rejected the plan before code was
  written?

Phase 3 - Implementation:
  Did the code faithfully implement the intended contract and plan?

Phase 4 - Implementation validation:
  Does code tracing reveal Rama correctness, locality, idempotency, I/O, or
  restart-safety defects?

Phase 5 - Tests:
  Do tests encode the actual Rama contract and failure cases?

Phase 6 - Test validation:
  Could the tests pass while the module is still wrong?

Phase 7 - Finish/runtime:
  Is there enough compile, runtime, and integration evidence to trust the slice
  in production-shaped conditions?
```

You are not required to walk these phases in order. Use them to classify what
you find.

If the old slice has no clean spec or plan, do not invent new design artifacts
as a repair. Reconstruct the apparent contract from committed evidence, then
say whether the missing/weak spec or plan is itself the problem.

Default stance: the review starts suspicious. It does not pass until explicit
Rama tracing proves the important contracts are sound.

## Reconstruct Phase Artifacts From Committed Docs

Before comparing code, use the committed product, implementation, architecture,
or design docs listed in the block `BRIEF.md` as source evidence.

Write the result into:

```text
docs/current-mental-model/build/rama-retro-review/<block>/PHASE_RECONSTRUCTION.md
```

Use:

```text
docs/current-mental-model/build/rama-retro-review/PHASE_RECONSTRUCTION_TEMPLATE.md
```

This reconstruction is not a new ideal design. It is the Rama-shaped phase pack
that should have existed if the old committed docs had been processed through
the Rama workflow at the time.

Reconstruct:

```text
Phase 0 - the implicit spec the docs imply.
Phase 1 - the Rama plan the docs imply.
Phase 2 - whether that implied plan should have passed validation.
Phase 3 - the implementation shape that should follow.
Phase 4 - the implementation validation checks that matter most.
Phase 5 - the tests that should exist.
Phase 6 - how those tests could be misleading.
Phase 7 - the finish/runtime evidence needed.
```

If the committed docs are missing a requirement, invariant, locality decision,
failure case, or read contract, record that as a reconstruction gap. Do not fill
the gap with a new preference unless the committed evidence supports it.

After reconstruction, compare the committed code and tests against
`PHASE_RECONSTRUCTION.md`. The main review question becomes:

```text
Did the actual implementation match the Rama-shaped phase artifacts implied by
the committed docs?
```

## How To Let The Rama Lens Run

Do not begin by fitting the code into the existing folder brief's theory. Begin
by asking what Rama would have asked before implementation:

```text
What new data enters the world?
What entity owns local ordering?
What is the first physical depot record?
What is request/proposal vs accepted fact vs observation?
Where is the authoritative decision made?
Which key should partition the depot?
Which PStates must be colocated for the hot decisions and reads?
Which reads must be fast, and what PState shape serves them?
What can retry, duplicate, restart, or arrive out of order?
Which side effects can happen, and what durable claim/decision gates them?
What evidence proves this in tests?
```

Then compare the committed code to those answers. If the code's own design
answers are different, say whether the code is right, the docs are right, both
are incomplete, or the underlying slice was conceived incorrectly.

## Rama Production Assumptions

Review as production Rama, not as an InProcessCluster-only demo.

Assume:

- Worker restart does not replay depot history.
- PStates are durable storage, not a cache rebuilt from depots on restart.
- Topologies resume from committed offsets or microbatch ids.
- Each task is single-threaded and executes topology events, queries, and local
  PState operations sequentially.
- Long synchronous topology work blocks that task, including query invocations.
- Fault tolerance matters even if tests do not hit retries.
- Duplicate side effects from retried processing are bugs.
- Data required by read operations must not be deleted unless the committed
  contract explicitly says so.

## Core Rama Review Lens

For the selected block, answer:

- What is the first physical record Rama sees?
- Is that record a request/proposal, an observation, a claim, or an accepted
  fact?
- Does Rama own acceptance/rejection?
- Are proposed requests and accepted facts separated?
- Are rejected decisions durable and queryable?
- Does accepted truth become a durable event or durable materialized state?
- Is the routing key semantic locality, not only request identity?
- Are depot partitioners aligned with the PStates touched by the topology?
- Are all `local-select>` and `local-transform>` calls on the correct task?
- Are PStates shaped around actual read/projection questions?
- Are side effects behind claims or otherwise retry-safe?
- Can duplicate depot records, repeated client appends, or topology retries
  create duplicate facts or duplicate external side effects?
- Are large scans subindexed or yielded where needed?
- Are query paths avoiding unnecessary RocksDB seeks and client-side network
  roundtrips?
- Do tests prove the contract that would fail if the interpretation was wrong?

## Wrong-Way Signals

Push especially hard on these. Any one of them can be enough for a major
finding:

- UI, API, helper code, executor code, or tests create accepted facts before
  Rama decides.
- A PState is treated as source truth rather than a materialized view with a
  clear durable input contract.
- Request ids or random ids are used as locality keys where entity-local order
  matters.
- The topology starts on one partition, then immediately reads/writes state
  owned by another partition.
- Rejected actions disappear or are only represented as thrown exceptions.
- A client operation needs multiple `foreign-append!` calls to become complete.
- External side effects can happen without a durable claim or decision gate.
- Retry can append duplicate downstream depot records or duplicate external
  effects.
- Worker restart loses executor state, pending work, or in-memory truth with no
  durable reconcile path.
- Large collections grow inside non-subindexed PStates without explicit caps.
- Reads require many client-side foreign selects when a query topology or
  denormalized PState should answer the question.
- Tests pass by checking helper return values while skipping depot/topology/PState
  materialization.
- Tests prove only InProcessCluster happy paths and not production-shaped retry,
  restart, idempotency, ordering, or locality concerns.

## Performance Lens

Use these cost heuristics:

```text
RocksDB seek: roughly 0.3-0.5ms.
Iterator next after a seek: roughly 1-10us.
100 point reads can cost around 50ms.
100 contiguous range iterations can cost around 0.6ms.
```

Prefer PState shapes that use a small number of seeks plus sequential iteration
over subindexed sorted maps/sets/vectors.

When code already has a value from a prior read, check whether it writes with
`termval` instead of re-reading through `term`.

When code does not need the old value, check whether it uses no-read write paths.

## Phase-4 Style Check Matrix

Every review must cover these checks.

### Redundant Conditionals

If every branch of an `<<if`, `<<cond`, or `<<switch` does the same operation
with only a variable differing, that is a review issue.

### Consecutive Keypath

Flag paths of this form:

```clojure
(keypath *a) (keypath *b)
```

Prefer:

```clojure
(keypath *a *b)
```

### Select-Compute-Transform

Flag `local-select>` followed by computation followed by `local-transform>` with
`termval` when a compound transform or aggregator would avoid a race, duplicate
read, or excess I/O. If the selected value is genuinely needed to compute a
multi-PState decision, explain why the pattern is acceptable.

### Unnecessary nil->val

Navigators handle nil as empty collection in many cases. Flag `nil->val` unless
the next navigator requires a concrete non-nil value, such as `(term inc)`.

### :allow-yield?

For `local-select>` or `select>` that iterates over a subindexed structure on a
non-mirror PState, require `{:allow-yield? true}` when the iteration count can
exceed roughly 100 entries.

This includes:

```text
ALL
MAP-KEYS
MAP-VALS
sorted-map-range
sorted-map-range-from
sorted-map-range-to
sorted-map-range-to-end
```

Do not require yielding for reads that are explicitly bounded to small counts.

### Non-Subindexed Collections Without Size Limits

For every write to a non-subindexed inner map, set, or vector, verify that the
application enforces a maximum size. If there is no cap and the collection can
grow beyond about 100 elements, flag it.

### Stream Topology Idempotency

For each stream topology, trace what happens if an event retries.

Check:

- Are PState writes idempotent overwrites?
- Does any write use append, increment, `AFTER-ELEM`, or another non-idempotent
  operation?
- Are IDs generated inside the topology?
- Would retry generate a different ID?
- Does the topology append to another depot?
- Would retry append duplicate downstream records?
- Does the receiving topology deduplicate?

### Partial Failure In Stream Topologies

For each stream event that writes multiple PStates or multiple partitions,
consider failure after some writes have committed. Trace retry behavior. Flag
any path where retry can leave permanent missing writes or inconsistent state.

### Single Depot Append Per Client Operation

Each client write operation should call `foreign-append!` exactly once. If one
operation needs additional depot writes, they should happen inside Rama through
topology-owned derived appends with dedupe/commit-boundary reasoning.

Multiple client-side appends for one logical operation are a review issue unless
the block contract explicitly proves crash-safe recovery.

### Application-State Caches Survive Restart

For every TaskGlobal, background executor, atom, in-process cache, or process
registry that holds application state, identify:

- Durable source of truth.
- Concrete rebuild/reconcile path after worker restart.
- Code that implements the path.

If there is no durable source and no rebuild path, flag it.

### No Reimplementation Of Built-In Operations

Flag custom code that reimplements Rama built-in operations or standard Rama
path/dataflow behavior without a committed reason.

## Severity

Use:

```text
critical - can create wrong durable truth, duplicate external side effects,
           unrecoverable restart behavior, or silent partition-local corruption.
high     - violates request/fact separation, idempotency, locality, or restart
           safety but has a contained repair.
medium   - avoidable I/O, missing yield/subindex, projection/query shape, or
           meaningful test gap.
low      - style, naming, small consistency issue, or local cleanup.
```

## Verdict

End the review artifact with exactly one verdict line:

```text
RETRO_RAMA_REVIEW:pass
RETRO_RAMA_REVIEW:minor-fail
RETRO_RAMA_REVIEW:major-fail
```

Use:

```text
pass       - every required check passed and no contract violation remains.
minor-fail - all failures are localized repairs inside the existing module shape.
major-fail - any failure requires changing depot boundaries, PState shape,
             partitioning, topology ownership, lifecycle semantics, or restart
             strategy.
```

If unsure between minor and major, choose `major-fail`.

Also include a `Primary Failure Phase` in the review artifact before the verdict:

```text
Primary Failure Phase: none | phase-0 | phase-1 | phase-2 | phase-3 | phase-4 | phase-5 | phase-6 | phase-7
```

Use `none` only when the slice passes. If multiple phases failed, choose the
earliest phase that would have prevented the later failure.

## Output Rules

Write only the selected block's `RAMA_REVIEW.md`.

Do not patch code.
Do not stage or commit.
Do not update global docs.
Do not edit other review blocks.
Do not continue into repair planning beyond the repair shape required in each
finding.

After writing the review, stop and report the verdict plus the path to the
artifact.
