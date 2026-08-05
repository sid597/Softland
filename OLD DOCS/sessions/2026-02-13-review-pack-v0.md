# Session Notes - 2026-02-13 (Review Pack v0 Implementation)

## Implemented in this session

Implemented the first concrete slice of the risk-first plan:

1. Review Pack v0 domain model and validation rules
2. HTTP API endpoints for create/get/publish/summary/feedback
3. Unit tests for pack creation, validation failure, publish + summary behavior
4. Sidebar UI mode for Review Packs (list + select + summary template preview)
5. Thread Canvas v0 UI (node graph + inspector + anchor open action)

This is additive and does not replace existing editor/agent runtime architecture.

---

## Files added/changed

- Added: `src/app/server/review_pack.clj`
- Updated: `src/app/server_jetty.clj`
- Updated: `src/app/client/webgpu/loop.cljs`
- Added tests: `test/app/server/review_pack_test.clj`

---

## API endpoints added

### Create draft pack
`POST /api/review-pack/create`

### List packs
`GET /api/review-pack/list`

### Get one pack
`GET /api/review-pack/:id`

### Publish pack (creates snapshot hash)
`POST /api/review-pack/:id/publish`

### Summary payload + PR template
`GET /api/review-pack/:id/summary?base-url=...`

### Add reviewer feedback
`POST /api/review-pack/:id/feedback`

All responses are EDN.

---

## Validation guarantees implemented

1. Required six sections:
   - `:intent`
   - `:scope`
   - `:change-summary`
   - `:decisions`
   - `:evidence`
   - `:risks-unknowns`
2. Section size budgets enforced.
3. Every changed file must have rationale text.
4. Evidence anchors must be commit-pinned (`:commit` required).
5. Core claims (`:core? true`) must reference valid evidence anchor IDs.

---

## Publish integrity behavior

On publish:

1. Re-validates pack.
2. Computes deterministic SHA-256 snapshot hash from canonicalized pack payload.
3. Marks status `:published` with `:published-at` and `:snapshot-sha`.
4. Returns publish payload including snapshot.

---

## Current limitations (intentional for this slice)

1. Storage is currently in-memory (`defonce` atom); no durable Rama persistence yet.
2. Sidebar UI now includes a Thread Canvas projection, but still no in-UI create/edit/publish actions.
3. No automatic extraction from git diff/session history yet; payload is client-supplied.
4. No rebase stale-anchor detection yet (planned next phase).

---

## Verification performed

1. Syntax parse check for modified `server_jetty.clj`.
2. Reader parse check for modified `loop.cljs` structure (with `#js` stripped for structural verification).
3. Isolated namespace load for `app.server.review-pack`.
4. Behavior smoke checks for create/publish/summary via isolated Clojure eval.
5. Unit tests:
   - `create-review-pack-success`
   - `create-review-pack-validation-errors`
   - `publish-and-summary`

---

## Why this is the right step

This implementation targets the highest-risk part first:
- structured, bounded review artifact quality and validation

without overcommitting to:
- heavy integrations
- broad ontology
- reactive graph UI complexity

So it enables fast pilot validation on reviewer utility and author overhead.
