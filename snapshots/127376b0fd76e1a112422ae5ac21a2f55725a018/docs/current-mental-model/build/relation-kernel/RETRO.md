# Package Retro — relation-kernel-module (closed 2026-07-03)

Written at package close (Fable, same session as the gate review). Material:
the full NOW baton trail, all 7 phase artifacts, the module + test source, and
the gate review. This retro is the pre-registered INPUT to the work-package
succession skill (`.claude/skills/work-package/` — spec in decisions.md Open
Questions); write that skill in a fresh session FROM this file, not from the
raw trail.

## The package in one paragraph

Fable wrote CONTRACT.md (D-004) and opened the package with a STANDING/NOW
baton. Codex ran Phases 0–2 (implicit spec, plan, two plan-validation rounds —
both FAIL). Opus 4.8 revised the plan, escalated the one genuine policy fork
(F2 idempotency scope) per the stop clause; Fable ruled (relation-scoped);
Codex re-validated (PASS). Opus implemented (Phase 3), self-validated
adversarially (Phase 4: minor-fail → localized F1 read-path fix), wrote tests
(Phase 5), Fable adversarially validated them (Phase 6: minor-fail, 10 items),
Opus applied the fixes, Phase 7 ran green on the FIRST invocation (2 tests,
165 assertions, 0 failures, swept task counts 2/4/8). Fable gate review:
PASS. Total: one contract amendment (the F2 ruling), zero redesigns, zero
runtime debugging.

## QC-layer scorecard (what each layer caught / missed)

| Layer | Caught | Missed / cost |
|---|---|---|
| Contract (Fable) | 9-trap ledger pre-empted the classic failures (stream partial writes, UUID accretion, pointer indexes, nil-key hotspot) | Left idempotency SCOPE implicit — the one amendment (F2). Promised "1 seek" (§6) without requiring an explicit read plan — Phase 4 had to catch the divergence |
| Implicit spec (Codex, P0) | Entity×write matrix later powered Phase 6's gap hunt | **CREATED the F2 conflict**: modeled `IdempotencyKey` as a standalone entity in partition-free language, contradicting the contract's colocated journal. A derivation layer can introduce ambiguity, not just find it |
| Plan validation (Codex, P2 ×2) | Round 1: duplicate-key semantics drift, reassertion no-ops, N>M query reads. Round 2: `RelationRequestRow` defrecord that couldn't route via `hash-by` (an implementer invention BEYOND the contract — §4 already said map envelope), + surfaced F2 | Two rounds cost two sessions — but both were cheaper than either bug reaching code |
| Stop clause | Fired exactly once, on the only genuine policy fork; ruling unblocked in one round; no improvisation | — |
| Impl validation (Opus, P4) | The 14-seeks-vs-1 read-path divergence (F1) — correct rows, broken §6 promise | — |
| Test validation (Fable, P6) | 10 gaps, incl. the T9 vacuity hole: R2 returns empty history for a missing row, so "no stray status-log write" was UNPROVABLE through the public surface — needed a direct PState read | — |
| Run-to-green (P7) | Nothing — passed first try. That is the upstream layers' receipt, not luck | — |
| Gate (Fable) | 4 non-blocking doubts (envelope/payload binding is client trust; dead `replayed-from-decision-id` field; O(n²) R1 dedup; single-worker) + the git-binary NUL problem at commit time | — |

## What the next contract should do differently

1. **State the scope of every uniqueness/identity claim explicitly** —
   "idempotency key" without "(relation-scoped)" cost two validation rounds and
   a ruling. Any key, journal, or dedup mechanism in a partitioned store MUST
   name its partition scope in the contract sentence that introduces it.
   *(Recheck: cost overcounted — one round + the ruling; see correction 2.)*
2. **Style gates need boundaries**: "typed defrecords" over-applied to the wire
   produced the F1 plan bug. Write style gates as "records IN PStates, maps ON
   the wire" — name where the rule stops.
   *(Recheck: slogan looser than the as-built code; precise form in correction 4.)*
3. **Performance promises need a read-plan clause**: if the contract promises
   "1 seek", require the plan to enumerate the read plan per query shape, so
   the promise is checkable at Phase 2 instead of Phase 4.
   *(Recheck: causal story corrected — keep the clause as contract hygiene, not
   as a Phase-2 mover; see correction 3.)*
4. **Derived-spec language rule**: the implicit spec must inherit the
   contract's partition/colocation context; entity descriptions in
   partition-free prose are how F2-class conflicts are born.
5. **Negative invariants need a physical-read plan in the test design**: any
   "X must NOT have happened" assertion must name the PState-level reader that
   can actually see X. Public surfaces mask their own bugs (R1 dedup, R2
   row-gating) — this produced both the V1-reader discipline and T9.
6. **Keep the barrier deterministic**: `wait-for-microbatch-processed-count`
   with a submit!==+1 counting invariant (all requests carry present routing
   keys) is what made no-op replays PROVABLE. Polling can never prove a no-op
   ran. Reuse this harness shape for every future kernel.
7. **No raw control bytes in source literals**: spell NUL as the `\u0000`
   string escape from day one (git classifies raw-NUL files as binary → diffs
   die). Value-identical, tooling-safe.

## Mechanisms that earned their keep (keep verbatim in the skill)

- STANDING/NOW split with the **precedence rule** (baton never outranks
  contract/log) — it absorbed the "10 vs 11 gates" drift without a session
  even pausing.
