# ZUI Research

Status: drafted, 2026-06-09.

## Origin Prompt

```text
Use this for the ZUI angle.

Continue the Softland design/view research track.

Read:
- docs/current-mental-model/design/codex/orientation.md
- docs/current-mental-model/design/codex/goal-7/description.md
- goal-3 outputs if present
- goal-4 outputs if present

Execute Goal 7 only.

Create the required files inside docs/current-mental-model/design/codex/goal-7/.

Research and define Softland's ZUI / semantic zoom model. Treat ZUI as lawful
compression across scales of understanding, not as zoom animation.

I want to know what changes at each zoom level, what survives every scale
change, how identity/provenance/relation/truth-state are preserved, how the
user knows where they are, and what small prototype slices can test this without
building the whole global map.

Use external research where needed and cite sources. Do not implement UI. Do
not commit docs.
```

## Goal

Define Softland's ZUI / semantic zoom model as lawful compression across scales
of understanding.

## Inputs Read

Local:

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-7/description.md`
- `docs/current-mental-model/design/codex/goal-3/description.md`
- `docs/current-mental-model/design/codex/goal-4/description.md`
- `docs/current-mental-model/design/codex/goal-5/description.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/current-mental-model/architecture/object-container-spec.md`
- `docs/current-mental-model/architecture/object-container-ingester-contract.md`
- `docs/current-mental-model/build/knowledge-earth-zui/CATEGORY_THEORY_DESIGN_RESEARCH.md`

Goal 3, Goal 4, and Goal 5 output files were not present in this checkout; only
their description files were present.

External:

- Bederson and Hollan, "Pad++: A Zoomable Graphical Interface System"
  https://hci.ucsd.edu/hollan/Pubs/JH1995-1.pdf
- Furnas and Bederson, "Space-Scale Diagrams: Understanding Multiscale
  Interfaces"
  https://www.cs.umd.edu/projects/hcil/pad%2B%2B/papers/chi-95-spacescale/chi-95-spacescale.pdf
- Bederson, Meyer, and Good, "Jazz: An Extensible Zoomable User Interface
  Graphics Toolkit in Java"
  https://www.cs.umd.edu/~bederson/images/pubs_pdfs/p171-bederson.pdf
- Furnas, "Generalized Fisheye Views"
  https://www.cs.columbia.edu/~feiner/courses/csw4170/resources/furnasCHI86.pdf
- Shneiderman, "The Eyes Have It: A Task by Data Type Taxonomy for Information
  Visualizations"
  https://drum.lib.umd.edu/items/155a868e-fb83-4115-9899-9187ea8c0498
- Cockburn, Karlson, and Bederson, "A Review of Overview+Detail, Zooming, and
  Focus+Context Interfaces"
  https://csse.canterbury.ac.nz/andrew.cockburn/papers/fc.pdf
- Munzner, "A Nested Model for Visualization Design and Validation"
  https://www.cs.ubc.ca/labs/imager/tr/2009/NestedModel/
- W3C PROV Overview
  https://www.w3.org/TR/prov-overview/
- ECMA-426 Source Map Format Specification
  https://tc39.es/ecma426/
- Chrome DevTools Source Maps documentation
  https://developer.chrome.com/docs/devtools/javascript/source-maps/
- Google Earth Engine scale documentation
  https://developers.google.com/earth-engine/guides/scale
- Google Maps zoom-level documentation
  https://developers.google.com/maps/documentation/maps-static/start#Zoomlevels
- Skeels, Lee, Smith, and Robertson, "Revealing Uncertainty for Information
  Visualization"
  https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/avi2008-uncertainty.pdf
- Spivak and Kent, "Ologs: A Categorical Framework for Knowledge Representation"
  https://arxiv.org/abs/1102.1889
- Topos Institute, Collective Intelligence
  https://topos.institute/work/collective-intelligence/

## Sources Researched

The sources above were used as research material, not decoration. The main
transfer into Softland is:

- ZUI sources define scale-space navigation and semantic representation changes.
- Focus+context sources define the local-detail/global-context pressure.
- GIS/scale sources warn that aggregation policies must be explicit.
- Source-map and PROV sources define reversible transformation and trust.
- Uncertainty sources warn that clean interfaces make uncertain material look
  absolute.
- Olog/Topos sources frame zoom levels as structure-preserving maps between
  worlds of understanding.

## Scope

This document defines what ZUI should mean for Softland. It does not specify UI
layout, animation, rendering technology, or implementation.

It treats ZUI as a model contract:

```text
same world
  different scale
  different representation
  preserved identity
  preserved provenance
  preserved relation state
  preserved truth state
  recoverable way back
```

## Core Claim

Softland ZUI is not a zoom animation. It is navigable, lawful compression across
scales of understanding.

The user should be able to move from knowledge landscape to source line without
feeling that the system swapped worlds. Zoom changes dosage, representation, and
valid actions. It must not change what is true, what is merely candidate, where
something came from, or which object is being followed.

## What ZUI Historically Means

### Large Continuous Information Space

Pad++ framed ZUI as an alternative to window/icon interfaces: objects can live
at different sizes in a large workspace, and zooming/panning become primary
navigation. The useful Softland borrowing is not the infinite canvas by itself.
It is the idea that a complex information space can be inhabited through scale,
position, and persistent objects.

Softland correction:

```text
large workspace
  is not enough

