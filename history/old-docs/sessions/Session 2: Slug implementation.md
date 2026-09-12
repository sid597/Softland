# Session 2: Slug implementation

## Purpose

Implement Slug as Softland's next text-rendering path for continuous semantic zoom.

This session is not about deciding whether Slug is justified. The direction is already chosen. The job is to build it well.

## Core promise

Render glyphs from curve data on the GPU so text remains clean across extreme zoom ranges without depending on a single MSDF atlas quality envelope.

## Inputs from the previous session

Session 1 provides:

- GPU budget visibility
- memory baseline for the current renderer
- allocation instrumentation useful for watching the new path

Those inputs support the implementation. They are not permission gates.

## Candidate code surfaces

- `src/app/client/substrate/webgpu/renderer.cljs`
- `src/app/client/workspace/runtime/fonts.cljs`
- `src/app/client/workspace/runtime/render.cljs`
- optional new helpers for glyph data packing if needed

## External resource anchors

- `external-resources/slug/experiments.md`
- `external-resources/synthesis.md`

## Session target

Land a real Slug-backed rendering path in the codebase, not just notes or theory.

The work should be staged, but aimed at integration:

1. ingest font outline data
2. pack the Slug-specific GPU data
3. build the WebGPU Slug pipeline
4. render glyphs at multiple scales
5. connect the path to a live runtime surface

## Implementation stages

### Stage 1. Font outline ingestion

Pick the first production target font and extract the outline information needed for Slug rendering.

Likely requirements:

- glyph curves
- contour / band information
- bounding geometry
- any preprocessing needed for GPU upload

If a small helper script is needed to preprocess the font asset, keep it explicit and checked into the repo if it becomes part of the workflow.

### Stage 2. GPU data model

Define the resources Slug needs, likely including:

- curve texture or buffer
- band texture or buffer
- glyph metadata lookup
- polygon vertices / indices

Make the data model legible enough that future fonts are not hardcoded chaos.

### Stage 3. WebGPU pipeline

Add the dedicated Slug pipeline:

- vertex shader
- fragment shader
- bind group layout
- pipeline creation
- draw path

Keep this as a named rendering path in the renderer, not hidden inside the MSDF path.

### Stage 4. Runtime integration

Connect Slug to a real runtime surface.

Preferred order:

1. render selected glyphs or selected text runs correctly
2. render a real text surface in-app
3. expand from experimental surface toward broader runtime use

If a temporary flag is useful, use one. The goal is still integration, not permanent isolation.

### Stage 5. Extreme zoom validation

Exercise the path at the scales that matter to Softland's world thesis:

- very small
- normal editor scale
- very large
- absurdly large

Validate:

- edge crispness
- corner fidelity
- punctuation clarity
- stroke stability
- behavior under DPR changes

## Definition of done

For this session to count as successful, there should be:

- a working Slug rendering path in the codebase
- real glyph rendering on WebGPU/WGSL
- runtime-visible output, not only offline experiments
- enough integration that the path can be extended rather than restarted

## Non-goals

- do not spend the session relitigating whether Slug is worth doing
- do not let the work collapse into only screenshots or only a one-glyph dead-end unless a hard technical blocker forces it
- do not over-polish fallback architecture before the core Slug path renders successfully

## Practical session posture

- build vertically
- keep the path visible in the app as early as possible
- use Session 1 instrumentation to watch memory consequences
- preserve the existing MSDF path while the Slug path is coming online

## Follow-through after the session

If Slug lands successfully, the next questions are:

- where it becomes the default
- whether MSDF remains as compatibility or fallback
- how text layout and future prose surfaces target the new path

Those are follow-up sessions, not prerequisites for this one.
