# Review of wave 1: a falsification pass (26 September)

An independent reviewer's attempt to break wave 1 (`2034cce5`: phase 2's
locks and forgetting and phase 3's micro store, merged and wired to the
one-owner read exit). The builder's validations were self-reviews in one
long session; this pass looked where they did not. Branch
`rig-review-wave1`, worktree `/mnt/data/projects/Softland-rig-review-wave1`,
Claude Opus 5.5. Every cluster run under
`flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock`.

The tests are `rig.store.review-wave1-test`: one in-process cluster
(4 tasks), one block per attempt. A finding counts only with a test that
fails before its fix and passes after it. Two findings would change what a
kept record carries. They are not fixed; their blocks are marked PENDING
and assert the rulings' reading only with `RIG_PENDING=1`, printing
OBSERVED lines otherwise. Run 1 (`runs/review-wave1-try1.log`, with
`RIG_PENDING=1`) had 66 assertions and 6 failures: R-2 (1), R-1 (2 at the
stream gate, 2 at the micro gate), and F-1 (1).

What was read: the rama skill (design rules, the single-threaded task,
yielding and replay); SPEC.md; RIG.md's defaults 1, 3, 5 and 6; PROGRESS.md
"Now" on main (the forget ruling, rulings 7 and 7b, the sharpenings, the
rig constraints); BUILD_NOTES-wave1.md whole; both wave 1 validations; the
code by the notes' windows (module.clj whole, and the seams in locks.clj,
reads.clj, gate.clj, envelope.clj, micro.clj, client.clj and
micro_client.clj); the model's control keys and `admit`.

## Findings

### F-1 (medium): after a refused lease the micro door sent its value act sealed under no lock. Fixed.

Phase 8's replays found it (D1, D2; `rig-build-replays` `d437dc17`), and
the coordinator passed it on. This review reproduced it.

- **Test.** Block "F-1". Bob writes a value into the group citing a
  permission kept in the base. His lease is refused
  (`:permission-does-not-cover-this`). The value act then came back
  `{:answer :no :reason :not-sealed :face true}`, where the stream door and
  the plans give `:no-such-lock`. It failed in try 1 and passes in try 2.
- **Cause.** `micro_client.clj` `write!` (298): on a refused lease `ks` is
  `{}`, so `seal` sealed each value under `(get locks lid)`, which is nil.
  `locks/seal` returned nil, and the envelope carried `:sealed nil`. The
  micro gate's `not-sealed?` (micro.clj 241 to 253) refuses that on its
  face as `:not-sealed`.
- **Fix,** `3940ff8d`. As the stream door's `assign!` does (client.clj
  155), the door cites the ids the refused lease would have minted and
  seals under locks it throws away. The gate finds no lease row and answers
  `:no-such-lock` on its face. `:locks` still returns only the locks
  taken. Reran: review-wave1-test and micro-test, 2 tests, 337 assertions,
  0 failures (`runs/review-wave1-try2.log`).
- **Not fixed alongside it.** A lease answered yes whose `take-locks` times
  out after 30 s still seals under no lock. The stream door throws after
  its tries instead. It is rare, and not wave 1's to decide.

### R-1 (high; first-record, PENDING): a fact under a control key that the gate does not act on keeps its value as plaintext, and a forget answered yes erases nothing. This is W1-7, widened.

The builder asked whether this matters beyond `:members`. It does, as
follows.

