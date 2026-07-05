# Policy Granularity And The Mastodon Parallel

Status: exploratory note, 2026-05-01.

This is not a settled Softland policy design. It captures a live intuition:
Rama's Mastodon implementation may be a useful analogy for how to support
fine-grained behavior without naively materializing permissions for every actor
and every tiny unit.

Origin prompt:

```text
does this not have parallels with the mastadom implementation by rama???? don't
they have fine grained?? i think they do over each post .. you can goas free
non registered user just scroll, or signup and write, follow, but who you follow
and what you see depends on a query ... then you can do the same to other users
```

Why this matters:

```text
This is an active systems-level question, not a settled design. Preserve the
question that opened the thread so future review can recover the original
pressure.
```

## The Question

Softland wants very fine-grained addressability:

```text
artifact
file
range
unit
claim
relation
projection item
local world
```

But fine-grained addressability could become expensive if every addressable unit
also gets fully materialized policy, visibility, projection, provenance, and
allowed-action state.

Open concern:

```text
If there are millions of Roam-graph-equivalent units, do we pay policy/storage
cost for every unit?
```

Working distinction:

```text
addressability granularity:
  how small a thing can be targeted or referenced

policy-definition granularity:
  where permissions are authored

materialization granularity:
  what is precomputed into PStates

query granularity:
  what can be derived for a viewer/action at read time
```

These do not have to be the same granularity.

## Why Mastodon Seems Relevant

Mastodon-like products have fine-grained behavior:

```text
anonymous reading
signup / posting
follow / unfollow
mute / block
favorite / boost
reply
timeline visibility
notification filtering
```

A user can read public posts without registering. A signed-in user can write,
follow, mute, block, favorite, boost, and reply. What a user sees is not simply
"all posts"; it is derived from follows, blocks, mutes, filters, author state,
status state, and timeline/query logic.

The Rama Mastodon writeup describes PStates shaped around the product questions,
not a generic permission matrix. For timeline rendering, it gathers status
content, stats, author information, block/mute state, booster information, and
pagination data through colocated PStates and query topology work.

Relevant example from the writeup:

```text
$$accountIdToStatuses
  account-id -> status-id -> content versions

$$statusIdToFavoriters
$$statusIdToReplies
$$statusIdToBoosters
$$statusIdToMuters
```

The notable Rama move is that some status-specific PStates are partitioned by
the author account id rather than by the apparent top-level status id. That
colocates data needed for rendering/status behavior.

Sources:

```text
https://github.com/redplanetlabs/twitter-scale-mastodon
https://blog.redplanetlabs.com/2023/08/15/how-we-reduced-the-cost-of-building-twitter-at-twitter-scale-by-100x/
```

## Softland Analogy

Possible analogy, not final design:

```text
Mastodon status/post
  ~ Softland unit/claim/range/artifact item

Mastodon follow/mute/block graph
  ~ Softland read/collab/write policy graph

Mastodon timeline query
  ~ Softland projection query

Mastodon status rendering
  ~ Softland local-world/projection rendering
```

This suggests a possible direction:

```text
fine-grained units remain addressable
permissions are usually inherited from coarser scopes
exceptional units get sparse overrides
viewer-specific answers are often query topology work
hot read surfaces may be materialized
```

## Possible Policy Classes

A useful decomposition might be:

```text
write / author:
  places where actor can create or directly mutate world state

collaborate:
  places owned by others where actor can read and contribute/respond

observe:
  places actor can read but not mutate
```

This is not necessarily UI structure. It is a possible policy/read shape.

Possible PStates to explore:

```text
$$actor-write-scopes
  actor-id -> scope-id -> grant

$$actor-collab-scopes
  actor-id -> scope-id -> grant

$$actor-read-scopes
  actor-id -> scope-id -> grant

$$scope-policy
  scope-id -> policy document

$$target-scope
  target-key -> nearest scope-id

$$policy-overrides
  target-key -> override policy only when exceptional
```

If this shape holds, cost would be closer to:

```text
number of scopes
+ number of grants
+ number of exceptional overrides
+ hot projection materializations
```

rather than:

```text
number of actors x number of units
```

This is still a hypothesis. It needs pressure testing against actual Softland
flows.

## Flow: Policy Grant As World Action

"Make this open to xyz" should probably not be hidden UI mutation. It is likely
an ActionRequest:

```text
ActionRequest :policy/grant
  -> target scope or artifact/local-world
  -> actor must be allowed to grant policy there
  -> ActionDecision accepted/rejected
```

Accepted path, if this model holds:

```text
KernelEvent :policy/granted
  -> update scope policy / actor scope indexes
  -> projections or query topologies expose changed visibility/actions
```

Rejected path:

```text
ActionDecision :rejected
  -> :decision/reason :actor-not-authorized
  -> no KernelEvent
```

## Open Questions

```text
What is Softland's equivalent of a Mastodon "account" for locality?
  actor?
  local world?
  artifact owner?
  branch?

What is Softland's equivalent of a Mastodon "status"?
  unit?
  claim?
  range?
  artifact revision?

Which policy answers are hot enough to materialize?

Which policy answers are viewer-specific enough to derive in query topology?

When does unit-level override justify its storage/write cost?

How do public anonymous reads fit with private local-world defaults?

Can write/collab/read scopes be represented as one policy graph, or do they need
separate indexes?

Where should allowed-action previews live:
  PState?
  query topology?
  projection cache?
  edge cache?
```

## Current Design Compass

Tentative, not settled:

```text
Addressable everywhere.
Materialized where hot.
Inherited where normal.
Overridden where exceptional.
Derived where cold or viewer-specific.
```

This note should stay open until we implement at least one real policy PState
and one projection/query that depends on it.
