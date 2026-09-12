# Rect Tree UI Architecture — Raw Design

> Status: DESIGN PHASE — captured 2026-02-25
> Source: User's verbal description of the target interaction model

---

## Core Principle

**Everything is a rect.** The UI is a tree of nested rectangles. Each rect can have actions attached (click, drag, drop, scroll, zoom). Child rects inherit their parent's coordinate space but can override behaviors. This is the DOM's event model, rebuilt for a single WebGPU canvas.

## The Rect Tree (Screen 1: Intake)

```
BG Rect  [pan: yes, zoom: yes]
│
├── Linear Panel Rect  (left 40%)  [scroll: yes, drop-target: yes]
│   │
│   ├── Group Header Rect  "In Progress (5)"  [click: toggle-collapse]
│   │
│   ├── Ticket Rect  DIS-142  [draggable: yes, click: select]
│   │   ├── ID Badge       "DIS-142"       [fixed, not interactive]
│   │   ├── Title Text     "Fix auth..."   [fixed, not interactive]
│   │   ├── Status Chip    "In Progress"   [fixed, not interactive]
│   │   └── Priority Dot   ● High          [fixed, not interactive]
│   │
│   ├── Ticket Rect  DIS-143  [draggable: yes, click: select]
│   │   └── ... (same internal structure, SOLID — children can't move)
│   │
│   ├── Group Header Rect  "Todo (8)"  [click: toggle-collapse]
│   ├── Ticket Rect  DIS-150  [draggable: yes, click: select]
│   └── ...
│
└── Canvas Panel Rect  (right 60%)  [scroll: yes, drop-target: yes, bg: dotted grid]
    │
    ├── Dropped Ticket Rect  DIS-142  [draggable: yes, click: select]
    │   └── ... (same solid internals as in Linear panel)
    │
    ├── Dropped Ticket Rect  DIS-143  [draggable: yes]
    │   └── ...
    │
    └── (empty space — visual hint: "drag tickets here")
```

## Three DnD Contexts

```
┌─────────────────────────────────────────────────────────────────────┐
│ BG Rect  (can pan/zoom the whole thing)                             │
│                                                                     │
│  ┌───────────────────┐    ┌────────────────────────────────────┐   │
│  │  LINEAR PANEL      │    │  CANVAS PANEL                      │   │
│  │                    │    │                                    │   │
│  │  ┌──────────────┐ │    │    ┌──────────────┐                │   │
│  │  │ Ticket A     │─┼────┼───>│ Ticket A     │  ③ Cross-panel │   │
│  │  └──────────────┘ │    │    └──┬───────────┘    DnD          │   │
│  │  ┌──────────────┐ │    │       │                             │   │
│  │  │ Ticket B     │ │    │       v                             │   │
│  │  └──────────────┘ │    │    ┌──────────────┐                │   │
│  │        ↕          │    │    │ Ticket A     │  ② Within-     │   │
│  │  ① Within-panel   │    │    └──────────────┘    canvas DnD  │   │
│  │     reorder       │    │                                    │   │
│  │                    │    │         · · · · · · · ·            │   │
│  │                    │    │         · · · · · · · ·  (dotted   │   │
│  │                    │    │         · · · · · · · ·   grid bg) │   │
│  └───────────────────┘    └────────────────────────────────────┘   │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘

① Within Linear Panel  — reorder tickets (future: drag between groups)
② Within Canvas Panel  — rearrange dropped tickets spatially
③ Cross-panel (Linear → Canvas) — equivalent to /select (pick tickets for work)
```

## Event Propagation Model

```
Mouse Event (x, y)
    │
    ▼
Hit Test: walk tree depth-first, find DEEPEST rect containing (x,y)
    │
    ▼
Event Dispatch (innermost → outermost, like DOM bubbling):
    │
    ├── If target rect handles this event type → run handler
    │   └── Handler can: stop propagation / let it bubble
    │
    └── If not handled → bubble to parent rect
        └── Continue until BG rect (root)

Example: clicking inside a ticket's Title Text
    1. Title Text rect: no click handler (fixed) → bubble
    2. Ticket Rect: has click handler → runs "select ticket" → stops
    (parent Linear Panel never sees the click)

Example: dragging a ticket
    1. mousedown on Ticket Rect → enters DRAG state
    2. mousemove → ticket follows cursor (detached from panel, rendered on top)
    3. mouseup → hit-test drop target:
       - Over Canvas Panel? → cross-panel drop (③)
       - Over Linear Panel? → reorder drop (①)
       - Over neither? → cancel, snap back
```

