# Staff-Level UI Review Rubric

## Scoring

Score two dimensions separately:
- Visual execution (0-10): hierarchy, typography, contrast, spacing, polish.
- Product clarity (0-10): orientation, state clarity, next-action discoverability, workflow coherence.

Interpretation:
- 9.0-10.0: category-leading.
- 8.0-8.9: premium and ship-ready.
- 6.5-7.9: strong but not benchmark quality.
- 5.0-6.4: functional prototype quality.
- <5.0: significant redesign required.

## Severity Definition

- P0: Broken rendering/interaction invariants or trust-breaking UX defects.
- P1: Major friction that slows experts or causes repeated mistakes.
- P2: Noticeable inconsistency/polish debt with moderate impact.
- P3: Nice-to-have refinement.

## Critique Checklist

1. Rendering Integrity
- Verify no cross-pane bleed.
- Verify clipping for text and overlays.
- Verify cursor/text alignment at long lines.

2. Hierarchy
- Verify one dominant working surface.
- Verify active pane is obvious in <1 second.
- Verify pane headers do not compete with content.

3. Spatial Rhythm
- Verify consistent grid usage.
- Verify gutters and panel padding are consistent.
- Verify list density supports fast scanning.

4. State System
- Verify default/hover/pressed/selected/focus-visible are distinct.
- Verify empty/loading/error states are intentional.
- Verify keyboard focus is visible and not confused with selection.

5. Guidance and Momentum
- Verify empty states provide clear next action.
- Verify command/input surfaces feel anchored and purposeful.
- Verify context layer communicates "where am I?" quickly.

## Acceptance Tests (Baseline)

1. Paste a 500+ character line and confirm no cross-pane pixels render.
2. Confirm selected row, hover row, and keyboard focus are visually distinct.
3. Confirm active pane is identifiable in under 1 second.
4. Confirm command/input bar reads as docked, not floating.
5. Confirm empty states include explicit next-step guidance.
6. Confirm no cursor drift at end of 100+ character lines.
