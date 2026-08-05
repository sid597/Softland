# Rama World Kernel V0 PR Trail

Status: implementation trail, 2026-04-29.

This note explains the V0 Rama kernel change and the correction made after review: the depot must receive requests before the world decides what is accepted.

Throughput/policy follow-up:

```text
docs/current-mental-model/architecture/rama-policy-throughput-post.md
```

## Correct Center

Old app-specific center:

```text
sidebar/settings/editor/agent/flow event
  |
  v
*node-events-depot
  |
  v
app-shaped PStates
```

Correct V0 kernel center:

```text
ActionRequest
  |
  v
*world-requests-depot
  |
  v
Rama interpreter/policy
  |
  +--> rejected ActionDecision
  |
  v
accepted KernelEvent
  |
  v
kernel materializations
  |
  v
projections with target refs
```

The important correction:

```text
The depot does not receive pre-decided KernelEvents.
The depot receives ActionRequests.
Rama records the request and derives the decision.
```

## What Changed In Code

Canonical file:

```text
src/app/server/rama/core.clj
```

Old implementation removed:

```text
node-events-module
*node-events-depot
app-specific PStates for sidebar/settings/editor/flow/trails
```

New implementation added:

```text
world-kernel-module
*world-requests-depot
$$requests-by-id
$$decisions-by-id
$$events-by-id
$$artifacts
$$artifact-heads
$$branches
$$policies
$$text-revisions
$$units-by-artifact
$$unit-status-by-branch
$$projection-cache
```

Adapter file:

```text
src/app/server/rama/util_fns.cljc
```

now appends compatibility `ActionRequest`s instead of prebuilt kernel events.

Test file:

```text
test/app/server/rama/world_kernel_test.clj
```

now asserts:

```text
request is stored
decision is stored
accepted request produces KernelEvent
rejected request stays durable and does not produce KernelEvent
```

## Two Envelopes

An `ActionRequest` is the raw proposal entering Rama:

```text
ActionRequest =
  request identity
  actor
  branch
  context
  target
  action
  payload
  causality
  provenance
```

A `KernelEvent` is an accepted world fact:

```text
KernelEvent =
  event identity
  type
  actor
  branch
  context
  target
  action
  payload
  causality
  ordering
  policy
  provenance
```

Mental rule:

```text
ActionRequest asks.
KernelEvent happened.
ActionDecision records the answer.
```

## Rama Boundary

The implemented V0 boundary is:

```text
helper/API builds ActionRequest
  |
  v
foreign-append!
  |
  v
*world-requests-depot        <-- Rama boundary
  |
  v
source> request
  |
  v
store $$requests-by-id
  |
  v
interpret request inside Rama topology
  |
  +--> store rejected $$decisions-by-id
  |
  v
store accepted $$decisions-by-id
  |
  v
materialize accepted KernelEvent into PStates
```

So the answer to "does change happen before or after the depot?" is:

```text
The request enters before any accept/reject decision.
The accept/reject decision happens after the depot, inside Rama ETL.
```

The helper still constructs a request map before append. That is not a world decision; it is just forming the input value, like constructing a command payload before appending it to a depot.

## V0 Text Flow

Ingest:

```text
ActionRequest :artifact/ingest
  |
  v
*world-requests-depot
  |
  v
Rama accepts or rejects
  |
  v
KernelEvent :artifact/ingested
  |
  v
materialize:
  $$events-by-id
  $$artifacts
  $$artifact-heads
  $$text-revisions
  $$units-by-artifact
```

Unit judgment:

```text
ActionRequest :unit/status-set
  |
  v
*world-requests-depot
  |
  v
Rama reads materialized unit state
  |
  +--> if unit missing: rejected ActionDecision
  |
  v
KernelEvent :unit/status-set
  |
  v
materialize:
  $$unit-status-by-branch
```

Projection:

```text
$$units-by-artifact + $$unit-status-by-branch
  |
  +--> canonical view: non-rejected units
  |
  +--> discarded view: rejected/hidden units
```

