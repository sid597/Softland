# Active work package: implement relation-kernel-module

This file has two sections with different rules:
- **STANDING** — the work package's contract-of-engagement. Do NOT edit or delete
  while this package is active; it is removed only when the package closes.
- **NOW** — the baton. Whoever runs the current session rewrites it freely at
  session end (model name, phase reached, artifact paths, next step). One
  writer at a time; it always reflects only the present.

---

## STANDING (keep until package closes)

- The decision log `docs/current-mental-model/decisions.md` is **binding**
  (D-001..D-006, CLOSED, countersigned by Sid). Do not reopen closed decisions;
  reopening requires evidence of a used form breaking.
- The design is decided: `docs/current-mental-model/build/relation-kernel/CONTRACT.md`
  is the Phase-0 input. Implement it; do not redesign it.
- Process: the `/rama` skill's full phased process — one phase per fresh
  session, each phase produces its artifact, validation phases emit PASS/FAIL
  and on FAIL the prior phase re-runs with the failure artifact as input.
- New files only: `src/app/server/rama/relation_kernel.clj` + test namespace.
  Do NOT edit existing kernels; only permitted dependency is plain helper fns
  from `app.server.rama.object-container` (`extract-object-key`,
  `fixed-width-order-key`, `actor-row`).
- Scope guard: CONTRACT.md §9 refusals (no FK validation, no confidence
  scores, no relation-as-container, no deletion). Do not add them.
- Verification duties (before code): check two contract claims against
  `.agents/skills/rama/references/` — (1) §5 microbatch fine print (mid-batch
  crash → batch replay; PState writes transactional per batch across tasks);
  (2) §7 query-topology idiom (per-key `|hash` fan-out, aggregate at
  `|origin`). Note findings in the phase artifact.
- Definition of done: all 10 acceptance gates in CONTRACT.md §11 green as IPC
  tests + style gates (typed defrecords, imported partition helpers,
  `{:allow-yield? true}` on unbounded range reads, consumers only via the two
  query topologies). Then Fable gate review.
- **Stop clause**: if the contract cannot be built as specified or is wrong
  under Rama semantics — STOP that thread, do not improvise around it. Record
  the problem under "Open questions" in decisions.md and in the phase
  artifact; it goes to Sid + a Fable session for amendment. Per D-006 this is
  also an evaluation result.
- Hard rules: commit only code files (`.clj` etc.); never commit `.md` or
  .gitignore changes; never read `src/app/server/env.clj`.
- **Precedence**: this file is a baton, not a source of truth. If it ever
  contradicts CONTRACT.md or decisions.md, those win — flag the discrepancy in
  the NOW section instead of following this file.
- After green gates (do NOT start without Sid): transcript→commit/doc join
  extractor + git-commit-metadata adapter (D-003 Regime-1 spine) feeding the
  27-04 trail view. Notes in D-003 and CONTRACT.md §12.

---

## NOW (rewrite freely at each session end)

- 2026-07-03, Fable: work package opened; CONTRACT.md written; handoff created.
- 2026-07-03, Codex: Rama Phase 0 completed. Artifact:
  `docs/current-mental-model/build/relation-kernel/IMPLICIT_SPEC.md`.
  No contract-breaking issue surfaced in Phase 0.
- 2026-07-03, Codex: Rama Phase 1 completed. Artifact:
  `docs/current-mental-model/build/relation-kernel/PLAN.md`.
  The two carried verification duties were checked against
  `.agents/skills/rama/references/` and recorded in PLAN.md:
  microbatch PState updates are exactly-once with cross-partition atomicity
  across a microbatch transaction; query topologies support dynamic `|hash`
  fan-out with final `|origin` aggregation. No implementation code or tests
  were written. The plan intentionally leaves Phase 2 watch items around the
  idempotency journal nesting/conflict behavior, accepted no-op status events,
  unary `:none` target-index shape, filtered query prefix seeks, and payload
  record split.
- 2026-07-03, Codex: Rama Phase 2 plan validation completed. Artifact:
  `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:fail`. No implementation code or tests were
  written. Blocking failures:
  1. PLAN.md changes duplicate idempotency-key semantics into a new rejected
     conflict decision, while CONTRACT.md and IMPLICIT_SPEC.md say duplicate
     idempotency keys replay the first decision and stop.
  2. PLAN.md treats accepted reassertions as decision-only no-ops, while the
     contract/implicit matrix require status/history evidence for
     reassertions.
  3. Filtered `relations-for-targets` and missing-id `relation-detail` examples
     have N > M read cases under the Rama query-validation template; the plan
     must either avoid those reads or explicitly stop for contract/skill
     adjudication.
