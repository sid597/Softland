# Truth-State Visual Grammar

Status: revised Goal 4 draft after reading completed Goals 1-3, 2026-06-09.

## Origin prompt

```text
Use this for the actual visual language.

Continue the Softland design/view research track.

Read:
- docs/current-mental-model/design/codex/orientation.md
- docs/current-mental-model/design/codex/goal-4/description.md
- goal-1 outputs if present
- goal-2 outputs if present
- goal-3 outputs if present

Execute Goal 4 only.

Design the truth-state visual grammar for Softland: raw source, SourceArtifact,
ObjectContainer, DerivedUnit, graduated object, SourceAnchor, accepted edge,
candidate edge, inferred relation, stale anchor, unanchored object, projection,
local-world placement, AI inference, human-authored fact, simulation output,
etc.

This is not normal component design. Start from how truth, uncertainty,
provenance, derivation, and relation appear.
Include anti-lie rules.

Do not design the whole screen yet. Do not commit docs.
```

Follow-up correction:

```text
can you recheck if other 3 goals are done if so use that and redo imo
```

## Goal

Define the first Softland visual grammar for truth, uncertainty, provenance,
derivation, relation, projection, locality, authority, and action state.

This grammar must inherit the completed prior goals:

```text
Goal 1:
  views are epistemic instruments, not ordinary UI;
  source-to-native trust is the first proof;
  truth state is a design primitive.

Goal 2:
  current view truth is code-grounded for markdown and transcript;
  source-specific projections are useful but not canonical truth;
  RelationEdge, ViewState, ContextMembership, LayerOverlay, code/Roam/Linear
  ingesters, and source health bundles are not currently implemented.

Goal 3:
  borrow source maps, trace viewers, lineage, local topology, uncertainty
  visualization, recoverable folding, and projection health;
  reject global graph, embedding-map certainty, clean-summary-as-view, and
  projection laundering.
```

## Inputs read

```text
docs/current-mental-model/design/codex/orientation.md
docs/current-mental-model/design/codex/goal-4/description.md

Goal 1:
docs/current-mental-model/design/codex/goal-1/charter.md
docs/current-mental-model/design/codex/goal-1/view-laws.md
docs/current-mental-model/design/codex/goal-1/track-glossary.md

Goal 2:
docs/current-mental-model/design/codex/goal-2/current-substrate-map.md
docs/current-mental-model/design/codex/goal-2/import-material-matrix.md
docs/current-mental-model/design/codex/goal-2/view-data-contract.md
docs/current-mental-model/design/codex/goal-2/missing-view-queries.md

Goal 3:
docs/current-mental-model/design/codex/goal-3/precedent-atlas.md
docs/current-mental-model/design/codex/goal-3/borrow-reject-table.md
docs/current-mental-model/design/codex/goal-3/research-gaps.md
docs/current-mental-model/design/codex/goal-3/annotated-bibliography.md

Architecture/design source:
docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md
docs/current-mental-model/architecture/object-container-spec.md
docs/current-mental-model/architecture/object-container-ingester-contract.md
```

## Scope

This document defines screen-agnostic visual grammar. It can be reused later by
Source World, Native World, Trust World, outline, graph, topology, timeline,
inspector, canvas, local-world, trace, and semantic-zoom views.

This document does not define:

```text
full screen layout
component library implementation
final palette
final icon artwork
keyboard map
responsive behavior
Goal 5 scenarios
Goal 6 first buildable view spec
```

## Core claim

Softland's first visual language is not brand style. It is calibration.

A mark in Softland must disclose what kind of thing it is, where it came from,
how grounded it is, whether it is accepted/candidate/inferred/partial/stale/
failed/disconnected/unanchored, whether it is common base truth or projection,
and what action can be taken without overstating certainty.

The core visual question is:

```text
What does truth look like before it becomes a screen?
```

The answer is a grammar of accountable transformation:

```text
raw source -> SourceArtifact -> DerivedUnit/ObjectContainer -> Revision
  -> SourceAnchor -> CompositionEdge / future RelationEdge
  -> Projection / local view / action / trace
```

But the grammar must also show what does not exist yet:

```text
no common semantic RelationEdge rows yet
no common ViewState rows yet
no ContextMembership / local-world rows yet
no LayerOverlay rows yet
no code, Roam/DG, Linear, or Softland-doc-specific ingesters yet
no global source inventory query yet
no hydrated source/object/trust bundle yet
no source-range-to-target query yet
```

Missing substrate is a truth state. Unknown is a truth state. Unimplemented is a
truth state. The view must not silently fill these gaps with visual confidence.

## The grammar stack

Each visible mark should be composed from six layers. The layers can compress
at overview scale, but they should not collapse into one ambiguous style.

```text
1. Material body:
   raw source, SourceArtifact, ObjectContainer, DerivedUnit, Revision,
   projection row, operational row, simulation output, or future/missing row.

2. Substrate badge:
   common base truth, source-specific projection, operational state, view-only
   state, future/unimplemented, or unknown because query missing.

3. Grounding channel:
   healthy SourceAnchor, partial anchor, stale anchor, missing anchor,
   unanchored, source-only, or unavailable.

4. Truth-state glyph:
   accepted, candidate, inferred, rejected, partial, failed, stale,
   disconnected, pending, unresolved disagreement, or unknown.

5. Relation/placement channel:
   CompositionEdge, future RelationEdge, candidate/inferred relation,
   SourceAnchor, projection proximity, local-world placement, or no relation.

6. Agency/authority mark:
   human-authored, AI-inferred, import-policy-derived, system/operational,
   simulation-produced, personal annotation, team annotation, accepted audit.
```

This is the Softland `TruthStateGlyph`: not one icon, but a stack of redundant
signals that travels with the object across projections.

## State families

### 1. Source and capture states

Source states answer:

```text
what came in?
what source version is this?
what source family does it belong to?
what raw body can be inspected?
what source-specific status exists?
```

Visual grammar:

```text
Raw source:
  preserved source slab or excerpt, source rail, source-family label.

SourceArtifact:
  immutable source body with version/hash cap and ref label.

SourceVersion / latest-by-ref:
  version cap, freshness mark, latest/current marker.

Source-only failure:
  blocked source segment with no native body.

Transcript source line:
  line-granular source artifact with byte offset/hash mark.

Markdown source:
  document source artifact with full raw text and block span affordances.

Future source family:
  ghosted source-family placeholder labelled "not imported in common substrate".
```

Goal 2 grounding:

```text
Markdown and transcript source artifacts are implemented.
Code/Roam/DG/Linear/Softland-doc-specific source families are not implemented
as common object-container ingesters.
```

### 2. Native material states

Native states answer:

```text
what became native?
does it have durable identity?
is it derived until touched?
does it have revisions?
```

Visual grammar:

```text
ObjectContainer:
  solid identity shell, kind mark, stable selection handle.

DerivedUnit:
  hollow/dashed shell, distiller mark, source stripe, no durable identity bead.

Graduated object:
  solid identity shell plus retained derived notch and source stripe.

Revision:
  content stratum or ledger tick attached to object shell.

NativeIdentityClaim:
  native-id badge with source-family/native-id reveal.

Projection-only row:
  lens shell, no native identity shell.

Operational row:
  run/status skin, no native object shell unless linked to a common target.
```

Goal 2 grounding:

```text
Markdown creates one document ObjectContainer immediately; markdown blocks are
DerivedUnits until graduation through object/edit.

Transcript creates conversation/message/tool-call/tool-result ObjectContainers
when source lines parse; parse-error lines create no containers.
```

### 3. Grounding and source-map states

Grounding states answer:

```text
can I move from native object to source?
can I move from source span to native target?
how exact is the mapping?
is the source still current?
```

This is the `SoftlandSourceMap` primitive from Goal 3.

