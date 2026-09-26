# Implementation Validation — stage 3, "micro-store"

<!-- Phase 4 of the rama skill (phase-4-impl-validate.md, template
artifact-impl-validation.md), run inside the build session on 26 September
2026 from 03:35 IST (Claude Opus 5.5, effort max), adversarial, default
major-fail. Module source: src/rig/store/micro.clj (the topology, the fold,
the queries), src/rig/store/micro_client.clj (the door), src/rig/store/permit.clj,
and the shared-file changes to gate.clj, envelope.clj and module.clj, at
commit 550da86d on rig-build-micro. Plan: PLAN-micro-store.md (revision of
26 September, §A to §J, [PV-F1] to [PV-F13]) with builder A's R19 (the walk)
and the root actor :operator. Spec: SPEC.md phase 3 and "What Rama showed".
Line numbers are micro.clj's unless another file is named. -->

The checks below were traced in the source. Where a trace found a fault, the
fault was fixed in this session before the verdict and the fix is named with
its commit; the verdict is on the code as it now stands.

## Redundant conditionals

Check: "if every branch of an <<if, <<cond, or <<switch does the same
operation with only a variable differing, replace with a single operation."

- Block 2a's `<<cond` (1270-1301): the `:entity` and `:name` branches both
  begin `(|hash *route2)` but write different PStates with different paths
  (`$$micro` by `(keypath route field k)`, `$$micro-names` by field, with
  the keep-first `term` for faces); `:task` routes `|direct` to a task id;
  `:mint` reads `$$persons` and explodes rows. Not the same operation.
- Block 2c's `<<if` (1365-1368): a no-read delete of one row against a
  delete of the whole `:leases` map. Different paths.
- `micro-lookup`'s two `<<if`s (1378-1385): the first reads a face only when
  a fingerprint is given (a nil key is not read); the second skips the
  record read when a face answers. Different operations, each saving a seek.
- Block 1's many `(<<if (some? x) (local-select> ...) (else>) (identity nil
  ...))` bind a var on both branches so the attach point unifies; the else
  branch does no read. Not redundant.
- gate.clj `refusal`: the permission clause was first written as
  `(and (not exempt?) (permit/refusal offer rows)) (permit/refusal offer
  rows)`, computing the walk twice; fixed to one `perm` binding (550da86d).

PASS.

## Consecutive keypath

Check: "(keypath *a) (keypath *b) → (keypath *a *b)". Every path in the
topology and the queries uses one multi-arity `keypath`: 1126
`(keypath *lname :leases *l1-i)`, 1167, 1179, 1181, 1194, 1196, 1217,
1231, 1234, 1273 `(keypath *route2 *field2 *k2)`, 1281, 1283, 1298, 1316,
1348, 1351, 1354, 1366, 1368, 1379, 1385, 1393, 1395, 1405, 1421. The
client's reads in micro_client.clj likewise (for example `(keypath L
:settings)` then `sorted-map-range-to-end`, a range navigator, not a
keypath). PASS.

## Select-compute-transform

Check: "local-select> followed by computation followed by local-transform>
with termval — replace with +compound and an aggregator when possible."

The fold decides on task 0 and every write in 2a is a `termval` of a value
the fold computed from the gathered rows (1273, 1283, 1287, 1298); none
reads the location it writes, and none is an accumulation an aggregator
could express: a replaced head is written whole with its fields known from
the gather (the plan's "no read"), a settings version is a new key, a
permission row is written whole from the merged read. The one `term` (1281)
is the keep-first face write, a read by design ([PV-F10]). Block 2b's row
writes (1351, 1354) write values computed on the arrival task. PASS.

## Unnecessary nil->val

No `nil->val` anywhere in micro.clj or micro_client.clj. Nil reads are
coalesced in pure functions where a number is needed (`(or nclock 0)` in
`name-step`, `(or clock 0)` in `entity-rows`, `frontier-of`'s -1). PASS.

## :allow-yield?

Check: local reads iterating a subindexed structure beyond ~100 entries
take `{:allow-yield? true}`; bounded reads do not.

- 1149 `[close :answers] (subselect MAP-KEYS)`: a session's acts on its
  own entity, unbounded in principle: yields.
- 1395 `micro-act`'s `[e :log name] (subselect ALL)`: an act's rows on one
  entity, unbounded (acts of any size). It lacked the option at first;
  added in 550da86d. Yields.
- 1405 `micro-lease`'s `[lease-name :leases] (subselect ALL)`: at most 256
  (M24) but it yields as the plan wrote it; harmless at that bound.
- 1421 `micro-leases-of`'s `[session :answers] (subselect ALL)`: yields.
- 1167 `[L :settings] (sorted-map-range-to-end 1)`: one entry, no option,
  correctly.
- Every other read is a point read (`keypath` to one value).

PASS.

## Non-subindexed collections without size limits

Every child collection that can grow is subindexed with size tracking off
(`sub`, 105): `$$micro`'s `:log` at both levels, `:heads`, `:answers`,
`:locks`, `:erased`, `:settings`, `:members`, `:permissions`, `:replaced`;
`$$micro-names`' `:faces`, `:stood-on`, `:leases`. The non-subindexed
inner collections and their enforced bounds:

- a record's `:subjects` set: the carried subjects are capped at 256 by the
  parser (F3); the union is capped by the recorded `:too-many-subjects`
  (L27), and for that refusal `record-subjects` (711) stores only the
  carried subjects, so no stored set exceeds 256 plus the owner.
- a row's `:mark` set: at most `#{:die-with-any :own-row}` (the parser's
  `marks`).
