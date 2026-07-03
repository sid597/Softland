# Meta Learnings - Rama Retro Review

Status: synthesized.

META_REVIEW:learning-taxonomy

## Purpose

This file preserves the cross-block learnings from the retroactive Rama review.
It answers:

```text
What bad practices appeared?
How bad were they?
Did recurring mistake groups emerge across implementations?
```

It synthesizes the six completed review blocks in this folder:

```text
01-world-kernel-contract
02-compute-track
03-space-runtime-spine
04-llm-agent-track
05-transcript-capture
06-text-kernel-shape
```

## Bottom Line

The old Rama work was not bad because the product ideas were wrong. The repeated
failure was that the implementations were built like feature slices instead of
production Rama truth machines.

The pattern:

```text
Phase 0 product/spec: mostly strong
Phase 1 Rama plan: often missed adversarial invariants
Phase 3 implementation: often built the happy spine but let invalid records mutate truth
Phase 5/6 tests: green, but proving the wrong thing
```

A later focused review of the committed chat-ingester Rama work sharpened this:

```text
The invariant was often named in the docs.
The validation accepted weaker evidence than the invariant required.
```

## Review Severity Snapshot

```text
6 / 6 blocks: RETRO_RAMA_REVIEW:major-fail

30 total findings:
4 critical
11 high
15 medium
```

Primary failure phase by block:

```text
01 world-kernel-contract: phase-1
02 compute-track: phase-3
03 space-runtime-spine: phase-3
04 llm-agent-track: phase-1
05 transcript-capture: phase-3
06 text-kernel-shape: phase-3
```

The strongest lesson:

```text
We are good at imagining the semantic object.
We are pretty good at building the first working projection.
We were not strict enough at turning the semantic object into a Rama contract.
```

## Rama Lens Used For This Synthesis

From the `$rama` skill and the review artifacts, a committed Rama feature is not
done when the happy path works. It is done when these are true:

```text
- the first physical depot record is named
- proposal/request is distinct from accepted fact
- rejected decisions are durable when rejection matters
- duplicate/retry semantics are explicit
- observations cannot mutate truth without authority
- topology-owned PStates are the source of materialized truth
- partition/routing keys match locality and read patterns
- hot PStates are bounded/subindexed/read-shaped
- worker restart does not require depot replay to recover correctness
- IPC tests are not treated as production proof
```

## Cross-Cutting Addendum - Named Invariant Vs Proven Invariant

The chat-ingester follow-up showed a subtler failure than "missing spec."

Several phase artifacts named the right invariant:

```text
- terminal IngestRuns are never mutated
- redaction happens before durable persistence
- tool_result creates durable result content
- source-record identity is byte/offset/hash based
- retry/idempotency must preserve required views
```

But the validation artifacts accepted weaker proof:

```text
- happy-path orchestration was treated as enough
- reused parser/executor code was treated as trusted
- row existence was treated as semantic correctness
- audit/tool indexes were treated as acceptable loss surfaces
- depots were not reviewed as adversarial production inputs
```

Bad practice:

```text
Confusing "the invariant appears in the phase docs" with "the invariant was
proven against the implementation and tests."
```

Repair pattern:

```text
Every named invariant needs a matching proof obligation:
- a plan mechanism that enforces it
- an implementation check that cannot be bypassed by direct depot append
- an adversarial scenario trace
- a test that would fail if the invariant was false
```

This is the difference between a feature-slice review and a Rama truth-machine
review. A feature-slice review checks that the intended path produces visible
rows. A Rama review checks that illegal facts cannot become durable truth, and
that replay/retry/crash/adversarial append preserve the contract.

## Group 1 - Idempotency And Duplicate Requests

This is the largest recurring smell.

Old pattern:

```text
Stable ids existed, but conflict semantics were not consistently defined.
```

Examples:

```text
World/Text:
Duplicate request ids can create multiple accepted KernelEvents.

Compute:
Duplicate run requests can reset a completed lifecycle and enqueue another spawn.

LLM:
Duplicate run requests can reset a completed run.

Space:
Compose idempotency is conflict-blind across spaces.

Kernel shape:
The shared lifecycle does not enforce one consistent request/decision/event contract.
```

Bad practice:

```text
Treating an id as a row key to overwrite instead of an identity whose duplicate
and conflict semantics must be decided.
```

Repair pattern:

```text
For every request id / run id / idempotency key:
- same id + same payload -> replay same decision or no-op
- same id + different payload -> reject conflict
- same run id after terminal -> never reset lifecycle
- same side-effect identity -> never respawn
- decision row records what happened
```

## Group 2 - Observations And Controls Are Trusted Too Easily

In Rama, observations are not harmless logs. They mutate durable truth.

Examples:

```text
Compute:
Unknown-run observations can fatal the stream topology.

LLM:
Observations mutate run truth without claim authorization.

LLM:
Wrong-token observations can still write items.

Space:
Approval controls can approve an approval that was never requested.

Space:
Non-patch LLM observations become pending Space patch proposals.

Transcript:
Status rows are not a real claim/spawn registry.

Chat-ingester:
Observation rows can materialize object-container truth without an accepted run.

Chat-ingester:
Late claim rows can mutate terminal run state.
```

Bad practice:

```text
Appending to an observation/control depot and assuming the record deserves to fold.
```

Repair pattern:

```text
Observation/control folding must check:
- target run/request exists
- target is in a state that can accept the mutation
- writer has the active claim token / claimant identity
- sequence is valid
- terminal rows reject late mutation
- controls target existing pending control state
```

## Group 3 - Transitional Compatibility Paths Become Real Contracts

The old work often used broad compatibility adapters to preserve momentum.
In an event-sourced system, a temporary accepted event still becomes permanent
truth.

Examples:

```text
World/Text:
Compatibility adapters keep hidden in-memory truth outside Rama.

Text/kernel shape:
:compat/record is too broad and can mint generic accepted KernelEvents.

Shape:
Identity headers point to KERNEL-SHAPE, but headers are comments, not contracts.

Text/kernel shape:
KERNEL-SHAPE says it is inspectable data, but the namespace does not load.
```

Bad practice:

```text
Letting temporary compatibility be broad because it is "temporary."
```

Repair pattern:

```text
Compatibility must be narrow:
- explicit allowed event types
- explicit payload validators
- explicit legacy provenance
- explicit migration/removal criteria
- tests that arbitrary compatibility events are rejected
```

## Group 4 - Green Tests Prove The Spine, Not The Contract

Every block had passing tests. The tests were useful but too friendly.

They mostly proved:

```text
happy request works
projection appears
basic lifecycle runs
```

They did not consistently prove:

```text
duplicates are safe
unknown records are rejected
stale executors cannot write
wrong claim token fails
malformed external input is safe
rows contain the semantic payload the spec promised
restart/retry does not duplicate side effects
dedup markers cannot hide partial cross-partition failures
PState shape scales
shared shape namespace loads
```

Examples:

```text
World:
Tests prove lifecycle but not retry/restart/locality.

Compute:
Tests prove spine, not adversarial Rama contract.

Space:
Missing negative tests let truth-boundary leaks pass.

LLM:
Tests prove happy spine but miss Rama failure modes.

Transcript:
Tests cover happy ASCII harvest/watch behavior but miss redaction and byte identity.

Chat-ingester:
Tests prove tool-result container presence, not actual tool-result content.

Chat-ingester:
Tests claim terminal-state immutability but use a different request id instead
of appending a late claim to the same terminal run.

Shape:
All module tests pass while app.server.rama.kernel does not load.
```

Repair pattern:

```text
Each Rama feature needs adversarial tests:
- duplicate same id, same payload
- duplicate same id, different payload
- depot event before its parent request exists
- observation before request
- observation after terminal
- wrong claim token
- control for missing target
- malformed input with secrets
- non-ASCII source bytes when source identity uses byte offsets
- semantic payload assertions, not just row-kind assertions
- partial retry across every partition hop
- large PState collection
- restart/retry simulation where possible
```

## Group 5 - Production PState Shape Was Deferred Too Often

This was not always the loudest correctness failure, but it repeats.

Examples:

```text
World/Text:
Physical locality and projection read shape are V0/transitional.

Compute:
Pending inbox and observation buffer are not bounded or subindexed.

Space:
Large Space/LLM PStates are not shaped for production read costs.

LLM:
Hot LLM PStates are map-shaped without production bounds.

Transcript:
Observation depot partitions by request instead of source file.
```

Bad practice:

```text
Making the PState shape that made implementation easy instead of the shape that
makes production queries cheap and locality-safe.
```

Repair pattern:

