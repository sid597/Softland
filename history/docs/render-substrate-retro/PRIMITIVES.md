# Render Substrate — Primitive Inventory (as-built, 2026-07-04)

Companion to `RETRO.md` (same directory). This file is the mechanical
inventory; the retro holds the judgment. Compiled during the Track-B
render-substrate retro (Fable session, 2026-07-04): Fable direct-read of
`renderer.cljs`, `electric_flow.cljc`, `trail.cljs`, `rect_tree.cljs`,
`runtime.cljs`, `runtime/render.cljs`, `runtime/state.cljs`,
`runtime/scroll.cljs`, `combined_text.cljs`, `shell.cljs`,
`file_viewer.cljc`; two Opus Explore passes covered the remaining breadth
(buffer_pool, gpu_budget internals; sidebar/settings/cmd-panel/dg_flow/jit
details; exact line refs marked ⓘ are agent-sourced, spot-checked but not
all independently re-read).

Consumed by: Track-B view-MVP contract; Track-D ideal-framework study
(input d). Per the fresh-cut rule this file describes ONLY what exists —
no gaps, no proposals (those live in RETRO.md).

## Layer 0 — GPU substrate (`src/app/client/substrate/webgpu/`)

### Draw primitives (5 pipelines, all instanced quads, single render pass)

| Primitive | Stride | Per-instance attributes | Source |
|---|---|---|---|
| Rich rect | 28 floats | x/y/w/h, rgba, per-corner radii, per-side border widths, border rgba, 2-stop linear gradient (angle + stop + color2) | `renderer.cljs:6-126,516` |
| Shadow | 20 floats | expanded rect, rgba, corner radii, blur/offset-x/offset-y/spread (Gaussian-CDF falloff via erf) | `renderer.cljs:128-231,920` |
| MSDF glyph | 12 floats | x/y/w/h, uv bounds, **rgba per glyph** | `renderer.cljs:233-280,690` |
| Slug glyph | 24 floats | x/y/w/h, sample bounds, inverse Jacobian, banding, glyph loc (u32×4), **rgba per glyph** | `renderer.cljs:282-477,746` |
| Clear-quad | none | fullscreen triangle, used only by (dormant) dirty-present | `renderer.cljs:1022-1046` |

ABSENT as primitives: line/segment, bezier/curve, image/sprite texture,
rotation on anything, circle (expressible as rect with radius = half-size),
per-instance z. No depth/stencil buffer, no MSAA, no compute pipelines.
Blend: straight alpha (`src-alpha`/`one-minus-src-alpha`) everywhere.
Z-order = painter's order — a hardcoded draw-call sequence in
`draw-frame!` (`renderer.cljs:1538-1731`): shadows → sidebar pool →
editor pool → content text → agent bg → cmd bg → chrome text → caret →
status bg → settings → diagnostics. Chrome text sub-ranges are drawn by
`firstInstance` offsets into one buffer via `:line-offsets`.

### Camera

One global uniform `{pan: vec2, zoom: f32, screen_dimensions: vec2}`
applied in every vertex shader: `ndc = ((world*zoom)+pan)/screen*2-1`
(`renderer.cljs:7,41-43`). In practice: `zoom` hardcoded `1.0` at both
call sites (`renderer.cljs:1549`, `runtime/render.cljs:446`); `pan-x`
always 0; `pan-y = -scroll-y` — global vertical scroll IS the camera.
Fixed-position chrome counter-compensates by adding `scroll-y` to its own
y coordinates (`combined_text.cljs:96,158,175`; `dg_flow.cljs:706` ⓘ).
A `!zoom-factor` atom exists (`global_flow.cljs:59` ⓘ) but is unwired.
Zoom-aware AA already present in slug shader (`renderer.cljs:320-321`).

### Text machinery

- Two backends, exclusive per text system: MSDF (rgba8 atlas) and Slug
  (curve rgba16float + band rg16uint textures, analytic GPU coverage).
  Backend mismatch throws (`renderer.cljs:814,865,1342`).
- CPU shaping (`shape-msdf-line`/`shape-slug-line`,
  `renderer.cljs:1147-1233`): input = vector of lines, each a vector of
  runs `{:text :x :y :size :r :g :b :a}`; output = one instance per glyph.
  **Uniform monospace advance** `fsize × char-width` (default 0.56); glyph
  metric advance in the atlas is ignored. `\newline` resets x/advances y;
  `\space` advances. **Missing glyph: silently skipped AND no advance**
  (the advance sits inside the glyph-lookup `when-let`,
  `renderer.cljs:1169-1184`) — see RETRO fix V3-5.