## Drag State Machine

```
         mousedown on         mousemove
         draggable rect      (> threshold)
IDLE ──────────────────> PENDING ──────────────> DRAGGING
  ▲                        │                       │
  │                        │ mouseup               │ mouseup
  │                        │ (no drag)             │
  │                        ▼                       ▼
  │                      CLICK                   DROP
  │                        │                       │
  │                        │                       │ hit-test drop zone
  └────────────────────────┘                       │
  └────────────────────────────────────────────────┘
```

- **IDLE**: no mouse interaction
- **PENDING**: mousedown happened, waiting to see if it's a click or drag (5px threshold)
- **DRAGGING**: past threshold, ticket follows cursor, original position shows ghost/placeholder
- **DROP**: mouseup while dragging — check what drop zone we're over
- **CLICK**: mouseup before threshold — normal click handler

## Rect Node Data Structure

```clojure
{:id        :ticket-142          ;; unique ID for hit-testing & state lookup
 :type      :ticket              ;; semantic type (for rendering dispatch)
 :bounds    {:x 20 :y 100 :w 300 :h 48}  ;; in PARENT's coordinate space
 :actions   {:click     (fn [rect event] ...)   ;; optional per-event handlers
             :drag      {:enabled? true
                         :axis :both}            ;; :x, :y, or :both
             :drop      {:accept? (fn [dragged-rect] ...) ;; can this rect receive drops?
                         :on-drop (fn [dragged-rect drop-pos] ...)}
             :scroll    {:axis :y :atom !scroll-y}
             :zoom      {:enabled? true :atom !zoom}}
 :style     {:bg [0.18 0.18 0.22 1.0]     ;; fill color
             :border [0.3 0.3 0.35 1.0]    ;; optional
             :corner-radius 4}
 :children  [{...} {...} ...]    ;; ordered list of child rects (rendered back-to-front)
}
```

## What This Replaces (Current vs Target)

### Current: Flat, ad-hoc
- Rects computed in `<editor-rects`, `<cmd-panel-rects` as flat vectors of GPU instances
- Text ops computed separately in `<combined-text-ops`
- Hit-testing: `(cond (> mouse-y panel-top) ... (< mouse-y cmd-bottom) ...)` — manual, per-panel
- Mouse routing: hardcoded `cond` chains checking Y coordinates
- No drag support at all
- No nesting / containment model

### Target: Tree, declarative
- Rect tree defined per-screen (Screen 1 = intake tree, Screen 2 = arrange tree, etc.)
- Each node: bounds + actions + style + children
- Hit-testing: generic tree walk (one function, works for any screen)
- Event dispatch: bubbling (one function, works for any event type)
- Drag/drop: state machine (one implementation, works across all panels)
- Rendering: walk tree → emit GPU instances + text ops (replaces per-panel compute fns)

## Rendering Pipeline (how rect tree becomes GPU data)

```
Rect Tree
    │
    ▼
walk-tree (depth-first, accumulate transforms)
    │
    ├── For each node: compute ABSOLUTE bounds (parent-x + node-x, ...)
    ├── Emit background rect instance (if node has :style :bg)
    ├── Emit border rect instance (if node has :style :border)
    ├── Emit text ops (if node has text content — dispatched by :type)
    │
    ▼
{:rect-instances [...flat vector for GPU...]
 :text-ops       [...flat vector for text shader...]}
    │
    ▼
Existing GPU pipeline (no changes needed below this point)
```

## Ticket Rect Internals (SOLID — not moveable)

```
┌─────────────────────────────────────────────┐
│ ● DIS-142   Fix auth flow for SSO users     │
│   In Progress  ·  High  ·  @siddharth       │
└─────────────────────────────────────────────┘
  ▲                                           ▲
  │                                           │
  Priority dot (color-coded)      Metadata line (muted color)
  ID badge (mono, bright)         All FIXED position — no child DnD
  Title (main text, may wrap)
```

Internal elements are positioned relative to the ticket rect's top-left. They NEVER move independently — dragging the ticket moves the whole thing as one unit.

## Screen Progression (rect trees per screen)

