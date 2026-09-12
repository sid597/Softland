---
name: atomize
description: The atomization lens — derive the atomic unit, block grammar, and spec for a NEW artifact family (code, images, data files, …) by reusing the family-invariant laws proven in the prose/transcript block round. Use when opening a spec round for an artifact family's atomic units, when asking "what is the atomic unit of X", or when mapping a new material type into the container kernel. NOT for benchmark work (SPEC ≠ BENCHMARK) and NOT for authoring/editing semantics through atoms.
---

# atomize — the artifact-atomization lens

Provenance: written 2026-07-09 by Fable from the FIRST full application — the
prose/transcript block round (spec room `spec-sense-line-fable-max`; artifacts:
`history/docs/sense-line-mvp/{GROUNDS,SPEC}.md`,
`block-kernel/CONTRACT.md`). **Amended same day after the SECOND application —
the code round** (session `code-atom-spec-round`; artifacts:
`build/sense-line-mvp/code-atom/{GROUNDS,SPEC,CONTRACT}.md`): one session ran
hand-pass → Sid rulings → SPEC → CONTRACT. Claims are marked **[R]** (receipt —
survived a round; R2 = confirmed again in the code round) or **[C]**
(conjecture). **AMEND THIS SKILL after every family round**, the way the
work-package skill amends per package. **Amended 2026-07-10 by Fable from both
families' BUILD packages** (code-atom close + block-kernel gate rounds, each
retro adversarially rechecked) — implementation receipts flow back into the
lens. Status: v1.1, N=2 (both families built and gated).

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

And its build-round companion: **classification is total and keyed on the
THING, not the code path.** When a family's spec classifies inputs (forms,
lanes, river/debris), the durable class record is a function of *what the
thing is* (`classify-event`), never of *which processing branch it took*
("did it produce a surface?"). Every ingested input gets exactly ONE durable
class row even when it yields no material, and the family's real-corpus
receipt asserts that completeness invariant against the DURABLE rows — never
in-process counters, which are exactly what lied. "The map must not lie,"
applied to class provenance. [R — block-kernel F3 + Round-2 Finding-1: two
instances of this one failure, once per code path — debris first inferred by
subtraction, then surfaceless-river events falling to a count-only branch
(printed 244/402 for a true 247/399).]

## The five-slot template — fill per family [R — held at the code round; the novum landed in slot 4 exactly as predicted]

| slot | question | prose/transcript answer [R] | clojure-code answer [R] |
|---|---|---|---|
| 1 · source identity | what is THE authoritative immutable raw + its version axis? | session jsonl events (immutable, no version axis) | git blob (text@commit, D-003); store raw + atoms (transcript pattern, Sid-ruled) |
| 2 · free cut | what producer grammar hands units over for free? | API typed parts + markdown structure | top-level forms, TWO LANES (code now · commentary deferred) |
| 3 · form vocabulary | the closed starting list of material types | thinking · prose-para · list-item · tool-use · … (SPEC §4.6) | head-symbol table `:clj/*` + project rows (defmodule, e/defn) |
| 4 · continuity rule | what persists across versions; how is lineage minted? | trivial (events immutable); born-native blocks supersede | var continuant + FORM-TEXT HASH (re-addressed vs superseded); renames silver |
| 5 · mechanical edge floor | which edges derive with near-certainty, zero interpretation? | write-tools → `produced` · read-tools → `grounds` | `:supersedes` (lineage) · `:requires`/`:calls` (analyzer, HEAD-scope, version-free asserters) |

The family's hard axis usually hides in slots 1 and 4 (code: the version
axis; the var as continuant). Expect one novum per family; find it early.

Code-round additions to the laws [R]:

- **Two-lane braid.** A family's container can braid world material with
  fossilized SENSE material (code files: comments — "the only place to put
  the why next to the code" before Softland). The free cut classes lanes
  mechanically; the sense lane defers at zero cost (strata are additive).
- **Content-hash at unit grain.** When the surface is coarser than the unit
  (file vs form), store a per-unit text hash — it splits *re-addressed*
  (surface changed elsewhere) from *superseded* (the unit changed). Without
  it lineage over-mints catastrophically (specimen: ~100 fake vs 6–8 real
  supersessions per commit).
- **Check asserter-versioning against retraction rights BEFORE contracting
  an edge deriver.** Version-in-asserter forks every edge id AND loses the
  right to retract stale edges (relation-kernel law: retract requires
  actor == stored asserter). Version-free actor, version rides `note`
  (git_spine precedent). The GROUNDS lean had this wrong; reading the
  driver precedent at source caught it.
- **Derived edges land at CONTINUANT grain with current-status semantics**;
  per-version detail rides evidence anchors. Instance-grain edge sets
  explode as pairs × versions.

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
  form-break evidence, not instinct. A family minting a NEW `imp:<family>:`
  import-key prefix registers its `extract-object-key` routing (or rides a
  handled prefix) and ships a foreign-read routing gate — the mis-route is
  latent until a foreign consumer calls `read-import-completion` on a
  multi-task cluster. [R2 — the identical bug bit BOTH families: `imp:clj:`
  (code-atom G-F2) and `imp:sense-block:` (block-kernel F2).]
- **Raw-form plurality resolves at the Material law.** [R2 — confirmed at
  the code round] When a family has rival representations (code: text vs
  AST vs runtime reflection), the authoritative immutable raw is ONE thing
  (for code: text@commit, git authority per D-003); the rivals are
  coexisting SEGMENTATION STRATA over it. Sharpened by Sid's AST question:
  within-unit containment is COMPUTED from spans (stored nowhere); only
  cross-references need storage; the AST itself is a transient parse.
- **Edge derivation is itself a distiller family.** [R — confirmed, with
  one correction (see the retraction-rights law above)] Mechanical edges
  are declared, re-runnable passes over atoms; re-runs CONVERGE on the
  same edge ids (version-free asserter) and retract what disappeared —
  strata live in notes + status history, not forked ids. Asserted edges
  (human tape) are the OTHER lane: never gated on atomization, any grain,
  any time.

## Family boundary notes

- **Code (Clojure-only for now — rama/missionary/webgpu; Electric left the tree 2026-08-20):** D-003 Regime 1
  ONLY — code as view, git authority, commit-boundary versioning. Atoms
  serve sense-making: reading, arranging, relating. Editing or recomposing
  code THROUGH atoms is Regime 2, gated on the self-hosting test. Cite
  D-003 narrowly; do not re-litigate it.

## Boot list for a family round

`history/docs/sense-line-model.md` fixed points (esp. #7:
labeling is emergence) · `build/sense-line-mvp/SPEC.md` (the generic laws +
the prose instance) · `docs/architecture/MAP.md` (inventory duty) · this
skill. Lazy-read everything else. Do NOT load decisions.md in full or old
batons — cite decisions narrowly when one binds.
