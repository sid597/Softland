# Phase 0: Ownership Freeze — Locked State Classification

> **Author**: Claude (Opus 4.6), Session 43 — 2026-03-23
> **Status**: LOCKED — every workspace atom has one intended owner class
> **Supersedes**: nothing (first ownership inventory)
> **Source of truth**: `src/app/client/workspace/runtime/state.cljs` (49 atoms inventoried)

---

## Classification Legend

| Class | Meaning | Persist? |
|-------|---------|----------|
| **T** — Committed truth | Semantically meaningful workspace state. Defines "what world the user is in." | Yes (when schema stable) |
| **O** — Optimistic overlay | Pending local intent waiting for truth confirmation. Short-lived bridge. | No — reconciled against truth |
| **E** — Local ephemeral | Transient UI state: hover, drag, scroll, blink, animation, input buffers. | Never |
| **D** — Derived | Computed from other state. Has no independent write path. | Never (recomputed) |
| **X** — Transitional | Will change shape or be replaced during refactor. Current form is legacy. | Defer |
| **G** — GPU/infra | GPU infrastructure, DOM references, buffer pools. Not semantic. | Never |

---

## Full State Classification Table

### Editor Core

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 1 | `!editor-doc` | **X** | `{:lines :cursor :selection :desired-col}` | **Conflated.** `:lines` = committed truth (the document). `:cursor`, `:selection`, `:desired-col` = local ephemeral (navigation). Phase 4 (editor commitment boundary) will split these. |
| 2 | `!cmd-panel` | **E** | `{:text :cursor :visible}` | Input buffer. Never persists. |
| 3 | `!focus` | **E** | `:editor` / `:command-panel` / `:chat` / `:settings-panel` | Keyboard routing target. Local-only forever. |
| 4 | `!scroll-y` | **E** | number | Editor vertical scroll. |
| 5 | `!scroll-x` | **E** | number | Editor horizontal scroll. |
| 6 | `!run-scroll-y` | **E** | number | Flow run detail scroll. |
| 7 | `!detail-scroll-y` | **E** | number | Flow detail pane scroll. |
| 8 | `!viewport` | **E** | `{:width :height :dpr}` | Browser dimensions. Environment fact, not user state. |
| 9 | `!folded-lines` | **T** | `#{line-nums}` | User-intentional folds. Per-file scope. Workspace-schema persistence (needs artifact model first). |
| 10 | `!caret-visible` | **E** | boolean | Blink timer phase. |
| 11 | `!clipboard` | **E** | string / nil | Duplicates system clipboard. |
| 12 | `!undo-stack` | **X** | `[{:lines :cursor}]` | Snapshot-based. Will change shape after editor commitment boundary (Phase 4). |
| 13 | `!redo-stack` | **X** | `[{:lines :cursor}]` | Same as undo-stack. |
| 14 | `!eval-result` | **E** | `{:text :line :expires-at}` / nil | SCI feedback with expiry. |
| 15 | `!dragging?` | **E** | boolean | Mouse drag-select flag. |
| 16 | `!active-pane` | **T** | `:editor` / `:chat` / `:preview` | Which pane is primary. Workspace-schema persistence. |
| 17 | `!drag-start` | **E** | `{:line :col}` / nil | Drag selection origin. |

### Font / Settings

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 18 | `!settings` | **T + E mixed** | See below | **Conflated.** Truth fields: `:font-size`, `:line-height`, `:px-range`, `:sharpness`, `:snap-to-pixel?`, `:show-diagnostics?`, `:font-id`, `:theme-id`. Ephemeral UI fields: `:visible`, `:selected-index`, `:slider-index`, `:focus-section`. Must be separated. |
| 19 | `!font-manifest` | **G** | `{:fonts :settings}` | System capability inventory. Not user state. |
| 20 | `!active-font` | **D** | `{:id :char-width :name}` | **Should be derived** from `!settings :font-id` + `!font-manifest`. Currently mutated directly by settings keyboard/mouse. Fix: make it a pure derivation. |
| 21 | `!font-assets` | **G** | `{:atlas :bitmap :id}` | GPU texture data. |

### GPU State

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 22 | `!text-geo` | **G** | GPU text geometry | Written by render loop. |
| 23 | `!editor-rect-sys` | **G** | GPU rect system | Written by render loop. |
| 24 | `!cmd-rect-sys` | **G** | GPU rect system | Written by render loop. |
| 25 | `!settings-rect-sys` | **G** | GPU rect system | Written by render loop. |
| 26 | `!shadow-sys` | **G** | GPU shadow system | Written by render loop. |
| 27 | `!sidebar-pool` | **G** | Buffer pool | Differential sidebar rect pool. |

