# Brief - Transcript Capture

## Review Block

This block reviews only the committed passive transcript capture slice. It does
not include later in-process transcript/object-container redesign work.

Committed shape:

```text
transcript harvest/watch request
  -> transcript run state
  -> claim/status
  -> source JSONL observations
  -> ledger, observed conversations, and tool-call index PStates
```

## Commit Anchors

```text
67be839 2026-05-11 llm: add Claude executor and transcript capture
59be788 2026-05-11 Add Claude LLM and transcript capture docs
```

## Code Files

```text
src/app/server/rama/dogfood/transcript.clj
```

## Test Files

```text
test/app/server/rama/dogfood_transcript_test.clj
```

## Architecture Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/transcript-capture.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-claude-research.md
docs/current-mental-model/architecture/dogfood-runtime/README.md
```

## Main Review Question

Does the committed transcript capture implementation preserve acquisition,
offset, redaction, observation, and audit behavior without confusing passive
source observation with accepted Space or LLM truth?

## Specific Questions

- Is transcript capture observation-only?
- Are source files read without spawning or controlling Claude/Codex?
- Are file ids, byte offsets, and line hashes sufficient for idempotency?
- Does watch resume from durable ledger state?
- Are partial lines withheld until complete?
- Are malformed lines recorded without killing the harvest/watch?
- Is redaction applied before persisted preview/content fields?
- Are large file scans streaming and bounded enough for task-thread safety?
- Are indexes shaped around actual reads, not around a future redesign?

## Out Of Scope

- `build/chat-ingester/`.
- `build/transcript-object-ingest/`.
- Any object-container transcript redesign.
- New transcript ingest implementation.
