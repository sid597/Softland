# Rama Blog Patterns For Softland

Status: research extraction, 2026-04-29.

This note captures direct and tangential lessons from the Rama blog posts that
should shape the next Softland implementation pass.

Sources:

- Collaborative editor:
  https://blog.redplanetlabs.com/2025/04/01/massively-scalable-collaborative-text-editor-backend-with-rama-in-120-loc/
- Personalized moderation:
  https://blog.redplanetlabs.com/2025/04/29/next-level-backends-with-rama-personalized-content-moderation-in-60-loc/
- Graphs:
  https://blog.redplanetlabs.com/2025/03/26/next-level-backends-with-rama-graphs/
- Recommendation engine:
  https://blog.redplanetlabs.com/2025/04/08/next-level-backends-with-rama-recommendation-engine-in-80-loc/
- Timed notifications:
  https://blog.redplanetlabs.com/2025/04/16/next-level-backends-with-rama-fault-tolerant-timed-notifications-in-25-loc/
- G+D Netcetera:
  https://blog.redplanetlabs.com/2025/04/22/how-gd-netcetera-used-rama-to-100x-the-performance-of-a-product-used-by-millions-of-people/
- Agent-o-rama:
  https://blog.redplanetlabs.com/2025/11/03/introducing-agent-o-rama-build-trace-evaluate-and-monitor-stateful-llm-agents-in-java-or-clojure/

## Collaborative Editor: What They Actually Do

The collaborative editor article is the most direct evidence for our typing/edit
question.

They do not model a document edit as a full document replacement. They send
incremental edit objects:

```text
Add text at offset.
Remove N chars at offset.
Each edit says which document version it was based on.
```

The appended depot object is shaped like:

```text
Edit
  id       ; document id
  version  ; document version the client edited against
  offset
  action   ; AddText or RemoveText
```

Rama layout:

```text
*edit-depot hash-by document id

$$docs
  document-id -> latest document contents

$$edits
  document-id -> subindexed list of applied edits
```

Write topology:

```text
source edit
  -> already on document partition because depot hashes by id
  -> read latest version as count($$edits[doc-id])
  -> if client version is stale, read missed edits from $$edits
  -> transform incoming edit against missed edits
  -> read latest document
  -> apply transformed edit(s)
  -> write $$docs[doc-id]
  -> append transformed edit(s) to $$edits[doc-id]
```

Frontend workflow:

```text
browser holds document contents
browser holds latest version
browser holds edits sent but not acknowledged
browser holds pending edits

browser buffers changes locally
browser sends one change at a time
when acked, browser transforms pending edits, updates version, sends next pending change
```

After initial load, the full document is not repeatedly sent. Browser and server
send incremental edit objects.

Softland lesson:

```text
Do not send one Rama ActionRequest per physical keypress.
Do send semantic edit objects or edit batches.
Route them by artifact/document id.
Keep per-document edit history subindexed.
Use version/base-revision to resolve stale edits in Rama.
```

The article does not prescribe a debounce interval. It establishes the shape:

```text
local buffer -> incremental edit object -> document-keyed depot -> local transform
```

Important nuance:

```text
The article does not prove "batch every word/paragraph."
It proves "do not ship whole-document state for every edit."
It proves "the client owns a local pending buffer."
It proves "Rama receives versioned edit operations keyed by document."
```

Those edit operations may be small. For Softland, the stronger rule comes from
our own contract boundary: a `KernelEvent` is accepted world fact, so raw DOM
input should not automatically become a world action. The editor can coalesce
physical gestures into the smallest operation that is meaningful for the
artifact and collaboration model.

## Personalized Moderation: Move Read Logic Into Query Topologies

The moderation article uses two depots:

```text
*post-depot  hash-by receiving user
*mute-depot  hash-by muting user
```

and two PStates:

```text
$$posts
  user-id -> subindexed feed posts

$$mutes
  user-id -> subindexed set of muted users
```

The important part is the read side. Fetching a page with muted posts removed
can require scanning more than the requested number of posts. They implement
that loop in a Rama query topology, not in the client. This avoids many
client/backend roundtrips.

Softland lesson:

```text
Projection filtering should be a Rama query/materialization concern.
If answering a projection requires loops, joins, or repeated reads, put that in a query topology.
Do not make the UI do multiple PState reads and stitch policy/filtering itself.
```

## Graphs: Traversal Belongs In Query Topologies

The graph article stores family graph data in a PState keyed by person id. The
write topology writes both direct fields and inverse child edges by partitioning
to parent ids.

