# UI/UX Tracker (newest first)

## Current Checklist (verified against code 2026-02-27)

### Tier 1: Agent Output Panel — COMPLETE
- [x] Dynamic panel height (`compute-agent-panel-h`, caps at 50% viewport)
- [x] Horizontal text wrapping (`wrap-line`, character-based)
- [x] Scroll within agent panel (`!agent-scroll-y`, wheel routing, clipping)
- [x] Dismiss (Escape closes cmd+agent as one unit)
- [x] Agent output background rect (fully opaque)
- [x] Enter feedback (immediate `[CLAUDE] running:` with background)
- [x] Command panel cursor alignment (`cmd-prompt-text` / `cmd-text-start-x` helpers)
- [x] Focus race conditions fixed (file-load guard, editor click auto-closes panel)

### Tier 2: Polish & Discoverability
- [x] Placeholder text — now `"Ask AI..."` (loop.cljs:1133)
- [x] Console logging in hot paths — `"Cmd Key:"` removed
- [x] Line numbers — rendering as `:line-number` text ops (loop.cljs:1065, 1102)
- [x] Dead code cleanup — `visible-agent-lines` removed
- [ ] Hardcoded constants — `cmd-panel-h 40` still inline (loop.cljs:1256)
- [x] Provider indicator — shown in status bar right side (`filename | PROVIDER`)

### Tier 2b: V0 Flow State Machine (Step 4) — COMPLETE (2026-02-18)
- [x] Flow state machine (pure fns: `flow-transitions`, `valid-transition?`, `transition-flow-state`)
- [x] 8 slash commands parsed (`/bootstrap`, `/select`, `/run`, `/review`, `/rework`, `/finalize`, `/status`, `/reset`) — Session 35 removed `/arrange` and renamed `/run-flow`
- [x] Prompt templates (`flow-prompt` for bootstrap, sequential, parallel, rework, finalize)
- [x] JSON ticket parser (`parse-tickets-from-output` — code block + bracket fallback)
- [x] Extracted `make-event-handler` (DRY: eliminates 3x duplicated stream handler)
- [x] `!flow-state` atom with workflow state tracking (node, tickets, selected, batch lanes, active lane, runs, decisions, session-id, history)
- [x] `fire-flow-run!` composes prompt → SSE stream with `on-done` callback
- [x] Manual bootstrap path (`/bootstrap`, `/mock`) into the workflow shell
- [x] `--allowedTools` for read-only Linear MCP (security: CLI args, not settings file)
- [x] Session continuity via `--resume` across workflow lifecycle
- [x] End-to-end validated: 21 real Linear tickets pulled on first load
- [x] `--json-schema` structured output: validated ticket JSON from Claude CLI (bypasses text parsing)
- [x] `fire-flow-run!` accepts `:json-schema`, `:max-turns`, `:model`, `:append-system-prompt`
- [x] Fallback chain: structured result → trail → text (graceful degradation)

### Tier 2b++: Plugin Workflow V0 Shell — COMPLETE ENOUGH TO DOGFOOD (2026-03-09)
- [x] `:arrange` removed as a node; ordering moved into intake as an execution stack
- [x] Right-pane stack view for multi-select with active lane focus
- [x] `Enter` triggers `/run`; `Shift+Up/Down` reorders the stack
- [x] Run/review master-detail shell (`build-run-tree`) wired into rect and text pipelines
- [x] Trail pane scroll for run/review (`!run-scroll-y`)
- [x] Input routing guarded so run/review clicks no longer mutate intake state
- [ ] Review tabs (`Summary | Trail | Diff | Tests`)
- [ ] Provenance badges (`Imported | Agent inference | Observed | Human decision`)
- [ ] Resume fork (`Resume | Refresh | Start clean`)
- [x] V0 bugs from review: `/select` helper bypass, run scroll reset, intake scroll leak, status/review clobber artifact buffer — FIXED (Session 36)

