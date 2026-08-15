# Basis and staleness

**Law.** "What was my basis, and what moved since, weighted by what I stand
on" is a query, not a virtue. Basis edges make it computable; change is
priced by reverse-dependency depth. Staleness is the normal condition of any
real concurrency; the system's job is to make it legible, not to eliminate
it.

**Why it binds.** At any real concurrency everyone's base is somewhat stale,
always — freshness is the special case that never occurs. Without basis
edges the query runs by hand, out of vigilance, at terrible attention cost:
retracted papers keep accruing citations for years because nobody knows what
stands on what. Load-bearing-ness computed from reverse-dependency depth is
the price tag on change — no more zombie citations.

**Enacted by.** [relation-kernel](../architecture/relation-kernel.md) —
directed basis edges · [graph-economy](../architecture/graph-economy.md) —
the producers that keep the graph non-empty ·
[debt-queue](../architecture/debt-queue.md) — stale-marks ·
[citizenship-contract](../architecture/citizenship-contract.md) — the served
staleness projection, nominated as the first coined lens.

**Related.** [consequence-modes](consequence-modes.md) (the exchange's
three-mode refinement of this clause) · [read-honesty](read-honesty.md) ·
[checkers-and-depth](checkers-and-depth.md).

**Provenance.** [Session 1, Part II](../provenance/what-should-softland-be-session-1.md),
consolidated clause 3; Part I clause 3. Converged at essay resolution,
2026-08-14.
