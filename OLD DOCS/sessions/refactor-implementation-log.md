# Workspace Substrate Refactor — Implementation Log

> Living document. Updated each session as phases ship.
> Canonical plan: `docs/plans/claude-consensus-refactor-program.md`
> Ownership table: `docs/architecture/phase-0-ownership-freeze.md`

---

## Session 43 — 2026-03-23

### Phase 0: Ownership Freeze (design only, no code)

**Status**: COMPLETE

**What was done**:
- Inventoried all 49 runtime atoms from `state.cljs`
- Classified each as: committed truth (6), mixed truth+ephemeral (3), optimistic overlay (1), local ephemeral (27), derived (2), transitional (5), GPU/infra (8)
- Identified and resolved 6 ownership ambiguities (A1–A6)
- Defined intended field sets for workspace-truth, workspace-overlay, workspace-ui
- Locked persistence tiers: safe-now vs workspace-schema

**Output**: `docs/architecture/phase-0-ownership-freeze.md`

**How to verify**: Read the doc. Every atom has one owner class. No unresolved ambiguity.

---

### Phase 1A: Settings Rama Slice (code, 7 files)

**Status**: VERIFIED

**What was done**: Full 7-layer persistence stack for user settings, following the sidebar Rama pattern.

| Layer | File | Change |
|-------|------|--------|
| PState + topology | `src/app/server/rama/core.clj` | `$$settings-pstate`, `:settings/update` case with `explode-map` |
| Server mirror | `src/app/server/rama/util_fns.cljc` | `settings-pstate`, `get-settings-state`, `!settings-truth-atom`, `emit-settings-event!` |
| Electric bridge | `src/app/file_viewer.cljc` | `WatchUserSettings` e/defn |
| HTTP endpoint | `src/app/server_jetty.clj` | `POST /api/settings/update`, `GET /api/settings/state` |
| Client wiring | `src/app/electric_flow.cljc` | `!settings-truth` atom, passed through to runtime |
| Reconciliation | `src/app/client/workspace/runtime.cljs` | Initial load from truth, suppressed echo, font sync, selected-index sync |
| Client emit | `src/app/client/workspace/runtime/sidebar_io.cljs` | `emit-settings-update!` fire-and-forget HTTP |

**Persisted fields**: `:font-size`, `:line-height`, `:px-range`, `:sharpness`, `:snap-to-pixel?`, `:show-diagnostics?`, `:font-id`, `:theme-id`

**UI-local fields (not persisted)**: `:visible`, `:selected-index`, `:slider-index`, `:focus-section`

**Design decisions**:
- Initial-load-only reconciliation (not continuous) — avoids stale-truth overwrites during rapid edits
- `!settings-loaded` suppression flag — prevents initial merge from triggering a pointless POST back
- Re-apply persisted settings after `!active-font` reset — undoes font-watch default overwrite (review catch)
- Compute `selected-index` on restore — keeps settings panel highlight in sync (review catch)

**Bugs caught in review and fixed**:
1. (High) Font-watch lifecycle: restoring a non-default font triggered `install-font-watch!` which overwrote persisted slider values with font defaults. Fix: re-merge `persistent-fields` after the font reset.
2. (Medium) Settings panel cursor: `selected-index` wasn't recalculated on font restore, causing highlight/active font divergence. Fix: compute `font-idx` from `available-fonts` and write `:selected-index`.
3. (High) Missing closing paren on the `(when !remote-settings-truth` form — compilation failure. Fix: added the 10th `)`.

**How to smoke test** (once build compiles):
1. Start the app
2. Open settings (Ctrl+,)
3. Change font-size (e.g. 19 → 22)
4. Change theme
5. **Reload the page**
6. Confirm: font-size is still 22, theme persists
7. Open settings again — confirm highlighted font row is correct
8. If available: switch to a non-default font, change font-size, reload — confirm both font and size persist

