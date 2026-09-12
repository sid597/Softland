# Consensus Proposal: Design Converter Pipeline

> Status: CONSENSUS DRAFT — 2026-03-01
> Sources: Claude (design-converter-plan.md), Gemini (ui-ingestion-pipeline.md, extractor-prompt.md), Codex (mvp-generalized-converter-plan.md)
> Goal: Merge the strongest ideas from all three into one encapsulating system

---

## Vision

A generalized pipeline that ingests UI components from any source — React source code, Tailwind HTML, live websites, Figma files — and produces pure-data WebGPU-native component specs. These specs are token-aware (theme-able), state-complete (all interaction variants), composable (slot-based), and verified (metric-based acceptance).

The pipeline grows the component library with every conversion. The library is queryable by an AI to compose new UIs from existing parts.

---

## Architecture: Three Input Paths, One IR, One Output

```
INPUT PATHS                      INTERMEDIATE            OUTPUT
(choose per situation)           (always)                (always)

┌──────────────────┐
│ Path A: LLM      │ Gemini's approach
│ Source Code       │ Read React/Tailwind source
│ Extractor         │ Translate intent → Design IR
│ (non-deterministic│ Best for: components with clear source code
│  but token-aware) │ available (shadcn, Radix, open-source libs)
└────────┬─────────┘
         │
         │         ┌──────────────────┐     ┌──────────────────┐
         ├────────>│                  │     │                  │
         │         │   DESIGN IR      │────>│   RT-NODE SPEC   │
         │         │                  │     │   (.edn file)    │
         ├────────>│  Codex's idea:   │     │                  │
         │         │  canonical       │     │  Token-aware     │
         │         │  intermediate    │     │  State-complete  │
┌────────┴─────────┐  representation │     │  Slot-based      │
│ Path B: Browser  │  that normalizes│     │  Verified        │
│ Computed Styles  │  all inputs     │     │                  │
│ Extractor        │                  │     └────────┬─────────┘
│ (deterministic)  │                  │              │
│ Claude's approach│                  │              ▼
│ Best for: any    └──────────────────┘     ┌──────────────────┐
│ rendered page,                            │   VERIFIER        │
│ closed-source UIs                         │   Codex's idea:   │
└──────────────────┘                        │   automated       │
                                            │   pass/fail       │
┌──────────────────┐                        │   metrics         │
│ Path C: Figma    │                        │                  │
│ API Extractor    │───────────────────────>│   Acceptance     │
│ (future)         │                        │   gate before    │
│ Best for: custom │                        │   library save   │
│ design files     │                        └────────┬─────────┘
└──────────────────┘                                 │
                                                     ▼
                                            ┌──────────────────┐
                                            │   COMPONENT       │
                                            │   LIBRARY         │
                                            │   components/     │
                                            │                  │
                                            │   Queryable by   │
                                            │   AI for future  │
                                            │   composition    │
                                            └──────────────────┘
```

---

## The Design IR (Intermediate Representation)

Codex's key contribution: all input paths converge to a canonical intermediate format before becoming rt-node specs. This is what makes the pipeline truly generalized.

The Design IR is NOT the final rt-node. It's a normalized description that captures:

```clojure
{:tag         "button"            ;; semantic tag from source
 :role        :interactive        ;; :container, :interactive, :decorative, :text
 :bounds      {:w 120 :h 36}     ;; source dimensions (reference, not absolute)
 :visual
   {:fill        {:type :solid :value "rgb(23,23,25)"}   ;; OR {:type :token :ref [:colors :bg]}
    :gradient    {:type :linear :angle 180 :stops [...]}  ;; optional
    :radius      {:uniform 8}                             ;; OR {:corners [8 8 0 0]}
    :border      {:width 1 :color "rgb(56,56,71)"}       ;; OR per-side
    :shadow      {:offset [0 1] :blur 4 :spread 0 :color "rgba(0,0,0,0.1)"}
    :opacity     1.0}
 :typography
   {:content     "Submit"
    :size        14
    :weight      600
    :color       "rgb(255,255,255)"
    :family      "Inter"}          ;; used for font-atlas selection
 :layout
   {:display     :flex
    :direction   :row
    :gap         8
    :padding     [8 16 8 16]
    :align-items :center
    :justify     :center}
 :states                           ;; Codex's contribution: first-class states
   {:hover   {:fill {:value "rgb(30,30,38)"}}
    :active  {:fill {:value "rgb(20,20,25)"}}
    :focus   {:border {:color "rgb(112,138,255)" :width 2}}
    :disabled {:opacity 0.5}}
 :slots                            ;; Gemini's contribution: composition points
   {:leading  {:role :decorative :description "icon or avatar"}
    :trailing {:role :decorative :description "icon or badge"}}
 :children  [...]                  ;; nested Design IR nodes (recursive)
 :source-meta
   {:library  "shadcn"
    :component "button"
    :variant   "default"
    :url       "https://ui.shadcn.com/docs/components/button"}}
```

