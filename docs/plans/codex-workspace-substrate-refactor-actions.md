# Codex Workspace-Substrate Refactor Actions

> Status: execution companion for the workspace-first refactor program
> Canonical spec: `docs/plans/codex-workspace-substrate-refactor-program.md`
> Program map: `docs/plans/codex-refactor-program-map.md`

---

## Active Phase

**Phase 1 — Safe-Now Persistence + Action Boundary**

Phase 0 is complete. The ownership freeze is locked at `docs/architecture/phase-0-ownership-freeze.md`.

Next steps (can run in parallel):

- **Phase 1A**: Settings Rama slice (safe-now persistence, 0.5 session)
- **Phase 1B**: Agent trail Rama slice (safe-now persistence, 1-2 sessions)
- **Phase 1C**: Workspace action boundary (semantic dispatch, 1 session)

## Phase 0 — COMPLETE (2026-03-23)

All 49 runtime atoms inventoried and classified. 6 ambiguities identified and resolved:
- A1: `!current-file` vs `!sidebar-truth :selected-file` → transitional, replaced in Phase 2
- A2: `!settings` mixes truth + UI → separate in Phase 1A
- A3: `!active-font` should be derived → fix derivation direction
- A4: `!agent-output` conflates trail + streaming → separate in Phase 1B
- A5: `!flow-state :selected` is ephemeral in a truth atom → extract
- A6: `!active-pane` vs `!focus` → correctly separate concerns, no change needed

---

## Ready Now

### Phase 1A: Settings Rama Slice
- Declare `$$user-settings-pstate` in `core.clj`
- Topology case: `:settings/update`
- Server atom mirror + Electric bridge (follows sidebar pattern exactly)
- Separate `!settings` into truth fields + UI-local fields
- Fix `!active-font` derivation direction (settings → active-font, not reverse)

### Phase 1B: Agent Trail Rama Slice
- Declare `$$agent-trails-pstate` in `core.clj` (keyed by run-id)
- Topology cases: `:agent-trail/append-block`, `:agent-trail/complete-run`
- Separate `!agent-output` into trail truth + streaming ephemeral
- Batch trail events in SSE handler (per completed block, not per token)

### Phase 1C: Workspace Action Boundary
- Define initial action vocabulary: `select-project`, `select-artifact`, `set-active-pane`, `enter-workflow`, `exit-workflow`, `toggle-sidebar`, `mutate-split`
- Route semantic transitions through explicit handlers
- `mouse.cljs` semantic clicks dispatch actions, not direct atom swaps
- `keyboard.cljs` semantic keys dispatch actions

---

## Blocked By

Nothing external blocks Phase 0.

Later phases are blocked by:

- unresolved ownership ambiguity
- unclear artifact-selection model
- unclear split semantics
- uncertainty about which current fields are durability-worthy
- lack of measured evidence for the editor durability strategy

---

## Watch-Fors

- do not reopen the sidebar slice as if it were the main unfinished thread
- do not redesign the UI while fixing substrate semantics
- do not move fields into Rama before their meaning is stable
- do not treat "editor must stay local until save" as a decided fact before testing
- do not introduce a second hidden truth store under the name of "overlay"
- do not let workflow mode and file mode keep separate substrate assumptions

---

## Phase Board

## Phase 0 — Freeze the truth model — ✓ COMPLETE

**Completed**: 2026-03-23, Session 43
**Output**: `docs/architecture/phase-0-ownership-freeze.md`

All 49 atoms classified. 6 ambiguities resolved. Intended field sets defined for workspace-truth, workspace-overlay, workspace-ui. Safe-now vs workspace-schema persistence boundaries locked.

---

## Early Parallel Lane — Safe-Now Persistence

### Objective

Take the lowest-risk persistence wins without hardening the workspace ontology prematurely.

### Concrete outputs

- settings persistence slice
- agent trail / agent-output persistence slice
- explicit statement of what is **not** safe to persist yet

### Implementation focus

- settings bridge/wiring
- trail/run persistence bridge/wiring
- Electric handoff for those slices

### Risks

- allowing the safe-now lane to expand into workspace-schema persistence too early
- confusing "some Rama progress" with substrate completion

### Exit criteria

- settings persist and reload cleanly
- agent trail structure persists and reloads cleanly
- selected artifact, split, local-world, and editor truth are still clearly deferred until their schema is stabilized

---

## Phase 1 — Workspace action/proposal boundary

### Objective

Name semantic transitions explicitly and stop letting surfaces own them by accident.

### Concrete outputs

- a workspace action vocabulary
- proposal handlers or equivalent routing points
- explicit separation between semantic actions and local-only mutations
- an initial editor event/proposal vocabulary, without locking the durability strategy yet

### Implementation focus

- `src/app/client/workspace/runtime/mouse.cljs`
- `src/app/client/workspace/runtime/keyboard.cljs`
- `src/app/client/workspace/runtime.cljs`

### Risks

- leaving "just one more direct mutation" in place for convenience
- over-abstracting before the action vocabulary is small and concrete

### Exit criteria

- every semantic workspace transition can be named as one action
- input modules no longer directly own committed workspace meaning in an ad hoc way

---

## Phase 2 — Selected artifact and load ownership

### Objective

Turn selection and loading into one coherent substrate path.

### Concrete outputs

- `artifact-ref` as the substrate selection unit
- a stable distinction between selected artifact, loaded content, and visible pane fill
- centralized artifact load ownership
- deduped in-flight artifact requests
- measured evidence for the editor commitment path before a batching/hybrid policy is chosen

### Implementation focus

- `src/app/client/workspace/runtime/sidebar_io.cljs`
- `src/app/client/workspace/runtime.cljs`
- any current `current-file` / selected-file bridging