**Blocking**: `deps.edn` was version-bumped (Clojure 1.11→1.12, shadow-cljs 2.25→2.28) before this session. Fresh dependency download needed before build compiles. Not caused by this session's changes.

---

### Phase 1B: Agent Trail Rama Slice (code, 8 files)

**Status**: VERIFIED

**What was done**: Persist completed agent trails to Rama. On run completion, client POSTs the assembled trail. On page reload, the last trail is restored from Rama truth.

**Design choice**: Client-side emit (not server-side accumulation). The server forwards raw CLI events; the client assembles them into structured trail entries. Duplicating assembly on the server would mean two copies of the same logic. One extra HTTP POST on run completion is simpler.

| Layer | File | Change |
|-------|------|--------|
| PState + topology | `src/app/server/rama/core.clj` | `$$agent-trails-pstate`, `:agent-trail/save-run` case |
| Server mirror | `src/app/server/rama/util_fns.cljc` | `agent-trails-pstate`, `get-agent-trail`, `get-latest-trail-run-id`, `!agent-trail-atom`, `save-agent-trail!` |
| Electric bridge | `src/app/file_viewer.cljc` | `WatchAgentTrail` e/defn |
| HTTP endpoint | `src/app/server_jetty.clj` | `POST /api/agent/trail/save` |
| Client emit | `src/app/client/workspace/runtime/sidebar_io.cljs` | `save-agent-trail!` |
| On-completion hook | `src/app/client/workspace/runtime/agent_flow.cljs` | Save trail on `:done`/`:run-done` event |
| Client wiring | `src/app/electric_flow.cljc` | `!agent-trail-truth` atom, passed to runtime |
| Restore on load | `src/app/client/workspace/runtime.cljs` | Restore `!agent-output` from saved trail if empty |

**What gets persisted per run**: `:status`, `:provider`, `:prompt`, `:trail` (vec of reasoning/tool-call/tool-result blocks), `:structured-result`

**What stays ephemeral**: `:output` (streaming text accumulation), `:tool-buf` (in-flight tool JSON)

**Latest-run tracking**: A special `"__latest"` key in the PState stores `{:run-id "..."}` so the server knows which trail to restore on boot.

**How to smoke test** (once build compiles):
1. Start the app
2. Run an agent command (e.g. type a prompt in the command panel)
3. Wait for the run to complete
4. Check console for `[TRAIL-HTTP] save round-trip: Xms`
5. **Reload the page**
6. Confirm: the trail from the last run is displayed (tool calls, reasoning, etc.)
7. The status should show as complete, not running

---

### Phase 1C: Workspace Action Boundary (code, 3 files)

**Status**: VERIFIED

**What was done**: Created `workspace_actions.cljs` as the central dispatch for workspace-level semantic transitions. Replaced all 6 direct mutation sites for `!active-pane` (4 sites) and `!sidebar-visible` (2 sites).

| File | Change |
|------|--------|
| `src/app/client/workspace/runtime/workspace_actions.cljs` | NEW — `set-active-pane!`, `toggle-sidebar!`, `hide-sidebar!` |
| `src/app/client/workspace/runtime/mouse.cljs` | 2 sites → `ws/set-active-pane!` |
| `src/app/client/workspace/runtime/keyboard.cljs` | 4 sites → `ws/set-active-pane!`, `ws/toggle-sidebar!`, `ws/hide-sidebar!` |

**Semantic actions (routed through dispatch)**:
- `set-active-pane!` — sets pane + auto-focuses keyboard for :editor/:chat
- `toggle-sidebar!` — toggles sidebar visibility
- `hide-sidebar!` — hides sidebar (escape cascade)

**Stays as direct mutation (correctly ephemeral)**:
- `!focus` when not part of a pane switch (e.g. clicking command panel)
- `!caret-visible`, `!cmd-panel :visible`, `!settings :visible`
- All scroll, hover, drag, mouse position

