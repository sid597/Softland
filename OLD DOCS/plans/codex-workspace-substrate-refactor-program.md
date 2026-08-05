# Codex Workspace-Substrate Refactor Program

> Status: canonical refactor plan
> Scope: the full workspace-first refactor program that follows the completed sidebar slice
> Strategic choice: workspace substrate first, then widened durability and broader product lanes

---

## Executive Overview

This document is the canonical specification for the current refactor thread.

The sidebar slice is treated as **done for this thread**. It is not the main unfinished problem anymore.

What the sidebar slice accomplished:

- it forced a real semantic truth boundary into the client/runtime
- it made `committed truth + optimistic overlay + local ephemeral/cache state` concrete
- it exposed where the next pressure lives

What it did **not** solve:

- what the workspace itself is
- what "selected" means at the artifact level
- how pane content and pane identity relate
- what a split means beyond geometry
- how workflows, files, trails, and previews inhabit the same local world
- how render and interaction should consume that world without oversized invalidation

So the refactor thread now moves up one level:

from:

```text
make the sidebar semantically correct
```

to:

```text
make the workspace semantically coherent
```

This program is about finishing that correction.

---

## What Is Genuinely Done

Several important prerequisites are already in place.

### Runtime modularization is real

The runtime has already been split into focused modules instead of one monolithic loop. That matters because:

- ownership is now inspectable
- responsibilities are more separable
- the substrate can be corrected without doing a single giant rewrite

### A shared spatial layer already exists

Rect-tree / scene-tree infrastructure is real and already shared between:

- rendering
- hit-testing
- interaction routing

That means the project already has the beginnings of:

```text
one spatialization
many consumers
```

which is exactly the right long-term law.

### The sidebar slice proved the correct ownership pattern

The sidebar now has:

- committed semantic truth
- optimistic semantic overlay
- local ephemeral/cache state
- one derived effective visible state

This is not just a sidebar pattern. It is the first real proof for the workspace substrate pattern.

### The workspace already contains real surfaces

The runtime is no longer a bare editor:

- file workspace exists
- 3-pane shell exists
- DG workflow surfaces exist
- trails already render as real pane content

This matters because the substrate is no longer abstract. There is enough live structure to stabilize against.

---

## What Is Not Done

This section is intentionally blunt. These are the unfinished substrate problems.

### There is no single canonical workspace object

The visible workspace is still reconstructed from multiple neighboring atoms and branches:

- current file state
- sidebar truth/overlay/ui
- workflow state
- pane focus
- active pane
- various scroll/focus/layout atoms

There is still no one object in code that answers:

```text
what local world is the user in right now?
```

### Selection and content ownership are still fragmented

The runtime still carries traces of multiple overlapping concepts:

- selected file
- current file
- loaded file content
- pane showing something
- workflow state showing something

Those concepts are related, but they are not the same. Right now they are too easy to conflate.

### Pane semantics are still weak

The UI visibly has panes, but the runtime still partially treats panes as:

- hardcoded layout branches
- mode-specific shells
- surface-specific logic

rather than as semantic fills inside one split/local-world model.

### Workflow mode and file mode still feel like adjacent systems

The DG workflow is real, the file workspace is real, and the chat/trail pane is real. But they still sit beside each other more than inside one coherent workspace substrate.

### Render still consumes a broad world snapshot

The render path already has some differential behavior, but it still largely works by assembling a broad snapshot and sampling it on RAF. That means semantic boundaries are not yet the thing narrowing work.

### Too many input paths still directly mutate semantic state

The runtime split made mutation sites easier to see, but not yet clean enough:

- input handlers still write directly to a mix of semantic and ephemeral atoms
- some transitions still happen because a module "owns a surface" rather than because the substrate owns a semantic action

That is exactly the sort of ambiguity this refactor must remove.

---

## What We Learned From The Sidebar Slice

The sidebar slice is important because it changed what we know.

### The problem is not persistence in isolation

The project does not mainly need "more things persisted in Rama." What it needs is the right ownership split:

- what is committed truth
- what is optimistic local intent
- what is ephemeral local UI state
- what is derived visible state

Without that split, persistence only hardens confusion.

### Live truth cannot naïvely overwrite local meaning

The sidebar work showed that live truth synchronization is only correct when:

- truth updates committed meaning
- overlay reconciles pending intent
- ephemeral state stays local
- the visible surface derives from all of the above

