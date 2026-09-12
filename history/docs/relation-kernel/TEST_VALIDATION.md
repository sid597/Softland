# Test Validation

<!-- Phase 6. Filled 2026-07-03 (Claude, adversarial pass). Default verdict was
     major-fail; every check below was explicitly walked before any PASS. -->

Reviewed: `test/app/server/rama/relation_kernel_test.clj` (3 deftests, 17
testing blocks, 27 submitted requests) against `CONTRACT.md` (§11 gates 1–11,
§9 refusals), `IMPLICIT_SPEC.md` (operations W1/W2/R1/R2/V1 + the entity×write
matrix), and the module `src/app/server/rama/relation_kernel.clj` (reference
only; Phase 4 validated it).

Independently re-verified this session (not taken from Phase 5's word):

- Test ns loads cleanly: `clojure -M:test -e "(require 'app.server.rama.relation-kernel-test)"`
  → `:TEST-NS-LOAD-OK` (run fresh this session; deftests define, none execute).
- `com.rpl.rama.test` arglists introspected from the jar:
  `wait-for-microbatch-processed-count` `([ipc module-name topology-name count]
  [ipc module-name topology-name count timeout-millis])`;
  `pause-microbatch-topology!` / `resume-microbatch-topology!`
  `([ipc module-name topology-name])`. Every `rtest/` call in the test file
  matches an existing arity.
- Barrier semantics from the skill references: count is **cumulative** — total
  records ever processed (`testing.md:120-121`, `references/rama/27-testing.md:121`);
  the 5-arity **throws on timeout** (`testing.md:106`); a microbatch is one
  cross-partition transaction (`core-concepts.md:11`), and the canonical idiom
  asserts PStates immediately after the wait (`testing.md:108-117`).
- Pause semantics: `pause-microbatch-topology!` waits for the in-flight batch
  to complete before returning; the next batch contains **all** records
  appended while paused, up to 1000/partition (`testing.md:136-139`).
- `oc/extract-object-key`: `oc:doc:` strips the prefix
  (`object_container.clj:313-314`), unknown ids verbatim (`:334`) — every test
  target-key derives exactly as the tests assume, all distinct per scenario.
  `oc/fixed-width-order-key` is `%020d:%s` (`object_container.clj:238-240`), so
  lexicographic history order == numeric timestamp order for e5's 1000/2000/3000.

## Minimize IPC launches

**Check: each `create-ipc` + `launch-module!` costs 30+ seconds; default is ONE
deftest; every additional deftest must be justified by naming the specific
shared mutable state that would interfere.**

Three deftests = three launches:

1. `relation-kernel-assert-and-read-test` (L73) — gates 1/7/8/9/10, F1a/F1b,
   R1/R2 edges.
2. `relation-kernel-idempotency-and-partition-test` (L214) — gates 2/3/11, G2,
   G1.
3. `relation-kernel-retract-and-reject-test` (L333) — gates 4/5/6, edges
   e1/e2/e3, lifecycle e5.

Walk-through:

- Deftest 2 is justified by named shared mutable state: its G1 block
  (L299-328) mutates the singleton topology's paused/running state via
  `pause!`/`resume!`. A failure between pause and resume starves **every**
  subsequent block's `drain!` in the same launch (each a 30s timeout that
  throws), cascading one failure into minutes of misleading timeouts. Isolating
  the pause-wielding scenario — and ordering it last within its deftest —
  bounds that blast radius. This is interference of shared state, not
  "different concern". ACCEPTED.
- Deftests 1 vs 3: **no interference justification exists or is stated.** All
  keys are disjoint (`g1-*`/`g7-*`/... vs `g4-*`/`g5-*`/`e*-*`), both use only
  `submit!`/`drain!`, and the template rules the available arguments
  ("different concern", per-launch task-count diversity) out of class.
  By the template's letter these blocks belong in one deftest/launch.

