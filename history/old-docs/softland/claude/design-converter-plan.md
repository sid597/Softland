# Design Converter — Plan of Action

> Status: PLAN — 2026-03-01
> Author: Claude (Opus 4.6)
> Scope: Generalized CSS→rt-node converter, MVP quality (full pipeline, not all edge cases)

---

## Goal

Build a generalized system that can take **any** DOM-rendered UI component (shadcn, Material, any website) and convert it into a Softland rt-node spec — pure EDN data, renderable on the WebGPU canvas.

Not a one-off example. A reusable pipeline: Extract → Convert → Save → Render.

---

## Architecture Overview

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│  EXTRACTOR   │ ──→ │  CONVERTER   │ ──→ │    STORE     │ ──→ │   RENDERER   │
│              │     │              │     │              │     │              │
│ Browser JS   │     │ ClojureScript│     │ .edn files   │     │ Existing GPU │
│ (any page)   │     │ (pure fns)   │     │ components/  │     │ pipeline     │
│              │     │              │     │ by-claude/   │     │              │
│ Input: DOM   │     │ Input: JSON  │     │              │     │ tree->rects  │
│ element      │     │ tree w/ CSS  │     │              │     │ tree->text   │
│              │     │              │     │              │     │ resolve-     │
│ Output: JSON │     │ Output: EDN  │     │              │     │ layout       │
│ tree w/      │     │ rt-node spec │     │              │     │              │
│ computed CSS │     │              │     │              │     │              │
└──────────────┘     └──────────────┘     └──────────────┘     └──────────────┘
  Claude-in-Chrome      New code             New dir            Already exists
```

---

## Deliverables

### 1. Extractor: `components/by-claude/_extractor.js` (~40 lines)

A JavaScript function that runs inside any browser page via Claude-in-Chrome's `javascript_tool`. Walks a DOM subtree and extracts:

- Bounding box (position, size) — from `getBoundingClientRect()`
- All visual computed styles — from `getComputedStyle()`
- Text content (leaf text nodes only)
- DOM tree structure (parent→children)

**Design decisions:**
- Extract computed styles, NOT class names or Tailwind utilities. Computed styles are universal — works regardless of what CSS framework was used.
- Bounds are absolute (viewport coords). The converter makes them parent-relative.
- Skips non-visual elements (`<script>`, `<style>`, `<meta>`, etc.)
- Captures the full tree — pruning happens in the converter step, not here.

**Key properties extracted:**
```
backgroundColor, color, fontSize, fontWeight, fontFamily,
borderRadius, borderTopLeftRadius, borderTopRightRadius, borderBottomRightRadius, borderBottomLeftRadius,
borderWidth, borderTopWidth, borderRightWidth, borderBottomWidth, borderLeftWidth,
borderColor, borderTopColor, borderRightColor, borderBottomColor, borderLeftColor,
boxShadow,
padding, paddingTop, paddingRight, paddingBottom, paddingLeft,
gap, flexDirection, alignItems, justifyContent,
opacity, display, overflow,
width, height,
backgroundImage (for gradients)
```

### 2. CSS Parser Functions: `components/by-claude/_css_parsers.cljc` (~100 lines)

Pure functions that convert CSS string values to rt-node equivalents:

| Function | Input | Output |
|----------|-------|--------|
| `css-color->rgba` | `"rgb(23, 23, 25)"`, `"rgba(0,0,0,0.05)"`, `"transparent"`, `"#171719"` | `[0.09 0.09 0.098 1.0]` |
| `css-shadow->shadow` | `"0px 1px 3px 0px rgba(0,0,0,0.1)"` | `{:offset-x 0 :offset-y 1 :blur 3 :spread 0 :color [0 0 0 0.1]}` |
| `css-radius->radius` | `"8px"` or `"8px 4px 0px 0px"` | `8` or `[8 4 0 0]` |
| `css-padding->padding` | `"24px"`, `"16px 24px"`, `"8px 16px 12px 16px"` | `24`, `[16 24]`, `[8 16 12 16]` |
| `css-px->num` | `"14px"`, `"1.5rem"` | `14`, `24` |
| `css-gradient->gradient` | `"linear-gradient(180deg, #1e1e26, #171719)"` | `{:gradient [180 0.5 0 0] :gradient-color2 [r g b a]}` |
| `css-border->border` | computed border styles map | `{:border-width N :border-widths [...] :border-color [...]}` |

**Design decision:** These are `.cljc` (portable). They work in both Clojure (server-side batch conversion) and ClojureScript (client-side live conversion). No dependencies beyond `clojure.string`.

### 3. Tree Converter: `components/by-claude/_converter.cljc` (~60 lines)

The main function `css-tree->rt-node` that walks the extracted JSON tree and produces a nested rt-node:

```clojure
(defn css-tree->rt-node [extracted-node parent-bounds & {:keys [id-prefix]}]
  ;; 1. Compute relative bounds (subtract parent origin)
  ;; 2. Parse all CSS properties via parser fns
  ;; 3. Determine if node is visible (non-transparent bg, has border, has text)
  ;; 4. Build :style map from parsed values
  ;; 5. Build :layout map from flex/padding/gap
  ;; 6. Build :text vec from leaf text content
  ;; 7. Recurse into children
  ;; 8. Return rt-node
  ...)
