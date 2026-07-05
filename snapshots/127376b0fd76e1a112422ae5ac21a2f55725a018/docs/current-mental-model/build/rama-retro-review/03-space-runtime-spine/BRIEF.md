# Brief - Space Runtime Spine

## Review Block

This block reviews the committed Space runtime spine, formerly named World in
older commits:

```text
space action
  -> Rama decision
  -> turns / context bundles / catalog objects / projections
  -> LLM run requests or controls when appropriate
  -> LLM observations back into Space as proposals, patches, slices, overlays,
     derivatives, and projection updates
```

## Commit Anchors

```text
3ebb272 2026-05-10 world: add chat turns and context bundles
2cae951 2026-05-10 llm: bridge compose send through world
bc56b16 2026-05-10 llm: add world-first run controls
3ea1855 2026-05-10 world: materialize catalog and raw item indexes
6de66b0 2026-05-10 world: add slices overlays and derivatives
c812646 2026-05-10 world: add LLM fork flows
584f8c5 2026-05-10 world: ingest LLM patch proposals
340a173 2026-05-11 world: add rebuildable projections
1ef1cbd 2026-05-13 rama: split text kernel and rename space
c0dfafe 2026-05-13 docs: update rama space rename context
```

## Code Files

```text
src/app/server/rama/dogfood/space.clj
src/app/server/rama/dogfood/llm.clj
```

`llm.clj` is included here only where Space creates or consumes LLM run/control
records as part of the Space-first contract.

## Test Files

```text
test/app/server/rama/dogfood_space_test.clj
test/app/server/rama/dogfood_llm_test.clj
```

## Architecture Docs

```text
docs/current-mental-model/architecture/dogfood-runtime/README.md
docs/current-mental-model/architecture/dogfood-runtime/three-depot-current-system.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-derived-contract.md
docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md
```

## Main Review Question

Does Space remain the user-facing truth layer, while LLM and compute tracks
produce observations, controls, or proposals rather than secretly mutating Space
state?

## Specific Questions

- Are compose/send, approval, cancel, compaction, fork, and patch-accept actions
  Space-first?
- Is each accepted Space fact materialized into durable PStates before
  downstream agent execution depends on it?
- Are context bundles immutable once a run is created?
- Are idempotency keys preventing duplicate turns, bundles, and run requests?
- Are LLM observations transformed into proposals or patches without being
  accepted as user-authored truth too early?
- Are projection PStates rebuildable from canonical Space and LLM inputs?
- Are catalog and relation indexes denormalized for actual read questions?
- Are old World names fully transitional, or do they still leak into runtime
  contracts?

## Out Of Scope

- Standalone LLM executor behavior, except where it is required to validate the
  Space-first contract.
- Object-container Slice 1.
- In-process chat ingester work.