**FAIL** — finding T1: merge deftest 3's five testing blocks into deftest 1
(one launch fewer; blocks are self-contained on disjoint keys; the shared
harness/counter design extends unchanged — the cumulative counter is
per-launch, so consolidation needs no logic change). Deftest 2 stays separate
on the pause-isolation argument.

## Implicit spec coverage

**Check: every edge case and entity state × write combination in
IMPLICIT_SPEC.md is tested; each one listed with the covering test.**

### CONTRACT §11 gates (binding count is 11, post-F2)

| Gate | Covering test (deftest / testing block) | Verdict |
|---|---|---|
| 1 assert + dual read, different partition keys | dt1 "Gate 1" L78-102: `(is (not= a-key b-key))` L89; R1 from both keys L91-97; kind/asserter/status L98-100; R2 row+history L101-102 | PASS |
| 2 idempotent client retry | dt2 "Gate 2" L219-234: identical request twice L228; history stays 1 L231; V1 copy-count 1 per endpoint L232-233; journal decision `:accepted` L234. Barrier counts both records processed (see Synchronization), so "second request consumed yet wrote nothing" is proven, which is the strongest observable of replay-and-stop in a fire-and-forget depot model | PASS |
| 3 convergent re-import | dt2 "Gate 3" L236-255: id equality check L245; separate batches (drain between) L246-247; V1 single copy both endpoints L248-249; descriptor not double-counted L250-253; reassertion history on ONE identity L254-255 | PASS |
| 4 retract consistency | dt3 "Gate 4" L338-361: hidden by default from A and B L352-353; `:retracted` via `include-retracted?` from both L355-358; detail status + ordered `[:asserted :retracted]` L359-361 | PASS |
| 5 retraction rights | dt3 "Gate 5" L363-381: wrong actor; status unchanged L376; **no status event added** L377 (this also falsifies the "rejected request writes status log" bug class on an existing row); still visible L378; rejected decision + reason L379-381 | PASS |
| 6 registry rejection, zero non-spine writes | dt3 "Gate 6" L383-399: R2 empty L392, R1 empty L393, V1 no authoritative row L394, no target copy L395, no descriptor L396, durable rejected decision + reason L397-399. **Partial — see T9**: `$$relation-status-log-by-relation` is never directly checked, and R2's empty history is vacuous for a missing row (module L664-676 reads the log only when the row exists), so a stray log write under a never-created relation-id escapes the suite | **FAIL (T9)** |
| 7 dangling accepted + readable | dt1 "Gate 7" L104-118 (no object-container material exists anywhere in these tests, so the endpoints address nothing; accepted + readable from both keys + detail) | PASS |
| 8 unary `:none`, one target-key touched | dt1 "Gate 8" L120-142: inheritance asserted L129; R1 dedups to one row with `:none`/nil-id/inherited-key fields L134-138; V1 count=2 under the single inherited key L141-142 — if the i-copy had gone to `""`/nil instead, this count would be 1 and fail, so the assertion does pin both copies to the one key | PASS |
| 9 asserter separation | dt1 "Gate 9" L144-166: distinct ids asserted L158; both returned from both endpoints L162-164; per-id detail scoping L165-166 | PASS |
| 10 kind filter | dt1 "Gate 10/F1a/F1b" L168-195: no-filter whole-map path returns both kinds L185-188 (F1a); per-kind filter isolates with an explicit no-leak assertion L190-195 (F1b). Direction-cross-leak needs no extra case: kinds-filter contract returns both directions of a kind, and R1 dedups by relation-id | PASS |
| 11 cross-relation key reuse | dt2 "Gate 11" L257-278: same `SHARED-KEY` on two different relations; both `:asserted` L271-272; two accepted journal decisions with distinct decision-ids L273-278 | PASS |

### CONTRACT §9 refusals

- (1) no FK validation → gate 7 (acceptance of dangling IS the refusal's
  observable). PASS.
