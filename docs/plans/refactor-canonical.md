# Canonical Refactor Plan: `loop.cljs` -> Workspace Runtime

> Consensus source: user + Claude + Codex
> Status: LOCKED - implementation document

---

## Goal

Correct the codebase ontology at zoom 100.

Move the active client runtime out of `webgpu/` into:
- `substrate/` for rendering only
- `workspace/` for shared runtime + composition
- `workflows/` for workflow-specific logic

No new runtime abstractions.
No new features.
Pure structural move plus dead-code quarantine.

---

## Final Target Structure

```text
old-infra/
└─ src/
   └─ ...all source files outside the active closure, preserving original paths

src/app/
├─ electric_flow.cljc
├─ server_jetty.clj
├─ server/
│  └─ ...active server files preserved by closure
└─ client/
   ├─ substrate/
   │  └─ webgpu/
   │     └─ renderer.cljs
   ├─ workspace/
   │  ├─ runtime.cljs
   │  ├─ events.cljs
   │  ├─ text_input.cljs
   │  ├─ themes.cljc
   │  ├─ rect_tree.cljs
   │  ├─ ui_primitives.cljs
   │  ├─ sidebar.cljs
   │  ├─ shell.cljs
   │  ├─ trail.cljs
   │  ├─ editor_compute.cljs
   │  ├─ cmd_panel.cljs
   │  ├─ agent.cljs
   │  ├─ settings_view.cljs
   │  └─ combined_text.cljs
   └─ workflows/
      ├─ dg_flow.cljs
      └─ jit.cljs
```

### Spine rename

| From | To | New Namespace |
|------|----|---------------|
| `app.client.webgpu.editor` | `app.client.substrate.webgpu.renderer` | `src/app/client/substrate/webgpu/renderer.cljs` |
| `app.client.webgpu.loop` | `app.client.workspace.runtime` | `src/app/client/workspace/runtime.cljs` |
| `app.client.webgpu.text-input` | `app.client.workspace.text-input` | `src/app/client/workspace/text_input.cljs` |
| `app.client.webgpu.themes` | `app.client.workspace.themes` | `src/app/client/workspace/themes.cljc` |

---

## Phase 0: Baseline + Build Gate

Before any move or extraction:

1. Confirm baseline runtime behavior:
   - editor renders and types
   - sidebar works
   - command panel works
   - agent streaming and trail rendering work
   - `/mock -> /select -> /arrange` works
   - settings open and font/theme changes work

2. Create target directories:
   - `src/app/client/substrate/webgpu/`
   - `src/app/client/workspace/`
   - `src/app/client/workflows/`

3. Confirm compile/build entrypoint is healthy:
   - run `clj -M:dev -m dev`
   - Electric/shadow must start with zero compile errors

---

## Phase 1: Build Active Closure + Quarantine Dead Source

### Law

Two active roots:
- `src/app/electric_flow.cljc`
- `src/app/server_jetty.clj`

Algorithm:
1. Scan all source files under `src/` with extensions `.clj`, `.cljc`, `.cljs`
2. Read each `ns` form
3. Collect local `:require` edges for namespaces that resolve to files under `src/`
4. Include shared requires and `:cljs` reader-conditional requires in `.cljc`
5. Recurse from both roots until the reachable closure is complete

Move rule:
- every source file under `src/` that is **not** in the closure moves to `old-infra/src/...`
- preserve original relative paths under `old-infra/src/`
- keep `old-infra/` tracked in git
- do **not** add `old-infra/` to `.gitignore`
- do **not** move docs, resources, or non-source files in this initiative
- do **not** rewrite moved namespaces during this phase

### Notes

- The closure algorithm is the source of truth.
- Any hand-written dead-file list is informational only and must not override reachability.
- `global_flow.cljs`, `app/file_viewer.cljc`, and `components/*` are determined by closure, not by intuition.

### Verification after Phase 1

1. Run `clj -M:dev -m dev`
   - Electric/shadow starts with zero compile errors
2. Confirm:
   - no active source file imports anything under `old-infra/`
   - client boot path from `electric_flow.cljc` still resolves
   - server boot path from `server_jetty.clj` still resolves

---

## Phase 2: Rename Active Spine

