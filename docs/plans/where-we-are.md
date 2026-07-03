# Where We Are — Softland

> Last updated: 2026-03-23 (sidebar Rama slice closed; dogfooding next)
> Read this when you sit down and need to know: what exists, what we decided, what's next.

---

## Current Watchlist (What To Keep Open)

### Code Files (active implementation surface)
- `src/app/electric_flow.cljc`
- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/workspace/runtime.cljs`
- `src/app/client/workspace/*.cljs`
- `src/app/client/workflows/*.cljs`
- `src/components/`
- `components/` — Design Converter pipeline (parsers, compiler, extractor, IR validator)

### Planning/Sync Docs (active source of truth)
- `docs/plans/where-we-are.md`
- `docs/plans/commission-consensus.md`
- `docs/plans/screen-1-spec.md`
- `docs/plans/ui-mockups-master-detail.md`
- `docs/plans/ui-explorations.md`

---

## What Exists (built, working, in the code)

### Current Runtime Refactor Status (2026-03-12)
- Active client runtime moved out of `src/app/client/webgpu/`
- New structure is live:
  - `electric_flow.cljc` = entry / membrane
  - `client/substrate/webgpu/renderer.cljs` = rendering substrate
  - `client/workspace/*` = shared runtime/composition/input/view machinery
  - `client/workflows/*` = DG + JIT workflow ownership
- **Session 37: `runtime.cljs` split into thin shell (106 LOC) + 9 modules under `workspace/runtime/`**
  - `state.cljs` (160) — atoms + `make-runtime-state`
  - `fonts.cljs` (38) — font loading + watch
  - `sidebar_io.cljs` (105) — sidebar HTTP + watch + seed
  - `interop.cljs` (156) — preview watch + dev replay + window globals
  - `agent_flow.cljs` (304) — agent API (auto-scroll, event handler, submit)
  - `scroll.cljs` (98) — wheel event routing
  - `keyboard.cljs` (406) — 6 keyboard consumers
  - `mouse.cljs` (508) — 9 named click handlers + consumer dispatch (12 LOC)
  - `render.cljs` (273) — derived flows + GPU diff-upload + draw
- Build: 238 files, 0 warnings. `start-loop!` signature unchanged.
- `set-selection` regression found and fixed (mouse.cljs + keyboard.cljs were bypassing `dg-flow/set-selection`)
- Remaining known regressions after runtime verification:
  - ~~editor drag-selection~~ — fixed (S35): raw DOM handlers, Missionary event buffering was the root cause
  - ~~chat click-through~~ — fixed (S35): missing paren caused unconditional editor cursor placement
  - settings panel text layout still has clipping/placement issues
  - intake detail scroll still broken (carried from S36)

### Editor / Workspace Runtime (`runtime.cljs` thin shell + `runtime/*.cljs` + `renderer.cljs`)
- WebGPU MSDF text rendering with per-glyph theme colors (Gruvbox, Rose Pine, Kanagawa, etc.)
- Full editing: type, delete, cut/copy/paste, undo/redo, tab/indent
- Keyboard + mouse navigation, bracket matching, code folding
- Selection: click-to-place and drag-select both working (fixed S35)
- SCI in-browser Clojure eval
- File explorer sidebar (Ctrl+B, file-only, no tabs), settings panel (Ctrl+G), command panel (Ctrl+K)
- 3-pane layout when file is open: Editor (40%) | Chat (55%) | Preview (5%) via `build-file-layout`
- Line numbers, current-line highlight, 24px status bar (Ln/Col + filename + provider)
- Idle CPU optimized: 76% → 6% via `identical?` dirty checking on `m/latest` cached objects

### Agent Runtime (server_jetty.clj + objects.cljc + loop.cljs)
- `/api/agent/stream` — SSE over POST with real token-level Claude CLI streaming
- `/api/agent/run` — blocking fallback endpoint
- Client: `stream-agent-run!` using `fetch()` + `ReadableStream` with SSE buffering (`\n\n` split)
- Agent output panel: dynamic height (caps 50% viewport), text wrapping, scroll, dismiss (Escape)
- Streaming text overlap bug FIXED (newline split before wrap-line)
- `--resume` session-id extraction + Rama persistence (`$$cli-sessions-pstate`)
- Multi-provider toggle (Claude/Codex/Gemini) via `/provider` command, shown in status bar

### Screen 1: Master-Detail Intake (loop.cljs — 2026-02-24)
- **Left pane (40%)**: Grouped ticket list by status, collapsible group headers, checkbox selection, hover highlight, priority badges, scroll with content clamping
- **Right pane (60%)**: 3 states — empty ("No tickets found"), unselected (instructional), selected (ticket detail with wrapped title/description/metadata OR multi-select summary)
- **Rects**: Header separator, detail background card, divider
- **Status bar**: Shows "INTAKE | N tickets | M selected" in flow canvas mode
- **Interactions**: Click row = toggle select, click group header = collapse/expand, mousemove = hover tracking
- **Bootstrap**: Lean prompt (one sentence), `normalize-ticket` field mapper, `parse-tickets-from-trail` ready for when server forwards `:tool-result` events
- **Defaults**: Command panel visible on load, focus on command panel (not editor), flow starts at `:idle` (ticket list only after `/bootstrap` aka `/dg`)

### Rama Backend (core.clj + util_fns.cljc)
- `*node-events-depot` — all graph/node events
- `$$cli-sessions-pstate` — `{file → provider → {:session-id :last-active}}` for `--resume`
- `$$agent-runs-pstate` — run lifecycle by run-id
- `$$sidebar-pstate` — **NEW (S40)**: global `{Keyword Object}` for sidebar committed truth (project, expanded-dirs, selected-file). 4 event handlers in topology.
- Stream topology with `:cli-command`, `:update-cli-session`, `:sidebar/*` cases
- Server-side `!sidebar-truth-atom` bridged to Electric via `e/watch` (direct Rama subscription blocked by RocksDBWrapper serialization bug in Rama 1.6.0)
- Discourse graph PStates exist (`$$dg-nodes-pstate`, `$$dg-edges-pstate`) but unused by agent flow

### Sidebar Rama Slice (S40-S41 — 2026-03-22 to 2026-03-23)
- **3-layer state model**: `!sidebar-truth` (committed semantic truth), `!sidebar-overlay` (optimistic semantic intent), `!sidebar-ui` (hover/scroll/cache/in-flight local state)
- **Truth boundary**: project + expanded-dirs + selected-file in Rama; hover, scroll, caches, and dedupe state local
- **Event flow**: client fire-and-forget POST → Rama topology → server atom → Electric `e/watch` → client reconciliation → derived effective sidebar
- **Live reconciliation**: truth watch is active again; overlay clears when truth catches up, including `nil` sentinel back-navigation cases
- **Async hardening**: in-flight dir/file dedupe, stale file-response protection, and error-path cleanup for in-flight flags
- **Shared scene**: `!sidebar-scene` cached by render flow, consumed by hit-test + sidebar text. Hover still repaints rects, but `!sidebar-scene` only updates when structural inputs change.
- **Structural hash narrowed**: only visual geometry inputs (`:scroll-y`, `:dir-cache`, `:home-dirs`) can invalidate sidebar scene/text; hover and in-flight flags no longer churn text uploads
- **Domain-identity IDs**: `(keyword prefix path)` — e.g. `:e//home/sid/foo.cljs` (collision-free, inspectable)
- **Remaining later-scope gaps**: sidebar PState is still global (not workspace-scoped), action POST failures still do not roll back optimistic semantic intent, shared scene is still a mutable cache rather than a first-class derived flow

### Design Converter Pipeline (components/ — 2026-03-01, multi-LLM)
- **Goal:** Extract any UI component from a rendered web page → compile to rt-node → render on canvas
- **Two extraction paths:**
  1. **Deterministic:** `_extractor.js` (DOM walk via `getComputedStyle`) → `_css_parsers` → `_token_matcher` → `_compiler` → rt-node
  2. **LLM-assisted:** `_extractor_prompt.md` (component source → LLM → Design IR directly)
- **Files:**
  - `_css_parsers.cljc` (436 lines) — CSS string → Clojure values (color, shadow, gradient, layout)
  - `_token_matcher.cljc` (228 lines) — literal values → nearest `dt` design tokens (fuzzy matching)
  - `_compiler.cljc` (352 lines) — Design IR → rt-node tree (dual-input: IR + rt-node passthrough)
  - `_extractor.js` (115 lines) — browser DOM walker (Chrome, 31 CSS properties, wrapper collapsing)
  - `_design_ir.cljc` (717 lines, Codex) — strict IR schema validator
  - `_verifier.cljc` (560 lines, Codex) — geometry/style/structure diffs with pass/fail thresholds
  - `_extractor_prompt.md` (113 lines, Gemini) — LLM prompt targeting Design IR output
  - `by-codex/blueprints/` — seed blueprints (button 3-state, sidebar composition)
- **Status:** All files built. End-to-end test pending (extract → parse → compile → render → visual compare).

### Review Pack Prototype (review_pack.clj — in-memory, not Rama)
- Create/publish/summary/feedback API endpoints
- 6-section validation (intent, scope, changes, decisions, evidence, risks)
- Unit tests passing
- Sidebar UI with thread canvas preview
- Storage: `defonce` atom (not persisted)

---

## What We Decided (convergence from all vision conversations)

### The Core Thesis
The artifact of development work should not be just the code diff. It should be the **reasoning trail** — the code changes PLUS the exploration path PLUS the decision tree PLUS the rejected alternatives.

### The Key Reframe (Feb 13)
The developer is the Claude agent. The user is the reviewer. The agent's `tool_use` stream (Read, Grep, Edit calls) IS the exploration trail. No special prompting needed — the work session IS the thread.

### The Data Insight
The CLI stream already contains structured `tool_use` events (`content_block_start` with `type: "tool_use"`, `input_json_delta`, `content_block_stop`). We currently **discard** these at `server_jetty.clj` in `parse-stream-json-line`. The trail data is flowing through the pipeline — we just throw it away.

### The Architecture Insight (Feb 16)
All 10 UI visions (Crime Board, Courtroom, Newspaper, Metro Map, etc.) are just different rendering skins over one universal discourse graph: `[Q] → [C] → [E] → [D] → [R] → [F]` with typed edges (supports, opposes, addresses, informs, depends_on, supersedes). Build the graph once in Rama, switch renderers later. **The graph is the data model. The metaphor is the rendering layer.**

### The Execution Strategy (Feb 18, updated Feb 24): Prompt-Driven UI
We are not building a complex backend to manage tickets and worktrees. The UI is a **Remote Control** for the Claude CLI.
- **App Load** → Command panel open, focused, editor with 3-pane layout; ticket view only after `/bootstrap` (`/dg`)
- **Drag Ticket** → Auto-prompt: "Create worktree"
- **Click Review** → Auto-prompt: "Show diff"
See: `docs/vision/prompt-driven-ui-gemini.md` (Strategy) and `docs/vision/problem-manual-orchestration-gemini.md` (Problem).

### The Concrete Instance (Feb 18, updated Mar 10): Plugin Workflow for the `discourse-graph` Codebase
Same paradigm as Gemini's Prompt-Driven UI, but grounded in one concrete use case: the developer's daily plugin workflow on the `discourse-graph` codebase.
- **Entry point:** Manual `/bootstrap` or `/mock` populates the intake list. Flow starts at `:idle`.
- **Current V0 state graph:** `:idle -> :bootstrapping -> :intake -> :run -> :review <-> :rework -> :finalize -> :intake`
- **Arrangement moved inward:** ordering is now an intake subphase shown as an execution stack in the right pane; there is no separate `:arrange` node.
- **One local world:** left pane stays the map, right pane stays the current artifact workspace.
- **Dogfooding loop is code-complete:** `/mock -> select -> execution stack -> Enter or /run -> trail stream -> /rework or /finalize`
- **Reasoning Trails now fill the artifact slot** for run/review instead of waiting for a hypothetical Screen 5.
See: `docs/vision/auto-prompt-workflow-claude.md`
Codex clarification (single-session, auto-bootstrap, flow-state semantics): `docs/vision/single-session-auto-bootstrap-codex.md`
**Implementation consensus (Codex + Claude, approved):** `docs/plans/commission-consensus.md` — locked decisions, event contract, state graph, V0 scope, execution order.

### UI Lock (Feb 19, updated Mar 9): Master-Detail Split — One Local World
The UI direction is a 2-pane master-detail pattern. Left pane is always the map, right pane is always the current artifact workspace:
- **Intake:** grouped ticket list (left) + empty state / single detail / execution stack (right). Arrangement is a spatial subphase — no separate `:arrange` screen.
- **Run:** batch map with lane statuses (left) + live trail stream (right)
- **Review:** batch map with outcomes (left) + tabbed artifact workspace: Summary | Trail | Diff | Tests (right)

Authoritative docs:
- Process contract + flow gates: `docs/plans/commission-consensus.md` (Sections 10 and 11)
- ASCII mocks per screen: `docs/plans/ui-mockups-master-detail.md`
- Screen 1 implementation spec (list/master-detail): `docs/plans/screen-1-spec.md`
- Explored alternatives archive (Codex + Claude + decision): `docs/plans/ui-explorations.md`

### What NOT To Build Yet
- Roam/Linear integrations (paste context manually — 10 seconds vs 2 weeks of integration work)
- Full discourse graph ontology (typed moves, tensions, decisions, DG indexing — defer to v2)
- Multi-lane "Inquiry Arena" UI (defer to v2)
- Category theory operators (v3+)
- Model training pipelines (different product entirely)
- Replacing GitHub for review (push rich context INTO GitHub via API, don't pull reviewers out)

---

## What's Next (active implementation track)

### Active Execution Path (updated 2026-03-10)

> **Status change:** Session 41 closed the sidebar Rama slice enough that it no longer blocks the main path. Next job: dogfood on a real batch, then deepen the review artifact workspace.

Immediate order:
1. ~~**Fix the four known workflow issues**~~ — DONE (Session 36): `/select` now uses `set-selection`, `!run-scroll-y` resets on run start, `!scroll-y` resets on intake→run, `/status` and `/review` guard against trail clobber.
2. **Dogfood the end-to-end loop on a real batch** — use the V0 flow exactly as it now exists: `/mock -> select -> stack -> /run -> review -> rework/finalize`.
3. **Deepen the artifact workspace** — add review tabs (`Summary | Trail | Diff | Tests`), provenance badges, pacing defaults, and resume fork.
4. **Only then expand the workflow model** — parallel lanes, richer per-lane tracking, preserved run history.

### Lane A (Active): Plugin Workflow Dogfooding + UX Deepening
1. ~~Fix the four known V0 workflow bugs from code review.~~ — DONE (Session 36)
2. ~~Preserve the run/review artifact surface while status/info commands execute.~~ — DONE (Session 36)
3. Dogfood the V0 loop on a real batch from the plugin codebase.
4. Keep rework-history as accepted V1 scaffolding unless streaming infrastructure changes.

References:
- Contract/gates: `docs/plans/commission-consensus.md`
- Visual target: `docs/plans/ui-mockups-master-detail.md`

### Lane B (Parallel): Reasoning Trail Spike

### The Hypothesis
> An agent's tool_use stream, interleaved with reasoning text, presented as a structured trail, makes code review meaningfully better than reading a diff or a chat log.

### What's Already Built (Steps 1-4 effectively done)

All 5 spike steps below were completed across Sessions 19-29 (not as a dedicated spike, but as part of streaming + UI work):

- **Step 1 (stream data):** DONE — `server_jetty.clj` parses all 8 canonical event kinds including `:tool-use-start`, `:tool-input-delta`, `:tool-result` (lines ~432-536)
- **Step 2 (server parsing):** DONE — server emits structured events, not raw `content_block_*`
- **Step 3 (client trail building):** DONE — `!agent-output` has `:trail` + `:tool-buf`, event handler builds trail nodes (loop.cljs ~4393-4452)
- **Step 4 (trail rendering):** DONE — `trail->chat-nodes` (250 lines, ~line 3004): tool cards with per-kind colors (cyan=Read, green=Grep, yellow=Edit, orange=Bash), collapsible thinking blocks, file-op grouping, shimmer for pending. `build-file-layout` chat pane calls it when `(seq trail)`.

### What Remains

**The real gap: Click-to-navigate.** When a trail card shows "Read server_jetty.clj:45-78", clicking it should open that file at line 45 in the editor pane. This turns trails from a log into a code thread.

**Step 5 (evaluate):** Still pending — needs a dedicated session to run a real multi-step task and assess whether the trail view is genuinely more useful than flat logs.

### Remaining Layers

**Layer 1: Structured Trail** — DONE. `group-trail-blocks` merges reasoning, `group-consecutive-file-ops` collapses file ops, `trail->chat-nodes` renders tool cards with colored backgrounds.

**Layer 2: Click-to-Navigate** — NEXT. Click tool card → open file at line in editor pane. The killer feature that turns trails into code threads.

**Layer 3: Rama Persistence** — `$$agent-trails` PState. Enables reopening trails later, querying by file, resume support.

**Layer 4: Summarization** — agent summarizes its own trail at different zoom levels. The fractal layer.

**Layer 5: Review Artifact Generation** — auto-generate rich PR description + inline comments from trails, push to GitHub via API.

---

## The Strategic Stance

1. **Self-review first.** Use the trail for your OWN review before trying to change how reviewers work. If it makes your work better, that's proof of value.
2. **Push context into GitHub, don't pull reviewers out.** Generate rich PR descriptions from trails. The reviewer stays in their normal workflow but gets better information.
3. **The graph is the data model, the metaphor is the rendering layer.** Build the discourse graph structure once. The Courtroom, Newspaper, Constellation views are just different renderers over the same `Q/C/E/D/R/F` nodes and typed edges.
4. **Lean into specificity.** Generic "session recording" will be commoditized by tool vendors. The specific "DiscourseGraphs development workflow" won't be.

---

## Thread Status Quick Reference

| Thread | Status | What's There |
|--------|--------|-------------|
| Editor Core | ~90% done | Full editing, rendering, UI panels, optimization |
| Agent Streaming | Working | SSE token streaming, output panel, --resume |
| Reasoning Trails | ~70% built | Spike Steps 1-4 effectively done: `trail->chat-nodes` (250 lines), `group-trail-blocks`, tool cards, shimmer, file-op grouping all shipped. Click-to-navigate is the real gap. |
| Prompt-Driven UI (Gemini) | Active reference | Architecture: UI as remote control for CLI, prompt registry, auto-prompter |
| discourse-graph Workflow (Claude) | Active | Concrete 5-screen instance of PDUI, scoped to discourse-graph project |
| Master-Detail UI (Codex+Claude+User) | **Locked** | 2-pane screen progression chosen; screen mocks approved |
| Screen 1 List Intake | **Implemented** | Grouped list, right-pane detail, hover/select/scroll/empty states done. Acceptance gate (keyboard nav, regressions) pending. |
| Rama Runtime | Partially built | Run lifecycle + sessions done, typed moves deferred |
| 3-Pane File Layout | **Implemented** | `build-file-layout`: Editor 40% / Chat 30% / Preview 30% for any open file |
| Sidebar (File Explorer) | **Rama-backed slice complete** | File-only sidebar with committed truth + optimistic overlay + local UI split; live reconciliation and narrowed invalidation shipped |
| Review Pack | Prototype | In-memory API + validation, no real usage yet; sidebar integration removed |
| Design Converter | **Pipeline built** | 7 files (3 LLMs), two extraction paths, end-to-end test pending |
| Component Library | **DEFERRED** | Architecture approved (`component-library-jit.md`), UI entry point removed in S28, planned reintegration via Phase 3 (`ui-excellence-spec.md`) |
| UI Visions | Creative archive | 10 concepts + discourse graph formalization, no commitment |
