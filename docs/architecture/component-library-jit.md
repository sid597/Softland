# JIT Component Library — Architecture

> Status: APPROVED SOLUTION — arrived at 2026-03-01, session 30
> Implements: the "Design Converter" vision from `vision/design-converter-ux.md`
> Supersedes: the browser-extraction-first pipeline approach (sessions 27-29)

---

## The Problem (one sentence)

How do we go from "I like this component at this URL" to a verified, reusable, native Softland component artifact, without manually rebuilding each one?

## The Solution (one sentence)

A pre-populated component inventory with JIT conversion — stubs exist for every component on day one, conversion happens on first click, and the result is a permanent `.cljc` file that renders as rt-nodes on the WebGPU canvas.

---

## Core Metaphor

> "No one lives on Mars but the land is there."

The scaffolding exists for 40+ components across multiple libraries from day one. Every component has a directory and a source URL. But the actual conversion only happens when you need it — JIT. Once converted, it's cached as a real file forever.

---

## Architecture Overview

```
components/
├── _registry.edn              Master inventory (all known components)
├── _token_maps/
│   ├── shadcn-v4.edn          Color/spacing/radius token map
│   └── linear.edn             (future)
├── button/
│   ├── _source.edn            Provenance: URLs, status per library
│   ├── shadcn.cljc            Converted component (when :ready)
│   └── linear.cljc            (future)
├── card/
│   ├── _source.edn
│   └── shadcn.cljc
├── badge/ ...
├── dialog/ ...
├── input/ ...
└── ...40+ component directories
```

## Three States Per Component

```
  ·  STUB       Source URL exists. File is empty. Click to convert.
  ⟳  WORKING    Conversion agent running. Stream visible in left pane.
  ✓  READY      .cljc file written. Code + rendered demo available.
```

Tracked in `_source.edn`:
```clojure
{:shadcn {:status :stub          ;; :stub | :working | :ready
          :source "https://github.com/shadcn-ui/ui/blob/.../button.tsx"
          :docs   "https://ui.shadcn.com/docs/components/button"
          :converted nil}}       ;; ISO date when :ready
```

---

## UX: Four-Panel Layout (Sidebar + 3 Content Panes)

The conversion is NOT one-shot — it's a conversation. You fire the initial conversion,
see the result, say "destructive is too bright" or "add a disabled state," and iterate.

```
┌──────────┬──────────────────┬──────────────────┬──────────────────┐
│ SIDEBAR  │   CODE FILE      │  CHAT SESSION    │  RENDERED DEMO   │
│          │                  │                  │                  │
│ button/  │ (ns components.  │ > Converting...  │ ┌──────────┐    │
│  shadcn✓ │   button.shadcn) │                  │ │ Default  │    │
│  linear· │                  │ Found 6 variants │ └──────────┘    │
│ card/    │ (def schema      │ Writing schema.. │ ┌──────────┐    │
│  shadcn⟳ │   {:variants     │                  │ │Secondary │    │
│ badge/   │    {:default ... │ Done! Check the  │ └──────────┘    │
│  shadcn· │     :outline ... │ preview ──────►  │ ┌────────────┐  │
│ dialog/  │     ...}})       │                  │ │Destructive │  │
│  shadcn· │                  │ You: "destructive│ └────────────┘  │
│ input/   │ (defn render ... │  is too bright"  │     ...         │
│  shadcn· │                  │                  │                  │
│          │ (defn demo ...)  │ Agent: Adjusting │  (re-renders    │
│          │                  │ alpha 0.2 → 0.15 │   live as chat  │
│          │  (file updates   │ Updated file.    │   session       │
│          │   in real-time)  │                  │   refines)      │
└──────────┴──────────────────┴──────────────────┴──────────────────┘
```

**Sidebar** — component tree with status indicators (·/⟳/✓)
**Code file** — the `.cljc` source, updates live as the agent writes/modifies
**Chat session** — a DEDICATED Claude session for THIS component (not the main cmd panel)
**Rendered demo** — the `(demo)` function rendered as rt-nodes, re-evaluates on code change

The chat session is scoped to one component. You can have a back-and-forth conversation
focused entirely on getting this component right. When satisfied, the session ends and
the file is permanent.

### Left Pane (Code File): Three States

**State 1 — STUB (file empty, not yet converted):**
```
┌───────────────────────────────┐
│ button/shadcn.cljc            │
│                               │
│  ┌─────────────────────────┐  │
│  │  >> Convert              │  │  ← runtime-rendered button
│  └─────────────────────────┘  │    (NOT in the file — metadata overlay)
│                               │
│  Source: shadcn v4            │
│  github.com/shadcn-ui/ui/... │  ← source URL from _source.edn
│  Docs: ui.shadcn.com/...     │
│                               │
└───────────────────────────────┘
```

