# Softland Editor — Implementation History (Compact)

> Full session log: `progressive_implementation.md` (3500+ lines, archived)
> This summary: key milestones, architecture decisions, current state, lessons learned.

---

### Session 49 — Rama Space Rename + Text Kernel Split — 2026-05-13
- **PR1 vocabulary landed.** Commit `1ef1cbd` completed the mechanical rename from dogfood world/thread/turn vocabulary to space/turn vocabulary and split the old text kernel surface into shared contracts plus a text instance. This was intentionally not `defkernel` generation and did not change Rama topology behavior.
- **Current source shape.** Shared ActionRequest/ActionDecision/KernelEvent contracts and helpers live in `src/app/server/rama/core.clj`; the V0/V1 text instance lives in `src/app/server/rama/text_kernel.clj`; the dogfood space runtime lives in `src/app/server/rama/dogfood/space.clj`; LLM-owned run state remains in `src/app/server/rama/dogfood/llm.clj`.
- **LLM bridge vocabulary updated.** LLM keeps its own `llm-turn-run` concept, while cross-module keys now bind by space/turn (`$$llm-thread-by-space`, `$$llm-turn-run-by-turn`, `read-run-for-turn`).
- **Collision guard added.** Tests now cover the overlapping `:turn/cancel` / `:turn/steer` keywords: space requests discriminate by `:request/type`, LLM controls discriminate by `:control/type`, and LLM run requests reject control-shaped request types.
- **Verification:** targeted Rama suite passed: `52 tests`, `447 assertions`, `0 failures`, `0 errors`.

### Session 48 — Rama LLM Contract MVP — 2026-05-11
- **Slice roadmap executed.** The 12-slice LLM contract roadmap landed as green vertical slices from `d7baea4` through `5402128`, followed by `287699c` documenting the cost-rollup tradeoff inline. At landing, the completed code lived in `src/app/server/rama/dogfood/llm.clj` and `src/app/server/rama/dogfood/world.clj`, with coverage in `dogfood_llm_test.clj` and `dogfood_world_test.clj`. Post `1ef1cbd`, the dogfood world file/test are renamed to `space.clj` and `dogfood_space_test.clj`.
- **World-first LLM loop implemented.** User sends enter World first, create WorldTurns, freeze ContextBundles, then derive LLM run requests. UI/helper paths do not append user sends directly to the LLM depot. Idempotency prevents duplicate turns, bundles, and runs on replay.
- **Execution and control contract implemented.** LLM run lifecycle, approval/cancel/compact/steer controls, approval timeout, executor claims, durable grant wait, fake Codex adapter tests, observations, stale approval behavior, follow-up runs on bound native threads, and patch proposal ingestion all landed.
- **Knowledge materialization landed.** Raw LLM item indexes, eager catalog rows, slices, overlays/comments, derivatives, fork/reconciliation flows, rebuildable projections, and per-thread token/cost rollups are now represented in the Rama dogfood runtime.
- **Writer asymmetry made testable.** External helpers and executors append depots only; PState changes are topology-derived. The writer-asymmetry property test started in the LLM spine and was extended across the LLM/World contract surface.
- **Verification at hardening:** focused LLM (`10 tests`, `80 assertions`), focused World (`25 tests`, `198 assertions`), focused LLM+World (`35 tests`, `278 assertions`), and combined Rama (`46 tests`, `383 assertions`) all passed with `0 failures`, `0 errors`.
- **Topology simplification explicitly recorded.** The final cost rollup uses an incremental per-run usage delta instead of recomputing totals from all `:runs` entries on each observation. What was lost: hot-path self-repair if `$$llm-cost-by-thread` is manually corrupted. What was kept: deterministic replay from depot history and no double-counting. Full repair should rebuild the rollup from canonical run token usage.

---

### Session 47 — Slug Font Backend: Evaluation & Switch — 2026-04-04
- **Side-by-side comparison built.** Split-screen renderer (`draw-comparison-frame!`) showing MSDF left / Slug right, same file content. Validated slug pipeline end-to-end.
- **WGSL shader fixes.** Two bugs in slug shaders: `@interpolate(flat)` required for `vec4<u32>` inter-stage variables; shift operator `<<` requires `u32` right operand.
- **Slug evaluated against MSDF.** Normal sizes: tie. Zoomed in: slug matches terminal crispness (same Bézier math as FreeType). Zoomed out: MSDF slightly bolder due to sharpness bias. User preferred slug's unbiased raw mathematical output.
- **Switched to slug as default backend.** `preferredBackend: "slug"` in manifest.json. MSDF code preserved but not active.
- **Settings panel cleaned up.** Removed pxRange and sharpness sliders (MSDF-only). Marked 3 MSDF-only fonts (Ubuntu Sans Mono, Ubuntu Mono, Noto Sans Mono) as `available: false`.
- **Fixed settings panel focus bug.** `file-load-consumer` was resetting focus to `:editor` even when settings panel was open.
- **Fixed electric_flow.cljc compilation.** Canvas boot section from S46 render pacing fix had structural issue — inner `let` body split confused edamame parser.
- **Key insight:** Slug renders the same math as FreeType (terminal text) but on the GPU per-pixel per-frame. Resolution-independent at any zoom. No tuning needed. The strategic choice for continuous semantic zoom.
- **Next:** Render pacing fix from S46 still unverified. Slug font tooling needed for additional fonts.

---

### Session 45 — Phase 6C-6E: Shadows, Mount, Dirty-Present — 2026-04-01
- **Phase 6C: Per-source shadow pools.**
  Shadows moved from full-upload `update-shadows` to `batch-update-pool!`. Two separate pools (`!editor-shadow-pool`, `!sidebar-shadow-pool`) prevent cross-source position shifts. Buffer pool generalized with configurable `:floats-per-item` and `:pack-fn` (rects=28 floats, shadows=20 floats). Legacy `update-shadows` init call cleaned up.
- **Phase 6D: Handle-checked API + GPU mount.**
  Generation counters on pool slots (incremented on free). Public handle API (`allocate-handle!`, `update-handle!`, `free-handle!`) with `validate-handle` that degrades to warning, never throws. `gpu-mount` returns 5 callbacks matching Electric's `incseq/mount` contract (`append-child`, `replace-child`, `insert-before`, `remove-child`, `nth-child`) with internal `!children` ordering vector. Slot-map experiment from dynamic-sdf-engine resource review completed.
- **Phase 6E: Dirty-present strategy.**
  One-shot RAF (`>dirty-raf`) replaces continuous `make-raf-flow` — zero callbacks when idle except 530ms blink. Persistent render target at physical pixel resolution survives swap chain double-buffering. Clear-quad shader for partial region clearing. Per-subsystem dirty flags mapped to screen regions (sidebar strip, editor area, chrome strip), unioned into scissor rect. `loadOp: "load"` + scissor + clear-quad for partial redraw; full clear fallback for first frame, resize, text/font changes.
- **Phase 6 "done" criteria met:** rect writes O(changed), text writes O(visible changed lines), idle = zero RAF except blink.
- **Commit:** `70a402a`
- **Next:** Phase 8 consolidation (remove scaffolding PStates, server atom mirrors, reconnect product lanes).

---

### Session 44 — Phase 6A+6B: Differential Pipeline for Rects and Text — 2026-03-29
- **Phase 6A: Editor rects differential.**
  Every editor rect now has `:id` + `:z`. New `ordered-diff-update-pool!` in buffer-pool combines identity-based diffing with sequential slot assignment (z-correct draw order). Editor rects moved from full-upload `update-rects` to differential pool. Caret blink: 1 GPU write per tick.
- **Phase 6B: Region-split text buffers.**
  `<combined-text-ops` split into three Missionary flows: `<content-text` (editor + sidebar), `<chrome-text` (cmd + agent + status), and a combining flow that preserves identity of unchanged region. Content flow watches deduped editor lines (`m/eduction (map :lines) (dedupe)`) so cursor-only moves skip content re-upload. Chrome text gets its own GPU buffer via `clone-text-system`.
- **Key results:**
  Sidebar-only changes show `chrome-same?: true` (chrome text skipped). Caret blink: 1 rect write, 0 text writes. Editor pool ordered-diff: 0.1-0.4ms for 8-15 rect updates on scroll.
- **Review-driven fixes:** z-order via ordered slots (not arbitrary keyed-diff allocation), `batch-update-pool!` write accounting includes zeroing, `chrome-same?` includes `settings-visible`, cursor-derived line-number highlighting removed from content flow.
- **Next:** Phase 6C-E completed in Session 45.

---

### Session 43 — Workspace Substrate Correction + Differential Proof + Workspace Truth Persistence — 2026-03-29
- Shipped Phases `0-5` and `7` of the consensus refactor program:
  ownership freeze, five Rama persistence slices, semantic action boundary, artifact selection, effective local world, pane descriptors, split semantics, workflow-as-substrate, editor committed-path measurement, keyed-diff proof, and workspace truth persistence.
