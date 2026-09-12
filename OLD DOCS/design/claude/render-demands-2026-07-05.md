# Render Demands — what the land asks of its framework

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../../../docs/carry-on.md) as a reference summary
and [the vision log](../../../vision/LOG.md) as the primary source.

**Track C → Track D handoff.** Written 2026-07-05 at the close of the Track-C
write-gesture sitting (Fable, designer lens, Sid live). This is the DEMAND side
only: what the framework must make possible, extracted from artifacts that
exist and can be driven. It binds nothing about implementation. It is input
(e) to Track D's ideal-framework study, alongside (a) Sid's framework docs,
(b) vision/LOG + BETS North, (c) the prior-art collection pass (not yet run),
(d) `build/render-substrate-retro/PRIMITIVES.md` + `build/view-mvp/` (as-built).

**Evidence base (all in `design/claude/`, all drivable):**
`write-gesture-sketch-2026-07-04.html` (v6: gesture beats, 1,000-thread scale
stage, the walk, the terrain descent; 27-item do-not-preclude ledger) ·
`softland-horizon-2026-07-05.html` (the full-scale dream, laws-on) ·
`softland-dummy-2026-07-05.html` (walkable simulation: camera + semantic LOD +
marks + walk, ~450 lines vanilla canvas) · the design decision-log + capstone
laws (esp. Law 7: zoom is a functor preserving identity, relation, confidence,
time, agency, open questions).

---

## 0. The type-hypothesis (STANCE — Track D's to confirm or kill)

The demands below imply a **camera-over-addressed-world renderer, not a widget
tree**:

- one world coordinate space; views are camera + projection-band policy over it;
- "components" are **view-specs-as-data** interpreted at runtime (agent-writable),
  not compiled widget classes;
- text is the primary material (screen-constant labels anchored to
  world positions — the cartographer's model), with a mark/assertion overlay as
  a first-class layer;
- every rendered element carries an address end to end.

Closer kin: map engines and game-world renderers with an ECS-ish data spine —
than React-style component libraries. That is a hypothesis from the demand
side; prior art + the as-built retro may sharpen or break it. Falsifier: a
demand below that a widget-tree model serves *better* at equal cost.

## 1. The demand catalog (falsifiable; each with source + evidence)

**D1 — Addressed instances.** Every visible element carries a stable address
resolvable by humans-in-speech and agents; selection shapes (point / interval /
prefix / suffix / cut / pair / set) are addressable at gesture grain.
*Source: ledger 1–3; INPUTS 14. Evidence: dummy rim address updates per move;
sketch selection strip.*

**D2 — One camera, lawful motion.** Zoom-to-cursor, drag, animated flyTo;
**animate within a geometry, cut between regimes, label every cut** (the gutter
is content). Three regimes: ground (time × lane), room (authored), ladder
(derivation). *Source: ledger 27; capstone Law 7; giants (Bederson ×
McCloud). Evidence: dummy camera + gutter labels; terrain descent stage.*

**D3 — Semantic LOD as data, not renderer hardcode.** Altitude bands
(dots / titles / 2-liners / open) with alpha ramps; the text at each band comes
from contract data (title · 2-liner · full per node — ledger 13; version
statements as fold text — ledger 23). LOD policy must be a projection of data
the contract supplies. *Evidence: sketch altitude buttons; dummy alpha bands.*

**D4 — Text at cartographic density.** Screen-constant mono text anchored to
world positions, mixed with world-scaled geometry. Ground views: 10²–10³ live
text elements; earth views: ~10² labels over 10⁴–10⁵ dots (aggregates only —
folding is law, raw millions are never rendered). **Known collision, first
delta-instrument reading:** the design language leans on glyphs
(⊢ │ ├ └ ─ • ✓ ✗ box-drawing + turnstile) that the current atlases cannot
render — Track B's charset audit found 95-glyph ASCII atlases, structural
chars unrenderable. The framework needs an extensible glyph pipeline or vector
fallback; the design will not retreat to ASCII. *Source: taste #1;
CHARSET_AUDIT.md; every artifact this sitting produced.*

**D5 — The mark layer.** Assertions render as a distinct material (kraft) over
terrain: badges, strikes, pennants, stance chips — anchored to elements,
carrying asserter identity, proposed≠signed always distinguishable (ledger 5,
7, 11). *Evidence: sketch beats 3–7; dummy sign interaction.*

**D6 — Open-in-place.** Local expansion with zero global reflow — no layout
dependency between siblings (the anti-outliner law: detail costs local space,
never the shared axis — ledger 14). Overlap tolerated; optional local dilation.
*Evidence: sketch stage 1 node-open; Sid's founder face test (LAW).*

**D7 — Folds and fog.** Aggregation chips at every fold carrying mark / dead /
question / new-since counts (ledger 16: no altitude may hide that marks exist
below); **fog as a first-class render state** — emptiness renders as terrain,
never blank (ledger 25). *Evidence: scale stage fold-chips; dummy fog coast.*

**D8 — Routes and walks.** Camera choreography over an ordered stop list;
attest-visuals (chip dimming); the route itself renderable as a path on the
land. Walks are data (ledger 21). *Evidence: walk stage; dummy morning walk.*

**D9 — The rim.** A constant chrome layer with exactly four slots (scope ·
delta · address · palette), identical across every face, address always live
(ledger 26). *Evidence: terrain descent — rim constant across six frames.*

**D10 — View-specs as data (the deepest constraint).** A view is a data object
interpreted at runtime: agents can write one; a scratch room can instantiate
one mid-sentence; a changed spec renders its own diff (renovation shows its
diff); specs have versions and lineage. The framework is therefore a
**view-spec interpreter over query results**, not a set of hand-built screens.
*Source: INPUTS 3; the view-forge scene; Sid's on-the-fly-UI vision (LOG
2026-07-04).*

