# Code-Atom Spec Round — opening prompt (paste into a FRESH session)

Spec room for the **atomic unit of code**. Scope: **Clojure only** — rama,
electric, webgpu are all Clojure implementations; no other language (Sid,
2026-07-09).

Read, in order, before anything else:

1. `.claude/skills/atomize/SKILL.md` — the lens (invoke `/atomize`). Its
   [R]/[C] markers are honest: this round is the skill's first test — amend
   it at close.
2. `docs/current-mental-model/build/sense-line-mvp/SPEC.md` — the
   family-invariant laws + the prose instance (v0, countersigned).
3. `docs/architecture/MAP.md` — INVENTORY FIRST; you are a consumer of the
   modules named there, not a builder of rivals.
4. `docs/current-mental-model/decisions.md` **D-003 only, cited narrowly**:
   Regime 1 = code as view, git authority, commit-boundary. Atoms are for
   sense (reading · arranging · relating); editing code through atoms is
   Regime 2, gated on the self-hosting test. Do not load the rest of the log.

**Goal (definition of done):** `build/sense-line-mvp/code-atom/GROUNDS.md` +
the five-slot template filled for family=clojure-code — candidate atom
definitions hand-applied to ONE real file and checked against a lived
engagement trace; the strata plan with per-consumer assignments; the two
forks ruled by Sid or teed with leans. SPEC-section and CONTRACT come after
the hand-pass survives Sid.

**Candidates already on the table** (braid discussion, session
`spec-sense-line-fable-max`, 2026-07-09 — re-derive freely; don't anchor):

- Material atom = **the top-level form**. Two-layer identity:
  *form-instance* (surface = file@commit, span) vs **the var as continuant**
  (ns-qualified name = a durable natural key Clojure gives for free);
  lineage = supersedes edges between instances — mechanical on name-match,
  silver-asserted on rename/move.
- Raw-form plurality resolved at the Material law: **text@commit is THE raw**
  (git authority, D-003); AST · analyzer · runtime-reflection · codeq-style
  lineage are coexisting SEGMENTATION STRATA over it, not rival raws.
  Consumer → stratum sketch: pointing-and-talking → reader-cut (top-level
  forms, the free cut); callers/callees → analyzer-cut (clj-kondo analysis
  lens); live truth → runtime-cut (var reflection); cross-commit lineage →
  codeq-style cut (prior art: Hickey's codeq, "code quantum" — lens, never
  frame).
- Sid's per-consumer test list = this family's break criteria: pointing and
  talking · callers/callees context · LLM reads and searches · versioning ·
  collaboration · context switch.
- Fork 1 — source storage: cache git blobs (sha-keyed; git stays authority)
  vs re-store text like md sources. Lean: cache-with-git-authority. Sid rules.
- Fork 2 — dependency edges (requires/calls): material edges in the
  container kernel (CompositionEdgeRow kin) vs silver `references` relations
  in the relation kernel. Undecided; argue it on the specimen.
- Edge-derivation-as-distiller (Sid's pipeline framing): mechanical edge
  passes are versioned + re-runnable over atoms; asserted edges are the
  other lane, never gated on atomization.
- P0-verify grade (when contracting, not now): what git_spine actually
  stores today (docstring-grade: commit metadata + canonical commit text;
  file text likely ABSENT); rewrite-clj / tools.reader positional behavior;
  clj-kondo analysis output shape.

**Specimen:** Sid picks. Defaults: `src/app/server/rama/object_container/
markdown_adapter.clj` (small, self-contained) or
`src/app/server/rama/relation_kernel.clj` (module decl + records + history).
Independent lived-engagement trace: MAP.md's reading trail of
`object_container.clj` (2026-07-09) — a real code-sense-making episode whose
engagement grain is on record.

**Method:** /atomize discipline — plural candidates, hand-pass, Sid rules on
everything; SPEC ≠ BENCHMARK; inventory-first. Capture into
`build/sense-line-mvp/code-atom/GROUNDS.md` as the round runs (direct
replacement, no banners; ledgers append).

**Do NOT load:** next-prompt batons, decisions.md in full, the block
package's phase artifacts, or the authoring transcript
(`spec-sense-line-fable-max`, on disk — read a span only for a specific
nuance).

**Register:** working spec session, Sid + Fable close iteration; his in-flow
verdicts are gold; no work-queues beyond the next concrete iteration.
