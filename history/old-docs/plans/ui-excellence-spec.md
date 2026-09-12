# Softland UI Excellence Report — Implementation Spec

**Date originated:** March 2, 2026
**Scope:** WebGPU workspace shell (sidebar, editor, chat/session, preview, command bar)
**Target quality:** Tier-1 spatial workspace (Linear, Zed, Figma)
**Status:** Phase 1+2 DONE (session 29), Phase 3+4 TODO

**Current implementation homes:**
- `src/app/client/workspace/runtime.cljs` — orchestration, reactive flows, click handlers
- `src/app/client/workspace/editor_compute.cljs` — editor layout/rect compute
- `src/app/client/workspace/combined_text.cljs` — text assembly
- `src/app/client/workspace/ui_primitives.cljs` — shared UI builders
- `src/app/client/substrate/webgpu/renderer.cljs` — rendering substrate
- `src/app/electric_flow.cljc` — entry/membrane, char-width invariant handoff
- `src/components/design_tokens.cljc` — surface elevation tokens

> Historical note: when this spec was written, most of this work lived in `src/app/client/webgpu/loop.cljs` plus `editor.cljs`. Those files have since been refactored into `workspace/*` and `substrate/webgpu/renderer.cljs`.

---

## 1. Executive Assessment

Current state at start: **5/10 usability, 4/10 visual execution.** Structurally sound prototype, not yet product quality. The layout renders data into rectangles but doesn't guide, differentiate, or communicate state.

**Three foundational failures:**
1. Text bleeds across pane boundaries (rendering invariant broken)
2. All panes have identical visual weight (no focus hierarchy)
3. No spatial rhythm — padding, margins, and gaps are ad-hoc

Target after this spec: **8.5/10**, with clear path to 9/10.

---

## 2. P0 Rendering Invariants (Fix Before Anything Else)

### 2a. Bounding Box Clipping — DONE (session 29)

**Bug:** Text in the Editor pane bleeds horizontally across the structural divider and overlaps the Chat pane.

**What was done:**
- `<combined-text-ops` line ~3735: replaced hardcoded `(* fs 0.56)` with `(* fs font-cw)` where `font-cw = (:char-width active-font 0.56)`
- Added per-glyph truncation for grouped ops (line numbers etc.) — was only filtering by position, not truncating text
- `tree->text-ops` (~line 829): added `clip-right` and `truncate-op` — text ops that extend beyond `clip-bounds` are character-truncated

### 2b. Text/Cursor Alignment Sync — DONE (session 29)

**Bug:** Three different char-width values in use: `electric_flow.cljc` had `0.60`, `loop.cljs` fallback had `0.60`, clipping used `0.56`.

**What was done:**
- `electric_flow.cljc:413` — changed `char-width 0.60` to `0.56`
- `loop.cljs` default-manifest — changed `:charWidth 0.60` to `0.56`
- `loop.cljs` default-font fallback — changed `:charWidth 0.60` to `0.56`
- Clipping path uses `(:char-width active-font)` so it tracks whatever font is active

**Invariant going forward:** Never hardcode char-width. Always read from `(:char-width active-font)` or the font manifest.

---

## 3. Spatial System (4px Grid) — PARTIALLY DONE (session 29)

### System

| Token | Value | Use |
|-------|-------|-----|
| `space-xs` | 4px | Minimum gap, icon margin |
| `space-sm` | 8px | Inter-element gap, group separator |
| `space-md` | 12px | Intra-component padding |
| `space-lg` | 16px | Pane content gutter (minimum) |
| `space-xl` | 24px | Section separation |
| `space-xxl` | 32px | Major section breaks |

These exist in `(:spacing dt)`.

### What was done (session 29):
- [x] Pane header height 32px → 36px
- [x] Pane header padding 12px → 16px (`space-lg`)
- [x] Sidebar active inset 6px → 8px, radius `:md` → `:sm` (4px)

### Still TODO:
- [ ] Audit all other builders for non-standard spacing values
- [ ] Command bar internal padding normalization

---

## 4. Visual System — Depth Over Borders — DONE (session 29)

### Color Tokens (added to `design_tokens.cljc` `:surfaces` key)

| Token | RGBA | Use |
|-------|------|-----|
| `surface-sunken` | `[0.05 0.06 0.08 1.0]` | Unfocused panes, base canvas |
| `surface-base` | `[0.07 0.08 0.11 1.0]` | Sidebar |
| `surface-elevated` | `[0.12 0.15 0.21 1.0]` | Active/focused pane |
| `surface-hover` | `[0.13 0.17 0.22 1.0]` | Hovered rows, menu items |
| `surface-active` | `[0.18 0.23 0.31 1.0]` | Selected file, pressed states |
| `text-primary` | `[0.90 0.93 0.97 1.0]` | Active code, selected text |
| `text-secondary` | `[0.55 0.61 0.70 1.0]` | Inactive files, pane headers |
| `text-muted` | `[0.36 0.42 0.50 1.0]` | Empty states, structural labels |
| `accent` | `[0.24 0.63 1.0 1.0]` | Focus rings, active indicators |