### Why a separate IR (not straight to rt-node)?

1. **Normalization point.** Path A (LLM) gives you token references. Path B (browser) gives you literal CSS strings. Path C (Figma) gives you Figma JSON. The IR is where `"rgb(23,23,25)"` and `(:bg (:colors dt))` and `{"r":0.09,"g":0.09,"b":0.098}` all become the same thing.

2. **Validation checkpoint.** You can validate the IR schema BEFORE converting to rt-node. Catch errors early.

3. **Token resolution happens here.** The IR stores both the literal value AND the closest design token match. The rt-node compiler chooses which to use based on context (literal for 1:1 reproduction, token for theme-able version).

4. **State variants live cleanly here.** CSS pseudo-classes (`:hover`, `:focus`) are captured at IR level. The rt-node compiler decides how to represent them (`:variants` map, separate nodes, etc.).

---

## Input Path A: LLM Source Code Extractor (Gemini's approach)

### When to use
You have the component's source code (React JSX, Tailwind HTML, Vue template). Open-source component libraries (shadcn, Radix, Headless UI, daisyUI).

### How it works
The LLM reads source code and outputs Design IR directly, guided by the Extractor Prompt.

### The Extractor Prompt (Gemini's contribution, adapted for Design IR)
Gemini already wrote a detailed system prompt (`extractor-prompt.md`) that teaches the LLM:
- The rt-node primitive and its properties
- Design token vocabulary (`dt` colors, radii, shadows)
- Layout engine rules (`:direction`, `:gap`, `:padding`, `:align`)
- Text rendering rules (`:text` vector, not children)
- "Collapse the DOM" — extract intent, not div-for-div
- Slot-based composition (`[id bounds slots props]`)

**Adaptation for consensus:** The prompt should output Design IR (not rt-node directly). The IR is then compiled to rt-node by the deterministic compiler. This makes the LLM's output validatable against the IR schema.

### Strengths
- Token-native: output already references `dt` tokens, immediately theme-able
- Intent-aware: understands "this is a button" not "this is a div with these 14 CSS properties"
- Can handle source code that isn't rendered anywhere (design system docs, Storybook stories)