- **Screen 1 (Intake)**: Linear Panel + Canvas Panel (described above)
- **Screen 2 (Arrange)**: Linear Panel (narrower) + Arrangement Canvas (sequential chain / parallel lanes)
- **Screen 3 (Run)**: Task List + Execution Feed (tool calls, diffs, reasoning trails)
- **Screen 4 (Review)**: Results List + Diff Viewer with tabs

Each screen is a DIFFERENT rect tree. The root BG rect persists; panels swap in/out.

## Implementation Order

1. **Rect tree data structure + generic tree walk** (pure functions, no GPU)
2. **Hit-testing** (point-in-rect tree walk, returns deepest matching node)
3. **Render pipeline** (tree walk → GPU instances + text ops)
4. **Click dispatch** (replace current ad-hoc mouse handlers)
5. **Drag state machine** (mousedown → pending → dragging → drop)
6. **Drop zones** (Linear panel, Canvas panel accept drops)
7. **Visual feedback** (ghost during drag, highlight drop zones, snap-back on cancel)

## Connection to ZUI Vision (Continuous Semantic Zoom)

From `claude-3-moonshot.md`:
> "Editor and graph view are not separate features. They're a continuous zoom on the same data."

The rect tree IS the ZUI data structure:

```
Zoom 1.0    →  You see ticket internals (ID, title, status, description)
                = deep nodes in the tree rendered at full size

Zoom 0.1    →  Tickets become colored cards (just bg rect + title)
                = mid-depth nodes, leaf text ops culled

Zoom 0.01   →  Linear Panel is one colored rect, Canvas another
                = top-level children of BG rect

Zoom 0.001  →  BG rect is one dot in a project landscape
                = root node from outside
```

The BG rect's pan/zoom action IS the ZUI camera. Tree depth = zoom granularity. The rect tree makes zoom a matter of "which depth level do we render down to?" rather than a mode switch.

**GPU pipeline doesn't change for zoom** — 12-float glyphs at 14px and 12-float nodes at 1400px are the same thing to the MSDF shader. The rect tree just changes what rects and text ops it emits based on camera zoom level.

## What Changes vs Current Infrastructure

### UNCHANGED (GPU layer)
- `init-rect-system`: 8 floats/rect stays
- `init-text-system`: 12 floats/glyph stays
- `shape-text`, `update-rects`, `update-text-data`: same input format
- `draw-frame!`: same draw calls
- `<world-snapshot` + `m/sample >raf`: same render consumer

### REPLACED (compute layer)
- `compute-ticket-list-rects` → rect tree walk emits rects
- `compute-ticket-list-text-ops` → rect tree walk emits text ops
- `compute-editor-rects` → editor becomes a rect node (future)
- `<cmd-panel-rects` → cmd panel becomes a rect node (future)
- Manual mousedown `cond` chain → `(hit-test tree x y)` + event dispatch
- `!mouse-y` routing → tree walk finds scrollable ancestor
- `flow-canvas-active?` mode switch → root tree changes per screen

### ADDED (new layer between atoms and GPU)
- Rect tree data structure (nodes with bounds, actions, style, children)
- Generic tree walk (depth-first, emits flat GPU vecs)
- Hit-test function (deepest node containing point)
- Event dispatch with bubbling (innermost → outermost)
- Drag state machine (IDLE → PENDING → DRAGGING → DROP)

## The Big Picture Map (canonical reference)

```
TODAY (flat)                    RECT TREE (next)                ZUI (horizon)
─────────────                  ──────────────                  ─────────────
Flat rect arrays               Nested rect tree               Same tree, but camera moves
Ad-hoc hit-testing             Generic tree walk              Semantic zoom = depth in tree
Mode switch (editor/list)      Screen = different tree        Zoom level = which subtree visible
No drag, no nesting            DnD state machine              Drag at any zoom level
8 floats/rect, 12/glyph       Same GPU format                Same GPU format (!)
```

The critical connection from claude-3-moonshot.md:

> "Everyone thinks 'editor' and 'graph view' are separate features you switch between.
> They're not. They're a continuous zoom on the same data."

The rect tree IS the data structure that makes this true. At zoom 1.0, you see ticket
internals. At zoom 0.1, tickets become colored dots. At zoom 0.01, the whole Linear
panel is one rect. **The tree depth maps to zoom levels.** The BG rect you described —
the one with pan/zoom — that's literally the ZUI camera. We're building the
infrastructure for it right now.