- 2026-07-03, Codex: Rama Phase 1 plan revision completed after the failed
  Phase 2 validation. Artifact revised:
  `docs/current-mental-model/build/relation-kernel/PLAN.md`.
  No implementation code or tests were written. The revised plan addresses the
  three Phase 2 failures by:
  1. Making duplicate idempotency keys replay the stored decision and stop,
     without creating a separate rejected conflict decision.
  2. Treating accepted reassertions/repeated valid retractions as accepted
     status/history evidence, not decision-only no-ops, while preserving one
     target-visible relation membership.
  3. Adding a bounded `$$relation-target-descriptors` PState to descriptor-gate
     `relations-for-targets` range reads, and making `relation-detail` skip the
     status-history range read for missing relation ids.
- 2026-07-03, Codex: Rama Phase 2 plan validation completed against the revised
  `PLAN.md`. Artifact rewritten:
  `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:fail`. No implementation code or tests were
  written. The revised plan fixed the previous same-relation duplicate replay,
  accepted-reassertion history, and empty-range query-read failures, but two
  blockers remain:
  1. The planned typed `RelationRequestRow` field is `relation-routing-key`,
     while the depot is still `(hash-by :relation/routing-key)`. The plan does
     not prove the physical record can route to hash(relation-id), so the
     colocated journal/decision/row invariant is not safe.
  2. The plan scopes idempotency as `(relation-id, idempotency-key)`, while
     `IMPLICIT_SPEC.md` still says same idempotency key collapses to the first
     decision without stating that relation-id scope. This needs either a
     contract/implicit-spec amendment or a concrete global/caller-scoped
     idempotency mechanism.