### Sidebar / File State

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 28 | `!current-file` | **X** | `{:path :name}` / nil | **Duplicate owner.** Overlaps with `!sidebar-truth :selected-file`. Phase 2 replaces with `!selected-artifact`. |
| 29 | `!sidebar-truth` | **T** | `{:project :expanded-dirs :selected-file}` | Already Rama-backed. Correctly classified. |
| 30 | `!sidebar-overlay` | **O** | `{:pending-project :pending-expanded-dirs :pending-collapsed-dirs :pending-selected-file}` | Correctly classified. |
| 31 | `!sidebar-ui` | **E** | `{:hover-id :scroll-y :pointer-state :dir-cache :home-dirs :loading? :in-flight-dirs :in-flight-files}` | Correctly classified. Cache + interaction state. |
| 32 | `!sidebar-scene` | **D** | Resolved rect tree | Cached by render flow, consumed by hit-testing. |

### Agent / AI

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 33 | `!ai-provider` | **T** | `:claude` / `:openai` / etc. | User preference. Safe-now persistence. |
| 34 | `!agent-output` | **T + E mixed** | See below | **Conflated.** Truth (persist): `:trail`, `:status`, `:run-id`, `:provider`, `:prompt`, `:structured-result`. Ephemeral (streaming): `:output` (accumulating text), `:tool-buf` (in-flight tool JSON). Phase 1B separates these. |
| 35 | `!agent-scroll-y` | **E** | number | Agent output scroll. |
| 36 | `!chat-scroll-y` | **E** | number | Chat pane scroll. |
| 37 | `!chat-input` | **E** | `{:text :cursor}` | Chat input buffer. |

### Mouse Tracking

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 38 | `!mouse-x` | **E** | number | Pointer X. |
| 39 | `!mouse-y` | **E** | number | Pointer Y. |

### Workflow (DG Flow)

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 40 | `!flow-state` | **T + E mixed** | See below | **Conflated.** Truth: `:node`, `:tickets`, `:batch`, `:active-lane-idx`, `:runs`, `:decisions`, `:session-id`, `:history`. Ephemeral: `:selected` (current ticket selection for UI interaction). |
| 41 | `!collapsed-groups` | **E** | `#{status-keywords}` | Flow canvas display state. |
| 42 | `!hovered-row-idx` | **E** | number / nil | Flow canvas hover. |
| 43 | `!drag-state` | **E** | `{:phase :origin :node :current}` | Drag interaction FSM. |
| 44 | `!extract-preview` | **E** | `{:rt-node :ir :source-url :html}` / nil | JIT component extraction preview. Transient. |
| 45 | `!trail-collapsed` | **E** | `#{collapse-ids}` | Collapsed trail tool sections in UI. |
| 46 | `!shimmer-phase` | **E** | boolean | Loading animation timer. |

### External Atoms (passed through from caller)

| # | Atom | Class | Fields | Notes |
|---|------|-------|--------|-------|
| 47 | `!sidebar-visible` | **T** | boolean | Sidebar visibility. Workspace-schema persistence (part of split semantics). |
| 48 | `!file-load-request` | **X** | `{:lines :target-line}` / nil | One-shot trigger. Replaced by artifact loading in Phase 2. |
| 49 | `!preview-el` | **G** | DOM element / nil | DOM reference for preview pane overlay. |

---

## Summary Counts

| Class | Count | % |
|-------|-------|---|
| **T** — Committed truth | 6 (pure) | 12% |
| **T + E mixed** (must separate) | 3 | 6% |
| **O** — Optimistic overlay | 1 | 2% |
| **E** — Local ephemeral | 27 | 55% |
| **D** — Derived | 2 | 4% |
| **X** — Transitional | 5 | 10% |
| **G** — GPU/infra | 8 | 16% |

**Key finding**: Over half the atoms are correctly local-ephemeral. The problem isn't atom count — it's the 3 atoms that conflate truth with ephemeral state (`!editor-doc`, `!settings`, `!agent-output`) and the 5 transitional atoms that need redesign.

---

## Identified Ambiguities (Resolved)

### A1: `!current-file` vs `!sidebar-truth :selected-file` — DUPLICATE OWNER

