# Trail-View WP1 — PLAN VALIDATION, Round 1

Status: QC-layer-2 artifact (Opus 4.8, effort max, fresh context, 2026-07-04).
Default-fail scenario-trace of `build/trail-view/PLAN.md` against
`build/trail-view/CONTRACT.md` v1.1 (BINDING) + `IMPLICIT_SPEC.md` (incl. its
§7 post-Phase-0 resolutions) + the on-disk code and Rama references. Per
`.claude/skills/work-package/SKILL.md` layer 2: PASS only after explicit
scenario tracing with citations; the verdict is FAIL until affirmatively earned.
This is a per-round file — never overwritten (cycle-1 lost both FAIL rounds;
only the baton saved them).

## VERDICT: **PASS** (with recorded non-blocking advisories + two open doubts)

Every load-bearing citation and mechanic in PLAN.md was traced to source and
verified correct. No correctness defect, no citation failure, no silently
resolved binding-doc conflict. The plan is safe to execute phase-by-phase as
written (A1 → A2 → A3 → B). The advisories below are refinements the implementer
absorbs naturally (none changes a design decision or prevents a bug); the two
open doubts are properly managed by the plan itself (a pre-specified fallback and
a §10 escalation trigger), so neither blocks the phase.

Contrast with cycle-1 (relation-kernel), where R1 and R2 both FAILED on real
correctness bugs (a `RelationRequestRow` that couldn't route; a 14-seeks-vs-1
read divergence). This plan is markedly more mature at the same stage — it
resolved its own hard parts (F-1…F-8), pre-traced its §9 risks, honestly recorded
its §10 open items, and ran a live spike to de-risk the one platform uncertainty.
That maturity is expected: it is the second package, written with the cycle-1
lessons encoded in the `/rama` + `/work-package` skills.

---

## 1. Method (what was actually verified, and how)

- **`relation_kernel.clj` (829 lines) — read IN FULL by this session.** Every
  Phase-A edit-site citation checked against the actual lines; the accepted-branch
  control flow and the 4th-hop write insertion traced by hand.
- **`object_container.clj` (2,648 lines) + adapters + `object_container_test.clj`
  — verified by an Opus subagent** reading the exact cited ranges (mirror PStates,
  ops module, `extract-object-key` routing, material queries + arg vectors, now-ms
  stamps, adapter offset units, record join-fields, test idioms). 13 claim groups,
  all MATCH (two cosmetic in-window line-label swaps).
- **Rama references (both trees) — verified by an Opus subagent.** 13 line
  citations (`mirrors.md` skill tree; `13-`/`18-` docs tree), all MATCH; the three
  load-bearing semantic questions answered from the doc text.
- **This session's own greps + compile check:** no direct `->Relation*Row`
  constructor in the test ns; no second hard-coded kind enumeration anywhere in
  `src/`+`test/`; the existing tests' `:actor` usage analyzed for the custody
  arity-change blast radius; kernel ns loads clean.
- **Reference-tree hazard ruled out:** `13-`/`18-` exist only under
  `docs/reference/rama/` (345/245 lines); `mirrors.md` only under the skill refs
  (201 lines). Every PLAN line citation is in range of its single existing copy —
  no wrong-copy ambiguity.

---

## 2. `relation_kernel.clj` edit-site citations — all verified EXACT