Correct the ontological boundary **before** internal extraction.

Moves:
- `src/app/client/webgpu/editor.cljs` -> `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/webgpu/loop.cljs` -> `src/app/client/workspace/runtime.cljs`
- `src/app/client/webgpu/text_input.cljs` -> `src/app/client/workspace/text_input.cljs`
- `src/app/client/webgpu/themes.cljc` -> `src/app/client/workspace/themes.cljc`

Update all active requires, including:
- `src/app/electric_flow.cljc`
- renamed `runtime.cljs`
- any reachable client/server code referencing the moved namespaces

### Verification after Phase 2

1. Run `clj -M:dev -m dev`
   - Electric/shadow starts with zero compile errors
2. Confirm runtime behavior:
   - app boots
   - editor renders
   - sidebar works
   - commands execute
   - agent streaming works
   - all baseline interactions remain functional

---

## Phase 3: Extract Modules from `runtime.cljs`

Extraction order is locked.
After **each step**, the compile gate and smoke checks must pass before continuing.

### Step 1: `workspace/events.cljs`

Move:
- `>canvas-resize`
- `>wheel`
- `>mouse`
- `snap-to-dpr`
- `maybe-snap`
- `parse-key-event`
- `>keyboard`
- `make-raf-flow`
- `make-blink-timer`
- `<global-events`
- `<editor-keys`
- `<cmd-panel-keys`
- `<chat-input-keys`
- `<settings-panel-keys`

Notes:
- event routing belongs here
- no settings helpers in this file

Risk: Low

### Step 2: `workspace/rect_tree.cljs`

Move:
- `wrap-line`
- `rt-node`
- `normalize-padding`
- `layout-children`
- `resolve-text-layout`
- `resolve-layout`
- `tree->rects`
- `tree->text-ops`
- `tree->shadows`
- `hit-test`
- `dispatch-event`

Risk: Low

### Step 3: `workspace/ui_primitives.cljs`

Move:
- `dt`
- typography defs
- all generic UI primitives (`ui-card`, `ui-badge`, `ui-button`, `ui-panel`, `ui-list-item`, etc.)
- `build-empty-state`

Risk: Low

### Step 4: `workspace/sidebar.cljs`

Move:
- sidebar constants
- `split-filename`
- `flatten-file-tree`
- `compute-sidebar-content-height`
- `build-sidebar-tree`

Risk: Low

### Step 5: `workspace/trail.cljs`

Move:
- markdown inline/block parsing
- trail grouping helpers
- `trail->chat-nodes`
- `trail->display-lines`
- agent panel sizing helpers

Risk: Low

### Step 6: `workspace/shell.cljs`

Move:
- `build-file-layout`
- shared pane offset helpers

Notes:
- this file owns the shared code/chat/preview shell
- ticket-list wrappers do **not** belong here

Risk: Medium

### Step 7: `workspace/settings_view.cljs`

Move:
- `manifest-defaults->settings`
- `compact-map`
- `font-defaults->settings`
- `slider-specs`
- `compute-settings-panel-rects`
- `<settings-panel-rects`
- `compute-settings-panel-text`
- `<settings-panel-text`

Risk: Low

### Step 8: `workspace/cmd_panel.cljs`

Move:
- `cmd-panel-apply-event`
- `cmd-prompt-text`
- `cmd-text-start-x`
- generic command parsing shell
- `<cmd-panel-rects`

Ownership rule:
- `cmd_panel.cljs` parses generic command structure only
- generic commands stay runtime/workspace concerns
- workflow command vocabularies are delegated by runtime to `dg_flow` and `jit`

Risk: Medium

### Step 9: `workspace/agent.cljs`

Move:
- ticket/result parsing helpers
- `stream-agent-run!`

Risk: Low

### Step 10: `workflows/dg_flow.cljs`

Move:
- `flow-transitions`
- `valid-transition?`
- `initial-flow-state`
- `transition-flow-state`
- `flow-canvas-active?`
- Screen 1 constants/helpers
- drag helpers
- `build-right-detail`
- `build-intake-tree`
- ticket-list rect/text wrapper helpers
- DG prompt text
- DG command parsing and handling

Risk: Medium

### Step 11: `workflows/jit.cljs`

