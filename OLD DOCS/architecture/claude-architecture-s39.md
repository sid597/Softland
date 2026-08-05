# Softland Architecture — Engineering Appendix (Session 39, 2026-03-21)

> **Status: DRAFT v1** — engineering hypotheses, not settled architecture.
> Decisions here are candidates under review, not commitments.
>
> **Master doc:** `docs/architecture/softland-master-architecture.md`
> defines the ontology (local world, split, artifact, trail, workflow, lineage)
> and operating law. **Read that first.** This doc provides concrete data shapes,
> event schemas, gap analysis, and migration detail underneath that law.
>
> **Hard rule: scene elements are DERIVED ONLY.**
> They are never stored, never persisted, never truth. They are a projection
> of watched Rama PState + client-local attention state, recomputed by Electric
> on every change. If the derivation is lost, it can be rebuilt from truth.

## Origin

This document emerged from a deep architectural conversation about how
UI elements should be represented, rendered, and interacted with in a
GPU-rendered, reactive, event-sourced system.

---

## Truth Boundary (definitive classification)

Every piece of state belongs to exactly ONE of these categories:

| Category | Where it lives | Examples | Properties |
|----------|---------------|----------|------------|
| **Committed truth** | Rama PState (server, persisted) | expanded-dirs, selected-file, project root, dir-cache, file contents, design tokens, component specs, agent trails, discourse claims | Survives restart. Undoable. History via event log. Shareable. |
| **Client-local** | Atoms (client, ephemeral) | hover, scroll-y, viewport, caret-blink, mouse-x/y, drag-preview, pointer state | Dies on refresh. No history. No undo. High-frequency OK. |
| **Derived** | Computed by Electric (not stored) | Scene elements, rect buffers, text ops, hit regions, layout positions | Never stored. Rebuilt from truth + client-local on any change. |

**No state may appear in more than one category.** If you're unsure, it's probably derived.

Concrete classification for the sidebar slice:

| Field | Category | Why |
|-------|----------|-----|
| expanded-dirs | Committed (Rama) | Survives restart, undoable |
| selected-file | Committed (Rama) | Survives restart, undoable |
| project root | Committed (Rama) | Survives restart |
| dir-cache (file tree) | Committed (Rama) | Shared truth from filesystem |
| hovered element | Client-local (atom) | Ephemeral, 60Hz, no history needed |
| sidebar scroll-y | Client-local (atom) | Ephemeral, no undo needed |
| viewport dimensions | Client-local (atom) | Device-specific |
| sidebar scene elements | Derived (Electric) | Projection of above two categories |

---

