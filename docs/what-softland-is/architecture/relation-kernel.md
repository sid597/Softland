# Relation kernel

**Duty.** The record's edge substrate — addresses and edges are the only
glue between media. Edge rows carry the asserter inside their identity (a
human asserting and an LLM proposing the same edge are two facts); the
envelope actor is recorded separately from the payload asserter; retraction
custody stays with the asserter. Holds the closed relation vocabulary
(`:based-on`, `:grounds`, `:produced`, `:assembled-from`, `:confirms`,
`:refutes`, …) and holes as queryable absence — deterministic, never-minted
endpoint ids. Under A6, co-reference identity is the unordered endpoint pair
(the group); per-asserter assertions are strength inputs within it.

**Seams.** Consumes edges from [graph-economy](graph-economy.md)'s
producers. Serves moved-since and staleness selection
(`$$relations-by-target`, both endpoints indexed) to the staleness lens and
[debt-queue](debt-queue.md). The publish-time dark-closure check consumes
direct holes ([workshops-and-publication](workshops-and-publication.md));
the hole-resolution transition, when it exists, writes here.

**Existence: exists** — the deepest-built part. Edge identity is ordered and
asserter-scoped (`relation_kernel.clj:118-129`); the vocabulary is closed
(`:58-75`); envelope-actor and payload asserter are distinct (`:251-257`);
retraction is asserter-locked (`:459-466`). **Build-gaps:** no co-reference
kind exists in the closed vocabulary and no unordered group identity (Codex
F6; A6 sets the law). No hole resolution or retraction transition exists —
`hole-endpoint-id` is deliberately never minted (`block_distiller.clj:495-501`),
a later distill pairs to a *different* endpoint (`:503-509`), and the driver
only appends (`:912-919`) — so holes stick and the darkness discount never
lifts (Codex F4, cross-confirmed with Claude's sealed F3; exchange-03 R1's
"self-resolving" leg receipt-trail-corrected).

**Open.**
- The hole-resolution transition (exchange-05 §7). A4 names the law — every
  hole names its resolution owner; publication triggers that owner's
  append-only transition, the discount-lift trigger — but the matching
  machinery (which organ pairs a publish event with the outstanding holes
  naming it: publication act, resident sweep, or this kernel) is unowned
  design work; the re-parented-while-dark case breaks deterministic id
  matching (sealed F3).
- Co-reference grouping (Codex missing-organ row): the canonical group id
  beside per-asserter assertion ids. A6 settles attachment — the question
  attaches to the group, one seam per subject-pair; closure internals live
  with [debt-queue](debt-queue.md).

**Law.** [identity-law](../constitution/identity-law.md) ·
[consequence-modes](../constitution/consequence-modes.md) (A6) ·
[settlement-publication-custody](../constitution/settlement-publication-custody.md)
(holes) · [total-provenance](../constitution/total-provenance.md).

**Provenance.** [exchange-02](../provenance/exchange-02-adjudications.md),
multi-turn item 2 and one-shot items 4–5 (the edge vocabulary, asserter
custody, co-reference); [exchange-04](../provenance/exchange-04-meta-syntheses.md)
clauses 6, 8, 11; [exchange-05](../provenance/exchange-05-falsification.md)
§5 F4/F6, A4/A6, §7.
