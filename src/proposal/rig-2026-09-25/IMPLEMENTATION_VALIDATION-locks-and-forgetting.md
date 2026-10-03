# Implementation Validation — stage 2, "locks-and-forgetting"

<!-- Phase 4 of the rama skill (references/phase-4-impl-validate.md, template
artifact-impl-validation.md), done inside the build phase (phase-build.md)
by the building session itself, Claude Opus 5.5, 26 September 2026, on branch
rig-build-locks. Default verdict major-fail. Validated: the source at
3ddc6eab (which includes the one line pair this validation found, F-IV1).

Read in full for this: src/rig/store/locks.clj, grammar.clj, gate.clj,
module.clj, envelope.clj, client.clj as they stand; PLAN-locks-and-forgetting.md
in full with [V-F1] to [V-F4]; SPEC.md phase 2 and "What Rama showed" 1 to 5.
Runtime evidence cited below is from scratch runs, named where they decide a
check: a launch on the in-process cluster (4 tasks: seed, a sealed act, its
resend, a reuse, a value forget, both person forgets, face refusals), a
create-test-pstate probe of the exact `$$layers` schema (bytes in the nested
row, `subselect ALL` on the row vector, per-index writes, reads past the end,
the lease deletes, the by-stamp ranges), and a pure script over the
primitives. The suite is phase 7's.

Line numbers are locks.clj unless another file is named. -->

## Findings, first

| # | Found by | What | Fixed in |
|---|---|---|---|
| F-SV1 | phase 3 self-check | `decision-reads>` read a forget target's rows and indexed them by the offer's index before `decide` checks control values: an index past int range makes `nth` throw inside the topology (a worker kill), and a non-keyword person id reached `$$persons`' key | cc506303: `row-at` (523), `forget-target` and `person-target` return only well-formed ids (543-557), `readable-fid?` (884) guards `open-value>`'s point read |
| F-SV2 | phase 3 self-check | `open-row>` compared `*stamp` with `*T` in dataflow (`<=` throws on a non-number); `read-as-of>` ranged on a `*T` that may not be a stamp | cc506303: `opens-at?` (944, pure and total), `read-as-of>` guards `(int? *T)` |
| F-IV1 | this validation, `:allow-yield?` | `read-as-of>` and `purge-dying>` read an act's rows (`subselect ALL`) without yielding, and an act can hold hundreds of rows (stage 1 admits a 600-fact act); both ops already yield elsewhere, so yielding there costs no correctness | 3ddc6eab: `{:allow-yield? true}` on both reads (1256, 1366) |

Each was a localized line fix in the existing module; none changed a design
decision. With them in, no check below fails.

## Redundant conditionals

Check: an `<<if`/`<<cond`/`<<switch` whose every branch does the same
operation with only a variable differing.

- `deliver-lock>` (1026-1047): the outer `<<if` reads a lease row or emits
  nil; the inner one takes the `:under` entry from `*persons` or reads it
  from `$$persons` first. Different operations.
- `consume-locks>` (1050-1061), `write-decision>` (1197-1242): one-branch
  `<<if`s guarding writes, no else.
- `decision-reads>` (1083-1108): `<<if (some? *target)` reads the target act
  or binds nil; `<<if (some? *tlid)` reads the ledger or binds nil. The nil
  branches exist so the attach point unifies (dataflow.md; F7).
