# LLM Track Contract Slice Roadmap

Status: implementation roadmap derived from `llm-track-derived-contract.md`,
2026-05-10. Slice-roadmap depth (not exhaustive schema spec). Vertical
green slices; each commit is independently green.

## Summary

Use slice-roadmap planning for the full remaining LLM contract and commit
green vertical slices as we go. The roadmap keeps slice size sane, moves
idempotency to the first bridge point where double-send matters, adds the
writer-asymmetry property test from slice 1, and explicitly defers
non-MVP contract surface.

Implementation order:

```text
LLM spine
  → World chat/bundles
    → World-first bridge
      → Controls
        → Executor
          → Follow-ups
            → Catalog/raw material
              → Slices/derivatives
                → Fork/reconcile
                  → Patch proposals
                    → Projections
                      → Hardening
```

## Slice And Commit Plan

### 1. `llm: add dogfood LLM run lifecycle`

- Start from the existing untracked `src/app/server/rama/dogfood/llm.clj`
  and `test/app/server/rama/dogfood_llm_test.clj`.
- Add `pstate-writer-asymmetry-property-test` against the LLM spine:
  external helpers append depots only; all PState changes are
  topology-derived and replayable from depot inputs.
- Commit only the LLM namespace and LLM tests after the focused and
  combined Rama suites pass.

### 2. `world: add chat turns and context bundles`

- Add world-side request handling for `:world-thread/create`,
  `:world-turn/compose-and-send`, and minimal world-only turn records.
- Add World PStates for threads, turns, turn ordering, context bundles,
  and bundle-by-turn lookup.
- Freeze ContextBundles inside WorldTopology with bundle-owned
  execution options and rendered model input.

### 3. `llm: bridge compose send through world`

- Make `:world-turn/compose-and-send` accept into sibling facts:
  WorldTurn created, ContextBundle frozen, LLM run requested.
- WorldTopology foreign-appends the derived `:llm/turn-run-request`;
  UI/helper paths never append user sends directly to `*llm-depot`.
- Add idempotency-key dedup here: replaying the same world send
  returns the same decision/result shape and does not mint duplicate
  WorldTurns, bundles, or runs.

### 4. `llm: add world-first run controls`

- Add `*llm-control-depot` and control state for approval resolve,
  cancel, compact, and steer.
- User control decisions enter through world actions first, then
  derive LLM control records.
- Add approval timeout behavior: unresolved approvals are durably
  declined/expired according to the contract timeout policy.

### 5. `llm: run Codex executor from Rama claims`

- Add executor boundary: pending-run reader, claim appender,
  durable-grant wait, bundle loader, Codex adapter, observation
  appender.
- Use a fake Codex adapter in tests and the same interface for the
  real app-server adapter.
- Add stale-on-crash behavior: if executor/Codex dies with unresolved
  approvals, mark approvals stale and fail or restart the run
  according to the recorded run policy.

### 6. `llm: support follow-up runs on bound threads`

- Record native Codex thread ids from observations onto LLMThread
  state.
- Follow-up sends create a new LLMTurnRun in the same LLMThread;
  never append a second turn to an old run.
- Preserve native thread binding before follow-up turn/start.

### 7. `world: materialize catalog and raw item indexes`

- Add catalog plumbing: `$$objects`, `$$artifact-graph`, and
  `$$artifact-graph-in`.
- Add eager object rows for WorldThread, WorldTurn, ContextBundle,
  and LLMTurnRun summary.
- Complete raw LLM item indexes, including `$$llm-item-by-id`,
  without adding slice/overlay/derivative behavior yet.

### 8. `world: add slices, overlays, derivatives`

- Add request handling for slice, overlay/comment, and derivative
  creation.
- Slice stores snapshot text/hash and source content hash; raw LLM
  items remain immutable.
- Lazily promote raw items into the object catalog when cited,
  sliced, commented, derived, or otherwise made public material.

### 9. `world: add LLM fork and reconciliation flows`

- Fork-from-span creates or reuses a slice snapshot, creates a child
  WorldThread, freezes a child ContextBundle, and uses Codex
  thread-level fork semantics only.
- Reconciliation creates a synthesis WorldThread with parent graph
  edges and a fresh native LLMThread.
- Executor must not send child turn/start until fork-completed
  binding is durable.

### 10. `world: ingest LLM patch proposals`

- LLM observations that produce patch-like TurnDiffs become pending
  world proposals.
- Add patch accept/reject WorldTurns.
- Keep tool approval separate from patch acceptance.

### 11. `world: rebuildable projections`

