# Composable GPU Component Library

> Status: ALL PHASES COMPLETE (2026-02-27)
> Phase 1: Layout Engine ✅ | Phase 2: Components ✅ | Phase 3: Rewrite ✅ | Visual Tuning ✅

## Context

**Philosophy**: Zed's insight — 5 GPU primitives (rect, shadow, text, quad, path), infinite components through pure composition. shadcn shows the "how" — small composable slots, parents arrange children. Our stack is functional Clojure all the way: components are pure functions that compose data (rt-node trees), not objects that manage state.

**Problem**: `build-intake-tree` is 350 lines of manual pixel arithmetic (`{:x 12 :y 17}`). The current `ui-card`/`ui-badge` components are monoliths — they handle their own styling but the caller must pre-compute every child's coordinates. This violates composition: layout knowledge leaks into the call site.

**Solution**: Add a `:layout` directive to `rt-node` (the only structural primitive). Parents declare `{:direction :column :gap 8 :padding 12}` and the system computes child positions. Components become thin composable functions that declare structure and style, not coordinates.

## Current implementation homes

- `src/app/client/workspace/rect_tree.cljs` — `rt-node`, layout engine, tree walkers
- `src/app/client/workspace/ui_primitives.cljs` — composable UI helpers
- `src/app/client/workspace/sidebar.cljs` — sidebar composition
- `src/app/client/workspace/shell.cljs` — shared workspace shell composition
- `src/app/client/workspace/runtime.cljs` — orchestration and consumers
- `src/app/client/substrate/webgpu/renderer.cljs` — GPU draw path (still untouched by this architecture note)

> Historical note: this doc originally pointed at `src/app/client/webgpu/loop.cljs` because the layout engine and UI primitives still lived inside the monolith at the time.

## Phase 1: Layout Engine (the foundation)

### Step 1.1: Extend `rt-node` (line 593)

One new optional kwarg `:layout`. Nodes without it are identical to today.

```clojure
;; The only structural primitive — gains layout awareness
(defn rt-node [id type bounds & {:keys [style actions children text clip? data layout]
                                  :or {clip? false}}]
  {:id id :type type :bounds bounds :style (or style {}) :actions (or actions {})
   :children (vec (or children [])) :text (or text []) :clip? clip?
   :data data :layout layout})
```

### Step 1.2: Layout computation (new pure fns, insert after `rt-node`)

Three functions, each pure data→data:

1. **`normalize-padding`** — `12` → `[12 12 12 12]`, `[8 16]` → `[8 16 8 16]`

2. **`layout-children`** — takes a node with `:layout`, computes child `:x/:y`:
   - `:direction :column` — stack top-to-bottom, respect `:gap` and `:padding`
   - `:direction :row` — stack left-to-right
   - `:align :start/:center/:end` — cross-axis alignment
   - `:auto-height? true` — parent `:h` = sum(children) + gaps + padding
   - Children without `:layout-skip?` get positioned; skip-children pass through (for absolute overlays)

3. **`resolve-layout`** — recursive pre-pass that walks tree depth-first, calls `layout-children` on each node, then recurses into children. Pure function. Returns fully-positioned tree ready for the existing tree walkers.

### Step 1.3: Text auto-flow (optional on nodes)

Nodes with `:text-layout {:line-height :max-chars :padding}` get text ops auto-positioned. Caller provides `:text`, `:size`, colors — but NOT `:x/:y`. `resolve-text-layout` computes them using vertical stacking + wrapping via existing `wrap-line`.

### Step 1.4: Wire into call sites (lines 1429-1444)

```clojure
;; One-line change at each wrapper — tree walkers untouched
(let [tree (resolve-layout (build-intake-tree ...))]
  {:rects (tree->rects tree) :shadows (tree->shadows tree)})
```

## Phase 2: Composable Components (pure fns → rt-nodes)

Each is a thin function that returns an rt-node with `:layout`. No state, no side effects. They compose via `:children` — just like Clojure data structures compose via nesting.

### Container tier (shadcn Sidebar = our Panel)