- **Test.** Block "R-1 PENDING" at the stream gate and "R-1 PENDING at the
  micro gate". Alice writes free text under six keys on entity `:rv1` in
  her personal layer.
  - **The stream gate** admits all six: `:owner`, `:kind`, `:class` and
    `:lock-grain` (on any entity but the layer's own), and
    `:promote-request` and `:crossed` (anywhere). The text is plaintext in
    three places: the `*offers` depot, the log row's `:v`, and both
    id-index entries (`:ix-ek` and `:ix-ke`). The exit shows it.
  - **The owner's forget** of the `:owner` fact answers
    `{:answer :yes :how nil}`, and the exit shows the text after it as
    before. A person forget cannot reach it either, because it has no lock.
  - **At the micro gate** Bob writes `:owner` on another entity in the
    group. The gate admits it: plaintext in `*micro-offers` and in the
    row's `:v`. The operator's forget answers yes, and `open-act` still
    shows the text.
  - **Who can do it:** any writer who holds a permission in the layer,
    agents included.
- **Cause.**
  - `envelope.clj` `control-keys` (43) keeps the value slot plaintext for
    every key in the set, wherever the fact sits (`value-slot`, 316), and
    the door does not seal them.
  - `gate.clj` `control-fact?` (59) checks only setting facts on the
    layer's own entity, grants, revokes and the lock control keys.
  - `stream-refusal` (228) refuses only `:members`
    (`micro-control-keys`, 219).
  - `micro.clj` `foreign-control-keys` (51) has no setting keys.
  - `locks.clj` `forget-writes` (779, the `{:how nil}` at 785) and
    `micro.clj` `forget-effect` (785, 809) answer yes for a row with no
    lock id.
- **Against the spec.** The model's control keys are seven
  (model.clj 54 to 57), and it locks every other fact (`admit`, 488). So
  a `:kind` or `:owner` fact on another entity is a sealed value in the
  executable spec, and plaintext in the rig. The model's `:class` and
  `:lock-grain` facts act on the layer's settings whatever their entity.
  Nothing in the model writes free text under a control key: "their values
  are ids and settings". Also, any writer can forge `:crossed`, phase 4's
  crossing fact, at the stream gate today.
- **Why not fixed.** Which keys are control keys, and what a record may
  carry under them, is first-record (the builder's question 3). OP9's edge
  case already says a fact with no lock "must stay readable either way"
  and leaves the forget's answer open (O1).

### R-2 (high; first-record, PENDING): a read entry's recorded `[:kv]` pattern pairs a forgotten value's text with its fact id, and no forget reaches it.

- **Test.** Block "R-2 PENDING".
  - `:rv-b` writes a value about itself alone into the base (7b as
    written: any-of `[:rv-b]`).
  - Alice reads the base through the exit with `[:kv :note text]`, and it
    matches.
  - `:rv-b` is forgotten. The value index no longer confirms the text (the
    purge holds), and the value reads erased on the forget's date.
  - Alice's own entry, read back through the exit, still reads
    `{:pattern [:kv :note "rv2 a value about rv-b alone"] :exact [[fid stamp]]}`.
    It is the forgotten value's text beside its fact id.
- **Cause.** `reads.clj` `entry-facts` (843) records `(:pattern answer)`.
  For `[:kv]` that holds the value in plaintext (`parse-pattern`, 540). A
  person's or a model's line also carries the exact list. The entry is
  sealed under the reader's working layer, so the subject's forget, and a
  value forget of the matched fact, cannot reach it. The reader can still
  forget the entry itself, if she knows to.
- **Against the spec.** The forget ruling ("gone for everyone including the
  past"). I-L4 ("nothing the store keeps holds that value's plaintext ...
  not a follow-on offer carrying a copy"). R1's rider: fingerprints cover
  the ids of the matched facts, not their values. The entry's fingerprint
  keeps that rider; its pattern does not. The sharpening's "a short read
  entry cannot re-run to its fingerprint; expected" covers the fingerprint,
  not a stored pattern.
- **Why not fixed.** What an entry carries is first-record (FR5 to FR8,
  default 3). One road that keeps "reads that happened cannot be unread":
  store a `[:kv]` line's value keyed, as the fingerprint is (an HMAC under
  the fingerprint secret), so the line still re-runs and compares but
  confirms nothing to a reader.

### Q (low; the ruling is open, O11; observed only): the two gates disagree about a mention of an already forgotten person in one layer across its re-class.

- **Probe.** Block "probe (O11 ...)". Alice's unmarked mention of the
  forgotten `:rv-b` in her new personal layer `:rv-pl` is admitted by the
  stream gate. After the layer's re-class, the same act at the micro gate
  is refused `:person-forgotten`, although its lease is admitted.
- **Cause.** `micro.clj` `persons-to-check` (440) requires every person in
  the act's subject union alive. `locks.clj` `persons-refusal` (706)
  requires only the wrap's persons alive: in a one-owner layer, the owner
  unless the value is marked. Implementation validation item 8 ("the micro
  fold and phase 2's `decide`, reconciled") missed it.
- **Why not fixed.** Writes about someone already forgotten are open
  (O11). Only one rule for both gates is asked for.

## What held, one line each

- **A value forget's lock writes and its purge commit together.** A crash
  thrown inside `purge>`'s recorder, after `write-decision>`'s transforms
  and before the purge's own writes, discards both. The replay writes both,
  and the ledger, the lock row's deletion and the tombstones all carry the
  committed stamp. No value-index address is left.
- **After a value or person forget no read confirms or opens the value.**
  This holds for point and pattern reads, as-of and current, through the
  exit and below it (`read-as-of`, `opens?`). `[:kv]` finds nothing, and
  because the open step reads the ledger first, even a stale or planted
  index entry reads erased.
- **A person forget purges every value that dies, on every task, before
  its answer** (wave 1's own test). In an agent layer (record locks), 7b
  held: an unmarked value about Alice and `:rv-q` survives `:rv-q`, and a
  marked one dies.
- **A person forget whose fan-out child crashed was closed at its answer.**
  With the answer found by name (RD1), as the door finds it after an append
  error, five of five tries: four probe tries recorded the append throwing
  and the answer taken by lookup, and one went through
  `offer-until-answered!`. At each answer the value was erased and the lock
  gone on its task. The window is reasoned: the home's record commits at
  `(|all)` before the children run. It never showed.
- **A person forget resent after a later value forget of a value it
  closed** does not re-date or re-purge that value. It purges the other
  dying values again, idempotently.
- **A rebuild after forgets writes nothing back.** After value forgets (a
  deleted row, an excised record lock) and person forgets, a rebuild of
  `:alice`, `:alice-agent` and `:base` deleted 0 and rewrote 0, and no
  forgotten text is in any index field. The purge writes what a rebuild
  implies.
- **Sealing reaches group layers and the base.** Value facts are sealed at
  both doors and refused unsealed on the face by both gates. Neither
  depot carries a lock. The exceptions are R-1's control keys.
- **W1-1.** An agent's unconsumed lease rows are sealed under its layer's
  owner (Alice), and only the gate holds the lock that opens them.
  Separately, and not W1-1's doing: `lease-locks` returns the 63
  unconsumed locks in plaintext to any caller that names the layer and
  `:door/rv-agent`, a session derived from the agent's id. `micro-lease`
  does the same by lease name (read from the code, not run). This is the
  rig's no-caller-identity limit.
- **Throws** (reasoned from the code, not run). No `case` without a
  default, and no nil dereference, in wave 1's new topology code: `purge>`,
  `dying>`, `fan-out>`, `open-row-with>`, `lease-under`, the micro
  forget's gather and its block 2a writes. Every write takes a schema'd
  row's fields or a guarded value.
- **Replays** (the resend was run; the rest is reasoned). A second
  tombstone cannot differ from the first. Its address comes from e, k, the
  record's stamp and the fact id. Its date comes from the ledger, or from
  `wrap-closed` over the same person entries. A value forget's record path
  writes nothing, and a person forget's fan-out is idempotent.

## The suite

One full run at the end, from the rig folder, on this branch with F-1's
fix: wave 1's 14 namespaces plus `rig.store.review-wave1-test`, without
`RIG_PENDING`:

```
flock /mnt/data/projects/rig-relay-2026-09-26/cluster.lock clojure -M:test rig.smoke-test rig.claims-test rig.revision-test rig.store.clock-test rig.store.envelope-test rig.store.stream-gate-test rig.store.reads-test rig.store.read-exit-test rig.store.read-model-test rig.store.lock-test rig.store.forget-test rig.store.micro-prepare-test rig.store.micro-test rig.store.wave1-test rig.store.review-wave1-test
```

It finished at 06:26:32 IST, after 11 min 14 s including the wait for the
lock: **94 tests, 5,630 assertions, 0 failures, 0 errors**
(`runs/review-wave1-suite.log`, git-ignored). The other runs were try 1
(the review namespace with `RIG_PENDING=1`: 6 failures, as listed above),
try 2 (the review namespace and micro-test after F-1's fix: 337
assertions, 0 failures) and try 3 (the review namespace with the two
probes: 77 assertions, 0 failures).

## For RIG.md

**New rig choice (the orchestrator assigns the R number).**

- **F-1.** After a refused lease, the micro door seals under throwaway
  locks citing the ids the refused lease would have minted, so the gate
  answers `:no-such-lock` on its face, as the stream door does. No
  record's form changes.

**Questions for Sid.**

1. **Which keys are control keys (R-1; first-record)?** Today any writer
  can keep free text, as plaintext and beyond every forget, under
  `:owner`, `:kind`, `:class` and `:lock-grain` on any entity but the
  layer's own, and under `:promote-request` and `:crossed`, at the stream
  gate, and under the four setting keys at the micro gate. A forget of
  such a fact answers yes and erases nothing. There are two roads:
  - refuse a control-key fact the gate does not act on
    (`:malformed-control`); or
  - treat the key as a value, sealed, away from the position the gate acts
    on. That is the model's reading for `:kind` and `:owner`.

  A second question under it: should a forget of a fact with no lock be
  refused rather than answered yes? OP9 leaves this open (O1).
2. **What does a `[:kv]` read entry carry (R-2; first-record)?** Today it
  keeps the matched value's text beside its fact id, beyond the subject's
  forget. Should a line's pattern value be keyed, as its fingerprint is?
3. **One rule for writes about someone already forgotten (O11)?** The two
  gates disagree across a re-class: the stream gate checks the wrap's
  persons, the micro gate the whole subject union.
4. **A caller identity for the door's road, before a kept store?**
  `lease-locks` and `micro-lease` hand plaintext locks to any caller that
  names a session or a lease name, and default sessions are derived from
  the writer's id. The operator's test-only index ops (`:put` with
  `:copy?`, `:drop`, `:purge`) are live on `*index-ops`. `:put` can plant
  a copy of a row's record lock at an address that no purge visits. The
  open step still shows the value erased, but the copy stays in the store.
5. **For wave 2 (phase 4).** A client can write `:crossed` and
  `:promote-request` today as ordinary plaintext facts at the stream gate.
  Phase 4's gate must refuse a client-made `:crossed`.