### Weakness
- Non-deterministic: same input may produce different output across runs
- Mitigated by: verification step (Codex's contribution)

---

## Input Path B: Browser Computed Styles Extractor (Claude's approach)

### When to use
The component is rendered on a live web page. Closed-source UIs, proprietary design systems, or any website you want to extract from.

### How it works
1. JavaScript extraction script runs in the browser (via Claude-in-Chrome `javascript_tool`)
2. Walks the DOM subtree of the target element
3. Captures `getComputedStyle()` + `getBoundingClientRect()` for every node
4. Returns JSON tree
5. Deterministic CSS parser functions convert JSON → Design IR

### The Extractor Script (`_extractor.js`)
Claude's contribution: a universal DOM walker that works on any page.

### CSS Parser Functions (`_css_parsers.cljc`)
Claude's contribution: deterministic pure functions for CSS value → Clojure value conversion.

```
css-color->rgba     "rgb(23,23,25)"              → [0.09 0.09 0.098 1.0]
css-shadow->shadow  "0px 1px 3px rgba(0,0,0,.1)" → {:offset-x 0 :offset-y 1 ...}
css-radius->radius  "8px 8px 0 0"                → [8 8 0 0]
css-padding->pad    "16px 24px"                   → [16 24 16 24]
css-px->num         "14px"                        → 14
```

### Token Matcher
After parsing literal values, a matcher finds the closest design token:
```clojure
(defn match-token [rgba-value]
  ;; Find the dt token whose value is closest (by Euclidean distance in RGBA space)
  ;; Returns {:ref [:colors :bg-subtle] :distance 0.02} or nil if no close match
  ...)
```
This bridges Claude's literal-value extraction with Gemini's token-native approach. The Design IR stores BOTH:
```clojure
{:fill {:type :solid
        :value [0.11 0.11 0.13 1.0]        ;; literal (Claude)
        :token-ref [:colors :bg-subtle]     ;; matched token (Gemini-compatible)
        :token-distance 0.008}}             ;; confidence (Codex-style metric)
```

### Strengths
- Works on ANY rendered page — not source-code dependent
- Deterministic: same page → same output, always
- Gets actual rendered values (after CSS cascade, media queries, JS modifications)

### Weakness
- Can't extract CSS pseudo-class states (`:hover`, `:focus`) from computed styles alone
- Mitigated by: capture component in each state separately (hover it, extract again)

---

## Input Path C: Figma API Extractor (Future)

### When to use
A designer provides a Figma file with custom components.

### How it works
Figma REST API returns a node tree with properties that map almost 1:1 to Design IR:
- `absoluteBoundingBox` → bounds
- `fills` → fill
- `cornerRadius`/`rectangleCornerRadii` → radius
- `strokes` + `strokeWeight` → border
- `effects` (DROP_SHADOW) → shadow
- `children` → recursive

### Not built for MVP
This path is structurally identical to Path B but with a different input format. Once Path B works, Path C is a second adapter on the same IR.

---

## The Design IR → rt-node Compiler (Deterministic)

A pure function that compiles Design IR to rt-node spec. This is the ONLY place where rt-node structures are produced. All input paths go through here.

```clojure
(defn compile-ir->rt-node [ir-node & {:keys [use-tokens?] :or {use-tokens? true}}]
  ;; 1. Resolve fill → :bg (use token if available and use-tokens? is true)
  ;; 2. Resolve radius → :radius or :corner-radii
  ;; 3. Resolve border → :border-width(s) + :border-color
  ;; 4. Resolve shadow → :shadow map
  ;; 5. Resolve layout → :layout map
  ;; 6. Resolve typography → :text vector
  ;; 7. Resolve states → :variants map (Codex's first-class states)
  ;; 8. Resolve slots → :children with :slot markers (Gemini's composition)
  ;; 9. Recurse into children
  ;; 10. Return rt-node
  ...)
```

**`:use-tokens?` flag:** When true, the compiler uses matched design tokens (theme-able output). When false, it uses literal values (pixel-perfect reproduction). This lets the same IR produce both "exact copy" and "our-design-language version" of a component.

---

## The Verifier (Codex's contribution)

Every conversion must pass automated verification before being saved to the library. The verifier is deterministic and metric-based.

### Verification Dimensions

1. **Geometry diff**
   - Compare bounds (x, y, w, h) of each node in source vs converted
   - Tolerance: ±2px per dimension
   - Metric: max deviation across all nodes

2. **Style diff**
   - Compare fill color, radius, border, shadow between source and converted
   - Tolerance: ±0.02 per RGBA channel (roughly ±5/255)
   - Metric: max color distance (Euclidean in RGBA)

3. **Structure diff**
   - Compare tree depth and child count at each level
   - Source may have more nodes (wrapper divs) — converted should have ≤ source node count
   - Metric: structural similarity ratio

4. **State parity** (Codex's innovation)
   - For each declared state (hover, active, focus, disabled):
     - Does the converted spec have a corresponding variant?
     - Do the variant's style overrides match the source state's computed styles?
   - Metric: states covered / states expected

5. **Visual similarity** (post-MVP, requires rendering harness)
   - Render both (source in browser, converted in WebGPU canvas)
   - Screenshot comparison
   - Pixel-level similarity percentage
   - MVP substitute: Claude's multimodal comparison (qualitative, not metric)

### Verification Report Format

```clojure
{:component    "shadcn/button"
 :timestamp    "2026-03-01T14:30:00Z"
 :verdict      :pass    ;; or :fail or :warn
 :metrics
   {:geometry   {:max-deviation-px 1.2  :threshold 2.0  :pass? true}
    :style      {:max-color-dist   0.01 :threshold 0.02 :pass? true}
    :structure  {:similarity-ratio 0.85 :threshold 0.7  :pass? true}
    :states     {:covered 4 :expected 5 :ratio 0.8 :threshold 0.6 :pass? true}}
 :notes ["focus ring state not extracted — pseudo-class limitation"]
 :source-path  "path/to/extraction/input"
 :extractor    :browser  ;; or :llm or :figma
}
```

### Acceptance Gate

All four metrics must pass their thresholds. If any fails:
- For Path A (LLM): re-prompt with the verification report as feedback
- For Path B (Browser): flag for manual review (likely a parser limitation)
- For Path C (Figma): flag for manual review (likely a mapping gap)

---

## Component Library Structure

Unified storage — one `components/` directory regardless of which LLM or method produced the component:

```
components/
  _extractor.js              ← Browser DOM walker script (Path B)
  _css_parsers.cljc          ← CSS string → value parsers (Path B)
  _design_ir.cljc            ← Design IR schema + validation
  _compiler.cljc             ← Design IR → rt-node compiler
  _verifier.cljc             ← Automated verification
  _token_matcher.cljc        ← Literal value → dt token matching
  _catalog.edn               ← Index of all components

  shadcn/
    button.edn               ← Component spec + metadata + verification report
    card.edn
    badge.edn
    input.edn
    dialog.edn
    sidebar.edn
    ...

  linear/
    issue-row.edn
    sidebar-item.edn
    ...

  github/
    issue-row.edn
    pr-card.edn
    ...

  custom/
    ...
```

### Component File Format (`.edn`)

```clojure
{;; ── Metadata ──
 :meta
   {:name         "button"
    :source       "shadcn/ui"
    :source-url   "https://ui.shadcn.com/docs/components/button"
    :extracted    "2026-03-01"
    :extractor    :browser       ;; :llm | :browser | :figma
    :extracted-by "claude"       ;; which LLM session produced this
    :tags         #{:interactive :control :primary}
    :description  "Primary action button with multiple variants"}

 ;; ── Design IR (preserved for re-compilation) ──
 :design-ir { ... }

 ;; ── Compiled rt-node spec (pure data, what actually renders) ──
 :spec
   {:id :button :type :button
    :bounds {:x 0 :y 0 :w 120 :h 36}
    :style {:bg [0.035 0.035 0.043 1.0] :radius 6}
    :layout {:direction :row :padding [8 16] :align :center}
    :text [{:text "$label" :size 14 :color [0.98 0.98 0.98 1.0]}]
    :children []}

 ;; ── Variants (Codex's state system) ──
 :variants
   {:default    {}   ;; base spec IS the default
    :secondary  {:style {:bg [0.15 0.15 0.17 1.0]}}
    :outline    {:style {:bg [0 0 0 0] :border-width 1 :border-color [0.22 0.22 0.28 1.0]}}
    :ghost      {:style {:bg [0 0 0 0]}}
    :destructive {:style {:bg [0.5 0.09 0.09 1.0]}}}

 ;; ── Interaction states (Codex's innovation) ──
 :states
   {:hover    {:style {:bg [0.045 0.045 0.055 1.0]}}
    :active   {:style {:bg [0.025 0.025 0.035 1.0]}}
    :focus    {:style {:border-width 2 :border-color [0.44 0.54 1.0 1.0]}}
    :disabled {:style {:bg [0.035 0.035 0.043 0.5]}}}

 ;; ── Slots (Gemini's composition system) ──
 :slots
   {:leading  {:role :decorative :bounds {:w 16 :h 16}}
    :trailing {:role :decorative :bounds {:w 16 :h 16}}}

 ;; ── Token mappings (for theming) ──
 :token-map
   {:bg     [:colors :bg-subtle]
    :radius [:radii :md]
    :fg     [:colors :fg]
    :accent [:colors :accent]}

 ;; ── Verification report (Codex's accountability) ──
 :verification
   {:verdict :pass
    :metrics {:geometry {:max-deviation-px 0.8}
              :style {:max-color-dist 0.005}
              :structure {:similarity-ratio 0.92}
              :states {:covered 4 :expected 5}}
    :notes ["focus state approximated — pseudo-class extraction limitation"]}}
```

---

## Dynamic Loading: SCI Hydration (Gemini's future vision)

Post-MVP: components stored as EDN can be loaded and evaluated at runtime via SCI (Small Clojure Interpreter) on the client. This means:

1. New component added to library → instantly available to all running clients
2. No recompilation, no deploy, no page refresh
3. The component library becomes a live, growing system

This is the bridge to the Rama storage future: components in a PState, pushed to clients via subscriptions, evaluated via SCI, rendered via WebGPU. Hot-reloadable design system.

---

## Implementation Phases

### Phase 1: Foundation (CSS Parsers + Design IR Schema)
- Write CSS parser functions (`_css_parsers.cljc`)
- Define Design IR schema (`_design_ir.cljc`)
- Write IR validation function
- Write IR → rt-node compiler (`_compiler.cljc`)

### Phase 2: Browser Extraction Path (Path B)
- Write DOM extractor script (`_extractor.js`)
- Write extracted JSON → Design IR converter (uses CSS parsers)
- Write token matcher (`_token_matcher.cljc`)
- End-to-end test: extract shadcn Card from live page → Design IR → rt-node → render

### Phase 3: LLM Extraction Path (Path A)
- Adapt Gemini's extractor prompt to output Design IR (not rt-node directly)
- Test with shadcn Button source code → Design IR → rt-node → render
- Compare Path A output vs Path B output for same component (should be equivalent)

### Phase 4: Verifier
- Implement geometry, style, structure, and state parity checks (`_verifier.cljc`)
- Define pass/fail thresholds
- Run verification on all existing conversions
- Store verification reports in component `.edn` files

### Phase 5: Library Growth
- Convert 10-15 core components (Button, Card, Badge, Input, Dialog, Alert, Tabs, Sidebar, List Item, Progress, Tooltip, Checkbox, Select, Table Row, Avatar)
- Build `_catalog.edn` with search tags
- Validate that the AI can query the catalog and compose new UIs from existing specs

### Phase 6: Design Feels (the bigger vision)
- Group components by source library (shadcn, Linear, GitHub, etc.)
- Each group = a "design feel"
- AI can suggest: "for your GitHub issues, here are 3 sidebar designs: shadcn-style, Linear-style, GitHub-native"
- Swap feels by swapping which component set is active

---

## What Each Session Should Do Next

### Claude (this session)
- Owns Phase 1 + Phase 2 (CSS parsers, Design IR schema, browser extraction, IR compiler)
- These are deterministic code — pure functions, testable, no LLM dependency
- Foundation that all paths build on

### Gemini
- Owns Phase 3 (LLM extraction path)
- Adapt the existing extractor prompt to target Design IR instead of rt-node directly
- The prompt is already 80% there — needs IR schema reference + output format change
- Test against same components Claude extracts via browser (cross-validation)

### Codex
- Owns Phase 4 (Verifier)
- Define the verification schema, thresholds, and report format
- Build the comparison functions (geometry diff, style diff, structure diff, state parity)
- This is Codex's natural strength: deterministic, metric-based, rigorous

### All three converge at Phase 5
- Each session contributes converted components to the shared library
- Cross-validate: same component extracted by all three methods should produce equivalent rt-node specs
- The library grows fastest when all three contribute simultaneously

---

## Key Design Decisions (Consensus)

1. **Design IR is mandatory.** All input paths produce Design IR first. No direct-to-rt-node. (Codex's architecture, validated by the normalization need)

2. **Token-aware by default.** Design IR stores both literal values AND matched design tokens. The compiler flag `:use-tokens?` controls which is used. (Gemini's insight + Claude's reverse-mapping)

3. **States are first-class.** Every component spec includes interaction states (hover, active, focus, disabled) as structured data, not afterthoughts. (Codex's contribution)

4. **Slots for composition.** Components declare named insertion points where children can be injected. (Gemini's contribution)

5. **Verification before acceptance.** No component enters the library without passing automated verification. (Codex's contribution)

6. **Unified library, attributed sources.** One `components/` directory. Each file's `:meta` records which extractor and which LLM produced it. No separate `by-claude/`, `by-gemini/`, `by-codex/` directories. (Consensus: the library is the product, not any one LLM's output)

7. **EDN data, not code.** Component specs are pure data (`.edn`), not Clojure functions (`.cljs`). This enables Rama storage, SCI hydration, and cross-LLM portability. Gemini's `.cljs` approach is the right ergonomic for authoring but wrong format for storage — the compiler bridges this gap.

8. **The extractor prompt is a shared asset.** Gemini's prompt works for any LLM, not just Gemini. It should live in `components/_extractor_prompt.md` and be maintained as a team resource.
