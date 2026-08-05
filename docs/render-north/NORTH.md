# Render North — the framework Softland is walking toward

**Status: DIRECTION, non-binding on build.** Written 2026-07-05 (Fable,
render-north study; commissioned by Sid under the emperor-designer →
engineering-head handoff frame). This document answers one question: *what is
the ideal UI framework, built on WebGPU (view) + Electric (reactive) over Rama
(truth), for Softland as one land at every zoom — collaborative, eventually 3D,
with UIs made on the fly by humans and agents, legible to agents in minimal
tokens?* It rests entirely on `INPUTS.md` (same directory); every external
claim there was web-verified this session. Nothing here authorizes code:
D-001 stands, the view-MVP contract's "no framework extraction" refusal
stands, and the delta instrument (what to change, when) remains a face-gate
concern, not this document's.

One sentence before the architecture: the framework north is not a rendering
library. It is the discipline under which **views become material in the land
they render** — data objects with provenance, versions, and lineage, projected
to pixels for humans and to rows for agents by the same machinery that
projects everything else. Get that one move right and versioning,
collaboration, agent authorship, and the 3D door all fall out of properties
Rama already has. Get it wrong — views as code outside the land — and every
one of those becomes a separate subsystem to build.

---

## 1 · Verdict on the type-hypothesis

The design track's §0 hypothesis was: *camera-over-addressed-world, not a
widget tree.* **Accepted, with two amendments.** The evidence:

- Every widget-tree candidate examined died on K1. Xilem: views are
  compile-time Rust. Flutter: widgets are code. Bevy BSN: the data-driven half
  is unshipped. React-descendants: the "slop of html, then react on top" Sid
  already rejected. The widget-tree *family* treats the view description as a
  program; Softland's founding requirement is that it be a value.
- Every system that got close to K1 is world/scene-shaped, not widget-shaped:
  the MapLibre style spec (a zoomable visualization as one JSON value,
  hot-edited at runtime), deck.gl/json (an external program authoring layers
  as data), Rerun's Blueprint (views as events in the same store as truth),
  tldraw (shapes as schema'd records). The K1 winners all describe *what
  exists in a space and how it looks at each scale* — not *how boxes nest*.
- The as-built substrate is already on the correct side: every vertex shader
  applies a world-space camera (`(world × zoom + pan) / screen`), currently
  driven at zoom = 1. The dummy artifact proved the full camera model —
  cursor-anchored continuous zoom, semantic cross-dissolve, address as a pure
  function of viewport — in ~450 lines. The retro already ruled: wiring zoom
  is "wire it," not "rebuild."

**Amendment 1 — the widget tree survives, demoted and interned.** Panels,
cards, rooms, and the editor need nested box layout, text flow, and clipping —
and the industry evidence is unanimous that this layer must NOT be
world-native: game engines lay out world-space UI in local 2D and project the
result; Bevy delegates layout to Taffy and text to an opaque measured block;
maps have no layout tier at all and are crippled for UI because of it. So the
model is **a camera-world of addressed entities, containing layout islands**:
a room or panel resolves its interior in its own local 2D space (the existing
`rect_tree` lineage — flexbox-ish, already pure cljc, already shared by render
and hit-test), and the island's *frame* is an addressed entity in the world
like any other. Never make flexbox zoom-aware; never force the world through a
box model. Two grammars, one containment relation.

**Amendment 2 — the root noun is the address, not the camera.** The camera is
just view-state — three numbers, data in a spec. What actually distinguishes
this architecture from both map engines and widget trees is that **every
rendered element carries a stable address end to end**: from the kernel object,
through the semantic-layer row, through the scene-store slot, through the GPU
instance and the token row, back through hit-testing. The research kept
converging on this from independent directions: WebRender's interning (damage =
ID-list diff), MapLibre's cross-tile symbol identity (labels never jump across
LOD seams), AccessKit's stable-ID precondition (without it both projections
collapse to rebuilds), tldraw's record IDs, W1's address grammar. Stable
addresses are the shared currency of the pixel face, the token face, the diff
protocol, and collaboration. The framework should be named from that:
**an address-projection engine** — the camera is merely one of its parameters.

