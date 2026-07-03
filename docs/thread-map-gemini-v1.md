# Softland Project Thread Map v1 (Synthesized)

> **Date:** 2026-03-03
> **Purpose:** A comprehensive, synthesized visual map of all development threads, dependencies, and component statuses.
> **Source:** Synthesized from previous Claude, Codex, and Gemini mappings to provide the absolute best, most accurate visualization of the Softland architecture and timeline.

---

## 1. Component-Level Visual Maps & State

The project has evolved into five distinct subsystems. Below are the dependency trees and current execution states for each component.

**Legend:** 
`[████] DONE (Shipped)` | `[▓▓░░] ACTIVE (In Progress)` | `[░░░░] DEFERRED / BLOCKED`

### 1.1 Editor Core & WebGPU Rendering
The foundational graphics and text editing layer. Completely client-side, powered by Lezer (parsing) and SCI (eval).

```text
┌───────────────────────────────────────────────────────────────────────────────────┐
│ WebGPU Text Pipeline                                                              │
│  (S1-S7) Editor Foundations (Caret, Typing, SCI, Lezer) ━━━━━━━━━━━━━━━━ [▓▓░░]   │
│         │                                                                         │
│         ├───> (S11-S14) Text Polish (MSDF, Px Snap, Crispness) ━━━━━━━━━ [████]   │
│         │                                                                         │
│         └───> (S15) Theme Engine (Zed-style, per-glyph color) ━━━━━━━━━━ [████]   │
│                                                                                   │
│ Scene Graph & GPU Layout                                                          │
│  (S23) Rect Tree UI (Scene Graph, Hit-test, DnD) ━━━━━━━━━━━━━━━━━━━━━━━ [████]   │
│         │                                                                         │
│         ├───> (S24) SDF Quads & Shadows (Rounded corners) ━━━━━━━━━━━━━━ [████]   │
│         │                                                                         │
│         └───> (S25) Virtual Layout Engine (Flexbox model) ━━━━━━━━━━━━━━ [████]   │
│                 │                                                                 │
│                 └───> (S29) UI Excellence Ph 1-2 (Depth, Focus, Scroll)  [████]   │
│                         │                                                         │
│                         └───> (S30+) UI Excellence Ph 3-4 (Component UX) [▓▓░░]   │
└───────────────────────────────────────────────────────────────────────────────────┘
```
**Current State:** Highly mature. The migration from flat layout math to the `rt-node` hierarchical tree was a massive success, unlocking the UI Excellence visual pass.

### 1.2 Reactive Architecture & Backend State (Electric/Rama)
The reactive dataflow and event-sourcing backend.

```text
┌───────────────────────────────────────────────────────────────────────────────────┐
│ Reactive Core (Client)                                                            │
│  (S8) The Great Refactor (Imperative → m/latest / Missionary) ━━━━━━━━━━ [████]   │
│         │                                                                         │
│         ├───> (S10) m/ap Crash Fix (Established anti-patterns) ━━━━━━━━━ [████]   │
│         │                                                                         │
│         └───> (S16b-17) CPU Optimization (Identical? diffing, split flow)[████]   │
│                                                                                   │
│ Event-Sourcing (Rama Backend)                                                     │
│  (S18) Architecture Audit (Wire HTTP into Rama PStates) ━━━━━━━━━━━━━━━━ [████]   │
│         │                                                                         │
│         ├───> $$cli-sessions-pstate (Session ID & --resume continuity) ━ [████]   │
│         │                                                                         │
│         └───> *node-events-depot (Event sourcing for interaction) ━━━━━━ [▓▓░░]   │
└───────────────────────────────────────────────────────────────────────────────────┘
```
**Current State:** Solidified. The CPU optimization (idle frame skipping) and Missionary `m/latest` guarantees mean the reactive loop is stable and performant.

### 1.3 Agent Runtime & LLM CLI Integration
The infrastructure that streams Claude's CLI output and parses it into structured data.

