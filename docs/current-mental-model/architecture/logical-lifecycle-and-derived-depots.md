# Logical Lifecycle And Derived Depots

Status: clarification note, 2026-05-01.

This note captures the current answer to:

```text
When we add more depots, does the world-kernel flow change?
After KernelEvent, do we need another depot based on artifact type or params?
```

## Short Answer

The physical topology will change as Softland grows.

The logical lifecycle should remain stable:

```text
Projection
  -> ActionRequest
  -> request depot
  -> authoritative topology
  -> ActionDecision
  -> KernelEvent?
  -> PStates / projections
  -> Projection
```

With more depots:

```text
Projection
  -> ActionRequest
  -> one of many request depots
  -> request-specific topology
  -> ActionDecision
  -> KernelEvent?
  -> PStates / derived depots / projections
```

## Current V1 Shape

V1 has one request depot:

```text
Projection
  -> ActionRequest
  -> *world-requests-depot
  -> world-kernel-topology
  -> request-validation
  -> action-dispatch
  -> action-interpretation
  -> authorization/world-state checks
  -> ActionDecision
```

Accepted:

```text
ActionDecision :accepted
  -> KernelEvent
  -> materialization
  -> PStates
  -> Projection
```

Rejected:

```text
ActionDecision :rejected
  -> :event/id nil
  -> :decision/reason
  -> no KernelEvent
  -> no world-state materialization
```

## Future Multi-Depot Shape

More depots mean more physical lanes, not a different ontology.

Possible examples:

```text
*text-edit-requests-depot
  -> text-edit-topology
  -> ActionDecision
  -> KernelEvent :text/edit-applied
  -> $$text-revisions
  -> optional derived indexing/distillation work
```

```text
*policy-requests-depot
  -> policy-topology
  -> ActionDecision
  -> KernelEvent :policy/granted
  -> $$scope-policy
  -> $$actor-read-scopes / $$actor-write-scopes
```

```text
*build-requests-depot
  -> build-topology
  -> ActionDecision
  -> KernelEvent :build/requested
  -> $$builds
  -> optional build job depot
```

## Does KernelEvent Need Another Depot?

Not automatically.

A topology can derive a `KernelEvent` and directly materialize primary PStates:

```text
ActionRequest depot
  -> authoritative topology
  -> ActionDecision :accepted
  -> KernelEvent
  -> primary PState writes
```

That is enough when the accepted fact only needs local materialization.

Add another depot only when a downstream process deserves its own lane.

## Derived Depots

Derived depots are for downstream work after truth has already been accepted.

They are useful for:

```text
indexing
distillation
build jobs
deploy jobs
notifications
search refresh
agent jobs
projection rebuilds
expensive extraction
cross-artifact graph updates
fanout to other modules
retryable external work
microbatch/bulk derived work
```

Important rule:

```text
Derived depots are not where truth first becomes true.
```

Truth becomes true here:

```text
ActionDecision :accepted
  -> KernelEvent
```

Derived depots are after that:

```text
KernelEvent
  -> primary PStates
  -> optional derived depot
  -> derived topology
  -> derived PStates / external side work / projections
```

## Artifact-Type Branching

Artifact type can choose downstream processing without changing the lifecycle.

Example:

```text
ActionRequest :artifact/ingest
  -> accepted KernelEvent :artifact/ingested
  -> primary artifact PStates
  -> case artifact/type
       :text -> text unitization / text indexes
       :pdf  -> pdf extraction job depot
       :code -> code index/build graph depot
```

The primary accepted fact is still:

```text
KernelEvent :artifact/ingested
```

Artifact-specific depots are downstream work lanes.

## Full Shape

```text
                 +----------------------+
Projection ----> | ActionRequest        |
                 +----------+-----------+
                            |
                            v
                  request depot lane
                            |
                            v
                  authoritative topology
                            |
                            v
                 +----------------------+
                 | ActionDecision       |
                 +----------+-----------+
                            |
          +-----------------+----------------+
          |                                  |
          v                                  v
   rejected decision                  accepted decision
   :event/id nil                      :event/id evt_1
          |                                  |
          v                                  v
   $$decisions-by-id                 KernelEvent
                                            |
                          +-----------------+----------------+
                          |                                  |
                          v                                  v
                  primary PStates                    optional derived depots
                          |                                  |
                          v                                  v
                    Projection                  derived topologies/PStates
```

## Captured Q&A

Question:

```text
Projection -> ActionRequest -> Depot -> Topology -> ActionDecision
  -> KernelEvent? -> PStates -> Projection

This will change when we have more depots right?

After the KernelEvent, do we need to have another depot that this gets added to
based on artifact type or other params?
```

Answer:

```text
Yes, the physical diagram changes when there are more depots.
No, the logical lifecycle does not need to change.

No, every KernelEvent does not automatically need another depot.
Add a derived depot only when downstream work deserves independent ordering,
locality, batching, retry, fanout, or external side-effect handling.
```

## Context Needed When Reopening This

To reason about a future depot split, answer:

```text
1. What is the authoritative ActionRequest family?
2. What entity provides the needed locality/order?
3. Which topology makes the accept/reject decision?
4. What primary PStates must update immediately from the accepted KernelEvent?
5. Which work is downstream/derived rather than authoritative?
6. Does downstream work need its own ordering, batching, retry, or locality?
7. Is the new depot client-facing, topology-owned, or derived-only?
```

Client-facing request depots accept proposed actions.

Topology-owned/derived depots should generally reject direct client appends.

## Current Compass

```text
Many depots are allowed.
Many topologies are expected.
The lifecycle stays request -> decision -> event? -> materialization.
Primary PStates come from accepted KernelEvents.
Derived depots are optional downstream lanes, not the source of truth.
```
