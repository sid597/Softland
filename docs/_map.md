# Softland Documentation Map

> Last updated: 2026-03-08
> Read this first. It tells you where everything is and how it got there.

## Living Documents (updated regularly)

| File | Purpose |
|------|---------|
| `plans/where-we-are.md` | THE orientation doc — what exists, what we decided, what's next |
| `history/progressive-summary.md` | THE compact implementation history (milestones, decisions) |
| `plans/ui-ux-tracker.md` | THE UI/UX checklist — Tier 1 done, Tier 2 in-progress, Tier 3 future |
| `exploration/claude-3-moonshot.md` | THE north star vision — event-sourced AI, "Context Teleporter" |
| `vision/design-brief-reasoning-trails.md` | THE core product definition — reasoning trails as first-class artifact |
| `vision/epistemic-framework.md` | THE stabilized epistemic framework — coordinates, pressures, loops, constraints |
| `vision/terminology-glossary.md` | THE shared vocabulary — centers, layers, loops, pressures, and refactor-era terms |

Everything else is write-once archive.

---

## Directory Structure

```
docs/
├── _map.md                  ◄── YOU ARE HERE
├── reference/               Static external library documentation
├── architecture/            Why the system is shaped this way
├── history/                 What happened (implementation logs, debugging patterns)
├── exploration/             The multi-agent brainstorm (Claude, Codex, Gemini)
├── vision/                  Product definition + speculative UI concepts
├── plans/                   Active and completed implementation plans
├── sessions/                Dated work logs (what was built per session)
└── transcripts/             Raw AI session dumps (unedited prompt logs)
```

---

## Causality Tree

