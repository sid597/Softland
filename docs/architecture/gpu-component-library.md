# GPU Component Library — Design Document

> Spawned 2026-02-27. May grow into its own project.

## Vision
A GPU-rendered UI component library built on WebGPU SDF shaders. Same visual quality as shadcn/Tailwind/Radix but rendered entirely on the GPU — no DOM, no CSS. Inspired by Zed's GPUI.

## Core Principle: 5 Primitives, Infinite Components

Zed's genius: only **5 shader primitives** — Quad, Shadow, Glyph, Sprite, Underline — compose into 40+ components.

- A **button** = quad + glyph
- A **card** = shadow + quad + glyphs
- A **dropdown** = quads + glyphs + shadow + clip rect
- A **tooltip** = shadow + quad + glyph
- A **badge** = quad (small, rounded-full) + glyph
- A **input field** = quad (border) + glyph (text) + quad (cursor) + quad (selection)
- A **tab bar** = quads (bg + active tab) + glyphs + underline (indicator)
- A **progress bar** = quad (track) + quad (fill, rounded)
- A **modal** = shadow (large) + quad (backdrop) + quad (panel) + glyphs

**We adopt this exactly.** The shader work is finite and small (5 programs). The component library is pure composition in ClojureScript — rect tree nodes styled with design tokens. No new shaders needed per component.

## Design References
- **Zed GPUI**: Architecture, SDF shaders, 5-primitive model
- **shadcn/ui**: Component catalog, visual language, dark theme defaults
- **Linear**: Clean, minimal, dark-mode-first aesthetic (also our integration target)
- **Radix**: Accessibility patterns, interaction states
- **Tailwind**: Design token system (spacing, colors, radii, shadows)

## Rendering Primitives (Shader-level)

### 1. Quad (Rich Rectangle)
- Rounded corners (4 independent radii)
- Anti-aliased edges (SDF-based)
- Borders (4 independent widths + color)
- Linear gradients (angle + 2 stops + color space)
- Content clipping (clip rect)
- ~28 floats per instance

### 2. Shadow
- Blurred rounded rectangle
- Analytical Gaussian blur (erf-based, no texture sampling)
- Separate render pass (drawn before quads)
- ~17 floats per instance

### 3. Glyph (ALREADY HAVE)
- MSDF font atlas (Ubuntu Sans Mono)
- 12 floats per instance

### 4. Sprite/Image (FUTURE)
- Textured quad for icons, avatars, images
- Requires texture atlas or individual textures

### 5. Underline (FUTURE)
- Solid, dashed, wavy lines
- For text decoration, links

### 6. Line/Path (FUTURE)
- Arbitrary lines, dividers, connectors
- SDF-based with configurable thickness

## Component Catalog

### Controls
- [ ] Button (solid, outline, ghost, icon-only)
- [ ] Input field (cursor, selection, placeholder)
- [ ] Checkbox
- [ ] Radio
- [ ] Toggle / Switch
- [ ] Slider / Range
- [ ] Dropdown / Select
- [x] Command palette (EXISTS)

### Data Display
- [ ] Card
- [ ] Badge / Chip / Tag
- [ ] Avatar (circle + initials or image)
- [ ] Progress bar
- [ ] Loading spinner
- [ ] Tooltip
- [ ] Toast / Notification
- [ ] Table / Data grid
- [ ] Tree view
- [ ] Breadcrumbs

### Layout
- [ ] Panel (resizable, collapsible)
- [ ] Split pane (drag handle)
- [ ] Tabs
- [ ] Scroll container (custom scrollbar)
- [ ] Modal / Dialog
- [ ] Context menu
- [ ] Popover
- [ ] Sidebar / Nav rail
- [ ] Status bar / Toolbar
- [ ] Divider / Separator

## Interaction System
- [ ] Hover states (enter/leave transitions)
- [ ] Focus ring (keyboard nav)
- [ ] Cursor types (pointer, text, resize, grab)
- [ ] State variants (default/hover/active/disabled/focused)
- [ ] Smooth transitions (lerp between states over N frames)

## Design Tokens

### Colors (Linear/shadcn-inspired dark theme)
```
bg:          [0.09 0.09 0.11 1.0]   -- #171719
bg-subtle:   [0.12 0.12 0.15 1.0]   -- #1e1e26
bg-muted:    [0.16 0.16 0.20 1.0]   -- #282833
border:      [0.22 0.22 0.28 1.0]   -- #383847
border-hover:[0.30 0.30 0.38 1.0]   -- #4d4d61
fg:          [0.90 0.90 0.93 1.0]   -- #e5e5ed
fg-muted:    [0.55 0.55 0.63 1.0]   -- #8c8ca0
accent:      [0.44 0.54 1.00 1.0]   -- #708aff
accent-hover:[0.55 0.64 1.00 1.0]   -- #8ca3ff
destructive: [1.00 0.45 0.45 1.0]   -- #ff7373
success:     [0.40 0.85 0.65 1.0]   -- #66d9a6
warning:     [0.95 0.79 0.45 1.0]   -- #f2c973
```

### Spacing (4px base)
```
xs: 4    sm: 8    md: 12    lg: 16    xl: 24    2xl: 32
```

### Border Radius
```
sm: 4    md: 6    lg: 8    xl: 12    full: 9999
```

### Shadows
```
sm:  blur=4  offset-y=1  alpha=0.15
md:  blur=8  offset-y=2  alpha=0.20
lg:  blur=16 offset-y=4  alpha=0.25
```

## Implementation Phases

### Phase 1: Rich Quads
- Rewrite rect fragment shader with SDF math
- Expand instance buffer to ~28 floats
- Rounded corners, borders, gradients, AA
- Update `tree->rects` to propagate style properties

### Phase 2: Shadows
- New shadow shader (erf-based analytical blur)
- Separate render pass (before quads)
- Shadow buffer + pipeline

### Phase 3: Component Library
- Define components as rect-tree node compositions
- Design token system (colors, spacing, radii, shadows)
- State management (hover/active/focus/disabled variants)
- Ship: card, button, badge, tabs, scrollbar, divider, tooltip, progress

## Key Technical References
- Inigo Quilez: SDF rounded boxes — https://iquilezles.org/articles/roundedboxes/
- Evan Wallace: Fast rounded rect shadows — https://madebyevan.com/shaders/fast-rounded-rectangle-shadows/
- Raph Levien: Blurred rounded rects — https://raphlinus.github.io/graphics/2020/04/21/blurred-rounded-rects.html
- Zed GPUI shaders: https://github.com/zed-industries/zed/blob/main/crates/gpui_wgpu/src/shaders.wgsl
- Zed blog (GPU UI): https://zed.dev/blog/videogame
