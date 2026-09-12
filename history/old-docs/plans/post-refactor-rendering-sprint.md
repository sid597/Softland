# Post-Refactor Rendering Sprint

> Drafted on 2026-04-01 before Phase 8 implementation review.
> Purpose: preserve the rendering/resource roadmap without mutating `docs/sessions/next-prompt.md` until review is complete.

## Why this exists

Phase 6A-6E completed the differential rendering and dirty-present substrate.

That creates a natural next rendering workstream that is broader than a single bug fix, but still bounded:

1. Make GPU resource usage visible.
2. Measure whether the current MSDF text pipeline survives the zoom range Softland actually wants.
3. Only then decide whether to stay on MSDF, add a cheaper fallback, or escalate to Slug.

This document is the saved plan for that workstream.

## Core question

The next rendering decision is not "implement Slug immediately."

The next rendering decision is:

**Does the current MSDF pipeline survive the quality envelope required for continuous semantic zoom, and what is the cheapest lawful response if it does not?**

That answer determines whether:

- current MSDF is good enough
- multi-resolution MSDF atlas is enough
- Slug becomes necessary

## External resources involved

### Already harvested into shipped work

- `external-resources/webgpu-field-report/`
  - Partial-clear shader informed Phase 6E dirty-present.
- `external-resources/dynamic-sdf-engine/`
  - Handle wrappers / generation counters informed Phase 6D.

### Immediate post-refactor work

- `external-resources/demote-tracker/`
  - GPU memory budget tracking
- `external-resources/slug/`
  - MSDF quality-envelope test
  - conditional Slug escalation path

### Near-future, but not first

- `external-resources/pretext/`
  - for prose / knowledge-layer layout, after the font-rendering decision is clearer

### Future / reference pressure

- `external-resources/panproto/`
  - artifact ontology / lawful projection pressure, not immediate renderer work
- `external-resources/batch-marching/`
- `external-resources/sdf-resource-collection/`
  - future world-scale rendering references

## Workstream structure

## Workstream A — GPU Budget + Resource Visibility

### Goal

Make GPU memory and allocation growth visible before adding more rendering experiments.

### Why now

The current runtime already allocates multiple GPU systems:

- text systems
- rect systems
- shadow systems
- differential pools
- persistent render target
- font atlas textures
- runtime-owned rect buffers

Without tracking, VRAM pressure and growth remain invisible.

### Scope

Build a GPU allocation tracker that reports:

- adapter / device limits available from WebGPU
- total reserved bytes
- total live-by-design resource categories
- per-subsystem breakdown
- growth events
- resize events

### Resource categories to track

- text system instance buffers
- text camera/sizing uniform buffers
- text atlas textures
- rect system instance buffers
- shadow system instance buffers
- persistent render target texture
- buffer pool buffers
- command panel rect buffer
- settings panel rect buffer

### Candidate implementation surfaces

- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/substrate/webgpu/buffer_pool.cljs`
- `src/app/client/workspace/runtime/state.cljs`
- optional diagnostics exposure in `src/app/client/workspace/runtime/render.cljs`

### Desired output

- startup console report with limits + current allocations
- logs on buffer growth and texture recreation
- optional debug overlay with current totals
- a baseline table of current runtime GPU footprint

### Exit criteria

We can answer:

- how many GPU bytes are currently reserved
- which subsystems dominate the footprint
- what changes usage during resize / font change / pool growth

## Workstream B — MSDF Quality Envelope Lab

### Goal

Measure the usable zoom envelope of the current MSDF pipeline.

### Why now

This is the cheapest architectural truth-discovery experiment in the whole rendering stack.

If MSDF already covers the needed range, there is no reason to rush Slug.
If it fails badly, the text-rendering roadmap changes immediately.

### Scope

Add a temporary experimental zoom multiplier for text rendering and test the current MSDF path at extreme scales.

### Test matrix

At minimum:

- `0.25x`
- `0.5x`
- `1x`
- `2x`
- `4x`
- `8x`
- `16x`
- `32x`

Also vary, where practical:

- DPR
- font choice
- `pxRange`
- `sharpness`

### What to inspect visually

- corner rounding
- edge blur
- color fringing / bleed
- punctuation clarity
- thin diagonal quality
- braces / brackets readability
- reduction artifacts at tiny sizes

### Candidate implementation surfaces

- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/workspace/runtime/render.cljs`
- `src/app/client/workspace/runtime/fonts.cljs`