Visual grammar:

```text
healthy anchor:
  paired source/native highlight plus solid tether.

partial anchor:
  segmented tether or bracket showing known span plus unresolved residue.

stale anchor:
  broken tether with source hash/span drift mark.

unanchored object:
  empty anchor socket attached to native shell.

source span with no native target:
  source-only span socket and "no native target" residue.

unknown mapping:
  unresolved lookup mark; used when the query is missing or hydration has not
  completed.
```

Goal 2 grounding:

```text
Target -> source anchors are available by target id.
Source -> target hover is weaker: source-side anchor refs exist, but no
byte-range overlap query or hydrated range bundle exists yet.
```

Design consequence:

```text
Native-to-source highlight may look exact when the anchor row is known.
Raw-source-to-native hover must show "resolving", "partial", or "not available"
unless the view has actually hydrated the relevant anchors and target rows.
```

### 4. Structural relation states

Relation states answer:

```text
what connects to what?
is this containment/order or semantic relation?
is this accepted, candidate, inferred, rejected, or unavailable?
```

Visual grammar:

```text
CompositionEdge:
  solid bracket/rail/containment/order connector with edge type label.

Accepted RelationEdge:
  solid typed semantic connector, but only when a real RelationEdge exists.

Candidate relation:
  dashed typed connector with proposal source and accept/reject path.

Inferred relation:
  dotted typed connector with AI/model/policy/procedure mark.

Rejected relation:
  recessed or struck connector, absent from accepted topology but recoverable.

Disconnected island:
  shoreline boundary and count, with no fake bridge.

Projection proximity:
  no edge styling; must sit under projection lens and method label.
```

Goal 2 grounding:

```text
CompositionEdge exists today.
RelationEdge does not exist today in the current object-container substrate.
Candidate relation substrate was not found.
```

Design consequence:

```text
In current markdown/transcript views, structural edges can be shown as current
truth. Semantic RelationEdges must be shown only as future, candidate/inferred
design states, or absent.
```

### 5. Projection and read-model states

Projection states answer:

```text
am I seeing the model, or a lens/read model/index over it?
what did this projection use as input?
what can its layout/order/proximity be trusted to mean?
```

This is the `ProjectionHealthPanel` family from Goal 3, reduced here to visual
grammar rather than a panel.

Visual grammar:

```text
projection lens:
  boundary or watermark that names the projection.

source-specific projection:
  lens plus source-family tag and target-id/common-row coverage mark.

projection health:
  input set, method, parameters/revision if meaningful, and interpretation
  warning when layout/proximity can be overread.

projection-only item:
  lens shell, not object shell.

unknown projection membership:
  unresolved membership mark when explanation query is missing.
```

Goal 2 grounding:

```text
Markdown outline and transcript conversation/tool/audit/source-line rows are
source-specific projections/indexes, not canonical object truth.
```

### 6. Warning, partial, failed, and unknown states

Warning states answer:

```text
what did not work?
what is only partial?
what is unknown because the query does not exist?
what is source-specific rather than common?
```

Visual grammar:

```text
partial parse:
  segmented coverage bar or segmented shell.

parse failure:
  blocked source segment with failure scope.

missing query:
  unresolved data socket labelled by missing read contract.

unknown health:
  neutral unknown mark, not success mark.

source-specific operational status:
  run/status strip with source-family badge.

not implemented source family:
  future substrate ghost, disabled but explicit.
```

Goal 2 grounding:

```text
There is no common source-health query.
Transcript has richer run/source-line/file-offset state than markdown.
No common global source inventory query exists.
No hydrated trust/source/topology bundle exists yet.
```

### 7. Locality, placement, and view state

Locality states answer:

```text
where am I?
is this a model-backed membership, a placement, or just view state?
does spatial nearness mean anything?
```

Visual grammar:

```text
you-are-here stack:
  current local world / source / object / projection / return path.

local-world placement:
  placement pin distinct from edge styling.

view state:
  subtle chrome for selection, hover, collapse, filter, zoom, and xy.

ContextMembership:
  membership badge only when model row exists.

future membership:
  ghosted placement pin labelled "not in current substrate".
```

Goal 2 grounding:

```text
ViewState and ContextMembership rows were not found in the current substrate.
View-specific collapse/selection/zoom/xy must not look like model truth.
```

### 8. Agency, authority, and disagreement states

Agency states answer:

```text
who or what produced this assertion?
who accepted it?
is it personal, team, imported, AI-inferred, simulated, or system-derived?
is disagreement preserved?
```

Goal 3 explicitly called authority state a gap for Goal 4.

Visual grammar:

```text
human-authored:
  author/time mark separate from truth-state glyph.

AI inference:
  dotted inference skin plus model/run provenance.

simulation output:
  run/output skin plus inputs/parameters/seed/version reveal.

import policy output:
  policy/distiller mark plus source-map channel.

personal annotation:
  personal scope mark, never accepted truth styling.

team annotation:
  shared annotation mark, still distinct from accepted model state.

accepted decision:
  audit stamp attached to object/edge/status change.

unresolved disagreement:
  split/forked glyph preserving sides and source anchors.

synthesis:
  accepted transformation mark that points back to preserved disagreement.
```

Design consequence:

```text
Authority and agency are not the same as truth. Human-authored does not mean
verified. AI-inferred does not mean accepted. Simulation-produced does not mean
observed. Team-shared does not mean canonical.
```

## Visual principles

### 1. Truth state is always redundant

Every important truth state needs at least two encodings. Core trust states
need three.

Use combinations of:

```text
body shape
stroke pattern
line pattern
label
glyph slot
source stripe/socket
projection lens
placement pin
opacity
interaction affordance
tooltip/inspector language
```

Color may amplify. Color may not carry the distinction alone.

### 2. Common base truth and projection must look different

Base rows get object/source/edge bodies. Projection rows get lens bodies.

Goal 2 makes this non-negotiable:

```text
OutlineNodeRow is useful, not canonical object truth.
Transcript projections/indexes are useful, not canonical object truth.
Operational run/file-offset status is useful, not common object truth.
```

### 3. Source map precision must be visible

Exact, partial, stale, missing, source-only, and unknown mapping states must not
look alike.

The strongest precedent from Goal 3 is source maps. The danger is source-map
theater: drawing a confident tether when only a coarse or unhydrated mapping
exists.

### 4. Composition is not semantic relation

CompositionEdge can be shown today. RelationEdge cannot be shown as current
common truth yet.

Use:

```text
brackets/rails/containment/order for composition
typed connectors for semantic relations
dashed/dotted treatment for candidate/inferred semantic relation
projection lens for proximity/cluster/order
```

### 5. Missing substrate must have a visual grammar

If a view cannot answer because the substrate has no row or no query, that must
appear as "unknown", "not implemented", "not hydrated", or "source-specific".

Do not use empty space to represent missing truth. Empty space looks like
success.

### 6. Recoverable fold leaves residue

Folded material must leave visible residue:

```text
anchor count
unanchored count
stale count
candidate count
inferred count
failed count
partial count
projection label
source family
query missing mark
```

This is the `RecoverableFold` primitive from Goal 3.

### 7. Local entry precedes global certainty

The grammar should work first for:

```text
one source
one native object
one source anchor
one composition edge
one projection row
one failure
one candidate/inferred proposal
```

Then it can scale into local topology and semantic zoom.

### 8. Agency is not acceptance

Use separate channels for:

```text
who produced it
who accepted it
what source grounds it
what model row stores it
what projection displays it
```

Do not compress these into one "trusted" visual mark.

## Encoding rules

### Body language

```text
source slab:
  raw source material and SourceArtifact body.

solid shell:
  durable ObjectContainer.

hollow/dashed shell:
  DerivedUnit.

solid shell with derived notch:
  graduated ObjectContainer.

strata/ledger tick:
  Revision or accepted decision event.

lens shell:
  projection/read model/index.

run/status strip:
  operational state.

ghost substrate:
  future/unimplemented source family, relation type, membership, layer, or
  query.
```