- (4) no auto-merge across asserters → gate 9 (two rows, never merged). PASS.
- (5) no cascading on retraction → gate 4/e5 prove retraction is status-only
  with history preserved, but no test retracts a relation while another
  relation shares its target-keys, so "nothing else changed" is currently
  vacuous. The missing multi-asserter mixed-state row (T2) is exactly the
  co-resident probe; its fix covers this refusal behaviorally.
- (6) no deletion → gate 4 + e5: retracted rows remain readable forever via
  `include-retracted?` and detail. PASS.
- (2) confidence scores, (3) relation-as-container → non-features with no
  behavioral surface an IPC test can assert the absence of; Phase 4's diff
  review owns these (it passed them). Not test obligations.

### Entity×write matrix walk (rows → covering block)

RelationEdge identity: does-not-exist×assert → dt1 Gate 1; does-not-exist×retract
→ dt3 e2 (L414-428, `:relation/absent`); asserted×assert-same-asserter-new-key →
dt2 Gate 3 (cross-batch) + dt2 G1 (same-batch); asserted×assert-different-asserter
→ dt1 Gate 9; asserted×retract-original → dt3 Gate 4; asserted×retract-different-actor
→ dt3 Gate 5; retracted×assert-original → dt3 e5 (L443-464, `[:asserted :retracted
:asserted]`, copies/descriptor return to 1); any×duplicate-same-key → dt2 Gate 2
(assert only — see T4); any×unregistered/malformed → dt3 Gate 6 + e1 + e3.

IdempotencyKey: unused×valid-assert → Gate 2 journal read; unused×valid-retract →
Gate 4 (accepted, by effect) / Gate 5 (rejected, read directly); unused×invalid →
Gate 6 + e1 (sentinel journal read with `""` key, L410-412);
used-accepted×same-key → Gate 2.

RelationKind: registered×assert/retract → throughout; unregistered×assert →
Gate 6.

RelationTargetRef: well-formed → throughout; dangling → Gate 7; unary-none →
Gate 8; malformed×assert → e3 (L430-441, `:relation/to-malformed`).

Asserter: original×assert-existing → Gate 3; different×assert → Gate 9;
original×retract → Gate 4; different×retract → Gate 5.

Membership: empty×assert → Gate 1; has-asserted×same-identity → Gate 3/G1;
×different-asserter → Gate 9; ×retract-original → Gate 4; has-retracted×assert →
e5; any×invalid → Gate 6.

Events/history: none×accepted → Gate 1 (history=1); none×rejected → e1/e2/e3/g6
via R2-empty (public surface; the PState-level hole is T9);
has-history×accepted → Gate 3/e5 (ordered); has-history×rejected → Gate 5
(history pinned at 1 — the strong falsifier).

R1 edges: empty input `{}` L199; unknown key `[]` L201; duplicate keys L204-205;
missing/malformed R2 id L207-208 (both the `rel:`-prefixed-missing and
non-`rel:` shapes).

### Missing combinations (each one an explicit spec row/edge with no covering test)

- **T2** — `has-multiple-asserters` × retract one original asserter
  (IMPLICIT_SPEC L443-448): mixed state; default R1 returns only still-asserted
  identities, `include-retracted?` returns both. No test retracts one of two
  co-resident assertions (Gate 9 never retracts). Also the behavioral probe for
  §9 refusal 5.
- **T3** — `retracted` × valid retract, new idempotency key ("retract-affirm",
  IMPLICIT_SPEC L286-293; W2 L131-133): stays retracted, no duplicate row, no
  second identity. The impl has a dedicated arm for it
  (`count-deltas` same-status branch, module L267) that no test reaches.
- **T4** — duplicate **retract** with the SAME idempotency key (W2 L129-130:
  replays, no new status event) and `used-rejected` × same-key replay
  (IdempotencyKey matrix L326-330: e.g. resubmit Gate 5's `g5-kbad` or e1's
  missing-key request and assert nothing changed). Gate 2 proves the journal
  for accepted asserts only.
