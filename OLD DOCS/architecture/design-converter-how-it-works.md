# Design Converter: How It Works

> See any component on the web. Render it on your canvas. Theme-matched automatically.

---

## What It Does

The design converter takes a **rendered UI component** from any website and recreates it inside Softland's WebGPU canvas, matched to your design tokens.

You point at a card on shadcn's docs. Seconds later, that card appears on your canvas — but using your colors, your spacing, your radii. Not a screenshot. A live, editable scene graph node.

---

## The Key Idea: Visual Extraction, Not Code Copying

Traditional approach: read the source code (React + Tailwind) and reproduce it.

Our approach: read what the browser **already rendered** and capture the visual result.

By the time a browser displays a component, all the framework magic is done. CSS variables are resolved. Tailwind classes are computed. Theme tokens are applied. What remains is concrete visual data: this element is `rgb(9,9,11)` with `8px` border radius and `1px` border.

We capture that layer — the same information a designer would hand-spec — and compile it into our rendering format.

This makes the system **source-agnostic**. It works on React, Vue, Svelte, plain HTML, WordPress, anything with a DOM.

---

## The Pipeline

```
Website (live DOM)
       |
   [ Extract ]     Browser walks the DOM, captures computed styles + bounds
       |
   [ Adapt ]       Converts raw CSS values into a structured visual description
       |
   [ Tokenize ]    Snaps values to your design tokens (rgb(9,9,11) -> :bg)
       |
   [ Compile ]     Generates a scene graph node your GPU can render
       |
Canvas (WebGPU)
```

Each stage is independent. You can inspect the intermediate output at any stage.

---

## Two Ways to Extract

### 1. Command Panel

Type in Softland's command panel:

```
/extract https://ui.shadcn.com/docs/components/card .card
```

This tells the system: go to that URL, find the element matching `.card`, extract it.

The selector is optional — without it, the system auto-detects the main content area (looks for `<main>`, `<article>`, or the largest visible element).

### 2. Claude-in-Chrome (automation)

Claude Code can drive the extraction end-to-end:
1. Open a tab to the target URL
2. Inject the extractor script
3. Run extraction
4. Pass the result back to Softland

No manual steps needed. The injection functions are always available on `window` — not gated behind any command.

---

## Token Matching

This is where "copy" becomes "adapt."

Raw extraction gives you exact CSS values. But your app has its own design language — a set of named tokens for colors, spacing, radii, shadows.

The tokenizer fuzzy-matches extracted values to your tokens:

| Extracted | Token Match | Why |
|-----------|-------------|-----|
| `rgb(9, 9, 11)` | `:bg` `[0.09 0.09 0.11]` | Color distance within threshold |
| `8px` border-radius | `:lg` radius | Exact match |
| `24px` padding | `:xl` spacing | Exact match |
| `rgb(161, 161, 170)` | `:fg-muted` | Closest color token |

The output uses your design system's vocabulary, not the source site's. A shadcn card and a MUI card that look similar will both compile to the same tokens.

---

## Split View Verification

After extraction, a split view shows:
- **Left pane**: the original component (live HTML reconstruction)
- **Right pane**: the compiled result (rendered on canvas)

You eyeball the delta. If the shadow is too dark or the padding is off, you can give feedback to refine it.

---

## What It Can and Cannot Capture

**Captures well:**
- Colors, backgrounds, gradients
- Border radius (per-corner)
- Border width and color
- Box shadows
- Typography (size, weight, color)
- Layout direction, gap, padding
- Element hierarchy (parent-child nesting)

**Cannot capture (static snapshot limitation):**
- Hover, focus, active states (only captures current state)
- Animations and transitions
- Responsive behavior at different breakpoints
- Component interaction logic (click handlers, state)

These are addressed through the feedback mechanism and Softland's own component library, which provides the behavior layer.

---

## Feedback and Learning

When an extraction isn't perfect, two feedback paths exist:

**Session feedback**: Tell the system what's wrong ("shadow too dark", "padding needs to be tighter"). It adjusts and re-renders. Conversational refinement — same loop as tweaking any code, but visually grounded.

**Persistent corrections**: Patterns that come up repeatedly get saved to a corrections file. The pipeline reads this file and applies learned adjustments automatically. Over time, extractions from similar sites improve without manual correction.

---

## Architecture at a Glance

```
components/
  _extractor.js          Browser-side DOM walker (runs in Chrome)

src/components/
  adapter.cljc           Raw CSS -> structured visual description
  css_parsers.cljc       CSS string parsers (colors, lengths, shadows)
  token_matcher.cljc     Fuzzy match to design tokens
  compiler.cljc          Visual description -> scene graph node
  design_tokens.cljc     Shared token definitions (client + server)
```

The server exposes a single endpoint (`/api/extract/compile`) that runs the full adapt-tokenize-compile chain. The client handles extraction (browser-side) and rendering (WebGPU-side).
