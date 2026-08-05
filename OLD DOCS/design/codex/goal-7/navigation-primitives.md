# Navigation Primitives

Status: drafted, 2026-06-09.

## Origin Prompt

```text
I want to know what changes at each zoom level, what survives every scale
change, how identity/provenance/relation/truth-state are preserved, how the user
knows where they are, and what small prototype slices can test this without
building the whole global map.
```

## Goal

Define the navigation primitives Softland needs for ZUI as lawful compression
and orientation across scales.

## Inputs Read

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-7/description.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/current-mental-model/architecture/object-container-spec.md`
- `docs/current-mental-model/architecture/object-container-ingester-contract.md`
- `docs/current-mental-model/build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md`
- External sources listed in `zui-research.md`.

## Scope

This document names primitives and model obligations. It does not specify screen
layout, styling, animation, or implementation.

## Core Claim

Softland navigation is not "move camera around a graph." It is "follow a stable
identity through lawful changes of scale, projection, evidence, and action while
remaining oriented."

## Findings / Design Decisions

1. Navigation starts with a semantic address, not a viewport coordinate.
2. The main thread through zoom is identity, not camera position.
3. Source reveal and relation follow must both preserve way back.
4. Compression needs a ledger so aggregates do not become false ontology.
5. Every gesture needs an action boundary: reveal, ViewState, ActionRequest,
   substrate inspection, or code change.

## Primitive 1 - Semantic Address

Purpose:

```text
tell the user where they are in the world of understanding
```

Minimum shape:

```text
semantic-address
  world: local-world id or aggregate region id
  scale: 0.001 / 0.01 / 0.1 / 1.0 / 10.0 / 100.0
  projection: outline / topology / source / timeline / substrate / code / etc.
  focus: object id / relation id / source span / aggregate id
  scope: included sources, filters, relation depth, time window
  state: truth/provenance summary
  trail: entry path and prior focuses
