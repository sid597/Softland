# Test validation — wave 1, the merge of phases 2 and 3 onto the read exit

<!-- Phase 6 of the rama skill (phase-6-test-validate.md, template
artifact-test-validation.md), done inside the build session by the builder,
Claude Opus 5.5, 26 September 2026, branch rig-wave1. Default verdict
major-fail. Validated: test/rig/store/wave1_test.clj (new), and the
merge's changes to reads_test.clj, read_exit_test.clj, read_model_test.clj
and micro_test.clj, against the seams of IMPLEMENTATION_VALIDATION-wave1.md
(S1 to S13), IMPLICIT_SPEC.md (I-L4, I-L5, I-L6 with D12, I-L7; OP9, OP10;
RD2, RD3, RD4, RD6; I-P1), the forget ruling and ruling 7 (PROGRESS.md
"Now"), and defaults 1, 4 and 6. The protocol read again for this: the
docstrings of `reads/purge>`, `reads/open-row>`, `reads/open-row-with>`,
`locks/open-row-with>`, `locks/lease-under`, `locks/dying>`,
`locks/fan-out>`, `gate/stamp-for`, `gate/plain-texts`,
`gate/stream-refusal`, `micro/forget-effect`,
`micro-client/forget-value!`, `rx/read!`. Each test the brief named as an
example is marked (E1 to E5). -->

## Findings (missing cases found by this validation, added to wave1_test)

| # | what was missing | added |
|---|---|---|
| T-W1 | S3's date rule (IMPLEMENTATION_VALIDATION, plan conformance item 2) had no test: a value needing two persons, closed by the first person's forget, must keep that date when the second is forgotten; the purge the second forget makes again must carry it, not its own stamp | wave1_test, "two forgets of one value, in both orders ...", the `qr` value |
| T-W2 | the two orders of a value forget and a person forget over one value were not tested on the index side: a value forget first must keep its date and not be purged again by the person forget (`dying>` skips a ledger entry); a value forget after the person forget must re-date the index to its own stamp, as the open step's ledger-first reading does (L16, OP9's edge case) | the same block, the `vq` and `pq` values |

Both are assertions in a new `testing` block of the existing namespace; no
restructuring.

## Minimize IPC launches

- `rig.store.wave1-test`: one cluster deftest (`wave1-seams`, one
  `create-ipc`, one `launch-module!`), every seam a `testing` block on
  disjoint entities, layers and persons (`:w1a`, `:w1p`, `:w1t`, `:w1g0`
  ...); the one other deftest is pure (the stamp seam), no cluster. The
  person forget is irreversible and global, so it uses its own person
  (`:w1p`) and layer; the crash comes last.
- The merge added no cluster to the other namespaces. The suite launches the
  clusters it did before, plus this one. PASS.

## Seam coverage (each wired seam, and the rule it must keep)

**S2, a value forget's purge (E1: "a value forget purges its index entries
in the same event, and a read through the exit afterwards cannot confirm
the value").** wave1_test, "a value forget purges its index entries in its
own event ...": before the forget the exit's `[:kv]` read confirms the
value and its text is in an `:ix-kv` address; right after the forget's
answer (no wait: OP9 "complete at the answer") no address holds its text,
no `:ix-of` entry names it, its two id entries are tombstones dated by the
forget's stamp holding no `:v`, `:sealed`, `:lock` or `:digest`, a scan of
the four fields finds the text nowhere (I-L4, I-L5), the act's other fact
keeps its entries; through the exit the `[:kv]` read returns nothing, its
fingerprint differs from the first (RD6), `[:e]` shows the fact with its
date and no value, a point read the same, and the entry records `:erased`.
"In the same event": the crash block ("the purge is the forget's own
event"): a crash armed after the forget's writes (the purge's included)
discards both, the replay writes both; the ledger and every tombstone carry
the committed stamp. And the recorder shows exactly one purge call, from the
forget, with exactly the erased value and its date. Also the read exit's
T19 now erases with a real forget and checks the value entry gone. PASS.

**S3, a person forget's purge (E2: "a person forget purges every value
dying with them").** wave1_test, "a person forget purges every value that
dies with the person ...": five values about `:w1p` on two tasks (the
layers' homes are printed: `:base` on another task than `:alice` and
`:w1p-layer`): three that die (the owner's own layer, owner required; a
`:die-with-any` mark in Alice's layer; a base value about `:w1p` alone,
any-of) and two that survive (an unmarked mention in Alice's layer, 7b; a
base value about `:w1p` and Alice, any-of). Right after the forget's answer:
each dying value was handed to the purge, dated by the forget, has no
value-index address or `:ix-of` entry left, its id entries are tombstones
with that date, and `[:kv]` cannot confirm it; each survivor was not
purged, keeps its value entry, and the exit still confirms it; the purged
set is exactly the dying set (both directions, R8's reading). Also forget_
test's A cases (phase 2's): every value the model says died went to the
purge, none that stays open. The dates across two forgets of one value, in
both orders, and a value needing two persons (T-W1, T-W2): "two forgets of
one value, in both orders ...". PASS with T-W1 and T-W2.