**How to smoke test**:
1. Click between editor, chat, and preview panes — confirm focus follows correctly
2. Press Ctrl+B (or toggle-file-viewer keybind) — sidebar toggles
3. Press Escape with sidebar visible, nothing else to dismiss — sidebar hides
4. Press Escape in chat — returns to editor pane
5. Click in chat pane — chat focuses, caret appears

---

### Phase 2A: Artifact Selection Model (code, 4 files)

**Status**: VERIFIED

**What was done**: Introduced `!selected-artifact` as the semantic selection intent. `!current-file` stays as a transitional I/O cache for the 40+ readers that check "is a file open?"

| File | Change |
|------|--------|
| `src/app/client/workspace/runtime/state.cljs` | Added `!selected-artifact` atom |
| `src/app/client/workspace/runtime/workspace_actions.cljs` | Added `select-artifact!`, `clear-artifact!` |
| `src/app/client/workspace/runtime/mouse.cljs` | File click → `ws/select-artifact!`; back → `ws/clear-artifact!` |
| `src/app/client/workspace/runtime.cljs` | Truth reconciliation syncs `!selected-artifact` from sidebar truth |
| `src/app/client/workspace/runtime/sidebar_io.cljs` | `seed-initial-file!` sets `!selected-artifact` |

**What changed**:
- `!selected-artifact` is now the *semantic selection* — `{:kind :file :path "..." :name "..."}`
- `!current-file` is kept in sync by `select-artifact!` and `clear-artifact!` — downstream readers don't need to change yet
- Sidebar truth reconciliation writes both `!selected-artifact` and `!current-file`
- Initial file seed writes both

**What didn't change (intentional)**:
- The 40+ readers of `!current-file` — they still work through the transitional shim
- `fetch-file!` success callback — it updates `!current-file` on load completion, which is correct (I/O consequence)
- Trail nav click — uses `fetch-file!` path, artifact catches up via truth reconciliation

**How to smoke test**:
1. Click a file in the sidebar — opens in editor
2. Click back button — returns to home, file closes
3. Click a different file — correct file opens
4. Rapid file switching — no stale file loads
5. (Dev console) Check `@!selected-artifact` — should show `{:kind :file :path "..." :name "..."}`

---

### Phase 2B: Effective Local World (code, 3 files)

**Status**: VERIFIED

**What was done**: Introduced `!effective-local-world` — a single derived object that answers "what world is the user in right now?" Reactively recomputed via watches on its 7 input atoms.

| File | Change |
|------|--------|
| `src/app/client/workspace/runtime/workspace_actions.cljs` | `derive-effective-local-world` pure fn |
| `src/app/client/workspace/runtime/state.cljs` | `!effective-local-world` atom |
| `src/app/client/workspace/runtime.cljs` | `recompute-local-world!` + watches on 7 inputs |

**The local world object**:
```clojure
{:mode              :file-workspace  ;; :flow-intake | :flow-run | :file-workspace | :editor
 :selected-artifact {:kind :file :path "..." :name "..."}
 :file-open?        true
 :flow-active?      false
 :project           {:name "..." :path "..."}
 :active-pane       :editor
 :sidebar-visible   true
 :flow-node         :idle
 :flow-session-id   nil
 :agent-status      nil
 :agent-run-id      nil}
```

**Key field**: `:mode` replaces the repeated `(if flow-canvas-active? ... (if file-open? ...))` pattern (~20 sites across 5 files). Consumer migration to read `:mode` from the local world is a future incremental step.

**Reactive inputs** (watches): `!selected-artifact`, `!active-pane`, `!flow-state`, `!agent-output`, `!sidebar-visible`, `!sidebar-truth`, `!sidebar-overlay`

**How to smoke test**:
1. Start the app, open dev console
2. Evaluate `(deref (:!effective-local-world (get-in @app ... :atoms)))` or check console logs
3. Open a file — `:mode` should be `:file-workspace`, `:file-open?` true
4. Close file (back button) — `:mode` should be `:editor`, `:file-open?` false
5. Toggle sidebar — `:sidebar-visible` updates
6. Switch active pane — `:active-pane` updates