- a lock record's `:required`/`:any-of` vectors and `:any-blobs` map: at
  most the value's subjects, within the same 256 cap.

PASS.

## Stream topology idempotency

The micro store has one stream topology, the `$$persons` placeholder
(1069-1077): `(|all)` then a `termval` of the carried entry at the person's
key on every task. A replay writes the same entry again. No generated ids,
no internal depot appends. (The micro topology is a microbatch: its writes
are exactly-once by Rama across a retried attempt, microbatch.md
"Guarantees"; every write is a `termval` or a no-read `NONE>` of values
computed from the batch and pre-batch state, except the random lease locks
and wrap nonces, which differ between attempts and of which only the
committed attempt's exist, as the plan states.) PASS.

## Partial failure in stream topologies

The placeholder's one event writes the same key on every task after one
`|all`; a failure part way replays the whole event (What Rama showed 2), and
each child's `termval` is idempotent, so no write is left unexecuted. The
microbatch's cross-task atomicity is Rama's (the attempt is discarded
whole). PASS.

## Single depot append per client operation

Each client operation appends once: `send!` (micro_client.clj) is one
`foreign-append!` of one envelope; a resend is the same map (P6's road).
The door's `write!` is two operations by design, as phase 2's lease road
has them: a lease act (answered by name) and then the sealed act citing
its locks. A crash between them leaves lease rows sealed under the lease
act's person, openable by nothing in the store, until a decision citing
them or the session's close (§A, [PV-F4]); that bounded retention is the
plan's, not a partial write. PASS.

## Application-state caches survive restart

No TaskGlobal, no in-process cache. The fold's working state W lives for
one call of `prepare` (847) on task 0 and is rebuilt from the gathered rows
every batch. The door holds leased locks in memory until it uses them; a
restarted door takes them again by `micro-leases-of` and `micro-lease`
(1401-1423), whose durable source is the lease rows in `$$micro-names`.
PASS.

## No reimplementation of built-in operations

Checked for hand-rolled versions of `com.rpl.rama.ops` and path navigators:
explode (`ops/explode`, `ops/explode-indexed`, `ops/explode-map` used),
task and batch ids (`ops/current-task-id`, `ops/current-microbatch-id`),
aggregation (`aggs/+map-agg`), ranges (`sorted-map-range-to-end`,
`subselect`). Hex is `java.util.HexFormat`. The cryptography is phase 2's
`rig.store.locks` (merged, not rewritten). PASS.

## Plan conformance

Every divergence found, with the reason it stands or the fix:

1. **The offerer's side is `rig.store.micro-client`, not `rig.store.micro`
   (M13).** `module.clj` requires `rig.store.micro` for `declare!`; the door
   needs the module's name and the stream client (`client.clj` requires
   `module.clj`), so a door inside `rig.store.micro` is a require cycle and
   does not compile. Correctness. PASS.
2. **`client.clj` is not changed; M13's dispatch (`gate-for`, `send-any!`)
   and M25's forget routing are in `rig.store.micro-client`.** Follows from
   1 (client.clj cannot require the door). Smaller shared-file change.
   PASS.
3. **`$$persons` is declared on a placeholder stream topology
   (`persons-placeholder?`, 1059-1077).** The merged phase 2 commit holds
   the pure lock namespace only; its gate, which declares and writes
   `$$persons`, is not in this branch, and the micro gate must read person
   locks. Same schema as phase 2's; one flag to turn off at the merge.
   Correctness (the module would not compile). PASS.
4. **Lease rows carry the layer's `:kind` and person `:owner`.** The plan's
   block 1 computes a value's own subjects on the arrival task, "no owner
   in a shared layer", and block 2b wraps there; for a re-classed one-owner
   layer the owner is required in every wrap and personal and hand kinds
   keep row locks, and neither is readable on the arrival task (the
   settings are on hash(L)). The plan is silent on how the arrival task
   learns them; the lease, minted by the fold that holds the settings,
   records them. Correctness (a re-classed layer's value would otherwise be
   wrapped without its owner). PASS.
5. **Leases are minted in block 2a from the fold's output (1289-1298), not
   in 2b from the offers.** With 4, the rows need the settings; the fold has
   them, and 2a reaches the lease name's task directly. The plan's 2b road
   would need a hop to the layer's task per lease act for the same fields.
   Performance (a hop) and the same rows in the same batch. PASS.
6. **The person checks are computed on the arrival task and carried in the
   skeleton (`person-reason`, 450; `persons-to-check`, 439), not emitted as
   `[[:person p] entry]` rows.** `$$persons` is another topology's PState,
   read at a moment on each task; during a person forget's `|all` fan-out
   two tasks can hold different entries, and `+map-agg` would keep one of
   them and decide an offer on another offer's read. Correctness. PASS.
7. **The record path's check rows are one per value, `[[:resend name fp
   i] check]` (1247-1249), not one per envelope.** `+map-agg`'s combine is
   `merge`, which keeps one value per key; the plan's one row per envelope
   "merged by the combiner" could drop a `:name-taken`. Correctness. PASS.
8. **`$$micro [L :members]` is a map person → batch, not a set (the plan's
   schema).** The plan's own writes address `$$micro [L :members person]`
   and M7 puts a batch on every row; a set can be addressed by neither.
   The plan was inconsistent; this follows its writes and M7. PASS.
9. **A setting fact for a layer whose settings the stream gate keeps is a
   face `:wrong-gate` at this gate (`layer-rows`, 517).** P16 and M25 keep
   such settings with the stream gate; without this a misrouted grain
   switch would write a micro settings version that shadows `$$layers` for
   every later offer (settings are read micro first). Correctness. PASS.
10. **The fold coalesces its writes by location (`put`, 663; `prepare`,
    847).** The plan's writes are "exactly once per location per attempt"
    by the fold's reasoning, but two acts of one batch can touch one
    location (a head made then replaced, a permission granted then
    revoked, two settings versions of one layer), which would be two
    termvals of different values in one batch, applied in no fixed order.
    The last write in the fold's order wins. Correctness. PASS.
11. **No bare lock crosses a task, the record path included (`carry-rows`,
    579).** [PV-F2] allowed an operator's bare lease row to travel on the
    record path. Rama ships every variable live after a partitioner
    whatever branch is then taken, so honouring that exception would ship
    every sealed offer's bare rows to its name task, on the fresh path
    too, which §A forbids. A resend under a fresh operator lease therefore
    skips its value check (phase 2's "a missing own lock skips rather than
    refuses"). Found and fixed in 9664de0a. Correctness. PASS.
12. **`:landing` is taken as data at this gate** (envelope.clj, one line):
    conformance with the plan's shapes and M4; found missing and added in
    550da86d. PASS.
13. **A forget of a stream-era target is refused `:no-such-value` here
    (`micro-extras`, 676).** M25 routes it to the stream gate; a misrouted
    one would otherwise be admitted with no effect. Correctness. PASS.
14. **The effect of a value forget in a shared layer (the record's lock
    excised, the ledger written) is not built.** The plan leaves it to
    stage 2 "through the row seam" (Writes, "forget in a shared layer";
    "What later stages consume"), and the forget fact itself is admitted
    and its row written, as the plan says for this stage. Not a divergence
    from this stage's plan; a seam left open, named in the receipt. PASS
    for this stage.
15. **R19**: the permission walk (`permit/chain`, `permit/refusal`; the
    stream gate's three places), not PV-F8's cascade, as builder A ruled.
    PASS.

16. **Added in phase 7, after the first cluster run: every envelope the
    record path answers leaves a keep-first `:recorded` entry under its
    fingerprint in `:faces`, and `micro-lookup` returns a found record with
    the fingerprint it was decided for (`[name :fp]`, one more point read,
    only when a record is found and no face is; the plan's "three reads at
    most" becomes four on that path).** The run showed the plan's lookup
    answering a resend by the record's parts digest before the gate had
    checked the resend: with values sealed, the parts digest holds no
    value, so other plaintext under a decided name was told "yes" and its
    `:name-taken` face came a batch later (E1 N2 × other content, from the
    offerer's side). With the trace and the decided fingerprint, an
    offerer can tell its own envelope's outcome from another's. The door's
    rule is `micro-client/own-answer?`. Correctness. PASS.

17. **The fold decides by `gate/refusal` and `gate/stamp-for`, not
    `gate/decide` (`micro-decision`, `micro-record`).** The plan reuses
    `gate/decide`. Phase 2's `decide` (rig-build-locks 33b357a4, read, not
    merged) now opens an act's values itself, and its 7-arity decides with
    an empty lock context in which "a sealed value then does not open", so
    after the merge the leader's call would refuse every sealed act. The
    plan named this adapter ([PV-F13]: "phase 2's decide on the stream gate
    opens values itself, which the leader must never do"). The reason
    order is still one function (`gate/refusal`, then `reason-order` for
    this gate's inputs) and the stamp one function (`gate/stamp-for`, over
    `rig.store.clock`); only the projections a yes writes (the settings
    merge, the first grant per pid, the revoke rows) are built here, as
    `gate/decide*` builds them. Correctness at the merge. PASS.

Everything else matches: the depot and its route key (M4 revised, 287);
the tick (1087-1089, behind the replaceable flag); the four PStates'
shapes (107-183); blocks 0 to 2c in the plan's order with the plan's reads
on the plan's tasks; the fold's four rules and M2's order with [PV-F3]'s
tiebreak (598, 814); `gate/refusal` and `gate/stamp-for` reused (item 17), with this gate's
reasons placed by one order (`reason-order`, 58; [PV-F13]); the four queries with explicit
F (1374-1423); the frontier as the previous batch's id (1105); keep-first
faces (1281); consumption in 2c after 2b (1357-1368); the session close
([PV-F4]); the base on the stream gate and its re-class before the first
group standing on it (micro_client.clj `make-base!`, `make-group!`).

## Spec checks (SPEC.md phase 3 and "What Rama showed")

- "A microbatch gate for group and base layers placed by entity, atomic
  across tasks": one microbatch topology; the act's verdict is the fold's,
  its writes span tasks in one attempt. Base per default R8 (§C).
- "with a name row written in the same batch so an answer is found by name
  plus layer": `[name :answer]` in 2a of the deciding batch; `micro-lookup`
  routes by the name, which carries its layer (P4).
- "Wrapped locks in the record": a shared layer's value lock, re-wrapped
  under its subjects in 2b, is the row's `:lock` (`fact-rows`, 950).
- "Permissions as facts in the layer they govern, checked only by the gate
  that orders that layer; a revoke; an offer citing a permission from
  another layer refused": grants and revokes are facts of the layer,
  projected into `$$micro [L :permissions]`; `permit/refusal` refuses a
  chain with any element kept elsewhere.
- "Re-class of a hot layer moves it here": the stream gate's re-class; this
  gate reads `$$layers` settings, permissions and heads as settled history
  and keeps the delta (M5).
- What Rama showed 3 (a gate never throws): every function topology code
  calls is total (`intake`, `parse-micro`, `route-key`, `arrival-open` via
  phase 2's total primitives, `resend-check`, `prepare` with a guard per
  envelope and a guard per row in `init-w`, `mint-rows`, `row-wraps`,
  `wrap-persons-of`, `nonces-needed`). `fact-rows`, the one function that
  computed block 2b's rows without a guard, was found by this check and
  given one per value fact: a value whose lock work fails is written
  admitted and closed (`:lock` and `:digest` nil, PV-F6's rule), never a
  throw (fixed before this verdict). The remaining helpers the topology
  calls (`lease-keys`, `unders`, `persons-to-check`, `person-reason`,
  `skeleton`, `carry-rows`, `name-step`, `layer-rows`, `micro-head-keys`,
  `entity-rows`, `resend-persons`, `rows-written?` and the query results)
  take only what `intake` validated and the PStates' schema-checked rows,
  and do nothing that can throw on them (map and vector access, `sort` of
  readable keywords).
- What Rama showed 5 (nothing may rely on seeing a microbatch on every task
  at once): every reader-facing read goes through one F (§D).

## Self-consistency

Items 11, 12, the `:allow-yield?` line and `fact-rows`' guard are faults
this validation found; each was fixed before this verdict. Nothing above is
passed while called a gap: item 14 is outside this stage's plan and is
carried to the receipt as an open seam, not certified as built.

## Verdict

**minor-fail**, now fixed: the faults found (a bare lock shipped on the
fresh path, the missing `:landing` line, a missing yield, a doubled walk,
an unguarded row computation) were each a localised edit to named lines,
made in this session; no restructuring was needed.

PHASE_VALIDATION:minor-fail
