# Annotated Bibliography For Goal 3

Status: draft research sweep, 2026-06-09.

## Origin Prompt

```text
Continue the Softland design/view research track. Execute Goal 3 only. Build a
precedent atlas, not a moodboard. Use primary or durable sources where possible.
Cite URLs.
```

## Goal

Provide a durable bibliography for the precedent atlas so future sessions can
return to primary references instead of inheriting only summaries.

## Inputs Read

Local inputs:

- `docs/current-mental-model/design/codex/orientation.md`
- `docs/current-mental-model/design/codex/goal-3/description.md`
- `docs/current-mental-model/build/imported-topology-view/PRINCIPAL_DESIGN_RESEARCH.md`
- `docs/vision/epistemic-framework.md`
- Checked prior goal folders; only `description.md` files existed.

External sources are listed below.

## Scope

This bibliography privileges official product documentation, standards, primary
project pages, publisher pages, and durable papers. Some game examples use
community documentation when official sources do not describe the relevant
interface mechanic in enough detail; those entries are marked lower confidence.

## Core Claim

The most useful sources are not the ones that look like Softland. They are the
ones that already solved pieces of Softland's trust problem: reversible
transformation, scoped orientation, uncertainty display, local wayfinding,
workflow pacing, and inspectable creative process.

## Findings Or Design Decisions

- Treat source maps, DevTools, DataHub, dbt, Perfetto, and OpenTelemetry as the
  hard engineering precedent family for transformation accountability.
- Treat Pad++, space-scale diagrams, Google Earth, Obsidian Local Graph, and
  Legible London as orientation/zoom precedents, not as visual skins.
- Treat Lightroom, Magnum contact sheets, DaVinci Resolve, and Ableton as
  workflow-pacing precedents that separate exploration, comparison, production,
  and artifact.
- Treat Distill, Victor, Observable, Jupyter, and Nicky Case as examples of
  manipulative understanding, but require stronger provenance than most
  public-facing examples carry.

## Bibliography