- Per-RUN font size supported (`:size` per run) — headers at larger sizes
  work today (trail tables use it, `trail.cljs:915-920`). Per-glyph color
  and alpha supported. Bold/italic/underline/background as glyph
  properties: ABSENT — bold/italic are color variants only
  (`trail.cljs:7-15`, comment "until font variants exist").
- One font family per text system per frame; content + chrome are two
  systems sharing font resources (`clone-text-system`,
  `renderer.cljs:902`).
- Text upload is FULL rebuild: any change to the ops vector re-shapes and
  re-writes the whole instance buffer (`update-text-data`,
  `renderer.cljs:1338-1396`).

### Buffers & capacity

- Direct systems (text/rect/shadow/cmd/settings): allocate-on-overflow,
  copy-free replace (`ensure-text-instance-buffer` `renderer.cljs:1249`,
  `update-rects` `renderer.cljs:1399`). Boot capacities: content text
  1,000,000 instances, rects 50,000, shadows 256
  (`renderer.cljs:1091-1104`); chrome text 2,000 (`render.cljs:512`).
- Differential pools (`buffer_pool.cljs` ⓘ): slot pools with grow-by-
  doubling + `copyBufferToBuffer` (ⓘ:137-154); three diff engines —
  keyed-by-:id (ⓘ:198), ordered (z-correct, ⓘ:259), positional batch
  (ⓘ:311); O(changed) `writeBuffer` calls; generation-validated handles
  degrade to console.warn. Used by: sidebar rects (keyed), editor rects
  (ordered), both shadow sets (batch).
- `gpu_budget.cljs` ⓘ: accounting/warn only (0.8 of adapter limits), no
  enforcement.

### Frame loop

`m/sample` of a 13-input `m/latest` world snapshot on a self-rescheduling
RAF flow; `m/reduce` with `identical?` skip (the CLAUDE.md pattern),
`runtime/render.cljs:78-555`. Reduce state carries ~24 `prev-*` keys;
per-subsystem `identical?` gates decide re-shape/re-upload. Dirty-present
(persistent render target + scissor + clear-quad + copy-to-swap) is fully
implemented but OFF (`use-persistent-render-target? false`,
`render.cljs:16`) — every dirty frame is a full-viewport redraw.
Instrumentation exists: `[RAF]` log when a dirty frame exceeds 5ms, with
prep/text-gpu/rects-gpu/draw breakdown (`render.cljs:479-482`).

## Layer 1 — Scene graph & layout (`workspace/rect_tree.cljs`)

- `rt-node`: id, type, parent-relative bounds, style (bg, radius,
  corner-radii, border-width(s), border-color, gradient(+color2), shadow),
  text ops, children, `:clip?`, `:data`, `:actions`, `:layout`.
- Layout engine (`resolve-layout`): column/row stacking with gap, CSS
  padding shorthand, cross-axis align start/center/end, `:auto-height?`
  shrink-wrap, `:layout-skip?` opt-out. Plus `resolve-text-layout`
  (`:text-layout {:max-chars :line-height :padding}` auto-wrap+stack).
- Flatten walks: `tree->rects` / `tree->text-ops` / `tree->shadows` —
  parent-relative → absolute, painter's order. With `:clip?` on a node:
  fully-offscreen subtrees are culled; text ops are dropped per op when
  the baseline leaves the clip band and truncated at the right edge
  (0.56-arithmetic, `rect_tree.cljs:279-294`). **Partially visible bg
  rects are emitted at full size — no clamping** (see RETRO fix T-4).
- Hit-testing: `hit-test` point → root-to-leaf node path (front-to-back);
  `dispatch-event` bubbles target→root through `:actions {:event fn}`.

## Layer 2 — Text/wrap/markdown (`trail.cljs`, `electric_flow.cljc`)

- `wrap-line` (char-count greedy word wrap, hard-split long words,
  `rect_tree.cljs:7-38`); `wrap-md-spans` (styled-span wrap,
  `trail.cljs:95-133`). All width math is `count × char-advance`
  (monospace assumption); no pixel-metric measurement.
