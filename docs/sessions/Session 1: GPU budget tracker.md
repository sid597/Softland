# Session 1: GPU budget tracker

## Purpose

Build GPU resource visibility into the current WebGPU runtime so future rendering work happens with live memory context instead of guesswork.

This session is not a debate about whether to do later rendering work. It is instrumentation and baseline capture.

## Why now

Phase 6 introduced or hardened:

- differential rect pools
- differential shadow pools
- dirty-present with persistent render target
- split runtime-owned GPU buffers

That means Softland now has enough GPU structure that invisible memory growth becomes an architectural blind spot.

## Primary outputs

1. A startup report of relevant WebGPU adapter/device limits.
2. A per-subsystem GPU allocation breakdown.
3. Growth and resize logs for buffers and textures.
4. A baseline measurement pass across the main runtime states.

## Candidate code surfaces

- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/substrate/webgpu/buffer_pool.cljs`
- `src/app/client/workspace/runtime/state.cljs`
- `src/app/client/workspace/runtime/render.cljs`

## Resource categories to track

- text instance buffers
- text uniform buffers
- font atlas textures
- rect system buffers
- shadow system buffers
- buffer-pool allocations
- command/settings runtime-owned buffers
- persistent render target texture

## Implementation shape

### 1. Add a central GPU allocation registry

Create one place that can record:

- resource kind
- subsystem label
- byte size
- creation event
- destruction event
- resize or replacement event

The tracker should answer both:

- current reserved total
- current breakdown by subsystem

### 2. Instrument every major allocation path

Track:

- `device.createBuffer`
- `device.createTexture`
- explicit destroy paths
- pool growth/replacement
- render-target recreation

If exact live bytes are not available for every resource, prioritize accurate reserved bytes.

### 3. Annotate subsystem ownership

Do not leave resources under generic labels like `buffer-1` or `texture-2`.

Use labels such as:

- `text/instances`
- `text/atlas`
- `rect/editor`
- `rect/sidebar`
- `shadow/editor`
- `shadow/sidebar`
- `render-target/persistent`
- `runtime/cmd`
- `runtime/settings`

### 4. Expose a readable report

At minimum:

- startup console table
- growth log when capacity changes
- resize log when the render target changes

Optional if easy:

- lightweight on-screen HUD in dev mode

### 5. Capture a baseline walkthrough

Run the tracker through these states and record totals:

1. Fresh app idle on localhost.
2. Default editor workspace with a file open.
3. `/bootstrap` flow screen.
4. Flow detail open.
5. Window resize.
6. DPR change if available.

## Deliverable format

By end of session, write down:

- total reserved GPU bytes at each state
- largest subsystems
- which operations trigger allocation growth
- whether any resource looks unexpectedly large

This can go into a short follow-up note or be appended to the rendering sprint plan.

## Definition of done

- Softland can report current GPU reserved bytes.
- Pool growth and render-target recreation are visible.
- Resource ownership is attributable by subsystem.
- A real baseline has been captured from live runtime use.

## Guardrails

- Keep instrumentation easy to remove or quiet in production.
- Prefer accurate byte accounting over fancy presentation.
- Do not let HUD work swallow the session; console-first is acceptable.
- Do not expand this into Slug work during this session.

## Context links

- `docs/plans/post-refactor-rendering-sprint.md`
- `external-resources/demote-tracker/experiments.md`
- `external-resources/synthesis.md`