### Risks

- preserving file-centric assumptions under a new name
- leaving workflow/trail selection outside the model
- stale responses still mutating visible state late
- deciding the editor durability model from fear instead of measurement

### Exit criteria

- one selection event leads to one artifact transition
- one artifact has one load owner
- repeated fast selection does not fork state
- direct committed editor behavior has been tested before any fallback model is locked

---

## Phase 3 — Derive the local world

### Objective

Create the single semantic object that explains the visible workspace.

### Concrete outputs

- `effective-local-world`
- derivation from truth + overlay + UI/cache + artifact content state
- pane builders consuming the local world

### Implementation focus

- `src/app/client/workspace/runtime.cljs`
- `src/app/client/workspace/runtime/render.cljs`
- shell/workflow composition entry points

### Risks

- creating an oversized god object rather than a useful semantic root
- leaving major pane semantics outside the local world

### Exit criteria

- the question "what world is active?" has one answer in code
- pane builders no longer reconstruct meaning from scattered atoms

---

## Phase 4 — Split and pane substrate

### Objective

Make panes and splits semantic rather than incidental.

### Concrete outputs

- split descriptor model
- pane descriptor model
- shell derived from local world + split
- initial semantic pane roles

### Implementation focus

- `src/app/client/workspace/shell.cljs`
- `src/app/client/workspace/runtime/render.cljs`
- local-world to shell composition boundary

### Risks

- making split too geometric
- collapsing pane roles back into mode-specific shell logic

### Exit criteria

- pane identity is explicit
- pane content is a semantic fill
- split meaning is represented in code

---

## Phase 5 — Workflow and review workspace migration

### Objective

Mount workflow surfaces into the same substrate as the rest of the workspace.

### Concrete outputs

- workflow artifact or workflow-context semantics inside local world
- DG flow mounted as a substrate resident
- initial review-workspace shape defined on the same model

### Implementation focus

- `src/app/client/workflows/dg_flow.cljs`
- `src/app/client/workspace/shell.cljs`
- workspace/runtime integration points

### Risks

- keeping a hidden "workflow mode" universe beside the substrate
- only wrapping the old behavior cosmetically

### Exit criteria

- workflow entry is modeled as a local-world transition
- workflow surfaces no longer require a parallel workspace ontology

---

## Phase 6 — Render and reactive invalidation narrowing

### Objective

Use the new semantic boundaries to narrow work.

### Concrete outputs

- smaller derivation branches
- more stable keyed scene/update paths
- a validated differential proof on a small surface
- an explicit text differential decision
- a dirty-present strategy
- reduced broad text/rect/shadow churn

### Implementation focus

- `src/app/client/workspace/runtime/render.cljs`
- scene derivation boundaries
- reactive branches in workspace compute code
- sidebar differential proof path first
- editor rect path second
- text differential design third

### Risks

- tuning symptoms while semantic boundaries are still leaky
- introducing brittle micro-optimizations instead of structural narrowing

### Exit criteria

- hover/scroll/local state invalidates only the branches it should
- semantic transitions have predictable render/update scopes
- the differential proof has validated whether keyed mounts are worth widening

### Sub-steps

#### 6A — Sidebar differential proof

- validate keyed mount lifecycle
- validate per-element update cost

#### 6B — Editor rect differential path

- move rect-like editor elements to stable identities

#### 6C — Text differential decision

- choose between per-line/per-block pools and shared-buffer dirty ranges
- base the choice on proof data and implementation cost

#### 6D — Dirty-present

- move toward one-shot RAF on semantic dirtiness
- explicitly account for caret blink and other timer-driven updates

---

## Phase 7 — Rama workspace durability expansion

### Objective

Persist the stabilized workspace semantics.

### Concrete outputs

- workspace truth schema in Rama
- durable re-entry/handoff fields
- preserved separation between durable truth and local-only state

### Implementation focus

- `src/app/server/rama/core.clj`
- `src/app/server/rama/util_fns.cljc`
- Electric boundary for workspace truth

### Risks

- promoting the wrong fields too early
- persisting convenience state instead of handoff-worthy state

### Exit criteria

- re-entry restores a coherent local world
- persistent workspace truth matches the stabilized substrate

---

## Phase 8 — Broader product lanes

### Objective

Resume widening the product on top of a corrected substrate.

### Concrete outputs

- review artifact workspace path
- trail durability widening path
- design-artifact lane reconnection path
- later artifact kinds mapped back to the workspace substrate

### Implementation focus

- whichever lane is chosen next, but now on top of the stabilized workspace model

### Risks

- treating Phase 8 as permission to ignore substrate completion

### Exit criteria

- the next product lane is implemented as a substrate resident, not as another parallel mode system

---

## Current Suggested Order Of Work

If implementation starts now, the execution order should be:

1. Complete Phase 0 ownership inventory and field classification
2. Start the early safe-now persistence lane for settings and agent trails
3. Lock the workspace action vocabulary in Phase 1
4. Unify artifact selection and loading in Phase 2
5. Introduce `effective-local-world` in Phase 3
6. Move shell/pane semantics to split descriptors in Phase 4
7. Re-mount workflow/review surfaces in Phase 5
8. Run the differential proof and widening path in Phase 6
9. Expand Rama durability in Phase 7

This is the sequence that should be followed unless a later discovery proves the dependency graph wrong.

---

## Definition Of Progress

Progress on this program is not:

- more isolated fixes
- more product screens
- more small performance wins
- more persisted fields

Progress is:

- fewer ambiguous owners
- fewer hidden semantic branches
- one clearer local-world model
- one clearer split/pane model
- one clearer durable workspace schema

That is the bar this actions doc is meant to enforce.
