# Design Converter UX Vision

> Saved: 2026-03-01, from conversation between user and Claude
> Status: Vision + active plan. Layer 1 extraction pipeline built. Split view next.

---

## The North Star

> "In future: type 'how does XYZ look' in the command panel → the component renders in the canvas without leaving the app. The canvas IS the browser."

The WebGPU canvas becomes a **rendering target for the entire web's component vocabulary**. Not a browser, but a browser's visual output rendered natively in your engine. You never leave your editor to evaluate a UI component — you describe what you want, it appears on canvas, theme-matched to your design tokens.

---

## The Three Layers

### Layer 1 — Extraction (two modes, both valid)
- `/extract url selector` from your command panel
- "Extract that" via Claude-in-Chrome pointing at a live tab
- Future: "how does Linear's sidebar look" → renders in canvas without leaving app

### Layer 2 — Visual Verification (split view)
- Left: the original component rendered as **live HTML** (not a screenshot — reconstructed from extracted styles in an iframe/div)
- Right: the conversion result (rt-node rendered on your canvas)
- You eyeball the delta
- For component libraries (shadcn, Radix, MUI, etc.) this works because components render in isolation — inject extracted HTML + computed styles into a DOM element, no build step needed
- Figma files are a different beast (proprietary format, needs API) — parked for later

### Layer 3 — Feedback (the hard question)
When the right side doesn't match the left side, how do you tell the system what's wrong?

---

## The Feedback Loop Question

There are really two different problems here:

### Session feedback ("fix this now")

You see the split view. The shadow is too dark. The padding is off. The radius looks wrong.

The simplest version: you just say it in the command panel. "Shadow too dark" or "padding needs to be tighter". Claude (me) can:
- Re-read the compiled rt-node
- Adjust the specific style values
- Re-render the right pane
- Iterate until it matches

This is basically **conversational refinement** — same as tweaking any code, but visually grounded because you can see both panes. The split view is the feedback surface.

### Meta feedback ("remember this forever")

This is harder and more interesting. When you correct a shadow 3 times across 3 different extractions, the system should learn that the shadow parser has a bias. Options:

**Option A: Correction file** — a `components/_corrections.edn` that accumulates manual overrides:
```clojure
{:shadow-blur-offset -1    ;; "our shadows always render 1px too blurry"
 :token-overrides {[0.09 0.09 0.11 1.0] [:colors :bg]}  ;; force this color → this token
 :padding-scale 0.9}       ;; "extracted padding is always 10% too generous"
```
The pipeline reads this file and applies corrections. You build it up over time.

**Option B: Verifier feedback** — Codex's `_verifier.cljc` already has `verify-component` with thresholds (97% pixel similarity, 1px geometry, 5% style delta). Each correction tightens or adjusts specific thresholds. The verifier becomes a learned model of "what counts as good enough."

**Option C: Memory-encoded patterns** — Claude notes patterns in MEMORY.md or CLAUDE.md. "When extracting from shadcn, their `--border` CSS variable maps to our `:border` token." "Tailwind's `rounded-lg` is 8px which matches our `:lg` radius." Future sessions inherit this knowledge.

**Option D: The prompt layer** — Gemini's `_extractor_prompt.md` gets refined. If the LLM extraction path consistently gets shadows wrong, the prompt gets a new rule: "Shadows: always reduce blur by 1px for our rendering engine." The prompt is the meta-learning surface for the LLM path.

### Practical ordering

You probably want **all four**, but in practice:
- **Session feedback** = just talking to Claude in the command panel (works today, no new code)
- **Meta feedback** = the corrections file (Option A) is the simplest first step, because it's just data. The compiler reads it, applies adjustments, done. Over time it becomes a "learned calibration" for your specific rendering engine.

**The split view is the keystone** — without it, you can't give feedback because you can't see the delta. With it, both session and meta feedback become natural.

---

## Implementation Status

### Built (Sessions 27-29, 2026-03-01)
- Extraction pipeline: `_css_parsers`, `_token_matcher`, `_compiler`, `_extractor.js`, `_adapter`
- LLM path: `_design_ir` (Codex), `_verifier` (Codex), `_extractor_prompt.md` (Gemini)
- `/extract` command + split view wiring
- `/hardcode button` — shadcn v4 button rendered with real extracted values

### Direction Change (Session 30, 2026-03-01)
**Pivoted from browser-extraction-first to JIT Component Library.**

Live extraction experiment on shadcn button docs proved accurate but slow (~20min/component).
Key finding: Claude reading source code is faster and more complete than browser extraction.
Browser extraction remains valuable for verification, not as the primary conversion path.

**New architecture: `docs/architecture/component-library-jit.md`**

### Current Plan
1. **Inventory**: Pre-populate 40+ component stubs with source URLs
2. **JIT conversion**: Click stub → fires Claude agent → writes .cljc → renders demo
3. **Interactive refinement**: 3-panel layout (code | chat | rendered) for iterating
4. **Verification**: Browser side-by-side comparison for final QA

### Future
- "How does XYZ look" → canvas render without leaving app (north star unchanged)
- Multi-library support (Linear, MUI, custom) via `_token_maps/`
- Figma extraction (API-based, different pipeline — parked)