- Add rebuildable projection PStates for chat canvas, run detail,
  object detail, and discourse graph-shaped object relations.
- Projection state is derived only from canonical world/LLM PStates
  and depot history.

### 12. `harden: cost rollups and remaining property tests`

- Add per-thread cost rollups from token usage observations.
- Re-run/extend writer-asymmetry property tests across world, LLM,
  control, catalog, and projection PStates.
- Add final projection rebuildability and replay consistency
  coverage.

## Required Tests

```text
SLICE   TESTS
─────   ────────────────────────────────────────────────────────────
1       existing LLM lifecycle tests
        + pstate-writer-asymmetry-property-test

3       world-first-send-test
        context-bundle-before-run-test
        one-bundle-per-run-test
        accepted-decision-fields-test
        idempotency-test

4       approval-world-first-test
        cancel-world-first-test
        compaction-world-first-test
        approval-timeout-test
        approval-is-not-patch-acceptance-test (weak: no patch-applied
                                               row created; strengthen
                                               at slice 10)

5       fake executor claim/stream tests
        stale-approval-on-executor-death-test
        run-failed-or-restart-policy-test

6       follow-up-new-run-same-thread-test

8       slice-raw-immutability-test
        slice-source-hash-test
        derivative-renders-as-user-authored-test
        world-only-turn-no-llm-side-effect-test

9       fork-span-softland-anchor-test
        fork-binding-before-turn-start-test

10      patch-proposal-creation-test
        patch-accept-test
        patch-reject-test
        approval-is-not-patch-acceptance-test (strengthened: approve
                                               apply_patch does NOT
                                               create patch-applied
                                               row in world)

11-12   projection-rebuildability-test
        cost-cache-fields-test
        expanded replay/property tests
```

Each slice must pass its focused tests plus the combined Rama suite
(`world-kernel-test`, `dogfood-compute-test`, `dogfood-llm-test`)
before commit.

## Deferred To V1.x

These are intentional green-MVP gaps, not forgotten contract surface.
Each is named in `llm-track-derived-contract.md` or
`llm-track-canonical.md` and remains part of the architecture; the
roadmap simply does not implement them in V0:

- Reverse-MCP / custom tools (agent reads world state via Softland-MCP
  server).
- Full discourse graph typed projection beyond rebuildable object
  relation projection.
- Multi-agent LLMThread variants and model-comparison lanes.
- Canvas / semantic zoom UI integration.
- Full UI workflows for slice editing, projection navigation, and
  zoom-level collaboration.

## Commit Discipline

- Commit after each green vertical slice.
- Stage exact code/test paths only; never use `git add -A`.
- Do not stage `.md`, `.gitignore`, `.agents/skills/...`, `scripts/`,
  `src-build/build/`, unrelated tests, or fixtures unless explicitly
  requested.
- Current first commit should stage only the LLM spine code/test
  files plus the added writer-asymmetry test work.
- Keep work on the private `docs/current-mental-model-local` branch
  unless later exporting selected code/test commits to a public
  branch.

Per Sid's workflow: docs (`.md`) commit on this branch only; main
gets cherry-picked code/test commits. The roadmap's slices are all
code/test, so each slice is cherry-pick-eligible to main.

## Assumptions

- The plan is a slice roadmap, not a full up-front schema spec.
- Commits are green vertical slices.
- Idempotency dedup belongs in the world-first bridge slice (slice 3),
  because that is where duplicate user sends first become dangerous.
- Approval timeout lands with control (slice 4); stale-on-crash lands
  with executor (slice 5).
- Deferred V1.x items stay visible but do not block the green MVP.
- The pstate-writer-asymmetry property test landed at slice 1 will be
  re-run / extended on every later slice that adds new PStates.
- Each slice's tests are additive; later slices do not break earlier
  ones.

## Companion docs

- `llm-track-derived-contract.md` — the contract this roadmap
  implements; invariants and tests come from there.
- `llm-track-canonical.md` — full architectural rationale + question
  index.
- `llm-track-v2.md` — focused architecture diagrams.
- `slice-a-compute-run-command.md` — sibling implementation pattern
  (4-tier asymmetry, claim/grant, back-arrow rule).

## Lineage

- Initial draft: 9-slice plan.
- Adjusted (post-review): split slice 7 → 7+8 (catalog vs slices),
  split slice 9 → 10+11+12 (proposals vs projections vs hardening),
  moved idempotency from slice 9 to slice 3, added
  pstate-writer-asymmetry-property-test at slice 1, made approval
  timeout (slice 4) and stale-on-crash (slice 5) explicit, added
  Deferred-To-V1.x section.
