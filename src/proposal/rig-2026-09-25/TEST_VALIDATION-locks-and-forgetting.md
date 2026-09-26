# Test Validation — stage 2, "locks-and-forgetting"

<!-- Phase 6 of the rama skill (references/phase-6-test-validate.md, template
artifact-test-validation.md), done inside the build phase by the building
session itself, Claude Opus 5.5, 26 September 2026, 04:05 IST, on branch
rig-build-locks. Default verdict major-fail. Validated: the tests at dce34804
(test/rig/store/lock_test.clj, forget_test.clj new; envelope_test.clj,
stream_gate_test.clj adapted), before any run (phase 7 runs them).

The protocol, read again for this: PLAN-locks-and-forgetting.md "Namespaces
and tests" (lock_test 1 to 6, forget_test 1 to 9), "The door and the lease
road" (its promises: the missing lock, consumption three ways, the record
path, session close, P6 widened), the [V-F4] list; the interfaces
`rig.store.locks` exports; IMPLICIT_SPEC.md I-L1 to I-L8, OP8 to OP11, RD4,
RD7, and the matrix rows E5 (a value lock) and E6 (a person lock); builder A's
two test asks (a forgotten person's entry gone on every task after the
answer; the values a person forget kills handed to the purge seam). -->

## Minimize IPC launches

Check: each `create-ipc` + `launch-module!` is justified by shared mutable
state that would interfere.

- `rig.store.lock-test`: no cluster. Its `create-test-pstate`s are
  module-free test PStates.
- `rig.store.forget-test`: one `deftest`, one IPC, every block a `testing`
  inside it. Blocks that forget a person (irreversible and global) use
  persons and layers of their own (`:pia`, `:fay`/`:gus`, `:erin`, `:hal`,
  `:ike`, `:ivy`, `:a1-alice` ... `:a8-bob`, `:rw-alice`, `:sim`, `:kit`),
  so they share the one cluster without interfering; the crashes come last.
- Phase 1's namespaces keep their one IPC each (`smoke`, `stream-gate`); the
  suite launches three clusters in all.

PASS.

## Implicit spec coverage

Each edge case and entity-state × write combination this stage owns, and the
test that covers it (file, then block).

**The plan's forget_test list.**
1. A1 and A8 through the module, both ways, compared with the model; A2 to A7
   in the base as the model's group cases; the re-wrap's direction; the
   lost-pool resend (its four parts): forget_test "the model's A cases
   through the module", "the base's re-wrap", "a resend from a door that
   lost its locks ([V-F1])" (with its crash replay sub-block). Covered.
2. The missing lock, then the resend; an id of another session (its row
   stays); an id already consumed: "the missing lock, then the resend (2)".
   Covered.
3. Lease consumption, admitted (personal and agent), refused (a revoke
   between the lease and the offer), answered from the record (under the
   recorded id, and under a new one): "lease consumption, three ways (3)".
   Covered; the third part asserts the record path's consumption of the
   resend's own lease ([V-F1]) where the 25 September text said "nothing
   written".
4. P6 widened in three places after a value forget in `:alice` (row deleted),
   in `:alice-agent` (excised), and A1's person forget: `p6-check!` called in
   "value forgets" (personal, agent) and in the A cases for A1; also after
   the lost-pool resend. Covered for the depot and the log bytes, the value
   digest and the plaintext. **Not covered: the lock store's third place for
   a person forget, "the lock record remains and unwraps with no live person
   lock"**: no assertion that A1's lock rows are still there after the
   forget. FAIL (T-F1).
5. Other content before and after a forget: "a resend with other content,
   before and after a forget (5)". Covered.
6. A lease resent: "a lease resent (6)". Covered, with another count under the
   same name taken.
7. The two crashes: "a crash between the lease and the offer" (`:seen`), "a
   crash during the gate event, before and after its writes"
   (`:before-writes`, `:after-writes`). Covered.
8. The 25 September cases: the ciphertext claim ("the door seals"); a value
   forget in `:alice` with its ledger, erasure at every moment after its
   stamp, the others open, heads unchanged, still replaceable; in
   `:alice-agent` (excised); in `:alice-hand`; `:own-row` in an agent layer;
   per-act grain with `:grain-mismatch`; a second forget; a forget after a
   person forget (L16); Bob's forget refused; unknown and foreign-layer
   targets; a forget of a control fact; a marked mention dying with its
   person; a write about a person with no lock; after a person's forget (a
   lease refused, an old lease's lock refused on its face, the grain switch
   admitted); a person made twice and forgotten twice; time travel; the crash
   in a person forget's fan-out; the crash in a value forget; the subject
   slot; the lock store's count. Each is a block of "value forgets, their
   ledger and their reach", "the door seals", or a crash block. Covered.
   **Not covered: L17's "a forget by a person whose permission is revoked is
   refused `:permission-revoked` as the model does" (O13).** FAIL (T-F2).
9. Session close: "session close (9)", with another session's rows staying,
   a closed lease refused, a second close, and a close from another session
   refused. Covered.

