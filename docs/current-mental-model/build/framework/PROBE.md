# Framework Step-0 PROBE — evidence, not architecture

2026-07-11 · probe session (Opus 4.8 subagent, dispatched by Fable). Evidence-grade findings from a thin vertical slice of the faces-as-assemblies interpreter, measured against the SHIPPED render path (`rect_tree.cljc` layout engine + `wrap-line` + `resolve-layout`). **The probe code is UNCOMMITTED evidence** (`src/app/client/workspace/face_probe.cljc` + `test/app/client/workspace/face_probe_test.clj`); Wave 1 harvests or discards it. Nothing here is a contract; the numbers and the rt-node gaps are.

## What was built

- A two-stage walker (ROAD.md Grammar v0): `compile-assembly` (EDN → validated builder closure, wear-time) and `apply-assembly` (closure × data-context → rt-node tree, then one `resolve-layout`, data-change-time). Recursion is plain function recursion; `:each` = `mapv` over `(get-in ctx path)`; `:bind` = `get-in` on the current data context (prop values only); unknown `:prim` / malformed node → an error-card rt-node, **never a throw** (verified by 4 acceptance tests).
- A plain-map registry `{:stack :text-run :block-card :turn-card}` → thin builders over `rect_tree.cljc` + `components.design-tokens` (both cljc).
- One hardcoded five-key assembly (ROAD.md's outline-face, simplified) + a hardcoded conversation (turns → blocks). Benchmarks scale a *generated* conversation through the *same* compiled assembly.
- 6 tests / 25 assertions, all green, JVM-side. Also verified the sample tree flattens through the exact render primitives `render.cljs`/`combined_text.cljs` call — `tree->rects` (9 rects, carrying `:id`/`:radius`/`:border` for keyed pool diff), `tree->text-ops` (13 op rows, correctly wrapped+positioned), `tree->shadows` — proving render-compatibility without a browser.

## The numbers

**Environment:** OpenJDK 21.0.11, AMD Ryzen 9 9900X (24 threads), Linux. JVM warm (20-run warmup, median of 100). **JVM numbers are a SHAPE proxy** — browser (ClojureScript/V8) constant factors will differ (likely slower per-op); the *linear scaling* and the *2× double-wrap ratio* below are the transferable facts. Browser confirmation rides Step 1.

### (a) `apply-assembly` on a data change — median ms

Measured with the working `:text-layout` hook attached (the tree that actually renders — see Finding 1). Blocks carry realistic prose (200–2000 chars). Node counts landed close to the ~200/500/1000 targets:

| turns×blocks | rt-nodes | median | min | max | mean |
|---|---|---|---|---|---|
| 10×9 | 201 | **5.18 ms** | 4.82 | 7.04 | 5.27 |
| 25×9 | 501 | **12.26 ms** | 12.15 | 14.22 | 12.37 |
| 50×9 | 1001 | **24.64 ms** | 24.34 | 26.11 | 24.74 |

Cost is ~linear in node count. **Attribution:** `resolve-layout` (the *arrange* pass) is cheap — <0.3 ms even at 1001 nodes; essentially all the cost is the *walk*, and ~half of the walk is text wrapping (see Finding 1).

**ROAD.md's "sub-millisecond at conversation scale" is optimistic and should be corrected.** At a realistic conversation pane (~200 nodes) a full data-change rebuild is ~5 ms two-wrap / ~2.5 ms one-wrap — comfortably inside a 16.6 ms frame, and it is a *data-change* cost, not per-frame (R3's `identical?` skip means unchanged frames are free). At 1000 nodes it is 12–25 ms — one frame, no longer trivial. The L6 coarse-rebuild bet holds at conversation scale; the phrasing should be **"low single-digit ms at conversation scale, ~linear to ~10–25 ms at 1000 nodes."**

### (a′) one-wrap vs two-wrap — the halving

