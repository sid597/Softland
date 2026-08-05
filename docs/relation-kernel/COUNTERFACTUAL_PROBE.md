# D-006 criterion 2 — the counterfactual probe, RUN 2026-07-05

**Pre-registration** (decisions.md D-006, fixed 2026-07-03 before any work):
"Give Opus 4.8 (fresh session) the same input manifest (recorded with the
contract) and ask for the same contract. Diff the load-bearing choices. If
Opus independently makes the same calls, the bet loses." Input manifest:
relation-kernel CONTRACT §13. Probe question, verbatim as registered:
*"design the RelationEdge contract for this substrate."*

**Scoring below is PROPOSED — Sid weighs it at final D-006 evaluation.**

## Method

- One fresh Opus 4.8 subagent (2026-07-05, delivery session), blind: inputs
  copied into an isolated scratch directory; explicit prohibition on reading
  the repo/web; agent's tool-use count (8) matches the 8 input files; agent
  attested "No other source was read."
- Inputs reconstructed at PRE-CONTRACT git states: `object_container.clj` and
  `space.clj` excerpts at `052d9b5` (the commit before the kernel code
  landed); `decisions.md` D-001..D-006 from the earliest tracked docs commit
  (`84b3d82`) with two post-contract blocks STRIPPED — the D-004 "contract
  recon" amendment (leaks the placement ruling) and the D-006 running
  evaluation notes + Open-Questions idempotency ruling (leak scope/QC
  outcomes). `/rama` SKILL.md at current (untracked; no history exists).
- Output: `COUNTERFACTUAL_PROBE_CONTRACT.md` (same dir, verbatim).
- Cost ledger: ~47k subagent tokens, 9.4 min wall.

## Caveats (named before the diff, so the scoring can be discounted honestly)

1. **Post-implementation timing.** The probe ran 2 days after the real
   contract and after the kernel shipped. Blindness was enforced by isolation
   + instruction, not by time. A contamination path through the /rama skill
   (updated since) can't be fully excluded.
2. **Scaffolded ask.** The probe prompt enumerated 9 decision axes and asked
   for traps explicitly; the registered question was one line. This raises
   the floor on COMPLETENESS (don't credit the probe's coverage to the model
   alone) but does not hand it any specific CHOICE.
3. **Residual manifest leak:** D-006 criterion-3's text cites "CONTRACT.md
   §11" (reveals a section count, no design content).
4. **n = 1.** One sample of a stochastic process.

## The diff — load-bearing choices, real contract vs blind probe

