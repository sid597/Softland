# Rama Review - <Block Name>

Status: not reviewed yet.

## Contract Restatement

Restate the intended lifecycle and identify the first physical record Rama sees.

Include:

- First depot/input record.
- Whether the record is a request, claim, observation, control, or accepted fact.
- Where acceptance/rejection happens.
- What durable state proves accepted truth.
- What durable state proves rejected or invalid input.
- What projection/read surface consumes the materialized state.

## Rama-First Reconstruction

Answer these before judging the committed shape:

- What new data enters the world?
- What entity owns local ordering?
- What is the first physical depot record?
- What is request/proposal vs accepted fact vs observation?
- Where is the authoritative decision made?
- Which key should partition the depot?
- Which PStates must be colocated for hot decisions and reads?
- Which reads must be fast?
- What can retry, duplicate, restart, or arrive out of order?
- Which side effects are possible, and what durable gate protects them?
- What evidence should tests provide?

## Phase Diagnosis

Use phases as diagnostic labels, not as a forward build plan. Base this section
on the block's `PHASE_RECONSTRUCTION.md`.

```text
Phase 0 - Implicit spec:

Phase 1 - Plan:

Phase 2 - Plan validation:

Phase 3 - Implementation:

Phase 4 - Implementation validation:

Phase 5 - Tests:

Phase 6 - Test validation:

Phase 7 - Finish/runtime:
```

Primary Failure Phase:

## Files Reviewed

List only files actually read.

### Phase Reconstruction

```text
```

### Code

```text
```

### Tests

```text
```

### Docs

```text
```

### Commit Anchors

```text
```

## Topology / Depot / PState Inventory

### Depots

```text
```

### Topologies

```text
```

### PStates

```text
```

### Query / Read Helpers

```text
```

### Client Append Helpers

```text
```

### TaskGlobals / Caches / Executors

```text
```

## Findings

Order findings by severity.

Use this format for each finding:

```text
### F1. <Title>

Severity: critical | high | medium | low

Evidence:
- path:line - what the code says
- path:line - related test/doc evidence

Runtime trace:
1. ...
2. ...
3. ...

Rama concern:
Which retro-review rule or Rama production assumption is violated.

Failure phase:
phase-0 | phase-1 | phase-2 | phase-3 | phase-4 | phase-5 | phase-6 | phase-7

Consequence:
What durable correctness, locality, retry, restart, I/O, or test risk this
creates.

Repair shape:
Smallest repair direction. Do not implement it in this review.
```

If there are no findings, write:

```text
No findings.
```

## Phase-4 Style Check Matrix

For each item, write `PASS`, `FAIL`, or `N/A`, with evidence.

### Redundant Conditionals

Verdict:

Evidence:

### Consecutive Keypath

Verdict:

Evidence:

### Select-Compute-Transform

Verdict:

Evidence:

### Unnecessary nil->val

Verdict:

Evidence:

### :allow-yield?

Verdict:

Evidence:

### Non-Subindexed Collections Without Size Limits

Verdict:

Evidence:

### Stream Topology Idempotency

Verdict:

Evidence:

### Partial Failure In Stream Topologies

Verdict:

Evidence:

### Single Depot Append Per Client Operation

Verdict:

Evidence:

### Application-State Caches Survive Restart

Verdict:

Evidence:

### No Reimplementation Of Built-In Operations

Verdict:

Evidence:

## Tests / Commands

List commands run, results, and any tests not run.

```text
```

## Recommended Repair Queue

List repair items only if findings exist. This is not an implementation plan.

```text
```

## Verdict

One-sentence justification.

```text
Primary Failure Phase: none
```

```text
RETRO_RAMA_REVIEW:pass
```