```

What this makes visible:

- Current world.
- Current scale.
- Current projection.
- Current focus.
- Current scope/filter.
- Current truth/provenance health.

What this keeps folded but recoverable:

- Full source bodies.
- Full trail history.
- Full projection policy.
- Full relation neighborhood.

What this must not imply:

- That the current projection is the whole truth.
- That a local-world boundary is global ontology.

Failure modes:

- The user knows what they selected but not what world/scope they are inside.
- The user sees a source span but not which native object it anchors.

Acceptance checks:

- At any point, the view can answer: "where am I, what am I focused on, what
  scale is this, what projection is this, and how did I get here?"

## Primitive 2 - Identity Thread

Purpose:

```text
keep one thing recognizably itself across scale and projection changes
```

Model obligation:

- Selection is keyed to model identity, not display node.
- A selected object remains selected across outline/topology/source/substrate.
- Aggregates expose member identity recovery.
- Source span and native object are linked both ways.

What this makes visible:

- The same object's appearances across scales.
- The difference between source, derived unit, graduated object, relation, and
  placement.

What this keeps folded but recoverable:

- Alternate representations.
- Full revision history.
- Non-current placements.

What this must not imply:

- That an aggregate is a singular object.
- That a placement is the object.

Failure modes:

- User zooms into a domain and lands on a different object than the one implied.
- Same object appears as separate copies in outline and graph.

Acceptance checks:

- Select an imported object, switch projection twice, zoom in to source, zoom
  out to topic, and verify identity is stable.

## Primitive 3 - Zoom Ladder

Purpose:

```text
make scale legible and bounded without forcing a premature global map
```

Semantic stops:

```text
0.001  knowledge landscape
0.01   domain
0.1    topic / local world
1.0    ground interaction
10.0   substrate / projection machinery
100.0  code / bedrock
```

Model obligation:

- Each stop has a user question.
- Each stop has visible entities and folded entities.
- Each transition has a compression policy.
- Continuous zoom may interpolate, but state changes resolve to named stops.

What this makes visible:

- What level the user is at.
- What kind of action is appropriate.
- What kind of entity is being viewed.

What this keeps folded but recoverable:

- Numeric internal zoom values.
- Intermediate animation.
- Non-current level detail.

What this must not imply:

- That each stop is a different app mode.
- That the same node-link graph should appear at every level.

Failure modes:

- User zooms but cannot tell whether they changed detail, scope, or truth.
- Scale labels exist but do not change the entity model.

Acceptance checks:

- At each stop, the system can answer: "what is visible, what is folded, what
  survived, and what actions are valid?"

## Primitive 4 - Source Reveal

Purpose:

```text
make transformation inspectable
```

Model obligation:

- Native object reveals source anchor.
- Source span reveals native object(s).
- Relation reveals evidence/decision trail where available.
- Source status is visible before reveal.

What this makes visible:

- Exact raw source span.
- SourceArtifact ref/hash/native id.
- Anchor health.
- Derived/graduated state.
- Transformation path.

What this keeps folded but recoverable:

- Full source body.
- Distiller details.
- Revision history.
- Redaction/acquisition details.

What this must not imply:

- That every object is equally source-backed.
- That source text alone makes a relation accepted.

Failure modes:

- Source is hidden in a modal-like dead end.
- Source reveal loses local context.
- Native object cannot be found from source span.

Acceptance checks:

- From object, reveal exact source. From source, return to object. Local context
  and selection survive both directions.

## Primitive 5 - Relation Follow

Purpose:

```text
move through knowledge structure without losing relation truth-state
```

Model obligation:

- Relation state travels with the edge.
- Relation type and evidence remain recoverable.
- Following a relation records trail step.
- Neighbor scope/depth is bounded.

Relation states:

```text
accepted
candidate
inferred
rejected
projection-derived
aggregate
```

What this makes visible:

- Why this neighbor is shown.
- What kind of edge connects the focus to the neighbor.
- Whether the edge is accepted or proposed.

What this keeps folded but recoverable:

- Distant graph.
- Full evidence list.
- Rejected relation detail.
- Relation decision event.

What this must not imply:

- That visual proximity is an accepted relation.
- That many weak candidate edges equal one accepted bridge.

Failure modes:

- Following a candidate edge feels like following accepted knowledge.
- User cannot return to the prior object/relation.

Acceptance checks:

- Follow accepted and candidate relations. The destination view preserves the
  distinction and offers a way back.

## Primitive 6 - Local Scope Ring

Purpose:

```text
show what is inside the current local world and what is outside
```

Model obligation:

- Current local world has a boundary/interface.
- Neighbor depth is visible.
- Filters and omitted regions are visible or inspectable.
- Disconnected islands are not silently hidden.

What this makes visible:

- Local focus.
- Nearby topology.
- Scope boundary.
- Out-of-scope counts.
- Disconnected or unresolved islands.

What this keeps folded but recoverable:

- Distant domains.
- Full graph.
- Historical routes not used now.

What this must not imply:

- That out-of-scope means unrelated.
- That hidden means absent.

Failure modes:

- Local view feels like the whole world.
- Filtered edges vanish without trace.

Acceptance checks:

- User can explain what is included, what is excluded, and why.

## Primitive 7 - Compression Ledger

Purpose:

```text
make aggregation and folding accountable
```

Model obligation:

Every aggregate can answer:

```text
what members are included?
what policy grouped them?
what truth states are inside?
what provenance states are inside?
what relation states are inside?
what has been hidden?
how do I expand or descend?
```

What this makes visible:

- Aggregation policy.
- Coverage.
- Worst-state or mixed-state summary.
- Hidden detail count.

What this keeps folded but recoverable:

- Member list.
- Exact source spans.
- Per-edge evidence.
- Projection algorithm parameters.

What this must not imply:

- That aggregation is acceptance.
- That generated labels are settled terms.

Failure modes:

- Cluster labels become false ontology.
- Hidden failures disappear from overview.

Acceptance checks:

- Choose any aggregate and explain why it exists without guessing.

## Primitive 8 - Truth-State Beacon

Purpose:

```text
keep calibration visible across scale
```

Model obligation:

- Current focus has truth/provenance/relation status.
- Current visible neighborhood has aggregate status.
- Status survives projection switches and zoom changes.
- Color is never the only state encoding.

States to summarize:

```text
source-backed
derived
graduated
accepted
candidate
inferred
rejected
partial
failed
stale
unanchored
disconnected
projection-derived
AI inference
human-authored
simulation output
```

What this makes visible:

- Whether the map can be trusted.
- Which parts need inspection.
- Which uncertainties are acquisition, transformation, or projection problems.

What this keeps folded but recoverable:

- Detailed uncertainty explanation.
- Evidence.
- Decision/event trail.

What this must not imply:

- That an absence of warnings equals certainty.
- That all uncertainty is bad.

Failure modes:

- At overview, warnings disappear.
- Candidate and inferred material look clean because the surface is clean.

Acceptance checks:

- A mixed-state region remains visibly mixed at every scale.

## Primitive 9 - Way-Back Trail

Purpose:

```text
preserve user's epistemic path
```

Model obligation:

- Navigation records semantic steps, not just viewport changes.
- Steps can fold into named route segments.
- Returning restores focus, scale, projection, and filters.

Trail step:

```text
from focus
relation/action followed
to focus
scale before/after
projection before/after
scope/filter
truth/provenance summary
time/event id if action occurred
```

What this makes visible:

- Entry path.
- Current path.
- Return options.
- Branch/fork points.

What this keeps folded but recoverable:

- Long trails.
- Low-level viewport changes.
- Repeated focus adjustments.

What this must not imply:

- That linear order is causal truth.
- That two parallel exploration trails have one false sequence.

Failure modes:

- Back button returns pixels but not meaning.
- A route through source/code loses the original question.

Acceptance checks:

- User can descend from topic to code and return to the original question with
  context intact.

## Primitive 10 - Action Boundary

Purpose:

```text
separate seeing, projecting, proposing, and changing
```

Model obligation:

Every gesture is one of:

```text
reveal existing model truth
change ViewState only
emit ActionRequest
inspect substrate/provenance
edit code/bedrock
```

What this makes visible:

- Whether the user is browsing, arranging, proposing, accepting, or changing the
  world.

What this keeps folded but recoverable:

- Full ActionRequest payload.
- Full decision event.
- Policy checks.

What this must not imply:

- That dragging equals acceptance.
- That projection layout is model truth.

Failure modes:

- A view gesture silently mutates canonical relation truth.
- A rejected action still appears as if accepted.

Acceptance checks:

- For every prototype interaction, the designer can identify its model
  consequence.

## Navigation Grammar

The primitives compose into a minimal grammar:

```text
enter region
  -> select focus
  -> inspect identity
  -> reveal source/provenance
  -> follow relation
  -> change scale
  -> inspect compression
  -> act through boundary
  -> return by trail
