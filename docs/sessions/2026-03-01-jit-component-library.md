# Session 30: From Extraction Pipeline to JIT Component Library

> Date: 2026-03-01
> Duration: ~2 hours
> Key outcome: Abandoned browser-extraction-first approach. Arrived at JIT component library.

---

## Where We Started

### The Problem Brief (from next-prompt.md)

> How do we go from "I like this component at this URL" to a verified, reusable, native Softland component artifact, without manually rebuilding each one?

### The Existing Pipeline (Sessions 27-29)

Three LLMs had built a deterministic extraction pipeline:
- `_extractor.js` — DOM walker, captures getComputedStyle
- `_css_parsers.cljc` — 12 CSS parsers (hex, rgb, hsl, shadow, etc.)
- `_token_matcher.cljc` — fuzzy match extracted values to dt tokens
- `_compiler.cljc` — Design IR → rt-node
- `_adapter.cljc` — extractor JSON → Design IR
- `_extractor_prompt.md` — LLM extraction prompt for semantic understanding
- `_design_ir.cljc` / `_verifier.cljc` — schema validation and verification

Two paths: deterministic (getComputedStyle → parsers → compiler) and LLM-assisted (source code → prompt → Design IR).

### The Assumption

The assumption was: **extract from the browser, convert deterministically, verify visually.**

---

## The First-Principles Analysis

Before touching code, we decomposed the problem:

### 1. Two Problems Conflated

**Problem A** ("make this look right") — a translation problem. Pixel similarity.
**Problem B** ("give me a reusable component") — a compilation problem. Composability.

The pipeline conflated these. Design IR tries to be both a visual snapshot AND a component spec. It's awkward for both.

### 2. Browser-as-Oracle

Key insight: **don't reverse-engineer CSS — the browser already resolved it.**

Instead of parsing `:hover` rules from source code, trigger the hover state in the browser and extract getComputedStyle again. Diff the two captures → that's the hover rule.

### 3. Component Schema > Design IR

A reusable component needs to describe what something IS (archetype, variants, sizes, states, slots), not just what it looks like. Proposed a Component Schema format — a specification, not a rendering.

### 4. Functions as Artifacts

The output should be a parameterized Clojure function (`ui-button`), not a data tree. Data trees are instances; functions are components.

### 5. State-Diff Rules

Instead of absolute values per state, store rules: `[:alpha 0.8]` or `[:swap {:bg :muted}]`. More portable, captures design intent.

---

## The Live Extraction Experiment

### What We Did

Navigated to `https://ui.shadcn.com/docs/components/radix/button` via Claude-in-Chrome and ran a full extraction:

1. **Discovery** — scrolled page, identified 12 demo sections (Size, Default, Outline, Secondary, Ghost, Destructive, Link, Icon, With Icon, Rounded, Spinner, Button Group)

2. **Programmatic button finding** — JS script to locate all demo buttons, filter out code-block/nav buttons, group by section heading

3. **Default state extraction** — getComputedStyle + canvas `getImageData` for RGBA conversion. Captured all 5 core variants.

4. **Hover state probing** — attempted three approaches:
   - Physical cursor hover via `computer` tool → `:hover` didn't stick between tool calls (`isHovered: false`)
   - CSS stylesheet search → ZERO `:hover` rules found in entire page (!)
   - CSS nesting search → zero nested hover rules

5. **The breakthrough** — read Tailwind class names directly from `className`. Found hover classes:
   ```
   Default:     hover:bg-primary/80
   Outline:     hover:bg-muted hover:text-foreground
   Secondary:   hover:bg-secondary/80
   Ghost:       hover:bg-muted hover:text-foreground
   Destructive: hover:bg-destructive/30
   Link:        hover:underline
   ```

6. **Token extraction** — read CSS custom properties (`--color-primary`, etc.) from `getComputedStyle(documentElement)`. Converted from `lab()` to RGBA via canvas.

7. **Data attributes** — discovered `data-variant="default"` and `data-size="default"` attributes (shadcn v4's approach vs className variants).

### Key Findings

- **Tailwind v4 uses `lab()`/`oklab()` colors** — our CSS parsers only handle hex/rgb/rgba/hsl. Pipeline would silently fail.
- **Zero `:hover` rules in CSSOM** — Tailwind v4 compiles hover states differently (not visible via `document.styleSheets`)
- **Tailwind class names ARE the specification** — `hover:bg-primary/80` literally encodes the rule (same color, 80% alpha)
- **All variants share**: radius 10px, font 14px/500, padding 0 10px, border-width 0.9px, height 32px
- **Two hover strategies**: alpha reduction (default, secondary) and token swap (outline, ghost)

### The Hardcoded Demo

Used extraction data to write a `/hardcode button` command that renders all 6 variants + hover states + 4 sizes on the WebGPU canvas. One-shot accuracy — colors matched the live page exactly.

**Total time: ~20 minutes for one component.**

---

## The Pivot

### The Realization

> The browser extraction took 20 minutes for one component. But the actual conversion (writing rt-node code from known values) took seconds. The bottleneck was interrogation, not conversion.

And: Claude can read a `.tsx` source file and understand the component just as well as — better than — extracting from a live page. The source code has ALL variants, ALL states, ALL slots. The live page only shows what the demo chooses to show.

### The User's Insight

> "Maybe the most realistic solution is I ask you to browse the codebase and pull the components, make them a list. For each one create a file. First pass: just the link. Second pass: extract and convert."

Then refined:

> "When I click on it and it's not present, we run the session to make it done and while it's working we show the status."

And further:

> "The space should exist even if not populated. No one lives on Mars but the land is there."

And the UX detail:

> "If the file is empty, the left pane shows a Convert button (runtime overlay, not in the file). When clicked, we see the conversion stream. When done, we see the code."

### What This Means

- **Day 1**: 40+ component directories exist with source URLs. Zero converted.
- **First click**: triggers JIT conversion. You watch it work.
- **After first click**: file is permanent. Instant load.
- **Library grows organically** as you use it.

---

## The Final Architecture

### Directory Structure
```
components/
├── _registry.edn
├── _token_maps/
│   └── shadcn-v4.edn
├── button/
│   ├── _source.edn       {shadcn: {:status :ready ...}}
│   └── shadcn.cljc        schema + render + demo
├── card/
│   ├── _source.edn       {shadcn: {:status :stub ...}}
│   └── (shadcn.cljc)      doesn't exist yet
├── ...40+ more
```

### Left Pane States
```
EMPTY → [Convert button] + source info        (runtime overlay)
WORKING → agent stream output                  (stream overlay)
READY → .cljc source code                      (actual file)
```

### Conversion Flow
```
Click stub → Convert button appears → Click Convert → Agent fires
  → Stream in left pane → File written → Code loads → Demo renders
```

### What Exists vs What's New

| EXISTS | NEW |
|--------|-----|
| Sidebar file explorer | Component inventory stubs |
| Split view rendering | Convert button overlay |
| Agent runner + streaming | Agent stream in left pane |
| rt-node + tree->rects pipeline | Conversion prompt template |
| `/hardcode` proof of concept | Token map as agent context |
| Design token system (dt) | _registry.edn + _source.edn |

---

## Artifacts Produced This Session

| File | What |
|------|------|
| `docs/architecture/component-library-jit.md` | The architecture doc (canonical reference) |
| `docs/sessions/2026-03-01-jit-component-library.md` | This session log |
| `loop.cljs` `/hardcode button` handler | Updated with real shadcn v4 extracted values |

## Next Session: Plan & Implement

See `docs/sessions/next-prompt.md` for the implementation-ready prompt.
