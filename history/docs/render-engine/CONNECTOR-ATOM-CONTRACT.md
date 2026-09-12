# CONNECTOR ATOM — one-pass contract (reference/connector)

Cut 2026-08-06 by Fable under the one-pass law (`.claude/skills/work-package/SKILL.md`;
decisions.md "How we work"). Two Sid-touches: this cut · the accept. The cut closes
through ONE bounded fresh-eyes falsification round, run as the armed A/B
(identical brief pasted into a fresh Claude session AND a Codex session, both at
Sid's hand; pooled findings, evidence-cited, no verdict authority, no recut;
author repairs in-session; the comparison datum — role-vs-model — lands in the
baton). The round RAN 2026-08-06: 6 + 10 findings pooled into nine defect
groups, all repaired in this document the same day; Sid's ruling: no further
rounds — the next touch is the implementation prompt. Do not imitate IMAGE-ATOM/SEAM-STEP1 docs;
they predate the law. The path atom (accepted 2026-08-06) is the structural
precedent AT THE SOURCE LEVEL.

## Scope — what this atom is

Package 2 wave 3 (ENGINE.md §0 slice line: "durable reference edges on the
relation kernel (D-23's costing) + angled segments + labels (post-T1) +
routing/arrowheads/attachment"). One new tape family `:render.family/connector`
whose material is the REFERENCE EDGE — "the arrow that survives movement is an
edge with identity between identities" (ENGINE.md §10) — and whose picture is a
deterministic, versioned derivation from that edge plus the CURRENT resolved
geometry of its endpoints.

**The two-truths law (this atom's novel obligation).** Every prior family owns
self-contained geometry; the connector does not.

- **Material (authoritative):** edge identity · kind · directed FROM→TO endpoint
  bindings · route policy + explicit waypoints · arrowhead spec per end ·
  optional label · paint · status · provenance (`{:actor-id :asserter-type}`
  mirroring the row's real shape — decisions.md: `asserted-by` rides every
  relation; the map must not lie).
  Binding is document-model truth (W0-C CH-06: "durable reference edge owns
  binding; renderer owns preview/feedback"). The durable form IS the relation
  kernel's `RelationEdgeRow` (`src/app/server/rama/relation_kernel.clj:267-273`)
  — and that row carries relation identity/kind/endpoints/asserter/status ONLY:
  no route, no heads, no label, no paint, and NO revision field. The client
  material is therefore TWO declared layers merged into one: the row PROJECTION
  (identity · kind · direction · provenance · status) + session DRESS (route
  policy/waypoints/heads/label/paint — D2's client-session home), validated as
  ONE merged material by the fail-closed grammar. Its cache revision is
  composite: `[row-stamp dress-revision]` — row-stamp = the row's event/read
  identity as read at the join (boot-static day-one, so read-time truth);
  dress-revision = path's `revisioned-edit` content revision. Semantic edits
  target the dress layer only day-one (D2). Directionality is semantic (from = carrier, to = judged thing) — the
  arrowhead is a projection of durable truth, never a per-connector style bit.
- **Derived (never truth):** the resolved route polyline, stroke + arrowhead
  meshes, label layout/placement, bounds. The route derivation carries the render
  seam's five declarations (decisions.md "The render seam"): keyed inputs =
  (material composite revision · resolved endpoint tuple · waypoints ·
  algorithm-version · regime · label `provider-identity` when a label exists);
  doors = store-change events for material/edge-set changes AND, for endpoint
  geometry, a per-entry VALUE-DIFF of the effective-transform map at the frame
  edge (`identical?`/`=` per container — the established consumer-edge shape:
  `set-transform!` mutates the registry with NO events and the consumer
  uploads only-when-changed, `scene_runtime.cljs:282-288`;
  `reconcile-slot-text-geos!` token compare) — never a clock: an unchanged map
  does ZERO derivations, and dirty containers route to exactly their bound
  edges through a `bound-edges-by-container` index rebuilt only when the
  edge-instance set changes, so per-frame work is O(changed containers +
  affected edges), never the population (receipt: the `:route-resolutions`
  counter); ownership = the lane's cache state, scoped to the store frame's
  edge-instance set (a set change resets it); projections = paint forward /
  pick reverse / census; oracle = full re-resolve (the batch pass IS v1 — its
  incremental sibling is the LATER road, and this batch stays as that
  sibling's oracle per the growth law).

**Bindings (attachment made real).**
- `{:bind :node, :target <address>, :anchor :boundary}` — aim at the target's
  bounds center, clip at its bounds boundary (gap 0.0, touch — the trail-face
  gate-7 precedent; `:straight` clips along the center↔center line, `:elbow`
  along its terminal axis-aligned segment so heads arrive perpendicular to the
  faced side), arrowhead length shortens the stroke so the tip sits at the
  anchor point. `{:bind :point, :position [x y]}` — free world endpoint.
- Cross-container binding is IN (ENGINE.md §10 names it real work) — and for
  real blocks it is the NORM, not the edge case: every product block rides its
  OWN container (`ground.cljs:94-97`), and a drag writes that container's
  transform, never the tree (`:placement/drag-group`, `ground.cljs:3575-3593`).
  **Anchor space (op-container == slot-container, ALWAYS):** an edge's ops and
  rt-tree nodes live in the LANE'S OWN container — fixture edges in their
  authoring fixture container; durable-join edges in the join's dedicated
  identity-transform container — never stamped into a foreign container
  (`pick` inverse-transforms by the SLOT's container,
  `scene_store.cljc:355-359`; a per-edge foreign stamp would split paint and
  pick onto two transform roads). Both endpoints resolve through the W2-A
  effective-transform chain into that anchor space — never a second transform
  path; under the identity join container that space equals world, lawful f32
  at day-one canvas magnitudes (the f32 road note path banked), and the
  per-edge anchor-container refinement is a named LATER road (the same road as
  per-edge pick-bbox tightening). Both endpoint containers' effective
  transforms are declared derivation inputs: a container/block move re-routes
  exactly the edges bound to it.
- **Occurrence expansion.** An address maps to a SET of view-instances
  (`scene_store` `:index`, address → #{vi}); a durable edge renders as
  edge-INSTANCES over the visible (from-vi × to-vi) pairs — identity
  everywhere (cache, census, receipts, pick) is `[relation-id from-vi to-vi]`;
  multiple occurrences draw every pair honestly (bundling/thinning stays the
  routed LATER refusal).
- A well-formed binding whose target is absent from the current scene projection
  is a DECLARED runtime state: the edge paints nothing, is counted in the census,
  and never crashes or paints stale (the trail-face trap-3 law: edges with
  missing endpoints are bundle data, not floating geometry). Malformed bindings
  refuse at admission (fail-closed grammar).
- Mixed-camera bindings (a `:world` node bound to a `:screen` node) are OUT
  day-one: census + skip, routed LATER.

**Routing (angled segments).** Day-one policies, both versioned under one
`algorithm-version`:
- `:straight` — one segment anchor→anchor;
- `:elbow` — pinned `:elbow/v1`: dominant axis = the larger |Δ| between the two
  UNCLIPPED bounds centers (tie → horizontal); the route is the 3-segment Z at
  the dominant-axis midpoint — dominant = x: A → (mx, Ay) → (mx, By) → B with
  mx = (Ax+Bx)/2; dominant = y is the transpose; terminal segments are
  axis-aligned (boundary clip runs along them); zero Δ on the non-dominant
  axis degrades to `:straight`. The trail-face one-bend L-route
  (`trail_face/lanes.cljc:109-123`: from-cx spine → to-cy elbow) is PRIOR ART
  NOT ADOPTED — different regime (lane chrome), and W1 puts route semantics in
  the pinned algorithm, never in caller choice;
- explicit `:waypoints [[x y] ...]` — honored verbatim between the resolved
  anchors (arbitrary-angle polylines; this is how "angled segments" are authored
  day-one).
Degenerate cases are declared, never NaN: coincident anchors / zero-length route
→ skip + census `:degenerate`; consecutive duplicate waypoints dedupe; an elbow
whose dx or dy is zero degrades to straight segments.

**Arrowheads.** Per end: `:none | :triangle` (filled). Size = k × stroke width
(k a recorded default). Head geometry folds into the same mesh derivation; the
route shortens by head length. Default projection of a durable edge: `:triangle`
at `:to`, `:none` at `:from`.

**Labels (post-T1 — the hard join satisfied: T1 landed `52d123f`).**
- Optional single-line label `{:text s, :at t, :offset [dx dy]}`, placed at
  parameter `t` along the resolved route plus a perpendicular offset (default
  lifts it off the stroke; placement is part of the versioned route derivation).
- Layout goes through THE ONE SEAM: `connector-route` calls
  `app.client.workspace.text-layout/layout` (Contract T; `text_layout.cljc:966`)
  — a new lawful PROVIDER-CALLING owner; the layout call carries the LIVE
  provider from `font-assets` (`layout` dispatches shaped-vs-legacy on
  provider presence, `text_layout.cljc:966-972` — a provider-less call would
  silently take the T0 legacy road), and `provider-identity` joins the
  derivation key (a font/provider swap re-keys the label). The T1
  independent-metrics fence must stay green with it — GREEN BY AUDIT, not by
  omission: the fence's hardcoded owners array
  (`verify_text_layout_fence.mjs:7-27`) GAINS the connector's layout-calling
  owner (`connector_route.cljc` `layout-label`, required token `tl/layout`),
  so the fence actually audits the new owner (no private `count × advance`,
  ever).
- Paint: connector labels ride ONE dedicated label text geo for the lane,
  CLONED off the content text system (`editor/clone-text-system` — the
  slot-text road, `runtime/render.cljs:84`: shared
  pipeline/bind-group/camera/font, NOT a second text path, T12), reshaped at
  the prepare site via `editor/update-text-data` only when the packed label
  set changes (token compare — `reconcile-slot-text-geos!` shape,
  `runtime/render.cljs:51-99`), recloned on font/backend change, destroyed
  with the system. `produce` mints the label's tape entry via ONE thin
  renderer helper calling `system-entry` with family = `text-system-family`
  of the live geo (`renderer.cljs:2845-2847` — NEVER a hardcoded family id;
  Slug-lawful) and the connector's order token (Contract O §2: a family may
  emit multiple ordered scene entries under one citizenship record). Label
  glyphs are byte-identical to the same string rendered by the slot-text lane
  at the same font params — receipt, not hope.
- Pick: label entries carry the edge identity + `:part :label` at the TAPE level
  (pick-reverse receipts). Product-surface label picking and label editing route
  to the label expansion, LATER.
- Stroke/label z: within one slot the label (text lane) paints before the
  connector mesh — declared; the default offset makes overlap a non-case day-one;
  ordering control rides the label expansion.

**Paint + provenance.** Solid color + opacity + world-unit width; straight
material color → tagged linear-premultiplied seam exactly like path (Contract C;
the shader joins `configure-*-color-shader`; default-OFF respected). Coverage is
declared ALIASED v1 (`:aliased-v1` — the path atom's honest Contract-G
declaration; same AA/overlap refusals, same routes). The day-one projection of
durable edges paints kind → color through a declared vocabulary (seed: the
trail-face palette, `trail_face/scene.cljc:173-190`) and provenance → a declared
distinct tint keyed by `:asserter-type` through the circulation classifier's
sets (machine = `#{:llm :agent :import :machine}`, human = `:human` —
`material_circulation.clj` `silver-edge?`/`gold-edge?` `:83-93`); `:actor-id`
rides the material for identity (the real row shape: `:asserter-actor-id "sid"`
+ `:asserter-type :human`, `relation_kernel_test.clj:258-268`) — machine output
visibly distinct from Sid's hand, always (decisions.md). Richer provenance
styling (dash etc.) rides the paint-axis expansion with path's refusal.

**Tessellation reuse.** The resolved route is a polyline; stroke expansion and
fill triangulation REUSE `path_tessellation.cljc` as a library (segment quads +
regime round join/cap fans; arrowheads are small convex fills). No fork of
tessellation math. Zoom regimes: import the path regime table
(`path_material.cljc:14-44`) verbatim — one table, total partition of
[0.01, 1000] (Q2). Vertices are anchor-container-local f32 (durable-join
edges: the identity join container, = world — lawful at day-one magnitudes);
in-shader container transforms carry camera motion. The ensemble free ride is
SCOPED: an edge whose BOTH endpoints ride its own anchor container
(fixture-authored edges) never re-tessellates under that container's move;
durable-join edges re-derive on ANY endpoint-container change — that IS S5's
live re-route, kept proportional by the transform value-diff + the
`bound-edges-by-container` index.

**The mesh cache is bounded (deviation from path, with cause).** Path's
derive-mesh-set cache grows per content key — lawful there because path content
changes are rare edits. Connector routes change CONTINUOUSLY under drag; an
unbounded key-grown cache is a drag-storm leak. The connector cache is
one-current-entry-per-edge-INSTANCE: keyed by the full derivation key, checked
for hit, REPLACED on miss — and it LIVES IN `connector_route.cljc` (.cljc,
JVM-tested; path's `derive-mesh-set` precedent, `path_tessellation.cljc:425`),
state-in/state-out, with BOTH counters (`:route-resolutions`
`:mesh-derivations`) in the cache state. `connector_gpu.cljs` holds upload
custody only (mesh-set identity gate, path's two-level shape) and mirrors the
counters into the GPU receipt. Receipt: a two-edge scene where one endpoint
moves N times holds cache size 2 while re-deriving only the moved edge's mesh
(`:mesh-derivations` == N for it, 0 for the sibling; `:route-resolutions`
likewise; a static frame adds 0 to both).

**The durable read join (Sid's slice ruling: "the slice carries durable
reference edges on the relation kernel").**
- READ-ONLY, through the kernel's ONE lawful product read: query R1
  `relations-for-targets` (`relation_kernel.clj:779-807`, wrapper
  `read-relations-for-targets` `:947-956` — batch-first by design: "consumers
  resolve a whole visible DAG region in one roundtrip"). Never a direct
  foreign-select of kernel PStates (the kernel's own §7 law).
- No kind is minted (the registry already carries `:references :pairs-with
  :based-on :confirms :refutes :supersedes` …, `relation_kernel.clj:58-83`); no
  depot append, no new PState, no module change, no deploy — D-23's "atom-free
  but not free" priced as pure wiring. The rama skill's build ladder is therefore
  NOT dragged into this atom; the kernel is consumed at its public seam.
- Behind the SAME `?live-atoms=1` flag (one flag lights Package-2 atoms live):
  at join boot, read asserted edges (R1 default visibility) whose BOTH endpoints
  resolve to on-canvas nodes; project RelationEdgeRow → connector material
  (kind→color vocabulary · from→to arrowhead · provenance tint · no label);
  inject through the REAL artery (rt-nodes → store → tape → registered
  pipelines) exactly as live_atoms does. The edge SET is boot-static day-one;
  endpoint GEOMETRY is live — drag a real block (the existing first-light
  gesture) and its edges follow, re-deriving only the affected routes.
- relation-kernel-module is one of the five DEPLOYED land modules
  (`bin/land:27`) — the join reads the real cluster. If the visible corpus
  yields zero resolvable edges, the fixture lane below still carries the felt
  receipt; the durable chain is additionally proven headlessly by a JVM tripwire
  over the kernel's IPC runtime (`start-relation-runtime!` — assert → R1 read →
  project → route → validate, no cluster needed).
- Address↔target-key: canvas block nodes are stamped `:address` = the unit-id
  (`ground.cljs:1751-1760`), and the kernel's `:derived-unit` route keys targets
  by unit-id VERBATIM (`->target-ref`'s `:else` branch,
  `relation_kernel.clj:856-864`; the pattern named at `code_atoms.clj:49-53`) —
  `material_circulation.clj:226-231` already mints `:references` /
  `:instance-of` / `:felt-at` between derived units exactly this way. So the
  join passes on-canvas addresses verbatim as R1 target-keys — no mapping
  helper exists today and NONE is built. Day-one kinds vocabulary (declared
  data, widenable): `:references`; `:instance-of`/`:felt-at` ride the same
  route when the vocabulary widens. `:pairs-with` is message-keyed (its
  target-key collapses to the conversation `chat:<sha>`,
  `machine_cut.clj:413-433`) and routes LATER with the message→unit join.
  R1 returns each edge under BOTH endpoints' target groups — the projection
  dedupes by `:relation-id`, THEN expands visible (from-vi × to-vi) occurrence
  pairs into edge-instances (D6). The join's face instance registers in a
  dedicated identity-transform container (the anchor-space ruling above).
- The Electric seam mirrors the TrailBundle chain exactly (the one existing
  durable-edges-to-client join): a plain server read fn in `live_edges.cljc`
  over the product `:rk-rt` handle (`cluster.clj:137-170` builds it;
  `face-ctx`, `file_viewer.cljc:453-461`, carries it) — R1 read + a
  `project-edge`-shaped projection (`trail_view.clj:387-403` precedent) · ONE
  `e/defn` thin hook in `file_viewer.cljc` beside `TrailBundle` (`:238-242`) ·
  one flag-gated pull + injection line in `electric_flow.cljc`. One-shot read
  at join mount (the boot-static default); no PState proxy — the
  foreign-proxy-async quarantine stands (`util_fns.cljc:96-112`).
- Flag absent → byte-identical product: no kernel query is issued at all, no
  systems constructed, `live_edges` loads pure (dark-lane laws).

**The fixture lane** (extends `live_atoms.cljs`'s corpus): (a) straight
arrow + label between two shapes; (b) elbow with an explicit waypoint and heads
both ends, crossing over image/path atoms; (c) two exactly-overlapping edges,
same (from,to), different asserters in the REAL row shape
(`{:actor-id "sid" :asserter-type :human}` vs
`{:actor-id "llm:…" :asserter-type :llm}`), provenance-distinct paint;
(d) one cross-container edge to a node in a second fixture container; (e) one
edge bound to a nonexistent address (census receipt, paints nothing). Fixtures
are client-session values ONLY — never written to the kernel.

## Ruled at cut — defaults with Sid veto slots (one line reverses any)

1. **Durable custody: READ-ONLY.** The atom reads kernel edges over R1; it never
   writes the kernel, never mints kinds, never touches Rama code. Edge AUTHORING
   (and any durable write) stays behind the authoring-artery/studio-custody
   ruling reserved to Sid.
2. **Dress is client-session day-one.** Route/waypoints/heads/label overrides on
   durable edges have no durable home yet; the projection defaults cover them.
   The durable dress home routes to the authoring slice (its grammar is frozen
   here so it lands without re-cutting).
3. **Join projection defaults:** asserted-only (retracted edges not rendered) ·
   boot-static edge set · same `?live-atoms=1` flag · arrow at `:to` ·
   kind→color vocabulary · provenance tint · no labels on durable edges ·
   day-one kinds = `:references` via the `:derived-unit` verbatim-key route
   (canvas addresses ARE the target-keys; message-keyed `:pairs-with` → LATER).
4. **Same-pair overlap paints honestly** (exact overlap, reverse-tape pick
   decides; the census counts overlap groups). Bundling/parallel offsets: LATER.
5. **Cross-container binding IN; mixed-camera binding OUT** (census + skip).
6. **Multi-occurrence expansion: all visible pairs.** An edge whose endpoint
   addresses have several on-canvas occurrences renders one edge-instance per
   visible (from-vi × to-vi) pair, identity `[relation-id from-vi to-vi]`;
   bundling/thinning stays the routed LATER refusal.

## Refusals — deliberately not in this atom (each routed, never a void)

- Curved routes (bezier/arc) → vector-network/curve slice, Package 2 (also
  trail-face INV-17's standing fence).
- Obstacle-avoiding auto-routing → routing expansion, at population pressure.
- Precise/fractional anchors · per-side pinning · snap points → attachment +
  chrome expansion (W0-C CH-06's chrome column is explicitly NOT this atom:
  renderer owns preview/feedback there, and none of it is day-one).
- Arrowheads beyond none/triangle → marker expansion.
- Label pill/background · multi-line/wrapped labels · product-surface label
  pick/edit → label expansion + studio custody.
- Dash · gradient · screen-constant width · provenance styling beyond the
  declared tint → paint-axis expansion (path's same refusal).
- Retracted-edge rendering (struck/dim — trail-face precedent banked) → LATER,
  with the trail-face migration.
- Live edge-set subscription (reactive edge updates while flagged on) → the
  Electric-native seam roadmap; boot-static day-one.
- Message-level `:pairs-with` rendering (edge target-ids are chat-message ids;
  target-key collapses to the conversation — needs the message→unit join) →
  LATER.
- Edge bundling / parallel-edge offsets for same-pair multiplicity → LATER.
- Connector CHROME (endpoint handles, target outlines, snap circles,
  intended-vs-bound stubs) → selection/manipulation wave.
- Authoring gestures (draw/reconnect) + durable dress writes + new relation
  kinds → authoring artery + studio custody, reserved to Sid.
- trail-face's thin-rect Manhattan connectors keep working untouched; their
  migration to this family → a natural touchpoint, LATER.
- Analytic AA / exact translucent self-overlap → the same LATER roads the path
  atom pinned (Sid's lived ruling stands).

## Laws — pointers, not restatements (each with its operationalizing scenario)

- `W1.md` Contract O — one ordered tape, paint forward/pick reverse, no family
  tie-breaks (z is sibling-rank/part-rank DATA; "connectors always under nodes"
  as a family rule would be unlawful — the join sets ranks as data). → S4.
- `W1.md` Contract G — unconditional geometry: authority = the reference-edge
  route inputs; tri-state CPU classification IS the truth (stroke =
  distance-to-route ≤ half-width, ε-boundary like path; heads = point-in-
  triangle); GPU coverage its parity-probed projection; byte-128 tie law;
  hit-slop declared (0.0 screen-px day-one, path's constant; product slop UX →
  chrome slice). → S3.
- `W1.md` Contract M — citizenship: fail-closed grammar, versioned semantic
  edits, provenance, census (`assert-corpus-coverage!` mirror). → S1.
- `W1.md` Contract T — the label consumes ONE layout result; the T1 fence stays
  green with the new provider-calling owner. → S4.
- `W1.md` Contract C — straight → tagged linear-premultiplied (default-OFF) →
  present; the connector shader joins `configure-*-color-shader` like path;
  translucent-on-non-black CPU reference receipt. → S4.
- ENGINE.md §0 gate sentence — the family REGISTERS through the W2-B seam
  (`scene-tape/family-ids` + `registration` + `family-contracts` +
  `frame-family-registry` `{:contract :produce :execute!}`); zero central
  branches; the seeded-branch fence self-test stays green; ALSO: add
  `:render.family/path` AND `:render.family/connector` to
  `verify_scene_tape_fence.mjs`'s families array (the path atom's omission,
  repaired here — the fence checks presence, so this only strengthens). → S4.
- decisions.md "The render seam" — the route derivation's five declarations
  (inputs/doors/ownership/projections/oracle); no execution clock as ancestor
  (the transform door is a value-diff at the frame edge — the consumer-edge
  precedent, zero work on an unchanged map); effects at mutation sites and the
  frame edge only; re-derivation proportional to the affected edge set through
  the `bound-edges-by-container` index. → S1, S5.
- decisions.md "Relations are typed edges" + "The map must not lie" — the kernel
  is the ONLY durable home; `asserter-type` rides the material; machine-asserted
  edges paint visibly distinct. → S5 (+ fixture (c)).
- decisions.md settled architecture — store coords f64 world, GPU buffers
  container-relative f32; shape-local normalization at admission; W2-A effective
  transforms are the ONLY transform path. → S2, S3.
- Dark-when-off (decisions.md dark-lane laws) — `live_edges` loads pure, routes
  nowhere and queries nothing without the flag; zoom envelope legal [0.01,1000],
  verifier stations 0.01/0.1/1/8/10/100/1000. → S4, S5.

## Exact entry points

New namespaces (ALL new code lives here):
- `src/app/client/substrate/connector_material.cljc` — grammar + fail-closed
  validation over the MERGED material (row projection + session dress:
  bindings/route/heads/label/paint/status/provenance
  `{:actor-id :asserter-type}`) · versioned semantic edits on the dress layer
  (`set-binding` `set-route` `set-waypoints` `set-heads` `set-label`
  `set-paint`, path's `revisioned-edit` shape) · Contract-G declaration data
  (authority `:reference-edge-route`; regimes imported from
  `path-material/legal-zoom-regimes`) · CPU hit truth (`classify` / `hit?`:
  route distance + head point-in-triangle, tri-state, `boundary-epsilon`) ·
  the composite-revision cache-key law (`[row-stamp dress-revision]` +
  resolved-anchor + label `provider-identity` extensions) · kind→color +
  provenance-tint vocabulary (data; tint keys off `:asserter-type` via the
  circulation classifier sets) · census (`assert-corpus-coverage!` mirror,
  keyed by edge-instance `[relation-id from-vi to-vi]`:
  resolved/unresolved/degenerate/mixed-camera/overlap-groups).
- `src/app/client/substrate/connector_route.cljc` — binding resolution against
  the store frame's target index + effective transforms · `:straight`/
  `:elbow/v1`/waypoint routing (one `algorithm-version`) · arrowhead geometry +
  stroke shortening · `layout-label` — the ONE named layout-calling owner
  (calls `text-layout/layout` with the live provider; the T1 fence audits this
  fn by name) + placement · the bounded one-entry-per-edge-instance cache with
  `:route-resolutions`/`:mesh-derivations` counters (state-in/state-out,
  JVM-tested) · `bound-edges-by-container` index + the per-frame transform
  value-diff · the LIVE per-edge-instance route cache the pick predicate
  consults (lazily re-resolved when its cached anchor tuple is stale vs
  current effective transforms) · the RelationEdgeRow→material projection
  (pure; used by the join and the JVM durable-chain tripwire).
- `src/app/client/substrate/webgpu/connector_gpu.cljs` — `init-connector-system`
  / `prepare-connector-frame!` (upload custody ONLY — the cache + counters live
  in `connector_route.cljc`; upload gated on mesh-set identity, path's
  two-level shape; prepare takes effective transforms + `font-assets` + the
  content text system) · the label text geo lifecycle (clone off the content
  system / `update-text-data` on label-set token change / reclone on
  font/backend change / destroy) / `connector-entries` (mesh entry + label
  text entry; part-rank 5) / `execute-connector-batch!` /
  `destroy-connector-system!` · same camera/containers buffers · gpu-budget
  registration · reuses `path-tessellation` + `path-material` packing/stride as
  libraries (zero edits to path files).
- `src/app/client/workspace/live_edges.cljc` — the flag-gated durable join:
  the server read fn (takes `:rk-rt`, calls `read-relations-for-targets` with
  on-canvas addresses verbatim + the kinds vocabulary + asserted-only, projects
  rows `project-edge`-shaped, dedupes by `:relation-id`) · the client
  projection RelationEdgeRow→connector-material + D6 occurrence expansion ·
  injection via `scene-runtime/register-face-instance!` into the join's
  dedicated identity-transform container · receipt on
  `globalThis.__softlandLiveEdgesReceipt` {edges-read, rendered, unresolved,
  source, zero-writes}. Its `e/defn` boundary rides `file_viewer.cljc` (below).

Thin hooks only (few lines each):
- `scene_tape.cljc` — `:render.family/connector` in `family-ids` (:11-20) ·
  `connector-registration` via `registration` with
  `[:grammar :entry-paint-required-keys] [:vertex-count]` (the path form,
  :396-415) · entry in `family-contracts` (:417) · require.
- `renderer.cljs` — the path atom's exact 4-hook shape (~21 lines): require ·
  `frame-family-registry` entry (:2948-2978) · `draw-frame!` kwarg + one
  `prepare-connector-frame!` call beside path's (:3142-3145) · frame-map key
  (:3164-3167) · PLUS one small public helper minting the label's tape entry
  via `system-entry` with family = `text-system-family` of the live geo — the
  label's paint door; family NEVER hardcoded; keeps ONE text road.
- `rect_tree.cljc` — `:render.family/connector` entry in `family-hit-predicates`
  (:480-494 registry + dispatch). The predicate reads the node's edge-instance
  id and classifies against `connector-route`'s LIVE route cache — signature
  `(fn [node local-point])` UNCHANGED (the registry is namespace code, not
  store data; G2 slot serializability untouched). Connector rt-nodes register
  at bounds origin (0,0) SPANNING the container, so the bbox-local point the
  walk hands the predicate (`rect_tree.cljc:515-516`) IS
  anchor-container-local and compares directly with route vertices; broad
  phase stays the container gate day-one — bounds never falsely reject a
  live-derived route (the tree is store data and CANNOT follow per-frame
  transform-driven geometry; per-edge bbox tightening + its refresh door =
  the named LATER road) · `tree->connectors` walk beside `tree->paths`.
- `scene_store.cljc` — `:connectors` lane beside `:paths` (flatten / stamp /
  `derive-store-frame` / `ops-count-by-vi`; ops stamp the SLOT's own
  container-idx, the uniform lane shape — the anchor-space ruling makes any
  per-op foreign stamp unlawful) · a `targets-by-address` frame index
  (address → occurrences `[{vi, bounds, container, container-idx}]` — the vi
  tag feeds D6's occurrence expansion): generalize the existing PRIVATE
  `addressed-rects` walk (`scene_store.cljc:456-474` — it already computes
  absolute container-local rects keyed by `:address`) run per slot, rather
  than minting a second walk; its incremental sibling is a named LATER road,
  this batch walk is that sibling's future oracle.
- `runtime/render.cljs` — pass `:connector-system` (+ effective transforms,
  `font-assets`, and the content text system to the prepare site) at the one
  `draw-frame!` call site (:748-749 shape).
- `live_atoms.cljs` — construct the connector system beside image/path in
  `augment-pipelines!` · fixture corpus (a)–(e).
- `file_viewer.cljc` — ONE `e/defn` (the join's server boundary, beside
  `TrailBundle` `:238-242`, reading `(face-ctx)`'s `:rk-rt` `:453-461`).
- `electric_flow.cljc` — one construction/mount line for the join (beside the
  live-atoms line, :743) + the flag-gated pull line (the trail pull-loop shape,
  `:665-681`).
- `test/app/test_runner.clj` — register `connector-material-test` +
  `connector-route-test` in `pure-namespaces` (:31-42) and
  `connector-join-test` (the durable-chain IPC tripwire) in
  `isolation-exceptions` (:73-156, beside `relation-kernel-test` :149-150) —
  an IPC boot NEVER rides the pure/fast lane.
- Verifier lane (append-only, the path shape): `webgpu/verifier.cljs` connector
  golden/parity/arrangement/dark-lane/upload blocks ·
  `test/render_engine/run_verifier.mjs` connector rows + inputs +
  `--append-connector-goldens` / `--append-connector-input-amendment` /
  `--assert-connector-contract` (scoped; `--update-goldens` stays dead) ·
  `manifest.json` gains `connectorAtomInputs`/`connectorAtomCases` ·
  `verify_scene_tape_fence.mjs` families array gains path + connector ·
  `verify_text_layout_fence.mjs` owners array gains the connector layout owner
  (`connector_route.cljc` `layout-label`, token `tl/layout`; append-only,
  self-tests intact).
  KNOWN DRIFT to respect, not inherit: `run_verifier.mjs` computes 11
  path-input fingerprint keys, the banked manifest holds 9 — the connector's
  input set must match what the script actually computes.

## Decisive scenarios (frozen as tripwires + goldens at close)

1. **Grammar, determinism, proportionality** [JVM]: malformed
   binding/kind/route/label refuses by name; identical (material composite
   revision, resolved-anchor tuple, algorithm-version, regime,
   provider-identity) → byte-identical route + mesh; a version OR provider
   bump changes the key, never silently the bytes; moving ONE endpoint
   (synthetic transform-map steps through the value-diff door) re-derives ONLY
   its bound edges — `:route-resolutions` AND `:mesh-derivations` == the
   moved-edge count, 0 for siblings, 0 across static frames (sibling meshes
   returned by identity) — and the bounded cache holds one entry per
   edge-instance through an N-step drag; census counts edge-instances (an
   address with two occurrences → two instances, D6).
2. **Attachment + routing truth** [JVM]: boundary-intersection anchoring across
   the four relative quadrants for `:straight` and `:elbow/v1` (exact Z-route
   vertices — dominant axis, midpoint, terminal-side clip — asserted per
   quadrant); explicit waypoints honored verbatim; arrowhead shortening puts the tip at the anchor;
   cross-container resolution through effective transforms (a container move
   re-routes exactly its crossing edges); degenerate cases (coincident anchors,
   duplicate waypoints, zero dx/dy elbow) produce their declared outcomes;
   unresolved + mixed-camera bindings skip + census, never crash, never stale.
3. **Pick truth** [JVM + verifier parity]: CPU classification is THE truth —
   stroke distance ≤ half-width (ε-boundary), head point-in-triangle; GPU
   coverage agrees outside the declared boundary band across the seven zoom
   stations (path's parity harness shape); overlapping edges resolve by reverse
   tape order; every part returns the edge identity (+ part route at tape
   level); the registered predicate classifies against the LIVE route cache: a
   point off-stroke inside the route's bbox is a MISS (predicate truth, not
   bbox), and after an endpoint-container move pick HITS the new route and
   MISSES the old midpoint with NO tree write (container-spanning bounds +
   lazy re-resolve against current effective transforms).
4. **Tape citizenship + goldens** [verifier]: connector entries interleave with
   rect/text/image/path forward and reverse (arrangement gate); label glyphs
   byte-identical to the slot-text lane for the same string (ONE text road —
   the label geo is a clone of the content system, and the label entry's
   family is asserted = `text-system-family` of the live geo; T1 fence green
   WITH the connector owner present in its audited owners array; text-layout
   fallback counter clean for the connector surface);
   3 goldens appended — straight-arrow-label · elbow-waypoints-heads ·
   provenance-pair-overlap — spanning ≥3 zoom stations with one outside the
   floor-default band; determinism ×2; the Contract-C translucent-on-non-black
   CPU reference receipt (path's `run-color-receipts!` shape); ALL existing
   goldens byte-identical; the MSDF counterexample stays RED; the scene-tape
   fence (with path + connector added to its families array) self-tests green.
5. **The durable live join** [felt receipt, Sid's eyes + JVM]: `?live-atoms=1`
   projects the relation kernel's asserted edges among on-canvas blocks as
   connectors through the real artery — drag a block, its edges follow while
   unaffected edges' meshes are untouched (receipt counters); flag absent →
   byte-identical product and ZERO kernel queries; the receipt reports
   {edges-read, rendered, unresolved, zero-writes true}. The durable chain is
   also proven headless: a JVM tripwire in `connector_join_test.clj`
   (isolation-exceptions lane; the `with-relation-runtime` fixture,
   `relation_kernel_test.clj:58-67`, with its microbatch barrier discipline)
   asserts one `:references` edge between two `:derived-unit` refs whose
   target-ids are the fixture addresses, reads it back through
   `read-relations-for-targets` with those addresses verbatim, projects it,
   routes it, and validates the material — against the test's OWN ephemeral
   IPC runtime, created and torn down in-test; never the land, zero writes
   outside it.

## MUST-NOTs

Never read `src/app/server/env.clj` · NO durable/Rama writes against any
deployed/land cluster (no depot append, no kind addition, no
PState/module/deploy change; fixture edges are client-session values; the join
is read-only over R1; the ONE carve-out: `connector_join_test`'s own ephemeral
IPC runtime — created in-test, torn down in-test, never the land) ·
never edit existing goldens/manifest rows or the MSDF RED case — append only ·
no hand-positioned central draw/pick branch · no new JS/npm dependency · no
private text metrics (the label rides the one layout seam) · no second
transform path (W2-A effective transforms only) · `ground.cljs` untouched;
`renderer.cljs`/`electric_flow.cljc` thin hooks only; zero edits to path
namespaces (consume as libraries) · commits only at Sid's word — code and docs
separate, both on `docs/current-mental-model-local`, exact-path staged, never
push.

## Close

Scenarios 1–3 freeze as ~5–6 tripwires across the three JVM namespaces
(`connector_material_test.clj` + `connector_route_test.clj` in the pure lane;
`connector_join_test.clj` carries the durable-chain IPC tripwire in
`isolation-exceptions`); scenario 4's goldens + parity rows join
the permanent bank via the scoped append road; focused suite = the three
namespaces GREEN + `npm run verify:render-engine` whose ONLY red is the
preserved MSDF counterexample (W1's canonical exit — never "fixed" by this
atom). Foreign failures are board debt, never stops. One NOW entry (≤15 lines,
self-audit included) in `CONNECTOR-ATOM-NOW.md` + the board line flip.
Acceptance = Sid's word.

**The cut's falsification round — RAN 2026-08-06, closed.** Identical brief to
a fresh Claude session and a Codex session (both pasted by Sid); 6 + 10
findings pooled into nine defect groups, all repaired in this document the
same day: pick seam (live-cache predicate + container-spanning bounds) · label
road (cloned label geo + live family + fence owner) · anchor space
(op-container == slot-container; identity join container) · occurrence
expansion (D6) · composite material revision · transform-door proportionality
(value-diff + bound-edges index + `:route-resolutions`) · IPC tripwire lane +
MUST-NOT carve-out · provenance row shape · `:elbow/v1` pin. The
pre-registered expiry did NOT fire (≫1 decision-changing finding — the round
earns its keep); the role-vs-model datum lives in `CONNECTOR-ATOM-NOW.md`.
Sid's ruling: no further rounds — the next touch is the implementation prompt.

## Codex opening prompt (implementation — after the cut closes)

```
You are implementing the CONNECTOR ATOM for Softland's render engine — one
pass, whole atom, one session.

Read first, nothing else needed:
1. docs/render-engine/CONNECTOR-ATOM-CONTRACT.md  (this contract)
2. docs/render-engine/W1.md                       (contracts O/G/M/T/C your family satisfies)
3. src/app/server/rama/relation_kernel.clj        (the durable edge truth; READ-ONLY — consume
                                                   read-relations-for-targets, never foreign-select)
4. .claude/skills/work-package/SKILL.md           (the one-pass law: build, close, when to ask)

The path atom is your structural precedent AT THE SOURCE LEVEL ONLY —
path_material.cljc, path_tessellation.cljc (consume as a library, zero edits),
path_gpu.cljs, live_atoms.cljs, the path blocks in verifier.cljs and
run_verifier.mjs. Do NOT read PATH-ATOM docs beyond the contract if you need
scope comparison; never read IMAGE-ATOM-* docs (pre-law).

This contract has been through its bounded falsification round (16 pooled
findings from two fresh instruments, repaired 2026-08-06) — build what is
WRITTEN; the repairs are law, not suggestions.

Build the WHOLE atom straight through. Keep your own falsification pass and fix
what it surfaces in-session. Ambiguity → strongest default + a note in
docs/render-engine/CONNECTOR-ATOM-NOW.md. A genuine fork (two readings that
cannot both hold) is ONE question in that file — route around it and keep
building; never stop. Foreign test failures are board debt, never stops.

Close per SKILL.md: tripwires + goldens frozen from the contract's scenarios;
focused suite: the three JVM test namespaces GREEN (two pure +
connector_join_test in the isolation-exceptions lane), and
verify:render-engine red ONLY on the preserved MSDF counterexample (its
canonical state — do not fix it).
NOW entry ≤15 lines, board flip. Commits only at Sid's word. Acceptance is
Sid's word — you never wait on a review round.
```
