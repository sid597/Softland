# Rama World Kernel + Text Instance

Status: current mental model anchor, 2026-04-28.

This note captures the working model from the April 28 discussion. Its purpose is to give implementation a single artifact to point at when building the Rama-backed text/world substrate.

The key correction: **text is not the ontology**. Text is the first carrier we use to exercise the general kernel.

## Core Loop

The whole system should be understood as one loop:

```text
Projection shows materialized state
  -> user/agent acts on a projected target
  -> helper/API builds an ActionRequest
  -> ActionRequest enters a Rama depot
  -> Rama interpreter/policy derives accepted KernelEvent or rejected ActionDecision
  -> Rama ETL updates materialized state
  -> projection changes
```

Everything else is a contract inside this loop.

## General Kernel

The general kernel is independent of text, chat, PDF, code, image, or future formal-model artifacts.

The first implementation focus is to work out the **general kernel data structure**. Text-specific payloads come after this boundary is stable.

Post-V0 correction:

```text
ActionRequest asks.
ActionDecision records the answer.
KernelEvent happened.
```

`ActionRequest` is not another carrier/instance. It is a lifecycle envelope in
the general kernel. Text, PDF, chat, code, image, and model artifacts are
carriers.

See:

```text
docs/current-mental-model/architecture/action-request-kernel-routing.md
```

### Event Contract

An event is the durable record that Rama accepted something as having happened.
Clients append `ActionRequest`s; Rama derives accepted `KernelEvent`s or
rejected `ActionDecision`s. Clients do not mutate PStates directly.

```clojure
{:event/id ...
 :event/type :artifact/ingested
 :event/time-ms ...
 :event/schema-version 1

 :actor {:actor/id ...
         :actor/type :human}

 :branch {:branch/id ...}

 :context {:context/id ...
           :projection/id ...
           :question/id ...}

 :target {:target/kind :artifact
          :target/id ...
          :target/address nil}

 :action {:action/type :artifact/ingest
          :action/capability :artifact/create}

 :payload {...}

 :causal {:parents [...]
          :correlation/id ...
          :intent/id ...}

 :ordering {:key [:artifact "..."]}

 :policy {:required-capabilities #{:artifact/create}
          :visibility :private}

 :provenance {:source/type :manual
              :source/ref nil}}
```

`event/type` says what happened. `target` says what it happened to. `action`
says which capability/change was accepted. `payload` carries type-specific data.
`ordering/key` is the local ordering key for the accepted fact.

Request routing uses `:routing/key` on `ActionRequest`.

The kernel event answers:

```text
What happened?       event/type
Who did it?          actor
In which world?      branch
From what context?   context
To what target?      target
By what action?      action
With what causality? causal
```

Everything artifact-specific, text-specific, PDF-specific, or code-specific should live in `payload` or `target/address`, not in the kernel envelope.

### Kernel Types

The kernel should make these references explicit:

```clojure
;; ActorRef
{:actor/id string?
 :actor/type :human|:agent|:system|:bot}

;; BranchRef
{:branch/id string?}

;; ContextRef
{:context/id string?
 :projection/id string?
 :selection/id string?
 :question/id string?}

;; TargetRef
{:target/kind :artifact|:revision|:address|:unit|:branch|:projection|:policy|:relation
 :target/id string?
 :target/address map?}

;; ActionRef
{:action/type keyword?
 :action/capability keyword?
 :action/params map?}

;; CausalRef
{:parents [event-id ...]
 :correlation/id string?
 :intent/id string?}
```

So the general kernel data structure is:

```text
KernelEvent =
  identity
  + type
  + actor
  + branch
  + context
  + target
  + action
  + payload
  + causality
  + ordering
  + policy
  + provenance
```

This is the object to work out before optimizing for text.

### Request And Decision Contracts

The first physical record Rama sees for a world write is an `ActionRequest`.

```clojure
{:request/id ...
 :request/type :artifact/ingest
 :request/time-ms ...
 :request/schema-version 1
 :routing/key [:artifact "..."]
 :actor {...}
 :branch {...}
 :context {...}
 :target {...}
 :action {...}
 :payload {...}
 :causal {...}
 :provenance {...}}
```

`ActionRequest` does not have a top-level `:event/id`; no event exists yet.

Rama records an `ActionDecision`:

```text
accepted decision -> carries :event/id and may include/point to KernelEvent
rejected decision -> carries :event/id nil plus :decision/reason/errors, no KernelEvent
```

The same shared contracts appear in request and accepted event. The difference
is timing and ownership:

```text
ActionRequest is proposed by edge/helper.
ActionDecision is recorded by Rama.
KernelEvent is derived by Rama only after acceptance.
```

### Target Contract

A target is a stable reference to the thing an action/event is about.

```clojure
{:target/kind :artifact|:revision|:address|:unit|:branch|:projection|:policy|:relation
 :target/id ...
 :target/address ...}
```

The important split:

```text
Artifact identity is not the same as an address inside the artifact.
Address is not the same as semantic unit.
Projection target is not the same as durable object truth.
```

### Action Contract

An action is a requested or interpreted operation over a target.

```text
UI gesture / agent intent
  -> action request
  -> permission check
  -> domain event
```

Actions should target capabilities, not UI widgets.

Examples:

```text
artifact ingest
text insert/delete/replace
unit create/split/merge
unit accept/reject/promote
branch fork/commit
policy grant/revoke
```

### Materialization Contract

PStates are derived reader-friendly shapes. They are not source of truth.

```text
Depot event log
  -> ETL
  -> PState materializations
  -> readers
```

Readers include UI, LLM context builders, query endpoints, permission checks, replay/debug tools, background distillers, and review/export generators.

### Projection Contract

A projection is a view over materialized state. Every meaningful item shown by a projection should carry a target reference.

```text
visible line
  -> target: text unit or address

visible artifact card
  -> target: artifact id

visible branch node
  -> target: branch id
```

If a UI interaction changes world meaning/history, the projection must lift that gesture into an action request.

### Policy Contract

Permission is about allowed observations and actions, not just row access.

```text
Can this subject observe this artifact/unit/relation/view/query?
Can this subject perform this action on this target?
```

A projection must not leak forbidden data through summaries, diffs, counts, branch names, or derived views.

### Distillation Contract

Distillers turn raw artifacts/events into derived units, relations, summaries, or branches. Distillers are versioned and non-final.

```text
raw artifact stays
distiller v1 derives units
distiller v2 may derive better units later
refinement maps preserve provenance
```

## Rama Mapping

There are three separate choices:

```text
logical kernel contracts
request/decision/event lifecycle
physical depot layout
```

The logical kernel contracts should be stable. Physical depots can start simple
and later split by ordering/throughput needs.

### Depot Families

Likely families:

```text
*world-requests-depot
  generic early request stream, hash by routing/key

*artifact-requests-depot
  artifact ingestion and metadata requests, hash by artifact id

*text-requests-depot
  text operations and revision requests, hash by artifact id

*unit-requests-depot
  unitization and unit status requests, hash by artifact id or unit id

*branch-requests-depot
  branch, commit, fork, merge requests, hash by branch id

*policy-requests-depot
  grants/revocations/policy updates, hash by target or subject

*accepted-events-depot
  optional topology-owned derived stream, no client appends
```

For the first implementation, it is acceptable to start with one generic request
depot. The next implementation should add `:routing/key` and hash by that key.
Splitting depots later should not change `ActionRequest`, `ActionDecision`, or
`KernelEvent` meaning.

### PState Families

Core materializations:

```text
$$events-by-id
  event-id -> event summary / payload pointer

$$timeline-index
  time bucket -> event ids

$$artifacts
  artifact-id -> metadata, source, type, root event

$$artifact-heads
  artifact-id -> current revision/head pointer

$$branches
  branch-id -> parent, head commit, owner, purpose

$$policies
  target-id -> policy entries
```

Text materializations:

```text
$$text-revisions
  artifact-id -> revision-id -> text state/hash/parent

$$anchors
  artifact-id -> anchor-id -> revision/range/resolution info

$$units-by-artifact
  artifact-id -> unit-id -> type, anchor, derived-by, preview

$$unit-status-by-branch
  branch-id -> unit-id -> accepted/rejected/hidden/promoted/superseded

$$commits-by-branch
  branch-id -> commit-id -> selected units, interpretation, causal events
```

Projection/read materializations:

```text
$$projection-cache
  projection-id -> materialized reader shape

$$visible-units-by-view
  branch-id -> view-id -> ordered visible unit ids

$$discarded-index
  branch-id -> rejected/hidden unit ids
```