### Current Infrastructure vs Rect Tree — The Gap

```
CURRENT (what exists)                    RECT TREE (what we need)
─────────────────────                    ────────────────────────

RECTS:                                   RECTS:
  compute-ticket-list-rects()              Tree node: {:bounds :style :children}
  compute-editor-rects()                   One walk fn emits all GPU instances
  <cmd-panel-rects                         Parent transforms cascade to children
  → 3 separate functions                   → 1 generic function
  → flat vec of {x y w h r g b a}         → same output format (GPU unchanged!)

HIT TESTING:                             HIT TESTING:
  mousedown: (cond                         (hit-test tree x y)
    (> y settings-top) → settings          → walks depth-first
    (< y status-bar) → status-bar          → returns deepest node containing point
    (< y cmd-bottom) → cmd-panel           → no cond chain, works for any layout
    flow-canvas? → list-view-hit
    :else → editor-hit)
  → 5 manual zones, hardcoded Y coords    → 1 function, any number of zones

EVENT DISPATCH:                          EVENT DISPATCH:
  if zone == :list-view                    node = (hit-test tree x y)
    find row by Y math                     (dispatch node :click event)
    if group-header → toggle               → bubbles up until handled
    if ticket → toggle select              → each node declares its own handlers
  → handler logic embedded in cond         → handlers live ON the nodes

DRAG:                                    DRAG:
  !dragging? atom (text selection only)    Drag state machine:
  No DnD at all                            IDLE → PENDING → DRAGGING → DROP
                                           Works for any draggable node
                                           Drop zones declared on nodes

SCROLL:                                  SCROLL:
  !scroll-y (editor/list)                  Each scrollable node owns its scroll
  !agent-scroll-y (agent panel)            Scroll atom lives in node's :actions
  Routing: check !mouse-y vs panel bounds  Routing: hit-test → find scrollable ancestor

MODE SWITCH:                             MODE SWITCH:
  flow-canvas-active? boolean              Root tree changes per screen
  Big if/else in every rect/text fn        Each screen = different tree structure
  → scattered conditionals                 → one tree, one render walk
```

### What DOESN'T Change

```
GPU pipeline (editor.cljs)         ← UNCHANGED
  init-rect-system                   8 floats/rect stays
  init-text-system                   12 floats/glyph stays
  shape-text                         same input format
  update-rects                       same input format
  update-text-data                   same input format
  draw-frame!                        same draw calls

Missionary reactive flows          ← ADAPTED, not replaced
  <world-snapshot                    still aggregates
  m/sample on >raf                   still drives render
  !atoms                             tree nodes reference same atoms
```

**The rect tree is a layer between atoms and GPU** — it replaces the scattered
`compute-*-rects` and `compute-*-text-ops` functions with one tree walk that
produces the same flat vectors the GPU already consumes.

## Open Questions — RESOLVED (2026-02-27)

- **Pan/zoom on BG rect**: Not needed for Screen 1. Future ZUI feature.
- **Scroll integration**: External atoms (`!scroll-y`, `!agent-scroll-y`). Tree nodes reference atoms via `:actions {:scroll {:atom !a}}` but the atoms live outside.
- **Transition strategy**: Incremental. Rect tree sits alongside existing code; `compute-ticket-list-*` became thin wrappers over `tree->rects`/`tree->text-ops`. Editor still uses flat rects.

## Implementation Status (2026-02-27)

- **Steps 1-7**: ALL COMPLETE (2026-02-25)
- **SDF Rich Quads + Shadows**: COMPLETE (2026-02-27) — 28 floats/rect, rounded corners, borders, gradients, analytical shadow pipeline
- **Layout Engine**: COMPLETE (2026-02-27) — `resolve-layout` pre-pass, `:layout` directive on `rt-node`, CSS flexbox equivalent
- **Composable Components**: COMPLETE (2026-02-27) — shadcn Sidebar pattern: `ui-panel > ui-panel-header > ui-panel-content > ui-panel-group > ui-list-item`
- **Visual Tuning**: COMPLETE (2026-02-27) — taller rows, rounded hover backgrounds, muted section labels
- **Next docs**: `docs/architecture/virtual-layout-engine.md` (layout plan), `docs/architecture/gpu-component-library.md` (component vision)
