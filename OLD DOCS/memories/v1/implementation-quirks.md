# Implementation Quirks — Learned Through Pain

Operational wisdom that prevents wasted time. These are patterns discovered by hitting errors, not from reading docs.

## Edit Tool on large CLJS files
- The Edit tool often fails with "String not found" due to deep indentation (30+ spaces)
- **Workaround**: Use python3 string replacement via Bash when Edit fails on deeply-indented code
- Always re-read the file after modifications before attempting new edits
- After S37 runtime split, no single file exceeds 508 LOC; this is less of an issue now

## Missionary / Reactive Flows
- **m/latest arg counts MUST match fn params** — silent corruption if they don't
  - `<editor-rects`: split into <layout(4), <mode(3), <sidebar(4), <intake-content(7), <run-content(7), <editor-content(17), combiner(6)
  - `<combined-text-ops`: split into <layout(4), <intake-text(7), <run-text(7), main(21)
  - `<cmd-panel-rects` = 11 args
  - These counts change when new atoms are threaded through — always verify after signature changes
- The m/ap + m/?< crash pattern and m/eduction fix are in CLAUDE.md — don't duplicate here

## Reactive Architecture: Coarse Invalidation + Impurity (S38)
- **Problem**: `<editor-rects` and `<combined-text-ops` are single `m/latest` blocks with 25+ watched atoms. When ANY atom changes (e.g. caret blink every 530ms), the ENTIRE function re-runs — including code paths that don't use the changed atom. This is coarse invalidation, not a bug — the reactive graph's invalidation domain is too wide.
- **Impurity compounds it**: `build-right-detail` (dg_flow.cljs:492) looks like a pure builder but secretly mutates `!detail-max-scroll` via `reset!` at lines 502 and 565. Side effects inside derivations are un-Electric — they create hidden edges Missionary can't see.
- **The Electric fix** (three parts):
  1. **Scope** — split each `m/latest` so each branch watches only the atoms it needs
  2. **Purity** — derivations compute and return data, they don't mutate atoms
  3. **Ownership** — each atom has one writer, or better, the value stays derived (a flow, not an atom)
- The unused-branch caveat: even with split flows, all branches stay live unless you use dynamic subscription (second step)

## Rama DSL
- No Java interop inside `<<cond`/`<<sources` — wrap in plain fn
- `local-transform>` = 2 args: `[path-with-termval] pstate`
- Both give cryptic "Unable to resolve symbol: `.`" error

