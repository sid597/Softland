# W1-B — the primitive vocabulary (phase artifact)

2026-07-11 · lane W1-B (Opus 4.8 subagent, dispatched by the Fable orchestrator).
Realizes CONTRACT §6 (registry + vocabulary) and gates §11 **G7–G9**.
Gates green in-context; this is the lane's definition of done. W1-INT wires the
real registry (A's interpreter × B's vocabulary) and runs the assembly-level
falsifier (G14) — NOT here.

## Files

- `src/app/client/workspace/face_primitives.cljc` — the vocabulary + registry.
- `test/app/face_primitives_test.clj` — gates G7–G9 (7 tests, 135 assertions).

Fence honored: new files only; `ui_primitives.cljs`, `trail_face/*`,
`rect_tree.cljc` untouched (READ-ONLY). No git state changed (orchestrator owns
commits). No fixtures dir needed — G8/G9 goldens are inline literals.

## What the vocabulary is

A plain-map `registry` (§6: no global mutable state, tests pass their own),
`{<prim-kw> → (fn [ctx props children] → rt-node)}`, 16 entries:

| keyword | source | measure |
|---|---|---|
| `:panel` `:panel-header` `:panel-content` `:panel-footer` `:panel-group` `:list-item` `:card` `:badge` `:divider` `:empty-state` `:scrollbar` | verbatim copies of `ui_primitives.cljs` builders, wrapped into §6 | wrapper measures bounds bottom-up from built children, then calls the original bounds-passing builder |
| `:omissions` `:hole-card` | verbatim copies of `trail_face/cards.cljc` (`omissions-block`, `hole-endpoint-card` + `omission-line` dep) — the D-005 "every lack the face exposes" affordances | original builders self-measure |
| `:stack` | genuinely new (§6) — bare layout node, `:layout` passthrough | column: h = Σ children + gaps + pad; row: w = Σ widths |
| `:text-run` | genuinely new (§6) — prose block | **wraps ONCE via `wrap-line`, emits own positioned ops, h = lines × line-height** |
| `:indent-rail` | genuinely new (§6) — outline indent guide | h = Σ children + gaps; layout-skipped gutter rail |

### Three-layer structure (why copies + wrappers)

1. **Verbatim copies** keep their ORIGINAL names/signatures/forms so G7 can pin
   them by source-form equality. `ui_primitives.cljs` is cljs-only (PROBE
   Finding 2) → the copies bring the vocabulary onto the JVM path.
2. **`*-prim` wrappers** are the registry values; they adapt `(ctx props
   children)` to each original, MEASURING bounds before the call (§6 measure
   rule / trap T7 — measure in the primitive, arrange in the engine).
3. **New primitives** written directly in the §6 interface.

`named-id` note: three copied builders (`ui-panel-group`/`ui-list-item`/
`ui-scrollbar`) derive child ids via `(name id)`, assuming a name-able keyword
(their sidebar origin), but the interpreter passes VECTOR-PATH ids (§5). The
wrappers fold the path to a name-able keyword (`[:root 1] → :root.1`) that still
travels with the item on reorder (trap T6). The copies are NOT edited (G7). This
tension is recorded as an extension point for the post-wave harmonization.

## `:text-run` — the one-wrap law (§6 / G8 / PROBE Finding 1 / trap T7)

Wraps ONCE via `rt/wrap-line`, emits its OWN positioned text ops (the
`build-empty-state` pattern, `ui_primitives.cljs:189-242`), sets its own
`:h` = wrapped-line-count × line-height. It does NOT touch `:text-layout` /
`resolve-text-layout` — the dead hook (not an rt-node param, `rect_tree.cljc:45`;
riding it doubles wrap cost ≈2×). Width defaults to `(:content-w geom)`;
max-chars = width ÷ `(:char-advance geom)` (0.56-derived advance only as the
fallback when a bare geom omits it — never a second constant).

Measured (fixture width 760, font 14, advance 7.84): max-chars 96, prose wraps
to 5 lines, node `:h` = 100 = 5 × 20, 5 positioned ops, `:text-layout` nil.

## Gate results (in-context, actually run)

Run: `clj -M:test -e "(require 'app.face-primitives-test) (clojure.test/run-tests 'app.face-primitives-test)"`
→ **7 tests, 135 assertions, 0 failures, 0 errors.**

- **G7 mechanical builder fidelity — PASS.** Source-form DIFF (not
  call-and-compare — originals are cljs, suite is JVM): reads the three files as
  DATA, indexes def/defn forms by symbol, and asserts every symbol defined in
  BOTH `face_primitives.cljc` and an origin (`ui_primitives.cljs` +
  `trail_face/cards.cljc`) is form-identical. **29 forms pinned** (11 named ui
  builders + 3 trail builders + 15 support defs); all 14 named builders covered;
  allowlist empty; ns forms exempt (carve-out). Not vacuous — a mutated copy
  compares non-equal (verified).
- **G8 the two new primitives measure — PASS.** `:text-run` h = lines ×
  line-height, one op per line, ops stack by line-height, one-wrap preserves
  prose, `:text-layout` absent (carve-out: the keyword and `resolve-text-layout`
  symbol appear in NO code form — checked on parsed forms so docstring mentions
  don't false-trip). `:indent-rail` golden: h = 136 = 40+60+20 + 2×8 gaps;
  after `resolve-layout` → `tree->rects`, children stack at y=[0,48,116]
  (non-overlapping), indented x=16, gutter rail at x=8, non-zero bounds.
- **G9 JVM purity — PASS.** Whole vocabulary loads + runs JVM-side; registry is
  a plain map of 16 keyword→fn; every builder returns a valid rt-node (`:type` +
  `:bounds`) under a call; no js/-namespaced symbol in any code form; no reader
  conditionals.

## Decisions / flags

- **feed-entry-card NOT harvested** (judgment call, recorded): it pulls a heavy
  private-helper chain (`band-line-ops` / two-clock stamps / `kind-glyph` …) and
  its band-specific measure is trail-view-specific, not outline-generic. The two
  lightweight, self-contained rt-node builders (`omissions-block`,
  `hole-endpoint-card`) were harvested instead — a bounded, D-005-relevant
  harvest. feed-entry-card is a named extension point, not a void.
- **G7 extended beyond CONTRACT §11's letter** (which names only
  `ui_primitives.cljs`) to also pin the `trail_face/cards.cljc` copies — honors
  the copy-then-harmonize ruling ("the two cannot drift while both exist") for
  ALL copies. Strengthening, no downside.
- No binding-doc conflict; nothing escalated.