| Domain | Reference | Source URL | Source type | Softland use | Confidence |
|---|---|---|---|---|---|
| HCI and visualization | Shneiderman, "The Eyes Have It" | https://hci.ucsd.edu/220/EyesHaveIt.pdf | Durable paper PDF | Task-first ordering: scope, zoom/filter, details, relation, history, extract | High |
| HCI and visualization | Munzner, "A Nested Model for Visualization Design and Validation" | https://vis.csail.mit.edu/classes/6.859/readings/pdfs/Munzner-ANestedModelForVisualizationDesignAndValidation.pdf | Durable paper PDF | Separate domain job, data abstraction, encoding, and implementation | High |
| HCI and visualization | HCIL Treemap project | https://www.cs.umd.edu/projects/hcil/treemap/ | Primary research project page | Hierarchical source/object containment with aggressive compression caveats | High |
| HCI and visualization | MacEachren, "Visualizing Uncertain Information" | https://cartographicperspectives.org/index.php/journal/article/view/cp13-maceachren | Journal article | Uncertainty as view material, not afterthought | High |
| HCI and visualization | Inselberg, "Parallel Coordinates" | https://link.springer.com/book/10.1007/978-0-387-68628-8 | Publisher/DOI page | Multi-dimensional inspector lanes for provenance and trust attributes | Medium |
| ZUI and navigation | Pad++ | https://www.cs.umd.edu/projects/hcil/pad%2B%2B/ | Primary project page | Zoom as fundamental navigation over multiscale information | High |
| ZUI and navigation | Furnas and Bederson, "Space-Scale Diagrams" | https://doi.org/10.1145/223904.223934 | DOI/paper | Formalize what must survive across scale | High |
| ZUI and navigation | Prezi | https://prezi.com/ | Official product page | Authored traversal path through spatial material | Medium |
| ZUI and navigation | Google Earth | https://earth.google.com/web/ | Official product page | Scale bands, landmarks, and local re-entry | Medium |
| ZUI and navigation | Obsidian Graph view | https://obsidian.md/help/plugins/graph | Official help page | Local graph depth, filters, and active object centering | High |
| Explorable explanations | Bret Victor, "Up and Down the Ladder of Abstraction" | https://worrydream.com/LadderOfAbstraction/ | Primary essay | Step up to abstraction, step down to concrete evidence | High |
| Explorable explanations | Explorable Explanations hub | https://explorabl.es/ | Primary/durable hub | Interactive explanation as manipulation, not passive prose | High |
| Explorable explanations | Nicky Case, Loopy | https://ncase.me/loopy/ | Primary interactive tool | Candidate causal sketching with explicit uncertainty | High |
| Explorable explanations | Distill, "Communicating with Interactive Articles" | https://distill.pub/2020/communicating-with-interactive-articles/ | Durable article with DOI | Interactive article as replayable explanatory medium | High |
| Explorable explanations | Observable notebooks | https://observablehq.com/documentation/notebooks/ | Official docs | Reactive cells, collaboration, version history, imports | High |
| Malleable systems | Dynamicland FAQ | https://dynamicland.org/2024/FAQ/ | Primary project documentation | Computing as inhabitable place and communal medium | High |
| Malleable systems | Dynamicland Intro | https://dynamicland.org/2024/Intro/ | Primary project documentation | Real-world collaborative computation as orientation reference | High |
| Malleable systems | Ink and Switch, "Malleable Software" | https://www.inkandswitch.com/essay/malleable-software/ | Primary research essay | User-shaped tools with versioned local adaptation | High |
| Malleable systems | Folk Computer | https://folk.computer/ | Primary project page | Regions as objects and situated local authoring | Medium |
| Malleable systems | Ink and Switch, Potluck | https://www.inkandswitch.com/potluck/ | Primary project page | Gradual structure over informal text | Medium |
| Creative professional tools | Figma version history | https://help.figma.com/hc/en-us/articles/360038006754-View-a-file-s-version-history | Official docs | Scoped object/version focus and handoff history | High |
| Creative professional tools | Figma Dev Mode | https://help.figma.com/hc/en-us/articles/15023124644247-Guide-to-Dev-Mode | Official docs | Inspect mode for translating design into implementation | High |
| Creative professional tools | Figma multiplayer limits | https://help.figma.com/hc/en-us/articles/1500006775761-How-many-people-can-be-in-a-file-at-once | Official docs | Presence with performance/pacing limits | High |
| Creative professional tools | Blender workspaces | https://docs.blender.org/manual/en/latest/interface/window_system/workspaces.html | Official manual | Task-specific workspaces over one object model | High |
| Developer and lineage tools | ECMA-426 Source Map spec | https://tc39.es/source-map/ | Official standard | Bidirectional source-to-transformed mapping | High |
| Developer and lineage tools | Chrome DevTools source maps | https://developer.chrome.com/docs/devtools/javascript/source-maps | Official docs | Debugging authored source through transformed runtime | High |
| Developer and lineage tools | Perfetto UI | https://perfetto.dev/docs/visualization/perfetto-ui | Official docs | Multi-track trace navigation and selected event detail | High |
| Developer and lineage tools | OpenTelemetry tracing overview | https://opentelemetry.io/docs/reference/specification/overview/ | Official specification docs | Span/trace model for action and transformation causality | High |
| Developer and lineage tools | dbt data lineage guide | https://www.getdbt.com/blog/guide-to-data-lineage | Official durable guide | DAG lineage, impact analysis, and root cause paths | High |
| Developer and lineage tools | DataHub lineage guide | https://docs.datahub.com/docs/features/feature-guides/lineage/ | Official docs | Focused lineage, column-level lineage, degrees of expansion | High |
| Scientific visualization | Distill, "How to Use t-SNE Effectively" | https://distill.pub/2016/misread-tsne/ | Durable article with DOI | Projection caution and parameter-calibrated embeddings | High |
| Scientific visualization | UMAP parameters documentation | https://umap-learn.readthedocs.io/en/latest/parameters.html | Official project docs | Projection parameter ledger and stability caveats | High |
| Scientific visualization | Jupyter Notebook Narratives | https://docs.jupyter.org/en/stable/use/use-cases/narrative-notebook.html | Official docs | Computational narrative with executable evidence | High |
| Scientific visualization | ParaView docs | https://docs.paraview.org/en/latest/ | Official docs | Transformation/filter pipeline as scientific view model | Medium |
| Games | Outer Wilds official page | https://www.mobiusdigitalgames.com/outer-wilds.html?pubDate=20250223 | Official product page | Knowledge-first exploration in a changing world | Medium |
| Games | Outer Wilds computer/ship log documentation | https://outerwilds.fandom.com/wiki/Computer | Community documentation | Rumor mode and knowledge relation tracking | Medium |
| Games | Fog of War in AFSIM | https://journals.sagepub.com/doi/10.1177/15485129211041963 | Journal article | Information-state fog as calibrated unknown | Medium |
| Games | Mixed-reality 3D minimap study | https://www.mdpi.com/1999-5903/14/11/325 | Journal article | Minimap as local orientation aid | Medium |
| Games | Zelda map pins | https://zeldawiki.wiki/wiki/Pin | Durable community wiki | User-authored return points and intent markers | Low |
| Architecture and wayfinding | Kevin Lynch, "The Image of the City" | https://mitpress.mit.edu/9780262620017/the-image-of-the-city/ | Publisher page | Paths, edges, districts, nodes, landmarks for knowledge terrain | High |
| Architecture and wayfinding | Legible London Yellow Book | https://content.tfl.gov.uk/ll-yellow-book.pdf | Official/public design standard PDF | You-are-here, landmarks, walking radius, sign consistency | High |
| Architecture and wayfinding | Pattern Language | https://www.patternlanguage.com/ | Project/publisher page | Reusable named view patterns with forces and checks | Medium |
| Photography, cinema, and music | ICP, "Magnum Contact Sheets" | https://www.icp.org/content/magnum-contact-sheets | Museum/library entry | Process, sequence, alternatives, and selected final image | High |
| Photography, cinema, and music | Lightroom Classic views | https://helpx.adobe.com/au/lightroom-classic/help/view-photos.html | Official docs | Grid, loupe, compare, and survey for epistemic triage | High |
| Photography, cinema, and music | Blackmagic Design support/manuals | https://www.blackmagicdesign.com/support | Official docs | Production stages and reference manual for integrated creative pipeline | High |
| Photography, cinema, and music | Blackmagic Design Resolve training | https://www.blackmagicdesign.com/au/products/davinciresolve/training | Official training docs | Stage-specific professional workflows | High |
| Photography, cinema, and music | Ableton Session View | https://www.ableton.com/en/live-manual/12/session-view/ | Official manual | Exploratory clip/session workspace | High |
| Photography, cinema, and music | Ableton Arrangement View | https://www.ableton.com/en/live-manual/12/arrangement-view/ | Official manual | Stabilized linear artifact arrangement | High |

