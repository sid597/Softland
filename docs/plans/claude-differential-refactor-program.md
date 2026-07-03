# Differential Architecture Refactor — Full Program

> **Author**: Claude (Opus 4.6), Session 42 — 2026-03-23
> **Scope**: The refactor thread only. Sidebar slice = done. Workspace substrate = next anchor.
> **Format**: Executive overview → program map → phase detail → action tracker

---

## Executive Overview

### Where we are
The sidebar Rama slice proved the end-to-end pattern: Rama PState → server atom mirror → Electric `e/watch` → client 3-layer model (truth/overlay/ui) → derived scene → GPU. One slice of ~12 is complete. The render pipeline still uses snapshot-rebuild everywhere except sidebar rects (which have a differential buffer pool).

### Where we're going
Every piece of committed state lives in Rama. Every render path operates on diffs, not full rebuilds. The commitment boundary (signal → proposal → commit) replaces direct atom mutation. `e/for-by` keyed diffs drive GPU buffer updates.

### What the program looks like
Four phases over ~13-19 sessions:

| Phase | Name | What It Does | Sessions |
|-------|------|-------------|----------|
| **1** | **Workspace Substrate** | Migrate editor-doc, settings, flow-state, agent-output to Rama. Define commitment boundary for editor. | 5-7 |
| **2** | **Differential Proof** | Wire `e/for-by` → buffer pool mount callbacks on sidebar (smallest surface). Validate overhead. | 1-2 |
| **3** | **Differential Pipeline** | Extend keyed diffs to editor rects, text ops, shadows. WebGPU mount callbacks. Dirty-present. | 4-6 |
| **4** | **Consolidation** | Remaining low-priority Rama slices. Remove scaffolding PStates. Clean atom mirrors if Rama bug is fixed. | 2-3 |

**The workspace substrate (Phase 1) is the load-bearing phase.** It touches the most files, introduces the commitment boundary, and forces the hardest design decisions (optimistic editing at keystroke rate). Phases 2-4 are incremental on top of it.

### Three Gaps (from S38)

| Gap | What | Status After Sidebar | Status After Phase 1 |
|-----|------|---------------------|---------------------|
| 1. Commitment boundary | signal → proposal → commit | 10% (sidebar has partial) | ~60% (editor events defined, editor uses it) |
| 2. Atoms → PStates | 12 atoms → Rama | 8% (1/12) | ~50% (5/12: sidebar + editor + settings + flow + agent) |
| 3. Snapshot → Differential | `e/for-by` keyed diffs | 5% (buffer pool built) | 5% (untouched until Phase 2) |

---

## Program Map

### State Inventory (what moves where)

**Committed truth → Rama** (12 atoms, 4 phases):

| Atom | Phase | Slice | Complexity | Notes |
|------|-------|-------|------------|-------|
| `!sidebar-truth` | — | Sidebar | — | **DONE** |
| `!editor-doc` | 1 | Workspace | Very High | Needs commitment boundary, optimistic overlay at keystroke rate |
| `!undo-stack` / `!redo-stack` | 1 | Workspace | High | Derived from editor event log, not stored separately |
| `!settings` | 1 | Workspace | Low | Identical to sidebar pattern |
| `!agent-output` | 1 | Workspace | Medium | Append-heavy (streaming), batch completed blocks to Rama |
| `!flow-state` | 1 | Workspace | Medium | FSM transitions become Rama events, enables --resume |
| `!current-file` | 4 | Consolidation | Low | Merge with editor session PState |
| `!folded-lines` | 4 | Consolidation | Low | Per-file, keyed by path |
| `!active-pane` | 4 | Consolidation | Low | Fold into settings PState |
| `!ai-provider` | 4 | Consolidation | Low | Fold into settings PState |
| `!active-font` | 4 | Consolidation | Low | Derived from settings |

**Stays client-local** (~25 atoms): scroll positions (6), mouse/drag (5), caret-visible, shimmer, clipboard, cmd-panel, chat-input, eval-result, hover, extract-preview, sidebar-overlay, sidebar-ui, trail-collapsed, collapsed-groups

**Should be derived** (~8 atoms): `!viewport`, `!sidebar-scene`, `!font-manifest`, GPU state atoms (text-geo, rect-sys, shadow-sys, sidebar-pool)

### Rama PStates (current vs target)

