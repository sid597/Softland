# The base layer — derivation (Fable, 2026-07-12)

Commissioned by Sid's session opener: derive the ONE floor under the five
unlocks from first principles against the record; adopt no staged material by
default; present divergence where the derivation disagrees with prior
assumptions. This document is the derivation and the test. The contract cut
from it is `CONTRACT.md` (same directory).

## 1. What "base" must mean

A base layer is the thing such that each unlock's remaining work becomes
**adding material INTO it** — data, faces, marks, gestures, probes — never
building another floor under it: no new render path, no new state channel, no
new addressing scheme, no second write loop. Test (Sid's, verbatim): for each
of the five unlocks, name what the base gives it; any need the base doesn't
meet is either a base defect or genuinely later — say which, with grounds.

## 2. Finding: the server floor exists; the client floor is scattered embryos

The record's rhyme (object-container existed before blocks, Jul 9;
`:object/edit` existed before block-write, Jul 12) repeats a third time, one
level up. Walk the five unlocks' SERVER needs: object storage + ingest
(object-container), blocks (block-kernel), typed edges + provenance
(relation-kernel), faces/assemblies as data (assembly + arsenal kernels),
writes (`:object/edit` + STREAM echo p95 7.66ms, committed), machine marking
(llm-module + machine-cut's live `pairs-with` run). **Every server organ the
five unlocks need already exists.** No new Rama organ is part of the base.

The CLIENT half of the center loop is where the floor is missing — and it is
missing as *fragments that each prove the right form locally*:

- **Scene trees with layout + hit-testing exist** (`rect_tree.cljc`:
  `rt-node`, `resolve-layout`, `tree->rects/text-ops/shadows`, `hit-test`) —
  the island grammar core (DELTA-B1 Δ21). But `resolve-layout` bakes
  absolute coordinates at flatten time, and producers add region offsets on
  top (`combined_text.cljs:444` — `offset-text-ops* ... sb-w`).
- **Cached scenes exist per mode, as singletons** (`!face-scene`,
  `!sidebar-scene`, `!trail-face-scene` — `runtime.cljs:28-35`). One face at
  a time; two simultaneous instances of anything is inexpressible.
- **One-tree render+hit-test is proven — locally.** Faces and sidebar
  hit-test the CACHED scene (`mouse.cljs:401-419` block pick;
  `mouse.cljs:421+` trail face). Chat and flow still rebuild trees at click
  time (`mouse.cljs:338,379`) — the drift-bug class DELTA-B1 Δ5 names.
- **Actions-as-data is proven — in one face.** Trail-face clicks dispatch on
  `{:action <kw>}` descriptors in node `:data` (`mouse.cljs:429-430`);
  elsewhere actions are closures (Δ6's hole).
- **Pick→address→edit→write→echo is proven — for one face kind.**
  block-write (gate-passed 2026-07-12) resolves a click on the cached face
  scene to a block unit-id, focuses, edits, writes through `:object/edit`,
  and re-renders from the echo. The full loop, one face kind.
- **Every rendered rt-node carries its template address**
  (`:assembly/src-path`, landed `c788188`) — click-to-edit's prerequisite,
  unconsumed.
- **The camera is global and dormant.** All four pipelines apply
  `(world_pos * camera.zoom) + camera.pan` (`renderer.cljs:41,161,253,322`);
  zoom is hardcoded 1.0, pan is `(0, -scroll-y)`; no zoom gesture exists.
  Chrome stays put by counter-baking scroll into its coords, which forces
  chrome re-layout on EVERY scroll tick (`render.cljs:275`).
- **No per-container transform exists.** Moving/zooming one thing = CPU
  re-layout + full buffer rewrite. Text upload is monolithic
  (`update-text-data` reshapes ALL visible glyphs on any text change) — the
  64-editor wall (spatial ROAD §1).

## 3. The derivation: four capabilities, one organ

Extract each unlock's floor-demands from Sid's verbatim asks and the record;
they collapse into exactly four capabilities:

1. **Plurality** — many view-instances in frame at once: 64 editors ("what
   is the performance of all this"), two faces side-by-side over one
   conversation, the design surface beside the thing being designed, an
   island beside a text face. The per-mode singleton atom is the wall; the
   store must key `(view-instance, address)` — precisely birth-law Δ1
   (H6, Sid-ruled 2026-07-06).
2. **Addressability** — every rendered thing resolves to
   `(address, appearance)`: pick a pixel → address → Rama truth + context
   bundle out (the agent context seam — agent and mouse are the same
   finger); a mark landing on an address finds every appearance (fan-out
   index). This is what makes the map queryable back — View-3 legibility.
3. **Per-object transform** — every container pans/zooms/moves
   independently, composed with the world camera ON the GPU; instance data
   in container-local coords; settled arrangements commit as assembly
   events (arrangement-only data — the existing kernel; no fn values).
   Kills four debts at once (spatial ROAD §2): per-object control, the
   chrome counter-bake tax, the 64-editor wall, dormant whole-frame zoom.
4. **One write loop** — gesture/agent → ActionRequest → Rama → echo →
   scene update. Already committed and measured; the base only ROUTES it:
   action descriptors on scene nodes are proto-ActionRequests (Δ6), so any
   face's affordances reach the same organ without new plumbing.

Four capabilities, one organ: **the scene substrate** — one keyed store of
EDN slots (resolved drawables in container-local coords, face/template
address, provenance channel, action descriptors), a container-transform
hierarchy composed in-shader under two cameras (world/screen), one pick
seam over the store returning addresses + context bundles, writes riding
the existing organ. This is the one-render-substrate law (settled ground)
made real, under the birth laws Sid already ruled (Δ1 keying, Δ2 f64
store / camera-relative f32 GPU, Fork-2 one-substrate).

## 4. The test — each unlock against the base

**1 · Design unlock** ("everything buildable + controllable in-frame by
agent and mouse/keyb"; figma-style edit 80→95%; MINDBLOW: pull arsenal UIs
up mid-conversation).
*Base gives:* pick → `:assembly/src-path` → template (landed, unconsumed);
edit template through `:object/edit` (organ committed); echo → re-render
(transport committed); per-object drag/resize (transform leg); proposals
render as overlay-stratum ghosts distinct from signed material (provenance
channel); agent affordances = the same action descriptors + context bundle
the mouse uses.
*Remaining work builds INTO:* the design-surface faces, the arsenal
browser, C1/C2 probes — all faces + arrangement data over the store.
*Not given, genuinely later:* nothing at the floor. C1's conversational
surface is a face over already-ingested conversation objects.

**2 · Editor + write + per-object control** ("once we have this every
other thing I can just directly build into it").
*Base gives:* per-editor container transform (pan/zoom one editor while 63
idle); per-container text geometry so a keystroke stops reshaping other
windows; caret/block addressability; the write loop it already has
(block-write, echo committed — the unlock's write half is DONE pre-base).
*Remaining INTO:* gesture wiring, editor-feel wear (already on Sid),
block-write G8/S3 (already on Sid).
*Not given, genuinely later:* rope-tree internals — Sid's own ruling
("we can break into chunks"): blocks are the chunks; no rope needed.

**3 · Semantic breaking of block types** (marks by local LLM; multiple
markings per node; the block container "is just a place to hold").
*Base gives:* a landing surface for marks — mark lands on an address in
Rama → fan-out index finds every appearance → those slots re-render/re-face;
proposed-vs-signed rendered distinct by the provenance channel
(map-must-not-lie in pixels, machine marks visibly machine).
*Remaining INTO:* annotators minting marks (llm-module — first one live),
new mark kinds (one reviewed enum line each).
*Not given, genuinely later — by design:* the kinds/lens round itself
(dg/olog/panproto, multi-marking) is gated on the dual-read — material-
gated at Sid's pace, not floor-gated. Grounds: the survivor rule — kinds
grow from what the material needs, never upfront ontology.

**4 · Representation of semantic blocks** ("different ways to represent a
thing in itself — thing = group = obj family").
*Base gives:* face-per-slot — the same address wearing different faces in
two view-instances SIMULTANEOUSLY (today's three faces can only wear
sequentially — the singleton wall); face swap = a data write, an
arrangement event; composition mechanics = assemblies nested in
container-local frames (a group is a container whose children are
containers — the transform hierarchy is the thing = group = obj-family
mechanics); scene-diff wearing = two view-instances over one base + delta.
*Remaining INTO:* the face design rounds, the composition grammar's
vocabulary growth (one reviewed primitive at a time — the growth engine).
*Not given, genuinely later:* nothing at the floor.

**5 · 3D render** (citizenship criterion: "liveable and controllable from
inside").
*Base gives:* the store is dimension-agnostic — keying and addressing
don't change when a slot's transform grows a pose; an island is a
container (own pipeline renders to texture, composited as a quad THROUGH
the same container transform — spatial ROAD: "the thing islands composite
through"); pick already returns addresses, so a 3D node = the same address
as its 2D card (identity survives strata); point-and-say is specified
dimension-agnostic.
*Remaining INTO:* the islands pipeline itself.
*Not given, genuinely later — by Sid's own hand:* the islands contract
stays staged behind his Box3D reading answer and his #5 ranking. The base
does not block it and nothing in the base precludes it.

**Verdict: the test passes.** Two "genuinely later" items exist and both
are Sid-gated by his own rulings (dual-read pace; Box3D answer + ranking),
not missing floor.

## 5. Alternatives falsified (so the staged materials weren't just adopted)

- **"The assembly layer is the base"** (everything-an-assembly, workspace
  = assembly of containers): assemblies DESCRIBE arrangement; something
  must still render many transformed, addressable containers cheaply.
  Description without the substrate leaves every unlock CPU-baking
  absolutes into one monolithic upload. The assembly layer sits ON the
  base; it is already built.
- **"The base is Rama-side"** (a workspace/arrangement kernel): settled
  positions are arrangement-only data and the assembly kernel already
  holds arrangement (faces-as-assemblies, D-011 form). One event
  vocabulary line, not an organ. Confirmed: the floor gap is client-side.
- **"Point-and-say / the agent seam is the base"**: the context bundle is
  the OUT half of pick; without the store it can only be assembled
  per-mode, one more fragment. It is a seam OF the base.

## 6. Divergences from prior assumptions (presented per the opener)

1. **Δ3's trigger is superseded.** DELTA-B1 paced the store's birth to
   "the face-2 (threaded/DAG timeline) render contract — not before." The
   named trigger never fired; a stronger one did: Sid's five-unlock
   ranking demands the store from five directions at once (plurality,
   pick, per-object control, simultaneous faces, islands' composite
   target). Named-trigger discipline is exactly satisfied — the demand is
   real, ranked, and verbatim.
2. **container-transforms is not a separate later package.** ROAD staged
   its contract behind Sid's Boxes/Minimap wearing notes. The wearing
   tunes gestures and defaults; the organ's existence was settled by Sid's
   verbatim ask ("dynamiclly have the full control over each and every
   object in frame"). Folded in as the base's transform leg; wearing notes
   still wanted, they amend gesture phases, not the architecture.
3. **point-and-say and scene-diff are not separate packages.** The
   pick/context-bundle half of point-and-say IS the base's OUT seam; the
   conversational loop and the diff-wearing are first consumers INTO it.
4. **islands stays staged** (consumer, Sid-gated) — unchanged from ROAD.
5. **No new Rama organ** — the arrangement settle-event rides the
   existing assembly kernel; the third occurrence of the rhyme.

## 7. Scope guard — what is NOT base (named refusals)

The design-surface faces and C1/C2 probes · the marks/kinds round · face
design rounds and vocabulary growth beyond the transform leg's needs · the
islands pipeline · editor internals (rope trees — refused by Sid's chunks
ruling) · registry-replaces-case (Δ12), conditional RAF (Δ15), text-run
interning (Δ16) — all trigger-paced inside or after the package, never
opening moves. Each refusal is an extension point the store's shape must
not preclude; the contract carries them as such.

*Sources: `vision/LOG.md` 2026-07-10→12 (Sid verbatim) · board FOREST ·
`build/render-north/DELTA-B1.md` (+ Sid's H6/N6/Fork-2 rulings 2026-07-06)
· `build/spatial/ROAD.md` · code at cited file:line (verified this
session, 2026-07-12) · `docs/history/sense-line-story.md` ch. 8–10.*