---

### Phase 2C: Flow-State Rama Slice (code, 7 files)

**Status**: CODE COMPLETE, AWAITING SMOKE TEST

**What was done**: Persist DG workflow FSM state to Rama. Restore on reload so workflow sessions survive page refresh.

| Layer | File | Change |
|-------|------|--------|
| PState + topology | `src/app/server/rama/core.clj` | `$$flow-session-pstate`, `:flow/save-state` |
| Server mirror | `src/app/server/rama/util_fns.cljc` | `flow-session-pstate`, `!flow-session-atom`, `emit-flow-session-event!` |
| Electric bridge | `src/app/file_viewer.cljc` | `WatchFlowSession` |
| HTTP endpoint | `src/app/server_jetty.clj` | `POST /api/flow/save-state` |
| Client emit | `src/app/client/workspace/runtime/sidebar_io.cljs` | `save-flow-state!` |
| Client wiring | `src/app/electric_flow.cljc` | `!flow-session-truth` atom |
| Restore + persist watch | `src/app/client/workspace/runtime.cljs` | Restore on load if idle; persist on FSM node transitions |

**Persisted fields**: `:node`, `:tickets`, `:batch`, `:active-lane-idx`, `:runs`, `:decisions`, `:session-id`, `:history`

**Not persisted** (ephemeral, per Phase 0 A5): `:selected` (ticket UI selection)

**Persistence trigger**: fires only on FSM `:node` transitions (not on every ticket selection or UI interaction)

**Restore guard**: only restores if `!flow-state` is at `:idle` — won't overwrite an active session

**How to smoke test**:
1. Run `/mock` to bootstrap the DG workflow with mock tickets
2. Confirm tickets appear in the intake screen
3. Reload the page
4. Confirm: tickets are still visible, workflow state is restored at `:intake`
5. Run `/reset` to return to idle, reload — should be idle again

---

### Phase 3A: Pane Descriptors (code, 1 file)

**Status**: CODE COMPLETE

**What was done**: Pane descriptors are now derived as part of `!effective-local-world`. Each mode produces a vector of semantic pane descriptions instead of the current hardcoded layout branches.

| File | Change |
|------|--------|
| `src/app/client/workspace/runtime/workspace_actions.cljs` | `:panes` field added to `derive-effective-local-world` |

**Pane descriptor shape**:
```clojure
{:pane/id    :main          ;; stable identity
 :role       :primary-artifact  ;; semantic purpose
 :artifact-ref {:kind :file :path "..."}  ;; what it shows
 :content    nil             ;; non-artifact content (:intake-tree, :cmd-panel, etc.)
 :width-pct  0.4}            ;; proportional width
```

**Mode → panes mapping**:
- `:file-workspace` → `[:main :right :preview]` (40/55/5 — matches current `build-file-layout`)
- `:flow-intake` → `[:main]` with `:content :intake-tree`
- `:flow-run` → `[:main]` with `:content :run-detail`
- `:editor` → `[:main]` with no artifact

**What didn't change**: `build-file-layout` and the render pipeline still use the old approach. The descriptors are available in the local world for consumers to migrate to incrementally.

**How to verify**: Deref `!effective-local-world` in dev console — `:panes` should reflect the current mode with correct artifact refs and widths.

---

### Phase 3B: Split Semantics (code, 1 file)

**Status**: CODE COMPLETE

**What was done**: Added `:split` field to `!effective-local-world`. Describes the co-presence arrangement — what's held together, which pane is primary, what's adjacent.

| File | Change |
|------|--------|
| `src/app/client/workspace/runtime/workspace_actions.cljs` | `:split` field in `derive-effective-local-world` |

**Split shape**:
```clojure
{:direction :horizontal  ;; or :single
 :primary   :main        ;; pane/id of primary artifact
 :adjacent  [:right :preview]}  ;; or [] for single-pane modes
```

---