Falsifier honored: the demand catalog was checked for anything a widget tree
serves *better at equal cost*. One candidate survived scrutiny — deeply nested
interactive panels (settings, forms, the editor chrome) — and Amendment 1
gives exactly those to the island grammar. Nothing else came close.

## 2 · The model — seven nouns and one loop

This extends the design track's five machines (log, semantic layer, two
projectors, camera-world renderer, assertion loop) downward into the render
stack. Nothing below contradicts them; machines 3–4 gain internal structure.

1. **World** — the addressed coordinate space. Entities are kernel objects
   with *derived* positions: layout is a projection of the log (x = time,
   y = lane for trails; region layout for the earth), deterministic, never
   force-directed, recomputable at any time (the Rama property). Positions are
   data rows, so the world survives re-layout the way any projection survives
   rebuild — and lazy server-side tiling (the tippecanoe/minzoom pattern, with
   per-entity enter-zoom as data) is available when scale demands it.

2. **View-spec** — the load-bearing noun. A view is an EDN value:
   `{query, projection-policy, camera, style-rules, actions, lineage}` —
   - *query*: which addresses/regions/kinds this view draws from the semantic
     layer (WP1 wrappers today; interest queries later);
   - *projection-policy*: altitude bands and what each entity kind renders at
     each band (dots / titles / 2-liners / open — the semantic-zoom authoring
     slot, which the ZUI literature says is the actual product);
   - *style-rules*: a **total expression sublanguage** — an EDN dialect of the
     MapLibre expression family (`get / match / case / coalesce / interpolate /
     step / zoom / let`), deliberately not Turing-complete, so an agent-authored
     spec can never hang the render loop and is always statically readable;
   - *actions*: **data, not closures** — `{:on-click {:action :open :target
     <address>}}`-shaped descriptors resolved against a registered handler
     vocabulary. This is rfw's and Airbnb's proven split, and it is also
     exactly Softland's center loop: an action descriptor IS a proto-
     ActionRequest. (It also closes the one non-serializable hole the code
     sweep found: `rect_tree` `:actions` closures.)
   - *lineage*: `based-on / supersedes / asserted-by` — because a view-spec is
     a **kernel object living in the land**, not a config file outside it.
     This is the Rerun Blueprint move (views as events in the same
     event-sourced store as truth, on their own timeline) landing on a
     substrate that was already event-sourced. Everything Sid asked for on
     2026-07-03 — natively versioned, collaborative, on-the-fly interfaces —
     is this one design decision: **specs are assertions**. Versioning is the
     log. Collaboration is the log. "On the fly" is a new assertion.

3. **Interpreter + two registries** — the only two places where code lives:
   - the **render-type registry**: per entity-kind, the authored
     representations per altitude band (what a claim IS as a dot, a title, an
     open card) and per island-kind layout builders. Curated code, grown only
     when a used form breaks (D-001 applied to the view layer — the SDUI
     lesson: instances free, vocabulary curated);
   - the **action registry**: descriptor → handler. Read-only actions run
     client-side (open, zoom, follow); world-changing actions become
     ActionRequests into Rama (D-008 gates these off for now).
   The interpreter itself is small: fold `(spec, query-results, camera)` into
   scene-store updates. It is a projection, so it can be rebuilt, versioned,
   and — at the Regime-2 horizon — itself rendered at zoom 100.

4. **Scene store** — the retained heart, and the diff boundary. A client-side
   keyed store of resolved drawables (`address → slot`), over-scanned beyond
   the viewport, updated **only at the reduce/consumer edge** (never inside
   `m/latest` — the CLAUDE.md law becomes an architectural wall). Its update
   language is Electric/Missionary's own `incseq` six-op diff
   (`:grow :degree :shrink :permutation :change :freeze`): `:change` →
   `writeBuffer` at a slot offset, `:grow/:shrink` → pool slot lifecycle,
   `:freeze` → settled history leaves the diff stream permanently ("history is
   terrain," paid for once). The as-built buffer pools with their three diff
   engines — and the dormant `gpu-mount` incseq bridge already sitting in
   `buffer_pool.cljs` — are this store's embryo.

5. **Frame** — per-RAF: cull the scene store to the viewport, resolve
   altitude-band opacity (labels fade, never pop — the MapLibre collision +
   fade recipe), encode. WebRender's law: one Scene, many Frames — pan and
   zoom rebuild only the Frame. This is what makes continuous zoom cheap, and
   it is a render-loop shape, not a substrate change.

6. **Two projectors, one store** — the pixel face (GPU encode) and the token
   face (an AccessKit-shaped push serializer: full tree once, keyed deltas
   after) read the SAME scene store and the same addresses. The token face is
   not an export; it is a citizenship right (D15), and it must fold — the
   verified evidence says naive 1-node→1-row projection costs 80–99% of an
   agent's tokens; the fold policy (merge/filter with a completeness check so
   folding never hides marks — ledger 16 at the token layer) is part of the
   view-spec, not an afterthought.