- **T5** — unregistered kind × **retract** (RelationKind matrix L348-351; W2
  edge L145): rejected durably, truth unchanged. Gate 6 tests assert only.
- **T6** — shape-rejection sweep incomplete (W1 edge L95-97; Asserter
  `missing-or-invalid` rows L399-402/411-414): missing request-id, missing
  actor id, malformed **from** target are never submitted. Covered today:
  missing idempotency-key (e1), malformed to (e3), unregistered kind (g6).
- **T7** — R1 nil-containing input (IMPLICIT_SPEC R1 edge L179-181): no
  `(read-relations-for-targets runtime [nil])` / `[nil a-key]` case proving
  empty behavior rather than a scan (one-line assertions).
- **T8** — kinds-filter × `include-retracted?` interaction (R1 invariants
  L162-164 with W2 L134-135): after Gate 4's retract, no filtered read is
  issued, so `relation-read-ranges`' kinds-branch is never exercised with
  total-count survivorship (`[:based-on] true` → `[:retracted]`) nor with the
  asserted-count=0 skip (`[:based-on] false` → `[]`). Both are one-line reads
  in the existing Gate 4 block.
- **T10** — W1 row-agreement invariant (L77-79: endpoints and by-id agree on
  "status, asserter, evidence, note, timestamps, event id, and request id")
  and R2's "why does this edge exist?" visibility (L198-199): no test submits
  a present `:evidence-source-id`/`:evidence-anchor-id`/`:note` and asserts
  round-trip, and Gate 1 compares only id/kind/asserter/status across
  endpoints. Fix: give one assert (e.g. Gate 7's) evidence+note and assert
  full record equality `(= (first from-side) (first to-side) (:row detail))`.

**FAIL** — findings T2–T10 (T9 recorded under the Gate 6 row above). Every one
is an additive testing block or assertion lines inside the existing deftests
using the existing harness, with expectations pinned by the matrix; none
touches the harness, barrier, key scheme, or module.

## Synchronization

**Check: every write that precedes a read is followed by the barrier before the
read.**

The suite's barrier is `drain!` = `wait-for-microbatch-processed-count ipc
module-name "relation-kernel-topology" @!appended 30000` (L58-61), with
`!appended` incremented once per successful `submit!` (L54-57). Topology name
matches the module (`relation_kernel.clj:513`).

Sequence walk (every block): Gate 1 submit→drain→reads L90-102 ✓; Gate 7
L115-118 ✓; Gate 8 L130-142 ✓; Gate 9 two submits→drain→reads L159-166 ✓;
Gate 10 two→drain→reads L183-195 ✓; R1/R2 edges: reads only, all prior writes
drained ✓; Gate 2 two→drain→reads L228-234 ✓; Gate 3 submit→drain, submit→
drain→reads L246-255 ✓; Gate 11 two→drain→reads L269-278 ✓; G2 one→drain→reads
L287-297 ✓; G1 pause→two submits→resume→drain→reads L311-328 ✓; Gate 4
assert→drain→read, retract→drain→reads L348-361 ✓; Gate 5 L373-381 ✓; Gate 6
L391-399 ✓; e1 L408-412 ✓; e2 L423-428 ✓; e3 L437-441 ✓; e5 three
submit→drain pairs→reads L453-464 ✓. **No write→read sequence skips the
barrier.**

Barrier correctness (the baton's scrutiny items 1–2, walked adversarially):

1. **Does the barrier guarantee visibility of the cross-partition `|hash`
   endpoint copies before the negative assertions read them?** Yes. A
   microbatch is a single cross-partition transaction (`core-concepts.md:11`);
   a record counts as processed only as part of a completed batch, and the
   documented purpose of the wait is "to ensure the PStates reflect the
   processing of the appended depot records" (`references/rama/27-testing.md:121`),
   with the canonical idiom asserting PStates immediately after the wait
   (`testing.md:108-117`). So at `drain!` return with count == all appends,
   every endpoint-copy and descriptor write from those records' batches is
   committed. A too-early return would make the *negative* copy-count
   assertions (L232-233, L248-249, L316-317, L460) vacuously green — this is
   why the semantics were verified against the references rather than assumed.
2. **Is submit! == +1-processed airtight?** Yes. (a) Every one of the 27
   requests is built by `assert-request`/`retract-request`, whose routing key
   is `relation-id-for` = `"rel:" + sha1(...)` — always present, so the
   client-side refusal (`relation_kernel.clj:768-770`) can never fire and the
   topology ingress filter (`relation_kernel.clj:551`) never drops a test
   record. (b) Even for a dropped-in-dataflow record the count would still
   advance — it counts depot records consumed, not writes. (c) `submit!`
   increments only after `foreign-append!` returns, so a failed append cannot
   overcount. (d) Counter and drain run on one thread; `@!appended` is read at
   call time. (e) The count is cumulative per launch (`testing.md:120-121`)
   and each deftest gets a fresh IPC, so cross-block accumulation is exact.
3. **Timeout behavior**: the 5-arity throws on timeout (`testing.md:106`) —
   a stuck record fails loudly, it cannot silently fall through to stale
   reads. Additionally every testing block pairs its negative assertions with
   at least one positive assertion (a decision, row, or history read), so even
   a hypothetical silent-early-return could not vacuously pass a block.
4. **G1's same-batch forcing is real**: pause returns only after the in-flight
   batch completes, and the next batch contains all records appended while
   paused (`testing.md:136-139`), so req-1 and req-2 are in ONE batch by
   construction, not by luck.

Falsification power of the V1 assertions (baton scrutiny item 3):

- **G1 (broken intra-batch read-your-writes)**: if request 2's
  `local-select> $$relations-by-id` missed request 1's in-batch write, the
  outcome would mint a fresh row with `first-asserted-at-ms` 2000 — the test
  deliberately uses different timestamps (1000/2000, L309-310) so the
  sort-keys diverge (`fixed-width-order-key` embeds the timestamp) and a
  second physical entry appears under both target keys → `(= 1 (count
  (read-target-index ...)))` fails (L316-317); `count-deltas` would also
  double the descriptor → L322-323 fails. History=2 (L328) additionally
  discriminates "request 2 journal-stopped/dropped" from "processed as
  reassert". Public R1 would mask all of this (it dedups by relation-id) —
  using the V1 physical reader is what makes the check falsifying. VERIFIED.
- **G2 (broken custom key-partitioner)**: the topology writes
  `$$relation-decisions-by-id` / `$$relation-events-by-id` on the depot's
  `hash-by`(relation-id) task with no repartition (`relation_kernel.clj:574,586`);
  `read-decision-by-id`/`read-event-by-id` route by the PStates' declared
  `:key-partitioner` (clojure `hash` of the recovered relation-id, mod N). A
  mismatch (mod N) returns nil and `(is (some? decision))` (L292) fails.
  Mod-N masking for one key is ~1/N, but the suite probes ≥6 distinct
  relation-ids through these readers (G2, Gates 5/6, e2, e3) under randomized
  task counts (2/4/8), so a systematic hash divergence cannot survive a run.
  VERIFIED.

**PASS.**

## Test namespaces compile

**Check: every test namespace loads cleanly; imports/`:refer`s resolve; no
private-var dependencies.**

- Fresh load this session: `:TEST-NS-LOAD-OK` (command above). Loading
  compiles every form and re-macroexpands the module `defmodule` via the
  `:require` of `app.server.rama.relation-kernel`; deftests define only.
- Requires: `app.server.rama.relation-kernel` (exists, compiles),
  `com.rpl.rama.test` (fns verified in the jar with matching arities),
  `clojure.test` (stdlib). No record-constructor `:refer`s are needed — the
  tests build records only through public module fns (`->target-ref`,
  `unary-to-ref`, `assert-request`, `retract-request`).
- Private-var scan: the only private defns in the module (`envelope`,
  `id-part-separator`, marker vars) are not referenced by the tests; all 19
  `rk/` vars the tests call are public `defn`s, arities hand-matched against
  the source this session.
- `topo-name` "relation-kernel-topology" (L30) matches
  `relation_kernel.clj:513`; harness destructuring keys (`:submit!` etc.)
  match the map literal (L54-62).

**PASS.**

## Observations (non-blocking, for Phase 7 / the Fable gate)

- Single worker (baton judgment (b)): the prod `|hash` hops carry
  `RelationEdgeRow`/descriptor records across tasks; with 1 IPC worker no
  cross-worker serialization occurs. All record fields are plain data
  (strings/keywords/longs/nested records) and Phase 4 signed the records, so
  this stays an observation per the codebase's compute/transcript convention —
  a `:workers 2` smoke run remains the cheap hardening follow-up already on
  record in the baton.
- e5's assertion message "delta underflow-guarded" (L463) overstates: the
  lifecycle never drives a count below zero, so the `max 0` guard itself is
  not exercised; the assertion is still correct as a returns-to-1 check.
- Judgment call (a) (no-op status events for reassert/retract-affirm recorded
  as history) is explicitly permitted by IMPLICIT_SPEC ("may add audit/status
  history for a reassertion", L73-74/291-292) and asserted consistently
  (Gate 3 history=2, G1 history=2, e5 history=3). Confirmed, not a redesign.
- Gate 2's two identical submits are not schedule-forced; the assertions hold
  under both the same-batch and cross-batch schedules (traced), and the
  intra-batch read-your-writes mechanism itself is deterministically proven by
  G1, so no additional forcing is required.

## Verdict

**minor-fail** — two checks fail (IPC-launch justification T1; implicit-spec
coverage T2–T10 including Gate 6's status-log hole T9), and every failure is
fixable by editing/adding specific lines and self-contained testing blocks
inside the existing test namespace with the existing harness — no new
namespaces, no harness/barrier redesign, no module change; the suite's
architecture (fixture, cumulative-count barrier, V1-vs-R1/R2 discipline, key
scheme) survives review intact.

Fix list for Phase 5 (all in `test/app/server/rama/relation_kernel_test.clj`):

1. **T1** — merge deftest 3's five blocks into deftest 1 (drop one IPC launch);
   keep deftest 2 separate (pause blast-radius justification stands).
2. **T9** — in Gate 6 (and cheaply in e1/e2/e3): assert
   `$$relation-status-log-by-relation` has no entry for the rejected
   relation-id via a direct `foreign-select` on the runtime's
   `:status-log-by-relation` handle (IMPLICIT_SPEC V1 explicitly blesses
   direct PState inspection in tests).
3. **T2** — extend Gate 9 (or a sibling block): retract the llm identity;
   assert default R1 returns only sid's row, `include-retracted?` returns
   both with correct statuses, sid's detail unchanged.
4. **T3** — retract-affirm: second retract with a new key on a retracted
   relation; assert still one identity/copy, still excluded by default,
   descriptor counts unchanged.
5. **T4** — duplicate retract with the SAME key (no new status event) and a
   same-key resubmit of a rejected request (e.g. Gate 5's) replaying the
   rejected decision with nothing changed.
6. **T5** — retract with an unregistered kind → rejected, truth unchanged.
7. **T6** — parameterized shape-rejection sweep: missing request-id, missing
   actor id, malformed from-target → each a durable rejected decision with the
   right reason and no materialization.
8. **T7** — `[nil]` / `[nil a-key]` R1 inputs → `{}` / a-key-only.
9. **T8** — in Gate 4 after the retract: `[:based-on] false` → `[]` and
   `[:based-on] true` → `[:retracted]` (kinds-branch × survivorship).
10. **T10** — add evidence/note to one assert and assert full-record equality
    across from-side, to-side, and detail rows.

Per `references/phases.md`, minor-fail → Phase 5 applies the fixes, Phase 6 is
NOT re-run, then Phase 7 runs the tests to green.