## CLI Integration
- `--max-turns` doesn't exist as a Claude CLI flag — use `--max-budget-usd` instead
- Invalid flags cause **silent exit** (no error, process just dies)
- Agent CWD priority chain: sidebar project root → file parent dir → `"."`
- Ring streaming: Use `StreamableResponseBody` reify (not `piped-input-stream` — Jetty won't flush)

## WebGPU Rendering
- **No GPU scissor rects** — all clipping is software-only (filter render ops outside visible bounds)
- **Clip on text baseline, not bounding box** — `(>= y (+ panel-top padding))` not `(>= (+ y line-step) panel-top)`. Loose checks cause glyph bleed
- **Font atlas only covers ASCII 32-126** — any Unicode renders as blank. Use `> v < ..` not arrows/Unicode
- **Char-width `0.56`** must be consistent across electric_flow.cljc, loop.cljs, editor.cljs — mismatch causes cursor drift
- **Horizontal scroll is per-element, not camera**: GPU camera only does vertical (`pan-y = -scroll-y`). H-scroll via `editor-lx = layout-x - scroll-x`. Gutter uses unscrolled `layout-x`
- **Never redefine a shared coordinate variable** for a per-element transform — create a NEW variable (the h-scroll contamination lesson from S30)

## Electric 3
- `try/catch` NOT supported inside `e/defn` — causes "try is TODO" error
- `e/watch` only works within a single peer — no server→client missionary pipe
- Pattern: HTTP for data transfer, Electric for bootstrap only

## Missionary + Raw DOM: Never Share Mutable State (S35)
- `m/observe` without `m/relieve` **buffers one event**. When `!` is called, the previously buffered value may drain first — a raw mousedown can trigger processing of a stale Missionary mouseup in the same synchronous dispatch
- Symptom: atom flips `true→false` within 1ms, no intermediate events fire
- Rule: if you move state ownership to raw DOM handlers, **remove ALL Missionary writes** to that state
- `add-watch` + `js/console.trace` on the atom is the best diagnostic for identifying the culprit

## Selection / Rect Clipping (S35)
- Any rect computed from character positions must clamp to pane boundary: `(min raw-w (max 0 (- viewport-w x)))`
- 3-pane widths (`0.4 / 0.55 / 0.05`) appear in `shell.cljs`, `editor_compute.cljs`, and `runtime.cljs` — must match everywhere

## Paren Bugs in ClojureScript (S35, S37)
- A missing `)` can push an `:else` cond clause inside a preceding `let` body — syntactically valid, semantically wrong
- `:else` becomes a dead keyword expression; the "else" code runs unconditionally for every match
- Paren checking tools must strip `;` comments and strings before counting
- **S37 "Can't call nil" cascade**: A missing `)` on an inner `let` inside a `cond` branch absorbed the next `:else` branch, making the parent `if` have too many args. The CLJS analyzer reported "Can't call nil" at the outermost `->>` form — 300 lines away from the actual bug. Binary search by stubbing `case` branches with `nil` is the fastest diagnostic.

## set-selection Contract (S37)
- Never use raw `swap! !flow-state assoc :selected new-sel` — always use `swap! !flow-state set-selection new-sel`
- `set-selection` (dg_flow.cljs:62) syncs three coupled fields: `:selected`, `[:batch :lanes]`, `:active-lane-idx` (with clamping)
- Bypassing it causes silent lane drift that only manifests during multi-ticket batch review
- Found in S37 during runtime split: mouse.cljs and keyboard.cljs had copied raw assoc from the monolith

## Shadow-cljs Server Classpath
- Must start with `clj -A:dev -M -m shadow.cljs.devtools.cli server` (not `npx shadow-cljs server`)
- Without `:dev` alias, `src-dev/` is missing from classpath → "The required namespace 'dev' is not available"

## Rama IPC Port Conflicts
- `util_fns.cljc` has a top-level `def ipc` that eagerly creates a Rama InProcessCluster on ns load
- If a previous JVM is still running (`ps aux | grep java`), the new one fails with "System map startup failed"
- Kill stale processes before starting server

## mapv/mapcat Arity with Index (S36)
- `(mapv (fn [idx item] ...) coll)` silently makes `idx=item, item=nil` — NO error in ClojureScript
- Fix: `(mapv (fn [idx item] ...) (range) coll)` to zip index + items
- Same for `mapcat` — found 3 instances of this in trail.cljs table rendering
- Symptom: allocated space (correct height) but empty content (nil cells)

## Rect vs Text Pipeline Consistency (S36, S39)
- `compute-ticket-list-rects` used to pass `font-size=0, char-advance=0` because "rects don't need fonts"
- BUT `build-right-detail` wraps description text to compute heights — different font-size = different wrapping = different heights
- When rect and text pipelines disagree on tree geometry, `m/latest` sees alternating states → infinite re-render oscillation
- Rule: if ANY tree builder uses font metrics for layout (not just text rendering), ALL callers must pass real font-size
- **S39 recurrence**: `handle-mousemove!` and `handle-flow-canvas-click!` in mouse.cljs both called `build-intake-tree` with `font-size=0, char-advance=0` for hit-testing. The rendered tree used real font metrics → row heights differed → hover flickered between adjacent items. Same root cause as S36, different call site.

## Hit-Test Child Stealing Hover (S39)
- **Pattern**: A hovered item adds a `:highlight` child rect. On the next mousemove, `hit-test` returns that child as the deepest node. The hover check (`(= :sidebar-entry (:type target))`) fails because the target is `:highlight`, not `:sidebar-entry`. Hover clears → highlight disappears → next frame the entry is deepest again → hover sets → cycle repeats endlessly.
- **Affected**: sidebar file entries, intake ticket rows — any node that conditionally adds children based on hover state
- **Fix**: Don't use `(peek path)` (deepest only). Walk the hit path with `(some (fn [node] (when (= :target-type (:type node)) ...)) (rseq path))` to find the nearest ancestor of the desired type. This way highlight children, accent bars, indent guides, etc. all resolve to their parent entry.
- **General rule**: If an interactive node can gain/lose children based on hover, the hit-test lookup must walk ancestors, not check only the leaf.

## Layered Backgrounds in Rect Tree (S39)
- `ui-panel`, `ui-panel-header`, and `ui-panel-footer` (ui_primitives.cljs) each paint their own `:bg` from design tokens, on top of any parent background rect.
- Setting a parent `left-bg` to black has no visible effect if `ui-panel` paints `(:bg (:colors dt))` over it.
- Override via the `:style` kwarg: `(ui-panel :id bounds :style {:bg [0.0 0.0 0.0 1.0]})` — the style map is `merge`d over defaults.
- Must override all three layers (panel + header + footer) for a fully consistent background.
