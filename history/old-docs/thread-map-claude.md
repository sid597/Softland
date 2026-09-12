# Softland Project Thread Map

> **Author:** Claude (Opus 4.6), reconstructed from all project docs + session logs
> **Date:** 2026-03-23 (updated 2026-05-13: Rama rename/text split addendum)
> **Purpose:** Single visual document tracing every development thread, decision fork, and dependency across the original 37-session reconstruction, with later addenda for major runtime milestones
> **Read time:** ~15 minutes
> **Honesty note:** Every claim below cites its source. Decisions are tagged `[USER]`, `[AI]`, or `[JOINT]` to distinguish who decided what. Status conflicts between docs are flagged explicitly.
>
> **2026-03-29 addendum:** Sessions around the consensus refactor closed Phases `0-5` and `7` of the workspace substrate correction. The workspace now has semantic local-world derivation, keyed-diff proof, and workspace-truth persistence; Phase `6` differential pipeline is the next major thread.
>
> **2026-05-11 addendum:** The dogfood runtime now has an implemented Rama-backed LLM contract MVP. The completed slice chain covers WorldTurns, frozen ContextBundles, LLM run lifecycle, controls, executor claims, follow-up runs, raw LLM items, catalog/slice/derivative materialization, fork/reconciliation, patch proposals, rebuildable projections, and cost rollups. The implementation record is `docs/current-mental-model/architecture/dogfood-runtime/llm-track-slice-roadmap.md`. At landing, code lived in `src/app/server/rama/dogfood/world.clj` and `src/app/server/rama/dogfood/llm.clj`; after `1ef1cbd`, active dogfood code is `space.clj` plus `llm.clj`.
>
> **2026-05-13 addendum:** Commit `1ef1cbd` performed PR1 as a mechanical rename plus file split, not codegen: the old text/world kernel surface is now shared contracts in `src/app/server/rama/core.clj` plus the text instance in `src/app/server/rama/text_kernel.clj`; the dogfood WorldThread/WorldTurn runtime is now space/turn vocabulary in `src/app/server/rama/dogfood/space.clj`. Active code should say space/turn; old world-thread/world-turn names in earlier docs are historical provenance unless explicitly marked current.

---

## 1. Master Timeline

```
SESSION   DATE        THREAD(S) ACTIVE                                    KEY EVENT
──────────────────────────────────────────────────────────────────────────────────────
 1        pre-Feb     Editor ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  Caret, data model
 2        pre-Feb     Editor ━━━━                                         Editing, SCI research
 3        pre-Feb     Editor ━━━━                                         Brackets, folding
 4        pre-Feb     Editor ━━━━                                         SCI integration
 5        pre-Feb     Editor ━━━━                                         Cut/copy/paste, undo
 6-7      pre-Feb     Editor ━━━━                                         Cmd panel, TextInput
 8        pre-Feb     Editor ━━━━ ReactiveArch ─────                      Great Refactor (multi-LLM)
 9        ---         (skipped)
10        pre-Feb     Editor ━━━━                                         m/ap crash fix (→ CLAUDE.md)
11        pre-Feb     Editor ━━━━                                         Font settings, MSDF
12        pre-Feb     Editor ━━━━                                         Reactive font fix
13-14     pre-Feb     Editor ━━━━                                         Text crispness, pixel snap
15        pre-Feb     Editor ━━━━                                         Theme switching (Zed-style)
──────────────────────────────────────────────────────────────────────────────────────
                      ║
              THE QUESTION: "How do we put AI inside it?" ── Feb 6
                      ║
              ┌───────╨───────────────────┐
              ▼           ▼               ▼
         Claude(×3)    Codex           Gemini
         Moonshot      Hybrid          Threaded Eng.
              │           │               │
              └───────────┼───────────────┘
                          ▼
                     SYNTHESIS ── Feb 6
──────────────────────────────────────────────────────────────────────────────────────
16        Feb ~8      Editor ━━━━ Sidebar(DOM) ────                       File explorer (imperative DOM)
16b       Feb ~8      Editor ━━━━                                         CPU optimization (76%→6%)
17        Feb ~9      Editor ━━━━                                         Large-file perf fix
17b       Feb  9      ──────────── BuildFix ─                             Build unblocked
18        Feb 11      AgentRT ━━━━━━━━━━━━━━━━━━━━━━━━━                   Rama audit, --resume wired
          Feb 12      Vision ════════════════                              Reasoning Trails design brief
          Feb 12      Tier1UI ━━━━━━━━━                                   Agent panel: height, wrap, scroll
          Feb 13      Vision ════                                          Streaming + threads merge
          Feb 13      ReviewPack ━━━━━━━                                  Review pack prototype
          Feb 13      RiskPlan ═══                                         Risk-first plan written
──────────────────────────────────────────────────────────────────────────────────────
19        Feb 13-14   AgentRT ━━━━                                         SSE streaming, 8 event kinds
──────────────────────────────────────────────────────────────────────────────────────
          Feb 13-16   UIVisions ══════                                     10 wild concepts (archive)
──────────────────────────────────────────────────────────────────────────────────────
20        Feb 18      Commission ━━━━━━━━━━━━━━                           Planning commission (3 LLMs)
                      FlowMachine ━━━━━━━━━━━━━━                          9 slash commands, auto-bootstrap
                      MasterDetail ═══════════════                         UI approach LOCKED
──────────────────────────────────────────────────────────────────────────────────────
21        Feb 24      Screen1 ━━━━━━━━━━━━━━━━━━━━━━━                     Structured output, list view
22        Feb 25      Screen1 ━━━━                                         Direct Linear API (bypass CLI)
                      RectTreeDesign ═══                                   Design doc written
──────────────────────────────────────────────────────────────────────────────────────
23        Feb 25      RectTree ━━━━━━━━━━━━━━━━━━━━━━━━                   Scene graph: all 7 steps
──────────────────────────────────────────────────────────────────────────────────────
24        Feb 27      GPUComponents ━━━━━━━━━━━━━━━━━━━━━                 SDF quads, shadows, dt tokens
25        Feb 27      LayoutEngine ━━━━━━━━━━━━━━━━━━━━━                  Layout engine, composable panels
──────────────────────────────────────────────────────────────────────────────────────
26        Feb 27      Sidebar(GPU) ━━━━━━━━━━━━━━━━━━━━━                  DOM→WebGPU sidebar migration
──────────────────────────────────────────────────────────────────────────────────────
27        Mar  1      DesignConverter ━━━━━━━━━━━━━━━━━━━                 Multi-LLM extraction pipeline
──────────────────────────────────────────────────────────────────────────────────────
28        Mar  2      Sidebar(simplify) ━━━━━━━━━                         Tabs removed, file-only
                      3PaneLayout ━━━━━━━━━━━━━━━━━━━━━                   Editor|Chat|Preview
                      TicketGate ━━                                        :idle default, /bootstrap gates
──────────────────────────────────────────────────────────────────────────────────────
29        Mar  2      UIExcellence ━━━━━━━━━━━━━━━━━━━━━                  Typography, surfaces, focus, h-scroll
──────────────────────────────────────────────────────────────────────────────────────
30        Mar  3      HScrollFix ━━━━━━━                                   Coordinate system fix
                      JITComponentLib ═══                                   Pivot from extraction→JIT
──────────────────────────────────────────────────────────────────────────────────────
31        Mar  3      ThreadMapSynthesis ═══════════                        3-LLM archaeological reconstruction
                                                                            Trails ~70% done (not ~10%)
──────────────────────────────────────────────────────────────────────────────────────
32        Mar  3      TrailsThesis ━━━━━━━━━━━━━━━━━━━━━                   Click-to-navigate, chat scroll,
                                                                            viewport pinning, persistent cmd bar
──────────────────────────────────────────────────────────────────────────────────────
33        Mar  3      SidebarFix ━━━━━━━                                    Readability: ASCII-only glyphs,
                                                                            uniform filename colors
──────────────────────────────────────────────────────────────────────────────────────
34        Mar  4      MarkdownRender ━━━━━━━━━━━━━━━━━━                    Chat pane: block parser, inline
                                                                            spans, code blocks, callouts, tables
35        Mar  9      PluginWorkflow ━━━━━━━━━━━━━━━━━━                    V0 DG workflow: intake→run→review,
                                                                            set-selection, batch/lane model
36        Mar 10      IntakePolish ━━━━━━━━━━━━━                           Markdown intake, Linear bootstrap,
                                                                            table rendering, scroll plumbing
37        Mar 12      RuntimeSplit ━━━━━━━━━━━━━━━━━━━━                    runtime.cljs 2215→106 LOC shell,
                                                                            9 modules, mouse.cljs named helpers,
                                                                            set-selection regression fix
38        Mar 15      DifferentialArch ════════════════                      3-way session: differential principle,
                                                                            commitment boundary, three gaps
39        Mar 21      BufferPool ━━━━━━ ArchReconcile ══════════════        buffer_pool.cljs shipped, sidebar
                                                                            decoupled. Master arch + appendix docs.
                                                                            Rama-first-by-slice agreed. Sidebar
                                                                            chosen as first Rama-backed slice.
40        Mar 22      SidebarRamaSlice ━━━━━━━━━━━━━━━━━━━━━━━━            First Rama-backed slice: sidebar truth
                                                                            in PState, Electric e/watch bridge,
                                                                            domain-identity IDs, shared scene cache,
                                                                            startup hydration + dir/file rehydration
41        Mar 23      SidebarSliceClose ━━━━━━━━━━━━━━━━━━━━━━━            3-layer sidebar state, live truth
                                                                            reconciliation, stale-response guards,
                                                                            hover/text invalidation narrowed
──────────────────────────────────────────────────────────────────────────────────────
42-45     Mar-Apr     (Sessions focused on Phase 6 consolidation, GPU budget tracker,
                      render pacing, font atlas refactoring)
──────────────────────────────────────────────────────────────────────────────────────
46        Apr  4      RenderPacingFix ━━━━━━━━━━━━━━━━━━                    Restored unconditional RAF,
                                                                            removed dirty-present side effects
                                                                            from m/latest (anti-pattern documented)
47        Apr  4      SlugFontBackend ━━━━━━━━━━━━━━━━━━━━━━━━━            MSDF→Slug switch. Side-by-side eval,
                                                                            WGSL fixes, sharpness uniform, settings
                                                                            cleanup, MSDF fonts marked unavailable.
                                                                            Slug = default text backend.
48        May 11      DogfoodLLM ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━             World-first LLM contract MVP:
                                                                            ContextBundles, LLM runs, controls,
                                                                            executor claims, catalog/slices/forks,
                                                                            patch proposals, projections, costs.
──────────────────────────────────────────────────────────────────────────────────────

LEGEND:  ━━━━ = code shipped    ════ = design/vision (no code)    ─── = one-off fix
```

