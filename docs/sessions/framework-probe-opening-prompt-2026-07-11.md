# Opening prompt — framework Step-0 probe

**Dispatchable NOW — runs in parallel with the Fable contract session.**
Model: Opus 4.8 (Codex fast-mode is a valid alternative per the 2026-07-06 pool ruling). One fresh session or one subagent context. Budget: half a day.

## Boot (read in this order, nothing else)

1. `docs/current-mental-model/build/framework/ROAD.md` — §"What the substrate actually is", §"Grammar v0", and Step 0. Direction-grade input, NOT a binding contract.
2. `docs/current-mental-model/build/framework/NOW.md` — STANDING rules (precedence, fences, hard rules).
3. `src/app/client/workspace/rect_tree.cljc` — `rt-node`, `resolve-layout`, `wrap-line`, `:actions`/`dispatch-event`.
4. `src/app/client/workspace/ui_primitives.cljs` — the existing pure-fn component library.
5. `src/app/client/workspace/runtime/render.cljs` lines ~60–160 — `<world-snapshot` m/latest → RAF sample → keyed pool diffs.
6. `src/app/client/workspace/trail_face/scene.cljc` + the header of `trail_face/wiring.cljs` — the worn-face precedent + seam law S1/S2.

## Task — evidence before architecture: build the thin slice, measure, report

1. **The walker probe.** New ns `src/app/client/workspace/face_probe.cljc` + a JVM test ns:
   - a probe registry: plain map `{:stack … :text-run … :block-card …}` → thin builder fns wrapping existing `rect-tree`/`ui-primitives` functions;
   - the two-stage walker: `compile-assembly` (EDN → validated closure) and `apply-assembly` (closure × data-context → rt-node tree). Recursion = plain function recursion; `:each` = `mapv`; `:bind` = `get-in` on the current data context; unknown `:prim` or malformed node → an error-card rt-node, never a throw;
   - one hardcoded five-key assembly (ROAD.md's outline-face example, simplified) + a hardcoded conversation map (turns → blocks).
2. **Measure, JVM-side** (the target fns are cljc; note in the report that JVM numbers are a shape-proxy — browser confirmation rides Step 1):
   - (a) `apply-assembly` cost at ~200 / 500 / 1000 rt-nodes on a data change — ms, median of 100 runs after warmup;
   - (b) `wrap-line` at prose scale: ~500 blocks × 200–2000 chars at realistic widths — total ms + per-block.
3. **Answer by code-read (cite file:line):** the per-pane scroll convention — how sidebar/settings/trail panes scroll today (`:scroll-y` ownership, clipping), and therefore what an assembly-hosted pane must declare.
4. **OPTIONAL, only if trivially cheap:** mount the probe pane beside the existing UI behind a dev-only hook and screenshot it. Dev app: `clj -A:dev -X dev/-main` (NEVER a standalone shadow-cljs compile — CLAUDE.md dev-workflow rule). If the hook needs more than ~10 isolated lines in one existing file, skip and record that instead.

## Fences + hard rules

- **New files only.** Do NOT edit `rect_tree.cljc` / `ui_primitives.cljs` / `electric_flow.cljc` (sole exception: the optional dev hook, ≤10 clearly-marked lines in one file, reported in PROBE.md).
- If rt-node cannot express something the grammar needs → **record it in PROBE.md, do not extend rect_tree** — that decision is contract material.
- **Probe code stays UNCOMMITTED** (it is evidence; Wave 1 harvests or discards it). Docs commit on the local docs branch only; code and docs never mix in one commit; the docs branch is never pushed or merged.
- NEVER read `src/app/server/env.clj`.

## Exit

- `docs/current-mental-model/build/framework/PROBE.md`: the numbers, the scroll-convention answer with citations, surprises, screenshot path if taken, and a ≤5-line "what this changes about the contract" section.
- Append a ≤15-line NOW entry to `build/framework/NOW.md`. Touch nothing else on the board.