large workspace + lawful object/provenance/relation preservation
  is the beginning of Softland ZUI
```

### Scale As An Explicit Dimension

Space-scale diagrams make scale analyzable rather than magical. A point in the
2D plane becomes a ray through scale. For Softland, this is the most important
historical idea: an object should have a scale-lineage. The object may render as
region, topic card, claim row, model node, source span, or code symbol, but the
view should preserve the object's "ray" through scale.

Softland borrowing:

```text
semantic address = object identity + scale + projection + local context
```

Softland rejection:

```text
scale as visual magnification only
```

### Semantic Zooming

Jazz names the crucial idea: objects can change representation based on current
magnification. At low zoom an object may be a compact glyph; at high zoom it may
show structure, labels, affordances, and details.

Softland borrowing:

```text
different visual representations
same underlying object identity
same truth/provenance state
same relation contract
```

Softland rejection:

```text
representation switch that makes the object feel copied, reclassified, or newly
asserted
```

### Focus Plus Context

Furnas' fisheye work and later overview/detail/focus+context surveys establish
a basic cognitive pressure: people need local detail and enough larger context
to know what the local detail means. Softland should borrow this, but with
calibration: distortion and aggregation must be labeled so the map does not lie.

Softland borrowing:

```text
local focus
nearby topology
compressed distant context
visible scope and way back
```

Softland rejection:

```text
distortion that makes a relation look stronger, closer, or more accepted than
the model says
```

### Overview, Zoom, Filter, Details

Shneiderman's mantra is useful, but Softland should not interpret "overview" as
"show the whole world first." For Softland, overview means "show the scope of
the local world before asking the user to inspect detail."

Softland borrowing:

```text
overview first
zoom/filter
details on demand
relate
history
extract
```

Softland correction:

```text
overview = local scope with honest coverage
not global spectacle
```

### GIS, Image Pyramids, And Scale Policies

Google Maps gives a familiar public example of zoom levels changing the expected
detail from world to buildings. Google Earth Engine is more important for
Softland: image pyramids aggregate data differently at different scales, and the
scale of analysis determines which level is pulled.

Softland borrowing:

```text
every zoom level must have an explicit aggregation/compression policy
```

Softland danger:

```text
if the aggregation policy changes the apparent truth, users need to see that
they are viewing an aggregate/projection, not the raw object
```

### Source Maps And Provenance

Source maps solve a trust problem: developers debug authored code even when the
runtime executes transformed code. Softland has the same shape for knowledge:
native objects are useful only if the user can recover their raw source anchors.

Softland borrowing:

```text
native object -> exact source span
source span -> native object
runtime/projection result -> authored/source material
```

W3C PROV adds the broader provenance frame: quality, reliability, and
trustworthiness depend on knowing the entities, activities, and agents involved
in producing a thing.

### Uncertainty Visualization

Uncertainty research warns that interfaces often make uncertain data look
absolute. Skeels et al. also separate uncertainty introduced at acquisition,
transformation, and visualization. That maps cleanly onto Softland:

```text
acquisition uncertainty
  source capture, redaction, parsing, missing native ids

transformation uncertainty
  distiller interpretation, candidate relations, inferred structure

visualization uncertainty
  projection simplification, aggregation, layout proximity
```

Softland borrowing:

```text
truth-state and uncertainty-state survive zoom as first-class state
```

Softland rejection:

```text
clean ZUI surfaces that hide parse failures, stale anchors, candidate edges, or
projection uncertainty
```

### Category Theory, Ologs, And Collective Intelligence

Spivak and Kent's ologs treat knowledge representation as typed objects,
aspects, facts, and structure-preserving comparison between worldviews. Topos'
collective-intelligence framing asks how structures, language, and intelligence
emerge from interaction between people, technologies, and environments.

Softland borrowing:

```text
zoom levels are not just levels of detail
they are different categories of objects and arrows

