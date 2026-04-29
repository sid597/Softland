# Prompt To Implementation Lossiness

Status: process guardrail, 2026-04-29.

This note exists because the V0 -> V1 Rama kernel pass exposed a real loss of
fidelity:

```text
conversation
  -> docs/prompt
  -> implementation
  -> review
```

Each step preserved the headline, but lost some operational meaning.

## What Was Lost

The conversation had a richer model:

```text
ActionRequest is the first Rama record.
ActionDecision is the durable answer.
KernelEvent exists only after accept.
Routing is locality, not approval.
Traceability should be visible in each durable record.
```

The implementation prompt compressed that into:

```text
add :routing/key
remove event-id from request payload
prevent request/action type drift
keep things traceable
```

That was directionally correct, but under-specified.

## Where The Drift Happened

```text
"traceable"
  became "can read the request separately"
  instead of "decision carries the important trail fields itself"

"route/hash by affected entity"
  became "hash by a semantic vector"
  instead of "prove whether the stream starts on the same locality as the needed PStates"

"event id only after accepted decision"
  became "event id moved out of payload"
  instead of "normal request helper APIs stop naming it :event-id"

"request-first pipeline"
  became "known action interpreters validate requests"
  instead of "all requests validate before dispatch, including unknown actions"
```

The failure was not lack of prose. It was lack of round-trip checks.

## New Rule

Before implementation, the agent must restate the contract in operational terms
and name the tests that would fail if it misunderstood the model.

```text
Do not begin editing from a vibe-complete prompt.
Make interpretation observable first.
```

## Preflight Template

Before editing, answer:

```text
1. What is the first physical record Rama sees?
2. What fields must ActionDecision carry on accept?
3. What fields must ActionDecision carry on reject?
4. Can ActionRequest contain any event id? If yes, under what explicit name and why?
5. Does every request validate before dispatch, including unknown actions?
6. Is :routing/key the semantic identity key or the exact physical locality key?
7. Which PState reads/writes should be local after depot routing?
8. What tests will fail if this interpretation is wrong?
```

## Acceptance Criteria Template

For every Rama implementation prompt, include explicit pass/fail criteria:

```text
Accepted ActionDecision must include:
  :decision/id
  :decision/status :accepted
  :request/id
  :request/type
  :routing/key
  :event/id

Rejected ActionDecision must include:
  :decision/id
  :decision/status :rejected
  :request/id
  :request/type
  :routing/key
  :event/id nil
  :decision/reason
  :errors

Every request must pass common request validation before action-specific logic.

Unknown actions are still requests, so malformed unknown requests fail request
validation before they fail unknown-action dispatch.

Normal request helper APIs must not accept ambiguous :event-id. If deterministic
tests need an id, use an explicit :proposed-event-id or :proposed/event-id and
document why it is not accepted world fact.

:routing/key must either be the exact physical locality key used for required
PState reads/writes, or the implementation must document the remaining re-hash
as transitional.
```

## When To Promote To A Skill

Do not create a new skill yet.

This learning is currently Rama-specific enough to live in:

```text
docs/current-mental-model/implementation-review-prompt.md
.agents/skills/think-in-rama/SKILL.md
```

Create a broader implementation-translation skill only if this same failure
repeats outside Rama work.