### Tier 2b+: Bootstrap Direct API (2026-02-25) — COMPLETE
- [x] Direct Linear GraphQL endpoint (`/api/linear/issues`) — bypasses Claude CLI
- [x] `fetch-linear-issues` server fn with `env/linear-api-key`
- [x] Client `/bootstrap` rewired to `js/fetch` (no streaming, no agent panel)
- [x] Fixed `--max-turns` (doesn't exist) → `--max-budget-usd`

### Tier 2c: Rect Tree UI — COMPLETE (2026-02-25, design: `docs/architecture/rect-tree-ui.md`)
- [x] **Step 1**: Rect tree data structure + generic tree walk (`rt-node`, `tree->rects`, `tree->text-ops`)
- [x] **Step 2**: Hit-testing (`hit-test` — deepest node containing point)
- [x] **Step 3**: Render pipeline (tree walk → GPU instances + text ops, replaces `compute-ticket-list-*`)
- [x] **Step 4**: Click dispatch (`dispatch-event` — bubbling, replaces ad-hoc mouse handlers)
- [x] **Step 5**: Drag state machine (IDLE → PENDING → DRAGGING → DROP, 5px threshold)
- [x] **Step 6**: Drop zones (left→right cross-panel DnD = select ticket)
- [x] **Step 7**: Visual feedback (ghost/dimmed original, floating ticket at cursor, blue drop zone border)

### Tier 2d: SDF Rich Quads + Shadows — COMPLETE (2026-02-27)
- [x] SDF fragment shader (Inigo Quilez `sd_rounded_box`, anti-aliased edges)
- [x] Per-corner radii, independent border widths/colors, linear gradients
- [x] Shadow pipeline (analytical Gaussian blur via `erf_approx`, separate render pass)
- [x] Rect buffer expanded: 8 → 28 floats/rect (112 bytes)
- [x] Design tokens `dt` — Linear/shadcn dark theme (colors, spacing, radii, shadows, font-sizes)
- [x] Component library: `ui-card`, `ui-badge`, `ui-button`, `ui-divider`, `ui-progress`, `ui-scrollbar`, `ui-tabs`, `ui-tooltip`

### Tier 2e: Layout Engine + Composable Components — COMPLETE (2026-02-27)
- [x] Layout engine: `normalize-padding`, `layout-children`, `resolve-text-layout`, `resolve-layout`
- [x] `:layout` directive on `rt-node` — `{:direction :column/:row :gap :padding :align :auto-height?}`
- [x] `:text-layout` directive — auto-wraps and positions text ops
- [x] Composable panel components (shadcn Sidebar pattern):
  - `ui-panel`, `ui-panel-header`, `ui-panel-content`, `ui-panel-footer`
  - `ui-panel-group` (collapsible section), `ui-list-item` (leading/title/trailing slots)
  - `ui-checkbox`, `ui-priority-dot`
- [x] `build-intake-tree` rewritten: left pane uses composable components (~60% shorter)
- [x] `build-right-detail` extracted as standalone fn
- [x] Visual tuning: taller rows (24→34), rounded hover/selected backgrounds, muted section labels, bigger checkboxes, group gaps
- [x] Design docs: `docs/architecture/virtual-layout-engine.md`, `docs/architecture/gpu-component-library.md`

### Tier 2f: WebGPU-Native Sidebar + Rama Truth Slice — COMPLETE (2026-03-23)
- [x] `build-sidebar-tree` — pure fn building sidebar as rt-node tree
- [x] File explorer only (no tabs) — 2 modes: home dirs list, file tree
- [x] `flatten-file-tree` — recursive dir-cache flattener with depth, expand/active tracking
- [x] `compute-sidebar-content-height` — scroll clamping
- [x] `offset-rects`, `offset-shadows`, `offset-text-ops` — shift all content right by `sidebar-w` (256px)
- [x] Three-layer sidebar state: `!sidebar-truth`, `!sidebar-overlay`, `!sidebar-ui`
- [x] `derive-effective-sidebar` — single merge point for committed truth + optimistic overlay + local caches/UI
- [x] Live truth reconciliation from Rama/Electric back into client state
- [x] Sidebar selection derives from semantic `selected-file`, not loaded file content
- [x] In-flight dir/file dedupe + stale file-response protection + error-path cleanup
- [x] Reactive flow integration: `<editor-rects` + `<combined-text-ops` build sidebar tree + offset content
- [x] Shared `!sidebar-scene` cache used by hit-test + sidebar text
- [x] Structural invalidation narrowed: hover repaints rects without resetting sidebar text scene; in-flight sets do not trigger text churn
- [x] Event routing: scroll/click/hover via `!mouse-x` hit-test (`mouse-x < sidebar-w`)
- [x] (2026-03-02) Removed tabs, Review/UI modes, component-mode click handler, ~540 lines deleted

### Tier 2h: 3-Pane File Layout — COMPLETE (2026-03-02)
- [x] `build-file-layout` (~line 1698) — Editor (40%) | Chat (30%) | Preview (30%) for any open file
- [x] Condition: `(some? current-file)` activates 3-pane in both `<editor-rects` and `<combined-text-ops`
- [x] Chat pane shows agent output trail (same data as bottom panel)
- [x] Preview pane: placeholder "No preview" — ready for future extension
- [x] Editor text ops clipped to left 40% when file is open
- [x] `initial-flow-state` starts at `:idle` — ticket list only after `/bootstrap` (`/dg`)
- [ ] Chat pane interactivity (typing, refinement)
- [ ] Preview pane content (compiled rt-nodes, HTML preview)
- [ ] Panel resize handles (drag to adjust 40/30/30 splits)

### Tier 2i: Horizontal Scroll Fix — COMPLETE (2026-03-03)
- [x] Split `layout-x` into unscrolled (`layout-x`) and scrolled (`editor-lx`) — gutter stays fixed
- [x] `clip-sub` clips text to `[layout-x, code-w]` — left (gutter edge) AND right (pane boundary)
- [x] Left-trim: chars scrolled past gutter edge are trimmed, not just dropped
- [x] `compute-editor-rects` takes `:gutter-lx` kwarg — fold indicators/gutter use unscrolled position
- [x] Scroll events restricted to editor pane only (`mouse-x < editor-right`)
- [x] `!scroll-x` resets to 0 on file open

### Tier 2g: Design Converter Pipeline — COMPLETE (2026-03-01, multi-LLM)
- [x] `_css_parsers.cljc` — 12 CSS string → Clojure value parsers (color/hex/rgb/hsl, shadow, gradient, layout props)
- [x] `_token_matcher.cljc` — fuzzy matching of literal CSS values → nearest `dt` design tokens (color/radius/shadow/spacing/font-size)
- [x] `_compiler.cljc` — Design IR → rt-node tree compilation (dual-input: IR + direct rt-node passthrough)
- [x] `_extractor.js` — browser DOM walker via `getComputedStyle()` + wrapper collapsing (runs in Chrome)
- [x] `_design_ir.cljc` (Codex) — strict IR schema validator with per-field validation
- [x] `_verifier.cljc` (Codex) — geometry/style/structure diffs, color parsing utils, pass/fail thresholds
- [x] `_extractor_prompt.md` (Gemini) — LLM extraction prompt targeting Design IR output (data not code)
- [x] `by-codex/blueprints/` — seed blueprints: button (3 states), sidebar composition
- [x] `by-codex/schemas/design-ir.edn` — schema definition (node types, style keys, compile target)
- [x] `by-codex/tests/verification-spec.edn` — verification thresholds spec
- [ ] End-to-end test: extract shadcn component → parse → tokenize → compile → render on canvas
- [ ] Visual comparison: screenshot source vs rendered rt-node

### Tier 2c-legacy (SUPERSEDED by rect tree)
- [ ] ~~Ticket cards on canvas~~ → rect tree ticket nodes
- [ ] ~~Drag-to-arrange~~ → rect tree DnD
- [ ] Review artifact tabs (Summary / Trail / Diff / Tests)
- [ ] Provenance + pacing polish in review workspace
- [ ] Default project file on load (discourse-graph `index.ts`)

### Tier 3: Editor Core
- [x] Code folding — gutter click folds/unfolds code (fixed: was only updating indicator, not text)
- [x] Current-line highlight — subtle full-width bg on cursor line (`{1.0 1.0 1.0 0.04}`)
- [x] Status bar — 24px bottom bar: `Ln X, Col Y` (left), `filename | PROVIDER` (right)
- [ ] Save to file — Ctrl+S still downloads `code.clj`, needs `/api/write-file` endpoint
- [ ] Smooth scrolling — raw wheel delta, no easing/momentum

### Tier 3+: Nice-to-haves (from original audit, low priority)
- [ ] Named theme picker — replace integer slider with named list
- [ ] Adaptive gutter width — derive from line count + char-advance
- [ ] Keyboard shortcut help — Ctrl-? or help command
- [ ] Sidebar search/filter for large projects
- [ ] Unsaved changes indicator (dot/marker)

---

# Session Log: 2026-02-13 — Status Bar Implementation

## What was done
Implemented the status bar — a 24px always-visible strip at the viewport bottom.

**Layout**: Everything shifts up by `status-bar-h` (24px). Bottom-up stacking: status bar → cmd panel → agent output.

**Implementation** (no new GPU buffers/flows — reuses existing systems):
- `<cmd-panel-rects`: added instance [3] for status bar bg (`{0.12, 0.12, 0.16, 1.0}`), shifted instances [0-2] up by `status-bar-h`
- `<combined-text-ops`: added `!current-file` + `status-bar-h` params, status bar text ops (left: `Ln X, Col Y`, right: `filename | PROVIDER`)
- Scroll consumer + mouse handler: Y bounds shifted, status bar clicks are no-ops
- `editor.cljs`: draw call for instance 3 (always visible)

**Files changed**: `loop.cljs` (6 locations), `editor.cljs` (1 location)

## Updated priorities
- Status bar: DONE (the "deepest UX issue" from original audit)
- Current-line highlight: also done (added in same branch, separate from status bar)
- Next: Save to file (Ctrl+S → `/api/write-file`), smooth scrolling, or theme picker

---

# Session Log: 2026-02-12 — Code Folding Fix + Performance

## What was done
Fixed gutter fold click: indicator color changed but code text didn't collapse.

**Root cause**: `<combined-text-ops` has a fast path for files >500 lines that skips fold detection entirely. The fold indicator rects (from `<editor-rects` / `<fold-state`) always reflected the fold state, but the text rendering path ignored it.

**Fix 1**: `large-file?` now requires `(empty? folded)` — when any fold is active, falls through to normal path.

**Fix 2 (performance)**: Eliminated redundant Lezer parse. `<combined-text-ops` now receives `<fold-data` flow (pre-computed, cached) instead of calling `detect-folds-fn` internally. No Lezer re-parse on scroll/resize/keystroke while folds are active.

### Cost analysis
- Before: every scroll event on a folded large file triggered full Lezer `.parse()` (~1s+)
- After: Lezer parse only on doc/fold change (cached in `<fold-state`). Scroll cost = `layout_fn` loop over N lines (microseconds)

---

# Session Log: 2026-02-12 — Agent Panel Scroll

## What was done
Implemented scroll within the agent output panel, completing all Tier 1 agent output items.

### Changes in `loop.cljs`:
1. **`!agent-scroll-y` atom** — scroll offset within agent panel, clamped 0 to max-scroll
2. **`!mouse-y` atom** — tracks mouse Y on every mousemove (not just during drag)
3. **Scroll consumer routing** — wheel events check if mouse is over agent panel area. If yes → update `!agent-scroll-y`. If no → update `!scroll-y` (editor).
4. **`<combined-text-ops` viewport clipping** — removed truncation. Offset all line Y by `-agent-scroll-y`, only emit render ops within panel bounds.
5. **Scroll reset** — `(reset! !agent-scroll-y 0)` at new-output entry points. Response callbacks do NOT reset.

### Bug found: top-edge clipping
- Clip on text baseline, not bounding box: `(>= y (+ panel-top 8))` not `(>= (+ y line-step) panel-top)`

### Lessons
- No GPU scissor = software clip. Always clip on baseline position.
- `>wheel` only provides delta, not mouse position. Need `!mouse-y` from mousemove for hit-testing.
- Scroll + truncation are mutually exclusive. Pick one per component.

---

# Session Log: 2026-02-12 — Agent Output Text Wrapping

## What was done
1. **`wrap-line` helper** — character-based wrapping, `max-chars = floor((viewport-w - 48) / char-advance)`
2. **`compute-agent-panel-h` updated** — wraps lines before counting, panel height reflects wrapped count
3. **`<combined-text-ops`** — wraps via `(mapcat #(wrap-line % max-chars))`

---

# Session Log: 2026-02-12 — Dynamic Agent Output Panel

## What was done
1. **Dynamic panel height** — `compute-agent-panel-h` pure fn. Grows to fit, caps at 50% viewport.
2. **Truncation indicator** — `visible-agent-lines` (later superseded by scroll)
3. **Escape dismiss** — Cmd panel + agent output as one unit. Cascade: Settings → Cmd+Agent → Sidebar → Clear selection.

### Design decision
Cmd panel and agent output are **one unit**. No state where one is visible without the other.

---

# Session Log: 2026-02-12 — Tier 1 Implementation + Re-Audit

## What was completed
- Command panel cursor alignment (shared helpers)
- Agent output background rect (3-slot vector, GPU-stable)
- Enter feedback (immediate `running:` with background)
- Focus race conditions (file-load guard, click-to-close)

### Bugs found during implementation
1. **1-frame race condition** on Ctrl-K — gated draws on `(< editor-lines total-lines)`
2. **Agent background bleed** — changed alpha to 1.0
3. **No follow-up path** — Enter with text keeps panel open, empty Enter closes

---

# Original Audit: 2026-02-11 (archive)

<details>
<summary>Click to expand original audit findings</summary>

## 1. Command Panel (Ctrl-K) — Bug + Missing Feedback

### Typing Bug (panel disappears on keystroke)
The Enter handler always hides the panel. Focus race condition: `@!focus` deref inside `m/eduction` filter. If anything flips `!focus` back to `:editor`, keystrokes go to editor flow.

### Other issues
- No visual distinction from editor (40px bar blends into background)
- Placeholder too long (leaked implementation details)
- No provider indicator beyond prefix
- Panel closes immediately on Enter

## 2. Agent Output Panel — Floating, Overlapping, No Interaction
- No background rect
- Fixed 180px height
- No scroll, resize, dismiss, or copy
- Persists indefinitely
- Status color is the only feedback

## 3. Editor Core — Missing Comfort Features
- No line numbers
- No current-line highlight
- No minimap
- Ctrl+S downloads instead of saving
- No unsaved changes indicator

## 4. Settings Panel (Ctrl-G)
- Keyboard-only with no instructions
- Theme is integer slider
- No labels showing current values

## 5. Sidebar (Ctrl-B) — ~~Imperative DOM Island~~ RESOLVED (Tier 2f)
- ~~Visual inconsistency (HTML vs WebGPU text)~~ → WebGPU-native (MSDF text, rect tree)
- No search/filter
- ~~Fixed 250px width~~ → 256px WebGPU sidebar with offset helpers

## 6. Cross-Cutting Issues
- No status bar
- No filename display
- No keyboard shortcut discoverability
- No smooth scrolling
- Hard-coded layout constants

## The Deepest UX Issue
The command panel → agent output flow has no visual continuity. The fix is a **status bar** that becomes the visual anchor: shows provider, running status, line:col, filename. This is the ambient presence that tells you the system is working without interrupting flow.

</details>