### What was done (session 29):
- [x] Added `:surfaces` key to `design_tokens.cljc`
- [x] Removed 1px full-height dividers between editor/chat/preview panes
- [x] Active pane = `surface-elevated`, inactive = `surface-sunken`
- [x] Sidebar keeps its right-edge 1px border (structural)
- [x] Root `file-layout-root` has no bg (transparent) so child pane bgs define depth directly

---

## 5. Focus Hierarchy — DONE (session 29)

### What was done:
- [x] `!active-pane` atom in `start-loop!` (`:editor` | `:chat` | `:preview`)
- [x] Click in any pane area → sets `!active-pane` (determined by `rel-x` position vs `code-w`/`chat-w`)
- [x] `Cmd+1`/`2`/`3` keyboard shortcuts → `:focus-pane` events in `parse-key-event`
- [x] Focused pane: `surface-elevated` bg, header text at `text-primary`
- [x] Unfocused panes: `surface-sunken` bg, header text at `text-secondary`
- [x] Focus indicator: 2px bottom accent underline (40px wide, `radius: 1`) on focused pane header
- [x] Wired through `m/watch` into `<editor-rects` (23 args) and `<combined-text-ops` (22 args)

---

## 6. Typography Hierarchy — DONE (session 29)

### System

| Level | Size | Alpha | Use |
|-------|------|-------|-----|
| **Title** | 20 (`:xl`) | 1.0 | Component name, pane title when focused |
| **Subtitle** | 16 (`:lg`) | 0.9 | Pane headers, section titles |
| **Body** | 14 (`:md`) | 0.85 | Content text, code, trail entries |
| **Caption** | 12 (`:sm`) | 0.6 | Metadata, timestamps, group headers |

Defined as `typo-title`, `typo-subtitle`, `typo-body`, `typo-caption` defs in loop.cljs after `dt`.

### What was done (session 29):
- [x] Constants defined
- [x] All 3 pane headers → `typo-subtitle`
- [x] Sidebar explorer label → overline (11px, `text-muted`)
- [x] Sidebar back button → `typo-subtitle`, subtitle text → `typo-body`
- [x] Sidebar breadcrumb → `typo-body` (was `typo-caption`, too small)
- [x] Intake tree header → `typo-subtitle`
- [x] Right-detail: title → `typo-subtitle`, body → `typo-body`, hints → `typo-caption`
- [x] Multi-select detail: count → `typo-subtitle`, items → `typo-body`

---

## 7. Interaction State Matrix — PARTIALLY TODO

### Required states (every interactive element)

| State | Visual Treatment |
|-------|-----------------|
| **Default** | `text-secondary` on transparent or `surface-base` |
| **Hover** | `surface-hover` bg, `text-primary` text |
| **Pressed** | `surface-active` bg, slightly dimmed |
| **Selected** | `surface-active` bg, `text-primary` text, 4px radius inset |
| **Selected + Focused** | Same as selected + accent bar |
| **Focus-visible** | 2px outer ring in `accent`, offset 1px |
| **Disabled** | 0.4 alpha, no hover response |
| **Loading** | Shimmer pulse |
| **Empty** | Centered icon + headline + description |
| **Error** | `destructive` accent |

### What was done (session 29):
- [x] Empty states redesigned with `build-empty-state` helper
- [x] Selected + hovered states work in sidebar and ticket list (pre-existing)
- [x] Sidebar: active file has accent bar + bg-selected, hovered has bg-hover

### Still TODO:
- [ ] Pressed state on chat pane tool cards (mousedown)
- [ ] Focus-visible ring on command bar (keyboard focus)
- [ ] Disabled state on buttons
- [ ] Loading shimmer on tool cards

---

## 8. Sidebar Improvements — PARTIALLY DONE (session 29)

### What was done:
- [x] Active selection: 8px horizontal inset, 4px radius (`sm`), 2px accent bar
- [x] Explorer label: overline style (11px, uppercase, `text-muted`)
- [x] Back button: `typo-subtitle` project name
- [x] Breadcrumb: `typo-body` filename (was `typo-caption`, too small)

### Still TODO (Phase 3):
- [ ] Category/group labels in component directories (overline style)
- [ ] Status dots for components (green=ready, amber=stub) — needs `!component-registry` + API fetch
- [ ] File type indicators (prefix: `.edn` → `{}`, `.cljs` → `fn`, `.md` → `#`)

---

## 9. Empty State Design — DONE (session 29)

### System

Every empty state follows: `[icon] → Headline → Description`

Implemented as `build-empty-state` (~line 1321 in loop.cljs).