| PLAN cite | Claim | Actual | Verdict |
|---|---|---|---|
| §2.1 `:178-180` | `RelationDecisionRow` shape (12 fields) | 178-180, 12 fields | ✅ |
| §2.1 `:182-185` | `RelationEventRow` (16 fields) | 182-185 | ✅ |
| §2.1 `:187-190` | `RelationEdgeRow` (14 fields) | 187-190 | ✅ |
| §2.1 | `RelationStatusLogRow`/`…DescriptorRow` unchanged | 192-198 | ✅ |
| §2.2 `:314-395` | `relation-outcome` span | 314-395 | ✅ |
| §2.2 `:325/:326` | binds `actor` / `actor-id` | `:325 actor`, `:326 (:actor/id actor)` | ✅ exact |
| §2.2 `:309-312` | `rejected-decision-row` helper | 309-312 | ✅ |
| §2.2 `:345/:351/:358` | 3 call sites of that helper | 345, 351, 358 | ✅ exact |
| §2.2 `:383-384` | accepted `->RelationDecisionRow` | 383-384 | ✅ |
| §2.2 `:373-376` | `->RelationEventRow` | 373-376 | ✅ |
| §2.2 `:271-286` | `transition-row`; new-row `:284-286`, assoc `:278-283` | assoc 279-283, new-row 284-286 | ✅ |
| §2.2 `:371` | `transition-row` call site | 371 | ✅ |
| §2.2 `:288-297` / `:610,:618` | `endpoint-copy`; copy write hops | 288-297; 610, 618 | ✅ exact |
| §3 `:46-47` | `relation-kinds` set (7 kinds) | 46-47 | ✅ |
| §3 `:210` | `registered-kind?` = `(contains? relation-kinds kind)` | 210 | ✅ exact |
| §3 `:438-453` | R1 kind filter is descriptor-driven, not a literal list | `relation-read-ranges` 423-453; filter at 449-453 keys on `(:relation-kind desc)` | ✅ |
| §4.4 `:336` | `ts = (long (or (:asserted-at-ms payload) 0))` | 336 | ✅ exact |
| §4.4 `:106` | `event-id-for` embeds relation-id + order-key | 106 | ✅ |
| §4.5 `:579` | accepted branch `<<if (outcome-accepted? *outcome)` | 579 | ✅ |
| §4.5 `:562-563` | journal dedup `(filter> (nil? *prior-decision))` | 562 | ✅ (one line) |
| §4.5 `:617-621` | to-side copy (insert 4th hop after) | 617-621 | ✅ |
| §4.7 `:739-756` / `:792-816` | runtime handles / V1 readers | 739-756 / 792-816 | ✅ |
| §4.3 `:705-718` / `:714` | `envelope` fn / actor default-to-asserter | 705-718 / 714 | ✅ exact |
| §4.3 `:720-734` / `:200-206` | assert/retract-request spread opts / accessors | 720-734 / 201-206 | ✅ |

Minor citation imprecision (non-blocking): §4.4 says `event-id` is "already at
`:373`"; it is actually **bound at `:370`** and *used* at 373. `event-id` is in
scope in the accepted branch either way — substantively fine.

---

## 3. Scenario traces (the §9 risks, traced hardest — all hold)

### 3.1 Gate 9 — activity write is replay-deterministic and duplicate/reject-free
- **No wall clock on the accepted path.** `relation-outcome` derives `ts` from
  `:asserted-at-ms` (`:336`); the plan's `arrival-at-ms` from the envelope
  `:request/sent-at-ms` (fallback `:asserted-at-ms`); `bucket`/`order-key` from
  those. Verified the **entire relation kernel is now-ms-free** — even the decision
  row's `decided-at-ms` uses `ts` (`:384`), *not* `(core/now-ms)`. (Contrast OC,
  which stamps `(core/now-ms)` at `object_container.clj:428/448/467` — the
  anti-pattern trap 4b names. Subagent confirmed all three.) A crash-replayed
  microbatch re-derives byte-identical rows → the `(keypath bucket order-key)`
  write with `(termval *activity-row)` (write-only) is idempotent per attempt.
- **Duplicates never reach the write.** A same-(relation-id, idempotency-key)
  replay is dropped by `(filter> (nil? *prior-decision))` at `:562`, *before*
  `relation-outcome` (`:566`) — so it never enters the accepted branch → zero
  activity rows.
- **Rejected requests never reach the write.** The 4th hop sits inside the
  `<<if (outcome-accepted? …)` at `:579`; `outcome-accepted?` is false for rejects
  → branch skipped.
- **Insertion point verified by paren-accounting.** The `let [mb …]` closes on
  `:621` (`…$$relation-target-descriptors))))` = local-transform / `<<if` /
  `<<sources` / `let`). The five new forms land after the to-side writes and
  *before* the `<<if` close — exactly where the plan places them.