```

This grammar works for source import, local topic navigation, trail replay,
substrate debugging, and zoom-100 code inspection.

## What This Makes Visible

- Orientation.
- Identity continuity.
- Source trust.
- Relation status.
- Truth-state calibration.
- Scope and compression.
- Valid actions.
- Way back.

## What This Keeps Folded But Recoverable

- Full source.
- Full graph.
- Full event history.
- Full projection implementation.
- Full codebase.

## What This Must Not Imply

- That navigation primitives are screen components.
- That every primitive must be visible at once.
- That ZUI requires a giant canvas.
- That early prototypes should build global terrain.

## Failure Modes

- Navigation is spatially smooth but semantically disorienting.
- User can move but cannot explain where they are.
- Identity, source, relation, or truth state changes silently.
- Way back loses filters/focus.
- Compression hides failures.

## Acceptance Checks

1. The current semantic address can be named.
2. The selected identity survives scale and projection change.
3. Source reveal works both ways.
4. Relation follow preserves relation state.
5. Scope boundary is legible.
6. Compression policy is inspectable.
7. Truth/provenance summary survives overview.
8. Way-back trail restores meaning, not just pixels.
9. Every action has a declared model consequence.

## Open Questions

1. Which primitive should be built first in a prototype: semantic address,
   source reveal, or way-back trail?
2. Should compression ledger be visible as normal UI or only through inspection?
3. How should trails represent parallel/branching semantic time?
4. What minimum action-boundary language should be shown before Goal 6?
5. How should local scope ring interact with future canvas/3D views?

## Next Recommended Goal

Run Goal 4 next to make Truth-State Beacon concrete, then Goal 5 to test these
navigation primitives against Softland self-import and Discourse Graph import
scenarios.