- 2026-07-03, Claude (Opus 4.8): Rama Phase 1 plan revision after the 2nd
  `PHASE_VALIDATION:fail`. Split the two blockers by disposition. **F1
  (depot/request-row partition-key mismatch): FIXED in PLAN.md, no binding-doc
  change** — reverted the depot event type from the invented `RelationRequestRow`
  defrecord to CONTRACT §4's request map envelope (namespaced keys), which is the
  verified codebase idiom (map on the wire, defrecord only in PStates:
  `object_container.clj:1657-1686`, `:383`, `space.clj:171`). Dropped
  `RelationRequestRow`; added the partition-provenance test to carry into Phase 5.
  **F2 (idempotency scope): ESCALATED to Sid/Fable per the stop clause — NOT
  decided.** The binding CONTRACT (colocated relation-id journal, "copied from
  object-container's decisions-by-idempotency") and the binding IMPLICIT_SPEC
  (global-sounding `IdempotencyKey` entity) conflict, and they cannot both hold
  in Rama. Recorded with a high-confidence recommendation (relation-scoped) in
  `PLAN.md` → "Idempotency scope (ESCALATED)" and in `decisions.md` Open
  Questions (PROPOSED). No implementation code or tests written.
  *(This session's next-step — referral to Sid/Fable — was resolved by the Fable
  ruling immediately below; F2 is now CLOSED (A) relation-scoped. PLAN.md's
  "Idempotency scope" section is now titled RESOLVED. See Fable entry for the
  live next step.)*
- 2026-07-03, Claude (Opus 4.8) follow-up: after the ruling landed, aligned
  PLAN.md to it — F2 sections flipped ESCALATED→RESOLVED (no design change; the
  nested relation-scoped journal was already the ruled shape), and the plan now
  points at CONTRACT §11 gate 11 (cross-relation key reuse) for Phase 5.
- 2026-07-03, Fable: **RULED (A) relation-scoped** on Sid's referral. Executed:
  decisions.md Open Questions entry CLOSED with reasoning; CONTRACT §5 states
  the scope explicitly; CONTRACT §11 gained gate 11 (cross-relation key reuse:
  both relations succeed); IMPLICIT_SPEC has a binding amendment banner
  (every "same idempotency key" = "same (relation-id, idempotency-key)").
  No plan change required. D-006 evaluation notes updated with the honest
  letter-vs-spirit scoring of the contract amendment.
- 2026-07-03, Codex: Rama Phase 2 plan validation re-run completed against the
  current `PLAN.md`. Artifact rewritten:
  `docs/current-mental-model/build/relation-kernel/PLAN_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:pass`. No implementation code or tests were
  written. Confirmed:
  1. F1 routing is fixed: the depot event is CONTRACT §4's map envelope with
     top-level `:relation/routing-key`, so `(hash-by :relation/routing-key)`
     routes to hash(relation-id); typed defrecords are PState rows only.
  2. F2 scope is resolved: CONTRACT §5 + IMPLICIT_SPEC amendment make
     idempotency relation-scoped as `(relation-id, idempotency-key)`.
  3. CONTRACT §11 gate 11 is covered: same idempotency key reused across two
     different relations must let both relations succeed independently.
  4. Query, PState schema, microbatch, production-readiness, retro-lens, and
     implementation style gates all passed under Phase 2 validation.
  Note: STANDING still says "all 10 acceptance gates"; binding CONTRACT §11 now
  has 11 gates after the F2 ruling, and CONTRACT wins by this file's precedence
  rule.
- 2026-07-03, Claude (Opus 4.8): **Rama Phase 3: Implementation completed.**
  Artifact: `src/app/server/rama/relation_kernel.clj` (801 lines). No tests
  written (Phase 5). No existing kernels edited; the only object-container
  dependency is the two plain fns `extract-object-key` + `fixed-width-order-key`
  (used in client-side id/target-key derivation — `actor-row` was not needed
  since the row stores `asserter-actor-id`/`asserter-type` flat, so it was not
  imported; permitted-but-unused is fine).
  Implemented per PLAN.md: depot `(hash-by :relation/routing-key)` on the map
  envelope (F1); 7 PStates (`$$relation-decisions-by-idempotency` nested
  relation-scoped journal; `$$relation-decisions-by-id` + `$$relation-events-by-id`
  colocated via key-partitioner extracting relation-id; `$$relations-by-id`;
  `$$relation-status-log-by-relation`; `$$relations-by-target` full-row copies;
  `$$relation-target-descriptors` bounded R1 gate); ONE microbatch topology
  (journal gate via `filter> nil?` = replay-and-stop; pure `relation-outcome`;
  decision writes both indexes; accepted branch writes event/row/log then
  `|hash` to from/to target-keys writing copy + descriptor delta in one
  transaction); TWO query topologies (`relations-for-targets` = descriptor-gated
  `ops/explode`→`|hash`→prefix range read→`|origin`→`+vec-agg`→post-agg map with
  nil-seed for empty targets + relation-id dedup; `relation-detail` =
  single-partition `subselect MAP-VALS`, no aggregation); foreign client
  (runtime, append, two public reads with empty-input short-circuit, V1
  test-only PState readers, `await-relation` poll barrier).
  Trap coverage in-code (comments cite them): trap 1 (microbatch not stream),
  2 (deterministic `sha1` relation-id), 3 (no target FK check), 5 (asserter in
  identity), 7 (full-row `termval` copies, preserved `first-asserted-at-ms` →
  stable sort-key), 8 (unary `:none` inherits from-key).
  **Determinism note for the reviewer**: all key-affecting timestamps
  (`first-asserted-at-ms`, sort-keys, order-keys) come from the client-provided
  `:asserted-at-ms` payload field — NEVER `System/currentTimeMillis` — so batch
  replay is deterministic and index copies never orphan.
  Verification: `clojure -M -e "(require 'app.server.rama.relation-kernel)"`
  → `:COMPILE-OK` (Rama `defmodule` macroexpansion validated the full dataflow:
  partition alignment, cross-branch unification, batch/`|origin` structure).
  clj-kondo: **0 warnings**; 4 `error: Unresolved symbol: $$…`, all PState refs
  inside the two query topologies — the known Rama-macro-scoping false-positive
  class present in every committed kernel (`object_container.clj` = 4 + 26,
  `compute.clj` = 1 + 4). Recorded in `memory/implementation-quirks.md`. The
  Rama compiler is authoritative; these are not defects.
  Judgment calls left for the Phase-4 reviewer to confirm against the contract:
  (a) accepted reassert/retract-affirm PRESERVE the original row's note/evidence
  (conservative "status event, not attribute churn"); (b) unary `:none` writes
  BOTH `o:`/`i:` copies under the inherited target-key with R1 dedup (PLAN's
  chosen option); (c) blank idempotency-key → rejected decision journaled under
  a sentinel key (`missing-idempotency-key`), not dropped.
- 2026-07-03, Claude (Opus 4.8): **Rama Phase 4: Implementation Validation
  completed.** Artifact:
  `docs/current-mental-model/build/relation-kernel/IMPLEMENTATION_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:minor-fail`. No tests written; module NOT rewritten.
  Re-verified `:COMPILE-OK` independently this session. Adversarially traced all
  11 template checks, all 11 CONTRACT §11 gates, all 6 §9 refusals, partition
  alignment, the 7-write cross-partition atomic write, idempotency replay,
  retraction rights, descriptor gating, and unary `:none` — **all pass**. Three
  memory-derived idioms with no local precedent were verified against the Rama
  references (not assumed): `aggs/+vec-agg` (real, `query-topologies.md:89`);
  microbatch intra-batch read-your-writes (`pstate-schema.md:37` in-memory buffer
  + single-threaded task model); `term`+`partial` closure over dataflow vars
  (matches the `core.clj:614` `(term #(write-if-absent % *row))` idiom). The
  custom-key-partitioner colocation (`positive-partition` = clojure `hash` vs the
  depot's `hash-by`) is the identical hardened idiom object-container uses
  (`object_container.clj:1690`/`:347`/`:1778`).
  **Single finding — F1 (MINOR, localized, NOT contract-breaking → stop clause
  not triggered):** R1's no-kind-filter read path issues one `sorted-map-range`
  seek **per surviving (direction,kind) descriptor** (up to 2×7=14/target) instead
  of the single whole-map `MAP-VALS` read the PLAN specifies (L786-789 "one `:all`
  range-read descriptor") and CONTRACT §6 promises ("1 seek"). Correct rows, but an
  I/O divergence on the dominant "all relations touching X" query.
- 2026-07-03, Claude (Opus 4.8): **Rama Phase 3 (localized F1 fix) completed.**
  Applied ONLY the F1 fix (IMPLEMENTATION_VALIDATION.md §F1) to
  `src/app/server/rama/relation_kernel.clj`. No PState/topology/write-path/
  public-API change; no tests written. Two edits:
  1. `relation-read-ranges` (now ~L423-455) returns a 3-way plan instead of always
     one prefix pair per surviving group: `[]` (nothing survives → nil seed,
     unchanged), `[:all]` (no kind filter, ≥1 survives → whole-map marker), or
     `[[lo hi]...]` (kind filter → per-prefix, unchanged). Survivorship is one
     local `survives?` closure shared by both branches so they can't drift. Added
     `whole-map-plan?` predicate (`= :all (first ranges)`).
  2. R1 topology else-arm (now L639-651) nests `(<<if (whole-map-plan? *ranges) …)`:
     `[:all]` → ONE `[(keypath *target-key) MAP-VALS]` read (1 seek + sequential
     iteration = CONTRACT §6's "all relations touching X = 1 seek"); kind-filter
     path keeps the per-prefix `sorted-map-range` reads verbatim. `relation-visible?`
     (unchanged, L652) still filters retracted rows the whole-map read carries →
     identical visible set (the descriptor gate was only ever an I/O optimization,
     never the correctness filter). Falsified: [:all] vs [[lo hi]] plans are
     disjoint (keyword vs string-pair first elem); nil descriptor-map → [] → nil
     seed; partition alignment untouched (both reads follow the same `|hash *target-key`).
  Verified: `clojure -M -e "(require 'app.server.rama.relation-kernel)"` →
  `:COMPILE-OK` (defmodule macroexpansion validated the nested `<<if`: `*row`
  unifies across all three read paths). clj-kondo: **0 warnings**; 4
  `Unresolved symbol: $$…` errors, all inside the two query topologies — the known
  Rama-macro-scoping false-positive class (count stayed 4: clj-kondo dedups the
  second `$$relations-by-target` ref I added). Rama compiler is authoritative.
  **File-tooling note (superseded at commit time)**: `relation_kernel.clj`
  originally held two deliberate raw NUL bytes — `id-part-separator` (L58) and
  the `missing-idempotency-key` sentinel prefix (L56), the collision-proof
  "byte that can't appear in user data" idiom. At commit (2026-07-03, Fable)
  git classified the file as binary because of them, killing future diffs, so
  the raw bytes were re-spelled as `"\u0000"` string escapes — the runtime
  strings are PROVEN byte-identical (`(= sep (str (char 0)))` → true), so no
  relation-id changes; only the source encoding did. The separator VALUE
  remains NUL and must never change (that would rewrite every relation-id).
- 2026-07-03, Claude (Opus 4.8): **Rama Phase 5: Tests completed.** Artifact:
  `test/app/server/rama/relation_kernel_test.clj` (new file only; no existing test
  or kernel edited). Compile-checked, NOT run (Phase 7 runs it):
  `clojure -M:test -e "(require 'app.server.rama.relation-kernel-test :reload)"` →
  `:TEST-NS-LOAD-OK` (Clojure compiler resolved every symbol + re-macroexpanded the
  module; all three `deftest`s define). Every `rk/`/`rtest/` call-arity hand-verified
  against source (Clojure has no compile-time arity check). clj-kondo not on PATH.
  **Structure**: 3 deftests, each ONE IPC launch (`rand-nth [2 4 8]` tasks, 1 worker),
  disjoint keys, and a centralized `submit!`/`drain!` harness over a per-deftest
  append counter. **Barrier = `wait-for-microbatch-processed-count`** (not the module's
  `await-relation` poll): it counts *consumed* depot records incl. replays/rejections,
  so every `submit!` == +1 processed (all requests carry a present routing-key → none
  refused/ingress-dropped) and the cumulative count can't desync — which is what makes
  the *negative* invariants (no duplicate copy, asserted-count=1, replayed decision)
  provable. Polling can't prove a no-op replay ran.
  **Coverage**: all 11 CONTRACT §11 gates + G1 (pause/resume forces both same-relation
  requests into ONE microbatch → intra-batch read-your-writes) + G2 (partition
  provenance via the V1 `read-decision-by-id`/`read-event-by-id`, which route by the
  custom key-partitioner = clojure hash of rel-id; a hit proves `clojure hash ≡ rama
  hash-by mod N`) + F1a/F1b (folded into the gate-10 block: no-filter whole-map path
  returns ≥2 kinds; kind-filter per-prefix path isolates) + 5 IMPLICIT_SPEC edges
  (missing idem-key→sentinel journal, retract-absent→`:relation/absent`, non-unary
  `:none`→`:relation/to-malformed`, empty/unknown/duplicate R1 input + malformed R2
  id, retracted→asserted lifecycle with underflow-guarded descriptor).
  **V1-vs-R1/R2 discipline** (deliberate, per baton): physical copy-count / descriptor
  / provenance assertions use the V1 readers because R1 dedups by relation-id and would
  MASK a duplicate-copy bug — this is exactly why the baton specified "one index entry
  per direction," not "one row from R1." Product-surface assertions (status, history,
  visibility, dual-read, kind filter) go through R1/R2.
  **Judgment calls for the Phase-6 reviewer to confirm against contract/impl:**
  (a) Gate-3 history=2 and E5 history=3 depend on the module recording no-op-status
  reassertions/retract-affirms as ordered status EVENTS — IMPLICIT_SPEC explicitly
  permits this ("may add audit/status history for a reassertion") and Phase-3 note (a)
  confirms the impl does it; tests match impl-as-built + the permitted reading, not a
  redesign. (b) Single worker: cross-worker serialization of the RelationEdge
  defrecords (the prod `|hash` copy hops) is NOT exercised — matches the codebase's
  compute/transcript convention; a `:workers 2` smoke test is a cheap follow-up if the
  gate wants it. (c) `extract-object-key` derivation checked against source
  (`oc:doc:` strips prefix; `:git-commit` → sha verbatim; `:else` verbatim) → all test
  target-ids yield non-blank, distinct keys. (d) Cumulative-count-per-deftest means a
  genuinely stuck record would fail *subsequent* blocks in the same deftest (loud, not
  silent) — a diagnosis property, not a correctness defect.
- 2026-07-03, Claude (Fable 5): **Rama Phase 6: Test Validation completed.**
  Artifact: `docs/current-mental-model/build/relation-kernel/TEST_VALIDATION.md`.
  Verdict: `PHASE_VALIDATION:minor-fail`. No tests run; test and module NOT edited.
  Every check walked with per-block citations. Independently verified this session
  (jar introspection + skill references, not Phase 5's word): the 5-arity
  `wait-for-microbatch-processed-count` exists and THROWS on timeout
  (`testing.md:106`); count is cumulative per launch (`:120`); pause blocks until
  the in-flight batch completes and the next batch contains ALL records appended
  while paused (`:136-139`) — so G1's same-batch forcing is by construction;
  `extract-object-key`/`fixed-width-order-key` behave exactly as the test keys
  assume (`object_container.clj:313,238`); test ns loads clean (`:TEST-NS-LOAD-OK`,
  re-run fresh). The baton's four scrutiny items all VERIFIED: barrier guarantees
  cross-partition copy visibility (microbatch = one cross-partition transaction;
  canonical assert-after-wait idiom); submit!==+1 airtight (all 27 requests carry
  present routing-keys; counter increments only after append returns; count tracks
  consumed records, not writes); G1's V1 assertions falsify broken intra-batch
  read-your-writes (different timestamps → divergent sort-keys → 2nd physical copy
  → count!=1; R1 would mask it); G2 falsifies a broken key-partitioner across ≥6
  relation-ids × randomized task counts.
  **Failures (all line-level, in the existing ns/harness): T1** deftests 1 vs 3
  split has no named shared-state justification (deftest 2's pause!/resume!
  isolation ACCEPTED) → merge dt3 into dt1. **T9** gate 6 is partial: no direct
  check that `$$relation-status-log-by-relation` stayed untouched; R2's empty
  history is row-gated (module L664-676) so it can't see a stray log write under a
  never-created rel-id (gate 5's history=1 covers only the existing-row case).
  **T2-T8, T10** IMPLICIT_SPEC matrix rows untested: multi-asserter mixed retract
  (also the §9-refusal-5 probe), retract-affirm (impl arm `count-deltas` L267 never
  reached), duplicate-retract same key + rejected-decision same-key replay,
  unregistered-kind retract, missing request-id/actor + malformed-from rejections,
  R1 nil-input, kinds-filter × include-retracted survivorship branches, and
  evidence/note round-trip + full row-agreement across endpoints/detail. Exact fix
  list with spec/line citations: TEST_VALIDATION.md "Fix list for Phase 5" (10 items).
- 2026-07-03, Claude (Opus 4.8): **Rama Phase 5 (fix round) completed.** Applied
  all 10 TEST_VALIDATION.md items to `test/app/server/rama/relation_kernel_test.clj`
  ONLY; module untouched. Compile-checked (`:TEST-NS-LOAD-OK`), NOT run (Phase 7).
  **T1**: 3 deftests → 2. dt1 renamed `relation-kernel-write-and-read-test` now owns
  all assert/retract/reject/read blocks (20 blocks, disjoint keys, one cumulative-count
  barrier); dt2 `…-idempotency-and-partition-test` kept separate on the pause-isolation
  blast-radius argument. Harness/barrier/V1 discipline unchanged.
  Additive coverage, each pinned to an IMPLICIT_SPEC matrix row: **T2** retract one of
  two co-resident asserters (also §9-refusal-5 no-cascade); **T3** retract-affirm (2nd
  retract, new key, on a retracted relation — impl records a no-op status event, so
  history=3); **T4a** duplicate retract same key (replay, history stays 2) + **T4b**
  resubmit Gate 5's rejected retract (rejected decision replays — proves the journal
  stores rejected decisions too, module L570-575 write decisions before the accepted
  `<<if`); **T5** unregistered-kind retract; **T6** shape sweep (missing request-id /
  missing actor id / malformed-`:none`-from) with reasons pinned to `request-shape-errors`
  `cond->` order; **T7** nil R1 input; **T8** kinds-filter × survivorship on the
  retracted Gate-4 relation (`[]` when asserted-count 0, `[:retracted]` when total 1);
  **T9** direct read of `$$relation-status-log-by-relation` on every rejected relation
  (R2 returns empty history for a nil row → vacuous; needed a real PState read); **T10**
  evidence/note round-trip + full-record equality `(= from-copy to-copy by-id-row)` in
  Gate 7. Also softened e5's overstated "underflow-guarded" message.
  **One test-ns require added** (NOT a module edit): `[com.rpl.rama :refer [foreign-select]]`
  + `[com.rpl.rama.path :refer [keypath ALL]]` — T9 needs a direct subindexed read
  (`[(keypath rel-id) ALL]`, mirroring the module's own `read-target-index` idiom) because
  the module exposes no V1 wrapper for the status log and Phase 5 may not edit the module;
  IMPLICIT_SPEC V1 blesses direct PState inspection in tests. Verified against source
  before writing: every `rk/`/`rtest/` call arity (Clojure has no compile-time arity
  check); the exact rejection-reason keywords from `request-shape-errors`/`relation-outcome`;
  `oc/extract-object-key` + `oc/fixed-width-order-key` behavior on the edge inputs;
  sibling `object_container_test.clj` import namespaces for the Rama client fns.
- 2026-07-03, Claude (Opus 4.8): **Rama Phase 7: run-to-green completed.**
  Verdict: `PHASE_VALIDATION:pass`. No module or test edits — the Phase-5 fix
  round's 10 items were already applied, so the phase-7 pre-loop was a no-op and
  the suite passed on the FIRST invocation. Driver: `clojure -M:test` running
  `clojure.test/run-tests` on `app.server.rama.relation-kernel-test` (the `:test`
  alias has no `-X` runner, so tests are driven explicitly via `-e`/a script
  file). Result: **Ran 2 tests, 165 assertions, 0 failures, 0 errors.**
  Then a robustness sweep forcing the launch task count to EACH of {2,4,8}
  deterministically (redef of `rand-nth` scoped to the literal `[2 4 8]`
  collection only; every other call delegates to the original, so Rama-internal
  randomness is untouched, and the module is deterministic so this only pins the
  partition count): all three configs green — 165 assertions each, aggregate
  fail=0 error=0. This empirically discharges the partition-alignment / G2
  provenance risk (`clojure hash ≡ rama hash-by mod N`) across every task count
  the harness randomizes over — the empirical complement to Phase 4's static
  alignment trace. Logs (uncommitted): scratchpad `phase7-run1.log`,
  `phase7-sweep.log`. The two new code files
  (`src/app/server/rama/relation_kernel.clj`,
  `test/app/server/rama/relation_kernel_test.clj`) remain **UNCOMMITTED** — not
  yet asked to commit.
- **Definition-of-done status**: CONTRACT §11 gates (11 after the F2 ruling —
  STANDING's "10" predates gate 11; CONTRACT wins by the precedence rule) all
  green as IPC tests; style gates satisfied per Phase 4/6. The one remaining DoD
  item is the **Fable gate review** (STANDING).
- 2026-07-03, Claude (Fable 5): **Fable gate review completed. Verdict: PASS.**
  Artifact: `docs/current-mental-model/build/relation-kernel/GATE_REVIEW.md`.
  Suite independently re-run this session: 2 tests, 165 assertions, 0 failures,
  0 errors. Traps 1/2/7 spot-checked in the diff (NUL id-separator
  byte-verified); CLAUDE.md falsification pass run — 9 failure modes attempted,
  none landed; 4 non-blocking open doubts recorded in the artifact (topology
  never rechecks routing-key==relation-id-for(payload) — client trust, cheap
  hardening before agent writers; dead `replayed-from-decision-id` field;
  O(n²) R1 post-agg dedup; single-worker tests). D-006 evaluation notes +
  Fable-window queue updated in decisions.md. **All STANDING definition-of-done
  items are now green.** Module + tests remain UNCOMMITTED.
- Next (**Sid decides, in order**): (1) commit the two code files
  (`src/app/server/rama/relation_kernel.clj`,
  `test/app/server/rama/relation_kernel_test.clj`) — code only, never the .md
  artifacts; (2) close the work package → run the package retro (NOW log +
  phase artifacts → lessons to implementation-quirks / operating-model / D-006
  notes; it is the INPUT to the succession-document skill per decisions.md);
  (3) the D-003 Regime-1 spine (transcript→commit/doc join extractor +
  git-commit-metadata adapter feeding the 27-04 trail view) — **do NOT start
  without Sid** (STANDING). Optional slot-anywhere items: D-006 criterion-2
  counterfactual probe (fresh Opus, CONTRACT §13 manifest); `:workers 2` smoke
  test.