---

## 2. Thread Dependency Graph

```
                    ┌──────────────────────────────────────────────────────────────┐
                    │                    EDITOR CORE (S1-17)                        │
                    │  WebGPU MSDF, Lezer, editing, SCI, themes, cmd panel         │
                    └──────┬──────────────┬──────────────────────┬─────────────────┘
                           │              │                      │
              ┌────────────▼──────┐       │          ┌───────────▼──────────────┐
              │ REACTIVE ARCH (S8)│       │          │ SIDEBAR v1: DOM (S16)    │
              │ m/latest pattern  │       │          │ (replaced in S26)        │
              └────────┬──────────┘       │          └───────────┬──────────────┘
                       │                  │                      │
    ┌──────────────────▼──────────────────▼──────────┐           │
    │            THE QUESTION (Feb 6)                 │           │
    │  "How do we put AI inside the editor?"          │           │
    │  ┌──────────┐ ┌──────────┐ ┌──────────┐        │           │
    │  │ Claude×3 │ │  Codex   │ │  Gemini  │        │           │
    │  │ Moonshot │ │  Hybrid  │ │ Threaded │        │           │
    │  └────┬─────┘ └────┬─────┘ └────┬─────┘        │           │
    │       └─────────────┼───────────┘               │           │
    │                     ▼                            │           │
    │               SYNTHESIS                          │           │
    └─────────────────────┬────────────────────────────┘           │
                          │                                        │
         ┌────────────────┼────────────────────┐                   │
         ▼                ▼                    ▼                   │
  ┌──────────────┐ ┌─────────────┐  ┌──────────────────┐          │
  │ AGENT RT     │ │ VISION      │  │ TIER 1 UI        │          │
  │ (S18-19)     │ │ (Feb 12-16) │  │ (Feb 11-13)      │          │
  │ Rama audit,  │ │ Reasoning   │  │ Agent panel,     │          │
  │ SSE stream,  │ │ Trails,     │  │ streaming text,  │          │
  │ 8 events,    │ │ 10 concepts │  │ scroll, dismiss  │          │
  │ --resume     │ │ Risk plan   │  │                  │          │
  └──────┬───────┘ └──────┬──────┘  └────────┬─────────┘          │
         │                │                   │                    │
         │       ┌────────▼───────────────────▼─────────┐         │
         │       │ PLANNING COMMISSION (S20, Feb 18)     │         │
         │       │ Claude + Codex + Gemini = consensus   │         │
         │       │ 3 vision docs → 1 locked contract     │         │
         │       │                                       │         │
         │       │ Spawns:                               │         │
         │       │  ├─ Flow state machine (9 commands)   │         │
         │       │  ├─ Master-Detail UI lock             │         │
         │       │  └─ Screen 1 spec                     │         │
         │       └────────┬──────────────────────────────┘         │
         │                │                                        │
         │    ┌───────────▼──────────────┐                         │
         │    │ SCREEN 1 INTAKE (S21-22) │                         │
         │    │ --json-schema pipeline   │                         │
         │    │ Direct Linear API        │                         │
         │    │ List view implementation │                         │
         │    └───────────┬──────────────┘                         │
         │                │                                        │
         │    ┌───────────▼──────────────┐                         │
         │    │ RECT TREE UI (S23)       │                         │
         │    │ Scene graph, DnD, events │                         │
         │    └───────┬──────────────────┘                         │
         │            │                                            │
         │    ┌───────▼──────────────────────────────────┐         │
         │    │ GPU COMPONENT LIBRARY (S24-25)            │         │
         │    │ SDF quads, shadows, layout engine,        │         │
         │    │ composable panels, design tokens           │         │
         │    └───────┬──────────────┬───────────────────┘         │
         │            │              │                             │
         │    ┌───────▼────┐  ┌──────▼──────────────────┐         │
         │    │ SIDEBAR v2 │  │ DESIGN CONVERTER (S27)  │         │
         │    │ GPU (S26)  │  │ Multi-LLM pipeline      │◄────────┘
         │    └───────┬────┘  │ → JIT Component Lib     │
         │            │       │   (S30 pivot)           │
         │    ┌───────▼────┐  └─────────────────────────┘
         │    │ SIDEBAR v3 │
         │    │ Simplified │
         │    │ (S28)      │
         │    └───────┬────┘
         │            │
         │    ┌───────▼──────────────────────────────────┐
         │    │ 3-PANE LAYOUT (S28)                       │
         │    │ Editor | Chat | Preview                   │
         │    └───────┬──────────────────────────────────┘
         │            │
         │    ┌───────▼──────────────────────────────────┐
         │    │ UI EXCELLENCE (S29-30)                    │
         │    │ Typography, surfaces, focus, h-scroll     │
         │    └──────────────────────────────────────────┘
         │
    (REASONING TRAILS — the unbuilt thread)
    (REVIEW PACK — prototype, unused, sidebar removed)
    (RAMA RUNTIME — partially built, typed moves deferred)
```

---

## 3. Thread Detail Cards

### T1: Editor Core
- **Origin:** Pre-February 2026, ~15 sessions
- **Who decided:** `[USER]` — built the editor as the starting point
- **Status:** ~90% complete (vim mode, structural editing, multi-file remaining)
- **Evidence:** `progressive-summary.md` Sessions 1-17, feature status table (25 of 29 features checked)
- **Reconnection:** Foundation for everything. Every thread below renders into this editor's WebGPU canvas.
- **Key artifact:** `loop.cljs` (~6200 lines), `editor.cljs`, `electric_flow.cljc`

### T2: Reactive Architecture
- **Origin:** Session 8, multi-agent session
- **Who decided:** `[JOINT]` — Codex built TextInputCore, Claude reviewed, Gemini critiqued; user approved reactive-first direction
- **Status:** DONE (one-time refactor, patterns now permanent)
- **Evidence:** `architecture/reactive-first-plan.md`, `CLAUDE.md` anti-patterns
- **Reconnection:** The `m/latest` pattern became the universal composition mechanism. The `m/ap` crash became the project's most important anti-pattern.