```text
┌───────────────────────────────────────────────────────────────────────────────────┐
│ Streaming Infrastructure                                                          │
│  (S19) SSE Streaming (Token-level stream, 8 canonical events) ━━━━━━━━━━ [████]   │
│         │                                                                         │
│         ├───> (S21) Structured Output Pipeline (--json-schema) ━━━━━━━━━ [████]   │
│         │                                                                         │
│         └───> (S20) Multi-Provider Support (Claude, Codex, Gemini) ━━━━━ [████]   │
│                                                                                   │
│ CLI Control Plane                                                                 │
│  (S20) Slash Commands (/bootstrap, /arrange, /run-flow, etc) ━━━━━━━━━━━ [████]   │
│         │                                                                         │
│         └───> (S22) Direct API Pivot (Linear GraphQL bypasses CLI) ━━━━━ [████]   │
└───────────────────────────────────────────────────────────────────────────────────┘
```
**Current State:** Working perfectly. The pivot to bypass the CLI for the initial workspace fetch (S22) saved massive overhead (300s -> 2s).

### 1.4 Prompt-Driven UI & UX Workflows
The product layer. UI actions function as a remote control for background LLM CLI prompts.

```text
┌───────────────────────────────────────────────────────────────────────────────────┐
│ Prompt-Driven UI (Discourse Graph Context)                                        │
│  (S20) Commission Consensus (Locked 5-Screen Master-Detail UX) ━━━━━━━━━ [▓▓░░]   │
│         │                                                                         │
│         ├───> (S21-22) Screen 1: Intake (List, Status, Select) ━━━━━━━━━ [▓▓░░]   │
│         │       │        (Blocked by Acceptance Gate: Keyboard Nav)               │
│         │       │                                                                 │
│         │       └───> Screen 2: Arrange (Sequential vs Parallel) ━━━━━━━ [░░░░]   │
│         │               │                                                         │
│         │               └───> Screens 3-5: Run & Review ━━━━━━━━━━━━━━━━ [░░░░]   │
│         │                                                                         │
│         └───> (S28) 3-Pane File Layout (Editor | Chat | Preview) ━━━━━━━ [████]   │
│                 │                                                                 │
│                 └───> (S28) Simplified Sidebar (File Explorer only) ━━━━ [████]   │
│                                                                                   │
│ Reasoning Trails (The North Star Vision)                                          │
│  (Feb 13) Design Brief (Tool-use stream = first-class UX artifact) ═════ [▓▓░░]   │
│         │                                                                         │
│         └───> Structured Trail Rendering (Agent Panel vis-spike) ━━━━━━━ [░░░░]   │
└───────────────────────────────────────────────────────────────────────────────────┘
```
**Current State:** Screen 1 (Intake) is built but awaiting its final acceptance gate. The Reasoning Trails feature remains a spike with partial rendering support, awaiting further integration.

### 1.5 Design Converter & Component Library
The pipeline to ingest external UI and render it natively.

```text
┌───────────────────────────────────────────────────────────────────────────────────┐
│ Extraction Pipeline                                                               │
│  (S27) Design Converter (Multi-LLM CSS -> Token Matcher -> Design IR) ━━ [████]   │
│         │                                                                         │
│         └───> (S28/30) JIT Component Library Pivot ━━━━━━━━━━━━━━━━━━━━━ [░░░░]   │
│                 (Claude parses URL directly; browser extract too slow)            │
│                 │                                                                 │
│                 └───> Context-Aware Reintegration (UI Excellence Ph 3) ━ [▓▓░░]   │
└───────────────────────────────────────────────────────────────────────────────────┘
```
**Current State:** Pipeline infrastructure is built and proven, but the UX was ripped out of the sidebar during simplification (S28). The pivot to "JIT Components" is awaiting its UI reintegration in the upcoming Phase 3 Excellence pass.

---

## 2. Synthesized Master Timeline Flow