- **Key result:** the workspace now has a coherent semantic substrate:
  `!effective-local-world` as semantic truth, keyed-diff GPU pools as render cache, and identity as the bridge between them.
- **Measurements:**
  editor Rama round-trip averaged ~`7.5ms` with low-teens p95;
  sidebar keyed-diff updates stayed roughly `0.1-0.9ms` over tens to hundreds of rows.
- Fixed several behavioral edge cases during review:
  workflow bootstrapping state, restore via semantic pane action, non-file restore clearing stale `!current-file`, full committed-event coverage for editor persistence, large-file read failures clearing both local and committed sidebar selection, and accurate keyed-diff write counting.
- **Next:** Phase `6` differential pipeline, starting with `6A` editor rect identities and per-slot rect updates.

---

## The Journey

### Session 1 — Caret & Architecture Foundations
**Starting point:** WebGPU viewer: file → Lezer parse → tokenize → GPU render → scroll/select.
**Goal:** Turn viewer into an editor.

- Explored production editor data structures (Gap Buffer, Piece Table, Rope, ProseMirror)
- **Chose:** Vector-of-strings (line-based) — matches existing line-based layout, Clojure persistent vectors are fast enough
- Proposed data model: `{:content :lines :tree :tokens :cursor :selection :scroll-y :dirty?}`
- Implemented blinking caret (530ms `m/ap` timer), click-to-place, selection/caret toggle

### Session 2 — Text Editing & Intelligence Research
- Full keyboard navigation: arrows, Home/End, sticky column (vim-style), auto-scroll
- Text editing: character insert, backspace (with line join), Enter (line split)
- Architecture: mutable atoms `!lines`, `!line-lengths`, `!text-geo`; text change → re-tokenize → re-layout → GPU update
- Researched code intelligence paths: Lezer capabilities, LSP, Clerk's approach, SCI-in-browser
- **Key decision:** SCI (Small Clojure Interpreter) for browser-side completions/eval (no server roundtrip)

### Session 3 — Bracket Matching & Code Folding
- Bracket matching via Lezer tree traversal → golden highlight rects
- Code folding: detect multi-line forms, 40px gutter with gold/blue indicators, click to fold/expand
- Line mapping system: visual line index ↔ logical line index (for hidden folded lines)
- Layout compaction: folded lines skip in y-positioning

### Session 4 — SCI Integration
- SCI evaluates Clojure in browser: parse, eval, namespace tracking, completions
- Eval results display inline next to code
- Architecture: Lezer (syntax) + SCI (semantics) + WebGPU (render), all client-side

### Session 5 — Core Editor Features
- Delete key (forward), Ctrl+Arrow word navigation
- Cut/Copy/Paste (Ctrl+X/C/V), Tab/indentation
- Undo/Redo stack, Save to file (Ctrl+S)

### Session 6–7 — UI System & TextInput Refactoring
- Command panel UI with keyboard input (Ctrl+Shift+P)
- Planned merge of editor + command panel into unified TextInput core
- Multi-focus system: `:editor` / `:command-panel` / `:settings`

### Session 8 — Reactive-First Architecture (The Great Refactor)
- **Multi-agent session:** Codex built TextInputCore, Claude reviewed, Gemini critiqued
- Refactored to pure reactive flows: `<editor-rects`, `<combined-text-ops`, `<cmd-panel-rects`
- All derived state as `m/latest` compositions — no imperative update-then-render
- Reducer returns new state; flows derive geometry; GPU consumes

### Session 10 — Critical Bug Fixes
- **`m/ap` + `m/?<` cancellation crash:** Multiple watches inside `m/ap` fed to `m/latest` killed the flow graph
- **Fix:** Replaced with `m/latest` for combining watches, `m/eduction` + `@deref` for event filtering
- **Char width mismatch:** `0.56` factor inconsistent between cursor positioning and text rendering → cursor drift
- These bugs became the **CLAUDE.md anti-patterns** to prevent recurrence

### Session 11 — Font Settings & MSDF Rendering
- Settings panel: font size, line height, px-range, sharpness sliders
- Multiple font support: Ubuntu Sans Mono, Ubuntu Mono, DejaVu Sans Mono, Noto Sans Mono
- MSDF atlas generation at size 128, pxRange 8
- Font switching via atom + watch pattern (avoids `m/ap` cancellation)
- Live preview: settings apply immediately, no "Enter to apply"

### Session 12 — Reactive Font Settings Fix
- Fixed call-site mismatches from Session 11 (wrong args to reactive flows)
- Made all mouse handlers, auto-scroll, cmd panel reactive to font changes
- Removed hardcoded `0.56` → uses `(:char-width active-font)`

### Sessions 13–14 — Text Crispness & MSDF Tuning
- **Root cause of blur:** Missing `screenPxRange >= 1.0` clamp in shader (tiny glyphs like quotes washed out)
- Pixel snapping (DPR-aligned) for scroll, layout, line height, char advance, hit testing
- Consistent metrics pipeline: manifest defaults = runtime settings = GPU geometry
- Diagnostics HUD for real-time tuning
- Regenerated all atlases at size 128

### Session 15 — Theme Switching (Zed-style Per-Instance Colors)
- **Problem discovered:** Colors computed correctly but discarded at GPU boundary (single uniform color)
- **Solution:** Expanded GPU instance data from 8 → 12 floats per glyph `[x,y,w,h,u0,v0,u1,v1,r,g,b,a]`
- Per-glyph color in vertex shader → fragment shader uses instance color
- Theme system: Gruvbox, Rose Pine, Kanagawa, etc. with live switching
- Data flow: `layout-tokens` (theme) → `render-ops` → `shape-text` → `GPU buffer` → shader

### Session 16 — File Explorer Sidebar
- **Ctrl+B** toggles sidebar: project picker → file tree → click to open in editor
- **Architecture:** Imperative DOM (`createElement`/`add-watch`) outside Electric DAG + HTTP API for file I/O
- HTTP API: `/api/home-dirs`, `/api/list-dir`, `/api/read-file` returning EDN via Ring middleware
- Lazy directory loading: `fetch-dir!` caches in `!dir-cache` atom, re-renders on response
- File open: `fetch-file!` → splits content into lines → resets `!editor-doc` via `!file-load-request`
- Removed Electric `FileTreeNode`/`LoadFile` e/defns (sidebar is fully imperative now)

### Session 16b — Render Loop CPU Optimization
- **Problem:** 45–76% CPU per core when idle (RAF loop doing unnecessary work every frame)
- **Diagnosis:** Added `!debug-counters` with batched 3s logging — revealed `draw-frame!` called 53×/sec even when nothing changed, and `(vec (concat ...))` + deep `=` on entire token tree every frame
- **Fix:** Two-level dirty checking:
  1. `identical?` on `world` object (O(1) pointer compare) → skips 98.5% of idle frames entirely
  2. `identical?` on flow sub-objects (`text-data`, `settings-text`) → avoids `vec`+`concat` reconstruction
  3. Skip `draw-frame!` GPU command submission when nothing changed
- **Result:** Idle CPU dropped from 45–76% to 0–6% per core; 398/404 frames skip all work

### Session 17b — Build & Lint Fixes
- Fixed `fetch-edn!` unresolved-symbol (clj-kondo) — replaced self-recursive multi-arity with non-recursive form
- Fixed build command: `clj -M:dev -m dev` (was pointing at nonexistent `user/main`)

### Session 18 — Architecture Audit: Closing the Rama Loop
- **Problem found:** AI interaction pipeline had two disconnected systems — working HTTP path bypassed Rama entirely
  - `$$cli-sessions-pstate` declared but never written to
  - `--resume` session-id support broken end-to-end (parser existed, never called)
  - Duplicate `provider-default-argv` in `server_jetty.clj` using `"text"` format (vs `"json"` in `objects.cljc`)
  - Context capture sent in POST body but ignored by server
- **Fix:** Wired `server_jetty.clj` to Rama:
  1. Before execution: look up session-id from `$$cli-sessions-pstate` for `--resume`
  2. Use `objects.cljc/provider-default-argv` with `"json"` format (enables session-id extraction)
  3. After execution: parse Claude JSON → extract session-id and clean content
  4. Store session-id in Rama via new `:update-cli-session` topology case
- **Result:** `--resume` works end-to-end; AI remembers per-file conversations across sessions
- Removed duplicate `provider-default-argv` from `server_jetty.clj`
- See `nxt_stp/session-2026-02-11-architecture-audit.md` for full details

### Session 19 — SSE Streaming & Structured Trail Rendering (Steps 0-3)
- **SSE streaming endpoint** (`/api/agent/stream`): token-level streaming via `ring.core.protocols/StreamableResponseBody`
- Stream event parser: normalizes Claude CLI `stream-json` output into 8 canonical event kinds (`:run-start`, `:text-delta`, `:tool-use-start`, `:tool-input-delta`, `:tool-result`, `:block-stop`, `:run-done`, `:run-error`)
- **Trail rendering** in agent panel: `:trail` + `:tool-buf` in `!agent-output`, per-kind colors
- Review pack API (`/api/review-pack/*`) for thread canvas sidebar mode