| rt-nodes | two-wrap (rides `:text-layout`) | one-wrap (primitive emits positioned ops) | savings |
|---|---|---|---|
| 201 | 4.87 ms | 2.50 ms | 49% |
| 501 | 12.20 ms | 6.13 ms | 50% |
| 1001 | 24.25 ms | 12.68 ms | 48% |

### (b) `wrap-line` at prose scale

500 blocks × 200–2000 chars, `max-chars = 94` (width 760, font 14, pad 8, 0.56 advance). Total 548,900 chars → 6,195 wrapped lines.

- total: **median 7.79 ms** (min 7.57, max 12.23, mean 8.00)
- per-block: **median 15.6 µs**, mean 16.0 µs

`wrap-line` is not a bottleneck on its own; the ~7.8 ms/500-blocks figure ≈ half the 501-node apply cost, which is exactly why doing it twice doubles the walk.

## The scroll-convention answer (check b) — cited

Two conventions coexist in the shipped code; an assembly-hosted pane must pick one and **declare a content-height**.

1. **Panel/sidebar convention — scroll baked INTO the rt-node tree** (`sidebar.cljs:300-316`): an outer `:clip? true` content node fixed to the viewport slot (`sidebar.cljs:307-310`, height = viewport-h) wraps an inner scroll container translated by `:y (- sidebar-scroll-y)` with a giant height and `:layout {:direction :column}` (`sidebar.cljs:303-306`). Scroll offset is owned by `!sidebar-ui :scroll-y`; the wheel handler clamps against a *separately computed* height (`compute-sidebar-content-height`, `sidebar.cljs:89-99`; clamp `scroll.cljs:52-59`). Clipping is `rect_tree`'s `:clip?` + `intersect-clip` (`rect_tree.cljc:207-223`). Cost: baking `scroll-y` into the tree makes scroll a *data input* → the scene rebuilds on every wheel tick unless separately gated.

2. **Face/camera convention — scroll-INDEPENDENT scene + GPU camera** (trail-face): the scene is built at absolute (0,0), scroll-independent — the build flow explicitly does **not** watch `scroll-y` (`scene.cljc:5-6`, `editor_compute.cljs:352-354`). Content height is DECLARED in root `:data` under `:trail-face/content-h` (`scene.cljc:772`; read via `content-height`, `scene.cljc:796-799`). Scroll rides the WebGPU camera (`pan-y = -scroll-y`, applied in the vertex shader `renderer.cljs:41`); the wheel handler clamps via `clamp-scroll(+ offset delta, content-h, visible-h)` (`scroll.cljs:96-101`; `clamp-scroll` `scene.cljc:62-66`).

**Therefore an assembly-hosted pane should follow the FACE/CAMERA convention** (a "face is an assembly"; trail-face is the precedent). It must declare exactly two things:
- (i) its **content height** in a blessed root `:data` key (the walker already measures it bottom-up — 620 px in the sample), so the wheel handler can clamp; and
- (ii) a **viewport-clip boundary** for the pane slot.
The host routes the pane's scroll atom into the camera pan and clamps against that height. The pane must **not** bake `scroll-y` into the tree (that reintroduces per-wheel rebuilds and forfeits R3's `identical?` skip). Net: **content-height in root `:data` + scroll-independence** is the pane's whole scroll contract.

## rt-node expressiveness — gaps found (do NOT extend `rect_tree`; contract material)

1. **`:text-layout` is not a constructor param, and no shipped code sets it.** `rt-node`'s kwargs are `{:keys [style actions children text clip? data layout]}` (`rect_tree.cljc:45`) — `:text-layout` passed as a kwarg is *silently dropped*. Grep confirms nothing in `src/` attaches `:text-layout` except the dead engine hook itself, so `resolve-text-layout` (`rect_tree.cljc:154-192`) has **never run in production**. ROAD.md's "wrapped text-run rides the existing `:text-layout` hook" is only half-true: the hook *function* works (attached via `assoc`, an op correctly became `:x 28 :y 60` wrapped) — but (a) it must be `assoc`'d on after `rt-node` (or `rt-node` extended, which the fence forbids), and (b) using it **doubles wrap cost**, because the primitive must wrap anyway to compute its own `:h`. **Recommendation:** the Step-1 wrapped-text-run should wrap ONCE and emit positioned ops itself — the `build-empty-state` / `ui-primitives.cljs:189-242` pattern that the shipped code actually uses — not ride `:text-layout`. ~2× cheaper and no dead-hook dependency.

