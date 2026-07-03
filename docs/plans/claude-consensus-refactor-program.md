# Workspace Substrate Refactor — Consensus Program

> **Author**: Claude (Opus 4.6), Session 42 — 2026-03-23
> **Status**: CANONICAL — merged from Claude + Codex plans after 3-way comparison
> **Principles**: Codex for substrate ordering. Claude for engineering detail. User correction: measure before assuming.
> **Supersedes**: `claude-differential-refactor-program.md` (Claude-only), but Codex docs remain valid reference for ontological framing
> **Origin docs**: `codex-refactor-program-map.md`, `codex-workspace-substrate-refactor-program.md`, `claude-differential-refactor-program.md`

---

## Executive Overview

### Where we are
The sidebar Rama slice proved the 3-layer pattern (committed truth + optimistic overlay + local ephemeral). One slice of ~12 is complete. The render pipeline uses snapshot-rebuild everywhere except sidebar rects. The workspace runtime is modularized (9 modules) but still atom-centered — ~46 mutable atoms with no single semantic root.

### Where we're going
One `effective-local-world` derived from workspace truth + overlay + local state. Semantic splits and panes, not hardcoded layout branches. Workflows and files as residents of one substrate. Differential rendering driven by `e/for-by` keyed diffs. Durable workspace truth in Rama — but only after the schema is stable.

### The merged ordering

| Phase | Name | From | Sessions |
|-------|------|------|----------|
| **0** | Ownership Freeze | Codex | 0.5-1 |
| **1** | Safe-Now Persistence + Action Boundary | Both | 2-3 |
| **2** | Artifact Model + Local World | Codex + Claude | 2-3 |
| **3** | Split/Pane + Workflow Integration | Codex | 2-3 |
| **4** | Editor Commitment Boundary | Claude + User | 2-3 |
| **5** | Differential Proof | Claude | 1-2 |
| **6** | Differential Pipeline | Claude | 3-5 |
| **7** | Workspace-Schema Rama Widening | Codex | 1-2 |
| **8** | Consolidation + Product Lanes | Both | 1-2 |
| | **Total** | | **~15-22** |

### Why this order

```
ownership freeze (what IS each atom?)
→ safe-now persistence (settings, trails — schema won't change)
→ artifact model (what is "selected"? stop special-casing files)
→ local world (one semantic root, not 15 scattered atoms)
→ split/pane (semantic fills, not hardcoded branches)
→ workflow integration (one substrate, not parallel modes)
→ editor commitment (measure the direct path first)
→ differential proof (e/for-by on sidebar, validate overhead)
→ differential pipeline (rects, text, shadows, mount, dirty-present)
→ workspace Rama widening (now the schema is right)
→ consolidation (cleanup, product lanes)
```

The key insight from the Codex plan: **don't persist the wrong schema.** Safe-now slices (settings, agent trails) can go early because their schemas are obvious. Workspace-shaping persistence (selected artifact, split, local-world truth) must wait until the semantic model is stable.

The key insight from the Claude plan: **differential rendering needs its own phases.** Text buffer restructuring is 2-3 sessions of hard GPU engineering. Treating it as a sub-step of "render narrowing" scopes it too small.

---

## Phase 0: Ownership Freeze (0.5-1 session)

### Purpose
Lock the ownership model before any implementation starts. Every atom gets one intended owner class.

### Work
- Inventory all ~46 runtime atoms from `state.cljs`
- Classify each as: committed truth / optimistic overlay / local ephemeral / derived / transitional
- Identify duplicate or ambiguous owners (e.g., `selected-file` vs `current-file`)
- Name the intended `workspace-truth`, `workspace-overlay`, `workspace-ui` field sets
- Distinguish safe-now persistence (schema stable) from workspace-schema persistence (schema must stabilize first)

### Key outputs
- Locked state classification table
- List of safe-now candidates (settings, agent trails)
- List of workspace-schema candidates (selected artifact, split, active pane)
- List of "local-only forever" fields

### Files to read (not change)
- `src/app/client/workspace/runtime/state.cljs` — all atoms
- `src/app/client/workspace/runtime/mouse.cljs` — semantic mutation sites
- `src/app/client/workspace/runtime/keyboard.cljs` — semantic mutation sites
- `src/app/client/workspace/sidebar.cljs` — sidebar pattern as precedent

### Done means
Every workspace concern has one intended owner class. No unresolved ambiguity.

---

## Phase 1: Safe-Now Persistence + Action Boundary (2-3 sessions)