```

**What it handles (MVP):**
- Background colors (solid)
- Gradients (linear)
- Border radius (uniform and per-corner)
- Borders (uniform and per-side)
- Box shadows (single shadow)
- Padding (uniform and per-side)
- Gap
- Flex direction + alignment
- Text content with color and size
- Opacity (multiplied into alpha)
- Nested children (recursive)

**What it skips (post-MVP):**
- `::before`/`::after` pseudo-elements (not in DOM tree)
- CSS `transform` (rotate, scale)
- CSS `transition`/`animation`
- Multiple box-shadows (takes first only)
- SVG content / icons
- `position: absolute` with complex z-stacking
- `overflow: hidden` → could map to `:clip? true` but needs testing

**Pruning heuristic:** Nodes with transparent background, no border, no shadow, no text, and only one child get collapsed (their child promoted up). This eliminates the wrapper `<div>` noise that DOM components have.

### 4. Component Spec Format

Each converted component saved as an `.edn` file:

```
components/
  by-claude/
    _extractor.js          ← browser-side extraction script
    _css_parsers.cljc      ← CSS string → Clojure value parsers
    _converter.cljc        ← tree walker: extracted JSON → rt-node
    _catalog.edn           ← index of all components (name, source, tags)
    shadcn/
      card.edn             ← converted spec + metadata
      button.edn
      badge.edn
      dialog.edn
      ...
    linear/
      issue-row.edn
      sidebar-item.edn
      ...
    custom/
      ...
```

Each `.edn` file structure:

```clojure
{:meta {:name        "card"
        :source      "shadcn/ui"
        :source-url  "https://ui.shadcn.com/docs/components/card"
        :extracted   "2026-03-01"
        :tags        #{:container :surface :elevated}
        :description "Container with border, radius, optional shadow"}

 :spec  ;; The rt-node tree (pure data, no functions)
 {:id       :card
  :type     :card
  :bounds   {:x 0 :y 0 :w 350 :h 0}  ;; w = reference width, h = auto
  :style    {:bg [0.035 0.035 0.043 1.0]
             :radius 8
             :border-width 1
             :border-color [0.153 0.153 0.165 1.0]
             :shadow {:blur 4 :offset-y 1 :spread 0 :color [0 0 0 0.05]}}
  :layout   {:direction :column :padding 24 :gap 8 :auto-height? true}
  :children [{:id :card-header :type :container
              :bounds {:x 0 :y 0 :w 0 :h 0}
              :layout {:direction :column :gap 6 :auto-height? true}
              :children [{:id :card-title :type :text-node
                          :text [{:text "$title" :size 16 :color [0.98 0.98 0.98 1.0]}]}
                         {:id :card-description :type :text-node
                          :text [{:text "$description" :size 14 :color [0.55 0.55 0.60 1.0]}]}]}
            {:id :card-content :type :container
              :bounds {:x 0 :y 0 :w 0 :h 0}
              :layout {:auto-height? true}
              :children []}  ;; slot — user fills this
            {:id :card-footer :type :container
              :bounds {:x 0 :y 0 :w 0 :h 0}
              :layout {:direction :row :gap 8 :auto-height? true}
              :children []}]}  ;; slot

 :variants {:elevated {:style {:shadow {:blur 16 :offset-y 4 :color [0 0 0 0.25]}}}
            :muted    {:style {:bg [0.14 0.14 0.17 1.0]}}
            :ghost    {:style {:bg [0 0 0 0] :border-width 0}}}

 :slots [:card-content :card-footer]  ;; IDs where children can be injected

 :token-refs  ;; Maps literal values back to design token paths (for theming)
 {:bg          [:colors :bg-subtle]
  :border      [:colors :border]
  :radius      [:radii :lg]
  :shadow      [:shadows :sm]
  :title-color [:colors :fg]
  :desc-color  [:colors :fg-muted]}}