```
FOUNDATION (pre-Feb 2026)
│
│  Built the reactive WebGPU editor over many sessions
│
├── history/progressive-implementation.md ── full session log (3800 lines, archive)
│     └──→ history/progressive-summary.md ── distilled from above (living)
│
├── architecture/reactive-first-plan.md ── "go reactive-first" decision
│     └──→ architecture/electric-architecture-review.md ── review of that decision
│
├── history/insights.md ── patterns discovered while building
│     └──→ CLAUDE.md (root) ── most critical patterns extracted for agent instructions
│
├── architecture/optimisations.md ── large-file perf investigation
├── architecture/usage-docs-msdf.md ── MSDF font tuning
├── architecture/rect-tree-ui.md ── Rect tree UI: scene graph, DnD, event propagation
├── architecture/gpu-component-library.md ── GPU component library vision (Zed GPUI 5-primitive model)
├── architecture/virtual-layout-engine.md ── Layout engine + composable components (shadcn pattern)
├── architecture/component-library-jit.md ── JIT Component Library (DEFERRED — architecture approved, UI entry point removed in S28, planned reintegration via Phase 3 of `ui-excellence-spec.md`)
│     Pre-populated inventory, JIT conversion on click, token maps.
│     Component files still exist on disk (`components/`); sidebar/click integration removed from loop.cljs.
│
│  Session 26: Sidebar migrated from DOM to WebGPU rect tree
│  Session 28: Sidebar simplified to file-explorer only (no tabs, no Review/UI modes)
│  3-pane layout (Editor|Chat|Preview) generalized for all open files via build-file-layout
│  Ticket view gated behind /bootstrap (/dg) — initial-flow-state starts at :idle


THE QUESTION (Feb 6, 2026)
│
│  "We have this editor. How do we put AI inside it?"
│
├── transcripts/2026-02-06-cli-integration-prompt.txt ◄── THE PROMPT
│   │
│   │  Sent to three AI agents simultaneously:
│   │
│   ├──→ exploration/codex-response.md ────── Codex: "hybrid Electric+Rama+WebGPU"
│   ├──→ exploration/gemini-response.md ───── Gemini: "Threaded Engineering, Risk-First"
│   └──→ exploration/claude-0.md ──────────── Claude r1: "three pillars, Rama-native"
│         │
│         └──→ exploration/claude-1.md ── Claude r2: deeper Rama mapping
│               │
│               └──→ exploration/claude-3-moonshot.md ── Claude r3: THE MOONSHOT
│                     │           "Context Teleporter", AI-as-collaborator
│                     │
│                     └──→ exploration/synthesis.md ── synthesis of all 3 agents


UNBLOCKING & AUDIT (Feb 9-11)
│
│  Before building anything new, fix the foundation
│
├── sessions/2026-02-09-build-fixes.md
│   │  Fixed fetch-edn!, unblocked build
│   │
│   └──→ sessions/2026-02-11-architecture-audit.md
│         │  DISCOVERY: System A (working) vs System B (dead Rama scaffold)
│         │
│         ├──→ plans/ui-ux-tracker.md ◄── TRACKER BORN (living checklist)
│         │
│         └──→ exploration/implementation-review.md
│              Review of Codex's streaming code, identifies dead System B


VISION CRYSTALLIZATION (Feb 12-13)
│
│  Three AI responses converge into concrete design briefs
│
├── sessions/2026-02-12-threaded-vision.md
│   │  User frames the goal: "Thread first, prose second"
│   │
│   ├──→ vision/threaded-engineering-workspace.md
│   │    (triggered by gemini-response.md's "threaded engineering" framing)
│   │
│   └──→ vision/design-brief-reasoning-trails.md
│        (triggered by claude-3-moonshot.md's "reasoning trails" concept)
│        ◄── THIS IS THE CORE PRODUCT DEFINITION
│
├── plans/risk-first-threaded-plan.md
│   │  "Don't build incrementally — validate riskiest assumptions first"
│   │
│   └──→ sessions/2026-02-13-streaming-threads.md
│         │  Merges streaming + threads: "guide pointing at stepping stones"
│         │
│         └──→ transcripts/2026-02-13-streaming-architecture.txt
│              Full SSE streaming architecture plan (raw session dump)


BUILDING TIER 1 (Feb 11-13, parallel track)
│
│  While vision was crystallizing, actual features shipped
│
├── plans/ui-ux-tier1-plan.md ◄── plan (COMPLETED)
│   │
│   └──→ BUILT: agent output panel, streaming, cmd panel
│        (code in loop.cljs, server_jetty.clj, objects.cljc)
│        Tracked in plans/ui-ux-tracker.md → Tier 1 COMPLETE
│
└── sessions/2026-02-13-review-pack-v0.md
    │  Built Review Pack domain, HTTP API, Thread Canvas UI
    │
    └──→ plans/implementation-rama-runtime.md
         Rama-native multi-agent runtime plan (next phase)


UI EXPLORATION (Feb 13-16)
│
│  "What should the reasoning trail LOOK like?"
│
├── transcripts/2026-02-13-wild-ui-visions.txt ◄── initial ASCII sketches
│   │
│   └──→ vision/10-wild-ui-concepts.md
│         │  10 metaphor-based concepts (Crime Board, Constellation, etc.)
│         │
│         ├──→ vision/courtroom-discourse-graph.md + vision/render_courtroom.py
│         └──→ vision/newspaper-discourse-graph.md + vision/render_newspaper.py
│
├── transcripts/2026-02-15-reasoning-constellation.txt
│   │  "Reasoning Constellation" orbit view concept
│   │
│   └──→ transcripts/2026-02-16-constellation-ascii-art.txt
│        Continuation — constellation views, Reasoning Cards ASCII art
```

---