### 1A: Settings Rama Slice (0.5 session)
Easiest win. Carbon copy of sidebar pattern. Settings rarely change.

| Step | What | File |
|------|------|------|
| Declare PState | `$$user-settings-pstate {Keyword Object}` | `core.clj` |
| Topology cases | `:settings/update` | `core.clj` |
| Server atom mirror | `!settings-atom`, `emit-settings-event!` | `util_fns.cljc` |
| Electric bridge | `WatchUserSettings` e/defn | `file_viewer.cljc` |
| Wire into client | Pass to runtime | `electric_flow.cljc` |
| Reconciliation | Initialize `!settings` from Rama truth on load | `runtime.cljs` |

### 1B: Agent-Output Rama Slice (1-2 sessions)
Trail data persists, survives reload, enables querying.

**Design**: Rama stores completed tool-use blocks and reasoning blocks (not per-token). Client still streams tokens for display responsiveness. They converge when run completes.

| Step | What | File |
|------|------|------|
| Declare PState | `$$agent-trails-pstate {String {Keyword Object}}` keyed by run-id | `core.clj` |
| Topology cases | `:agent-trail/append-block`, `:agent-trail/complete-run` | `core.clj` |
| Server atom mirror | `!agent-trails-atom`, `emit-agent-trail-event!` | `util_fns.cljc` |
| Electric bridge | `WatchAgentTrails` e/defn | `file_viewer.cljc` |
| Batch trail events | Accumulate in SSE handler, emit per completed block | `server_jetty.clj` |
| Reconciliation | Reconcile remote trails with `!agent-output` on run complete | `runtime.cljs` |

### 1C: Workspace Action Boundary (1 session)
Define the semantic action vocabulary. Stop letting input handlers directly own committed workspace meaning.

**Initial action vocabulary**:
- `select-project`, `select-artifact`, `set-active-pane`
- `enter-workflow`, `exit-workflow`
- `toggle-sidebar`, `mutate-split`

Route semantic transitions through explicit proposal handlers. Preserve direct mutation only for truly ephemeral concerns (hover, scroll, drag).

| File | Change |
|------|--------|
| `mouse.cljs` | Semantic clicks dispatch workspace actions, not direct atom swaps |
| `keyboard.cljs` | Semantic keys dispatch workspace actions |
| `runtime.cljs` or new `workspace_actions.cljs` | Action handlers as central semantic dispatch |

### Done means
- Settings persist across reload
- Agent trail data persists across reload
- Semantic workspace transitions are named actions, not scattered atom mutations

---

## Phase 2: Artifact Model + Local World (2-3 sessions)

### 2A: Unify Artifact Selection (1 session)
Replace file-special-case thinking with `artifact-ref`.

```clojure
;; Instead of:
{:current-file {:path "..." :name "..."}}

;; Generalize to:
{:selected-artifact {:kind :file :path "..."}}
;; or:
{:selected-artifact {:kind :trail :run-id "..."}}
;; or:
{:selected-artifact {:kind :workflow :flow-id "..."}}
```

Separate three things currently conflated:
1. **Artifact selected** (semantic intent — "the user chose this")
2. **Artifact content loaded** (I/O consequence — "the bytes are here")
3. **Pane showing artifact** (visual consequence — "it's on screen")

Centralize artifact loading. Dedupe in-flight loads. Ignore stale responses.

| File | Change |
|------|--------|
| `state.cljs` | Replace `!current-file` with `!selected-artifact` |
| `sidebar_io.cljs` | Generalize `fetch-file!` to `load-artifact!` |
| `mouse.cljs` | Sidebar file clicks dispatch `select-artifact` action |
| `runtime.cljs` | Artifact load lifecycle |

### 2B: Derive Effective Local World (1 session)
The key missing concept. One derived semantic root:

```clojure
(defn derive-local-world
  [workspace-truth workspace-overlay workspace-ui artifact-state]
  {:project (or (:pending-project overlay) (:project truth))
   :selected-artifact (or (:pending-selection overlay) (:selected-artifact truth))
   :artifact-content (get-in artifact-state [(:selected-artifact ...) :content])
   :active-pane (or (:pending-pane overlay) (:active-pane truth))
   :workflow-context (:workflow-context truth)
   :split (:split truth)
   ...})
```

Pane builders, workflow surfaces, scene derivation, and render all consume `effective-local-world` instead of chasing 15 scattered atoms.

| File | Change |
|------|--------|
| `runtime.cljs` | Derive `!effective-local-world` from truth+overlay+ui+artifacts |
| `render.cljs` | World snapshot builds from local world, not raw atoms |
| `editor_compute.cljs` | Consumes local world for mode/content decisions |

