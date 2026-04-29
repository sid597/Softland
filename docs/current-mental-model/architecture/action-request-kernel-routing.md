# ActionRequest, KernelEvent, Policy, And Routing

Status: clarification note, 2026-04-29.

This note exists because the mental model shifted during implementation.

Old simplified model:

```text
projection -> typed event -> Rama -> materialized state
```

Corrected model:

```text
projection -> ActionRequest -> depot/routing -> Rama decision
  -> accepted KernelEvent or rejected ActionDecision
  -> materialized state
```

The shift is not cosmetic. It separates "someone asked the world to change" from
"the world accepted that change as fact".

## One Sentence

```text
ActionRequest is a proposed change; KernelEvent is an accepted fact; ActionDecision
is the durable answer; routing is how the request reaches the state needed to
decide.
```

The first physical record Rama sees for a user/world write is an
`ActionRequest`, not a `KernelEvent`.

The event contract still exists. It now means:

```text
KernelEvent = accepted world fact derived by Rama after decision
```

## Is ActionRequest Another Instance?

No.

Softland has three different axes that are easy to collapse:

```text
Kernel contract = shared vocabulary/shape across all world changes
Lifecycle envelope = where a thing is in the accept/reject pipeline
Instance/carrier = text, PDF, chat, code, image, model, etc.
```

`ActionRequest` is not a text instance, PDF instance, or separate ontology. It is
a lifecycle envelope in the general kernel.

```text
ActionRequest asks.
ActionDecision answers.
KernelEvent happened.
```

A text artifact ingest and a PDF artifact ingest can both use `ActionRequest`.
Text/PDF/chat/code differences live in `:payload`, `:target/address`, and
distillation/materialization details.

## The General Kernel Is Not One Map

The general kernel is the set of contracts shared across the lifecycle:

```text
actor
branch
context
target
action
payload
causality
ordering/routing
policy
provenance
```

Those contracts appear in different envelopes.

### ActionRequest

`ActionRequest` is the request/proposal entering Rama.

It says:

```text
who is asking?
from what context/projection?
what target are they acting on?
what action/capability are they requesting?
what payload parameters are they proposing?
what causal/provenance trail produced the request?
where should Rama route the request to decide?
```

Shape:

```clojure
{:request/id ...
 :request/type :unit/status-set ; usually mirrors or derives from action/type
 :request/time-ms ...
 :request/schema-version 1

 :routing/key [:artifact "art_1"]

 :actor {:actor/id "sid"
         :actor/type :human}

 :branch {:branch/id "main"}

 :context {:projection/id "text/canonical"
           :selection/id "..."}

 :target {:target/kind :unit
          :target/id "art_1/line/2"
          :target/address {...}}

 :action {:action/type :unit/status-set
          :action/capability :unit/judge
          :action/params {:status :rejected}}

 :payload {:artifact/id "art_1"
           :unit/id "art_1/line/2"
           :status :rejected}

 :causal {:parents [...]
          :correlation/id ...
          :intent/id ...}

 :provenance {:source/type :projection
              :source/ref "text/canonical"}}
```

`ActionRequest` is allowed to be wrong. The target can be missing, the actor can
lack permission, the status can be invalid, or the request can be stale. That is
why it needs a decision.

`ActionRequest` should not have a top-level `:event/id`, because no event exists
yet. For retries/client reconciliation, use explicit request-side fields:

```text
:idempotency/key
:client/op-id
:proposed/event-id  ; only if we intentionally support caller-proposed ids
```

Do not hide event identity inside arbitrary payload unless it is temporary test
plumbing.

### Action Object

Keep `:action`. Do not collapse it into `:request/type`.

`ActionRequest` is the whole envelope. `:action` is the operation object inside
the envelope:

```clojure
:action {:action/type :unit/status-set
         :action/capability :unit/judge
         :action/params {:status :rejected
                         :reason "not canonical"}}
```

`:action` is intentionally expressive. It can grow metadata without changing
the envelope:

```text
capability
params
mode
tool
gesture
intent
operation family
client/editor hints
```

Rule:

```text
:action/type is the canonical operation.
:request/type may exist for dispatch/backcompat, but must not drift from action/type.
```

### ActionDecision

`ActionDecision` is the answer Rama records after checking current durable
state.

Accepted:

```clojure
{:decision/id "req_2/decision"
 :decision/status :accepted
 :request/id "req_2"
 :event/id "req_2/event"
 :event <KernelEvent>
 :decided-at ...}
```

Rejected:

```clojure
{:decision/id "req_2/decision"
 :decision/status :rejected
 :request/id "req_2"
 :reason :actor-not-authorized
 :errors [...]
 :decided-at ...}
```

Rejected requests are not world facts. They are still part of the trail.

Storage model:

```text
$$requests-by-id[request-id] = ActionRequest
$$decisions-by-id[decision-id] = ActionDecision
$$events-by-id[event-id] = KernelEvent, accepted only
```

The decision is not added to the request. It points back to the request.

### KernelEvent

`KernelEvent` is the accepted world fact derived from an accepted request.

It says:

```text
what did the world accept as having happened?
which request caused it?
what target changed?
what ordering key governs this accepted fact?
what policy/provenance should future projections preserve?
```

Shape:

```clojure
{:event/id ...
 :event/type :unit/status-set
 :event/time-ms ...
 :event/schema-version 1

 :actor {:actor/id "sid"}
 :branch {:branch/id "main"}
 :context {:projection/id "text/canonical"}

 :target {:target/kind :unit
          :target/id "art_1/line/2"
          :target/address {...}}

 :action {:action/type :unit/status-set
          :action/capability :unit/judge
          :action/params {:status :rejected}}

 :payload {:artifact/id "art_1"
           :unit/id "art_1/line/2"
           :status :rejected}

 :causal {:parents [...]
          :correlation/id "req_2"}

 :ordering {:key [:artifact "art_1"]}

:policy {:required-capabilities #{:unit/judge}
          :decision/id "req_2/decision"
          :visibility :private}

 :provenance {:source/type :action-request
              :source/ref "req_2"}}
```

## Authentication, Routing, Authorization

These are different.

### Authentication

Authentication answers:

```text
who is the caller?
is there a valid session/token?
is this request obviously malformed or abusive?
```

This happens before Rama for operational reasons. It is an edge guard, not a
world decision.

If authentication fails, the request usually does not become an `ActionRequest`.
There may be a separate security/audit path later, but it is not the normal
world-kernel action path.

### Routing

Routing answers:

```text
which depot/partition should receive this request so Rama can decide locally?
```

Routing happens after basic authentication and request parsing, but before the
authoritative world decision.

Routing is not approval.

Example:

```text
request says target unit art_1/line/2
  -> routing/key [:artifact "art_1"]
  -> request lands on the partition that owns artifact/unit/policy state
  -> Rama decides accept or reject
```

If the artifact does not exist, that is not a routing failure. The request still
routes to `[:artifact "art_1"]`, then Rama records a rejected `ActionDecision`
with reason `:target-not-found`.

If the request has no usable target/routing key, route it to a fallback key such
as:

```text
[:request request-id]
[:quarantine action-type]
```

and reject from there.

### Authorization

Authorization answers:

```text
may this actor perform this action on this target in this branch/current state?
```

This belongs inside Rama when the answer should be replayable, auditable, or
explainable.

Authorization needs more than the `ActionRequest` map. It needs current durable
state:

```text
actor grants/capabilities
target policy
branch policy
unit/artifact existence
current unit status
causal parent/head state
transition rules
```

That is why the request routes before authorization. The route gets the request
to the state needed to make the decision.

## Why Action And Policy Contracts Were Not Enough

The original kernel had:

```text
action contract
policy contract
```

Those are still correct. They were just not enough to express lifecycle.

The `action` contract says:

```text
what operation is being requested or recorded?
what capability does it require?
what parameters does it carry?
```

The `policy` contract says:

```text
what rules/grants/visibility apply to this action/target/projection?
```

But the decision also needs:

```text
current Rama state
target existence
branch state
prior events
policy PStates
actor state
transition rules
```

So:

```text
Action contract + policy contract describe the decision surface.
ActionRequest carries a proposed use of that surface.
Rama topology reads current state and records ActionDecision.
KernelEvent exists only if the decision is accepted.
```

## Does Everything Get Routed After We Authenticate?

For normal world actions:

```text
yes, after basic edge authentication, construct and route an ActionRequest.
```

More precise flow:

```text
edge request
  |
  v
authenticate transport/session
  |
  +--> fail: reject at edge, no world ActionRequest
  |
  v
parse intent + target from projection/API
  |
  v
build typed ActionRequest
  |
  v
derive routing/key from target/action
  |
  v
append to routed Rama depot
  |
  v
Rama reads policy/state PStates
  |
  +--> rejected ActionDecision
  |
  v
accepted KernelEvent
  |
  v
materialized PStates
  |
  v
projection updates
```

The important line:

```text
Authentication gates entry to the system.
Routing finds the decision locality.
Authorization/validation decides world truth.
```

## Accept Vs Reject

These are not three requests. They are three records in one lifecycle.

Accepted path:

```text
ActionRequest stored
ActionDecision stored as accepted
KernelEvent derived and stored
PStates materialized from KernelEvent
```

Rejected path:

```text
ActionRequest stored
ActionDecision stored as rejected
no KernelEvent
no world-state materialization
```

Example accepted decision:

```clojure
{:decision/id "req_1/decision"
 :decision/status :accepted
 :request/id "req_1"
 :request/type :unit/status-set
 :event/id "evt_2"
 :decided-at 1777400000012}
```

Example rejected decision:

```clojure
{:decision/id "req_1/decision"
 :decision/status :rejected
 :request/id "req_1"
 :request/type :unit/status-set
 :reason :actor-not-authorized
 :errors []
 :decided-at 1777400000012}
```

## I/O Discipline

Rama modules are production backends. Do not trade I/O efficiency for code
simplicity.

`ActionRequest` is for meaningful durable world actions, not every physical UI
gesture.

For an editor:

```text
bad: one ActionRequest per keystroke
good: local typing buffer -> semantic text/edit-batch request
```

Authorize at the largest safe scope:

```text
Can actor edit this artifact/session/branch?
```

Then batch keystrokes into meaningful operations:

```clojure
{:action/type :text/edit-batch
 :action/capability :text/edit
 :action/params {:base-revision/id "rev_7"}
 :payload {:ops [{:insert "hello" :at 42}
                 {:delete 3 :at 50}]}}
```

Store only the records needed for replay, audit, projection, recovery, or
debugging. If a hot path can return accepted decision data via stream ack and
derive accepted state from `KernelEvent`, do not add extra PState writes just to
make a diagram prettier.

## Concrete Example: Reject A Unit

Projected row:

```text
line "foo"
target:
  kind: unit
  id: art_1/line/2
  address: revision rev_1 range 4..7
```

User clicks reject.

Edge:

```text
session says actor = sid
payload is small and well-formed
```

ActionRequest:

```text
request/type :unit/status-set
routing/key [:artifact art_1]
actor sid
target art_1/line/2
action capability :unit/judge
payload status :rejected
```

Rama decision:

```text
read $$units-by-artifact[art_1][art_1/line/2]
read policy/capability state for sid + art_1 + branch main
check transition to :rejected
```

If allowed:

```text
store accepted ActionDecision
derive KernelEvent :unit/status-set
update $$unit-status-by-branch[main][art_1/line/2]
```

If forbidden:

```text
store rejected ActionDecision :actor-not-authorized
do not derive KernelEvent
do not update unit status
```

## The Mental Model Shift

Before:

```text
event = input and truth
```

After:

```text
request = input
decision = answer
event = accepted truth
```

Before:

```text
policy contract seemed like something the helper could enforce before append
```

After:

```text
policy contract is part of the request/event vocabulary, but the authoritative
policy decision is a Rama topology reading durable state
```

Before:

```text
depot is where events go
```

After:

```text
client-facing depot receives requests
optional topology-owned depots may receive accepted/derived events
PStates materialize the accepted world
```

This is the whole correction.
