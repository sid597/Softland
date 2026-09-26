# Test Validation — stage 3, "micro-store"

<!-- Phase 6 of the rama skill (phase-6-test-validate.md, template
artifact-test-validation.md), run inside the build session on 26 September
2026 from 03:50 IST (Claude Opus 5.5, effort max), adversarial, default
major-fail. Tests: test/rig/store/micro_prepare_test.clj and
test/rig/store/micro_test.clj at commit e46badd2. Against: IMPLICIT_SPEC.md
(I-G, I-O, I-L1, I-P; OP2, OP3, OP4, OP5, OP6, OP7, OP8; E1 to E4 on the
micro side), PLAN-micro-store.md §F (the named tests, [PV-F2, F3, F4]), and
the module's public functions (the door, rig.store.micro-client, and the
four queries). Not run in this phase. -->

## Minimize IPC launches

`rig.store.micro-test` has one `deftest`, `micro-store`, with one
`create-ipc` and one `launch-module!`; every scenario is a `testing` block
on that cluster, on disjoint names, sessions and (where a case consumes
state, such as a revoke or a person forget) its own persons and sessions.
`rig.store.micro-prepare-test` launches no cluster (pure; `deftest`s split
by concern cost nothing). PASS.

## Implicit spec coverage

Each line: the requirement, the test (file, `deftest`, `testing` block), and
whether it is exercised. "micro-test" is micro_test.clj's `micro-store`;
"prepare" is micro_prepare_test.clj.

**The ruled requirements**

- I-G1 (a gate never throws): prepare `nothing-throws` (generated batches
  through intake, the gather's pure steps and `prepare`; parse and the parts
  digest; `row-wraps`/`fact-rows`; junk states), `route-key` (total).
  Exercised.
- I-G2 (idempotent under replay, one stamp): micro-test "a batch that fails
  on one task leaves nothing" (one-shot throws in blocks 1, 2a, 2b, 2c; one
  record, one stamp, rows once, leases consumed once, at least two runs).
  Exercised.
- I-G3 (resend after an error, answer from the record): micro-test
  "a resend in a later batch", the door's `offer!` resend road, and the
  crash block (the send survives a worker restart). Exercised.
- I-G4 (found by name through the name row): micro-test first micro case
  (`mc/lookup` by name, digest, fingerprint). Exercised.
- I-G5 (the order of checks) and PV-F13's L27 order: prepare
  `permit-refusal-order`, `fold-decisions` (stage 1's reasons, control
  facts, the four permission reasons), `arrival-lock-work` (the value and
  person reasons in order). Exercised.
- I-G6 (whole or not at all across tasks): micro-test "an act on two
  entities is admitted whole" (two tasks when the cluster has them) and
  "one good fact and one stale: refused whole"; prepare `fold-decisions`.
  Exercised for two entities. **FAIL (minor):** OP3's edge "an act touching
  many entities: many tasks, still whole" is not exercised beyond two.
- I-G7 (reuse refused by digest; landing named under a reserved scheme):
  micro-test "other content under the name"; prepare `parse-and-digests`
  (`:landing` taken as data here, `:crossing` refused). Exercised.
- I-G8 (unknown parts refused): prepare `parse-and-digests`. Exercised.
- I-O1 (re-class moves a layer here, its order promise ends): micro-test
  "re-class moves a layer here" (UUID7 order across arrival order).
  Exercised.
- I-O2 (never backward, after what it stood on): prepare
  `fold-order-and-the-race` "stamps strictly increase"; micro-test chains
  ("after what it replaces") and the group's making act ("stamped after
  what it stood on"). Exercised.
- I-O4 (order between stores only through stood-on): micro-test "the first
  group re-classes the base, then is made here standing on the re-class".
  Exercised.
- I-O5 (the settled frontier): micro-test "a reader never sees half a
  batch" (300 batches or more, four readers, one F per pass, zero
  violations; the raw readers' count), the crash block's reader. Exercised.
- I-L1 (the wraps, shared side) and R8's A cases both ways: micro-test "the
  group's six A cases" against `formal.scenarios/play`; prepare
  `block-2b-rows` (the wraps and their deaths both ways). Exercised.
- I-L2 (lock rows on the value's task): prepare `block-2b-rows` "a personal
  layer re-classed here keeps a lock row". Exercised (pure only; no
  re-classed personal layer on the cluster). Accepted: the placement is
  decided by the pure `fact-rows`, and the cluster's row write is the same
  code path as a record lock's.
- I-L6 (per-act grain): micro-test grain switch block; prepare
  `block-2b-rows` "per-act grain". Exercised.
- I-P1 (a gate checks only its own layers' permissions; another layer's
  refused): micro-test D1, "a chain whose ancestor lives in another layer"
  on both gates, "a stream-side revoke ... leaves her group permission
  live". Exercised.

**E1 (a name and its answer) at this gate**

