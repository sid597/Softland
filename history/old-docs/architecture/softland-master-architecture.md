# Softland Master Architecture

> Status: MASTER THREAD
> Purpose: one canonical architecture board for the broadest Softland we intend to build.
> Use: start here, then branch into narrower implementation docs.

## 1. Operating Law

```text
Persist meaning.
Derive the current local world.
Spatialize once.
Mount many projections.
Keep ephemeral attention local.
```

That is the whole architecture in five lines.

## 2. Canonical Nouns

These are the identity-bearing objects. They are the ontology row, not the rendering row.

- `world substrate` — the family of Softlands, branches, forks, lineage
- `local world` — the atomic inhabitable packet of understanding
- `split` — preserved tension / co-presence inside a local world
- `artifact` — the current consequence or work product
- `trail` — replayable process / history / probe-response-revision path
- `workflow` — traversal between local worlds
- `relation` — the underlying graph / discourse / dependency structure
- `projection grammar` — rules that turn ontology into visible form
- `event log` — append-only temporal substrate
- `lineage` — relations between world versions, branches, and forks

Important:

- `editor`, `chat`, `preview`, `summary`, `diff`, `tests` are not top-level ontology objects.
- They are projection fills or workflow-specific surfaces inside a local world.

## 3. Structural Model

```text
WORLD SUBSTRATE
|
|-- local world A
|    |-- split / map surface
|    |-- artifact / workspace surface
|    |-- trail / history surface
|    |-- affordance / command surface
|    `-- live tension / question
|
|-- local world B
|    `-- same pattern, different contents
|
|-- workflows = traversals between local worlds
`-- lineage   = relations between world versions / branches / forks
```

Read this as:

- the world is primary
- the local world is the atomic unit
- workflows move through local worlds
- lineage relates versions of worlds

## 4. System Pipeline

This is the execution row.

```text
AUTHORS / SOURCES
human | agent | designer plugin | importer | external refs
                    |
                    v
PROPOSALS
commands | edits | patches | imports | workflow asks
                    |
                    v
COMMITMENT BOUNDARY
commit | reject | fork | merge | undo point | public form
                    |
                    v
RAMA EVENT LOG
append-only events
                    |
                    v
RAMA PSTATES
artifacts | local worlds | workflows | trails | sessions | lineage
                    |
                    v
ELECTRIC DERIVATION
watch committed truth + choose branch + theme + viewport + workflow
                    |
                    v
VISIBLE LOCAL WORLD
the current inhabitable slice
                    |
                    v
SPATIAL PROJECTION
rect-tree | layout | bounds | z | clip | actions | hit regions
                    |
          +---------+-------------+-------------+
          |                       |             |
          v                       v             v
   INTERACTION MOUNT         WEBGPU MOUNT     EXPORT MOUNT
   hit-index / dispatch      rects/text/etc   .edn / .cljc / APIs
          |                       |
          v                       v
      input loop               pixels
          |
          `-----------------> back to proposals
```

Core reading:

- `Rama` stores committed meaning and history.
- `Electric` derives the current world.
- `WebGPU` renders one projection of that world.
- the interaction system queries the same spatialized world, not a second guessed one.

## 5. Responsibility Split

### Rama

Owns:

- committed artifacts
- local worlds
- workflow state that should survive handoff / replay
- trails and lineage
- append-only event history

Does not own:

- hover
- drag jitter
- caret blink
- one-frame preview noise

### Electric

Owns:

- watching committed truth
- deriving the current visible / interactable local world
- keyed differential mount points
- branch / theme / viewport selection logic

### Spatial Layer

Owns:

- rect-tree / scene-tree
- layout
- bounds
- hit regions
- z-order / clip

This is the shared inverse-preserving layer between rendering and interaction.

### WebGPU

Owns:

- buffers
- camera transforms
- rect / text / shadow instance data
- final draw

Does not own:

- semantic meaning
- workflow meaning
- artifact identity

### Local Client State

Owns only fleeting attention state:

- hover
- drag preview
- caret blink
- temporary focus
- transient pointer state

## 6. Current Reality

Today the runtime is still mostly:

```text
atoms
-> Missionary derives world snapshot
-> render samples snapshot on RAF
-> rebuild / upload subsystems
-> WebGPU draws
```

Concrete current facts:

- truth is still primarily client-local and atom-centered in `runtime/state.cljs`
- input handlers still mutate atoms directly in `runtime/mouse.cljs`
- render still assembles a `world` snapshot in `runtime/render.cljs`
- `rect_tree.cljs` is a derived scene/layout layer, not source of truth
- Rama currently holds only a partial durable backend slice
- the sidebar buffer pool is the first real differential rendering beachhead

So the refactor succeeded at:

- splitting the runtime into sane modules
- making shared runtime context explicit
- separating render assembly from input routing
- giving us a reusable spatial layer
- creating a real seam for differential rendering

But it did not yet complete:

- `commitment boundary`
- `atoms -> PStates`
- `snapshot -> keyed differential mount`
- `viewport / zoom -> visible node diffs`

## 7. Target Runtime

The target is:

```text
gesture / command / agent action
-> proposal
-> commitment boundary
-> Rama event
-> Rama PState
-> Electric watch
-> visible local world
-> spatial projection
-> keyed differential mount
-> GPU + interaction + export sinks
```

The important correction is this:

```text
one semantic world
-> one spatial projection
-> many mounts
```

Not:

```text
one render reconstruction
+ one interaction reconstruction
+ one export reconstruction
```

Each stable entity should have:

- one identity
- one lifetime
- one semantic owner
- one spatialization
- many projections

## 8. Designer / Theme / Component Lane

This is the design-artifact slice that can become the first clean end-to-end implementation.

### Canonical saved thing

The canonical saved thing should be:

```text
canonical component blueprint
= tokenized design IR
+ states
+ slots
+ source-meta
+ provenance / trail
```

It should not be:

- screenshot
- extractor JSON
- `_source.edn`
- compiled rt-node
- GPU buffers
- generated `.cljc` alone

### Pipeline

```text
Inventory & Provenance
registry + _source.edn
          |
          v
Source Acquisition
DOM extraction or LLM-assisted read
          |
          v
Design IR Normalization
adapter -> literal Design IR
          |
          +<-------------------+
          |                    |
          v                    |
Theme Tokenization             |
token map + matcher            |
          |                    |
          v                    |
Canonical Component Blueprint  |
states + slots + source-meta   |
          |                    |
     +----+----+               |
     |         |               |
     v         v               |
Compiler   Exporter            |
     |         |               |
     v         v               |
rt-node    .cljc module        |
preview    schema/render/demo  |
     |                         |
     +---- verification -------+
           chat corrections
```

This lane is already visible in the current code spine:

- `adapter` normalizes extracted data into Design IR
- `token_matcher` converts literal values into token refs
- `compiler` turns the blueprint into `rt-node`
- JIT component library exports `schema + render + demo`

## 9. What Softland Should Persist

Persist these:

- artifact identity
- local-world identity
- workflow-relevant arrangement
- selected / expanded / open / active state when it matters to handoff
- provenance
- trails
- branches / lineage
- design blueprints
- token packs / themes

Do not persist these as canonical truth:

- absolute row `y` positions
- flattened rect arrays
- glyph quads
- GPU buffers
- one-frame hover state
- temporary pointer ownership

## 10. First Migration Slice

Do not migrate the whole app at once.

The best first clean slice is:

```text
component-spec + theme-pack + live preview
```

Sequence:

1. define committed `component blueprint` and `theme-pack` schemas in Rama
2. route designer edits through proposal -> commitment
3. watch committed artifact state from Electric
4. derive `active blueprint + active theme + demo props`
5. compile into `rt-node`
6. spatialize once
7. mount differentially to:
   - GPU preview
   - interaction regions
   - exported module output
8. keep hover / drag / temporary focus local

Why this slice first:

- it is small enough to finish
- it exercises the full architecture honestly
- it gives a live preview loop
- it creates a real canonical artifact type
- it proves the commitment boundary matters

## 11. Migration Path After That

After the design-artifact slice works end-to-end:

1. move trail persistence into Rama
2. move local-world / workspace state into Rama where handoff matters
3. replace snapshot world assembly with keyed Electric mount paths
4. make interaction mount differential alongside GPU mount
5. wire viewport / zoom into visible-node derivation
6. only then generalize to larger workflow surfaces

The rule is:

```text
prove one full vertical slice,
then widen the ontology,
then widen the runtime.
```

## 12. Code Anchors

Current runtime anchors:

- `src/app/client/workspace/runtime/state.cljs`
- `src/app/client/workspace/runtime/render.cljs`
- `src/app/client/workspace/runtime/mouse.cljs`
- `src/app/client/workspace/rect_tree.cljs`
- `src/app/server/rama/core.clj`

Current architecture docs:

- `docs/plans/where-we-are.md`
- `docs/architecture/differential-pipeline-s38.md`
- `docs/architecture/migration-concrete-s38.md`

Design-artifact / JIT anchors:

- `docs/architecture/component-library-jit.md`
- `docs/architecture/design-converter-how-it-works.md`
- `src/components/adapter.cljc`
- `src/components/token_matcher.cljc`
- `src/components/compiler.cljc`
- `components/_registry.edn`
- `components/*/_source.edn`

Vision / ontology anchors:

- `docs/vision/epistemic-framework.md`
- `docs/vision/what-softland-is-claude.md`
- `docs/vision/what-softland-is-codex.md`

## 13. One-Line Summary

```text
Softland should store committed meaning in Rama,
derive the current inhabitable world in Electric,
spatialize that world once,
and let WebGPU, interaction, and exports consume that shared projection.
```