### T3: The Question + 3 LLM Responses
- **Origin:** Feb 6, 2026 — user's prompt: "I want to press Enter and pass commands to Claude/Codex/Gemini via CLI subscriptions"
- **Who decided:** `[USER]` — posed the question, chose to send it to all 3 LLMs simultaneously
- **Status:** COMPLETED (as a decision-making process — all 3 responses archived)
- **Evidence:** `exploration/claude-0.md`, `exploration/codex-response.md`, `exploration/gemini-response.md`, `exploration/claude-3-moonshot.md`, `exploration/synthesis.md`
- **Key fork:**
  - Codex: "hybrid Electric+Rama+WebGPU" — operationally clean, architecturally shallow
  - Gemini: "Context Teleporter" — CLI tools are blind, Rama knows the trajectory
  - Claude r3 (Moonshot): "Every interaction is a Rama event; editor = live materialized view of event stream"
- **Reconnection:** Codex's operational pragmatism became the actual shipping path. Claude's Moonshot reframing became the north star vision. Gemini's "Threaded Engineering" framing named the product category.

### T4: Agent Runtime
- **Origin:** Session 18 (Feb 11) — architecture audit discovered dead Rama scaffolding
- **Who decided:** `[AI]` (Claude) identified the problem; `[USER]` approved the fix
- **Status:** WORKING — SSE streaming, `--resume`, multi-provider toggle all functional
- **Evidence:** `sessions/2026-02-11-architecture-audit.md`, `progressive-summary.md` S18-19
- **Key discovery:** "System A (working HTTP) vs System B (dead Rama scaffold)" — Rama PStates were declared but never written to. The audit wired them together.
- **Reconnection:** This is the plumbing for everything that runs Claude CLI. Screen 1 bootstrap, flow state machine, and future reasoning trail rendering all depend on this.

### T5: Vision — Reasoning Trails
- **Origin:** Feb 12 (session), from user framing: "Thread first, prose second"
- **Who decided:** `[USER]` — the core thesis. `[JOINT]` — the design brief was co-authored
- **Status:** ~85% BUILT (Sessions 19, 32, 33, 34 — trail rendering + click-to-navigate + markdown)
- **Evidence:** `vision/design-brief-reasoning-trails.md` (THE core product definition per `_map.md`)
- **The hypothesis:** "An agent's tool_use stream, presented as a structured trail, makes code review meaningfully better than reading a diff"
- **CONFLICT RESOLVED (S32-34):** The 5-step spike plan was effectively completed:
  - Step 1 (stream data): DONE (S19) — 8 canonical events parsed
  - Step 2 (server parsing): DONE (S19) — structured events, not raw content_block
  - Step 3 (client trail building): DONE (S19-20) — `!agent-output` has `:trail` + `:tool-buf`
  - Step 4 (trail rendering): DONE (S19-34) — `trail->chat-nodes` (250 lines), tool cards with per-kind colors, markdown rendering (block parser, inline spans, code blocks, callouts, tables)
  - Step 5 (click-to-navigate): DONE (S32) — THE killer feature. Click tool card → open file at line.
- **What remains (Layers 3-5):**
  - Layer 3: Rama persistence (`$$agent-trails` PState) — 0%
  - Layer 4: Trail summarization at different zoom levels — 0%
  - Layer 5: PR artifact generation from trails — 0%
- **Reconnection:** The trail is now a usable interactive artifact in the chat pane. Remaining layers are persistence + intelligence layers on top of working UI.

### T6: Planning Commission
- **Origin:** Feb 18 (Session 20) — multi-LLM collaboration on how to build the workflow
- **Who decided:** `[JOINT]` — 3 LLMs (Claude, Codex, Gemini) debated over 5+ rounds; user approved final consensus
- **Status:** APPROVED, partially implemented
- **Evidence:** `plans/commission-consensus.md` (Sections 1-11 APPROVED), `sessions/2026-02-18-planning-commission.md`
- **What it produced:**
  - 6 locked decisions (Section 1)
  - 8 canonical event kinds (Section 2) — implemented
  - Parser boundary invariants (Section 3) — partially enforced
  - State graph with 6 nodes + intake ordering subphase (Section 4) — implemented in code
  - V0 scope checklist (Section 5) — partially done
  - Execution order (Section 6) — Steps 0-4 done, Step 5 pending
  - Screen 1 acceptance gate (Section 10) — now an intake quality bar, not a blocker to the shipped run/review shell
  - Master-Detail UI lock (Section 11) — implemented for intake plus run/review shell
- **Reconnection:** This is the implementation contract. All workflow UI decisions trace back here.

### T7: Three Parallel Vision Documents
- **Origin:** Feb 18, commissioned alongside the Planning Commission
- **Who decided:** `[AI]` — each LLM wrote their version independently; `[USER]` approved all three as valid perspectives on the same idea
- **Status:** ACTIVE REFERENCES (not superseded — they describe the same thing from different angles)
- **Evidence:** `vision/prompt-driven-ui-gemini.md`, `vision/auto-prompt-workflow-claude.md`, `vision/single-session-auto-bootstrap-codex.md`
- **Key insight:** All three converge on: UI events → prompt templates → Claude CLI → parsed state → next UI state. Gemini named it "Prompt-Driven UI," Claude grounded it in discourse-graph, Codex formalized the state transitions.
- **Reconnection:** The `/bootstrap` → selection/ordering in intake → `/run` → `/review` → `/finalize` sequence in `dg_flow.cljs` / `runtime.cljs` is the current implementation of all three documents.

### T8: Screen 1 — Intake List
- **Origin:** Feb 19 (UI lock) → Feb 24 (implementation) → Feb 25 (Direct API)
- **Who decided:** `[USER]` — selected master-detail over card grid; `[AI]` — implementation
- **Status:** IMPLEMENTED, acceptance gate pending (keyboard nav, regression testing)
- **Evidence:** `plans/screen-1-spec.md`, `plans/ui-mockups-master-detail.md`, `progressive-summary.md` S21-22
- **Key pivot (Feb 25):** Bootstrap changed from Claude CLI (30-300s agentic loop) to Direct Linear GraphQL API (1-2s). `[USER]` decided after `--max-turns` flag was discovered to not exist.
- **Reconnection:** This is the first working phase of the plugin workflow local world. Session 35 folded arrangement into intake and shipped the V0 shell beyond this point.

### T9: Rect Tree UI
- **Origin:** Feb 25 (Session 23) — designed and fully implemented in one session
- **Who decided:** `[USER]` — verbal description of the interaction model; `[AI]` — implementation
- **Status:** COMPLETE (all 7 steps)
- **Evidence:** `architecture/rect-tree-ui.md`, `progressive-summary.md` S23
- **What it replaced:** Flat ad-hoc rendering (`compute-ticket-card-rects`, manual `cond` chains)
- **Reconnection:** Every UI surface after S23 uses rect trees: intake list, execution stack, run/review shell, sidebar, and empty states.

### T10: GPU Component Library
- **Origin:** Sessions 24-25 (Feb 27) — SDF quads, shadows, layout engine, composable panels
- **Who decided:** `[JOINT]` — user wanted Linear-quality visuals; AI designed the implementation
- **Status:** COMPLETE
- **Evidence:** `architecture/gpu-component-library.md`, `architecture/virtual-layout-engine.md`, `progressive-summary.md` S24-25
- **What it enabled:**
  - SDF rounded corners, borders, gradients (shader-level)
  - Shadow pipeline (separate render pass)
  - Design tokens (`dt` def — Linear/shadcn dark theme)
  - Auto-layout (`:layout` directive on `rt-node` — CSS flexbox equivalent)
  - Composable panels: `ui-panel`, `ui-panel-header`, `ui-panel-content`, `ui-list-item`, etc.
- **Reconnection:** This is the rendering substrate. Every new UI feature uses `dt` tokens, `resolve-layout`, and the `ui-*` component functions.

### T11: Sidebar Evolution (4 versions)
- **Origin:** Session 16 → 26 → 28 → 40
- **Who decided:** `[USER]` — requested simplification; `[AI]` — implementation
- **v1 (S16):** Imperative DOM, `createElement`/`add-watch`, HTML text
- **v2 (S26):** Full WebGPU rewrite, rect tree, MSDF text, tabs (Files/Review/UI)
- **v3 (S28):** Simplified — tabs removed, file-explorer only, 7-key `!sidebar-state`
- **v4 (S40-S41):** Rama-backed semantic truth + client overlay/UI split. `!sidebar-truth`, `!sidebar-overlay`, and `!sidebar-ui` now drive one derived effective sidebar, with live reconciliation, stale-response guards, and narrowed hover/text invalidation.
- **Status:** COMPLETE for the current file-explorer slice (v4). Remaining future work is workspace scoping + optimistic rollback on action failure, not local authority/perf rescue work.
- **Evidence:** `progressive-summary.md` S16/26/28/40/41
- **Reconnection:** The sidebar is now a thin file tree with a real truth boundary. Review and component browsing were removed because the 3-pane layout handles those functions instead; the remaining job is workflow dogfooding, not more sidebar architecture.