```text
For every PState:
- name the primary read
- name the partition key
- decide if nested collections need subindexing
- avoid unbounded vectors/maps in hot rows
- colocate writes/read paths intentionally
- avoid client-side joins for core projections
```

## Group 6 - External I/O Was Not Treated As Source Identity

This appears most strongly in Transcript, but it applies to future file/chat/
source ingestion.

Examples:

```text
Parse-error rows persist unredacted preview text.
UTF-8 transcript rows corrupt byte identity and payload text.
Harvest accumulates observations instead of streaming appends.
Only the first tool_use block is indexed.
Reused parser paths were treated as trusted instead of re-proven against the
new Rama module's privacy and identity invariants.
```

Bad practice:

```text
Treating source file reading as helper code around Rama instead of part of the
durable source identity contract.
```

Repair pattern:

```text
For external source ingestion:
- raw byte identity first
- redaction before persistence
- byte offset and byte length from bytes, not decoded strings
- malformed data must be safe
- reused parser/helper paths must be re-proven against the new module contract
- stream observations instead of accumulating huge batches
- every source record needs replay/idempotency identity
```

## Added Review Obligations

These obligations should be added to future Rama review passes, especially
Phase 2 plan validation, Phase 4 implementation validation, and Phase 6 test
validation.

### 1. Depot Adversary Matrix

Every depot source must be reviewed as a production input, not only as something
called by the intended executor.

For each depot event, enumerate:

```text
event x entity state -> accepted / ignored / rejected / materialized
```

Examples:

```text
observation x missing request
observation x pending request
observation x complete/failed/cancelled request
claim x missing request
claim x terminal request
control x missing target
```

### 2. Compatibility Path Re-Proof

Any reused parser, executor, helper, or legacy function must be re-proven
against the new module's invariants.

Do not accept "existing function handles this" unless the review traces the
exact hard cases:

```text
malformed input
secret redaction
UTF-8 / byte identity
duplicate native IDs
missing IDs
schema drift
```

### 3. Semantic Assertion Rule

Tests cannot pass by proving that a row exists. They must prove the semantic
payload required by the spec.

Examples:

```text
tool_result test must assert actual result content, not only container kind
terminal immutability test must append a late same-request claim
redaction test must use malformed JSON, not only valid JSON
source anchor test must include non-ASCII byte math
retry test must prove required later-partition views are not lost
```

## Per-Implementation Mistake Type

```text
01 World/Text contract:
Good conceptual lifecycle, weak duplicate/idempotency semantics, compatibility
truth outside Rama.

02 Compute:
Good command-run spine, weak invalid-observation handling, duplicate run reset,
deferred restart/stall/spawn safety.

03 Space:
Good local-world ambition, weak truth-boundary validation between Space and LLM,
control/idempotency too permissive.

04 LLM:
Good agent-run spine, but the claim/auth model was not strict enough. Executor
observations can mutate truth without proving authority.

05 Transcript:
Good passive-observation idea, but source identity/privacy/I/O safety were not
treated as first-class Rama contracts.

06 Text/kernel shape:
Good split direction, but meta-contract failed. KERNEL-SHAPE is comments/data in
spirit, broken namespace in reality, and not enforced across modules.
```

## Overall Classification

```text
Architecturally promising.
Rama-contract immature.
Production unsafe until hardened.
```

The strongest concise diagnosis:

```text
We confused "the thing exists in Rama" with "Rama owns the truth safely."
```

## New Default Gate For Future Rama Work

Before calling a future Rama implementation done, answer:

```text
1. What is the first physical record Rama sees?
2. What is proposal/request versus accepted fact?
3. What durable row records rejection?
4. What happens for exact duplicate identity?
5. What happens for same identity with different payload?
6. Who is authorized to append observations?
7. What happens if an observation arrives before request state exists?
8. What happens after terminal state?
9. What happens after worker restart?
10. Which entity owns local ordering?
11. Which partition key gives locality for the main write/read path?
12. Which PState can grow, and is it subindexed/bounded?
13. Which test would fail if this contract was misunderstood?
14. Has every depot event been checked against every relevant entity state?
15. Which reused helper/parser/executor path was re-proven against this contract?
16. Which tests assert semantic payload, not just row existence?
17. Can any dedup marker hide partial later-partition writes after retry?
```

META_REVIEW_VERDICT: old-implementation-practice-was-rama-contract-immature