That law should now become the workspace law, not just the sidebar law.

### The real pressure moved upward

After sidebar correctness improves, the next failures are no longer "which row is highlighted." They become:

- what artifact is actually selected
- what pane should show what
- whether workflow and file worlds are actually one world
- whether render invalidation follows semantic boundaries

That is why the workspace substrate is the next anchor.

---

## Why Workspace Substrate Comes Before Design-Artifact

The master architecture names a design-artifact slice as a strong proving lane. That is still valuable, but it is not the right next anchor for the live codebase.

Why workspace substrate first:

- the runtime already has enough live surfaces to expose substrate errors
- the next bugs and ambiguities are all in workspace ownership, not lack of a richer artifact type
- design-artifact work would inherit the current ambiguity around selection, panes, and workspace truth
- the sidebar already gave us one real proving slice; now the missing layer is the workspace that all future slices sit inside

So the strategic sequence is:

```text
sidebar proving slice
-> workspace substrate correction
-> workspace durability widening
-> richer artifact lanes
```

not:

```text
sidebar proving slice
-> immediate design-artifact lane
```

---

## Current Runtime Reality

This program is grounded in what the code currently does.

### Current shape

Today the runtime is still broadly:

```text
atoms
-> Missionary/Electric derive large runtime structures
-> render assembles a world snapshot
-> render uploads and draws
-> input modules mutate many atoms directly
```

### What is already better than before

- runtime modules are separated
- shared scene caching exists
- the sidebar slice has a real truth boundary
- the spatial layer is already more reusable than the semantic layer

### What remains atom-centered

The state layer still has many adjacent atoms that represent one partially reconstructed workspace:

- editor document
- current file
- sidebar truth/overlay/ui
- focus
- active pane
- flow state
- various scroll positions
- various drag/hover state

This is not inherently wrong. The problem is that too much semantic meaning still lives across those atoms without one canonical composition point.

### What that means

The runtime has already been modularized enough to refactor correctly. But the ontology is still only partially corrected.

This refactor is the step where the ontology gets pulled into the runtime proper.

---

## Target Architecture

The target architecture for this program is:

```text
gesture / command / agent action
-> workspace proposal
-> commitment boundary
-> committed workspace truth
-> optimistic overlay
-> ephemeral UI/cache state
-> effective local world
-> semantic split/pane model
-> one spatial projection
-> render / interaction / export sinks
```

The key idea is:

```text
one semantic world
-> one spatialization
-> many consumers
```

not:

```text
one render reconstruction
+ one hit-test reconstruction
+ one export reconstruction
+ one workflow-specific reconstruction
```

### Responsibility split

#### Rama

Owns:

- durable workspace truth that matters to handoff/re-entry
- selected artifact identity when it matters durably
- split arrangement when it matters durably
- workflow state that must survive handoff/replay
- trails/lineage later in the program

Does not own:

- hover
- blink
- transient pointer state
- scroll jitter
- one-frame drag preview noise

#### Electric / Missionary derivation layer

Owns:

- watching committed truth
- combining truth, overlay, and local state
- deriving the current effective local world
- deriving pane descriptors and semantic visibility
- narrowing invalidation around actual dependency boundaries

#### Spatial layer

Owns:

- scene tree
- layout
- clipping
- hit regions
- z-order

It is the inverse-preserving bridge between semantic world and render/interaction.

#### WebGPU

Owns:

- buffers
- instance data
- final draw

It does not own semantic meaning.

#### Local client state

Owns only:

- hover
- drag preview
- pointer state
- scroll positions
- caches
- in-flight request state
- blink/focus noise

The local client is not allowed to become the second hidden source of workspace truth.

---

## Core State Classes

The workspace should be described with four classes of state.

### 1. Workspace truth

This is the durable or semantically committed workspace state.

Proposed shape:

```clojure
{:workspace/id ...
 :project {:path ... :name ...}
 :selected-artifact {:kind :file :path ...}
 :split ...
 :active-pane ...
 :workflow-context ...}
```

This shape may later be normalized differently in Rama, but these are the semantic fields that matter.

### 2. Workspace overlay

This is optimistic local intent that has not yet been reconciled with truth.

Proposed shape:

```clojure
{:pending-project ...
 :pending-selection ...
 :pending-split ...
 :pending-pane-focus ...}
```

The overlay is not a second truth store. It is a short-lived semantic bridge.

### 3. Workspace UI/cache state

This is local-only state.