### Session 20 — V0 Flow State Machine (Step 4) — 2026-02-18
- **Planning commission:** Multi-LLM collaboration (Claude + Codex + Gemini) produced `commission-consensus.md`
- **Flow state machine:** Pure fns encoding Section 4 graph — `:idle` → `:bootstrapping` → `:intake` → `:run` → `:review` → `:rework` → `:finalize`, with back-edges and human override. (Note: `:arrange` was later removed in Session 35 — arrangement became an intake subphase.)
- **8 slash commands:** `/bootstrap`, `/select`, `/run`, `/review`, `/rework`, `/finalize`, `/status`, `/reset` (Note: `/arrange` removed and `/run-flow` renamed to `/run` in Session 35.)
- **Prompt templates:** `flow-prompt` composes Claude CLI prompts for each workflow action (bootstrap, sequential run, parallel run, rework, finalize)
- **JSON ticket parser:** `parse-tickets-from-output` extracts structured ticket data from Claude's Linear MCP responses (code block + bracket fallback)
- **Extracted `make-event-handler`:** DRY refactor — 3x duplicated stream handler → single reusable fn with `on-done` callback
- **Auto-bootstrap on load:** `setTimeout` at 1.5s fires `/bootstrap` with fresh/resume/no-cache semantics
- **`--allowedTools` support:** Read-only Linear MCP tools pre-approved in CLI args (not settings file) for security
- **Session continuity:** `session-id` captured from `:init`/`:run-start`/`:result` events, passed via `--resume` across entire workflow lifecycle
- **End-to-end validated:** Auto-bootstrap successfully pulled 21 real Linear tickets from `discourse-graph` project on first load
- Files changed: `loop.cljs` (~430 lines added), `objects.cljc` (3 lines), `server_jetty.clj` (1 line)

### Session 21 — Structured Output Pipeline (`--json-schema`) — 2026-02-24
- **Problem:** Bootstrap ticket parsing relied on Claude's free-text output — lossy (UUIDs instead of short IDs, string priorities, truncated descriptions). Raw MCP `tool_result` events never reach client.
- **Solution:** Wire `--json-schema` flag through the entire streaming pipeline for validated structured output.
- **`provider-default-argv`:** Added 4 new kwargs: `:json-schema`, `:max-turns`, `:model`, `:append-system-prompt` → each maps to its Claude CLI flag
- **Server (`server_jetty.clj`):** Extracts new fields from request, passes to `provider-default-argv`. Forwards Claude CLI's `:result` field in `:run-done` SSE event (was previously discarded).
- **Client (`loop.cljs`):**
  - `ticket-json-schema`: Schema constant — `{tickets: [{title, status, id?, assignee?, priority?, description?}]}`
  - `parse-structured-result`: New parser for `--json-schema` output with string/map polymorphism
  - `:run-done` handler stores `:structured-result` in `!agent-output`
  - `fire-flow-run!` extended to accept/forward `:json-schema`, `:max-turns`, `:model`, `:append-system-prompt`
  - Bootstrap `on-done` fallback chain: `structured → trail → text` (graceful degradation)
- **Files changed:** `objects.cljc` (docstring + 4 kwargs + 4 argv entries), `server_jetty.clj` (2 locations), `loop.cljs` (4 locations + 2 new defs)

### Session 22 — Direct Linear API + Rect Tree Design — 2026-02-25
- **Problem:** Bootstrap via Claude CLI took 30-300s (agentic loop calling MCP tools, multiple LLM round-trips). Also discovered `--max-turns` is not a real Claude CLI flag (caused silent process exit, breaking bootstrap).
- **Fix 1: `--max-turns` → `--max-budget-usd`** across all 3 files (the only real cost-limiting flag).
- **Fix 2: Direct Linear GraphQL API** — bypasses Claude CLI entirely for bootstrap:
  - Server: `fetch-linear-issues` in `server_jetty.clj` — `clj-http/post` to `api.linear.app/graphql`, uses `env/linear-api-key`, returns pre-normalized `{:ok true :tickets [...]}`
  - Route: `GET /api/linear/issues?team=DIS` — ~1-2 seconds vs 30-300s
  - Client: `/bootstrap` now does `js/fetch` → `reader/read-string` → populate `!flow-state :tickets` (no streaming, no agent output panel, no parsing)
- **Cleanup:** Removed verbose console logs (`[AGENT][RESULT]` was dumping full payloads, `[FLOW][DEBUG]` replaced with compact summary). Trimmed bootstrap success message (no more ticket title list in agent panel — data goes straight to list view).
- **Rect Tree UI Architecture:** Designed scene graph to replace flat ad-hoc rendering. Full design in `docs/architecture/rect-tree-ui.md`.
  - Everything is a nested rect with actions (click, drag, drop, scroll, zoom)
  - Tree walk → flat GPU vecs (same 8-float rects, 12-float glyphs)
  - Hit-test via depth-first tree walk (replaces manual cond chains)
  - Event dispatch with bubbling (replaces handler logic in cond)
  - Drag state machine: IDLE → PENDING → DRAGGING → DROP
  - Three DnD contexts: within-panel, within-canvas, cross-panel
  - Maps directly to ZUI vision: tree depth = zoom granularity
- **Files changed:** `objects.cljc`, `server_jetty.clj` (new endpoint + Linear API fn), `loop.cljs` (bootstrap rewrite)
- **NEXT:** Implement rect tree, starting with data structure + tree walk (step 1 of 7)

### Session 23 — Rect Tree Implementation (Steps 1-7) — 2026-02-25
- **All 7 steps done in one session:** data structure, tree walk, hit-test, click dispatch, drag state machine, drop zones, visual feedback
- **Core fns:** `rt-node`, `tree->rects`, `tree->text-ops`, `tree->shadows`, `hit-test`, `dispatch-event`
- **`build-intake-tree`:** Scene graph for Screen 1 intake — replaces flat `compute-ticket-list-*` fns
- **Drag state machine:** `!drag-state` atom, IDLE → PENDING → DRAGGING → DROP, 5px threshold
- **Cross-panel DnD:** left→right = select ticket, ghost on original, floating ticket at cursor
- **GPU unchanged:** tree is a compute layer between atoms and GPU (same 8-float rects, 12-float glyphs)

### Session 24 — SDF Rich Quads + Shadows — 2026-02-27
- **Rect shader rewrite:** SDF-based fragment shader (Inigo Quilez `sd_rounded_box`)
- **Buffer expanded:** 8 → 28 floats/rect (112 bytes) — geometry, color, corner_radii, border_widths, border_color, gradient, gradient_color2
- **Shadow pipeline:** Separate render pass, analytical Gaussian blur (erf-based), 20 floats/shadow
- **Design tokens:** `dt` def — Linear/shadcn dark theme (colors, spacing, radii, shadows, font-sizes)
- **Component library:** `ui-card`, `ui-badge`, `ui-button`, `ui-divider`, `ui-progress`, `ui-scrollbar`, `ui-tabs`, `ui-tooltip`

### Session 25 — Layout Engine + Composable Sidebar — 2026-02-27
- **Layout engine:** `normalize-padding`, `layout-children`, `resolve-text-layout`, `resolve-layout`
- **`:layout` on rt-node:** `{:direction :column/:row :gap :padding :align :auto-height?}` — CSS flexbox equivalent
- **Composable panel components** (shadcn Sidebar pattern):
  - Container: `ui-panel`, `ui-panel-header`, `ui-panel-content`, `ui-panel-footer`
  - Group: `ui-panel-group` (collapsible labeled section)
  - Item: `ui-list-item` (leading/title/trailing slots), `ui-checkbox`, `ui-priority-dot`
- **`build-intake-tree` rewritten:** left pane uses composable components (~60% shorter)
- **Visual tuning:** taller rows (24→34px), rounded hover/selected backgrounds (inset pill), muted section labels (10px gray), bigger checkboxes (14px, accent border), group gaps (8px)
- **Design docs:** `docs/architecture/virtual-layout-engine.md`, `docs/architecture/gpu-component-library.md`

### Session 26 — WebGPU-Native Sidebar (DOM → GPU migration) — 2026-02-27
- **Problem:** Session 16 sidebar was imperative DOM (`createElement`/`add-watch`) — visual inconsistency (HTML text vs WebGPU MSDF text), not composable with rect tree UI, required separate event routing
- **Solution:** Full rewrite as WebGPU rect tree, same architecture as `build-intake-tree`
- **`electric_flow.cljc` simplified:**
  - Removed `#file-sidebar` DOM div (sidebar constants + entire `dom/div` element)
  - Canvas changed from `flex: 1` to `width: 100vw` (full viewport, no flex partner)
  - Outer container simplified from 2-child flex to single `dom/canvas`