### Truth-state line language

```text
solid:
  accepted in current model.

dashed:
  candidate/proposed, not accepted.

dotted:
  inferred/computed/AI/policy-derived, not accepted.

broken:
  stale/drifted/missing target.

segmented:
  partial coverage or partial parse.

blocked:
  failure.

recessed/struck:
  rejected, retained in audit/history.

hollow socket:
  missing anchor, missing source, missing query, or ungrounded object.
```

### Substrate badges

```text
base:
  common object-container truth.

projection:
  source-specific read model or view projection.

ops:
  operational harvest/watch/run/file-offset/source-line state.

view:
  hover, selection, collapse, zoom, filter, xy.

future:
  concept exists in architecture but no current row/query exists.

unknown:
  could be true, but current read path cannot answer.
```

### Source-map channel

```text
exact:
  bidirectional highlight possible with hydrated anchor row and target row.

target-to-source:
  target anchor known; reverse lookup may still be unresolved.

source-to-target resolving:
  source-side refs exist but hydrated range/target query is missing or pending.

partial:
  mapping covers only part of source/native material.

stale:
  source hash/span/version drift detected.

none:
  no SourceAnchor.

not implemented:
  source family lacks common ingester.
```

### Authority channel

```text
human:
  author/imported human-authored source.

AI:
  model/agent inference.

simulation:
  run output with inputs/parameters/seed/version.

policy:
  import/distiller policy output.

personal:
  personal annotation or pin.

team:
  shared annotation, still not accepted model truth.

audit:
  explicit accepted/rejected decision event.
```

## Cross-view consistency rules

### Outline/list

```text
ObjectContainer -> solid row shell / identity rail.
DerivedUnit -> hollow/dashed row shell.
Projection row -> lens row, never native shell.
CompositionEdge -> indentation/order rail.
TruthStateGlyph -> left status stack plus labels/counts.
Folded warnings -> count residue.
```

### Graph/topology

```text
Node perimeter carries object/derived/projection/missing state.
CompositionEdge uses bracket/rail/order language.
Semantic RelationEdge uses typed connector only when real/current.
Candidate/inferred uses dashed/dotted typed connector.
Projection layout has visible lens/method label.
Disconnected island has shoreline, not fake connectors.
```

### Timeline/trace

```text
Revision -> stratum/tick.
Import/parse/derive/edit/accept/reject/fail -> TransformationTrace event.
Parallel work must not collapse into false linear causality.
Rejected/pending/failed actions leave residue.
```

### Inspector/trust view

```text
All compressed glyph layers expand into explicit fields:
material kind, substrate badge, source map, anchors, revisions, edges,
projection rows, operational state, authority, audit, missing queries.
```

The inspector explains; it must not be the only place truth state appears.

### Canvas/local world

```text
Placement pin is not a RelationEdge.
View xy/collapse/selection is ViewState and gets chrome treatment.
ContextMembership/local-world membership gets model-backed treatment only when
such a row exists.
```

### Semantic zoom

```text
Objects may compress to glyphs.
Glyphs must preserve identity class, substrate badge, source-map health, and
truth residue.
Projection labels survive as watermarks/legends.
Zoom does not turn missing queries into known facts.
```

## What this makes visible

```text
source vs native material
common base truth vs source-specific projection
projection/read model vs canonical object
operational state vs model object
implemented current substrate vs future/unimplemented substrate
ObjectContainer vs DerivedUnit vs graduated object
healthy/partial/stale/missing/unknown source mapping
CompositionEdge vs semantic RelationEdge
accepted vs candidate vs inferred vs rejected
human/AI/policy/simulation/personal/team/audit authority
view state vs model state
local placement vs relation
parse failure, partial parse, missing query, and unknown health
```