**S1, the open step (E3: "a read through the exit opens sealed values and
shows 'erased on this date' after a forget").** wave1_test, "the exit opens
sealed values through phase 2's open step; time travel ...": the value is
stored sealed (bytes, a lock id, no text); the exit shows it opened; after
a replacement and a forget, a read as of the first stamp shows only the
first fact, erased on the forget's date though the forget is after the
moment, with no value, and neither the replacement nor the forget fact
(I-L7, RD4); a point read as of that moment shows the date and the
replacement absent (RD2); now the replacement is open. The read exit's own
tests run over sealed values (T2 to T19), and read-model-test's histories
compare 0 differences beyond the named ones. PASS.

**S4, the value index's text.** reads_test `gate-integration`: a sealed
offer decided with a lock context gives rows with no text, `plain-texts`
gives the opened texts, and the index lists equal `index-writes` over
those texts, with the `:ix-kv` address built from the opened text; a value
that does not open gives `:does-not-open` and no index writes.
`generated-offers`: 300 generated offers sealed, decided, their index
writes replay-equal, one value entry per note with a value. PASS.

**S5, a sealed entry's moment.** wave1_test `a-sealed-read-entry-is-
stamped-after-its-moment` (pure): an entry sealed at the door whose line
records a moment 100 s ahead is stamped after it, and without the opened
value (`stamp-for`'s 4-arity) the moment would not count. The read exit's
T18 (cluster): an entry decided on another task is stamped after its moment
and after every matched stamp. wave1_test's entry block asserts it again.
PASS.

**S6, the exit's entries at the door (E4-like: "the exit's entries lease
and seal at the door, like any offer").** wave1_test, "the exit's read
entries lease and seal at the door like any offer": the entry's rows are
sealed in the log; every depot copy of the entry holds `:sealed` and no
`:v` (default 1: no plaintext line in the depot); its lock was leased in
the working layer, by a lease act answered by name; the lock was consumed
at the entry's decision; the entry is stamped after its moment; and the
entry can be forgotten like any value and then reads erased. The agent's
case (W-V2): "an agent's reads are recorded in its person's session
layer": the agent's read returns rows, its entry's `:who` is the agent,
its unconsumed lease rows are sealed under Alice. The read exit's T13 and
T15 (agents and tools reading) pass on it. PASS.

**S9, `:members` (E5: "a group's making act passes with `:members`").**
wave1_test, "a group's making act passes with :members ...": the making
act is a yes, the members are projected, the sealed parse takes the act,
`:members` is a control key, and a plaintext value fact at the same gate is
still `:not-sealed`. "`:members` ... at the stream gate it is refused":
`:control-not-allowed`, nothing written (W-V3). micro-prepare-test's
"members only in the making act" (which failed on the merge before the
fix) passes. PASS.

**S10, persons.** wave1_test, "persons are phase 2's acts on the micro
side": a person made through the micro door has a lock on every task before
the answer, the micro gate reads the same `$$persons`, and its forget is a
fact dated by its stamp. micro_test's seed, lease and A cases run through
phase 2's acts. PASS.

**S12, a shared layer's value forget (E4: "a group layer's value forget
reaches its lock in the record").** wave1_test: "a value forget in the
group reaches its lock in the record": the value's lock is in the record
and has no lock row; after the forget the record's lock is excised, the
sealed bytes stay (opened by nothing), the ledger on the value's entity is
`{:stamp f :how :excised}`, the value reads erased on that date and the
act's other value stays open; a second forget is a yes that changes
nothing (the first date stays; OP9's concurrency line). "an :own-row value
in the group": its lock row deleted, `:row-deleted`. "a forget of no value
... :no-such-value", and a forget naming another entity. "per-act grain:
one lock for the act across entities, forgotten as a whole": two entities
on two tasks (printed), one lock for both values, the name row keeps both
entities, one forget excises and dates both, and the other entity's value
reads erased (ruling 7; I-L6's D12 part, reported as a difference from the
model). PASS.

**S7, `read-as-of` internal.** Contractual (IMPLEMENTATION_VALIDATION,
item 10): no test can show a contract. The tests that use `c/read-as-of`,
`c/opens?` or `c/facts` do so as the store's own view: forget_test,
read_exit_test's `line`/`log-acts`, stream_gate_test, the smoke test; no
test reads a value for a reader through it. PASS as scoped.

**S8, the put page's cap.** A measurement, not a test (the bench,
BUILD_NOTES-wave1.md). Not a suite member by the brief. PASS as scoped.

**S11, R19.** micro_test's permission cases (the walk on both gates, the
revoke cutting below) run on the merged `refusal`. PASS.

**S13, the micro fold with phase 2's control rules.** micro-prepare-test
(34 tests with the lock tests and envelope tests, 960 assertions) and
micro_test run the fold over `gate/refusal` as merged. PASS.

**Adapted, not weakened.** Each changed expectation in the older namespaces
is marked in place and has its reason: read_exit_test T3 (`:no-such-lock`
where a lease into a layer never made is refused, For Sid 14);
read_model_test's named exceptions (For Sid 14, 2, 16), counted in an
OBSERVED line, every other answer and read compared exactly; micro_test's
chain case (asserted on the lease act, For Sid 14) and its byte-identical
row (compared by its bytes). Removed with the code they tested: the
`open-double` assertions of reads_test's `open-row` deftest (the hook no
longer exists; the read exit's own notes said the merge drops it); the rest
of that deftest now checks the same shapes through phase 2's pure half,
plus three sealed-row cases (a record lock opens, a ledger dates an erasure
after the moment, a forgotten owner closes the wrap). No failing assertion
was deleted to pass.

## Implicit spec coverage (the lines wave 1 touches)

- I-L4 (no plaintext after the lock is gone): the value forget block's four
  field scan; the entries' depot copies sealed. PASS.
- I-L5 (every index purgeable by value id; forget reaches it): S2, S3. PASS.
- I-L6 with D12 (per-act, whole; across tasks): the per-act block. PASS.
- I-L7 (as of a moment, only the erasure's date from after it): S1's block,
  T8. PASS.
- OP9 (complete at the answer; row deleted or excised; the date is the
  forget's stamp; a second forget changes nothing; a value not in the layer
  refused): the value forget and group blocks. PASS.
- OP10 (every value whose wrap needs the person closes; the others stay;
  complete at the answer): S3's block. PASS.
- RD6 (a re-run cannot reach the fingerprint): the value forget block. PASS.
- I-P1 at the stream gate after the merge (a chain in another layer):
  micro_test, on the lease act. PASS.

## Synchronization

- Stream gate writes: every write is `foreign-append!` with `:ack` (the
  door, `offer-until-answered!`), which returns once the event tree is
  complete, the person fan-out's children and their purges included; every
  read after it is the claim under test (OP9, OP10: "complete at the
  answer").
- Micro writes: `mc/offer!` returns only when `micro-lookup` answers at a
  frontier at or past the act's batch, so every task committed it; the raw
  reads after it (`mc/row-of`, `mc/ledger-entry`, `mc/lock-row`, the name
  row's `:entities`) read committed state. `mc/open-act` reads through the
  name task's frontier (the micro build's own fix).
- After the injected crash: `wait-until` on the forget's record, then reads
  through `retrying`, as phase 2's crash blocks do.
PASS.

## Test namespaces compile

All fourteen namespaces load together with the test classpath (a scratch
loader requiring each, 05:20 IST: `rig.smoke-test` ... `rig.store.wave1-test`).
No private namespace required; `micro/parse-micro` is public. PASS.

## Verdict

**minor-fail**: two missing cases (T-W1, T-W2), added as one `testing`
block of `rig.store.wave1-test` before the suite's final runs; with them
every check above passes, and each seam has a test that asserts what the
rulings require of it.

PHASE_VALIDATION:minor-fail
EOF
echo ok