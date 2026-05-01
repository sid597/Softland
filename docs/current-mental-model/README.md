# Current Mental Model

Status: global context pack, 2026-05-01.

This folder is the current working mental model for the Rama/text/world-kernel work. Use it to start new chats without rehydrating the whole prior conversation.

This is not the same thing as a task handoff. Global context tells a new session
how to think with us; a handoff tells it what to execute next. See
`context-map.md`.

## Read Order

1. `context-map.md`
2. `new-chat-bootstrap.md`
3. `conversation-trail.md`
4. `rama-world-kernel-text-instance.md`
5. `architecture/action-request-kernel-routing.md`
6. `architecture/logical-lifecycle-and-derived-depots.md`
7. `architecture/rama-world-kernel-v0-pr-trail.md`
8. `architecture/rama-policy-throughput-post.md`
9. `architecture/rama-blog-patterns.md`
10. `architecture/prompt-to-implementation-lossiness.md`

Read `implementation-review-prompt.md` only when the user is ready to review or
change code. Do not treat it as the default bootstrap for open-ended discussion.

## Active Explorations

- `architecture/policy-granularity-mastodon-parallel.md` — open question on
  fine-grained policy behavior without per-unit permission materialization.

## Current Center

The system is one loop:

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

## Current Kernel Shape

The general kernel is still the outer shared structure. It was not replaced by
`ActionRequest`. The correction was to add lifecycle around the event contract:

```text
ActionRequest asks.
ActionDecision records the answer.
KernelEvent happened.
```

The event contract remains the accepted fact contract:

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

Text-specific, PDF-specific, chat-specific, and code-specific details plug into `payload` and `target/address`. They should not change the kernel envelope.

The same shared contracts appear in the request and accepted event:

```text
actor
branch
context
target
action
payload
causality
policy/provenance
```

What changes is timing and ownership:

```text
ActionRequest is proposed by the edge/helper.
ActionDecision is recorded by Rama.
KernelEvent is derived by Rama only after acceptance.
```

## Key Correction

Text is not the ontology. Text is the first carrier we use to exercise the general kernel.

The kernel is:

```text
event contract
action request contract
action decision contract
target contract
action contract
materialization contract
projection contract
policy contract
distillation contract
```

`ActionRequest` is not a text/PDF/chat/code instance. It is a lifecycle envelope
inside the general kernel. Text/PDF/chat/code are carriers.

Text plugs into this kernel as one instance:

```text
text artifact
text revision
text address/anchor
text-derived unit
unit status in a branch
text/editor/unit projections
```

## Working Rule

Do not collapse planes into fake options.

The right separations are:

```text
Plane = system-level concern / contract
Instance = concrete carrier, e.g. text/chat/PDF/code
Module = implementation boundary
Walkthrough = one vertical path through all contracts
```

## Current Implementation State

V0 and V1 prove the request-first loop in code. The global context does not
prescribe the next implementation direction.

Open design pressure to preserve:

```text
physical routing/locality still needs clearer Rama shape
policy state is not yet a full Rama-owned authorization model
future depot splitting remains a systems question
editor/write batching remains a systems question
```

Do not make every physical UI gesture a world action. For typing, follow the
collaborative editor pattern: local buffer, versioned edit operation or edit
batch, artifact/document-keyed depot, and Rama-side transform/decision. See
`architecture/rama-blog-patterns.md`.

V0 proof covered:

```text
1. Ingest one text artifact.
2. Materialize artifact/text state.
3. Unitize into lines or paragraphs.
4. Accept/reject one unit in a branch.
5. Query canonical and discarded views separately.
6. Preserve provenance back to raw artifact/event.
```