- **New constants** (loop.cljs ~line 938): `sidebar-w` (256), `sidebar-tab-h`, `sidebar-row-h`, `sidebar-back-h`, `sidebar-breadcrumb-h`, `sidebar-indent-px`, `sidebar-padding-x`, `sidebar-item-inset`, `sidebar-font-size`
- **New helpers:**
  - `flatten-file-tree` — recursively flattens dir-cache tree into flat row descriptors with depth
  - `compute-sidebar-content-height` — total content height for scroll clamping
  - `offset-rects`, `offset-shadows`, `offset-text-ops` — shift GPU coordinates by `sidebar-w`
- **`build-sidebar-tree`** (~line 1490): pure fn building sidebar as rt-node tree
  - File explorer only (no tabs) — 2 modes: home dirs list, file tree
  - Hover, active file highlight, chevron icons, depth-based indentation
  - Scroll via inner container offset within clipped parent
- **Consolidated atom `!sidebar-state`:** 7 keys: `:project`, `:expanded-dirs`, `:dir-cache`, `:home-dirs`, `:scroll-y`, `:hovered-id`, `:loading?`
- **Modified reactive flows:** `<editor-rects` builds sidebar rects + offsets content; `<combined-text-ops` builds sidebar text ops + offsets content; `<cmd-panel-rects` offsets caret
- **Modified event handlers:** scroll routes to sidebar when `mouse-x < sidebar-w`; mousedown hit-tests sidebar tree; mousemove tracks `!mouse-x` + sidebar hover
- **Fetch fns** (`fetch-home-dirs!`, `fetch-dir!`): swap `!sidebar-state` directly (no render callback)
- **Removed ~470 lines:** `render-sidebar!` (423 lines DOM manipulation), 11 `add-watch` registrations, `sidebar-el` binding, individual sidebar atoms
- **Files changed:** `electric_flow.cljc` (simplified layout), `loop.cljs` (sidebar rewrite + offset integration)

---

## Current Architecture

```
┌─────────────────────────────────────────────────────┐
│                  Browser (Client-Side)               │
├─────────────────────────────────────────────────────┤
│  Lezer Parser     SCI Interpreter     WebGPU Render  │
│  ├─ Syntax HL     ├─ Eval             ├─ MSDF Text   │
│  ├─ Brackets      ├─ Completions      ├─ Caret       │
│  ├─ Fold regions  ├─ Var lookup       ├─ Selection    │
│  └─ Indentation   └─ Namespaces       └─ Fold gutter │
├─────────────────────────────────────────────────────┤
│  Missionary Reactive Flows (m/latest, m/eduction)    │
│  ├─ <editor-rects      (selection, caret, brackets,  │
│  │                       sidebar rects + offset)      │
│  ├─ <combined-text-ops  (editor + cmd + agent +       │
│  │                       sidebar text + offset)       │
│  ├─ <cmd-panel-rects    (command palette geometry)    │
│  └─ <world-snapshot     (full render state for GPU)   │
├─────────────────────────────────────────────────────┤
│  State Atoms: !editor-doc, !settings, !active-font,  │
│    !focus, !ai-provider, !agent-output, !cmd-panel,   │
│    !flow-state, !sidebar-state (consolidated)         │
└──────────────────────┬──────────────────────────────┘
                       │ SSE POST /api/agent/stream (token-level)
                       │ HTTP POST /api/agent/run (blocking fallback)
                       │ Electric 3 (file content, one-shot)
┌──────────────────────┴──────────────────────────────┐
│  Server: Ring/Jetty                                  │
│  ├─ File API (/api/read-file, /api/list-dir)         │
│  └─ Agent API (/api/agent/run → CLI process)         │
│       ├─ Rama session lookup (--resume injection)     │
│       ├─ Claude JSON parsing (session-id extraction)  │
│       └─ Rama session store (for next --resume)       │
├─────────────────────────────────────────────────────┤
│  Rama (Event-Sourced State)                          │
│  ├─ *node-events-depot (all events)                  │
│  ├─ $$cli-sessions-pstate {file→provider→session-id} │
│  ├─ $$agent-runs-pstate {run-id→metadata}            │
│  ├─ $$nodes-pstate, $$dg-*-pstate (graph data)       │
│  └─ Stream topology: :cli-command, :update-cli-       │
│     session, :agent-run, :llm-request, :new-node...  │
└─────────────────────────────────────────────────────┘
```

### GPU Text Pipeline (Zed-style)
```
layout-tokens (per-token theme color)
  → render-ops [{:text "defn" :r 0.5 :g 0.7 :b 0.4 ...}]
  → shape-text → {:vertices [x,y,w,h,u0,v0,u1,v1] :color [r,g,b,a]}
  → GPU buffer (12 floats/glyph) → vertex shader → fragment shader
  → Per-glyph colored MSDF text
```

---

## Key Architecture Decisions & Why

| Decision | Alternatives Considered | Why This |
|----------|------------------------|----------|
| Vector-of-strings buffer | Gap buffer, Piece table, Rope | Line-based layout already works; Clojure vectors O(log32 n) |
| SCI for code intelligence | LSP, no intelligence | Runs in browser, instant feedback, no server roundtrip |
| Lezer for syntax | Tree-sitter, manual | Already included, incremental parsing, Clojure + markdown |
| `m/latest` over `m/ap` | `m/ap` + `m/?<` | `m/ap` cancels branches → crashes flow graph |
| Zed-style per-glyph color | Uniform color, batch by color | Single draw call, theme colors flow through reactive pipeline |
| Atom + watch for font loading | `m/ap` async flow | Avoids cancellation; async `.then` resets atom safely |
| MSDF rendering | Bitmap fonts, SDF | Resolution-independent, one atlas per font, GPU-native |
| `identical?` dirty check | Deep `=` comparison | O(1) pointer compare vs O(n) structural; `m/latest` caches objects |

---

## Lessons Learned (Bug Patterns)

### 1. Missionary `m/ap` Cancellation (Session 10)
**Pattern:** `m/ap` + multiple `m/?<` inside flows fed to `m/latest`
**Symptom:** "Watch cancelled" crash
**Fix:** Use `m/latest` for combining, `m/eduction` + `@deref` for filtering
**Now in CLAUDE.md** as a hard rule.

### 2. Char Width Consistency (Session 10, 12)
**Pattern:** Hardcoded `0.56` in one place, different value elsewhere
**Symptom:** Cursor drifts from text
**Fix:** Single source of truth via `(:char-width active-font)` atom

### 3. MSDF Tiny Glyph Blur (Session 13)
**Pattern:** Missing `screenPxRange >= 1.0` clamp
**Symptom:** Quotes, commas blurry
**Fix:** Clamp + use actual atlas size (not hardcoded 64)

### 4. Reactive Call-Site Drift (Session 12)
**Pattern:** Refactor function signatures but forget to update call sites
**Symptom:** Wrong values at wrong positions, silent corruption
**Fix:** Always trace the full call chain after signature changes

### 5. Initial vs Runtime Geometry Mismatch (Session 13)
**Pattern:** `electric_flow.cljc` initial layout uses different defaults than `loop.cljs` runtime
**Symptom:** Text "jumps" on startup
**Fix:** Manifest defaults mirror runtime defaults

### 6. RAF Loop Deep Equality Tax (Session 16b)
**Pattern:** Reconstructing data (`vec`+`concat`) then comparing with `=` every frame
**Symptom:** 45–76% idle CPU — deep `=` on 100K+ token maps 53×/sec, plus unconditional `draw-frame!`
**Fix:** `identical?` on `m/latest` cached objects (O(1) pointer compare); skip `draw-frame!` when nothing changed

### 7. Disconnected Parallel Systems (Session 18)
**Pattern:** Working HTTP handler bypasses Rama entirely; Rama scaffolding (PStates, topology cases, utility functions) exists but is never called
**Symptom:** AI interactions are fire-and-forget; session-ids never stored; `--resume` broken; refresh loses everything
**Fix:** Wire the HTTP handler to Rama: session-id lookup before execution, JSON parsing after, depot-append to store results
**Lesson:** When building incrementally, it's easy to scaffold the "right" system (Rama event-sourced) alongside a "quick" system (HTTP + atom) and forget to connect them. The scaffolding looks like progress but is dead code until wired.

### 8. Horizontal Scroll Coordinate Contamination (Session 30)
**Pattern:** `scroll-x` subtracted from `layout-x` at the top of `<combined-text-ops`, making it a global shift that affected ALL text — editor, line numbers, right-pane content — instead of just the editor text.
**Symptom:** Scrolling right made sidebar text disappear, line numbers slide off-screen, editor text bleed into sidebar area. No left-side clipping meant text rendered in the gutter zone.
**Fix:** Split into `layout-x` (unscrolled, for gutter/line-nums) and `editor-lx` (scrolled, for code text). `clip-sub` clips to `[layout-x, code-w]` with left-trim + right-truncate. `compute-editor-rects` takes `:gutter-lx` kwarg so fold indicators stay fixed. Scroll events restricted to editor pane area.
**Lesson:** When adding a per-element transform (h-scroll), never redefine a shared coordinate variable. Create a NEW variable for the scrolled coordinate and use it only where needed. The gutter is a "fixed column" — it should never participate in horizontal scroll.

