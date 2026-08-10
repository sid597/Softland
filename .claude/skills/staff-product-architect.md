# Staff-Level Product Architect Guide

This document provides the mindset, mental models, and exact execution standards required to deliver a Staff-Level Product Architecture Document instead of a standard UI/UX critique or basic design system handoff.

A staff-level product architect doesn't just decorate existing screens or define hex codes. They bridge the gap between profound visual hierarchy, rendering engine constraints, reactive system boundaries, and user intent.

## The 4 Pillars of Staff-Level Product Architecture

### 1. Code-Level Actionability for Visual Bugs

Never just point out that "the layout is broken" or prescribe generic CSS fixes when working in custom engines (like WebGPU, Canvas, or custom layout trees).
*   **Junior/Mid:** "The text overlaps the other pane. Add `overflow: hidden`."
*   **Staff-Level:** Diagnose the specific layout invariant failure. Identify the exact function (e.g., `tree->text-ops`), explain *why* it fails (e.g., checks origin but doesn't truncate overflowing character counts), and provide specific implementation paths (shader-level `wgpu.ScissorRect` clip vs. string truncation).

### 2. Product Design Revelation (Questioning the Premise)

Never just accept the screen you are given and try to make it look better. Analyze the data being presented and the user's intent.
*   **Junior/Mid:** "How do we make this code editor look better when displaying `_source.edn`?"
*   **Staff-Level:** "Wait, `_source.edn` is a component's metadata file. The user shouldn't be reading raw EDN in a text editor. We need a context-aware Component Detail View with a 'Convert' CTA."
*   **Rule:** Junior designers style the screen they are given. Principals question if it's the right screen for the user's intent. Design context-aware views instead of generic containers.

### 3. Deep Integration with Existing Primitives

Do not invent isolated design systems or external tokens if the codebase already has a structural primitive in place.
*   **Junior/Mid:** Creates a new list of CSS-style hex variables (`--surface-0: #111`).
*   **Staff-Level:** Investigates the current engine, sees that spacing uses a specific design token map (e.g., `(:spacing dt)`), and formulates exact structure representations (like RGBA arrays `[0.04 0.05 0.07 1.0]`) natively mapped to the platform's render pipeline. Write specs that can be copy-pasted into the engine.

### 4. Spatial Environments over Flat Rectangles

Modern UI requires shifting from "rendering flat rectangles" to "simulating a physical, spatial environment."
*   **Junior/Mid:** Uses harsh 1px borders to separate panes and flat muddy colors.
*   **Staff-Level:** Replaces harsh lines with background luminance contrast (Surface Elevation). The active/focused pane gets a lighter surface; inactive panes get a darker surface. The luminance difference creates natural soft edges without explicit lines. Establish rigorous spatial rhythms (e.g., a strict 4px grid for margins/padding).

## Execution Guardrails

When delivering your architecture document, always include:

1.  **P0 Rendering Invariants:** Before any visual polish, list the foundational layout or sync rules that must never be broken (e.g., Text/Cursor Alignment Sync mathematically tied to a specific multiplier).
2.  **Engineering Guardrails:** Explicitly list reactive architecture constraints. If you know the engine uses specific reactivity patterns (e.g., Electric Clojure `m/latest` vs `m/ap`), document how the UI state matrix must interact with these without causing crashes.
3.  **State Matrix & Phased Delivery:** Define deterministic states (Default, Hover, Pressed, Selected, Selected+Focused, Focus-visible, Disabled, Loading, Empty, Error) and break down implementation into realistic phases, avoiding scope creep in Phase 1.