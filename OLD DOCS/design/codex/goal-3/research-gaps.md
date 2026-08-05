# Research Gaps After Goal 3

Status: draft research sweep, 2026-06-09.

## Origin Prompt

```text
Continue the Softland design/view research track. Execute Goal 3 only. Build a
precedent atlas, not a moodboard. Include research gaps, especially around HCI,
ZUI, visualization uncertainty, and collaborative sensemaking.
```

## Goal

Identify what this broad precedent sweep still does not settle, so future goals
can deepen the right questions without reopening everything.

## Inputs Read

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-3/description.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/design/codex/goal-1/description.md`
- `docs/current-mental-model/design/codex/goal-2/description.md`
- External sources cited in `precedent-atlas.md` and `annotated-bibliography.md`

## Scope

This document is not a backlog. It names missing research areas and unresolved
questions that matter for truthful Softland views. The goal is to protect future
design work from pretending Goal 3 answered more than it did.

## Core Claim

The atlas is strong enough to choose first primitives, but not strong enough to
settle visual grammar, collaboration semantics, semantic-time rendering,
projection truth, or the precise source-to-native data contract. Those need
focused follow-up passes.

## Findings Or Design Decisions

- The first gap to close is truth-state grammar. Many references warn about
  uncertainty, but none gives Softland's exact state system.
- The second gap is source-to-native granularity. The atlas says mapping must be
  bidirectional; Goal 2 or Goal 6 must say which current rows and queries can
  support that.
- The third gap is collaborative sensemaking. Figma and DataHub show pieces of
  shared editing and manual lineage, but Softland needs authority, provenance,
  disagreement, and synthesis states.
- The fourth gap is semantic time. Trace tools are useful, but Softland's time
  is a DAG of causality over an append-only substrate, not only a linear
  timeline.

## Gap 1: Truth-State Visual Grammar

What is missing:

- Exact definitions for raw source, native object, derived unit, graduated object, source anchor, accepted edge, candidate edge, partial parse, stale anchor, unanchored object, disconnected island, projection, and local-world placement.
- Redundant visual encodings beyond color.
- Interaction semantics for each state.
- How states compose: for example, accepted relation attached to stale source anchor.

Why it matters:

Softland's first design-system question is "How does truth appear?" Without a
truth-state grammar, any topology view risks making candidate or inferred
structure look accepted.

References that point at the gap:

- MacEachren uncertainty visualization: https://cartographicperspectives.org/index.php/journal/article/view/cp13-maceachren
- DataHub lineage: https://docs.datahub.com/docs/features/feature-guides/lineage/
- Principal local design context: `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`

Suggested next work:

- Run Goal 4.
- Produce a state matrix with name, definition, visual encoding, interaction,
  model requirement, dangerous implication, and acceptance check.

Acceptance checks:

- Users can distinguish accepted/candidate/inferred/partial/stale/unanchored
  without relying on color.
- Every relation edge has an authority state.
- Every source anchor shows whether it is exact, partial, stale, or missing.

## Gap 2: Source-To-Native Mapping Granularity

What is missing:

- Minimum source-map schema for markdown, transcript, code, Roam/DG, Linear, and Softland docs.
- Whether mappings are character spans, block spans, semantic fields, row IDs, or hybrid.
- How to represent partial, ambiguous, generated, or many-to-many mappings.
- Query requirements for hover/highlight in both directions.

Why it matters:

The atlas says source maps are the strongest precedent. But Softland cannot
borrow source maps abstractly; it needs an explicit mapping contract.

References that point at the gap:

- ECMA-426 Source Maps: https://tc39.es/source-map/
- Chrome DevTools source maps: https://developer.chrome.com/docs/devtools/javascript/source-maps
- Goal 2 description: `docs/current-mental-model/design/codex/goal-2/description.md`

Suggested next work:

- Re-run Goal 2 if the current substrate map does not exist.
- In Goal 6, require a source-to-native hover contract with implemented versus missing queries.

Acceptance checks:

- Native object -> raw source span works.
- Raw source span -> native object works.
- Partial or missing mapping is visible, not silently rounded to a broader span.

## Gap 3: Collaborative Sensemaking And Authority

What is missing:

- Distinction between personal annotation, team annotation, candidate relation, accepted relation, rejected relation, and system-derived relation.
- Visual language for disagreement held structurally.
- How manual lineage edits coexist with automatic lineage.
- How synthesis appears when disagreement becomes accepted structure.

Why it matters:

Softland values multi-LLM and multi-person disagreement as frontier material.
Collaboration cannot mean shared cursors alone. It must preserve authority,
provenance, disagreement, and the path to synthesis.

References that point at the gap:

- Figma collaboration and version history: https://help.figma.com/hc/en-us/articles/360038006754-View-a-file-s-version-history
- Figma multiplayer limits: https://help.figma.com/hc/en-us/articles/1500006775761-How-many-people-can-be-in-a-file-at-once
- DataHub manual lineage caveats: https://docs.datahub.com/docs/features/feature-guides/lineage/
- Ink and Switch malleable software: https://www.inkandswitch.com/essay/malleable-software/

Suggested next work:

- Add an "authority state" dimension to Goal 4.
- Later run a dedicated collaborative-sensemaking goal covering comments,
  decisions, accepted/rejected edges, and preserved disagreement.

Acceptance checks:

- A personal pin cannot be mistaken for accepted truth.
- A manual relation cannot silently overwrite an automatic relation.
- A disagreement can be revisited with both sides and source anchors intact.

## Gap 4: Semantic Time And DAG Causality

What is missing:

- How to visualize semantic time as a DAG while preserving the usability of a timeline.
- How to display parallel agent work, retries, rejected attempts, and synthesized outcomes.
- Whether trails, traces, revisions, and action requests share one temporal substrate or separate views.

Why it matters:

Perfetto and OpenTelemetry give strong trace precedents, but Softland's model
explicitly warns that linear logs create false causality when multiple agents
work in parallel.

References that point at the gap:

- Perfetto UI: https://perfetto.dev/docs/visualization/perfetto-ui
- OpenTelemetry tracing overview: https://opentelemetry.io/docs/reference/specification/overview/
- `docs/vision/epistemic-framework.md`

Suggested next work:

- Later produce a semantic-time design note: timeline lane plus causal links,
  or DAG-first with timeline projection.

Acceptance checks:

- Parallel transformations do not render as a false sequence.
- A retry is distinguishable from a new independent attempt.
- A synthesized object can show which events and source anchors contributed.

## Gap 5: Projection And Embedding Calibration

What is missing:

- Policy for when embedding maps are allowed in Softland.
- Required projection metadata: method, parameters, input set, revision, stability, interpretation warnings.
- Visual distinction between proximity, candidate relation, and accepted relation.
- Tests for whether users overread clusters.

Why it matters:

Softland will be tempted by "knowledge landscape" views. Without calibration,
embedding maps become persuasive fiction.

References that point at the gap:

- Distill t-SNE caution: https://distill.pub/2016/misread-tsne/
- UMAP parameters: https://umap-learn.readthedocs.io/en/latest/parameters.html
- Munzner nested model: https://vis.csail.mit.edu/classes/6.859/readings/pdfs/Munzner-ANestedModelForVisualizationDesignAndValidation.pdf

Suggested next work:

- Keep embedding maps out of the first prototype unless they include a
  `ProjectionHealthPanel`.
- Later run a projection-specific research pass before any "knowledge landscape"
  prototype.

Acceptance checks:

- Users do not describe projection neighbors as accepted links unless an edge
  exists.
- Changing projection parameters produces a visible projection revision.
- Projection warnings are understandable without academic background.

## Gap 6: Pacing And Cognitive Dosage

What is missing:

- Rules for how much structure appears at once.
- Defaults for folded neighborhoods, warning aggregation, candidate counts, and staged reveal.
- How pacing changes by user mode: review, exploration, handoff, debugging.

Why it matters:

Softland's framework treats pacing as epistemic machinery, not polish. The
atlas gives many orientation references, but not a dosage policy.

References that point at the gap:

- Shneiderman task taxonomy: https://hci.ucsd.edu/220/EyesHaveIt.pdf
- Lightroom view modes: https://helpx.adobe.com/au/lightroom-classic/help/view-photos.html
- Legible London: https://content.tfl.gov.uk/ll-yellow-book.pdf

Suggested next work:

- Add pacing constraints to Goal 6 acceptance checks.
- Prototype folded counts and reveal actions before dense graph expansion.

Acceptance checks:

- First view does not show more topology than needed to answer the selected
  trust question.
- A user can reveal more without losing the path back.
- Warning aggregation does not hide severe parse/source failures.

## Gap 7: Game Precedents Need Stronger Primary Evidence

What is missing:

- First-party sources for specific game UI mechanics such as Outer Wilds rumor
  mode, Zelda pins, minimaps, and fog-of-war player experience.
- Comparable examples from strategy games, immersive sims, and mystery games
  with durable design talks or manuals.

Why it matters:

Game precedents are conceptually useful for local entry, fog, landmarks, and
knowledge-first progression, but several sources are community-maintained rather
than primary.

References used in this pass:

- Outer Wilds official page: https://www.mobiusdigitalgames.com/outer-wilds.html?pubDate=20250223
- Outer Wilds computer/ship log documentation: https://outerwilds.fandom.com/wiki/Computer
- Fog of War in AFSIM: https://journals.sagepub.com/doi/10.1177/15485129211041963
- Zelda pins: https://zeldawiki.wiki/wiki/Pin

Suggested next work:

- If games become central to Goal 7 or a semantic-zoom prototype, gather
  first-party manuals, GDC talks, or direct screenshots with usage notes.

Acceptance checks:

- Any game-derived primitive has at least one durable source and one Softland
  test that does not depend on liking the game.

## Gap 8: Architecture Beyond Wayfinding

What is missing:

- Deeper architectural precedent for thresholds, rooms, districts, renovation,
  adaptive reuse, and public/private spatial gradients.
- How "place" translates into view constraints without becoming metaphor.

Why it matters:

Softland is a place made of software. Lynch and Legible London cover
orientation, but the project may also need architecture research on habitability,
maintenance, renovation, and public form.

References used in this pass:

- Kevin Lynch: https://mitpress.mit.edu/9780262620017/the-image-of-the-city/
- Legible London: https://content.tfl.gov.uk/ll-yellow-book.pdf
- Pattern Language: https://www.patternlanguage.com/

Suggested next work:

- Later run a place/habitability precedent pass only if Goal 6 needs spatial
  laws beyond Source-to-World trust.

Acceptance checks:

- Architecture language yields concrete primitives such as entry, threshold,
  landmark, district, and route, not decorative metaphors.

## Gap 9: Malleability Sequencing

What is missing:

- When users can modify views.
- What is a local patch versus shared canonical view.
- How patches are versioned, reviewed, merged, or rejected.
- How malleability interacts with truth-state and lineage.

Why it matters:

Malleability is core to Softland's long-term identity, but if it arrives before
trust grammar, it can damage the map.

References that point at the gap:

- Ink and Switch malleable software: https://www.inkandswitch.com/essay/malleable-software/
- Dynamicland: https://dynamicland.org/2024/FAQ/
- Folk Computer: https://folk.computer/

Suggested next work:

- Defer user-programmable views from the first Source-to-World prototype.
- Specify `ViewPatch` only after accepted/candidate/manual/automatic authority
  states exist.

Acceptance checks:

- Every user modification is traceable to base view, owner, reason, and affected
  objects.
- Local modifications cannot silently alter accepted model truth.

## What This Makes Visible

- The atlas is actionable but incomplete.
- Truth grammar, mapping granularity, collaboration authority, semantic time,
  and projection calibration require focused work.
- Some gaps are blockers for the first prototype; others are sequencing notes.

## What This Keeps Folded But Recoverable

- Full reference notes remain in `precedent-atlas.md`.
- Hard product calls remain in `borrow-reject-table.md`.
- Source URLs remain in `annotated-bibliography.md`.

## What This Must Not Imply

- These gaps are not a roadmap.
- The first Source-to-World prototype should not wait for every gap to close.
- "Study more" is not permission to avoid building once the trust-inspector
  contract is clear.

## Failure Modes

- Research infinity: every unresolved question becomes a reason not to prototype.
- Premature synthesis: collaboration, semantic zoom, and malleability are merged
  before the first trust view works.
- Overclaiming: the atlas is treated as proof that a design will work.
- Metaphor drift: place, fog, contact sheet, and minimap become vibes rather
  than testable primitives.

## Acceptance Checks

- A future session can choose Goal 4 without rereading the whole atlas.
- A future Goal 6 session can identify which gaps block the first buildable
  Source-to-World View and which can wait.
- Each gap names sources, why it matters, suggested next work, and acceptance
  checks.

## Open Questions

- Should Goal 4 include authority state, or should authority/disagreement be a
  later collaboration-specific goal?
- Should Goal 2 be run before Goal 4 if current source-to-native mapping
  support is uncertain?
- Should semantic time get its own goal before trail persistence work resumes?

## Next Recommended Goal

Goal 4: truth-state visual grammar. The most immediate blocker for truthful
views is not more reference collection; it is deciding how Softland visibly
distinguishes fact, candidate, inference, uncertainty, stale grounding,
disconnection, and failure.