Proposed shape:

```clojure
{:hover ...
 :scrolls ...
 :drag ...
 :caches ...
 :in-flight ...}
```

The important thing is not the exact literal shape yet. It is that these are explicitly local and not mistaken for truth.

### 4. Effective local world

This is the key missing object.

It is derived from:

- workspace truth
- workspace overlay
- workspace UI/cache state
- artifact content/cache state

and it should be the single semantic root consumed by:

- pane builders
- workflow surface builders
- scene derivation
- render
- interaction

---

## Artifact Model

The substrate should stop special-casing files as if the workspace were fundamentally "a file plus some side panels."

Selection should be generalized to `artifact`.

### Initial artifact kinds

The first kinds should be:

- `:file`
- `:workflow`
- `:trail`
- `:preview` later

### Why this matters

Once selection is artifact-based:

- file loading becomes one instance of artifact loading
- workflow entry becomes one kind of artifact transition
- trails in the chat pane become actual pane content, not an incidental side effect
- preview can later become an artifact consequence slot rather than a placeholder box

This is the point where the workspace starts to match the ontology instead of only the current file-centric implementation.

---

## Split And Pane Model

The 3-pane shell should be preserved visually for now, but its semantics need to be corrected.

### A split is not layout trivia

A split is preserved co-presence. It should represent:

- what is being held together right now
- which artifact is primary
- which artifact/process is adjacent
- which consequence slot is present

### A pane is a semantic fill

Panes should be described semantically, not by hardcoded mode branches.

For example:

```clojure
{:pane/id :main
 :role :primary-artifact
 :artifact-ref {:kind :file :path ...}}
```

or:

```clojure
{:pane/id :right
 :role :trail
 :artifact-ref {:kind :trail :run-id ...}}
```

### Preserve current UI, improve semantics

This refactor should not begin by redesigning the workspace layout. It should begin by making the existing shell a derived consequence of the semantic substrate.

That means:

- keep the 3-pane layout for now
- stop letting the current shell be the hidden ontology
- make shell composition derive from local-world + split

---

## Execution Principles

These rules govern how this program should be implemented.

### 1. Phase 0 is mandatory before major code moves

The ownership freeze is not optional planning overhead. It is the step that prevents the rest of the program from hardening confusion.

### 2. Safe-now persistence is allowed before full workspace durability

Not all Rama persistence carries the same ontological risk.

There are two categories:

- **safe-now persistence**
  - settings
  - agent trail / agent-output persistence
- **workspace-schema persistence**
  - selected artifact
  - split semantics
  - local-world truth
  - workflow context that depends on the corrected substrate
  - editor truth once its commitment boundary is understood

Safe-now persistence can begin after Phase 0, because it does not decide the core workspace ontology.

Workspace-schema persistence still stays later, after the semantic model stabilizes.

### 3. No editor latency model is assumed in advance

The editor durability strategy is **not pre-locked**.

Specifically:

- do **not** assume up front that the editor must stay local-until-save
- do **not** assume up front that per-keystroke committed events will be too slow
- first try the direct committed path on the real system and measure it
- only introduce batching, checkpoints, or a hybrid commit model if the direct path misses responsiveness goals in practice

The decision rule is:

```text
measure first
assume later only if measurement forces it
```

### 4. Differential rendering is part of this program

Differential rendering is not optional polish. It is part of the refactor because:

- the spatial layer already exists
- the sidebar already proved one small differential beachhead
- semantic cleanup should converge with keyed mounts and narrower invalidation

### 5. Keep architecture and engineering at different altitudes, but in one program

This program is architecture-first in ordering, but it must still contain:

- concrete event/proposal design
- concrete persistence slices
- concrete differential rendering proof work
- concrete verification gates

That is the intended blend.

---

## Program Phases

## Phase 0 — Freeze the truth model

### Purpose

Stop the refactor from beginning with more code movement and more local fixes.

The first job is to make the ownership map explicit.

### Work

- inventory the meaningful runtime atoms and classify each one as:
  - committed truth
  - optimistic overlay
  - local ephemeral/cache state
  - derived state
  - transitional/legacy compatibility
- identify duplicate or ambiguous ownership
- name the intended canonical workspace fields
- decide which fields remain explicitly local even after the refactor

### Key outputs

- a locked workspace state table
- a locked list of transitional fields
- a locked list of "local-only forever" fields

### Why it comes first