## What this keeps folded but recoverable

```text
full raw source body
full source hash/version metadata
full SourceAnchor rows
full revision history
full import completion and decision rows
full transcript run/file-offset/source-line status
full projection row payload
full AI/model/simulation run metadata
full rejected proposal history
full missing-query explanation
```

Folded detail must leave status residue.

## What this must not imply

```text
that a clean projection is canonical truth
that source-specific rows are common base truth
that missing query means success
that empty space means complete import
that source grounding means semantic correctness
that human-authored means verified
that AI-inferred means accepted
that simulation output means observed fact
that transcript operational state generalizes to all sources
that future source families are implemented
that composition/order means semantic relation
that local placement means relation
that ViewState is model truth
that global overview is a trustworthy world before local trust works
```

## Failure modes

### Projection laundering

A markdown outline, transcript conversation, embedding, cluster, source-specific
index, or layout appears as canonical Softland object truth.

### Source-map theater

The view draws exact bidirectional anchors when only target-side anchors,
source-side refs, coarse spans, or missing range queries exist.

### Semantic-edge hallucination

CompositionEdges or layout proximity are rendered as supports/contradicts/
depends-on style RelationEdges before relation substrate exists.

### Empty-success lie

Missing source inventory, source health, trust bundle, or hydrated topology
queries produce a calm empty state instead of an unknown/missing-query state.

### Authority laundering

Human, AI, simulation, import policy, personal annotation, team annotation, and
accepted audit all share one generic "fact" look.

### Folded uncertainty

Collapse, zoom, filter, or local topology hides stale anchors, parse failures,
candidate relations, unanchored objects, disconnected islands, or missing
queries without residue.

### Future-substrate confusion

Code/Roam/DG/Linear/docs-specific material or RelationEdge/ViewState/
ContextMembership/LayerOverlay concepts appear current simply because the
architecture has names for them.

## Acceptance checks

1. In grayscale and without hover, accepted, candidate, inferred, rejected,
   partial, failed, stale, disconnected, unanchored, unknown, projection, and
   future/unimplemented states remain distinguishable.
2. A markdown document ObjectContainer and markdown block DerivedUnit cannot be
   mistaken for each other.
3. A transcript conversation/message/tool-call ObjectContainer and transcript
   projection/index row cannot be mistaken for each other.
4. A selected native object can show whether its trust data is fully hydrated,
   partially hydrated, unknown because a query is missing, or absent because no
   substrate row exists.
5. Native-to-source and source-to-native mapping display their precision and
   query availability separately.
6. No semantic RelationEdge appears as current common truth until the substrate
   has one.
7. Projection proximity never uses accepted edge styling.
8. ViewState, local placement, and model membership use different marks.
9. Code/Roam/DG/Linear/Softland-doc-specific import appears as future/missing
   unless imported through an implemented path such as generic markdown.
10. Collapsed and zoomed-out states preserve residues for stale anchors,
    unanchored objects, parse failures, candidates, inferred relations,
    disconnected islands, unknown health, and missing queries.

## Open questions

1. Which truth states should be enforced by renderer assertions rather than
   design review?
2. Should the first prototype include a debug overlay that labels every mark as
   base/projection/ops/view/future/unknown?
3. What is the minimum source-range index needed before source-to-native hover
   can be shown as exact?
4. Should future semantic RelationEdge styling be present in design artifacts
   now, or hidden until the substrate exists?
5. What is the smallest useful authority-state set for the first prototype:
   human, AI, policy, simulation, audit; or also personal/team/disagreement?
6. How should unknown/missing-query states be copywritten so they feel honest
   but not broken?
7. Should projection health be a small glyph everywhere or an expanded panel
   only when projection risk is high?

## Next recommended goal

Goal 5 should use this revised grammar in concrete markdown and transcript
walkthroughs first, because those are the implemented source families in the
current object-container substrate.

Goal 6 should not design code/Roam/Linear/global topology as if those source
families or RelationEdges already exist.