### Phase 3C: Workflow as Substrate Resident (code, 3 files)

**Status**: CODE COMPLETE

**What was done**: Workflow entry/exit is now a workspace action. DG command handlers call `enter-workflow!` / `exit-workflow!` for substrate-level consequences (clear artifact, reset pane/scroll). No circular dependency — closures passed via the existing env map.

| File | Change |
|------|--------|
| `src/app/client/workspace/runtime/workspace_actions.cljs` | `enter-workflow!`, `exit-workflow!` |
| `src/app/client/workspace/runtime/agent_flow.cljs` | Pass `enter-workflow!`/`exit-workflow!` closures in env |
| `src/app/client/workflows/dg_flow.cljs` | Call `enter-workflow!` on bootstrap, `exit-workflow!` on reset |

**Design**: `dg_flow.cljs` owns the FSM logic. `workspace_actions.cljs` owns the substrate consequences. The bridge is closures passed in the env map — no coupling between modules.

**How to smoke test**:
1. `/mock` — should enter workflow mode (intake screen), file artifact cleared
2. `/reset` — should exit workflow, back to editor mode
3. Open a file, then `/mock` — file should close, workflow takes over
4. `/reset` after workflow — back to bare editor, active-pane is :editor

---

### Phase 4A: Editor Event Classification (code, 1 file)

**Status**: CODE COMPLETE

**What was done**: Classified editor events into committed (content-changing) vs ephemeral (navigation-only). Added classification sets and predicate to `workspace_actions.cljs`.

- **Committed**: `:char`, `:backspace`, `:delete`, `:enter`, `:paste`, `:cut`, `:tab`
- **Ephemeral**: `:left`, `:right`, `:up`, `:down`, `:home`, `:end`, `:word-left`, `:word-right`, `:copy`, `:eval`, `:undo`, `:redo`
- **Structural** (committed, low-frequency): `:fold`, `:unfold`

---

### Phase 4B: Editor Direct Rama Path + Measurement (code, 5 files)

**Status**: MEASURED — DIRECT PATH VIABLE

**What was done**: Full Rama path for editor document persistence. The editor updates locally immediately (no latency for user). Committed events fire-and-forget to Rama in background. Console logs the full round-trip with server/network breakdown.

| Layer | File | Change |
|-------|------|--------|
| PState + topology | `src/app/server/rama/core.clj` | `$$editor-state-pstate`, `:editor/save-doc` |
| Server helper | `src/app/server/rama/util_fns.cljc` | `editor-state-pstate`, `!editor-doc-atom`, `save-editor-doc!` with latency measurement |
| HTTP endpoint | `src/app/server_jetty.clj` | `POST /api/editor/save-doc` |
| Client emit | `src/app/client/workspace/runtime/sidebar_io.cljs` | `save-editor-doc!` with full round-trip + server/network breakdown logging |
| Measurement hook | `src/app/client/workspace/runtime/keyboard.cljs` | Committed editor events fire background Rama emit |

**Design**: Editor updates locally first (zero latency for user). The Rama path runs in the background, measuring round-trip. This is NOT reconciliation — the editor doesn't read back from Rama yet. This phase measures: "how fast is the Rama round-trip at keystroke rate?"

**How to measure**:
1. Start the app, open a file
2. Type normally — watch console for `[EDITOR-RAMA] round-trip: Xms | server: Yms | network: Zms`
3. Type fast (burst) — check if round-trips stack up or stay consistent
4. Record: average, p95, max round-trip times

**Measurement result** (2026-03-29):
- Average: 7.5ms | Median: 7.3ms | P95: ~13ms | Max: 20.7ms
- Server (Rama): 4-5ms | Network (localhost): 2-4ms
- **Verdict: DIRECT PATH VIABLE.** 6x under 50ms threshold.
- **Decision: direct committed editing through Rama is the architecture.**
- No hybrid, no batching, no local-until-save needed.
- Phase 4C: event-reversal undo is the natural consequence.

---