DESIGN CONVERTER (Mar 1, 2026)
│
│  "Extract any UI component from a web page → compile to rt-node → render on canvas"
│  Multi-LLM: Claude (parsers + compiler + extractor), Codex (schema + verifier),
│  Gemini (LLM extraction prompt)
│
├── sessions/2026-03-01-design-converter.md ◄── session log
│
├── src/components/css_parsers.cljc ─ CSS string → Clojure value parsers
├── src/components/token_matcher.cljc ─ literal values → nearest dt tokens
├── src/components/compiler.cljc ──── Design IR → rt-node trees
├── src/components/adapter.cljc ───── component adapter / normalization path
├── src/components/design_tokens.cljc ─ shared design token surface
├── components/_extractor.js ──────── browser DOM walker (deterministic path)
│
├── old-infra/src/components/_design_ir.cljc ─ IR schema validator (Codex, quarantined)
├── old-infra/src/components/_verifier.cljc ─ geometry/style/structure diffs (Codex, quarantined)
├── components/_extractor_prompt.md ── LLM extraction prompt (Gemini)
│
├── components/by-codex/ ──────────── blueprints, schemas, test specs (Codex)
│   ├── blueprints/button/ ── shadcn button (3 states)
│   ├── blueprints/sidebar/ ── Linear-feel issue sidebar
│   ├── schemas/design-ir.edn
│   └── tests/verification-spec.edn
│
└── vision/design-converter-ux.md ── UX vision: 3 layers, split view, feedback loop, north star

3-LLM CONSENSUS DEBATE (Mar 3, 2026)
│
│  "What do we build next? Three LLMs debate, user picks."
│  Thread maps revealed reasoning trails are ~70% done (not ~10%).
│  Claude's position won: test the trails thesis before building more.
│
├── consensus-next-execution-path.md ◄── 3-way debate (Gemini: Phase 3, Codex: Gate first, Claude: Test thesis)
│   └──→ User ruling: Claude's path accepted (verify trails → click-to-navigate → evaluate → gate)
│
├── thread-map-claude.md ── per-component ASCII dashboards, 15 thread cards (~900 lines)
├── thread-map-codex.md ── 18 threads, 5 status conflicts, code-level line refs
├── thread-map-gemini.md ── 5 subsystem diagrams, architecture flow
├── thread-map-gemini-v1.md ── Gemini's synthesized v1
│
├── sessions/2026-03-03-thread-map-session.md ◄── Session 31 log + synthesis
└── sessions/next-prompt.md ◄── Session 32 resume prompt (trails thesis test)


UI EXCELLENCE PASS (Mar 2, 2026)
│
│  "Bring workspace shell from prototype to product quality"
│  Scope: rendering invariants, typography, surface elevation, focus hierarchy, empty states
│
├── plans/ui-excellence-spec.md ◄── CANONICAL SPEC (full history, what/why/status per section)
│
├── Phase 1 (P0) DONE: char-width normalization, text clipping, typography hierarchy
├── Phase 2 (P1) DONE: surface elevation tokens, !active-pane focus, empty states, h-scroll
├── Phase 3 (P2) TODO: context-aware content, component detail view, sidebar enhancements
│
├── components/design_tokens.cljc ─── :surfaces key added (elevation palette)
├── electric_flow.cljc ────────────── char-width 0.60 → 0.56
├── loop.cljs ─────────────────────── typography, depth, focus, empty states, h-scroll
│
└── New atoms: !active-pane, !scroll-x (wired into <editor-rects + <combined-text-ops)


## Reference Docs (static, external)

| File | Lines | Source |
|------|-------|--------|
| `reference/electric-tutorial.txt` | 746 | Hyperfiddle Electric 3 tutorial |
| `reference/electric-codebase.txt` | 11,746 | Full Electric repo file index |
| `reference/missionary-reference.txt` | 996 | Missionary API reference |

---

## Files NOT in docs/ (intentionally)

| File | Location | Why |
|------|----------|-----|
| `CLAUDE.md` | project root | Claude Code loads it automatically from root |
| `README.md` | project root | Tracked in git, public-facing |
| `vision/what-softland-is-codex.md` | docs/vision/ | Codex perspective on what Softland is |
| `vision/what-softland-is-claude.md` | docs/vision/ | Claude perspective on what Softland is |