### T12: Design Converter + JIT Component Library
- **Origin:** Session 27 (Mar 1) — multi-LLM extraction pipeline; Session 30 (Mar 1) — pivot to JIT
- **Who decided:** `[USER]` — commissioned the pipeline AND approved the pivot
- **Status:** **STATUS CONFLICT** (see Section 5 below)
- **Evidence:**
  - Pipeline: `sessions/2026-03-01-design-converter.md`, 7 files in `components/`
  - JIT pivot: `sessions/2026-03-01-jit-component-library.md`, `architecture/component-library-jit.md`
- **The pivot:** Session 30 discovered the extraction pipeline conflated two problems (pixel translation vs reusable component). Pivoted to JIT: pre-populated inventory with stub directories, conversion on first click, permanent `.cljc` files.
- **Reconnection:** Pipeline files still exist on disk. The JIT architecture doc describes how they'd be used. But neither path has been end-to-end tested. The sidebar integration for components was removed in S28.

### T13: 3-Pane Layout + UI Excellence
- **Origin:** Session 28 (Mar 2) — 3-pane generalized; Session 29 (Mar 2) — excellence pass
- **Who decided:** `[JOINT]` — user wanted product-quality workspace; AI implemented
- **Status:** Phases 1-2 DONE, Phases 3-4 TODO. **Chat pane now ACTIVE (S32-34).**
- **Evidence:** `plans/ui-excellence-spec.md`, `progressive-summary.md` S28-34
- **What shipped:**
  - 3-pane: Editor (40%) | Chat (55%) | Preview (5%) for any open file (proportions updated S34)
  - Typography hierarchy (4 levels), surface elevation (9 tokens), focus hierarchy (`!active-pane`)
  - Empty states (`build-empty-state`), bottom clip, horizontal scrolling
  - Char-width normalization (0.56 across all 3 files)
  - (S32) Chat scroll, viewport pinning, persistent cmd bar, click-to-navigate
  - (S33) Sidebar readability (ASCII-only glyphs, uniform filename colors)
  - (S34) Full markdown rendering in chat pane (8 block types, inline spans, callouts, tables)
  - (S34) Chat bg warm dark `[0.06 0.04 0.03]`, focus underline (no bg highlight)
  - (S34) Rect z-order fix, editor click handler fix in file-open mode
- **Reconnection:** Chat pane is now a **working** trail display (not placeholder). Preview pane is still a 5% placeholder — this is where JIT component demos (T12) would render.

### T14: Review Pack (orphaned)
- **Origin:** Feb 13 (Session 19 area)
- **Who decided:** `[AI]` — built prototype; `[USER]` — didn't reject it
- **Status:** PROTOTYPE (in-memory atom, not Rama, sidebar integration removed in S28)
- **Evidence:** `plans/implementation-rama-runtime.md` (file not found — may have been removed), `where-we-are.md` describes it as "Prototype"
- **Reconnection:** The review pack concept (6-section validation: intent, scope, changes, decisions, evidence, risks) maps directly to the reasoning trails vision. But the implementation was a standalone in-memory API that was never connected to the main workflow. Sidebar integration removed when sidebar was simplified.

### T15: 10 Wild UI Concepts
- **Origin:** Feb 13-16 — creative exploration triggered by the reasoning trails design brief
- **Who decided:** `[AI]` — generated; `[USER]` — explored but made no commitment
- **Status:** CREATIVE ARCHIVE (no code, no commitment)
- **Evidence:** `vision/10-wild-ui-concepts.md`, `vision/courtroom-discourse-graph.md`, `vision/newspaper-discourse-graph.md`
- **Key insight (Feb 16):** All 10 concepts are just different rendering skins over one universal discourse graph: `[Q]→[C]→[E]→[D]→[R]→[F]`. Build the graph once, switch renderers.
- **Reconnection:** This insight (`[USER]` framing, `[AI]` articulation) is preserved in `where-we-are.md` as strategic stance #3: "The graph is the data model, the metaphor is the rendering layer."

---

## 4. Decision Log (Chronological Fork Points)

```
DATE         DECISION                              WHO        EVIDENCE
──────────────────────────────────────────────────────────────────────────────
pre-Feb      Vector-of-strings buffer (not Rope)   [JOINT]    progressive-summary S1
pre-Feb      SCI for code intelligence (not LSP)   [JOINT]    progressive-summary S2
pre-Feb      m/latest pattern (not m/ap)           [AI→USER]  CLAUDE.md, S10 crash
Feb 6        Send same question to 3 LLMs          [USER]     exploration/claude-0.md
Feb 6        Moonshot as north star                 [USER]     implied by exploring/archiving claude-3
~Feb 8       Reactive-first refactor               [JOINT]    architecture/reactive-first-plan.md
Feb 11       Wire Rama (not bypass it)              [AI→USER]  sessions/2026-02-11-audit
Feb 12       "Thread first, prose second"           [USER]     sessions/2026-02-12-threaded-vision
Feb 13       Risk-first over feature-first          [USER]     plans/risk-first-threaded-plan.md
Feb 13       Review Pack prototype                  [AI]       built speculatively, not explicitly asked
Feb 18       Planning Commission format             [USER]     sessions/2026-02-18-planning-commission
Feb 18       Lock 8 canonical event kinds           [JOINT]    commission-consensus.md §2
Feb 18       Flow state machine with back-edges     [JOINT]    commission-consensus.md §4
Feb 18       Prompt-Driven UI paradigm              [JOINT]    3 vision docs converge
Feb 19       Master-Detail over Card Grid           [USER]     commission-consensus.md §11 (selected Approach 1)
Feb 19       Screen 1 acceptance gate               [JOINT]    commission-consensus.md §10
Feb 24       Card grid → list view (Screen 1)      [USER]     screen-1-spec.md (list was the locked direction)
Feb 24       --json-schema for structured output    [AI→USER]  progressive-summary S21
Feb 25       Direct Linear API (bypass Claude CLI)  [USER]     after discovering --max-turns doesn't exist
Feb 25       Rect tree as UI foundation             [USER]     verbal description → architecture/rect-tree-ui.md
Feb 27       SDF quads + shadows (not flat rects)   [AI→USER]  progressive-summary S24
Feb 27       Layout engine (not manual coords)      [AI→USER]  architecture/virtual-layout-engine.md
Feb 27       Sidebar: DOM → WebGPU                  [USER]     progressive-summary S26
Mar 1        Multi-LLM design converter pipeline    [USER]     commissioned 3 LLMs in parallel
Mar 1        JIT pivot (not extraction-first)       [USER]     sessions/2026-03-01-jit-component-library.md
Mar 2        Sidebar simplified (tabs removed)      [USER]     progressive-summary S28
Mar 2        3-pane for all files (not just comps)  [AI→USER]  progressive-summary S28
Mar 2        :idle default (ticket list gated)      [AI→USER]  progressive-summary S28
Mar 2        UI Excellence pass commissioned        [USER]     plans/ui-excellence-spec.md
──────────────────────────────────────────────────────────────────────────────

LEGEND:  [USER]    = User explicitly chose this direction
         [AI]      = AI proposed and implemented without explicit user selection
         [AI→USER] = AI proposed, user approved (often implicitly by not objecting)
         [JOINT]   = Collaborative decision through discussion/commission
```

---

## 5. Status Conflicts (Where Docs Disagree)

### Conflict 1: Component Library Status

| Document | Says | Date |
|----------|------|------|
| `_map.md` | "Archive — sidebar integration removed session 28" | Mar 2 |
| `architecture/component-library-jit.md` | "APPROVED SOLUTION" | Mar 1 |
| `where-we-are.md` | "Archive" | Mar 2 |
| `ui-ux-tracker.md` | Not mentioned (only Design Converter pipeline listed) | Mar 2 |

**What actually happened:** The JIT Component Library architecture was designed and approved in Session 30 (Mar 1). The VERY NEXT DAY (Session 28, Mar 2), the sidebar was simplified and component browsing was removed from the UI. The architecture doc was never updated to reflect this.

**Resolution:** The JIT architecture design is valid but has NO UI surface to activate it. The `component-library-jit.md` status of "APPROVED SOLUTION" refers to the architecture being approved, not to it being built or accessible. The `_map.md` "Archive" label is more accurate about the *current state* but misleading because the architecture wasn't rejected — the UI entry point was removed.

**Honest status:** APPROVED DESIGN, DISCONNECTED (no UI entry point, no end-to-end test).

---

### Conflict 2: Design Converter Pipeline Status