The "Convert" button is a **runtime UI overlay** — not in the file. When file is empty/stub, it appears automatically. Clicking it fires the conversion agent.

**State 2 — WORKING (agent active, chat open):**
```
┌───────────────────────────────┐
│ button/shadcn.cljc  ⟳         │
│                               │
│ ;; file being written...      │
│ (ns components.               │  ← file updates progressively
│   button.shadcn)              │    as agent writes to it
│                               │
│ (def schema                   │
│   {:variants                  │
│    {:default {...}             │
│     ...building...            │
│                               │
└───────────────────────────────┘
```

Code pane shows the file as the agent writes it. Chat pane (middle) shows the conversation. Render pane (right) shows the demo as soon as enough code exists.

**State 3 — READY (chat ended or idle):**
```
┌───────────────────────────────┐
│ button/shadcn.cljc  ✓         │
│                               │
│ (ns components.               │
│   button.shadcn)              │
│                               │  ← final file content
│ (def schema                   │
│   {:variants                  │
│    {:default {...}             │
│     :outline {...}}            │
│    :sizes {...}                │
│    :hover-rules {...}})        │
│                               │
│ (defn render [opts] ...)      │
│ (defn demo [] ...)            │
│                               │
└───────────────────────────────┘
```

Chat pane can show session history or collapse. Render pane shows the final demo. File is permanent.

### Chat Session Properties

- **Scoped**: One chat session per component conversion. Not the main command panel.
- **Contextual**: Agent knows: the source URL, the token map, the rt-node conventions.
- **Interactive**: User can refine: "shadow too dark", "add icon slot", "match this screenshot"
- **Persistent until dismissed**: Session stays open so you can iterate. Close when satisfied.
- **Re-openable**: Click ✓ component later → can open a new session to refine further.

### Render Pane

- **STUB**: Empty or shows component metadata from source library docs
- **WORKING**: Re-renders as code updates (may be partial during early writes)
- **READY**: Full `(demo)` function rendered — all variants, sizes, hover states

---

## JIT Conversion Flow (detailed)

```
User clicks "card/shadcn ·" in sidebar
        │
        ▼
Left pane shows State 1: Convert button + source info
        │
        ▼  (user clicks Convert button)
┌─────────────────────────────────────────┐
│  1. Read _source.edn → get GitHub URL    │
│  2. Status → :working                    │
│  3. Sidebar shows ⟳ next to item         │
│  4. Left pane switches to State 2        │
│  5. Fire Claude CLI agent with prompt:   │
│                                          │
│     Context:                             │
│     - Token map from _token_maps/        │
│     - rt-node conventions                │
│     - Component file format spec         │
│                                          │
│     Task:                                │
│     "Read shadcn card source at [URL].   │
│      Extract variants, sizes, states.    │
│      Write components/card/shadcn.cljc   │
│      with schema + render + demo fns."   │
│                                          │
│  6. Agent streams → left pane shows it   │
└─────────────────────────────────────────┘
        │
        ▼  (agent completes, file written)
┌─────────────────────────────────────────┐
│  7. Status → :ready in _source.edn      │
│  8. Sidebar shows ✓                      │
│  9. Left pane switches to State 3 (code) │
│ 10. Right pane renders (demo) as rt-node │
│ 11. File persists — next click instant   │
└─────────────────────────────────────────┘
```

## The Converted Component File Format

Every `.cljc` file has the same three sections:

```clojure
(ns components.button.shadcn
  (:require [components.design-tokens :refer [dt]]))

;; Source: github.com/shadcn-ui/ui/.../button.tsx
;; Library: shadcn v4 | Converted: 2026-03-01

;; ── 1. SCHEMA ───────────────────────────────────────────────
;; Data-only spec: what the component IS.

(def schema
  {:shared {:radius 10 :font-size 14 :font-weight 500
            :padding [0 10 0 10] :border-width 0.9}
   :variants
   {:default     {:bg [:primary 1.0]       :fg :primary-fg}
    :secondary   {:bg [:secondary 1.0]     :fg :secondary-fg}
    :destructive {:bg [:destructive 0.2]   :fg :destructive}
    :outline     {:bg [:white 0.04]        :fg :fg-muted  :border :border}
    :ghost       {:bg :transparent         :fg :fg-muted}
    :link        {:bg :transparent         :fg :accent}}
   :sizes
   {:xs {:h 24 :font 12 :pad [0 8 0 8]}
    :sm {:h 28 :font 12 :pad [0 10 0 10]}
    :md {:h 32 :font 14 :pad [0 10 0 10]}
    :lg {:h 40 :font 14 :pad [0 14 0 14]}}
   :hover-rules
   {:default [:alpha 0.8]  :secondary [:alpha 0.8]
    :destructive [:alpha-shift +0.1]
    :outline [:swap {:bg :muted :fg :foreground}]
    :ghost [:swap {:bg :muted :fg :foreground}]
    :link [:underline true]}
   :slots [:leading-icon :label :trailing-icon]})

;; ── 2. RENDER ───────────────────────────────────────────────
;; Pure function: options → rt-node. Used in application code.

(defn render
  [{:keys [variant size label on-click]
    :or {variant :default size :md}}]
  ...)

;; ── 3. DEMO ─────────────────────────────────────────────────
;; Showcase for the preview pane. Shows all variants + sizes.

(defn demo []
  ...)
```

