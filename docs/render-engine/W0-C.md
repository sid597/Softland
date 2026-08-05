# W0-C — chrome, envelope, frame-runtime, and D2 paper lane

**Status:** DRAFT · NONBINDING · campaign artifact, not a scope gate ·
2026-08-02. This paper prepares decisions reserved to Sid; it does not
make them. No finding below can cancel a capability already committed
by `ENGINE.md` §0. An omitted or unsupported practice becomes a demand
on the campaign. It never becomes evidence for a smaller engine.

**Campaign law pin:** `ENGINE.md` §0 governs this whole document.
Instruments choose roads, never scope. A failed receipt repairs its
rung and the full-breadth campaign continues. The seam demo remains the
completion gate.

**Lane boundary:** docs only. No product surface, server, engine code,
Studio/Playground source, or commit is part of W0-C. W0-A owns durable
machine verification; W0-B owns E0 measurements. This lane supplies the
paper contracts those lanes and the downstream engine can use.

## 0. Receipt header and hard verdict

Pre-write sibling check:

- branch: `docs/current-mental-model-local`;
- HEAD: `bd1914068f57ca19c5b85d993339c8dfe37fdb3d`;
- sibling commit: `docs: the campaign settlement — order ratified, MVP
  grammar out, the FULL-BREADTH CAMPAIGN LAW in (third 08-02 ruling)`;
- existing foreign Studio/Playground and code/test changes were not
  touched.

**Hard verdict:** the chrome hypothesis in `ENGINE.md` §9 survives only
in amended form. No audited in-envelope chrome needs a sixth coverage
mode, but `screen-flag + overlay patterns` is not yet a sufficient
provider contract. Practice-weight chrome requires:

1. a hybrid coordinate seam: anchors project from world, container, or
   3D-region space while strokes, handles, and hit targets keep
   screen-pixel dimensions;
2. explicit visual-geometry / mathematical-interior / hit-slop
   relationships, including coarse-pointer variants;
3. a single paint/pick order truth for artifact and chrome;
4. depth and object-ID policies for 3D overlays and selection
   silhouettes;
5. a clocked-overlay schedule for marching, pulsing, fading, laser, and
   other transient feedback;
6. layout-result ownership for carets, text selections, baselines, and
   text resize handles;
7. scene-color access for eyedropper and sampling feedback.

The existing screen flag is a useful screen-space transport seed. It
does not, by itself, express a world-anchored fixed-pixel handle, a
depth-tested 3D gizmo, or a clocked overlay. Those are registered
demands on the committed foundation and 2D/3D floors, not capability
cuts.

Visual-execution and product-clarity scores are **N/A**: W0-C has no
Softland product surface to score. Assigning a premium-product score to
paper or to absent chrome would fabricate lived evidence.

## 1. Source ledger

### 1.1 Softland source, re-read at the bytes

- Production scheduling is demand-only today: the RAF consumer skips
  when world identity is unchanged
  (`src/app/client/workspace/runtime/render.cljs:219-223`).
- The persistent render target is machinery-exists/default-OFF
  (`runtime/render.cljs:18`); copy-to-swap runs only when enabled
  (`renderer.cljs:1893-1900`).
- The central frame is one hand-positioned sequence of shadows, rect
  pools, content text, isolated text geos, command/status/settings
  chrome, and diagnostics (`renderer.cljs:1758-1889`).
- The shared container entry is `(offset.x, offset.y, scale, flags)`;
  the screen flag makes the whole instance bypass world camera
  pan/zoom (`renderer.cljs:553-598`). It does not split anchor space
  from metric space and carries no rotation.
- CPU pick orders containers by descending `:layer`
  (`scene_store.cljc:196-224`), while paint follows the central frame
  sequence. This is the known divergence W1 must remove.
- The floor default zoom is `[0.1, 8.0]`, but material validation admits
  each bound in `[0.01, 1000]` and the worn values drive the live camera
  (`space_material.cljc:18-23,47-61`;
  `ground.cljs:3823-3840`).

### 1.2 tldraw source corpus — pinned, not reconstructed

