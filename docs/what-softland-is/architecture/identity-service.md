# Identity service

**Duty.** Mint and resolve the identity triple's durable leg. One durable,
medium-blind address per thing: declared-grain parts born at import or
authoring; previously unrepresented subparts demand-minted by addressing
acts, idempotent by (parent-address, medium-typed region-identity) — whether
this is the part's first naming or its hundredth, the same address comes
back. Hold anchors — one medium-typed link to the immediate parent — and
resolve chains only at resolution time. Occurrences (per-view, ephemeral)
resolve to the addresses this service owns.

**Seams.** Consumes mint requests from
[dispatch-and-courts](dispatch-and-courts.md) — the walk hands over the raw
material: which part-geometry was touched, in which parent. Consumes each
family's region-identity type, normalization, anchor type, re-anchor law,
and dangling policy from the
[citizenship-contract](citizenship-contract.md). Serves addresses to
[relation-kernel](relation-kernel.md) (edges and marks attach here),
[universal-verbs](universal-verbs.md) (go-to/reveal; the address-set),
[set-acts](set-acts.md), and [graph-economy](graph-economy.md).

**Existence: embryo.** The mint law runs for text: `refine-block-path` is
deterministic from (surface, sub-span) — "a re-refine of the same span
RESOLVES to the same id" (`block_distiller.clj:997`, `:1002`; re-verified in
exchange-05). Two-tier birth is already practiced: `free-cut-part` mints
declared-grain blocks at import while tool-result blocks mint lazily on
edge-demand (`block_distiller.clj:412-434`, `:464-468`; §5 F1 ruling). The
occurrence leg exists and is correctly ephemeral: `targets-by-address`,
incrementally maintained per view-instance (`selection.cljc:94-109`).
Missing: the service as one named organ; generic re-parenting is unverified
(Codex receipt audit: refinement verified narrowly, "does not verify generic
re-parenting"). The dangling-policy precedent (deterministic never-minted
hole ids) lives with [relation-kernel](relation-kernel.md).

**Open.**
- Address/anchor lifecycle (Codex missing-organ table, exchange-05 §3.5):
  who writes re-anchors; how (parent, region-identity) idempotency transfers
  across re-parenting; the hole-alias leg; the conflict winner for two
  re-anchor writers.
- Revision/cut snapshot service (same table; the record assigns no owner —
  placed here because resolution is this part's duty, flag stands):
  cross-source consistency of a pinned revision vector; who declares
  ambiguity; what invalidates a cut. The capture instant itself is settled
  at law — once, at invocation admission
  ([read-honesty](../constitution/read-honesty.md)).

**Law.** [identity-law](../constitution/identity-law.md);
[structural-guarantee](../constitution/structural-guarantee.md) supplies the
four questions every open item above must answer.

**Provenance.** [exchange-02](../provenance/exchange-02-adjudications.md),
multi-turn item 1 ("my kernel's fifth piece becomes an identity service")
and one-shot item 2 (who mints; refine-style requests after the walk);
[exchange-04](../provenance/exchange-04-meta-syntheses.md) clauses 1, 11;
[exchange-05](../provenance/exchange-05-falsification.md) A1, §3.5 rows 2–3.