- **`ui-panel`** `[id bounds & {:keys [children variant]}]` — column layout container, bg from `dt`
- **`ui-panel-header`** `[id w h & {:keys [children text style]}]` — fixed-height slot, bottom border
- **`ui-panel-content`** `[id w h & {:keys [children clip? layout-opts]}]` — scrollable area, `:clip? true`, column layout
- **`ui-panel-footer`** `[id w h & {:keys [children text style]}]` — fixed-height slot, top border

### Group tier (shadcn SidebarGroup)

- **`ui-panel-group`** `[id w & {:keys [label icon icon-color collapsed? items font-size]}]` — labeled section, header + collapsible item list, height = header-h + (items × row-h)

### Item tier (shadcn SidebarMenuItem + MenuButton + MenuBadge)

- **`ui-list-item`** `[id w & {:keys [title leading trailing selected? hovered? ghost? data]}]` — row layout, slots for leading/title/trailing components
- **`ui-checkbox`** `[id & {:keys [checked? size]}]` — leaf visual, rounded rect + fill
- **`ui-priority-dot`** `[id priority]` — leaf visual, circular color dot

### Composition example

```clojure
;; Reads like a description of what you want, not how to position pixels
(ui-panel :left {:x 0 :y sy :w left-w :h vh}
  :children
  [(ui-panel-header :hdr left-w 36
     :text [{:text "DISCOURSE-GRAPH  10 active" ...}])
   (ui-panel-content :list left-w (- vh 36)
     :children
     [(ui-panel-group :grp-progress left-w
        :label "In Progress" :icon "●" :collapsed? false
        :items [(ui-list-item :t-0 left-w
                  :title "Fix SSO auth flow"
                  :leading (ui-checkbox :cb-0 :checked? true)
                  :trailing (ui-priority-dot :pd-0 1)
                  :selected? true)
                (ui-list-item :t-1 left-w
                  :title "WebGPU text rendering"
                  :leading (ui-checkbox :cb-1 :checked? false))])])])
```

## Phase 3: Rewrite `build-intake-tree`

### Step 3.1: Extract `build-right-detail`

Pull the 3-branch cond (empty/no-selection/single/multi) into its own fn. Use `ui-card` with `:text-layout` for auto-positioned detail text.

### Step 3.2: Replace left pane (~120 lines → ~40 lines)

Replace manual `ticket-list-layout` → `mapv entry case` with `ui-panel` > `ui-panel-header` > `ui-panel-content` > `ui-panel-group` > `ui-list-item`. The layout engine handles all Y-stacking.

### Step 3.3: Replace right pane

Use `ui-panel` + conditionally rendered `ui-card` children.

### Step 3.4: Keep overlays manual

Drop zone borders and floating drag ticket use absolute world-space coordinates. These stay as raw `rt-node` calls — `:layout-skip?` exempts them from auto-positioning.

## What Does NOT Change (the 5 primitives are untouched)

- **Rect primitive**: `tree->rects`, `update-rects`, SDF fragment shader — unchanged
- **Shadow primitive**: `tree->shadows`, `update-shadows`, shadow fragment shader — unchanged
- **Text primitive**: `tree->text-ops`, `shape-text`, MSDF text pipeline — unchanged
- **Tree walk**: `hit-test`, `dispatch-event` — unchanged
- **GPU pipeline**: `init-rect-system`, `init-shadow-system`, `draw-frame!` — unchanged
- **Reactive flows**: all Missionary flows (`m/latest`, `m/watch`, `m/sample`) — unchanged
- **editor.cljs, electric_flow.cljc** — unchanged

## Constants (line 775-781)

`list-row-h=24`, `list-group-header-h=28`, `list-padding-x=12`, `list-checkbox-size=12`, `list-left-pane-pct=0.40`, `list-divider-w=1`

## Verification

1. Phase 1: UI renders identically (resolve-layout is no-op on nodes without `:layout`)
2. Phase 2: new fns exist, no visual change (not called yet)
3. Phase 3: intake list identical, `build-intake-tree` ~60% shorter
4. Browser console: no WebGPU validation errors
5. Interactions: click rows, hover, collapse groups, drag&drop — all still work
