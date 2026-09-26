# Full-spec review of the rig (26 September)

The rama skill's full-spec review of the whole module against SPEC.md, the
plans, and RIG.md's rig choices, defaults and rulings. Reviewer: Claude
Opus 5.5, for builder C. Branch `rig-review-full-spec`, worktree
`/mnt/data/projects/Softland-rig-review-full-spec`, made at
`rig-2026-09-25`'s head `13c8360b` (step R landed, phase 8's pass 3 landed,
step 6b in flight in Codex).

## How this review differs from the skill's

The skill's full-spec review (`phase-full-spec-review.md`) is one session
that finds, fixes, reruns the suite and loops until a pass finds nothing.
This one finds only. C's instructions: no code changes, no fixes, no test
runs of any kind (Sid's rule, about 08:20: test only when it is needed; a
review is done by reading). So every finding below is reasoned from the
code and says so; where an existing test already exercises a path, the
test is named. By the skill's own rule the verdict is `fail`, because
items are outstanding and nothing was rerun; that verdict is the skill's
form, not a judgment that the rig is unsound.

## Scope

Everything on the branch but step 6b's path, which Sid is rebuilding in
Codex and which gets its own review: `micro/control-keys`, the micro fold's
two opens `arrival-open` and `row-wraps` (their use of the compiled
grammar), the compiled constants `grammar/grammars` and `reads/seed-hints`
on the micro side, and the micro side of the machinery count. Where a
finding touches 6b's ground it says so. Phase 7's harnesses are on
`rig-build-numbers`, not on this branch, and were not reviewed; phase 7's
plan was read only for what its workloads touch.

## What was read

- The spec and its sources, whole: SPEC.md; PROGRESS.md "Now" (lines
  9-195); the formal model's README 42-152; `model.clj`; `scenarios.clj`.
- IMPLICIT_SPEC.md sections 0 to 3, every operation and read, and the open
  list O1-O23 (the entity matrix consulted where a finding needed it).
- RIG.md whole: the Overnight state, Found tonight, For Sid 1-68, the
  defaults 1-8, rig choices R1-R97. REVIEW-wave1.md whole.
