# Codex Refactor Program Map

> Status: canonical top-level map for the workspace-first refactor program
> Scope: explains what the refactor thread is, how the documents relate, how the work should progress, and how to read the planning stack
> Canonical companion docs:
> - `docs/plans/codex-workspace-substrate-refactor-program.md`
> - `docs/plans/codex-workspace-substrate-refactor-actions.md`

---

## Why This Program Exists

The left-sidebar slice is treated as **done for this thread**.

That does not mean the deeper refactor is done. It means the sidebar already did its job as the first proving slice:
- it forced a real semantic truth boundary into the client/runtime
- it proved `committed truth + optimistic overlay + local ephemeral/cache state` is the right shape
- it exposed that the real unfinished work is no longer "the tree on the left"
- it moved the pressure one level up, into the workspace substrate itself

So this program exists to finish the part that the sidebar slice only revealed:

- what is the actual unit of workspace truth?
- what is selected, loaded, visible, and focused?
- what is a pane, semantically?
- what is a split, semantically?
- what is the local world the user is inhabiting right now?
- how do workflows, files, trails, and previews all become residents of one workspace substrate instead of neighboring systems?

This is the refactor thread.

---

## What Counts As The Refactor Thread

This program is about the substrate that sits above the sidebar slice and below the broader product lanes.

It includes:

- workspace semantic truth
- optimistic workspace overlay
- local ephemeral workspace/UI state
- selected artifact ownership
- file/trail/workflow content ownership
- local-world derivation
- split and pane semantics
- workflow/review surfaces mounting into the same substrate
- render/reactive invalidation narrowing after semantic boundaries are cleaned up
- Rama widening only after the workspace substrate shape is stable

It does **not** primarily include:

- more sidebar rescue work
- a broad visual redesign
- a pure performance-only campaign divorced from state ownership
- immediate design-artifact implementation as the next anchor slice
- "move everything to Rama now" before the schema is correct

The sidebar remains relevant only as precedent and as an already-shipped slice that taught the right ownership pattern.

---

## The Core Strategic Choice

There are two plausible anchors after the sidebar:

1. follow the master architecture literally and make the design-artifact lane the next proving slice
2. follow the pressure in the live code and stabilize the workspace substrate first

This program chooses **workspace substrate first**.

Why:

- the runtime is still atom-heavy in the places that matter most:
  - `selected-file`
  - `current-file`
  - pane ownership
  - workflow/file branching
  - active pane/focus
  - broad world snapshot assembly
- the code already contains a live 3-pane workspace, DG workflow surfaces, trail panes, and file panes
- those things are currently adjacent mechanisms rather than one world model
- if we skip this cleanup and jump straight to richer artifact lanes, the same ownership mistakes will be propagated upward

So this program says:

```text
finish the workspace substrate first,
then widen the ontology,
then widen the runtime,
then widen the product lanes
```

---

## Relationship To Existing Docs

This program does not erase earlier planning artifacts. It sits on top of them and clarifies what is now canonical for the refactor thread.

### Existing docs that still matter

- `docs/plans/refactor-canonical.md`
  - historical and structural
  - captures the earlier `loop.cljs -> workspace runtime` extraction and module move program
- `docs/plans/where-we-are.md`
  - current architecture/status summary
- `docs/thread-map-claude.md`
  - project-wide thread positioning and chronology
- `docs/architecture/softland-master-architecture.md`
  - the long-term architectural law

### What this Codex stack adds

This stack is the missing bridge between:

- the high-level architectural law in the master architecture
- the live state of the runtime after the sidebar slice
- the actual next dependency-ordered refactor program

It is intentionally more operational than the master architecture and more architectural than a session handoff.

---

## Document Stack

This planning stack has three layers.

### 1. `docs/plans/codex-refactor-program-map.md`

This file.

It answers:

- what the refactor thread is
- why it exists
- what the program structure is
- which document does what
- what the current active phase is
- how work should move across the planning docs

This is the **entrypoint**.

### 2. `docs/plans/codex-workspace-substrate-refactor-program.md`

This is the **full canonical refactor plan**.

It answers:

- what is actually done
- what is not done
- what the target architecture is
- what interfaces and state classes should exist
- what the phases are
- what depends on what
- what "done" means

This is the **canonical substance**.

### 3. `docs/plans/codex-workspace-substrate-refactor-actions.md`

This is the **execution companion**.

It answers:

- what phase is active now
- what is ready
- what blocks what
- what the concrete outputs are
- what risks to watch
- how to know a phase is done

This is the **operational tracker**.

---

## Reading Order

If someone is returning to this program fresh, the reading order is:

1. `docs/plans/codex-refactor-program-map.md`
2. `docs/plans/codex-workspace-substrate-refactor-program.md`
3. `docs/plans/codex-workspace-substrate-refactor-actions.md`
4. `docs/sessions/next-prompt.md` only for immediate session state

The important rule is:

- the map tells you how to orient
- the canonical program tells you what is true
- the actions doc tells you what to do next
- `next-prompt` only tells you where the currently active edge is

---

## Program Shape

The program is organized into nine phases.

### Phase 0 — Freeze the truth model
- inventory every relevant runtime atom and classify it
- identify duplicate or ambiguous owners
- lock one intended owner class per concern

### Phase 1 — Create the workspace action/proposal boundary
- define the semantic action vocabulary
- separate semantic actions from ephemeral UI mutations
- stop letting input handlers directly own committed workspace meaning

### Phase 2 — Unify selected artifact and load ownership
- replace file-special-case thinking with artifact selection
- unify selection, loading, and visible pane content
- dedupe requests and stale responses around artifacts

### Phase 3 — Derive the local world
- introduce one derived `effective-local-world`
- make it the semantic root of the visible workspace
- stop reconstructing the workspace from scattered atoms

### Phase 4 — Formalize split and pane semantics
- make panes semantic fills, not ad hoc surfaces
- make split meaning-bearing, not just geometry
- derive shell composition from local-world + split

### Phase 5 — Migrate workflow/review surfaces onto the substrate
- stop treating workflow mode as a separate workspace universe
- mount workflows and review artifacts inside the same substrate

### Phase 6 — Narrow render and reactive invalidation
- use the semantic cleanup to narrow derivations and mounts
- make GPU, scene, and interaction branches more stable and more local

### Phase 7 — Widen Rama to true workspace durability
- move stabilized workspace semantics into durable truth
- restore handoff/re-entry on the correct schema, not a temporary atom layout

### Phase 8 — Reconnect broader product lanes
- review artifact workspace
- trail persistence/lineage widening
- design-artifact and preview lanes
- later zoom-level artifact types

---

## Dependency Topology

This program is not a flat backlog. It has a hard dependency order.

```text
ownership map
-> action/proposal boundary
-> selected-artifact + load ownership
-> effective local world
-> split/pane semantics
-> workflow/review migration
-> render/invalidation narrowing
-> Rama durability widening
-> broader product lanes
```

This order matters because:

- you cannot define the correct split until you know what world is being split
- you cannot narrow invalidation honestly until semantic boundaries exist
- you should not widen Rama durability until the workspace truth schema is stable
- you should not build richer artifact lanes on top of substrate ambiguity

---

## How Work Should Move

This stack is intended to support real ongoing work, not become a frozen design essay.

### When to update the canonical program

Update `docs/plans/codex-workspace-substrate-refactor-program.md` when:

- the phase boundaries change
- the dependency order changes
- a major architectural assumption changes
- a state ownership class changes
- the target interface/model changes
- a phase is discovered to be wrong in shape, not just incomplete

### When to update the actions doc

Update `docs/plans/codex-workspace-substrate-refactor-actions.md` when:

- the active phase changes
- a phase gets partially completed
- blockers change
- ready-now work changes
- risks/watch-fors become more concrete
- a phase exit criterion is satisfied

### When to update the map

Update this file when:

- the structure of the planning stack changes
- the top-level framing of the program changes
- the canonical reading order changes
- the strategic anchor slice changes

This file should change slowly. The actions doc should change most often.

---

## Current Active Phase

Current active phase for the program:

**Phase 0 — Freeze the truth model**

Reason:

- the runtime now has enough structure to see the real duplication and ambiguity
- the next implementation work should not start by coding more branches
- it should start by locking the ownership map and naming the intended substrate

This is the correct first execution phase because the rest of the program depends on it.

---

## What This Program Is Trying To Prevent

Without this refactor program, the likely failure mode is:

- more local fixes
- more product surfaces
- more mode-specific work
- broader render/reactive tuning
- more Rama persistence

all layered onto a workspace that still lacks:

- one canonical semantic world
- one coherent artifact selection model
- one meaning-bearing split model
- one clear separation of truth vs overlay vs ephemeral state

That would produce motion, but not substrate correction.

This program exists to prevent that.

---

## The Short Version

The sidebar slice proved a pattern.

This program applies that pattern to the whole workspace:

```text
committed truth
+ optimistic overlay
+ ephemeral UI/cache state
= effective local world
-> semantic split/pane model
-> one spatial projection
-> narrower mounts and cleaner durability
```

If the program succeeds, the next product lanes will sit on a substrate that finally matches the ontology Softland keeps describing.