**The plan's lock_test list.** 1: `a-cases-at-the-lock-level` (the eight
cases, the wrap and the close date against the model, then real key
wrapping, open exactly when the model says open) and
`wrap-table-against-the-model` (generated owners, subjects, marks, forget
orders; unwrap exactly when open). 2: `crypto-round-trips`. 3:
`the-grammar`. 4: `placement-and-the-per-act-plan`. 5: `nothing-throws`. 6:
`the-resend-check`. Also `the-value-checks` (L27's order),
`the-lock-decision` (refusals and effects), `every-lock-write-fits-the-schema`.
Covered.

**IMPLICIT_SPEC, the locks invariants.** I-L1: the A cases, both files. I-L2
(lock rows on the values' task): by construction, and the foreign reads by
layer find the rows (every `lock-row` read routes by the layer). I-L3:
erasure is by a destroyed lock (`the-lock-decision`, the forget blocks).
I-L4: `p6-check!` over the raw depot. I-L5: the purge seam records the values
a forget erases ("in a personal layer", the A cases' purge assertions); the
ledger and by-stamp hold ids and stamps only (read, not rebuilt: rebuilding
is the read exit's). I-L6: per-act grain. I-L7: time travel. I-L8: the
replays in the crash blocks write other bytes and the same facts (a
committed encoding only). Covered.

**OP9, OP10.** The answer is complete at once (every assertion right after
the forget's answer, no waiting); per-act erases the act; two forgets; a
forget after a person forget; unknown targets; the owner or the operator;
the forgetter's permission revoked: T-F2 above. Person forget: every task at
once after the answer (the A cases, `:ike`), twice keeps the date, a write
about a forgotten person refused, their later lease refused, their grain
switch admitted. Covered but T-F2.

**E5, a value lock.** K0 × forget (a control fact: "refused forgets and
forgets that change nothing"); K1 × forget (personal); K2 × forget (agent,
excised); K1/K2 × a required person's forget (A1, the marked mention); any-of
× the first forget (A4, A5) and the last (A6); K3 × forget again ("a second
forget"); K5 × value forget (`:erin`); K1/K2 × grain switch (per-act: a value
before the switch keeps its lock). **Not covered: K4 × value forget again
(an excised value forgotten a second time keeps the first date)**, and RD7s
after a person forget (the row stays, closed; the same assertion as T-F1).
FAIL (T-F3). K × read-out and restore are stage 4's and absent (O16).

**E6, a person lock.** Q0 × forget (the eight A cases); Q1 × forget again
(`:ike`); Q1 × a write naming them marked (`:gus`, `:person-forgotten`) and a
write by them (`:hal`'s lease). **Not covered: Q1 × an unmarked mention of the
forgotten person in someone else's one-owner layer, which the wrap table
admits (the owner alone is required; L11 refuses only a wrap naming them).**
FAIL (T-F4).

**Builder A's asks.** A forgotten person's entry on every task right after the
answer: the A cases (every forgotten person, per task, via
`gen-hashing-index-keys`), `:ike`, and the fan-out crash (at least once).
The purge seam: "in a personal layer" (the value forget's one value) and the
A cases (every value the model says died is purged, none that stays open).
Covered.

**Phase 1's suite under sealing ([V-F4]).** Each changed expectation is marked
at its place: envelope_test (seal before parse; the value rows at the
opening; the parts digest; `:not-sealed`), stream_gate_test (13 seed acts; the
grammar's subject; sealed raw rows; the lease-refused rows; a name decided
fresh once the writer can lease; a revoke between lease and offer stays
recorded; persons for L11; the in-flight winner; stocked pools where stamps
are predicted). The model comparisons that now differ (a value act refused on
its face where the model records a permission reason) are asserted as the
lease's recorded reason and named as differences.

**Not testable on this branch:** `open-value>` has no caller here (the read
exit calls it after the merge); its pure half (`open-with`), its fid guard and
the point read's behaviour past the end of a row vector are tested, and
`open-row>`, its twin, runs in every `read-as-of` read. The holder road is not
built. Named, not a FAIL.

## Synchronization

Check: every write that precedes a read waits for processing.

Every write is `foreign-append!` with `:ack` (the door's `offer!`, and
`raw-send!`), which for a stream topology returns once the event tree is
complete and its writes visible (stream.md, "Read-after-write"); the person
fan-out is inside that tree, so the per-task reads right after a forget's
answer are the very claim under test. After an injected crash the tests wait
for the record (`wait-until` on `c/lookup`) and read through `retrying`, as
phase 1 does. No microbatch in this stage. PASS.

## Test namespaces compile

`rig.store.lock-test`, `rig.store.forget-test`, `rig.store.envelope-test`,
`rig.store.stream-gate-test`, `rig.smoke-test`, `rig.store.clock-test` load
cleanly with the test classpath (a scratch loader requiring each; 04:02).
No private namespace is required; private vars are reached with `@#'` as
phase 1's tests do. PASS.

## Fixes (applied in phase 7's pre-loop)

- **T-F1** forget_test, the A cases: after A1's person forget, its two values'
  lock rows are still in `:locks`, and none unwraps with the live persons.
- **T-F2** forget_test: a person whose own-layer permission is revoked, then
  forgets a value of theirs: `:permission-revoked`, recorded, the value still
  open.
- **T-F3** forget_test, the agent layer: the excised value forgotten again is
  a yes with `:how nil`, and the ledger keeps the first date.
- **T-F4** forget_test: an unmarked mention of `:gus` (forgotten) in `:fay`'s
  layer is admitted after his forget, wrapped under Fay alone.

## Verdict

**minor-fail**: four missing cases, each an assertion or a short block added
to an existing `testing` of `rig.store.forget-test`; no new namespace, no
restructuring.

PHASE_VALIDATION:minor-fail