- `row-records>` (1110-1132): three different continuations (skip, the
  row's own `:lock`, the lock row).
- `open-row>` (1291-1318): a ledger-closed row (no more reads), a record
  lock (persons read), a row lock (lock row and persons read).
- `record-path>` (1147-1180): two independent reads-or-nil `<<if`s.
- `read-as-of>`, `lease-locks>`: guards that read or bind an empty value.
- module.clj 128-185: the face refusal or the decision.

PASS.

## Consecutive keypath

Check: `(keypath *a) (keypath *b)` in one path.

Every stage 2 path is one multi-arity `keypath`: `(keypath *layer :leases
*session *lock-id)` in `deliver-lock>`, `(keypath *layer :leases *session
*id)` (1060), `(keypath *layer :locks *lid)` (1212, 1221), `(keypath *layer
:leases *session *lid)` (1215), `(keypath *layer :log *rname *ridx)` (1224),
`(keypath *layer :erased *lid)` (1227), `(keypath *layer :by-stamp *bstamp)`
(1240), `(keypath *layer :log *name *idx)` in `open-value>`. A search for
`(keypath [^)]*) (keypath` over the stage 2 files finds nothing. PASS.

## Select-compute-transform

Check: a `local-select>`, a computation, then a `local-transform>` with
`termval`, where `+compound` with an aggregator would do.

- The excision (`forget-writes` 755-769 computes, `write-decision>` 1224
  writes): the target act's rows were read in `decision-reads>` for the
  decision itself (the target must exist, `:no-such-value`; the rows sharing
  the lock are the purge list); the write is a `termval` of the row already
  held, with `:lock nil`. That is the skill's own rule ("if you already HAVE
  the value ... use (termval *new-val) — this does NO read"); it is not an
  accumulation, so no aggregator applies, and `(keypath ... :lock) (termval
  nil)` would read the row again.
- The person fan-out (`fan-out>` 1270-1289): reads the home's entry and
  `termval`s it on every task: a carried value (L9), not an accumulation.
- Nothing else reads then writes. PASS.

## Unnecessary nil->val

No `nil->val` in any stage 2 path. PASS.

## :allow-yield?

Check: a `local-select>` iterating a subindexed structure on a non-mirror
PState that can exceed about 100 entries has `{:allow-yield? true}`; reads
bounded to small counts do not.

- With it: `purge-dying>` over the task's layers (1253), each layer's
  `:answers` (1254), each act's rows (1256, F-IV1); `read-as-of>` the
  by-stamp range (1357), each act's rows (1366, F-IV1), the ledger (1375);
  `lease-locks>` the session's lease rows (1388).
- Without it, by design, inside the gate's decision event: `decision-reads>`
  (the forget target's act rows), `record-path>` (the act's rows, the
  forget target's rows), `row-records>` (a loop over an act's rows). Traced:
  the gate reads the record, settings, persons, leases, permission rows and
  heads, then decides from what it read, then writes. A yield between those
  reads would let a later offer on the same layer be decided in between and
  change a head, a lease row or a permission the decision already read
  (dataflow.md, "Yielding": reads across a yield are not atomic; "do NOT
  yield on a path where correctness depends on same-key events processing
  in order"). These are `deframafn`s besides, where a suspend on the path
  to the emit is not allowed. The reads are bounded by the act being decided
  (its own rows), as stage 1's single write of the act's whole row vector
  is; the plan: "No loop over PState contents in the gate".
- `open-row>`, `open-value>`: point reads.

PASS with F-IV1.

## Non-subindexed collections without size limits

Check: every write to a non-subindexed inner collection has an enforced
maximum size, else it is subindexed.

- Lock record `:any-blobs` (a map), `:required` and `:any-of` (vectors):
  bounded by the value's own subjects, a subset of the act's union, which
  `subjects-refusal` (401) caps at 256 (`:too-many-subjects`, recorded,
  L13); a yes cannot carry more. The same record in a lock row and in a
  row's `:lock`.
- Answer record `:subjects` (gate.clj 282-302): carried (the parser's cap,
  256, F3), the owner, and the union only when within the cap (301).
- Row `:mark`: the two marks at most (envelope.clj `marks`).
- Lease row, ledger entry, person entry, `:scheme`: fixed fields.
- Every unbounded collection is subindexed with size tracking off:
  `:locks`, `:leases` (both levels), `:erased`, `:by-stamp` (971-988), as
  the plan's schema has them.

PASS.

## Stream topology idempotency

Check: under any retry, every write is idempotent; no ids generated in the
topology; internal depot appends guarded.

Every stage 2 write, traced for a replay after its event committed and one
after it did not (SPEC "What Rama showed" 1, 2):

- Consumption, a `NONE>` on `[layer :leases session id]` (1060), on the
  fresh path and the record path: deleting twice is the same; a replay that
  finds the rows gone deletes nothing.
- Lock rows `termval` (1212), lease rows `termval` (1215), the ledger
  `termval` (1227), by-stamp `termval` of the name (1240), the home's person
  entry `termval` (1235), excised rows `termval` (1224), lock-row deletes and
  the session close `NONE>` (1218, 1221): each a set or a delete keyed by
  lock id, session, stamp, fact id or person.
- A replay before the commit: the event's writes are discarded together
  (the fan-out's `(|all)` is the only partitioner and comes after every home
  write), so the replay decides again on the same rows (the consumption was
  discarded with the rest) and writes other fresh bytes (lease locks, a
  person lock, wrap nonces) that nobody saw: one encoding ever commits
  (I-L8).
- A replay after the commit: the record is found first (module.clj
  114-123); the record path writes only the idempotent consumption; a
  lease's replay answers `(lease-ids name n)`, the same ids, and mints
  nothing (`record-answer` 859-882).
- Ids: none generated in the topology. Lock ids are `[lease-name i]`
  (`lease-ids` 193-199), a function of a name the offerer made; the random
  bytes are locks and nonces, not ids.
- `depot-partition-append!`: none.

Observed in the scratch launch: a resend of an admitted act returned the
recorded answer and stamp. PASS.

## Partial failure in stream topologies

Check: an event writing across partitions, failing after some writes
committed, leaves nothing permanently unexecuted.

The one cross-partition write is the person fan-out (`fan-out>`
1270-1289). The home's writes commit at `(|all)`; a child that fails
replays the whole record (`:all-after`); the replay finds the record on the
home and takes the record path, where `record-answer` sets `:fan-out` again
for a yes person act, so `fan-out>` re-reads the home's entry as it stands
and `termval`s it on every task, children that had written included (L9).
The enumeration and the purge seam in each child run again; the purge is by
value id, and the seam is a no-op tonight. Nothing is left unexecuted. PASS.

## Single depot append per client operation

Check: each client write operation calls `foreign-append!` once.

`offer!` (client.clj) appends once per call. An act with value facts is two
operations by design (plan, "The door"; CONCLUSION R1): a lease act, then
the value act, each an ordinary offer answered by name. A client crash
between them leaves lease rows unconsumed and no value: every invariant
holds (the lease is decided; the value never was), and the rows are a
named, bounded retention (plan, "Lease rows at session close": until the
session closes or its writer is forgotten), which a restarted door takes
again by `lease-locks`. A session close is its own act. PASS.

## Application-state caches survive restart

Check: every TaskGlobal or in-process cache of application state has a
durable source and a rebuild path.

None in the module: no TaskGlobal (the holder road is described, not
built), person locks read from `$$persons` at every use. The door's memory
(client.clj `fresh-door`) is the offerer's, not the module's; its durable
source is the lease rows and its rebuild `lease-locks` (`refresh!`), which a
fresh `connect` performs on its first seal. PASS.

## No reimplementation of built-in operations

`ops/explode` where a write per element is wanted (1060, 1210-1227). The
loops (`read-persons>`, `deliver-all>`, `row-records>`, `own-locks>`,
`read-as-of>`) accumulate a map or vector across reads, which `explode`
does not; `indexed` (941) feeds such a loop. No sum, range, explode or
task id is rebuilt. PASS.

## Plan conformance

Each place the build differs from the plan's text, with its reason. None
changes a decision the plan made.

1. **The decision's lock part lives in `rig.store.locks`** (`lock-refusal`
   696-711, `lock-effects` 784-816), called from `gate/decide*` (gate.clj
   312-353: one reason clause, one effects call, `:locks` in the decision,
   the union in the answer record). The plan lists these under gate.clj
   ("`decide` extended with the lock effects ...") and puts the stage's
   bodies in `rig.store.locks` so module.clj carries calls. Behaviour as
   planned, pure and total. Located so for builder A's instruction to keep
   shared files as small as the plan allows and for phase 3's need of the
   value checks apart from `decide` (PV-F13). Conforms.
2. **`read-as-of`'s fact entries also carry `:e :k :replaces :mark`**
   (`fact-entry` 925-931), a superset of the plan's `:id :stamp` with
   `:value` or `:erased-at`. The plan requires stage 1's suite to run
   through the door, and the door receives plaintext only from
   `lease-locks` and `read-as-of` (plan, O2 point 6), so stage 1's `facts`
   (an act's rows with values) can be served only by a read that says which
   e and k each value is. Justified: without it the plan's own requirement
   fails.
3. **`read-as-of>` emits once** (a `deframaop`, 1348-1376): per act one
   `subselect` of its rows, per row `open-row>`, accumulated in `loop<-`,
   instead of an emit per row and `aggs/+vec-agg` after `|origin`. The same
   reads (the by-stamp range, one seek per act, per row the ledger, the lock
   row and the persons) and the same output, bound when empty. A single emit
   keeps the query's pre-agg one branch with no joins. The plan's pre-agg and
   agg lines are its sketch of the same reads.
4. **A value forget reads the target act's rows** (one seek plus f
   iterations) where the plan reads the target row, and the act's rows only
   for a per-act excision. Under per-act grain a row lock's deletion erases
   every value of the act, and builder A's purge seam needs every erased
   value's id and row; the rows sharing the lock are found only by reading
   the act. A forget is rare (0.015 of offers). Required by the seam.
5. **`record-path>` gives a recorded forget's `:how`** from the ledger
   (859-882, 1166-1177); the plan gives it on the fresh path and is silent on
   the record path. SPEC "What Rama showed" 2: a replayed offer must get the
   same answer. Two seeks on a rare path.
6. **Builder A's additions, not in the plan:** the purge seam
   `purge-read-indexes>` (1182-1195), called by a value forget after the
   ledger write (1229-1231) and, per task, by a person forget's fan-out
   child through `purge-dying>` (1243-1268), which enumerates the values
   dying on that task; its cost, O(acts on the task) iterations plus a few
   seeks per value that dies, is over any per-event budget at scale (named
   in BUILD_NOTES and the receipt). The lock record's `:scheme :aes-gcm-1`
   (281-288, first-record placeholder), checked by `unwrap`.
7. **Rig choices where the plan is silent** (BUILD_NOTES, "Rig choices"):
   an act carrying a lock control fact carries it alone (gate.clj 199-200,
   `:malformed-control`); person facts only in a `:store` layer (gate.clj
   102-119, `:control-not-allowed`); a making act whose owner's lock is
   destroyed is `:person-forgotten` (the plan names `:no-such-person` only);
   an opened text must be its value's canonical text (`decode-plain`,
   `:malformed-value`); the door's default session `:door/<who>`, its lease
   size 64, and its reply to a refused lease (it cites the refused lease's
   ids, so the gate answers `:no-such-lock`). Each changes without touching
   a record.
