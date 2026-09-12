# Softland Project Thread Map

## 1. Master Thread Graph

```text
TIME ──────────────────────────────────────────────────────────────────────────────────────────>

Editor Core [ACTIVE] ────┬─ Reactive Refactor [DONE] ── MSDF/Themes [DONE] ── CPU Opt [DONE]
                         │                                                      │
Sidebar [ACTIVE] ────────┼─ DOM Sidebar [DONE] ──────── WebGPU Sidebar [DONE] ─ Sidebar Simplified [DONE]
                         │                                                      │
                         │                                                      └─ 3-Pane Layout [DONE] ──┐
                         │                                                                                │
Agent Runtime [DONE] ────┼─ Rama Loop/Resume [DONE] ─── SSE Streaming [DONE] ─┬─ Reasoning Trails [ACTIVE]│
                         │                                                    └─ Structured Output [DONE] │
                         │                                                                                │
                         └─ Prompt-Driven UI [ACTIVE] ─ V0 Flow State [DONE] ── Direct Linear API [DONE]  │
                                                                                │                         │
Rect Tree UI [DONE] ─────── SDF Quads [DONE] ────────── Layout Engine [DONE] ───┼─ Screen 1 Intake [DONE] │
                                                                                ├─ Screen 2 Arrange [BLOCKED]
                                                                                │                         │
Design Converter [DONE] ─── Browser Pipeline [DONE] ─── JIT Component Lib [DEFERRED]                      │
                                                                                                          │
                                                                        UI Excellence (Ph 1-2) [DONE] ────┘
                                                                        UI Excellence (Ph 3-4) [ACTIVE] 
```

## 2. Thread Detail Cards

**Editor Core**
- **Origin**: Sessions 1-7 (Caret, Text Editing, SCI)
- **Depends on**: None
- **Feeds into**: Rect Tree UI, 3-Pane Layout
- **Key artifacts**: `loop.cljs`, `editor.cljs`, `electric_flow.cljc`
- **Status**: `ACTIVE`. Experienced a major "Great Refactor" in Session 8 to Missionary reactive flows (`m/latest`). Continues to receive optimizations (e.g., Session 17 CPU opt) and UI tuning.

**Sidebar**
- **Origin**: Session 16 (DOM-based file explorer)
- **Depends on**: Editor Core
- **Feeds into**: 3-Pane Layout, UI Excellence Pass
- **Key artifacts**: `loop.cljs` (`build-sidebar-tree`)
- **Status**: `ACTIVE`. Migrated from imperative DOM to WebGPU Rect Tree in Session 26. Radically simplified in Session 28 (tabs removed, file-explorer only) to clear the path for the 3-pane default layout.

**Agent Runtime / Streaming**
- **Origin**: Sessions 18, 19, 21
- **Depends on**: Editor Core
- **Feeds into**: Reasoning Trails, Prompt-Driven UI
- **Key artifacts**: `server_jetty.clj` (SSE streaming), `objects.cljc`, `$$cli-sessions-pstate`
- **Status**: `DONE`. Replaced a disconnected HTTP pipe with a Rama-native event log enforcing `--resume` continuity across sessions. Handles structured output via `--json-schema` (Session 21).

**Reasoning Trails**
- **Origin**: `claude-3-moonshot.md`, `design-brief-reasoning-trails.md`
- **Depends on**: Agent Runtime
- **Feeds into**: Screen 4/5 (Review)
- **Key artifacts**: Structured events via stream parser (tool-use blocks)
- **Status**: `ACTIVE` (`where-we-are.md` lists it in Lane B / Spike). The hypothesis is that capturing `tool_use` alongside reasoning text into structured visual nodes makes review significantly better than reading diffs.

**Prompt-Driven UI**
- **Origin**: `prompt-driven-ui-gemini.md`, `commission-consensus.md`
- **Depends on**: Agent Runtime
- **Feeds into**: Master-Detail Screens (1-5)
- **Key artifacts**: `/bootstrap` command, `!flow-state`, `fire-flow-run!`
- **Status**: `ACTIVE`. The architectural pattern turning UI interactions into hidden Claude CLI prompts. Scoped specifically to the `discourse-graph` daily workflow for V0.

**Rect Tree UI & Layout Engine**
- **Origin**: `rect-tree-ui.md`, `virtual-layout-engine.md` (Sessions 23-25)
- **Depends on**: Editor Core
- **Feeds into**: Screen 1 Intake, Sidebar, Composable Components
- **Key artifacts**: `rt-node`, `layout-children`, `tree->rects`, `dt` tokens
- **Status**: `DONE`. Replaced flat coordinate calculations with a hierarchical scene graph, SDF shaders (rounded corners, borders), and a flexbox-equivalent layout engine.

**Screen 1: Intake (Master-Detail)**
- **Origin**: `screen-1-spec.md` (Session 22)
- **Depends on**: Prompt-Driven UI, Rect Tree UI, Layout Engine
- **Feeds into**: Screen 2 Arrange
- **Key artifacts**: `loop.cljs` (`build-intake-tree`), `fetch-linear-issues`
- **Status**: `DONE`. Implemented using the new Composable Panel components. Bootstraps via a direct Linear API call (bypassing Claude CLI for speed). Pending acceptance gate to move to Screen 2.

**Screen 2: Arrange**
- **Origin**: `commission-consensus.md`, `ui-mockups-master-detail.md`
- **Depends on**: Screen 1 Intake
- **Feeds into**: Screen 3 Run
- **Status**: `BLOCKED`. Awaiting Screen 1 acceptance gate (keyboard navigation validation, regression checks).

