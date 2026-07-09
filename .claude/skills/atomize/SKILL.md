---
name: atomize
description: The atomization lens — derive the atomic unit, block grammar, and spec for a NEW artifact family (code, images, data files, …) by reusing the family-invariant laws proven in the prose/transcript block round. Use when opening a spec round for an artifact family's atomic units, when asking "what is the atomic unit of X", or when mapping a new material type into the container kernel. NOT for benchmark work (SPEC ≠ BENCHMARK) and NOT for authoring/editing semantics through atoms.
---

# atomize — the artifact-atomization lens

Provenance: written 2026-07-09 by Fable from the FIRST full application — the
prose/transcript block round (spec room `spec-sense-line-fable-max`; artifacts:
`docs/current-mental-model/build/sense-line-mvp/{GROUNDS,SPEC}.md`,
`block-kernel/CONTRACT.md`). Claims are marked **[R]** (receipt — survived the
block round) or **[C]** (conjecture — awaiting the code round, this skill's
scheduled first test). **AMEND THIS SKILL after every family round**, the way
the work-package skill amends per package. Status: v0, N=1 — hold confidence
accordingly.

## The invariant laws [R — countersigned in SPEC v0; written family-generic]

1. **Material.** A unit is an address `(surface, span)` into immutable
   canonical raw — never a copy. Canonical = the producer's output after ONE
   declared, versioned redaction pass. Blocks are places, not things; minting
   costs a pointer, so total coverage is affordable.
2. **Boundary.** Segmentations are plural, declared (rule-id@version,
   producer), demand-refined, err-coarse. The FREE CUT = the producer's own
   structure (format/grammar) — cheap, total, boring. Semantic cuts are
   additional strata, never edits to the free cut. An undeclared chunk is
   torn paper.
3. **Identity.** Span-keyed at the material layer; same key resolves, never
   duplicates; overlap/nesting legal; containment COMPUTED, never stored
   paths. Identity is never path-dependent.
4. **Composition.** Every production event records context-parents; any
   assembly that gets used becomes new material with `assembled-from` edges;
   buildup ≠ breakdown⁻¹ (projection down, synthesis-that-cites up).

Plus the cross-cutting law: **form (material fact, producer-given) vs kind
(interpretation, proposed mark) — never merged.** [R — the five-field
molecule died of merging these, 2026-07-07.]

## The five-slot template — fill per family [C — minted in the code-braid discussion; untested]

| slot | question | prose/transcript answer [R] |
|---|---|---|
| 1 · source identity | what is THE authoritative immutable raw + its version axis? | session jsonl events (immutable, no version axis) |
| 2 · free cut | what producer grammar hands units over for free? | API typed parts + markdown structure |
| 3 · form vocabulary | the closed starting list of material types | thinking · prose-para · list-item · tool-use · … (SPEC §4.6) |
| 4 · continuity rule | what persists across versions; how is lineage minted? | trivial (events immutable); born-native blocks supersede |
| 5 · mechanical edge floor | which edges derive with near-certainty, zero interpretation? | write-tools → `produced` · read-tools → `grounds` |

The family's hard axis usually hides in slots 1 and 4 (code: the version
axis; the var as continuant). Expect one novum per family; find it early.

## The method [R unless marked]

- **Function determines grammar.** Name the CONSUMERS and their operations
  first; derive the unit from engagement, never from the material's
  aesthetics. (Prose: agree/disagree/pull-thread → the stance-flip seam.)
  Write the family's per-consumer break criteria as testable checks.
- **INVENTORY FIRST.** Read `docs/architecture/MAP.md` and the actual
  modules before proposing any placement or contract. [R — the 2026-07-09
  goose chase: a full contract was written for machinery that already
  existed (`object-container` derived units/anchors/distillers); Sid caught
  it; cost a placement re-ruling. A grep is not a read; a prior contract's
  characterization is not a read.]
- **Specimen discipline.** Hand-apply candidates to ONE real artifact;
  validate against an INDEPENDENT trace of lived engagement (block round:
  Sid's recorded in-flow reactions; code round candidate: a real
  reading/debugging trail such as MAP.md's own reading log).
- **Plural candidates; Sid rules; never converge-and-sell.** Names are
  scaffolding; recurrence earns them; the naming is Sid's.
- **SPEC ≠ BENCHMARK.** This lens produces schema-fit against reality. How a
  model performs against the spec is a different, later room.
- **Capture structure:** GROUNDS (warrant) · SPEC (yield) · specimens
  (evidence) · CONTRACT (downstream, worker models). Working docs update by
  direct replacement — git is the history; only ledger-genre docs append.
- **Existing-infra mapping.** [R] A new family lands as a new DISTILLER
  ADAPTER over `object-container-module` (precedents: `markdown-block-v0`,
  the sense-block distiller) + mechanical edges into `relation-kernel`
  driver-side with deterministic idempotency keys. New modules require
  form-break evidence, not instinct.
- **Raw-form plurality resolves at the Material law.** [C] When a family has
  rival representations (code: text vs AST vs runtime reflection), the
  authoritative immutable raw is ONE thing (for code: text@commit, git
  authority per D-003); the rivals are coexisting SEGMENTATION STRATA over
  it, each assigned to the consumers it serves, all sharing the anchor space.
- **Edge derivation is itself a distiller family.** [C — Sid's pipeline
  framing, 2026-07-09] Mechanical edges are versioned, declared, re-runnable
  passes over atoms — re-run a better deriver over the whole past and the
  log appreciates for edges too. Asserted edges (human tape) are the OTHER
  lane: never gated on atomization, any grain, any time.

## Family boundary notes

- **Code (Clojure-only for now — rama/electric/webgpu):** D-003 Regime 1
  ONLY — code as view, git authority, commit-boundary versioning. Atoms
  serve sense-making: reading, arranging, relating. Editing or recomposing
  code THROUGH atoms is Regime 2, gated on the self-hosting test. Cite
  D-003 narrowly; do not re-litigate it.

## Boot list for a family round

`docs/current-mental-model/sense-line-model.md` fixed points (esp. #7:
labeling is emergence) · `build/sense-line-mvp/SPEC.md` (the generic laws +
the prose instance) · `docs/architecture/MAP.md` (inventory duty) · this
skill. Lazy-read everything else. Do NOT load decisions.md in full or old
batons — cite decisions narrowly when one binds.