- **Fresh-context validation phases with default-fail verdicts** — every
  validation layer found something real; author self-review never substituted
  (Phase 3's self-review was thorough and Phase 4 still found F1).
- **Stop clause with escalation path** — the one policy fork went to a ruling
  instead of an improvisation, and the ruling was executed across ALL binding
  artifacts (contract, spec, log) in one pass.
- **Pre-registered evaluation (D-006)** — the letter-vs-spirit scoring of the
  F2 amendment was recorded honestly BECAUSE the criterion existed before the
  work; without it the amendment would have been quietly absorbed.
- **Traps ledger in the contract** — Phase 3 cited trap numbers in code
  comments; the traps were live constraints, not documentation.

## Residue (tracked, not blocking)

- D-006 criterion 2 (counterfactual probe, CONTRACT §13 manifest) — unrun.
- `:workers 2` smoke test — cross-worker record serialization unexercised.
- Envelope/payload binding recheck — do before agent-authored writers (D-003).
- ~~`decisions.md` is not in git; commit decision is Sid's~~ — superseded the
  same day it was written: `84b3d82` (17:26) tracked the full docs tree
  including decisions.md. *(Recheck correction 1.)*
- *(Recheck addition)* **Importer timestamp discipline**: status-history order
  keys ride client-supplied `asserted-at-ms` (the price of deterministic
  replay), so an importer submitting out-of-order timestamps gets claimed-time
  order, not arrival order. Must be stated in the D-003 extractor / trail-view
  contract — the next package's consumer-facing obligation (from the gate's
  async-ordering note, which this residue list had dropped).

## Adversarial recheck (2026-07-03, fresh Fable session)

Pre-registered in the baton at package close: verify every claim above before
this file feeds the work-package skill. Method: every scorecard/narrative
claim traced to the seven phase artifacts, the baton trail in git (per-phase
commits on `next-prompt.md`), `decisions.md`, the module + test source, and a
fresh suite run THIS session on the committed code: `clojure -M:test` →
**2 tests, 165 assertions, 0 failures** — matching Phase 7, the gate re-run,
and the static count (151 `(is ...)` forms; T6's doseq×3 adds 14 at runtime).

**Verified intact:** the phase/actor/verdict narrative (the baton NOW log
corroborates every entry, including both FAIL rounds' findings near-verbatim);
the 9-trap ledger and its trap-number citations in code comments; the F2 story
end to end; the 14-seeks-vs-1 Phase-4 finding and its fix (the `[:all]`
whole-map plan, `relation_kernel.clj:444,639`); Phase 6's ten findings, all
present as applied testing blocks; the "10 vs 11 gates" absorption — baton
STANDING froze "10" at package open, and BOTH the Phase-2 re-run and the
Phase-7 DoD entries flagged and dismissed the drift inline citing the
precedence rule; Phase 7 first-invocation green plus the deterministic {2,4,8}
sweep; the NUL-as-string-escape re-spelling (`relation_kernel.clj:56,58`); the gate's
four open doubts.

**Corrections:**

1. Residue "decisions.md is not in git" — stale seven minutes after writing;
   struck above.
2. Lesson 1 overattributes: round 1's three findings (duplicate-key conflict
   decision, reassertion no-ops, N>M reads — baton verbatim) were unrelated to
   idempotency scope. The implicit scope cost part of round 2 plus the
   escalation and ruling — not two rounds.
3. Lesson 3's causal story is wrong for this instance. The final PLAN did
   enumerate read plans per query shape (the /rama Phase-2 template forces
   this — its N>M rule is what produced the descriptor PState in round 1), and
   Phase 2 validated them. The 14-vs-1 divergence was implementation-vs-plan,
   born in Phase 3; no contract clause moves that catch to Phase 2 — Phase 4
   is the layer that owns it, and it worked. Keep the read-plan clause as
   contract hygiene. The real lesson: **promises in binding docs don't
   self-enforce** — the implementer had both the "1 seek" promise and the
   ":all one-read" instruction in front of them and still diverged;
   fresh-context validation is the mechanism that catches it.
4. Lesson 2's slogan ("records IN PStates, maps ON the wire") is looser than
   the as-built code: the wire envelope carries a typed
   `RelationMutationPayload` INSIDE the map (PLAN-blessed; the partitioner
   never reads inside `:payload`). Precise rule for the skill: **the
   partitioner-read key must be a top-level namespaced key on a plain map
   envelope; typed records may ride inside fields the partitioner never
   reads.**

**Missing lessons (append to "what the next contract should do differently"):**

8. **Never overwrite a FAIL validation artifact.** Both PLAN_VALIDATION FAIL
   rounds were rewritten in place before docs were tracked; their full text is
   unrecoverable. The findings survive only because the baton NOW entries
   carried them near-verbatim — luck that must become a rule: per-round files
   (`*_R1.md`, `*_R2.md`) or a commit before rewrite, and the NOW entry always
   carries the finding list verbatim. The log is primary — applied to the
   package's own trail.
9. **A ruling amendment is a sweep, not a banner.** The F2 execution amended
   CONTRACT §5, added gate 11, and bannered IMPLICIT_SPEC — but IMPLICIT_SPEC's
   own gate enumeration still reads "all ten contract gates" (1–10, no gate
   11). Absorbed by precedence + PLAN carrying gate 11, but "executed across
   ALL binding artifacts in one pass" (above) is true only at banner level.
   Rule: after a ruling, grep every derived enumeration/count/list across the
   binding artifacts, not just the sentence that introduced the concept.

**Minor notes (no body change):** the {2,4,8} sweep was a Phase-7 scoped
`rand-nth` redef — not reproducible from the committed harness, which
randomizes per launch; make task count injectable when the harness is next
touched. PLAN's conditional `IRelationRequestPayload` was never defined (a
single payload record made it moot) — harmless plan-to-code drift, noted so
Phase 4's "no other divergence found" is exact. The module ships a polling
`await-relation` helper while lesson 6 rejects polling — not a contradiction
(the suite uses the deterministic barrier), but the skill should state: test
barriers are the deterministic counter, never the poll.
