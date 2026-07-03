# Electric Way Audit

Purpose: discover which parts of the codebase do not follow the Electric / Missionary way.

Scope of this document:
- discovery only
- no implementation plans yet
- record findings as source -> derivation -> terminal topology problems

## Audit Lens

Questions for each area:
- What are the sources?
- What derivations subscribe to them?
- Where are the terminal side effects?
- Is invalidation scoped narrowly or does one derivation own too much?
- Are "pure-looking" builders actually pure?
- Does each atom have one clear owner?
- Are there undeclared dependencies via direct deref inside derivations?
- Is the world-model computed once and reused, or rebuilt separately in event handlers?

## Coverage Status

This audit has now covered the active source tree broadly enough to stop calling it a hotspot-only read.

Audited as active client/runtime code:
- `src/app/client/workspace/combined_text.cljs`
- `src/app/client/workspace/editor_compute.cljs`
- `src/app/client/workspace/runtime/render.cljs`
- `src/app/client/workspace/runtime.cljs`
- `src/app/client/workspace/events.cljs`
- `src/app/client/workspace/runtime/state.cljs`
- `src/app/client/workspace/runtime/mouse.cljs`
- `src/app/client/workspace/runtime/scroll.cljs`
- `src/app/client/workspace/runtime/keyboard.cljs`
- `src/app/client/workspace/runtime/sidebar_io.cljs`
- `src/app/client/workspace/runtime/interop.cljs`
- `src/app/client/workspace/runtime/agent_flow.cljs`
- `src/app/client/workspace/cmd_panel.cljs`
- `src/app/client/workspace/settings_view.cljs`
- `src/app/client/workspace/shell.cljs`
- `src/app/client/workspace/sidebar.cljs`
- `src/app/client/workspace/rect_tree.cljs`
- `src/app/client/workspace/ui_primitives.cljs`
- `src/app/client/workspace/text_input.cljs`
- `src/app/client/workspace/trail.cljs`
- `src/app/client/workspace/agent.cljs`
- `src/app/client/workspace/themes.cljc`
- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/workflows/dg_flow.cljs`
- `src/app/client/workflows/jit.cljs`

Audited as Electric bridge / shared app boundary:
- `src/app/electric_flow.cljc`
- `src/app/file_viewer.cljc`

Audited as server / component / domain code:
- `src/app/server_jetty.clj`
- `src/app/server/file.clj`
- `src/app/server/review_pack.clj`
- `src/app/server/rama/core.clj`
- `src/app/server/rama/objects.cljc`
- `src/app/server/rama/roam_ns.clj`
- `src/app/server/rama/util_fns.cljc`
- `src/components/adapter.cljc`
- `src/components/compiler.cljc`
- `src/components/css_parsers.cljc`
- `src/components/design_tokens.cljc`
- `src/components/token_matcher.cljc`

Audited as legacy / historical artifact:
- `src/global_flow.cljs`
- `src/app/client/workspace/runtime/mouse.cljs.bak`

Explicitly excluded:
- `src/app/server/env.clj`
  - intentionally not read because it contains secrets

## Paradigm Placement

The current codebase is not best described as "pure FRP."

It is best described as:
- `FRP islands inside an event-loop and shared-state architecture`

If placed on the "principal programming paradigms" chart, its center of gravity sits between:
- `Functional reactive programming`
- `Event-loop programming`
- `Stateful functional programming`
- and imperative terminals at the edge

More concretely:
- Closest to FRP:
  - `events.cljs`
  - scoped derivations in `combined_text.cljs`
  - scoped derivations in `editor_compute.cljs`
  - scoped derivations in `settings_view.cljs`
- Closest to event-loop programming:
  - `runtime/mouse.cljs`
  - `runtime/keyboard.cljs`
  - `runtime/scroll.cljs`
  - callback-driven boundary code in `runtime/sidebar_io.cljs`
- Closest to stateful functional / shared-state programming:
  - `runtime/state.cljs`
  - the atom lake threaded through `runtime.cljs`
  - agent / flow / sidebar state mutated from several modules
- Closest to imperative programming:
  - `runtime/render.cljs`
  - `substrate/webgpu/renderer.cljs`
  - DOM overlay control in `runtime/interop.cljs`
  - HTTP / SSE boundary code in `server_jetty.clj`

Electric-specific reading:
- `electric_flow.cljc` is reactive and distributed in form
- but the application world is not yet primarily modeled as Electric expressions
- Electric currently bootstraps and sites a Missionary + atoms runtime more than it defines the whole application as one Electric graph

## Finding 1: Render Hub Has Improved, But The Split Is Incomplete

Area:
- `src/app/client/workspace/combined_text.cljs`
- `src/app/client/workspace/editor_compute.cljs`
- `src/app/client/workspace/runtime/render.cljs`

Diagnosis:
- The terminal render boundary is reasonably clean.
- The render hub has already been partially refactored into scoped sub-flows.
- The old "one giant projection per side" shape is no longer fully true.
- The remaining issue is that some local worlds are still assembled too broadly and then rebuilt again elsewhere.

Evidence:
- `combined_text.cljs:27-68`
- `combined_text.cljs:74-413`
- `editor_compute.cljs:272-420`
- `render.cljs:62-97`

Why this is not the Electric way:
- The flow-canvas branch and editor branch are now more separated, which is good.
- But the main text assembly still braids editor text, file-layout text, sidebar text, command panel text, agent text, and status text into one derivation.
- The rect side also still assembles sidebar, mode selection, and editor/file/extract content into one higher-order projection.
- This is much better than the earlier flat shape, but it is still not yet "one clear local world per derivation."

Concrete examples:
- `combined_text.cljs:44-68`
  - `<flow-text-content>` isolates intake/run text from editor concerns.
- `editor_compute.cljs:313-337`
  - `<flow-content>` isolates intake/run rects from editor concerns.
- `combined_text.cljs:74-413`
  - the main assembly is still wide enough that file-open chat, sidebar, command panel, and status text continue to live together.
- `runtime.cljs:56-83`
  - timer sources still exist, but the graph is now in a mixed state: partly scoped, partly broad.

Important nuance:
- This is not a terminal-side problem first.
- `render.cljs:100-248` keeps GPU writes and draw calls near the edge, which is healthy.
- The current problem is less "everything is one monolith" and more "the refactor is incomplete and the same world is still rebuilt across layers."

## Finding 2: There Are Also Good Electric Islands

Area:
- `src/app/client/workspace/editor_compute.cljs`
- `src/app/client/workspace/settings_view.cljs`
- `src/app/client/workspace/runtime/render.cljs`

Healthy patterns:
- `editor_compute.cljs:128-149`
  - `<fold-state>` and `<bracket-match>` are scoped derivations with clear inputs.
- `editor_compute.cljs:272-420`
  - `<layout>`, `<mode>`, `<flow-content>`, and `<editor-content>` show the intended split explicitly.
- `combined_text.cljs:27-68`
  - `<layout>` and `<flow-text-content>` are another good step toward narrower invalidation.
- `settings_view.cljs:178-190`
  - `<settings-panel-rects>` is narrow and legible.
- `settings_view.cljs:316-327`
  - `<settings-panel-text>` is similarly scoped.
- `render.cljs:100-248`
  - terminal side effects remain concentrated in one consumer.

Why note this:
- The codebase already contains the shape we want.
- The main issue is inconsistency: some areas are nicely scoped, others collapse multiple concerns into one derivation.

## Finding 3: `build-right-detail` Is Now Pure, Which Is Progress

Area:
- `src/app/client/workflows/dg_flow.cljs:488-674`

Diagnosis:
- The old hidden side effect inside `build-right-detail` is gone.
- The function currently behaves like a pure builder again.

Why this matters:
- This means the earlier smell was real, but has already been removed.
- The audit should not keep treating this function as impure.

Related note:
- `build-intake-tree` at `dg_flow.cljs:676-840` is also largely in the "pure projection" shape.
- That makes it a relatively healthy local world compared to the render hub.

## Finding 4: Detail Scroll Ownership Is Still Fuzzy

Area:
- `src/app/client/workflows/dg_flow.cljs:547-560`
- `src/app/client/workspace/runtime/scroll.cljs:83-88`

Diagnosis:
- The render-side detail view computes `max-scroll` locally and clamps `detail-scroll-y` to it.
- The scroll consumer increments `!detail-scroll-y` without clamping to that semantic max.

Why this is not quite the Electric way:
- The source atom can drift beyond the meaningful range for the current projection.
- The projection compensates locally instead of the source and projection agreeing on one semantic value.
- This is much better than mutating derived state from a builder, but ownership is still not fully crisp.

Important nuance:
- This is a topology / ownership issue, not a current purity violation in `dg_flow`.

## Finding 5: Interaction Code Rebuilds The World Model Imperatively

Area:
- `src/app/client/workspace/runtime/mouse.cljs`

Diagnosis:
- Mouse handlers reconstruct trees directly from atoms in order to hit-test.
- This means the interaction layer recomputes view structure independently of the render projections.

Evidence:
- Sidebar click rebuild:
  - `mouse.cljs:159-198`
- Chat pane rebuild:
  - `mouse.cljs:261-311`
- Flow canvas click rebuild:
  - `mouse.cljs:313-334`
- Sidebar hover rebuild:
  - `mouse.cljs:423-442`
- Flow canvas hover rebuild:
  - `mouse.cljs:454-470`

Why this is not the Electric way:
- The rendered world and the interactable world are not obviously the same derived object.
- Instead, event handlers rebuild approximations of the world from source atoms.
- This splits topology: one branch exists for rendering, another for hit-testing.

Risk:
- Drift between what is rendered and what is hit-tested.
- Extra recomputation in event paths.
- Harder reasoning about ownership because interaction logic is not consuming a shared derived projection.

## Finding 6: File Layout Is One Pure Builder, But Three Separate Reconstructions

Area:
- `src/app/client/workspace/shell.cljs:8-279`
- `src/app/client/workspace/combined_text.cljs:226-234`
- `src/app/client/workspace/editor_compute.cljs:383-391`
- `src/app/client/workspace/runtime/mouse.cljs:273-280`

Diagnosis:
- `build-file-layout` is a pure builder for the 3-pane local world.
- But that world is reconstructed separately for:
  - text extraction
  - rect/shadow extraction
  - mouse hit-testing

Why this is not fully the Electric way:
- A single local world exists conceptually, but it is not carried through the graph as one shared derived object.
- Instead, each subsystem rebuilds it from source atoms.
- Some duplication is natural because text and rect extraction are different projections.
- The stronger smell is that interaction also rebuilds from atoms instead of consuming the same already-derived world.

Electric reading:
- This is close to "one ontology, many projections," but the projections are not yet sharing one authoritative intermediate world.

## Finding 7: Some Event-Side Reconstructions Use Placeholder Inputs

Area:
- `mouse.cljs:318-320`
- `mouse.cljs:464-466`

Diagnosis:
- `build-intake-tree` is rebuilt for hit-testing with placeholder values:
  - `detail-scroll-y nil`
  - `font-size 0`
  - `char-advance 0`
  - `drag-state nil`

Why this is a strong smell:
- The interaction branch is not derived from the same parameters as the render branch.
- It is intentionally constructing a simplified alternate world.
- That may be acceptable for rough hit regions, but it is not the same local world the renderer inhabits.

Electric reading:
- This is not "one world, many projections."
- It is closer to "two separately computed worlds that happen to be similar."

## Finding 8: There Are Undeclared Reactive Dependencies

Area:
- `src/app/client/workspace/cmd_panel.cljs:173`

Diagnosis:
- `<cmd-panel-rects>` dereferences `@!ai-provider` inside the derivation body.
- But `!ai-provider` is not part of the watched input set at `cmd_panel.cljs:189-200`.

Why this is not the Electric way:
- The graph does not fully declare its dependency edge.
- The derivation depends on a source that is not visible in the `m/latest` input list.
- That weakens the "the graph is the architecture" property.

## Finding 9: Some Interaction Paths Bypass Existing Derived State

Area:
- `mouse.cljs:217-259`

Diagnosis:
- `handle-editor-click!` recomputes fold regions directly with `detect-folds-fn`.
- The render path already has `<fold-state>` as a scoped derivation in `editor_compute.cljs:128-135`.

Why this matters:
- The codebase already has a derived fold model, but the click path does not consume that model.
- This is another sign that render and interaction are not yet sharing one authoritative local world.

## Finding 10: The Event Source Layer Is Mostly Healthy

Area:
- `src/app/client/workspace/events.cljs:10-219`
- `src/app/client/workspace/runtime.cljs:55-106`

Diagnosis:
- The cleanest Missionary code in the codebase currently lives in `events.cljs`.
- DOM inputs are modeled as discrete flows with `m/observe`.
- `>wheel` and `>keyboard` use `m/relieve` appropriately to coalesce pressure.
- Focus routing uses `m/eduction` over the keyboard flow rather than storing routed events.

Why this matters:
- The main architectural problem is not "the source layer is imperative."
- The event layer is already much closer to the Electric way than the stateful consumers downstream of it.

Important nuance:
- `events.cljs:175-219` dereferences `!focus` directly inside routing filters.
- That would normally look like an undeclared edge, but here it is an explicit escape hatch to avoid cancellation behavior from `m/watch`.
- This is better read as a deliberate Missionary tradeoff than as a hidden reactivity bug.

## Finding 11: Runtime State Is A Shared Atom Lake

Area:
- `src/app/client/workspace/runtime/state.cljs:26-152`
- writers spread across `scroll.cljs`, `mouse.cljs`, `keyboard.cljs`, `dg_flow.cljs`, `agent_flow.cljs`, `interop.cljs`, `sidebar_io.cljs`, `jit.cljs`

Diagnosis:
- `make-runtime-state` centralizes almost the entire client world as one large map of atoms.
- That is convenient, but it means single ownership is no longer enforced by topology.
- Multiple important atoms have several upstream writers:
  - `!scroll-y`
    - `dg_flow.cljs:192,220,272,335,366`
    - `scroll.cljs:96,116`
    - `keyboard.cljs:118,174`
    - `mouse.cljs:299`
  - `!agent-output`
    - `keyboard.cljs:60`
    - `agent_flow.cljs:52-274`
    - `interop.cljs:49-124`
  - `!extract-preview`
    - `jit.cljs:205,218,224`
    - `interop.cljs:145,155`
  - `!current-file`
    - `sidebar_io.cljs:59,88`
    - `mouse.cljs:180`

Why this is not the Electric way:
- Atoms stop being lakes with one upstream producer and become shared mutable surfaces.
- The graph no longer tells you who owns a value.
- Correctness depends on social discipline across modules rather than on the structure of the flow graph.

Important nuance:
- Not every UI atom must become a pure derivation.
- But the current pattern is broad enough that ownership itself is now a core architectural issue, not just an implementation detail.

## Finding 12: Agent Trail State Has Multiple Reducers And Duplicate Semantics

Area:
- `src/app/client/workspace/runtime/agent_flow.cljs:20-304`
- `src/app/client/workspace/runtime/interop.cljs:25-129`
- `src/app/server_jetty.clj:546-765`

Diagnosis:
- Live agent streaming and dev replay both interpret the same event vocabulary and both mutate `!agent-output` imperatively.
- The event-to-trail reducer logic is duplicated across:
  - `agent_flow.cljs:46-144`
  - `interop.cljs:44-129`
- Both paths also own `!agent-scroll-y`.

Why this is not the Electric way:
- The trail artifact is not modeled as one derived object from one preserved event stream.
- Instead, multiple consumers rebuild the same semantic state by hand with parallel `case` statements.
- This is a strong example of "one concept, multiple owners."

Important nuance:
- The server streaming parser in `server_jetty.clj:546-765` is actually relatively disciplined: it normalizes raw lines into invariant-checked events.
- The duplication happens after that, in client-side state accumulation.

## Finding 13: Electric Is Mostly Being Used As A Bootstrap Shell

Area:
- `src/app/electric_flow.cljc:397-506`
- `src/app/client/workspace/runtime.cljs:25-106`

Diagnosis:
- `LoadWebGPU` and `Prepare-Geometry` are legitimate Electric entrypoints.
- But `main` quickly creates plain CLJS atoms for sidebar/file-load state, computes initial layout eagerly, captures DOM nodes via `reset!`, and then hands the world to `start-loop!`.
- From there, the app is mostly a Missionary + atoms runtime mounted inside an Electric shell.

Why this matters:
- If the question is "what in this codebase is not the Electric way?", this is one of the deepest answers.
- Electric is not yet the language in which the application world is modeled.
- Electric is primarily bootstrapping and siting an already-constructed client runtime.

Important nuance:
- This is not automatically wrong.
- It is an architectural choice.
- But it explains why many app-level questions are answered in Missionary/atom terms instead of Electric expression terms.

## Finding 14: Sidebar And File I/O Are Acceptable Boundary Code, But Still Bypass Flow Ownership

Area:
- `src/app/client/workspace/runtime/sidebar_io.cljs:6-105`
- `src/app/file_viewer.cljc:95-102`
- `src/app/server_jetty.clj:909-923`

Diagnosis:
- `file_viewer.cljc` is a thin Electric bridge over server file I/O, which is clean enough.
- But on the client side, sidebar/file loading is callback-driven mutation:
  - fetch response arrives
  - `!sidebar-state`, `!current-file`, `!scroll-x`, and `!file-load-request` are reset directly

Why this is not the Electric way:
- The server response is not reified as a source flow with one reducer.
- Instead, imperative callbacks directly patch shared atoms.
- This is boundary code, but it still participates in the same ownership blurring as the rest of the runtime.

Important nuance:
- Because this is boundary code, it is less severe than hidden mutation inside a derivation.
- The smell here is not impurity in a pure builder.
- The smell is that the boundary writes directly into shared state instead of into a clearly-owned ingestion path.

## Finding 15: Many Non-Workspace Files Are Actually Fine Because They Are Pure Helpers Or True Terminals

Area:
- `src/components/*.cljc`
- `src/app/server/review_pack.clj`
- `src/app/server_jetty.clj`
- `src/app/file_viewer.cljc`
- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/workspace/text_input.cljs`
- `src/app/client/workspace/trail.cljs`
- `src/app/client/workspace/rect_tree.cljs`

Diagnosis:
- A large portion of the tree is not "anti-Electric"; it is simply not the layer where Electric questions meaningfully apply.
- Components:
  - `adapter.cljc`, `compiler.cljc`, `css_parsers.cljc`, `token_matcher.cljc`, `design_tokens.cljc`
  - these are mostly pure transforms
- Server:
  - `server_jetty.clj` is a Ring / HTTP / SSE terminal boundary
  - `review_pack.clj` is mostly pure domain logic with one small atom-backed store
  - `file_viewer.cljc` is a thin client/server bridge
- Rendering substrate:
  - `renderer.cljs` is mostly shader/data preparation and draw helpers
- Text/layout helpers:
  - `text_input.cljs`, `trail.cljs`, `rect_tree.cljs` are mostly pure projection logic

Why note this:
- A codebase-wide Electric audit should avoid flattening everything into "bad reactivity."
- The current problems are concentrated in the client workspace runtime where sources, derivations, and mutable UI state meet.

## Finding 16: The Rama Utility Layer Uses Reactive Pieces, But Its Shape Is Mostly Global Service State

Area:
- `src/app/server/rama/util_fns.cljc:25-297`
- `src/app/server/rama/core.clj`
- `src/app/server/rama/objects.cljc`

Diagnosis:
- `util_fns.cljc` has one genuinely reactive function: `!subscribe` at `util_fns.cljc:107-119`.
- But the dominant shape is top-level process state:
  - `!rama-ipc`
  - top-level `ipc` creation and module launch
  - many top-level foreign handles bound from that IPC instance
- Most functions are imperative wrappers around append/select/query operations.

Why this is not especially Electric:
- Startup and ownership are global rather than explicit in a local graph.
- The layer is reactive in spirit only at the subscription edge; most of it is service plumbing.

Important nuance:
- This code should mostly be judged against Rama/event-sourcing idioms, not against Electric DOM idioms.
- So this is less a "bug" and more a reminder that the project has several paradigms living together.

## Finding 17: `global_flow.cljs` Is Far From The Current Electric Way, But Appears Historical

Area:
- `src/global_flow.cljs:10-87`
- `src/app/client/workspace/runtime/mouse.cljs.bak`

Diagnosis:
- `global_flow.cljs` contains debug-printing signals, many global atoms, and exploratory flow fragments.
- `mouse.cljs.bak` appears to be an archived predecessor rather than active runtime code.

Why this matters:
- If someone asks "what file is most obviously not the Electric way?", `global_flow.cljs` is a strong answer.
- But it should not be over-weighted because it does not appear to be the active architectural center anymore.

## Blockers To Making The Whole System FRP

These are the main architectural blockers.

### 1. Shared State Has Multiple Upstream Writers

Evidence:
- `runtime/state.cljs:26-152`
- writers spread across `scroll.cljs`, `mouse.cljs`, `keyboard.cljs`, `dg_flow.cljs`, `agent_flow.cljs`, `interop.cljs`, `sidebar_io.cljs`, and `jit.cljs`

Why this blocks FRP:
- Important values are not owned by one explicit upstream reducer or derivation.
- Several atoms act as shared mutable meeting points.
- The graph cannot fully explain causality because you must know which modules also write the same atom.

Most important examples:
- `!scroll-y`
- `!agent-output`
- `!extract-preview`
- `!current-file`

### 2. Local Worlds Are Not First-Class Shared Values

Evidence:
- `mouse.cljs:159-198`
- `mouse.cljs:261-311`
- `mouse.cljs:313-334`
- `mouse.cljs:423-470`
- `shell.cljs:8-279`
- `dg_flow.cljs:679-840`

Why this blocks FRP:
- Sidebar, file-layout, and intake worlds are often rebuilt separately for render, text extraction, hit-testing, and interaction.
- There is not yet one authoritative derived world object that all projections consume.

Consequence:
- render and interaction do not clearly inhabit the same value
- the system keeps falling back to imperative reconstruction

### 3. Event Consumers Still Contain Domain Logic, Derivation, And Mutation Together

Evidence:
- `runtime/mouse.cljs:217-520`
- `runtime/keyboard.cljs:19-406`
- `runtime/scroll.cljs:10-126`

Why this blocks FRP:
- Consumers inspect atoms, compute derived structure, choose transitions, and mutate multiple atoms in one place.
- Source, derivation, and sink are still braided together inside event-handling code.

Consequence:
- state transitions are spread across many consumers instead of being represented as one explicit reactive model

### 4. Electric Does Not Yet Host The Main Application Semantics

Evidence:
- `electric_flow.cljc:432-506`
- `runtime.cljs:25-106`

Why this blocks FRP:
- Electric mostly boots resources, allocates a few atoms, mounts the canvas, and hands control to the Missionary runtime.
- The main application world is not yet primarily expressed as Electric lexical reactive structure.

Consequence:
- the system is philosophically Electric
- but operationally the app is still mostly a Missionary + atoms runtime

### 5. Boundary Ingestion Writes Directly Into Shared App State

Evidence:
- `runtime/sidebar_io.cljs:37-105`
- `runtime/interop.cljs:131-156`
- `jit.cljs:196-224`

Why this blocks FRP:
- File fetches, extractor results, and preview injections patch shared atoms directly.
- They do not flow through one clear source-flow-to-model path.

Consequence:
- ingestion semantics stay ad hoc
- ownership and temporal meaning are implicit

### 6. Some Core Artifacts Have More Than One Reducer

Evidence:
- `runtime/agent_flow.cljs:46-144`
- `runtime/interop.cljs:44-129`

Why this blocks FRP:
- Live agent streaming and replay both interpret the same event vocabulary and both rebuild `!agent-output`.
- One artifact is being accumulated in more than one place.

Consequence:
- duplicated semantics
- drift risk
- no canonical accumulation path

### 7. Scroll State Is Not Yet One Coherent Subsystem

Evidence:
- `runtime/scroll.cljs:10-126`
- `runtime/keyboard.cljs:98-175`
- `runtime/mouse.cljs:293-301`
- `dg_flow.cljs:547-560`

Why this blocks FRP:
- Scroll values are driven by wheel routing, keyboard navigation, click navigation, and local projection clamps.
- The scroll family does not yet behave like one semantically owned modeled subsystem.

Important atoms:
- `!scroll-y`
- `!chat-scroll-y`
- `!run-scroll-y`
- `!detail-scroll-y`
- `!agent-scroll-y`

### 8. The Codebase Still Mixes Paradigms Without Sharp Interior Boundaries

Evidence:
- Electric shell in `electric_flow.cljc`
- Missionary event/derive code in `events.cljs`, `combined_text.cljs`, `editor_compute.cljs`
- imperative consumer modules in `runtime/*`
- callback-driven boundary code in several I/O modules

Why this blocks FRP:
- Hybrid systems are fine when imperative code stays clearly terminal.
- In the current codebase, imperative logic still lives in the middle of application semantics, not only at the edge.

## What The Current Paradigm Actually Is

If stated positively, the current system is:
- a `reactive hybrid`
- more specifically: `Missionary/Electric-inspired FRP projections mounted inside an event-loop runtime with shared mutable UI state`

That is the paradigm it currently inhabits.

## Current Summary

High confidence:
- The audit has now covered the active codebase broadly, excluding only the secret-bearing `env.clj`.
- The render hub has improved materially; the older "one monolithic `m/latest`" reading is now outdated.
- The strongest current issue is duplicated world-model reconstruction across render and interaction.
- The second major issue is atom ownership: several important client atoms have multiple upstream writers.
- The third issue is architectural altitude: Electric is mostly acting as a bootstrap shell around a Missionary + atoms runtime.
- The main blockers to becoming fully FRP are now clear:
  - shared multi-owner state
  - no first-class authoritative world objects
  - event consumers that still own domain logic
  - Electric not yet hosting the main application semantics
- The previous `build-right-detail` impurity has already been removed.
- Smaller but real issues remain around detail scroll ownership, undeclared derefs, and duplicated agent-state reducers.

## Next Discovery Passes

1. Build an explicit ownership matrix for the highest-pressure atoms:
   - `!scroll-y`
   - `!agent-output`
   - `!extract-preview`
   - `!current-file`
   - `!flow-state`
2. Trace whether sidebar, file-view, and intake can each be named as one authoritative local world shared by render and hit-test.
3. Audit the scroll family as one semantic cluster:
   - `!scroll-y`
   - `!chat-scroll-y`
   - `!run-scroll-y`
   - `!detail-scroll-y`
   - `!agent-scroll-y`
4. Audit whether command/provider and flow-session state have any remaining undeclared edges or hidden multi-owner updates.