```text
SESSION    TRACK            STATUS     KEY EVENT
──────────────────────────────────────────────────────────────────────────────────────────
 1-7       Editor           ████       Caret, Data Model, Editing, SCI, Initial Canvas
 8         Architecture     ████       [GREAT REFACTOR] Imperative UI -> Missionary flows
 10-15     Editor           ████       m/ap crash fix, Font settings, MSDF crispness, Zed Themes
──────────────────────────────────────────────────────────────────────────────────────────
 Feb 6     Vision           ════       THE QUESTION: "How do we put AI inside it?"
                                       -> Claude: Moonshot (Rama events, Continuous Zoom)
                                       -> Codex: Hybrid Pragmatic path
                                       -> Gemini: Threaded Engineering
──────────────────────────────────────────────────────────────────────────────────────────
 16        Sidebar          ████       v1: Imperative DOM File Sidebar
 16b-17    Architecture     ████       CPU Optimization (76% -> 6%) via object diffing
 18        Agent Runtime    ████       Rama Audit: Wired HTTP into $$cli-sessions-pstate
 19        Agent Runtime    ████       SSE Streaming, 8 Event Types
──────────────────────────────────────────────────────────────────────────────────────────
 Feb 18    Vision/Planning  ════       COMMISSION CONSENSUS (3 LLMs + User)
 20        UI/UX Workflow   ████       Flow State Machine (9 slash commands), Prompt-Driven UI
──────────────────────────────────────────────────────────────────────────────────────────
 21-22     UI/UX Workflow   ▓▓░░       Screen 1 Intake, Direct Linear API Pivot
 23        Graphics Engine  ████       Rect Tree UI (Scene Graph, DnD, Hit-Testing)
 24-25     Graphics Engine  ████       SDF Quads, Virtual Layout Engine, Composable Panels
 26        Sidebar          ████       v2: WebGPU-native Sidebar migration
 27        Component Lib    ████       Design Converter Pipeline (Multi-LLM extraction)
──────────────────────────────────────────────────────────────────────────────────────────
 28        UI Architecture  ████       3-Pane Layout generalized; Sidebar v3 (Simplified)
 29        Graphics Engine  ████       UI Excellence Pass Ph 1-2 (Surfaces, Focus, H-Scroll)
 30        Component Lib    ░░░░       JIT Component Library Pivot (Pending UI integration)
──────────────────────────────────────────────────────────────────────────────────────────
```

---

## 3. The Big Picture: Architecture Flow

How the system routes intent through the UI and Backend seamlessly:

```text
[ HUMAN INTENT ]                         [ COMPUTE & STATE ]                     [ WEBGPU RENDER ]
Keyboard/Mouse  ────────────────────┐   Missionary Reactor  ────────────────┐   WebGPU Canvas
(DOM Events routed to canvas)       │   (<editor-rects, <text-ops)          │   (SDF Quads, MSDF)
                                    ▼                                       ▼
                             ┌──────────────┐                       ┌───────────────┐
                             │  State Atoms │◄── identical? diff ───┤  Rect Tree    │
                             │ (!editor-doc,│                       │  Scene Graph  │
                             │ !flow-state) │                       └───────────────┘
                             └──────┬───────┘                               ▲
                                    │                                       │
[ AI / SYSTEM INTENT ]              │                                       │
Server Jetty (Ring) ◄─── SSE ───────┤                           [ LAYOUT ENGINE ]
(Agent CLI Stream)                  │                           resolve-layout
                                    │                           normalize-padding
$$cli-sessions-pstate ◄─── watch ───┘                                       ▲
(--resume, conversation history)                                            │
                                                              [ UI COMPONENTS ]
                                                              ui-panel, ui-list-item
```

---

## 4. Synthesized Rulings on Conflicting Docs

After reviewing all documentation and LLM analysis, here are the absolute truths:

1. **The Component Library is NOT dead (The "Archive" Mismatch).**
   - *Issue:* `_map.md` calls it archived.
   - *Truth:* It is **DEFERRED**. The UI was aggressively stripped during Session 28's sidebar simplification to clear cognitive load, but the JIT architecture (Session 30) is the locked paradigm. It is scheduled to be wired back in via `file-content-type` checking in UI Excellence Phase 3.

2. **Extraction Methodology (Browser DOM vs URL Context).**
   - *Issue:* Conflicting plans on how components are generated.
   - *Truth:* The browser DOM walker (`_extractor.js`) works but proved too slow for daily driving (~20 mins per component to extract all pseudo-states). The system pivoted to **JIT URL generation** where Claude is fed the component source URL directly. The browser extractor remains purely as a QA verification tool (split-view visual diffs).

3. **Auto-Bootstrap Semantics.**
   - *Issue:* Early specs demanded auto-load of tickets.
   - *Truth:* Auto-bootstrap was disabled in code (`src/app/client/webgpu/loop.cljs:5234`). Initial state is firmly `:idle` and requires an explicit `/bootstrap` slash command to prevent blocking workspace loads.

4. **Screen 1 Status.**
   - *Issue:* Docs claim Screen 1 is implemented, but Screen 2 is blocked.
   - *Truth:* Screen 1's UI is implemented, but the formal *Acceptance Gate* (keyboard navigation regression testing) is pending. Screen 2 is strictly blocked by the Commission Consensus rules until Screen 1 officially passes that gate.