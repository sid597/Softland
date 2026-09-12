# MVP Plan: Generalized Component Converter (Local Library First)

## Decision
Build the converter as a generalized system now, with constrained coverage.
Do not use Rama depot yet. Store blueprints locally in this repo under `components/by-codex/`.

## Goal
A deterministic pipeline that can convert source UI components (starting with shadcn-style components) into our runtime format and verify fidelity automatically.

## Scope For MVP
- Generalized core pipeline:
  - Source snapshot -> Design IR -> WebGPU-compatible blueprint -> Verification report
- First source adapter:
  - Controlled DOM/screenshot snapshot adapter for shadcn-style components
- First component set:
  - Button, Input, Card, Sidebar block
- First verification contract:
  - Pixel similarity, geometry diff, style diff, state parity

## Out Of Scope For MVP
- Full effect parity for all CSS/SVG edge cases
- Rama persistence and retrieval ranking
- Figma adapter as the first adapter

## Storage Strategy (Current)
- Local component library path: `components/by-codex/`
- All blueprints and schemas are plain data files in-repo
- Each accepted conversion stores:
  - Component blueprint
  - Source metadata
  - Verification metrics

## Proposed Pipeline
1. Snapshot Adapter
- Capture component variants and interaction states in a deterministic harness.
- Normalize computed style and bounds into a source snapshot tree.

2. Deterministic Normalizer
- Convert snapshot tree into canonical Design IR primitives.
- Preserve state variants (`default`, `hover`, `active`, `focus`, `disabled`).

3. Runtime Compiler
- Compile Design IR into rect/text/shadow/layout structures compatible with our current WebGPU runtime.

4. Verifier
- Render source and converted output under same harness.
- Compute fidelity metrics with explicit pass/fail thresholds.

5. Local Library Writer
- Save accepted blueprint + metadata + verification report to `components/by-codex/`.

## Determinism Policy
- Converter and verifier are deterministic and authoritative.
- LLM is optional assistant for fallback suggestions only.
- Acceptance gate is metric-based, not subjective.

## Milestones
1. M1: Schema + library layout
- Finalize Design IR schema and blueprint file contract.

2. M2: Button end-to-end
- Convert one shadcn-style button with all main states.
- Pass verification thresholds.

3. M3: Core primitives coverage
- Add Input and Card.
- Expand style mapping coverage where needed.

4. M4: Sidebar block
- Convert one sidebar composition from reusable primitives.
- Save multiple design variants in local library.

## Acceptance Criteria For This MVP
- Same pipeline handles all four target components.
- Each component has a verification report and deterministic pass/fail output.
- New component conversions can be added without changing converter architecture.
