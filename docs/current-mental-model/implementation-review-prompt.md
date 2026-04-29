# Implementation Review Prompt

Status: next implementation prompt, 2026-04-29.

Use this to start the next implementation/review chat.

```text
We are working in /mnt/data/projects/Softland.

Before changing code, read:

1. docs/current-mental-model/README.md
2. docs/current-mental-model/rama-world-kernel-text-instance.md
3. docs/current-mental-model/architecture/action-request-kernel-routing.md
4. docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md
5. docs/current-mental-model/architecture/rama-policy-throughput-post.md
6. docs/current-mental-model/architecture/rama-blog-patterns.md
7. docs/architecture/think-in-rama.md
8. .agents/skills/think-in-rama/SKILL.md

Review the current implementation against the settled model:

ActionRequest asks.
ActionDecision records Rama's answer.
KernelEvent happened only if accepted.

Important constraints:

- The event contract remains the accepted fact contract.
- ActionRequest is a lifecycle envelope, not a text/PDF/chat/code instance.
- :action is the expressive operation object and must stay.
- :request/type may exist for dispatch/backcompat, but must not drift from :action/type.
- No top-level :event/id belongs on ActionRequest unless we explicitly add :proposed/event-id.
- Routing is not approval. Routing finds the state locality needed for Rama to decide.
- Authenticate at the edge; authorize/validate world truth inside Rama from PStates.
- Do not add Rama I/O just because it makes code simpler.
- Do not send one Rama write per keystroke; follow the collaborative editor pattern of local buffer, semantic edit object/batch, document-keyed depot, and local transform.

Review questions:

1. Does src/app/server/rama/core.clj still hide event ids in request payloads?
2. Should V1 add :routing/key to ActionRequest and hash the request depot by it?
3. Is :request/type redundant with :action/type, and if kept, how is drift prevented?
4. Where should ActionDecision be stored on hot paths without adding unnecessary I/O?
5. Which policy PStates are needed first for unit/status-set?
6. Does unit/status-set route by artifact, unit, branch, or a compound key?
7. Which code paths still append compatibility requests that should become real world actions?
8. What is the smallest next patch that improves Rama locality without expanding scope?
9. If implementing text edits, what is the edit object/batch shape and where is client buffering acknowledged?
10. Which reads should become query topologies to avoid client-side loops/roundtrips?

Output:

- Current code shape.
- Mismatches against the settled model.
- Proposed V1 contract changes.
- Smallest implementation plan.
- Tests to prove accept and reject paths.

Then implement the smallest safe patch.
```