8. **Interfaces.** `open-row>` takes the read exit's order, `[*layer *fid
   *row *stamp *T]` (the plan does not fix one); `open-value>` is `[*layer
   *fid *T]` with the plan's three return shapes. The install functions add
   `(row-fields)` beside `(layer-fields)`: the plan adds four row fields, and
   the row's schema sits inside stage 1's `:log` field, so "one merge
   argument per stage" needs a row merge argument too. The door's
   `depot-records` reads every partition where the plan's `depot-record`
   takes an offset a test does not know; `lock-rows` returns the rows and
   leaves sizing to phase 7.

Everything else traced as the plan has it: the refusal order (stage 1's
list, then `:does-not-open` to `:no-such-value`, 696-711); the face
`:no-such-lock` after the record, the settings and the persons and before
every recorded reason (module.clj 125-129); consumption yes or no (784-788)
and on the record path (859-882, module.clj 121), never on a face refusal;
the re-wrap table (`wrap-of` 253-268) and placement (`row-lock?` 366-373,
`lock-plan` 608-633); per-act grain (L6) and `:grain-mismatch` (406-416);
the parts digest (envelope.clj `digest`); the value digest keyed by the lock
and written for a yes only (720-741); the record path's check with its skip
([V-F1], 464-487, 844-857); lease rows under the lease's writer or bare for
the operator ([V-F2], 211-221, 743-753); the base with no person owner
(`person-owner` 247-251); `lease-locks` unleasing by each row's `:under`
(893-901, 1378-1392); `deliver-lock>` read-only with `consume-locks>` apart
([V-F3]); the session close (L28); the person acts in `:people` and their
fan-out (L7, L9); `$$persons` on every task (L1); `:by-stamp` for yes acts;
three install lines and the call sites in module.clj. PASS, with the
justified items above.

## Spec coverage (SPEC.md phase 2, and "What Rama showed")

- "A small symmetric lock per value at write": `lease-writes` mints 32-byte
  AES locks (743-753); the door seals each value under one (client.clj
  `seal-with`), one per act under per-act grain. PASS.
- "Wrapped under person locks as ruled, owner required in one-owner layers,
  7b among the other subjects, marks honoured": `wrap-of` (253-268): the
  person owner required, any-of in a layer with no person owner,
  `:die-with-any` making every subject required, `:own-row` a row; the
  ciphertext enforces it (`wrap` 290-316, `unwrap` 318-333). Scratch launch:
  Bob's forget left the mention of Bob in `:alice` open; Alice's closed it.
  PASS.
- "Lock rows in a lock store for personal and hand layers, on the same task
  as their values; in the record for agent layers": `row-lock?` (366-373);
  the rows in the layer's `:locks` on its home (971-988). PASS.
- "Value forget deletes the row; person forget destroys the person lock;
  every forget is a fact": `forget-writes` (755-769) deletes the row or
  excises the record lock and dates the ledger; `person-writes` (771-782)
  and the fan-out destroy the lock on every task; both are acts. Scratch
  launch: `:how :row-deleted`, the ledger dated by the forget's stamp. PASS.
- "Time travel shows erased on this date and nothing else from after its
  moment": `read-as-of>` ranges by stamp at or before T and shows each
  fact's value or `:erased-at` (1348-1376, `open-with` 491-518). PASS.
- "What Rama showed" 3, a gate never throws: every function the event calls
  is total (the primitives catch `Throwable`; `read-values`, `check-resend`,
  `fresh-for`, `open-with` catch; `decide` keeps stage 1's guard); every key
  and index read before the decision is validated (F-SV1, F-SV2); an
  impossible state inside the decision throws to `decide`'s guard, the face
  `:gate-error`, never a half-locked admission (`fail!` 713-718). PASS with
  F-SV1 and F-SV2.

## Verdict

**minor-fail**: three localized failures (F-SV1 and F-SV2 from the phase 3
self-check, F-IV1 from this validation), each fixed by editing specific
lines of the existing module, with no restructuring and no design change.
With the fixes in, every check above passes.

PHASE_VALIDATION:minor-fail