### 2C: Flow-State Rama Slice (1 session)
Follows sidebar pattern. Workflow FSM transitions become Rama events. Enables `--resume`.

| Step | What | File |
|------|------|------|
| Declare PState | `$$flow-session-pstate {String {Keyword Object}}` | `core.clj` |
| Topology cases | `:flow/transition`, `:flow/select-tickets`, `:flow/update-batch` | `core.clj` |
| Server atom mirror | `!flow-state-atom`, `emit-flow-event!` | `util_fns.cljc` |
| Transition handlers | Replace direct `swap! !flow-state` with action emission | `dg_flow.cljs` |

### Done means
- One `effective-local-world` answers "what world is active?"
- Artifact selection is general (not file-special-case)
- Workflow state persists for `--resume`

---

## Phase 3: Split/Pane Semantics + Workflow Integration (2-3 sessions)

### 3A: Pane Descriptors (1 session)
Panes become semantic fills, not hardcoded branches.

```clojure
;; Instead of: (if file-open? (build-file-layout ...) (build-intake-tree ...))

;; Pane descriptors:
{:pane/id :main   :role :primary-artifact  :artifact-ref {:kind :file :path "..."}}
{:pane/id :right  :role :trail             :artifact-ref {:kind :trail :run-id "..."}}
{:pane/id :bottom :role :command           :content :cmd-panel}
```

Shell composition derives from local-world + split, not mode branching.

### 3B: Split Semantics (0.5 session)
A split is preserved co-presence, not layout trivia. What's held together right now, which artifact is primary, what's adjacent.

### 3C: Workflow as Substrate Resident (1 session)
DG workflow surfaces mount inside the same substrate. Entering workflow context is a local-world transition, not a mode escape.

| File | Change |
|------|--------|
| `shell.cljs` | Derive shell from local-world + split descriptors |
| `dg_flow.cljs` | Workflow entry = local-world transition, not mode flag |
| `editor_compute.cljs` | Mode dispatch via pane descriptors, not `flow-canvas-active?` |
| `combined_text.cljs` | Text ops via pane descriptors |

### Done means
- Pane identity and content are semantically explicit
- Workflow entry is a local-world transition
- Shell composition derives from the semantic model

---

## Phase 4: Editor Commitment Boundary (2-3 sessions)

### 4A: Define Editor Event Types (1 session)

```clojure
;; Editing events (committed to Rama)
{:type :insert-char   :char "a"    :at {:line 5 :col 3}}
{:type :delete-backward            :at {:line 5 :col 3}}
{:type :paste         :text "..."  :at {:line 5 :col 3}}
{:type :newline                     :at {:line 5 :col 3}}
{:type :tab           :direction :indent  :lines [5 6 7]}

;; Navigation (ephemeral, not stored)
{:type :cursor-move   :to {:line 8 :col 0}}
{:type :select        :range {...}}

;; Structural
{:type :fold :line 42}
{:type :unfold :line 42}

;; Undo/redo
{:type :undo}
{:type :redo}
```

`editor-apply-event` already exists in `editor_compute.cljs`. It stays, but gets called by Rama topology instead of keyboard handler directly.

### 4B: Wire Editor to Rama — Direct Path First (1-2 sessions)

**THE RULE**: Try direct committed editing through Rama first. Measure the round-trip latency. Only fall back to batching/hybrid if measurement proves it necessary.

Do NOT assume local-until-save. Do NOT pre-build a hybrid model. Start with:

1. Keyboard handler emits editor event via HTTP POST to Rama
2. Rama topology materializes new doc state
3. Server atom mirror updates
4. Electric `e/watch` pushes to client
5. Client `!editor-doc` updates from truth

**Measure**: what is the round-trip latency? Is it perceptible while typing? At what typing speed does it become jank?

If direct path works: done. If measurement shows it's too slow: THEN design the optimistic/hybrid model based on actual numbers.

| Step | What | File |
|------|------|------|
| Declare PState | `$$editor-state-pstate {String {Keyword Object}}` keyed by file path | `core.clj` |
| Topology | Editor event handlers | `core.clj` |
| Server mirror | `!editor-state-atom`, `emit-editor-event!` | `util_fns.cljc` |
| Electric bridge | `WatchEditorState` | `file_viewer.cljc` |
| Keyboard handlers | Emit events instead of direct atom mutation | `keyboard.cljs` |
| Reconciliation | `!editor-doc` from Rama truth | `runtime.cljs` |

### 4C: Undo Model (0.5-1 session) — PROVISIONAL