If this is not done first, every later phase will make hidden assumptions and the program will become circular again.

### Done means

- every workspace concern has one intended owner class
- there is no unresolved ambiguity about whether a field is truth, overlay, local, or derived

---

## Early Parallel Lane — Safe-Now Persistence

This is not a replacement for the semantic phases. It is a narrow lane that can begin **after Phase 0** because it does not harden the core workspace ontology.

### Included now

#### Settings persistence

This is the lowest-risk persistence slice.

Why it is safe now:

- the settings schema is already well understood
- it does not decide local-world semantics
- it reuses the sidebar pattern almost directly

#### Agent trail / agent-output persistence

This is also safe enough to start early, provided it is scoped correctly.

Important boundary:

- do not persist per-token display noise as canonical truth
- persist completed semantic trail blocks and run structure
- keep display-time streaming responsiveness local

### Explicitly not included in the early lane

- selected artifact truth
- split truth
- local-world truth
- workflow context truth that depends on the corrected substrate
- editor truth before the commitment boundary is understood

### Why this lane exists

This captures the strongest concrete insight from the differential plan without giving up the architecture ordering:

- some persistence wins are safe now
- the workspace ontology still should not be hardened prematurely

---

## Phase 1 — Create the workspace action/proposal boundary

### Purpose

Separate semantic actions from surface-local mutations.

### Work

- define a small workspace action vocabulary
- route semantic changes through explicit proposal handlers
- stop letting input modules directly own committed workspace meaning
- preserve direct local-only mutation for truly ephemeral concerns

### Initial action vocabulary

At minimum:

- select project
- select artifact
- mutate split
- set active pane
- enter workflow artifact
- exit workflow artifact

### Why it matters

Without a workspace action boundary, the substrate cannot own meaning. Surfaces will continue to own meaning accidentally.

### Done means

- every semantic transition can be named as one workspace action
- direct semantic mutation from input modules is clearly reduced and bounded

---

## Phase 2 — Unify selected artifact and load ownership

### Purpose

Turn selection and loading into one coherent semantic path.

### Work

- replace file-special-case thinking with artifact selection
- separate:
  - artifact selected
  - artifact content loaded
  - pane showing artifact
- centralize artifact loading ownership
- dedupe in-flight loads
- ignore stale responses cleanly

### Why it matters

Right now, selection and loading still overlap too loosely. This phase makes the workspace speak in terms of:

```text
the user selected artifact X
the system is loading artifact X
the visible pane is showing X
```

instead of those states bleeding into each other.

### Done means

- one selection action creates one stable semantic transition
- one artifact has one load owner
- stale loads do not fork visible state

### Editor commitment boundary rule

The editor belongs to this substrate work, but its durability strategy is still experimentally open.

The implementation order is:

1. define the editor event/proposal vocabulary
2. attempt the direct committed path on the real system
3. measure responsiveness under normal typing, burst editing, undo/redo, and paste
4. only then choose whether the editor needs:
   - direct committed events
   - buffered checkpoints
   - save-time batching
   - some other hybrid

This program does **not** lock the hybrid model in advance.

---

## Phase 3 — Derive the local world

### Purpose

Introduce the first real canonical workspace object.

### Work

- define `effective-local-world`
- derive it from truth, overlay, local UI/cache state, and artifact content/cache state
- move pane builders and workflow surface composition to consume it
- stop reconstructing the workspace separately in multiple places

### Why it matters

This is the phase where the project can finally answer:

```text
what local world is active right now?
```

with one object in code.

### Done means

- the visible workspace has one semantic root
- pane builders do not need to chase scattered atoms to infer meaning

---

## Phase 4 — Formalize split and pane semantics

### Purpose

Turn the shell into a semantic split model instead of a hardcoded layout branch.

### Work

- define split as a meaning-bearing structure
- define pane descriptors as semantic fills
- derive shell composition from local-world + split
- preserve current proportions/visual behavior where possible while correcting the underlying model

### Why it matters

Without this phase, the workspace remains visually multi-pane but semantically flat.

### Done means

- pane identity and pane content are semantically explicit
- split state is not just geometry
- workflow/file/trail/preview can all be expressed as pane fills

---

## Phase 5 — Migrate workflow and review surfaces onto the substrate

### Purpose

Make workflow surfaces live inside the workspace substrate instead of beside it.

### Work

- express DG workflow surfaces as local-world/split fills
- stop treating workflow mode as a second workspace universe
- define how review-artifact surfaces will live inside the same substrate
- preserve existing workflow behavior while moving its mounting model

