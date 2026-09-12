# Session 27: Design Converter Pipeline — 2026-03-01

## Goal
Build Phase 1+2 of the Design Converter: a generalized system that extracts UI components from any rendered web page and converts them into rt-node specs for the WebGPU canvas.

## Multi-LLM Collaboration
Three LLMs worked in parallel on complementary pieces:

| Contributor | Files | Role |
|-------------|-------|------|
| **Codex** | `_design_ir.cljc`, `_verifier.cljc`, `by-codex/blueprints/`, `by-codex/schemas/`, `by-codex/tests/` | Schema layer — validation, ground truth blueprints, verification thresholds |
| **Claude** | `_css_parsers.cljc`, `_token_matcher.cljc`, `_compiler.cljc`, `_extractor.js` | Transformation layer — parsing, matching, compiling between formats |
| **Gemini** | `_extractor_prompt.md` | LLM extraction layer — prompt for Design IR output (data, not code) |

## Two Extraction Paths

### Path 1: Deterministic (Claude)
```
Browser DOM → _extractor.js (getComputedStyle) → JSON
  → _css_parsers (parse CSS strings) → Clojure maps
  → _token_matcher (fuzzy match → dt tokens) → tokenized IR
  → _compiler (IR → rt-node) → rt-node tree
  → resolve-layout → tree->rects / tree->text-ops → GPU
```
- Fast, exact pixel values, no LLM involved
- Cannot capture states (hover/disabled) or semantic intent

### Path 2: LLM-Assisted (Gemini)
```
Component source code → LLM (via _extractor_prompt.md) → Design IR
  → _compiler → rt-node → GPU
```
- Captures states, slots, token refs, semantic structure
- Slower, requires LLM call

Both paths converge at Design IR format, validated by `_design_ir.cljc`.

## Files Created

| File | Lines | Description |
|------|-------|-------------|
| `_css_parsers.cljc` | 436 | 12 parsers: color (hex/rgb/rgba/hsl/hsla/named/CSS4), px, shadow, radius, padding, border, gradient, direction, align, display, font-weight |
| `_token_matcher.cljc` | 228 | Fuzzy matching: color-distance (Euclidean, alpha 2x), match-color/radius/shadow/font-size/spacing, recursive tokenize-ir |
| `_compiler.cljc` | 352 | compile-ir (IR→rt-node), compile-component (blueprint→per-state rt-nodes), dual-input detection |
| `_extractor.js` | 115 | extractComponent(selector): DOM walk, 31 CSS properties, wrapper collapsing, relative bounds |

## Key Design Decisions

1. **Shadow format bridge**: IR `{:offset [x y]}` → rt-node `{:offset-x N :offset-y N}` (different conventions)
2. **Dual-input compiler**: `is-rt-node?` detects format — blueprints pass through, IR gets compiled
3. **Token matching thresholds**: color 0.08, spacing/radius 2px, font-size 1px, shadow composite 5.0
4. **All colors normalize to `[r g b a]` with 0-1 floats** — consistent across all parsers
5. **Wrapper collapsing in extractor**: transparent bg + no border + no shadow + single child + div/span → promote child

## Bug Found and Fixed
- `match-shadow` was missing X-offset comparison — only compared Y offset and blur
- Fixed: now compares both `s-ox`/`t-ox` and `s-oy`/`t-oy` in distance calculation

## No Existing Files Modified
Entire pipeline is additive — 4 new files in `components/`, zero changes to loop.cljs, editor.cljs, or electric_flow.cljc.

## Session 28 Additions (same day)

### Adapter + Split View Wiring
- **`_adapter.cljc`** (new, 155 lines): `extracted->ir` converts extractor JSON → Design IR, `extract->rt-node` full pipeline convenience fn
- **`/api/extract/compile`** endpoint in `server_jetty.clj`: POST extracted JSON → runs adapter→tokenizer→compiler → returns `{:ok :ir :rt-node}`
- **`deps.edn`**: added `"components"` to `:paths` so server can `require` pipeline namespaces
- **Split view DOM overlay**: `div#extract-preview-overlay` in `electric_flow.cljc` (fixed position, left 50%, hidden default)
- **`!extract-preview` atom** in loop.cljs: watched by `<editor-rects` and `<combined-text-ops`, renders rt-node on right half of canvas
- **`start-loop!`** accepts `:!preview-el` kwarg, passed from electric_flow
- **`/extract` command** parsed in `parse-command`, handler sets up `window.__softland_inject_preview()` and `window.__softland_inject_rt_node()`
- **Extractor.js** updated: captures `sourceHTML` for left-pane DOM preview

### UX Vision Doc
- Created `docs/vision/design-converter-ux.md` — north star, 3 layers, feedback loop options A-D
- Key quote saved: "The canvas IS the browser"
- Left pane = live HTML (DOM), right pane = compiled rt-node (WebGPU) — two renderers side by side

### Known Issues / TODOs
- `/extract` command takes CSS selector but should take URL: `/extract https://ui.shadcn.com/docs/components/button`
- dt (design tokens) duplicated in server endpoint — needs single source of truth
- End-to-end test not yet run
- Feedback mechanism design (`_corrections.edn`) not started
- `_css_parsers.cljc` has private `safe-parse-double` — adapter uses `parse-px` for opacity as workaround
