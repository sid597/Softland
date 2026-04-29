# Rama Policy, Throughput, and the Softland World Kernel

Status: research post, 2026-04-29.

This is the detailed answer to the question:

```text
Is request -> Rama -> policy/decision -> materialization a throughput loss?
Is this how Rama wants policy-like work to be done?
What should Softland do next?
```

Short answer:

```text
Request-first is the right Rama shape.
One random "world requests" depot is not the final Rama shape.
Policy-after-depot is right when the policy decision is world truth.
Policy-in-one-generic-topology is not enough for scale.
```

The correct correction is not to move policy back before the depot. The
correction is to make the depot and PState layout match the entity whose state is
being decided.

## Sources Read

Primary sources:

- Rama depots docs:
  https://redplanetlabs.com/docs/~/depots.html
- Rama stream topology docs:
  https://redplanetlabs.com/docs/~/stream.html
- Rama microbatch topology docs:
  https://redplanetlabs.com/docs/~/microbatch.html
- Rama partitioner docs:
  https://redplanetlabs.com/docs/~/partitioners.html
- Rama tutorial 2, depots/ETLs/PStates:
  https://redplanetlabs.com/docs/~/tutorial2.html
- Twitter-scale Mastodon repo:
  https://github.com/redplanetlabs/twitter-scale-mastodon
- Twitter-scale Mastodon blog:
  https://blog.redplanetlabs.com/2023/08/15/how-we-reduced-the-cost-of-building-twitter-at-twitter-scale-by-100x/
- AfterHour chat blog:
  https://blog.redplanetlabs.com/2025/03/11/how-afterhour-built-an-ultra-scalable-chat-service-in-one-month-with-rama/
- Collaborative editor blog:
  https://blog.redplanetlabs.com/2025/04/01/massively-scalable-collaborative-text-editor-backend-with-rama-in-120-loc/

Local Softland files inspected:

- `src/app/server/rama/core.clj`
- `src/app/server/rama/util_fns.cljc`
- `docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md`

Extracted operating doctrine:

```text
docs/architecture/think-in-rama.md
```

## The Rama Rule That Matters

Rama's own docs establish the center:

```text
new data -> depots
depot records -> topologies
topologies -> PStates
queries/projections -> PStates
```

The docs describe depots as distributed logs, and tutorial 2 calls PStates
"materialized views of depots". This is the key inversion versus a CRUD app:
clients should not directly mutate indexed state. They append facts/commands to
logs, and topologies own the state transitions.

That supports the Softland correction:

```text
helper builds ActionRequest
  |
  v
append to Rama depot
  |
  v
Rama topology validates/interprets/decides
  |
  +--> rejected ActionDecision
  |
  v
accepted KernelEvent
  |
  v
PState materializations
```

So the answer to "should policy happen before the depot?" is:

```text
Not if the policy decision is part of world truth.
```

If Softland wants to know "who attempted what, against which projected target,
under which policy state, and why was it accepted/rejected", the request has to
enter the durable substrate before the authoritative decision.

But this does not mean every request type should share one physical depot
forever. Rama is very explicit that related records belong together and
unrelated records should split. The docs give two reasons:

```text
1. local ordering for related entity changes
2. performance/locality with the PStates that are updated
```

That is where our V0 is still rough.

## What Softland Currently Has

Current code center:

```text
src/app/server/rama/core.clj
```

The implemented loop is visible in the file header:

```text
helper builds ActionRequest
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
Rama ETL materializations
  |
  v
PState readers -> projection items with target refs
```

The current physical Rama module declares:

```clojure
(declare-depot setup *world-requests-depot :random)
```

Then the stream topology reads all request records:

```clojure
(source> *world-requests-depot :> *request)
(request-id *request :> *request-id)
(request-action-type *request :> *action-type)
(|hash *request-id)
(local-transform> [(keypath *request-id) (termval *request)] $$requests-by-id)
```

Then it branches by action type:

```text
:artifact/ingest
:unit/status-set
:compat/record
unknown action
```

Accepted requests derive `KernelEvent`s and update:

```text
$$events-by-id
$$artifacts
$$artifact-heads
$$branches
$$text-revisions
$$units-by-artifact
$$unit-status-by-branch
```