**Problem**: Two atoms both claim "which file is selected."
- `!current-file` is set by sidebar-io `fetch-file!` on load success
- `!sidebar-truth :selected-file` is set by Electric truth sync
- `runtime.cljs:73` patches `!current-file` from truth, but they can diverge during in-flight loads

**Resolution**: `!current-file` is transitional. Phase 2 introduces `!selected-artifact` as the single semantic selection. `!current-file` remains as a compatibility shim until then, but is NOT truth — it's a downstream consequence of `!sidebar-truth :selected-file`.

### A2: `!settings` mixes truth and UI-local state

**Problem**: One atom holds both persistent preferences and settings-panel interaction state.
- `:font-size`, `:line-height`, `:theme-id`, etc. = committed truth (safe-now)
- `:visible`, `:selected-index`, `:slider-index`, `:focus-section` = local ephemeral

**Resolution**: Phase 1A separates these. Persistent settings fields go to a Rama settings slice. UI fields stay in a local `!settings-ui` atom or are folded into the settings panel's own ephemeral state.

### A3: `!active-font` is mutable but should be derived

**Problem**: Settings keyboard/mouse handlers write `!active-font` directly, duplicating the derivation path (settings → font-manifest → active font).

**Resolution**: `!active-font` should be a pure derivation: `(select-font (:font-id @!settings) @!font-manifest)`. The `install-font-watch!` pattern already exists but goes the wrong direction — it watches `!active-font` and updates `!settings`, when it should be the reverse.

### A4: `!agent-output` conflates trail data and streaming state

**Problem**: One atom holds both the persistent trail (`:trail`, `:status`, `:run-id`) and the transient streaming accumulation (`:output`, `:tool-buf`).

**Resolution**: Phase 1B separates these. Trail data goes to Rama (keyed by run-id). Streaming state stays local. They converge when a run completes.

### A5: `!flow-state` has mixed truth and interaction state

**Problem**: `:selected` (ticket selection for drag/click interaction) is ephemeral UI state packed into the same atom as workflow FSM truth.

**Resolution**: `:selected` moves to a local ephemeral atom. FSM truth (`:node`, `:tickets`, `:batch`, etc.) stays and becomes workspace-schema persistence in Phase 2C.

### A6: `!active-pane` vs `!focus` — different concerns, some overlap

**Problem**: `!active-pane` = which content pane (:editor/:chat/:preview). `!focus` = which keyboard target (:editor/:command-panel/:chat/:settings-panel). They overlap on `:editor` and `:chat`.