### Why it matters

This is where the runtime stops being:

```text
file workspace here
workflow workspace there
```

and becomes:

```text
one workspace substrate
multiple artifact/workflow fills
```

### Done means

- workflow entry is a local-world transition, not a mode escape hatch
- right-pane detail/trail/review surfaces are substrate residents

---

## Phase 6 — Narrow render and reactive invalidation

### Purpose

Exploit the semantic cleanup to reduce oversized derivations and mounts.

### Work

- split broad derivations where semantic branches are now stable
- key scene/update paths off stable identity
- reduce unnecessary text/rect/shadow rebuild churn
- ensure render and interaction consume the same semantic-spatial projection

### Concrete sub-phases

#### 6A — Differential proof on the sidebar path

Use the smallest already-proven surface to validate:

- keyed reactive diffing cost
- mount/unmount lifecycle correctness
- buffer-pool slot ownership
- whether `e/for-by` or equivalent keyed mount paths are cheap enough on real surfaces

#### 6B — Editor rect differential path

Move editor rect-like structures toward keyed identity:

- caret
- current line
- selection ranges
- fold markers
- bracket match markers

The goal is O(changed) rect updates rather than broad rect rewrites.

#### 6C — Text differential design decision

This is the hardest render-engineering subproblem in the program.

The main decision space is:

- per-line or per-block text pools
- one shared buffer with dirty-range tracking

Do not lock this by elegance alone. Choose based on the differential proof results and actual engineering disruption.

#### 6D — Dirty-present strategy

Move toward:

- semantic diff sets dirty flag
- one-shot RAF
- present once
- no continuous polling loop except where explicitly required

This phase must account for timers like caret blink and any other local time-based invalidators.

### Why it comes after semantic cleanup

Performance tuning before semantic cleanup usually treats symptoms. This phase happens here because semantic boundaries are what make invalidation narrowing honest.

### Done means

- hover/scroll/local changes stay local
- semantic transitions invalidate the branches they actually affect
- render and interaction reuse the same identity map

---

## Phase 7 — Widen Rama to true workspace durability

### Purpose

Persist the stabilized workspace semantics.

### Work

- move the correct workspace truth fields into Rama PStates
- keep ephemeral/UI-only state local
- preserve re-entry/handoff on the stabilized schema
- expand from sidebar truth into actual workspace truth

### Why it is not earlier

Moving these fields to Rama before the model stabilizes would just harden the wrong schema.

### Done means

- workspace re-entry restores a coherent local world
- persisted workspace truth matches the corrected substrate model

---

## Phase 8 — Reconnect broader product lanes

### Purpose

Resume product widening on top of the corrected substrate.

### Includes

- review artifact workspace
- trail persistence/lineage widening
- design-artifact / preview lanes
- richer public-form artifact types

### Rule

These lanes come after substrate stabilization, not before.

---

## Dependency Order

This is the hard critical path:

```text
Phase 0 ownership map
-> early safe-now persistence lane may run in parallel
-> Phase 1 action/proposal boundary
-> Phase 2 selected-artifact/load ownership
-> Phase 3 effective local world
-> Phase 4 split/pane semantics
-> Phase 5 workflow/review migration
-> Phase 6 render/reactive narrowing + differential pipeline
-> Phase 7 Rama workspace durability widening
-> Phase 8 broader product lanes
```

### Why the order is mandatory

- you cannot make split meaning-bearing until you know what local world it belongs to
- you cannot make local world coherent until selection/load ownership is stable
- you cannot narrow invalidation around semantic boundaries that do not yet exist
- you should not widen Rama durability before the workspace truth schema is correct
- you can safely persist some non-ontological slices earlier, but that does not remove the need for the semantic ordering

---

## File-Level Pressure Areas

This is not a file-by-file edit plan, but these are the pressure zones the program is about.

### Workspace runtime shell
- `src/app/client/workspace/runtime.cljs`

This is where truth/watch wiring, module integration, and current world assembly pressure are most visible.

### Runtime state and ownership
- `src/app/client/workspace/runtime/state.cljs`

This is where the current atom-centered shape is most visible and where the first ownership freeze has to be grounded.

### Input ownership
- `src/app/client/workspace/runtime/mouse.cljs`
- `src/app/client/workspace/runtime/keyboard.cljs`