Rejected requests update:

```text
$$decisions-by-id
```

This is semantically better than the first attempt because the depot is no
longer a test depot of pre-decided events. It receives requests.

However, physically it has three throughput issues:

```text
1. depot partitioning is random
2. the first hash is by request id, not target/artifact/unit
3. every action type enters the same topology and gets filtered/branched there
```

Those are acceptable for V0 traceability. They are not the final shape Rama's
own examples point toward.

## Is This A Throughput Loss?

There are two separate questions hiding inside "throughput loss".

### 1. Is appending to a depot before processing a throughput loss?

Rama's answer is basically no, not in the way it would be in a conventional
"app server -> queue -> workers -> database" stack.

The collaborative editor post is the cleanest small example. It declares one
edit depot keyed by document id:

```clojure
(declare-depot setup *edit-depot (hash-by :id))
```

Then the stream topology owns the document PStates:

```text
*edit-depot
  -> $$docs
  -> $$edits
```

The post explicitly says the extra append step before materializing the PState
does not lower performance in their benchmarked model. The reason is not magic.
It is Rama's integration:

```text
depot partition
topology event
local PState partition
task event queue
replication/ack machinery
```

are all part of one system. This is not Kafka plus a consumer plus a database
with hand-built consistency glue.

So:

```text
append request -> process in Rama
```

is not itself the bad part.

### 2. Is our current one-random-depot V0 a throughput loss?

Yes. This part is the user's objection landing correctly.

Rama's depots docs say `Depot.random` distributes appends evenly, but local
ordering is not guaranteed. They also show that random partitioning followed by
hash partitioning to the PState key adds an extra network hop.

That maps directly onto our current V0:

```text
client append
  |
  v
random depot partition
  |
  v
source> request
  |
  v
hash by request id
  |
  v
later hash by artifact / branch / event id
```

For low-volume V0, fine. For production Softland, this is not the canonical
physical design.

The better Rama-shaped version is:

```text
client append
  |
  v
depot partitioned by target/routing key
  |
  v
topology starts on the task that owns the relevant PState shard
  |
  v
local policy read + local state transition where possible
```

In other words:

```text
request-first is correct
random global request inbox is not the scalable form
```

## What Rama Examples Actually Do

### Collaborative Editor: Key The Depot By The Thing Being Edited

The collaborative editor backend uses one depot because the domain is one hot
record type: document edit.

Important shape:

```text
*edit-depot, hash by document id
  |
  v
stream topology "core"
  |
  v
local select $$edits[doc-id]
  |
  v
transform edit if client version is behind
  |
  v
local transform $$docs[doc-id]
  |
  v
local transform $$edits[doc-id]
```

This is a policy-like decision. The backend does not blindly accept the user's
edit as already correct. It checks the current version, transforms against
missed edits, and then applies the result. The decision happens in Rama, but
the depot is keyed to make the decision local to the document.

Softland translation:

```text
unit/status-set should not enter a random world inbox
unit/status-set should route to the artifact/branch/unit locality that owns the
unit and policy state needed to decide the action
```

### AfterHour Chat: One Module, Many Depots, Many PStates

The AfterHour post says their chat service is a single Rama module with:

```text
8 depots
14 PStates
2 microbatch topologies
1 stream topology
0 query topologies
```

This is the opposite of "one world depot for everything". It is also not
microservice sprawl. It is one module whose physical depots and PStates match
the use cases:

```text
rooms
messages
members
presence
typing
```

The post distinguishes stream topologies from microbatch topologies:

```text
stream: lower latency
microbatch: higher throughput and exactly-once processing
```

AfterHour uses stream for chat operations that need quick feedback, and
microbatch for presence/typing where a few hundred milliseconds is acceptable.

Softland translation:

```text
interactive world actions -> stream topology
bulk distillation / projection rebuilds -> microbatch topology
```

### Twitter-Scale Mastodon: Split By Concept, Key By Entity, Derive More Streams

The Mastodon repo is the strongest evidence against "one generic depot".

The README says the backend has six Rama modules:

```text
Relationships
Core
Notifications
TrendsAndHashtags
GlobalTimelines
Search
```

Each module owns product logic and materialized indexes. The API layer mostly
calls Rama modules and returns JSON.

