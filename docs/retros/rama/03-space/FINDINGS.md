# Findings — Space Track (inline consolidation)

Source: `IMPLEMENTATION_VALIDATION.md` (R4, **major-fail**) — full line-cited runtime traces live there; this file is the actionable index. Test validation (R6) was not run for this track; test gaps fold into the fix session. The blind PLAN.md in this folder is the reference design for fixes.

## HIGH

- **SP-01 Compose retry permanently loses facts.** Retry after the mid-tree `$$send-by-idempotency` write (space.clj:1324-5) takes the decision-only branch → catalog writes, relation edges, and the LLM dispatch are **permanently lost** while the send looks successful.
- **SP-02 No journal/dedup on request folds.** Create retry flips decision/event to `:space/touched`; fork retry re-appends the dispatch; LLM-intake resets run rows unconditionally (llm.clj:1329) → **duplicate model runs**; compact/steer redelivery duplicates history; proposal-create redelivery resets resolved proposals to `:pending`.
- **SP-03 Idempotency check/write split by ~10 hops** → two concurrent same-key sends both pass the check and double-mint full fact families.
- **SP-04 Turn-order read→hop→write race** (read at :1491, write-back at :1512) → concurrent turns permanently drop a turn from the order.

## MEDIUM

- **SP-05** `append-llm-observation!` performs two foreign appends; crash between them → observation durable LLM-side but the proposal never reaches Space (Space sources no observation depot at all).
- **SP-06** Patch resolution unconditional: phantom proposal rows under nil/unknown ids; a retried accept overwrites a later reject (first-resolution-wins violated).
- **SP-07** Ingest mints a decision + turn (spec: neither); executor-streamed proposals never materialize; unresolvable space-id silently drops the proposal (never-drop violated).
- **SP-08** Replay snapshot equality fails: created-vs-touched interleaving dependence; chat canvas wiped to `[]` on duplicate create.
- **SP-09** Unbounded non-subindexed collections on the hot send path: `$$turns-by-space`, chat-canvas turn order, `$$space-graph`, `$$artifact-graph(-in)`, relations projections — wholesale rewrites per send.

## LOW

- **SP-10** Hygiene cluster: redundant decision-write trio in all 6 branches; ~19 consecutive-keypath sites; select-compute-transform on full rows; `select-pstate-one` reimplements `foreign-select-one`.

## What held up

Bundle-freeze determinism under replay, the fork-binding durability gate (traced through llm.clj), space-only turns having no LLM side effects, always-readable read surfaces.

## Fix direction

(1) Atomic journal-entry-first fold (PLAN.md Segment-1 pattern) closing SP-01/02/03/04, (2) observation-depot sourcing + proposal lifecycle guards (SP-05/06/07), (3) replay-deterministic decisions (SP-08), (4) schemas/subindexing (SP-09), (5) hygiene. Resume skill Phase 3 against PLAN.md; re-run Phase 4 (+ add Phase 6) after changes.