### 9. Expensive Computation in Blink-Triggered Flows (Session 17)
**Pattern:** `m/latest` flow watches both document AND visual state (blink timer); inner function performs full-document operations (Lezer parse, tree walk, line mapping)
**Symptom:** 1440ms LONGTASK every 530ms when idle with large file; debug counters show RAF is fast, misleading because expensive work is in Missionary reactor propagation (outside RAF)
**Fix:** Separate document-level computation (folds, line mapping) from visual-state flows (blink, caret). Cache document-level results in their own flow that only watches `!editor-doc`.

### 10. Missionary Event Buffering Kills Raw DOM State (Session 35)
**Pattern:** Shared mutable atom (`!dragging?`) written by both raw DOM event listeners and Missionary's `m/reduce` consumer of `m/observe`. Raw mousedown sets atom to `true`, but Missionary's internal buffering drains a *previously queued mouseup* synchronously during the same event dispatch, resetting it to `false` before any mousemove fires.
**Symptom:** Drag-select never worked — `!dragging?` flipped `true→false` within ~1ms, no mousemove events were processed, no selection appeared.
**Root cause stack trace:** `down_h@events.cljs:54 → Observe.cljs:37 → Reduce.cljs → runtime.cljs:1506 (reset! !dragging? false)`. The Missionary mousedown observer triggered processing of a buffered mouseup from the *previous* click.
**Fix:** Move drag-select entirely to raw DOM `addEventListener` (mousedown on canvas, mousemove/mouseup on window). Remove ALL Missionary writes to `!dragging?`. Guard Missionary mousedown cursor-placement with `(when-not @!drag-start ...)` to prevent double-processing.
**Lesson:** When mixing raw DOM handlers and Missionary `m/observe` consumers for the same events, **never share mutable state between them**. Missionary's `m/observe` without `m/relieve` buffers one event — calling `!` stores the new value but may drain the *previously buffered* value first. If you move state ownership to raw DOM, remove ALL Missionary writes to that state.

### 11. Selection Rect Overflow Into Adjacent Panes (Session 35)
**Pattern:** Selection highlight rects used `(* width-chars char-w)` for width with no upper bound. In 3-pane layout (editor 40%, chat 55%, preview 5%), selected lines extending past the editor boundary rendered selection highlights over the chat panel.
**Symptom:** Blue selection rectangles visible in the chat pane area.
**Fix:** Clamp selection rect width: `(min raw-w (max 0 (- viewport-w x)))` where `viewport-w` is the editor pane width (`code-w`).
**Lesson:** Any rect computed from text character positions must be clipped to its containing pane boundary. The current-line highlight already did this (`:w viewport-w`), but selection rects didn't.

---

## Current Feature Status