In `Relationships.java`, depot declarations include:

```java
setup.declareDepot("*followAndBlockAccountDepot", Depot.hashBy(ExtractAccountId.class));
setup.declareDepot("*muteAccountDepot", Depot.hashBy(ExtractAccountId.class));
setup.declareDepot("*featureAccountDepot", Depot.hashBy(ExtractAccountId.class));
setup.declareDepot("*followHashtagDepot", Depot.hashBy(ExtractToken.class));
setup.declareDepot("*listDepot", Depot.hashBy(ExtractListId.class));
setup.declareDepot("*filterDepot", Depot.hashBy(ExtractFilterAccountId.class));
setup.declareDepot("*authCodeDepot", Depot.hashBy(ExtractCode.class));
setup.declareDepot("*noteDepot", Depot.hashBy(ExtractAccountId.class));
```

In `Core.java`, depot declarations include:

```java
setup.declareDepot("*statusDepot", Depot.hashBy(StatusDepotExtractor.class));
setup.declareDepot("*scheduledStatusDepot", Depot.hashBy(ScheduledStatusDepotExtractor.class));
setup.declareDepot("*statusWithIdDepot", Depot.disallow());
setup.declareDepot("*accountDepot", Depot.hashBy(MastodonHelpers.ExtractName.class));
setup.declareDepot("*accountWithIdDepot", Depot.disallow());
setup.declareDepot("*favoriteStatusDepot", Depot.hashBy(MastodonHelpers.ExtractAccountId.class));
setup.declareDepot("*bookmarkStatusDepot", Depot.hashBy(MastodonHelpers.ExtractAccountId.class));
setup.declareDepot("*conversationDepot", Depot.hashBy(MastodonHelpers.ExtractAccountId.class));
```

Two things matter:

```text
1. user-originated depots are partitioned by business keys
2. topology-derived depots use Depot.disallow so clients cannot append to them
```

For example, `Core` accepts account registration records, generates account ids
inside the topology, writes account PStates, and then appends a derived
`AccountWithId` record to a topology-owned depot. The same pattern appears for
statuses:

```text
client appends AddStatus to *statusDepot
  |
  v
Core topology generates/looks up status id
  |
  v
updates author/status PStates
  |
  v
appends derived StatusWithId to *statusWithIdDepot
  |
  v
fanout/search/notification/global timeline modules consume derived stream
```

That is extremely relevant to Softland:

```text
ActionRequest depot: client-facing proposal
AcceptedEvent depot: optional topology-owned downstream stream
PStates: materialized indexes/projections
```

Our V0 currently stores accepted events directly into `$$events-by-id`. That is
fine for V0. Later, if downstream modules need a durable accepted-event stream,
we should introduce a `Depot.disallow` accepted-events depot and append to it
from the topology.

## What About Policy Specifically?

Policy is not one thing. There are at least four layers:

```text
edge guard
request typing/routing
authoritative world decision
projection/read filtering
```

They do not all belong in the same place.

### Edge Guard

This is not world truth. It is operational protection.

Examples:

```text
is there an HTTP session?
is the payload too large?
is the token structurally valid?
is this IP flooding us?
```

These can happen before Rama because they are not epistemic decisions about the
world. They protect the system.

If an edge guard rejects a request, Softland may or may not append an audit
record depending on product/security needs. But we should not pretend an edge
rate-limit reject is the same thing as a world-level ActionDecision.

### Request Typing / Routing

This can also happen before append if it is just packaging the request.

The Mastodon API sometimes queries Rama state before deciding which request
object to append. The blog's follow example reads the followee account and
chooses either `FollowLockedAccount` or `FollowAccount`. The repo also has
`acceptFollowRequest` checking whether a follow request exists before appending
an accept/reject record.

This does not mean the API owns world truth. It means the API is using Rama
state to avoid dumb appends or choose the correct command type.

Softland equivalent:

```text
projection can ask Rama:
  what actions are currently available for this target?

UI can show/hide commands based on that answer.

But the authoritative action still enters Rama as an ActionRequest and is
decided against current Rama state.
```

### Authoritative World Decision

This is the important one.

Examples:

```text
can actor X reject unit U on branch B?
does unit U exist in artifact A?
is the branch still open?
is this event causal parent valid?
does the proposed transition violate policy?
```

This should be decided inside Rama from durable PStates, because otherwise the
decision is based on stale, private, or non-replayable state.

The Mastodon Relationships topology does this kind of thing. For follow events,
it checks current relationship/request PStates, enforces relationship limits,
handles locked-account requests, turns accepted follow requests into actual
follows, and materializes multiple indexes. That is policy/business logic in the
Rama topology, not a pre-decided event handed to Rama.

The collaborative editor does the concurrency-policy version of this: the edit
is appended, the topology checks current version state, transforms if needed,
then writes the materialized document and edit log.

### Projection / Read Filtering

This is the read side of policy:

```text
what can this actor see?
what should be hidden?
what fields are redacted?
which local world/projection is allowed?
```

This can be served by PStates or query topologies. It should still be derived
from Rama state, not from hidden UI conditionals.

For Softland, projection policy matters as much as write policy because "public
form" means the map must not lie. A projection should carry target refs and
policy provenance so another mind can know why something appears or disappears.

## The Current V0 Throughput Shape

Current V0:

```text
*world-requests-depot :random
  |
  v
source every request
  |
  v
hash by request-id to store request
  |
  v
case action type
  |
  v
hash by artifact/event/branch as needed
```

This costs more than necessary:

```text
random start task
  -> request-id task
  -> artifact-id task
  -> branch-id task
```

It also weakens local ordering. If two requests mutate the same artifact or
branch, `:random` does not guarantee they enter the same depot partition. Rama
can preserve order between specific tasks once partitioned, but we should not
start by scattering causally related target updates.

Better V1:

```text
ActionRequest includes :routing/key
  |
  v
physical depot hashes by :routing/key
  |
  v
topology starts near the entity state it needs
```

For text artifact V1:

```text
:artifact/ingest
  routing/key = [:artifact artifact-id]

:unit/status-set
  routing/key = [:artifact artifact-id]

:artifact/redistill
  routing/key = [:artifact artifact-id]

:branch/commit
  routing/key = [:branch branch-id]

:policy/grant
  routing/key = [:policy target-kind target-id]
```

This does not mean request id is unimportant. It means request id is not the
primary locality key for processing. Request id is an identity/index key.

The topology should separate:

```text
processing locality key
identity lookup key
causal ordering key
projection target key
```

V0 collapsed these because it is a proof of the kernel contracts, not the final
physical design.

## Recommended Softland V1 Shape

Keep the logical contracts:

```text
ActionRequest
ActionDecision
KernelEvent
Target
Action
Policy
Materialization
Projection
Distillation
```

Change the physical shape.

### Step 1: Add Routing Key To ActionRequest

Add a top-level key:

```clojure
:routing/key [:artifact artifact-id]
```

The routing key should be derived by the request constructor, but the topology
should verify it. A bad/missing routing key should become a rejected
ActionDecision or be routed to a quarantine depot/path.

The initial rule can be:

```text
if payload has artifact/id:
  [:artifact artifact-id]
else if target has target/id:
  [target-kind target-id]
else:
  [:request request-id]
```

The important part is that target-changing actions route by the target's owner,
not by request id.

### Step 2: Replace Random Depot With Hash-By Routing Key

Move from:

```clojure
(declare-depot setup *world-requests-depot :random)
```

to the Rama Clojure shape shown in the editor post:

```clojure
(declare-depot setup *world-requests-depot (hash-by :routing/key))
```

This is still one depot, but it removes the obvious random-partition loss.

### Step 3: Stop Starting The Topology By Hashing Request Id

Current:

```text
source request
  |
  v
hash request-id
  |
  v
store request
  |
  v
do action work
```

Better:

```text
source request on routing-key partition
  |
  +--> process authoritative decision locally where possible
  |
  +--> store request/decision indexes by request id as side indexes
```

The request id index is for lookup/debug. It should not force all business
logic to start on the request-id partition.

### Step 4: Split Physical Depots Once Action Families Are Clear

Do not split too early by current namespace/module names. Split by world
semantics and throughput shape.

Likely V1/V2 physical depots:

```text
*artifact-requests-depot
  ingest, edit, redistill, fork artifact
  hash by artifact id

*unit-requests-depot
  status-set, promote, hide, supersede
  hash by artifact id or branch+artifact id

*branch-requests-depot
  fork, commit, merge, publish
  hash by branch id

*policy-requests-depot
  grant, revoke, install policy
  hash by policy target

*agent-run-requests-depot
  start, cancel, resume, attach trail
  hash by run id or local-world id
```

Optional topology-owned depots:

```text
*accepted-events-depot       Depot.disallow
*artifact-events-depot       Depot.disallow
*unit-events-depot           Depot.disallow
*projection-invalidations    Depot.disallow
```

The Mastodon pattern for topology-owned depots is `Depot.disallow`: clients
cannot append derived facts directly.

### Step 5: Make Policy PStates Real

Current V0 has `$$policies` declared but not meaningfully populated.

V1 needs policy/state PStates like:

```text
$$actor-capabilities
  actor-id -> capability set / roles / grants

$$policy-by-target
  target-key -> policy document

$$branch-policy
  branch-id -> branch policy

$$target-acl
  target-key -> actor/group permissions

$$policy-decisions-by-request
  request-id -> compact decision record
```

Then authoritative policy checks become local where possible:

```text
request routes to [:artifact art_1]
  |
  v
read $$units-by-artifact[art_1][unit-id]
read $$policy-by-target[artifact art_1]
read branch policy / actor grant mirror
  |
  v
accepted or rejected ActionDecision
```

If actor policy lives on a different key from artifact policy, there are two
options:

```text
1. mirror compact actor grants into the artifact/target policy partition
2. partition to actor state, read, then return/partition to target state
```

The right choice depends on write/read ratios. For Softland, target-local policy
mirrors are likely better because target actions should be cheap and common.

### Step 6: Use Query Topologies For "Can I Do This?"

Softland projections need fast questions:

```text
for actor sid and target unit U:
  what actions are available?
  why?
  what policy made that true?
```

That should be a query topology or PState read, not hidden UI logic.

Then UI flow becomes:

```text
projection reads allowed actions
  |
  v
user chooses an action
  |
  v
append ActionRequest
  |
  v
Rama decides against current state
```

The query result is a convenience and preview. The ActionDecision is truth.

### Step 7: Use Stream Ack Returns For Interactive Decisions

Current V0 appends with append-ack and then polls for a decision:

```clojure
(foreign-append! (:world-requests-depot runtime) request :append-ack)
(await-decision runtime (:request/id request))
```

Rama's stream topology docs describe ack behavior: `AckLevel.ACK` waits for
colocated stream topology processing, and stream topologies can return ack
values to the appender.

For interactive Softland actions, better options are:

```text
async action:
  append with append-ack
  projection updates later

interactive action requiring immediate answer:
  append with ACK / stream ack return
  return ActionDecision or event id

bulk distillation:
  append request
  microbatch processes later
  projection shows pending/progress state
```

Polling is fine for V0 tests. It is not the final interaction contract.

## Concrete Example 1: Cold Artifact Ingest

Current V0:

```text
ActionRequest :artifact/ingest
  request/id req_1
  artifact/id art_1
  text/content "a\nb\nc"
  |
  v
*world-requests-depot :random
  |
  v
hash req_1
  |
  v
store request by id
  |
  v
interpret/authorize
  |
  v
hash event id, branch id, artifact id
  |
  v
store event/artifact/head/revision/units
```

Recommended V1:

```text
ActionRequest :artifact/ingest
  request/id req_1
  routing/key [:artifact art_1]
  artifact/id art_1
  text/content "a\nb\nc"
  |
  v
*artifact-requests-depot hash-by routing/key
  |
  v
starts on artifact partition
  |
  +--> side index request by id
  |
  v
local policy check for creating/updating artifact
  |
  v
derive KernelEvent :artifact/ingested
  |
  v
local materializations:
    $$artifacts[art_1]
    $$artifact-heads[art_1]
    $$text-revisions[art_1][rev_1]
    $$units-by-artifact[art_1]
  |
  v
optional derived depot:
    *accepted-events-depot, Depot.disallow
```

Throughput difference:

```text
V0 starts random and hops.
V1 starts where the artifact state lives.
```