- The module source whole, every namespace under `src/rig/store/`, and
  `src/rig/revision.clj` by its entry points and its git road.
  `src/rig/claims.clj` (phase 0's check) and `src/rig/bench/lock_slice.clj`
  (phase 7's slice) were not reviewed as store code.
- The tests by targeted reads, and a read-only gatherer's map of which
  tests cover each test the spec names (Sonnet; three of its citations
  spot-checked: `stream_gate_test.clj` 494-498, `promote_test.clj`
  492-507, `forget_test.clj` 612-625). `gate_test.clj` is not in this
  worktree; nothing opened it.
- The plans and their validations by targeted reads where a finding
  needed the plan's decision.

## Severity

- **High**: a ruled path the store core would take stops working, or data
  is exposed or corrupted, on a path an ordinary store takes.
- **Medium**: a spec clause or the model's decision is broken on a narrow
  path, or a pick contradicts a ruling and has not reached Sid.
- **Low**: a gap or inconsistency with no wrong outcome today, or a
  reading that has not reached Sid.

"First-record" means the finding bears on what a kept record would carry.

## Findings

### H-1 (high; first-record): once a layer that is someone's working layer is re-classed, every read entry into it is refused, so the one exit shows that reader nothing

Reasoned from the code, not run. No test re-classes a working layer.

- **Where.** Every act the read machinery writes into the working layer is
  built for the stream gate and tagged by layer:
  - `read_exit.clj` 101 and 124: the read entry, `:class :by-layer`, named
    `(env/make-name working :by-layer)`, sent by the stream door
    (`client/offer-until-answered!`, 134);
  - `standing.clj` 36, 73, 112 and 126: every standing-read line, the same;
  - `read_exit.clj` 203 and 219: a session close with `:reads`, and each
    dropped entry's forget act, `:class :by-layer`;
  - related, on phase 6's stream side: `recipe.clj` 267 names every tool
    run `[layer :by-layer :offer id]` (T-FR5, first-record), while
    `runner.clj` 155 takes the act's class from the layer's settings.
- **What happens.** A re-class moves a one-owner layer to the micro gate,
  and the stream gate then refuses a by-layer offer into it
  `:class-mismatch` (`gate.clj` 224). `micro_test.clj` 405-420 does exactly
  this to the agent session `:alice-agent` and then writes into it at the
  micro gate, green. The exit's next entry into that layer goes to the
  stream gate on the layer's home and is refused either way:
  - if the door still holds locks it leased before the re-class, the
    entry is decided and recorded `:class-mismatch`;
  - otherwise its lease act, also `:class :by-layer`, is refused
    `:class-mismatch`, the door seals under throwaway locks
    (`client.clj` `assign!` 176-207), and the entry is refused
    `:no-such-lock` on its face.

  `read!` then returns `{:refused r :entry nm}` and shows nothing
  (`read_exit.clj` 136-142), since nothing is shown before the entry is
  acknowledged. The same holds for every standing-read line. A drop cannot
  run either: each dropped entry's forget is `:class-mismatch` at the home,
  and a close with `:reads :drop` sent to the micro gate is
  `:malformed-control`, since `micro/close-ok?` (354-359) takes only
  `{:session s}`. A tool run in that layer is refused `:mis-tagged` on its
  face (a name tagged `:by-layer` on an act of class `:by-entity`), and
  the runner's own `[:k :tool]` read is refused first.
- **Why it matters.** Default 4 records an agent's reads in its own
  session layer ("Agent session reads are recorded there, and kept or
  dropped when the session closes"), and an agent session writing
  continuously is the hot layer ruling 2's re-class exists for
  (IMPLICIT_SPEC OP7: "typically an agent session writing continuously").
  After the re-class the agent can still write into its session, at the
  micro gate, and can read nothing through the one exit. A person who
  re-classes her own layer loses her reads the same way. The plan assumed
  this could not happen: PLAN-reads-rest.md 341-344, "entries are written
  into the reader's working layer, always one-owner, FR3", and 1066, "the
  reader's working layer, a one-owner layer on the stream gate". A
  re-classed layer is one-owner in kind but placed by entity; even with
  its entries routed to the micro gate, RR4's `open-entry>` would answer
  their `:no-copy` rows `{:unreadable :no-copy}`, so the close and the
  drop would need the same repair.
- **Against the spec.** SPEC phase 3: "Re-class of a hot layer moves it
  here." Ruling 3 (PROGRESS 78-80): "a model's or a person's reads are the
  crossing's exact list, always, because there is no re-run." Default 4.
- **First-record.** Yes. FR3 ("an ordinary act in the reader's working
  layer ..., through `*offers`") and FR4 fix the entry's gate and the class
  its name carries (For Sid 3); T-FR5 fixes the run name's `:by-layer`
  tag. An entry into a re-classed working layer needs a name tagged
  `:by-entity` and the micro door, and a kept entry carries its name.
- **Phase 7.** Its workloads never re-class (PLAN-numbers.md mentions
  re-class once, at 613, quoting README), so this does not block the runs.
  It does qualify number 3's verdict: re-class is "the way out for a hot
  layer" the verdict points to (README.md 121), and today that way out
  ends the layer owner's reads when the layer is also a working layer.

### M-1 (medium; first-record): at the micro gate a value can be answered yes and then written with no lock, and every read of it shows `:does-not-open`, never "erased on" a date

Reasoned from the code. The write is unit-tested; the read of such a row is
not.

- **Where.** `micro.clj` block 2b: `row-wraps` 1138-1170, `wrap-live`
  1185-1195, `fact-rows` 1197-1240 (the closed row at 1219-1220, `:lock`
  at 1233), and its second read of `$$persons` at 1603-1618; the read side,
  `shared_reads.clj` `open-entry>` 700-721 and `locks.clj` `open-with`
  501-528; the person purge, `shared_reads.clj` `person-purge-page>`
  1156-1188 with `live-for?` 910-914.
- **What happens.** Block 1 opens the act's values on the arrival task with
  the person entries it reads there, and the fold answers yes. Block 2b
  reads `$$persons` again to wrap the lock, and that read is "a read at a
  moment, so it can differ from block 1's read" (PLAN-micro-store.md
  320-327, PV-F6): a person forget's fan-out child can run on the task
  between the two blocks, since the stream topology shares the task
  thread. Two cases follow.
  - The lease act's writer was forgotten in between: `unlease` gives nil,
    the value's row is written `{:lock nil :digest nil}` with its lock id,
    and block 2c consumes the lease row, so the value's lock exists
    nowhere.
  - Every person the wrap needs was forgotten in between (a required
    person, or the last of the any-of people): `locks/wrap` gives nil, so
    the row is written the same way, and for a row lock (a re-classed
    personal or hand layer, or `:own-row`) no lock row is written.

  No ledger entry is written, and the answer stays yes. Every read of the
  value then reaches `locks/open-with` with no ledger entry and no record:
  `erasure` is nil, `unwrap` of nil is nil, and the read shows
  `{:unreadable :does-not-open}`. `open-with`'s docstring (512-513) says no
  write produces that result; PV-F6 produces it. The date cannot be
  recovered later, since the row keeps neither its wrap nor a ledger entry.
  And the person purge never finds the row, because `live-for?` looks for
  the person in the entry's lock record, which is nil; for a key indexed by
  value the keyed digest stays in `:ix-kv` until an operator's value forget.
- **How the other gate and the model decide.** The stream gate reads the
  persons, decides and writes in one event, so the same race cannot happen
  there: a forgotten lease writer makes the delivery fail (`:no-such-lock`
  on its face) and a forgotten wrap person is recorded `:person-forgotten`;
  it never admits a value it cannot lock. The model locks at commit with the
  wrap as data (`model.clj` `micro-commit` 748-753, `lock-for` 371-389), so
  a value about a person forgotten between prepare and commit reads
  `:erased-at` the forget's stamp (`erasure` 391-398, `read-as-of`
  1001-1021), and a group note about no one survives Bob's forget
  (scenarios A3, 36-38). In the rig, the same group note leased by Bob and
  caught by the first case is admitted and then lost to everyone, with no
  date.
- **Against the spec.** SPEC phase 2: "time travel shows 'erased on this
  date' and nothing else from after its moment." SPEC's sources, item 3:
  "The rig's gates must decide the same way." Scenario A3.
- **Tests.** `micro_prepare_test.clj` 619-640 asserts the rows block 2b
  writes (`:lock nil`, `:digest nil`); nothing reads such a row back.
- **First-record.** Yes: a kept log row of a yes act holding sealed bytes
  and a lock id with neither a lock nor a ledger entry, and no erasure date
  to recover.

### M-2 (medium; first-record): ruling 9's default visibility is compiled code, where the ruling makes it seed policy facts, and RIG.md does not carry the pick

- **Where.** `reads.clj` `visible?` 578-589 (the one-owner exit: personal,
  hand and agent layers to their owner, the base to any actor, every other
  kind to no one) and `shared_reads.clj` `visible?` 296-314 (a group by its
  `:members` row as of F). Nothing under `src/` writes or reads a policy
  fact.
- **Against.** Ruling 9 (PROGRESS 105-107): "Default visibility: base open
  to any authenticated actor; a person's own and session layers private to
  that person; group layers visible to the group's members. Seed policy
  facts, so a store can differ." IMPLICIT_SPEC I-P2: "Seeded as policy
  facts, so a store can differ. Every read below applies it." Sid's frame
  (PROGRESS 38-40): "the menu is baked in code, the default lives in the
  first facts, the pick is a fact on a layer, key, tool or value." SPEC.md
  gives rig choices to the places where "the rulings are silent"; ruling 9
  is not silent here.
- **Where the pick was made.** PLAN-read-exit.md RC6 (1255-1256):
  "Visibility by ruling 9's default as a constant: ... seed policy facts
  later." RIG.md lists neither RC6 among its rig choices nor a For Sid
  item, so the pick has not reached Sid.
- **First-record.** Yes: ruling 9 puts the defaults in the first facts. A
  store whose first record holds no policy fact decides every read taken
  before those facts exist by code, and the entries of those reads are kept.
- **Beside the count.** Phase 6's count asks only about one tool and one
  grammar, so it does not show this fixed-side piece either.

### L-1 (low; first-record): a refused act's answer record keeps, in plaintext, the people its values name, though none of its values was admitted

Reasoned from the code.

- **Where.** `locks.clj` `decision-reads>` 1205-1225 runs the value checks
  (`read-values`) for every act whose locks were delivered, before the
  decision. `gate.clj` `decide*` 468 hands `(:union (:read lx))` to
  `answer-record` 409-429, whose `:subjects` (427-429) takes the union for a
  no as for a yes. At the micro gate `record-subjects` 892-903 keeps the
  union unless the reason is `:too-many-subjects` (`decide-envelope` 970).
- **What happens.** An act refused after its locks were delivered, say
  `:permission-revoked` or `:stale-replaces`, keeps in its answer record
  every person its values' grammar names. Its lease rows are consumed, so
  its values open nowhere, yet whom they mentioned stays in plaintext, out
  of reach of every forget.
- **Against.** The model's answer has no subjects (`model.clj` `decide`
  513-518: answer, reason, stamp, tick, because-of, digest); subjects sit on
  admitted facts (`admit` 495-496). PROGRESS 139-140: the act's subject slot
  is "the union, for finding", and a refused act leaves nothing to find.
  For Sid 17 raises plaintext subjects for admitted acts and the request's
  `:subjects`, not for refused acts.
- **First-record.** Yes: the answer record's `:subjects` on a no is part of
  every kept record.

### L-2 (low): the one-owner value index keeps each value's text in plaintext as its address, where the shared store keeps a keyed digest

Reasoned from the code.

- **Where.** `reads.clj` `address` 186-198 (`:ix-kv`: key, length, value
  text, stamp, fact id) and `fact-writes` 262-263. The shared side keys it:
  `shared_reads.clj` `address` 42-53 and `kv-digests` 165-183, through
  `reads/kv-digest` (PLAN-reads-rest.md F12).
- **What happens.** For every key a layer's grammar indexes by value, each
  value's canonical text is a PState key on the layer's home. A forget
  purges it (`purge-writes` 298-321), so no read finds it again; but a
  deleted key stays in RocksDB's files, on every replica, until compaction.
  For Sid 25 names that physical reach for a destroyed lock's bytes, which
  open only the sealed copies; the leftover here is the value itself, and
  it needs no lock.
- **Against.** PROGRESS 45-47: "Erasure by destroying a lock, not values
  beside the log." IMPLICIT_SPEC I-L4 (derived): "nothing the store keeps
  holds that value's plaintext". The rig constraint (PROGRESS 167-168)
  allows an index over values that is purgeable by value id, which this one
  is; the point is that the two stores keep different things, and the keyed
  form loses nothing, since a `[:kv]` read is an exact prefix either way.
- **First-record.** No: indexes are rebuildable state.

### L-3 (low): a shared index entry carries a copy of its value's lock row on the layer's task; the reads-rest plan named this reading "for Sid", and RIG.md does not carry it

- **Where.** `shared_reads.clj` `entry-of` 122-129: an entry whose row keeps
  no `:lock` takes a copy of the value's lock row. So every value of a
  re-classed personal or hand layer, and every `:own-row` value in a shared
  layer, has its wrapped lock on the layer's task beside the lock row on
  its entity's task.
- **Against.** SPEC phase 2: "Lock rows in a lock store for personal and
  hand layers, on the same task as their values". The rig constraint
  (PROGRESS 168): "Lock rows sit on the same task as their values."
  PLAN-reads-rest.md 356-365: "A reading of the rig constraint 'lock rows
  sit on the same task as their values', named for Sid: the index keeps, on
  the layer's task, a copy of the sealed value and a copy of its wrapped
  lock together, and the forget's batch nils both copies with the original
  (atomic across tasks)".
- **Checked by reading.** A value forget reaches both in one batch (block
  2a's `:purge` write and the lock-row delete), and after a person forget
  the copy no longer opens, so no forget window opens. The gap is only that
  the reading has not reached Sid.
- **First-record.** No.

### L-4 (low; first-record): the group's rule of ruling 7b is not built, and a group member can never forget a group value, marked or not; RIG.md lists neither

- **Where.** `micro.clj` `micro-extras` 765-773: every `:forget` at the
  micro gate is the operator's (`:control-not-allowed` otherwise, M14), and
  nothing states or reads a group's rule. PLAN-micro-store.md 2124-2127:
  "a group's rule that requires the mark on write (O17) is not implemented,
  and stays open."
- **Against.** Ruling 7b (PROGRESS 101-102): "dies by a mark at write or a
  group's rule." The second consequence (PROGRESS 117-118): "A group member
  cannot forget one value in a group layer themselves unless the group's
  rule requires the mark on write; otherwise it is an excision."
  IMPLICIT_SPEC OP12 and O17.
- **Status.** The model has no group rule and the D cases do not need one,
  so the replays cannot see this. For Sid 41 raises only the owner's forget
  after a re-class.
- **First-record.** Yes: the rule decides how a group's values are written
  (with the mark), and a value written before the rule exists keeps no mark.

### L-5 (low): README.md and five sections of RIG.md still describe the rig as it stood on 25 September

- README.md: the status note (12-18), "At a glance" (145-163: phases 2 and
  3 "designed, not built", 4 to 8 "not started"), "What exists" (405-440),
  and the appendix's R1-R18 (537-561). README says "Read this first".
- RIG.md: "Status, 25 September, evening" (1391-1398: "Phases 2, 3 and 4
  wait"), "Status after phase 1" (1410 on), "Numbers so far" (1975-1997:
  "each small act makes 4 index writes", where PLAN-numbers.md counts 9),
  "The skill's artifacts so far" (2065-2084: phase 2's plan "not yet
  validated", phase 3's "being written"), "What is next" (2085-2107).
- The Overnight state says it wins on build state, so no builder is misled;
  a reader who follows README's "Read this first" is. SPEC.md: "Each phase
  ends with its tests run, `RIG.md` rewritten, and a commit."
- **First-record.** No.

## What held (read, not run)

- **Every test the spec names has a covering test** (the gatherer's map,
  three citations spot-checked): R7's fourteen stream branches in
  `stream_gate_test.clj` 205-787; R8's eight A cases both ways in
  `forget_test.clj` 612-665, whose shared cases run on the base while it is
  placed by layer, and through the micro gate in `replay_test.clj`; D1, D2
  and the revocation race in `micro_test.clj` 318-403 and
  `micro_prepare_test.clj` 417-429; B1 to B4, a retried request landing
  once and a failover between forward and landing in `promote_test.clj`
  (T1 to T7, 353-540); the read entry's parts and the empty pattern read in
  `reads_test.clj` 484-524; a grammar and a tool added as facts only in
  `tools_test.clj`; the fourteen histories in `replay_test.clj` 2064-2081.
- **The refusal order.** The stream gate's is the model's (`gate.clj`
  207-267), with the rig's own reasons where SPEC.md (R13) and the plans put
  them; the micro gate places every reason by one `reason-order`
  (`micro.clj` 61-76).
- **Step R holds.** No control-key fact is admitted that its gate does not
  act on: `gate/control-fact?`, `misplaced-setting?` and `stream-refusal`
  at the stream gate, `micro-extras` and `foreign-control-keys` at the
  micro gate cover the whole of `env/control-keys`.
- **No throw on an offer.** Every function the topologies call on an offer
  is guarded (per function, per envelope in the fold, per value in block
  2b), and every PState write I traced fits its schema.
- **Replays.** Every promotion step answers from its record by name; a
  replayed forward's duplicate landing is decided once (one `+map-agg` key
  in one batch, the record path in a later one).
- **The person fan-out.** `dying>` yields only between reads; the ledger
  check and each value's purge run with no yield between them, and the
  skill's rule ("A topology event's writes ... are atomic — yielding does
  not change that") keeps the purge whole.
- **Partition alignment.** `$$micro [L ...]`'s index fields, `$$layers
  [L ...]`, the read queries and block 2d all route by the layer id; a
  landing's forward appends on the task its lease rows were minted on.
- **The frontier.** Block 0 writes b - 1 in batch b, so a reader at F sees
  only batches every task has committed (SPEC "What Rama showed" 7).

Nothing here was run, and "held" means only that reading found no break.

## For RIG.md

No new rig choice: this review picked nothing. Questions for Sid, one per
finding that asks something of him:

1. **H-1.** Should a read entry, a standing-read line, a drop's forget and a
   tool run follow the working layer's class and gate, named `:by-entity`
   through the micro door once the layer is re-classed? It changes FR3 and
   FR4 (For Sid 3) and T-FR5's name.
2. **M-1.** When the micro gate cannot lock a value it answered yes, should
   the row carry a ledger entry dated by the forget that closed it, so it
   reads erased on that date, or should the gate refuse such a value as the
   stream gate does? And should a value leased under a person forgotten
   before its lock was made survive, as A3 would have it?
3. **M-2.** Seed policy facts now, as ruling 9 says, or accept RC6's
   constant for the store core?
4. **L-1.** Should a refused act's record keep the subjects its values
   named?
5. **L-3.** Is a copy of a lock row in a shared index entry a reading of
   "lock rows on the same task as their values" Sid accepts?
6. **L-4.** The group's rule, and a member's forget of a marked group value.

## Items outstanding

The working for each is in its finding above; this is the index.

| Item | Location | Clause | Direction | Why unresolved |
|---|---|---|---|---|
| H-1 | `read_exit.clj` 101, 124, 203, 219; `standing.clj` 36, 73, 112, 126; `recipe.clj` 267 | SPEC phase 3; ruling 3; default 4 | route by the working layer's class and gate | first-record (FR3, FR4, T-FR5); findings only |
| M-1 | `micro.clj` 1197-1240; `locks.clj` 501-528 | SPEC phase 2 ("erased on this date"); model `erasure`; A3 | date the row, or refuse as the stream gate does | first-record; Sid's reading |
| M-2 | `reads.clj` 578-589; `shared_reads.clj` 296-314 | ruling 9; I-P2 | seed policy facts | ruling-level; not surfaced |
| L-1 | `gate.clj` 409-429, 468; `micro.clj` 892-903 | model `decide`; PROGRESS 139-140 | no subjects on a no | first-record |
| L-2 | `reads.clj` 186-198, 262-263 | PROGRESS 45-47; I-L4 | key the value index | design |
| L-3 | `shared_reads.clj` 122-129 | SPEC phase 2; PROGRESS 168 | carry to For Sid | not surfaced |
| L-4 | `micro.clj` 765-773 | ruling 7b; the second consequence | build the rule, or ask | open (O17) |
| L-5 | README.md; RIG.md 1391-2107 | SPEC "RIG.md rewritten" | rewrite at the fold | documentation |

Verdict: fail. Eight items are outstanding (one high, two medium, five
low), nothing was fixed and nothing was rerun, as C's instructions and
Sid's test rule ask.

PHASE_VALIDATION:fail