```

### 5. Verification: Visual Comparison

No automated pixel-diff for MVP. The verification loop is:

1. Open source component in browser (Claude-in-Chrome)
2. Screenshot it
3. Render the converted rt-node spec in the Softland canvas
4. Screenshot that
5. I (Claude) compare both screenshots visually — I'm multimodal
6. Report discrepancies, adjust, re-render

This is the same workflow a human designer uses (compare mockup to implementation). It works today with existing tools.

---

## Implementation Order

### Phase 1: Parser Functions (`_css_parsers.cljc`)
Write and test the pure CSS→value parsers. These have no dependencies and can be tested in isolation with literal strings.

### Phase 2: Extractor Script (`_extractor.js`)
Write the browser-side DOM walker. Test by running it on shadcn.com via Claude-in-Chrome.

### Phase 3: Tree Converter (`_converter.cljc`)
Wire parsers into the tree walker. Takes extractor output → produces rt-node.

### Phase 4: First Real Conversion
Pick one shadcn component (Card — simple, well-known). Run the full pipeline: extract from shadcn docs → convert → save as `shadcn/card.edn` → render in canvas → visual comparison.

### Phase 5: Iterate on 3-5 More Components
Button, Badge, Dialog, Alert. Each one will surface edge cases in the parsers/converter. Fix as we go. Save each to the library.

### Phase 6: Catalog Index
Build `_catalog.edn` — index of all saved components with metadata and tags. This is what the AI searches when composing new UIs.

---

## What This Does NOT Cover (Future)

- **Rama storage**: Components live as `.edn` files for now. Migrate to Rama PStates later.
- **Icon/sprite rendering**: Needs the Sprite GPU primitive. Converter will skip SVG/icon content.
- **Second font**: Components with sans-serif text will use Ubuntu Sans Mono until we add a second MSDF atlas.
- **Interaction states**: Hover/active/focus variants captured in `:variants` but not yet wired to the interaction system.
- **Design feels**: The multi-feel system (Linear vs GitHub vs Notion). Comes after we have 20+ components in the library.
- **Live preview grid**: Rendering N component options side-by-side for the user to pick. Comes after the library has enough variety.

---

## File Inventory

```
NEW FILES:
  components/by-claude/_extractor.js       ~40 lines   Browser DOM walker
  components/by-claude/_css_parsers.cljc   ~100 lines  CSS string parsers
  components/by-claude/_converter.cljc     ~60 lines   Tree converter
  components/by-claude/_catalog.edn        ~20 lines   Component index (grows)
  components/by-claude/shadcn/card.edn     ~50 lines   First converted component

NO EXISTING FILES MODIFIED.
The converter is standalone tooling — it produces .edn specs that can be loaded
by the existing rt-node/tree->rects/resolve-layout pipeline. Zero coupling.
```

---

## Success Criteria

1. Run `_extractor.js` on any shadcn component page → get valid JSON tree
2. Feed that JSON into `css-tree->rt-node` → get valid rt-node
3. That rt-node, when rendered via `tree->rects` + `tree->text-ops`, looks visually identical to the original shadcn component (within font/icon limitations)
4. The spec is saved as `.edn` and can be loaded + rendered in any future session
5. The pipeline works for components we haven't seen before — generalized, not hardcoded