These are where semantic actions are still too close to surface-specific mutation.

### Render/world assembly
- `src/app/client/workspace/runtime/render.cljs`

This is where broad snapshot assembly and future invalidation narrowing will meet.

### Shell and workflow surfaces
- `src/app/client/workspace/shell.cljs`
- `src/app/client/workflows/dg_flow.cljs`

These are where pane semantics and workflow-as-substrate-resident work become concrete.

### Server durability
- `src/app/server/rama/core.clj`
- `src/app/server/rama/util_fns.cljc`

These should widen only after the workspace schema is stabilized.

---

## Acceptance Criteria

The program is only "done" when all of these are true.

### Workspace coherence

- one derived `effective-local-world` explains the visible workspace
- one semantic owner exists per workspace concern
- there is one coherent selected-artifact path

### Selection and content

- artifact selection is no longer conflated with buffer loading
- one artifact has one authoritative load owner
- repeated selection does not fork or duplicate visible state

### Pane/split semantics

- split is meaning-bearing, not just layout
- panes are semantic fills
- workflow/file/trail/preview surfaces are expressible within one pane model

### Workflow integration

- workflow surfaces mount inside the same substrate as file workspace
- entering workflow context is a local-world transition, not a separate workspace ontology

### Render/reactive integrity

- render and interaction consume the same semantic-spatial projection
- local ephemeral changes do not trigger broad semantic churn
- invalidation boundaries are narrower and semantically explainable

### Durability

- durable workspace state restores a coherent local world on re-entry
- local-only state remains local

### Editor durability decision

- the editor commitment model is chosen from measurement rather than assumption
- if a hybrid model is adopted, it is because direct committed editing was actually tested and shown to miss responsiveness goals

---

## Risk Register

| Risk | Why it matters | Mitigation |
|------|----------------|------------|
| Hardening the wrong workspace schema in Rama | Would freeze the wrong ontology into durable truth | Keep workspace-schema persistence late; allow only safe-now slices early |
| Editor direct-commit path introduces typing jank | Could force the wrong conclusion if assumed rather than measured | Test direct committed editing first, then decide from evidence |
| Differential keyed mounts add too much reactive overhead | Could make the differential pipeline more expensive than batch rebuilds | Validate on the sidebar surface first before widening |
| Text differential restructuring is more invasive than expected | Could destabilize the renderer if chosen casually | Treat text differential as an explicit design decision with proof data first |
| Workflow remains a parallel workspace ontology | Would undermine the whole substrate program | Give workflow migration its own phase and exit criteria |

---

## Verification Checklist

| Stage | Test | Pass criteria |
|-------|------|---------------|
| Phase 0 | ownership review | every meaningful field has one owner class |
| Early lane | change setting, reload | persisted setting restores correctly |
| Early lane | complete agent run, reload | persisted trail/run structure restores correctly |
| Phase 2 | switch artifacts quickly | one stable selected artifact, no stale late overwrite |
| Phase 2 | editor direct-commit experiment | typing path is measured before any batching assumption |
| Phase 3 | inspect workspace state during use | one derived local world explains the visible workspace |
| Phase 4 | change pane focus/content | pane identity and pane fill remain semantically coherent |
| Phase 5 | move between file and workflow contexts | same substrate, not two parallel workspace models |
| Phase 6 | hover/scroll/local interactions | no broad unrelated text/rect churn |
| Phase 6 | differential proof | changed elements update in O(changed), not O(total) where intended |
| Phase 7 | reload or handoff | coherent local world restores from durable truth |

---

## Non-Goals

This program is not:

- another sidebar rescue
- a broad visual redesign
- a pure performance pass detached from state ownership
- a "move everything to Rama immediately" initiative
- a design-artifact implementation-first initiative

Those things may happen later, but they are not the substrate program.

---

## Assumptions And Defaults

- the sidebar slice is treated as done for this refactor thread
- workspace substrate is the chosen anchor slice
- preserve current visible product behavior where possible while correcting substrate semantics
- use the sidebar’s truth/overlay/ui split as precedent, not as the final substrate itself
- let Rama widen only after the workspace schema is stable

---

## The Short Version

This program moves the project from:

```text
modularized but still atom-centered workspace runtime
```

to:

```text
workspace truth
+ workspace overlay
+ workspace UI/cache state
= effective local world
-> semantic split/pane model
-> one spatial projection
-> narrower mounts and better durability
```

That is the real unfinished refactor thread.