**D11 — Rooms.** Authored-geometry canvases (unrestricted lawful zoom) hosting
arrangements, transcluded rooms, and — milestone 2+ — editable surfaces over
addressed material (the existing WebGPU editor hosted as a room). Arrangement
is data (an event). *Source: broader-frame commitments; derivation grammar.*

**D12 — Ambient life without full-scene cost.** Presence warmth (read-wear
shading), a visitor dot walking, freshness states — continuous low-cost
animation compatible with the existing render-loop discipline (unconditional
RAF + `identical?` skip; no side effects in `m/latest` — CLAUDE.md law).
*Evidence: dummy kestrel; warmth strokes.*

**D13 — Honest channels, both themes.** Token-driven light/dark; truth-states
(fact / hypothesis / guess, proposed / signed, fresh / stale) encoded
redundantly, never color alone (Law 12). *Evidence: all three artifacts.*

**D14 — Doors held open.** A 3D room as a view-spec kind (WebGPU is native
ground); multi-mind presence; text-projection duality (D15). None built now;
none may be precluded.

**D15 — Projection duality (the most Softland-specific demand).** Every face
has a text body: the same view-spec must project to pixels for humans AND to
minimal-token rows for agents (View 3 is a citizenship right, not an export).
A framework that renders only to screen fails the first paying reader.
*Source: D-002 View 3; INPUTS 3; ledger 10.*

## 2. Scale bounds (honest, staged)

| Stage | On-screen elements | Notes |
|---|---|---|
| Phase 1 (solo, read-only) | ~10²–10³ nodes, 10² marks | current corpus, one land |
| Year-scale (solo + agents) | 10⁴–10⁵ addressable, folded to 10³ visible | folding is law |
| Horizon (many minds) | earth = aggregates only | never raw; D7 carries it |

## 3. What this document does NOT do

Choose an architecture · judge the current renderer (Track B's PRIMITIVES.md
owns as-built) · select prior art (input (c), unrun) · resolve Fork 2
(one-substrate-or-three — founder fork). Anchoring order stands: ideal ×
as-built × face-demands meet only at Track D's delta step; this doc is the
face-demands leg, now complete enough to stand on.

## 4. Suggested Track D opening move

Assemble `build/render-north/INPUTS.md` = this doc + (a) + (b) + (c-when-run) +
(d); then test §0's type-hypothesis against the prior art; then the delta
instrument vs PRIMITIVES.md. First checkpoint question for every candidate
architecture: **can it do D10 (interpret view-specs) and D15 (project the same
spec to pixels and to tokens)?** Most frameworks die on those two.

---

# Part II — the emperor's brief to the engineering head

**The commission (Sid, 2026-07-05, verbatim in spirit):** this handoff runs
from the principal visionary to an engineering head whose job is to *see the
whole* — what the final phase is — and judge whether we have the gunpowder to
build to that level **without descending into refactoring or shit-patching.**
Part I is the demand catalog. Part II is the whole, the walls, and the powder.

*(Honesty flag: the powder judgments below come from reading the code, the
retros, and the decision log across this window — and from building the three
drivable artifacts — not from load tests. They are the designer's briefing on
WHERE to verify, with committed reads where evidence exists.)*

## 5. The whole, in one frame

At final phase, Softland is **five machines**. Everything else is content.

1. **The log** — append-only events, engine-neutral schemas (the log is the
   asset; Rama is the host). All truth. Already real.