## Cold Start Example

Starting condition:

```text
Rama has no artifacts.
Rama has no branches.
Rama has no user policies.
Only trusted bootstrap/system actor exists in code/config.
```

Cold-start request:

```text
ActionRequest
  actor: system/bootstrap
  branch: main
  target: world/main
  action: world/bootstrap
  payload:
    create main branch
    install default private policy
    ingest first artifact
```

Correct cold-start flow:

```text
empty Rama
  |
  v
append bootstrap ActionRequest
  |
  v
Rama interpreter sees no world exists
  |
  v
genesis rule:
  only system/bootstrap may create first world facts
  |
  v
accepted KernelEvents:
  :branch/created
  :policy/installed
  :artifact/ingested
  |
  v
materialized world exists
```

The data to decide comes from:

```text
request actor/action/payload
empty-world state
trusted bootstrap rule
```

There is one unavoidable trusted root: genesis. After genesis, ordinary actions should be decided from durable Rama state.

## Mid-State Example

Starting condition:

```text
Rama already has:
  branch main
  artifact art_1
  units art_1/line/1..3
  policy or actor capability allowing sid to judge units
```

Projection item:

```text
visible row "line 2"
  target:
    kind: unit
    id: art_1/line/2
    address:
      revision: rev_1
      range: 5..11
```

User action:

```text
sid clicks reject
```

Request entering Rama:

```text
ActionRequest
  actor: sid
  branch: main
  context:
    projection/id: text/canonical
  target:
    kind: unit
    id: art_1/line/2
  action:
    type: unit/status-set
    capability: unit/judge
  payload:
    artifact/id: art_1
    unit/id: art_1/line/2
    status: rejected
```

Rama decision:

```text
read $$units-by-artifact[art_1][art_1/line/2]
read policy/capability state
check status transition
  |
  +--> missing or forbidden: rejected ActionDecision
  |
  v
accepted KernelEvent :unit/status-set
```

Materialized result:

```text
$$unit-status-by-branch[main][art_1/line/2] = rejected
```

Projection result:

```text
canonical view hides line 2
discarded view shows line 2
```

## Why One Depot Still Exists

V0 still uses:

```text
*world-requests-depot
```

That does not mean final architecture has one physical depot. It means V0 has one request inbox while the logical contracts settle.

Future physical split:

```text
*action-requests-depot
*artifact-requests-depot
*unit-requests-depot
*policy-requests-depot
*agent-requests-depot
```

or:

```text
*artifact-events-depot
*unit-events-depot
*policy-events-depot
```

The compatibility promise is:

```text
Splitting depots should change routing/topology,
not the meaning of ActionRequest or KernelEvent.
```

## What V0 Includes

```text
ActionRequest as depot input
ActionDecision materialization
accepted KernelEvent derivation inside Rama
rejected request audit trail
text artifact ingest
text revision materialization
line unit derivation from accepted ingest
unit status request
canonical/discarded projection reads
provenance from projected unit to accepted ingest event/request
```

## What V0 Does Not Include

```text
HTTP/API endpoint for action requests
UI consumption of kernel projections
real persisted policy tables
branch fork/commit/merge
accepted-unit commit artifacts
edit-stable anchors
text operation log
distiller registry
redistillation
timeline indexes
event replay/debug routes
schema migration machinery
production Rama deployment topology
multi-depot physical split
```

## How The Implementation Got Here

Reasoning trail:

```text
1. Started from April 28 mental model:
   projection -> action -> event -> materialization -> projection.

2. First V0 incorrectly appended pre-decided KernelEvents.

3. User objected:
   world depot as test/proof is sloppy; correct path should be upfront.

4. Correction:
   depot receives ActionRequest, not KernelEvent.

5. Rama ETL now stores requests, derives accepted/rejected decisions,
   and materializes accepted KernelEvents.

6. Tests now prove both paths:
   accepted request produces world state;
   rejected request remains durable and produces no KernelEvent.
```