## Text Instance

Text is the first specimen passed through the kernel.

### Text Artifact

A raw text artifact is the coarse thing that arrived before interpretation.

```clojure
{:artifact/id ...
 :artifact/type :text
 :source/type :paste|:file|:chat|:pdf-extraction|:llm-response
 :content/hash ...
 :created-by ...
 :created-at ...
 :root-event-id ...}
```

It is not yet line, paragraph, claim, decision, function, task, or answer fragment. Those are units derived later.

This is a text payload plugged into the kernel, not a replacement for the kernel:

```clojure
{:event/type :artifact/ingested
 :target {:target/kind :artifact
          :target/id "art_1"
          :target/address nil}
 :action {:action/type :artifact/ingest
          :action/capability :artifact/create}
 :payload {:artifact/id "art_1"
           :artifact/type :text
           :source/type :paste
           :text/content "line one\nline two"
           :content/hash "..."}}
```

A unit status update uses the same kernel, with a different target and payload:

```clojure
{:event/type :unit/status-set
 :target {:target/kind :unit
          :target/id "unit_2"
          :target/address nil}
 :action {:action/type :unit/status-set
          :action/capability :unit/judge}
 :payload {:status :rejected
           :reason "not useful in canonical view"}}
```

### Text Events

Minimum useful event set:

```text
:artifact/ingested
:text/revision-created
:text/op-applied
:anchor/created
:unit/created
:unit/status-set
:commit/created
```

V0 may store full text snapshots for simplicity. The event contract should not require this forever; later materializations can use chunks, ropes, or piece tables.

### Text Actions

Text-specific:

```text
insert
delete
replace
paste
create revision
```

General actions over text-derived units:

```text
create unit
split unit
merge unit
accept unit
reject unit
promote unit
link unit
annotate unit
```

### Text Projection Flow

Ingest:

```text
user pastes text
  -> ActionRequest: artifact ingest
  -> ActionDecision: accepted or rejected
  -> KernelEvent: :artifact/ingested, accepted only
  -> PStates: $$artifacts, $$text-revisions, $$artifact-heads
  -> projection: editor/document view shows text
```

Unitize:

```text
system splits text into lines/paragraphs
  -> event: :unit/created for each unit, or batch event
  -> PState: $$units-by-artifact
  -> projection: visible lines/paragraphs carry target refs
```

Reject:

```text
user rejects line 3
  -> projected target: unit id
  -> ActionRequest: unit status set
  -> Rama policy/state check
  -> ActionDecision: accepted or rejected
  -> KernelEvent: :unit/status-set {:status :rejected}, accepted only
  -> PState: $$unit-status-by-branch
  -> projection: canonical view hides/dims it
  -> discarded index keeps it available for later learning/debug
```

## Implementation Boundary

The first development pass should not build a full editor ontology. It should implement the smallest loop that proves the contracts:

```text
generic lifecycle envelopes
kernel reference types
target refs
branch id
actor id
context id
action refs
policy check hook
materializer convention

then text plugin:
text artifact
text revision
text range/anchor
text unitization
unit status update
```

## Done Criteria For First Pass

The first pass is real when all of this works:

```text
1. Ingest one text artifact through an ActionRequest.
2. Query materialized artifact/text state from a PState.
3. Derive at least line or paragraph units.
4. Show units in a projection with stable target refs.
5. Accept/reject one unit through ActionRequest -> ActionDecision -> KernelEvent -> PState update.
6. Query canonical view and discarded view separately.
7. Preserve provenance back to the raw artifact/request/decision/event.
```

## Design Rules

```text
Accepted KernelEvents are truth. ActionRequests are proposals. PStates are derived.
Text is an instance, not the ontology.
Actions target capabilities, not widgets.
Every meaningful projection item carries a target ref.
Every accepted durable action becomes an event.
Permission gates observations, not only objects.
Raw artifacts stay; distillers can improve later.
Do not make every high-frequency UI gesture a world action.
```

## Open Decisions

These should remain explicit during implementation:

```text
One generic request depot first, or separate request depot families immediately?
Snapshot text revisions first, or operation log first?
What is the first anchor representation?
Is branch id required for every event, or only interpretation/status events?
Where is policy enforced first: edge guard, Rama write decision, query path, projection path, or all?
Which unitization is V0: line, paragraph, or manually selected range?
```
