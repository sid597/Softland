# Think In Rama: The Softland Ramanian POV

Status: working doctrine, 2026-04-29.

This note captures the mental shift from "using Rama as a backend component" to
"thinking in Rama". It is not a Rama tutorial. It is the architecture posture we
want future Softland work to inherit.

Related implementation/research notes:

```text
docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md
docs/current-mental-model/architecture/rama-policy-throughput-post.md
```

Primary external references:

- https://redplanetlabs.com/docs/~/depots.html
- https://redplanetlabs.com/docs/~/stream.html
- https://redplanetlabs.com/docs/~/microbatch.html
- https://redplanetlabs.com/docs/~/partitioners.html
- https://redplanetlabs.com/docs/~/tutorial2.html
- https://github.com/redplanetlabs/twitter-scale-mastodon
- https://blog.redplanetlabs.com/2023/08/15/how-we-reduced-the-cost-of-building-twitter-at-twitter-scale-by-100x/
- https://blog.redplanetlabs.com/2025/03/11/how-afterhour-built-an-ultra-scalable-chat-service-in-one-month-with-rama/
- https://blog.redplanetlabs.com/2025/04/01/massively-scalable-collaborative-text-editor-backend-with-rama-in-120-loc/

## The Center

The ordinary app-server instinct is:

```text
request
  -> service validates
  -> service mutates database
  -> other systems catch up
```

The Ramanian instinct is:

```text
new data enters through depots
  -> topologies own interpretation and state transitions
  -> PStates are materialized views shaped for questions/actions
  -> queries/projections read PStates
```

In Softland terms:

```text
Projection
  -> ActionRequest
  -> depot
  -> Rama decision
  -> accepted KernelEvent or rejected ActionDecision
  -> materialized state
  -> Projection
```

That loop is the bedrock. Do not make the API, UI, or helper layer the hidden
source of truth.

## The First Question Is Not "Where Do I Store This?"

The first Ramanian question is:

```text
What is the new data entering the world?
```

Then:

```text
What entity does this data affect?
What local ordering must be preserved?
Which PStates must be updated?
Which future questions must be answered quickly?
Which computations need low latency, and which need high throughput?
```

Only after those are clear do we choose depots, topology type, partitioning, and
PState shape.

## Depots Are Not Tables

A depot is the entry log for new data. It is not a table, queue, namespace, or
module folder.

Choose depot boundaries by:

```text
relatedness
local ordering
shared conceptual entity
PState locality
topology consumption
throughput profile
```

Do put records in the same depot when they must preserve order for the same
conceptual entity. `Follow` and `Unfollow` belong together because both mutate
the same relationship state.

Do split depots when unrelated data would force every topology to filter noise
or would destroy locality. Pageviews and profile-field updates should not share
a depot just because both are "events".

Softland implication:

```text
one logical kernel does not imply one physical depot
```

## Partitioning Is A Design Decision, Not A Later Optimization

Random partitioning is fine only when:

```text
ordering does not matter
processing does not need entity-local state
extra hops are acceptable
```

Most Softland actions are target-local. They touch:

```text
artifact
unit
branch
policy target
agent run
local world
```

So the request needs a routing key.

Preferred V1 shape:

```text
ActionRequest
  :routing/key [:artifact artifact-id]
```

Then the depot should hash by that routing key, so the topology starts near the
state it needs.

Bad smell:

```text
source request
  -> hash request-id
  -> later hash artifact-id
  -> later hash branch-id
```

Better:

```text
source request already on artifact/local-world partition
  -> decide from local or mirrored PStates
  -> side-index by request id for trace/debug
```

Request id is identity. It is not usually business locality.

## PStates Are Shaped By Questions

A PState is not "the database schema". It is a materialized view owned by a
topology.

Ask:

```text
What question must be answered in less than a millisecond?
What action needs a local policy/state read before deciding?
What projection needs a compact recoverable shape?
What history/provenance must remain navigable?
```

Then build PStates in those shapes.

Softland PState examples:

```text
$$requests-by-id
$$decisions-by-id
$$events-by-id
$$artifacts
$$artifact-heads
$$text-revisions
$$units-by-artifact
$$unit-status-by-branch
$$policy-by-target
$$actor-capabilities
$$projection-cache
```

Do not overvalue normalization. Rama PStates should be shaped for use.

## Policy Is Not Middleware

There are four different things people call "policy":

```text
edge guard
request typing/routing
authoritative world decision
projection/read filtering
```

They belong in different places.

Edge guards can happen before Rama:

```text
session exists
payload size is acceptable
token is structurally valid
request rate is not abusive
```

These protect the system. They are not world truth.

Request typing can happen before Rama:

```text
construct ActionRequest
derive routing key
choose a command/request type
ask Rama which actions are currently available
```