zoom transitions must preserve enough structure that the user can move between
worlds without private reconstruction
```

Softland rejection:

```text
showing category theory as boxes and arrows because the math is elegant
```

## What Softland Should Borrow

1. From Pad++: persistent information objects in a navigable scale space.
2. From Space-Scale Diagrams: scale as an explicit coordinate, not a visual
   afterthought.
3. From Jazz: semantic zoom as multiple representations of the same data model.
4. From fisheye/focus+context: local detail plus compressed context.
5. From Shneiderman: overview, zoom/filter, details, relate, history, extract.
6. From GIS/image pyramids: explicit aggregation policies per scale.
7. From source maps: transformed result must map back to authored/original
   material.
8. From W3C PROV: provenance is part of trust, not metadata decoration.
9. From uncertainty visualization: uncertainty must be represented, not cleaned
   away.
10. From ologs/category theory: levels have objects and arrows, and transitions
    should preserve structure where they claim to preserve structure.

## What Softland Should Reject

1. Reject zoom as spectacle.
2. Reject global map as first prototype.
3. Reject embedding proximity as semantic relationship unless the projection is
   visibly labeled.
4. Reject any zoom transition that loses the selected object's identity.
5. Reject summaries that hide anchor status, parse failure, stale source, or
   candidate relation state.
6. Reject "overview" that implies total understanding when coverage is partial.
7. Reject treating every scale as the same node-link graph with different font
   sizes.
8. Reject math-flavored UI that does not improve orientation, provenance, or
   action.

## Design Decisions

### 1. Every Zoom Level Has A Different Job

Zoom 0.001 is for field-scale orientation. Zoom 100.0 is for bedrock/code. They
should not expose the same entities with different styling. Each level answers a
different user question and permits different actions.

### 2. Every Zoom Transition Has A Compression Policy

Each transition must say:

```text
what aggregates
what folds
what disappears from the surface
what remains recoverable
what truth-state survives
what relations are preserved, summarized, or hidden
what actions become valid or invalid
```

### 3. Identity Is The Main Navigation Thread

The user is never just zooming into space. They are following an identity across
representations:

```text
domain region
  -> topic/local world
  -> ObjectContainer or DerivedUnit
  -> Revision / SourceAnchor
  -> code/source span
```

### 4. Provenance Is A Recoverable Layer At Every Scale

At high-level views, exact spans can be folded. Anchor coverage cannot be
folded away. The user must see whether a region/object/relation is source-backed,
partially anchored, stale, inferred, or unanchored.

### 5. Truth State Survives Compression

Accepted/candidate/rejected/inferred/partial/stale/unanchored are not inspector
details. They affect what the map may imply at every scale.

### 6. Semantic Zoom Is Pacing

Zoom level controls dosage:

```text
how much arrives
how fast it arrives
what is folded
what is recoverable
what action is safe now
```

This is the operational meaning of "pacing is epistemic machinery."

## What This Makes Visible

- Where the user is in the world of understanding.
- Which scale they are inhabiting.
- What object or local world they are following.
- What source/provenance remains recoverable.
- Which relations are accepted, candidate, rejected, inferred, or aggregated.
- Which truth states are present in the current region.
- Which actions are appropriate at this scale.

## What This Keeps Folded But Recoverable

- Exact source spans at overview scales.
- Full raw source bodies until requested.
- Distant neighborhoods.
- Long revision/event history.
- Candidate relation evidence.
- Projection machinery.
- Code internals when the user is not at bedrock scale.

## What This Must Not Imply

- That lower detail means lower uncertainty.
- That visual closeness means accepted semantic relation.
- That an aggregate region is a single accepted object.
- That a smooth transition means the transformation is trustworthy.
- That a generated topic label is human-accepted truth.
- That source-backed and inferred material have the same status.

## Risks

1. The global map becomes a beautiful lie.
2. Semantic zoom becomes a rendering trick without model contracts.
3. Aggregation hides the exact failures the user needed to see.
4. Users lose place because zoom changes both scale and focus at once.
5. "Knowledge Earth" becomes too broad before Source-to-World trust is proven.
6. Candidate/inferred relation density makes the world look more known than it
   is.
7. Code/bedrock zoom is treated as developer tooling instead of Softland's
   zoom-100 layer.

## Failure Modes

- A selected object becomes visually different at another scale but no longer
  feels like the same object.
- A high-level domain region lacks visible coverage/truth-state summary.
- The user cannot recover the source span behind a native object.
- A relation is visible at overview but its accepted/candidate/rejected state is
  hidden.
- Zooming out changes the user's scope without preserving the trail back.
- A projection clusters objects by embedding similarity without labeling that as
  projection-derived proximity.

## Acceptance Checks

- Given a selected object at any zoom level, the user can reveal its stable
  identity and current truth/provenance state.
- Given a high-level aggregate, the user can inspect what aggregation policy
  produced it.
- Given an accepted relation visible at overview, the user can descend to its
  evidence or decision trail.
- Given a candidate relation visible at overview, it never looks accepted.
- Given a source span, the user can move to the native object(s) derived from it.
- Given a native object, the user can return to the entry context after following
  relation links.
- Given a zoom transition, the system can state what changed, what survived, and
  what was folded.

## Open Questions

1. What is the minimum truth-state grammar needed before a ZUI prototype can be
   honest?
2. Should Softland use continuous numeric zoom internally, discrete semantic
   levels, or both?
3. What is the canonical semantic address shape for a local world at a scale?
4. How should aggregate labels be generated and calibrated?
5. Which zoom transitions should be user-controlled versus system-suggested?
6. How much provenance coverage should be visible at landscape/domain scales?
7. What are the first non-geographic wayfinding metaphors that still feel like
   place?

## Next Recommended Goal

Run Goal 4 next if the truth-state grammar is still absent. Goal 7 can define
that truth state must survive zoom, but Goal 4 needs to define the exact visual
and interaction grammar for accepted, candidate, inferred, stale, partial,
failed, disconnected, unanchored, source-backed, and projection-derived states.