**Currently in use (5)**:
- `$$sidebar-pstate` — sidebar committed truth
- `$$agent-runs-pstate` — run metadata
- `$$cli-sessions-pstate` — --resume session IDs
- `$$user-registration-pstate` — user registry
- `$$user-graph-settings-pstate` — per-user UI modes

**To add (Phase 1)**:
- `$$editor-state-pstate` — editor doc per file path
- `$$user-settings-pstate` — font, theme, preferences
- `$$agent-trails-pstate` — structured trail data per run
- `$$flow-session-pstate` — workflow FSM state per session

**Unused scaffolding (9, remove in Phase 4)**:
`$$nodes-pstate`, `$$node-ids-pstate`, `$$node-ids-inview-pstate`, `$$dg-node-ids-pstate`, `$$dg-pages-pstate`, `$$dg-nodes-pstate`, `$$dg-edges-pstate`, `$$components-pstate`, `$$event-id-pstate`

### Render Pipeline (current → target)

**Current (snapshot-rebuild)**:
```
46 atoms → m/latest chains (~22 watches)
  → <combined-text-ops (flat vector of ALL text ops)
  → <editor-rects+sidebar (editor: snapshot, sidebar: differential)
  → m/latest (world snapshot)
    → m/sample on RAF 60Hz
      → m/reduce (render consumer)
        → identical? (10 sub-conditions for text)
          → different? → full GPU buffer rewrite O(total)
          → same? → skip
```

**Target (differential)**:
```
Rama PStates → Electric e/watch (automatic transfer)
  → e/for-by :key [element visible-elements]
    → mount: allocate-slot!, upload rect/text
    → change: update-slot! (O(1) per changed element)
    → unmount: free-slot!
  → dirty flag → one-shot RAF → present
```

**The gap between these is Phases 1-3 of this program.**

---

## Phase 1: Workspace Substrate (5-7 sessions)

The anchor phase. Four Rama slices + commitment boundary.

### 1.1 Settings Rama Slice (0.5 sessions)
**Start here** — easiest win, validates pattern reuse.

Identical to sidebar pattern. Settings change infrequently (font selection, theme, etc.).

| Step | What | File |
|------|------|------|
| Declare PState | `$$user-settings-pstate {Keyword Object}` | `core.clj` |
| Topology cases | `:settings/update` | `core.clj` |
| Server atom mirror | `!settings-atom`, `emit-settings-event!` | `util_fns.cljc` |
| Electric bridge | `WatchUserSettings` e/defn | `file_viewer.cljc` |
| Wire into Electric graph | Pass to client runtime | `electric_flow.cljc` |
| Client reconciliation | Initialize `!settings` from Rama truth on load | `runtime.cljs` |

**Risk**: Low. Carbon copy of sidebar pattern.

### 1.2 Agent-Output Rama Slice (1-2 sessions)
Trail data persists, survives reload, enables querying.

Key difference from sidebar: **append-heavy**. Agent trails accumulate streaming data. Sidebar is click-response.

**Design decision**: Rama stores completed tool-use blocks and reasoning blocks — NOT per-token. Client still streams tokens into `!agent-output` for display responsiveness. Rama holds the structured result. They converge when the run completes.

| Step | What | File |
|------|------|------|
| Declare PState | `$$agent-trails-pstate {String {Keyword Object}}` keyed by run-id | `core.clj` |
| Topology cases | `:agent-trail/append-block`, `:agent-trail/complete-run` | `core.clj` |
| Server atom mirror | `!agent-trails-atom`, `emit-agent-trail-event!` | `util_fns.cljc` |
| Electric bridge | `WatchAgentTrails` e/defn | `file_viewer.cljc` |
| Wire into Electric graph | Pass to client runtime | `electric_flow.cljc` |
| Batch trail events | Accumulate in SSE handler, emit to Rama per completed block | `server_jetty.clj` |
| Client reconciliation | Reconcile remote trails with `!agent-output` on run complete | `runtime.cljs` |

**Risk**: Medium. Streaming→batching interface is new territory. Sidebar was request/response.

### 1.3 Flow-State Rama Slice (1 session)
Workflow FSM transitions become Rama events. Enables `--resume` across page reload.

| Step | What | File |
|------|------|------|
| Declare PState | `$$flow-session-pstate {String {Keyword Object}}` keyed by session-id | `core.clj` |
| Topology cases | `:flow/transition`, `:flow/select-tickets`, `:flow/update-batch` | `core.clj` |
| Server atom mirror | `!flow-state-atom`, `emit-flow-event!` | `util_fns.cljc` |
| Electric bridge | `WatchFlowState` e/defn | `file_viewer.cljc` |
| Wire + reconcile | Same pattern | `electric_flow.cljc`, `runtime.cljs` |
| Transition handlers | Replace direct `swap! !flow-state` with event emission | `dg_flow.cljs` |