| Pane | Icon | Headline | Description |
|------|------|----------|-------------|
| Chat (idle) | `--` | No session | Type a prompt below to start a conversation. |
| Preview (no content) | `[]` | No preview | Preview will appear when a component is compiled. |
| Right detail (no selection) | `<>` | Select a ticket | Click a ticket from the list to view its details. |
| Right detail (empty) | `{}` | No tickets | Run /bootstrap to fetch tickets from Linear. |

---

## 10. Context-Aware Content — TODO (Phase 3)

### What needs to be done:
- [ ] `file-content-type` fn: detect `_source.edn` → `:component-meta`, else `:code`
- [ ] `build-component-detail` fn: single-surface component detail page for `:component-meta` files
  - Title bar with status badge
  - Source URL + Documentation URL
  - Status metadata + converted date
  - CTA button (`:stub` → "Convert Component", `:ready` → "Re-convert")
  - Preview area (empty state or rt-node)
- [ ] Wire to `/api/extract/compile` pipeline for conversion

---

## 11. Command Bar Integration — PARTIALLY DONE (session 29)

### What was done:
- [x] Command bar bg updated to `surface-elevated`
- [x] Bottom clip: editor text ops filtered to not bleed into cmd-panel zone

### Still TODO:
- [ ] Embed command input inside chat pane bottom (larger refactor, P3)

---

## 12. Engineering Guardrails (permanent)

1. **Combine watches with `m/latest`** — NEVER `m/ap` + multiple `m/?<`
2. **Event filtering with `m/eduction` + `deref`** — not `m/ap` + `m/watch`
3. **No `try/catch` inside `e/defn`** — Electric 3 limitation
4. **Text/cursor sync** — `0.56` char-width factor identical in loop.cljs and editor.cljs
5. **New atoms** must be wired through `m/watch` into `<editor-rects` AND `<combined-text-ops` — arg counts MUST match

---

## 13. Horizontal Scrolling — DONE (session 29)

Not in original spec — added during implementation.

- `>wheel` flow: now emits `{:dy :dx :shift?}` maps (was bare deltaY number)
- `!scroll-x` atom in `start-loop!`, wired into both m/latest flows
- Shift+wheel or native trackpad dx → horizontal scroll, clamped to >= 0
- `layout-x` offset by `-scroll-x` in text ops, editor rects, cursor click, and drag-selection handlers

---

## 14. Implementation Order + Status

### Phase 1: Rendering Integrity (P0) — DONE
- [x] Fix char-width mismatch: normalize to `0.56`
- [x] Fix clipping in `<combined-text-ops`: use `(:char-width active-font)`
- [x] Fix grouped op clipping: add per-glyph truncation
- [x] Fix `tree->text-ops`: add clip-right truncation
- [x] Typography hierarchy constants
- [x] Apply typography constants everywhere

### Phase 2: Space & Depth (P1) — DONE
- [x] Surface elevation tokens in `design_tokens.cljc`
- [x] Remove harsh pane dividers, use surface contrast
- [x] Active pane focus hierarchy (`!active-pane` atom)
- [x] Spatial rhythm — 4px grid padding on pane headers
- [x] Pane header 36px height, 16px padding
- [x] Sidebar active selection: 8px inset, 4px radius, 2px accent bar
- [x] Empty state redesign (`build-empty-state` helper)
- [x] Command bar docking (elevated bg)
- [x] Chat header status colors
- [x] Bottom clip (editor text doesn't bleed into cmd bar)
- [x] Horizontal scrolling

### Phase 3: Context-Aware Features (P2) — TODO
- [ ] Content type detection (`file-content-type`)
- [ ] `build-component-detail` — component detail view for `_source.edn`
- [ ] Sidebar status dots (requires `!component-registry` + API)
- [ ] Sidebar category labels (overline style)
- [ ] File type indicators in sidebar (prefix icons)
- [ ] Convert button wired to `/api/extract/compile`

### Phase 4: Motion & Identity (P3) — TODO
- [ ] Embedded command panel in chat pane
- [ ] Hover transitions across all interactive elements
- [ ] Component preview rendering in detail view

---

## 15. Acceptance Tests

### Phase 1 Ship Gate — SHOULD PASS NOW
1. Paste a 500+ char line in editor → zero pixels bleed past code pane boundary
2. Cursor at end of 100-char line → aligns exactly with last character
3. All pane headers use consistent Subtitle typography
4. Typography scale is visually distinguishable

### Phase 2 Ship Gate — SHOULD PASS NOW
5. Focused pane identifiable in under 1 second (elevated vs sunken)
6. Sidebar selected row has 8px inset + 4px radius + 2px accent bar
7. Command bar appears docked with elevated bg
8. Empty chat pane shows centered icon + headline + description
9. Empty preview pane shows centered message
10. Cmd+1/2/3 switches pane focus

### Phase 3 Ship Gate — NOT STARTED
11. Click `button/_source.edn` → see component detail view
12. Click `calendar/_source.edn` → see "Convert Component" CTA
13. Component folders in sidebar show green/amber status dots