Graph traversal uses query topologies:

```text
loop
  -> hash partition by current node id
  -> local select neighbors/parents/children
  -> continue traversal
  -> origin partition
  -> aggregate result
```

It also shows temporary in-memory PStates inside a query topology to avoid
revisiting the same node.

Softland lesson:

```text
Trails, provenance, relations, and discourse graph traversals should not be UI loops.
Use Rama query topology traversal.
Use temporary query PState for visited sets when traversing graph-shaped knowledge.
```

## Recommendation Engine: Use Microbatch For Broad Derived Work

The recommendation article recomputes "who to follow" periodically. It uses:

```text
stream topology for immediate follow writes
tick depot every 30 seconds
microbatch topology for recommendation recomputation
per-partition cursor PState
all-partition fanout
batch block aggregation
```

The article explicitly chooses microbatch because single-digit millisecond
latency does not matter for this background work, while throughput and batching
do.

Softland lesson:

```text
Interactive target-local actions -> stream topology.
Broad derived work -> microbatch topology.
```

Examples of broad derived Softland work:

```text
redistillation
recommendations / related local worlds
projection rebuilds
large provenance indexes
semantic search refresh
agent-run aggregate metrics
```

## Timed Notifications: Use Tick Depots And TopologyScheduler

The timed notification article uses:

```text
*scheduled-post-depot hash-by id
*tick tick depot every second
TopologyScheduler-backed PState
```

Scheduling an item is one durable write. Expiration is handled by the topology
on ticks, and the callback writes to the right feed partition because the
scheduled item was partitioned correctly.

Softland lesson:

```text
Do not outsource scheduled world work to ad hoc cron if the result is world state.
Model scheduled work as Rama state.
Use tick depots / scheduler patterns for reminders, delayed distillation, expirations, and agent timeouts.
```

## G+D Netcetera: Denormalize For Reads, Incrementally

The Netcetera article is the clearest production-scale lesson.

Their old system recomputed pages repeatedly from normalized CMS data and leaned
on cache TTLs. Rama let them maintain denormalized page views incrementally as
content changed.

Important pattern:

```text
source truth remains coherent
PStates are shaped for reads
complex page/query result becomes a single key lookup
microbatch topology updates derived views as source data changes
```

The article also reports their real module shape:

```text
content module:
  7 depots
  24 PStates
  5 microbatch topologies
  3 stream topologies
  13 query topologies

user preferences module:
  3 depots
  3 PStates
  0 microbatch topologies
  3 stream topologies
  3 query topologies
```

Softland lesson:

```text
Do not worship one depot/module.
Do not make projections recompute from raw truth on every read.
Materialize projection-oriented PStates incrementally.
Keep source truth and read shape separate in the same Rama system.
```

## Agent-o-rama: Trace Is A First-Class Runtime Product

Agent-o-rama is not a low-level Rama topology tutorial, but it matters for
Softland's agent trail work.

Patterns to steal:

```text
agent execution as graph of named nodes
parallel branches visible in trace
aggregations explicit in trace
model calls traced automatically
arguments, returned values, token counts, and operation stats visible
human input prompts represented in the run UI
traces stay inside owned infrastructure
```

Softland lesson:

```text
Agent trails should be structured execution graphs, not only markdown logs.
Trace capture should be automatic at operation boundaries.
LLM calls, Rama reads/writes, tool calls, and human prompts should be trace nodes/events.
```

## Direct Update To The Softland Rule

Old draft:

```text
Do not make every physical UI gesture a world action.
```

Sharper rule after reading the editor post:

```text
Do not make every physical UI gesture a Rama write.
Use local buffers for gestures.
Emit semantic operations or operation batches.
Key the depot by the edited artifact/document/local-world.
Keep operation history in a subindexed PState.
Use version/base-revision to transform or reject stale edits.
Use reactive queries/proxies to notify clients of new versions.
```

For text editing, the next plausible Softland shape is:

```text
local editor buffer
  -> ActionRequest :text/edit-batch
       routing/key [:artifact artifact-id]
       action/params {:base-revision/id rev-id}
       payload {:ops [...]}
  -> Rama stream topology
       read latest version/edit history
       transform/validate ops
       accepted KernelEvent :text/edit-applied
       materialize text revision/edit history/projection invalidation
```

For unit judgment, the current request/event shape is fine because clicking
accept/reject is already a semantic operation, not a high-frequency gesture.