2. **`ui_primitives.cljs` is CLJS-only — a cljc walker cannot require it.** Its fns are pure and dep only on cljc (`rect_tree` + `design-tokens`), but a `.cljs` ns is invisible to the JVM require path, so a `.cljc`/`.clj` interpreter (needed for golden snapshot tests) cannot use it directly. The probe re-expressed the two card/text patterns it needed over `rect_tree` instead. **Step-1 extraction must port the vocabulary cljs→cljc**, not merely reference it — this is a real cost in the "extraction and gap-fill" line, currently under-scoped in ROAD.md.

3. **Height is the primitive's job, confirmed (check d).** `layout-children` only writes parent `:h` under `:auto-height?`, and `resolve-layout` is parent-first — so nested `:auto-height?` reads stale child heights and mis-measures. `resolve-text-layout` never writes `:h` at all. The walker's children-first build order makes bottom-up measurement natural; every primitive computes its own `:h` before the engine arranges. **"Measure in the primitive, arrange in the engine" is load-bearing law, not a rule of thumb** — a walker that leans on `:auto-height?` produces zero/wrong-height cards. No `rt-node` change needed; a contract statement is.

4. **Minor grammar note:** the "five keys" `{:prim :props :children :each :bind}` is really six — an `:each` node needs a `:template` body (as ROAD.md's own example shows). `:template` should be named in the grammar.

Everything else `rt-node` expressed cleanly: `:prim`→builder, `:props`→args, `:children`→children, `:each`→`mapv`, `:bind`→`get-in`; vector/path-based node ids (trail-face's own idiom) give keyed-pool-diff-stable ids across rebuilds when data order is stable (no gensym) — the walker derives ids from the data path, so no rt-node gap there.

## Optional dev-hook mount + screenshot — NOT taken (recorded)

Skipped. Mounting a probe pane into the live path is not a ≤10-line one-file change: the render path is a single `<world-snapshot` `m/latest` over ~40 atoms (`render.cljs:81-118`), so a new pane needs a new atom + a slot in that `m/latest`, a branch in `combined_text.cljs` (text-op flatten), a pane mode + command in `events.cljs`, and a scroll clause in `scroll.cljs`/`state.cljs` — well over the fence. **Substitute evidence** (cheaper and browser-independent): the sample tree was flattened through the real `tree->rects`/`tree->text-ops`/`tree->shadows` (the exact fns the render loop calls) and produced correct, `:id`-carrying, correctly-wrapped output — see "What was built." A live screenshot rides Step 1, where the pane slot is built for real.

## What this changes about the contract (≤5 lines)

1. "Sub-millisecond" is wrong: budget **low single-digit ms at conversation scale (~200 nodes), ~linear to ~12–25 ms at 1000 nodes**, as a data-change (not per-frame) cost behind R3's skip.
2. The wrapped-text-run primitive must **wrap once and emit its own positioned ops** (`build-empty-state` pattern), NOT ride `:text-layout` — 2× cheaper, and `:text-layout`/`resolve-text-layout` is an untested/dead hook (`rt-node` doesn't even expose it).
3. **Extraction is cljs→cljc porting**, not reference — `ui_primitives.cljs` is unusable from the cljc interpreter; scope Step 1 accordingly.
4. Codify **"measure in the primitive, arrange in the engine"** as law (nested `:auto-height?` mis-measures), and an assembly pane's scroll contract as **"declare content-height in root `:data` + stay scroll-independent (ride the camera)."**
5. Name **`:template`** in the grammar (the sixth key `:each` requires).