**Risk**: Medium. FSM transitions have more semantic complexity than sidebar clicks.

### 1.4 Editor Doc Rama Slice + Commitment Boundary (3-4 sessions)
**The hard one.** This is where the commitment boundary gets designed and implemented, because you can't put editor state in Rama without defining what an editor event IS.

#### 1.4a: Define Editor Event Types (1 session, design)

The event vocabulary that replaces direct atom mutation:

```clojure
;; Editing events
{:type :insert-char   :char "a"    :at {:line 5 :col 3}}
{:type :delete-backward            :at {:line 5 :col 3}}
{:type :delete-forward              :at {:line 5 :col 3}}
{:type :paste         :text "..."  :at {:line 5 :col 3}}
{:type :cut           :from {:line 1 :col 0} :to {:line 3 :col 5}}
{:type :newline                     :at {:line 5 :col 3}}
{:type :tab           :direction :indent  :lines [5 6 7]}

;; Navigation/selection events (not stored in Rama — ephemeral)
{:type :cursor-move   :to {:line 8 :col 0}}
{:type :select        :from {:line 1 :col 0} :to {:line 3 :col 5}}

;; Structural events
{:type :fold          :line 42}
{:type :unfold        :line 42}

;; Undo is a reversal, not a snapshot
{:type :undo}
{:type :redo}
```

`editor-apply-event` in `editor_compute.cljs` already takes event keywords + doc → new doc. This function stays but gets called by Rama topology, not keyboard handler directly.

**Key design choice — Hybrid model**: Editor events stay LOCAL until explicit save (Ctrl+S) or session commit. Then a batch of events goes to Rama. Rationale:
- Keystroke-rate Rama round-trips would cause jank (even test IPC latency)
- In-progress editing IS ephemeral (you expect to lose unsaved work on crash)
- Saved state IS committed truth (you expect it to survive)
- This matches every editor's mental model (unsaved = ephemeral, saved = durable)

This means `!editor-doc` stays as a client-local atom during active editing. On save, the accumulated edit events batch-append to Rama. On reload, Rama truth materializes the last-saved state.

#### 1.4b: Wire Editor Events (1-2 sessions, implementation)

| Step | What | File |
|------|------|------|
| Declare PState | `$$editor-state-pstate {String {Keyword Object}}` keyed by file path | `core.clj` |
| Topology cases | `:editor/save` (batch of edit events), `:editor/load` | `core.clj` |
| Server mirror | `!editor-state-atom`, `emit-editor-save!` | `util_fns.cljc` |
| Electric bridge | `WatchEditorState` e/defn | `file_viewer.cljc` |
| Event accumulator | Keyboard handlers push events to local event log (not Rama) | `keyboard.cljs` |
| Save handler | On Ctrl+S: batch-append accumulated events to Rama, clear local log | `keyboard.cljs` |
| Reload reconciliation | On page load: materialize `!editor-doc` from Rama PState | `runtime.cljs` |

#### 1.4c: Undo as Event Reversal (1 session)

| Step | What | File |
|------|------|------|
| Remove snapshot stacks | Delete `!undo-stack`, `!redo-stack` atoms | `state.cljs` |
| Local event log | Events accumulate in a local vector; undo = pop last event + recompute | `keyboard.cljs` |
| Save checkpoints | On Ctrl+S, current doc state becomes a checkpoint in Rama | `core.clj` topology |
| Undo across saves | Rama stores event log; replay from last checkpoint minus N events | future (not Phase 1) |

**Risk**: HIGH for 1.4b. The hybrid model simplifies the latency problem but introduces a new concept (local event log + batch commit). The undo model changes from "snapshot stack" to "event reversal," which is conceptually cleaner but requires rethinking the undo/redo keyboard handlers.

---

## Phase 2: Differential Proof (1-2 sessions)

Wire `e/for-by` on the smallest possible surface: sidebar rects.

### 2.1 e/for-by → Buffer Pool on Sidebar

Replace `batch-update-pool!` (the batch compare-and-write API) with Electric keyed diffs + per-slot callbacks:

```clojure
;; Target: Electric drives GPU buffer slots directly
(e/for-by :id [rect sidebar-rects]
  (let [slot (pool/allocate-slot! !sidebar-pool)]
    (pool/update-slot! !sidebar-pool slot rect)
    (e/on-unmount #(pool/free-slot! !sidebar-pool slot))))
```

| Step | What | File |
|------|------|------|
| Electric sidebar flow | Sidebar rects as an Electric incremental sequence | `electric_flow.cljc` |
| Mount callbacks | Wire `allocate-slot!` / `update-slot!` / `free-slot!` as e/for-by body | new or `render.cljs` |
| Remove batch API usage | Replace `batch-update-pool!` call in render consumer | `render.cljs` |
| Measure overhead | Log per-frame: how many e/for-by bodies fire, total time | diagnostic |

**What this validates**:
- Does `e/for-by` correctly track sidebar items by identity?
- What is the reactive overhead per node? (Currently unmeasured, flagged in S38 docs)
- Does the buffer pool per-slot API work correctly under Electric lifecycle?

**Risk**: Medium. If `e/for-by` overhead per node is too high for 200+ sidebar items, the approach needs rethinking before Phase 3 scales it to editor rects and text.

---

## Phase 3: Differential Pipeline (4-6 sessions)

Extend keyed diffs from sidebar to the full render pipeline.

### 3.1 Editor Rects Differential (1-2 sessions)

Each editor rect type gets a stable identity:
- `:caret` — one rect, identity = `:caret`
- `:current-line` — one rect, identity = `:current-line`
- `:selection-{line}` — per-line, identity = `[:selection line-num]`
- `:bracket-open`, `:bracket-close` — identity = position
- `:fold-{line}` — per fold region, identity = `[:fold line-num]`

Replace `compute-editor-rects` → full `update-rects` upload with `e/for-by` + buffer pool.

| File | Change |
|------|--------|
| `editor_compute.cljs` | Emit keyed rect items instead of flat vector |
| `render.cljs` | Replace `update-rects` with pool-based differential |
| `buffer_pool.cljs` | May need second pool instance for editor rects |

### 3.2 Text Ops Differential (2-3 sessions) — HARDEST

The monolithic text buffer (`update-text-data` in `renderer.cljs`) packs ALL glyph instances into one Float32Array. Making it differential means:

**Option A: Per-line text pools**
- Each visible line gets a buffer pool slot (or small sub-buffer)
- When line content changes, only that line's glyphs re-upload
- Requires restructuring the vertex buffer from one array to indexed sub-arrays
- Shader may need changes to handle non-contiguous glyph data

**Option B: Ring buffer with dirty tracking**
- Keep one buffer, track which line ranges are dirty
- On change, repack only dirty lines into their buffer region
- Less disruptive to shaders but more complex dirty tracking

**This is the phase where the S38 vision meets real GPU engineering constraints.** The design choice between A and B needs to be made based on what Phase 2 reveals about `e/for-by` overhead.

### 3.3 Shadow Differential (0.5 sessions)

Currently editor+sidebar shadows are combined and fully rebuilt. Split into per-source shadow pools or aggregate from multiple pools.

### 3.4 WebGPU Mount Callbacks (1 session)

Implement the 5-callback `mount` pattern for WebGPU as a reusable abstraction:

```clojure
(defn gpu-mount [pool]
  {:append-child  (fn [rect] (let [s (allocate-slot! pool)] (update-slot! pool s rect) s))
   :replace-child (fn [slot rect] (update-slot! pool slot rect))
   :insert-before (fn [slot _sibling] slot)  ;; z-order: TBD
   :remove-child  (fn [slot] (free-slot! pool slot))
   :nth-child     (fn [_i slot] slot)})
```

### 3.5 Dirty-Present Strategy (0.5 sessions)

Replace continuous RAF loop with:
1. Electric diff fires → set dirty flag
2. Request one RAF via `requestAnimationFrame`
3. In RAF callback: submit command encoder → present → clear dirty flag
4. No more continuous `m/sample` polling

**Caveat**: Cursor blink (530ms timer) must explicitly set dirty flag to trigger RAF. Currently it's implicit because the RAF loop runs continuously.

---

## Phase 4: Consolidation (2-3 sessions)

### 4.1 Remaining Rama Slices
- `!current-file` → merge into editor session PState
- `!folded-lines` → per-file fold prefs in editor PState
- `!active-pane`, `!ai-provider` → fold into `$$user-settings-pstate`