## The Three Things

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  RAMA                                          GROUND TRUTH      │
│                                                                  │
│  Everything that ever happened. Append-only.                     │
│  All other state is a projection of this.                        │
│                                                                  │
│  Event Log              PStates (materialized views)             │
│  ──────────             ────────────────────────────             │
│  [:file/save ...]       {:files {path → content}}                │
│  [:dir/expand ...]      {:workspace {:expanded #{} :selected ..}}│
│  [:design/set-prop ..]  {:design {:tokens {} :components {}}}    │
│  [:agent/start ...]     {:trails {id → steps}}                   │
│  [:dg/claim ...]        {:agents {:runs [] :status ..}}          │
│                                                                  │
│  NOTE: hover, scroll, pointer are NOT Rama events.               │
│  They stay client-local. See Truth Boundary above.               │
│                                                                  │
│  History = the event log itself                                  │
│  Single-user undo = replay without last N events (tractable)     │
│  Multi-user collaboration = UNSOLVED. Needs CRDT/OT for text,   │
│    event ordering for structural ops. Design TBD.                │
│  Commitment boundary: signal → proposal → commit                 │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
                                │
                                │ e/watch (PStates → reactive signals)
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  ELECTRIC                                  CONNECTIVE TISSUE     │
│                                                                  │
│  Derives what's on screen from what's true.                      │
│  Differential: only recomputes what changed.                     │
│  Spans server ↔ client (no API boundary).                        │
│                                                                  │
│  ┌─ SERVER ──────────────┐  ┌─ CLIENT ───────────────────────┐   │
│  │ e/watch Rama PStates  │  │ Design spec resolution         │   │
│  │ File I/O              │  │ Layout computation              │   │
│  │ Agent orchestration   │  │ Scene element derivation        │   │
│  │ DG queries            │──│ GPU buffer management           │   │
│  │                       │  │ Event handlers                  │   │
│  └───────────────────────┘  └────────────────────────────────┘   │
│                                                                  │
│  Key operation: e/for-by :key [element elements]                 │
│    mount   → create GPU slot + register handler                  │
│    change  → update GPU slot                                     │
│    unmount → free GPU slot + unregister handler                  │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
                                │
                                │ element data → GPU buffers
                                ▼
┌──────────────────────────────────────────────────────────────────┐
│                                                                  │
│  WEBGPU                                          TERMINAL        │
│                                                                  │
│  Turns element data into pixels.                                 │
│  One canvas. No DOM elements.                                    │
│                                                                  │
│  Buffers:                     Draw calls:                        │
│  rect instances (28f/rect)    shadows → rects → text → chrome    │
│  text instances (12f/glyph)   All in ONE render pass.            │
│  shadow instances (20f/shd)   Painter's order.                   │
│                                                                  │
│  Camera: {pan_x, pan_y, zoom, screen_w, screen_h}               │
│  Zoom + pan = continuous semantic zoom.                           │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## The Data Model

Three layers of data flow through the system:

### Layer 1: Design Spec (pure data, stored in Rama)

Describes HOW things look. Tokens, components, states, layout rules.
Separate from WHAT data fills them.

```clojure
;; See: DESIGN SPEC FORMAT section below
{:tokens {:surface-base [0.06 0.06 0.07 1.0] ...}
 :components {"sidebar" {:width 256 :bg :surface-base ...}
              "file-row" {:height 32 :padding-x 16 :states {...}}
              "button" {:height 32 :radius 6 ...}
              ...}}
```

### Layer 2: Scene (flat keyed elements, derived by Electric)

A DERIVED projection of watched truth + client-local state.
Never stored, never persisted. Each element carries identity, position,
appearance, and action descriptors (not closures).

```clojure
{"/src/main.cljs"
 {:key "/src/main.cljs"
  :bounds {:x 0 :y 80 :w 256 :h 32}
  :visual {:bg [0.18 0.22 0.35 1.0] :radius 4}
  :action {:click [:workspace/file-select "/src/main.cljs"]   ;; → Rama event
           :hover [:local/set-hover "/src/main.cljs"]}        ;; → client-local atom
  :z 4}}
```

Three kinds of actions (determines commitment level):
- `[:workspace/...]` — semantic, committed to Rama, undoable
- `[:local/...]` — ephemeral, stays in client atoms, no history
- `[:proposal/...]` — requires review before committing (e.g. file save)

### Layer 3: GPU Buffers (binary, managed by pool slots)

28 floats per rect, packed into instance buffers.
Written incrementally — only changed elements update their slot.

---

## The Interaction Loop

```
pixels → mouse (x,y) → scan element bounds → find element → call handler
   ↑                                                              │
   │                                                              ▼
   │                                                    emit EVENT to Rama
   │                                                              │
   │                                                              ▼
pixels ← GPU ← Electric re-derives ← PState updates ← Rama processes event
```

Future commitment boundary:
```
click → PROPOSAL → review → COMMIT → Rama
(enables undo, collaboration, conflict resolution)
```

### Inverse Mapping (pixels → data)

Five known strategies, applicable at different scales:

| Scale | Strategy | Mechanism |
|-------|----------|-----------|
| <100 elements | Reconstruct tree | Build tree from atoms, walk it |
| <1000 elements | Scan element bounds | Walk scene elements in reverse z |
| <10,000 elements | Spatial index | Grid hash or quadtree over bounds |
| >10,000 elements | ID buffer | GPU renders element IDs to offscreen texture, read 1 pixel |
| Any (hover) | Cached ID buffer | Read once on layout change, CPU lookup on every hover |

Current sidebar: reconstruction (rebuild tree per click).
Target: scan element bounds from scene map (identity preserved).

---

## The Zoom Levels

```
zoom 0.001 ─── KNOWLEDGE LANDSCAPE ─── 10,000+ nodes
zoom 0.01  ─── DOMAIN ─────────────── 1,000+ nodes (Q→C→E→D→R→F)
zoom 0.1   ─── TOPIC ──────────────── 100+ nodes (trails, evidence)
zoom 1.0   ─── GROUND LEVEL ───────── interactive models, SCI REPL
zoom 10    ─── BELOW GROUND ────────── workflow engine, agent runtime
zoom 100   ─── THE CODE ITSELF ─────── editor, sidebar, Softland editing itself
```

Camera uniform `{ zoom: N }` → GPU draws at that scale.
Continuous, not mode-switched.

---

## DESIGN SPEC FORMAT

> **Status: DRAFT v1** — Session 39.

The design spec answers "how does a sidebar/button/card look?" as pure data, not
procedural code. It replaces the scattered `def sidebar-w 256` constants and
inline `{:bg [0.16 0.16 0.19 1.0]}` maps with a single inspectable, themeable,
serializable EDN structure stored in Rama.

### Design Principles

**What this learns from existing systems:**

| System | Key Idea Adopted | What We Skip |
|--------|-----------------|--------------|
| Zed GPUI | Tailwind-style properties on GPU primitives; style = data struct with overflow, margin, padding, border, bg, corner-radius, shadow, text, opacity | Rust type system enforcement (we use EDN validation instead) |
| Flutter ThemeData | Immutable config object; ColorScheme + component-level overrides; ThemeExtension for custom token sets | Widget tree inheritance (we resolve tokens at spec read time, not render time) |
| Tailwind v4 @theme | Three-layer tokens (raw palette -> semantic -> component); CSS variables as the runtime representation | Build-time class generation (we resolve at Electric derivation time) |
| W3C DTCG spec (2025.10) | `$value`/`$type`/`$description` per token; groups; alias references `{color.accent}` | JSON format (we use EDN); cross-tool interop (single system) |
| CSS Custom Properties | Cascading overrides; scoped variables; computed values reference other variables | Cascade specificity (we use flat merge: base -> theme -> component -> state) |

**Core constraints from Softland's architecture:**
- Colors are `[r g b a]` float vectors (WebGPU shaders consume 0.0-1.0 floats, not hex)
- Sizes are in logical pixels (DPR scaling happens at the GPU camera level)
- No DOM — no CSS cascade. Resolution is a pure function: `(resolve-style spec tokens component state)`
- Tokens live in a Rama PState. `[:design/set-token :accent [0.35 0.55 0.95 1.0]]` is a Rama event
- Component specs live in a Rama PState. `[:design/set-prop "button" :height 36]` is a Rama event
- Electric watches the PState. Token change -> every component re-derives in one frame

### Format Overview

```
design-spec (stored in Rama PState {:design ...})
├── :tokens          — named primitive values (colors, sizes, spacing)
│   ├── :colors      — semantic color names -> [r g b a]
│   ├── :spacing     — named sizes -> px number
│   ├── :radii       — corner radius names -> px number
│   ├── :shadows     — named shadows -> {:blur :offset-y :color}
│   ├── :font-sizes  — named type scale -> px number
│   ├── :surfaces    — elevation-based bg/text colors -> [r g b a]
│   └── :typography  — named type presets -> {:size :alpha :weight}
│
├── :components      — keyed by component name (string)
│   └── "sidebar"
│       ├── :layout  — {:direction :width :height :padding :gap ...}
│       ├── :style   — {:bg :border-color :border-width :radius :shadow ...}
│       ├── :text    — {:color :size :alpha ...}
│       ├── :slots   — {:header {:height ...} :content {:clip? true} ...}
│       └── :states  — {:hover {:style {...}} :active {:style {...}} ...}
│
└── :aliases         — token references (resolved at read time)
    └── "accent" -> [:tokens :colors :accent]
```

### Layer 1: Tokens

Tokens are the atomic design decisions. Every visual value in the system traces
back to a named token. No raw color literals in component specs.

```clojure
;; ─── TOKEN MAP ────────────────────────────────────────────────────────
;; This is the COMPLETE token set. Swapping this map = swapping the theme.
;; All values are primitives: numbers, vectors, or maps of primitives.

{:tokens
 {:colors
  {;; Background surfaces (darkest to lightest)
   :bg              [0.09 0.09 0.11 1.0]
   :bg-subtle       [0.11 0.11 0.13 1.0]
   :bg-muted        [0.14 0.14 0.17 1.0]
   :bg-elevated     [0.13 0.13 0.16 1.0]
   :bg-hover        [0.16 0.16 0.19 1.0]
   :bg-selected     [0.20 0.24 0.36 0.9]
   :bg-active       [0.15 0.15 0.18 1.0]

   ;; Borders
   :border          [0.22 0.22 0.28 1.0]
   :border-subtle   [0.18 0.18 0.22 0.6]

   ;; Foreground text
   :fg              [0.90 0.90 0.92 1.0]
   :fg-muted        [0.55 0.55 0.60 1.0]
   :fg-subtle       [0.40 0.40 0.45 0.8]
   :fg-section      [0.42 0.42 0.48 1.0]

   ;; Semantic
   :accent          [0.35 0.55 0.95 1.0]
   :accent-muted    [0.25 0.38 0.65 0.3]
   :destructive     [0.90 0.30 0.30 1.0]
   :success         [0.30 0.80 0.50 1.0]
   :warning         [0.95 0.75 0.25 1.0]

   ;; Surfaces (elevation-based, for panels/chrome)
   :surface-sunken   [0.05 0.06 0.08 1.0]
   :surface-base     [0.07 0.08 0.11 1.0]
   :surface-elevated [0.12 0.15 0.21 1.0]
   :surface-hover    [0.13 0.17 0.22 1.0]
   :surface-active   [0.18 0.23 0.31 1.0]

   ;; Text on surfaces
   :text-primary     [0.90 0.93 0.97 1.0]
   :text-secondary   [0.55 0.61 0.70 1.0]
   :text-muted       [0.36 0.42 0.50 1.0]

   ;; Agent/trail-specific
   :trail-reasoning  [0.82 0.82 0.85 1.0]
   :trail-tool       [0.00 0.63 0.89 1.0]
   :trail-result     [0.30 0.80 0.50 1.0]
   :trail-error      [0.90 0.30 0.30 1.0]

   ;; Flow canvas node colors
   :flow-intake      [0.35 0.55 0.95 0.15]
   :flow-run         [0.30 0.80 0.50 0.15]
   :flow-review      [0.95 0.75 0.25 0.15]
   :flow-done        [0.55 0.55 0.60 0.10]}

  :spacing
  {:xs 4  :sm 8  :md 12  :lg 16  :xl 24  :xxl 32}

  :radii
  {:none 0  :sm 4  :md 6  :lg 8  :xl 12  :full 9999}

  :shadows
  {:none {}
   :sm   {:blur 4   :offset-y 1  :color [0 0 0 0.15]}
   :md   {:blur 8   :offset-y 2  :color [0 0 0 0.25]}
   :lg   {:blur 16  :offset-y 4  :color [0 0 0 0.35]}}

  :font-sizes
  {:xs 10  :sm 12  :md 14  :lg 16  :xl 20}

  :typography
  {:title    {:size :xl  :alpha 1.0}
   :subtitle {:size :lg  :alpha 0.9}
   :body     {:size :md  :alpha 0.85}
   :caption  {:size :sm  :alpha 0.6}
   :overline {:size :xs  :alpha 0.7}}

  ;; Character advance ratio — font-specific (Ubuntu Sans Mono = 0.56)
  ;; Used everywhere: cursor, text layout, truncation, hit testing
  :char-advance-ratio 0.56}}
```

**Token referencing convention:** Component specs use keyword paths into the
token map. `[:colors :accent]` means "look up `:accent` in `:tokens :colors`."
Resolution is a flat lookup, not a cascade.

### Layer 2: Component Specs

Each component spec is a map keyed by string name. The spec describes structure
(layout, slots) and appearance (style, text, states) using token references.

**Convention for token references:** Any value that is a keyword vector like
`[:colors :bg-hover]` is resolved against the token map. Raw values (numbers,
literal color vectors) are used as-is. This lets specs mix references and
overrides.

```clojure
;; ─── COMPONENT SPECS ──────────────────────────────────────────────────

{:components
 {;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; SIDEBAR (file explorer panel)
  ;; ════════════════════════════════════════════════════════════════════
  "sidebar"
  {:layout {:direction :column
            :width     256
            :height    :viewport   ;; special: fills viewport height
            :padding   [0 0 0 0]
            :gap       0}

   :style {:bg           [:colors :surface-base]
           :border-right {:width 1 :color [:colors :border]}}

   :slots
   {:header  {:height   48
              :style    {:bg [:colors :bg-elevated]
                         :border-bottom {:width 1 :color [:colors :border-subtle]}}
              :text     {:color [:colors :text-muted]
                         :size  [:font-sizes :xs]
                         :transform :uppercase}}

    :content {:clip?    true
              :layout   {:direction :column :gap 0}
              :scroll   {:axis :y :bar-width 6
                         :thumb-color [0.35 0.35 0.40 0.5]}}

    :back-btn {:height  48
               :style   {:bg [:colors :bg-elevated]
                          :border-bottom {:width 1 :color [:colors :border-subtle]}}
               :text    {:primary   {:size [:typography :subtitle :size]
                                     :color [:colors :text-primary]
                                     :alpha [:typography :subtitle :alpha]}
                         :secondary {:size [:typography :body :size]
                                     :color [:colors :text-secondary]
                                     :alpha 0.7}}}

    :breadcrumb {:height 24
                 :style  {:bg [:colors :bg]
                           :border-bottom {:width 1 :color [:colors :border-subtle]}}
                 :text   {:color [:colors :text-muted]
                          :size  [:typography :body :size]
                          :alpha 0.8}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; FILE-ROW (one entry in the sidebar)
  ;; ════════════════════════════════════════════════════════════════════
  "file-row"
  {:layout {:direction :row
            :height    32
            :width     :parent     ;; fills parent width
            :padding   [0 16 0 16] ;; [top right bottom left]
            :align     :center}

   :style {:bg     :transparent
           :radius [:radii :sm]}

   :text {:color  [:colors :fg]
          :size   13           ;; sidebar-font-size, slightly below :md
          :alpha  1.0
          :truncate {:suffix ".." :max-chars :computed}}

   :slots
   {:indent-guide {:width 1
                   :color [:colors :border]
                   :alpha 0.10
                   :per-depth {:offset 14}}  ;; sidebar-indent-px

    :chevron {:width    :text     ;; width of 2 chars
              :chars    {:expanded "▾ " :collapsed "▸ " :file "  "}}

    :accent-bar {:width  2
                 :radius [:radii :sm]
                 :color  [:colors :accent]
                 :inset  {:x 9 :y 6 :bottom 6}}  ;; sidebar-item-inset + 1

    :highlight {:inset   {:x 8 :top 2 :bottom 2}  ;; sidebar-item-inset
                :radius  [:radii :sm]}}

   :states
   {:hover    {:slots {:highlight {:style {:bg [:colors :bg-hover]}}}}
    :active   {:style {:bg :transparent}
               :text  {:color [0.95 0.95 0.98 1.0]}
               :slots {:highlight {:style {:bg [:colors :bg-selected]}}
                       :accent-bar {:visible true}}}
    :disabled {:text {:alpha 0.3}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; BUTTON (generic interactive button)
  ;; ════════════════════════════════════════════════════════════════════
  "button"
  {:layout {:direction :row
            :height    32
            :padding   [0 [:spacing :sm] 0 [:spacing :sm]]  ;; token ref in padding
            :align     :center
            :justify   :center}

   :style {:bg     [:colors :accent]
           :radius [:radii :md]
           :shadow :none}

   :text {:color  [1 1 1 1]
          :size   [:font-sizes :sm]
          :alpha  1.0}

   :slots
   {:icon  {:position :leading   ;; before label
            :size     16
            :gap      [:spacing :xs]}
    :label {:position :center}
    :badge {:position :trailing  ;; after label
            :gap      [:spacing :xs]}}

   :variants
   {:solid   {:style {:bg [:colors :accent]}
              :text  {:color [1 1 1 1]}}
    :outline {:style {:bg [0 0 0 0]
                      :border {:width 1 :color [:colors :accent]}}
              :text  {:color [:colors :accent]}}
    :ghost   {:style {:bg [0 0 0 0]}
              :text  {:color [:colors :fg-muted]}}
    :danger  {:style {:bg [:colors :destructive]}
              :text  {:color [1 1 1 1]}}}

   :states
   {:hover    {:style {:bg [:colors :surface-hover]}
               :text  {:alpha 1.0}}
    :active   {:style {:bg [:colors :surface-active]}}
    :disabled {:style {:bg [:colors :bg-muted]}
               :text  {:alpha 0.4}}
    :loading  {:text  {:label "..."} ;; slot override
               :style {:bg [:colors :bg-muted]}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; CARD (container with border, shadow, radius)
  ;; ════════════════════════════════════════════════════════════════════
  "card"
  {:layout {:direction :column
            :padding   [:spacing :md]
            :gap       [:spacing :sm]}

   :style {:bg           [:colors :bg-subtle]
           :radius       [:radii :lg]
           :border       {:width 1 :color [:colors :border]}
           :shadow       :md}

   :slots
   {:header  {:layout {:direction :row :align :center :justify :between}
              :text   {:color [:colors :fg]
                       :size  [:font-sizes :md]}}
    :body    {:layout {:direction :column :gap [:spacing :sm]}}
    :footer  {:layout {:direction :row :align :center :justify :end}
              :style  {:border-top {:width 1 :color [:colors :border-subtle]}}
              :text   {:color [:colors :fg-muted]
                       :size  [:font-sizes :sm]}}}

   :variants
   {:elevated {:style {:bg [:colors :bg-elevated]
                       :shadow :lg}}
    :muted    {:style {:bg [:colors :bg-muted]
                       :shadow :none
                       :border {:width 0}}}
    :flat     {:style {:shadow :none}}}

   :states
   {:hover {:style {:bg [:colors :bg-hover]
                    :shadow :lg}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; TRAIL-BLOCK (one step in a reasoning trail)
  ;; ════════════════════════════════════════════════════════════════════
  ;;
  ;; Trail blocks are polymorphic: :block-type determines which slot
  ;; template is active. The spec defines the visual envelope and
  ;; per-type overrides.
  ;;
  "trail-block"
  {:layout {:direction :column
            :width     :parent
            :padding   [0 12 0 12]   ;; pad = 12 (matches current trail.cljs)
            :gap       [:spacing :xs]}

   :style {:bg     :transparent
           :radius [:radii :md]}

   :text {:color [:colors :trail-reasoning]
          :size  [:font-sizes :md]
          :line-height-add 4}         ;; line-h = font-size + 4

   :slots
   {:header    {:layout {:direction :row :align :center}
                :text   {:color [:colors :fg]
                         :size  [:typography :subtitle :size]}}
    :body      {:layout {:direction :column :gap 0}}
    :collapse  {:position :top-right
                :text {:chars {:expanded "▾" :collapsed "▸"}
                       :color [:colors :fg-muted]}}}

   ;; Block-type variants (polymorphic rendering)
   :variants
   {:reasoning {:style {:bg :transparent}
                :text  {:color [:colors :trail-reasoning]}
                ;; Markdown sub-blocks: header, paragraph, code, blockquote, list
                :slots {:md-header   {:text {:color [0.90 0.89 0.89 1.0]
                                             :size  [:typography :title :size]}}
                        :md-code-bg  {:style {:bg [:colors :bg-muted]
                                              :radius [:radii :sm]
                                              :padding [:spacing :sm]}}
                        :md-quote    {:slots {:bar {:width 3
                                                    :color [:colors :accent]
                                                    :radius 2}}}
                        :md-list     {:indent 16
                                      :bullet-chars {:unordered "- "
                                                     :ordered   :numeric}}}}

    :tool-use  {:style {:bg [:colors :bg-muted]
                        :radius [:radii :md]
                        :border {:width 1 :color [:colors :border-subtle]}}
                :text  {:color [:colors :trail-tool]}
                :slots {:tool-name {:text {:color [:colors :trail-tool]
                                           :size  [:font-sizes :sm]}}
                        :tool-args {:text {:color [:colors :fg-muted]
                                           :size  [:font-sizes :sm]}}}}

    :tool-result {:style {:bg :transparent
                          :border-left {:width 2 :color [:colors :trail-result]}}
                  :text  {:color [:colors :trail-result]}}

    :error     {:style {:bg :transparent
                        :border-left {:width 2 :color [:colors :trail-error]}}
                :text  {:color [:colors :trail-error]}}

    :streaming {:style {:bg :transparent}
                :text  {:color [:colors :trail-reasoning]
                        :alpha :shimmer}}}  ;; :shimmer = animated alpha

   :states
   {:hover     {:style {:bg [:colors :bg-hover]}}
    :collapsed {:slots {:body {:visible false}
                        :collapse {:text {:chars {:use :collapsed}}}}}
    :expanded  {:slots {:collapse {:text {:chars {:use :expanded}}}}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; FLOW-NODE (one node in the workflow canvas)
  ;; ════════════════════════════════════════════════════════════════════
  ;;
  ;; Flow nodes sit on the zoomable canvas. They have typed appearance
  ;; based on workflow state and connections to other nodes via edges.
  ;;
  "flow-node"
  {:layout {:direction :column
            :width     220
            :min-height 80
            :padding   [:spacing :md]
            :gap       [:spacing :sm]}

   :style {:bg     [:colors :bg-elevated]
           :radius [:radii :xl]
           :border {:width 1 :color [:colors :border]}
           :shadow :md}

   :text {:color [:colors :fg]
          :size  [:font-sizes :sm]}

   :slots
   {:status-dot {:size     8
                 :radius   [:radii :full]
                 :position {:x [:spacing :md] :y [:spacing :md]}}
    :title      {:text {:color [:colors :fg]
                        :size  [:font-sizes :md]}}
    :subtitle   {:text {:color [:colors :fg-muted]
                        :size  [:font-sizes :sm]}}
    :progress   {:height   4
                 :radius   [:radii :sm]
                 :track    [:colors :bg-muted]
                 :fill     [:colors :accent]}
    :badge      {:radius [:radii :full]
                 :text   {:size [:font-sizes :xs]}}}

   ;; Workflow-state variants
   :variants
   {:intake   {:style {:bg [:colors :flow-intake]
                       :border {:width 1 :color [:colors :accent]}}
               :slots {:status-dot {:color [:colors :accent]}}}
    :run      {:style {:bg [:colors :flow-run]}
               :slots {:status-dot {:color [:colors :success]}}}
    :review   {:style {:bg [:colors :flow-review]}
               :slots {:status-dot {:color [:colors :warning]}}}
    :done     {:style {:bg [:colors :flow-done]
                       :shadow :none}
               :slots {:status-dot {:color [:colors :fg-subtle]}}}
    :error    {:style {:bg [:colors :flow-intake]
                       :border {:width 2 :color [:colors :destructive]}}
               :slots {:status-dot {:color [:colors :destructive]}}}}

   :states
   {:hover     {:style {:shadow :lg
                        :border {:color [:colors :accent]}}}
    :selected  {:style {:border {:width 2 :color [:colors :accent]}
                        :shadow :lg}}
    :dragging  {:style {:shadow :lg}
                :layout {:opacity 0.7}}
    :disabled  {:style {:bg [:colors :bg-muted]}
                :text  {:alpha 0.4}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; CMD-PANEL (command input bar)
  ;; ════════════════════════════════════════════════════════════════════
  "cmd-panel"
  {:layout {:direction :row
            :height    40               ;; cmd-panel-h
            :width     :viewport
            :padding   [0 24 0 24]      ;; left margin for prompt
            :align     :center
            :position  {:anchor :bottom
                        :offset-y 24}}  ;; sits above status-bar

   :style {:bg [:colors :surface-elevated]
           :border-top {:width 0 :color [:colors :border]}}

   :text {:color [:colors :fg]
          :size  [:font-sizes :md]}

   :slots
   {:prompt {:text {:color [:colors :fg-muted]
                    :size  [:font-sizes :md]}
             :template "[{provider}]> "}

    :input  {:text {:color [:colors :fg]
                    :size  [:font-sizes :md]}
             :caret {:width  2
                     :color  [0.9 0.9 0.9 1.0]
                     :blink  {:on-ms 530 :off-ms 530}
                     :inset  {:top 8 :bottom 8}}}

    :status {:position :trailing
             :text {:color [:colors :fg-muted]
                    :size  [:font-sizes :xs]}}}

   :states
   {:focused  {:style {:border-top {:width 1 :color [:colors :accent]}}}
    :hidden   {:layout {:height 0}
               :style  {:bg [0 0 0 0]}}
    :loading  {:slots {:status {:text {:color [:colors :warning]}}}}
    :error    {:slots {:status {:text {:color [:colors :destructive]}}}}}}

  ;;
  ;; ════════════════════════════════════════════════════════════════════
  ;; STATUS-BAR (bottom chrome bar)
  ;; ════════════════════════════════════════════════════════════════════
  "status-bar"
  {:layout {:direction :row
            :height    24               ;; status-bar-h
            :width     :viewport
            :padding   [0 [:spacing :md] 0 [:spacing :md]]
            :align     :center
            :justify   :between
            :position  {:anchor :bottom :offset-y 0}}

   :style {:bg [0.12 0.12 0.16 1.0]}

   :text {:color [:colors :fg-muted]
          :size  [:font-sizes :xs]}

   :slots
   {:left   {:layout {:direction :row :gap [:spacing :md]}
             :text   {:color [:colors :fg-muted]}}
    :center {:text {:color [:colors :fg-subtle]}}
    :right  {:layout {:direction :row :gap [:spacing :md]}
             :text   {:color [:colors :fg-muted]}}}}}}
```

### Layer 3: Resolution Algorithm

Resolution is a pure function. No side effects, no mutable state. Given a
component name, a set of active states, an optional variant, and the token
map, it returns a flat style map ready for `rt-node`.

```
resolve-style : (spec, tokens, component-name, variant?, states) → flat-style-map
```

**Merge order (last wins):**
```
base component style
  ← variant override (if variant specified)
    ← state overrides (applied in declaration order)
```

**Token dereferencing:**
Any value that is a vector of keywords (e.g., `[:colors :accent]`) is resolved
against the token map by `get-in`. Literal values pass through unchanged.

```clojure
;; Pseudocode for resolution (NOT in the spec — this is implementation guidance)
;;
;; (defn resolve-style [design-spec component-name variant active-states]
;;   (let [tokens     (:tokens design-spec)
;;         component  (get-in design-spec [:components component-name])
;;         base       (:style component)
;;         var-patch  (get-in component [:variants variant :style])
;;         state-patches (map #(get-in component [:states % :style]) active-states)
;;         merged     (apply deep-merge base var-patch state-patches)]
;;     (walk-resolve-tokens merged tokens)))
;;
;; (defn walk-resolve-tokens [m tokens]
;;   "Replace every [:colors :x] with (get-in tokens [:colors :x])"
;;   ...)
```

**Example resolution trace:**

```
Input:  component="button", variant=:outline, states=[:hover]

1. Base style:   {:bg [:colors :accent],  :radius [:radii :md]}
2. + :outline:   {:bg [0 0 0 0],  :border {:width 1 :color [:colors :accent]}}
3. + :hover:     {:bg [:colors :surface-hover]}
4. Merged:       {:bg [:colors :surface-hover], :radius [:radii :md],
                  :border {:width 1 :color [:colors :accent]}}
5. Tokens resolved: {:bg [0.13 0.17 0.22 1.0], :radius 6,
                     :border {:width 1 :color [0.35 0.55 0.95 1.0]}}
```

### Theming

A theme is a replacement token map. Component specs stay identical.

```clojure
;; Light theme: only the token map changes
{:tokens
 {:colors
  {:bg             [0.96 0.96 0.97 1.0]
   :bg-subtle      [0.93 0.93 0.95 1.0]
   :bg-hover       [0.90 0.90 0.93 1.0]
   :bg-selected    [0.82 0.87 0.95 0.9]
   :bg-elevated    [0.98 0.98 0.99 1.0]
   :border         [0.82 0.82 0.86 1.0]
   :border-subtle  [0.88 0.88 0.90 0.6]
   :fg             [0.15 0.15 0.18 1.0]
   :fg-muted       [0.45 0.45 0.50 1.0]
   :accent         [0.20 0.45 0.90 1.0]
   ;; ... rest of colors
   }
  ;; spacing, radii, shadows, font-sizes — typically unchanged between themes
  ;; but CAN be overridden (e.g. a "compact" theme with smaller spacing)
  :spacing {:xs 4 :sm 8 :md 12 :lg 16 :xl 24 :xxl 32}}}
```

**Theme application as a Rama event:**
```clojure
;; Switch theme: one event, one frame
[:design/set-theme :light {:tokens {...}}]

;; Partial token override (keeps everything else)
[:design/set-token [:colors :accent] [0.90 0.30 0.50 1.0]]

;; Component-level override (design tool editing a property)
[:design/set-prop "file-row" [:layout :height] 36]

;; Add a state to an existing component
[:design/set-prop "button" [:states :streaming] {:text {:alpha :shimmer}}]
```

### Plugin Extension

Plugins can add new components or override existing ones. The merge is
always `deep-merge`, so a plugin can patch one property without replacing
the entire spec.

```clojure
;; Plugin registers a new component
[:plugin/load
 {:type :component
  :components
  {"kanban-card"
   {:layout {:direction :column
             :width     200
             :padding   [:spacing :sm]
             :gap       [:spacing :xs]}
    :style  {:bg     [:colors :bg-subtle]
             :radius [:radii :md]
             :border {:width 1 :color [:colors :border]}}
    :slots  {:title {:text {:size [:font-sizes :sm]}}
             :tags  {:layout {:direction :row :gap [:spacing :xs]}}}
    :states {:hover {:style {:shadow :md}}}}}}]

;; Plugin overrides existing component (partial patch)
[:plugin/load
 {:type :component-patch
  :patches
  {"sidebar" {:layout {:width 300}}    ;; wider sidebar
   "button"  {:variants
              {:primary {:style {:bg [0.80 0.20 0.40 1.0]}}}}}}]  ;; pink buttons
```

### Inspectability

The spec is pure data, so any tool can query it:

```clojure
;; "What is the hover color of a button?"
(get-in design-spec [:components "button" :states :hover :style :bg])
;; => [:colors :surface-hover]

;; Resolved:
(get-in tokens [:colors :surface-hover])
;; => [0.13 0.17 0.22 1.0]

;; "What variants does trail-block support?"
(keys (get-in design-spec [:components "trail-block" :variants]))
;; => (:reasoning :tool-use :tool-result :error :streaming)

;; "What slots does cmd-panel have?"
(keys (get-in design-spec [:components "cmd-panel" :slots]))
;; => (:prompt :input :status)

;; "List all components"
(keys (:components design-spec))
;; => ("sidebar" "file-row" "button" "card" "trail-block"
;;     "flow-node" "cmd-panel" "status-bar")
```

### Mapping to Current Code

| Current code | Design spec equivalent |
|---|---|
| `(def sidebar-w 256)` in sidebar.cljs | `[:components "sidebar" :layout :width]` |
| `(def sidebar-row-h 32)` in sidebar.cljs | `[:components "file-row" :layout :height]` |
| `(def cmd-panel-h 40)` in sidebar.cljs | `[:components "cmd-panel" :layout :height]` |
| `(def status-bar-h 24)` in sidebar.cljs | `[:components "status-bar" :layout :height]` |
| `(:bg-hover (:colors dt))` in ui-primitives.cljs | `[:tokens :colors :bg-hover]` (same) |
| `(def dt ...)` in design_tokens.cljc | `[:tokens]` (the entire token map) |
| `(case variant :solid ... :outline ...)` in ui-button | `[:components "button" :variants :solid]` etc. |
| `(cond active? ... hovered? ...)` in sidebar.cljs | `[:components "file-row" :states :active]` etc. |
| `(def md-style-colors ...)` in trail.cljs | `[:components "trail-block" :variants :reasoning :text]` |
| Inline `{:bg [0.10 0.10 0.13 1.0]}` in cmd_panel.cljs | `[:components "cmd-panel" :style :bg]` via tokens |

---

## SCENE ELEMENT FORMAT

> **Status: DRAFT v1** — Session 39

The flat keyed map that sits between Design Spec and GPU.
Each element carries: identity, bounds, visual, action descriptors, z-order.
Managed by Electric e/for-by — one reactive scope per element.

### Core Principle

Every visible thing on screen is a **scene element**. A scene element is a
flat ClojureScript map with a stable `:key`. The scene is a single flat map
from key to element: `{key -> element}`. There is no nesting — parent/child
is expressed through `:parent` references and `:z` ordering, not containment.

This is the critical departure from the current rt-node tree: rt-nodes nest
children inside parents, requiring full tree walks (`tree->rects`,
`tree->text-ops`) to produce GPU data. Scene elements are born flat and
absolute — each element independently maps to its GPU slot.

### Element Schema

```clojure
;; Every scene element has this shape.
;; All fields are concrete values — no lazy derivation, no deferred computation.

{;; === IDENTITY ===
 :key       "/src/main.cljs"         ;; DOMAIN IDENTITY, never positional index.
                                      ;; Files: path. Components: component-id. Chrome: role string.
                                      ;; Trails: step-id. Agents: run-id. NEVER :entry-0, :entry-1.

 ;; === COMPONENT ===
 :component :file-row                ;; Design spec component name (for token resolution).
 :state     :hovered                 ;; Visual state: :default :hovered :active :disabled :selected

 ;; === SPATIAL ===
 :x         0                        ;; Absolute x (post-layout, in world pixels)
 :y         112                      ;; Absolute y
 :w         256                      ;; Width
 :h         32                       ;; Height
 :z         4                        ;; Draw order (0 = back, higher = front)
 :parent    :sidebar/content         ;; Parent key (nil = root). Used for:
                                      ;;   - propagating scroll/position changes
                                      ;;   - clip region inheritance
                                      ;;   - hit-test bubbling

 ;; === VISUAL (GPU rect data) ===
 :bg        [0.16 0.16 0.19 1.0]     ;; Fill color [r g b a] (nil = no rect emitted)
 :radius    4                         ;; Uniform corner radius (or nil)
 :corner-radii  [4 4 4 4]            ;; Per-corner [tl tr br bl] (overrides :radius)
 :border-width  0                     ;; Uniform border (or nil)
 :border-widths [0 0 1 0]            ;; Per-side [t r b l] (overrides :border-width)
 :border-color  [0.18 0.18 0.22 0.6] ;; Border color [r g b a]
 :gradient      nil                   ;; [angle t-stop 0 0] or nil
 :gradient-color2 nil                 ;; [r g b a] second gradient stop
 :shadow    nil                       ;; {:blur N :offset-y N :color [r g b a]} or nil
 :clip?     false                     ;; If true, children are clipped to this element's bounds

 ;; === TEXT ===
 :text      [{:text "main.cljs"       ;; Text ops for the text GPU pipeline.
              :x    30 :y 22           ;; Absolute positions (not parent-relative).
              :size 13                 ;; Font size in px.
              :r 0.90 :g 0.90         ;; Text color channels.
              :b 0.92 :a 1.0
              :from 0 :to 9}]         ;; Character range (for partial updates / syntax hl).
                                       ;; Empty vector or nil = no text.

 ;; === ACTION DESCRIPTORS (no closures — pure data) ===
 :action    {:click [:workspace/file-select "/src/main.cljs"]   ;; Semantic → Rama event
             :hover [:local/set-hover "/src/main.cljs"]}        ;; Ephemeral → client atom
                                              ;; Three commitment levels:
                                              ;;   [:workspace/...] → Rama (persisted, undoable)
                                              ;;   [:local/...]     → client atom (ephemeral)
                                              ;;   [:proposal/...]  → review before commit
                                              ;; Dispatch resolves descriptors to side effects at the edge.
                                              ;; Scene data stays pure and serializable.
 }
```

### Design Decisions

**Why flat, not nested?**
The current rt-node tree requires O(n) tree walks on every frame to produce
GPU data. With flat elements, Electric's `e/for-by` tracks each element by
`:key` — when one element changes, only that element's GPU slot is updated.
The tree walk disappears entirely. Parent-child relationships are preserved
through `:parent` references for position propagation and hit-test bubbling.

**Why absolute coordinates?**
Layout computation happens once when the element is created or its parent
moves. The result is absolute screen coordinates stored directly in the
element. The GPU buffer packing function reads `:x :y :w :h` directly —
no traversal, no accumulation. When a parent scrolls, Electric propagates
the delta to children via `:parent` references.

**Why `:bg nil` instead of separate "has rect" flag?**
A scene element with `:bg nil` emits no GPU rect. This handles transparent
container elements (like the scroll-inner in the current sidebar) that exist
only for clipping or hit-testing. The text pipeline checks `:text` independently.
An element can have text without a rect, a rect without text, or both.

**Why `:state` as a single keyword?**
States are mutually exclusive visual variants. A file row is `:default` OR
`:hovered` OR `:selected` — never two at once. The design spec maps
`(component, state) -> visual properties`. If a future element genuinely
needs orthogonal states (e.g., `:selected` AND `:hovered`), `:state` becomes
a set — but keep it simple until that's needed.

**Sub-elements are separate scene elements, not properties.**
The highlight rect behind a hovered file row, the accent bar on the active
file, and the indent guides — these are all first-class scene elements with
their own `:key`, `:z`, and `:parent`. This means:
- They can be independently added/removed (hover on → add highlight element)
- Each gets its own GPU buffer slot (no re-packing the parent)
- Hit-testing bubbles through them to the parent via `:parent`

### Sidebar as Concrete Example

Here is the complete scene element map for a sidebar showing a project
with three entries (a directory, a hovered file, and the active file):

```clojure
;; The full scene map — one flat map, no nesting.
;; Keys are stable identifiers. Values are scene elements.

{;; === SIDEBAR FRAME ===

 :sidebar/root
 {:key       :sidebar/root
  :component :panel
  :state     :default
  :x 0  :y 0  :w 256  :h 800
  :z 0
 :parent    nil
  :bg        [0.07 0.08 0.11 1.0]     ;; surface-base
  :clip?     false}

 :sidebar/border
 {:key       :sidebar/border
  :component :chrome
  :state     :default
  :x 255  :y 0  :w 1  :h 800
  :z 1
  :parent    :sidebar/root
  :bg        [0.22 0.22 0.28 1.0]}    ;; border color

 ;; === BACK BUTTON (project header) ===

 :sidebar/back-btn
 {:key       :sidebar/back-btn
  :component :sidebar-header
  :state     :default
  :x 0  :y 0  :w 256  :h 48
  :z 2
  :parent    :sidebar/root
  :bg        [0.13 0.13 0.16 1.0]     ;; bg-elevated
  :border-widths [0 0 1 0]
  :border-color  [0.18 0.18 0.22 0.6]
  :text      [{:text "← myproject"  :x 16  :y 26  :size 14
               :r 0.90 :g 0.90 :b 0.92 :a 0.9  :from 0 :to 12}
              {:text "Project Workspace"  :x 32  :y 40  :size 13
               :r 0.55 :g 0.55 :b 0.60 :a 0.7  :from 0 :to 17}]
  :action    {:click [:workspace/project-back "/project"]}}

 ;; === CONTENT AREA (clips scrolled children) ===

 :sidebar/content
 {:key       :sidebar/content
  :component :panel-content
  :state     :default
  :x 0  :y 48  :w 256  :h 752       ;; viewport-h minus back-btn
  :z 1
  :parent    :sidebar/root
  :clip?     true}                    ;; hides overflow from scrolled entries

 ;; === ENTRY 0: expanded directory "src" ===

 "/project/src"
 {:key       "/project/src"
  :component :file-row
  :state     :default                 ;; not hovered, not active
  :x 0  :y 48  :w 256  :h 32        ;; absolute y = content-top + (0 * 32) - scroll-y
  :z 3
  :parent    :sidebar/content
  :text      [{:text "▾ src"  :x 16  :y 69  :size 13
               :r 0.90 :g 0.90 :b 0.92 :a 1.0  :from 0 :to 5}]
  :action    {:click [:workspace/dir-toggle "/project/src"]
              :hover [:local/set-hover "/project/src"]}}

 ;; === ENTRY 1: file "main.cljs" (HOVERED) ===
 ;; Hovered state adds a highlight sub-element and changes :state.

 "/project/src/main.cljs"
 {:key       "/project/src/main.cljs"
  :component :file-row
  :state     :hovered
  :x 0  :y 80  :w 256  :h 32
  :z 3
  :parent    :sidebar/content
  :text      [{:text "  main.cljs"  :x 30  :y 101  :size 13
               :r 0.90 :g 0.90 :b 0.92 :a 1.0  :from 0 :to 12}]
  :action    {:click [:workspace/file-select "/project/src/main.cljs"]
              :hover [:local/set-hover "/project/src/main.cljs"]}}

 ;; Highlight rect — exists ONLY when parent is hovered or selected
 "/project/src/main.cljs::hl"
 {:key       "/project/src/main.cljs::hl"
  :component :highlight
  :state     :default
  :x 8  :y 82  :w 240  :h 28        ;; inset from parent bounds
  :z 2                                ;; BEHIND the text (lower z than entry)
  :parent    "/project/src/main.cljs"
  :bg        [0.16 0.16 0.19 1.0]    ;; bg-hover
  :radius    4}

 ;; Indent guide — 1px vertical line at depth 1
 "/project/src/main.cljs::guide:0"
 {:key       "/project/src/main.cljs::guide:0"
  :component :indent-guide
  :state     :default
  :x 23  :y 80  :w 1  :h 32
  :z 2
  :parent    "/project/src/main.cljs"
  :bg        [0.22 0.22 0.28 0.10]}

 ;; === ENTRY 2: file "util.cljs" (ACTIVE / SELECTED) ===

 "/project/src/util.cljs"
 {:key       "/project/src/util.cljs"
  :component :file-row
  :state     :selected
  :x 0  :y 112  :w 256  :h 32
  :z 3
  :parent    :sidebar/content
  :text      [{:text "  util.cljs"  :x 30  :y 133  :size 13
               :r 0.95 :g 0.95 :b 0.98 :a 1.0  :from 0 :to 12}]
  :action    {:click [:workspace/file-select "/project/src/util.cljs"]
              :hover [:local/set-hover "/project/src/util.cljs"]}}

 ;; Selected highlight (brighter than hover)
 "/project/src/util.cljs::hl"
 {:key       "/project/src/util.cljs::hl"
  :component :highlight
  :state     :default
  :x 8  :y 114  :w 240  :h 28
  :z 2
  :parent    "/project/src/util.cljs"
  :bg        [0.20 0.24 0.36 0.9]    ;; bg-selected
  :radius    4}

 ;; Active accent bar (blue left indicator)
 "/project/src/util.cljs::acc"
 {:key       "/project/src/util.cljs::acc"
  :component :accent-bar
  :state     :default
  :x 9  :y 118  :w 2  :h 20
  :z 5                                ;; above highlight
  :parent    "/project/src/util.cljs"
  :bg        [0.35 0.55 0.95 1.0]    ;; accent
  :radius    4}

 ;; Indent guide for depth 1
 "/project/src/util.cljs::guide:0"
 {:key       "/project/src/util.cljs::guide:0"
  :component :indent-guide
  :state     :default
  :x 23  :y 112  :w 1  :h 32
  :z 2
  :parent    "/project/src/util.cljs"
  :bg        [0.22 0.22 0.28 0.10]}
 }
```

### How State Changes Flow

#### Hover: mouse enters `"/project/src/main.cljs"`

```
1. Mouse moves to (120, 90)
2. Hit-test: scan elements in reverse :z order whose bounds contain (120, 90)
   → finds "/project/src/main.cljs" (x:0 y:80 w:256 h:32)
3. Dispatch :hover action → [:local/set-hover "/project/src/main.cljs"]
4. Handler mutates atom: (swap! !sidebar-state assoc :hovered-id "/project/src/main.cljs")
5. Electric re-derives ONLY the changed elements:
   a. "/project/src/main.cljs" gets :state :hovered (was :default)
   b. "/project/src/main.cljs::hl" is ADDED to the scene (didn't exist before)
6. e/for-by diff:
   - "/project/src/main.cljs" → changed → update GPU rect slot
   - "/project/src/main.cljs::hl" → new key → allocate-slot! → write GPU rect
7. If "/project/src" was previously hovered:
   - "/project/src" → :state back to :default → update GPU rect slot
   - "/project/src::hl" → key removed → free-slot! (zero GPU rect)
```

The key insight: the ONLY elements that touch the GPU are the ones that
changed. Every other element's GPU slot is untouched. This is O(changed),
not O(total).

#### Hover exit: mouse leaves the sidebar entirely

```
1. Mouse moves to (300, 90) — outside sidebar bounds
2. Hit-test finds no sidebar element
3. Clear hover: (swap! !sidebar-state assoc :hovered-id nil)
4. Electric re-derives:
   - "/project/src/main.cljs" → :state back to :default (no visual change if
     :default and un-hovered have the same bg = nil)
   - "/project/src/main.cljs::hl" → REMOVED from scene → free-slot!
```

#### Expand/collapse directory `"/project/src"`

```
BEFORE: scene has `"/project/src"` (expanded), `"/project/src/main.cljs"`, and `"/project/src/util.cljs"`

1. Click on `"/project/src"` → toggle-expand! removes `"/project/src"` from expanded-dirs set
2. Electric re-derives the scene from sidebar-state:
   - `"/project/src"`: :state changes, chevron text changes "▾ src" → "▸ src"
   - `"/project/src/main.cljs"`, `"/project/src/main.cljs::hl"`, `"/project/src/main.cljs::guide:0"`: REMOVED
   - `"/project/src/util.cljs"`, `"/project/src/util.cljs::hl"`, `"/project/src/util.cljs::acc"`, `"/project/src/util.cljs::guide:0"`: REMOVED
3. e/for-by diff:
   - `"/project/src"`: update text ops, update GPU text buffer
   - file rows and sub-elements: free-slot! for each removed key

AFTER: scene has only `"/project/src"` (collapsed)

EXPAND again:
1. Click → add `"/project/src"` to expanded-dirs
2. Electric re-derives: file rows and sub-elements are ADDED
3. e/for-by: allocate-slot! for each new element, write GPU data
```

### GPU Buffer Mapping

Each scene element with non-nil `:bg` maps to exactly one GPU buffer slot
(28 floats = 112 bytes). The packing is:

```
Slot   Offset   Field            Scene Element Source
────   ──────   ─────            ────────────────────
  0    [0-3]    rect_geometry    :x  :y  :w  :h
  1    [4-7]    color            :bg [r g b a]
  2    [8-11]   corner_radii     :corner-radii [tl tr br bl]
                                   OR [:radius :radius :radius :radius]
                                   OR [0 0 0 0]
  3    [12-15]  border_widths    :border-widths [t r b l]
                                   OR [:border-width :border-width :border-width :border-width]
                                   OR [0 0 0 0]
  4    [16-19]  border_color     :border-color [r g b a]
                                   OR [0 0 0 0]
  5    [20-23]  gradient         :gradient [angle t-stop 0 0]
                                   OR [0 0 0 0]
  6    [24-27]  gradient_color2  :gradient-color2 [r g b a]
                                   OR [0 0 0 0]
```

The `pack-element` function (replacing `pack-rect` in buffer-pool) is trivial:

```clojure
(defn pack-element
  "Pack a scene element into a Float32Array(28) for the GPU rect pipeline.
   Only called when the element actually changed (detected by e/for-by)."
  [elem]
  (let [data (js/Float32Array. 28)
        cr   (:corner-radii elem)
        r    (or (:radius elem) 0.0)
        bw   (:border-widths elem)
        ubw  (or (:border-width elem) 0.0)
        bc   (or (:border-color elem) [0 0 0 0])
        gr   (or (:gradient elem) [0 0 0 0])
        gc2  (or (:gradient-color2 elem) [0 0 0 0])]
    ;; rect_geometry
    (aset data 0 (:x elem 0)) (aset data 1 (:y elem 0))
    (aset data 2 (:w elem 0)) (aset data 3 (:h elem 0))
    ;; color (bg)
    (let [bg (:bg elem [0 0 0 0])]
      (aset data 4 (nth bg 0)) (aset data 5 (nth bg 1))
      (aset data 6 (nth bg 2)) (aset data 7 (nth bg 3)))
    ;; corner_radii
    (if cr
      (do (aset data 8 (nth cr 0)) (aset data 9 (nth cr 1))
          (aset data 10 (nth cr 2)) (aset data 11 (nth cr 3)))
      (do (aset data 8 r) (aset data 9 r) (aset data 10 r) (aset data 11 r)))
    ;; border_widths
    (if bw
      (do (aset data 12 (nth bw 0)) (aset data 13 (nth bw 1))
          (aset data 14 (nth bw 2)) (aset data 15 (nth bw 3)))
      (do (aset data 12 ubw) (aset data 13 ubw) (aset data 14 ubw) (aset data 15 ubw)))
    ;; border_color
    (aset data 16 (nth bc 0)) (aset data 17 (nth bc 1))
    (aset data 18 (nth bc 2)) (aset data 19 (nth bc 3))
    ;; gradient
    (aset data 20 (nth gr 0)) (aset data 21 (nth gr 1))
    (aset data 22 (nth gr 2)) (aset data 23 (nth gr 3))
    ;; gradient_color2
    (aset data 24 (nth gc2 0)) (aset data 25 (nth gc2 1))
    (aset data 26 (nth gc2 2)) (aset data 27 (nth gc2 3))
    data))
```

Text ops from the `:text` field go to the separate text GPU pipeline
(12 floats/glyph). Shadow data from `:shadow` goes to the shadow pipeline
(20 floats/shadow). These are managed by their own buffer pools with the
same slot-based pattern — allocate on mount, update on change, free on unmount.

### Hit-Testing

Hit-testing against the flat scene map is a scan, not a tree walk.

```clojure
(defn hit-test-scene
  "Find the scene element under point (px, py).
   Scans all elements in reverse :z order (front to back).
   Returns the first element whose bounds contain the point,
   respecting :clip? parents.

   For <1000 elements this is fast enough (< 0.1ms).
   For >1000, replace with spatial index or ID buffer."
  [scene px py]
  (let [;; Sort once per layout change, cache the sorted list
        sorted (sort-by :z > (vals scene))]
    (loop [elems sorted]
      (when-let [elem (first elems)]
        (if (and (>= px (:x elem))
                 (< px (+ (:x elem) (:w elem)))
                 (>= py (:y elem))
                 (< py (+ (:y elem) (:h elem)))
                 (not (clipped? scene elem px py)))  ;; walk :parent chain for clip?
          elem
          (recur (rest elems)))))))

(defn clipped?
  "Walk the :parent chain to check if any ancestor with :clip? true
   excludes point (px, py) from its bounds."
  [scene elem px py]
  (loop [parent-key (:parent elem)]
    (if-not parent-key
      false
      (let [parent (get scene parent-key)]
        (if (and (:clip? parent)
                 (not (and (>= px (:x parent))
                           (< px (+ (:x parent) (:w parent)))
                           (>= py (:y parent))
                           (< py (+ (:y parent) (:h parent))))))
          true  ;; point is outside a clipping ancestor
          (recur (:parent parent)))))))

(defn dispatch-scene-event
  "Dispatch event to hit element, with bubbling through :parent chain.
   Returns {:handled? bool :action descriptor :element elem}."
  [scene elem event-type event-data]
  (loop [current elem]
    (if-not current
      {:handled? false}
      (if-let [action (get-in current [:action event-type])]
        {:handled? true :action action :element current :event-data event-data}
        (recur (get scene (:parent current)))))))
```

### Hidden Pick Pass / ID Buffer

Bounds scans are the default strategy for ordinary UI, but they are not the
only strategy. The other first-class option is a hidden GPU pick pass.

```text
scene elements
  -> visible pass (normal colors/text/shadows)
  -> hidden pick pass (same geometry, same z/clip, each element writes scene-id)

mousemove / click
  -> pixel lookup in pick texture
  -> scene-id
  -> scene element
  -> action descriptor / semantic resolver
```

Recommended use:

- ordinary sidebar / button / card UI: bounds scan is fine
- dense fields or exact topmost ownership: hidden pick pass is stronger
- hover at 60Hz: keep a CPU-cached copy of the pick texture if using ID picking
- click: one-pixel lookup is acceptable

Important distinction:

- the pick pass answers `which scene element owns this pixel?`
- the semantic layer answers `what does that element mean?`

Examples:

- file row → open/select file
- button → invoke action
- editor line → compute local text position after element pick
- knowledge cluster → open cluster / provenance object

### Text Content and Rect Elements

A scene element can carry BOTH a rect (`:bg`) and text (`:text`). They are
independent — the rect goes to the rect GPU pipeline, text ops go to the
text GPU pipeline. This matches the current design where a sidebar entry
has a background rect AND a text label rendered by different shaders in the
same render pass.

```
Scene element                    GPU pipelines
─────────────                    ─────────────
"/project/src/main.cljs"         text pipeline (entry label)
  :bg nil                   NO rect emitted
  :text [{...}]             → 1 text instance per glyph

"/project/src/main.cljs::hl" rect pipeline (hover highlight)
  :bg [0.16 ...]            → 1 rect instance (28 floats)
  :text nil                 NO text emitted

:sidebar/back-btn           BOTH pipelines
  :bg [0.13 ...]            → 1 rect instance (28 floats)
  :text [{...} {...}]       → N glyph instances (12 floats each)
```

This avoids the current problem where changing a highlight rect forces
re-packing the parent's text ops (because they were stored together in
the same rt-node and walked together).

### Child Elements and Parent Propagation

Sub-elements (highlight, accent bar, indent guides) have `:parent`
pointing to their owning element. This serves three purposes:

**1. Position propagation.** When a parent scrolls or moves, all children
with `:parent` pointing to it must update their `:y` (or `:x`). In the
Electric model, the parent's position is a reactive signal; children
derive their absolute position from it. A scroll changes the parent's
`:y`, which triggers re-derivation of all children's `:y`.

**2. Clip inheritance.** Hit-testing walks the `:parent` chain to check
`:clip?`. The sidebar content area clips its children — entries scrolled
above or below the visible area are hit-test-invisible.

**3. Event bubbling.** When a click lands on an indent guide, that element has
no matching action descriptor, so the event bubbles to `:parent` → the file row
→ which exposes a `:click` action. This matches the current `dispatch-event` behavior in
rect-tree but without requiring a pre-computed tree path.

### Deriving Scene Elements from State (the Electric layer)

The sidebar's scene elements are derived from sidebar-state (an atom/PState):

```clojure
;; Pseudocode — actual implementation uses Electric e/for-by

(defn derive-sidebar-scene
  "Derive flat scene elements from sidebar state.
   Pure function: sidebar-state → {key → element}.
   In Electric, this becomes e/for-by on the flat-rows sequence."
  [sidebar-state current-file viewport-h scroll-y]
  (let [{:keys [project expanded-dirs dir-cache hovered-id]} sidebar-state
        flat-rows (flatten-file-tree ...)  ;; existing fn from sidebar.cljs
        base-y    48                        ;; below back-btn
        content-h (- viewport-h 48)]
    (-> {}
        ;; Frame elements (always present)
        (assoc :sidebar/root    (make-root-element viewport-h))
        (assoc :sidebar/border  (make-border-element viewport-h))
        (assoc :sidebar/back-btn (make-back-btn project))
        (assoc :sidebar/content (make-content-element base-y content-h))

        ;; File entries — THIS is what e/for-by iterates over
        (into
          (mapcat
            (fn [i {:keys [entry depth expanded? active?]}]
              (let [key-base (:path entry)
                    abs-y    (+ base-y (* i 32) (- scroll-y))
                    hovered? (= key-base hovered-id)]
                (cond-> [(make-entry-element key-base entry i abs-y depth
                                             hovered? active? expanded?)]
                  ;; Sub-elements: only present when state demands them
                  (or hovered? active?)
                  (conj (make-highlight-element key-base abs-y hovered? active?))

                  active?
                  (conj (make-accent-bar key-base abs-y))

                  (pos? depth)
                  (into (make-indent-guides key-base abs-y depth)))))
            (map-indexed vector flat-rows))))))
```

### Key Stability Under Expand/Collapse

Keys are semantic from v1:

- files use file-path keys
- directories use directory-path keys
- chrome uses role keys like `:sidebar/root`
- sub-elements derive from parent identity using stable suffixes

Examples:

- `"/project/src/main.cljs"`
- `"/project/src/main.cljs::hl"`
- `"/project/src/util.cljs::acc"`

Expand/collapse does not change those identities. It only adds or removes
elements whose underlying domain object entered or left the visible scene.

### Scale Implications

| Sidebar entries | Hit-test strategy | GPU writes on hover |
|-----------------|-------------------|---------------------|
| <100            | Linear scan       | 2-4 elements        |
| 100-500         | Linear scan       | 2-4 elements        |
| 500-1000        | Sorted z scan     | 2-4 elements        |
| >1000           | Spatial hash      | 2-4 elements        |

Note that GPU writes on hover are ALWAYS O(1) regardless of total elements —
only the old hovered element, the new hovered element, and their sub-elements
change. The hit-test cost scales, but the render cost does not.

---

## RAMA EVENT MODEL FOR UI

> **Status: DRAFT v1** -- Session 39 (2026-03-18)

---

### 1. EVENT TAXONOMY

Committed UI actions become Rama events. Events are categorized by domain,
but all share a common envelope:

```clojure
;; Universal event envelope
{:event/type    :workspace/dir-toggle   ;; namespaced keyword
 :event/id      #uuid "..."             ;; unique, generated at emission
 :event/ts      1710734400000           ;; js/Date.now at emission
 :event/session "session-abc"           ;; ties to user session
 :event/data    {...}}                  ;; domain-specific payload
```

Local-only actions like `[:local/set-hover ...]`, `[:local/scroll ...]`, and
pointer motion are NOT part of the Rama event taxonomy below. They stay client-local.

Six domains, each with concrete events:

#### 1a. Workspace events (sidebar, layout, panels)

```clojure
;; Sidebar click -- expand directory
[:workspace/dir-toggle {:path "/home/sid/projects/discourse-graph/src"
                        :action :expand}]    ;; or :collapse

;; Sidebar click -- select file (triggers file load)
[:workspace/file-select {:path "/home/sid/projects/discourse-graph/src/index.ts"
                         :project "/home/sid/projects/discourse-graph"}]

;; Panel visibility
[:workspace/panel-toggle {:panel :command     ;; :command | :settings | :sidebar
                          :visible true}]

;; Focus change
[:workspace/focus {:target :editor}]          ;; :editor | :command-panel | :chat | :settings-panel

;; Pane switch (3-pane mode)
[:workspace/active-pane {:pane :chat}]        ;; :editor | :chat | :preview

;; Viewport resize
[:workspace/resize {:width 1920
                    :height 1080
                    :dpr 2}]
```

#### 1b. Editor events (text editing, cursor, selection)

```clojure
;; Character insertion
[:editor/insert {:char "a"
                 :line 5 :col 12
                 :lines-snapshot-hash "abc123"}]   ;; for conflict detection

;; Backspace / Delete
[:editor/delete {:direction :backward     ;; :backward | :forward
                 :line 5 :col 12
                 :selection nil}]         ;; or {:start {:line 5 :col 3} :end {:line 5 :col 8}}

;; Enter (line split)
[:editor/newline {:line 5 :col 12}]

;; Paste
[:editor/paste {:text "pasted text\nsecond line"
                :line 5 :col 12
                :selection nil}]

;; Cursor movement (no text change -- still logged for trail replay)
[:editor/cursor-move {:direction :down    ;; :left :right :up :down :home :end :word-left :word-right
                      :from {:line 5 :col 12}
                      :to {:line 6 :col 12}}]

;; Selection via drag
[:editor/select {:start {:line 5 :col 3}
                 :end {:line 7 :col 15}}]

;; Copy (no state change, but logged for provenance)
[:editor/copy {:text "copied text"
               :range {:start {:line 5 :col 3} :end {:line 5 :col 15}}}]

;; Cut (state change + clipboard)
[:editor/cut {:text "cut text"
              :range {:start {:line 5 :col 3} :end {:line 5 :col 11}}}]

;; Fold toggle
[:editor/fold {:line 12 :action :fold}]   ;; or :unfold

;; SCI eval
[:editor/eval {:form "(+ 1 2)"
               :result "3"
               :line 5}]

;; Undo / Redo (higher-order: reversal of previous event batch)
[:editor/undo {:batch-id "batch-xyz"}]
[:editor/redo {:batch-id "batch-xyz"}]

;; File load (content replacement)
[:editor/file-load {:path "/src/index.ts"
                    :line-count 342
                    :target-line 0}]

;; File save
[:editor/file-save {:path "/src/index.ts"
                    :content-hash "sha256-..."}]
```

#### 1c. Agent events (AI runs, streaming, tool calls)

```clojure
;; Start agent run
[:agent/start {:run-id "run-abc"
               :provider :claude           ;; :claude | :gemini | :openai
               :prompt "explain this function"
               :cwd "/home/sid/projects/discourse-graph"
               :file "/src/index.ts"
               :context {:cursor {:line 5 :col 0}
                         :selection nil
                         :visible-range [0 1080]}}]

;; Streaming text delta (high frequency -- batched)
[:agent/text-delta {:run-id "run-abc"
                    :text "The function"
                    :block-idx 0}]

;; Thinking delta (extended thinking)
[:agent/thinking-delta {:run-id "run-abc"
                        :text "Let me analyze..."}]

;; Tool call start
[:agent/tool-start {:run-id "run-abc"
                    :tool-id "tool-xyz"
                    :tool-name "Read"
                    :block-idx 1}]

;; Tool input (streamed JSON chunks)
[:agent/tool-input {:run-id "run-abc"
                    :tool-id "tool-xyz"
                    :json-chunk "{\"file_path\":"}]

;; Tool result
[:agent/tool-result {:run-id "run-abc"
                     :tool-id "tool-xyz"
                     :content "file contents..."}]

;; Run complete
[:agent/complete {:run-id "run-abc"
                  :status :complete        ;; :complete | :failed | :cancelled
                  :session-id "sess-123"}]

;; Provider switch
[:agent/set-provider {:provider :gemini}]

;; Trail interaction -- collapse/expand a tool step
[:agent/trail-toggle {:collapse-id "tool-xyz"
                      :action :collapse}]

;; Trail navigation -- click file reference in trail
[:agent/trail-navigate {:file-path "/src/index.ts"
                        :line 42
                        :from-step "tool-xyz"}]
```

#### 1d. Design events (tokens, components, themes)

```clojure
;; Set a design token
[:design/set-token {:token-key :surface-base
                    :value [0.06 0.06 0.07 1.0]}]

;; Set component property
[:design/set-prop {:component "file-row"
                   :prop :height
                   :value 40}]

;; Swap theme
[:design/swap-theme {:theme-id :solarized-dark}]

;; Load plugin (theme, component library, or workflow)
[:design/load-plugin {:plugin-type :theme         ;; :theme | :components | :workflow
                      :spec {:tokens {...}
                             :components {...}}}]

;; Settings change (font, rendering)
[:design/settings {:setting :font-size
                   :value 16}]

[:design/font-select {:font-id "ubuntu-sans-mono"
                      :char-width 0.56
                      :name "Ubuntu Sans Mono"}]
```

#### 1e. Discourse events (Q->C->E->D->R->F protocol)

```clojure
;; Create a claim
[:discourse/create-claim {:claim-id "claim-abc"
                          :text "React hooks solve state management"
                          :confidence :hypothesis    ;; :fact | :hypothesis | :guess
                          :source {:type :manual}}]

;; Link evidence to claim
[:discourse/link-evidence {:evidence-id "ev-xyz"
                           :claim-id "claim-abc"
                           :relation :supports       ;; :supports | :contradicts | :qualifies
                           :content "https://..."
                           :source-type :url}]

;; Resolve a question
[:discourse/resolve {:question-id "q-123"
                     :resolution :claim-abc
                     :confidence :established}]

;; DG workflow commands (flow canvas)
[:discourse/flow-bootstrap {:cwd "/home/sid/projects/discourse-graph"}]
[:discourse/flow-select {:ticket-indices [0 2 5]}]
[:discourse/flow-arrange {}]
[:discourse/flow-run {:action :review}]
```

#### 1f. Navigation events (committed navigation)

```clojure
;; Goto (jump to a specific location)
[:nav/goto {:target {:type :file :path "/src/index.ts" :line 42}}]
[:nav/goto {:target {:type :claim :id "claim-abc"}}]
[:nav/goto {:target {:type :trail :run-id "run-abc" :step 7}}]
[:nav/goto {:target {:type :zoom-level :zoom 0.1}}]
```

Camera pan/zoom/scroll are usually client-local attention state. They become
Rama events only when navigation provenance itself is being recorded.

---

### 2. PSTATE SCHEMAS

PStates are materialized views that serve the UI. Each is derived from the
event log by a Rama topology. The UI never reads the event log directly --
it watches PStates via `e/watch`.

#### 2a. $$workspace -- what's visible, focused, expanded

```clojure
;; $$workspace PState schema
;; Keyed by session-id (multi-user ready)
{:expanded-dirs   #{"/src" "/src/app"}                ;; sidebar expansion state
 :selected-file   {:path "/src/index.ts"
                   :name "index.ts"}                  ;; currently open file (nil = none)
 :project         {:name "discourse-graph"
                   :path "/home/sid/projects/discourse-graph"}
 :focus           :editor                             ;; :editor | :command-panel | :chat | :settings-panel
 :active-pane     :editor                             ;; 3-pane: :editor | :chat | :preview
 :panels          {:command  {:visible true :text "" :cursor 0}
                   :settings {:visible false}
                   :sidebar  {:visible true}}}
```

#### 2b. $$files -- directory tree, file contents

```clojure
;; $$files PState schema
;; Server-side, served differentially to client
{:home-dirs    [{:name "discourse-graph"
                 :path "/home/sid/projects/discourse-graph"}
                {:name "Softland"
                 :path "/mnt/data/projects/Softland"}]

 :dir-cache    {"/home/sid/projects/discourse-graph"
                [{:name "src" :path "/home/sid/projects/discourse-graph/src" :type :dir}
                 {:name "README.md" :path "/home/sid/projects/discourse-graph/README.md" :type :file}]

                "/home/sid/projects/discourse-graph/src"
                [{:name "index.ts" :path "..." :type :file}]}

 :file-contents {"/src/index.ts" {:lines ["line 1" "line 2" ...]
                                   :loaded-at 1710734400000
                                   :hash "sha256-..."}}}
```

#### 2c. $$editor -- document state, cursor, undo history

```clojure
;; $$editor PState schema
;; Per-file editor state. Keyed by file path.
{"/src/index.ts"
 {:lines       ["(ns app.core)" "(defn main []" "  (println \"hello\"))" ""]
  :cursor      {:line 1 :col 5}
  :selection   nil                          ;; or {:start {:line ..} :end {:line ..}}
  :desired-col 5
  :folded      #{12 45}                     ;; folded line numbers
  :undo-stack  [{:lines [...] :cursor {:line 0 :col 0}} ...]   ;; max 100
  :redo-stack  []}}
```

#### 2d. $$agents -- runs, status, trails

```clojure
;; $$agents PState schema
;; Keyed by run-id
{:runs
 {"run-abc"
  {:run-id      "run-abc"
   :provider    :claude
   :prompt      "explain this function"
   :status      :running              ;; :running | :complete | :failed | :cancelled
   :started-at  1710734400000
   :session-id  "sess-123"
   :output      "The function computes..."     ;; accumulated text
   :trail       [{:kind :reasoning :text "The function"}
                 {:kind :tool-call-start :tool-name "Read" :tool-id "tool-xyz"}
                 {:kind :tool-call :tool-name "Read" :tool-id "tool-xyz"
                  :input {:file_path "/src/index.ts"}}
                 {:kind :tool-result :tool-id "tool-xyz" :content "..."}
                 {:kind :reasoning :text " computes..."}]
   :tool-buf    {"tool-xyz" {:tool-name "Read" :json "" :block-idx 1}}}}

 :active-run   "run-abc"             ;; which run is currently displayed
 :provider     :claude               ;; current provider preference
 :collapsed    #{"tool-xyz"}}        ;; which trail steps are collapsed
```

#### 2e. $$design -- tokens, components, theme, settings

```clojure
;; $$design PState schema
{:tokens
 {:surface-base       [0.06 0.06 0.07 1.0]
  :surface-raised     [0.10 0.10 0.11 1.0]
  :text-primary       [0.90 0.90 0.90 1.0]
  :text-secondary     [0.55 0.55 0.55 1.0]
  :accent-blue        [0.30 0.55 0.95 1.0]
  :border-subtle      [0.20 0.20 0.22 1.0]}

 :components
 {"sidebar"    {:width 256 :bg :surface-base
                :item-height 32 :padding-x 16}
  "file-row"   {:height 32 :padding-x 16
                :states {:normal   {:bg :surface-base :text :text-primary}
                         :hovered  {:bg :surface-raised :text :text-primary}
                         :selected {:bg :accent-blue :text :text-primary}}}
  "cmd-panel"  {:height 36 :bg :surface-raised}
  "status-bar" {:height 24 :bg :surface-base}}

 :settings
 {:font-id          "dejavu-sans-mono"
  :font-size        19
  :line-height      1.2
  :px-range         8
  :sharpness        0.0
  :snap-to-pixel?   true
  :show-diagnostics? false
  :theme-id         :default}

 :fonts
 [{:id "dejavu-sans-mono" :name "DejaVu Sans Mono"
   :char-width 0.56 :default true}]}
```

#### 2f. $$discourse -- claims, evidence, links, workflow state

```clojure
;; $$discourse PState schema
{:claims
 {"claim-abc" {:id "claim-abc"
               :text "React hooks solve state management"
               :confidence :hypothesis
               :created-at 1710734400000
               :evidence ["ev-xyz"]}}

 :evidence
 {"ev-xyz" {:id "ev-xyz"
            :content "https://react.dev/reference/react/useState"
            :source-type :url
            :claims {"claim-abc" :supports}}}

 :questions
 {"q-123" {:id "q-123"
           :text "How should we manage state?"
           :status :open              ;; :open | :resolved
           :resolution nil}}

 :flow-state
 {:stage :idle                        ;; :idle | :intake | :selecting | :arranging | :running | :reviewing
  :tickets [{:idx 0 :title "Fix bug" :status :todo :id "LINEAR-123"} ...]
  :selected [0 2]
  :session-id "sess-123"
  :collapsed-groups #{}}}
```

---

### 3. COMMITMENT BOUNDARY

The commitment boundary decides which events auto-commit and which need review.

```
raw signal --> interpretation --> proposal --> commit? --> Rama depot
                     |                            |
                context:              +-----------+-----------+
                what's focused,       |                       |
                what's hovered,     AUTO-COMMIT:         HOLD-FOR-REVIEW:
                what's visible      low-risk,            high-risk,
                                    reversible,          irreversible,
                                    high-frequency       or ambiguous
```

#### 3a. Auto-commit events (committed, low-risk)

These events go straight to the depot with no review step.
They are committed state transitions, not client-local attention updates.

```clojure
;; AUTO-COMMIT: fire and forget
#{:workspace/focus           ;; which pane has focus
  :workspace/active-pane     ;; 3-pane tab switch
  :workspace/panel-toggle    ;; open/close command panel, settings
  :workspace/file-select     ;; open/select file
  :workspace/dir-toggle      ;; expand/collapse sidebar directory

  :editor/fold               ;; toggle fold
  :editor/eval               ;; SCI eval (read-only side effect)

  :agent/text-delta          ;; streaming tokens (server-originated)
  :agent/thinking-delta      ;; extended thinking
  :agent/tool-input          ;; tool JSON chunks
  :agent/tool-result         ;; tool output
  :agent/complete            ;; run finished
  :agent/trail-toggle        ;; collapse/expand step
  :agent/trail-navigate      ;; click file link in trail

  :design/settings           ;; font size, line height adjustments
  :design/font-select        ;; font picker
  :design/swap-theme}        ;; theme toggle
```

#### 3b. Review-required events (higher risk, less reversible)

These events produce a PROPOSAL that can be inspected before committing.
The user (or a policy) decides to accept or reject.

```clojure
;; REVIEW-REQUIRED: proposal -> accept/reject
#{:editor/file-save          ;; writing to disk -- irreversible without git
  :editor/file-load          ;; replaces editor content

  :agent/start               ;; starts an API call, costs money, takes time
  :agent/set-provider        ;; changes AI provider

  :design/set-token          ;; changes visual design for everyone
  :design/set-prop           ;; changes component spec
  :design/load-plugin        ;; loads external code

  :discourse/create-claim    ;; adds to knowledge graph
  :discourse/link-evidence   ;; modifies knowledge structure
  :discourse/resolve         ;; closes a question

  :discourse/flow-bootstrap  ;; starts a workflow
  :discourse/flow-run}       ;; fires an agent with real side effects
```

#### 3c. Proposal data shape

```clojure
;; A proposal wraps an event with review metadata
{:proposal/id       #uuid "..."
 :proposal/event    [:editor/file-save {:path "/src/index.ts" :content-hash "sha256-..."}]
 :proposal/status   :pending           ;; :pending | :accepted | :rejected
 :proposal/risk     :medium            ;; :low | :medium | :high
 :proposal/created  1710734400000
 :proposal/reason   "File save writes to disk. Undo requires git."
 :proposal/preview  {:lines-changed 3  ;; what would change
                     :diff "..."}}
```

#### 3d. Auto-commit policy (configurable)

```clojure
;; Policy: maps event types to commitment behavior
;; Users can promote review-required events to auto-commit (and vice versa)
(def default-commit-policy
  {:auto-commit
   #{:workspace/file-select :workspace/dir-toggle
     :workspace/focus :workspace/active-pane :workspace/panel-toggle
     :editor/fold :editor/eval
     :nav/goto
     :agent/text-delta :agent/thinking-delta :agent/tool-input
     :agent/tool-result :agent/complete :agent/trail-toggle
     :agent/trail-navigate :design/settings :design/font-select
     :design/swap-theme}

   :review-required
   #{:editor/file-save :editor/file-load
     :agent/start :agent/set-provider
     :design/set-token :design/set-prop :design/load-plugin
     :discourse/*}

   ;; Events below this frequency threshold are batched
   ;; (e.g., 100 text-delta events become one batch commit)
   :batch-threshold-ms 100})
```

#### 3e. Undo and collaboration honesty

Single-user undo via event reversal / replay is tractable.
Multi-user text collaboration is NOT settled architecture here.

For now the honest statement is:

- structural operations can plausibly ride on event ordering
- text editing likely needs CRDT/OT or another dedicated merge design
- the specific collaboration model is TBD

So this document does NOT claim that append-only ordering alone solves multi-user editing.

---

### 4. EVENT BATCHING (high-frequency events)

Some events fire at extreme rates (hover: 60Hz, text-delta: ~100/sec).
These are batched before committing to the depot:

```clojure
;; Client-side batching before depot append
(defn batch-events [events]
  (let [groups (group-by #(namespace (first %)) events)]
    ;; Hover: keep only latest
    ;; Text-delta: concatenate text
    ;; Cursor-move: keep only final position
    ;; Everything else: pass through as-is
    ...))

;; Example: 60 local hover updates in one second become one client-local batch:
[:local/hover-batch {:positions [{:element-key "/src/main.cljs" :ts 1710...}
                                 {:element-key "/src/util.cljs" :ts 1710...}
                                 ...last-one-wins...]}]
```

For the initial implementation, high-frequency ephemeral events (hover, mouse-move)
can skip the depot entirely and only update a client-local atom. They become Rama
events only when provenance matters (e.g., recording a user study session).

---

### 5. MIGRATION FROM ATOMS TO PSTATES

Maps each current atom to its target PState. Some atoms stay as client-local atoms
because they are ephemeral rendering state that Rama does not need to know about.

```
CURRENT ATOM               | TARGET                          | NOTES
---------------------------|-------------------------------- |------------------
                           |                                 |
SIDEBAR / WORKSPACE        |                                 |
!sidebar-state             | $$workspace + $$files           | split
  .expanded-dirs           |   $$workspace.expanded-dirs     |
  .dir-cache               |   $$files.dir-cache             |
  .project                 |   $$workspace.project           |
  .hovered-id              |   CLIENT-LOCAL                  | ephemeral
  .scroll-y                |   CLIENT-LOCAL                  | ephemeral
  .home-dirs               |   $$files.home-dirs             |
  .loading?                |   DROP                          | derive from PState
!current-file              | $$workspace.selected-file       |
!sidebar-visible           | $$workspace.panels.sidebar      |
                           |                                 |
EDITOR                     |                                 |
!editor-doc                | $$editor[path]                  | per-file keyed
  .lines                   |   $$editor[path].lines          |
  .cursor                  |   $$editor[path].cursor         |
  .selection               |   $$editor[path].selection      |
  .desired-col             |   $$editor[path].desired-col    |
!folded-lines              | $$editor[path].folded           |
!undo-stack                | $$editor[path].undo-stack       | or: derive from log
!redo-stack                | $$editor[path].redo-stack       | or: derive from log
!clipboard                 | CLIENT-LOCAL                    | browser clipboard
!scroll-y                  | CLIENT-LOCAL                    | ephemeral viewport
!scroll-x                  | CLIENT-LOCAL                    | ephemeral viewport
!eval-result               | CLIENT-LOCAL (transient)        | 5sec TTL display
!file-load-request         | DROP                            | becomes Rama event
                           |                                 |
FOCUS / UI STATE           |                                 |
!focus                     | $$workspace.focus               |
!active-pane               | $$workspace.active-pane         |
!cmd-panel                 | $$workspace.panels.command      |
!caret-visible             | CLIENT-LOCAL                    | blink timer
!dragging?                 | CLIENT-LOCAL                    | drag gesture state
!drag-start                | CLIENT-LOCAL                    | drag gesture state
                           |                                 |
SETTINGS / FONT            |                                 |
!settings                  | $$design.settings               |
!active-font               | $$design (derived from fonts)   |
!font-manifest             | $$design.fonts                  |
!font-assets               | CLIENT-LOCAL                    | GPU atlas reference
                           |                                 |
AGENT / AI                 |                                 |
!agent-output              | $$agents.runs[run-id]           |
!ai-provider               | $$agents.provider               |
!agent-scroll-y            | CLIENT-LOCAL                    | ephemeral
!chat-scroll-y             | CLIENT-LOCAL                    | ephemeral
!chat-input                | CLIENT-LOCAL                    | in-flight text
!trail-collapsed           | $$agents.collapsed              |
!shimmer-phase             | CLIENT-LOCAL                    | animation state
                           |                                 |
WORKFLOW                   |                                 |
!flow-state                | $$discourse.flow-state          |
!collapsed-groups          | $$discourse.flow-state          | .collapsed-groups
!hovered-row-idx           | CLIENT-LOCAL                    | ephemeral hover
!drag-state                | CLIENT-LOCAL                    | gesture state
!extract-preview           | CLIENT-LOCAL                    | transient UI
                           |                                 |
GPU STATE                  |                                 |
!text-geo                  | CLIENT-LOCAL                    | GPU pipeline state
!editor-rect-sys           | CLIENT-LOCAL                    | GPU pipeline state
!cmd-rect-sys              | CLIENT-LOCAL                    | GPU pipeline state
!settings-rect-sys         | CLIENT-LOCAL                    | GPU pipeline state
!shadow-sys                | CLIENT-LOCAL                    | GPU pipeline state
!sidebar-pool              | CLIENT-LOCAL                    | GPU buffer pool
                           |                                 |
MOUSE                      |                                 |
!mouse-x                   | CLIENT-LOCAL                    | 60Hz ephemeral
!mouse-y                   | CLIENT-LOCAL                    | 60Hz ephemeral
!viewport                  | CLIENT-LOCAL                    | window dimensions
                           |                                 |
EXTERNAL                   |                                 |
!preview-el                | CLIENT-LOCAL                    | DOM reference

SUMMARY:
  Migrate to PStates:  ~25 atoms (ground truth, shared state)
  Keep CLIENT-LOCAL:   ~20 atoms (ephemeral, GPU, gesture, viewport)
  DROP:                ~3 atoms  (replaced by Rama events / derivation)

  Rule of thumb:
    Would another session/user/replay need this value?  -> PState
    Is it pure rendering/gesture plumbing?              -> CLIENT-LOCAL
    Is it a request trigger that becomes an event?      -> DROP
```

---

### 6. TOPOLOGY SKETCHES

How Rama topologies process events into PStates:

```clojure
;; Workspace topology -- processes workspace events into $$workspace
(deframatopology workspace-topology [$$workspace]
  (<<sources
    (source> *workspace-depot :> %event)
    (let [type (first %event)
          data (second %event)]
      (case type
        :workspace/dir-toggle
        (<<pstate $$workspace
          (if (= (:action data) :expand)
            (update :expanded-dirs conj (:path data))
            (update :expanded-dirs disj (:path data))))

        :workspace/file-select
        (<<pstate $$workspace
          (assoc :selected-file {:path (:path data)
                                 :name (last (str/split (:path data) #"/"))}))

        :workspace/focus
        (<<pstate $$workspace
          (assoc :focus (:target data)))

        :workspace/panel-toggle
        (<<pstate $$workspace
          (assoc-in [:panels (:panel data) :visible] (:visible data)))

        ;; ... other workspace events
        nil))))

;; Editor topology -- processes editor events into $$editor
(deframatopology editor-topology [$$editor]
  (<<sources
    (source> *editor-depot :> %event)
    (let [type (first %event)
          data (second %event)
          path (:file-path data)]  ;; events carry which file they apply to
      (case type
        :editor/insert
        (<<pstate $$editor path
          (-> (update-in [:lines (:line data)]
                         #(str (subs % 0 (:col data)) (:char data) (subs % (:col data))))
              (assoc :cursor {:line (:line data) :col (inc (:col data))})))

        :editor/undo
        ;; ... replay without reversed events
        ;; ... or maintain undo stack in PState
        nil))))
```

Note: these are SKETCHES, not runnable Rama code. The actual topology DSL
depends on Rama version and will need adaptation. The key insight is that
each event type maps to a specific PState transformation.

---

### 7. CLIENT-SIDE EVENT EMISSION PATTERN

How the current handler code transforms into event emitters:

```clojure
;; CURRENT (mouse.cljs line ~189):
(swap! !sidebar-state update :expanded-dirs
  (fn [dirs] (if (contains? dirs path) (disj dirs path) (conj dirs path))))
(fetch-dir! path)

;; TARGET:
(emit! [:workspace/dir-toggle {:path path
                                :action (if (contains? @expanded-dirs path)
                                          :collapse :expand)}])
;; Rama topology processes the event -> $$workspace updates
;; Electric watches $$workspace -> sidebar re-renders
;; Filesystem loading still needs explicit transport, but truth moves into $$files
;; rather than bouncing through ad hoc REST-style sidebar fetch state
```

```clojure
;; CURRENT (keyboard.cljs line ~148):
(let [new-doc (editor-apply-event doc event lengths @!clipboard)]
  (reset! !editor-doc new-doc)
  (reset! !caret-visible true))

;; TARGET:
(emit! [:editor/insert {:char (:char event)
                         :line (:line cursor)
                         :col (:col cursor)
                         :file-path @current-file-path}])
;; !caret-visible stays CLIENT-LOCAL (blink timer, not world state)
```

```clojure
;; CURRENT (agent_flow.cljs line ~261):
(reset! !agent-output {:status :running :provider provider ...})
(stream-agent-run! "/api/agent/stream" request-body handler err-handler)

;; TARGET:
(emit! [:agent/start {:run-id (str (random-uuid))
                       :provider provider
                       :prompt prompt
                       :cwd cwd
                       :file file-path
                       :context context}])
;; Server topology receives the event, starts the agent run,
;; streams results back as [:agent/text-delta ...] events
;; Electric watches $$agents PState -> trail renders incrementally
;; Explicit transport to the external process still exists; this only reduces
;; app-internal plumbing
```

The `emit!` function is the universal entry point:

```clojure
(defn emit!
  "Emit a UI event. Routes through commitment boundary, then to Rama depot."
  [event]
  (let [[type data] event
        policy (get-commit-policy type)]
    (case policy
      :auto-commit   (depot-append! event)
      :review        (create-proposal! event)
      :client-local  (apply-locally! event)    ;; for events that never reach Rama
      :batch         (enqueue-for-batch! event))))
```

---

## GAP ANALYSIS: CURRENT -> TARGET

> **Status: DRAFT v1** -- Session 39 (2026-03-18)

Maps each existing source file to its role in the target architecture.

```
CURRENT FILE                           | TARGET ROLE
---------------------------------------|---------------------------------------
                                       |
runtime/state.cljs                     | SPLIT:
  make-runtime-state (all atoms)       |   PState schemas (server topology)
                                       |   + client-local-atoms fn (reduced)
                                       |   + init-pstates fn (server)
                                       |
runtime/mouse.cljs                     | THIN EMITTERS:
  handle-mousedown! (coord->atom)      |   mousedown -> emit! event
  handle-mousemove! (hover->atom)      |   mousemove -> client-local only
  handle-mouseup! (drag->atom)         |   mouseup -> emit! if meaningful
  handle-sidebar-click! (tree->atom)   |   click -> emit! [:workspace/...]
  handle-editor-click! (cursor->atom)  |   click -> emit! [:editor/...]
  handle-chat-click! (nav->atom)       |   click -> emit! [:agent/trail-nav]
                                       |   Hit-testing STAYS (GPU-local)
                                       |   Coord conversion STAYS (GPU-local)
                                       |
runtime/keyboard.cljs                  | THIN EMITTERS:
  editor-keys-consumer (key->atom)     |   key -> emit! [:editor/...]
  command-keys-consumer (key->atom)    |   key -> emit! [:workspace/...]
  chat-keys-consumer (key->atom)       |   key -> emit! [:agent/...]
  settings-keys-consumer (key->atom)   |   key -> emit! [:design/...]
  global-keys-consumer (key->atom)     |   key -> emit! [:workspace/...]
  file-load-consumer (watch->atom)     |   DROP -- event replaces watch
                                       |
runtime/scroll.cljs                    | CLIENT-LOCAL:
  scroll-consumer (wheel->atom)        |   Sidebar scroll -> CLIENT-LOCAL
                                       |   Editor scroll -> CLIENT-LOCAL
                                       |   Agent scroll -> CLIENT-LOCAL
                                       |
runtime/sidebar_io.cljs                | REWORK:
  make-sidebar-io (HTTP fetch)         |   Truth moves to $$files PState
  fetch-dir! fetch-file!               |   explicit filesystem transport remains
                                       |   REST round-trips disappear, not transport itself
                                       |
runtime/agent_flow.cljs                | SPLIT:
  make-event-handler (SSE->atom)       |   Server topology ingests events
  submit-agent-run! (HTTP->atom)       |   emit! [:agent/start ...] only
  fire-flow-run! (HTTP->atom)          |   emit! [:discourse/flow-run ...]
  auto-scroll-agent! (atom->atom)      |   CLIENT-LOCAL (scroll tracking)
                                       |
runtime/render.cljs                    | REFACTOR:
  render-consumer (atoms->GPU)         |   e/for-by mount callbacks
                                       |   Snapshot rebuild -> differential
                                       |   m/sample RAF -> dirty-present
                                       |
runtime/fonts.cljs                     | KEEP (client-local):
  Font loading, atlas management       |   Pure GPU concern, no PState needed
                                       |
runtime/interop.cljs                   | REDUCE:
  SCI eval, dev replay                 |   SCI stays. Replay becomes built-in
                                       |   (Rama event replay = native)
                                       |
substrate/webgpu/renderer.cljs         | KEEP (terminal):
  GPU pipeline, draw calls             |   Receives diffs from Electric mount
  Buffer management                    |   callbacks instead of full snapshots
                                       |
substrate/webgpu/buffer_pool.cljs      | KEEP + EXTEND:
  Buffer pool allocator                |   Gets mount callbacks wired in
                                       |   (append-child -> allocate-slot etc)
                                       |
electric_flow.cljc                     | EXTEND:
  Electric UI (current)                |   Add e/server PState watches
                                       |   Add e/for-by differential rendering
                                       |   Remove HTTP fetch code
                                       |
events.cljs                            | THIN DOWN:
  Event classification, key mapping    |   Keep: DOM event -> classified event
                                       |   Remove: all atom-mutation logic
                                       |
sidebar.cljs, shell.cljs, trail.cljs   | REFACTOR:
  Tree building, layout computation    |   Pure functions stay (tree->rects)
                                       |   Data comes from PState not atoms
                                       |
themes.cljs                            | MOVE INTO $$design:
  Theme definitions                    |   Theme data -> $$design.tokens
                                       |   Token lookup -> design PState query
                                       |
agent.cljs                             | REWORK / POSSIBLY DROP:
  stream-agent-run! (HTTP SSE)         |   explicit external-process transport still needed
                                       |   client-side SSE parsing may disappear later, not assumed here
                                       |
workflows/dg_flow.cljs                 | SPLIT:
  DG flow state machine                |   State machine -> Rama topology
                                       |   Parse commands -> event emission
                                       |   Layout -> stays (pure function)
                                       |
workflows/jit.cljs                     | SPLIT:
  JIT component extraction             |   Commands -> Rama events
                                       |   Extraction logic -> server topology

SUMMARY BY ACTION:
  DROP entirely:      sidebar_io.cljs, agent.cljs (client SSE)
  THIN to emitters:   mouse.cljs, keyboard.cljs, events.cljs
  SPLIT (server+cl):  state.cljs, agent_flow.cljs, dg_flow.cljs, jit.cljs
  REFACTOR in place:  render.cljs, electric_flow.cljc, sidebar/shell/trail
  KEEP as-is:         renderer.cljs, buffer_pool.cljs, fonts.cljs
  MOVE to PState:     themes.cljs -> $$design

  New files needed:
  server/topologies/workspace.clj   -- $$workspace topology
  server/topologies/editor.clj      -- $$editor topology
  server/topologies/agents.clj      -- $$agents topology
  server/topologies/design.clj      -- $$design topology
  server/topologies/discourse.clj   -- $$discourse topology
  client/emit.cljs                  -- emit! + commitment boundary + batching
```

---

## The Subsystems (zoom 100)

```
┌──────────────────────────────────────────────────────────────┐
│ ┌──────────┐ ┌────────────────────────────────────────────┐  │
│ │ SIDEBAR  │ │ MAIN CONTENT                              │  │
│ │          │ │                                            │  │
│ │ files    │ │ MODE: editor | file-view | flow-canvas |  │  │
│ │ dirs     │ │       intake | run | settings             │  │
│ │          │ │                                            │  │
│ │          │ │ ┌─ EDITOR ──────┬─ RIGHT PANE ─────────┐ │  │
│ │          │ │ │ syntax hl     │ agent trail           │ │  │
│ │          │ │ │ cursor/sel    │ chat input            │ │  │
│ │          │ │ │ fold/bracket  │ reasoning steps       │ │  │
│ │          │ │ │ SCI eval      │                       │ │  │
│ │          │ │ └───────────────┴───────────────────────┘ │  │
│ │          │ │                                            │  │
│ │          │ │ ┌─ FLOW CANVAS ─────────────────────────┐ │  │
│ │          │ │ │ workflow nodes, tickets, drag-drop    │ │  │
│ │          │ │ └──────────────────────────────────────┘ │  │
│ └──────────┘ └────────────────────────────────────────────┘  │
│ ┌─ CMD PANEL ──────────────────────────────────────────────┐ │
│ │ command input    agent status    provider selector       │ │
│ ├──────────────────────────────────────────────────────────┤ │
│ │ STATUS BAR                                               │ │
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

---

## Plugin / Theme / Design System

```
theme plugin        component library      workflow plugin
(token map)         (component specs)      (new node types)
      │                    │                      │
      └────────────────────┼──────────────────────┘
                           │
                           ▼
                  Rama event: [:plugin/load {...}]
                           │
                           ▼
                  Design PState deep-merged
                           │
                           ▼
                  Electric re-derives everything.
                  New theme = one frame.
```

---

## Agent System

```
prompt → [:agent/start {:prompt ... :provider :claude}]
       → server SSE stream
       → PState updates incrementally (streaming tokens)
       → Electric e/for-by on output blocks
       → each block = trail step (reasoning trail)
       → click step → navigate to referenced code
       → trail IS the knowledge artifact (saveable, forkable, queryable)
       → at zoom 0.1, trails ARE the content
```

---

## Self-Modification Loop

```
zoom 50: designer edits sidebar row height in design tool
       → event: [:design/set-prop "file-row" :height 40]
zoom 100: sidebar re-renders with h=40 (one frame)
zoom 50: designer sees the change live

The design tool IS Softland at a different zoom level.
One world. One truth. Many projections.
```

---

## Migration Path

### Sequencing discipline: Rama-first, by slice

Not "Rama everything first" — that's too big. Not "flatten first, Rama later" — that
builds on the wrong foundation. Instead:

```
pick one slice
  → define its Rama truth (events + PState)
  → define its commitment boundary
  → watch it in Electric
  → derive the flat keyed scene from that truth
  → mount to GPU + interaction
  → then pick the next slice
```

### The editor is a special case

Sidebar, design preview, flow canvas — these can be committed-first (events → Rama → PState).
Editor text entry CANNOT. Keystrokes are too high-frequency for per-event Rama commits.

Editor policy: **optimistic-local first.** Keystrokes apply to a local atom instantly.
Batched commit to Rama on save, blur, or periodic interval. Conflict resolution between
local edits and server-pushed changes is a separate design problem (CRDT/OT territory).

### Transport honesty

Electric reduces custom API plumbing for reactive state transfer. But explicit transport
is still needed for: filesystem I/O (server reads files), agent SSE streaming (external
process), CLI integration. "No API endpoints" is too strong — the reduction is real but
not total.

### First slice: sidebar (Rama-informed)

| Step | What | Status |
|------|------|--------|
| 0 | Buffer pool, sidebar extraction (GPU substrate) | DONE (S39) |
| 1 | Define sidebar Rama events + PState | NOT STARTED |
| 2 | Wire sidebar handlers to emit events (not atom swaps) | NOT STARTED |
| 3 | Electric watches sidebar PState | NOT STARTED |
| 4 | Derive flat keyed scene from PState (semantic keys, action descriptors) | NOT STARTED |
| 5 | e/for-by manages pool slots from scene | NOT STARTED |
| 6 | Hit-testing via scene element bounds (not tree reconstruction) | NOT STARTED |

### After sidebar proves the pattern

| Step | What |
|------|------|
| 7 | Design spec as Rama PState (tokens, component specs) |
| 8 | Component preview slice (full design-artifact thesis) |
| 9 | Agent output / trail slice |
| 10 | Flow canvas slice |
| 11 | Spatial index for >1000 elements |
| 12 | ID buffer for >10,000 elements |

### Exploration inventory (NOT settled, for future reference)

The event taxonomy (51 events), PState schemas (6 domains), and atom-to-PState mapping
documented in the sections below are **exploration notes from parallel agents**, not
committed architecture. Use them as starting points when designing each slice's truth
model. Expect them to change as implementation reveals the actual shapes needed.