| Document | Says | Date |
|----------|------|------|
| `where-we-are.md` | "Pipeline built" with detailed file list | Mar 2 |
| `ui-ux-tracker.md` | "COMPLETE" (with 2 unchecked items) | Mar 2 |
| `sessions/2026-03-01-jit-component-library.md` | "Abandoned browser-extraction-first approach" | Mar 1 |

**What actually happened:** The extraction pipeline files (7 files, ~2400 lines across 3 LLMs) were built in Session 27. In Session 30 (same day), the *approach* was reconsidered — the browser-extraction-first strategy was abandoned in favor of JIT conversion. But the pipeline FILES were not deleted or modified. They still exist on disk and could theoretically be used by the JIT conversion agent.

**Resolution:** The files are complete. The approach that commissioned them was pivoted. The files are "built but not integrated" — they are building blocks without a caller.

**Honest status:** FILES COMPLETE, APPROACH PIVOTED. Pipeline components may be reused by JIT conversion agent, but that integration doesn't exist yet.

---

### Conflict 3: Reasoning Trails Progress — RESOLVED (S32-34)

| Document | Said | Date | Updated? |
|----------|------|------|----------|
| `where-we-are.md` | "In progress" — 5-step spike plan | Mar 2 | Needs update |
| `progressive-summary.md` | Only mentions agent panel (S19) | Mar 2 | Updated through S32 |
| `ui-ux-tracker.md` | Not listed | Mar 2 | Needs tier entry |

**What happened since:** Sessions 32-34 dedicated focused work to trails:
- S32: Click-to-navigate (tool card → file:line), chat scroll, viewport pinning, persistent cmd bar
- S33: Sidebar readability fixes (ASCII-only glyphs)
- S34: Full markdown rendering in chat pane (8 block types, inline spans, code blocks, callouts, tables)

**Resolution:** The 5-step spike from `where-we-are.md` is now effectively DONE. The remaining work (Rama persistence, summarization, PR generation) is Layers 3-5 — intelligence/persistence features on top of a working UI.

**Honest status:** INTERACTIVE TRAIL UI DONE (~85%). Persistence + intelligence layers NOT STARTED (0%).

---

### Conflict 4: Review Pack Status

| Document | Says | Date |
|----------|------|------|
| `where-we-are.md` | "Prototype — In-memory API + validation, no real usage; sidebar integration removed" | Mar 2 |
| `ui-ux-tracker.md` | Not mentioned | Mar 2 |
| `_map.md` | Listed under "Building Tier 1" with session reference | Mar 2 |

**What actually happened:** Review Pack was built speculatively in Session 19 area. It was never integrated with the main workflow. Its sidebar entry point was removed in Session 28 when the sidebar was simplified. The HTTP API endpoints may still exist in `server_jetty.clj`.

**Honest status:** ORPHANED PROTOTYPE. Code exists but has no UI, no usage, and no connection to the current workflow. The concept (structured review artifacts) maps to the Reasoning Trails vision but the implementation is disconnected.

---

## 6. Unbuilt Threads (Designed but No Code)

| Thread | Status | Design Doc | Blocker |
|--------|--------|-----------|---------|
| ~~Reasoning Trails (5-step spike)~~ | **DONE (S32-34)** | `where-we-are.md` | — |
| ~~Click-to-navigate (trail→file)~~ | **DONE (S32)** | — | — |
| ~~Intake ordering / execution stack~~ | **DONE (S35)** | `ui-mockups-master-detail.md` | — |
| ~~Run / review shell~~ | **DONE (S35)** | `ui-mockups-master-detail.md` | — |
| Review tabs + provenance / pacing | Partially designed | `ui-mockups-master-detail.md` | Artifact workspace pass |
| Rama trail persistence | Described in `where-we-are.md` Layer 3 | — | Working UI exists (no blocker) |
| Trail summarization (zoom levels) | Described in `where-we-are.md` Layer 4 | — | Working UI exists |
| PR artifact generation | Described in `where-we-are.md` Layer 5 | — | Working UI exists |
| JIT component conversion | Architecture approved | `component-library-jit.md` | No UI entry point |
| UI Excellence Phase 3 | Spec exists | `ui-excellence-spec.md` | Phase 2 verified |
| UI Excellence Phase 4 | Spec exists | `ui-excellence-spec.md` | Phase 3 |
| Discourse graph ontology | Deferred to v2 | `commission-consensus.md` §5 | — |
| Multi-project generalization | Deferred to v2+ | `commission-consensus.md` §5 | — |

---

## 7. Thread Reconnection Map

Where do dormant/completed threads feed back into active work?

```
ACTIVE NOW                        FEEDS INTO
──────────────────────────────────────────────────
3-Pane Chat pane (WORKING)  ←───  ✓ Trails rendering DONE (S32-34)
3-Pane Preview pane (empty) ←───  JIT Component demos (when built)
                                  Design Converter (if reconnected)
Screen 1 acceptance gate    ────► Intake polish only
Plugin workflow shell       ────► Review tabs / provenance / resume fork
Rect tree + layout engine   ────► All future UI surfaces
Agent runtime (streaming)   ────► ✓ Trail rendering DONE, workflow shell shipped
Design tokens (dt)          ────► All future components
Trail UI (S32-34)           ────► Rama persistence → Summarization → PR gen
10 UI concepts insight      ────► Future discourse graph renderer selection
Review Pack (orphaned)      ────► Could merge with Reasoning Trails Layer 3
```

---

## 8. The Big Picture (One Paragraph)

Softland started as a WebGPU code editor (Sessions 1-17) and pivoted to become a **prompt-driven AI workflow tool** when the user asked "how do we put AI inside it?" on Feb 6. Three LLMs responded; their ideas were synthesized into a Planning Commission consensus (Feb 18) that locked the architecture: prompts as API calls, one Claude CLI session with `--resume`, and a master-detail workflow shell. Screen 1 (ticket intake) was built with a direct Linear API, a rect tree scene graph, SDF rendering, and composable layout engine. The sidebar evolved three times (DOM→WebGPU→simplified). A design converter pipeline and JIT component library were designed but never connected end-to-end. Sessions 31-34 (Mar 3-4) were a turning point: the 3-LLM thread map synthesis revealed trails were ~70% done (not ~10%), and Sessions 32-34 closed the gap — click-to-navigate turns tool cards into live code links, and full markdown rendering (8 block types, callouts, tables) makes the chat pane a proper reasoning trail viewer. Session 35 then folded arrangement into intake and shipped the V0 plugin workflow dogfooding loop. **The core product thesis now has a working interactive UI.** What remains is workflow stabilization, the richer review artifact workspace, the persistence layer (Rama trails), the intelligence layer (summarization), and the output layer (PR artifact generation).

---

## 9. Per-Component State Diagrams

### LLM Contribution Standing

```
                     CONTRIBUTION LEADERBOARD
  ╔══════════════════════════════════════════════════════════════╗
  ║                                                              ║
  ║   CLAUDE    ████████████████████████████████████████  1st     ║
  ║             Primary implementer. All shipped code.           ║
  ║             S1-30: editor, agent RT, flows, UI, layout,      ║
  ║             SDF shaders, sidebar, CSS parsers, compiler      ║
  ║                                                              ║
  ║   CODEX     ██████████████████                       2nd     ║
  ║             S8: TextInputCore (refactored by Claude)         ║
  ║             S20: commission co-author, review checklists     ║
  ║             S27: IR schema, verifier, blueprints             ║
  ║             S20: single-session semantics doc                ║
  ║                                                              ║
  ║   GEMINI    ██████████                               3rd     ║
  ║             S8: architecture critique                        ║
  ║             S18: "Context Teleporter" framing                ║
  ║             S20: planning commission participant             ║
  ║             S27: LLM extraction prompt                       ║
  ║                                                              ║
  ╚══════════════════════════════════════════════════════════════╝

  Context: Claude authored/modified ALL production code files.
  Codex's TextInputCore (S8) was refactored into reactive-first.
  Codex's IR schema + verifier (S27) are on disk but not called.
  Gemini's contributions are framing/prompts (no .clj/.cljs code).
```

---

