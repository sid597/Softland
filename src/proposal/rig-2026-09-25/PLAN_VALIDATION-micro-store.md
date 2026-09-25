# Plan Validation — stage 3, "micro-store", the 26 September revision

<!-- The rama skill's phase 2 (phase-2-plan-validate.md, template
artifact-plan-validation.md), run on 26 September 2026 from 02:19 IST by a
fresh-context session (Claude Opus 5.5, effort max), adversarial, default
FAIL. Plan: PLAN-micro-store.md in this folder at commit 2b41aebc (branch
rig-plan-micro). Spec for this stage, as the step brief names it: SPEC.md
(Sid's phase 3 verbatim, and "What Rama showed", binding); the main rig
worktree's RIG.md, "Defaults taken overnight, not ruled", defaults 1, 2, 3,
5 and 6 (default 6 overrides SPEC phase 3's placement of the base);
PROGRESS.md "Now" on main (rulings 1, 2, 7, 7b, 9; the layer kinds; the two
consequences; the sharpenings); CONCLUSION.md R1, R3, R7, R8 and "Also going
into the model"; the formal model (model.clj, scenarios.clj, README.md);
IMPLICIT_SPEC.md; PLAN-stream-store.md; the phase 1 code (main rig
worktree); PLAN-locks-and-forgetting.md at the locks branch's latest commits
(b11525b4 and 6320b312, after its own validation, 870b62c2); PLAN-read-exit.md
at the reads branch's 5a6bb732. Vocabulary: "key" is a fact's key, "lock" an
encryption key. -->

## Verdict

**minor-fail.** Thirteen failures, each fixable by an edit to named sections
of the plan; the architecture stands (lease rows beside the lease act's
name row, routing by lease name, one fold on task 0 over skeletons, blocks
0 to 2c, the frontier as the previous microbatch id, the four PStates).
The fixes are applied in the plan and marked `[PV-Fn]` there.

The failures, one line each (the traces below give the working):

- **F1.** Lease rows are sealed under the session owner in the plan; phase
  2's latest (V-F2) seals them under the lease act's `:who` person, the
  operator's bare (`{:under nil :sealed K}`); and §A's row shape disagrees
  with §I's schema (`:under`/`:sealed` against `:lock`/`:who`).
- **F2.** The record path is phase 2's superseded one: phase 2 (V-F1) now
  leaves lock ids out of the parts digest, opens a resend with its own lock
  and recomputes under the recorded one, and consumes the cited rows on the
  record path (V-F3). The plan's "the record path writes nothing and
  consumes nothing" and "the same lock id, since the parts digest covers
  it" no longer hold.
- **F3.** Two envelopes under one name in one batch collapse in
  `+map-agg` (the offer row is keyed `[:offer name]`), M2's order ties on
  them, and block 2b's filter by parts digest lets a second envelope with
  other plaintext write the rows the first was decided on: a reused name
  admitted without its content check, and its subjects unchecked.
- **F4.** Session close is not designed on the micro side (§J 7), so an
  unconsumed lease row, and the depot bytes it opens, are kept with no end;
  default 1 says "unconsumed lease rows go when their session closes".
- **F5.** The operator's lease ("under an empty wrap") contradicts the
  plan's own lease gather, which would refuse it `:no-such-person`; phase
  2 names it `{:under nil :sealed K}`, a bare lock at rest, whose retention
  the plan does not name.
- **F6.** The revision says the block 1 `$$persons` subject read "is
  replaced"; `gate/decide` still needs each subject's entry for
  `:no-such-person` and `:person-forgotten`, and 2b's re-read of
  `$$persons` can differ from block 1's (a read at a moment of another
  topology's PState) without the plan saying what is then written.
- **F7.** The partitioning table counts block 1 only; the template wants
  the total per operation across all tasks (block 2b's lease re-read and
  name read, every `$$persons` read, the record path's reads).
- **F8.** The permission walk is kept although the plan's own arithmetic
  shows the cascade doing less I/O per offer (about 0.1 against 1.5 seeks);
  the template's throughput rule and the skill ("Never trade I/O efficiency
  for code simplicity") fail it. Fixed by adopting the cascade.
- **F9.** The "Query Topologies" body carries no revision pointer and still
  describes `micro-lookup [*name *digest]` reading faces only when the row
  is absent, which §D reverses; the read exit's slot moved to line 128.
- **F10.** A faces entry written again for the same `[name fp]` in a later
  batch moves its `:batch` above a reader's F (M7's "never overwritten").
- **F11.** §B says the check is "exactly P8's four checks" for a triple;
  M21 adds coverage by `:session`, which the model does not have (model.clj
  `refusal`: holder must be `:who`), and the model has no chain at all.
- **F12.** The base's owner and root holder is `:root` here and `:operator`
  in phase 2's latest (V-F2, "the rig uses `:operator`").
- **F13.** The value-level refusals differ from phase 2's L27 order: the
  plan makes a value that does not open a `:malformed` face, a lock cited
  twice in one act `:no-such-lock`, and the subject cap a face; phase 2
  records `:does-not-open`, `:grain-mismatch` and `:too-many-subjects`
  after the delivery, consuming the leases, and refuses a plaintext value
  `:not-sealed` on its face, which the plan does not have. "The rig's gates
  must decide the same way" (M21) fails, and a refused value's leases stay
  openable.

## The traces the step names

Concrete world for every trace: N = 4 tasks; group `G` (shared,
`:by-entity`, per-value grain) with root `[G G G]`, Alice's permission
`pA = [alice G G [G G G]]`, her session `S`'s permission
`pS = [S G G pA]`; hash(S) = 1, hash(G) = 3; Bob a person. Task numbers are
illustrative; only "same task or not" matters.

### T1. §A: a sealed act routed by its lock's lease name

History. Batch b1: lease act `LA`, name `NL = [G :by-entity :offer u1]`,
`:who alice`, `:session S`, `:permission pS`, one fact `{:e S :k :lease :v
{:count 2}}`; routed by its first entity to hash(S) = 1. Batch b3 (after
the door read the lease at F ≥ b1): offer `O`, name `NO = [G :by-entity
:offer u2]`, one fact `{:e thread-7 :k :note :v <V sealed under K0> :lock-id
[NL 0]}` with V about Bob; routed by `route-key` = `NL` to hash(NL) = 2
(plan lines 173-179). hash(NO) = 0, hash(thread-7) = 3.

Where each thing happens (plan lines 189-268, with F1/F2 applied):

| step | task | batch, block | what |
|---|---|---|---|
| lease rows minted | 2 = hash(NL) | b1, 2b | `fresh` K0, K1; each sealed under Alice's person lock (the lease act's `:who`, F1); rows `[NL :leases 0/1]` with `:batch b1` |
| door takes the plaintext | 2 | query, after b1 ≤ F | `micro-lease [NL F]`: F, the range, `unlease` each |
| the lock read and the value opened | 2 | b3, block 1 | range read of `[NL :leases]`, `:session S` and `:layer G` match the offer's, `unlease` K0, `open` V, subjects {bob}, `$$persons bob` entry (F6) |
| the decision | 0 (leader) | b3, block 1 post-agg | the fold over skeletons: parts digest, subject union, lock ids, lease status; `gate/decide` gives yes or a recorded no |
| the record | 0 = hash(NO), 3 = hash(thread-7) | b3, 2a | `[NO :answer]`, `[thread-7 :answers NO]` |
| the row | 3 | b3, 2b | re-read `[NL :leases 0]` on task 2, `unlease`, `open`, re-wrap under {bob} (`:required [] :any-of [bob]`), value digest; carried wrapped with the sealed bytes via task 0 (the name check) to task 3; `[thread-7 :log NO 0]` written |
| the lease row consumed | 2 | b3, 2c | `(termval NONE)` at `[NL :leases 0]` |

All of it is in batch b3. **Atomic across tasks:** a microbatch commits
all of its writes on every task or none ("What Rama showed" 5: "decided
before it is visible and leaves no partial state across tasks"), so there
is no state in which the row exists and the lease row was not consumed, or
the reverse. **Idempotent under a retried batch:** every write is a
`termval` of a value computed in the attempt (plan lines 1397-1405), a
failed attempt's writes are discarded, and only the committed attempt's
bytes exist; the re-wrap's nonces differ between attempts, which is safe
because only one attempt commits. The barrier between 2b and 2c is what
lets 2b read the row 2c deletes (plan lines 263-266): checked, correct.

**What travels between tasks in the clear.** Block 1: the parts digest, the
subject union (person ids), the cited lock ids, a lease status. Block 2b:
the sealed bytes, the value digest, and the lock record, which is wrapped
under the subjects' person locks, except when the value is about no one:
then it is an empty wrap, "K bare in `:blob`" (phase 2, "Lock record").
So the plan's "A lock never leaves the task it is read on" (line 110) is
false for a value about no one: its K crosses from task 2 to task 3 bare,
inside the cluster, to be stored bare in the record, where shared-layer
rules put it anyway (7b as written; excision is the only forget for it).
That is acceptable, since the destination holds it bare by ruling, and the
fix states it (F2). The plaintext never leaves task 2. The door's lease
plaintext leaves by the query, the path "the depot never sees" (R1).

**Verdict: PASS after F1, F2 and F6.** Before them: FAIL on the sealing
person and the record path (F1, F2).

### T2. The operator's leases: a lock at rest bare until consumed

History. The operator (no person lock, `exempt?` in the model) leases 1
lock in `G` in its session `S0`: under phase 2's V-F2 the row is `{:under
nil :sealed K}`, K bare in `$$micro-names [NL' :leases 0]`. The operator
seals V about Bob under K and offers `O'`.

- **Bob forgotten before the decision.** Bob's person forget is admitted by
  the stream gate (the `:people` layer, phase 2) between the append of `O'`
  and its batch. Block 1: `unlease` of a bare row gives K; V opens; its
  subjects are {bob}; `$$persons bob` shows the lock destroyed; the fold's
  `gate/decide` refuses `:person-forgotten`, recorded (phase 2's L27: after
  the delivery). 2c consumes K. After the batch: the depot holds V sealed
  under K; K exists nowhere. P6 widened, "nothing retained opens or
  confirms a forgotten value": holds.
- **The same, but `O'` is refused on its face** (`:name-taken`: its name was
  used by other content). Faces consume nothing (phase 2: "A `:name-taken`
  writes nothing and leaves the cited rows alone, as every face refusal
  does"). K stays bare in the row; the depot holds V about Bob; Bob is
  forgotten. Something retained opens a value about a forgotten person.
  Phase 2 names this "the retention it bounds": "Such bytes are not a value
  of the store (never admitted), so no forget can name them; the close, the
  owner's forget, or a later decision citing the row reaches them." For the
  operator's bare row there is no owner's forget, so only a later decision
  or the close reaches it, and **the plan has no close** (§J 7): the
  retention is unbounded. FAIL (F4, F5).
- **Before the decision, in the window.** Between Bob's forget and `O'`'s
  batch, K and V are both retained; the batch closes the window (normally
  one batch, 250 ms to a few seconds). This holds for any lease, not only
  the operator's: a person-wrapped row opens for the store too, because the
  store holds person locks ("locks are for forgetting, not for hiding from
  the operator"). Named, not a failure; the window's length is the batch's.

Against default 1's sub-pick: "a lease row is consumed at decision whatever
the decision (admitted, it becomes the value's lock; refused or answered
from the record, it is destroyed), so nothing openable is kept that no
forget can name; a lock leased to another session is refused and left
alone; unconsumed lease rows go when their session closes." Phase 2 reads
"refused" as a recorded refusal; the plan follows phase 2 (one rule for
both gates), which I accept as the alignment, so the close is what bounds
the face-refused case. **Verdict: FAIL, fixed by F4 (the close on the micro
side) and F5 (the operator's row named as phase 2 names it, and its gather
exemption).** Whether a face-refused offer's depot bytes about a person
forgotten later fall under "gone for everyone including the past" is a
question for Sid (below).

### T3. The faces entry for refusals meant to record nothing

R1: "a missing lock is refused on the offer's face with nothing recorded, so
a resend under the same name is still allowed."

History. `O` (fingerprint X) cites `[NL 5]`, never leased. Block 1: the
range read finds no row 5, status missing. Name task: no record. Face
`:no-such-lock` at `[NO :faces X]`. The door re-leases and resends under the
same name with lock `[NL2 0]` (fingerprint Y). Block 1 of the later batch
reads only `[NO :answer]` (plan line 1293), not faces; no record; the
lease is present; decided fresh; admitted. `micro-lookup [NO d Y F]`:
faces under Y absent, the record's parts digest matches, `:yes`. With
lock ids out of the parts digest (F2), X's and Y's parts digests are equal;
`micro-lookup [NO d X F]` returns X's face first (§D, lines 481-485), which
is the true answer for attempt X.

Does anything read it as a record? The gate: no (block 1, the fold and 2b
read `:answer` only; checked in lines 1293-1296, 1337-1342, 1358-1363). The
lookup: it returns the face's reason, marked as a face, never as the
name's answer. So the purpose, "a resend under the same name is still
allowed", is kept. **PASS**, with F10 (keep the first face, for M7).

### T4. R7: revoke cuts everything below, in one batch, the race, another layer, the stream gate's changes

- **D2 (model)**: `[:revoke [:alice :group :own]]`, a batch, then her group
  write, a batch: the rig's `[alice G G [G G G]]` revoked in batch b1; the
  write in b2 reads the committed row; refused `:permission-revoked`.
  The model's expected `:missing`. PASS.
- **D1 (model)**: the write cites `[:alice :group :session]`, whose fact
  lives in `:alice-hand` (model.clj `permission-layer`). The model refuses
  it `:permission-from-another-layer` at prepare, whatever the revoke's
  timing (model.clj `refusal` clause order, README 211-212). The rig maps it
  to `[alice G :alice-hand]` (plan lines 536-539); `permit/refusal` gives the
  same reason. PASS.
- **Trace 14** ("a gate that checks a permission kept in the other store
  misses a revocation"): under R7 the session's permission for a group or
  the base is written into that layer through its own gate, so it is kept
  where the micro gate orders it; a grant or revoke is never store-placed
  (`placed-keys` is `#{:class :lock-grain :forget :crossed}`, envelope.clj
  lines 35-38 in the main rig worktree), so after a re-class it carries
  `:by-entity` and reaches only this gate. PASS.
- **In one batch.** Order `[uuid7 name]`: revoke of `pA` (u10), then
  a write citing `pS` (u11): W marks `pA` revoked; `pS`'s chain holds `pA`;
  refused `:permission-revoked`. The reverse order admits the write. PASS
  with the walk and with the cascade (F8 keeps the in-batch check on the
  chain carried in the id).
- **A permission cited from another layer**, the pid or an ancestor:
  refused. PASS.
- **The stream gate's three changes** (§B): `gate/refusal`'s clauses,
  `pids-to-read`, `pid?`. Checked against the code: `refusal` at gate.clj
  127 with the clauses at 152-155, `pids-to-read` at 105, `pid?` at
  envelope.clj 265-268 (`(= 3 (count x))`). The three places exist and are
  as described. F8 adds to them (the cascade's writes in `decide`, the
  children index and the cut mark in `$$layers`).
- **Throughput.** The plan constructs the cascade and finds it cheaper
  (lines 392-402: "on throughput alone the cascade wins"), then keeps the
  walk for fewer stream-gate lines. **FAIL** under the template's
  throughput rule; fixed by F8.

### T5. R8: the base, one-owner under the root actor, re-classed before the first group

History. Seed: the base made on the stream gate, `:kind :base`, owner the
root actor, `:by-layer`, per-value; root `[R :base :base]` (R is `:root` in
the plan, `:operator` in phase 2: F12); Alice's `[alice :base :base
[R :base :base]]`. A base fact `f1` admitted there, its lock in the record
(shared rules from day one: phase 2, V-F2). `make-group!`: reads the class,
offers `{:e :base :k :class :v :by-entity}` (store-placed, so on the stream
gate, P16), waits, then offers G's making act standing on the re-class's
stamp (M6).

What the micro gate then reads as settled history, on hash(:base):
`$$layers [:base :settings]` (class, kind, owner, grain), the stream-era
permission rows, and the stream-era heads (`f1`). Settled means written
before the re-class on the home's single thread: the home commits each
event whole, so when the micro gate reads the class `:by-entity`, every
earlier stream event on that task is visible (What Rama showed 1). After
the re-class the stream gate can still write two things for the base: a
store-placed setting (`:lock-grain`, P16) and a forget of a stream-era
target (M25). Both are reads at a moment for this gate (the named D6
consequence and §J 10); neither is a permission. Permissions after the
re-class go only through this gate (T4). A stream-era `f1` replaced here
gets its tombstone in `$$micro [:base :replaced ...]`, the stream row stays
byte-identical. **PASS** after F12.

### T6. The frontier

Block 0 of batch b writes `:frontier = b − 1` on every task (plan lines
1274-1279); it becomes visible on task T when b commits on T; batch b
started only after every task committed b − 1 (What Rama showed 7). So a
reader who reads F on any task knows every batch ≤ F is committed on every
task. It passes F to every task and hides every row whose `:batch` is above
F (M7). History: a two-entity act in batch 7, entities on tasks 1 and 3;
task 1 commits 7 before task 3. A reader takes F = 6 from task 1 (batch 7's
block 0 wrote 6, visible after 7 committed on task 1), reads task 1 (sees
the act's row, `:batch 7` > 6, hides it) and task 3 (row not yet visible, or
visible and hidden). It never sees half: PASS. The id's class: a Long, the
same across a retry (the probe, §G, run 2 passed). The read entry's slot
`{:frontier F}` is the read exit's (PLAN-read-exit.md line 128 at 5a6bb732,
"will put `{:frontier id}` in its place (phase 3's settled-frontier id)"):
PASS, with the line number corrected (F9).

Exceptions to "never overwritten": lease rows consumed (named as ruled);
the close's deletion (F4, same class); a face written twice (F10, fixed);
`:clock` is the gate's own, never read by a reader. A door reading a
consumed lease at an old F sees it gone, which is harmless: it was used.

### T7. Names

- **Found by name plus layer through the name row written in the same
  batch**: 2a writes `[NO :answer]` on hash(NO) in the deciding batch; the
  lookup routes by the name, whose first element is the layer. PASS.
- **A retried batch**: the attempt is discarded whole; the retry decides the
  same order on the same state; the stamp can differ, only one commits.
  PASS.
- **A reused name refused by digest, in another batch**: parts digest
  differs → `:name-taken`; same parts digest → the record path's value
  check (F2 aligns it with phase 2's V-F1). PASS after F2.
- **The same, in the same batch**: FAIL (F3). History: envelopes E1 (V1
  about Carol) and E2 (V2 about Bob, forgotten), one name, same lock id
  `[NL 0]` (a buggy door), so one parts digest (with or without lock ids).
  Both arrive on task 2. Both emit `[[:offer NO] skeleton]`; `+map-agg`
  keeps one, whichever merges last (plan line 1316, 1322). Say the fold
  decides E1's skeleton (subjects {carol}): yes. 2b: both E1 and E2 pass
  `(= (:digest *rec) *digest)` (line 1339) and write `[e NO 0]`; if E2's
  write lands last, the store holds V2, about a forgotten person, never
  checked, under a yes decided on V1. Fixed by F3.

### T8. The two build checks

- `materialize>` after the post-agg of `|global` + `+map-agg`: the skill's
  own example (batch.md "Materialization") materializes in a post-agg
  after an aggregation, and the plan's post-agg holds no partitioner
  (batch.md: "No partitioners in post-agg"). Its fallback (a durable
  scratch PState on task 0, "What this plan could not settle") changes the
  place, not the design; block 2c rides on the same. Not probed: its
  failure would not change the design.
- A `<<cond` with partitioners in a pre-agg-only block: batch.md says
  control flow "is fine within a branch" in pre-agg; the fallback (three
  blocks, one per route kind) changes no design. Not probed, same reason.

### T9. Cost

The table's arithmetic checks: 0.50·7 + 0.18·8 + 0.05·8 + 0.12·11 +
0.10·3 + 0.02·1 + 0.03·4 = 7.10; proportions sum to 1.00; flat in N. But it
is block 1's count only (FAIL, F7). The recount (F7, in the plan's §H)
gives about 10.6 seeks per operation with the cascade and about 12.5 with
the walk, flat in N both ways. The walk against the cascade: F8.

## Query topology: micro-lookup, micro-act, micro-lease, micro-leases-of

- Input examples present: yes for `micro-lookup` and `micro-act` (plan
  lines 1449-1467), `micro-lease` (lines 155-166).
- `micro-lookup`, after §D: a decided name: N = 2 (F, faces) + 1 (record)
  = 3, M = 3 (faces is asked first, and an absent face is the answer "no
  face"); a face-refused attempt: N = 2, M = 2 (the face ends it); a name
  never sent: N = 3, M = 3. The body (lines 1449-1462) still describes the
  old order (F9). M varies; the plan marks it variable (`<<if`). PASS after
  F9.
- `micro-lease`: N = 2 (F, the range), M = 2 in both examples; fixed. PASS.
- `micro-leases-of`: 1 seek + k iterations on hash(s), then `micro-lease`
  per name: a client-side fan-out of k queries for a restarted door, rare.
  PASS.

## PState schemas

- Groups by (key type, partitioner): `$$micro` (entity or layer id,
  hash), `$$micro-names` (name vector, hash), `$$micro-task` (none, per
  task). Lease rows sit in `$$micro-names` (same key and partitioner as the
  lease act's name row): merged, not split. PASS.
- Object type: none. PASS.
- Fixed-keys for records: yes. The lease row's fields disagree between
  §A and §I (F1). FAIL → fixed.
- Variant instances: none new. PASS.
- Subindexing: `:leases` subindexed (≤ 256, for one-seek deletes); `:faces`
  subindexed. With F8, the children index `[L :children parent]` is
  subindexed (a group root's children are every member). PASS after F8.

## Partitioning

- Every write's partitioner: lease rows by the lease name (hash, a random
  UUID7 inside, so no hot key: a hot session moves to a new task with every
  lease, M24); rows by entity; records by name; permissions by layer (a
  hot group's layer task carries its permission reads, as in the 25
  September plan). PASS.
- The table: FAIL (F7), recomputed in the plan.
- Weighted seeks from N = 1 to 128: flat. PASS.
- Placement state considered: no stored placement (P2's argument). PASS.

## Topologies

- Microbatch, ruling 1's type; no stream concern; no test-synchronisation
  argument. PASS.

## Production readiness

- Concurrent clients: FAIL on two envelopes under one name in one batch
  (T7, F3). Two offers citing one lock under two names: the double-use
  check, `:no-such-lock` for the later (PASS once F3 places the check after
  the same-name step).
- Client restart: a restarted door finds its leases (`micro-leases-of`) or
  re-leases; its resend is value-checked under its own lock (F2). PASS
  after F2.
- Worker restart: the attempt is discarded; the retry is deterministic
  except stamps and nonces, of which one attempt's exist. PASS.
- Scale: every growing collection subindexed. PASS.
- Stream topologies: none added here. N/A. Multi-partition stream writes:
  N/A.

## Internal depots, cross-topology, stream topology correctness

None added (the landing append is stage 4's). N/A.

## In-memory state efficiency

No TaskGlobal; the fold's W lives for one batch. PASS.

## Minimality — adversarial simplification

Simplest design sketch: one microbatch topology; lease rows keyed where
the sealed act arrives; one fold on one task; one row pass; one
consumption pass; a frontier from the microbatch id. The plan is that
sketch plus the name row (ruled), the faces entry (T3), the tick depot
(frontier liveness), and the record path.

- **Block 2c.** Delete it: 2b would have to delete the rows it reads, and
  2b does not know a face from a decision (the fold's output). Merge into
  2b: 2b knows the record (it reads `[name :answer]`), so a yes or no
  decided this batch could delete its own cited rows in 2b after the
  re-read; a face could not (not in the record). 2c is the one place that
  covers decided acts, record-path answers (F2) and the close (F4) alike;
  it costs no reads. Kept.
- **The double lock work (block 1 and 2b).** Constructed alternative: carry
  the wrapped lock and the sealed bytes in the skeleton through the leader
  and write rows from 2a. Cost: every value's bytes funnel through task 0
  (the plan's own objection to leases on the leader, line 113); against it
  2b re-reads 1 lease range and 2 `$$persons` entries per sealed act, all
  local to the arrival task. The funnel's cost is unbounded in value size;
  the re-read is 3 local seeks. Kept.
- **The faces entry.** Delete it: the offerer learns a face refusal only
  by timing out (no ack on a microbatch depot). Kept (T3).
- **The tick depot.** Delete it: the frontier stops after the last data
  batch; a reader never sees it. Kept.
- **`$$micro-task`.** Required for the per-task clock and the frontier.
  Kept.

## Throughput — adversarial

- **The permission walk** (F8). The plan's construction: walk ≈ +1.5 seeks
  per offer (+2 at depth 3, +1 at depth 2; +4 for a re-classed layer, both
  stores); cascade ≈ +1 read per grant, a subtree read per revoke, ≈ 0.1
  seek per offer amortised at one revoke per 10,000 offers and s ≈ 1,000.
  Same latency class. The cascade does less total work: FAIL, adopt it.
  Fixed by F8 (in §B), with the walk kept as the named fallback.
- **The lease range read in block 1 and 2b**: one seek each; a point read
  per lock would be one seek per lock. The range is the cheaper. PASS.
- **Routing by lease name**: the entity task is one hop away for the row;
  the alternative (route by entity, lease rows on the entity task) needs
  the lock minted before the entity is known, which a lease cannot be
  (lines 43-47). PASS.

## Spec coverage

### Sid's phase 3: "A microbatch gate for group and base layers placed by entity, atomic across tasks"

- **Source**: SPEC.md phase 3, verbatim; default 6 moves the base's start
  to the stream gate.
- **Trace**: T1 (one batch, every write, all tasks), T5 (the base moves at
  the first group).
- **Fault tolerance**: a worker crash in any block discards the attempt on
  every task (What Rama showed 5); the retry decides the same order; the
  plan's test injects one throw per block (§F).
- **Race**: two clients on one entity: one batch order; two on one name:
  F3.
- **Flaws**: F3. **Verdict**: FAIL → fixed.

### "with a name row written in the same batch so an answer is found by name plus layer"

- **Trace**: T7. **Fault tolerance**: the name row is a 2a write of the
  deciding batch, atomic with the rows. **Race**: F3.
- **Verdict**: PASS after F3.

### "Wrapped locks in the record."

- **Source**: SPEC phase 3; ruling 7 ("in the record for agent sessions,
  group layers and the base"); default 1.
- **Trace**: T1: the row's `:lock` is the re-wrapped lease lock, wrapped
  under the value's own subjects (7b as written: `:required []`, `:any-of`
  the subjects; marked: required all).
- **Fault tolerance**: T1; 2b's re-read of `$$persons` can differ from
  block 1's (F6): a subject forgotten in between gives a record that does
  not open for that subject (the any-of list is the subject list, never
  the live list, so never an empty wrap); a lease person forgotten in
  between gives `unlease` nil in 2b and the row is written closed.
- **Verdict**: PASS after F6.

### "Permissions as facts in the layer they govern, checked only by the gate that orders that layer; a revoke; an offer citing a permission from another layer refused."

- **Trace**: T4. **Race**: the revocation race in one batch (T4), across
  batches (committed rows), across stores (trace 14, closed by R7).
- **Verdict**: PASS for behaviour; FAIL on throughput (F8) → fixed.

### "Re-class of a hot layer moves it here."

- **Trace**: T5 for the base; the same for a one-owner layer (plan §F's
  `:alice-agent` test). PASS.

### "Tests: the model's micro and permission cases, and the revocation race."

- **Trace**: §F lists `micro-prepare`/`micro-decision`/`micro-commit`,
  `stamp-for`, `lookup`, `refusal`, D1, D2, the six A cases in the group
  (scenarios.clj: A2 to A7), and the race. The chain tests are the rig's
  own: the model has no parent or chain (checked: no "parent" and no
  permission chain in model.clj, scenarios.clj or README.md). PASS, with
  F11's wording.

### What Rama showed 5: "Nothing may rely on seeing a microbatch on every task at once."

- **Trace**: T6. PASS.

### Default 1 (forget, sealed at the door)

- **Trace**: T1, T2, T3. **Flaws**: F1, F2, F4, F5. **Verdict**: FAIL →
  fixed.

### Default 2 (hybrid clock)

- **Trace**: §E: `max(ms × 65536, last + 1, stood-on + 1)` through
  `gate/decide`, the leader's wall read once per batch; the touched tasks'
  clocks from W. Checked against the clock default in RIG.md. PASS.

### Default 3 (read entries: a shared layer's moment is its settled frontier's id)

- **Trace**: T6. PASS after F9's citation.

### Default 5 (R7)

- T4. FAIL (F8, F11) → fixed.

### Default 6 (R8)

- T5. FAIL (F12) → fixed.

### IMPLICIT_SPEC and the stream plan

The 25 September plan's coverage of I-G1 to I-G8, I-O1 to I-O5, I-P1,
I-P3 to I-P5 is unchanged by the revision except where F2 and F3 touch
I-G4 (the name row) and I-G5's digest (the parts digest now without lock
ids); both traced above.

## Self-consistency

Nothing above is passed while called a gap. The two named edges (the
batch-long window before a decision, T2; the same-batch honest re-seal
answered by a face, F3) are stated as behaviour with their reason, and go
to builder A and Sid below.

## Fixes applied to the plan (marked `[PV-Fn]` in PLAN-micro-store.md)

F1 to F13 as listed under the verdict; the text of each is in the plan,
in the revision's change list, §A, §B, §C, §D, §F, §H, §I, §J, "The shapes"
(batch order), block 1's face list, and the "Query Topologies" section.
Commits: 6b12638c (this file, first version), dfed5467 (F1 to F12 in the
plan), and the commit after it (F13, the born-cut reason, this file's
update).

Residual edges left named, not failed: a lease act decided after a close
of the same session in one batch keeps its rows (the close's gather reads
committed state), and `(termval NONE)` on a whole subindexed `:leases` map
is the build's to check (the fallback deletes entry by entry).

## For builder A and Sid

1. **F8 overrides the brief's "fewest lines in the stream gate."** The
   cascade changes `decide`, `$$layers`' schema and the grant path on the
   stream gate, while phase 2 edits `gate.clj` in parallel. The walk stays
   written as the fallback (correct, about 1.9 seeks per operation more).
   Builder A's call if the merge risk tonight outweighs the template.
2. **Sid: a face-refused offer's depot bytes about a person forgotten
   later** stay openable by their unconsumed lease row until the session
   closes (phase 2's "retention it bounds"). Is that inside "gone for
   everyone including the past"? If not, faces that proved their lock
   (the value opened) consume it too, on both gates.
3. **For builder A (F13):** phase 2's value checks (`:does-not-open` to
   `:grain-mismatch`) must be callable apart from its `decide`, since the
   micro leader never holds a value; the reasons and their order stay one
   code.
4. **First-record picks touched by the fixes**: the parts digest without
   lock ids (phase 2's, adopted); the name row's decided fingerprint
   (`[name :fp]`, F3, a gate mark outside the answer record); the cut mark
   and the children index (F8, projections, rebuildable, no record); the
   root actor's id `:operator` (F12, phase 2's).
5. **Named divergence (F3)**: an honest door that re-seals and resends
   within one batch has one attempt decided (whichever sorts first by
   fingerprint) and hears `:name-taken` on its face for the other, so it
   asks by every fingerprint it sent; the stream gate would answer the
   second from the record after a value check.

PHASE_VALIDATION:minor-fail