| Feature | Status |
|---------|--------|
| WebGPU MSDF text rendering | ✅ |
| Lezer syntax highlighting | ✅ |
| Per-glyph theme colors (Zed-style) | ✅ |
| Theme switching (Gruvbox, Rose Pine, etc.) | ✅ |
| Keyboard navigation (arrows, Home/End, Ctrl+Arrow) | ✅ |
| Text editing (type, backspace, delete, enter) | ✅ |
| Cut/Copy/Paste, Undo/Redo | ✅ |
| Mouse selection (click, drag) | ✅ |
| Bracket matching (golden highlights) | ✅ |
| Code folding (gutter indicators) | ✅ |
| SCI browser-side eval | ✅ |
| Command panel (Ctrl+Shift+P) | ✅ |
| Font settings panel (size, line-height, sharpness) | ✅ |
| Multi-font support (4 monospace fonts) | ✅ |
| Pixel snapping (DPR-aligned) | ✅ |
| Diagnostics HUD | ✅ |
| Save to file (Ctrl+S) | ✅ |
| File explorer sidebar (Ctrl+B, WebGPU-native) | ✅ |
| Open files from disk | ✅ |
| AI command panel (Ctrl+K) | ✅ |
| Multi-provider AI toggle (/provider) | ✅ |
| AI output rendering in WebGPU | ✅ |
| Rama session-id persistence (--resume) | ✅ |
| Rect tree scene graph (rt-node, hit-test, DnD) | ✅ |
| SDF rich quads (rounded corners, borders, gradients) | ✅ |
| Shadow pipeline (analytical Gaussian blur) | ✅ |
| Layout engine (auto-positioning, CSS flexbox equivalent) | ✅ |
| Composable panel components (shadcn Sidebar pattern) | ✅ |
| Click-to-navigate (trail tool cards → file:line) | ✅ |
| Keyboard ticket navigation (visual order) | ✅ |
| Vertical text clipping in rect tree | ✅ |
| Vim modal editing | 🔲 |
| Structural editing (paredit) | 🔲 |
| Multi-file / tabs | 🔲 |
| Font atlas reload on font switch | 🔲 (char-width changes, glyphs don't yet) |
| localStorage persistence | 🔲 |

---

## Files (Only These Matter)

| File | Role |
|------|------|
| `src/app/electric_flow.cljc` | Entry point, server file I/O, Lezer parsing, tokenization, layout |
| `src/app/client/webgpu/editor.cljs` | WebGPU shaders, GPU pipeline, text shaping, hit-testing, rects |
| `src/app/client/webgpu/loop.cljs` | Missionary event loop, state management, reactive flows, UI panels, sidebar |
| `src/app/file_viewer.cljc` | Server-side file I/O (list dirs, read files) + Electric bridge fns |
| `src/app/server_jetty.clj` | Ring/Jetty server, HTTP API for file explorer + agent runner |
| `src/app/server/rama/core.clj` | Rama module: depots, PStates, stream topology |
| `src/app/server/rama/objects.cljc` | TaskGlobals, CLI process execution, provider argv building |
| `src/app/server/rama/util_fns.cljc` | Foreign PState/depot bindings, helper functions for depot appends |

---

---

## ✅ RESOLVED: Large File Idle CPU (Session 17)

**Root cause:** `compute-editor-rects` calls `detect-fold-regions` (full Lezer re-parse) and `find-matching-bracket` (second full Lezer re-parse) on **every blink tick** (530ms) for ALL 5420 lines. Each call takes ~1440ms, blocking the main thread continuously.

**Why counters were misleading:** The expensive work happens in **Missionary's `m/latest` reactor propagation** (synchronous, outside RAF). The RAF callback only reads the pre-computed result (~0.4ms), so `[FRAME]` stats looked fast. `text-ops=0` was correct because `<combined-text-ops` (the optimised flow) was idle — the problem was in `<editor-rects` → `compute-editor-rects`.

**Fix:** Extract fold regions and line mapping into a cached flow that watches only `!editor-doc` (not `!caret-visible`). Skip fold detection for large files. Cache bracket matching.

### Session 17 — Chrome Trace Analysis & Root Cause
- Analyzed 75s Chrome DevTools trace (354K events, 520K CPU profiler samples)
- Found 36 "mega-frames" of ~1.5s each running back-to-back
- Profiler hot spots: `cljs.core.some`, `contains?`, `detect_fold_regions`, `offset->line-col`, `find_matching_bracket`
- All called from `compute-editor-rects` via `<editor-rects` on every blink tick
- See `optimisations.md` for full investigation timeline and trace analysis

### Session 27 — Design Converter Pipeline (Multi-LLM) — 2026-03-01
- **Goal:** Generalized system to extract UI components from any rendered web page and compile into rt-node specs
- **Multi-LLM collaboration:** Claude (parsers + compiler + extractor), Codex (IR schema + verifier + blueprints), Gemini (LLM extraction prompt)
- **Two extraction paths:**
  1. **Deterministic** (`_extractor.js`): `getComputedStyle()` DOM walk → raw JSON → CSS parsing → token matching → compile → rt-node
  2. **LLM-assisted** (`_extractor_prompt.md`): Feed component source to LLM → Design IR directly (with states, slots, token refs)
- **New files (all under `components/`):**
  - `_css_parsers.cljc` (436 lines) — 12 CSS string → Clojure value parsers (color, shadow, gradient, layout, etc.)
  - `_token_matcher.cljc` (228 lines) — fuzzy match literal CSS values → nearest `dt` design tokens
  - `_compiler.cljc` (352 lines) — Design IR → rt-node tree compilation (handles both IR and direct rt-node input)
  - `_extractor.js` (115 lines) — browser DOM walker via `getComputedStyle()`, wrapper collapsing
  - `_design_ir.cljc` (717 lines, Codex) — strict IR schema validator
  - `_verifier.cljc` (560 lines, Codex) — geometry/style/structure diffs with pass/fail thresholds
  - `_extractor_prompt.md` (113 lines, Gemini) — LLM prompt targeting Design IR output (data, not code)
  - `by-codex/blueprints/` — seed blueprints (button 3-state, sidebar composition)
- **Key design decisions:**
  - IR format: `{:tag :role :visual {:fill :radius :border :shadow :gradient} :typography :layout :states :slots :children}`
  - Shadow conversion: IR `{:offset [x y]}` → rt-node `{:offset-x N :offset-y N}`
  - Dual-input compiler: detects rt-node vs Design IR via `is-rt-node?`, passes through blueprints unchanged
  - Token matching: Euclidean color distance with alpha 2x weighted, threshold 0.08
  - All colors normalize to `[r g b a]` with 0-1 floats
- **Pipeline:** Browser DOM → `_extractor.js` → JSON → `_css_parsers` → `_token_matcher` → `_compiler` → rt-node → `resolve-layout` → `tree->rects` / `tree->text-ops` → GPU
- **No existing files modified** — entire pipeline is additive

### Session 28 — Sidebar Simplification + 3-Pane Default + Idle Start — 2026-03-02
- **Goal:** Simplify sidebar (file explorer only), make 3-pane layout default for open files, gate ticket view behind `/dg`
- **Sidebar simplified:**
  - Removed `ui-tabs` (Files/Review/UI) — no tab bar, content fills full height
  - Removed `:review-packs` and `:components` cond branches from `build-sidebar-tree`
  - `!sidebar-state` reduced from 18 keys to 7 (removed `:mode`, 6 `:review-pack-*`, 3 `:component-*`)
  - Deleted `fetch-component-registry!`, `fetch-review-packs!`, `fetch-review-pack-summary!`, `build-review-pack-canvas-model`, `node-by-id`, `compact-text`
  - Removed `:tab` click handler, component-mode click block, review-packs fetch branches
- **3-pane layout generalized:**
  - Replaced `build-component-layout` with `build-file-layout` (~line 1698)
  - Activates when `(some? current-file)` — any open file, not just components
  - Layout: Editor (40%) | Chat (30%) | Preview (30%)
  - Chat pane shows `agent-output` trail; Preview shows "No preview" placeholder
  - Wired into both `<editor-rects` and `<combined-text-ops` with `file-open?` gating
- **Ticket view gated:**
  - `initial-flow-state` returns `{:node :idle}` instead of `{:node :intake}`
  - `flow-canvas-active?` (`#{:intake :arrange}`) returns `false` for `:idle` (Note: later expanded to `#{:intake :run :review :rework :finalize}` in Session 35)
  - `/bootstrap` transitions `:idle → :bootstrapping → :intake` (unchanged)
- **~540 lines removed**, file went from 6275 → 5735 lines
- **Files changed:** `loop.cljs` only

### Session 29 — UI Excellence Pass (Phases 1–2) — 2026-03-02
- **Goal:** Bring workspace shell from 5/10 → 8.5/10 visual quality. Fix rendering bugs, add depth hierarchy, typography system, empty states, horizontal scrolling.
- **Phase 1 — Rendering Integrity (P0):**
  - Normalized `char-width` to `0.56` across 3 files: `electric_flow.cljc:413`, `loop.cljs` fallback manifest + fallback font (was `0.60` in two places → cursor drift on long lines)
  - Fixed text clipping in `<combined-text-ops`: replaced hardcoded `0.56` with `(:char-width active-font)`, added per-glyph truncation for grouped ops (line numbers)
  - Fixed `tree->text-ops`: added clip-right truncation — text extending beyond clip bounds now character-truncated
  - Added typography hierarchy: `typo-title` (20px/1.0), `typo-subtitle` (16px/0.9), `typo-body` (14px/0.85), `typo-caption` (12px/0.6)
  - Applied typography constants to: all 3 pane headers, sidebar explorer/back-button/breadcrumb, intake tree header, right-detail (4 states), multi-select detail
- **Phase 2 — Space & Depth (P1):**
  - Added `:surfaces` key to `design_tokens.cljc`: 9 elevation tokens (sunken, base, elevated, hover, active, text-primary, text-secondary, text-muted, accent)
  - Removed 1px full-height pane dividers between editor/chat/preview — replaced by surface color contrast
  - Added `!active-pane` atom (`:editor` | `:chat` | `:preview`): wired through `m/watch` into `<editor-rects` (23 args) and `<combined-text-ops` (22 args)
  - Focus indicator: 2px bottom accent underline (40px wide) on focused pane header
  - Click handler: determines clicked pane from `rel-x` position, sets `!active-pane`
  - Keyboard shortcuts: `Cmd+1`/`2`/`3` → pane focus via `parse-key-event` + global event handler
  - Pane headers: 32px → 36px height, 12px → 16px padding (4px grid rhythm)
  - Sidebar: `sidebar-item-inset` 6→8px, highlight radius `:md`→`:sm` (4px), accent bar 3→2px width
  - `build-empty-state` reusable helper: centered icon + headline + description
  - Chat empty: `"--"` + `"No session"` + guidance text. Preview empty: `"[]"` + `"No preview"` + guidance
  - Right-detail empty states: replaced card-based with `build-empty-state` (`"{}"` / `"<>"` icons)
  - Command bar: bg updated to `surface-elevated` color
  - Chat header: status accent bars (blue streaming, green complete, red failed)
  - Bottom clip: editor text ops filtered to not bleed into cmd-panel/status-bar zone
  - Horizontal scrolling: `>wheel` emits `{:dy :dx :shift?}`, new `!scroll-x` atom, shift+wheel or trackpad dx, layout-x offset by `-scroll-x` in text ops + editor rects + click/drag handlers
- **Files changed:** `loop.cljs`, `electric_flow.cljc`, `design_tokens.cljc`
- **Net new atoms:** `!active-pane`, `!scroll-x`
- **Key fns added:** `build-empty-state`, `clip-bottom`
- **Key fns modified:** `build-file-layout` (accepts `:active-pane` kwarg), `>wheel` (map output), `parse-key-event` (Cmd+1/2/3), `tree->text-ops` (clip-right), all empty state builders

### Session 30 — Horizontal Scroll Coordinate Fix — 2026-03-03
- **Problem:** Session 29 h-scroll subtracted `scroll-x` from `layout-x` at the top of `<combined-text-ops`, making it a global shift affecting ALL text — editor, line numbers, sidebar, right-pane — instead of just editor code text.
- **Symptom:** Scrolling right made sidebar text disappear, line numbers slide off-screen, editor text bleed into gutter area. No left-side clipping.
- **Fix:** Split into two coordinate systems:
  - `layout-x` (unscrolled) — for gutter, line numbers, fold indicators
  - `editor-lx = layout-x - scroll-x` (scrolled) — for editor code text only
  - `clip-sub` clips text to `[layout-x, code-w]` with left-trim + right-truncate
  - `compute-editor-rects` takes `:gutter-lx` kwarg so fold indicators stay fixed
  - Scroll events restricted to editor pane area (`mouse-x < editor-right`)
- **Lesson:** When adding a per-element transform (h-scroll), never redefine a shared coordinate variable. Create a NEW variable for the scrolled coordinate.
- **Files changed:** `loop.cljs`

### Session 31 — 3-LLM Thread Map Synthesis — 2026-03-03
- **Goal:** First complete archaeological reconstruction of the project across 30 sessions. Three LLMs (Claude, Codex, Gemini) independently wrote thread maps, then synthesized.
- **Key finding:** Reasoning trail infrastructure is ~70% built, not ~10% as docs claimed. `trail->chat-nodes` (250 lines), `group-trail-blocks`, tool cards, per-kind colors, shimmer all exist. The 5-step spike plan in `where-we-are.md` described work already done.
- **Stale docs discovered:** 5 docs found to be stale (where-we-are.md Reasoning Trails, Component Library, auto-bootstrap; _map.md; progressive-summary.md itself)
- **Status conflicts resolved:** All 3 LLMs agreed on 4 main conflicts + Codex found 2 additional (auto-bootstrap semantics drift, char-width 0.60 remnant at `electric_flow.cljc:466`)
- **Execution path debate:** 3-way consensus — Claude (test trails thesis) vs Gemini (UI Excellence Phase 3) vs Codex (Screen 1 gate first). User selected Claude's recommendation: test the trails thesis before building anything else.
- **Files produced:** `thread-map-claude.md`, `thread-map-codex.md`, `thread-map-gemini.md`, `thread-map-gemini-v1.md`, `consensus-next-execution-path.md`, session log

### Session 32 — Reasoning Trails Thesis Test — 2026-03-03
- **Goal:** Execute the 3-LLM consensus decision: test the reasoning trails thesis before building anything else. 4 planned steps + 6 reactive fixes from live testing.
- **Step 4 (char-width fix):** `electric_flow.cljc:466` had `0.60` while all other sources used `0.56` — fixed. Eliminates text-jump on initial load.
- **Step 1 (click-to-navigate):** The killer feature — clicking a tool card in the chat pane opens the referenced file at the relevant line.
  - `:nav` data on rt-nodes: `trail->chat-nodes` annotates tool-card nodes with `{:nav {:file-path "..." :line N}}` extracted from tool input (`:file_path`, `:path`, `:offset`, `:line`)
  - Visual affordance: navigable cards get brighter border (`(:border colors)` vs `(:border-subtle colors)`)
  - `fetch-file!` extended: accepts optional `:target-line` kwarg, passes through `!file-load-request`
  - FILE LOAD CONSUMER extended: reads `:target-line`, positions cursor at that line, scrolls with ~100px top margin
  - Mousedown handler: two-pass hit-test — Priority 1: `:nav` data → navigate to file:line. Priority 2: `:collapse-id` → toggle collapse. Navigation takes precedence so Read/Edit/Write cards navigate, while Thinking/Grep cards collapse.
  - Path comparison: handles relative vs absolute paths via `str/ends-with?`
  - Same-file optimization: repositions cursor + scroll without reloading
- **Step 3 (keyboard nav):** Up/Down arrows in cmd panel navigate tickets in **visual order** (grouped by status, not flat vector order). Enter on hovered ticket toggles selection. Visual order computed via `group-tickets-by-status` → `mapcat :idx`.
- **Bug fix: Agent CWD** — was using file parent dir (`src/app/`), now uses sidebar project root path first (`:path (:project @!sidebar-state)`). Agent searches the whole project, not just the file's directory.
- **Bug fix: Bottom panel duplication** — agent output panel (text + background rect + scroll height) suppressed when 3-pane layout active (`file-open?`). Chat pane is the sole trail display. Required adding `!current-file` as parameter to `<cmd-panel-rects` (11 params, 11 watches).
- **Bug fix: Vertical text clipping** — `tree->text-ops` only had horizontal clipping (`clip-right`). Added `clip-top` and `clip-bottom` to the `in-clip?` predicate, preventing chat pane text from bleeding past pane boundaries.
- **Bug fix: Click priority** — original two-pass (collapse first, nav second) meant `:tool-header` (28px of 34px card) always caught clicks, making navigation unreachable. Reversed to nav-first, collapse-second.
- **Files changed:** `loop.cljs` (~160 lines net new), `electric_flow.cljc` (1 line)
- **Persistent command bar**: When file open, cmd panel is always visible (can't Ctrl+K dismiss or Escape close). `panel-visible? = (or (:visible panel) (some? current-file))`. Escape just unfocuses; empty Enter unfocuses without hiding. Provides a permanent chat input at the bottom of the 3-pane layout.
- **Chat pane scrolling**: New `!chat-scroll-y` atom. Wheel events over chat pane route to it (hit-test: `rel-mx` in `[code-w, code-w+chat-w]`). `build-file-layout` accepts `:chat-scroll-y` kwarg — replaces forced auto-scroll-to-bottom with interactive `(min chat-scroll-y max-chat-scroll)`.
- **Viewport pinning**: 3-pane layout rects/shadows/text-ops all get `(update :y + scroll-y)` so the file layout stays screen-pinned while the editor scrolls beneath. Without this, scrolling the editor would move the chat pane off-screen.
- **Editor pane scroll isolation**: `when` guard on editor scroll checks mouse isn't over preview/chat pane. Prevents editor from scrolling when user scrolls in other panes.
- **Header clip-top**: `clip-sub` in `<combined-text-ops` adds `clip-top = scroll-y + header-h` to prevent editor text from rendering above the 36px pane header. Line numbers also clipped via `clipped-ln` filter.
- **Key insight:** Trail infrastructure WAS 70% built (confirmed). Click-to-navigate was the missing 30% that transforms trails from passive log → interactive navigation artifact.

### Session 35 — Drag-Select, Chat Click-Through, Selection Clipping — 2026-03-09
- **Goal:** Fix two post-refactor runtime regressions: drag-select broken, chat panel clicks passing through to editor
- **Chat click-through root cause:** Missing `)` at line ~1286 in `runtime.cljs` put the `:else` cond branch inside a `let` body. Every click with a file open ran BOTH the 3-pane routing AND unconditional editor cursor placement.
- **Chat scroll width:** Was using `0.3` instead of `0.55` for chat pane width in scroll hit-test — mismatched with `shell.cljs` layout.
- **Drag-select root cause:** Missionary's `m/observe` (without `m/relieve`) buffers events. When raw DOM mousedown fires and sets `!dragging?=true`, Missionary simultaneously drains a previously buffered mouseup, hitting `(reset! !dragging? false)` in the Missionary mouseup handler — all within the same synchronous event dispatch. Diagnosed via `add-watch` + `js/console.trace` on the `!dragging?` atom.
- **Drag-select fix:** Moved drag-select to raw DOM `addEventListener` (mousedown on canvas, mousemove/mouseup on window). Removed Missionary's `(reset! !dragging? false)` from the `:mouseup` handler. Added `(when-not @!drag-start ...)` guard on Missionary mousedown cursor placement.
- **Selection highlight overflow:** Selection rects used raw `(* width-chars char-w)` width. Clamped to `viewport-w` (= `code-w` in 3-pane mode) via `(min raw-w (max 0 (- viewport-w x)))`.
- **Files changed:** `runtime.cljs`, `editor_compute.cljs`
- **Key lesson:** Never share mutable atoms between raw DOM handlers and Missionary `m/observe` consumers. Missionary's event buffering causes out-of-order state mutations that are invisible without stack traces.

### Session 35b — Plugin Workflow V0: Stack-Native Dogfooding Loop — 2026-03-09
- **Ontological shift:** 3-way synthesis (Claude + Codex + user) reframed the DG workflow through the epistemic loop: probe (intake) → response (run) → projection (review) → revision (rework). Clarified "discourse graph" terminology: the plugin (user's day job) vs the data model (Softland's future native protocol).
- **`:arrange` removed as state node.** Arrangement became an intake subphase — spatial and keyboard-native. Selected tickets appear as an ordered execution stack in the right pane. Shift+Arrow reorders, Enter triggers `/run`.
- **Object model locked:** batch (work session) → lane (ordered ticket) → run (execution attempt) → artifact (trail/diff/tests) → decision (approve/rework/finalize). Flow-state restructured with `:batch {:lanes [...]}`, `:active-lane-idx`, `:runs`, `:decisions`.
- **Commands simplified:** `/arrange` removed, `/run-flow` renamed to `/run`. 8 commands remain: `/bootstrap`, `/select`, `/run`, `/review`, `/rework`, `/finalize`, `/status`, `/reset`.
- **`flow-canvas-active?` expanded:** Now covers `#{:intake :run :review :rework :finalize}` — the master-detail layout persists across all active flow states. Left pane is always the map, right pane is always the current artifact.
- **Run/review screens built:** `build-run-tree` creates the master-detail view for `:run`/`:review`/`:rework`/`:finalize` states. Left pane shows batch map with lane statuses, right pane shows live trail stream via `trail->chat-nodes`.
- **Rendering pipeline wired:** `editor_compute.cljs` and `combined_text.cljs` dispatch by `(:node flow-state)` — `:intake` routes to intake tree, all other active states route to run tree.
- **Substrate regressions fixed:** Drag-selection char-width fix (`combined_text.cljs` — snapped `char-advance` in clip function). Settings panel text clipping (`settings_view.cljs` — slider spacing, value overflow clamp, snapped char-w).
- **Codex review fixes:** Mouse routing guarded to `:intake` only (prevents run/review clicks from mutating intake state). Trail scroll wired (`!run-scroll-y` atom + wheel routing). `set-selection` helper centralizes selection sync with active-lane-idx clamping. Settings char-w now properly snapped.
- **Files changed:** `dg_flow.cljs` (state machine, stack view, run tree, compute functions), `runtime.cljs` (keyboard/mouse/scroll routing, selection sync), `editor_compute.cljs` (run rects dispatch), `combined_text.cljs` (run text ops dispatch, char-width fix), `settings_view.cljs` (layout fixes), `events.cljs` (shift flag on arrow keys), `CLAUDE.md` (terminology).
- **Plan saved:** `/home/sid/.claude/plans/partitioned-inventing-diffie.md` — merged Claude + Codex plan with phased implementation order.

### Session 37 — Runtime Split: Thin Shell + 9 Modules — 2026-03-12
- **runtime.cljs refactored from 2215 LOC monolith to 106 LOC thin shell.** 9 modules extracted under `workspace/runtime/`: state (160), fonts (38), sidebar_io (105), interop (156), agent_flow (304), scroll (98), keyboard (406), mouse (508), render (273). Total: 2154 LOC across 10 files.
- **mouse.cljs further refactored into named helpers.** The original 370-line anonymous fn body inside `m/reduce` was causing "Can't call nil" compile errors due to a missing closing paren at 15 levels of nesting. Extracted 9 named `defn-` helpers (largest: 73 LOC). Root cause: `(let [content-w ...])` in the file-layout branch was unclosed, cascading through `cond`→`if`→`case` into a misleading CLJS analyzer error.
- **set-selection regression found and fixed.** During the split, `mouse.cljs` and `keyboard.cljs` copied raw `swap! !flow-state assoc :selected` instead of using `dg-flow/set-selection`, which syncs `:batch :lanes` and `:active-lane-idx`. Fixed in 3 call sites.
- **Build clean:** 238 files, 0 warnings. Public API `start-loop!` signature unchanged.
- **Files changed:** `runtime.cljs` (thin shell), `runtime/state.cljs`, `runtime/fonts.cljs`, `runtime/sidebar_io.cljs`, `runtime/interop.cljs`, `runtime/agent_flow.cljs`, `runtime/scroll.cljs`, `runtime/keyboard.cljs`, `runtime/mouse.cljs`, `runtime/render.cljs`, `events.cljs` (removed noisy wheel log).

### Session 39 — Architecture Reconciliation + Buffer Pool — 2026-03-21
- **Buffer pool implemented** (`buffer_pool.cljs`): slot-based GPU allocator with per-rect differential writes. Sidebar rects decoupled from editor rect system into own buffer + draw call. Build: 0 warnings.
- **Architecture docs created**: `claude-architecture-s39.md` (2500-line engineering appendix — scene element format, design spec EDN, event taxonomy, gap analysis). Codex created `softland-master-architecture.md` (canonical ontology/law doc). Both reviewed and reconciled through 3-way critique.
- **Key sequencing correction**: Rama-first by slice (not flatten-first). Sidebar chosen as first Rama-backed slice. Truth boundary defined: Rama for committed state, client-local for ephemeral, derived for scene.
- **Files created:** `buffer_pool.cljs`. **Files modified:** `editor_compute.cljs` (split sidebar from editor rects), `render.cljs` (pool wiring), `state.cljs` (pool atom), `renderer.cljs` (pool draw call).

### Session 40 — Sidebar As First Rama-Backed Slice — 2026-03-22
- **Rama sidebar PState** (`$$sidebar-pstate`): global `{Keyword Object}`, 4 event handlers (`:sidebar/dir-toggle`, `:sidebar/file-select`, `:sidebar/project-select`, `:sidebar/project-back`) in stream topology. Server-side atom bridges truth to Electric via `e/watch` (direct `foreign-proxy-async` subscription fails due to RocksDBWrapper serialization in Rama 1.6.0's wire protocol).
- **Electric bridge** (`WatchSidebarTruth`): server-side `e/watch` on truth atom → transfers to client via websocket. Client atom `!sidebar-truth` created in `electric_flow.cljc`, passed to runtime.
- **Client event emission**: fire-and-forget HTTP POST (`/api/sidebar/action`) with optimistic local updates. Committed truth flows back via Electric subscription, not HTTP callback.
- **Startup hydration**: `apply-sidebar-truth!` reads `@!sidebar-truth` once at boot, then installs `add-watch` for future changes. Rehydrates `dir-cache` (fetches project root + expanded dirs) and file content (fetches selected-file if not loaded).
- **Domain-identity IDs**: `(keyword prefix path)` — `:e//home/sid/foo.cljs` instead of `:entry-0`. Collision-free, fully inspectable. Sub-elements derived with namespace preservation.
- **Shared sidebar scene**: `!sidebar-scene` atom cached by render flow (`editor_compute.cljs`), consumed by hit-test (click + hover in `mouse.cljs`) and text ops (`combined_text.cljs`). Three redundant tree rebuilds eliminated.
- **proxy-callback fix**: Rama callback now returns `nil` instead of opaque Missionary notifier result (prevents wire protocol serialization failure on all PState subscriptions, not just sidebar).
- **Known remaining issues**: global PState (not workspace-scoped), no optimistic failure reconciliation, shared scene is mutable cache not first-class derived flow, `dir-cache` still local-only.
- **Files changed (13):** `core.clj`, `util_fns.cljc`, `server_jetty.clj`, `file_viewer.cljc`, `electric_flow.cljc`, `runtime.cljs`, `state.cljs`, `sidebar_io.cljs`, `mouse.cljs`, `render.cljs`, `editor_compute.cljs`, `combined_text.cljs`, `sidebar.cljs`.

### Session 41 — Sidebar Truth/Overlay/UI Completion — 2026-03-23
- **Three-layer sidebar state model landed.** `!sidebar-state` was split into `!sidebar-truth` (committed semantic state), `!sidebar-overlay` (optimistic semantic intent), and `!sidebar-ui` (hover/scroll/cache/in-flight local state). `derive-effective-sidebar` became the single visible-state merge point.
- **Live reconciliation restored.** The runtime now continuously watches remote sidebar truth again, clears optimistic overlay entries when truth catches up, and correctly handles `nil` sentinel cases for back-navigation instead of leaving permanent masking overlays.
- **Selection authority corrected.** Semantic `selected-file` now drives active sidebar highlighting and breadcrumb state. `!current-file` remains the loaded buffer consequence, not the selection authority.
- **Async safety tightened.** Dir/file fetches now dedupe with in-flight sets, stale file responses are ignored with latest-request tracking, rapid dir retoggles are last-write-wins, and in-flight flags clear on both success and error paths.
- **Hover/text invalidation narrowed.** Sidebar rects still rebuild on hover so highlight visuals stay live, but `!sidebar-scene` only resets when structural inputs change. The structural hash was narrowed to geometry-affecting UI keys (`:scroll-y`, `:dir-cache`, `:home-dirs`) so hover and in-flight bookkeeping no longer cause `text-same?` churn.
- **Instrumentation corrected.** Sidebar `rows` telemetry now follows the actual rect-tree shape (`sidebar-root -> content-node -> scroll-inner -> rows`) instead of reporting the border node.
- **Review-process lesson made durable.** `CLAUDE.md` now includes a falsification-pass review protocol: ownership, lifecycle, consumer, error path, ordering, shape, and a "done gate" that requires naming a plausible failure mode before declaring a step complete.
- **Validation:** repeated `shadow-cljs` builds passed with `0 warnings`.
- **Files changed:** `runtime.cljs`, `state.cljs`, `sidebar_io.cljs`, `mouse.cljs`, `render.cljs`, `editor_compute.cljs`, `combined_text.cljs`, `sidebar.cljs`, `CLAUDE.md`.

### Session 42 — Refactor Scoping + Claude/Codex Consensus — 2026-03-23
- **Editor scroll fix**: viewport height wasn't subtracting chrome (cmd panel 40px + status bar 24px), cutting off last ~3 lines. Added 10-line overscroll padding. Fixed in both wheel scroll (`scroll.cljs`) and keyboard auto-scroll (`keyboard.cljs`).
- **Sidebar slice verified complete**: Code-verified all 7 steps from `next-prompt.md` against actual implementation — all done. Three-layer model, live reconciliation, click→overlay, dedup, hover isolation, loading UX, instrumentation.
- **Full pipeline mapping**: Three exploration agents mapped: (1) entire render pipeline end-to-end (46 atoms → m/latest → m/sample → identical? → GPU), (2) Electric/Missionary capabilities (e/for-by not used anywhere, buffer pool per-slot API ready), (3) complete atom inventory with classification (12 should be Rama, 25 client-local, 8 derived).
- **Claude refactor plan written**: 4 phases, ~13-19 sessions. Organized around Three Gaps from S38. Concrete PState schemas, topology cases, file tables, risk register, verification checklists.
- **Codex refactor plan reviewed**: 9 phases, 3-doc structure (map + program + actions). Organized around semantic substrate correction. Key contribution: `effective-local-world` as the missing concept, artifact model generalization, split/pane semantics.
- **3-way comparison**: Core tension identified — Claude builds bottom-up (atoms → PStates → GPU diffs), Codex designs top-down (what is the workspace? → semantic boundaries → then persist). Both have valid contributions.
- **Consensus plan merged**: `docs/plans/claude-consensus-refactor-program.md`. Codex substrate ordering + Claude engineering detail + user correction (measure before assuming on editor latency). 9 phases, ~15-22 sessions.
- **Key user correction**: Do NOT assume editor keystroke-rate Rama round-trips will cause jank. Try direct committed editing first, measure, only fall back to hybrid if measurement requires it. Undo model stays provisional until editor path is decided.
- **Codex reviewed and signed off** with two corrections: (1) next-prompt dogfooding contradiction fixed, (2) Phase 4C undo model made provisional.
- **Files changed**: `scroll.cljs`, `keyboard.cljs` (scroll fix). Plan docs created: `claude-differential-refactor-program.md`, `claude-consensus-refactor-program.md`.
- **Next**: Phase 0 — Freeze the truth model (ownership inventory, no code).

*Last updated: Session 42 (refactor program canonicalized; Phase 0 next)*
*Full detailed log: `progressive_implementation.md`*