- Markdown: `parse-md-blocks` (`trail.cljs:210-368`) — headers,
  paragraphs, fenced code, blockquotes, bullet/numbered/task lists, GFM
  tables (with header bg + row/col rules), `★ Insight`-style callouts,
  horizontal rules; inline `parse-md-inline-spans` (bold/italic/strike/
  code/link → color styles).
- `trail->chat-nodes` (`trail.cljs:501-1233`): agent-run trail → typed
  card rt-nodes — reasoning (full markdown), thinking (dimmed,
  collapsible), tool cards (status dot with shimmer pulse when pending,
  collapsible 2-line result, `:nav` click target from file paths),
  tool-groups (3+ consecutive file ops collapse). `trail->display-lines`:
  the same trail as flat colored lines (bottom overlay variant).
- Editor text: Lezer tokenization + `layout-tokens`
  (`electric_flow.cljc:315-355`) — token `:from` × advance grid, fold-
  aware visual/logical line mapping, 6 syntax themes (`themes.cljc` ⓘ).

## Layer 3 — Views, modes, events (workspace + workflows)

- **No central router.** Mode = `derive-effective-local-world`
  (`workspace_actions.cljs:125-206` ⓘ) → `:editor | :file-workspace |
  :flow-intake | :flow-run`, recomputed by add-watch over 7 atoms
  (`runtime.cljs:301-322`). Overlays: settings modal, sidebar, cmd panel,
  extract-preview.
- Views in daily reach: editor (virtualized: only visible lines tokenized/
  laid out, two paths at the >500-line threshold,
  `combined_text.cljs:263-318`); sidebar (rt-node tree, cached in
  `!sidebar-scene` — ONE build serves render + hit-test); 3-pane file
  workspace (`shell.cljs`: editor | chat | preview; chat = trail cards in
  a `:clip?` scroll container with clamped scroll); dg_flow master-detail
  (grouped ticket list, drag-reorder FSM with ghost + drop-zone, run view
  with lane status + trail stream); settings modal; cmd panel + status
  bar; jit extract-preview (hardcoded demo tree).
- **Only the editor virtualizes.** Chat/trail, sidebar, dg lists build
  every node per change and rely on clip-culling at flatten (agent-2
  table, ⓘ).
- Events: Missionary flows for mouse/wheel/keyboard/resize + RAF/blink/
  shimmer timers (`events.cljs` ⓘ); keyboard routed by `!focus` via
  eduction-deref (the safe pattern); wheel routed by mouse zone
  (`scroll.cljs`); clicks by geometric zone cascade then rect-tree
  hit-test (`mouse.cljs:393` ⓘ). Chat/flow trees are REBUILT at click
  time for hit-testing (`mouse.cljs:325,374` ⓘ); sidebar uses its cached
  scene.
- Per-region scroll: 7 scroll atoms; offsets subtracted at tree-build
  time; clamped against content heights computed by summing wrapped-line
  counts (`compute-agent-panel-h`, `shell.cljs:144-147`,
  `scroll.cljs`).
- Electric/server boundary: boot + five `Watch*` e/defns
  (`file_viewer.cljc:105-133`) — `e/watch` on server mirror atoms (Rama
  1.6.0 foreign-proxy workaround) → client `reset!` → `add-watch`
  reconcilers → local atoms → `m/watch` flows. Writes go back as
  fire-and-forget HTTP (`sidebar_io.cljs` ⓘ).
- Design tokens `dt` + typography scale (`ui_primitives.cljs:30-40`);
  panel/list/checkbox/empty-state components used by dg_flow; six
  components currently dead (badge, button, progress, scrollbar, tabs,
  tooltip — ⓘ, grep-verified by agent).

## The 7-step recipe a new full-screen view followed last time (dg_flow)

Recorded verbatim from the as-built precedent (agent-extracted ⓘ,
spot-checked): (1) mode branch in `derive-effective-local-world`;
(2) `local-world-<x>?` predicate; (3) `build-<x>-tree` + compute-rects/
compute-text-ops wrappers; (4) branch in `<editor-content` mode case
(`editor_compute.cljs:457-464`) and `<content-text` mode branch
(`combined_text.cljs:253-255`); (5) pass compute fns into
`render-consumer` (`render.cljs:49,62`); (6) scroll zone (`scroll.cljs`)
+ mouse routing (`mouse.cljs`) + click handler; (7) an entry command
dispatched via `agent_flow.cljs`.