7. **Rooms** — layout islands with authored geometry: the editor at zoom 100,
   settings, the JIT component workshop, one day a 3D room. A room's interior
   runs the island grammar (rect-tree lineage; box layout; text flow); its
   frame is a world entity; its contents are addressed; a room kind is an
   entry in the render-type registry. The rim (scope · delta · address ·
   palette) is the one screen-space layer that never zooms.

**The loop** that closes it: gestures hit the scene store (same tree renders
and hit-tests — already the as-built correct form), resolve to
`(address, action-descriptor)`, and either mutate local view-state or emit an
ActionRequest into Rama; Rama decides; kernel events flow back as semantic
diffs; the scene store updates; both faces move. The back-arrow rule is
preserved by construction: nothing renders that didn't come through the log
except explicitly-local view-state (camera, hover, collapse).

## 3 · Division of labor — who owns what

- **Rama owns:** truth, view-specs (as kernel objects), derived layout,
  altitude text forms (title / 2-liner / full as contract data — D3), the
  token projections' source (View-3 already generates server-side), and —
  at scale — interest: the viewport as a first-class query (bbox × zoom-band
  × kind, per-query fidelity; the QBI pattern) so zoom band = subscription
  fidelity = render LOD, one knob. Softland's semantic layer is already the
  "one layer, never render-ready" wall (W3); this study found nothing that
  needs that wall breached and several architectures (SDUI divergence,
  Figma's untyped tree) showing what happens when it is.
- **Electric owns boundary 1 — semantic deltas, not frames.** Its verified
  role: carry `incseq` diffs of query results and view-specs server→client
  ("no collections over the wire, only diffs"; `e/for-by` diffs server-side).
  Electric's footprint stays what it already is in this codebase — the
  truth-to-client tissue — and never enters the per-frame path. Writing the
  scene-store consumer makes Softland the first serious `electric-gpu`; the
  protocol is public and stable, the consumer pattern (interpret six ops at a
  retained store) is the same one `electric-dom` embodies.
- **Missionary owns time.** Sources → scoped `m/latest` derivations → one
  sample point → reduce with skip. Unconditional RAF + `identical?` stays the
  law **until** the scene store consumes real diffs — the `incseq` stream is
  precisely the change signal CLAUDE.md's Gap 3 has been waiting for, which
  makes conditional RAF (and the dormant dirty-present machinery) legal for
  the first time. That transition is measurement-gated, not assumed.
- **WebGPU owns pixels, with an AOT-enumerated vocabulary.** The Zed-lineage
  instinct in Sid's own 2026-02 doc is confirmed by the as-built reality
  (rect / shadow / MSDF / slug already carry the whole product) and by
  Impeller's lesson: enumerate the finite primitive set, precompile every
  pipeline at load, never compile mid-zoom. The north adds primitives rarely
  and deliberately (line/curve when Manhattan breaks — already O-1;
  image/sprite when a form demands; per-instance z when 2.5D layering
  demands) — each a registry event, not a rewrite.

Where truth enters: exactly once, at boundary 1. The server mirror-atom
workaround is an implementation detail of that boundary and can be replaced
under it without moving the boundary.

## 4 · How on-the-fly UIs actually work

**Author.** Three entrances, one destination: (a) a human adjusts a live view
— every adjustment is a spec patch (order param, altitude, lens — the W5
discipline of spec-shaped params from face 1 makes this true from day one);
(b) an agent emits or patches a spec mid-conversation — the MCP-Apps
interaction pattern (agent → spec → host renders live inline), but with an
EDN semantic spec instead of HTML-in-iframe, so the result is zoomable,
multiplayer, and token-legible instead of an opaque rectangle; (c) the forge:
a scoped agent conversation refines a view against live render feedback —
which is precisely Sid's 2026-03 JIT-component design (schema + render + demo,
conversation-scoped, cached forever), pointed at view-specs instead of
converted buttons.