**Design Converter Pipeline**
- **Origin**: `design-converter-ux.md` (Session 27)
- **Depends on**: Layout Engine
- **Feeds into**: JIT Component Library
- **Key artifacts**: `components/_extractor.js`, `_css_parsers.cljc`, `_compiler.cljc`, `_design_ir.cljc`
- **Status**: `DONE`. A multi-LLM (Claude, Codex, Gemini) effort that built a deterministic extraction pipeline turning live browser CSS into a canonical Design IR, and finally into `rt-node` trees.

**JIT Component Library**
- **Origin**: `component-library-jit.md` (Session 30)
- **Depends on**: Design Converter
- **Feeds into**: UI Excellence Phase 3
- **Key artifacts**: `components/` directory structure, `_registry.edn`, `shadcn-v4.edn` token map
- **Status**: `DEFERRED`. After finding browser extraction too slow (~20m/component), the project pivoted to JIT generation via Claude reading source URLs directly. The UI for this was temporarily removed in the Session 28 sidebar simplification, but is slated to return in UI Excellence Phase 3.

**UI Excellence Pass**
- **Origin**: `ui-excellence-spec.md` (Session 29)
- **Depends on**: Layout Engine, 3-Pane Layout
- **Feeds into**: Final workspace polish
- **Key artifacts**: `!active-pane`, `!scroll-x`, typography, surface elevation tokens
- **Status**: `ACTIVE`. Phase 1 (Rendering Integrity) and Phase 2 (Space & Depth) are complete. Phase 3 (Context-Aware Features, including component details) is the immediate next step.

## 3. Decision Log

- **Session 1-7**: Decided to build a reactive WebGPU editor from scratch using Clojure/Electric. Chose vector-of-strings for data model. Chose SCI for in-browser intelligence over LSP to avoid server roundtrips.
- **Session 8**: Decided to abandon imperative updates for a strictly reactive architecture (`m/latest`) to prevent state desync.
- **Session 15**: Decided to move theme coloring to the GPU instance data (12 floats/glyph) for live Zed-style theme switching without re-parsing.
- **Session 16b / 17**: Decided to isolate visual tick logic from heavy document-level logic. Switched to `identical?` dirty checking to optimize idle CPU from 76% to 6%.
- **Session 18**: Architecture Audit. Decided to close the loop on Agent connections, wiring the HTTP API directly into Rama `$$cli-sessions-pstate` to gain true `--resume` continuity.
- **Session 20 (Feb 18)**: Locked the "Prompt-Driven UI" model via multi-LLM consensus. UI interactions map to CLI commands instead of custom backend orchestrators.
- **Session 22 (Feb 25)**: Decided to bypass the Claude CLI for the initial workspace bootstrap. Directly hitting the Linear API took 2s vs the CLI's 300s.
- **Session 23-25 (Feb 25-27)**: Decided flat coordinate math wouldn't scale for complex UIs. Introduced the `rt-node` scene graph and a flexbox-like Layout Engine.
- **Session 26 (Feb 27)**: Decided to kill the DOM-based sidebar and rebuild it purely in WebGPU for visual consistency and single-event-loop routing.
- **Session 27 (Mar 1)**: Decided to build the Design Converter with an Intermediate Representation (`Design IR`) so both LLM-driven and browser-extracted components output identical data.
- **Session 28 (Mar 2)**: Decided to brutally simplify the sidebar (removing component/review tabs) and generalize the 3-pane view (Editor|Chat|Preview) for *all* files, not just component files.
- **Session 30 (Mar 1)**: Live extraction experiment proved the browser DOM crawler was too slow. Decided to pivot to "JIT Component Library" where 40+ stubs exist, and clicking one triggers an LLM session to convert it.

## 4. Status Conflicts Explained

**Conflict 1: The Status of the Component Library**
- **Docs say:** `_map.md` and `where-we-are.md` label the Component Library as "Archive" (noting its files exist but UI integration was removed in Session 28).
- **Docs say:** `component-library-jit.md` (Session 30) explicitly calls itself the "APPROVED SOLUTION".
- **Docs say:** `ui-excellence-spec.md` and `next-prompt.md` list building `build-component-detail` and detecting `_source.edn` as active TODOs for Phase 3.
- **Resolution:** The component library is **DEFERRED**, not archived. The AI maintaining the map likely labeled it "archive" because the user violently ripped out its sidebar UI hooks during the Session 28 simplification pass. However, the explicit spec in `ui-excellence-spec.md` proves it is scheduled to be reintegrated contextually (via `file-content-type`) during Phase 3. It was delayed purely to fix core workspace aesthetics first.

**Conflict 2: Design Converter Extraction Modality**
- **Docs say:** `consensus-design-converter.md` emphasizes a multi-path input, investing heavily in a browser DOM walker (`_extractor.js`).
- **Docs say:** `component-library-jit.md` abandons browser-extraction-first in favor of giving Claude a source URL and having it output the Clojure file.
- **Resolution:** Both exist as infrastructure, but the *primary workflow* pivoted. The browser extraction code (`_extractor.js`, `_css_parsers.cljc`) was built and works (Session 27), but during the live extraction experiment (Session 30) it took ~20 minutes to reverse-engineer shadcn's hover states. Thus, the project pivoted the *user experience* to JIT generation via Claude reading source URLs directly. The browser pipeline remains active for visual verification (split view comparison), but it is no longer the primary ingest path.