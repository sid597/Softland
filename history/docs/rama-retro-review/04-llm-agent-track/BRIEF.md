# Brief - LLM Agent Track

## Review Block

This block reviews the committed LLM run lifecycle and executor-facing agent
track:

```text
LLM run request
  -> Rama accepted run state
  -> executor claim
  -> Codex or Claude process
  -> observation depot
  -> items, approvals, tool calls, usage, cost, run detail, and views
```

## Commit Anchors

```text
d7baea4 2026-05-10 llm: add dogfood LLM run lifecycle
a330c26 2026-05-10 llm: run Codex executor from Rama claims
ccfa4dc 2026-05-10 llm: support follow-up runs on bound threads
5402128 2026-05-11 harden: add llm cost rollups
287699c 2026-05-11 llm: document cost rollup tradeoff
67be839 2026-05-11 llm: add Claude executor and transcript capture
59be788 2026-05-11 Add Claude LLM and transcript capture docs
5479656 2026-05-11 docs: record LLM contract MVP completion
```

## Code Files

```text
src/app/server/rama/dogfood/llm.clj
src/app/server/rama/dogfood/space.clj
```

`space.clj` is included only where Space creates the run/control input that the
LLM track consumes.

## Test Files

```text
test/app/server/rama/dogfood_llm_test.clj
test/app/server/rama/dogfood_space_test.clj
```

## Architecture Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-v2.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-canonical.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
```

## Main Review Question

Does the LLM agent track treat executor work as module-owned async execution
whose durable lifecycle is owned by Rama, rather than as a process that writes
truth directly?

## Specific Questions

- Is a claim required before any executor starts work?
- Can a stale or wrong-token executor write observations?
- Are observation sequence gaps buffered, rejected, or otherwise handled
  deterministically?
- Are approvals pending and approvals resolved durable native state?
- Are unresolved approvals handled on executor death according to recorded
  policy?
- Are Codex and Claude executor differences represented as backend-specific
  adapter details, not separate ontologies?
- Are raw response items preserved without being rewritten by later projections?
- Are cost rollups correct under duplicate or updated usage observations?
- Are follow-up runs bound to the intended thread without mutating older runs?

## Out Of Scope

- Passive transcript capture, except for commit-bound overlap with Claude docs.
- Space projection correctness beyond what LLM state exposes.
- In-process chat ingester work.