### Phase 5: Differential Proof — Keyed-Diff Sidebar Pool (code, 3 files)

**Status**: VERIFIED

**What was done**: Proved keyed-diff approach for buffer pool rendering. Added `:id` to `tree->rects`, implemented `keyed-diff-update-pool!` (allocate/update/free by identity), wired into render consumer with measurement logging.

| File | Change |
|------|--------|
| `src/app/client/workspace/rect_tree.cljs` | `tree->rects` carries `:id` from source node |
| `src/app/client/substrate/webgpu/buffer_pool.cljs` | `keyed-diff-update-pool!` + `clojure.set` require |
| `src/app/client/workspace/runtime/render.cljs` | Sidebar pool uses keyed-diff when rects have IDs |

**Measurement result** (sidebar with 50-300 items):
- Hover (1 write): 0.1-0.3ms
- File select (2 writes): 0.2-0.5ms
- Dir expand (30+ new rects): 0.7-0.9ms
- Dir collapse (21 frees): 0.5ms
- All cases: O(changed), not O(total)

**Gate result: PASSED.** Per-slot keyed-diff is viable for Phase 6.

**Caveat for Phase 6**: keyed-diff collapses sequence into id→slot map. Sidebar rects are non-overlapping so draw order doesn't matter. Editor rects have z-ordering (selections under cursor). Phase 6A must handle draw order explicitly.

---

### Phase 7: Workspace-Schema Rama Widening (code, 7 files)

**Status**: VERIFIED

**What was done**: Persist workspace truth (selected-artifact, active-pane, sidebar-visible) to Rama. Page reload restores coherent workspace state.

| Layer | File | Change |
|-------|------|--------|
| PState + topology | `src/app/server/rama/core.clj` | `$$workspace-truth-pstate`, `:workspace/save-truth` |
| Server mirror | `src/app/server/rama/util_fns.cljc` | `workspace-truth-pstate`, `!workspace-truth-atom`, `emit-workspace-truth-event!` |
| Electric bridge | `src/app/file_viewer.cljc` | `WatchWorkspaceTruth` |
| HTTP endpoint | `src/app/server_jetty.clj` | `POST /api/workspace/save-truth` |
| Client emit | `src/app/client/workspace/runtime/sidebar_io.cljs` | `save-workspace-truth!` |
| Client wiring | `src/app/electric_flow.cljc` | `!workspace-truth` atom, `!sidebar-visible` initialized from truth |
| Restore + persist | `src/app/client/workspace/runtime.cljs` | Debounced persist, restore via `ws/set-active-pane!`, file read error rollback |

**Review fixes applied**:
- Debounced persist (16ms) prevents intermediate states from sequential atom mutations
- Restore uses `ws/set-active-pane!` to sync focus + caret (not raw `reset!`)
- Non-file restore clears `!current-file` to prevent stale identity
- `!sidebar-visible` initialized from workspace truth BEFORE `install-sidebar-watch!` fires
- File read error (including >1MB) rolls back all three layers: local atoms, committed sidebar truth, workspace truth

**How to smoke test**:
1. Open a file, switch to chat pane, reload — same file, same pane
2. Toggle sidebar off, reload — stays hidden
3. Click back to home, reload — home screen, no stale file
4. Click a >1MB file — error logged, selection rolls back, reload doesn't retry

---

## What's next

| Phase | Work | Est |
|-------|------|-----|
| **6A** | Editor rects differential — keyed identity for caret, selection, brackets | 1-2 sessions |
| **6B** | Text ops differential — per-line/per-block pools vs ring buffer | 2-3 sessions |
| **6C** | Shadow differential — per-source pools | 0.5 session |
| **6D** | WebGPU mount callbacks — 5-callback gpu-mount abstraction | 0.5-1 session |
| **6E** | Dirty-present — replace continuous RAF with dirty-flag + one-shot | 0.5 session |
| **8** | Consolidation — remove unused PStates, reconnect product lanes | 1-2 sessions |