- **Variables survive the hops.** The existing `*row` (bound `:587`, before any
  hop) is consumed at `:610` (after `|hash *from-tk`) and `:618` (after
  `|hash *to-tk`) — proving Rama carries in-scope vars across ≥2 partition hops.
  So `*outcome` (bound `:566`) is available on the to-side task for the plan's
  activity extraction, and `*bucket`/`*activity-ok`/`*activity-row` carry across
  the `(|hash *bucket)` 4th hop. ✅

### 3.2 `RelationActivityRow` constructor — 16 args, correct order
Field-by-field match of the plan's §4.5 `->RelationActivityRow` against the §5.3
defrecord: `order-key bucket relation-id relation-kind from-kind from-id to-kind
to-id asserter-actor-id asserter-type envelope-actor-id relation-status
previous-status event-id claimed-at-ms arrival-at-ms`. All 16 line up
(`new-status`→relation-status, `prev-status`→previous-status; `from`/`to` are
`RelationTargetRef` records with `:target-kind`/`:target-id`). `new-status`
(`:366`) and `prev-status` (`:367`) are in scope. For a unary `:none` target,
`(:target-id to)` is nil → `to-id` nil, `to-kind` `:none` — honest, not a break.

### 3.3 R3 placement + let-scope — follows the R1/R2 precedent exactly
R3 is declared in the `defmodule` body (using `topologies`), *outside* the
`let [mb …]` where `$$relation-activity-by-bucket` is declared. This is
identical to how the *existing, green* R1 (`:628`) and R2 (`:662`) — both outside
the let — reference `$$relations-by-target` etc. declared inside it. So the naive
worry ("R3 can't see the let-scoped PState") is refuted by working precedent. R3's
dataflow (`explode` buckets → `|hash` per bucket → `local-select> MAP-VALS
{:allow-yield? true}` → `|origin` → `+vec-agg` → sort) mirrors R1's idiom. ✅

### 3.4 A2 one-line registry edit is safe
Verified by grep across `src/`+`test/`: **no second enumeration** of the kind set
exists. Every kind check routes through `registered-kind?` (`:210`) or the
descriptor-driven R1 filter; the only other files touching kind keywords are
per-test usages in `relation_kernel_test.clj` (e.g. `:based-on` in a request), not
an allowlist. Adding `:confirms :refutes :supersedes` to `relation-kinds` requires
touching nothing else. Descriptor bound grows to ≤ 2×10 = 20 rows, still
non-subindexed (`:539-540`, no `:subindex? true`). ✅

### 3.5 Custody (A1) provably cannot regress the 165 assertions
- **No `->Relation*Row` literal in the test ns** (grep clean) → no expected-row
  comparison can go stale on the arity change; assertions are field-level or
  cross-copy.
- **Cross-copy full-row equalities are custody-invariant.** T10 at
  `relation_kernel_test.clj:168` is `(= (first from-side) (first to-side) row)` —
  three *copies of the same `*row`*, written via `termval *row` at 587/610/618.
  Custody is written identically to all three, so equality holds for ANY envelope
  actor, defaulted or not.
- **Existing tests that pass a distinct `:actor`** (llm `:230`, mallory
  `:359/:400`, empty `:513`) are all retraction-**rejected** / shape-rejected
  cases that assert on decision *status/reason*, never full rows.
- The plan's §2.4 justification (assert defaults envelope-actor to asserter via
  `envelope :714`) is correct but narrower than the real reason above; conclusion
  robust. ✅