| Choice | Real (CONTRACT.md / as built) | Probe (blind) | Verdict |
|---|---|---|---|
| **Placement** | NEW `relation-kernel-module`; D-004's "space kernel" read as role, override recorded as a D-004 amendment | NEW module, same name; same role-vs-file reading; **also** flagged the D-004 override for Sid | **SAME — the headline call matched, including the escalation instinct** |
| Asserter in identity | yes — `relation-id = sha1(kind, from, to, asserter)`; two actors = two edges | yes — identity tuple includes `asserter-key`; two actors = two edges | **SAME** |
| Deterministic content-addressed id as the idempotency foundation | yes | yes | **SAME** |
| Dangling endpoints legal (no existence check) | yes (trap 3 of the real ledger) | yes (T4, on D-003 grounds) | **SAME** |
| Retraction = status flip, never delete; per-relation-id | yes | yes (+ a `:sid` human override the real contract deliberately refused) | SAME principle, one divergent right |
| Colocated dual endpoint reads, ~2 seeks per neighborhood, query topology over client roundtrips | yes (`$$relations-by-target`, two copies per edge, descriptor/sort keys) | yes (two PStates out/in, full-row denormalized) | SAME shape, different factoring |
| **Depot envelope** | PLAIN MAP with namespaced keys + an explicit NO-defrecord note — the fence exists because plan-validation F1 caught a `RelationRequestRow` that couldn't route via `hash-by` | **`RelationRequestRow` defrecord + `(hash-by :routing/key)` — as written, a namespaced-keyword lookup on a record field named `routing-key` reads nil → every request funnels to one task. The EXACT F1 defect, uncaught** | **DIVERGED — into the known trap** |
| **Routing/colocation architecture** | route by relation-id (`:relation/routing-key`); all rows of one RELATION colocate on hash(relation-id); zero substrate touches | route by from-object-key embedded IN the relation-id; edge colocates with its FROM OBJECT; requires an `extract-object-key` "rel:" branch (substrate touch) | **DIFFERENT architecture** (both self-consistent; probe's couples relation identity to OC key derivation and touches the substrate) |
| **Idempotency scope** | relation-scoped `(relation-id, idempotency-key)` journal — the scope that needed a validation round + explicit ruling in the real cycle | from-object-scoped, TWO layers (idempotency + fingerprint dedup); the routing-⊆-identity scope invariant articulated unprompted and correctly | Same INSIGHT; different scope key + an extra mechanism |
| **Re-assert semantics** | re-assert with a fresh key = ACCEPTED transition (new event; note/confidence ride re-asserts) — "revisable judgments riding an immutable log" | re-assert = `:deduped`, NO event; mutations via a separate `:annotate-relation` request type | **DIFFERENT write semantics** (probe's dedup layer suppresses the transition history the real design treats as the point) |
| Unary marks (dead-end as node mark) | built in v1 (`:none` target, from-key inheritance, no nil-key hotspot) | REFUSED (T12), deferred on D-001 grounds | **DIFFERENT** — probe more conservative; the real contract's key-inheritance trick dissolves the nil-key objection T12 relies on |
| Traps ledger | 8+ traps, criterion-1 grade | 14 traps, most overlapping (UUID ids, routing ⊆ identity, retract-as-delete, global-PState hotspot, subindex, validation-PStates-not-truth) | Comparable — but prompted (caveat 2) |

## Scoring against the kill condition (PROPOSED)

**Not a clean loss, not a clean win; the evidence leans BET HOLDS, with one
honest ding.**

- **The ding:** the headline placement call — new module over D-004's literal
  "space kernel," with the same role-vs-file reasoning AND the same
  flag-for-Sid instinct — matched exactly. On the single most visible choice,
  Opus independently made Fable's call.
- **What did not match, and why it matters:** the three divergences are not
  style. (1) The probe's envelope reproduces, uncorrected, the precise defect
  class (F1) that the real cycle's validation layer caught before code — the
  real contract carries an explicit fence ("no RelationRequestRow; the
  partitioner-read key must be a top-level namespaced key on a plain map")
  BECAUSE a validator caught a plan making the probe's exact move. As
  written, the probe's depot decl mis-routes every request to one task.
  (2) Re-assert-as-dedup suppresses transition events — in tension with the
  revisable-judgment ground the substrate exists for (a dead-end flip should
  be an event, not an annotation route). (3) The id-embeds-object-key scheme
  buys elegance at the cost of a substrate touch and coupling relation
  identity to OC key derivation.
- **The honest frame:** the probe shows Opus produces a contract of
  comparable CRAFT (the scope invariant articulation in its §4 is
  excellent). What the D-006 window bought, on this evidence, is not craft —
  it is (a) the QC process wrapped around the text (the F1 fence exists
  because a validation round fired), and (b) grounding in context beyond any
  manifest (re-assert semantics follow from the trail-view's purpose, which
  the manifest only gestures at). Note honestly: (b) partially reflects
  manifest limits rather than model limits.
- **Corroborating same-day evidence, outside this probe:** the R-2 contract
  validation round (2026-07-05, `build/trail-room/CONTRACT_R2_VALIDATION_R1.md`)
  returned FAIL with two real blockers against a FABLE-authored contract —
  the value concentrates in fresh-context adversarial LAYERS, not in any
  single author's first pass, Fable's included. That cuts BOTH ways on D-006
  and belongs in the final weighing.

## Disposition

Criterion 2 is now RUN (it had been open since 2026-07-03). Verdict recording
goes to decisions.md D-006 evaluation notes (one paragraph, pointing here).
Sid may discount per the caveats or order a re-run at a cleaner point (e.g.
before the next Fable-authored contract, with no scaffolded axes).
