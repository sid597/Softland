# editable-material — Softland's components become Softland's material

2026-07-23 · landing of the editable-material sitting (Sid × Fable, direction
session; verbatim strands in `vision/LOG.md` 2026-07-23). **Status: DIRECTION
— adopted at Sid's word ("Then lets go???"); the provenance-facet probe is its
wear test.** decisions.md gets its bullet only after the probe wears (or at
Sid's word sooner). Answers the standing C1/C2 pull: "a tool to build the
tool — design, deploy and use it all at once all from softland."

## The question it answers

The components Sid inhabits (block, thread lane, world camera, anchor caret,
provisional projection) are hand-coded; his lived frictions (screenshots
2026-07-23: Tasks 1–11, the feedback zones) are addressing-, type-, and
arrangement-shaped, and nothing accumulates. What layer makes components
editable FROM INSIDE, so instance use informs the type and the type informs
every instance — without betraying team functional/concurrent/reactive?

## The layer

- **Type-objects.** A component type is a durable, addressable, revisioned
  material object holding exactly two strata + identity: an **arrangement
  stratum** (pure data, directly manipulable) and a **binding stratum**
  (gesture→verb mappings as descriptors — names are data, implementations
  are code). Nothing else lives inside it.
- **Records are queries, never containers.** A comment/friction/wish stays a
  block where it was born, edge-linked (relation kernel) to the instance it
  was felt at; instance edge-links to type. "Everything experienced around
  this type" is a standing query materialized at serve — the river-merge move
  repeated. Edges travel both ways (record item → originating instance).
- **Instances** = type-ref + content + local deviations (settled cells are
  the existing precedent) + their own attachments. Deviation → later "make
  this the type" is the promotion gesture.
- **Portals are deterministic projections.** Opening a type layer is a
  projection read — never an LLM interpretation pass. The resident works
  INSIDE a layer with the layer's own projection as briefing (same finger,
  same eyes). The portal is itself a component with a type-object in the
  layer it opens (the strange loop) — lawful only because the floor is dull:
  total render (error card, never black) + previous revision always wearable
  + a code-guaranteed floor rendering no data revision can break.

## Paradigm rulings (the type-system scare, resolved)

- **Describe, never gate.** THE GATE TEST: does this description ever refuse
  an instance? Then stop — that's a type system. Types here summarize the
  past (memoir), never gate the future (prophecy). An untyped block renders,
  forever. Carve-out: totality gates on the meta-material's OWN form (a
  malformed master error-cards) are the dull floor, not a violation.
- **Masters ≠ kinds — never fuse them** (CSS's sin). Masters are mechanical
  (which template draws me; ~1 per instance, birth-assigned). Kinds are
  semantic (#TASK/#Feedback — many per block, fluid, often query-shaped;
  hand-assignment is just one more predicate). Masters drive propagation;
  kinds drive gathering/briefing.
- **Precedent, not statute.** Instance repair → recurrence → scoped
  ratification → inheritance, reversible with record (the evolution law,
  unchanged). Prescription is a dial indexed to stranger-count: conformance
  LENSES (lint, rendered) may arrive with future inhabitants; refusal never.
- In-paradigm proof: masters are values, promotions are events, records are
  queries, kinds are predicates. More log vocabulary, not a new semantics.

## The facet architecture (mechanism in code, policy in material)

- **Entity** = address. **Facets** = data rows an entity wears (provenance,
  positioned, text-body, foldable, attention, threaded — all exist today as
  tangled keys in `ground.cljs`; facets are ONLY promoted from worn code,
  never invented as taxonomy). **Masters compose from facet-masters, flat —
  no hierarchy** (propagation runs along facets: improve `attention`, every
  wearer breathes; anti-fragile-base-class).
- **Render** = sum of facet contributions, each **stamped with its master**
  (the `src-path` discipline — non-optional; the pick must answer "who drew
  this pixel").
- **Interaction, four stations:** gesture (kernel, code) → pick/target
  (kernel, code) → **binding (material)** → verb implementation (code-lane
  registry; the action-descriptor registry already exists). ONE dispatch law:
  innermost picked entity whose facets claim the gesture wins; unclaimed
  falls through containment to the space; modifiers select among verbs at the
  same depth. Meanings multiply in data; the click abstraction never grows.
  "What does click mean, everywhere" = a served table face — the join is a
  query, not code archaeology.
- **Electric placement:** bindings/masters are served, revisioned material
  (the assembly epoch loop precedent — live rebind ≤1s); the 60Hz gesture
  path stays client-side in the scene runtime (settled gesture/truth split).
- **Kernel criterion:** mechanism in code, policy in material; the kernel
  (gesture detection, pick, containment math, total interpreter, log, the
  dispatch sentence) shrinks monotonically. **Kernel surface area over time
  is the self-hosting metric.**

## Guards (from the falsification pass, binding on the probe and after)

1. Facets promoted from worn code only — no up-front taxonomy.
2. Per-contribution provenance stamps, from the first extraction.
3. Ordering/precedence: declared priority on masters, deterministic
   tiebreak; conflicts render as lint IN the land, never a silent winner.
4. Perf: facet memoization; the 52ms echo bar is the standing gate — it
   regresses beyond repair, we stop.
5. Input safety: code-floor bindings (space pan/zoom, click-focus)
   unbreakable by any data revision — unbrickable input, unbrickable portal.
6. Migration is strangler-only, one facet at a time, each reversible;
   big-bang rewrite of the inhabited surface is forbidden; camera bindings
   go LAST.

## The probe (first act — one session's whole scope)

Extract the **provenance facet**: three render sites in `ground.cljs` today
(machine rail, fold-header tint, episode-boundary line — one `machine-tint`
constant). Deliver: provenance facet-master as served, revisioned material;
the block renders those sites through it; contributions stamped. **Pass:**
sites render byte-identical at current values; then ONE master revision moves
all three live; echo bar unchanged. **Doubles as:** first facet-master, first
type-object, first editable component material — three session threads, one
act. **Falsifiers:** after 2–3 extractions composition is harder to reason
about than the monolith was, or the echo bar regresses beyond memoization —
reverse (cheap), re-cut the seams; the direction, not just the code, reverts
to open.

## Relation to first-light

Genesis is LIT (Sid 07-23: "first light is past us its already well lit" —
genesis-day note in-land, Tasks 1–11 metabolized through the resident). A's
**metabolism gate stays OPEN**: repairs so far ride the code lane with
terminal-escape ≈ all — no wish objects, no preview membrane, no reversal.
The probe furnishes the material P3's wishes can repair as data; it is its
own small package, not a first-light phase.

## Only-Sid

Names ("facet", "master", "kind", "portal", "editable material") are working
scaffolding — names finalize by recurrence, Sid names. decisions.md entry =
after probe wear, or his word.