The target direction is undo as event reversal (append reversal event, history = event log, remove snapshot-based `!undo-stack` / `!redo-stack`). But this architecture is provisional until Phase 4B measures the direct editor path.

If the direct Rama path works at keystroke rate: event-reversal undo is the natural consequence.
If the direct path requires a hybrid/batching model: the undo architecture must be redesigned to match whatever model emerges from measurement.

**Do not remove `!undo-stack` / `!redo-stack` or commit to the reversal model until 4B is measured and the editor policy is decided.**

### Done means
- Editor changes flow through Rama with measured latency
- Editor policy (direct vs hybrid) is decided based on measurement
- Undo model is designed to match the chosen editor policy (not pre-committed)

---

## Phase 5: Differential Proof (1-2 sessions)

**This is its own phase because it's a validation gate.** If `e/for-by` overhead is too high, Phase 6 needs rethinking.

### Wire e/for-by → Buffer Pool on Sidebar

Replace `batch-update-pool!` with Electric keyed diffs + per-slot callbacks:

```clojure
(e/for-by :id [rect sidebar-rects]
  (let [slot (pool/allocate-slot! !sidebar-pool)]
    (pool/update-slot! !sidebar-pool slot rect)
    (e/on-unmount #(pool/free-slot! !sidebar-pool slot))))
```

| Step | What | File |
|------|------|------|
| Electric sidebar flow | Sidebar rects as incremental sequence | `electric_flow.cljc` |
| Mount callbacks | Wire per-slot API as e/for-by body | `render.cljs` |
| Remove batch API | Replace `batch-update-pool!` | `render.cljs` |
| Measure overhead | Log: how many bodies fire, total time per frame | diagnostic |

**Validates**:
- Does `e/for-by` track sidebar items correctly?
- What is reactive overhead per node? (50-200 sidebar items)
- Does buffer pool per-slot API work under Electric lifecycle?

### Done means
- Sidebar rects render via `e/for-by` keyed diffs
- Overhead per node is measured and acceptable
- OR: overhead is too high and Phase 6 approach needs revision

---

## Phase 6: Differential Pipeline (3-5 sessions)

### 6A: Editor Rects Differential (1-2 sessions)
Each editor rect gets a stable identity:
- `:caret`, `:current-line`, `[:selection line-num]`, `:bracket-open`, `[:fold line-num]`

Replace `compute-editor-rects` → full `update-rects` upload with `e/for-by` + buffer pool.

| File | Change |
|------|--------|
| `editor_compute.cljs` | Keyed rect items instead of flat vector |
| `render.cljs` | Pool-based differential for editor rects |
| `buffer_pool.cljs` | Second pool instance for editor rects |

### 6B: Text Ops Differential (2-3 sessions) — HARDEST

Monolithic text buffer → differential. Two options (decide based on Phase 5 results):

**Option A: Per-line text pools**
- Each visible line gets a buffer slot
- Only changed lines re-upload
- Requires vertex buffer restructuring
- Shader may need changes for non-contiguous data

**Option B: Ring buffer with dirty tracking**
- One buffer, per-line dirty flags
- Repack only dirty line regions
- Less disruptive to shaders

### 6C: Shadow Differential (0.5 session)
Split editor+sidebar shadows into per-source pools.

### 6D: WebGPU Mount Callbacks (0.5-1 session)
The 5-callback `mount` abstraction for GPU:

```clojure
(defn gpu-mount [pool]
  {:append-child  (fn [rect] (let [s (allocate-slot! pool)] (update-slot! pool s rect) s))
   :replace-child (fn [slot rect] (update-slot! pool slot rect))
   :insert-before (fn [slot _sibling] slot)
   :remove-child  (fn [slot] (free-slot! pool slot))
   :nth-child     (fn [_i slot] slot)})
```

### 6E: Dirty-Present Strategy (0.5 session)
Replace continuous RAF with dirty-flag + one-shot RAF. Cursor blink explicitly sets dirty flag.

### Done means
- Rect GPU writes = O(changed rects), not O(all rects)
- Text GPU writes = O(visible changed lines)
- Idle with file open = zero RAF callbacks (except 530ms blink)

---

## Phase 7: Workspace-Schema Rama Widening (1-2 sessions)

**Now the semantic model is stable.** Safe to persist workspace-shaping fields.

Move into Rama:
- Selected artifact identity (from `effective-local-world`)
- Split/pane arrangement
- Workspace-level preferences (active pane, etc.)