**Resolution**: These are correctly separate concerns. `!active-pane` is committed truth (workspace-schema). `!focus` is local ephemeral (keyboard routing). They overlap because "focusing the chat pane" sets both, but their lifecycles differ: `!active-pane` persists across focus changes (clicking cmd panel doesn't change active pane), while `!focus` changes on every keyboard-target switch.

---

## Intended Field Sets

### `workspace-truth` (committed, eventually Rama-backed)

```clojure
;; Sidebar (already Rama-backed)
{:project {:name "..." :path "..."}
 :expanded-dirs #{"..." "..."}
 :selected-file {:path "..." :name "..."}}

;; Settings (safe-now — Phase 1A)
{:font-size 19
 :line-height 1.2
 :px-range 8
 :sharpness 0.0
 :snap-to-pixel? true
 :show-diagnostics? false
 :font-id "dejavu-sans-mono"
 :theme-id :solarized-dark}

;; AI provider (safe-now — Phase 1A)
:claude

;; Agent trails (safe-now — Phase 1B)
;; keyed by run-id
{"run-abc" {:status :complete
            :provider :claude
            :prompt "..."
            :trail [{:kind :reasoning :text "..."}
                    {:kind :tool-call :tool-name "..." :input {...}}
                    ...]
            :structured-result {...}}}

;; Workspace-schema (Phase 2+, schema must stabilize first)
{:selected-artifact {:kind :file :path "..."}  ;; replaces !current-file
 :active-pane :editor
 :sidebar-visible true
 :folded-lines {:file-path #{3 17 42}}  ;; per-file fold sets
 :flow-state {:node :idle :tickets [...] :batch {...} ...}}
```

### `workspace-overlay` (pending intent, reconciled against truth)

```clojure
;; Already exists for sidebar
{:pending-project {:name "..." :path "..."}
 :pending-expanded-dirs #{"..."}
 :pending-collapsed-dirs #{"..."}
 :pending-selected-file {:path "..." :name "..."}}

;; Future (Phase 2+)
{:pending-selected-artifact {:kind :file :path "..."}
 :pending-active-pane :chat}
```

### `workspace-ui` (local ephemeral, never persists)

```clojure
;; Navigation / input
{:focus :editor
 :cmd-panel {:text "" :cursor 0 :visible true}
 :chat-input {:text "" :cursor 0}
 :clipboard "..."
 :eval-result {:text "42" :line 5 :expires-at 1711234567}}

;; Scroll positions
{:scroll-y 0 :scroll-x 0
 :agent-scroll-y 0 :chat-scroll-y 0
 :run-scroll-y 0 :detail-scroll-y 0}

;; Pointer / drag
{:mouse-x 0 :mouse-y 0
 :dragging? false :drag-start nil
 :drag-state {:phase :idle}}

;; Viewport
{:viewport {:width 1920 :height 1080 :dpr 2}}

;; Animation
{:caret-visible true :shimmer-phase false}

;; Sidebar UI
{:sidebar-hover-id nil
 :sidebar-scroll-y 0
 :dir-cache {} :home-dirs [...]
 :in-flight-dirs #{} :in-flight-files #{}}

;; Workflow UI
{:collapsed-groups #{} :hovered-row-idx nil
 :trail-collapsed #{} :extract-preview nil
 :flow-selected []}  ;; ticket selection moved from !flow-state

;; Settings panel UI
{:settings-visible false
 :settings-selected-index 0
 :settings-slider-index 0
 :settings-focus-section :fonts}
```

---

## Persistence Tiers

### Safe-Now (schema stable, can persist in Phase 1)

| Field | Schema risk | Notes |
|-------|-----------|-------|
| User settings (font, theme, etc.) | None | Carbon copy of sidebar pattern |
| AI provider | None | Single keyword |
| Agent trails (completed runs) | Low | Append-only, keyed by run-id |

### Workspace-Schema (must wait for Phase 2+ stability)

| Field | Why wait | Depends on |
|-------|----------|------------|
| `selected-artifact` | Replacing `!current-file` — artifact model not designed yet | Phase 2A |
| `active-pane` | Pane descriptors not defined yet | Phase 3A |
| `sidebar-visible` | Part of split semantics | Phase 3B |
| `folded-lines` | Per-file scope needs artifact model | Phase 2A |
| `flow-state` (FSM truth) | `:selected` extraction + FSM schema review needed | Phase 2C |

### Local-Only Forever

All 27 ephemeral atoms (scrolls, hover, drag, blink, input buffers, viewport, mouse, animation timers, caches, in-flight flags).

### Derived (recomputed, never stored)

- `!active-font` — derive from `!settings :font-id` + `!font-manifest`
- `!sidebar-scene` — derived rect tree from truth + overlay + ui

### GPU/infra (never semantic)

All 8 GPU atoms + `!preview-el` + `!font-manifest` + `!font-assets`.

---

## Existing Rama PStates (Server-Side Inventory)

### In active use
- `$$sidebar-pstate` — sidebar truth (project, expanded-dirs, selected-file)
- `$$agent-runs-pstate` — agent run metadata (run-id, status, provider, prompt)
- `$$event-id-pstate` — global event counter
- `$$cli-sessions-pstate` — CLI session tracking

### Unused scaffolding (Phase 8 cleanup)
- `$$nodes-pstate`, `$$dg-nodes-pstate`, `$$dg-pages-pstate`, `$$dg-edges-pstate`, `$$dg-node-ids-pstate`
- `$$components-pstate`, `$$node-ids-pstate`, `$$node-ids-inview-pstate`
- `$$user-registration-pstate`, `$$user-graph-settings-pstate`

### To be added
- `$$settings-pstate` — Phase 1A (global, single-user; keyed by `:settings`)
- `$$agent-trails-pstate` — Phase 1B (extends `$$agent-runs-pstate` with trail data)
- `$$workspace-truth-pstate` — Phase 7 (after schema stabilizes)

---

## Done Gate

Every workspace concern now has one intended owner class. The ambiguities (A1-A6) are identified and resolved. No unresolved ownership collisions remain.

**Plausible failure mode**: The biggest risk is that the `!editor-doc` split (Phase 4) reveals that cursor state actually needs to be committed truth for collaboration scenarios. Mitigation: Phase 4 measures the direct Rama path first and makes the cursor ownership decision based on evidence, not assumption. The current classification (cursor = ephemeral) is correct for single-user; if collaboration changes that, the commitment boundary design absorbs it.