## Concrete Example 2: Mid-State Unit Rejection

Starting state:

```text
artifact art_1 exists
branch main exists
unit art_1/line/2 exists
actor sid has :unit/judge on artifact art_1
```

Projection row:

```text
preview: "line 2"
target:
  kind: unit
  id: art_1/line/2
  address:
    artifact/id: art_1
    revision/id: rev_1
    range: 5..11
```

User action:

```text
sid clicks reject
```

Recommended V1 request:

```clojure
{:request/id "req_2"
 :request/type :unit/status-set
 :routing/key [:artifact "art_1"]
 :actor {:actor/id "sid"}
 :branch {:branch/id "main"}
 :target {:target/kind :unit
          :target/id "art_1/line/2"}
 :action {:action/type :unit/status-set
          :action/capability :unit/judge
          :action/params {:status :rejected}}
 :payload {:artifact/id "art_1"
           :unit/id "art_1/line/2"
           :status :rejected}}
```

Rama flow:

```text
*unit-requests-depot or *artifact-requests-depot
  hash-by [:artifact art_1]
  |
  v
local read $$units-by-artifact[art_1][art_1/line/2]
local/mirrored read policy for actor+artifact+branch
check transition :unjudged -> :rejected
  |
  +--> reject:
  |      store ActionDecision only
  |
  v
accept:
  derive KernelEvent :unit/status-set
  update $$unit-status-by-branch[main][art_1/line/2]
```

Projection result:

```text
canonical projection no longer includes line 2
discarded projection includes line 2
both rows preserve target/provenance back to request/event
```

This is "policy after depot", but not "policy after random global queue".

## The Correct Mental Model

The sloppy version:

```text
validate before depot
authorize before depot
append event
hope the event is truth
```

The corrected semantic version:

```text
construct request envelope
append request
Rama decides
accepted event becomes truth
rejected decision remains audit
```

The corrected throughput version:

```text
construct request envelope with routing key
append to a depot keyed by the state being touched
Rama decides using local/co-located PStates where possible
accepted event materializes local indexes
derived streams fan out only when needed
```

That is the shape to hold onto.

## What We Did Not Include In V0

V0 intentionally does not include:

```text
routing/key on ActionRequest
hash-by routing key depot partitioning
physical depot split by action family
real policy PStates
query topology for allowed actions
stream ack return for ActionDecision
topology-owned accepted-events depot
microbatch distillation/projection rebuilds
performance tests for extra hops
ordering tests for same-target requests
backpressure/rate-limit strategy
multi-module deployment shape
```

That is why V0 should be treated as:

```text
canonical logical kernel
not canonical final physical Rama topology
```

## The Compatibility Promise

Future-compatible means:

```text
ActionRequest semantics do not change
ActionDecision semantics do not change
KernelEvent semantics do not change
target refs stay stable
provenance survives depot splitting
materialized projections remain derived, not source truth
```

Future-compatible does not mean:

```text
one depot forever
random partitioning forever
request-id as processing locality forever
polling decisions forever
```

So the accountability line is:

```text
If future versions preserve the logical contracts while changing physical depot
layout, this V0 is compatible.

If future versions require changing what ActionRequest/ActionDecision/KernelEvent
mean, then V0 failed.
```

## Recommendation

Do not revert to pre-depot policy. That would lose the trail.

Do not freeze the current one-depot V0 as the production shape. That would lose
Rama's locality and throughput advantages.

Next implementation step:

```text
Add :routing/key to ActionRequest.
Change *world-requests-depot from :random to hash-by :routing/key.
Rework the topology so request-id indexes are side indexes, not the first
business partition.
Add one real policy PState and make unit/status-set check it.
Add tests proving same-artifact requests preserve target-local ordering.
```

After that:

```text
split physical depots by action family
add query topology for allowed actions
add stream ack return for ActionDecision
add topology-owned accepted-events depot only if downstream fanout needs it
```

This is the Rama-aligned stance:

```text
Requests first.
Decisions in Rama.
Depots keyed by the world entity they affect.
PStates shaped exactly for the questions/actions they serve.
Separate depots when unrelated streams would force filtering or destroy locality.
```

That is the version I would be willing to hold as the next canonical direction.