## What This Makes Visible

- Which sources are primary, official, standard, or lower-confidence community references.
- Which references directly inform the first Source-to-World View.
- Which references are useful but dangerous if copied at the surface level.

## What This Keeps Folded But Recoverable

- Full paper details and product manuals remain at cited URLs.
- Deep theoretical synthesis remains in `precedent-atlas.md`.
- Hard product calls remain in `borrow-reject-table.md`.

## What This Must Not Imply

- That all references are equally authoritative.
- That lower-confidence game/interface examples are safe to build from without validation.
- That source URLs settle design decisions by themselves.

## Failure Modes

- Bibliography as decoration: sources are cited but do not constrain design.
- Product cargo-culting: a famous tool is copied because it looks adjacent.
- Research laundering: a paper is cited to justify a decision it did not test.
- Stale source risk: product docs may change; future implementation specs should re-check live docs when relying on exact behavior.

## Acceptance Checks

- Every atlas entry has a URL.
- Primary or durable sources are used where practical.
- Lower-confidence sources are labeled rather than hidden.
- A future Goal 4 or Goal 6 session can trace a primitive back to at least one reference.

## Open Questions

- Should a later pass add ACM/IEEE formal citations for the HCI/ZUI papers?
- Should game precedents be strengthened with first-party manuals or screenshots from legally usable sources?
- Should Softland keep a separate "projection caution" bibliography for embeddings, clustering, and uncertainty visualization?

## Next Recommended Goal

Goal 4: truth-state visual grammar. The bibliography now supports a focused visual-language pass for truth, uncertainty, provenance, and relation.