Source snapshot: tldraw `main` at
[`4c6a5fbc75a082738cd6d3008c8497d54defcf6c`](https://github.com/tldraw/tldraw/commit/4c6a5fbc75a082738cd6d3008c8497d54defcf6c),
committed 2026-07-31.

- The default registry has thirteen overlay utilities, including
  selection, handles, shape indicators, snap indicators, brush,
  scribble, arrow/binding hints, zoom brush, and collaborator variants
  ([registry source](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/packages/tldraw/src/lib/defaultOverlayUtils.ts#L1-L30)).
- Selection foreground is not one box: it registers separate resize,
  rotate, mobile-rotate, crop, and text-resize forms, with distinct
  visual and interactive sizing
  ([selection overlay](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/packages/tldraw/src/lib/overlays/SelectionForegroundOverlayUtil.ts#L39-L176)).
- Shape handles distinguish endpoint/vertex/create/clone roles, give
  vertices hit-test priority, reverse their paint order to match, and
  divide visible and hit radii by zoom
  ([handle overlay](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/packages/tldraw/src/lib/overlays/ShapeHandleOverlayUtil.ts#L22-L209)).
- Snap chrome contains both point-alignment lines and equal-gap
  indicators, with line/tick sizes divided by zoom
  ([snap overlay](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/packages/tldraw/src/lib/overlays/SnapIndicatorOverlayUtil.ts#L17-L194)).
- Connector feedback includes target-shape indicators, edge snap
  circles, intended-vs-snapped endpoint stubs, precision markers, and a
  low-zoom dashed-to-solid fallback
  ([target hints](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/packages/tldraw/src/lib/overlays/ArrowHintOverlayUtil.ts#L41-L179),
  [binding hints](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/packages/tldraw/src/lib/overlays/ArrowBindingHintOverlayUtil.ts#L21-L240)).
- tldraw now golden-tests overlay appearance as a dedicated corpus,
  including selection, rotation, shape handles, brush, snap, and
  scribble states
  ([overlay snapshot suite](https://github.com/tldraw/tldraw/blob/4c6a5fbc75a082738cd6d3008c8497d54defcf6c/apps/examples/e2e/tests/test-overlay-snapshots.spec.ts#L8-L260)).

### 1.3 Figma community/practice corpus — official primary sources

This is a qualitative, category-stratified sweep, not a statistical
sample of private files. Figma Community pages were not machine-readable
without an account, so the corpus uses Figma's official Community guide,
official gallery/category surface, and official practice documentation.
That is sufficient to identify demand classes; it does not support
frequency claims.

- The Community contains duplicable Figma/FigJam files, UI kits,
  wireframes, websites, whiteboarding resources, and slide templates
  ([Community guide](https://help.figma.com/hc/en-us/articles/360038510693-Guide-to-the-Figma-Community)).
- Figma's official gallery advertises 300+ curated templates and 1,000+
  Community templates across interface, diagramming, planning, and
  presentation practices
  ([template gallery](https://www.figma.com/templates/)).
- Current product practice includes responsive/nested auto layout
  ([auto layout](https://help.figma.com/hc/en-us/articles/360040451373-Explore-auto-layout-properties)),
  components/variants
  ([variants](https://help.figma.com/hc/en-us/articles/360056440594-Create-and-use-variants)),
  variables and modes
  ([variables](https://help.figma.com/hc/en-us/articles/15339657135383-Guide-to-variables-in-Figma)),
  practice-weight typography
  ([text styles](https://help.figma.com/hc/en-us/articles/360039957034-Create-and-apply-text-styles)),
  open and branching vector networks
  ([vector networks](https://help.figma.com/hc/en-us/articles/360040450213-Vector-networks)),
  non-destructive booleans
  ([boolean operations](https://help.figma.com/hc/en-us/articles/31130266267287-FD4B-Combine-shapes-using-boolean-operations)),
  image fill/crop/tile behavior
  ([image properties](https://help.figma.com/hc/en-us/articles/360041098433-Adjust-the-properties-of-an-image)),
  vector/alpha/luminance masks
  ([masks](https://help.figma.com/hc/en-us/articles/360040450253-Masks)),
  stroke profiles/caps/joins/dashes
  ([strokes](https://help.figma.com/hc/en-us/articles/360049283914-Apply-and-adjust-stroke-properties)),
  blends
  ([blend modes](https://help.figma.com/hc/en-us/articles/360040667874-Use-blend-modes-to-create-unique-effects)),
  and a current effect stack extending through shadows, layer/background
  blur, glass, noise, and texture
  ([effects](https://help.figma.com/hc/en-us/articles/360041488473-Apply-effects-to-layers)).
- Prototype practice includes overlays, overflow/scroll behavior,
  variables, conditionals, Smart Animate, video, and animated GIFs
  ([prototyping guide](https://help.figma.com/hc/en-us/articles/360040314193-Guide-to-prototyping-in-Figma)).

### 1.4 Blender scene-composition practice — official primary sources

- Viewport chrome at the relevant scope includes grids/floor/axes,
  camera guides, non-geometry objects such as cameras and lights,
  relationship lines, selected-object outlines, origins, annotations,
  measurements, normals, and grease-pencil overlays
  ([viewport overlays](https://docs.blender.org/manual/en/latest/editors/3dview/display/overlays.html)).
- Composition uses separate translate/rotate/scale gizmos, color-coded
  axes, plane handles, and selectable pivots
  ([gizmos](https://docs.blender.org/manual/en/latest/editors/3dview/display/gizmo.html),
  [pivots](https://docs.blender.org/manual/en/latest/editors/3dview/controls/pivot_point/index.html)).
- The relevant material ladder is the interactive, physically based
  viewport/material model represented by Principled BSDF/OpenPBR-like
  parameters
  ([Principled BSDF](https://docs.blender.org/manual/en/latest/render/shader_nodes/shader/principled.html)).

## 2. Chrome walkthrough ledger

### 2.1 Provider vocabulary

The ledger assigns one or more provider classes. These are contracts,
not prescribed pipeline implementations:

- **O-SCREEN:** pure screen-space overlay; position and dimensions are
  screen-relative.
- **O-ANCHORED:** world/container/region anchor projected to screen,
  then decorated with screen-pixel metrics.
- **O-WORLD:** chrome whose geometry and width lawfully scale with the
  world.
- **O-DEPTH:** 3D-region overlay with an explicit `depth-tested`,
  `x-ray`, or `always-front` policy; may read depth or ID resources.
- **O-CLOCK:** overlay whose state changes without a world mutation and
  therefore declares a clock/deadline to the scheduler.
- **O-READ:** overlay that reads scene color, depth, or an ID resource.
- **O-LAYOUT:** overlay located by the authoritative text/layout result.

Every provider declares: anchor space · metric space · coverage mode ·
paint/theme · composition/depth policy · geometry truth owner · visual
width · hit slop · pick priority · lifetime (`gesture`, `hover`,
`selection`, `settled`) · invalidation causes · regime.

### 2.2 Walkthrough

| ID | Practice walk | Required chrome | Provider | Receipt / source | Campaign demand; never a cut |
|---|---|---|---|---|---|
| CH-01 | tldraw/Figma select one or many rotated objects | outline, bounds, corner/edge resize, rotation handles, selection-vs-focus distinction | O-ANCHORED | tldraw selection foreground; rotated overlay goldens | Hybrid anchor/metric projection; full affine bounds; one pick/paint order. |
| CH-02 | Crop or mask an image | crop boundary, dimmed outside, crop handles, image transform preview | O-ANCHORED + O-READ | tldraw crop states; Figma image crop/masks | Image UV transform; mask/group pass; visual/hit handle separation. |
| CH-03 | Drag a marquee or zoom brush | translucent area plus constant-width boundary; directional crossing semantics remain legible | O-ANCHORED or O-SCREEN | tldraw brush/zoom-brush source and goldens | Overlay batch for gesture-lifetime rects; no durable material write. |
| CH-04 | Lasso, erase, or laser through the hand regime | pressure/taper preview, fade tail, optional marching/pulse | O-ANCHORED + O-CLOCK | tldraw scribble source | Tessellated/analytic live stroke plus explicit expiry/clock; no world-change hack. |
| CH-05 | Edit a line/path | endpoint, vertex, tangent, midpoint-create, bend, cap/join feedback | O-ANCHORED | tldraw handle roles; Figma vector/stroke docs | Day-one open-path chrome; D2 geometry owner; hit priority independent of paint order. |
| CH-06 | Draw or reconnect an arrow | endpoint handle, target outline, snap points, intended-vs-bound stub, exact/precise marker, arrow preview | O-ANCHORED | tldraw arrow and binding hints | Durable reference edge owns binding; renderer owns preview/feedback, including curved/angled paths. |
| CH-07 | Align or distribute objects | point guide, edge/center guide, equal-gap ticks, numeric distance, smart-spacing continuation | O-ANCHORED | tldraw snap points/gaps; Figma layout guides | Constant-pixel guides tied to world anchors; measure labels route through shaped text. |
| CH-08 | Resize an auto-layout frame or component | padding/gap bands, insert/reorder marker, hug/fill/fixed preview, overflow/clip indication | O-ANCHORED + O-LAYOUT | Figma auto-layout corpus | Layout engine emits authoritative guide geometry; chrome never reimplements layout. |
| CH-09 | Type and select text | caret, range highlight, composition underline, baseline/box, overflow and resize handles | O-LAYOUT + O-CLOCK | Figma typography; Softland T0/T1/T2 evidence | One layout result for shape, wrap, caret, selection, clip, hit; clock only for caret blink. |
| CH-10 | Inspect component/instance/variant/variable state | instance/master outline, override/deviation badge, exposed-property anchors, state transition preview | O-ANCHORED + O-CLOCK where transitioning | Figma variants/variables corpus | Material identity/provenance supplies state; chrome visualizes it without becoming truth. |
| CH-11 | Draw/edit open or branching vectors | points, segments, tangent handles, branch junctions, close-path affordance, fill-rule/inside feedback | O-ANCHORED | Figma vector-network corpus | **Envelope tripwire reopened:** branching networks cannot remain silently OUT if icon/illustration practice is ratified as Figma-core. |
| CH-12 | Eyedrop/sample a composite | reticle, sampled swatch/value, valid/invalid region, latency feedback | O-SCREEN + O-READ | Figma effect/image practice; `ENGINE` eyedropper seed | Scene-color resource and async readback contract; no GPU→CPU synchronization in the hand path. |
| CH-13 | Manipulate a 3D object | translate/rotate/scale gizmos, plane handles, pivot/orientation, numeric delta | O-DEPTH + O-ANCHORED | Blender gizmo/pivot manuals | Region projection plus explicit depth mode and region-routed picking. Screen flag alone fails. |
| CH-14 | Orient inside a 3D region | adaptive grid/floor/axes, view/navigation gizmo, camera frame/safe guides | O-DEPTH + O-SCREEN | Blender overlay manual | Region-owned overlay pass; screen-pixel axis labels; camera state is input, not material redefinition. |
| CH-15 | Select and inspect 3D scene objects | silhouette/outline, origin, camera/light/empty icons, relationship lines, bounds | O-DEPTH + O-READ | Blender overlay manual | ID/depth or geometric-outline road priced later; relationship identity remains material/document truth. |
| CH-16 | Snap and measure in 3D | projected snap targets, constraint axes/planes, distances/angles, normals | O-DEPTH + O-ANCHORED + O-LAYOUT | Blender overlay/manual practice | Numeric/text overlay in region; transform-space declaration; constant-screen normals where requested. |
| CH-17 | Annotate or draw in 3D | projected stroke preview, active-plane grid, occluded/x-ray choice, anchor marker | O-DEPTH + O-ANCHORED | Blender annotation/grease-pencil overlays | Shared 2D path atom receives 3D transform; overlay declares depth law rather than duplicating stroke material. |
| CH-18 | Preview PBR while editing scene | selected light/camera indicators, material-preview state, environment/HDRI cue, compile/loading state | O-DEPTH + O-SCREEN + O-CLOCK | Blender viewport/PBR manuals | PBR ladder includes interactive viewport feedback; scheduler wakes for resource readiness without becoming a path tracer. |
| CH-19 | Play prototype/live-component motion | hotspots/connections in authoring, transition bounds/path, play state, focus/hover states | O-ANCHORED + O-CLOCK | Figma prototype/Smart Animate practice | Motion already carried IN; scheduler gets clocked regions and deterministic time injection. |
| CH-20 | Work across zoom and input modalities | constant-pixel hairlines/handles at legal zoom; coarse-pointer larger hit targets; low-zoom simplification | every provider | tldraw fixed-pixel and coarse-pointer code; D-25 | Receipts tag default `[0.1,8]` vs legal `[0.01,1000]`; visual size and hit slop are separate policies. |
| CH-21 | Collaborator/agent hand points or marks | cursor, presence outline, remote brush/scribble, ownership label | O-ANCHORED + O-CLOCK | tldraw collaborator overlay registry | Multiplayer remains outside the provisional envelope, but the agent hand is a distinct committed/future pressure; do not delete the provider class. |
| CH-22 | Select through overlap, masks, and mixed 2D/3D regions | hover/preselect, pick cycle, hidden/locked indication, active editing scope | O-ANCHORED + O-DEPTH | tldraw focus/selection source; Blender selection | One ordered scene truth with explicit pickability/visibility; masks and region routing must affect paint and pick coherently. |

### 2.3 Demand register, prioritized for the foundation

These priorities order repairs and contracts. They do not order product
scope.

**P0 — trust invariants**

1. **One geometry truth, two readers:** visual AA, mathematical
   interior, hit slop, and coarse-pointer expansion are declared per
   atom/overlay. Pick-parity failure halts its rung for repair under E0.
2. **One order truth:** the ordered draw plan is the source for paint
   and reversed/topmost pick traversal; depth regions explicitly own
   their interior order.
3. **Hybrid coordinate contract:** anchor space and metric space are
   separate. No provider may infer fixed-pixel behavior from a whole
   container's screen flag.
4. **Depth/ID semantics:** every 3D overlay says depth-tested, x-ray, or
   always-front. Selection silhouettes and eyedropper name the resource
   they read.
5. **Text/layout ownership:** caret, selection, IME, label, and hit-test
   geometry come from one layout result.

**P1 — practice fluency**

6. Registered overlay families for selection, handles, brush/scribble,
   snap/measurement, binding hints, text chrome, sampling, and 3D
   gizmos.
7. Scheduler support for transient and clocked overlays, with idle when
   no work is due.
8. Theme/contrast state and visible distinctions among default, hover,
   active, selected, focus-visible, locked, disabled, loading, and
   error.
9. Low-zoom simplification and high-zoom precision rules, all
   regime-tagged across the legal material domain.

**P2 — polish and extension pressure**

10. Minimap/export inclusion policies for chrome (default: authoring
    chrome does not export; annotations do because they are material).
11. Multi-hand/agent overlay ownership and latency state.
12. Accessibility mirrors for non-DOM canvas chrome; renderer coverage
    is not semantic accessibility.

## 3. Corpus sweep → envelope pressure

### 3.1 tldraw-full pressure

The source corpus corroborates tldraw-full as more than a shape list.
Its floor includes pressure-sensitive ink, rich text, arrows/bindings,
images/video/embeds, selection/manipulation, crop, snap and gap
feedback, export, and a first-class overlay registry. For Softland:

- chrome is an atom-adjacent system with its own registration,
  rendering, picking, themes, lifetimes, and golden bank;
- fixed-pixel visual size is distinct from interaction radius;
- connector chrome is inseparable from durable edge/binding truth;
- transient preview and durable shape remain distinct lifecycle tiers;
- product parity cannot be claimed from artifact pixels alone.

### 3.2 Figma-core pressure

The category-stratified corpus yields six practice strata:

| Practice stratum | Recurring material/artifact demand | Engine/composition demand | Chrome/process demand |
|---|---|---|---|
| UI kits, mobile/web screens, wireframes | frames, rich text, images, reusable components, variables/styles | masks, clips, group opacity, blends, shadows/blur, image fills | precision selection, guides, spacing/measurement, responsive resize |
| Design systems | master/instance/variant/deviation, typed properties, modes | identical render paths for masters and instances; stable identity | override/variant/state visualization, library/provenance cues |
| Responsive layout | nested horizontal/vertical/grid flows, constraints, content-driven size | clipping/culling from authoritative layout results | padding/gap/insert/reorder and resize previews |
| Icons and illustration | open/closed/branching vectors, booleans, holes, variable strokes, gradients | tessellation/path coverage, fill rules, masks/effects | point/tangent/branch edit chrome and close-path affordance |
| Prototype and live interaction | transitions, state changes, variables/conditions, overlays, scroll | clocked frame policy, offscreen groups, compositing, moving sampled media | hotspots/connections, play/transition feedback, one-live-state authoring |
| Presentations/marketing/media | image-heavy and typographic composition, repeated styles, video/GIF in prototype contexts | sampled media, color, effect stacks, export | crop/sample/media-state feedback |

Findings that reopen or sharpen the provisional envelope:

1. **Vector-network tripwire: REOPENED, not adjudicated.** `ENGINE.md`
   §3 names vector networks OUT but says icon-drawing practice deepening
   reopens them. Figma's own documentation describes branching vector
   networks as the model for detailed icons and illustration. Sid must
   decide whether `Figma-core at practice weight` includes this current
   practice or deliberately names a narrower vector floor. Until then,
   the demand stays visible and nothing downstream may pretend it was
   disproved.
2. **Effect-stack drift: registered.** The provisional list names
   masks, blends, shadows, and blur. Current Figma practice also names
   glass, noise, and texture. These pressure paint and composition,
   especially backdrop sampling and effect ordering. They are not
   silently excluded by the older enumeration.
3. **Moving sampled media: registered.** Video/GIF in prototypes is a
   distinct sampled-resource/scheduling demand adjacent to the already
   declared motion floor. It is not the same thing as parameter
   interpolation.
4. **Typography is structural, corroborated.** Font family/weight/size,
   line height, letter/paragraph spacing, OpenType features, lists, and
   language variation confirm T0/T1/T2 as a cross-engine migration, not
   garnish.
5. **Auto layout/components/variables are mostly material and process,
   not new coverage modes.** Their renderer pressure is lawful bounds,
   clips, effects, instances, transition time, and chrome. The renderer
   must serve them without defining them.

### 3.3 Blender scope pressure

The scene-composition corpus supports the current proposed boundary:

- IN: region camera/depth, meshes, transforms, hierarchy, cameras and
  lights as scene objects, gizmos/pivots/snapping, selection/inspection,
  viewport overlays, annotations/strokes in 3D, and an interactive
  material/lighting ladder through PBR;
- NOT implied by that floor: sculpting, procedural geometry/shader-node
  authoring as a full practice, volumes, offline photorealism, or path
  tracing.

This is a scope-description draft, not a new ruling. Only Sid can ratify
or expand it.

## 4. Envelope ratification paragraph — RATIFIED 2026-08-02 (Sid)

> **RATIFIED 2026-08-02 (Sid) — as drafted, all clauses, with one
> rider recorded below.** Ratify the rendering
> envelope as **tldraw-full**, **Figma-core at practice weight**, and a
> **Blender-style scene-composition floor**. Figma-core includes the
> artifact and authoring requirements needed for real interface,
> design-system, responsive-layout, icon/illustration, and live-state
> work: shapes and paths; open strokes, closed contours, and holes;
> booleans; images; masks/clips; blends; ordered effects including
> shadows and blur; practice-weight typography; reusable
> components/instances/variants; variables/styles; precision layout
> and manipulation chrome; and motion/transition support. The
> corpus-found vector-network, glass/noise/texture, and moving-media
> pressures remain visible demands and require an explicit Sid word if
> any is to be staged outside that Figma-core floor; omission is not
> exclusion. Blender scope means scene composition and inspection:
> grey-box geometry, hierarchy, cameras, lights, transforms, picking,
> gizmos, viewport overlays, annotations/strokes in 3D, and a material
> and lighting ladder through interactive PBR. It does **not** mean
> sculpting, a procedural-authoring suite, volumes, offline
> photorealism, or path tracing unless Sid separately rules them in.
> The floor-default zoom clamp remains **[0.1, 8.0]**, while material
> may legally wear bounds inside **[0.01, 1000]**. Therefore every
> precision/performance receipt states the extent, normalization,
> zoom, and backend regime it proves; an engine road may change across
> those regimes, but default-clamp success never waives legal material.
> This draft keeps the legal validator intact rather than narrowing it.

**Ratification record (2026-08-02, Sid — verbatim in `vision/LOG.md`
fourth 08-02 entry):** "decision 1 all yes." Every clause adopted as
drafted: tldraw-full · Figma-core with the three corpus reopeners —
branching vector networks, glass/noise/texture, moving sampled media —
explicitly IN · Blender scene-composition floor through interactive
PBR · legal zoom `[0.01, 1000]` KEPT and priced per regime (D-25's
price-the-domain closure; the validator stays intact).

**The one rider (Sid's edit):** the exclusions — sculpting, procedural
geometry/shader authoring, volumes, offline photorealism, path
tracing — are **campaign-scoped, not forever**: "tbey can stsy out for
now but not like cant wver be build because i will soon ask for
sculpting and node authoring." Nothing in this campaign may treat them
as architecturally impossible; adding any of them later is one new Sid
word, and sculpting + node authoring are an expected near-future ask,
not a hypothetical.

## 5. Frame-graph + scheduler contract

**Status:** DRAFT · NONBINDING implementation contract. Data and a
loop, not a framework. W2-B makes it code-real; W4 extends the same
seam with groups/effects/time. This paper does not prescribe Clojure
types, a library, or a class hierarchy.

### 5.1 Outcome and negative-space contract

The central frame body becomes a stable executor over validated data.
An atom, overlay, effect, or region family **registers** resources,
passes, batches, pick geometry, and invalidation causes. It never adds
a hand-positioned draw branch to the executor.

The contract is violated if adding the image atom, path atom, mask,
group blur, or 3D region requires editing the central encode loop to
name that family. Extending the generic executor for a genuinely new
pass/resource capability is allowed; naming one more atom in the frame
body is not.

### 5.2 Data model

Illustrative shape, not binding syntax:

```clojure
{:graph/version 1
 :resources
 {:scene-color {:kind :color :lifetime :frame :size :viewport}
  :present      {:kind :external-swap}
  :scene-depth  {:kind :depth :enabled-when #{:region/3d}}
  :object-id    {:kind :id :enabled-when #{:selection/silhouette}}}

 :passes
 [{:pass/id :flat/main
   :pass/kind :render
   :sampled-reads #{}
   :attachments {:color {:resource :scene-color
                         :load :clear :store :store}}
   :batches :registered/flat
   :order [:scene/order]}
  {:pass/id :overlay/main
   :pass/kind :render
   :sampled-reads #{}
   :attachments {:color {:resource :scene-color
                         :load :load :store :store}}
   :batches :registered/overlay
   :order [:overlay/z]}
  {:pass/id :present
   :pass/kind :copy-or-present
   :copy-reads #{:scene-color}
   :copy-writes #{:present}}]

 :schedule {:policy :on-demand
            :clock-source :injected/monotonic
            :causes #{:world :viewport :resource :interaction
                      :clock :readback :device-recovery}}}
```

The actual plan may direct-present when no intermediate is required.
`:scene-color` is a logical resource, not a mandate to enable the
currently dormant persistent target on every frame. The plan chooses
direct swap, persistent intermediate, group target, or region target
from declared needs.

Attachment load/store is not a sampled read. No pass may bind the same
texture subresource as both a sampled/storage read and a writable
render attachment. A backdrop or other sampled-feedback effect declares
a separate prior-scene snapshot/intermediate, or ping-pongs between two
scene-color resources; the graph makes the copy/producer edge and
lifetime explicit. It never relies on undeclared read-write aliasing.

### 5.3 Family registration

Each atom/overlay family registers a descriptor containing at least:

- stable family ID and version;
- pass class and required resource formats/usages;
- pipeline/bind-group/buffer producer;
- batch collector and stable order key;
- geometry-truth owner and pick reader;
- visibility, culling, clip/mask, and depth policy;
- anchor space and metric space for chrome;
- invalidation signature and optional damage bounds;
- lifetime/budget owner and cleanup hook;
- deterministic receipt identity and environment-sensitive inputs.

Registration is append-only at the registry seam. The executor sees
generic descriptors and batches. Central `cond`, `case`, or manual
calls naming `rich-rect`, `msdf`, `slug`, `shadow`, `image`, `path`, or
`region-3d` are forbidden after migration.

### 5.4 Ordered scene truth

The compiled plan emits one total order for 2D/composited batches:

```text
scene order = (pass order, layer/stack key, family tie-break,
               material identity, stable instance identity)
```

- Paint traverses forward.
- CPU pick traverses the corresponding pickable projection in reverse.
- Masks/clips/group visibility modify both projections.
- Hit slop can widen pick geometry but cannot change stack order.
- A 3D region is one ordered compositor item in the 2D world; inside
  the region, depth owns picture order and the region's pick router
  returns the resolved inner identity.
- Overlay order is explicit and can be above/below artifact groups, but
  remains part of the same plan and receipt.

This replaces today's `:layer`-sorted CPU pick versus hand-sequenced
paint coherence-by-producer-discipline.

### 5.5 Graph compile and validation

Compile when registry, enabled capabilities, formats, or viewport
resource shape changes — not once per ordinary frame.

Validation fails closed on:

1. duplicate pass/resource/family IDs;
2. cycles or unstable topological ties;
3. read-before-produce, illegal usage flags, incompatible formats, or a
   sampled/storage-read and writable-attachment alias on the same
   subresource;
4. more or fewer than one presentation terminal;
5. a registered drawable without geometry/pick ownership;
6. an overlay without anchor/metric/hit/depth policy;
7. a clocked family without an injected clock and stop condition;
8. a family whose pick order cannot be derived from scene order;
9. undeclared readback or GPU→CPU synchronization in the hand path;
10. an enabled resource with no lifetime/budget owner.

### 5.6 Executor loop

The stable loop is deliberately boring:

1. receive a scheduler pulse with explicit causes and time;
2. snapshot the world/registry/resource revisions once;
3. select the already-compiled plan for enabled capabilities;
4. collect registered batches; cull and stable-sort by plan order;
5. compute invalidated resources/regions from declared causes;
6. encode each due pass in topological order;
7. submit once, present once, and enqueue only declared asynchronous
   readbacks;
8. record plan hash, revision set, causes, clock, batches, resources,
   and environment fingerprint for replay.

No atom may recursively request a draw while the frame is encoding.
It invalidates data or registers a future deadline at an edge; the
scheduler coalesces the work.

### 5.7 Scheduler contract

The scheduler owns **when** a compiled graph executes, not what the
scene means. It supports these cause classes:

- `:world` — durable/material or camera/world projection changed;
- `:viewport` — size, DPR, color/format, or device state changed;
- `:resource` — font/image/mesh/pipeline upload or compile became ready;
- `:interaction` — pointer preview, hover, selection, drag, or chrome
  changed without a durable world change;
- `:clock` — one or more registered regions/overlays have a due
  animation deadline;
- `:readback` — golden capture, export, thumbnail, eyedropper, or probe;
- `:device-recovery` — resources/plan must be recreated.

Rules:

1. Coalesce causes to at most one normal encode per RAF opportunity.
2. Sleep when there is no invalidation, deadline, or readback. Do not
   turn motion into an always-on global loop.
3. A clocked region registers `next-deadline`, target cadence, and stop
   predicate. Region cadence may differ; the compositor samples the
   most recent completed region texture.
4. Hand-regime interaction is highest latency priority; settle/resource
   work may be deferred or budgeted but not starved.
5. No ordinary frame performs synchronous GPU readback.
6. Replay injects recorded logical time and cause order; shader wall
   time never becomes material truth.
7. Deterministic tie-breaking never depends on map iteration, promise
   completion race, or registration call order.
8. A pass is absent only when its capability is explicitly disabled by
   the compiled capability/material set. Once a committed capability is
   enabled for a rung, failure records the resource/pass and halts that
   rung for repair; the executor may not recover by skipping the pass,
   dropping the capability, or shrinking the graph. The campaign
   continues after repair under §0.

### 5.8 Migration map from current code

| Current fact | Contract landing |
|---|---|
| world-identity skip in `runtime/render.cljs:219-223` | becomes scheduler cause `:world`, preserving idle-on-clean behavior |
| `use-persistent-render-target? false` | becomes a logical scene-color resource choice; direct-present remains legal until a pass requires an intermediate |
| hand-sequenced family draws in `renderer.cljs:1758-1889` | each family registers batches/order; executor no longer names them |
| CPU `:layer` pick order | replaced by pick projection derived from the compiled scene order |
| screen-flag container | retained as one transport road for pure screen space; hybrid anchored chrome gets explicit anchor+metric policy |
| island-probe offscreen 3D ancestor | becomes a registered region pass/resource and 2D compositor batch under the same scheduler |

### 5.9 Code-real receipts reserved for W2-B/W4

1. Existing five families migrate with W0-A zero-diff golden and
   determinism receipts.
2. Static/executable fence: adding a test image/path family changes a
   registry entry and family implementation, not the executor body.
3. Paint/pick overlap corpus returns the same topmost identity across
   families, masks, clips, and nested containers.
4. Direct-present and intermediate-present plans are golden-equivalent
   when no effect requires the intermediate.
5. Clean scene produces no frames; interaction wakes one; a clocked
   region wakes only to its declared cadence and sleeps on stop.
6. Replayed cause/time/batch record produces the same plan hash and
   pixels on the same fingerprinted environment.
7. Offscreen group, mask, backdrop blur, readback, and 3D-region fixtures
   extend graph data/resource providers rather than adding central
   family branches.

## 6. D2 round preparation — material truth stays Sid's

**Status:** RULED 2026-08-02 — choice A (§6.2). The question set below
stands as the decision record and as W1/D2 contract input: the
falsifier set (§6.4) and common fields (§6.3) now bind the
centerline-truth representation. The renderer never defines the
material.

### 6.1 Settled before the round

The day-one shape family must represent, preserve identity for, edit,
pick, and export:

1. **open polylines/paths** — including cap/join/dash and variable-width
   stroke semantics;
2. **closed contours** — stroke-only, fill-only, or both;
3. **holes** — multiple contours with an explicit nonzero/even-odd fill
   rule and orientation/role semantics.

These are not three renderer implementations. They are one
material-shape envelope served by live/settle roads per regime. The
corpus-reopened branching-vector-network question is adjacent but
separate: D2 must not accidentally preclude it, and Sid's envelope word
decides whether it joins the day-one contract.

### 6.2 Decision — RULED 2026-08-02 (Sid): **A**

**"Decision 2: yes A"** (verbatim in `vision/LOG.md`, fourth 08-02
entry). For freehand ink, centerline + pressure is authoritative
material; the outline is a deterministic, versioned derivation — it
may be retained as a cache or receipt, never a second coequal truth.
The forbidden non-answer below stays law. The question as it was put,
with both options preserved as the decision record:

For freehand ink, which fact is authoritative material?

**A. Centerline + pressure is truth; outline is derived.**

- Preserves the gesture, timing/pressure samples, cap/join semantics,
  and agent/human ability to re-thicken or restyle later.
- Makes open strokes native and aligns with `material → process →
  artifact`.
- Requires deterministic outline derivation, smoothing/resampling law,
  pressure normalization, self-intersection handling, and stable
  versioning of the derivation algorithm.
- Pick can use centerline distance + width profile in the hand regime
  and derived outline after settle, but both must satisfy the geometry
  truth contract and parity receipts.

**B. Outline polygon is truth; centerline/pressure is transient
provenance.**

- Makes the visible boundary directly durable and gives fill/pick/export
  an immediate closed-contour truth.
- Can preserve exactly what was seen at settle even if stroke expansion
  algorithms later change.
- Loses lawful re-thickening and gesture-level editing unless a second
  non-authoritative trail is retained.
- Does not replace the general open-path contract: lines/connectors and
  non-filled strokes still require open centerline/path material.

**Forbidden non-answer:** two coequal authoritative bodies that can
diverge. Either choice may retain the other as provenance or a derived,
versioned cache. One is material truth; the other must be reconstructible
or explicitly lossy.

### 6.3 Common shape-material fields independent of Sid's choice

- stable material ID, revision, provenance, author, and draft/settle
  lifecycle;
- local coordinate space plus affine instance transform;
- ordered subpaths/contours and explicit open/closed role;
- segment grammar at least line + quadratic/cubic curve, with a lawful
  extension point;
- fill rule and contour/hole semantics;
- stroke paint: width or width profile, cap, join, miter limit, dash;
- fill/stroke paints and opacity/blend references;
- source bounds and normalization transform;
- derived render caches tagged by source revision, algorithm version,
  tolerance/LOD, normalization, backend, and regime;
- pick contract relating visual AA, mathematical interior/centerline,
  and hit slop;
- export projection policy and any intentional loss.

### 6.4 D2 falsifier set

The round should reason through all cases before Sid's call; the later
machine roads must replay them:

1. a fast pressure-varying open stroke with round caps;
2. an acute miter and a bevel/round join at the same centerline;
3. a closed self-intersecting loop under both fill rules;
4. a compound closed shape with one hole and nested transforms;
5. erasing/splitting the middle of an open stroke;
6. re-thickening and restyling after settle;
7. boolean use of a settled ink artifact;
8. agent edit of one control point without private reconstruction;
9. crop/mask use and SVG/PDF projection;
10. boundary pick pixels across default `[0.1,8]` and legal
    `[0.01,1000]`, extent/normalization/backend tagged;
11. the same identity moving hand → settle → scene without a visual or
    pick discontinuity;
12. a future branch/junction case, so the chosen representation does
    not silently make the reopened vector-network decision.

### 6.5 Questions for the D2 round

1. What must another mind be able to re-enter and edit: the gesture,
   the settled silhouette, or both with one explicitly derived?
2. Is `re-thicken this stroke` a lawful semantic edit or a new stroke?
3. Which fact should survive a change in outline-generation algorithm?
4. Which representation makes erase, split, join, boolean, and export
   least privately reconstructive?
5. What is the authoritative pick interior during hand and settle, and
   how is parity proved at the boundary?
6. What provenance is needed to explain a settled outline without
   retaining a second authority?
7. Does icon/illustration practice ratify branching networks into the
   same material family, or a related family sharing segments/paints?

## 7. W0-C handoff

This lane delivers, without settling Sid's decisions:

- a source-grounded chrome walkthrough ledger and prioritized provider
  demands;
- a tldraw-source and Figma/Blender-practice corpus sweep;
- one paste-ready, explicitly nonbinding envelope paragraph carrying
  the default-vs-legal zoom rider and Blender scope confirmation;
- a data+loop frame-graph/scheduler contract whose families register
  instead of editing the central frame body;
- a D2 round packet preserving open paths, closed contours, and holes
  while reserving centerline+pressure vs outline truth to Sid.

**Reserved decisions — status after the 2026-08-02 sitting:** envelope
paragraph RATIFIED (§4, with the campaign-scoped-exclusion rider) ·
D2 ink-material truth RULED — choice A (§6.2) · studio capability
custody REMAINS Sid's. The corpus reopeners are resolved by the
ratification: branching vector networks, glass/noise/texture, and
moving sampled media are IN Figma-core.

Nothing in this document authorizes a capability cut, code commit,
server/product surface, or change to foreign Studio/Playground custody.