### 4.2 Cleanup
- Remove 9 unused scaffolding PStates from `core.clj` topology
- Remove atom mirror workaround if Rama fixes `foreign-proxy-async` in a future version
- Delete `electric_flow_old.cljc` reference if still present

---

## Risk Register

| # | Risk | Phase | Severity | Mitigation |
|---|------|-------|----------|------------|
| R1 | Editor optimistic updates cause jank | 1.4 | HIGH | Hybrid: local until save. No Rama in keystroke path. |
| R2 | Text buffer restructuring breaks shaders | 3.2 | HIGH | Defer decision until Phase 2 validates e/for-by overhead. |
| R3 | `e/for-by` reactive overhead too high per node | 2.1 | MEDIUM | Test on sidebar (50-200 nodes) before scaling to editor (200+ lines). |
| R4 | Server atom mirrors don't scale (one per slice) | 1.x | MEDIUM | Acceptable for 5-6 slices. Revisit if Rama fixes `foreign-proxy-async`. |
| R5 | Agent trail batching granularity wrong | 1.2 | MEDIUM | Start with completed blocks. Can refine later. |
| R6 | Undo model change (snapshots → events) confuses user | 1.4c | LOW | Same Ctrl+Z behavior, different internals. Test carefully. |
| R7 | Cursor blink breaks in dirty-present | 3.5 | LOW | Blink timer sets dirty flag explicitly. |

---

## Files Changed Per Phase

### Phase 1 (Workspace Substrate)
| File | What Changes |
|------|-------------|
| `src/app/server/rama/core.clj` | 4 new PState declarations, ~12 new topology cases |
| `src/app/server/rama/util_fns.cljc` | 4 new mirror atoms, 4 new emit fns |
| `src/app/file_viewer.cljc` | 4 new `e/defn` bridges |
| `src/app/electric_flow.cljc` | Wire 4 bridges into client |
| `src/app/client/workspace/runtime.cljs` | 4 new truth reconciliation watchers |
| `src/app/client/workspace/runtime/state.cljs` | Remove `!undo-stack`, `!redo-stack` |
| `src/app/client/workspace/runtime/keyboard.cljs` | Editor handlers emit events, not direct atom mutation |
| `src/app/client/workflows/dg_flow.cljs` | Flow transitions emit events, not direct atom mutation |
| `src/app/server_jetty.clj` | Trail event batching in SSE handler |

### Phase 2 (Differential Proof)
| File | What Changes |
|------|-------------|
| `src/app/electric_flow.cljc` | Sidebar rects as Electric incremental sequence |
| `src/app/client/workspace/runtime/render.cljs` | Replace `batch-update-pool!` with e/for-by |
| `src/app/client/substrate/webgpu/buffer_pool.cljs` | Verify per-slot API under Electric lifecycle |

### Phase 3 (Differential Pipeline)
| File | What Changes |
|------|-------------|
| `src/app/client/workspace/editor_compute.cljs` | Keyed rect items instead of flat vectors |
| `src/app/client/workspace/combined_text.cljs` | Per-line text identity + differential ops |
| `src/app/client/substrate/webgpu/renderer.cljs` | Text buffer restructuring, mount abstraction |
| `src/app/client/workspace/runtime/render.cljs` | Dirty-present, remove continuous RAF |

### Phase 4 (Consolidation)
| File | What Changes |
|------|-------------|
| `src/app/server/rama/core.clj` | Remove 9 scaffolding PStates |
| `src/app/client/workspace/runtime/state.cljs` | Remove migrated atoms |

---

## Verification Checklist

| Phase | Test | Pass Criteria |
|-------|------|---------------|
| 1.1 | Change font → reload page | Setting persists |
| 1.2 | Run agent → reload page | Trail data persists |
| 1.3 | `/mock` → select → reload | Flow state persists, --resume works |
| 1.4 | Type → Ctrl+S → reload | Saved content survives |
| 1.4 | Type → undo → redo | Event reversal works, same UX as before |
| 2.1 | Toggle sidebar dirs rapidly | No visual glitch, GPU writes = O(changed) |
| 3.1 | Move cursor, select text | Rect GPU writes = O(changed rects), not O(all rects) |
| 3.2 | Scroll large file | Text GPU writes = O(visible changed lines) |
| 3.5 | Idle with file open | Zero RAF callbacks (except cursor blink every 530ms) |
| All | `npx shadow-cljs compile dev` | 0 warnings |
