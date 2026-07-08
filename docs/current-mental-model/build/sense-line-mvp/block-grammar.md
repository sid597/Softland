# Block Grammar — the sense-line's material layer (SPEC · Part 1)

2026-07-08 · SPEC ROOM (Sid + Fable, session `spec-sense-line`) · **status: LIVING v0**
— approved by default (D-010); Sid redlines anytime. Part 1 of the sense-line SPEC;
Part 2 (mark grammar — kinds + edge families) is next and, by design, **separable**
from this (B-4). All vocabulary is working scaffolding — names finalize by recurrence,
Sid names (sense-line-model fixed-point #7).

Boots from `sense-line-model.md` (the model) and `DIRECTION.md` §3.1 (the room).
The **Trail** (§2) carries the warrant — how each statement was earned, and the
nuances we caught in the river. This doc is itself a **buildup-with-provenance**
(B-12): read §1 as the gold, drill to §2 as the source.

---

## 0 · In one paragraph

A wall of text — an agent reply, a human message — is stored **once and immutable**.
Everything else is **addressing over it**, never a copy. A **block** is a span
(address + offset range) **plus the carving-spec that justifies its boundaries**: a
span without a spec is a meaningless *chunk*, useless to the sense line. Chunking is
**plural and open** — many carving-specs coexist as overlays on the same raw text, and
new ones emerge by use. v0 seeds three: **S0** (structural — the free, always-on floor:
provider content-blocks, markdown, human compose-units), **S2** (epistemic-kind — break
where the kind changes), **S3** (purpose/serves — break where intent shifts). Blocks are
**first-class and flat**; provenance is **edges, not nesting**; identity under reuse is
**transclude-by-default**. The whole structure stays safe under re-runs and new specs
because of one invariant: **marks and specs address raw spans, never block-IDs (B-4).**

---

## 1 · The spec (block layer only)

Statements are numbered so they are addressable and cross-referable — the same
gesture the spec is about, and the wall's own discipline (numbered panels).

**B-1 · Raw is immutable; a block is an address, never a copy.**
Every wall (message/reply) is stored once, unchanged. All structure over the river is
*addressing* — a span into a raw source — never duplication. Structuring is therefore
non-destructive by construction: the wall stays whole and readable (immersive mode)
while blocks are a queryable overlay (operational mode). (Address-don't-copy; D-003
Regime-1 lineage.)

**B-2 · A block is a span + the carving-spec that produced it.**
A bare span is a *chunk*. A **block** is a chunk plus the spec that justifies its
boundaries. The carving-spec is first-class provenance — every block records
`carved-by` which spec cut it. Without a spec a break carries no reasoning trail and is
**not admissible to the sense line**.

**B-3 · Chunking is plural and open.**
There is no single carving. The block layer is an **open family of carving-specs**,
each a lens over the same raw text; multiple carvings coexist as overlays. New specs may
be added at any time (emergence / iterative formalization). A candidate spec **earns its
place by the same gate a mark-kind does** — recurrence + the read-side test (does any
consumer ever read blocks carved this way?). Open at the top, gated by use: **plural
without mush.**

**B-4 · Addressing-layer separability — the invariant that makes plurality safe.**
Every carving-spec's output is **spans over the immutable raw text**, and marks/edges
address **raw spans, never block-IDs**. Therefore:
  (a) specs cannot corrupt one another;
  (b) a spec may freely consult labels, purpose, stance, or any signal as its boundary
      *criterion* — **label-driven chunking is legal** — because its output is still only
      spans;
  (c) re-running any spec, or a labeler that drives one, **re-projects** without breaking
      any other spec or any existing mark.
Separability is a property of the **addressing layer**, not a restriction on criteria.
This is what lets the log appreciate at the block layer: a better carver re-reads all
history and nothing downstream breaks.

**B-5 · S0 — structural: the always-on, authored, free floor.**
No model, lossless, present at write time: provider content-blocks
(`text` / `thinking` / `tool_use` / `tool_result`) and markdown structure for agent
output; the human's own discrete compose-units. S0 runs on **every** wall
unconditionally and guarantees **no wall is ever pure unstructured chunk** — there is
always at least the author's own reasoning trail. `tool_use` / `tool_result` are
first-class S0 blocks (the cleanest — provider-minted, atomic) and are the **seam to the
world-line**: a writing tool (`Edit`/`Write`) is an artifact-delta; a reading tool
(`Read`/`Grep`) is an observation.

**B-6 · S2 — epistemic-kind: proposed, label-driven.**
Break where the epistemic kind changes (claim / question / evidence / verdict / …).
Boundaries follow kind-transitions. Proposed (silver), model-run, lazy. This is the
**bridge to the mark grammar** — S2 uses Part 2's kinds as its cut criterion, which is
legal under B-4(b).

**B-7 · S3 — purpose/serves: proposed, teleological.**
Break where the purpose/intent shifts. The carving that makes purpose-chains — and their
*untested links* — visible; the graph-derivation showed this is exactly where a
dissolution becomes topology (an `opposes` landing at the end of an untested
`serves`-chain). Proposed, model-run, lazy.

*(S1 — stance/agreement, "break where a reader's stance could flip" — is a real
candidate carving-spec, deferred to emerge by use rather than seeded. Fixed-point #7.)*

**B-8 · Blocks nest; marks target any grain.**
Blocks exist at **every grain** — a fine block is span-contained in a coarser one; the
whole wall is the max-grain block. Marks/edges may attach at any grain. Consequence:
**stance may differ across grains simultaneously** — accept the whole message, reject one
claim inside it. "Addressing the bigger one" is simply operating at a coarse grain; no
special construct. (The UI surface this implies — navigation on two axes, **zoom** ×
**lens** — is handed to the DESIGN ROOM.)

**B-9 · Transclude by default.**
Lifting a block into a new context is **re-addressing, not copying**: the block keeps one
identity, appears in many places, carries its full provenance graph, and accrues
**context-independent marks per appearance**. A *new* block is minted only when new words
are authored — which then carry `based-on`/`references` edges to the sources it drew from.
(This is the "much more than a Roam block-ref": a dumb pointer becomes a first-class thing
that travels with its warrant and can be marked independently wherever it lands.)

**B-10 · Provenance is edges over flat blocks — never nesting depth.**
Blocks are top-level and addressable. Provenance is carried by **edges**, never by how
deeply something is nested. Core edges:
  - `authored-by` → actor (human / model id)
  - `in-response-to` → block(s): the **context DAG** — the parent chain, incl.
    agent-calls-agent
  - `part-of` → container **lens**: turn → session → … (demoted to one evidence-lens per
    amendment A1)
  - `produced-by` → an action/invocation
  - `instantiates` → action → spec (automations; **the spec is itself blocks**)
  - `carved-by` → block → carving-spec (per B-2)
The deep nesting tree (`human → spec → instance → msg → reply → block`) is a **projection
you compute by walking edges**, never the storage.

**B-11 · Actor is metadata; the block is symmetric, the mint is not.**
The substrate does not distinguish human from LLM authors — `authored-by` is metadata,
so **multiplayer is the substrate, not a feature** (human↔human and human↔LLM are the
identical shape). But **minting is asymmetric**: LLM output may be author-minted (S0) or
asked to self-mark; **human output is never *forced* to pre-chunk** — no ceremony. The
environment must make block-grain composition the **natural** human act (affordance, not
discipline) — a hard requirement handed to the DESIGN ROOM. The rule is not "human = one
block"; it is "the human is never forced to pre-chunk" (this very session's messages are
human walls that want reader-chunking — Exhibit A).

**B-12 · Breakdown and buildup are not inverses.**
Breakdown (wall → blocks) is a **lossless projection** — mechanical, cheap. Buildup
(selected blocks, possibly across sessions, + new authorship → a new wall) is a
**generative synthesis that cites**: the new wall's blocks carry provenance edges down to
their sources. The recursion is a **spiral** — new sense on top, a drillable line to
source below — and **buildup-with-provenance is the engine of the return path**: the
morning-answer, the weekly sense, the briefing are all buildups tuned to a target mind's
register.

---

## 2 · Trail — how each statement was earned (the warrant)

Keyed to the statements above. This is the deliberation the spec rode in on; kept so the
boundaries in §1 are drillable rather than asserted.

**Origin of B-2/B-3 — the singular-grain sin.** Fable first proposed *"the* grain rule":
break where a reader's stance could flip. Sid caught it — that is **one** carving, not the
grammar: *"why are we assuming there is only one to break a wall-of-text… what we need to
gate is… the different specs of breaking… without the specific spec it just makes it a
chunk which will not be useful for the sense line."* That correction is the spine of the
whole block layer: chunking is plural (B-3), and a break means nothing until a spec
justifies it (B-2). It is fixed-point #7 ("labeling is emergence") one layer down —
not only labels emerge, **carvings** emerge.

**B-4 — the apparent contradiction, resolved.** "Chunking and labeling are separable
stages" (DIRECTION §3.1) seems to forbid S2's label-driven chunking. It does not, and the
reconciliation is the load-bearing move: separability lives at the **addressing** layer
(everything is spans over immutable text; marks address raw spans), which **frees the
criterion layer completely**. A spec may cut *by* labels because its output is still just
spans — re-labeling re-projects, breaks nothing. Get this wrong (marks referencing
block-IDs) and every re-chunk orphans every mark, silently killing the log-appreciates
property. This is the same invariant that governs the mark layer one story up.

**B-5 — the floor resolves Sid's both-POV.** Sid: *"unstructured provides no reasoning
trail, and everything can't be structured and broken down upfront… we do need at least
one but I strongly believe in emergence."* S0 is the "at least one" that costs nothing —
authored structure is already there at write time — so no wall is ever pure chunk, while
S1..Sₙ stay emergent and lazy. Both POVs, one architecture. Tool blocks earned their
first-class status here too: a `tool_use` like `Edit` **is** the artifact-delta, the point
where a sense-line block touches the world-line — not noise to strip but the seam.

**B-6/B-7 — why these two seeded, S1 deferred.** S2 because it is the **bridge** to the
mark grammar and the best-evidenced (specimen-01's epistemic layer maps clean onto
Q/C/E/Source). S3 because it is the **one carving that made the dissolution visible** as
topology (graph-derivation §3). S1 (stance) is real but downstream of both and can earn in
later — seeding it now would be pre-speccing against Sid's own emergence principle (B-3).

**B-8 — the "bigger one" and the UI.** Sid: *"sometimes the thing we need to address is
the bigger one… i think its a ui question as well."* Answer: the bigger one is a
coarse-grain block; nesting is span-containment; marks attach at any grain — which is
precisely what carries his earlier *"agree with part, disagree with other, can or cannot
accept the artifact."* The UI crystallizes to two axes — **zoom** (grain, coarse↔fine)
and **lens** (which spec's carving you see). That is a DESIGN ROOM problem, named here,
not solved here.

**B-9 — transclude by default (Sid's ruling).** The block is an address (B-1), so lifting
is re-addressing. Roam gives transclusion as a dumb pointer; Rama lets the transcluded
block carry its whole provenance graph and accrue independent marks in each context — the
"we have Rama so we can do much more" made concrete.

**B-10 — flatten (Sid's own diagnosis).** Sid drew the maximalist nesting himself and
called it *"inefficient for querying and manipulating."* The fix is the same move as
B-4: don't nest, carry provenance as edges over flat first-class blocks; the tree is a
computed projection. This also answers the agent-calls-agent chain and automations
uniformly — an automation is an action that `instantiates` a spec, and the spec is itself
blocks, so *everything is blocks + typed edges*. Containers survive only as a lens
(amendment A1: the container trail demotes to one evidence-lens).

**B-11 — the symmetry and its governed exception.** Sid's intuition that human↔LLM is
structurally human↔human is right and load-bearing — multiplayer falls out for free. The
precision that keeps it from over-reaching: the *block* is symmetric but the *mint* is not
— forcing the human to pre-chunk is the ceremony the architecture is built to avoid. The
resolution is Sid's own future-POV: the environment affords block-grain, so humans
author-mint too, but as affordance, never discipline.

**B-12 — the spiral, and the strange loop.** Sid had seen the spiral before and named the
next turn: *"could also turn into strange loops."* It does — the sense-line about the work
is itself work with a sense-line; the block grammar must carve the very sessions that
produce block grammars (self-hosting at the sense layer; DIRECTION §7). specimen-01
already saw it: *"this whole chat is yet another example."* The operative payload is that
the loop is a **test harness**, not a curiosity (see §4).

---

## 3 · What this doc does NOT settle

- **Mark grammar** — kinds + the two-or-three edge families — is SPEC Part 2, next.
  Separable from this by B-4. (Watch there: whether `serves`/`invokes` force a **third**
  edge family beyond epistemic + process; the specimen forces them but their family is
  unruled.)
- **S1 (stance/agreement)** — a real candidate carving-spec; deferred to emerge by use
  (B-3 gate), not seeded.
- **The UI two axes (zoom × lens)** — handed to the DESIGN ROOM (B-8).
- **panproto** as schema notation — parked until we write the schema formally
  (github.com/panproto/panproto, panproto-toolkit; Sid's pointer). Olog used lightly where
  it helps (a spec = a carving/functor over raw text; provenance paths that should agree
  must **commute** = an honesty constraint), never as forced formalization.
- **Grounding vs current infra** — B-5's structural claims match how the API shapes
  replies; **not yet checked** against what transcript-ingest actually stores at block
  grain (DIRECTION §4.2 gap #2). A one-read verification, deferred until spec close per
  Sid ("close the current spec first").

## 4 · Self-test (the strange loop)

Cheapest possible validation of this spec: **carve THIS session under S0/S2/S3** and see
whether the resulting blocks make it navigable — does S3 surface the purpose-mutations,
does S2 find the claims/verdicts, does S0 give the free floor? The block grammar must
carve the sessions that produce block grammars. Noted as a fixture for the DESIGN /
BENCHMARK rooms, not run here (spec-first, per Sid).

## Lineage & preservation

Born of session `spec-sense-line` (2026-07-08), from the block-grammar inquiry braided
live with Sid. The chat's **yield** is minted here (§2); the **raw** river stays on disk
(`~/.claude/projects/-mnt-data-projects-Softland/*.jsonl`), harvestable later — the model
working exactly as designed: forward-capture the yield, past re-reads optional forever
(the log appreciates). That we hand-mint this today, because nothing yet carves the river
for us, is the very loop the MVP closes — this doc is one more **hand-cranked buildup**
awaiting its S0/S2/S3.
