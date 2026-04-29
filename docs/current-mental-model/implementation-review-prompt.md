# Implementation Review Prompt

Status: next implementation prompt, 2026-04-29.

Use this to start the next implementation/review chat.

```text
We are working in /mnt/data/projects/Softland.

Branch protocol:

- The current mental-model docs live on local branch
  docs/current-mental-model-local.
- This branch is private/local. Do not push it.
- Do not merge this private branch into main.
- Work may happen directly on this private branch, or on a private branch made
  from it, because commits are not public until pushed.
- Build and test may run on the private branch.
- Keep docs changes and code/test changes in separate commits.
- If docs need to change during implementation, update and commit docs locally.
- If code needs to change, commit only explicit code/test paths, for example
  `git add src test`; never use `git add -A`.
- Never use `git reset --hard`, `git clean`, or path-wide `git restore` to make
  room for implementation unless the user explicitly asks for it.

When implementation is ready to share, export code only:

```bash
git switch main
git switch -c impl/rama-world-kernel-v1-public
git cherry-pick <code-commit-1> <code-commit-2>
```

The public branch receives only selected code/test commits. The private docs
commits stay local forever.

Before changing code, read:

1. docs/current-mental-model/README.md
2. docs/current-mental-model/rama-world-kernel-text-instance.md
3. docs/current-mental-model/architecture/action-request-kernel-routing.md
4. docs/current-mental-model/architecture/rama-world-kernel-v0-pr-trail.md
5. docs/current-mental-model/architecture/rama-policy-throughput-post.md
6. docs/current-mental-model/architecture/rama-blog-patterns.md
7. docs/current-mental-model/architecture/prompt-to-implementation-lossiness.md
8. docs/architecture/think-in-rama.md
9. .agents/skills/think-in-rama/SKILL.md

Preflight before editing:

1. Restate the first physical record Rama sees.
2. Restate the accepted ActionDecision fields.
3. Restate the rejected ActionDecision fields.
4. State whether ActionRequest may contain any event id, and under what explicit name.
5. State whether common request validation happens before dispatch for every request.
6. State whether :routing/key is the semantic identity key or exact physical locality key.
7. Name the PState reads/writes expected to be local after depot routing.
8. Name the tests that would fail if this interpretation is wrong.

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

V1 acceptance criteria:

- Accepted ActionDecision includes :decision/id, :decision/status, :request/id,
  :request/type, :routing/key, and :event/id.
- Rejected ActionDecision includes :decision/id, :decision/status, :request/id,
  :request/type, :routing/key, :event/id nil, :decision/reason, and :errors.
- Every request passes common request validation before action-specific logic.
- Malformed unknown actions fail request validation before unknown-action dispatch.
- Normal request helper APIs do not accept ambiguous :event-id; deterministic
  tests must use explicit :proposed-event-id or :proposed/event-id if needed.
- :routing/key is either the exact physical locality key for required PState
  reads/writes, or the implementation documents the remaining re-hash as
  transitional.

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