Move:
- JIT command parsing
- extract/hardcode helpers
- current inline JIT demo builders

Risk: Low

### Step 12: `workspace/editor_compute.cljs`

Move:
- `editor-apply-event`
- `calculate-logical->visual`
- `build-line-mapping`
- `compute-fold-state`
- `compute-editor-rects`
- `<editor-rects`

Keep in `runtime.cljs`:
- `<fold-state`
- `<bracket-match`

Reason:
- `<fold-state` and `<bracket-match` are thin reactive wrappers
- `<editor-rects` is large enough to justify extraction despite being reactive

Risk: HIGH

### Step 13: `workspace/combined_text.cljs`

Move:
- `<combined-text-ops`

Rules:
- this is the **final extraction**
- do not move it earlier
- it is the highest-risk text join and depends on the upstream module boundaries already being stable

Risk: HIGH

---

## Phase 4: Final `runtime.cljs` Shape

After all extractions, `runtime.cljs` owns:
- atom creation and state ownership
- `m/watch` / `m/latest` / `m/sample` composition
- event consumers
- mutation-heavy routing
- top-level command dispatch shell
- upload orchestration
- final draw loop

`runtime.cljs` must **not** own:
- rect-tree implementation
- markdown/trail formatting
- workflow state machines
- JIT demo builders
- settings view builders
- generic UI primitives
- text-input helpers
- theme definitions

It is acceptable for `runtime.cljs` to remain large.
It is **not** acceptable for it to remain semantically mixed.

---

## Dependency Rules

- `renderer.cljs` depends on nothing in `workspace/` or `workflows/`
- `workspace/*` may depend on workspace modules
- `workflows/*` may depend on workspace modules
- `cmd_panel.cljs` does not own workflow vocabularies
- `flow-canvas-active?` is a workflow concern and lives in `dg_flow.cljs`
- `components/design-tokens` stays where it is in this initiative
- `renderer.cljs` is renamed but not internally rewritten as part of this split

For high-risk joins:
- `editor_compute.cljs` and `combined_text.cljs` may receive workflow-specific builders/flags from `runtime.cljs`
- avoid introducing bad dependency direction only for convenience

---

## Verification Protocol

After **every unit** — cleanup move, rename checkpoint, or extraction step:

### 1. Compile gate

Run:

```bash
clj -M:dev -m dev
```

Pass condition:
- Electric/shadow starts with zero compile errors
- changed namespaces recompile cleanly

If compile fails, stop immediately and fix before continuing.

### 2. Runtime smoke gate

Confirm:
- browser loads and WebGPU canvas renders
- editor typing works
- cursor movement works
- selection works
- sidebar click/expand/navigation works
- command panel opens and submits
- agent panel streams and trail renders
- click-to-navigate still works
- `/mock -> /select -> /arrange` works
- settings open and arrow-key navigation work
- font/theme changes work
- horizontal and vertical scroll work in all panes
- `/hardcode button` renders preview pane

If smoke fails, stop immediately and fix before continuing.

### 3. Invariant

The `0.56` char-width factor must remain aligned between:
- `runtime.cljs`
- `editor_compute.cljs`
- `renderer.cljs`

---

## Risk Map

| Unit | Risk | Why |
|------|------|-----|
| Phase 1: closure + quarantine | Low | no active code logic change, only source moves |
| Phase 2: spine rename | Medium | requires update across active closure |
| Steps 1-5 | Low | mostly pure extraction |
| Step 6: shell | Medium | shared layout composer |
| Step 8: cmd_panel | Medium | command-path coverage matters |
| Step 10: dg_flow | Medium | many runtime references |
| Step 12: editor_compute | HIGH | Missionary pressure zone, `<editor-rects` is large |
| Step 13: combined_text | HIGH | 28-arg central text join, final extraction |

---

## Assumptions

- "Active" means reachable from `electric_flow.cljc` or `server_jetty.clj`
- `old-infra/` is tracked quarantine, not trash and not ignored state
- No new runtime abstractions (`LocalWorld`, `Split`, etc.) are introduced in code during this initiative
- Server files are preserved by closure; no server architecture rewrite is part of this plan
- The only intentionally large file at the end is `workspace/runtime.cljs`
