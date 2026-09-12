# smalltalk-ui-vm — the Workshop: component types become living material

References below to `BETS.md` are historical; that document is retired.
For vision orientation, use [carry-on](../../../docs/carry-on.md) as a reference summary
and [the vision log](../../../vision/LOG.md) as the primary source.

**Status: SETTLED as direction 2026-07-30** — one direction sitting (Sid +
Fable), three corrections by Sid folded in as they landed: the Workshop is
NOT built (organs ≠ loop) · three strata, not two ("the code editor is
Softland at zoom 100") · sequencing (material zoom first, zoom-100 inside
when the need arises). Name is Sid's: *"like a smalltalk but for the ui
side"* — with his naming law: *"the name should always be such that i say
and it makes me think is the thing built enough, is it doing the thing it
was supposed to."* Verbatim strands: `vision/LOG.md` 2026-07-30. Parent
direction: `../editable-material/DIRECTION.md` — its horizon clause
*"recipes (named at recurrence)"* is **DISCHARGED by this package**: the
recurrence arrived as three structural pressures in one week through one
wall. Serves the H3 gate Sid pre-registered in BETS (2026-07-04): *"Softland
is used to make sense of Softland and to build Softland from Softland."*

## The blocker (checked against source, 2026-07-30)

The type is not an object in the world. The block type's *policy surface*
is material — seven facet-masters (six block wears + fm:space): values,
defaults, gesture→verb binding rows, all revisioned, activatable,
rollbackable, provenance-stamped. Its *structural surface* — which parts
exist, how they nest, where pixels come from — exists only as `block-tree`
(`src/app/client/workspace/ground.cljs:599`), a function: wears feed
**values** into a part-tree whose parts and nesting are hand-written code.
Probe receipts (tracked in `probes/`, checked against source by the probe
session and re-checked by this one): *"material can tune every green thing
today, and cannot add a single new part"* · *"turn a knob = data. add a
part = code. that is the wall you hit."*

The matter room inspects instances; the halo acts on instances; nothing
anywhere IS "how the thing is made," so no surface can stand Sid in front
of it. Adjectives material; the noun still compiled.

## The Workshop loop — Sid's spec, the acceptance spine

> type/anatomy exists as material → open it → see its composition → edit
> parts/nesting/bindings/defaults → render the candidate through the real
> interpreter → activate or reverse it

**None of the six arrows exists today.** The organs the loop reuses DO
exist and are the reuse surface, never the loop: halo entry (pick), the
room (container), revision/candidate/activation/rollback semantics, the
preview membrane, scene primitives, policy resolution. Reuse buys laws and
patterns; every arrow is new build.

## What the package builds — three missing things