2. **The semantic layer** — queries/bundles over the log: nodes with altitude
   text forms, relations, marks, aggregates, walk cursors. One layer, never
   render-ready. (Track A's WP1 territory.)
3. **Two projectors off that one layer** — the token face (View 3, agents) and
   the pixel face (humans), both driven by **view-specs-as-data**. Same spec,
   two projections (D15).
4. **The camera-world renderer** — one world space, lawful zoom regimes,
   cartographic text, mark overlay, rim (D1–D9, D11–D13).
5. **The assertion loop** — select·type·sign, walks, versions: gestures that
   append back into the log, closing the circle.

The horizon (many minds, economy, 3D rooms, the earth) is these same five
machines at larger numbers — **not a sixth machine.** That is what makes the
final phase buildable at all.

## 6. The load-bearing walls — right early, or rewrite everything

The Rama property does most of the anti-refactor work for free: **anything
that is a projection can be rebuilt from the log at any time.** So the
refactor-hell risk concentrates in exactly the places that are NOT
projections. Five walls:

- **W1 · The address grammar.** Object ids, anchors, AND view addresses
  (`view://…`, `room://…`) — every face, agent, screenshot, and view-spec
  binds to them forever. A second address scheme appearing later is the
  beginning of the end. Get one grammar, version it, never fork it.
- **W2 · Event schemas.** Engine-neutral maps; two clocks (claimed-time vs
  arrival-time); custody + assertion both durable ("sid, by fable's hand").
  Already ruled in the decision log — the wall is holding the line in every
  new emitter.
- **W3 · One semantic layer, never render-ready queries.** The moment a query
  returns "rows shaped for this screen," the token face and pixel face begin
  to diverge and D15 dies quietly. Two projectors, one layer, forever.
- **W4 · The spatial camera model in the first pixel face.** If face 2 (the
  threaded/DAG timeline) ships as DOM-scroll outliner "for now," the
  camera-world retrofit is a rewrite of the face AND its habits. The dummy
  proves the camera model costs ~450 lines to stand up — it is cheaper to
  start right than to patch later. This is the single most tempting shortcut
  and the single most expensive one.
- **W5 · View parameters as data from face 1.** The first faces may be
  hand-built (no interpreter yet — that would violate the D-001 clock), but
  their parameters (root, span, altitude, folds, lens) must live in a
  spec-shaped map from day one. The interpreter is furniture; the spec shape
  is a wall.

**Everything not on this list is furniture** — deferrable on the D-001 clock,
because log + projections make later moves cheap: fold-aggregates, walk
choreography, warmth, fog, rooms, the forge, 3D. Defer freely; they are new
projections, not surgery.

## 7. The gunpowder audit

- **Rama — powder confirmed, architecturally.** Append-only log +
  recomputable PStates is precisely the anti-refactor substrate; new demands
  become new topologies, not migrations. The kernels were built for
  convergent re-import. No structural wall between here and horizon numbers
  found in this window's reading.
- **WebGPU + the existing editor — powder substantially confirmed.** GPU mono
  text at density is already shipped (the zoom-100 editor); the camera-world
  model is a render-loop shape, not a substrate change (dummy as evidence).
  One bounded gap: **the glyph pipeline** (95-glyph ASCII atlases vs the
  design language — D4). Known fix, known tool, do before face 2 ships.
- **Electric — the one leg needing a verdict.** Track B's retro says YES for
  the near faces. The unproven stretch: differential updates at 10⁴+
  addressed elements (the CLAUDE.md "Gap 3" note — dirty-present needs
  Electric diffs). The engineering head's first real question: does Electric
  carry land-scale deltas, or does the render path need its own delta
  protocol between the semantic layer and the GPU loop? **Answer this before
  face 2 scales, not after.**
- **The LLM/JIT lane — powder exists** (JIT workflows already in the
  codebase); the view forge is that lane pointed at view-specs.

**Rule of thumb for every future change:** if it's a new projection, defer or
build freely. If it touches W1–W5, stop and contract it (work-package
machinery exists for exactly this).

## 8. Shit-patching tripwires — smells that mean a wall is being breached

1. A query that returns screen-shaped rows (W3 breach).
2. A second way to address anything (W1 breach).
3. A mark rendered from client state that never hit the log (back-arrow
   breach — the shadow-log ledger item).
4. Altitude text derived by truncating whatever string exists (ledger 13
   breach — semantic zoom decays into ellipsis).
5. "We'll add the spatial model after the outliner version" (W4 breach — the
   expensive shortcut).
6. A face whose parameters exist only in code (W5 breach — the land's views
   stop being the land's material).

Any one of these appearing in a diff is grounds to stop the phase and
escalate, per the standing stop-clause discipline.