- N0 × admitted: micro-test first micro case. Exercised.
- N0 × refused: micro-test "recorded refusals" gives the reasons.
  **FAIL (minor):** "the refusal consumed a stamp" (its stamp above the
  touched task's clock before, and the clock at it after) is not asserted,
  nor that a refused first use still holds the name (RD1x).
- N0 × tag mismatch: micro-test "refused on the face" (nothing recorded).
  Exercised.
- N1 × resend of the same content: "a resend and its original in one
  batch". Exercised.
- N1 × other content, both in flight: "two envelopes under one name in one
  batch". Exercised.
- **FAIL (minor):** N1 × tag mismatch while the original is in flight: not
  exercised.
- N1 × crash mid-decision: the crash block. Exercised.
- N2 × resend: "a resend in a later batch". Exercised.
- N2 × other content: "other content under the name". Exercised.
- **FAIL (minor):** N2 × tag mismatch (an offer under a decided name, made
  for another layer): not exercised on the cluster.
- **FAIL (minor):** N3 × resend (the same no and stamp), N3 × other content
  (other non-value parts `:name-taken`; other value content the recorded no,
  §J 1, [PV-F2]), N3 × tag mismatch: not exercised on the cluster (prepare
  `fold-names-and-faces` covers the resend of a recorded no at the fold).
- N2 and N3 × crash replay of a finished record: not applicable to this
  gate; a committed microbatch never retries (microbatch.md, "Guarantees"),
  and a record's later appearance is a resend, which is covered.

**E2 (a chain) at this gate**

- C0 × new fact, C0 × replace naming nothing (stale), C1 × replace of the
  head, C3 × replace of the replaced fact, C1 × two facts replacing it
  (doubled): micro-test chains block; prepare `fold-decisions`. Exercised.
- **FAIL (minor):** C1 × new fact without replace (two unreplaced facts on
  one chain, each replaceable once) and C1 × retract (a retract replacing
  the head; the replaced value still opens) are not exercised.
- C1 in a shared layer × person forgets: the A cases. Exercised.
- C1 × value forget: the micro value forget's lock effect is not built in
  this stage (IMPLEMENTATION_VALIDATION item 14, the stage 2 seam); not
  tested here, carried to the receipt.
- A stream-era head of a re-classed layer replaced here (OP4's R 105 edge):
  the re-class block and the base block (tombstone, frozen head, second
  replace stale). Exercised.

**E3 (a layer) at this gate**

- **FAIL (minor):** L0 × offer (an offer into a shared layer never made:
  `:no-such-layer`) is exercised only in prepare, not on the cluster.
- L0 × make: the seed (the group, a second group). Exercised.
- L3 × offer: the re-class block. Exercised.
- L4 × grain switch (the operator's a version, a person's refused):
  micro-test grain block. Exercised.
- **FAIL (minor):** L4 × re-class (`:unsupported-reclass`, O9) and a
  setting fact for a stream-held layer sent here (`:wrong-gate`, P16/M25)
  are not exercised on the cluster (the first is in prepare).
- L1 (a session) × close: "a session close in the group". Exercised.

**E4 (a permission) at this gate**

- P0 × write citing it (`:no-permission`), P0 × grant (`open-session!`),
  P1 × write in its own layer, P1 × write into another layer, P1 × revoke
  (D2), P1 × revoke racing a write (the race block), P2 × write citing it
  (D2 "and in the next batch"), P1 (a session root) × revoke (the stream
  gate's chain block). Exercised.
- **FAIL (minor):** P0 × revoke (`:stale-revoke`), P2 × revoke again
  (`:stale-revoke`, R18) and P2 × re-grant (P8: the first grant stays, so
  still revoked) are not exercised on the cluster.
- P2 × forget citing it: forgets at this gate are the operator's (M14),
  who cites none; not applicable.

**The plan's named tests (§F, [PV-F2, F3, F4])**

The micro cases, D1 and D2 (both compared with `fs/play`: `:missing`), the
revocation race both ways and the ancestor revoke (same batch and next),
the stream-side revoke leaving the group live, revoke cutting below on the
stream gate, re-class moving a layer, the base re-classed at the first
group (a base offer there first; `:class-mismatch` after; a `:by-entity`
one here; the tombstone; the stream-era row unchanged), the five missing
locks (never leased, consumed, another layer's, another session's, a
destroyed person lock) each a face with the name free, consumption three
ways, two offers citing one lock, the lease visible only at F, the value
check before and after a forget, the door that lost its locks both ways,
another session's lease ids, two envelopes under one name, the session
close, the six A cases, the crashes in 1, 2a, 2b, 2c, the frontier run.
Each has its `testing` block in micro-test. PASS for presence; the
cascade's tests ([PV-F8]) are not written because R19 builds the walk.

## Synchronization

Microbatch reads are synchronised on the frontier (M15): `mc/offer!`,
`mc/write!` and `mc/await-answer` return only when micro-lookup answers at
F >= the act's batch, which means every task has committed that batch
(batch F + 1 began only after every task committed F). The raw reads after
it (`record-of`, `lease-rows`, `head-of`, `tombstone-of`, `settings-of`,
`members-of`, `permission-of`, `clock-of`) read committed state on their
tasks and are therefore safe. **FAIL (minor):** `rows-of` (the door's
`open-act`) and the direct `mc/act` calls read through micro-act with F
nil, the entity task's *own* frontier, which can lag the name task's where
the answer was read (each task writes its frontier when the next batch
commits there). A lagging entity task would hide an act that is already
answered, and the test would fail by timing. The reader's rule (§D) is one
F per read: `open-act` must take F from the name's task and pass it, and
each direct `mc/act` must pass the answer's `:frontier`. The crash block's
reader and the frontier run already pass one F to every read.

## Test namespaces compile

Both namespaces loaded cleanly in this session
(`(require 'rig.store.micro-prepare-test)`, `(require 'rig.store.micro-test)`,
03:41 and 03:47 IST): no unresolved symbols, no private-namespace use
beyond `rig.store.micro`'s public functions. PASS.

## Self-consistency

Every gap above is marked FAIL; the not-applicable lines give their reason.
The value forget's effect is a named seam, not a passed check.

## Verdict

**minor-fail**: the synchronization fault and the missing matrix rows are
each fixed by editing lines inside the two existing test namespaces (one
change to the door's `open-act`, a passed F at the direct `mc/act` calls,
and assertions added to existing `testing` blocks); no new namespace and no
restructuring. Phase 7 applies them before its first run.

PHASE_VALIDATION:minor-fail