### 3.6 id-prefix routing + family colocation
Subagent confirmed `extract-object-key`/`leading-object-key`
(`object_container.clj:262-334`) map `oc:doc:<k>`, `oc:block:<k>:<u>`,
`du:<k>:<u>` → `<k>`, and `oc:chat-conversation:chat:<id>` /
`oc:chat-message:` / `oc:tool-call:` / `oc:tool-result:` → **`chat:<id>`**
(keeps the `chat:` segment). So a conversation family colocates on
`hash("chat:<id>")`, and B1's per-target `(|hash$$ $$containers-by-id
*object-key)` with the EXTRACTED key routes to the source family task
(mirrors.md:100). The plan's §9.4 rule — `bundle-target-class` admits only §4's id
set and routes `tc:*`/`src:tr:`/`imp:*` to `:target/unrecognized` — is the correct
guard. ✅ (Family-grouping edge cases E-6 unary→`:this`, F-6 sibling→from-keyed,
R1 dedup by relation-id at `relations-pairs->map :469-494` — all present in the
plan and consistent with the code.)

---

## 4. Cross-document conflict scan (no silently resolved binding-doc conflict)

The only cross-doc discrepancies are ones the PLAN already recorded, or ones the
IMPLICIT_SPEC's own §7 addendum flags — handled per the precedence rule
(contract > derived spec), none silently picked:

- **F-3 (`:latest`/`:rev` resolution).** IMPLICIT_SPEC C7 says
  `read-current-revision` for `:latest`; CONTRACT §3 v1.1 says
  `read-latest-source-by-ref`. PLAN §5.3/§10 **follows the contract** and records
  the discrepancy + a conditional escalation trigger. Subagent confirmed the arg
  vectors force the plan's reading: `read-latest-source-by-ref [*source-ref]`,
  `read-source-by-ref-version [*source-ref *source-version-key]`,
  `read-current-revision [*container-id]`, and **no query reads an arbitrary
  container revision by revision-id** — so a spec addressed `oc:doc:<k>` needs the
  container→source-ref join. Join fields exist: `ObjectContainerRow.source-id`
  (`:99-102`), `SourceArtifactRow.source-ref` (`:82-84`). Coherent; see Open
  Doubt B for the residue.
- **`:clock` → `:order`.** IMPLICIT_SPEC B2 (pre-v1.1) lists `opts (:clock)`; F-1
  ruled it to `:order` and the SPEC §7 addendum says so. PLAN uses `:order`
  throughout. ✅ No conflict.
- **F-6 sibling key, E-6 unary, gate 4 two-clock** — PLAN matches CONTRACT v1.1
  verbatim. ✅

---

## 5. Rama-platform verification (subagent, both trees) — all citations MATCH

Load-bearing semantics confirmed from the doc text:
- `(|hash$$ $$mirror *k)` **routes to the source module's task** owning `*k`
  (mirrors.md:100, "not the current module's tasks") — validates the spike's
  family-read rule and B1's one-hop family read.
- **Sibling-index reuse**: one object-partitioner hop, then read same-partitioned
  sibling mirror PStates in turn (18:119) — validates B1 reading the whole OC
  family in one hop.
- **Mirror `local-select>` is an async boundary**; sequential mirror reads "may
  see temporally inconsistent snapshots" (mirrors.md:70/126; 18:131-141) —
  validates the plan's §9.3 honesty stance (bundle resolves to NOW, not an atomic
  instant; `rendered-at-ms` is the drift detector).
- **Launch-time dependency enforcement** (mirrors.md:193-195; 18:145-149) —
  validates the launch-order duty (source modules before trail-view).
- **Leading-partitioner-can't-be-mirror-state-partitioner is OPTIMIZATION only,
  not correctness** (13:191-199) — validates F-7 as a latency note; without it the
  start task is random and the topology hops normally.

---

## 6. Advisory notes for the implementer (NON-BLOCKING — fold in during the phase)

- **N1 (A3 accessor list incomplete).** §4.5's dataflow calls
  `(activity-order-key *activity-row :> *activity-ok)`, but §4.4's "add accessors"
  list names only `outcome-activity-row`/`outcome-bucket`. The implementer must
  also add an `activity-order-key` record accessor (keywords cannot sit in
  dataflow operation position — the `:397-398` comment; cf. `copy-*` accessors
  `:411-414`). Self-correcting (the code won't compile without it); recorded so
  it is not a surprise. Route: A3.
- **N2 (A3 extraction placement, style).** The plan extracts activity fields on
  the to-side task after two hops. The file's stated discipline (`:591-592`)
  extracts copy specs on the relation-id task *before* the hops, carrying only
  scalars/records. Both are correct (§3.1 shows vars carry across hops);
  extracting alongside `:593-604` would be marginally more idiomatic and avoid
  carrying `*outcome` across the hops. Optional.
- **N3 (C7 source-ref join under-specified).** `trail/via` must obtain the spec
  doc's `source-ref` from its container id (`oc:doc:<k>` → `source-id` →
  `source-ref`) before invoking `read-latest-source-by-ref` /
  `read-source-by-ref-version`. The plan names the join but not the *read path*
  the client wrapper uses to get there (call `read-context-bundle` on the spec
  first — its L0 `source-id` / L1 `source-ref` are already assembled — or hold an
  extra foreign handle). This is the thinnest spot in the plan; see Open Doubt B.
  Route: Phase B.
- **N4 (cosmetic).** IMPLICIT_SPEC I-20 / PLAN §9.4 cite `extract-object-key` at
  `:262-334` and `leading-object-key` at `:279-334`; the two are actually
  `leading-object-key :262-271`, `extract-object-key :279-334` (reversed labels,
  both in the cited window; routing confirmed true). No action.
- **N5 (spike attestation).** The plan's single-roundtrip design rests on the F-8
  spike; see Open Doubt A. Non-blocking because the fallback ships the same
  contract.
- **N6 (bucket-string width consistency).** §4.4 write, §4.6 R3, and §5.3 C2 must
  all use the same fixed-width `"%08d"` day-index format so lexical string order =
  numeric order and enumerated ranges line up. The plan flags this itself
  (§10 "R3 `[bucket-lo bucket-hi]` type"). Hold it across the three sites.

---

## 7. Open doubts (recorded with named falsifiers — neither blocks the phase)

- **Doubt A — F-8 spike is un-reverifiable from here.** The throwaway spike
  modules that proved "a query topology can invoke a cross-module mirror query"
  were never committed, so this session cannot re-run them. The Rama docs make it
  **inferable but not exemplified for the query-topology case** (the only concrete
  cross-module `invokeQuery("*mirrorQuery")` example, 18:68, is from a *stream*
  topology). **Why it does not block:** (1) the baton attests the spike ran green
  on `create-ipc` ("VERDICT = WORKS"), and the baton is the assertion-grade record
  designed to survive artifact loss; (2) the plan carries a **pre-specified
  fallback** — client-side composition of R1/R3, unchanged wrapper signatures, 2
  roundtrips (§0 residual, §7, §9.5) — and §7 states latency is not a gate. So even
  if the spike were wrong, Phase B has a defined path that ships the same contract.
  **Falsifier:** the Phase-B platform-verification smoke test; if
  query→mirror-query fails, take the documented fallback (no redesign). Note (from
  the Rama subagent): do not conflate `invokeQuery`-of-a-mirror-query with a
  *leading* mirror-state partitioner — 13:196 forbids the latter as a leading
  optimization only; the plan correctly treats them separately (§10 F-7).
- **Doubt B — gate-12 spec revision must be re-ingest, not edit.** The F-3
  resolution means `:latest` tracks the newest *source version* of the spec's
  source-ref, so the gate-12 fixture must revise the spec by **re-ingesting the
  spec file** (new `SourceVersionRow`), not via an `ObjectEditPayload` revision.
  The plan states this (§10) with a conditional stop-clause trigger: *if re-ingest
  cannot yield a stable pinnable source-version-key for a spec doc, escalate.*
  **Falsifier:** the gate-12 implementation; the trigger is already defined, so
  this is tracked, not open-ended.

*(Not re-verified here because it is a code-phase gate, not a plan concern:* the
full 165-assertion IPC suite (gate 16). This session confirmed the kernel ns loads
clean; the baton attests the suite green twice this day. Gate 16 runs at the code
phase.)*

---

## 8. Disposition

- **Verdict PASS** → per the session-routing table, the next phase is **A1
  custody** in a fresh session (Opus 4.8, high). The plan's phase order stands:
  A1 → A2 → A3 → B, one phase per fresh session; tests-write then run-to-green as
  their own phases; Fable gate on gates-green.
- **No stop-clause escalation.** No genuine two-reading binding-doc conflict was
  found; F-3 is contract-governed + §10-tracked, not a fork.
- **Advisories N1–N6** travel with the plan (recorded here; the implementer folds
  N1 into A3 and pins N3 in Phase B). Open doubts A/B carry their falsifiers.
- **file(1) gate:** this artifact was written without any raw control byte or
  `\u`-escape; verified `text` on save (standing package gate).

— End PLAN_VALIDATION_R1 —
