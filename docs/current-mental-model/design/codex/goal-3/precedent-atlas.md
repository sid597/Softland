# Precedent Atlas For Softland Views

Status: draft research sweep, 2026-06-09.

## Origin Prompt

```text
Continue the Softland design/view research track. Execute Goal 3 only. Build a
precedent atlas, not a moodboard. Research HCI, visualization, ZUI, explorable
explanations, malleable systems, creative professional tools, developer/lineage
tools, games, architecture, photography/cinema/music where useful.
```

## Goal

Build a precedent atlas for Softland's view layer: a practical research map of
what other fields have learned about orientation, transformation, provenance,
zoom, manipulation, trust, collaboration, and inhabitable work surfaces.

## Inputs Read

Local inputs:

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-3/description.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/vision/epistemic-framework.md`
- `docs/current-mental-model/design/codex/goal-1/description.md`
- `docs/current-mental-model/design/codex/goal-2/description.md`
- Checked `docs/current-mental-model/design/codex/*`; no prior goal output artifacts existed beyond the goal descriptions.

External inputs are cited per reference.

## Scope

This atlas covers precedents that can inform Softland views without turning the
first prototype into a generic graph, dashboard, canvas, or editor. The focus is
the Source-to-World problem: a finite mind must enter transformed material while
still seeing origin, identity, relation, uncertainty, pacing, and available
action.

## Core Claim

Softland should borrow mechanisms of inspectable transformation, scoped
orientation, reversible compression, multi-scale navigation, and professional
workflow pacing. It should reject surface mimicry: global graphs, pretty zoom,
generic canvases, unexplained embeddings, and "clean" views that hide the
conditions under which an object became native.

## Findings Or Design Decisions

- The strongest precedent family is developer tooling, especially source maps,
  trace viewers, and lineage systems. They solve Softland's first trust problem:
  "show me the transformed thing and the authored source it came from."
- ZUI work is valuable for lawful compression, not spectacle. Zoom only matters
  if identity, provenance, and relation survive every scale transition.
- Visualization research gives the order of operations: domain/task first,
  abstraction second, encoding third. Softland should not choose visual form
  before it names the epistemic job.
- Creative tools teach workspace pacing: professional users move among focused
  modes, contact sheets, timelines, inspectors, and comparison surfaces. These
  are not "panes"; they are cognitive gears.
- Games and wayfinding teach local entry: a person understands a world through
  local position, remembered trails, landmarks, fog, and a way back.
- Explorable explanations teach manipulative understanding, but Softland must
  preserve provenance and uncertainty more rigorously than most public essays.

## HCI And Visualization

### Shneiderman, "The Eyes Have It"

- Reference: Ben Shneiderman, "The Eyes Have It: A Task by Data Type Taxonomy for Information Visualizations"
- Source URL: https://hci.ucsd.edu/220/EyesHaveIt.pdf
- Problem it solves: Gives a task-first grammar for navigating large information spaces: overview, zoom/filter, details, relate, history, and extract.
- Softland borrowing: Start every view with scope, then local focus, then exact detail. Use this as a baseline contract for Source-to-World views.
- Softland should reject: Treating "overview first" as "global graph first." Softland's first overview should be scoped source inventory plus local topology, not the entire world.
- Candidate primitive: `ScopedOverview`: an import-level view that shows source artifacts, native objects, warning counts, accepted relation counts, and selected-object neighborhood.
- Failure mode warning: A visual system can offer detail-on-demand while still making the wrong thing feel central. A beautiful overview that hides source anchors is a trust failure.
- Borrowing test: A user can enter one imported source, select one object, inspect exact anchors, and return to scope without losing orientation.
- Relevance score: 5
- Confidence: high

### Munzner, Nested Model For Visualization Design

- Reference: Tamara Munzner, "A Nested Model for Visualization Design and Validation"
- Source URL: https://vis.csail.mit.edu/classes/6.859/readings/pdfs/Munzner-ANestedModelForVisualizationDesignAndValidation.pdf
- Problem it solves: Separates domain problem, data/task abstraction, visual encoding, and algorithmic implementation so upstream errors do not masquerade as design decisions.
- Softland borrowing: Use the nested model as a review gate: first name the epistemic job, then the data contract, then the visual grammar, then implementation.
- Softland should reject: Designing visual novelty before validating whether the view can answer "what came in, what became native, and why should I trust it?"
- Candidate primitive: `ViewDesignLadder`: a checklist attached to every view spec with domain job, query contract, encoding, interaction, and test.
- Failure mode warning: If the domain abstraction is wrong, every downstream visual choice gets polished in the wrong direction.
- Borrowing test: A proposed view can be rejected at the domain/task level before debating layout or color.
- Relevance score: 5
- Confidence: high

### HCIL Treemap

- Reference: University of Maryland HCIL Treemap project
- Source URL: https://www.cs.umd.edu/projects/hcil/treemap/
- Problem it solves: Makes large hierarchies space-efficient while preserving relative size and containment.
- Softland borrowing: Use containment views for source artifacts, derived units, revisions, and object groups when hierarchy matters more than network relation.
- Softland should reject: Encoding epistemic confidence or truth as area. Size should mean a stable quantity, not importance or certainty.
- Candidate primitive: `SourceContainmentMap`: a compact hierarchy of source artifact -> derived unit -> native object, with warnings layered as status, not area.
- Failure mode warning: Treemaps compress aggressively; they can make small but epistemically crucial objects disappear.
- Borrowing test: A user can find parse gaps and stale anchors in a large import without scanning a table.
- Relevance score: 4
- Confidence: high

### MacEachren, Visualizing Uncertain Information

- Reference: Alan M. MacEachren, "Visualizing Uncertain Information"
- Source URL: https://cartographicperspectives.org/index.php/journal/article/view/cp13-maceachren
- Problem it solves: Frames uncertainty as something maps must communicate during exploration, not a footnote after facts are settled.
- Softland borrowing: Treat accepted, candidate, inferred, partial, stale, failed, disconnected, and unanchored as first-class view states encoded redundantly.
- Softland should reject: Using color alone for truth state or hiding uncertainty behind hover-only metadata.
- Candidate primitive: `TruthStateGlyph`: redundant label, icon, stroke, opacity, and interaction grammar for every object and relation state.
- Failure mode warning: If uncertainty is visually weaker than clean structure, users will over-trust transformed knowledge.
- Borrowing test: In grayscale and without hover, users can distinguish accepted relation from candidate relation and stale anchor from unanchored object.
- Relevance score: 5
- Confidence: high

### Inselberg, Parallel Coordinates

- Reference: Alfred Inselberg, "Parallel Coordinates: Visual Multidimensional Geometry and Its Applications"
- Source URL: https://link.springer.com/book/10.1007/978-0-387-68628-8
- Problem it solves: Lets users inspect many dimensions without collapsing them into a single reduced projection.
- Softland borrowing: Use parallel attribute lanes when comparing objects by source type, anchor quality, relation type, revision age, truth state, and workflow status.
- Softland should reject: Dense line fields as the default view for ordinary users. This is an analysis instrument, not a home surface.
- Candidate primitive: `EpistemicLanes`: optional inspector view where selected objects become lines across provenance, relation, uncertainty, and action dimensions.
- Failure mode warning: Multi-dimensional views can look authoritative while becoming unreadable under real density.
- Borrowing test: A power user can isolate "highly connected but weakly anchored" objects faster than by table filtering.
- Relevance score: 3
- Confidence: medium

## ZUI And Navigation

### Pad++

- Reference: Pad++ Zoomable User Interface project
- Source URL: https://www.cs.umd.edu/projects/hcil/pad%2B%2B/
- Problem it solves: Explores zoom as a fundamental interaction for multiscale information spaces.
- Softland borrowing: Use zoom as a controlled transition among representations, where object identity and labels change lawfully with scale.
- Softland should reject: Infinite canvas romance. Infinite extent is not the same as inhabitable terrain.
- Candidate primitive: `ScaleStableObject`: an object that keeps identity, source anchor status, and relation count visible while its representation changes from glyph to card to inspector.
- Failure mode warning: Smooth zoom can hide conceptual jumps. The user may feel oriented while the meaning of marks has changed.
- Borrowing test: After zooming from import scope to one object, a user can say which object stayed selected, what source it came from, and which relations remained accepted.
- Relevance score: 5
- Confidence: high

### Space-Scale Diagrams

- Reference: Furnas and Bederson, "Space-Scale Diagrams: Understanding Multiscale Interfaces"
- Source URL: https://doi.org/10.1145/223904.223934
- Problem it solves: Makes scale itself analyzable, including visibility, distance, and navigation costs across magnifications.
- Softland borrowing: Model zoom transitions before rendering them: what becomes visible, what folds, what remains selectable, and what paths remain cheap.
- Softland should reject: Treating zoom as a camera effect rather than an epistemic contract.
- Candidate primitive: `ZoomContract`: per scale band, define visible identity, provenance, relation, uncertainty, and available actions.
- Failure mode warning: A ZUI can be mathematically continuous but semantically discontinuous.
- Borrowing test: The same object has a documented representation at import, neighborhood, object, field, and source-span scale.
- Relevance score: 5
- Confidence: high

### Prezi

- Reference: Prezi presentation canvas
- Source URL: https://prezi.com/
- Problem it solves: Makes presentation navigation spatial and path-based instead of slide-stack based.
- Softland borrowing: Borrow the idea of authored traversal paths through spatial material for handoff and replay.
- Softland should reject: Cinematic swoop as understanding. Motion can impress while weakening orientation.
- Candidate primitive: `GuidedTrailPath`: a saved path through source, native object, relation, and trust inspector states.
- Failure mode warning: Spatial presentations can become choreography instead of evidence.
- Borrowing test: A viewer can exit a guided path, inspect an anchor, and rejoin without losing position in the trail.
- Relevance score: 3
- Confidence: medium

### Google Earth

- Reference: Google Earth
- Source URL: https://earth.google.com/web/
- Problem it solves: Lets users move continuously from planetary overview to local ground, using stable geography as the identity substrate.
- Softland borrowing: Borrow scale bands, landmarks, and "you are here" positioning for knowledge terrain.
- Softland should reject: Literal earth-map mimicry. Knowledge does not have natural Euclidean geography unless Softland earns that projection.
- Candidate primitive: `KnowledgeLandmark`: a stable named node or source island used for orientation across zoom levels.
- Failure mode warning: Map metaphors make projections feel more objective than they are.
- Borrowing test: A user can zoom from source corpus to one source span and still know the enclosing source, import, and local world.
- Relevance score: 4
- Confidence: medium

### Obsidian Graph And Local Graph

- Reference: Obsidian Graph view and Local Graph
- Source URL: https://obsidian.md/help/plugins/graph
- Problem it solves: Shows note-link relationships globally or around the active note, with filters and depth control.
- Softland borrowing: Borrow local graph depth, filters, and active-note centering for scoped topology.
- Softland should reject: Global graph as first screen or proof of understanding.
- Candidate primitive: `LocalTopologyLens`: selected object at center, typed accepted/candidate edges, depth slider, counts for folded neighborhoods.
- Failure mode warning: Link graphs can reward link density rather than grounded understanding.
- Borrowing test: Users prefer local topology over a global graph for answering "why is this object connected?"
- Relevance score: 5
- Confidence: high

## Explorable Explanations

### Bret Victor, Ladder Of Abstraction

- Reference: Bret Victor, "Up and Down the Ladder of Abstraction"
- Source URL: https://worrydream.com/LadderOfAbstraction/
- Problem it solves: Shows how interactive systems can let a person move between concrete instances and abstract patterns.
- Softland borrowing: Every abstraction should provide a way to step down to source evidence and step up to pattern.
- Softland should reject: Abstract views with no path back to concrete source spans.
- Candidate primitive: `StepDown`: interaction from any projection mark to exact source anchor, revision, and transformation record.
- Failure mode warning: Abstraction without re-entry becomes private reconstruction.
- Borrowing test: From a cluster, relation, or summary, users can reach the raw material and explain why that abstraction exists.
- Relevance score: 5
- Confidence: high

### Explorable Explanations

- Reference: Nicky Case's Explorable Explanations hub
- Source URL: https://explorabl.es/
- Problem it solves: Collects interactive explanations where readers learn by manipulating variables, not just reading claims.
- Softland borrowing: Treat views as epistemic instruments with knobs, local simulations, and reversible probes.
- Softland should reject: One-off bespoke demos as the core substrate. Softland needs durable objects, provenance, and reusable primitives.
- Candidate primitive: `ProbeControl`: a view control that changes a projection while recording the probe as an event.
- Failure mode warning: Interactivity can create engagement without preserving what was learned.
- Borrowing test: A probe changes the view, records the parameter, and can be replayed in a reasoning trail.
- Relevance score: 4
- Confidence: high

### Loopy

- Reference: Nicky Case, "LOOPY: a tool for thinking in systems"
- Source URL: https://ncase.me/loopy/
- Problem it solves: Lets users sketch causal loops and simulate feedback in a lightweight model.
- Softland borrowing: Borrow simple direct manipulation for relation hypotheses and feedback structures.
- Softland should reject: Treating user-drawn relation loops as accepted knowledge without grounding.
- Candidate primitive: `CandidateCausalSketch`: user-authored relation layer that stays visually separate from accepted edges until reviewed.
- Failure mode warning: Playful causal diagrams can overstate causality.
- Borrowing test: Users can draw a candidate relation, see it marked candidate, attach evidence, and either accept or reject it.
- Relevance score: 4
- Confidence: high

### Distill Interactive Articles

- Reference: Distill, "Communicating with Interactive Articles"
- Source URL: https://distill.pub/2020/communicating-with-interactive-articles/
- Problem it solves: Studies interactive articles as a medium for making technical research clearer, more dynamic, and more inspectable.
- Softland borrowing: Borrow the web-native article as a replayable loop: narrative, interactive model, evidence, and reader reflection in one object.
- Softland should reject: Linear article form as the final destination. Softland's material must remain transformable and queryable after publication.
- Candidate primitive: `ReplayableExplanation`: artifact that bundles prose, parameterized view, source anchors, and trail provenance.
- Failure mode warning: Polished explanation can flatten uncertainty and disagreement.
- Borrowing test: A reader can inspect which source objects support each explanatory claim and fork the explanation without breaking provenance.
- Relevance score: 5
- Confidence: high

### Observable Notebooks

- Reference: Observable notebooks
- Source URL: https://observablehq.com/documentation/notebooks/
- Problem it solves: Combines text, code, outputs, reactive cells, collaboration, version history, and reusable imports in a web-native notebook.
- Softland borrowing: Borrow reactive cells, visible outputs, cell import/reuse, and shareable computational documents.
- Softland should reject: Notebook linearity and hidden execution-order confusion as the main knowledge substrate.
- Candidate primitive: `ReactiveArtifactCell`: local-world artifact slot whose output is live, inspectable, and tied to source/revision state.
- Failure mode warning: Notebooks can mix exploration and explanation so thoroughly that provenance and execution state become ambiguous.
- Borrowing test: A cell output shows its inputs, revision, source anchors, and whether it is stale.
- Relevance score: 4
- Confidence: high

## Malleable Systems

### Dynamicland

- Reference: Dynamicland FAQ and 2024 intro
- Source URLs: https://dynamicland.org/2024/FAQ/ and https://dynamicland.org/2024/Intro/
- Problem it solves: Reimagines computing as a communal, physical, inhabitable medium where people manipulate computational models together.
- Softland borrowing: Borrow the commitment that computation can be a place and that authoring can happen inside the medium itself.
- Softland should reject: Physical-space literalism for the first view. Softland must first prove accountable source-to-native trust on screen.
- Candidate primitive: `InhabitableLocalWorld`: a bounded workspace where objects, sources, relations, probes, and artifacts persist as a place.
- Failure mode warning: Inhabitability language can become vibe if it does not cash out in inspectable objects and actions.
- Borrowing test: A local world can be revisited by another person who can see material, process, artifact, provenance, and open tensions.
- Relevance score: 5
- Confidence: high

### Ink And Switch, Malleable Software

- Reference: Ink and Switch, "Malleable Software"
- Source URL: https://www.inkandswitch.com/essay/malleable-software/
- Problem it solves: Argues for software that users can reshape at the point of use rather than waiting for distant product teams.
- Softland borrowing: Borrow end-user adaptation as a long-term property of views and workflows.
- Softland should reject: Opening malleability before trust grammar exists. First prototype should not let users reshape truth states casually.
- Candidate primitive: `ViewPatch`: a user-visible, versioned local modification to a projection that can be shared or reverted.
- Failure mode warning: Malleability without lineage creates unreviewable private worlds.
- Borrowing test: A view patch records who changed what, why, from which base view, and what downstream views it affects.
- Relevance score: 5
- Confidence: high

### Folk Computer

- Reference: Folk Computer
- Source URL: https://folk.computer/
- Problem it solves: Explores physical computing where ordinary objects, projected interfaces, and local programs become part of a shared environment.
- Softland borrowing: Borrow "regions as objects" and lightweight authoring around situated material.
- Softland should reject: Assuming spatial magic solves epistemic calibration.
- Candidate primitive: `RegionObject`: a selected region in a source, canvas, timeline, or transcript that can graduate into a native object with anchors.
- Failure mode warning: Physical or spatial affordances can hide the model contract.
- Borrowing test: Creating a region object emits a traceable action and shows source anchor, revision, and candidate/native status.
- Relevance score: 4
- Confidence: medium

### Ink And Switch, Potluck

- Reference: Ink and Switch, Potluck
- Source URL: https://www.inkandswitch.com/potluck/
- Problem it solves: Investigates gradual enrichment of informal text into structured, interactive local tools.
- Softland borrowing: Borrow gradual structure: let imported text remain readable while objects, fields, and actions emerge.
- Softland should reject: Treating extraction as lossless because it feels convenient.
- Candidate primitive: `GraduatedStructure`: visible transition from raw span to candidate unit to accepted object.
- Failure mode warning: If the enriched view replaces source too quickly, users cannot tell whether structure was preserved or invented.
- Borrowing test: Users can see both the raw text and the promoted object, with a clear state for candidate versus accepted.
- Relevance score: 4
- Confidence: medium

## Creative Professional Tools

### Figma Version History, Dev Mode, And Multiplayer

- Reference: Figma help on version history, Dev Mode, and collaboration limits
- Source URLs: https://help.figma.com/hc/en-us/articles/360038006754-View-a-file-s-version-history, https://help.figma.com/hc/en-us/articles/15023124644247-Guide-to-Dev-Mode, https://help.figma.com/hc/en-us/articles/1500006775761-How-many-people-can-be-in-a-file-at-once
- Problem it solves: Makes a shared creative file inspectable across versions, implementation handoff, and live collaboration.
- Softland borrowing: Borrow scoped version history, inspect mode, shareable links to specific objects/versions, and presence that does not dominate the workspace.
- Softland should reject: Making collaborative presence more important than source truth.
- Candidate primitive: `ObjectVersionFocus`: selected object view with history, anchors, comments, status, and handoff readiness.
- Failure mode warning: Live collaboration can create confidence that everyone sees the same meaning while underlying object truth is unresolved.
- Borrowing test: A developer or reviewer can inspect one Softland object, its exact source, its last transformation, and whether it is ready for action.
- Relevance score: 5
- Confidence: high

### Blender Workspaces

- Reference: Blender workspaces
- Source URL: https://docs.blender.org/manual/en/latest/interface/window_system/workspaces.html
- Problem it solves: Organizes a complex professional tool into task-specific workspaces over a shared scene/data model.
- Softland borrowing: Borrow workspace modes that expose different instruments over one object substrate: import review, topology, trail replay, artifact execution.
- Softland should reject: Feature sprawl hidden behind many modes before the model is stable.
- Candidate primitive: `EpistemicWorkspace`: saved arrangement of views and tools for a specific job over the same local world.
- Failure mode warning: Workspaces can become silos if identity does not survive across them.
- Borrowing test: Selecting an object in import review and switching to topology keeps selection, status, anchors, and breadcrumb stable.
- Relevance score: 4
- Confidence: high

### Adobe Lightroom Classic Views

- Reference: Lightroom Classic Grid, Loupe, Compare, and Survey views
- Source URL: https://helpx.adobe.com/au/lightroom-classic/help/view-photos.html
- Problem it solves: Lets photographers move between collection scan, single-object inspection, pair comparison, and multi-candidate selection.
- Softland borrowing: Borrow view modes for epistemic triage: grid for sources/objects, loupe for one object, compare for two transformations, survey for multiple candidates.
- Softland should reject: Treating all view modes as equally truth-bearing. Compare/survey are judgment aids, not accepted structure.
- Candidate primitive: `ObjectSurvey`: compare candidate objects or relation interpretations while preserving source anchors.
- Failure mode warning: Selection workflows can optimize for taste rather than truth unless grounded evidence remains visible.
- Borrowing test: A user can compare three candidate relations and see their respective source spans side by side.
- Relevance score: 4
- Confidence: high

### DaVinci Resolve Pages

- Reference: Blackmagic Design DaVinci Resolve manuals and training
- Source URLs: https://www.blackmagicdesign.com/support and https://www.blackmagicdesign.com/au/products/davinciresolve/training
- Problem it solves: Separates professional video work into focused pages for media, editing, effects, color, audio, and delivery over one project.
- Softland borrowing: Borrow the idea that different stages of one artifact require distinct high-power surfaces with handoff between them.
- Softland should reject: Heavy page architecture for the first prototype. The first view needs source, native, and trust surfaces before a full production suite.
- Candidate primitive: `WorkflowPage`: a mature view mode for one stage of a local-world lifecycle, with explicit entry and exit contracts.
- Failure mode warning: Page-based power can hide cross-stage causality.
- Borrowing test: A transformation made in an import page remains inspectable in topology and artifact pages.
- Relevance score: 4
- Confidence: high

## Developer And Lineage Tools

### ECMA-426 Source Maps

- Reference: ECMA-426 Source Map specification
- Source URL: https://tc39.es/source-map/
- Problem it solves: Maps generated/transformed code back to original source for debugging and stack trace deobfuscation.
- Softland borrowing: This is the closest direct precedent for source-to-native trust. Every native object should map back to raw source spans, and raw spans should map forward to native objects.
- Softland should reject: One-way provenance or coarse document-level attribution.
- Candidate primitive: `SoftlandSourceMap`: bidirectional mapping from SourceArtifact spans to ObjectContainer fields, DerivedUnits, and relations.
- Failure mode warning: If mappings are partial but shown as exact, the map lies.
- Borrowing test: Hover native field -> highlight raw span; hover raw span -> highlight native field/object; missing mappings render partial.
- Relevance score: 5
- Confidence: high

### Chrome DevTools Source Maps

- Reference: Chrome DevTools source map debugging
- Source URL: https://developer.chrome.com/docs/devtools/javascript/source-maps
- Problem it solves: Lets developers debug authored source while the browser executes transformed deployed code.
- Softland borrowing: Borrow the debugging stance: the view should let users inspect transformed knowledge as if they were debugging preservation.
- Softland should reject: Developer-only complexity in the default view.
- Candidate primitive: `TransformationDebugger`: inspector showing authored source, generated/native object, mapping confidence, warnings, and revision.
- Failure mode warning: Debugging illusions are dangerous if users forget the runtime/native artifact is what the system acts on.
- Borrowing test: A user can identify whether a wrong native object came from bad source, bad parse, bad mapping, or later revision.
- Relevance score: 5
- Confidence: high

### Perfetto Trace Viewer

- Reference: Perfetto UI
- Source URL: https://perfetto.dev/docs/visualization/perfetto-ui
- Problem it solves: Makes large, multi-track execution traces navigable through timelines, selections, slices, and details.
- Softland borrowing: Borrow multi-track timeline inspection for reasoning trails, import pipelines, agent runs, and transformation events.
- Softland should reject: Timeline-as-everything. Provenance is not only linear time; Softland also needs DAG causality and source anchoring.
- Candidate primitive: `TransformationTrace`: timeline lane view of import, parse, object creation, relation creation, acceptance, rejection, and failures.
- Failure mode warning: Linear timelines can invent false causality when work is parallel or braided.
- Borrowing test: Selecting a transformation event reveals affected objects, source spans, previous state, and downstream relations.
- Relevance score: 5
- Confidence: high

### OpenTelemetry Traces

- Reference: OpenTelemetry tracing concepts
- Source URL: https://opentelemetry.io/docs/reference/specification/overview/
- Problem it solves: Records distributed traces as spans with parent/child relationships and cross-process context.
- Softland borrowing: Borrow span-like structure for agent/tool/reviewer activity, especially when one user action triggers many derived events.
- Softland should reject: Treating observability traces as user-facing knowledge trails without translation.
- Candidate primitive: `EpistemicSpan`: bounded probe/action event with inputs, outputs, source context, parent spans, links, and status.
- Failure mode warning: Instrumentation can produce lots of logs without meaningful causality.
- Borrowing test: One import action expands into spans that answer what ran, what changed, what failed, and what was accepted.
- Relevance score: 4
- Confidence: high

### dbt Lineage

- Reference: dbt Labs, "Getting started with data lineage"
- Source URL: https://www.getdbt.com/blog/guide-to-data-lineage
- Problem it solves: Uses DAGs to show data origins, transformations, downstream impact, and root-cause paths.
- Softland borrowing: Borrow upstream/downstream impact analysis and version-controlled lineage generated from actual references.
- Softland should reject: Manual lineage as source of truth when automatic grounded edges exist.
- Candidate primitive: `ImpactNeighborhood`: focused upstream/downstream view for a selected object, source, or relation.
- Failure mode warning: Lineage graphs become overwhelming as nodes grow; column-level detail is often necessary.
- Borrowing test: Before accepting a source/object change, the view shows downstream affected objects and artifacts.
- Relevance score: 5
- Confidence: high

### DataHub Lineage

- Reference: DataHub lineage feature guide
- Source URL: https://docs.datahub.com/docs/features/feature-guides/lineage/
- Problem it solves: Tracks data flow across datasets, pipelines, dashboards, columns, and cross-platform dependencies with scoped expansion.
- Softland borrowing: Borrow centered lineage explorer, column-level focus, degrees of separation, hidden-asset handling, and time filtering with caveats.
- Softland should reject: Letting manual lineage and automatic lineage conflict invisibly.
- Candidate primitive: `FocusedLineageExplorer`: center object, one-to-three degree expansion, column/field-level toggle, manual/automatic edge badges.
- Failure mode warning: Latest-lineage filtering can look like historical truth if the UI does not explain what time means.
- Borrowing test: A user can ask "where did this field come from?" and "what uses it?" without expanding the whole graph.
- Relevance score: 5
- Confidence: high

## Scientific Visualization

### Distill, "How To Use t-SNE Effectively"

- Reference: Wattenberg, Viegas, and Johnson, "How to Use t-SNE Effectively"
- Source URL: https://distill.pub/2016/misread-tsne/
- Problem it solves: Shows how dimensionality-reduction plots can be useful but easy to misread without parameter awareness and multiple views.
- Softland borrowing: Borrow calibrated projection controls and explicit warnings for embeddings or similarity maps.
- Softland should reject: 2D embedding as knowledge truth or as first screen.
- Candidate primitive: `ProjectionHealthPanel`: shows method, parameters, stability checks, source population, and what distances do or do not mean.
- Failure mode warning: Clusters and distances in reduced space are seductive and often overinterpreted.
- Borrowing test: Users can distinguish accepted relations from mere projection proximity.
- Relevance score: 5
- Confidence: high

### UMAP Parameters Documentation

- Reference: UMAP documentation on parameters
- Source URL: https://umap-learn.readthedocs.io/en/latest/parameters.html
- Problem it solves: Makes visible how parameters like neighbors and minimum distance change the structure of a projection.
- Softland borrowing: Borrow parameter surfacing for any projection that rearranges knowledge.
- Softland should reject: Hiding projection parameters behind a clean "AI map."
- Candidate primitive: `ProjectionParameterLedger`: stores projection method, settings, input corpus, revision, and stability notes.
- Failure mode warning: Small parameter changes can change perceived structure while the UI looks equally confident.
- Borrowing test: Recomputing a projection with changed parameters creates a new projection revision, not a silent replacement.
- Relevance score: 4
- Confidence: high

### Jupyter Narrative Notebooks

- Reference: Jupyter Notebook Narratives
- Source URL: https://docs.jupyter.org/en/stable/use/use-cases/narrative-notebook.html
- Problem it solves: Interleaves code, data, visualizations, equations, and narrative for computational explanation.
- Softland borrowing: Borrow computational narrative as a replayable trail and artifact form.
- Softland should reject: Hidden execution order and stale outputs.
- Candidate primitive: `ExecutableNarrative`: source-grounded explanation with runnable cells, outputs, and explicit freshness.
- Failure mode warning: A notebook can show a polished result detached from the execution path that produced it.
- Borrowing test: A reader can rerun or inspect the provenance of every artifact output.
- Relevance score: 4
- Confidence: high

### ParaView

- Reference: ParaView documentation
- Source URL: https://docs.paraview.org/en/latest/
- Problem it solves: Provides pipeline-based scientific visualization over large datasets with filters, views, and derived representations.
- Softland borrowing: Borrow visible dataflow/pipeline stages for transformations from source into native objects and projections.
- Softland should reject: Expert-only pipeline complexity in the first user path.
- Candidate primitive: `TransformationPipelineView`: folded pipeline that can expand to show source -> parse -> derive -> relate -> project.
- Failure mode warning: Pipelines can become control panels that only experts can understand.
- Borrowing test: A non-expert can see which transformation stage produced a bad object and open only that stage.
- Relevance score: 3
- Confidence: medium

## Games

### Outer Wilds Ship Log

- Reference: Outer Wilds by Mobius Digital, plus durable ship-log documentation
- Source URLs: https://www.mobiusdigitalgames.com/outer-wilds.html?pubDate=20250223 and https://outerwilds.fandom.com/wiki/Computer
- Problem it solves: Supports nonlinear knowledge-first exploration through a persistent log and relation/rumor mode.
- Softland borrowing: Borrow knowledge progression as the real progression: discovered facts, unresolved clues, local mysteries, and relation webs.
- Softland should reject: Turning knowledge into checklist completion.
- Candidate primitive: `DiscoveryLog`: records what was found, what it anchors to, what remains unresolved, and what new local paths opened.
- Failure mode warning: Logs can substitute for understanding if users chase completion instead of synthesis.
- Borrowing test: A user can use the log to re-enter an unresolved thread without being told the answer.
- Relevance score: 4
- Confidence: medium

### Fog Of War

- Reference: "Modeling fog of war effects in AFSIM"
- Source URL: https://journals.sagepub.com/doi/10.1177/15485129211041963
- Problem it solves: Represents incomplete, uncertain, or hidden state as a deliberate part of the environment.
- Softland borrowing: Borrow fog as an epistemic state: unknown, unparsed, unvisited, stale, hidden by scope, or unsupported.
- Softland should reject: Fake mystery. Fog must mean a precise information condition.
- Candidate primitive: `EpistemicFog`: visual cover for folded or unknown regions with reason labels and reveal actions.
- Failure mode warning: Fog can frustrate if users cannot tell how to resolve it.
- Borrowing test: Every fogged region explains why it is fogged and what action would reveal or ground it.
- Relevance score: 4
- Confidence: medium

### Game Minimap Research

- Reference: "Exploring the Design of a Mixed-Reality 3D Minimap to Enhance Pedestrian Satisfaction in Urban Exploratory Navigation"
- Source URL: https://www.mdpi.com/1999-5903/14/11/325
- Problem it solves: Studies local orientation aids for exploratory navigation.
- Softland borrowing: Borrow minimap as local context, not global world map.
- Softland should reject: Persistent minimap clutter if it competes with source/trust inspection.
- Candidate primitive: `LocalWorldMinimap`: small orientation surface showing current object, enclosing source, nearby accepted/candidate edges, and way back.
- Failure mode warning: Minimap attention can detach the user from the actual material.
- Borrowing test: Users recover their previous position faster after inspecting a deep source span.
- Relevance score: 3
- Confidence: medium

### Zelda Map Pins

- Reference: Zelda Wiki on map pins
- Source URL: https://zeldawiki.wiki/wiki/Pin
- Problem it solves: Lets players externalize intended return points and personal orientation in a large world.
- Softland borrowing: Borrow user-authored markers for "return here," "check source," "candidate synthesis," and "needs review."
- Softland should reject: Undifferentiated pins that become private clutter.
- Candidate primitive: `EpistemicPin`: typed marker with owner, reason, linked object/span, and optional expiry.
- Failure mode warning: User marks can look like system truth unless visually separated.
- Borrowing test: A reviewer can distinguish personal pin, team pin, and accepted status without opening details.
- Relevance score: 3
- Confidence: low

## Architecture And Spatial Wayfinding

### Kevin Lynch, The Image Of The City

- Reference: Kevin Lynch, "The Image of the City"
- Source URL: https://mitpress.mit.edu/9780262620017/the-image-of-the-city/
- Problem it solves: Explains how people form legible mental maps through paths, edges, districts, nodes, and landmarks.
- Softland borrowing: Borrow imageability as a design criterion for knowledge terrain.
- Softland should reject: Literal city metaphor as a skin.
- Candidate primitive: `LocalWorldLegibility`: every local world has paths, boundaries, landmarks, entry points, and named districts appropriate to its material.
- Failure mode warning: Without landmarks and boundaries, a world becomes a canvas with lost objects.
- Borrowing test: After five minutes away, a user can re-enter a local world and name where they are and why.
- Relevance score: 5
- Confidence: high

### Legible London

- Reference: Transport for London, "Legible London Yellow Book"
- Source URL: https://content.tfl.gov.uk/ll-yellow-book.pdf
- Problem it solves: Creates a coherent pedestrian wayfinding system across a complex city with local maps, landmarks, walking radii, and consistent signs.
- Softland borrowing: Borrow consistent "you are here," nearby landmarks, range rings, and local route guidance.
- Softland should reject: Over-signage. Softland should not plaster every surface with instructions.
- Candidate primitive: `YouAreHereStack`: source, local world, selected object, current projection, and nearest return paths.
- Failure mode warning: Inconsistent signs across views force users to rebuild orientation privately.
- Borrowing test: A user can move from source list to object inspector to topology and always identify current context.
- Relevance score: 5
- Confidence: high

### Pattern Language

- Reference: Christopher Alexander's Pattern Language project
- Source URL: https://www.patternlanguage.com/
- Problem it solves: Provides a composable language of recurring spatial/social design problems and solutions.
- Softland borrowing: Borrow pattern-level names for view primitives so they can compose beyond one prototype.
- Softland should reject: Treating patterns as timeless truth rather than situated, testable design hypotheses.
- Candidate primitive: `ViewPattern`: named reusable view relation, such as Source-to-Native Hover or Local Topology Lens, with context, problem, forces, solution, and failure checks.
- Failure mode warning: Pattern catalogs can become abstract comfort if not tested in live workflows.
- Borrowing test: A future Goal 6 spec can reuse atlas-derived primitive names without re-explaining the whole theory.
- Relevance score: 3
- Confidence: medium

## Photography, Cinema, And Music

### Magnum Contact Sheets

- Reference: ICP entry on "Magnum Contact Sheets"
- Source URL: https://www.icp.org/content/magnum-contact-sheets
- Problem it solves: Reveals the process, sequence, alternatives, and selection decisions behind iconic final images.
- Softland borrowing: Borrow contact sheet logic for transformations: show candidates, near misses, accepted object, and rejected alternatives.
- Softland should reject: Showing only the final artifact as if it appeared without process.
- Candidate primitive: `TransformationContactSheet`: array of candidate derived units or relation candidates with selection rationale and source anchors.
- Failure mode warning: A final polished object hides the edit path that made it trustworthy or suspect.
- Borrowing test: A reviewer can compare accepted and rejected candidate objects and see why one survived.
- Relevance score: 5
- Confidence: high

### Lightroom Grid, Loupe, Compare, Survey

- Reference: Adobe Lightroom Classic view modes
- Source URL: https://helpx.adobe.com/au/lightroom-classic/help/view-photos.html
- Problem it solves: Supports professional triage and judgment by switching among collection, single item, pair, and multi-item comparison.
- Softland borrowing: Borrow these as epistemic inspection modes for sources, objects, candidates, and conflicting interpretations.
- Softland should reject: Pure aesthetic selection flow.
- Candidate primitive: `CandidateSurveyMode`: compare multiple imported interpretations while keeping anchors visible.
- Failure mode warning: Survey without provenance becomes preference sorting.
- Borrowing test: Users can choose between competing derived objects based on source evidence, not visual polish.
- Relevance score: 4
- Confidence: high

### DaVinci Resolve Timeline And Pages

- Reference: DaVinci Resolve manuals and training
- Source URLs: https://www.blackmagicdesign.com/support and https://www.blackmagicdesign.com/au/products/davinciresolve/training
- Problem it solves: Coordinates media bins, timelines, effects nodes, color decisions, audio tracks, and delivery in a single production environment.
- Softland borrowing: Borrow the separation of material organization, sequence editing, effect/derivation graph, and final artifact.
- Softland should reject: Professional-suite complexity before users trust one imported object.
- Candidate primitive: `MaterialProcessArtifactStack`: source material, transformation process, and output artifact in coordinated surfaces.
- Failure mode warning: Powerful page systems can hide which stage changed meaning.
- Borrowing test: A user can identify whether a change happened in source ingest, relation derivation, projection, or final artifact.
- Relevance score: 4
- Confidence: high

### Ableton Session And Arrangement Views

- Reference: Ableton Live Session View and Arrangement View
- Source URLs: https://www.ableton.com/en/live-manual/12/session-view/ and https://www.ableton.com/en/live-manual/12/arrangement-view/
- Problem it solves: Separates improvisational clip launching from linear arrangement while allowing material to move between them.
- Softland borrowing: Borrow the distinction between exploratory local world and stabilized artifact sequence.
- Softland should reject: Making exploration and publication the same surface.
- Candidate primitive: `ExplorationToArtifactBridge`: move selected probes, objects, and relations from exploratory workspace into a durable explanation or handoff.
- Failure mode warning: If session and arrangement diverge invisibly, users cannot tell what is exploratory versus committed.
- Borrowing test: A user can promote exploratory material into an artifact while preserving provenance and candidate/accepted states.
- Relevance score: 5
- Confidence: high

## What This Makes Visible

- Source-to-native mappings
- Local scope before global topology
- Truth state and uncertainty as first-class visual grammar
- Object identity across projections
- Transformation process, not only transformed result
- User probes, decisions, pins, and accepted/rejected alternatives

## What This Keeps Folded But Recoverable

- Full source bodies except the selected anchor
- Distant graph neighborhoods
- Long revision histories
- Secondary projection parameters
- Low-relevance candidate relations
- Expert-only transformation pipeline details

## What This Must Not Imply

- That a global map is the world
- That proximity in a projection means accepted relation
- That a transformed object is trustworthy without anchor inspection
- That manual relation edits and automatically derived relations are equivalent
- That clean visual organization means preserved understanding

## Failure Modes

- Global graph seduction: the view looks visionary but answers no trust question.
- Projection laundering: dimensionality reduction or AI clustering looks like fact.
- Hidden severance: source remains stored but no longer experientially reachable.
- Pane genericity: left/center/right becomes nav/content/details instead of Source World, Native World, Trust World.
- Interaction amnesia: user probes change views but leave no reusable trail.
- Malleability without lineage: users reshape views into private, unreviewable worlds.

## Acceptance Checks

- Select any native object and inspect its exact raw source span.
- Select any raw source span and see what native object or candidate unit came from it.
- Distinguish accepted, candidate, inferred, partial, stale, failed, disconnected, and unanchored without relying only on color.
- Move between outline, graph, timeline, and inspector while keeping object identity stable.
- Expand topology locally by degree and edge type without opening a global graph.
- Inspect a projection's method, parameters, source population, and limitations.
- Promote an exploratory finding into an artifact while preserving source, process, and decision trail.

## Open Questions

- What is the minimum Softland source-map schema needed for source-to-native hover in the first prototype?
- Should the first topology view support relation candidates, or should candidate review live only in the trust inspector?
- How much timeline/trail machinery belongs in the first Source-to-World View versus a later trail replay view?
- Which uncertainty states are implemented today versus only design targets?
- What is the right first "you are here" stack: source -> object -> projection, or local world -> source -> object?

## Next Recommended Goal

Goal 4: truth-state visual grammar. Goal 3 establishes the precedent field; the next useful move is to turn accepted/candidate/partial/stale/unanchored/disconnected into a concrete visual and interaction grammar before writing the first buildable view spec.