| Step | What | File |
|------|------|------|
| Declare PState | `$$workspace-truth-pstate` | `core.clj` |
| Topology | Workspace semantic events | `core.clj` |
| Electric bridge | `WatchWorkspaceTruth` | `file_viewer.cljc` |
| Re-entry | On reload: restore coherent local world from Rama | `runtime.cljs` |

### Done means
- Page reload restores a coherent local world
- Persisted workspace truth matches the stabilized semantic model

---

## Phase 8: Consolidation + Product Lanes (1-2 sessions)

- Remaining low-priority atom migrations (`!folded-lines`, `!active-font` → settings PState)
- Remove 9 unused scaffolding PStates from `core.clj`
- Remove server atom mirror workaround if Rama fixes `foreign-proxy-async`
- Reconnect product lanes (review workspace, trail lineage, design artifacts) on top of the corrected substrate

---

## Risk Register

| # | Risk | Phase | Severity | Mitigation |
|---|------|-------|----------|------------|
| R1 | Editor direct path too slow at keystroke rate | 4B | HIGH | Measure first. Fall back to hybrid only with numbers. |
| R2 | Text buffer restructuring breaks shaders | 6B | HIGH | Defer text option decision until Phase 5 validates e/for-by overhead. |
| R3 | `e/for-by` reactive overhead too high per node | 5 | MEDIUM | Test on sidebar (50-200 nodes) before scaling. Phase 5 is a gate. |
| R4 | Server atom mirrors don't scale (one per slice) | 1-4 | MEDIUM | Acceptable for 5-6 slices. Revisit if Rama fixes `foreign-proxy-async`. |
| R5 | Agent trail batching granularity wrong | 1B | MEDIUM | Start with completed blocks. Refine later. |
| R6 | `effective-local-world` becomes a god object | 2B | MEDIUM | Keep it as a derivation, not a store. It has no write path of its own. |
| R7 | Artifact model generalization premature | 2A | LOW | Start with `:file` + `:workflow` + `:trail`. Add kinds as needed. |
| R8 | Cursor blink breaks in dirty-present | 6E | LOW | Blink timer sets dirty flag explicitly. |

---

## Verification Checklist

| Phase | Test | Pass Criteria |
|-------|------|---------------|
| 0 | State classification table reviewed | Every atom has one owner class |
| 1A | Change font → reload | Setting persists |
| 1B | Run agent → reload | Trail data persists |
| 1C | Click file in sidebar | Dispatches `select-artifact` action, not direct atom swap |
| 2A | Select file, then select trail | Both use same `selected-artifact` path |
| 2B | Read `effective-local-world` | One object explains current workspace |
| 2C | `/mock` → select → reload | Flow state persists |
| 3A | Open file | Pane descriptor drives content, not mode branch |
| 4B | Type at normal speed | Measure: is round-trip perceptible? |
| 4C | Type → undo → redo | Event reversal works |
| 5 | Toggle sidebar dirs rapidly | GPU writes = O(changed), overhead measured |
| 6A | Move cursor, select text | Rect writes = O(changed), not O(all) |
| 6B | Scroll large file | Text writes = O(visible changed lines) |
| 6E | Idle with file open | Zero RAF (except 530ms blink) |
| 7 | Reload page | Coherent local world restored |
| All | `npx shadow-cljs compile dev` | 0 warnings |

---

## Files Changed Per Phase (summary)

| Phase | Primary Files |
|-------|--------------|
| 0 | None (design only) |
| 1 | `core.clj`, `util_fns.cljc`, `file_viewer.cljc`, `electric_flow.cljc`, `runtime.cljs`, `mouse.cljs`, `keyboard.cljs`, `server_jetty.clj` |
| 2 | `state.cljs`, `sidebar_io.cljs`, `runtime.cljs`, `render.cljs`, `editor_compute.cljs`, `dg_flow.cljs`, `core.clj`, `util_fns.cljc` |
| 3 | `shell.cljs`, `dg_flow.cljs`, `editor_compute.cljs`, `combined_text.cljs` |
| 4 | `core.clj`, `util_fns.cljc`, `keyboard.cljs`, `editor_compute.cljs`, `runtime.cljs`, `state.cljs` |
| 5 | `electric_flow.cljc`, `render.cljs`, `buffer_pool.cljs` |
| 6 | `editor_compute.cljs`, `combined_text.cljs`, `renderer.cljs`, `render.cljs`, `buffer_pool.cljs` |
| 7 | `core.clj`, `util_fns.cljc`, `file_viewer.cljc`, `runtime.cljs` |
| 8 | `core.clj`, `state.cljs` |