1. **The anatomy layer** — the recipe made real: per-type part-tree,
   arrangement, per-part binding attachment as revisioned material;
   components-in-components (anatomies referencing anatomies). Likely
   triggers the parent DIRECTION's parked material-kernel-module
   platform-check ("first homeless truth" clause). The recipe identity
   already has a working name: P7 named the worn composition "block"
   (working scaffolding, Sid's to keep or change).
2. **The interpreter** — one generic constructor reads an anatomy + the
   subject's wears and yields the scene tree the renderer already
   consumes. At block cutover, `block-tree`-the-function dies (no second
   truth — the machine-tint precedent).
3. **The Workshop surface** — the six arrows over real machinery:
   type-condensation entry, anatomy projection (see composition), the
   edit affordance (THE one genuinely new widget class) + validated write
   lanes for anatomy edits, candidate preview **through the real
   interpreter**, activation with structural-migration honesty.

## Laws carried from the sitting (falsification-derived)

- **Pressure-cut vocabulary.** The anatomy schema is cut FROM the three
  living pressures, which span the three birth kinds: paste takes at most
  half the space (policy birth) · visible ctrl+enter model/effort/
  precontext settings (master birth, fm:invocation) · reply/thread
  structure (part birth — threaded's first pixels). Each must be
  expressible as a material diff with an acceptance test before the
  schema settles. Not expressible ⇒ the joints are wrong.
- **Real-thing rendering, never a picture.** The Workshop renders the
  actual component via the actual interpreter on actual revisions
  (preview = wearing a candidate revision). A simulacrum with its own
  drawing code = guaranteed drift = the Figma-mirage failure.
- **Hard cutover per component.** Anatomy activates ⇒ the hand-written
  composer for that component is deleted in the same phase.
- **The echo bar holds** (the standing 52ms p95 discipline). If
  interpretation breaks it, anatomy compiles to a cached scene template
  invalidated on activation — a compile step, not a law change.
- **Structural change is an explicit recorded migration** (parent
  DIRECTION law, now load-bearing): a type-flip never silently migrates
  instance state; ephemeral interaction state (caret, in-flight drag)
  gets an explicit continuity ruling at contract time.
- **Agent-legibility from day one** (Sid's easiness bar): flat rows,
  closed vocabulary, refusal messages that teach — a format a small model
  edits correctly ("free for e.g a local 32b model or haiku"). Retrofit
  is expensive; it shapes the schema now.

## The three strata (Sid's correction — the settled frame)

"Implemented in code" ≠ "outside Softland." Three strata:

- **material** — in-land editable now, through the revision loop.
- **Softland code** — the land at zoom 100, not yet brought inside.
  Already *addressable* from in-land: commits and code are queryable
  material (git-spine, code-atoms families), and say→wish→session→commit
  is the commissioning bridge — indirect hands, same land. Zoom-100-inside
  upgrades commissioning to direct manipulation. The material↔code line is
  DESIGNED to move (the kernel-shrink gauge).
- **host floor** — genuinely outside: JVM/browser/GPU/OS · Rama platform
  internals (license-bound) · the rotating inviolate moment (whatever
  executes the current transition is trusted for it — the 07-27 "always
  one inviolate level," correctly a rotating point, not a standing layer)
  · the constitutional floor (secrets, spend, push authority — outside by
  law, not physics).

**This package moves ONE row: structure, stratum 2 → 1.** Pre-named walls
that REMAIN after it — known edges, never vanishing floors: behavior/verb
bodies (stratum 2, commissionable) · process/systems (stratum 2; the
multi-cascade terrain) · space/world nesting (its own lane) · the
meta-schema (the anatomy format itself) · zoom-100.

## Sequencing (Sid, ratified)

Build the Workshop at the material zoom → build Softland's surfaces from
inside at that zoom → bring zoom-100 (the in-land code editor) inside when
the need arises — *"we will build all the strange loopy loops."* The
ratchet: each zoom is brought inside using the zoom above it; once inside,
that level's edits never need the terminal again. The scheduler is
mechanical: the terminal-escape gauge **split by stratum** — after this
package, structure-escapes → 0; behavior-escapes persist BY DESIGN and
their count is the demand signal for zoom-100. (Proposed as a C1-instrument
refinement — extends the armed matter-room §8 falsifier; lands at the next
evidence review, BETS untouched today.)

## Gauges — "how do we measure easyness" (Sid)

Smallest correct edit (probe 2's measure) · smallest model that lands a
valid anatomy edit first try · intent→live-change seconds for Sid by hand
· floor-touch rate per shipped change, trending down · the escape gauge
split by stratum (above).

## Anti-spiral riders

The spiral's generator, named in-session: the material/code boundary lived
only in docs while Sid's mental model lived in the land — the same
question asked four times in one week (07-23 "is it all data?" · 07-26 "is
space an entity?" · 07-27 "can i talk about the entities?" · 07-30 "is
structure data?"). A question that recurs four times demands a standing
view:

- **The boundary is a standing rendered surface, three colors** — green
  material · amber Softland-code (commissionable today, in-land at
  zoom-100) · red host/constitutional floor. Seeds exist: the halo's
  "code-owned · floored" labels, why-this-pixel doctrine, probe 1 as the
  hand-made prototype. Exact scope decided at contract; minimum bar:
  anything condensed shows its stratum honestly.
- **Does-NOT-build travels with the package** — the pre-named walls above
  ride the contract and its gates, not a doc read once.

## Open at contract time

Preview scope (sandbox instance vs live ground wearing a candidate) ·
phase split under the phases-low ruling · anatomy row shape vs the OC
platform-check · the ephemeral-state continuity ruling · boundary-map
minimum scope.

## Only-Sid

The name (standing; his naming law above) · North · spend · docs-branch
push (never) · genuinely irreversible forks.