---

## Token Map: shadcn v4

Extracted 2026-03-01 from live shadcn docs page via Claude-in-Chrome.
Colors converted from `lab()`/`oklab()` → RGBA via `<canvas>` `getImageData`.

```
shadcn CSS var                lab() value           RGBA [0-255]       Softland dt ~
──────────────────────────── ───────────────────── ───────────────── ─────────────
--color-primary              lab(90.95%)           [229,229,229,1]   :fg
--color-primary-foreground   lab(7.78%)            [23,23,23,1]      :bg
--color-secondary            lab(15.20%)           [38,38,38,1]      :bg-muted
--color-secondary-foreground lab(98.26%)           [250,250,250,1]   :fg
--color-destructive          lab(63.7% 60.7 31.3) [255,100,103,1]   :destructive
--color-foreground           lab(98.26%)           [250,250,250,1]   :fg
--color-muted                lab(15.20%)           [38,38,38,1]      :bg-muted
--color-accent               lab(27.04%)           [64,64,64,1]      :bg-hover
--color-border               lab(100%/0.1)         [255,255,255,0.1] :border (lower a)
--color-background           lab(2.75%)            [10,10,10,1]      :bg
--radius                     0.625rem              10px               custom (between :lg and :xl)
```

Key findings:
- Tailwind v4 uses `lab()`/`oklab()` color spaces (not hex/rgb)
- Hover via alpha reduction (`hover:bg-primary/80`) or token swap (`hover:bg-muted`)
- `data-variant` / `data-size` attributes on elements (not className variants)
- Zero `:hover` CSS rules in CSSOM — Tailwind v4 compiles them differently
- `getComputedStyle` + canvas `getImageData` is the reliable path to RGBA

Saved to: `components/_token_maps/shadcn-v4.edn`

---

## What Generalizes

The architecture is library-agnostic. To add a new library:

1. Create `_token_maps/library-name.edn` with its color/spacing map
2. Add `library-name` entries to each component's `_source.edn`
3. The conversion agent receives the appropriate token map as context
4. The output file format is identical: schema + render + demo

Planned libraries:
- **shadcn v4** (current — dark theme, Tailwind v4, lab() colors)
- **Linear** (future — our primary design reference)
- **Softland custom** (future — user's own design language)

---

## Implementation Phases

### Phase 1: Inventory + First Conversions
- Scrape shadcn registry → `_registry.edn` + `_source.edn` stubs
- Save token map → `_token_maps/shadcn-v4.edn`
- Convert first 5 manually: button, card, badge, input, dialog
- Sidebar shows component tree with ·/✓ indicators

### Phase 2: Left Pane UX
- Empty file detection → show Convert button (runtime overlay)
- Click Convert → fire agent + show stream in left pane
- On completion → load code, render demo in right pane

### Phase 3: Verification
- Split view: browser render (Claude-in-Chrome) vs canvas render
- Per-state screenshot comparison
- Corrections feed into token map refinement

### Phase 4: Multi-Library + Polish
- Linear token map + source URLs
- Status transitions, error handling, retry
- Component search / filter in sidebar

---

## Connection to Existing Infrastructure

| Piece | Status | Where |
|-------|--------|-------|
| Sidebar file explorer | EXISTS | `build-sidebar-tree` in loop.cljs |
| Split view (code + render) | EXISTS | `!extract-preview` render path |
| Agent runner (Claude CLI) | EXISTS | `fire-flow-run!` / streaming endpoint |
| rt-node rendering | EXISTS | `tree->rects` / `tree->text-ops` pipeline |
| `/hardcode` command (proof of concept) | EXISTS | Handler renders shadcn button demo |
| Component stubs | NEW | `_registry.edn` + `_source.edn` files |
| Convert button overlay | NEW | Runtime UI in left pane for empty files |
| Agent stream in left pane | NEW | Stream overlay layer |
| Conversion prompt template | NEW | Parameterized prompt with token map context |