### A. Editor Core — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                        EDITOR CORE                                  │
  │                   loop.cljs + editor.cljs                           │
  │                      ~6200 + ~1200 lines                            │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  RENDERING                            EDITING                       │
  │  ──────────                           ────────                      │
  │  [##########] MSDF text        100%   [##########] Type/BS/Del 100% │
  │  [##########] Per-glyph color  100%   [##########] Cut/Copy/P  100% │
  │  [##########] SDF rects        100%   [##########] Undo/Redo   100% │
  │  [##########] Shadow pipeline  100%   [##########] Tab/Indent  100% │
  │  [##########] Pixel snapping   100%   [########  ] Save file    80% │
  │  [##########] Themes (5+)      100%   │             (downloads,     │
  │                                       │              no write-file) │
  │  NAVIGATION                           [          ] Vim mode      0% │
  │  ───────────                          [          ] Paredit       0% │
  │  [##########] Arrows/Home/End  100%   [          ] Multi-file    0% │
  │  [##########] Ctrl+Arrow word  100%                                 │
  │  [##########] Mouse click      100%   CODE INTEL                    │
  │  [##########] Mouse select     100%   ──────────                    │
  │  [##########] Scroll vert      100%   [##########] Lezer syntax100% │
  │  [##########] Scroll horiz     100%   [##########] Brackets    100% │
  │  [##########] Code folding     100%   [##########] SCI eval    100% │
  │                                       [##########] Fold regions100% │
  │  PANELS                                                             │
  │  ───────                              PERFORMANCE                   │
  │  [##########] Cmd panel(Ctrl+K)100%   ─────────────                 │
  │  [##########] Settings (Ctrl+G)100%   [##########] Idle CPU     100% │
  │  [##########] Status bar       100%   │            76%→6%           │
  │  [##########] Agent output     100%   [##########] Large file   100% │
  │  [##########] 3-pane layout    100%   │            1440ms→cached    │
  │  [##########] Sidebar (file)   100%   [##########] Dirty check  100% │
  │                                       │            identical?       │
  │                                                                     │
  │  OVERALL: ████████████████████░░  ~90%                              │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ████████████████████████████  S1-17, all code            │
  │    Codex   ████                          S8 TextInputCore (refactored)
  │    Gemini  ██                            S8 critique                │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### B. LLM CLI Integration — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                    LLM CLI INTEGRATION                              │
  │            server_jetty.clj + objects.cljc + loop.cljs              │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  TRANSPORT LAYER                                                    │
  │  ┌──────────────────────────────────────────────────────────┐       │
  │  │                                                          │       │
  │  │  Browser ──POST──► /api/agent/stream ──SSE──► Browser    │       │
  │  │     │                    │                       ▲       │       │
  │  │     │              ProcessBuilder                │       │       │
  │  │     │              claude --output-format         │       │       │
  │  │     │              stream-json                    │       │       │
  │  │     │                    │                       │       │       │
  │  │     │              parse-stream-json-line         │       │       │
  │  │     │              (8 canonical event kinds)      │       │       │
  │  │     │                    │                       │       │       │
  │  │     │              StreamableResponseBody        │       │       │
  │  │     │              .flush on each event ─────────┘       │       │
  │  │     │                                                    │       │
  │  │     └──fallback──► /api/agent/run (blocking) ────────────┘       │
  │  └──────────────────────────────────────────────────────────┘       │
  │                                                                     │
  │  STATUS PER FEATURE                                                 │
  │                                                                     │
  │  [##########] SSE streaming endpoint             100%  (S19)        │
  │  [##########] 8 canonical event kinds            100%  (S19-20)     │
  │  [##########] --resume session continuity        100%  (S18)        │
  │  [##########] Rama session-id persistence        100%  (S18)        │
  │  [##########] Multi-provider toggle              100%  (S20)        │
  │  [##########] --json-schema structured output    100%  (S21)        │
  │  [##########] --allowedTools MCP permissions     100%  (S20)        │
  │  [##########] Agent output panel (wrap/scroll)   100%  (S12)        │
  │  [##########] Trail rendering (structured cards)   100%  (S19,32)   │
  │  │             tool cards, per-kind colors, collapsible,            │
  │  │             file-op grouping, shimmer for pending                │
  │  [##########] Click-to-navigate (trail→file)      100%  (S32)      │
  │  │             click tool card → open file at line in editor        │
  │  [##########] Markdown in chat pane               100%  (S34)      │
  │  │             8 block types, inline spans, code blocks,            │
  │  │             callouts, tables, word wrap                          │
  │  [########  ] Full reasoning trails                85%  CORE THESIS│
  │  │             interactive UI done, persistence TBD                 │
  │  [          ] Rama trail persistence               0%               │
  │  [          ] Trail summarization (zoom levels)    0%               │
  │  [          ] PR artifact generation               0%               │
  │                                                                     │
  │  OVERALL: ████████████████████░░  ~85%                              │
  │  (interactive trail UI done, persistence + intelligence TBD)        │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ██████████████████████████████  All transport + parsing   │
  │    Codex   ██████████                      Event contract co-author  │
  │    Gemini  ██████                          Commission participation  │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### C. Electric / Missionary Reactive Layer — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                   ELECTRIC / MISSIONARY LAYER                       │
  │               electric_flow.cljc + loop.cljs flows                  │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  HOW DATA FLOWS (the reactive spine of the app)                     │
  │                                                                     │
  │  ┌─────────────┐     ┌──────────────────────────────────────┐       │
  │  │ e/server     │     │ loop.cljs reactive flows (Missionary)│       │
  │  │ file slurp   │────►│                                      │       │
  │  │ (one-shot)   │     │  Source atoms (6):                   │       │
  │  │              │     │    !editor-doc  !settings             │       │
  │  │ Lezer parse  │     │    !focus       !active-font          │       │
  │  │ SCI eval     │     │    !cmd-panel   !ai-provider          │       │
  │  │              │     │                                      │       │
  │  │ char-width   │     │  Derived atoms (reactive state):     │       │
  │  │ = 0.56       │     │    !flow-state  !sidebar-truth       │       │
  │  │ (normalized) │     │    !sidebar-overlay !sidebar-ui       │       │
  │  └─────────────┘     │    !agent-output !active-pane         │       │
  │                       │    !scroll-y    !scroll-x             │       │
  │                       │    !drag-state  !mouse-x/y           │       │
  │  Key constraint:      │                                      │       │
  │  e/watch only works   │  Terminal flows (m/latest):          │       │
  │  within single peer.  │    <editor-rects    (23 args!)       │       │
  │  No server→client     │    <combined-text-ops (22 args!)     │       │
  │  missionary pipes.    │    <cmd-panel-rects                  │       │
  │                       │    <world-snapshot                   │       │
  │  Pattern:             │                                      │       │
  │  HTTP for data xfer,  │  m/reduce → draw-frame! (terminal)  │       │
  │  Electric for boot    └──────────────────────────────────────┘       │
  │                                                                     │
  │  STATUS                                                             │
  │                                                                     │
  │  [##########] m/latest composition pattern      100%  (S8-10)       │
  │  [##########] m/eduction event filtering        100%  (S10)         │
  │  [##########] Dirty checking (identical?)       100%  (S16b)        │
  │  [##########] File content via e/server         100%  (S1)          │
  │  [##########] Fold/bracket cached flows         100%  (S17)         │
  │  [##########] Font settings reactive            100%  (S12)         │
  │  [######    ] HTTP data + Electric boot split    60%                │
  │  │             pattern works but not all data                       │
  │  │             paths use it consistently                            │
  │  [          ] e/watch cross-peer (blocked)        0%  (limitation)  │
  │                                                                     │
  │  ANTI-PATTERNS (hard rules in CLAUDE.md)                            │
  │  ╔══════════════════════════════════════════════════╗                │
  │  ║ NEVER: m/ap + multiple m/?< inside m/latest     ║                │
  │  ║ NEVER: try/catch inside e/defn                   ║                │
  │  ║ USE:   m/eduction + @deref for event filtering   ║                │
  │  ║ USE:   m/latest for combining watches            ║                │
  │  ╚══════════════════════════════════════════════════╝                │
  │                                                                     │
  │  OVERALL: ████████████████████░░  ~90%                              │
  │  (mature, stable, the patterns are locked)                          │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ████████████████████████████  All reactive flows          │
  │    Codex   ████                          S8 initial TextInputCore    │
  │    Gemini  ██                            S8 critique of patterns     │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### D. UI/UX — Workflow Screens — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                    UI/UX WORKFLOW SCREENS                          │
  │              Plugin Workflow V0 (updated S35)                      │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  THE FLOW STATE MACHINE                                             │
  │                                                                     │
  │   :idle ──/bootstrap──► :bootstrapping ──► :intake ──► :run        │
  │                                             ▲            │          │
  │                                             │            ▼          │
  │                                        :finalize ◄── :review       │
  │                                             ▲            │          │
  │                                             │            ▼          │
  │                                          :intake ◄── :rework        │
  │                                                                     │
  │  PER-PHASE STATUS                                                   │
  │                                                                     │
  │  1. INTAKE          ████░  grouped list + detail/stack shipped      │
  │                     Remaining: keyboard gate, small polish          │
  │  2. ORDERING        █████  execution stack shipped inside intake     │
  │                     Shift+Up/Down reorder, Enter or /run            │
  │  3. RUN             ███░░  batch map + trail stream shipped          │
  │                     Remaining: scroll reset + intake scroll leak    │
  │  4. REVIEW/REWORK   ███░░  shell shipped, artifact surface partial   │
  │                     Remaining: tabs, provenance, pacing, resume     │
  │                                                                     │
  │  OVERALL: ███████████░░░░░░  ~60%                                    │
  │  (V0 dogfooding loop exists; review workspace still deepening)      │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ██████████████████████████  Implementation + prompts      │
  │    Codex   ████████████████            Commission co-author, gates   │
  │    Gemini  ██████████████              Commission co-author, PDUI    │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### E. GPU Rendering Stack — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                     GPU RENDERING STACK                             │
  │              editor.cljs shaders + loop.cljs scene graph            │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  THE PIPELINE                                                       │
  │                                                                     │
  │  rt-node tree (data)                                                │
  │       │                                                             │
  │       ├── resolve-layout    (CSS flexbox equivalent)                │
  │       │     │                                                       │
  │       ├── tree->rects       28 floats/rect (SDF)                    │
  │       │     │                ┌─────────────────────────────┐        │
  │       │     └───────────────►│ RECT PIPELINE               │        │
  │       │                      │ sd_rounded_box (IQ SDF)     │        │
  │       │                      │ Per-corner radii             │        │
  │       │                      │ Independent border widths    │        │
  │       │                      │ Linear gradients             │        │
  │       │                      │ Anti-aliased edges           │        │
  │       │                      └─────────────────────────────┘        │
  │       │                                                             │
  │       ├── tree->shadows     20 floats/shadow                        │
  │       │     │                ┌─────────────────────────────┐        │
  │       │     └───────────────►│ SHADOW PIPELINE              │        │
  │       │                      │ Gaussian CDF (erf_approx)   │        │
  │       │                      │ 3x blur_radius expansion    │        │
  │       │                      │ Separate render pass        │        │
  │       │                      └─────────────────────────────┘        │
  │       │                                                             │
  │       └── tree->text-ops    12 floats/glyph                         │
  │             │                ┌─────────────────────────────┐        │
  │             └───────────────►│ TEXT PIPELINE                │        │
  │                              │ MSDF atlas (Ubuntu Sans)    │        │
  │                              │ Per-glyph RGBA color        │        │
  │                              │ screenPxRange clamp         │        │
  │                              │ DPR-aligned pixel snapping  │        │
  │                              └─────────────────────────────┘        │
  │                                                                     │
  │  COMPONENT LIBRARY                                                  │
  │  ┌────────────┬────────────┬────────────┬────────────────────┐      │
  │  │ ui-panel   │ ui-card    │ ui-badge   │ ui-button          │      │
  │  │ ui-panel-  │ ui-divider │ ui-progress│ ui-scrollbar       │      │
  │  │  header    │ ui-list-   │ ui-tabs    │ ui-tooltip         │      │
  │  │  content   │  item      │ ui-checkbox│ ui-priority-dot    │      │
  │  │  footer    │ ui-panel-  │            │ build-empty-state  │      │
  │  │  group     │  group     │            │                    │      │
  │  └────────────┴────────────┴────────────┴────────────────────┘      │
  │                                                                     │
  │  DESIGN TOKENS (dt)                                                 │
  │  ┌──────────┬──────────┬──────────┬──────────┬──────────────┐      │
  │  │ :colors  │ :spacing │ :radii   │ :shadows │ :font-sizes  │      │
  │  │ :surfaces│          │          │          │              │      │
  │  └──────────┴──────────┴──────────┴──────────┴──────────────┘      │
  │                                                                     │
  │  STATUS                                                             │
  │  [##########] Rect tree (rt-node, hit-test, DnD)    100%  (S23)     │
  │  [##########] SDF fragment shader                   100%  (S24)     │
  │  [##########] Shadow pipeline                       100%  (S24)     │
  │  [##########] Layout engine (resolve-layout)        100%  (S25)     │
  │  [##########] Composable panel components           100%  (S25)     │
  │  [##########] Design tokens                         100%  (S24)     │
  │  [##########] Typography hierarchy                  100%  (S29)     │
  │  [##########] Surface elevation system              100%  (S29)     │
  │  [##########] Text clipping (clip-right)            100%  (S29)     │
  │  [##########] Horizontal scroll coordinates         100%  (S30)     │
  │                                                                     │
  │  OVERALL: ████████████████████  100%                                │
  │  (complete — this is the most mature subsystem)                     │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ██████████████████████████████  All shaders + tree walks  │
  │    Codex   (none)                                                   │
  │    Gemini  (none)                                                   │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### F. Rama Backend — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │                      RAMA BACKEND                                   │
  │              core.clj + util_fns.cljc + objects.cljc                │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  DEPOTS (event streams)                                             │
  │    *node-events-depot ━━━━━━━━━━━━━━  declared, used               │
  │                                                                     │
  │  PSTATES (materialized views)                                       │
  │    $$cli-sessions-pstate ━━━━━━━━━━━  {file→provider→session-id}    │
  │      └── WIRED (S18): lookup before, store after CLI run            │
  │    $$agent-runs-pstate ━━━━━━━━━━━━━  {run-id→metadata}            │
  │      └── WIRED: run lifecycle events                                │
  │    $$nodes-pstate ════════════════════  declared, unused             │
  │    $$dg-nodes-pstate ════════════════  declared, unused             │
  │    $$dg-edges-pstate ════════════════  declared, unused             │
  │                                                                     │
  │  STREAM TOPOLOGY                                                    │
  │    :cli-command ━━━━━━━━━━━━━━━━━━━━  wired                        │
  │    :update-cli-session ━━━━━━━━━━━━━  wired (S18)                   │
  │    :agent-run ━━━━━━━━━━━━━━━━━━━━━━  wired                        │
  │    :llm-request ════════════════════  exists, not used by workflow   │
  │    :new-node ═══════════════════════  exists, not used               │
  │                                                                     │
  │  TASK GLOBALS                                                       │
  │    CliProcessTaskGlobal ━━━━━━━━━━━  ProcessBuilder for CLI exec    │
  │    CljHttpTaskGlobal ━━━━━━━━━━━━━━  HTTP client for Linear API     │
  │                                                                     │
  │  STATUS                                                             │
  │  [##########] CLI session persistence (--resume)     100%  (S18)    │
  │  [##########] Agent run lifecycle                    100%  (S18)    │
  │  [##########] Process execution (ProcessBuilder)     100%  (S18)    │
  │  [######    ] Event routing (3 of 5 topology cases)   60%  (S18)    │
  │  [###       ] PState utilization (2 of 5 used)        30%           │
  │  [          ] Discourse graph PStates                   0%  deferred│
  │  [          ] Trail persistence ($$agent-trails)        0%  planned │
  │  [          ] Typed moves (DG ontology)                 0%  deferred│
  │                                                                     │
  │  OVERALL: ██████████████░░░░░░  ~55%                                │
  │  (plumbing works, but half the Rama surface is scaffolding)         │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ████████████████████████████  All wiring + audit          │
  │    Codex   ████████                      Original scaffold (S pre)   │
  │    Gemini  (none)                                                   │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### G. Design Converter + Component Library — State of Work

```
  ┌─────────────────────────────────────────────────────────────────────┐
  │             DESIGN CONVERTER + COMPONENT LIBRARY                    │
  │                   components/ directory                             │
  ├─────────────────────────────────────────────────────────────────────┤
  │                                                                     │
  │  TWO EXTRACTION PATHS (neither end-to-end tested)                   │
  │                                                                     │
  │  PATH 1: DETERMINISTIC                                              │
  │  ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌──────────┐        │
  │  │_extractor│──►│_css_     │──►│_token_   │──►│_compiler │──► rt   │
  │  │.js       │   │parsers   │   │matcher   │   │.cljc     │   node  │
  │  │(DOM walk)│   │.cljc     │   │.cljc     │   │          │        │
  │  │ 115 lines│   │ 436 lines│   │ 228 lines│   │ 352 lines│        │
  │  │ Claude   │   │ Claude   │   │ Claude   │   │ Claude   │        │
  │  └──────────┘   └──────────┘   └──────────┘   └──────────┘        │
  │       Built ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  Not connected        │
  │                                                                     │
  │  PATH 2: LLM-ASSISTED                                              │
  │  ┌──────────────┐                              ┌──────────┐        │
  │  │ _extractor_  │──► LLM ──► Design IR ───────►│_compiler │──► rt  │
  │  │ prompt.md    │                               │.cljc     │  node  │
  │  │ 113 lines    │                               │          │        │
  │  │ Gemini       │                               │ Claude   │        │
  │  └──────────────┘                               └──────────┘        │
  │       Built ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  Not connected        │
  │                                                                     │
  │  VALIDATION LAYER (Codex)                                           │
  │  ┌──────────────┐   ┌──────────────┐   ┌──────────────────┐        │
  │  │_design_ir    │   │ _verifier    │   │ by-codex/        │        │
  │  │.cljc         │   │ .cljc        │   │ blueprints/      │        │
  │  │ 717 lines    │   │ 560 lines    │   │ schemas/         │        │
  │  │ Schema valid │   │ Geometry diff│   │ tests/           │        │
  │  │ Codex        │   │ Codex        │   │ Codex            │        │
  │  └──────────────┘   └──────────────┘   └──────────────────┘        │
  │       Built ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━  Not connected        │
  │                                                                     │
  │  JIT COMPONENT LIBRARY (the pivot — Mar 1)                          │
  │  ┌──────────────────────────────────────────────────────────┐       │
  │  │ Architecture: APPROVED    UI entry point: REMOVED (S28)  │       │
  │  │                                                          │       │
  │  │  components/                                             │       │
  │  │  ├── _registry.edn       (planned, not created)          │       │
  │  │  ├── _token_maps/                                        │       │
  │  │  │   └── shadcn-v4.edn   (created, token map extracted)  │       │
  │  │  ├── button/              (blueprint exists)              │       │
  │  │  │   └── shadcn.cljc     (stub)                          │       │
  │  │  └── ... 40+ planned directories (not created)           │       │
  │  │                                                          │       │
  │  │  Sidebar "UI" tab:       REMOVED in S28                  │       │
  │  │  Convert button:         NOT BUILT                       │       │
  │  │  Agent stream in pane:   NOT BUILT                       │       │
  │  │  Render preview:         NOT BUILT                       │       │
  │  └──────────────────────────────────────────────────────────┘       │
  │                                                                     │
  │  STATUS                                                             │
  │  [##########] CSS parsers (12 parsers)               100%           │
  │  [##########] Token matcher (fuzzy matching)         100%           │
  │  [##########] IR → rt-node compiler                  100%           │
  │  [##########] DOM extractor (JS)                     100%           │
  │  [##########] IR schema validator (Codex)            100%           │
  │  [##########] Geometry verifier (Codex)              100%           │
  │  [##########] LLM extraction prompt (Gemini)         100%           │
  │  [          ] End-to-end test (extract→render)         0%           │
  │  [          ] JIT registry + stubs                     0%           │
  │  [          ] Convert button UI                        0%           │
  │  [          ] Integration with main app                0%           │
  │                                                                     │
  │  OVERALL: ██████████████░░░░░░  ~60%                                │
  │  (all pieces built, nothing connected)                              │
  │                                                                     │
  │  WHO BUILT IT:                                                      │
  │    Claude  ██████████████████████          Parsers, matcher, compiler│
  │    Codex   ████████████████                Schema, verifier, blueprints
  │    Gemini  ██████████                      Extraction prompt         │
  └─────────────────────────────────────────────────────────────────────┘
```

---

### H. Overall System Health — Dashboard

```
  ╔═══════════════════════════════════════════════════════════════════════╗
  ║                    SOFTLAND — SYSTEM HEALTH DASHBOARD                ║
  ║                         as of March 4, 2026                          ║
  ╠═══════════════════════════════════════════════════════════════════════╣
  ║                                                                       ║
  ║  COMPONENT               HEALTH   BUILT   NOTES                      ║
  ║  ─────────────────────── ──────── ─────── ────────────────────────── ║
  ║  A. Editor Core           ████░    90%    Vim/paredit/multi-file TBD ║
  ║  B. LLM CLI Integration   ████░    85%    Trail UI done, persist TBD ║
  ║  C. Electric/Missionary    ████░    90%    Mature, patterns locked   ║
  ║  D. Workflow Screens       █░░░░    25%    Screen 1 only of 5        ║
  ║  E. GPU Rendering Stack    █████   100%    Most mature subsystem     ║
  ║  F. Rama Backend           ██░░░    55%    Half is dead scaffolding  ║
  ║  G. Design Converter       ███░░    60%    All pieces, none connected║
  ║                                                                       ║
  ║  PRODUCT THESIS STATUS                                                ║
  ║  ──────────────────────                                              ║
  ║  "Reasoning trails as first-class review artifacts"                  ║
  ║                                                                       ║
  ║  Infrastructure for it:     ████████████████████░░░░  ~80%           ║
  ║  Interactive trail UI:      █████████████████████░░░  ~85%           ║
  ║  Persistence + intelligence:██░░░░░░░░░░░░░░░░░░░░░░  ~5%           ║
  ║                                                                       ║
  ║  THE GAP HAS SHIFTED: The trail is now a working interactive UI.    ║
  ║  S32 added click-to-navigate, S34 added markdown rendering.          ║
  ║  What remains: Rama persistence, summarization, PR generation.       ║
  ║  The gap is now persistence + intelligence, not rendering.           ║
  ║                                                                       ║
  ╠═══════════════════════════════════════════════════════════════════════╣
  ║                                                                       ║
  ║  LLM CONTRIBUTION SUMMARY                                            ║
  ║  ──────────────────────────                                          ║
  ║                                                                       ║
  ║  1. CLAUDE   [Primary implementer]                                   ║
  ║     Code:    ██████████████████████████████████████  ~95% of all code║
  ║     Design:  ████████████████████████████████        ~80% of design  ║
  ║     Sessions: 1-30 (all of them)                                     ║
  ║     Strongest: Shipping code. Every production file.                 ║
  ║                                                                       ║
  ║  2. CODEX    [Schema architect + reviewer]                           ║
  ║     Code:    ██████████████                          ~15% (S8,S27)   ║
  ║     Design:  ████████████████████████                ~60% co-author  ║
  ║     Sessions: 8, 20, 27                                              ║
  ║     Strongest: Acceptance gates. IR schemas. Rigor.                  ║
  ║     Weakest:  S8 code was refactored; S27 code not called.          ║
  ║                                                                       ║
  ║  3. GEMINI   [Vision framer + critiquer]                             ║
  ║     Code:    ████                                    ~5% (prompt)    ║
  ║     Design:  ████████████████████                    ~50% co-author  ║
  ║     Sessions: 8, 18, 20, 27                                          ║
  ║     Strongest: Named "Prompt-Driven UI". "Context Teleporter".      ║
  ║     Weakest:  No .clj/.cljs production code shipped.                ║
  ║                                                                       ║
  ╚═══════════════════════════════════════════════════════════════════════╝
```

---

### I. The Critical Path Forward

```
  ╔═══════════════════════════════════════════════════════════════════════╗
  ║                       WHAT UNBLOCKS WHAT                             ║
  ╠═══════════════════════════════════════════════════════════════════════╣
  ║                                                                       ║
  ║                                                                       ║
  ║                                                                       ║
  ║   TRACK A (Workflow)     TRACK B (Trails)        TRACK C (Components)║
  ║   ─────────────────      ─────────────────       ──────────────────  ║
  ║   Intake quality         Review workspace        JIT UI entry       ║
  ║   polish                 deepen                  point (Phase 3)    ║
  ║   (kbd nav, intake       (tabs, provenance,                           ║
  ║   cleanup)               pacing, resume)                              ║
  ║        │                     │                        │              ║
  ║        ▼                     ▼                        ▼              ║
  ║   Workflow               Rama persistence        Preview pane       ║
  ║   stabilization          ($$agent-trails)        (component demos)  ║
  ║   (4 known bugs)         Trail summarization     JIT conversion UI  ║
  ║        │                     │                                       ║
  ║        └──────────────┐      ▼                                       ║
  ║                       ▼   PR artifact                                ║
  ║                  Real dogfood generation for review                  ║
  ║                  batches                                             ║
  ║                                                                       ║
  ║   ┌─────────────────────────────────────────────────────────────┐    ║
  ║   │ UPDATE (S35): V0 plugin workflow shell is shipped.         │    ║
  ║   │ Track A: stabilize shipped workflow shell                  │    ║
  ║   │ Track B: deepen review artifact workspace                  │    ║
  ║   │ Track C: reconnect JIT/component UX                        │    ║
  ║   │ A+B converge in the review surface; C stays parallel.      │    ║
  ║   └─────────────────────────────────────────────────────────────┘    ║
  ║                                                                       ║
  ╚═══════════════════════════════════════════════════════════════════════╝
```