This packages intent. It does not accept the action.

Authoritative decisions should happen in Rama:

```text
does target exist?
can actor perform action?
is transition valid?
is causal parent current?
does policy permit this branch/projection effect?
```

Projection filtering should be derived from Rama state:

```text
what this actor can see
which units appear in canonical/discarded views
why something is hidden
which policy caused the shape of the projection
```

Softland rule:

```text
If a decision should be replayable, auditable, or explainable later, it belongs
after a depot inside Rama.
```

## Request First Does Not Mean Sloppy

Sloppy:

```text
append anything
hope validation catches up somewhere
mix accepted facts and rejected attempts
call the depot "world events"
```

Correct:

```text
append typed ActionRequest
store request
decide inside Rama
store ActionDecision
derive KernelEvent only when accepted
materialize accepted facts
```

Rejected requests are not world facts, but they are part of the trail.

This distinction is what makes Softland's "how the LLM got here" trail honest:

```text
ActionRequest asks.
ActionDecision answers.
KernelEvent happened.
PState projection shows.
```

## Stream Or Microbatch?

Use stream topologies when:

```text
interactive latency matters
the user expects fast feedback
the action is small and target-local
```

Examples:

```text
unit status change
artifact ingest
branch fork
policy grant/revoke
agent run start/cancel
```

Use microbatch topologies when:

```text
throughput matters more than single-record latency
exactly-once batch semantics matter
the work is broad, derived, or periodic
```

Examples:

```text
redistillation
projection rebuild
trend/ranking computation
large provenance index rebuild
semantic search index refresh
```

Do not force a low-latency interaction into microbatch. Do not force a huge
derived rebuild into per-record stream work.

## Query Topologies Are Part Of The Design

Rama reads are not an afterthought.

Use direct PState reads for simple keyed lookups. Use query topologies when the
answer requires cluster-side computation, multiple PStates, partition movement,
or an "available actions" decision that should not live in UI code.

Softland should have queries like:

```text
allowed-actions(actor, target, branch)
artifact-head(artifact-id)
projection(projection-id, actor, branch, artifact-id)
trail-for-target(target)
decision-for-request(request-id)
```

The UI may use `allowed-actions` to render affordances. The authoritative
mutation still appends an ActionRequest and receives an ActionDecision.

## Derived Depots Are Real, But Protected

The Mastodon implementation uses client-facing depots and topology-owned
derived depots.

Softland can use the same pattern:

```text
client-facing:
  *artifact-requests-depot
  *unit-requests-depot
  *branch-requests-depot
  *policy-requests-depot

topology-owned:
  *accepted-events-depot
  *projection-invalidations-depot
  *distillation-jobs-depot
```

Topology-owned depots should reject client appends. They are derived streams,
not public command surfaces.

## The API Layer Is A Thin Boundary

The API may:

```text
authenticate transport/session
parse payload
construct request envelope
derive routing key
call query topology for UI convenience
append request asynchronously
return ack/decision/event id
```

The API should not:

```text
mutate PStates
decide world truth from private memory
hide meaningful policy transitions
prebuild accepted KernelEvents for user actions
fork business logic away from Rama topologies
```

If the API makes a convenience decision before append, the Rama topology must
still be able to reject the request using current durable state.

## Softland Defaults

Until proven otherwise:

```text
ActionRequest is depot input.
KernelEvent is accepted world fact.
ActionDecision is durable answer.
PStates are materialized views.
Projection items carry target refs.
Policy decisions happen in Rama if they affect world truth.
Routing key follows the affected world entity.
Request id is an index key, not locality.
Depots split by relatedness/local ordering/locality.
Microbatch is for derived/bulk work.
Stream is for interactive actions.
```

## Red Flags

Stop and rethink when you see:

```text
"We'll validate everything before Rama."
"One world depot can handle all future actions."
"This PState is the source of truth."
"We'll add partitioning later."
"The UI knows whether this is allowed."
"This rejected action can just disappear."
"Let's key by request id because every request has one."
"The topology filters 80 percent of the depot records."
"This event was built outside Rama and appended as truth."
"The projection hides something but cannot explain why."
```

## The Staff-Level Review Question

For any Rama proposal, ask:

```text
Does the physical layout preserve the logical truth?
```

Meaning:

```text
Does the request enter Rama before authoritative decision?
Is the depot keyed by the entity whose ordering/locality matters?
Are PStates shaped for the actual questions/actions?
Can policy be replayed and explained from durable state?
Can rejected attempts be traced without becoming accepted facts?
Can downstream projections derive from accepted facts without hidden coupling?
Can the system split depots later without changing ActionRequest/KernelEvent meaning?
```

If yes, the design is probably Ramanian.

If no, it is probably a normal app wearing Rama-shaped clothes.
