# Imported Topology View — Design Research

> First read-only orientation surface for imported static material in Softland.
> Status: design research / specification. Read-only V1. No editing, no
> accept/reject, no semantic search, no canvas authoring, no full-world nav.

This document is grounded in the **actual** Softland codebase, not a generic SaaS
sketch:

- The world kernel is event-sourced (`src/app/server/rama/core.clj`):
  `ActionRequest → Decision → KernelEvent → Materialization (PStates) → Projection`.
- The UI substrate is a **WebGPU retained scene graph** (`rt-node` rect-trees) with a
  Linear/shadcn-inspired dark design system already built
  (`src/app/client/workspace/ui_primitives.cljs`, `src/components/design_tokens.cljc`).
- The aesthetic is IDE/terminal, not dashboard: gruvbox/rosé-pine themes, monospace,
  2px accent focus bars, depth-via-surface, ASCII glyphs in empty states.

The single most important finding: **the kernel already encodes the brief's hardest
requirements.** The accepted-vs-candidate distinction, provenance, lineage, actors, and
source anchors are not features this view must invent — they are PStates this view must
*project*. That makes the Imported Topology View a faithful instance of the Softland
principle "views are projections over shared objects, not separate copies of the world."

---

## Contents

1. [Product interpretation of Softland in design terms](#1-product-interpretation)
2. [Design principles for the Imported Topology View](#2-design-principles)
3. [Information architecture](#3-information-architecture)
4. [First screen layout](#4-first-screen-layout)
5. [Source / silo inventory design](#5-source--silo-inventory-left-rail)
6. [Object / topology browser design](#6-object--topology-browser-center)
7. [Selected-object inspector design](#7-selected-object-inspector-right)
8. [Raw source vs Softland-native representation](#8-raw-source-vs-softland-native-representation)
9. [Local graph / neighborhood view](#9-local-graph--neighborhood-view)
10. [Timeline / history / provenance view](#10-timeline--history--provenance-view)
11. [Empty / loading / error states](#11-empty--loading--error-states)
12. [Interaction model](#12-interaction-model)
13. [What must be clickable](#13-what-must-be-clickable)
14. [What must never be hidden](#14-what-must-never-be-hidden)
15. [What should stay folded / compressed](#15-what-should-stay-folded--compressed)
16. [Visual references and patterns to borrow](#16-visual-references-and-patterns-to-borrow)
17. [Anti-patterns to avoid](#17-anti-patterns-to-avoid)
18. [Assumptions required from the Object-Container Kernel](#18-assumptions-required-from-the-kernel)
- [Appendix A: Kernel → View mapping](#appendix-a-kernel--view-mapping)
- [Appendix B: Status & provenance visual legend](#appendix-b-status--provenance-visual-legend)
- [Appendix C: Two worked examples](#appendix-c-two-worked-examples)

---

## 1. Product interpretation

**Softland is software as place; the Imported Topology View is its port of entry.**

The README frames the long arc: "We have Google Earth to explore any part of the world,
but we don't have anything like that for knowledge… Time travel should be possible…
I should be able to ask *how we got here?*" The Imported Topology View is the first
honest, walkable step toward that: it is the **arrivals hall** of the world — the place
where scattered outside material (markdown, transcripts, code, Roam, Linear) has just
landed and is being made into terrain you can inspect, trust, revisit, and later
transform.

The job-to-be-done is not "browse files." It is **orientation and trust**:

> *Something was imported into my world. What is it, where did it come from, what did
> Softland make of it, what is already connected, what is still loose, and can I believe
> any of it?*

In design terms, the product center — "a world for holding understanding in public
form" — translates here into three commitments:

- **Recoverable compression.** Show the skeleton so another mind can enter without
  privately reconstructing everything: silos, types, counts, connectivity, what's
  accepted vs guessed, what's disconnected. Keep the detail (every byte, every revision,
  every event) *folded but one hop away*. Compression that cannot be expanded is lossy
  lying; this view never does that.
- **Provenance as terrain, not metadata.** The "where did this come from" chain
  (native object → source anchor → raw artifact → originating event → actor) is not a
  tooltip afterthought. It is the primary structure of the place. You navigate
  *by* provenance.
- **Epistemic honesty about status.** The world contains preserved raw source, durable
  native objects, machine-distilled spans, and candidate guesses. These are *different
  kinds of truth* and must look different. The kernel already separates them
  (`unit-statuses #{:accepted :rejected :hidden :promoted :superseded}` + `:unjudged`,
  and distinct `read-canonical-view` / `read-discarded-view`). The view's job is to make
  that separation *visible and never collapsible by accident*.

This is "part IDE/project explorer, part data-catalog/lineage browser, part DevTools
source inspector, part Figma layers+inspector." It is **not** a marketing dashboard, a
generic file manager, or a decorative graph canvas. It is a serious epistemic workspace
whose first virtue is that you can trust what it tells you, and verify it in one click.

---

## 2. Design principles

These specialize the global Softland principles for *this* read-only view.

1. **Orientation before manipulation.** V1 is a map, not a control panel. Every
   affordance navigates, selects, reveals, or filters. Nothing mutates world truth.
   (The kernel boundary — `*world-requests-depot` — is simply not touched.)

2. **Overview first, zoom and filter, details on demand.** Shneiderman's mantra is the
   literal spine. The screen opens at world/import scope (census), descends to silo →
   artifact → object, and reveals raw bytes / full lineage only on demand.

3. **Provenance is never more than one hop away.** From any visible object you can reach
   its source anchor, its raw artifact, the event that created it, and the actor
   responsible — without leaving the view or losing your selection.

4. **Raw and native are co-present.** Never render an interpreted/native object without a
   live path to the exact raw span it came from. The **anchor is the bridge** (DevTools
   source-map discipline). Parity is a guarantee, not a feature.

5. **Three tiers of truth, three visual registers.** *Accepted/native* (solid), *candidate/
   unjudged* (ghosted, dashed), *discarded* (hidden by default, recoverable). Plus a
   fourth: *raw-but-not-yet-interpreted* (present source with no native object yet). These
   never blur into each other. (See [Appendix B](#appendix-b-status--provenance-visual-legend).)

6. **Counts before contents; every count is a door.** Containers report a census
   (N artifacts · M units · K anchored · J connected · P disconnected) before you open
   them, and every number is clickable into the filtered set it summarizes. No dead-end
   metrics.

7. **Disconnection is a first-class signal.** "What did not connect" is shown as
   prominently as what did. Islands and unresolved anchors are terrain features, not
   errors swept under a rug.

8. **Local over global.** Neighborhoods are bounded (1–2 hops, node-capped). The
   all-world graph is *explicitly refused* — it is the canonical anti-pattern
   (§17). You widen by one hop, deliberately; you never "show all."

9. **One object, many projections.** List, outline, timeline, neighborhood, and inspector
   are *modes over a single current selection*, not separate datasets. Selection is
   linked across all regions (every projected item already carries a `:target` ref —
   see `projection-item` in `core.clj`).

10. **Dense but quiet.** Linear/IDE information density with terminal calm. No vanity
    sparklines, no gauge widgets, no motion that isn't a state change. Depth comes from
    surfaces (`:sunken`/`:elevated`) and a single accent, exactly as the existing shell does.

11. **Identity survives zoom.** Semantic zoom changes level-of-detail, never identity:
    the same `:target/id` is the same object whether shown as a silo census tile, an
    outline row, a graph node, or a full inspector. Provenance and relations are preserved
    at every level.

12. **Honest about machine work.** Distiller id + version (`text-line-v0`), content hash
    integrity, freshness, and human-vs-agent-vs-system authorship are visible. The user
    should always be able to tell what a machine guessed from what a person asserted.

---

## 3. Information architecture

### 3.1 The object IA (what the world contains)

```
World
└─ Branch (e.g. main)                         kernel: $$branches
   └─ Silo  (a source: a Roam graph, a repo,  view concept over :source/type +
      │      a Linear workspace, a doc set)    a source/import registry
      └─ SourceArtifact                         kernel: $$artifacts
         ├─ Revision (versioned content)        kernel: $$text-revisions, $$artifact-heads
         ├─ DerivedUnit (source-derived span)   kernel: $$units-by-artifact
         │  ├─ SourceAnchor (→ exact raw span)  unit :anchor {:range {:start :end ...}}
         │  └─ UnitStatus (per branch)          kernel: $$unit-status-by-branch
         └─ CompositionEdge (containment/order) unit :unit/order  (+ target-kind :relation)
   Relation (cross-source / cross-object)       target-kind :relation  (assumed PState)
```

Two **orthogonal provenance dimensions** cut across the whole tree and are always
reachable:

- **Actor** — who/what produced or judged a thing: `#{:human :agent :system :bot}`.
- **Event / lineage** — the causal chain `request → decision → event → materialization`,
  carried by `:root-event-id`, `:created-by-event/id`, `:causal {:parents …}`,
  and queryable via `$$events-by-id` / `$$decisions-by-id`.

### 3.2 The navigational IA (how the user moves)

Three persistent regions + one secondary tray + a scope bar:

- **Scope bar (top):** `World ▸ Silo ▸ Artifact ▸ Object` breadcrumb, branch selector,
  import freshness, command/filter entry (`/`). The breadcrumb *is* the zoom ladder.
- **Inventory rail (left):** source/silo census and drill-in.
- **Topology browser (center):** the current scope's objects, in one of four **modes**
  (`List · Outline · Timeline · Neighborhood`).
- **Inspector (right):** the current object, in **facet tabs**
  (`Native · Raw · Anchors · Revisions · Edges · Connections · Provenance`).
- **Connectivity tray (bottom):** accepted cross-source connections · folded candidates ·
  disconnected islands · errors. Always visible as counts; expandable.

### 3.3 The zoom ladder (semantic zoom, identity preserved)

| Scope | Altitude | Default center mode | Question answered |
|---|---|---|---|
| **World / Import** | 60k ft | Census tiles | What did we import, how much, which silos, how connected? |
| **Silo / Source** | 30k ft | List | What artifacts came from this source, their status/freshness? |
| **Artifact** | 10k ft | Outline | What native objects exist inside this artifact, in what order? |
| **Object / Unit** | ground | Inspector | What is this exactly — raw, native, anchors, edges, lineage? |
| **Neighborhood** | local | Bounded graph | What is *directly* related to this object (1–2 hops)? |

Crucially, every level is a **projection over the same PStates**. Descending the ladder
narrows the query; it never forks the data.

---

## 4. First screen layout

Default landing scope is **World / Import** — overview first. The user has imported a
Discourse Graph project; the screen orients them before they touch anything.

```
┌─ Softland · Imported Topology ────────────────────────────────────────────────────────────┐
│ World ▸ Discourse Graph                     branch: main ▾    imported 2h ago        ⌕ /    │  scope bar (36px)
├───────────────────┬─────────────────────────────────────────────────────┬───────────────────┤
│ SOURCES / SILOS   │  TOPOLOGY · Roam (Discourse Graph)                    │  INSPECTOR        │
│ ───────────────── │  ▸ List   Outline   Timeline   Neighborhood           │  ───────────────  │
│ ▸ All         318 │  type▾  status▾  actor▾  anchored▾  connected▾   ⌕     │  block #a7f3      │
│ ▾ Roam / DG   142 │  ───────────────────────────────────────────────────  │  unit · ACCEPTED  │
│    Pages       12 │  ◆ Claim     Memory is reconstructive        ✓  3 ⇄   │  human · main     │
│    Blocks     130 │  ◇ Evidence  Loftus (1974) misinformation    ·  1 ⇄   │ ┌───────────────┐ │
│ ▸ Code         96 │  ◆ Question  Does sleep consolidate memory?  ✓  2 ⇄   │ │Native  Raw    │ │
│ ▸ Linear       48 │  ◌ Claim     Stress impairs recall  (cand.)  ⋯  0 ⇄   │ │Anchors Revs   │ │
│ ▸ Transcripts  32 │  ◆ Source    Nature Neuroscience 2021        ✓  4 ⇄   │ │Edges Conn Prov│ │
│ ! Markdown      0 │  ◆ Claim     Reconsolidation reopens traces  ✓  1 ⇄   │ └───────────────┘ │
│    ⚠ 3 failed     │  … 124 more                                            │  « Native »       │
│ ───────────────── │                                                       │  Claim:           │
│ DISCONNECTED   41 │                                                       │  "Memory is       │
│ ───────────────── │                                                       │   reconstructive" │
│ legend  ◆accepted │                                                       │  ── from ──       │
│   ◌candidate ◇evid │                                                       │  page "Memory" ▸  │
│                   │                                                       │  block #a7f3      │
│                   │                                                       │  → view raw ▸     │
├───────────────────┴─────────────────────────────────────────────────────┴───────────────────┤
│ CONNECTIONS  ✓ 88 accepted ⇄   ◌ 23 candidates (folded) ⋯   ⛌ 41 islands   ⚠ 3 import errors  │  tray (28px)
└────────────────────────────────────────────────────────────────────────────────────────────-┘
```

Region widths follow the existing shell's pane-descriptor model
(`ws/pane-width-pct`): inventory ~256px (matches `sidebar-w`), inspector ~360–420px,
browser flexes. Header 36px and 4px spacing rhythm match `build-file-layout`.

Notes encoded in the wireframe:
- The **left rail** is the silo census; `All` and `Disconnected` are pseudo-silos
  (top and bottom). A failed source (`Markdown 0 · ⚠ 3 failed`) is visible at rest.
- The **center** opens in List for a silo scope; each row carries a *type glyph*, a
  *status tier* mark, and a *connection count* `N ⇄`. The candidate row (`◌ … (cand.)`)
  is ghosted.
- The **inspector** always shows identity + status tier + actor + branch, and the
  `── from ──` provenance path is on-screen even before you open a facet tab.
- The **tray** keeps accepted truth, folded candidates, islands, and errors as standing
  counts — the four things that must never silently vanish.

---

## 5. Source / silo inventory (left rail)

**Purpose:** answer *What did we import? How much? Which silos? Are they fresh/healthy?*
before any drill-in. This is the data-catalog "datasets" pane crossed with the IDE file
explorer — and it reuses the existing `ui-panel` → `ui-panel-group` → `ui-list-item`
vocabulary almost verbatim.

### 5.1 Structure

```
SOURCES / SILOS                       ← ui-panel-header, uppercase muted (fg-section)
─────────────────────────────
▸ All                            318  ← pseudo-silo: everything
─────────────────────────────
▾ Roam / DG                      142  ← ui-panel-group (collapsible, ▾/▸)
    ◆ graph "second-brain"  ●   130   ← artifact rows (ui-list-item)
    ◆ graph "dg-method"     ●    12
▸ Code                            96  ●
▸ Linear                          48  ◐ stale 6d
▸ Transcripts                     32  ●
! Markdown                         0  ⚠ 3 failed   ← error silo, expandable
─────────────────────────────
⛌ DISCONNECTED                    41  ← pseudo-silo: zero accepted relations
```

### 5.2 Silo row anatomy (`ui-list-item` slots)

- **leading:** source-type glyph (monochrome, not a colorful icon zoo):
  Roam `◆`, code `</>`, Linear `▤`, transcript `❝`, markdown `#`, PDF `▭`, canvas `▢`.
- **title:** silo name (the source: a specific graph, repo, workspace, or doc set).
- **trailing:** artifact **count** + a single **health dot**:
  - `●` success (synced, all anchors resolve) — token `:success`
  - `◐` warning (stale / partial / anchor drift) — token `:warning`
  - `⚠` destructive (ingest errors) — token `:destructive`
  - `◌` spinner (importing, with streaming count) — reuse `spinner` component

### 5.3 Census on hover / expand

Hovering a silo reveals its full census inline (or in the inspector if a silo is
selected) — never buried:

```
Roam / DG  ·  2 graphs · 12 pages · 130 blocks
            ·  118 anchored (91%) · 88 connected · 41 disconnected
            ·  newest 2h ago · oldest 9d · distiller block-v1
            ·  imported by: human ✋  agent 🤖
```

### 5.4 Behaviors

- Expanding a silo lists its **artifacts** (pages / files / issues), themselves
  expandable to units in the *browser*, not the rail (the rail stays an inventory).
- Selecting a silo scopes the center browser and updates the breadcrumb.
- `All` and `Disconnected` are first-class pseudo-silos; `Disconnected` selecting drives
  the browser to the islands set.
- Error silos expand to a list of **rejected decisions** (`decision/status :rejected`,
  with `:decision/reason` and `:errors`) — the kernel already produces these; this is
  where they surface.

Reused/adjacent code: `build-sidebar-tree` (indent guides, chevrons, scroll, hover/active
highlight), `ui-panel-group`, `ui-list-item`, `ui-badge`, `priority-colors` (for Linear),
`spinner`, `skeleton`.

---

## 6. Object / topology browser (center)

**Purpose:** show the objects in the current scope, in the projection that best fits that
scope, with filters — *without ever drawing the whole world*.

### 6.1 Four modes (tabs; `ui-tabs` already exists)

| Mode | Best at | Backed by | Default for |
|---|---|---|---|
| **List** | dense scan, sort, filter | `read-unit-projection` / artifact list | Silo scope |
| **Outline** | composition & order | `:unit/order` + CompositionEdges (Roam `:block/children`) | Artifact scope |
| **Timeline** | import order & lineage | `:event/time-ms`, causal chain | "what just arrived" / history |
| **Neighborhood** | local relations | bounded relation traversal | from a selected object |

The **default mode is chosen by scope** (see zoom ladder). World scope shows **census
tiles** (one quiet card per silo: count, status, connectivity bar) rather than a list —
overview without a hairball.

### 6.2 List mode (the workhorse — Linear density)

Each row (`ui-list-item`) carries, left → right:

```
[type glyph] [title / preview]                         [anchored] [status] [N ⇄] [actor]
◆            Memory is reconstructive                   ⚓         ✓        3 ⇄   ✋
◌            Stress impairs recall (candidate)          ⚓         ⋯        0 ⇄   🤖
◇            Loftus (1974) misinformation effect        ⚓         ·        1 ⇄   ✋
```

- **type glyph** — DerivedUnit type or DG node type (Claim/Evidence/Question/Source).
- **anchored** `⚓` present if the unit has a resolvable `:anchor`; absent/struck if the
  anchor failed to resolve against the current revision (anchor drift → warning tint).
- **status tier** — `✓` accepted, `·` plain/unjudged-but-not-candidate, `⋯` candidate
  (ghosted row), `⊘` superseded (struck), discarded hidden unless filtered in.
- **N ⇄** — connection count; **clickable** → Neighborhood mode focused here.
- **actor** — `✋` human · `🤖` agent · `⚙` system · `▸bot`. (Rendered as small glyphs/
  badges, not avatars — terminal register.)

### 6.3 Outline mode (composition & order)

For an artifact, render its CompositionEdges as a collapsible tree — exactly the existing
sidebar tree mechanics (indent guides, `▾/▸` chevrons), but over *units* instead of files.
This is where Roam's `:block/children` / `:block/order` and a doc's heading hierarchy
become legible:

```
▾ page "Memory"                              (artifact)
  ▾ ◆ Claim   Memory is reconstructive       ✓  3 ⇄
      ◇ Evidence  Loftus (1974)              ·  1 ⇄
      ◌ Claim  (candidate) Stress impairs    ⋯  0 ⇄
  ▸ ◆ Question  Does sleep consolidate…      ✓  2 ⇄
```

### 6.4 Filters / facets (`/` or filter bar)

`type · status · actor · anchored? · connected? · distiller-version · branch`.
Filters are **set operations over the projection**, mirrored in the URL/selection state so
a filtered view is shareable and re-enterable. Examples that matter:

- `status:candidate` → only machine guesses (review surface).
- `connected:false` → the islands.
- `anchored:false` OR `anchor:drift` → integrity problems.
- `actor:agent` → "what did the AI bring in / assert?"

### 6.5 What the browser must *not* do

No global force-directed graph. No infinite canvas. No "render all 318 nodes." Large sets
collapse to `… N more` with progressive loading (the kernel reads are keyed and paginable).

---

## 7. Selected-object inspector (right)

**Purpose:** everything about *one* object, with provenance and raw↔native parity always
reachable. This is the DevTools "elements/sources" inspector crossed with a lineage panel.

### 7.1 Always-visible header (never scrolls away)

```
block #a7f3                                  ← target ref (target/kind · target/id)
Claim · ACCEPTED · main · ✋ human            ← type · status tier · branch · actor
created evt_88c1 · 2h ago · distiller block-v1 · hash ✓
```

### 7.2 Facet tabs

| Tab | Shows | Kernel source |
|---|---|---|
| **Native** | the interpreted ObjectContainer/unit (e.g., the Claim, the def, the issue) | unit + revision head |
| **Raw** | preserved source bytes for this object's span | `$$text-revisions` + `:anchor :range` |
| **Anchors** | every SourceAnchor on this object; resolve status | unit `:anchor`(s) |
| **Revisions** | head + history; diff between revisions | `$$artifact-heads`, `$$text-revisions` |
| **Edges** | CompositionEdges in/out (parent, children, order) | `:unit/order`, composition |
| **Connections** | accepted cross-source relations + folded candidates | relations (assumed PState) |
| **Provenance** | lineage chain: request → decision → event → actor; causal parents | `$$events-by-id`, `$$decisions-by-id`, `:causal` |

### 7.3 The standing provenance footer

Below the tabs, always rendered (this is principle #3 made physical):

```
── from ──
silo  Roam / DG  ▸
page  "Memory"   ▸          (artifact)
block #a7f3      ▸ raw      (→ scroll Raw tab to exact range)
event evt_88c1   ▸          (→ Provenance tab)
actor ✋ human    ▸          (→ filter browser to this actor)
```

Each line is a link that *navigates without discarding the current selection* — you can
always come back. Folded by default: the full event history and every revision (shown as
"head + N more"); expanded on demand.

---

## 8. Raw source vs Softland-native representation

This is the trust core. The principle: **never show native without a one-action path to
the exact raw span, and never show raw without telling the user what Softland made of it.**

### 8.1 The anchor is the bridge (source-map discipline)

A DerivedUnit carries a `SourceAnchor` with an exact range, e.g.
`{:anchor/type :text/range :revision/id rev_… :range {:start 412 :end 487 :line-index 9}}`.
Selecting the native object highlights that span in Raw; selecting a raw span resolves to
the native object(s) anchored there. This is Xanadu/Memex transclusion made literal: the
native view *transcludes* a span of preserved source.

### 8.2 Two presentations

- **Toggle** (default in the narrow inspector): `« Native »` / `« Raw »` switch, with the
  anchored span scrolled into view and highlighted on switch.
- **Split** (when the browser is given to this artifact): raw on one side, native outline
  on the other, **scroll-and-selection linked** — the existing 3-pane shell
  (`build-file-layout`: Code | Chat | Preview) already proves Softland renders linked
  panes; this reuses that muscle as `Raw | Native`.

```
┌──────── RAW (markdown / EDN / code) ────┬──────── NATIVE (Softland objects) ────────┐
│  8 ## Memory                            │  ◆ Heading  "Memory"                       │
│  9 Memory is reconstructive, not a      │  ▾ ◆ Claim  Memory is reconstructive  ✓    │
│ 10 faithful recording. Loftus (1974)…   │      ◇ Evidence  Loftus (1974)        ·    │
│ 11                                      │      ◌ Claim (candidate) Stress…      ⋯    │
│ ▒▒ lines 9–10 anchor block #a7f3 ▒▒     │  hash ✓ · distiller block-v1               │
└──────────────────────────────────────--┴───────────────────────────────────────────┘
```

### 8.3 What the bridge must always disclose

- **Distiller + version** that produced native from raw (`text-line-v0`, `block-v1`).
  Native is a *derivation*; the user must know which machine made it.
- **Integrity:** `:content/hash` match (`✓`) or mismatch (`⚠` — raw changed under the
  anchor). Mismatch is anchor drift and is surfaced, never hidden.
- **Coverage:** which raw spans are *not yet* covered by any native object
  ("raw-but-uninterpreted" — a distinct fourth tier; rendered as plain, un-anchored raw
  with a subtle "no native object here" hint).

---

## 9. Local graph / neighborhood view

A graph mode exists — but it is the **antidote** to the global hairball, not an instance of
it. It is Obsidian's *local* graph done with discipline, never Obsidian's global graph.

### 9.1 Hard constraints

- **Center + 1 hop** by default; **2 hops** maximum; **node cap ~50** with `+N more`
  bundling. There is no "show all" control. Widening is one deliberate hop at a time.
- **Stable, readable layout** (layered/radial with deterministic positions), not jittering
  force-directed physics. Identity and position are stable across re-entry.
- The node at center is the current selection; selecting any node re-centers (and updates
  every other region — linked selection).

### 9.2 Edge & node encoding (carries the truth tiers)

```
                ┌─────────────┐
   ✋ human      │ ◆ Claim     │  accepted (solid border, fg full)
                │  Memory is  │
                │ reconstr.   │
                └──────┬──────┘
        supports ✓     │ contains (composition, thin grey)
        (solid accent) │
   ┌──────────────┐    ▼            ╌╌╌ candidate (dashed, ghosted) ╌╌╌┐
   │ ◇ Evidence   │◀───┘                                              ▼
   │  Loftus 1974 │                                        ┌──────────────────┐
   └──────────────┘                                        │ ◌ Claim (cand.)  │
        · · · anchored to raw (dotted, to source) · · ·    │  Stress impairs  │
                                                           └──────────────────┘
```

- **Composition edges**: thin neutral, with order preserved.
- **Accepted cross-source connections**: solid `:accent`.
- **Candidate connections**: dashed + ghosted; visually demoted, never mixed with accepted.
- **Anchor links** (object→raw): dotted; can be toggled off to reduce clutter.
- Node fill/border = status tier; small actor glyph on each node; type glyph as in List.

### 9.3 Standing reassurance

A persistent caption — *"Neighborhood of block #a7f3 · 1 hop · 14 of 142 objects"* — keeps
the user oriented that this is a *local* slice. A `widen +1 hop` chip is the only growth
control. This is the GIS/semantic-zoom commitment: you see your neighborhood, never the
whole earth at once.

---

## 10. Timeline / history / provenance view

This is where the README's "*how did we get here?*" begins — read-only.

### 10.1 Two timelines, one mechanism

- **Import timeline** (silo/world scope): when artifacts and units entered, by
  `:event/time-ms` of `:artifact/ingested` and unit creation. Answers "what arrived, and
  in what order / in what burst?" Bursts (a bulk Roam import) read as a dense band.
- **Object lineage** (object scope): the causal chain for *one* object —
  `ActionRequest → Decision(accepted/rejected) → KernelEvent → Materialization`,
  plus revision succession and status changes — walked via `:causal {:parents}`,
  `:root-event-id`, `:created-by-event/id`, and `$$decisions-by-id`.

```
IMPORT TIMELINE · Discourse Graph
 9d ───●────────────────────────────────────────●──────────●─── now
       │ Roam bulk import                        │ Linear   │ transcript
       │ 12 pages · 130 blocks · 🤖+✋            │ 48 issues│ 32 turns
                                                            ▲ you are here (2h ago)

OBJECT LINEAGE · block #a7f3  (Claim "Memory is reconstructive")
  req_88c0 ─ accepted ─▶ evt_88c1  artifact/ingested      ✋ human   9d
                          └▶ rev_3 (head)  hash ✓
  evt_91aa  unit/status-set → ACCEPTED                     ✋ human   2h
  ◌ candidate edge → "Stress impairs recall"  (unjudged)  🤖 agent   1h
```

### 10.2 Encoding

- **Actor lanes/coloring**: human / agent / system / bot are visually distinct so you can
  read "what did the machine do vs the person" at a glance.
- **Accepted vs candidate** events keep their tier styling here too (a candidate edge in
  lineage is ghosted).
- **Rejected decisions** appear on the timeline as visible stops (`✕ rejected: reason`),
  not silent gaps — failed imports are part of the history.
- Scrubbing selects a moment and filters the browser to "objects as of / created at" that
  point. (Full time-travel/replay is future; V1 reads the recorded order.)

---

## 11. Empty / loading / error states

Reuse `build-empty-state` (ASCII icon + headline + description) and the `skeleton` /
`spinner` components. Tone matches the existing shell's empty states ("No session", "No
preview").

### 11.1 Empty

- **Nothing imported:** icon `[]`, headline *"No material imported yet"*, description
  *"When sources are imported, this becomes the place where they land — sources on the
  left, objects in the middle, provenance on the right."* (Describe the terrain; do **not**
  build the import flow here — out of scope.)
- **Empty silo:** *"This source imported, but produced no objects yet"* (raw artifact
  exists, no units distilled) — points at distiller status.
- **Empty neighborhood:** *"Nothing is connected to this object yet"* — and a link to the
  islands set. Disconnection is a finding, stated plainly.

### 11.2 Loading

- Per-silo **skeleton rows** in the rail; **streaming counts** while ingesting
  (`importing… 412 units` ticking up), backed by the event stream.
- The browser shows skeleton list rows, not a blank pane or a spinner-only screen.
- Reads are progressive: census/counts first (cheap), then rows, then heavy raw/lineage on
  demand. Never block the overview on detail.

### 11.3 Error / degraded

- **Ingest failure:** a rejected `Decision` — surfaced in the error silo and the tray with
  `:decision/reason` + `:errors`, each expandable to the offending request. Honest, not
  buried.
- **Anchor drift / unresolved anchor:** the raw changed under an anchor
  (`:content/hash` mismatch, or anchored revision ≠ head). Marked `⚠` on the unit; the
  Anchors tab shows expected vs actual; the unit is *not* silently dropped.
- **Unknown source type:** the kernel's `unknown-action-decision` path — shown as
  "imported but not interpreted" raw artifacts (fourth tier), not as a crash.
- **Partial import:** "3 of 5 sources synced" banner in the scope bar; partial silos carry
  `◐`. The view is usable mid-import.

---

## 12. Interaction model

V1 is **read / navigate / reveal only.** The complete verb set:

- **select** (click) — sets the single current `:target`; mirrors to all regions.
- **peek** (hover) — transient preview (e.g., raw snippet, census) without committing
  selection. (`hover-id` already flows separately from selection in `build-sidebar-tree`.)
- **descend / ascend** (double-click / breadcrumb) — move the zoom scope.
- **expand / collapse** — silos, outline subtrees, folded detail, `+N more`.
- **switch mode** (center tabs) and **switch facet** (inspector tabs).
- **filter** (`/` + facets) — set operations over the projection.
- **follow link** — any provenance link / count / edge endpoint; selection-preserving.
- **widen neighborhood** (+1 hop) — the only graph growth verb.
- **scrub timeline** — select a moment; filter by time.

**Explicitly absent in V1** (out of scope, by design): editing, accept/reject/promote of
candidates, semantic search, LLM continuation, canvas authoring, source ingestion UI,
full-world navigation. The view *shows* candidate relations and discarded units (via
filter) but offers **no verb to change their status** — that arrives with the active layer.

### 12.1 Keyboard-first (IDE/Linear muscle; `cmd_panel` already exists)

```
j / k        move selection down / up        / or ⌘K   filter / command
h / l        ascend / descend scope          1 2 3 4   List / Outline / Timeline / Neighborhood
enter        open in inspector               [ ]       collapse / expand node
g then s/a/o jump to Silos / All / Object     r         reveal raw for selection
⇧+click      add to neighborhood (peek)       esc       clear filter / collapse inspector
```

### 12.2 Linked-selection invariant

There is exactly **one current object** at a time, identified by its kernel `:target`.
Changing it anywhere (rail, browser, inspector link, graph node, timeline mark) updates
everywhere. You never lose your place by inspecting provenance — the defining failure of
modal file managers.

---

## 13. What must be clickable

Everything that denotes an object or a relationship is a navigation target. Concretely:

- **Every object** — silo, artifact, unit/native object, revision, anchor, edge,
  connection, event, decision, actor.
- **Every count** — silo counts, census numbers, `N ⇄`, "124 more", islands count, error
  count. A count is a *door into its filtered set*, never a static stat.
- **Every anchor** — jumps Raw to the exact `:range` and highlights it.
- **Every edge / connection endpoint** — selects the other end (re-centers neighborhood).
- **Every provenance line** (the `── from ──` footer) — silo / artifact / event / actor.
- **Every actor glyph** — filters the world to that actor's contributions.
- **Every timeline mark** — selects that moment / object.
- **Every error / rejected decision** — opens its reason + offending request.
- **The disconnected/island chips** and the candidate tray — open their sets.
- **Breadcrumb segments** — ascend the zoom ladder.

If something on screen represents part of the world and *isn't* clickable, that's a bug.

---

## 14. What must never be hidden

These are always present (possibly compact, never absent):

1. **Provenance reachability** — the path from any object back to its raw source, event,
   and actor. (The `── from ──` footer guarantees this at object scope.)
2. **The truth tier** of any shown object or relation — accepted / candidate / discarded /
   raw-uninterpreted is always legible from its rendering.
3. **The existence of raw source** — even when folded, the affordance to see bytes is
   present. No native object pretends to be the origin.
4. **Disconnected / unresolved counts** — islands and orphans have a standing home in the
   tray and left rail.
5. **Errors and failed imports** — rejected decisions and anchor drift are surfaced, with
   reasons, not swallowed.
6. **Authorship register** — human vs agent vs system vs bot for created/judged things.
7. **Integrity & machine-origin signals** — content-hash status and distiller+version (at
   least as an indicator that expands).
8. **Branch context** — which branch you are viewing (the kernel is branch-aware;
   `unit-status` is *per branch*).
9. **Scope / "you are here"** — the breadcrumb and the neighborhood caption; the user
   always knows their altitude and that local ≠ global.

---

## 15. What should stay folded / compressed

Recoverable compression: present the skeleton, fold the mass, keep every fold openable.

- **Full raw bytes** — show the anchored span; "expand to full artifact" on demand.
- **Complete event/causal history** — show the creating event + status changes + a count
  ("+ 11 events"); expand to the full chain.
- **Every revision** — show head + "N revisions"; expand to history/diff.
- **Mass of homogeneous units** — "130 blocks · 412 line-units" with a sample and a search,
  rather than 412 rows dumped at once (progressive `+N more`).
- **Deep composition subtrees** — outline collapses by default below the focused level.
- **Candidate connections** — *separated from accepted truth* and folded to a count in the
  tray / a `⋯` per row; expand into the candidate set deliberately. (They are visible as
  existing, but never commingled with accepted facts.)
- **Discarded units** (`:rejected`/`:hidden`) — hidden by default, recoverable via the
  `status:discarded` filter (the kernel already has `read-discarded-view`).
- **Per-object anchors when numerous** — collapse to "anchored ✓" with an Anchors tab.
- **The global graph** — folded permanently into bounded neighborhoods; the *only* fold in
  this list that does not offer "expand to all," on purpose.

---

## 16. Visual references and patterns to borrow

| Source | What to borrow | Where it lands here |
|---|---|---|
| **VS Code / IDE explorer** | tree with indent guides, chevrons, sticky scope, reveal-in-tree | Inventory rail, Outline mode (already in `build-sidebar-tree`) |
| **Data catalogs** (DataHub, Atlan, Unity Catalog, dbt docs) | dataset census, freshness/health, upstream/downstream, lineage DAG done *bounded* | Silo census, connectivity stats, neighborhood |
| **OpenLineage / lineage tools** | run/event lineage, "what produced this" | Provenance tab, object lineage timeline |
| **Chrome DevTools — Sources + source maps** | minified↔original via mapping; jump-to-source; element inspector | Native↔Raw anchor bridge (§8), inspector |
| **Figma** | layers + canvas + inspector; one selection, three linked surfaces | Region model; linked selection; (canvas later) |
| **Linear** | dense rows, fast filters, command palette, keyboard-first, quiet dark UI | List mode, filters, `cmd_panel`, key model |
| **Obsidian / Roam local graph** | *local* graph around a node | Neighborhood (and its global graph = the anti-pattern, §17) |
| **Google Earth / GIS semantic zoom** | overview→detail with identity preserved; level-of-detail | Zoom ladder (§3.3); README's stated north star |
| **Memex / Xanadu** | transclusion, links as first-class, provenance | Anchor-as-transclusion (§8); links everywhere clickable |
| **Engelbart (NLS)** | view/control separation; multiple views of one structure | "one object, many projections" (principle #9) |
| **Bret Victor / Ink & Switch / Distill** | show the work; legible provenance; reactive linked views; explorable explanations | Provenance as terrain; linked selection; honest machine-origin |
| **Nicky Case** | gentle, legible systems; no intimidation | Empty states, captions, quiet density |

Reuse from the existing system: `dt` tokens (`:accent` blue, `:success` green, `:warning`
amber, `:destructive` red, surface elevation), typography scale (`typo-title/subtitle/
body/caption`), and the `ui-*` builders. The view should look like it *grew from* the
existing shell, because it literally renders in the same rect-tree substrate.

---

## 17. Anti-patterns to avoid

1. **The global hairball graph.** The cardinal sin. Rendering all objects as one
   force-directed node-link cloud is unreadable, slow, and epistemically useless. *Refused
   by construction* — only bounded neighborhoods exist (§9).
2. **Marketing-dashboard chrome.** No big number tiles with `↑24%`, no gauges, no vanity
   sparklines, no "engagement" framing. Counts here are navigational, not celebratory.
3. **Generic file-manager flattening.** Folders/files that hide type, status, provenance,
   and connectivity reduce the world to a disk. Every row must carry epistemic signal.
4. **Decorative motion.** No drifting nodes, physics jitter, or animated graph "breathing."
   Motion only marks a real state change.
5. **Commingling candidates with accepted facts.** The most dangerous failure: a guess that
   reads as truth. Candidates are always ghosted/dashed/folded and never share a register
   with accepted objects (§5/§6/§15).
6. **Native without raw.** Showing interpreted structure with no path to the bytes destroys
   provenance — the opposite of the product's reason to exist.
7. **Modal, selection-destroying navigation.** Drilling into provenance must not lose your
   place. One linked selection, always recoverable.
8. **Dead-end statistics.** Any number you can't click is a wall, not a door.
9. **Premature canvas / authoring.** This is not a canvas-first product; V1 must not grow
   editing, accept/reject, or free-form canvas. Resist scope creep toward them.
10. **Over-compression that hides problems.** Folding is good; folding *errors, islands, or
    drift out of existence* is not. The four tray counts are the guardrail.
11. **Icon zoo / color carnival.** Many saturated colors and cute icons read as toy. Stay
    monochrome-plus-one-accent; reserve `:success/:warning/:destructive` for status only.

---

## 18. Assumptions required from the kernel

The view is a **pure read-projection**; it never appends to `*world-requests-depot`. It
needs read access to the following. Most already exist; a few are reasonable additions the
kernel must expose for the view to be honest.

### 18.1 Already present (confirmed in `core.clj`)

- **Artifacts** — `$$artifacts`: `:artifact/id :artifact/type :source/type :content/hash
  :created-by :created-at :root-event-id`.
- **Revisions + heads** — `$$text-revisions`, `$$artifact-heads`.
- **Units + anchors + order** — `$$units-by-artifact`: `:unit/type :anchor {:range …}
  :unit/order :derived-by {:distiller/id :version} :provenance {:root-event-id}`.
- **Per-branch statuses** — `$$unit-status-by-branch` with
  `#{:accepted :rejected :hidden :promoted :superseded}` (+ `:unjudged` default).
- **Events & decisions** — `$$events-by-id`, `$$decisions-by-id` (accepted/rejected with
  `:reason`/`:errors`), `:causal {:parents :correlation/id :intent/id}`, `:actor`.
- **Branches** — `$$branches`. **Projections** — `$$projection-cache`,
  `read-unit-projection` (every item carries a `:target` — basis for linked selection).
- **Discarded projection** — `read-discarded-view` (basis for the `status:discarded`
  filter).

### 18.2 Assumed / to be exposed (small, read-only additions)

1. **Silo / source registry.** A notion of a *silo* (a specific Roam graph, repo, Linear
   workspace, doc set) above `:source/type`, so the rail can group by *source instance*,
   not just type. Assume artifacts can be grouped by a `:source/id` (silo) and that silos
   carry name + import status + freshness.
2. **Relations PState.** `target-kind :relation` exists, but no relations materialization
   is shown. The view assumes a readable `$$relations` (or composition+cross-source edge
   store): each relation has two endpoint `:target`s, a relation type
   (composition/order/production vs cross-source connection), a **status tier** (accepted /
   promoted / unjudged-candidate / discarded), `:provenance`, and `:actor`. This single
   store drives connection counts, the neighborhood, the tray, and the candidate fold.
3. **Connectivity / census aggregates.** Cheap counts per silo/type/status/actor and
   per-object connection degree — ideally materialized (extend `$$projection-cache`), so
   the overview never has to scan the world.
4. **Disconnection query.** "Objects/artifacts with zero *accepted* relations" — the
   islands set. Derivable from the relations store, but needs a defined read.
5. **Anchor resolution + drift.** Resolve a unit `:anchor :range` against the current head
   revision and report resolved / drifted (anchored revision ≠ head, or `:content/hash`
   mismatch). Needed for the `⚓`/`⚠` marks and the Anchors tab.
6. **Cross-source predicate.** "A relation whose endpoints belong to different silos /
   `:source/type`s" — defines what counts as a *cross-source* connection vs internal
   composition.
7. **Actor resolution.** Map `:actor/id` → display label + type for the actor glyphs and
   the actor filter (actor data is already on every event; assume a lookup).
8. **Policy / visibility honored on read.** Respect `:policy {:visibility}` so the view
   only projects what the viewer may see (kernel already carries policy).
9. **Stable, addressable selection.** The view leans on `:target {:target/kind :target/id
   :target/address}` as the universal identity for linked selection and shareable filtered
   states. Confirmed present in `projection-item`.
10. **Time-ordered reads.** Events are timestamped (`:event/time-ms`) and causally linked;
    the import timeline assumes an event read ordered by time and scoped by silo/branch.

> Design contract: if the kernel cannot yet answer a query above, the corresponding view
> element should **degrade to an honest unknown** (e.g., "connectivity not yet computed"),
> never fabricate a number. Epistemic honesty applies to the tool's own limits too.

---

## Appendix A: Kernel → View mapping

| Brief concept | Kernel reality (`core.clj`) | View surface |
|---|---|---|
| SourceArtifact | `$$artifacts` (`:artifact/type`, `:source/type`, `:content/hash`) | Silo rows, artifact rows, Raw tab |
| ObjectContainer / native object | unit + revision head (durable `:target/id`) | Browser rows, Native tab, graph nodes |
| Revision | `$$text-revisions` + `$$artifact-heads` | Revisions tab, timeline |
| DerivedUnit | `$$units-by-artifact` (`:derived-by` distiller+version) | Outline/List rows, "distilled by" |
| SourceAnchor | unit `:anchor {:range {:start :end :line-index}}` | `⚓` mark, Anchors tab, Raw↔Native bridge |
| CompositionEdge | `:unit/order` (+ Roam `:block/children`) | Outline tree, composition edges |
| Cross-source connection (accepted) | relation, status `:accepted`/`:promoted` | `N ⇄`, neighborhood (solid accent), tray |
| Candidate connection | relation, status `:unjudged` | ghosted/dashed, folded candidate tray |
| Projection / View | `read-unit-projection`, `$$projection-cache` | the whole view (modes are projections) |
| Actor | `:actor {:actor/type #{:human :agent :system :bot}}` | actor glyphs, actor lanes/filter |
| Lineage | `:causal`, `:root-event-id`, `$$decisions-by-id` | Provenance tab, object-lineage timeline |
| Branch | `$$branches`, `$$unit-status-by-branch` | branch selector, per-branch status |

## Appendix B: Status & provenance visual legend

| Tier | Kernel status | Glyph | Render (tokens) |
|---|---|---|---|
| **Accepted** | `:accepted` | `✓` `◆` | solid; `:fg` full; relations in `:accent` |
| **Promoted / canonical** | `:promoted` | `★` | solid + slight `:accent` emphasis |
| **Candidate** | `:unjudged` | `◌` `⋯` | **ghosted** (alpha ~0.5, the `ghost?` path in `ui-list-item`), dashed edges |
| **Superseded** | `:superseded` | `⊘` | struck/dim + "superseded by ▸" |
| **Discarded** | `:rejected` / `:hidden` | — | hidden by default; `status:discarded` filter (`read-discarded-view`) |
| **Raw-uninterpreted** | (no unit yet) | `·` | plain raw, "no native object here" hint |

| Health | Token | Glyph | Meaning |
|---|---|---|---|
| Synced / integrity OK | `:success` (green) | `●` `✓` | anchors resolve, hash matches |
| Stale / partial / drift | `:warning` (amber) | `◐` `⚠` | needs attention, anchor drift |
| Error / rejected | `:destructive` (red) | `⚠` `✕` | ingest failure, rejected decision |
| Connection / selection | `:accent` (blue) | `⇄` | accepted relation, current selection |

| Actor | Glyph |
|---|---|
| human | `✋` |
| agent | `🤖` |
| system | `⚙` |
| bot | `▸bot` |

## Appendix C: Two worked examples

### C.1 Softland project (markdown + code + transcripts + history)

- **Silos:** `Markdown` (README, design notes), `Code` (`.cljs`/`.clj`),
  `Transcripts` (Claude CLI agent trails — `agent_flow`/`trail` already exist),
  `History` (commits).
- **Artifacts → units → anchors:** a markdown doc → heading/paragraph units (anchored by
  char range); a code file → namespace/def units (anchored by line range); a transcript →
  turn/message units (anchored by message span), each with an `:actor` of human or agent.
- **Composition:** doc → section → paragraph; file → ns → def; transcript → turn.
- **Cross-source connections:** a design-note paragraph *references* a function (candidate
  until accepted); a commit *touches* files (production edge); a transcript turn *produced*
  a code change (`:provenance`/production). These populate `N ⇄` and the neighborhood.
- **What orientation reveals:** "Code: 96 artifacts, 91% anchored, 12 disconnected defs;
  Transcripts brought in 32 turns, 6 of which produced code; Markdown import had 3
  failures." The user trusts code-with-provenance, spots disconnected defs, and sees what
  the agent contributed vs the human.

### C.2 Discourse Graph project (Roam + codebase + Linear + chat)

- **Silos:** `Roam / DG`, `Code`, `Linear`, `Transcripts`.
- **Roam mapping (from `roam_ns.clj`):** pages (`:node/title`) → **artifacts**; blocks
  (`:block/uid`, `:block/string`) → **units**; `:block/children`/`:block/parents` +
  `:block/order` → **CompositionEdges**; `:block/refs` → **cross-object connections**;
  edit/create attrs → events/actors. Discourse-graph node types (Claim / Evidence /
  Question / Source) are the native unit types; DG relation types (supports / opposes /
  informs) are the cross-source connections — *accepted* if asserted, *candidate* if
  machine-suggested.
- **Linear:** issues → artifacts; title/description/comments → units; `priority` →
  `ui-priority-dot` (already built); an issue that *references* a Roam claim or a commit is
  a cross-source connection.
- **What orientation reveals:** the landing screen of §4 — "142 Roam objects, 88 accepted
  connections, 23 candidate links (folded), 41 islands, 3 import errors." The user reviews
  candidate DG links *separately* from accepted claims, jumps from a Claim to the exact
  Roam block that anchors it, and walks the lineage from a Linear issue to the commit that
  closed it — all without editing anything, because V1 is read-only.

---

*End of design research. This view's success test, in the README's spirit: another person
can open it and understand what entered the world — and trust it — without privately
reconstructing everything first.*