### Deliverables

- screenshot matrix
- measured acceptable zoom envelope
- written summary of breakpoints
- explicit recommendation:
  - stay on single-atlas MSDF
  - try multi-resolution MSDF
  - escalate to Slug PoC

### Exit criteria

The team knows whether MSDF is structurally good enough for the zoom story.

## Workstream C — Multi-Resolution MSDF Fallback

### Condition

Only do this if Workstream B shows that single-atlas MSDF is weak enough to matter, but not so bad that full Slug is obviously required.

### Goal

Try the cheapest renderer-side extension before committing to a new glyph rendering architecture.

### Scope

- generate multiple MSDF atlases at different base resolutions
- select atlas by rendered glyph size
- compare visual quality and transition behavior

### Candidate implementation surfaces

- font asset loading in `src/app/client/workspace/runtime/fonts.cljs`
- text bind group / texture selection in `src/app/client/substrate/webgpu/renderer.cljs`

### Deliverables

- prototype using multiple atlases
- screenshots at failing zoom levels from Workstream B
- judgment on whether this is sufficient

### Exit criteria

Either:

- this solves the problem cheaply, or
- evidence now justifies a Slug PoC

## Workstream D — Slug Single-Glyph PoC

### Condition

Only do this if:

- Workstream B shows a meaningful MSDF quality failure, and
- Workstream C is either insufficient or intentionally skipped

### Goal

Test Slug viability on this WebGPU/WGSL stack without prematurely rewriting the text renderer.

### Scope

Bounded proof only:

- one glyph
- one separate pipeline
- reference shader subset ported to WGSL
- quality comparison against MSDF

No editor integration yet.
No full text system yet.
No multi-glyph layout yet.

### Candidate implementation surfaces

- isolated experiment file or temporary branch under `renderer.cljs`
- font outline extraction helper

### Deliverables

- WGSL PoC
- screenshot comparison
- implementation risk notes
- go / no-go recommendation for fuller Slug work

### Exit criteria

We know whether Slug is technically viable and whether it materially improves the problem Workstream B exposed.

## Ordering

Recommended order:

1. GPU budget tracker
2. MSDF quality-envelope lab
3. Multi-resolution MSDF fallback if needed
4. Slug single-glyph PoC if still needed

## Explicit non-goals for this sprint

Not included here:

- full Slug integration into the editor
- Pretext integration
- world-scale SDF rendering
- batch marching implementation
- panproto implementation work

Those are separate lanes and should not be smuggled into this sprint.

## Relationship to product work

This rendering sprint can run in parallel with Phase 8 product-lane work.

Suggested split:

- one thread/agent works on product-lane reconnect work such as trail click-to-navigate
- another thread/agent works on renderer observability and the MSDF decision tree

## Decision gates

### Gate 1 — After Workstream A

Do we now have enough GPU observability to evaluate further rendering work confidently?

If no:

- finish tracker before proceeding

### Gate 2 — After Workstream B

What is the verdict on current MSDF?

- good enough -> stop Slug lane for now
- borderline -> do Workstream C
- clearly insufficient -> Slug PoC is justified

### Gate 3 — After Workstream C

Is multi-resolution MSDF sufficient?

- yes -> keep MSDF path
- no -> proceed to Slug PoC

### Gate 4 — After Workstream D

Is Slug viable enough to justify fuller integration work?

- yes -> plan a dedicated Slug integration phase
- no -> keep MSDF-family approaches and constrain zoom expectations

## Suggested immediate next actions

If this sprint starts right after the current refactor/product review sequence:

1. Implement GPU budget tracker first.
2. Immediately follow with the MSDF quality-envelope test.
3. Write down the outcome before touching Slug proper.

## Status

- Saved as draft plan before Phase 8 review
- Not yet reflected into `docs/sessions/next-prompt.md`
- Not yet synced into `external-resources/experiments-tracker.md`