**Interpret.** Live, no compile step: specs are values; the expression
sublanguage is total; unknown constructs degrade visibly (the marker-table
law from the view-MVP contract generalizes: unknown → plain + notice, never
dropped). A spec that references an unregistered render-type renders as an
honest hole — the registry is the capability boundary, and a hole names the
next registry candidate (desire-paths applied to the vocabulary itself).

**Version.** Specs are assertions with lineage; edits are new events on the
view's own timeline (Blueprint's isolated-store-with-own-timeline, which Rama
gives for free); **scene-as-patch layering** (the BSN idea, trivial in EDN)
makes "my variant of your view" a patch with provenance rather than a fork of
a file; tldraw-style up/down migrations keep old specs opening as the schema
evolves — history-is-terrain applied to the views themselves. A changed spec
can render its own diff (D10) because both versions are values in the log.

**Share.** A spec travels as data — to another person (same land, same
addresses), to an agent (the spec IS its own minimal-token description —
compact, typed, self-describing), into a context bundle, or across worlds
later. Sid's evolution answer is the governing dynamic: defaults first
("this might help"), then solidify by observed demand — the framework's job
is to make each solidification step an assertion, so the UI's own history is
a trail the trail view can render.

## 5 · Agents — authoring and reading

An agent is a first-class inhabitant of the view layer in exactly two verbs:

- **Writes:** an agent asserts view-specs and spec-patches through the same
  loop as every other assertion — `asserted-by` provenance mandatory, so
  machine-proposed views are visibly distinct from human-signed ones
  (proposed ≠ signed at the view layer too). The total expression language +
  registry boundary make hostile or broken specs safe by construction: worst
  case is an ugly view or an honest hole, never a hung loop or an exfiltrated
  capability.
- **Reads:** the token face. Same store, same addresses, folded to budget.
  Because every face renders its own address (D-008.4 law), the screenshot
  loop and the token face are two views of one pointer — an agent can go from
  a screenshot to the query to the material without a human translating.
  The H2 bet (relations make agent handoff cheaper) eventually measures this
  face; the framework's obligation now is only that the token face is never a
  second pipeline (W3) — divergence between what Sid sees and what the agent
  reads is the failure mode the map-must-not-lie law exists to kill.

## 6 · 3D and collaboration — same architecture, larger numbers

**3D.** The model is already dimension-agnostic in the right places: addresses
don't care; specs don't care (camera gains a dimension; projection-policy
gains a band axis); the scene store is data; only two things are inherently
2D — the island layout grammar (which stays 2D by design even inside 3D
worlds: game engines project laid-out panels into world space, and so will
Softland's 3D rooms) and the current pipeline set (which grows by registry
event: depth, per-instance z, mesh primitives when a 3D room form demands
them). Cesium's screen-space-error refinement generalizes LOD to "semantic
coarseness" with one rule that works in 2D now and 3D later. A 3D room is a
room kind — not a second architecture (D14 held).

**Collaboration.** Rama is already the hard half: server-authoritative order,
event-sourced, scalable. The verified Figma model (LWW per (object, property),
client IDs, atomic parent+fractional-index, optimistic clients that trust
their unacked edits) maps almost 1:1 onto depot-order + a PState — **but only
for spatial/layout state.** The research's sharpest cross-cutting finding:
every industrial multiplayer system converges by *erasing* disagreement
(LWW or rebase), and Softland's constitution says the opposite for meaning —
disagreement is preserved until synthesis is real, and the relation kernel
already implements that (assertions never merge; both asserters render,
badged). So the north draws one line: **layout state converges (LWW);
epistemic state accumulates (assertions).** Presence is just more rows
(warmth, walkers, the kestrel); interest management is the viewport-as-query
pattern when H5-scale arrives. No CRDT library, no second truth path, no new
architecture.

## 7 · The composition story — "hierarchical, easy to reason about"

Sid's 2026-07-04 requirement, answered as a ladder where **every level is
data except two named code layers, both registries, both D-001-paced**:

```
L0  GPU pipelines        CODE (registry #1) — few, AOT-enumerated, grown rarely
L1  scene primitives     data — rect / glyph-run / shadow / connector / island-frame
L2  render-types         CODE (registry #2) — kind × altitude-band representations,
                          island layout builders, action handlers
L3  components           data — spec fragments, patch-composable (BSN layering),
                          token-mapped (the JIT library's schema section, kept)
L4  view-specs           data — query + policy + style + actions + lineage
L5  faces / rooms        data — addressed, versioned instances of L4 in the land
L6  the land             the log, projected
```

Compare the html-slop stack Sid rejected: HTML's levels are all code-bearing,
none addressed, none versioned, and the reactive layer sits *on top* fighting
the document model. Here the reactive layer sits *underneath* (diffs feed the
store), addresses run through every level, and the two code layers are thin,
enumerable, and grown only on form-break. That is the honest sense in which
this can be "best principle wise": not more features than the DOM — fewer,
with the growth path governed.

## 8 · Continuity — the north is reachable by accretion (evidence)

Not a build plan; a demonstration that no wall stands between as-built and
north. Each north-noun has a living embryo in the tree today:

| North noun | As-built embryo | Distance |
|---|---|---|
| World camera | pan/zoom uniforms in every shader; slug AA already zoom-aware | wire a constant + hit-test math (retro O-3) |
| View-spec | `!effective-local-world` mode map; W5 spec-shaped face params; View-3 address-as-data | schema + persistence, not invention |
| Interpreter/registries | the 7-step recipe's `case` branches (the friction the retro logged) | registration replaces dispatch — the one real refactor, incremental per mode |
| Scene store | cached scene atoms (`!sidebar-scene` pattern, gate-13 discipline); buffer pools + `gpu-mount` incseq bridge | promote from per-mode atom to keyed store |
| Frame | RAF sample + `identical?` skip + per-subsystem diff gates | cull step; fade attribute |
| Token face | WP1 `render-bundle-text` server-side; View-3 verbatim law | generalize per-spec; fold policy |
| Actions-as-data | `rect_tree` `:actions` (closures today); `dispatch-event` bubbling | descriptor vocabulary swap |
| Rooms | the editor; settings modal; rect-tree islands | anchor frames into world coords |

The pattern in the right-hand column is uniform: promote, wire, generalize —
never replace. This is the same shape as the retro's no-rebuild verdict, now
extended from "the two faces" to "the framework north."

## 9 · Divergence check — and the one leg that needs evidence

**No fundamental divergence found; no rebuild-in-place candidate bet is
warranted.** The commission asked for a CANDIDATE BET if the substrate cannot
reach the north; the finding is the opposite, and §8 is the evidence. A bet
needs a falsifiable route-claim whose failure would change the route — the
one genuine unknown found does not qualify, because both of its outcomes keep
this architecture:

**Evidence item (pre-registered here, measured at face-gate time per the
view-MVP contract's delta-instrument slot):** *does Electric carry land-scale
deltas?* — the Part-II gunpowder question, now sharpened by the verified
protocol: feed a synthetic 10⁴-element `incseq` stream (grow/change/permute
mixes at trail-view rates) through boundary 1 into a scene-store consumer and
read the existing `[RAF]`>5ms instrument. If it holds: Gap 3 closes on
Electric itself. If it fails: the scene store's boundary means the fallback is
a coarser wire protocol (windowed rows re-diffed client-side) behind the SAME
store — transport swaps, model stands. Either way no rebuild; that is why it
is an evidence item and not a bet. The known traps to watch in that
measurement, from the verified research: `:permutation` degrades to full
re-upload without an indirection buffer; slot lifecycle on `:shrink` (a freed
slot that isn't cleared masks future truth); ID churn collapses everything.

## 10 · Fork 2 — OPEN (founder's), with this study's recommendation

Fork 2 asks: does the gentle slope break at the Rama/Electric/WebGPU seams —
one substrate or three? **It stays OPEN; only Sid closes it.** What this study
adds as evidence for the sitting: the two seams were examined and both carry
load without breaking — the Rama→client seam has a verified diff protocol and
a proven ×5 bridge precedent; the reactive→GPU seam has a named boundary (the
scene store) that every mature system independently converged on
(WebRender's Scene, Rerun's store, Figma's retained tree). The editor already
ships on the same five pipelines the land needs. **Recommendation: one
substrate, three *projection families* (world, islands, token) over one
address space** — the seams are where projections change, not where
architectures change. The strongest counter-signal to watch for: if the
island grammar (editor-grade text interaction) ever demands renderer-level
machinery the world grammar cannot host (e.g. sub-pixel text shaping at
arbitrary zoom under IME), that is the fork reopening with evidence.

## 11 · Open questions, each with a recommendation

1. **Expression-language scope.** Which op families does v1 of the style/
   policy sublanguage carry, and is SCI ever allowed inside a spec?
   *Rec:* MapLibre's families in EDN (`get match case coalesce interpolate
   step zoom let` + arithmetic/color), total by construction; SCI stays
   OUTSIDE specs — trusted code enters only via the render-type registry.
   Camera-vs-data expression separation is kept from day one because it is
   also the reactive invalidation map (what recomputes on zoom vs on data).
2. **Where layout runs.** World layout as a Rama projection vs client-side?
   *Rec:* world layout is derived data in the semantic layer (deterministic,
   log-projected, agent-visible); island layout is client-side and ephemeral.
   The line is the island frame.
3. **Scene-store thread.** WebRender builds scenes off-thread. *Rec:* main
   thread until the `[RAF]` instrument objects; the store's data-in/data-out
   shape (pure cljc discipline, per the B2 placement ruling) keeps the worker
   door open without pre-paying for it.
4. **Fold policy for the token face.** How much folding, governed how?
   *Rec:* fold policy lives in the view-spec with a completeness check
   (folds must carry counts — ledger 16 applied to tokens); measure against
   the H3 agent-benchmark when it exists.
5. **Proportional text / shaping.** Monospace advance is a load-bearing
   simplification (~30 sites). *Rec:* keep mono for all v0/v1 faces (the
   design language is mono-first); if cartographic labels at earth scale
   demand proportional, it enters as a new text render-type (registry event,
   collision-grid already pattern-proven), never as a mutation of the mono
   path.
6. **LWW line placement.** Which properties exactly are "layout" (converge)
   vs "meaning" (accumulate)? *Rec:* rule at the read→write milestone
   (D-008.5) when the write surface exists; default anything ambiguous to
   accumulate — erasing disagreement is the irreversible direction.
7. **Interest-query activation.** When does viewport-as-query become real?
   *Rec:* not before H5-scale material exists (matches the standing open
   question on server-side spatial queries); until then client-side filtering
   over full feeds, with the query *shape* (bbox × band × kind) already
   present in view-specs so activation is a backend change, not a spec change.
8. **The desert-fog obligation.** Which layer owns "content lies this way"
   cues? *Rec:* the semantic layer — frontier/fog is data (aggregates +
   uncovered counts already exist in WP1 shapes); the renderer may never
   synthesize terrain the layer didn't assert (map-must-not-lie at the
   emptiness boundary, and the anti-desert-fog fix for free).

## 12 · Laws this north adds to the walls

W1–W5 stand untouched and did most of the work already. The study adds five,
all evidence-backed:

- **N1 — Specs are assertions.** A view-spec that isn't in the log isn't a
  view, it's a prototype. (The whole of §4 rides on this.)
- **N2 — Two code layers only.** Capability enters through the pipeline
  registry or the render-type/action registry, on form-break, and nowhere
  else. A spec must never smuggle code (no closures in scene data — the
  as-built `:actions` hole closes and stays closed).
- **N3 — Stable addresses end to end.** Any layer that mints its own transient
  IDs for addressed material (per-frame keys, index-as-identity) is breaching
  the wall that both projections and the diff protocol stand on.
- **N4 — Layout local, transform after.** The island/world boundary is
  one-way: islands resolve in local 2D, then project. Zoom-aware flexbox and
  world-coordinate text flow are both forbidden — every system that tried
  either paid for it.
- **N5 — Converge layout, accumulate meaning.** LWW may touch geometry only.
  Epistemic rows never last-writer-win. (The Figma inversion, named.)

---

*Companion: `INPUTS.md` (the full evidence manifest). Consumers: the Fork-2
sitting when Sid convenes it; the delta instrument at face gates; any future
session asked "what framework are we walking toward" — the answer is this
document, and it changes only the way North changes: by evidence, at a
sitting, in the log.*
